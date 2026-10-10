package drzhark.mocreatures.spawn;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The Forge 1.12.2 {@code BiomeDictionary} types of a 26.2 biome, so 12.0.5's per-species spawn lists (which name
 * those types, e.g. {@code FOREST, PLAINS, SNOWY}) apply exactly as they did there.
 *
 * <p>12.0.5 added each species to every biome {@code BiomeDictionary.getBiomes(type)} returned for any of its types
 * ({@code MoCEntities}:455-470). The vanilla table below is Forge 1.12.2's own {@code registerVanillaBiomes}
 * (read from forge-1.12.2-14.23.5.2847), each 26.2 biome keyed to the 1.12 biome it descends from: the sunflower
 * plains was 1.12's MUTATED_PLAINS, the dark forest its ROOFED_FOREST, the eroded badlands its MUTATED_MESA (which
 * Forge did not tag MESA), and so on. As in Forge, {@code water} is added to every ocean and river biome (WATER was
 * the supertype of OCEAN and RIVER). Types are lowercase.</p>
 *
 * <p>Biomes 1.12 never had get the types Forge's later versions gave them or their nearest 1.12 kin (the mangrove
 * swamp a swamp, the pale garden a dark forest, the meadow a mountain plain, the peaks snowy mountains). The cave
 * biomes are the one real approximation: 1.12 had none, and an underground spawn took the types of the land above,
 * so they get the types of ordinary temperate land ({@code forest, plains, hills, mountain}, plus {@code spooky}
 * for the deep dark). A modded biome is classified from its vanilla tags.</p>
 */
public final class MoCBiomeTypes {

    private MoCBiomeTypes() {}

    private static final Set<String> TEMPERATE_LAND = Set.of("forest", "plains", "hills", "mountain");

    private static final Map<String, Set<String>> VANILLA = Map.ofEntries(
            Map.entry("the_void", Set.of("void")),
            Map.entry("plains", Set.of("plains")),
            Map.entry("sunflower_plains", Set.of("plains", "rare")),
            Map.entry("snowy_plains", Set.of("cold", "snowy", "wasteland")),
            Map.entry("ice_spikes", Set.of("cold", "snowy", "hills", "rare")),
            Map.entry("desert", Set.of("hot", "dry", "sandy")),
            Map.entry("swamp", Set.of("wet", "swamp")),
            Map.entry("mangrove_swamp", Set.of("hot", "wet", "swamp")),
            Map.entry("forest", Set.of("forest")),
            Map.entry("flower_forest", Set.of("forest", "hills", "rare")),
            Map.entry("birch_forest", Set.of("forest")),
            Map.entry("dark_forest", Set.of("spooky", "dense", "forest")),
            Map.entry("pale_garden", Set.of("spooky", "dense", "forest")),
            Map.entry("old_growth_birch_forest", Set.of("forest", "dense", "hills", "rare")),
            Map.entry("old_growth_pine_taiga", Set.of("cold", "coniferous", "forest")),
            Map.entry("old_growth_spruce_taiga", Set.of("dense", "forest", "rare")),
            Map.entry("taiga", Set.of("cold", "coniferous", "forest")),
            Map.entry("snowy_taiga", Set.of("cold", "coniferous", "forest", "snowy")),
            Map.entry("savanna", Set.of("hot", "savanna", "plains", "sparse")),
            Map.entry("savanna_plateau", Set.of("hot", "savanna", "plains", "sparse", "rare")),
            Map.entry("windswept_hills", Set.of("mountain", "hills")),
            Map.entry("windswept_gravelly_hills", Set.of("mountain", "sparse", "rare")),
            Map.entry("windswept_forest", Set.of("mountain", "forest", "sparse")),
            Map.entry("windswept_savanna", Set.of("hot", "dry", "sparse", "savanna", "mountain", "rare")),
            Map.entry("jungle", Set.of("hot", "wet", "dense", "jungle")),
            Map.entry("sparse_jungle", Set.of("hot", "wet", "jungle", "forest", "rare")),
            Map.entry("bamboo_jungle", Set.of("hot", "wet", "dense", "jungle")),
            Map.entry("badlands", Set.of("mesa", "sandy", "dry")),
            Map.entry("eroded_badlands", Set.of("hot", "dry", "sparse", "mountain", "rare")),
            Map.entry("wooded_badlands", Set.of("mesa", "sandy", "dry", "sparse")),
            Map.entry("meadow", Set.of("plains", "mountain")),
            Map.entry("cherry_grove", Set.of("forest", "mountain")),
            Map.entry("grove", Set.of("cold", "coniferous", "forest", "snowy", "mountain")),
            Map.entry("snowy_slopes", Set.of("cold", "sparse", "snowy", "mountain")),
            Map.entry("frozen_peaks", Set.of("cold", "sparse", "snowy", "mountain")),
            Map.entry("jagged_peaks", Set.of("cold", "sparse", "snowy", "mountain")),
            Map.entry("stony_peaks", Set.of("hot", "mountain")),
            Map.entry("river", Set.of("river")),
            Map.entry("frozen_river", Set.of("cold", "river", "snowy")),
            Map.entry("beach", Set.of("beach")),
            Map.entry("snowy_beach", Set.of("cold", "beach", "snowy")),
            Map.entry("stony_shore", Set.of("beach")),
            Map.entry("warm_ocean", Set.of("ocean", "hot")),
            Map.entry("lukewarm_ocean", Set.of("ocean")),
            Map.entry("deep_lukewarm_ocean", Set.of("ocean")),
            Map.entry("ocean", Set.of("ocean")),
            Map.entry("deep_ocean", Set.of("ocean")),
            Map.entry("cold_ocean", Set.of("ocean", "cold")),
            Map.entry("deep_cold_ocean", Set.of("ocean", "cold")),
            Map.entry("frozen_ocean", Set.of("cold", "ocean", "snowy")),
            Map.entry("deep_frozen_ocean", Set.of("cold", "ocean", "snowy")),
            Map.entry("mushroom_fields", Set.of("mushroom", "rare")),
            Map.entry("dripstone_caves", TEMPERATE_LAND),
            Map.entry("lush_caves", TEMPERATE_LAND),
            Map.entry("sulfur_caves", TEMPERATE_LAND),
            Map.entry("deep_dark", Set.of("forest", "plains", "hills", "mountain", "spooky")),
            Map.entry("nether_wastes", Set.of("hot", "dry", "nether")),
            Map.entry("warped_forest", Set.of("hot", "dry", "nether")),
            Map.entry("crimson_forest", Set.of("hot", "dry", "nether")),
            Map.entry("soul_sand_valley", Set.of("hot", "dry", "nether")),
            Map.entry("basalt_deltas", Set.of("hot", "dry", "nether")),
            Map.entry("the_end", Set.of("cold", "dry", "end")),
            Map.entry("end_highlands", Set.of("cold", "dry", "end")),
            Map.entry("end_midlands", Set.of("cold", "dry", "end")),
            Map.entry("small_end_islands", Set.of("cold", "dry", "end")),
            Map.entry("end_barrens", Set.of("cold", "dry", "end")));

