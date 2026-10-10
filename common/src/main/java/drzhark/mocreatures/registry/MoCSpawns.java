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
        // MANTICORE: 12.0.5 spawned it in nearly every Overworld biome type AND the Nether (MoCEntities.java:450-453).
        // In the Overworld it keeps legacy MoCEntityMob.getCanSpawnHere's EntityMob darkness rule (a night / cave
        // hunter, like every other overworld monster); in the Nether it takes the any-light rule its fire-immune
        // Nether neighbours use. MANTICORE_PET never spawns naturally — the only way to get one is to hatch the
        // manticore egg a wild manticore drops.
        SpawnPlacementsRegistry.register(MoCEntities.MANTICORE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == net.minecraft.world.level.Level.NETHER
                        ? net.minecraft.world.entity.monster.Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)
                        : net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
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

        /** The biome's registry id, if known (it picks the biome's Forge 1.12 types, see {@code MoCBiomeTypes}). */
        net.minecraft.resources.@Nullable Identifier biomeId();

        /** Appends one spawn entry to this biome. */
        void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max);
    }

    /** One naturally spawning Mo'Creatures entity and the spawn pool 12.0.5 put it in. */
    private record NaturalSpawn(MobCategory category, java.util.function.Supplier<? extends EntityType<?>> type) {}

    /**
     * Every Mo'Creatures entity that spawns naturally, in 12.0.5's own pools: CREATURE animals; AMBIENT insects and
     * critters, which spawn around players all the time with the bats under the ambient cap (12.0.5's
     * {@code MoCEntityAmbient.isCreatureType} counted them as AMBIENT); WATER_CREATURE aquatics; MONSTERs. Where
     * each spawns and how often comes from its 12.0.5 biome-type list and weights in {@link MoCConfig}.
     *
     * <p>Not here: the pet scorpion (12.0.5 only got one from a mother scorpion), the wyvern and the Ogre Prince
     * (residents of the Wyvern and Ogre Lairs, listed in those biomes' json), and the pet manticore (hatched from
     * the egg a wild manticore drops). The golem's placement rule adds legacy's can-see-sky + Y>50 gate, and the
     * manticore's coat follows the biome it spawns in (frozen: blue; Nether: red, with the Fire Egg).</p>
     */
    private static final java.util.List<NaturalSpawn> NATURAL_SPAWNS = java.util.List.of(
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.BEAR),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.BIG_CAT),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.BIRD),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.BOAR),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.BUNNY),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.CHIMPANZEE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.CROCODILE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.DEER),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.DUCK),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.ELEPHANT),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.ENT),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.FOX),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.GOAT),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.HORSE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.KITTY),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.KOMODO),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.MOLE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.MOUSE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.OSTRICH),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.RACCOON),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.SNAKE),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.TURKEY),
            new NaturalSpawn(MobCategory.CREATURE, MoCEntities.TURTLE),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.ANT),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.BEE),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.BUTTERFLY),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.CRAB),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.CRICKET),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.DRAGONFLY),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.FIREFLY),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.FLY),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.MAGGOT),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.ROACH),
            new NaturalSpawn(MobCategory.AMBIENT, MoCEntities.SNAIL),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.DOLPHIN),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.FISHY),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.JELLYFISH),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.RAY),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.SHARK),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.SMALL_FISH),
            new NaturalSpawn(MobCategory.WATER_CREATURE, MoCEntities.MEDIUM_FISH),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.GOLEM),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.OGRE),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.RAT),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.SCORPION),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.WILD_WOLF),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.WEREWOLF),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.WRAITH),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.HELL_RAT),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.FLAME_WRAITH),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.HORSE_MOB),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.SILVER_SKELETON),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.MINI_GOLEM),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.MANTICORE),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.MEDUSA),
            new NaturalSpawn(MobCategory.MONSTER, MoCEntities.MINOTAUR));

    /**
     * Adds every Mo'Creatures spawn entry appropriate to the biome behind {@code view}. Called once per biome, per
     * loader. Only overworld and Nether biomes take entries; the Wyvern and Ogre Lairs (tagged as neither) list
     * their own residents in {@code data/mocreatures/worldgen/biome/}.
     */
    public static void addBiomeSpawns(BiomeSpawnView view) {
        if (!view.hasTag(BiomeTags.IS_OVERWORLD) && !view.hasTag(BiomeTags.IS_NETHER)) {
            return;
        }
        Set<String> types = drzhark.mocreatures.spawn.MoCBiomeTypes.of(view.biomeId(), view::hasTag);
        for (NaturalSpawn spawn : NATURAL_SPAWNS) {
            addSpawn(types, view, spawn.category(), spawn.type().get());
        }
    }

    /**
     * Adds one config-driven spawn entry: the weight and pack size {@link MoCConfig} gives this entity in a biome
     * of these Forge 1.12 types ({@code spawn.<id>.frequency / .min / .max} overrides, else 12.0.5's own), or
     * nothing where it does not spawn (weight 0).
     */
    private static void addSpawn(Set<String> biomeTypes, BiomeSpawnView view, MobCategory category, EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        MoCConfig cfg = MoCConfig.get();
        int weight = cfg.spawnFrequency(id, biomeTypes);
        if (weight <= 0) {
            return;
        }
        int min = cfg.spawnMin(id, biomeTypes);
        int max = cfg.spawnMax(id, biomeTypes);
        if (max < min) {
            max = min;
        }
        view.addSpawn(category, type, weight, min, max);
    }
}
