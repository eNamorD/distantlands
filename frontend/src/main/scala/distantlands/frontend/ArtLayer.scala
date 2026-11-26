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
  /** Lays `key`'s image over `box`, when `GameArt` has a URL for it. */
  def attach(box: dom.Element, key: Option[ArtKey]): Unit =
    key.flatMap(GameArt.url).filterNot(GameArt.hasFailed).foreach { url =>
      val image = dom.document.createElement("img")
        .asInstanceOf[dom.html.Image]
      image.className = "art"
      image.alt = ""
      image.setAttribute("aria-hidden", "true")
      image.setAttribute("decoding", "async")
      image.setAttribute("loading", "lazy")
      // The table is rebuilt on every poll, which builds a fresh image.
      // The browser already holds one it has loaded, so the fresh box is
      // ready at once and a redraw never flashes the drawn face.
      if GameArt.hasLoaded(url) then box.classList.add("art-ready")
      image.addEventListener("load", (_: dom.Event) =>
        GameArt.markLoaded(url)
        box.classList.add("art-ready"))
      image.addEventListener("error", (_: dom.Event) =>
        GameArt.markFailed(url)
        box.classList.remove("art-ready")
        image.remove())
      image.src = url
      box.appendChild(image)
    }
