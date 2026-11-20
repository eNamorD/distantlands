package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}
import distantlands.frontend.TestBoards.quietBoard
import distantlands.frontend.TestSites.quietSite
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent}
import distantlands.testkit.Unwrap.present
import org.scalajs.dom

/** The Darkest Secret's Festival is a waivable power its holder rules
  * (ruling 16), and a consent prompt sends them to it: the banner, and the
  * festival marker the face places, open the holder's consent preferences.
  * Red holds the banner, Festival face up, and the marker lies at Deep
  * Woods. */
class BannerConsentGridSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val festival = BannerState("darkest-secret", "festival",
    Some("red"), 2)
  private val festivalPower = "banner.darkest-secret.festival"
  private val tableWithRedHoldingTheFestival =
    TestProjections.actingProjection(players = Vector(red, blue),
      world = Vector(GameRegion("provinces",
        Vector(quietSite("site:woods", "Deep Woods").copy(festival = true)))))
      .copy(playerBoards = Vector(quietBoard("red").copy(
        banners = Vector(festival)), quietBoard("blue")))
  private val redRulingTheFestival = tableWithRedHoldingTheFestival.copy(
    consentPreferences = Vector(ConsentPreferencesState(festivalPower,
      "Darkest Secret", Vector(ConsentPreferenceRowState("blue", "never")),
      bannerKey = Some("darkest-secret"))))

  private var opened = Vector.empty[CardInspection.Request]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen(request => opened :+= request)

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private def festivalGrid(using munit.Location): Option[ConsentGrid] =
    opened match
      case Vector(request: CardInspection.Request.Festival) => request.grid
      case other => fail(s"expected one Festival request, got $other")

  private def markerGrid(using munit.Location): Option[ConsentGrid] =
    opened match
      case Vector(request: CardInspection.Request.FestivalMarker) =>
        request.grid
      case other => fail(s"expected one marker request, got $other")

  test("the holder's banner opens the Festival with their grid, and a " +
      "choice sends the preference"):
    var sent = Vector.empty[Intent]
    val boards = WorldBoardRenderer.playerBoards(redRulingTheFestival, "red",
      BoardOptions.none, Some(command => sent :+= command))
    click(one(boards, ".banner-face"))
    val grid = present(festivalGrid, "the Festival grid")
    assertEquals(grid.rows, Vector(ConsentGridRow("blue", "Blue", "never")))
    grid.choose.foreach(_("blue", "always"))
    assertEquals(sent, Vector(Intent.SetConsentPreference(festivalPower,
      "blue", "always")))

  test("a viewer who rules no Festival reads the banner with no grid"):
    click(one(WorldBoardRenderer.playerBoards(tableWithRedHoldingTheFestival,
      "red"), ".banner-face"))
    assertEquals(festivalGrid, None)

  test("the festival marker on the map opens the holder's grid too"):
    click(one(WorldBoardRenderer.world(redRulingTheFestival,
      BoardOptions.none), ".festival-marker"))
    assertEquals(present(markerGrid, "the marker grid").rows,
      Vector(ConsentGridRow("blue", "Blue", "never")))

  test("the overlay draws the grid below the Festival's rules and below " +
      "the printed marker"):
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val overlay = new CardInspectionOverlay(root)
    val opener = dom.document.createElement("button")
      .asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(opener)
    val grid = Some(ConsentGrid(
      Vector(ConsentGridRow("blue", "Blue", "never")), None))
    overlay.showFestival("Banner of the Darkest Secret · Festival",
      BannerFace.rules("festival"), Some("Deep Woods"), grid, opener)
    assertEquals(all(root, ".consent-grid-row").map(
      _.getAttribute("data-player-id")), Vector("blue"))
    overlay.showFestivalMarker(grid, opener)
    assertEquals(all(root, ".consent-grid-row").size, 1)
    assertEquals(all(root, ".festival-plate-rule").size, 1)
    overlay.dispose()
    root.remove()
    opener.remove()
