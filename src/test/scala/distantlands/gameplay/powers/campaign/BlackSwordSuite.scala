package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.Look

/** Black Sword: an attacker burns two favor for five attack dice. */
class BlackSwordSuite extends munit.FunSuite:
  private val relic = BlackSwordCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Relic(relic)

  test("an attacker burns two favor for five attack dice"):
    val held = withRelic(board(warbands = 5, supply = 7), relic)
    val b = on(held)(_.favor(held.actor, 2))
    val run = commit(rules(winningDice), b, 2)
    assert(awaits(run, b.actor, CampaignIds.attackerPlan))
    val picked = run.pick(b.actor, CampaignIds.attackerPlan, ref)
    assert(picked.ops.contains(ModifyDicePool(CampaignIds.attackPool, 5)))
    assertEquals(Look(picked.state).favor(b.actor), 0)

  test("an attacker with one favor is not offered it"):
    val held = withRelic(board(warbands = 5, supply = 7), relic)
    val b = on(held)(_.favor(held.actor, 1))
    assert(awaits(commit(rules(winningDice), b, 2), b.actor,
      CampaignIds.sacrifice))

  test("a defender is not offered it"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = on(withRelicFor(base, base.rival, relic))(
      _.favor(base.rival, 2))
    val run = commit(rules(winningDice), b, 2)
    // The defender holds the title, so its defense is offered and the choice
    // is asked; Black Sword is not among the options.
    assert(awaits(run, b.rival, CampaignIds.defenderPlan))
    assert(!run.offered(b.actor).contains(ref))
