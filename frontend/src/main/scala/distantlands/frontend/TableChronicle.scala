package distantlands.frontend

import distantlands.protocol.ChronicleEntry
import distantlands.protocol.projection.ChronicleProjection
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** The table's Chronicle section (Chronicle browsing design, "Automatic
  * save"): the game's Chronicle name, and once the Chronicle Phase has
  * journaled the evolved Chronicle, its automatic save and its export.
  * Each evolved Chronicle is handled once per page, so a redraw neither
  * saves again nor asks again. The dialog that asks lives in `mount`,
  * outside the panes a redraw replaces, so what the player types survives
  * the poll. Beside the export it offers the way on: back to the Chronicle
  * page (`mainMenu`), or to player selection for the next game of the saved
  * Chronicle (`nextGame` gets that page's address). `beforeAsking` runs
  * before the dialog opens, so an overlay drawn above it closes first
  * (Chronicle Details implementation ruling R21). */
private[frontend] final class TableChronicle(mount: dom.Element,
    shelf: Option[ChronicleShelf], client: ChronicleClient,
    files: ChronicleFiles, repaint: () => Unit, mainMenu: () => Unit,
    nextGame: String => Unit, beforeAsking: () => Unit = () => ()):
  private var handled = Set.empty[String]
  /** The name each evolved Chronicle, by digest, was saved under: not its
    * own when the player renamed it to resolve a clash. */
  private var savedNames = Map.empty[String, String]
  private var note = ""
  /** The export controls of the evolved Chronicle shown, kept across
    * redraws so a copy in flight or a TTS box to copy by hand survives. */
  private var exports = Option.empty[(String, dom.Element)]

  def section(chronicle: Option[ChronicleProjection]): Option[dom.Element] =
    chronicle.map { value =>
      value.evolved.filterNot(evolved => handled(evolved.digest))
        .foreach(evolved => save(evolved, value.startingDigest))
      val node = element("section", "table-chronicle")
      node.setAttribute("aria-label", "Chronicle")
      node.appendChild(text("p", "table-chronicle-name",
        s"Chronicle: ${value.name}"))
      value.evolved.foreach { evolved =>
        if note.nonEmpty then
          node.appendChild(text("p", "table-chronicle-note", note))
        node.appendChild(wayOn(evolved))
        node.appendChild(exportControls(evolved))
      }
      node
    }

  /** Back to the Chronicle page, and on to the next game, which waits for
    * the Chronicle to be saved: player selection finds it in the browser. */
  private def wayOn(evolved: ChronicleEntry): dom.Element =
    val row = element("div", "table-chronicle-way-on")
    val menu = button("Back to Main Menu", "back-to-main-menu")
    menu.onclick = _ => mainMenu()
    val next = button("Start Next Game", "start-next-game")
    val saved = for
      kept <- shelf
      name <- savedNames.get(evolved.digest)
      found <- kept.find(name)
    yield found
    (shelf, saved) match
      case (None, _) => HoverTip.withhold(next,
        "This browser cannot keep Chronicles; download the JSON to keep it")
      case (_, None) => HoverTip.withhold(next,
        "Save the Chronicle in this browser first")
      case (_, Some(found)) =>
        next.onclick = _ => nextGame(ChroniclePages.continueUrl(found.name))
    Vector(menu, next).foreach(row.appendChild)
    row

  private def exportControls(evolved: ChronicleEntry): dom.Element =
    exports.filter(_._1 == evolved.digest).map(_._2).getOrElse {
      val controls = ChronicleTransfer.exportControls(Some(evolved), client,
        files)
      exports = Some(evolved.digest -> controls)
      controls
    }

  private def save(evolved: ChronicleEntry, startingDigest: String): Unit =
    handled += evolved.digest
    note = shelf match
      case None => "Chronicles cannot be kept in this browser; export this " +
        "one to keep it."
      case Some(kept) => kept.autoSave(evolved, startingDigest) match
        case AutoSave.Saved(saved) =>
          savedNames += evolved.digest -> saved.name
          savedAs(saved)
        case AutoSave.Unchanged =>
          savedNames += evolved.digest -> evolved.summary.name
          "Already saved in this browser."
        case AutoSave.Refused(reason) => reason
        case AutoSave.Conflict(existing) =>
          ask(kept, evolved, existing)
          ""

  private def savedAs(saved: SavedChronicle): String =
    s"Saved in this browser as \"${saved.name}\"."

  private def ask(kept: ChronicleShelf, evolved: ChronicleEntry,
      existing: SavedChronicle): Unit =
    beforeAsking()
    val overlay = element("div", "chronicle-save-prompt")
    overlay.setAttribute("role", "dialog")
    overlay.setAttribute("aria-modal", "true")
    overlay.setAttribute("aria-labelledby", "chronicle-save-heading")
    val panel = element("div", "chronicle-save-panel")
    val heading = text("h2", "", "Save the Chronicle")
    heading.id = "chronicle-save-heading"
    panel.appendChild(heading)
    panel.appendChild(text("p", "", "This browser already has a different " +
      s"Chronicle named \"${existing.name}\". Save this game's Chronicle " +
      "under a new name, or overwrite the saved one."))
    val field = textField("chronicle-save-name", "New Chronicle name",
      kept.uniqueName(existing.name))
    val status = text("p", "status chronicle-save-status", "")
    status.setAttribute("role", "status")
    val saveNew = button("Save under new name", "chronicle-save-new")
    val overwrite = button("Overwrite", "chronicle-overwrite")

    // One choice is made: while a rename is in flight neither button
    // answers, so a second click cannot save a second copy.
    def busy(on: Boolean): Unit = Vector(saveNew, overwrite)
      .foreach(_.disabled = on)

    def finish(result: Either[String, SavedChronicle]): Unit = result match
      case Left(reason) =>
        busy(false)
        status.textContent = reason
      case Right(saved) =>
        savedNames += evolved.digest -> saved.name
        note = savedAs(saved)
        if overlay.parentNode != null then mount.removeChild(overlay)
        repaint()

    overwrite.onclick = _ =>
      if !overwrite.disabled then
        busy(true)
        finish(kept.put(evolved))
    saveNew.onclick = _ => kept.nameProblem(field.value) match
      case _ if saveNew.disabled => ()
      case Some(problem) => status.textContent = problem
      case None =>
        busy(true)
        status.textContent = "Saving…"
        client.rename(evolved, field.value).foreach(result =>
          finish(result.flatMap(renamed => kept.put(renamed))))
    Vector(field, saveNew, overwrite, status).foreach(panel.appendChild)
    overlay.appendChild(panel)
    mount.appendChild(overlay)
    field.focus()
