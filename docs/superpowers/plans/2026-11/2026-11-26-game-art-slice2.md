# Game Art, Slice 2: The World Board Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** With Game art on, the World pane shows the licensed mat under its
pieces, each site's card in its frame, each banner's face, the backs of
the decks and discard piles on the mat, and the foundation and Oath goal
cards in the inspection overlay. The layout is the same with art on or
off, and each box falls back to its drawing on its own.

**Architecture:** Slice 1 built the plumbing: `ArtCatalog`, `GameArt` and
`ArtLayer.attach(box, key)`, with printed content hidden by its nearest
box through `--printed-opacity`. This slice adds catalog keys for sites,
banners, foundations, Oath goals and the mat, calls `ArtLayer.attach` from
the renderers that draw them, and adds each new box kind to the
stylesheet's box list. A site's printed words lie beside its face in the
site's row, so the image goes in the face and the row is the box that
turns ready; `ArtLayer.attach` takes the box to mark as a third argument.

**Tech Stack:** Scala 3, Scala.js, munit, jsdom, CSS, `./sbtw` (sbt
wrapper), compiler flags including `-Werror`.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md),
"The surfaces" and "Delivery", item 2. Rulings are in the
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md)
(R1 to R9 so far). The mat's numbers are in
[asset proportions](../../../catalog/asset-proportions.md). Slice 1's plan:
[plumbing and cards](2026-11-26-game-art-slice1.md).

## Rulings folded in

The orchestrator ruled on the plan's open questions (R13 to R19 in the
implementation rulings). The plan's Q numbers map to them: Q1 is R13, Q2
R14, Q3 R15, Q4 R16, Q5 R17, Q6 R18, Q7 R19.

- **R13.** Foundations, the Oath goal and a board's legacies show their
  image in the inspection overlay; the rows, the button and the pills stay
  words (Task 5).
- **R14.** A facedown legacy's pill stays drawn.
- **R15.** A site's power buttons stay in place; the browser check reports
  where each lands on the art (Task 2, Task 7).
- **R16.** Piles on the mat show their top card's back, sideways ones
  turned; with art off a pile looks exactly as today; the count stays
  readable (Task 4).
- **R17.** Chronicle Details' Atlas rows show site art (Task 2).
- **R18.** A by-eye rectangle over printed art moves, and is reported
  (Task 6).
- **R19.** No site back or Oath goal back in the catalog.

## Global Constraints

- Work only in `/Users/roman/projects/distantlands/.worktrees/game-art`, on
  branch `game-art`. Never commit on `0.2-alpha` or `main`.
- Licensed art: never commit, copy into the worktree, or upload any file
  from `/Users/roman/projects/distantlands/reference/assets/`. No test
  reads a licensed file. Tracked docs list file names only.
- `.tooling` and `node_modules` are symlinks. Never `git add -A` or
  `git add .`; add files by path.
- The layout must not depend on whether art is shown. With art off, or
  with no manifest, every renderer builds the DOM it builds today, apart
  from a class this plan adds for art on and off alike.
- A card's file is the one its "New Foundations" entry names in the
  `bgs-cards` clone's card data (R1). Build each name list from that data
  and note any entry that names a file outside the two forms.
- `docs/operations/game-art.md` lists exactly `ArtCatalog.allPaths`, in
  order (R6).
- Printed content is hidden by its nearest box (R3). Every box kind that
  holds printed content of its own is in the stylesheet's box list.
- No control is printed or sits inside printed content (R9 of the real
  proportions design). No control becomes unreachable when art is ready.
- No `transition` and no `animation`. No new colour.
- The build is `-Werror`; `scripts/check-architecture.py` enforces size
  limits (800 lines a file; `WorldBoardRenderer.scala` is at 643) and
  layering.
- Never write a bare `===` or `=word` in a shell command.
- Never touch the live database `var/distantlands*`. The browser check
  uses a scratch database on port 8083.
- Docs, comments, commit messages and plans are plain English prose. Test
  helper names state what they build.
- Commit messages are Conventional Commits and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after every commit.

### Protected

These must behave exactly as before. Do not edit them except where a step
names the edit:

- Slice 1's behaviour: `GameArt`, `ArtLayer.attach(box, key)` with two
  arguments, card faces and backs, the Settings dialog. Its suites pass
  without edits.
- The `printed` marking: which elements carry it. `PrintedLayerSuite`
  passes without edits.
- `MatLayout`: every number, unless Task 6 finds a by-eye rectangle over
  printed art.
- `BoardOptions`: how a site, a deck, a banner and a card take their
  option marks, rings and inspect buttons.
- `CardInspection.openText`'s existing callers: the new argument has a
  default.
- Player boards and the reliquary board stay abstract. The Chronicle pages
  draw no art.

## File Structure

| File | Responsibility |
|---|---|
| `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala` | Keys for sites, banners, foundations, Oath goals and the mat. |
| `docs/operations/game-art.md` | The new paths. |
| `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala` | The optional box to mark ready. |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` | The mat's image, a site's image, the piles' backs. |
| `frontend/src/main/scala/distantlands/frontend/BannerFace.scala` | A banner's image. |
| `frontend/src/main/scala/distantlands/frontend/CardInspection.scala`, `CardInspectionOverlay.scala`, `SetupCardStrip.scala`, `FoundationStrip.scala` | The plate's image for a foundation and the Oath goal. |
| `frontend/styles.css` | The box list, the mat's and the piles' rules. |
| `frontend/src/main/scala/distantlands/frontend/MatLayout.scala`, `docs/catalog/asset-proportions.md` | Only if Task 6 moves a rectangle. |

---

### Task 1: Catalog keys for the world board

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala`
- Modify: `docs/operations/game-art.md`
- Test: `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala`

