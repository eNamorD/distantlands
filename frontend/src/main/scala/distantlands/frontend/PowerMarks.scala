package distantlands.frontend

import org.scalajs.dom
import distantlands.protocol.projection.ModifierMarkProjection

/** The marks a card prints around its powers and over its whole face, drawn
  * as original glyphs after the physical layout rather than its art.
  *
  *  - A modifier is an arch-topped tab holding its action's emblem: warm grey,
  *    or black when the power is persistent, as the card blackens a
  *    persistent modifier with its braid.
  *  - A battle plan is a Campaign modifier with a crowned tab, holding the
  *    Campaign emblem, in its side's colour: Discord red to attack, Order navy
  *    to defend, the two as one gradient for either side. Colour is never the
  *    only cue: the plan's side is also its name, and its chip says it.
  *  - A restriction is a banner hung over the whole card: a bust for adviser
  *    only, a tree for site only, a tower, whole or broken, for an edifice's
  *    face, and a chain across the top when the card is locked.
  *
  * Each mark is one small inline SVG built from paths in the frame and
  * emblems from the sprite, so the frame can take its fill from a class and
  * the emblem its colour from `currentColor`, as the token glyphs do.
  */
private[frontend] object PowerMarks:
  private val Svg = "http://www.w3.org/2000/svg"

  /** Emblems, each on a 24-unit square, filled even-odd so the cut lines
    * (a boot's cuff, a pack's pocket, a scroll's lines) are holes. */
  private val emblems: Vector[(String, String)] = Vector(
    // A horse's head, the mounted soldier of a Campaign.
    "campaign" -> ("M14.5 2.5c-1 0-1.8.5-2.3 1.3L11 3l-.6 2.2C7.8 6.6 6 9 5 " +
      "11.6L3 16l2.2 1.6 2.8-2.2 2.4-.6c.3 1.6-.5 3-1.7 4.3L7.5 21.5h11c.3-" +
      "3.6.5-7.4-.4-10.6-.8-2.8-1.8-5.4-3.6-8.4z" +
      "M11.3 8.3a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2z"),
    // A hand held out under a coin.
    "trade" -> ("M12 1.5a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm0 1.8a2.2 2.2 0 1 1 0 " +
      "4.4 2.2 2.2 0 0 1 0-4.4z" +
      "M2 13.5h3.5l3.2-1.5h4.8a1.6 1.6 0 0 1 0 3.2H10v.6h4.2l5.1-3.6a1.7 1.7 " +
      "0 0 1 2.2 2.5l-6 5.6c-.6.5-1.3.7-2 .7H5.5l-.5.5H2z"),
    // A swallowtailed flag on its staff.
    "muster" -> "M4.5 2h2.2v20H4.5zM7.4 3.2h12.1l-3.3 4.4 3.3 4.4H7.4z",
    // A spyglass drawn out to three tubes, tilted to the far corner.
    "search" -> ("M3.94 17.94 6.06 20.06 9.24 16.88 7.12 14.76z" +
      "M7.19 13.69 10.31 16.81 14.34 12.78 11.22 9.66z" +
      "M11.22 8.54 15.46 12.78 20.91 7.33 16.67 3.09z"),
    // A boot in profile, toe forward: the straight shaft with its cuff cut
    // across it, the foot running out to the toe, on its sole.
    "travel" -> ("M6.5 2h6.5v9l5.9 2.7A3.6 3.6 0 0 1 21.5 17v2.4h-15z" +
      "M6.5 4.6H13v1.3H6.5z" + "M5.5 20.4h17v1.8h-17z"),
    // A pack with its bedroll strapped on top.
    "recover" -> ("M6.5 2.5h11a1.75 1.75 0 0 1 0 3.5h-11a1.75 1.75 0 0 1 0-3.5z" +
      "M8 7h8a3.5 3.5 0 0 1 3.5 3.5V20a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2v-9.5A3.5 " +
      "3.5 0 0 1 8 7z" + "M6 11h12v1.2H6z" + "M8.5 14h7v4.5h-7z"),
    // A written challenge and the quill that wrote it.
    "challenge" -> ("M3 6h9v13h1.5v1a2 2 0 0 1-2 2H3.5a2 2 0 0 1-2-2v-1H3z" +
      "M5 9h5v1H5zM5 12h5v1H5zM5 15h3.5v1H5z" +
      "M22.5 1.5c-3.6.6-6.6 3.1-8.2 6.6l-1.5 3.8.6.6 3.8-1.5c3.5-1.6 6.1-4.6 " +
      "6.7-8.2z"),
    // Head and shoulders: an adviser.
    "adviser" -> ("M12 2.5a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9z" +
      "M3.5 22c0-4.7 3.8-8.5 8.5-8.5s8.5 3.8 8.5 8.5z"),
    // A fir: a place in the world.
    "site" -> ("M12 1.5 5.5 9h3.2L4.5 14.5h3.7L3.5 20h7.3v2.5h2.4V20h7.3l-4.7-" +
      "5.5h3.7L15.3 9h3.2z"),
    "tower" -> ("M5 2h2.8v2.5h2.8V2h2.8v2.5h2.8V2H19v5.5l-2 2V22H7V9.5l-2-2z" +
      "M10.5 22v-4a1.5 1.5 0 0 1 3 0v4z" + "M11 11.5h2v3h-2z"),
    // The same tower with its right side broken away in a falling jagged
    // edge, and a crack run down from the break.
    "tower-ruined" -> ("M5 2h2.8v2.5h2.8V2h1.6l1 2.6 1.5-1 1.3 2.4 1.6-.6 " +
      "1.4 2.1v.5l-2 2V22H7V9.5l-2-2z" +
      "M13.2 8.6l-1.5 3 1.6 2.4-1.3 3.6.9.3 1.4-3.9-1.6-2.4 1.3-2.7z" +
      "M8.6 22v-3.5a1.4 1.4 0 0 1 2.8 0V22z"),
    // Three links, the middle one seen edge on.
    "chain" -> ("M3 9.5h4a2.5 2.5 0 0 1 0 5H3a2.5 2.5 0 0 1 0-5z" +
      "M3 11h4a1 1 0 0 1 0 2H3a1 1 0 0 1 0-2z" +
      "M11 10.6h2a1.4 1.4 0 0 1 0 2.8h-2a1.4 1.4 0 0 1 0-2.8z" +
      "M17 9.5h4a2.5 2.5 0 0 1 0 5h-4a2.5 2.5 0 0 1 0-5z" +
      "M17 11h4a1 1 0 0 1 0 2h-4a1 1 0 0 1 0-2z"))

  private val Arch = "M3 23.5V10a9 9 0 0 1 18 0v13.5z"
  private val ArchKeyline = "M5 23.5V10a7 7 0 0 1 14 0v13.5"
  private val Crown = "M2 23.5V6.5l4.5 3.5L9 3.5l3 3.5 3-3.5 2.5 6.5 4.5-3.5v17z"
  private val Banner = "M3 1h18v15.2L12 23l-9-6.8z"
  private val BannerBody = "M4.7 2.7h14.6v12.6L12 20.9l-7.3-5.6z"

  /** The id of the gradient an either-side plan's crown is filled with. */
  val EitherSideGradient: String = "mark-either-side"

  private val actionNames: Map[String, String] = Map("search" -> "Search",
    "travel" -> "Travel", "campaign" -> "Campaign", "muster" -> "Muster",
    "trade" -> "Trade", "recover" -> "Recover", "challenge" -> "Challenge")

  /** Appends the hidden emblem sprite, and the either-side gradient, once. */
  def mount(root: dom.Element): Unit =
    if root.querySelector("svg.mark-sprite") == null then
      val sprite = dom.document.createElementNS(Svg, "svg")
      sprite.setAttribute("class", "mark-sprite")
      sprite.setAttribute("aria-hidden", "true")
      val defs = dom.document.createElementNS(Svg, "defs")
      val gradient = dom.document.createElementNS(Svg, "linearGradient")
      gradient.setAttribute("id", EitherSideGradient)
      Vector("0" -> "var(--suit-discord)", "1" -> "var(--suit-order)")
        .foreach { (offset, colour) =>
          val stop = dom.document.createElementNS(Svg, "stop")
          stop.setAttribute("offset", offset)
          stop.setAttribute("style", s"stop-color: $colour")
          gradient.appendChild(stop)
        }
      defs.appendChild(gradient)
      sprite.appendChild(defs)
      emblems.foreach { (id, path) =>
        val symbol = dom.document.createElementNS(Svg, "symbol")
        symbol.setAttribute("id", s"mark-$id")
        symbol.setAttribute("viewBox", "0 0 24 24")
        val shape = dom.document.createElementNS(Svg, "path")
        shape.setAttribute("d", path)
        shape.setAttribute("fill", "currentColor")
        shape.setAttribute("fill-rule", "evenodd")
        symbol.appendChild(shape)
        sprite.appendChild(symbol)
      }
      root.appendChild(sprite)

  /** A power's modifier icon: one tab per action it modifies, or the
    * crowned tab of a battle plan. */
  def modifier(mark: ModifierMarkProjection, persistent: Boolean)
      : Vector[dom.Element] = mark match
    case ModifierMarkProjection.Action(actions) =>
      actions.filter(actionNames.contains).map(action(_, persistent))
    case ModifierMarkProjection.BattlePlan(sides) => Vector(plan(sides))

  /** The arch-topped tab of a modifier of `action`, by its key. */
  def action(action: String, persistent: Boolean): dom.Element =
    val name = actionNames.getOrElse(action, action.capitalize)
    val mark = frame(s"power-mark power-mark-action" +
      (if persistent then " power-mark-persistent" else ""),
      (if persistent then s"Persistent $name modifier" else s"$name modifier"))
    mark.appendChild(path(Arch, "power-mark-frame"))
    mark.appendChild(path(ArchKeyline, "power-mark-keyline"))
    mark.appendChild(emblem(action, 4.5, 7.5, 15))
    mark

  /** A battle plan's crowned tab, for the sides of a Campaign it is played
    * on ("attack", "defense"). */
  def plan(sides: Vector[String]): dom.Element =
    val (side, label) = planSide(sides)
    val mark = frame(s"power-mark power-mark-plan power-mark-plan-$side", label)
    mark.appendChild(path(Crown, "power-mark-frame"))
    mark.appendChild(emblem("campaign", 5, 9.2, 14))
    mark

  /** The crown a plan option's side chip names ("Attack Plan", "Defense
    * Plan", "Battle Plan"), or none for any other chip. */
  def planForBadge(badge: String): Option[dom.Element] = badge match
    case "Attack Plan" => Some(plan(Vector("attack")))
    case "Defense Plan" => Some(plan(Vector("defense")))
    case "Battle Plan" => Some(plan(Vector("attack", "defense")))
    case _ => None

  private def planSide(sides: Vector[String]): (String, String) =
    (sides.contains("attack"), sides.contains("defense")) match
      case (true, false) => ("attack", "Attack plan")
      case (false, true) => ("defense", "Defense plan")
      case _ => ("either", "Battle plan for either side")

  /** The banner a card's restriction hangs over its face, or none for an
    * unrestricted card. An edifice's face shows its tower, whole or broken,
    * and every locked card the chain. */
  def restriction(card: CardDetails): Option[dom.Element] =
    val restriction = card.restrictions.getOrElse("unrestricted")
    val locked = restriction.startsWith("locked")
    val edifice = Option.when(card.cardKind == "edifice")(card.side).flatten
    val emblem: Option[(String, String)] = edifice match
      case Some("ruined") => Some("tower-ruined" -> "Ruined edifice")
      case Some(_) => Some("tower" -> "Intact edifice")
      case None if restriction.endsWith("adviser-only") =>
        Some("adviser" -> "Adviser only")
      case None if restriction == "site-only" => Some("site" -> "Site only")
      case None => None
    Option.when(emblem.nonEmpty || locked) {
      val kind = emblem.map(_._1).getOrElse("locked")
      val label = (emblem.map(_._2).toVector ++
        Option.when(locked)("locked")).mkString(", ").capitalize
      val mark = frame(s"restriction-mark restriction-mark-$kind", label)
      mark.appendChild(path(Banner, "restriction-mark-edge"))
      mark.appendChild(path(BannerBody, "restriction-mark-body"))
      emblem.foreach { (id, _) =>
        if edifice.nonEmpty then
          mark.appendChild(this.emblem(id, 6, 4.2, 12))
        else
          val disc = dom.document.createElementNS(Svg, "circle")
          disc.setAttribute("class", "restriction-mark-disc")
          disc.setAttribute("cx", "12")
          disc.setAttribute("cy", "10.2")
          disc.setAttribute("r", "5.4")
          mark.appendChild(disc)
          mark.appendChild(this.emblem(id, 7.6, 5.8, 8.8))
      }
      if locked then
        val chain = this.emblem("chain", 3, -5.4, 18)
        chain.setAttribute("class", "restriction-mark-chain")
        mark.appendChild(chain)
      mark
    }

  private def frame(classes: String, label: String): dom.Element =
    val node = dom.document.createElementNS(Svg, "svg")
    node.setAttribute("class", classes)
    node.setAttribute("viewBox", "0 0 24 24")
    node.setAttribute("role", "img")
    node.setAttribute("aria-label", label)
    node

  private def path(d: String, classes: String): dom.Element =
    val node = dom.document.createElementNS(Svg, "path")
    node.setAttribute("d", d)
    node.setAttribute("class", classes)
    node

  private def emblem(id: String, x: Double, y: Double, size: Double)
      : dom.Element =
    val node = dom.document.createElementNS(Svg, "use")
    node.setAttribute("href", s"#mark-$id")
    node.setAttribute("class", s"power-mark-emblem mark-emblem-$id")
    node.setAttribute("x", x.toString)
    node.setAttribute("y", y.toString)
    node.setAttribute("width", size.toString)
    node.setAttribute("height", size.toString)
    node
