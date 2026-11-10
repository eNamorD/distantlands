package distantlands.frontend

import org.scalajs.dom
import scala.util.Try

/** The edge between the desktop table's left column (Actions over the Log)
  * and the players and map beside it. A drag, or the arrow keys, set the
  * column's width; a double click returns the stylesheet's own columns. The
  * width is kept in `store`, the browser's storage the log marker uses too,
  * so it is one per browser rather than one per seat.
  *
  * The divider only sets `--sidebar-w` and the `sidebar-sized` class on the
  * table; the stylesheet applies them on a desktop only and clamps the width
  * against the table, so a width stored on a wider window cannot squeeze the
  * map away on a narrower one.
  */
private[frontend] final class SidebarDivider(table: dom.html.Element,
    sidebar: dom.html.Element, store: Option[LogStore]):
  import SidebarDivider.*

  private val handle = dom.document.createElement("div").asInstanceOf[dom.html.Div]
  handle.className = "sidebar-divider"
  handle.tabIndex = 0
  handle.setAttribute("role", "separator")
  handle.setAttribute("aria-orientation", "vertical")
  handle.setAttribute("aria-label", "Resize the Actions and Log column")
  handle.setAttribute("aria-valuemin", MinWidth.toInt.toString)
  HoverTip.set(handle, "Drag to resize; double-click to reset")
  table.appendChild(handle)
  private val capture = handle.asInstanceOf[PointerCapture]

  private var width = store.flatMap(_.read(StoreKey)).flatMap(_.toDoubleOption)
    .filter(_ > 0)
  private var start = Option.empty[(Double, Double)]
  show()

  private def show(): Unit = width match
    case Some(value) =>
      table.classList.add("sidebar-sized")
      table.style.setProperty("--sidebar-w", s"${value.toInt}px")
      handle.setAttribute("aria-valuenow", value.toInt.toString)
    case None =>
      table.classList.remove("sidebar-sized")
      table.style.removeProperty("--sidebar-w")
      handle.removeAttribute("aria-valuenow")

  /** The width now on screen: the one set, else the column as laid out. */
  private def current: Double =
    width.getOrElse(math.max(MinWidth, sidebar.getBoundingClientRect().width))

  /** At least the minimum, and at most half the table once it has a width
    * (the stylesheet holds the same bounds as the window resizes). */
  private def bounded(value: Double): Double =
    val half = table.getBoundingClientRect().width / 2
    val upper = if half > MinWidth then math.min(value, half) else value
    math.max(MinWidth, math.rint(upper))

  private def set(value: Double, keep: Boolean): Unit =
    width = Some(bounded(value))
    show()
    if keep then save()

  private def save(): Unit =
    store.foreach(_.write(StoreKey, width.fold("")(_.toInt.toString)))

  private val down: dom.PointerEvent => Unit = e => {
    if e.button == 0 then
      e.preventDefault()
      Try(capture.setPointerCapture(e.pointerId))
      start = Some((e.clientX, current))
      table.classList.add("sidebar-resizing")
  }
  private val move: dom.PointerEvent => Unit = e => start.foreach { (x, from) =>
    set(from + e.clientX - x, keep = false)
  }
  private val up: dom.PointerEvent => Unit = e => {
    if start.nonEmpty then
      if capture.hasPointerCapture(e.pointerId) then
        capture.releasePointerCapture(e.pointerId)
      start = None
      table.classList.remove("sidebar-resizing")
      save()
  }
  private val key: dom.KeyboardEvent => Unit = e => e.key match
    case "ArrowLeft" => e.preventDefault(); set(current - KeyStep, keep = true)
    case "ArrowRight" => e.preventDefault(); set(current + KeyStep, keep = true)
    case "Home" => e.preventDefault(); set(MinWidth, keep = true)
    case _ => ()
  private val reset: dom.MouseEvent => Unit = _ => {
    width = None
    show()
    save()
  }
  handle.addEventListener("pointerdown", down)
  handle.addEventListener("pointermove", move)
  handle.addEventListener("pointerup", up)
  handle.addEventListener("pointercancel", up)
  handle.addEventListener("keydown", key)
  handle.addEventListener("dblclick", reset)

  def dispose(): Unit =
    handle.removeEventListener("pointerdown", down)
    handle.removeEventListener("pointermove", move)
    handle.removeEventListener("pointerup", up)
    handle.removeEventListener("pointercancel", up)
    handle.removeEventListener("keydown", key)
    handle.removeEventListener("dblclick", reset)
    handle.remove()

private[frontend] object SidebarDivider:
  val StoreKey = "oath.table.sidebar-width"
  /** Narrow enough to give the map room, wide enough that the Actions pane's
    * buttons keep their words on a line and a hand shows two cards a row. */
  val MinWidth = 240.0
  /** One arrow key press. */
  val KeyStep = 16.0
