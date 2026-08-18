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

    // ------------------------------------------------------------------------- pick up / carry a pet

    /**
     * The network id of the player carrying this creature, or -1. Synched so the client can position and
     * pose the creature on its carrier's head.
     *
     * <p><b>Why this is not the vanilla riding system.</b> Legacy did {@code mountEntity(entityplayer)} —
     * the pet literally became the player's passenger. That is impossible on a 26.2 server:
     * {@code Entity.startRiding} bails out at {@code !entityToRide.type.canSerialize()}, and
     * {@code EntityType.PLAYER} is built with {@code .noSave()}, so {@code canSerialize()} is false and a
     * mob can NEVER be a passenger of a player. The check sits above the {@code force} branch, so
     * {@code startRiding(player, true, true)} does not get around it either. Carrying is therefore driven
     * by this field instead: the creature stays an ordinary world entity and is simply pinned to the
     * carrier's head each tick — which, as a bonus, means it saves and loads normally rather than being
     * silently discarded the way a passenger of a non-serializable vehicle is.</p>
     */
    private static final EntityDataAccessor<Integer> CARRIER =
            SynchedEntityData.defineId(MoCAnimal.class, EntityDataSerializers.INT);

    /**
     * The persisted claim on this creature: the UUID of the player carrying it, or {@code null}.
     *
     * <p>{@link #CARRIER} holds a network entity id, which is only meaningful for as long as that player
     * is online — it is reassigned on every join and is useless across a save. So the id is treated as a
     * live cache and this UUID is the durable record: on reload (or when the carrier logs back in) the
     * creature re-attaches itself to the same player, and you get your pet back on your head instead of
     * finding it on the floor where you left the world.</p>
     */
    private java.util.@Nullable UUID carrierUuid;

    /** True while this creature is being carried by a player (legacy {@code pickedUp}). */
    public boolean isBeingCarried() {
        return this.entityData.get(CARRIER) != -1;
    }

    /** The player carrying this creature, or {@code null}. Resolved from the synched carrier id. */
    public @Nullable Player getCarrier() {
        int id = this.entityData.get(CARRIER);
        return id != -1 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    /**
     * Re-points the live carrier id at the claimed player, or clears it while they are offline. Server
     * side; run every tick so a carrier who logs back in (or reloads the world) picks their pet straight
     * back up.
     */
    private void refreshCarrierLink() {
        if (this.carrierUuid == null) {
            if (this.entityData.get(CARRIER) != -1) {
                this.entityData.set(CARRIER, -1);
            }
            return;
        }
        Player claimed = this.level().getPlayerByUUID(this.carrierUuid);
        int live = claimed != null && claimed.isAlive() && !claimed.isRemoved() ? claimed.getId() : -1;
        if (this.entityData.get(CARRIER) != live) {
            this.entityData.set(CARRIER, live);
            if (live == -1) {
                this.setNoGravity(false); // carrier offline: let it rest on the ground until they return
            }
        }
    }

    /** The sound made when the creature is set down; legacy used {@code mob.chickenplop} for most species. */
    protected net.minecraft.sounds.@Nullable SoundEvent getPutDownSound() {
        return net.minecraft.sounds.SoundEvents.CHICKEN_EGG;
    }

    /**
     * How far below the top of the carrier's head the creature sits, so it nestles rather than floats.
     * Public because {@code MoCMobRenderer} rebuilds the same pin point per FRAME (via
     * {@code EntityRenderState.passengerOffset}) to keep the pet glued to the carrier between ticks.
     */
    public double carriedHeadSink() {
        return 0.15D;
    }

    /**
     * Legacy pick-up / put-down toggle ({@code MoCEntityBunny.interact}). Right-click a small creature to
     * carry it on your head; right-click again — or sneak — to set it down, and if you are running or
     * jumping as you do, to throw it, since legacy applied the carrier's momentum at 5x on release.
     * Server-side only; returns {@code true} when the interaction was consumed.
     */
    protected boolean toggleCarry(Player player, boolean tameOnPickup) {
        if (isBeingCarried()) {
            if (getCarrier() != player) {
                return false;
            }
            putDown(player);
            return true;
        }
        // One pet at a time, and never off another creature's back.
        if (this.isVehicle() || this.isPassenger() || carriedBySomeone(player)) {
            return false;
        }
        if (tameOnPickup && !getIsTamed()) {
            if (exceedsTameCap(player)) {
                return true; // consumed, but refused
            }
            setTamed(true);
            setOwnerName(player.getName().getString());
            // Legacy tameWithName prompted for a name the instant a creature was tamed.
            drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
            hearts(7);
        }
        // Legacy snapped the pet to the carrier's facing on pick-up (MoCEntityBunny:161).
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setYBodyRot(player.getYRot());
        this.setYHeadRot(player.getYRot());
        this.carrierUuid = player.getUUID();
        this.entityData.set(CARRIER, player.getId());
        this.getNavigation().stop();
        return true;
    }

    /** Whether {@code player} is already carrying some other Mo'Creature. */
    private boolean carriedBySomeone(Player player) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(3.0D))) {
            if (entity != this && entity instanceof MoCAnimal moc && moc.getCarrier() == player) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the creature down, applying the legacy release impulse — the carrier's momentum at 5x, so
     * dropping one while sprinting or jumping hurls it (legacy {@code MoCEntityBunny}:187-189).
     */
    public void putDown(@Nullable Player carrier) {
        this.carrierUuid = null;
        this.entityData.set(CARRIER, -1);
        this.setNoGravity(false);
        if (carrier != null) {
            Vec3 pv = carrier.getDeltaMovement();
            this.setDeltaMovement(pv.x * 5.0D, (pv.y / 2.0D) + 0.5D, pv.z * 5.0D);
            this.hurtMarked = true; // 26.2 syncs impulses via hurtMarked, not hasImpulse
        }
        net.minecraft.sounds.SoundEvent sound = getPutDownSound();
        if (sound != null && !this.level().isClientSide()) {
            this.level().playSound(null, this.blockPosition(), sound,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
        }
    }

    /**
     * Pins a carried creature to its carrier's head, on both sides so it does not jitter. Runs from
     * {@link #tick()} before the AI so the creature never fights the position. Sneaking sets it down —
     * without that there is no way to release one in first person, where it is deliberately not rendered.
     */
    private void tickCarried() {
        if (!this.level().isClientSide()) {
            refreshCarrierLink();
        }
        Player carrier = getCarrier();
        if (carrier == null) {
            // Either nothing is carrying this creature, or its carrier is offline. In the second case the
            // claim is KEPT: quitting to the title screen and coming back must hand the pet straight back,
            // not leave it on the floor. refreshCarrierLink re-establishes the link when they return.
            return;
        }
        if (!this.level().isClientSide() && carrier.isShiftKeyDown()) {
            putDown(carrier);
            return;
        }
        // On the client the server is ALSO streaming this creature's position and rotation, and
        // LivingEntity.aiStep replays those packets through its InterpolationHandler
        // (mc262-ref LivingEntity.java:3018-3020). Two authorities disagreeing every time a packet lands is
        // an irregular wobble on top of everything else, so the replay is cancelled while carried: the pin
        // below is the only thing allowed to place a carried pet.
        if (this.level().isClientSide() && this.getInterpolation().hasActiveInterpolation()) {
            this.getInterpolation().cancel();
        }
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        this.setPos(carrier.getX(),
                carrier.getY() + carrier.getBbHeight() - carriedHeadSink(),
                carrier.getZ());
        // Face the carried yaw (legacy re-synced this every tick for the mouse). WHICH of the carrier's
        // yaws that is depends on the pose — see carriedYaw(): head-carried pets copy the LOOK yaw, the
        // kitty's side/shoulder poses copy the BODY yaw so they stay glued to the hip while the camera pans.
        //
        // Deliberately NOT touching yRotO / yBodyRotO / yHeadRotO. The renderer draws the creature at
        // Mth.rotLerp(partialTick, <field>O, <field>) — yHeadRot at
        // mc262-ref LivingEntityRenderer.java:247, yBodyRot at :323 — and those *O fields hold the previous
        // tick's value, captured by Entity.setOldPosAndRot() (Entity.java:1802) which both
        // ServerLevel.java:827 and ClientLevel.java:471 call BEFORE ticking the entity. Assigning the new
        // yaw over yRotO collapsed that interval to zero, so the creature snapped once per tick at 20 Hz
        // while the camera turned at frame rate — the reported left/right jitter. Leaving them alone is
        // what makes the turn interpolate.
        float carriedYaw = carriedYaw(carrier);
        this.setYRot(carriedYaw);
        this.setYBodyRot(carriedYaw);
        this.setYHeadRot(carriedYaw);
        if (!this.level().isClientSide()) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    /**
     * The yaw (degrees) a carried pet copies from its carrier each tick. Head-carried pets follow the
     * LOOK yaw ({@code getYRot}) — a bunny on your head turns with your face. Side-carried pets (the
     * kitty's dangling and shoulder poses) override this to the carrier's BODY yaw ({@code yBodyRot})
     * instead: the body yaw only moves when the body actually turns — vanilla eases it toward the
     * movement direction, and only drags it after a head turn beyond ~50 degrees
     * (mc262-ref LivingEntity.tickHeadTurn, LivingEntity.java:3000-3008) — while the camera pans freely,
     * so a side-carried pet stays glued to the hip instead of orbiting the player with the camera.
     * {@code yBodyRot} is maintained on the client for both the local player (computed in aiStep) and
     * remote players (lerped from network state), so the same field is valid on both sides of this tick.
     */
    protected float carriedYaw(Player carrier) {
        return carrier.getYRot();
    }

    /** A carried pet is baggage: it must not shove its carrier around. */
    @Override
    public boolean isPushable() {
        return !isBeingCarried() && super.isPushable();
    }

    /**
     * The other half of "baggage does not shove": {@link #isPushable} only stops others from pushing the
     * PET — {@code EntitySelector.pushableBy} (mc262-ref EntitySelector.java:36) filters it out of their
     * push lists — but the pet's own {@code pushEntities} still collects the carrier, and inside
     * {@code Entity.push} each side is gated on its OWN {@code isPushable()} (mc262-ref
     * Entity.java:1882-1888), so the CARRIER (a pushable player) got shoved away from the pet. Because the
     * carry pin runs after {@code super.tick()} (see {@link #tick()}), the pet sits at the carrier's
     * LAST-tick position throughout {@code aiStep}: whenever the carrier moves, that offset exceeds the
     * 0.01 dead-zone and the shove fires every tick — in the direction of travel, re-fed each tick by the
     * drift it causes — which read as the carrier sliding on ice for as long as the pet was carried.
     */
    @Override
    protected void doPush(Entity entity) {
        if (isBeingCarried()) {
            return;
        }
        super.doPush(entity);
    }

    /**
     * The carry pin runs AFTER the superclass tick, and that ordering matters.
     *
     * <p>{@code LivingEntity.aiStep} eases the body rotation toward the AI's target every tick —
     * {@code tickHeadTurn} (mc262-ref LivingEntity.java:3000-3008) moves {@code yBodyRot} 30% of the way
     * there and then clamps the head to within 50 degrees of it — and it replays server position packets
     * through the interpolation handler (:3018-3020). Pinning before {@code super.tick()} therefore had the
     * superclass drag the creature part of the way back every single tick, which is a second source of the
     * stutter on top of the destroyed rotation interpolation. Placing it last makes the pin the final word
     * on where a carried pet is and which way it faces.</p>
     */
    @Override
    public void tick() {
        super.tick();
        tickCarried();
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
                toggleCarry(player, true);
            }
            return InteractionResult.SUCCESS;
        }
        // PICKUP_TAMED: carry an already-TAMED creature on the player's head, but NEVER wild-tame by right-click
        // (legacy egg-hatched tamed snakes are carriable, yet a wild snake's right-click does nothing). A wild
        // one falls through so its heal/name interactions still run; only empty-hand carry is gated on tamed.
        if (spec.tame == MoCBehavior.Tame.PICKUP_TAMED && stack.isEmpty() && getIsTamed()) {
            if (server) {
                toggleCarry(player, false);
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
                // Legacy tameWithName prompted for a name the instant a creature was tamed.
                drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
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
        // The same two-stage rule applies to the kitty: legacy MoCEntityKitty.interact:615 only accepted a
        // medallion at kittyState 2, which a wild kitty reaches by finding and eating a dropped cooked fish
        // (legacy :789-820). Both medallion species must therefore have eaten first.
        if (spec.tame == MoCBehavior.Tame.MEDALLION && !getIsTamed()
                && stack.is(MoCItems.MEDALLION.get()) && !stack.has(DataComponents.CUSTOM_NAME)
                && (!requiresFeedingBeforeTaming() || getHasEatenMoC())) {
            if (server) {
                // Enforce the tamed-per-player cap; refuse without consuming the medallion.
                if (exceedsTameCap(player)) {
                    return InteractionResult.SUCCESS;
                }
                consume(player, stack);
                setTamed(true);
                setOwnerName(player.getName().getString());
                // Legacy tameWithName prompted for a name the instant a creature was tamed.
                drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
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
     * Legacy ownership lockout ({@code MoCEntityAnimal.interact}:491-495, {@code MoCEntityAquatic}:1046,
     * {@code MoCEntityKitty}:612): with {@link MoCConfig#enableOwnership} on, only the owner may interact
     * with a tamed creature at all. Without it any player can feed, heal, saddle, re-equip, mount or ride
     * someone else's horse, big cat, elephant or wyvern — the port already blocked them from HURTING it.
     */
    static boolean canBeHandledBy(IMoCEntity moc, Player player) {
        if (!MoCConfig.get().enableOwnership || !moc.getIsTamed()) {
            return true;
        }
        String owner = moc.getOwnerName();
        return owner == null || owner.isEmpty() || owner.equals(player.getName().getString());
    }

    protected boolean canBeHandledBy(Player player) {
        return canBeHandledBy(this, player);
    }

    /**
     * The lockout is applied at {@code interact}, not {@code mobInteract}, so it covers every subclass —
     * a dozen species override {@code mobInteract} and act before delegating to super, and a check placed
     * there would let each of them slip through.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!canBeHandledBy(player)) {
            return InteractionResult.PASS; // someone else's pet — hands off
        }
        return super.interact(player, hand, location);
    }

    /**
     * Shared with {@link MoCAquatic}: legacy funnelled every tame through {@code MoCTools.tameWithName}, so the
     * per-player cap applied identically to land creatures, aquatics and monsters.
     */
    public static boolean exceedsTameCap(net.minecraft.world.entity.Entity self, Player player) {
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
        builder.define(CARRIER, -1);
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
            // Legacy tameWithName prompted for a name the instant a creature was tamed.
            drzhark.mocreatures.network.MoCNetwork.promptName(this, rider);
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
        if (this.carrierUuid != null) {
            output.store("CarriedBy", net.minecraft.core.UUIDUtil.CODEC, this.carrierUuid);
        }
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
        this.carrierUuid = input.read("CarriedBy", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    // -------------------------------------------------------------------------- spawn / breeding

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Entity baby = this.getType().create(level, EntitySpawnReason.BREEDING);
        if (baby instanceof MoCAnimal moc) {
            moc.setAdult(false);
            // Newborns start at the bottom of their species' growth curve, not the shared default of 50 —
            // otherwise every baby is born half-grown and matures in half the intended time.
            moc.setMoCAge(newbornAge());
            // EntityType.create does not run finalizeSpawn, so selectType() never fires for a bred baby and
            // it would keep sub-type 0 (which renders as the fallback texture). Inherit the mother's coat
            // where the species breeds true, otherwise roll a fresh one.
            if (inheritsParentType()) {
                moc.setTypeMoC(this.getTypeMoC());
            } else {
                moc.selectType();
            }
        }
        if (baby instanceof AgeableMob am) {
            am.setAge(-24000); // vanilla baby age -> renders small and grows up
        }
        return baby instanceof AgeableMob ageable ? ageable : null;
    }

    /**
     * Whether a Medallion alone is not enough to tame this creature — legacy gated both medallion species
     * behind a first stage in which the wild animal has to find and eat food dropped on the ground (a big
     * cat cub eats raw pork/fish, a kitty eats a cooked fish), recorded in the shared {@code EATEN} flag.
     */
    protected boolean requiresFeedingBeforeTaming() {
        return false;
    }

    /** The MoC age a newborn of this species starts at (legacy: kitty 40, fishy 20, dolphin 35, horse 1). */
    protected int newbornAge() {
        return 1;
    }

    /** Whether a newborn takes its parent's sub-type rather than rolling a fresh one. */
    protected boolean inheritsParentType() {
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return MoCBehavior.matches(behavior().foods, stack);
    }

    /**
     * Legacy per-species growth ({@code edad}). Runs for every creature whose {@link MoCBehavior} spec
     * declares a curve; species that grow themselves (bunny, deer, big cat, boar, komodo, shark, wyvern,
     * horse, dolphin) declare none and keep their own tick.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        MoCBehavior.tickGrowth(this, this.random, behavior());
    }

    /**
     * Vanilla-age safety net: babies are given {@code setAge(-24000)} in several places, and when that
     * counter climbs back to zero vanilla considers them grown. Without this the MoC {@code ADULT} flag
     * would stay false forever, leaving the creature stuck at 0.75x with every {@code getIsAdult()} gate
     * shut.
     *
     * <p><b>The direction check is essential.</b> {@code AgeableMob.setAge} (mc262-ref
     * AgeableMob.java:147-153) fires this hook on BOTH boundary crossings — growing up
     * ({@code oldAge < 0 && newAge >= 0}) <em>and</em> becoming a baby ({@code oldAge >= 0 && newAge < 0}).
     * A newborn is built at the default age of 0 and then given {@code setAge(-24000)}, which is the second
     * of those. Reacting to that crossing made every bred baby of every species an instant MoC adult:
     * full-size, unable to grow, and immediately re-breedable. So only the grown-up direction counts.</p>
     */
    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.level().isClientSide() && this.getAge() >= 0 && !getIsAdult()) {
            setAdult(true);
            MoCBehavior.Spec spec = behavior();
            if (getMoCAge() < spec.adultAge) {
                setMoCAge(spec.adultAge);
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        // Legacy applied its spawn age / baby roll in the constructor, i.e. only to creatures that came from
        // the world. A bred foal, an egg hatchling or a released amulet pet sets its own age, so skip those.
        if (spawnReason != EntitySpawnReason.BREEDING) {
            MoCBehavior.applySpawnAge(this, this.random, behavior());
        }
        selectType();
        return data;
    }

    /** Helper: builds an Identifier for an entity texture located under {@code textures/models/}. */
    protected static Identifier modelTexture(String file) {
        return Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "textures/models/" + file);
    }
}
