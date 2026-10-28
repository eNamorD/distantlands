package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Outcomes, RecordedOps, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** A game won for real, through Start Chronicle, the Sun and the Throne, to
  * the journaled Chronicle. p1 is the Usurper as their Wake begins. */
class ChronicleSunThroneEndToEndSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val usurperWakes: OathState = Table.start.turn(p1, Phase.Wake)
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  test("the winner's Chronicle journals p1 as Chancellor with 3 lineage " +
      "points and a new Oath, and replays"):
    val won = accepted(rules.next(OathTransition(usurperWakes, Vector.empty),
      PhaseTransitionRef.FinishRest))
    val started = accepted(rules.resolveWalker(won.state, p1,
      ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start)))
    val done = ChronicleWalk.answeringEverything(rules, started)
    val events = won.events ++ done.events
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    val ready = Outcomes.ready(done.state)
    val record = RecordedOps.all(done.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.last
    val lineage = ready.game.current.players.find(_.player == p1).get.lineage
    assertEquals(record.lineages.find(_.id == lineage).map(_.role),
      Some(Role.Chancellor))
    assertEquals(record.era.lineageScores.get(lineage), Some(3))
    assertNotEquals(record.oathkeeperGoal,
      ready.game.campaign.baseline.oathkeeperGoal)
    assertEquals(ready.game.campaign.evolved, Some(record))
    assertEquals(accepted(events.foldLeft[Either[OathViolation, OathState]](
      Right(usurperWakes))((state, event) => state.flatMap(
        rules.evolve(_, event)))), done.state)
