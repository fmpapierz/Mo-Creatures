package drzhark.mocreatures.registry;

import dev.architectury.hooks.level.biome.BiomeProperties;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import drzhark.mocreatures.config.MoCConfig;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

/** Spawn placement rules + overworld biome spawns for every Mo'Creatures entity. Generated. */
public final class MoCSpawns {

    private MoCSpawns() {}

    public static void register() {
        SpawnPlacementsRegistry.register(MoCEntities.BUNNY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BEAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        // BIG_CAT: legacy getCanSpawnHere (MoCEntityBigCat:489) began with `if (MoCTools.isNearTorch(this)) return
        // false;` — a big cat never spawns near a man-made light source (torch/glowstone/lit-lamp/jack-o'-lantern
        // within ~8 blocks). Mirror the crocodile/scorpion ports: reject the spawn wherever block light >= 8, then
        // defer to the vanilla animal rules. (Placed on the predicate here since MoCEntityBigCat has no override.)
        SpawnPlacementsRegistry.register(MoCEntities.BIG_CAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) < 8
                        && net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        SpawnPlacementsRegistry.register(MoCEntities.BIRD, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BOAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CROCODILE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DEER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DUCK, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.ELEPHANT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FOX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.GOAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.HORSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.KITTY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.KOMODO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MOUSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.OSTRICH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.PET_SCORPION, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SNAKE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.TURKEY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.TURTLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WYVERN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.MAGGOT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SNAIL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DOLPHIN, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FISHY, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.JELLYFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.RAY, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SHARK, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BEE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.BUTTERFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.DRAGONFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FIREFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.CRICKET, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.ROACH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules);
        // GOLEM: legacy getCanSpawnHere (MoCEntityGolem:1272) required frequency>0 AND canBlockSeeTheSky(x,y,z) AND
        // posY>50 AND super (monster darkness) — golems only spawn at night on the open surface above Y50. AND that
        // surface-only gate (can-see-sky + Y>50) onto the vanilla monster rules so golems no longer spawn underground.
        SpawnPlacementsRegistry.register(MoCEntities.GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 50 && level.canSeeSky(pos)
                        && net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        SpawnPlacementsRegistry.register(MoCEntities.OGRE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.RAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.SCORPION, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WILD_WOLF, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WEREWOLF, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.HELL_RAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.FLAME_WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        SpawnPlacementsRegistry.register(MoCEntities.HORSE_MOB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
    }

    public static void registerBiomeSpawns() {
        BiomeModifications.addProperties((ctx, props) -> {
            if (ctx.hasTag(BiomeTags.IS_OVERWORLD)) {
                // Which legacy biome groups this biome belongs to (best-effort from vanilla biome tags; the
                // vanilla desert has no tag, so 'desert' approximates via badlands/savanna). An entity with a
                // biomegroup restriction (config / /moc biomegroup) only spawns where its groups intersect this.
                java.util.Set<String> bgroups = new java.util.HashSet<>();
                if (ctx.hasTag(BiomeTags.IS_FOREST)) bgroups.add("forest");
                if (ctx.hasTag(BiomeTags.IS_JUNGLE)) bgroups.add("jungle");
                if (ctx.hasTag(BiomeTags.IS_MOUNTAIN) || ctx.hasTag(BiomeTags.IS_HILL)) bgroups.add("mountain");
                if (ctx.hasTag(BiomeTags.IS_TAIGA) || ctx.hasTag(BiomeTags.SPAWNS_COLD_VARIANT_FROGS)) bgroups.add("arctic");
                if (ctx.hasTag(BiomeTags.IS_BADLANDS) || ctx.hasTag(BiomeTags.IS_SAVANNA)) bgroups.add("desert");
                if (ctx.hasTag(BiomeTags.HAS_SWAMP_HUT)) bgroups.add("swamp");
                if (bgroups.isEmpty()) bgroups.add("normal"); // plains-like fallback
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BEAR.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BEE.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BIG_CAT.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BIRD.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BOAR.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BUNNY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.BUTTERFLY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.CRAB.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.CRICKET.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.CROCODILE.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.DEER.get());
                addSpawn(bgroups, props, MobCategory.WATER_CREATURE, MoCEntities.DOLPHIN.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.DRAGONFLY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.DUCK.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.ELEPHANT.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.FIREFLY.get());
                addSpawn(bgroups, props, MobCategory.WATER_CREATURE, MoCEntities.FISHY.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.FLAME_WRAITH.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.FLY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.FOX.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.GOAT.get());
                // GOLEM: legacy spawned it as a rare night surface monster (MoCEntityGolem.getCanSpawnHere:
                // frequency>0 + can-see-sky + Y>50 + monster darkness, capped at 1/chunk). The placement predicate
                // in register() now explicitly ANDs the legacy can-see-sky + Y>50 gate onto checkMonsterSpawnRules'
                // night gate. Player self-assembly from blocks is an additional legacy path, not a replacement.
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.GOLEM.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.HELL_RAT.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.HORSE.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.HORSE_MOB.get());
                addSpawn(bgroups, props, MobCategory.WATER_CREATURE, MoCEntities.JELLYFISH.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.KITTY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.KOMODO.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.MAGGOT.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.MOUSE.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.OGRE.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.OSTRICH.get());
                // PET_SCORPION is intentionally NOT naturally spawned: legacy pet scorpions were only
                // bred from a mother scorpion (baby-on-back), never placed by the spawner.
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.RAT.get());
                addSpawn(bgroups, props, MobCategory.WATER_CREATURE, MoCEntities.RAY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.ROACH.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.SCORPION.get());
                addSpawn(bgroups, props, MobCategory.WATER_CREATURE, MoCEntities.SHARK.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.SNAIL.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.SNAKE.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.TURKEY.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.TURTLE.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.WEREWOLF.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.WILD_WOLF.get());
                addSpawn(bgroups, props, MobCategory.MONSTER, MoCEntities.WRAITH.get());
                addSpawn(bgroups, props, MobCategory.CREATURE, MoCEntities.WYVERN.get());
            }
        });
    }

    /**
     * Adds one config-driven spawn entry. The entity's weight and min/max group size are read from
     * {@link MoCConfig} keyed by the entity's lowercase registry id
     * ({@code spawn.<id>.frequency / .min / .max}). When no override is set, the weight falls back to
     * a per-category default (creature 8 / monster 6 / water 8 / insect 10) and min/max to 1..4.
     *
     * <p>Note: the legacy 1.12.2 CustomSpawner assigned a distinct per-entity frequency to every
     * creature (e.g. horses, bears and elephants did not share one weight). The port collapses that
     * to the uniform per-category fallback above, which is an accepted simplification: any server
     * wanting the old varied weights can restore them per entity via {@code spawn.<id>.frequency}
     * in {@code mocreatures.properties}.</p>
     */
    private static void addSpawn(java.util.Set<String> biomeGroups, BiomeProperties.Mutable props,
            MobCategory category, EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        MoCConfig cfg = MoCConfig.get();
        // Biome-group restriction (legacy CustomSpawner): if this entity is assigned any groups, it only
        // spawns in biomes belonging to one of them. No assignment (the default) = spawn everywhere.
        java.util.List<String> assigned = cfg.biomeGroups(id);
        if (!assigned.isEmpty() && java.util.Collections.disjoint(assigned, biomeGroups)) {
            return;
        }
        int weight = cfg.spawnFrequency(id);
        int min = cfg.spawnMin(id);
        int max = cfg.spawnMax(id);
        if (max < min) {
            max = min;
        }
        props.getSpawnProperties().addSpawn(category, new MobSpawnSettings.SpawnerData(type, min, max), weight);
    }
}
