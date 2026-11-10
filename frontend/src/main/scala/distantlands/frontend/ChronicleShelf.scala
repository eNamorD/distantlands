package distantlands.frontend

import distantlands.protocol.{ChronicleEntry, ChronicleEntryCodec}
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

  /** `base (2)`, `base (3)`, …: the first that is free, cut to fit. */
  def uniqueName(base: String): String =
    Iterator.from(2).map { number =>
      val suffix = s" ($number)"
      base.trim.take(MaxNameLength - suffix.length).trim + suffix
    }.find(find(_).isEmpty).get

  /** Saves `entry` and loads it. An entry with its name, or with the name
    * `replacing`, is replaced where it stands; otherwise it is added last.
    * Callers that must not overwrite check `nameProblem` first. */
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

  /** Removes the row at `index` if it still holds the entry named `shown`,
    * or an unreadable one when `shown` is empty: another page of this
    * browser may have changed the list since this one was drawn. */
  def delete(index: Int, shown: Option[String]): Unit =
    val current = items
    val gone = current.lift(index).map(decode(_).map(_.name))
    if gone.contains(shown) then
      store(current.patch(index, Nil, 1))
      if shown.exists(name => storage.read(LoadedKey).contains(name)) then
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
  val ListKey = "distantlands.chronicles"
  val LoadedKey = "distantlands.chronicles.loaded"
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
    case value: ujson.Obj =>
      for
        entry <- ChronicleEntryCodec.decodeFields(value, "$").toOption
        name <- value.value.get("name").collect { case ujson.Str(text) => text }
        savedAt <- value.value.get("savedAt")
          .collect { case ujson.Str(text) => text }
        if name == entry.summary.name
      yield SavedChronicle(entry, savedAt)
    case _ => None
