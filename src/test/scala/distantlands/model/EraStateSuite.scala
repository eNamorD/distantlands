package oathdigital.model

/** The era scorepad (Chronicle browsing design, "The era scorepad"). */
class EraStateSuite extends munit.FunSuite:
  private val red = LineageId("red-lineage")
  private val blue = LineageId("blue-lineage")

  private def game(entries: (LineageId, Int, Role)*): EraGame =
    EraGame(entries.toVector.map(EraEntry.apply))

  test("a lineage's score is its latest entry's total, or 0 without one"):
    val era = EraState(20, Vector(
      game((red, 3, Role.Chancellor), (blue, 1, Role.Exile)),
      game((red, 5, Role.Chancellor))))
    assertEquals(era.score(red), 5)
    assertEquals(era.score(blue), 1)
    assertEquals(era.score(LineageId("pink-lineage")), 0)

  test("a game records each lineage once, with a non-negative total"):
    intercept[IllegalArgumentException](
      game((red, 3, Role.Exile), (red, 4, Role.Exile)))
    intercept[IllegalArgumentException](EraEntry(red, -1, Role.Exile))
    intercept[IllegalArgumentException](EraState(0, Vector.empty))
