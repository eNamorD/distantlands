# Chronicle Browsing Slice 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a player keep Chronicles in their browser. They can load, rename, delete, import and export them, start a game from a new or loaded Chronicle, and get the evolved Chronicle saved automatically at the table.

**Architecture:**
- Everything is in the Scala.js frontend, apart from one server change: the server also serves the page at `/new-game`.
- `ChronicleStorage` wraps `localStorage` the same way `LogStore` does, so a suite can supply its own store. `ChronicleShelf` holds the stored list and its rules: unique names, unreadable entries, the loaded Chronicle, and the automatic-save decision.
- `ChronicleClient` calls `POST /chronicles/convert`. Renaming is a convert call: the browser changes the JSON's `name` and the server answers with the new canonical JSON, digest and summary.
- `ChroniclePages` sends the browser between the Chronicle page (`/`) and the player selection page (`/new-game`) with `history.pushState`. The player's choice of New or Continue is held in memory, so loading `/new-game` without a choice redirects to `/`.
- `TableChronicle` puts a Chronicle section in the table's action pane. The section saves the evolved Chronicle automatically and offers its export. When the browser already holds a different Chronicle under the same name, it asks the player in a dialog. The spec says "the table's menu gains Export Chronicle", but the trusted table has no menu. The export therefore sits in this section, which every seat sees.
- Every browser effect goes through `ChronicleDevices`, which bundles storage, file download and read, the clipboard, history and the clock. The jsdom suites pass fakes for all of them.

**Tech Stack:** Scala 3, Scala.js, scalajs-dom, ujson, munit (jsdom), Akka HTTP for the one route.

**Spec:** `docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md`, sections "Browser", "Errors" and "Testing", with slice 2 in "Order of work". The server side (slice 1) is already merged. `POST /chronicles/convert`, `ChronicleRequest.New|Saved` on `POST /games`, and `GameProjection.chronicle` already exist.

## Global Constraints

- **Branch:** work in a worktree off `0.2.0-alpha`. Symlink the main checkout's `.tooling` and `node_modules` into it before running `./sbtw`. Merge back into `0.2.0-alpha` only when the user says so. Push nothing.
- **Commits:** never `git add -A`; add the files you changed by name. End every commit message with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Storage keys:** `oath.chronicles` holds a JSON array of `{name, json, digest, summary, savedAt}`. `oath.chronicles.loaded` holds the loaded entry's name.
- **Names:** trimmed, 1 to 80 characters, and unique within a browser (exact match after trimming). The name is the identity; there is no other id.
- **Refused storage:** the page says Chronicles cannot be kept in this browser. Import, export and game creation still work for the Chronicle on screen, which lasts until the page is left. An entry that does not parse is listed as "Unreadable" with only Delete.
- **Automatic save:** look up the entry with the game's Chronicle name. If there is none, save the evolved Chronicle. If its digest equals the evolved digest, do nothing. If it equals the starting digest, replace it silently. If it is any other digest, ask at the table: save under a new name, or overwrite. The saved entry becomes the loaded Chronicle.
- **Disabled controls** are shown with a hover reason through `HoverTip.withhold`, never hidden. Edit Chronicle's reason is "Coming in a later release".
- **Errors:** the convert and create routes return the codec's or game start's reason. The page shows it in a `status` element beside the control that sent it.
- **TTS:** the codec has landed, so TTS import and export are on. The convert response's `tts` is absent when TTS cannot hold the Chronicle. The page then says "This Chronicle cannot be written as a TTS string."
- **Tests:** jsdom's `TestBrowser` makes `localStorage`, `sessionStorage` and `document.cookie` throw. Suites pass `MemoryChronicleStorage` or `None` for storage, plus `FakeChronicleFiles` and `FakePageHistory`. They never touch the real browser storage.
- **Build rules:** the 800-line file cap, `-Werror`, `python3 scripts/check-architecture.py` and `python3 scripts/check-markdown-links.py`. Run `./sbtw frontend/test` for the frontend. Run backend coverage of at least 92% statements (`./sbtw clean coverage test coverageReport`) once, in Task 8.
- **Copy:** user-facing text is sentence case and names the Chronicle in straight double quotes, e.g. `A Chronicle named "Ashen Crown" is already saved.`

## File map

| File | Change |
|---|---|
| `src/main/scala/oathdigital/server/ProductionFrontendRoutes.scala` | serve `index.html` at `/new-game` |
| `src/main/scala/oathdigital/server/DevelopmentRoutes.scala` | serve `frontend/index.html` at `/new-game` |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleStorage.scala` | new: `ChronicleStorage`, `MemoryChronicleStorage`, `ChronicleStorage.browser` |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleShelf.scala` | new: `SavedChronicle`, `ShelfRow`, `AutoSave`, `ChronicleShelf` |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleText.scala` | new: summary line, row details, lineage names |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleClient.scala` | new: convert and rename |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleDevices.scala` | new: `ChronicleFiles`, `PageHistory`, `ChronicleDevices` |
| `frontend/src/main/scala/oathdigital/frontend/ChronicleTransfer.scala` | new: import section, export controls |
| `frontend/src/main/scala/oathdigital/frontend/ChroniclePage.scala` | new: the `/` page |
| `frontend/src/main/scala/oathdigital/frontend/ChroniclePages.scala` | new: `HostChronicle`, routing between `/` and `/new-game` |
| `frontend/src/main/scala/oathdigital/frontend/TrustedHostUi.scala` | name field or loaded name, Back, `New`/`Saved`, save on success |
| `frontend/src/main/scala/oathdigital/frontend/Main.scala` | route `/` and `/new-game` to `ChroniclePages`; pass devices |
| `frontend/src/main/scala/oathdigital/frontend/TableChronicle.scala` | new: the table's Chronicle section, automatic save, the save dialog |
| `frontend/src/main/scala/oathdigital/frontend/TableScreen.scala` | append the Chronicle section |
| `frontend/src/main/scala/oathdigital/frontend/ServerModeUi.scala` | build `TableChronicle` |
| `frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala` | `textField` helper |
| `frontend/styles.css` | Chronicle page, list, import, export, table section, dialog |
| `frontend/src/test/scala/oathdigital/frontend/…` | new suites and fakes (listed per task); `ServerModeHostFormSuite` and `TestBrowser` updated |
| `docs/…` | spec status, ROADMAP, traceability |

---

### Task 1: Serve the page at `/new-game`

**Files:**
- Modify: `src/main/scala/oathdigital/server/ProductionFrontendRoutes.scala`
- Modify: `src/main/scala/oathdigital/server/DevelopmentRoutes.scala`
- Test: `src/test/scala/oathdigital/server/ProductionFrontendRoutesSuite.scala`, `src/test/scala/oathdigital/server/GameRoutesSuite.scala`

**Interfaces:**
- Produces: `GET /new-game` answers the frontend shell in both modes. A reload on the player selection page therefore loads the app, which redirects to `/` (Task 6).

- [ ] **Step 1: Write the failing tests**

In `ProductionFrontendRoutesSuite`, inside the existing test after the `index` checks, add:

```scala
      val newGame = server.get("/new-game")
      assertEquals(newGame.statusCode(), 200)
      assertEquals(newGame.body(), index.body())
      assertEquals(cacheControl(newGame), Some("no-cache"))
```

Open `GameRoutesSuite.scala` near line 50, where `server.get("/")` checks the development index. Add the same check for `/new-game` and assert that its body equals the `/` body.

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.server.ProductionFrontendRoutesSuite oathdigital.server.GameRoutesSuite"`
Expected: FAIL, because `/new-game` answers 404.

- [ ] **Step 3: Implement**

`ProductionFrontendRoutes.route`:

```scala
  val route: Route =
    pathEndOrSingleSlash {
      gamePage
    } ~
      // The player selection page is drawn by the app; a reload there loads
      // the same shell, which returns to the Chronicle page.
      path("new-game") {
        gamePage
      } ~
      pathPrefix("assets"):
```

`DevelopmentRoutes.route`, inside `respondWithHeader(DevelopmentAssetCacheControl)`:

```scala
          pathEndOrSingleSlash {
            getFromFile("frontend/index.html")
          } ~
          path("new-game") {
            getFromFile("frontend/index.html")
          } ~
          getFromDirectory("frontend")
```

- [ ] **Step 4: Run them to see them pass**

Run the same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/server/ProductionFrontendRoutes.scala src/main/scala/oathdigital/server/DevelopmentRoutes.scala src/test/scala/oathdigital/server/ProductionFrontendRoutesSuite.scala src/test/scala/oathdigital/server/GameRoutesSuite.scala
git commit -m "feat(chronicle): serve the app at /new-game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Chronicle storage and the shelf

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleStorage.scala`
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleShelf.scala`
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleText.scala`
- Test (new): `ChronicleStorageContract.scala`, `MemoryChronicleStorageSuite.scala`, `BrowserChronicleStorageSuite.scala`, `ChronicleShelfSuite.scala`, `ChronicleTextSuite.scala`, `ChronicleFixtures.scala`, all in `frontend/src/test/scala/oathdigital/frontend/`

**Interfaces:**
- Consumes: `oathdigital.protocol.{ChronicleEntry, ChronicleSummary, ChronicleScore, ChronicleEntryCodec}` (shared).
- Produces:
  - `trait ChronicleStorage { def read(key: String): Option[String]; def write(key: String, value: String): Boolean; def remove(key: String): Unit }`
  - `final class MemoryChronicleStorage(initial: Map[String, String] = Map.empty) extends ChronicleStorage { var values: Map[String, String] }`
  - `object ChronicleStorage { def browser: Option[ChronicleStorage] }`
  - `final case class SavedChronicle(entry: ChronicleEntry, savedAt: String) { def name: String }`
  - `enum ShelfRow { Readable(at: Int, saved: SavedChronicle); Unreadable(at: Int); def index: Int }`
  - `enum AutoSave { Saved(saved: SavedChronicle); Unchanged; Conflict(existing: SavedChronicle); Refused(reason: String) }`
  - `final class ChronicleShelf(storage: ChronicleStorage, now: () => String)` with `rows`, `saved`, `find(name)`, `loaded`, `nameProblem(name, except = None)`, `uniqueName(base)`, `put(entry, replacing = None): Either[String, SavedChronicle]`, `load(name)`, `delete(index)`, `autoSave(evolved, startingDigest): AutoSave`
  - `object ChronicleShelf { ListKey; LoadedKey; MaxNameLength = 80; Refused: String; def nameProblem(name: String): Option[String] }`
  - `object ChronicleText { def lineage(id: String): String; def summaryLine(summary: ChronicleSummary): String; def rowDetails(saved: SavedChronicle): String }`
  - Test fixture `ChronicleFixtures.entry(name, digest = "d-1", game = Some(1), json = None): ChronicleEntry` and `ChronicleFixtures.clock: () => String`

- [ ] **Step 1: Write the storage contract and its two suites**

`ChronicleStorageContract.scala`. It follows `LogStoreContract`, plus refusal and removal:

```scala
package oathdigital.frontend

