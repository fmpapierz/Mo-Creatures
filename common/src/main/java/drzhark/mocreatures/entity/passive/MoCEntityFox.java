package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityFox}. A small wild fox with a normal and a snow variant.
 */
public class MoCEntityFox extends MoCAnimal {

    public MoCEntityFox(EntityType<? extends MoCEntityFox> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy fox dealt force=2 flat (attackEntityFrom(causeMobDamage(this), 2) -> 2 damage / 1 heart).
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy fox is isNotScared() == true: it never flees (onLivingUpdate zeroes fleeingTick). Strip the
        // PanicGoal that MoCAnimal installs for every animal so a struck fox stands its ground and bites back.
        this.goalSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.PanicGoal);
        // Restore the legacy predator: a wild (untamed) fox chases and bites ANY nearby creature strictly
        // smaller than itself. Legacy getClosestTarget excluded players and hostiles (EntityMob) and required
        // BOTH the fox's height AND width to exceed the prey's, with line of sight. The 1/80-per-tick legacy
        // trigger is approximated by the target goal's own random interval; taming a fox (raw turkey, handled
        // by the data-driven MoCAnimal.mobInteract) stops it hunting via the getIsTamed() gate.
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.3D, true));
        // Legacy attackEntityFrom set entityToAttack = attacker ONLY when worldObj.difficultySetting > 0, so a
        // wild fox struck on Peaceful did NOT retaliate. Gate the retaliation goal on non-Peaceful difficulty.
        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return MoCEntityFox.this.level().getDifficulty() != Difficulty.PEACEFUL && super.canUse();
            }
        });
        // Legacy findPlayerToAttack returned a hunt target only when (rand.nextInt(80)==0 && difficultySetting > 0),
        // so on Peaceful a wild fox was fully passive and hunted nothing. Gate the hunt predicate on non-Peaceful.
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                (living, serverLevel) -> serverLevel.getDifficulty() != Difficulty.PEACEFUL
                        && !this.getIsTamed()
                        && !(living instanceof net.minecraft.world.entity.player.Player)
                        && !(living instanceof net.minecraft.world.entity.monster.Monster)
                        && this.getBbHeight() > living.getBbHeight()
                        && this.getBbWidth() > living.getBbWidth()));
    }

    /**
     * Legacy {@code attackEntity}: when the fox's bite connected against a NON-player it called
     * {@code MoCTools.destroyDrops(this, 3D)}, wiping the fresh loot around the kill (item entities younger
     * than 50 ticks within 3 blocks) so wild foxes left no lootable drops. Mirrors {@link MoCEntityBigCat}'s
     * port; gated on the {@code destroyDrops} config flag.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !(target instanceof net.minecraft.world.entity.player.Player)
                && drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            for (net.minecraft.world.entity.item.ItemEntity ie : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
                if (ie.isAlive() && ie.tickCount < 50) {
                    ie.discard();
                }
            }
        }
        return hit;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Snow foxes (type 2) spawn in snowy/frozen biomes; the red fox (type 1) everywhere else.
            boolean cold = this.level().getBiome(this.blockPosition()).value().getBaseTemperature() <= 0.05F;
            setTypeMoC(cold ? 2 : 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("foxsnow.png");
            default -> modelTexture("fox.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.FOXCALL.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.FOXHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.FOXDYING.get();
    }
}
