# Chronicle Details, Slice 3: Site Drawing Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The body that draws one site in `WorldBoardRenderer.world()`
becomes `siteRow`, which slice 4's overlay reuses for the Atlas's Recent
site. The map draws exactly as before.

**Architecture:** `world()` draws each site inline: it places an
`article.site` at the site's row on the mat, then adds the face, the
heading, the cost badge, the pawns, the festival marker and the cards, each
placed relative to the row (`MatLayout.siteRow(column, row)`). The site's
body moves, unchanged, into `siteRow`. Two inputs that come from the whole
projection come in as arguments. The site power links are passed in, so
the overlay can pass `SitePowerLinks.none`. The pawn markers are built by
`world()` and passed in, so `siteRow` needs no `GameProjection` and the
overlay passes none. The one inline piece of `world()` that reads the
projection per pawn moves to a private `pawnMarker`.

**Where the row itself is placed (R9).** `world()` places
the article at the row inside the mat: `place(article, rowRect)`, whose
parent defaults to `MatLayout.Mat`. The overlay needs the same article to
fill a box of the row's own ratio, which is `place(article, rowRect,
rowRect)`: left 0, top 0, width and height 100%. So `siteRow` takes the
rectangle the row is placed `within`, and places the article first, before
any child is added or any option class is set, as today. The row's own
rectangle is derived from `column` and `row`, as every child's is.

**Tech Stack:** Scala.js frontend, munit, `./sbtw` (sbt wrapper),
`-Werror`.

**Spec:** [Chronicle Details design](../../specs/2026-11/2026-11-18-chronicle-details-design.md),
section "Slice 3: site drawing". Rulings made while this plan runs go in
the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md),
from R9.

## Global Constraints

- No behaviour change. The map's DOM is the same element for element:
  same tags, classes in the same order, attributes, inline styles,
  children in the same order, and the same handlers.
- `MatBoardSuite` and the other world map suites pass unchanged:
  `SiteBoxLayoutSuite`, `SiteFaceSuite`, `EmptySiteSlotSuite`,
  `SitePowerGridSuite`, `BoardOptionsRenderSuite`, `PrintedLayerSuite`,
  `PanelPlacementSuite`, and every other suite `./sbtw frontend/test` runs.
- A new test renders one `siteRow` alone and checks that its card and
  relic slots sit where `MatLayout` places them in the row.
- Protected (do not change): `MatLayout`, `ServerUiSupport.siteHeading`
  and `siteDetails`, `SitePowerLinks`, `BoardOptions`, `styles.css`, and
  every file of slices 1 and 2.
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
  Scala and Markdown at 80 columns. Scaladoc cites "Chronicle Details
  design, slice 3".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## Rulings

Recorded in the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md);
Task 1 applies them.

- **R9.** `siteRow(site, column, row, within, options, links, pawns)`.
  `siteRow` computes the row rectangle and places the article in
  `within`: `MatLayout.Mat` on the map, the row's own rectangle in the
  overlay. The map DOM stays identical.
- **R10.** `pawns: Vector[dom.Element]`, built by `pawnMarker`; the
  overlay passes `Vector.empty`. Each marker is now marked before the site
  rather than after; marking touches only the marker itself.

## Files

- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  (`world()`'s site loop; new `siteRow` and `pawnMarker`)
- Create: `frontend/src/test/scala/distantlands/frontend/SiteRowSuite.scala`
- Modify docs: the spec's status line.

## Running things

- Frontend suites: `./sbtw "frontend/testOnly distantlands.frontend.SiteRowSuite distantlands.frontend.MatBoardSuite distantlands.frontend.SiteBoxLayoutSuite distantlands.frontend.SiteFaceSuite distantlands.frontend.EmptySiteSlotSuite distantlands.frontend.SitePowerGridSuite distantlands.frontend.BoardOptionsRenderSuite distantlands.frontend.PrintedLayerSuite distantlands.frontend.PanelPlacementSuite"`.
- Every frontend test: `./sbtw frontend/test`.
- Full gate at slice end, long ones in the background: `./sbtw compile`,
  `./sbtw test`, `./sbtw frontend/test`, `./sbtw frontend/fullOptJS`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory below was read at `44df95976`. Each task begins by running
its grep at HEAD. Treat what the grep shows as the list; handle a hit
missing from the task by the task's rule, and report it.

---

### Task 1: Extract `siteRow` from `world()`

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/SiteRowSuite.scala`

**Interfaces:**
- Produces, in `WorldBoardRenderer` (`private[frontend]`):
  `def siteRow(site: GameSite, column: Int, row: Int, within: MatRect, options: BoardOptions, links: SitePowerLinks, pawns: Vector[dom.Element]): dom.Element`.
  Slice 4's overlay calls
  `siteRow(site, 0, 0, MatLayout.siteRow(0, 0), BoardOptions.none, SitePowerLinks.none, Vector.empty)`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'region\.sites\.zipWithIndex|site-pawns|festivalMarker|SitePowerLinks\.of' -- frontend/src/main
