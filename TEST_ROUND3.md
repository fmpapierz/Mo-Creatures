# Test — Round 3 (2026-09-25): manticores, eggs, recipes, pets, horses, wyvern, kitty bed, bunny

Every fix from your last report, with exact steps. Each section ends with **PASS if / FAIL if**. When something
fails, tell me the section + step number and what you saw (a screenshot helps).

| # | Your report | What was wrong | What changed |
|---|---|---|---|
| A | Pet amulet / fishing net recipes | The pet amulet had no recipe. The net's recipe existed but only showed in the recipe book after you'd picked up Shark Teeth | Pet amulet recipe added (12.0.5 layout). The net now shows in the recipe book once you have string |
| B | Manticores drop spawn eggs, not manticore eggs | The drop was a creative spawn egg | Kills now drop a real coat-matched Manticore Egg (25% chance) |
| C | A placed manticore egg never hatches | Land eggs secretly needed a torch within 4 blocks (a 5.1.5 rule) | Eggs now hatch anywhere out of water in ~30 s, like 12.0.5, with an "Egg hatching soon!" message and hops |
| D/E | Can't tame manticores | Combination of the above, plus no feedback when the medallion is refused | The egg route works, and the medallion now tells you what's missing |
| F | Manticores never spawn in the wild | Never added to any biome's spawn list | They now spawn at night in the Overworld and anywhere in the Nether |
| G | Pets keep following me | Every tamed pet had a follow-and-teleport AI the original never had | Pets roam freely. The whip toggles *stay* ↔ *roam*; a rope or lead brings a pet along |
| H | Bat horse wings invisible | The hostile bat horse used a separate model whose wing tiles are blank on its texture | Hostile horses use the real horse model, as the original did |
| I | Horse legs detach when walking | The lower leg didn't follow the knee | Original knee-joint math restored (also fixes rearing and flying leg poses) |
| J | Wrong coats from the horse egg | Used 5.1.5's coat table | 12.0.5 rules: tier-2 coats (6/7/8) plus zebras, with biome rules |
| K | Zebra can't be tamed | You're refused on foot (original rule), silently, and the port had invented a 5-apple rule | Original rule kept: ride a spotted/cow horse up to it and give **one** apple. On foot it now tells you so |
| L | Wyvern eye not on its face | The eye is painted *inside* a socket, and the port culled those faces | Sockets rebuilt so the painted eye shows |
| M | Wyvern wings stay folded in flight | The flight pose reused the folded pose, and the "flying" flag was often wrong | Original flight pose (wings open and beating); correct flying detection |
| N | Kitty beds: no colour, float in the air, can't break | No wool sheet was drawn at all; NoAI stopped gravity; all damage was ignored | Coloured sheet restored; beds fall to the ground; punching breaks them and drops the coloured item |
| O | Bunnies glide instead of hopping | No hop at all, and the model floated 3 px above the ground | Original bound restored (0.3 jump every 15 ticks while moving) plus a hop pose; feet on the ground |

---

## 0. Setup (do this once)

1. **Run the right copy.** These fixes are in the worktree, not yet in your main folder. Open PowerShell and run:
   ```
   cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
   .\gradlew.bat :fabric:runClient
   ```
   Wait for the Minecraft title screen.
2. **Make a fresh test world.** Singleplayer → Create New World → name it `Round3` → Game Mode **Creative** →
   **Allow Commands: ON** → Create New World.
3. **Paste these once** (press `T` to open chat, paste, press Enter; one command at a time):
   ```
   /difficulty normal
   /time set day
   /gamerule advance_time false
   ```
   `advance_time false` freezes the clock so it stays day (or night) while you test. Use `/time set midnight` or
   `/time set day` whenever a step asks for it.
4. **Handy keys:**
   - `T` opens chat. `↑` in chat recalls the last command, so you can repeat it.
   - `F5` cycles camera views (first person → behind → in front).
   - `F3`+`B` toggles hitbox outlines.
   - `Q` drops the held item.
   - `Shift` sneaks, and dismounts a mount.
   - Double-tap `Space` to fly in creative.
5. `/clear` empties your inventory between sections if it gets cluttered.

