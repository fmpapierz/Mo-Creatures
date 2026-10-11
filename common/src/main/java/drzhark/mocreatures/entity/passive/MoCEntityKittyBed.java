package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityKittyBed}. A placeable piece of furniture — a bed the kitty
 * sleeps in and feeds from. It has no goals and cannot be pushed; it drops to the ground where the player
 * placed it and then stays put. Faithful to the legacy entity it restores the milk/pet-food feeding,
 * kitty-healing-while-nestled and pickaxe-pickup mechanics:
 *
 * <ul>
 *   <li>Right-click with a <b>milk bucket</b> to fill the bed with milk (the bucket is emptied);</li>
 *   <li>Right-click with <b>pet food</b> to fill the bed with food;</li>
 *   <li>Right-click with any <b>pickaxe</b> to pick the bed back up as its item (setDead);</li>
 *   <li>Right-click empty-handed (or with anything else) to carry the bed on your head / set it down;</li>
 *   <li>Hit it (left-click) to break it: it drops its coloured item, or simply vanishes in creative;</li>
 *   <li>while a kitty is nestled in a stocked bed it heals to full, slowly draining the supply.</li>
 * </ul>
 */
public class MoCEntityKittyBed extends MoCAnimal {

    /** Legacy datawatcher 16: the bed currently holds milk. */
    private static final EntityDataAccessor<Boolean> HAS_MILK =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.BOOLEAN);
    /** Legacy datawatcher 15: the bed currently holds pet food. */
    private static final EntityDataAccessor<Boolean> HAS_FOOD =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.BOOLEAN);
    /** Legacy datawatcher 18: dye/sheet colour, round-tripped through the kittybed item on pickup. */
    private static final EntityDataAccessor<Integer> SHEET_COLOUR =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.INT);

    /** Server-side supply counter: rises +0.003/tick while a kitty feeds, emptying the bed past 2.0. */
    private float milkLevel;

    /**
     * No {@code setNoAi(true)} here, although the bed has no goals. In 26.2 NoAI does not only silence the
     * brain: {@code LivingEntity.aiStep} calls {@code travel} — the ONLY place gravity and momentum are applied —
     * solely when {@code isEffectiveAi()} (mc262-ref LivingEntity.java:3110), and {@code Mob.isEffectiveAi()} is
     * {@code super && !isNoAi()} (Mob.java:1301-1302). A NoAI bed therefore never fell: it hung wherever it was
     * created — at the player's feet while flying or jumping, one block up after a click on a grass tuft or a
     * wall, and at head height after being carried and set down ({@code putDown}'s release impulse was never
     * integrated either). Legacy was an ordinary {@code EntityLiving} that fell and then held still through its
     * {@code move} gate, which {@link #move} reproduces.
     */
    public MoCEntityKittyBed(EntityType<? extends MoCEntityKittyBed> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                // Legacy MoCEntityKittyBed.getMaxHealth() == 20.
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Static furniture: no goals at all. */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_MILK, false);
        builder.define(HAS_FOOD, false);
        builder.define(SHEET_COLOUR, 0);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("fullkittybed.png");
    }

    // ------------------------------------------------------------------------------ synched state

    public boolean getHasMilk() {
        return this.entityData.get(HAS_MILK);
    }

    public void setHasMilk(boolean hasMilk) {
        this.entityData.set(HAS_MILK, hasMilk);
    }

    public boolean getHasFood() {
        return this.entityData.get(HAS_FOOD);
    }

    public void setHasFood(boolean hasFood) {
        this.entityData.set(HAS_FOOD, hasFood);
    }

    public int getSheetColour() {
        return this.entityData.get(SHEET_COLOUR);
    }

    public void setSheetColour(int colour) {
        this.entityData.set(SHEET_COLOUR, colour);
    }

    /**
     * Maps the bed's sheet colour back to the item it should be picked up as. Colour 0 (plain) → the base
     * {@code kittybed}; 1..16 → the matching dyed kittybed (index = DyeColor id + 1). Legacy round-tripped the
     * colour through the single damage-keyed item; the 26.2 split uses one item per colour.
     */
    private static net.minecraft.world.item.Item bedItemForColour(int colour) {
        return switch (colour) {
            case 1 -> MoCItems.KITTYBED_WHITE.get();
            case 2 -> MoCItems.KITTYBED_ORANGE.get();
            case 3 -> MoCItems.KITTYBED_MAGENTA.get();
            case 4 -> MoCItems.KITTYBED_LIGHT_BLUE.get();
            case 5 -> MoCItems.KITTYBED_YELLOW.get();
            case 6 -> MoCItems.KITTYBED_LIME.get();
            case 7 -> MoCItems.KITTYBED_PINK.get();
            case 8 -> MoCItems.KITTYBED_GRAY.get();
            case 9 -> MoCItems.KITTYBED_SILVER.get();
            case 10 -> MoCItems.KITTYBED_CYAN.get();
            case 11 -> MoCItems.KITTYBED_PURPLE.get();
            case 12 -> MoCItems.KITTYBED_BLUE.get();
            case 13 -> MoCItems.KITTYBED_BROWN.get();
            case 14 -> MoCItems.KITTYBED_GREEN.get();
            case 15 -> MoCItems.KITTYBED_RED.get();
            case 16 -> MoCItems.KITTYBED_BLACK.get();
            default -> MoCItems.KITTYBED.get();
        };
    }

    // ------------------------------------------------------------------------------- interaction

    /**
     * Stocks the bowl from the item in {@code hand}: a milk bucket loads milk and hands back an empty bucket
     * (legacy bucketMilk -> bucketEmpty); pet food loads food, only if the bowl holds no food yet (legacy). Used
     * by a click on the bed and by a click on its food bowl ({@link #fillBowlUnderCrosshair}).
     *
     * @return whether the item was one the bowl takes (the click is then consumed on both sides)
     */
    public boolean fillBowl(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean server = !this.level().isClientSide();
        if (stack.is(Items.MILK_BUCKET)) {
            if (server) {
                player.setItemInHand(hand,
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                setHasMilk(true);
                setHasFood(false);
                this.playSound(MoCSounds.POURINGMILK.get(), 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return true;
        }
        if (stack.is(MoCItems.PETFOOD.get()) && !getHasFood()) {
            if (server) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setHasMilk(false);
                setHasFood(true);
                this.playSound(MoCSounds.POURINGFOOD.get(), 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return true;
        }
        return false;
    }

    /**
     * The food bowl (the tray drawn beside the bed: MoCModelKittyBed's food_tray parts, model x -16..-10, z -1..6,
     * y 21..24 px) lies outside the bed's 0.98-wide hitbox, so a click on it used to land on the ground behind
     * it. This box is that tray in the bed's own frame: MoCMobRenderer draws a model point (x, y, z) at
     * {@code Ry(180 - bodyRot) * (-x/16, 1.501 - y/16, z/16)} from the bed's position (LivingEntityRenderer's
     * rotation, then scale(-1, -1, 1) and the 1.501 drop), with a little margin so the rim is easy to hit.
     */
    private static final net.minecraft.world.phys.AABB BOWL = new net.minecraft.world.phys.AABB(0.6D, 0.0D, -0.1D,
            1.05D, 0.3D, 0.42D);

    /** How far along a look ray (eye + t * unit look) it meets this bed's food bowl, or -1 if it misses. */
    private double bowlHitDistance(Vec3 eye, Vec3 look, double reach) {
        double theta = Math.toRadians(180.0D - this.yBodyRot);
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        Vec3 o = eye.subtract(position());
        // world -> bed frame: the inverse of the renderer's rotation about Y
        Vec3 from = new Vec3(o.x * cos - o.z * sin, o.y, o.x * sin + o.z * cos);
        Vec3 dir = new Vec3(look.x * cos - look.z * sin, look.y, look.x * sin + look.z * cos);
        return BOWL.clip(from, from.add(dir.scale(reach))).map(hit -> hit.distanceTo(from)).orElse(-1.0D);
    }

    /**
     * A right click (on a block or on air) with a milk bucket or pet food whose crosshair is on a kitty bed's food
     * bowl stocks that bed. Runs on both sides with the same geometry, so the client neither drinks the milk nor
     * places anything. A block nearer than the bowl keeps the click. Registered for Architectury's
     * RIGHT_CLICK_BLOCK and RIGHT_CLICK_ITEM events in {@code MoCreatures.init}.
     */
    public static boolean fillBowlUnderCrosshair(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.MILK_BUCKET) && !stack.is(MoCItems.PETFOOD.get())) {
            return false;
        }
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        MoCEntityKittyBed best = null;
        double bestT = Double.MAX_VALUE;
        for (MoCEntityKittyBed bed : player.level().getEntitiesOfClass(MoCEntityKittyBed.class,
                player.getBoundingBox().inflate(reach + 2.0D))) {
            if (!bed.onGround() || bed.isBeingCarried()) {
                continue; // the tray is only drawn on a bed standing on the ground
            }
            double t = bed.bowlHitDistance(eye, look, reach);
            if (t >= 0.0D && t < bestT) {
                best = bed;
                bestT = t;
            }
        }
        if (best == null) {
            return false;
        }
        net.minecraft.world.phys.BlockHitResult block = player.level().clip(new net.minecraft.world.level.ClipContext(
                eye, eye.add(look.scale(bestT)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (block.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && block.getLocation().distanceTo(eye) < bestT - 1.0E-3D) {
            return false;
        }
        return best.fillBowl(player, hand);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        final boolean server = !this.level().isClientSide();

        if (fillBowl(player, hand)) {
            return InteractionResult.SUCCESS;
        }
        // Any pickaxe -> pick the bed back up as its item (carrying its sheet colour) and remove it.
        if (stack.is(ItemTags.PICKAXES)) {
            if (server) {
                ItemStack drop = new ItemStack(bedItemForColour(getSheetColour()));
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
                this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                breakEffects((ServerLevel) this.level());
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        // Otherwise: carry the bed on the player's head, or set it back down (legacy mount toggle).
        if (server) {
            // Shared carry toggle. The furniture used the same "ride the player" idiom the pets did, and it
            // was broken for exactly the same reason: a player cannot be a vehicle on a 26.2 server.
            toggleCarry(player, false);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // Clear any grass or flower the bed came to rest in, so it doesn't poke up through the bedding.
        if (this.onGround()) {
            breakPlantsInside((ServerLevel) this.level());
        }
        // While a kitty eats in a stocked bed it heals to full and slowly drains the supply (legacy MoCEntityKitty
        // state 4 set health = maxHealth; the bed drained milklevel in onUpdate). Legacy kitties only ever sat in a
        // bed to eat; a kitty that merely sleeps in one (the port's night routine) leaves the bowl alone.
        if ((getHasMilk() || getHasFood()) && this.getFirstPassenger() instanceof MoCEntityKitty kitty
                && kitty.getKittyState() == MoCEntityKitty.STATE_EATING) {
            kitty.heal(kitty.getMaxHealth());
            milkLevel += 0.003F;
            if (milkLevel > 2.0F) {
                milkLevel = 0.0F;
                setHasMilk(false);
                setHasFood(false);
            }
        }
    }

    // ---------------------------------------------------------------------------------- movement

    /**
     * Legacy {@code MoCEntityKittyBed.move} ran the move only while the bed was riding, airborne, or
     * {@code staticBed} was off (5.1.5 {@code MoCEntityKittyBed.java}:231-240; 12.0.5:195-202 tested
     * {@code staticLitter} by copy-paste): a bed resting on the ground ignored every shove, so it fell into
     * place and then held still. The same gate here, with one deliberate difference — a grounded bed still
     * takes the DOWNWARD part of its motion. Legacy skipped {@code move} entirely, which also froze
     * {@code onGround} (only {@code move} recomputes it, mc262-ref Entity.java:764-768), so a bed whose floor
     * was dug out hovered in the air. Only the bed's own ({@link MoverType#SELF}) motion is gated — pistons and
     * shulkers still move it — and the discarded momentum is cleared too, so an explosion or a water current
     * cannot bank it for the next airborne tick.
     */
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && drzhark.mocreatures.config.MoCConfig.get().staticBed && this.onGround()) {
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
            delta = new Vec3(0.0D, Math.min(0.0D, delta.y), 0.0D);
        }
        super.move(type, delta);
    }

    /** Legacy {@code fall()} was empty: dropping the bed from a height makes no fall sound or damage roll. */
    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    /** Legacy {@code canBreatheUnderwater() == true}: a bed placed in water does not run an air supply. */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /**
     * A kitty that climbs into the bed sits ON THE CUSHION, inside the walls. With no seat of its own the bed used
     * the default passenger attachment — the top of its 0.35-high hitbox — so the kitty hovered about 0.16 blocks
     * above the cushion, level with the rim. The cushion (the wool sheet, {@code MoCModelKittyBed.Sheet}: model
     * y 21-24) tops out 3 px = 0.1875 above the ground, and a kitty's feet are its vehicle attachment point
     * (the AT_FEET default, mc262-ref EntityAttachment.java:8), so that is the seat. (Legacy seated it at the 1.12
     * default {@code height * 0.75} = 0.225 of a 0.3-high bed, i.e. on the cushion as well.)
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions,
            float scale) {
        return new Vec3(0.0D, (3.0D / 16.0D) * scale, 0.0D);
    }

    /**
     * Whatever lies in the bed never steers it. With AI enabled, {@code Mob.getControllingPassenger} hands
     * control to a mob passenger (mc262-ref Mob.java:221-224 — it was only null before because of NoAI), and a
     * controlled mob's {@code getNavigation()}/{@code getMoveControl()} then return the VEHICLE's
     * (Mob.java:208-218): a nestled kitty would have driven the bed's pathfinder instead of its own.
     */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    // ------------------------------------------------------------------------------------- damage

    /**
     * Legacy {@code MoCEntityKittyBed.attackEntityFrom} returned {@code false}: no mob, arrow, explosion, fire,
     * fall or drowning can destroy the bed. The one exception is damage tagged
     * {@code BYPASSES_INVULNERABILITY} — {@code /kill} and the void. {@code LivingEntity.kill} IS
     * {@code hurtServer(genericKill, MAX_VALUE)} (mc262-ref LivingEntity.java:317-319) and falling out of the world
     * is {@code hurt(fellOutOfWorld)} (:2162-2164), so the blanket {@code false} this used to return made the bed
     * immune to {@code /kill} and let it fall through the void forever. Players break it through
     * {@link #skipAttackInteraction} instead of through damage.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    /**
     * A player's hit breaks the bed and drops its item, carrying the sheet colour (none in creative), the way
     * vanilla's paintings and item frames break ({@code BlockAttachedEntity.skipAttackInteraction}). Legacy's
     * only removal was right-clicking with a pickaxe (kept in {@link #mobInteract}); punching did nothing,
     * which is exactly what read as "I can't break them".
     *
     * <p>This hook, rather than {@link #hurtServer}, because {@code Player.attack} consults it only for the
     * entity actually struck (mc262-ref Player.java:1010-1012) — a sword's sweep reaches bystanders through
     * {@code hurtServer} (:1150-1157), so a fight next to the bed cannot shatter it by accident. It runs on both
     * sides; returning {@code true} ends the attack there, so the client does not predict a damage flash and the
     * weapon takes no wear. Spawn protection and the world border are honoured through {@code mayInteract},
     * as vanilla furniture does. {@link #discard} ejects a nestled kitty (Entity.setRemoved:3921), and the
     * kitty's own AI forgets a bed that is no longer alive.</p>
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved()
                && serverLevel.mayInteract(player, this.blockPosition())) {
            if (!player.hasInfiniteMaterials()) {
                this.spawnAtLocation(serverLevel, new ItemStack(bedItemForColour(getSheetColour())));
            }
            this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            breakEffects(serverLevel);
            this.discard();
        }
        return true;
    }

    /**
     * Breaking the bed looks and sounds like breaking a block: a burst of break particles of the bed's own item
     * (so they carry its sheet colour) and the wood break sound.
     */
    private void breakEffects(ServerLevel level) {
        level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(
                        net.minecraft.core.particles.ParticleTypes.ITEM, bedItemForColour(getSheetColour())),
                getX(), getY() + 0.2D, getZ(), 24, 0.3D, 0.12D, 0.3D, 0.06D);
        level.playSound(null, getX(), getY(), getZ(),
                net.minecraft.world.level.block.SoundType.WOOD.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                1.0F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    // ------------------------------------------------------------------------------ push behaviour

    // Static furniture isn't shoved around by other entities (legacy staticBed, default on). Setting
    // staticBed=false lets the bed be pushed like a loose entity.
    @Override
    public boolean isPushable() {
        return !drzhark.mocreatures.config.MoCConfig.get().staticBed;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    // ------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasMilk", getHasMilk());
        output.putBoolean("HasFood", getHasFood());
        output.putInt("SheetColour", getSheetColour());
        output.putFloat("MilkLevel", milkLevel);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasMilk(input.getBooleanOr("HasMilk", false));
        setHasFood(input.getBooleanOr("HasFood", false));
        setSheetColour(input.getIntOr("SheetColour", 0));
        milkLevel = input.getFloatOr("MilkLevel", 0.0F);
        // Beds placed by the earlier build were saved with NoAI:1b (Mob.addAdditionalSaveData writes it and
        // readAdditionalSaveData restores it, mc262-ref Mob.java:388-389 / :408), and would reload still frozen
        // in mid-air. The bed has no AI to disable, so the flag only ever meant "no gravity": clear it.
        this.setNoAi(false);
    }
}
