package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture, SetupProcedure, SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseManyAnswer, ChooseOneAnswer}
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Wealthy: "After placing your pawn, take favor from two players or take
  * three favor from the shared bank." (spec ruling 17; R27, R29). l3, an
  * Exile, holds it; every board starts with 1 favor. */
class WealthySuite extends munit.FunSuite:
  private val (l1, l2, l3) = (LineageId("l1"), LineageId("l2"),
    LineageId("l3"))
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"),
    PlayerId("p3"))
  private val rules = Situation.productionRules(catalog)
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile, WealthyCard.id -> true)

  private def parkedAt(decisionId: String, answers: Answers =
      PartialFunction.empty, chronicle: Chronicle = record,
      seats: Vector[FirstGameParticipant] = LaterGameFixture.seats(3))
      : Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(chronicle, LaterGameFixture.orders(chronicle, p1,
        seats)))

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def favor(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.player(player).get.board.favor

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  private val fromPlayers: Answers =
    case park if park.decisionId == Wealthy.decisionId(p3) =>
      ChooseOneAnswer(Wealthy.fromPlayers)
    case park if park.decisionId == Wealthy.playersDecisionId(p3) =>
      ChooseManyAnswer(Vector(DecisionOptionRef.Player(p1),
        DecisionOptionRef.Player(p2)))
  private val fromBank: Answers =
    case park if park.decisionId == Wealthy.decisionId(p3) =>
      ChooseOneAnswer(Wealthy.fromBank)

  test("with two other players holding favor the holder chooses between " +
      "them and the bank, no Decline"):
    val at = parkedAt(Wealthy.decisionId(p3))
    val asked = decide(at)
    assertEquals(asked.owner, p3)
    asked.query match
      case one: DecisionQuery.ChooseOne => assertEquals(one.options.map(_.ref),
        Vector(Wealthy.fromPlayers, Wealthy.fromBank))
      case other => fail(s"expected a choose-one, got $other")

  test("from two of three players: one choose-many of exactly two, each " +
      "answer resumed, then 1 favor from each"):
    val p4 = PlayerId("p4")
    val at = parkedAt(Wealthy.playersDecisionId(p3), fromPlayers,
      seats = LaterGameFixture.seats(4))
    decide(at).query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max), (2, 2))
        assertEquals(many.options.map(_.ref), Vector(p1, p2, p4)
          .map(DecisionOptionRef.Player(_)))
      case other => fail(s"expected a choose-many, got $other")
    val before = at.ready
    // p3's adviser question is the step after their pawn window.
    val next = at.parkedAt(SetupProcedure.adviserDecisionId(p3))()
    assertEquals(decide(next).decisionId, SetupProcedure.adviserDecisionId(p3))
    val after = next.ready
    assertEquals(Vector(p1, p2, p3, p4).map(favor(after, _)), Vector(
      favor(before, p1) - 1, favor(before, p2) - 1, favor(before, p3) + 2,
      favor(before, p4)))

  test("with exactly two other players holding favor both give, with no " +
      "second question"):
    val at = parkedAt(Wealthy.decisionId(p3), fromPlayers)
    val before = at.ready
    val after = at.parkedAt(Wealthy.playersDecisionId(p3))()
    assertNotEquals(after.ready.game.current.turn.phase, Phase.Setup)
    assertEquals(favor(after.ready, p3), favor(before, p3) + 2)

  test("from the bank: 3 favor, no further question"):
    val at = parkedAt(Wealthy.decisionId(p3), fromBank)
    val before = favor(at.ready, p3)
    val done = at.parkedAt(Wealthy.playersDecisionId(p3))()
    assertEquals(favor(done.ready, p3), before + 3)

  test("a two-player game asks nothing and the bank gives 3"):
    val two = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Citizen, WealthyCard.id -> true)
    val ready = accepted(GameStartRules.evolve(catalog, two,
      LaterGameFixture.orders(two, p1, LaterGameFixture.seats(2))))
    val walked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
        Vector.empty))
    assert(!walked.asked.exists(_.decisionId.contains("wealthy")),
      walked.asked)
    assertEquals(favor(walked.ready, p2), favor(ready, p2) + 3)

  test("the lines name the players or the bank"):
    val players = Situation.wake(rules.withAnswers(fromPlayers), record,
      LaterGameFixture.orders(record, p1))
    assert(lines(players).contains(
      s"p3 took 1 favor from p1 and p2 with ${WealthyCard.id.value}"),
      lines(players))
    val bank = Situation.wake(rules.withAnswers(fromBank), record,
      LaterGameFixture.orders(record, p1))
    assert(lines(bank).contains(
      s"p3 took 3 favor from the shared bank with ${WealthyCard.id.value}"),
      lines(bank))

  test("The Standard Bearer then Wealthy for one holder (R27)"):
    val both = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l3, Role.Exile, TheStandardBearerCard.id -> true,
      WealthyCard.id -> true)
    val ready = accepted(GameStartRules.evolve(catalog, both,
      LaterGameFixture.orders(both, p1)))
    val asked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
        Vector.empty)).asked.map(_.decisionId).filter(_.contains(".legacy."))
    assertEquals(asked, Vector(TheStandardBearer.decisionId(p3),
      Wealthy.decisionId(p3)))

  test("Wealthy is registered among the setup legacies and alters " +
      "Foundation II"):
    assert(LegacySetupPowers.all.contains(Wealthy))
    assert(LegacySetupPowers.all.indexOf(TheStandardBearer) <
      LegacySetupPowers.all.indexOf(Wealthy))
    assertEquals(WealthyCard.alters, Some(FoundationNumber.II))
