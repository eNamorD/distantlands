# Chronicle Details, Slice 4: Button and Overlay Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** A Chronicle Details button beside the Shared Bank opens a
read-only overlay. The overlay shows the Chronicle's name, the Foundations,
the era scorepad with each lineage's legacy chips, the Atlas's Recent site
drawn as the map draws a site, and the five Chronicle Phase aids.

**Architecture:** The World pane is rebuilt on every snapshot, so the
button cannot own the overlay. It calls a small `ChronicleDetails.open`
hook, on the pattern of `CardInspection.onOpen`, which `GameTableShell`
registers. `ChronicleDetailsOverlay` follows `GameLogOverlay`: an `aside`
dialog with a scrim, a Pane-fill panel and a pane header with Close. The
table behind it is inert while it is open. `TableScreen` hands it every
snapshot. While it is open it redraws through `PanelContent.replace`,
which keeps the reader's scroll and focus. `ChronicleDetailsContent`
builds the five sections from the projection with the pieces slices 1 to
3 made:
- `FoundationStrip` and `ChronicleTaskStrip` on `SetupCardStrip`;
- `WorldBoardRenderer.siteRow`;
- `ChronicleScoresheet.of(details)` with `LegacyStrip.chip`.

Cards open the existing `CardInspectionOverlay`, one z-index layer above.

**Why the Atlas box is a `.mat-board`.** `siteRow`'s layout rules apply
only under a `.mat-board` ancestor (`.mat-board .mat-slot { position:
absolute }`, `.mat-board .site`, `.mat-board .site-name`, and the other
`.mat-board .site-*` rules). `.mat-board` also brings the mat's ratio and
its painted background. The Atlas box takes the class
`mat-board chronicle-details-atlas-board`, so every site rule applies as
on the map. One rule with both classes (`.mat-board.chronicle-details-atlas-board`)
gives the box the row's width and ratio, from custom properties the code
sets from `MatLayout`, and a neutral fill (open question Q12).

**Tech Stack:** Scala.js frontend, munit, jsdom (`TestBrowser`), CSS,
`./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Chronicle Details design](../../specs/2026-11/2026-11-18-chronicle-details-design.md),
ruling D1 and section "Slice 4: button and overlay". Rulings so far are in
the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md);
new ones go there from R11. Read `DESIGN.md` ("Elevation & Depth",
"Buttons", "Panes", "Foundation Strip", "Layout") before the CSS task.

## Global Constraints

- The button: `tableEdge` draws `button.chronicle-details-button`, reading
  "Chronicle Details", beside the Shared Bank section, only when
  `chronicleDetails` is present. `sharedBank` no longer draws the
  Foundations strip.
- The overlay shell: an `aside` with `role="dialog"` and
  `aria-modal="true"`, a scrim, a pane-fill panel, and a header with the
  heading and Close.
  - Escape, Close or a click on the scrim closes it.
  - On open, focus moves to the content. On close, focus returns to the
    opener while it is still in the document, else to the rebuilt button,
    found by its class.
  - `show(projection)` runs on every snapshot. A redraw keeps the content's
    scroll position.
  - While the overlay is open, the table behind it is inert.
- Cards in the overlay open `CardInspectionOverlay`, which stacks one
  z-index layer higher. Escape while the inspector is open closes only the
  inspector; the next Escape closes the overlay.
- Read-only: the overlay's cards are drawn with `BoardOptions.none`, so a
  parked decision never marks a card inside it.
- The content, in order:
  1. A header: the heading `chronicle.name`, which labels the dialog. Under
     it, a quiet line: "Chronicle Details · Era target N".
  2. Foundations.
  3. The era scorepad. Each lineage cell holds its swatch, its name and its
     legacy chips; a click on a chip opens the card. It shows the last five
     games, with the "…" column when there are earlier ones. With no games
     played, it shows lineage rows and totals only.
  4. "Atlas · Recent": a `siteRow` in a box of the mat site row's ratio,
     with no pawns, no festival marker and no site-power links. An empty
     Atlas reads "The Atlas holds no sites."
  5. "Chronicle Phase": the five task cards.
- Styles: the panel fills the pane and scrolls inside itself, and its
  sections stack in one column. The button uses the Shared Bank's tokens
  and is full width at phone widths.
- DESIGN.md: no transition or animation; no resting shadow; the scrim is
  `#0b0a08e8`; the Control face is Raised fill, `#e8d9bb` text, Brass Line
  border, 4px radius, Pressed fill and Bright Cream border on hover; focus
  is a 2px Cream Focus outline at -3px offset; touch controls are 44px.
- The Chronicle page keeps its text list of legacies;
  `ChroniclePageSuite` passes unchanged.
- Browser check: a scratch DB server run through Bash on port 8081,
  driven with the built-in browser tools. Never `preview_start`, and never
  `var/distantlands` or another live database.
- Protected (do not change): every slice 1 to 3 file except where a task
  names it; `GameLogOverlay`; `MatLayout`; `RulesTextRenderer`; every
  golden log.
- Work in `/Users/roman/projects/distantlands/.worktrees/chronicle-details`
  on branch `chronicle-details`. `.tooling` and `node_modules` are
  symlinks; never commit them. Never commit on `0.2.0-alpha` or `main`.
  Push nothing, merge nothing.
- Stage files by name. The hook blocks `git add -A`, `git add -u`,
  `git add .`, `git add --all`, `git commit -a` and `git stash`.
- Commits use Conventional Commits with a scope and end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Run
  `git log -3` after each commit and report any commit that is not ours.
- Code, comments, docs and commit messages are plain English prose. Wrap
  Scala, CSS comments and Markdown at 80 columns. Scaladoc cites
  "Chronicle Details design, slice 4".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## Open questions (rulings pending)

The plan below is written with each recommendation applied. A different
ruling changes only the steps named.

- **Q11. Where the inspector returns focus after a redraw.** A snapshot
  while the inspector is open, over a chip, rebuilds the chip.
  `CardInspectionOverlay` looks for a rebuilt opener only inside its
  `.game-pane`, so focus would fall to the page.
  (A, recommended) The inspector also treats `.chronicle-details` as a
  container: one selector (`".game-pane, .chronicle-details"`). The
  rebuilt chip is then found by its place; failing that, the overlay's
  `.pane-heading` is used.
  (B) The overlay skips redraws while the inspector is open and catches
  up when it closes. That needs a close callback from the inspector.
  (C) Accept the focus loss.
  Affects Task 4.
- **Q12. How the Atlas box gets the site rules** (the slice 3 reviewer's
  second item). (A, recommended) The box carries
  `mat-board chronicle-details-atlas-board`. One two-class rule replaces
  the mat's width, ratio and painted background with `var(--atlas-w)`,
  `var(--atlas-ratio)` and the Base fill. The code sets both custom
  properties from `MatLayout.siteRow(0, 0)` and `MatLayout.DesignWidthPx`,
  so the ratio has one source. The site rules keep their selectors.
  (B) A new shared class on both the map board and the Atlas box, with
  every `.mat-board .site*` selector rewritten to it. That is more CSS
  churn, and it risks the map.
  Affects Tasks 3 and 5.
- **Q13. The Atlas row at narrow widths.** The site's words are sized in
  rem, as they are on the map at 100%. (A, recommended) The box is the
  width a row has on the map at 100%, about 742px, computed from
  `MatLayout`. A panel narrower than that scrolls the box sideways inside
  its frame, as the scoresheet's frame does. (B) The box shrinks to the
  panel, and the words overflow the face at phone width. (C) The box is
  scaled to fit with a measured transform, as the map viewport is. That
  is more code.
  Affects Tasks 3 and 5.
