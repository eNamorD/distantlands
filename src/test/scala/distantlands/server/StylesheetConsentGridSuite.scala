package distantlands.server

import java.nio.file.{Files, Path}

/** The consent column's preference rows (reconsider design, slice 1): a
  * player's name on its own line, and that player's three settings below
  * it in equal thirds, so no setting wraps alone. */
class StylesheetConsentGridSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule whose selector is `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val at = css.indexOf(s"$selector {")
    assert(at >= 0, s"no rule $selector")
    css.substring(at, css.indexOf("}", at))

  /** The stylesheet's consent rules: from their opening comment to the
    * overlay's close button, which follows them. */
  private def consentRules(using munit.Location): String =
    val from = css.indexOf("/* The consent confirmation")
    val to = css.indexOf(".card-overlay-close {", from)
    assert(from >= 0 && to > from, "the consent rules were not found")
    css.substring(from, to)

  test("a row stacks the player's name over the settings"):
    val row = rule(".consent-grid-row")
    assert(row.contains("display: grid;"), row)
    assert(!row.contains("flex"), row)

  test("the three settings share one line in equal thirds"):
    val settings = rule(".consent-grid-choices")
    assert(settings.contains("display: grid;"), settings)
    assert(settings.contains(
      "grid-template-columns: repeat(3, minmax(0, 1fr));"), settings)
    assert(settings.contains("gap: 6px;"), settings)

  test("a setting keeps the compact Control face, with the side padding " +
      "a third leaves room for at 360px (R3)"):
    assert(consentRules.contains(
      ".overlay-confirm button, .consent-grid-choice { min-width: 32px;\n" +
      "  min-height: 30px; padding: 4px 9px;"), consentRules)
    assert(rule(".consent-grid-choices .consent-grid-choice")
      .contains("padding: 4px 6px;"))

  test("rows stand further apart than a name stands from its settings"):
    assert(rule(".consent-grid").contains("gap: 12px;"))
    assert(rule(".consent-grid-row").contains("gap: 4px;"))

  test("the column never outgrows the overlay, and a long name wraps"):
    assert(rule(".consent-grid")
      .contains("grid-template-columns: minmax(0, 1fr);"))
    assert(rule(".consent-grid-player").contains("overflow-wrap: anywhere;"))

  test("the column reads at the details column's size (R1)"):
    assert(rule(".card-overlay-details").contains("font-size: 0.82em;"))
    assert(rule(".consent-grid").contains("font-size: 0.82em;"))

  test("the pressed setting is the column's one selected thing"):
    val pressed = rule(".consent-grid-choice[aria-pressed=\"true\"]")
    assert(pressed.contains("border-color: var(--selection);"), pressed)
    assert(pressed.contains("background: var(--selection-fill);"), pressed)
    assert(!consentRules.contains("consent-amber"))

  test("a finger gets a 44px setting"):
    val coarse = css.substring(css.indexOf("@media (pointer: coarse) {"))
    assert(coarse.contains(
      ".consent-grid-choice { min-width: 44px; min-height: 44px; }"))

  test("the consent rules move nothing"):
    assert(!consentRules.contains("transition"))
    assert(!consentRules.contains("animation"))
