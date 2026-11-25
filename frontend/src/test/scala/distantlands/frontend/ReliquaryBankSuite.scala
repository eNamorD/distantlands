package distantlands.frontend

import distantlands.frontend.Dom.{all, firstMatch, one}

/** The Reliquary in the shared bank (Legacy in-play powers ruling 9). */
class ReliquaryBankSuite extends munit.FunSuite:
  private val back = CardDetails("hidden", "relic", "Facedown relic",
    orientation = Some("face-down"), hidden = true)
  private val known = CardDetails("R19", "relic", "Book of Records",
    orientation = Some("face-down"))
  private def bank(relics: CardDetails*) =
    val world = WorldBoardRenderer.world(TestProjections.actingProjection()
      .copy(reliquary = relics.toVector), BoardOptions.none)
    one(world, ".table-edge > .shared-bank")

  test("the shared bank shows a non-empty Reliquary, its relics facedown"):
    val reliquary = one(bank(back, back), ".shared-reliquary")
    assertEquals(reliquary.getAttribute("aria-label"), "Reliquary")
    assertEquals(all(reliquary, ".card-back-letter").map(_.textContent),
      Vector("R", "R"))

  test("a relic its viewer may peek at is drawn knowable, by its name"):
    val reliquary = one(bank(known), ".shared-reliquary")
    assertEquals(all(reliquary, ".card-face-knowable").size, 1)
    assert(reliquary.textContent.contains("Book of Records"))

  test("an empty Reliquary is not shown"):
    assertEquals(firstMatch(bank(), ".shared-reliquary"), None)

  test("the Reliquary's relics split into even rows"):
    assert(one(bank(back, back), ".shared-reliquary").classList
      .contains("even-rows"))
