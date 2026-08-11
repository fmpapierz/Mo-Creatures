package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.IMoCManticore;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityManticorePet} — the TAMEABLE manticore: the same winged,
 * scorpion-tailed big cat as {@link drzhark.mocreatures.entity.monster.MoCEntityManticore}, but raised
 * from a cub instead of fought, and ridden through the air once it is grown.
 *
 * <p><b>Why this is a second class rather than a sub-type of the wild manticore.</b> The port's
 * merged-class convention ({@code MoCEntityRay} covering mantaray and stingray) merges variants that
 * share a base class. These two do not: 12.0.5 put the wild manticore on {@code MoCEntityMob} and the
 * pet on {@code MoCEntityBigCat extends MoCEntityTameableAnimal}, and this port keeps exactly that split
 * — hostile creatures extend {@link drzhark.mocreatures.entity.MoCMob} (vanilla {@code Monster}: monster
 * spawn category, despawns when far away, monster spawn placement) and passive ones extend
 * {@link MoCAnimal} (vanilla {@code Animal}, which is where taming, saddling, mounting, sitting,
 * ownership and the {@code MoCBehavior} interaction table all live). They are also registered as two
 * separate entity ids, exactly as legacy registered {@code manticore} and {@code manticorepet}. Merging
 * them would mean reimplementing one half's base class inside the other.
 *
 * <p><b>Why it does not extend {@link MoCEntityBigCat}, as legacy did.</b> The port's big cat drives its
 * stats from a private per-coat table ({@code applyTypeStats}, re-applied on every AI tick) keyed to the
 * seven feline coats, so a manticore pet inheriting it would be silently reset to lion/panther numbers
 * (25 hp, 5 damage) every tick instead of keeping the legacy manticore-pet overrides below —
 * {@code calculateMaxHealth() = 40}, {@code calculateAttackDmg() = 7}, {@code getAttackRange() = 8},
 * {@code getMaxEdad() = 130} ({@code MoCEntityManticorePet:93-116}). That table is private and this
 * port's split-file workflow forbids editing the shared big-cat class, so the manticore-relevant slice
 * of the big cat's behaviour (cub-eats-meat before it can be medallion-tamed, slow regeneration, the
 * bite that gapes the maw) is reproduced here directly.
 *
 * <p><b>What it does.</b> Four coats (1 red, 2 dark, 3 blue, 4 green — the same textures and the same
 * sting table as the wild form), a mane and sabre teeth always shown, wings that beat in flight and a
 * scorpion tail that strikes on roughly one bite in five. Legacy {@code hasMane()}/{@code hasSaberTeeth()}
 * /{@code getHasStinger()}/{@code isFlyer()} are all constant {@code true}, which is why they appear here
 * as geometry that is simply always present rather than as flags. It cannot breed at all (legacy
 * {@code compatibleMate}, {@code readytoBreed} and {@code getOffspringClazz} are hard {@code false}/empty),
 * so a manticore is only ever obtained from a wild one.
 */
public class MoCEntityManticorePet extends MoCAnimal implements IMoCManticore {

    /** Synched: a wing beat is in progress; the client sweeps the wings at full amplitude. */
    private static final EntityDataAccessor<Boolean> FLAPPING =
            SynchedEntityData.defineId(MoCEntityManticorePet.class, EntityDataSerializers.BOOLEAN);
    /** Synched: the sting is mid-strike (legacy {@code swingingTail()}), so the barb whips forward. */
    private static final EntityDataAccessor<Boolean> STINGING =
            SynchedEntityData.defineId(MoCEntityManticorePet.class, EntityDataSerializers.BOOLEAN);
    /** Synched: the maw is open (legacy {@code mouthCounter}), dropping the lower jaw. */
    private static final EntityDataAccessor<Boolean> OPEN_JAW =
            SynchedEntityData.defineId(MoCEntityManticorePet.class, EntityDataSerializers.BOOLEAN);

    /** Legacy {@code wingFlapCounter}: runs 1..20; the flap sound fires on tick 5 of the beat. */
    private int wingFlapCounter;
    /** Legacy {@code poisontimer}: runs 1..50 while a sting is being delivered. */
    private int poisonTimer;
    /** Legacy {@code isPoisoning}: a sting is in progress (server-side truth behind {@link #STINGING}). */
    private boolean poisoning;
    /** Legacy {@code mouthCounter}, collapsed to a countdown that closes the jaw again. */
    private int jawTicks;

