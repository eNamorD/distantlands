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

  /** Amber Doors at Ancient City in p1's game, p1 holding no legacy. */
  private def doorsWithNoneHeld: Table =
    Table.start.edifice(AmberDoorsCard.id, EdificeSide.Intact,
      at = AncientCitySite)

  test("a deck of one is activated without a question"):
    val run = walkedAtSetup(doorsWithNoneHeld.legacyDeck(RivalCard),
      discarding(rival))
    assert(!run.asked.exists(_.decisionId == activate), run.asked)
    assertEquals(legaciesOf(run.ready),
      Vector(LegacyState(rival, active = true)))

  test("an empty deck draws and activates nothing, and says so"):
    val run = walkedAtSetup(doorsWithNoneHeld.legacyDeck(), discarding(rival))
    assertEquals(legaciesOf(run.ready), Vector.empty)
    assertEquals(lines(run.events), Vector("The legacy deck was empty."))

  test("the discards go to the bottom before the draw, so a short deck " +
      "draws them back (rulings 2 and 12)"):
    val run = walkedAtSetup(doorsWithTwoHeld.legacyDeck(RivalCard),
      discarding(key))
    val asked = run.asked.filter(_.decisionId == activate)
    assertEquals(asked.size, 1)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(rival, key)
          .map(DecisionOptionRef.Legacy(_)))
      case other => fail(s"expected a choose-one, got $other")
    assertEquals(legaciesOf(run.ready), Vector(
      LegacyState(key, active = true), LegacyState(rival, active = false)))
    assertEquals(deckOf(run.ready), Vector(ruthless))

  test("a player holding no legacy writes no discard line"):
    val run = walkedAtSetup(doorsWithNoneHeld.legacyDeck(RivalCard,
      ArbiterCard), discarding(rival))
    assertEquals(lines(run.events), Vector(s"p1 activated ${rival.value}."))
