# Complete test — everything built since Round 6 (2026-09-26)

This one file covers **everything** changed since your last test. It replaces TEST_ROUND7.md and TEST_ROUND8.md, so you
only need this one.

| Part | What | Section |
|---|---|---|
| A | Wyvern flight speed (12.0.5 physics) | 2 |
| B | Pegasus, the other winged horses and the pet manticore fly with 12.0.5 physics; unicorns drift down | 3 |
| C | Wings fold smoothly on landing, every winged creature (plus the bird, bee and fly wing bugs) | 4 |
| D | The original 12.0.5 egg | 5 |
| E | Model scan: painted details that were invisible from behind or below, 25 creatures + the kitty | 6 |
| F | Model scan: flat parts whose back side was laid out wrong (99 faces) | 7 |

Every step has the exact command, where to stand and what you should see. Each section ends with **PASS if / FAIL if**.
If something fails, tell me the step number (e.g. "6.11") and what you saw.

---

## 1. Setup (do this first)

1. The test client is started for you. If it isn't open, run this in PowerShell:
   ```
   cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
   .\gradlew.bat :fabric:runClient
   ```
2. Open your test world, or create a new **Creative** world with **Allow Commands: ON**. A superflat world is easiest
   for sections 2–4. Stand in a big open area.
3. Run these one at a time:
   ```
   /time set day
   /gamerule advance_time false
   /difficulty normal
   ```
   `/difficulty normal` lets the hostile creatures (manticore, werewolf, ogre, rats, silver skeleton) exist; Creative
   keeps you safe from them.
