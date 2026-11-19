package distantlands.frontend

import Dom.{all, one}
import TestSites.*
import org.scalajs.dom

/** One site's row drawn alone (Chronicle Details design, slice 3): placed
  * within the rectangle it is given, with its face, its cards and its
  * relic where `MatLayout` places them in the row, and only the pawns it
  * is given. */
class SiteRowSuite extends munit.FunSuite:
  private val relic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-up"))
  private val woodsWithTwoDenizenSlotsAndAKnownRelic =
    quietSite("woods", "Deep Woods").withDenizenCapacity(2)
      .withRelicCapacity(1).withRelics(1, relic)
  private val row = MatLayout.siteRow(1, 1)

  /** Deep Woods in the Provinces' second row, drawn to fill a box of the
    * row's own ratio, with `pawns` and nothing to choose. */
  private def woodsRowFillingItsBox(
      pawns: Vector[dom.Element] = Vector.empty,
      site: GameSite = woodsWithTwoDenizenSlotsAndAKnownRelic): dom.Element =
    WorldBoardRenderer.siteRow(site, 1, 1, row, BoardOptions.none,
      SitePowerLinks.none, pawns)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  test("the row is the site's article, filling the rectangle it is placed " +
      "within, its face first at the site card's place in the row"):
    val site = woodsRowFillingItsBox()
    assertEquals(site.tagName.toLowerCase, "article")
    assertEquals(site.getAttribute("aria-label"), "Deep Woods")
    assertEquals(box(site), row.percentOf(row))
    assertEquals(box(site.firstElementChild),
      MatLayout.siteCard(1, 1).percentOf(row))

  test("its cards and its relic stand where MatLayout places them in the " +
      "row"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-cards > *").map(box),
      Vector(MatLayout.cardSlot(1, 1, 0), MatLayout.cardSlot(1, 1, 1),
        MatLayout.relicSlot(1, 1, 2)).map(_.percentOf(row)))

  test("it draws the pawns it is given, and the festival marker only on " +
      "a festival site"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-pawns > *").size, 0)
    val pawn = ServerUiSupport.element("span", "pawn")
    assertEquals(all(woodsRowFillingItsBox(Vector(pawn)), ".site-pawns > *"),
      Vector(pawn))
    assertEquals(all(woodsRowFillingItsBox(site =
      woodsWithTwoDenizenSlotsAndAKnownRelic.copy(festival = true)),
      ".site-pawns > .festival-marker").size, 1)

  test("on the mat, the row stands where world() places Deep Woods"):
    val onMat = WorldBoardRenderer.siteRow(
      woodsWithTwoDenizenSlotsAndAKnownRelic, 1, 1, MatLayout.Mat,
      BoardOptions.none, SitePowerLinks.none, Vector.empty)
    assertEquals(box(onMat), row.percentOf(MatLayout.Mat))
    assertEquals(one(onMat, ".site-face").getAttribute("aria-hidden"),
      "true")
