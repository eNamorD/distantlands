package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.CampaignFixture.{Board, board, on, rules}
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.model._
import oathdigital.testkit.Look
import oathdigital.testkit.Table.{p1, p2, p3}

/** When the plan window is asked (Empire phase design, "1b-ii", "The
  * window"): an Imperial user's window opens for a plan an ally could lend,
  * or one the allies could help pay for, even when the user can use
  * nothing alone. p1, an Exile, raids p3 at the bandit-held Ancient City;
  * p2 is the Chancellor. Nobody holds the Oathkeeper title. */
class ImperialTacticsWindowSuite extends munit.FunSuite:
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)

  /** p1 raids p3, who has `favor` and 2 board warbands at the origin; p2,
    * the Chancellor, has `chancellorFavor`. `citizens` are Citizens. */
  private def raidOnP3(favor: Int, chancellorFavor: Int,
      citizens: PlayerId*): Board =
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, citizens*).oathkeeper(None))
    on(b)(_.pawn(p3, at = b.origin).boardWarbands(p3, 2).favor(p3, favor)
      .favor(p2, chancellorFavor))

  private def raided(b: Board): Run =
    commit(rules(winningDice), b, 4, raid = true)

  test("a Citizen's window opens for an ally's plan though they have none"):
    val b = raidOnP3(favor = 1, chancellorFavor = 1, p3)
    val run = raided(on(b)(_.adviser(p2, ShieldWall.cardId)))
    assert(awaits(run, p3, CampaignIds.defenderPlan))

  test("a Citizen's window opens for a plan only allies can pay for"):
    val b = raidOnP3(favor = 0, chancellorFavor = 2, p3)
    val run = raided(on(b)(_.adviser(p3, ShieldWall.cardId)))
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    assert(!run.offered(p1).contains(shieldWall))

  test("a Citizen's window stays shut when no ally can help"):
    val b = raidOnP3(favor = 0, chancellorFavor = 0, p3)
    val run = raided(on(b)(_.adviser(p3, ShieldWall.cardId)))
    assert(!awaits(run, p3, CampaignIds.defenderPlan))

  test("an Exile's window is skipped as before"):
    val b = raidOnP3(favor = 0, chancellorFavor = 2)
    val run = raided(on(b)(_.adviser(p3, ShieldWall.cardId)))
    assert(!awaits(run, p3, CampaignIds.defenderPlan))

  test("the window stays open when an ally withdraws while it waits"):
    val b = raidOnP3(favor = 1, chancellorFavor = 1, p3)
    val run = raided(on(b)(_.adviser(p2, ShieldWall.cardId)
      .aid(p2, defending, AidOffer(plans = Vector(shieldWall)))))
    assert(run.offered(p1).contains(shieldWall))
    val withdrawn = run.copy(transition = run.transition.copy(state =
      run.game.evolve(run.state, OathEvent.PlanAidSaved(p2, defending,
        AidOffer())).toOption.get))
    assert(awaits(withdrawn, p3, CampaignIds.defenderPlan))
    assertEquals(withdrawn.withheld(p1, shieldWall),
      Some(ImperialTactics.awaitsPlan(p2)))
    // The parked decision resumes: Finish is still its answer.
    val finished = withdrawn.finishDefenderPlans(p3)
    assert(!awaits(finished, p3, CampaignIds.defenderPlan))

  test("a partial offer keeps the window open and the decision resumable"):
    // Hearts and Minds, the Chancellor's at an Imperial site, costs 3; p3
    // has none and p2 offers it with 1 of their 3 favor.
    val b = raidOnP3(favor = 0, chancellorFavor = 3, p3)
    val at = b.player(p2).pawnSite.get
    val heartsAndMinds = DecisionOptionRef.Denizen(HeartsAndMindsCard.id)
    val run = raided(on(b)(_.warbandsAt(at, p2, 2)
      .denizen(HeartsAndMindsCard.id, at)
      .aid(p2, defending, AidOffer(plans = Vector(heartsAndMinds),
        amounts = AidAmounts(favor = 1)))))
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    assert(!run.offered(p1).contains(heartsAndMinds))
    val raised = run.copy(transition = run.transition.copy(state =
      run.game.evolve(run.state, OathEvent.PlanAidSaved(p2, defending,
        AidOffer(plans = Vector(heartsAndMinds),
          amounts = AidAmounts(favor = 3)))).toOption.get))
    assert(raised.offered(p1).contains(heartsAndMinds))
    val picked = raised.pick(p3, CampaignIds.defenderPlan, heartsAndMinds)
    assertEquals(Look(picked.state).favor(p3), 0)

  test("a plan the attacker used keeps no window open"):
    // p1, a Citizen, uses p2's Encirclement; p3 could pay for it with p2's
    // favor, but the attacker used it.
    val encirclement = DecisionOptionRef.Denizen(EncirclementCard.id)
    val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
    val b = raidOnP3(favor = 0, chancellorFavor = 2, p1, p3)
    val run = raided(on(b)(_.adviser(p2, EncirclementCard.id)
      .aid(p2, attacking, AidOffer(plans = Vector(encirclement)))))
      .pick(p1, CampaignIds.attackerPlan, encirclement)
      .finishAttackerPlans(p1)
    assert(!awaits(run, p3, CampaignIds.defenderPlan))
