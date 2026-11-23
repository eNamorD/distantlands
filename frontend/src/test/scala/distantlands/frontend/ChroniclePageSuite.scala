package distantlands.frontend

import distantlands.frontend.ChronicleFixtures.{convertingTransport, entry}
import distantlands.protocol.{ChronicleLegacy, ChronicleScore,
  ChronicleScorepadEntry}
import distantlands.testkit.Unwrap.{accepted, as, present}
import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The Chronicle page at `/`: which controls show for an empty browser,
  * saved entries and a loaded one, and loading, renaming and deleting. */
class ChroniclePageSuite extends munit.FunSuite:
  /** The page over `storage`, rendered into `browser`; the returned buffer
    * records each New or Continue chosen. */
  private def renderChroniclePage(browser: TestBrowser,
      storage: ChronicleStorage = new MemoryChronicleStorage(),
      kept: Boolean = true,
      transport: JsonTransport = convertingTransport())
      : ArrayBuffer[HostChronicle] =
    val choices = ArrayBuffer.empty[HostChronicle]
    ChroniclePage.render(browser.mount,
      new ChronicleShelf(storage, ChronicleFixtures.clock), kept,
      ChronicleClient(transport), FakeChronicleFiles(), choices += _)
    choices

  /** A storage holding `names` in order, each with digest d-1, and
    * `loaded` loaded. */
  private def storageHolding(loaded: Option[String], names: String*)
      : MemoryChronicleStorage =
    val storage = new MemoryChronicleStorage()
    val shelf = new ChronicleShelf(storage, ChronicleFixtures.clock)
    names.foreach(name => accepted(shelf.put(entry(name))))
    storage.values -= ChronicleShelf.LoadedKey
    loaded.foreach(shelf.load)
    storage

  private def storedNames(storage: ChronicleStorage): Vector[String] =
    new ChronicleShelf(storage, ChronicleFixtures.clock).saved.map(_.name)

  private def textOf(browser: TestBrowser, className: String): String =
    present(browser.byClass(className).headOption, className).textContent

  private def shows(browser: TestBrowser, className: String): Boolean =
    browser.byClass(className).nonEmpty

  /** Clicks the `className` control of the saved row named `name`. */
  private def clickInRow(browser: TestBrowser, name: String,
      className: String): Unit =
    val row = present(browser.byClass("chronicle-row").find(row =>
      row.querySelector(".chronicle-row-name").textContent == name), name)
    row.querySelector(s".$className").asInstanceOf[dom.html.Button].click()

  test("an empty browser keeps the Loaded Chronicle box with None, offers " +
      "New, leaves out Load, and withholds Edit and Export with reasons"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser)
      val box = browser.byClass("chronicle-loaded").head
      assertEquals(Dom.one(box, ".chronicle-loaded-label").textContent,
        "Loaded Chronicle")
      assertEquals(textOf(browser, "chronicle-loaded-none"), "None")
      assert(!shows(browser, "continue-chronicle"))
      assert(!shows(browser, "chronicle-scoresheet"))
      assert(shows(browser, "new-chronicle"))
      assert(shows(browser, "import-chronicle"))
      // With nothing saved there is nothing to load, so no Load button.
      assert(!shows(browser, "open-load-chronicle"))
      Vector("edit-chronicle" -> "Coming in a later release",
        "export-chronicle" -> "Load a Chronicle to export it").foreach {
        case (name, reason) =>
          val control = browser.byClass(name).head
          assertEquals(control.getAttribute("aria-disabled"), "true")
          assertEquals(control.getAttribute(HoverTip.Attribute), reason)
      }
      assert(!shows(browser, "chronicle-storage-refused"))

  test("Load Chronicle opens a dialog listing the saved entries"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(None, "Ashen Crown",
        "Iron Vow"))
      assert(!shows(browser, "chronicle-row"))
      browser.click("open-load-chronicle")
      val dialog = present(browser.byClass("load-chronicle-dialog").headOption,
        "the Load dialog")
      assertEquals(Dom.all(dialog, ".chronicle-row-name").map(_.textContent),
        Vector("Ashen Crown", "Iron Vow"))
      assertEquals(browser.byClass("chronicle-row-details").head.textContent,
        "Game 1 · Era: First to 20 · saved 2026-10-29")
      assertEquals(textOf(browser, "chronicle-loaded-none"), "None")

  test("a loaded entry shows its name, summary and Continue inside the box"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown"))
      val box = browser.byClass("chronicle-loaded").head
      assertEquals(textOf(browser, "chronicle-loaded-name"), "Ashen Crown")
      assertEquals(textOf(browser, "chronicle-summary-line"),
        "Game 1 · Era: First to 20")
      assertEquals(Dom.all(box, ".continue-chronicle").size, 1)
      assert(!shows(browser, "chronicle-loaded-none"))
      assert(!browser.byClass("export-chronicle").head
        .hasAttribute("aria-disabled"))

  test("a loaded entry shows no foundations, though its summary lists them"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        ChronicleFixtures.listingFoundations(entry("Ashen Crown"), 3)))
      renderChroniclePage(browser, storage)
      assert(!shows(browser, "foundation-card"))

  test("the scoresheet lists each lineage with its legacies and total, " +
      "and no game columns before a game is scored"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      val saved = entry("Ashen Crown")
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        saved.copy(summary = saved.summary.copy(scores = Vector(
          ChronicleScore("red-lineage", 9, legacies = Vector(
            ChronicleLegacy("Seer", active = false))),
          ChronicleScore("blue-lineage", 6))))))
      renderChroniclePage(browser, storage)
      val sheet = browser.byClass("chronicle-scoresheet").head
      assertEquals(Dom.all(sheet, "thead th").map(_.textContent),
        Vector("Lineage", "Total"))
      assertEquals(Dom.all(sheet, "tbody tr").map(row =>
        Dom.all(row, "th, td").map(_.textContent)), Vector(
        Vector("RedSeer (dormant)", "9"), Vector("Blue", "6")))

  test("the scoresheet shows the last five games, marks the Chancellor and " +
      "Citizens, leaves a sat-out game blank, and flags earlier games"):
    TestBrowser.useSynchronously(): browser =>
      def game(red: Int, blue: Option[Int]) = Vector(
        ChronicleScorepadEntry("red-lineage", red, Some("chancellor"))) ++
        blue.map(ChronicleScorepadEntry("blue-lineage", _, Some("citizen")))
      val storage = new MemoryChronicleStorage()
      val saved = entry("Ashen Crown")
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        saved.copy(summary = saved.summary.copy(game = Some(8),
          scorepad = Vector(game(3, Some(0)), game(3, Some(0)),
            game(6, Some(0)), game(6, Some(3)), game(9, Some(3)),
            game(9, None), game(9, Some(6)))))))
      renderChroniclePage(browser, storage)
      val sheet = browser.byClass("chronicle-scoresheet").head
      assertEquals(Dom.all(sheet, "thead th").map(_.textContent), Vector(
        "Lineage", "", "Game 3", "Game 4", "Game 5", "Game 6", "Game 7",
        "Total"))
      val rows = Dom.all(sheet, "tbody tr").map(row =>
        Dom.all(row, "td").map(_.textContent))
      assertEquals(rows, Vector(
        Vector("…", "6", "6", "9", "9", "9", "9"),
        Vector("…", "0", "3", "3", "", "6", "6")))
      val earlier = Dom.all(sheet, "td.scoresheet-earlier")
      assertEquals(earlier.map(_.getAttribute(HoverTip.Attribute)).distinct,
        Vector("Previous games not shown"))
      assertEquals(Dom.all(sheet, ".scoresheet-chancellor").size, 5)
      assertEquals(Dom.all(sheet, ".scoresheet-citizen").size, 4)
      assertEquals(Dom.all(sheet, ".scoresheet-citizen").head
        .getAttribute(HoverTip.Attribute), "Citizen")

  test("a Chronicle with no lineages yet says no points are scored, with " +
      "no scoresheet"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      val saved = entry("Ashen Crown")
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        saved.copy(summary = saved.summary.copy(scores = Vector.empty))))
      renderChroniclePage(browser, storage)
      assertEquals(textOf(browser, "chronicle-no-scores"),
        "No lineage points scored yet.")
      assert(!shows(browser, "chronicle-scoresheet"))

  test("five games or fewer show no earlier-games column"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      val saved = entry("Ashen Crown")
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        saved.copy(summary = saved.summary.copy(scorepad = Vector.fill(5)(
          Vector(ChronicleScorepadEntry("red-lineage", 3)))))))
      renderChroniclePage(browser, storage)
      assert(!shows(browser, "scoresheet-earlier"))
      assertEquals(Dom.all(browser.byClass("chronicle-scoresheet").head,
        "thead th").size, 7)

  test("the sections run in the spec's order"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown"))
      val known = Vector("chronicle-loaded", "new-chronicle",
        "open-load-chronicle", "edit-chronicle", "import-chronicle",
        "export-chronicle")
      val page = browser.byClass("chronicle-page").head
      val order = (0 until page.children.length).toVector
        .map(page.children(_).getAttribute("class"))
        .flatMap(classes => Option(classes).toVector
          .flatMap(_.split(" ")).filter(known.contains))
      assertEquals(order, known)

  test("Continue chooses the loaded Chronicle; New chooses a new one"):
    TestBrowser.useSynchronously(): browser =>
      val choices = renderChroniclePage(browser,
        storageHolding(Some("Ashen Crown"), "Ashen Crown"))
      browser.click("continue-chronicle")
      browser.click("new-chronicle")
      assertEquals(choices.toVector, Vector(
        HostChronicle.Continue(SavedChronicle(entry("Ashen Crown"),
          ChronicleFixtures.savedAt)),
        HostChronicle.New))

  test("Load makes an entry the loaded Chronicle and closes the dialog"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown",
        "Iron Vow")
      renderChroniclePage(browser, storage)
      browser.click("open-load-chronicle")
      clickInRow(browser, "Iron Vow", "load-chronicle")
      assertEquals(textOf(browser, "chronicle-loaded-name"), "Iron Vow")
      assertEquals(storage.values(ChronicleShelf.LoadedKey), "Iron Vow")
      assert(!shows(browser, "load-chronicle-dialog"))

  test("Delete asks first; Keep leaves the entry and the dialog open"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(None, "Ashen Crown")
      renderChroniclePage(browser, storage)
      browser.click("open-load-chronicle")
      browser.click("delete-chronicle")
      assertEquals(textOf(browser, "chronicle-confirm"),
        "Delete Ashen Crown?")
      assertEquals(storedNames(storage), Vector("Ashen Crown"))
      browser.click("keep-chronicle")
      assert(!shows(browser, "chronicle-confirm"))
      assert(shows(browser, "load-chronicle-dialog"))
      assertEquals(storedNames(storage), Vector("Ashen Crown"))

  test("confirmed Delete of the loaded entry clears the box and keeps the " +
      "dialog open on the rest"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown",
        "Iron Vow")
      renderChroniclePage(browser, storage)
      browser.click("open-load-chronicle")
      clickInRow(browser, "Ashen Crown", "delete-chronicle")
      browser.click("confirm-delete-chronicle")
      assert(shows(browser, "load-chronicle-dialog"))
      assertEquals(browser.byClass("chronicle-row-name").map(_.textContent),
        Vector("Iron Vow"))
      assertEquals(textOf(browser, "chronicle-loaded-none"), "None")
      assert(!shows(browser, "continue-chronicle"))

  test("deleting the last entry says nothing is saved, in the dialog"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(None, "Ashen Crown"))
      browser.click("open-load-chronicle")
      browser.click("delete-chronicle")
      browser.click("confirm-delete-chronicle")
      assertEquals(textOf(browser, "chronicle-saved-none"),
        "No Chronicles saved in this browser.")
      assert(!shows(browser, "open-load-chronicle"))
      // Its opener gone, closing the dialog focuses New Chronicle.
      browser.click("chronicle-dialog-close")
      assertEquals(dom.document.activeElement,
        browser.byClass("new-chronicle").head)

  test("an unreadable entry is listed as Unreadable with only Delete"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage(
        Map(ChronicleShelf.ListKey -> "[{\"name\":7}]"))
      renderChroniclePage(browser, storage)
      browser.click("open-load-chronicle")
      assertEquals(textOf(browser, "chronicle-row-name"), "Unreadable")
      assert(!shows(browser, "load-chronicle"))
      browser.click("delete-chronicle")
      browser.click("confirm-delete-chronicle")
      assert(!shows(browser, "chronicle-row"))

  test("Rename refuses an empty or taken name without a request"):
    TestBrowser.useSynchronously(): browser =>
      val requests = ArrayBuffer.empty[RecordedRequest]
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown", "Iron Vow"), transport = convertingTransport(requests))
      browser.click("rename-chronicle")
      browser.input("Chronicle name").value = "  "
      browser.click("save-chronicle-name")
      assert(browser.text.contains("A Chronicle needs a name."))
      browser.input("Chronicle name").value = "Iron Vow"
      browser.click("save-chronicle-name")
      assert(browser.text.contains(
        "A Chronicle named \"Iron Vow\" is already saved."))
      assertEquals(requests.size, 0)

  test("Rename saves the server's entry under the new name in place"):
    TestBrowser.use(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown",
        "Iron Vow")
      renderChroniclePage(browser, storage)
      browser.click("rename-chronicle")
      browser.input("Chronicle name").value = " Ember Throne "
      browser.click("save-chronicle-name")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "chronicle-loaded-name"), "Ember Throne")
        assertEquals(storedNames(storage), Vector("Ember Throne", "Iron Vow"))
        assertEquals(storage.values(ChronicleShelf.LoadedKey), "Ember Throne")
      }

  test("Cancel leaves the name as it was"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown")
      renderChroniclePage(browser, storage)
      browser.click("rename-chronicle")
      browser.input("Chronicle name").value = "Ember Throne"
      browser.click("cancel-chronicle-name")
      assertEquals(textOf(browser, "chronicle-loaded-name"), "Ashen Crown")
      assertEquals(storedNames(storage), Vector("Ashen Crown"))

  test("refused storage says Chronicles cannot be kept, and an import " +
      "still loads for the page"):
    TestBrowser.use(): browser =>
      renderChroniclePage(browser, kept = false)
      val notice = browser.byClass("chronicle-storage-refused").head
      assertEquals(notice.getAttribute("role"), "alert")
      browser.click("import-chronicle")
      as[dom.html.TextArea](browser.byClass("import-chronicle-tts").head)
        .value = "abc"
      browser.input("Name for the TTS import").value = "Iron Vow"
      browser.click("import-tts")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "chronicle-loaded-name"), "Iron Vow")
        assert(shows(browser, "chronicle-storage-refused"))
        // The redraw that shows the import closes the dialog.
        assert(!shows(browser, "chronicle-dialog"))
      }
