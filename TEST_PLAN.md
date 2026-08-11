# Mo'Creatures 26.2 — Test Plan

Covers everything changed in this pass: the 10 newly ported creatures, the follow/stay system, the
whip, pick-up & carry, the growth curves, taming, breeding, and horse genetics.

Two facts that make all of this much faster:

* **Every Mo'Creatures field is plain NBT**, so you can both *set up* and *verify* state from chat:
  `TypeMoC`, `Tamed`, `Adult`, `AgeMoC`, `OwnerName`, `Saddled`, `Sitting`, `HasEaten`, `Temper`
  (horses add `HorseArmor`, `EatenPumpkin`, `ChestedHorse`).
* **`/summon` with NBT skips `finalizeSpawn`**, so a coat/age you specify is kept exactly.
  `/summon` *without* NBT runs the normal spawn roll. Use the NBT form whenever you want determinism.

Throughout, replace `Dev` with your actual dev username (`/data get entity @s` → look for the name, or
just read the nameplate).

---

## 0. Launch

```bash
./gradlew.bat :fabric:runClient
```

NeoForge, when you want to confirm both loaders:

```bash
./gradlew.bat :neoforge:runClient
```

Then in-game: create a world → **Creative**, **Superflat**, **Peaceful** to start.

Baseline setup, paste into chat:

```
/gamemode creative
/time set day
/gamerule doMobSpawning false
/gamerule mobGriefing true
```

**Expect in the log:** `Mo'Creatures (Architectury multi-loader, MC 26.2) initialized with 56 creatures`.

---

## 1. Smoke test — do all 10 new creatures render?

Line them all up in one go and walk down the row:

```
/summon mocreatures:ant ~2 ~ ~
/summon mocreatures:raccoon ~4 ~ ~
/summon mocreatures:mole ~6 ~ ~
/summon mocreatures:ent ~9 ~ ~
/summon mocreatures:silver_skeleton ~12 ~ ~
/summon mocreatures:mini_golem ~14 ~ ~
/summon mocreatures:manticore ~17 ~ ~
/summon mocreatures:manticore_pet ~20 ~ ~
```

Aquatics need water — dig a pool or:

```
/fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 water
/summon mocreatures:small_fish ~ ~ ~
/summon mocreatures:medium_fish ~1 ~ ~
```

**What to look for on each, in priority order:**

1. **Not invisible / not a black-and-magenta box** → texture path resolves.
2. **Not inside-out or scattered** → the mesh converted correctly. Inverted or exploded geometry is the
   most likely failure of a hand-converted model.
3. **Right size** relative to you (Ent ≈ 7 blocks tall, ant tiny, manticore lion-sized).
4. **Walks without limbs detaching** → `setupAnim` pivots are right.
5. **Shadow roughly matches the footprint.**

Variants — these use the same model with a different sheet, so check each renders:

```
/summon mocreatures:manticore ~2 ~ ~ {TypeMoC:1}
/summon mocreatures:manticore ~4 ~ ~ {TypeMoC:2}
/summon mocreatures:manticore ~6 ~ ~ {TypeMoC:3}
/summon mocreatures:manticore ~8 ~ ~ {TypeMoC:4}
/summon mocreatures:ent ~11 ~ ~ {TypeMoC:1}
/summon mocreatures:ent ~14 ~ ~ {TypeMoC:2}
```

Small fish has 8 sub-types (1 anchovy → 8 piranha), medium fish 3 (1 salmon, 2 cod, 3 bass):

```
/summon mocreatures:small_fish ~ ~ ~ {TypeMoC:1}
```
…through `{TypeMoC:8}`, and `{TypeMoC:1..3}` for `medium_fish`.

Creative-tab check: search `mocreatures` in the creative inventory — you should find 10 new spawn eggs
(Ant, Raccoon, Mole, Ent, Small Fish, Medium Fish, Silver Skeleton, Mini Golem, Manticore, Pet
Manticore) with proper names, not `item.mocreatures.…` placeholder strings.

**Known-good expectation:** all 9 new model classes passed a static part-name/UV consistency check, so
a *crash* on render is unlikely; what needs your eyes is whether the geometry looks right.

---

## 2. Per-creature behaviour (the new ten)

### Ant
```
/summon mocreatures:ant ~ ~ ~
/give @s wheat 10
```
Drop wheat on the ground near it (press `Q`). **Expect:** the ant walks to the item, then carries it —
the item entity literally rides on the ant's back and travels with it. It also crawls up walls
(bump it into a block face).

