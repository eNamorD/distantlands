package distantlands.frontend

import distantlands.protocol.TrustedGameCreateResponse
import org.scalajs.dom
import scala.scalajs.js
import scala.util.Try

/** The two start pages (Chronicle browsing design, "Browser"): the
  * Chronicle page at `/` and player selection at `/new-game`. Continuing a
  * Chronicle is named in the address, `/new-game?chronicle=<name>`, so a
  * reload, Back and Forward keep it, and the table's Start Next Game can link
  * to it. A New Chronicle has no name yet and lives only in this page's
  * memory, so `/new-game` without a name, as after a reload, returns to `/`.
  * `location` is a path with its query, as `PageHistory` reports it. */
private[frontend] object ChroniclePages:
  val NewGamePath = "/new-game"

  /** Player selection for continuing the Chronicle saved as `name`. */
  def continueUrl(name: String): String =
    s"$NewGamePath?chronicle=${js.URIUtils.encodeURIComponent(name)}"

  /** The Chronicle named in a `/new-game` location's query, if any. */
  private def continued(location: String): Option[String] =
    location.split("\\?", 2).lift(1).toVector.flatMap(_.split("&"))
      .flatMap { pair =>
        pair.split("=", 2).toVector match
          case Vector("chronicle", value) =>
            Try(js.URIUtils.decodeURIComponent(value)).toOption
          case _ => None
      }.headOption.filter(_.nonEmpty)

  def start(mount: dom.Element, location: String, transport: JsonTransport,
      devices: ChronicleDevices,
      openCreated: Option[TrustedGameCreateResponse => Unit] = None): Unit =
    val kept = devices.storage.nonEmpty
    val shelf = new ChronicleShelf(
      devices.storage.getOrElse(new MemoryChronicleStorage()), devices.now)
    val client = new ChronicleClient(transport)
    var startedNew = false
    var here = NewGamePath
    var creating: () => Boolean = () => false
    // The table's stylesheet fixes the page to the viewport; the start pages
    // are long, so they scroll inside it.
    mount.classList.add("start-page")

    def create(chosen: HostChronicle): Unit =
      creating = TrustedHostUi.start(mount, transport, chosen, shelf,
        () => { devices.history.push("/"); show("/") }, openCreated)

    def toLobby(): Unit =
      startedNew = false
      devices.history.replace("/")
      show("/")

    def show(where: String): Unit =
      if where.takeWhile(_ != '?') == NewGamePath then
        here = where
        continued(where) match
          // The Chronicle may have been renamed or deleted since the address
          // was made; it is then chosen again from `/`.
          case Some(name) => shelf.find(name) match
            case Some(saved) => create(HostChronicle.Continue(saved))
            case None => toLobby()
          case None if startedNew => create(HostChronicle.New)
          case None => toLobby()
      else
        creating = () => false
        ChroniclePage.render(mount, shelf, kept, client, devices.files,
          chosen => {
            val target = chosen match
              case HostChronicle.New =>
                startedNew = true
                NewGamePath
              case HostChronicle.Continue(saved) =>
                startedNew = false
                continueUrl(saved.name)
            devices.history.push(target)
            show(target)
          })

    // The browser's Back or Forward while a game is being created would
    // lose its seat links: player selection stays, at its own path.
    devices.history.onPop(path =>
      if creating() then devices.history.push(here) else show(path))
    show(location)
