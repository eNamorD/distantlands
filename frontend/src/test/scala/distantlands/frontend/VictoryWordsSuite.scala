package distantlands.frontend

/** What the victory screen calls each kind of win (Empire phase design,
  * "Slice 2", "UI"). */
class VictoryWordsSuite extends munit.FunSuite:
  test("each victory kind has its name"):
    val names = Vector(
      "usurper" -> "Usurper victory",
      "visionary" -> "Vision victory",
      "oathkeeper" -> "Oathkeeper victory",
      "random-selection" -> "victory by random selection",
      "stable-regime" -> "Stable Regime victory",
      "chancellor-exhaustion" -> "War Exhaustion victory",
      "successor" -> "Successor to the Chancellor")
    names.foreach { case (key, name) =>
      assertEquals(VictoryWords.of(Some(key)), name, clue(key))
    }

  test("an unknown or missing kind still reads as a victory"):
    assertEquals(VictoryWords.of(Some("grand-council")),
      "grand council victory")
    assertEquals(VictoryWords.of(None), "victory")