/** What every `ChronicleStorage` promises: a written key reads back, a
  * second write replaces the first, a removed or never-written key reads
  * as nothing, and a write that is kept answers true.
  * `MemoryChronicleStorage` and `ChronicleStorage.browser`, over a working
  * `localStorage`, both run it. */
private[frontend] abstract class ChronicleStorageContract extends munit.FunSuite:
  protected def make(): ChronicleStorage
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
```

`MemoryChronicleStorageSuite.scala`:

```scala
package oathdigital.frontend

class MemoryChronicleStorageSuite extends ChronicleStorageContract:
  protected def make(): ChronicleStorage = new MemoryChronicleStorage()
```

`BrowserChronicleStorageSuite.scala`: copy `BrowserLogStoreSuite`'s set-up and release, which install a `localStorage` stand-in through `Object.defineProperty`. Give the stand-in a `removeItem` as well:

```scala
    val removeItem: js.Function1[String, Unit] = key => { items.remove(key); () }
    val storage = js.Dynamic.literal(getItem = getItem, setItem = setItem,
      removeItem = removeItem)
```

`make()` returns `present(ChronicleStorage.browser, "a store over the installed localStorage")`. Add two tests of its own beyond the contract:

```scala
  test("a browser that throws on localStorage gives no store"):
    val saved = jsObject.getOwnPropertyDescriptor(window, "localStorage")
    jsObject.defineProperty(window, "localStorage", js.Dynamic.literal(
      configurable = true,
      get = (() => throw js.JavaScriptException("denied")): js.Function0[Unit]))
    try assertEquals(ChronicleStorage.browser, None)
    finally restore(saved)

  test("a storage that refuses writes, as a full one does, answers false"):
    // install a stand-in whose setItem throws only for keys other than the
    // probe, then: assert(!store.write("oath.chronicles", "[]"))
```

For the second test, write the stand-in the same way as `make()`. Its `setItem` throws `js.JavaScriptException("QuotaExceededError")` when `key != ChronicleStorage.Probe`. Name the restore helper after what it does, e.g. `putBackLocalStorage`, as `BrowserLogStoreSuite` does.

- [ ] **Step 2: Write the shelf and text tests**

`ChronicleFixtures.scala`:

```scala
package oathdigital.frontend

import oathdigital.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}

/** Chronicle entries and a fixed clock for the browsing suites. An entry's
  * JSON is a stand-in, `{"format":1,"name":…}`: the browser never reads a
  * Chronicle's JSON except to rename it. */
private[frontend] object ChronicleFixtures:
  val savedAt = "2026-10-29T12:00:00.000Z"
  val clock: () => String = () => savedAt

  def entry(name: String, digest: String = "d-1", game: Option[Int] = Some(1),
      json: Option[String] = None): ChronicleEntry =
    ChronicleEntry(
      json.getOrElse(ujson.write(ujson.Obj("format" -> 1, "name" -> name))),
      digest,
      ChronicleSummary(name, game, 20, Vector(
        ChronicleScore("red-lineage", 9), ChronicleScore("blue-lineage", 6))))
```

`ChronicleShelfSuite.scala`. Each test builds `new ChronicleShelf(storage, ChronicleFixtures.clock)` over `new MemoryChronicleStorage(...)`, so a test can read `storage.values`. Write these tests, with full assertions:

```scala
  test("an empty browser has no rows and nothing loaded")
    // rows == Vector.empty, loaded == None

  test("put stores {name, json, digest, summary, savedAt} and loads it"):
    val storage = new MemoryChronicleStorage()
    val shelf = new ChronicleShelf(storage, ChronicleFixtures.clock)
    val saved = accepted(shelf.put(ChronicleFixtures.entry("Ashen Crown")))
    assertEquals(saved.savedAt, ChronicleFixtures.savedAt)
    val stored = ujson.read(storage.values("oath.chronicles")).arr.head.obj
    assertEquals(stored.keySet.toSet,
      Set("name", "json", "digest", "summary", "savedAt"))
    assertEquals(stored("name").str, "Ashen Crown")
    assertEquals(storage.values("oath.chronicles.loaded"), "Ashen Crown")
    assertEquals(shelf.loaded.map(_.name), Some("Ashen Crown"))

  test("put under a saved name replaces that entry in place")
    // put A, put B, put A with digest "d-2": names A, B in that order, A has d-2

  test("put with replacing renames in place and loads the new name")
    // put A, put B, put entry "C" replacing Some("A"): names C, B; loaded C

  test("a refused write answers Refused and keeps the old list"):
    // a storage whose write returns false: Left(ChronicleShelf.Refused)

  test("an element that does not parse is an Unreadable row"):
    // values "oath.chronicles" = [<valid entry A>, {"name":7}]
    // rows == Vector(Readable(0, A…), Unreadable(1))

  test("an entry whose name differs from its summary's is unreadable")

  test("a value that is not a JSON array is one Unreadable row, and " +
      "deleting it empties the list"):
    // values "oath.chronicles" = "not json": rows == Vector(Unreadable(0));
    // delete(0); storage.values("oath.chronicles") == "[]"

  test("deleting the loaded entry clears the loaded Chronicle")
  test("deleting another entry keeps the loaded Chronicle")
  test("load ignores a name the shelf does not hold")

  test("nameProblem trims, refuses empty, over-long and taken names, and " +
      "allows the name being renamed"):
    // "   " → Some("A Chronicle needs a name.")
    // "x" * 81 → Some("A Chronicle name has at most 80 characters.")
    // " Ashen Crown " with A saved → Some("A Chronicle named \"Ashen Crown\" is already saved.")
    // nameProblem("Ashen Crown", except = Some("Ashen Crown")) → None

  test("uniqueName counts up from (2) and stays within 80 characters"):
    // A and "A (2)" saved: uniqueName("A") == "A (3)"
    // uniqueName("y" * 80) has length 80 and ends with " (2)"

  test("autoSave with no entry of that name saves and loads it")
  test("autoSave with the evolved digest already saved changes nothing " +
      "but loads it")
  test("autoSave over the starting Chronicle replaces it silently")
  test("autoSave over any other digest is a Conflict naming the entry")
  test("autoSave whose write is refused is Refused")
```

Import `oathdigital.testkit.Unwrap.accepted`; `ServerModeHostFormSuite` already uses it. For the refusing storage, subclass `MemoryChronicleStorage` and override `write` to return `false`.

`ChronicleTextSuite.scala`:

```scala
  test("lineage names a generated lineage by its color"):
    assertEquals(ChronicleText.lineage("red-lineage"), "Red")
    assertEquals(ChronicleText.lineage("l9"), "L9")

  test("the summary line reads game, era target and scores"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleFixtures.entry("A", game = Some(3)).summary),
      "Game 3 · Era: 20 to win · Red 9, Blue 6")

  test("a TTS import's unknown game reads as unknown, and no scores add " +
      "nothing"):
    assertEquals(ChronicleText.summaryLine(
      ChronicleSummary("A", None, 20, Vector.empty)),
      "Game unknown · Era: 20 to win")

  test("a row's details add the saved date"):
    assertEquals(ChronicleText.rowDetails(SavedChronicle(
      ChronicleFixtures.entry("A"), ChronicleFixtures.savedAt)),
      "Game 1 · Era: 20 to win · saved 2026-10-29")
```

- [ ] **Step 3: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.MemoryChronicleStorageSuite oathdigital.frontend.BrowserChronicleStorageSuite oathdigital.frontend.ChronicleShelfSuite oathdigital.frontend.ChronicleTextSuite"`
Expected: compile errors, because the types do not exist yet.

- [ ] **Step 4: Implement `ChronicleStorage.scala`**

```scala
package oathdigital.frontend

import org.scalajs.dom
import scala.scalajs.js
import scala.util.Try

/** Where a browser keeps its Chronicles (Chronicle browsing design,
  * "Storage"). A seam, as `LogStore` is, so the suites can supply a store:
  * the jsdom harness makes the real one throw. */
private[frontend] trait ChronicleStorage:
  def read(key: String): Option[String]
  /** False when the browser refused the write, as a full storage does. */
  def write(key: String, value: String): Boolean
  def remove(key: String): Unit

/** A store that lasts as long as the page: the stand-in when the browser
  * keeps nothing, and the suites' store. `values` is the whole store. */
private[frontend] class MemoryChronicleStorage(
    initial: Map[String, String] = Map.empty) extends ChronicleStorage:
  var values: Map[String, String] = initial
  def read(key: String): Option[String] = values.get(key)
  def write(key: String, value: String): Boolean =
    values += key -> value
    true
  def remove(key: String): Unit = values -= key

private[frontend] object ChronicleStorage:
  /** Written and removed once to learn whether the browser keeps anything:
    * some private windows have a `localStorage` that refuses every write. */
  val Probe = "oath.chronicles.probe"

  /** The browser's `localStorage`, or nothing when the browser refuses it. */
  def browser: Option[ChronicleStorage] =
    Try(dom.window.localStorage).toOption
      .filter(storage => !js.isUndefined(storage) && storage != null)
      .filter(storage => Try {
        storage.setItem(Probe, Probe)
        storage.removeItem(Probe)
      }.isSuccess)
      .map(storage =>
        new ChronicleStorage:
          def read(key: String): Option[String] =
            Try(Option(storage.getItem(key))).toOption.flatten
          def write(key: String, value: String): Boolean =
            Try(storage.setItem(key, value)).isSuccess
          def remove(key: String): Unit =
            Try(storage.removeItem(key)).getOrElse(()))
```

