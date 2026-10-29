package oathdigital.frontend

import oathdigital.frontend.ChronicleFixtures.{convertingTransport, entry}
import oathdigital.testkit.Unwrap.{accepted, as, present}
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

  test("an empty browser offers New, a withheld Edit, Import and a " +
      "withheld Export, and no Continue or Load"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser)
      assert(!shows(browser, "continue-chronicle"))
      assert(!shows(browser, "chronicle-saved"))
      assert(!shows(browser, "chronicle-loaded"))
      assert(shows(browser, "new-chronicle"))
      assert(shows(browser, "chronicle-import"))
      val edit = browser.byClass("edit-chronicle").head
      assertEquals(edit.getAttribute("aria-disabled"), "true")
      assertEquals(edit.getAttribute(HoverTip.Attribute),
        "Coming in a later release")
      assertEquals(browser.byClass("export-chronicle-json").head
        .getAttribute("aria-disabled"), "true")
      assert(!shows(browser, "chronicle-storage-refused"))

  test("entries with none loaded list them but offer no Continue"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(None, "Ashen Crown",
        "Iron Vow"))
      assertEquals(browser.byClass("chronicle-row-name").map(_.textContent),
        Vector("Ashen Crown", "Iron Vow"))
      assertEquals(browser.byClass("chronicle-row-details").head.textContent,
        "Game 1 · Era: 20 to win · saved 2026-10-29")
      assert(!shows(browser, "continue-chronicle"))
      assert(!shows(browser, "chronicle-loaded"))

  test("a loaded entry shows its name, summary and Continue"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown"))
      assertEquals(textOf(browser, "chronicle-loaded-name"), "Ashen Crown")
      assertEquals(textOf(browser, "chronicle-summary-line"),
        "Game 1 · Era: 20 to win · Red 9, Blue 6")
      assert(shows(browser, "continue-chronicle"))
      assert(!browser.byClass("export-chronicle-json").head
        .hasAttribute("aria-disabled"))

  test("the sections run in the spec's order"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown"))
      val known = Vector("chronicle-loaded", "continue-chronicle",
        "new-chronicle", "chronicle-saved", "edit-chronicle",
        "chronicle-import", "chronicle-export")
      val order = (0 until browser.mount.children.length).toVector
        .map(browser.mount.children(_).getAttribute("class"))
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

  test("Load makes an entry the loaded Chronicle"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown",
        "Iron Vow")
      renderChroniclePage(browser, storage)
      clickInRow(browser, "Iron Vow", "load-chronicle")
      assertEquals(textOf(browser, "chronicle-loaded-name"), "Iron Vow")
      assertEquals(storage.values(ChronicleShelf.LoadedKey), "Iron Vow")

  test("Delete asks first; Keep leaves the entry"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(None, "Ashen Crown")
      renderChroniclePage(browser, storage)
      browser.click("delete-chronicle")
      assertEquals(textOf(browser, "chronicle-confirm"),
        "Delete Ashen Crown?")
      assertEquals(storedNames(storage), Vector("Ashen Crown"))
      browser.click("keep-chronicle")
      assert(!shows(browser, "chronicle-confirm"))
      assertEquals(storedNames(storage), Vector("Ashen Crown"))

  test("confirmed Delete of the loaded entry clears the loaded Chronicle"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(Some("Ashen Crown"), "Ashen Crown",
        "Iron Vow")
      renderChroniclePage(browser, storage)
      clickInRow(browser, "Ashen Crown", "delete-chronicle")
      browser.click("confirm-delete-chronicle")
      assertEquals(browser.byClass("chronicle-row-name").map(_.textContent),
        Vector("Iron Vow"))
      assert(!shows(browser, "chronicle-loaded"))
      assert(!shows(browser, "continue-chronicle"))

  test("an unreadable entry is listed as Unreadable with only Delete"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage(
        Map(ChronicleShelf.ListKey -> "[{\"name\":7}]"))
      renderChroniclePage(browser, storage)
      assertEquals(textOf(browser, "chronicle-row-name"), "Unreadable")
      assert(!shows(browser, "load-chronicle"))
      browser.click("delete-chronicle")
      browser.click("confirm-delete-chronicle")
      assert(!shows(browser, "chronicle-saved"))

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
      as[dom.html.TextArea](browser.byClass("import-chronicle-tts").head)
        .value = "abc"
      browser.input("Name for the TTS import").value = "Iron Vow"
      browser.click("import-tts")
      browser.settle.map { _ =>
        assertEquals(textOf(browser, "chronicle-loaded-name"), "Iron Vow")
        assert(shows(browser, "chronicle-storage-refused"))
      }
