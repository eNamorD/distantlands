package distantlands.serialization

import distantlands.model._
import distantlands.serialization.WalkerWireEvents.{encodedStream,
  recordedOperation}
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.accepted

/** Walker steps carrying card operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class CardOperationWireSuite extends munit.FunSuite:
  test("an actorless discard says so on the wire"):
    val discard = Discard.Denizen(DenizenId("D1"),
      PositionedLocation(Location.Site(SiteId("site"))), Region.Provinces,
      Suit.Beast, 0, 1, Table.p1, actorless = true)
    val event = recordedOperation(discard)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))

  test("a discard's cause round trips, and a discard without one writes " +
      "none"):
    val atSite = PositionedLocation(Location.Site(SiteId("site")))
    val cause = DiscardCause(PowerId("denizen.roving-terror"),
      makesRoom = true)
    val caused = Vector[CoreOperation](
      Discard.Denizen(DenizenId("D1"), atSite, Region.Provinces, Suit.Nomad,
        0, 1, Table.p1, cause = Some(cause)),
      Discard.RuinedEdifice(EdificeId("E1"), atSite, Suit.Nomad, 1, 0,
        Table.p1, cause = Some(DiscardCause(PowerId("denizen.riots")))))
    caused.foreach { discard =>
      val encoded = encodedStream("walker", Vector(recordedOperation(discard)))
      assertEquals(accepted(GameEventWire.decodeStream(encoded))
        .map(_.event), Vector(recordedOperation(discard)))
    }
    val plain = Discard.Denizen(DenizenId("D1"), atSite, Region.Provinces,
      Suit.Nomad, 0, 0, Table.p1)
    val written = ujson.read(encodedStream("walker",
      Vector(recordedOperation(plain)))).arr
    assert(!written(0)("payload")("step")("ops")(0).obj.contains("cause"))

  test("a discard's pile position round trips, and a discard that names " +
      "none, or names neither end, is refused"):
    val inHand = PositionedLocation(Location.Hand(Table.p1))
    Vector[CoreOperation](
      Discard.Denizen(DenizenId("D1"), inHand, Region.Hinterland,
        Suit.Beast, 0, 0, Table.p1, position = StackPosition.Bottom),
      Discard.Vision(VisionId("vision:v1"), inHand, Region.Cradle,
        position = StackPosition.Bottom)
    ).foreach { discard =>
      val encoded = encodedStream("walker", Vector(recordedOperation(discard)))
      def withPosition(edit: ujson.Value => Unit): String =
        val stream = ujson.read(encoded).arr
        edit(stream(0)("payload")("step")("ops")(0))
        ujson.write(stream)
      assertEquals(accepted(GameEventWire.decodeStream(encoded))
        .map(_.event), Vector(recordedOperation(discard)))
      assertEquals(GameEventWire.decodeStream(withPosition(
        _.obj.remove("position"))), Left(WireError.InvalidValue(
          "$[0].payload.step.ops[0].position",
          "a discard names the top or the bottom of its pile")))
      assertEquals(GameEventWire.decodeStream(withPosition(
        _("position") = "unspecified")), Left(WireError.InvalidValue(
          "$[0].payload.step.ops[0].position",
          "a discard goes to the top or the bottom of its pile")))
    }

  test("a swap names its mover on the wire"):
    val swap = Swap(DenizenId("D1"),
      PositionedLocation(Location.Site(SiteId("here"))), DenizenId("D2"),
      PositionedLocation(Location.Site(SiteId("there"))),
      by = Some(Table.p1))
    val event = recordedOperation(swap)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))

  test("a draw written before it had a position decodes as a draw from the " +
      "top, and a draw of no position is refused"):
    val player = PlayerId("red")
    val draw = Draw(player, Vector(DenizenId("denizen")),
      Location.RegionalDiscard(Region.Cradle), Location.Hand(player))
    val encoded = encodedStream("walker",
      Vector(recordedOperation(draw)))
    def withPosition(edit: ujson.Value => Unit): String =
      val stream = ujson.read(encoded).arr
      edit(stream(0)("payload")("step")("ops")(0))
      ujson.write(stream)
    assertEquals(ujson.read(encoded).arr(0)("payload")("step")("ops")(0)("position")
      .str, "top")
    val oldJournal = withPosition(_.obj.remove("position"))
    assertEquals(accepted(GameEventWire.decodeStream(oldJournal))
      .map(_.event), Vector(recordedOperation(draw)))
    assertEquals(GameEventWire.decodeStream(withPosition(
      _("position") = "unspecified")), Left(WireError.InvalidValue(
        "$[0].payload.step.ops[0].position",
        "a draw takes from the top or the bottom")))