- [ ] **Step 5: Implement `ChronicleShelf.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.{ChronicleEntry, ChronicleEntryCodec}
import scala.util.Try

/** One stored Chronicle: the server's entry and when this browser saved it. */
private[frontend] final case class SavedChronicle(entry: ChronicleEntry,
    savedAt: String):
  def name: String = entry.summary.name

/** A stored list element as the Chronicle page lists it. `index` is its
  * place in the stored array, which Delete removes. */
private[frontend] enum ShelfRow:
  case Readable(at: Int, saved: SavedChronicle)
  case Unreadable(at: Int)

  def index: Int = this match
    case Readable(at, _) => at
    case Unreadable(at) => at

/** What the automatic save did with an evolved Chronicle (Chronicle browsing
  * design, "Automatic save"). */
private[frontend] enum AutoSave:
  case Saved(saved: SavedChronicle)
  case Unchanged
  case Conflict(existing: SavedChronicle)
  case Refused(reason: String)

/** The browser's Chronicles: the list under `oath.chronicles` and the loaded
  * entry's name under `oath.chronicles.loaded`. Every read goes to the
  * storage, so two pages of one browser see each other's writes. */
private[frontend] final class ChronicleShelf(storage: ChronicleStorage,
    now: () => String):
  import ChronicleShelf.*

  private def items: Vector[ujson.Value] =
    storage.read(ListKey).fold(Vector.empty[ujson.Value])(text =>
      Try(ujson.read(text)).toOption match
        case Some(ujson.Arr(values)) => values.toVector
        // Not a list at all: one unreadable row, whose Delete empties it.
        case _ => Vector(ujson.Str(text)))

  private def store(values: Vector[ujson.Value]): Boolean =
    storage.write(ListKey, ujson.write(ujson.Arr.from(values)))

  def rows: Vector[ShelfRow] = items.zipWithIndex.map((value, index) =>
    decode(value).fold(ShelfRow.Unreadable(index))(ShelfRow.Readable(index, _)))

  def saved: Vector[SavedChronicle] =
    rows.collect { case ShelfRow.Readable(_, entry) => entry }

  def find(name: String): Option[SavedChronicle] = saved.find(_.name == name)

  def loaded: Option[SavedChronicle] = storage.read(LoadedKey).flatMap(find)

  /** Why `name` cannot be a new Chronicle's name here, if it cannot.
    * `except` is the name being renamed, which the new name may keep. */
  def nameProblem(name: String, except: Option[String] = None): Option[String] =
    val trimmed = name.trim
    ChronicleShelf.nameProblem(trimmed).orElse(Option.when(
      !except.contains(trimmed) && find(trimmed).nonEmpty)(
      s"A Chronicle named \"$trimmed\" is already saved."))

  /** `base (2)`, `base (3)`, … : the first that is free, cut to fit. */
  def uniqueName(base: String): String =
    Iterator.from(2).map { number =>
      val suffix = s" ($number)"
      base.trim.take(MaxNameLength - suffix.length).trim + suffix
    }.find(find(_).isEmpty).get

  /** Saves `entry` and loads it. An entry with its name, or with the name
    * `replacing`, is replaced where it stands; otherwise it is added last. */
  def put(entry: ChronicleEntry, replacing: Option[String] = None)
      : Either[String, SavedChronicle] =
    val added = SavedChronicle(entry, now())
    val names = Set(added.name) ++ replacing
    def matches(value: ujson.Value) = decode(value).exists(s => names(s.name))
    val current = items
    val at = current.indexWhere(matches)
    val next =
      if at < 0 then current :+ encode(added)
      else current.zipWithIndex.flatMap((value, index) =>
        if index == at then Some(encode(added))
        else Option.unless(matches(value))(value))
    if store(next) then
      storage.write(LoadedKey, added.name)
      Right(added)
    else Left(Refused)

  def load(name: String): Unit =
    if find(name).nonEmpty && !storage.read(LoadedKey).contains(name) then
      storage.write(LoadedKey, name)

  def delete(index: Int): Unit =
    val current = items
    val gone = current.lift(index).flatMap(decode).map(_.name)
    store(current.patch(index, Nil, 1))
    if gone.exists(name => storage.read(LoadedKey).contains(name)) then
      storage.remove(LoadedKey)

  def autoSave(evolved: ChronicleEntry, startingDigest: String): AutoSave =
    def saving = put(evolved).fold(AutoSave.Refused(_), AutoSave.Saved(_))
    find(evolved.summary.name) match
      case None => saving
      case Some(existing) if existing.entry.digest == evolved.digest =>
        load(existing.name)
        AutoSave.Unchanged
      case Some(existing) if existing.entry.digest == startingDigest => saving
      case Some(existing) => AutoSave.Conflict(existing)

private[frontend] object ChronicleShelf:
  val ListKey = "oath.chronicles"
  val LoadedKey = "oath.chronicles.loaded"
  /** The server's limit, `Chronicle.maxNameLength`. */
  val MaxNameLength = 80
  val Refused = "This browser refused to keep the Chronicle. Export it to " +
    "keep a copy."

  /** The name rules a browser can check alone: trimmed, 1 to 80 characters. */
  def nameProblem(name: String): Option[String] =
    val trimmed = name.trim
    if trimmed.isEmpty then Some("A Chronicle needs a name.")
    else if trimmed.length > MaxNameLength then
      Some(s"A Chronicle name has at most $MaxNameLength characters.")
    else None

  private def encode(saved: SavedChronicle): ujson.Value =
    val value = ChronicleEntryCodec.encodeValue(saved.entry)
    value("name") = saved.name
    value("savedAt") = saved.savedAt
    value

  private def decode(raw: ujson.Value): Option[SavedChronicle] = raw match
    case value: ujson.Obj => for
      entry <- ChronicleEntryCodec.decodeFields(value, "$").toOption
      name <- value.value.get("name").collect { case ujson.Str(text) => text }
      savedAt <- value.value.get("savedAt").collect { case ujson.Str(text) => text }
      if name == entry.summary.name
    yield SavedChronicle(entry, savedAt)
    case _ => None
```

- [ ] **Step 6: Implement `ChronicleText.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.ChronicleSummary

/** How the Chronicle pages word a Chronicle's summary. */
private[frontend] object ChronicleText:
  /** A generated lineage is `<color>-lineage`; it reads as its color. */
  def lineage(id: String): String = id.stripSuffix("-lineage").capitalize

  /** "Game 3 · Era: 20 to win · Red 9, Blue 6". */
  def summaryLine(summary: ChronicleSummary): String =
    (headline(summary) ++ Option.when(summary.scores.nonEmpty)(
      summary.scores.map(score => s"${lineage(score.lineage)} ${score.score}")
        .mkString(", "))).mkString(" · ")

  /** "Game 3 · Era: 20 to win · saved 2026-10-29". */
  def rowDetails(saved: SavedChronicle): String =
    (headline(saved.entry.summary) :+ s"saved ${saved.savedAt.take(10)}")
      .mkString(" · ")

  private def headline(summary: ChronicleSummary): Vector[String] = Vector(
    summary.game.fold("Game unknown")(game => s"Game $game"),
    s"Era: ${summary.eraTarget} to win")
```

- [ ] **Step 7: Run the suites to see them pass**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/ChronicleStorage.scala frontend/src/main/scala/oathdigital/frontend/ChronicleShelf.scala frontend/src/main/scala/oathdigital/frontend/ChronicleText.scala frontend/src/test/scala/oathdigital/frontend/ChronicleStorageContract.scala frontend/src/test/scala/oathdigital/frontend/MemoryChronicleStorageSuite.scala frontend/src/test/scala/oathdigital/frontend/BrowserChronicleStorageSuite.scala frontend/src/test/scala/oathdigital/frontend/ChronicleShelfSuite.scala frontend/src/test/scala/oathdigital/frontend/ChronicleTextSuite.scala frontend/src/test/scala/oathdigital/frontend/ChronicleFixtures.scala
git commit -m "feat(chronicle): keep Chronicles in browser storage

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The convert client and the browser devices

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleClient.scala`
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleDevices.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala` (add `textField`)
- Modify: `frontend/src/test/scala/oathdigital/frontend/ChronicleFixtures.scala` (add `converted`, `convertingTransport`, `devices`)
- Test (new): `ChronicleClientSuite.scala`, `ChronicleFilesSuite.scala`, `FakeChronicleFiles.scala`, `FakePageHistory.scala`

**Interfaces:**
- Consumes: `JsonTransport`, `GameJson.responseFailure`, `ChronicleConvertRequest`, `ChronicleConvertRequestCodec`, `ChronicleConvertResponse`, `ChronicleConvertResponseCodec`, plus `MemoryChronicleStorage` and `ChronicleFixtures` from Task 2.
- Produces:
  - `final class ChronicleClient(transport: JsonTransport) { def convert(request: ChronicleConvertRequest): Future[Either[String, ChronicleConvertResponse]]; def rename(entry: ChronicleEntry, name: String): Future[Either[String, ChronicleEntry]] }`
  - `object ChronicleClient { def withName(json: String, name: String): Either[String, String] }`
  - `trait ChronicleFiles { def download(filename: String, text: String): Unit; def copy(text: String): Future[Boolean]; def read(input: dom.html.Input): Future[Either[String, String]] }`
  - `object ChronicleFiles { def filename(name: String): String; val browser: ChronicleFiles }`
  - `trait PageHistory { def push(path: String): Unit; def replace(path: String): Unit; def onPop(handler: String => Unit): Unit }`
  - `object PageHistory { val browser: PageHistory }`
  - `final case class ChronicleDevices(storage: Option[ChronicleStorage], files: ChronicleFiles, history: PageHistory, now: () => String)` and `object ChronicleDevices { def browser: ChronicleDevices }`
  - `ServerUiSupport.textField(className: String, label: String, value: String): dom.html.Input`
  - Test fakes: `FakeChronicleFiles(var fileText: Either[String, String] = Left("Choose a Chronicle file."))` with `downloads: ArrayBuffer[(String, String)]`, `copies: ArrayBuffer[String]` and `var copyWorks = true`. `FakePageHistory` with `pushed`, `replaced` and `back(path: String)`.
  - Fixtures: `ChronicleFixtures.converted(entry, tts = None): TransportResponse`, `ChronicleFixtures.convertingTransport(requests = ArrayBuffer.empty, tts = Some("TTS-STRING")): ScriptedTransport`, `ChronicleFixtures.devices(storage = Some(new MemoryChronicleStorage), files = new FakeChronicleFiles, history = new FakePageHistory): ChronicleDevices`

- [ ] **Step 1: Write the fakes and fixtures**

`FakeChronicleFiles.scala`:

```scala
package oathdigital.frontend

import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** Fake of `ChronicleFiles`: records downloads and copies, and answers
  * every file read with `fileText`. `copyWorks = false` is a browser that
  * will not copy. */
private[frontend] final class FakeChronicleFiles(
    var fileText: Either[String, String] = Left("Choose a Chronicle file."))
    extends ChronicleFiles:
  val downloads = ArrayBuffer.empty[(String, String)]
  val copies = ArrayBuffer.empty[String]
  var copyWorks = true
  def download(filename: String, text: String): Unit =
    downloads += filename -> text
  def copy(text: String): Future[Boolean] =
    if copyWorks then copies += text
    Future.successful(copyWorks)
  def read(input: dom.html.Input): Future[Either[String, String]] =
    Future.successful(fileText)
```

`FakePageHistory.scala`:

