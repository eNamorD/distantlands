package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.projection.{GameTracksProjection,
  OathkeeperProjection}
import TestSites.*
import org.scalajs.dom

/** Pieces and markers with game art (game art design, "Pieces and
  * tokens"; implementation rulings R26 to R33): warbands and pawns by
  * colour, the Visions Drawn marker, the Usurper's mark, the festival
  * plate and the title plate. */
class PieceArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val black = GamePlayer("black", "Black", "Exile", PlayerColor.Black)

  private def site(id: String, forces: SiteForces): GameSite =
    quietSite(id, id.capitalize).copy(forces = Some(forces))

  /** A world with a red, an Imperial and a bandit force, a red and a black
    * pawn, the Usurper limited at round 5 and two Visions drawn. */
  private def worldWithForcesAndPawns(): dom.Element =
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red, black),
      world = Vector(GameRegion("cradle", Vector(
        site("keep", SiteForces.Exile(2, "red", PlayerColor.Red,
          "Red Warbands")),
        site("camp", SiteForces.Imperial(3, "Imperial Warbands")))),
        GameRegion("provinces", Vector(
          site("ford", SiteForces.Bandit(1, "Bandit Warbands")))))).copy(
      tracks = Some(GameTracksProjection(round = 3, visionsDrawn = 2,
        usurperLimited = true, limiterRound = 5, firstPlayerId = "red")),
      pawnLocations = Vector(GamePawn("red", "keep"),
        GamePawn("black", "camp"))), BoardOptions.none)

  private def shortForces(world: dom.Element, label: String): Vector[String] =
    all(one(world, s"""article.site[aria-label="$label"]"""),
      ".site-warbands > .site-force image.art").map(_.getAttribute("href"))

  test("each force's warbands hold that force's piece (R26)"):
    TestArt.listingWholeCatalog()
    val world = worldWithForcesAndPawns()
    assertEquals(shortForces(world, "Keep"),
      Vector("/art/tts/pieces/warband-red.png"))
    assertEquals(shortForces(world, "Camp"),
      Vector("/art/tts/pieces/warband-purple.png"))
    assertEquals(shortForces(world, "Ford"),
      Vector("/art/tts/tokens/bandit.png"))

  test("warbands on a card hold their force's piece"):
    TestArt.listingWholeCatalog()
    val card = CardFace.render(CardDetails("c", "relic", "Obsidian Cage",
      orientation = Some("face-up"), warbands = Vector(
        SiteForces.Exile(2, "red", PlayerColor.Red, "Red Warbands"))))
    assertEquals(all(card, ".card-warbands image.art")
      .map(_.getAttribute("href")),
      Vector("/art/tts/pieces/warband-red.png"))
    assertEquals(one(card, ".card-warbands .card-token-count").textContent,
      "2")

  test("the Supply track's warband holds no image"):
    TestArt.listingWholeCatalog()
    assertEquals(all(RulesTextRenderer.glyph("warband"), "image"),
      Vector.empty)

  test("a pawn on the map holds its player's pawn in its piece, over " +
      "its player's name"):
    TestArt.listingWholeCatalog()
    val world = worldWithForcesAndPawns()
    val pawns = all(world, ".site-pawns .pawn")
    assertEquals(pawns.map(pawn => all(pawn, ".pawn-piece > img.art")
      .map(_.getAttribute("src"))), Vector(
      Vector("/art/tts/pieces/pawn-red.png"),
      Vector("/art/tts/pieces/pawn-black.png")))
    pawns.foreach { pawn =>
      assertEquals(all(pawn, ":scope > *").map(_.getAttribute("class")
        .split(" ").head), Vector("pawn-piece", "pawn-name"))
      assertEquals(all(pawn, ".pawn-name img"), Vector.empty)
    }

  test("the Visions Drawn marker holds its picture on the current cell " +
      "(R31)"):
    TestArt.listingWholeCatalog()
    assertEquals(all(worldWithForcesAndPawns(), ".vision-marker > img.art")
      .map(_.getAttribute("src")),
      Vector("/art/tts/markers/visions-drawn-marker.png"))

  test("the printed festival plate holds the marker's picture; the " +
      "plate without its rule holds none (R29)"):
    TestArt.listingWholeCatalog()
    assertEquals(all(FestivalMarker.plate(withRule = true),
      ":scope > img.art").map(_.getAttribute("src")),
      Vector("/art/tts/tokens/festival-underway.jpg"))
    assertEquals(all(FestivalMarker.plate(withRule = false), "img"),
      Vector.empty)
    assertEquals(all(FestivalMarker.band(MatLayout.festivalMarker(0, 0)),
      "img"), Vector.empty)

  test("the title's pill asks the overlay for the picture of the side " +
      "in force (R28)"):
    TestArt.listingWholeCatalog()
    var asked = Vector.empty[Option[String]]
    CardInspection.onOpen {
      case CardInspection.Request.Text(_, _, _, plate, art, _) =>
        if plate.contains("oathkeeper-title") then
          asked :+= art.map(_.path)
      case _ => ()
    }
    try Vector("oathkeeper", "usurper").foreach { side =>
      val boards = WorldBoardRenderer.playerBoards(
        TestProjections.actingProjection(players = Vector(red)).copy(
          oathkeeper = Some(OathkeeperProjection("supremacy", Some("red"),
            side, usurperLimited = false, winnerPlayerId = None)),
          playerBoards = Vector(TestBoards.quietBoard("red"))), "red",
        BoardOptions.none)
      one(boards, ".player-title").asInstanceOf[dom.html.Button].click()
    }
    finally CardInspection.clear()
    assertEquals(asked, Vector(Some("tts/titles/oathkeeper.jpg"),
      Some("tts/titles/usurper.jpg")))

  test("with art off no piece or marker holds an image, and the world's " +
      "markup is what it is with art once the images are removed"):
    val drawn = worldWithForcesAndPawns()
    assertEquals(all(drawn, ".art"), Vector.empty)
    TestArt.listingWholeCatalog()
    val withArt = worldWithForcesAndPawns()
    val copy = withArt.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, ".art-crop, .art").foreach(_.remove())
    assertEquals(copy.outerHTML, drawn.outerHTML)
