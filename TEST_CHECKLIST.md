# Test checklist — Round 2, 2026-08-17 (gap-closure fixes, both loaders)

> ## FINAL ROUND — last fixes, 2026-08-17 evening
>
> Everything before this block has PASSED on Fabric (fish net, hook, kitty carry incl. the forward
> nudge, F9, vials, trees, drift). This block covers the last batch, then the NeoForge pass closes
> the project.
>
> **F1. Backface culling restored (the 134-box shimmer fix — biggest visual change).** All MoC
> models now render culled like they did in 1.12, and every one-sided flat part was split into a
> proper two-faced pair. Walk around each of these and check their flat parts are visible from
> BOTH sides with NO shimmer/flicker: bee/fly/dragonfly/butterfly/firefly (wings+legs in flight
> and at rest), roach (shell + flight wings), crocodile (teeth + back scutes), elephant with tusk
> armor tiers, ostrich (demon wings on types 5/6, neck feathers, a dyed flag, helmet 7's ears),
> turkey (tail fan), snake (tongue + fangs), silver skeleton (both katana blades), small/medium
> fish fins, wyvern (wing membranes from above AND below), bat/ghost horse membranes. Also: one
> mob under an Invisibility potion still invisible; `/effect give` Glowing still outlines; riding
> a wyvern with the camera clipped inside now sees THROUGH it (vanilla-standard, not a bug).
> Fairy/pegasus horse wings were deliberately left as-is (already correct) — quick glance only.
> **FAIL:** any flat part invisible from one side (a wing that vanishes when seen from behind),
> or shimmer still present somewhere.
>
> **F2. Kitty predation now honors Enable Hunters:** flag off → a hungry wild kitty ignores
> chickens/mice; on → hunts again, no restart needed. Retaliation unaffected.
>
> **F3. Fishnet recipe-book unlock:** pick up a sharkteeth item → toast pops, fishnet recipe
> appears in the book.
>
> **F4. Stale-save migrations (ray/turtle/shark):**
> `/summon mocreatures:ray ~ ~ ~ {TypeMoC:1,Adult:1b,AgeMoC:50}` → renders FULL-size manta
> (1.5x), `/data get` shows AgeMoC 180. Same shape with TypeMoC:2 → 0.9x stingray at 90.
> `/summon mocreatures:shark {Adult:1b,AgeMoC:50}` → full 2x, age 200. A mid-growth ray
> (`Adult:0b,AgeMoC:50`) must stay small and keep growing — only the stale adult shape migrates.
>
> After F1–F4: the **NeoForge pass** (see §10 below, plus one kitty carry, one fish bite, and a
> glance at a few F1 mobs there — the model code is common but the pass has never been run).

> ## RETEST FIRST — fixes for your Fabric-pass reports (2026-08-17) — ALL PASSED ✔
>
> §1/§4/§5/§6 passed and need no repeat. Three failures were root-caused and fixed; retest these,
> then do the NeoForge pass (§10).
>
> **R1. Fishing hook (was: fish never bite).** The fish physically couldn't swim UP — their move
> control had no vertical thrust, so they stalled at the bottom below the path to the surface
> bobber. They now use a swim-aware control (vanilla fish pattern). Retest: summon a fishy in
> 2–3-deep water, cast the bobber nearby, stand still. PASS: within ~5–20 s it rises, loiters at
> the bobber, and reeling in yanks the live fish out. (The bobber still won't visually snap onto
> the fish — vanilla limitation, expected.) Side effect to spot-check: sharks now track you
> vertically while chasing (intended); dolphins still ride fine.
>
> **R2. Kitty carry + ice drift (was: loaf on lead / floating sideways / player slides).**
> Three real bugs: vanilla LEASHED the cat before our code ever saw the lead click (the "loaf"
> was a leashed cat); the shoulder translate was mirrored (a legacy 1.12-vs-26.2 transform-order
> conjugation); and the drift was the carried pet pushing YOU every tick after yesterday's tick
> reorder. All fixed. Retest: (a) carry bunny, sprint, release keys → dead stop, zero ice-slide,
> no creep while standing; (b) lead-click the tamed kitty WITHOUT sneaking → NO leash rope, lead
> not consumed, cat hangs fully upside-down at chest height; click again → lands standing;
> (c) empty-hand pickup → cat lies on its side ON your shoulder (not floating beside you); sneak
> → set down. NOTE: **sneak + lead-click now applies a vanilla leash** — that's the deliberate
> way to actually leash a kitty.
>
> **R3. F9 screen (was: buttons piled on the grid).** Rebuilt as two pages: toggles page and an
> admin page ("Admin >" / "< Toggles" button next to Done, always bottom-most). If a page is
> still too tall for the window, rows hide and the mouse wheel scrolls them in — overlap is
> impossible by construction. Retest at GUI scale 2, 3, and Auto: no overlap anywhere, page
> switch works, the numeric row still pre-fills values, spawn-rate row still validates entity
> ids, Enable Hunters toggle persists.
>
> Then run **§10 (NeoForge)** — none of it has been tested yet.

Round 1 (2026-08-16: minotaur v1, ogre princes, Ogre Lair, medusa/minotaur/chimpanzee, peaceful
sweep) **passed in full** and is not repeated here. This round covers the nine fixes just landed —
the last items open from the full gap audit. After these, every audit item is closed.

**What landed:** Fish Net (small-aquatic capture/taming), fishing-hook capture of MoC fish (mixin,
per-loader), `enableHunters` config flag, ostrich essence-vial transforms, F9 numeric/spawn-rate
GUI rows, Wyvern Lair shrub/big-tree mix, zero-lag carried pets + first-person hide, kitty
lead/shoulder carry, adult render scale uncapped (turtle 3×, shark 2×, manta ray 1.5×), and
legacy-exact raw-meat/turtle-soup nutrition values.

**Plan: full pass on Fabric first (§1–9), then the shorter NeoForge pass (§10).** Close any
running dev client before launching — it's running yesterday's build.

```bash
.\gradlew.bat :fabric:runClient
```

Setup in a fresh world: `/gamemode creative`, `/difficulty normal`, `/gamerule doMobSpawning false`.
Reminder: predation/aggro tests need `/gamemode survival`.

---

## 1. Fish Net (small-fish taming — the oldest known gap, now closed)

1. **Craft:** 5 string + 4 shark teeth (`/give @s mocreatures:sharkteeth 4`):
   ```
   . S .        S = string
   T S T        T = shark teeth
   S T S
   ```
   **PASS:** one Fish Net. Note: the recipe book will NOT auto-unlock it (no advancement yet) —
   placing the pattern manually must work.
2. **Capture wild small aquatics:** right-click with the empty net on a `fishy`, `small_fish`,
   `medium_fish`, `ray`, or `jellyfish`. **PASS:** vanish-puff, creature gone, net becomes the full
   Fish Net. In survival the empty net is replaced; in creative you keep it and gain the full one.
3. **Release (wild-caught):** right-click the full net at the water's edge. **PASS:** same
   species/sub-type reappears with star particles, **naming screen opens**, it's tamed to you, and
   you hold the empty net again.
4. **Tamed-aquatic rule:** a WILD dolphin/shark must NOT be capturable (click does nothing); a
   TAMED one must be — and on release keeps name/owner/health/age with NO re-prompt.

## 2. Fishing-hook capture (per-loader mixin — must be tested on BOTH loaders)

1. `/summon mocreatures:fishy ~ ~-1 ~4` into water, cast a fishing rod so the bobber lands near it
   (within ~18 blocks). **PASS:** within ~1–30 s the fish swims to the bobber; reeling in yanks the
   LIVE fish out toward you — no loot roll, no XP orb, rod loses 5 durability. (The bobber may keep
   bobbing in place instead of gluing to the fish — vanilla only snaps it for mid-air hooks; the
   pull on retrieve is the test.)
2. `small_fish` and `medium_fish` also bite. Dolphins, sharks, rays, jellyfish NEVER bite.
3. Tame a fishy (`/data modify entity @n[type=mocreatures:fishy] Tamed set value 1b`) — a tamed
   fish never seeks the bobber.
4. Hook a fish and DON'T reel: it wriggles free within ~10 s; a later empty retrieve is normal.
5. **Startup log check:** no errors mentioning `FishingHookMixin`.

## 3. enableHunters flag (creature-vs-creature predation switch)

1. **Plumbing:** `/moc config enableHunters` → true (default). Set false, check
   `config/mocreatures.properties` contains `enableHunters=false`, restart world, still false.
2. **Fox/chicken pen:** flag off → fox never touches the chicken (~2 min). Flip the flag on
   WITHOUT restarting → fox attacks within ~a minute (live toggle).
3. **Also gated:** raccoon hunting bursts, big cat prey, wild wolf prey, boar, bear, snake prey,
   shark's squid-hunting, dolphin-vs-shark (needs `attackDolphins` true AND `enableHunters` true).
4. **NOT gated (must still happen with the flag off):** everything that targets the PLAYER —
   wild wolf night hunts, polar bear, shark attacking you, snake retaliation when punched,
   HurtByTarget retaliation of any struck animal. The flag only governs animal-vs-animal.
5. **F9 screen:** a 20th toggle "Enable Hunters" sits after "Attack Horses" and persists.
6. Known follow-up (don't report): the KITTY's mouse/bird hunting doesn't honor the flag yet.

## 4. Ostrich essence-vial transforms (types 5–8 finally reachable)

Setup: `/summon mocreatures:ostrich ~ ~ ~ {TypeMoC:3,Tamed:1b,Adult:1b,AgeMoC:100}`.

1. Right-click with `mocreatures:essencedarkness`. **PASS:** vial consumed, glass bottle back,
   drink sound; the skin FLICKERS to the black-wyvern skin, slow then faster, transform sound ~2 s
   in, and after ~5 s it lands permanently as the black ostrich (type 6). Repeat: essencefire → 5
   (nether), essenceundead → 7 (undead), essencelight → 8 (unicorned).
2. Same-type vial: full heal, bottle back, NO morph.
3. While RIDING it: vial consumed but no morph ever starts (legacy-faithful).
4. With a helmet fitted: the helmet pops off the moment the morph starts.
5. Quit to title mid-flicker, reload: the morph resumes and completes. (Repeat once on NeoForge.)

## 5. Wyvern Lair tree variety

1. `/place feature mocreatures:wyvern_lair_shrub` → a one-log bush in lair leaves.
   `/place feature mocreatures:wyvern_lair_big_tree` → the tall branching fancy tree.
2. `/place feature mocreatures:wyvern_lair_tree` ~20 times → roughly 9 shrubs per big tree.
3. In the Wyvern Lair, NEW chunks generate mostly shrubs with scattered tall trees. Old chunks
   keep their old look — that's not a bug.
4. Startup log: no `Failed to parse` errors naming any wyvern_lair feature.

## 6. Carried pets: zero lag + first-person hide

1. Tame-carry a bunny (right-click, it sits on your head). Third person: sprint, jump, spin hard.
   **PASS:** glued to your head every frame — no one-tick trail, no rubber-banding on stops.
2. First person: NO bunny model in view at any pitch (it used to fill the screen looking up).
   Third person / other viewers: visible.

## 7. Kitty carry (legacy states 14/15)

Tame a kitty first (drop cooked cod when hungry, then medallion — or
`/summon mocreatures:kitty ~ ~ ~ {Tamed:1b,Adult:1b,AgeMoC:100}`).

1. **Lead carry:** right-click it holding a LEAD. **PASS:** it flips fully upside-down, hanging at
   hand height, frame-locked to you; the lead is NOT consumed. Right-click again → it drops,
   upright, normal calm pose.
2. **Shoulder carry:** right-click with an EMPTY hand. **PASS:** it lies rolled on its side at
   shoulder height. Sneak → set down, normal pose. Hidden in first person like the bunny.
   *If the cat sits mirrored/clipping into your head, note WHICH side — that's a one-line
   translate tune.*
3. **No stale poses:** quit mid-carry and rejoin → it re-attaches, normal pose. Force
   `KittyState:14` on a grounded cat via /data → self-heals to 0 within a tick, never renders
   flipped.
4. **No regressions:** feeding (cod), medallion naming, and bunny/bird carry all unchanged.

## 8. Adult render scale uncapped (turtle / shark / ray)

1. `/summon mocreatures:turtle` (NO nbt) → `AgeMoC` 60–109, `Adult:0b`, renders small.
2. `/data merge entity @n[type=mocreatures:turtle] {Tamed:1b,Adult:1b,AgeMoC:300}` → renders
   **3×** (shell ~2 blocks). A TAMED adult turtle slowly grows past 120 toward 300 (~+1 per 45 s);
   a wild one stays put.
3. `/summon mocreatures:ray` in an ocean → manta (TypeMoC 1), age 80–179; in a river → stingray
   (TypeMoC 2), age 50–99; both render at age/100 scale. Manta at `AgeMoC:300` caps at **1.5×**.
4. `/summon mocreatures:shark {AgeMoC:200,Adult:1b}` → renders **2×**.
5. OLD-WORLD caveat (don't report): rays/turtles saved before this fix carry the old default age
   and will render small — `/data merge` their AgeMoC or respawn them; fresh spawns are correct.

## 9. F9 GUI numeric rows + raw-meat values

1. **F9 numbers row:** cycle the name button through all 16 tunables — the value box re-fills
   with the live value each time. Set `maxMobs` to 70 → chat confirms, `/moc setnumber maxMobs`
   reads 70. Typing `abc` + Set sends nothing and reverts.
2. **Spawn-rate row:** entity `wyvern`, `frequency`, `12`, Set → chat confirms "applies on next
   world load". Entity `notamob` → red unknown-entity error.
3. **Layout:** at GUI scale 2, all rows visible and non-overlapping, on both loaders.
4. **Food values:** raw crab/ostrich/rat/turkey and turtle soup have legacy nutrition (turkey now
   3 drumsticks; soup returns the bowl as before).

## 10. NeoForge pass

```bash
.\gradlew.bat :neoforge:runClient
```

Repeat, in order of loader-sensitivity:

1. **§2 fishing hook** (the mixin is duplicated per loader — this is the highest-risk item) +
   startup log clean of `FishingHookMixin` errors.
2. **§4 test 5** — quit/reload mid-morph (exercises the loader's save codec path).
3. **§5 test 4** — datapack loads with no worldgen errors.
4. **§9 test 3** — F9 screen layout.
5. **§1 capture/release once, §6 carry once, §8 shark 2× once** — one-shot smokes.
6. General: world loads, `initialized with 60 creatures` in the log, no startup errors.

## Known gaps that REMAIN (expected, don't report)

* **134 zero-thickness boxes across 20 models** may shimmer (culling decision still pending —
  the one remaining open item, and it's a design call).
* Ent silent; chimpanzee uses pitched panda sounds.
* Fishnet has no recipe-book advancement (manual crafting only).
* Kitty predation doesn't honor `enableHunters` yet.
* Ogre multi-head ear/brow ~2px touches at extreme angles.
* Vanilla `ClientShutdownWatchdog` teardown crash on quitting the dev client.
