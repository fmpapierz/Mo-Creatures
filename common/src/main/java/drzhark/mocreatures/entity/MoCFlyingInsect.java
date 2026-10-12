package drzhark.mocreatures.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Base class for flying Mo'Creatures insects (bee, butterfly, dragonfly, firefly, fly). Reuses the
 * shared Mo'Creatures state from {@link MoCAnimal} and swaps in flight navigation + a hovering move
 * control, mirroring the airborne behaviour of the legacy {@code MoCEntityInsect} flyers.
 */
public abstract class MoCFlyingInsect extends MoCAnimal {

    protected MoCFlyingInsect(EntityType<? extends MoCFlyingInsect> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.60D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    /** Ticks until this insect may buzz again ({@link #buzzNearPlayer}). */
    private int buzzCountdown;

    /**
     * 12.0.5's insect buzz ({@code onLivingUpdate} of its fly and dragonfly): only while a player is within 5 blocks,
     * at most once every {@code interval} ticks. The port used to give them vanilla's ambient sound instead, which
     * every insect plays every few seconds to anyone within 16 blocks; insects never despawn, so the fields buzzed
     * without end while the tiny flies making it stayed out of sight (player report 2026-10-12). Call from
     * {@code customServerAiStep}, and return no ambient sound.
     */
    protected void buzzNearPlayer(ServerLevel level, SoundEvent sound, int interval) {
        if (--this.buzzCountdown <= 0 && level.getNearestPlayer(this, 5.0D) != null) {
            playSound(sound, 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            this.buzzCountdown = interval;
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
