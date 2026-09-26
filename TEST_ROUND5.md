# Test — Round 5 (2026-09-26): only what was still broken

Each section ends with **PASS if / FAIL if**. If something fails, tell me the section and step number and what
you saw.

| # | You reported | Cause | Fix |
|---|---|---|---|
| 1 | No grey puff when capturing with the Pet Amulet | The puff used a custom sprite that is almost empty (a 21-pixel speck), so 20 of them were invisible. The original showed no effect at all | Capture now makes Minecraft's own grey "poof" (the smoke when a mob vanishes), spread over the animal's body. Same for the Fish Net |
| 2 | Manticore wings beat, stop, then beat again | Each 20-tick beat started on a random roll, as in the original, which leaves pauses | While airborne, each beat now rolls straight into the next, so the wings beat steadily like the pegasus. Also applies to the wild manticore |
| 3 | Wyvern wings stop while hovering | Same random-roll gaps | Same fix: steady beating whenever it's in the air, hovering included |
| 4 | Kitty still slightly above the bed | When sitting or sleeping, the original model was lowered 4 pixels (legs 5) so the folded legs lie flat. The port copied the folded legs but not the lowering, so every sitting kitty floated about 5 pixels up | Lowering restored. This also fixes a kitty sitting on the ground (whip "stay") |
| 5 | Wild wyvern bites its rider | Its hunting code picked the nearest player, including the one riding it. The original stopped all hunting while the wyvern was ridden or sitting | Same rule restored |
| 6 | Scan all models for hidden painted details | 1.12 drew both sides of every face on a mob; the port hides back faces, which made the wyvern's eyes vanish | Every model was scanned; results and checks are in section 6 |

---

## 0. Setup

1. The test client from the fixed copy is started for you. If it isn't open, run in PowerShell:
   ```
   cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
   .\gradlew.bat :fabric:runClient
   ```
2. Load your test world (Creative, commands on), then:
   ```
   /time set day
   /gamerule advance_time false
   /difficulty normal
   ```

---

## 1. Grey puff when capturing

1. `/give @s mocreatures:petamulet`, `/summon mocreatures:goat ~3 ~ ~` and `/give @s minecraft:wheat`. Right-click
   the goat with wheat to tame it (hearts and naming screen).
2. Hold the **Pet Amulet** and right-click the goat.
   **Look for:** a **grey smoke puff** over where the goat stood, the same puff you see when a mob dies and
   vanishes, and the goat is gone.
3. Right-click the ground to release it: sparkles, and the goat is back.
4. Optional, Fish Net: `/give @s mocreatures:fishnet`, then make a pool with
   `/fill ~2 ~-2 ~2 ~5 ~-1 ~5 minecraft:water` and add a fish with
   `/summon mocreatures:fishy ~3 ~-1 ~3 {TypeMoC:1}`. Right-click the fish with the net: the same grey puff.

**PASS if** you clearly see the grey puff. **FAIL if** the goat just vanishes with no puff.

---

## 2. Manticore wings beat steadily in the air

1. `/summon mocreatures:manticore_pet ~3 ~ ~ {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:130,Saddled:1b}`
2. Right-click it with an **empty hand** to mount, then press `F5` to watch from behind.
3. Look steeply up and hold `W` to climb 15+ blocks, then **let go of every key** and watch for at least 10
   seconds while it glides slowly down.
   **Look for:**
   - The wings flap **steadily the whole time**, with no pauses where they hold still.
   - The flap sound repeats about once a second.
4. Fly around in circles: still steady flapping, no pauses.
5. Land (look down + `W`): the wings fold as soon as it touches the ground, and stay folded while you stand or
   walk.
6. Wild manticore, optional:
   - `/summon mocreatures:manticore ~ ~15 ~ {TypeMoC:2}` drops from 15 blocks up.
   - While it's in the air its wings beat steadily. It attacks you in survival only; stay in creative.

**PASS if** the wings never stop beating while it's airborne. **FAIL if** you still see pauses in mid-air.

---

## 3. Wyvern wings beat steadily while hovering

1. `/summon mocreatures:wyvern ~3 ~ ~ {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,Saddled:1b}`
2. Mount with an **empty hand**, then press `F5`.
3. Look up and hold `W` to climb 15+ blocks, then **let go of every key** for 10 seconds.
   **Look for:** the wings **flap steadily without stopping** while it hovers and slowly glides down.
