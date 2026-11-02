package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Outcomes, RoundEndChoices, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Stable Regime with a Successor taking the win (CR p. 19; Empire phase
  * design, "Slice 2"), played through the service. */
class EmpireEndingsScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-endings"

  /** p1, the Chancellor, holds the Oathkeeper title of the People with the
    * People's Favor. p2, a Citizen, holds the Darkest Secret, which is the
    * Successor goal. p3, an Exile, acts last in round 5. */
  private val table: Table = Table.start.imperial(p1, p2)
    .oathkeeperGoal(OathkeeperGoal.ThePeople).oathkeeper(Some(p1))
    .peoplesFavor(Some(p1), favor = 1).darkestSecret(Some(p2), secrets = 1)
    .turn(p3, Phase.Act)
    // No Table step sets the round.
    .update(_.updateCurrent(c => c.copy(tracks =
      c.tracks.copy(round = 5, usurperLimited = false))))

  test("a 6 on the end die gives the Successor the Chancellor's win"):
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    val rested = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    assertEquals(Outcomes.ready(rested.state).game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.contains("The end die rolled 6. Stable Regime needed 6."),
      lines)
    assert(lines.contains("p2 won as Successor to the Chancellor"), lines)

  test("a 5 on the end die starts round 6"):
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(5))
    val rested = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    val current = Outcomes.ready(rested.state).game.current
    assertEquals(current.result, None)
    assertEquals(current.tracks.round, 6)
