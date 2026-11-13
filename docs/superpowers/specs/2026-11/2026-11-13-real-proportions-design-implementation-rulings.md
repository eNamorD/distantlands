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

## Slice 1 review

**R4. Where the token band sits on each box kind.**
Question: Task 3 anchored the token band a fixed 2.1em above a card's
foot. On a box shorter than a denizen (a relic, a banner, a legacy) that
lands mid-face over the name, the goal or the defense dice, and on a
denizen with a long name, a restriction banner, an Unimplemented band or a
Replace badge it runs into the header or the lifted power marks.
Decision: the band is anchored to the box's foot region, per kind, and the
face reserves its room so printed content never runs under it. On square
and landscape boxes (relic, banner, legacy, dormant legacy) the band sits
at the foot, about 0.3em up, and the face's flowing or centred content
stops above it. On denizen-shaped boxes the band keeps its place just above
the power marks' row; the header region ends above it, and a name too long
for the room is clipped there rather than overlapped, since the hover tip
and the accessible name carry the whole name. Unimplemented and Replace
faces lift the band by the same amount they lift the power marks. The
compact map keeps its own offset.
Reason: the design anchors live state to the box so licensed art can
replace the printed parts; an anchor that depends on the box's height is
not one anchor, and state covering the name breaks the design's rule that
the drawn name must still fit the narrower face.
Slice/files: slice 1 review; `frontend/styles.css`.