### Raccoon
```
/summon mocreatures:raccoon ~ ~ ~
```
**Expect:** ambient chittering (it uses the `raccoon*.ogg` files that already shipped), and it hunts
small creatures. Tame it by feeding *any* edible — bread, an apple, wheat, cooked meat all work.
1 spawn in 3 is a kit; check with `/data get entity @e[type=mocreatures:raccoon,limit=1] Adult`.

### Mole
```
/summon mocreatures:mole ~ ~ ~
/data get entity @e[type=mocreatures:mole,limit=1] 
```
**Expect:** it burrows — the model sinks into the ground and resurfaces. Drop items nearby; it steals
them. `moleState` drives the render sink (0 outside → 2 underground).

### Ent
```
/summon mocreatures:ent ~5 ~ ~
```
**Expect:** ~7 blocks tall with a leaf crown, wanders slowly, **ignores you**. Now hit it with an axe:

```
/give @s minecraft:iron_axe
```
**Expect:** *only then* does it turn hostile. Hitting it with a sword or fist should not provoke the
same reaction (legacy singled out axes). It plants saplings as it walks. Kill it → 4–15 logs/sticks/
saplings of its wood type (oak for type 1, birch for type 2).
**Known gap:** the Ent is silent. 12.0.5 never shipped its sound files (`// TODO` in its own source).

### Silver Skeleton
```
/difficulty normal
/summon mocreatures:silver_skeleton ~3 ~ ~
```
**Expect:** dual katanas, alternating left/right arm swings when it attacks. Kill it → it should drop
its silver sword (`mocreatures:silversword`), which until now had no source in the game.

Undead cavalry (newly wired):
```
/summon mocreatures:horse_mob ~3 ~ ~ {TypeMoC:23}
/summon mocreatures:silver_skeleton ~4 ~ ~
```
**Expect:** within a few seconds the skeleton climbs onto the undead horse.

### Mini Golem
```
/summon mocreatures:mini_golem ~3 ~ ~
```
**Expect:** it rips up a block, hoists it overhead (you should see a real full-size block above it),
and throws it. When it has a target its head/body switch to the red-hot skin.

### Manticore + Pet Manticore
```
/summon mocreatures:manticore ~4 ~ ~
```
**Expect:** flies, wings beat, scorpion tail strikes when it attacks, poison on hit.

The pet form's *legacy* source is the manticore egg, newly wired here (ids 62–65):
```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:62}]
```
Throw it, wait for it to hatch. **Expect:** a manticore *cub* (small), tamed to you, that grows.
Verify: `/data get entity @e[type=mocreatures:manticore_pet,limit=1] AgeMoC` → starts at 30.
Once grown, tame with a medallion (see §6) and saddle to ride it.

### Small / Medium Fish
```
/fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 water
/summon mocreatures:small_fish ~ ~ ~ {TypeMoC:8}
```
**Expect:** type 8 is the piranha and is aggressive; 1–7 school peacefully. Medium fish should swim
without the "vibrating in place" look that a broken swim animation gives.

---

## 3. Follow / stay — the whip (this was the headline complaint)

```
/give @s mocreatures:whip
/summon mocreatures:bunny ~2 ~ ~
/summon mocreatures:goat ~4 ~ ~
```

Tame both first — right-click the bunny with an **empty hand** (that both tames and picks it up; put it
down again), and feed the goat any edible.

**Test 3a — single-creature order.** Right-click the goat **with the whip**.
* **Expect:** action-bar message `Goat stays put`, happy-villager particles, and the goat stops
  following you. Walk 30+ blocks away — it must **not** teleport after you.
* Right-click again → `Goat follows you`, cloud particles, it resumes following.
* Verify from chat: `/data get entity @e[type=mocreatures:goat,limit=1] Sitting` → `1b` / `0b`.

**Test 3b — area crack.** Right-click a **block** (the top or side of one, with air above it) with the
whip. **Expect:** a smoke+flame puff at the block, a deep quiet crack sound, and every tamed pet of
yours within 12 blocks toggles at once. Durability drops by 1 per crack (whip has 24 uses).

**Test 3c — the gating that used to be broken.**
* Right-click the *underside* of a block, or a block with something directly on top of it →
  **expect nothing at all**: no sound, no particles, **no durability loss**. (It used to fire anywhere.)