**Interfaces:**
- Produces:
  - `ArtCatalog.sites: Vector[String]`, the 24 site names; a site's face is
    `ArtCatalog.cardFace(name)`, path `cards/<Name> (NF).png`.
  - `ArtCatalog.banner(face: String): Option[ArtKey]` for the projection's
    faces `"mob"`, `"grand-council"`, `"wandering-flame"`, `"festival"`.
  - `ArtCatalog.foundation(number: Int, name: String): Option[ArtKey]`,
    path `cards/Foundation <numeral> - <Name> (NF).png`.
  - `ArtCatalog.oathGoal(goal: String): Option[ArtKey]` for `"supremacy"`,
    `"protection"`, `"devotion"`, `"the-people"`.
  - `ArtCatalog.mat: ArtKey`, path `tts/mat/mat.jpg`.
  - `allPaths` grows by 24 + 4 + 12 + 4 + 1 = 45 paths, appended after the
    slice 1 paths in that order.

The names, read from the card data's "New Foundations" entries:

- Sites: Ancient City, Broken Peaks, Buried Giant, Deep Woods, Desolate
  Shore, Dunes, Fair Isle, Golden Valley, Great Slum, Green Shore,
  Headwaters, Hidden Place, Mines, Narrow Pass, Painted Towers, Riverbank,
  Rocky Coast, Salt Flats, Shrouded Woods, Solitary Pillar, Standing
  Stones, Steppe, Sunken Isles, Tidal Marshes.
- Banners: `mob` is "Banner of the People's Favor - Mob", `grand-council`
  "Banner of the People's Favor - Grand Council", `wandering-flame`
  "Banner of the Darkest Secret - Wandering Flame", `festival` "Banner of
  the Darkest Secret - Festival".
- Foundations: I Imperial Maps, I Wide Horizons, II Powerful Tribes, II
  Movers and Shakers, III Quiet Ambitions, III Public Ambitions, IV Ties
  of Fellowship, IV Teeming World, V Mob's Favor, V Grand Council, VI
  Wandering Flame, VI Festival.
- Oath goals: `supremacy` "Supremacy Oath", `protection` "Protection
  Oath", `devotion` "Devotion Oath", `the-people` "People Oath". These
  files are not named for the card's title ("Oathkeeper of Supremacy");
  the card data's `image` field names them.

- [ ] **Step 1: Write the failing tests**

Append to `ArtCatalogSuite`, with the import
`distantlands.protocol.projection.OathkeeperPresentation` and
`distantlands.model.FoundationFace`:

```scala
  test("every site has one face path, named for the site"):
    assertEquals(ArtCatalog.sites.sorted, catalog.sites.map(_.name).sorted)
    catalog.sites.foreach(site => assertEquals(pathOf(site.name),
      Some(s"cards/${site.name} (NF).png"), site.name))

  test("each banner face has its file"):
    assertEquals(Vector("mob", "grand-council", "wandering-flame", "festival")
      .map(face => ArtCatalog.banner(face).map(_.path)), Vector(
      Some("cards/Banner of the People's Favor - Mob (NF).png"),
      Some("cards/Banner of the People's Favor - Grand Council (NF).png"),
      Some("cards/Banner of the Darkest Secret - Wandering Flame (NF).png"),
      Some("cards/Banner of the Darkest Secret - Festival (NF).png")))
    assertEquals(ArtCatalog.banner("unknown"), None)

  test("both faces of every foundation card have their file"):
    val numerals = Vector("I", "II", "III", "IV", "V", "VI")
    FoundationCards.all.zipWithIndex.foreach { (card, at) =>
      Vector(card.normal, card.altered).foreach(face =>
        assertEquals(ArtCatalog.foundation(at + 1, face.name).map(_.path),
          Some(s"cards/Foundation ${numerals(at)} - ${face.name} (NF).png"),
          face.name))
    }
    assertEquals(ArtCatalog.foundation(7, "Imperial Maps"), None)
    assertEquals(ArtCatalog.foundation(1, "Festival"), None)

  test("each Oath goal has its file"):
    assertEquals(OathkeeperPresentation.byGoal.keySet,
      Set("supremacy", "protection", "devotion", "the-people"))
    assertEquals(Vector("supremacy", "protection", "devotion", "the-people")
      .map(goal => ArtCatalog.oathGoal(goal).map(_.path)), Vector(
      Some("cards/Supremacy Oath (NF).png"),
      Some("cards/Protection Oath (NF).png"),
      Some("cards/Devotion Oath (NF).png"),
      Some("cards/People Oath (NF).png")))
    assertEquals(ArtCatalog.oathGoal("unknown"), None)

  test("the mat is one file"):
    assertEquals(ArtCatalog.mat.path, "tts/mat/mat.jpg")
    assert(ArtCatalog.allPaths.contains("tts/mat/mat.jpg"))
```

Change the slice 1 test "no two faces share a path, and every path is
listed once" so its count is the catalog's whole: replace
`assertEquals(ArtCatalog.allPaths.size, faces.size + 4)` with
`assertEquals(ArtCatalog.allPaths.size, faces.size + 4 + 24 + 4 + 12 + 4 + 1)`.
That edit is this task's; the test's other lines stay.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: compilation fails with `value sites is not a member of object
ArtCatalog`.

- [ ] **Step 3: Add the keys**

In `ArtCatalog.scala`, after `visions`:

