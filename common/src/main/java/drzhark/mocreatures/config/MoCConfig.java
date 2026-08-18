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
    /** Use the legacy curated per-creature biome-group defaults when an entity has no explicit biomegroup
     * override (legacy MoCProperties useDefaultBiomeGroups: true) — bears favour forests, crocs swamps, etc. */
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
     * through the vanilla pipeline and now uses spawn weights sized against vanilla's (see
     * DEFAULT_FREQ_CREATURE), so the cull is no longer needed to leave room — and it is destructive by nature:
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
    /** Percent chance a bred horse yields a zebra. Legacy ZebraChance: 2. */
    public int zebraChance;
    /** Percent chance a wyvern drops an egg on death. New tunable in the port. */
    public int wyvernEggDropChance;
    /** Percent chance a monster drops a monster-egg on death. New tunable in the port. */
    public int monsterEggDropChance;

    /** Soft cap of Mo'Creatures animals per player view (custom spawner). Port default 40 (legacy 90). */
    public int maxAnimals;
    /** Soft cap of Mo'Creatures monsters per player view (custom spawner). Port default 60 (legacy 70). */
    public int maxMobs;
    /** Soft cap of Mo'Creatures water mobs per player view. Port default 10 (legacy 30). */
    public int maxWaterMobs;
    /** Soft cap of Mo'Creatures ambient/insect mobs per player view. Port default 10 (legacy 20). */
    public int maxAmbient;
    /** Max tamed creatures a normal player may own. Legacy maxTamedPerPlayer: 10 (flag list suggested 5). */
    public int maxTamed;
    /** Max tamed creatures an op may own. Legacy maxTamedPerOP: 20. */
    public int maxOPTamed;

    // ------------------------------------------------------------------------------------------
    // Per-entity spawn tuning (keyed spawn.<id>.frequency / .min / .max)
    //   Downstream MoCSpawns reads these via spawnFrequency/spawnMin/spawnMax(String entityId).
    //   Absent keys fall back to the current uniform vanilla weights.
    // ------------------------------------------------------------------------------------------

    /** Backing store for the whole file, retained for the keyed per-entity spawn lookups. */
    private final Properties props;

    // Uniform fallback spawn weights, sized against the vanilla weights they actually compete with.
    //
    // What matters at chunk generation is weight x average pack size, not weight alone: vanilla's farm animals
    // all spawn in fixed packs of 4, so a plains biome is 46 points of weight but 182 points of "animals placed"
    // (sheep 12x4, pig 10x4, chicken 10x4, cow 8x4, horse 5x4, donkey 1x2). Mo'Creatures packs average 2.5
    // (min 1 / max 4 below), so its weights must be read against that 182, not against 46.
    //
    // These used to be creature 8 / insect 10 / water 8. That put ~26 Mo'Creatures species at 218 of the 264
    // points of CREATURE weight in a plains biome (83%) and crowded vanilla farm animals out of new chunks —
    // which is the pressure the (destructive) despawnVanilla cull existed to relieve. At creature 3 / insect 4
    // the mod places 21x3x2.5 + 5x4x2.5 = ~208 against vanilla's 182, i.e. a little over half of the animals in
    // a new chunk are Mo'Creatures, which is the point of the mod, while vanilla keeps a healthy 47%.
    //
    // Monsters were never the problem (8 species x 6 against vanilla's ~400 points of monster weight), and water
    // at 2 gives the four ocean species 8 points against vanilla ocean's 7. Both are unchanged.
    // Any server wanting different numbers can set spawn.<id>.frequency in mocreatures.properties.
    private static final int DEFAULT_GROUP_MIN = 1;
    private static final int DEFAULT_GROUP_MAX = 4;
    private static final int DEFAULT_FREQ_CREATURE = 3;
    private static final int DEFAULT_FREQ_MONSTER = 6;
    private static final int DEFAULT_FREQ_WATER = 2;
    private static final int DEFAULT_FREQ_INSECT = 4;

    // Ids whose default spawn weight is not the plain creature weight of 8.
    private static final java.util.Set<String> MONSTER_IDS = java.util.Set.of(
            "golem", "ogre", "rat", "scorpion", "wildwolf", "wild_wolf", "werewolf", "wraith",
            "hellrat", "hell_rat", "flamewraith", "flame_wraith", "horsemob", "horse_mob",
            "silver_skeleton", "mini_golem", "manticore", "medusa", "minotaur");
    private static final java.util.Set<String> WATER_IDS = java.util.Set.of(
            "dolphin", "fishy", "jellyfish", "ray", "shark", "small_fish", "medium_fish");
    private static final java.util.Set<String> INSECT_IDS = java.util.Set.of(
            "bee", "butterfly", "dragonfly", "firefly", "fly");

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
        this.zebraChance         = getInt(p, "zebraChance", 2);
        this.wyvernEggDropChance = getInt(p, "wyvernEggDropChance", 10);
        this.monsterEggDropChance= getInt(p, "monsterEggDropChance", 25);

        // Legacy population caps (MoCProxy: maxAnimals=90, maxMobs=70, maxWaterMobs=30, maxAmbient=20,
        // maxTamedPerPlayer=10) — the legacy world was denser than the port's earlier conservative values.
        this.maxAnimals          = getInt(p, "maxAnimals", 90);
        this.maxMobs             = getInt(p, "maxMobs", 70);
        this.maxWaterMobs        = getInt(p, "maxWaterMobs", 30);
        this.maxAmbient          = getInt(p, "maxAmbient", 20);
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
     * Spawn weight/frequency for an entity, keyed {@code spawn.<id>.frequency}. When the key is
     * absent it falls back to the uniform default weight for that entity's category
     * (creature 8, monster 6, water 8, insect 10). {@code id} is the lowercase registry path,
     * e.g. {@code "elephant"}, {@code "shark"}, {@code "ogre"}.
     */
    public int spawnFrequency(String entityId) {
        return spawnInt(entityId, "frequency", defaultFrequency(entityId));
    }

    /** Minimum pack size for an entity, keyed {@code spawn.<id>.min}. Defaults to 1. */
    public int spawnMin(String entityId) {
        return spawnInt(entityId, "min", DEFAULT_GROUP_MIN);
    }

    /** Maximum pack size for an entity, keyed {@code spawn.<id>.max}. Defaults to 4. */
    public int spawnMax(String entityId) {
        return spawnInt(entityId, "max", DEFAULT_GROUP_MAX);
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

    // ---- Per-entity biome-group assignment (legacy CustomSpawner biome groups) -------------------
    // Keyed spawn-restriction: biomegroup.<id> = comma list of the groups an entity is allowed to spawn in.
    // Empty (the default) means "spawn in every overworld biome" (unchanged behaviour). Consumed by MoCSpawns.

    /** The seven legacy biome groups the port recognises (desert is approximated by badlands/savanna). */
    private static final java.util.List<String> BIOME_GROUP_NAMES =
            java.util.List.of("forest", "arctic", "normal", "mountain", "jungle", "desert", "swamp");

    /**
     * Curated per-creature default biome groups, seeded from the legacy {@code MoCProperties.cfg}
     * {@code useDefaultBiomeGroups} table and mapped onto the seven overworld-terrain groups the port models.
     *
     * <p>Every group must have a substantial roster, because this is a hard gate: a creature whose groups do
     * not intersect the biome's is never added to that biome's spawn list at all. The legacy-transcribed table
     * left {@code desert} with two species (elephant, ostrich) and {@code arctic} with three (bear, bunny, fox),
     * so deserts, badlands, savannas and snowy biomes read as empty of Mo'Creatures fauna. The assignments below
     * keep every legacy entry and extend each creature to the terrain it plausibly belongs in — lions and
     * cheetahs on savanna, rattlesnakes and komodos in the desert, snow leopards and taiga boar in the arctic —
     * so no group has fewer than ten species.</p>
     *
     * <p>Aquatic / monster-category / nether / Wyvern-Lair creatures are intentionally absent: their legacy
     * groups (OCEAN/RIVER/BEACHES/MOBS/NETHER/WYVERNLAIR) have no overworld-terrain equivalent, so they are
     * placed by an explicit dimension or water-tag gate in {@code MoCSpawns} instead.</p>
     */
    private static final java.util.Map<String, java.util.List<String>> DEFAULT_BIOME_GROUPS =
            java.util.Map.ofEntries(
                    java.util.Map.entry("bear", java.util.List.of("forest", "normal", "arctic", "mountain", "jungle")),
                    java.util.Map.entry("bee", java.util.List.of("forest", "normal", "jungle", "mountain")),
                    java.util.Map.entry("big_cat", java.util.List.of("forest", "normal", "jungle", "mountain", "desert", "arctic")),
                    java.util.Map.entry("bird", java.util.List.of("forest", "normal", "jungle", "mountain", "desert", "arctic", "swamp")),
                    java.util.Map.entry("boar", java.util.List.of("forest", "normal", "jungle", "mountain", "arctic")),
                    java.util.Map.entry("bunny", java.util.List.of("forest", "normal", "jungle", "arctic", "mountain", "desert")),
                    java.util.Map.entry("butterfly", java.util.List.of("normal", "forest", "jungle", "mountain", "swamp")),
                    java.util.Map.entry("chimpanzee", java.util.List.of("forest", "jungle")),
                    java.util.Map.entry("crab", java.util.List.of("normal", "swamp", "jungle", "desert")),
                    java.util.Map.entry("cricket", java.util.List.of("forest", "normal", "jungle", "mountain", "desert", "swamp")),
                    java.util.Map.entry("crocodile", java.util.List.of("swamp", "jungle")),
                    java.util.Map.entry("deer", java.util.List.of("forest", "normal", "arctic", "mountain")),
                    java.util.Map.entry("dragonfly", java.util.List.of("forest", "normal", "jungle", "mountain", "swamp")),
                    java.util.Map.entry("duck", java.util.List.of("forest", "normal", "jungle", "swamp")),
                    java.util.Map.entry("elephant", java.util.List.of("forest", "normal", "desert", "jungle")),
                    java.util.Map.entry("firefly", java.util.List.of("forest", "normal", "jungle", "mountain", "swamp")),
                    java.util.Map.entry("fly", java.util.List.of("forest", "normal", "jungle", "mountain", "desert", "swamp")),
                    java.util.Map.entry("fox", java.util.List.of("forest", "jungle", "arctic", "normal", "mountain")),
                    java.util.Map.entry("goat", java.util.List.of("forest", "normal", "mountain", "arctic")),
                    java.util.Map.entry("horse", java.util.List.of("forest", "normal", "mountain", "desert", "arctic")),
                    java.util.Map.entry("kitty", java.util.List.of("normal", "forest")),
                    java.util.Map.entry("komodo", java.util.List.of("swamp", "jungle", "desert")),
                    java.util.Map.entry("maggot", java.util.List.of("forest", "normal", "jungle", "mountain", "desert", "swamp")),
                    java.util.Map.entry("mouse", java.util.List.of("forest", "jungle", "normal", "mountain", "desert", "arctic", "swamp")),
                    java.util.Map.entry("ostrich", java.util.List.of("normal", "desert")),
                    java.util.Map.entry("roach", java.util.List.of("normal", "swamp", "mountain", "forest", "jungle", "desert")),
                    java.util.Map.entry("snail", java.util.List.of("forest", "jungle", "normal", "swamp")),
                    java.util.Map.entry("snake", java.util.List.of("forest", "jungle", "normal", "mountain", "desert", "swamp")),
                    java.util.Map.entry("turkey", java.util.List.of("normal", "forest", "desert")),
                    java.util.Map.entry("turtle", java.util.List.of("swamp", "jungle", "normal")));

    public static java.util.List<String> biomeGroupNames() {
        return BIOME_GROUP_NAMES;
    }

    /** The biome groups an entity is restricted to, or an empty list = no restriction (spawns everywhere). */
    public java.util.List<String> biomeGroups(String entityId) {
        if (entityId == null) {
            return java.util.List.of();
        }
        String v = props.getProperty("biomegroup." + entityId.toLowerCase(java.util.Locale.ROOT), "");
        if (v.isBlank()) {
            // No explicit override: fall back to the legacy curated defaults (so creatures favour their native
            // biomes) unless the user has turned useDefaultBiomeGroups off, in which case spawn everywhere.
            if (this.useDefaultBiomeGroups) {
                return DEFAULT_BIOME_GROUPS.getOrDefault(
                        entityId.toLowerCase(java.util.Locale.ROOT), java.util.List.of());
            }
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String s : v.split(",")) {
            String t = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    /** Adds a biome group to an entity's allow-list (call {@link #save()} to persist). False if unknown group. */
    public boolean addBiomeGroup(String entityId, String group) {
        String g = group == null ? "" : group.toLowerCase(java.util.Locale.ROOT);
        if (!BIOME_GROUP_NAMES.contains(g) || entityId == null) {
            return false;
        }
        java.util.List<String> cur = new java.util.ArrayList<>(biomeGroups(entityId));
        if (!cur.contains(g)) {
            cur.add(g);
        }
        props.setProperty("biomegroup." + entityId.toLowerCase(java.util.Locale.ROOT), String.join(",", cur));
        return true;
    }

    /** Removes a biome group from an entity's allow-list (call {@link #save()} to persist). */
    public boolean removeBiomeGroup(String entityId, String group) {
        if (entityId == null) {
            return false;
        }
        String g = group == null ? "" : group.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> cur = new java.util.ArrayList<>(biomeGroups(entityId));
        boolean removed = cur.remove(g);
        props.setProperty("biomegroup." + entityId.toLowerCase(java.util.Locale.ROOT), String.join(",", cur));
        return removed;
    }

    private static int defaultFrequency(String entityId) {
        if (entityId == null) {
            return DEFAULT_FREQ_CREATURE;
        }
        String id = entityId.toLowerCase(java.util.Locale.ROOT);
        if (MONSTER_IDS.contains(id)) {
            return DEFAULT_FREQ_MONSTER;
        }
        if (WATER_IDS.contains(id)) {
            return DEFAULT_FREQ_WATER;
        }
        if (INSECT_IDS.contains(id)) {
            return DEFAULT_FREQ_INSECT;
        }
        return DEFAULT_FREQ_CREATURE;
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
            case "maxanimals":           this.maxAnimals = (int) Math.round(value);    return true;
            case "maxmobs":              this.maxMobs = (int) Math.round(value);       return true;
            case "maxwatermobs":         this.maxWaterMobs = (int) Math.round(value);  return true;
            case "maxambient":           this.maxAmbient = (int) Math.round(value);    return true;
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
        line(sb, "maxAnimals", this.maxAnimals);
        line(sb, "maxMobs", this.maxMobs);
        line(sb, "maxWaterMobs", this.maxWaterMobs);
        line(sb, "maxAmbient", this.maxAmbient);
        line(sb, "maxTamed", this.maxTamed);
        line(sb, "maxOPTamed", this.maxOPTamed);
        sb.append('\n');

        sb.append("# --- Per-entity spawn tuning (optional) ---\n");
        sb.append("# Uncomment and edit to override the default spawn weight / pack size for an\n");
        sb.append("# entity. Key form: spawn.<id>.frequency, spawn.<id>.min, spawn.<id>.max\n");
        sb.append("# where <id> is the lowercase entity id (e.g. elephant, shark, ogre).\n");
        sb.append("# Defaults: frequency 8 (creature) / 6 (monster) / 8 (water) / 10 (insect),\n");
        sb.append("# min 1, max 4.\n");
        // Round-trip any spawn.* overrides currently in the backing store so save() preserves them.
        boolean wroteOverride = false;
        for (String key : new java.util.TreeSet<>(this.props.stringPropertyNames())) {
            if (key.startsWith("spawn.")) {
                line(sb, key, this.props.getProperty(key));
                wroteOverride = true;
            }
        }
        if (!wroteOverride) {
            sb.append("#spawn.elephant.frequency=8\n");
            sb.append("#spawn.elephant.min=1\n");
            sb.append("#spawn.elephant.max=4\n");
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
