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
    Vector("background: var(--chip-ground)", "border-radius",
      "box-shadow: 0 0 0 0.3em var(--chip-ground)")
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

  /** Words on a site's art keep a ground (R34, rewritten by R51): opaque
    * Base, a fill and a shadow of one colour, so nothing moves. A piece's
    * picture has none. */
  test("what lies on a site's card or on the mat in play keeps a ground " +
      "over the art, without moving"):
    val tokens = rule(".site.art-ready .site-tokens")
    // Opaque Base, not the name band's translucent fill: the black
    // player's colour measured 4.17 : 1 on the translucent ground over
    // white art; on Base every player colour is 6.6 : 1 or better.
    Vector("background: var(--chip-ground)", "box-shadow: 0 0 0 3px var(--chip-ground)",
      "border-radius: 3px").foreach(part =>
      assert(tokens.contains(part), part))
    // A pawn and a warband entry each stand on one ground, behind the
    // picture and its words together (R61, which rewrites R51). The
    // pawn's is a layer of its own inside the pawn, so an offered pawn
    // has it too and keeps its ring and its selected fill, which are the
    // pawn's own paint; the layer ends inside the ring.
    val grounds = rule(".site.art-ready .site-force")
    assert(grounds.contains("background: var(--chip-ground)"))
    assert(rule(".site.art-ready .pawn-ground")
      .contains("background: var(--chip-ground)"))
    val words = rule(".site.art-ready :is(.pawn-name, .site-force-count)")
    assert(words.contains("text-shadow: var(--chip-text-edge)"))
    assert(rule(".site.art-ready .site-force")
      .contains("box-shadow: 0 0 0 4cqh var(--chip-ground)"))
    val layer = rule(".mat-board .pawn-ground")
    Vector("position: absolute", "inset: 0", "z-index: -1",
      "border-radius: inherit").foreach(part =>
      assert(layer.contains(part), part))
    Vector("position: relative", "isolation: isolate").foreach(part =>
      assert(rule(".mat-board .pawn").contains(part), part))
    assert(rule(""".mat-board .pawn[class*="board-option"] > .pawn-ground""")
      .contains("inset: var(--option-ring)"))
    Vector(tokens, grounds, rule(".site.art-ready .pawn-ground")).foreach(
      ground =>
      Vector("padding", "margin", "width", "height", "border:").foreach(
        part => assert(!ground.contains(part), part)))
    // No art rule paints the pawn itself, its row or the warbands' row.
    Vector(".site.art-ready .site-pawns", ".site.art-ready .pawn {",
      ".site.art-ready .pawn:not(", ".site.art-ready .pawn-piece {",
      ".site.art-ready .site-warbands").foreach(gone =>
      assert(!css.contains(gone), gone))
    // The tracker's lines are chips on Base over the mat (R41): the
    // owner could not read them through a halo.
    val line = rule(".mat-board.art-ready .track-note > p")
    Vector("background: var(--chip-ground)", "box-shadow: 0 0 0 0.25em var(--chip-ground)",
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

  test("a pawn's drawn disc hides under its picture and keeps its room"):
    assert(rule(".pawn-piece.art-ready::before").contains("opacity: 0"))
    val piece = rule(".mat-board .pawn-piece")
    Vector("position: relative", "width: 100%", "height: 73cqh")
      .foreach(part => assert(piece.contains(part), part))
    assert(rule(".mat-board .pawn-piece::before")
      .contains("background: currentColor"))

  test("the black pawn and the black warband get a light edge (R27)"):
    val edge = rule(""".token-warband > .art[href$="warband-black.png"],""" +
      "\n" + """.pawn-piece > .art[src$="pawn-black.png"]""")
    assert(edge.contains("filter: drop-shadow(0 0 1px var(--cream))"))
    // On a site the black pieces stand on the chips' dark ground, where
    // one pixel of Cream was not enough to make out the silhouette (R66):
    // a brighter, doubled edge there. Other colours are left alone.
    val onGround = rule(
      """.site.art-ready .site-force .art[href$="warband-black.png"],""" +
      "\n" + """.site.art-ready .pawn-piece > .art[src$="pawn-black.png"]""")
    assert(onGround.contains("filter: drop-shadow(0 0 1px var(--cream-bright)) " +
      "drop-shadow(0 0 2px var(--cream-bright))"))
    assertEquals(css.split("black.png", -1).length - 1, 4)

  /** The pieces on a site are a share of the site at every zoom (R48 to
    * R52): each row is a container of its own size, and every length in
    * it is a share of the row. No rule sets a piece against the map's
    * scale or the glance size. */
  test("a site's pieces are sized as shares of their rows, at every zoom"):
    val pawns = rule(".mat-board .site-pawns")
    assert(pawns.contains("container-type: size"))
    assert(!pawns.contains("wrap"))
    val pawn = rule(".mat-board .pawn")
    Vector("flex: 0 0 calc(100% / 6)", "flex-direction: column",
      "font-size: 20cqh").foreach(part => assert(pawn.contains(part), part))
    val name = rule(".mat-board .pawn-name")
    Vector("overflow: hidden", "text-overflow: ellipsis",
      "white-space: nowrap").foreach(part => assert(name.contains(part), part))
    assert(rule(".mat-board .site-warbands").contains("container-type: size"))
    assert(rule(".mat-board .site-force").contains("font-size: 55cqh"))
    // Up to five pictures stand side by side in the row (R63): five of
    // the row's height and four gaps are 5.12 of it, and the row is six
    // wide.
    assert(rule(".mat-board .site-force").contains("gap: 3cqh"))
    assert(!rule(".mat-board .site-force").contains("wrap"))
    val warband = rule(".mat-board .site-force > .token-warband")
    Vector("width: 100cqh", "height: 100cqh")
      .foreach(part => assert(warband.contains(part), part))
    // R50: the name is hidden from sight on the tiny map alone.
    assert(rule(".map-tiny .mat-board .pawn-name")
      .contains("clip-path: inset(50%)"))
    assert(!css.contains(".map-compact .mat-board .pawn"))
    // The old forms and their steps against the glance size are gone.
    Vector(".pawn-dot", ".pawn-long", ".pawn-initial", ".site-forces",
      ".site-powers", ".map-tiny .mat-board .site-pawns",
      ".map-compact .mat-board .site-pawns").foreach(gone =>
      assert(!css.contains(gone), gone))
    val sized = """(?s)\.(?:pawn|pawn-piece|pawn-name|site-force|""" +
      """site-warbands|site-pawns)[^{}]*\{[^{}]*(glance|map-scale)"""
    assert(sized.r.findFirstIn(uncommented).isEmpty)

  test("the Visions Drawn marker, the Usurper's mark and the festival " +
      "plate hide what they draw when their picture is ready"):
    assert(rule(".vision-marker.art-ready::after").contains("opacity: 0"))
    val limiter = rule(".round-tracker .limiter-marker.art-ready,\n" +
      ".round-tracker .limiter-marker.art-ready + .limiter-diamond")
    assert(limiter.contains("fill: transparent"))
    assert(limiter.contains("stroke: transparent"))
    assert(rule(".festival-plate-printed.art-ready > :not(.art)")
      .contains("opacity: 0"))

  /** A site's powers are buttons on the shield the card prints (R53,
    * R54): drawn, each is an outlined box with its label, cut where it
    * does not fit; over the site's art the outline goes with the print
    * and nothing else is painted, so the print shows through. The hit
    * area is the button's box alone. */
  test("a power button is its part of the shield: outlined when drawn, " +
      "clear over the art"):
    val drawn = rule(".mat-board .site-power")
    Vector("border: 1px solid var(--line)", "overflow: hidden",
      "padding: 0 3cqw", "font-size: 15cqh")
      .foreach(part => assert(drawn.contains(part), part))
    assert(rule(".mat-board .site-shield").contains("container-type: size"))
    val label = rule(".mat-board .site-power > .printed")
    Vector("overflow: hidden", "text-overflow: ellipsis",
      "white-space: nowrap").foreach(part => assert(label.contains(part), part))
    assertEquals(rule(".site.art-ready .site-power").replaceAll("\\s+", " "),
      ".site.art-ready .site-power { border-color: transparent; ")
    val ring = rule(".mat-board .site-power:is(:hover, :focus-visible)")
    assert(ring.contains("outline: var(--option-ring) solid " +
      "var(--cream-focus)"))
    // No pseudo-element or padding box grows the target past the print.
    assert(!css.contains(".site-power::"))
    assert(!rule(".site-power").contains("min-width"))
    // The drawn name starts right of the shield.
    assert(rule(".mat-board .site-name").contains(
      "padding-left: calc(var(--shield-reach, 0%) + 6px)"))

  /** The review's finding: the requirement's footer spanned the face's
    * width over the name band, came after the shield in the markup and
    * covered the top third of every power button on a site with a
    * requirement. The footer has no action: it takes no pointer, stands
    * at the face's right at its own width, and ends short of the shield. */
  test("the requirement's footer does not cover the power shield"):
    val footer = rule(".mat-board .site-footer")
    Vector("pointer-events: none",
      "right: calc(100% - var(--face-left) - var(--face-w) + 8px)",
      "max-width: calc(var(--face-w) - var(--shield-reach, 0%) - 16px)")
      .foreach(part => assert(footer.contains(part), part))
    assert(!footer.contains("left:"), footer)
    assert(!"""(?<![-\w])width\s*:""".r.findFirstIn(footer).nonEmpty, footer)

  /** The review's finding: on the tiny map a cost badge moved to the
    * face's left foot, over the shield. It starts right of the shield's
    * reach and keeps inside the row. */
  test("on the tiny map a cost badge starts right of the power shield"):
    val badge = rule(".map-tiny .mat-board .site > .target-detail-badge")
    Vector("left: calc(var(--face-left) + var(--shield-reach, 0%))",
      "max-width: calc(100% - var(--shield-reach, 0%))", "right: auto")
      .foreach(part => assert(badge.contains(part), part))

  /** Over the mat an empty slot's dashed line was lost (R44). */
  test("an empty card slot shows on the mat, and an offered one keeps " +
      "its ring"):
    val slot = rule(
      """.mat-board.art-ready .card-slot-empty:not([class*="board-option"])""")
    Vector("border-color: var(--cream-bright)",
      "box-shadow: inset 0 0 0 calc(2px / var(--map-scale, 1)) var(--base)")
      .foreach(part => assert(slot.contains(part), part))
    // One pixel on the board is a sixth of one on a phone's screen at
    // Fit, so the line's width follows the map's scale, with art on and
    // off alike (R44). The slot's box is border-box: it does not grow.
    // The owner asked for both lines twice as thick (R64).
    assert(rule(".mat-board .card-slot-empty")
      .contains("border-width: calc(2px / var(--map-scale, 1))"))
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
    Vector("background: var(--chip-ground)", "border-radius: 0.3em",
      "box-shadow: 0 0 0 0.3em var(--chip-ground)")
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

  /** The review's finding: the site card grew past its row by the
    * frame's line (R38) and an offered site's ring stayed on the row, 8
    * mat px inside the card on three sides. The ring's layer reaches the
    * card's edge on those sides: the face's own offsets in the row, which
    * are negative, and equal at the top and the foot. */
  test("an offered site's ring runs along its grown card's edge"):
    val ring = rule(".mat-board .site.board-option::after")
    Vector("top: var(--face-bottom, 0)", "bottom: var(--face-bottom, 0)",
      "left: var(--face-left, 0)")
      .foreach(part => assert(ring.contains(part), part))
    assert(!ring.contains("right"), ring)
    assert(css.indexOf(".mat-board .site.board-option::after {") >
      css.indexOf(".mat-board :is(.site, .deck-body).board-option::after {"))

  /** The review's finding: the favor filling a bank's circle (R42)
    * covered part of an offered bank's ring. The ring is drawn on a
    * layer over the favor, as a site's is; the bank's own box draws
    * none. Its hover glow stays inside the ring, since the board's foot
    * clips what lies outside. */
  test("an offered favor bank's ring is drawn over its favor"):
    assert(rule(".mat-board .favor-bank.board-option")
      .contains("box-shadow: none"))
    val ring = rule(".mat-board .favor-bank.board-option::after")
    Vector("content: \"\"", "position: absolute", "inset: 0", "z-index: 1",
      "border-radius: inherit", "pointer-events: none",
      "box-shadow: inset 0 0 0 var(--option-ring) var(--option-line)")
      .foreach(part => assert(ring.contains(part), part))
    val hover = rule(".mat-board .favor-bank.board-option:is(:hover, " +
      ":focus-visible,\n  .board-option-hover)::after")
    assert(hover.contains(
      "inset 0 0 0 calc(var(--option-ring) * 3) var(--option-glow)"))
    assert(rule(".mat-board .favor-bank:is(.board-option-selected, " +
      ".board-option-locked,\n  .board-option-played)::after").contains(
      "box-shadow: inset 0 0 0 var(--option-ring) var(--selection)"))
    assert(rule(".mat-board .favor-bank.board-option-askable::after")
      .contains("var(--consent-amber)"))
    assert(rule(".mat-board .favor-bank.board-option-unavailable::after")
      .contains("box-shadow: none"))
    // No ring is left on the bank's own box, under the favor.
    assert(!css.contains(".mat-board .favor-bank.board-option:is(:hover, " +
      ":focus-visible,\n  .board-option-hover) {"))

  /** The owner's second review (R59, R60): the chips over art are
    * see-through, 65% opaque, from one setting. `--chip-ground` is Base
    * at `--chip-ground-alpha`; every chip ground uses it for its fill
    * and its halo, and a chip's words carry a Base edge so they read on
    * whatever shows through. */
  test("the chips over art share one see-through ground, set once"):
    assertEquals(css.split("--chip-ground-alpha:", -1).length - 1, 1)
    assert(css.contains("--chip-ground-alpha: 0.65;"))
    assert(css.contains("--chip-ground: color-mix(in srgb, var(--base) " +
      "calc(var(--chip-ground-alpha) * 100%), transparent);"))
    assert(css.contains("--chip-text-edge: 0 0 0.08em var(--base), " +
      "0 0 0.2em var(--base);"))
    val chips = Vector(".mat-board .favor-bank-count",
      ".deck-card.art-ready > .pile-count",
      ".mat-board.art-ready .deck-card.pile-empty > .pile-count",
      ".site.art-ready .site-tokens", ".mat-board.art-ready .track-note > p")
    chips.foreach { selector =>
      val chip = rule(selector)
      assert(chip.contains("background: var(--chip-ground)"), selector)
      assert(chip.contains("text-shadow: var(--chip-text-edge)"), selector)
      assert(!chip.contains("var(--base)"), selector)
    }
    // No ground over art is opaque Base any more.
    assert(!artRules.contains("background: var(--base)"))

  /** The slice 6 review: on the 65% ground over bright art the black
    * player's colour measured 2.6 : 1 and brown's 3.1 : 1. Over art,
    * words on a see-through chip are Bright Cream, whatever player they
    * name (R65): the piece's picture carries the colour. The ground is
    * computed over white art, the worst a chip can lie on, at the alpha
    * the stylesheet sets. */
  test("every word on a see-through chip is Bright Cream, which reads " +
      "on the ground over white art"):
    val alpha = """--chip-ground-alpha: ([0-9.]+);""".r.findFirstMatchIn(css)
      .getOrElse(fail("no chip ground alpha")).group(1).toDouble
    val base = colour("base")
    def overWhite(channel: Int): Int =
      math.round(alpha * channel + (1 - alpha) * 255).toInt
    val ground = (overWhite(base._1), overWhite(base._2), overWhite(base._3))
    assert(contrast(colour("cream-bright"), ground) >= 4.5,
      contrast(colour("cream-bright"), ground))
    // A player's colour would not do: black's and brown's fail there.
    assert(contrast(colour("player-black"), ground) < 4.5)
    assert(contrast(colour("player-brown"), ground) < 4.5)
    Vector(".mat-board .favor-bank-count",
      ".deck-card.art-ready > .pile-count",
      ".mat-board.art-ready .deck-card.pile-empty > .pile-count",
      ".site.art-ready .site-tokens", ".mat-board.art-ready .track-note > p",
      ".card-face.banner-face > .card-tokens",
      ".site.art-ready :is(.pawn-name, .site-force-count)",
      ".site.art-ready .pawn-name .player-ref,\n" +
        ".mat-board.art-ready .track-note .player-ref").foreach(selector =>
      assert(rule(selector).contains("color: var(--cream-bright)"), selector))
    // No other rule gives a chip's words a colour over art.
    val colours = """(?<![-\w])color\s*:\s*([^;}]+)""".r
    assertEquals(colours.findAllMatchIn(artRules.replaceAll(
      "(?s)/\\*.*?\\*/", "")).map(_.group(1).trim).toSet
      .filter(_.contains("player")), Set.empty[String])

  /** The owner's second review (R62): what a banner holds was hard to
    * see from afar. It stands in a chip at the middle of the face, a
    * third down from its top, sized as a share of the face so it reads at
    * desktop Fit as a bank's count does (24% of a 57px banner is about
    * 14px). The same box in both modes: the rule names no art state. */
  test("a banner's resources stand in a chip a third down its face, " +
      "sized from the face"):
    assert(css.contains(".banner-face { container-type: inline-size; }"))
    val chip = rule(".card-face.banner-face > .card-tokens")
    Vector("top: 33%", "left: 50%", "right: auto", "bottom: auto",
      "transform: translate(-50%, -50%)", "width: max-content",
      "height: auto", "background: var(--chip-ground)",
      "text-shadow: var(--chip-text-edge)", "font-size: 24cqw")
      .foreach(part => assert(chip.contains(part), part))
    assert(!chip.contains("art-ready"))
    // The drawn name stands at the face's foot, under the chip, which
    // would cover it in the face's middle.
    assert(rule(".card-face.banner-face").contains("justify-content: flex-end"))
    // The overlay's face is large; its chip is a smaller share of it.
    assert(rule(".card-overlay-body > .card-face.banner-face > .card-tokens")
      .contains("font-size: 12cqw"))

  /** A colour variable's value, as `#rrggbb`. */
  private def colour(name: String)(using munit.Location): (Int, Int, Int) =
    val hex = s"--$name: #([0-9a-f]{6});".r.findFirstMatchIn(css)
      .getOrElse(fail(s"no colour --$name")).group(1)
    (Integer.parseInt(hex.substring(0, 2), 16),
      Integer.parseInt(hex.substring(2, 4), 16),
      Integer.parseInt(hex.substring(4, 6), 16))

  /** The WCAG contrast ratio of two sRGB colours. */
  private def contrast(one: (Int, Int, Int), other: (Int, Int, Int)): Double =
    def luminance(value: (Int, Int, Int)): Double =
      def linear(channel: Int): Double =
        val share = channel / 255.0
        if share <= 0.03928 then share / 12.92
        else math.pow((share + 0.055) / 1.055, 2.4)
      0.2126 * linear(value._1) + 0.7152 * linear(value._2) +
        0.0722 * linear(value._3)
    val (light, dark) = (luminance(one) max luminance(other),
      luminance(one) min luminance(other))
    (light + 0.05) / (dark + 0.05)

  /** The review's finding on the drawn wheel: a number must read at
    * 4.5 : 1 on whatever lies under it. The diamond stands clear of it
    * (`RoundWheelSuite`), so its ground is its wedge's fill, the
    * segment's or the limiter's tint over it; the current round's number
    * lies on the round marker at the wedge's centre (R57), in Base on
    * the marker's silver. */
  test("a round's number reads on each wedge fill the drawn wheel has"):
    val cream = colour("cream-bright")
    val site = colour("site")
    assert(rule(".round-tracker text").contains("fill: var(--cream-bright)"))
    assert(rule(".round-segment").contains("fill: var(--site)"))
    assert(contrast(cream, site) >= 4.5)
    val limiter = rule(".round-tracker .limiter-marker")
    assert(limiter.contains("fill: #ff8c7f"))
    assert(limiter.contains("fill-opacity: 0.45"))
    def over(tint: Int, ground: Int): Int =
      math.round(0.45 * tint + 0.55 * ground).toInt
    assert(contrast(cream, (over(0xff, site._1), over(0x8c, site._2),
      over(0x7f, site._3))) >= 4.5)
    assert(rule(".round-current").contains("fill: var(--brass)"))
    assert(rule(".round-tracker .round-number-current")
      .contains("fill: var(--base)"))
    assert(contrast(colour("base"), colour("round-marker")) >= 4.5)
    assert(rule(".round-marker").contains("fill: var(--round-marker)"))
    // The diamond is small enough for the wedge's rim.
    assert(rule(".mat-board .round-tracker .limiter-diamond")
      .contains("font-size: 12px"))

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
      "border-radius: 50%", "background: var(--chip-ground)", "font-size: 44cqw")
      .foreach(part => assert(count.contains(part), part))
    assert(!count.contains("padding"))
    // On the far zoomed-out map the circle is some 11px across on a
    // phone: there the count takes the whole circle, to stay near 9px.
    val tiny = rule(".map-tiny .mat-board .favor-bank-count")
    Vector("width: 100cqw", "height: 100cqw", "font-size: 78cqw")
      .foreach(part => assert(tiny.contains(part), part))
    // Two figures at that size are wider than the disc: 52cqw sets them
    // about 62cqw wide, inside the circle.
    assert(rule(".map-tiny .mat-board .favor-bank-count[data-figures=\"2\"]")
      .contains("font-size: 52cqw"))
    // The old grid, which sized a bank's parts from the text, is gone.
    assert(!css.contains(".mat-board .favor-bank { display: grid"))
    assert(!css.contains(".favor-bank > .token-favor"))

  test("a banner's face in its overlay is as large as a card's (R43)"):
    val face = rule(".card-overlay-body > .banner-face")
    Vector("width: var(--card-h)", "height: var(--card-h)")
      .foreach(part => assert(face.contains(part), part))
    assert(rule(".card-overlay-body > .card-face").contains("font-size: 1.9em"))

  /** The Oath goal's card on the table's edge (R37): the goal's ratio at
    * the banners' height. */
  test("the Oath goal's card is as tall as a banner on the table's edge"):
    val card = rule(".table-edge .oathkeeper-status > .oathkeeper-goal")
    Vector("height: var(--banner-edge-side)", "overflow: hidden",
      "cursor: pointer").foreach(part => assert(card.contains(part), part))
    assert(rule(".card-plate-oath-goal")
      .contains("aspect-ratio: var(--ratio-oath-goal)"))
    // It is no longer a line of link text.
    assert(!css.contains(".oathkeeper-status > .oathkeeper-goal:not(:hover)"))

  /** The review's finding: the card's own font size outranked the edge's
    * glance rule, so the drawn goal's title was about 2px at a phone's
    * Fit. Its words follow the edge's size (em, not rem), and zoomed out
    * it shows its short name at the glance size, as a banner does. */
  test("the drawn Oath goal keeps a readable name on the zoomed-out edge"):
    val card = rule(".table-edge .oathkeeper-status > .oathkeeper-goal")
    assert(card.contains("font-size: 0.8em"), card)
    assert(!card.contains("rem;"), card)
    assert(rule(".map-compact .table-edge .oathkeeper-goal")
      .contains("font-size: var(--glance-text)"))
    assert(rule(".map-compact .table-edge .oathkeeper-goal > .card-name")
      .contains("display: none"))
    val short = rule(
      ".map-compact .table-edge .oathkeeper-goal > .card-name-short")
    Vector("max-height: none", "font-size: 1em")
      .foreach(part => assert(short.contains(part), part))
    // The card's ring on hover and focus, inside its own box.
    val ring = rule(".table-edge .oathkeeper-goal:hover, " +
      ".table-edge .oathkeeper-goal:focus-visible")
    Vector("outline: var(--option-ring) solid var(--cream-focus)",
      "outline-offset: calc(var(--option-ring) * -1.5)")
      .foreach(part => assert(ring.contains(part), part))

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

