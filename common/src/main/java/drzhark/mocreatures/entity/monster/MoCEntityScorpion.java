package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.entity.passive.MoCEntityPetScorpion;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityScorpion}. A poisonous monster with four biome-based variants
 * (dirt, cave, nether, frost).
 *
 * <p>Restored combat behaviour from the 1.12.2 source:</p>
 * <ul>
 *   <li>Per-type sting on a successful melee hit: dirt/cave -&gt; poison, nether -&gt; sets the
 *       target on fire, frost -&gt; slowness, undead -&gt; wither (undead only occurs on the pet
 *       variant, handled defensively here).</li>
 *   <li>A female scorpion "carrying young" ({@link #getHasBabies()}, rolled on spawn) releases up to
 *       five baby {@link MoCEntityPetScorpion} of the same variant on death, so tameable pet
 *       scorpions can be obtained naturally.</li>
 * </ul>
 */
public class MoCEntityScorpion extends MoCMob {

    /** Synched: this scorpion is a female carrying young and will drop babies on death. */
    private static final EntityDataAccessor<Boolean> HAS_BABIES =
            SynchedEntityData.defineId(MoCEntityScorpion.class, EntityDataSerializers.BOOLEAN);
    /** Synched sting-strike flag: true for a few ticks after a hit, so the client arches the tail forward. */
    private static final EntityDataAccessor<Boolean> STINGING =
            SynchedEntityData.defineId(MoCEntityScorpion.class, EntityDataSerializers.BOOLEAN);

    /** Server-side countdown holding the sting pose up after a hit; decremented each tick. */
    private int stingTicks;

    public MoCEntityScorpion(EntityType<? extends MoCEntityScorpion> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy scorpions only hunt players at night ({@code findPlayerToAttack} required
     * {@code !worldObj.isDaytime()}). The inherited {@link MoCMob#registerGoals()} installs an always-on
     * player target goal, which would let the scorpion attack in broad daylight — so replace it with one
     * gated on darkness ({@code getLightLevelDependentMagicValue() < 0.5F}). The {@code HurtByTargetGoal}
     * from the base still lets a struck scorpion retaliate at any time.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal);
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isDarkEnough() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isDarkEnough() && super.canContinueToUse();
            }
        });
    }

    /** Approximates the legacy {@code getBrightness(1.0F) < 0.5F} night-only aggression gate. */
    private boolean isDarkEnough() {
        return this.getLightLevelDependentMagicValue() < 0.5F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy scorpion attackStrength was 1 (plain claw strike dealt 1 damage), not 2.
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_BABIES, false);
        builder.define(STINGING, false);
    }

    /** Whether this scorpion is carrying young (and will release babies on death). */
    public boolean getHasBabies() {
        return this.entityData.get(HAS_BABIES);
    }

    public void setHasBabies(boolean flag) {
        this.entityData.set(HAS_BABIES, flag);
    }

    /** True for a few ticks after a successful sting — the client arches the tail forward for the strike pose. */
    public boolean isStinging() {
        return this.entityData.get(STINGING);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Hold the sting pose briefly after a hit, then relax the tail.
        if (this.stingTicks > 0 && --this.stingTicks == 0 && isStinging()) {
            this.entityData.set(STINGING, false);
        }
        // Legacy attackEntity gap-charge: when grounded with a target 2-6 blocks away, occasionally (1/15
        // tick) lunge toward it — horizontally at 0.5*0.8 with a little carried momentum, plus motionY 0.4.
        LivingEntity target = getTarget();
        if (target != null && this.onGround()) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double distSq = (dx * dx) + (dz * dz);
            if (distSq > 4.0D && distSq < 36.0D && this.random.nextInt(15) == 0) {
                double dist = Math.sqrt(distSq);
                net.minecraft.world.phys.Vec3 dm = this.getDeltaMovement();
                this.setDeltaMovement(
                        ((dx / dist) * 0.5D * 0.8D) + (dm.x * 0.2D),
                        0.4D,
                        ((dz / dist) * 0.5D * 0.8D) + (dm.z * 0.2D));
                this.hurtMarked = true; // force a velocity sync so the client sees the lunge
            }
        }
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Faithful legacy checkSpawningBiome: nether-scorpion in the Nether, frost in snowy/frozen biomes,
            // cave scorpion deep underground with no sky, dirt scorpion everywhere else.
            if (this.level().dimension() == net.minecraft.world.level.Level.NETHER) {
                setTypeMoC(3); // nether scorpion
            } else if (this.level().getBiome(this.blockPosition()).value().getBaseTemperature() <= 0.05F) {
                setTypeMoC(4); // frost scorpion
            } else if (!this.level().canSeeSky(this.blockPosition()) && this.getY() < 50.0D) {
                setTypeMoC(2); // cave scorpion
            } else {
                setTypeMoC(1); // dirt scorpion
            }
        }
    }

    /** Spider-style wall climb (legacy {@code isOnLadder() = isCollidedHorizontally}). */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /**
     * Legacy {@code checkSpawningBiome} / {@code getCanSpawnHere} refused to spawn a scorpion near a torch
     * ({@code MoCTools.isNearTorch}). Approximate that here: reject the spawn wherever there is appreciable
     * block light (a torch or other man-made light source), keeping scorpions to the true dark.
     */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
        if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockPosition()) >= 8) {
            return false;
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        // Legacy: ~1 in 4 wild scorpions is a female carrying young.
        setHasBabies(this.random.nextInt(4) == 0);
        return data;
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("scorpioncave.png");
            case 3 -> modelTexture("scorpionnether.png");
            case 4 -> modelTexture("scorpionfrost.png");
            default -> modelTexture("scorpiondirt.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.SCORPIONGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.SCORPIONHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.SCORPIONDYING.get();
    }

    @Override
    protected void applyHitEffects(LivingEntity target) {
        // Trigger the sting-strike pose: the tail whips forward for ~half a second on a landed hit.
        this.stingTicks = 10;
        if (!isStinging()) {
            this.entityData.set(STINGING, true);
        }
        // Legacy sting PROC gate: the actual potion/fire sting only lands on ~1-in-5 hits
        // (rand.nextInt(5) == 0); every other hit is a plain claw strike with no added effect.
        if (this.random.nextInt(5) == 0) {
            // Legacy per-type sting: dirt/cave (<=2) poison, nether (3) fire, frost (4) slowness,
            // undead (5) wither. Undead only exists on the pet variant but is mapped defensively.
            switch (getTypeMoC()) {
                case 3 -> { // nether scorpion: burns the target
                    target.igniteForSeconds(15.0F);
                }
                case 4 -> // frost scorpion: slows the target
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0), this);
                case 5 -> // undead scorpion: withers the target
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0), this);
                default -> // dirt/cave scorpions: poison the target
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0), this);
            }
            // Sting sound, matching the legacy scorpionsting cue.
            this.level().playSound(null, this.blockPosition(), MoCSounds.SCORPIONSTING.get(), SoundSource.HOSTILE,
                    1.0F, 1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel && getIsAdult() && getHasBabies()) {
            int count = this.random.nextInt(5); // up to 5 babies (legacy rand.nextInt(5))
            for (int i = 0; i < count; i++) {
                MoCEntityPetScorpion baby = new MoCEntityPetScorpion(MoCEntities.PET_SCORPION.get(), serverLevel);
                baby.setPos(this.getX(), this.getY(), this.getZ());
                baby.setYRot(this.getYRot());
                baby.setTypeMoC(getTypeMoC());
                baby.setAdult(false);
                baby.setMoCAge(35);
                baby.setAge(-24000); // vanilla baby age -> renders small and grows up
                baby.setHealth(baby.getMaxHealth());
                serverLevel.addFreshEntity(baby);
                serverLevel.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                        1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
            }
        }
        super.die(damageSource);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Babies", getHasBabies());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasBabies(input.getBooleanOr("Babies", false));
    }
}
