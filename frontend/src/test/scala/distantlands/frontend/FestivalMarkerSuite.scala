package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.{all, click, one}
import TestBoardOptions.{available, option, recording}
import TestProjections.actingProjection
import TestSites.quietSite
import org.scalajs.dom

/** The festival marker over a site's cards, drawn at the end of the site's
  * pawn row (Foundations design, slice 6). */
class FestivalMarkerSuite extends munit.FunSuite:
  private val woods = quietSite("site:woods", "Deep Woods")
  private val peaks = quietSite("site:peaks", "Broken Peaks")

  /** The title and lines of every text the inspector was asked to open. */
  private var opened = Vector.empty[(String, Vector[String])]

  /** The marker site of every Festival inspection asked for. */
  private var festivalsOpened = Vector.empty[Option[String]]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    festivalsOpened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, _) =>
        opened :+= title -> lines
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

  /** The class of each item in the pawn row of the site named `label`. */
  private def pawnRow(root: dom.Element, label: String): Vector[String] =
    all(root, s".site[aria-label='$label'] .site-pawns > *")
      .map(_.getAttribute("class"))

  test("the site it lies over ends its pawn row with the marker, which " +
      "names itself and holds the Festival's power in its note"):
    val root = world(woods.copy(festival = true), peaks)
    assertEquals(pawnRow(root, "Deep Woods"), Vector("pawn", "festival-marker"))
    assertEquals(pawnRow(root, "Broken Peaks"), Vector.empty)
    val marker = all(root, ".festival-marker")
    assertEquals(marker.map(_.textContent), Vector("Festival"))
    assertEquals(marker.map(_.getAttribute("aria-label")),
      Vector("Festival marker"))
    assertEquals(marker.map(_.getAttribute("data-tip")),
      Vector(WorldBoardRenderer.festivalNote))

  test("it stands alone in the row of a site no pawn stands at"):
    val root = world(woods, peaks.copy(festival = true))
    assertEquals(pawnRow(root, "Broken Peaks"), Vector("festival-marker"))

  test("with the marker beside the map no site draws it"):
    assertEquals(all(world(woods, peaks), ".festival-marker").size, 0)

  test("the marker is a button, so a keyboard reaches it, and pressing it " +
      "opens the Festival's power in the inspector"):
    val root = world(woods.copy(festival = true))
    val marker = one(root, ".festival-marker")
    assertEquals(marker.tagName, "BUTTON")
    click(marker)
    assertEquals(opened, Vector("Festival marker" ->
      Vector(WorldBoardRenderer.festivalNote)))

  test("pressing the marker on a site that is an option leaves the site " +
      "unpicked"):
    val woodsSite = option("site", "site:woods")
    val shown = recording(available(woodsSite))
    val root = worldWith(shown.options, woods.copy(festival = true))
    click(one(root, ".festival-marker"))
    assertEquals(shown.picks, Vector.empty)
    assertEquals(opened.map(_._1), Vector("Festival marker"))
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
    assertEquals(pawnRow(root, "Broken Peaks"), Vector("festival-marker"))
