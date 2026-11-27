package distantlands.frontend

import org.scalajs.dom
import scala.scalajs.js
import ServerUiSupport.{button, element}

/** A Vision waiting in the hand points at the spot a reveal would put it
  * in. A player who drew a Vision reaches for the card, not for a frame on
  * a board, so while the parked decision lights a revealed Vision spot,
  * hovering the card lights the spot too, and a click brings the spot into
  * view and lights it for a moment. Reading the card moves to an inspect
  * button beside it, the way a card on offer reads (`BoardOptions.control`).
  */
private[frontend] object VisionSpotPointer:
  /** How long a click keeps the spot lit, in milliseconds. */
  private val LitFor = 1500

  /** The keys of the revealed Vision spots the routed decision offers. */
  def spots(routed: ParkedDecision.Routed): Vector[String] =
    routed.surface.toVector.flatMap {
      case board: ParkedDecision.Surface.BoardDecision =>
        board.query.offeredOptions
          .filter(option => option.kind == "revealed-vision" &&
            option.state != "unavailable")
          .map(BoardOptionViews.key)
      case _ => Vector.empty
    }

  /** `card` as the hand draws it: a plain face, or for a Vision with a lit
    * spot, a face that points at the spot and an inspect button. */
  def face(card: CardDetails, spots: Vector[String]): dom.Element =
    val face = CardFace.render(card)
    if card.cardKind != "vision" || card.hidden || spots.isEmpty then face
    else
      face.onclick = _ => point(spots)
      face.addEventListener("mouseenter", (_: dom.Event) =>
        spots.foreach(BoardOptions.highlight(_, on = true)))
      face.addEventListener("mouseleave", (_: dom.Event) =>
        spots.foreach(BoardOptions.highlight(_, on = false)))
      val box = element("span", "board-option-card")
      box.appendChild(face)
      val reader = button("i", "board-option-inspect")
      reader.setAttribute("aria-label", s"Inspect ${card.name}")
      reader.addEventListener("click", (event: dom.Event) => {
        event.stopPropagation()
        CardInspection.open(card, reader)
      })
      box.appendChild(reader)
      box

  private def point(spots: Vector[String]): Unit =
    spots.foreach(BoardOptions.highlight(_, on = true))
    spots.iterator.flatMap(key => Option(dom.document.querySelector(
        s"""[data-option-ref="$key"]"""))).nextOption()
      .map(_.asInstanceOf[js.Dynamic])
      .filterNot(spot => js.isUndefined(spot.scrollIntoView))
      .foreach(_.scrollIntoView(js.Dynamic.literal(block = "nearest",
        inline = "nearest")))
    dom.window.setTimeout(() =>
      spots.foreach(BoardOptions.highlight(_, on = false)), LitFor)
