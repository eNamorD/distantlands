package distantlands.frontend

import org.scalajs.dom
import ServerUiSupport.{element, text}

/** Legacies on the table (Legacies design, slice 5): each player area's
  * strip, a row of its own below the cards in play (implementation ruling
  * R37), and the Public Ambitions row the World pane heads above its
  * regions while it holds a legacy (R38).
  *
  * The players pane never scrolls and leaves a board one line under its
  * cards, so the strip draws each legacy as a chip: its suit seal and name,
  * "Dormant" when dormant, its goal in the chip's hover note, and the whole
  * card in the inspection a click opens. A dormant legacy a viewer may not
  * know is an L chip (R36). The public row has the room for whole cards,
  * drawn by `CardFace`. Each legacy is keyed by its card when known, so a
  * decision over legacies marks it here. */
private[frontend] object LegacyStrip:
  /** `board`'s strip, or none when its lineage holds no legacy. */
  def board(board: PlayerBoard, options: BoardOptions): Option[dom.Element] =
    Option.when(board.legacies.nonEmpty) {
      val row = element("div", "board-legacies")
      row.setAttribute("aria-label", "Legacies")
      board.legacies.foreach(card => row.appendChild(options.card(chip(card),
        card, keys(card))))
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
    options.card(CardFace.render(card), card, keys(card))

  private def keys(card: CardDetails): Vector[String] =
    Option.unless(card.hidden)(BoardOptions.cardKey(card)).toVector

  /** One legacy in the strip: its seal, name and dormancy, or an L back. */
  private def chip(card: CardDetails): dom.html.Button =
    val node = dom.document.createElement("button").asInstanceOf[dom.html.Button]
    node.`type` = "button"
    node.setAttribute("data-card-id", card.cardId)
    if card.hidden then
      node.className = "legacy-chip legacy-chip-back"
      node.setAttribute("aria-label", "Legacy")
      node.appendChild(text("span", "card-back-letter",
        CardFace.backLetter(card.cardKind)))
    else
      val dormant = card.orientation.contains("face-down")
      node.className = if dormant then "legacy-chip legacy-chip-dormant"
        else "legacy-chip"
      node.setAttribute("aria-label",
        if dormant then s"${card.name}, dormant" else card.name)
      card.legacyGoal.foreach(goal => HoverTip.set(node, goal.text))
      card.suit.foreach(suit =>
        node.appendChild(RulesTextRenderer.glyph(s"suit-$suit")))
      node.appendChild(text("span", "legacy-chip-name", card.name))
      if dormant then node.appendChild(text("span", "legacy-dormant", "Dormant"))
      node.onclick = _ => CardInspection.open(card, node)
    node
