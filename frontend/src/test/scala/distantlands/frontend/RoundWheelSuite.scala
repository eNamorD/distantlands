package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.projection.GameTracksProjection
import org.scalajs.dom

/** The round wheel's pieces in play (implementation rulings R39, R40):
  * the round marker on the current round's wedge and the Usurper's token
  * on the limiter's. Both are live state, drawn with art on and off. */
class RoundWheelSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)

  /** The wheel of a game in `round`, the Usurper limited at `limiter`. */
  private def wheelAt(round: Int, limiter: Int): dom.Element =
    one(WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(GameRegion("cradle", Vector.empty))).copy(
      tracks = Some(GameTracksProjection(round = round, visionsDrawn = 0,
        usurperLimited = true, limiterRound = limiter,
        firstPlayerId = "red"))), BoardOptions.none), ".round-tracker svg")

  private def round(wheel: dom.Element, number: Int): dom.Element =
    one(wheel, s"""g[data-round="$number"]""")

  test("the current round's wedge holds the round marker, a disc in its " +
      "outer half, and no other wedge does"):
    val wheel = wheelAt(round = 3, limiter = 5)
    val segment = WorldBoardRenderer.roundSegment(3)
    val disc = one(round(wheel, 3), "circle.round-marker")
    assertEquals(disc.getAttribute("cx").toDouble, segment.discX, 0.001)
    assertEquals(disc.getAttribute("cy").toDouble, segment.discY, 0.001)
    assertEquals(disc.getAttribute("r"), "11")
    assertEquals(Option(disc.closest(".printed")), None)
    assertEquals(all(wheel, ".round-marker").size, 1)
    // 74 of the wheel's 100 from its hub: the outer half of the wedge.
    assertEquals(math.hypot(segment.discX - 100, segment.discY - 100), 74.0,
      0.001)

  test("the current wedge keeps its border, under the round marker"):
    val group = round(wheelAt(round = 3, limiter = 5), 3)
    val order = all(group, ":scope > *").map(_.getAttribute("class"))
    assert(order.indexOf("round-current") < order.indexOf("round-marker"),
      order)

  test("with art off the limiter's wedge is drawn as a wedge with its " +
      "diamond, and carries the lock's words"):
    val wheel = wheelAt(round = 1, limiter = 4)
    val mark = one(round(wheel, 4), "path.limiter-marker")
    assertEquals(mark.getAttribute("d"),
      WorldBoardRenderer.roundSegment(4).path)
    assertEquals(mark.getAttribute(HoverTip.Attribute),
      "Usurper locked until round 4")
    assertEquals(one(round(wheel, 4), "text.limiter-diamond").textContent,
      "◆")
    assertEquals(all(wheel, ".limiter-marker").size, 1)
    assertEquals(all(wheel, "image"), Vector.empty)

  test("the Usurper's token fills the limiter's wedge, its point to the " +
      "hub (R39)"):
    TestArt.listingWholeCatalog()
    val wheel = wheelAt(round = 1, limiter = 4)
    val segment = WorldBoardRenderer.roundSegment(4)
    val image = one(round(wheel, 4), "image.art")
    assertEquals(image.getAttribute("href"), "/art/tts/tokens/usurper.png")
    assertEquals(image.getAttribute("style"), "width: 54px; height: 55px;")
    assertEquals(image.getAttribute("x").toDouble, segment.centreX - 27,
      0.001)
    assertEquals(image.getAttribute("y").toDouble, segment.centreY - 27.5,
      0.001)
    // The file stands point up; turned by the wedge's angle less a
    // quarter, its point faces the hub.
    assertEquals(image.getAttribute("transform"),
      f"rotate(${segment.middle - 90}%.2f ${segment.centreX}%.2f " +
        f"${segment.centreY}%.2f)")
    image.dispatchEvent(new dom.Event("load"))
    assert(one(round(wheel, 4), ".limiter-marker").classList
      .contains("art-ready"))

  test("when the limiter's round is the current round the token lies " +
      "under the round marker and the wedge's border"):
    TestArt.listingWholeCatalog()
    val group = round(wheelAt(round = 4, limiter = 4), 4)
    val order = all(group, ":scope > *").map(node =>
      Option(node.getAttribute("class")).getOrElse(node.tagName))
    assert(order.indexOf("art") < order.indexOf("round-current"), order)
    assert(order.indexOf("round-current") < order.indexOf("round-marker"),
      order)

  test("the wheel drawn with art and without is the same markup once " +
      "the image is removed"):
    val drawn = wheelAt(round = 4, limiter = 4)
    TestArt.listingWholeCatalog()
    val copy = wheelAt(round = 4, limiter = 4).cloneNode(true)
      .asInstanceOf[dom.Element]
    all(copy, ".art").foreach(_.remove())
    assertEquals(copy.outerHTML, drawn.outerHTML)
