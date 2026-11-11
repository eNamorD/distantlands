package distantlands.frontend

import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** The six foundation setup cards as a read-only strip (Foundations design,
  * slice 1): one row a card, its numeral, the name of the face in use and,
  * on an altered card, the word. The table's strip stands in the shared
  * bank, and a click on a card reads its face's rules in the overlay; the
  * Chronicle page lists the same six with no rules to read.
  */
private[frontend] object FoundationStrip:
  /** One card as the strip draws it. `rules` is the text of the face in
    * use, which a click reads; the Chronicle page carries none. */
  final case class Card(number: Int, altered: Boolean, name: String,
      rules: Option[String])

  private val Numerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** "IV" for 4; a number no card prints reads as its digits. */
  def numeral(number: Int): String =
    Numerals.lift(number - 1).getOrElse(number.toString)

  def ofGame(foundations: Vector[FoundationProjection]): Vector[Card] =
    foundations.map(foundation => Card(foundation.number,
      foundation.face == "altered", foundation.name,
      Some(foundation.rulesText)))

  def ofChronicle(foundations: Vector[ChronicleFoundation]): Vector[Card] =
    foundations.map(foundation => Card(foundation.number,
      foundation.face == "altered", foundation.name, None))

  /** "Foundation I · Wide Horizons": the overlay's title and the hover
    * note. */
  def title(card: Card): String =
    s"Foundation ${numeral(card.number)} · ${card.name}"

  /** "Foundation I, Wide Horizons, altered": a table card's label. */
  def label(card: Card): String =
    s"Foundation ${numeral(card.number)}, ${card.name}" +
      (if card.altered then ", altered" else "")

  def render(cards: Vector[Card]): dom.Element =
    val section = element("section", "foundations")
    section.setAttribute("aria-label", "Foundations")
    section.appendChild(text("h3", "foundations-title", "Foundations"))
    val strip = element("ol", "foundation-strip")
    cards.foreach { card =>
      val item = element("li", "")
      item.appendChild(face(card))
      strip.appendChild(item)
    }
    section.appendChild(strip)
    section

  private def face(card: Card): dom.Element =
    val node = card.rules match
      case Some(rules) =>
        val button = ServerUiSupport.button("", "foundation-card")
        button.`type` = "button"
        // A control is named in one phrase; its parts alone open with "I".
        button.setAttribute("aria-label", label(card))
        button.onclick = _ =>
          CardInspection.openText(title(card), Vector(rules), button)
        button
      // A plain row is read as its own text: numeral, name and mark. A
      // label on a span is ignored, so it carries none.
      case None => element("span", "foundation-card")
    // The name is cut to one line on a small map; the note keeps it whole.
    HoverTip.set(node, title(card))
    node.appendChild(text("span", "foundation-number", numeral(card.number)))
    node.appendChild(text("span", "foundation-name", card.name))
    if card.altered then
      node.appendChild(text("span", "foundation-mark", "Altered"))
    node