---

## A. Recipes: Pet Amulet and Fish Net

**A1. Pet Amulet**
1. `/give @s minecraft:crafting_table`, then place it and right-click it.
2. `/give @s minecraft:gold_nugget 4` and `/give @s minecraft:diamond 1`
3. Lay out the 3×3 grid exactly like this:
   ```
   [Gold Nugget] [  empty  ] [Gold Nugget]
   [  empty    ] [ Diamond ] [  empty    ]
   [Gold Nugget] [  empty  ] [Gold Nugget]
   ```
4. **Look for:** the output slot shows **Pet Amulet**. Take it.
5. Recipe book check: click the green book on the left of the crafting grid and type `amulet` in its search box.
   **Pet Amulet** is listed. It unlocks as soon as you hold a diamond or a gold nugget.

**A2. Fish Net**
1. `/give @s minecraft:string 4` and `/give @s mocreatures:sharkteeth 3`
2. Grid:
   ```
   [   empty   ] [ String ] [   empty   ]
   [Shark Teeth] [ String ] [Shark Teeth]
   [  String   ] [ Shark Teeth ] [  String  ]
   ```
3. **Look for:** output **Fish Net**. It's also in the recipe book (search `net`).
4. Survival note: Shark Teeth drop from sharks. The net now appears in the recipe book as soon as you pick up
   **string**, instead of only after your first Shark Teeth.

**A3. Use them**
1. **Pet Amulet:**
   - `/summon mocreatures:goat ~3 ~ ~` and `/give @s minecraft:wheat 3`
   - Hold wheat and right-click the goat. You get hearts and a naming screen; type a name and click Done. It's tamed.
   - Hold the **Pet Amulet** and right-click the goat. It vanishes in a puff; it's stored in the amulet.
   - Right-click the air with the amulet. The goat reappears in front of you with sparkles and the same name.
2. **Fish Net:**
   - Make a pool next to you: `/fill ~2 ~-2 ~2 ~5 ~-1 ~5 minecraft:water`
   - Put a fish in it: `/summon mocreatures:fishy ~3 ~-1 ~3 {TypeMoC:1}`
   - Right-click the fish with the empty **Fish Net**. The fish vanishes and the net now holds it.
   - Right-click the water. The fish is released.

**PASS if** both recipes craft, both show in the recipe book, and both items capture and release.
**FAIL if** either output slot stays empty, either is missing from the book, or capture/release does nothing.

---

## B. Wild manticores drop real Manticore Eggs

1. `/difficulty normal`
2. Summon 8 red (Nether-coat) manticores that stand still. Type this once, then press `↑` + Enter seven more times:
   ```
   /summon mocreatures:manticore ~4 ~ ~ {TypeMoC:1,NoAI:1b}
   ```
3. Kill them all: `/kill @e[type=mocreatures:manticore]`
4. **Look at the items on the ground.** About 1 in 4 kills drops a **Fire Manticore Egg**, a dark-red tinted egg; the
   name shows when you pick it up or hover it in your inventory. The other kills drop 0-2 **Chitin Nether** or
   **Nether Scorpion Sting**. With 8 kills you'll almost always get at least one egg. If you got none, repeat
   steps 2-3.
5. Other coats. Repeat with `TypeMoC:2`, `3` and `4`: dark gives **Dark Manticore Egg**, blue gives **Snow
   Manticore Egg**, and green gives **Manticore Egg**.

**PASS if** the eggs are named "… Manticore Egg" and match the coat.
**FAIL if** a **Pet Manticore Spawn Egg** ever drops, or 20+ kills give no egg.

---

## C. Hatching the Manticore Egg (no torch needed)

Keep a **Fire Manticore Egg** from B. If you don't have one:
`/give @s mocreatures:mocegg[minecraft:custom_data={EggType:62}]`

1. Stand on open grass in daylight, **with no torch anywhere nearby**. That's the point of this test.
2. Hold the egg and look at the **top of a grass block about 3 blocks in front of you**. Right-click. The egg
   appears sitting on that block.
