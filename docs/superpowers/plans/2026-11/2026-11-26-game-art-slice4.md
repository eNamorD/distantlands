# Game Art, Slice 4: Owner Review Fixes Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The nine fixes the owner asked for after looking at the running
table: the round wheel's marker and the Usurper's token, labels that read
on the mat, a site card that covers the mat's printed frame, no power
chips over site art, favor banks that can be read, the banner's art in
its overlay, empty denizen slots that show on the mat, and the Oath
goal's card in the shared bank.

**Architecture:** No new plumbing. Each fix is a renderer or stylesheet
change on top of `ArtLayer` and the nearest-box rule. Where a box moves
or grows, the change is made in `MatLayout` or in markup that is the
same with art on and off, so the drawn fallback takes the same box; where
only paint changes over art, the rule is scoped to a ready box.

**Tech Stack:** Scala 3, Scala.js, munit, jsdom, CSS, `./sbtw`, compiler
flags including `-Werror`.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md)
and its
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md)
(R1 to R36). This slice replaces R20 and amends R13. The owner's
screenshots (not in the repository) are numbered 8 to 14 in the
orchestrator's brief; each task names the one it answers.

## Open questions this plan waits on

The plan is written to one answer for each. No production code is written
until the orchestrator rules.

- **Q1. Which image is "the Oathkeeper image" (item 9).** Two files could
  be meant: the Oath goal card (`cards/Supremacy Oath (NF).png`, portrait,
  titled "Oathkeeper of Supremacy", printing the goal and the successor
  clause) and the Oathkeeper title (`tts/titles/oathkeeper.jpg`, the card
  the Oathkeeper holds). The shared bank's line names the goal and opens
  the goal's words, so the plan shows the **Oath goal card** there, as a
  card box at the goal's ratio and the banners' height, a button that
  opens the same overlay as now. The holder stays a line of words under
  it: "Oathkeeper: unheld", "Oathkeeper: Black", "Usurper: Black". The
  title's art stays on the holder's title plate (R28).
- **Q2. How the site card covers the mat's frame (item 4).** The mat
  prints a 669×509 frame; the site card's box is 657.6×509, centred, so
  the frame's line shows at the sides, and where the line is thicker than
  the gap it shows above and below too. Two ways:
  (A) the site's box becomes the frame's rectangle grown by the line's
  measured half-thickness on every side, and the image fills it with
  `object-fit: cover`, which trims about one percent of the card's
  picture; the drawn face takes the same box. (B) the card keeps its
  ratio exactly and is scaled until it covers the line, which makes a
  site about three percent taller than the denizen cards beside it.
  The plan takes A.
- **Q3. The Usurper's token on the wheel (item 2).** The token fills its
  wedge: sized to the wedge and turned to the wedge's angle, its point
  toward the hub, as in the reference. With art off, the drawn mark takes
  the same room: the wedge's own shape in the limiter's colour with the
  diamond on it. When the limiter's round is also the current round, the
  token lies under the round marker and the wedge's border.
- **Q4. The round marker (item 1).** A disc in the outer half of the
  current wedge, where the cylinder stands in the reference. It needs a
  silver, which the palette does not have: the plan adds one named
  colour, `--round-marker` (a light warm grey with a darker rim), and
  records it in `DESIGN.md`. The current wedge keeps its Bright Cream
  border. Its brass fill hides the mat's own wedge colour, so over a
  ready mat the plan drops the fill and keeps the border and the disc;
  with art off the fill stays.
- **Q5. Labels on the mat (item 3).** The tracker's two lines ("Visions
  Drawn 0 · First player Blue", "Usurper locked until round 4") become
  chips on opaque Base over a ready mat, each hugging its text; with art
  off they are unchanged. An audit of every other element drawn straight
  on the mat is part of the task; anything found gets the same ground and
  is listed in the report.