```scala
package oathdigital.frontend

import scala.collection.mutable.ArrayBuffer

/** Fake of `PageHistory`: records pushed and replaced paths; `back(path)`
  * plays the browser's Back button landing on `path`. */
private[frontend] final class FakePageHistory extends PageHistory:
  val pushed = ArrayBuffer.empty[String]
  val replaced = ArrayBuffer.empty[String]
  private var popped: String => Unit = _ => ()
  def push(path: String): Unit = pushed += path
  def replace(path: String): Unit = replaced += path
  def onPop(handler: String => Unit): Unit = popped = handler
  def back(path: String): Unit = popped(path)
```

Add to `ChronicleFixtures`. Its imports grow to include `ChronicleConvertRequest`, `ChronicleConvertRequestCodec`, `ChronicleConvertResponse`, `ChronicleConvertResponseCodec`, `ArrayBuffer` and `Future`:

```scala
  def converted(entry: ChronicleEntry, tts: Option[String] = None)
      : TransportResponse =
    TransportResponse(200, ChronicleConvertResponseCodec.encode(
      ChronicleConvertResponse(entry, tts)))

  /** Answers `POST /chronicles/convert` as the server would for these
    * fixtures: JSON comes back under the name it carries, with digest
    * "d-<name>"; a TTS string comes back under its name with no game. Any
    * other request is a 404. */
  def convertingTransport(
      requests: ArrayBuffer[RecordedRequest] = ArrayBuffer.empty,
      tts: Option[String] = Some("TTS-STRING")): ScriptedTransport =
    ScriptedTransport((_, url, body) => Future.successful(Right(
      if url != "/chronicles/convert" then
        TransportResponse(404, """{"error":"not-found","message":"no route"}""")
      else ChronicleConvertRequestCodec.decode(body.getOrElse("")) match
        case Right(ChronicleConvertRequest.FromJson(json)) =>
          val name = ujson.read(json)("name").str
          converted(entry(name, s"d-$name", json = Some(json)), tts)
        case Right(ChronicleConvertRequest.FromTts(_, name)) =>
          converted(entry(name, s"d-$name", game = None), tts)
        case Left(_) =>
          TransportResponse(400, """{"error":"malformed","message":"bad"}"""))),
      requests)

  def devices(storage: Option[ChronicleStorage] =
        Some(new MemoryChronicleStorage()),
      files: FakeChronicleFiles = new FakeChronicleFiles(),
      history: FakePageHistory = new FakePageHistory()): ChronicleDevices =
    ChronicleDevices(storage, files, history, clock)
```

The `Future.successful` here needs no execution context.

- [ ] **Step 2: Write the failing client and file tests**

`ChronicleClientSuite.scala` (async munit tests, with `JSExecutionContext.Implicits.queue` imported):

```scala
  test("convert posts the request and decodes the answer"):
    val requests = ArrayBuffer.empty[RecordedRequest]
    val client = ChronicleClient(ChronicleFixtures.convertingTransport(requests))
    client.convert(ChronicleConvertRequest.FromTts("abc", "Iron Vow")).map {
      result =>
        assertEquals(accepted(result).entry.summary.name, "Iron Vow")
        assertEquals(accepted(result).tts, Some("TTS-STRING"))
        assertEquals(requests.map(r => r.method -> r.url).toVector,
          Vector("POST" -> "/chronicles/convert"))
    }

  test("a refusal answers the server's reason"):
    val client = ChronicleClient(ScriptedTransport((_, _, _) =>
      Future.successful(Right(TransportResponse(422,
        """{"error":"invalid-chronicle","message":"$.era: bad"}""")))))
    client.convert(ChronicleConvertRequest.FromJson("{}"))
      .map(result => assertEquals(result, Left("$.era: bad")))

  test("a transport failure answers its message")
    // ScriptedTransport answering Left(GameClientFailure.NetworkFailure("offline"))
    // → Left("offline")

  test("an answer that does not decode says so")
    // 200 with body "{}" → Left starting "The server's answer could not be read"

  test("rename rewrites the JSON's name and returns the server's entry"):
    val requests = ArrayBuffer.empty[RecordedRequest]
    val client = ChronicleClient(ChronicleFixtures.convertingTransport(requests))
    client.rename(ChronicleFixtures.entry("Ashen Crown"), "  Iron Vow ").map {
      result =>
        assertEquals(accepted(result).summary.name, "Iron Vow")
        val sent = accepted(ChronicleConvertRequestCodec.decode(
          present(requests.head.body, "a request body")))
        sent match
          case ChronicleConvertRequest.FromJson(json) =>
            assertEquals(ujson.read(json)("name").str, "Iron Vow")
          case other => fail(other.toString)
    }

  test("rename of JSON that does not parse answers without a request")
    // entry with json = Some("not json") → Left("The saved Chronicle could not be read."), requests empty
```

`ChronicleFilesSuite.scala`:

```scala
  test("a download is named after the Chronicle, with unsafe characters " +
      "replaced"):
    assertEquals(ChronicleFiles.filename("Ashen Crown"),
      "Ashen Crown.chronicle.json")
    assertEquals(ChronicleFiles.filename("a/b:c?"), "a-b-c-.chronicle.json")
```

- [ ] **Step 3: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.ChronicleClientSuite oathdigital.frontend.ChronicleFilesSuite"`
Expected: compile errors.

- [ ] **Step 4: Implement `ChronicleClient.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec, ChronicleConvertResponse,
  ChronicleConvertResponseCodec, ChronicleEntry}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try

/** The browser's one Chronicle route, `POST /chronicles/convert` (Chronicle
  * browsing design, "Server"). Every failure comes back as the words the
  * page shows. */
private[frontend] final class ChronicleClient(transport: JsonTransport):
  def convert(request: ChronicleConvertRequest)
      : Future[Either[String, ChronicleConvertResponse]] =
    transport.request("POST", "/chronicles/convert",
        Some(ChronicleConvertRequestCodec.encode(request)))
      .map(_.left.map(_.message).flatMap { response =>
        if response.status >= 200 && response.status < 300 then
          ChronicleConvertResponseCodec.decode(response.body).left.map(error =>
            s"The server's answer could not be read: ${error.message}")
        else Left(GameJson.responseFailure(response).message)
      })

  /** The Chronicle under `name`: the browser changes the JSON's name and the
    * server returns the new canonical JSON, digest and summary. */
  def rename(entry: ChronicleEntry, name: String)
      : Future[Either[String, ChronicleEntry]] =
    ChronicleClient.withName(entry.json, name.trim) match
      case Left(reason) => Future.successful(Left(reason))
      case Right(json) =>
        convert(ChronicleConvertRequest.FromJson(json)).map(_.map(_.entry))

private[frontend] object ChronicleClient:
  def withName(json: String, name: String): Either[String, String] =
    Try {
      val value = ujson.read(json)
      value("name") = name
      ujson.write(value)
    }.toOption.toRight("The saved Chronicle could not be read.")
```

If `GameClientFailure.HttpFailure.message` is not the server's message, read `GameClient.scala:69-80` and use the field that is. The test "a refusal answers the server's reason" decides it.

- [ ] **Step 5: Implement `ChronicleDevices.scala`**

```scala
package oathdigital.frontend

import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Files and the clipboard, for import and export (Chronicle browsing
  * design, "Chronicle page"). */
private[frontend] trait ChronicleFiles:
  def download(filename: String, text: String): Unit
  /** True once the text is on the clipboard. */
  def copy(text: String): Future[Boolean]
  /** The text of the file chosen in `input`, or why there is none. */
  def read(input: dom.html.Input): Future[Either[String, String]]

private[frontend] object ChronicleFiles:
  /** `<name>.chronicle.json`, with characters no file system allows
    * replaced by "-". */
  def filename(name: String): String =
    name.replaceAll("""[\\/:*?"<>|\p{Cntrl}]""", "-") + ".chronicle.json"

  val browser: ChronicleFiles = new ChronicleFiles:
    def download(filename: String, text: String): Unit =
      val blob = new dom.Blob(js.Array[dom.BlobPart](text),
        new dom.BlobPropertyBag { `type` = "application/json" })
      val url = dom.URL.createObjectURL(blob)
      val link = dom.document.createElement("a").asInstanceOf[dom.html.Anchor]
      link.href = url
      link.setAttribute("download", filename)
      dom.document.body.appendChild(link)
      link.click()
      dom.document.body.removeChild(link)
      dom.URL.revokeObjectURL(url)

    def copy(text: String): Future[Boolean] =
      val clipboard = dom.window.navigator.asInstanceOf[js.Dynamic]
        .selectDynamic("clipboard")
      if js.isUndefined(clipboard) || clipboard == null then
        Future.successful(false)
      else clipboard.writeText(text).asInstanceOf[js.Promise[Unit]].toFuture
        .map(_ => true).recover { case _ => false }

    def read(input: dom.html.Input): Future[Either[String, String]] =
      Option(input.files).filter(_.length > 0).map(_(0)) match
        case None => Future.successful(Left("Choose a Chronicle file."))
        case Some(file) => file.asInstanceOf[js.Dynamic].text()
          .asInstanceOf[js.Promise[String]].toFuture.map(Right(_))
          .recover { case _ => Left("The file could not be read.") }

/** The address bar, for moving between the Chronicle page and player
  * selection without reloading. */
private[frontend] trait PageHistory:
  def push(path: String): Unit
  def replace(path: String): Unit
  /** `handler` gets the path the browser's Back or Forward lands on. */
  def onPop(handler: String => Unit): Unit

private[frontend] object PageHistory:
  val browser: PageHistory = new PageHistory:
    def push(path: String): Unit = dom.window.history.pushState(null, "", path)
    def replace(path: String): Unit =
      dom.window.history.replaceState(null, "", path)
    def onPop(handler: String => Unit): Unit =
      dom.window.addEventListener("popstate", (_: dom.PopStateEvent) =>
        handler(dom.window.location.pathname))

/** Every browser effect the Chronicle pages and the table's Chronicle
  * section use. `storage` is absent when the browser keeps nothing; `now`
  * is an ISO 8601 timestamp. */
private[frontend] final case class ChronicleDevices(
    storage: Option[ChronicleStorage], files: ChronicleFiles,
    history: PageHistory, now: () => String)

private[frontend] object ChronicleDevices:
  def browser: ChronicleDevices = ChronicleDevices(ChronicleStorage.browser,
    ChronicleFiles.browser, PageHistory.browser,
    () => new js.Date().toISOString())
```

If the scalajs-dom version in `build.sbt` has no `BlobPart` or `BlobPropertyBag` in that form, use whatever it provides for `new Blob([text], {type})`, and keep the behavior the same.

- [ ] **Step 6: Add `textField` to `ServerUiSupport`**

Next to `button`:

```scala
  /** A text box named `label` for assistive technology, holding `value`. */
  private[frontend] def textField(className: String, label: String,
      value: String): dom.html.Input =
    val node = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    node.className = className
    node.setAttribute("aria-label", label)
    node.setAttribute("autocomplete", "off")
    node.value = value
    node
```

