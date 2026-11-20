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
      ".consent-grid-choices .consent-grid-choice", ".consent-requests",
      ".consent-request-list", ".consent-request", ".consent-request-asked",
      ".consent-request-also", ".consent-request-answer").foreach(selector =>
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
      ".overlay-confirm button, .consent-grid-choice,\n" +
      ".consent-request-change { min-width: 32px;\n" +
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
    val disabled = rule(".consent-request-change[aria-disabled=\"true\"]")
    assert(disabled.contains("opacity: 0.5;"), disabled)
    assert(disabled.contains("cursor: not-allowed;"), disabled)
    // One rule gives the face to a disabled setting, a disabled change
    // and a withheld change.
    assert(css.contains(".consent-grid-choice:disabled, " +
      ".consent-request-change:disabled,\n" +
      ".consent-request-change[aria-disabled=\"true\"] {"))
    assert(css.contains(".overlay-confirm button:hover, " +
      ".consent-grid-choice:hover:not(:disabled),\n" +
      ".consent-request-change:hover:not(:disabled, " +
      "[aria-disabled=\"true\"]) {"))
    assert(!css.contains(".consent-grid-choice:hover {"))
    assert(!css.contains(".consent-request-change:hover {"))

  test("the requests block stands under the rows with more space above " +
      "its heading than below, and takes no room while hidden"):
    val block = rule(".consent-requests")
    assert(block.contains("display: grid;"), block)
    assert(block.contains("gap: 8px;"), block)
    assert(block.contains("margin-top: 8px;"), block)
    assert(css.contains(".consent-requests[hidden] { display: none; }"))

  test("entries stand apart as the rows do, and an entry's lines stand " +
      "close"):
    assert(rule(".consent-request-list").contains("gap: 12px;"))
    assert(rule(".consent-request").contains("gap: 4px;"))
    assert(rule(".consent-request-asked")
      .contains("overflow-wrap: anywhere;"))

  test("the second line is quiet and never under the Eleven-Pixel Floor"):
    val also = rule(".consent-request-also")
    assert(also.contains("color: var(--ink-dim);"), also)
    assert(also.contains("font-size: max(0.85em, 0.7rem);"), also)

  test("the state stands before its button on one line"):
    val answer = rule(".consent-request-answer")
    assert(answer.contains("display: flex;"), answer)
    assert(answer.contains("align-items: center;"), answer)
    assert(answer.contains("gap: 8px;"), answer)

  test("alone on its line the column fills it, up to a reading measure " +
      "(R16)"):
    val column = rule(".consent-grid")
    assert(column.contains("flex: 1 1 17rem;"), column)
    assert(column.contains("max-width: 22rem;"), column)

  test("the change button keeps the focus ring of the settings"):
    assert(css.contains(".overlay-confirm button:focus-visible, " +
      ".consent-grid-choice:focus-visible,\n" +
      ".consent-request-change:focus-visible {"))

  test("a finger gets a 44px setting"):
    val coarse = css.substring(css.indexOf("@media (pointer: coarse) {"))
    assert(coarse.contains(
      ".consent-grid-choice, .consent-request-change { min-width: 44px; " +
        "min-height: 44px; }"))

  test("the consent rules move nothing"):
    assert(!consentRules.contains("transition"))
    assert(!consentRules.contains("animation"))