```
Expected: `WorldBoardRenderer.scala` only: the site loop in `world()`, its
`site-pawns` box, `festivalMarker`'s use and definition, and
`SitePowerLinks.of(value, submit)`. A hit elsewhere is a second site
drawing; report it, leave it.

- [ ] **Step 2: Write the failing test**

Create `frontend/src/test/scala/distantlands/frontend/SiteRowSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one}
import TestSites.*
import org.scalajs.dom

/** One site's row drawn alone (Chronicle Details design, slice 3): placed
  * within the rectangle it is given, with its face, its cards and its
  * relic where `MatLayout` places them in the row, and only the pawns it
  * is given. */
class SiteRowSuite extends munit.FunSuite:
  private val relic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-up"))
  private val woodsWithTwoDenizenSlotsAndAKnownRelic =
    quietSite("woods", "Deep Woods").withDenizenCapacity(2)
      .withRelicCapacity(1).withRelics(1, relic)
  private val row = MatLayout.siteRow(1, 1)

  /** Deep Woods in the Provinces' second row, drawn to fill a box of the
    * row's own ratio, with `pawns` and nothing to choose. */
  private def woodsRowFillingItsBox(
      pawns: Vector[dom.Element] = Vector.empty,
      site: GameSite = woodsWithTwoDenizenSlotsAndAKnownRelic): dom.Element =
    WorldBoardRenderer.siteRow(site, 1, 1, row, BoardOptions.none,
      SitePowerLinks.none, pawns)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  test("the row is the site's article, filling the rectangle it is placed " +
      "within, its face first at the site card's place in the row"):
    val site = woodsRowFillingItsBox()
    assertEquals(site.tagName.toLowerCase, "article")
    assertEquals(site.getAttribute("aria-label"), "Deep Woods")
    assertEquals(box(site), row.percentOf(row))
    assertEquals(box(site.firstElementChild),
      MatLayout.siteCard(1, 1).percentOf(row))

  test("its cards and its relic stand where MatLayout places them in the " +
      "row"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-cards > *").map(box),
      Vector(MatLayout.cardSlot(1, 1, 0), MatLayout.cardSlot(1, 1, 1),
        MatLayout.relicSlot(1, 1, 2)).map(_.percentOf(row)))

  test("it draws the pawns it is given, and the festival marker only on " +
      "a festival site"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-pawns > *").size, 0)
    val pawn = ServerUiSupport.element("span", "pawn")
    assertEquals(all(woodsRowFillingItsBox(Vector(pawn)), ".site-pawns > *"),
      Vector(pawn))
    assertEquals(all(woodsRowFillingItsBox(site =
      woodsWithTwoDenizenSlotsAndAKnownRelic.copy(festival = true)),
      ".site-pawns > .festival-marker").size, 1)

  test("on the mat, the row stands where world() places Deep Woods"):
    val onMat = WorldBoardRenderer.siteRow(
      woodsWithTwoDenizenSlotsAndAKnownRelic, 1, 1, MatLayout.Mat,
      BoardOptions.none, SitePowerLinks.none, Vector.empty)
    assertEquals(box(onMat), row.percentOf(MatLayout.Mat))
    assertEquals(one(onMat, ".site-face").getAttribute("aria-hidden"),
      "true")
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SiteRowSuite"`
Expected: compilation fails with "value siteRow is not a member of object
distantlands.frontend.WorldBoardRenderer".

- [ ] **Step 4: Move the site's body into `siteRow`**

In `WorldBoardRenderer.scala`, replace the site loop in `world()` (from
`region.sites.zipWithIndex.foreach { (site, row) =>` to its closing
`}` before the empty slots) with:

```scala
      region.sites.zipWithIndex.foreach { (site, row) =>
        val pawns = value.pawnLocations.filter(_.siteId == site.siteId)
          .map(pawn => pawnMarker(value, pawn.playerId, options))
        sites.appendChild(siteRow(site, column, row, MatLayout.Mat, options,
          SitePowerLinks.of(value, submit), pawns))
      }
```

After `world()`, add:

```scala
  /** One site's row on the mat (Chronicle Details design, slice 3): the
    * `article.site` placed at its row inside `within`, then its face,
    * heading, cost badge, `pawns`, festival marker and cards, each placed
    * relative to the row `MatLayout.siteRow(column, row)`. `world()`
    * passes the mat; the Chronicle Details overlay passes the row itself,
    * so the row fills a box of its ratio. `links` opens the site's powers;
    * `options` marks what the parked decision offers. */
  private[frontend] def siteRow(site: GameSite, column: Int, row: Int,
      within: MatRect, options: BoardOptions, links: SitePowerLinks,
      pawns: Vector[dom.Element]): dom.Element =
    val rowRect = MatLayout.siteRow(column, row)
    val faceRect = MatLayout.siteCard(column, row)
    val control = place(element("article", "site site-readonly"), rowRect,
      within)
    control.setAttribute("aria-label", site.label)
    // The site's words are laid over its face, so the stylesheet needs
    // the face's extent in the row.
    val faceBox = faceRect.percentOf(rowRect).toMap
    val style = control.asInstanceOf[dom.html.Element].style
    style.setProperty("--face-left", faceBox("left"))
    style.setProperty("--face-w", faceBox("width"))
    val siteKeys = Vector(BoardOptionViews.key("site", site.siteId))
    options.decorate(control, siteKeys)
    // The card itself: drawn until licensed art takes its place.
    val face = place(element("div", "site-face"), faceRect, rowRect)
    face.setAttribute("aria-hidden", "true")
    control.appendChild(face)
    control.appendChild(siteHeading(site))
    // What reaching this site costs, or why it cannot be reached, comes
    // from the decision that offers it.
    badged(control, siteKeys, options)
    val pawnBox = element("div", "site-pawns")
    pawns.foreach(pawnBox.appendChild)
    if site.festival then pawnBox.appendChild(festivalMarker)
    control.appendChild(pawnBox)
    val details = siteDetails(site, options, links)
    placeCards(details, column, row, rowRect)
    control.appendChild(details)
    control

  /** A pawn on the map. Both forms are in the DOM and the map's scale picks
    * one (ruling R6): the dot and the name at rest, the dot and the
    * initial zoomed out, the dot alone further out. The dot and the
    * initial are for the eye; the name stays the pawn's name. */
  private def pawnMarker(value: GameProjection, playerId: String,
      options: BoardOptions): dom.Element =
    val marker = element("span", "pawn")
    val player = playerReference(value, playerId)
    val colour = playerColorClass(value, playerId)
    val dot = text("span", s"pawn-dot $colour", "●")
    dot.setAttribute("aria-hidden", "true")
    marker.appendChild(dot)
    // The long form: the space and the name, as the pawn reads at rest.
    val long = element("span", "pawn-long")
    long.appendChild(dom.document.createTextNode(" "))
    long.appendChild(player)
    marker.appendChild(long)
    val initial = text("span", s"pawn-initial $colour",
      player.textContent.take(1))
    initial.setAttribute("aria-hidden", "true")
    marker.appendChild(initial)
    HoverTip.set(marker, player.textContent)
    options.decorate(marker, Vector(BoardOptionViews.key("player", playerId)))
    marker
```

Every statement of `siteRow` and `pawnMarker` is a statement of the old
loop body, in its old order. The only differences are that the pawn
markers are built before the site is marked (R10), and the names `pawns`
(the markers) and `pawnBox` (the `site-pawns` box, which the old code
called `pawns`).

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SiteRowSuite distantlands.frontend.MatBoardSuite distantlands.frontend.SiteBoxLayoutSuite distantlands.frontend.SiteFaceSuite distantlands.frontend.EmptySiteSlotSuite distantlands.frontend.SitePowerGridSuite distantlands.frontend.BoardOptionsRenderSuite distantlands.frontend.PrintedLayerSuite distantlands.frontend.PanelPlacementSuite"`,
then `./sbtw frontend/test` in the background.
Expected: all pass, the world map suites unchanged.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/SiteRowSuite.scala
git commit -m "refactor(frontend): draw one site's row on its own

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: Docs and the full gate

**Files:**
- Modify: `docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md` (status line)

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'have landed|siteRow' -- docs DESIGN.md ':!docs/superpowers/plans'
```
Expected: the spec's status line and its slice 3 and slice 4 text. If
`DESIGN.md` or an architecture doc describes `world()`'s site drawing by
name, report it; the change draws the same DOM, so no design text
changes.

- [ ] **Step 2: Update the status line**

`**Status:** designed 2026-11-18. Slices 1 (projection), 2 (aids and
strip) and 3 (site drawing) have landed.`

- [ ] **Step 3: Run the full gate**

Run, the long ones in the background:
`./sbtw compile`, `./sbtw test`, `./sbtw frontend/test`,
`./sbtw frontend/fullOptJS`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md
git commit -m "docs(chronicle): record Chronicle Details slice 3

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

- **Spec coverage.** The per-site body extracted into `siteRow`, which
  returns the `article.site` with its face, heading, pawns, festival marker
  and cards placed relative to the row: Task 1, Step 4 (the `within`
  argument per R9). `world()` calls it for each site and behaves as
  before: Task 1, Steps 4 and 5. `MatBoardSuite` and the other world map
  suites pass unchanged: Task 1, Step 5. A new test renders one `siteRow`
  alone and checks its card and relic slots against `MatLayout`: Task 1,
  Step 2.
- **Placeholder scan.** Every code step shows its code.
- **Type consistency.** `siteRow(site, column, row, within, options,
  links, pawns)` and `pawnMarker(value, playerId, options)` carry the same
  names and types in the interface block, the test and the code.