- [ ] **Step 7: Run the suites to see them pass**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/ChronicleClient.scala frontend/src/main/scala/oathdigital/frontend/ChronicleDevices.scala frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala frontend/src/test/scala/oathdigital/frontend/ChronicleClientSuite.scala frontend/src/test/scala/oathdigital/frontend/ChronicleFilesSuite.scala frontend/src/test/scala/oathdigital/frontend/FakeChronicleFiles.scala frontend/src/test/scala/oathdigital/frontend/FakePageHistory.scala frontend/src/test/scala/oathdigital/frontend/ChronicleFixtures.scala
git commit -m "feat(chronicle): convert, rename and the browser's file seams

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Import and export controls

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/ChronicleTransfer.scala`
- Modify: `frontend/src/test/scala/oathdigital/frontend/TestBrowser.scala` (add `change`)
- Modify: `frontend/styles.css`
- Test (new): `ChronicleTransferSuite.scala`

**Interfaces:**
- Consumes: from Tasks 2-3, `ChronicleShelf`, `SavedChronicle`, `ChronicleClient`, `ChronicleFiles`, `textField`, `HoverTip.withhold`, and `ChronicleFixtures`.
- Produces:
  - `ChronicleTransfer.importSection(shelf: ChronicleShelf, client: ChronicleClient, files: ChronicleFiles, imported: () => Unit): dom.Element`. It calls `imported()` once an import is saved and loaded.
  - `ChronicleTransfer.exportSection(entry: Option[ChronicleEntry], client: ChronicleClient, files: ChronicleFiles): dom.Element`
  - `ChronicleTransfer.exportControls(entry: Option[ChronicleEntry], client: ChronicleClient, files: ChronicleFiles): dom.Element`, reused at the table.
  - `TestBrowser.change(name: String): Unit` fires the `onchange` of the first element with class `name`.
  - CSS classes: `import-chronicle-file`, `import-chronicle-tts`, `import-chronicle-name`, `import-tts`, `import-status`, `import-clash`, `import-new-name`, `import-rename`, `import-replace`, `import-cancel`, `export-chronicle-json`, `export-chronicle-tts`, `export-status`, `export-tts-text`

- [ ] **Step 1: Add `TestBrowser.change`**

Next to `click`:

```scala
  def change(name: String): Unit =
    byClass(name).head.asInstanceOf[js.Dynamic].onchange(
      js.Dynamic.literal(preventDefault = (() => ()): js.Function0[Unit]))
```

- [ ] **Step 2: Write the failing tests**

`ChronicleTransferSuite.scala`. Each test mounts the section into `browser.mount` inside `TestBrowser.use()` and waits on `browser.settle` after every asynchronous click. Write a helper named after what it builds, such as `mountImportSection(browser, shelf, transport, files): ArrayBuffer[Unit]`, which returns the recorded `imported()` calls. Tests:

```scala
  test("a JSON file is converted, saved and loaded"):
    // files.fileText = Right(entry("Ashen Crown").json); change("import-chronicle-file")
    // after settle: shelf.loaded.map(_.name) == Some("Ashen Crown"); imported.size == 1;
    // the one request is POST /chronicles/convert with FromJson of that text

  test("a file that cannot be read shows why, beside the control")
    // files.fileText = Left("The file could not be read."); text appears in import-status; no request

  test("a convert refusal shows the server's reason")
    // transport answers 422 {"error":"invalid-chronicle","message":"$.era: bad"}
    // import-status text == "$.era: bad"; shelf.saved empty

  test("a TTS string needs a string and a name")
    // click import-tts with empty textarea → "Paste a TTS string to import."
    // fill textarea, empty name → "A Chronicle needs a name."; no requests

  test("a TTS string is imported under the name given, trimmed")
    // textarea "abc", name "  Iron Vow "; click import-tts
    // request FromTts("abc", "Iron Vow"); shelf.loaded name "Iron Vow"

  test("a name clash offers a new name or replacing the saved entry"):
    // shelf holds "Ashen Crown" (digest d-1); import a file of "Ashen Crown"
    // import-clash shows; import-new-name value == "Ashen Crown (2)"; nothing saved yet

  test("importing under the new name renames through the server")
    // continue the clash: click import-rename; second request is FromJson whose name is
    // "Ashen Crown (2)"; shelf names == Vector("Ashen Crown", "Ashen Crown (2)"); loaded the new one

  test("a taken new name is refused without a request")
    // set import-new-name to "Ashen Crown"; click import-rename → "already saved" text, 1 request

  test("replacing keeps one entry, the import's")
    // click import-replace: shelf.saved.map(_.entry.digest) == Vector("d-Ashen Crown")

  test("cancel drops the clash and saves nothing")

  test("export without a Chronicle is withheld with a reason"):
    // exportControls(None, …): both buttons have aria-disabled="true" and
    // HoverTip note "Load a Chronicle to export it"; clicking does nothing

  test("Download JSON downloads <name>.chronicle.json holding the JSON")
    // files.downloads == Vector("Ashen Crown.chronicle.json" -> entry.json)

  test("Copy TTS string copies the server's TTS")
    // files.copies == Vector("TTS-STRING"); export-status "TTS string copied."

  test("a Chronicle TTS cannot hold says so")
    // convertingTransport(tts = None) → "This Chronicle cannot be written as a TTS string."

  test("a browser that will not copy shows the string to copy by hand")
    // files.copyWorks = false → export-tts-text textarea holds "TTS-STRING", readOnly
```

To read a hover note, use the attribute `HoverTip` sets. Look for `Attribute` in `HoverTip.scala`, or read the note through an existing helper in `HoverTipSuite`.

- [ ] **Step 3: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.ChronicleTransferSuite"`
Expected: compile errors.

- [ ] **Step 4: Implement `ChronicleTransfer.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.{ChronicleConvertRequest, ChronicleEntry}
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
          client.rename(entry, field.value).foreach(_.fold(
            reason => status.textContent = reason,
            renamed => keep(shelf.put(renamed))))
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
    file.onchange = _ => files.read(file).foreach(_.fold(
      reason => status.textContent = reason,
      json => convert(ChronicleConvertRequest.FromJson(json))))
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
```

`HoverTip.withhold` clears `onclick`. That is why it is applied only in the `None` branch, where no handler is set.

- [ ] **Step 5: Add the styles**

Append to `frontend/styles.css`, after the `.seat-link` rules:

```css
/* Chronicle page (Chronicle browsing design, "Chronicle page"). */
.chronicle-import, .chronicle-export, .chronicle-saved, .chronicle-loaded {
  max-width: 40rem; margin: 1.5rem 0; display: grid; gap: 0.75rem; }
.import-chronicle-tts { font: inherit; min-height: 5rem; padding: 0.5rem;
  overflow-wrap: anywhere; }
.import-chronicle-name, .import-new-name { font: inherit; padding: 0.5rem; }
.import-clash { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem; }
.import-clash:empty { display: none; }
.import-clash > p { flex: 1 0 100%; margin: 0; }
.chronicle-export-controls { display: flex; flex-wrap: wrap; align-items: center;
  gap: 0.75rem; }
.chronicle-export-controls .status { flex: 1 0 100%; margin: 0; }
.export-tts-text { flex: 1 0 100%; font: inherit; min-height: 4rem;
  overflow-wrap: anywhere; }
```

- [ ] **Step 6: Run the suite to see it pass**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/ChronicleTransfer.scala frontend/src/test/scala/oathdigital/frontend/ChronicleTransferSuite.scala frontend/src/test/scala/oathdigital/frontend/TestBrowser.scala frontend/styles.css
git commit -m "feat(chronicle): import and export a Chronicle as JSON or TTS

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The Chronicle page

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/ChroniclePage.scala`
- Modify: `frontend/styles.css`
- Test (new): `ChroniclePageSuite.scala`

**Interfaces:**
- Consumes: from Tasks 2-4, `ChronicleShelf`, `ShelfRow`, `SavedChronicle`, `ChronicleClient`, `ChronicleFiles`, `ChronicleText`, `ChronicleTransfer`, `textField` and `HoverTip.withhold`.
- Produces:
  - `enum HostChronicle { case New; case Continue(saved: SavedChronicle) }`, defined in `ChroniclePage.scala`.
  - `ChroniclePage.render(mount: dom.Element, shelf: ChronicleShelf, kept: Boolean, client: ChronicleClient, files: ChronicleFiles, choose: HostChronicle => Unit): Unit`
  - CSS classes: `chronicle-storage-refused`, `chronicle-loaded`, `chronicle-loaded-name`, `rename-chronicle`, `chronicle-rename`, `save-chronicle-name`, `cancel-chronicle-name`, `chronicle-summary-line`, `continue-chronicle`, `new-chronicle`, `chronicle-saved`, `chronicle-row`, `chronicle-row-name`, `chronicle-row-details`, `load-chronicle`, `delete-chronicle`, `confirm-delete-chronicle`, `keep-chronicle`, `edit-chronicle`

- [ ] **Step 1: Write the failing tests**

`ChroniclePageSuite.scala`. Write a helper `renderChroniclePage(browser, storage = new MemoryChronicleStorage(), kept = true, transport = ChronicleFixtures.convertingTransport()): ArrayBuffer[HostChronicle]`. It renders into `browser.mount` and returns the recorded choices. Tests:

```scala
  test("an empty browser offers New, a withheld Edit, Import and a " +
      "withheld Export, and no Continue or Load"):
    // byClass("continue-chronicle") empty, byClass("chronicle-saved") empty,
    // new-chronicle present, edit-chronicle aria-disabled with note
    // "Coming in a later release", export buttons aria-disabled

  test("entries with none loaded list them but offer no Continue")
    // two entries stored, no loaded key: two chronicle-row, no continue-chronicle,
    // no chronicle-loaded

  test("a loaded entry shows its name, summary and Continue"):
    // chronicle-loaded-name "Ashen Crown"; chronicle-summary-line
    // "Game 1 · Era: 20 to win · Red 9, Blue 6"; continue-chronicle present;
    // export buttons not withheld

  test("the sections run in the spec's order")
    // the classes of the page's top-level children, filtered to the known ones, read
    // chronicle-loaded, continue-chronicle, new-chronicle, chronicle-saved,
    // edit-chronicle, chronicle-import, chronicle-export

  test("Continue chooses the loaded Chronicle; New chooses a new one")
    // choices == Vector(HostChronicle.Continue(saved A), HostChronicle.New)

  test("Load makes an entry the loaded Chronicle")
    // click the load-chronicle of "Iron Vow": chronicle-loaded-name "Iron Vow";
    // storage.values("oath.chronicles.loaded") == "Iron Vow"

  test("Delete asks first; Keep leaves the entry")
  test("confirmed Delete of the loaded entry clears the loaded Chronicle")
    // after confirm: one chronicle-row left, no chronicle-loaded, no continue-chronicle

  test("an unreadable entry is listed as Unreadable with only Delete")
    // chronicle-row-name "Unreadable"; that row has no load-chronicle

  test("Rename refuses an empty or taken name without a request")
  test("Rename saves the server's entry under the new name in place")
    // click rename-chronicle, set chronicle-rename to "Ember Throne", click
    // save-chronicle-name; settle; chronicle-loaded-name "Ember Throne"; stored names
    // keep their order with the renamed one in place; loaded key "Ember Throne"
  test("Cancel leaves the name as it was")

  test("refused storage says Chronicles cannot be kept, and an import " +
      "still loads for the page")
    // kept = false with a MemoryChronicleStorage: chronicle-storage-refused
    // present with role alert; import a TTS string; chronicle-loaded-name shows it
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.ChroniclePageSuite"`
Expected: compile errors.

