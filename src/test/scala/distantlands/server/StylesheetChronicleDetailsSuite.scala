package distantlands.server

import java.nio.file.{Files, Path}

/** Chronicle Details' styles (Chronicle Details design, slice 4): the log
  * overlay's layer and scrim under the card overlay, a mat row's box for
  * the Atlas, the foundation strip's face for the aids, and the button on
  * the table's edge. */
class StylesheetChronicleDetailsSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule whose selector is `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val at = css.indexOf(s"$selector {")
    assert(at >= 0, s"no rule $selector")
    css.substring(at, css.indexOf("}", at))

  test("the overlay takes the log overlay's layer and scrim, one step " +
      "under the card overlay"):
    val overlay = rule(".chronicle-details")
    assert(overlay.contains("position: fixed; z-index: 300;"), overlay)
    assert(overlay.contains("background: #0b0a08e8;"), overlay)
    assert(rule(".card-overlay").contains("z-index: 310;"))
    assert(css.contains(".chronicle-details[hidden] { display: none; }"))

  test("the panel fills the pane's height at a 50rem measure (R16)"):
    val panel = rule(".chronicle-details-panel")
    assert(panel.contains("width: min(50rem, 100%);"), panel)
    assert(panel.contains("background: var(--pane);"), panel)

  test("the Atlas box is a mat board of the row's width and ratio on the " +
      "Base fill (R12)"):
    val box = rule(".mat-board.chronicle-details-atlas-board")
    assert(box.contains("width: var(--atlas-w);"), box)
    assert(box.contains("aspect-ratio: var(--atlas-ratio);"), box)
    assert(box.contains("background: var(--base);"), box)
    assert(rule(".chronicle-details-atlas-frame").contains("overflow-x: auto;"))

  test("the aids wear the foundation strip's face, and the table's edge " +
      "keeps no foundation rule"):
    assert(css.contains(".foundation-card, .chronicle-task-card {"))
    assert(css.contains(".foundation-strip, .chronicle-task-strip {"))
    assert(!css.contains(".table-edge .foundations"))
    assert(!css.contains(".map-compact .foundation-card"))

  test("the button fills the edge after the Shared Bank, as tall as the " +
      "edge, a Control full width on a phone (R18)"):
    assert(rule(".table-edge .shared-bank").contains("flex: 0 1 auto;"))
    val button = rule(".table-edge .chronicle-details-button")
    assert(button.contains("flex: 1 1 12rem;"), button)
    assert(button.contains("align-self: stretch;"), button)
    assert(button.contains("min-height: calc(44px / var(--map-scale, 1));"),
      button)
    assert(button.contains("border: 1px solid var(--brass-line);"), button)
    assert(css.contains("@media (max-width: 620px) {\n  .table-edge " +
      "{ flex-wrap: wrap; }\n  .table-edge .chronicle-details-button " +
      "{ flex: 1 1 100%; }\n}"))
    assert(rule(".map-compact .table-edge .chronicle-details-button")
      .contains("font-size: var(--glance-text);"))

  test("the sections fit the panel's width, so only the Atlas and the " +
      "scorepad frames scroll sideways (R13)"):
    val body = rule(".chronicle-details-body")
    assert(body.contains("grid-template-columns: minmax(0, 1fr);"), body)
    assert(rule(".chronicle-details-body > *").contains("min-width: 0;"))

  test("a scorepad legacy chip wraps its name, so a phone keeps the Total " +
      "column in view"):
    assert(rule(".scoresheet-legacy-chips .legacy-chip")
      .contains("white-space: normal;"))

  test("the foundation strip keeps no margin of its own, which the table's " +
      "edge needed and the overlay's grid gap replaces"):
    assert(!css.contains(".foundations { margin-top"))
    assert(!css.contains(".chronicle-details .foundations {"))
