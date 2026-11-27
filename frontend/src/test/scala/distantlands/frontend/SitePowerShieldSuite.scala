package distantlands.frontend

import Dom.{all, one}
import TestSites.*
import distantlands.protocol.projection.SitePowerProjection
import org.scalajs.dom

/** A site's powers on the shield its card prints (game art rulings R53 to
  * R55): one button a power, on the part of the shield the card prints
  * that power in, the same markup with art on and off. */
class SitePowerShieldSuite extends munit.FunSuite:
  private var opened = Vector.empty[CardInspection.Request]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen(request => opened :+= request)

  override def afterEach(context: AfterEach): Unit =
    CardInspection.clear()
    GameArt.reset()

  private def power(kind: String, label: String): SitePowerProjection =
    SitePowerProjection(kind, label, Some(s"$label rule."),
      id = s"site.test.$kind")

  private val row = MatLayout.siteRow(1, 0)

  /** The Provinces' first site, named `label`, printing `powers`, and
    * the picks a click on the site records. */
  private def siteWithPowers(label: String, powers: SitePowerProjection*)
      : (dom.Element, RecordingOptions) =
    val offered = TestBoardOptions.recording(TestBoardOptions.available(
      TestBoardOptions.option("site", "keep")))
    val value = TestProjections.actingProjection(world = Vector(
      GameRegion("cradle", Vector.empty), GameRegion("provinces", Vector(
        quietSite("keep", label).copy(powers = powers.toVector)))))
    (one(WorldBoardRenderer.world(value, offered.options), "article.site"),
      offered)

  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  test("one power is a button over the whole shield, named by its label, " +
      "its label printed and its rule on its hover note"):
    val (site, _) = siteWithPowers("Mines", power("mountain", "Mountain"))
    val shield = one(site, ":scope > .site-shield")
    val place = MatLayout.sitePowerShield(1, 0)
    assertEquals(box(shield), place.percentOf(row))
    assert(!shield.classList.contains("printed"))
    val button = one(shield, ":scope > button.site-power")
    assertEquals(box(button), place.percentOf(place))
    assertEquals(button.getAttribute("aria-label"), "Mountain")
    assertEquals(all(button, ":scope > .printed").map(_.textContent),
      Vector("Mountain"))
    assertEquals(button.getAttribute(HoverTip.Attribute),
      "Mountain: Mountain rule.")
    assertEquals(Option(button.closest(".printed")), None)

  /** R55: Headwaters arrives as River, Mountain and prints Mountain at
    * the left. The buttons keep the projection's order for reading and
    * the keyboard; each stands on its own side. */
  test("two powers side by side stand as the card prints them, in the " +
      "projection's order in the markup"):
    val (site, _) = siteWithPowers("Headwaters", power("river", "River"),
      power("mountain", "Mountain"))
    val place = MatLayout.sitePowerShield(1, 0)
    val buttons = all(site, ".site-shield > .site-power")
    assertEquals(buttons.map(_.getAttribute("aria-label")),
      Vector("River", "Mountain"))
    val regions = MatLayout.sitePowerRegions(Vector("river", "mountain"), 1, 0)
    assertEquals(buttons.map(box), regions.map(_.percentOf(place)))
    // Mountain's left edge is the shield's; River begins at the red line.
    assertEquals(box(buttons(1)).toMap.apply("left"), "0%")
    assertEquals(box(buttons(0)).toMap.apply("left"), "49.6403%")

  test("an island's powers stand one above the other: the Coast's ship " +
      "over the Island's cost"):
    val (site, _) = siteWithPowers("Fair Isle", power("coast", "Coast"),
      power("island", "Island"))
    val buttons = all(site, ".site-shield > .site-power")
    assertEquals(buttons.map(node => box(node).toMap.apply("top")),
      Vector("0%", "60.6879%"))
    assertEquals(buttons.map(node => box(node).toMap.apply("width")),
      Vector("100%", "100%"))

  test("a press on a power opens it in the inspector and does not " +
      "reach the site, which a press beside the shield still picks"):
    val (site, offered) = siteWithPowers("Mines", power("mountain",
      "Mountain"))
    one(site, ".site-power").asInstanceOf[dom.html.Button].click()
    opened match
      case Vector(request: CardInspection.Request.Power) =>
        assertEquals((request.title, request.lines, request.grid),
          ("Mountain", Vector("Mountain rule."), None))
      case other => fail(s"expected one power request, got $other")
    assertEquals(offered.picks, Vector.empty)
    site.asInstanceOf[dom.html.Element].click()
    assertEquals(offered.picks.map(_.id), Vector("keep"))

  test("the shield and its buttons are the same markup with art on and " +
      "off, and the old line of power labels is gone"):
    val (drawn, _) = siteWithPowers("Headwaters", power("river", "River"),
      power("mountain", "Mountain"))
    assertEquals(all(drawn, ".site-powers"), Vector.empty)
    assertEquals(one(drawn, ".site-shield").textContent, "RiverMountain")
    TestArt.listingWholeCatalog()
    val (withArt, _) = siteWithPowers("Headwaters", power("river", "River"),
      power("mountain", "Mountain"))
    assertEquals(all(withArt, ".site-face > img.art").size, 1)
    all(withArt, ".art-crop, .art").foreach(_.remove())
    assertEquals(withArt.outerHTML, drawn.outerHTML)

  /** The drawn name starts right of the shield, as the card prints it:
    * the site carries how far the shield reaches into its face. */
  test("the site says how far the shield reaches into its face, for the " +
      "drawn name"):
    val (site, _) = siteWithPowers("Mines", power("mountain", "Mountain"))
    assertEquals(site.asInstanceOf[dom.html.Element].style
      .getPropertyValue("--shield-reach"), "7.9556%")