- [ ] **Step 3: Implement `ChroniclePage.scala`**

```scala
package oathdigital.frontend

import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** What player selection starts a game from: a new Chronicle, named there,
  * or the loaded one. */
private[frontend] enum HostChronicle:
  case New
  case Continue(saved: SavedChronicle)

/** The Chronicle page at `/` (Chronicle browsing design, "Chronicle page").
  * Every change redraws the whole page from the shelf. `kept` is false when
  * the browser refused storage; the shelf then lasts as long as the page. */
private[frontend] object ChroniclePage:
  def render(mount: dom.Element, shelf: ChronicleShelf, kept: Boolean,
      client: ChronicleClient, files: ChronicleFiles,
      choose: HostChronicle => Unit): Unit =
    def redraw(): Unit = render(mount, shelf, kept, client, files, choose)
    mount.textContent = ""
    mount.appendChild(text("h1", "", "Oath Digital"))
    if !kept then
      val notice = text("p", "status chronicle-storage-refused",
        "Chronicles cannot be kept in this browser. A Chronicle you import " +
          "or create stays here until you leave this page; export it to " +
          "keep a copy.")
      notice.setAttribute("role", "alert")
      mount.appendChild(notice)
    val loaded = shelf.loaded
    loaded.foreach { saved =>
      mount.appendChild(loadedSummary(saved, shelf, client, () => redraw()))
      val continue = button("Continue Chronicle", "continue-chronicle")
      continue.onclick = _ => choose(HostChronicle.Continue(saved))
      mount.appendChild(continue)
    }
    val fresh = button("New Chronicle", "new-chronicle")
    fresh.onclick = _ => choose(HostChronicle.New)
    mount.appendChild(fresh)
    val rows = shelf.rows
    if rows.nonEmpty then mount.appendChild(savedList(rows, shelf, () => redraw()))
    val edit = button("Edit Chronicle", "edit-chronicle")
    HoverTip.withhold(edit, "Coming in a later release")
    mount.appendChild(edit)
    mount.appendChild(ChronicleTransfer.importSection(shelf, client, files,
      () => redraw()))
    mount.appendChild(ChronicleTransfer.exportSection(loaded.map(_.entry),
      client, files))

  private def loadedSummary(saved: SavedChronicle, shelf: ChronicleShelf,
      client: ChronicleClient, redraw: () => Unit): dom.Element =
    val section = element("section", "chronicle-loaded")
    section.setAttribute("aria-label", "Loaded Chronicle")
    val heading = element("div", "chronicle-loaded-heading")
    val status = text("p", "status", "")
    status.setAttribute("role", "status")

    def showName(): Unit =
      heading.textContent = ""
      heading.appendChild(text("h2", "chronicle-loaded-name", saved.name))
      val rename = button("Rename", "rename-chronicle")
      rename.setAttribute("aria-label", s"Rename ${saved.name}")
      rename.onclick = _ => editName()
      heading.appendChild(rename)

    def editName(): Unit =
      heading.textContent = ""
      val field = textField("chronicle-rename", "Chronicle name", saved.name)
      val save = button("Save", "save-chronicle-name")
      val cancel = button("Cancel", "cancel-chronicle-name")
      cancel.onclick = _ =>
        status.textContent = ""
        showName()
      save.onclick = _ =>
        shelf.nameProblem(field.value, except = Some(saved.name)) match
          case Some(problem) => status.textContent = problem
          case None if field.value.trim == saved.name => showName()
          case None =>
            save.disabled = true
            status.textContent = "Renaming…"
            client.rename(saved.entry, field.value).foreach { result =>
              result.flatMap(entry => shelf.put(entry, Some(saved.name))) match
                case Left(reason) =>
                  save.disabled = false
                  status.textContent = reason
                case Right(_) => redraw()
            }
      Vector(field, save, cancel).foreach(heading.appendChild)
      field.focus()

    showName()
    section.appendChild(heading)
    section.appendChild(text("p", "chronicle-summary-line",
      ChronicleText.summaryLine(saved.entry.summary)))
    section.appendChild(status)
    section

  private def savedList(rows: Vector[ShelfRow], shelf: ChronicleShelf,
      redraw: () => Unit): dom.Element =
    val section = element("section", "chronicle-saved")
    section.appendChild(text("h2", "", "Load Chronicle"))
    val list = element("ol", "chronicle-list")
    rows.foreach { row =>
      val item = element("li", "chronicle-row")
      val controls = element("span", "chronicle-row-controls")
      val name = row match
        case ShelfRow.Readable(_, saved) =>
          item.appendChild(text("span", "chronicle-row-name", saved.name))
          item.appendChild(text("span", "chronicle-row-details",
            ChronicleText.rowDetails(saved)))
          val load = button("Load", "load-chronicle")
          load.setAttribute("aria-label", s"Load ${saved.name}")
          load.onclick = _ =>
            shelf.load(saved.name)
            redraw()
          controls.appendChild(load)
          saved.name
        case ShelfRow.Unreadable(_) =>
          item.appendChild(text("span", "chronicle-row-name", "Unreadable"))
          "this unreadable entry"
      val delete = button("Delete", "delete-chronicle")
      delete.setAttribute("aria-label", s"Delete $name")
      delete.onclick = _ =>
        controls.textContent = ""
        controls.appendChild(text("span", "chronicle-confirm", s"Delete $name?"))
        val confirm = button("Delete", "confirm-delete-chronicle")
        confirm.onclick = _ =>
          shelf.delete(row.index)
          redraw()
        val keep = button("Keep", "keep-chronicle")
        keep.onclick = _ => redraw()
        controls.appendChild(confirm)
        controls.appendChild(keep)
        keep.focus()
      controls.appendChild(delete)
      item.appendChild(controls)
      list.appendChild(item)
    }
    section.appendChild(list)
    section
```

- [ ] **Step 4: Add the styles**

Append after the Task 4 styles:

```css
.chronicle-loaded-heading { display: flex; flex-wrap: wrap; align-items: center;
  gap: 0.75rem; }
.chronicle-loaded-name { margin: 0; font-family: Georgia, serif; font-size: 1.6rem;
  color: var(--cream); }
.chronicle-rename { font: inherit; padding: 0.5rem; flex: 1 1 14rem; min-width: 0; }
.chronicle-summary-line { margin: 0; color: var(--brass); }
.continue-chronicle, .new-chronicle { margin: 0.5rem 0.75rem 0.5rem 0; }
.chronicle-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.5rem; }
.chronicle-row { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem; }
.chronicle-row-name { font-weight: 700; }
.chronicle-row-details { color: var(--brass); flex: 1 1 14rem; }
.chronicle-row-controls { display: flex; gap: 0.5rem; align-items: center; }
```

- [ ] **Step 5: Run the suite to see it pass**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/ChroniclePage.scala frontend/src/test/scala/oathdigital/frontend/ChroniclePageSuite.scala frontend/styles.css
git commit -m "feat(chronicle): the Chronicle page lists, loads, renames and deletes

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Player selection and routing

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/ChroniclePages.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/TrustedHostUi.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/Main.scala`
- Modify: `frontend/src/test/scala/oathdigital/frontend/ServerModeHostFormSuite.scala`
- Test (new): `ChroniclePagesSuite.scala`

**Interfaces:**
- Consumes: from Tasks 2-5, `HostChronicle`, `ChroniclePage.render`, `ChronicleShelf`, `MemoryChronicleStorage`, `ChronicleClient`, `ChronicleDevices` and `textField`. From the shared protocol, `ChronicleRequest.New|Saved` and `TrustedGameCreateResponse.chronicle`.
- Produces:
  - `ChroniclePages.NewGamePath = "/new-game"`
  - `ChroniclePages.start(mount: dom.Element, pathname: String, transport: JsonTransport, devices: ChronicleDevices, openCreated: Option[TrustedGameCreateResponse => Unit] = None): Unit`
  - `TrustedHostUi.start(mount, transport, chronicle: HostChronicle, shelf: ChronicleShelf, back: () => Unit, openCreated = None)`
  - `Main.start(mount, pathname, trustedAlpha, transport, navigate = …, devices: ChronicleDevices = ChronicleDevices.browser)`
  - CSS classes: `chronicle-back`, `chronicle-name`, `chronicle-continued`

- [ ] **Step 1: Write the failing routing tests**

`ChroniclePagesSuite.scala` uses `ChronicleFixtures.devices(...)`. Its transport answers `POST /games` with the `seatLinksResponse` body from `ServerModeHostFormSuite`, which you move into `ChronicleFixtures` as `seatLinksResponse(name: String)`, and answers convert through `convertingTransport`. Tests:

```scala
  test("/new-game without a choice returns to the Chronicle page"):
    // ChroniclePages.start(mount, "/new-game", …): history.replaced == Vector("/");
    // new-chronicle present; no create-trusted-game

  test("New Chronicle opens player selection with a dated, free name"):
    // click new-chronicle: history.pushed == Vector("/new-game");
    // input "Chronicle name" value starts with "Chronicle 20"; Back present

  test("the default name skips a saved one")
    // storage holds an entry named TrustedHostUi.defaultChronicleName():
    // the field holds that name + " (2)"

  test("Back returns to the Chronicle page")
    // click chronicle-back: history.pushed ends with "/"; new-chronicle present

  test("the browser's Back button returns to the Chronicle page")
    // history.back("/"): new-chronicle present

  test("a taken or empty name is refused beside Create game, with no request")

  test("Create with New sends the name and saves and loads the returned " +
      "Chronicle"):
    // name "  Ashen Crown "; request.chronicle == ChronicleRequest.New("Ashen Crown");
    // shelf.loaded name == the response's chronicle summary name

  test("Continue shows the loaded name and sends its JSON"):
    // storage holds A, loaded; click continue-chronicle: chronicle-continued
    // text "Chronicle: Ashen Crown"; no "Chronicle name" input; create sends
    // ChronicleRequest.Saved(A.entry.json); stored list unchanged

  test("a refused saved Chronicle shows the server's reason and keeps the " +
      "form editable"):
    // POST /games answers 422 {"error":"chronicle-refused",
    //   "message":"a non-empty world requires the Empire"}
    // status text contains that message; create-trusted-game not disabled

  test("refused storage still creates from a new Chronicle")
    // devices(storage = None): create succeeds; seat links shown
