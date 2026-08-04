package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy {@code MoCEntityShark}. A large aquatic predator.
 */
public class MoCEntityShark extends MoCAquatic {

    public MoCEntityShark(EntityType<? extends MoCEntityShark> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code getCanSpawnHere} / {@code sharkSpawnDif} gate: a shark only spawns when the world
     * difficulty is at least {@code sharkStrength + 1} (the tunable is the legacy spawn-difficulty offset),
     * so with the default 0 sharks never appear on Peaceful. It does NOT scale bite damage.
     */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.EntitySpawnReason reason) {
        int minDiff = drzhark.mocreatures.config.MoCConfig.get().sharkStrength + 1;
        return level.getDifficulty().getId() >= minDiff && super.checkSpawnRules(level, reason);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy FindTarget(this, 16): an untamed adult shark hunts ANY in-water EntityLiving within range,
        // EXCEPT other MoCEntityAquatic (this also covers dolphins, which the legacy MoCEntityAquatic clause
        // excluded before its dolphin-specific test, so no dolphin is ever targeted), eggs (not LivingEntity in
        // the port, so implicitly excluded), players (handled by the base Player target goal), and wolves/horses
        // unless their config flag is enabled. This replaces the old Wolf/Horse-only goals with the full legacy set.
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                (e, sl) -> !this.getIsTamed() && this.getIsAdult() && e.isInWater()
                        && !(e instanceof MoCAquatic)
                        && !(e instanceof net.minecraft.world.entity.player.Player)
                        && (!(e instanceof net.minecraft.world.entity.animal.wolf.Wolf) || cfg.attackWolves)
                        && (!(e instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse) || cfg.attackHorses)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("shark.png");
    }
}
