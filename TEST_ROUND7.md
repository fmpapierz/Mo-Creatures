# Test — Round 7 (2026-09-26): wyvern flight speed and hidden painted details

> **Replaced by TEST_ROUND7_8_COMPLETE.md**, which covers this round and the next one with step-by-step checks. Use that file.


Each section ends with **PASS if / FAIL if**. If something fails, tell me the section, the step and what you saw.

| # | What | Cause | Fix |
|---|---|---|---|
| 1 | Wyvern flies way too fast | The port used 5.1.5's ridden speed (2.0, or 3.0 for the big wyverns) instead of 12.0.5's 1.0. It also hit full speed in 0.15 s, stopped dead, and "walked" on the ground at full flight speed | 12.0.5's own flight physics, ported: the same speed factor for every wyvern, speed that builds up over 1.5–2 s, drifting turns, and a glide when you let go. On the ground it walks at 12.0.5's walking speed |
| 2 | Scan all models for hidden painted details | 1.12 drew both sides of every face on a mob; the port hides back faces. Details painted on one side of a box (whiskers, antlers, medallions, wing undersides, the inside of mouths) vanished when seen from behind | 25 models got the missing back sides (about 560 faces), and 99 flat parts had a back side that was laid out back-to-front, now corrected |

---

## 0. Setup

1. The test client is started for you. If it isn't open, run this in PowerShell:
   ```
   cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
   .\gradlew.bat :fabric:runClient
   ```
2. Load your test world (Creative, commands on), stand on a big flat open area, then:
   ```
   /time set day
   /gamerule advance_time false
   /difficulty normal
   ```
3. Press `F3` so you can see your coordinates and the "Facing:" line.

---

## 1. Wyvern flight speed (matches 12.0.5)

