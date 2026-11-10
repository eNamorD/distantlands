package distantlands.serialization

import distantlands.gameplay.setup.FirstGameSetupFixture.{chronicle, orders}
import distantlands.model._
import distantlands.model.OathEvent.{RoundEnded, WarExhaustionResolved}
import distantlands.serialization.WalkerWireEvents.{assertStreamRoundTrips,
  encodedStreamThatRoundTrips}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** Single game facts on the wire: round end, victory, state-based events and
  * the GameStarted Chronicle. */
class GameEventFactsWireSuite extends munit.FunSuite:
  test("round ending and War Exhaustion preserve outcome and random domain"):
    val p1 = PlayerId("p1")
    val p2 = PlayerId("p2")
    val events = Vector[OathEvent](RoundEnded(8, None),
      WarExhaustionResolved(p2, VictoryKind.RandomSelection, Vector.empty,
        Vector(p1, p2)))
    events.zipWithIndex.foreach { case (event, index) =>
      val encoded = accepted(GameEventWire.encodeEvent("war", index, event))
      val decoded = accepted(GameEventWire.decode(encoded))
      assertEquals(decoded.formatVersion, GameEventWire.FormatVersion)
      assertEquals(decoded.event, event)
    }

  test("War Exhaustion round trips every victory kind"):
    VictoryKind.values.toVector.zipWithIndex.foreach { case (kind, index) =>
      val event = WarExhaustionResolved(PlayerId("p1"), kind, Vector.empty,
        Vector.empty)
      val encoded = accepted(GameEventWire.encodeEvent("war", index, event))
      assertEquals(accepted(GameEventWire.decode(encoded)).event, event,
        clue(kind))
    }

  test("the end die round trips with and without a win"):
    val events = Vector[OathEvent](OathEvent.EndDieRolled(5, 4, None),
      OathEvent.EndDieRolled(7, 3,
        Some(GameResult(PlayerId("p2"), VictoryKind.Successor))))
    assertStreamRoundTrips("end-die", events)

  test("an end die with half a win or an unknown kind is refused"):
    def encodedWin = accepted(GameEventWire.encodeEvent("end-die", 0,
      OathEvent.EndDieRolled(6, 5,
        Some(GameResult(PlayerId("p1"), VictoryKind.StableRegime)))))
    Vector[(String, ujson.Value)]("victoryKind" -> ujson.Null,
      "winnerPlayerId" -> ujson.Null, "victoryKind" -> ujson.Str("bogus"),
      "victoryKind" -> ujson.Num(3)).foreach { (field, value) =>
      val encoded = encodedWin
      encoded("payload")(field) = value
      assert(GameEventWire.decode(encoded).isLeft, clue((field, value)))
    }

  test("a Vision victory event round trips"):
    val events = Vector[OathEvent](OathEvent.VisionVictory(PlayerId("red"),
      Vector(VisionId("vision:vision-of-faith"),
        VisionId("vision:vision-of-conquest"))))
    assertStreamRoundTrips("visions", events)

  test("a Visionary War Exhaustion round trips its Visions"):
    val event = WarExhaustionResolved(PlayerId("p1"), VictoryKind.Visionary,
      Vector(VisionId("vision:vision-of-faith")), Vector.empty)
    val encoded = accepted(GameEventWire.encodeEvent("war", 0, event))
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)

  test("state-based events are written at format version 1"):
    val events = Vector[OathEvent](
      OathEvent.BanditsRefilled(Vector(SiteId("site:a") -> 1)),
      OathEvent.UsurperFlipped(PlayerId("red")))
    val encoded = encodedStreamThatRoundTrips("state-based", events)
    // Every event is written at format version 1, the one version the codec
    // supports.
    assertEquals(
      ujson.read(encoded).arr.map(_("formatVersion").num.toInt).toVector,
      Vector.fill(events.size)(1))

  test("GameStarted round trips its Chronicle and dealt SetupOrders"):
    val event = OathEvent.GameStarted(chronicle, orders)
    val encoded = accepted(GameEventWire.encodeEvent("goal", 0L, event))
    assertEquals(encoded("payload")("chronicle")("worldDeck").arr.size,
      chronicle.worldDeck.size)
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)

  test("a GameStarted Chronicle round trips its era scores and Oathkeeper goal"):
    val scored = chronicle.copy(
      era = EraState(targetScore = 15, games = Vector(EraGame(Vector(
        EraEntry(LineageId("l1"), 4, Role.Chancellor),
        EraEntry(LineageId("l9"), 7, Role.Exile))))),
      oathkeeperGoal = OathkeeperGoal.Devotion)
    val event = OathEvent.GameStarted(scored, orders)
    val encoded = accepted(GameEventWire.encodeEvent("goal", 0L, event))
    assertEquals(encoded("payload")("chronicle")("oathkeeperGoal").str,
      OathkeeperGoal.Devotion.key)
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)

  test("a GameStarted Chronicle round trips its name and refuses a blank one"):
    val event = OathEvent.GameStarted(chronicle.copy(name = "Ashen Crown"),
      orders)
    val encoded = accepted(GameEventWire.encodeEvent("named", 0L, event))
    assertEquals(encoded("payload")("chronicle")("name").str, "Ashen Crown")
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)
    encoded("payload")("chronicle")("name") = ""
    rejected(GameEventWire.decode(encoded)) match
      case WireError.InvalidValue(path, message) =>
        assert(path.endsWith(".name"), path)
        assertEquals(message, "a Chronicle needs a name")
      case other => fail(other.toString)

  test("a GameStarted Chronicle keeps each lineage role, the Chancellor's " +
      "included, and refuses an unknown one"):
    val roles = Vector(Role.Exile, Role.Citizen, Role.Chancellor)
    val event = OathEvent.GameStarted(chronicle.copy(lineages =
      roles.zipWithIndex.map((role, index) => LineageState(
        LineageId(s"l$index"), None, role, Vector.empty, Vector.empty,
        startingRelics =
          if index == 0 then chronicle.relicDeck.take(1) else Vector.empty))),
      orders)
    val encoded = accepted(GameEventWire.encodeEvent("roles", 0L, event))
    assertEquals(encoded("payload")("chronicle")("lineages").arr
      .map(_("role").str).toVector, Vector("exile", "citizen", "chancellor"))
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)
    encoded("payload")("chronicle")("lineages")(0)("role") = "regent"
    rejected(GameEventWire.decode(encoded)) match
      case WireError.InvalidValue(path, _) => assert(path.endsWith(".role"), path)
      case other => fail(other.toString)

  test("a GameStarted Chronicle refuses an unknown Oathkeeper goal, a zero " +
      "era target, a doubled scorepad lineage and a negative total"):
    val event = OathEvent.GameStarted(chronicle.copy(era =
      EraState(20, Vector(EraGame(Vector(
        EraEntry(LineageId("l1"), 2, Role.Exile)))))), orders)
    def refusedPath(edit: ujson.Value => Unit): String =
      val encoded = accepted(GameEventWire.encodeEvent("goal", 0L, event))
      edit(encoded("payload")("chronicle"))
      rejected(GameEventWire.decode(encoded)) match
        case WireError.InvalidValue(path, _) => path
        case other => fail(other.toString)
    assert(refusedPath(_("oathkeeperGoal") = "glory").endsWith(".oathkeeperGoal"))
    assert(refusedPath(_("era")("targetScore") = 0).endsWith(".era.targetScore"))
    assert(refusedPath { c =>
      val entries = c("era")("games")(0)("entries")
      entries.arr.append(entries(0))
    }.endsWith(".era.games[0].entries"))
    assert(refusedPath(_("era")("games")(0)("entries")(0)("totalScore") = -1)
      .endsWith(".totalScore"))

  test("GameStarted refuses an unknown stored item kind with a typed " +
      "WireError"):
    val event = OathEvent.GameStarted(chronicle, orders)
    val invalid = accepted(GameEventWire.encodeEvent("goal", 0L, event))
    val storedIndex = chronicle.atlasBox.indexWhere(_.items.nonEmpty)
    val storedBox = invalid("payload")("chronicle")("atlasBox")(storedIndex)
    storedBox("items")(0)("kind") = "unknown"
    val failure = rejected(GameEventWire.decode(invalid))
    assert(failure match {
      case WireError.InvalidValue(path, _) => path.endsWith(".kind")
      case _ => false
    }, failure.toString)
