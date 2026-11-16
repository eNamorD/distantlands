# Catalog Batch 13, Slice 5: The Doors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Amber Doors and Lost Doors (E05) resolve at Setup and When
Explored for the player who placed their pawn at the site, and the card
leaves `catalog/holding/`.

**Architecture:** Both faces follow Great Forge's shape
(`GreatForgeRules.scala`): a sealed base that finds the placer through
`EdificeSetupSupport.pawnPlacement`, then offers the face's steps to
Setup's ordered pawn window (`OfferResolution`, slice 4) and appends them at
an Explore (`Transform`). The Mouth's activation moves out of `TheMouth`
into a shared `LegacyActivation`, which Amber Doors calls with the legacy
coming from the player's hand. Both faces draw through `LegacyDraws` and
discard to the bottom of the legacy deck.

**Why the decision ids name the placer at Setup.** Setup is one walk, and
several players may place their pawn at the Doors' site, so each answers
the same face's question in that walk. `PowerAnswers.one` reads the first
answer to an id, so a fixed id would read the first placer's answer for the
second. At Setup the ids are `setup.<face>.<step>.<player>`, which Setup's
procedure log narrates by its `setup.` prefix, as it narrates The Standard
Bearer's and Wealthy's. An Explore has one explorer, so there the ids are
fixed (`power.<face>.<step>`) and each face lists them in
`narratedDecisions`, which matches ids exactly.

**Why the steps are sibling `Branch`es.** A parked walk selects every
`Branch` on the path to the park again from the resumed state (the
"branch reselects on resume" lesson). Each question sits in its own
`Branch`, after the `Branch` that drew, so its selection reads only the
answers and state that nothing has changed since the park: the hand or the
lineage the question lists. The draw's `Branch` reads the deck, but it is
before the cursor and is never selected again.

**Changes made while executing.** Amber Doors' discards reach the bottom
of the deck before its draw, so a short deck draws them back (rulings 2 and
12). The plan's "deck of one" and "empty deck" tests held legacies, so they
drew those back; they now start with none, and a new test pins the
redraw. `DoorsPowerOrderSuite`'s fixture seats three players, so its deck
gained Beloved and Pathfinder for p3's keep. Review A found no bug; its
gap, two players at Lost Doors in one Setup walk, is a new test there,
which fails when the Setup ids drop the placer. `PhasePowerHarness.awaits`
reads the park with its own powers, so the Explore test proves the park by
answering it. The ROADMAP keeps no note that the Doors landed.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
rulings 2, 4, 5 and 10 to 13, and section "Slice 5: the Doors".

## Global Constraints

- "If you place your pawn here" is the placer in `SetupPawnPlaced` and the
  explorer in `WhenExplored` (ruling 11; `EdificeSetupSupport.pawnPlacement`).
- Amber Doors is "you may" (ruling 12). Its activation is The Mouth's: it
  sets the legacy's foundation, and an Imperial's Iron Hand is discarded at
  once. It needs no goal and no cap: the player holds only the drawn
  legacies, at most one active.
- Lost Doors never discards an active legacy (ruling 13). Matchmaker plays
  no part in either face (rulings 10 and 13).
- Every discard goes to the bottom of the legacy deck (ruling 2), through
  `PositionedLocation.legacyDeckBottom`. Discarding an active legacy flips
  no foundation back, as `ResolveLegacies.cap` does not.
- A power's resolution offer must stand while its own steps run (slice 4).
  Both faces read only the edifice's site and the pawn answer to apply, and
  their steps change neither.
- The Mouth's lines, decision ids and behaviour are unchanged: Task 1 is a
  refactor that `TheMouthSuite` guards.
- Lines (spec, "Lines", in the Game Log's "Source: text" form):
  - "Amber Doors: p2 discarded their legacies." (only when p2 held one)
  - "Amber Doors: p2 activated Seer.", or "Amber Doors: p2 activated Iron
    Hand." with Iron Hand's own "p2 discarded Iron Hand" when an Imperial
    activates it
  - the activated legacy's "Wealthy set Foundation II to …" line
  - "Lost Doors: p2 drew a legacy.", then "Lost Doors: p2 discarded a
    dormant legacy."
  - "The legacy deck was empty." under either face
  - a declined Amber Doors writes no line.
- Hidden information: the drawn legacies are in the drawer's hand. The
  dormant ones go to the lineage facedown and the discards to the deck,
  so no line names a dormant or discarded legacy.
- No production Scala file exceeds 800 lines (`check-architecture.py`).
- Docs, comments and commit messages are written in plain English.

## Files

- Create: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActivation.scala`.
  The activation and its two lines, shared by The Mouth and Amber Doors.
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/TheMouth.scala`.
  Calls `LegacyActivation`.
- Create: `src/main/scala/distantlands/gameplay/powers/setup/AmberDoorsRules.scala`.
  `AmberDoorsCard`, moved from holding, the `DoorsRule` base, `LostDoors`
  and `AmberDoors`.
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`.
  `AmberDoorsCard` leaves; Stone Baths stays for slice 6.
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`.
  Registers both faces beside the other edifices.
- Modify: `src/main/scala/distantlands/gameplay/powers/LegacyDraws.scala`.
  Doc comment only.
- Modify: `src/test/resources/powers/unimplemented.txt`. Drops both E05 lines.
- Create: `src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala`.
  A test helper: a power's steps as Setup's pawn window offers them.
- Create: `src/test/scala/distantlands/gameplay/powers/setup/LostDoorsSuite.scala`.
- Create: `src/test/scala/distantlands/gameplay/powers/setup/AmberDoorsSuite.scala`.
- Create: `src/test/scala/distantlands/gameplay/setup/DoorsPowerOrderSuite.scala`.
  The spec's power-order cases with real cards. The spec names
  `OrderedWindowSuite` for them; that suite holds slice 4's stub powers, so
  the real cards get their own suite beside `SetupPowerOrderSuite`.
- Modify docs: `docs/rules/ambiguities.md`, `docs/rules/rules-reference.md`,
  `docs/rules/implementation-traceability.md`, `docs/ROADMAP.md` and the
  spec.

---

### Task 1: Extract The Mouth's activation

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActivation.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/TheMouth.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/TheMouthSuite.scala` (unchanged; it is the guard)

**Interfaces:**
- Produces:
  - `LegacyActivation.notes: NoteSet` (unowned; a power that writes its
    keys includes it)
  - `LegacyActivation.altered: Map[FoundationNumber, NoteKey]`
  - `LegacyActivation.operations(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId, lineage: LineageId, legacy: LegacyId, from: Location): Vector[CoreOperation]`
  - `LegacyActivation.discardsIronHand(ready: ReadyGame, player: PlayerId, legacy: LegacyId): Boolean`
  - `LegacyActivation.alterationNote(states: NoteStates, activated: Option[LegacyId]): Option[LogNote]`
  - `LegacyActivation.ironHandNote(states: NoteStates, player: PlayerId, activated: Option[LegacyId]): Option[LogNote]`
  - `LegacyActivation.holdsIronHand(ready: ReadyGame, lineage: LineageId): Boolean`

- [ ] **Step 1: Run the guard before the change**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.TheMouthSuite"`
Expected: PASS. Also run `grep -rn "TheMouth.altered" src`; expected: no
match, so the keys can move.

- [ ] **Step 2: Create `LegacyActivation.scala`**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.chronicle.ChronicleTasks
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}
import distantlands.gameplay.powers.LegacyDraws
import distantlands.model._