- **Q14. The Chronicle Phase in the browser check.** No route in the app
  reaches the Chronicle Phase short of playing a whole game (ROADMAP,
  "Play-test Legacies' Chronicle Phase in a browser"). (A, recommended)
  Check the overlay mid-game in the real app, on a game started from an
  imported Chronicle built for the check. It would have a scorepad of six
  games, an unseated lineage with a dormant legacy, and a Recent site
  storing a denizen, an edifice and a relic. Leave the Chronicle Phase
  faces to the projection tests (slice 1) and say so in the report. (B)
  Write a test-scope tool that journals a game at the Chronicle Phase into
  the scratch database. That is new code beyond the design.
  Affects Task 7.
- **Q15. Light and dark themes.** The app has one theme: `styles.css`
  sets `color-scheme: dark`, and no rule reads `prefers-color-scheme`.
  (A, recommended) Take screenshots in the one theme, at desktop and phone
  widths, and record that no light theme exists. (B) Take each screenshot
  under both emulated schemes, to show that nothing changes.
  Affects Task 7.
- **Q16. The panel's measure.** The log overlay's panel is
  `min(46rem, 100%)`. The Atlas row at the map's 100% is about 742px wide,
  plus 24px of padding. (A, recommended) `min(50rem, 100%)`, so the row
  fits without scrolling on a desktop. (B) The log overlay's 46rem, with
  the row scrolling sideways everywhere.
  Affects Task 5.
- **Q17. Copy and placement the design leaves open.** (A, recommended)
  - The quiet line is the first line of the scrolling content, in Ink Dim
    at 0.82rem. The header stays the 39px pane header, with the name as
    the pane heading.
  - The scorepad section is headed "Era Scorepad", in the title case of
    "Shared Bank" and "Public Ambitions".
  - "The Shared Bank's tokens" is read as the table edge's sizing: 1rem,
    and the glance size on the compact map. The button wears the Control
    face, wide (12rem at least), with a 44px floor set against the map
    scale, as the Oath goal's 24px is.

  (B) Name another reading for any of the three.
  Affects Tasks 1, 3 and 5.

## Deviations already made in this plan

- `GameTableShell.behindLog` is renamed `inertBehind`, since the Chronicle
  Details overlay makes the table inert too.
- `ScoresheetRow.legacies` becomes a `ScoresheetLegacies` enum:
  `Named(Vector[ChronicleLegacy])` for the Chronicle page, and
  `Cards(Vector[CardDetails])` for the overlay. `of(summary)` and
  `of(details)` share one private `sheet` builder.
- `LegacyStrip.chip` becomes `private[frontend]`, as the design says.
- The hook is its own file, `ChronicleDetails.scala`, so Task 1 stands
  without the overlay. `ChronicleDetailsContent` holds the five sections
  apart from the shell, so the content is tested without a browser.
- `TableScreen.render` calls the new `shell.showChronicleDetails`, since
  `GameTableShell.update` receives rendered panes, not the projection.
- Foundation rules that only the table's edge used are removed from
  `styles.css`: `.table-edge .foundations`, `.table-edge .foundation-strip`,
  `.map-compact .table-edge .foundations`, `.map-compact .foundation-card`,
  and `.foundations-title` from the compact `:is()` list. The base
  `.foundation*` rules stay, and the aids share them.

## Files

- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetails.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsContent.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsOverlay.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` (`tableEdge`, `sharedBank`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ChronicleScoresheet.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/LegacyStrip.scala` (`chip`)
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/TableScreen.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala` (`open`'s container)
- Modify: `frontend/styles.css`
- Modify test: `frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala`
- Create tests: `ChronicleDetailsScoresheetSuite.scala`,
  `ChronicleDetailsContentSuite.scala`, `ChronicleDetailsOverlaySuite.scala`
  (all in `frontend/src/test/scala/distantlands/frontend/`), and
  `src/test/scala/distantlands/server/StylesheetChronicleDetailsSuite.scala`.
- Modify docs: `DESIGN.md`, `docs/ROADMAP.md`, the spec's status line.

## Running things

- Frontend suites: `./sbtw "frontend/testOnly distantlands.frontend.PanelPlacementSuite distantlands.frontend.ChronicleDetailsScoresheetSuite distantlands.frontend.ChronicleDetailsContentSuite distantlands.frontend.ChronicleDetailsOverlaySuite distantlands.frontend.ChroniclePageSuite distantlands.frontend.GameLogOverlaySuite distantlands.frontend.ShellInspectionSuite"`.
- Stylesheet suites: `./sbtw "testOnly distantlands.server.Stylesheet*"`.
- Full gate at slice end, long ones in the background: `./sbtw compile`,
  `./sbtw test`, `./sbtw frontend/test`, `./sbtw frontend/fullOptJS`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory below was read at `9a14afc73`. Each task begins by running
its grep at HEAD. Treat what the grep shows as the list; handle a hit
missing from the task by the task's rule, and report it.

---

### Task 1: The button, through a hook; the Shared Bank drops the strip

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetails.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
- Modify test: `frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala`

**Interfaces:**
- Produces: `ChronicleDetails.onOpen(value: dom.html.Element => Unit)`,
  `ChronicleDetails.open(origin: dom.html.Element)`,
  `ChronicleDetails.clear()`; `button.chronicle-details-button` with
  `aria-controls="chronicle-details"` and `aria-haspopup="dialog"`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'FoundationStrip\.render|foundation-(card|name)|\.foundations' -- frontend/src
```
Expected:
- `WorldBoardRenderer.sharedBank`;
- `PanelPlacementSuite` (the foundations test);
- `ChroniclePageSuite:110` ("shows no foundations"; it stays);
- the `FoundationStrip` and `SetupCardStrip` suites.

- [ ] **Step 2: Write the failing tests**

In `PanelPlacementSuite.scala`, add `import Dom.{click, one}` and replace
the test "the foundations close the shared bank, and a projection with
none draws no strip" with:

```scala
  private val foundations = Vector(
    distantlands.protocol.projection.FoundationProjection(1, "normal",
      "Imperial Maps", "Fill the map."),
    distantlands.protocol.projection.FoundationProjection(3, "altered",
      "Public Ambitions", "Deal 4 legacy cards."))
  private val detailsWithNoGamesNoLineagesAndNoAtlas =
    distantlands.protocol.projection.ChronicleDetailsProjection(20,
      Vector.empty, Vector.empty, None)

  test("the shared bank holds no foundation strip, and the Chronicle " +
      "Details button stands after it only while the snapshot carries " +
      "Chronicle Details"):
    val shown = world(projection().copy(foundations = foundations,
      chronicleDetails = Some(detailsWithNoGamesNoLineagesAndNoAtlas)))
    assertEquals(Dom.all(shown, ".foundations").size, 0)
    assertEquals(Dom.all(one(shown, ".table-edge"), ":scope > *")
      .map(_.getAttribute("class")),
      Vector("shared-bank", "chronicle-details-button"))
    val button = one(shown, "button.chronicle-details-button")
    assertEquals(Vector(button.textContent, button.getAttribute("type"),
      button.getAttribute("aria-haspopup"),
      button.getAttribute("aria-controls")),
      Vector("Chronicle Details", "button", "dialog", "chronicle-details"))
    assertEquals(Dom.all(world(projection().copy(foundations = foundations)),
      ".chronicle-details-button").size, 0)

  test("the Chronicle Details button opens through the shell's hook, from " +
      "itself"):
    var origins = Vector.empty[dom.Element]
    ChronicleDetails.onOpen(origin => origins :+= origin)
    try
      val button = one(world(projection().copy(chronicleDetails =
        Some(detailsWithNoGamesNoLineagesAndNoAtlas))),
        "button.chronicle-details-button")
      click(button)
      assertEquals(origins, Vector(button))
    finally ChronicleDetails.clear()
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.PanelPlacementSuite"`
Expected: compilation fails with "Not found: ChronicleDetails".

- [ ] **Step 4: Write the hook and the button**

Create `frontend/src/main/scala/distantlands/frontend/ChronicleDetails.scala`:

