# Real proportions implementation rulings

Rulings the owner's orchestrator made while the
[real proportions design](2026-11-13-real-proportions-design.md) was
implemented, on questions the design and its
[plan](../../plans/2026-11/2026-11-13-real-proportions.md) did not settle.
Each entry gives the question, the decision and the reason. The design's
own owner rulings are not repeated here.

## Slice 1

**R1. The flowing map before the mat board.**
Question: with the denizen ratio, a card on the flowing map grows from
12.04rem to 13.45rem tall inside a site box of fixed height with hidden
overflow, so the powers line may clip; `GameTableShell.nameBoxEm` still
reckons a 7.3rem card. Both last until slice 2 replaces the map.
Decision: accept both through slice 1. The slice 1 browser check notes what
it shows and fixes nothing.
Reason: the branch does not ship between slices, and a temporary cap would
be code written to be deleted; tasks 6 and 7 replace both.
Slice/files: slice 1; `frontend/styles.css`, `GameTableShell.scala`.

**R2. An opener for the Oath goal.**
Question: the design shows every stand-in at its real ratio in the
inspection overlay, but the shared bank's Oath goal line is plain text, so
the `oath-goal` plate had nothing to open it.
Decision: the shared bank's `.oathkeeper-goal` becomes a button, its class
and visible text unchanged, that opens `CardInspection.openText` with the
title and lines the Oathkeeper title pill uses and the plate `oath-goal`;
an unknown goal falls back to its raw key and no lines, as the pill does.
It takes the stylesheet's existing plain text-button treatment and a
visible focus ring. The `.oathkeeper-holder` line stays text, and the title
pill keeps the plate `oathkeeper-title`. A test pins the plate and the
lines.
Reason: the design promises every stand-in its real ratio in the overlay,
and an unheld Oath has no way to read its goal today; the change reuses
data already projected.
Slice/files: slice 1, Task 4; `WorldBoardRenderer.sharedBank`,
`frontend/styles.css`, a render suite.

**R3. Relics and banners shrink with the card width.**
Question: a relic's box is a square of `--card-w`, so the narrower card
makes relics and banners about ten percent smaller.
Decision: keep the plan. A relic and a banner are squares of `--card-w`.
Reason: the real relic is exactly a denizen's width (381 px), which also
matches `MatLayout.relicSlot`, and the owner allows relative sizes to
change as long as each asset's own ratio is right.
Slice/files: slice 1; `frontend/styles.css`.
