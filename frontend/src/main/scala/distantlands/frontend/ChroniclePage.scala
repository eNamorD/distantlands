package distantlands.frontend

import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** What player selection starts a game from: a new Chronicle, named there,
  * or the loaded one. */
private[frontend] enum HostChronicle:
  case New
  case Continue(saved: SavedChronicle)

/** The Chronicle page at `/` (Chronicle browsing design, "Chronicle page").
  * Every change redraws the whole page from the shelf. `kept` is false when
  * the browser refused storage; the shelf then lasts as long as the page. */
private[frontend] object ChroniclePage:
  def render(mount: dom.Element, shelf: ChronicleShelf, kept: Boolean,
      client: ChronicleClient, files: ChronicleFiles,
      choose: HostChronicle => Unit): Unit =
    // An import or rename that answers after the player has moved on to
    // player selection must not draw this page over it.
    def redraw(): Unit =
      if mount.querySelector(".chronicle-page-title") != null then
        render(mount, shelf, kept, client, files, choose)
    mount.textContent = ""
    mount.appendChild(text("h1", "chronicle-page-title", "Oath Digital"))
    if !kept then
      val notice = text("p", "status chronicle-storage-refused",
        "Chronicles cannot be kept in this browser. A Chronicle you import " +
          "or create stays here until you leave this page; export it to " +
          "keep a copy.")
      notice.setAttribute("role", "alert")
      mount.appendChild(notice)
    val loaded = shelf.loaded
    loaded.foreach { saved =>
      mount.appendChild(loadedSummary(saved, shelf, client, () => redraw()))
      val continue = button("Continue Chronicle", "continue-chronicle")
      continue.onclick = _ => choose(HostChronicle.Continue(saved))
      mount.appendChild(continue)
    }
    val fresh = button("New Chronicle", "new-chronicle")
    fresh.onclick = _ => choose(HostChronicle.New)
    mount.appendChild(fresh)
    val rows = shelf.rows
    if rows.nonEmpty then
      mount.appendChild(savedList(rows, shelf, () => redraw()))
    val edit = button("Edit Chronicle", "edit-chronicle")
    HoverTip.withhold(edit, "Coming in a later release")
    mount.appendChild(edit)
    mount.appendChild(ChronicleTransfer.importSection(shelf, client, files,
      () => redraw()))
    mount.appendChild(ChronicleTransfer.exportSection(loaded.map(_.entry),
      client, files))

  private def loadedSummary(saved: SavedChronicle, shelf: ChronicleShelf,
      client: ChronicleClient, redraw: () => Unit): dom.Element =
    val section = element("section", "chronicle-loaded")
    section.setAttribute("aria-label", "Loaded Chronicle")
    val heading = element("div", "chronicle-loaded-heading")
    val status = text("p", "status", "")
    status.setAttribute("role", "status")

    def showName(): Unit =
      heading.textContent = ""
      heading.appendChild(text("h2", "chronicle-loaded-name", saved.name))
      val rename = button("Rename", "rename-chronicle")
      rename.setAttribute("aria-label", s"Rename ${saved.name}")
      rename.onclick = _ => editName()
      heading.appendChild(rename)

    def editName(): Unit =
      heading.textContent = ""
      val field = textField("chronicle-rename", "Chronicle name", saved.name)
      val save = button("Save", "save-chronicle-name")
      val cancel = button("Cancel", "cancel-chronicle-name")
      cancel.onclick = _ =>
        status.textContent = ""
        showName()
      save.onclick = _ =>
        shelf.nameProblem(field.value, except = Some(saved.name)) match
          case Some(problem) => status.textContent = problem
          case None if field.value.trim == saved.name =>
            status.textContent = ""
            showName()
          case None =>
            save.disabled = true
            status.textContent = "Renaming…"
            client.rename(saved.entry, field.value).foreach { result =>
              result.flatMap(entry => shelf.put(entry, Some(saved.name))) match
                case Left(reason) =>
                  save.disabled = false
                  status.textContent = reason
                case Right(_) => redraw()
            }
      Vector(field, save, cancel).foreach(heading.appendChild)
      field.focus()

    showName()
    section.appendChild(heading)
    section.appendChild(text("p", "chronicle-summary-line",
      ChronicleText.summaryLine(saved.entry.summary)))
    section.appendChild(status)
    section

  private def savedList(rows: Vector[ShelfRow], shelf: ChronicleShelf,
      redraw: () => Unit): dom.Element =
    val section = element("section", "chronicle-saved")
    section.appendChild(text("h2", "", "Load Chronicle"))
    val list = element("ol", "chronicle-list")
    rows.foreach { row =>
      val item = element("li", "chronicle-row")
      val controls = element("span", "chronicle-row-controls")
      val shown = row match
        case ShelfRow.Readable(_, saved) => Some(saved.name)
        case ShelfRow.Unreadable(_) => None
      val name = row match
        case ShelfRow.Readable(_, saved) =>
          item.appendChild(text("span", "chronicle-row-name", saved.name))
          item.appendChild(text("span", "chronicle-row-details",
            ChronicleText.rowDetails(saved)))
          val load = button("Load", "load-chronicle")
          load.setAttribute("aria-label", s"Load ${saved.name}")
          load.onclick = _ =>
            shelf.load(saved.name)
            redraw()
          controls.appendChild(load)
          saved.name
        case ShelfRow.Unreadable(_) =>
          item.appendChild(text("span", "chronicle-row-name", "Unreadable"))
          "this unreadable entry"
      val delete = button("Delete", "delete-chronicle")
      delete.setAttribute("aria-label", s"Delete $name")
      delete.onclick = _ =>
        controls.textContent = ""
        controls.appendChild(text("span", "chronicle-confirm", s"Delete $name?"))
        val confirm = button("Delete", "confirm-delete-chronicle")
        confirm.onclick = _ =>
          shelf.delete(row.index, shown)
          redraw()
        val keep = button("Keep", "keep-chronicle")
        keep.onclick = _ => redraw()
        controls.appendChild(confirm)
        controls.appendChild(keep)
        keep.focus()
      controls.appendChild(delete)
      item.appendChild(controls)
      list.appendChild(item)
    }
    section.appendChild(list)
    section
