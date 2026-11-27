package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.Look

/** Cracking Ground: a plan costing one burnt secret, for one attack die per
  * targeted site, added or removed. */
class CrackingGroundSuite extends munit.FunSuite:
  private val card = CrackingGroundCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)

  test("an attacker burns a secret and adds one attack die per targeted " +
      "site"):
    val b = withSecrets(withAdviser(board(extras = 1, warbands = 5,
      supply = 7), card, Orientation.FaceUp), 1)
    val run = commit(rules(winningDice), b, 2,
      targets = Vector(DecisionOptionRef.Site(b.extras.head)))
    val picked = run.pick(b.actor, CampaignIds.attackerPlan, ref)
    // Two targeted sites: the origin plus the extra site.
    assert(
      picked.since(run).contains(ModifyDicePool(CampaignIds.attackPool, 2)))
    assert(picked.since(run).exists {
      case pay: PayCost => pay.player == b.actor &&
        pay.cost == Cost(secretBurnt = 1)
      case _ => false })
    assertEquals(Look(picked.state).faceUpSecrets(b.actor), 0)
    // Burnt, so nothing rests on the card.
    assertEquals(Look(picked.state).tokensOn(card), Tokens.empty)

  test("an attacker with no faceup secret cannot choose it"):
    val b = withSecrets(withAdviser(board(extras = 1, warbands = 5,
      supply = 7), card, Orientation.FaceUp), 0)
    val run = commit(rules(winningDice), b, 2,
      targets = Vector(DecisionOptionRef.Site(b.extras.head)))
    assert(!run.offers(b.actor, b.actor, CampaignIds.attackerPlan, ref))

  test("it is not offered in a Raid, which targets no site"):
    val b = withSecrets(withAdviser(withEnemyAtOrigin(board(warbands = 5,
      supply = 7)), card, Orientation.FaceUp), 1)
    assert(commit(rules(winningDice), b, 2).offers(b.actor, b.actor,
      CampaignIds.attackerPlan, ref))
    assert(!commit(rules(winningDice), b, 2, raid = true)
      .offers(b.actor, b.actor, CampaignIds.attackerPlan, ref))

  test("a defender removes one attack die per targeted site"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = on(withAdviserFor(base, base.rival, card, Orientation.FaceUp))(
      _.secrets(base.rival, faceUp = 1))
    val run = commit(rules(winningDice), b, 3)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
    assert(run.ops.contains(ModifyDicePool(CampaignIds.attackPool, -1)))

  test("a bandit defender, which pays nothing, does not apply it"):
    val base = board(extras = 1, warbands = 5, supply = 7)
    val b = withSiteCard(base, base.origin, card)
    val done = commit(rules(winningDice), b, 4,
      targets = Vector(DecisionOptionRef.Site(b.extras.head))).finishPlanWindows
    assert(!done.ops.contains(ModifyDicePool(CampaignIds.attackPool, -2)))
