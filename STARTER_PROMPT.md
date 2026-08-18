# Starter prompt — new session

Copy the block below into a fresh session opened in
`C:\Users\warwa\ModDev\Mo' Creatures 26.2 Multiloader`.

*(The previous starter prompt — the one that produced the Ogre Prince, the Ogre Lair, Medusa, Minotaur
and the Chimpanzee — is preserved at the bottom of this file for reference.)*

---

```
Read HANDOFF_CONTINUE.md first, then TEST_PLAN.md.

Last session built the Ogre Prince + Ogre Lair and then Medusa, Minotaur and the
Chimpanzee. All of it is uncommitted working-tree state and the build is green on
both loaders — but an adversarial review found defects that were never applied
because the session ran out, and I stopped it mid-fix. Section 3 of the handoff
lists them exactly, with file:line and the fix for each.

Do these in order:

1. Apply the four confirmed fixes and the one-vote-confirmed one (§3a, §3b).
   They're all small and the handoff quotes the exact change.

2. Then the one that actually matters to me: I reported in-game that multi-head
   ogres' heads clip into each other, and the fix that shipped may only work at
   rest. The claim (§3c #6) is that setupAnim adds the full look yaw to every
   head, so they re-clip once an ogre turns its head more than ~11 degrees — which
   is most of the time. That claim was never verified because the verifier agents
   died. Settle it with real geometry across the whole 0-75 degree yaw sweep, not
   at the bind pose, and fix it if it's true. Do not tell me the clipping is fixed
   until you've checked it in motion.

3. §3c #7 (beard z-fighting on the green prince) if it's real.

Standing rules for this repo:

- Verify every vanilla API and behaviour against C:\Users\warwa\ModDev\mc262-ref
  (mapped 26.2 sources) rather than reasoning from memory. Legacy Mo'Creatures
  source is also on disk — 12.0.5 at
  "C:\Users\warwa\ModDev\Mo' Creatures\_mocreatures_aside\source-12.0.5" and
  5.1.5 at "...\legacy-source" — grep it before designing anything upstream may
  already answer.
- These four mobs were never in any Mo'Creatures release, so anything not in the
  handoff's evidence list is our design. Keep telling me which is which; don't
  present an invention as restoration.
- No pixels from the wiki/imgur preview renders in shipped textures. Concept art
  only. Everything currently in the repo respects that — keep it that way.
- If you run parallel agents: one owner per file. Agents write their own new
  entity/model files; shared registries get integrated centrally from returned
  snippets.
- If a review or agent batch comes back empty, check whether the agents actually
  ran before believing it. Last session a review returned "no findings" because
  every agent had died on a session limit, and findings whose verifiers died
  looked refuted when they were merely unverified.

Build with ./gradlew.bat build -x test and keep both loaders green. Finish with a
focused, copy-pasteable test list — I do the in-game testing, you can't drive the
dev client. Nothing is committed; ask me before you commit anything.
```

---

## Notes for whoever pastes this

- If you only want the quick wins, keep step 1 and drop steps 2-3 — but step 2 is the one visible bug
  a human actually reported, so it's the highest-value item in the file.
- The stopped verification workflow that would settle steps 2 and 3 is persisted; the handoff gives its
  script path, and re-running it is cheaper than redoing the analysis by hand.
- To test rather than build, skip the prompt entirely and work from `TEST_PLAN.md` — §12-13 and the
  Ogre Lair section are untested by a human, and the expected log line is now
  `initialized with 60 creatures`.

---

## Previous starter prompt (2026-08-10) — produced the four unreleased mobs

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
