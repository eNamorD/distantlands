package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** `ArtLayer.attach` is the one way a surface swaps in art (game art
  * design, "`ArtLayer`"). jsdom loads no image, so the suite dispatches
  * the image's `load` and `error` itself. */
class ArtLayerSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = ArtCatalog.cardFace("Alchemist")
  private val alchemistUrl = "/art/cards/Alchemist%20(NF).png"

  /** A box holding one printed child, as a drawn face does. */
  private def drawnBox(): dom.Element =
    val box = dom.document.createElement("span")
    val printed = dom.document.createElement("span")
    printed.setAttribute("class", "printed")
    printed.textContent = "Alchemist"
    box.appendChild(printed)
    box

  test("with no URL for the key nothing is attached"):
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    ArtLayer.attach(box, None)
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))

  test("the image is decoration that fills the box and loads lazily"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    val image = one(box, "img.art")
    assertEquals(image.getAttribute("src"), alchemistUrl)
    assertEquals(image.getAttribute("alt"), "")
    assertEquals(image.getAttribute("aria-hidden"), "true")
    assertEquals(image.getAttribute("decoding"), "async")
    assertEquals(image.getAttribute("loading"), "lazy")
    assert(!box.classList.contains("art-ready"))
    assertEquals(one(box, ".printed").textContent, "Alchemist")

  test("a load marks the box ready and keeps the drawn content"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("load"))
    assert(box.classList.contains("art-ready"))
    assertEquals(one(box, ".printed").textContent, "Alchemist")
    assert(GameArt.hasLoaded(alchemistUrl))

  test("an error removes the image and the box stays drawn"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("error"))
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))
    assert(GameArt.hasFailed(alchemistUrl))

  test("a URL remembered as loaded marks a fresh box ready at once"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    assert(box.classList.contains("art-ready"))
    assertEquals(one(box, "img.art").getAttribute("src"), alchemistUrl)

  test("a URL remembered as failed makes no image"):
    TestArt.listing(alchemist.get.path)
    GameArt.markFailed(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))

  test("an error after a remembered load takes the ready mark back"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("error"))
    assert(!box.classList.contains("art-ready"))
    assertEquals(all(box, "img"), Vector.empty)

  test("a load marks the element named ready, not the image's box"):
    TestArt.listing(alchemist.get.path)
    val row = dom.document.createElement("article")
    val face = dom.document.createElement("div")
    row.appendChild(face)
    ArtLayer.attach(face, alchemist, Some(row))
    one(face, "img.art").dispatchEvent(new dom.Event("load"))
    assert(row.classList.contains("art-ready"))
    assert(!face.classList.contains("art-ready"))

  test("a remembered load marks the named element at once, and an error " +
      "takes the mark back from it"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val row = dom.document.createElement("article")
    val face = dom.document.createElement("div")
    row.appendChild(face)
    ArtLayer.attach(face, alchemist, Some(row))
    assert(row.classList.contains("art-ready"))
    one(face, "img.art").dispatchEvent(new dom.Event("error"))
    assert(!row.classList.contains("art-ready"))
    assertEquals(all(face, "img"), Vector.empty)
