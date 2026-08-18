package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Port of the legacy {@code MoCEntityShark}. A large aquatic predator.
 */
public class MoCEntityShark extends MoCAquatic {

    public MoCEntityShark(EntityType<? extends MoCEntityShark> type, Level level) {
        super(type, level);
        // Legacy constructor (MoCEntityShark:27): sharks spawn already part-grown, 100-199. Without this every
        // shark stayed at the base age of 50, so the age>150 egg drop could never fire.
        setAdult(false);
        setMoCAge(100 + this.random.nextInt(100));
    }

    /** Legacy growth (MoCEntityShark:180-189): a non-adult ages on a 1-in-50 tick and matures at 200. */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !getIsAdult() && this.random.nextInt(50) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 200) {
                setAdult(true);
            }
        }
    }

    /**
     * Load-time migration for stale saves: builds where the constructor did not roll the part-grown spawn
     * age persisted every shark with the shared MoCAquatic defaults (Adult:1b, AgeMoC:50). Under
     * {@link #getSizeFactor} that shape renders at 0.5x forever — an adult never re-enters the growth tick.
     * A current-format shark's age never dips below the constructor floor of 100 (and an adult's below the
     * mature age of 200), so an adult at exactly the shared default can only be the stale shape: snap it
     * to the mature age. Mid-growth sharks (Adult:0b) are left untouched.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (getIsAdult() && getMoCAge() == 50) {
            setMoCAge(200);
        }
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
        // The whole prey filter sits behind the enableHunters master switch (legacy MoCProxy.java:314-316,
        // default true); the base player-target goal is untouched — the flag only governs creature prey.
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                (e, sl) -> cfg.enableHunters
                        && !this.getIsTamed() && this.getIsAdult() && e.isInWater()
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

    /**
     * Legacy {@code MoCRenderShark.stretch} (:157-159): a shark renders at exactly {@code edad * 0.01F},
     * uncapped — spawned part-grown (age 100-199) it is already 1.0x-1.99x, and a full adult (age 200)
     * a full 2.0x. {@code MoCMobRenderer.scale} multiplies this factor by the port's shared baby curve
     * ({@code adult ? 1 : 0.5 + 0.5 * min(age,100)/100}), so that curve is divided back out here to
     * reproduce the legacy factor exactly — the same divide-out {@code MoCEntityMediumFish.getSizeFactor}
     * uses. The divisor bottoms out at 0.5 (and is 1.0 for every shark, whose age never dips below 100).
     */
    @Override
    public float getSizeFactor() {
        float legacy = getMoCAge() * 0.01F;
        float sharedCurve = getIsAdult() ? 1.0F : 0.5F + 0.5F * Math.min(getMoCAge(), 100) / 100.0F;
        return legacy / sharedCurve;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("shark.png");
    }
}
