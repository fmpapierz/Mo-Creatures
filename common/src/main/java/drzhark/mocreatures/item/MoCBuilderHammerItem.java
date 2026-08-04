package drzhark.mocreatures.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Builder Hammer — a long-range block placer (faithful to the legacy {@code ItemBuilderHammer} /
 * {@code ItemOgreHammer}; both hammers share this behaviour). Ray-marches to the first solid surface
 * and places the first block found on the player's hotbar against it, at a distance.
 */
public class MoCBuilderHammerItem extends Item {

    public MoCBuilderHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            BlockPos target = null;
            for (double d = 3.0D; d <= 128.0D; d += 0.5D) {
                BlockPos pos = BlockPos.containing(eye.add(look.scale(d)));
                BlockState bs = level.getBlockState(pos);
                if (!bs.isAir() && !bs.canBeReplaced()) {
                    target = BlockPos.containing(eye.add(look.scale(d - 0.5D)));
                    break;
                }
            }
            if (target != null && level.getBlockState(target).canBeReplaced()) {
                for (int i = 0; i <= 8; i++) {
                    ItemStack slot = player.getInventory().getItem(i);
                    if (slot.getItem() instanceof BlockItem blockItem) {
                        // Faithful to the legacy hammer, which placed the held block WITH its
                        // metadata/state (spruce planks, coloured wool, specific slab, etc.). Derive
                        // the placement state from the held stack via a BlockPlaceContext aimed at the
                        // (replaceable) target cell, so the variant/orientation is preserved instead of
                        // discarding it with a bare defaultBlockState().
                        Direction face = Direction.getApproximateNearest(look.x, look.y, look.z).getOpposite();
                        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), face, target, false);
                        BlockPlaceContext ctx = new BlockPlaceContext(player, hand, slot, hit);
                        BlockState state = blockItem.getBlock().getStateForPlacement(ctx);
                        if (state == null) {
                            state = blockItem.getBlock().defaultBlockState();
                        }
                        level.setBlock(target, state, 3);
                        level.playSound(null, target, state.getSoundType().getPlaceSound(),
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (!player.getAbilities().instabuild) {
                            slot.shrink(1);
                        }
                        break;
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
