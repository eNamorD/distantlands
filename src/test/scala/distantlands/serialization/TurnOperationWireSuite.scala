package distantlands.serialization

import distantlands.gameplay.cards.DeepWoodsSite
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.model._
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.walker.WalkerStepRecorded
import distantlands.serialization.WalkerWireEvents.recordedOperation
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Unwrap.{accepted, as}

/** Walker steps carrying turn operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class TurnOperationWireSuite extends munit.FunSuite:
  test("recorded Visions Drawn advancement round trips without an amount " +
      "and replays as one more Vision drawn"):
    val event = recordedOperation(AdvanceVisionsDrawn)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "advance-visions-drawn"))
    val decoded = as[WalkerStepRecorded](
      accepted(GameEventWire.decode(encoded)).event)
    val base = Table.start.ready
    val replayed = accepted(OperationPipeline.run(base, RecordedOps.of(decoded),
      OperationPolicy.Permissive, Vector.empty)(Right(_))).state
    assertEquals(replayed.game.current.tracks.visionsDrawn,
      base.game.current.tracks.visionsDrawn + 1)

  test("a recorded round advance round trips and replays as one more round"):
    val event = recordedOperation(AdvanceRound)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "advance-round"))
    val decoded = as[WalkerStepRecorded](
      accepted(GameEventWire.decode(encoded)).event)
    val base = Table.start.roundMarkerAt(4).ready
    val replayed = accepted(OperationPipeline.run(base, RecordedOps.of(decoded),
      OperationPolicy.Permissive, Vector.empty)(Right(_))).state
    assertEquals(replayed.game.current.tracks.round, 5)

  test("a journalled phase change round-trips the phase it names"):
    // `EnterPhase` is only ever declared with `Act` today (End Wake is its
    // one caller), so a codec that ignored the value and decoded `Act`
    // unconditionally would pass every other test in the tree. Rest is used
    // here for exactly that reason.
    val event = recordedOperation(EnterPhase(Phase.Rest))
    val encoded = accepted(GameEventWire.encodeEvent("phase", 0L, event))
    assertEquals(encoded("payload")("step")("ops")(0)("phase").str, "rest")
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)

  test("a deferral and its take round trip, with the held ops"):
    val key = DeferralKey("test.held")
    val held = Vector[CoreOperation](ModifyDicePool(PoolKey("test.pool"), 2),
      Gain.Secrets(Table.p1, 1))
    Vector[CoreOperation](Defer(key, held), TakeDeferred(key)).foreach { op =>
      val event = recordedOperation(op)
      val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
      assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    }
    val deferral = accepted(GameEventWire.encodeEvent("walker", 0,
      recordedOperation(Defer(key, held))))
    assertEquals(ujson.read(deferral)("payload")("step")("ops")(0)("kind").str,
      "defer")

  test("a pick record's opening, with and without a label, and its closing " +
      "round trip"):
    Vector[CoreOperation](
      OpenPick("campaign.defender-plan", DecisionOptionRef.Button("title"),
        Some("Oathkeeper title: add 1 defense die")),
      OpenPick("search.modifiers",
        DecisionOptionRef.Denizen(WrestlersCard.id)),
      ClosePick()).foreach { op =>
      val event = recordedOperation(op)
      val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
      assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    }

  test("an as-if site round trips"):
    val event = recordedOperation(ActAsIf(DeepWoodsSite.id))
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0)("kind").str,
      "act-as-if")

  test("an as-if adviser holder round trips"):
    val event = recordedOperation(ActAsIfAdvisers(PlayerId("p2")))
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0)("kind").str,
      "act-as-if-advisers")