```scala
  /** Every site. */
  val sites: Vector[String] = names("""
    Ancient City; Broken Peaks; Buried Giant; Deep Woods; Desolate Shore;
    Dunes; Fair Isle; Golden Valley; Great Slum; Green Shore; Headwaters;
    Hidden Place; Mines; Narrow Pass; Painted Towers; Riverbank; Rocky
    Coast; Salt Flats; Shrouded Woods; Solitary Pillar; Standing Stones;
    Steppe; Sunken Isles; Tidal Marshes
    """)

  /** A banner's file for each face, by the projection's face key. */
  private val banners: Vector[(String, ArtKey)] = Vector(
    "mob" -> "Banner of the People's Favor - Mob",
    "grand-council" -> "Banner of the People's Favor - Grand Council",
    "wandering-flame" -> "Banner of the Darkest Secret - Wandering Flame",
    "festival" -> "Banner of the Darkest Secret - Festival")
    .map((face, name) => face -> ArtKey(s"cards/$name (NF).png"))

  private val FoundationNumerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** Each foundation card's two faces, by its number, 1 to 6, and the
    * face's printed name. */
  private val foundations: Vector[((Int, String), ArtKey)] = Vector(
    1 -> "Imperial Maps", 1 -> "Wide Horizons",
    2 -> "Powerful Tribes", 2 -> "Movers and Shakers",
    3 -> "Quiet Ambitions", 3 -> "Public Ambitions",
    4 -> "Ties of Fellowship", 4 -> "Teeming World",
    5 -> "Mob's Favor", 5 -> "Grand Council",
    6 -> "Wandering Flame", 6 -> "Festival")
    .map((number, name) => (number, name) -> ArtKey(
      s"cards/Foundation ${FoundationNumerals(number - 1)} - $name (NF).png"))

  /** Each Oath goal card, by the projection's goal key. The files are
    * not named for the card's title; the card data names them. */
  private val oathGoals: Vector[(String, ArtKey)] = Vector(
    "supremacy" -> "Supremacy Oath", "protection" -> "Protection Oath",
    "devotion" -> "Devotion Oath", "the-people" -> "People Oath")
    .map((goal, name) => goal -> ArtKey(s"cards/$name (NF).png"))

  /** The mat the World pane is laid out on. */
  val mat: ArtKey = ArtKey("tts/mat/mat.jpg")

  def banner(face: String): Option[ArtKey] = banners.toMap.get(face)
  def foundation(number: Int, name: String): Option[ArtKey] =
    foundations.toMap.get((number, name))
  def oathGoal(goal: String): Option[ArtKey] = oathGoals.toMap.get(goal)
```

Add the sites to the lookup, after the base denizens:

```scala
  private val facePaths: Vector[(String, String)] =
    reprinted.map(name => name -> s"cards/$name (NF).png") ++
      baseDenizens.map(name => name -> s"cards/$name.png") ++
      sites.map(name => name -> s"cards/$name (NF).png")
```

and build `allPaths` in slice order, so the slice 1 paths keep their
places and the new ones follow them:

```scala
  val allPaths: Vector[String] =
    (reprinted.map(name => s"cards/$name (NF).png") ++
      baseDenizens.map(name => s"cards/$name.png") ++
      Vector("denizen", "vision", "relic", "legacy")
        .map(kind => backs(kind).path)) ++
      sites.map(name => s"cards/$name (NF).png") ++
      banners.map(_._2.path) ++ foundations.map(_._2.path) ++
      oathGoals.map(_._2.path) :+ mat.path
```

The `banners`, `foundations`, `oathGoals` and `mat` definitions must stand
before `allPaths` in the object.

- [ ] **Step 4: List the paths in `game-art.md`**

Add to the folder sketch a `mat/` line under `tts/`, and a bullet: "`tts/mat/`
holds the mat, `mat.jpg`, from the same mod." Change the `cards/` bullet's
first sentence to say the folder also holds the site, banner, foundation
and Oath goal cards. Append the 45 bullets in `allPaths` order; the test's
failure output lists them when the page is short.

- [ ] **Step 5: Run the tests and the link check**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: PASS, every test, the page-equality test included.

Run: `python3 scripts/check-markdown-links.py`
Expected: no error.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala \
  docs/operations/game-art.md \
  src/test/scala/distantlands/catalog/ArtCatalogSuite.scala
git commit -m "feat(protocol): add the world board's art to the catalog

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: Site cards

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala:275-310`
  (`siteRow`)
- Modify: `frontend/styles.css` (the art rules)
- Test: `frontend/src/test/scala/distantlands/frontend/ArtLayerSuite.scala`
  (append)
- Test: `frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `ArtLayer.attach(box: dom.Element, key: Option[ArtKey],
  ready: Option[dom.Element] = None): Unit`. The image goes in `box`; the
  class `art-ready` goes on `ready` when given, else on `box`.
- Consumes: `ArtCatalog.cardFace(site.label)`.

**Why a second element.** A site's row (`article.site`) holds the face
(`div.site-face`, an empty box at the card's ratio) and, beside it, the
site's printed words: the name, the defense, the power labels and the
requirement. The image belongs in the face, which has the card's ratio.
The words belong to the row. So the row is the box that turns ready, and
`--printed-opacity` on the row hides its words. The row's cards and its
empty slots are `card-face` boxes and answer for themselves.

**What stays over a site's image.** Loose tokens, pawns, forces (both
forms), the festival marker, a decision's cost badge and ring. The short
defense is printed and hides (the design, "The glance layer over art").
The power buttons are not printed: their labels hide and the buttons stay
focusable in place, with their hover notes (Q3).

- [ ] **Step 1: Write the failing tests**

Append to `ArtLayerSuite`:

```scala
  test("a load marks the element named ready, not the image's box"):
    TestArt.listing(alchemist.get.path)
    val row = dom.document.createElement("article")
    val face = dom.document.createElement("div")
    row.appendChild(face)
    ArtLayer.attach(face, alchemist, Some(row))
    one(face, "img.art").dispatchEvent(new dom.Event("load"))
    assert(row.classList.contains("art-ready"))
    assert(!face.classList.contains("art-ready"))

  test("a remembered load marks the named element at once, and an error " +
      "takes the mark back from it"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val row = dom.document.createElement("article")
    val face = dom.document.createElement("div")
    row.appendChild(face)
    ArtLayer.attach(face, alchemist, Some(row))
    assert(row.classList.contains("art-ready"))
    one(face, "img.art").dispatchEvent(new dom.Event("error"))
    assert(!row.classList.contains("art-ready"))
    assertEquals(all(face, "img"), Vector.empty)
```

Create `frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala`
with the board fixture this slice's tasks share:

