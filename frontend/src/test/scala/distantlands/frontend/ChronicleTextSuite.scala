package distantlands.frontend

import distantlands.protocol.{ChronicleLegacy, ChronicleSummary}

class ChronicleTextSuite extends munit.FunSuite:
  test("lineage names a generated lineage by its color"):
    assertEquals(ChronicleText.lineage("red-lineage"), "Red")
    assertEquals(ChronicleText.lineage("l9"), "L9")

  test("the summary line reads the next game and the era's target, and " +
      "leaves the scores to the scoresheet"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleFixtures.entry("A", game = Some(3)).summary),
      "Game 3 · Era: First to 20")

  test("a TTS import's unknown game reads as unknown"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleSummary("A", None, 20, Vector.empty)),
      "Game unknown · Era: First to 20")

  test("a row's details add the saved date"):
    assertEquals(ChronicleText.rowDetails(SavedChronicle(
      ChronicleFixtures.entry("A"), ChronicleFixtures.savedAt)),
      "Game 1 · Era: First to 20 · saved 2026-10-29")

  test("a lineage's legacies are named, the dormant ones marked (R39)"):
    assertEquals(ChronicleText.legacies(Vector(
      ChronicleLegacy("Seer", active = true),
      ChronicleLegacy("Iron Hand", active = false))),
      "Seer, Iron Hand (dormant)")
