package distantlands.gameplay.setup

import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.model._

/** Foundation III's deal of the legacy deck, on both faces (Legacies
  * design, slice 2, rulings 3-5; catalog batch 13, ruling 6): the public
  * row, and the rest the game keeps as its deck. */
class LegacyDealSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val seats = FirstGameSetupFixture.participants
  private val p1 = seats.head.playerId
  private val order = catalog.legacies.map(_.id)

  private def withFace(face: FoundationFace): Chronicle =
    FirstGameSetupFixture.chronicle.copy(foundations = Map(
      FoundationNumber.III -> FoundationState(face, Set.empty)))

  test("Quiet Ambitions deals no public row and leaves the whole order " +
      "as the deck"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Normal), seats)
    assertEquals(deal.publicRow(order), Vector.empty)
    assertEquals(deal.rest(order), order)

  test("Public Ambitions deals the first 4 faceup and leaves the rest"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Altered), seats)
    assertEquals(deal.publicRow(order), order.take(4))
    assertEquals(deal.rest(order), order.drop(4))

  test("a Chronicle that lists no Foundation III deals Quiet Ambitions"):
    assertEquals(LegacyDeal.of(FirstGameSetupFixture.chronicle, seats).face,
      FoundationFace.Normal)

  test("a short deck deals what remains to the public row"):
    val short = order.take(3)
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .publicRow(short), short)
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .rest(short), Vector.empty)

  test("the deck is every legacy no lineage holds, seated or in the box"):
    val held = FirstGameSetupFixture.chronicle.copy(lineages = Vector(
      LineageState(LineageId("l1"), None, Role.Exile,
        Vector(LegacyState(SeerCard.id, active = true)), Vector.empty),
      LineageState(LineageId("boxed"), None, Role.Citizen,
        Vector(LegacyState(RivalCard.id, active = false),
          LegacyState(ArbiterCard.id, active = true)), Vector.empty)))
    assertEquals(LegacyDeal.deck(catalog, held),
      order.filterNot(Set(SeerCard.id, RivalCard.id, ArbiterCard.id)))
