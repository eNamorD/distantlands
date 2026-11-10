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
