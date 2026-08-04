package drzhark.mocreatures.neoforge;

import com.mojang.serialization.MapCodec;
import drzhark.mocreatures.registry.MoCSpawns;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * NeoForge-native carrier for the Mo'Creatures biome spawn lists.
 *
 * <p>Why this exists rather than reusing Architectury's {@code BiomeModifications} (which the Fabric module
 * does): on NeoForge, biome modifiers live in the {@code neoforge:biome_modifier} <em>datapack</em> registry
 * and are applied by {@code ServerLifecycleHooks.runModifiers}, which iterates that registry's elements.
 * Architectury registers the serializer for its own modifier but ships no {@code data/.../neoforge/biome_modifier}
 * entry that references it, so its modifier is never instantiated and every {@code addProperties} callback is
 * silently discarded — the reason no Mo'Creatures mob spawned naturally on NeoForge.</p>
 *
 * <p>Registering our own modifier (serializer in {@link MoCreaturesNeoForge}, entry in
 * {@code data/mocreatures/neoforge/biome_modifier/spawns.json}) also avoids the trap in the obvious workaround:
 * shipping an {@code architectury:none_biome_mod_codec} entry would replay <em>every</em> Architectury mod's
 * modifications once per such entry in the pack, doubling spawn weights as soon as a second mod ships the same
 * file.</p>
 */
public final class MoCBiomeSpawnModifier implements BiomeModifier {

    public static final MoCBiomeSpawnModifier INSTANCE = new MoCBiomeSpawnModifier();
    /** No fields to serialise: the entry JSON is just {@code {"type": "mocreatures:spawns"}}. */
    public static final MapCodec<MoCBiomeSpawnModifier> CODEC = MapCodec.unit(INSTANCE);

    private MoCBiomeSpawnModifier() {}

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
        public void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max) {
            this.spawns.addSpawn(category, weight, new MobSpawnSettings.SpawnerData(type, min, max));
        }
    }
}
