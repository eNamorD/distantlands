# Real Proportions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every drawn asset the proportions of the real one and lay the map out on the mat's own coordinates, so that licensed art can later replace drawn art without moving anything.

**Architecture:** One CSS ratio table shapes every card box. Elements that redraw printed content carry the class `printed`; live state is anchored to the box. The map becomes one `.mat-board` of the mat's ratio whose children are absolutely positioned from `MatLayout`, a pure Scala table of rectangles measured from the mat image.

**Tech Stack:** Scala 3, Scala.js, munit under jsdom, plain CSS in `frontend/styles.css`.

**Spec:** [docs/superpowers/specs/2026-11/2026-11-13-real-proportions-design.md](../../specs/2026-11/2026-11-13-real-proportions-design.md)

## Global Constraints

- Work in the worktree `.worktrees/real-proportions`, branch `real-proportions`, based on `0.2.0-alpha`. Do not push.
- Before the first `./sbtw` in the worktree, link the tooling: `ln -s ../../.tooling .tooling` and `ln -s ../../node_modules node_modules` (both are ignored).
- Frontend suites: `./sbtw "frontend/test"`. One suite: `./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`. JVM suites: `./sbtw "testOnly distantlands.server.<Suite>"`.
- Ratios, copied from the spec: denizen `381 / 596`, site `770 / 596`, relic `1 / 1`, banner `1 / 1`, legacy and Vision `596 / 381`, foundation `469 / 803`, Oath goal `673 / 898`, Oathkeeper title `1016 / 700`. Mat `5787 / 2303`.
- No product code may load a licensed image. Licensed files live only under the main checkout's ignored `reference/assets/` (`/Users/roman/projects/distantlands/reference/assets/`), never in the worktree's tracked tree.
- `BoardOptions` keys, option `data-option-ref` values and every `aria-label` stay as they are unless a step says otherwise.
- Player boards, the reliquary, and the pills, chips and rows named in the spec's "Out of scope" stay as they are.
- Code comments, commit messages and docs are normal prose. End each commit message with the `Co-Authored-By` trailer of the model that makes the commit.
- Browser checks use a scratch database on port 8081 (8082 if taken), never `var/distantlands` or any live database. The recipe is in Task 9.

## File Structure

| File | Responsibility |
|---|---|
| `frontend/styles.css` | Ratio table; card boxes; `printed` marking; mat board; site face; table edge; glance layer |
| `frontend/src/main/scala/distantlands/frontend/CardFace.scala` | Box class per card kind; `printed` on printed parts |
| `frontend/src/main/scala/distantlands/frontend/MatLayout.scala` (new) | Mat size and every slot rectangle, in mat pixels; no DOM |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` | Builds the mat board and the table edge from `MatLayout` |
| `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` | `printed` on a site's printed parts; deck figure for the relic deck |
| `frontend/src/main/scala/distantlands/frontend/CardInspection.scala`, `CardInspectionOverlay.scala` | A ratio plate in the text overlay for foundation, Oath goal, Oathkeeper title |
| `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala` | Compact-name width reckoned from the mat's card slot |
| `src/test/scala/distantlands/server/StylesheetProportionsSuite.scala` (new) | Pins the ratio table and the absence of the old literal |
| `frontend/src/test/scala/distantlands/frontend/MatLayoutSuite.scala` (new) | Geometry of the mat table |
| `frontend/src/test/scala/distantlands/frontend/MatBoardSuite.scala` (new) | The rendered board places things at `MatLayout`'s rectangles |
| `docs/catalog/asset-proportions.md` (new) | Tracked record of measured proportions |
| `DESIGN.md` | Card ratios, map layout and glance layer sections brought up to date |

---

## Slice 1: card boxes

### Task 1: The ratio table

**Files:**
- Create: `src/test/scala/distantlands/server/StylesheetProportionsSuite.scala`
- Modify: `frontend/styles.css` (`:root` block at the `--card-w` rule near line 725; `.site .card-face` near 380; `.map-compact .site` near 1404–1413; `.card-list-item > .card-face` near 1855)

**Interfaces:**
- Produces: CSS custom properties `--ratio-denizen`, `--ratio-site`, `--ratio-relic`, `--ratio-banner`, `--ratio-legacy`, `--ratio-foundation`, `--ratio-oath-goal`, `--ratio-oathkeeper-title`; `--card-h: 18.2ex`; `--card-w` derived from it.

- [ ] **Step 1: Link the tooling** (once per worktree; skip a link that is already there, as both are in `.worktrees/real-proportions`)

```bash
ln -s ../../.tooling .tooling
```

```bash
ln -s ../../node_modules node_modules
```

- [ ] **Step 2: Write the failing test**

```scala
package distantlands.server

import java.nio.file.{Files, Path}

/** The card boxes take their shape from one table of real proportions
  * (real proportions design). A literal ratio anywhere else would let one
  * box drift from the asset it stands for.
  */
class StylesheetProportionsSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  private val table = Vector(
    "--ratio-denizen: 381 / 596", "--ratio-site: 770 / 596",
    "--ratio-relic: 1 / 1", "--ratio-banner: 1 / 1",
    "--ratio-legacy: 596 / 381", "--ratio-foundation: 469 / 803",
    "--ratio-oath-goal: 673 / 898", "--ratio-oathkeeper-title: 1016 / 700")

  test("the stylesheet names every card kind's real ratio once"):
    table.foreach(entry =>
      assertEquals(css.split(java.util.regex.Pattern.quote(entry), -1).length - 1,
        1, entry))

  test("no card box carries the old 1 : 1.4 ratio"):
    assert(!css.contains("1 / 1.4"), "aspect-ratio: 1 / 1.4 remains")
    assert(!css.contains("* 1.4)"), "a width times 1.4 remains")
    assert(!css.contains("/ 1.4)"), "a height over 1.4 remains")

  test("a card's width follows from its height and the denizen ratio"):
    assert(css.contains("--card-w: calc(var(--card-h) * 381 / 596)"))
```

- [ ] **Step 3: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.server.StylesheetProportionsSuite"`
Expected: FAIL, three tests, the first on `--ratio-denizen: 381 / 596` found 0 times.

- [ ] **Step 4: Change the stylesheet**

Replace the `:root` rule that defines `--card-w` (and its comment) with:

```css
/* The real proportions of each card kind, as the pixel size of its image
   (docs/catalog/asset-proportions.md). Every card box reads this table, so
   a drawn card and its licensed image fill the same box. */
:root {
  --ratio-denizen: 381 / 596;
  --ratio-site: 770 / 596;
  --ratio-relic: 1 / 1;
  --ratio-banner: 1 / 1;
  --ratio-legacy: 596 / 381;
  --ratio-foundation: 469 / 803;
  --ratio-oath-goal: 673 / 898;
  --ratio-oathkeeper-title: 1016 / 700;
}
/* One box per card type, in ex so a pane's font size scales every card in it
   together. Never overridden per pane: per-context size is font size alone.
   The height is the one a card has always had; the width follows from it,
   so a pane's height budget did not change when the ratio did. */
:root { --card-h: 18.2ex; --card-w: calc(var(--card-h) * 381 / 596);
  --card-gap: 0.55ex; }
```

Change the three fluid-width rules to read the table:

```css
.site .card-face { height: auto; aspect-ratio: var(--ratio-denizen); }
.site .card-face-relic { height: auto; aspect-ratio: var(--ratio-relic); }
```

```css
.card-list-item > .card-face { width: 100%; height: auto;
  aspect-ratio: var(--ratio-denizen); }
.card-list-item > .card-face-relic { aspect-ratio: var(--ratio-relic); }
```

In `.shared-banners .banner-face`, replace `aspect-ratio: 1` with `aspect-ratio: var(--ratio-banner)`.

In the two `.map-compact .site` `--card-w` rules, replace the final `/ 1.4)` of each with `* 381 / 596)`, and in the comment above them replace "a denizen is 1.4 of its width" with "a denizen's width is 381/596 of its height". (Task 6 deletes both rules; until then they must stay correct.)

- [ ] **Step 5: Run the test to see it pass**

Run: `./sbtw "testOnly distantlands.server.StylesheetProportionsSuite"`
Expected: PASS, three tests.

- [ ] **Step 6: Run the frontend suites**

Run: `./sbtw "frontend/test"`
Expected: PASS. No suite reads the ratio.

- [ ] **Step 7: Commit**

```bash
git add src/test/scala/distantlands/server/StylesheetProportionsSuite.scala frontend/styles.css && git commit -m "feat(frontend): shape card boxes from a table of real ratios"
```

### Task 2: The legacy box and the Vision mark

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala` (`boxClass`, the object's doc comment)
- Modify: `frontend/styles.css` (after `.card-face-relic`; `.board-option-card:has(> .card-face)`; `.partition-zone .decision-option`; `.card-list-item`; `.card-overlay-body > .card-face`)
- Test: `frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala`

**Interfaces:**
- Produces: `CardFace.boxClass(cardKind: String): String` returns `"card-face-relic"` for `relic`, `"card-face-legacy"` for `legacy`, `"card-face-denizen card-face-vision"` for `vision`, `"card-face-denizen"` otherwise. The result may hold two class names separated by a space.

- [ ] **Step 1: Change the test**

In `CardFaceSuite`, replace the test "each card type gets its box, whichever way it faces" with:

```scala
  test("each card type gets its box, whichever way it faces"):
    Vector("denizen" -> "card-face-denizen", "vision" -> "card-face-denizen",
      "edifice" -> "card-face-denizen", "relic" -> "card-face-relic",
      "legacy" -> "card-face-legacy").foreach {
      case (kind, box) =>
        val up = CardFace.render(CardDetails("c", kind, "Name",
          orientation = Some("face-up")))
        val down = CardFace.render(CardDetails("hidden", kind,
          s"Facedown $kind", orientation = Some("face-down"), hidden = true))
        assert(up.classList.contains(box), kind)
        assert(down.classList.contains(box), kind)
    }

  /** A Vision is a denizen card on its side: it keeps the denizen box in a
    * row, and carries a mark so the overlay can show it upright. */
  test("a Vision keeps the denizen box and is marked as a Vision"):
    val vision = CardFace.render(CardDetails("v", "vision", "Vision of Faith",
      orientation = Some("face-up")))
    assert(vision.classList.contains("card-face-vision"))
    assert(!vision.classList.contains("card-face-legacy"))
    val denizen = CardFace.render(CardDetails("d", "denizen", "Name",
      orientation = Some("face-up")))
    assert(!denizen.classList.contains("card-face-vision"))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardFaceSuite"`