```scala
package distantlands.frontend

import org.scalajs.dom

/** The slot the Chronicle Details button opens through (Chronicle Details
  * design, slice 4). The World pane is rebuilt on every snapshot and cannot
  * own the overlay, so the button asks whatever `GameTableShell`
  * registered, as a card asks `CardInspection`. */
private[frontend] object ChronicleDetails:
  private var handler = Option.empty[dom.html.Element => Unit]

  def onOpen(value: dom.html.Element => Unit): Unit = handler = Some(value)

  def clear(): Unit = handler = None

  def open(origin: dom.html.Element): Unit = handler.foreach(_(origin))
```

In `WorldBoardRenderer.scala`:
- In `tableEdge`, after `edge.appendChild(sharedBank(value, options))`:
  ```scala
    // Beside the Shared Bank, while the snapshot carries the Chronicle
    // (Chronicle Details design, slice 4).
    if value.chronicleDetails.nonEmpty then
      edge.appendChild(chronicleDetailsButton)
  ```
- After `tableEdge`, add:
  ```scala
  /** Opens the Chronicle Details overlay through the shell's hook. */
  private def chronicleDetailsButton: dom.Element =
    val node = button("Chronicle Details", "chronicle-details-button")
    node.`type` = "button"
    node.setAttribute("aria-haspopup", "dialog")
    node.setAttribute("aria-controls", "chronicle-details")
    node.onclick = _ => ChronicleDetails.open(node)
    node
  ```
- In `sharedBank`, delete the comment "The setup cards close the bank: ..."
  and the `if value.foundations.nonEmpty then` block that draws
  `FoundationStrip`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.PanelPlacementSuite distantlands.frontend.MatBoardSuite distantlands.frontend.FoundationStripSuite distantlands.frontend.ChroniclePageSuite"`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChronicleDetails.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala
git commit -m "feat(frontend): open Chronicle Details from beside the Shared Bank

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The era scorepad with legacy chips

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/ChronicleScoresheet.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/LegacyStrip.scala` (`chip`)
- Test: `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsScoresheetSuite.scala`

**Interfaces:**
- Produces: `enum ScoresheetLegacies { case Named(held: Vector[ChronicleLegacy]); case Cards(held: Vector[CardDetails]) }`;
  `ScoresheetRow(lineage: String, legacies: ScoresheetLegacies, cells: Vector[Option[ScoresheetMark]], total: Int)`;
  `ChronicleScoresheet.of(details: ChronicleDetailsProjection): Scoresheet`;
  `LegacyStrip.chip(card: CardDetails): dom.html.Button` (`private[frontend]`).

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'ScoresheetRow|ChronicleScoresheet\.|LegacyStrip\.chip|\.legacies' -- frontend/src
```
Expected: `ChronicleScoresheet.scala`, `ChroniclePage.scala` (or the file
that calls `ChronicleScoresheet.of(summary)`), `LegacyStrip.scala`, and the
Chronicle page suites. A suite that builds a `ScoresheetRow` by hand takes
`ScoresheetLegacies.Named(...)`; report it.

- [ ] **Step 2: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsScoresheetSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, click, one}
import distantlands.protocol.ChronicleScorepadEntry
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  LineageDetailsProjection}

/** The era scorepad in Chronicle Details (Chronicle Details design, slice
  * 4): the Chronicle page's sheet, with each lineage's legacies as chips
  * that open the card. */
class ChronicleDetailsScoresheetSuite extends munit.FunSuite:
  private var inspected = Vector.empty[String]

  override def beforeEach(context: BeforeEach): Unit =
    inspected = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Card(card, _) => inspected :+= card.cardId
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private val seer = CardDetails("L22", "legacy", "Seer",
    suit = Some("arcane"), orientation = Some("face-up"))
  private val legacyBack = CardDetails("hidden", "legacy", "Facedown legacy",
    orientation = Some("face-down"), hidden = true)

  /** Red, a Chancellor at 3 holding Seer and a legacy the viewer may not
    * know, and Blue, an Exile at 2 holding none, after `games` games in
    * which both scored. */
  private def redWithSeerAndABackBlueWithNone(games: Int)
      : ChronicleDetailsProjection =
    ChronicleDetailsProjection(20,
      Vector.fill(games)(Vector(
        ChronicleScorepadEntry("red-lineage", 3, Some("chancellor")),
        ChronicleScorepadEntry("blue-lineage", 2))),
      Vector(
        LineageDetailsProjection("red-lineage", 3, Some("chancellor"),
          Vector(seer, legacyBack)),
        LineageDetailsProjection("blue-lineage", 2, None, Vector.empty)),
      None)

  private def drawn(details: ChronicleDetailsProjection) =
    ChronicleScoresheet.render(ChronicleScoresheet.of(details))

  test("each lineage's cell holds its name and its legacies as chips: a " +
      "face that opens the card, and an L back"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(1))
    val red = all(sheet, "tbody tr").head
    assertEquals(one(red, ".scoresheet-lineage-name").textContent, "Red")
    val chips = all(red, ".scoresheet-legacy-chips > button")
    assertEquals(chips.map(_.getAttribute("class")),
      Vector("legacy-chip", "legacy-chip legacy-chip-back"))
    assertEquals(chips.map(_.textContent).last, "L")
    click(chips.head)
    assertEquals(inspected, Vector("L22"))
    assertEquals(all(all(sheet, "tbody tr")(1), ".scoresheet-legacy-chips")
      .size, 0)
    assertEquals(all(sheet, ".scoresheet-legacies").size, 0)

  test("with no game played the sheet has its lineage rows and totals " +
      "only"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(0))
    assertEquals(all(sheet, "thead th").map(_.textContent),
      Vector("Lineage", "Total"))
    assertEquals(all(sheet, ".scoresheet-total").map(_.textContent),
      Vector("3", "2"))

  test("seven games show the last five, after the earlier-games column"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(7))
    assertEquals(all(sheet, "thead th").map(_.textContent),
      Vector("Lineage", "", "Game 3", "Game 4", "Game 5", "Game 6", "Game 7",
        "Total"))
    assertEquals(all(sheet, "tbody td.scoresheet-earlier")
      .map(_.textContent), Vector("…", "…"))
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsScoresheetSuite"`
Expected: compilation fails, because `ChronicleScoresheet.of` has no
alternative taking `ChronicleDetailsProjection`.

- [ ] **Step 4: Add the second source and the chips**

In `LegacyStrip.scala`, change `private def chip` to
`private[frontend] def chip`, and add to its doc comment "Chronicle
Details' scorepad draws the same chip (Chronicle Details design, slice
4)."

In `ChronicleScoresheet.scala`:
- Import `distantlands.protocol.ChronicleScorepadEntry` and
  `distantlands.protocol.projection.ChronicleDetailsProjection`.
- Before `ScoresheetRow`, add:
  ```scala
  /** A lineage's legacies as a scoresheet row shows them: named in words on
    * the Chronicle page, as chips that open the card in Chronicle Details
    * (Chronicle Details design, slice 4). */
  private[frontend] enum ScoresheetLegacies:
    case Named(held: Vector[ChronicleLegacy])
    case Cards(held: Vector[CardDetails])
  ```
