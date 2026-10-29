package oathdigital.frontend

import oathdigital.protocol.ChronicleEntry
import oathdigital.protocol.projection.ChronicleProjection
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** The table's Chronicle section (Chronicle browsing design, "Automatic
  * save"): the game's Chronicle name, and once the Chronicle Phase has
  * journaled the evolved Chronicle, its automatic save and its export.
  * Each evolved Chronicle is handled once per page, so a redraw neither
  * saves again nor asks again. The dialog that asks lives in `mount`,
  * outside the panes a redraw replaces, so what the player types survives
  * the poll. */
private[frontend] final class TableChronicle(mount: dom.Element,
    shelf: Option[ChronicleShelf], client: ChronicleClient,
    files: ChronicleFiles, repaint: () => Unit):
  private var handled = Set.empty[String]
  private var note = ""

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
        node.appendChild(ChronicleTransfer.exportControls(Some(evolved), client,
          files))
      }
      node
    }

  private def save(evolved: ChronicleEntry, startingDigest: String): Unit =
    handled += evolved.digest
    note = shelf match
      case None => "Chronicles cannot be kept in this browser; export this " +
        "one to keep it."
      case Some(kept) => kept.autoSave(evolved, startingDigest) match
        case AutoSave.Saved(saved) => savedAs(saved)
        case AutoSave.Unchanged => "Already saved in this browser."
        case AutoSave.Refused(reason) => reason
        case AutoSave.Conflict(existing) =>
          ask(kept, evolved, existing)
          ""

  private def savedAs(saved: SavedChronicle): String =
    s"Saved in this browser as \"${saved.name}\"."

  private def ask(kept: ChronicleShelf, evolved: ChronicleEntry,
      existing: SavedChronicle): Unit =
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

    def finish(result: Either[String, SavedChronicle]): Unit = result match
      case Left(reason) => status.textContent = reason
      case Right(saved) =>
        note = savedAs(saved)
        mount.removeChild(overlay)
        repaint()

    overwrite.onclick = _ => finish(kept.put(evolved))
    saveNew.onclick = _ => kept.nameProblem(field.value) match
      case Some(problem) => status.textContent = problem
      case None =>
        status.textContent = "Saving…"
        client.rename(evolved, field.value).foreach(result =>
          finish(result.flatMap(renamed => kept.put(renamed))))
    Vector(field, saveNew, overwrite, status).foreach(panel.appendChild)
    overlay.appendChild(panel)
    mount.appendChild(overlay)
    field.focus()