    /** The 1.12 types of a biome, from its id (vanilla) or its tags (anything else). */
    public static Set<String> of(@Nullable Identifier id, Predicate<TagKey<Biome>> hasTag) {
        Set<String> types = new HashSet<>();
        Set<String> known = id != null && id.getNamespace().equals("minecraft") ? VANILLA.get(id.getPath()) : null;
        if (known != null) {
            types.addAll(known);
        } else {
            guess(hasTag, types);
        }
        if (types.contains("ocean") || types.contains("river")) {
            types.add("water");
        }
        return types;
    }

    /** Forge-style best guess for a modded biome, from the vanilla biome tags it carries. */
    private static void guess(Predicate<TagKey<Biome>> hasTag, Set<String> types) {
        if (hasTag.test(BiomeTags.IS_NETHER)) {
            types.addAll(Set.of("hot", "dry", "nether"));
            return;
        }
        if (hasTag.test(BiomeTags.IS_END)) {
            types.addAll(Set.of("cold", "dry", "end"));
            return;
        }
        if (hasTag.test(BiomeTags.IS_OCEAN) || hasTag.test(BiomeTags.IS_DEEP_OCEAN)) types.add("ocean");
        if (hasTag.test(BiomeTags.IS_RIVER)) types.add("river");
        if (hasTag.test(BiomeTags.IS_BEACH)) types.add("beach");
        if (hasTag.test(BiomeTags.IS_FOREST)) types.add("forest");
        if (hasTag.test(BiomeTags.IS_TAIGA)) types.addAll(Set.of("cold", "coniferous", "forest"));
        if (hasTag.test(BiomeTags.IS_JUNGLE)) types.addAll(Set.of("hot", "wet", "dense", "jungle"));
        if (hasTag.test(BiomeTags.IS_SAVANNA)) types.addAll(Set.of("hot", "savanna", "plains", "sparse"));
        if (hasTag.test(BiomeTags.IS_BADLANDS)) types.addAll(Set.of("mesa", "sandy", "dry"));
        if (hasTag.test(BiomeTags.IS_MOUNTAIN)) types.add("mountain");
        if (hasTag.test(BiomeTags.IS_HILL)) types.add("hills");
        if (hasTag.test(BiomeTags.HAS_DESERT_PYRAMID) || hasTag.test(BiomeTags.HAS_VILLAGE_DESERT)) {
            types.addAll(Set.of("hot", "dry", "sandy"));
        }
        if (hasTag.test(BiomeTags.HAS_SWAMP_HUT) || hasTag.test(BiomeTags.HAS_RUINED_PORTAL_SWAMP)) {
            types.addAll(Set.of("wet", "swamp"));
        }
        if (hasTag.test(BiomeTags.HAS_VILLAGE_SNOWY) || hasTag.test(BiomeTags.HAS_IGLOO)
                || hasTag.test(BiomeTags.SPAWNS_SNOW_FOXES)) {
            types.addAll(Set.of("cold", "snowy"));
        }
        if (hasTag.test(BiomeTags.HAS_VILLAGE_PLAINS)) types.add("plains");
        if (types.isEmpty() && hasTag.test(BiomeTags.IS_OVERWORLD)) {
            types.add("plains"); // an untagged overworld biome: ordinary open land
        }
    }
}
