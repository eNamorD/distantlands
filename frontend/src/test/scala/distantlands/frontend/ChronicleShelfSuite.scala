package distantlands.frontend

import distantlands.frontend.ChronicleFixtures.entry
import distantlands.testkit.Unwrap.{accepted, present}

/** The browser's stored Chronicles: the list's shape, unreadable entries,
  * the loaded Chronicle, names, and the automatic-save decision. */
class ChronicleShelfSuite extends munit.FunSuite:
  /** A storage whose every write is refused, as a full one is. */
  private final class RefusingStorage(initial: Map[String, String])
      extends MemoryChronicleStorage(initial):
    override def write(key: String, value: String): Boolean = false

  private def shelfOver(storage: ChronicleStorage): ChronicleShelf =
    new ChronicleShelf(storage, ChronicleFixtures.clock)

  /** A storage holding `names`, each saved once, with digest "d-1". */
  private def storageHolding(names: String*): MemoryChronicleStorage =
    val storage = new MemoryChronicleStorage()
    val shelf = shelfOver(storage)
    names.foreach(name => accepted(shelf.put(entry(name))))
    storage.values -= ChronicleShelf.LoadedKey
    storage

  private def names(shelf: ChronicleShelf): Vector[String] =
    shelf.saved.map(_.name)

  test("an empty browser has no rows and nothing loaded"):
    val shelf = shelfOver(new MemoryChronicleStorage())
    assertEquals(shelf.rows, Vector.empty)
    assertEquals(shelf.loaded, None)

  test("put stores {name, json, digest, summary, savedAt} and loads it"):
    val storage = new MemoryChronicleStorage()
    val shelf = shelfOver(storage)
    val saved = accepted(shelf.put(entry("Ashen Crown")))
    assertEquals(saved.savedAt, ChronicleFixtures.savedAt)
    val stored = ujson.read(storage.values(ChronicleShelf.ListKey)).arr.head.obj
    assertEquals(stored.keySet.toSet,
      Set("name", "json", "digest", "summary", "savedAt"))
    assertEquals(stored("name").str, "Ashen Crown")
    assertEquals(storage.values(ChronicleShelf.LoadedKey), "Ashen Crown")
    assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown"))
    assertEquals(shelf.loaded.map(_.entry), Some(entry("Ashen Crown")))

  test("put under a saved name replaces that entry in place"):
    val shelf = shelfOver(storageHolding("A", "B"))
    accepted(shelf.put(entry("A", "d-2")))
    assertEquals(names(shelf), Vector("A", "B"))
    assertEquals(present(shelf.find("A"), "A").entry.digest, "d-2")

  test("put with replacing renames in place and loads the new name"):
    val shelf = shelfOver(storageHolding("A", "B"))
    accepted(shelf.put(entry("C"), replacing = Some("A")))
    assertEquals(names(shelf), Vector("C", "B"))
    assertEquals(shelf.loaded.map(_.name), Some("C"))

  test("a refused write answers Refused and keeps the old list"):
    val before = storageHolding("A").values
    val storage = new RefusingStorage(before)
    val shelf = shelfOver(storage)
    assertEquals(shelf.put(entry("B")), Left(ChronicleShelf.Refused))
    assertEquals(storage.values, before)

  test("an element that does not parse is an Unreadable row"):
    val storage = storageHolding("A")
    val listed = ujson.read(storage.values(ChronicleShelf.ListKey)).arr
    listed += ujson.Obj("name" -> 7)
    storage.values += ChronicleShelf.ListKey -> ujson.write(listed)
    val shelf = shelfOver(storage)
    assertEquals(shelf.rows, Vector(
      ShelfRow.Readable(0, SavedChronicle(entry("A"), ChronicleFixtures.savedAt)),
      ShelfRow.Unreadable(1)))

  test("an entry whose name differs from its summary's is unreadable"):
    val storage = storageHolding("A")
    val listed = ujson.read(storage.values(ChronicleShelf.ListKey)).arr
    listed.head("name") = "Not A"
    storage.values += ChronicleShelf.ListKey -> ujson.write(listed)
    assertEquals(shelfOver(storage).rows, Vector(ShelfRow.Unreadable(0)))

  test("a value that is not a JSON array is one Unreadable row, and " +
      "deleting it empties the list"):
    val storage = new MemoryChronicleStorage(
      Map(ChronicleShelf.ListKey -> "not json"))
    val shelf = shelfOver(storage)
    assertEquals(shelf.rows, Vector(ShelfRow.Unreadable(0)))
    shelf.delete(0, None)
    assertEquals(storage.values(ChronicleShelf.ListKey), "[]")
    assertEquals(shelf.rows, Vector.empty)

  test("deleting the loaded entry clears the loaded Chronicle"):
    val storage = storageHolding("A", "B")
    val shelf = shelfOver(storage)
    shelf.load("A")
    shelf.delete(0, Some("A"))
    assertEquals(names(shelf), Vector("B"))
    assertEquals(shelf.loaded, None)
    assert(!storage.values.contains(ChronicleShelf.LoadedKey))

  test("deleting another entry keeps the loaded Chronicle"):
    val shelf = shelfOver(storageHolding("A", "B"))
    shelf.load("A")
    shelf.delete(1, Some("B"))
    assertEquals(shelf.loaded.map(_.name), Some("A"))

  test("delete leaves a row that no longer holds the entry shown"):
    val storage = storageHolding("A", "B")
    val stale = shelfOver(storage)
    shelfOver(storage).delete(0, Some("A"))
    stale.delete(0, Some("A"))
    assertEquals(names(stale), Vector("B"))

  test("load ignores a name the shelf does not hold"):
    val storage = storageHolding("A")
    shelfOver(storage).load("Z")
    assert(!storage.values.contains(ChronicleShelf.LoadedKey))

  test("nameProblem trims, refuses empty, over-long and taken names, and " +
      "allows the name being renamed"):
    val shelf = shelfOver(storageHolding("Ashen Crown"))
    assertEquals(shelf.nameProblem("   "), Some("A Chronicle needs a name."))
    assertEquals(shelf.nameProblem("x" * 81),
      Some("A Chronicle name has at most 80 characters."))
    assertEquals(shelf.nameProblem("x" * 80), None)
    assertEquals(shelf.nameProblem(" Ashen Crown "),
      Some("A Chronicle named \"Ashen Crown\" is already saved."))
    assertEquals(shelf.nameProblem("Ashen Crown",
      except = Some("Ashen Crown")), None)

  test("uniqueName counts up from (2) and stays within 80 characters"):
    val shelf = shelfOver(storageHolding("A", "A (2)"))
    assertEquals(shelf.uniqueName("A"), "A (3)")
    val long = shelf.uniqueName("y" * 80)
    assertEquals(long.length, 80)
    assert(long.endsWith(" (2)"), long)

  test("autoSave with no entry of that name saves and loads it"):
    val shelf = shelfOver(new MemoryChronicleStorage())
    val evolved = entry("A", "d-2")
    assertEquals(shelf.autoSave(evolved, "d-1"),
      AutoSave.Saved(SavedChronicle(evolved, ChronicleFixtures.savedAt)))
    assertEquals(shelf.loaded.map(_.entry), Some(evolved))

  test("autoSave with the evolved digest already saved changes nothing " +
      "but loads it"):
    val storage = new MemoryChronicleStorage()
    val shelf = new ChronicleShelf(storage, () => "earlier")
    accepted(shelf.put(entry("A", "d-2")))
    storage.values -= ChronicleShelf.LoadedKey
    val list = storage.values(ChronicleShelf.ListKey)
    assertEquals(shelfOver(storage).autoSave(entry("A", "d-2"), "d-1"),
      AutoSave.Unchanged)
    assertEquals(storage.values(ChronicleShelf.ListKey), list)
    assertEquals(storage.values(ChronicleShelf.LoadedKey), "A")

  test("autoSave over the starting Chronicle replaces it silently"):
    val shelf = shelfOver(storageHolding("A", "B"))
    val evolved = entry("A", "d-2")
    assertEquals(shelf.autoSave(evolved, "d-1"),
      AutoSave.Saved(SavedChronicle(evolved, ChronicleFixtures.savedAt)))
    assertEquals(names(shelf), Vector("A", "B"))
    assertEquals(present(shelf.find("A"), "A").entry.digest, "d-2")
    assertEquals(shelf.loaded.map(_.entry), Some(evolved))

  test("autoSave over any other digest is a Conflict naming the entry"):
    val shelf = shelfOver(storageHolding("A"))
    val existing = present(shelf.find("A"), "A")
    assertEquals(shelf.autoSave(entry("A", "d-2"), "d-0"),
      AutoSave.Conflict(existing))
    assertEquals(present(shelf.find("A"), "A").entry.digest, "d-1")

  test("autoSave whose write is refused is Refused"):
    val shelf = shelfOver(new RefusingStorage(Map.empty))
    assertEquals(shelf.autoSave(entry("A", "d-2"), "d-1"),
      AutoSave.Refused(ChronicleShelf.Refused))
