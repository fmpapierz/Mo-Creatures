# Test — Round 4 (2026-09-26): only the things you reported

Each section ends with **PASS if / FAIL if**. If something fails, tell me the section and step number and what
you saw.

| # | You reported | Cause | Fix |
|---|---|---|---|
| 1 | Pet Amulet in creative: the animal vanishes and never comes back | In creative, Minecraft gives the item a throwaway *copy* of your held stack (so creative never uses items up). The amulet saved the animal into that copy | The animal is saved into the amulet actually in your hand. Amulets no longer stack (one pet per amulet, as in the original), and a wild animal gets a message instead of nothing |
| 2 | Manticore on the ground: wings flap and "lag like crazy" | Flight mode was keyed on Minecraft's "on ground" flag, which 26.2 reports wrongly for a gravity-free mount standing still. The manticore flipped between flying and landed every tick | Flight and landing now check for actual ground under the feet |
| 2 | Manticore hovering: wings don't flap, and it doesn't sink | While a player rides a mob, Minecraft skips that mob's AI step, where the wing-beat timer lived. Separately, the hover code never actually sank | Wing timer moved to a step that still runs while ridden. Hovering now glides slowly down like the original (about 2-3 blocks/s) with the wings beating |
| 3 | No hint; the cub won't eat raw porkchop or cod | The cub had to be within 1.5 blocks to eat, but its 1.4-block-wide body makes the pathfinder stop about that far from the food, so it stood next to it forever. (The Pet Amulet on a wild cub also said nothing) | Eating range is now the original 2 blocks (the big cat had the same bug). Both the Medallion and the Pet Amulet now explain themselves |
| 4 | Rider sits over the horse's head, not on the saddle | The horse had no seat position, so Minecraft seated you at the top-centre of its hitbox: too high, and a quarter-block too far forward | Seat is now on the saddle: a quarter-block back and lower, as in the original (hostile horses' skeleton riders too) |
| 5 | Can't sprint on a horse | Minecraft lets you sprint on a mount only if it opts in (vanilla's camel does); no Mo'Creatures mount did | Every rideable Mo'Creature can now be sprinted: 1.3× speed on the ground and in the air |
| 6 | Wyvern hovering: wings don't flap | Ridden wyverns used the original's "lazy when hovering" beat rate: still wings for most of each second | Airborne wyverns now beat almost continuously. Like the manticore, a hovering wyvern also glides slowly down now, as in the original |
| 7 | Kitty floats above its bed | A kitty in the bed sat on top of the bed's hitbox, level with the rim | The kitty now sits on the cushion, inside the walls |

The pegasus-type horses share the manticore's flight code, so they got the same landing and hover fixes. They're
covered at the end of section 2.

---

## 0. Setup

1. The test client from the fixed copy is started for you. If you closed it, open PowerShell and run:
   ```
   cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
   .\gradlew.bat :fabric:runClient
   ```
2. Load your test world, or create one: Creative, **Allow Commands: ON**.
3. Type these once (`T` opens chat):
   ```
   /time set day
   /gamerule advance_time false
   /difficulty normal
   ```
4. Keys used below:
   - `F5`: third-person view.
   - `Shift`: get off a mount.
   - Sprint: `Ctrl`, or double-tap `W`.
   - `Q`: drop the held item.

---

## 1. Pet Amulet works in creative

1. `/gamemode creative`
2. `/give @s mocreatures:petamulet 2`
   **Look for:** the two amulets go into **two separate slots**; they no longer stack.
3. `/summon mocreatures:goat ~3 ~ ~` and `/give @s minecraft:wheat 2`. Hold the wheat and right-click the goat:
   hearts appear and a naming screen opens. Type `Billy` and click Done. The goat is tamed.
4. Hold one **Pet Amulet** and right-click Billy.
   **Look for:** a grey puff, and Billy disappears into the amulet.
