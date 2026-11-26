package distantlands.frontend

import distantlands.protocol.art.ArtKey
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.Try

/** Whether a piece of the table has licensed art to show (game art design,
  * "The client"): the server's manifest joined with the player's
  * preference. Renderers ask one question, `url`, and draw what they draw
  * today when the answer is `None`.
  *
  * One object, as `CardInspection` is: cards reach `CardFace.render`
  * through renderers that carry no session. Nothing waits for the
  * manifest; the table is drawn at once and `onChange`'s listener redraws
  * it when the manifest arrives or the preference flips.
  */
private[frontend] object GameArt:
  /** What the server said it can serve. */
  sealed trait Manifest extends Product with Serializable
  object Manifest:
    /** Not answered yet. */
    case object Pending extends Manifest
    /** The fetch failed; it is not tried again for this page load. */
    case object Failed extends Manifest
    /** The paths the server serves; empty when it has no art folder. */
    final case class Listed(files: Set[String]) extends Manifest

  /** The preference's `localStorage` key; "off" turns art off, and
    * anything else, or nothing, leaves it on. */
  val PreferenceKey = "distantlands.art"
  val ManifestUrl = "/art/manifest.json"

  private var current: Manifest = Manifest.Pending
  private var storage: ChronicleStorage = new MemoryChronicleStorage()
  /** The switch's position once flipped on this page, which holds even
    * when the browser refuses to store it. */
  private var chosen = Option.empty[Boolean]
  private var listener: () => Unit = () => ()
  private var loaded = Set.empty[String]
  private var failed = Set.empty[String]

  def manifest: Manifest = current

  /** Reads the preference from `browser`, or keeps it for the page alone
    * when the browser stores nothing, and fetches the manifest once. */
  def start(transport: JsonTransport, browser: Option[ChronicleStorage])
      : Future[Unit] =
    storage = browser.getOrElse(new MemoryChronicleStorage())
    chosen = None
    transport.request("GET", ManifestUrl, None).map { answer =>
      current = answer.toOption
        .filter(response => response.status >= 200 && response.status < 300)
        .flatMap(response => Try(ujson.read(response.body)("files").arr
          .map(_.str).toSet).toOption)
        .fold[Manifest](Manifest.Failed)(Manifest.Listed(_))
      listener()
    }

  /** Puts a manifest and a store in place without a fetch. */
  def install(manifest: Manifest, store: ChronicleStorage): Unit =
    current = manifest
    storage = store
    chosen = None
    listener()

  /** `listener` runs when the manifest arrives and when the preference
    * changes: the table redraws. */
  def onChange(listener: () => Unit): Unit = this.listener = listener

  def enabled: Boolean =
    chosen.getOrElse(!storage.read(PreferenceKey).contains("off"))

  def setEnabled(on: Boolean): Unit =
    chosen = Some(on)
    storage.write(PreferenceKey, if on then "on" else "off")
    listener()

  /** Where `key`'s image is served, when the preference is on and the
    * manifest lists its path. */
  def url(key: ArtKey): Option[String] = current match
    case Manifest.Listed(files) if enabled && files(key.path) =>
      Some("/art/" + key.path.split('/')
        .map(segment => js.URIUtils.encodeURIComponent(segment))
        .mkString("/"))
    case _ => None

  /** The browser holds `url`'s image: a fresh box may show it at once. */
  def markLoaded(url: String): Unit = loaded += url
  def hasLoaded(url: String): Boolean = loaded(url)
  /** `url` failed: it is not asked for again until the page reloads. */
  def markFailed(url: String): Unit = failed += url
  def hasFailed(url: String): Boolean = failed(url)

  /** Back to a page that has asked for nothing, for the suites. */
  def reset(): Unit =
    current = Manifest.Pending
    storage = new MemoryChronicleStorage()
    chosen = None
    listener = () => ()
    loaded = Set.empty
    failed = Set.empty