**What 12.0.5 actually does** (read from its code and 1.12.2's own riding code):
- **Speed factor:** every ridden wyvern gets the same factor, 1.0. The bigger wyverns (mother, undead, light, dark)
  are faster only because they keep their momentum better: friction 0.96 against 0.94.
- **Momentum:** each tick it pushes the wyvern a little in the direction you face and bleeds off a little speed. So
  speed builds up, turns drift wide, and letting go of `W` glides on.
- **Top speed:** about **21.5 blocks/s** for the ordinary wyverns and **30 blocks/s** for the big ones. That is
  genuinely fast, about 1.5–2× a fast horse. It takes about 1.5 s (big ones about 2 s) to get there.

**Before this fix:** 23 and 35 blocks/s, reached in 0.15 s, with a dead stop when you let go. It also ran at that full
flight speed on the ground.

**Now:** 21.5 and 30 blocks/s, built up gradually, with 12.0.5's glide and drift. On the ground it walks at
12.0.5's 13 blocks/s. Sprinting still adds 30% (28 and 39 blocks/s); 12.0.5 had no sprint at all, so fly without
sprint to compare with the original.

**Controls:** these are unchanged. Look where you want to go and hold `W`. Look up to climb, look down to dive. Let
go and it glides down slowly.

### 1a. Get an ordinary wyvern and a mother
```
/summon mocreatures:wyvern ~4 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}
/summon mocreatures:wyvern ~-4 ~ ~ {TypeMoC:5,Tamed:1b,Adult:1b,AgeMoC:180,Saddled:1b}
```
Right-click the first one (the smaller of the two) with an **empty hand** to get on. Don't press sprint (`Ctrl`) or
double-tap `W` during 1b–1f.

### 1b. On the ground: walking pace
Look straight ahead (level) and hold `W` for a few seconds.
**Look for:** it walks about as fast as a quick horse. Before, it shot across the ground at full flight speed.

### 1c. Take off and speed up
1. Look up a little more than 25° and hold `W`. It climbs, with its wings beating.
2. Get about 20 blocks up, let go of `W`, and wait until it has nearly stopped (it sinks slowly).
3. Look level and hold `W`.

**Look for:** the speed **builds up** over about a second and a half and then holds steady. It does not jump
straight to full speed.

### 1d. Distance check (optional, uses F3)
1. While it hovers nearly still, turn so the F3 "Facing:" line says **north** or **south** (the Z coordinate will
   change), and look level.
2. Note your Z. Hold `W` while you count five seconds out loud, let go, and note your Z again.

**Look for:** about **95 blocks** covered (the mother: about **125**). Before this fix it was about 115 (mother 175).
Hand-counting is rough, so anything within ±20 is fine.

### 1e. Let go at full speed: it glides
At full speed, let go of `W`.
**Look for:** it **coasts on** for about 13 blocks while sinking slowly, then hangs and sinks. Before, it stopped
almost dead. The mother coasts about 25 blocks.

### 1f. Turn at full speed: it drifts
At full speed, turn sharply 90° with the mouse while holding `W`.
**Look for:** it **swings wide** and takes about a second to settle on the new heading, like a heavy flyer. Before,
it snapped straight onto the new line.

### 1g. The mother is the fast one
Land, press `Shift` to get off, and get on the mother (the second one you summoned). Repeat 1c–1f.
**Look for:** she builds up more slowly (about 2 s) but tops out clearly faster (about 30 blocks/s), and she coasts
further when you let go.

### 1h. Sprint (a port extra)
Hold `Ctrl` (or double-tap `W`) while flying.
**Look for:** about 30% faster. 12.0.5 couldn't sprint on anything; this is the same mount sprint the horse got in
Round 4.

**PASS if** the wyvern builds up speed instead of snapping to it, glides on and drifts through turns, and the mother
is the faster of the two. **FAIL if** it still reaches full speed instantly, stops dead when you let go, or clearly
beats the numbers above. If it matches all this but still feels too fast at full speed, tell me: that is 12.0.5's
own top speed, and it is one number to turn down.

---

## 2. Hidden painted details (model scan)

**Background:** 1.12 drew both sides of every face on a mob, and the original art relies on it. Many boxes are
painted on one face and left see-through on the others, so you see the painted face from behind through the empty
ones. The port hides back faces, so those details vanished from those angles. Every model was scanned against the
textures in 88 poses. The missing back sides are now added as separate faces that show exactly the same pixels. No
shapes or animations changed.

**How to check:** summon each creature with `NoAI:1b` so it stands still. With `NoAI`, a mob summoned in the air
**stays in the air**, which is how you look at it from below. Fly around it in Creative.
- "From behind" means from behind that face, looking through the creature's see-through side at the painted face.
- "The same from both sides" means the back of a flat part shows the mirror image of its front, not a blank or
  scrambled pattern.

**PASS for each line** if the detail is there and painted from the angle given, looking like the side you normally
see. **FAIL** if it vanishes, or you see straight through to the background. Tell me the creature and the angle.

### 2a. Wyvern
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:wyvern ~5 ~3 ~ {TypeMoC:1,Adult:1b,AgeMoC:100,NoAI:1b}` | The head from behind and to its left | The inside of the beak, painted |
| (same) | Wings and ear frills from below | The undersides show the same pattern as the tops |
| (same) | Eyes from the side | Eyes present (the fix from Round 5; a control check) |
| `/summon mocreatures:wyvern ~5 ~3 ~5 {TypeMoC:2,Adult:1b,AgeMoC:100,NoAI:1b}` | Where the tail joins the body, from behind and above | Painted, no hole |
| `/summon mocreatures:wyvern ~5 ~3 ~-5 {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b,ArmorType:1,NoAI:1b}` | The helmet from behind-left, and the bit (mouthpiece) from below | The helmet's inside walls and the bit painted. Repeat with `ArmorType:2` and `ArmorType:3` |

### 2b. Big creatures (summoned 3 blocks up so you can look from below)
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:elephant ~7 ~3 ~ {TypeMoC:5,ArmorStage:3,NoAI:1b}` | The cabin on its back from in front and above; the cabin roof from below; the side drape from below and to its right | The cabin's inside walls, the roof's underside and the drape's underside, all painted |
| `/summon mocreatures:elephant ~7 ~3 ~7 {TypeMoC:2,NoAI:1b}` | The ears from behind; the tail tuft from below | Painted |
| `/summon mocreatures:elephant ~7 ~3 ~-7 {TypeMoC:1,Tusks:3,NoAI:1b}` | The flat fins on the tusks, from both sides | The same from both sides |
| `/summon mocreatures:ent ~7 ~3 ~ {TypeMoC:1,NoAI:1b}` | The fingers and the base of the tree from behind; the feet from below | Painted. **Known leftover:** a faint speckle deep inside the leafy crown, visible only through gaps between the leaves. 1.12 flickered there too; fixing it would need about a thousand extra boxes |
| `/summon mocreatures:big_cat ~5 ~3 ~ {TypeMoC:5,NoAI:1b}` | The collar medallion from the front; the claws from below | Painted |
| `/summon mocreatures:big_cat ~5 ~ ~5 {TypeMoC:5,Tamed:1b,Saddled:1b,NoAI:1b}` | The stirrup rings, from all sides | Painted |
| `/summon mocreatures:manticore ~5 ~3 ~-5 {TypeMoC:2,NoAI:1b}` | The wings and the claws from below | The wing undersides show the membranes; the claws are painted |
| `/summon mocreatures:manticore_pet ~-5 ~ ~ {TypeMoC:1,Tamed:1b,Saddled:1b,NoAI:1b}` | The stirrup rings | Painted |

### 2c. Ostrich, werewolf, wolf
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:ostrich ~4 ~ ~ {TypeMoC:7,NoAI:1b}` | The folded wing feathers from behind | Painted |
| `/summon mocreatures:ostrich ~4 ~ ~4 {TypeMoC:3,Tamed:1b,Saddled:1b,Helmet:2,Chested:1b,FlagColor:1,NoAI:1b}` | The helmet from front-left; the flag from both sides; the saddle rings | The helmet painted inside; the flag the same from both sides; the rings painted |
| Same, with `Helmet:7` | The reptile helmet's ears from both sides | The same from both sides |
| `/summon mocreatures:ostrich ~4 ~ ~-4 {TypeMoC:5,NoAI:1b}` | The wings and neck feathers from both sides | The same from both sides |
| `/summon mocreatures:werewolf ~-4 ~ ~ {TypeMoC:2,HumanForm:0b,NoAI:1b}` | The upper teeth, from in front and below; the tail, thighs and sideburns from behind | Painted |
| `/summon mocreatures:wild_wolf ~-4 ~ ~4 {TypeMoC:2,NoAI:1b}` | The tail and flanks, from behind and above | Painted |

### 2d. Small animals (summoned 2 blocks up; crouch-fly under them for "from below")
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:deer ~3 ~ ~ {TypeMoC:1,Adult:1b,NoAI:1b}` | The antlers from its left side | The same as from its right |
| `/summon mocreatures:duck ~3 ~2 ~2 {NoAI:1b}` | The head from below; the legs from above | Painted |
| `/summon mocreatures:bird ~3 ~2 ~4 {TypeMoC:3,NoAI:1b}` | The legs from in front and above | Painted |
| `/summon mocreatures:boar ~-3 ~ ~ {Adult:1b,NoAI:1b}` | The ears from the front | Painted |
| `/summon mocreatures:bunny ~-3 ~ ~2 {TypeMoC:1,NoAI:1b}` | The cheek whiskers from behind | Painted |
| `/summon mocreatures:rat ~-3 ~ ~4 {TypeMoC:1,NoAI:1b}`, `/summon mocreatures:hell_rat ~-3 ~ ~6 {NoAI:1b}`, `/summon mocreatures:mouse ~-3 ~ ~8 {TypeMoC:1,NoAI:1b}` | The whiskers from behind | Painted |
| `/summon mocreatures:fox ~3 ~2 ~6 {TypeMoC:1,NoAI:1b}` | The head from its right and below | Painted |
| `/summon mocreatures:dragonfly ~3 ~1 ~8 {TypeMoC:1,NoAI:1b}` | The tail (abdomen) from behind; the legs from both sides | Painted; the legs the same from both sides |
| `/summon mocreatures:kitty ~-3 ~ ~-3 {TypeMoC:1,Tamed:1b,NoAI:1b}` | The cheeks (whiskers) and the collar, from behind and to its left | Painted |

### 2e. Fish (make a pool first)
```
/fill ~3 ~-4 ~-3 ~9 ~-1 ~3 minecraft:water
```
Without moving from that spot (the positions are relative to you), summon them all into the pool, then swim down
and look:
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:fishy ~6 ~-2 ~ {TypeMoC:10,NoAI:1b}` | The tail from behind-left | Painted |
| `/summon mocreatures:small_fish ~5 ~-2 ~2 {TypeMoC:3,NoAI:1b}`, then `{TypeMoC:4,...}` | The tail and middle fin from both sides | Painted |
| `/summon mocreatures:medium_fish ~7 ~-2 ~-2 {TypeMoC:1,NoAI:1b}` | The fins from both sides | The same from both sides |
| `/summon mocreatures:shark ~6 ~-2 ~ {NoAI:1b}` | The right fin from behind and to its right | Painted |
| `/summon mocreatures:ray ~6 ~-3 ~2 {TypeMoC:1,NoAI:1b}` | The base of the tail from its right | Painted |

### 2f. Everything else
| Summon | Look at | You should see |
|---|---|---|
| `/summon mocreatures:turkey ~3 ~ ~ {NoAI:1b}` | The tail fan from both sides | The same from both sides |
| `/summon mocreatures:turkey ~3 ~ ~3` (no `NoAI`) | The neck while it looks around | No gaps in the neck |
| `/summon mocreatures:silver_skeleton ~-3 ~ ~ {NoAI:1b}` | Through the gaps in the ribcage, from the front; the sword blade from both sides | The inside and sides of the ribcage painted; the blade the same from both sides. **Expected:** a brief shimmer where an arm touches the ribcage at the peak of its sway (about every 3.5 s). 1.12 had exactly the same |
| `/summon mocreatures:ogre ~-5 ~ ~4 {TypeMoC:3,NoAI:1b}` | The hair tuft from behind and below | Painted |
| `/summon mocreatures:ogre ~-5 ~ ~-4 {TypeMoC:1,NoAI:1b}` | The holes in the ear rings | Painted |
| `/summon mocreatures:cricket ~2 ~ ~-2 {TypeMoC:1,NoAI:1b}`, `/summon mocreatures:crocodile ~4 ~ ~-6 {NoAI:1b}`, `/summon mocreatures:roach ~2 ~ ~-4 {NoAI:1b}`, `/summon mocreatures:snake ~2 ~ ~-6 {TypeMoC:1,NoAI:1b}` | The flat parts (antennae, back spikes, teeth, wings, fangs), from both sides | The same from both sides |

**Lighting note:** an inside face can look a shade lighter or darker than it did in 1.12. The pixels are the same;
26.2 lights a face by the way it points.

**Clean up** (removes only the frozen `NoAI` creatures, not your pets): `/kill @e[nbt={NoAI:1b},distance=..40]`
