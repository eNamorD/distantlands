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
    assert(store.write("distantlands.chronicles", "[]"))
    assertEquals(store.read("distantlands.chronicles"), Some("[]"))

  fresh.test("a second write to a key replaces the first"): store =>
    store.write("distantlands.chronicles.loaded", "Ashen Crown")
    store.write("distantlands.chronicles.loaded", "Iron Vow")
    assertEquals(store.read("distantlands.chronicles.loaded"), Some("Iron Vow"))

  fresh.test("a removed key reads as nothing"): store =>
    store.write("distantlands.chronicles.loaded", "Ashen Crown")
    store.remove("distantlands.chronicles.loaded")
    assertEquals(store.read("distantlands.chronicles.loaded"), None)

  fresh.test("a key that was never written reads as nothing"): store =>
    assertEquals(store.read("distantlands.chronicles"), None)