```

- [ ] **Step 2: Update `ServerModeHostFormSuite` to reach the form through the Chronicle page**

Change `startHostForm` so it passes `devices = ChronicleFixtures.devices()` to `Main.start` and then calls `browser.click("new-chronicle")`. Make the same change in the tests that call `Main.start(browser.mount, "/", …)` directly. Those are the duplicate-game test and the development-root test. In the development-root test, the assertion `requests.size == 0` before the click still holds. The assertions after it are unchanged.

- [ ] **Step 3: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.ChroniclePagesSuite oathdigital.frontend.ServerModeHostFormSuite"`
Expected: compile errors.

- [ ] **Step 4: Implement `ChroniclePages.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.TrustedGameCreateResponse
import org.scalajs.dom

/** The two start pages (Chronicle browsing design, "Browser"): the
  * Chronicle page at `/` and player selection at `/new-game`. The choice of
  * New or Continue lives only in this page's memory, so `/new-game` without
  * one, as after a reload, returns to `/`. */
private[frontend] object ChroniclePages:
  val NewGamePath = "/new-game"

  def start(mount: dom.Element, pathname: String, transport: JsonTransport,
      devices: ChronicleDevices,
      openCreated: Option[TrustedGameCreateResponse => Unit] = None): Unit =
    val kept = devices.storage.nonEmpty
    val shelf = new ChronicleShelf(
      devices.storage.getOrElse(new MemoryChronicleStorage()), devices.now)
    val client = new ChronicleClient(transport)
    var choice = Option.empty[HostChronicle]

    def show(path: String): Unit =
      if path == NewGamePath then choice match
        case Some(chosen) => TrustedHostUi.start(mount, transport, chosen,
          shelf, () => { devices.history.push("/"); show("/") }, openCreated)
        case None =>
          devices.history.replace("/")
          show("/")
      else ChroniclePage.render(mount, shelf, kept, client, devices.files,
        chosen => {
          choice = Some(chosen)
          devices.history.push(NewGamePath)
          show(NewGamePath)
        })

    devices.history.onPop(show)
    show(pathname)
```

- [ ] **Step 5: Change `TrustedHostUi`**

- Change the signature to `start(mount: dom.Element, transport: JsonTransport, chronicle: HostChronicle, shelf: ChronicleShelf, back: () => Unit, openCreated: Option[TrustedGameCreateResponse => Unit] = None)`.
- Replace the doc of `defaultChronicleName` with `"Chronicle 2026-10-29", today's date: the name field's default.`
- After the intro paragraph and before `val form`, add:

```scala
    val backLink = button("Back", "chronicle-back")
    backLink.setAttribute("type", "button")
    backLink.onclick = event => { event.preventDefault(); back() }
    mount.appendChild(backLink)
```

- After `val form = element("form", "trusted-host-form")`, add:

```scala
    val nameField = chronicle match
      case HostChronicle.New =>
        val default = defaultChronicleName()
        val field = textField("chronicle-name", "Chronicle name",
          if shelf.find(default).isEmpty then default
          else shelf.uniqueName(default))
        val label = element("label", "host-field")
        label.appendChild(text("span", "", "Chronicle name"))
        label.appendChild(field)
        form.appendChild(label)
        Some(field)
      case HostChronicle.Continue(saved) =>
        form.appendChild(text("p", "chronicle-continued",
          s"Chronicle: ${saved.name}"))
        None
```

- In `submit()`, put the name check first:

```scala
      val problem =
        nameField.flatMap(field => shelf.nameProblem(field.value)).orElse(
          if rows.size < MinPlayers then Some(s"Need at least $MinPlayers players.")
          else …existing player checks…)
```

- Build the request from the choice:

```scala
      val request = TrustedGameCreateRequest(participants, chronicle match
        case HostChronicle.New =>
          ChronicleRequest.New(nameField.fold("")(_.value.trim))
        case HostChronicle.Continue(saved) =>
          ChronicleRequest.Saved(saved.entry.json))
```

- On success, save a new Chronicle before either branch runs:

```scala
            case Right(created) =>
              val unsaved = chronicle match
                case HostChronicle.New => shelf.put(created.chronicle).left.toOption
                case HostChronicle.Continue(_) => None
              …then the existing openCreated / seat-links branches. In the
              seat-links branch, set the status to
              "Game created. Copy and save each assigned seat link." +
                unsaved.fold("")(reason => s" $reason")
```

  Keep the two existing branches (`openCreated.nonEmpty` and seat links). The guard order is unchanged.
- Update the object's doc. It now says that the page is player selection (`/new-game`), that it starts a game from a new or loaded Chronicle, and that it saves a new Chronicle once the game is created.
- Add `textField` to the `ServerUiSupport._` import. That import is already a wildcard.

- [ ] **Step 6: Change `Main`**

```scala
  def main(args: Array[String]): Unit =
    …
    start(mount, dom.window.location.pathname,
      mount.getAttribute("data-runtime-mode") == "trusted-alpha",
      new SameOriginJsonTransport)

  private[frontend] def start(mount: dom.Element, pathname: String,
      trustedAlpha: Boolean, transport: JsonTransport,
      navigate: String => Unit = url => dom.window.location.assign(url),
      devices: ChronicleDevices = ChronicleDevices.browser): Unit =
    val startPage = pathname == "/" || pathname == ChroniclePages.NewGamePath
    ServerUiSupport.canonicalGameId(pathname) match
      case Some(gameId) =>
        ServerModeUi.start(mount, new TrustedHttpGameClient(transport), Some(gameId))
      case None if trustedAlpha && startPage =>
        ChroniclePages.start(mount, pathname, transport, devices)
      case None if trustedAlpha => mount.textContent = "Open your assigned seat link."
      case None if ServerUiSupport.queryParameter("gameId").isEmpty =>
        // Development games are created exactly like trusted ones, then opened
        // in the development table as the first listed player.
        ChroniclePages.start(mount, pathname, transport, devices, Some(created =>
          navigate(ServerUiSupport.sessionUrl(created.gameId, created.seats.head.playerId))))
      case None => ServerModeUi.start(mount, new HttpGameClient(transport),
        startOver = () => navigate(DevelopmentStartUrl))
```

Task 7 passes `devices` and `transport` on to `ServerModeUi`. Leave those calls as they are for now.

- [ ] **Step 7: Add the styles**

```css
.chronicle-back { margin: 0 0 1rem; }
.chronicle-name { font: inherit; padding: 0.5rem; }
.chronicle-continued { margin: 0; font-weight: 700; }
```

- [ ] **Step 8: Run the frontend suites**

Run: `./sbtw frontend/test`
Expected: PASS, including every existing `ServerModeSessionSuite` test. Those open games by path or `?gameId=` and never reach the start pages.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/ChroniclePages.scala frontend/src/main/scala/oathdigital/frontend/TrustedHostUi.scala frontend/src/main/scala/oathdigital/frontend/Main.scala frontend/src/test/scala/oathdigital/frontend/ChroniclePagesSuite.scala frontend/src/test/scala/oathdigital/frontend/ServerModeHostFormSuite.scala frontend/src/test/scala/oathdigital/frontend/ChronicleFixtures.scala frontend/styles.css
git commit -m "feat(chronicle): start a game from a new or loaded Chronicle

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Automatic save and export at the table

**Files:**
- Create: `frontend/src/main/scala/oathdigital/frontend/TableChronicle.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/TableScreen.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/ServerModeUi.scala`
- Modify: `frontend/src/main/scala/oathdigital/frontend/Main.scala` (pass `transport` and `devices`)
- Modify: `frontend/styles.css`
- Test (new): `TableChronicleSuite.scala`. Modify: `ServerModeSessionSuite.scala` (one wiring test)

**Interfaces:**
- Consumes: from Tasks 2-4, `ChronicleShelf.autoSave`, `AutoSave`, `ChronicleClient.rename`, `ChronicleTransfer.exportControls` and `textField`. From the shared protocol, `oathdigital.protocol.projection.ChronicleProjection(name, startingDigest, evolved)`.
- Produces:
  - `final class TableChronicle(mount: dom.Element, shelf: Option[ChronicleShelf], client: ChronicleClient, files: ChronicleFiles, repaint: () => Unit) { def section(chronicle: Option[ChronicleProjection]): Option[dom.Element] }`
  - `TableScreen(shell, view, controls, session, chronicle: Option[TableChronicle] = None)`
  - `ServerModeUi.start(mount, client, trustedGameId, startOver, transport: JsonTransport = new SameOriginJsonTransport, devices: ChronicleDevices = ChronicleDevices.browser)`
  - CSS classes: `table-chronicle`, `table-chronicle-name`, `table-chronicle-note`, `chronicle-save-prompt`, `chronicle-save-panel`, `chronicle-save-name`, `chronicle-save-new`, `chronicle-overwrite`, `chronicle-save-status`

- [ ] **Step 1: Write the failing tests**

`TableChronicleSuite.scala`. Construct `new TableChronicle(browser.mount, Some(shelf), client, files, () => repaints += 1)` and call `section(Some(ChronicleProjection(...)))`. Tests:

```scala
  test("before the Chronicle Phase the section names the Chronicle and " +
      "offers no export"):
    // evolved None: text "Chronicle: Ashen Crown"; no export-chronicle-json; nothing stored

  test("no section without a Chronicle")  // section(None) == None

  test("an evolved Chronicle with no entry of its name is saved and loaded"):
    // shelf empty; evolved entry("Ashen Crown", "d-2"), starting "d-1"
    // stored digest d-2, loaded "Ashen Crown"; note "Saved in this browser as \"Ashen Crown\"."
    // export-chronicle-json present

  test("the starting Chronicle under that name is replaced silently")
    // shelf holds A d-1; starting d-1; evolved d-2: stored d-2; no chronicle-save-prompt

  test("the evolved Chronicle already saved changes nothing")
    // shelf holds A d-2: one entry, savedAt unchanged; note "Already saved in this browser."

  test("a different Chronicle under that name asks, and asks once"):
    // shelf holds A d-9; evolved d-2, starting d-1: one chronicle-save-prompt with
    // role dialog; chronicle-save-name value "Ashen Crown (2)"; calling section again
    // adds no second prompt; stored still d-9

  test("Overwrite replaces the entry and closes the dialog")
    // click chronicle-overwrite: stored d-2; no prompt; repaints == 1;
    // next section's note "Saved in this browser as \"Ashen Crown\"."

  test("Save under new name renames through the server and keeps both")
    // transport convertingTransport; click chronicle-save-new; settle:
    // names Vector("Ashen Crown", "Ashen Crown (2)"); loaded "Ashen Crown (2)"; prompt gone

  test("a taken new name is refused in the dialog without a request")

  test("a browser that keeps nothing says so and still offers export")
    // shelf None: note "Chronicles cannot be kept in this browser; export this one to keep it."
    // export-chronicle-json present

  test("a refused write shows the shelf's reason")
```

