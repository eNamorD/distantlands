package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport._

/** Chronicle Details over the table (Chronicle Details design, slice 4), on
  * the log overlay's idiom: a scrim, a pane-fill panel and a header with
  * the Chronicle's name and Close. Escape, Close or a click on the scrim
  * close it. Focus goes to the content on open, and back to the opener on
  * close, or to the rebuilt Chronicle Details button when a snapshot has
  * replaced the opener. It is fed every snapshot and redraws while open,
  * keeping the reader's scroll and focus. The card inspector stacks above
  * it and closes first: `inspectorOpen` says when it is open. `opened` and
  * `closed` let the shell make the table inert behind it.
  */
private[frontend] final class ChronicleDetailsOverlay(root: dom.Element,
    inspectorOpen: () => Boolean, opened: () => Unit, closed: () => Unit):
  private val node = element("aside", "chronicle-details")
    .asInstanceOf[dom.html.Element]
  node.id = "chronicle-details"
  node.setAttribute("hidden", "")
  node.setAttribute("role", "dialog")
  node.setAttribute("aria-modal", "true")
  node.setAttribute("aria-labelledby", "chronicle-details-heading")
  private val panel = element("div", "chronicle-details-panel")
  private val header = element("div", "pane-header")
  private val heading = text("h2", "pane-heading", "Chronicle Details")
    .asInstanceOf[dom.html.Element]
  heading.id = "chronicle-details-heading"
  heading.tabIndex = -1
  private val close = button("Close", "chronicle-details-close")
  private val content = element("div",
    "pane-content chronicle-details-content").asInstanceOf[dom.html.Element]
  content.tabIndex = 0
  content.setAttribute("aria-label", "Chronicle Details contents")
  header.appendChild(heading)
  header.appendChild(close)
  panel.appendChild(header)
  panel.appendChild(content)
  node.appendChild(panel)
  root.appendChild(node)
  private var latest = Option.empty[GameProjection]
  private var opener = Option.empty[dom.html.Element]

  def isOpen: Boolean = !node.hasAttribute("hidden")

  /** Every snapshot: the latest one carrying Chronicle Details. An open
    * overlay redraws from it, or closes when the snapshot carries none. */
  def show(projection: Option[GameProjection]): Unit =
    latest = projection.filter(_.chronicleDetails.nonEmpty)
    if isOpen then latest.fold(hide())(draw(_, resetScroll = false))

  def open(origin: dom.html.Element): Unit =
    latest.foreach { value =>
      opener = Some(origin)
      draw(value, resetScroll = true)
      node.removeAttribute("hidden")
      opened()
      content.focus()
    }

  private def draw(value: GameProjection, resetScroll: Boolean): Unit =
    heading.textContent = value.chronicle.fold("Chronicle Details")(_.name)
    PanelContent.replace(content, ChronicleDetailsContent.render(value),
      heading, resetScroll)

  def hide(): Unit =
    if isOpen then
      node.setAttribute("hidden", "")
      // Before the focus moves: the owner makes the table usable again.
      closed()
      opener.filter(dom.document.contains)
        .orElse(Option(dom.document.querySelector(
          ".chronicle-details-button")).map(_.asInstanceOf[dom.html.Element]))
        .foreach(_.focus())
      opener = None

  private val dismiss: dom.MouseEvent => Unit = event =>
    if event.target == node || event.target == close then hide()

  // On the document, as the log overlay's, so Escape closes it from the
  // page too. The inspector above it closes first: this handler listens in
  // the capture phase, so it sees the inspector still open and leaves that
  // Escape to it, whether or not the key event can be cancelled.
  private val escape: dom.KeyboardEvent => Unit = event =>
    if event.key == "Escape" && isOpen && !event.defaultPrevented &&
        !inspectorOpen() then
      event.preventDefault()
      hide()

  node.addEventListener("click", dismiss)
  dom.document.addEventListener("keydown", escape, useCapture = true)

  def dispose(): Unit =
    node.removeEventListener("click", dismiss)
    dom.document.removeEventListener("keydown", escape, useCapture = true)
    node.remove()
