package distantlands.frontend

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
