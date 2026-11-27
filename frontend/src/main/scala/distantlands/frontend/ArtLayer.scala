package distantlands.frontend

import distantlands.protocol.art.{ArtKey, DieCrop}
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
      readyAtOnce(url, ready.getOrElse(box), held = image.complete)
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

  /** Lays `key`'s image in the SVG group `group`, in a box `width` by
    * `height` centred on (`x`, `y`) in the group's own units and turned
    * `turn` degrees clockwise about that centre, over the mark the group
    * draws there. `mark` hides when the image has loaded. */
  def attachMark(group: dom.Element, mark: dom.Element, key: Option[ArtKey],
      x: Double, y: Double, width: Int, height: Int, turn: Double): Unit =
    wanted(key).foreach { url =>
      val image = svgImage(url, mark)
      image.setAttribute("x", (x - width / 2.0).toString)
      image.setAttribute("y", (y - height / 2.0).toString)
      image.setAttribute("transform", f"rotate($turn%.2f $x%.2f $y%.2f)")
      // As an inline style, in the group's units: the stylesheet gives
      // every art image the whole of its box, and it outranks an SVG
      // size attribute.
      image.setAttribute("style",
        s"width: ${width}px; height: ${height}px;")
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
    // An SVG image cannot say whether the browser holds it, so a
    // remembered one is taken as held: a glyph is small, and a frame of
    // an empty glyph is the lesser fault beside a flash of its sprite.
    readyAtOnce(url, marked, held = true)
    image

  /** Lays a die face over `box`: a square holding the die's sheet, sized
    * and offset so only the face's crop shows (game art design, "Dice").
    * The square is centred in the box and as tall as it; the box keeps
    * its size. A sheet that fails takes the square with it. */
  def attachCrop(box: dom.Element, crop: Option[DieCrop]): Unit =
    for face <- crop; url <- wanted(Some(face.sheet)) do
      val frame = dom.document.createElement("span")
      frame.setAttribute("class", "art-crop")
      frame.setAttribute("aria-hidden", "true")
      val image = dom.document.createElement("img")
        .asInstanceOf[dom.html.Image]
      image.className = "art"
      image.alt = ""
      image.setAttribute("decoding", "async")
      def percent(value: Double): String =
        f"${value * 100}%.4f".reverse.dropWhile(_ == '0')
          .dropWhile(_ == '.').reverse + "%"
      image.style.width = percent(face.sheetW.toDouble / face.w)
      image.style.height = percent(face.sheetH.toDouble / face.h)
      image.style.left = percent(-face.x.toDouble / face.w)
      image.style.top = percent(-face.y.toDouble / face.h)
      watch(image, url, box, remove = frame)
      image.src = url
      readyAtOnce(url, box, held = image.complete)
      frame.appendChild(image)
      box.appendChild(frame)

  /** The URL to ask for: `GameArt` has one for `key`, and it has not
    * failed on this page. */
  private def wanted(key: Option[ArtKey]): Option[String] =
    key.flatMap(GameArt.url).filterNot(GameArt.hasFailed)

  /** The table is rebuilt on every poll, which builds a fresh image. When
    * `url` loaded before on this page and the browser still `held` it for
    * the fresh image, the box is ready at once, so a redraw never flashes
    * the drawn face. When the browser must fetch or decode it again, the
    * box waits for the image's `load`, so it never shows neither face nor
    * art. */
  private def readyAtOnce(url: String, marked: dom.Element, held: Boolean)
      : Unit =
    if held && GameArt.hasLoaded(url) then marked.classList.add("art-ready")

  /** Marks `marked` ready when `image` loads, and takes the image out
    * when it fails. */
  private def watch(image: dom.Element, url: String, marked: dom.Element,
      remove: dom.Element | Null = null): Unit =
    val gone = if remove == null then image else remove
    image.addEventListener("load", (_: dom.Event) =>
      GameArt.markLoaded(url)
      marked.classList.add("art-ready"))
    image.addEventListener("error", (_: dom.Event) =>
      GameArt.markFailed(url)
      marked.classList.remove("art-ready")
      gone.asInstanceOf[dom.Node].parentNode match
        case null => ()
        case parent => parent.removeChild(gone))
