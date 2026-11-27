package distantlands.frontend

import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom
import ServerUiSupport._

/** The table's settings (game art design, "The Settings dialog"): a
  * button for the Players pane header and the dialog it opens, built as
  * the development panel is. It holds one switch, Game art.
  *
  * `opening` runs before the dialog opens, so the shell can close what
  * shares its place. `refresh` brings the switch up to date in place and
  * never rebuilds it, so a redraw of the table leaves focus on it.
  */
private[frontend] final class SettingsDialog(mount: dom.Element,
    opening: () => Unit):
  val toggle: dom.html.Button = button("Settings", "settings-toggle")
  toggle.setAttribute("aria-controls", "settings-panel")
  toggle.setAttribute("aria-expanded", "false")

  val node: dom.html.Element =
    element("aside", "settings-panel").asInstanceOf[dom.html.Element]
  node.id = "settings-panel"
  node.setAttribute("hidden", "")
  node.setAttribute("role", "dialog")
  node.setAttribute("aria-labelledby", "settings-heading")
  private val header = element("div", "pane-header")
  private val heading =
    text("h2", "pane-heading", "Settings").asInstanceOf[dom.html.Element]
  heading.id = "settings-heading"
  heading.tabIndex = -1
  private val closeButton = button("Close", "settings-close")
  header.appendChild(heading)
  header.appendChild(closeButton)
  private val content = element("div", "pane-content")

  private val art = button("", "setting-switch")
  art.setAttribute("role", "switch")
  art.setAttribute("aria-describedby", "game-art-hint game-art-reason")
  art.appendChild(text("span", "setting-switch-label", "Game art"))
  private val state = text("span", "setting-switch-state", "Off")
  // `aria-checked` says it; the word is for the eye.
  state.setAttribute("aria-hidden", "true")
  art.appendChild(state)
  private val hint = text("p", "setting-hint", "")
  hint.id = "game-art-hint"
  private val reason = text("p", "setting-reason", "")
  reason.id = "game-art-reason"
  private val setting = element("div", "setting")
  Vector(art, hint, reason).foreach(setting.appendChild)
  content.appendChild(setting)
  node.appendChild(header)
  node.appendChild(content)
  mount.appendChild(node)

  def isOpen: Boolean = !node.hasAttribute("hidden")

  private def open(): Unit =
    opening()
    refresh()
    node.removeAttribute("hidden")
    toggle.setAttribute("aria-expanded", "true")
    closeButton.focus()

  /** Hides the dialog. Focus returns to the button unless another dialog
    * is taking it. */
  def close(returnFocus: Boolean = true): Unit =
    val wasOpen = isOpen
    node.setAttribute("hidden", "")
    toggle.setAttribute("aria-expanded", "false")
    if wasOpen && returnFocus then toggle.focus()

  /** The switch, its hint and its reason, from `GameArt` as it stands. */
  def refresh(): Unit =
    val total = ArtCatalog.allPaths.size
    val (available, why, count) = GameArt.manifest match
      case GameArt.Manifest.Pending => (false, "Loading the art list…", "")
      case GameArt.Manifest.Failed =>
        (false, "Couldn't load the art list; reload to try again.", "")
      case GameArt.Manifest.Listed(files) if files.isEmpty =>
        (false, "This server has no game art.", "")
      case GameArt.Manifest.Listed(files) if files.size < total =>
        (true, "", s" ${files.size} of $total images; the rest stay drawn.")
      case GameArt.Manifest.Listed(_) => (true, "", "")
    val on = available && GameArt.enabled
    // The Withheld face (DESIGN.md, "Buttons"): the switch still takes
    // focus, and its hover note gives the reason the line below it shows.
    if available then
      art.removeAttribute("aria-disabled")
      art.removeAttribute(HoverTip.Attribute)
    else
      art.setAttribute("aria-disabled", "true")
      HoverTip.set(art, why)
    art.setAttribute("aria-checked", on.toString)
    state.textContent = if on then "On" else "Off"
    hint.textContent = SettingsDialog.Hint + count
    reason.textContent = why
    if why.isEmpty then reason.setAttribute("hidden", "")
    else reason.removeAttribute("hidden")

  toggle.onclick = _ => if isOpen then close() else open()
  closeButton.onclick = _ => close()
  art.onclick = _ =>
    // Saves the preference and redraws the table, which refreshes this
    // dialog through the shell; the direct refresh covers a page with no
    // table to redraw. A withheld switch answers no click.
    if !art.hasAttribute("aria-disabled") then
      GameArt.setEnabled(!GameArt.enabled)
      refresh()
  private val escape: dom.KeyboardEvent => Unit = event =>
    if event.key == "Escape" && isOpen then
      event.preventDefault()
      close()
  node.addEventListener("keydown", escape)
  refresh()

  def dispose(): Unit =
    node.removeEventListener("keydown", escape)
    toggle.remove()
    node.remove()

private[frontend] object SettingsDialog:
  val Hint = "Card faces, the mat and pieces drawn from the host's art."