- **Q6. Favor banks (item 6).** The bank keeps its box (emblem and
  circle, the decision's target). Inside it, the favor token fills the
  mat's circle and the count stands centred over the token, large, on a
  small Base disc; the suit's glyph stands on the mat's emblem and stays
  printed. The drawn fallback has the same three places with the drawn
  glyphs. The circle and emblem rectangles join `MatLayout` and
  `asset-proportions.md`.
- **Q7. The banner's overlay (item 7).** The overlay shows the banner's
  own face (the `BannerFace` box, with its art and what it holds) at the
  overlay's size, left of its rules, the way a card's overlay shows the
  card. With art off it shows the drawn face.
- **Q8. Empty denizen slots (item 8).** Over a ready mat an empty slot's
  dashed outline turns Bright Cream with a dark line under it, so it
  reads on light and dark parts of the mat. A slot a decision offers
  keeps its option ring. With art off the outline is unchanged.
- **Q9. Site powers between slices (item 5).** With the chips gone and
  slice 5's click targets not yet built, a site's powers have no visible
  cue over art until slice 5 lands; their buttons stay focusable in
  place with their hover notes. The plan accepts that for the time
  between the two slices.

## Global Constraints

- Work only in `/Users/roman/projects/distantlands/.worktrees/game-art`, on
  branch `game-art`. Never commit on `0.2-alpha` or `main`.
- A scratch server the owner is looking at runs on port 8083 from this
  worktree. Do not stop or restart it and do not use its database.
  Browser checks use port 8084 and a scratch database of their own.
  Rebuilding the frontend is allowed.
- Licensed art: never commit, copy into the worktree, or upload any file
  from `/Users/roman/projects/distantlands/reference/assets/`. No test
  reads a licensed file. Tracked docs hold file names and numbers only.
- `.tooling` and `node_modules` are symlinks. Never `git add -A` or
  `git add .`; add files by path.
- The layout is the same with art on and off. A box that moves or grows
  does so for the drawn fallback too. A ground under live state is a fill
  and a same-colour shadow, never padding. The invariance tests and the
  browser's rectangle comparison cover every box this slice touches.
- No art rule paints (`background`, `box-shadow`, `border-color`) an
  element a decision can mark, unless it excludes marked ones; no rule
  outside the art block names `art-ready`; a rule that sets opacity on an
  element drawn with `printed` multiplies by `--printed-opacity` (the
  three stylesheet guards).
- An SVG art image's own size is an inline style, never an attribute.
- Change a selector list with an exact, counted replacement; read the
  diff of `styles.css` before each commit.
- Chain a commit after a check with `&&` only, on a command whose exit
  status is the check's; never pipe a check's output before `&&`.
- Contrast is measured: text on a ground meets 4.5 : 1 against the ground
  composited over white and over black.
- One new colour is allowed, the round marker's silver (Q4), named once
  and recorded in `DESIGN.md`. No other.
- No `transition` and no `animation`.
- The build is `-Werror`; `scripts/check-architecture.py` enforces size
  limits (800 lines a file; `WorldBoardRenderer.scala` is near 680) and
  layering. If a task would push a file over, split by reason to change
  and list the split as a deviation.
- Never write a bare `===` or `=word` in a shell command.
- Docs, comments, commit messages and plans are plain English prose. Test
  helper names state what they build.
- Commit messages are Conventional Commits and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after every commit.

### Protected

These must behave exactly as before. Do not edit them except where a step
names the edit:

- `GameArt`, `ArtLayer.attach`, `attachGlyph`, `attachCrop`; card faces
  and backs; piles; plates; dice; the Settings dialog; the game log's
  redraw on a change of art.
- `MatLayout`'s numbers, except the site card's rectangle (Task 4) and
  the two rectangles Task 6 adds.
- `BoardOptions`: how a site, a bank, a slot and a banner take their
  option marks.
- The `printed` marking, except where a task names an element.
- The festival marker's band, the pawn and force rows on a site (slice 5
  changes them), player boards.

## File Structure

