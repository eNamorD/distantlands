package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}
import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection

/** The six foundation setup cards as a strip (Foundations design, slice 1):
  * read-only, the altered faces marked, and on the table a click reads a
  * face's rules.
  */
class FoundationStripSuite extends munit.FunSuite:
  private val projected = Vector(
    FoundationProjection(1, "normal", "Imperial Maps", "Fill the map."),
    FoundationProjection(2, "normal", "Powerful Tribes", "Place 3 [favor]."),
    FoundationProjection(3, "altered", "Public Ambitions",
      "Deal 4 legacy cards.\n\nEveryone competes."),
    FoundationProjection(4, "normal", "Ties of Fellowship", "Draw 3."),
    FoundationProjection(5, "normal", "Mob's Favor", "Set the Mob side."),
    FoundationProjection(6, "normal", "Wandering Flame", "Set the Flame."))

  /** The title and rules lines of every text the inspector was asked to
    * open, in order. */
  private var opened = Vector.empty[(String, Vector[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, _) =>
        opened :+= title -> lines
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private def tableStrip = FoundationStrip.render(
    FoundationStrip.ofGame(projected))

  test("the strip lists the six cards in order, each by its numeral and " +
      "the name of its face in use"):
    val strip = tableStrip
    assertEquals(strip.getAttribute("aria-label"), "Foundations")
    assertEquals(one(strip, ".foundations-title").textContent, "Foundations")
    assertEquals(all(strip, ".foundation-strip > li").size, 6)
    assertEquals(all(strip, ".foundation-number").map(_.textContent),
      Vector("I", "II", "III", "IV", "V", "VI"))
    assertEquals(all(strip, ".foundation-name").map(_.textContent),
      Vector("Imperial Maps", "Powerful Tribes", "Public Ambitions",
        "Ties of Fellowship", "Mob's Favor", "Wandering Flame"))

  test("an altered card is marked by word and label, never by a class of " +
      "its own; a normal card carries no mark"):
    val cards = all(tableStrip, ".foundation-card")
    assertEquals(cards.map(_.getAttribute("class")).distinct,
      Vector("foundation-card"))
    assertEquals(cards.map(card => all(card, ".foundation-mark")
      .map(_.textContent)), Vector(Vector.empty, Vector.empty,
      Vector("Altered"), Vector.empty, Vector.empty, Vector.empty))
    assertEquals(cards(2).getAttribute("aria-label"),
      "Foundation III, Public Ambitions, altered")
    assertEquals(cards(0).getAttribute("aria-label"),
      "Foundation I, Imperial Maps")
    assertEquals(cards(2).getAttribute(HoverTip.Attribute),
      "Foundation III · Public Ambitions")

  test("a click on a table card reads the rules of its face in use"):
    val cards = all(tableStrip, "button.foundation-card")
    assertEquals(cards.size, 6)
    assertEquals(cards.map(_.getAttribute("type")).distinct, Vector("button"))
    click(cards(2))
    assertEquals(opened, Vector("Foundation III · Public Ambitions" ->
      Vector("Deal 4 legacy cards.\n\nEveryone competes.")))

  test("a Chronicle's cards show the same names and marks and are not " +
      "buttons"):
    val strip = FoundationStrip.render(FoundationStrip.ofChronicle(Vector(
      ChronicleFoundation(1, "altered", "Wide Horizons"),
      ChronicleFoundation(2, "normal", "Powerful Tribes"))))
    assertEquals(all(strip, "button").size, 0)
    val cards = all(strip, "span.foundation-card")
    // A plain row is read as its text, so it carries no label to ignore.
    assertEquals(cards.map(_.hasAttribute("aria-label")),
      Vector(false, false))
    assertEquals(cards.map(card => all(card, "span").map(_.textContent)),
      Vector(Vector("I", "Wide Horizons", "Altered"),
        Vector("II", "Powerful Tribes")))
    click(cards.head)
    assertEquals(opened, Vector.empty)

  test("a number no card prints reads as its digits"):
    assertEquals(FoundationStrip.numeral(4), "IV")
    assertEquals(FoundationStrip.numeral(7), "7")

  test("a table card opens with the foundation's plate"):
    var plates = Vector.empty[Option[String]]
    CardInspection.onOpen:
      case CardInspection.Request.Text(_, _, _, plate) => plates :+= plate
      case _ => ()
    click(all(tableStrip, "button.foundation-card")(2))
    assertEquals(plates, Vector(Some("foundation")))
