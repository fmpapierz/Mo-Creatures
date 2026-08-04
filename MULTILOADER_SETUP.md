# Mo'Creatures — Architectury multi-loader for Minecraft 26.2 (COMPLETE)

`./gradlew build` produces **both** loader jars, each containing the full mod:
- `fabric/build/libs/mocreatures-fabric-26.2.0+26.2.jar`
- `neoforge/build/libs/mocreatures-neoforge-26.2.0+26.2.jar`

All 46 creatures, 182 items, 14 blocks, the Wyvern Lair dimension and natural spawning are migrated
into `common/` and shared by both loaders. **Validated at runtime:** both the Fabric and NeoForge dev
servers (`./gradlew :fabric:runServerDebug` / `:neoforge:runServerDebug`) boot to `Done`, log
`Mo'Creatures … initialized with 46 creatures`, register the `mocreatures:wyvern_lair` dimension, and
report no mod errors.

## Architecture

- **`common/`** — everything: ~104 pure-vanilla classes (entities, models, renderers, items — they use
  only `net.minecraft.*`, so they compile once against `com.mojang:minecraft:26.2` and run on both
  loaders) plus the cross-loader registration layer written against **Architectury API 21.0.2**:
  - `registry/MoCEntities|MoCItems|MoCBlocks|MoCSounds|MoCDataComponents|MoCCreativeTabs` — Architectury
    `DeferredRegister` / `RegistrySupplier`.
  - `registry/MoCSpawns` — `SpawnPlacementsRegistry` (placements) + `BiomeModifications` (overworld
    spawns; replaces NeoForge's `neoforge:add_spawns` biome-modifier JSON, which is loader-specific).
  - `MoCreatures.init()` — registers everything, wires attributes via `EntityAttributeRegistry`, and
    schedules client rendering via `EnvExecutor.runInEnv(Env.CLIENT, …)`.
  - `client/MoCreaturesClient` — `EntityModelLayerRegistry` + `EntityRendererRegistry`.
- **`fabric/` / `neoforge/`** — thin entrypoints that just call `MoCreatures.init()`.

### The two build rules that make 26.x work

1. **MC 26.x ships UNOBFUSCATED → no mappings at all.** Plugin `dev.architectury.loom-no-remap`
   `1.14-SNAPSHOT` + `architectury-plugin 3.5-SNAPSHOT` + `com.gradleup.shadow 9.4.x`,
   `loom.ignoreDependencyLoomVersionValidation=true`, Gradle on JDK 25, standard
   `neoForge "net.neoforged:neoforge:$ver"`, no `remapJar` (each platform's `shadowJar` is the artifact).
2. **`common` compiles against plain vanilla Minecraft, which lacks NeoForge's convenience overloads.**
   e.g. use `CreativeModeTab.builder(Row.TOP, 0)`, not NeoForge's no-arg `builder()`; in 26.2 items /
   blocks / entity-types must carry their registry id at construction via `.setId(ResourceKey…)` (the
   NeoForge `DeferredRegister.Items/Blocks` helpers set it automatically — Architectury's generic
   `register` does not, so we set it explicitly).

Dependencies (loom-no-remap uses plain `implementation`/`api`, NOT `modImplementation`):
common `implementation "dev.architectury:architectury:21.0.2"` (+ `injectInjectables = false`),
fabric `api "…:architectury-fabric:21.0.2"`, neoforge `api "…:architectury-neoforge:21.0.2"`.
This matches the user's other working 26.2 mods (Lycanites Mobs, Falling Leaves, FindMe, Dynamic
Surroundings).

## Regenerating the registration layer

`scratchpad/gen-arch.js` parses the single-loader NeoForge sources
(`_mocreatures_aside/_singleloader_backup`) and emits the Architectury-converted registry/client/init
classes deterministically. The one manual touch-up after generation is the `CreativeModeTab.builder`
arity (see rule 2). The generated `.java` files are committed, so the generator is only needed to
re-derive them.

## Note on the `Mo' Creatures/Mo' Creatures 26.2/` directory

That directory has the same (correct) build files and a synced copy of `common/`, but its local
Gradle/Loom state is stuck: `neoForge()` fails to resolve there even though byte-identical files build
fine here (and a fresh `:neoforge:help` succeeds in this directory). It likely needs a reboot to clear.
**This `Mo' Creatures 26.2 Multiloader/` directory is the working deliverable.**
