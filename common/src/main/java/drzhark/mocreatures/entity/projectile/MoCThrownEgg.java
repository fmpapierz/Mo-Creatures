package drzhark.mocreatures.entity.projectile;

import drzhark.mocreatures.entity.passive.MoCEntityEgg;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * The thrown {@code mocegg} projectile (modelled on the vanilla snowball / {@link MoCEntityRock}). When it
 * strikes a block or entity it hatches: a {@link MoCEntityEgg} of a random species is spawned at the impact
 * point, rests on the ground and — via {@code MoCEntityEgg.tick} — hatches into that species' baby.
 */
public class MoCThrownEgg extends ThrowableItemProjectile {

    public MoCThrownEgg(EntityType<? extends MoCThrownEgg> type, Level level) {
        super(type, level);
    }

    public MoCThrownEgg(Level level, LivingEntity shooter) {
        super(MoCEntities.THROWN_EGG.get(), shooter, level, new net.minecraft.world.item.ItemStack(MoCItems.MOCEGG.get()));
    }

    /** Throws the exact item stack, so its stored {@code EggType} subtype travels with the projectile. */
    public MoCThrownEgg(Level level, LivingEntity shooter, net.minecraft.world.item.ItemStack stack) {
        super(MoCEntities.THROWN_EGG.get(), shooter, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return MoCItems.MOCEGG.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            MoCEntityEgg egg = MoCEntities.EGG.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (egg != null) {
                // Deterministic species + variant if the thrown egg carried a legacy composite EggType
                // (1-54: fishy 1-10, shark 11, snakes 21-28, ostrich 30-32, komodo 33, scorpions 41-45,
                // wyverns 50-54); otherwise a random land species (a plain unlabelled "spoiled egg").
                int eggType = 0;
                net.minecraft.world.item.component.CustomData cd =
                        this.getItem().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (cd != null) {
                    eggType = cd.copyTag().getIntOr("EggType", 0);
                }
                if (eggType > 0) {
                    egg.setEggType(eggType);
                } else {
                    egg.setTypeMoC(1 + this.random.nextInt(6));
                }
                egg.setPos(this.getX(), this.getY(), this.getZ());
                egg.setYRot(this.getYRot());
                serverLevel.addFreshEntity(egg);
            }
            this.discard();
        }
    }
}
