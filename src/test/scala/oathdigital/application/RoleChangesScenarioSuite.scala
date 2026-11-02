package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.GrandScepterAction
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, present}

/** An Exile holding the Grand Scepter becomes a Citizen, and on a later
  * turn exiles themself (CR p. 34; Empire slice 3a), played through the
  * service. Slice 3b adds the spec's version, where Citizenship comes in a
  * deal. */
class RoleChangesScenarioSuite extends munit.FunSuite:
  private val gameId = "role-changes"
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)

  /** p2 is the Chancellor. p1, an Exile in their Act at Broken Peaks, holds
    * the Scepter, 2 board warbands and 5 favor. */
  private val table: Table = Table.start.imperial(p2).roundStarter(p1)
    .turn(p1, Phase.Act).pawn(p1, at = "Broken Peaks")
    .relic(p1, "The Grand Scepter").boardWarbands(p1, 2).favor(p1, 5)
    .allowingCitizenshipPowers

  private def lines(service: GameApplicationService)(using munit.Location) =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(p2)))

  test("an Exile becomes a Citizen by the Scepter and later exiles themself"):
    val (service, repository) = table.service()
    val citizen = accepted(service.handle(gameId, 0L, MenuPlay.starting(p1,
      ActionRef.UsePower(GrandScepterAction.id), Vector(scepter))))
    val afterScepter = Outcomes.ready(citizen.state)
    assertEquals(Imperials.role(afterScepter, p1), Some(Role.Citizen))
    // The Act ended: Rest ran and p2's turn began.
    assertEquals(afterScepter.game.current.turn.activePlayer, p2)
    assert(lines(service).contains(
      "The Grand Scepter: Became a Citizen."), lines(service))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, citizen.state)

    // A later turn of p1's, from the state the Scepter left.
    val (later, _) = Table.from(afterScepter).turn(p1, Phase.Act).service()
    val exiled = accepted(later.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.SelfExile)))
    assertEquals(Imperials.role(Outcomes.ready(exiled.state), p1),
      Some(Role.Exile))
    assert(lines(later).contains("Gave 3 favor to p2 and became an Exile."),
      lines(later))

  test("the projection shows the Scepter disabled under the first-game option"):
    val ignoring = table.update(ready => ready.copy(setup =
      ready.setup.copy(ignoreCitizenshipPowers = true)))
    val row = present(new GameProjector(catalog).project(gameId,
      LoadedGame(ignoring.phaseMenuParked.state, 0L), p1).walkerDecision
      .flatMap(_.query).toVector.flatMap(_.offeredOptions)
      .find(_.id == scepter.wireId), "the Scepter row")
    assertEquals(row.state, "unavailable")
    assertEquals(row.reason, Some(Citizenship.ignoredReason))
