package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCFlyingInsect;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBee}. A small flying insect.
 */
public class MoCEntityBee extends MoCFlyingInsect {

    /** Countdown between buzz sounds, mirroring the legacy {@code soundCount} (~20 ticks). */
    private int soundCount;

    public MoCEntityBee(EntityType<? extends MoCEntityBee> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.60D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // A bee stings back when attacked, but doesn't hunt unprovoked.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4D, true));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        // A sting inflicts poison on living victims.
        if (hurt && target instanceof net.minecraft.world.entity.LivingEntity victim) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 100, 0), this);
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy onLivingUpdate: while flying, a bee within 5 blocks of a player buzzes every 20 ticks,
        // switching to the angry 'beeupset' cue once it has picked a target (getMySound()).
        Player near = level.getNearestPlayer(this, 5.0D);
        if (near != null && --this.soundCount <= 0) {
            SoundEvent sound = getTarget() != null ? MoCSounds.BEEUPSET.get() : MoCSounds.BEE.get();
            level.playSound(null, this.blockPosition(), sound, SoundSource.NEUTRAL, 0.1F, 1.0F);
            this.soundCount = 20;
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("bee.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }
}
