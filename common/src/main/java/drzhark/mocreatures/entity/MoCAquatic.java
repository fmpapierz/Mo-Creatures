package drzhark.mocreatures.entity;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
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
