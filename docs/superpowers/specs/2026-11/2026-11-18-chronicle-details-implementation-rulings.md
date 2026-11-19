# Chronicle Details implementation rulings

Rulings the owner's orchestrator made while the
[Chronicle Details design](2026-11-18-chronicle-details-design.md) was
implemented, on questions the design and its slice plans did not settle.
Each entry gives the question, the decision and the reason, the confidence
when the orchestrator marked it low, and the slice and files it touches.
The design's own rulings D1-D4 are not repeated here.

## Slice 1

**R1. Where unseated lineages come from.**
Question: the design reads roles and legacies from `campaign.lineages`,
which holds the seated lineages only; a lineage nobody seated lives in
`campaign.baseline.lineages`, its in-game role in `campaign.boxRoles`.
Decision: add `CampaignState.standingLineages`: the seated lineages plus
the baseline lineages missing from `campaign.lineages`, each boxed one
with `boxRoles.getOrElse(id, role)`. `ChronicleCrown.roles` switches to
it with no behaviour change, its suites unchanged, and the projection
reads it.
Reason: one rule in one place; a second copy in the projector would
drift from the crown's.
Slice/files: 1; `model/GameState.scala`,
`gameplay/chronicle/ChronicleCrown.scala`,
`application/GameProjection.scala`.

**R2. Naming the Recent site's cards outside the overlay.**
Decision: `identifiesCard` names the denizens and edifices stored at the
Atlas's Recent site to every viewer, and never names relics. Its doc
comment cites the NF rulebook's Atlas privacy line. If a golden log
changes, stop and report the diff; do not re-record.
Reason: the rulebook makes exactly those cards public.
Slice/files: 1; `application/GamePresentationProjector.scala`.

**R3. The side a stored edifice shows.**
Decision: intact.
Reason: the rulebook stores intact edifices and starts them intact.
Slice/files: 1; `GamePresentationProjector.atlasSite`.

**R4. The case class default.**
Decision: `chronicleDetails = None` is the last `GameProjection` parameter
in Scala, and the key is required on the wire. A lineage's `role` and
`atlasRecent` are required and nullable.
Reason: this matches `foundations`; the wire rule is what matters.
Slice/files: 1; `GameProjectionDto.scala`, `GameProjectionCodec.scala`,
`ChronicleDetailsCodec.scala`.

**R5. Lineage order.**
Decision: lineages sort by their seat's colour when seated, otherwise by
the colour the lineage id names, then any left by id.
Reason: the design's "PlayerColor order" needs a colour for unseated and
test lineages.
Slice/files: 1; `application/GameProjection.scala`.

## Slice 2

**R6. Symbols and the flip line.**
Question: the card data marks icons as `symbol:` tokens the frontend has no
glyph for, and the Sun's and Throne's fronts end "Now, flip this card
over."
Decision: drop each symbol and keep its words. The Sun's legacy-goal legend
is one line per goal. Drop "Now, flip this card over": the "Back" line
marks the flip. Follow the card data where the printed sheet differs; the
card images (`The Sun (NF).png`, `The Sun - Back (NF).png`) agree with it.
Reason: the frontend has no glyphs for these icons, and the game already
words goals in place of icons (Legacies R34, R35, R42).
Slice/files: 2; `ChronicleTaskPresentation.scala`.

**R7. The aid strip's classes.**
Decision: one class stem per strip. The foundation DOM keeps every class;
the aids use the `chronicle-task` stem.
Reason: the design requires the foundation tests to pass unchanged.
Slice/files: 2; `SetupCardStrip.scala`, `FoundationStrip.scala`,
`ChronicleTaskStrip.scala`.

**R8. A task card's names.**
Decision: the strip shows "The Sun"; the inspector title and the hover note
read "Chronicle task · The Sun"; the button label reads "Chronicle task,
The Sun". No plate.
Reason: this parallels the foundation wording, and no task card ratio token
or image exists.
Slice/files: 2; `ChronicleTaskStrip.scala`.

## Slice 3

**R9. Where the row is placed, and the design's `rowRect` argument.**
Question: the design passes `rowRect`, which is always
`MatLayout.siteRow(column, row)`, and the row must sit at its slot on the
map but fill its box in the overlay.
Decision: `siteRow(site, column, row, within, options, links, pawns)`.
`siteRow` computes the row rectangle and places the article in `within`:
`MatLayout.Mat` on the map, the row's own rectangle in the overlay.
Reason: it removes a redundant argument that could mismatch, and keeps
the map DOM identical.
Slice/files: 3; `WorldBoardRenderer.siteRow`.

**R10. The pawns argument.**
Decision: `pawns: Vector[dom.Element]`, built by `pawnMarker`; the
overlay passes `Vector.empty`.
Reason: `siteRow` needs no projection, and the change in marking order is
invisible.
Slice/files: 3; `WorldBoardRenderer.siteRow`, `pawnMarker`.

## Slice 4

**R11. Where the inspector returns focus after a redraw.**
Question: a snapshot rebuilds a legacy chip while the inspector opened from
it is open, and `CardInspectionOverlay` looks for a rebuilt opener only in
its `.game-pane`.
Decision: widen that container selector to
`.game-pane, .chronicle-details`; the overlay's `.pane-heading` is the
fallback.
Reason: a one-selector change keeps focus on the rebuilt chip; holding
redraws would leave the overlay stale.
Slice/files: 4; `CardInspectionOverlay.open`.

