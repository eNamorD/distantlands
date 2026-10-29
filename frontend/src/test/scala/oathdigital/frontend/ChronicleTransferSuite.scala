package oathdigital.frontend

import oathdigital.frontend.ChronicleFixtures.{convertingTransport, entry}
import oathdigital.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec}
import oathdigital.testkit.Unwrap.{accepted, as, present}
import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Import from a JSON file or a TTS string, with its name clash, and
  * export as a JSON download or a copied TTS string. */
class ChronicleTransferSuite extends munit.FunSuite:
  /** The import section over `shelf`, mounted in `browser`; the returned
    * buffer records each call of `imported`. */
  private def mountImportSection(browser: TestBrowser, shelf: ChronicleShelf,
      transport: JsonTransport, files: FakeChronicleFiles): ArrayBuffer[Unit] =
    val imported = ArrayBuffer.empty[Unit]
    browser.mount.appendChild(ChronicleTransfer.importSection(shelf,
      ChronicleClient(transport), files, () => imported += (())))
    imported

  private def mountExportControls(browser: TestBrowser,
      chronicle: Option[oathdigital.protocol.ChronicleEntry],
      transport: JsonTransport, files: FakeChronicleFiles): Unit =
    browser.mount.appendChild(ChronicleTransfer.exportControls(chronicle,
      ChronicleClient(transport), files))

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
      browser.settle.map { _ =>
        val shown = as[dom.html.TextArea](present(
          browser.byClass("export-tts-text").headOption, "the TTS box"))
        assertEquals(shown.value, "TTS-STRING")
        assert(shown.readOnly)
        assertEquals(textOf(browser, "export-status"), "Copy this TTS string.")
      }
