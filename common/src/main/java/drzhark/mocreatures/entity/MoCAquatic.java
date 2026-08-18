package drzhark.mocreatures.entity;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base class for aquatic Mo'Creatures (dolphin, shark, fishy, ray, jellyfish) on 26.2 / NeoForge —
 * the modern equivalent of the legacy {@code MoCEntityAquatic}. Extends vanilla {@link WaterAnimal}
 * (water-bound navigation) and carries the shared Mo'Creatures type / tamed / owner state.
 */
public abstract class MoCAquatic extends WaterAnimal implements IMoCEntity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(MoCAquatic.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED =
            SynchedEntityData.defineId(MoCAquatic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ADULT =
            SynchedEntityData.defineId(MoCAquatic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(MoCAquatic.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER =
            SynchedEntityData.defineId(MoCAquatic.class, EntityDataSerializers.STRING);

    protected MoCAquatic(EntityType<? extends MoCAquatic> type, Level level) {
        super(type, level);
        // A WaterBoundPathNavigation path is only followable with a swim-aware move control — see
        // AquaticMoveControl below for why the default Mob control strands every aquatic at depth.
        this.moveControl = new AquaticMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Interval 40 matches vanilla's own fish (AbstractFish.FishSwimGoal:200 — RandomSwimmingGoal(fish,
        // 1.0, 40)) and is closer to legacy's EntityAIWanderMoC2 intervals (50-80). The 10 this used to be
        // re-rolled a fresh wander target every ~5 ticks (Goal.adjustedTickDelay halves it), which mattered
        // for fishing: it re-stole the navigation almost immediately every time the bobber-approach in
        // customServerAiStep parked a fish near a hook, so the 1-in-30 bite roll below never got to sample
        // the fish while it was still close.
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 40));
        // Wild aquatic predators (untamed adults) hunt players and retaliate when struck.
        if (MoCBehavior.of(this).wildHostile) {
            this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4D, true));
            this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.player.Player.class, 10, true, false,
                    (living, serverLevel) -> !this.getIsTamed() && this.getIsAdult()));
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    /**
     * Legacy ownership lockout ({@code MoCEntityAquatic.interact}:1046): with ownership enforced, only the
     * owner may handle a tamed dolphin or ray at all. Applied at {@code interact} so subclasses that
     * override {@code mobInteract} cannot slip past it.
     */
    @Override
    public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
        if (!MoCAnimal.canBeHandledBy(this, player)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        MoCBehavior.Spec spec = MoCBehavior.of(this);
        final boolean server = !this.level().isClientSide();
        // Tame a dolphin by feeding it an apple / golden apple.
        if (spec.tame == MoCBehavior.Tame.FEED && !getIsTamed() && MoCBehavior.matches(spec.foods, stack)) {
            if (server) {
                if (MoCAnimal.exceedsTameCap(this, player)) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
                if (!player.getAbilities().instabuild) stack.shrink(1);
                setTamed(true);
                setOwnerName(player.getName().getString());
                // Legacy tameWithName prompted for a name the instant a creature was tamed.
                drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
                heal(getMaxHealth());
                aquaHearts();
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Feed its heal food to a hurt aquatic.
        if (getHealth() < getMaxHealth() && MoCBehavior.matches(spec.healOrFood(), stack)) {
            if (server) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                heal(fishHealAmount(stack));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Ride a rideable aquatic (dolphin / manta ray) with an empty hand. Legacy puts NO tamed gate here:
        // MoCEntityRay.interact:99 mounts a type-1 mantaray outright (rays are not tameable at all), and
        // MoCEntityDolphin.interact:405 mounts a wild dolphin so it can buck you until it submits. Steering is
        // what requires taming — see getControllingPassenger.
        if (spec.rideable && stack.isEmpty() && !this.isVehicle()) {
            if (server) player.startRiding(this);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Swallow attraction food so the client doesn't predict eating it.
        if (!stack.isEmpty() && MoCBehavior.matches(spec.foods, stack)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Health restored by a single heal-food feed. The generic aquatic heals a flat 15% of its max health
     * (min 2). The dolphin is faithful to the legacy {@code MoCEntityDolphin.interact()} amounts instead:
     * feeding raw fish restores 15 HP, and feeding cooked fish to a tamed adult restores 25 HP (both capped
     * by {@link #heal}). This heals a hurt 30-HP dolphin roughly half-to-fully per fish, as in 1.12.2 —
     * rather than the ~4.5 HP the flat 15% formula gave. Guarded on the dolphin so other aquatics are
     * unaffected (they are the only heal-food carrier anyway).
     */
    protected float fishHealAmount(net.minecraft.world.item.ItemStack stack) {
        if (this instanceof drzhark.mocreatures.entity.passive.MoCEntityDolphin) {
            return (stack.is(net.minecraft.world.item.Items.COOKED_COD) && getIsTamed() && getIsAdult())
                    ? 25.0F : 15.0F;
        }
        return Math.max(2.0F, getMaxHealth() * 0.15F);
    }

    private void aquaHearts() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(),
                    getY() + getBbHeight() * 0.5D, getZ(), 6, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    @Override
    public net.minecraft.world.entity.@Nullable LivingEntity getControllingPassenger() {
        // A creature that has to be broken in (the dolphin) only answers to the reins once tamed; one that was
        // never tameable in the first place (the mantaray) steers as soon as you are on it, as legacy did.
        MoCBehavior.Spec spec = MoCBehavior.of(this);
        if (spec.rideable && (getIsTamed() || !spec.rideTames)
                && getFirstPassenger() instanceof net.minecraft.world.entity.player.Player p) {
            return p;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(net.minecraft.world.entity.player.Player controller, net.minecraft.world.phys.Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
    }

    @Override
    protected net.minecraft.world.phys.Vec3 getRiddenInput(net.minecraft.world.entity.player.Player controller,
            net.minecraft.world.phys.Vec3 selfInput) {
        // Allow the rider to steer the swim, including up/down with the look pitch.
        float forward = controller.zza;
        float strafe = controller.xxa * 0.5F;
        double vertical = -controller.getXRot() * 0.02D * forward;
        return new net.minecraft.world.phys.Vec3(strafe, vertical, forward);
    }

    @Override
    protected float getRiddenSpeed(net.minecraft.world.entity.player.Player controller) {
        // Water drag heavily damps movement, so ridden aquatics get a strong speed boost to feel responsive.
        return Math.max(1.2F, (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.5F);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.damagesource.DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        MoCBehavior.dropLoot(this, level, MoCBehavior.of(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
        builder.define(TAMED, false);
        builder.define(ADULT, true);
        builder.define(AGE, 50);
        builder.define(OWNER, "");
    }

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
     * Legacy {@code MoCEntityAquatic.canDespawn() == !getIsTamed()}. Vanilla {@link WaterAnimal} inherits
     * {@code Mob.removeWhenFarAway == true}, so without this a tamed dolphin (or a fishy released from a
     * bowl, or a hatched shark) is silently discarded the moment the nearest player is more than 128
     * blocks away — {@code MoCMobCap} protects tamed creatures from the mod's own cap, but not from
     * vanilla's despawn pass.
     */
    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !getIsTamed();
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

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TypeMoC", getTypeMoC());
        output.putBoolean("Tamed", getIsTamed());
        output.putBoolean("Adult", getIsAdult());
        output.putInt("AgeMoC", getMoCAge());
        output.putString("OwnerName", getOwnerName());
        output.putInt("Temper", this.temper);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setTypeMoC(input.getIntOr("TypeMoC", getTypeMoC()));
        setTamed(input.getBooleanOr("Tamed", false));
        setAdult(input.getBooleanOr("Adult", true));
        setMoCAge(input.getIntOr("AgeMoC", 50));
        setOwnerName(input.getStringOr("OwnerName", ""));
        this.temper = input.getIntOr("Temper", 0);
    }

    /** Legacy {@code temper}: how far a wild dolphin has been won over while being broken in. */
    private int temper;

    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int temper) {
        this.temper = temper;
    }

    /** Legacy {@code MoCEntityAquatic.getMaxTemper()} — base difficulty, matching the land animals. */
    public int getMaxTemper() {
        return 100;
    }

    /**
     * Legacy {@code MoCEntityAquatic}:383-445 — breaking in a wild dolphin. Ride it while untamed and it
     * thrashes and throws you off, rolling {@code nextInt((maxTemper - temper) * 8) == 0} each tick until it
     * submits. Feeding it raw fish raises the temper and shortens the odds.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || getIsTamed() || !isVehicle()) {
            return;
        }
        if (!MoCBehavior.of(this).rideTames
                || !(getFirstPassenger() instanceof net.minecraft.world.entity.player.Player rider)) {
            return;
        }
        if (this.random.nextInt(10) == 0) {
            setDeltaMovement(getDeltaMovement().add(this.random.nextDouble() / 30.0D,
                    this.random.nextDouble() / 30.0D, this.random.nextDouble() / 10.0D));
            this.hurtMarked = true;
        }
        if (this.random.nextInt(50) == 0) {
            rider.stopRiding();
            rider.setDeltaMovement(rider.getDeltaMovement().add(0.0D, 0.9D, -0.3D));
            rider.hurtMarked = true;
            return;
        }
        int chance = getMaxTemper() - getTemper();
        if (chance <= 0) {
            chance = 5;
        }
        if (this.random.nextInt(chance * 8) == 0 && !MoCAnimal.exceedsTameCap(this, rider)) {
            setTamed(true);
            setOwnerName(rider.getName().getString());
            // Legacy tameWithName prompted for a name the instant a creature was tamed.
            drzhark.mocreatures.network.MoCNetwork.promptName(this, rider);
            aquaHearts();
        }
    }

    /**
     * Is this aquatic prone to bite a fishing rod's bobber? Legacy {@code MoCEntityAquatic.isFisheable()}:693-695
     * defaulted to false; only the wild fishy, small fish and medium fish said yes.
     */
    protected boolean isFisheable() {
        return false;
    }

    /**
     * Whether this fish currently has a bobber in its mouth. Deliberately not saved — legacy
     * {@code MoCEntityAquatic.fishHooked}:55 was a plain field too, and a hook never survives a reload anyway.
     */
    private boolean fishHooked;

    /**
     * The bobber this fish bit, cached at bite time so the per-tick follow and the wriggle-free roll below
     * can reach it directly. Transient for the same reason {@link #fishHooked} is.
     */
    private net.minecraft.world.entity.projectile.@Nullable FishingHook biteHook;

    /**
     * Legacy fishing-rod capture, {@code MoCEntityAquatic.onLivingUpdate}:453-470 plus {@code getFished()}:672-686.
     * On a 1-in-30 tick roll a fisheable fish looks for the nearest player within 18 blocks with a free bobber
     * in the water, swims toward it, and once close enough hooks itself on; a hooked fish then wriggles free
     * on a 1-in-200 tick roll. Reeling in while hooked pulls the LIVE fish to the player through vanilla
     * {@code FishingHook.retrieve}:451-455, exactly like legacy — vanilla's private {@code setHookedEntity}
     * is reached through the mixin-backed {@link drzhark.mocreatures.util.FishingHookAccess}.
     *
     * <p>Two deliberate departures from the legacy numbers, both forced by 26.2 geometry:</p>
     *
     * <ul>
     * <li><b>The bite radius is 2.0, not legacy's 1.0.</b> The bobber floats at the fluid surface,
     * {@code blockY + 0.888} ({@code FishingHook.tick}:207 settles at the block's {@code liquidHeight}),
     * while a pathing fish is steered to the node block's BOTTOM ({@code WaterBoundPathNavigation.getGroundY}
     * returns {@code target.y}, and path nodes sit at integer Y) — so even a fish parked directly under the
     * bobber is ~0.9 blocks away feet-to-feet, leaving a horizontal budget of only ~0.45 for a 1.0 trigger.
     * The approach can't deliver that: {@code PathNavigation.moveTo(x,y,z,speed)} hardcodes
     * {@code reachRange=1} and the pathfinder stops as soon as a node is within MANHATTAN 1 of the target
     * block ({@code PathFinder.findPath}:85), with a further 0.6-block waypoint acceptance
     * ({@code PathNavigation.followThePath}:247), so the fish legitimately parks 0.8-2.3 blocks from the
     * hook and repeat {@code moveTo} calls from there build instantly-done paths that never move it closer.
     * A 1.0 radius therefore never triggered at all — the reported "fish never hook".</li>
     * <li><b>Each approach roll also lunges the fish directly at the hook.</b> A small velocity impulse
     * closes the final blocks (including the vertical 0.9 a path never climbs, since nodes are whole
     * blocks) and cannot be stolen by the wander goal the way a navigation path can. This is also why the
     * impulse, not {@code getMoveControl().setWantedPosition}, is the direct-steering mechanism: the
     * {@link AquaticMoveControl} below (like vanilla {@code AbstractFish.FishMoveControl}:174) only acts on
     * a wanted position while {@code !getNavigation().isDone()}, so a bare wanted position with no live
     * path is ignored.</li>
     * </ul>
     */
    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        if (isFisheable() && !this.fishHooked && this.random.nextInt(30) == 0) {
            net.minecraft.world.entity.player.Player angler = level.getNearestPlayer(this, 18.0D);
            if (angler != null && angler.fishing != null && angler.fishing.getHookedIn() == null) {
                net.minecraft.world.entity.projectile.FishingHook hook = angler.fishing;
                float dist = distanceTo(hook);
                if (dist > 2.0F) {
                    getNavigation().moveTo(hook.getX(), hook.getY(), hook.getZ(), 1.0D);
                    // The direct lunge (see the javadoc above). Water drag (~0.8/tick) turns a 0.2 impulse
                    // into roughly a block of glide, so every roll visibly draws the fish a block closer no
                    // matter what the pathfinder or the wander goal are doing. The vertical share is kept
                    // smaller so a fish in a 1-deep pool nudges up toward the bobber without breaching.
                    setDeltaMovement(getDeltaMovement().add(
                            (hook.getX() - getX()) / dist * 0.2D,
                            (hook.getY() - getY()) / dist * 0.12D,
                            (hook.getZ() - getZ()) / dist * 0.2D));
                } else {
                    ((drzhark.mocreatures.util.FishingHookAccess) hook).moc$setHookedEntity(this);
                    this.fishHooked = true;
                    this.biteHook = hook;
                }
            }
        }
        if (this.fishHooked) {
            net.minecraft.world.entity.projectile.FishingHook hook = this.biteHook;
            if (hook == null || hook.isRemoved() || hook.getHookedIn() != this) {
                // Reeled in, line broken (owner left / swapped items — FishingHook.shouldStopFishing), or
                // something else unhooked us: forget the bite so the fish can be caught again.
                this.fishHooked = false;
                this.biteHook = null;
            } else if (this.random.nextInt(200) == 0) {
                // Legacy wriggle-free (MoCEntityAquatic:457-470). The cached hook replaces legacy's 2-block
                // box scan — the box only worked because the 1.12 bobber teleported to its caught fish.
                ((drzhark.mocreatures.util.FishingHookAccess) hook).moc$setHookedEntity(null);
                this.fishHooked = false;
                this.biteHook = null;
            } else {
                // Legacy 1.12 EntityFishHook.onUpdate dragged the bobber onto its caughtEntity every tick.
                // The 26.2 hook only does that in the HOOKED_IN_ENTITY state (FishingHook.tick:190-203),
                // which a bobber bitten while BOBBING never enters — the only transition is in the FLYING
                // branch (:175-180). Pin it ourselves, mirroring the HOOKED_IN_ENTITY setPos (:195), so the
                // player SEES the fish take the bobber and the line follows the fish until it is reeled in
                // (retrieve:451-455 pulls hookedIn regardless of the hook's state) or wriggles free.
                hook.setPos(getX(), getY(0.8D), getZ());
            }
        }
    }

    /**
     * Swim-aware move control, copied from vanilla {@code AbstractFish.FishMoveControl}
     * ({@code AbstractFish.java}:163-193). The default {@code MoveControl} that {@code Mob} installs only
     * sets yaw and forward thrust ({@code MoveControl.tick}:84-108) — its sole vertical mechanism is a jump
     * impulse when the mob is already within a block horizontally below a waypoint — while default water
     * travel ({@code LivingEntity.travelInWater}:2496-2523) applies gravity. That combination cannot follow
     * any {@code WaterBoundPathNavigation} path with ascending nodes: the fish stalls below the first rising
     * waypoint (waypoint acceptance is 0.5 blocks vertically, {@code WaterBoundPathNavigation
     * .getMaxVerticalDistanceToWaypoint}), stuck detection cancels the path, and the fish sinks back down.
     * This is why a fisheable fish never reached a bobber — the bobber bobs in the topmost water block
     * ({@code FishingHook.tick}:207), the highest node of every path. Vanilla pairs every water navigator
     * with a swim-aware control for exactly this reason; this is that control for the Mo'Creatures aquatics.
     */
    private static class AquaticMoveControl extends MoveControl<MoCAquatic> {

        AquaticMoveControl(MoCAquatic mob) {
            super(mob);
        }

        @Override
        public void tick() {
            // Gentle buoyancy while submerged, countering water gravity (AbstractFish.FishMoveControl:170-172).
            if (this.mob.isEyeInFluid(FluidTags.WATER)) {
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
            }
            if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
                float targetSpeed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.mob.setSpeed(Mth.lerp(0.125F, this.mob.getSpeed(), targetSpeed));
                double xd = this.wantedX - this.mob.getX();
                double yd = this.wantedY - this.mob.getY();
                double zd = this.wantedZ - this.mob.getZ();
                if (yd != 0.0D) {
                    double dd = Math.sqrt(xd * xd + yd * yd + zd * zd);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement()
                            .add(0.0D, this.mob.getSpeed() * (yd / dd) * 0.1D, 0.0D));
                }
                if (xd != 0.0D || zd != 0.0D) {
                    float yRotD = (float) (Mth.atan2(zd, xd) * 180.0F / (float) Math.PI) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 90.0F));
                    this.mob.yBodyRot = this.mob.getYRot();
                }
            } else {
                this.mob.setSpeed(0.0F);
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        selectType();
        return data;
    }

    protected static Identifier modelTexture(String file) {
        return Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "textures/models/" + file);
    }
}