3. **Step back 2-3 blocks and don't walk over it.** After about 10 seconds, walking into an egg picks it back up;
   that's the original "collect eggs by walking over them" rule. Stay within 24 blocks: the hatchling is tamed to
   the nearest player within 24 blocks.
4. Within about 5-10 seconds a chat line appears:
   **`Egg hatching soon! KEEP WATCH! The egg is at X, Y, Z.`**
5. Every few seconds the egg does a small hop.
6. About 20-45 seconds after placing it (30 s on average), the egg pops with the chicken-egg "plop" sound. A small
   **manticore cub** appears, and a **naming screen** opens. Type a name and click Done, or press Esc.
7. Verify (paste each):
   ```
   /data get entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] Tamed
   /data get entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] TypeMoC
   /data get entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] OwnerName
   ```
   Expected: `1b`, `1` (red coat from a Fire egg; 63→2 dark, 64→3 blue, 65→4 green), and your name.

**PASS if** it hatches within about a minute with no torch, shows the message, and gives a tamed cub whose coat
matches the egg.
**FAIL if** nothing happens after 2 minutes, the cub is wild (`Tamed` = `0b`), or the coat doesn't match the egg.

---

## D. Raising and riding your manticore

1. The cub grows by itself over about 20 minutes of play. To skip that, look at the cub and run:
   ```
   /data merge entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] {AgeMoC:130,Adult:1b,Age:0}
   ```
   It grows to full size, a big winged cat.
2. Saddle it: `/give @s minecraft:saddle`, then hold the saddle and right-click the manticore. A saddle appears on its
   back (in survival the saddle is used up; creative never uses up items). A cub below age 80 refuses the
   saddle, as in the original.
3. Mount it: right-click it with an **empty hand**.
4. Fly:
   - Look up above the horizon, a bit steeper than 25°, and hold `W`. It lifts off and flies where you look, beating
     its wings with a flap sound.
   - Look down and hold `W` to dive and land.
   - Let go of `W` in the air: it hovers and sinks slowly.
   - `A`/`D` strafe.
5. Dismount with `Shift`.
6. Heal it: right-click it with a raw porkchop or raw cod.

**PASS if** it saddles, you can mount it, and it flies and lands under your control.
**FAIL if** the saddle is refused on an adult, you can't mount it, or looking up + `W` never takes off.

---

## E. Taming a manticore cub with a Medallion (the creative / spawn-egg route)

1. `/give @s mocreatures:manticore_pet_spawn_egg`, `/give @s mocreatures:medallion 2`,
   `/give @s minecraft:porkchop 3` (raw porkchop)
2. Right-click the ground with the **Pet Manticore Spawn Egg**. A **wild cub** appears.
3. Hold the **Medallion** and right-click the cub. An action-bar hint appears above your hotbar:
   **"Pet Manticore won't take the medallion yet - drop a raw porkchop or raw cod on the ground near it and let it
   eat first"**. Nothing is used up, even in survival.
4. Stand 3-4 blocks from the cub, hold the raw porkchop, face the ground near it and press **`Q` once** to drop one
   porkchop.
5. Within a few seconds the cub walks over and **eats it**: an eating sound, and the porkchop disappears.
6. Right-click the cub with the Medallion again. Hearts appear and the naming screen opens (in survival the medallion is used up).
   Check it: `/data get entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] Tamed` should give `1b`.

Notes:
- The plain **Manticore Spawn Egg**, without "Pet", spawns the **wild monster** form. That form can never be tamed,
  exactly as in the original. Only its eggs and the "Pet Manticore" can be.
- A fully grown wild pet manticore can't be medallion-tamed either. The hint then says **"…is fully grown - only a
  cub that has eaten can be tamed with a medallion"**.

**PASS if** the hint shows before feeding, the cub eats the dropped porkchop, and the medallion tames it afterwards.
**FAIL if** there's no hint, the cub ignores the porkchop for 30+ s, or the medallion still fails after it has
eaten.

---

## F. Manticores now spawn naturally

At the normal spawn weight manticores are uncommon, about as rare as witches. So there are two ways to check.

**F1. Quick check (boosted spawns, recommended first):**
1. Close the game. Open this file in Notepad:
   `C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53\fabric\run\config\mocreatures.properties`
   Add this line at the very bottom, then save:
   ```
   spawn.manticore.frequency=60
   ```