| File | Responsibility |
|---|---|
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` | The round marker, the Usurper's token, the bank's parts, the Oath goal card. |
| `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala` | `attachMark` takes a size of two lengths and a turn. |
| `frontend/src/main/scala/distantlands/frontend/MatLayout.scala` | The site card's rectangle; a bank's circle and emblem. |
| `frontend/src/main/scala/distantlands/frontend/BannerFace.scala`, `CardInspection.scala`, `CardInspectionOverlay.scala`, `GameTableShell.scala` | The banner's face in its overlay. |
| `frontend/styles.css` | Every paint change. |
| `docs/catalog/asset-proportions.md` | The new rectangles and how they were measured. |
| `DESIGN.md`, the design, the rulings | Kept in step. |

---

### Task 1: The round marker on the current wedge

Answers screenshot 9 (the cylinder on the top right wedge).

**Files:**
- Modify: `WorldBoardRenderer.scala` (`roundSegment`, `roundTracker`)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/MatBoardSuite.scala`
  or the suite that already covers `roundTracker` (find it with
  `grep -ln "round-current" frontend/src/test`)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `RoundSegment.discX`, `discY`: the centre of the outer half
  of the wedge, at radius 74 of the wheel's 100, on the wedge's middle
  angle. A `circle.round-marker`, radius 11, in the current round's
  group, after the wedge and before the number. It is not printed.

- [ ] **Step 1: Write the failing tests**
  - "the current round's wedge holds the round marker, a disc in its
    outer half, and no other wedge does": one `circle.round-marker` in
    `g[data-round="3"]` for round 3, its `cx`/`cy` equal to the segment's
    `discX`/`discY`, `r="11"`, no `printed` class, and
    `closest(".printed")` empty.
  - "the round marker is drawn with art on and off": same markup both
    ways (the invariance helper).
  - Stylesheet: `rule(".round-marker")` contains
    `fill: var(--round-marker)` and a stroke;
    `rule(".mat-board.art-ready .round-current")` contains `fill: none`
    (Q4) and no `stroke: none`; `--round-marker:` is defined once in
    `:root`.
