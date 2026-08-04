package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityHorseMob} — a hostile undead/skeleton/bat horse variant.
 * Complex legacy flying behaviour is dropped, but the signature undead mechanics are preserved:
 * type selection, textures, sounds, the death-slime burst (undead types 23/24/25) and letting a
 * nearby riderless skeleton/zombie mount it to form undead cavalry.
 */
public class MoCEntityHorseMob extends MoCMob {

    public MoCEntityHorseMob(EntityType<? extends MoCEntityHorseMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public void selectType() {
        // Legacy: in the Nether (isHellWorld) the horse-mob is always the fire nightmare (type 38);
        // below Y 50 it is forced to the bat horse (type 32); otherwise it rolls undead/skeleton/bat.
        if (this.level().dimension() == Level.NETHER) {
            setTypeMoC(38); // nightmare
            return;
        }
        if (getTypeMoC() == 0) {
            if (getY() < 50.0D) {
                setTypeMoC(32); // bat horse (deep underground), legacy getCanSpawnHere override
                return;
            }
            int j = this.random.nextInt(100);
            if (j <= 40) {
                setTypeMoC(23); // undead
            } else if (j <= 80) {
                setTypeMoC(26); // skeleton horse
            } else {
                setTypeMoC(32); // bat horse (legacy overworld roll never yields the nightmare, type 38)
            }
        }
    }

    @Override
    public boolean fireImmune() {
        // The nightmare (type 38) is a fire horse and, like the legacy isImmuneToFire flag, ignores fire/lava.
        return getTypeMoC() == 38 || super.fireImmune();
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 26 -> modelTexture("horseskeleton.png");
            case 32 -> modelTexture("horsebat.png");
            case 38 -> modelTexture("horsenightmare1.png");
            default -> modelTexture("horseundead.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.HORSEGRUNTUNDEAD.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.HORSEHURTUNDEAD.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.HORSEDYINGUNDEAD.get();
    }

    @Override
    protected boolean burnsInDaylight() {
        // Undead/skeleton/bat horses burn in daylight; the fire-immune nightmare (type 38) does not.
        return getTypeMoC() != 38;
    }

    @Override
    public void tick() {
        super.tick();
        // Legacy onLivingUpdate: when unridden, an undead horse mob lets the first nearby riderless
        // skeleton or zombie (boundingBox.expand(4,3,4) scan) climb aboard, forming undead cavalry.
        if (this.level() instanceof ServerLevel serverLevel && this.getVehicle() == null) {
            for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(4.0D, 3.0D, 4.0D))) {
                if ((entity instanceof Skeleton || entity instanceof Zombie) && entity.getVehicle() == null) {
                    entity.startRiding(this);
                    break;
                }
            }
        }
        // Client-side decay wisp: the undead horse mob (type 23) sheds a greenish FX_UNDEAD wisp as it
        // rots (legacy MoCEntityHorseMob ~1/50 tick), mirroring the passive undead-horse emit.
        if (this.level().isClientSide() && getTypeMoC() == 23
                && drzhark.mocreatures.config.MoCConfig.get().particleFX
                && this.random.nextInt(50) == 0) {
            double w = getBbWidth();
            this.level().addParticle(drzhark.mocreatures.registry.MoCParticles.FX_UNDEAD.get(),
                    getX() + (this.random.nextDouble() - 0.5D) * w,
                    getY() + getBbHeight() * 0.6D,
                    getZ() + (this.random.nextDouble() - 0.5D) * w, 0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    protected void applyHitEffects(net.minecraft.world.entity.LivingEntity target) {
        super.applyHitEffects(target);
        // Legacy attackEntity played MoCTools.playCustomSound(this, "horsemad", worldObj) on every
        // successful melee strike (volume 1.0, pitch 1.0 +/- 0.2 jitter) alongside the stand/openMouth anim.
        this.level().playSound(null, this.blockPosition(), MoCSounds.HORSEMAD.get(),
                this.getSoundSource(), 1.0F,
                1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        // Legacy onDeath: an undead horse mob (types 23/24/25) bursts into a slime when it dies
        // (MoCTools.spawnSlimes -> exactly one small EntitySlime at the death position).
        int type = getTypeMoC();
        if (this.level() instanceof ServerLevel serverLevel && (type == 23 || type == 24 || type == 25)) {
            Slime slime = EntityTypes.SLIME.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (slime != null) {
                // Legacy MoCTools.spawnSlimes left setSlimeSize commented out, so the burst uses the
                // vanilla EntitySlime constructor's random size (1 << rand.nextInt(3) -> 1/2/4): the
                // undead horse can splatter into a small, medium, or large splitting slime.
                slime.setSize(1 << this.random.nextInt(3), true);
                slime.snapTo(getX() - 0.125D, getY() + 0.5D, getZ() - 0.125D,
                        this.random.nextFloat() * 360.0F, 0.0F);
                serverLevel.addFreshEntity(slime);
            }
        }
    }
}
