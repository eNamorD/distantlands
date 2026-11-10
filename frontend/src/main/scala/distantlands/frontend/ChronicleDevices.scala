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
      // Some browsers cancel a download whose URL is revoked at once.
      dom.window.setTimeout(() => dom.URL.revokeObjectURL(url), 10000)

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