2. Start the game again (`.\gradlew.bat :fabric:runClient`) and load `Round3`.
3. `/difficulty normal` and `/gamerule spawn_monsters true`
4. **Nether:** `/execute in minecraft:the_nether run spreadplayers 0 0 0 64 under 100 false @s`
   This drops you on a safe floor in the Nether. Fly around slowly for about 1 minute so new chunks load.
5. Count them: `/execute if entity @e[type=mocreatures:manticore]`. Chat says **`Test passed. Count: N`**, with N at
   least 1.
6. See them: `/effect give @e[type=mocreatures:manticore] minecraft:glowing 60 0 true`. Every manticore gets a
   white outline, visible through walls. Nether ones are the **red** coat:
   `/data get entity @e[type=mocreatures:manticore,limit=1,sort=nearest] TypeMoC` gives `1`.
7. **Overworld at night:**
   - `/execute in minecraft:overworld run spreadplayers 0 0 0 64 false @s`
   - `/time set midnight`
   - Fly around for 1-2 minutes, then repeat step 5.
   - Overworld manticores are the **dark (2)** or **green (4)** coat, or **blue (3)** in snowy biomes.
   - They don't spawn in daylight in the Overworld. If one survives into morning it smoulders but doesn't die; it
     was fire-immune in the original too.
8. **Afterwards, delete the `spawn.manticore.frequency=60` line** and restart, so spawns go back to normal.

**F2. Real rate (optional):** without the config line, explore the Nether for a few minutes and use step 5/6 now and
then. You should run into one eventually.

**PASS if** step 5 finds manticores in the Nether, and at night in the Overworld, with the right coats.
**FAIL if** the count is always 0 even with the boosted line.

---

## G. Pets roam instead of following you

1. `/summon mocreatures:goat ~3 ~ ~` and `/give @s minecraft:wheat 3`. Right-click the goat with wheat to tame it
   (hearts and naming screen).
2. `/summon mocreatures:bunny ~3 ~ ~1`. Right-click it with an **empty hand**: this tames it and it sits on your
   head. Press `Shift` to put it down.
3. Walk about 20 blocks away and wait 30 seconds, watching them (F5 helps).
   **Look for:** neither pet walks after you or teleports to you. They wander around where you left them.
4. **Whip:** `/give @s mocreatures:whip`. Hold it and right-click the goat.
   - The action bar says **"<name> stays put"**, with green sparkles, and the goat stops moving.
   - Right-click again: **"<name> is free to roam"**, with a cloud puff, and it wanders again.
5. **Rope (to bring a pet along):** `/give @s mocreatures:rope`. Right-click the tamed goat with the rope. A leash
   appears, and when you walk away the goat follows on it. A vanilla lead works the same way.

**PASS if** tamed pets stay in their area on their own, the whip toggles stay/roam, and a rope makes them follow.
**FAIL if** any pet trails after you without a leash, or teleports to you.

---

## H. Bat horse wings are visible

1. `/time set midnight` and `/difficulty easy`. Undead horses burn in daylight.
2. Hostile bat horse, frozen in place:
   `/summon mocreatures:horse_mob ~5 ~ ~ {TypeMoC:32,NoAI:1b,PersistenceRequired:1b}`
3. Tame bat horse, for comparison: `/summon mocreatures:horse ~-5 ~ ~ {TypeMoC:32,Tamed:1b,Adult:1b}`
4. Walk all the way around both, and look from above too (fly up).
   **Look for:** both show **dark, leathery bat wings folded down along each side of the body**, from every angle.
   The hostile one has **no thin black sticks/poles** poking up from its back.
5. Also look at the other hostile horses, which should look normal with no stray floating parts. Run these one at
   a time:
   ```
   /summon mocreatures:horse_mob ~5 ~ ~4 {TypeMoC:23,NoAI:1b,PersistenceRequired:1b}
   /summon mocreatures:horse_mob ~5 ~ ~8 {TypeMoC:26,NoAI:1b,PersistenceRequired:1b}
   /summon mocreatures:horse_mob ~5 ~ ~12 {TypeMoC:38,NoAI:1b,PersistenceRequired:1b}
   ```
   These are undead, skeleton and nightmare.
