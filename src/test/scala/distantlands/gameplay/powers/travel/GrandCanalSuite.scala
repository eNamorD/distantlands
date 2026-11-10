package distantlands.gameplay.powers.travel

import distantlands.gameplay.powerresolver.{ContributingPower,
  ContributionCollector, PowerCtx, PowerWindowIndex}
import distantlands.gameplay.cards.DesolateShoreSite
import distantlands.gameplay.powers.PowerFixture
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Outcomes}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Grand Canal and Lost Lagoon (E03, catalog batch 10, slice 5): the site
  * of the face in play also has the Coast power (the Canal) or the Island
  * power, which has the Coast power too (the Lagoon, ruling 12). */
class GrandCanalSuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  private val canal = GrandCanalCard.id

  /** The Travel board with the actor's pawn moved to `from`. The board's
    * own `source` would reorder its regions for a named site. */
  private def boardWithPawnAt(from: SiteId): ReadyGame =
    on(board())(_.pawn(actor, from))

  /** The Travel board with the actor at `from` and E03's `side` at `at`. */
  private def boardWithE03(side: EdificeSide, at: SiteId, from: SiteId)
      : ReadyGame =
    on(boardWithPawnAt(from))(_.edifice(canal, side, at = at))

  /** The ids gathered at `TravelCost` for the actor's pawn moving `from`
    * `to` at `ready`, among `candidates` only. */
  private def travelCostPowersGathered(ready: ReadyGame, from: SiteId,
      to: SiteId, candidates: Vector[ContributingPower]): Vector[PowerId] =
    val node = Sequence(Vector(
      SpendSupply(actor, 2),
      Move(Piece.Pawn(actor), PositionedLocation(Location.Site(from)),
        PositionedLocation(Location.Site(to)))), Some(PowerWindow.TravelCost))
    ContributionCollector.gather(PowerWindow.TravelCost,
      PowerWindowIndex.of(candidates), power => PowerCtx(catalog, ready,
        actor, power.source, PowerWindow.TravelCost, Vector.empty, node))
      .order

  test("a Coast reaches the Canal's site for 1 Supply, and the journal " +
      "replays and round trips"):
    // The printed Cradle-to-Hinterland 4.
    assertEquals(candidates(boardWithPawnAt(coast)).get(hinterlandPlains),
      Some(4))
    val ready = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = coast)
    assertEquals(candidates(ready).get(hinterlandPlains), Some(1))
    val done = accepted(travel(ready, hinterlandPlains))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).player(actor).pawnSite, Some(hinterlandPlains))
    assertEquals(Look(result).supply(actor), 7 - 1)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), result)
    assertWireRoundTrips(done.events)

  test("from a plain site a Canal at a Mountain is still behind the Narrow " +
      "Pass, and still pays the Mountain's Supply"):
    val blocked = boardWithE03(EdificeSide.Intact, at = mountain,
      from = cradlePlains)
    assertEquals(candidates(blocked).get(mountain), None)
    // p1 rules the Pass: the printed 2 and the Mountain's 1.
    val ruled = on(blocked)(_.warbandsAt(pass, actor, 1))
    assertEquals(candidates(ruled).get(mountain), Some(3))

  test("from a plain site the Canal's site keeps its printed cost: a Coast " +
      "adds nothing to arrive"):
    val ready = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = cradlePlains)
    assertEquals(candidates(ready).get(hinterlandPlains), Some(4))

  test("the Canal's site reaches a Coast or an Island for 1 Supply, and a " +
      "plain site at its printed cost"):
    val costs = candidates(boardWithE03(EdificeSide.Intact,
      at = hinterlandPlains, from = hinterlandPlains))
    // The printed 4.
    assertEquals(costs.get(coast), Some(1))
    // The printed 3 and the Island's 2, both skipped.
    assertEquals(costs.get(island), Some(1))
    assertEquals(costs.get(secondHinterlandPlains), Some(3))
    assertEquals(costs.get(cradlePlains), Some(4))

  test("a Canal at a Mountain: a Coast route skips the Mountain's Supply " +
      "and the Narrow Pass"):
    // The Pass blocks the Provinces to a player who does not rule it.
    assertEquals(candidates(boardWithPawnAt(coast)).get(mountain), None)
    val ready = boardWithE03(EdificeSide.Intact, at = mountain, from = coast)
    // The printed 2 and the Mountain's 1, both skipped.
    assertEquals(candidates(ready).get(mountain), Some(1))
    val result = Outcomes.ready(accepted(travel(ready, mountain)).state)
    assertEquals(Look(result).player(actor).pawnSite, Some(mountain))
    assertEquals(Look(result).supply(actor), 7 - 1)

  test("Lost Lagoon's site costs 2 more Supply to reach, except from a " +
      "Coast, and the journal replays and round trips"):
    val fromPlain = boardWithE03(EdificeSide.Ruined, at = hinterlandPlains,
      from = cradlePlains)
    // The printed 4 and the Island's 2.
    assertEquals(candidates(fromPlain).get(hinterlandPlains), Some(6))
    val done = accepted(travel(fromPlain, hinterlandPlains))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).supply(actor), 7 - 6)
    assertEquals(ActPowerHarness.replayed(rules, fromPlain, done.events),
      result)
    assertWireRoundTrips(done.events)
    val fromCoast = boardWithE03(EdificeSide.Ruined, at = hinterlandPlains,
      from = coast)
    assertEquals(candidates(fromCoast).get(hinterlandPlains), Some(1))
    // The Coast power drops the add, whatever order the transforms fold in.
    assert(!travelCostPowersGathered(fromCoast, coast, hinterlandPlains,
      Vector(LostLagoon, CoastSitePower(DesolateShoreSite.coast, coast)))
      .contains(LostLagoon.id))

  test("a Lagoon at a printed Island adds the Island's 2 Supply once " +
      "(ruling 13)"):
    val costs = candidates(boardWithE03(EdificeSide.Ruined, at = island,
      from = hinterlandPlains))
    // The printed 3 and one Island's 2.
    assertEquals(costs.get(island), Some(5))

  test("Lost Lagoon has the Coast power: its site reaches a Coast or an " +
      "Island for 1 Supply (ruling 12)"):
    val costs = candidates(boardWithE03(EdificeSide.Ruined,
      at = hinterlandPlains, from = hinterlandPlains))
    assertEquals(costs.get(coast), Some(1))
    assertEquals(costs.get(island), Some(1))
    assertEquals(costs.get(secondHinterlandPlains), Some(3))

  test("a Lagoon at a Mountain adds both from a plain site, and neither " +
      "from a Coast"):
    // p1 rules the Pass, so the Provinces are open from the Cradle.
    val fromPlain = on(boardWithE03(EdificeSide.Ruined, at = mountain,
      from = cradlePlains))(_.warbandsAt(pass, actor, 1))
    // The printed 2, the Mountain's 1 and the Island's 2.
    assertEquals(candidates(fromPlain).get(mountain), Some(5))
    val fromCoast = boardWithE03(EdificeSide.Ruined, at = mountain,
      from = coast)
    // A Coast route: no Pass, no Mountain, no Island add.
    assertEquals(candidates(fromCoast).get(mountain), Some(1))

  test("ruining the Canal mid-game turns its site from a Coast into an " +
      "Island at once"):
    val intact = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = cradlePlains)
    assertEquals(candidates(intact).get(hinterlandPlains), Some(4))
    val ruined = on(intact)(_.edifice(canal, EdificeSide.Ruined,
      at = hinterlandPlains))
    assertEquals(candidates(ruined).get(hinterlandPlains), Some(6))

  test("Forest Paths ignores both faces when a beast card stands at the " +
      "destination"):
    val canalRoute = on(boardWithE03(EdificeSide.Intact,
      at = hinterlandPlains, from = hinterlandPlains))(
      _.denizen(ErrandBoyCard.id, at = island))
    assert(travelCostPowersGathered(canalRoute, hinterlandPlains, island,
      Vector(GrandCanal)).contains(GrandCanal.id))
    val withPaths = travelCostPowersGathered(canalRoute, hinterlandPlains,
      island, Vector(GrandCanal, ForestPaths))
    assert(withPaths.contains(ForestPaths.id))
    assert(!withPaths.contains(GrandCanal.id))

    val lagoonRoute = on(boardWithE03(EdificeSide.Ruined,
      at = hinterlandPlains, from = cradlePlains))(
      _.denizen(ErrandBoyCard.id, at = hinterlandPlains))
    assert(travelCostPowersGathered(lagoonRoute, cradlePlains,
      hinterlandPlains, Vector(LostLagoon)).contains(LostLagoon.id))
    assert(!travelCostPowersGathered(lagoonRoute, cradlePlains,
      hinterlandPlains, Vector(LostLagoon, ForestPaths))
      .contains(LostLagoon.id))
