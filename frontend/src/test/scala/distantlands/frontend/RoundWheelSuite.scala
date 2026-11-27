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

  /** The owner's second review (R57): the disc stands at the centre of
    * the current wedge, radially and by angle. */
  test("the current round's wedge holds the round marker, a disc at its " +
      "centre, and no other wedge does"):
    val wheel = wheelAt(round = 3, limiter = 5)
    val disc = one(round(wheel, 3), "circle.round-marker")
    // Round 3's middle is 22.5 degrees below the right; the wedge runs
    // from 49 to 88 of the wheel's 100 from its hub, so its centre is
    // 68.5 out.
    assertEqualsDouble(disc.getAttribute("cx").toDouble, 163.286, 0.001)
    assertEqualsDouble(disc.getAttribute("cy").toDouble, 126.214, 0.001)
    // As large as the wedge allows (R57, rewritten): 16 of the wedge's
    // 19.5 either side of its centre, so it stays inside the wedge and
    // its number reads at desktop Fit.
    assertEquals(disc.getAttribute("r"), "16")
    assertEquals(Option(disc.closest(".printed")), None)
    assertEquals(all(wheel, ".round-marker").size, 1)

  /** R57: every number stands at its wedge's centre; the current round's
    * lies on the disc, drawn after it, in Base (the stylesheet suite pins
    * its contrast on the silver). The limiter's diamond stands in the
    * wedge's rim, 82 from the hub, clear of the number: a number is 8
    * either side of its middle, and the diamond, at 12 units, 5. */
  test("a round's number stands at its wedge's centre, over the round " +
      "marker, and clear of the limiter's diamond"):
    val wheel = wheelAt(round = 3, limiter = 4)
    def fromHub(node: dom.Element, x: String, y: String): Double =
      math.hypot(node.getAttribute(x).toDouble - 100,
        node.getAttribute(y).toDouble - 100)
    val number = one(round(wheel, 3), "text.round-number")
    assertEqualsDouble(number.getAttribute("x").toDouble, 163.286, 0.001)
    assertEqualsDouble(number.getAttribute("y").toDouble, 126.214, 0.001)
    all(wheel, "text.round-number").foreach(label =>
      assertEqualsDouble(fromHub(label, "x", "y"), 68.5, 0.001))
    val order = all(round(wheel, 3), ":scope > *").map(_.getAttribute("class"))
    assert(order.indexOf("round-marker") <
      order.indexOf("round-number round-number-current printed"), order)
    val diamond = one(round(wheel, 4), "text.limiter-diamond")
    assertEqualsDouble(diamond.getAttribute("x").toDouble, 131.380, 0.001)
    assertEqualsDouble(diamond.getAttribute("y").toDouble, 175.758, 0.001)
    assert(fromHub(diamond, "x", "y") - 5 >= 68.5 + 8)

  test("the current round's number is marked, for its colour on the " +
      "round marker, and no other is"):
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
