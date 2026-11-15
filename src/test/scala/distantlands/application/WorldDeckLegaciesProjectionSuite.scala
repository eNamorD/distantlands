package distantlands.application

import distantlands.gameplay.setup.{ImperialMaps, LaterGameFixture,
  SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{SecretSociety,
  SecretSocietyCard, Seer, SeerCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.protocol.projection.DecisionQueryProjection
import distantlands.testkit.{Answers, Situation}

/** Who sees the world deck's Visions while Secret Society's and Seer's
  * holder chooses (spec rulings 14 and 15; R20): the holder, and only
  * while their question is the parked decision. l2, a Citizen, holds the
  * legacies. */
class WorldDeckLegaciesProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    SecretSocietyCard.id -> true, SeerCard.id -> true)
  private val rules = Situation.productionRules(catalog)
  /** Secret Society takes the first Vision it offers. */
  private val taking: Answers =
    case park if park.decisionId == SecretSociety.decisionId(p2) =>
      park.decide.query match
        case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
        case other => fail(s"expected a choose-one, got $other")

  /** Secret Society alone: no Seer peeks after it. */
  private val societyOnly = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    SecretSocietyCard.id -> true)

  private def parkedAt(decisionId: String, answers: Answers =
      PartialFunction.empty, chronicle: Chronicle = record): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(chronicle, LaterGameFixture.orders(chronicle, p1)))

  private def deckVisions(ready: ReadyGame): Vector[VisionId] =
    ready.game.current.commonCards.worldDeck.collect { case id: VisionId => id }

  private def seenBy(ready: ReadyGame, viewer: Option[PlayerId])
      : Vector[VisionId] = deckVisions(ready)
    .filter(presentation.identifiesAt(ready, viewer, _))

  test("before the question nobody identifies the deck's Visions"):
    val at = parkedAt(ImperialMaps.decisionId).ready
    Vector(Some(p2), Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))

  test("while Secret Society's question waits, only its holder does"):
    val at = parkedAt(SecretSociety.decisionId(p2))
    assertEquals(seenBy(at.ready, Some(p2)), deckVisions(at.ready))
    Vector(Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at.ready, viewer), Vector.empty, viewer))
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("deck", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 5)))
    assert(!projector.project("deck", loaded, p1).walkerDecision
      .exists(_.query.nonEmpty))

  test("after the take nobody identifies the deck's Visions, and only the " +
      "holder their new adviser"):
    val at = parkedAt(SetupProcedure.pawnDecisionId(p1), taking,
      societyOnly).ready
    Vector(Some(p2), Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))
    val taken = at.game.current.player(p2).get.advisers.collect {
      case VisionState(id, _) => id }
    assertEquals(taken.size, 1)
    assert(presentation.identifiesAt(at, Some(p2), taken.head))
    Vector(Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(at, viewer, taken.head), viewer))

  test("while Seer's question waits, only its holder identifies the deck's " +
      "Visions and is shown their positions"):
    val at = parkedAt(Seer.decisionId(p2), taking)
    assertEquals(seenBy(at.ready, Some(p2)), deckVisions(at.ready))
    Vector(Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at.ready, viewer), Vector.empty, viewer))
    val loaded = LoadedGame(at.state, 1L)
    projector.project("deck", loaded, p2).walkerDecision
        .flatMap(_.query) match
      case Some(amounts: DecisionQueryProjection.ChooseAmounts) =>
        assertEquals(amounts.slots.flatMap(_.suggested),
          deckVisions(at.ready).map(vision =>
            at.ready.game.current.commonCards.worldDeck.indexOf(vision) + 1))
      case other => fail(s"expected the Seer's question, got $other")
    assert(!projector.project("deck", loaded, p1).walkerDecision
      .exists(_.query.nonEmpty))

  test("after a Decline the holder does not identify the deck's Visions " +
      "(R20)"):
    val at = parkedAt(SetupProcedure.pawnDecisionId(p1),
      chronicle = societyOnly).ready
    assertEquals(seenBy(at, Some(p2)), Vector.empty)

  test("after Seer's answer the holder keeps knowing the deck's Visions, " +
      "and nobody else does (R20)"):
    val at = parkedAt(SetupProcedure.pawnDecisionId(p1), taking).ready
    assertEquals(seenBy(at, Some(p2)), deckVisions(at))
    Vector(Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))

  test("an Exile holder's Vision is named to other seats only once it is " +
      "revealed at Reveal Cards (R38)"):
    val exile = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Exile, SecretSocietyCard.id -> true)
    def visionSlot(ready: ReadyGame): Option[(Int, CardId)] =
      ready.game.current.player(p2).toVector.flatMap(_.advisers).zipWithIndex
        .collectFirst { case (VisionState(id, Orientation.FaceDown), slot) =>
          slot -> id }
    val revealing = taking.orElse {
      case park if park.decisionId == SetupProcedure.revealDecisionId(p2) =>
        DecisionAnswer.ChooseManyAnswer(visionSlot(park.ready).toVector.map(
          (slot, _) => DecisionOptionRef.AdviserSlot(p2, slot)))
    }
    val at = parkedAt(SetupProcedure.revealDecisionId(p2), revealing, exile)
    val (_, vision) = visionSlot(at.ready).getOrElse(fail("no Vision taken"))
    assert(presentation.identifiesAt(at.ready, Some(p2), vision))
    Vector(Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(at.ready, viewer, vision), viewer))
    val after = at.after().ready
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(presentation.identifiesAt(after, viewer, vision), viewer))
