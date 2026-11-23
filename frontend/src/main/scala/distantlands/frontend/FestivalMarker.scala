package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{element, svgElement, text}

/** The festival marker the Darkest Secret's holder lays over the cards at
  * their site, drawn as the printed marker is: a teal plate leaning to the
  * right, its title in capitals and its text beneath.
  *
  * On the map it is a band across the middle of the site's card slots that
  * prints only the title; pressing it opens the whole plate in the
  * inspector.
  */
private[frontend] object FestivalMarker:
  val Title = "Festival Underway"

  /** The text printed on the marker, under its title. */
  val Rule: String = "Enemies of the holder of the Darkest Secret cannot " +
    "use these cards for major actions, placing resources, and non-global " +
    "card powers."

  /** The printed marker's proportions, about three to one. */
  private val PrintedW = 600.0
  private val PrintedH = 200.0

  /** How far each bottom corner lies right of the top corner above it, as
    * a share of the plate's height: the printed marker's left edge leans a
    * little, its right edge more. */
  private val LeftLean = 0.09
  private val RightLean = 0.25

  /** The plate's corners in a `w` by `h` box, clockwise from the top
    * left. */
  def corners(w: Double, h: Double): Vector[(Double, Double)] =
    Vector((0.0, 0.0), (w - RightLean * h, 0.0), (w, h), (LeftLean * h, h))

  /** The plate's outline in a `w` by `h` box: its corners, each rounded. */
  private[frontend] def outline(w: Double, h: Double): String =
    val points = corners(w, h)
    val radius = 0.08 * h
    def toward(from: (Double, Double), to: (Double, Double))
        : (Double, Double) =
      val (dx, dy) = (to._1 - from._1, to._2 - from._2)
      val length = math.hypot(dx, dy)
      (from._1 + dx * radius / length, from._2 + dy * radius / length)
    def at(point: (Double, Double)): String =
      f"${point._1}%.2f ${point._2}%.2f"
    val n = points.size
    val turns = points.indices.map { i =>
      val corner = points(i)
      val before = toward(corner, points((i + n - 1) % n))
      val after = toward(corner, points((i + 1) % n))
      (before, corner, after)
    }
    val (start, _, _) = turns.head
    s"M ${at(start)} " + turns.map((before, corner, after) =>
      s"L ${at(before)} Q ${at(corner)} ${at(after)}").mkString(" ") + " Z"

  /** The plate's teal face and dark rim, stretched over its box, which has
    * the `w` by `h` proportions. It is for the eye only. */
  private def shape(w: Double, h: Double): dom.Element =
    val svg = svgElement("svg")
    svg.setAttribute("class", "festival-plate-shape")
    svg.setAttribute("viewBox", f"0 0 $w%.2f $h%.2f")
    svg.setAttribute("preserveAspectRatio", "none")
    svg.setAttribute("aria-hidden", "true")
    val path = svgElement("path")
    path.setAttribute("class", "festival-plate-outline")
    path.setAttribute("d", outline(w, h))
    path.setAttribute("vector-effect", "non-scaling-stroke")
    svg.appendChild(path)
    svg

  /** A picture of the marker: the title alone, at the map band's
    * proportions, or the whole printed marker `withRule`. */
  def plate(withRule: Boolean): dom.Element =
    val (w, h) =
      if withRule then (PrintedW, PrintedH)
      else
        val band = MatLayout.festivalMarker(0, 0)
        (band.w, band.h)
    val node = element("div",
      if withRule then "festival-plate festival-plate-printed"
      else "festival-plate")
    node.asInstanceOf[dom.html.Element].style
      .setProperty("aspect-ratio", f"$w%.2f / $h%.2f")
    node.appendChild(shape(w, h))
    node.appendChild(text("span", "festival-plate-title", Title))
    if withRule then node.appendChild(text("p", "festival-plate-rule", Rule))
    node

  /** The marker lying over a site's cards, in the band `rect`: the plate
    * with its title. It is a button, so a keyboard reaches it and its note
    * shows on focus, and pressing it opens the printed marker in the
    * inspector, with `grid`, the holder's consent preferences, when the
    * viewer holds the Festival. It is no board option. */
  def band(rect: MatRect, grid: Option[ConsentGrid] = None)
      : dom.html.Button =
    val node = dom.document.createElement("button")
      .asInstanceOf[dom.html.Button]
    node.className = "festival-marker festival-plate"
    node.`type` = "button"
    node.setAttribute("aria-label", "Festival marker")
    node.appendChild(shape(rect.w, rect.h))
    node.appendChild(text("span", "festival-plate-title", Title))
    HoverTip.set(node, Rule)
    node.onclick = _ => CardInspection.openFestivalMarker(grid, node)
    node
