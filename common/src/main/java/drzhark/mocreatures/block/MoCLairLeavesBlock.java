package drzhark.mocreatures.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lair leaves that wither when cut off from their tree. Revives the legacy {@code MoCBlockLeaf} decay: a
 * random-ticking leaf with no matching lair log within 4 blocks removes itself (like vanilla leaves losing
 * support). Shears / silk-touch / sapling drops are handled by the block's loot table.
 *
 * <p>The matching lair log is supplied lazily so registration order and Architectury's {@code RegistrySupplier}
 * lazy resolution never cause a circular-init problem.
 */
public class MoCLairLeavesBlock extends Block {

    private final Supplier<? extends Block> logBlock;

    public MoCLairLeavesBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> logBlock) {
        super(properties);
        this.logBlock = logBlock;
    }

    /** Max leaf-steps a lair log may be from this leaf before it withers (legacy {@code byte0 = 4}). */
    private static final int MAX_DISTANCE = 4;

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block log = this.logBlock.get();
        if (log == null || hasLogNearby(level, pos, log)) {
            return;
        }
        // No supporting log nearby -> wither away. Legacy MoCBlockLeaf.removeLeaves dropped the block as an item
        // (quantityDropped ~1/20 -> a lair sapling) before clearing the cell. Run the block's loot table via
        // dropResources so a decaying lair leaf still yields its sapling, then remove without a second drop.
        Block.dropResources(state, level, pos);
        level.removeBlock(pos, false);
    }

    /**
     * Legacy-faithful connected-tree check. The original {@code MoCBlockLeaf.updateTick} seeded a log cell at
     * distance 0 and flooded outward one ring per iteration (up to 4) exclusively through adjacent lair leaves,
     * only decaying when the center leaf was never reached. This reproduces that gradient with a bounded BFS: it
     * walks 6-connected steps starting from this leaf's own position, stepping ONLY through matching lair leaves,
     * and returns true as soon as a lair log is found within {@value #MAX_DISTANCE} leaf-steps. A disconnected
     * log of the same block that merely happens to sit within a 9x9x9 cube no longer counts.
     */
    private boolean hasLogNearby(ServerLevel level, BlockPos pos, Block log) {
        Block leaf = this; // only step through this exact lair-leaves block

        // (randomTick only fires on ticking chunks in 26.2, and the BFS steps at most MAX_DISTANCE blocks out,
        // so the legacy checkChunksExist guard is unnecessary - the neighbourhood is effectively always loaded.)
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        ArrayDeque<BlockPos> next = new ArrayDeque<>();
        visited.add(pos.immutable());
        frontier.add(pos.immutable());

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dist = 0; dist < MAX_DISTANCE && !frontier.isEmpty(); dist++) {
            while (!frontier.isEmpty()) {
                BlockPos cur = frontier.poll();
                for (Direction dir : Direction.values()) {
                    m.set(cur.getX() + dir.getStepX(), cur.getY() + dir.getStepY(), cur.getZ() + dir.getStepZ());
                    BlockState neighbor = level.getBlockState(m);
                    if (neighbor.is(log)) {
                        return true; // a lair log reachable within MAX_DISTANCE leaf-steps -> stay
                    }
                    if (neighbor.is(leaf)) {
                        BlockPos immutable = m.immutable();
                        if (visited.add(immutable)) {
                            next.add(immutable); // step further out through connected lair leaves next ring
                        }
                    }
                }
            }
            ArrayDeque<BlockPos> swap = frontier;
            frontier = next;
            next = swap; // next is already empty after the swap
        }
        return false;
    }
}
