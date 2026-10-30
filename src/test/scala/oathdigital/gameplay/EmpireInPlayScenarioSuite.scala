package oathdigital.gameplay

import oathdigital.application._
import oathdigital.gameplay.actions.minor.MinorActionProcedures
import oathdigital.gameplay.powers.PayOrAsk
import oathdigital.gameplay.powers.travel.TollRoads
import oathdigital.gameplay.actions.travel.TravelProcedure
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{CatalogNames, Look, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** A Citizen's turn and the Exile's after it, played through the service on
  * a table where the Empire holds two sites (Empire slice 1a): the Citizen
  * moves a warband, rests on the Chancellor's Supply, an Exile pays the
  * Chancellor a toll, and the next round starts with the Chancellor. */
class EmpireInPlayScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-in-play"
  private val toll = CatalogNames.denizen("Toll Roads")
  private val peaks = CatalogNames.site("Broken Peaks")
  private val otherEmpireSite = Table.otherSiteInRegionOf(peaks)

  /** p1 is the Chancellor and p2 a Citizen, who is active in Act at Broken
    * Peaks; p3 is an Exile at Buried Giant with 1 favor and 7 Supply. The
    * Empire holds Broken Peaks (2 warbands) and a second site in its region,
    * where Toll Roads stands. p1's Supply marker reads 5 and p2 carries 3
    * unspent. */
  private val empireTable: Table = Table.start.imperial(p1, p2)
    .turn(p2, Phase.Act).pawn(p2, at = peaks)
    .boardWarbands(p2, 3).warbandsAt(peaks, p2, 2)
    .warbandsAt(otherEmpireSite, p1, 2).denizen(toll, at = otherEmpireSite)
    .supply(p1, 5).supply(p2, 3).favor(p1, 0).favor(p3, 1).supply(p3, 7)

  private def look(at: GameAccepted): Look = Look(Outcomes.ready(at.state))

  private def siteWarbands(at: GameAccepted, site: SiteId): Int =
    look(at).forces(site) match
      case SiteForces.Occupied(_, n) => n
      case SiteForces.Empty => 0

  test("a Citizen's turn, an Exile's toll to the Chancellor, and a round " +
      "that starts with the Chancellor"):
    val (service, _) = empireTable.service()
    val start = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.MoveWarbands)))
    val direction = accepted(service.handle(gameId, start.nextSequence,
      GameCommand.ResolveWalker(p2, TreeDecision(
        MinorActionProcedures.directionDecisionId,
        ChooseOneAnswer(MinorActionProcedures.toSite)))))
    val moved = accepted(service.handle(gameId, direction.nextSequence,
      GameCommand.ResolveWalker(p2, TreeDecision(
        MinorActionProcedures.amountDecisionId, ChooseAmountAnswer(1)))))
    // One warband went from p2's board to the Empire site at their pawn.
    assertEquals(look(moved).boardWarbands(p2), 2)
    assertEquals(siteWarbands(moved, peaks), 3)

    val rested = accepted(service.handle(gameId, moved.nextSequence,
      MenuPlay.starting(p2, PhaseTransitionRef.BeginRest)))
    // p1's Supply of 5 plus p2's 3 unspent, capped at 7.
    assertEquals(look(rested).supply(p2), 7)
    assertEquals(look(rested).supply(p1), 5)
    assertEquals(look(rested).active, p3)

    assertEquals(look(rested).phase, Phase.Act)
    val started = accepted(service.handle(gameId, rested.nextSequence,
      MenuPlay.starting(p3, ActionRef.Travel)))
    val parked = accepted(service.handle(gameId, started.nextSequence,
      GameCommand.ResolveWalker(p3, TreeDecision(
        TravelProcedure.destinationDecisionId,
        TravelPlay.destination(otherEmpireSite)))))
    val paid = accepted(service.handle(gameId, parked.nextSequence,
      GameCommand.ResolveWalker(p3, TreeDecision(TollRoads.decisionId,
        ChooseOneAnswer(PayOrAsk.pay)))))
    assertEquals(look(paid).pawn(p3), otherEmpireSite)
    assertEquals(look(paid).favor(p3), 0)
    assertEquals(look(paid).favor(p1), 1)

    val next = accepted(service.handle(gameId, paid.nextSequence,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    assertEquals(look(next).active, p1)
    // Wake has nothing to ask the Chancellor, so p1's turn is in Act.
    assertEquals(look(next).phase, Phase.Act)
