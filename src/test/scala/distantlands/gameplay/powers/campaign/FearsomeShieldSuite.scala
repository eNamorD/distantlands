package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.Look

/** Fearsome Shield: a defender burns two favor for two more defense
  * dice, and nothing is placed on the relic.
  */
class FearsomeShieldSuite extends munit.FunSuite:
  private val relic = FearsomeShieldCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Relic(relic)

  private def defending(favor: Int): Board =
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    on(withRelicFor(base, base.rival, relic))(
      _.favor(base.rival, favor))

  test("a defender burns two favor and adds two defense dice"):
    val b = defending(3)
    val run = commit(rules(losingDice), b, 4)
    assert(awaits(run, b.rival, CampaignIds.defenderPlan))
    val picked = run.pick(b.rival, CampaignIds.defenderPlan, ref)
    // The dice-pool change and the PayCost, inside the pick's record.
    assertEquals(picked.since(run).toSet, Set[CoreOperation](
      OpenPick(CampaignIds.defenderPlan, ref), ClosePick(),
      ModifyDicePool(CampaignIds.defensePool, 2),
      PayCost(b.rival, Location.OnCard(relic), Cost(favorBurnt = 2),
        intoOccupied = true)))
    assertEquals(Look(picked.state).favor(b.rival), 1)
    // Nothing rests on the relic.
    assertEquals(Look(picked.state).player(b.rival).relics.map(_.tokens),
      Vector(Tokens.empty))

  test("the pick's record holds the two favor it burnt"):
    val b = defending(3)
    val picked = commit(rules(losingDice), b, 4)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
    assertEquals(distantlands.testkit.Outcomes.ready(picked.state).game.current
      .walkerPicks, Vector(PlayedPick(CampaignIds.defenderPlan, ref, None,
        OptionPrice(favorBurnt = 2), open = false)))

  test("with fewer than two favor the defender cannot pay, and the " +
      "plan is not offered"):
    val b = defending(1)
    val run = commit(rules(losingDice), b, 4)
    // Only the title's plan is left to choose.
    assert(awaits(run, b.rival, CampaignIds.defenderPlan))
    assert(!run.offered(b.actor).contains(ref))
    val done = run.finishPlanWindows
    assertEquals(Look(done.state).favor(b.rival), 1)
    assert(!done.ops.exists(_.isInstanceOf[PayCost]))

  test("the relic must be faceup, and it is not the attacker's plan"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val up = on(withRelicFor(base, base.rival, relic))(
      _.favor(base.rival, 3))
    // Faceup, the same board offers it; facedown, it does not.
    assert(commit(rules(losingDice), up, 4).offered(up.actor).contains(ref))
    val down = on(up)(_.relic(up.rival, relic, facedown = true))
    val facedown = commit(rules(losingDice), down, 4)
    assert(!facedown.offered(down.actor).contains(ref))
    assertEquals(facedown.ops.count(_.isInstanceOf[PayCost]), 0)
    val held = withRelic(board(warbands = 5, supply = 7), relic)
    val attacker = on(held)(_.favor(held.actor, 3))
    assert(awaits(commit(rules(losingDice), attacker, 2), attacker.actor,
      CampaignIds.sacrifice))
