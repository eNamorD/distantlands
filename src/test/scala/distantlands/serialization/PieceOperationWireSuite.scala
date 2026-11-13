package distantlands.serialization

import distantlands.gameplay.actions.VisionRules
import distantlands.model._
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.walker.WalkerStepRecorded
import distantlands.serialization.WalkerWireEvents.{
  encodedStreamThatRoundTrips, recordedOperation}
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Unwrap.{accepted, as}

/** Walker steps carrying piece operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class PieceOperationWireSuite extends munit.FunSuite:
  test("a recorded move of a Vision to the shared bank round trips and replays"):
    val actor = Table.p1
    val card = VisionRules.Conspiracy
    val prepared = Table.start.hand(actor, card).ready
    val box = Move(Piece.Card(card), PositionedLocation(Location.Hand(actor)),
      PositionedLocation(Location.SharedBank))
    val event = recordedOperation(box)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    val decoded = as[WalkerStepRecorded](
      accepted(GameEventWire.decode(encoded)).event)
    val replayed = accepted(OperationPipeline.run(prepared, RecordedOps.of(decoded),
      OperationPolicy.Permissive, Vector.empty)(Right(_))).state
    assertEquals(replayed.game.current.temporaryHands(actor), Vector.empty)

  test("a move names its mover on the wire, and a site card's move " +
      "without one decodes as moved by nobody"):
    val move = Move(Piece.Card(RelicId("R1")),
      PositionedLocation(Location.Site(SiteId("site"))),
      PositionedLocation(Location.PlayArea(Table.p1)),
      resultingOrientation = Some(Orientation.FaceDown), by = Some(Table.p1))
    val event = recordedOperation(move)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    val stripped = ujson.read(ujson.write(encoded))
    stripped("payload")("step")("ops")(0).obj.remove("by")
    assertEquals(GameEventWire.decode(stripped).map(_.event),
      Right(recordedOperation(move.copy(by = None))))

  test("a replace names its performer on the wire, and one nobody " +
      "performs writes none (R20)"):
    val replace = Replace(Piece.Warbands(ForceKind.Bandit, 1),
      Piece.Warbands(ForceKind.Imperial, 1),
      PositionedLocation(Location.Site(SiteId("site"))), by = Some(Table.p1))
    val event = recordedOperation(replace)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    val nobody = accepted(GameEventWire.encodeEvent("walker", 0,
      recordedOperation(replace.copy(by = None))))
    assert(!ujson.read(ujson.write(nobody))("payload")("step")("ops")(0)
      .obj.contains("by"))

  test("a Take's leaving operations round trip, and a Take without them " +
      "writes none"):
    val player = PlayerId("red")
    val holder = PlayerId("blue")
    val leaving = Vector[CoreOperation](
      Move(Piece.Favor(1),
        PositionedLocation(Location.OnBanner(Banner.PeoplesFavor)),
        PositionedLocation(Location.FavorBank(Suit.Order))),
      Burn.secrets(2, PositionedLocation(Location.OnBanner(Banner.DarkestSecret)),
        None))
    val takes = Vector(
      Take(Piece.Banner(Banner.PeoplesFavor), player, Location.PlayArea(holder),
        Location.PlayArea(player), leaving = leaving),
      Take(Piece.Banner(Banner.DarkestSecret), player, Location.PlayArea(holder),
        Location.PlayArea(player)))
    val events = takes.map(take => recordedOperation(take))
    val encoded = encodedStreamThatRoundTrips("take-leaving", events)
    val ops = ujson.read(encoded).arr.map(_("payload")("step")("ops")(0))
    assertEquals(ops(0)("leaving").arr.size, 2)
    assert(!ops(1).obj.contains("leaving"))

  test("a secret move's side round trips, and a move without one writes " +
      "none"):
    val red = PlayerId("red")
    val blue = PlayerId("blue")
    val moves = Vector[CoreOperation](
      Move(Piece.Secrets(2), PositionedLocation(Location.PlayArea(red),
        side = Some(SecretSide.FaceDown)),
        PositionedLocation(Location.PlayArea(blue))),
      Take(Piece.Secrets(1), blue, Location.PlayArea(red),
        Location.PlayArea(blue), sourceSide = Some(SecretSide.FaceUp)),
      Move(Piece.Favor(1), PositionedLocation(Location.PlayArea(red)),
        PositionedLocation(Location.PlayArea(blue))))
    val events = moves.map(move => recordedOperation(move))
    val encoded = encodedStreamThatRoundTrips("secret-side", events)
    val ops = ujson.read(encoded).arr.map(_("payload")("step")("ops")(0))
    assertEquals(ops(0)("from")("side").str, "face-down")
    assertEquals(ops(1)("sourceSide").str, "face-up")
    assert(!ops(2)("from").obj.contains("side"))
