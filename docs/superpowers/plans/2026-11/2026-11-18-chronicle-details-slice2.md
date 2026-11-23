# Chronicle Details, Slice 2: Aids and Strip Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The five Chronicle task cards' text lives in a static
`ChronicleTaskPresentation`. The foundation strip becomes a generic
`SetupCardStrip`, and the foundation cards and the task cards are both
built for it.

**Architecture:** `ChronicleTaskPresentation` sits in
`shared/.../protocol/projection` beside `OathkeeperPresentation` (ruling
D4). It holds the five cards in Chronicle order, each with a title and one
or two faces, and joins a card's faces into the lines the inspector reads.
In the frontend, `SetupCardStrip` draws any strip of setup cards under a
heading, with a class stem that names its classes. `FoundationStrip`
keeps `ofGame`, `numeral` and a `render` that passes its cards to
`SetupCardStrip`, so the Shared Bank and its suites see no change. A new
`ChronicleTaskStrip` builds the task cards and the strip headed "Chronicle
Phase". Nothing draws the task strip until slice 4, which also styles it.

**Why a class stem.** The foundation strip's classes (`foundations`,
`foundations-title`, `foundation-strip`, `foundation-card`,
`foundation-number`, `foundation-name`, `foundation-mark`) are what its
suites, `PanelPlacementSuite`, `ChroniclePageSuite` and `styles.css` read.
`FoundationStripSuite` also checks that a card's `class` is exactly
`foundation-card`. The design requires those tests to pass unchanged, so a
strip cannot add a shared class to its cards. Each strip instead gets its
classes from a stem: `foundation` for the foundations, `chronicle-task` for
the aids (R7).

**Why the task cards are built in the frontend.** The design says
`ChronicleTaskPresentation` builds the aid cards. A strip card is a
frontend type, and `shared` cannot see the frontend. So
`ChronicleTaskPresentation.lines` joins the faces in `shared`, and
`ChronicleTaskStrip.cards` in the frontend makes the strip cards from it.

**Tech Stack:** Scala 3.9.0, munit, Scala.js frontend, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Chronicle Details design](../../specs/2026-11/2026-11-18-chronicle-details-design.md),
ruling D4 and section "Slice 2: aids and strip". Rulings made while this
plan runs go in the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md),
from R6.

## Global Constraints

- D4: the task card text is static, in `ChronicleTaskPresentation` in
  `shared/src/main/scala/distantlands/protocol/projection`, not in the
  projection.
- The five cards, in Chronicle order: The Sun, The Throne, The World, The
  Beacon, The Stars. The Sun and The Throne have a front and a back; the
  others have one face.
- The text is transcribed from the local card data,
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (`OATH-CHRON01a-NF` to `OATH-CHRON05-NF`). Never fetch the web. Where the
  printed card image (`reference/2026-03-17-new-foundations-rev1/chronicle-tasks.pdf`)
  words a step differently, the card data wins: the design names it. For
  example, the image prints Crown Chancellor on the Sun's front, and the
  card data puts it on the back.
- A two-sided card's lines are its front's, a "Back" line, then its
  back's.
- A strip card has an optional mark (the foundation's numeral; none for a
  task card), a name, an optional tag ("Altered"), an inspector title and
  the lines its click opens. It also carries the label and the plate the
  foundation strip already sets, so the foundations draw as before.
- `FoundationStripSuite`, `PanelPlacementSuite` and `ChroniclePageSuite`
  pass unchanged. The Shared Bank's DOM does not change.
- Protected (do not change): slice 1's files except where this plan names
  them (it names none); `styles.css` (slice 4 styles the aid strip);
  `CardInspection` and `CardInspectionOverlay`; `RulesTextRenderer`.
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
  design, slice 2".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## Rulings

Recorded in the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md);
the tasks below apply them.

- **R6.** Drop each card data symbol and keep its words; the Sun's
  legacy-goal legend is one line per goal; drop "Now, flip this card
  over", since the "Back" line marks the flip. The card images agree with
  the card data, which wins over the printed sheet. Task 1.
- **R7.** One class stem per strip: `foundation` keeps every foundation
  class; the aids use `chronicle-task` (`chronicle-tasks`,
  `chronicle-tasks-title`, `chronicle-task-strip`, `chronicle-task-card`,
  `chronicle-task-name`). Slice 4 adds their CSS. Tasks 2 and 3.
