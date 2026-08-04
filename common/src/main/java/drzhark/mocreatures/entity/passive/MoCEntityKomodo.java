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
 * Port of the legacy {@code MoCEntityKomodo}. A large lizard with a single texture variant.
 */
public class MoCEntityKomodo extends MoCAnimal {

    public MoCEntityKomodo(EntityType<? extends MoCEntityKomodo> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("komododragon.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.SNAKEHISS.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.SNAKEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.SNAKEDYING.get();
    }

    /** Komodo dragons deliver a nasty toxic bite: Poison II for 6 seconds on top of the melee damage. */
    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof net.minecraft.world.entity.LivingEntity victim) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 120, 1), this);
        }
        return hit;
    }

    /** Adult komodos occasionally lay an egg while resting on the ground, capped by local population. */
    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level
                && getIsAdult() && this.onGround() && level.getRandom().nextInt(4000) == 0
                && level.getEntitiesOfClass(MoCEntityKomodo.class, this.getBoundingBox().inflate(16.0D)).size() < 6) {
            MoCEntityEgg egg = new MoCEntityEgg(drzhark.mocreatures.registry.MoCEntities.EGG.get(), level);
            egg.setTypeMoC(MoCEntityEgg.TYPE_KOMODO);
            egg.setPos(this.getX(), this.getY(), this.getZ());
            level.addFreshEntity(egg);
            level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.TURTLE_LAY_EGG,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }
}
