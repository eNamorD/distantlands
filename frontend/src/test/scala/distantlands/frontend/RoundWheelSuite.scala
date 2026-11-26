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
    val disc = one(round(wheel, 3), "circle.round-marker")
    // Round 3's middle is 22.5 degrees below the right; the disc stands
    // 76 of the wheel's 100 from its hub.
    assertEqualsDouble(disc.getAttribute("cx").toDouble, 170.215, 0.001)
    assertEqualsDouble(disc.getAttribute("cy").toDouble, 129.084, 0.001)
    assertEquals(disc.getAttribute("r"), "10")
    assertEquals(Option(disc.closest(".printed")), None)
    assertEquals(all(wheel, ".round-marker").size, 1)

  /** The review's finding: cream figures on the silver disc measured
    * 1.47 : 1, and on the limiter's diamond 2.0 : 1. Each wedge's number
    * stands in its inner half, 59 from the hub, and the disc and the
    * diamond in its outer half, 76 from it, so neither lies under a
    * number: the number keeps the wedge's own fill as its ground. */
  test("a round's number stands clear of the round marker and of the " +
      "limiter's diamond"):
    val wheel = wheelAt(round = 3, limiter = 4)
    def fromHub(node: dom.Element, x: String, y: String): Double =
      math.hypot(node.getAttribute(x).toDouble - 100,
        node.getAttribute(y).toDouble - 100)
    val number = one(round(wheel, 3), "text.round-number")
    assertEqualsDouble(number.getAttribute("x").toDouble, 154.509, 0.001)
    assertEqualsDouble(number.getAttribute("y").toDouble, 122.578, 0.001)
    all(wheel, "text.round-number").foreach(label =>
      assertEqualsDouble(fromHub(label, "x", "y"), 59.0, 0.001))
    // The number is 22 units tall at most: 8 either side of its middle.
    val numberOuter = 59.0 + 8
    val disc = one(wheel, "circle.round-marker")
    assert(fromHub(disc, "cx", "cy") - disc.getAttribute("r").toDouble >=
      numberOuter - 1)
    val diamond = one(round(wheel, 4), "text.limiter-diamond")
    assertEqualsDouble(diamond.getAttribute("x").toDouble, 129.084, 0.001)
    assertEqualsDouble(diamond.getAttribute("y").toDouble, 170.215, 0.001)

  test("the current round's number is marked, for its colour on the " +
      "wedge's brass, and no other is"):
    val wheel = wheelAt(round = 3, limiter = 5)
    assertEquals(all(wheel, "text.round-number-current").map(
      _.textContent), Vector("3"))
    assert(one(wheel, "text.round-number-current").classList
      .contains("printed"))

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
    val image = one(round(wheel, 4), "image.art")
    assertEquals(image.getAttribute("href"), "/art/tts/tokens/usurper.png")
    assertEquals(image.getAttribute("style"), "width: 54px; height: 55px;")
    // Round 4's middle is 67.5 degrees below the right, and its wedge's
    // centre 68.5 from the hub, at (126.214, 163.286); the token's box is
    // centred there.
    assertEqualsDouble(image.getAttribute("x").toDouble, 99.214, 0.001)
    assertEqualsDouble(image.getAttribute("y").toDouble, 135.786, 0.001)
    // The file stands point up; turned by the wedge's angle less a
    // quarter, its point faces the hub.
    assertEquals(image.getAttribute("transform"),
      "rotate(-22.50 126.21 163.29)")
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
