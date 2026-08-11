# Handoff — the four mobs DrZhark never shipped

Medusa, Minotaur, Chimpanzee and Ogre Prince. Every other creature on the Mo'Creatures roster is ported
and working as of `b62b21c`. These four are different in kind: **no Mo'Creatures build ever contained
them.** No class, no texture, no lang key, no partial implementation — verified by recursive filename and
content greps over the 12.0.5 upstream, the 5.1.5 legacy tree, this port, and the two known continuation
mods (`Elite-Modding-Team/MoCreaturesExtended`, `Rozmir-Rohi/mo-creatures-legacy`).

So this is **design-and-build, not extraction.** Read that sentence twice before starting, because the
instinct built up over the last ten creatures — find the legacy class, convert it — has nothing to bite on
here.

---

## The one rule that matters

**Separate evidence from wiki speculation, and never let the second masquerade as the first.**

The Fandom pages for all four are written in hedged editor voice — "may", "might", "most likely",
"possibly" — by fans reading a preview image in 2016. Almost none of it is DrZhark's design. Two concrete
examples of how easily this misleads:

- **Medusa's petrification is not confirmed.** The most-repeated "fact" about her traces to a *player
  asking* whether she'd turn you to stone or freeze you (forum comment #64599, 14 Mar 2015). DrZhark's
  reply was "I haven't worked on either yet" (#64602). The wiki trivia accurately records an *absence of
  a decision*; downstream retellings turned it into a mechanic.
- **The Minotaur wiki page originally claimed** they "might also transform at night like werewolves" and
  "might drop leather". Both were unsourced editor guesses and were later deleted from the wiki itself.

Where the record is silent, say so and make a deliberate design decision. Do not dress an invention up as
restoration.

---

## Readiness, worst to best

| Mob | Evidence | Assets in repo | Verdict |
|---|---|---|---|
| Chimpanzee | 1 promo render, no behaviour at all | none | Weakest. Pure design work. |
| Minotaur | 1 render (3 variants), 4 words of commentary | none | Art direction only. |
| Medusa | 1 model render (3 variants), finished model, no behaviour | none | Best art reference of the three. |
| **Ogre Prince** | 3 hard facts + a half-built dimension | **substantial** | **Start here.** |

---

## 1. Ogre Prince — the one to do first

Only mob of the four with real scaffolding, and the only one where DrZhark stated concrete design facts.

**Evidence-grade facts (his words):** there are **three princes** and they are **bosses**; **each drops
one unique item**; they spawn in the **Ogre Lair** alongside ordinary ogres. That is the whole corpus.
Everything else on the wiki ("may have high attack damage", "may summon ogres", "larger blast radius") is
fan reading of the preview image.

**Already in the repo, needing no new work:**

- **Seven `*_ogre_lair` blocks** fully registered and wired — `MoCBlocks.java:55-80`, BlockItems at
  `MoCItems.java:496-509`, blockstates, block/item models, all ten textures, seven loot tables, a plank
  recipe + advancement, and lang entries. **Nothing generates any of them** because no dimension exists.
- **Two of the three prince drops, already implemented.** `BUILDERHAMMER` → `MoCBuilderHammerItem`
  (working long-range block placer; the wiki ties two builder hammers to the green prince) and
  `STAFFTELEPORT` → `MoCStaffTeleportItem` with `.durability(128)`, matching legacy's `setMaxDamage(128)`
  (the wiki ties a teleport staff to the cave/blue prince).
- **Spare art for the third.** `staff.png`, `staff2.png`, `staff3.png`, `builderhammer2.png` all ship, and
  `STAFF`/`STAFF2`/`STAFF3` are registered as plain `new Item(...)` with no behaviour and placeholder lang
  names. In 12.0.5 these textures and item models exist but were **never registered as items** — DrZhark's
  unused spare art, the obvious candidate for the fire prince's unmade weapon.
- Reusable sounds: `MoCSounds.OGRE`, `OGREDYING`, `OGREHURT`, `DESTROY`.

**The Ogre Lair is a near-mechanical clone of the Wyvern Lair.** That dimension is complete and working in
this port — 17 data files plus code. The ogre side has 9 of them (loot tables, recipe, advancement). The
gap is exactly these:

```
data/mocreatures/dimension/ogre_lair.json                       ← 6 lines
data/mocreatures/dimension_type/ogre_lair.json
neoforge/.../data/mocreatures/dimension_type/ogre_lair.json     ← NeoForge duplicate, adds custom_skybox
data/mocreatures/worldgen/noise_settings/ogre_lair.json         ← swap 3 block ids
data/mocreatures/worldgen/biome/ogre_lair.json                  ← princes + ogres in spawners.monster
data/mocreatures/worldgen/configured_feature/ogre_lair_{tree,grass}.json
data/mocreatures/worldgen/placed_feature/ogre_lair_{tree,grass}.json
```

Plus code mirroring `MoCWyvernPortalFeature` (a portal-frame landmark that no-ops unless the placement
origin is within 24 blocks of 0,0, so exactly one frame builds), a travel item modelled on
`MoCStaffPortalItem` (stores the return dimension+position in `CUSTOM_DATA`, force-loads the destination
chunk, spiral-searches y=160..-32 for a landing surface, builds a 3×3 fallback platform, 1 durability per
jump), and the per-dimension sky in `MoCLairSky`/`MoCTwinSuns` + `SkyRendererMixin` (Fabric) /
`MoCLairSkyboxRenderer` (NeoForge). `MoCEntityWyvern.java:165` shows the in-lair behaviour switch pattern.

**Baseline to scale the bosses from** — the ordinary ogre is already ported in `MoCEntityOgre.java`:
darkness-gated player targeting with no line-of-sight check out to config `ogreAttackRange` (default 12);
a ground smash on a 50-tick cooldown within 3.0 blocks destroying blocks of hardness < 3.0 (mobGriefing
gated); per-variant smash radius from config (green 2.5 / fire 2.0 / cave 3.0); fire ogres fire-immune;
fire and cave ogres take 5 HP under bright open sky.

**One loose end worth a look:** `common/src/main/resources/assets/mocreatures/textures/blockstates/dirt_ogre_lair.png`
is a stray file in the wrong directory, probably dead.

**Also:** `GAP_AUDIT.md` H2 claims the Wyvern Lair still uses vanilla `floating_islands` with no
mocreatures noise settings. That is stale — `worldgen/noise_settings/wyvern_lair.json` now exists with the
mod's own blocks and surface rules. Close H2.

## 2. Medusa

**Status:** never coded, but **a finished model existed.** DrZhark, 11 Mar 2015 (comment #64585): *"You
mean like this one? The model is ready, but the animation has proven difficult to pull right"*. That is
the actionable engineering fact — **the serpentine locomotion is what killed her**, and it's the risk to
plan around, not the art.

**Appearance — strong evidence.** Render at `https://i.imgur.com/Rd0mq.png` (1000×472, 468,879 bytes;
the Fandom copy is byte-identical, so the wiki hosts his original upload). Humanoid female upper body with
bare midriff joined at the waist to a thick legless serpentine lower body, coiling then tapering into a
long tail. Six or seven segmented snake stalks rise from the scalp, each ending in a small snake head with
two coloured eye pixels — three tall above the crown, shorter ones framing the face. Wears shoulder
pauldrons, a chest wrap and bracers. Three variants: **olive-green body / red snake eyes / steel armour**;
**brown-tan / orange eyes / gold armour**; **slate-blue / yellow eyes / copper armour**.

Size is **not** derivable — it's a perspective view in a model editor and the plinths sit at a different
depth, so pixel ratios contradict each other. Pick a size deliberately.

**Behaviour: nothing documented.** Hostility is a reasonable inference (the mod's other mythological mobs
are hostile) but it is an inference. Stats, drops, spawning: the archived wiki infobox is literally
`health=? damage=? spawn=? drops=? exp=? sounds=?`. The one "Spawning" sentence that exists reads *"It is
not known where Medusa will spawn, but it may spawn..."* — self-labelled speculation, and the current wiki
dropped it.

## 3. Minotaur

**Evidence:** exactly one image and four words. `https://i.imgur.com/yq4Mt.png` (900×482), posted 14 Oct
2016 (comment #66979) captioned *"that's an early version"*. Three variants side by side on log blocks in
a model-editor viewport — the source of the wiki's "Variant 1/2/3", matching the mod's usual
`getType()`-driven texture convention. Variant 1 is Holstein/cow-patterned white with irregular markings.

**Behaviour: nothing.** The wiki's own infobox behaviour field is `?`. Its "may be neutral or aggressive"
line is a hedge, and the original 2016 page's werewolf-transformation and leather-drop claims were
unsourced and later deleted.

## 4. Chimpanzee

**Weakest of the four, but the announcement was genuine.** Posted 1 Apr 2013 alongside an Ogre render —
and the Ogre from that same batch shipped, which establishes it was a real roadmap post, not an April
Fools joke, and that a working model existed in his dev build. DrZhark at the time: *"this is very early,
I'm not working on it yet... Please don't get your hopes up."*

**Appearance:** `https://i.imgur.com/98kbv7V.jpg` (684×417 JPEG) — two chimpanzees beside a black
player-model silhouette placed deliberately for scale. Quadrupedal knuckle-walking stance, arms noticeably
longer than legs, torso angled forward. Dark brown-to-black shaggy fur with streaky vertical texture,
greying toward the lower limbs; dark fur cap over a large pale greyish-tan bare face with heavy brow ridge
and forward muzzle; large flat protruding pale ears.

**Behaviour: nothing from DrZhark.** Every wiki line is 2016 editor speculation — "may be neutral and
attack if provoked, much like boars" (the boar analogy is the editor's), "may be possible to tame, though
how is unknown", "may spawn with babies on their back, much like scorpions, as hinted by the picture"
(explicitly flagged as inference from the image).

---

## Artwork and licensing — read before shipping any pixels

**Do not lift pixels from any of these renders into a shipped texture.**

The GPLv3 grant covers the **`mocreaturesdev` repository**. None of these four renders was ever in it —
they are forum/imgur images from 2013–2016, years before the mod was open-sourced in 12.0.0 (Dec 2017), so
the code licence does not obviously extend to them. Fandom tags the Medusa file with
`Template:Copyright game`, whose text is explicit: *"This file (or parts of it) is copyright DrZhark ...
freely usable on this wiki"* — a wiki-scoped permission and an explicit refusal of general redistribution.

They are also **renders, not texture atlases** — perspective screenshots of posed models. No UV sheet was
ever published for any of them. Even setting licensing aside you cannot extract a usable texture: the
chimpanzee reference is a low-resolution JPEG.

**So: treat every one of them exactly as you would a concept-art sheet.** Author original models and
textures in DrZhark's style, matching the documented silhouette, proportions, palette and variant count.
Do not trace, upscale, or colour-pick-and-blit. The chimpanzee is the cleanest position of the four
precisely because there is nothing to be tempted by.

Reference copies pulled during research (scratchpad, not in the repo):
`scratchpad/medusa/imgur_orig.png`, `medusa/{A,B,C}.png`, `yq4Mt.png`, `mino_mid_zoom.png`,
`minotaur_{A,B,C}.png`. Note the Fandom per-variant renders were uploaded by a wiki user in 2024 with no
source field — third-party derivatives; verify anything from them against the imgur originals.

---

## How to build one, in this codebase

The port has strong conventions. Follow them and a creature is mostly declarative:

1. **Entity class** extending `MoCAnimal` (passive land), `MoCMob` (hostile), `MoCAquatic` or
   `MoCFlyingInsect`. Implement `selectType()` and `getTexture()`.
2. **Behaviour spec** — one `reg("id")` line in `MoCBehavior`'s static block covers taming, foods, drops,
   breeding, rideability, and the growth curve (`grow`/`spawnAge`/`babyRoll`).
3. **Model** extending the port's `EntityModel<MoCEntityRenderState>` pattern, with `createBodyLayer()`.
4. **Registration** across `MoCEntities`, `MoCModelLayers`, `MoCreaturesClient` (layer + renderer),
   `MoCreatures.registerEntityAttributes`, `MoCSpawns` (placement + biome list), `MoCConfig`'s id sets,
   `MoCItems` (spawn egg) and `en_us.json`.
5. **Assets** — `textures/models/<name>.png`, spawn-egg item + model JSON.

`MoCEntityRaccoon` / `MoCModelRaccoon` (a straightforward passive) and `MoCEntityMiniGolem` (a hostile
with a held-block render layer) are the cleanest recent examples to copy from.

**Verify against `C:\Users\warwa\ModDev\mc262-ref`** — the full mapped 26.2 sources. Do not guess a
vanilla signature or behaviour; grep it. Four traps that already cost debug cycles are documented in
`TEST_PLAN.md` and in the project memory: mobs cannot ride players, `ageBoundaryReached` fires on both
age crossings, a non-`SUCCESS` `mobInteract` makes the client eat the player's own item, and entities
render unculled so zero-thickness boxes tear.

**Build with `./gradlew.bat build -x test`.** Both loaders must stay green. The user tests in a dev
client and is good at it — finish with a focused, copy-pasteable test list rather than claiming
verification you did not do.
