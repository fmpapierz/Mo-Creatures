# Test — Round 8 (2026-09-26): the original egg, pegasus and manticore flight, wings that fold smoothly

> **Replaced by TEST_ROUND7_8_COMPLETE.md**, which covers this round and the next one with step-by-step checks. Use that file.


Each section ends with **PASS if / FAIL if**. If something fails, tell me the section, the step and what you saw.

| # | What | Fix |
|---|---|---|
| 1 | The placed egg was a placeholder box | Restored 12.0.5's egg: a small rounded egg made of five boxes, on its real texture layout (the placeholder squeezed the texture to half width). No shadow, a quarter-block hitbox, and every egg the same size, exactly as players saw it in 12.0.5 |
| 2 | Pegasus and pet manticore flew the port's snappy way | Both now fly with 12.0.5's own flight physics, like the wyvern: speed builds up, turns drift, and they coast when you let go. Two more 12.0.5 details came with it: a gliding pegasus sinks slowly, and unicorns and the ghost horse drift down instead of dropping |
| 3 | Wings snapped shut on landing | Every winged creature now folds its wings smoothly when it lands (about half a second) and spreads them on take-off. This covers wyverns, pegasi and the other winged horses, fairy horses, manticores, birds, ducks, bees, flies, butterflies, dragonflies, fireflies, roaches and crickets. Birds, bees and flies also had wing bugs of their own, now fixed |

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
3. Press `F5` for the third-person view when you're riding, so you can see the wings.

---

## 1. The original egg

```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:31}]
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:21}]
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:54}]
```
(an ostrich egg, a snake egg, a mother wyvern egg)

1. Right-click the top of a block with each one to set them down in a row, about a block apart.
2. Look at them up close from the side and from above.

**Look for:**
- **Shape:** a **small, rounded egg**, about a quarter of a block across. It's a little cube with a bump on the top,
  the bottom and two sides, so it reads as an egg, not a box. Before, it was a plain box about a third of a block wide
  and nearly half a block tall.
- **Texture:** the egg's texture sits on it properly. It isn't squashed or smeared sideways.
- **Shadow:** **no shadow** under it, as in 12.0.5.
- **Size:** all three eggs are **the same size**. (12.0.5's code asked for bigger ostrich eggs, but that size never
  reached the game screen, so every egg looked the same.)
- **Hatching:** the eggs still hatch as before (about 30 s, "Egg hatching soon!"). Walking over an egg that has sat
  a moment picks it up.

**PASS if** the eggs are small rounded eggs with a clean texture and no shadow. **FAIL if** you still see a plain box,
a smeared texture, or a shadow.

---

## 2. Pegasus and pet manticore flight (12.0.5 physics)

**What 12.0.5 does for these two:**
- **Speed:** both use the default flyer friction. Their top speed is about **15 blocks/s** (the ghost and undead
  pegasus about 12), reached in about **1 s**.
- **Momentum:** they drift a little through turns and coast about 6 blocks when you let go.
- **Glide:** a pegasus, gliding with no keys held, sinks at only about **1 block/s**, because 12.0.5 slows a falling
  winged horse. The manticore sinks at about 2.6 blocks/s.

**Before:** the pegasus flew 10 blocks/s and the manticore 12.8, both reached almost instantly, with a dead stop when
you let go.

**Controls:** these are unchanged. Look up past about 25° and hold `W` to take off. Look where you want to go. Let go
to glide down. Fly without sprinting to compare with 12.0.5; sprinting still adds 30%.

