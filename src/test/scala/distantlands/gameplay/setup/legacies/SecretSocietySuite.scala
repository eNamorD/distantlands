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

  test("under Teeming World the holder discards the excess right after " +
      "their pawn, in one choose-many (R21)"):
    val teeming = record.copy(foundations = record.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)))
    val taking: Answers =
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    val at = Situation.start(rules.withAnswers(taking))
      .parkedAt(SecretSociety.discardDecisionId(p2))(GameCommand.Begin(
        teeming, LaterGameFixture.orders(teeming, p1)))
    val held = at.ready.game.current.player(p2).get
    assert(held.pawnSite.nonEmpty)
    decide(at).query match
      case many: DecisionQuery.ChooseMany =>
        val excess = held.advisers.size - 3
        assertEquals((many.min, many.max), (excess, excess))
      case other => fail(s"expected a choose-many, got $other")
    // Teeming World asks no adviser: p2's reveal is the next question.
    val resumed = at.parkedAt(SetupProcedure.revealDecisionId(p2))()
    assertEquals(decide(resumed).decisionId,
      SetupProcedure.revealDecisionId(p2))
    val after = resumed.ready
    assertEquals(after.game.current.player(p2).get.advisers.size, 3)
    // The line names the card by the knowledge rule; the holder knows it
    // (R21).
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, resumed.events).exists(said =>
      said.text.startsWith("p2 discarded ") &&
        said.text.endsWith(s"with ${SecretSocietyCard.id.value}")))

  /** Takes the first Vision offered and, at Reveal Cards, reveals every
    * facedown Vision it is offered. */
  private val takingAndRevealing: Answers =
    case park if park.decisionId == SecretSociety.decisionId(p2) =>
      ChooseOneAnswer(park.decide.query match
        case one: DecisionQuery.ChooseOne => one.options(1).ref
        case other => fail(s"expected a choose-one, got $other"))
    case park if park.decisionId == SetupProcedure.revealDecisionId(p2) =>
      DecisionAnswer.ChooseManyAnswer(visionSlots(park.ready))

  /** p2's facedown Visions offered by slot. */
  private def visionSlots(ready: ReadyGame): Vector[DecisionOptionRef] =
    ready.game.current.player(p2).toVector.flatMap(_.advisers).zipWithIndex
      .collect { case (VisionState(_, Orientation.FaceDown), slot) =>
        DecisionOptionRef.AdviserSlot(p2, slot) }

  test("an Exile holder may reveal the Vision at Reveal Cards, which puts " +
      "it on their Vision space (R38)"):
    val exile = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Exile, SecretSocietyCard.id -> true)
    val at = Situation.start(rules.withAnswers(takingAndRevealing))
      .parkedAt(SetupProcedure.revealDecisionId(p2))(GameCommand.Begin(exile,
        LaterGameFixture.orders(exile, p1)))
    val slots = visionSlots(at.ready)
    assertEquals(slots.size, 1)
    val vision = at.ready.game.current.player(p2).get.advisers(
      slots.head.asInstanceOf[DecisionOptionRef.AdviserSlot].slot).id
    decide(at).query match
      case many: DecisionQuery.ChooseMany =>
        assert(many.options.map(_.ref).contains(slots.head), many.options)
      case other => fail(s"expected a choose-many, got $other")
    val after = at.after().ready.game.current.player(p2).get
    assertEquals(after.revealedVisions.map(_.id), Vector(vision))
    assert(!after.advisers.exists(_.id == vision), after.advisers)

  test("an Imperial holder is not offered the Vision at Reveal Cards (R38)"):
    val at = Situation.start(rules.withAnswers(takingAndRevealing))
      .parkedAt(SetupProcedure.revealDecisionId(p2))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    val slots = visionSlots(at.ready)
    assertEquals(slots.size, 1)
    decide(at).query match
      case many: DecisionQuery.ChooseMany =>
        assert(!many.options.map(_.ref).contains(slots.head), many.options)
      case other => fail(s"expected a choose-many, got $other")

  test("a holder within the limit is asked nothing"):
    // p2 holds no starting adviser and draws a hand, so the Vision leaves
    // them one adviser before Choose Adviser: a walk that never parks at
    // the discard settles through Setup.
    val taking: Answers =
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    val done = Situation.start(rules.withAnswers(taking))
      .parkedAt(SecretSociety.discardDecisionId(p2))(GameCommand.Begin(
        record, LaterGameFixture.orders(record, p1)))
    assertNotEquals(done.ready.game.current.turn.phase, Phase.Setup)
    assertEquals(done.ready.game.current.player(p2).get.advisers.size, 2)

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(SecretSociety))
    assert(PowerImplementationStatus.implemented(SecretSocietyCard.power.id))
