package distantlands.frontend

import distantlands.protocol.art.ArtKey
import org.scalajs.dom

/** The one way a surface swaps licensed art in for what it draws (game
  * art design, "`ArtLayer`").
  *
  * The image is laid over the box and never sizes it. Until it loads the
  * box shows its drawn content; once it has, the box is `art-ready` and
  * the stylesheet hides the box's `printed` content with opacity, which
  * keeps the content's space and keeps it in the accessibility tree. An
  * image that fails is removed, and the box stays drawn.
  */
private[frontend] object ArtLayer:
  /** Lays `key`'s image over `box`, when `GameArt` has a URL for it.
    * `ready` is the box whose printed content the image replaces, when it
    * is not the box the image lies in: a site's words lie in its row,
    * beside the face that holds the image. */
  def attach(box: dom.Element, key: Option[ArtKey],
      ready: Option[dom.Element] = None): Unit =
    wanted(key).foreach { url =>
      val image = dom.document.createElement("img")
        .asInstanceOf[dom.html.Image]
      image.className = "art"
      image.alt = ""
      image.setAttribute("aria-hidden", "true")
      image.setAttribute("decoding", "async")
      image.setAttribute("loading", "lazy")
      watch(image, url, ready.getOrElse(box))
      image.src = url
      box.appendChild(image)
    }

  /** Lays `key`'s image inside the glyph `svg`, fitted to its square box
    * (implementation ruling R22). The sprite's `<use>` is what the glyph
    * draws itself; it hides when the image has loaded and stays when it
    * fails. */
  def attachGlyph(glyph: dom.Element, key: Option[ArtKey]): Unit =
    wanted(key).foreach { url =>
      val image = svgImage(url, glyph)
      image.setAttribute("width", "100%")
      image.setAttribute("height", "100%")
      glyph.appendChild(image)
    }

  /** Lays `key`'s image in the SVG group `group`, in a square of `size`
    * centred on (`x`, `y`) in the group's own units, beside the mark the
    * group draws there. `mark` hides when the image has loaded. */
  def attachMark(group: dom.Element, mark: dom.Element, key: Option[ArtKey],
      x: Double, y: Double, size: Double): Unit =
    wanted(key).foreach { url =>
      val image = svgImage(url, mark)
      image.setAttribute("x", (x - size / 2).toString)
      image.setAttribute("y", (y - size / 2).toString)
      image.setAttribute("width", size.toString)
      image.setAttribute("height", size.toString)
      group.appendChild(image)
    }

  /** An SVG image of `url`, fitted to the room it is given, that marks
    * `marked` ready when it loads. */
  private def svgImage(url: String, marked: dom.Element): dom.Element =
    val image = ServerUiSupport.svgElement("image")
    image.setAttribute("class", "art")
    image.setAttribute("preserveAspectRatio", "xMidYMid meet")
    image.setAttribute("aria-hidden", "true")
    watch(image, url, marked)
    image.setAttribute("href", url)
    image

  /** The URL to ask for: `GameArt` has one for `key`, and it has not
    * failed on this page. */
  private def wanted(key: Option[ArtKey]): Option[String] =
    key.flatMap(GameArt.url).filterNot(GameArt.hasFailed)

  /** Marks `marked` ready when `image` loads, and takes the image out
    * when it fails. The table is rebuilt on every poll, which builds a
    * fresh image; the browser already holds one it has loaded, so the
    * fresh box is ready at once and a redraw never flashes the drawn
    * face. */
  private def watch(image: dom.Element, url: String, marked: dom.Element)
      : Unit =
    if GameArt.hasLoaded(url) then marked.classList.add("art-ready")
    image.addEventListener("load", (_: dom.Event) =>
      GameArt.markLoaded(url)
      marked.classList.add("art-ready"))
    image.addEventListener("error", (_: dom.Event) =>
      GameArt.markFailed(url)
      marked.classList.remove("art-ready")
      image.asInstanceOf[dom.Node].parentNode match
        case null => ()
        case parent => parent.removeChild(image))