6. Let the bat horse move and check its legs stay joined, as in I:
   `/data merge entity @e[type=mocreatures:horse_mob,nbt={TypeMoC:32},limit=1,sort=nearest] {NoAI:0b}`.
   It will come at you, which is harmless in creative.
7. Clean up: `/kill @e[type=mocreatures:horse_mob]`

**PASS if** the wings are visible on both bat horses. **FAIL if** either is wingless, or the hostile one shows only
thin poles.

---

## I. Horse legs stay attached

1. `/time set day`
2. `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:3,Tamed:1b,Adult:1b,Saddled:1b}` (a tamed, saddled brown horse)
3. Right-click it with an **empty hand** to mount. Press `F5` once to see it from behind, then again for the front.
4. Walk forward with `W`, then sprint (hold `Ctrl` + `W`).
   **Look for:** each leg stays one connected piece at every point of the stride. The knees bend (the front lower
   legs fold back as they swing forward), but **no gap opens** between the upper leg and the lower leg or hoof.
5. **Rearing:** `Shift` off, then `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:3,Adult:1b,Saddled:1b}`. This one is
   **wild**. Mount it with an empty hand; it bucks and **rears up** trying to throw you.
   **Look for:** the front legs paw the air with the knees attached, and the back hooves stay on the ground rather
   than sunk into it.
6. **Flying legs:** `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:39,Tamed:1b,Adult:1b,Saddled:1b}` is a white
   pegasus. Mount it, look up and hold `W` to fly. **Look for:** the legs tuck back neatly in the air, still joined,
   and the wings beat.
7. `/kill @e[type=mocreatures:horse]` to clean up.

**PASS if** the legs never separate at the knee. **FAIL if** you see a gap, or a floating lower leg or hoof.

---

## J. Horse spawn egg gives the original (tier 2) coats

The coat numbers you'll see:

| Id | Coat |
|---|---|
| 6 | bright creamy (pale cream) |
| 7 | speckled (white with dark speckles) |
| 8 | pale brown |
| 60 | zebra |
| 1-5 | tier 1: white, creamy, brown, dark brown, black |
| 65 | donkey |

1. Fly up high (double-tap and hold `Space`). Run `/locate biome minecraft:forest`, click the green coordinates in
   the chat reply (that fills in a `/tp` command), and press Enter.
2. `/give @s mocreatures:horse_spawn_egg`. Right-click the ground with it about 10 times; in creative it isn't used
   up.
3. **Look for:**
   - The horses are **bright creamy, speckled, or pale brown**, roughly a third each.
   - An occasional **zebra**: about 1 in 30 with your current `zebraChance=2`; a fresh config now defaults to 10%.
   - About 1 in 5 is a small foal.
   - **No** plain white, black, brown or dark brown horses, and **no donkeys**.
   - Check any horse with `/data get entity @e[type=mocreatures:horse,limit=1,sort=nearest] TypeMoC`. It should be
     6, 7 or 8, or 60 for a zebra.
4. `/kill @e[type=mocreatures:horse]`, then do the same in a desert (`/locate biome minecraft:desert`). **Every**
   horse is a **zebra**.
5. In plains (`/locate biome minecraft:plains`), about **1 in 3** is a zebra.
6. Port addition, so white and black coats (and the breeding chains that need them) stay obtainable: in a meadow
   (`/locate biome minecraft:meadow`) you get the tier-1 coats 1-5 plus about 1 donkey in 6.
7. `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:5}` stays black. An explicit coat is never overridden.

**PASS if** forest eggs give only 6/7/8 plus rare zebras, deserts are all zebras, and plains are about ⅓ zebras.
**FAIL if** you get tier-1 coats or donkeys in a forest.

---

## K. Taming a zebra (the original rule)

The original rule: a wild zebra won't let anyone **on foot** handle it. You have to **ride up to it on a spotted
(16) or cow (17) horse, or a zebra or zorse**, and then **one apple** tames it.