- Change `ScoresheetRow`'s second field to `legacies: ScoresheetLegacies`.
- Replace `of(summary)` with:
  ```scala
  def of(summary: ChronicleSummary): Scoresheet =
    sheet(summary.scorepad, summary.scores.map(score => (score.lineage,
      ScoresheetLegacies.Named(score.legacies), score.score)))

  /** The live scorepad of the game in play, its legacies as the viewer may
    * see them (Chronicle Details design, slice 4). */
  def of(details: ChronicleDetailsProjection): Scoresheet =
    sheet(details.scorepad, details.lineages.map(lineage =>
      (lineage.lineage, ScoresheetLegacies.Cards(lineage.legacies),
        lineage.score)))

  private def sheet(scorepad: Vector[Vector[ChronicleScorepadEntry]],
      lineages: Vector[(String, ScoresheetLegacies, Int)]): Scoresheet =
    val shown = scorepad.zipWithIndex.takeRight(ShownGames)
    Scoresheet(scorepad.size > ShownGames, shown.map(_._2 + 1),
      lineages.map((lineage, legacies, total) => ScoresheetRow(lineage,
        legacies, shown.map { case (column, _) =>
          column.find(_.lineage == lineage)
            .map(mark => ScoresheetMark(mark.score, mark.role))
        }, total)))
  ```
- In `lineageCell`, replace the `if row.legacies.nonEmpty then ...` block
  with:
  ```scala
    row.legacies match
      case ScoresheetLegacies.Named(held) if held.nonEmpty =>
        cell.appendChild(text("span", "scoresheet-legacies",
          ChronicleText.legacies(held)))
      case ScoresheetLegacies.Cards(held) if held.nonEmpty =>
        val chips = element("span", "scoresheet-legacy-chips")
        held.foreach(card => chips.appendChild(LegacyStrip.chip(card)))
        cell.appendChild(chips)
      case _ => ()
  ```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsScoresheetSuite distantlands.frontend.ChroniclePageSuite distantlands.frontend.LegacyStripSuite"`
Expected: all pass, `ChroniclePageSuite` unchanged.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChronicleScoresheet.scala \
  frontend/src/main/scala/distantlands/frontend/LegacyStrip.scala \
  frontend/src/test/scala/distantlands/frontend/ChronicleDetailsScoresheetSuite.scala
git commit -m "feat(frontend): draw the live era scorepad with legacy chips

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: The overlay's content

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsContent.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsContentSuite.scala`

**Interfaces:**
- Consumes: `FoundationStrip.ofGame` and `render`,
  `ChronicleTaskStrip.render()`, `ChronicleScoresheet.of(details)`,
  `WorldBoardRenderer.siteRow`.
- Produces: `ChronicleDetailsContent.render(value: GameProjection): dom.Element`
  (`div.chronicle-details-body`).

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'def siteRow|def render\(\): dom.Element|DesignWidthPx' -- frontend/src/main
```
Expected: `WorldBoardRenderer.siteRow`, `ChronicleTaskStrip.render()`,
and `MatLayout.DesignWidthPx` with its uses.

- [ ] **Step 2: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsContentSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one}
import TestSites.*
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  FoundationProjection}
import org.scalajs.dom

/** What Chronicle Details shows, top to bottom (Chronicle Details design,
  * slice 4): the quiet line, the Foundations, the era scorepad, the
  * Atlas's Recent site and the Chronicle Phase aids. */
class ChronicleDetailsContentSuite extends munit.FunSuite:
  private val deepWoodsStoringAKnownDenizen = quietSite("site:deep-woods",
    "Deep Woods").withDenizens(GameSiteCard("208", "Wizard's Conclave",
      Some(CardDetails("208", "denizen", "Wizard's Conclave",
        orientation = Some("face-up")))))

  /** A game with one foundation, no game on the scorepad, no lineage, and
    * `recent` at the Atlas's Recent end. */
  private def gameWithRecent(recent: Option[GameSite]): GameProjection =
    TestProjections.actingProjection().copy(
      foundations = Vector(FoundationProjection(1, "normal",
        "Imperial Maps", "Fill the map.")),
      chronicleDetails = Some(ChronicleDetailsProjection(20, Vector.empty,
        Vector.empty, recent)))

  test("the five sections stand in order, the quiet line naming the era " +
      "target"):
    val body = ChronicleDetailsContent.render(
      gameWithRecent(Some(deepWoodsStoringAKnownDenizen)))
    assertEquals(all(body, ":scope > *").map(_.getAttribute("class")),
      Vector("chronicle-details-subtitle", "foundations",
        "chronicle-details-scorepad", "chronicle-details-atlas",
        "chronicle-tasks"))
    assertEquals(one(body, ".chronicle-details-subtitle").textContent,
      "Chronicle Details · Era target 20")
    assertEquals(all(body, ":scope > * > h3").map(_.textContent),
      Vector("Foundations", "Era Scorepad", "Atlas · Recent",
        "Chronicle Phase"))

  test("the Recent site is a mat row in a mat-board box of the row's width " +
      "and ratio, drawn with nothing to choose (R12)"):
    val body = ChronicleDetailsContent.render(
      gameWithRecent(Some(deepWoodsStoringAKnownDenizen)))
    val board = one(body,
      ".chronicle-details-atlas-frame > .chronicle-details-atlas-board")
    // The site rules apply under a .mat-board.
    assert(board.matches(".mat-board"), board.getAttribute("class"))
    val row = MatLayout.siteRow(0, 0)
    val style = board.asInstanceOf[dom.html.Element].style
    assertEquals(style.getPropertyValue("--atlas-w"),
      f"${row.w / MatLayout.Width * MatLayout.DesignWidthPx}%.2fpx")
    assertEquals(style.getPropertyValue("--atlas-ratio"),
      s"${row.w} / ${row.h}")
    val site = one(board, ":scope > article.site")
    assertEquals(site.getAttribute("aria-label"), "Deep Woods")
    assertEquals(all(site, ".site-pawns > *").size, 0)
    assertEquals(all(body, ".board-option, [data-option-ref]").size, 0)

  test("an empty Atlas reads that it holds no sites"):
    val atlas = one(ChronicleDetailsContent.render(gameWithRecent(None)),
      ".chronicle-details-atlas")
    assertEquals(all(atlas, ".chronicle-details-atlas-board").size, 0)
    assertEquals(one(atlas, "p.empty-state").textContent,
      "The Atlas holds no sites.")
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsContentSuite"`
Expected: compilation fails with "Not found: ChronicleDetailsContent".

- [ ] **Step 4: Write the content**

Create `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsContent.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.projection.ChronicleDetailsProjection
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** What Chronicle Details shows, top to bottom (Chronicle Details design,
  * slice 4): the quiet line under the heading, the Foundations, the era
  * scorepad, the Atlas's Recent site and the Chronicle Phase aids.
  * Read-only: every card is drawn with `BoardOptions.none`, so a parked
  * decision marks nothing here. */
private[frontend] object ChronicleDetailsContent:
  /** The row the Atlas's site is drawn in. Every mat row lays out its face
    * and slots alike, so the Cradle's first serves. */
  private val AtlasRow = MatLayout.siteRow(0, 0)

  def render(value: GameProjection): dom.Element =
    val body = element("div", "chronicle-details-body")
    value.chronicleDetails.foreach { details =>
      body.appendChild(text("p", "chronicle-details-subtitle",
        s"Chronicle Details · Era target ${details.eraTarget}"))
      body.appendChild(FoundationStrip.render(
        FoundationStrip.ofGame(value.foundations)))
      body.appendChild(scorepad(details))
      body.appendChild(atlas(details))
      body.appendChild(ChronicleTaskStrip.render())
    }
    body

  private def section(className: String, heading: String): dom.Element =
    val node = element("section", className)
    node.setAttribute("aria-label", heading)
    node.appendChild(text("h3", "chronicle-details-title", heading))
    node

  private def scorepad(details: ChronicleDetailsProjection): dom.Element =
    val node = section("chronicle-details-scorepad", "Era Scorepad")
    node.appendChild(ChronicleScoresheet.render(
      ChronicleScoresheet.of(details)))
    node

  /** The Recent site in a `.mat-board` box, so the map's site rules apply,
    * at the width and ratio a row has on the map at 100% (implementation
    * rulings R12 and R13). No pawns, no festival marker, no site-power
    * links: its powers are read in the inspector, not used. */
  private def atlas(details: ChronicleDetailsProjection): dom.Element =
    val node = section("chronicle-details-atlas", "Atlas · Recent")
    details.atlasRecent match
      case Some(site) =>
        val frame = element("div", "chronicle-details-atlas-frame")
        val board = element("div", "mat-board chronicle-details-atlas-board")
        val style = board.asInstanceOf[dom.html.Element].style
        val width = AtlasRow.w / MatLayout.Width * MatLayout.DesignWidthPx
        style.setProperty("--atlas-w", f"$width%.2fpx")
        style.setProperty("--atlas-ratio", s"${AtlasRow.w} / ${AtlasRow.h}")
        board.appendChild(WorldBoardRenderer.siteRow(site, 0, 0, AtlasRow,
          BoardOptions.none, SitePowerLinks.none, Vector.empty))
        frame.appendChild(board)
        node.appendChild(frame)
      case None =>
        node.appendChild(text("p", "empty-state",
          "The Atlas holds no sites."))
    node
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsContentSuite"`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChronicleDetailsContent.scala \
  frontend/src/test/scala/distantlands/frontend/ChronicleDetailsContentSuite.scala
