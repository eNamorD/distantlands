package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.{ProcedureNotes, VisionRules}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Secret Society: "Before building the world deck, you may secretly choose
  * a Vision except the Conspiracy to take as a facedown adviser (discard
  * excess advisers). If you do, advance the Visions Drawn marker once."
  * (spec ruling 14; R19, R22). */
class SecretSocietySuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    SecretSocietyCard.id -> true)
  private val rules = Situation.productionRules(catalog)

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def parked(answers: Answers = PartialFunction.empty)
      : Situation = Situation.start(rules.withAnswers(answers))
    .parkedAt(SecretSociety.decisionId(p2))(GameCommand.Begin(record,
      LaterGameFixture.orders(record, p1)))

  private def deckVisions(ready: ReadyGame): Vector[VisionId] =
    ready.game.current.commonCards.worldDeck.collect { case id: VisionId => id }

  test("the holder is offered every Vision but the Conspiracy, Decline " +
      "first, then in catalog order (R22)"):
    val at = parked()
    val asked = decide(at)
    assertEquals(asked.owner, p2)
    asked.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), SecretSociety.decline +:
          deckVisions(at.ready).filterNot(_ == VisionRules.Conspiracy)
            .sortBy(_.value).map(DecisionOptionRef.Vision(_)))
      case other => fail(s"expected a choose-one, got $other")

  test("a walk resumed at the question takes the Vision facedown, advances " +
      "Visions Drawn and rebuilds the deck in packets"):
    val at = parked()
    val vision = deckVisions(at.ready).filterNot(_ == VisionRules.Conspiracy)
      .head
    val drawn = at.ready.game.current.tracks.visionsDrawn
    val after = at.withAnswers {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Vision(vision))
    }.parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assert(after.game.current.player(p2).get.advisers.contains(
      VisionState(vision, Orientation.FaceDown)))
    assertEquals(after.game.current.tracks.visionsDrawn, drawn + 1)
    val deck = after.game.current.commonCards.worldDeck
    assertEquals(deck.take(12).count(_.isInstanceOf[VisionId]), 2)
    assertEquals(deck.slice(12, 29).count(_.isInstanceOf[VisionId]), 2)
    assertEquals(deck.filterNot(_.isInstanceOf[VisionId]),
      at.ready.game.current.commonCards.worldDeck
        .filterNot(_.isInstanceOf[VisionId]))

  test("under Teeming World the deck holds only Visions, and the four left " +
      "are shuffled through it"):
    val teeming = record.copy(foundations = record.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)))
    val at = Situation.start(rules).parkedAt(SecretSociety.decisionId(p2))(
      GameCommand.Begin(teeming, LaterGameFixture.orders(teeming, p1)))
    val before = at.ready.game.current.commonCards.worldDeck
    assert(before.forall(_.isInstanceOf[VisionId]), before)
    val vision = deckVisions(at.ready).filterNot(_ == VisionRules.Conspiracy)
      .head
    val after = at.withAnswers {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Vision(vision))
    }.parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assertEquals(after.game.current.commonCards.worldDeck.toSet,
      before.filterNot(_ == vision).toSet)

  test("declining changes nothing and writes no line"):
    val at = parked()
    val after = at.parkedAt(SetupProcedure.pawnDecisionId(p1))()
    assertEquals(after.ready.game.current.commonCards.worldDeck,
      at.ready.game.current.commonCards.worldDeck)
    assert(!NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, after.events).exists(_.text.contains(
      SecretSocietyCard.id.value)))

  test("the line names no Vision"):
    val woken = Situation.wake(rules.withAnswers {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    }, record, LaterGameFixture.orders(record, p1))
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text).contains(
      s"p2 took a Vision as a facedown adviser with " +
        SecretSocietyCard.id.value))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(SecretSociety))
    assert(PowerImplementationStatus.implemented(SecretSocietyCard.power.id))