* Right-click a creature directly → **expect no durability loss** and no crack sound; the order is free.

**Test 3d — species flourishes.**
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/summon mocreatures:ostrich ~5 ~ ~ {TypeMoC:2,Tamed:1b,Adult:1b,OwnerName:"Dev"}
```
* Whip the unridden horse → it stays **and drops its head to graze**.
* Whip the unridden ostrich → it stays **and buries its head in the sand**.
* Now **ride** the horse and whip → it should **surge into a sprint** (speed multiplier, not a teleport
  lurch — the old non-legacy shove was removed).
* Ride a **nightmare** (`{TypeMoC:38,Tamed:1b,Adult:1b,OwnerName:"Dev"}`) and whip → it should lay a
  **trail of fire** as it gallops and keep *you* from burning. This is new; it used to be a single
  particle puff.

**Test 3e — ownership.** With `enableOwnership` on (default), another player must not be able to feed,
saddle, mount or otherwise interact with your tamed pet. Single-player proxy: set a different owner and
try to interact —
```
/data modify entity @e[type=mocreatures:goat,limit=1] OwnerName set value "SomeoneElse"
```
**Expect:** right-clicking it now does nothing at all.

---

## 4. Pick up & carry

Carrying does **not** use the vanilla passenger system. It cannot: `Entity.startRiding` bails at
`!entityToRide.type.canSerialize()` and `EntityType.PLAYER` is `.noSave()`, so a mob can never be a
player's passenger on a 26.2 server — and `force = true` does not bypass it, the check sits above the
force branch. Legacy's `mountEntity(entityplayer)` therefore has no equivalent, and every carry path in
this port was silently dead. It is now a custom system: a synched carrier id, with the creature pinned
to the carrier's head each tick as an ordinary world entity.

```
/summon mocreatures:bunny ~2 ~ ~
```

* Right-click with an **empty hand** → the bunny is tamed and **sits on your head**. Press F5 to see it;
  in first person it is deliberately **hidden** so it doesn't fill your view.
* It should **face the way you face**, stop trying to walk, and not shove you around.
* **Sneak (Shift) to set it down** — this is the reliable release, and the only one available in first
  person, where you cannot click a creature sitting on your own head. Right-clicking it in third person
  works too.
* **Sprint or jump as you drop it** → it should be **thrown** (5× your momentum), with `rabbitlift`.
* When it lands you should hear `rabbitland`, and **every hostile within 12 blocks turns on the bunny** —
  the legacy decoy trick. Test with `/difficulty normal` and a couple of zombies nearby.

**Persistence:** pick a bunny up, quit to title, come back. It should still exist. (Under the new system
it is never a passenger, so it saves through the ordinary entity path.)

**Carrier leaves:** pick one up and `/kill @s`. The bunny should drop where you were, not hang in the air.

**Second-pet guard:** while carrying one bunny, right-click a second one. **Expect:** nothing happens —
and crucially the second bunny is **not** silently tamed.

Also carriable, all through the same toggle: mouse, tamed bird, tamed snake, baby pet scorpion, turtle,
**kitty bed and litter box** (the furniture was broken for exactly the same reason).

---

## 5. Growth curves — 14 species that never aged

This is the biggest functional fix and the easiest to verify from chat.

**Elephant (was permanently untameable):**
```
/gamerule randomTickSpeed 3
/summon mocreatures:elephant ~4 ~ ~
/data get entity @e[type=mocreatures:elephant,limit=1] Adult
```
Summon several — **expect roughly 1 in 4 to be a calf** (`Adult: 0b`), rendering visibly smaller.
Now feed the calf **cake** or a **sugar lump** repeatedly:
```
/give @s minecraft:cake 10
/give @s mocreatures:sugarlump 10
```
**Expect:** it tames once temper reaches 10 (cake +2, sugar lump +1). Check with
`/data get entity @e[type=mocreatures:elephant,limit=1] Temper`.
Before this fix **every elephant spawned adult and no elephant in the world could ever be tamed.**

**Goat (kids never became milkable):**
```
/summon mocreatures:goat ~3 ~ ~ {TypeMoC:1,Adult:0b,AgeMoC:95}
```
Watch `AgeMoC` climb: `/data get entity @e[type=mocreatures:goat,limit=1] AgeMoC`.
At 100 → `Adult:1b`, and `TypeMoC` re-rolls to 2–7. If it lands on 2–4 (female) you can now milk it
with an empty bucket. Kids and males give nothing (males headbutt you for trying).

**Ostrich (stolen-egg route to a rideable bird was dead):**
```
/summon mocreatures:ostrich ~3 ~ ~ {TypeMoC:1,Adult:0b,AgeMoC:35}
```
**Expect:** stays a chick, `AgeMoC` climbs, at 100 it becomes an adult and re-rolls to type 2/3/4 — and
*only then* can be saddled. Previously a natural chick matured on its first tick and an egg-hatched one
stayed a chick forever.

**Pet scorpion (carried as a baby, ridden as an adult):**
```
/summon mocreatures:pet_scorpion ~3 ~ ~
/data get entity @e[type=mocreatures:pet_scorpion,limit=1] Adult
```
**Expect:** always born a baby (`Adult:0b`, `AgeMoC:20`), grows to 120, and only then is rideable.

Quick sweep of the rest — summon one of each and confirm `AgeMoC` moves over a few minutes:
`kitty`, `bear`, `crocodile`, `jellyfish`, `ray`, `snake`, `duck`, `fishy`, `scorpion`, `wyvern`.
Wild wyverns should now spawn as **juveniles** (`Adult:0b`, age 50–99) and refuse a saddle until grown.

Crabs should now vary in size (age 50–99 → 0.35×–0.69×) instead of all being identical.

---

## 6. Taming

| Creature | How | Command to set up |
|---|---|---|
| Goat / raccoon | any edible | `/give @s bread` |
| Horse (ordinary) | apple | `/give @s apple` |
| **Horse (magic/undead)** | **must NOT tame by apple** | `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:36}` |
| Zebra | 5 apples (temper 200) | `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:60}` |
| Big cat | medallion, **cub that has eaten** | `/give @s mocreatures:medallion` |
| **Kitty** | medallion, **after it eats a cooked fish** | see below |
| Elephant | cake/sugar lump to a calf | §5 |
| Bird | scatter seeds, then hand-feed | `/give @s wheat_seeds 20` |

**Test 6a — magic horses can no longer be apple-tamed.**
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:36}
/give @s minecraft:apple
```
**Expect:** feeding the unicorn does **nothing** — no taming, no love-mode hearts. The only route is to
saddle it and break it in. Same for pegasus (39/40), bat horse (32), ghost (21/22), undead (23–28) and
fairies (48–59). One apple used to tame any of them instantly.

