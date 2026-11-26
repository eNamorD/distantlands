package distantlands.server

import java.nio.file.{Files, Path}

/** The art layer's rules (game art design, "`ArtLayer`"). The suite pins
  * declarations; that an image covers its box and hides only that box's
  * printed content is covered by the browser check. */
class StylesheetArtSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule that opens a line with
    * `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val opening = ("(?m)^" + java.util.regex.Pattern.quote(selector) +
      " \\{").r.findFirstMatchIn(css)
    assert(opening.nonEmpty, s"no rule $selector")
    css.substring(opening.get.start, css.indexOf("}", opening.get.start))

  /** The stylesheet without its comments, so a rule's selector list is
    * the selectors alone. */
  private val uncommented = css.replaceAll("(?s)/\\*.*?\\*/", "")

  /** The art layer's rules: from their opening comment to the document
    * level overlay's comment, which follows them. */
  private def artRules(using munit.Location): String =
    val from = css.indexOf("/* Licensed art")
    val to = css.indexOf("/* Document level, above .game-pane", from)
    assert(from >= 0 && to > from, "the art rules were not found")
    css.substring(from, to)

  test("the image fills its box, keeps its proportions and sizes nothing"):
    val image = rule(".art")
    Vector("position: absolute", "inset: 0", "width: 100%", "height: 100%",
      "object-fit: contain", "z-index: -1", "pointer-events: none")
      .foreach(part => assert(image.contains(part), part))

  test("a box holding an image isolates its stacking, so the image lies " +
      "over the box's fill and under everything else in it"):
    assert(artRules.contains(":has(> .art) { isolation: isolate; }"))

  /** Every box kind that answers for printed content of its own. */
  private val boxes = ".card-face, .card-hover-summary, .site"

  test("printed content is hidden with opacity, by its nearest box"):
    assert(rule(".printed").contains("opacity: var(--printed-opacity, 1)"))
    assert(artRules.contains(s":is($boxes) { --printed-opacity: 1; }"))
    assert(artRules.contains(
      s":is($boxes).art-ready { --printed-opacity: 0; }"))

  test("the art layer hides nothing with display or visibility, and " +
      "moves nothing"):
    Vector("display: none", "visibility: hidden", "transition", "animation")
      .foreach(part => assert(!artRules.contains(part), part))

  test("no other rule sets a printed element's opacity"):
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    val setters = rules.findAllMatchIn(uncommented).filter(found =>
      found.group(1).contains(".printed") &&
        found.group(2).contains("opacity")).map(_.group(1).trim).toVector
    assertEquals(setters, Vector(".printed", ".end-die-idle.printed",
      ".map-compact .card-header.printed > *,\n" +
        ".map-compact .card-header.printed > .card-emblems > *"))

  /** Opacity reaches nothing through an element with no box. On the
    * compact map a card's header and its emblems column are
    * `display: contents`, so their parts take the box's answer
    * themselves; found in the slice 1 browser check, where a ready card
    * on the zoomed-out map kept its drawn name over the image. */
  test("on the compact map a card header's parts hide with their box"):
    assert(css.contains(".map-compact .card-header { display: contents; }"))
    assert(css.contains(".map-compact .card-emblems { display: contents; }"))
    assert(rule(".map-compact .card-header.printed > *,\n" +
      ".map-compact .card-header.printed > .card-emblems > *")
      .contains("opacity: var(--printed-opacity, 1)"))

  /** A Vision's image is landscape and its back portrait. Whichever does
    * not match the box it is in is turned a quarter and sized to the
    * box: its width is the box's height. */
  test("a Vision's face is turned in its denizen box, and its back in " +
      "the overlay's landscape box"):
    val turned = rule(".card-face-vision:not(.card-face-down) > .art,\n" +
      ".card-face-vision > .card-hover-summary > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% / (var(--ratio-denizen)))",
      "height: calc(100% * (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(turned.contains(part), part))
    val upright = rule(
      ".card-overlay-body > .card-face-vision:not(.card-face-down) > .art")
    Vector("inset: 0", "width: 100%", "height: 100%", "transform: none")
      .foreach(part => assert(upright.contains(part), part))
    val back = rule(
      ".card-overlay-body > .card-face-vision.card-face-down > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% * (var(--ratio-denizen)))",
      "height: calc(100% / (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(back.contains(part), part))

  /** The declarations of every rule whose selector list names
    * `selector`. */
  private def rulesNaming(selector: String): Vector[String] =
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    rules.findAllMatchIn(uncommented).filter(_.group(1).split(",")
      .exists(_.trim == selector)).map(_.group(2)).toVector

  test("the Settings button and Close wear the Control face, with the " +
      "touch size"):
    Vector(".settings-toggle", ".settings-close").foreach { selector =>
      val face = rulesNaming(selector).mkString(" ")
      Vector("background: var(--raised)", "border: 1px solid var(--brass-line)",
        "border-radius: 4px", "min-height: 30px", "min-height: 44px")
        .foreach(part => assert(face.contains(part), s"$selector $part"))
    }

  test("the settings dialog sits where the development panel does and " +
      "hides when hidden"):
    val panel = rule(".settings-panel")
    Vector("position: fixed", "z-index: 50", "right: 12px", "top: 48px")
      .foreach(part => assert(panel.contains(part), part))
    assert(rule(".settings-panel[hidden]").contains("display: none"))

  test("the switch is a full-width control of touch height; on, it wears " +
      "the selection colours; withheld, it dims"):
    val control = rule(".setting-switch")
    Vector("width: 100%", "min-height: 44px", "background: var(--raised)",
      "border: 1px solid var(--brass-line)")
      .foreach(part => assert(control.contains(part), part))
    val on = rule(""".setting-switch[aria-checked="true"]""")
    Vector("border-color: var(--selection)",
      "background: var(--selection-fill)")
      .foreach(part => assert(on.contains(part), part))
    assert(rule(""".setting-switch[aria-disabled="true"]""")
      .contains("opacity: 0.5"))

  test("a hidden reason takes no room"):
    assert(rule(".setting-reason[hidden]").contains("display: none"))

  /** The idle end die is printed and dimmed. `.printed` sets opacity
    * too, later in the sheet, so the dimming carries the art layer's
    * answer itself (slice 1 review). */
  test("the idle end die stays dimmed, and hides with the mat"):
    assert(rule(".end-die-idle.printed")
      .contains("opacity: calc(0.4 * var(--printed-opacity, 1))"))
    assert(css.indexOf(".end-die-idle.printed {") >
      css.indexOf(".printed { opacity"))
    assert(!css.contains(".end-die-idle { opacity"))
  /** The frontend's renderer sources, read from disk as the stylesheet
    * is: the frontend's own test runner has no file access. */
  private val rendererSources: String =
    val folder = Path.of("frontend", "src", "main", "scala", "distantlands",
      "frontend")
    val listing = Files.list(folder)
    try
      import scala.jdk.CollectionConverters.*
      listing.iterator().asScala.filter(_.toString.endsWith(".scala"))
        .map(Files.readString(_)).mkString("\n")
    finally listing.close()

  /** Every class a renderer gives an element together with `printed`:
    * the other words of each string literal that holds the word, with a
    * `$`{...}` interpolation cut to the stem before it. An element marked
    * with `classList.add("printed")` writes no such literal, so its
    * classes are listed by hand below; add a class there when a renderer
    * marks an element that way. */
  private val printedTogetherWith: Set[String] =
    val literal = "\"([^\"\\n]*\\bprinted\\b[^\"\\n]*)\"".r
    val written = literal.findAllMatchIn(rendererSources)
      .flatMap(_.group(1).replaceAll("\\$\\{[^}]*\\}", " ").split("\\s+"))
      .map(_.stripSuffix("-")).filter(word =>
        word.matches("[a-z][a-z0-9-]*") && word != "printed").toSet
    // The favor bank's suit glyph (`WorldBoardRenderer.favorBanks`).
    written ++ Set("token-glyph")

  /** `.printed`'s opacity is the art layer's. A rule that sets opacity on
    * an element that is also printed would win or lose against it by
    * source order, so it must carry the layer's answer itself, by
    * multiplying with `--printed-opacity`. The slice 1 review found the
    * idle end die undimmed for want of this. */
  test("every opacity rule on a class drawn together with printed " +
      "multiplies by the art layer's answer"):
    assert(printedTogetherWith("end-die-idle"), printedTogetherWith)
    assert(printedTogetherWith("card-header"), printedTogetherWith)
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    val setsOpacity = """(?<![-\w])opacity\s*:""".r
    val offenders = rules.findAllMatchIn(uncommented).filter { found =>
      val declarations = found.group(2)
      setsOpacity.findFirstIn(declarations).nonEmpty &&
        !declarations.contains("var(--printed-opacity") &&
        found.group(1).split(",").exists { selector =>
          // The selector's subject: its last compound.
          val subject = selector.trim.split("""[\s>+~]+""").last
          """\.([a-z][a-z0-9-]*)""".r.findAllMatchIn(subject)
            .exists(name => printedTogetherWith(name.group(1)))
        }
    }.map(_.group(1).trim).toVector
    assertEquals(offenders, Vector.empty)

