package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityWraith}. A hostile flying ghost mob.
 */
public class MoCEntityWraith extends MoCMob {

    public MoCEntityWraith(EntityType<? extends MoCEntityWraith> type, Level level) {
        super(type, level);
        // Legacy isFlyer()=true: the wraith hovers through the air toward players. Mirror the in-port
        // flyer setup (MoCFlyingInsect / MoCEntityWyvern) with a hovering flight move-control.
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                // Legacy getMoveSpeed()=1.3; the wraith drifts noticeably faster than a walking mob.
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    /**
     * Flyer goals: replace the base-class ground stroll with a hovering air-wander (mirrors the insect
     * flyers), while keeping the melee attack, player-targeting and look goals so the wraith still floats
     * toward and strikes players.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy getAttackStrength scaled the wraith's bite by difficulty: `difficultySetting == 1` (EASY)
        // returned 2, everything ELSE (peaceful/normal/hard) returned 3. Re-derive the ATTACK_DAMAGE base each
        // tick so it tracks runtime difficulty changes (mirrors MoCEntityWWolf).
        net.minecraft.world.entity.ai.attributes.AttributeInstance attack =
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double force = level.getDifficulty() == net.minecraft.world.Difficulty.EASY ? 2.0D : 3.0D;
            if (attack.getBaseValue() != force) {
                attack.setBaseValue(force);
            }
        }
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false; // a flyer never takes fall damage
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("wraith.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.WRAITH.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.WRAITHHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.WRAITHDYING.get();
    }

    @Override
    protected boolean burnsInDaylight() {
        return true; // wraiths catch fire in daylight
    }
}
