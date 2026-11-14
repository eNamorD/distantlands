package distantlands.gameplay.setup

import distantlands.catalog.holding.{ArbiterCard, SeerCard}
import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.model._

/** Foundation III's deal of the derived legacy deck, on both faces
  * (Legacies design, slice 2, rulings 3-5). */
class LegacyDealSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val seats = FirstGameSetupFixture.participants
  private val Vector(p1, p2, p3) = seats.map(_.playerId)
  private val order = catalog.legacies.map(_.id)

  private def withFace(face: FoundationFace): Chronicle =
    FirstGameSetupFixture.chronicle.copy(foundations = Map(
      FoundationNumber.III -> FoundationState(face, Set.empty)))

  test("Quiet Ambitions draws 2 for each player in seat order and deals " +
      "no public row"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Normal), seats)
    assertEquals(seats.map(seat => deal.drawn(order, seat.playerId)),
      Vector(order.slice(0, 2), order.slice(2, 4), order.slice(4, 6)))
    assertEquals(deal.publicRow(order), Vector.empty)

  test("Public Ambitions deals the first 4 faceup and nobody draws"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Altered), seats)
    assertEquals(deal.publicRow(order), order.take(4))
    assertEquals(seats.map(seat => deal.drawn(order, seat.playerId)),
      Vector.fill(3)(Vector.empty))

  test("a Chronicle that lists no Foundation III deals Quiet Ambitions"):
    assertEquals(LegacyDeal.of(FirstGameSetupFixture.chronicle, seats).face,
      FoundationFace.Normal)

  test("a short deck draws what remains, in seat order"):
    val deal = LegacyDeal(FoundationFace.Normal, Vector(p1, p2, p3))
    val short = order.take(3)
    assertEquals(Vector(p1, p2, p3).map(deal.drawn(short, _)),
      Vector(short.take(2), short.drop(2), Vector.empty))
    assertEquals(deal.drawn(short, PlayerId("nobody")), Vector.empty)
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .publicRow(short), short)

  test("the deck is every legacy no lineage holds, seated or in the box"):
    val held = FirstGameSetupFixture.chronicle.copy(lineages = Vector(
      LineageState(LineageId("l1"), None, Role.Exile,
        Vector(LegacyState(SeerCard.id, active = true)), Vector.empty),
      LineageState(LineageId("boxed"), None, Role.Citizen,
        Vector(LegacyState(RivalCard.id, active = false),
          LegacyState(ArbiterCard.id, active = true)), Vector.empty)))
    assertEquals(LegacyDeal.deck(catalog, held),
      order.filterNot(Set(SeerCard.id, RivalCard.id, ArbiterCard.id)))