**Test 6b — kitty two-stage taming.**
```
/summon mocreatures:kitty ~3 ~ ~
/give @s minecraft:cooked_cod 5
```
Right-click it with a medallion straight away → **expect nothing**. Now **drop** a cooked cod on the
ground and wait for the kitty to get hungry and eat it (`/data get entity @e[type=mocreatures:kitty,limit=1] HasEaten`
→ `1b`). *Then* the medallion works. Big cats already worked this way; kitties skipped it.

**Test 6c — the naming prompt (new).** Any successful tame — feed, medallion, pick-up, break-in, egg
hatch, fish-bowl release — should now **pop up the naming screen immediately**. Legacy did this on every
tame; the port previously only offered naming via a separate medallion right-click.

**Test 6d — pet cap.** Set a low cap in `fabric/run/config/mocreatures.properties`
(`maxTamedPerPlayer=2`), `/reload`, then try to tame a third pet by each route: feeding, medallion,
picking up, panda sugar cane, bird seeds, elephant temper, turtle ground-eat, **egg hatch**, and
**fish-bowl release**. **Expect:** all nine refuse with *"You have too many pets."* — the last six used
to bypass the cap entirely.

---

## 7. Breeding

**Fishy (mechanic was entirely missing):**
```
/fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 water
/summon mocreatures:fishy ~ ~ ~ {TypeMoC:3,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/summon mocreatures:fishy ~1 ~ ~ {TypeMoC:3,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/give @s minecraft:cod 5
```
Feed both raw cod (sets `HasEaten`). **Expect:** after a gestation period a litter of **1–3 fry** of the
**same colour**, auto-tamed to you, at `AgeMoC:20`. Note: this never actually worked in 5.1.5 either —
the flag it gates on was never set — so the feed step is the port supplying the missing half.

**Kitty generations:** breed two adult kitties, then wait for the kitten to grow (§5) and breed *it*.
**Expect:** it works. Previously the line stopped after one generation because kittens never matured.

