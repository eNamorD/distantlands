package distantlands.serialization

import distantlands.model._
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.walker.{DeltaPayload, WalkerStepRecorded}
import distantlands.serialization.WalkerWireEvents.{
  encodedStreamThatRoundTrips, recordedOperation}
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.accepted

/** Walker steps carrying resource operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class ResourceOperationWireSuite extends munit.FunSuite:
  test("reduced optional spend is canonical in memory and on the wire"):
    val withOneSupply = Table.start.supply(Table.p1, 1).ready
    val run = accepted(OperationPipeline.run(withOneSupply,
      Vector(SpendSupply(Table.p1, 3, required = false)),
      OperationPolicy.Permissive, Vector.empty)(Right(_)))
    val event = WalkerStepRecorded("0", DeltaPayload(run.executed), Vector.empty)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assert(!ujson.read(encoded)("payload")("step")("ops")(0).obj.contains("required"))

  test("a burn's burner round trips, and a burn no player makes writes " +
      "null"):
    val red = PlayerId("red")
    val burns = Vector[CoreOperation](
      Burn.favor(2, PositionedLocation(Location.PlayArea(red)), Some(red)),
      Burn.favor(1, PositionedLocation(Location.FavorBank(Suit.Order)),
        None))
    val events = burns.map(burn => recordedOperation(burn))
    val encoded = encodedStreamThatRoundTrips("burn-by", events)
    val ops = ujson.read(encoded).arr.map(_("payload")("step")("ops")(0))
    assertEquals(ops(0)("by").str, "red")
    assertEquals(ops(1)("by"), ujson.Null)

  test("a draw from the Nomad bank round trips"):
    val event = recordedOperation(DrawNomadFavor(PlayerId("p1"), 2))
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0)("kind").str,
      "draw-nomad-favor")
