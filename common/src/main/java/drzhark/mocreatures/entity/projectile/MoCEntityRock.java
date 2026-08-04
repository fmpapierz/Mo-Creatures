package drzhark.mocreatures.entity.projectile;

import drzhark.mocreatures.registry.MoCEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A real thrown rock projectile, modelled on vanilla {@link net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball}.
 * Unlike the snowball it re-skinned, this carries a cobblestone {@link ItemStack} (so it renders as a tumbling
 * cobblestone block) and actually deals impact damage. Hurled by {@code MoCEntityGolem}.
 */
public class MoCEntityRock extends ThrowableItemProjectile {

    /** Impact damage dealt to a struck entity. */
    private static final float ROCK_DAMAGE = 5.0F;

    public MoCEntityRock(EntityType<? extends MoCEntityRock> type, Level level) {
        super(type, level);
    }

    public MoCEntityRock(Level level, LivingEntity shooter) {
        super(MoCEntities.ROCK.get(), shooter, level, new ItemStack(Items.COBBLESTONE));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.level() instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, this.damageSources().thrown(this, this.getOwner()), ROCK_DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.1D, 0.1D, 0.1D, 0.0D);
            this.discard();
        }
    }
}
