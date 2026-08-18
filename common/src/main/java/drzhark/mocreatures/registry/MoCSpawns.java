package drzhark.mocreatures.registry;

import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import drzhark.mocreatures.config.MoCConfig;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/** Spawn placement rules + per-dimension biome spawns for every Mo'Creatures entity. */
public final class MoCSpawns {

    private MoCSpawns() {}

    public static void register() {
        SpawnPlacementsRegistry.register(MoCEntities.BUNNY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BEAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        // BIG_CAT: legacy getCanSpawnHere (MoCEntityBigCat:489) began with `if (MoCTools.isNearTorch(this)) return
        // false;` — a big cat never spawns near a man-made light source (torch/glowstone/lit-lamp/jack-o'-lantern
        // within ~8 blocks). Mirror the crocodile/scorpion ports: reject the spawn wherever block light >= 8, then
        // defer to the vanilla animal rules. (Placed on the predicate here since MoCEntityBigCat has no override.)
        SpawnPlacementsRegistry.register(MoCEntities.BIG_CAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) < 8
                        && checkMoCAnimalSpawnRules(type, level, reason, pos, random));
        SpawnPlacementsRegistry.register(MoCEntities.BIRD, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BOAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CROCODILE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DEER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DUCK, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.ELEPHANT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FOX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.GOAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.HORSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.KITTY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.KOMODO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MOUSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.OSTRICH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.PET_SCORPION, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SNAKE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.TURKEY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.TURTLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WYVERN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MAGGOT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SNAIL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        // DOLPHIN genuinely is a surface animal, so it keeps vanilla's surface rule (sea level-13 .. sea level).
        SpawnPlacementsRegistry.register(MoCEntities.DOLPHIN, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        // The other four live at any depth. checkSurfaceWaterAnimalSpawnRules pins spawns to the top 14 blocks of
        // the column (Y 50..63 at default sea level) regardless of ocean depth, which kept fishy/jellyfish/ray/shark
        // out of everything below the surface layer; allow the whole column up to sea level instead.
        SpawnPlacementsRegistry.register(MoCEntities.FISHY, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.JELLYFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.RAY, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SHARK, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BEE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BUTTERFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DRAGONFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FIREFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CRICKET, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.ROACH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        // GOLEM: legacy getCanSpawnHere (MoCEntityGolem:1272) required frequency>0 AND canBlockSeeTheSky(x,y,z) AND
        // posY>50 AND super (monster darkness) — golems only spawn at night on the open surface above Y50. AND that
        // surface-only gate (can-see-sky + Y>50) onto the vanilla monster rules so golems no longer spawn underground.
        SpawnPlacementsRegistry.register(MoCEntities.GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 50 && level.canSeeSky(pos)
                        && net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        // OGRE: darkness-gated in the overworld as always, but any-light inside the Ogre Lair —
        // the lair's biome json spawns them as its resident population, day and night.
        SpawnPlacementsRegistry.register(MoCEntities.OGRE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == drzhark.mocreatures.item.MoCStaffPortalItem.OGRE_LAIR
                        ? net.minecraft.world.entity.monster.Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)
                        : net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        // OGRE_PRINCE: boss — any-light rules (princes hunt day and night; they only appear in the
        // lair's biome spawner list), plus exclusivity: never place a second prince within 96 blocks.
        SpawnPlacementsRegistry.register(MoCEntities.OGRE_PRINCE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        net.minecraft.world.entity.monster.Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)
                                && level.getEntitiesOfClass(drzhark.mocreatures.entity.monster.MoCEntityOgrePrince.class,
                                        new net.minecraft.world.phys.AABB(pos).inflate(96.0D)).isEmpty());
        SpawnPlacementsRegistry.register(MoCEntities.MEDUSA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MINOTAUR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.RAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SCORPION, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WILD_WOLF, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WEREWOLF, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        // HELL_RAT / FLAME_WRAITH are Nether mobs (both are fireImmune) and are only listed in Nether biomes below.
        // checkMonsterSpawnRules' darkness gate would confine them to unlit pockets; vanilla Nether mobs use the
        // any-light rule instead, so they can appear anywhere in the dimension.
        SpawnPlacementsRegistry.register(MoCEntities.HELL_RAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FLAME_WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules);

        // ------------------------------------------------ ported from Mo'Creatures 12.0.5
        SpawnPlacementsRegistry.register(MoCEntities.ANT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.RACCOON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CHIMPANZEE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MOLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.ENT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkMoCAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SMALL_FISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MEDIUM_FISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoCSpawns::checkDeepWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SILVER_SKELETON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MINI_GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        // MANTICORE is a Nether mob (fire-immune), so it uses the any-light rule its Nether neighbours do
        // rather than checkMonsterSpawnRules darkness gate. MANTICORE_PET never spawns naturally — the only
        // way to get one is to hatch a manticore egg.
        SpawnPlacementsRegistry.register(MoCEntities.MANTICORE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.HORSE_MOB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
    }

    /**
     * Ground blocks a Mo'Creatures land animal may spawn on, from
     * {@code data/mocreatures/tags/block/animals_spawnable_on.json}.
     *
     * <p>Vanilla's {@code minecraft:animals_spawnable_on} contains <em>grass_block and nothing else</em>, which is
     * why vanilla gives rabbits, camels, armadillos, goats and foxes their own wider tags. Mo'Creatures animals
     * were all using the bare {@code Animal.checkAnimalSpawnRules}, so not one of them could spawn on sand,
     * terracotta, snow, stone, mycelium or mud — deserts, badlands, beaches, snowy plains, stony peaks and
     * mushroom fields had no Mo'Creatures fauna at all no matter what the biome-group table allowed. This tag
     * unions the vanilla per-species tags with the ordinary surface blocks, so every overworld biome has ground
     * a creature can stand on.</p>
     */
    private static final TagKey<net.minecraft.world.level.block.Block> ANIMALS_SPAWNABLE_ON =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(
                            drzhark.mocreatures.MoCreatures.MOD_ID, "animals_spawnable_on"));

    /**
     * Mo'Creatures' replacement for {@code Animal.checkAnimalSpawnRules}: identical light rule (raw brightness
     * above 8 unless the spawn reason ignores light, which keeps creatures out of unlit caves), but checks
     * {@link #ANIMALS_SPAWNABLE_ON} instead of the grass-block-only vanilla tag.
     */
    private static boolean checkMoCAnimalSpawnRules(EntityType<? extends net.minecraft.world.entity.Mob> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        boolean lit = net.minecraft.world.entity.EntitySpawnReason.ignoresLightRequirements(reason)
                || level.getRawBrightness(pos, 0) > 8;
        return lit && level.getBlockState(pos.below()).is(ANIMALS_SPAWNABLE_ON);
    }

    /**
     * Water-column spawn rule for the aquatics that are not surface animals: any fully submerged position at or
     * below sea level. Vanilla's {@code checkSurfaceWaterAnimalSpawnRules} additionally requires
     * {@code y >= seaLevel - 13}, which is what previously kept them in the top 14 blocks.
     */
    private static boolean checkDeepWaterAnimalSpawnRules(EntityType<? extends net.minecraft.world.entity.Mob> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        // Upper bound: at or below sea level. Lower bound: still within the ocean column rather than an
        // underground aquifer or a flooded cave — the deepest vanilla ocean floor sits around 40 blocks below
        // sea level, so anything under that is cave water, where sharks and rays have no business.
        return pos.getY() <= level.getSeaLevel()
                && pos.getY() >= level.getSeaLevel() - 40
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    /**
     * A loader-neutral view of the one biome currently being modified.
     *
     * <p>This exists because the two loaders reach biome spawn lists by different routes and only one of them
     * works through Architectury. On Fabric, {@code BiomeModifications.addProperties} is applied by Fabric API's
     * biome-modification hook. On NeoForge, biome modifiers are a <em>datapack registry</em>
     * ({@code neoforge:biome_modifier}): Architectury registers its modifier's serializer but ships no registry
     * entry that uses it, so its {@code addProperties} callbacks are never replayed and every Mo'Creatures spawn
     * entry was silently dropped. The NeoForge module therefore supplies its own {@code BiomeModifier} and feeds
     * it back into {@link #addBiomeSpawns} here, so both loaders run exactly the same rules.</p>
     */
    public interface BiomeSpawnView {
        /** Whether the biome being modified carries {@code tag}. */
        boolean hasTag(TagKey<Biome> tag);

        /** Appends one spawn entry to this biome. */
        void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max);
    }

    /**
     * Adds every Mo'Creatures spawn entry appropriate to the biome behind {@code view}. Called once per biome,
     * per loader. Safe to call on any biome — non-Overworld, non-Nether biomes (including the Wyvern Lair, whose
     * spawns are baked into {@code data/mocreatures/worldgen/biome/wyvern_lair.json}) are left untouched.
     */
    public static void addBiomeSpawns(BiomeSpawnView view) {
        if (view.hasTag(BiomeTags.IS_NETHER)) {
            addNetherSpawns(view);
            return;
        }
        if (!view.hasTag(BiomeTags.IS_OVERWORLD)) {
            return;
        }

        // Which legacy biome groups this biome belongs to (best-effort from vanilla biome tags). An entity with a
        // biomegroup restriction (config / /moc biomegroup) only spawns where its groups intersect this.
        Set<String> bgroups = new java.util.HashSet<>();
        if (view.hasTag(BiomeTags.IS_FOREST)) bgroups.add("forest");
        if (view.hasTag(BiomeTags.IS_JUNGLE)) bgroups.add("jungle");
        if (view.hasTag(BiomeTags.IS_MOUNTAIN) || view.hasTag(BiomeTags.IS_HILL)) bgroups.add("mountain");
        if (view.hasTag(BiomeTags.IS_TAIGA) || view.hasTag(BiomeTags.SPAWNS_COLD_VARIANT_FROGS)) bgroups.add("arctic");
        // The vanilla desert carries no terrain tag of its own, so it is picked out by the structure tag that
        // contains exactly [minecraft:desert]; badlands/savanna round out the legacy 'desert' group.
        if (view.hasTag(BiomeTags.HAS_DESERT_PYRAMID) || view.hasTag(BiomeTags.IS_BADLANDS)
                || view.hasTag(BiomeTags.IS_SAVANNA)) bgroups.add("desert");
        // has_swamp_hut is [minecraft:swamp] only; has_ruined_portal_swamp is [swamp, mangrove_swamp], so both are
        // needed for the mangrove swamp to count as swamp at all.
        boolean swamp = view.hasTag(BiomeTags.HAS_SWAMP_HUT) || view.hasTag(BiomeTags.HAS_RUINED_PORTAL_SWAMP);
        if (swamp) bgroups.add("swamp");
        // 'normal' is the plains-like baseline. Swamp is a wetness flavour layered on ordinary temperate land
        // rather than a terrain type of its own, so it must not suppress the fallback: otherwise a swamp would
        // host only the four species that name "swamp" explicitly and none of the other two dozen.
        if (bgroups.isEmpty() || (swamp && bgroups.size() == 1)) bgroups.add("normal");

        // ------------------------------------------------ ported from Mo'Creatures 12.0.5
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.ANT.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.RACCOON.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.CHIMPANZEE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.MOLE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.ENT.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.SILVER_SKELETON.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.MINI_GOLEM.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BEAR.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BEE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BIG_CAT.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BIRD.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BOAR.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BUNNY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.BUTTERFLY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.CRAB.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.CRICKET.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.CROCODILE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.DEER.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.DRAGONFLY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.DUCK.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.ELEPHANT.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.FIREFLY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.FLY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.FOX.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.GOAT.get());
        // GOLEM: legacy spawned it as a rare night surface monster (MoCEntityGolem.getCanSpawnHere:
        // frequency>0 + can-see-sky + Y>50 + monster darkness, capped at 1/chunk). The placement predicate
        // in register() now explicitly ANDs the legacy can-see-sky + Y>50 gate onto checkMonsterSpawnRules'
        // night gate. Player self-assembly from blocks is an additional legacy path, not a replacement.
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.GOLEM.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.HORSE.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.HORSE_MOB.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.KITTY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.KOMODO.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.MAGGOT.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.MOUSE.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.MEDUSA.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.MINOTAUR.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.OGRE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.OSTRICH.get());
        // PET_SCORPION is intentionally NOT naturally spawned: legacy pet scorpions were only
        // bred from a mother scorpion (baby-on-back), never placed by the spawner.
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.RAT.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.ROACH.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.SCORPION.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.SNAIL.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.SNAKE.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.TURKEY.get());
        addSpawn(bgroups, view, MobCategory.CREATURE, MoCEntities.TURTLE.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.WEREWOLF.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.WILD_WOLF.get());
        addSpawn(bgroups, view, MobCategory.MONSTER, MoCEntities.WRAITH.get());
        // WYVERN is deliberately absent: it is the Wyvern Lair's mob and the Lair biome
        // (data/mocreatures/worldgen/biome/wyvern_lair.json) already lists it at weight 20. Adding it to every
        // overworld biome put wild wyverns in plains and forests and short-circuited the Lair/staff progression.

        // Aquatics only belong in water bodies. Without this gate all five were added to every overworld biome —
        // wasted spawn attempts in deserts and plains, and sharks/rays in village ponds.
        boolean ocean = view.hasTag(BiomeTags.IS_OCEAN);
        boolean river = view.hasTag(BiomeTags.IS_RIVER);
        if (ocean) {
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.DOLPHIN.get());
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.JELLYFISH.get());
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.RAY.get());
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.SHARK.get());
        }
        if (ocean || river) {
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.FISHY.get());
            // Ported from 12.0.5: the small-fish and medium-fish schools are the ordinary background fish of
            // both oceans and rivers, exactly like fishy.
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.SMALL_FISH.get());
            addSpawn(null, view, MobCategory.WATER_CREATURE, MoCEntities.MEDIUM_FISH.get());
        }
    }

    /**
     * The Nether roster. Hell rats and flame wraiths are both {@code fireImmune} Nether natives that were
     * previously only ever added to Overworld biomes; the scorpion is listed here as well because
     * {@code MoCEntityScorpion.selectType} only produces the nether coat when {@code dimension() == NETHER}.
     */
    private static void addNetherSpawns(BiomeSpawnView view) {
        addSpawn(null, view, MobCategory.MONSTER, MoCEntities.HELL_RAT.get());
        addSpawn(null, view, MobCategory.MONSTER, MoCEntities.FLAME_WRAITH.get());
        addSpawn(null, view, MobCategory.MONSTER, MoCEntities.SCORPION.get());
    }

    /**
     * Adds one config-driven spawn entry. The entity's weight and min/max group size are read from
     * {@link MoCConfig} keyed by the entity's lowercase registry id
     * ({@code spawn.<id>.frequency / .min / .max}). When no override is set, the weight falls back to
     * a per-category default and min/max to 1..4.
     *
     * <p>{@code biomeGroups} is the set of legacy biome groups the current biome belongs to; the entry is
     * skipped when the entity is restricted to groups that do not intersect it. Pass {@code null} for entries
     * placed by an explicit dimension/terrain gate rather than by biome group (the Nether roster, aquatics),
     * where the legacy overworld-terrain groups have no meaning.</p>
     *
     * <p>Note: the legacy 1.12.2 CustomSpawner assigned a distinct per-entity frequency to every
     * creature (e.g. horses, bears and elephants did not share one weight). The port collapses that
     * to the uniform per-category fallback above, which is an accepted simplification: any server
     * wanting the old varied weights can restore them per entity via {@code spawn.<id>.frequency}
     * in {@code mocreatures.properties}.</p>
     */
    private static void addSpawn(@Nullable Set<String> biomeGroups,
            BiomeSpawnView view, MobCategory category, EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        MoCConfig cfg = MoCConfig.get();
        // Biome-group restriction (legacy CustomSpawner): if this entity is assigned any groups, it only
        // spawns in biomes belonging to one of them. No assignment (the default) = spawn everywhere.
        if (biomeGroups != null) {
            java.util.List<String> assigned = cfg.biomeGroups(id);
            if (!assigned.isEmpty() && java.util.Collections.disjoint(assigned, biomeGroups)) {
                return;
            }
        }
        int weight = cfg.spawnFrequency(id);
        if (weight <= 0) {
            return;
        }
        int min = cfg.spawnMin(id);
        int max = cfg.spawnMax(id);
        if (max < min) {
            max = min;
        }
        view.addSpawn(category, type, weight, min, max);
    }
}