Expected: FAIL on `legacy` in the first test and on `card-face-vision` in the second.

- [ ] **Step 3: Implement**

In `CardFace.scala`, replace the object's doc comment sentence "Every card occupies its type's box -- denizen 1:1.4, relic 1:1 -- in both states" with "Every card occupies its type's box, at the real card's ratio (the ratio table in `styles.css`), in both states", and replace `boxClass` and its comment with:

```scala
  /** Edifices share the denizen box because they occupy denizen slots. A
    * legacy is a landscape card. A Vision is a denizen card on its side:
    * in a row it keeps the denizen box, so the row stays one grid, and its
    * mark lets the inspection overlay show it upright.
    */
  def boxClass(cardKind: String): String = cardKind match
    case "relic" => "card-face-relic"
    case "legacy" => "card-face-legacy"
    case "vision" => "card-face-denizen card-face-vision"
    case _ => "card-face-denizen"
```

`render`, `facedownCard` and `emptySlot` already interpolate `boxClass` into a class string, so a two-class result needs no other change.

In `styles.css`, after the `.card-face-relic` rule add:

```css
/* A legacy is the denizen card turned on its side: the same two lengths,
   exchanged. */
.card-face-legacy { width: var(--card-h); height: var(--card-w);
  border-color: var(--line); }
```

Replace `.board-option-card:has(> .card-face) { width: var(--card-w); }` with:

```css
.board-option-card:has(> .card-face) { width: var(--card-w); }
.board-option-card:has(> .card-face-legacy) { width: var(--card-h); }
```

After the `.partition-zone .decision-option` rule add:

```css
.partition-zone .decision-option:has(.card-face-legacy) { width: var(--card-h); }
```

After `.card-list-item > .card-face-relic` add:

```css
/* A legacy takes two columns of the list, so it reads at a card's size. */
.card-list-item:has(> .card-face-legacy) { grid-column: span 2; }
.card-list-item > .card-face-legacy { aspect-ratio: var(--ratio-legacy); }
```

After `.card-overlay-body > .card-face { font-size: 1.9em; }` add:

```css
/* The overlay reads a Vision upright, in the landscape box it is printed
   in. */
.card-overlay-body > .card-face-vision { width: var(--card-h);
  height: var(--card-w); }
```

- [ ] **Step 4: Run the suites**

Run: `./sbtw "frontend/test"`
Expected: PASS. If `LegacyStripSuite` or another suite asserts `card-face-denizen` on a legacy, change that expectation to `card-face-legacy`; nothing else about those tests changes.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/CardFace.scala frontend/src/test/scala/distantlands/frontend frontend/styles.css && git commit -m "feat(frontend): draw a legacy landscape and mark a Vision"
```

### Task 3: Printed content and anchored state on a card face

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala` (`render`, `summary`)
- Modify: `frontend/src/main/scala/distantlands/frontend/BannerFace.scala` (the banner's side and name elements)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` (`facedownCard`'s back letter)
- Modify: `frontend/styles.css` (`.card-tokens`; `.map-compact .card-tokens`)
- Test: `frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala`

**Interfaces:**
- Produces: the class `printed` on every element of a card face that redraws what the card prints: `.card-header`, `.legacy-goal`, `.card-power-marks`, `.card-back-letter`. Never on `.card-tokens`, `.card-unimplemented-badge`, `.legacy-dormant`, `.card-knowable-pip`.

- [ ] **Step 1: Write the failing test**

Add to `CardFaceSuite`:

```scala
  /** What a card prints is marked, so licensed art can replace exactly
    * that; what lies on the card in play is not (real proportions design). */
  test("printed parts carry the printed mark and live state does not"):
    val card = CardDetails("c", "denizen", "Sly Fox", suit = Some("beast"),
      orientation = Some("face-up"), favor = 2, implemented = false)
    val node = CardFace.render(card)
    assertEquals(all(node, ".printed").map(_.getAttribute("class")
      .split(" ").head), Vector("card-header"))
    assertEquals(all(node, ".card-tokens.printed, " +
      ".card-unimplemented-badge.printed"), Vector.empty)
    val back = CardFace.render(CardDetails("hidden", "denizen",
      "Facedown denizen", orientation = Some("face-down"), hidden = true))
    assertEquals(all(back, ".card-back-letter.printed").size, 1)
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardFaceSuite"`
Expected: FAIL, `Vector()` did not equal `Vector("card-header")`.

- [ ] **Step 3: Implement**

In `CardFace.summary`, change three element class strings: `"card-header"` to `"card-header printed"`, `"card-power-marks"` to `"card-power-marks printed"`, and the goal's `s"legacy-goal legacy-goal-${goal.key}"` to `s"legacy-goal legacy-goal-${goal.key} printed"`. In `render` (two places) and in `ServerUiSupport.facedownCard`, change the `"card-back-letter"` class strings to `"card-back-letter printed"`. The back letter on a legacy chip (`LegacyStrip.chip`) stays as it is: the chip is one of the stand-ins the spec leaves out of scope. In `BannerFace.render`, change `text("span", "banner-side", side(banner.face))` to use the class `"banner-side printed"` and `element("span", "card-header")` to use `"card-header printed"`.

Suites that compare a whole `class` attribute for these elements now see the extra word; where one fails, change its expected string to include ` printed`.

In `styles.css`, replace the `.card-tokens` rule with:

```css
/* What lies on a card in play is anchored to the card's box, not to what
   the card prints: a band across the card above its foot, the same place
   over a drawn face and over an image. */
.card-tokens { position: absolute; right: 0.3em; bottom: 2.1em; left: 0.3em;
  display: flex; align-items: center; justify-content: center; gap: 0.15em;
  font-size: 1em; }
```

and add after the `.map-compact .card-tokens` rule:

```css
.map-compact .card-tokens { bottom: 0.25em; }
```

- [ ] **Step 4: Run the suites**

Run: `./sbtw "frontend/test"`
Expected: PASS after the expected-string updates described in Step 3.

- [ ] **Step 5: Commit**

```bash
git add frontend/src frontend/styles.css && git commit -m "feat(frontend): mark a card's printed parts and anchor its tokens to the box"
```

### Task 4: A ratio plate in the text overlay

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspection.scala` (`openText` and the request it builds)
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala` (`showText`)
- Modify: `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala:66`, `WorldBoardRenderer.scala:105` (the Oathkeeper title pill)
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala:202` (the handler's `Request.Text` case)
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` (`sharedBank`: the Oath goal opener, ruling R2)
- Test: `frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala` (the Oath goal opener)
- Modify: `frontend/styles.css` (after the `.card-overlay-body > .card-face-vision` rule)
- Test: `frontend/src/test/scala/distantlands/frontend/CardInspectionOverlaySuite.scala`, `FoundationStripSuite.scala`, `PlayerBoardSuite.scala`
- Modify tests (a fourth `_` in each `Request.Text` pattern): `PlayerBoardSuite.scala:260`, `BannerFaceSuite.scala:30`, `FoundationStripSuite.scala:28`, `FestivalMarkerSuite.scala:26`

**Interfaces:**
- Produces: `CardInspection.openText(title: String, lines: Vector[String], origin: dom.html.Element, plate: Option[String] = None)` and `CardInspectionOverlay.showText(title, lines, origin, plate: Option[String] = None)`. `plate` is one of `"foundation"`, `"oath-goal"`, `"oathkeeper-title"`. With a plate, the overlay body's first child is `span.card-plate.card-plate-<plate>` holding the title in a `span.card-name.printed`.

- [ ] **Step 1: Write the failing test**

Add to `CardInspectionOverlaySuite` (it already has `focusableOpener()` for the overlay to return focus to):

```scala
  /** A printed card that is shown as words on the table has its box in the
    * overlay, at its real ratio, where its image will go. */
  test("a text overlay with a plate leads with the card's box"):
    val mount = mountedOverlay()
    mount.overlay.showText("The Sun", Vector("Deal the sites."),
      focusableOpener(), plate = Some("foundation"))
    val body = Dom.one(mount.root, ".card-overlay-body")
    val plate = Dom.one(body, ".card-plate")
    assert(plate.classList.contains("card-plate-foundation"))
    assertEquals(Dom.one(plate, ".card-name.printed").textContent, "The Sun")
    assertEquals(body.firstElementChild, plate)

  test("a text overlay without a plate draws no box"):
    val mount = mountedOverlay()
    mount.overlay.showText("Supply", Vector("A line."), focusableOpener())
    assertEquals(Dom.all(mount.root, ".card-plate"), Vector.empty)
```

Add to `FoundationStripSuite` (its `afterEach` clears the handler):

```scala
  test("a table card opens with the foundation's plate"):
    var plates = Vector.empty[Option[String]]
    CardInspection.onOpen:
      case CardInspection.Request.Text(_, _, _, plate) => plates :+= plate
      case _ => ()
    click(all(tableStrip, "button.foundation-card")(2))
    assertEquals(plates, Vector(Some("foundation")))
```

Add to `PlayerBoardSuite`, after "the title pill opens the Oath text":

```scala
  test("the title pill opens with the Oathkeeper title's plate"):
    var plates = Vector.empty[Option[String]]
    CardInspection.onOpen:
      case CardInspection.Request.Text(_, _, _, plate) => plates :+= plate
      case _ => ()
    val node = renderWith(board, Some(OathkeeperStatus("devotion",
      Some("red"), "oathkeeper", usurperLimited = false, None)))
    as[dom.html.Button](firstMatch(node, ".player-slots .player-title")
      .getOrElse(fail("no title pill"))).click()
    CardInspection.clear()
    assertEquals(plates, Vector(Some("oathkeeper-title")))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardInspectionOverlaySuite"`
Expected: compile error, `showText` has no parameter `plate` (and `Request.Text` has three fields, not four).

- [ ] **Step 3: Implement**

In `CardInspectionOverlay.showText`, add the parameter `plate: Option[String] = None` and, immediately after `node.setAttribute("aria-label", title)`, add:

```scala
    plate.foreach { kind =>
      val box = element("span", s"card-plate card-plate-$kind")
      box.appendChild(text("span", "card-name printed", title))
      body.appendChild(box)
    }
```

In `CardInspection`, the text request and `openText` become:

```scala
    /** `plate` names the printed card behind the words, when there is one:
      * `foundation`, `oath-goal` or `oathkeeper-title`. */
    final case class Text(title: String, lines: Vector[String],
        origin: dom.html.Element, plate: Option[String] = None)
        extends Request
```

