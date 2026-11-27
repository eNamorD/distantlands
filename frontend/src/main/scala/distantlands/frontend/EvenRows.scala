package distantlands.frontend

import org.scalajs.dom
import scala.scalajs.js

/** A row of cards that wraps splits its cards evenly: as many rows as plain
  * wrapping needs, the same number of cards on each, give or take one. Six
  * cards with room for five sit three and three, not five and one.
  *
  * A watched row wraps as it would without this until it is measured. Once
  * its cards need more than one row, the row is marked `even-rows-balanced`
  * and its inline size is capped at the narrowest width that keeps that many
  * rows, so a flex row and an auto-fill grid alike wrap into even lines.
  * Cards of mixed widths, a legacy among denizens, even out by length.
  * Headings in the row are not counted. A row set not to wrap is left alone.
  *
  * The row is measured again whenever it or its parent changes size, so the
  * count follows the pane. Its parent is watched too: a capped row does not
  * grow when its parent does, so the row alone would never learn of the room.
  *
  * CSS `flex-wrap: balance` does the same in Chrome and Edge 150. Once Firefox
  * and Safari ship it, it can replace this object.
  */
private[frontend] object EvenRows:
  /** The class that marks a row for balancing. */
  val Watched = "even-rows"
  /** The class that marks a row capped at an even count of cards a row. */
  val Balanced = "even-rows-balanced"

  /** How many lines cards of `widths`, in order, take when they wrap across
    * `width`, with `gap` between each two on a line. Each line takes cards
    * while they fit, as a wrapping flex row does.
    */
  def lines(widths: Seq[Double], gap: Double, width: Double): Int =
    lineLengths(widths, gap, width).size

  private def lineLengths(widths: Seq[Double], gap: Double, width: Double)
      : Vector[Double] =
    widths.foldLeft(Vector.empty[Double]) { (filled, card) =>
      filled.lastOption match
        case Some(used) if used + gap + card <= width =>
          filled.init :+ (used + gap + card)
        case _ => filled :+ card
    }

  /** The narrowest width that still wraps cards of `widths` into as many
    * lines as `width` does, so those lines come out as even as they can be;
    * None when the cards fit on one line or the row is not laid out yet.
    * Equal cards split into equal counts give or take one: six with room
    * for five sit three and three.
    */
  def balanced(widths: Seq[Double], gap: Double, width: Double)
      : Option[Double] =
    if width <= 0 || widths.isEmpty || widths.exists(_ <= 0) then None
    else
      val target = lines(widths, gap, width)
      Option.when(target > 1) {
        @annotation.tailrec
        def narrowest(low: Double, high: Double): Double =
          if high - low <= 0.25 then high
          else
            val middle = (low + high) / 2
            if lines(widths, gap, middle) <= target then narrowest(low, middle)
            else narrowest(middle, high)
        val widest = widths.max
        val found =
          if lines(widths, gap, widest) <= target then widest
          else narrowest(widest, width)
        // The longest line at that width wraps the cards the same way, and
        // is an exact sum of widths rather than the halving's last guess.
        lineLengths(widths, gap, found).max
      }

  /** Caps `row`'s inline size at `width` pixels and marks it, or clears the
    * cap and the mark.
    */
  def set(row: dom.html.Element, width: Option[Double]): Unit = width match
    case Some(pixels) =>
      row.style.setProperty("max-inline-size", s"${pixels}px")
      row.classList.add(Balanced)
    case None =>
      row.classList.remove(Balanced)
      row.style.removeProperty("max-inline-size")

  /** Marks `row` for balancing and watches its size. Returns the row. */
  def watch[A <: dom.Element](row: A): A =
    row.classList.add(Watched)
    observer.foreach(_.observe(row))
    row

  // jsdom, where the renderer suites run, has no ResizeObserver; there a row
  // is only marked.
  private lazy val observer: Option[dom.ResizeObserver] =
    Option.when(!js.isUndefined(dom.window.asInstanceOf[js.Dynamic]
      .ResizeObserver))(new dom.ResizeObserver((entries, watching) =>
        entries.foreach(entry => resized(entry.target, watching))))

  private def resized(target: dom.Element, watching: dom.ResizeObserver): Unit =
    if !target.isConnected then watching.unobserve(target)
    else if target.classList.contains(Watched) then
      Option(target.parentNode).collect { case parent: dom.Element => parent }
        .foreach(watching.observe(_))
      balance(target.asInstanceOf[dom.html.Element])
    else
      target.querySelectorAll(s":scope > .$Watched").foreach(row =>
        balance(row.asInstanceOf[dom.html.Element]))

  private def balance(row: dom.html.Element): Unit =
    // Measured uncapped, so the width is what the row has room for, not what
    // its last count left it.
    set(row, None)
    val style = dom.window.getComputedStyle(row)
    def pixels(property: String) = style.getPropertyValue(property)
      .stripSuffix("px").toDoubleOption.getOrElse(0.0)
    val cards = row.children.toVector
      .filterNot(_.matches("h1, h2, h3, h4, h5, h6"))
    if style.getPropertyValue("flex-wrap") != "nowrap" && cards.nonEmpty then
      // The cap is on the box the row's box-sizing names, so a border-box
      // row's padding and border count toward it.
      val edges = Vector("padding-left", "padding-right")
        .map(pixels).sum
      val frame =
        if style.getPropertyValue("box-sizing") == "border-box" then
          edges + pixels("border-left-width") + pixels("border-right-width")
        else 0.0
      val width = row.clientWidth - edges
      val widths = cards.map(_.getBoundingClientRect().width)
      // A pixel of slack, so rounding does not push a card to the next line.
      set(row, balanced(widths, pixels("column-gap"), width)
        .map(_ + 1 + frame))