/** Activating a legacy the way The Mouth does (Legacy in-play powers
  * rulings 23 and 24; R25), shared with Amber Doors (catalog batch 13,
  * ruling 12). The legacy goes faceup into the holder's lineage: a flip
  * when it is already there, dormant, else a move from where it waits (the
  * Public Ambitions row, or the holder's hand after a draw). The same batch
  * alters the foundation the legacy names, unless that foundation already
  * lists it, and discards an Iron Hand its Imperial holder activated to the
  * bottom of the legacy deck, whose standing rule says to.
  *
  * Callers check their own conditions first (The Mouth's goal) and handle
  * what the activation leaves (The Mouth's cap). The two lines are read
  * after the activation's own step, which nothing parks after.
  */
object LegacyActivation:
  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("LegacyActivation")
  /** "{Beloved} set Foundation {IV} to {Teeming World}", one key per
    * foundation, as the Sun's are. */
  val altered: Map[FoundationNumber, NoteKey] = FoundationNumber.all.map(
    number => number -> notes.key(
      s"altered-foundation-${number.toString.toLowerCase}", Vector(
      NotePart.Arg(0), NotePart.Text(s" set Foundation $number to " +
        FoundationCards.face(number, FoundationFace.Altered).name)))).toMap

  /** `legacy` faceup in `lineage`, from `from`, the foundation it names
    * altered, and an Imperial's Iron Hand discarded (R25). */
  def operations(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, lineage: LineageId, legacy: LegacyId,
      from: Location): Vector[CoreOperation] =
    val into = Location.Legacies(lineage)
    val activate: CoreOperation =
      if from == into then Flip(legacy, into, Orientation.FaceUp)
      else Move(Piece.Card(legacy), PositionedLocation(from),
        PositionedLocation(into),
        resultingOrientation = Some(Orientation.FaceUp))
    val alteration = catalog.legacy(legacy).flatMap(_.alters).filterNot(
      number => ready.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)))
      .map(number => FoundationAltered(number, legacy))
    val ironHand = Option.when(discardsIronHand(ready, player, legacy))(
      Move(Piece.Card(legacy), PositionedLocation(into),
        PositionedLocation.legacyDeckBottom))
    Vector[CoreOperation](activate) ++ alteration.toVector ++
      ironHand.toVector

  /** Whether activating `legacy` discards it at once: Iron Hand, activated
    * by an Imperial (R25). */
  def discardsIronHand(ready: ReadyGame, player: PlayerId,
      legacy: LegacyId): Boolean =
    legacy == IronHandCard.id && Imperials.isImperial(ready, player)

  /** The foundation the activation of `activated` altered. */
  def alterationNote(states: NoteStates, activated: Option[LegacyId])
      : Option[LogNote] = for
    (before, after) <- states.previous
    legacy <- activated
    number <- FoundationNumber.all.find(number =>
      after.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)) &&
        !before.game.campaign.foundations.get(number)
          .exists(_.alterationSources(legacy)))
  yield altered(number).sourceless(NoteArg.Card(legacy))

  /** "p1 discarded Iron Hand", when the activation of `activated` sent it
    * back from wherever it waited. */
  def ironHandNote(states: NoteStates, player: PlayerId,
      activated: Option[LegacyId]): Option[LogNote] = for
    (before, after) <- states.previous
    if activated.contains(IronHandCard.id)
    lineage <- ChronicleTasks.lineageOf(after, player)
    if !holdsIronHand(after, lineage) && (holdsIronHand(before, lineage) ||
      before.game.current.commonCards.publicLegacies
        .contains(IronHandCard.id) ||
      LegacyDraws.hand(before, player).contains(IronHandCard.id))
  yield IronHand.discardedNow.sourceless(NoteArg.Player(player))

  def holdsIronHand(ready: ReadyGame, lineage: LineageId): Boolean =
    ready.game.campaign.lineages.get(lineage)
      .exists(_.legacies.exists(_.id == IronHandCard.id))
```

- [ ] **Step 3: Make The Mouth call it**

In `TheMouth.scala`:

1. Delete the `altered` map and its doc comment. After
   `private val notes = NoteSet(id)` add
   `notes.include(LegacyActivation.notes)`.
2. In `build`, replace the two notes after `activationNote`:

```scala
    Note(id, states => activationNote(states, player)),
    Note(id, states =>
      LegacyActivation.alterationNote(states, chosen(states.answered))),
    Note(IronHand.id, states => LegacyActivation.ironHandNote(states,
      player, chosen(states.answered))),
```

3. Replace the body of `activation`'s `yield` with:

```scala
  yield
    val from =
      if ready.game.current.commonCards.publicLegacies.contains(legacy) then
        Location.PublicLegacies
      else Location.Legacies(lineage)
    LegacyActivation.operations(catalog, ready, player, lineage, legacy, from)
```

4. In `activationNote`, replace `!holdsIronHand(after, _)` with
   `!LegacyActivation.holdsIronHand(after, _)`.
5. Delete `alterationNote`, `ironHandNote` and `holdsIronHand` from
   `TheMouth`, and the imports the compiler then reports unused
   (`FoundationCards` among them). Update the class doc's "The same batch
   alters …" sentence to end "(`LegacyActivation`)".

- [ ] **Step 4: Run the guard after the change**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.application.gamelog.*"`
Expected: PASS, with no golden or note-key change. The keys keep their
names; only their owning set moves, and each is still written under
The Mouth's id.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies
git commit -m "refactor(legacies): share The Mouth's activation as LegacyActivation"
```

---

### Task 2: Lost Doors

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/setup/AmberDoorsRules.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:99-100`
- Modify: `src/main/scala/distantlands/gameplay/powers/LegacyDraws.scala` (doc)
- Modify: `src/test/resources/powers/unimplemented.txt`
- Create: `src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/LostDoorsSuite.scala`

**Interfaces:**
- Consumes: `LegacyDraws.top`, `LegacyDraws.draw`, `LegacyDraws.hand`,
  `LegacyDraws.empty`; `EdificeSetupSupport.pawnPlacement` and `.option`;
  `OfferResolution`, `ResolutionOffer`.
- Produces:
  - `AmberDoorsCard` in `distantlands.gameplay.powers.setup`
  - `sealed abstract class DoorsRule extends ContributingPower` with
    `edifice: EdificeId` and `protected def steps(catalog: ExecutableCatalog, actor: PlayerId, placer: Option[PlayerId]): Vector[Operation]`
  - `LostDoors.discardDecisionId(placer: Option[PlayerId]): String`
  - `LostDoors.drew: NoteKey` ("{0} drew a legacy."), `LostDoors.discarded: NoteKey` ("{0} discarded a dormant legacy.")
  - test helper `PawnWindowOffers.steps(catalog: ExecutableCatalog, power: ContributingPower, ready: ReadyGame, placer: PlayerId, site: SiteId): Vector[Operation]`

