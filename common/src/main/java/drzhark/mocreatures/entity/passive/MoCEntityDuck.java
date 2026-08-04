package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityDuck}. A small passive duck with a single variant.
 */
public class MoCEntityDuck extends MoCAnimal {

    /** Ticks until this duck lays its next duck egg (chicken-style ambient behaviour). */
    private int eggTimer;

    public MoCEntityDuck(EntityType<? extends MoCEntityDuck> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void tick() {
        super.tick();
        // Legacy onLivingUpdate glide: while airborne and descending, ducks flutter down at
        // 60% descent speed (softer landings / reduced fall damage). Runs on both sides.
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level) {
            // Lazily seed the timer on first server tick.
            if (this.eggTimer <= 0) {
                this.eggTimer = level.getRandom().nextInt(6000) + 6000;
            }
            if (--this.eggTimer <= 0) {
                this.eggTimer = level.getRandom().nextInt(6000) + 6000;
                if (getIsAdult()) {
                    this.spawnAtLocation(level, drzhark.mocreatures.registry.MoCItems.DUCK_EGG.get());
                    this.level().playSound(null, this.blockPosition(),
                            net.minecraft.sounds.SoundEvents.CHICKEN_EGG,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        // Legacy MoCEntityDuck.canDespawn() returns true unconditionally: ducks are untamable
        // and always despawn when far from players.
        return true;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("duck.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.DUCK.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.DUCKHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.DUCKHURT.get();
    }
}
