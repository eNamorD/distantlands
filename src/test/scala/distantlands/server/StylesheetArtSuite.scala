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
  private val boxes =
    ".card-face, .card-hover-summary, .site, .deck-card, .mat-board, " +
      ".card-plate"

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
  test("a pile on its side turns its back a quarter, as a Vision is " +
      "turned, and sizes it to the pile"):
    val turned = rule(".mat-board .deck-sideways > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% * (var(--ratio-denizen)))",
      "height: calc(100% / (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(turned.contains(part), part))

  test("a pile's count stays readable over its back"):
    val count = rule(".deck-card.art-ready > .pile-count")
    Vector("background: var(--base)", "border-radius",
      "box-shadow: 0 0 0 0.3em var(--base)")
      .foreach(part => assert(count.contains(part), part))
    // A ground that takes no room: the count's box is the same with art
    // and without (slice 2 browser check).
    assert(!count.contains("padding"))

  test("the mat's image fills the board"):
    assert(rule(".mat-board > .art").contains("object-fit: fill"))

  test("a plate is a positioned box, so its image fills it"):
    assert(rule(".card-plate:has(> .art)").contains("position: relative"))

  /** Found in the slice 2 browser check: a bank's own pill covered the
    * emblem and the circle the mat prints for it. */
  test("over the mat a favor bank drops its own pill"):
    val bank = rule(
      """.mat-board.art-ready .favor-bank:not([class*="board-option"])""")
    Vector("border-color: transparent", "background: none")
      .foreach(part => assert(bank.contains(part), part))

  /** Found in the slice 2 browser check: forces and tokens were hard to
    * read on a site's art. The ground is a fill and a shadow of one
    * colour, the name band's, so nothing moves. */
  test("what lies on a site's card or on the mat in play keeps a ground " +
      "over the art, without moving"):
    val onSite = rule(".site.art-ready :is(.site-forces, " +
      ".site-forces-short, .site-tokens)")
    // Opaque Base, not the name band's translucent fill: forces and pawns
    // wear player colours, and the black player's measured 4.17 : 1 on
    // the translucent ground over white art; on Base every player colour
    // is 6.6 : 1 or better (slice 3, measured).
    Vector("background: var(--base)", "box-shadow: 0 0 0 3px var(--base)",
      "border-radius: 3px").foreach(part =>
      assert(onSite.contains(part), part))
    Vector("padding", "margin", "width", "height", "border:").foreach(part =>
      assert(!onSite.contains(part), part))
    // A pawn can be an option, so the ground is its row's, and the pawn
    // keeps its own background and shadow for its option states.
    // The ground hugs each pawn (R35); a pawn a decision offers is left
    // alone, so its option states keep its background and shadow.
    val pawns = rule(
      """.site.art-ready .pawn:not([class*="board-option"])""")
    assert(pawns.contains("background: var(--base)"))
    assert(!css.contains(".site.art-ready .site-pawns"))
    Vector("box-shadow", "padding", "margin", "width", "height", "border:")
      .foreach(part => assert(!pawns.contains(part), part))
    // The tracker's lines are chips on Base over the mat (R41): the
    // owner could not read them through a halo.
    val line = rule(".mat-board.art-ready .track-note > p")
    Vector("background: var(--base)", "box-shadow: 0 0 0 0.25em var(--base)",
      "border-radius: 0.2em").foreach(part =>
      assert(line.contains(part), part))
    Vector("padding", "margin", "width", "height").foreach(part =>
      assert(!line.contains(part), part))
    assert(!css.contains("text-shadow: 0 0 3px var(--base)"))

  test("a pile on the mat that holds an image is a positioned box"):
    assert(rule(".mat-board .deck-card:has(> .art)")
      .contains("position: relative"))

  /** A glyph keeps its svg and holds its image inside it (R22). */
  test("a ready glyph hides its sprite, and a ready restriction banner " +
      "its drawn parts but not its chain"):
    assert(rule(".token-glyph.art-ready > use").contains("opacity: 0"))
    assert(rule(".restriction-mark.art-ready > " +
      ":not(.art, .restriction-mark-chain)").contains("opacity: 0"))

  test("a pawn's dot gives up its ink to its picture and keeps its room"):
    assert(rule(".pawn-dot:has(> .art)").contains("position: relative"))
    assert(rule(".pawn-dot.art-ready").contains("color: transparent"))

  test("the black pawn and the black warband get a light edge (R27)"):
    val edge = rule(""".token-warband > .art[href$="warband-black.png"],""" +
      "\n" + """.pawn-dot > .art[src$="pawn-black.png"]""")
    assert(edge.contains("filter: drop-shadow(0 0 1px var(--cream))"))

  test("the Visions Drawn marker, the Usurper's mark and the festival " +
      "plate hide what they draw when their picture is ready"):
    assert(rule(".vision-marker.art-ready::after").contains("opacity: 0"))
    val limiter = rule(".round-tracker .limiter-marker.art-ready,\n" +
      ".round-tracker .limiter-marker.art-ready + .limiter-diamond")
    assert(limiter.contains("fill: transparent"))
    assert(limiter.contains("stroke: transparent"))
    assert(rule(".festival-plate-printed.art-ready > :not(.art)")
      .contains("opacity: 0"))

  /** The owner asked for no power words over a site's art (R45, which
    * replaces R20): the label hides with the rest of the print and the
    * button keeps its place. */
  test("on a ready site a power button's label hides with the print"):
    assert(!css.contains(".site.art-ready .site-power"))

  /** Over the mat an empty slot's dashed line was lost (R44). */
  test("an empty card slot shows on the mat, and an offered one keeps " +
      "its ring"):
    val slot = rule(
      """.mat-board.art-ready .card-slot-empty:not([class*="board-option"])""")
    Vector("border-color: var(--cream-bright)",
      "box-shadow: inset 0 0 0 1px var(--base)")
      .foreach(part => assert(slot.contains(part), part))
    Vector("padding", "margin", "width", "height").foreach(part =>
      assert(!slot.contains(part), part))

  test("an empty pile on a ready mat drops its fill, so the mat's slot " +
      "shows (R21)"):
    val empty = rule(".mat-board.art-ready .deck-card.pile-empty")
    Vector("border-color: transparent", "background: none")
      .foreach(part => assert(empty.contains(part), part))

  test("a die face's crop is a square as tall as its chip, centred, " +
      "showing only the face; the chip keeps its size (R25)"):
    val chip = rule(".die-face:has(> .art-crop)")
    assert(chip.contains("position: relative"))
    val crop = rule(".art-crop")
    Vector("position: absolute", "top: 0", "bottom: 0", "left: 50%",
      "aspect-ratio: 1", "overflow: hidden", "transform: translateX(-50%)")
      .foreach(part => assert(crop.contains(part), part))
    val sheet = rule(".art-crop > .art")
    Vector("z-index: auto", "max-width: none", "object-fit: fill",
      "right: auto", "bottom: auto")
      .foreach(part => assert(sheet.contains(part), part))
    // The glyphs hide; the doubler's own "×2" too, since its side of the
    // sheet prints it.
    assert(rule(".die-face.art-ready > svg").contains("opacity: 0"))
    assert(rule(".die-face.art-ready").contains("color: transparent"))
    Vector("width", "height", "padding", "margin").foreach(part =>
      assert(!rule(".die-face.art-ready").contains(part), part))

  /** The title files are the landscape card stored on its side (seen in
    * the slice 3 browser check), so the image is turned a quarter and
    * fitted to the plate, which keeps its ratio (R28). */
  test("the title's image is turned upright and fitted to its plate"):
    val title = rule(".card-plate-oathkeeper-title > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% / (var(--ratio-oathkeeper-title)))",
      "height: calc(100% * (var(--ratio-oathkeeper-title)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(title.contains(part), part))

  /** `.art` gives every image the whole of its box, and a stylesheet
    * outranks an SVG size attribute: an SVG image with a size of its own
    * must set it as an inline style (`ArtLayer.attachMark`). The slice 3
    * review saw the Usurper's piece drawn as large as the wheel. Glyph
    * images want the whole glyph and need nothing more. */
  test("no art rule but the layer's own sizes an image, so an inline " +
      "size is the only override"):
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    val sizing = rules.findAllMatchIn(uncommented).filter(found =>
      found.group(1).split(",").exists(_.trim.endsWith(".art")) &&
        """(?<![-\w])(width|height)\s*:""".r.findFirstIn(found.group(2))
          .nonEmpty).map(_.group(1).trim.replaceAll("\\s+", " ")).toVector
    assertEquals(sizing, Vector(".art",
      ".card-face-vision:not(.card-face-down) > .art, " +
        ".card-face-vision > .card-hover-summary > .art",
      ".card-overlay-body > .card-face-vision:not(.card-face-down) > .art",
      ".card-overlay-body > .card-face-vision.card-face-down > .art",
      ".mat-board .deck-sideways > .art",
      ".card-plate-oathkeeper-title > .art"))
    val source = Files.readString(Path.of("frontend", "src", "main", "scala",
      "distantlands", "frontend", "ArtLayer.scala"))
    assert(source.contains(
      """s"width: ${width}px; height: ${height}px;""""))

  test("an empty pile's count keeps its ground on a ready mat"):
    val count = rule(
      ".mat-board.art-ready .deck-card.pile-empty > .pile-count")
    Vector("background: var(--base)", "border-radius: 0.3em",
      "box-shadow: 0 0 0 0.3em var(--base)")
      .foreach(part => assert(count.contains(part), part))
    assert(!count.contains("padding"))

  /** The round marker (R40): one new colour, named once. Over a ready
    * mat the current wedge keeps its border and drops its fill, which
    * would hide the wedge the mat prints. */
  test("the round marker is silver, and over the mat the current wedge " +
      "is a border alone"):
    assertEquals(css.split("--round-marker:", -1).length - 1, 1)
    assert(rule(".round-marker").contains("fill: var(--round-marker)"))
    val current = rule(".mat-board.art-ready .round-current")
    assert(current.contains("fill: none"))
    assert(!current.contains("stroke"))
    assert(rule(".round-current").contains("stroke: var(--cream-bright)"))

  /** A site's box is the mat's frame and its line (R38), a little off
    * the card's own ratio, so the image covers it and loses a sliver. */
  test("a site's image covers its box, and its name band stands at the " +
      "face's foot"):
    assert(rule(".site-face > .art").contains("object-fit: cover"))
    assert(rule(".mat-board .site-name")
      .contains("bottom: var(--face-bottom, 0)"))

  /** A bank's parts take their size from the mat, not from the text
    * (R42): the favor fills its circle and the count is as large as its
    * disc allows, at every zoom. */
  test("a bank's favor fills its circle and its count stands over it " +
      "on a Base disc"):
    val token = rule(".mat-board .favor-bank-token")
    assert(token.contains("container-type: size"))
    val favor = rule(".mat-board .favor-bank-token > .token-glyph")
    Vector("position: absolute", "inset: 0", "width: 100%", "height: 100%")
      .foreach(part => assert(favor.contains(part), part))
    val count = rule(".mat-board .favor-bank-count")
    Vector("position: absolute", "top: 50%", "left: 50%",
      "transform: translate(-50%, -50%)", "width: 62cqw", "height: 62cqw",
      "border-radius: 50%", "background: var(--base)", "font-size: 44cqw")
      .foreach(part => assert(count.contains(part), part))
    assert(!count.contains("padding"))
    // The old grid, which sized a bank's parts from the text, is gone.
    assert(!css.contains(".mat-board .favor-bank { display: grid"))
    assert(!css.contains(".favor-bank > .token-favor"))

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

  /** The classes `BoardOptions` can mark as an option (`decorate`,
    * `control`, `card`): a site's row, a pawn, a favor bank, a pile's
    * body, a card or banner face, a card slot and a player's summary. A
    * marked element shows its state with `border-color`, `box-shadow` and
    * `background` (`.board-option` and its states, one class each), so a
    * heavier art rule that sets one of them on such an element would wipe
    * the ring, the hover glow and the selected fill. Add a class here
    * when a renderer starts marking it. */
  private val optionTargets = Set("site", "pawn", "favor-bank", "deck-body",
    "card-face", "banner-face", "card-slot-empty", "player-summary")

  /** The slice 2 review found a pawn offered as a target with no ring
    * over site art: the ground under live state had been set on the pawn
    * itself. */
  test("no art rule paints an element a decision can mark, unless it " +
      "leaves marked ones alone"):
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    val paints = """(?<![-\w])(background|box-shadow|border-color)\s*:""".r
    val block = artRules.replaceAll("(?s)/\\*.*?\\*/", "")
    val offenders = rules.findAllMatchIn(block).filter { found =>
      paints.findFirstIn(found.group(2)).nonEmpty &&
        found.group(1).split(",(?![^(]*\\))").exists { selector =>
          val subject = selector.trim.split("""[\s>+~]+(?![^(]*\))""").last
          // What a `:has()` names is a descendant, not the subject.
          val own = subject.replaceAll(""":has\([^)]*\)""", "")
          !subject.contains(""":not([class*="board-option"])""") &&
            """\.([a-z][a-z0-9-]*)""".r.findAllMatchIn(own)
              .exists(name => optionTargets(name.group(1)))
        }
    }.map(_.group(1).trim).toVector
    assertEquals(offenders, Vector.empty)

  /** Everything the art layer does to the page stands in one block, so a
    * reader, and these guards, see all of it. A stray edit once added a
    * site to three card rules outside it. */
  test("no rule outside the art block names art-ready, and the box list " +
      "stands only in the two rules that set the printed opacity"):
    val from = css.indexOf("/* Licensed art")
    val to = css.indexOf("/* Document level, above .game-pane", from)
    val outside = css.substring(0, from) + css.substring(to)
    assert(!outside.contains("art-ready"))
    assert(!outside.contains("--printed-opacity"))
    assertEquals(css.split(java.util.regex.Pattern.quote(s":is($boxes)"), -1)
      .length - 1, 2)
    // No shorter list of the same boxes hides in another rule.
    assertEquals(css.split(java.util.regex.Pattern.quote(
      ":is(.card-face, .card-hover-summary, .site"), -1).length - 1, 2)