5. Keep holding the **same amulet** and right-click the ground about 3 blocks in front of you.
   **Look for:** sparkles, and **Billy comes back** with his name tag.
6. Do steps 4 and 5 again: capture and release work every time.
7. **Wild animal:** `/summon mocreatures:goat ~3 ~ ~2` (not tamed). Right-click it with an empty amulet.
   **Look for:** an action-bar message above your hotbar: **"Only a tamed pet fits in the Pet Amulet - tame it
   first"**. The goat stays where it is.
8. **Survival check:** `/gamemode survival`, then repeat steps 4 and 5 once with Billy. Then `/gamemode creative`.

**PASS if** Billy comes back in creative. **FAIL if** he's lost after capture, or the amulets stack.

---

## 2. Riding the manticore: ground, hover, flight

1. Summon a grown, tamed, saddled pet manticore:
   ```
   /summon mocreatures:manticore_pet ~3 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:130,Saddled:1b}
   ```
2. Right-click it with an **empty hand** to get on. Press `F5` once to see it from behind.
3. **On the ground:**
   - Stand still for 10 seconds, then walk around with `W` and turn.
   - Look up a little (not steeply) and down.
   **Look for:** the wings stay **folded along its back**. No flapping, no flickering, no jitter while standing
   or walking.
4. **Take off:** look **steeply up** (well above the horizon) and hold `W`.
   **Look for:** it lifts off, the wings open and start beating (with a flapping sound), and the legs tuck back.
5. **Hover:** at about 10+ blocks up, **let go of all keys** and watch for 10 seconds.
   **Look for:**
   - The wings keep **beating** almost all the time.
   - The manticore **slowly glides down**, about 2-3 blocks per second, like when you dismount it in the air.
   - When it reaches the ground it lands and the wings fold, with no jitter.
6. **Fly around:** take off again and fly in circles. The wings keep beating. Hold `Ctrl` (sprint) while flying
   forward: it flies noticeably faster.
7. **Land:** look down and hold `W` until it touches down. The wings fold as soon as it lands.
8. `Shift` to get off.
9. **Pegasus (same flight code), quick check:**
   - `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:39,Tamed:1b,Adult:1b,Saddled:1b}`. Mount it with an empty hand.
   - On the ground: wings folded, no flicker.
   - Look steeply up + `W`: it flies with its wings beating.
   - Let go in the air: it glides slowly down and lands.

**PASS if** the wings are calm on the ground, beat while hovering, and a hovering mount slowly sinks and lands.
**FAIL if** the wings flicker on the ground, stay still while hovering, or it hangs in the air without sinking.

---

## 3. Taming a manticore cub with the Medallion

Use the **Medallion**: the gold disc on a leather strap, item name "Medallion". *Not* the Pet Amulet.

1. `/give @s mocreatures:manticore_pet_spawn_egg`, `/give @s mocreatures:medallion 2`,
   `/give @s minecraft:porkchop 3` (raw porkchop), `/give @s minecraft:cod 3` (raw cod)
2. Right-click the ground with the **Pet Manticore Spawn Egg**. A **wild cub** appears (smaller than an adult).
3. Hold the **Medallion** and right-click the cub.
   **Look for:** a message above your hotbar: **"Pet Manticore won't take the medallion yet - drop a raw porkchop or
   raw cod on the ground near it and let it eat first"**.
4. Stand 3-5 blocks from the cub, hold the **raw porkchop**, face the ground next to the cub and press **`Q`
   once**. One porkchop drops.
5. Within about 10 seconds the cub **walks to the porkchop and eats it**: you hear an eating sound and the porkchop
   disappears.
6. Right-click the cub with the **Medallion** again.
   **Look for:** hearts, and the naming screen opens. Check with
   `/data get entity @e[type=mocreatures:manticore_pet,limit=1,sort=nearest] Tamed`, which should give `1b`.