- [ ] **Step 1: Write the test helper**

`src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{ContributingPower,
  OfferResolution, PowerCtx}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Unwrap.present

/** A power's steps as Setup's pawn window offers them, so a suite can walk
  * one power's resolution without the whole Setup walk. */
object PawnWindowOffers:
  /** `power`'s offered steps in `ready`, once `placer` has placed their
    * pawn at `site`. Fails the test when the power offers nothing. */
  def steps(catalog: ExecutableCatalog, power: ContributingPower,
      ready: ReadyGame, placer: PlayerId, site: SiteId)(
      using munit.Location): Vector[Operation] =
    val placed = Vector(Answered(SetupProcedure.pawnDecisionId(placer),
      ChooseOneAnswer(DecisionOptionRef.Site(site)), placer))
    val ctx = PowerCtx(catalog, ready, placer, power.source,
      PowerWindow.SetupPawnPlaced, Vector.empty,
      OrderedWindow(placer, Vector.empty), answered = placed)
    present(power.contributions(PowerWindow.SetupPawnPlaced)
      .collectFirst { case resolution: OfferResolution =>
        resolution.offer(ctx) }.flatten, s"${power.id.value}'s offer").steps
```

- [ ] **Step 2: Write the failing Lost Doors suite**

`src/test/scala/distantlands/gameplay/powers/setup/LostDoorsSuite.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite,
  DeepWoodsSite}
import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.{LegacyDraws, NoteText,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.legacies.{GoldenKeyCard, RuthlessCard}
import distantlands.gameplay.setup.{PawnWindowOffers, SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{WalkerCompleted, WalkerDice,
  WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, PhasePowerHarness, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Lost Doors (E05 ruined), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, draw a dormant legacy, then discard one you hold" (catalog
  * batch 13, rulings 2, 11 and 13). */
class LostDoorsSuite extends munit.FunSuite:
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)
  private val (rival, arbiter, ruthless, key) = (RivalCard.id,
    ArbiterCard.id, RuthlessCard.id, GoldenKeyCard.id)

  /** Lost Doors at Ancient City in p1's game. */
  private def doorsAtAncientCity: Table =
    Table.start.edifice(AmberDoorsCard.id, EdificeSide.Ruined,
      at = AncientCitySite)

  /** Lost Doors' Setup steps for p1, who placed their pawn at Ancient
    * City, walked with `answers`. */
  private def walkedAtSetup(table: Table,
      answers: Answers = PartialFunction.empty): SetupWalkDriver.SetupWalked =
    val ready = table.ready
    SetupWalkDriver.driveWithEvents(ready, Sequence(PawnWindowOffers.steps(
      catalog, LostDoors, ready, p1, AncientCitySite.id)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private def legaciesOf(ready: ReadyGame): Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(p1).get.lineage)
      .legacies

  private def deckOf(ready: ReadyGame): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(LostDoors.id, LostDoors.noteKeys, events).map(_.text)

  private val setupDiscard = LostDoors.discardDecisionId(Some(p1))

  test("it draws the top legacy dormant, then asks which dormant legacy " +
      "to discard, never an active one"):
    val run = walkedAtSetup(doorsAtAncientCity
      .legacy(p1, RuthlessCard, active = false).legacy(p1, GoldenKeyCard)
      .legacyDeck(RivalCard, ArbiterCard), {
        case park if park.decisionId == setupDiscard =>
          ChooseOneAnswer(DecisionOptionRef.Legacy(rival))
      })
    val asked = run.asked.filter(_.decisionId == setupDiscard)
    assertEquals(asked.size, 1)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(ruthless, rival)
          .map(DecisionOptionRef.Legacy(_)))
      case other => fail(s"expected a choose-one, got $other")
    assertEquals(legaciesOf(run.ready), Vector(
      LegacyState(ruthless, active = false), LegacyState(key, active = true)))
    assertEquals(deckOf(run.ready), Vector(arbiter, rival))
    assertEquals(LegacyDraws.hand(run.ready, p1), Vector.empty)

  test("with no other dormant legacy, the drawn one is discarded unasked"):
    val run = walkedAtSetup(doorsAtAncientCity.legacy(p1, GoldenKeyCard)
      .legacyDeck(RivalCard, ArbiterCard))
    assert(!run.asked.exists(_.decisionId == setupDiscard), run.asked)
    assertEquals(legaciesOf(run.ready),
      Vector(LegacyState(key, active = true)))
    assertEquals(deckOf(run.ready), Vector(arbiter, rival))

  test("an empty deck draws nothing and says so, and the only dormant " +
      "legacy is still discarded"):
    val run = walkedAtSetup(doorsAtAncientCity
      .legacy(p1, RuthlessCard, active = false).legacyDeck())
    assertEquals(legaciesOf(run.ready), Vector.empty)
    assertEquals(deckOf(run.ready), Vector(ruthless))
    assertEquals(lines(run.events), Vector("The legacy deck was empty.",
      "p1 discarded a dormant legacy."))

  test("an empty deck and no dormant legacy do nothing but say so"):
    val run = walkedAtSetup(doorsAtAncientCity.legacy(p1, GoldenKeyCard)
      .legacyDeck())
    assertEquals(legaciesOf(run.ready),
      Vector(LegacyState(key, active = true)))
    assertEquals(lines(run.events), Vector("The legacy deck was empty."))

  test("its lines say a legacy was drawn and one discarded, never which"):
    val run = walkedAtSetup(doorsAtAncientCity
      .legacy(p1, RuthlessCard, active = false).legacyDeck(RivalCard))
    assertEquals(lines(run.events), Vector("p1 drew a legacy.",
      "p1 discarded a dormant legacy."))

  // ---- When explored ----

  /** Every defense die two shields, so an Explore succeeds on its first
    * roll, and every shuffle reverses its pile (as `WhenExploredSuite`). */
  private val shieldsAndReversedShuffles: WalkerDice = new WalkerDice:
    def roll(kind: DiceKind, count: Int)
        : Either[OathViolation, Vector[DieFace]] =
      Right(Vector.fill(count)(DefenseDieFace.TwoShields))
    override def shuffle(count: Int): Either[OathViolation, Vector[Int]] =
      Right((0 until count).reverse.toVector)

  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = shieldsAndReversedShuffles)

  test("when explored, the explorer draws, then discards a dormant legacy " +
      "under the Explore's fixed id"):
    val table = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
      .storedInAtlas(DeepWoodsSite, AmberDoorsCard.id)
      .legacy(p1, RuthlessCard, active = false)
      .legacyDeck(RivalCard, ArbiterCard)
    val rolled = accepted(rules.startWalker(Ready(table.ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    val discard = LostDoors.discardDecisionId(None)
    assert(PhasePowerHarness.awaits(placed, discard), placed.events)
    val done = accepted(rules.resolveWalker(placed.state, p1, discard,
      ChooseOneAnswer(DecisionOptionRef.Legacy(rival))))
    assert(done.events.contains(WalkerCompleted(ActionRef.Explore)),
      done.events)
    val ready = PhasePowerHarness.after(done)
    assertEquals(legaciesOf(ready), Vector(
      LegacyState(ruthless, active = false)))
    assertEquals(deckOf(ready), Vector(arbiter, rival))
    assertEquals(lines(rolled.events ++ placed.events ++ done.events),
      Vector("p1 drew a legacy.", "p1 discarded a dormant legacy."))
```