```scala
package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.projection.{GameTracksProjection,
  SitePowerProjection}
import TestSites.*
import org.scalajs.dom

/** The world board with game art (game art design, "The surfaces"): the
  * mat, site cards, banners and the piles' backs. With art on a box holds
  * an `img.art` with the catalog's `src`; with art off it holds none, and
  * the board is the same either way. */
class WorldArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val alchemist = CardDetails("9", "denizen", "Alchemist",
    suit = Some("arcane"), orientation = Some("face-up"))

  /** A site named for a real site card, with a power, forces, a denizen
    * and loose tokens. */
  private def deepWoods: GameSite =
    quietSite("deep-woods", "Deep Woods").copy(defense = 1,
      powers = Vector(SitePowerProjection("action", "Homeland", Some("Rule"),
        id = "deep-woods-power")),
      forces = Some(SiteForces.Bandit(2, "Bandit Warbands")))
      .withDenizens(GameSiteCard("9", "Alchemist", Some(alchemist)))
      .withLooseTokens(1, 1)

  /** A three-region board: a real site, a site no card is named for, an
    * empty slot, decks, discards, banks and both banners. */
  private def boardWithTheWorld(): dom.Element =
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(
        GameRegion("cradle", Vector(deepWoods, quietSite("camp", "Camp")),
          discardCount = 2, discardTopCardKind = Some("denizen")),
        GameRegion("provinces", Vector(quietSite("ford", "Ford")),
          discardCount = 1, discardTopCardKind = Some("vision")),
        GameRegion("hinterland", Vector(quietSite("coast", "Coast")),
          emptySlots = 1))).copy(
      worldDeckCount = 40, worldDeckTopCardKind = Some("denizen"),
      relicDeckCount = 9,
      tracks = Some(GameTracksProjection(round = 3, visionsDrawn = 2,
        usurperLimited = false, limiterRound = 5, firstPlayerId = "red")),
      favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 3)),
      banners = Vector(BannerState("peoples-favor", "mob", None, 1),
        BannerState("darkest-secret", "festival", None, 2)),
      pawnLocations = Vector(GamePawn("red", "deep-woods"))),
      BoardOptions.none)

  /** The `src` of each image directly in `box`. */
  private def images(box: dom.Element): Vector[String] =
    all(box, ":scope > img.art").map(_.getAttribute("src"))

  private def site(board: dom.Element, label: String): dom.Element =
    one(board, s"""article.site[aria-label="$label"]""")

  test("a site named for a site card holds the card's image in its face"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    assertEquals(images(one(row, ":scope > .site-face")),
      Vector("/art/cards/Deep%20Woods%20(NF).png"))
    assertEquals(images(row), Vector.empty)

  test("the site's row is the box that turns ready, and its cards keep " +
      "their own answer"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    one(row, ".site-face > img.art").dispatchEvent(new dom.Event("load"))
    assert(row.classList.contains("art-ready"))
    assert(!one(row, ".site-face").classList.contains("art-ready"))
    assert(!one(row, ".card-face").classList.contains("art-ready"))

  test("a site no card is named for stays drawn"):
    TestArt.listingWholeCatalog()
    assertEquals(all(site(boardWithTheWorld(), "Camp"),
      ".site-face img"), Vector.empty)

  test("what lies on a site in play is not printed, and its power " +
      "buttons stay controls"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    Vector(".site-tokens", ".site-pawns", ".site-forces",
      ".site-forces-short", ".site-power").foreach(selector =>
      assert(all(row, s"$selector:not(.printed)").nonEmpty, selector))
    all(row, ".site-power").foreach(control =>
      assert(Option(control.closest(".printed")).isEmpty))
    assertEquals(all(row, ".site-face *:not(img)"), Vector.empty)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ArtLayerSuite distantlands.frontend.WorldArtSuite"`
Expected: compilation fails, `too many arguments for method attach`.

- [ ] **Step 3: Give `ArtLayer.attach` the box to mark**

In `ArtLayer.scala`, change the signature and use `marked` where the box
was marked:

```scala
  /** Lays `key`'s image over `box`, when `GameArt` has a URL for it.
    * `ready` is the box whose printed content the image replaces, when it
    * is not the box the image lies in: a site's words lie in its row,
    * beside the face that holds the image. */
  def attach(box: dom.Element, key: Option[ArtKey],
      ready: Option[dom.Element] = None): Unit =
    val marked = ready.getOrElse(box)
```

and replace the three `box.classList` calls with `marked.classList`.

- [ ] **Step 4: Attach the site's image**

In `WorldBoardRenderer.siteRow`, after `control.appendChild(face)`:

```scala
    // The image fills the face; the site's printed words lie in the row,
    // so the row is the box that turns ready.
    ArtLayer.attach(face, ArtCatalog.cardFace(site.label), Some(control))
```

with `import distantlands.protocol.art.ArtCatalog`. Update the comment
above `face` to say the face is drawn where its image is missing.

- [ ] **Step 5: Add the site's row to the box list**

Write the failing stylesheet test first. In `StylesheetArtSuite`, replace
the two box-list assertions in "printed content is hidden with opacity, by
its nearest box" with a list the later tasks extend:

```scala
  /** Every box kind that answers for printed content of its own. */
  private val boxes = ".card-face, .card-hover-summary, .site"

  test("printed content is hidden with opacity, by its nearest box"):
    assert(rule(".printed").contains("opacity: var(--printed-opacity, 1)"))
    assert(artRules.contains(s":is($boxes) { --printed-opacity: 1; }"))
    assert(artRules.contains(
      s":is($boxes).art-ready { --printed-opacity: 0; }"))
```

Run it and see it fail. Then change the two rules in `frontend/styles.css`
to `:is(.card-face, .card-hover-summary, .site)`.

A site's face is a box with a fill and a border; the image covers the fill
and the border stays. No other rule is needed: `.site-face` is placed
(`mat-slot`, `position: absolute`), so the image fills it.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ArtLayerSuite distantlands.frontend.WorldArtSuite distantlands.frontend.PrintedLayerSuite distantlands.frontend.SiteFaceSuite distantlands.frontend.ChronicleDetailsContentSuite"`
Expected: PASS, with no edit to the three existing suites.

Run: `./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ArtLayer.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/ArtLayerSuite.scala \
  frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show a site's card in its frame

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Banners

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/BannerFace.scala`
  (`render`)
- Test: `frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala`
  (append)

