package distantlands.frontend

import distantlands.frontend.Dom.{click, one}
import distantlands.frontend.TestBoards.{quietBoard, withAdvisers}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent}
import distantlands.testkit.Unwrap.present

/** A card on a player's board that prints a power its holder rules opens
  * with their consent preferences, as a site's card does (ruling 16):
  * Ancient Bloodline, an adviser-only denizen, rules from Red's board. */
class BoardCardConsentGridSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val bloodline = CardDetails("165", "denizen", "Ancient Bloodline",
    orientation = Some("face-up"))
  private val tableWithRedHoldingTheBloodline =
    TestProjections.actingProjection(players = Vector(red, blue))
      .copy(playerBoards = Vector(quietBoard("red").withAdvisers(bloodline),
        quietBoard("blue")))
  private val redRulingTheBloodline = tableWithRedHoldingTheBloodline.copy(
    consentPreferences = Vector(ConsentPreferencesState(
      "denizen.ancient-bloodline", "Ancient Bloodline",
      Vector(ConsentPreferenceRowState("blue", "case-by-case")),
      cardId = Some("165"))))

  private var opened = Vector.empty[CardInspection.Request]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen(request => opened :+= request)

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private def adviser(value: GameProjection,
      submit: Option[Intent => Unit] = None) =
    one(WorldBoardRenderer.playerBoards(value, "red", BoardOptions.none,
      submit), ".board-cards .card-face-denizen:not(.card-slot-empty)")

  test("the holder's adviser opens with their grid, and a choice sends " +
      "the preference"):
    var sent = Vector.empty[Intent]
    click(adviser(redRulingTheBloodline, Some(command => sent :+= command)))
    val grid = opened match
      case Vector(CardInspection.Request.RuledCard(card, grid, _)) =>
        assertEquals(card.name, "Ancient Bloodline")
        grid
      case other => fail(s"expected one ruled card, got $other")
    assertEquals(grid.rows,
      Vector(ConsentGridRow("blue", "Blue", "case-by-case")))
    present(grid.choose, "the grid's choice").apply("blue", "never")
    assertEquals(sent, Vector(Intent.SetConsentPreference(
      "denizen.ancient-bloodline", "blue", "never")))

  test("an adviser printing no power the viewer rules opens as a card"):
    click(adviser(tableWithRedHoldingTheBloodline))
    opened match
      case Vector(CardInspection.Request.Card(card, _)) =>
        assertEquals(card.name, "Ancient Bloodline")
      case other => fail(s"expected one plain card, got $other")
