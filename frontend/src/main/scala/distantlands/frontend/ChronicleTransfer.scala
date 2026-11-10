package distantlands.frontend

import distantlands.protocol.{ChronicleConvertRequest, ChronicleEntry}
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** Import and export (Chronicle browsing design, "Chronicle page", items 6
  * and 7). The server checks every import; the browser only stores what it
  * returns. */
private[frontend] object ChronicleTransfer:
  def importSection(shelf: ChronicleShelf, client: ChronicleClient,
      files: ChronicleFiles, imported: () => Unit): dom.Element =
    val section = element("section", "chronicle-import")
    section.appendChild(text("h2", "", "Import Chronicle"))
    val status = statusLine("import-status")
    val clash = element("div", "import-clash")

    def keep(result: Either[String, SavedChronicle]): Unit =
      result.fold(reason => status.textContent = reason, _ => imported())

    def askAboutClash(entry: ChronicleEntry): Unit =
      val name = entry.summary.name
      clash.textContent = ""
      clash.appendChild(text("p", "", s"A Chronicle named \"$name\" is " +
        "already saved. Import this one under a new name, or replace the " +
        "saved one."))
      val field = textField("import-new-name", "New name for the import",
        shelf.uniqueName(name))
      val rename = button("Import under new name", "import-rename")
      val replace = button("Replace saved Chronicle", "import-replace")
      val cancel = button("Cancel", "import-cancel")
      rename.onclick = _ => shelf.nameProblem(field.value) match
        case Some(problem) => status.textContent = problem
        case None =>
          status.textContent = "Importing…"
          client.rename(entry, field.value).foreach(result =>
            keep(result.flatMap(renamed => shelf.put(renamed))))
      replace.onclick = _ => keep(shelf.put(entry))
      cancel.onclick = _ =>
        clash.textContent = ""
        status.textContent = ""
      Vector(field, rename, replace, cancel).foreach(clash.appendChild)
      field.focus()

    def convert(request: ChronicleConvertRequest): Unit =
      clash.textContent = ""
      status.textContent = "Checking the Chronicle…"
      client.convert(request).foreach(_.fold(
        reason => status.textContent = reason,
        response =>
          status.textContent = ""
          val entry = response.entry
          if shelf.find(entry.summary.name).isEmpty then keep(shelf.put(entry))
          else askAboutClash(entry)))

    val file = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    file.`type` = "file"
    file.className = "import-chronicle-file"
    file.accept = ".json,application/json"
    file.setAttribute("aria-label", "Chronicle JSON file")
    file.onchange = _ => files.read(file).foreach { result =>
      // Choosing the same file again, after a refusal, fires again.
      file.value = ""
      result.fold(reason => status.textContent = reason,
        json => convert(ChronicleConvertRequest.FromJson(json)))
    }
    section.appendChild(labelled("A Chronicle JSON file", file))

    val tts = dom.document.createElement("textarea")
      .asInstanceOf[dom.html.TextArea]
    tts.className = "import-chronicle-tts"
    tts.setAttribute("aria-label", "TTS string")
    tts.setAttribute("spellcheck", "false")
    val ttsName = textField("import-chronicle-name", "Name for the TTS import",
      "")
    val fromTts = button("Import TTS string", "import-tts")
    fromTts.onclick = _ =>
      if tts.value.trim.isEmpty then
        status.textContent = "Paste a TTS string to import."
      else ChronicleShelf.nameProblem(ttsName.value) match
        case Some(problem) => status.textContent = problem
        case None => convert(ChronicleConvertRequest.FromTts(tts.value.trim,
          ttsName.value.trim))
    section.appendChild(labelled("Or a TTS string", tts))
    section.appendChild(labelled("Its name", ttsName))
    section.appendChild(fromTts)
    section.appendChild(clash)
    section.appendChild(status)
    section

  def exportSection(entry: Option[ChronicleEntry], client: ChronicleClient,
      files: ChronicleFiles): dom.Element =
    val section = element("section", "chronicle-export")
    section.appendChild(text("h2", "", "Export Chronicle"))
    section.appendChild(exportControls(entry, client, files))
    section

  def exportControls(entry: Option[ChronicleEntry], client: ChronicleClient,
      files: ChronicleFiles): dom.Element =
    val box = element("div", "chronicle-export-controls")
    val json = button("Download JSON", "export-chronicle-json")
    val tts = button("Copy TTS string", "export-chronicle-tts")
    val status = statusLine("export-status")
    entry match
      case None =>
        // withhold clears onclick, so it is used only where none is set.
        Vector(json, tts).foreach(HoverTip.withhold(_,
          "Load a Chronicle to export it"))
      case Some(chronicle) =>
        json.onclick = _ => files.download(
          ChronicleFiles.filename(chronicle.summary.name), chronicle.json)
        tts.onclick = _ =>
          status.textContent = "Writing the TTS string…"
          client.convert(ChronicleConvertRequest.FromJson(chronicle.json))
            .foreach {
              case Left(reason) => status.textContent = reason
              case Right(response) => response.tts match
                case None => status.textContent =
                  "This Chronicle cannot be written as a TTS string."
                case Some(string) => files.copy(string).foreach { copied =>
                  if copied then status.textContent = "TTS string copied."
                  else showForCopying(box, status, string)
                }
            }
    Vector(json, tts, status).foreach(box.appendChild)
    box

  /** A browser that will not copy gets the string selected in a box. */
  private def showForCopying(box: dom.Element, status: dom.Element,
      string: String): Unit =
    status.textContent = "Copy this TTS string."
    Option(box.querySelector(".export-tts-text")).foreach(box.removeChild)
    val shown = dom.document.createElement("textarea")
      .asInstanceOf[dom.html.TextArea]
    shown.className = "export-tts-text"
    shown.readOnly = true
    shown.value = string
    shown.setAttribute("aria-label", "TTS string to copy")
    box.appendChild(shown)
    shown.focus()
    shown.select()

  private def statusLine(className: String): dom.Element =
    val node = text("p", s"status $className", "")
    node.setAttribute("role", "status")
    node

  private def labelled(words: String, control: dom.Element): dom.Element =
    val label = element("label", "host-field")
    label.appendChild(text("span", "", words))
    label.appendChild(control)
    label
