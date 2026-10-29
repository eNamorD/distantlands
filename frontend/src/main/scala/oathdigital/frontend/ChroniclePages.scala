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
    // The table's stylesheet fixes the page to the viewport; the start pages
    // are long, so they scroll inside it.
    mount.classList.add("start-page")

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
