package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}
import distantlands.protocol.projection.ChronicleTaskPresentation

/** The Chronicle Phase aids (Chronicle Details design, slice 2): the five
  * task cards in the foundation strip's format, unmarked, each opening its
  * lines. */
class ChronicleTaskStripSuite extends munit.FunSuite:
  private var opened = Vector.empty[(String, Vector[String], Option[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate) =>
        opened :+= ((title, lines, plate))
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  test("the strip is headed Chronicle Phase and lists the five cards in " +
      "order, with no numeral and no tag"):
    val strip = ChronicleTaskStrip.render()
    assertEquals(strip.getAttribute("aria-label"), "Chronicle Phase")
    assertEquals(one(strip, ".chronicle-tasks-title").textContent,
      "Chronicle Phase")
    assertEquals(all(strip, ".chronicle-task-name").map(_.textContent),
      Vector("The Sun", "The Throne", "The World", "The Beacon",
        "The Stars"))
    assertEquals(all(strip, ".chronicle-task-number, .chronicle-task-mark")
      .size, 0)

  test("a card is labelled and noted as a Chronicle task (R8)"):
    val sun = all(ChronicleTaskStrip.render(), "button.chronicle-task-card")
      .head
    assertEquals(sun.getAttribute("aria-label"), "Chronicle task, The Sun")
    assertEquals(sun.getAttribute(HoverTip.Attribute),
      "Chronicle task · The Sun")

  test("a click on the Sun reads its front, a Back line and its back; a " +
      "click on the World reads its one face; neither has a plate"):
    val cards = all(ChronicleTaskStrip.render(), "button.chronicle-task-card")
    click(cards(0))
    click(cards(2))
    assertEquals(opened.map((title, _, plate) => (title, plate)), Vector(
      ("Chronicle task · The Sun", None),
      ("Chronicle task · The World", None)))
    // The Sun: Resolve Legacies and the six goals, Back, then Crown
    // Chancellor and Assign Tasks; the World: its three steps.
    val sun = opened(0)._2
    assertEquals((sun.size, sun.indexOf("Back")), (10, 7))
    assert(sun.head.startsWith("**Resolve Legacies.** Reveal facedown " +
      "relics held by players."), sun.head)
    assert(sun.last.startsWith("**Assign Tasks.** The winner takes the " +
      "Throne task card."), sun.last)
    val world = opened(1)._2
    assertEquals((world.size, world.head), (3,
      "**Separate Visions.** Set aside all 5 Vision cards."))
    assert(!world.contains("Back"), world)

  test("every task line renders as one paragraph, its markup all read: no " +
      "literal asterisk, underscore or backtick is left in its text"):
    ChronicleTaskPresentation.all.flatMap(_.lines).foreach { line =>
      val paragraphs = RulesTextRenderer.powers(line)
      assertEquals(paragraphs.size, 1, line)
      val words = paragraphs.head.textContent
      assert(!words.exists(Set('*', '_', '`')), words)
    }
