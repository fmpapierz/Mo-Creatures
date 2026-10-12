package drzhark.mocreatures.forge;

import com.mojang.serialization.MapCodec;
import drzhark.mocreatures.registry.MoCSpawns;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Forge's biome modifier for Mo'Creatures' natural spawns: the NeoForge module's MoCBiomeSpawnModifier on Forge's
 * (identical) biome modifier API. Every biome gets the shared {@link MoCSpawns} rules in the ADD phase.
 */
public final class MoCForgeBiomeSpawns implements BiomeModifier {

    public static final MoCForgeBiomeSpawns INSTANCE = new MoCForgeBiomeSpawns();
    public static final MapCodec<MoCForgeBiomeSpawns> CODEC = MapCodec.unit(INSTANCE);

    private MoCForgeBiomeSpawns() {}

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // ADD is the phase for appending to spawn lists; the other phases would apply the same entries again.
        if (phase != Phase.ADD) {
            return;
        }
        MoCSpawns.addBiomeSpawns(new View(biome, builder.getMobSpawnSettings()));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }

    private record View(Holder<Biome> biome, MobSpawnSettings.Builder spawns) implements MoCSpawns.BiomeSpawnView {

        @Override
        public boolean hasTag(TagKey<Biome> tag) {
            return this.biome.is(tag);
        }

        @Override
        public Identifier biomeId() {
            return this.biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
        }

        @Override
        public void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max) {
            this.spawns.addSpawn(category, weight, new MobSpawnSettings.SpawnerData(type, min, max));
        }
    }
}
