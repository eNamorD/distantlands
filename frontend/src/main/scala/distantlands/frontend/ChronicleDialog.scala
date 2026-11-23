package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{button, element, text}

/** A dialog over the Chronicle page, in the card overlay's idiom: a scrim, a
  * panel with a heading and Close, and Escape, Close or a click on the scrim
  * closing it and returning focus to the control that opened it. It lives
  * in the page's mount, so the redraw after a successful import removes it
  * with the rest of the page. */
private[frontend] object ChronicleDialog:
  /** Opens a dialog titled `title` around `body` in `host`, focusing the
    * body's first control. `name` is the dialog's own class, so a page can
    * hold one of each kind; opening a second of a kind replaces the first. */
  def open(host: dom.Element, opener: dom.html.Element, name: String,
      title: String, body: dom.Element): dom.html.Element =
    Option(host.querySelector(s".$name")).foreach(_.remove())
    val node = element("div", s"chronicle-dialog $name")
      .asInstanceOf[dom.html.Element]
    node.setAttribute("role", "dialog")
    node.setAttribute("aria-modal", "true")
    node.setAttribute("aria-labelledby", s"$name-heading")
    val panel = element("div", "chronicle-dialog-panel")
    val header = element("div", "chronicle-dialog-header")
    val heading = text("h2", "chronicle-dialog-heading", title)
    heading.id = s"$name-heading"
    val close = button("Close", "chronicle-dialog-close")
    header.appendChild(heading)
    header.appendChild(close)
    panel.appendChild(header)
    panel.appendChild(body)
    node.appendChild(panel)

    def hide(): Unit =
      dom.document.removeEventListener("keydown", escape)
      node.remove()
      if dom.document.contains(opener) then opener.focus()

    lazy val escape: dom.KeyboardEvent => Unit = event =>
      // A redraw removes the dialog without closing it; the first key
      // after that drops the listener.
      if !dom.document.contains(node) then
        dom.document.removeEventListener("keydown", escape)
      else if event.key == "Escape" then
        event.preventDefault()
        hide()

    close.onclick = _ => hide()
    node.addEventListener("click", (event: dom.MouseEvent) =>
      if event.target == node then hide())
    // On the document: a click on the panel's blank space drops focus to
    // the body, and Escape must still close the dialog from there.
    dom.document.addEventListener("keydown", escape)
    host.appendChild(node)
    Option(body.querySelector("input, textarea, button"))
      .getOrElse(close).asInstanceOf[dom.html.Element].focus()
    node