4. Press **`F3`**. Keep it open for sections 2–3 (it shows your coordinates, and the "Facing:" line tells you which
   way you're looking).
5. Two keys you'll use a lot:
   - **`F5`** switches between first and third person. Use third person while riding, so you can see the wings.
   - **`Shift`** gets off a mount.

**About directions in sections 6–7:** a creature summoned with these commands **faces south** (toward +Z; press F3
and face the same way to check). So:
- its **front** is the south side and its **back** is the north side;
- its **left** is the east side and its **right** is the west side.

"Behind-left" means stand north-east of it. "From below" means get under it; the commands put those creatures a few
blocks up in the air (`NoAI:1b` keeps a creature frozen exactly where it's summoned, even mid-air).

**Cleaning up between sections:** `/kill @e[type=!player,type=!item,distance=..40]` removes every creature within 40
blocks. It also removes your own pets, so do it away from them.

---

## 2. Wyvern flight speed (12.0.5 physics)

**What changed:**
- **Speed factor:** 12.0.5 gives every ridden wyvern the same speed factor. The port had used an older version's
  (5.1.5) higher values.
- **Top speed:** ordinary wyverns fly about **21.5 blocks/s**; the big ones (mother, undead, light, dark) about
  **30 blocks/s**.
- **Momentum:** speed **builds up over 1.5–2 s**, turns **drift wide**, and it **coasts** when you let go of `W`.
- **Before:** 23 and 35 blocks/s, reached in 0.15 s, with a dead stop when you let go.
- **On the ground:** it walks at 12.0.5's 13 blocks/s. Before, it ran at full flight speed there too.
- **Sprint:** still adds 30%. 12.0.5 had no sprint, so **don't sprint** while comparing (don't hold `Ctrl` or
  double-tap `W`).

**2.1** Summon an ordinary wyvern and a mother, 8 blocks apart:
```
/summon mocreatures:wyvern ~4 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}
```
```
/summon mocreatures:wyvern ~-4 ~ ~ {TypeMoC:5,Tamed:1b,Adult:1b,AgeMoC:180,Saddled:1b}
```
**2.2** Walk to the **smaller** one (the ordinary wyvern). Right-click it with an **empty hand** to get on. Press `F5`
for third person.

**2.3 On the ground:** look straight ahead and hold `W` for 3 seconds.
**Look for:** it walks at about the speed of a quick horse. It does **not** shoot across the ground.

**2.4 Take off:** look up a bit more than 25° (the top of the screen above the horizon) and hold `W`.
**Look for:** it lifts off and climbs with its wings beating. Climb to about 20 blocks above the ground (F3 "XYZ", the
middle number).

**2.5 Stop and hover:** let go of `W` and wait about 3 seconds.
**Look for:** it **coasts forward**, then hangs in the air **sinking slowly** with its wings beating.

**2.6 Speed-up:** look level (the horizon in the middle of the screen) and hold `W`.
**Look for:** the speed **builds up over about a second and a half**, then holds steady. It does **not** jump to full
speed in an instant.

**2.7 Distance check** (optional, rough):
1. Let go of `W` and wait until it has almost stopped.
2. Turn until F3 "Facing:" says **north** or **south**. Note the **Z** number (the third one in "XYZ").
3. Hold `W` while you count five seconds out loud, then let go and note Z again.

**Look for:** about **95 blocks** covered (before this fix: about 115). ±20 is fine; hand-counting is rough.

**2.8 Coast:** at full speed, let go of `W`.
**Look for:** it **glides on about 13 blocks**, slowing down, then hangs and sinks. Before, it stopped almost dead.

**2.9 Drift:** at full speed, keep `W` held and swing the mouse sharply 90° to one side.
**Look for:** it **swings wide** and takes about a second to settle on the new heading. Before, it snapped straight
onto the new line.

**2.10 Mother:** land (look down and fly into the ground), press `Shift` to get off, get on the **mother**, and repeat
2.4–2.9.
**Look for:**
- **Build-up:** she takes a little longer, about 2 s.
- **Top speed:** clearly faster, about 30 blocks/s. In 2.7 that's about **125 blocks** in five seconds (before: 175).
- **Coast:** she glides on further when you let go, about 25 blocks.

**2.11 Sprint:** while flying, hold `Ctrl` (or double-tap `W`).
**Look for:** about 30% faster. This is the port's mount sprint, the same one the horse got in Round 4.

**PASS if** speed builds up instead of snapping, it coasts and drifts, it walks at horse pace on the ground, and the
mother is the faster one. **FAIL if** it still reaches full speed instantly, stops dead when you let go, or runs at
flight speed on the ground. If all of this matches but full speed still feels too fast, tell me: that's 12.0.5's own
top speed, and it's one number to turn down.

---

## 3. Pegasus, other winged horses, pet manticore: 12.0.5 flight; unicorns drift down

**What changed:**
- **Physics:** both now fly with 12.0.5's own flight physics, the same as the wyvern in section 2.
- **Top speed:** about **15 blocks/s** for both (before: 10 for the pegasus, 12.8 for the manticore), reached in about
  **1 s**.
- **Momentum:** they drift slightly in turns and coast about **6 blocks** when you let go.
- **Pegasus glide:** it now sinks at only about **1 block/s** (before: 2.4). 12.0.5 slows any falling winged horse.
  The manticore still sinks about 2.6 blocks/s, as in 12.0.5.
- **Black pegasus:** now as fast as the other magic horses. The port had it at an older version's slower speed.
- **Unicorns and the ghost horse** now **drift down** at about 2 blocks/s instead of dropping. 12.0.5 had this; the
  port never did.
- **Controls:** unchanged. Look up past 25° + `W` to take off, look where you want to go, let go to glide down.

**3.1** Summon a pegasus:
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:39,Tamed:1b,Adult:1b,Saddled:1b}
```
Get on with an empty hand, then press `F5`.

**3.2 Take off:** look up past 25° and hold `W`.
**Look for:** it lifts off and **picks up speed as it climbs**, instead of shooting up at once. Climb about 20 blocks.

**3.3 Speed-up:** let go of `W` until it's nearly still, then look level and hold `W`.
**Look for:** the speed **builds up over about a second** and settles at about 15 blocks/s: faster than before, but not
instant. Optional distance check like 2.7: about **68 blocks** in five seconds (before: 50).

**3.4 Coast and glide:** at full speed, let go of `W`.
**Look for:**
- **Coast:** it glides on **about 6 blocks**.
- **Sink:** it then sinks **slowly, about 1 block a second**. That's noticeably slower than before (2.4) and slower
  than the manticore in 3.8.
- **Wings:** they keep beating.

**3.5 Drift:** at full speed, hold `W` and swing the mouse 90°.
**Look for:** a slight swing wide. It's much lighter than the wyvern, but not a snap.

**3.6 Land:** look down and fly into the ground, or let it sink.
**Look for:** it lands and walks with normal gravity.

**3.7 Black pegasus:**
```
/summon mocreatures:horse ~3 ~ ~3 {TypeMoC:40,Tamed:1b,Adult:1b,Saddled:1b}
```
Repeat 3.2–3.4. **Look for:** exactly the same speed as the white pegasus. On the ground it also gallops as fast as the
fairy horses now.

**3.8 Pet manticore:**
```
/summon mocreatures:manticore_pet ~3 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:130,Saddled:1b}
```
Get on, then repeat 3.2–3.6.
**Look for:**
- **Build-up, coast and drift:** the same as the pegasus.
- **Top speed:** the same as the pegasus. It used to be a bit faster.
- **Glide:** with no keys held it sinks faster than the pegasus, about 2.6 blocks/s.

**3.9 The other winged horses** (optional, same checks as 3.2–3.4):
```
/summon mocreatures:horse ~3 ~ ~ {TypeMoC:50,Tamed:1b,Adult:1b,Saddled:1b}
```
```
/summon mocreatures:horse ~3 ~ ~3 {TypeMoC:32,Tamed:1b,Adult:1b,Saddled:1b}
```
```
/summon mocreatures:horse ~3 ~ ~6 {TypeMoC:25,Tamed:1b,Adult:1b,Saddled:1b}
```
These are a white fairy horse (butterfly wings), the bat horse and the undead pegasus. The fairy and the bat horse fly
like the pegasus. The undead pegasus is a bit slower (about 12 blocks/s), as in 12.0.5.

**3.10 Unicorns drift down:** summon a unicorn and an ordinary horse 15 blocks up, side by side:
```
/summon mocreatures:horse ~3 ~15 ~ {TypeMoC:36,Adult:1b}
```
```
/summon mocreatures:horse ~6 ~15 ~ {TypeMoC:1,Adult:1b}
```
**Look for:**
- **The ordinary horse** drops to the ground in about a second.
- **The unicorn** **drifts down slowly** (about 2 blocks/s), taking around 7 seconds, and isn't hurt.
- **The ghost horse** (`TypeMoC:22`) does the same.

**3.11 Sprint:** while flying any of them, hold `Ctrl`. **Look for:** about 30% faster.

**PASS if** both flyers build up speed, coast and drift, top out around 15 blocks/s, the pegasus glides down slowly,
and the unicorn drifts down. **FAIL if** either still snaps to full speed or stops dead, or the unicorn drops like a
stone.

---

## 4. Wings fold smoothly on landing (every winged creature)

**What changed:** every winged creature eases its wings between the flight pose and the folded pose.
- **Landing:** they fold over about **half a second**. Before, they snapped shut in one frame.
- **Take-off:** they spread over about a fifth of a second.
- **Bugs fixed along the way:**
  - **Birds** beat their wings nonstop, even standing still. Now they flap only in the air, as in 12.0.5.
  - **Bees and flies** showed their folded wings and their spread wings at the same time. Now they show one set, as
    in 12.0.5.

Watch the rides in third person (`F5`), and the small creatures from 2–3 blocks away.

**4.1 Pegasus (feathered wings):** use the pegasus from 3.1 (summon again if you cleaned up). Fly, then land.
**Look for:**
- **On touch-down:** the beating wings **fold down along its sides smoothly**. The long outer feathers sweep back as
  they settle, over about half a second.
- **On take-off:** the wings spread smoothly (quicker).
- **Before:** they jumped between the two poses in one frame.

**4.2 Fairy horse (butterfly wings):** use the fairy horse from 3.9 (`TypeMoC:50`). Fly, then land.
**Look for:** the membranes flutter fast in flight and **settle** to their resting spread on landing, instead of
jumping there.

**4.3 Wyvern:** use the wyvern from 2.1. Fly, then land.
**Look for:** the big spread wings **fold back along its body**, arm, fingers and all, over about half a second. They
spread again smoothly as you take off.

**4.4 Pet manticore:** use the one from 3.8. Fly, then land.
**Look for:** its wings **fold down onto its back smoothly**.

**4.5 Wild manticore:** summon one 6 blocks up:
```
/summon mocreatures:manticore ~5 ~6 ~ {TypeMoC:1}
```
It falls to the ground (it's hostile, but Creative keeps you safe).
**Look for:** its wings are spread while it falls and **fold smoothly** when it lands.

**4.6 Birds:**
```
/summon mocreatures:bird ~2 ~ ~ {TypeMoC:1}
```
```
/summon mocreatures:bird ~2 ~ ~2 {TypeMoC:3}
```
Watch them for a minute as they fly off and land again.
**Look for:**
- **In the air:** the wings beat fast and the legs are tucked back.
- **On landing:** the wing beat **dies away within a few ticks** and the wings rest folded.
- **On the ground:** the wings **don't move at all**, and the legs walk.
- **Before:** birds beat their wings nonstop, even when standing on the ground.

**4.7 Duck:**
```
/summon mocreatures:duck ~2 ~5 ~
```
It drops from 5 blocks up, flapping.
**Look for:** when it lands, the wings **fold down flat** against its body smoothly, instead of snapping.

**4.8 Bee and fly:**
```
/summon mocreatures:bee ~2 ~1 ~
```
```
/summon mocreatures:fly ~2 ~1 ~2
```
They fly around and land now and then. Watch one land.
**Look for:**
- **On landing:** the buzzing spread wings **swing back along the body** and become the folded pair.
- **On the ground:** you see **only one pair of wings, folded flat** on its back.
- **In the air:** only the spread, buzzing pair.
- **Before:** both sets showed at the same time.

**4.9 Firefly and roach:**
```
/summon mocreatures:firefly ~2 ~1 ~4
```
```
/summon mocreatures:roach ~2 ~ ~6
```
Watch one land. The firefly lands often; the roach flies when startled, so walk at it.
**Look for:**
- **Wings:** on landing they stop beating and **swing back along the body**.
- **Shell:** the **shell (the two hard wing covers) closes smoothly** over them, instead of swapping in one frame.
- **Firefly body:** it **levels out gently** from its forward flying tilt.

**4.10 Butterfly, dragonfly, cricket:**
```
/summon mocreatures:butterfly ~2 ~1 ~8
```
```
/summon mocreatures:dragonfly ~2 ~1 ~10
```
```
/summon mocreatures:cricket ~2 ~ ~12
```
Watch each one land. The cricket "flies" during its jumps.
**Look for:** the wing beat **settles** on landing, instead of stopping dead mid-beat.
- **Butterfly:** the wings return to their resting fold.
- **Dragonfly:** the wings stay out but go still.
- **Cricket:** the buzz fades.

**PASS if** every creature's wings fold or settle over a moment when it lands, birds don't flap on the ground, and bees
and flies never show two sets of wings. **FAIL if** any wings still snap shut in one frame, or you see two wing sets.

---

## 5. The original egg

**What changed:**
- **Shape:** the placed egg is 12.0.5's again: a small **rounded** egg, about a quarter of a block across, built from
  a little cube with a bump on the top, the bottom and two sides.
- **Before:** a plain box about a third of a block wide and nearly half a block tall, with its texture squeezed to half
  width.
- **Shadow and hitbox:** it has no shadow and a quarter-block hitbox, as in 12.0.5.
- **Size:** every egg is the same size. 12.0.5's code asked for bigger ostrich eggs, but that size never reached the
  game screen, so every egg you ever saw was this size.

**5.1** Get three different eggs:
```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:31}]
```
```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:21}]
```
```
/give @s mocreatures:mocegg[minecraft:custom_data={EggType:54}]
```
(an ostrich egg, a snake egg, a mother wyvern egg)

**5.2** Hold each one and right-click the top of a grass block to set it down. Put the three in a row about a block
apart.

**5.3** Crouch next to them and look from the side, then from straight above.
**Look for:**
- **Shape:** a **small rounded egg**, not a box.
- **Texture:** the pattern sits cleanly on it, not smeared or squashed sideways.
- **Shadow:** **no dark shadow** under it.
- **Size:** all three are **the same size**.

**5.4** Stand back and wait about 30 seconds.
**Look for:** each egg shows "Egg hatching soon!", hops, and hatches as before.

**5.5** Set down one more egg and walk straight over it.
**Look for:** you pick it up (it needs to sit a moment first).

**PASS if** the eggs are small rounded eggs with a clean texture and no shadow, and they still hatch. **FAIL if** you see
a plain box, a smeared texture or a shadow.

---

## 6. Model scan: painted details that were invisible from behind or below

**Background:** 1.12 drew both sides of every face on a mob, and the original art relies on it. Many boxes are painted
on one face and left see-through on the others, so you look **through** the empty faces at the painted one. The port
hides back faces, so those details disappeared from those angles, like the wyvern's eyes did. About 560 faces in 25
creatures now show exactly the pixels 1.12 showed. No shapes or animations changed.

**How to check each line:** go to the spot described and look at the part.
- **PASS:** the detail is there and **painted**, the same colours you see from its usual side.
- **FAIL:** it's missing, or you see straight through it to the sky or ground.

For a FAIL, tell me the step number and what you saw.

**Do one subsection at a time** (6A, then 6B, ...). Before each new one, clear the previous creatures with
`/kill @e[type=!player,type=!item,distance=..40]`. Otherwise the big ones overlap.

**Lighting:** an inside face can look a shade lighter or darker than it did in 1.12. The pixels are the same; 26.2
shades a face by the way it points. That's not a fail.

### 6A. Wyvern
```
/summon mocreatures:wyvern ~5 ~3 ~ {TypeMoC:4,Adult:1b,AgeMoC:100,NoAI:1b}
```
(a sun wyvern hanging 3 blocks up with its wings spread)
- **6.1 Beak inside:** stand **behind it and to its left** (north-east of it), level with its head, and look at the
  head. **See:** the inside of the beak/mouth is painted.
- **6.2 Eyes (control):** look at the head **from the side**. **See:** the eyes are there (the Round 5 fix).

```
/summon mocreatures:wyvern ~5 ~ ~6 {TypeMoC:2,Adult:1b,AgeMoC:100,NoAI:1b}
```
(a mixed-colour wyvern on the ground)
- **6.3 Tail root:** stand **behind it** and look down at **where the tail joins the body**. **See:** painted, no hole.

```
/summon mocreatures:wyvern ~5 ~ ~-6 {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b,ArmorType:1,NoAI:1b}
```
(saddled, iron armour)
- **6.4 Helmet:** stand **behind it and to its left** and look at the **helmet** on its head. **See:** the helmet's inner
  walls are painted.

```
/summon mocreatures:wyvern ~5 ~ ~-11 {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b,ArmorType:2,NoAI:1b}
```
```
/summon mocreatures:wyvern ~5 ~ ~-16 {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b,ArmorType:3,NoAI:1b}
```
- **6.5 Gold and diamond helmets:** check both new wyverns the same way as 6.4. **See:** the same for each.

```
/summon mocreatures:wyvern ~-5 ~3 ~ {TypeMoC:5,Tamed:1b,Adult:1b,AgeMoC:180,Saddled:1b,NoAI:1b}
```
(a saddled mother, 3 blocks up)
- **6.6 Bit:** stand **underneath its head** and look up at the **bit**, the bar through its mouth. **See:** painted.
  (A sliver where the bit touches the strap can still flicker; 1.12 flickered there too.)

### 6B. Elephant (big: give it room)
```
/summon mocreatures:elephant ~8 ~3 ~ {TypeMoC:5,ArmorStage:3,NoAI:1b}
```
(a decorated Indian elephant with its cabin, 3 blocks up)
- **6.7 Cabin inside:** fly up **in front of it and above**, and look down into the **cabin** on its back. **See:** the
  cabin's inner walls are painted. (Two tiny spots behind the rails can still flicker; 1.12 did too.)
- **6.8 Cabin roof:** on its **left** (east side), fly just **below the roof's edge**, and look up at the roof. **See:** its
  underside is painted.
- **6.9 Drape:** stand **below it, on its right** (west side), and look up at the cloth **drape** hanging along its
  side. **See:** its underside is painted.

```
/summon mocreatures:elephant ~8 ~3 ~9 {TypeMoC:1,NoAI:1b}
```
(an African elephant, 3 blocks up. Only this kind has the big ears.)
- **6.10 Big ears:** stand **behind it** and look at the backs of its big ears. **See:** painted.
- **6.11 Tail tuft:** go **under its tail** and look up at the tuft at the tip. **See:** painted.

### 6C. Ent (a walking tree)
```
/summon mocreatures:ent ~7 ~3 ~ {TypeMoC:1,NoAI:1b}
```
(an oak ent, 3 blocks up)
- **6.12 Fingers:** stand **behind it** and look at its **hands**. **See:** the fingers are painted from behind.
- **6.13 Feet:** go **underneath** and look up at its **feet**. **See:** painted.
- **6.14 Ankles:** look at the **ankles** from the side. **See:** painted.
- **Known leftover (not a fail):** a faint speckle deep **inside the leafy crown**, visible only through gaps between
  the leaves. 1.12 flickered there too; fixing it would take about a thousand extra boxes.

```
/summon mocreatures:ent ~7 ~ ~8 {TypeMoC:2,NoAI:1b}
```
(a birch ent)
- **6.15 Tree base:** stand **behind it** and look at the **base of the tree** (where the trunk meets its back).
  **See:** painted.

### 6D. Big cat
```
/summon mocreatures:big_cat ~5 ~3 ~ {TypeMoC:1,NoAI:1b}
```
(a male lion, 3 blocks up)
- **6.16 Medallion:** stand **in front of it** and look at its **chest/throat**. **See:** the collar's **medallion**.
- **6.17 Claws:** go **underneath** and look up at its paws. **See:** the claws are painted.

```
/summon mocreatures:big_cat ~5 ~ ~6 {TypeMoC:1,Tamed:1b,Saddled:1b,NoAI:1b}
```
(saddled)
- **6.18 Stirrup rings:** look at the **rings** hanging from the saddle on each side, from above and from the side.
  **See:** painted. (The very bottom of each ring can flicker where it touches the strap; 1.12 did too.)

### 6E. Manticore
```
/summon mocreatures:manticore ~5 ~3 ~ {TypeMoC:2,NoAI:1b}
```
(a dark manticore, 3 blocks up, wings spread)
- **6.19 Wing undersides:** go **underneath** and look up at the wings. **See:** the **wing membranes are painted**
  from below. Before, big parts of the wings were missing from underneath.
- **6.20 Claws:** from below, look at the paws. **See:** painted.

```
/summon mocreatures:manticore_pet ~-5 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:130,Saddled:1b,NoAI:1b}
```
- **6.21 Stirrup rings:** look at the **rings** on each side of the saddle. **See:** painted.

### 6F. Ostrich
```
/summon mocreatures:ostrich ~4 ~ ~ {TypeMoC:7,NoAI:1b}
```
- **6.22 Folded wings:** stand **behind it** and look at its **folded wing feathers**. **See:** painted.

```
/summon mocreatures:ostrich ~4 ~ ~5 {TypeMoC:3,Tamed:1b,Saddled:1b,Helmet:2,Chested:1b,FlagColor:1,NoAI:1b}
```
(saddled, with an iron helmet, saddlebags and a flag)
- **6.23 Helmet:** stand **in front of it and to its left** and look at the **helmet**. **See:** the helmet's inner side
  walls are painted.
- **6.24 Saddle rings:** look at the **rings** on the saddle. **See:** painted. (The very bottom can flicker; 1.12 did
  too.)
- (The flag on its back is part of section 7, step 7.9.)

### 6G. Werewolf and wild wolf
```
/summon mocreatures:werewolf ~-4 ~ ~ {TypeMoC:2,HumanForm:0b,NoAI:1b}
```
(a brown werewolf in beast form)
- **6.25 Teeth:** stand **in front of it**, crouch, and look up into its open mouth. **See:** the **lower teeth** from
  the front and the **upper teeth** from below are painted.
- **6.26 Back:** stand **behind it** and look at the **tail**, the **backs of the thighs** and the **sideburns** (the
  tufts at the sides of the head). **See:** painted.

```
/summon mocreatures:wild_wolf ~-4 ~ ~5 {TypeMoC:2,NoAI:1b}
```
- **6.27 Tail and flanks:** stand **behind it and above** it (jump or fly a little) and look at the **tail** and its
  **flanks** (the sides of its body). **See:** painted.

### 6H. Small animals
Summon each and check it before the next (or summon them all; the positions don't overlap).

```
/summon mocreatures:deer ~3 ~ ~ {TypeMoC:1,Adult:1b,NoAI:1b}
```
- **6.28 Antlers:** stand on its **left** (east side) and look at the antlers. **See:** painted, the same as from its
  right side.

```
/summon mocreatures:duck ~3 ~2 ~3 {NoAI:1b}
```
(2 blocks up)
- **6.29 Head:** go **under it** and look up at its **head**. **See:** painted.
- **6.30 Legs:** fly **above it** and look down at its **legs**. **See:** painted.

```
/summon mocreatures:bird ~3 ~ ~6 {TypeMoC:3,NoAI:1b}
```
(a green bird; a frozen bird may beat its wings, which is expected, since a frozen creature can't tell it's on the
ground)
- **6.31 Legs:** stand **in front of it**, look down at its **legs** from above. **See:** painted (the leg and the
  toes).

```
/summon mocreatures:boar ~-3 ~ ~ {Adult:1b,NoAI:1b}
```
- **6.32 Ears:** stand **in front of it** and look at its **ears**. **See:** painted.

```
/summon mocreatures:bunny ~-3 ~ ~3 {TypeMoC:1,NoAI:1b}
```
- **6.33 Whiskers:** stand **behind it** and look at the **cheeks**. **See:** the whiskers are painted.

```
/summon mocreatures:rat ~-3 ~ ~6 {TypeMoC:1,NoAI:1b}
```
```
/summon mocreatures:hell_rat ~-3 ~ ~8 {NoAI:1b}
```
```
/summon mocreatures:mouse ~-3 ~ ~10 {TypeMoC:1,NoAI:1b}
```
- **6.34 Whiskers:** stand **behind** each and look at the whiskers. **See:** painted. For the mouse, also look from
  **in front**: its right whisker is the one that shows from there.

```
/summon mocreatures:fox ~3 ~2 ~9 {TypeMoC:1,NoAI:1b}
```
(2 blocks up)
- **6.35 Head:** stand **on its right** (west side), **below it**, and look up at its **head**. **See:** painted. (A
  small spot at the back of the head can flicker when the head turns; 1.12 did too.)

```
/summon mocreatures:dragonfly ~3 ~1 ~12 {TypeMoC:1,NoAI:1b}
```
- **6.36 Tail:** stand **behind it** and look at the **abdomen** (its long tail). **See:** painted.

```
/summon mocreatures:kitty ~-3 ~ ~-3 {TypeMoC:1,Tamed:1b,NoAI:1b}
```
- **6.37 Whiskers:** stand on its **left** (east side) and look at the **cheeks**. **See:** the whiskers are painted.
- **6.38 Collar:** crouch on its **right**, below head height, and look at the **collar** under its chin. **See:** the
  medallion is painted.

### 6I. Fish (in a pool)
Make a pool next to you:
```
/fill ~3 ~-4 ~-3 ~9 ~-1 ~3 minecraft:water
```
**Without moving from that spot** (the positions are relative to you), summon all five:
```
/summon mocreatures:fishy ~6 ~-2 ~ {TypeMoC:9,NoAI:1b}
```
```
/summon mocreatures:small_fish ~5 ~-2 ~2 {TypeMoC:3,NoAI:1b}
```
```
/summon mocreatures:small_fish ~7 ~-2 ~2 {TypeMoC:4,NoAI:1b}
```
```
/summon mocreatures:shark ~6 ~-3 ~-1 {NoAI:1b}
```
```
/summon mocreatures:ray ~5 ~-4 ~-2 {TypeMoC:1,NoAI:1b}
```
Then swim down and look:
- **6.39 Fishy tail:** stand **behind it and to its left**. **See:** the tail is painted.
- **6.40 Angler fish** (the first small fish): **behind and to its right**, look at the **tail**. **See:** painted.
- **6.41 Clownfish** (the second small fish): **behind it**, look at the **middle fin** on its body. **See:** painted.
- **6.42 Shark:** **behind and to its right**, look at its **right fin**. **See:** painted.
- **6.43 Manta ray:** on its **right**, look at the **base of the tail**. **See:** painted.

### 6J. Everything else
```
/summon mocreatures:turkey ~3 ~ ~ {NoAI:1b}
```
```
/summon mocreatures:turkey ~3 ~ ~3
```
- **6.44 Neck:** watch the **second** turkey (it can move) turn its head around. **See:** no gaps appear in the neck.
  (The first turkey is for 7.11.)

```
/summon mocreatures:silver_skeleton ~-3 ~ ~ {NoAI:1b}
```
- **6.45 Ribcage:** stand **in front of it and a little above**, and look **through the gaps of its ribcage**. **See:**
  the inside and the sides of the ribcage are painted.
- **6.46 Arms:** look at its **arms from below**. **See:** painted.
- **Expected (not a fail):** about every 3.5 s, where an arm touches the ribcage at the end of its sway, there's a
  brief shimmer. 1.12 had exactly the same. If you'd rather have no shimmer at all, I can remove the ribcage sides and
  it won't happen; tell me.

```
/summon mocreatures:ogre ~-6 ~ ~5 {TypeMoC:3,NoAI:1b}
```
(a red ogre)
- **6.47 Hair:** stand **behind it and crouch**, looking up at the **hair tuft** on top of its head. **See:** painted.
  From its **right** side, look at the **rope** tying the hair. **See:** painted.

```
/summon mocreatures:ogre ~-6 ~ ~-5 {TypeMoC:1,NoAI:1b}
```
(a green ogre)
- **6.48 Ear rings:** stand **in front of it and to its left** and look at the **rings** in its ear. **See:** the hole
  in the ring is painted.

**PASS if** every detail in 6.1–6.48 is painted from the angle given. **FAIL if** any of them is missing or
see-through. Tell me the step number.

---

## 7. Model scan: flat parts that should look the same from both sides

**Background:** many thin parts (fins, spikes, flags, membranes, antennae) are flat sheets. In 1.12 their back showed
the same picture as the front, mirrored. In the port, 99 of these backs showed their picture laid out the wrong way
round (scrambled, or the wrong bit of texture). Now they're right.

**How to check:** look at each flat part from one side, then walk around and look from the other side.
- **PASS:** you see the same picture, mirrored.
- **FAIL:** one side is blank, scrambled, or clearly a different picture.

Clear section 6 first with `/kill @e[type=!player,type=!item,distance=..40]`. The positions below are chosen so none
of them overlap, so you can summon them all at once or a few at a time.

```
/summon mocreatures:cricket ~1 ~ ~-6 {TypeMoC:1,NoAI:1b}
```
- **7.1 Cricket:** the **antennae**, **legs** and the two little **tail spikes**, from both sides.

```
/summon mocreatures:crocodile ~-4 ~ ~-11 {NoAI:1b}
```
- **7.2 Crocodile:** the **spikes along its back and tail**, the spikes over its eyes, and the **teeth**, from both
  sides.

```
/summon mocreatures:dragonfly ~1 ~1 ~-9 {TypeMoC:1,NoAI:1b}
```
- **7.3 Dragonfly:** the **legs**, from both sides.

```
/summon mocreatures:elephant ~16 ~ ~ {TypeMoC:1,Tusks:1,NoAI:1b}
```
```
/summon mocreatures:elephant ~16 ~ ~9 {TypeMoC:1,Tusks:2,NoAI:1b}
```
```
/summon mocreatures:elephant ~16 ~ ~-9 {TypeMoC:1,Tusks:3,NoAI:1b}
```
- **7.4 Elephant tusk fins:** wooden, iron and diamond tusk armour. Look at the **flat fins** on the tusks of each, from
  both sides.

Make a pool next to you, then summon the fish into it **without moving** (the same pool as 6I; skip the first command if
you're standing where you made it):
```
/fill ~3 ~-4 ~-3 ~9 ~-1 ~3 minecraft:water
```
```
/summon mocreatures:medium_fish ~6 ~-2 ~ {TypeMoC:1,NoAI:1b}
```
- **7.5 Medium fish:** all its **fins** (top, bottom, sides, tail), from both sides.

```
/summon mocreatures:roach ~1 ~ ~6 {NoAI:1b}
```
- **7.6 Roach:** the **legs** and the two **tail feelers**, from both sides.

```
/summon mocreatures:silver_skeleton ~-3 ~ ~ {NoAI:1b}
```
- **7.7 Silver skeleton:** the **sword blades**, from both sides.

```
/summon mocreatures:snake ~1 ~ ~9 {TypeMoC:1,NoAI:1b}
```
- **7.8 Snake:** the **fangs**, from both sides (look in from the front, left then right).

```
/summon mocreatures:ostrich ~-8 ~ ~5 {TypeMoC:3,Tamed:1b,Saddled:1b,Helmet:7,Chested:1b,FlagColor:1,NoAI:1b}
```
- **7.9 Ostrich flag:** the **flag** on its back, from both sides. Optional: summon again with `FlagColor:2` through
  `FlagColor:16` for the other colours.
- **7.10 Reptile helmet ears:** the **two ears** on the reptile helmet (`Helmet:7`), from both sides.

```
/summon mocreatures:ostrich ~-8 ~ ~-5 {TypeMoC:5,NoAI:1b}
```
- **7.11 Demon ostrich:** the **feathers on its neck** and its **dark wings**, from both sides.

- **7.12 Turkey:** summon `/summon mocreatures:turkey ~-3 ~ ~-4 {NoAI:1b}`. Look at the
  **tail fan** from behind and from in front. **See:** the same picture.

```
/summon mocreatures:wyvern ~-14 ~3 ~ {TypeMoC:4,Adult:1b,AgeMoC:100,NoAI:1b}
```
- **7.13 Wyvern wing membranes:** the skin between the wing fingers, from **above and below**.
- **7.14 Wyvern ear frills:** the frills at the sides of the head, from **both sides**.

**PASS if** every flat part in 7.1–7.14 looks the same from both sides. **FAIL if** a side is blank, scrambled or a
different picture.

---

## What's expected, not a fail

- **Ent crown:** a faint speckle deep inside the crown, seen only through gaps between the leaves (6.14).
- **Silver skeleton:** a brief shimmer where an arm meets the ribcage at the end of each sway (6.46).
- **Small flicker spots where two parts touch, as in 1.12:**
  - the wyvern's bit where it meets the strap (6.6);
  - two spots behind the elephant's cabin rails (6.7);
  - the bottoms of the stirrup and saddle rings (6.18, 6.24);
  - the back of the fox's head when it turns (6.35).
- **Frozen creatures:** a bird frozen with `NoAI` may beat its wings on the ground (6.31). A normal bird (4.6) doesn't.
- **Lighting:** inside faces can be a shade lighter or darker than in 1.12.

When you're done, tell me the step numbers of anything that failed, or "all good".
