package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}

/** A strip of setup cards under a heading (Chronicle Details design, slice
  * 2): its classes come from a stem, a card without a mark or a tag draws
  * neither, and a click reads the card's lines. */
class SetupCardStripSuite extends munit.FunSuite:
  private var opened = Vector.empty[(String, Vector[String], Option[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate) =>
        opened :+= ((title, lines, plate))
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private val unmarkedAidCard = SetupCardStrip.Card(None, "The Sun", None,
    "Aid · The Sun", "Aid, The Sun", Vector("Front step.", "Back",
      "Back step."), None)
  private val markedTaggedFoundationCard = SetupCardStrip.Card(Some("III"),
    "Public Ambitions", Some("Altered"), "Foundation III · Public Ambitions",
    "Foundation III, Public Ambitions, altered", Vector("Deal 4."),
    Some("foundation"))

  private def aidStripOfOneUnmarkedCard =
    SetupCardStrip.render("Aids", "aid", Vector(unmarkedAidCard))

  test("the strip's section, heading, list and card take their classes " +
      "from its stem"):
    val strip = aidStripOfOneUnmarkedCard
    assertEquals(strip.getAttribute("class"), "aids")
    assertEquals(strip.getAttribute("aria-label"), "Aids")
    assertEquals(one(strip, "h3.aids-title").textContent, "Aids")
    assertEquals(all(strip, "ol.aid-strip > li > button.aid-card").size, 1)
    assertEquals(one(strip, ".aid-name").textContent, "The Sun")

  test("a card without a mark or a tag draws neither; one with both draws " +
      "both"):
    assertEquals(all(aidStripOfOneUnmarkedCard, ".aid-number, .aid-mark")
      .size, 0)
    val marked = SetupCardStrip.render("Foundations", "foundation",
      Vector(markedTaggedFoundationCard))
    assertEquals(one(marked, ".foundation-number").textContent, "III")
    assertEquals(one(marked, ".foundation-mark").textContent, "Altered")

  test("a card is labelled and noted by its own words, and a click reads " +
      "its lines under its title, with its plate when it has one"):
    val card = one(aidStripOfOneUnmarkedCard, "button.aid-card")
    assertEquals(card.getAttribute("type"), "button")
    assertEquals(card.getAttribute("aria-label"), "Aid, The Sun")
    assertEquals(card.getAttribute(HoverTip.Attribute), "Aid · The Sun")
    click(card)
    click(one(SetupCardStrip.render("Foundations", "foundation",
      Vector(markedTaggedFoundationCard)), "button.foundation-card"))
    assertEquals(opened, Vector(
      ("Aid · The Sun", Vector("Front step.", "Back", "Back step."), None),
      ("Foundation III · Public Ambitions", Vector("Deal 4."),
        Some("foundation"))))