- [ ] **Step 2: Run them and see them fail.**
- [ ] **Step 3: Implement.** Add the two coordinates to `RoundSegment`
  from `point(74.0, middle)`. In `roundTracker`, after the current wedge
  is appended, append the circle. In `styles.css`, in the `:root` colour
  table, add `--round-marker: #cfcac0;` with a one-line comment (the
  game's silver round marker); beside `.round-current` add
  `.round-marker { fill: var(--round-marker); stroke: #6f6a61;
  stroke-width: 2; }`. In the art block add
  `.mat-board.art-ready .round-current { fill: none; }`.
  If the rim's grey is not an existing colour, use `var(--line-mid)`.
- [ ] **Step 4: Run the frontend and stylesheet suites; commit**
  `feat(frontend): mark the current round with the round marker`.

---

### Task 2: The Usurper's token fills its wedge

Answers screenshots 8 and 9.

**Files:**
- Modify: `ArtLayer.scala` (`attachMark`)
- Modify: `WorldBoardRenderer.scala` (`roundSegment`, `roundTracker`)
- Modify: `frontend/styles.css`
- Test: `PieceArtSuite.scala`, `StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `ArtLayer.attachMark(group, mark, key, x, y, width, height,
  turn: Double)`: the image's box is `width` by `height`, centred on
  (`x`, `y`), with `transform="rotate(turn x y)"`; its size stays an
  inline style.
- Produces: `RoundSegment.middle` (the wedge's middle angle in degrees),
  `centreX`, `centreY` (radius 68.5, the wedge's radial middle). The
  drawn mark becomes `path.limiter-marker`, the wedge's own path, with
  the diamond `text.limiter-diamond` on it; both are in the limiter's
  round's group, under the round marker when the rounds coincide.

The wedge spans radii 49 to 88 and 41 degrees. The token's box is 54 wide
(the chord near the outer edge) and 39 tall (the radial depth), centred
on the wedge's middle, turned by the wedge's middle angle plus 90 degrees
so its foot faces the rim. The exact turn depends on how the file stands
(point up or down); Step 3 settles it in the browser and the test pins
the number.

- [ ] **Step 1: Write the failing tests**
  - "the Usurper's token fills the limiter's wedge": the image's style is
    `width: 54px; height: 39px;`, its `x`/`y` centre it on
    (`centreX`, `centreY`), its `transform` is `rotate(<turn> <cx> <cy>)`.
  - "with art off the limiter's wedge is drawn as a wedge with its
    diamond": `path.limiter-marker` has the segment's `d`;
    `text.limiter-diamond` reads "◆"; the hover note is on the path.
  - "a load hides the drawn wedge and its diamond": `art-ready` on the
    path; stylesheet `rule(".round-tracker .limiter-marker.art-ready")`
    and the sibling rule for the diamond.
  - "when the limiter's round is the current round the token lies under
    the round marker": document order in the group.
- [ ] **Step 2: See them fail.**
- [ ] **Step 3: Implement,** then start the scratch server on 8084 and
  look: the token must sit inside the wedge's printed edges at three
  sizes, point to the hub. Correct the turn or the box by eye, update the
  test's numbers, and take a screenshot of the wheel beside the
  reference for the report.
- [ ] **Step 4: Run the suites; commit**
  `fix(frontend): make the Usurper's token fill its wedge`.

---

### Task 3: Labels that read on the mat

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [ ] **Step 1: Audit.** On the scratch table with art on, list every
  element inside `.mat-board` that draws text or a glyph and is neither
  inside a box with a ground (`.site`, `.card-face`, `.deck-card`, a
  bank) nor `printed`:

  ```js
  [...document.querySelectorAll('.mat-board *')].filter(e =>
    e.children.length === 0 && e.textContent.trim() &&
    getComputedStyle(e).opacity !== '0' &&
    !e.closest('.printed, .card-face, .deck-card, .site, .favor-bank'))
    .map(e => e.className + ': ' + e.textContent.trim())
  ```

  Record the list. The tracker's two lines are expected; anything else is
  fixed in this task and named in the report.
- [ ] **Step 2: Write the failing test.**
  `rule(".mat-board.art-ready .track-note > p")` contains
  `background: var(--base)`, `box-shadow: 0 0 0 0.25em var(--base)` and
  `border-radius: 0.2em`, and none of `padding`, `margin`, `width`,
  `height`; the halo rule from slice 2 is gone
  (`!css.contains("text-shadow: 0 0 3px var(--base)")`).
- [ ] **Step 3: Implement.** Replace the halo rule with the chip rule.
  The lines are `display: inline` already, so the ground hugs each line.
  Measure cream on Base (15.6 : 1, from slice 3).
- [ ] **Step 4: Run the stylesheet suites; commit**
  `fix(frontend): put the tracker's lines on a ground over the mat`.

---

### Task 4: The site card covers the mat's frame

Answers screenshots 10 and 13.

**Files:**
- Modify: `MatLayout.scala` (`siteCard`), `docs/catalog/asset-proportions.md`
- Modify: `frontend/styles.css`
- Test: `MatLayoutSuite.scala`, `StylesheetArtSuite.scala`,
  `SiteBoxLayoutSuite.scala` if it pins the face's rectangle

- [ ] **Step 1: Measure the frame's line.** On the scratch server, draw
  the mat to a canvas and scan across one frame edge on each side (rows
  through a frame's vertical middle, columns through its horizontal
  middle) for the run of the line's colour, as the die crops were
  measured. Record, in mat pixels, the line's outer edge on each of the
  four sides for the Cradle's first frame, and check two other frames.
  The margin `m` is the largest distance from the 669×509 frame to a
  line's outer edge, rounded up.
- [ ] **Step 2: Write the failing tests.** `MatLayout.siteCard(c, r)` is
  `siteFrame(c, r)` grown by `m` on every side; it does not overlap the
  first card slot (the gap is 48); the row rectangle `siteRow` still
  starts at the frame's left and the face is placed relative to it (a
  negative left is allowed; assert the percentages). Stylesheet:
  `rule(".site-face > .art")` contains `object-fit: cover`.
