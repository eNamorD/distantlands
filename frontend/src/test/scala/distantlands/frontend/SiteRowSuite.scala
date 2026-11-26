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
  /** The box a row drawn alone stands in: the row and the whole of its
    * site card, which reaches past the row by the frame's line. */
  private val whole = MatLayout.siteRowWithCard(1, 1)

  /** Deep Woods in the Provinces' second row, drawn alone in the box
    * that holds its row and its whole site card, with `pawns` and nothing
    * to choose. */
  private def woodsRowFillingItsBox(
      pawns: Vector[dom.Element] = Vector.empty,
      site: GameSite = woodsWithTwoDenizenSlotsAndAKnownRelic): dom.Element =
    WorldBoardRenderer.siteRow(site, 1, 1, whole, BoardOptions.none,
      SitePowerLinks.none, pawns)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  test("the row is the site's article, at the row's place in the " +
      "rectangle it is placed within, its face first at the site card's " +
      "place in the row"):
    val site = woodsRowFillingItsBox()
    assertEquals(site.tagName.toLowerCase, "article")
    assertEquals(site.getAttribute("aria-label"), "Deep Woods")
    assertEquals(box(site), row.percentOf(whole))
    assertEquals(box(site.firstElementChild),
      MatLayout.siteCard(1, 1).percentOf(row))

  /** The review's finding: a box of the row's own size cut the site
    * card, which reaches past the row. The face's box, taken through the
    * row's, must lie inside the box the row is drawn in. */
  test("a row drawn alone shows its whole site card: the face lies " +
      "inside the box the row stands in"):
    val site = woodsRowFillingItsBox()
    def percent(node: dom.Element, name: String): Double =
      node.asInstanceOf[dom.html.Element].style.getPropertyValue(name)
        .stripSuffix("%").toDouble
    val face = site.firstElementChild
    val left = percent(site, "left") +
      percent(face, "left") * percent(site, "width") / 100
    val top = percent(site, "top") +
      percent(face, "top") * percent(site, "height") / 100
    val bottom = top + percent(face, "height") * percent(site, "height") / 100
    assert(left >= -0.001, left)
    assert(top >= -0.001, top)
    assert(bottom <= 100.001, bottom)

  test("its cards and its relic stand where MatLayout places them in the " +
      "row"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-cards > *").map(box),
      Vector(MatLayout.cardSlot(1, 1, 0), MatLayout.cardSlot(1, 1, 1),
        MatLayout.relicSlot(1, 1, 2)).map(_.percentOf(row)))

  test("it draws the pawns it is given"):
    assertEquals(all(woodsRowFillingItsBox(), ".site-pawns > *").size, 0)
    val pawn = ServerUiSupport.element("span", "pawn")
    assertEquals(all(woodsRowFillingItsBox(Vector(pawn)), ".site-pawns > *"),
      Vector(pawn))

  test("only a festival site draws the festival marker, where MatLayout " +
      "lays it over the cards in the row"):
    assertEquals(all(woodsRowFillingItsBox(), ".festival-marker").size, 0)
    val markers = all(woodsRowFillingItsBox(site =
      woodsWithTwoDenizenSlotsAndAKnownRelic.copy(festival = true)),
      ":scope > .festival-marker")
    assertEquals(markers.map(box),
      Vector(MatLayout.festivalMarker(1, 1).percentOf(row)))

  test("on the mat, the row stands where world() places Deep Woods, and " +
      "its face and cards still stand relative to the row"):
    val onMat = WorldBoardRenderer.siteRow(
      woodsWithTwoDenizenSlotsAndAKnownRelic, 1, 1, MatLayout.Mat,
      BoardOptions.none, SitePowerLinks.none, Vector.empty)
    assertEquals(box(onMat), row.percentOf(MatLayout.Mat))
    val face = one(onMat, ".site-face")
    assertEquals(face.getAttribute("aria-hidden"), "true")
    assertEquals(box(face), MatLayout.siteCard(1, 1).percentOf(row))
    assertEquals(all(onMat, ".site-cards > *").map(box),
      Vector(MatLayout.cardSlot(1, 1, 0), MatLayout.cardSlot(1, 1, 1),
        MatLayout.relicSlot(1, 1, 2)).map(_.percentOf(row)))
