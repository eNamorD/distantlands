package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.{all, click, one}
import TestBoardOptions.{available, option, recording}
import TestProjections.actingProjection
import TestSites.quietSite
import org.scalajs.dom

/** The festival marker lying over a site's cards: a band across the middle
  * of its three card slots that prints the marker's title and opens the
  * marker as printed. */
class FestivalMarkerSuite extends munit.FunSuite:
  private val woods = quietSite("site:woods", "Deep Woods")
  private val peaks = quietSite("site:peaks", "Broken Peaks")

  /** How many times the inspector was asked to open the marker. */
  private var markersOpened = 0

  /** The marker site of every Festival inspection asked for. */
  private var festivalsOpened = Vector.empty[Option[String]]

  override def beforeEach(context: BeforeEach): Unit =
    markersOpened = 0
    festivalsOpened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.FestivalMarker(_) => markersOpened += 1
      case CardInspection.Request.Festival(_, _, at, _) =>
        festivalsOpened :+= at
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  /** The World pane of a table whose Provinces hold `sites`, with Red's
    * pawn at Deep Woods. */
  private def world(sites: GameSite*): dom.Element =
    worldWith(BoardOptions.none, sites*)

  /** `world`, drawn with `options` on the table. */
  private def worldWith(options: BoardOptions, sites: GameSite*)
      : dom.Element =
    WorldBoardRenderer.world(actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      world = Vector(GameRegion("provinces", sites.toVector)))
      .copy(pawnLocations = Vector(GamePawn("red", "site:woods"))),
      options)

  /** The festival markers lying over the cards of the site named `label`. */
  private def markersAt(root: dom.Element, label: String)
      : Vector[dom.Element] =
    all(root, s".site[aria-label='$label'] > .festival-marker")

  test("the site it lies over carries the marker over its cards, which " +
      "prints its title and holds its text in its note"):
    val root = world(woods.copy(festival = true), peaks)
    val marker = markersAt(root, "Deep Woods")
    assertEquals(marker.size, 1)
    assertEquals(markersAt(root, "Broken Peaks").size, 0)
    assertEquals(marker.map(_.textContent), Vector("Festival Underway"))
    assertEquals(marker.map(_.getAttribute("aria-label")),
      Vector("Festival marker"))
    assertEquals(marker.map(_.getAttribute("data-tip")),
      Vector(FestivalMarker.Rule))

  test("the marker leaves the pawn row to the pawns"):
    val root = world(woods.copy(festival = true))
    assertEquals(
      all(root, ".site[aria-label='Deep Woods'] .site-pawns > *")
        .map(_.getAttribute("class")),
      Vector("pawn"))

  test("with the marker beside the map no site draws it"):
    assertEquals(all(world(woods, peaks), ".festival-marker").size, 0)

  test("the marker is a button, so a keyboard reaches it, and pressing it " +
      "opens the marker as printed in the inspector"):
    val root = world(woods.copy(festival = true))
    val marker = one(root, ".festival-marker")
    assertEquals(marker.tagName, "BUTTON")
    click(marker)
    assertEquals(markersOpened, 1)

  test("pressing the marker on a site that is an option leaves the site " +
      "unpicked"):
    val woodsSite = option("site", "site:woods")
    val shown = recording(available(woodsSite))
    val root = worldWith(shown.options, woods.copy(festival = true))
    click(one(root, ".festival-marker"))
    assertEquals(shown.picks, Vector.empty)
    assertEquals(markersOpened, 1)
    click(one(root, ".site[aria-label='Deep Woods']"))
    assertEquals(shown.picks, Vector(woodsSite))

  test("the marker is no board option"):
    val root = world(woods.copy(festival = true))
    assertEquals(all(root, ".festival-marker[data-option-ref]").size, 0)
    assertEquals(all(root, ".festival-marker button").size, 0)

  test("the Festival banner in the shared bank names the site the marker " +
      "lies at, and the map keeps its own marker"):
    val festival = BannerState("darkest-secret", "festival", None, 0)
    val root = WorldBoardRenderer.world(actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      world = Vector(GameRegion("provinces",
        Vector(woods, peaks.copy(festival = true)))))
      .copy(banners = Vector(festival)), BoardOptions.none)
    click(one(root, ".shared-banners .banner-face"))
    assertEquals(festivalsOpened, Vector(Some("Broken Peaks")))
    assertEquals(markersAt(root, "Broken Peaks").size, 1)

  test("the printed plate carries the title, and the marker's text only " +
      "when asked for it"):
    val full = FestivalMarker.plate(withRule = true)
    assertEquals(all(full, ".festival-plate-title").map(_.textContent),
      Vector("Festival Underway"))
    assertEquals(all(full, ".festival-plate-rule").map(_.textContent),
      Vector(FestivalMarker.Rule))
    assertEquals(all(FestivalMarker.plate(withRule = false),
      ".festival-plate-rule").size, 0)

  test("the plate's outline leans right as the printed marker does: its " +
      "bottom corners lie right of its top ones"):
    val points = FestivalMarker.corners(100, 20)
    val (topLeft, topRight, bottomRight, bottomLeft) =
      (points(0), points(1), points(2), points(3))
    assertEquals((topLeft._2, topRight._2), (0.0, 0.0))
    assertEquals((bottomRight._2, bottomLeft._2), (20.0, 20.0))
    assert(bottomLeft._1 > topLeft._1)
    assert(bottomRight._1 - topRight._1 > bottomLeft._1 - topLeft._1)
    assert(points.forall((x, _) => x >= 0 && x <= 100))