- [ ] **Step 3: Implement.** `siteCard` returns the grown frame. The row
  (`article.site`) keeps its rectangle, so cards, pawns and the option
  ring do not move; only the face grows, by `m`, on each side. The
  face's own ratio rule (`aspect-ratio: var(--ratio-site)`) gives way to
  its placed size on the mat. Update the "Site frame" rows and the
  paragraph under the table in `asset-proportions.md`, with how `m` was
  measured.
- [ ] **Step 4: Check in the browser** at three sizes that no frame line
  shows around a site with art, that the drawn face covers the same box
  with art off, and that the site's words (name band, defense, tokens)
  still sit inside the face.
- [ ] **Step 5: Run the suites; commit**
  `fix(frontend): make a site's card cover the frame the mat prints`.

---

### Task 5: No power chips over site art

Replaces R20.

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [ ] **Step 1: Rewrite the tests.** The R20 test becomes "on a ready
  site a power button's label hides with the rest of the print, and the
  button keeps its place": no rule `.site.art-ready .site-power`, no rule
  `.site.art-ready .site-power > .printed`; the "no other rule sets a
  printed element's opacity" list loses its R20 entry.
- [ ] **Step 2: See them fail; delete the two rules and their comment.**
- [ ] **Step 3: Run the stylesheet suites; commit**
  `fix(frontend): hide a site's power labels under its art`.

---

### Task 6: Favor banks that can be read

Answers screenshot 11.

**Files:**
- Modify: `MatLayout.scala`, `docs/catalog/asset-proportions.md`
- Modify: `WorldBoardRenderer.scala` (`favorBanks`)
- Modify: `frontend/styles.css`
- Test: `MatLayoutSuite.scala`, `MatBoardSuite.scala`, `WorldArtSuite.scala`,
  `StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `MatLayout.favorBankCircle(suit): Option[MatRect]`, the
  square around the mat's circle (centre from `BankCentre`, y 2155,
  radius 93); `MatLayout.favorBankEmblem(suit): Option[MatRect]`,
  measured in Step 1.
- The bank's markup: the suit glyph (printed) placed on the emblem; a
  `span.favor-bank-token` placed on the circle, holding the favor piece
  and, over it, `span.favor-bank-count`; the suit's name stays in the
  bank's words. The bank's box and its option key do not change.

- [ ] **Step 1: Measure the emblem.** Scan the mat for the emblem's
  circle beside the first bank (centre and radius), check a second bank,
  and record the rectangle relative to the bank's centre.
- [ ] **Step 2: Write the failing tests.**
  - `MatLayoutSuite`: both rectangles lie inside the bank's box for every
    suit and do not overlap each other.
  - `MatBoardSuite`: a bank holds its suit glyph placed at the emblem's
    percentages, a token box placed at the circle's, the favor piece
    filling the token box, and the count inside the token box after the
    piece; the bank's accessible words still read "Arcane: 3".
  - `WorldArtSuite`: with art, the token box holds the favor token's
    image; the world invariance test still passes.
  - Stylesheet: `rule(".mat-board .favor-bank-token > .token-glyph")`
    contains `width: 100%` and `height: 100%`;
    `rule(".mat-board .favor-bank-count")` is absolutely placed, centred,
    with `background: var(--base)`, a `border-radius: 50%` and a font
    size set from the token box (`container` units), and no padding.
- [ ] **Step 3: Implement.** Replace the bank's grid with the two placed
  children. Remove the grid rules for `.mat-board .favor-bank` and the
  compact and tiny overrides that sized its glyphs; the parts now take
  their size from the mat. Keep the slice 2 rule that drops the pill
  over a ready mat.
- [ ] **Step 4: Check in the browser** at Fit on three sizes that a
  count is at least 9px tall on a phone and reads on its disc, and that
  an offered bank still shows its ring.
- [ ] **Step 5: Run the suites; commit**
  `fix(frontend): fill a favor bank with its favor and count`.

---

### Task 7: The banner's face in its overlay

Answers screenshot 12.

**Files:**
- Modify: `CardInspection.scala` (`Request.Text`, `Power`, `Festival`,
  and their openers), `CardInspectionOverlay.scala`, `GameTableShell.scala`,
  `BannerFace.scala`
- Modify: `frontend/styles.css`
- Test: `BannerFaceSuite.scala`, `BannerConsentGridSuite.scala`,
  `PlateArtSuite.scala`, `StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `banner: Option[BannerState] = None` as the last field of
  `Request.Text`, `Request.Power` and `Request.Festival`, and the last
  parameter of `openText`, `openPower`, `openFestival`, `showText`,
  `showPower`, `showFestival`. When set, the overlay draws
  `BannerFace.render(banner, None)` first in its body, before the
  details. `BannerFace.inspect` passes its banner on all three paths.
