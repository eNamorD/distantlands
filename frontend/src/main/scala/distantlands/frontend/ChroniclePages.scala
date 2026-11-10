package distantlands.frontend

import distantlands.protocol.TrustedGameCreateResponse
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
    var creating: () => Boolean = () => false
    // The table's stylesheet fixes the page to the viewport; the start pages
    // are long, so they scroll inside it.
    mount.classList.add("start-page")

    def show(path: String): Unit =
      if path == NewGamePath then choice match
        // The browser's Forward can return here after the loaded Chronicle
        // was renamed or deleted; it is then chosen again from `/`.
        case Some(HostChronicle.Continue(saved))
            if !shelf.loaded.contains(saved) =>
          choice = None
          devices.history.replace("/")
          show("/")
        case Some(chosen) => creating = TrustedHostUi.start(mount, transport,
          chosen, shelf, () => { devices.history.push("/"); show("/") },
          openCreated)
        case None =>
          devices.history.replace("/")
          show("/")
      else
        creating = () => false
        ChroniclePage.render(mount, shelf, kept, client, devices.files,
          chosen => {
            choice = Some(chosen)
            devices.history.push(NewGamePath)
            show(NewGamePath)
          })

    // The browser's Back or Forward while a game is being created would
    // lose its seat links: player selection stays, at its own path.
    devices.history.onPop(path =>
      if creating() then devices.history.push(NewGamePath) else show(path))
    show(pathname)
