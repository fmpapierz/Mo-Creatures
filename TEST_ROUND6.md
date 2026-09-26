# Test — Round 6 (2026-09-26): fish-bowl plants and wyverns (eggs only)

The game is already open with these fixes. If you close it, restart it by running this in PowerShell:
```
cd "C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader\.claude\worktrees\mo-creatures-spawn-textures-097d53"
.\gradlew.bat :fabric:runClient
```

---

## 1. Fish bowl clears the plant it sits in

1. Creative. Find grass with plants, or make some: `/fill ~1 ~ ~1 ~4 ~ ~4 minecraft:short_grass`, and put a
   flower next to it: `/setblock ~5 ~ ~1 minecraft:poppy`.
2. `/give @s mocreatures:fishbowl_1`. Right-click **a short-grass tuft** with it. Clicking a plant now puts the
   bowl in that plant's spot; before, it did nothing.
   **Look for:** the tuft **breaks** (grass-breaking particles) and the bowl sits there with no grass showing
   inside the glass.
3. Right-click the **poppy** with a bowl: the poppy breaks and **drops as an item** you can pick up.
4. Placing on bare ground still works: right-click the top of a plain block. The bowl sits on it as before.
5. **Kitty bed and litter box:** right-click a grass tuft with `mocreatures:kittybed_red`, and another with
   `mocreatures:kittylitter`. The grass breaks, so nothing pokes through them.

**PASS if** no plant is left inside a placed bowl, bed or box. **FAIL if** grass or flowers still show through.

---

## 2. Wyverns: eggs only (exactly like 12.0.5)

**What changed:**
- A **wild wyvern can no longer be saddled or ridden.** The only way to get one is to **hatch an egg**, exactly as in
  12.0.5.
- Trying to saddle a wild one shows a message and keeps your saddle.
- Separately, the port bug that launched you into the air when a mount bucked you off is fixed. That still matters
  for wild horses.

**Why you died before:** the old 5.1.5-style break-in let a wild wyvern buck you off about 16 times. Each throw
launched you into the air (fall damage, which armor doesn't reduce), and it bit you and poisoned you in between.
With eggs only, you never climb onto a wild wyvern at all.

### 2a. A wild wyvern refuses a saddle
```
/summon mocreatures:wyvern ~4 ~ ~ {TypeMoC:1,Adult:1b,AgeMoC:100,NoAI:1b}
/give @s minecraft:saddle
```
Right-click the wyvern with the saddle.
**Look for:** an action-bar message: **"A wild wyvern won't take a saddle - hatch a wyvern egg to raise your
own"**. The saddle stays in your hand.

### 2b. A wild wyvern can't be ridden even if it already has a saddle
```
/summon mocreatures:wyvern ~4 ~ ~4 {TypeMoC:1,Adult:1b,AgeMoC:100,Saddled:1b,NoAI:1b}
```
Right-click it with an **empty hand**. **Look for:** nothing happens; you do **not** get on.

### 2c. Wild wyverns drop eggs in the Wyvern Lair
```
/execute in mocreatures:wyvern_lair run spreadplayers 0 0 0 64 false @s
/give @s minecraft:netherite_sword[minecraft:enchantments={"minecraft:sharpness":255}]
/summon mocreatures:wyvern ~3 ~ ~ {TypeMoC:5,Adult:1b,AgeMoC:180,NoAI:1b}
```
1. The first command takes you to the Wyvern Lair.
2. Summon about 6 mother wyverns: press `↑` + Enter on the summon command.
3. Hit each once with the sword; one hit kills.
**Look for:** about **1 in 3** mothers drop a **Mother Wyvern Egg**. Ordinary wyverns (`TypeMoC` 1–4) drop one
about 1 in 10. Eggs only drop in the Lair, and only from a wyvern you killed.

### 2d. Hatch the egg: a tamed wyvern
1. If you didn't get one from 2c: `/give @s mocreatures:mocegg[minecraft:custom_data={EggType:54}]`
   (Mother Wyvern Egg; 50–53 are Jungle / Swamp / Sand / Savanna).
2. Right-click the top of a block about 3 blocks away, step back, and wait about 30 s.
   **Look for:**
   - An "Egg hatching soon!" message and the egg hopping.
   - A **baby wyvern** hatches and a naming screen opens.
   - `/data get entity @e[type=mocreatures:wyvern,limit=1,sort=nearest] Tamed` gives `1b`.
3. Skip the growing (mothers are adults at 180, the others at 100):
   `/data merge entity @e[type=mocreatures:wyvern,limit=1,sort=nearest] {AgeMoC:180,Adult:1b,Age:0}`
4. `/give @s minecraft:saddle`, right-click it: it **takes the saddle**. Right-click with an empty hand: you get on
   and fly it (look up + `W`). No bucking; it's yours.

**PASS if** wild wyverns refuse saddles and riders, the Lair drops eggs, and a hatched wyvern is tamed and rideable.
**FAIL if** you can saddle or ride a wild wyvern, or a hatched one isn't tamed.

---

## 3. Hidden painted details (model scan)

Moved to **TEST_ROUND7.md, section 2**. Round 7 also covers the wyvern flight speed.
