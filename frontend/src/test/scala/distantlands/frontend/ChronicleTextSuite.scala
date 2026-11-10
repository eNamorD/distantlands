package oathdigital.frontend

import oathdigital.protocol.ChronicleSummary

class ChronicleTextSuite extends munit.FunSuite:
  test("lineage names a generated lineage by its color"):
    assertEquals(ChronicleText.lineage("red-lineage"), "Red")
    assertEquals(ChronicleText.lineage("l9"), "L9")

  test("the summary line reads game, era target and scores"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleFixtures.entry("A", game = Some(3)).summary),
      "Game 3 · Era: 20 to win · Red 9, Blue 6")

  test("a TTS import's unknown game reads as unknown, and no scores add " +
      "nothing"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleSummary("A", None, 20, Vector.empty)),
      "Game unknown · Era: 20 to win")

  test("a row's details add the saved date"):
    assertEquals(ChronicleText.rowDetails(SavedChronicle(
      ChronicleFixtures.entry("A"), ChronicleFixtures.savedAt)),
      "Game 1 · Era: 20 to win · saved 2026-10-29")