**Interfaces:**
- Consumes: `ArtCatalog.banner(banner.face)`.
- Produces: no new name. A banner is a `card-face` box already, so the
  box list covers it: its side and its name are printed and hide; its
  tokens stay.

- [ ] **Step 1: Write the failing tests**

Append to `WorldArtSuite`:

```scala
  test("a banner holds the image of the face it shows, and keeps what " +
      "it holds over it"):
    TestArt.listingWholeCatalog()
    val board = boardWithTheWorld()
    val favor = one(board, ".banner-peoples-favor")
    assertEquals(images(favor), Vector(
      "/art/cards/Banner%20of%20the%20People's%20Favor%20-%20Mob%20(NF).png"))
    assertEquals(images(one(board, ".banner-darkest-secret")), Vector(
      "/art/cards/Banner%20of%20the%20Darkest%20Secret%20-%20Festival%20(NF).png"))
    assertEquals(all(favor, ".card-tokens:not(.printed)").size, 1)
    assertEquals(all(favor, ".printed").map(_.getAttribute("class")
      .split(" ").head), Vector("banner-side", "card-header"))

  test("a banner on a face the catalog does not know stays drawn"):
    TestArt.listingWholeCatalog()
    assertEquals(all(BannerFace.render(
      BannerState("peoples-favor", "unknown", None, 0), None), "img"),
      Vector.empty)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.WorldArtSuite"`
Expected: the first new test FAILS with `Vector() != Vector("/art/...")`.

- [ ] **Step 3: Attach the banner's image**

In `BannerFace.render`, after `node.appendChild(tokens)`:

```scala
    ArtLayer.attach(node, ArtCatalog.banner(banner.face))
```

with `import distantlands.protocol.art.ArtCatalog`.

- [ ] **Step 4: Run the suites**

Run: `./sbtw "frontend/testOnly distantlands.frontend.WorldArtSuite distantlands.frontend.BannerFaceSuite distantlands.frontend.BannerConsentGridSuite"`
Expected: PASS, the two existing suites unedited.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/BannerFace.scala \
  frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala
