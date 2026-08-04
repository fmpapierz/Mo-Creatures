package drzhark.mocreatures.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.block.MoCLairGrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Wyvern Lair / Ogre Lair blocks, registered cross-loader via Architectury. Generated. */
public final class MoCBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.BLOCK);

    private MoCBlocks() {}

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, name));
    }

    public static final RegistrySupplier<Block> DIRT_WYVERN_LAIR = BLOCKS.register("dirt_wyvern_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(0.5F).setId(key("dirt_wyvern_lair"))));
    public static final RegistrySupplier<Block> GRASS_WYVERN_LAIR = BLOCKS.register("grass_wyvern_lair",
            () -> new MoCLairGrassBlock(BlockBehaviour.Properties.of().strength(0.6F).randomTicks().setId(key("grass_wyvern_lair")),
                    DIRT_WYVERN_LAIR));
    public static final RegistrySupplier<Block> STONE_WYVERN_LAIR = BLOCKS.register("stone_wyvern_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(1.5F, 6.0F).setId(key("stone_wyvern_lair"))));
    public static final RegistrySupplier<Block> LOG_WYVERN_LAIR = BLOCKS.register("log_wyvern_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0F).setId(key("log_wyvern_lair"))));
    public static final RegistrySupplier<Block> LEAVES_WYVERN_LAIR = BLOCKS.register("leaves_wyvern_lair",
            () -> new drzhark.mocreatures.block.MoCLairLeavesBlock(
                    BlockBehaviour.Properties.of().strength(0.2F).noOcclusion().randomTicks().setId(key("leaves_wyvern_lair")),
                    LOG_WYVERN_LAIR));
    public static final RegistrySupplier<Block> PLANKS_WYVERN_LAIR = BLOCKS.register("planks_wyvern_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0F, 3.0F).setId(key("planks_wyvern_lair"))));
    public static final RegistrySupplier<Block> TALL_GRASS_WYVERN_LAIR = BLOCKS.register("tall_grass_wyvern_lair",
            // A proper vegetation bush (vanilla tall-grass class): it requires a valid soil block below
            // and pops off when unsupported, faithfully restoring the legacy MoCBlockTallGrass (BlockFlower)
            // plant/support behaviour. mayPlaceOn also accepts the lair dirt/grass so it survives on lair terrain.
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion()
                    .setId(key("tall_grass_wyvern_lair"))) {
                @Override
                protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
                    return super.mayPlaceOn(soil, level, pos)
                            || soil.is(GRASS_WYVERN_LAIR.get()) || soil.is(DIRT_WYVERN_LAIR.get());
                }
            });
    public static final RegistrySupplier<Block> DIRT_OGRE_LAIR = BLOCKS.register("dirt_ogre_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(0.5F).setId(key("dirt_ogre_lair"))));
    public static final RegistrySupplier<Block> GRASS_OGRE_LAIR = BLOCKS.register("grass_ogre_lair",
            () -> new MoCLairGrassBlock(BlockBehaviour.Properties.of().strength(0.6F).randomTicks().setId(key("grass_ogre_lair")),
                    DIRT_OGRE_LAIR));
    public static final RegistrySupplier<Block> STONE_OGRE_LAIR = BLOCKS.register("stone_ogre_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(1.5F, 6.0F).setId(key("stone_ogre_lair"))));
    public static final RegistrySupplier<Block> LOG_OGRE_LAIR = BLOCKS.register("log_ogre_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0F).setId(key("log_ogre_lair"))));
    public static final RegistrySupplier<Block> LEAVES_OGRE_LAIR = BLOCKS.register("leaves_ogre_lair",
            () -> new drzhark.mocreatures.block.MoCLairLeavesBlock(
                    BlockBehaviour.Properties.of().strength(0.2F).noOcclusion().randomTicks().setId(key("leaves_ogre_lair")),
                    LOG_OGRE_LAIR));
    public static final RegistrySupplier<Block> PLANKS_OGRE_LAIR = BLOCKS.register("planks_ogre_lair",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0F, 3.0F).setId(key("planks_ogre_lair"))));
    public static final RegistrySupplier<Block> TALL_GRASS_OGRE_LAIR = BLOCKS.register("tall_grass_ogre_lair",
            // See TALL_GRASS_WYVERN_LAIR: vanilla tall-grass vegetation block needing soil support,
            // with mayPlaceOn widened to the ogre-lair dirt/grass so it survives on lair terrain.
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion()
                    .setId(key("tall_grass_ogre_lair"))) {
                @Override
                protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
                    return super.mayPlaceOn(soil, level, pos)
                            || soil.is(GRASS_OGRE_LAIR.get()) || soil.is(DIRT_OGRE_LAIR.get());
                }
            });
}
