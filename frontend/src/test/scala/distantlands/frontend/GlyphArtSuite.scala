package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** Symbols in text and tokens in play with game art (game art design,
  * "The surfaces"; implementation rulings R22, R23): a glyph keeps its
  * `svg` and holds the image inside it, and each glyph falls back on its
  * own. */
class GlyphArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** The `href` of each image directly in the glyph. */
  private def images(glyph: dom.Element): Vector[String] =
    all(glyph, ":scope > image.art").map(_.getAttribute("href"))

  test("a glyph with a symbol holds its image inside the same svg"):
    TestArt.listingWholeCatalog()
    val glyph = RulesTextRenderer.glyph("favor")
    assertEquals(glyph.tagName.toLowerCase, "svg")
    assertEquals(glyph.getAttribute("class"), "token-glyph token-favor")
    assertEquals(glyph.getAttribute("role"), "img")
    assertEquals(glyph.getAttribute("aria-label"), "favor")
    assertEquals(one(glyph, ":scope > use").getAttribute("href"),
      "#token-favor")
    assertEquals(images(glyph), Vector("/art/symbols/symbol-favor.png"))
    val image = one(glyph, "image.art")
    assertEquals(image.getAttribute("width"), "100%")
    assertEquals(image.getAttribute("height"), "100%")
    assertEquals(image.getAttribute("preserveAspectRatio"), "xMidYMid meet")
    assertEquals(image.getAttribute("aria-hidden"), "true")

  test("each symbol's glyph asks for its own file"):
    TestArt.listingWholeCatalog()
    TokenSprite.ids.filterNot(_ == "warband").foreach(token =>
      assertEquals(images(RulesTextRenderer.glyph(token)),
        Vector(GameArt.url(ArtCatalog.symbol(token).get).get), token))

  test("the warband glyph and an unknown token hold no image"):
    TestArt.listingWholeCatalog()
    assertEquals(all(RulesTextRenderer.glyph("warband"), "image"),
      Vector.empty)
    assertEquals(all(RulesTextRenderer.glyph("unknown"), "image"),
      Vector.empty)

  test("a load marks the glyph ready; an error removes the image and " +
      "leaves the sprite"):
    TestArt.listingWholeCatalog()
    val loaded = RulesTextRenderer.glyph("secret")
    one(loaded, "image.art").dispatchEvent(new dom.Event("load"))
    assert(loaded.classList.contains("art-ready"))
    assertEquals(all(loaded, ":scope > use").size, 1)
    val failed = RulesTextRenderer.glyph("skull")
    one(failed, "image.art").dispatchEvent(new dom.Event("error"))
    assert(!failed.classList.contains("art-ready"))
    assertEquals(all(failed, "image"), Vector.empty)
    assertEquals(all(failed, ":scope > use").size, 1)
    // The failed symbol is not asked for again; its neighbour still is.
    assertEquals(all(RulesTextRenderer.glyph("skull"), "image"),
      Vector.empty)
    assert(RulesTextRenderer.glyph("secret").classList.contains("art-ready"))

  test("with art off, and before the manifest arrives, a glyph is the " +
      "svg it is today"):
    val drawn = RulesTextRenderer.glyph("favor").outerHTML
    assert(!drawn.contains("image"))
    TestArt.listingWholeCatalog()
    GameArt.setEnabled(false)
    assertEquals(RulesTextRenderer.glyph("favor").outerHTML, drawn)
    assertEquals(RulesTextRenderer.piece("favor").outerHTML, drawn)

  test("rules text draws each token with its symbol, one image a glyph"):
    TestArt.listingWholeCatalog()
    val power = RulesTextRenderer.power(
      "Gain [favor] [favor] from the [suit-order] bank.")
    assertEquals(all(power, "image.art").map(_.getAttribute("href")),
      Vector("/art/symbols/symbol-favor.png", "/art/symbols/symbol-favor.png",
        "/art/symbols/symbol-suit-order.png"))

  test("a drawn card's adviser-only and site-only banners hold their " +
      "symbols, and a tower holds none (R32)"):
    TestArt.listingWholeCatalog()
    def mark(card: CardDetails): dom.Element =
      PowerMarks.restriction(card).get
    assertEquals(images(mark(CardDetails("a", "denizen", "A",
      restrictions = Some("adviser-only")))),
      Vector("/art/symbols/symbol-restrictionperson.png"))
    assertEquals(images(mark(CardDetails("s", "denizen", "S",
      restrictions = Some("site-only")))),
      Vector("/art/symbols/symbol-restrictiontree.png"))
    assertEquals(all(mark(CardDetails("e", "edifice", "E",
      restrictions = Some("locked"), side = Some("intact"))), "image"),
      Vector.empty)

  test("a token lying in play holds the token's picture, and one with " +
      "no picture of its own its symbol (R23)"):
    TestArt.listingWholeCatalog()
    assertEquals(images(RulesTextRenderer.piece("favor")),
      Vector("/art/tts/tokens/favor.png"))
    assertEquals(images(RulesTextRenderer.piece("secret")),
      Vector("/art/tts/tokens/secret.png"))
    assertEquals(images(RulesTextRenderer.piece("favor-burnt")),
      Vector("/art/symbols/symbol-favorb.png"))
    assertEquals(RulesTextRenderer.piece("favor").getAttribute("class"),
      "token-glyph token-favor")

  test("favor on a card and on a banner is the token; favor in rules " +
      "text and in a Forge price is the symbol"):
    TestArt.listingWholeCatalog()
    val card = CardFace.render(CardDetails("x", "denizen", "Old Oak",
      suit = Some("order"), orientation = Some("face-up"), favor = 2,
      secrets = 1))
    assertEquals(all(card, ".card-tokens image.art")
      .map(_.getAttribute("href")), Vector("/art/tts/tokens/favor.png",
      "/art/tts/tokens/secret.png"))
    assertEquals(all(card, ".card-header image.art")
      .map(_.getAttribute("href")),
      Vector("/art/symbols/symbol-suit-order.png"))
    val banner = BannerFace.render(BannerState("peoples-favor", "mob", None,
      3), None)
    assertEquals(all(banner, ".card-tokens image.art")
      .map(_.getAttribute("href")), Vector("/art/tts/tokens/favor.png"))
    val site = ServerUiSupport.siteDetails(TestSites.quietSite("forge",
      "Forge").copy(forgeCost = Some(ForgeCost(1, 1))))
    assertEquals(all(site, ".site-requirement image.art")
      .map(_.getAttribute("href")), Vector("/art/symbols/symbol-favor.png",
      "/art/symbols/symbol-secret.png"))