- Existing pattern matches on the three requests gain one `_`; that
  mechanical edit to existing suites is expected and listed as a
  deviation.

- [ ] **Step 1: Write the failing tests.**
  - "a banner's overlay shows the banner's face, with its image, beside
    its rules": through the shell, for Mob (text path), a ruled banner
    (power path) and Festival; the face is
    `.card-overlay-body > .banner-face`, holds the catalog's image with
    art on and none with art off, and shows what the banner holds.
  - "a text overlay with no banner is as before".
  - Stylesheet: `rule(".card-overlay-body > .banner-face")` gives it the
    overlay's card size (`font-size: 1.9em`) and a square of the
    overlay's card width.
- [ ] **Step 2: See them fail; implement; update the pattern matches.**
- [ ] **Step 3: Run the frontend suites; commit**
  `feat(frontend): show a banner's face in its overlay`.

---

### Task 8: Empty denizen slots that show on the mat

Answers screenshot 13.

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [ ] **Step 1: Write the failing test.** The rule
  `.mat-board.art-ready .card-slot-empty:not([class*="board-option"])`
  contains `border-color: var(--cream-bright)` and
  `box-shadow: inset 0 0 0 1px var(--base)`, and no width, height,
  padding or margin. The option-target guard stays green.
- [ ] **Step 2: Implement the rule in the art block.** The border keeps
  its width and style, so the slot's box does not change.
- [ ] **Step 3: Check in the browser** that the outline reads over the
  mat's light and dark parts at Fit on three sizes; if one pixel is too
  thin at a phone's Fit, set the border's width from the map's scale
  (`calc(1px / var(--map-scale, 1))`, capped) for art on and off alike
  and pin it.
- [ ] **Step 4: Run the stylesheet suites; commit**
  `fix(frontend): make empty card slots show on the mat`.

---

### Task 9: The Oath goal's card in the shared bank

Answers screenshot 14. Amends R13 for the Oath goal.

**Files:**
- Modify: `WorldBoardRenderer.scala` (`sharedBank`)
- Modify: `frontend/styles.css`
- Test: the suite that covers the shared bank (find it with
  `grep -ln "oathkeeper-goal" frontend/src/test`), `PlateArtSuite.scala`,
  `StylesheetArtSuite.scala`, `StylesheetTableEdgeSuite.scala`

**Interfaces:**
- The goal button keeps its class `oathkeeper-goal`, its accessible name
  (the goal's title) and its click (the overlay with the goal's plate
  and art). It becomes a card box: `button.oathkeeper-goal.card-plate
  .card-plate-oath-goal` holding `span.card-name.printed` with the
  title, and the goal's image through `ArtLayer.attach`. The holder's
  line (`span.oathkeeper-holder`) follows it, under the card.
- The box is the Oath goal's ratio at the banners' height on the table's
  edge (`--banner-edge-side`), in the shared bank's row before the
  banners.

- [ ] **Step 1: Write the failing tests.**
  - "the shared bank shows the Oath goal as a card at its ratio, with
    its image": the button holds the goal's image with art on and its
    title as printed text with art off; it has the plate classes.
  - "the holder stands under the card": "Oathkeeper: unheld",
    "Oathkeeper: Red", "Usurper: Red".
  - "the goal card still opens the goal's overlay".
  - The table-edge invariance: the shared bank's markup is the same with
    art on and off once images are removed.
  - Stylesheet: the goal card's height is `var(--banner-edge-side)`, its
    `aspect-ratio` is `var(--ratio-oath-goal)`; it is in the nearest-box
    list through `.card-plate`; the old text-link rules for
    `.oathkeeper-goal` are gone.
