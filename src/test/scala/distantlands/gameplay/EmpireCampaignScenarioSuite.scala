package distantlands.gameplay

import distantlands.application._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.{Commands, Dice, Look, MenuPlay, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** A Citizen conquers an Imperial site and leaves it to the bandits
  * (Imperial Infighting, CR p. 35), played through the service. */
class EmpireCampaignScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-campaign"
  private val peaks = BrokenPeaksSite.id

  /** p2 is the Chancellor and p3 a Citizen, active in Act at Broken Peaks,
    * which the Empire holds with 2 warbands. p3 has 4 warbands on their
    * board and 7 Supply; p2 has none on their board and stands at Buried
    * Giant with p1. p1, an Exile, holds the Oathkeeper title, so the
    * Chancellor has no title plan. */
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p3, Phase.Act).pawn(p3, at = peaks)
    .pawn(p2, at = BuriedGiantSite).pawn(p1, at = BuriedGiantSite)
    .warbandsAt(peaks, p2, 2).boardWarbands(p3, 4).boardWarbands(p2, 0)
    .supply(p3, 7).oathkeeper(Some(p1))

  private def look(at: GameAccepted): Look = Look(Outcomes.ready(at.state))

  private def resolve(service: GameApplicationService, from: GameAccepted,
      player: PlayerId, id: String, answer: DecisionAnswer): GameAccepted =
    accepted(service.handle(gameId, from.nextSequence,
      Commands.resolveWalker(player, TreeDecision(id, answer))))

  test("a Citizen conquers the Empire's site, places no warbands, and the " +
      "bandits take it"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, ActionRef.Campaign)))
    val forced = resolve(service, started, p3, CampaignIds.force,
      ChooseAmountAnswer(4))
    val sacrificed = resolve(service, forced, p3, CampaignIds.sacrifice,
      ChooseAmountAnswer(0))
    // Four swords beat the 2 purple warbands and blank defense dice. Both
    // die; one returns to the Chancellor's board.
    assertEquals(look(sacrificed).boardWarbands(p2), 1)
    assertEquals(look(sacrificed).boardWarbands(p3), 4)
    val placed = resolve(service, sacrificed, p3, CampaignIds.placement,
      ChooseAmountAnswer(0))
    val capacity = FirstGameSetupFixture.catalog.site(peaks).fold(0)(_.capacity)
    assertEquals(look(placed).forces(peaks),
      SiteForces.Occupied(ForceKind.Bandit, capacity))
    assertEquals(look(placed).boardWarbands(p3), 4)
