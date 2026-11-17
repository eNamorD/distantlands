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
  * the browser refused storage; the shelf then lasts as long as the page.
  * The page is one centered column: the Loaded Chronicle box, then each
  * button on a line of its own; Load, Import and Export open dialogs over
  * it. */
private[frontend] object ChroniclePage:
  def render(mount: dom.Element, shelf: ChronicleShelf, kept: Boolean,
      client: ChronicleClient, files: ChronicleFiles,
      choose: HostChronicle => Unit): Unit =
    draw(mount, shelf, kept, client, files, choose, loadOpen = false)

  /** Draws the page; `loadOpen` reopens the Load dialog, as a Delete or
    * Keep in it redraws the page behind it too. */
  private def draw(mount: dom.Element, shelf: ChronicleShelf, kept: Boolean,
      client: ChronicleClient, files: ChronicleFiles,
      choose: HostChronicle => Unit, loadOpen: Boolean): Unit =
    // An import or rename that answers after the player has moved on to
    // player selection must not draw this page over it.
    def redraw(loadOpen: Boolean = false): Unit =
      if mount.querySelector(".chronicle-page-title") != null then
        draw(mount, shelf, kept, client, files, choose, loadOpen)
    mount.textContent = ""
    val page = element("div", "chronicle-page")
    mount.appendChild(page)
    page.appendChild(text("h1", "chronicle-page-title", "Distant Lands"))
    if !kept then
      val notice = text("p", "status chronicle-storage-refused",
        "Chronicles cannot be kept in this browser. A Chronicle you import " +
          "or create stays here until you leave this page; export it to " +
          "keep a copy.")
      notice.setAttribute("role", "alert")
      page.appendChild(notice)
    val loaded = shelf.loaded
    page.appendChild(loaded.fold(noneLoaded())(saved =>
      loadedSummary(saved, shelf, client, () => redraw(),
        () => choose(HostChronicle.Continue(saved)))))
    val fresh = button("New Chronicle", "new-chronicle")
    fresh.onclick = _ => choose(HostChronicle.New)
    page.appendChild(fresh)
    // With nothing saved there is nothing to load, so the page leaves Load
    // out. A Delete that empties the list redraws with the dialog still
    // open, and closing it then focuses New Chronicle in Load's place.
    val load = Option.when(shelf.rows.nonEmpty)(
      button("Load Chronicle", "open-load-chronicle"))
    def openLoad(): Unit = ChronicleDialog.open(mount, load.getOrElse(fresh),
      "load-chronicle-dialog", "Load Chronicle",
      savedList(shelf.rows, shelf, () => redraw(),
        () => redraw(loadOpen = true)))
    load.foreach { control =>
      control.onclick = _ => openLoad()
      page.appendChild(control)
    }
    val edit = button("Edit Chronicle", "edit-chronicle")
    HoverTip.withhold(edit, "Coming in a later release")
    page.appendChild(edit)
    page.appendChild(ChronicleTransfer.importButton(mount, shelf, client,
      files, () => redraw()))
    page.appendChild(ChronicleTransfer.exportButton(mount,
      loaded.map(_.entry), client, files))
    if loadOpen then openLoad()

  private val NothingSaved = "No Chronicles saved in this browser"

  /** The Loaded Chronicle box, labelled on its border, around `content`. */
  private def loadedBox(content: dom.Element*): dom.Element =
    val section = element("section", "chronicle-loaded")
    val label = text("h2", "chronicle-loaded-label", "Loaded Chronicle")
    label.id = "chronicle-loaded-label"
    section.setAttribute("aria-labelledby", label.id)
    section.appendChild(label)
    content.foreach(section.appendChild)
    section

  /** The box kept in place with nothing loaded. */
  private def noneLoaded(): dom.Element =
    val box = loadedBox(text("p", "chronicle-loaded-none", "None"))
    box.classList.add("chronicle-loaded-empty")
    box

  private def loadedSummary(saved: SavedChronicle, shelf: ChronicleShelf,
      client: ChronicleClient, redraw: () => Unit,
      continueWith: () => Unit): dom.Element =
    val section = loadedBox()
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
    // A Chronicle gains its lineages when its first game is scored.
    section.appendChild(
      if saved.entry.summary.scores.isEmpty then
        text("p", "chronicle-no-scores", "No lineage points scored yet.")
      else ChronicleScoresheet.render(
        ChronicleScoresheet.of(saved.entry.summary)))
    section.appendChild(status)
    val continue = button("Continue Chronicle", "continue-chronicle")
    continue.onclick = _ => continueWith()
    section.appendChild(continue)
    section

  /** The Load dialog's list. Load closes the dialog with the page's redraw;
    * a Delete or Keep redraws with the dialog open again. */
  private def savedList(rows: Vector[ShelfRow], shelf: ChronicleShelf,
      redraw: () => Unit, redrawOpen: () => Unit): dom.Element =
    val section = element("div", "chronicle-saved")
    if rows.isEmpty then
      section.appendChild(text("p", "chronicle-saved-none", s"$NothingSaved."))
    else section.appendChild(savedRows(rows, shelf, redraw, redrawOpen))
    section

  private def savedRows(rows: Vector[ShelfRow], shelf: ChronicleShelf,
      redraw: () => Unit, redrawOpen: () => Unit): dom.Element =
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
          redrawOpen()
        val keep = button("Keep", "keep-chronicle")
        keep.onclick = _ => redrawOpen()
        controls.appendChild(confirm)
        controls.appendChild(keep)
        keep.focus()
      controls.appendChild(delete)
      item.appendChild(controls)
      list.appendChild(item)
    }
    list
