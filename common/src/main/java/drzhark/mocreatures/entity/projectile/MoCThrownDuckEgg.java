package drzhark.mocreatures.entity.projectile;

import drzhark.mocreatures.entity.passive.MoCEntityDuck;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * The thrown {@code duck_egg} projectile — the duck's answer to the vanilla thrown egg. Like a vanilla egg
 * spawning a chick, on impact it has a ~1-in-8 chance to hatch a baby {@link MoCEntityDuck} at the point of
 * impact (otherwise it just splats). Distinct from {@link MoCThrownEgg} (the collectable creature-hatching
 * {@code mocegg}); a duck egg is an ordinary food/crafting egg you can also throw.
 */
public class MoCThrownDuckEgg extends ThrowableItemProjectile {

    public MoCThrownDuckEgg(EntityType<? extends MoCThrownDuckEgg> type, Level level) {
        super(type, level);
    }

    public MoCThrownDuckEgg(Level level, LivingEntity shooter) {
        super(MoCEntities.THROWN_DUCK_EGG.get(), shooter, level, new ItemStack(MoCItems.DUCK_EGG.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return MoCItems.DUCK_EGG.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            // Vanilla egg odds: ~1 in 8 throws hatches a duckling at the splat point.
            if (this.random.nextInt(8) == 0) {
                MoCEntityDuck duck = MoCEntities.DUCK.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                if (duck != null) {
                    duck.setAdult(false);
                    duck.setMoCAge(35);
                    duck.setAge(-24000); // renders as a duckling and grows up
                    duck.setPos(this.getX(), this.getY(), this.getZ());
                    duck.setYRot(this.getYRot());
                    serverLevel.addFreshEntity(duck);
                }
            }
            this.discard();
        }
    }
}
