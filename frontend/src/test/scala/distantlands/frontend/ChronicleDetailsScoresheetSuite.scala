package distantlands.frontend

import Dom.{all, click, one}
import distantlands.protocol.ChronicleScorepadEntry
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  LineageDetailsProjection}

/** The era scorepad in Chronicle Details (Chronicle Details design, slice
  * 4): the Chronicle page's sheet, with each lineage's legacies as chips
  * that open the card. */
class ChronicleDetailsScoresheetSuite extends munit.FunSuite:
  private var inspected = Vector.empty[String]

  override def beforeEach(context: BeforeEach): Unit =
    inspected = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Card(card, _) => inspected :+= card.cardId
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private val seer = CardDetails("L22", "legacy", "Seer",
    suit = Some("arcane"), orientation = Some("face-up"))
  private val legacyBack = CardDetails("hidden", "legacy", "Facedown legacy",
    orientation = Some("face-down"), hidden = true)

  /** Red, a Chancellor at 3 holding Seer and a legacy the viewer may not
    * know, and Blue, an Exile at 2 holding none, after `games` games in
    * which both scored. */
  private def redWithSeerAndABackBlueWithNone(games: Int)
      : ChronicleDetailsProjection =
    ChronicleDetailsProjection(20,
      Vector.fill(games)(Vector(
        ChronicleScorepadEntry("red-lineage", 3, Some("chancellor")),
        ChronicleScorepadEntry("blue-lineage", 2))),
      Vector(
        LineageDetailsProjection("red-lineage", 3, Some("chancellor"),
          Vector(seer, legacyBack)),
        LineageDetailsProjection("blue-lineage", 2, None, Vector.empty)),
      None)

  private def drawn(details: ChronicleDetailsProjection) =
    ChronicleScoresheet.render(ChronicleScoresheet.of(details))

  test("each lineage's cell holds its name and its legacies as chips: a " +
      "face that opens the card, and an L back"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(1))
    val red = all(sheet, "tbody tr").head
    assertEquals(one(red, ".scoresheet-lineage-name").textContent, "Red")
    val chips = all(red, ".scoresheet-legacy-chips > button")
    assertEquals(chips.map(_.getAttribute("class")),
      Vector("legacy-chip", "legacy-chip legacy-chip-back"))
    assertEquals(chips.map(_.textContent).last, "L")
    click(chips.head)
    assertEquals(inspected, Vector("L22"))
    assertEquals(all(all(sheet, "tbody tr")(1), ".scoresheet-legacy-chips")
      .size, 0)
    assertEquals(all(sheet, ".scoresheet-legacies").size, 0)

  test("with no game played the sheet has its lineage rows and totals " +
      "only"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(0))
    assertEquals(all(sheet, "thead th").map(_.textContent),
      Vector("Lineage", "Total"))
    assertEquals(all(sheet, ".scoresheet-total").map(_.textContent),
      Vector("3", "2"))

  test("seven games show the last five, after the earlier-games column"):
    val sheet = drawn(redWithSeerAndABackBlueWithNone(7))
    assertEquals(all(sheet, "thead th").map(_.textContent),
      Vector("Lineage", "", "Game 3", "Game 4", "Game 5", "Game 6", "Game 7",
        "Total"))
    assertEquals(all(sheet, "tbody td.scoresheet-earlier")
      .map(_.textContent), Vector("…", "…"))
