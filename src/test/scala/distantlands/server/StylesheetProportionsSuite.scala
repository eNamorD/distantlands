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
    "--ratio-oath-goal: 673 / 898", "--ratio-oathkeeper-title: 1016 / 700",
    "--ratio-mat: 5787 / 2303")

  test("the stylesheet names every card kind's real ratio once"):
    table.foreach(entry =>
      assertEquals(css.split(java.util.regex.Pattern.quote(entry), -1).length - 1,
        1, entry))

  test("no card box carries the old 1 : 1.4 ratio"):
    assert(!css.contains("1 / 1.4"), "aspect-ratio: 1 / 1.4 remains")
    assert(!css.contains("* 1.4)"), "a width times 1.4 remains")
    assert(!css.contains("/ 1.4)"), "a height over 1.4 remains")

  test("a card's width follows from its height and the denizen ratio"):
    assert(css.contains("--card-w: calc(var(--card-h) * var(--ratio-denizen))"))

  /** Every box reads the table; a ratio written anywhere else would let a
    * box drift from it. */
  test("each ratio is written once, in the table"):
    Vector("381 / 596", "596 / 381", "770 / 596", "469 / 803", "673 / 898",
      "1016 / 700", "5787 / 2303").foreach(ratio =>
      assertEquals(css.split(java.util.regex.Pattern.quote(ratio), -1).length - 1,
        1, ratio))
    assert(css.contains("--ratio-mat: 5787 / 2303"))
    Vector("--ratio-site", "--ratio-relic", "--ratio-banner", "--ratio-legacy",
      "--ratio-mat").foreach(name =>
      assert(css.contains(s"var($name)"), s"nothing reads $name"))

  /** `MatLayout` is Scala.js, and the frontend's test runner has no file
    * access, so its source is read here beside the stylesheet: the card and
    * site ratios, the mat's size and the board's design width must be the
    * stylesheet's own. */
  test("MatLayout's ratios, mat size and design width are the stylesheet's"):
    val layout = Files.readString(Path.of("frontend", "src", "main", "scala",
      "distantlands", "frontend", "MatLayout.scala"))
    def number(pattern: String): String =
      pattern.r.findFirstMatchIn(layout).map(_.group(1))
        .getOrElse(fail(s"MatLayout has no $pattern"))
    def ratio(name: String): String =
      s"$name: ([0-9]+ / [0-9]+);".r.findFirstMatchIn(css).map(_.group(1))
        .getOrElse(fail(s"the stylesheet has no $name"))
    assertEquals(number("""val CardW: Double = CardH \* ([0-9]+ / [0-9]+)"""),
      ratio("--ratio-denizen"))
    assertEquals(number("""val SiteW: Double = CardH \* ([0-9]+ / [0-9]+)"""),
      ratio("--ratio-site"))
    assertEquals(s"${number("""val Width = ([0-9]+)\.0""")} / " +
      number("""val Height = ([0-9]+)\.0"""), ratio("--ratio-mat"))
    val design = number("""val DesignWidthPx = ([0-9]+)\.0""").toInt
    val surface = """\.map-content \{ position: absolute; width: ([0-9]+)px; padding: ([0-9]+)px;""".r
      .findFirstMatchIn(css).getOrElse(fail("no map surface width"))
    assertEquals(surface.group(1).toInt, design + 2 * surface.group(2).toInt)
