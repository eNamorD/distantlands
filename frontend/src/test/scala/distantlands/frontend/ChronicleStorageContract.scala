package distantlands.frontend

/** What every `ChronicleStorage` promises: a written key reads back, a
  * second write replaces the first, a removed or never-written key reads
  * as nothing, and a write that is kept answers true.
  * `MemoryChronicleStorage` and `ChronicleStorage.browser`, over a working
  * `localStorage`, both run it. */
private[frontend] abstract class ChronicleStorageContract extends munit.FunSuite:
  /** A new, empty store. */
  protected def make(): ChronicleStorage
  /** Undoes what `make` set up. */
  protected def release(store: ChronicleStorage): Unit = ()

  private val fresh = FunFixture[ChronicleStorage](_ => make(), release)

  fresh.test("a key that was written reads back its value"): store =>
    assert(store.write("oath.chronicles", "[]"))
    assertEquals(store.read("oath.chronicles"), Some("[]"))

  fresh.test("a second write to a key replaces the first"): store =>
    store.write("oath.chronicles.loaded", "Ashen Crown")
    store.write("oath.chronicles.loaded", "Iron Vow")
    assertEquals(store.read("oath.chronicles.loaded"), Some("Iron Vow"))

  fresh.test("a removed key reads as nothing"): store =>
    store.write("oath.chronicles.loaded", "Ashen Crown")
    store.remove("oath.chronicles.loaded")
    assertEquals(store.read("oath.chronicles.loaded"), None)

  fresh.test("a key that was never written reads as nothing"): store =>
    assertEquals(store.read("oath.chronicles"), None)
