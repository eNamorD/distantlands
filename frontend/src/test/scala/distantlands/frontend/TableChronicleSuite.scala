package distantlands.frontend

import distantlands.frontend.ChronicleFixtures.{convertingTransport, entry}
import distantlands.protocol.ChronicleEntry
import distantlands.protocol.projection.ChronicleProjection
import distantlands.testkit.Unwrap.{accepted, present}
import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The table's Chronicle section: the automatic save's four cases, the
  * dialog that asks, and export for every seat. */
class TableChronicleSuite extends munit.FunSuite:
  /** The Chronicle "Ashen Crown" started from digest d-1, evolved into
    * `evolved` once the Chronicle Phase journals it. */
  private def ashenCrown(evolved: Option[ChronicleEntry])
      : Option[ChronicleProjection] =
    Some(ChronicleProjection("Ashen Crown", "d-1", evolved))

  private val evolved = entry("Ashen Crown", "d-2")

  /** A table section over `storage` (none: the browser keeps nothing),
    * with its repaints counted. */
  private final class Table(browser: TestBrowser,
      storage: Option[ChronicleStorage],
      requests: ArrayBuffer[RecordedRequest] = ArrayBuffer.empty):
    var repaints = 0
    val chronicle = new TableChronicle(browser.mount,
      storage.map(new ChronicleShelf(_, ChronicleFixtures.clock)),
      ChronicleClient(convertingTransport(requests)), FakeChronicleFiles(),
      () => repaints += 1)
    def show(value: Option[ChronicleProjection]): Option[dom.Element] =
      val node = chronicle.section(value)
      node.foreach(browser.mount.appendChild)
      node

  /** A storage holding `saved`, loaded. */
  private def storageHolding(saved: ChronicleEntry*): MemoryChronicleStorage =
    val storage = new MemoryChronicleStorage()
    val shelf = new ChronicleShelf(storage, () => "earlier")
    saved.foreach(entry => accepted(shelf.put(entry)))
    storage

  private def shelfOver(storage: ChronicleStorage): ChronicleShelf =
    new ChronicleShelf(storage, ChronicleFixtures.clock)

  private def noteOf(node: Option[dom.Element]): String =
    present(node, "a section").querySelector(".table-chronicle-note")
      .textContent

  private def prompts(browser: TestBrowser): Vector[dom.Element] =
    browser.byClass("chronicle-save-prompt")

  test("before the Chronicle Phase the section names the Chronicle and " +
      "offers no export"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      val node = Table(browser, Some(storage)).show(ashenCrown(None))
      assertEquals(present(node, "a section").textContent,
        "Chronicle: Ashen Crown")
      assertEquals(browser.byClass("export-chronicle-json"), Vector.empty)
      assertEquals(storage.values, Map.empty[String, String])

  test("no section without a Chronicle"):
    TestBrowser.useSynchronously(): browser =>
      assertEquals(Table(browser, None).chronicle.section(None), None)

  test("an evolved Chronicle with no entry of its name is saved and loaded"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      val node = Table(browser, Some(storage)).show(ashenCrown(Some(evolved)))
      assertEquals(shelfOver(storage).loaded.map(_.entry), Some(evolved))
      assertEquals(noteOf(node), "Saved in this browser as \"Ashen Crown\".")
      assertEquals(browser.byClass("export-chronicle-json").size, 1)

  test("the starting Chronicle under that name is replaced silently"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(entry("Ashen Crown", "d-1"),
        entry("Iron Vow"))
      val node = Table(browser, Some(storage)).show(ashenCrown(Some(evolved)))
      assertEquals(shelfOver(storage).saved.map(_.entry),
        Vector(evolved, entry("Iron Vow")))
      assertEquals(shelfOver(storage).loaded.map(_.entry), Some(evolved))
      assertEquals(noteOf(node), "Saved in this browser as \"Ashen Crown\".")
      assertEquals(prompts(browser), Vector.empty)

  test("the evolved Chronicle already saved changes nothing"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(evolved, entry("Iron Vow"))
      val node = Table(browser, Some(storage)).show(ashenCrown(Some(evolved)))
      assertEquals(shelfOver(storage).saved.map(_.savedAt),
        Vector("earlier", "earlier"))
      assertEquals(shelfOver(storage).loaded.map(_.entry), Some(evolved))
      assertEquals(noteOf(node), "Already saved in this browser.")

  test("a different Chronicle under that name asks, and asks once"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(entry("Ashen Crown", "d-9"))
      val table = Table(browser, Some(storage))
      table.show(ashenCrown(Some(evolved)))
      table.show(ashenCrown(Some(evolved)))
      assertEquals(prompts(browser).size, 1)
      assertEquals(prompts(browser).head.getAttribute("role"), "dialog")
      assertEquals(browser.input("New Chronicle name").value,
        "Ashen Crown (2)")
      assertEquals(shelfOver(storage).saved.map(_.entry.digest),
        Vector("d-9"))

  test("Overwrite replaces the entry and closes the dialog"):
    TestBrowser.useSynchronously(): browser =>
      val storage = storageHolding(entry("Ashen Crown", "d-9"))
      val table = Table(browser, Some(storage))
      table.show(ashenCrown(Some(evolved)))
      browser.click("chronicle-overwrite")
      assertEquals(shelfOver(storage).saved.map(_.entry), Vector(evolved))
      assertEquals(prompts(browser), Vector.empty)
      assertEquals(table.repaints, 1)
      assertEquals(noteOf(table.show(ashenCrown(Some(evolved)))),
        "Saved in this browser as \"Ashen Crown\".")

  test("Save under new name renames through the server and keeps both"):
    TestBrowser.use(): browser =>
      val storage = storageHolding(entry("Ashen Crown", "d-9"))
      val table = Table(browser, Some(storage))
      table.show(ashenCrown(Some(evolved)))
      browser.click("chronicle-save-new")
      browser.settle.map { _ =>
        val shelf = shelfOver(storage)
        assertEquals(shelf.saved.map(_.name),
          Vector("Ashen Crown", "Ashen Crown (2)"))
        assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown (2)"))
        assertEquals(prompts(browser), Vector.empty)
        assertEquals(table.repaints, 1)
      }

  test("while a new name is being saved, Overwrite does nothing"):
    TestBrowser.use(): browser =>
      val storage = storageHolding(entry("Ashen Crown", "d-9"))
      val table = Table(browser, Some(storage))
      table.show(ashenCrown(Some(evolved)))
      browser.click("chronicle-save-new")
      browser.click("chronicle-overwrite")
      browser.click("chronicle-save-new")
      browser.settle.map { _ =>
        val shelf = shelfOver(storage)
        assertEquals(shelf.saved.map(_.entry.digest),
          Vector("d-9", "d-Ashen Crown (2)"))
        assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown (2)"))
        assertEquals(prompts(browser), Vector.empty)
        assertEquals(table.repaints, 1)
      }

  test("a redraw keeps the export controls, and what they show"):
    TestBrowser.useSynchronously(): browser =>
      val table = Table(browser, Some(new MemoryChronicleStorage()))
      val first = table.show(ashenCrown(Some(evolved)))
      val controls = present(first, "a section")
        .querySelector(".chronicle-export-controls")
      val second = table.show(ashenCrown(Some(evolved)))
      assert(present(second, "a section")
        .querySelector(".chronicle-export-controls") eq controls)

  test("a taken new name is refused in the dialog without a request"):
    TestBrowser.useSynchronously(): browser =>
      val requests = ArrayBuffer.empty[RecordedRequest]
      val storage = storageHolding(entry("Ashen Crown", "d-9"))
      Table(browser, Some(storage), requests).show(ashenCrown(Some(evolved)))
      browser.input("New Chronicle name").value = "Ashen Crown"
      browser.click("chronicle-save-new")
      assert(browser.byClass("chronicle-save-status").head.textContent
        .contains("already saved"))
      assertEquals(requests.size, 0)
      assertEquals(prompts(browser).size, 1)

  test("a browser that keeps nothing says so and still offers export"):
    TestBrowser.useSynchronously(): browser =>
      val node = Table(browser, None).show(ashenCrown(Some(evolved)))
      assertEquals(noteOf(node), "Chronicles cannot be kept in this " +
        "browser; export this one to keep it.")
      assertEquals(browser.byClass("export-chronicle-json").size, 1)

  test("a refused write shows the shelf's reason"):
    TestBrowser.useSynchronously(): browser =>
      val refusing = new MemoryChronicleStorage():
        override def write(key: String, value: String): Boolean = false
      val node = Table(browser, Some(refusing)).show(ashenCrown(Some(evolved)))
      assertEquals(noteOf(node), ChronicleShelf.Refused)