- [ ] **Step 3: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.LostDoorsSuite"`
Expected: compile failure, `Not found: LostDoors` (and `AmberDoorsCard`
from the new package).

- [ ] **Step 4: Move the card and write Lost Doors**

Delete `AmberDoorsCard` from `HoldingEdifices.scala` (Stone Baths stays).
Create `AmberDoorsRules.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.catalog.{Edifice, EdificeFace, ExecutableCatalog,
  Locked, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleTasks
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution, PowerCtx, Transform}
import distantlands.gameplay.powers.LegacyDraws
import distantlands.model._

object AmberDoorsCard extends Edifice(EdificeId("E05"), Suit.Discord):
  object intact extends EdificeFace("Amber Doors") with Locked:
    val power = PrintedPower(PowerId("edifice.e05.intact"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, you " +
        "may discard all of your legacies (even dormant) to draw 2 " +
        "dormant legacies and activate one.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Doors"):
    val power = PrintedPower(PowerId("edifice.e05.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, " +
        "draw a dormant legacy, then discard one you hold.")
    val powers: Vector[PrintedPower] = Vector(power)

/** E05, both faces (catalog batch 13, rulings 11 to 13), shaped as
  * `ForgeRule` is: "If you place your pawn here" is the placer at Setup
  * and the explorer at an Explore (`EdificeSetupSupport.pawnPlacement`).
  *
  * Setup is one walk in which several players may place at this site, so
  * there a face's decision ids name the placer, under `setup.`, which
  * Setup's log narrates. An Explore has one explorer, so there the ids are
  * fixed and each face narrates them itself. Each question sits in its own
  * `Branch` after the draw's, so a walk resumed at it selects the same
  * shape: it lists a hand or a lineage that nothing changed since the park.
  */
sealed abstract class DoorsRule extends ContributingPower:
  final val edifice: EdificeId = AmberDoorsCard.id
  protected def side: EdificeSide
  /** The face's name in its decision ids. */
  protected def slug: String

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  protected final val card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** The id of this face's `step` question: at Setup, for `placer`; at an
    * Explore, `None`. */
  protected final def decision(step: String,
      placer: Option[PlayerId]): String = placer match
    case Some(player) => s"setup.$slug.$step.${player.value}"
    case None => s"power.$slug.$step"

  private def at(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    for
      edificeSite <- ctx.state.game.current.edificeSite(edifice, side)
      placement <- EdificeSetupSupport.pawnPlacement(ctx)
      if placement._2 == edificeSite
    yield placement

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The face's steps for `actor`, asking under `placer`'s ids. */
  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation]

  /** `actor`'s lineage in `ready`. */
  protected final def lineageOf(ready: ReadyGame, actor: PlayerId)
      : Either[OathViolation, LineageId] =
    ChronicleTasks.lineageOf(ready, actor).toRight(
      OathViolation.InvalidEventOrder(s"${actor.value} has no lineage"))

  /** The legacies `actor`'s lineage holds in `ready`, in lineage order. */
  protected final def held(ready: ReadyGame, actor: PlayerId)
      : Vector[LegacyState] =
    ChronicleTasks.lineageOf(ready, actor).toVector.flatMap(lineage =>
      ready.game.campaign.lineages.get(lineage).toVector.flatMap(_.legacies))

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (catalog batch 13, ruling 4). An Explore folds them in.
    */
  final def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, _) => ResolutionOffer(
        EdificeSetupSupport.option(edifice),
        steps(ctx.catalog, actor, Some(actor)))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, _) =>
        ops ++ steps(ctx.catalog, actor, None)))))

/** Lost Doors (E05 ruined): the player draws 1 legacy, dormant, then
  * discards one dormant legacy they hold, the new one included, to the
  * bottom of the legacy deck (ruling 13). Active legacies are never
  * offered. With one dormant legacy it is discarded unasked; with none
  * nothing is discarded. An empty deck draws nothing and says so, and the
  * discard still follows. Matchmaker plays no part. Lines: "p2 drew a
  * legacy." and "p2 discarded a dormant legacy.", never naming either.
  */
case object LostDoors extends DoorsRule:
  val id: PowerId = AmberDoorsCard.ruined.power.id
  protected def side: EdificeSide = EdificeSide.Ruined
  protected def slug: String = "lost-doors"

  def discardDecisionId(placer: Option[PlayerId]): String =
    decision("discard", placer)

  private val notes = NoteSet(id)
  /** "p2 drew a legacy." */
  val drew: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" drew a legacy.")))
  /** "p2 discarded a dormant legacy." */
  val discarded: NoteKey = notes.key("discarded", Vector(NotePart.Arg(0),
    NotePart.Text(" discarded a dormant legacy.")))
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** Its discard line tells the answer, and names no legacy. */
  override def narratedDecisions: Set[String] = Set(discardDecisionId(None))

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    Vector(draw(actor), discard(actor, discardDecisionId(placer)))

  /** The top legacy, through the hand, onto the lineage, dormant. */
  private def draw(actor: PlayerId): Operation = Branch((live, _) =>
    LegacyDraws.top(live, 1) match
      case Vector() => Vector(Note(id, _ => Some(LegacyDraws.empty(card))))
      case taken => Vector(LegacyDraws.draw(actor, taken),
        BuildOps((ready, _) => lineageOf(ready, actor).map(lineage =>
          taken.map(legacy => Move(Piece.Card(legacy),
            PositionedLocation(Location.Hand(actor)),
            PositionedLocation(Location.Legacies(lineage)))))),
          required = true),
        Note(id, _ => Some(drew(card, NoteArg.Player(actor))))))

  private def discard(actor: PlayerId, decisionId: String): Operation =
    Branch((live, _) =>
      held(live, actor).collect { case LegacyState(legacy, false) =>
        legacy } match
        case Vector() => Vector.empty
        case Vector(only) => discardOf(actor, _ => Some(only))
        case dormant => Decide(decisionId, actor, DecisionQuery.ChooseOne(
          dormant.map(legacy =>
            DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
          heading = Some("Lost Doors: discard a dormant legacy"),
          confirmVerb = Some("Discard"))) +:
          discardOf(actor, pending => PowerAnswers.one(pending, decisionId)
            .collect { case DecisionOptionRef.Legacy(legacy) => legacy }))

  /** The legacy `chosen` names, from the lineage to the deck's bottom, and
    * the line. */
  private def discardOf(actor: PlayerId,
      chosen: PendingTree => Option[LegacyId]): Vector[Operation] = Vector(
    BuildOps((ready, pending) => for
      legacy <- chosen(pending).toRight(PowerAnswers.missing(
        "a Lost Doors discard"))
      lineage <- lineageOf(ready, actor)
    yield Vector(Move(Piece.Card(legacy),
      PositionedLocation(Location.Legacies(lineage)),
      PositionedLocation.legacyDeckBottom)), required = true),
    Note(id, _ => Some(discarded(card, NoteArg.Player(actor)))))
```

