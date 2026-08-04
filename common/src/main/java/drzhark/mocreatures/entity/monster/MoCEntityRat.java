package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Port of the legacy {@code MoCEntityRat}. A small, hostile rat with three colour variants.
 */
public class MoCEntityRat extends MoCMob {

    /** The last attacker we already rallied the pack against, so the alert fires once per hit, not every tick. */
    @Nullable
    private LivingEntity alertedAgainst;

    public MoCEntityRat(EntityType<? extends MoCEntityRat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy rat attackStrength = 1 (getAttackStrength returned 1).
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    /**
     * Legacy rats only hunt players in the dark ({@code getBrightness < 0.5F}). Replace the base
     * always-on player target goal with one gated on low light level; the {@code HurtByTargetGoal}
     * installed by {@link MoCMob#registerGoals()} still lets a hurt rat retaliate at any time.
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

    /** Approximates the legacy {@code getBrightness(1.0F) < 0.5F} darkness check. */
    private boolean isDarkEnough() {
        return this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7;
    }

    /** Legacy rats climb walls (their {@code isOnLadder} returned {@code isCollidedHorizontally}). */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy: rats stop UNPROVOKED hunting of players once it becomes bright again — but a rat that was
        // just hit still retaliates in any light (legacy attackEntityFrom set the attacker unconditionally),
        // so never clear a target that is our current attacker.
        if (this.getTarget() instanceof Player && !isDarkEnough()
                && this.getTarget() != this.getLastHurtByMob()) {
            this.setTarget(null);
        }

        // Legacy pack aggro: when FIRST hurt by a new attacker, alert every nearby rat once to gang up on it.
        // getLastHurtByMob() lingers ~100 ticks, so guard on a change to avoid re-scanning every tick.
        LivingEntity attacker = this.getLastHurtByMob();
        if (attacker != null && attacker != this.alertedAgainst) {
            this.alertedAgainst = attacker;
            List<MoCEntityRat> nearby =
                    level.getEntitiesOfClass(MoCEntityRat.class, this.getBoundingBox().inflate(16.0D, 4.0D, 16.0D));
            for (MoCEntityRat rat : nearby) {
                if (rat != this && rat.getTarget() == null) {
                    rat.setTarget(attacker);
                }
            }
        } else if (attacker == null) {
            this.alertedAgainst = null;
        }
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 65) {
                setTypeMoC(1);
            } else if (i <= 98) {
                setTypeMoC(2);
            } else {
                setTypeMoC(3);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("ratbl.png");
            case 3 -> modelTexture("ratw.png");
            default -> modelTexture("ratb.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.RATGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.RATHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.RATDYING.get();
    }
}