```scala
  def openText(title: String, lines: Vector[String],
      origin: dom.html.Element, plate: Option[String] = None): Unit =
    handler.foreach(_(Request.Text(title, lines, origin, plate)))
```

In `GameTableShell`, the handler's text case becomes:

```scala
    case CardInspection.Request.Text(title, lines, origin, plate) =>
      inspector.showText(title, lines, origin, plate)
```

Any other pattern match on `Request.Text` (`grep -rn "Request.Text(" frontend/src`) gains a fourth `_`.

The Oath goal gets an opener (implementation ruling R2). In `WorldBoardRenderer.sharedBank`, the goal's span becomes a button with the classes `oathkeeper-goal card-link` and the same visible text; it opens the title and lines the Oathkeeper title pill uses, with the plate `oath-goal`. An unknown goal falls back to its raw key and no lines, as the pill does. The holder line stays a span.

```scala
      val presented = distantlands.protocol.projection
        .OathkeeperPresentation.byGoal.get(oath.goal)
      val title = presented.fold(oath.goal)(_.title)
      val goal = button(title, "oathkeeper-goal card-link")
      goal.`type` = "button"
      goal.onclick = _ => CardInspection.openText(title,
        presented.fold(Vector.empty[String])(_.lines), goal, Some("oath-goal"))
      status.appendChild(goal)
```

Its test, in `PanelPlacementSuite`, goes first and fails before the change:

```scala
  test("the oath's goal opens its card with the Oath goal's plate"):
    var opened = Vector.empty[(String, Vector[String], Option[String])]
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate) =>
        opened :+= (title, lines, plate)
      case _ => ()
    val goal = Dom.one(world(projection()), ".shared-bank .oathkeeper-goal")
    goal.asInstanceOf[dom.html.Button].click()
    CardInspection.clear()
    assertEquals(opened, Vector(("Oathkeeper of Supremacy",
      Vector("Rules the most sites",
        "Successor to the Chancellor: Holds more relics"),
      Some("oath-goal"))))
```

The existing test "the oath and its holder are listed in the shared bank" reads `status`'s children by `:scope > *` in place of `span`, since the goal is now a button. In `styles.css`, `.oathkeeper-status > span { display: block; }` becomes `.oathkeeper-status > * { display: block; }`; the button keeps `.card-link`'s treatment and focus ring, in the status line's colour and weight (`.oathkeeper-status > .oathkeeper-goal { color: inherit; font-weight: inherit; text-align: left; }`).

Pass plates at the two call sites: in `FoundationStrip.scala` change the call to `CardInspection.openText(title(card), Vector(rules), button, Some("foundation"))`; in `WorldBoardRenderer.playerBoards`, the title pill's call becomes `CardInspection.openText(title, presented.fold(Vector.empty[String])(_.lines), pill, Some("oathkeeper-title"))`.

In `styles.css` add:

```css
/* A card the table shows as words has its box here, at its real ratio,
   where its image will go. */
.card-plate { display: flex; align-items: center; justify-content: center;
  box-sizing: border-box; flex: 0 0 auto; height: calc(var(--card-h) * 1.9);
  padding: 0.6em; border: 1px solid var(--line); border-radius: 0.35em;
  background: var(--base); color: var(--cream); text-align: center;
  font-family: Georgia, serif; }
.card-plate-foundation { aspect-ratio: var(--ratio-foundation); }
.card-plate-oath-goal { aspect-ratio: var(--ratio-oath-goal); }
.card-plate-oathkeeper-title { aspect-ratio: var(--ratio-oathkeeper-title);
  height: calc(var(--card-w) * 1.9); }
```

- [ ] **Step 4: Run the suites**

Run: `./sbtw "frontend/test"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src frontend/styles.css && git commit -m "feat(frontend): show a ratio plate for cards the table draws as words"
```

---

## Slice 2: the mat

### Task 5: `MatLayout`

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/MatLayout.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/MatLayoutSuite.scala`

**Interfaces:**
- Produces: `MatRect(x, y, w, h)` with `right`, `bottom`, `ratio`, `overlaps(that)`, `within(that)`, `percentOf(parent): Vector[(String, String)]` (the four CSS properties `left`, `top`, `width`, `height` as percentages of `parent`).
- Produces: `MatLayout.Width`, `Height`, `Mat`, `DesignWidthPx = 2400.0`, `CardH`, `CardW`, `SiteW`, `SlotsPerSite = 3`, `Rows = Vector(2, 3, 3)`, `column(regionId: String, at: Int): Int`, `siteFrame(column, row)`, `siteCard(column, row)`, `siteRow(column, row)`, `cardSlot(column, row, slot)`, `relicSlot(column, row, slot)`, `discard(column)`, `regionName(column)`, `relicDeck`, `worldDeck`, `roundWheel`, `trackNote`, `visionCells: Vector[MatRect]`, `visionsLabel`, `FavorBankSuits`, `favorBank(suit: String): Option[MatRect]`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.frontend

/** The mat's coordinate table (real proportions design): every slot lies
  * where the mat prints it and has the proportions of the card it holds,
  * so the mat image can be laid under the board without moving anything.
  */
class MatLayoutSuite extends munit.FunSuite:
  import MatLayout.*

  private val sites = for column <- 0 until 3; row <- 0 until Rows(column)
    yield column -> row
  private val slots = for (column, row) <- sites; slot <- 0 until SlotsPerSite
    yield cardSlot(column, row, slot)
  private val banks = FavorBankSuits.flatMap(favorBank)

  /** Every rectangle the board draws, each once. */
  private val everything: Vector[MatRect] =
    sites.map(siteFrame).toVector ++ slots ++
      (0 until 3).map(discard) ++ (0 until 3).map(regionName) ++
      Vector(relicDeck, worldDeck, roundWheel, trackNote, visionsLabel) ++
      visionCells ++ banks

  private def near(actual: Double, expected: Double): Boolean =
    math.abs(actual / expected - 1) < 0.01

  test("the mat has the image's proportions"):
    assertEquals(Width, 5787.0)
    assertEquals(Height, 2303.0)

  test("the mat prints two site rows in the Cradle and three elsewhere"):
    assertEquals(Rows, Vector(2, 3, 3))
    assertEquals(sites.size, 8)

  test("every rectangle lies inside the mat"):
    everything.foreach(rect => assert(rect.within(Mat), rect.toString))

  test("no two rectangles overlap"):
    for a <- everything.indices; b <- everything.indices if a < b do
      assert(!everything(a).overlaps(everything(b)),
        s"${everything(a)} overlaps ${everything(b)}")

  test("each site has three card slots to its right, of the denizen ratio"):
    sites.foreach { (column, row) =>
      val frame = siteFrame(column, row)
      val row3 = (0 until SlotsPerSite).map(cardSlot(column, row, _))
      assertEquals(row3.size, 3)
      row3.foreach { slot =>
        assert(slot.x > frame.right, slot.toString)
        assertEquals(slot.y, frame.y)
        assert(near(slot.ratio, 381.0 / 596), slot.toString)
      }
    }

  test("a site card has the site ratio and lies inside its frame"):
    sites.foreach { (column, row) =>
      val card = siteCard(column, row)
      assert(near(card.ratio, 770.0 / 596), card.toString)
      assert(card.within(siteFrame(column, row)), card.toString)
    }

  test("a site's row spans its frame and its three slots"):
    sites.foreach { (column, row) =>
      val span = siteRow(column, row)
      assertEquals(span.x, siteFrame(column, row).x)
      assertEquals(span.right, cardSlot(column, row, 2).right)
    }

  test("a relic slot is the square top of its card slot"):
    val slot = cardSlot(1, 0, 2)
    val relic = relicSlot(1, 0, 2)
    assert(near(relic.ratio, 1.0), relic.toString)
    assertEquals((relic.x, relic.y, relic.w), (slot.x, slot.y, slot.w))

  test("a discard and the world deck are denizen cards on their side"):
    ((0 until 3).map(discard) :+ worldDeck).foreach(rect =>
      assert(near(rect.ratio, 596.0 / 381), rect.toString))

  test("the relic deck is square and the round wheel is round"):
    assert(near(relicDeck.ratio, 1.0))
    assert(near(roundWheel.ratio, 1.0))

  test("the Visions Drawn track has six cells, left to right"):
    assertEquals(visionCells.size, 6)
    assertEquals(visionCells.map(_.x), visionCells.map(_.x).sorted)

  test("the favor banks stand in the mat's order"):
    assertEquals(FavorBankSuits,
      Vector("discord", "arcane", "order", "hearth", "beast", "nomad"))
    assertEquals(banks.size, 6)
    assertEquals(banks.map(_.x), banks.map(_.x).sorted)
    assertEquals(favorBank("no-such-suit"), None)

  test("a region takes its column from its id, and an unknown one from " +
      "its place"):
    assertEquals(column("cradle", 2), 0)
    assertEquals(column("provinces", 0), 1)
    assertEquals(column("hinterland", 0), 2)
    assertEquals(column("other", 1), 1)
    assertEquals(column("other", 7), 2)

  /** Written as the CSSOM reads a percentage back, with no trailing zeros,
    * so a placed element's inline style compares equal to its rectangle. */
  test("a rectangle reads as percentages of its parent"):
    assertEquals(MatRect(50, 25, 100, 50).percentOf(MatRect(0, 0, 200, 100)),
      Vector("left" -> "25%", "top" -> "25%",
        "width" -> "50%", "height" -> "50%"))
    assertEquals(MatRect(1, 0, 3, 1).percentOf(MatRect(0, 0, 8, 1)),
      Vector("left" -> "12.5%", "top" -> "0%",
        "width" -> "37.5%", "height" -> "100%"))
    assertEquals(MatRect(1, 0, 1, 1).percentOf(MatRect(0, 0, 3, 1)).head,
      "left" -> "33.3333%")
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.MatLayoutSuite"`
Expected: compile error, `MatLayout` not found.

- [ ] **Step 3: Implement**

