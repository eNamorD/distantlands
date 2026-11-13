package distantlands.frontend

import distantlands.model.PlayerColor
import distantlands.protocol.projection.{GameTracksProjection,
  SitePowerProjection, TrackedPawnProjection}
import Dom.all
import TestBoardOptions.{available, option, recording}
import TestSites.*
import org.scalajs.dom

/** The printed mark splits what art will replace from what lies on the
  * table in play (ruling R9): it marks content, never a control, and never
  * holds one or any live state. Checked across a full board with options
  * offered on every kind of piece.
  */
class PrintedLayerSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val tracks = GameTracksProjection(round = 3, visionsDrawn = 5,
    usurperLimited = true, limiterRound = 5, firstPlayerId = "red")
  private val busyDenizen = GameSiteCard("d1", "Old Oak", Some(CardDetails("d1",
    "denizen", "Old Oak", suit = Some("order"), orientation = Some("face-up"),
    favor = 12, secrets = 3,
    warbands = Vector(SiteForces.Bandit(2, "Bandit Warbands")),
    tracking = Some(TrackedPawnProjection("red", PlayerColor.Red,
      "Tracking Red")))))
  private val knownRelic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-down"), defense = Some(1), favor = 2)
  private def site(id: String): GameSite =
    quietSite(id, id.capitalize).copy(defense = 1, recoverDifficulty = Some(4),
      powers = Vector(SitePowerProjection("action", "Mountain", Some("Rule"),
        id = s"$id-power")),
      forces = Some(SiteForces.Imperial(10, "Imperial Warbands")))
  private val woods = site("woods").withDenizens(busyDenizen)
    .withRelicCapacity(1).withRelics(1, knownRelic).withLooseTokens(11, 2)
    .copy(festival = true)
  private val table = TestProjections.actingProjection(players = Vector(red),
    world = Vector(
      GameRegion("cradle", Vector(woods, site("keep"))),
      GameRegion("provinces", Vector(site("ford"), site("hill"),
        site("lake"))),
      GameRegion("hinterland", Vector(site("coast"), site("slum")),
        emptySlots = 1))).copy(
    worldDeckCount = 123, worldDeckTopCardKind = Some("denizen"),
    relicDeckCount = 9, tracks = Some(tracks),
    favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 12)),
    pawnLocations = Vector(GamePawn("red", "woods")))
  private val offered = recording(
    available(option("site", "woods")), available(option("denizen", "d1")),
    available(option("relic", "r1")), available(option("deck", "world")),
    available(option("discard-pile", "provinces")),
    available(option("favor-bank", "arcane")),
    available(option("player", "red"))).options
  private val board = WorldBoardRenderer.world(table, offered)

  private def insidePrinted(node: dom.Element): Boolean =
    Option(node.closest(".printed")).nonEmpty

  test("no control is printed or sits inside printed content"):
    val controls = all(board, "button, a, [tabindex]")
    assert(controls.nonEmpty)
    assertEquals(controls.filter(insidePrinted)
      .map(_.getAttribute("class")), Vector.empty)

  test("no live state is printed or sits inside printed content"):
    val live = Vector(".card-tokens", ".card-token-count", ".card-warbands",
      ".card-tracking", ".site-tokens", ".site-token-count", ".pawn",
      ".site-forces", ".site-forces-short", ".pile-count", ".favor-bank-count",
      ".vision-marker", ".round-current", ".limiter-marker", ".legacy-dormant",
      ".card-knowable-pip", ".board-option-badge", ".target-detail-badge",
      ".festival-marker")
    live.foreach { selector =>
      val found = all(board, selector)
      assertEquals(found.filter(insidePrinted).map(_.getAttribute("class")),
        Vector.empty, selector)
    }

  test("the content a control shows is printed, inside the control"):
    val power = all(board, ".site-power").head
    assertEquals(all(power, ".printed").map(_.textContent), Vector("Mountain"))
    assert(all(board, ".site-requirement").forall(_.classList.contains("printed")))
    assert(all(board, ".deck-letter").forall(_.classList.contains("printed")))
    assert(all(board, ".favor-bank > .token-glyph:first-child")
      .forall(_.classList.contains("printed")))
    assert(all(board, ".site-slot-empty").forall(_.classList.contains("printed")))
