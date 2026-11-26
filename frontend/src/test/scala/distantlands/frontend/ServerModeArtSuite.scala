package distantlands.frontend

import Dom.all
import distantlands.frontend.ServerModeFixtures.withEmptyLogPage
import distantlands.model.PlayerColor
import distantlands.protocol.projection.GameProjectionCodec

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** A mounted table redraws when game art changes (game art design, "The
  * client"): the manifest arriving after the table is drawn, and the Game
  * art switch, each reach the table through the listener `ServerModeUi`
  * registers. */
class ServerModeArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** A game whose Public Ambitions row holds one face-up legacy, Iron
    * Hand, as the server sends it to blue's seat. */
  private val encodedGameWithPublicLegacy: String =
    GameProjectionCodec.encode(TestProjections.actingProjection(
      gameId = "my game", nextSequence = 1L, phase = "act",
      activePlayer = Some("red"), players = Vector(
        GamePlayer("red", "Red Exile", "exile", PlayerColor.Red),
        GamePlayer("blue", "Blue Exile", "exile", PlayerColor.Blue)),
      ready = true).copy(viewerPlayerId = Some("blue"),
      publicLegacies = Vector(CardDetails("L01", "legacy", "Iron Hand",
        suit = Some("order"), orientation = Some("face-up")))))

  test("the table redraws with art when the manifest arrives, and " +
      "without it when the preference is switched off"):
    TestBrowser.use(): browser =>
      val transport = ScriptedTransport((_, _, _) => Future.successful(
        Right(TransportResponse(200, encodedGameWithPublicLegacy))))
      Main.start(browser.mount, "/games/my%20game/seats/blue",
        trustedAlpha = true, withEmptyLogPage(transport),
        devices = ChronicleFixtures.devices(None))
      browser.settle.map { _ =>
        def legacyImages: Vector[String] = all(browser.mount,
          ".public-legacies .card-face > img.art").map(_.getAttribute("src"))
        assertEquals(all(browser.mount, ".public-legacies .card-face").size, 1)
        assertEquals(legacyImages, Vector.empty)
        // The manifest arrives after the table was drawn.
        TestArt.listingWholeCatalog()
        assertEquals(legacyImages,
          Vector("/art/cards/Iron%20Hand%20(NF).png"))
        GameArt.setEnabled(false)
        assertEquals(legacyImages, Vector.empty)
        GameArt.setEnabled(true)
        assertEquals(legacyImages.size, 1)
      }
