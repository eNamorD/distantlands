package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{element, text}

/** A read-only strip of setup cards under a heading (Chronicle Details
  * design, slice 2): the foundations (`FoundationStrip`) and the Chronicle
  * Phase aids (`ChronicleTaskStrip`). One row a card: its mark, its name
  * and its tag, each drawn only when present. A click reads the card's
  * lines in the inspector. A strip's classes come from its stem, so the
  * foundation strip keeps the classes its styles and suites read
  * (implementation ruling R7). */
private[frontend] object SetupCardStrip:
  /** One card as a strip draws it. `mark` is the foundation's numeral;
    * `tag` is "Altered" on an altered face; `title` heads the inspector and
    * is the hover note; `label` names the control; `lines` are what a
    * click reads; `plate` names the printed card behind the words. */
  final case class Card(mark: Option[String], name: String,
      tag: Option[String], title: String, label: String,
      lines: Vector[String], plate: Option[String])

  /** The strip headed `heading`. With stem "foundation" its classes are
    * `foundations`, `foundations-title`, `foundation-strip`,
    * `foundation-card`, `foundation-number`, `foundation-name` and
    * `foundation-mark`. */
  def render(heading: String, stem: String, cards: Vector[Card])
      : dom.Element =
    val section = element("section", s"${stem}s")
    section.setAttribute("aria-label", heading)
    section.appendChild(text("h3", s"${stem}s-title", heading))
    val strip = element("ol", s"$stem-strip")
    cards.foreach { card =>
      val item = element("li", "")
      item.appendChild(face(card, stem))
      strip.appendChild(item)
    }
    section.appendChild(strip)
    section

  private def face(card: Card, stem: String): dom.Element =
    val node = ServerUiSupport.button("", s"$stem-card")
    node.`type` = "button"
    // A control is named in one phrase; its parts alone may open with a
    // numeral.
    node.setAttribute("aria-label", card.label)
    node.onclick = _ =>
      CardInspection.openText(card.title, card.lines, node, card.plate)
    // The name is cut to one line on a small map; the note keeps it whole.
    HoverTip.set(node, card.title)
    card.mark.foreach(mark =>
      node.appendChild(text("span", s"$stem-number", mark)))
    node.appendChild(text("span", s"$stem-name", card.name))
    card.tag.foreach(tag =>
      node.appendChild(text("span", s"$stem-mark", tag)))
    node
