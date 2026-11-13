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
Amended after the whole-feature review: a banner is a square of
`--ratio-banner` everywhere. On a player board its side is `--card-w`; on
the table's edge it keeps its larger side, held in one named custom
property. The real banner is three card widths across, and relative sizes
are not this pass's concern.

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
compact map keeps its own offset. A square or landscape box that prints
power marks sets the band above the marks, as a denizen does, since at the
foot it would cover them (confirmed after slice 2).
Reason: the design anchors live state to the box so licensed art can
replace the printed parts; an anchor that depends on the box's height is
not one anchor, and state covering the name breaks the design's rule that
the drawn name must still fit the narrower face.
Slice/files: slice 1 review; `frontend/styles.css`.

## Slice 3

**R5. What a site face keeps below scale 0.4.**
Question: at a tablet's or a phone's Fit (below 0.4, `map-tiny`) the site
face hid its defense and forces as well as its footer, so who rules a site
and with how many warbands could not be read without zooming.
Decision: forces and defense stay on the face at every scale, in a short
form at the glance size; only the footer (terrain, requirement, powers) is
left to the zoom and the overlay. As a card carries `card-name` and
`card-name-short`, a site carries both forms in the DOM and the stylesheet
picks one, so a zoom never re-renders. The short form of forces is the
warband glyph in the force's colour with its count, the treatment
`CardFace` gives warbands on a card; the short form of defense is one
defense-die glyph with a count. The short forms are hidden from assistive
technology and carry a hover note with the long text; the long forms keep
the accessible text they have. Under `map-tiny` the long forms hide and the
short forms show; above it the reverse. If the phone's face at Fit cannot
hold everything at the glance size, defense drops first, then the name's
tail; pawns, forces and tokens never drop.
Reason: the design's glance layer holds names, pawns, tokens, defense and
forces at a readable size while the board shrinks, and who rules a site is
the state a player reads the whole map for; the earlier instruction to hide
what cannot reach about 9px was meant for decoration and printed labels,
not for state.
Slice/files: slice 3; `ServerUiSupport.siteHeading`, `siteDetails`,
`frontend/styles.css`.

## Slice 3 review and whole-feature review

**R6. How pawns are drawn on a zoomed-out site face.**
Question: below the compact threshold the pawn row wrapped without limit,
so one long player name or four pawns ran into the site's name or past the
face's foot.
Decision: below scale 0.72 the pawn row is one line that never wraps and
never leaves the face. Each pawn carries both forms in the DOM, as names
do: the long form (dot and player name) above the compact threshold; under
`map-compact` the dot with the player's initial; under `map-tiny` the dot
alone. The short forms are hidden from assistive technology; the pawn keeps
its accessible name and gets the player's name as its hover note. Six pawns
and a festival marker fit the phone's face at Fit, the dot sized from the
face's width where the glance size would not fit them. Pawns keep a row of
their own that overlaps neither tokens, forces, defense nor the name.
Reason: ruling R5's "pawns never drop" needs a form that fits.
Slice/files: slice 3 review; `WorldBoardRenderer.world`,
`frontend/styles.css`.

**R7. A non-colour cue for the Empire's short forces.**
Question: with the warband glyph in the force's colour, an Imperial force
and a purple Exile read the same.
Decision: the Empire's short form also carries a cue that is not colour,
drawn from what the design system already uses for the Empire, or, if
nothing fits, an outline ring around the glyph. Bandit and player forms
need nothing beyond colour and the hover note, as pawns do today.
Reason: colour alone may not carry a distinction the rules depend on.
Slice/files: slice 3 review; `frontend/styles.css`.

**R8. The round wheel's printed parts.**
Question: no part of the drawn wheel was marked printed, and the current
round was only one segment's fill, so licensed art could neither hide the
drawn wheel nor keep the round.
Decision: the wheel's eight segments and their numbers are printed, drawn
in one neutral fill; the current round is a separate unmarked element over
its segment, a wedge in the current brass fill; the limiter mark stays
state. The tracker's accessible label is unchanged.
Reason: the printed marking must split what art replaces from what lies on
it in play.
Slice/files: slice 3 review; `WorldBoardRenderer.roundTracker`.

**R9. Where the printed mark may go.**
Question: the site-power buttons sat inside the printed footer, and they
are the map's only opener for a site power's text and its consent grid.
Decision: printed marks content, never a control or a box. No interactive
element is printed or sits inside a printed element; the marking goes on
the label inside the control, and on the terrain and requirement text. Box
fills, borders and hit areas are not content: they stay unmarked, and L3
decides how a box is filled. So the site-power buttons are unmarked with
their visible label printed; a deck's back letter is printed, as a card's
is; the bank's suit glyph is printed; an empty site frame's dashed outline
is printed where the mat prints the frame, while empty card-slot outlines
stay unmarked, since the mat prints none. Glance short forms that restate
printed content stay printed; whether L3 keeps a glance layer over art is
L3's question, recorded in the roadmap.
Reason: hiding the printed layer must never take a control or state with
it.
Confidence: low on the last point only (glance short forms over art).
Slice/files: whole-feature review; `ServerUiSupport`, `WorldBoardRenderer`,
`docs/ROADMAP.md`.

## As built

The design says `--card-h` derives from `--card-w` and that Fit is near
0.45 at 1440x900. What shipped keeps `--card-h: 18.2ex` and derives
`--card-w` from it, the plan's choice, so no card changed height; and Fit
there is about 0.416, height-bound once the table's edge is counted.
