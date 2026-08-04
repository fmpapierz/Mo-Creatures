package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityCricket}. A tiny jumping insect with two colour variants
 * that hops around and chirps.
 */
public class MoCEntityCricket extends MoCAnimal {

    /**
     * Synched: the cricket is airborne. Legacy {@code MoCEntityInsect} tracked this on datawatcher 22 and
     * toggled it via {@code getFlyingFreq} (take off near a large entity) / a rare land chance; ported here
     * so the cricket can leave the ground again (its legacy insect base was a flyer).
     */
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(MoCEntityCricket.class, EntityDataSerializers.BOOLEAN);

    /** Legacy 30-tick cooldown between directional hops (updateEntityActionState gated the leap on this). */
    private int jumpCounter;
    /** Countdown between airborne {@code cricketfly} buzz sounds (legacy soundCounter, ~10 ticks). */
    private int soundCounter;

    public MoCEntityCricket(EntityType<? extends MoCEntityCricket> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean getIsFlying() {
        return this.entityData.get(FLYING);
    }

    public void setIsFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 50) {
                setTypeMoC(1);
            } else {
                setTypeMoC(2);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy MoCEntityInsect flight trigger (getFlyingFreq() == 20 for cricket): when grounded, a rare
        // chance to take off if a large living entity (bbox >= 0.4 x 0.4) it can see is within ~4 blocks —
        // an upward hop (motionY += 0.3) plus the airborne flag.
        if (!getIsFlying() && this.random.nextInt(20) == 0) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(4.0D), e -> e != this)) {
                if (other.getBbWidth() >= 0.4F && other.getBbHeight() >= 0.4F && this.hasLineOfSight(other)) {
                    Vec3 dm = getDeltaMovement();
                    setDeltaMovement(dm.x, dm.y + 0.3D, dm.z);
                    this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
                    setIsFlying(true);
                    break;
                }
            }
        }
        // Legacy: a flying cricket has a rare (~1/50) chance to settle back down each tick.
        if (getIsFlying() && this.random.nextInt(50) == 0) {
            setIsFlying(false);
        }
        // Legacy: while airborne (flying or simply not on the ground) buzz 'cricketfly' to a nearby player
        // (within 5 blocks) roughly every 10 ticks.
        if (getIsFlying() || !this.onGround()) {
            Player near = level.getNearestPlayer(this, 5.0D);
            if (near != null && --this.soundCounter <= 0) {
                level.playSound(null, this.blockPosition(), MoCSounds.CRICKETFLY.get(),
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
                this.soundCounter = 10;
            }
        }

        // Legacy cricket hop (updateEntityActionState): only leaps while already moving on the ground, and
        // only once per 30-tick jumpCounter cycle — a directional spring that boosts existing horizontal
        // motion x5 with motionY = 0.45 (NOT a fixed impulse regardless of movement).
        Vec3 dm = getDeltaMovement();
        if (this.jumpCounter == 0 && this.onGround()
                && (Math.abs(dm.x) > 0.05D || Math.abs(dm.z) > 0.05D)) {
            setDeltaMovement(dm.x * 5.0D, 0.45D, dm.z * 5.0D);
            this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
            this.jumpCounter = 1;
        }
        if (this.jumpCounter > 0 && ++this.jumpCounter > 30) {
            this.jumpCounter = 0;
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("cricketb.png");
            default -> modelTexture("cricketa.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        // Legacy crickets chirped at NIGHT only (updateEntityActionState gated on !isDaytime()).
        return level().isBrightOutside() ? null : MoCSounds.CRICKET.get();
    }
}