git commit -m "feat(frontend): lay out the Chronicle Details content

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The overlay shell in the table

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsOverlay.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/TableScreen.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala` (`open`)
- Test: `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsOverlaySuite.scala`

**Interfaces:**
- Consumes: `ChronicleDetails` (Task 1), `ChronicleDetailsContent.render`
  (Task 3).
- Produces: `ChronicleDetailsOverlay(root, inspectorOpen: () => Boolean, opened: () => Unit, closed: () => Unit)`
  with `isOpen`, `show(projection: Option[GameProjection])`,
  `open(origin: dom.html.Element)`, `hide()`, `dispose()`;
  `GameTableShell.showChronicleDetails(projection: Option[GameProjection])`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'behindLog|closest\("\.game-pane"\)|shell\.showLog' -- frontend/src/main
```
Expected: `GameTableShell` (`behindLog`, its definition and two calls),
`CardInspectionOverlay.open`, and `TableScreen`.

- [ ] **Step 2: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/ChronicleDetailsOverlaySuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, click, one, press}
import distantlands.model.PlayerColor
import distantlands.protocol.ChronicleScorepadEntry
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  ChronicleProjection, LineageDetailsProjection}
import org.scalajs.dom

/** Chronicle Details over the table (Chronicle Details design, slice 4): a
  * modal dialog, current on every snapshot, under the card inspector. Runs
  * the real shell in `TestBrowser`. */
