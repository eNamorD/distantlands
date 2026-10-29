package oathdigital.frontend

import oathdigital.frontend.Dom.all
import oathdigital.protocol.projection.ModifierMarkProjection
import org.scalajs.dom

/** The printed marks drawn as glyphs: a restriction banner over the whole
  * card, a modifier's arch and a battle plan's crown beside a power. Runs
  * under jsdom. */
class PowerMarksSuite extends munit.FunSuite:
  private def denizenRestricted(restriction: String) = CardDetails("d1",
    "denizen", "Old Oak", restrictions = Some(restriction))

  private def edificeFace(side: String, restriction: String) = CardDetails(
    "E01", "edifice", "Hall of Debate", restrictions = Some(restriction),
    side = Some(side))

  private def label(mark: Option[dom.Element]): Option[String] =
    mark.map(_.getAttribute("aria-label"))

  private def emblems(mark: dom.Element): Vector[String] =
    all(mark, "use").map(_.getAttribute("href"))

  test("each restriction is a banner named in words, with its own emblem"):
    assertEquals(label(PowerMarks.restriction(denizenRestricted("adviser-only"))),
      Some("Adviser only"))
    assertEquals(label(PowerMarks.restriction(denizenRestricted("site-only"))),
      Some("Site only"))
    assertEquals(label(PowerMarks.restriction(
      denizenRestricted("locked-adviser-only"))), Some("Adviser only, locked"))
    assertEquals(PowerMarks.restriction(denizenRestricted("site-only"))
      .map(emblems), Some(Vector("#mark-site")))
    assertEquals(PowerMarks.restriction(denizenRestricted("unrestricted")),
      None)

  test("a locked card carries the chain over its banner; an unlocked one none"):
    val locked = PowerMarks.restriction(denizenRestricted("locked-adviser-only"))
      .getOrElse(fail("no banner"))
    assertEquals(emblems(locked), Vector("#mark-adviser", "#mark-chain"))
    val open = PowerMarks.restriction(denizenRestricted("adviser-only"))
      .getOrElse(fail("no banner"))
    assertEquals(emblems(open), Vector("#mark-adviser"))

  test("an edifice face shows its tower, whole and locked or broken"):
    val intact = PowerMarks.restriction(edificeFace("intact", "locked"))
      .getOrElse(fail("no banner"))
    assertEquals(intact.getAttribute("aria-label"), "Intact edifice, locked")
    assertEquals(emblems(intact), Vector("#mark-tower", "#mark-chain"))
    val ruined = PowerMarks.restriction(edificeFace("ruined", "unrestricted"))
      .getOrElse(fail("no banner"))
    assertEquals(ruined.getAttribute("aria-label"), "Ruined edifice")
    assertEquals(emblems(ruined), Vector("#mark-tower-ruined"))

  test("a modifier is an arch per action, black and named so when persistent"):
    val plain = PowerMarks.modifier(ModifierMarkProjection.Action(
      Vector("muster", "trade")), persistent = false)
    assertEquals(plain.map(_.getAttribute("aria-label")),
      Vector("Muster modifier", "Trade modifier"))
    assertEquals(plain.flatMap(emblems), Vector("#mark-muster", "#mark-trade"))
    val persistent = PowerMarks.action("travel", persistent = true)
    assertEquals(persistent.getAttribute("aria-label"),
      "Persistent Travel modifier")
    assert(persistent.classList.contains("power-mark-persistent"))

  test("a battle plan is a crowned Campaign tab in its side's colour"):
    def crown(sides: String*) = PowerMarks.modifier(
      ModifierMarkProjection.BattlePlan(sides.toVector), persistent = false).head
    val attack = crown("attack")
    assertEquals(attack.getAttribute("class"),
      "power-mark power-mark-plan power-mark-plan-attack")
    assertEquals(emblems(attack), Vector("#mark-campaign"))
    assertEquals(crown("defense").getAttribute("aria-label"), "Defense plan")
    assertEquals(crown("attack", "defense").getAttribute("aria-label"),
      "Battle plan for either side")

  test("the sprite is mounted once, with the either-side gradient"):
    val root = dom.document.createElement("div")
    PowerMarks.mount(root)
    PowerMarks.mount(root)
    assertEquals(all(root, "svg.mark-sprite").size, 1)
    assertEquals(all(root, s"linearGradient#${PowerMarks.EitherSideGradient}")
      .size, 1)
    assert(all(root, "symbol").map(_.getAttribute("id"))
      .contains("mark-travel"))
