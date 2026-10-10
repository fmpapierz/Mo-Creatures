# Player reports round (2026-10-09/10): what to test

Covers the fixes made for the first player reports: horse taming, MoC x vanilla horse breeding, the kitty bed, the
scorpion model, led animals on steps, and spawning (no animal cap, 12.0.5 weights, 12.0.5 biome placement, insects
back in the ambient pool).

## Setup

Launch the Fabric dev client into the prepared test world (a creative world with test shortcuts):

```
.\gradlew.bat :fabric:runClient "--args=--username MoCTester --quickPlaySingleplayer MoC_Player_Reports_Test"
```

- The world is `fabric/run/saves/MoC_Player_Reports_Test`. Creative, cheats on.
- The username `MoCTester` matters: the test pets are owned by `MoCTester`, so the kitty's mood icon shows.
- If an "Experimental Settings" screen appears, accept it.
- Chat lists the shortcuts on joining. Each `/function moctest:...` builds a fenced grass pen around you and puts
  the test animals and items in it. Run each in open ground; `/function moctest:guide` shows the list again.

## 1. Horse taming (Mo'Creatures and vanilla)

`/function moctest:horses`: left is a wild Mo'Creatures horse, right is a wild vanilla horse. Gives apples,
saddles, a MoC horse saddle and golden carrots.

| Step | Do | Expected |
|---|---|---|
| 1a | Empty hand, right-click the **Wild MoC horse** | Action bar: "Wild MoC horse won't let you on bareback - give it an apple to tame it, or saddle it and ride it until it stops bucking". You do not mount. |
| 1b | Right-click it with an **apple** | Hearts, the naming screen opens. It is tamed. Saddle it, then right-click empty-handed to ride. |
| 1c | `/function moctest:horses` again. Right-click the new wild MoC horse with a **saddle**, then right-click it empty-handed | You mount. It bucks you off now and then; keep remounting until it stops (hearts): tamed by riding. Wheat, a sugar lump or bread shorten this. |
| 1d | Empty hand, right-click the **Wild vanilla horse** | You mount it (vanilla). Keep remounting until hearts appear: vanilla taming works with the mod installed. |
| 1e | `/summon mocreatures:horse ~ ~ ~3 {TypeMoC:2,Adult:0b,AgeMoC:10}`, empty-hand right-click the foal | "... is too young to ride - give it an apple to tame it". |
| 1f | `/summon mocreatures:horse ~ ~ ~3 {TypeMoC:36,Adult:1b,AgeMoC:100}` (a unicorn), empty-hand right-click | "... won't take food - once it is grown, saddle it and ride it until it stops bucking". |

## 2. Breeding a Mo'Creatures horse with a vanilla horse

`/function moctest:crossbreed`: a tamed black MoC horse and a tamed black vanilla horse, a pumpkin and a golden carrot.

| Step | Do | Expected |
|---|---|---|
| 2a | Pumpkin to the **MoC black horse**, golden carrot to the **Vanilla black horse** | Both show hearts. Within a few seconds the MoC horse walks to the vanilla one and a foal appears. |
| 2b | `/data get entity @e[type=mocreatures:horse,nbt={Adult:0b},sort=nearest,limit=1] TypeMoC` | `5` (black, from black x black). The foal is a tamed Mo'Creatures foal. |
| 2c | Feed only one of the two | No foal: both need to be in love. Vanilla horses need to be tamed to take the golden carrot. Donkeys don't cross (as in 12.0.5). |

## 3. Kitty bed

`/function moctest:kitty`: a hungry tamed kitty, an empty kitty bed, pet food and a milk bucket.

| Step | Do | Expected |
|---|---|---|
| 3a | Look at the kitty | A food-bowl icon above its name (hungry). |
| 3b | Right-click the bed with **pet food** | The bowl in the bed fills. Within a few seconds the kitty walks over, climbs in and makes an eating sound; its icon becomes a happy cat face. |
| 3c | Wait about 35 seconds | The bowl empties, the kitty climbs out, its icon is gone (fed). `/data get entity @e[type=mocreatures:kitty,sort=nearest,limit=1] KittyHunger` shows a small number. |
| 3d | Repeat with the **milk bucket** (run `/function moctest:kitty` again for a new hungry kitty) | The same, with the drinking sound. |
| 3e | `/time set night` and wait near a calm, fed kitty | When it naps, the icon is the sleeping cat with "Zz" (it used to show the litter-box icon). Napping starts at random, so it can take a while. |

