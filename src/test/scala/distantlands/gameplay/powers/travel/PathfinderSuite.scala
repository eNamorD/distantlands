package distantlands.gameplay.powers.travel

import distantlands.gameplay.powers.PowerFixture
import distantlands.model._
import distantlands.testkit.{Look, Outcomes}
import distantlands.testkit.Unwrap.accepted

/** Pathfinder (L36): its holder's travel ignores the Mountain, Island and
  * Pass powers (Legacy in-play powers ruling 15; implementation rulings R6
  * and R8). On `TravelFixture`'s board: the Cradle holds Buried Giant
  * (`cradlePlains`, p1's site) and Desolate Shore; the Provinces Dunes
  * (`provincePlains`), Broken Peaks (`mountain`) and Narrow Pass (`pass`,
  * ruled by bandits); the Hinterland Salt Flats (`hinterlandPlains`) and
  * Fair Isle (`island`). From the Cradle the printed base is 2 to the
  * Provinces and 4 to the Hinterland. */
class PathfinderSuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  /** p1 at Buried Giant holding Pathfinder active. */
  private val holder: ReadyGame =
    on(board())(_.legacy(actor, PathfinderCard))
  /** The same board without the legacy. */
  private val without: ReadyGame = board()
  /** p1 holding Pathfinder dormant. */
  private val dormant: ReadyGame =
    on(board())(_.legacy(actor, PathfinderCard, active = false))

  /** `table` with p1 ruling Narrow Pass, so the Pass lets p1 into the
    * Provinces and only the Mountain's add is left to compare. */
  private def passRuled(table: ReadyGame): ReadyGame =
    on(table)(_.warbandsAt(pass, actor, 1))

  /** Lost Lagoon (E03, ruined) at Salt Flats, with or without the legacy. */
  private def lagoon(table: ReadyGame): ReadyGame =
    on(table)(_.edifice(GrandCanalCard.id, EdificeSide.Ruined,
      at = hinterlandPlains))

  test("travel to a Mountain costs no Mountain add"):
    assertEquals(candidates(passRuled(without)).get(mountain), Some(3))
    assertEquals(candidates(passRuled(holder)).get(mountain), Some(2))
    val done = accepted(travel(holder, mountain))
    assertEquals(Look(Outcomes.ready(done.state)).supply(actor), 7 - 2)

  test("travel to an Island costs no Island add"):
    assertEquals(candidates(without).get(island), Some(6))
    assertEquals(candidates(holder).get(island), Some(4))

  test("Lost Lagoon's Island add is ignored too (R6)"):
    assertEquals(candidates(lagoon(without)).get(hinterlandPlains), Some(6))
    assertEquals(candidates(lagoon(holder)).get(hinterlandPlains), Some(4))

  test("leaving Lost Lagoon's site by a Coast route keeps its Coast power " +
      "(R15)"):
    // p1 at Salt Flats, Lost Lagoon's site, an Island with the Coast power.
    def fromLagoon(table: ReadyGame) = on(lagoon(table))(
      _.pawn(actor, hinterlandPlains))
    assertEquals(candidates(fromLagoon(without)).get(coast), Some(1))
    assertEquals(candidates(fromLagoon(holder)).get(coast), Some(1))
    assertEquals(candidates(fromLagoon(without)).get(island), Some(1))
    assertEquals(candidates(fromLagoon(holder)).get(island), Some(1))

  test("a Pass the holder does not rule does not stop their travel into " +
      "its region"):
    assertEquals(candidates(without).get(provincePlains), None)
    assertEquals(candidates(holder).get(provincePlains), Some(2))
    assert(travel(holder, provincePlains).isRight)

  test("the Pass still limits a campaign's targets with Pathfinder held"):
    // `CampaignConsentFixture`: p1 at Ancient City, in the Cradle, starts a
    // Conquest of the bandits; the bandit-held Narrow Pass, in the
    // Provinces, forbids Deep Woods there as an extra target.
    import distantlands.gameplay.CampaignConsentFixture.{deepWoodsRef,
      passHeldByBandits, started, targetOptions}
    val withPathfinder = on(passHeldByBandits)(
      _.legacy(actor, PathfinderCard))
    val woods = targetOptions(started(withPathfinder))
      .find(_.ref == deepWoodsRef)
    assert(woods.exists(!_.available), woods.toString)

  test("a dormant Pathfinder ignores nothing (ruling 1)"):
    assertEquals(candidates(passRuled(dormant)).get(mountain), Some(3))
    assertEquals(candidates(dormant).get(provincePlains), None)
