package distantlands.gameplay.setup

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** `relicOrder` fills the site relic slots from its top, and the rest, in
  * the same order, is the drawable deck: a slot takes the card a draw from
  * the deck would have taken.
  */
class FirstGameSetupMaterializerSuite extends munit.FunSuite:
  private val materializer = new FirstGameSetupMaterializer(catalog)

  test("site relic slots are filled from the top of relicOrder, leaving the " +
      "remainder -- in the same order -- as the drawable deck"):
    val sites = catalog.sites.filter(_.relicSlots > 0)
      .sortBy(_.relicSlots)(using Ordering[Int].reverse).take(3).map(_.id)
    assert(sites.nonEmpty, "fixture catalog needs at least one relic-slotted site")
    val totalSlots = sites.map(id => catalog.sites.find(_.id == id).get.relicSlots).sum
    val relicOrder = (1 to totalSlots + 5).map(i => RelicId(s"relic:test-$i")).toVector
    val plan = FirstGameSetupPlan(
      participants = Vector(
        FirstGameParticipant(PlayerId("p1"), LineageId("l1"), PlayerColor.Red)),
      firstPlayer = PlayerId("p1"),
      orderedSites = sites,
      denizenOrder = Vector.empty,
      worldDeckOrder = Vector.empty,
      relicOrder = relicOrder,
      homelandEdifices = Vector.empty,
      oathkeeperGoal = OathkeeperGoal.Supremacy)
    val material = materializer.materialize(plan,
      SetupDeal.firstGame(plan.participants))

    val placedRelics = sites.flatMap(id => material.map.sites(id).relics.map(_.id))
    assertEquals(placedRelics.toSet, relicOrder.take(totalSlots).toSet)
    assertEquals(material.commonCards.relicDeck, relicOrder.drop(totalSlots))