1. `/summon mocreatures:horse ~8 ~ ~ {TypeMoC:60,Adult:1b}` (a wild zebra) and `/give @s minecraft:apple 3`
2. **On foot**, hold an apple and right-click the zebra.
   **Look for:** the action bar says **"The zebra shies away from anyone on foot - ride up to it on a spotted or cow
   horse (or a zebra or zorse), then give it an apple"**, the zebra **runs off about 8 blocks**, and **you don't
   eat your apple**.
3. `/summon mocreatures:horse ~ ~ ~2 {TypeMoC:16,Tamed:1b,Adult:1b,Saddled:1b}` (a tamed spotted horse). Right-click
   it with an **empty hand** to mount.
4. Ride right up next to the zebra; it no longer runs from you. Select the apple, aim at the zebra and right-click.
5. **Look for:** hearts and a naming screen for the zebra after a **single** apple (in survival exactly one
   apple is used up). Check:
   `/data get entity @e[type=mocreatures:horse,nbt={TypeMoC:60},limit=1,sort=nearest] Tamed` gives `1b`.
6. Survival route to a spotted or cow horse: breed up the tiers (tier 3 × tier 3 gives tier 4), or use a zebra or
   zorse you already own.

**PASS if** on foot you get the hint and it bolts, and from a spotted horse one apple tames it.
**FAIL if** it still can't be tamed from the saddle, or takes more than one apple.

---

## L. Wyvern eyes sit in the face

1. `/summon mocreatures:wyvern ~4 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100}` (tamed, so it stands still)
2. Walk slowly around its head: from the side, the front corner, and a little above.
   **Look for:** each eye is **painted in its socket**, so you see the eye itself in the recessed hole. You should
   **never see through** the head, and never a wrong or mirrored eye deep inside it.
3. Try other skins: `/data merge entity @e[type=mocreatures:wyvern,limit=1,sort=nearest] {TypeMoC:4}` (then 5, 6,
   7, 8).
4. Also look for:
   - The jaw drops **open downward** every few seconds; it used to swing up through the snout.
   - There's **no metal bar through its mouth** unless it's saddled.

**PASS if** the eyes are visible and solid from all angles. **FAIL if** there's a see-through hole or a missing eye.

---

## M. Wyvern wings open and beat in flight

1. **Wild, falling:** `/summon mocreatures:wyvern ~ ~15 ~ {TypeMoC:4,Adult:1b,AgeMoC:100}`
   **Look for:** as it drops, the **wings spread wide and beat** with a flap sound; they aren't folded against its
   sides. On landing they fold back. Now and then it takes off on its own, and while cruising the wings stay open.
2. **Ridden:** `/summon mocreatures:wyvern ~3 ~ ~ {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}`
   Right-click it with an **empty hand** to mount, then press `F5`.
   - **Standing still on the ground:** wings folded and legs standing. Before, it looked mid-flight while parked.
   - **Look up and hold `W`:** it takes off. The wings **open and beat** and the neck stretches out forward.
   - **Let go of `W` in the air:** it hovers, wings open with slower beats. Flying fast beats more often.
   - **Look down and hold `W`** to land: the wings **fold** on touchdown.
   - Optional: Options → Controls → Key Binds → bind **"Mo'Creatures Mount Jump"**. Pressing it in the air gives an
     instant wing beat.
   - `Shift` to get off. The reins disappear when nobody is riding.

**PASS if** the wings are open whenever it's in the air, and folded on the ground.
**FAIL if** the wings stay folded while flying or falling.

---

## N. Kitty beds

1. Get beds and supplies (one command per line):
   ```
   /give @s mocreatures:kittybed_red
   /give @s mocreatures:kittybed_light_blue
   /give @s mocreatures:kittybed
   /give @s mocreatures:petfood 4
   /give @s minecraft:milk_bucket
   /give @s minecraft:iron_pickaxe
   ```
   To craft one in survival:
   ```
   [Planks] [Planks] [Planks]
   [Planks] [ Wool ] [Planks]
   [ Iron ] [ empty] [ empty]
   ```
   Planks can be any wood; Iron is an iron ingot. The wool colour is the bed colour. Any kitty bed plus a dye
   re-colours it.
