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
    val marked = ready.getOrElse(box)
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
      if GameArt.hasLoaded(url) then marked.classList.add("art-ready")
      image.addEventListener("load", (_: dom.Event) =>
        GameArt.markLoaded(url)
        marked.classList.add("art-ready"))
      image.addEventListener("error", (_: dom.Event) =>
        GameArt.markFailed(url)
        marked.classList.remove("art-ready")
        image.remove())
      image.src = url
      box.appendChild(image)
    }
