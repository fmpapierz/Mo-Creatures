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
        // Legacy constructor (MoCEntityKomodo:48-62): 1 in 6 hatch small (30-69), the rest spawn near-grown
        // (90-119), and none start adult. Without this every komodo sat at the base age of 50 forever, which
        // is what made the age>90 egg drop unreachable.
        setAdult(false);
        setMoCAge(this.random.nextInt(6) == 0 ? 30 + this.random.nextInt(40) : 90 + this.random.nextInt(30));
    }

    /** Legacy growth (MoCEntityKomodo:176-181): a non-adult ages on a 1-in-500 tick and matures at 120. */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !getIsAdult() && this.random.nextInt(500) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 120) {
                setAdult(true);
            }
        }
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

    // Legacy komodos never lay eggs while alive — the egg is a death drop only (see MoCBehavior.dropLoot).
    // The passive egg-laying tick that used to live here was a port invention with no legacy counterpart.
}
