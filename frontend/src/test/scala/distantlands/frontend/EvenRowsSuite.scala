package distantlands.frontend

import org.scalajs.dom

/** A card row that wraps splits its cards evenly: as many rows as plain
  * wrapping needs, the same number of cards on each, give or take one.
  */
class EvenRowsSuite extends munit.FunSuite:
  private def cards(count: Int, width: Double = 100) = Vector.fill(count)(width)

  test("six cards with room for five split three and three"):
    // Five 100px cards and four 10px gaps are 540px; three are 320px.
    assertEquals(EvenRows.balanced(cards(6), gap = 10, width = 540), Some(320.0))

  test("seven cards with room for five split four and three"):
    assertEquals(EvenRows.balanced(cards(7), gap = 10, width = 540), Some(430.0))

  test("five cards with room for four split three and two"):
    assertEquals(EvenRows.balanced(cards(5), gap = 10, width = 430), Some(320.0))

  test("cards that fill their rows keep them full"):
    assertEquals(EvenRows.balanced(cards(8), gap = 10, width = 430), Some(430.0))

  test("a row with room for one card holds one"):
    assertEquals(EvenRows.balanced(cards(3), gap = 10, width = 150), Some(100.0))

  test("cards of mixed widths even out by length, keeping the line count"):
    // Plain wrapping at 338px gives three lines: three denizens, two
    // legacies, one denizen. Two cards a line is the narrowest three-line fit.
    val mixed = Vector(83.0, 83, 83, 130, 130, 83)
    assertEquals(EvenRows.lines(mixed, gap = 4, width = 338), 3)
    assertEquals(EvenRows.balanced(mixed, gap = 4, width = 338), Some(217.0))
    assertEquals(EvenRows.lines(mixed, gap = 4, width = 217), 3)

  test("the line count counts the gap between cards, not after the last"):
    assertEquals(EvenRows.lines(cards(5), gap = 10, width = 540), 1)
    assertEquals(EvenRows.lines(cards(5), gap = 10, width = 539), 2)

  test("cards that fit on one line are left to wrap as they do"):
    assertEquals(EvenRows.balanced(cards(5), gap = 10, width = 540), None)

  test("an unmeasured row, not yet laid out, is left alone"):
    assertEquals(EvenRows.balanced(cards(6, width = 0), gap = 8, width = 0),
      None)

  test("an even count caps the row's inline size and marks it"):
    val row = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    EvenRows.set(row, Some(321))
    assert(row.classList.contains("even-rows-balanced"))
    assertEquals(row.style.getPropertyValue("max-inline-size"), "321px")

  test("no even count clears a row capped before"):
    val row = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    EvenRows.set(row, Some(321))
    EvenRows.set(row, None)
    assert(!row.classList.contains("even-rows-balanced"))
    assertEquals(row.style.getPropertyValue("max-inline-size"), "")

  test("a watched row is marked even without a ResizeObserver to watch it"):
    val row = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    assertEquals(EvenRows.watch(row), row)
    assert(row.classList.contains("even-rows"))