**Bred babies inherit properly:**
```
/data get entity @e[type=mocreatures:kitty,sort=nearest,limit=1] AgeMoC
```
**Expect:** a newborn kitty starts at `AgeMoC:40` (its species newborn age) and inherits the parent's
`TypeMoC` — not `AgeMoC:50` and `TypeMoC:0` (which rendered the fallback texture).

---

## 8. Horse genetics & fairy horses

The full 53-entry Mendelian table was already correct; these confirm it end-to-end.

Breeding needs both parents tamed, adult, and **pumpkin-fed** (`EatenPumpkin`). Fast setup:

```
/summon mocreatures:horse ~2 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
/summon mocreatures:horse ~4 ~ ~ {TypeMoC:2,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
```
**Expect:** white (1) × creamy (2) → **bright creamy (6)**, per the table. A few more pairs to spot-check:
`1×3 → 2`, `1×4 → 7`, `2×4 → 3`, `4×17 → 5`, `13×17 → 9`.

**Hybrids:**
```
/summon mocreatures:horse ~2 ~ ~ {TypeMoC:60,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
/summon mocreatures:horse ~4 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
```
zebra × ordinary → **zorse (61)**; donkey (65) × ordinary → **mule (66)**; zebra × donkey → **zonky (67)**.

**White fairy — the signature one:**
```
/summon mocreatures:horse ~2 ~ ~ {TypeMoC:36,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
/summon mocreatures:horse ~4 ~ ~ {TypeMoC:39,Tamed:1b,Adult:1b,OwnerName:"Dev",EatenPumpkin:1b}
```
Unicorn (36) × white pegasus (39) → **white fairy (50)**, and **both parents vanish**.

**Fairy colours:** dye a white fairy with any of 11 dyes → coats 48–59.
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:50,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/give @s minecraft:red_dye
```
**Expect:** a ~5-second morph animation, then the red fairy (55).

**Wild zebras and donkeys:** turn mob spawning back on in a plains biome and confirm both appear
(~9% donkey, ~5% zebra with the default `zebraChance`).

---

## 9. Regression sweep

Things most likely to have been disturbed by this pass:

* **Sitting pets and mounts don't fight.** Tell a pet to stay, then mount it (if rideable) or pick it up.
  Expect no stuck-in-place behaviour after dismount.
* **Existing worlds load.** Open a world saved before these changes — no crash, pets keep their owner.
* **Spawn distribution.** `/gamerule doMobSpawning true`, fly around a fresh chunk area, confirm the new
  creatures appear at sane rates and haven't crowded out the existing roster.
* **Both loaders.** Run §1 and §3 on `:neoforge:runClient` too.

---

## 10. Re-test of the six reported bugs

**Manticore egg naming**
```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:62}]
```
**Expect:** "Fire Manticore Egg", not "Spoiled Egg". Ids 63/64/65 are Dark / Snow / Manticore, and the
four now carry distinct tints in the creative tab instead of four identical white eggs. Throw one, wait
by a torch → a manticore cub of the matching coat (62→red, 63→dark, 64→blue, 65→green).

**Silver sword** — the rate is legacy-exact but genuinely rare: a 1-in-10 item pick multiplied by a
0–2 copy count that is zero a third of the time, i.e. **~6.7% per kill**. Looting now applies again
(it had been dropped), taking it to ~8.3/8.9/9.2% at I/II/III. To confirm the path works rather than
grinding kills:
```
/give @s minecraft:diamond_sword[minecraft:enchantments={"minecraft:looting":3}]
```
then kill ~20 silver skeletons. If you want certainty in one kill, temporarily read the code path —
it is `MoCEntitySilverSkeleton.dropCustomDeathLoot`.

**Bunny carry** — see §4 above; the whole mechanism was replaced.

**Pegasus / fairy wings**
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:39}
/summon mocreatures:horse ~6 ~ ~ {TypeMoC:50}
/summon mocreatures:horse ~9 ~ ~ {TypeMoC:1}
```
**Expect:** the pegasus has feathered wings only, folded and drooping along the flank when grounded;
the fairy has membrane wings only; and the **plain horse has no wing geometry at all**. That last one is
the real tell — the flat "2D wing image offset from the wings" was two butterfly-wing planes that were
being drawn on *every* horse in the game, because `ModelPart.visible` defaults to true and nothing ever
cleared it. Ride the pegasus up: the outer half of the wing must stay attached to the inner half through
the whole stroke, and the tips must rise **above** the back, not swing down through the ribs.

