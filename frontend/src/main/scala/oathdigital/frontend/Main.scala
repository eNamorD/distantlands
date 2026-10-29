package oathdigital.frontend

import org.scalajs.dom

object Main:
  /** The development start page; `ServerModeUi` returns here to create another game. */
  private[frontend] val DevelopmentStartUrl = "/?mode=server"

  def main(args: Array[String]): Unit =
    val mount = Option(dom.document.getElementById("app")).getOrElse:
      throw new IllegalStateException("missing #app mount point")
    HoverTip.install()
    start(mount, dom.window.location.pathname,
      mount.getAttribute("data-runtime-mode") == "trusted-alpha",
      new SameOriginJsonTransport)

  private[frontend] def start(mount: dom.Element, pathname: String,
      trustedAlpha: Boolean, transport: JsonTransport,
      navigate: String => Unit = url => dom.window.location.assign(url),
      devices: ChronicleDevices = ChronicleDevices.browser): Unit =
    val startPage = pathname == "/" || pathname == ChroniclePages.NewGamePath
    ServerUiSupport.canonicalGameId(pathname) match
      case Some(gameId) =>
        ServerModeUi.start(mount, new TrustedHttpGameClient(transport), Some(gameId),
          transport = transport, devices = devices)
      case None if trustedAlpha && startPage =>
        ChroniclePages.start(mount, pathname, transport, devices)
      case None if trustedAlpha => mount.textContent = "Open your assigned seat link."
      case None if ServerUiSupport.queryParameter("gameId").isEmpty =>
        // Development games are created exactly like trusted ones, then opened
        // in the development table as the first listed player.
        ChroniclePages.start(mount, pathname, transport, devices, Some(created =>
          navigate(ServerUiSupport.sessionUrl(created.gameId, created.seats.head.playerId))))
      case None => ServerModeUi.start(mount, new HttpGameClient(transport),
        startOver = () => navigate(DevelopmentStartUrl), transport = transport,
        devices = devices)
