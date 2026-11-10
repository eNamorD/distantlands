package distantlands.frontend

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
    * some private windows have a `localStorage` that refuses every write.
    * A browser that already holds Chronicles keeps its store even when the
    * probe fails, as a full storage's does: they stay listed and loadable,
    * and only writes are refused. */
  val Probe = "oath.chronicles.probe"

  /** The browser's `localStorage`, or nothing when the browser refuses it. */
  def browser: Option[ChronicleStorage] =
    Try(dom.window.localStorage).toOption
      .filter(storage => !js.isUndefined(storage) && storage != null)
      .filter(storage => Try {
        storage.setItem(Probe, Probe)
        storage.removeItem(Probe)
      }.isSuccess || Try(storage.getItem(ChronicleShelf.ListKey) != null)
        .getOrElse(false))
      .map(storage =>
        new ChronicleStorage:
          def read(key: String): Option[String] =
            Try(Option(storage.getItem(key))).toOption.flatten
          def write(key: String, value: String): Boolean =
            Try(storage.setItem(key, value)).isSuccess
          def remove(key: String): Unit =
            Try(storage.removeItem(key)).getOrElse(()))
