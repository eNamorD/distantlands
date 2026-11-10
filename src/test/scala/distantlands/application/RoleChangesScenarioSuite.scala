package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.gameplay.actions.negotiation.NegotiationDeal
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.GrandScepterAction
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Commands, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Role changes played through the service (CR p. 34). An Exile holding
  * the Grand Scepter becomes a Citizen and on a later turn exiles themself
  * (Empire slice 3a); the Chancellor offers an Exile Citizenship in a deal
  * (slice 3b). */
class RoleChangesScenarioSuite extends munit.FunSuite:
  private val gameId = "role-changes"
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)

  /** p2 is the Chancellor. p1, an Exile in their Act at Broken Peaks, holds
    * the Scepter, 2 board warbands and 5 favor. */
  private val table: Table = Table.start.imperial(p2).roundStarter(p1)
    .turn(p1, Phase.Act).pawn(p1, at = BrokenPeaksSite)
    .relic(p1, TheGrandScepterCard).boardWarbands(p1, 2).favor(p1, 5)
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

  // The Scepter's battle plan exiling a Citizen through the service is the
  // scepter-exiled golden log's script.
  test("the Chancellor offers Citizenship in a deal and the Exile accepts"):
    // p1, the Chancellor, holds the Scepter at Ancient City beside p2, an
    // Exile.
    val start = Table.start.imperial(p1).turn(p1, Phase.Act)
      .pawn(p1, at = AncientCitySite).pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BuriedGiantSite).relic(p1, TheGrandScepterCard)
      .favor(p1, 3).boardWarbands(p1, 4).boardWarbands(p2, 2)
      .allowingCitizenshipPowers
    val (service, _) = start.service()
    def deal(by: PlayerId, answer: DecisionAnswer, version: Long) =
      accepted(service.handle(gameId, version, Commands.resolveWalker(by,
        TreeDecision(NegotiationDeal.dealDecisionId, answer))))
    val opened = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Negotiation)))
    val proposed = deal(p1, DecisionAnswer.ProposeTerms(NegotiationTerms(
      citizenship = Vector(p2))), opened.nextSequence)
    val agreed = deal(p1, DecisionAnswer.AcceptDeal,
      deal(p2, DecisionAnswer.AcceptDeal, proposed.nextSequence).nextSequence)
    assertEquals(Imperials.role(Outcomes.ready(agreed.state), p2),
      Some(Role.Citizen))
    assert(lines(service).contains("p2 became a Citizen."), lines(service))