Add `PowerAnswers` to the `distantlands.gameplay.powers` import. In
`WalkerPowerCatalog.scala`, add `LostDoors` after `EmptyGrounds` in the
edifice list (line 99-100), so the edifice powers stay before the Setup
legacies in the power-order question. In `unimplemented.txt`, delete
`edifice.e05.ruined`. In `LegacyDraws`'s doc, replace "Ancient Writ's 3,
and later the Doors'" with "Ancient Writ's 3, Amber Doors' 2 and Lost
Doors' 1".

- [ ] **Step 5: Run the suite and the catalog checks**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.* distantlands.gameplay.cards.CardCatalogSuite distantlands.gameplay.powers.UnimplementedPowersSuite"`
Expected: PASS. If the Explore test finds the walk parked elsewhere, read
`WhenExploredSuite` for how an Explore window parks before changing the
test.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala src/test/scala src/test/resources
git commit -m "feat(edifices): Lost Doors draws a legacy and discards a dormant one"
```

---

### Checkpoint review A (after Task 2)

Dispatch one reviewer (no higher model or effort than the main agent) on
`git diff 0.2.0-alpha...HEAD`, briefed with this plan's Global
Constraints and the two "Why" paragraphs. Ask it to look for a walk
resumed at the Lost Doors question that selects a different shape, an
answer read by the wrong player at Setup, an active legacy that can be
discarded, and any change to The Mouth's behaviour or lines. Fix what it
confirms before Task 3.

---

### Task 3: Amber Doors

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/AmberDoorsRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/AmberDoorsSuite.scala`

**Interfaces:**
- Consumes: everything Task 1 produces; `DoorsRule`; `PawnWindowOffers.steps`.
- Produces:
  - `AmberDoors.useDecisionId(placer: Option[PlayerId]): String`
  - `AmberDoors.activateDecisionId(placer: Option[PlayerId]): String`
  - `AmberDoors.keep`, `AmberDoors.discard: DecisionOptionRef.Button`
  - `AmberDoors.Drawn: Int` (2)

- [ ] **Step 1: Write the failing Amber Doors suite**

`src/test/scala/distantlands/gameplay/powers/setup/AmberDoorsSuite.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.catalog.FoundationCards
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard,
  RivalCard}
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.{LegacyDraws, NoteText,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.legacies.{GoldenKeyCard, RuthlessCard}
import distantlands.gameplay.setup.{PawnWindowOffers, SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.WealthyCard
import distantlands.gameplay.walker.{LogNoted, WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Table}
import distantlands.testkit.Table.p1

/** Amber Doors (E05 intact), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, you may discard all of your legacies (even dormant) to draw
  * 2 dormant legacies and activate one" (catalog batch 13, rulings 2, 11
  * and 12). */
class AmberDoorsSuite extends munit.FunSuite:
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)
  private val (rival, arbiter, ruthless, key, wealthy) = (RivalCard.id,
    ArbiterCard.id, RuthlessCard.id, GoldenKeyCard.id, WealthyCard.id)
  private val use = AmberDoors.useDecisionId(Some(p1))
  private val activate = AmberDoors.activateDecisionId(Some(p1))

  /** Amber Doors at Ancient City in p1's game. p1 holds Golden Key active
    * and Ruthless dormant. */
  private def doorsWithTwoHeld: Table =
    Table.start.edifice(AmberDoorsCard.id, EdificeSide.Intact,
      at = AncientCitySite).legacy(p1, GoldenKeyCard)
      .legacy(p1, RuthlessCard, active = false)

  /** p1 discards and draws, then activates `chosen` if asked. */
  private def discarding(chosen: LegacyId): Answers = {
    case park if park.decisionId == use => ChooseOneAnswer(AmberDoors.discard)
    case park if park.decisionId == activate =>
      ChooseOneAnswer(DecisionOptionRef.Legacy(chosen))
  }

  /** Amber Doors' Setup steps for p1, who placed their pawn at Ancient
    * City, walked with `answers`. */
  private def walkedAtSetup(table: Table, answers: Answers)
      : SetupWalkDriver.SetupWalked =
    val ready = table.ready
    SetupWalkDriver.driveWithEvents(ready, Sequence(PawnWindowOffers.steps(
      catalog, AmberDoors, ready, p1, AncientCitySite.id)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private def legaciesOf(ready: ReadyGame): Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(p1).get.lineage)
      .legacies

  private def deckOf(ready: ReadyGame): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(AmberDoors.id, AmberDoors.noteKeys, events).map(_.text)

  test("it asks first, and keeping the legacies changes nothing and " +
      "writes no line"):
    val table = doorsWithTwoHeld.legacyDeck(RivalCard, ArbiterCard)
    val run = walkedAtSetup(table, {
      case park if park.decisionId == use => ChooseOneAnswer(AmberDoors.keep)
    })
    assertEquals(run.asked.map(_.decisionId), Vector(use))
    assertEquals(legaciesOf(run.ready), legaciesOf(table.ready))
    assertEquals(deckOf(run.ready), Vector(rival, arbiter))
    assertEquals(lines(run.events), Vector.empty)

  test("discarding sends every held legacy to the deck's bottom, draws " +
      "2, and asks which to activate, in draw order"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(RivalCard,
      ArbiterCard, WealthyCard), discarding(arbiter))
    val asked = run.asked.filter(_.decisionId == activate)
    assertEquals(asked.size, 1)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(rival, arbiter)
          .map(DecisionOptionRef.Legacy(_)))
      case other => fail(s"expected a choose-one, got $other")
    assertEquals(legaciesOf(run.ready), Vector(
      LegacyState(arbiter, active = true), LegacyState(rival, active = false)))
    assertEquals(deckOf(run.ready), Vector(wealthy, key, ruthless))
    assertEquals(LegacyDraws.hand(run.ready, p1), Vector.empty)

  test("its lines say the legacies were discarded and name the activated " +
      "one, never the dormant one"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(RivalCard,
      ArbiterCard), discarding(arbiter))
    assertEquals(lines(run.events), Vector("p1 discarded their legacies.",
      s"p1 activated ${arbiter.value}."))

  test("the activated legacy sets its foundation, as The Mouth's does"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(WealthyCard,
      RivalCard), discarding(wealthy))
    assert(run.ready.game.campaign.foundations(FoundationNumber.II)
      .alterationSources(wealthy))
    val altered =
      FoundationCards.face(FoundationNumber.II, FoundationFace.Altered).name
    assert(lines(run.events).contains(
      s"${wealthy.value} set Foundation II to $altered"), lines(run.events))

  test("an Imperial's activated Iron Hand is discarded at once, in words"):
    val run = walkedAtSetup(Table.start.imperial(p1)
      .edifice(AmberDoorsCard.id, EdificeSide.Intact, at = AncientCitySite)
      .legacyDeck(IronHandCard), discarding(IronHandCard.id))
    assertEquals(legaciesOf(run.ready), Vector.empty)
    assertEquals(deckOf(run.ready), Vector(IronHandCard.id))
    assertEquals(lines(run.events), Vector("p1 activated Iron Hand."))
    assert(run.events.exists {
      case note: LogNoted => note.power == IronHand.id &&
        note.note.key == IronHand.discardedNow.name
      case _ => false
    }, run.events)

  test("a deck of one is activated without a question"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(RivalCard),
      discarding(rival))
    assert(!run.asked.exists(_.decisionId == activate), run.asked)
    assertEquals(legaciesOf(run.ready),
      Vector(LegacyState(rival, active = true)))

  test("an empty deck draws and activates nothing, and says so; the " +
      "legacies are still discarded"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(), discarding(rival))
    assertEquals(legaciesOf(run.ready), Vector.empty)
    assertEquals(deckOf(run.ready), Vector(key, ruthless))
    assertEquals(lines(run.events), Vector("p1 discarded their legacies.",
      "The legacy deck was empty."))

  test("a player holding no legacy writes no discard line"):
    val run = walkedAtSetup(Table.start.edifice(AmberDoorsCard.id,
      EdificeSide.Intact, at = AncientCitySite).legacyDeck(RivalCard,
      ArbiterCard), discarding(rival))
    assertEquals(lines(run.events), Vector(s"p1 activated ${rival.value}."))
```

