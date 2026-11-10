package distantlands.frontend

import distantlands.protocol.projection.{LogEntry, LogKind, LogSection,
  LogSpan, LogTakenBack}
import org.scalajs.dom
import scala.scalajs.js

/** Builders for the log lines and the content box the log suites draw. */
private[frontend] object TestLog:
  /** A log entry with every field named at the call. Its depth is its
    * kind's. */
  def entry(sequence: Long, kind: LogKind, spans: Vector[LogSpan],
      section: Option[LogSection] = None,
      takenBack: Option[LogTakenBack] = None): LogEntry =
    LogEntry(sequence = sequence, ordinal = 0, kind = kind, spans = spans,
      section = section, takenBack = takenBack)

  /** A log entry that reads `text` as one plain span. */
  def textEntry(sequence: Long, kind: LogKind, text: String,
      section: Option[LogSection] = None,
      takenBack: Option[LogTakenBack] = None): LogEntry =
    entry(sequence, kind, Vector(LogSpan.Text(text)), section, takenBack)

  /** A content box jsdom gives a size: `height` tall, `scrolled` from the
    * top, holding `total` pixels. `scrollTop` is a plain writable property
    * that jsdom does not clamp, so scrolling to the end sets it to `total`
    * (the box's `scrollHeight`), not to `total - height`.
    */
  def sizedBox(total: Int, height: Int, scrolled: Double): dom.html.Element =
    val content = dom.document.createElement("div")
      .asInstanceOf[dom.html.Element]
    size(content, total, height, scrolled)
    content

  /** Gives `content`, which jsdom lays out as nothing, the size `sizedBox`
    * gives its box. */
  def size(content: dom.Element, total: Int, height: Int,
      scrolled: Double): Unit =
    def define(name: String, value: js.Any, writable: Boolean) =
      js.Object.defineProperty(content, name, js.Dynamic.literal(value = value,
        writable = writable, configurable = true)
        .asInstanceOf[js.PropertyDescriptor])
    define("scrollHeight", total, writable = false)
    define("clientHeight", height, writable = false)
    define("scrollTop", scrolled, writable = true)

  /** Gives a box `size` made a new size, as a pane shown or hidden by the
    * stylesheet gets one: a hidden pane is 0 tall and holds 0 pixels. */
  def resize(content: dom.Element, total: Int, height: Int): Unit =
    Vector("scrollHeight" -> total, "clientHeight" -> height).foreach {
      case (name, value) => js.Object.defineProperty(content, name,
        js.Dynamic.literal(value = value, writable = false,
          configurable = true).asInstanceOf[js.PropertyDescriptor])
    }
