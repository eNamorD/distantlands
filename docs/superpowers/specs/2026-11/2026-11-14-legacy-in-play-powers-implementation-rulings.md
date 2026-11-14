# Legacy in-play powers: implementation rulings

Rulings taken while planning the [Legacy in-play powers design](2026-11-14-legacy-in-play-powers-design.md),
numbered across its slices. Each says which plan raised it. They continue
the design's rulings 1-27 in the "Legacy in-play powers rulings" section
of `docs/rules/ambiguities.md`.

## Slice 1

[Plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice1.md).
Ruled by the owner on 2026-11-14.

- **R1. Does "If you are an Imperial" cover Golden Key's ACTION as well
  as its peek?** *Decision:* yes, it gates both; an Exile holder's menu
  leaves the ACTION out. *Reason:* the condition heads the whole text, as
  on the other two "IF YOU ARE AN IMPERIAL:" legacies, whose condition
  gates their TRAVEL and BATTLE PLAN. *Slice/files:* slice 1, Tasks 5 and
  6; `GoldenKey`, `GamePresentationProjector.identifiesCard`.
- **R2. Which relics has The Ear's holder "not identified"?** *Decision:*
  those missing from the holder's recorded card knowledge
  (`knowledge.siteRelics`), so their own pawn's site counts until they
  peek there. *Reason:* knowledge is what the minor peek changes; the
  projection's view is a presentation detail. *Slice/files:* slice 1,
  Task 8; `TheEarAction`.
- **R3. What do High Priest's and The Ear's lines say?** *Decision:* "p2
  moved 3 secrets from the Darkest Secret with High Priest" and "p2
  peeked at the relics at Deep Woods with The Ear". *Reason:* they follow
  ruling 13's pattern, and The Ear's line never names the relics.
  *Slice/files:* slice 1, Tasks 7 and 8; `HighPriest`, `TheEarAction`.
- **R4. How does the Act menu show an ACTION with nothing to do?**
  *Decision:* unavailable, with its reason, as card ACTIONs are. Only
  Golden Key for an Exile holder is left out. *Reason:* consistent with
  the existing menu; R1 makes the Exile case a role gate, not a lack of
  targets. *Slice/files:* slice 1, Tasks 3, 4, 5, 7 and 8; each power's
  `refusal`, `GoldenKey.listed`.
- **R5. What do the shared bank and Golden Key read while the Reliquary
  is set aside?** *Decision:* both read `reliquaryInPlay`, so a set-aside
  Reliquary shows nothing and gives nothing. *Reason:* the design's
  ruling 9 is about display, not an override of Foundations ruling 3, and
  with no Empire no holder is Imperial anyway. *Slice/files:* slice 1,
  Tasks 5 and 6; `GoldenKey`, `GamePresentationProjector.reliquary`.

## Slice 2

[Plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice2.md).

None recorded yet. The plan's open questions wait for the owner's rulings.