    public MoCEntityManticorePet(EntityType<? extends MoCEntityManticorePet> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code MoCEntityManticorePet.calculateMaxHealth/calculateAttackDmg/getAttackRange} (40 / 7 / 8),
     * which the big cat pushed onto MAX_HEALTH, ATTACK_DAMAGE and FOLLOW_RANGE in {@code selectType}.
     * Movement speed is the big cat's {@code getMoveSpeed() = 1.6} put through the port's 0.2 scaling
     * convention (the same one {@code MoCEntityBigCat.applyTypeStats} and the dolphin use), i.e. 0.32.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAPPING, false);
        builder.define(STINGING, false);
        builder.define(OPEN_JAW, false);
    }

    // ------------------------------------------------------------------------------- coat / textures

    /** Legacy {@code selectType}: an even roll across all four coats ({@code rand.nextInt(4) + 1}). */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(4) + 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("bcmanticoredark.png");
            case 3 -> modelTexture("bcmanticoreblue.png");
            case 4 -> modelTexture("bcmanticoregreen.png");
            default -> modelTexture("bcmanticore.png"); // type 1 and fallback (legacy default)
        };
    }

    /**
     * Legacy big cats render at {@code getEdad() * 0.01F} and the manticore pet's {@code getMaxEdad()} is
     * 130, so a full-grown one is 1.3x the base model. The port's renderer already ramps a juvenile up with
     * its own age curve and then multiplies by this factor, so this only has to state the adult size.
     */
    @Override
    public float getSizeFactor() {
        return 1.3F;
    }

    // No fireImmune() override on purpose: legacy MoCEntityManticorePet does NOT set isImmuneToFire —
    // only the wild MoCEntityManticore's constructor does. Even a red (nether-coat) pet therefore burns
    // like any other animal. Recorded here because it is a real asymmetry between the two forms, not an
    // omission in this port.

    /** Legacy {@code fall()} on the flying big cat is an empty override: a flyer never breaks its legs. */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ----------------------------------------------------------------------------- animation plumbing

    @Override
    public boolean isWingFlapping() {
        return this.entityData.get(FLAPPING);
    }

    @Override
    public boolean isStingStriking() {
        return this.entityData.get(STINGING);
    }

    @Override
    public boolean getJawOpen() {
        return this.entityData.get(OPEN_JAW);
    }

    /** Legacy {@code openMouth()}: gapes the maw for a roar or a bite. Server-side only. */
    private void openJaw() {
        if (this.level().isClientSide()) {
            return;
        }
        this.jawTicks = 30; // legacy mouthCounter ran to 30
        if (!getJawOpen()) {
            this.entityData.set(OPEN_JAW, true);
        }
    }

    /** Legacy {@code wingFlap()}: starts a 20-tick beat. A beat already running is not restarted. */
    private void wingFlap() {
        if (this.wingFlapCounter == 0) {
            this.wingFlapCounter = 1;
            this.entityData.set(FLAPPING, true);
        }
    }

    /** Legacy {@code setPoisoning(true)}: begins the 50-tick sting cycle (its first 15 ticks are the strike). */
    private void startSting() {
        this.poisoning = true;
        this.poisonTimer = 0;
        this.entityData.set(STINGING, true);
    }

    /**
     * Legacy {@code MoCEntityBigCat.makeEntityJump}: a flying big cat beats its wings as it springs, so the
     * Jump keybind both flaps and hops.
     */
    @Override
    public void makeEntityJump() {
        wingFlap();
        super.makeEntityJump();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Poisoning", this.poisoning);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.poisoning = input.getBooleanOr("Poisoning", false);
    }

    // ------------------------------------------------------------------------------------- AI tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // --- jaw / wing beat / sting cycle: identical timing to the wild form ---
        if (this.jawTicks > 0 && --this.jawTicks == 0 && getJawOpen()) {
            this.entityData.set(OPEN_JAW, false);
        }
        if (this.wingFlapCounter > 0 && ++this.wingFlapCounter > 20) {
            this.wingFlapCounter = 0;
            this.entityData.set(FLAPPING, false);
        }
        if (this.wingFlapCounter == 5) {
            level.playSound(null, blockPosition(), MoCSounds.WINGFLAP.get(), SoundSource.NEUTRAL, 1.0F,
                    1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
        }
        // Legacy MoCEntityBigCat.onLivingUpdate:336-345: a flyer off the ground beats its wings on a
        // speed-derived interval — as often as 1-in-5 ticks, and never rarer than that while carrying a
        // rider. Grounded, it just stretches them now and again.
        if (!this.onGround() && this.random.nextInt(5) == 0) {
            wingFlap();
        } else if (this.onGround() && this.random.nextInt(500) == 0) {
            wingFlap();
        }
        if (this.poisoning) {
            this.poisonTimer++;
            if (this.poisonTimer == 1) {
                level.playSound(null, blockPosition(), MoCSounds.SCORPIONSTING.get(), SoundSource.NEUTRAL,
                        1.0F, 1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
            }
            if (this.poisonTimer >= 15 && isStingStriking()) {
                this.entityData.set(STINGING, false); // legacy swingingTail(): strike is the first 15 ticks
            }
            if (this.poisonTimer > 50) {
                this.poisonTimer = 0;
                this.poisoning = false;
            }
        }

        // Legacy MoCEntityBigCat.onLivingUpdate: a slow trickle of regeneration, 1 hp per ~300 ticks.
        if (this.random.nextInt(300) == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }

        // Gliding descent for an unridden manticore (the ridden one is flown in travel()): a winged cat
        // parachutes rather than plummets. This is legacy MoCEntityManticore's `motionY *= 0.6D` slow-fall.
        if (!this.onGround() && !this.isVehicle()) {
            Vec3 dm = getDeltaMovement();
            if (dm.y < 0.0D) {
                setDeltaMovement(dm.x, dm.y * 0.6D, dm.z);
            }
        }

        tickCubFeeding(level);
    }

    /**
     * Legacy {@code MoCEntityBigCat.onLivingUpdate}:503-520, the FIRST half of big-cat taming: a wild cat
     * walks to a raw porkchop or raw fish lying on the ground, eats it, heals to full and sets the "has
     * eaten" flag — and only a cub that has eaten can then be tamed with a Medallion
     * ({@code MoCEntityBigCat.processInteract}:466 requires {@code getHasEaten() && !getIsAdult()}). Without
     * this loop a manticore pet could never be tamed at all. Mirrors the port's big-cat implementation.
     */
    private void tickCubFeeding(ServerLevel level) {
        boolean wantsFood = getHealth() < getMaxHealth() || (!getIsAdult() && !getHasEatenMoC());
        if (!wantsFood || this.random.nextInt(20) != 0 || isSitting() || isVehicle()) {
            return;
        }
        ItemEntity food = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(12.0D),
                e -> e.isAlive() && (e.getItem().is(Items.PORKCHOP) || e.getItem().is(Items.COD)))) {
            double d = ie.distanceToSqr(this);
            if (d < best) {
                best = d;
                food = ie;
            }
        }
        if (food == null) {
            return;
        }
        if (best < 2.25D) {
            food.getItem().shrink(1);
            if (food.getItem().isEmpty()) {
                food.discard();
            }
            setHealth(getMaxHealth());
            if (!getIsAdult()) {
                setHasEatenMoC(true);
            }
            level.playSound(null, blockPosition(), MoCSounds.EATING.get(), SoundSource.NEUTRAL, 1.0F,
                    1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
        } else {
            this.getNavigation().moveTo(food, 1.2D);
        }
    }

    /** Legacy medallion taming needs a cub that has already eaten — see {@link #tickCubFeeding}. */
    @Override
    protected boolean requiresFeedingBeforeTaming() {
        return true;
    }

    // -------------------------------------------------------------------------------- ridden flight

    /**
     * Rider-steered flight, modelled on the port's winged horse ({@code MoCEntityHorse.travel}) rather than
     * on the wyvern's move-control rewrite, because the pegasus solves exactly this problem: a big creature
     * that walks normally with ordinary ground pathing and takes to the air only under a rider. Pitch sets
     * altitude (look up to climb or take off, down to dive), W/S drive forward and brake, A/D strafe, and no
     * input at all leaves the manticore hovering with a slow sink. Legacy reached the same result through
     * {@code MoCEntityAnimal}'s flyer branch plus {@code getCustomSpeed() = 2.0}; here the speed comes off
     * MOVEMENT_SPEED so the ground gait and the flight stay in proportion.
     */
    @Override
    public void travel(Vec3 input) {
        if (this.isVehicle() && getControllingPassenger() instanceof Player rider
                && (!this.onGround() || rider.getXRot() < -25.0F)) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setNoGravity(true);
            float fwd = rider.zza;
            float str = rider.xxa;
            float speed = (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.2D);
            if (fwd != 0.0F || str != 0.0F) {
                Vec3 look = rider.getLookAngle();
                double fwdScale = fwd > 0.0F ? speed : (fwd < 0.0F ? -speed * 0.4D : 0.0D);
                Vec3 forward = look.scale(fwdScale);
                Vec3 side = new Vec3(look.z, 0.0D, -look.x).normalize().scale(str * speed * 0.5D);
                Vec3 desired = forward.add(side);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(desired.scale(0.5D)));
            } else {
                Vec3 dm = this.getDeltaMovement();
                this.setDeltaMovement(dm.x * 0.7D, Math.max(dm.y * 0.8D, -0.04D), dm.z * 0.7D);
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            // Beat the wings while actually flying, so the mount looks like it is holding itself up.
            if (!this.level().isClientSide() && this.random.nextInt(8) == 0) {
                wingFlap();
            }
        } else {
            this.setNoGravity(false); // also clears the flight no-gravity on landing
            super.travel(input);
        }
    }

    // -------------------------------------------------------------------------------------- combat

    /**
     * The bite, plus the scorpion sting. Legacy routed the sting through {@code applyEnchantments}, called
     * after every landed melee hit: roughly one hit in five (and never while a sting is still running)
     * delivers the coat's venom — green/dark POISON, blue SLOWNESS, red fire — and every other hit merely
     * bares the teeth. The base {@link MoCAnimal} hit already applies the 7-damage ATTACK_DAMAGE.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (!hit) {
            return false;
        }
        openJaw(); // the maw gapes on the bite itself (legacy openMouth)
        if (target instanceof LivingEntity victim && !this.poisoning && this.random.nextInt(5) == 0) {
            startSting();
            switch (getTypeMoC()) {
                case 3 -> victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0), this);
                case 1 -> {
                    // Legacy skipped the burn inside the Nether, where it is no threat.
                    if (this.level().dimension() != Level.NETHER) {
                        victim.igniteForSeconds(15.0F);
                    }
                }
                default -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0), this);
            }
        }
        return hit;
    }

    // --------------------------------------------------------------------------------- interaction

    /**
     * Two legacy gates that the shared {@link MoCAnimal} interaction table cannot express, both from
     * {@code MoCEntityBigCat}/{@code MoCEntityManticorePet}:
     *
     * <ul>
     *   <li>SADDLING requires a TAMED cat past {@code edad > 80} ({@code MoCEntityBigCat.processInteract}
     *       :507) — a cub cannot be tacked up.</li>
     *   <li>MOUNTING requires {@code getIsAdult()} ({@code MoCEntityManticorePet.processInteract}:60).</li>
     * </ul>
     *
     * Both are refused here before the base class sees them, so the saddle is never consumed and the
     * interaction falls through to whatever else the player intended.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean saddleInHand = stack.is(Items.SADDLE) || stack.is(MoCItems.HORSESADDLE.get());
        if (saddleInHand && !isSaddled() && (!getIsTamed() || getMoCAge() <= 80)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty() && !this.isVehicle() && getIsTamed() && isSaddled() && !getIsAdult()) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    // -------------------------------------------------------------------------------------- sounds
    // Legacy reused the lion cries for both manticore forms, with the cub cries while it is small.

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        openJaw();
        return getIsAdult() ? MoCSounds.LIONGRUNT.get() : MoCSounds.CUBGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        openJaw();
        return getIsAdult() ? MoCSounds.LIONHURT.get() : MoCSounds.CUBHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        openJaw();
        return getIsAdult() ? MoCSounds.LIONDEATH.get() : MoCSounds.CUBDYING.get();
    }

    // --------------------------------------------------------------------------------------- drops

    /**
     * Legacy {@code MoCEntityBigCat.dropMyStuff} handed the saddle back when the cat died, and the port's
     * big cat additionally returns the Medallion that was spent taming it, so the owner is not permanently
     * out of pocket. The coat-agnostic claw drop comes from the {@code MoCBehavior} spec, exactly as the
     * big cat's does. (Legacy's chest/armour returns have no counterpart: this port fits no storage chest
     * or barding to a big cat.)
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        if (isSaddled()) {
            spawnAtLocation(level, new ItemStack(MoCItems.HORSESADDLE.get()));
            setSaddled(false);
        }
        if (getIsTamed()) {
            spawnAtLocation(level, new ItemStack(MoCItems.MEDALLION.get(), 1));
        }
    }

    /**
     * Scroll-of-Freedom release hook, matching {@code MoCEntityBigCat.dropWornGear}. Deliberately empty: the
     * only gear a manticore pet ever wears is its saddle, and {@code MoCScrollItem.dropMyStuff} already hands
     * that back generically for any saddled {@link MoCAnimal} before the per-species hook runs, so dropping it
     * again here would be a duplicate. Kept so the scroll's per-species dispatch has something to call.
     */
    public void dropWornGear(ServerLevel level) {
        // no-op — mirrors MoCEntityBigCat.dropWornGear (legacy MoCEntityBigCat.dropMyStuff released no extras
        // beyond the saddle/chest the shared code already covers).
    }
}
