package distantlands.application

import distantlands.application.chronicle.ChronicleEntries
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.OathState
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The Chronicle every seat sees (Chronicle browsing design, "Projection"). */
class ChronicleProjectionSuite extends munit.FunSuite:
  private val projector = GameProjector(catalog)
  private val entries = ChronicleEntries()

  test("every seat sees the name and the starting digest from the start, " +
      "and no evolved Chronicle"):
    val baseline = Table.start.ready.game.campaign.baseline
    Vector(p1, p2).foreach(seat =>
      val chronicle = projector.project("g", LoadedGame(Table.start.state,
        1L), seat).chronicle
      assertEquals(chronicle.map(_.name), Some(baseline.name))
      assertEquals(chronicle.map(_.startingDigest),
        Some(entries.entry(baseline).digest))
      assertEquals(chronicle.flatMap(_.evolved), None))

  test("once the Chronicle is journaled, every seat sees the evolved one"):
    val state = EvolvedChronicleFixture.done.state
    val expected = entries.entry(EvolvedChronicleFixture.record)
    Vector(p1, p2).foreach(seat =>
      assertEquals(projector.project("g", LoadedGame(state, 1L), seat)
        .chronicle.flatMap(_.evolved), Some(expected)))
    assertEquals(projector.projectPublic("g", LoadedGame(state, 1L))
      .chronicle.flatMap(_.evolved), Some(expected))

  test("a game not yet started has no Chronicle"):
    assertEquals(projector.project("g", LoadedGame(OathState.NoGame, 0L),
      p1).chronicle, None)
