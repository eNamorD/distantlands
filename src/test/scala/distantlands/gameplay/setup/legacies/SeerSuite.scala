package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, DistributeAnswer}
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Seer: "When building the world deck, peek at the Visions and secretly
  * place them in any positions." (spec ruling 15; R23, R24). */
class SeerSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val rules = Situation.productionRules(catalog)
  private def holding(chronicle: Chronicle,
      legacies: (LegacyId, Boolean)*): Chronicle =
    SetupLegacyFixture.holding(chronicle, l2, Role.Citizen, legacies*)
  private val record = holding(LaterGameFixture.chronicle(l1, l2),
    SeerCard.id -> true)
  private def teeming(chronicle: Chronicle): Chronicle = chronicle.copy(
    foundations = chronicle.foundations.updated(FoundationNumber.IV,
      FoundationState(FoundationFace.Altered, Set.empty)))

  private def parked(chronicle: Chronicle,
      answers: Answers = PartialFunction.empty): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(
      Seer.decisionId(p2))(GameCommand.Begin(chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def asked(situation: Situation): DecisionQuery.ChooseAmounts =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.query match
        case amounts: DecisionQuery.ChooseAmounts =>
          assertEquals(decide.owner, p2)
          amounts
        case other => fail(s"expected one amount per Vision, got $other")
      case other => fail(s"expected a decision, parked on $other")

  private def deck(ready: ReadyGame): Vector[WorldCardId] =
    ready.game.current.commonCards.worldDeck
  private def visions(ready: ReadyGame): Vector[VisionId] =
    deck(ready).collect { case id: VisionId => id }

  /** Each Vision at `positions`, in slot order. */
  private def placing(positions: Vector[Int]): Answers =
    case park if park.decisionId == Seer.decisionId(p2) =>
      park.decide.query match
        case amounts: DecisionQuery.ChooseAmounts =>
          DistributeAnswer(amounts.slots.zip(positions).map((slot, at) =>
            DistributeAmount(slot.ref, at)))
        case other => fail(s"expected one amount per Vision, got $other")

  test("the holder is asked one amount per Vision, 1 to the deck's size, " +
      "suggested where each lies"):
    val at = parked(record)
    val query = asked(at)
    val cards = deck(at.ready)
    assertEquals(query.slots, visions(at.ready).map(vision => DistributeSlot(
      DecisionOptionRef.Vision(vision), 1, cards.size,
      Some(cards.indexOf(vision) + 1))))
    assert(query.distinct)

  test("a walk resumed at the question lands each Vision exactly where " +
      "chosen, the denizens in their order"):
    val at = parked(record)
    val before = deck(at.ready)
    val count = visions(at.ready).size
    val after = at.withAnswers(placing((1 to count).toVector))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assertEquals(deck(after).take(count), visions(at.ready))
    assertEquals(deck(after).filterNot(_.isInstanceOf[VisionId]),
      before.filterNot(_.isInstanceOf[VisionId]))

  test("a duplicate position is refused"):
    val at = parked(record)
    val oath = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
    val query = asked(at)
    assert(oath.resolveWalker(at.state, p2, Seer.decisionId(p2),
      DistributeAnswer(query.slots.map(slot =>
        DistributeAmount(slot.ref, 1)))).isLeft)

  test("under Teeming World the holder sets the Visions' order"):
    val at = parked(teeming(record))
    assert(deck(at.ready).forall(_.isInstanceOf[VisionId]))
    val count = visions(at.ready).size
    val after = at.withAnswers(placing((1 to count).reverse.toVector))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assertEquals(deck(after), visions(at.ready).reverse)

  test("Secret Society then Seer for one holder: Seer places the Visions " +
      "left (R24)"):
    val both = holding(LaterGameFixture.chronicle(l1, l2),
      SecretSocietyCard.id -> true, SeerCard.id -> true)
    val at = parked(both, {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => fail(s"expected a choose-one, got $other")
    })
    val taken = at.ready.game.current.player(p2).get.advisers.collect {
      case VisionState(id, _) => id }
    assertEquals(taken.size, 1)
    assertEquals(asked(at).slots.map(_.ref),
      visions(at.ready).map(DecisionOptionRef.Vision(_)))
    assert(!visions(at.ready).contains(taken.head))

  test("the line names no Vision"):
    val woken = Situation.wake(rules, record,
      LaterGameFixture.orders(record, p1))
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text).contains(
      s"p2 placed the Visions with ${SeerCard.id.value}"))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Seer))
    assert(LegacySetupPowers.all.indexOf(SecretSociety) <
      LegacySetupPowers.all.indexOf(Seer))
    assert(PowerImplementationStatus.implemented(SeerCard.power.id))

  test("placed inserts in ascending position so each lands where chosen"):
    val (v1, v2) = (VisionId("vision:1"), VisionId("vision:2"))
    val (d1, d2, d3) = (DenizenId("d1"), DenizenId("d2"), DenizenId("d3"))
    assertEquals(Seer.placed(Vector(v1, d1, d2, v2, d3),
      Map(v1 -> 5, v2 -> 2)), Vector(d1, v2, d2, d3, v1))

  test("after the answer the holder keeps knowing the Visions, as a peek is " +
      "kept, and nobody else does (R20)"):
    val at = parked(record)
    val count = visions(at.ready).size
    val after = at.withAnswers(placing((1 to count).toVector))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    val known = after.knowledge.advisers.getOrElse(p2, Vector.empty)
    assert(visions(after).forall(known.contains), known)
    assert(after.knowledge.advisers.getOrElse(p1, Vector.empty)
      .forall(card => !visions(after).contains(card)))
