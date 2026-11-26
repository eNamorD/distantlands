package distantlands.frontend

import distantlands.protocol.art.ArtCatalog
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** `GameArt` joins the server's manifest with the player's preference
  * (game art design, "The client"). */
class GameArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = ArtCatalog.cardFace("Alchemist").get
  private val relicBack = ArtCatalog.cardBack("relic").get

  /** A transport that answers the manifest request with `answer` and
    * records every request. */
  private def answering(answer: Either[GameClientFailure, TransportResponse])
      : ScriptedTransport =
    new ScriptedTransport((_, _, _) => Future.successful(answer))

  test("before the manifest arrives nothing has art"):
    assertEquals(GameArt.manifest, GameArt.Manifest.Pending)
    assertEquals(GameArt.url(alchemist), None)

  test("a listed key's URL is its path under /art, each segment encoded"):
    TestArt.listing(alchemist.path, relicBack.path)
    assertEquals(GameArt.url(alchemist),
      Some("/art/cards/Alchemist%20(NF).png"))
    assertEquals(GameArt.url(relicBack), Some("/art/tts/backs/relic-back.jpg"))

  test("a key the manifest lacks has no URL"):
    TestArt.listing(relicBack.path)
    assertEquals(GameArt.url(alchemist), None)

  test("with the preference off nothing has a URL"):
    TestArt.listing(alchemist.path)
    GameArt.setEnabled(false)
    assertEquals(GameArt.url(alchemist), None)
    GameArt.setEnabled(true)
    assert(GameArt.url(alchemist).nonEmpty)

  test("the preference is on by default and is saved under " +
      "distantlands.art"):
    val store = TestArt.listing(alchemist.path)
    assert(GameArt.enabled)
    GameArt.setEnabled(false)
    assertEquals(store.values.get("distantlands.art"), Some("off"))
    GameArt.setEnabled(true)
    assertEquals(store.values.get("distantlands.art"), Some("on"))

  test("a stored off is read when the page loads"):
    GameArt.install(GameArt.Manifest.Listed(Set(alchemist.path)),
      new MemoryChronicleStorage(Map("distantlands.art" -> "off")))
    assert(!GameArt.enabled)
    assertEquals(GameArt.url(alchemist), None)

  test("start fetches the manifest once and lists its files"):
    val transport = answering(Right(TransportResponse(200,
      """{"files":["cards/Alchemist (NF).png"]}""")))
    GameArt.start(transport, Some(new MemoryChronicleStorage())).map { _ =>
      assertEquals(transport.requests.toVector,
        Vector(RecordedRequest("GET", "/art/manifest.json", None)))
      assertEquals(GameArt.manifest,
        GameArt.Manifest.Listed(Set("cards/Alchemist (NF).png")))
      assert(GameArt.url(alchemist).nonEmpty)
    }

  test("a failed fetch, a refusal and an unreadable answer each count as " +
      "a failed manifest"):
    val answers = Vector[Either[GameClientFailure, TransportResponse]](
      Left(GameClientFailure.NetworkFailure("GET /art/manifest.json failed")),
      Right(TransportResponse(404, "")),
      Right(TransportResponse(200, "<html>")),
      Right(TransportResponse(200, """{"files":"all"}""")))
    answers.foldLeft(Future.successful(())) { (done, answer) =>
      done.flatMap(_ => GameArt.start(answering(answer), None)).map { _ =>
        assertEquals(GameArt.manifest, GameArt.Manifest.Failed)
        assertEquals(GameArt.url(alchemist), None)
      }
    }

  test("a browser that refuses storage gets the default, and the switch " +
      "still works for the life of the page"):
    GameArt.start(answering(Right(TransportResponse(200,
      """{"files":["cards/Alchemist (NF).png"]}"""))), None).map { _ =>
      assert(GameArt.enabled)
      GameArt.setEnabled(false)
      assert(!GameArt.enabled)
      assertEquals(GameArt.url(alchemist), None)
    }

  test("a store that refuses the write still switches for the page"):
    val refusing = new ChronicleStorage:
      def read(key: String): Option[String] = None
      def write(key: String, value: String): Boolean = false
      def remove(key: String): Unit = ()
    GameArt.install(GameArt.Manifest.Listed(Set(alchemist.path)), refusing)
    GameArt.setEnabled(false)
    assert(!GameArt.enabled)

  test("the listener hears the manifest arrive and each change of the " +
      "preference"):
    var heard = 0
    GameArt.onChange(() => heard += 1)
    GameArt.start(answering(Right(TransportResponse(200, """{"files":[]}"""))),
      None).map { _ =>
      assertEquals(heard, 1)
      GameArt.setEnabled(false)
      assertEquals(heard, 2)
    }

  test("loaded and failed URLs are remembered until reset"):
    GameArt.markLoaded("/art/a.png")
    GameArt.markFailed("/art/b.png")
    assert(GameArt.hasLoaded("/art/a.png") && !GameArt.hasFailed("/art/a.png"))
    assert(GameArt.hasFailed("/art/b.png") && !GameArt.hasLoaded("/art/b.png"))
    GameArt.reset()
    assert(!GameArt.hasLoaded("/art/a.png") && !GameArt.hasFailed("/art/b.png"))
