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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Port of the legacy {@code MoCEntityHellRat}. A fire-immune nightmare rat (type 4).
 */
public class MoCEntityHellRat extends MoCMob {

    public MoCEntityHellRat(EntityType<? extends MoCEntityHellRat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Drop the base mob's always-on player-hunting target goal: a hellrat only stalks players in the dark
        // (handled in customServerAiStep). Left in, the inherited goal re-acquires the nearest player every
        // tick — even in daylight — which defeats the darkness gate and the skittish light-flee below.
        this.targetSelector.removeAllGoals(
                g -> g instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal);
    }

    @Override
    public void selectType() {
        setTypeMoC(4);
    }

    @Override
    public Identifier getTexture() {
        return modelTexture(nightmareFrame());
    }

    /**
     * The nightmare flicker: the hellrat's coat alternates {@code hellrat1..2} for a shimmering, unstable
     * hellish texture when the {@code animateTextures} config is on (legacy {@code getTexture} textCounter
     * flip-flop); static frame 1 when off.
     */
    private String nightmareFrame() {
        if (drzhark.mocreatures.config.MoCConfig.get().animateTextures) {
            return "hellrat" + (((this.tickCount / 3) % 2) + 1) + ".png";
        }
        return "hellrat1.png";
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

    @Override
    public boolean fireImmune() {
        return true; // nether rat
    }

    // ---------------------------------------------------------------- rat behaviours (climbing / darkness / pack)
    // Faithful to the legacy rat that the hellrat inherited from: it scuttles up walls, only hunts players in
    // the dark, and rouses the whole pack when one of them tangles with a foe.

    /** Legacy {@code isOnLadder}: rats (and hellrats) scuttle straight up any wall they run into. */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Darkness-only hunting (legacy findPlayerToAttack / attackEntity): the hellrat only stalks players in
        // gloom — it acquires the nearest visible player within 16 blocks when it's dark, and loses interest
        // (occasionally) once the light around it rises, mirroring getBrightness < 0.5F.
        float brightness = this.getLightLevelDependentMagicValue();
        LivingEntity target = this.getTarget();
        if (brightness < 0.5F) {
            if (target == null) {
                Player prey = level.getNearestPlayer(this, 16.0D);
                if (prey != null && prey.isAlive() && !prey.isCreative() && !prey.isSpectator()
                        && this.hasLineOfSight(prey)) {
                    // Legacy findPlayerToAttack only sets THIS rat's own target (with its own line-of-sight
                    // check); it does not rally the pack. Pack aggro fires solely from the damage path below.
                    this.setTarget(prey);
                }
            }
        } else if (target instanceof Player && this.random.nextInt(100) == 0) {
            // In the light the rat skittishly gives up the chase now and then.
            this.setTarget(null);
        }
    }

    /**
     * Legacy pack aggro ({@code attackEntityFrom}): when a hellrat locks onto a foe, every other hellrat
     * within a 16x4x16 box that has no target of its own piles onto the same victim.
     */
    private void alertPack(ServerLevel level, LivingEntity victim) {
        AABB area = this.getBoundingBox().inflate(16.0D, 4.0D, 16.0D);
        List<MoCEntityHellRat> pack = level.getEntitiesOfClass(MoCEntityHellRat.class, area,
                r -> r != this && r.isAlive() && r.getTarget() == null);
        for (MoCEntityHellRat rat : pack) {
            rat.setTarget(victim);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Retaliate and summon the swarm: a struck hellrat turns on its attacker and drags the pack in.
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.setTarget(attacker);
            alertPack(level, attacker);
        }
        return hurt;
    }
}