`Table.start` deals no legacy to a lineage (`AncientWritSuite` relies on
that), and `LogNote.key` is the key's name, a `String`.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.AmberDoorsSuite"`
Expected: compile failure, `Not found: AmberDoors`.

- [ ] **Step 3: Write Amber Doors**

Append to `AmberDoorsRules.scala`, and add
`distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}` and
`distantlands.gameplay.powers.legacies.LegacyActivation` to its imports:

```scala
/** Amber Doors (E05 intact): "you may" (ruling 12). On yes, every legacy
  * the player holds, active and dormant, goes to the bottom of the legacy
  * deck; then they draw 2 into their hand and choose one to activate, and
  * the other joins their lineage dormant. A deck of one is activated
  * unasked; an empty deck draws and activates nothing, and says so. The
  * activation is The Mouth's (`LegacyActivation`): the legacy's foundation
  * is altered, and an Imperial's Iron Hand is discarded at once. It needs
  * no goal, and no cap: the player holds only what they drew. Matchmaker
  * plays no part (ruling 10; its printed text). A declined Doors writes no
  * line. Lines: "p2 discarded their legacies.", "p2 activated Seer.", or
  * "p2 activated Iron Hand." in words when it is discarded at once (R32 of
  * the Legacy in-play powers), then Iron Hand's own line.
  */
case object AmberDoors extends DoorsRule:
  val id: PowerId = AmberDoorsCard.intact.power.id
  protected def side: EdificeSide = EdificeSide.Intact
  protected def slug: String = "amber-doors"
  /** How many legacies the Doors draw. */
  val Drawn: Int = 2

  def useDecisionId(placer: Option[PlayerId]): String =
    decision("discard", placer)
  def activateDecisionId(placer: Option[PlayerId]): String =
    decision("activate", placer)
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val discard: DecisionOptionRef.Button = DecisionOptionRef.Button("discard")

  private val notes = NoteSet(id)
  /** "p2 discarded their legacies." */
  val discardedAll: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Arg(0), NotePart.Text(" discarded their legacies.")))
  /** "p2 activated Seer." */
  val activated: NoteKey = notes.key("activated", Vector(NotePart.Arg(0),
    NotePart.Text(" activated "), NotePart.Arg(1), NotePart.Text(".")))
  /** "p2 activated Iron Hand.", for an Iron Hand the same batch discards:
    * it never shows faceup, so a card reference would read "a Legacy". */
  val activatedIronHand: NoteKey = notes.key("activated-iron-hand", Vector(
    NotePart.Arg(0), NotePart.Text(s" activated ${IronHandCard.name}.")))
  notes.include(LegacyActivation.notes)
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The discard and activation lines tell both answers. */
  override def narratedDecisions: Set[String] =
    Set(useDecisionId(None), activateDecisionId(None))

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    val use = useDecisionId(placer)
    val activate = activateDecisionId(placer)
    def used(pending: PendingTree): Boolean =
      PowerAnswers.one(pending, use).contains(discard)
    Vector(
      Decide(use, actor, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(keep, "Keep your legacies"),
        DecisionOption.Button(discard, "Discard them and draw 2")),
        heading = Some("Amber Doors: you may discard all your legacies " +
          "to draw 2 and activate one"))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else discardAll(actor, held(live, actor).map(_.id))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else LegacyDraws.top(live, Drawn) match
          case Vector() => Vector(Note(id, _ => Some(LegacyDraws.empty(card))))
          case taken => Vector(LegacyDraws.draw(actor, taken))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else LegacyDraws.hand(live, actor) match
          case Vector() => Vector.empty
          case Vector(only) => activation(catalog, actor, Vector(only),
            _ => Some(only))
          case drawn => Decide(activate, actor, DecisionQuery.ChooseOne(
            drawn.map(legacy =>
              DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
            heading = Some("Amber Doors: activate a legacy"),
            confirmVerb = Some("Activate"))) +:
            activation(catalog, actor, drawn, answered =>
              PowerAnswers.last(answered, activate).collect {
                case DecisionOptionRef.Legacy(legacy) => legacy })))

  /** Every legacy `actor` holds, from the lineage to the deck's bottom, and
    * the line. */
  private def discardAll(actor: PlayerId, legacies: Vector[LegacyId])
      : Vector[Operation] =
    if legacies.isEmpty then Vector.empty
    else Vector(
      BuildOps((ready, _) => lineageOf(ready, actor).map(lineage =>
        legacies.map(legacy => Move(Piece.Card(legacy),
          PositionedLocation(Location.Legacies(lineage)),
          PositionedLocation.legacyDeckBottom))), required = true),
      Note(id, _ => Some(discardedAll(card, NoteArg.Player(actor)))))

  /** The legacy `chosen` names from `drawn` activated from `actor`'s hand,
    * the others onto the lineage dormant, then the lines. */
  private def activation(catalog: ExecutableCatalog, actor: PlayerId,
      drawn: Vector[LegacyId], chosen: Vector[Answered] => Option[LegacyId])
      : Vector[Operation] = Vector(
    BuildOps((ready, pending) => for
      legacy <- chosen(pending.answered).filter(drawn.contains)
        .toRight(PowerAnswers.missing("an Amber Doors activation"))
      lineage <- lineageOf(ready, actor)
    yield LegacyActivation.operations(catalog, ready, actor, lineage, legacy,
      Location.Hand(actor)) ++ drawn.filterNot(_ == legacy).map(other =>
        Move(Piece.Card(other), PositionedLocation(Location.Hand(actor)),
          PositionedLocation(Location.Legacies(lineage)))),
      required = true),
    Note(id, states => for
      (before, _) <- states.previous
      legacy <- chosen(states.answered)
    yield
      if LegacyActivation.discardsIronHand(before, actor, legacy) then
        activatedIronHand(card, NoteArg.Player(actor))
      else activated(card, NoteArg.Player(actor), NoteArg.Card(legacy))),
    Note(id, states =>
      LegacyActivation.alterationNote(states, chosen(states.answered))),
    Note(IronHand.id, states => LegacyActivation.ironHandNote(states, actor,
      chosen(states.answered))))
```