4. Fly around: steady flapping. Land: wings fold.

**PASS if** the flapping never pauses while it's in the air. **FAIL if** the wings hold still while hovering.

---

## 4. Kitty sits inside its bed

1. `/time set midnight`
2. `/give @s mocreatures:kittybed_red` and `/give @s minecraft:milk_bucket`. Place the bed on grass, then right-click
   it with the milk bucket (milk appears in the bowl).
3. `/summon mocreatures:kitty ~1 ~ ~1 {Tamed:1b,Adult:1b,AgeMoC:100}`
4. Send it to bed right away (a hurt tamed kitty goes to a stocked bed to heal):
   `/damage @e[type=mocreatures:kitty,limit=1,sort=nearest] 2`
   Or just wait up to a minute at night for it to get sleepy.
5. When it's in the bed, look closely from the side (F5 helps).
   **Look for:** the kitty lies curled **down on the red cushion, inside the walls**: legs flat on the cushion,
   no gap underneath.
6. Kitty sitting on the ground (same fix):
   - `/give @s mocreatures:whip` and right-click the tamed kitty with it ("stays put").
   - It sits with its legs flat on the ground, not floating.
   - Whip again to let it roam.

**PASS if** there's no gap between the kitty and the cushion (or the ground). **FAIL if** it still floats above
either.

---

## 5. A wild wyvern doesn't bite its rider

1. Stay in **creative** for now; wild wyverns ignore creative players, so you can get on safely.
2. Summon a **wild**, saddled adult wyvern:
   `/summon mocreatures:wyvern ~3 ~ ~ {TypeMoC:1,Adult:1b,AgeMoC:100,Saddled:1b}`
3. Right-click it with an **empty hand** to climb on.
4. While riding, switch to survival and give yourself a safety net:
   `/gamemode survival`, then `/effect give @s minecraft:resistance 300 1 true`
5. Stay on it for about 30 seconds. A wild wyvern bucks while you break it in: it hops, jerks sideways, and
   sometimes throws you off. If it throws you, go back to creative, get on, and switch to survival again. If it
   gets tamed (hearts and naming screen), summon a fresh one and repeat.
   **Look for:** it bucks, but **never bites you** while you're on its back: no bite damage, no poison, no hurt
   sound.
6. Get off (`Shift`) and step away in survival: it's allowed to come after you again, as a wild wyvern should.
7. `/gamemode creative` when done.

**PASS if** it never bites you while you ride it. **FAIL if** you take bite damage while mounted.

---

## 5b. Fish bowl falls and can be removed

Cause: the fish bowl was flagged "no AI", which in 26.2 also switches off gravity, and it ignored all damage,
`/kill` included. The same bug the kitty bed had.

1. `/gamemode creative`, then `/give @s mocreatures:fishbowl_1` (a bowl with a blue fish) and
   `/give @s minecraft:iron_pickaxe`
2. **Falls to the ground:** stand next to a wall, then right-click the **side** of a block about 2 blocks above
   the floor with the fish bowl.
   **Look for:** the bowl appears beside the block and **drops to the floor**; before, it hung in the air. The
   fish keeps swimming inside.
3. **Sits still:** walk into it. It doesn't slide around.
4. **Breaks with a punch:**
   - `/gamemode survival`, then punch the bowl once. It pops and drops a **"Fish Bowl (Fishy)"** item.
   - Pick it up and place it on the floor again: the same fish is still inside.
   - `/gamemode creative`, then punch a bowl: it disappears with no drop, like creative block-breaking.
5. **Pickaxe still works:** right-click a placed bowl with the iron pickaxe. It goes straight into your inventory.
6. **`/kill` works:** place a bowl, then `/kill @e[type=mocreatures:fish_bowl]`. It's removed.
7. Old worlds: any bowl that was already floating drops to the ground the next time the world loads.

**PASS if** bowls fall, stay put, and punching or `/kill` removes them (punching drops the bowl with its fish).
**FAIL if** a bowl floats or can't be removed.

---

## 6. Hidden painted details (model scan)

Moved to **TEST_ROUND7.md, section 2**.
