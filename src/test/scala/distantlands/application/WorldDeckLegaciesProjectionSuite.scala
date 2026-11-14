package distantlands.application

import distantlands.gameplay.setup.{ImperialMaps, LaterGameFixture,
  SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{SecretSociety,
  SecretSocietyCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
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
    SecretSocietyCard.id -> true)
  private val rules = Situation.productionRules(catalog)
  /** Secret Society takes the first Vision it offers. */
  private val taking: Answers =
    case park if park.decisionId == SecretSociety.decisionId(p2) =>
      park.decide.query match
        case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
        case other => fail(s"expected a choose-one, got $other")

  private def parkedAt(decisionId: String, answers: Answers =
      PartialFunction.empty): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(record, LaterGameFixture.orders(record, p1)))

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
    val at = parkedAt(SetupProcedure.pawnDecisionId(p1), taking).ready
    Vector(Some(p2), Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))
    val taken = at.game.current.player(p2).get.advisers.collect {
      case VisionState(id, _) => id }
    assertEquals(taken.size, 1)
    assert(presentation.identifiesAt(at, Some(p2), taken.head))
    Vector(Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(at, viewer, taken.head), viewer))
