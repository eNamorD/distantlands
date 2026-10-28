package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{EconomyActions, Outcomes, RecordedOps, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The Chronicle Phase's start and end (Chronicle steps design, "Explicit
  * start" and "Chaining"). Each game here ends for real: p1 is the Usurper
  * when their Wake begins, so the Wake check after Finish Rest makes p1 the
  * winner. */
class ChronicleBeginSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.blanks)

  private val usurperWakes: OathState = Table.start.turn(p1, Phase.Wake)
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  /** The driver after p2's Finish Rest hands p1 a Wake that p1 wins. */
  private def usurperWon: OathTransition =
    accepted(rules.next(OathTransition(usurperWakes, Vector.empty),
      PhaseTransitionRef.FinishRest))

  private def startChronicle(state: OathState, player: PlayerId) =
    rules.resolveWalker(state, player, ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start))

  private def current(state: OathState): CurrentGameState =
    Outcomes.ready(state).game.current

  private def replayed(from: OathState, events: Vector[OathEvent])
      : OathState =
    accepted(events.foldLeft[Either[OathViolation, OathState]](Right(from))(
      (state, event) => state.flatMap(rules.evolve(_, event))))

  test("a result parks the winner's Start Chronicle decision"):
    val won = usurperWon
    assertEquals(current(won.state).result.map(_.winner), Some(p1))
    assertEquals(current(won.state).walkerProcedure,
      Some(TriggeredProcedureRef.ChronicleBegin))
    val parked = EconomyActions.parkedDecision(won.state)
    assertEquals((parked.decisionId, parked.owner),
      (ChronicleBeginProcedure.decisionId, p1))
    assertEquals(current(won.state).turn.phase, Phase.Wake)

  test("only the winner may start the Chronicle"):
    rejected(startChronicle(usurperWon.state, p2))

  test("starting the Chronicle enters its phase and parks the Sun's first " +
      "choice"):
    val started = accepted(startChronicle(usurperWon.state, p1))
    assertEquals(current(started.state).turn.phase, Phase.Chronicle)
    val parked = EconomyActions.parkedDecision(started.state)
    assertEquals((parked.decisionId, parked.owner),
      (ChronicleTaskAssignment.worldDecisionId, p1))
    assertEquals(Outcomes.ready(started.state).game.campaign.evolved, None)

  test("finishing the Chronicle journals the evolved Chronicle"):
    val done = ChronicleWalk.answeringEverything(rules,
      accepted(startChronicle(usurperWon.state, p1)))
    val campaign = Outcomes.ready(done.state).game.campaign
    assert(campaign.evolved.nonEmpty)
    assert(RecordedOps.all(done.events).contains(
      ChronicleUpdated(campaign.evolved.get)), done.events)
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    assertEquals(current(done.state).walkerPending, None)

  test("the Chronicle runs once: nothing starts after it is journaled"):
    val started = ChronicleWalk.answeringEverything(rules,
      accepted(startChronicle(usurperWon.state, p1)))
    Vector[ProcedureRef](TriggeredProcedureRef.ChronicleFinish,
      PhaseTransitionRef.FinishRest).foreach: finished =>
      assertEquals(accepted(rules.next(
        OathTransition(started.state, Vector.empty), finished)).events,
        Vector.empty, finished)
    rejected(startChronicle(started.state, p1))

  test("replaying the recorded events reproduces the Chronicle phase state"):
    val won = usurperWon
    val started = ChronicleWalk.answeringEverything(rules,
      accepted(startChronicle(won.state, p1)))
    assertEquals(replayed(usurperWakes, won.events ++ started.events),
      started.state)