The notes read the answer from `NoteStates.answered`, a plain vector, so
the activation reads it with `PowerAnswers.last`, as The Mouth does. Register `AmberDoors` after `LostDoors` in
`WalkerPowerCatalog`, and delete `edifice.e05.intact` from
`unimplemented.txt`.

- [ ] **Step 4: Run the suites**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.* distantlands.gameplay.powers.legacies.* distantlands.gameplay.cards.CardCatalogSuite distantlands.gameplay.powers.UnimplementedPowersSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala src/test/scala src/test/resources
git commit -m "feat(edifices): Amber Doors trades every legacy for 2 draws and an activation"
```

---

### Task 4: The Doors in Setup's ordered window

**Files:**
- Test: `src/test/scala/distantlands/gameplay/setup/DoorsPowerOrderSuite.scala`

**Interfaces:**
- Consumes: `AmberDoors.useDecisionId`, `.activateDecisionId`,
  `.discard`, `.discardedAll`; `OrderedWindow.decisionId(placer, round)`;
  `LegacyKeep.decisionId(player)`; `TheStandardBearer.decisionId(player)`;
  `Wealthy.tookFromPlayers`, `Wealthy.tookFromBank`.

These tests pin rulings 4, 5 and 12 together through the whole Setup walk,
whose driver resumes the walk at every park, so they also cover a resume
inside the window after Amber Doors' steps.

- [ ] **Step 1: Write the suite**

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.legacies.{RivalCard, SteadfastCard}
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.legacies.{GoldenKeyCard, RuthlessCard}
import distantlands.gameplay.powers.setup.{AmberDoors, AmberDoorsCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{TheStandardBearer,
  TheStandardBearerCard, Wealthy, WealthyCard}
import distantlands.gameplay.walker.{ChoicePayload, LogNoted, WalkerPowers,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.accepted

/** Amber Doors in Setup's ordered pawn window (catalog batch 13, rulings
  * 4, 5 and 12). l2, a Citizen, holds Wealthy active. Amber Doors stands
  * intact at the first site in play, where the driver places every pawn.
  * The legacy deck holds, from the top, the keeps' draws (Rival and Arbiter
  * for p1, Ruthless and Golden Key for p2), then The Standard Bearer and
  * Steadfast, which Amber Doors draws for p2. p1 keeps Rival and p2
  * Ruthless; p1 declines Amber Doors, the driver's first option. */
class DoorsPowerOrderSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    WealthyCard.id -> true)
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)
  private val doors = DecisionOptionRef.Edifice(AmberDoorsCard.id)
  private val wealthy = DecisionOptionRef.Legacy(WealthyCard.id)

  private val ready: ReadyGame =
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    Table.from(started).edifice(AmberDoorsCard.id, EdificeSide.Intact,
      at = started.game.current.map.inPlay.head)
      .legacyDeck(RivalCard, ArbiterCard, RuthlessCard, GoldenKeyCard,
        TheStandardBearerCard, SteadfastCard).ready

  /** The Setup walk with p2 resolving `first` first, discarding at Amber
    * Doors and activating `activated`. */
  private def walked(first: DecisionOptionRef, activated: LegacyId)
      : SetupWalkDriver.SetupWalked =
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, powers, answers = {
        case park if park.decisionId == LegacyKeep.decisionId(p1) =>
          ChooseOneAnswer(DecisionOptionRef.Legacy(RivalCard.id))
        case park if park.decisionId == LegacyKeep.decisionId(p2) =>
          ChooseOneAnswer(DecisionOptionRef.Legacy(RuthlessCard.id))
        case park if park.decisionId == OrderedWindow.decisionId(p2, 0) =>
          ChooseOneAnswer(first)
        case park if park.decisionId == AmberDoors.useDecisionId(Some(p2)) =>
          ChooseOneAnswer(AmberDoors.discard)
        case park
            if park.decisionId == AmberDoors.activateDecisionId(Some(p2)) =>
          ChooseOneAnswer(DecisionOptionRef.Legacy(activated))
      })

  private def l2Legacies(run: SetupWalkDriver.SetupWalked)
      : Vector[LegacyState] = run.ready.game.campaign.lineages(l2).legacies

  /** Where in `events` Wealthy wrote its line, or -1. */
  private def wealthyLine(events: Vector[OathEvent]): Int =
    events.indexWhere {
      case note: LogNoted => Set(Wealthy.tookFromPlayers.name,
        Wealthy.tookFromBank.name).contains(note.note.key)
      case _ => false
    }

  /** Where in `events` Amber Doors wrote p2's discard line, or -1. */
  private def doorsDiscardLine(events: Vector[OathEvent]): Int =
    events.indexWhere {
      case note: LogNoted => note.power == AmberDoors.id &&
        note.note.key == AmberDoors.discardedAll.name &&
        note.note.args.headOption.contains(NoteArg.Player(p2))
      case _ => false
    }

  test("p2 is asked to order Amber Doors and Wealthy, the edifice first"):
    val asked = walked(doors, SteadfastCard.id).asked
      .filter(_.decisionId == OrderedWindow.decisionId(p2, 0))
    assertEquals(asked.size, 1)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(doors, wealthy))
      case other => fail(s"expected a choose-one, got $other")

  test("Amber Doors first discards Wealthy, so Wealthy does not resolve " +
      "(ruling 5)"):
    val run = walked(doors, SteadfastCard.id)
    assert(doorsDiscardLine(run.events) >= 0, run.events)
    assertEquals(wealthyLine(run.events), -1)
    assertEquals(l2Legacies(run), Vector(
      LegacyState(SteadfastCard.id, active = true),
      LegacyState(TheStandardBearerCard.id, active = false)))
    assert(run.ready.game.current.commonCards.legacyDeck
      .contains(WealthyCard.id))

  test("Wealthy chosen first resolves before Amber Doors discards it"):
    val run = walked(wealthy, SteadfastCard.id)
    val (atWealthy, atDoors) =
      (wealthyLine(run.events), doorsDiscardLine(run.events))
    assert(atWealthy >= 0 && atDoors >= 0, s"$atWealthy, $atDoors")
    assert(atWealthy < atDoors, s"$atWealthy, $atDoors")
    assertEquals(run.events.count {
      case note: LogNoted => Set(Wealthy.tookFromPlayers.name,
        Wealthy.tookFromBank.name).contains(note.note.key)
      case _ => false
    }, 1)

  test("a Standard Bearer that Amber Doors activated resolves in the same " +
      "window, picked alone (rulings 4 and 5)"):
    val run = walked(doors, TheStandardBearerCard.id)
    assert(run.asked.exists(_.decisionId ==
      TheStandardBearer.decisionId(p2)), run.asked.map(_.decisionId))
    assert(!run.asked.exists(_.decisionId ==
      OrderedWindow.decisionId(p2, 1)), run.asked.map(_.decisionId))
    assert(run.events.exists {
      case WalkerStepRecorded(_, ChoicePayload(id, ChooseOneAnswer(ref),
          _, true), _) =>
        id == OrderedWindow.decisionId(p2, 1) &&
          ref == DecisionOptionRef.Legacy(TheStandardBearerCard.id)
      case _ => false
    }, run.events)
```

