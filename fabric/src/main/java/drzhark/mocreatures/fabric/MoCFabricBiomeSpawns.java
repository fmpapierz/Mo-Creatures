package drzhark.mocreatures.fabric;

import dev.architectury.hooks.level.biome.BiomeProperties;
import dev.architectury.registry.level.biome.BiomeModifications;
import drzhark.mocreatures.registry.MoCSpawns;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Fabric side of the Mo'Creatures biome spawn lists.
 *
 * <p>Architectury's {@code BiomeModifications} works out of the box here: its Fabric implementation registers
 * itself with Fabric API's biome-modification hook at class-init, so the callback below is replayed for every
 * biome at world load. (The NeoForge implementation only registers a modifier <em>serializer</em> and needs a
 * datapack entry that Architectury does not ship, which is why that loader has its own
 * {@code MoCBiomeSpawnModifier} instead of sharing this path.)</p>
 */
public final class MoCFabricBiomeSpawns {

    private MoCFabricBiomeSpawns() {}

    public static void register() {
        BiomeModifications.addProperties((ctx, props) -> MoCSpawns.addBiomeSpawns(new View(ctx, props)));
    }

    private record View(BiomeModifications.BiomeContext ctx, BiomeProperties.Mutable props)
            implements MoCSpawns.BiomeSpawnView {

        @Override
        public boolean hasTag(TagKey<Biome> tag) {
            return this.ctx.hasTag(tag);
        }

        @Override
        public void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max) {
            this.props.getSpawnProperties().addSpawn(category,
                    new MobSpawnSettings.SpawnerData(type, min, max), weight);
        }
    }
}
