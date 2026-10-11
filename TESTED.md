# Test ledger

What has been verified to work, and against what. A fact stays KNOWN while nothing it depends on has changed since its
commit (`git diff --name-only <commit>..HEAD -- <paths>` from the repo root, whose paths are the ones below) and the
versions match; anything else goes into the next final pass. Machine rule "Testing" in `~/.claude/CLAUDE.md`.

Versions of the pass of 2026-10-10 (commit `00497ee`): Minecraft 26.2, Architectury API 21.0.2.
- Fabric: Loader 0.19.3 + Fabric API 0.153.0+26.2 (the build's own; plain run), and Loader 0.19.5 + Fabric API
  0.161.0+26.2 (the newest; the perf, popular and tour reruns).
- NeoForge 26.2.0.7-beta.
- Tools: `tools/releasetest/releasetest.py check <loader> <port> <rcon> [--set <set>] [--tour-only]` (real dedicated
  server from the official installer/launcher with the release jar, server checks over RCON by
  `tools/servertest/servertest.py`, a real client joining it and then singleplayer in a copy of the world, both running
  the tour `tools/testclient/moctour.py`, every log scanned). Dev runs: `gradlew :fabric:runTestServer` +
  `servertest.py mechanic`.

Common paths: `common/` = `common/src/main/java/drzhark/mocreatures/`. "Release jars" facts depend on everything that
goes into a jar: `common/`, `fabric/`, `neoforge/`, `gradle.properties`, every `build.gradle`.

| What works | Loaders | Versions | Commit, date | How checked | Depends on |
|---|---|---|---|---|---|
| The release jars load on a real dedicated server and a real client; server, multiplayer-client and singleplayer-client logs have no mixin, missing-class, refmap or loading errors | Fabric, NeoForge | above | 00497ee, 2026-10-10 | release check, plain set (server + client joining + singleplayer); `scan_log` | the release jars (see above) |
| Every Mo'Creatures entity summons and every loot table spawns on a dedicated server | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest entities + loot (14 tables) | `common/entity/`, `common/registry/`, `common/src/main/resources/data/mocreatures/loot_table/` |
| A hungry tamed kitty climbs into a stocked bed, eats until it is empty and is fed | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `kitty_bed` (dev server and release servers) | `common/entity/passive/MoCEntityKitty.java`, `MoCEntityKittyBed.java` |
| At night every calm, fed, tamed kitty turns in: one kitty per free bed, the rest sleep on the floor; they stay asleep (and do not get hungrier) until day, which wakes them and gets them out of the beds | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `kitty_night` (3 kitties, 2 beds, midnight, 60 s hold, `time set day`), dev server + both release servers | `MoCEntityKitty.java`, `MoCEntityKittyBed.java` |
| A kitty whose bed is unreachable gives it up and sleeps in a reachable one | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `kitty_unreachable` (nearest bed in a glass ring); failed on the code before the fix, passes after | `MoCEntityKitty.java` |
| At night a kitty that has eaten in its bed stays in it and falls asleep there; in a bed (eating or asleep) or asleep on the floor it holds one yaw (in a bed: the open side +/- 45 degrees), head still; asleep its eyes are shut and small "z"s rise from its head; hungry, playing and eating kitties show their mood icon above the head; a punched kitty bed bursts into break particles | Fabric (shared code: one loader, by the testing rule) | Fabric Loader 0.19.5, Fabric API 0.161.0 | the commit adding this row, 2026-10-11 | one real Fabric client in singleplayer: states 0 -> 4 -> 3 while riding the bed, yaw 92.9 in five samples (bed yaw 0 -> 45..135), screenshots looked at (eyes, z's in three frames, icons, break burst) | `MoCEntityKitty.java`, `MoCEntityKittyBed.java`, `common/client/renderer/MoCMobRenderer.java`, `common/client/state/MoCEntityRenderState.java`, `textures/models/pussycat?_sleep.png`, `textures/misc/sleep_z.png` |
| A right click on the kitty bed's food bowl with a milk bucket stocks the bed and gives the bucket back, in multiplayer and singleplayer | Fabric, NeoForge | above, and every compatibility set below | 00497ee, 2026-10-10 | tour `kitty_bowl` (the agent's real `use` with the crosshair on the bowl) | `MoCEntityKittyBed.java`, `common/MoCreatures.java` (Architectury RIGHT_CLICK_BLOCK/ITEM) |
| A ridden horse never rears, while it bucks and while it is hurt; a third-person screenshot shows the rider on the saddle | Fabric, NeoForge | above | 00497ee, 2026-10-10 | tour `horse_no_rear` (synced rearing flag read on the client twice a second, hurt every 2 s; 0 rears in every run) | `common/entity/passive/MoCEntityHorse.java`, `common/entity/MoCAnimal.java`, `common/client/model/MoCModelHorse*` |
| An empty hand on a wild MoC horse shows the taming hint; a wild vanilla horse still takes a rider | Fabric, NeoForge | above | 00497ee, 2026-10-10 | tour `horse_hint`, `vanilla_mount` | `MoCEntityHorse.java`, `MoCAnimal.java` |
| A tamed MoC horse and a tamed vanilla horse in love foal a MoC foal of the 12.0.5 coat | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `crossbreed` | `MoCEntityHorse.java` |
| Led MoC horse, bear, elephant, ostrich and big cat walk up a 1-block step behind the holder | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `lead_step` (tick-timed datapack, vanilla horse and a 0.6-step horse as controls) | `common/entity/passive/` (STEP_HEIGHT attributes), `MoCAnimal.java` |
| No cap removes animals (120 wild deer survive three cap sweeps; the monster cap still trims rats) | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `animal_cap` | `common/spawn/MoCMobCap.java`, `common/config/MoCConfig.java` |
| Spawn lists are 12.0.5's: species, pools and weights per Forge 1.12 biome type in 12 test biomes | Fabric, NeoForge | above, also with BetterEnd/BetterNether/Naturalist/Friends&Foes installed | 00497ee, 2026-10-10 | servertest `biome_placement` (`/moc spawnlist`) | `MoCConfig.java`, `common/spawn/MoCBiomeTypes.java`, `common/registry/MoCSpawns.java`, both loaders' biome modifier glue |
| Fresh chunks get Mo'Creatures animals at generation, and none vanish afterwards | Fabric, NeoForge | above | 00497ee, 2026-10-10 | servertest `spawn_census` (256 never-visited chunks of a plains) | as the row above |
| Mo'Creatures insects spawn around a player (ambient pool) | Fabric, NeoForge | above | 00497ee, 2026-10-10 | tour `ambient_insects` (11-16 insects in 75 s on a plains) | `common/registry/MoCEntities.java`, `MoCSpawns.java`, `MoCConfig.java` |
| The scorpions' carapace stays on the body while they turn; the kitty bed and its bowl, the Wyvern Lair terrain and sky render | Fabric, NeoForge | above, also under shaders | 00497ee, 2026-10-10 | tour screenshots, looked at | `common/client/model/MoCModelScorpion*`, `MoCModelKittyBed.java`, `common/client/MoCLairSky.java`, `fabric/` sky mixin |
| Compatible with Sodium 0.9.2 (`mc26.2-0.9.2-fabric`) | Fabric | Loader 0.19.3, Fabric API 0.153.0 | 00497ee, 2026-10-10 | release check `--set sodium`: 13/13 | rendering and client code in `common/client/`, `fabric/` render hooks |
| Compatible with Sodium 0.9.2 + Iris 1.11.4 + Complementary Reimagined r5.9.3, shaders ON | Fabric; NeoForge | Fabric 0.19.3; NeoForge 26.2.0.7-beta (Sodium/Iris `-neoforge` builds) | 00497ee, 2026-10-10 | release check `--set shaders:complementary-reimagined`: 15/15 each (Iris pipeline built, no shader failure; screenshots, the Lair included) | as above, and `neoforge/` render hooks |
| Compatible with Sodium 0.9.2 + Iris 1.11.4 + Complementary Unbound r5.9.3, shaders ON | Fabric | Loader 0.19.3 | 00497ee, 2026-10-10 | release check `--set shaders:complementary-unbound`: 15/15 | as above |
| Compatible with the 16 most-downloaded 26.2 Fabric performance mods together: Sodium 0.9.2, Iris 1.11.4, EntityCulling 1.11.3, FerriteCore 9.0.0, Lithium 0.25.3, ImmediatelyFast 1.16.5, Sodium Extra 0.9.4, MoreCulling 1.8.1, Dynamic FPS 3.11.9, BadOptimizations 2.4.1, Krypton 0.3.1, Language Reload 1.7.8, C2ME 0.4.2-alpha.0.56, Debugify 26.2.0.1, Distant Horizons 3.3.3, Cloth Config 26.2.155 (the server-capable ones on the server) | Fabric | Loader 0.19.5, Fabric API 0.161.0 | 00497ee, 2026-10-10 | release check `--set perf`: every server check passed with the server mods; tour rerun (`--tour-only`) 13/13 in multiplayer and singleplayer; logs clean | the release jar; worldgen/spawn code in `common/spawn/`, `common/registry/` |
| Compatible with the popular mods: Entity Model Features 3.3.11, Dungeons and Taverns 5.3.2, BetterEnd 26.201.2, BetterNether 26.201.2, Friends&Foes 5.0.2, Naturalist 2.0.6, JEI 30.39.0.233, Jade 26.2.11, ModMenu 20.0.3, Xaero's Minimap 26.6.0, Distant Horizons 3.3.3, Entity Texture Features 7.2.5 (+ WorldWeaver, BCLib, Resourceful Lib, MezzConfig, Placeholder API) | Fabric | Loader 0.19.5, Fabric API 0.161.0 | 00497ee, 2026-10-10 | release check `--set popular`: every server check passed (biome placement and census included, with BetterEnd/BetterNether worldgen), the tour passed in multiplayer and singleplayer, logs clean | as above |
| A failed launch is caught within seconds and no error window is left: a join to a closed port, a mod set the loader refuses (no Architectury API), a loader-like error window | Fabric, NeoForge | above | 00497ee, 2026-10-10 | `releasetest.py probe connect|refused|window` | `tools/testclient/`, `tools/releasetest/` |
| Test clients stay minimized and unfocused through every run | Fabric, NeoForge | above | 00497ee, 2026-10-10 | `windowguard.ps1` in every check: 0 violations, 0 dialogs | `tools/testclient/` |

Not covered: Quilt and Forge (Mo'Creatures ships for Fabric and NeoForge only); NeoForge with the perf and popular
sets (only the short Iris check is due there by the compatibility rule's scope).

Notes from the 2026-10-10 pass (test setup, not mod bugs):
- The perf and popular sets need the newest Fabric Loader and Fabric API (Distant Horizons 3.3.3 needs Loader
  >=0.19.5, BCLib 26.201.2 Fabric API >=0.155.0); the release check now takes both newest.
- Dynamic FPS stops rendering a minimized window, which hung the minimized client's singleplayer world load; WorldWeaver
  opens a click-to-continue welcome screen on a fresh game folder. The release check presets both mods' configs.
- Known noise from other mods is filtered in `releasetest.py` (`NOISE`): their optional mixins probing absent mods,
  refmaps, "recommends" notices, BCLib patching a fresh world, Distant Horizons x BetterEnd water tint.
