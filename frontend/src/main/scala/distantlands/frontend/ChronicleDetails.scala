package distantlands.frontend

import org.scalajs.dom

/** The slot the Chronicle Details button opens through (Chronicle Details
  * design, slice 4). The World pane is rebuilt on every snapshot and cannot
  * own the overlay, so the button asks whatever `GameTableShell`
  * registered, as a card asks `CardInspection`. */
private[frontend] object ChronicleDetails:
  private var handler = Option.empty[dom.html.Element => Unit]

  def onOpen(value: dom.html.Element => Unit): Unit = handler = Some(value)

  def clear(): Unit = handler = None

  def open(origin: dom.html.Element): Unit = handler.foreach(_(origin))
