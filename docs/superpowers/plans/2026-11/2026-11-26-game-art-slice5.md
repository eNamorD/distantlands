# Game Art, Slice 5: Pieces and Powers on a Site Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The owner's tenth request, in two parts. Part A: the pawns and
warbands on a site are their pieces' pictures, larger than today, in two
rows of their own, and they keep their size against the site at every
zoom. Part B: the power symbols a site card prints are what a player
clicks to read a power; the drawn site has the same click target in the
same place. One more task fixes the Atlas row in Chronicle Details, which
clips the site card.

**Architecture:** A site's face gets three boxes that `MatLayout` places
as rectangles inside the face, the way it places everything else on the
mat: a pawn row, a warband row and the power shield. Every length inside
them is a percentage of the box or a container unit of it, so the pieces
are a fixed share of the site at any map scale, with art on and off. The
markup is the same in both modes; only paint differs.

**Tech Stack:** Scala 3, Scala.js, munit, jsdom, CSS, `./sbtw`, compiler
flags including `-Werror`.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md)
and its
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md)
(R1 to R45). This slice settles R45, replaces R34 and R35 for pawns and
forces, and replaces the pawn and force forms of the glance layer
(implementation rulings R5 and R6 of the real proportions design, as
`DESIGN.md` records them).

## What was measured for this plan

Read from the code and from eight site card images (770×596 px; the
files are not in the repository):

- **One force a site.** `SetupSiteProjection.forces` is an `Option`: a
  site holds the warbands of its ruler and no one else's. The worst case
  on one face is six pawns and one warband group, not eight groups. The
  warband row is still built as a row, so a second group would stand
  beside the first if the projection ever carried one.
- **What lies on a face today.** Loose favor and secrets in the upper
  left, defense in the upper right, the pawns on one line, the forces on
  the next, then the powers' labels and the requirement over the name
  band, and a decision's cost badge at the foot. Relics and denizens are
  cards in the three slots to the right; the festival marker lies over
  those. Nothing else stands in the face's centre.
- **The print's power shield is in one place on every site card.** A
  shield at the lower left, about x 20 to 162 and y 425 to 596 of the
  770×596 image (Task 6 measures it to the pixel). It holds every power
  symbol the card prints.
- **A site has one or two powers** (24 sites, 5 with two). With two the
  print shows them in one of two ways: three sites split the shield left
  and right with a red line at about x 91 (Ancient City: Enduring, River;
  Headwaters: Mountain, River; Tidal Marshes: River, Coast), and the two
  islands (Fair Isle, Sunken Isles) print the Coast's ship over the
  Island's "+2" with no line. The projection's order is not the print's
  (Headwaters arrives as River, Mountain), so the side comes from the
  power's kind: Enduring, Mountain, River, Coast, left to right; Coast
  over Island.
- **Piece pictures.** Warbands are about 281×262. Pawns differ by colour,
  from 219×281 to 343×390 and 313×282, so a pawn stands in a box of one
  size and its picture is fitted inside it.
- **The face at each zoom.** The face is 685×525 mat px: 284×218 CSS px
  at 100%, about 105 px wide at desktop Fit and about 44 px wide at a
  phone's Fit.
- **The Atlas row clips the site card.** Looked at in the browser
  (Chronicle Details, Atlas, Headwaters): the row's board is the row's
  own box with `overflow: hidden`, and the face reaches 2.3 CSS px past
  it at the left, top and foot, so the card's edge and its left corners
  are cut. Task 9 fixes it.

## Global Constraints

- Work only in `/Users/roman/projects/distantlands/.worktrees/game-art`, on
  branch `game-art`. Never commit on `0.2-alpha` or `main`.
- A scratch server the owner is looking at runs on port 8083 from this
  worktree. Do not stop or restart it and do not use its database.
  Browser checks use port 8084 and a scratch database of their own.
- Licensed art: never commit, copy into the worktree, or upload any file
  from `/Users/roman/projects/distantlands/reference/assets/`. No test
  reads a licensed file. Tracked docs hold file names and numbers only.
