package distantlands.server

import java.nio.file.{Files, Path}

/** The table's edge sizes the Reliquary as it sizes the banners (Legacy
  * in-play powers ruling 9). */
class StylesheetTableEdgeSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  test("on the table's edge the Reliquary's relics take the banners' side"):
    assert(css.contains(".table-edge .shared-reliquary { --card-w: " +
      "var(--banner-edge-side);"))

  test("at fit zoom its label takes the glance size, as the bank's do"):
    val glance = css.indexOf(".map-compact .table-edge :is(")
    assert(glance >= 0)
    assert(css.substring(glance, css.indexOf("}", glance))
      .contains(".shared-reliquary-label"))
