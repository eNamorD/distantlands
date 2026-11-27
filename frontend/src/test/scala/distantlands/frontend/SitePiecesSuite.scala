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
    // The ground first, under the piece and the name (R61): a layer of
    // the pawn's own box, for the eye, painted over art alone.
    assertEquals(all(pawn, ":scope > *").map(_.getAttribute("class")),
      Vector("pawn-ground", "pawn-piece player-red", "pawn-name"))
    assertEquals(one(pawn, ".pawn-ground").getAttribute("aria-hidden"),
      "true")
    assertEquals(one(pawn, ".pawn-ground").childNodes.length, 0)
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
    // Five or fewer are drawn singly, side by side (R63): no count.
    assertEquals(all(force, ":scope > *").map(_.getAttribute("class")),
      Vector("token-glyph token-warband", "token-glyph token-warband"))
    assertEquals(all(force, ":scope > *").map(_.getAttribute("aria-hidden")),
      Vector("true", "true"))

  /** R63: the number that decides is the force's warbands on the site.
    * One to five are that many pictures; six or more are one picture and
    * the count. The name and the note always state the number. */
  test("up to five warbands are drawn one a picture, more as one " +
      "picture and a count"):
    def forceOf(count: Int): dom.Element =
      one(siteWithPieces(Vector.empty, Some(SiteForces.Bandit(count,
        "Bandit Warbands"))), ".site-force")
    (1 to 5).foreach { count =>
      val force = forceOf(count)
      assertEquals(all(force, ":scope > .token-warband").size, count)
      assertEquals(all(force, ".site-force-count"), Vector.empty)
      assertEquals(force.getAttribute("aria-label"),
        s"Bandit Warbands x$count")
      assertEquals(force.getAttribute(HoverTip.Attribute),
        s"Bandit Warbands x$count")
    }
    Vector(6, 12).foreach { count =>
      val force = forceOf(count)
      assertEquals(all(force, ":scope > *").map(_.getAttribute("class")),
        Vector("token-glyph token-warband", "site-force-count"))
      assertEquals(one(force, ".site-force-count").textContent, s"x$count")
      assertEquals(one(force, ".site-force-count")
        .getAttribute("aria-hidden"), "true")
      assertEquals(force.getAttribute("aria-label"),
        s"Bandit Warbands x$count")
    }

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
      .map(_.getAttribute("src")), Vector("/art/pieces/pawn-red.png",
      "/art/pieces/pawn-black.png"))
    assertEquals(all(withArt, ".site-force image.art")
      .map(_.getAttribute("href")), Vector("/art/tokens/bandit.png"))
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
    // The offered pawn has its ground too (R61).
    assertEquals(all(pawns(0), ":scope > .pawn-ground").size, 1)
    pawns(0).asInstanceOf[dom.html.Element].click()
    assertEquals(offered.picks.map(_.id), Vector("red"))

  /** A pawn stands inside its site's box, and a decision may offer both:
    * a press on the pawn picks the player and does not reach the site. */
  test("an offered pawn on an offered site picks its player alone, by " +
      "pointer and by Enter and Space"):
    def pressed(press: dom.html.Element => Unit): Vector[String] =
      val offered = recording(available(option("player", "red")),
        available(option("site", "keep")))
      val site = siteWithPieces(Vector("red"), options = offered.options)
      assert(site.classList.contains("board-option"))
      press(one(site, ".site-pawns > .pawn").asInstanceOf[dom.html.Element])
      offered.picks.map(pick => s"${pick.kind}:${pick.id}")
    def keyDown(name: String)(pawn: dom.html.Element): Unit =
      pawn.dispatchEvent(new dom.KeyboardEvent("keydown",
        new dom.KeyboardEventInit:
          key = name
          bubbles = true
          cancelable = true))
    assertEquals(pressed(_.click()), Vector("player:red"))
    assertEquals(pressed(keyDown("Enter")), Vector("player:red"))
    assertEquals(pressed(keyDown(" ")), Vector("player:red"))