- `.tooling` and `node_modules` are symlinks. Never `git add -A` or
  `git add .`; add files by path.
- The layout is the same with art on and off: the markup is equal after
  the art layer's own nodes are removed, and every box has the same
  rectangle. The invariance tests and the browser's rectangle comparison
  cover every box this slice adds or moves.
- The three stylesheet guards hold: no art rule paints an element a
  decision can mark unless it excludes marked ones; no rule outside the
  art block names `art-ready`; a rule that sets opacity on an element
  drawn with `printed` multiplies by `--printed-opacity`.
- Change a selector list with an exact, counted replacement; read the
  diff of `styles.css` before each commit.
- Never chain a commit after a check with `;` and never pipe a check's
  output before `&&`.
- `WorldBoardRenderer.scala` is at 712 lines of the 800 allowed. The new
  site code goes to a new file, `SitePieces.scala`.

### Protected

- A pawn stays an option target (`player` key), with its ring, its
  disabled reason and its hover note.
- A pawn's accessible name is its player's name; a force's is its label
  and count ("Red Exile x2"); a power's is its label. Each keeps its
  hover note.
- A power button keeps its click (`CardInspection.openPower` with the
  viewer's consent grid) and stops the click from reaching the site.
- The site stays one option target; nothing new inside it takes a click
  that is not a button or a marked pawn.
- Loose tokens, defense, the requirement, the name band, the cost badge
  and the three card slots keep their boxes.

## File Structure

| File | Change |
|---|---|
| `frontend/src/main/scala/distantlands/frontend/MatLayout.scala` | `sitePawnRow`, `siteWarbandRow`, `sitePowerShield`, `sitePowerRegions` |
| `frontend/src/main/scala/distantlands/frontend/SitePieces.scala` | new: the pawn row, the warband row and the power shield of one site |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` | `siteRow` and `pawnMarker` hand over to `SitePieces` |
| `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` | `siteDetails` no longer builds forces or power buttons |
| `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsContent.scala` | the Atlas board grows by the frame's line |
| `frontend/styles.css` | piece rows, shield, removal of the old pawn and force forms |
| `frontend/src/test/scala/distantlands/frontend/SitePiecesSuite.scala` | new |
| `frontend/src/test/scala/distantlands/frontend/SitePowerShieldSuite.scala` | new |
| `frontend/src/test/scala/distantlands/frontend/MatLayoutSuite.scala` and the suites that pin the old forms | changed |
| `src/test/scala/distantlands/server/StylesheetArtSuite.scala` | new pins |
| `docs/catalog/asset-proportions.md`, `DESIGN.md`, the spec, the rulings file | records |

## Part A: pieces on a site

### The layout this part builds

Inside the face, top to foot, in shares of the face's height (525 mat
px):

| Band | Top | Height | Holds |
|---|---|---|---|
| Resources | 2% | 16% | loose favor and secrets at the left, defense dice at the right (as today) |
| Pawns | 19% | 30% | one row, six boxes wide: a pawn's picture (22%) and its player's name under it (8%) |
| Warbands | 50% | 20% | one row: the warband's picture and "x2" beside it |
| Print | 71% | 29% | the power shield at the left; the name, the requirement and the cost badge as today |

Both rows start at the face's left margin and run to its right margin
(4% of the face's width each side). A pawn's box is one sixth of the row,
so six pawns fit on one row without wrapping or shrinking, and one pawn
is the same size as six. Pawns stand from the left in seat order. The
warband row holds one group today; a group is its picture, as tall as the
row, and its count.

Proposed answers to the open questions are written into the tasks; each
is marked **(Q<n>)** and listed again at the end.

### Task 1: The rows' rectangles

**Files:** `MatLayout.scala`, `MatLayoutSuite.scala`,
`docs/catalog/asset-proportions.md`.

- [ ] Write failing tests in `MatLayoutSuite`: `sitePawnRow(column, row)`
  and `siteWarbandRow(column, row)` lie inside `siteFrame(column, row)`,
  do not overlap each other, both end above `sitePowerShield`'s top, and
  the pawn row starts below 18% of the frame's height (the print's
  resources and die end there).
- [ ] Add the two rectangles to `MatLayout` from the table above, as
  shares of `siteFrame` (not of `siteCard`, so the frame's line margin
  does not move them).
- [ ] Record the shares in `asset-proportions.md`.
- [ ] `./sbtw "frontend/testOnly *MatLayoutSuite"` green, then commit:
  `feat(frontend): give a site's pawns and warbands rows of their own`.

### Task 2: The pawn row

**Files:** `SitePieces.scala` (new), `WorldBoardRenderer.scala`,
`SitePiecesSuite.scala` (new), `styles.css`.

- [ ] Write failing tests in `SitePiecesSuite` (a site row rendered with
  no art, then with a temporary manifest):
  - a pawn is `span.pawn` holding `span.pawn-piece` (a coloured disc,
    `aria-hidden`) and `span.pawn-name` with the player's name under it;
  - the `div.site-pawns` box is placed at `sitePawnRow` (its inline
    `left`, `top`, `width`, `height` equal the rectangle's percentages of
    the row);
  - six pawns give six `.pawn` children in seat order;
  - with art, `.pawn-piece` holds the pawn's image for the player's
    colour and turns ready; the markup without `.art` equals the markup
    with art off;
  - a pawn a decision offers still carries its option classes, its badge
    and its hover note.
- [ ] Move `pawnMarker` to `SitePieces.pawn`. The dot's character, the
  long form and the initial go; the piece is a box, the name is one span.
  `HoverTip` and `options.decorate` stay on `.pawn`.
- [ ] Stylesheet: `.mat-board .site-pawns` is a `mat-slot` with
  `display: flex` and `container-type: size`; `.pawn` is `flex: 0 0
  calc(100% / 6)`, a column; `.pawn-piece` is 73cqh tall, as wide as the
  pawn's box, the drawn disc a circle of the player's colour centred in
  it; its `.art` is `object-fit: contain`; `.pawn-name` is one line, cut
  with an ellipsis, centred, `font-size: 20cqh`.
- [ ] Delete the old pawn rules, each by an exact counted replacement:
  the `--row` placement, the two-names rule and its `:has()` forms, the
  `map-compact` and `map-tiny` pawn rules and the phone's five font
  steps (`styles.css` lines 496 to 498, 681 to 707, 758 to 761 and 776
  to 788 at this plan's HEAD).
- [ ] **(Q3)** The name under a pawn hides from sight (and stays the
  pawn's name) where it would be under 9 CSS px: on the tiny map. On the
  compact map it stays, at its share of the site.
- [ ] Update the suites that pin the old forms (`SiteFaceSuite`,
  `PieceArtSuite`, `WorldArtSuite`, `BoardOptionsRenderSuite`,
  `PrintedLayerSuite`, `MatBoardSuite`): each old assertion is replaced
  by the new form's, never deleted without a successor. List every
  replaced assertion in the commit body.
- [ ] `./sbtw frontend/test` green, then commit:
  `feat(frontend): stand a site's pawns in a row of pieces with names`.

### Task 3: The warband row

**Files:** `SitePieces.scala`, `ServerUiSupport.scala`,
`SitePiecesSuite.scala`, `styles.css`.

- [ ] Write failing tests: a site with forces has `div.site-warbands`
  placed at `siteWarbandRow`, holding one `span.site-force` with the
  force's class, its `data-ruler-*` attributes, the warband piece
  (`RulesTextRenderer.warband(forceKey)`), `span.site-force-count`
  "x2", and the accessible name "Red Exile x2"; a site with no forces
  has the box, empty (so the markup's shape does not depend on state the
  art cannot change); with art the piece holds the warband's image, and
  the bandit's file for bandits.
- [ ] Move the forces out of `siteDetails` into
  `SitePieces.warbands(site)`. The long row (`p.site-forces`) and the
  short form (`span.site-forces-short`) become the one `span.site-force`.
  Check every reader of `data-ruler-kind` and `.site-forces` in main and
  test code and move it to the new element.
- [ ] Stylesheet: the row is a flex row with `container-type: size`; the
  piece is 100cqh square; the count is `font-size: 55cqh`, bold, in the
  force's colour as today. Delete the old force rules (the `--row`
  placement, the label and count rules, the tiny map's short-form rules
  and the phone's), each by an exact counted replacement.
- [ ] `./sbtw frontend/test` green, then commit:
  `feat(frontend): show a site's warbands as their piece and a count`.

### Task 4: Grounds, option states and the black pieces

**Files:** `styles.css`, `StylesheetArtSuite.scala`.

- [ ] **(Q4)** Replace R34 and R35 for pawns and forces. A piece's
  picture needs no ground: it is its own shape, and the black pawn and
  black warband keep R27's light edge. Words over art keep one: the
  pawn's name and the warband's count are chips on Base (a fill and a
  same-colour shadow), over a ready site only, and the name's chip only
  while no decision offers the pawn. Loose tokens keep R34 as it is.
- [ ] With art off the same words stand on the drawn face, where they
  already measure well; no chip.
- [ ] An offered pawn's ring goes around the pawn's whole box (piece and
  name); its selected fill is the box's background. Pin in
  `StylesheetArtSuite` that no art rule paints `.pawn` without the
  `:not([class*="board-option"])` exclusion, and that the old
  `.site.art-ready .pawn` rule is gone.
- [ ] Measure contrast of each player colour's name and count on Base
  (R34 measured 6.6 : 1 or better; the colours do not change, so this is
  a re-check, recorded in the rulings file).
- [ ] `./sbtw test` and `./sbtw frontend/test` green, then commit:
  `fix(frontend): keep a ground under a piece's words, not its picture`.

### Task 5: Every zoom, and the drawn fallback

**Files:** `styles.css`, `DESIGN.md`.

- [ ] In the browser on port 8084, with a six-player scratch game and
  pawns moved onto one site (or the six pawns injected into one site's
  row for the look, noted as such): compare the rectangles of
  `.site-pawns`, `.pawn`, `.pawn-piece`, `.site-warbands`, `.site-force`
  with art on and off at 1440×900, 1024×768 and 375×812, at 100%, at Fit
  and at one step between. Each piece's width over its face's width is
  the same number at every zoom (the owner's "keeps its ratio").
- [ ] **(Q5)** Record the sizes that result: a pawn's box is 43×65 CSS px
  at 100%, 16×24 at desktop Fit and 7×10 at a phone's Fit; a warband is
  44, 16 and 7 px square. The glance layer today holds a pawn's dot near
  a readable size by letting it grow against the site; this part ends
  that for pieces, by the owner's request.
- [ ] Rewrite `DESIGN.md`'s glance paragraphs for pawns and forces and
  its Game Art section to say what is built.
- [ ] Commit: `docs: record the piece rows on a site`.

## Part B: site powers on the print's symbols

### Task 6: The shield's rectangle and its regions

**Files:** `MatLayout.scala`, `MatLayoutSuite.scala`,
`docs/catalog/asset-proportions.md`.

- [ ] Measure the shield on the 24 site images with the scratch script
  used for the frame line (outer edge of the braid, its peak, the red
  line where there is one). Record the numbers, the spread between
  cards, and the list of sites by form (one power; split left and right;
  ship over "+2") in `asset-proportions.md`.
- [ ] Write failing tests: `sitePowerShield(column, row)` lies inside
  `siteCard(column, row)` at the lower left, below the warband row;
  `sitePowerRegions(kinds, column, row)` returns one rectangle a power,
  in the order given, that together tile the shield: one power takes the
  whole shield; two that include `island` are the upper and lower halves
  (Coast above); any other two are left and right halves, the left one
  the earlier of Enduring, Mountain, River, Coast, Homeland, Pass,
  Plains.
- [ ] Implement both. The shield is given in the card image's pixels and
  mapped through the same `cover` fit the image has (the image is scaled
  to the box's width and trimmed evenly top and foot), so the region
  lies on the print whatever the frame's line margin is.
- [ ] `./sbtw "frontend/testOnly *MatLayoutSuite"` green, then commit:
  `feat(frontend): locate the power shield a site card prints`.

### Task 7: The power buttons on the shield

**Files:** `SitePieces.scala`, `ServerUiSupport.scala`,
`SitePowerShieldSuite.scala` (new), `styles.css`.

- [ ] Write failing tests: each power is `button.site-power` placed at
  its region, with its label as `aria-label`, its hover note, and the
  click that opens the power's overlay with the viewer's grid and does
  not reach the site; the buttons are in the projection's order in the
  DOM (reading and tab order), whatever their sides; the markup is equal
  with art on and off; `p.site-powers` and the " · " separators are
  gone; the requirement keeps its corner in the footer.
- [ ] Move the buttons from `siteDetails` to `SitePieces.powers(site,
  links)`.
- [ ] Stylesheet, both modes: a button is its region's box, with a
  focus ring and a hover ring (Bright Cream, as a card's), a pointer
  cursor, no fill. **(Q6)** Drawn fallback: the shield's outline (a
  plain rounded box with a 1 px Line border, not the print's pointed
  shape) and, inside each
  region, the power's label as printed words: whole where the region is
  the whole shield, cut with an ellipsis in a half. Over a ready site
  the label hides with the print and the outline goes, since the card
  prints both.
- [ ] The name band's left edge moves to the shield's right edge in both
  modes, so the drawn name does not run under the shield (the print
  does the same).
- [ ] **(Q7)** Each button is at least the region's size and never
  larger: at desktop Fit the shield is about 19×24 CSS px and a half is
  about 10 px wide. That is under the 24 px target size; the site's
  overlay and Chronicle Details stay the other way to a power's text.
- [ ] Delete R45's interim rules and the `map-compact` and `map-tiny`
  rules for `.site-powers` and `.site-footer` that no longer apply, by
  exact counted replacements; pin in `StylesheetArtSuite` that
  `.site-power` has no `background` over a ready site.
- [ ] Update `SiteFaceSuite`, `SitePowerGridSuite`, `SiteRowSuite`,
  `ChronicleDetailsContentSuite` and `ChronicleDetailsOverlaySuite` for
  the buttons' new parent; list the replaced assertions in the commit
  body.
- [ ] `./sbtw test` and `./sbtw frontend/test` green, then commit:
  `feat(frontend): open a site's powers from the shield its card prints`.

### Task 8: Look at every form

- [ ] In the browser on 8084, art on and off: a one-power site, a
  left-and-right site and an island (the scratch game's world, or sites
  swapped in for the look and noted as such). Each region's rectangle
  lies on its symbol; a click on each opens the right power; Tab reaches
  each; the site's own click still works beside the shield; a decision
  that offers the site still rings the whole row and a click on the
  shield opens the power, not the decision.
- [ ] Screenshots of the three forms, art on and off, for the report.

## Task 9: The Atlas row holds the whole site card

**Files:** `ChronicleDetailsContent.scala`, `styles.css`,
`ChronicleDetailsContentSuite.scala`.

- [ ] Write a failing test: the Atlas board's box is the row grown by the
  frame's line on each side (`--atlas-w` and `--atlas-ratio` from
  `siteCard`'s left, top and foot and the row's right), and the site row
  inside it is placed within that grown rectangle.
- [ ] Give `siteRow` the grown rectangle as `within` and size the board
  from it. No stylesheet change is expected; if the board's rounded
  corner still cuts the card, drop the board's radius there.
- [ ] Browser: Chronicle Details, Atlas, art on and off: the face's four
  edges and corners show.
- [ ] Commit: `fix(frontend): give the Atlas row room for the whole site card`.

## Task 10: Records, gate and browser check

- [ ] Rulings file: record the coordinator's rulings on Q1 to Q8; mark
  R34 and R35 amended, R45 closed (or reopened, if Part B is deferred),
  R20 as it stands.
- [ ] Spec: the site card bullet (pieces in rows, powers on the shield).
- [ ] `DESIGN.md` in step (Task 5 and Part B).
- [ ] Full gate: `./sbtw test`, `./sbtw frontend/test`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.
- [ ] Browser check on 8084: rectangle comparison art on against off at
  the three sizes; console clean; no horizontal scroll at 375.
- [ ] Commit the records: `docs: record pieces and powers on a site`.

## Size and risk

| Part | Tasks | Estimate | Risk |
|---|---|---|---|
| A, pieces on a site | 1 to 5 | about the size of slice 4: 5 commits, some 150 stylesheet lines replaced, 6 suites edited | Medium. It removes the glance layer's pawn and force forms, which about 30 assertions pin, and it trades readable pieces on a zoomed-out map for a fixed ratio (Q5). No rules or projection change. |
| B, powers on the shield | 6 to 8 | about half of A: 3 commits, one measurement of 24 images | Low to medium. The shield's place is the same on every card; the only table is which side a kind takes. The weak point is target size when zoomed out (Q7). |
| Atlas fix | 9 | one small commit | Low. |

Both parts can land well. If one is to be deferred, B is the one that
stands alone: A does not need it, and without B the powers stay as they
are after slice 4 (labels in the drawn footer, nothing over art), R45
is reopened, and the ROADMAP item would read "Site powers open from the
power shield a site card prints (game art slice 5, part B)". B without A also
works: the shield lies below today's pawn and force lines.

## Open questions

1. **Q1, pawn row capacity.** One row of six fixed boxes, no wrap and no
   shrink; one pawn is as large as six. Agreed, or should fewer pawns
   stand larger?
2. **Q2, warband groups.** The projection carries one force a site, so
   the row holds one group. Agreed that eight groups is not a case to
   build for?
3. **Q3, names under pawns.** Shown at 100% and on the compact map at
   their share of the site (8% of the face's height: 17 px at 100%, about
   6 px at desktop Fit); hidden from sight on the tiny map. Or: hide them
   wherever they fall under 9 px, which is already desktop Fit?
4. **Q4, grounds.** No ground under a piece's picture; a Base chip under
   a pawn's name and a warband's count over art; an offered pawn's ring
   around piece and name. This replaces R34 and R35 for pawns and
   forces.
5. **Q5, size against readability.** Fixed ratio means a pawn 16 px wide
   at desktop Fit and 7 px at a phone's Fit, smaller than today's dots
   there. The owner asked for the ratio; confirm that it holds on the
   tiny map and the phone too, or name a floor.
6. **Q6, the drawn shield.** A plain rounded outline with the powers'
   labels inside, cut in a half shield. Or the shield's pointed shape,
   or a letter for each power in a half?
7. **Q7, small targets.** The shield's regions are under 24 px at
   desktop Fit and smaller on a phone. Accept, with Chronicle Details
   and a zoom as the other ways, or enlarge the hit area past the print
   when zoomed out?
8. **Q8, the islands.** Coast is the ship above, Island the "+2" below.
   The other split is by kind order, since the projection's order is not
   the print's. Agreed?

## Self-Review

- Every owner sentence of request 10 has a task: pictures not words
  (2, 3), larger (the table), resources and dice at top (unchanged
  band), one row of pawns (2), one row of warbands with a count (3),
  the print's band at the foot (table, 7), a name under a pawn (2), the
  drawn fallback's same rows (2, 3, 5), the ratio at every zoom (5),
  symbols as click targets (6, 7), the drawn outline in the same place
  (7).
- The coordinator's notes: R34 and R35 (Task 4, Q4), row capacity
  (Q1, Q2), what else is in the centre (measured: nothing), how the
  regions are located (measured: one shield, two split forms), the drawn
  fallback (Q6), the Atlas (Task 9), deferral (Size and risk).
- No task reads a licensed file in a test; the shield's numbers are
  measured by a scratch script outside the repository.
