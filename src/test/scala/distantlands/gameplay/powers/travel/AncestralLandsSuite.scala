package distantlands.gameplay.powers.travel

import distantlands.gameplay.powers.PowerFixture
import distantlands.gameplay.powers.legacies.LegacyActionFixture
import distantlands.model._
import distantlands.testkit.{Look, Outcomes}
import distantlands.testkit.Unwrap.accepted

/** Ancestral Lands (L08): an Imperial holder's travel to the Cradle costs
  * 1 Supply (Legacy in-play powers rulings 2 and 14; implementation ruling
  * R8). On `TravelFixture`'s board: the Cradle holds Buried Giant
  * (`cradlePlains`) and Desolate Shore (`coast`); the Provinces Dunes
  * (`provincePlains`), Broken Peaks (`mountain`) and Narrow Pass (`pass`);
  * the Hinterland Salt Flats (`hinterlandPlains`) and Fair Isle
  * (`island`). */
class AncestralLandsSuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  /** p1, the Chancellor, at Salt Flats in the Hinterland, holding Ancestral
    * Lands active. The printed cost to the Cradle is 4. */
  private val imperialInHinterland: ReadyGame =
    on(board())(_.imperial(actor).pawn(actor, hinterlandPlains)
      .legacy(actor, AncestralLandsCard))

  /** The same with p1 an Exile. */
  private val exileInHinterland: ReadyGame =
    on(board())(_.pawn(actor, hinterlandPlains)
      .legacy(actor, AncestralLandsCard))

  /** p1, the Chancellor, at Broken Peaks, a Mountain in the Provinces,
    * holding it. The printed cost to the Cradle is 2. */
  private val imperialOnMountain: ReadyGame =
    on(board())(_.imperial(actor).pawn(actor, mountain)
      .legacy(actor, AncestralLandsCard))

  test("an Imperial holder reaches each Cradle site for 1 Supply"):
    val costs = candidates(imperialInHinterland)
    assertEquals(costs.get(cradlePlains), Some(1))
    assertEquals(costs.get(coast), Some(1))
    val done = accepted(travel(imperialInHinterland, cradlePlains))
    assertEquals(Look(Outcomes.ready(done.state)).supply(actor), 7 - 1)

  test("from a Mountain route too"):
    assertEquals(candidates(imperialOnMountain).get(cradlePlains), Some(1))

  test("it never raises a cost another power made lower: Special Envoy " +
      "still spends none"):
    val envoy = on(imperialInHinterland)(_.adviser(actor, SpecialEnvoyCard))
    assertEquals(candidates(envoy, Vector(SpecialEnvoy.id)).get(cradlePlains),
      Some(0))

  test("an Exile holder pays the printed cost (ruling 2)"):
    assertEquals(candidates(exileInHinterland).get(cradlePlains), Some(4))

  test("travel outside the Cradle is unchanged"):
    assertEquals(candidates(imperialOnMountain).get(provincePlains),
      candidates(on(board())(_.imperial(actor).pawn(actor, mountain)))
        .get(provincePlains))

  test("a boxed lineage's Ancestral Lands changes nothing (ruling 1)"):
    assertEquals(candidates(LegacyActionFixture.boxed(imperialInHinterland,
      actor)).get(cradlePlains), Some(4))

  test("a dormant Ancestral Lands changes nothing (ruling 1)"):
    val dormant = on(board())(_.imperial(actor).pawn(actor, hinterlandPlains)
      .legacy(actor, AncestralLandsCard, active = false))
    assertEquals(candidates(dormant).get(cradlePlains), Some(4))