**R12. How the Atlas box gets the site rules.**
Question: `siteRow`'s layout rules apply only under a `.mat-board`, which
also brings the mat's ratio and background (slice 3 review).
Decision: the Atlas box carries `mat-board chronicle-details-atlas-board`.
One two-class rule replaces the width, ratio and background with
`--atlas-w`, `--atlas-ratio` (set from `MatLayout`) and the Base fill.
Reason: every site rule is reused unchanged with one ratio source;
rewriting the map's selectors risks the map.
Slice/files: 4; `ChronicleDetailsContent.atlas`, `styles.css`.

**R13. The Atlas row at narrow widths.**
Decision: the Atlas box keeps the row's width on the map at 100% (about
742px) and scrolls sideways inside its own frame in a narrower panel. The
page and the panel never scroll sideways.
Reason: rem-sized text cannot shrink with the box, and a measured
transform is more code than the case earns.
Slice/files: 4; `ChronicleDetailsContent.atlas`, `styles.css`.

**R14. The Chronicle Phase in the browser check.**
Decision: check the overlay mid-game on a scratch database, on a game
started from an imported Chronicle built for the check (six scored games,
an unseated lineage with a dormant legacy, a denizen, an edifice and a
relic stored at the Recent site). The Chronicle Phase reveal stays covered
by the slice 1 projection tests, and the report says it was not seen in
the browser.
Reason: no route reaches the phase short of a whole game, and a seeding
tool is out of scope.
Slice/files: 4; the browser check.

**R15. Light and dark themes.**
Decision: screenshots in the one dark theme, at desktop and phone widths;
the report notes that no light theme exists.
Reason: the design's "both themes" assumed a light theme the app does not
have.
Slice/files: 4; the browser check.

**R16. The panel's measure.**
Decision: `min(50rem, 100%)`.
Reason: the Atlas row and its padding fit a desktop panel without
scrolling.
Slice/files: 4; `styles.css`.

**R17. Copy and placement the design leaves open.**
Decision: the quiet line is the first line of the scrolling content, and
the header stays the 39px pane header with the Chronicle's name as its
heading. The scorepad section is headed "Era Scorepad". The button wears
the Control face at the table edge's sizes (1rem, the glance size on the
compact map), at least 12rem wide, with a 44px touch floor set against the
map scale.
Reason: this fits the pane header pattern and the table edge's sizing and
title case.
Slice/files: 4; `ChronicleDetailsContent`, `styles.css`.

**R18. The button's size.**
Question: R17 read "a wide rectangular button beside the Shared Bank" as a
12rem Control, which the table's edge pushed to its far right.
Decision: the button takes the width the foundation strip used to fill.
`.table-edge .shared-bank` stops growing (`flex: 0 1 auto`), and
`.table-edge .chronicle-details-button` grows to fill the rest of the
table's edge and stretches to its height (`flex: 1 1 12rem;
align-self: stretch`), so it sits directly after the bank's last item,
wide and rectangular. R17's 44px floor on screen stays. This narrows R17.
Reason: the design's goal asks for a wide rectangular button beside the
Shared Bank.
Slice/files: 4 (review); `styles.css`.

**R19. An inspector open when the overlay closes.**
Decision: `ChronicleDetailsOverlay.hide` closes a card inspector open over
it before hiding, through a callback `GameTableShell` passes in.
Reason: otherwise the inspector outlives its overlay and focus falls to
the body; skipping hidden focus targets would still leave the inspector up
over the table.
Slice/files: 4 (review); `ChronicleDetailsOverlay`, `GameTableShell`.

## Whole-feature review

**R20. The rules register on the Atlas and the Foundations.**
Decision: amend the Foundations entry 8 in `docs/rules/ambiguities.md`
("nothing else about the Atlas is shown before the fill"): Chronicle
Details shows the Recent site's stored denizens and edifice to every
viewer at all times, Setup included, per the rulebook's Atlas privacy line
(R2); the Forgotten end stays unnamed. Mark entry 16 as superseded by the
Chronicle Details design, since the Foundations now live in the overlay.
Reason: the register must match shipped behaviour, and the rulebook makes
those cards public.
Slice/files: review; `docs/rules/ambiguities.md`.

**R21. The game-end save prompt over Chronicle Details.**
Decision: when `TableChronicle.ask` opens the game-end save prompt,
Chronicle Details closes first, which also closes an inspector over it
(R19).
Reason: the prompt (z 60) is modal and was drawn under the overlay
(z 300); closing beats re-layering, since the prompt then also clears any
inspector.
Slice/files: review; `TableChronicle`, `GameTableShell`.

**R22. Annotating the design.**
Decision: the design stays as approved, annotated rather than rewritten.
Its status line links the four plans and says questions were settled in
these rulings, R1-R22. At each line a ruling changed, a short "(Changed by
Rn.)" note names it. D1's "(R18)" reads "(Legacies implementation ruling
R18)".
Reason: readers implement from the design; its own rulings must be
findable.
Slice/files: review; the design.

**R23. The button's width.**
Question: R18's button filled the rest of the table's edge, about four
times wider than its label needs.
Decision: `.table-edge .chronicle-details-button` is as wide as a site's
three denizen slots and does not grow (`flex: 0 0 445px`): three card
widths and two slot gaps, 1072 of the mat's 5787 units, on the map's
2400px. It still sits directly after the Shared Bank and stretches to the
edge's height. The phone rule that set it to full width goes, because the
table's edge lies on the map, which scales as one at every screen size.
At the phone's fit zoom the label wraps to two lines above R17's floor.
This narrows R18.
Reason: the owner asked for a button about a quarter as wide, measured
against the denizens on the mat.
Slice/files: follow-up; `styles.css`.
