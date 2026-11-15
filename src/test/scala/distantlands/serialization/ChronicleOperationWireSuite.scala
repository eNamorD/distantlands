package distantlands.serialization

import distantlands.model._
import distantlands.serialization.WalkerWireEvents.{encodedStream,
  encodedStreamThatRoundTrips, recordedOperation}

/** Walker steps carrying Chronicle operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class ChronicleOperationWireSuite extends munit.FunSuite:
  test("an unknown Chronicle task and non-positive lineage points or Imperial " +
      "supply decode to a typed WireError"):
    def refusedPath(op: CoreOperation, edit: ujson.Value => Unit): String =
      val injected = ujson.read(encodedStream("chronicle",
        Vector(recordedOperation(op)))).arr
      edit(injected(0)("payload")("step")("ops")(0))
      GameEventWire.decodeStream(ujson.write(injected)) match
        case Left(WireError.InvalidValue(path, _)) => path
        case other => fail(other.toString)
    assert(refusedPath(ChronicleTaskAssigned(ChronicleTask.World,
      PlayerId("red")), _("task") = "moon").endsWith(".task"))
    assert(refusedPath(LineagePointsScored(LineageId("lineage:red"), 2),
      _("points") = 0).endsWith(".points"))
    assert(refusedPath(ImperialWarbandsTaken(8), _("count") = -1)
      .endsWith(".count"))

  test("a foundation restore names its number, and an unknown number " +
      "decodes to a typed WireError"):
    val encoded = encodedStreamThatRoundTrips("foundation-restored",
      Vector(recordedOperation(FoundationRestored(FoundationNumber.I))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "foundation-restored", "number" -> 1))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("number") = 7
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].number",
        "unknown Foundation number 7")))

  test("a foundation alteration names its number and source legacy, and " +
      "an unknown number decodes to a typed WireError"):
    val encoded = encodedStreamThatRoundTrips("foundation-altered",
      Vector(recordedOperation(FoundationAltered(FoundationNumber.II,
        LegacyId("L31")))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "foundation-altered", "number" -> 2,
        "source" -> "L31"))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("number") = 7
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].number",
        "unknown Foundation number 7")))

  test("a site placement names its Atlas end, its region and the site's " +
      "printed facts, and an unknown end, an unknown region or a negative " +
      "capacity decodes to a typed WireError"):
    val placement = PlaceAtlasSite(AtlasEnd.Recent, Region.Hinterland,
      capacity = 3, relicSlots = 2, Tokens(1, 2))
    val encoded = encodedStreamThatRoundTrips("place-atlas-site",
      Vector(recordedOperation(placement)))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "place-atlas-site", "end" -> "recent",
        "region" -> "hinterland", "capacity" -> 3, "relicSlots" -> 2,
        "resources" -> ujson.Obj("favor" -> 1, "secrets" -> 2)))
    def refusedPath(edit: ujson.Value => Unit): String =
      val injected = ujson.read(encoded).arr
      edit(injected(0)("payload")("step")("ops")(0))
      GameEventWire.decodeStream(ujson.write(injected)) match
        case Left(WireError.InvalidValue(path, _)) => path
        case other => fail(other.toString)
    assert(refusedPath(_("end") = "middle").endsWith(".end"))
    assert(refusedPath(_("region") = "sea").endsWith(".region"))
    assert(refusedPath(_("capacity") = -1).endsWith(".capacity"))
    assert(refusedPath(_("relicSlots") = -1).endsWith(".relicSlots"))

  test("scored legacies name each legacy, and an empty list decodes to a " +
      "typed WireError"):
    val encoded = encodedStreamThatRoundTrips("legacies-scored",
      Vector(recordedOperation(LegaciesScored(Vector(LegacyId("L02"),
        LegacyId("L22"))))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "legacies-scored",
        "legacies" -> ujson.Arr("L02", "L22")))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("legacies") = ujson.Arr()
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].legacies",
        "scores at least one legacy")))

  test("a kept starting relic names its player and its relic"):
    val encoded = encodedStreamThatRoundTrips("starting-relic-kept",
      Vector(recordedOperation(StartingRelicKept(PlayerId("red"),
        RelicId("R1")))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "starting-relic-kept", "player" -> "red",
        "relic" -> "R1"))

  test("hidden relics name each relic, and an empty list decodes to a " +
      "typed WireError"):
    val encoded = encodedStreamThatRoundTrips("relics-hidden",
      Vector(recordedOperation(RelicsHidden(Vector(RelicId("R1"),
        RelicId("R2"))))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "relics-hidden", "relics" -> ujson.Arr("R1", "R2")))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("relics") = ujson.Arr()
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].relics",
        "hides at least one relic")))

  test("a named winner names its winner and victory, and an unknown victory " +
      "decodes to a typed WireError"):
    val encoded = encodedStreamThatRoundTrips("winner-named",
      Vector(recordedOperation(WinnerNamed(PlayerId("red"),
        VictoryKind.Successor))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "winner-named", "winner" -> "red",
        "victory" -> "successor"))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("victory") = "luck"
    GameEventWire.decodeStream(ujson.write(injected)) match
      case Left(WireError.InvalidValue(path, _)) =>
        assert(path.endsWith(".victory"), path)
      case other => fail(other.toString)