### 2a. Pegasus
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:39,Tamed:1b,Adult:1b,Saddled:1b}
```
1. Mount it with an empty hand.
2. **Take off:** look up and hold `W`. It lifts off and climbs, picking up speed.
3. **Speed up:** at about 20 blocks up, look level and hold `W`. Its speed **builds up over about a second**, then
   holds at about 15 blocks/s: faster than before, but not instant.
4. **Let go at full speed:** it **coasts about 6 blocks**, then sinks **slowly**, clearly slower than before, about a
   block a second.
5. **Land:** look down and fly into the ground, or let it sink down. It lands and walks again.

### 2b. Black pegasus
```
/summon mocreatures:horse ~3 ~ ~3 {TypeMoC:40,Tamed:1b,Adult:1b,Saddled:1b}
```
It flies exactly like the white pegasus. The port had it slower: it used 5.1.5's speed for the black pegasus, and
12.0.5 fixed that. On the ground it now gallops at the fast fairy-tier pace too.

### 2c. Pet manticore
```
/summon mocreatures:manticore_pet ~3 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:130,Saddled:1b}
```
Repeat 2a steps 2–5. **Look for:** the same build-up, drift and coast as the pegasus, with the same top speed (it used
to be a bit faster than the pegasus). With no keys held it sinks faster than the pegasus, about 2.6 blocks/s, as in
12.0.5.

### 2d. Unicorns drift down
Summon a unicorn and an ordinary horse 15 blocks up, side by side:
```
/summon mocreatures:horse ~3 ~15 ~ {TypeMoC:36,Adult:1b}
/summon mocreatures:horse ~6 ~15 ~ {TypeMoC:1,Adult:1b}
```
**Look for:** the ordinary horse drops in about a second. The unicorn **drifts down** at about 2 blocks/s, taking
around 7 s to land, and takes no fall damage. This is 12.0.5's "floater" slow fall, which the port never had; the ghost
horse (`TypeMoC:22`) does the same. A ridden unicorn floats down the same way after jumping off a ledge.

**PASS if** both flyers build up speed, coast and drift, top out around 15 blocks/s, and the pegasus glides down
slowly. **FAIL if** either still snaps to full speed or stops dead, or the unicorn drops fast.

---

## 3. Wings fold smoothly on landing

**What changed:** every winged creature eases its wings between the flight pose and the folded pose.
- **Landing:** they fold over about **half a second** instead of snapping shut in one frame.
- **Take-off:** they spread over about a fifth of a second.

Watch in third person (`F5`) for the rides, and from a few blocks away for the small ones.

### 3a. Pegasus (and the other winged horses)
Use the pegasus from 2a. Fly, then land.
**Look for:**
- **On landing:** the beating wings **fold down along its sides smoothly**. The outer feathers sweep back as they
  settle. They don't jump to the folded pose.
- **On take-off:** they spread smoothly.

Also try a fairy horse: `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:50,Tamed:1b,Adult:1b,Saddled:1b}`. Its butterfly
wings flutter in flight and settle when it lands.

### 3b. Wyvern
```
/summon mocreatures:wyvern ~4 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}
```
Fly it, then land. **Look for:** the big spread wings **fold back along its body** over about half a second, fingers
and all, instead of snapping. They spread again smoothly as you take off.

### 3c. Manticore
Use the pet manticore from 2c: fly and land. For the wild one, summon it 6 blocks up
(`/summon mocreatures:manticore ~5 ~6 ~ {TypeMoC:1}`). It drops to the ground; it's hostile, but Creative keeps you
safe.
**Look for:** its wings **fold down onto its back smoothly** when it touches the ground.

### 3d. Birds
```
/summon mocreatures:bird ~2 ~ ~ {TypeMoC:1}
/summon mocreatures:bird ~2 ~ ~2 {TypeMoC:3}
```
Watch them for a while as they fly off and come back down.
**Look for:**
- **In the air:** the wings beat.
- **On landing:** the beat **dies away within a few ticks**, and the wings rest folded.
- **On the ground:** the wings **don't move** at all, and the legs walk. In the air, the legs tuck back.
- **Before:** birds beat their wings nonstop, even standing on the ground. 12.0.5 never did.

### 3e. Duck
```
/summon mocreatures:duck ~2 ~5 ~
```
It drops from 5 blocks up, flapping. **Look for:** when it lands, the wings **fold down flat** smoothly instead of
snapping.

### 3f. Insects
Summon a few of each and watch them land (they land every now and then):
```
/summon mocreatures:bee ~2 ~ ~
/summon mocreatures:fly ~2 ~ ~2
/summon mocreatures:butterfly ~2 ~ ~4
/summon mocreatures:dragonfly ~2 ~ ~6
/summon mocreatures:firefly ~2 ~ ~8
/summon mocreatures:roach ~2 ~ ~10
/summon mocreatures:cricket ~2 ~ ~12
```
**Look for:**
- **Bee and fly:** on landing the spread wings **swing back along the body** and become the folded pair. On the ground
  you see **only the folded pair**. Before, the ground pose showed both sets at once; 12.0.5 showed just one.
- **Firefly and roach:** on landing the wings swing back and the **shell closes over them** smoothly, instead of
  swapping in one frame. The firefly also levels out gently from its flying tilt.
- **Butterfly, dragonfly, cricket:** the wing beat **settles** on landing instead of stopping dead mid-beat.

**PASS if** every creature's wings fold or settle over a moment when it lands, and birds, bees and flies look right on
the ground. **FAIL if** any wings still snap shut, or a creature shows two sets of wings.

**Clean up:** `/kill @e[type=!player,type=!item,distance=..30]` removes everything summoned nearby. It also removes
anything else alive within 30 blocks, so move away from your own pets first.
