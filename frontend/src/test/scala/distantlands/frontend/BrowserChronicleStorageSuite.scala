package distantlands.frontend

import distantlands.testkit.Unwrap.present
import org.scalajs.dom
import scala.scalajs.js

/** `ChronicleStorage.browser`, held to the store contract over a working
  * `localStorage`: jsdom's own storage throws in this harness, so each test
  * installs a plain object as `window.localStorage` and puts back what was
  * there afterwards. */
class BrowserChronicleStorageSuite extends ChronicleStorageContract:
  private val window = dom.window.asInstanceOf[js.Dynamic]
  private val jsObject = js.Dynamic.global.Object
  private var replaced: js.Dynamic = js.undefined.asInstanceOf[js.Dynamic]

  /** Installs a `localStorage` stand-in over a plain dictionary holding
    * `initial`, whose `setItem` throws for every key that `refuses`. */
  private def installLocalStorage(refuses: String => Boolean,
      initial: Map[String, String] = Map.empty): Unit =
    replaced = jsObject.getOwnPropertyDescriptor(window, "localStorage")
    val items = js.Dictionary(initial.toSeq*)
    val getItem: js.Function1[String, String] =
      key => items.get(key).orNull
    val setItem: js.Function2[String, String, Unit] = (key, value) =>
      if refuses(key) then throw js.JavaScriptException("QuotaExceededError")
      items(key) = value
    val removeItem: js.Function1[String, Unit] = key => { items.remove(key); () }
    val storage = js.Dynamic.literal(getItem = getItem, setItem = setItem,
      removeItem = removeItem)
    jsObject.defineProperty(window, "localStorage",
      js.Dynamic.literal(value = storage, configurable = true))

  protected def make(): ChronicleStorage =
    installLocalStorage(_ => false)
    // A fixture whose setup throws is never released, so put the storage
    // back here if there is no store to release.
    try present(ChronicleStorage.browser,
      "a store over the installed localStorage")
    catch
      case error: Throwable =>
        putBackLocalStorage()
        throw error

  override protected def release(store: ChronicleStorage): Unit =
    putBackLocalStorage()

  private def putBackLocalStorage(): Unit =
    if js.isUndefined(replaced) then js.special.delete(window, "localStorage")
    else jsObject.defineProperty(window, "localStorage", replaced)
    ()

  test("a browser that throws on localStorage gives no store"):
    replaced = jsObject.getOwnPropertyDescriptor(window, "localStorage")
    jsObject.defineProperty(window, "localStorage", js.Dynamic.literal(
      configurable = true,
      get = (() => throw js.JavaScriptException("denied")): js.Function0[Unit]))
    try assertEquals(ChronicleStorage.browser, None)
    finally putBackLocalStorage()

  test("a browser that refuses every write gives no store"):
    installLocalStorage(_ => true)
    try assertEquals(ChronicleStorage.browser, None)
    finally putBackLocalStorage()

  test("a full storage that already holds Chronicles keeps its store"):
    installLocalStorage(_ => true, Map(ChronicleShelf.ListKey -> "[]"))
    try
      val store = present(ChronicleStorage.browser, "a store")
      assertEquals(store.read(ChronicleShelf.ListKey), Some("[]"))
      assert(!store.write(ChronicleShelf.ListKey, "[1]"))
    finally putBackLocalStorage()

  test("a storage that refuses writes, as a full one does, answers false"):
    installLocalStorage(_ != ChronicleStorage.Probe)
    try
      val store = present(ChronicleStorage.browser, "a store")
      assert(!store.write("oath.chronicles", "[]"))
      assertEquals(store.read("oath.chronicles"), None)
    finally putBackLocalStorage()
