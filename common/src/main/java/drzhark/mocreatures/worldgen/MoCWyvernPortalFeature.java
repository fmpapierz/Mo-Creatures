package drzhark.mocreatures.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Builds the legacy Wyvern Lair QUARTZ PORTAL-FRAME landmark (see the original
 * {@code drzhark.mocreatures.dimension.MoCWorldGenPortal}). The historic structure was a
 * ~4x5 nether-quartz frame with four corner pillars, a stair-lined base and a quartz lintel.
 *
 * <p>To make this a single dimension-origin landmark (rather than one frame per chunk), the
 * feature NO-OPs unless its placement origin falls within {@link #ORIGIN_RADIUS} blocks of
 * world (x=0, z=0). It builds exactly one frame near the Wyvern Lair origin, at the surface
 * heightmap height carried by the placement origin.</p>
 */
public class MoCWyvernPortalFeature extends Feature<NoneFeatureConfiguration> {

    /** Max chebyshev distance (in blocks) from world origin at which the frame is allowed to build. */
    private static final int ORIGIN_RADIUS = 24;

    public MoCWyvernPortalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final BlockPos origin = context.origin();

        // Single-landmark guard: only the placement attempt closest to the dimension origin builds.
        if (Mth.abs(origin.getX()) > ORIGIN_RADIUS || Mth.abs(origin.getZ()) > ORIGIN_RADIUS) {
            return false;
        }

        final int x = origin.getX();
        final int z = origin.getZ();
        // Anchor to the true surface heightmap at the frame centre.
        final int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        final BlockPos base = new BlockPos(x, y, z);

        buildFrame(level, base);
        return true;
    }

    private void buildFrame(WorldGenLevel level, BlockPos c) {
        final int x = c.getX();
        final int y = c.getY();
        final int z = c.getZ();

        final BlockState wall = Blocks.QUARTZ_BLOCK.defaultBlockState();
        final BlockState center = Blocks.QUARTZ_BLOCK.defaultBlockState();
        final BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

        // Base platform: 4x4 quartz floor one layer above ground (legacy y+1 layer).
        for (int nX = x - 2; nX < x + 2; nX++) {
            for (int nZ = z - 2; nZ < z + 2; nZ++) {
                setBlock(level, new BlockPos(nX, y + 1, nZ), wall);
            }
        }

        // Inner 2x2 centre block (legacy centre marker).
        for (int nX = x - 1; nX < x + 1; nX++) {
            for (int nZ = z - 1; nZ < z + 1; nZ++) {
                setBlock(level, new BlockPos(nX, y + 1, nZ), center);
            }
        }

        // Stair skirt on the two z-facing edges of the base (legacy stair rows at z-3 and z+2).
        for (int nX = x - 2; nX < x + 2; nX++) {
            setBlock(level, new BlockPos(nX, y + 1, z - 3),
                    Blocks.QUARTZ_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.SOUTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM));
            setBlock(level, new BlockPos(nX, y + 1, z + 2),
                    Blocks.QUARTZ_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.NORTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM));
        }

        // Four corner pillars, 6 tall (legacy generatePillar at the four base corners).
        buildPillar(level, x - 3, y, z - 3, pillar);
        buildPillar(level, x - 3, y, z + 2, pillar);
        buildPillar(level, x + 2, y, z - 3, pillar);
        buildPillar(level, x + 2, y, z + 2, pillar);

        // Quartz lintel walls capping the two x-side pillar rows at the top (legacy y+6 wall rows).
        for (int nZ = z - 3; nZ < z + 3; nZ++) {
            setBlock(level, new BlockPos(x - 3, y + 6, nZ), wall);
            setBlock(level, new BlockPos(x + 2, y + 6, nZ), wall);
        }
    }

    private void buildPillar(WorldGenLevel level, int x, int y, int z, BlockState pillar) {
        for (int nY = y; nY < y + 6; nY++) {
            setBlock(level, new BlockPos(x, nY, z), pillar);
        }
    }
}