git commit -m "feat(frontend): show a banner's face

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The mat and the piles on it

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  (`world`, `cradleFoot`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala`
  (a helper beside `deckCard`)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala`
  (append)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Consumes: `ArtCatalog.mat`, `ArtCatalog.cardBack(kind)`.
- Produces: `ServerUiSupport.pileBack(pile: dom.Element, count: Int,
  topCardKind: Option[String], sideways: Boolean): Unit`, which lays the
  top card's back over the pile's `.deck-card` and, for a pile that lies
  on its side, adds the class `deck-sideways` to the card for art on and
  off alike.

**The mat.** One image in `.mat-board`. When it is ready the board hides
what it prints that is not inside a box of its own: the region names, the
deck captions, the Visions Drawn cells and label, the round wheel's
segments, numbers and end dice, the favor banks' suit glyphs and an empty
site slot's frame. The board's own gradient is a fill, which the image
covers. What stays: every site row, card, pile, pawn, token and count; the
current round's wedge and the limiter mark; the Visions Drawn marker; the
dashed outline of an empty card slot, which the mat does not print and a
card play needs.

**The piles.** A pile on the mat is a `.deck-card` holding a printed
letter and the count. It becomes a box: the letter hides under the back,
the count stays. The relic deck is square and its back upright. The world
deck and the discards lie on their sides, so their back is turned a
quarter as a Vision's face is (R8). The kind comes from the projection:
`worldDeckTopCardKind`, `discardTopCardKind`, and `"relic"` for the relic
deck. No kind, or an empty pile, attaches nothing.

- [ ] **Step 1: Write the failing tests**

Append to `WorldArtSuite`:

```scala
  test("the board holds the mat's image, directly in the board"):
    TestArt.listingWholeCatalog()
    assertEquals(images(one(boardWithTheWorld(), ".mat-board")),
      Vector("/art/tts/mat/mat.jpg"))

  test("what the mat prints is printed content of the board itself, in " +
      "no box of its own"):
    val board = one(boardWithTheWorld(), ".mat-board")
    val boxes = ".card-face, .card-hover-summary, .site, .deck-card"
    Vector(".region-label", ".deck-caption", ".vision-cell",
      ".visions-drawn-label", ".round-segment", ".round-number",
      ".site-slot-empty").foreach { selector =>
      val found = all(board, selector)
      assert(found.nonEmpty, selector)
      found.foreach { node =>
        assert(node.classList.contains("printed"), selector)
        assertEquals(Option(node.closest(boxes)), None, selector)
      }
    }

  test("what lies on the mat in play is not printed"):
    val board = one(boardWithTheWorld(), ".mat-board")
    Vector(".vision-marker", ".pile-count", ".favor-bank-count",
      ".site-pawns .pawn").foreach(selector =>
      all(board, selector).foreach(node =>
        assertEquals(Option(node.closest(".printed")), None, selector)))

  test("a pile on the mat holds the back of its top card's kind, and " +
      "keeps its count"):
    TestArt.listingWholeCatalog()
    val board = one(boardWithTheWorld(), ".mat-board")
    def pile(caption: String): dom.Element =
      all(board, ".deck-figure").find(figure =>
        one(figure, ".deck-caption").textContent == caption).map(figure =>
        one(figure, ".deck-card")).get
    assertEquals(images(pile("Relic deck")),
      Vector("/art/tts/backs/relic-back.jpg"))
    assertEquals(images(pile("World deck")),
      Vector("/art/tts/backs/denizen-back.jpg"))
    assertEquals(all(board, ".deck-figure .deck-card").map(images),
      Vector(Vector("/art/tts/backs/denizen-back.jpg"),
        Vector("/art/tts/backs/vision-back.jpg"), Vector.empty,
        Vector("/art/tts/backs/relic-back.jpg"),
        Vector("/art/tts/backs/denizen-back.jpg")))
    assertEquals(one(pile("World deck"), ".pile-count").textContent, "×40")

  test("the world deck and the discards lie on their sides, with art or " +
      "without; the relic deck does not"):
    def sideways(board: dom.Element): Vector[Boolean] =
      all(board, ".deck-figure .deck-card")
        .map(_.classList.contains("deck-sideways"))
    val expected = Vector(true, true, true, false, true)
    assertEquals(sideways(boardWithTheWorld()), expected)
    TestArt.listingWholeCatalog()
    assertEquals(sideways(boardWithTheWorld()), expected)

  /** `node`'s markup with every art image taken out. */
  private def withoutArt(node: dom.Element): String =
    val copy = node.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, "img.art").foreach(_.remove())
    copy.outerHTML

  test("the world drawn with art and without has the same boxes, classes " +
      "and inline geometry once the images are removed"):
    val drawn = boardWithTheWorld()
    TestArt.listingWholeCatalog()
    val withArt = boardWithTheWorld()
    // The mat, one site, one denizen, two banners and four piles.
    assertEquals(all(withArt, "img.art").size, 9)
    assertEquals(withoutArt(withArt), drawn.outerHTML)

  test("with art off the world holds no image"):
    TestArt.listingWholeCatalog()
    GameArt.setEnabled(false)
    assertEquals(all(boardWithTheWorld(), "img"), Vector.empty)
```

The order of `.deck-figure` in the document is the Cradle's discard, the
Cradle's foot (relic deck, world deck), then the other regions' discards;
if the fixture's order differs, read it from the failure and set the
expected vectors to the document's order, keeping one pile of each case.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.WorldArtSuite"`
Expected: the mat, pile, sideways and invariance tests FAIL.

- [ ] **Step 3: Attach the mat and the piles' backs**

In `ServerUiSupport`, beside `deckCard`:

```scala
  /** Lays the back of a pile's top card over the pile drawn in `pile`
    * (game art design, "Card faces"): the kind's back when the viewer may
    * know the kind and the pile holds a card. A pile that lies on its
    * side is marked `deck-sideways`, with art or without, so the
    * stylesheet turns its back a quarter. */
  private[frontend] def pileBack(pile: dom.Element, count: Int,
      topCardKind: Option[String], sideways: Boolean): Unit =
    Option(pile.querySelector(".deck-card")).foreach { card =>
      if sideways then card.classList.add("deck-sideways")
      if count > 0 then
        ArtLayer.attach(card, topCardKind.flatMap(art.ArtCatalog.cardBack))
    }
```

In `WorldBoardRenderer.world`:

- after `val deck = deckBody(...)`:
  `pileBack(deck, value.worldDeckCount, value.worldDeckTopCardKind, sideways = true)`
- after `val discard = openableDeck(...)`:
  `pileBack(discard, region.discardCount, region.discardTopCardKind, sideways = true)`
- after `board.appendChild(favorBanks(value, options))`:
  `ArtLayer.attach(board, Some(ArtCatalog.mat))`

In `cradleFoot`, build the relic deck's body in a value and call
`pileBack(relics, value.relicDeckCount, Some("relic"), sideways = false)`
before placing it.

- [ ] **Step 4: Write the failing stylesheet tests**

In `StylesheetArtSuite`, extend `boxes` to
`".card-face, .card-hover-summary, .site, .deck-card, .mat-board"` and
add:

```scala
  test("a pile on its side turns its back a quarter, as a Vision is " +
      "turned, and sizes it to the pile"):
    val turned = rule(".mat-board .deck-sideways > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% * (var(--ratio-denizen)))",
      "height: calc(100% / (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(turned.contains(part), part))

  test("a pile's count stays readable over its back"):
    val count = rule(".deck-card.art-ready > .pile-count")
    Vector("background: var(--base)", "border-radius", "padding")
      .foreach(part => assert(count.contains(part), part))

  test("the mat's image fills the board"):
    assert(rule(".mat-board > .art").contains("object-fit: fill"))
```

Run: `./sbtw "root/testOnly distantlands.server.StylesheetArtSuite"`
Expected: FAIL.

- [ ] **Step 5: Add the rules**

In `frontend/styles.css`, change the box list in both rules to
`:is(.card-face, .card-hover-summary, .site, .deck-card, .mat-board)` and
add after the Vision's rules:

```css
/* The mat under the board. The board has the mat's proportions, so the
   image fills it exactly; `fill` keeps a rounding pixel from leaving a
   strip of the drawn lights at an edge. */
.mat-board > .art { object-fit: fill; }
/* A pile on the mat shows the back of its top card. The world deck and
   the discards lie on their sides, so their back is turned a quarter, the
   way a Vision is, the image's width being the pile's height. The count
   is what lies there in play: it keeps a ground of its own over the
   back. */
.mat-board .deck-sideways > .art { inset: auto; top: 50%; left: 50%;
  width: calc(100% * (var(--ratio-denizen)));
  height: calc(100% / (var(--ratio-denizen)));
  transform: translate(-50%, -50%) rotate(-90deg); }
.deck-card.art-ready > .pile-count { padding: 0 0.35em;
  border-radius: 0.3em; background: var(--base); }
```

`.deck-card` must be a positioned box for the image to fill it. Check its
computed `position` in the browser check; if it is `static`, add
`.mat-board .deck-card { position: relative; }` to the rules above and a
line to the stylesheet test.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS.

Run: `./sbtw frontend/test`
Expected: PASS. `DeckFigureSuite`, `DiscardPileSuite` and `MatBoardSuite`
pass unedited unless one compares a pile's whole class list; if so, the
new `deck-sideways` class is the only change to make, listed as a
deviation.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): lay the mat under the board and backs on its piles

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: Foundations and the Oath goal in the overlay

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspection.scala`
  (`Request.Text`, `openText`)
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala`
  (`showText`)
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`
  (the `Request.Text` case)
- Modify: `frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala`
  (`Card`), `FoundationStrip.scala` (`ofGame`)
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  (`sharedBank`'s Oath goal button)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/PlateArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces:
  - `CardInspection.Request.Text(title, lines, origin, plate: Option[String]
    = None, art: Option[ArtKey] = None)`
  - `CardInspection.openText(title, lines, origin, plate = None, art = None)`
  - `CardInspectionOverlay.showText(title, lines, origin, plate = None,
    art = None)`
  - `SetupCardStrip.Card(..., plate: Option[String], art: Option[ArtKey] =
    None)`
- Consumes: `ArtCatalog.foundation(number, name)`, `ArtCatalog.oathGoal(goal)`.

A foundation and the Oath goal are words on the table and a plate at the
card's ratio in the overlay, "where its image will go" (Q1). The plate
holds the title, marked printed. The image goes on the plate; the plate
joins the box list. The Oathkeeper title pill on a board opens the plate
`oathkeeper-title`, whose art is a slice 3 token and is passed no key
here. The Chronicle Phase aids pass no key.

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/PlateArtSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.projection.{FoundationProjection,
  OathkeeperProjection}
import org.scalajs.dom

/** The printed cards the table shows as words have their image on the
  * inspection overlay's plate (game art design, "Card faces"): a
  * foundation and the Oath goal. */
class PlateArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** Runs `check` against a shell's mount, and disposes of the shell. */
  private def withShell(check: dom.Element => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try check(browser.mount)
    finally
      shell.dispose()
      browser.close()

  private def plateImages(mount: dom.Element): Vector[String] =
    all(mount, ".card-overlay .card-plate > img.art")
      .map(_.getAttribute("src"))

  test("a foundation's plate holds the image of the face in use"):
    TestArt.listingWholeCatalog()
    withShell { mount =>
      val strip = FoundationStrip.render(FoundationStrip.ofGame(Vector(
        FoundationProjection(1, "altered", "Wide Horizons", "Rule"))))
      mount.appendChild(strip)
      one(strip, ".foundation-card").asInstanceOf[dom.html.Button].click()
      assertEquals(plateImages(mount), Vector(
        "/art/cards/Foundation%20I%20-%20Wide%20Horizons%20(NF).png"))
      // The title on the plate is printed; the rules beside it are not.
      assertEquals(all(mount, ".card-plate > .printed").size, 1)
      assertEquals(all(mount, ".card-overlay-details .printed").size, 0)
    }

  test("the strip's rows hold no image"):
    TestArt.listingWholeCatalog()
    assertEquals(all(FoundationStrip.render(FoundationStrip.ofGame(Vector(
      FoundationProjection(1, "normal", "Imperial Maps", "Rule")))), "img"),
      Vector.empty)

  test("the Oath goal's plate holds the goal card's image"):
    TestArt.listingWholeCatalog()
    withShell { mount =>
      val world = WorldBoardRenderer.world(TestProjections.actingProjection()
        .copy(oathkeeper = Some(OathkeeperProjection("supremacy", None,
          "oathkeeper", usurperLimited = false, winnerPlayerId = None))),
        BoardOptions.none)
      mount.appendChild(world)
      one(world, ".oathkeeper-goal").asInstanceOf[dom.html.Button].click()
      assertEquals(plateImages(mount),
        Vector("/art/cards/Supremacy%20Oath%20(NF).png"))
    }

  test("a plate with no key, and any plate with art off, stays drawn"):
    withShell { mount =>
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      mount.appendChild(origin)
      TestArt.listingWholeCatalog()
      CardInspection.openText("Oathkeeper", Vector("Line"), origin,
        Some("oathkeeper-title"))
      assertEquals(plateImages(mount), Vector.empty)
      GameArt.setEnabled(false)
      CardInspection.openText("Foundation I · Wide Horizons", Vector("Rule"),
        origin, Some("foundation"),
        distantlands.protocol.art.ArtCatalog.foundation(1, "Wide Horizons"))
      assertEquals(plateImages(mount), Vector.empty)
    }
```

