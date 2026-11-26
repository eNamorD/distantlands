package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.projection.{GameTracksProjection,
  SitePowerProjection}
import TestSites.*
import org.scalajs.dom

/** The world board with game art (game art design, "The surfaces"): the
  * mat, site cards, banners and the piles' backs. With art on a box holds
  * an `img.art` with the catalog's `src`; with art off it holds none, and
  * the board is the same either way. */
class WorldArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val alchemist = CardDetails("9", "denizen", "Alchemist",
    suit = Some("arcane"), orientation = Some("face-up"))

  /** A site named for a real site card, with a power, forces, a denizen
    * and loose tokens. */
  private def deepWoods: GameSite =
    quietSite("deep-woods", "Deep Woods").copy(defense = 1,
      powers = Vector(SitePowerProjection("action", "Homeland", Some("Rule"),
        id = "deep-woods-power")),
      forces = Some(SiteForces.Bandit(2, "Bandit Warbands")))
      .withDenizens(GameSiteCard("9", "Alchemist", Some(alchemist)))
      .withLooseTokens(1, 1)

  /** A three-region board: a real site, a site no card is named for, an
    * empty slot, decks, discards, banks and both banners. */
  private def boardWithTheWorld(): dom.Element =
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(
        GameRegion("cradle", Vector(deepWoods, quietSite("camp", "Camp")),
          discardCount = 2, discardTopCardKind = Some("denizen")),
        GameRegion("provinces", Vector(quietSite("ford", "Ford")),
          discardCount = 1, discardTopCardKind = Some("vision")),
        GameRegion("hinterland", Vector(quietSite("coast", "Coast")),
          emptySlots = 1))).copy(
      worldDeckCount = 40, worldDeckTopCardKind = Some("denizen"),
      relicDeckCount = 9,
      tracks = Some(GameTracksProjection(round = 3, visionsDrawn = 2,
        usurperLimited = false, limiterRound = 5, firstPlayerId = "red")),
      favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 3)),
      banners = Vector(BannerState("peoples-favor", "mob", None, 1),
        BannerState("darkest-secret", "festival", None, 2)),
      pawnLocations = Vector(GamePawn("red", "deep-woods"))),
      BoardOptions.none)

  /** The `src` of each image directly in `box`. */
  private def images(box: dom.Element): Vector[String] =
    all(box, ":scope > img.art").map(_.getAttribute("src"))

  private def site(board: dom.Element, label: String): dom.Element =
    one(board, s"""article.site[aria-label="$label"]""")

  test("a site named for a site card holds the card's image in its face"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    assertEquals(images(one(row, ":scope > .site-face")),
      Vector("/art/cards/Deep%20Woods%20(NF).png"))
    assertEquals(images(row), Vector.empty)

  test("the site's row is the box that turns ready, and its cards keep " +
      "their own answer"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    one(row, ".site-face > img.art").dispatchEvent(new dom.Event("load"))
    assert(row.classList.contains("art-ready"))
    assert(!one(row, ".site-face").classList.contains("art-ready"))
    assert(all(row, ".card-face").nonEmpty)
    all(row, ".card-face").foreach(card =>
      assert(!card.classList.contains("art-ready")))

  test("a site no card is named for stays drawn"):
    TestArt.listingWholeCatalog()
    assertEquals(all(site(boardWithTheWorld(), "Camp"),
      ".site-face img"), Vector.empty)

  test("what lies on a site in play is not printed, and its power " +
      "buttons stay controls"):
    TestArt.listingWholeCatalog()
    val row = site(boardWithTheWorld(), "Deep Woods")
    Vector(".site-tokens", ".site-pawns", ".site-forces",
      ".site-forces-short", ".site-power").foreach(selector =>
      assert(all(row, s"$selector:not(.printed)").nonEmpty, selector))
    all(row, ".site-power").foreach(control =>
      assert(Option(control.closest(".printed")).isEmpty))
    assertEquals(all(row, ".site-face *:not(img)"), Vector.empty)

  test("a banner holds the image of the face it shows, and keeps what " +
      "it holds over it"):
    TestArt.listingWholeCatalog()
    val board = boardWithTheWorld()
    val favor = one(board, ".banner-peoples-favor")
    assertEquals(images(favor), Vector(
      "/art/cards/Banner%20of%20the%20People's%20Favor%20-%20Mob%20(NF).png"))
    assertEquals(images(one(board, ".banner-darkest-secret")), Vector(
      "/art/cards/Banner%20of%20the%20Darkest%20Secret%20-%20Festival%20(NF).png"))
    assertEquals(all(favor, ".card-tokens:not(.printed)").size, 1)
    assertEquals(all(favor, ".printed").map(_.getAttribute("class")
      .split(" ").head), Vector("banner-side", "card-header"))

  test("a banner on a face the catalog does not know stays drawn"):
    TestArt.listingWholeCatalog()
    assertEquals(all(BannerFace.render(
      BannerState("peoples-favor", "unknown", None, 0), None), "img"),
      Vector.empty)

  test("the board holds the mat's image, directly in the board"):
    TestArt.listingWholeCatalog()
    assertEquals(images(one(boardWithTheWorld(), ".mat-board")),
      Vector("/art/tts/mat/mat.jpg"))

  test("what the mat prints is printed content of the board itself, in " +
      "no box of its own"):
    val board = one(boardWithTheWorld(), ".mat-board")
    val boxes = ".card-face, .card-hover-summary, .site, .deck-card"
    Vector(".region-label", ".deck-caption", ".vision-cell",
      ".visions-drawn-label", ".round-segment", ".round-number",
      ".site-slot-empty").foreach { selector =>
      val found = all(board, selector)
      assert(found.nonEmpty, selector)
      found.foreach { node =>
        assert(node.classList.contains("printed"), selector)
        assertEquals(Option(node.closest(boxes)), None, selector)
      }
    }

  test("what lies on the mat in play is not printed"):
    val board = one(boardWithTheWorld(), ".mat-board")
    Vector(".vision-marker", ".pile-count", ".favor-bank-count",
      ".site-pawns .pawn").foreach { selector =>
      val found = all(board, selector)
      assert(found.nonEmpty, selector)
      found.foreach(node =>
        assertEquals(Option(node.closest(".printed")), None, selector))
    }

  test("a pile on the mat holds the back of its top card's kind, and " +
      "keeps its count"):
    TestArt.listingWholeCatalog()
    val board = one(boardWithTheWorld(), ".mat-board")
    def pile(caption: String): dom.Element =
      all(board, ".deck-figure").find(figure =>
        one(figure, ".deck-caption").textContent == caption).map(figure =>
        one(figure, ".deck-card")).get
    assertEquals(images(pile("Relic deck")),
      Vector("/art/tts/backs/relic-back.jpg"))
    assertEquals(images(pile("World deck")),
      Vector("/art/tts/backs/denizen-back.jpg"))
    assertEquals(all(board, ".deck-figure .deck-card").map(images),
      // The Cradle's discard, the relic deck, the world deck, then the
      // Provinces' discard and the Hinterland's, which is empty.
      Vector(Vector("/art/tts/backs/denizen-back.jpg"),
        Vector("/art/tts/backs/relic-back.jpg"),
        Vector("/art/tts/backs/denizen-back.jpg"),
        Vector("/art/tts/backs/vision-back.jpg"), Vector.empty))
    assertEquals(one(pile("World deck"), ".pile-count").textContent, "×40")

  test("the world deck and the discards lie on their sides, with art or " +
      "without; the relic deck does not"):
    def sideways(board: dom.Element): Vector[Boolean] =
      all(board, ".deck-figure .deck-card")
        .map(_.classList.contains("deck-sideways"))
    val expected = Vector(true, false, true, true, true)
    assertEquals(sideways(boardWithTheWorld()), expected)
    TestArt.listingWholeCatalog()
    assertEquals(sideways(boardWithTheWorld()), expected)

  /** `node`'s markup with every art image taken out. */
  private def withoutArt(node: dom.Element): String =
    val copy = node.cloneNode(true).asInstanceOf[dom.Element]
    // Images in boxes and, since slice 3, inside glyphs.
    all(copy, ".art-crop, .art").foreach(_.remove())
    copy.outerHTML

  test("the world drawn with art and without has the same boxes, classes " +
      "and inline geometry once the images are removed"):
    val drawn = boardWithTheWorld()
    TestArt.listingWholeCatalog()
    val withArt = boardWithTheWorld()
    // The mat, one site, one denizen, two banners, four piles, one pawn
    // and the Visions Drawn marker.
    assertEquals(all(withArt, "img.art").size, 11)
    assertEquals(withoutArt(withArt), drawn.outerHTML)

  test("with art off the world holds no image"):
    TestArt.listingWholeCatalog()
    GameArt.setEnabled(false)
    assertEquals(all(boardWithTheWorld(), "img"), Vector.empty)

  /** A world whose relic deck is empty and whose Cradle discard holds
    * cards of a kind the viewer may not know. */
  private def boardWithAnEmptyDeckAndAHiddenDiscard(): dom.Element =
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(GameRegion("cradle", Vector(deepWoods),
        discardCount = 2, discardTopCardKind = None))).copy(
      worldDeckCount = 40, worldDeckTopCardKind = Some("denizen"),
      relicDeckCount = 0,
      tracks = Some(GameTracksProjection(round = 1, visionsDrawn = 0,
        usurperLimited = false, limiterRound = 5, firstPlayerId = "red"))),
      BoardOptions.none)

  test("an empty pile and a pile whose top card's kind is hidden hold no " +
      "image (R16)"):
    TestArt.listingWholeCatalog()
    val board = one(boardWithAnEmptyDeckAndAHiddenDiscard(), ".mat-board")
    def pile(caption: String): dom.Element =
      all(board, ".deck-figure").find(figure =>
        one(figure, ".deck-caption").textContent == caption).map(figure =>
        one(figure, ".deck-card")).get
    assertEquals(all(pile("Relic deck"), "img"), Vector.empty)
    assert(pile("Relic deck").classList.contains("pile-empty"))
    assertEquals(all(pile("Discard"), "img"), Vector.empty)
    assertEquals(one(pile("Discard"), ".pile-count").textContent, "×2")
    // The world deck beside them, whose top kind is known, has its back.
    assertEquals(images(pile("World deck")),
      Vector("/art/tts/backs/denizen-back.jpg"))

  /** Chronicle Details draws the Atlas's Recent site with the world's own
    * site renderer, inside the table (implementation ruling R17). */
  test("Chronicle Details' Atlas row shows its site's card, and the row " +
      "turns ready"):
    import distantlands.protocol.projection.ChronicleDetailsProjection
    TestArt.listingWholeCatalog()
    val body = ChronicleDetailsContent.render(
      TestProjections.actingProjection().copy(chronicleDetails = Some(
        ChronicleDetailsProjection(20, Vector.empty, Vector.empty,
          Some(quietSite("site:deep-woods", "Deep Woods"))))))
    val row = one(body, ".chronicle-details-atlas-board > article.site")
    val image = one(row, ":scope > .site-face > img.art")
    assertEquals(image.getAttribute("src"),
      "/art/cards/Deep%20Woods%20(NF).png")
    image.dispatchEvent(new dom.Event("load"))
    assert(row.classList.contains("art-ready"))

