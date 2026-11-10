package oathdigital.model

class PlanAidStateSuite extends munit.FunSuite:
  private val p1 = PlayerId("p1")
  private val p2 = PlayerId("p2")
  private val p3 = PlayerId("p3")
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)
  private val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
  private val twoFavor = AidOffer(amounts = AidAmounts(favor = 2))

  test("a saved offer replaces the ally's earlier offer in the same window"):
    val first = PlanAidState().saved(p2, defending, twoFavor)
    val second = first.saved(p2, defending, AidOffer())
    assertEquals(second.offerOf(p2), AidOffer())
    assertEquals(second.window, Some(defending))

  test("an offer for another window starts the state afresh"):
    val state = PlanAidState().saved(p2, attacking, twoFavor)
      .saved(p2, defending, AidOffer())
    assertEquals(state.window, Some(defending))
    assertEquals(state.offers, Map(p2 -> AidOffer()))

  test("amounts subtract down to zero and compare part by part"):
    val offered = AidAmounts(favor = 2, secrets = 1)
    assertEquals(offered - AidAmounts(favor = 3), AidAmounts(secrets = 1))
    assert(offered.atLeast(AidAmounts(favor = 2)))
    assert(!offered.atLeast(AidAmounts(favorBurnt = 1)))
    assertEquals(AidAmounts.of(Cost(favor = 1, secretBurnt = 2)),
      AidAmounts(favor = 1, secretsBurnt = 2))