If `OathkeeperProjection` has more fields than the five named, fill them
from its definition in `ActionProjectionDtos.scala`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.PlateArtSuite"`
Expected: compilation fails, `too many arguments for method openText`.

- [ ] **Step 3: Carry the key to the plate**

1. `CardInspection.Request.Text` and `openText` take
   `art: Option[ArtKey] = None` last and pass it on.
2. `GameTableShell`'s case becomes
   `case CardInspection.Request.Text(title, lines, origin, plate, art) =>
   inspector.showText(title, lines, origin, plate, art)`.
3. `CardInspectionOverlay.showText` takes `art: Option[ArtKey] = None`
   and, where it builds the plate, calls `ArtLayer.attach(box, art)` after
   the title is appended.
4. `SetupCardStrip.Card` gains `art: Option[ArtKey] = None` last, and
   `face` passes `card.art` to `openText`.
5. `FoundationStrip.ofGame` passes
   `ArtCatalog.foundation(foundation.number, foundation.name)`.
6. `WorldBoardRenderer.sharedBank`'s goal button passes
   `ArtCatalog.oathGoal(oath.goal)`.

- [ ] **Step 4: Add the plate to the box list**

Extend `boxes` in `StylesheetArtSuite` to
`".card-face, .card-hover-summary, .site, .deck-card, .mat-board, .card-plate"`
and add:

```scala
  test("a plate is a positioned box, so its image fills it"):
    assert(rule(".card-plate:has(> .art)").contains("position: relative"))
```

See it fail, then change both box-list rules in `frontend/styles.css` and
add after the pile rules:

```css
/* A foundation's and the Oath goal's plate in the overlay holds the
   card's image; the title on it is printed and hides. */
.card-plate:has(> .art) { position: relative; }
```

- [ ] **Step 5: Run the suites**

