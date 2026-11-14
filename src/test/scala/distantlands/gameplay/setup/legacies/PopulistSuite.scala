package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}
import distantlands.application.GameCommand

/** Populist: "When setting up the favor banks, you may choose to divide the
  * favor among the banks evenly. If you do, take any remainder." (spec
  * ruling 12; R14, R15). l2, a Citizen, holds it. */
class PopulistSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))

  private def holding(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.holding(chronicle, l2, Role.Citizen,
      PopulistCard.id -> true)
  private val record = holding(LaterGameFixture.chronicle(l1, l2))
  private def moversAndShakers(chronicle: Chronicle): Chronicle =
    chronicle.copy(foundations = chronicle.foundations.updated(
      FoundationNumber.II, FoundationState(FoundationFace.Altered, Set.empty)))

  private val dividing: Answers =
    case park if park.decisionId == Populist.decisionId(p2) =>
      ChooseOneAnswer(Populist.divide)

  /** Populist hooks a window, so the walk needs the real power catalog. */
  private val rules = Situation.productionRules(catalog)

  /** Setup parked at p1's pawn, the first question after the banks. */
  private def atPawn(chronicle: Chronicle, answers: Answers): Situation =
    Situation.start(rules.withAnswers(answers))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        chronicle, LaterGameFixture.orders(chronicle, p1)))

  /** The banks and p2's favor just before Populist's question. */
  private def before(chronicle: Chronicle): (Map[Suit, Int], Int) =
    val ready = Situation.start(rules)
      .parkedAt(Populist.decisionId(p2))(GameCommand.Begin(chronicle,
        LaterGameFixture.orders(chronicle, p1))).ready
    ready.banks.favor -> ready.game.current.player(p2).get.board.favor

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  Vector("Powerful Tribes" -> record,
      "Movers and Shakers" -> moversAndShakers(record)).foreach {
    case (face, chronicle) =>
      test(s"dividing under $face leaves every bank the even share and " +
          "gives the holder the remainder"):
        val (banks, favor) = before(chronicle)
        val (each, rest) = Populist.shares(banks)
        val ready = atPawn(chronicle, dividing).ready
        assertEquals(ready.banks.favor, Suit.all.map(_ -> each).toMap)
        assertEquals(ready.game.current.player(p2).get.board.favor,
          favor + rest)
  }

  test("the line names the remainder taken (R15)"):
    val (banks, _) = before(record)
    val (_, rest) = Populist.shares(banks)
    assert(rest > 0, s"the fixture's banks divide evenly: $banks")
    assert(lines(atPawn(record, dividing)).contains(
      s"p2 divided the favor evenly among the banks with " +
        s"${PopulistCard.id.value} and took $rest favor"))

  test("with no remainder the line says only that the banks were divided " +
      "(R15)"):
    val (_, rest) = Populist.shares(before(record)._1)
    // Each denizen stored at an Empire site adds 1 favor to its bank.
    val (moved, deck) = record.worldDeck.splitAt(Suit.all.size - rest)
    val even = record.copy(worldDeck = deck, world = record.world.updated(0,
      record.world(0).copy(items = record.world(0).items ++ moved)))
    assertEquals(Populist.shares(before(even)._1)._2, 0)
    val said = lines(atPawn(even, dividing))
    assert(said.contains(s"p2 divided the favor evenly among the banks " +
      s"with ${PopulistCard.id.value}"), said)

  test("declining leaves the banks as they were and writes no line"):
    val (banks, _) = before(record)
    val declined = atPawn(record, PartialFunction.empty)
    assertEquals(declined.ready.banks.favor, banks)
    assert(!lines(declined).exists(_.contains(PopulistCard.id.value)))

  test("the holder is asked once, Decline first, and a walk resumed at the " +
      "question divides on the answer"):
    val parked = Situation.start(rules.withAnswers(dividing))
      .parkedAt(Populist.decisionId(p2))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    present(accepted(ParkedNode.of(parked.state, catalog,
        WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.query match
        case one: DecisionQuery.ChooseOne => assertEquals(
          one.options.map(_.ref), Vector(Populist.decline, Populist.divide))
        case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected a decision, parked on $other")
    val (each, _) = Populist.shares(parked.ready.banks.favor)
    assertEquals(parked.parkedAt(SetupProcedure.pawnDecisionId(p1))()
      .ready.banks.favor.values.toSet, Set(each))

  test("shares divides by six, rounding down"):
    assertEquals(Populist.shares(Suit.all.map(_ -> 3).toMap
      .updated(Suit.all.head, 4)), (3, 1))
    assertEquals(Populist.shares(Map.empty), (0, 0))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Populist))
    assert(PowerImplementationStatus.implemented(PopulistCard.power.id))
