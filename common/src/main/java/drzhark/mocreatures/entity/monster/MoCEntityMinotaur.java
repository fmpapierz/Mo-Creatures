package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Minotaur — a hostile bull-headed bruiser in three coats (1 = Holstein white, 2 = brown,
 * 3 = black). Between ordinary melee swings it lines up its signature bull charge: when prey
 * stands 5-16 blocks away in open line of sight it thunders in at high speed, and a connecting
 * charge lands a heavy blow with a ravager-grade launch. Voice is the vanilla cow's classic
 * moo pitched well down into a bellow.
 */
public class MoCEntityMinotaur extends MoCMob {

    /** Maximum duration of one charge run, in ticks. */
    private static final int CHARGE_DURATION = 40;
    /** Ticks between consecutive charges. */
    private static final int CHARGE_COOLDOWN = 200;
    /** A charge only starts on a target at least this far away... */
    private static final double CHARGE_MIN_RANGE = 5.0D;
    /** ...and no farther than this. */
    private static final double CHARGE_MAX_RANGE = 16.0D;
    /** The target escaping beyond this distance mid-run aborts the charge. */
    private static final double CHARGE_ABORT_RANGE = 24.0D;
    /** Closing to this distance mid-run lands the impact. */
    private static final double CHARGE_IMPACT_RANGE = 2.2D;
    /** Move-speed multiplier while charging. */
    private static final double CHARGE_SPEED = 2.3D;

    /** Remaining ticks of the current charge run; 0 = not charging. Deliberately not persisted. */
    private int chargeTicks;
    /** Counts down between charges. Deliberately not persisted. */
    private int chargeCooldown;

    public MoCEntityMinotaur(EntityType<? extends MoCEntityMinotaur> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    /** Three coats, uniform roll: 1 = Holstein white, 2 = brown, 3 = black. */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(3) + 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("minotaurbrown.png");
            case 3 -> modelTexture("minotaurblack.png");
            default -> modelTexture("minotaurwhite.png");
        };
    }

    /**
     * The signature bull charge, layered over the inherited {@code MeleeAttackGoal} (which keeps
     * handling the ordinary close-quarters swings).
     *
     * <p>The run is driven through the <em>move control</em>, not the navigation. {@code
     * Mob.serverAiStep} ticks the goals, then the navigation, then {@code customServerAiStep}, and
     * only then the move control (mc262-ref {@code Mob.java:714-750}) — so a {@code
     * navigation.moveTo(target, 2.3)} issued from here would not reach the move control until the
     * <em>next</em> tick, and every {@code MeleeAttackGoal} path recalc (every 4-20 ticks against a
     * moving target) would overwrite the speed back to 1.0 before it got there. Writing the wanted
     * position straight to the move control lands in the same tick and cannot be undercut. It also
     * makes the charge a straight-line rush rather than a pathfound approach, which is the point of
     * a bull charge; the line of sight is checked when the run starts and again at impact.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
        }

        LivingEntity target = this.getTarget();

        if (this.chargeTicks > 0) {
            // Mid-charge. Abort if the prey died or teleported out of reach; otherwise thunder on.
            if (target == null || !target.isAlive() || this.distanceTo(target) > CHARGE_ABORT_RANGE) {
                endCharge();
                return;
            }
            this.chargeTicks--;
            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), CHARGE_SPEED);
            if (this.distanceTo(target) <= CHARGE_IMPACT_RANGE && this.getSensing().hasLineOfSight(target)) {
                // IMPACT: a heavy horn blow plus a ravager-style launch (Ravager.strongKnockback idiom,
                // over the horizontal distance). hurtMarked forces the velocity sync so a hit player's
                // client actually flies. The line-of-sight gate mirrors MeleeAttackGoal.canPerformAttack
                // so the blow cannot land through a wall the prey ducked behind mid-run, and the swing
                // is what drives MoCModelMinotaur's horn toss (state.attackSwing).
                this.swing(InteractionHand.MAIN_HAND);
                target.hurtServer(level, this.damageSources().mobAttack(this), 10.0F);
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double dd = Math.max(Math.sqrt(dx * dx + dz * dz), 0.001D);
                target.push(dx / dd * 2.2D, 0.5D, dz / dd * 2.2D);
                target.hurtMarked = true;
                this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.8F);
                endCharge();
            } else if (this.chargeTicks <= 0) {
                // Ran the full 40 ticks without connecting: wind down and catch its breath.
                endCharge();
            }
            return;
        }

        // Not charging: start a run when prey stands in the charge window with a clear line of sight.
        if (this.chargeCooldown <= 0 && target != null && target.isAlive()) {
            double dist = this.distanceTo(target);
            if (dist >= CHARGE_MIN_RANGE && dist <= CHARGE_MAX_RANGE && this.hasLineOfSight(target)) {
                this.chargeTicks = CHARGE_DURATION;
            }
        }
    }

    /** Ends the current charge run (connected or not) and schedules the next one. */
    private void endCharge() {
        this.chargeTicks = 0;
        this.chargeCooldown = CHARGE_COOLDOWN;
    }

    /**
     * A bovine drops bovine things: 0-2 leather and 0-2 raw beef on top of {@code MoCMob}'s
     * behavior-table loot and chance-based spawn-egg drop.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        int leather = this.random.nextInt(3);
        if (leather > 0) {
            spawnAtLocation(level, new ItemStack(Items.LEATHER, leather));
        }
        int beef = this.random.nextInt(3);
        if (beef > 0) {
            spawnAtLocation(level, new ItemStack(Items.BEEF, beef));
        }
    }

    // ---------------------------------------------------------------------------------------- sounds

    /**
     * The vanilla cow's classic voice set, dropped to a bellow by {@link #getVoicePitch()}. 26.2 has
     * no {@code SoundEvents.COW_AMBIENT} constants any more — cow sounds live in the
     * {@code COW_SOUNDS} variant map (classic/moody), resolved exactly like {@code AbstractCow} does.
     */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).ambientSound().value();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).hurtSound().value();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).deathSound().value();
    }

    /** A minotaur is no dairy cow: every moo comes out as a deep bellow. */
    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