Run: `./sbtw frontend/test` and
`./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS; `FoundationStripSuite`, `SetupCardStripSuite`,
`ShellInspectionSuite` and `ChronicleTaskStripSuite` unedited.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/CardInspection.scala \
  frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala \
  frontend/src/main/scala/distantlands/frontend/GameTableShell.scala \
  frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala \
  frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/PlateArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show foundation and Oath goal cards in the overlay

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: The three rectangles placed by eye

**Files:** none, unless a rectangle covers printed art. Then:
`frontend/src/main/scala/distantlands/frontend/MatLayout.scala`,
`docs/catalog/asset-proportions.md`, `MatLayoutSuite.scala`, and the
roadmap's note under L3 is left for slice 3 to delete.

The tracker's line of text (`MatLayout.trackNote`, 71, 1500, 1790×80), the
Visions Drawn label (`visionsLabel`, 1680, 1650, 210×150) and the six
favor bank boxes (`favorBank`, centre − 217, 2050, 310×240) have no
printed counterpart on the mat.

- [ ] **Step 1: Start the scratch server and open a table**

As in slice 1, Task 9, Step 2, with `--art-path
/Users/roman/projects/distantlands/reference/assets` on port 8083.

- [ ] **Step 2: Look at each rectangle over the mat**

With art on, at map scale 1 and at Fit, for each of the three kinds:

1. Read its bounding rectangle from the page (`.track-note`, the
   `.visions-drawn-label`, each `.favor-bank`).
2. Outline it for inspection with the browser tools (a debugging style in
   the page, not a source edit) and take a zoomed screenshot of it over
   the mat.
3. Judge whether it covers printed art that is not its own counterpart:
   lettering, a frame, a track cell, an emblem that another piece does not
   already stand for.

With the mat ready, the Visions Drawn label is printed and hides, so only
its place matters with art off; the tracker's line and the banks' counts
are live and stay. For a bank, the drawn suit glyph hides and the count
and favor glyph stay: check they lie inside the mat's printed circle and
emblem box and cover no lettering.

- [ ] **Step 3: Move a rectangle that covers printed art**

Only if Step 2 finds one. Measure the free place on the mat image in its
own pixels, change the number in `MatLayout` and the row in
`asset-proportions.md`, and update the `MatLayoutSuite` assertion that
pins it, test first. The move applies with art on and off alike. Commit:

```bash
git add frontend/src/main/scala/distantlands/frontend/MatLayout.scala \
  frontend/src/test/scala/distantlands/frontend/MatLayoutSuite.scala \
  docs/catalog/asset-proportions.md
git commit -m "fix(frontend): move a mat rectangle off the mat's printed art

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

- [ ] **Step 4: Record the result**

For the report: each rectangle, what it lies over, and whether it moved,
with the numbers before and after.

---

### Task 7: Gate and browser check

- [ ] **Step 1: Run the full gate**

```bash
./sbtw test
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```

Run it after the last commit, so its numbers are HEAD's.

- [ ] **Step 2: Check the world board in the browser**

On the scratch server, at 1440×900, 1024×768 and 375×812:

1. The startup line counts the slice's files and warns of none, or names
   what the folder lacks.
2. With art on: the mat lies under the board and its lettering, wheel and
   banks line up with the pieces on them; each site shows its card in its
   frame, with pawns, forces and loose tokens over it; both banners show
   their faces with their counts; the piles show backs with counts, the
   sideways ones turned; an empty site slot shows the mat's frame.
3. The bounding rectangles of every `.site`, `.site-face`, `.card-face`,
   `.mat-slot`, `.deck-card`, `.favor-bank`, `.game-pane` and the
   `.mat-board` are equal with art on and off, at each size.
4. A site's power buttons: reach each with the keyboard, see its focus
   ring and hover note over the image, and open it. Report where they lie
   on the card's art (Q3).
5. The glance layer at Fit and at the tiny scale: a ready site keeps its
   pawns, forces and tokens, and its short defense is hidden.
6. A site whose image fails stays drawn while the mat and its neighbours
   keep their art, and the mat failing leaves the drawn lights and every
   printed label back in place. Dispatch `error` for inspection if no
   request can be blocked, and say so.
7. The inspection overlay: a foundation's plate and the Oath goal's plate
   show their images; Chronicle Details' Atlas rows show site art (Q5).
8. What slice 1 could not observe, by playing the scratch game forward
   (Dev tools switch the seat): a knowable card's hover summary showing
   its face while the box shows its back; tokens lying on an art card; a
   card offered as a board option, with its inspect button, pick mark and
   Replace badge above the art; a Vision in a row and in the overlay if
   one can be reached. Report each as observed or not reached.
9. No error in the browser console.

- [ ] **Step 3: Stop the scratch server and report**

`lsof -nP -iTCP:8083 -sTCP:LISTEN` prints nothing. Report commits, gate
numbers, the browser check, the three rectangles, deviations, open
questions and anything left undone.

---

## Self-Review

**Spec coverage, slice 2 ("The world board").**

| Spec | Task |
|---|---|
| The mat: one image behind the slots; printed washes, frames, names and labels hide; live layer unchanged | 4 |
| The site card: its face in the frame; power buttons stay clickable | 2 |
| Banners (`BannerFace`) | 3 |
| Legacies: the Public Ambitions row draws `CardFace` (slice 1); the pills stay words | Q1, Q2 |
| Foundations and Oath goals | 5, Q1 |
| Decks' and discards' backs, left from slice 1 | 4 |
| The glance layer over art | 2, 7 |
| The three rectangles placed by eye | 6 |
| Catalog and `game-art.md` kept equal | 1 |
| Surface tests, layout invariance | 2 to 5 |
| Browser check at three sizes | 7 |

Left to slice 3: symbols in text, pieces, tokens (the Oathkeeper title,
the festival marker, the first player and Visions Drawn markers), dice,
`DESIGN.md`, the quick start and `configuration.md` links, and L3's
removal from the roadmap.

**Placeholder scan.** Task 1 Step 4 and Task 5 Step 3 describe edits in
words where the edit is a parameter passed through; each names the exact
parameter and where it goes. Task 6 changes code only if the check finds a
defect, and says how.

**Type consistency.** `ArtLayer.attach(box, key, ready)`;
`ArtCatalog.sites`, `banner(face)`, `foundation(number, name)`,
`oathGoal(goal)`, `mat`; `ServerUiSupport.pileBack(pile, count,
topCardKind, sideways)`; `CardInspection.openText(..., plate, art)`;
`SetupCardStrip.Card(..., plate, art)`. The box list is one string in
`StylesheetArtSuite`, extended in Tasks 2, 4 and 5 to
`.card-face, .card-hover-summary, .site, .deck-card, .mat-board, .card-plate`.