7. **Raw cod:** spawn a second cub with the spawn egg and repeat steps 4-6, dropping **raw cod** instead. It eats
   that too.
8. Pet Amulet on a wild cub: right-click an untamed cub with the Pet Amulet. The message says **"Only a tamed pet
   fits in the Pet Amulet - tame it first"**.

**PASS if** the medallion hint shows, the cub eats both raw porkchop and raw cod, and it's tamed afterwards.
**FAIL if** there's no hint, or the cub ignores the dropped food for 20+ seconds.

---

## 4. Sitting on the horse's saddle

1. `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:3,Tamed:1b,Adult:1b,Saddled:1b}`
2. Right-click it with an **empty hand** to mount. Press `F5` once (behind), then again (front).
   **Look for:** you sit **on the saddle in the middle of the back**, legs down its sides. You are **not** up by
   the neck or head, and not floating above the horse.
3. Walk around: you stay on the saddle.
4. Also check a donkey: `/summon mocreatures:horse ~3 ~ ~ {TypeMoC:65,Tamed:1b,Adult:1b,Saddled:1b}`. You sit on
   its (smaller) saddle too.
5. Optional, hostile horse at night: `/time set midnight`, then
   `/summon mocreatures:horse_mob ~5 ~ ~ {TypeMoC:23,NoAI:1b,PersistenceRequired:1b}` and
   `/summon minecraft:skeleton ~5 ~ ~`. Within a few seconds the skeleton climbs on and sits on the horse's back.

**PASS if** you're seated on the saddle. **FAIL if** you still sit forward over the neck or float above the back.

---

## 5. Sprinting on a horse

1. Mount the tamed horse from section 4 and face a long flat stretch.
2. Hold `W` for a few seconds (normal speed).
3. Keep `W` held and press `Ctrl` (or double-tap `W`).
   **Look for:**
   - The horse **speeds up** noticeably (about 30% faster).
   - Your view widens slightly (sprint FOV).
   - Letting go of `W` stops the sprint.
4. Works on other mounts too: an ostrich, elephant, wyvern or manticore can also be sprinted.

**PASS if** sprinting makes the horse faster. **FAIL if** nothing changes when you press sprint.

---

## 6. Wyvern hovering: wings keep flapping

1. `/summon mocreatures:wyvern ~3 ~ ~ {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}`
2. Right-click with an **empty hand** to mount, then press `F5`.
3. Look up and hold `W` to climb 10+ blocks.
4. **Let go of all keys** and watch for 10 seconds.
   **Look for:** the wings **keep beating** almost all the time (flap sound), and it slowly glides down, like the
   manticore, until it lands.
5. Fly around: the wings beat. Land: the wings fold.

**PASS if** the wings beat while hovering. **FAIL if** they hold still while you hover.

---

## 7. Kitty sits inside its bed

1. `/time set midnight`
2. `/give @s mocreatures:kittybed_red` and `/give @s minecraft:milk_bucket`
3. Right-click the top of a grass block to place the bed, then right-click the bed with the **milk bucket**. White
   milk appears in its bowl.
4. Summon a tamed kitty right next to it:
   ```
   /summon mocreatures:kitty ~1 ~ ~1 {Tamed:1b,Adult:1b,AgeMoC:100}
   ```
5. Wait up to about a minute. At night a tamed kitty gets sleepy and **climbs into the bed**. Stand still nearby;
   F5 helps.
   **Look for:** the kitty lies curled **on the red cushion, inside the bed's walls**. It is not floating above the
   rim.
6. If it hasn't climbed in after a minute, send it to bed straight away. A hurt tamed kitty always goes to a
   stocked bed to heal:
   `/damage @e[type=mocreatures:kitty,limit=1,sort=nearest] 2`
   Within a few seconds it walks over and climbs in. It stays until it has healed, then may hop out; that's
   normal.

**PASS if** the kitty sits inside the bed on the cushion. **FAIL if** it hovers above the bed.
