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

## Rulings folded in

The orchestrator ruled on the plan's nine questions as R37 to R45, in
order (Q1 is R37 and so on); every one confirmed the plan's answer. R42
adds that a bank's count is as large as its disc allows, and R37 that
with art off the Oath goal's box holds the drawn goal at the same size.

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

- [x] **Step 1: Write the failing tests**
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
- [x] **Step 2: Run them and see them fail.**
- [x] **Step 3: Implement.** Add the two coordinates to `RoundSegment`
  from `point(74.0, middle)`. In `roundTracker`, after the current wedge
  is appended, append the circle. In `styles.css`, in the `:root` colour
  table, add `--round-marker: #cfcac0;` with a one-line comment (the
  game's silver round marker); beside `.round-current` add
  `.round-marker { fill: var(--round-marker); stroke: #6f6a61;
  stroke-width: 2; }`. In the art block add
  `.mat-board.art-ready .round-current { fill: none; }`.
  If the rim's grey is not an existing colour, use `var(--line-mid)`.
- [x] **Step 4: Run the frontend and stylesheet suites; commit**
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
and 55 tall (the file's own shape, sized in the browser to the wedge the
mat prints; the plan first said 39, the radial depth), centred on the
wedge's middle. The file stands point up, so it is turned by the wedge's
middle angle less 90 degrees and its point faces the hub.

- [x] **Step 1: Write the failing tests**
  - "the Usurper's token fills the limiter's wedge": the image's style is
    `width: 54px; height: 55px;`, its `x`/`y` centre it on
    (`centreX`, `centreY`), its `transform` is `rotate(<turn> <cx> <cy>)`.
  - "with art off the limiter's wedge is drawn as a wedge with its
    diamond": `path.limiter-marker` has the segment's `d`;
    `text.limiter-diamond` reads "◆"; the hover note is on the path.
  - "a load hides the drawn wedge and its diamond": `art-ready` on the
    path; stylesheet `rule(".round-tracker .limiter-marker.art-ready")`
    and the sibling rule for the diamond.
  - "when the limiter's round is the current round the token lies under
    the round marker": document order in the group.
- [x] **Step 2: See them fail.**
- [x] **Step 3: Implement,** then start the scratch server on 8084 and
  look: the token must sit inside the wedge's printed edges at three
  sizes, point to the hub. Correct the turn or the box by eye, update the
  test's numbers, and take a screenshot of the wheel beside the
  reference for the report.
- [x] **Step 4: Run the suites; commit**
  `fix(frontend): make the Usurper's token fill its wedge`.

---

### Task 3: Labels that read on the mat

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [x] **Step 1: Audit.** On the scratch table with art on, list every
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
- [x] **Step 2: Write the failing test.**
  `rule(".mat-board.art-ready .track-note > p")` contains
  `background: var(--base)`, `box-shadow: 0 0 0 0.25em var(--base)` and
  `border-radius: 0.2em`, and none of `padding`, `margin`, `width`,
  `height`; the halo rule from slice 2 is gone
  (`!css.contains("text-shadow: 0 0 3px var(--base)")`).
- [x] **Step 3: Implement.** Replace the halo rule with the chip rule.
  The lines are `display: inline` already, so the ground hugs each line.
  Measure cream on Base (15.6 : 1, from slice 3).
- [x] **Step 4: Run the stylesheet suites; commit**
  `fix(frontend): put the tracker's lines on a ground over the mat`.

---

### Task 4: The site card covers the mat's frame

Answers screenshots 10 and 13.

**Files:**
- Modify: `MatLayout.scala` (`siteCard`), `docs/catalog/asset-proportions.md`
- Modify: `frontend/styles.css`
- Test: `MatLayoutSuite.scala`, `StylesheetArtSuite.scala`,
  `SiteBoxLayoutSuite.scala` if it pins the face's rectangle

- [x] **Step 1: Measure the frame's line.** On the scratch server, draw
  the mat to a canvas and scan across one frame edge on each side (rows
  through a frame's vertical middle, columns through its horizontal
  middle) for the run of the line's colour, as the die crops were
  measured. Record, in mat pixels, the line's outer edge on each of the
  four sides for the Cradle's first frame, and check two other frames.
  The margin `m` is the largest distance from the 669×509 frame to a
  line's outer edge, rounded up.
- [x] **Step 2: Write the failing tests.** `MatLayout.siteCard(c, r)` is
  `siteFrame(c, r)` grown by `m` on every side; it does not overlap the
  first card slot (the gap is 48); the row rectangle `siteRow` still
  starts at the frame's left and the face is placed relative to it (a
  negative left is allowed; assert the percentages). Stylesheet:
  `rule(".site-face > .art")` contains `object-fit: cover`.
- [x] **Step 3: Implement.** `siteCard` returns the grown frame. The row
  (`article.site`) keeps its rectangle, so cards, pawns and the option
  ring do not move; only the face grows, by `m`, on each side. The
  face's own ratio rule (`aspect-ratio: var(--ratio-site)`) gives way to
  its placed size on the mat. Update the "Site frame" rows and the
  paragraph under the table in `asset-proportions.md`, with how `m` was
  measured.
