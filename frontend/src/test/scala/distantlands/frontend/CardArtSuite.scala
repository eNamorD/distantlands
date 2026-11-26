package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.art.ArtCatalog
import TestSites.*
import org.scalajs.dom

/** Card faces and backs with game art (game art design, "The surfaces"):
  * with art on a box holds an `img.art` with the catalog's `src`, with art
  * off it holds none, and the layout is the same either way. */
class CardArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = CardDetails("9", "denizen", "Alchemist",
    suit = Some("arcane"), orientation = Some("face-up"), favor = 1)
  private val knownRelic = CardDetails("R01", "relic", "Sticky Fire",
    orientation = Some("face-down"), defense = Some(3))
  private val vision = CardDetails("vision:vision-of-faith", "vision",
    "Vision of Faith", orientation = Some("face-up"))
  private def hidden(kind: String): CardDetails = CardDetails("hidden", kind,
    s"Facedown $kind", orientation = Some("face-down"), hidden = true)

  /** The `src` of each image directly in `box`. */
  private def images(box: dom.Element): Vector[String] =
    all(box, ":scope > img.art").map(_.getAttribute("src"))

  test("a face-up card holds its face's image"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(alchemist)),
      Vector("/art/cards/Alchemist%20(NF).png"))

  test("an edifice holds the image of the face it shows"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(CardDetails("E01", "edifice",
      "Hall of Mockery", orientation = Some("face-up"),
      side = Some("ruined")))), Vector("/art/cards/Hall%20of%20Mockery%20(NF).png"))

  test("a card the viewer cannot identify holds its kind's back"):
    TestArt.listingWholeCatalog()
    Vector("denizen", "vision", "relic", "legacy").foreach(kind =>
      assertEquals(images(CardFace.render(hidden(kind))),
        Vector(s"/art/tts/backs/$kind-back.jpg"), kind))

  test("a facedown site relic holds the relic back"):
    TestArt.listingWholeCatalog()
    assertEquals(images(ServerUiSupport.facedownCard("relic")),
      Vector("/art/tts/backs/relic-back.jpg"))

  test("a knowable card holds its back, and its hover summary its face"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(knownRelic)
    assert(node.classList.contains("card-face-knowable"))
    assertEquals(images(node), Vector("/art/tts/backs/relic-back.jpg"))
    assertEquals(images(one(node, ".card-hover-summary")),
      Vector("/art/cards/Sticky%20Fire%20(NF).png"))

  test("a dormant legacy its viewer knows holds its face and keeps its " +
      "Dormant tag"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(CardDetails("L01", "legacy", "Iron Hand",
      suit = Some("order"), orientation = Some("face-down")))
    assertEquals(images(node), Vector("/art/cards/Iron%20Hand%20(NF).png"))
    assertEquals(one(node, ".legacy-dormant").textContent, "Dormant")
    assert(!one(node, ".legacy-dormant").classList.contains("printed"))

  test("a Vision holds its face in the denizen box it keeps"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(vision)
    assert(node.classList.contains("card-face-vision"))
    assertEquals(images(node),
      Vector("/art/cards/Vision%20of%20Faith%20(NF).png"))

  test("a denizen New Foundations did not reprint holds its base file"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(CardDetails("1", "denizen",
      "Wrestlers", suit = Some("order"), orientation = Some("face-up")))),
      Vector("/art/cards/Wrestlers.png"))

  test("a card the catalog does not name stays drawn"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(CardDetails("x", "denizen", "Old Oak",
      suit = Some("order"), orientation = Some("face-up")))
    assertEquals(all(node, "img"), Vector.empty)

  test("a card whose file the folder lacks stays drawn, and its " +
      "neighbour does not"):
    TestArt.listing(ArtCatalog.cardFace("Alchemist").get.path)
    assertEquals(all(CardFace.render(knownRelic), "img"), Vector.empty)
    assertEquals(images(CardFace.render(alchemist)).size, 1)

  test("with art off, and before the manifest arrives, no card holds an " +
      "image"):
    val cards = Vector(alchemist, knownRelic, vision, hidden("denizen"))
    cards.foreach(card =>
      assertEquals(all(CardFace.render(card), "img"), Vector.empty))
    TestArt.listingWholeCatalog()
    GameArt.setEnabled(false)
    cards.foreach(card =>
      assertEquals(all(CardFace.render(card), "img"), Vector.empty))
    assertEquals(all(ServerUiSupport.facedownCard("relic"), "img"),
      Vector.empty)

  test("the image is the box's last child and sits under no printed mark"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(alchemist)
    assertEquals(node.lastElementChild.getAttribute("class"), "art")
    assertEquals(all(node, ".printed img, .card-tokens img"), Vector.empty)
    // What lies on the card in play is still there, unmarked.
    assertEquals(all(node, ".card-tokens:not(.printed)").size, 1)

  test("a loaded image marks only its own box ready"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(knownRelic)
    one(node, ":scope > img.art").dispatchEvent(new dom.Event("load"))
    assert(node.classList.contains("art-ready"))
    assert(!one(node, ".card-hover-summary").classList.contains("art-ready"))

  test("the inspection overlay shows a card's face image, and a hidden " +
      "card's back"):
    TestArt.listingWholeCatalog()
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      browser.mount.appendChild(origin)
      CardInspection.open(knownRelic, origin)
      assertEquals(images(one(browser.mount, ".card-overlay-body > .card-face")),
        Vector("/art/cards/Sticky%20Fire%20(NF).png"))
      // The rules beside the image are not printed and stay.
      assertEquals(all(browser.mount,
        ".card-overlay-details .printed").size, 0)
      CardInspection.open(hidden("relic"), origin)
      assertEquals(images(one(browser.mount, ".card-overlay-body > .card-face")),
        Vector("/art/tts/backs/relic-back.jpg"))
    finally
      shell.dispose()
      browser.close()

  /** A board with a face-up denizen, a known and an unknown site relic,
    * and a player to see it. */
  private def boardWithCards(): dom.Element =
    val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
    val woods = quietSite("woods", "Woods").withDenizens(
      GameSiteCard("9", "Alchemist", Some(alchemist)))
      .withRelicCapacity(2).withRelics(2, knownRelic)
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(GameRegion("cradle", Vector(woods)))),
      BoardOptions.none)

  /** `node`'s markup with every art image taken out. */
  private def withoutArt(node: dom.Element): String =
    val copy = node.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, "img.art").foreach(_.remove())
    copy.outerHTML

  test("a board drawn with art and without has the same boxes, classes " +
      "and inline geometry once the images are removed"):
    val drawn = boardWithCards()
    TestArt.listingWholeCatalog()
    val withArt = boardWithCards()
    assert(all(withArt, "img.art").size >= 2)
    assertEquals(withoutArt(withArt), drawn.outerHTML)