```scala
package distantlands.frontend

/** A rectangle on the mat, in the mat image's pixels from its upper left. */
private[frontend] final case class MatRect(x: Double, y: Double, w: Double,
    h: Double):
  def right: Double = x + w
  def bottom: Double = y + h
  def ratio: Double = w / h

  def overlaps(that: MatRect): Boolean =
    x < that.right && that.x < right && y < that.bottom && that.y < bottom

  def within(that: MatRect): Boolean =
    x >= that.x && y >= that.y && right <= that.right && bottom <= that.bottom

  /** The CSS box of this rectangle inside `parent`, each length a
    * percentage of the parent's, so the board keeps its layout at any size.
    * Four decimals at most and no trailing zeros, the form the CSSOM reads
    * a percentage back in.
    */
  def percentOf(parent: MatRect): Vector[(String, String)] =
    def percent(value: Double, of: Double): String =
      f"${value / of * 100}%.4f".reverse.dropWhile(_ == '0')
        .dropWhile(_ == '.').reverse + "%"
    Vector("left" -> percent(x - parent.x, parent.w),
      "top" -> percent(y - parent.y, parent.h),
      "width" -> percent(w, parent.w), "height" -> percent(h, parent.h))

/** Where everything lies on the map mat (real proportions design).
  *
  * The numbers are measured from the mat image, 5787×2303 px, and recorded
  * in `docs/catalog/asset-proportions.md`. The mat prints a frame for each
  * site and none for its cards: a site's three card slots are derived, the
  * height of the frame and the width the denizen ratio gives it.
  */
private[frontend] object MatLayout:
  val Width = 5787.0
  val Height = 2303.0
  val Mat: MatRect = MatRect(0, 0, Width, Height)

  /** The board's width in the map's own pixels, before the map is scaled:
    * it puts a card slot at about 135px, the size a map card had before the
    * board took the mat's proportions. */
  val DesignWidthPx = 2400.0

  /** A card stands as tall as a site frame. */
  val CardH = 509.0
  val CardW: Double = CardH * 381 / 596
  val SiteW: Double = CardH * 770 / 596
  val SlotsPerSite = 3

  /** The site rows the mat prints in each column. */
  val Rows: Vector[Int] = Vector(2, 3, 3)

  private val FrameW = 669.0
  private val FrameLeft = Vector(71.0, 1994.0, 3945.0)
  private val RowTop = 428.0
  private val RowPitch = 534.5
  private val SlotGap = 48.0

  /** The mat's column for a region: the Cradle, the Provinces, the
    * Hinterland, left to right. A region with another id takes the column
    * of its place in the world, held to the last. */
  def column(regionId: String, at: Int): Int = regionId match
    case "cradle" => 0
    case "provinces" => 1
    case "hinterland" => 2
    case _ => math.max(0, math.min(at, FrameLeft.size - 1))

  def siteFrame(column: Int, row: Int): MatRect =
    MatRect(FrameLeft(column), RowTop + row * RowPitch, FrameW, CardH)

  /** The site card, centred in the frame the mat prints for it. */
  def siteCard(column: Int, row: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.x + (frame.w - SiteW) / 2, frame.y, SiteW, CardH)

  def cardSlot(column: Int, row: Int, slot: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.right + SlotGap + slot * (CardW + SlotGap), frame.y,
      CardW, CardH)

  /** A relic lies in a card slot, at its top. */
  def relicSlot(column: Int, row: Int, slot: Int): MatRect =
    cardSlot(column, row, slot).copy(h = CardW)

  /** A site's frame and its three slots together: one site's whole row. */
  def siteRow(column: Int, row: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.x, frame.y,
      cardSlot(column, row, SlotsPerSite - 1).right - frame.x, CardH)

  /** A region's discard pile: a card on its side, at the region's upper
    * right. */
  def discard(column: Int): MatRect =
    MatRect(FrameLeft(column) + 932, 80, CardH, CardW)

  def regionName(column: Int): MatRect =
    MatRect(FrameLeft(column), 60, 560, 240)

  val relicDeck: MatRect = MatRect(881, 1913, CardW, CardW)
  /** The world deck lies on its side, as the discards do. */
  val worldDeck: MatRect = MatRect(1278, 1913, CardH, CardW)
  val roundWheel: MatRect = MatRect(62, 1602, 642, 642)
  /** The tracker's line of text, in the clear band over the Cradle's foot. */
  val trackNote: MatRect = MatRect(71, 1500, 1790, 80)
  val visionCells: Vector[MatRect] =
    Vector(805.0, 1000.0, 1120.0, 1295.0, 1415.0, 1535.0)
      .map(x => MatRect(x, 1690, 103, 159))
  val visionsLabel: MatRect = MatRect(1680, 1650, 210, 150)

  val FavorBankSuits: Vector[String] =
    Vector("discord", "arcane", "order", "hearth", "beast", "nomad")
  private val BankCentre =
    Vector(2438.0, 2839.0, 3241.0, 3642.0, 4044.0, 4449.0)

  /** A suit's emblem and its bank circle, as one box. */
  def favorBank(suit: String): Option[MatRect] =
    Option(FavorBankSuits.indexOf(suit)).filter(_ >= 0)
      .map(at => MatRect(BankCentre(at) - 217, 2050, 310, 240))
```

