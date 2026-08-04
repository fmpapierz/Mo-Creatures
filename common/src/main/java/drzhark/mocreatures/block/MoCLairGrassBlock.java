package drzhark.mocreatures.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * Living lair grass block. Ports the legacy {@code MoCBlockGrass} spread/revert behaviour to the
 * 26.2 random-tick API, using vanilla {@code SpreadingSnowyBlock} light-dampening semantics in place
 * of the legacy explicit light thresholds (revert when top light &lt; 4 and opacity &gt; 2, spread
 * when top light &ge; 9) - behaviourally equivalent for lair terrain:
 * <ul>
 *   <li>If the block above blocks too much light, this grass reverts to its matching lair
 *       dirt block.</li>
 *   <li>Otherwise, when bright enough, it spreads onto adjacent lair dirt blocks that are lit
 *       and have a light-passing block above, converting them into this grass.</li>
 * </ul>
 * The target dirt block is supplied lazily via a {@link Supplier} so registration order and the
 * lazy resolution of Architectury {@code RegistrySupplier}s never cause a circular-init problem.
 * Wyvern grass spreads onto wyvern dirt; ogre grass onto ogre dirt.
 */
public class MoCLairGrassBlock extends Block {

    private final Supplier<? extends Block> dirtBlock;

    public MoCLairGrassBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> dirtBlock) {
        super(properties);
        this.dirtBlock = dirtBlock;
    }

    /** Mirrors vanilla SpreadingSnowyBlock#canStayAlive: the block above must not dampen light too much. */
    private static boolean canBeGrass(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.getFluidState().isFull()) {
            return false;
        }
        int lightDampeningTopFace =
                LightEngine.getLightDampeningInto(state, aboveState, Direction.UP, aboveState.getLightDampening());
        return lightDampeningTopFace < 15;
    }

    /** Mirrors vanilla SpreadingSnowyBlock#canPropagate. */
    private static boolean canPropagate(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return canBeGrass(state, level, pos) && !level.getFluidState(above).is(FluidTags.WATER);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // Ambient "depthsuspend" wisp of the Wyvern Lair: an end-portal mote drifting up off the living grass.
        if (random.nextInt(75) == 0) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.1D, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 0.5D, random.nextDouble() * 0.2D, (random.nextDouble() - 0.5D) * 0.5D);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block dirt = this.dirtBlock.get();
        if (dirt == null) {
            return;
        }

        if (!canBeGrass(state, level, pos)) {
            // Too dark / covered -> revert to the matching lair dirt. (randomTick only fires on ticking
            // chunks in 26.2, so the old isAreaLoaded guard is unnecessary.)
            level.setBlockAndUpdate(pos, dirt.defaultBlockState());
        } else {
            if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
                BlockState grassState = this.defaultBlockState();
                for (int i = 0; i < 45; i++) {
                    BlockPos testPos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                    if (level.getBlockState(testPos).is(dirt) && canPropagate(grassState, level, testPos)) {
                        level.setBlockAndUpdate(testPos, grassState);
                    }
                }
            }
        }
    }
}
