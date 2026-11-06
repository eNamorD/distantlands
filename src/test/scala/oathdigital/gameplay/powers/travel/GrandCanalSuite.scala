package oathdigital.gameplay.powers.travel

import oathdigital.gameplay.powers.PowerFixture
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, Outcomes}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Unwrap.accepted

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
