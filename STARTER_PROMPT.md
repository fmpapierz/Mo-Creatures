# Starter prompt — new session

Copy the block below into a fresh session opened in
`C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader`.

---

```
Read HANDOFF_UNRELEASED_MOBS.md first, then TEST_PLAN.md.

I want to add the four Mo'Creatures mobs DrZhark announced but never released:
Ogre Prince, Medusa, Minotaur and Chimpanzee. Start with the Ogre Prince — it's
the only one with real scaffolding already in the repo.

Two things to get straight before you write any code.

First: these were NEVER implemented in any Mo'Creatures build. There is no legacy
class to convert. This is design-and-build, and the wiki pages are mostly hedged
fan speculation ("may", "might", "most likely") rather than DrZhark's design. The
handoff separates the handful of evidence-grade facts from the guesswork — hold
that line. Where the record is silent, make a deliberate design decision and tell
me it's yours rather than presenting it as restoration.

Second: do not lift pixels from the surviving preview renders into shipped
textures. They were never in the GPLv3 repository, Fandom tags them
"copyright DrZhark, freely usable on this wiki", and they're perspective renders
rather than UV sheets anyway. Use them as concept art — match silhouette,
proportions, palette and variant count, and author the textures originally.

For the Ogre Prince specifically, the handoff lists what already exists (seven
ogre-lair blocks fully wired with nothing generating them, two of the three
prince drop items implemented, spare art for the third) and the exact 8 data
files plus code the Ogre Lair dimension needs, using the working Wyvern Lair as
the template.

Verify every vanilla API and behaviour against C:\Users\warwa\ModDev\mc262-ref
(full mapped 26.2 sources) rather than reasoning from memory — the handoff and
TEST_PLAN list four traps that already cost debug cycles.

Build with ./gradlew.bat build -x test and keep both loaders green. When you're
done, give me a focused test list with exact commands. I'll do the in-game
testing; you can't drive the dev client.

Plan the Ogre Prince before implementing it, and check the design with me first —
three bosses, one unique drop each, spawning in the Ogre Lair is all DrZhark
actually specified, so the rest is ours to decide.
```

---

## Notes for whoever pastes this

- Drop the last paragraph if you'd rather it just build something and iterate.
- To do a different mob first, swap the name — but the Ogre Prince is genuinely
  the highest-value start, and building the Ogre Lair unblocks the other three
  having somewhere thematic to live if you want them there.
- If you want all four scoped before any code, replace the last paragraph with:
  *"Produce a design doc covering all four before implementing anything."*
- The session will likely want to run parallel agents. One rule from last time:
  **one owner per file** — agents write their own new entity/model files, and the
  shared registries get integrated centrally afterwards from returned snippets.
