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
    // The walk parks at the discard: resolving it under this id is the
    // proof, since the walker refuses an answer to any other question.
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