class ChronicleDetailsOverlaySuite extends munit.FunSuite:
  private val seer = CardDetails("L22", "legacy", "Seer",
    suit = Some("arcane"), orientation = Some("face-up"))

  /** Red's game in the Ashen Crown Chronicle: one game on the scorepad, Red
    * the Chancellor holding Seer, and an empty Atlas. */
  private val ashenCrownWithRedHoldingSeer: GameProjection =
    TestProjections.actingProjection(
      players = Vector(GamePlayer("red", "Red", "Chancellor",
        PlayerColor.Red)),
      world = Vector(GameRegion("cradle", Vector.empty))).copy(
      chronicle = Some(ChronicleProjection("Ashen Crown", "d-1", None)),
      chronicleDetails = Some(ChronicleDetailsProjection(20,
        Vector(Vector(ChronicleScorepadEntry("red-lineage", 3,
          Some("chancellor")))),
        Vector(LineageDetailsProjection("red-lineage", 3,
          Some("chancellor"), Vector(seer))), None)))

  private def withShell(body: (TestBrowser, GameTableShell) => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try body(browser, shell)
    finally
      shell.dispose()
      browser.close()

  /** A snapshot of `value` reaching the shell, as `TableScreen` hands it:
    * the World pane rebuilt, then the overlay told. */
  private def snapshot(shell: GameTableShell, value: GameProjection): Unit =
    shell.update("g", "k", dom.document.createElement("div"),
      WorldBoardRenderer.world(value, BoardOptions.none),
      dom.document.createElement("div"), dom.document.createElement("div"))
    shell.showChronicleDetails(Some(value))

  private def overlay(browser: TestBrowser): dom.Element =
    one(browser.mount, "aside.chronicle-details")
  private def content(browser: TestBrowser): dom.html.Element =
    one(browser.mount, ".chronicle-details-content")
      .asInstanceOf[dom.html.Element]
  private def detailsButton(browser: TestBrowser): dom.Element =
    one(browser.mount, "button.chronicle-details-button")
  private def table(browser: TestBrowser): dom.Element =
    one(browser.mount, ".game-table")

  test("the button opens a modal dialog named by the Chronicle, the table " +
      "inert behind it and focus in its content"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      assert(overlay(browser).hasAttribute("hidden"))
      click(detailsButton(browser))
      val node = overlay(browser)
      assert(!node.hasAttribute("hidden"))
      assertEquals(Vector(node.getAttribute("role"),
        node.getAttribute("aria-modal"), node.id),
        Vector("dialog", "true", "chronicle-details"))
      assertEquals(one(browser.mount,
        s"#${node.getAttribute("aria-labelledby")}").textContent,
        "Ashen Crown")
      assert(table(browser).hasAttribute("inert"))
      assertEquals(dom.document.activeElement, content(browser))
      assertEquals(all(content(browser), ".chronicle-details-body").size, 1)
    }

  test("Escape, Close and the scrim close it, the table usable again and " +
      "focus back on the button"):
    Vector[TestBrowser => Unit](
      browser => press(dom.document.body, "Escape"),
      browser => click(one(browser.mount, ".chronicle-details-close")),
      browser => click(overlay(browser))).foreach { closing =>
      withShell { (browser, shell) =>
        snapshot(shell, ashenCrownWithRedHoldingSeer)
        click(detailsButton(browser))
        closing(browser)
        assert(overlay(browser).hasAttribute("hidden"))
        assert(!table(browser).hasAttribute("inert"))
        assertEquals(dom.document.activeElement, detailsButton(browser))
      }
    }

  test("a click inside the panel does not close it"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      click(content(browser))
      assert(!overlay(browser).hasAttribute("hidden"))
    }

  test("Escape with the inspector open closes only the inspector; a second " +
      "closes the overlay, and focus returns to the rebuilt button"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      val first = detailsButton(browser)
      click(first)
      click(one(content(browser), "button.legacy-chip"))
      val inspector = one(browser.mount, ".card-overlay")
      assert(!inspector.hasAttribute("hidden"))
      press(dom.document.activeElement, "Escape")
      assert(inspector.hasAttribute("hidden"))
      assert(!overlay(browser).hasAttribute("hidden"))
      // A snapshot rebuilds the World pane, and the button with it.
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      press(content(browser), "Escape")
      assert(overlay(browser).hasAttribute("hidden"))
      assertNotEquals(detailsButton(browser), first)
      assertEquals(dom.document.activeElement, detailsButton(browser))
    }

  test("a snapshot redraws the open overlay in place, keeping its scroll, " +
      "and the inspector returns focus to the rebuilt chip (R11)"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      content(browser).scrollTop = 40
      val chip = one(content(browser), "button.legacy-chip")
      click(chip)
      snapshot(shell, ashenCrownWithRedHoldingSeer.copy(chronicle =
        Some(ChronicleProjection("Ashen Crown II", "d-1", None))))
      assertEquals(content(browser).scrollTop, 40.0)
      assertEquals(one(browser.mount, "#chronicle-details-heading")
        .textContent, "Ashen Crown II")
      press(dom.document.activeElement, "Escape")
      val rebuilt = one(content(browser), "button.legacy-chip")
      assertNotEquals(rebuilt, chip)
      assertEquals(dom.document.activeElement, rebuilt)
    }

  test("an open overlay closes when a snapshot carries no Chronicle " +
      "Details, and a new opening starts at the top"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      shell.showChronicleDetails(None)
      assert(overlay(browser).hasAttribute("hidden"))
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      content(browser).scrollTop = 40
      press(content(browser), "Escape")
      click(detailsButton(browser))
      assertEquals(content(browser).scrollTop, 0.0)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsOverlaySuite"`
Expected: compilation fails with "value showChronicleDetails is not a
member of distantlands.frontend.GameTableShell".

- [ ] **Step 4: Write the overlay and wire it**

Create `frontend/src/main/scala/distantlands/frontend/ChronicleDetailsOverlay.scala`:

```scala
package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport._

/** Chronicle Details over the table (Chronicle Details design, slice 4), on
  * the log overlay's idiom: a scrim, a pane-fill panel and a header with
  * the Chronicle's name and Close. Escape, Close or a click on the scrim
  * close it. Focus goes to the content on open, and back to the opener on
  * close, or to the rebuilt Chronicle Details button when a snapshot has
  * replaced the opener. It is fed every snapshot and redraws while open,
  * keeping the reader's scroll and focus. The card inspector stacks above
  * it and closes first: `inspectorOpen` says when it is open. `opened` and
  * `closed` let the shell make the table inert behind it.
  */
private[frontend] final class ChronicleDetailsOverlay(root: dom.Element,
    inspectorOpen: () => Boolean, opened: () => Unit, closed: () => Unit):
  private val node = element("aside", "chronicle-details")
    .asInstanceOf[dom.html.Element]
  node.id = "chronicle-details"
  node.setAttribute("hidden", "")
  node.setAttribute("role", "dialog")
  node.setAttribute("aria-modal", "true")
  node.setAttribute("aria-labelledby", "chronicle-details-heading")
  private val panel = element("div", "chronicle-details-panel")
  private val header = element("div", "pane-header")
  private val heading = text("h2", "pane-heading", "Chronicle Details")
    .asInstanceOf[dom.html.Element]
  heading.id = "chronicle-details-heading"
  heading.tabIndex = -1
  private val close = button("Close", "chronicle-details-close")
  private val content = element("div",
    "pane-content chronicle-details-content").asInstanceOf[dom.html.Element]
  content.tabIndex = 0
  content.setAttribute("aria-label", "Chronicle Details contents")
  header.appendChild(heading)
  header.appendChild(close)
  panel.appendChild(header)
  panel.appendChild(content)
  node.appendChild(panel)
  root.appendChild(node)
  private var latest = Option.empty[GameProjection]
  private var opener = Option.empty[dom.html.Element]

  def isOpen: Boolean = !node.hasAttribute("hidden")

  /** Every snapshot: the latest one carrying Chronicle Details. An open
    * overlay redraws from it, or closes when the snapshot carries none. */
  def show(projection: Option[GameProjection]): Unit =
    latest = projection.filter(_.chronicleDetails.nonEmpty)
    if isOpen then latest.fold(hide())(draw(_, resetScroll = false))

  def open(origin: dom.html.Element): Unit =
    latest.foreach { value =>
      opener = Some(origin)
      draw(value, resetScroll = true)
      node.removeAttribute("hidden")
      opened()
      content.focus()
    }

  private def draw(value: GameProjection, resetScroll: Boolean): Unit =
    heading.textContent = value.chronicle.fold("Chronicle Details")(_.name)
    PanelContent.replace(content, ChronicleDetailsContent.render(value),
      heading, resetScroll)

  def hide(): Unit =
    if isOpen then
      node.setAttribute("hidden", "")
      // Before the focus moves: the owner makes the table usable again.
      closed()
      opener.filter(dom.document.contains)
        .orElse(Option(dom.document.querySelector(
          ".chronicle-details-button")).map(_.asInstanceOf[dom.html.Element]))
        .foreach(_.focus())
      opener = None

  private val dismiss: dom.MouseEvent => Unit = event =>
    if event.target == node || event.target == close then hide()

  // On the document, as the log overlay's, so Escape closes it from the
  // page too. The inspector above it closes first: its handler marks the
  // event handled, and while it is open this one waits.
  private val escape: dom.KeyboardEvent => Unit = event =>
    if event.key == "Escape" && isOpen && !event.defaultPrevented &&
        !inspectorOpen() then
      event.preventDefault()
      hide()

  node.addEventListener("click", dismiss)
  dom.document.addEventListener("keydown", escape)

  def dispose(): Unit =
    node.removeEventListener("click", dismiss)
    dom.document.removeEventListener("keydown", escape)
    node.remove()
```

In `GameTableShell.scala`:
- Rename `behindLog` to `inertBehind` at its definition and both calls.
  Change its doc comment's first sentence to "The log overlay and
  Chronicle Details are modal: while either is open the table and the
  developer panel behind it are inert, so Tab cannot leave it and no
  second dialog opens under it."
- After the `CardInspection.onOpen` block, add:
  ```scala
  // Chronicle Details (Chronicle Details design, slice 4): modal as the
  // log overlay is, one layer under the card overlay, which closes first.
  private val chronicleDetails = new ChronicleDetailsOverlay(mount,
    () => inspector.isOpen, () => inertBehind(true),
    () => inertBehind(false))
  ChronicleDetails.onOpen { origin =>
    if inspector.isOpen then inspector.hide()
    chronicleDetails.open(origin)
  }

  /** Every snapshot reaches Chronicle Details, so an open one stays
    * current. */
  def showChronicleDetails(projection: Option[GameProjection]): Unit =
    chronicleDetails.show(projection)
  ```
- In `dispose()`, after `CardInspection.clear()`, add
  `ChronicleDetails.clear()` and `chronicleDetails.dispose()`.

In `TableScreen.scala`, after the `shell.showLog(...)` call, add
`shell.showChronicleDetails(projection)`.

In `CardInspectionOverlay.scala`, in `open`, change
`Option(origin.closest(".game-pane"))` to
`Option(origin.closest(".game-pane, .chronicle-details"))`, and in the
`Opener` doc comment, after "`pane` is the opener's `.game-pane`", add
"or Chronicle Details, which a snapshot also redraws (Chronicle Details
implementation ruling R11)".

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleDetailsOverlaySuite distantlands.frontend.GameLogOverlaySuite distantlands.frontend.ShellInspectionSuite distantlands.frontend.PhonePaneSwitchSuite distantlands.frontend.TablePaneOrderSuite"`
then `./sbtw frontend/test` in the background.
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChronicleDetailsOverlay.scala \
  frontend/src/main/scala/distantlands/frontend/GameTableShell.scala \
  frontend/src/main/scala/distantlands/frontend/TableScreen.scala \
  frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala \
  frontend/src/test/scala/distantlands/frontend/ChronicleDetailsOverlaySuite.scala
git commit -m "feat(frontend): show Chronicle Details over the table

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: Styles

**Files:**
- Modify: `frontend/styles.css`
- Test: `src/test/scala/distantlands/server/StylesheetChronicleDetailsSuite.scala`

- [ ] **Step 1: Re-inventory**

Run:
```bash
grep -n -E 'table-edge \.foundation|map-compact \.foundation-card|foundations-title\) \{|^\.foundation|^button\.foundation|log-overlay-close|z-index: 3[01]0' frontend/styles.css
```
Expected: the table edge's foundation rules (lines about 571-587), the
compact `:is()` list ending `.foundations-title) {`, the base
`.foundation*` rules (about 1573-1594), the three Control selector lists
naming `.log-overlay-close` (about 1801, 1807-1809 and 2376), and the two
overlay layers (`.log-overlay` 300, `.card-overlay` 310).

- [ ] **Step 2: Write the failing test**

Create `src/test/scala/distantlands/server/StylesheetChronicleDetailsSuite.scala`:

```scala
package distantlands.server

import java.nio.file.{Files, Path}

/** Chronicle Details' styles (Chronicle Details design, slice 4): the log
  * overlay's layer and scrim under the card overlay, a mat row's box for
  * the Atlas, the foundation strip's face for the aids, and the button on
  * the table's edge. */
class StylesheetChronicleDetailsSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule whose selector is `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val at = css.indexOf(s"$selector {")
    assert(at >= 0, s"no rule $selector")
    css.substring(at, css.indexOf("}", at))

  test("the overlay takes the log overlay's layer and scrim, one step " +
      "under the card overlay"):
    val overlay = rule(".chronicle-details")
    assert(overlay.contains("position: fixed; z-index: 300;"), overlay)
    assert(overlay.contains("background: #0b0a08e8;"), overlay)
    assert(rule(".card-overlay").contains("z-index: 310;"))
    assert(css.contains(".chronicle-details[hidden] { display: none; }"))

  test("the panel fills the pane's height at a 50rem measure (R16)"):
    val panel = rule(".chronicle-details-panel")
    assert(panel.contains("width: min(50rem, 100%);"), panel)
    assert(panel.contains("background: var(--pane);"), panel)

  test("the Atlas box is a mat board of the row's width and ratio on the " +
      "Base fill (R12)"):
    val box = rule(".mat-board.chronicle-details-atlas-board")
    assert(box.contains("width: var(--atlas-w);"), box)
    assert(box.contains("aspect-ratio: var(--atlas-ratio);"), box)
    assert(box.contains("background: var(--base);"), box)
    assert(rule(".chronicle-details-atlas-frame").contains("overflow-x: auto;"))

  test("the aids wear the foundation strip's face, and the table's edge " +
      "keeps no foundation rule"):
    assert(css.contains(".foundation-card, .chronicle-task-card {"))
    assert(css.contains(".foundation-strip, .chronicle-task-strip {"))
    assert(!css.contains(".table-edge .foundations"))
    assert(!css.contains(".map-compact .foundation-card"))

  test("the button is a wide Control on the edge, full width on a phone"):
    val button = rule(".table-edge .chronicle-details-button")
    assert(button.contains("min-width: 12rem;"), button)
    assert(button.contains("border: 1px solid var(--brass-line);"), button)
    assert(css.contains("@media (max-width: 620px) {\n  .table-edge " +
      "{ flex-wrap: wrap; }\n  .table-edge .chronicle-details-button " +
      "{ flex: 1 1 100%; }\n}"))
    assert(rule(".map-compact .table-edge .chronicle-details-button")
      .contains("font-size: var(--glance-text);"))
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.server.StylesheetChronicleDetailsSuite"`
Expected: FAIL with "no rule .chronicle-details".

- [ ] **Step 4: Write the styles**

In `frontend/styles.css`:

1. Table edge: replace the comment above `.table-edge` with "Beside the
   mat, as the physical table has them: the Public Ambitions, then the
   oath and the banners, then the Chronicle Details button." Delete
   `.table-edge .foundations { ... }`,
   `.table-edge .foundation-strip { ... }`, the comment "Set larger, the
   foundations take a row of their own, ..." with
   `.map-compact .table-edge .foundations { ... }`, and `.foundations-title`
   from the compact `:is()` list. After
   `.table-edge .oathkeeper-status > .oathkeeper-goal { ... }`, add:
   ```css
   /* The Chronicle Details button beside the Shared Bank (Chronicle Details
      design, slice 4): a wide Control at the edge's own sizes, its touch
      floor set against the scale as the Oath goal's is. */
   .table-edge .chronicle-details-button { flex: 0 0 auto; min-width: 12rem;
     min-height: calc(44px / var(--map-scale, 1)); padding: 8px 16px;
     color: #e8d9bb; background: var(--raised);
     border: 1px solid var(--brass-line); border-radius: 4px; font: inherit;
     font-size: 1rem; }
   .table-edge .chronicle-details-button:hover {
     border-color: var(--cream-bright); background: var(--pressed); }
   .map-compact .table-edge .chronicle-details-button {
     font-size: var(--glance-text); }
   @media (max-width: 620px) {
     .table-edge { flex-wrap: wrap; }
     .table-edge .chronicle-details-button { flex: 1 1 100%; }
   }
   ```
2. Foundation strip: extend the base rules to the aids. Change the
   selectors and leave the declarations as they are:
   - `.foundations-title` becomes `.foundations-title, .chronicle-tasks-title`.
   - `.foundation-strip` becomes `.foundation-strip, .chronicle-task-strip`.
   - `.foundation-card` becomes `.foundation-card, .chronicle-task-card`.
   - `button.foundation-card` becomes
     `button.foundation-card, button.chronicle-task-card`.
   - `button.foundation-card:hover` becomes
     `button.foundation-card:hover, button.chronicle-task-card:hover`.
   - `.foundation-name` becomes `.foundation-name, .chronicle-task-name`.

   Delete the comment "On the zoomed-out map the cards hold the glance
   text size, ..." and `.map-compact .foundation-card { ... }`. Then add:
   ```css
   /* A task card has no numeral: its name takes the whole row. */
   .chronicle-task-card { grid-template-columns: minmax(0, 1fr); }
   ```
3. Controls: add `.chronicle-details-close` to the three Control lists
   that name `.log-overlay-close`: the face (about line 1801), its hover
   (about 1807-1809) and the coarse-pointer floor (about 2376).
4. After the `.log-overlay` block, add:
   ```css
   /* Chronicle Details (Chronicle Details design, slice 4): the log
      overlay's layer, scrim and pane-fill panel, full height, at a measure
      a mat row fits in (R16). It sits outside the table, so it names its
      own button cursor and focus ring. Its sections stack in one column
      and scroll inside the panel. */
   .chronicle-details { position: fixed; z-index: 300; inset: 0;
     display: flex; justify-content: center; padding: 4vmin 16px;
     background: #0b0a08e8; }
   .chronicle-details[hidden] { display: none; }
   .chronicle-details-panel { display: flex; flex-direction: column;
     min-height: 0; width: min(50rem, 100%); background: var(--pane);
     border: 1px solid var(--line-pane); }
   .chronicle-details button { cursor: pointer; }
   .chronicle-details button:focus-visible,
   .chronicle-details [tabindex]:focus-visible {
     outline: 2px solid var(--cream-focus); outline-offset: -3px; }
   .chronicle-details-body { display: grid; gap: 18px; }
   .chronicle-details-subtitle { margin: 0; color: var(--ink-dim);
     font-size: 0.82rem; }
   .chronicle-details-title { margin: 0 0 6px; font-size: 1rem; }
   .chronicle-details .foundations { margin-top: 0; }
   .chronicle-details .foundation-strip, .chronicle-task-strip {
     grid-template-columns: repeat(3, minmax(0, 1fr)); }
   .scoresheet-legacy-chips { display: flex; flex-wrap: wrap; gap: 4px;
     margin-top: 4px; }
   /* The Atlas's Recent site: one mat row as the map draws it at 100%, in a
      .mat-board box so the site rules apply, at the row's width and ratio
      on the Base fill. The Atlas is a box, not a region (R12). A narrower
      panel scrolls the row sideways (R13). */
   .chronicle-details-atlas-frame { overflow-x: auto;
     scrollbar-width: thin; }
   .mat-board.chronicle-details-atlas-board { width: var(--atlas-w);
     aspect-ratio: var(--atlas-ratio); background: var(--base); }
   /* A phone has room for one aid or foundation a row. */
   @media (max-width: 620px) {
     .chronicle-details .foundation-strip, .chronicle-task-strip {
       grid-template-columns: minmax(0, 1fr); }
   }
   ```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.server.Stylesheet*"` and
`./sbtw "frontend/testOnly distantlands.frontend.FoundationStripSuite distantlands.frontend.PanelPlacementSuite"`
Expected: all pass, including `StylesheetTableEdgeSuite` and
`StylesheetProportionsSuite` unchanged.

- [ ] **Step 6: Commit**

```bash
git add frontend/styles.css \
  src/test/scala/distantlands/server/StylesheetChronicleDetailsSuite.scala
git commit -m "feat(frontend): style the Chronicle Details overlay and button

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Docs

**Files:**
- Modify: `DESIGN.md` ("Layout", "Elevation & Depth", "Panes",
  "Foundation Strip", "Don't")
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md` (status line)

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E "foundations in one row|four of those|card and log overlays|Show the Atlas's first site|Move the foundation cards|close the shared bank" -- DESIGN.md docs/ROADMAP.md
```
Expected: the `DESIGN.md` Layout paragraph ("then the oath, the banners
and the foundations in one row"), "Elevation & Depth" ("there are four of
those" and the Overlay scrim line), the Don't list ("only the card and log
overlays scrim"), "Foundation Strip" ("On the table they close the shared
bank"), and the two ROADMAP items.

- [ ] **Step 2: Update the docs**

- `DESIGN.md`, Layout: "then the oath, the banners and the foundations in
  one row" becomes "then the oath and the banners, and the Chronicle
  Details button, in one row".
- "Elevation & Depth":
  - The Overlay scrim line becomes "the card inspection overlay, the log
    overlay and Chronicle Details darken the table instead of casting a
    shadow."
  - The Flat Board Rule's "there are four of those: ... and the card and
    log overlays' scrim" becomes "five: ... and the card, log and
    Chronicle Details overlays' scrim".
- Don't: "only the card and log overlays scrim" becomes "only the card,
  log and Chronicle Details overlays scrim".
- "Panes": after the log overlay paragraph, add:
  > The **Chronicle Details** overlay gathers the Chronicle's meta
  > information over the table: the log overlay's scrim and layer, a
  > Pane-fill panel at a 50rem measure under a pane header naming the
  > Chronicle, with Close. Its content scrolls in one column: a quiet Ink
  > Dim line ("Chronicle Details · Era target N"), the Foundations, the
  > Era Scorepad with each lineage's legacy chips, the Atlas's Recent site
  > drawn as one mat row at the map's 100% (scrolling sideways in a
  > narrower panel), and the Chronicle Phase aids in the Foundation
  > Strip's face. It is read-only. The card overlay stacks one step above
  > it and closes first on Escape. It opens from a wide Control-face button
  > after the Shared Bank on the table's edge, which takes the edge's whole
  > width on a phone.
- "Foundation Strip": "On the table they close the shared bank on the
  table's edge, after the banners, in three columns, one row a card"
  becomes "In Chronicle Details they stand first, in three columns, one row
  a card". Delete the sentences about the compact map, the glance text
  size and the Glance Layer Rule, which applied only on the table's edge.
  Add at the end: "The Chronicle Phase aids share the face, with no
  numeral."
- `docs/ROADMAP.md`: delete the items "Show the Atlas's first site." and
  "Move the foundation cards to an overlay of their own." outright.
- Spec status line: `**Status:** designed 2026-11-18. All four slices have
  landed.`

- [ ] **Step 3: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`
Expected: passed.

```bash
git add DESIGN.md docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md
git commit -m "docs(chronicle): describe the Chronicle Details overlay

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: The full gate and the browser check

- [ ] **Step 1: Run the full gate**

Run, the long ones in the background:
`./sbtw compile`, `./sbtw test`, `./sbtw frontend/test`,
`./sbtw frontend/fullOptJS`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass.

- [ ] **Step 2: Start a scratch server**

Make `$DB` a new, empty folder in the scratchpad. Never point the server
at `var/distantlands` or another live database. In the background, from
the worktree:

```bash
./sbtw frontend/fastLinkJS "runMain distantlands.server.DistantLandsServer --port 8081 --database-path $DB/distantlands"
```

Wait until `curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/ready`
prints `200`. Never use `preview_start`.

- [ ] **Step 3: Reach the states the check needs (Q14)**

Open `http://localhost:8081/?mode=server` with the built-in browser
tools. Choose New Chronicle, and read (only read) the loaded Chronicle's
JSON from `localStorage`. Edit a copy:
- Give it a new name, "Chronicle Details check".
- Give its era scorepad six games, so the "…" column shows.
- Add a lineage that nobody will seat, holding one active and one dormant
  legacy taken from the legacy ids the JSON does not already place.
- Store a denizen, an edifice and a relic at the Atlas's front Recent
  site, taken from the world deck, the edifices and the relic deck the
  JSON lists.

Check every field name against the JSON first. Import the copy through the
"A Chronicle JSON file" input, and create a game seating fewer lineages
than the Chronicle holds. Report any refusal with its message, and what
was dropped to get past it.

- [ ] **Step 4: Check the overlay and take screenshots**

At 1440×900, then at the phone preset (375×812):
1. The button stands after the Shared Bank, with no foundation strip in
   the bank; on the phone it is full width.
2. Open the overlay. Check:
   - the heading names the Chronicle, and the quiet line reads "Chronicle
     Details · Era target N";
   - the sections stand in order;
   - the scorepad shows "…" and five games, and the unseated lineage's
     dormant legacy is an L chip;
   - the Atlas row's face, denizen, edifice (intact) and relic back sit as
     a map row's do;
   - the five aids show.
3. Click the active legacy chip. The inspector opens above the overlay.
   Escape closes only the inspector; a second Escape closes the overlay,
   and focus is on the button.
4. Scroll the overlay, wait for a poll, and check that it holds its place.

Save one screenshot of the open overlay at each size, and one of the
inspector over it at 1440×900, to the scratchpad. Give their full paths in
the report. The app has one theme (Q15). Fix what the check finds as one
batch of test-first commits, then run at most one confirming round.

- [ ] **Step 5: Stop the server**

```bash
pkill -f "DistantLandsServer --port 8081"
```

---

## Self-Review

- **Spec coverage.**
  - Button: drawn by `tableEdge` only when `chronicleDetails` is present;
    `sharedBank` no longer draws the strip; the hook on the
    `CardInspection.onOpen` pattern, registered by `GameTableShell`
    (Task 1).
  - Overlay shell: an `aside` dialog with a scrim, a panel and a header
    with Close; Escape, Close and the scrim close it; focus goes to the
    content, then back to the opener or the rebuilt button; `show` on every
    snapshot, keeping scroll; the table inert; the inspector one layer up
    and Escape ordered (Tasks 4 and 5).
  - Read-only through `BoardOptions.none` (Tasks 3 and 4).
  - Content 1 to 5, the chips, the last five games with "…", the no-games
    fallback, the Atlas box and the empty Atlas (Tasks 2 and 3).
  - Styles: the panel fills the pane and scrolls; one column; the button
    at phone width (Task 5).
  - Tests named by the design (Tasks 1 to 4).
  - Browser check (Task 7; the Chronicle Phase and themes per Q14 and
    Q15).
  - The slice 3 reviewer's CSS item: Q12, Task 3's `.mat-board` test,
    Task 5's rule, and Task 7's check.
- **Placeholder scan.** Every code step shows its code. Task 7's JSON
  edits name the fields to check, because the Chronicle JSON is read at
  run time.
- **Type consistency.** These keep one name and one type across every
  task:
  - `ChronicleDetails.onOpen`, `open` and `clear`;
  - `ChronicleDetailsOverlay(root, inspectorOpen, opened, closed)`;
  - `GameTableShell.showChronicleDetails(Option[GameProjection])`;
  - `ChronicleDetailsContent.render(GameProjection)`;
  - `ChronicleScoresheet.of(ChronicleDetailsProjection)`;
  - `ScoresheetLegacies.Named` and `Cards`;
  - `LegacyStrip.chip(CardDetails)`;
  - `WorldBoardRenderer.siteRow(site, 0, 0, AtlasRow, BoardOptions.none,
    SitePowerLinks.none, Vector.empty)`.
