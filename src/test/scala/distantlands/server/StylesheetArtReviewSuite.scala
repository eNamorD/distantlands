package distantlands.server

/** The art layer's rules that the owner's reviews and their code reviews
 * added (game art slices 4 to 6): option rings on grown boxes, the
 * see-through chips and their words, a banner's resources, the round
 * wheel, the favor banks and the Oath goal's card. `StylesheetArtSuite`
 * holds the layer's own rules and its guards. */
class StylesheetArtReviewSuite extends StylesheetText:
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
    // The slice 6 review: a face holding tokens keeps room for their
    // band at its foot (`:has(> .card-tokens)`), which pushed the drawn
    // name up under the chip. A banner's tokens are no band any more,
    // so its face takes its plain foot back; the rule must outrank the
    // band's and follow it.
    val foot = ".card-face.banner-face:has(> .card-tokens)"
    assert(rule(foot).contains("padding-bottom: 0.4em"))
    val bands = """(?m)^[^{}]*:has\(> \.card-tokens[^{}]*\{[^{}]*padding-bottom""".r
    assert(bands.findAllMatchIn(css).toVector.last.matched.contains(foot),
      "a later band rule outranks the banner's")
    // The overlay's face is large; its chip is a smaller share of it.
    assert(rule(".card-overlay-body > .card-face.banner-face > .card-tokens")
      .contains("font-size: 12cqw"))

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
