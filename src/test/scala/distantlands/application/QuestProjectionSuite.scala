package distantlands.application

import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{QuestToDistantLands,
  QuestToDistantLandsCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Situation}

/** Who sees the Dispossessed and the swapped cards of Quest to Distant
  * Lands (spec ruling 19; R31): the holder alone. l2, a Citizen, holds it
  * and stays off the map; two denizens wait in the Dispossessed. */
class QuestProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record =
    val base = LaterGameFixture.chronicle(l1, l2)
    val (kept, deck) = base.worldDeck.splitAt(2)
    SetupLegacyFixture.holding(base.copy(worldDeck = deck,
      dispossessed = kept), l2, Role.Citizen,
      QuestToDistantLandsCard.id -> true)
  private val rules = Situation.productionRules(catalog)
  private val stayingOff: Answers =
    case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
      ChooseOneAnswer(QuestToDistantLands.stayOff)

  private def parkedAt(decisionId: String, answers: Answers) =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(record, LaterGameFixture.orders(record, p1)))

  test("while the swap waits, the holder alone identifies the Dispossessed " +
      "and is offered them"):
    val at = parkedAt(QuestToDistantLands.swapDecisionId(p2), stayingOff)
    record.dispossessed.foreach { card =>
      assert(presentation.identifiesAt(at.ready, Some(p2), card), card)
      Vector(Some(p1), None).foreach(viewer =>
        assert(!presentation.identifiesAt(at.ready, viewer, card),
          s"$viewer $card"))
    }
    val loaded = LoadedGame(at.state, 1L)
    assertEquals(projector.project("quest", loaded, p2).walkerDecision
      .flatMap(_.query).map(_.offeredOptions.size),
      Some(record.dispossessed.size + 1))
    assert(!projector.project("quest", loaded, p1).walkerDecision
      .exists(_.query.nonEmpty))

  test("after the swap only the holder identifies the card taken, and " +
      "nobody the card given"):
    val taken = record.dispossessed.head
    val hand = parkedAt(QuestToDistantLands.swapDecisionId(p2), stayingOff)
      .ready.game.current.temporaryHands(p2)
    val handed = hand.head
    val answers = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(taken))
      case park if park.decisionId == QuestToDistantLands.giveDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(
          handed.asInstanceOf[DenizenId]))
    }
    val after = parkedAt(SetupProcedure.pawnDecisionId(PlayerId("p3")),
      answers).ready
    assert(presentation.identifiesAt(after, Some(p2), taken))
    Vector(Some(p1), None).foreach { viewer =>
      assert(!presentation.identifiesAt(after, viewer, taken), viewer)
      assert(!presentation.identifiesAt(after, viewer, handed), viewer)
    }
