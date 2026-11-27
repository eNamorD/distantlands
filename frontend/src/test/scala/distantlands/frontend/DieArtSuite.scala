package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.projection.{Die, DieFace}
import org.scalajs.dom

/** A rolled face with game art (game art design, "Dice"; implementation
  * ruling R25): the chip shows a square crop of its die's sheet and keeps
  * its size, its label and, underneath, its glyphs. */
class DieArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private def style(node: dom.Element): dom.CSSStyleDeclaration =
    node.asInstanceOf[dom.html.Element].style

  test("a rolled face holds its crop of the die's sheet"):
    TestArt.listingWholeCatalog()
    val chip = DieFaceChip.face("one-sword", Some(Die.Attack))
    val image = one(chip, ":scope > .art-crop > img.art")
    assertEquals(image.getAttribute("src"),
      "/art/dice/attack.png")
    // The sheet is 2048 wide and the face 638, at (1392, 702).
    assertEquals(style(image).width, "321.0031%")
    assertEquals(style(image).height, "321.0031%")
    assertEquals(style(image).left, "-218.1818%")
    assertEquals(style(image).top, "-110.0313%")
    assertEquals(one(chip, ".art-crop").getAttribute("aria-hidden"), "true")

  test("each face asks for its own die's sheet"):
    TestArt.listingWholeCatalog()
    DieFace.values.foreach { face =>
      val chip = DieFaceChip.face(face.key, Some(face.die))
      assertEquals(all(chip, ".art-crop > img.art").map(_.getAttribute("src")),
        Vector(s"/art/dice/${face.die.key}.png"),
        face.key)
    }

  test("a face keeps its glyphs, its label and its classes under the crop"):
    TestArt.listingWholeCatalog()
    val chip = DieFaceChip.face("two-swords-skull", Some(Die.Attack))
    assertEquals(chip.getAttribute("class"),
      "die-face die-face-two-swords-skull die-face-attack")
    assertEquals(chip.getAttribute("aria-label"), "two swords and a skull")
    assertEquals(all(chip, ":scope > .token-glyph").size, 3)
    val doubler = DieFaceChip.face("doubler", Some(Die.Defense))
    assert(doubler.textContent.contains("×2"))

  test("an unknown face, and any face with art off, holds no crop"):
    TestArt.listingWholeCatalog()
    assertEquals(all(DieFaceChip.face("mystery"), ".art-crop"), Vector.empty)
    GameArt.setEnabled(false)
    assertEquals(all(DieFaceChip.face("one-sword", Some(Die.Attack)),
      ".art-crop, img"), Vector.empty)

  test("a load marks the chip ready; an error removes the crop"):
    TestArt.listingWholeCatalog()
    val loaded = DieFaceChip.face("blank", Some(Die.Defense))
    one(loaded, ".art-crop > img").dispatchEvent(new dom.Event("load"))
    assert(loaded.classList.contains("art-ready"))
    GameArt.reset()
    TestArt.listingWholeCatalog()
    val failed = DieFaceChip.face("one-shield", Some(Die.Defense))
    one(failed, ".art-crop > img").dispatchEvent(new dom.Event("error"))
    assert(!failed.classList.contains("art-ready"))
    assertEquals(all(failed, ".art-crop"), Vector.empty)
    assertEquals(all(failed, ":scope > .token-glyph").size, 1)

  /** A die chip in the layout-invariance check (implementation ruling
    * R36: no `load` is dispatched; the ready state is the stylesheet
    * pins' and the browser check's). */
  test("a roll drawn with art and without is the same markup once the " +
      "crops are removed"):
    val faces = Vector(DieFace.TwoSwordsSkull, DieFace.OneSword)
    val drawn = DieFaceChip.roll(faces)
    TestArt.listingWholeCatalog()
    val withArt = DieFaceChip.roll(faces)
    assertEquals(all(withArt, ".art-crop").size, 2)
    val copy = withArt.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, ".art-crop, .art").foreach(_.remove())
    assertEquals(copy.outerHTML, drawn.outerHTML)