- [ ] **Step 4: Run the test to see it pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.MatLayoutSuite"`
Expected: PASS, 14 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/MatLayout.scala frontend/src/test/scala/distantlands/frontend/MatLayoutSuite.scala && git commit -m "feat(frontend): record where everything lies on the mat"
```

### Task 6: Build the map as the mat board

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` (`world`, `roundTracker`, `favorBanks`, `sharedBank`; new `place`, `placeCards`, `cradleFoot`, `visionsDrawn`, `tableEdge`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` (`siteHeading`, `siteDetails`: the `printed` mark)
- Create: `frontend/src/test/scala/distantlands/frontend/MatBoardSuite.scala`
- Modify tests: `DeckFigureSuite.scala`, `EmptySiteSlotSuite.scala`, `PanelPlacementSuite.scala`

**Interfaces:**
- Consumes: everything `MatLayout` produces (Task 5).
- Produces: DOM. `section.world` holds `h2`, `div.mat-board`, `div.table-edge`. `.mat-board` holds `div.regions` then `div.favor-banks`. Every element placed from `MatLayout` carries the class `mat-slot` and inline `left`, `top`, `width`, `height` in percent. `article.site` is placed at `siteRow`, carries `--face-left` and `--face-w`, and holds `div.site-face` first. `.deck-row` in the Cradle holds, in order: `section.round-tracker`, `div.visions-drawn`, the world deck `div.deck-figure`, the relic deck `div.deck-figure`. `.table-edge` holds the Public Ambitions row (when present) and `section.shared-bank`, which no longer holds the relic deck.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.{all, one}
import TestSites.*
import org.scalajs.dom

/** The map is the mat (real proportions design): one board, with every
  * piece at the rectangle `MatLayout` gives it.
  */
class MatBoardSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val tracks = distantlands.protocol.projection.GameTracksProjection(
    round = 1, visionsDrawn = 2, usurperLimited = false, limiterRound = 4,
    firstPlayerId = "red")
  private val denizen = CardDetails("d1", "denizen", "Old Oak",
    orientation = Some("face-up"))
  private val relic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-up"))
  private val woods = quietSite("woods", "Deep Woods")
    .withDenizenCapacity(2).withRelicCapacity(1).withRelics(1, relic)
  private val table = TestProjections.actingProjection(players = Vector(red),
    world = Vector(
      GameRegion("cradle", Vector(quietSite("keep", "The Keep"))),
      GameRegion("provinces", Vector(quietSite("ford", "Old Ford"), woods),
        emptySlots = 1),
      GameRegion("hinterland", Vector.empty, emptySlots = 3))).copy(
    worldDeckCount = 53, relicDeckCount = 9, tracks = Some(tracks),
    favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 3)))

  private val world = WorldBoardRenderer.world(table, BoardOptions.none)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  private def at(rect: MatRect, parent: MatRect = MatLayout.Mat)
      : Vector[(String, String)] = rect.percentOf(parent)

  test("the world is the mat board and then the table's edge"):
    assertEquals(all(world, ":scope > *").map(_.getAttribute("class")),
      Vector(null, "mat-board", "table-edge"))
    assertEquals(all(one(world, ".mat-board"), ":scope > *")
      .map(_.getAttribute("class")), Vector("regions", "favor-banks"))

  test("a site is placed at its row, with its face first"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    assert(site.classList.contains("mat-slot"))
    assertEquals(box(site), at(row))
    val face = site.firstElementChild
    assert(face.classList.contains("site-face"))
    assertEquals(box(face), at(MatLayout.siteCard(1, 1), row))
    val style = site.asInstanceOf[dom.html.Element].style
    val faceBox = at(MatLayout.siteCard(1, 1), row).toMap
    assertEquals(style.getPropertyValue("--face-left"), faceBox("left"))
    assertEquals(style.getPropertyValue("--face-w"), faceBox("width"))

  test("a site's cards stand in the three slots to its right, a relic " +
      "in the square top of its slot"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    val cards = all(site, ".site-cards > *")
    assertEquals(cards.size, 3)
    assertEquals(box(cards(0)), at(MatLayout.cardSlot(1, 1, 0), row))
    assertEquals(box(cards(1)), at(MatLayout.cardSlot(1, 1, 1), row))
    assertEquals(box(cards(2)), at(MatLayout.relicSlot(1, 1, 2), row))

  test("an empty site slot is the mat's frame"):
    val frames = all(world, ".site-slot-empty")
    assertEquals(frames.size, 4)
    assertEquals(box(frames(0)), at(MatLayout.siteFrame(1, 2)))
    assertEquals(box(frames(1)), at(MatLayout.siteFrame(2, 0)))

  test("a region's name and discard are placed in its column"):
    val provinces = one(world, """section.region[aria-label="Provinces"]""")
    assertEquals(box(one(provinces, ".region-label")),
      at(MatLayout.regionName(1)))
    assertEquals(box(one(provinces, ".region-header > .deck-figure")),
      at(MatLayout.discard(1)))

  test("the Cradle's foot holds the wheel, the track and both decks"):
    val foot = one(world, """section.region[aria-label="Cradle"] .deck-row""")
    assertEquals(all(foot, ":scope > *").map(_.classList.item(0)),
      Vector("round-tracker", "visions-drawn", "deck-figure", "deck-figure"))
    assertEquals(box(one(foot, ".round-tracker svg")),
      at(MatLayout.roundWheel))
    assertEquals(box(one(foot, ".track-note")), at(MatLayout.trackNote))
    val decks = all(foot, ":scope > .deck-figure")
    assertEquals(box(decks(0)), at(MatLayout.worldDeck))
    assertEquals(box(decks(1)), at(MatLayout.relicDeck))
    assertEquals(decks.map(deck => one(deck, ".deck-caption").textContent),
      Vector("World deck", "Relic deck"))
    assertEquals(one(decks(1), ".pile-count").textContent, "×9")

  test("Visions Drawn is a marker on the mat's six cells"):
    val track = one(world, ".visions-drawn")
    assertEquals(track.getAttribute("aria-label"), "Visions Drawn 2")
    val cells = all(track, ".vision-cell")
    assertEquals(cells.map(_.textContent), Vector("0", "1", "2", "3", "4", "5"))
    assertEquals(cells.map(box), MatLayout.visionCells.map(at(_)))
    assertEquals(box(one(track, ".vision-marker")),
      at(MatLayout.visionCells(2)))

  test("a count past the track's end stands on its last cell"):
    val many = WorldBoardRenderer.world(table.copy(tracks =
      Some(tracks.copy(visionsDrawn = 7))), BoardOptions.none)
    assertEquals(box(one(many, ".vision-marker")),
      at(MatLayout.visionCells(5)))

  test("each favor bank stands on its circle"):
    val banks = all(world, ".favor-banks > .favor-bank")
    assertEquals(banks.map(box),
      MatLayout.FavorBankSuits.flatMap(MatLayout.favorBank).map(at(_)))

  test("the shared bank is on the table's edge and holds no relic deck"):
    val bank = one(world, ".table-edge > .shared-bank")
    assertEquals(all(bank, ".pile-display"), Vector.empty)

  test("what the mat and a site print is marked printed"):
    assertEquals(all(world, ".region-label.printed").size, 3)
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    assertEquals(all(site, ".site-name.printed").size, 1)
    assertEquals(all(site, ".site-defense.printed").size, 1)
    assertEquals(all(site, ".site-footer.printed").size, 1)
    assertEquals(all(site, ".site-tokens.printed, .site-pawns.printed"),
      Vector.empty)
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.MatBoardSuite"`
Expected: FAIL, the first test on `Vector(null, "world-layout")`.

- [ ] **Step 3: Mark a site's printed parts**

In `ServerUiSupport.siteHeading`, change the class strings `"site-name"` to `"site-name printed"` and `"site-defense"` to `"site-defense printed"`. In `siteDetails`, change `"site-footer"` to `"site-footer printed"`.

- [ ] **Step 4: Add the placement helpers to `WorldBoardRenderer`**

```scala
  /** Places `node` at `rect`, each length a percentage of `parent`, which
    * is the rectangle of the element it is positioned in. */
  private def place[E <: dom.Element](node: E, rect: MatRect,
      parent: MatRect = MatLayout.Mat): E =
    node.classList.add("mat-slot")
    val style = node.asInstanceOf[dom.html.Element].style
    rect.percentOf(parent).foreach((property, value) =>
      style.setProperty(property, value))
    node

  /** Stands each of a site's cards in its slot, in the row's order: a
    * relic in the square top of its slot, anything else in the whole of
    * it. */
  private def placeCards(details: dom.Element, column: Int, row: Int,
      rowRect: MatRect): Unit =
    Option(details.querySelector(".site-cards")).foreach { cards =>
      (0 until cards.children.length).foreach { slot =>
        val card = cards.children(slot)
        val relic = card.classList.contains("card-face-relic") ||
          card.querySelector(".card-face-relic") != null
        place(card,
          if relic then MatLayout.relicSlot(column, row, slot)
          else MatLayout.cardSlot(column, row, slot), rowRect)
      }
    }

  /** The Cradle's foot, as the mat prints it: the round wheel, the Visions
    * Drawn track, the world deck and the relic deck. */
  private def cradleFoot(value: GameProjection, deck: dom.Element)
      : dom.Element =
    val row = element("div", "deck-row")
    value.tracks.foreach { track =>
      row.appendChild(roundTracker(value, track))
      row.appendChild(visionsDrawn(track.visionsDrawn))
    }
    row.appendChild(place(deckFigure("World deck", deck),
      MatLayout.worldDeck))
    row.appendChild(place(deckFigure("Relic deck",
      deckBody(value.relicDeckCount, Some("relic"))), MatLayout.relicDeck))
    row

  /** The mat's six cells, each printing its number, and the marker on the
    * cell of the count. A count past the last cell stands on the last. */
  private def visionsDrawn(count: Int): dom.Element =
    val track = element("div", "visions-drawn")
    track.setAttribute("role", "img")
    track.setAttribute("aria-label", s"Visions Drawn $count")
    MatLayout.visionCells.zipWithIndex.foreach((cell, at) =>
      track.appendChild(place(text("span", "vision-cell printed",
        at.toString), cell)))
    track.appendChild(place(text("span", "visions-drawn-label printed",
      "Visions Drawn"), MatLayout.visionsLabel))
    val current = math.max(0, math.min(count, MatLayout.visionCells.size - 1))
    track.appendChild(place(element("span", "vision-marker"),
      MatLayout.visionCells(current)))
    track

  /** What the physical table keeps beside the mat. */
  private def tableEdge(value: GameProjection, options: BoardOptions)
      : dom.Element =
    val edge = element("div", "table-edge")
    LegacyStrip.publicRow(value, options).foreach(edge.appendChild)
    edge.appendChild(sharedBank(value, options))
    edge
```

- [ ] **Step 5: Rewrite `world`'s body**

Keep the panel, heading, `deckKeys`, `deck`, `options.decorate(deck, deckKeys)` and `badged(deck, deckKeys, options)` lines as they are. Replace everything from `val regions = element("div", "regions")` to the end of the method with:

```scala
    val regions = element("div", "regions")
    value.world.zipWithIndex.foreach { (region, at) =>
      val column = MatLayout.column(region.regionId, at)
      val section = element("section", "region")
      val name = region.regionId match
        case "cradle" => "Cradle"
        case "provinces" => "Provinces"
        case "hinterland" => "Hinterland"
        case other => other
      section.setAttribute("aria-label", name)
      val header = element("div", "region-header")
      header.appendChild(place(text("h3", "region-label printed", name),
        MatLayout.regionName(column)))
      val discardTitle = s"$name discard pile"
      val discardKeys = Vector(BoardOptionViews.key("discard-pile",
        region.regionId))
      val discard = openableDeck(region.discardCount,
        region.discardTopCardKind, discardTitle, region.discardCards)
      badged(discard, discardKeys, options)
      header.appendChild(place(deckFigure("Discard", options.control(discard,
        discardKeys, Option.when(region.discardCount > 0)(discardTitle ->
          (anchor => CardInspection.openCards(
            s"$discardTitle (${region.discardCount})", region.discardCards,
            anchor))))), MatLayout.discard(column)))
      section.appendChild(header)
      val sites = element("div", "sites")
      region.sites.zipWithIndex.foreach { (site, row) =>
        val rowRect = MatLayout.siteRow(column, row)
        val faceRect = MatLayout.siteCard(column, row)
        val control = place(element("article", "site site-readonly"), rowRect)
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
        val pawns = element("div", "site-pawns")
        value.pawnLocations.filter(_.siteId == site.siteId).foreach { pawn =>
          val marker = element("span", "pawn")
          marker.appendChild(dom.document.createTextNode("● "))
          marker.appendChild(playerReference(value, pawn.playerId))
          options.decorate(marker,
            Vector(BoardOptionViews.key("player", pawn.playerId)))
          pawns.appendChild(marker)
        }
        if site.festival then pawns.appendChild(festivalMarker)
        control.appendChild(pawns)
        val details = siteDetails(site, options,
          SitePowerLinks.of(value, submit))
        placeCards(details, column, row, rowRect)
        control.appendChild(details)
        sites.appendChild(control)
      }
      // A slot no site fills is the frame the mat prints for it.
      (0 until region.emptySlots).foreach(extra =>
        sites.appendChild(place(emptySlot(name),
          MatLayout.siteFrame(column, region.sites.size + extra))))
      section.appendChild(sites)
      if region.regionId == "cradle" then
        section.appendChild(cradleFoot(value, deck))
      regions.appendChild(section)
    }
    val board = element("div", "mat-board")
    board.appendChild(regions)
    board.appendChild(favorBanks(value, options))
    panel.appendChild(board)
    panel.appendChild(tableEdge(value, options))
    panel
```

Update `world`'s doc comment: after its sentence on `options`, add "The map is one board with the mat's proportions; every piece is placed from `MatLayout`."

- [ ] **Step 6: Place the wheel, the note and the banks; drop the relic deck from the bank**

In `roundTracker`: replace `section.appendChild(svg)` with `section.appendChild(place(svg, MatLayout.roundWheel))`; then build `val note = place(element("div", "track-note"), MatLayout.trackNote)`, append `summary` and (when limited) the `usurper-notice` paragraph to `note` instead of `section`, and end with `section.appendChild(note)`.

In `favorBanks`: after `options.decorate(box, ...)` add `MatLayout.favorBank(bank.suit).foreach(rect => place(box, rect))`.

In `sharedBank`: delete the line `section.appendChild(pileDisplay("Relic deck", value.relicDeckCount, None))`. That was `pileDisplay`'s only caller: delete `ServerUiSupport.pileDisplay` and the four `.pile-display` rules in `styles.css`.

- [ ] **Step 7: Bring three suites to the new structure**

`EmptySiteSlotSuite`: the expected class vectors become `"site site-readonly mat-slot"` and `"site-slot-empty mat-slot"` (three assertions).

`DeckFigureSuite`: in "a region's discard sits in its header", compare first class names: `assertEquals(all(header, ":scope > *").map(_.classList.item(0)), Vector("region-label", "deck-figure"))`. Replace the test "the world deck sits beside the round tracker, in the Cradle" with:

```scala
  test("the world deck and the relic deck stand in the Cradle's foot"):
    val world = WorldBoardRenderer.world(table, BoardOptions.none)
    val row = one(region(world, "Cradle"), ".deck-row")
    assertEquals(all(row, ":scope > *").map(_.classList.item(0)),
      Vector("round-tracker", "visions-drawn", "deck-figure", "deck-figure"))
    assertEquals(all(row, ".deck-caption").map(_.textContent),
      Vector("World deck", "Relic deck"))
    assertEquals(all(world, ".world > .deck-figure, .world > .pile-display"),
      Vector.empty)
```

and in "the count is drawn on the card" read the world deck's count from the first figure: `all(region(world, "Cradle"), ".deck-row .deck-card .pile-count").head.textContent`.

`PanelPlacementSuite`: replace the test "the shared bank stands beside the regions and their favor banks" with:

```scala
  test("the shared bank stands on the table's edge, under the mat"):
    val panel = world(projection())
    assertEquals(Dom.all(panel, ":scope > div").map(_.getAttribute("class")),
      Vector("mat-board", "table-edge"))
    assertEquals(Dom.all(Dom.one(panel, ".mat-board"), ":scope > *")
      .map(_.getAttribute("class")), Vector("regions", "favor-banks"))
    assertEquals(Dom.all(panel, ".table-edge > .shared-bank").size, 1)
```

`sharedBank` itself does not change: it moves under `.table-edge` whole, with the Oath goal's button (ruling R2). The Oath goal tests in `PanelPlacementSuite` select inside `.shared-bank` and pass unchanged.

- [ ] **Step 8: Run the suites**

Run: `./sbtw "frontend/test"`
Expected: PASS. A suite that fails only because an element's `class` attribute now ends in ` mat-slot` or ` printed` gets its expected string updated; a failure of any other kind is a defect in Steps 3 to 6.

- [ ] **Step 9: Commit**

```bash
git add frontend/src && git commit -m "feat(frontend): build the map as one board on the mat's coordinates"
```

### Task 7: Style the mat board

**Files:**
- Modify: `frontend/styles.css`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala` (`CardWidthPx`, its comment; the scale callback; new `tinyAtScale`)
- Test: `frontend/src/test/scala/distantlands/frontend/MapViewStateSuite.scala`

**Interfaces:**
- Consumes: `MatLayout.DesignWidthPx`, `MatLayout.CardW`, `MatLayout.Width`.
- Produces: `GameTableShell.tinyAtScale(scale: Double): Boolean`, true below 0.4; the class `map-tiny` on `.map-content` while it holds.

- [ ] **Step 1: Change the scale tests**

In `MapViewStateSuite`, replace the three `nameBoxEm` expectations with the mat card's width (2400 × 325.386 / 5787 = 134.95px), and the comment above them with one that says so ("A card slot on the mat is 134.95px wide in map space, so on screen it is 48.6px at 0.36 and 61.1px at 0.4524; less the border, that is 4.23em and 5.37em of 11px text."):

```scala
    assertEqualsDouble(GameTableShell.nameBoxEm(0.36).get, 4.23, 0.01)
    assertEqualsDouble(GameTableShell.nameBoxEm(0.4524).get, 5.37, 0.01)
    assertEqualsDouble(GameTableShell.nameBoxEm(0.2).get, 2.27, 0.01)
```

and add:

```scala
  test("the map is tiny below 0.4, where a site's face has no room for " +
      "its powers"):
    assert(GameTableShell.tinyAtScale(0.2))
    assert(GameTableShell.tinyAtScale(0.39))
    assert(!GameTableShell.tinyAtScale(0.4))
    assert(!GameTableShell.tinyAtScale(0.72))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.MapViewStateSuite"`
Expected: compile error, `tinyAtScale` not found.

- [ ] **Step 3: Implement in `GameTableShell`**

Replace `CardWidthPx` and its comment with:

```scala
  /** A card slot's width on the mat in the map's own pixels, the same for
    * every card on the board: the mat fixes it. The compact name spans the
    * face to its 1px border.
    */
  private val CardWidthPx =
    MatLayout.DesignWidthPx * MatLayout.CardW / MatLayout.Width
```

Add beside `compactAtScale`:

```scala
  /** Below this a site's face holds its name, tokens, pawns and forces at
    * the glance size and nothing more: its powers are read on a zoom. */
  private val TinyBelow = 0.4
  def tinyAtScale(scale: Double): Boolean = scale < TinyBelow
```

In the `MapViewport` scale callback, after `mapContent.style.setProperty("--map-scale", scale.toString)`, add:

```scala
    mapContent.classList.toggle("map-tiny", GameTableShell.tinyAtScale(scale))
```

- [ ] **Step 4: Run the test to see it pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.MapViewStateSuite"`
Expected: PASS.

- [ ] **Step 5: Remove the flowing map's rules from `styles.css`**

Delete these rules and the comments that belong to them: `.regions`; `.region`; `.sites` (with `--site-h`); the `.site` rule's `height`, `padding`, `overflow`, `border`, `background`, `display`, `flex-direction` and `--card-w` declarations (keep `position: relative`, `min-width: 0`, `color`, `text-align`); `.site-slot-empty`'s `height`; `.site > .target-detail-badge`; `.region-header`; `.region .region-header > .region-label`; `.region:has(> .deck-row)`; `.deck-row` and its four descendants' rules (`> .round-tracker`, `.deck-figure`, `.deck-card`, `.deck-card > .pile-count`); `.world-layout`; `.shared-bank`'s border and padding; `.favor-banks`' grid; `.map-content .regions`; `.map-content .sites`; the three `.map-content .region` colour rules; `.map-content .region-label` (it comes later than the new `.mat-board .region-label` with the same specificity, so it would override it); `.map-compact .site` (the grid rule and both `--card-w` rules); `.map-compact .site-heading`; the `grid-row`/`grid-column` declarations of `.map-compact .site-name`, `.site-tokens`, `.site-pawns`, `.site-defense`, `.site-details`. Keep every `font-size: var(--glance-...)` declaration.

- [ ] **Step 6: Add the mat board's rules**

Replace the `.map-content` rule and the comment above it that explains the 1500px regions and the bank's column (keep the "Pinned, never inherited" comment) with:

```css
/* The board is 2400px wide before the map is scaled (MatLayout
   .DesignWidthPx): a card slot is then about 135px, the size a map card
   has always had. */
.map-content { position: absolute; width: 2416px; padding: 8px;
  transform-origin: top left; font-size: 1rem; }
```

and add, where the deleted `.regions` rule stood:

```css
/* The map is the mat: one board with the mat's proportions. Everything on
   it is placed from MatLayout as a percentage of the board, so the mat's
   image can lie under it without moving anything. Until then the board
   paints the mat's three lights, blended as the mat blends them. */
.mat-board { position: relative; width: 100%; aspect-ratio: 5787 / 2303;
  overflow: hidden; border-radius: 9px;
  background: linear-gradient(90deg, #213a24 0 27%, #1d3441 39% 61%,
    #46261a 73% 100%); }
.mat-slot { position: absolute; box-sizing: border-box; margin: 0; }
/* The groups a piece belongs to are there for reading order and names, not
   for layout: each is a box of no size, and its pieces are positioned in
   the board. */
.mat-board :is(.regions, .region, .region-header, .sites, .deck-row,
  .favor-banks, .round-tracker, .visions-drawn, .site-details, .site-cards) {
  position: static; display: block; margin: 0; padding: 0; border: 0;
  background: none; min-height: 0; }
.mat-board .region-label { height: auto; margin: 0; overflow: hidden;
  white-space: nowrap; color: var(--cream-bright); font-family: Georgia, serif;
  font-weight: 700; letter-spacing: 0.01em; text-transform: none;
  line-height: 1.1;
  font-size: clamp(2.6rem, calc(14px / var(--map-scale, 1)), 4.2rem); }

/* A site is its card and the three slots to its right: one row, one option
   target. The row itself draws nothing but a decision's ring. */
.mat-board .site { border: 1px solid transparent; border-radius: 9px; }
/* The site card, at the real card's proportions, in the frame the mat
   prints for it. */
.site-face { border: 1px solid var(--line); border-radius: 9px;
  background: var(--site); }
.site-slot-empty { border: 1px dashed var(--line-mid); border-radius: 9px;
  background: transparent; }
/* A site's words are laid over its face. What the card prints: the name in
   a band at its foot, the powers and the requirement over the band, the
   defense in the upper right. What lies on it in play: loose tokens in the
   upper left, where the card prints its starting resources, then the pawns
   and the forces. */
.mat-board .site-heading { display: contents; }
.mat-board .site-name { position: absolute; bottom: 0; left: var(--face-left);
  width: var(--face-w); box-sizing: border-box; padding: 4px 8px;
  border-radius: 0 0 9px 9px; background: #1a1610d9; line-height: 1.15; }
.mat-board .site-tokens { position: absolute; top: 8px;
  left: calc(var(--face-left) + 8px); }
.mat-board .site-defense { position: absolute; top: 8px;
  right: calc(100% - var(--face-left) - var(--face-w) + 8px); }
.mat-board .site-pawns { position: absolute; top: 2.4em; margin: 0;
  left: calc(var(--face-left) + 8px); width: calc(var(--face-w) - 16px); }
.mat-board .site-forces { position: absolute; top: 4.4em; margin: 0;
  left: calc(var(--face-left) + 8px); }
.mat-board .site-footer { position: absolute; bottom: 2.5em; margin: 0;
  left: calc(var(--face-left) + 8px); width: calc(var(--face-w) - 16px); }
.mat-board .site > .target-detail-badge { position: absolute; top: 2.4em;
  right: calc(100% - var(--face-left) - var(--face-w) + 8px); margin: 0; }
/* A card fills the slot the mat gives it. */
.mat-board .site-cards > .mat-slot > .card-face { width: 100%; height: 100%;
  aspect-ratio: auto; }

/* A deck fills its slot; its caption lies on its upper edge, as the mat
   prints the slot's name inside it. */
.mat-board .deck-figure { display: block; }
.mat-board .deck-figure > .board-option-card { display: block; width: 100%;
  height: 100%; }
.mat-board .deck-body { display: block; box-sizing: border-box; width: 100%;
  height: 100%; margin: 0; padding: 0; }
.mat-board .deck-card { width: 100%; height: 100%; padding-bottom: 0;
  font-size: 2.6rem; }
.mat-board .deck-caption { position: absolute; top: 6px; right: 0; left: 0;
  text-align: center; pointer-events: none; }
.mat-board .deck-body > .target-detail-badge { position: absolute;
  bottom: 6px; left: 50%; transform: translateX(-50%); }
.mat-board .deck-figure .board-option-inspect { top: auto; right: 4px;
  bottom: 4px; }

/* The Cradle's foot. */
.mat-board .round-tracker svg { margin: 0; }
.track-note { overflow: hidden; font-size: 1.05rem; line-height: 1.2; }
.track-note p { display: inline; margin: 0 1.2em 0 0; }
.vision-cell { display: flex; align-items: flex-start;
  justify-content: center; padding-top: 3px; border: 2px solid var(--line-mid);
  border-radius: 4px; font-size: 1.1rem; font-weight: 700; }
.visions-drawn-label { display: flex; align-items: center;
  color: var(--parchment-dim); font-family: Georgia, serif; line-height: 1.1; }
.vision-marker { display: flex; align-items: flex-end;
  justify-content: center; padding-bottom: 8%; pointer-events: none; }
.vision-marker::after { content: ""; width: 46%; aspect-ratio: 1;
  border-radius: 50%; background: var(--brass-bright); }

/* A favor bank on the mat's pair of circles: the suit, the count, favor. */
.mat-board .favor-bank { padding: 0 6px; border-radius: 999px;
  background: #1a1610b3; font-size: 1.05rem; line-height: 1.1; }

/* Beside the mat, as the physical table has them: the Public Ambitions,
   then the oath, the banners and the foundations in one row. */
.table-edge { display: flex; align-items: flex-start; gap: 16px;
  margin-top: 12px; }
.table-edge .shared-bank { display: flex; flex: 1; align-items: flex-start;
  gap: 16px; padding: 0; border: 0; }
.table-edge .shared-bank > h3 { margin: 0; font-size: 1rem; }
.table-edge .shared-banners { grid-template-columns: repeat(2, 8.6rem);
  margin-top: 0; }
.table-edge .foundations { flex: 1; margin-top: 0; }
.table-edge .foundation-strip {
  grid-template-columns: repeat(3, minmax(0, 1fr)); }
.table-edge .public-legacies { margin: 0; }

/* The glance layer on the mat. A site's face is a fixed box, so each line
   it holds is set against the scale and placed in em of its own size. */
.map-compact .mat-board .site-name { font-size: var(--glance-name);
  line-height: 1.05; }
.map-compact .mat-board :is(.site-tokens, .site-defense, .site-pawns,
  .site-forces, .site-footer, .site > .target-detail-badge, .track-note,
  .vision-cell, .visions-drawn-label, .deck-caption, .favor-bank) {
  font-size: var(--glance-text); }
.map-compact .mat-board .site-footer { bottom: 2.9em; }
.map-compact .mat-board .site-powers { -webkit-line-clamp: 1; }
/* Smaller still, the face keeps what a player scans for and gives up its
   powers, which are read on a zoom or in the overlay. */
.map-tiny .mat-board .site-footer { display: none; }
```

- [ ] **Step 7: Run every suite**

Run: `./sbtw "frontend/test"` and `./sbtw "testOnly distantlands.server.*"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend && git commit -m "feat(frontend): style the mat board, its site faces and the table's edge"
```

### Task 8: Gather the reference assets

**Files:**
- Create (ignored, main checkout only): `/Users/roman/projects/distantlands/reference/assets/cards/`, `/Users/roman/projects/distantlands/reference/assets/symbols/`, `/Users/roman/projects/distantlands/reference/assets/backs/`

Nothing in this task is tracked, and nothing is committed. The mat, the dice and the tokens are already under `reference/assets/tts/`.

- [ ] **Step 1: Copy the New Foundations card images and the symbols**

```bash
mkdir -p /Users/roman/projects/distantlands/reference/assets/cards /Users/roman/projects/distantlands/reference/assets/symbols /Users/roman/projects/distantlands/reference/assets/backs
```

```bash
find "/Users/roman/projects/bgs-cards/content/card-images/oath/en-US" -name "*(NF).png" -exec cp {} /Users/roman/projects/distantlands/reference/assets/cards/ \;
```

```bash
cp /Users/roman/projects/bgs-cards/content/card-symbols/oath/*.png /Users/roman/projects/distantlands/reference/assets/symbols/
```

- [ ] **Step 2: Check the counts**

Run: `ls /Users/roman/projects/distantlands/reference/assets/cards | wc -l` and `ls /Users/roman/projects/distantlands/reference/assets/symbols | wc -l`
Expected: `240` and `21`.

- [ ] **Step 3: Copy the card back sheets**

The backs are print-and-play sheets, several cards to a page. They are kept as the PDFs they came in; L3 cuts them when it needs them.

```bash
cp "/Users/roman/projects/distantlands/reference/2026-03-17-new-foundations-rev1/"{denizen-backs,relic-backs,legacy-backs,site-backs-1up,oathkeeper-goal-backs}.pdf /Users/roman/projects/distantlands/reference/assets/backs/
```

- [ ] **Step 4: Confirm nothing became tracked**

Git is not run in the main checkout. From the worktree, which tracks the same `.gitignore`:

Run: `git check-ignore -v --no-index reference/assets/cards/x.png reference/assets/backs/x.pdf reference/assets/symbols/x.png`
Expected: each path printed against the `.gitignore` rule `reference/`.

---

## Slice 3: finish

### Task 9: Check and tune in the browser

**Files:**
- Modify: `frontend/styles.css` (values only, in the rules Tasks 1, 2, 3, 7 and the R4 band added)
- Modify, only if a measured number is wrong: `frontend/src/main/scala/distantlands/frontend/MatLayout.scala` (numbers only) and the same number in Task 10's record

The stylesheet values in Task 7 are computed, not yet seen. This task looks at the result at three sizes, with the real mat and real cards laid under the drawn ones, and corrects values. It changes no structure; the only Scala it may touch is a measured number in `MatLayout`.

It follows Impeccable's bounded verification, with `reference/adapt.md` and `reference/polish.md` read first: one batched inspection round over all three viewports (Steps 2 and 3, every row of the table), one batch of fixes, and at most one confirming round. A finding the detector hook reports is judged by line in this worktree; a `CONTEXT_STALE` finding is reported, not repaired.

Before starting the server, rebuild without coverage (`./sbtw clean`): a coverage run leaves instrumented classes behind.

- [ ] **Step 1: Start a scratch server**

The scratch copy of the database made in slice 1 is in `DB=/private/tmp/claude-501/-Users-roman-projects-oathdigital/c17e13c6-2b51-40f4-a67d-0fd7cda78149/scratchpad/real-proportions-db`; it already holds a fresh two-player game, `game-yi5xmjyf3sh6`. If the folder is gone, copy `distantlands.script`, `distantlands.properties` and `distantlands.lobs` from the main checkout's `var/` into it first. Then, from the worktree, in the background:

```bash
./sbtw frontend/fastLinkJS "runMain distantlands.server.DistantLandsServer --port 8081 --database-path $DB/distantlands"
```

Wait until `curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/` prints `200`.

- [ ] **Step 1b: Reach the states slices 1 and 2 could not show**

A fresh game shows no legacy in the Public Ambitions row, no Vision, and no relic or banner on a player board. They come from the Chronicle the game starts from, so build one Chronicle that sets them up and start a new game from it:

No Chronicle already in the browser's storage is edited or deleted: the edited JSON is imported as a new Chronicle with a name of its own, and the others stay as they are.

1. On the root page, read (only read) the loaded Chronicle's JSON. In the script tool, parse `localStorage["distantlands.chronicles"]`, take the entry named by `localStorage["distantlands.chronicles.loaded"]`, and parse its `json` field. Check the field names below against what it holds before editing.
2. Edit a copy of that JSON:
   - Set its `name` to one no stored Chronicle has (for example "Real proportions check"), so the import adds a Chronicle instead of replacing one.
   - In `foundations`, set number 3's `face` to `"altered"`. Its altered face is Public Ambitions, so setup deals four legacies faceup to the public row (`LegacyDeal`).
   - Give one lineage a starting relic: add a relic id that the relic deck holds (for example the first id in `relicDeck`, removed from there) to its `startingRelics`. That relic starts on its holder's board.
   - Set `oathkeeperGoal` to Devotion (`"devotion"`, in the form the JSON already uses) and give one entry of `lineages` the role `"chancellor"`. Setup gives the Darkest Secret to the Chancellor when the goal is the Darkest Secret, so the banner starts on a player board.
   - For a Vision, add one to a lineage's `startingAdvisers` as `{"kind": "vision", "id": …, "orientation": …}`, using a Vision id from `worldDeck`. If setup refuses that, use a Vision a hand or a discard list shows (switch seats from the toolbar), and failing that, build the overlay case in the page from `CardFace.render`'s markup and report that it was built.
3. Import the edited JSON through the root page's "A Chronicle JSON file" input. In the script tool, put a `File` made from the edited text into the input through a `DataTransfer`, then dispatch `change`. Then create a game with one row per lineage.

A step the server refuses is reported with its message. That case falls back to a page-built harness, as the token band check does, and the report says which cases were built.

- [ ] **Step 2: Lay the real mat and real cards under the drawn ones**

The dev server does not serve `reference/`. Serve it from a second local port for the check only:

```bash
python3 -m http.server 8090 --directory /Users/roman/projects/distantlands/reference/assets
```

In the browser's script tool, lay the mat under the board and a real card image under one site, one denizen and one relic, each at half opacity so both layers show:

```js
const under = (el, url) => { el.style.backgroundImage = `url("${url}")`;
  el.style.backgroundSize = "100% 100%"; };
const board = document.querySelector(".mat-board");
under(board, "http://localhost:8090/tts/mat/mat.jpg");
const face = document.querySelector(".site-face");
under(face, "http://localhost:8090/cards/" + encodeURIComponent("Ancient City (NF).png"));
document.querySelectorAll(".site .card-face-denizen").forEach(card =>
  under(card, "http://localhost:8090/cards/" + encodeURIComponent("Alchemist (NF).png")));
document.querySelectorAll(".site .card-face-relic").forEach(card =>
  under(card, "http://localhost:8090/cards/" + encodeURIComponent("Bandit Crown (NF).png")));
document.querySelectorAll(".printed").forEach(el => el.style.opacity = "0.35");
```

This is a check, run in the page; it is not product code and nothing of it is saved.

- [ ] **Step 3: Inspect at 1440×900, 1024×768 and a 390×844 phone, at Fit and zoomed to 100%**

This is the one batched inspection round. Every row below is checked at all three sizes before anything is changed.

Confirm each of these, and where one fails, correct the value named:

| Check | If it fails, change |
|---|---|
| Each site face sits inside the mat's printed frame; each discard, the relic deck, the world deck, the wheel, the six cells and the six banks sit on what the mat prints | the measured number in `MatLayout` (and its test stays green) |
| The mat image, a site image, a denizen image and a relic image fill their boxes with no stretch (circles stay round, text is not squashed) | the ratio in use; it must come from the table |
| Three card slots fit to the right of every site; the Hinterland's third slot is inside the mat | `SlotGap` in `MatLayout` |
| At Fit, a site's name, tokens, pawns and forces read at 11px or more on screen and stay inside the face | `--glance-name`, `--glance-text` clamps; the `top`/`bottom` em offsets of the site's lines |
| At 100%, the site's name band, two lines of powers, the requirement and the defense stay inside the face and do not cover the pawns | the same offsets, outside `.map-compact` |
| A deck's caption, letter and count are all readable in its slot; the inspect button covers none of them | `.mat-board .deck-card` font size; the caption's `top` |
| A favor bank shows its suit, its count and the favor glyph inside its box | `.mat-board .favor-bank` font size |
| The table's edge sits under the mat, and Fit shows the mat and the edge together | `.table-edge` gap and the banner column width |
| On the player strip and in the action pane, a card's name and emblems fit the narrower face; the strip is no taller than before | `.card-name` font size on that pane; never `--card-h` |
| A legacy card is landscape in the Public Ambitions row and in a card list | the Task 2 rules |
| In the inspection overlay a Vision is landscape and upright; a foundation opens with its plate | the Task 2 and Task 4 rules |
| A board decision that picks a site rings the whole row; one that picks a card rings the card; the inspect button sits in the card's lower right | `.mat-board .site` border radius; nothing else |
| The phone pans and zooms the board as before, and Fit shows the whole mat | nothing expected; report if not |
| The round wheel's rectangle, `MatRect(62, 1602, 642, 642)`, sits on the wheel the mat image prints: its circle inside the rim, the eight drawn segments on the printed ones | `roundWheel` in `MatLayout`, and Task 10's record |
| At Fit, a site face's lines do not run into each other: a two-line site name such as "Shrouded Woods" against the forces line above it, and the region and requirement line ("Mountain · Recover 4") against the forces line and the name band | the `top` and `bottom` em offsets of `.mat-board .site-pawns`, `.site-forces` and `.site-footer`, in and outside `.map-compact`; the name band's line height |
| Each region name shows whole: "Hinterland" is not clipped by its box at the clamp's minimum | the `.mat-board .region-label` clamp; never the `regionName` rectangle unless it disagrees with the mat |
| The track note's words read at 11px or more on screen at Fit | `.track-note` font size, in and outside `.map-compact` |
| A relic with a two-line name keeps its defense dice inside the box, on a player board and in a site slot | the relic face's type scale (`.card-name` or the header's line height on `.card-face-relic`); never `--card-w` (ruling R3) |
| A relic and a banner on a player board are square, read their name and count, and their token band clears the name (ruling R4) | the R4 band rules; `.player-board .banner-face` padding |
| On the 390×844 phone at Fit (about 0.14), nothing drawn on the board is under about 9px on screen: per `reference/adapt.md`, decide which glance text stays legible (raise a glance clamp's maximum, or drop a line under `map-tiny`), and hide at that scale what cannot reach 9px rather than show it illegible. Accessible names and hover tips are untouched | the `--glance-name` and `--glance-text` clamps and `.mat-board .region-label`'s clamp; `.map-tiny` rules |
| At 1024×768 and on the phone, Fit shows the whole mat with the table's edge under it, and the edge's row wraps rather than overflowing | `.table-edge` and `.table-edge .shared-bank` wrapping; the banner column width |
| Measured, not looked at: the bounding box of each face's `.card-tokens` overlaps none of `.card-header` (as the header clips it), `.legacy-goal`, `.card-power-marks`, `.card-unimplemented-badge` or `.board-option-badge`, on a banner in the shared bank and on a player board, a relic, a legacy, a dormant legacy, a denizen with a restriction banner and a long name, an unimplemented denizen with power marks, a Replace option card, and a knowable face-down relic and legacy holding tokens and a power mark with the hover summary open (`.card-hover-summary`'s band against its own marks) (implementation ruling R4). Cases the game does not show are built in the page from the markup `CardFace.render` and `BannerFace.render` make, inside the pane that would hold them | the R4 band rules (`--marks-bottom`, `--band-bottom`, `--band-h`) |

