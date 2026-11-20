package distantlands.server

import java.nio.file.{Files, Path}

/** The consent column's preference rows (reconsider design, slice 1): a
  * player's name on its own line, and that player's three settings below
  * it in equal thirds, so no setting wraps alone.
  *
  * The suite pins declarations, and that no second rule overrides them.
  * The layout itself, three settings on one line with every label whole,
  * is covered only by the browser check at desktop width and at 360px. */
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

  /** How many rules name `selector` as a whole selector of their list. */
  private def rulesOpenedBy(selector: String): Int =
    (java.util.regex.Pattern.quote(selector) + "\\s*[,{]").r
      .findAllIn(css).size

  /** Each rule whose selector names a setting, as its selector list and
    * its declarations: the shared Control face, its states, the thirds'
    * padding and the touch size. */
  private def settingRules: Vector[(String, String)] =
    val rule = """([^{}]+)\{([^{}]*)\}""".r
    val setting = """\.consent-grid-choice(?![\w-])""".r
    rule.findAllMatchIn(css).map(found =>
        found.group(1).trim -> found.group(2))
      .filter((selector, _) => setting.findFirstIn(selector).nonEmpty)
      .toVector

  test("each layout selector opens one rule, so no later rule overrides " +
      "what this suite reads"):
    Vector(".consent-grid", ".consent-grid-heading", ".consent-grid-row",
      ".consent-grid-player", ".consent-grid-choices",
      ".consent-grid-choices .consent-grid-choice").foreach(selector =>
      assertEquals(rulesOpenedBy(selector), 1, selector))

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
    // The thirds were measured with the label at this size: one rule sets
    // a setting's font size, and it is the shared face's 0.78rem.
    assertEquals(settingRules.map(_._2).filter(_.contains("font-size")).map(
      _.contains("font-size: 0.78rem;")), Vector(true))

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

  test("a disabled setting wears the Disabled face and does not answer " +
      "hover (R4)"):
    val disabled = rule(".consent-grid-choice:disabled")
    assert(disabled.contains("opacity: 0.5;"), disabled)
    assert(disabled.contains("cursor: not-allowed;"), disabled)
    assert(css.contains(".overlay-confirm button:hover, " +
      ".consent-grid-choice:hover:not(:disabled) {"))
    assert(!css.contains(".consent-grid-choice:hover {"))

  test("a finger gets a 44px setting"):
    val coarse = css.substring(css.indexOf("@media (pointer: coarse) {"))
    assert(coarse.contains(
      ".consent-grid-choice { min-width: 44px; min-height: 44px; }"))

  test("the consent rules move nothing"):
    assert(!consentRules.contains("transition"))
    assert(!consentRules.contains("animation"))
