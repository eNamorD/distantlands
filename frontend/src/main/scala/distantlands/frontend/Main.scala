package distantlands.frontend

import org.scalajs.dom

object Main:
  /** The development start page; `ServerModeUi` returns here to create another game. */
  private[frontend] val DevelopmentStartUrl = "/?mode=server"

  def main(args: Array[String]): Unit =
    val mount = Option(dom.document.getElementById("app")).getOrElse:
      throw new IllegalStateException("missing #app mount point")
    HoverTip.install()
    val transport = new SameOriginJsonTransport
    // The manifest is fetched once per page load and nothing waits for
    // it: the table draws its own faces until it arrives.
    GameArt.start(transport, ChronicleStorage.browser)
    start(mount, dom.window.location.pathname,
      mount.getAttribute("data-runtime-mode") == "trusted-alpha",
      transport, search = dom.window.location.search)

  private[frontend] def start(mount: dom.Element, pathname: String,
      trustedAlpha: Boolean, transport: JsonTransport,
      navigate: String => Unit = url => dom.window.location.assign(url),
      devices: ChronicleDevices = ChronicleDevices.browser,
      search: String = ""): Unit =
    val startPage = pathname == "/" || pathname == ChroniclePages.NewGamePath
    ServerUiSupport.seatPage(pathname) match
      case Some(seat) =>
        ServerModeUi.start(mount, new TrustedHttpGameClient(transport, seat.playerId),
          Some(seat.gameId),
          transport = transport, devices = devices, nextGame = navigate)
      case None if trustedAlpha && startPage =>
        ChroniclePages.start(mount, pathname + search, transport, devices)
      case None if trustedAlpha => mount.textContent = "Open your assigned seat link."
      case None if ServerUiSupport.queryParameter("gameId").isEmpty =>
        // Development games are created exactly like trusted ones, then opened
        // in the development table as the first listed player.
        ChroniclePages.start(mount, pathname + search, transport, devices, Some(created =>
          navigate(ServerUiSupport.sessionUrl(created.gameId, created.seats.head.playerId))))
      case None => ServerModeUi.start(mount, new HttpGameClient(transport),
        startOver = () => navigate(DevelopmentStartUrl), transport = transport,
        devices = devices, nextGame = navigate)
