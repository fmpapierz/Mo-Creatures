package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityCrocodile}. A large amphibious predator.
 */
public class MoCEntityCrocodile extends MoCAnimal {

    public MoCEntityCrocodile(EntityType<? extends MoCEntityCrocodile> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code getCanSpawnHere} began with {@code if (MoCTools.isNearTorch(this)) return false;} — a
     * crocodile never spawns near a man-made light source (torch, glowstone, redstone lamp, lit pumpkin
     * within ~8 blocks). Approximate that here by rejecting the spawn wherever there is appreciable block
     * light, mirroring {@code MoCEntityScorpion}. Block light only, so daytime skylight does not trip it.
     */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.EntitySpawnReason reason) {
        if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockPosition()) >= 8) {
            return false;
        }
        return super.checkSpawnRules(level, reason);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy crocBite deals a plain 2 damage (1 heart); its lethality is the grab-and-drown roll.
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("crocodile.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        // Legacy getLivingSound(): distinct resting call while basking, ordinary grunt otherwise.
        return getIsResting() ? MoCSounds.CROCRESTING.get() : MoCSounds.CROCGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.CROCHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.CROCDYING.get();
    }

    // Signature grab-and-drown death-roll (legacy): the crocodile seizes prey, drags it underwater, keeps it
    // drowning, then spins to tear it apart.
    private static final EntityDataAccessor<Boolean> CAUGHT_PREY =
            SynchedEntityData.defineId(MoCEntityCrocodile.class, EntityDataSerializers.BOOLEAN);
    // Legacy basking/resting idle state: the croc periodically lies still on the bank, halves its alert
    // range while resting and gives the distinct "crocresting" call.
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(MoCEntityCrocodile.class, EntityDataSerializers.BOOLEAN);
    private int spinInt;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAUGHT_PREY, false);
        builder.define(RESTING, false);
    }

    public boolean getHasCaughtPrey() {
        return this.entityData.get(CAUGHT_PREY);
    }

    public void setHasCaughtPrey(boolean caught) {
        this.entityData.set(CAUGHT_PREY, caught);
    }

    public boolean getIsResting() {
        return this.entityData.get(RESTING);
    }

    public void setIsResting(boolean resting) {
        this.entityData.set(RESTING, resting);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Holds the MOVE flag while basking so the wandering goal cannot budge it (legacy isMovementCeased).
        this.goalSelector.addGoal(1, new RestGoal(this));
        // Legacy findPlayerToAttack also called getClosestEntityLiving(this, attackD): beyond the player,
        // the crocodile proactively preys on any nearby living entity that entitiesToIgnore does NOT exclude —
        // i.e. not another crocodile and not strictly smaller than the croc in BOTH height and width (so cows,
        // sheep, pigs, villagers, zombies, etc. are fair game). Unlike the player clause this had no adult
        // gate, so juveniles stalk livestock too. MoCAnimal only installs the untamed-adult Player target goal,
        // which loses this animal predation; restore it here.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                (e, lvl) -> !(e instanceof MoCEntityCrocodile)
                        && !(e.getBbHeight() < getBbHeight() && e.getBbWidth() < getBbWidth())));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // A grown crocodile may SEIZE its prey (1/3) instead of a plain bite — mounting it to drag it under.
        if (!getHasCaughtPrey() && getIsAdult() && target instanceof LivingEntity && !target.isPassenger()
                && this.random.nextInt(3) == 0 && target.startRiding(this)) {
            setHasCaughtPrey(true);
            setTarget(null);
            level.playSound(null, blockPosition(), MoCSounds.CROCJAWSNAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            level.playSound(null, blockPosition(), MoCSounds.CROCJAWSNAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Basking/resting idle cycle (legacy onLivingUpdate): while resting, wake on a target, on caught prey,
        // or a random ~1/500 stir; while active and idle, occasionally settle down to bask (~1/500).
        if (getIsResting()) {
            if (getTarget() != null || getHasCaughtPrey() || this.random.nextInt(500) == 0) {
                setIsResting(false);
            }
        } else if (getTarget() == null && !getHasCaughtPrey() && this.random.nextInt(500) == 0) {
            setIsResting(true);
            this.getNavigation().stop();
        }
        // Legacy findPlayerToAttack detection range: 12 blocks normally, halved to 6 while basking.
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        double wantRange = getIsResting() ? 6.0D : 12.0D;
        if (followRange != null && followRange.getBaseValue() != wantRange) {
            followRange.setBaseValue(wantRange);
        }

        if (!getHasCaughtPrey()) {
            return;
        }
        // Let go if the prey is gone or dead.
        if (!(getFirstPassenger() instanceof LivingEntity victim) || !victim.isAlive()) {
            unMount();
            return;
        }
        setTarget(null);
        // Drag the prey toward the nearest water while it isn't submerged.
        if (!this.isInWater()) {
            moveToNearestWater(level);
        }
        // Keep the victim in its drowning loop (reset its death animation) and gnaw at it periodically.
        victim.deathTime = 0;
        if (this.random.nextInt(50) == 0) {
            victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
        }
        // Death-roll once submerged: spin, roar the roll every ~20 ticks, and rip a big chunk every ~80.
        if (this.isInWater()) {
            this.spinInt += 3;
            if (this.spinInt % 20 == 0) {
                level.playSound(null, blockPosition(), MoCSounds.CROCROLL.get(), SoundSource.HOSTILE,
                        1.0F, 1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
            }
            if (this.spinInt > 80) {
                this.spinInt = 0;
                victim.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
            }
        }
        // Prey occasionally wrenches free.
        if (this.random.nextInt(200) == 0) {
            unMount();
        }
    }

    /** Releases the caught prey (letting it die or flee normally) and clears the roll. */
    private void unMount() {
        if (getFirstPassenger() instanceof LivingEntity victim) {
            if (victim.getHealth() > 0.0F) {
                victim.deathTime = 0;
            }
            victim.stopRiding();
        }
        setHasCaughtPrey(false);
        this.spinInt = 0;
    }

    @Override
    public void die(DamageSource cause) {
        unMount(); // a slain crocodile lets go of its prey
        super.die(cause);
    }

    /** Paths toward the nearest water block (throttled) so the crocodile can drag its catch under. */
    private void moveToNearestWater(ServerLevel level) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        BlockPos base = blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double best = Double.MAX_VALUE;
        BlockPos target = null;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -4; dy <= 2; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    p.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (level.getFluidState(p).is(FluidTags.WATER)) {
                        double d = p.distSqr(base);
                        if (d < best) {
                            best = d;
                            target = p.immutable();
                        }
                    }
                }
            }
        }
        if (target != null) {
            this.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
        }
    }

    /**
     * Keeps the crocodile motionless while it is basking (legacy {@code isMovementCeased}). Holding the
     * MOVE flag stops the wandering goal from strolling it off; it releases the moment the croc wakes.
     */
    private static final class RestGoal extends Goal {

        private final MoCEntityCrocodile croc;

        RestGoal(MoCEntityCrocodile croc) {
            this.croc = croc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return croc.getIsResting() && !croc.isVehicle() && !croc.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return croc.getIsResting() && !croc.isVehicle() && !croc.isPassenger();
        }

        @Override
        public void start() {
            croc.getNavigation().stop();
        }

        @Override
        public void tick() {
            croc.getNavigation().stop();
        }
    }
}
