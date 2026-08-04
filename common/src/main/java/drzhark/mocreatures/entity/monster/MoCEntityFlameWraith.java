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
 * Port of the legacy {@code MoCEntityFlameWraith}. A fiery hostile wraith that FLIES: like the legacy
 * {@code MoCEntityWraith} (which returned {@code isFlyer() == true}) it hovers and drifts through the air
 * after its prey. It repeatedly self-ignites and, because it is a creature of night, LOSES health when
 * caught in bright daylight under open sky even though it is otherwise fire-immune. A successful melee hit
 * sets the victim ablaze for 30 seconds (unless fought in the Nether, whose denizens shrug off fire).
 */
public class MoCEntityFlameWraith extends MoCMob {

    public MoCEntityFlameWraith(EntityType<? extends MoCEntityFlameWraith> type, Level level) {
        super(type, level);
        // Flyer: mirror the in-port flyer setup (see MoCEntityWyvern) so the wraith hovers/drifts.
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.60D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // Flyer goal set (same pattern as the port's other flyers): float, wander the air, melee the prey,
        // watch nearby players, and target players / whatever hurt it.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false; // a flyer never takes fall damage
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("flamewraith.png");
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
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy onLivingUpdate: randomly wreathe itself in flame (~2s), and — although fire-immune —
        // burn away 2 HP whenever it is caught in bright daylight under open sky.
        if (this.random.nextInt(40) == 0) {
            this.igniteForSeconds(2.0F);
        }
        if (level.isBrightOutside() && level.canSeeSky(this.blockPosition())) {
            // Legacy proc gate: the 2 HP daylight drain is a RARE random roll scaled by brightness
            // (getBrightness > 0.5 && rand*30 < (f-0.4)*2 -> at most ~4%/tick), NOT every tick — otherwise
            // 2 HP * 20 ticks/s kills it in well under a second. Fire-immune, so use a generic source so
            // the drain actually lands (same self-damage idiom as MoCEntityOgre's daylight wither).
            float f = this.getLightLevelDependentMagicValue();
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F) {
                this.hurtServer(level, this.damageSources().generic(), 2.0F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Client-side: wreathe the wraith in flame + a little smoke so it clearly reads as burning.
        // (The vanilla full-entity fire overlay was tried first but rendered a solid wall of fire that
        // occluded the wraith itself; particles + the emissive glow layer convey "on fire" without hiding it.)
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                double ox = (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                double oy = this.random.nextDouble() * this.getBbHeight();
                double oz = (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0.0D, 0.0D, 0.0D);
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            this.getX() + ox, this.getY() + oy + 0.3D, this.getZ() + oz, 0.0D, 0.02D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void applyHitEffects(net.minecraft.world.entity.LivingEntity target) {
        // Legacy attackEntity: set the victim ablaze for 30s — but NOT in the Nether (its mobs ignore fire).
        if (this.level().dimension() != Level.NETHER) {
            target.igniteForSeconds(30.0F);
        }
    }
}