- [ ] **Step 4: Remove the check's overlays and take screenshots**

Reload the page. Save one screenshot at each of the three sizes to the scratchpad and give their full paths in the report. After the one batch of fixes, run at most one confirming round over the rows that failed.

- [ ] **Step 5: Run every suite, stop the servers**

Run: `./sbtw "frontend/test"` and `./sbtw "testOnly distantlands.server.*"`
Expected: PASS.

```bash
pkill -f "http.server 8090"; pkill -f "DistantLandsServer --port 8081"
```

Confirm with `lsof -i :8081 -sTCP:LISTEN` and `lsof -i :8090 -sTCP:LISTEN` that nothing listens.

(Exit status 144 from the second is expected.)

- [ ] **Step 6: Commit**

```bash
git add frontend/styles.css frontend/src && git commit -m "fix(frontend): tune the mat board against the real mat and cards"
```

### Task 10: Record the proportions and update the design system

**Files:**
- Create: `docs/catalog/asset-proportions.md`
- Modify: `DESIGN.md` (the passages the Step 2 search finds, at HEAD near lines 229, 295, 441, 510, 512, 588 and 914, that state `1:1.4`, the three-region grid, the fixed site box, the side column bank and the compact site grid)
- Modify: `docs/catalog/README.md` (one line pointing to the new record, so the catalog's index names it)
- Modify: `docs/ROADMAP.md` (the L3 line)

- [ ] **Step 1: Write the proportions record**

```markdown
# Asset proportions

The real size and proportions of each game asset the table draws, and where
each number came from. The stylesheet's ratio table and `MatLayout` are
built from this record; roadmap item L3 reads it for the box each licensed
image must fill. It holds numbers only. The assets themselves are licensed
and live under the ignored `reference/assets/`.

## Cards

Sizes are the pixel sizes of the New Foundations card images in the
`bgs-cards` clone (`content/card-images/oath/en-US`).

| Asset | Size | Ratio (w : h) | CSS |
|---|---|---|---|
| Denizen, edifice, their backs | 381×596 | 1 : 1.564 | `--ratio-denizen` |
| Relic | 381×381 | 1 : 1 | `--ratio-relic` |
| Site | 770×596 | 1.292 : 1 | `--ratio-site` |
| Legacy, Vision | 596×381 | 1.564 : 1 | `--ratio-legacy` |
| Banner | 1181×1181 | 1 : 1 | `--ratio-banner` |
| Foundation card | 469×803 | 1 : 1.712 | `--ratio-foundation` |
| Oath goal | 673×898 | 1 : 1.334 | `--ratio-oath-goal` |
| Oathkeeper title | 101.6×70 mm (print-and-play sheet) | 1.452 : 1 | `--ratio-oathkeeper-title` |

A Vision is a denizen card printed on its side. In a row it keeps the
denizen box; upright, it is the legacy box.

## Pieces

| Asset | Shape | Source |
|---|---|---|
| Attack, defense and round die face | square | Tabletop Simulator die textures |
| Favor token | round | owner's screenshot of the model |
| Secret token | book, about 0.87 : 1 | owner's screenshot of the model |

Token and die glyph boxes are square; a token image is fitted inside its
box.

## The mat

The mat image is 5787×2303 px (2.513 : 1). All numbers below are in its
pixels, from its upper left.

| Piece | x | y | w | h |
|---|---|---|---|---|
| Site frame, Cradle column | 71 | rows below | 669 | 509 |
| Site frame, Provinces column | 1994 | rows below | 669 | 509 |
| Site frame, Hinterland column | 3945 | rows below | 669 | 509 |
| Site rows (the Cradle has the first two) | | 428, 962.5, 1497 | | |
| Card slot | frame's right + 48, then every 373.4 | frame's | 325.4 | 509 |
| Discard pile (card on its side) | column's frame + 932 | 80 | 509 | 325.4 |
| Region name | column's frame | 60 | 560 | 240 |
| Relic deck | 881 | 1913 | 325.4 | 325.4 |
| World deck (card on its side) | 1278 | 1913 | 509 | 325.4 |
| Round wheel | 62 | 1602 | 642 | 642 |
| Visions Drawn cells 0 to 5 | 805, 1000, 1120, 1295, 1415, 1535 | 1690 | 103 | 159 |
| Favor bank circle centres, Discord to Nomad | 2438, 2839, 3241, 3642, 4044, 4449 | 2155 | radius 93 | |

The mat prints a frame for each site and none for its cards. A site frame
is 669×509, a little wider than a site card of the same height (657.6), so
the card is centred in it. The three card slots are derived: the frame's
height, the denizen ratio's width, 48 px apart.

Not on the mat and not drawn at their proportions: player boards and the
reliquary board, which the table keeps abstract.
```

If Task 9 changed a number in `MatLayout`, write the changed number here.

- [ ] **Step 2: Update `DESIGN.md`**

Run `grep -n "1:1.4\|1 : 1.4\|1\.4 ratio\|--site-h\|shared bank\|Shared Bank\|side column\|bank's column\|three regions\|site box" DESIGN.md` and rewrite each passage it finds so that it states: cards take their ratio from the ratio table (denizen 381:596, site 770:596, relic and banner square, legacy 596:381); a card outside the mat keeps `--card-h` and derives its width; the map is one board of the mat's ratio placed from `MatLayout`; a site is a site card with three card slots to its right; decks, discards, the round wheel, the Visions Drawn track and the favor banks stand at the mat's positions; the banners, the oath and the foundations stand on the table's edge under the mat; printed content carries `printed` and live state is anchored to the box; the token band stands at a square or landscape box's foot and just above a denizen's power marks, above the marks on any box that prints them, and the face keeps its room free (ruling R4); a card the table shows as words (foundation, Oath goal, Oathkeeper title) opens with a plate at its real ratio, and the Oath goal in the shared bank is a link that opens it (ruling R2); the glance layer has a second step, `map-tiny`, below 0.4. Keep the document's own voice and structure; change statements of fact, not headings.

In `docs/catalog/README.md`, add after its opening paragraph: "`asset-proportions.md` records the real size and ratio of each game asset the table draws, and where the mat prints each piece."

- [ ] **Step 3: Update the roadmap**

In `docs/ROADMAP.md`, extend the L3 item with one sentence (its links are relative to `docs/`):

```markdown
The layout is ready for it: every drawn asset has its real proportions and the map is laid out on the mat's coordinates ([real proportions design](superpowers/specs/2026-11/2026-11-13-real-proportions-design.md), [asset proportions](catalog/asset-proportions.md)).
```

- [ ] **Step 4: Check the links**

Run: `grep -n "asset-proportions\|real-proportions" docs/ROADMAP.md DESIGN.md docs/catalog/asset-proportions.md`
Expected: every path printed exists relative to its file (`ls` each one).

- [ ] **Step 5: Commit**

```bash
git add docs/catalog/asset-proportions.md docs/catalog/README.md DESIGN.md docs/ROADMAP.md && git commit -m "docs: record asset proportions and the mat layout"
```