2. **Colour + placement:** look at the **top of a grass block** 2 blocks away and right-click with the **Red Kitty
   Bed**.
   **Look for:**
   - It sits **flat on the ground**, not floating.
   - The **wooden frame is plain wood colour**.
   - The **cushion inside is red**. It's a slightly deep red: the original drew the wool at reduced brightness.
   - The ground doesn't show through the bottom.
   - The open side with the food bowl faces you.
3. Place the **Light Blue** bed: light-blue cushion. The plain **Kitty Bed**: white/light-grey cushion.
4. **Falls to the ground:**
   - Jump and, while in the air, right-click the **air** with a bed. It lands on the ground at your feet.
   - Right-click the **side** of a wall block. It drops down to the ground.
   - `/summon mocreatures:kitty_bed ~ ~4 ~ {SheetColour:15}` falls and is red.
5. **Filling:**
   - Right-click a bed with the **Milk Bucket**: white milk appears in the bowl and you get the empty bucket back.
   - Right-click another bed with **Pet Food**: brown food appears in the bowl.
6. **Breaking:**
   - `/gamemode survival`, then **punch** the red bed once. It breaks with a pop and **drops a "Red Kitty Bed"
     item**. Pick it up and place it again: still red.
   - Right-click a bed with the **iron pickaxe**: it goes straight into your inventory.
   - `/gamemode creative`, then punch a bed: it just disappears with no drop, like creative block-breaking.
   - `/kill @e[type=mocreatures:kitty_bed]` removes all beds.
7. **Carry:** right-click a bed with an **empty hand**: it goes on your head. Press `Shift`: it lands on the ground,
   not hanging in the air.
8. **Litter box:** `/give @s mocreatures:kittylitter`. Place it: it sits on the ground. Punch it in survival: it
   drops a Litter Box.
9. Optional, kitty uses the bed: `/summon mocreatures:kitty ~2 ~ ~ {Tamed:1b,Adult:1b,AgeMoC:100}` next to a bed
   filled with milk or food, then `/time set midnight` and wait about a minute. A sleepy or hurt tamed kitty climbs
   in.

**PASS if** the colour shows, it sits on the ground, and punching breaks it and returns the coloured item.
**FAIL if** there's no coloured cushion, it floats, or punching does nothing.

---

## O. Bunnies hop

1. `/time set day` on a flat grass area.
2. `/summon mocreatures:bunny ~3 ~ ~`. Run it 3 times, moving a bit between them.
3. **Standing still:** its feet touch the grass, with no gap under the bunny. F3+B shows the hitbox floor.
4. **Wandering:** wait a few seconds for them to move.
   **Look for:**
   - They **bound in little hops**, about 0.7 block high every ¾ second, instead of sliding.
   - Nose up on the way up, nose down on the way down, back feet stretched in the air.
   - When a bunny stops, the hopping stops.
5. **Fleeing:** punch one once. It panics and bounds away in longer hops.
6. **Carried:** right-click one with an empty hand; it tames it and puts it on your head. Press F5: on your head it
   **doesn't bounce or tilt** and its legs stay still. Press `Shift` to set it down, and it starts bounding again.
7. **Told to stay:** whip the tamed bunny (`/give @s mocreatures:whip`) so it's "stays put". It doesn't hop, even
   if you push it. Whip again ("free to roam") and it hops again.
8. **Baby:** `/summon mocreatures:bunny ~2 ~ ~ {TypeMoC:3,Adult:0b,AgeMoC:10}` is a small bunny that hops the same
   way.

**PASS if** moving bunnies visibly bound and still ones sit on the grass. **FAIL if** they still slide, or float
above the ground.

---

## P. NeoForge spot check (optional)

The code is shared, so a short pass is enough:
```
.\gradlew.bat :neoforge:runClient
```
Repeat **C** (egg hatch), **H** (bat horse wings), **M1** (wyvern falling with open wings) and **O** (bunny hops).

---

### When you're done
Tell me the section and step of anything that failed. If you'd rather the zebra were tameable on foot, or the
kitty-bed wool were brighter than the original's, say so; both are one-line changes.
