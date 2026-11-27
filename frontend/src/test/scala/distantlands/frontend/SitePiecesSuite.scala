package distantlands.frontend

import Dom.{all, one}
import TestBoardOptions.*
import TestSites.*
import distantlands.model.PlayerColor
import org.scalajs.dom

/** The pieces on a site's card (game art rulings R48 to R51): a row of
  * pawns, each its piece over its player's name, and a row of warbands,
  * the ruler's piece and a count, each row where `MatLayout` places it on
  * the card. The markup is the same with art on and off. */
class SitePiecesSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val colours = Vector(PlayerColor.Red, PlayerColor.Blue,
    PlayerColor.Yellow, PlayerColor.Purple, PlayerColor.Black,
    PlayerColor.White)
  private val sixPlayers = colours.map(colour => GamePlayer(colour.key,
    colour.key.capitalize, "Exile", colour))
  private val row = MatLayout.siteRow(1, 0)

  /** The Provinces' first site, held by `forces`, with the pawns of
    * `standing` on it, and `options` to choose from. */
  private def siteWithPieces(standing: Vector[String],
      forces: Option[SiteForces] = None,
      options: BoardOptions = BoardOptions.none): dom.Element =
    val value = TestProjections.actingProjection(players = sixPlayers,
      world = Vector(GameRegion("cradle", Vector.empty),
        GameRegion("provinces", Vector(quietSite("keep", "Mines").copy(
          forces = forces))))).copy(pawnLocations =
      standing.map(GamePawn(_, "keep")))
    one(WorldBoardRenderer.world(value, options), "article.site")

  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  test("a site's pawns stand in the row MatLayout places on its card, " +
      "each its piece over its player's name"):
    val site = siteWithPieces(Vector("red"))
    val pawns = one(site, ":scope > .site-pawns")
    assertEquals(box(pawns), MatLayout.sitePawnRow(1, 0).percentOf(row))
    val pawn = one(pawns, ":scope > .pawn")
    assertEquals(all(pawn, ":scope > *").map(_.getAttribute("class")),
      Vector("pawn-piece player-red", "pawn-name"))
    assertEquals(one(pawn, ".pawn-piece").getAttribute("aria-hidden"), "true")
    assertEquals(one(pawn, ".pawn-piece").textContent, "")
    assertEquals(pawn.textContent, "Red")
    assertEquals(pawn.getAttribute(HoverTip.Attribute), "Red")

  /** R48: the row has six places; a pawn takes one from the left, in the
    * order the projection gives, and the markup of one pawn is the same
    * among six as alone. */
  test("six pawns stand in one row, and one pawn is drawn the same " +
      "alone as among six"):
    val alone = one(siteWithPieces(Vector("red")), ".pawn").outerHTML
    val six = all(siteWithPieces(colours.map(_.key)), ".site-pawns > .pawn")
    assertEquals(six.map(_.textContent),
      Vector("Red", "Blue", "Yellow", "Purple", "Black", "White"))
    assertEquals(six.head.outerHTML, alone)
    assertEquals(MatLayout.PawnsPerSite, 6)

  test("a site's warbands stand in their row: the ruler's piece and " +
      "how many, named in words"):
    val site = siteWithPieces(Vector.empty, Some(SiteForces.Exile(2, "red",
      PlayerColor.Red, "Red Exile")))
    val warbands = one(site, ":scope > .site-warbands")
    assertEquals(box(warbands), MatLayout.siteWarbandRow(1, 0).percentOf(row))
    val force = one(warbands, ":scope > .site-force")
    assertEquals(force.getAttribute("class"), "site-force force-red")
    assertEquals(force.getAttribute("role"), "img")
    assertEquals(force.getAttribute("aria-label"), "Red Exile x2")
    assertEquals(force.getAttribute(HoverTip.Attribute), "Red Exile x2")
    assertEquals(force.getAttribute("data-ruler-kind"), "player")
    assertEquals(force.getAttribute("data-ruler-player-id"), "red")
    assertEquals(all(force, ":scope > *").map(_.getAttribute("class")),
      Vector("token-glyph token-warband", "site-force-count"))
    assertEquals(all(force, ":scope > *").map(_.getAttribute("aria-hidden")),
      Vector("true", "true"))
    assertEquals(one(force, ".site-force-count").textContent, "x2")

  test("the Empire's and the bandits' warbands carry their kind and " +
      "their colour class"):
    Vector(SiteForces.Imperial(10, "Imperial Warbands") ->
        ("empire", "force-empire", "Imperial Warbands x10"),
      SiteForces.Bandit(3, "Bandit Warbands") ->
        ("bandit", "force-bandit", "Bandit Warbands x3")).foreach {
      case (forces, (kind, cssClass, words)) =>
        val force = one(siteWithPieces(Vector.empty, Some(forces)),
          ".site-force")
        assertEquals(force.getAttribute("data-ruler-kind"), kind)
        assert(force.classList.contains(cssClass))
        assertEquals(force.getAttribute("aria-label"), words)
        assert(!force.hasAttribute("data-ruler-player-id"))
    }

  /** R49: the row is there whether or not the site is held, so the
    * site's shape does not depend on it. */
  test("a site no one holds keeps its warband row, empty, and a site " +
      "no one stands on its pawn row"):
    val site = siteWithPieces(Vector.empty)
    assertEquals(all(site, ":scope > .site-warbands").size, 1)
    assertEquals(all(site, ".site-warbands > *"), Vector.empty)
    assertEquals(all(site, ":scope > .site-pawns").size, 1)
    assertEquals(all(site, ".site-pawns > *"), Vector.empty)

  test("with art the pieces hold their pictures, and the site is the " +
      "same markup once the art layer is removed"):
    val forces = Some(SiteForces.Bandit(1, "Bandit Warbands"))
    val drawn = siteWithPieces(Vector("red", "black"), forces)
    assertEquals(all(drawn, "img, image"), Vector.empty)
    TestArt.listingWholeCatalog()
    val withArt = siteWithPieces(Vector("red", "black"), forces)
    assertEquals(all(withArt, ".pawn-piece > img.art")
      .map(_.getAttribute("src")), Vector("/art/tts/pieces/pawn-red.png",
      "/art/tts/pieces/pawn-black.png"))
    assertEquals(all(withArt, ".site-force image.art")
      .map(_.getAttribute("href")), Vector("/art/tts/tokens/bandit.png"))
    val piece = all(withArt, ".pawn-piece").head
    one(piece, "img.art").dispatchEvent(new dom.Event("load"))
    assert(piece.classList.contains("art-ready"))
    val copy = siteWithPieces(Vector("red", "black"), forces)
    all(copy, ".art-crop, .art").foreach(_.remove())
    assertEquals(copy.outerHTML, drawn.outerHTML)

  /** R51: a decision that offers a player marks the whole pawn, piece
    * and name, and a click on it picks the player. */
  test("an offered pawn is marked as a whole and picks its player"):
    val offered = recording(available(option("player", "red")))
    val site = siteWithPieces(Vector("red", "blue"), options = offered.options)
    val pawns = all(site, ".site-pawns > .pawn")
    assert(pawns(0).classList.contains("board-option"))
    assert(!pawns(1).classList.contains("board-option"))
    assert(!one(pawns(0), ".pawn-piece").classList.contains("board-option"))
    pawns(0).asInstanceOf[dom.html.Element].click()
    assertEquals(offered.picks.map(_.id), Vector("red"))
