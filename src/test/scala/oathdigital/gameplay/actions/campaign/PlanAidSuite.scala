package oathdigital.gameplay.actions.campaign

import oathdigital.testkit.CatalogNames
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** What Imperial Tactics lends a plan's user (Empire phase design,
  * "1b-ii"). The quiet table seats p1, p2 and p3; with p2 the Chancellor
  * the turn order is p2, p3, p1. */
class PlanAidSuite extends munit.FunSuite:
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)
  private val oneFavor = AidOffer(amounts = AidAmounts(favor = 1))

  private def allImperialOffering: Table = Table.start.imperial(p2, p1, p3)
    .favor(p1, 3).favor(p2, 3)
    .aid(p1, defending, oneFavor).aid(p2, defending, oneFavor)

  private def withPool(ready: ReadyGame, pool: PoolKey, n: Int): ReadyGame =
    ready.updateCurrent(current => current.copy(rollPools =
      current.rollPools.updated(pool, DicePoolState(n))))

  test("allies are the other Imperial players, in turn order after the user"):
    assertEquals(PlanAid.allies(Table.start.imperial(p2, p1, p3).ready, p3),
      Vector(p1, p2))

  test("an Exile between the user and an ally is skipped"):
    assertEquals(PlanAid.allies(Table.start.imperial(p2, p3).ready, p3),
      Vector(p2))

  test("an Exile user gets no window"):
    val ready = Table.start.imperial(p2, p1).ready
    assertEquals(PlanAid.window(ready, p3, CampaignPlanSide.Attacker, p3),
      None)
    assertEquals(PlanAid.window(ready, p3, CampaignPlanSide.Defender, p1),
      Some(AidWindow(p3, CampaignPlanSide.Defender, p1)))

  test("allies pay first, in ally order, and the user pays the rest"):
    assertEquals(PlanAid.payments(allImperialOffering.ready, Some(defending),
      p3, Cost(favor = 3)), Vector(p1 -> Cost(favor = 1),
        p2 -> Cost(favor = 1), p3 -> Cost(favor = 1)))

  test("a cost the allies cover leaves the user nothing to pay"):
    assertEquals(PlanAid.payments(allImperialOffering.ready, Some(defending),
      p3, Cost(favor = 1)), Vector(p1 -> Cost(favor = 1)))

  test("with no window the user pays everything"):
    assertEquals(PlanAid.payments(allImperialOffering.ready, None, p3,
      Cost(favor = 2)), Vector(p3 -> Cost(favor = 2)))

  test("offers saved for another window do not reach this one"):
    val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
    assertEquals(PlanAid.offers(allImperialOffering.ready, attacking),
      Vector.empty)

  test("spent aid is not offered again"):
    val after = withPool(allImperialOffering.ready,
      PlanAid.amountMarker(defending, p2, "favor"), 1)
    assertEquals(PlanAid.remaining(after, defending, p2), AidAmounts.none)
    assertEquals(PlanAid.used(after, defending, p2), AidAmounts(favor = 1))

  test("the spent markers count each part paid"):
    assertEquals(PlanAid.spent(defending, p2, Cost(favor = 1,
      secretBurnt = 2)), Vector(
        ModifyDicePool(PlanAid.amountMarker(defending, p2, "favor"), 1),
        ModifyDicePool(PlanAid.amountMarker(defending, p2, "secrets-burnt"),
          2)))

  test("allies could cover a shortfall within what they hold unoffered"):
    val ready = allImperialOffering.ready
    assert(PlanAid.couldCover(ready, defending,
      OathViolation.InsufficientFavor(2, 0)))
    assert(!PlanAid.couldCover(ready, defending,
      OathViolation.InsufficientFavor(99, 0)))
    assert(!PlanAid.couldCover(ready, defending,
      OathViolation.GameNotStarted))

  test("a lent plan's use reads back from its marker"):
    val shieldWall =
      DecisionOptionRef.Denizen(CatalogNames.denizen("Shield Wall"))
    val pool = PlanAid.lentUse(defending, p2, shieldWall) match
      case ModifyDicePool(pool, _, _) => pool
      case other => fail(s"not a pool marker: $other")
    val after = withPool(allImperialOffering.ready, pool, 1)
    assertEquals(PlanAid.usedRefs(after, defending, p2), Vector(shieldWall))
    assertEquals(PlanAid.usedRefs(after, defending, p1), Vector.empty)

  test("the Darkest Secret's holder meets it"):
    val ready = Table.start.imperial(p2, p1, p3).darkestSecret(Some(p2), 1)
      .ready
    assert(PlanAid.meets(ready, p2, PlanAid.darkestSecret))
    assert(!PlanAid.meets(ready, p1, PlanAid.darkestSecret))

  test("a faceup Yew Staff meets the Darkest Secret, a facedown one does not"):
    val faceup = Table.start.imperial(p2, p1, p3).relic(p2, "Yew Staff").ready
    val facedown = Table.start.imperial(p2, p1, p3)
      .relic(p2, "Yew Staff", facedown = true).ready
    assert(PlanAid.meets(faceup, p2, PlanAid.yewStaff))
    assert(!PlanAid.meets(facedown, p2, PlanAid.yewStaff))

  test("the People's Favor's holder meets it"):
    val ready = Table.start.imperial(p2, p1, p3).peoplesFavor(Some(p2), 1)
      .ready
    assert(PlanAid.meets(ready, p2, PlanAid.peoplesFavor))
    assertEquals(PlanAid.requirementsOf(ready, p2), Vector(
      PlanAid.peoplesFavor, DecisionOptionRef.Player(p2)))

  test("an ally on the board meets their own pawn, not another's"):
    val ready = Table.start.imperial(p2, p1, p3).ready
    assert(PlanAid.meets(ready, p2, DecisionOptionRef.Player(p2)))
    assert(!PlanAid.meets(ready, p2, DecisionOptionRef.Player(p1)))

  test("the Darkest Secret is met alone only by its holder"):
    val ready = Table.start.imperial(p2, p1, p3).darkestSecret(Some(p2), 1)
      .ready
    assert(PlanAid.meetsAlone(ready, p2, PlanNeed.DarkestSecret))
    assert(!PlanAid.meetsAlone(ready, p3, PlanNeed.DarkestSecret))

  test("an ally lends the Darkest Secret only once offered, or in full"):
    val held = Table.start.imperial(p2, p1, p3).darkestSecret(Some(p2), 1)
    val unlent = held.ready
    val lent = held.aid(p2, defending,
      AidOffer(requirements = Vector(PlanAid.darkestSecret))).ready
    assertEquals(PlanAid.lentNeed(unlent, defending, PlanNeed.DarkestSecret,
      full = false), None)
    assertEquals(PlanAid.lentNeed(unlent, defending, PlanNeed.DarkestSecret,
      full = true), Some(p2 -> PlanAid.darkestSecret))
    assertEquals(PlanAid.lentNeed(lent, defending, PlanNeed.DarkestSecret,
      full = false), Some(p2 -> PlanAid.darkestSecret))

  test("an ally lends a pawn only at the site it stands on"):
    val ready = Table.start.imperial(p2, p1, p3).ready
    val at = ready.game.current.players.find(_.player == p2)
      .flatMap(_.pawnSite).get
    assertEquals(PlanAid.lentNeed(ready, defending, PlanNeed.PawnAt(at),
      full = true), Some(p2 -> DecisionOptionRef.Player(p2)))
    assertEquals(PlanAid.lentNeed(ready, defending,
      PlanNeed.PawnAt(SiteId("nowhere")), full = true), None)

  test("a shortfall the pipeline reports by code is read against the price"):
    val ready = Table.start.imperial(p2, p1, p3).favor(p3, 0).favor(p1, 0)
      .favor(p2, 2).ready
    val rejected = OathViolation.CoreOperationRejected("insufficient-pieces",
      "player play area contains 0 of requested favor")
    assert(PlanAid.couldCover(ready, defending, rejected,
      PlanAid.printed(Vector(CampaignPlanCost.Favor(1)))))
    assert(!PlanAid.couldCover(ready, defending, rejected,
      PlanAid.printed(Vector(CampaignPlanCost.Favor(3)))))
    assert(!PlanAid.couldCover(ready, defending, rejected,
      PlanAid.printed(Vector(CampaignPlanCost.SacrificeWarband))))
