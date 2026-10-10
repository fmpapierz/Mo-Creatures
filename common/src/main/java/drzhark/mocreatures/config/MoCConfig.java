package drzhark.mocreatures.config;

import dev.architectury.platform.Platform;
import drzhark.mocreatures.MoCreatures;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Cross-loader, dependency-free configuration for Mo'Creatures (MC 26.2 Architectury port).
 *
 * <p>The legacy 1.12.2 mod used a bespoke {@code MoCProperties.cfg} parser driven from
 * {@code MoCProxy.readConfigValues()}. This class reproduces every tunable of that file using a
 * plain {@link java.util.Properties} store so it works identically on Fabric and NeoForge without
 * any loader-specific config library. The config dir is resolved via Architectury's
 * {@link Platform#getConfigFolder()} (a {@link java.nio.file.Path}); the file is
 * {@code mocreatures.properties}.</p>
 *
 * <p>Every flag is exposed as a {@code public} typed field on the singleton returned by
 * {@link #get()}. Read them verbatim, e.g. {@code MoCConfig.get().elephantBulldozer}. {@link #get()}
 * is null-safe: if {@link #load()} was never called (early caller, or a context without a config
 * folder) it lazily returns an all-defaults instance, so server, client and world-gen callers are
 * always safe.</p>
 *
 * <p>The flag/tunable fields are non-final and can be changed at runtime (e.g. from the {@code /moc}
 * command or a settings GUI). After mutating a field, call {@link #save()} to persist all current
 * values back to {@code mocreatures.properties}. Convenience setters {@link #setFlag(String, boolean)}
 * and {@link #setNumber(String, double)} mutate a field by name but do <em>not</em> auto-save; the
 * caller decides when to {@link #save()}.</p>
 */
public final class MoCConfig {

    public static final String FILE_NAME = "mocreatures.properties";

    /** The active singleton. Written once by {@link #load()}; lazily defaulted by {@link #get()}. */
    private static volatile MoCConfig INSTANCE;

    // ------------------------------------------------------------------------------------------
    // General / behaviour toggles (booleans)
    //   Defaults follow the port contract in the flag list; where that diverges from the legacy
    //   MoCProperties.cfg value the legacy value is noted in a trailing comment.
    // ------------------------------------------------------------------------------------------

    // Flag fields are non-final so /moc config and a settings GUI can change them at runtime.
    // Field names and public visibility are unchanged; persist with save() after mutating.

    /** Green/cave/fire ogres bulldoze (destroy) blocks. Legacy: true. */
    public boolean elephantBulldozer;
    /** Simplified breeding (feed two adults to breed). Legacy MoCProperties EasyBreeding: true. */
    public boolean easyBreeding;
    /** Simplified harvesting (e.g. milk/wool without extra steps). New tunable in the port. */
    public boolean easyHarvesting;
    /** Spawn each creature only in the biomes 12.0.5 listed for it (DEFAULT_SPAWNS) when it has no explicit
     * biomegroup override. Off: every biome of its world. Legacy MoCProperties useDefaultBiomeGroups: true. */
    public boolean useDefaultBiomeGroups;
    /** Water creatures may attack dolphins. Legacy: false. */
    public boolean attackDolphins;
    /** Creatures may attack vanilla wolves. Legacy: false. */
    public boolean attackWolves;
    /** Creatures may attack (Mo'Creatures) horses. Legacy: false. */
    public boolean attackHorses;
    /** Creatures may hunt other creatures (animal-vs-animal predation). Legacy MoCProperties EnableHunters:
     * true — "Allows creatures to attack other creatures. Not recommended if despawning is off." */
    public boolean enableHunters;
    /** Piranha fishy variant that attacks players may spawn. Legacy MoCProperties SpawnPiranhas: true. */
    public boolean spawnPiranhas;
    /** Mo'Creatures' custom spawner handles/overrides vanilla spawns. Legacy: true. */
    public boolean modifyVanillaSpawns;
    /**
     * Custom spawner periodically despawns distant vanilla animals so Mo'Creatures fauna has room.
     * Only takes effect when {@link #modifyVanillaSpawns} is also on.
     *
     * <p>Legacy MoCProperties DespawnVanilla defaulted to true, but legacy also replaced vanilla spawning
     * wholesale with its own CustomSpawner, so thinning vanilla herds was how it made room. This port spawns
     * through the vanilla pipeline, where vanilla and Mo'Creatures animals share one weighted pick per chunk
     * (see DEFAULT_SPAWNS), so the cull is not needed to leave room — and it is destructive by nature:
     * vanilla farm animals never despawn on their own, so anything it removes is a permanent loss the player
     * did not ask for. Defaults to false; the cull additionally refuses to touch any animal that shows a sign
     * of player investment — named, leashed, ridden, tamed, persistence-flagged, or bred/fed/still a baby
     * (see MoCMobCap.despawnVanillaAnimals).
     *
     * <p><strong>Turning this on is still destructive by design.</strong> An ordinary adult cow has
     * {@code age == 0} and carries no other marker, so a penned herd the player walked away from <em>will</em>
     * be removed once it is more than 128 blocks off. Enable it only if you want vanilla herds thinned.</p>
     */
    public boolean despawnVanilla;
    /** Tamed creatures get an owner; only the owner interacts with them. Legacy MoCProperties: false. */
    public boolean enableOwnership;
    /** Reset-ownership scroll works (untame a creature). Legacy MoCProperties enableResetOwnerScroll: false. */
    public boolean enableResetOwnership;
    /** Kitty bed placed as a static block-like furniture entity rather than a mob. Legacy: true. */
    public boolean staticBed;
    /** Litterbox placed as a static block-like furniture entity rather than a mob. Legacy: true. */
    public boolean staticLitter;
    /** Animate creature textures (blink/mouth etc.). Legacy MoCProperties animateTextures: false. */
    public boolean animateTextures;
    /** Creatures can destroy item drops / trample crops as they path. Legacy: false. */
    public boolean destroyDrops;
    /** Creatures destroy passive drops (grass/tallgrass) as they path. New tunable in the port. */
    public boolean destroyPassiveDrops;
    /** Client: render the pet's name above it. Legacy MoCProperties displayPetName: false. */
    public boolean displayPetName;
    /** Client: render the pet's health bar. Legacy MoCProperties displayPetHealth: false. */
    public boolean displayPetHealth;
    /** Client: render pet emote/status icons. Legacy MoCProperties displayPetIcons: false. */
    public boolean displayPetIcons;
    /** Master toggle for Mo'Creatures particle effects. Legacy stored particleFX as an int (3). */
    public boolean particleFX;

    // ------------------------------------------------------------------------------------------
    // Strength / chance / cap tunables (ints & doubles)
    // ------------------------------------------------------------------------------------------

    // Numeric fields are also non-final so setNumber(...) and a settings GUI can retune them.

    /** Block-destruction radius of green ogres. Legacy OgreStrength: 2.5. */
    public double ogreStrength;
    /** Block-destruction radius of cave ogres. Legacy CaveOgreStrength: 3.0. */
    public double caveOgreStrength;
    /** Block-destruction radius of fire ogres. Legacy FireOgreStrength: 2.0. */
    public double fireOgreStrength;
    /** Block radius where ogres 'smell'/detect players. Legacy OgreAttackRange: 12. */
    public int ogreAttackRange;
    /** Percent chance of spawning a cave ogre at depth in the Overworld. Legacy CaveOgreChance: 75. */
    public int caveOgreChance;
    /** Percent chance of spawning a fire ogre in the Overworld. Legacy FireOgreChance: 25. */
    public int fireOgreChance;
    /** Minimum world difficulty (0-3) offset a shark needs to spawn. Legacy sharkSpawnDif: 0. */
    public int sharkStrength;
    /**
     * Percent of wild horses (spawn egg / natural spawn, outside the zebra biomes) that come out as zebras.
     * 12.0.5 MoCProxy ZebraChance: 10 (5.1.5: 2). Only the default changes: an existing config keeps its value.
     */
    public int zebraChance;
    /** Percent chance a wyvern drops an egg on death. New tunable in the port. */
    public int wyvernEggDropChance;
    /** Percent chance a monster drops a monster-egg on death. New tunable in the port. */
    public int monsterEggDropChance;

    // There is no animal or ambient cap. The port had maxAnimals and maxAmbient caps (MoCMobCap) that discarded
    // wild Mo'Creatures animals and insects over them, farthest from players first. Neither ever despawns, and
    // animals only spawn when chunks generate, so every one removed was gone for good (old configs still held an
    // animal cap of 40). 12.0.5 had no caps at all. Removed 2026-10-09 (animals) and 2026-10-10 (insects) at the
    // user's request; maxAnimals / maxAmbient lines left in an old config file are ignored and dropped on save.
    /** Cap on wild Mo'Creatures monsters per dimension (MoCMobCap trims the excess; they respawn). Default 70. */
    public int maxMobs;
    /** Cap on wild Mo'Creatures water mobs per dimension (MoCMobCap trims the excess; they respawn). Default 30. */
    public int maxWaterMobs;
    /** Max tamed creatures a normal player may own. Legacy maxTamedPerPlayer: 10 (flag list suggested 5). */
    public int maxTamed;
    /** Max tamed creatures an op may own. Legacy maxTamedPerOP: 20. */
    public int maxOPTamed;

    // ------------------------------------------------------------------------------------------
    // Per-entity spawn tuning (keyed spawn.<id>.frequency / .min / .max)
    //   Downstream MoCSpawns reads these via spawnFrequency/spawnMin/spawnMax(String entityId).
    //   Absent keys fall back to the 12.0.5 per-species defaults below.
    // ------------------------------------------------------------------------------------------

    /** Backing store for the whole file, retained for the keyed per-entity spawn lookups. */
    private final Properties props;

    // Default spawns: 12.0.5's own per-species SpawnListEntry values (weight, pack) AND biome-type lists
    // (MoCEntities.java:262-453), restored at the user's request 2026-10-09 (weights) and 2026-10-10 (biomes, insects).
    // 12.0.5 added each species to every biome whose Forge 1.12 BiomeDictionary types met its list; MoCBiomeTypes
    // gives a 26.2 biome those same types, so the lists below apply unchanged. They replace the port's own seven
    // biome groups, which had widened most species well past 12.0.5 (kitties in forests, elephants in jungles,
    // ants, moles, raccoons and Ents everywhere) and left others short (no crabs on beaches, no hell rats in dark
    // forests, no fire ogres or nightmares in the Nether).
    //
    // Where the port merged several 12.0.5 species into one entity (4 bears, 4 big cats, 3 ogres, 8 small fish,
    // 3 medium fish, 2 rays) each subspecies keeps its own entry: a biome gets the summed weight of those whose list
    // it meets (a forest: black + grizzly + panda bear = 18; a snowy plain: the polar bear's 6), the pack spanning
    // theirs, and the entity spawns only where at least one is listed.
    //
    // The eleven insects and small critters are 12.0.5 AMBIENT spawns (MoCSpawns spawns them in the ambient pool,
    // with the bats). The chimpanzee, medusa and minotaur never existed in any Mo'Creatures release, so their
    // entries are the port's own: the chimpanzee (built on the raccoon) the raccoon's weight in forests and jungles,
    // the two monsters the werewolf's list at the port's 6.
    //
    // A server can still override any entity with spawn.<id>.frequency / .min / .max (any biome it spawns in) and
    // biomegroup.<id> (the 1.12 types it spawns in) in mocreatures.properties.
    private record SpawnSub(int weight, int min, int max, java.util.Set<String> types) {}

    /** One 12.0.5 SpawnListEntry: weight, pack size, and its space-separated biome types (lowercase). */
    private static SpawnSub sub(int weight, int min, int max, String types) {
        return new SpawnSub(weight, min, max, java.util.Set.of(types.split(" ")));
    }

    // 12.0.5's recurring lists, verbatim.
    private static final String LAND_MOBS = "sandy forest snowy jungle hills mesa mountain plains swamp wasteland";
    private static final String GRASSLANDS = "forest hills jungle mesa mountain plains";
    private static final String WATERS = "beach swamp water ocean river";

    private static final java.util.Map<String, java.util.List<SpawnSub>> DEFAULT_SPAWNS = java.util.Map.ofEntries(
            // --- animals (12.0.5 CREATURE)
            java.util.Map.entry("bear", java.util.List.of(
                    sub(6, 1, 3, "forest hills mesa mountain plains"),          // black bear
                    sub(6, 1, 2, "forest hills mesa mountain plains"),          // grizzly
                    sub(6, 1, 3, "forest hills mesa plains"),                   // panda
                    sub(6, 1, 2, "snowy"))),                                    // polar bear
            java.util.Map.entry("big_cat", java.util.List.of(
                    sub(6, 1, 2, "hills jungle mountain snowy"),                // leopard
                    sub(6, 1, 2, "jungle plains"),                              // lion
                    sub(6, 1, 2, "forest hills jungle mountain"),               // panther
                    sub(6, 1, 2, "forest jungle plains"))),                     // tiger
            java.util.Map.entry("bird", java.util.List.of(sub(15, 2, 3, GRASSLANDS))),
            java.util.Map.entry("boar", java.util.List.of(sub(8, 2, 2, "forest jungle plains"))),
            java.util.Map.entry("bunny", java.util.List.of(sub(8, 2, 3, "forest jungle plains snowy"))),
            java.util.Map.entry("crocodile", java.util.List.of(sub(6, 1, 2, "swamp"))),
            java.util.Map.entry("deer", java.util.List.of(sub(8, 1, 2, "forest plains"))),
            java.util.Map.entry("duck", java.util.List.of(sub(8, 1, 2, "forest jungle plains"))),
            java.util.Map.entry("elephant", java.util.List.of(sub(4, 1, 1, "sandy forest plains snowy"))),
            java.util.Map.entry("ent", java.util.List.of(sub(4, 1, 2, "forest jungle plains"))),
            java.util.Map.entry("fox", java.util.List.of(sub(8, 1, 1, "forest jungle plains snowy"))),
            java.util.Map.entry("goat", java.util.List.of(sub(8, 1, 3, GRASSLANDS))),
            java.util.Map.entry("horse", java.util.List.of(sub(8, 1, 4, "forest hills mesa mountain plains"))),
            java.util.Map.entry("kitty", java.util.List.of(sub(8, 1, 2, "plains"))),
            java.util.Map.entry("komodo", java.util.List.of(sub(8, 1, 2, "swamp"))),
            java.util.Map.entry("mole", java.util.List.of(sub(7, 1, 2, "forest plains"))),
            java.util.Map.entry("mouse", java.util.List.of(sub(7, 1, 2, GRASSLANDS))),
            java.util.Map.entry("ostrich", java.util.List.of(sub(4, 1, 1, "sandy plains"))),
            java.util.Map.entry("raccoon", java.util.List.of(sub(8, 1, 2, "forest hills mesa mountain plains"))),
            java.util.Map.entry("snake", java.util.List.of(sub(8, 1, 2,
                    "sandy forest hills jungle mesa mountain plains swamp wasteland"))),
            java.util.Map.entry("turkey", java.util.List.of(sub(8, 1, 2, "plains"))),
            java.util.Map.entry("turtle", java.util.List.of(sub(6, 1, 2, "jungle swamp"))),
            java.util.Map.entry("chimpanzee", java.util.List.of(sub(8, 1, 2, "forest jungle"))),     // port-only
            // --- insects and small critters (12.0.5 AMBIENT)
            java.util.Map.entry("ant", java.util.List.of(sub(7, 1, 4, GRASSLANDS + " swamp wasteland"))),
            java.util.Map.entry("bee", java.util.List.of(sub(6, 1, 2, GRASSLANDS))),
            java.util.Map.entry("butterfly", java.util.List.of(sub(8, 1, 3, GRASSLANDS))),
            java.util.Map.entry("crab", java.util.List.of(sub(8, 1, 2, "beach water"))),
            java.util.Map.entry("cricket", java.util.List.of(sub(8, 1, 2, GRASSLANDS + " swamp"))),
            java.util.Map.entry("dragonfly", java.util.List.of(sub(6, 1, 2, GRASSLANDS + " swamp beach"))),
            java.util.Map.entry("firefly", java.util.List.of(sub(8, 1, 2, GRASSLANDS + " swamp"))),
            java.util.Map.entry("fly", java.util.List.of(sub(8, 1, 2, GRASSLANDS + " swamp wasteland"))),
            java.util.Map.entry("maggot", java.util.List.of(sub(8, 1, 2, "forest jungle plains swamp"))),
            java.util.Map.entry("roach", java.util.List.of(sub(8, 1, 2, "forest jungle plains swamp"))),
            java.util.Map.entry("snail", java.util.List.of(sub(7, 1, 2, "forest jungle plains"))),
            // --- monsters (12.0.5 MONSTER)
            java.util.Map.entry("golem", java.util.List.of(sub(3, 1, 1, LAND_MOBS))),
            java.util.Map.entry("ogre", java.util.List.of(
                    sub(8, 1, 2, "sandy forest jungle hills mesa mountain plains swamp wasteland dead spooky"), // green
                    sub(6, 1, 2, "sandy forest jungle hills mesa mountain nether plains swamp wasteland dead spooky"), // fire
                    sub(5, 1, 2, LAND_MOBS + " dead spooky"))),                                              // cave
            java.util.Map.entry("rat", java.util.List.of(sub(7, 1, 2, LAND_MOBS))),
            java.util.Map.entry("scorpion", java.util.List.of(sub(6, 1, 3, LAND_MOBS + " nether"))),
            java.util.Map.entry("wild_wolf", java.util.List.of(sub(8, 1, 3, LAND_MOBS))),
            java.util.Map.entry("werewolf", java.util.List.of(sub(8, 1, 4, LAND_MOBS))),
            java.util.Map.entry("wraith", java.util.List.of(sub(6, 1, 4, LAND_MOBS + " dead spooky"))),
            java.util.Map.entry("hell_rat", java.util.List.of(sub(6, 1, 4, "nether dead spooky"))),
            java.util.Map.entry("flame_wraith", java.util.List.of(sub(5, 1, 2, LAND_MOBS + " nether dead spooky"))),
            java.util.Map.entry("horse_mob", java.util.List.of(sub(8, 1, 3, LAND_MOBS + " nether dead spooky"))),
            java.util.Map.entry("silver_skeleton", java.util.List.of(sub(6, 1, 4, LAND_MOBS + " dead spooky"))),
            java.util.Map.entry("mini_golem", java.util.List.of(sub(6, 1, 3, LAND_MOBS))),
            java.util.Map.entry("manticore", java.util.List.of(sub(8, 1, 3,
                    "sandy forest snowy jungle hills mountain plains swamp wasteland nether dead spooky"))),
            java.util.Map.entry("medusa", java.util.List.of(sub(6, 1, 4, LAND_MOBS))),             // port-only
            java.util.Map.entry("minotaur", java.util.List.of(sub(6, 1, 4, LAND_MOBS))),           // port-only
            // --- aquatics (12.0.5 WATER_CREATURE; WATER is every ocean and river)
            java.util.Map.entry("dolphin", java.util.List.of(sub(6, 2, 4, "beach water ocean river"))),
            java.util.Map.entry("fishy", java.util.List.of(sub(12, 1, 6, WATERS))),
            java.util.Map.entry("jellyfish", java.util.List.of(sub(8, 1, 4, "water ocean river"))),
            java.util.Map.entry("shark", java.util.List.of(sub(6, 1, 2, "water ocean river"))),
            java.util.Map.entry("ray", java.util.List.of(
                    sub(10, 1, 2, "ocean"),                                     // manta ray
                    sub(10, 1, 2, "swamp water river"))),                       // sting ray
            java.util.Map.entry("small_fish", java.util.List.of(
                    sub(12, 1, 6, WATERS),                                      // anchovy
                    sub(12, 1, 6, WATERS),                                      // angelfish
                    sub(12, 1, 6, "beach swamp water ocean"),                   // angler
                    sub(12, 1, 6, "beach swamp water ocean"),                   // clownfish
                    sub(12, 1, 6, "beach swamp water river"),                   // goldfish
                    sub(12, 1, 6, "beach swamp water ocean"),                   // hippo tang
                    sub(12, 1, 6, "beach swamp water ocean"),                   // manderin
                    sub(4, 1, 3, WATERS))),                                     // piranha
            java.util.Map.entry("medium_fish", java.util.List.of(
                    sub(10, 1, 4, WATERS), sub(10, 1, 4, WATERS), sub(10, 1, 4, WATERS))));  // bass, cod, salmon

    /** An id no table entry covers: 12.0.5's commonest animal entry, on its ordinary land. */
    private static final SpawnSub FALLBACK_SPAWN = sub(8, 1, 2, GRASSLANDS);

    // ------------------------------------------------------------------------------------------

    /** Builds an instance from a resolved Properties store (defaults already merged in). */
    private MoCConfig(Properties p) {
        this.props = p;

        this.elephantBulldozer   = getBool(p, "elephantBulldozer", true);
        this.easyBreeding        = getBool(p, "easyBreeding", true);
        this.easyHarvesting      = getBool(p, "easyHarvesting", false);
        this.useDefaultBiomeGroups = getBool(p, "useDefaultBiomeGroups", true);
        this.attackDolphins      = getBool(p, "attackDolphins", false);
        this.attackWolves        = getBool(p, "attackWolves", false);
        this.attackHorses        = getBool(p, "attackHorses", false);
        this.enableHunters       = getBool(p, "enableHunters", true);
        this.spawnPiranhas       = getBool(p, "spawnPiranhas", true);
        this.modifyVanillaSpawns = getBool(p, "modifyVanillaSpawns", true);
        this.despawnVanilla      = getBool(p, "despawnVanilla", false);
        // Legacy defaults (MoCProxy readConfigValues): ownership OFF out of the box — no owner assignment and
        // no per-player tame cap unless the server opts in. (Existing mocreatures.properties keep their values;
        // this only changes a fresh config.)
        this.enableOwnership     = getBool(p, "enableOwnership", false);
        this.enableResetOwnership= getBool(p, "enableResetOwnership", false);
        this.staticBed           = getBool(p, "staticBed", true);
        this.staticLitter        = getBool(p, "staticLitter", true);
        this.animateTextures     = getBool(p, "animateTextures", true);
        this.destroyDrops        = getBool(p, "destroyDrops", false);
        this.destroyPassiveDrops = getBool(p, "destroyPassiveDrops", false);
        this.displayPetName      = getBool(p, "displayPetName", true);
        this.displayPetHealth    = getBool(p, "displayPetHealth", true);
        this.displayPetIcons     = getBool(p, "displayPetIcons", true);
        this.particleFX          = getBool(p, "particleFX", true);

        this.ogreStrength        = getDouble(p, "ogreStrength", 2.5D);
        this.caveOgreStrength    = getDouble(p, "caveOgreStrength", 3.0D);
        this.fireOgreStrength    = getDouble(p, "fireOgreStrength", 2.0D);
        this.ogreAttackRange     = getInt(p, "ogreAttackRange", 12);
        this.caveOgreChance      = getInt(p, "caveOgreChance", 75);
        this.fireOgreChance      = getInt(p, "fireOgreChance", 25);
        this.sharkStrength       = getInt(p, "sharkStrength", 0);
        this.zebraChance         = getInt(p, "zebraChance", 10);
        this.wyvernEggDropChance = getInt(p, "wyvernEggDropChance", 10);
        this.monsterEggDropChance= getInt(p, "monsterEggDropChance", 25);

        // Population caps (5.1.5 MoCProxy: maxMobs=70, maxWaterMobs=30, maxTamedPerPlayer=10).
        // Its maxAnimals and maxAmbient are gone: see the note on the fields.
        this.maxMobs             = getInt(p, "maxMobs", 70);
        this.maxWaterMobs        = getInt(p, "maxWaterMobs", 30);
        this.maxTamed            = getInt(p, "maxTamed", 10);
        this.maxOPTamed          = getInt(p, "maxOPTamed", 20);
    }

    /** Constructs the all-defaults instance (used by the lazy fallback in {@link #get()}). */
    private static MoCConfig defaults() {
        return new MoCConfig(new Properties());
    }

    // ------------------------------------------------------------------------------------------
    // Public accessors
    // ------------------------------------------------------------------------------------------

    /**
     * Returns the loaded config, or a usable all-defaults instance if {@link #load()} has not run.
     * Never returns {@code null}, so any early or off-thread caller is safe.
     */
    public static MoCConfig get() {
        MoCConfig local = INSTANCE;
        if (local == null) {
            synchronized (MoCConfig.class) {
                local = INSTANCE;
                if (local == null) {
                    local = defaults();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    /**
     * Loads {@code mocreatures.properties} from the Architectury config folder, writing a fully
     * populated default file (with a header comment) if it is absent, and installs the result as
     * the singleton. Call this once, first thing in {@code MoCreatures.init()}. Any failure is
     * logged and swallowed, falling back to defaults so init never breaks on config I/O.
     */
    public static synchronized void load() {
        Properties p = new Properties();
        try {
            Path dir = Platform.getConfigFolder();
            Path file = dir.resolve(FILE_NAME);
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    p.load(in);
                }
            } else {
                // First run: build defaults, expose them, then persist a documented file.
                // writeTo emits the instance's current values, which here are exactly the defaults.
                MoCConfig defaults = new MoCConfig(p);
                INSTANCE = defaults;
                defaults.writeTo(file);
                MoCreatures.LOGGER.info("Mo'Creatures: wrote default config to {}", file);
                return;
            }
        } catch (Exception e) {
            MoCreatures.LOGGER.warn("Mo'Creatures: failed to load {}, using defaults", FILE_NAME, e);
            p = new Properties();
        }
        INSTANCE = new MoCConfig(p);
    }

    // ------------------------------------------------------------------------------------------
    // Per-entity spawn tuning API (used later by MoCSpawns)
    // ------------------------------------------------------------------------------------------

    /**
     * Spawn weight of an entity in a biome whose Forge 1.12 types are {@code biomeTypes} (see
     * {@code MoCBiomeTypes}), or 0 where it does not spawn. Without a {@code spawn.<id>.frequency} override it is
     * the 12.0.5 default (see {@link #DEFAULT_SPAWNS}): for a merged entity, the summed weight of the subspecies
     * 12.0.5 listed for one of those types. {@code id} is the lowercase registry path, e.g. {@code "elephant"}.
     */
    public int spawnFrequency(String entityId, java.util.Set<String> biomeTypes) {
        java.util.List<SpawnSub> subs = spawnSubs(entityId, biomeTypes);
        if (subs.isEmpty()) {
            return 0;
        }
        int weight = 0;
        for (SpawnSub s : subs) {
            weight += s.weight();
        }
        return spawnInt(entityId, "frequency", weight);
    }

    /** Minimum pack size for an entity in a biome, keyed {@code spawn.<id>.min}; defaults to 12.0.5's. */
    public int spawnMin(String entityId, java.util.Set<String> biomeTypes) {
        int min = Integer.MAX_VALUE;
        for (SpawnSub s : spawnSubs(entityId, biomeTypes)) {
            min = Math.min(min, s.min());
        }
        return spawnInt(entityId, "min", min == Integer.MAX_VALUE ? 1 : min);
    }

    /** Maximum pack size for an entity in a biome, keyed {@code spawn.<id>.max}; defaults to 12.0.5's. */
    public int spawnMax(String entityId, java.util.Set<String> biomeTypes) {
        int max = 1;
        for (SpawnSub s : spawnSubs(entityId, biomeTypes)) {
            max = Math.max(max, s.max());
        }
        return spawnInt(entityId, "max", max);
    }

    /**
     * The 12.0.5 subspecies entries an entity spawns with in a biome; empty where it does not spawn. Whether it
     * spawns there at all: an explicit {@code biomegroup.<id>} list decides if set; otherwise 12.0.5's own lists
     * (any subspecies listed for one of the biome's types), or, with {@link #useDefaultBiomeGroups} off, every
     * biome of its world (overworld, or the Nether for a species 12.0.5 listed there). Where it spawns but no
     * subspecies is listed (an override or the flag put it there), it spawns at its heaviest entry.
     */
    private java.util.List<SpawnSub> spawnSubs(String entityId, java.util.Set<String> biomeTypes) {
        String id = entityId == null ? "" : entityId.toLowerCase(java.util.Locale.ROOT);
        java.util.List<SpawnSub> subs = DEFAULT_SPAWNS.getOrDefault(id, java.util.List.of(FALLBACK_SPAWN));
        java.util.List<SpawnSub> listed = new java.util.ArrayList<>();
        for (SpawnSub s : subs) {
            if (!java.util.Collections.disjoint(s.types(), biomeTypes)) {
                listed.add(s);
            }
        }
        java.util.List<String> override = biomeGroupOverride(id);
        boolean spawns;
        if (override != null) {
            spawns = !java.util.Collections.disjoint(override, biomeTypes);
        } else if (this.useDefaultBiomeGroups) {
            spawns = !listed.isEmpty();
        } else {
            boolean nether = biomeTypes.contains("nether");
            spawns = !biomeTypes.contains("end") && subs.stream().anyMatch(
                    s -> nether ? s.types().contains("nether") : !s.types().equals(java.util.Set.of("nether")));
        }
        if (!spawns) {
            return java.util.List.of();
        }
        if (listed.isEmpty()) {
            listed.add(subs.stream().max(java.util.Comparator.comparingInt(SpawnSub::weight)).orElseThrow());
        }
        return listed;
    }

    private int spawnInt(String entityId, String suffix, int fallback) {
        if (entityId == null) {
            return fallback;
        }
        String key = "spawn." + entityId.toLowerCase(java.util.Locale.ROOT) + "." + suffix;
        return getInt(props, key, fallback);
    }

    /**
     * Sets a per-entity spawn tuning value in the backing store ({@code suffix} = frequency|min|max),
     * so {@code MoCSpawns} picks it up on the next reload. Call {@link #save()} to persist it. Used by the
     * {@code /moc spawnrate} command (the runtime equivalent of the legacy per-entity spawn subcommands).
     */
    public void setSpawnValue(String entityId, String suffix, int value) {
        if (entityId == null) {
            return;
        }
        props.setProperty("spawn." + entityId.toLowerCase(java.util.Locale.ROOT) + "." + suffix,
                String.valueOf(value));
    }

    // ---- Per-entity biome restriction (12.0.5's per-species biome-type lists) -----------------------------
    // Keyed: biomegroup.<id> = comma list of the Forge 1.12 biome types an entity spawns in, replacing its 12.0.5
    // list. Absent (the default) = the 12.0.5 list in DEFAULT_SPAWNS. Consumed by MoCSpawns via spawnFrequency.

    /** Forge 1.12.2's BiomeDictionary types (lowercase), the vocabulary of 12.0.5's lists and of the overrides. */
    private static final java.util.List<String> BIOME_GROUP_NAMES = java.util.List.of(
            "forest", "plains", "jungle", "mountain", "hills", "swamp", "sandy", "mesa", "savanna", "snowy",
            "wasteland", "beach", "ocean", "river", "water", "mushroom", "spooky", "dead", "coniferous", "hot", "cold",
            "wet", "dry", "dense", "sparse", "lush", "magical", "rare", "nether", "end", "void");

    /**
     * The port's earlier seven group names (biomegroup overrides written before 2026-10-10) and the 1.12 types each
     * stood for: its "normal" was open plains, "arctic" snow, "desert" desert/badlands/savanna, "mountain"
     * mountains and hills.
     */
    private static final java.util.Map<String, java.util.List<String>> LEGACY_GROUP_NAMES = java.util.Map.of(
            "normal", java.util.List.of("plains"),
            "arctic", java.util.List.of("snowy"),
            "desert", java.util.List.of("sandy", "mesa", "savanna"),
            "mountain", java.util.List.of("mountain", "hills"));

    public static java.util.List<String> biomeGroupNames() {
        return BIOME_GROUP_NAMES;
    }

    /** An explicit biomegroup.<id> list (old group names translated), or null when there is none. */
    private java.util.@org.jspecify.annotations.Nullable List<String> biomeGroupOverride(String id) {
        String v = props.getProperty("biomegroup." + id, "");
        if (v.isBlank()) {
            return null;
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String raw : v.split(",")) {
            String t = raw.trim().toLowerCase(java.util.Locale.ROOT);
            for (String type : LEGACY_GROUP_NAMES.getOrDefault(t, java.util.List.of(t))) {
                if (!type.isEmpty() && !out.contains(type)) {
                    out.add(type);
                }
            }
        }
        return out;
    }

    /**
     * The biome types an entity spawns in: its biomegroup override, else its 12.0.5 list (all subspecies), or an
     * empty list = every biome of its world (useDefaultBiomeGroups off).
     */
    public java.util.List<String> biomeGroups(String entityId) {
        if (entityId == null) {
            return java.util.List.of();
        }
        String id = entityId.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> override = biomeGroupOverride(id);
        if (override != null) {
            return override;
        }
        if (!this.useDefaultBiomeGroups) {
            return java.util.List.of();
        }
        java.util.Set<String> all = new java.util.TreeSet<>();
        for (SpawnSub s : DEFAULT_SPAWNS.getOrDefault(id, java.util.List.of(FALLBACK_SPAWN))) {
            all.addAll(s.types());
        }
        return new java.util.ArrayList<>(all);
    }

    /** Adds a biome type to an entity's list (call {@link #save()} to persist). False if the type is unknown. */
    public boolean addBiomeGroup(String entityId, String group) {
        String g = group == null ? "" : group.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> types = LEGACY_GROUP_NAMES.getOrDefault(g, java.util.List.of(g));
        if (entityId == null || !BIOME_GROUP_NAMES.containsAll(types)) {
            return false;
        }
        java.util.List<String> cur = new java.util.ArrayList<>(biomeGroups(entityId));
        for (String t : types) {
            if (!cur.contains(t)) {
                cur.add(t);
            }
        }
        props.setProperty("biomegroup." + entityId.toLowerCase(java.util.Locale.ROOT), String.join(",", cur));
        return true;
    }

    /** Removes a biome type from an entity's list (call {@link #save()} to persist). */
    public boolean removeBiomeGroup(String entityId, String group) {
        if (entityId == null) {
            return false;
        }
        String g = group == null ? "" : group.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> cur = new java.util.ArrayList<>(biomeGroups(entityId));
        boolean removed = cur.removeAll(LEGACY_GROUP_NAMES.getOrDefault(g, java.util.List.of(g)));
        props.setProperty("biomegroup." + entityId.toLowerCase(java.util.Locale.ROOT), String.join(",", cur));
        return removed;
    }

    // ------------------------------------------------------------------------------------------
    // Parsing helpers (null-safe, tolerant of malformed values)
    // ------------------------------------------------------------------------------------------

    private static boolean getBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        v = v.trim();
        if (v.equalsIgnoreCase("true")) {
            return true;
        }
        if (v.equalsIgnoreCase("false")) {
            return false;
        }
        return def;
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double getDouble(Properties p, String key, double def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Default-file writer
    // ------------------------------------------------------------------------------------------

    /**
     * Persists ALL current field values back to {@code mocreatures.properties} in the Architectury
     * config folder, preserving any per-entity {@code spawn.*} overrides currently held in the
     * backing store. Call after mutating fields directly, or after {@link #setFlag(String, boolean)}
     * / {@link #setNumber(String, double)} (which deliberately do not auto-save). Any I/O failure is
     * logged and swallowed so a bad save never crashes the caller.
     */
    public void save() {
        try {
            Path file = Platform.getConfigFolder().resolve(FILE_NAME);
            writeTo(file);
            MoCreatures.LOGGER.info("Mo'Creatures: saved config to {}", file);
        } catch (Exception e) {
            MoCreatures.LOGGER.warn("Mo'Creatures: failed to save {}", FILE_NAME, e);
        }
    }

    /**
     * Sets a boolean flag by (case-insensitive) field name and returns whether the name matched a
     * known flag. Does <em>not</em> persist &mdash; the caller decides when to call {@link #save()}.
     */
    public boolean setFlag(String name, boolean value) {
        if (name == null) {
            return false;
        }
        switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "elephantbulldozer":    this.elephantBulldozer = value;    return true;
            case "easybreeding":         this.easyBreeding = value;         return true;
            case "easyharvesting":       this.easyHarvesting = value;       return true;
            case "usedefaultbiomegroups": this.useDefaultBiomeGroups = value; return true;
            case "attackdolphins":       this.attackDolphins = value;       return true;
            case "attackwolves":         this.attackWolves = value;         return true;
            case "attackhorses":         this.attackHorses = value;         return true;
            case "enablehunters":        this.enableHunters = value;        return true;
            case "spawnpiranhas":        this.spawnPiranhas = value;        return true;
            case "modifyvanillaspawns":  this.modifyVanillaSpawns = value;  return true;
            case "despawnvanilla":       this.despawnVanilla = value;       return true;
            case "enableownership":      this.enableOwnership = value;      return true;
            case "enableresetownership": this.enableResetOwnership = value; return true;
            case "staticbed":            this.staticBed = value;            return true;
            case "staticlitter":         this.staticLitter = value;         return true;
            case "animatetextures":      this.animateTextures = value;      return true;
            case "destroydrops":         this.destroyDrops = value;         return true;
            case "destroypassivedrops":  this.destroyPassiveDrops = value;  return true;
            case "displaypetname":       this.displayPetName = value;       return true;
            case "displaypethealth":     this.displayPetHealth = value;     return true;
            case "displaypeticons":      this.displayPetIcons = value;      return true;
            case "particlefx":           this.particleFX = value;           return true;
            default:                     return false;
        }
    }

    /**
     * Sets a numeric tunable by (case-insensitive) field name and returns whether the name matched.
     * Integer-typed fields are rounded from the supplied {@code double}. Does <em>not</em> persist
     * &mdash; the caller decides when to call {@link #save()}.
     */
    public boolean setNumber(String name, double value) {
        if (name == null) {
            return false;
        }
        switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "ogrestrength":         this.ogreStrength = value;                    return true;
            case "caveogrestrength":     this.caveOgreStrength = value;                return true;
            case "fireogrestrength":     this.fireOgreStrength = value;                return true;
            case "ogreattackrange":      this.ogreAttackRange = (int) Math.round(value); return true;
            case "caveogrechance":       this.caveOgreChance = (int) Math.round(value);  return true;
            case "fireogrechance":       this.fireOgreChance = (int) Math.round(value);  return true;
            case "sharkstrength":        this.sharkStrength = (int) Math.round(value); return true;
            case "zebrachance":          this.zebraChance = (int) Math.round(value);   return true;
            case "wyverneggdropchance":  this.wyvernEggDropChance = (int) Math.round(value); return true;
            case "monstereggdropchance": this.monsterEggDropChance = (int) Math.round(value); return true;
            case "maxmobs":              this.maxMobs = (int) Math.round(value);       return true;
            case "maxwatermobs":         this.maxWaterMobs = (int) Math.round(value);  return true;
            case "maxtamed":             this.maxTamed = (int) Math.round(value);      return true;
            case "maxoptamed":           this.maxOPTamed = (int) Math.round(value);    return true;
            default:                     return false;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Properties writer (shared by first-run default write and runtime save())
    // ------------------------------------------------------------------------------------------

    /**
     * Writes a fully-populated properties file with a short header comment, emitting the current
     * value of every field (not literal defaults). Uses a manual writer rather than
     * {@link Properties#store} so the layout is grouped and readable, and so the per-entity spawn
     * section is documented. Shared by the first-run default write in {@link #load()} and by
     * {@link #save()}; on first run {@code this} already holds the defaults, so both paths emit the
     * correct values. Any per-entity {@code spawn.*} overrides in the backing store are appended so
     * a save round-trips them rather than dropping them.
     */
    private void writeTo(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# Mo'Creatures configuration (MC 26.2 Architectury multi-loader).\n");
        sb.append("# Written by the mod; edited by /moc config, the settings GUI, or by hand.\n");
        sb.append("# Booleans are true/false; delete this file to regenerate defaults.\n");
        sb.append('\n');

        sb.append("# --- Behaviour toggles ---\n");
        line(sb, "elephantBulldozer", this.elephantBulldozer);
        line(sb, "easyBreeding", this.easyBreeding);
        line(sb, "easyHarvesting", this.easyHarvesting);
        line(sb, "useDefaultBiomeGroups", this.useDefaultBiomeGroups);
        line(sb, "attackDolphins", this.attackDolphins);
        line(sb, "attackWolves", this.attackWolves);
        line(sb, "attackHorses", this.attackHorses);
        line(sb, "enableHunters", this.enableHunters);
        line(sb, "spawnPiranhas", this.spawnPiranhas);
        line(sb, "modifyVanillaSpawns", this.modifyVanillaSpawns);
        line(sb, "despawnVanilla", this.despawnVanilla);
        line(sb, "enableOwnership", this.enableOwnership);
        line(sb, "enableResetOwnership", this.enableResetOwnership);
        line(sb, "staticBed", this.staticBed);
        line(sb, "staticLitter", this.staticLitter);
        line(sb, "animateTextures", this.animateTextures);
        line(sb, "destroyDrops", this.destroyDrops);
        line(sb, "destroyPassiveDrops", this.destroyPassiveDrops);
        sb.append('\n');

        sb.append("# --- Client display ---\n");
        line(sb, "displayPetName", this.displayPetName);
        line(sb, "displayPetHealth", this.displayPetHealth);
        line(sb, "displayPetIcons", this.displayPetIcons);
        line(sb, "particleFX", this.particleFX);
        sb.append('\n');

        sb.append("# --- Strengths, chances, caps ---\n");
        line(sb, "ogreStrength", this.ogreStrength);
        line(sb, "caveOgreStrength", this.caveOgreStrength);
        line(sb, "fireOgreStrength", this.fireOgreStrength);
        line(sb, "ogreAttackRange", this.ogreAttackRange);
        line(sb, "caveOgreChance", this.caveOgreChance);
        line(sb, "fireOgreChance", this.fireOgreChance);
        line(sb, "sharkStrength", this.sharkStrength);
        line(sb, "zebraChance", this.zebraChance);
        line(sb, "wyvernEggDropChance", this.wyvernEggDropChance);
        line(sb, "monsterEggDropChance", this.monsterEggDropChance);
        line(sb, "maxMobs", this.maxMobs);
        line(sb, "maxWaterMobs", this.maxWaterMobs);
        line(sb, "maxTamed", this.maxTamed);
        line(sb, "maxOPTamed", this.maxOPTamed);
        sb.append('\n');

        sb.append("# --- Per-entity spawn tuning (optional) ---\n");
        sb.append("# Uncomment and edit to override the default spawn weight / pack size for an\n");
        sb.append("# entity. Key form: spawn.<id>.frequency, spawn.<id>.min, spawn.<id>.max\n");
        sb.append("# where <id> is the lowercase entity id (e.g. elephant, shark, ogre).\n");
        sb.append("# Defaults: Mo'Creatures 12.0.5's own per-species weights and pack sizes (e.g. bird 15,\n");
        sb.append("# horse 8, elephant 4, golem 3); a merged species such as the bear sums the subspecies\n");
        sb.append("# 12.0.5 had in each biome. An override here applies in every biome.\n");
        // Round-trip any spawn.* overrides currently in the backing store so save() preserves them.
        boolean wroteOverride = false;
        for (String key : new java.util.TreeSet<>(this.props.stringPropertyNames())) {
            if (key.startsWith("spawn.")) {
                line(sb, key, this.props.getProperty(key));
                wroteOverride = true;
            }
        }
        if (!wroteOverride) {
            sb.append("#spawn.elephant.frequency=4\n");
            sb.append("#spawn.elephant.min=1\n");
            sb.append("#spawn.elephant.max=1\n");
        }

        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append('=').append(value).append('\n');
    }

    private static void line(StringBuilder sb, String key, boolean value) {
        sb.append(key).append('=').append(value).append('\n');
    }

    private static void line(StringBuilder sb, String key, int value) {
        sb.append(key).append('=').append(value).append('\n');
    }

    private static void line(StringBuilder sb, String key, double value) {
        sb.append(key).append('=').append(value).append('\n');
    }
}