**Fairy dye morph**
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:50,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/give @s minecraft:red_dye
```
**Expect:** ~5 seconds of the coat **strobing** between white and red, accelerating, then settling on
red. The morph was always running server-side; the client just had no data to draw it, so it snapped.

**Horse breeding**
```
/summon mocreatures:horse ~2 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/summon mocreatures:horse ~4 ~ ~ {TypeMoC:2,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/give @s minecraft:pumpkin 4
```
Feed both a pumpkin. **Expect:** hearts, they walk to each other, and a foal appears — coat **6**
(bright creamy) from the 1×2 pairing. An apple on a tamed adult now also works as an aphrodisiac at any
health. Note **`EatenPumpkin:1b` in the summon NBT is no longer enough on its own** — that only sets the
legacy readiness flag; the pumpkin *feed* is what puts them in love.

---

## 11. Round-3 fixes

**Every newborn was born full-grown.** `AgeableMob.setAge` (mc262-ref AgeableMob.java:147-153) fires
`ageBoundaryReached()` on **both** boundary crossings — growing up *and* becoming a baby. The safety-net
override reacted to both, so the `setAge(-24000)` that makes a newborn a baby immediately flipped the MoC
`ADULT` flag back to true. That hit **six** sites, not just the horse: bred babies of every species
(`MoCAnimal`), horse foals, **every egg hatchling** (ostrich chick, komodo, wyvern, snake, scorpion,
fishy, shark, manticore cub), brooded baby scorpions, and hatched ducklings.

```
/summon mocreatures:horse ~2 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/summon mocreatures:horse ~4 ~ ~ {TypeMoC:2,Tamed:1b,Adult:1b,OwnerName:"Dev"}
/give @s minecraft:pumpkin 4
```
Feed both. **Expect a visibly small foal**, and:
```
/data get entity @e[type=mocreatures:horse,sort=nearest,limit=1] Adult
```
→ `0b`, with `AgeMoC: 1`. Re-check the §5 growth tests too — they were all reading `Adult:1b` for the
same reason. In particular an egg-hatched ostrich chick should now actually *be* a chick.

**Carried pet survives quitting to the title screen.** The carrier is now recorded as a persisted UUID
claim rather than only a live entity id, so the pet re-attaches when you come back.
1. Pick up a bunny. 2. Quit to title. 3. Re-enter.
* **Expect:** it is back on your head, still carried — not on the floor.
* While you are away it simply rests on the ground; the claim is kept, not the pose.
* Sneak still sets it down for good (that clears the claim).

**Fairy wings** — see the note below on what changed.

---

## Outstanding decision: zero-thickness boxes

A zero-thickness box emits BOTH of its opposing quads at identical depth (`ModelPart.java:299-304`) and
they sample *different* UV tiles, and 26.2 renders entities with culling off
(`RenderPipelines.ENTITY_CUTOUT` is `.withCull(false)`, line 254). 1.12 culled back faces, so these
models were authored assuming exactly one twin would draw. There are **134 such boxes across 20 models**
— insect wings, crocodile scutes, fish fins, elephant ear panels, turkey tail, the pegasus feathered
wings. Only the fairy wings have been fixed (split into single-face boxes).

Two options if shimmer shows up elsewhere:
* **Global** — give the Mo'Creatures renderer a render type with culling on, matching what the models
  were authored for. One change, fixes all 134, but affects how all ~50 models render and single-sided
  geometry currently visible from both sides would stop being so.
* **Targeted** — split boxes into single faces per model, as the fairy wings were. No blast radius,
  but 134 boxes if done exhaustively.

---

## What is *not* covered (known gaps)

* **Ent sounds** — no audio files exist in any DrZhark release.
* **Small fish taming** — legacy's only route was the fishnet, which this port doesn't implement.
* **Fishing-hook capture** and the `enableHunters` config flag — no port equivalent.
* **Uncapped adult render scale** — legacy grew tamed turtles to 3×, sharks to 2×, rays to 1.8×. The
  growth data is now correct but the renderer still caps adults at 1×.
* **Kitty carry** (legacy kitty states 14/15) — not wired.
* Medusa, Minotaur, Chimpanzee, Ogre Prince — never existed in any release.
