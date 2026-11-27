package distantlands.frontend

import distantlands.frontend.ChronicleFixtures.{convertingTransport, entry}
import distantlands.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec}
import distantlands.testkit.Unwrap.{accepted, as, present}
import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Import from a JSON file or a TTS string, with its name clash, and
  * export as a JSON download or a copied TTS string: the page's buttons and
  * the dialogs they open, and the table's export controls. */
class ChronicleTransferSuite extends munit.FunSuite:
  /** The import form over `shelf`, mounted in `browser`; the returned
    * buffer records each call of `imported`. */
  private def mountImportSection(browser: TestBrowser, shelf: ChronicleShelf,
      transport: JsonTransport, files: FakeChronicleFiles): ArrayBuffer[Unit] =
    val imported = ArrayBuffer.empty[Unit]
    browser.mount.appendChild(ChronicleTransfer.importForm(shelf,
      ChronicleClient(transport), files, () => imported += (())))
    imported

  private def mountExportControls(browser: TestBrowser,
      chronicle: Option[distantlands.protocol.ChronicleEntry],
      transport: JsonTransport, files: FakeChronicleFiles): Unit =
    browser.mount.appendChild(ChronicleTransfer.exportControls(chronicle,
      ChronicleClient(transport), files))

  /** The page's Export button for `chronicle`, mounted in `browser`. */
  private def mountExportButton(browser: TestBrowser,
      chronicle: distantlands.protocol.ChronicleEntry,
      transport: JsonTransport, files: FakeChronicleFiles): dom.html.Button =
    val open = ChronicleTransfer.exportButton(browser.mount, chronicle,
      ChronicleClient(transport), files)
    browser.mount.appendChild(open)
    open

  private def freshShelf(): ChronicleShelf =
    new ChronicleShelf(new MemoryChronicleStorage(), ChronicleFixtures.clock)

  /** A shelf holding "Ashen Crown" with digest d-1, loaded. */
  private def shelfWithAshenCrown(): ChronicleShelf =
    val shelf = freshShelf()
    accepted(shelf.put(entry("Ashen Crown")))
    shelf

  private def sentRequests(requests: collection.Seq[RecordedRequest])
      : Vector[ChronicleConvertRequest] =
    requests.toVector.map(request => accepted(ChronicleConvertRequestCodec
      .decode(present(request.body, "a request body"))))

  private def textOf(browser: TestBrowser, className: String): String =
    present(browser.byClass(className).headOption, className).textContent

  private def typeInto(browser: TestBrowser, className: String,
      value: String): Unit =
    browser.byClass(className).head match
      case area: dom.html.TextArea => area.value = value
      case other => as[dom.html.Input](other).value = value

  /** Imports a JSON file of "Ashen Crown" into `shelf`. */
  private def importAshenCrownFile(browser: TestBrowser, shelf: ChronicleShelf,
      requests: ArrayBuffer[RecordedRequest]): Future[ArrayBuffer[Unit]] =
    val files = FakeChronicleFiles(Right(entry("Ashen Crown").json))
    val imported = mountImportSection(browser, shelf,
      convertingTransport(requests), files)
    browser.change("import-chronicle-file")
    browser.settle.map(_ => imported)

  test("a JSON file is converted, saved and loaded"):
    TestBrowser.use(): browser =>
      val shelf = freshShelf()
      val requests = ArrayBuffer.empty[RecordedRequest]
      importAshenCrownFile(browser, shelf, requests).map { imported =>
        assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown"))
        assertEquals(imported.size, 1)
        assertEquals(requests.map(_.url).toVector, Vector("/chronicles/convert"))
        assertEquals(sentRequests(requests), Vector(
          ChronicleConvertRequest.FromJson(entry("Ashen Crown").json)))
      }

  test("a file that cannot be read shows why, beside the control"):
    TestBrowser.use(): browser =>
      val requests = ArrayBuffer.empty[RecordedRequest]
      mountImportSection(browser, freshShelf(), convertingTransport(requests),
        FakeChronicleFiles(Left("The file could not be read.")))
      browser.change("import-chronicle-file")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "import-status"),
          "The file could not be read.")
        assertEquals(requests.size, 0)
      }

  test("a convert refusal shows the server's reason"):
    TestBrowser.use(): browser =>
      val shelf = freshShelf()
      mountImportSection(browser, shelf, ScriptedTransport((_, _, _) =>
        Future.successful(Right(TransportResponse(422,
          """{"error":"invalid-chronicle","message":"$.era: bad"}""")))),
        FakeChronicleFiles(Right("{}")))
      browser.change("import-chronicle-file")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "import-status"), "$.era: bad")
        assertEquals(shelf.saved, Vector.empty)
      }

  test("a TTS string needs a string and a name"):
    TestBrowser.useSynchronously(): browser =>
      val requests = ArrayBuffer.empty[RecordedRequest]
      mountImportSection(browser, freshShelf(), convertingTransport(requests),
        FakeChronicleFiles())
      browser.click("import-tts")
      assertEquals(textOf(browser, "import-status"),
        "Paste a TTS string to import.")
      typeInto(browser, "import-chronicle-tts", "abc")
      browser.click("import-tts")
      assertEquals(textOf(browser, "import-status"), "A Chronicle needs a name.")
      assertEquals(requests.size, 0)

  test("a TTS string is imported under the name given, trimmed"):
    TestBrowser.use(): browser =>
      val shelf = freshShelf()
      val requests = ArrayBuffer.empty[RecordedRequest]
      mountImportSection(browser, shelf, convertingTransport(requests),
        FakeChronicleFiles())
      typeInto(browser, "import-chronicle-tts", " abc ")
      typeInto(browser, "import-chronicle-name", "  Iron Vow ")
      browser.click("import-tts")
      browser.settle.map { _ =>
        assertEquals(sentRequests(requests),
          Vector(ChronicleConvertRequest.FromTts("abc", "Iron Vow")))
        assertEquals(shelf.loaded.map(_.name), Some("Iron Vow"))
      }

  test("a name clash offers a new name or replacing the saved entry"):
    TestBrowser.use(): browser =>
      val shelf = shelfWithAshenCrown()
      importAshenCrownFile(browser, shelf, ArrayBuffer.empty).map { imported =>
        assert(textOf(browser, "import-clash").contains(
          "A Chronicle named \"Ashen Crown\" is already saved."))
        assertEquals(browser.input("New name for the import").value,
          "Ashen Crown (2)")
        assertEquals(imported.size, 0)
        assertEquals(shelf.saved.map(_.entry.digest), Vector("d-1"))
      }

  test("importing under the new name renames through the server"):
    TestBrowser.use(): browser =>
      val shelf = shelfWithAshenCrown()
      val requests = ArrayBuffer.empty[RecordedRequest]
      importAshenCrownFile(browser, shelf, requests).flatMap { _ =>
        browser.click("import-rename")
        browser.settle
      }.map { _ =>
        sentRequests(requests).last match
          case ChronicleConvertRequest.FromJson(json) =>
            assertEquals(ujson.read(json)("name").str, "Ashen Crown (2)")
          case other => fail(other.toString)
        assertEquals(shelf.saved.map(_.name),
          Vector("Ashen Crown", "Ashen Crown (2)"))
        assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown (2)"))
      }

  test("a taken new name is refused without a request"):
    TestBrowser.use(): browser =>
      val shelf = shelfWithAshenCrown()
      val requests = ArrayBuffer.empty[RecordedRequest]
      importAshenCrownFile(browser, shelf, requests).map { _ =>
        browser.input("New name for the import").value = "Ashen Crown"
        browser.click("import-rename")
        assert(textOf(browser, "import-status").contains("already saved"))
        assertEquals(requests.size, 1)
      }

  test("replacing keeps one entry, the import's"):
    TestBrowser.use(): browser =>
      val shelf = shelfWithAshenCrown()
      importAshenCrownFile(browser, shelf, ArrayBuffer.empty).map { _ =>
        browser.click("import-replace")
        assertEquals(shelf.saved.map(_.entry.digest), Vector("d-Ashen Crown"))
      }

  test("cancel drops the clash and saves nothing"):
    TestBrowser.use(): browser =>
      val shelf = shelfWithAshenCrown()
      importAshenCrownFile(browser, shelf, ArrayBuffer.empty).map { imported =>
        browser.click("import-cancel")
        assertEquals(textOf(browser, "import-clash"), "")
        assertEquals(shelf.saved.map(_.entry.digest), Vector("d-1"))
        assertEquals(imported.size, 0)
      }

  test("export without a Chronicle is withheld with a reason"):
    TestBrowser.useSynchronously(): browser =>
      val files = FakeChronicleFiles()
      mountExportControls(browser, None, convertingTransport(), files)
      Vector("export-chronicle-json", "export-chronicle-tts").foreach { name =>
        val control = browser.byClass(name).head
        assertEquals(control.getAttribute("aria-disabled"), "true")
        assertEquals(control.getAttribute(HoverTip.Attribute),
          "Load a Chronicle to export it")
        assertEquals(as[dom.html.Button](control).onclick, null)
      }

  test("Download JSON downloads <name>.chronicle.json holding the JSON"):
    TestBrowser.useSynchronously(): browser =>
      val files = FakeChronicleFiles()
      mountExportControls(browser, Some(entry("Ashen Crown")),
        convertingTransport(), files)
      browser.click("export-chronicle-json")
      assertEquals(files.downloads.toVector, Vector(
        "Ashen Crown.chronicle.json" -> entry("Ashen Crown").json))

  test("Copy TTS string copies the server's TTS"):
    TestBrowser.use(): browser =>
      val files = FakeChronicleFiles()
      mountExportControls(browser, Some(entry("Ashen Crown")),
        convertingTransport(), files)
      browser.click("export-chronicle-tts")
      browser.settle.map { _ =>
        assertEquals(files.copies.toVector, Vector("TTS-STRING"))
        assertEquals(textOf(browser, "export-status"), "TTS string copied.")
      }

  test("a Chronicle TTS cannot hold says so"):
    TestBrowser.use(): browser =>
      val files = FakeChronicleFiles()
      mountExportControls(browser, Some(entry("Ashen Crown")),
        convertingTransport(tts = None), files)
      browser.click("export-chronicle-tts")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "export-status"),
          "This Chronicle cannot be written as a TTS string.")
        assertEquals(files.copies.toVector, Vector.empty)
      }

  test("a browser that will not copy shows the string to copy by hand"):
    TestBrowser.use(): browser =>
      val files = FakeChronicleFiles()
      files.copyWorks = false
      mountExportControls(browser, Some(entry("Ashen Crown")),
        convertingTransport(), files)
      browser.click("export-chronicle-tts")
      browser.settle.flatMap { _ =>
        val shown = as[dom.html.TextArea](present(
          browser.byClass("export-tts-text").headOption, "the TTS box"))
        assertEquals(shown.value, "TTS-STRING")
        assert(shown.readOnly)
        assertEquals(textOf(browser, "export-status"), "Copy this TTS string.")
        browser.click("export-chronicle-tts")
        browser.settle
      }.map { _ =>
        assertEquals(browser.byClass("export-tts-text").size, 1)
      }

  test("Import opens a dialog: a file, a divider, then the TTS string"):
    TestBrowser.useSynchronously(): browser =>
      val open = ChronicleTransfer.importButton(browser.mount, freshShelf(),
        ChronicleClient(convertingTransport()), FakeChronicleFiles(),
        () => ())
      browser.mount.appendChild(open)
      assertEquals(open.textContent, "Import Chronicle")
      assertEquals(browser.byClass("chronicle-dialog").size, 0)
      browser.click("import-chronicle")
      val dialog = present(browser.byClass("import-chronicle-dialog")
        .headOption, "the import dialog")
      assertEquals(dialog.getAttribute("role"), "dialog")
      assertEquals(textOf(browser, "chronicle-dialog-heading"),
        "Import Chronicle")
      val form = present(browser.byClass("chronicle-import").headOption,
        "the import form")
      val order = (0 until form.children.length).toVector
        .map(form.children(_)).map(child =>
          if child.tagName == "HR" then "divider"
          else Option(child.querySelector("span")).fold(
            child.getAttribute("class"))(_.textContent))
      assertEquals(order.take(4), Vector("Upload a Chronicle JSON file",
        "divider", "Or paste a TTS string", "Its name"))
      assertEquals(dom.document.activeElement,
        browser.byClass("import-chronicle-file").head)

  test("Close, Escape and the scrim close the dialog and refocus its " +
      "button"):
    TestBrowser.useSynchronously(): browser =>
      val open = mountExportButton(browser, entry("Ashen Crown"),
        convertingTransport(), FakeChronicleFiles())
      def closedBy(close: () => Unit): Unit =
        browser.click("export-chronicle")
        assertEquals(browser.byClass("export-chronicle-dialog").size, 1)
        close()
        assertEquals(browser.byClass("export-chronicle-dialog").size, 0)
        assertEquals(dom.document.activeElement, open)
      closedBy(() => browser.click("chronicle-dialog-close"))
      closedBy(() => dom.document.dispatchEvent(new dom.KeyboardEvent(
        "keydown", new dom.KeyboardEventInit { key = "Escape" })))
      closedBy(() => as[dom.html.Element](browser.byClass(
        "export-chronicle-dialog").head).click())

  test("the Export dialog shows the TTS string as it opens, and copies it"):
    TestBrowser.use(): browser =>
      val files = FakeChronicleFiles()
      val requests = ArrayBuffer.empty[RecordedRequest]
      mountExportButton(browser, entry("Ashen Crown"),
        convertingTransport(requests), files)
      browser.click("export-chronicle")
      assertEquals(textOf(browser, "export-status"),
        "Writing the TTS string…")
      browser.settle.flatMap { _ =>
        assertEquals(sentRequests(requests), Vector(
          ChronicleConvertRequest.FromJson(entry("Ashen Crown").json)))
        val shown = as[dom.html.TextArea](present(
          browser.byClass("export-tts-text").headOption, "the TTS box"))
        assertEquals(shown.value, "TTS-STRING")
        assert(shown.readOnly)
        assertEquals(textOf(browser, "export-status"), "")
        browser.click("export-chronicle-json")
        assertEquals(files.downloads.toVector, Vector(
          "Ashen Crown.chronicle.json" -> entry("Ashen Crown").json))
        browser.click("export-chronicle-tts")
        browser.settle
      }.map { _ =>
        assertEquals(files.copies.toVector, Vector("TTS-STRING"))
        assertEquals(textOf(browser, "export-status"), "TTS string copied.")
      }

  test("the Export dialog says when TTS cannot hold the Chronicle"):
    TestBrowser.use(): browser =>
      mountExportButton(browser, entry("Ashen Crown"),
        convertingTransport(tts = None), FakeChronicleFiles())
      browser.click("export-chronicle")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "export-status"),
          "This Chronicle cannot be written as a TTS string.")
        assertEquals(browser.byClass("export-tts-text").size, 0)
        assertEquals(browser.byClass("export-chronicle-tts").size, 0)
      }

  test("in the Export dialog, a browser that will not copy leaves the " +
      "string selected"):
    TestBrowser.use(): browser =>
      val files = FakeChronicleFiles()
      files.copyWorks = false
      mountExportButton(browser, entry("Ashen Crown"),
        convertingTransport(), files)
      browser.click("export-chronicle")
      browser.settle.flatMap { _ =>
        browser.click("export-chronicle-tts")
        browser.settle
      }.map { _ =>
        val shown = as[dom.html.TextArea](browser.byClass("export-tts-text")
          .head)
        assertEquals(textOf(browser, "export-status"), "Copy this TTS string.")
        assertEquals(dom.document.activeElement, shown)
        assertEquals((shown.selectionStart, shown.selectionEnd),
          (0, "TTS-STRING".length))
      }
