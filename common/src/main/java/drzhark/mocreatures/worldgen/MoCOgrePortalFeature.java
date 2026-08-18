package drzhark.mocreatures.worldgen;

import com.mojang.serialization.Codec;

import drzhark.mocreatures.registry.MoCBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Builds the Ogre Lair PORTAL-FRAME landmark — the dark twin of {@link MoCWyvernPortalFeature}.
 * Identical geometry to the wyvern quartz frame (4x4 platform, stair skirt, four 6-tall corner
 * pillars, lintel rows) but in the Ogre Lair's palette: lair stone platform with a glowstone
 * centre, blackstone stairs, and obsidian pillars/lintels.
 *
 * <p>To make this a single dimension-origin landmark (rather than one frame per chunk), the
 * feature NO-OPs unless its placement origin lies in chunk (0, 0). Each chunk gets exactly one
 * placement attempt (in_square keeps it inside the chunk), so exactly one frame builds per
 * dimension, at the surface heightmap height carried by the placement origin. A block-radius
 * guard is NOT sufficient here: a &plusmn;24-block radius admits the attempts of at least the
 * four chunks around the origin and builds overlapping frames.</p>
 */
public class MoCOgrePortalFeature extends Feature<NoneFeatureConfiguration> {

    public MoCOgrePortalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final BlockPos origin = context.origin();

        // Single-landmark guard: only chunk (0,0)'s one placement attempt builds.
        if ((origin.getX() >> 4) != 0 || (origin.getZ() >> 4) != 0) {
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

        final BlockState platform = MoCBlocks.STONE_OGRE_LAIR.get().defaultBlockState();
        final BlockState center = Blocks.GLOWSTONE.defaultBlockState();
        final BlockState pillar = Blocks.OBSIDIAN.defaultBlockState();
        final BlockState lintel = Blocks.OBSIDIAN.defaultBlockState();

        // Base platform: 4x4 lair-stone floor one layer above ground (legacy y+1 layer).
        for (int nX = x - 2; nX < x + 2; nX++) {
            for (int nZ = z - 2; nZ < z + 2; nZ++) {
                setBlock(level, new BlockPos(nX, y + 1, nZ), platform);
            }
        }

        // Inner 2x2 glowstone centre block (legacy centre marker).
        for (int nX = x - 1; nX < x + 1; nX++) {
            for (int nZ = z - 1; nZ < z + 1; nZ++) {
                setBlock(level, new BlockPos(nX, y + 1, nZ), center);
            }
        }

        // Stair skirt on the two z-facing edges of the base (legacy stair rows at z-3 and z+2).
        for (int nX = x - 2; nX < x + 2; nX++) {
            setBlock(level, new BlockPos(nX, y + 1, z - 3),
                    Blocks.BLACKSTONE_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.SOUTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM));
            setBlock(level, new BlockPos(nX, y + 1, z + 2),
                    Blocks.BLACKSTONE_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.NORTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM));
        }

        // Four obsidian corner pillars, 6 tall (legacy generatePillar at the four base corners).
        buildPillar(level, x - 3, y, z - 3, pillar);
        buildPillar(level, x - 3, y, z + 2, pillar);
        buildPillar(level, x + 2, y, z - 3, pillar);
        buildPillar(level, x + 2, y, z + 2, pillar);

        // Obsidian lintel walls capping the two x-side pillar rows at the top (legacy y+6 wall rows).
        for (int nZ = z - 3; nZ < z + 3; nZ++) {
            setBlock(level, new BlockPos(x - 3, y + 6, nZ), lintel);
            setBlock(level, new BlockPos(x + 2, y + 6, nZ), lintel);
        }
    }

    private void buildPillar(WorldGenLevel level, int x, int y, int z, BlockState pillar) {
        for (int nY = y; nY < y + 6; nY++) {
            setBlock(level, new BlockPos(x, nY, z), pillar);
        }
    }
}
