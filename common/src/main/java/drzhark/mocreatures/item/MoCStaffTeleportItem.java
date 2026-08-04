package drzhark.mocreatures.item;

import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Staff of Teleport — a short-range blink. Ray-marches from the player's eyes along their look
 * direction and teleports them to the last empty cell before the first solid block (faithful to the
 * legacy {@code ItemStaffTeleport}). Does nothing while riding or being ridden.
 */
public class MoCStaffTeleportItem extends Item {

    public MoCStaffTeleportItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isPassenger() || player.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 dest = null;
            boolean hitWall = false;
            for (double d = 4.0D; d <= 128.0D; d += 0.5D) {
                Vec3 p = eye.add(look.scale(d));
                if (!level.getBlockState(BlockPos.containing(p)).isAir()) {
                    hitWall = true;
                    break;
                }
                dest = p;
            }
            // Legacy ItemStaffTeleport only teleports when a solid block is actually hit; aiming at
            // open sky/terrain with no wall in range is a no-op (no teleport, no durability loss).
            if (dest != null && hitWall) {
                serverPlayer.teleport(new TeleportTransition(server, dest, Vec3.ZERO,
                        player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
                level.playSound(null, serverPlayer.blockPosition(), MoCSounds.APPEARMAGIC.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                // Legacy ItemStaffTeleport: each teleport wears the staff by 1 durability.
                player.getItemInHand(hand).hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
