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