- [ ] **Step 2: See them fail; implement.** Keep the card a `button`, so
  it is not printed and holds no control. Its title fits in the drawn
  box at the smallest size the table shows; if "Oathkeeper of the
  People" does not, set it in the compact face's way (smaller text,
  wrapped), and check at a phone's Fit.
- [ ] **Step 3: Run the suites; commit**
  `feat(frontend): show the Oath goal's card in the shared bank`.

---

### Task 10: Records, gate and browser check

- [ ] **Step 1: The records.** In the rulings file, record the rulings
  this plan's questions get (from R37), mark R20 as replaced and R13 as
  amended for the Oath goal, each with a pointer to the ruling that
  replaces it; do not delete them. In the design, amend "The surfaces"
  (the site card's bullet, the Oath goal, the banks, the round wheel).
  In `DESIGN.md`'s "Game Art": drop the site-power chips, add the round
  marker and its colour, the Usurper's token, the banks' form, the
  tracker's chips, the slots' outline and the Oath goal's card; add the
  colour to the colour tables. Commit
  `docs: record the owner review's changes to game art`.
- [ ] **Step 2: The full gate, after the last commit.**

  ```bash
  ./sbtw test
  ./sbtw frontend/test
  python3 scripts/check-architecture.py
  python3 scripts/check-markdown-links.py
  ```

- [ ] **Step 3: The browser check,** on port 8084 with a scratch
  database, at 1440×900, 1024×768 and 375×812, art on and off:
  1. The wheel: the round marker on the current wedge, the Usurper's
     token filling round 4's wedge; a screenshot beside the reference.
  2. The tracker's lines read on the mat; the audit's list is empty
     apart from them.
  3. No frame line shows around a site; drawn sites cover the same box.
  4. No chips on ready sites; the power buttons still open by keyboard.
  5. Banks: token fills the circle, count reads; an offered bank shows
     its ring if a decision that offers one can be reached.
  6. A banner's overlay shows its face.
  7. Empty slots show on the mat.
  8. The Oath goal's card in the shared bank, held and unheld.
  9. Bounding rectangles of every box in slices 2 and 3's lists, plus
     `.favor-bank-token`, `.favor-bank-count`, `.oathkeeper-goal`,
     `.round-marker` and `.limiter-marker`, are equal with art on and
     off at each size.
  10. No error in the browser console.
- [ ] **Step 4: Stop the 8084 server (not 8083) and report** commits,
  gate numbers, the browser check with screenshots, the measured
  numbers, deviations, open questions and anything left undone.

---

## Self-Review

**Coverage of the owner's nine items.**

| Item | Task |
|---|---|
| 1. Round marker on the current wedge | 1 |
| 2. Usurper's token fills its wedge | 2 |
| 3. Labels readable on the mat, with an audit | 3 |
| 4. Site box covers the mat's frame | 4 |
| 5. No power chips over site art (replaces R20) | 5 |
| 6. Favor banks readable | 6 |
| 7. Banner art in its overlay | 7 |
| 8. Empty denizen slots visible | 8 |
| 9. Oath goal image in the shared bank (amends R13) | 9 |
| Rulings, design and `DESIGN.md` kept in step | 10 |

**Placeholder scan.** Three numbers are measured during execution and
cannot be in the plan: the frame line's margin (Task 4), a bank's emblem
rectangle (Task 6) and the token's turn (Task 2). Each task's first step
is the measurement, a test pins the number, and `asset-proportions.md`
records the two mat numbers with the method. Tests are given by name and
assertion; the code that is new in kind is described at the element and
rule level.

**Type consistency.** `RoundSegment` gains `discX`, `discY`, `middle`,
`centreX`, `centreY`. `ArtLayer.attachMark(group, mark, key, x, y, width,
height, turn)`. `MatLayout.favorBankCircle(suit)` and
`favorBankEmblem(suit)` return `Option[MatRect]`. The three inspection
requests gain `banner: Option[BannerState]` last.
