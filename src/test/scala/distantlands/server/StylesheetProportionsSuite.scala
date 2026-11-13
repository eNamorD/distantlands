package distantlands.server

import java.nio.file.{Files, Path}

/** The card boxes take their shape from one table of real proportions
  * (real proportions design). A literal ratio anywhere else would let one
  * box drift from the asset it stands for.
  */
class StylesheetProportionsSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  private val table = Vector(
    "--ratio-denizen: 381 / 596", "--ratio-site: 770 / 596",
    "--ratio-relic: 1 / 1", "--ratio-banner: 1 / 1",
    "--ratio-legacy: 596 / 381", "--ratio-foundation: 469 / 803",
    "--ratio-oath-goal: 673 / 898", "--ratio-oathkeeper-title: 1016 / 700")

  test("the stylesheet names every card kind's real ratio once"):
    table.foreach(entry =>
      assertEquals(css.split(java.util.regex.Pattern.quote(entry), -1).length - 1,
        1, entry))

  test("no card box carries the old 1 : 1.4 ratio"):
    assert(!css.contains("1 / 1.4"), "aspect-ratio: 1 / 1.4 remains")
    assert(!css.contains("* 1.4)"), "a width times 1.4 remains")
    assert(!css.contains("/ 1.4)"), "a height over 1.4 remains")

  test("a card's width follows from its height and the denizen ratio"):
    assert(css.contains("--card-w: calc(var(--card-h) * 381 / 596)"))