Add one wiring test to `ServerModeSessionSuite`. Open a trusted game whose projection carries an evolved Chronicle: use `encodedGame` with `.copy(chronicle = Some(ChronicleProjection("Ashen Crown", "d-1", Some(ChronicleFixtures.entry("Ashen Crown", "d-2")))))`, extending `encodedGame` with a `chronicle` parameter. Pass `devices = ChronicleFixtures.devices(storage = Some(storage))` to `Main.start`. Assert that `table-chronicle` shows in the action pane and that `storage.values("oath.chronicles")` holds digest `d-2`.

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.TableChronicleSuite oathdigital.frontend.ServerModeSessionSuite"`
Expected: compile errors.

- [ ] **Step 3: Implement `TableChronicle.scala`**

```scala
package oathdigital.frontend

import oathdigital.protocol.ChronicleEntry
import oathdigital.protocol.projection.ChronicleProjection
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport.{button, element, text, textField}

/** The table's Chronicle section (Chronicle browsing design, "Automatic
  * save"): the game's Chronicle name, and once the Chronicle Phase has
  * journaled the evolved Chronicle, its automatic save and its export.
  * Each evolved Chronicle is handled once per page, so a redraw neither
  * saves again nor asks again. The dialog that asks lives in `mount`,
  * outside the panes a redraw replaces, so what the player types survives
  * the poll. */
private[frontend] final class TableChronicle(mount: dom.Element,
    shelf: Option[ChronicleShelf], client: ChronicleClient,
    files: ChronicleFiles, repaint: () => Unit):
  private var handled = Set.empty[String]
  private var note = ""

  def section(chronicle: Option[ChronicleProjection]): Option[dom.Element] =
    chronicle.map { value =>
      value.evolved.filterNot(evolved => handled(evolved.digest))
        .foreach(evolved => save(evolved, value.startingDigest))
      val node = element("section", "table-chronicle")
      node.setAttribute("aria-label", "Chronicle")
      node.appendChild(text("p", "table-chronicle-name",
        s"Chronicle: ${value.name}"))
      value.evolved.foreach { evolved =>
        if note.nonEmpty then
          node.appendChild(text("p", "table-chronicle-note", note))
        node.appendChild(ChronicleTransfer.exportControls(Some(evolved), client,
          files))
      }
      node
    }

  private def save(evolved: ChronicleEntry, startingDigest: String): Unit =
    handled += evolved.digest
    note = shelf match
      case None => "Chronicles cannot be kept in this browser; export this " +
        "one to keep it."
      case Some(kept) => kept.autoSave(evolved, startingDigest) match
        case AutoSave.Saved(saved) => savedAs(saved)
        case AutoSave.Unchanged => "Already saved in this browser."
        case AutoSave.Refused(reason) => reason
        case AutoSave.Conflict(existing) =>
          ask(kept, evolved, existing)
          ""

  private def savedAs(saved: SavedChronicle): String =
    s"Saved in this browser as \"${saved.name}\"."

  private def ask(kept: ChronicleShelf, evolved: ChronicleEntry,
      existing: SavedChronicle): Unit =
    val overlay = element("div", "chronicle-save-prompt")
    overlay.setAttribute("role", "dialog")
    overlay.setAttribute("aria-modal", "true")
    overlay.setAttribute("aria-labelledby", "chronicle-save-heading")
    val panel = element("div", "chronicle-save-panel")
    val heading = text("h2", "", "Save the Chronicle")
    heading.id = "chronicle-save-heading"
    panel.appendChild(heading)
    panel.appendChild(text("p", "", "This browser already has a different " +
      s"Chronicle named \"${existing.name}\". Save this game's Chronicle " +
      "under a new name, or overwrite the saved one."))
    val field = textField("chronicle-save-name", "New Chronicle name",
      kept.uniqueName(existing.name))
    val status = text("p", "status chronicle-save-status", "")
    status.setAttribute("role", "status")
    val saveNew = button("Save under new name", "chronicle-save-new")
    val overwrite = button("Overwrite", "chronicle-overwrite")

    def finish(result: Either[String, SavedChronicle]): Unit = result match
      case Left(reason) => status.textContent = reason
      case Right(saved) =>
        note = savedAs(saved)
        mount.removeChild(overlay)
        repaint()

    overwrite.onclick = _ => finish(kept.put(evolved))
    saveNew.onclick = _ => kept.nameProblem(field.value) match
      case Some(problem) => status.textContent = problem
      case None =>
        status.textContent = "Saving…"
        client.rename(evolved, field.value).foreach(result =>
          finish(result.flatMap(renamed => kept.put(renamed))))
    Vector(field, saveNew, overwrite, status).foreach(panel.appendChild)
    overlay.appendChild(panel)
    mount.appendChild(overlay)
    field.focus()
```

- [ ] **Step 4: Wire it in**

`TableScreen`: add the constructor parameter `chronicle: Option[TableChronicle] = None`. In `render`, right after the `seat-identity` paragraph block, add:

```scala
    chronicle.flatMap(_.section(projection.flatMap(_.chronicle)))
      .foreach(actionContent.appendChild)
```

`ServerModeUi.start`: add the parameters `transport: JsonTransport = new SameOriginJsonTransport` and `devices: ChronicleDevices = ChronicleDevices.browser` after `startOver`. Build the section after `shell`, which clears `mount`:

```scala
    lazy val chronicle = new TableChronicle(mount,
      devices.storage.map(new ChronicleShelf(_, devices.now)),
      new ChronicleClient(transport), devices.files, () => screen.render())
    lazy val screen: TableScreen = new TableScreen(shell, session, session,
      session, Some(chronicle))
```

`Main.start`: pass `transport = transport, devices = devices` to both `ServerModeUi.start` calls.

- [ ] **Step 5: Add the styles**

```css
/* The table's Chronicle section and its save dialog. */
.game-table .table-chronicle { margin: 0 0 12px; display: grid; gap: 6px; }
.game-table .table-chronicle p { margin: 0; }
.table-chronicle-note { color: var(--brass); }
.chronicle-save-prompt { position: fixed; inset: 0; z-index: 60; display: grid;
  place-items: center; padding: 16px; background: #000a; }
.chronicle-save-panel { width: min(30rem, 100%); display: grid; gap: 0.75rem;
  padding: 18px; background: #211d17; border: 1px solid var(--brass);
  border-radius: 12px; }
.chronicle-save-name { font: inherit; padding: 0.5rem; }
```

- [ ] **Step 6: Run the frontend suites**

Run: `./sbtw frontend/test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/main/scala/oathdigital/frontend/TableChronicle.scala frontend/src/main/scala/oathdigital/frontend/TableScreen.scala frontend/src/main/scala/oathdigital/frontend/ServerModeUi.scala frontend/src/main/scala/oathdigital/frontend/Main.scala frontend/src/test/scala/oathdigital/frontend/TableChronicleSuite.scala frontend/src/test/scala/oathdigital/frontend/ServerModeSessionSuite.scala frontend/styles.css
git commit -m "feat(chronicle): save the evolved Chronicle at the table and export it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Check in the real app, gates, and docs

**Files:**
- Modify: `docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/rules/implementation-traceability.md`

- [ ] **Step 1: Run the app and walk the pages**

Follow the memory note: do not use `preview_start`, because it runs against the live database. Start a scratch-database development server on port 8081 through Bash, then open `http://localhost:8081/` in the built-in browser. Check each of the following:
- The empty page offers New, a withheld Edit with its hover note, and Import.
- New Chronicle goes to `/new-game` with a dated name, Back returns to `/`, and a reload on `/new-game` lands on `/`.
- Creating a game opens it. Going back to `/` shows the new Chronicle loaded, with "Game 1".
- Rename, Delete with its confirmation, Download JSON, and Copy TTS string all work.
- Importing the downloaded file raises the name clash. Both the new-name and the replace path work.
- The layout holds at 375 px width.

Fix anything broken in the task that owns it, then rerun its suite. Taking the table to the Chronicle Phase by hand is out of reach here. `TableChronicleSuite` and the wiring test cover the automatic save.

- [ ] **Step 2: Run every gate**

```bash
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
./sbtw clean coverage test coverageReport
```

Expected: all pass, and statement coverage is at least 92%. Check that no file is over 800 lines:

```bash
wc -l frontend/src/main/scala/oathdigital/frontend/*.scala | sort -n | tail -5
```

- [ ] **Step 3: Record the slice as landed**

- Spec status line:

```markdown
Status: implemented, 2026-10-29. Slice 1 is the server side; slice 2 the
browser pages, storage, import and export, and the automatic save.
```

- ROADMAP:
  - Tick **Saved-campaign browsing**. Replace its "Slice 1 landed…" sub-bullet with: `Landed in two slices: the server side, then the Chronicle page, player selection, browser storage, JSON and TTS import and export, and the automatic save at the table. Starting game 2 from an evolved Chronicle waits for the Empire phase.`
  - Tick **Chronicle import and export**. Change its sub-bullet's last sentence to `Import and export in the UI landed with Saved-campaign browsing.`
- In `implementation-traceability.md`, find the "Full campaign serialization and migration" row. Append this sentence to its evidence cell: `Browsers keep Chronicles as canonical JSON under oath.chronicles and save the evolved Chronicle when the game's projection carries it (frontend ChronicleShelfSuite, TableChronicleSuite).` Leave the status as **Partial**: starting game 2 is still missing.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md docs/ROADMAP.md docs/rules/implementation-traceability.md
git commit -m "docs(chronicle): record saved-campaign browsing as landed

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Known gaps carried forward

- Game start seats every lineage as an Exile and ignores a saved Chronicle's roles. This slice does not reach that path, because game start refuses an evolved Chronicle until the Empire phase. It must be fixed before the Empire phase lets game 2 start. Fixed on 2026-10-29: game start seats each lineage from its record and refuses what later-game setup must handle.
- A player who never reopens the game after its Chronicle Phase gets no automatic save, as the spec's "Known limits" says.
