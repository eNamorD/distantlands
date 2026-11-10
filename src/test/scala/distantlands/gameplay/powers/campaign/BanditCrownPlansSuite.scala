package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.CampaignFixture._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.model._
import oathdigital.testkit.Look

/** Bandit Crown's battle plans (catalog batch 11, ruling 3): the holder
  * uses the plans of cards at bandit sites in any Campaign, and a bandit
  * defender still applies them, so one plan may be used twice. Traveling
  * Doctor is free, and is discarded after a Campaign its user lost. */
class BanditCrownPlansSuite extends munit.FunSuite:
  private val card = TravelingDoctorCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)

  /** p1 attacks the bandits at the origin, where the Doctor stands. */
  private def doctorWithTheDefendingBandits(crown: Boolean): Board =
    val base = board(warbands = 5, supply = 7)
    val b = on(base)(_.denizen(card, at = base.origin))
    if crown then withRelic(b, BanditCrownCard) else b

  /** p1 attacks the rival at the origin; the Doctor stands at another
    * bandit site. */
  private def doctorAtAnotherBanditSite(crown: Boolean): Board =
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(extras = 1, warbands = 5, supply = 7))
    val b = on(base)(_.denizen(card, at = base.extras.head))
    if crown then withRelic(b, BanditCrownCard) else b

  test("the holder attacks the bandits with their plan, and the bandits " +
      "apply it too"):
    val b = doctorWithTheDefendingBandits(crown = true)
    val committed = commit(rules(winningDice), b, 4)
    assert(committed.offered(b.actor).contains(ref))
    val done = committed.pick(b.actor, CampaignIds.attackerPlan, ref)
      .finishPlanWindows
    assertEquals(done.winner, Some(true))
    // Only the losing bandits' application discards the Doctor.
    assert(Look(done.state).inRegionalDiscard(card))

  test("the holder uses a bandit card's plan against a player"):
    val b = doctorAtAnotherBanditSite(crown = true)
    val committed = commit(rules(losingDice), b, 4)
    assert(committed.offered(b.actor).contains(ref))
    val done = committed.pick(b.actor, CampaignIds.attackerPlan, ref)
      .finishPlanWindows
    assertEquals(done.winner, Some(false))
    // The Doctor spared p1's force.
    assertEquals(Look(done.state).player(b.actor).board.warbands, 5)

  test("a defending holder uses a bandit card's plan too"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(extras = 1, warbands = 5, supply = 7))
    val b = withRelicFor(on(base)(_.denizen(card, at = base.extras.head)),
      base.rival, BanditCrownCard)
    val committed = commit(rules(winningDice), b, 4)
      .finishAttackerPlans(b.actor)
    assert(committed.offered(b.rival).contains(ref))
    val done = committed.pick(b.rival, CampaignIds.defenderPlan, ref)
      .finishPlanWindows
    assertEquals(done.winner, Some(true))
    // The defeated holder used it, so it is discarded.
    assert(Look(done.state).inRegionalDiscard(card))

  test("without the Crown, neither plan is offered"):
    Vector(doctorWithTheDefendingBandits(crown = false),
        doctorAtAnotherBanditSite(crown = false)).foreach { b =>
      val committed = commit(rules(losingDice), b, 4)
      assert(!committed.offered(b.actor).contains(ref))
    }
