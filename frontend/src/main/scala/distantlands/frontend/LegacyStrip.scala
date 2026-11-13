package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{element, text}

/** Legacies on the table (Legacies design, slice 5): each player area's
  * strip, a row of its own below the cards in play (implementation ruling
  * R37), and the Public Ambitions row the World pane heads above its
  * regions while it holds a legacy (R38). Each card is drawn by `CardFace`,
  * so a dormant legacy reads as its face marked Dormant to a viewer who
  * knows it and as an L back to anyone else (R36), and is keyed by its card
  * when known, so a decision over legacies marks it here. */
private[frontend] object LegacyStrip:
  /** `board`'s strip, or none when its lineage holds no legacy. */
  def board(board: PlayerBoard, options: BoardOptions): Option[dom.Element] =
    Option.when(board.legacies.nonEmpty) {
      val row = element("div", "board-legacies")
      row.setAttribute("aria-label", "Legacies")
      board.legacies.foreach(card => row.appendChild(drawn(card, options)))
      row
    }

  /** The Public Ambitions row, or none while it is empty. */
  def publicRow(value: GameProjection, options: BoardOptions)
      : Option[dom.Element] =
    Option.when(value.publicLegacies.nonEmpty) {
      val section = element("section", "public-legacies")
      section.setAttribute("aria-label", "Public Ambitions")
      section.appendChild(text("h3", "public-legacies-label",
        "Public Ambitions"))
      val row = element("div", "public-legacies-cards")
      value.publicLegacies.foreach(card => row.appendChild(drawn(card, options)))
      section.appendChild(row)
      section
    }

  private def drawn(card: CardDetails, options: BoardOptions): dom.Element =
    options.card(CardFace.render(card), card,
      Option.unless(card.hidden)(BoardOptions.cardKey(card)).toVector)
