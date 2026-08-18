# Handoff — resume point, 2026-08-11

Everything below is **uncommitted working-tree state** on `main` (last commit `40b35cf`). The build is
green on both loaders (`./gradlew.bat build -x test` → BUILD SUCCESSFUL), but an adversarial review
found defects that were **never applied because the session ended**. Read §3 before you tell the user
anything is finished.

---

## 1. What shipped this session

### Ogre Prince + Ogre Lair (user-approved design, then built)

- **`mocreatures:ogre_prince`** — `MoCEntityOgrePrince` extends `MoCEntityOgre`. TypeMoC 1 green /
  2 fire / 3 cave. 150 HP, armor 5, KB resist 1.0, melee 8, xp 40, `sizeFactor` 1.4, hitbox 2.7×5.6.
  Per-variant `ServerBossEvent` (green/red/blue) wired on the WitherBoss idiom. Always aggressive (no
  darkness gate), immune to the fire/cave ogres' bright-sky burn, `requiresCustomPersistence()` true,
  `.notInPeaceful()` on the builder, smash radius = variant config strength + 1.5, damage 12.
  Signature moves: green = ravager-style launch on hit, fire = ignite on hit + two heads + fire rim,
  cave = enderman-style `randomTeleport` to a distant target.
- **Drops, guaranteed one per variant** (this mapping is DrZhark's, not ours — see §4):
  green → `builderhammer2` (renamed **"Ogre Hammer"**), fire → new **`machete`** ("Ogre Machete",
  diamond-tier, ignites on hit, fire-resistant), cave → `staffteleport` (renamed **"Staff of Teleport"**).
  All three are now survival-obtainable for the first time.
- **`mocreatures:ogre_lair` dimension** — full datapack clone of the Wyvern Lair: `dimension/`,
  `dimension_type/` (+ the NeoForge twin carrying `neoforge:custom_skybox`), `noise_settings/` (End-style
  floating islands in the ogre block set), `biome/` (ogres w8 + princes w1 in `monster`), configured/
  placed features for grass, trees and the portal. Ember-ring sky on both loaders
  (`MoCLairSky.sunTextureFor` + parameterized `MoCTwinSuns.draw`, Fabric mixin + second NeoForge
  skybox registration). Travel item **`staffportalogre`** ("Ogre Portal Staff", durability 3,
  ender eye + **heart of fire** + blaze rod) — `MoCStaffPortalItem` is now parameterized by target
  dimension, so the wyvern staff is unchanged.
- **Portal frame guard fixed in BOTH dimensions.** The wyvern portal had shipped since day one with a
  ±24-**block** radius guard, which admits ≥4 chunks' placement attempts → 4–9 overlapping frames. Both
  features now gate on chunk identity (`(origin.getX() >> 4) != 0 || (origin.getZ() >> 4) != 0`).
  Old worlds keep whatever already generated; only fresh chunks are correct.

### Medusa, Minotaur, Chimpanzee (design-and-build, approved wholesale)

- **`mocreatures:medusa`** — `MoCMob`, 40 HP / 0.28 / 5, hitbox 1.2×2.1, TypeMoC 1-3
  (`medusagreen/tan/blue.png`). **Stiffening gaze** (ours): every 10 ticks, 40-tick cooldown, if the
  target player is within 12 blocks and looking at her (`LivingEntity.isLookingAtMe`, EnderMan's call
  minus the pumpkin mask) → Slowness III + Weakness, 60 ticks, amethyst-resonate sound. Poison bite via
  `applyHitEffects`. Snake hiss sound set. Drops 1-2 emeralds + 1-in-8 random snake `mocegg` (21-28).
  Serpentine locomotion uses the in-repo snake's travelling-wave technique down a parent-child chain —
  that animation is *the* thing that killed her in 2015, so it was the deliberate risk to retire first.
- **`mocreatures:minotaur`** — `MoCMob`, 50 HP / 0.3 / 7 / KB resist 0.6, hitbox 1.4×2.6, TypeMoC 1-3
  (`minotaurwhite` = Holstein per the render, `brown`/`black` ours). **Charge** (ours): target 5-16
  blocks with LOS and cooldown 0 → up to 40 ticks of `navigation.moveTo(target, 2.3)`; on closing to
  2.2 blocks, 10 damage + ravager-style launch + `hurtMarked` + `RAVAGER_ATTACK`; cooldown 200.
  Cow-variant-map sounds (`SoundEvents.COW_SOUNDS` — there are no `COW_AMBIENT` constants in 26.2)
  pitched 0.6. Drops 0-2 leather + 0-2 beef.
- **`mocreatures:chimpanzee`** — `MoCAnimal`, 15 HP / 0.3 / 2, hitbox 0.9×1.1, single texture.
  Raccoon-mold: tames on any edible, **breeds** (the one deliberate divergence — its `mobInteract` only
  consumes when `!tamed || hurt || !adult` so a fed tamed adult falls through to vanilla love mode),
  growth curve copied verbatim from the raccoon (`spawnAge(50,64).babyRoll(3).grow(300,100,100)`),
  restricted to `forest`+`jungle` biome groups in `MoCConfig.DEFAULT_BIOME_GROUPS`. Panda sounds at
  pitch ×1.4 are a **flagged placeholder** (no chimp audio exists; precedent is the silent Ent).
  Knuckle-walking quadruped model with arms longer than legs.

### Ogre head clipping (the user's in-game report — **see §3, may not be fixed in motion**)

Measured a real 4px interpenetration between the centre head and both side heads on triple-head ogres
(types 2/4/6). Legacy 12.0.5 had the identical ±7 pivots and masked it by rendering only two heads and
yaw-tracking one at a time. Applied a fan-out instead: side clusters moved to ±12.2 (ogre) / ±7 (prince),
y −12.8, with ±10° baked yaw (`HEAD2_BASE_YAW`/`HEAD3_BASE_YAW`), and `setupAnim` writes
`BASE_YAW + headYaw` to the head cube **and every flat root-level sub-cube of that cluster** (they are
NOT children — that is why cluster rigidity has to be maintained by hand, or brows/tusks detach).
Rest-pose clearance verified by SAT script.

### Housekeeping

- Creature-count log literal → **60** (`MoCreatures.java:65`) and TEST_PLAN updated. Note the literal is
  hardcoded, not a count — it has been wrong before.
- `GAP_AUDIT.md` H2 closed as stale (the wyvern noise settings it demanded now exist); H4 annotated
  (the three ogre-strength config flags are live now).
- Deleted the confirmed-dead `textures/blockstates/dirt_ogre_lair.png` (byte-identical duplicate).
- Lang: added all new names; renamed placeholders "Builderhammer2" → **Ogre Hammer**, "Staff Teleport" →
  **Staff of Teleport**.

---

## 2. What the user has actually tested

They ran `:fabric:runClient` once (this session) and reported exactly one defect: **multi-head ogres'
heads clip into each other**. Everything else in `TEST_PLAN.md` §12-13 and the Ogre Lair list is
UNTESTED by a human. The shutdown crash in that run (`crash-2026-08-11_18.32.02-client.txt`) is a
vanilla `ClientShutdownWatchdog` teardown hang, not mod code — the only mocreatures mention in it is
the mod-list line.

---

## 3. Pending work — apply these first

> **STATUS 2026-08-11 (later session): ALL ITEMS BELOW ARE RESOLVED.** #1-#5 applied as specified
> (#5 got the full move-control fix, not just the comment). #6 was verified TRUE by SAT sweeps of the
> real baked geometry + real setupAnim — but the suggested damping fix was disproven (damping the side
> heads makes clipping WORSE; the driver is the centre head's own ~10.4px corner sweep at full
> yaw+pitch, which no wearable pivot width can absorb). Shipped fix: ogre side pivots ±12.2 → ±13.5,
> side clusters take look pitch at half rate, and `MoCEntityOgre`/`MoCEntityOgrePrince` clamp
> `getMaxHeadYRot`/`getMaxHeadXRot` on multi-head variants (15/25 ogre, 25/25 fire prince) so vanilla
> `BodyRotationControl` turns the whole body beyond the clamp. Fire prince pair widened ±7 → ±7.5.
> SAT-verified zero head-box interpenetration across the clamped envelopes + margin (ogre ±21°/±30°,
> prince ±30°/±30°, 1° grid); residual is the accepted §3d ear-seam class (≤2.7px). #7 was verified
> TRUE (permanent coplanar seam — both parts rotate about the same pivot, so the shared x=±4 planes
> never separate) and fixed with an x-only `CubeDeformation(0.15, 0, 0)` flare on the beard's upper
> slab: geometry moves to ±4.15, UV layout untouched. Coplanar patch measured 0 at all poses after.

An adversarial review (3 finder lenses × 2 refuters per finding) ran against the three new mobs and the
head fix. **6 of its 19 agents died on a session limit**, so the buckets below are not all equal.

### 3a. CONFIRMED (both refuters traced it and agreed it is real)

| # | File:line | Defect | Fix |
|---|---|---|---|
| 1 | `config/MoCConfig.java:184` | `MONSTER_IDS` never got `"medusa"`/`"minotaur"`, so `defaultFrequency()` falls through to `DEFAULT_FREQ_CREATURE` (3) instead of `DEFAULT_FREQ_MONSTER` (6) — both spawn at **half** the weight of every other MoC monster. | Add `"medusa"`, `"minotaur"` to the `MONSTER_IDS` set. |
| 2 | `registry/MoCEntities.java:110-113` | MEDUSA/MINOTAUR builders lack `.notInPeaceful()` (the Ogre Prince two lines above has it). In 26.2 peaceful removal is *exclusively* flag-driven, so both persist in Peaceful and `/summon` works on them there. | Append `.notInPeaceful()` to both builder chains. |
| 3 | `entity/monster/MoCEntityMinotaur.java:101` | Charge impact fires on `distanceTo(target) <= 2.2` alone; LOS is only checked at charge *start* (line 123). The 10-damage blow + launch land **through walls**. | `&& this.hasLineOfSight(target)` on the impact condition, mirroring `MeleeAttackGoal.canPerformAttack`. |
| 4 | `entity/monster/MoCEntityMinotaur.java:101` | The impact branch never calls `swing()`, so `state.attackSwing` stays 0 and `MoCModelMinotaur`'s horn-toss **never animates on the signature hit**. | `this.swing(InteractionHand.MAIN_HAND);` in the impact branch (needs the `InteractionHand` import). Verified present at `mc262-ref LivingEntity.java:2010`. |

### 3b. ONE-VOTE CONFIRMED (its second refuter died; the surviving one traced it and confirmed)

5. **`MoCEntityMinotaur.java:100` + the javadoc at lines 77-82.** The comment claims the 2.3-speed
   `moveTo` "overrides the melee goal's pathing for the tick", but `Mob.serverAiStep` runs
   `navigation.tick()` **before** `customServerAiStep` (`mc262-ref Mob.java:714`), so the speed lands a
   tick late, and every `MeleeAttackGoal` path-recalc tick (every 4-11 ticks against a moving target)
   re-issues speed 1.0. Charge is ~10-20% slower and stuttery. *Severity: polish.* Fix: drive the move
   control directly (`getMoveControl().setWantedPosition(x, y, z, 2.3D)` — verified at
   `mc262-ref MoveControl.java:40`) or suppress the melee goal for the charge duration; **at minimum
   correct the comment**, which is currently wrong about vanilla ordering.

### 3c. UNVERIFIED — both refuters died. NOT refuted. Verify before trusting.

6. **`client/model/MoCModelOgre.java:436` (and `MoCModelOgrePrince`) — the head fix may only work at
   rest.** Claim: `setupAnim` adds the *full* look yaw to all three heads, so an SAT check on the head
   footprints (centre 12×12 at x 0, side 10×12 at x ±12.2, 10° relative) shows interpenetration once
   `|yRot|` exceeds ~11°, reaching ~3px at 45°; mob head yaw routinely hits `getMaxHeadYRot()` = 75° via
   `LookAtPlayerGoal`. The prince's ±7 pair is claimed clean to ~43°, then clipping. **This is the
   user's original bug report** — if true, they will see it again the moment an ogre watches them
   off-axis. Suggested fix: damp the look yaw on the extra-head clusters (`BASE_YAW + headYaw * 0.55F`,
   mirrored) or widen the ogre pivots to ~13.5. Any fix must keep every sub-cube of a cluster on the
   same yaw expression as its head.
7. **`client/model/MoCModelOgrePrince.java:470` — green prince beard z-fights the base ogre `Lip`.**
   Claim: the beard upper slab spans x −4..4, the same planes as `Lip`, both rotating about the same
   head pivot, giving a coplanar ~1px patch per side with both faces opaque on `ogreprincegreen.png`.
   *Severity: polish.* Suggested fix: widen to ±4.15 (mind that the sheet was painted for an 8-wide box —
   check the artist's face rects still sample acceptably).

A verification workflow for #6 and #7 (geometry lens + skeptical runtime lens + beard/texture sampling,
producing exact patches) was launched and **stopped before completion** at the user's request. Script is
persisted at
`.claude/projects/…/3f98f042-…/workflows/scripts/verify-model-findings-wf_fc130c2b-212.js` — re-running
it is the cheapest way to settle both.

### 3d. Known, deliberate, not bugs

- **Roster-wide peaceful debt:** no Mo'Creatures monster except `ogre_prince` carries `.notInPeaceful()`.
  Fixing medusa/minotaur (3a #2) is the local fix; a full roster sweep is a separate, larger decision.
- Sub-cube ear seams still touch between ogre heads (~2px) — clearing them needs absurd pivot widths.
- Chimpanzee has no real audio; panda sounds are a flagged placeholder.

---

## 4. Context a fresh session must not re-derive

- **New evidence found this session.** The Fandom Ogre Prince page carries a **direct DrZhark quote**
  (minecraftforum comment #64650): *"The ogre's lair will add three ogre princes (bosses), each will drop
  an unique item. Two of the items are available on creative. The staff of teleport and the hammer. The
  third item is not done yet."* That makes the staff/hammer pairing evidence, not fan guesswork. The
  fire prince's blade ("some type of axe/machete") comes from the render.
- **`HANDOFF_UNRELEASED_MOBS.md` was wrong about one thing** and is now corrected in place: the spare
  `staff.png`/`staff2.png`/`staff3.png` art is a wood/gold/diamond **scepter tier**, not the fire
  prince's weapon. It remains unused — a candidate for Medusa/Minotaur gear.
- **Licensing line held throughout.** No pixels from any wiki/imgur render are in the repo. Prince sheets
  are the GPLv3 ogre sheets verbatim in rows 0-127 with original regalia painted into a new 128×256
  bottom half; Medusa/Minotaur/Chimpanzee sheets are 100% original procedural art. Generator scripts and
  8× previews live in the session scratchpad (`scratchpad/ogre_prince_art/`, `scratchpad/three_mobs_art/`),
  never in the repo — re-running them reproduces the sheets bit-identically.
- **Legacy 12.0.5 source is on disk** at `C:\Users\warwa\ModDev\Mo' Creatures\_mocreatures_aside\source-12.0.5`
  (and 5.1.5 at `…\legacy-source`). Grep it before designing anything upstream may already answer.
- **Verification order:** `mc262-ref` (mapped 26.2 sources) → legacy source → the port. Never assert a
  vanilla signature from memory.
- **One owner per file** for parallel agents; shared registries (`MoCEntities`, `MoCItems`, `MoCBehavior`,
  `MoCModelLayers`, `MoCreaturesClient`, `MoCreatures`, `MoCSpawns`, `MoCConfig`, `en_us.json`) get
  integrated centrally from returned snippets.

---

## 5. Suggested order for the next session

1. Apply 3a #1-#4 (all small, all confirmed) and 3b #5.
2. Settle 3c #6 — the head-clip-in-motion question — before reporting the user's bug as fixed. Re-run the
   persisted verification workflow or redo the SAT sweep across 0…75° yourself.
3. Then 3c #7 if real.
4. `./gradlew.bat build -x test`, both loaders green.
5. Hand the user an updated test list. They test in a dev client and are good at it — they found the head
   clipping that a full review pass had missed.
6. Nothing is committed. Ask before committing; the user has not requested it.
