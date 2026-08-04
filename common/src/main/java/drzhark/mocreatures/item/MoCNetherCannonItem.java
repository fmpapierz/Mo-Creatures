package drzhark.mocreatures.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Nether Cannon — fires a short-range beam of nether flame along the player's look (faithful in spirit
 * to the legacy ranged nether weapon): ignites and damages the first creature hit, or sets fire to the
 * surface it strikes, with a flame trail and a short cooldown. Implemented as a hitscan to avoid a
 * bespoke projectile entity.
 */
public class MoCNetherCannonItem extends Item {

    public MoCNetherCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = eye.add(look.scale(24.0D));
            BlockHitResult hit = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : end;
            double reach = eye.distanceTo(hitPos);
            for (double d = 1.0D; d < reach; d += 0.6D) {
                Vec3 p = eye.add(look.scale(d));
                server.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
            }
            boolean struckEntity = false;
            AABB hitBox = new AABB(hitPos.subtract(1.5D, 1.5D, 1.5D), hitPos.add(1.5D, 1.5D, 1.5D));
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player)) {
                le.igniteForSeconds(8.0F);
                le.hurtServer(server, level.damageSources().playerAttack(player), 6.0F);
                struckEntity = true;
            }
            if (!struckEntity && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
                if (level.getBlockState(firePos).isAir()) {
                    level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 15);
        }
        return InteractionResult.SUCCESS;
    }
}