## 4. Scorpion

`/function moctest:scorpion`: a wild scorpion and a pet scorpion.

| Step | Do | Expected |
|---|---|---|
| 4a | Walk in a circle around them, close enough that they turn to look at you (F5 helps) | The big back plate (carapace) stays fixed on the body while they turn. Before the fix it swung sideways on its own, splitting the body down the middle. |

## 5. Led animals and 1-block steps

`/function moctest:leads`: a long grass strip with a 1-block step 8 blocks east; a tamed MoC horse, bear, elephant,
ostrich and big cat, a wild deer, and a vanilla cow labelled "old behaviour". Gives leads.

| Step | Do | Expected |
|---|---|---|
| 5a | Lead the MoC horse and walk east over the step at walking pace (no sprinting) | It walks straight up the step behind you, like a vanilla horse. |
| 5b | The same with the bear, elephant, ostrich and big cat | All walk up the step. |
| 5c | The same with the **vanilla cow** | It gets stuck at the step face and the lead snaps if you keep walking: this is what MoC horses did before. |
| 5d | Lead the **deer** | It follows instead of running from you. |
| 5e | Stand next to the leashed bear for a while | It never rears up and freezes (it used to, and the lead snapped). |

## 6. Spawning: weights, biome placement, insects, no animal cap

| Step | Do | Expected |
|---|---|---|
| 6a | `/moc spawnlist` | Your biome's 1.12 types and every Mo'Creatures spawn entry with its weight, pack size and share of its pool. |
| 6b | `/moc spawnlist minecraft:plains` | kitty 8, bear 18, bird 15, big_cat 12, turkey 8, horse 8; ambient: butterfly 8, ant 7. No crocodile or chimpanzee. |
| 6c | `/moc spawnlist minecraft:desert` | ostrich 4, elephant 4, snake 8. No kitty, ant, mole, raccoon, Ent, bear or butterfly. |
| 6d | `/moc spawnlist minecraft:snowy_plains` | bear 6 (polar), big_cat 6 (leopard), bunny 8, fox 8, elephant 4. No Ent, mole or raccoon. |
| 6e | `/moc spawnlist minecraft:beach`, `minecraft:river`, `minecraft:ocean` | Beach: crab (ambient), dragonfly, dolphin, small_fish 88, fishy. River: ray 10 (sting ray), shark, small_fish 88. Ocean: ray 20 (manta + sting). |
| 6f | `/moc spawnlist minecraft:dark_forest` and `minecraft:nether_wastes` | Dark forest includes hell_rat 6. Nether: ogre 6 (fire ogre), horse_mob 8, hell_rat, flame_wraith, scorpion, manticore. |
| 6g | `/moc spawnlist minecraft:mushroom_fields` | No Mo'Creatures spawns (as in 12.0.5). |
| 6h | Stand on a plains or forest in daylight for a minute or two, then `/execute if entity @e[type=mocreatures:butterfly,distance=..64]` (also try bee, fly, cricket, ant) | Insects appear around you over time (the ambient pool spawns around players all the time, like bats). |
| 6i | `/moc spawn deer 120`, wait 30 seconds, `/execute if entity @e[type=mocreatures:deer]` | Still 120 (or all the adults that haven't died): nothing removes animals any more. The old cap cut them to 90 (40 in old configs). |
| 6j | Fly into unexplored land in different biomes | New chunks hold Mo'Creatures animals at 12.0.5's frequencies: birds and bears common in temperate land, kitties only on plains, elephants and ostriches in deserts, crocodiles, komodos and turtles in swamps. |
| 6k | Open the F9 settings, page 2, numeric tunables | No maxAnimals or maxAmbient any more. |

## Reporting back

For anything that does not match, note the step number, what happened, and send `fabric/run/logs/latest.log`.
