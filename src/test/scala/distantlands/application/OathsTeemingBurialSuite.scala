package distantlands.application

import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.setup.{LaterGameFixture, SetupDeal}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** Under Teeming World an Oaths of Loyalty holder's buried draws are named
  * to the holder alone (Legacy Chronicle powers R24, re-ruled): they lie in
  * the world deck, which nobody else knows. l1, played by p1, holds Oaths. */
class OathsTeemingBurialSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val l1 = LineageId("l1")

  test("the holder identifies the cards it buried; another seat and the " +
      "public do not"):
    val record = LaterGameFixture.chronicle(l1, LineageId("l2"))
    val chronicle = record.copy(
      foundations = record.foundations.updated(FoundationNumber.IV,
        FoundationState(FoundationFace.Altered, Set.empty)),
      lineages = record.lineages.map(lineage =>
        if lineage.id == l1 then lineage.copy(legacies =
          Vector(LegacyState(OathsOfLoyaltyCard.id, active = true)))
        else lineage))
    val orders = LaterGameFixture.orders(chronicle, p1)
    val buried = SetupDeal.of(chronicle, orders.participants)
      .buried(orders.denizenOrder, p1)
    assertEquals(buried.size, 3)
    val ready = Situation.wake(Situation.rules(catalog), chronicle, orders)
      .ready
    buried.foreach { card =>
      assert(presentation.identifiesAt(ready, Some(p1), card), card)
      Vector(Some(p2), None).foreach(viewer =>
        assert(!presentation.identifiesAt(ready, viewer, card),
          s"$viewer $card"))
    }