- **R8.** The strip shows "The Sun"; the inspector title and the hover
  note read "Chronicle task · The Sun"; the button label reads "Chronicle
  task, The Sun"; no plate. Task 3.

## Deviations already made in this plan

- `FoundationStrip.Card` goes; `FoundationStrip.ofGame` returns
  `Vector[SetupCardStrip.Card]`. `FoundationStrip.render(cards)` stays as
  a one-line call to `SetupCardStrip.render`, so `WorldBoardRenderer` and
  `FoundationStripSuite` are unchanged. `FoundationStrip.title` and `label`
  take the number, name and face instead of a `Card`.
- `ChronicleTaskStrip` (frontend) builds the task cards, since `shared`
  cannot build a frontend type (see "Why the task cards are built in the
  frontend").
- The strip card's `mark` is drawn in the `-number` span and its `tag` in
  the `-mark` span, the classes the foundation strip already uses for the
  numeral and "Altered".

## Files

- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleTaskPresentation.scala`
- Create: `shared/src/test/scala/distantlands/protocol/ChronicleTaskPresentationSuite.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala`
- Create: `frontend/src/test/scala/distantlands/frontend/SetupCardStripSuite.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleTaskStrip.scala`
- Create: `frontend/src/test/scala/distantlands/frontend/ChronicleTaskStripSuite.scala`
- Modify docs: `DESIGN.md` ("Foundation Strip"), the spec's status line.

## Running things

- Shared sources build in the root project, so the root runs their suites.
- Shared suite: `./sbtw "testOnly distantlands.protocol.ChronicleTaskPresentationSuite"`.
- Frontend suites: `./sbtw "frontend/testOnly distantlands.frontend.SetupCardStripSuite distantlands.frontend.ChronicleTaskStripSuite distantlands.frontend.FoundationStripSuite distantlands.frontend.PanelPlacementSuite distantlands.frontend.ChroniclePageSuite"`.
- Full gate at slice end, long ones in the background: `./sbtw compile`,
  `./sbtw test`, `./sbtw frontend/test`, `./sbtw frontend/fullOptJS`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory below was read at `c60a3fb7d`. Each task begins by running
its grep at HEAD. Treat what the grep shows as the list; handle a hit
missing from the task by the task's rule, and report it.

---

### Task 1: The five Chronicle task cards

**Files:**
- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleTaskPresentation.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ChronicleTaskPresentationSuite.scala`

**Interfaces:**
- Produces, in package `distantlands.protocol.projection`:
  `final case class ChronicleTaskFace(label: String, lines: Vector[String])`;
  `final case class ChronicleTaskPresentation(title: String, faces: Vector[ChronicleTaskFace])`
  with `def lines: Vector[String]`;
  `ChronicleTaskPresentation.all: Vector[ChronicleTaskPresentation]`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'ChronicleTask(Presentation|Face)' -- shared src frontend
grep -n -E 'id: OATH-CHRON|flipSide' ~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml | grep -A1 CHRON
```
Expected: no hit in the code. The card data lists `OATH-CHRON01a-NF`,
`01b`, `02a`, `02b`, `03`, `04` and `05`. The `a` and `b` ids flip to each
other. If the card text differs from the strings in Step 4, the card data
wins: report each difference.

- [ ] **Step 2: Write the failing test**

Create `shared/src/test/scala/distantlands/protocol/ChronicleTaskPresentationSuite.scala`:

```scala
package distantlands.protocol

import distantlands.protocol.projection.{ChronicleTaskFace,
  ChronicleTaskPresentation}

/** The five Chronicle task cards as the card data words them (Chronicle
  * Details design, slice 2): their order, their faces, and the steps each
  * face holds. */
class ChronicleTaskPresentationSuite extends munit.FunSuite:
  private val Heading = """^\*\*(.+?)\.\*\*""".r.unanchored

  /** Each line's bold step name, where it opens with one. */
  private def stepsOf(face: ChronicleTaskFace): Vector[String] =
    face.lines.collect { case Heading(step) => step }

  private def card(title: String): ChronicleTaskPresentation =
    ChronicleTaskPresentation.all.find(_.title == title)
      .getOrElse(fail(s"no task card $title"))

  test("the five cards stand in Chronicle order, and only the Sun and the " +
      "Throne have a back"):
    assertEquals(ChronicleTaskPresentation.all.map(task =>
      task.title -> task.faces.map(_.label)), Vector(
        "The Sun" -> Vector("Front", "Back"),
        "The Throne" -> Vector("Front", "Back"),
        "The World" -> Vector("Front"),
        "The Beacon" -> Vector("Front"),
        "The Stars" -> Vector("Front")))

  test("each face holds the steps the card data prints on it"):
    assertEquals(ChronicleTaskPresentation.all.map(_.faces.map(stepsOf)),
      Vector(
        Vector(Vector("Resolve Legacies"),
          Vector("Crown Chancellor", "Assign Tasks")),
        Vector(Vector("Offer Citizenship", "Shape Empire", "Build Edifice",
            "Stock Reliquary"),
          Vector("Scatter Relics", "Score Lineage Points", "Swear an Oath")),
        Vector(Vector("Separate Visions", "Gather Denizens",
          "Protect and Dispossess")),
        Vector(Vector("Introduce Newcomers", "Welcome a Stranger",
          "Shape Foundation")),
        Vector(Vector("Write in the Stars", "Archive Rival Sites",
          "Clean Up", "Play Again or Store"))))

  test("the Sun's front lists the six legacy goals in words, one a line"):
    assertEquals(card("The Sun").faces.head.lines.tail, Vector(
      "Hold People's Favor.", "Hold Darkest Secret.", "Rule the most sites.",
      "Hold Grand Scepter. _If none, win._",
      "Rule most cards of suit _(even advisers)_.",
      "Hold relic of highest value _(under name)_."))

  test("a two-sided card reads its front, a Back line, then its back; a " +
      "one-sided card reads its face"):
    val sun = card("The Sun")
    assertEquals(sun.lines,
      sun.faces(0).lines ++ Vector("Back") ++ sun.faces(1).lines)
    val world = card("The World")
    assertEquals(world.lines, world.faces.head.lines)

  test("no line carries a card data symbol or the flip instruction"):
    val lines = ChronicleTaskPresentation.all.flatMap(_.lines)
    assert(!lines.exists(line => line.contains("symbol:") ||
      line.contains("`") || line.contains("flip this card")), lines)
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleTaskPresentationSuite"`
Expected: compilation fails with "value ChronicleTaskFace is not a member
of distantlands.protocol.projection".

- [ ] **Step 4: Write the presentation**

Create `shared/src/main/scala/distantlands/protocol/projection/ChronicleTaskPresentation.scala`:

```scala
package distantlands.protocol.projection

/** One printed side of a Chronicle task card: its label ("Front" or
  * "Back") and its lines, one a paragraph, in the rules text markup the
  * inspector renders. */
final case class ChronicleTaskFace(label: String, lines: Vector[String])

/** A Chronicle task card as the card data words it (Chronicle Details
  * design, ruling D4 and slice 2): its title and its faces, front first.
  * Static, beside [[OathkeeperPresentation]]: the text is the same in every
  * game, so the projection does not carry it.
  *
  * The text is transcribed from the New Foundations card data
  * (`OATH-CHRON01a-NF` to `OATH-CHRON05-NF`). Each icon the data marks as a
  * symbol is left out and its words kept, and the fronts' "Now, flip this
  * card over." is left out, since the "Back" line stands where the flip
  * happens (implementation ruling R6). */
final case class ChronicleTaskPresentation(title: String,
    faces: Vector[ChronicleTaskFace]):
  /** What the inspector reads: the front's lines, then each later face's
    * label and lines. */
  def lines: Vector[String] = faces match
    case front +: rest =>
      front.lines ++ rest.flatMap(face => face.label +: face.lines)
    case _ => Vector.empty

object ChronicleTaskPresentation:
  private def front(lines: String*) = ChronicleTaskFace("Front", lines.toVector)
  private def back(lines: String*) = ChronicleTaskFace("Back", lines.toVector)

  val sun: ChronicleTaskPresentation = ChronicleTaskPresentation("The Sun",
    Vector(
      front(
        "**Resolve Legacies.** Reveal facedown relics held by players. " +
          "Dormant legacies become active if their goal is met. If not, " +
          "discard them. All Imperials rule Imperial sites for legacy " +
          "goals. Ties between only Imperials are friendly. All other ties " +
          "are unfriendly. Active legacies of Exiles become dormant if goal " +
          "is not met. Players discard down to 3 active legacies. Shuffle " +
          "discarded legacies into legacy deck.",
        "Hold People's Favor.",
        "Hold Darkest Secret.",
        "Rule the most sites.",
        "Hold Grand Scepter. _If none, win._",
        "Rule most cards of suit _(even advisers)_.",
        "Hold relic of highest value _(under name)_."),
      back(
        "**Crown Chancellor.** The winner becomes the Chancellor and takes " +
          "the Grand Scepter. **If an Exile won,** their newly dormant " +
          "legacies become active. Old Imperials, even in the box, become " +
          "Exiles. Discard the Reliquary. Remove Imperial warbands from " +
          "map, then mark winner's sites with Imperial warbands. **If a " +
          "Citizen won,** old Chancellor turns Citizen.",
        "**Assign Tasks.** The winner takes the Throne task card. The " +
          "player with the People's Favor takes the World task card. The " +
          "player with the Darkest Secret takes the Beacon task card. **If " +
          "no one holds a banner,** the winner may give its task to anyone. " +
          "**If a player holds both the Beacon and the World tasks,** they " +
          "must give away one of them.")))

  val throne: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Throne", Vector(
      front(
        "**Offer Citizenship.** You may offer Citizenship to Exiles: Their " +
          "board flips. Their newly dormant legacies become active. You " +
          "take rule of their sites.",
        "**Shape Empire.** You may remove warbands from sites you rule, " +
          "except your last site. If you rule none, replace the warbands " +
          "on your site with one of yours. Then, discard all denizens " +
          "**you don't rule** except intact edifices. _Discarded ruined " +
          "edifices go to the bottom of the edifice deck in the box._",
        "**Build Edifice.** Draw the topmost edifice of each suit. You may " +
          "bury a denizen at a site you rule to replace it with the " +
          "matching drawn edifice. _Burying ignores the locked restriction " +
          "and places the card on the bottom of its respective deck._ " +
          "Reshuffle the edifice deck.",
        "**Stock Reliquary.** Add all your relic cards to the Reliquary " +
          "facedown. Discard relics in excess of five."),
      back(
        "**Scatter Relics.** Set aside the Grand Scepter and Reliquary. " +
          "Shuffle together discarded relics and relics held by players. " +
          "Deal relics from this pile facedown to fill all empty relic " +
          "slots at sites, going Hinterward. Return remaining relics to the " +
          "relic deck.",
        "**Score Lineage Points.** The winner scores 3 points + extra " +
          "points for their legacies: each legacy scores 1 point per " +
          "matching card **at sites** they rule. Then, they may give points " +
          "to other players, spending 2 points for each point given. " +
          "Finally, record scores, circle the Chancellor's score, and " +
          "underline each Citizen's score.",
        "**Swear an Oath.** If a Vision won, set the Oathkeeper goal to " +
          "match it. Otherwise, choose an Oathkeeper goal that is different " +
          "from the current one.")))

  val world: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The World", Vector(front(
      "**Separate Visions.** Set aside all 5 Vision cards.",
      "**Gather Denizens.** Shuffle all denizens from the discard piles and " +
        "players' advisers into a stack. Return all but 9 denizens from " +
        "this stack to the world deck. _Don't look at these cards._ Split " +
        "these 9 denizens into three stacks of 3 denizens each. Flip two of " +
        "these stacks faceup.",
      "**Protect and Dispossess.** Choose one faceup denizen stack to " +
        "protect: Secretly choose 1 denizen as a facedown starting adviser " +
        "you keep for the next game, and add the other 2 denizens facedown " +
        "to the world deck. Place the other two stacks, 6 denizens in " +
        "total, behind the Dispossessed divider in the box.")))

  val beacon: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Beacon", Vector(front(
      "**Introduce Newcomers.** Choose 3 different suits. Draw 3 of the " +
        "first, then 2 of the second, and then 1 of the third from the box. " +
        "_Don't draw from the Dispossessed or Edifices sections._",
      "**Welcome a Stranger.** Examine the drawn cards. Keep one as a " +
        "facedown starting adviser for the next game, and add the other " +
        "five to the world deck.",
      "**Shape Foundation.** You may flip one altered foundation to its " +
        "normal side unless an active legacy is altering it.")))

  val stars: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Stars", Vector(front(
      "**Write in the Stars.** The winner draws and names a constellation " +
        "on the back of the era scorepad.",
      "**Archive Rival Sites.** **At each site not ruled by the winner,** " +
        "remove any pieces and stack cards there on the site. Stack these " +
        "sites with their cards going Hinterward, and place the Empire " +
        "divider on top of them. Slide all of this into the Atlas box on " +
        "the Recent Sites side, on top of the sites already in the Atlas " +
        "box.",
      "**Clean Up.** Return all pieces to their banks. Consolidate Empire " +
        "sites going Cradleward.",
      "**Play Again or Store.** To store: Stack the Empire going " +
        "Hinterward and slide it on top of the Empire divider in the Atlas " +
        "box. Store each player's pieces and cards. _Grand Scepter and " +
        "Reliquary are stored separately._ Return Visions and the denizen " +
        "stack from the map to the World Deck section in the box.")))

  /** The five cards in the order the Chronicle Phase uses them. */
  val all: Vector[ChronicleTaskPresentation] =
    Vector(sun, throne, world, beacon, stars)
```

Compare every string with the card data, word for word, before Step 5.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleTaskPresentationSuite distantlands.protocol.OathkeeperPresentationSuite"`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ChronicleTaskPresentation.scala \
  shared/src/test/scala/distantlands/protocol/ChronicleTaskPresentationSuite.scala
git commit -m "feat(protocol): word the five Chronicle task cards

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: `SetupCardStrip`, with the foundations built for it

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/SetupCardStripSuite.scala`

**Interfaces:**
- Produces, `private[frontend]`:
  `SetupCardStrip.Card(mark: Option[String], name: String, tag: Option[String], title: String, label: String, lines: Vector[String], plate: Option[String])`;
  `SetupCardStrip.render(heading: String, stem: String, cards: Vector[SetupCardStrip.Card]): dom.Element`;
  `FoundationStrip.ofGame(foundations: Vector[FoundationProjection]): Vector[SetupCardStrip.Card]`;
  `FoundationStrip.render(cards: Vector[SetupCardStrip.Card]): dom.Element`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'FoundationStrip\.' -- frontend src
```
Expected: `WorldBoardRenderer.scala` (`render(ofGame(...))`) and
`FoundationStripSuite.scala` (`render`, `ofGame`, `numeral`). A use of
`FoundationStrip.Card`, `title` or `label` elsewhere is updated in Step 4.

- [ ] **Step 2: Write the failing test**

Create `frontend/src/test/scala/distantlands/frontend/SetupCardStripSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}

/** A strip of setup cards under a heading (Chronicle Details design, slice
  * 2): its classes come from a stem, a card without a mark or a tag draws
  * neither, and a click reads the card's lines. */
class SetupCardStripSuite extends munit.FunSuite:
  private var opened = Vector.empty[(String, Vector[String], Option[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate) =>
        opened :+= ((title, lines, plate))
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private val unmarkedAidCard = SetupCardStrip.Card(None, "The Sun", None,
    "Aid · The Sun", "Aid, The Sun", Vector("Front step.", "Back",
      "Back step."), None)
  private val markedTaggedFoundationCard = SetupCardStrip.Card(Some("III"),
    "Public Ambitions", Some("Altered"), "Foundation III · Public Ambitions",
    "Foundation III, Public Ambitions, altered", Vector("Deal 4."),
    Some("foundation"))

  private def aidStripOfOneUnmarkedCard =
    SetupCardStrip.render("Aids", "aid", Vector(unmarkedAidCard))

  test("the strip's section, heading, list and card take their classes " +
      "from its stem"):
    val strip = aidStripOfOneUnmarkedCard
    assertEquals(strip.getAttribute("class"), "aids")
    assertEquals(strip.getAttribute("aria-label"), "Aids")
    assertEquals(one(strip, "h3.aids-title").textContent, "Aids")
    assertEquals(all(strip, "ol.aid-strip > li > button.aid-card").size, 1)
    assertEquals(one(strip, ".aid-name").textContent, "The Sun")

  test("a card without a mark or a tag draws neither; one with both draws " +
      "both"):
    assertEquals(all(aidStripOfOneUnmarkedCard, ".aid-number, .aid-mark")
      .size, 0)
    val marked = SetupCardStrip.render("Foundations", "foundation",
      Vector(markedTaggedFoundationCard))
    assertEquals(one(marked, ".foundation-number").textContent, "III")
    assertEquals(one(marked, ".foundation-mark").textContent, "Altered")

  test("a card is labelled and noted by its own words, and a click reads " +
      "its lines under its title, with its plate"):
    val card = one(aidStripOfOneUnmarkedCard, "button.aid-card")
    assertEquals(card.getAttribute("type"), "button")
    assertEquals(card.getAttribute("aria-label"), "Aid, The Sun")
    assertEquals(card.getAttribute(HoverTip.Attribute), "Aid · The Sun")
    click(card)
    assertEquals(opened, Vector(("Aid · The Sun",
      Vector("Front step.", "Back", "Back step."), None)))
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SetupCardStripSuite"`
Expected: compilation fails with "Not found: SetupCardStrip".

- [ ] **Step 4: Write the strip and build the foundations for it**

Create `frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala`:

```scala
package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{element, text}

/** A read-only strip of setup cards under a heading (Chronicle Details
  * design, slice 2): the foundations (`FoundationStrip`) and the Chronicle
  * Phase aids (`ChronicleTaskStrip`). One row a card: its mark, its name
  * and its tag, each drawn only when present. A click reads the card's
  * lines in the inspector. A strip's classes come from its stem, so the
  * foundation strip keeps the classes its styles and suites read
  * (implementation ruling R7). */
private[frontend] object SetupCardStrip:
  /** One card as a strip draws it. `mark` is the foundation's numeral;
    * `tag` is "Altered" on an altered face; `title` heads the inspector and
    * is the hover note; `label` names the control; `lines` are what a
    * click reads; `plate` names the printed card behind the words. */
  final case class Card(mark: Option[String], name: String,
      tag: Option[String], title: String, label: String,
      lines: Vector[String], plate: Option[String])

  /** The strip headed `heading`. With stem "foundation" its classes are
    * `foundations`, `foundations-title`, `foundation-strip`,
    * `foundation-card`, `foundation-number`, `foundation-name` and
    * `foundation-mark`. */
  def render(heading: String, stem: String, cards: Vector[Card])
      : dom.Element =
    val section = element("section", s"${stem}s")
    section.setAttribute("aria-label", heading)
    section.appendChild(text("h3", s"${stem}s-title", heading))
    val strip = element("ol", s"$stem-strip")
    cards.foreach { card =>
      val item = element("li", "")
      item.appendChild(face(card, stem))
      strip.appendChild(item)
    }
    section.appendChild(strip)
    section

  private def face(card: Card, stem: String): dom.Element =
    val node = ServerUiSupport.button("", s"$stem-card")
    node.`type` = "button"
    // A control is named in one phrase; its parts alone may open with a
    // numeral.
    node.setAttribute("aria-label", card.label)
    node.onclick = _ =>
      CardInspection.openText(card.title, card.lines, node, card.plate)
    // The name is cut to one line on a small map; the note keeps it whole.
    HoverTip.set(node, card.title)
    card.mark.foreach(mark =>
      node.appendChild(text("span", s"$stem-number", mark)))
    node.appendChild(text("span", s"$stem-name", card.name))
    card.tag.foreach(tag =>
      node.appendChild(text("span", s"$stem-mark", tag)))
    node
```

Replace `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala`
with:

```scala
package distantlands.frontend

import distantlands.protocol.projection.FoundationProjection
import org.scalajs.dom

/** The six foundation setup cards as a read-only strip (Foundations design,
  * slice 1), drawn by `SetupCardStrip` (Chronicle Details design, slice 2):
  * one row a card, its numeral, the name of the face in use and, on an
  * altered card, the word. A click on a card reads its face's rules in the
  * overlay.
  */
private[frontend] object FoundationStrip:
  private val Numerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** "IV" for 4; a number no card prints reads as its digits. */
  def numeral(number: Int): String =
    Numerals.lift(number - 1).getOrElse(number.toString)

  def ofGame(foundations: Vector[FoundationProjection])
      : Vector[SetupCardStrip.Card] =
    foundations.map { foundation =>
      val altered = foundation.face == "altered"
      SetupCardStrip.Card(Some(numeral(foundation.number)), foundation.name,
        Option.when(altered)("Altered"),
        title(foundation.number, foundation.name),
        label(foundation.number, foundation.name, altered),
        Vector(foundation.rulesText), Some("foundation"))
    }

  /** "Foundation I · Wide Horizons": the overlay's title and the hover
    * note. */
  def title(number: Int, name: String): String =
    s"Foundation ${numeral(number)} · $name"

  /** "Foundation I, Wide Horizons, altered": a table card's label. */
  def label(number: Int, name: String, altered: Boolean): String =
    s"Foundation ${numeral(number)}, $name" +
      (if altered then ", altered" else "")

  def render(cards: Vector[SetupCardStrip.Card]): dom.Element =
    SetupCardStrip.render("Foundations", "foundation", cards)
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SetupCardStripSuite distantlands.frontend.FoundationStripSuite distantlands.frontend.PanelPlacementSuite distantlands.frontend.ChroniclePageSuite"`
Expected: all pass, `FoundationStripSuite` unchanged.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/SetupCardStrip.scala \
  frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala \
  frontend/src/test/scala/distantlands/frontend/SetupCardStripSuite.scala
git commit -m "refactor(frontend): draw the foundation strip as a setup card strip

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: The Chronicle Phase aid strip

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ChronicleTaskStrip.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ChronicleTaskStripSuite.scala`

**Interfaces:**
- Consumes: `ChronicleTaskPresentation.all` and `lines` (Task 1);
  `SetupCardStrip.Card` and `render` (Task 2).
- Produces, `private[frontend]`:
  `ChronicleTaskStrip.cards: Vector[SetupCardStrip.Card]`;
  `ChronicleTaskStrip.render(): dom.Element`. Slice 4's overlay calls
  `render()`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'ChronicleTaskStrip|chronicle-task' -- frontend DESIGN.md
```
Expected: no hit.

- [ ] **Step 2: Write the failing test**

Create `frontend/src/test/scala/distantlands/frontend/ChronicleTaskStripSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}
import distantlands.protocol.projection.ChronicleTaskPresentation

/** The Chronicle Phase aids (Chronicle Details design, slice 2): the five
  * task cards in the foundation strip's format, unmarked, each opening its
  * lines. */
class ChronicleTaskStripSuite extends munit.FunSuite:
  private var opened = Vector.empty[(String, Vector[String], Option[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate) =>
        opened :+= ((title, lines, plate))
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  test("the strip is headed Chronicle Phase and lists the five cards in " +
      "order, with no numeral and no tag"):
    val strip = ChronicleTaskStrip.render()
    assertEquals(strip.getAttribute("aria-label"), "Chronicle Phase")
    assertEquals(one(strip, ".chronicle-tasks-title").textContent,
      "Chronicle Phase")
    assertEquals(all(strip, ".chronicle-task-name").map(_.textContent),
      Vector("The Sun", "The Throne", "The World", "The Beacon",
        "The Stars"))
    assertEquals(all(strip, ".chronicle-task-number, .chronicle-task-mark")
      .size, 0)

  test("a card is labelled and noted as a Chronicle task (R8)"):
    val sun = all(ChronicleTaskStrip.render(), "button.chronicle-task-card")
      .head
    assertEquals(sun.getAttribute("aria-label"), "Chronicle task, The Sun")
    assertEquals(sun.getAttribute(HoverTip.Attribute),
      "Chronicle task · The Sun")

  test("a click on the Sun reads its front, a Back line and its back; a " +
      "click on the World reads its one face; neither has a plate"):
    val cards = all(ChronicleTaskStrip.render(), "button.chronicle-task-card")
    click(cards(0))
    click(cards(2))
    val sun = ChronicleTaskPresentation.sun
    assertEquals(opened, Vector(
      ("Chronicle task · The Sun", sun.faces(0).lines ++ Vector("Back") ++
        sun.faces(1).lines, None),
      ("Chronicle task · The World",
        ChronicleTaskPresentation.world.faces.head.lines, None)))
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleTaskStripSuite"`
Expected: compilation fails with "Not found: ChronicleTaskStrip".

- [ ] **Step 4: Write the aid strip**

Create `frontend/src/main/scala/distantlands/frontend/ChronicleTaskStrip.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.projection.ChronicleTaskPresentation
import org.scalajs.dom

/** The Chronicle Phase aids: the five Chronicle task cards as a setup card
  * strip, headed "Chronicle Phase" (Chronicle Details design, slice 2).
  * A task card has no numeral and no tag; a click reads its lines, a
  * two-sided card's front, a "Back" line and its back. */
private[frontend] object ChronicleTaskStrip:
  /** "Chronicle task · The Sun": the inspector's title and the hover note
    * (implementation ruling R8). */
  def title(task: ChronicleTaskPresentation): String =
    s"Chronicle task · ${task.title}"

  val cards: Vector[SetupCardStrip.Card] =
    ChronicleTaskPresentation.all.map(task => SetupCardStrip.Card(None,
      task.title, None, title(task), s"Chronicle task, ${task.title}",
      task.lines, None))

  def render(): dom.Element =
    SetupCardStrip.render("Chronicle Phase", "chronicle-task", cards)
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChronicleTaskStripSuite distantlands.frontend.SetupCardStripSuite"`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChronicleTaskStrip.scala \
  frontend/src/test/scala/distantlands/frontend/ChronicleTaskStripSuite.scala
git commit -m "feat(frontend): build the Chronicle Phase aid strip

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Docs and the full gate

**Files:**
- Modify: `DESIGN.md` ("Foundation Strip")
- Modify: `docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md` (status line)

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'FoundationStrip|Slice 1 \(projection\) has landed' -- DESIGN.md docs ':!docs/superpowers/plans'
```
Expected: `DESIGN.md:961` and the spec's status line.

- [ ] **Step 2: Update the docs**

- `DESIGN.md`, "Foundation Strip": replace "read-only (`FoundationStrip`)."
  with "read-only (`FoundationStrip`, drawn by `SetupCardStrip`, which the
  Chronicle Phase aids share)."
- Spec status line: `**Status:** designed 2026-11-18. Slices 1
  (projection) and 2 (aids and strip) have landed.`

- [ ] **Step 3: Run the full gate**

Run, the long ones in the background:
`./sbtw compile`, `./sbtw test`, `./sbtw frontend/test`,
`./sbtw frontend/fullOptJS`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add DESIGN.md docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md
git commit -m "docs(chronicle): record Chronicle Details slice 2

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

- **Spec coverage.** `ChronicleTaskPresentation` in `protocol/projection`
  beside `OathkeeperPresentation`, five cards in Chronicle order, a title
  and faces, a face's label and lines, the Sun and the Throne two-sided,
  transcribed from the local card data: Task 1. `SetupCardStrip` with a
  heading and cards, each with an optional mark, a name, an optional tag,
  an inspector title and lines: Task 2. `FoundationStrip.ofGame` builds
  the foundation cards: Task 2. The aid cards, a two-sided card's lines
  being its front's, "Back", then its back's: Tasks 1 and 3. A test pins
  the five cards, their order and which have two faces: Task 1. The
  existing foundation strip tests pass unchanged: Task 2, Step 5.
- **Placeholder scan.** Every code step shows its code. The task text is
  given in full and checked against the card data in Task 1, Step 4.
- **Type consistency.** `ChronicleTaskFace`, `ChronicleTaskPresentation`
  (`title`, `faces`, `lines`, `all`, `sun`, `world`),
  `SetupCardStrip.Card` (seven fields in one order), `SetupCardStrip.render(heading, stem, cards)`,
  `FoundationStrip.ofGame`, `title(number, name)`,
  `label(number, name, altered)` and `ChronicleTaskStrip.cards` and
  `render()` carry the same names and types in every task.
- **Ruling numbers.** The code cites R6, R7 and R8 as recorded.
