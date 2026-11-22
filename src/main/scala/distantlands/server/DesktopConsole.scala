package distantlands.server

import java.net.BindException
import java.nio.file.Path

import scala.util.control.NonFatal

/** What the desktop profile shows and opens after the server binds. */
object DesktopConsole:
  def banner(config: ServerConfig, launch: DesktopLaunch): String =
    val address = config.publicBaseUrl match
      case Some(url) => s"  Players open:  $url"
      case None => s"  Only this computer can connect: http://127.0.0.1:${config.port}"
    val lines =
      Vector(
        s"Distant Lands ${config.version} is running.",
        address
      ) ++
        thisComputerUrl(config)
          .map(url => s"  On this computer you can also use: ${url.stripSuffix("/")}") ++
      Vector(
        s"  Data folder:   ${config.databasePath.getParent}",
        s"  Settings:      ${launch.settingsFile}"
      ) ++
        (if launch.lanDetectionFailed then
           Vector("No local network address found. Set DLQ_PUBLIC_BASE_URL in the settings file.")
         else Vector.empty) ++
        (if config.publicBaseUrl.nonEmpty then
           Vector("Seat links contain this address. If it changes, players need the new address.")
         else Vector.empty) :+
        "Close this window or press Ctrl-C to stop."
    lines.mkString("", System.lineSeparator, System.lineSeparator)

  /** A loopback address for the host's own browser, when the public address
    * is a plain-HTTP, non-loopback one and the server also listens on
    * loopback. Many home routers cannot loop a connection back to their own
    * public address, so the host uses this one instead. An HTTPS public
    * address goes through a proxy that loopback would skip. */
  def thisComputerUrl(config: ServerConfig): Option[String] =
    val listensOnLoopback =
      WildcardHosts.contains(config.host) || ServerConfig.isLoopback(unbracketed(config.host))
    config.publicBaseUrl
      .filter(url => Option(url.getScheme).exists(_.equalsIgnoreCase("http")))
      .filterNot(url => ServerConfig.isLoopback(unbracketed(url.getHost)))
      .filter(_ => listensOnLoopback)
      .map(_ => s"http://localhost:${config.port}/")

  def browserUrl(config: ServerConfig): String =
    thisComputerUrl(config)
      .orElse(config.publicBaseUrl.map(_.toString.stripSuffix("/") + "/"))
      .getOrElse(s"http://127.0.0.1:${config.port}/")

  private val WildcardHosts = Set("0.0.0.0", "::", "[::]")

  private def unbracketed(host: String): String =
    host.stripPrefix("[").stripSuffix("]")

  def browserCommand(osName: String, url: String): Vector[String] =
    val os = osName.toLowerCase
    if os.startsWith("mac") then Vector("open", url)
    else if os.startsWith("windows") then Vector("cmd", "/c", "start", "", url)
    else Vector("xdg-open", url)

  def openBrowser(
      osName: String,
      url: String,
      run: Vector[String] => Unit
  ): Option[String] =
    try
      run(browserCommand(osName, url))
      None
    catch
      case NonFatal(error) => Some(s"could not open a browser: ${error.getMessage}")

  def bindFailure(
      error: Throwable,
      port: Int,
      settingsFile: Path
  ): Option[String] =
    Iterator.iterate(error)(_.getCause).takeWhile(_ != null).take(20)
      .collectFirst:
        case cause if cause.isInstanceOf[BindException] ||
            cause.getClass.getSimpleName == "BindFailedException" =>
          s"port $port is in use. Is Distant Lands already running? " +
            s"Change DLQ_PORT in $settingsFile."
