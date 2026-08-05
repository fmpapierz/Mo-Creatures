package drzhark.mocreatures.entity;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base class for all passive Mo'Creatures animals on Minecraft 26.2.
 *
 * <p>Shared Mo'Creatures state (sub-type, tamed, adult, age, owner, saddled) lives on synched-data and
 * is persisted via {@link ValueInput}/{@link ValueOutput}. The per-species interaction rules — how the
 * creature is tamed (feed / pick-up / medallion), what it eats, whether it heals on feeding, whether it
 * can breed, be milked, be saddled and ridden, whether it scales as a baby, and what it drops — are
 * data-driven from {@link MoCBehavior}, ported faithfully from the legacy 1.12.2 source.
 */
public abstract class MoCAnimal extends Animal implements IMoCEntity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ADULT =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.BOOLEAN);
    /**
     * Legacy big-cat "hasEaten" flag (datawatcher 23): set true only when a non-adult cub eats a dropped
     * raw pork/fish, and it is the prerequisite for medallion-taming a big cat. Kept on the shared base so
     * the taming gate compiles and persists; only big cats read/write it.
     */
    private static final EntityDataAccessor<Boolean> EATEN =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.BOOLEAN);

    protected MoCAnimal(EntityType<? extends MoCAnimal> type, Level level) {
        super(type, level);
    }

    /** Reasonable shared defaults; most creatures override with their own attribute values. */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    protected MoCBehavior.Spec behavior() {
        return MoCBehavior.of(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoCSitGoal(this));
        // Fearless legacy predators never flee: MoCEntityBigCat.isNotScared() forced fleeingTick=0 every
        // tick so a wild lion/tiger stands and fights when struck. Only non-predators panic and run.
        if (!behavior().wildHostile) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        }
        if (behavior().canBreed) {
            this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        }
        this.goalSelector.addGoal(5, new MoCFollowOwnerGoal(this, 1.1D, 10.0F, 4.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Wild predators (untamed adults) hunt players and retaliate when struck.
        if (behavior().wildHostile) {
            this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.3D, true));
            // Retaliate when hurt — but a TAMED big cat never rounds on a player attacker, including its
            // own owner (legacy MoCEntityBigCat.attackEntityFrom returned false without setting
            // entityToAttack when a tamed cat was struck by a player).
            this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this) {
                @Override
                public boolean canUse() {
                    if (MoCAnimal.this instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat
                            && MoCAnimal.this.getIsTamed()
                            && MoCAnimal.this.getLastHurtByMob() instanceof Player) {
                        return false;
                    }
                    // Legacy MoCEntityBoar.attackEntityFrom only set entityToAttack when getIsAdult():
                    // a young (non-adult) boar never acquired its attacker as a target (isNotScared()==
                    // getIsAdult()==false meant it purely fled via vanilla panic). Keep piglets from
                    // fighting back — with no target the ungated MeleeAttackGoal also never engages.
                    if (MoCAnimal.this instanceof drzhark.mocreatures.entity.passive.MoCEntityBoar boar
                            && !boar.getIsAdult()) {
                        return false;
                    }
                    return super.canUse();
                }
            });
            this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<Player>(
                    this, Player.class, 10, true, false,
                    (living, serverLevel) -> !this.getIsTamed() && this.getIsAdult()) {
                @Override
                protected double getFollowDistance() {
                    // Legacy MoCEntityBoar.findPlayerToAttack only ever returned a player from
                    // getClosestVulnerablePlayerToEntity(this, attackRange) where attackRange was just 2 (Easy)
                    // or 3 (Normal/Hard) — and never on Peaceful. A wild boar therefore mostly ignored players
                    // and only threatened one at point-blank range, instead of pursuing across the full follow
                    // range. Scope this short range to the boar; every other wild predator keeps the base
                    // follow-range player targeting. (Range 0 would mean UNLIMITED in TargetingConditions, so a
                    // bounded value is used on Peaceful too, where the boar's own AI step clears the target.)
                    if (MoCAnimal.this instanceof drzhark.mocreatures.entity.passive.MoCEntityBoar) {
                        return switch (MoCAnimal.this.level().getDifficulty()) {
                            case EASY -> 2.0D;
                            case NORMAL, HARD -> 3.0D;
                            case PEACEFUL -> 2.0D;
                        };
                    }
                    return super.getFollowDistance();
                }
            });
        }
    }

    /** Whether this creature can be ridden once tamed (data-driven). */
    public boolean isRideable() {
        return behavior().rideable;
    }

    // ---------------------------------------------------------------------------- interaction

    private void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** Legacy {@code temper}: accrued progress towards taming, raised by feeding and rolled while being ridden. */
    private int temper;

    private void hearts(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    count, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MoCBehavior.Spec spec = behavior();
        final boolean server = !this.level().isClientSide();
        // Legacy MoCEntityWyvern.interact gated BOTH saddling (setRideable, edad > 90) and mounting
        // (mountEntity, edad > 90) on an essentially-adult wyvern — the same age gate its barding still
        // enforces (MoCEntityWyvern only wears barding while getIsAdult()). A freshly-hatched, tamed
        // non-adult wyvern must therefore not be saddleable or rideable. Scope the gate to the wyvern so
        // other rideables (horse, elephant, big cat, ostrich, komodo, ...) are unaffected.
        final boolean rideAgeOk =
                !(this instanceof drzhark.mocreatures.entity.passive.MoCEntityWyvern) || getIsAdult();
        // A horse must be an adult to be MOUNTED (legacy MoCEntityHorse.interact line 1882 gated
        // mountEntity on getIsAdult()), but — unlike the wyvern — a foal may still be SADDLED (legacy
        // saddling at line 1328 had no age gate). So gate only the mount branch for the horse, leaving
        // the saddle branch on rideAgeOk (which the horse doesn't restrict).
        final boolean mountAgeOk = rideAgeOk
                && (!(this instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse) || getIsAdult());

        // Milk a milkable creature (e.g. goat) with an empty bucket — or, with easyHarvesting on, straight
        // into a milk bucket handed over with an empty hand (legacy easyHarvesting: no bucket needed).
        // Legacy only allowed FEMALES to be milked: kids and males give no milk. Gate on adulthood, and
        // for a goat require a female sub-type (types 2-4; type 1 = kid, types 5-7 = male).
        if (spec.milkable && stack.is(Items.BUCKET) && canBeMilked()) {
            if (server) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.MILK_BUCKET)));
            }
            return InteractionResult.SUCCESS;
        }
        if (spec.milkable && canBeMilked() && stack.isEmpty() && MoCConfig.get().easyHarvesting) {
            if (server) {
                if (!player.addItem(new ItemStack(Items.MILK_BUCKET))) {
                    player.drop(new ItemStack(Items.MILK_BUCKET), false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Pick-up creatures (bunny, bird, mouse, snake, baby pet scorpion): right-click to carry / put down.
        // Legacy only ever picked up the SMALL ones — a pet scorpion is carried as a baby and RIDDEN as an
        // adult (MoCEntityPetScorpion.interact). Without the adult gate the pick-up branch swallows the
        // interaction and an adult pet scorpion can never be mounted.
        if (spec.tame == MoCBehavior.Tame.PICKUP && stack.isEmpty() && (!spec.rideable || !getIsAdult())) {
            if (server) {
                if (this.isPassenger() && this.getVehicle() == player) {
                    this.stopRiding();
                } else if (!this.isVehicle() && !player.isPassenger()) {
                    if (!getIsTamed()) {
                        // Enforce the tamed-per-player cap before taming.
                        if (exceedsTameCap(player)) {
                            return InteractionResult.SUCCESS;
                        }
                        setTamed(true);
                        setOwnerName(player.getName().getString());
                        hearts(7);
                    }
                    this.startRiding(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // PICKUP_TAMED: carry an already-TAMED creature on the player's head, but NEVER wild-tame by right-click
        // (legacy egg-hatched tamed snakes are carriable, yet a wild snake's right-click does nothing). A wild
        // one falls through so its heal/name interactions still run; only empty-hand carry is gated on tamed.
        if (spec.tame == MoCBehavior.Tame.PICKUP_TAMED && stack.isEmpty() && getIsTamed()) {
            if (server) {
                if (this.isPassenger() && this.getVehicle() == player) {
                    this.stopRiding();
                } else if (!this.isVehicle() && !player.isPassenger()) {
                    this.startRiding(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Feed an untamed creature its food to tame it.
        if (spec.tame == MoCBehavior.Tame.FEED && !getIsTamed() && MoCBehavior.matches(spec.foods, stack)) {
            if (server) {
                // Enforce the tamed-per-player cap; refuse without consuming the food.
                if (exceedsTameCap(player)) {
                    return InteractionResult.SUCCESS;
                }
                consume(player, stack);
                setTamed(true);
                setOwnerName(player.getName().getString());
                // Legacy taming does NOT make the creature an adult — MoCTools.tameWithName only sets tamed +
                // owner. Feeding nudges growth by one step and the normal age tick matures it from there, so a
                // tamed foal stays a foal (and stays unrideable) instead of snapping to full size.
                if (!getIsAdult() && getMoCAge() < 100) {
                    setMoCAge(getMoCAge() + 1);
                }
                heal(getMaxHealth());
                hearts(7);
            }
            return InteractionResult.SUCCESS;
        }
        // Give a Medallion to tame (big cat, kitty). Legacy MoCEntityBigCat.interact only tamed a big cat
        // that had already EATEN as a cub (getHasEaten): the eaten flag is set only when a non-adult cub
        // eats a dropped raw pork/fish, so a freshly-spawned cat — and every adult, which never eats and
        // so never sets the flag — cannot be tamed until raised and fed. Other medallion creatures (kitty)
        // keep their instant medallion taming.
        if (spec.tame == MoCBehavior.Tame.MEDALLION && !getIsTamed()
                && stack.is(MoCItems.MEDALLION.get()) && !stack.has(DataComponents.CUSTOM_NAME)
                && (!(this instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat) || getHasEatenMoC())) {
            if (server) {
                // Enforce the tamed-per-player cap; refuse without consuming the medallion.
                if (exceedsTameCap(player)) {
                    return InteractionResult.SUCCESS;
                }
                consume(player, stack);
                setTamed(true);
                setOwnerName(player.getName().getString());
                heal(getMaxHealth());
                hearts(7);
            }
            return InteractionResult.SUCCESS;
        }
        // Feed a TAMED big cat raw pork/fish (PORKCHOP/COD): legacy MoCEntityBigCat.interact set
        // health = getMaxHealth(), cleared hunger (setHungry(false)) and played the "eating" sound at
        // ANY health, and did NOT consume the item (no stack decrement). Special-cased before the generic
        // heal branch so a full-health cat still eats and a hurt cat keeps its porkchop/cod.
        if (this instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat bigCat
                && getIsTamed() && MoCBehavior.matches(spec.healOrFood(), stack)) {
            if (server) {
                setHealth(getMaxHealth());
                bigCat.setHungry(false);
                this.level().playSound(null, blockPosition(),
                        drzhark.mocreatures.registry.MoCSounds.EATING.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                        1.0F + (this.getRandom().nextFloat() - this.getRandom().nextFloat()) * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // Feed a tamed, hurt creature its (heal) food to heal it. Legacy fully restored health
        // (MoCEntityAnimal.interact / MoCEntityBigCat.interact both set health = getMaxHealth()); a fed
        // big cat also had its hunger cleared (setHungry(false)).
        if (getIsTamed() && getHealth() < getMaxHealth() && MoCBehavior.matches(spec.healOrFood(), stack)) {
            if (server) {
                consume(player, stack);
                heal(getMaxHealth());
                if (this instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat bigCat) {
                    bigCat.setHungry(false);
                }
                hearts(4);
            }
            return InteractionResult.SUCCESS;
        }
        // Name a tamed creature with a renamed medallion (anvil-rename, then right-click).
        if (getIsTamed() && stack.is(MoCItems.MEDALLION.get()) && stack.has(DataComponents.CUSTOM_NAME)) {
            if (server) {
                this.setCustomName(stack.getHoverName());
                this.setCustomNameVisible(true);
            }
            return InteractionResult.SUCCESS;
        }
        // Name a tamed creature you own with a PLAIN Medallion — opens the naming pop-up (client-side);
        // the screen sends the typed name back to the server. (Renamed-medallion anvil naming above still works.)
        if (getIsTamed() && stack.is(MoCItems.MEDALLION.get()) && !stack.has(DataComponents.CUSTOM_NAME)
                && getOwnerName().equals(player.getName().getString())) {
            if (!server) {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                        () -> () -> drzhark.mocreatures.client.MoCClientHelper.openNameScreen(
                                this.getId(), this.hasCustomName() ? this.getCustomName().getString() : ""));
            }
            return InteractionResult.SUCCESS;
        }
        // Saddle a tamed rideable creature that needs a saddle (the mod horse-saddle works like the vanilla one).
        // A wyvern must be adult first (legacy edad > 90), matching its barding gate.
        // A ride-tameable creature (horse, wyvern) may be saddled while still WILD — that is how you get on it
        // to break it in (legacy MoCEntityHorse:1328 / MoCEntityWyvern:363 have no tamed gate here).
        if ((getIsTamed() || spec.rideTames) && spec.rideable && spec.rideNeedsSaddle && !isSaddled() && rideAgeOk
                && (stack.is(Items.SADDLE) || stack.is(drzhark.mocreatures.registry.MoCItems.HORSESADDLE.get()))) {
            if (server) {
                consume(player, stack);
                setSaddled(true);
            }
            return InteractionResult.SUCCESS;
        }
        // Mount a tamed, rideable creature with an empty hand (a wyvern and a horse must be adult first —
        // legacy wyvern edad > 90, legacy horse getIsAdult() at line 1882).
        // Likewise mounting: a wild ride-tameable creature can be mounted, and then bucks until it submits
        // (see tickBreakingIn). getControllingPassenger still requires tamed, so a wild one cannot be steered.
        if ((getIsTamed() || spec.rideTames) && spec.rideable && stack.isEmpty() && !this.isVehicle() && mountAgeOk
                && (!spec.rideNeedsSaddle || isSaddled())) {
            if (server) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        // Non-breeders: swallow their (attraction) food so vanilla doesn't put them into love mode (or eat it).
        if (!spec.canBreed && !stack.isEmpty() && MoCBehavior.matches(spec.foods, stack)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Whether this creature may currently be milked. Legacy Mo'Creatures only let FEMALE adults be
     * milked — kids and males give no milk. Requires the creature to be an adult, and for a goat
     * (the only vanilla-milkable Mo'Creature) also requires a female sub-type: legacy goat types are
     * 1 = kid, 2-4 = females, 5-7 = males, so only 2-4 are milkable.
     */
    protected boolean canBeMilked() {
        if (!getIsAdult()) {
            return false;
        }
        if (this instanceof drzhark.mocreatures.entity.passive.MoCEntityGoat) {
            int type = getTypeMoC();
            return type >= 2 && type <= 4;
        }
        return true;
    }

    // ------------------------------------------------------------------------ tamed-per-player cap

    /**
     * Enforces the legacy tamed-per-player cap ({@code maxTamedPerPlayer}/{@code maxTamedPerOP}).
     * When {@link MoCConfig#enableOwnership} is on, counts how many tamed {@link IMoCEntity}s the
     * given player already owns among the entities currently loaded on the server, and returns
     * {@code true} if taming one more would meet or exceed the player's cap ({@code maxOPTamed} for
     * ops/permission level &gt;= 2, otherwise {@code maxTamed}). On refusal the player is messaged
     * "you have too many pets". Server-side only; a no-op (returns {@code false}) on the client or
     * when ownership is disabled.
     *
     * <p>Like the legacy mod this counts only <em>loaded</em> entities, so pets in unloaded chunks
     * or other dimensions that are not currently ticking are not included in the total.</p>
     */
    protected boolean exceedsTameCap(Player player) {
        return exceedsTameCap(this, player);
    }

    /**
     * Shared with {@link MoCAquatic}: legacy funnelled every tame through {@code MoCTools.tameWithName}, so the
     * per-player cap applied identically to land creatures, aquatics and monsters.
     */
    static boolean exceedsTameCap(net.minecraft.world.entity.Entity self, Player player) {
        MoCConfig config = MoCConfig.get();
        if (!config.enableOwnership) {
            return false;
        }
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        String ownerName = player.getName().getString();
        int owned = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof IMoCEntity moc && moc.getIsTamed()
                        && ownerName.equals(moc.getOwnerName())) {
                    owned++;
                }
            }
        }
        boolean op = player instanceof ServerPlayer sp
                && server.getPlayerList().isOp(sp.nameAndId());
        int cap = op ? config.maxOPTamed : config.maxTamed;
        if (owned >= cap) {
            player.sendSystemMessage(Component.literal("You have too many pets."));
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------------- riding controls

    @Override
    public net.minecraft.world.entity.@Nullable LivingEntity getControllingPassenger() {
        if (isRideable() && getFirstPassenger() instanceof Player player && getIsTamed()) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.4F;
        float forward = controller.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // ------------------------------------------------------------------------------------ damage

    /**
     * Ownership damage-immunity, ported from legacy {@code MoCEntityAnimal.attackEntityFrom}: with
     * {@link MoCConfig#enableOwnership} on, a tamed creature takes NO damage from any player other than
     * its owner. This is what stopped other players from harming (or, for a big cat, provoking) someone
     * else's pet; combined with the tamed big cat never retaliating against a player, it means the only
     * player who can hit a tamed cat is its owner — and even then the cat will not turn on them.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (MoCConfig.get().enableOwnership && getIsTamed()) {
            String owner = getOwnerName();
            if (owner != null && !owner.isEmpty() && source.getEntity() instanceof Player attacker
                    && !owner.equals(attacker.getName().getString())) {
                return false;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    // ------------------------------------------------------------------------ drops & baby scaling

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        MoCBehavior.dropLoot(this, level, behavior());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
        builder.define(TAMED, false);
        builder.define(ADULT, true);
        builder.define(AGE, 50);
        builder.define(OWNER, "");
        builder.define(SADDLED, false);
        builder.define(SITTING, false);
        builder.define(EATEN, false);
    }

    /** Legacy big-cat "hasEaten" prerequisite for medallion taming (set when a cub eats raw pork/fish). */
    public boolean getHasEatenMoC() {
        return this.entityData.get(EATEN);
    }

    public void setHasEatenMoC(boolean eaten) {
        this.entityData.set(EATEN, eaten);
    }

    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    // ---------------------------------------------------------------- keybind jump / dismount (legacy)
    private boolean jumpPending;

    @Override
    public void makeEntityJump() {
        this.jumpPending = true;
    }

    /** Upward velocity of a keybind-triggered mount jump; a horse-like mount may override higher. */
    protected double getCustomJump() {
        return 0.5D;
    }

    @Override
    public void dismountMoCEntity(net.minecraft.server.level.ServerLevel level) {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Consume a queued Jump-key press: a grounded mount springs upward (legacy makeEntityJump).
        if (this.jumpPending) {
            this.jumpPending = false;
            if (this.onGround()) {
                this.setDeltaMovement(getDeltaMovement().x, getCustomJump(), getDeltaMovement().z);
                this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
            }
        }
        tickBreakingIn();
    }

    /**
     * Legacy {@code MoCEntityAnimal.onLivingUpdate}:1096-1142 — breaking in a wild mount.
     *
     * <p>While an untamed ride-tameable creature (horse, wyvern) carries a rider it bucks: it hops, veers, and
     * on a 1-in-50 tick throws the rider off with its angry sound. Every tick it also rolls
     * {@code rand.nextInt((maxTemper - temper) * 8) == 0}; when that hits, the creature submits and is tamed.
     * Feeding it wheat / sugar lump / bread raises its temper and so shortens the odds. This is the ONLY way
     * legacy tamed a wyvern, and the ordinary way it tamed a wild horse.</p>
     */
    private void tickBreakingIn() {
        if (!(this.level() instanceof ServerLevel serverLevel) || getIsTamed() || !isVehicle()) {
            return;
        }
        if (!MoCBehavior.of(this).rideTames) {
            return;
        }
        if (!(getFirstPassenger() instanceof Player rider)) {
            return;
        }
        // Buck: a hop when grounded, and a sideways lurch.
        if (this.random.nextInt(5) == 0 && this.onGround()) {
            setDeltaMovement(getDeltaMovement().x, getCustomJump(), getDeltaMovement().z);
            this.hurtMarked = true;
        }
        if (this.random.nextInt(10) == 0) {
            setDeltaMovement(getDeltaMovement().add(this.random.nextDouble() / 30.0D, 0.0D,
                    this.random.nextDouble() / 10.0D));
            this.hurtMarked = true;
        }
        // Throw the rider off.
        if (this.random.nextInt(50) == 0) {
            net.minecraft.sounds.SoundEvent mad = getAngrySound();
            if (mad != null) {
                serverLevel.playSound(null, this, mad, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            rider.stopRiding();
            rider.setDeltaMovement(rider.getDeltaMovement().add(0.0D, 0.9D, -0.3D));
            rider.hurtMarked = true;
            return;
        }
        // Submit? The closer temper is to maxTemper, the shorter the odds.
        int chance = getMaxTemper() - getTemper();
        if (chance <= 0) {
            chance = 5;
        }
        // Legacy routed this through MoCTools.tameWithName, which enforces the per-player tamed cap like every
        // other taming path does.
        if (this.random.nextInt(chance * 8) == 0 && !exceedsTameCap(rider)) {
            setTamed(true);
            setOwnerName(rider.getName().getString());
            hearts(7);
        }
    }

    /** The sound this creature makes when it bucks; null for species with no angry sound. */
    protected net.minecraft.sounds.@Nullable SoundEvent getAngrySound() {
        return null;
    }

    /**
     * Legacy {@code getTemper()} — how far this creature has been won over. Only meaningful while untamed and
     * being broken in, or for the zebra's graduated feed taming.
     */
    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int temper) {
        this.temper = temper;
    }

    /**
     * Legacy {@code getMaxTemper()}: how hard this creature is to tame — higher is harder. Base 100, matching
     * {@code MoCEntityAnimal.getMaxTemper}; the zebra overrides it to 200.
     */
    public int getMaxTemper() {
        return 100;
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
    }

    // ------------------------------------------------------------------ shared Mo'Creatures state

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public int getTypeMoC() {
        return this.entityData.get(TYPE);
    }

    @Override
    public void setTypeMoC(int type) {
        this.entityData.set(TYPE, type);
    }

    @Override
    public boolean getIsTamed() {
        return this.entityData.get(TAMED);
    }

    @Override
    public void setTamed(boolean tamed) {
        this.entityData.set(TAMED, tamed);
    }

    /**
     * Mo'Creatures land animals persist exactly like vanilla ones ({@code Animal.removeWhenFarAway} is a hard
     * {@code false} in 26.2), rather than despawning like monsters.
     *
     * <p>This used to return {@code !getIsTamed()}, mirroring legacy {@code MoCEntityAnimal.canDespawn()}. That
     * was faithful to 1.12 but wrong for this port, because legacy paired it with its own CustomSpawner that
     * continuously re-seeded creatures around the player. Here creatures come from the vanilla pipeline, which
     * places CREATURE-category mobs almost entirely at chunk generation — so an untamed animal deleted at 128
     * blocks was never replaced. Measured on a fresh world: Mo'Creatures holds ~55% of the animal spawn weight
     * at generation, but only 16.6% of the animals actually alive in explored chunks (69 vs 347), because
     * vanilla cows and pigs stayed and every Mo'Creatures animal beyond the despawn radius was discarded. In
     * play that reads as "Mo'Creatures mobs don't spawn".</p>
     *
     * <p>Population is instead bounded by {@code MoCMobCap}, which is what it was written for: it trims untamed,
     * un-named, non-persistent Mo'Creatures entities down to {@code maxAnimals} per level and removes the ones
     * <em>farthest</em> from any player first, so the surviving population concentrates where it can be seen.
     * Aquatics ({@code MoCAquatic}) and monsters ({@code MoCMob}) keep {@code !getIsTamed()}, which is what
     * vanilla {@code WaterAnimal} and {@code Monster} do.</p>
     */
    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean getIsAdult() {
        return this.entityData.get(ADULT);
    }

    @Override
    public void setAdult(boolean adult) {
        this.entityData.set(ADULT, adult);
    }

    @Override
    public int getMoCAge() {
        return this.entityData.get(AGE);
    }

    @Override
    public void setMoCAge(int age) {
        this.entityData.set(AGE, age);
    }

    @Override
    public String getOwnerName() {
        return this.entityData.get(OWNER);
    }

    @Override
    public void setOwnerName(String name) {
        this.entityData.set(OWNER, name == null ? "" : name);
    }

    // ------------------------------------------------------------------------------ persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TypeMoC", getTypeMoC());
        output.putBoolean("Tamed", getIsTamed());
        output.putBoolean("Adult", getIsAdult());
        output.putInt("AgeMoC", getMoCAge());
        output.putString("OwnerName", getOwnerName());
        output.putBoolean("Saddled", isSaddled());
        output.putBoolean("Sitting", isSitting());
        output.putBoolean("HasEaten", getHasEatenMoC());
        output.putInt("Temper", getTemper());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setTypeMoC(input.getIntOr("TypeMoC", getTypeMoC()));
        setTamed(input.getBooleanOr("Tamed", false));
        setAdult(input.getBooleanOr("Adult", true));
        setMoCAge(input.getIntOr("AgeMoC", 50));
        setOwnerName(input.getStringOr("OwnerName", ""));
        setSaddled(input.getBooleanOr("Saddled", false));
        setSitting(input.getBooleanOr("Sitting", false));
        setHasEatenMoC(input.getBooleanOr("HasEaten", false));
        setTemper(input.getIntOr("Temper", 0));
    }

    // -------------------------------------------------------------------------- spawn / breeding

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Entity baby = this.getType().create(level, EntitySpawnReason.BREEDING);
        if (baby instanceof MoCAnimal moc) {
            moc.setAdult(false);
        }
        if (baby instanceof AgeableMob am) {
            am.setAge(-24000); // vanilla baby age -> renders small and grows up
        }
        return baby instanceof AgeableMob ageable ? ageable : null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return MoCBehavior.matches(behavior().foods, stack);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        selectType();
        return data;
    }

    /** Helper: builds an Identifier for an entity texture located under {@code textures/models/}. */
    protected static Identifier modelTexture(String file) {
        return Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "textures/models/" + file);
    }
}