If the fixture's public row or Iron Hand rules already hold one of the
deck's legacies, swap it for another plain legacy (Beloved, Pathfinder)
and update the suite doc. If The Standard Bearer is not offered because no
banner is in the shared bank at that point, assert the round-1 alone pick
only and say why in the test name.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly distantlands.gameplay.setup.DoorsPowerOrderSuite"`
Expected: PASS without production changes. A failure here is a finding
about slice 4's window or Task 3, not about the test: diagnose it before
editing either.

- [ ] **Step 3: Run the full JVM suite and the checks**

Run: `./sbtw test`, then `python3 scripts/check-architecture.py`.
Expected: every test passes and the goldens are unchanged (E05 is on no
golden's map; Discord's first-game edifice stays E01). The architecture
check passes.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/gameplay/setup/DoorsPowerOrderSuite.scala
git commit -m "test(setup): pin Amber Doors in the ordered pawn window"
```

---

### Checkpoint review B (after Task 4)

Dispatch one reviewer (no higher model or effort than the main agent) on
the diff since checkpoint A, briefed with the Global Constraints. Ask it
to check Amber Doors' resume shapes, the Iron Hand path, that a declined
Doors changes nothing, that the dormant legacy is never named in a line or
to another viewer, and that The Mouth still validates its goal before the
shared activation. Fix what it confirms before Task 5.

---

### Task 5: Record the rulings and close the slice

**Files:** the docs listed above.

- [ ] **Step 1: Add the rulings to `docs/rules/ambiguities.md`**

In "Catalog batch 13 rulings", extend the intro's item list after "items 9
and 10 are rulings 4 and 5" with "; items 11 to 13 are rulings 11 to 13".
In item 3, after "Ancient Writ's two," insert " Lost Doors' one and Amber
Doors' all,". In item 10, replace "No card does either yet; the Doors will
(slice 5)." with "Amber Doors does both." Then add after item 10:

```markdown
11. **Both Doors resolve at Setup and when explored** for the player whose pawn was just placed at the site: the placer in their pawn window, where they order it among their powers (item 9), and the explorer after an Explore. An explored site's edifice is placed ruined, so only Lost Doors resolves there in play.
12. **Amber Doors.** The player may discard every legacy they hold, active and dormant, to the bottom of the legacy deck, then draw 2 legacies and choose one to activate; the other stays dormant. A short deck draws what remains, and a single draw is activated without a question; an empty deck draws and activates nothing. The activation is The Mouth's: it sets the legacy's foundation, and Iron Hand activated by an Imperial is discarded at once. It needs no goal. Discarding an active legacy flips no foundation back. Matchmaker plays no part. Lines: "p2 discarded their legacies." and "p2 activated Seer."; a declined Doors writes none.
13. **Lost Doors.** The player draws 1 legacy, dormant, then discards one dormant legacy they hold, the new one included, to the bottom of the legacy deck; active legacies are never discarded. With a single dormant legacy it is discarded without a question, and with none nothing is discarded. An empty deck draws nothing, and the discard still follows. Matchmaker plays no part. Lines: "p2 drew a legacy." and "p2 discarded a dormant legacy.", never naming either.
```

- [ ] **Step 2: Add the Digital note to `docs/rules/rules-reference.md`**

After the **Ancient Writ.** bullet, add:

```markdown
- **Amber Doors and Lost Doors.** SETUP / WHEN EXPLORED, for the player who placed their pawn there: Amber Doors may discard all their legacies to draw 2 and activate one; Lost Doors draws a dormant legacy, then discards one they hold. Digital: the draws take the top of the legacy deck and the discards go to its bottom; Amber Doors activates as The Mouth does, and Lost Doors never discards an active legacy ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 11 to 13).
```

- [ ] **Step 3: Update `docs/rules/implementation-traceability.md`**

In the Explore (Wide Horizons) row, replace "Lost Doors and Ruined Temple
wait for the ROADMAP's **Cards that act on legacies**." with "Ruined
Temple waits for the ROADMAP's **Cards that act on legacies**." and, after
"Hall of Mockery and Lost Guardians move a held banner to the shared bank,
at Setup and when explored.", add:

```markdown
Lost Doors draws a legacy dormant and discards a dormant one, and Amber Doors trades every held legacy for 2 draws and an activation, at Setup and when explored (`gameplay/powers/setup/AmberDoorsRules.scala`; `LostDoorsSuite`, `AmberDoorsSuite`, `setup/DoorsPowerOrderSuite`; [Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 11 to 13).
```

In the "Legacy activation and prediction powers" row, after "caps at three
active legacies with `ResolveLegacies.cap`", add ", through
`LegacyActivation`, which Amber Doors shares".

- [ ] **Step 4: Update the ROADMAP**

In **Cards that act on legacies**, delete "Amber Doors and Lost Doors
(E05, draw legacies when explored or at setup), " and, after "slice 2",
add "; the Doors in slice 5". In **Retire the unimplemented-card machinery
across the stack**, replace "Nine powers remain" with "Seven powers
remain" and delete "Amber Doors and Lost Doors (E05) and " so the list
reads "Stone Baths and Ruined Baths (E04) and Ruined Temple (E10 ruined)".
Check the count against `unimplemented.txt`.

- [ ] **Step 5: Update the spec**

In the status line, after "Slice 4 landed 2026-11-15." add "Slice 5
landed <date>.", using the date the slice actually lands. In "Slice 5: the
Doors", after "Lost Doors draws 1, then asks which dormant legacy to
discard to the bottom.", add: "At Setup their decision ids name the
placer, since several players may place at the site in one walk."

- [ ] **Step 6: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`. Expected: no broken link.

```bash
git add docs
git commit -m "docs(catalog): record the Doors and close batch 13 slice 5"
```

- [ ] **Step 7: Merge**

Record any change made while executing in a "Changes made while
executing" paragraph after "Why the steps are sibling `Branch`es", commit
it, then merge `batch13-slice5` into `0.2.0-alpha` with `--no-ff` from the
main checkout, run `./sbtw test` there, and push nothing.