- [x] **Step 4: Check in the browser** at three sizes that no frame line
  shows around a site with art, that the drawn face covers the same box
  with art off, and that the site's words (name band, defense, tokens)
  still sit inside the face.
- [x] **Step 5: Run the suites; commit**
  `fix(frontend): make a site's card cover the frame the mat prints`.

---

### Task 5: No power chips over site art

Replaces R20.

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [x] **Step 1: Rewrite the tests.** The R20 test becomes "on a ready
  site a power button's label hides with the rest of the print, and the
  button keeps its place": no rule `.site.art-ready .site-power`, no rule
  `.site.art-ready .site-power > .printed`; the "no other rule sets a
  printed element's opacity" list loses its R20 entry.
- [x] **Step 2: See them fail; delete the two rules and their comment.**
- [x] **Step 3: Run the stylesheet suites; commit**
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

- [x] **Step 1: Measure the emblem.** Scan the mat for the emblem's
  circle beside the first bank (centre and radius), check a second bank,
  and record the rectangle relative to the bank's centre.
- [x] **Step 2: Write the failing tests.**
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
- [x] **Step 3: Implement.** Replace the bank's grid with the two placed
  children. Remove the grid rules for `.mat-board .favor-bank` and the
  compact and tiny overrides that sized its glyphs; the parts now take
  their size from the mat. Keep the slice 2 rule that drops the pill
  over a ready mat.
- [x] **Step 4: Check in the browser** at Fit on three sizes that a
  count is at least 9px tall on a phone and reads on its disc, and that
  an offered bank still shows its ring.
- [x] **Step 5: Run the suites; commit**
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

- [x] **Step 1: Write the failing tests.**
  - "a banner's overlay shows the banner's face, with its image, beside
    its rules": through the shell, for Mob (text path), a ruled banner
    (power path) and Festival; the face is
    `.card-overlay-body > .banner-face`, holds the catalog's image with
    art on and none with art off, and shows what the banner holds.
  - "a text overlay with no banner is as before".
  - Stylesheet: `rule(".card-overlay-body > .banner-face")` gives it the
    overlay's card size (`font-size: 1.9em`) and a square of the
    overlay's card width.
- [x] **Step 2: See them fail; implement; update the pattern matches.**
- [x] **Step 3: Run the frontend suites; commit**
  `feat(frontend): show a banner's face in its overlay`.

---

### Task 8: Empty denizen slots that show on the mat

Answers screenshot 13.

**Files:**
- Modify: `frontend/styles.css`
- Test: `StylesheetArtSuite.scala`

- [x] **Step 1: Write the failing test.** The rule
  `.mat-board.art-ready .card-slot-empty:not([class*="board-option"])`
  contains `border-color: var(--cream-bright)` and
  `box-shadow: inset 0 0 0 1px var(--base)`, and no width, height,
  padding or margin. The option-target guard stays green.
- [x] **Step 2: Implement the rule in the art block.** The border keeps
  its width and style, so the slot's box does not change.
- [x] **Step 3: Check in the browser** that the outline reads over the
  mat's light and dark parts at Fit on three sizes; if one pixel is too
  thin at a phone's Fit, set the border's width from the map's scale
  (`calc(1px / var(--map-scale, 1))`, capped) for art on and off alike
  and pin it.
- [x] **Step 4: Run the stylesheet suites; commit**
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

- [x] **Step 1: Write the failing tests.**
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
- [x] **Step 2: See them fail; implement.** Keep the card a `button`, so
  it is not printed and holds no control. Its title fits in the drawn
  box at the smallest size the table shows; if "Oathkeeper of the
  People" does not, set it in the compact face's way (smaller text,
  wrapped), and check at a phone's Fit.
- [x] **Step 3: Run the suites; commit**
  `feat(frontend): show the Oath goal's card in the shared bank`.

---

### Task 10: Records, gate and browser check

- [x] **Step 1: The records.** In the rulings file, record the rulings
  this plan's questions get (from R37), mark R20 as replaced and R13 as
  amended for the Oath goal, each with a pointer to the ruling that
  replaces it; do not delete them. In the design, amend "The surfaces"
  (the site card's bullet, the Oath goal, the banks, the round wheel).
  In `DESIGN.md`'s "Game Art": drop the site-power chips, add the round
  marker and its colour, the Usurper's token, the banks' form, the
  tracker's chips, the slots' outline and the Oath goal's card; add the
  colour to the colour tables. Commit
  `docs: record the owner review's changes to game art`.
- [x] **Step 2: The full gate, after the last commit.**

  ```bash
  ./sbtw test
  ./sbtw frontend/test
  python3 scripts/check-architecture.py
  python3 scripts/check-markdown-links.py
  ```

- [x] **Step 3: The browser check,** on port 8084 with a scratch
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
- [x] **Step 4: Stop the 8084 server (not 8083) and report** commits,
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
