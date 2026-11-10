package distantlands.gameplay

import distantlands.application._
import distantlands.gameplay.actions.minor.MinorActionProcedures
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.powers.PayOrAsk
import distantlands.gameplay.powers.travel.{TollRoads, TollRoadsCard}
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import distantlands.testkit.{Commands, Look, MenuPlay, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** A Citizen's turn and the Exile's after it, played through the service on
  * a table where the Empire holds two sites (Empire slice 1a): the Citizen
  * moves a warband, rests on the Chancellor's Supply, an Exile pays the
  * Chancellor a toll, and the next round starts with the Chancellor. */
class EmpireInPlayScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-in-play"
  private val toll = TollRoadsCard.id
  private val peaks = BrokenPeaksSite.id
  private val otherEmpireSite = Table.otherSiteInRegionOf(peaks)

  /** p2 is the Chancellor and p3 a Citizen, who is active in Act at Broken
    * Peaks; p1 is an Exile at Buried Giant with 1 favor and 7 Supply. The
    * Empire holds Broken Peaks (2 warbands) and a second site in its region,
    * where Toll Roads stands. p2's Supply marker reads 5 and p3 carries 3
    * unspent. */
  private val empireTable: Table = Table.start.imperial(p2, p3)
    .turn(p3, Phase.Act).pawn(p3, at = peaks)
    .pawn(p1, at = BuriedGiantSite)
    .boardWarbands(p3, 3).warbandsAt(peaks, p3, 2)
    .warbandsAt(otherEmpireSite, p2, 2).denizen(toll, at = otherEmpireSite)
    .supply(p2, 5).supply(p3, 3).favor(p2, 0).favor(p1, 1).supply(p1, 7)

  private def look(at: GameAccepted): Look = Look(Outcomes.ready(at.state))

  private def siteWarbands(at: GameAccepted, site: SiteId): Int =
    look(at).forces(site) match
      case SiteForces.Occupied(_, n) => n
      case SiteForces.Empty => 0

  test("a Citizen's turn, an Exile's toll to the Chancellor, and a round " +
      "that starts with the Chancellor"):
    val (service, _) = empireTable.service()
    val start = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, ActionRef.MoveWarbands)))
    val direction = accepted(service.handle(gameId, start.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(
        MinorActionProcedures.directionDecisionId,
        ChooseOneAnswer(MinorActionProcedures.toSite)))))
    val moved = accepted(service.handle(gameId, direction.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(
        MinorActionProcedures.amountDecisionId, ChooseAmountAnswer(1)))))
    // One warband went from p3's board to the Empire site at their pawn.
    assertEquals(look(moved).boardWarbands(p3), 2)
    assertEquals(siteWarbands(moved, peaks), 3)

    val rested = accepted(service.handle(gameId, moved.nextSequence,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    // p2's Supply of 5 plus p3's 3 unspent, capped at 7.
    assertEquals(look(rested).supply(p3), 7)
    assertEquals(look(rested).supply(p2), 5)
    assertEquals(look(rested).active, p1)

    assertEquals(look(rested).phase, Phase.Act)
    val started = accepted(service.handle(gameId, rested.nextSequence,
      MenuPlay.starting(p1, ActionRef.Travel)))
    val parked = accepted(service.handle(gameId, started.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        TravelProcedure.destinationDecisionId,
        TravelPlay.destination(otherEmpireSite)))))
    val paid = accepted(service.handle(gameId, parked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(TollRoads.decisionId,
        ChooseOneAnswer(PayOrAsk.pay)))))
    assertEquals(look(paid).pawn(p1), otherEmpireSite)
    assertEquals(look(paid).favor(p1), 0)
    assertEquals(look(paid).favor(p2), 1)

    val next = accepted(service.handle(gameId, paid.nextSequence,
      MenuPlay.starting(p1, PhaseTransitionRef.BeginRest)))
    // The Chancellor, seated second, starts the round; p1 is first player.
    assertEquals(look(next).active, p2)
    assertEquals(TurnOrder.order(Outcomes.ready(next.state)),
      Vector(p2, p3, p1))
    // Wake has nothing to ask the Chancellor, so p2's turn is in Act.
    assertEquals(look(next).phase, Phase.Act)
