package distantlands.frontend

import distantlands.protocol.art.ArtKey
import org.scalajs.dom
import ServerUiSupport._

/** A full-window card inspector, rendered at document level above the panes.
  *
  * Follows the development panel's dialog pattern in `GameTableShell`: hidden
  * attribute, Escape handler, Close button, focus return to the opening
  * control. It ignores projection changes while open -- what a card does does
  * not change when the card moves, and closing on every poll tick would yank
  * the card out from under a reader mid-sentence, which is the interruption
  * this whole design exists to remove. There is one exception: the consent
  * column's requests list is redrawn from each projection
  * (`showConsentRequests`; reconsider design, "Live while open"), so an
  * entry locks in place when the requester acts.
  */
private[frontend] final class CardInspectionOverlay(root: dom.Element):
  private val node = element("aside", "card-overlay").asInstanceOf[dom.html.Element]
  node.setAttribute("hidden", "")
  node.setAttribute("role", "dialog")
  node.setAttribute("aria-modal", "true")
  node.setAttribute("aria-label", "Card details")
  private val body = element("div", "card-overlay-body")
  private val close = button("Close", "card-overlay-close")
  node.appendChild(body)
  node.appendChild(close)
  root.appendChild(node)
  TokenSprite.mount(root)
  PowerMarks.mount(root)

  /** What opened the overlay, and where focus goes back to when it closes.
    * `pane` is the opener's `.game-pane`, or Chronicle Details, which a
    * snapshot also redraws (Chronicle Details implementation ruling R11):
    * a poll rebuilds a pane's content but keeps the pane and its heading,
    * so the pane is where a rebuilt opener is looked for, and its heading
    * is the fallback when none is found -- the same fallback
    * `PanelContent.replace` uses.
    */
  private final case class Opener(node: dom.html.Element,
      pane: Option[dom.Element], place: Option[PanelContent.Place])

  private var opener = Option.empty[Opener]

  /** The requests the open column shows, to tell a changed list from the
    * same one. `None` while no column is drawn. */
  private var shownRequests = Option.empty[ConsentRequests]
  /** Whether a click since the last redraw waits on the server's word. */
  private var changeSent = false
  /** The position and the failure the last repaint showed, and those
    * shown when the waiting click was made. The server's word on a click
    * is a new position or a new failure; any other repaint, such as a log
    * page landing, leaves the button waiting (implementation ruling
    * R20). */
  private var shownStamp = Option.empty[(Long, Option[AnyRef])]
  private var sentStamp = Option.empty[(Long, Option[AnyRef])]

  private def sameStamp(one: (Long, Option[AnyRef]),
      other: (Long, Option[AnyRef])): Boolean =
    one._1 == other._1 && ((one._2, other._2) match
      case (Some(first), Some(second)) => first eq second
      case (None, None) => true
      case _ => false)

  /** `requests` with each click noted, so the next snapshot redraws the
    * list even when the server refused and the rows are the same
    * (implementation ruling R20). */
  private def noting(requests: ConsentRequests): ConsentRequests =
    requests.copy(change = requests.change.map(send => (entry, allow) =>
      changeSent = true
      sentStamp = shownStamp
      send(entry, allow)))

  /** The consent column for `grid`, with its requests remembered. */
  private def drawColumn(grid: ConsentGrid): Unit =
    val noted = noting(grid.requests)
    body.appendChild(ConsentGridView.render(grid.copy(requests = noted)))
    shownRequests = Some(noted)
    changeSent = false

  /** Each snapshot's requests for the power the open column is about
    * (reconsider design, "Live while open"): the list is redrawn when it
    * differs from what is drawn, or when the server has answered a click:
    * `position`, the snapshot's next sequence, or `failure`, the failure
    * the table shows, is new since the click. The preference rows are
    * left as the reader set them. A power the viewer no longer rules
    * lists nothing. */
  def showConsentRequests(links: SitePowerLinks, position: Long,
      failure: Option[AnyRef]): Unit =
    val stamp = (position, failure)
    val answered = changeSent && sentStamp.forall(!sameStamp(_, stamp))
    shownStamp = Some(stamp)
    if isOpen then shownRequests.foreach { shown =>
      val next = links.requests(shown.powerId).getOrElse(
        ConsentRequests(shown.powerId, Vector.empty, None))
      if answered || next.entries != shown.entries ||
          next.change.isEmpty != shown.change.isEmpty then
        changeSent = false
        val noted = noting(next)
        Option(body.querySelector(".consent-requests")).foreach(block =>
          ConsentRequestsView.redraw(block, noted, body, close))
        shownRequests = Some(noted)
    }

  def isOpen: Boolean = !node.hasAttribute("hidden")

  def show(card: CardDetails, origin: dom.html.Element): Unit =
    clear()
    node.setAttribute("aria-label", "Card details")
    // Inspection reads a card, so a card the viewer is allowed to read is
    // turned over here. One the viewer cannot identify has nothing to turn.
    body.appendChild(CardFace.render(
      if card.hidden then card else card.copy(orientation = Some("face-up"))))
    if !card.hidden then body.appendChild(details(card))
    open(origin)

  /** The overlay over something that is not a card: a title and its printed
    * lines. The Oath is one of these -- it is a state of the game with a
    * printed card behind it, and faking a `CardDetails` for it would put a
    * card face on the table that nothing can be played from.
    */
  def showText(title: String, lines: Vector[String],
      origin: dom.html.Element, plate: Option[String] = None,
      art: Option[ArtKey] = None, banner: Option[BannerState] = None): Unit =
    clear()
    // A banner's own face first, large, as a card's overlay shows the
    // card (game art ruling R43): a picture, not the table's control.
    banner.foreach(value => body.appendChild(BannerFace.picture(value)))
    // Named for what it shows: it is not a card, so "Card details" would
    // misname it.
    node.setAttribute("aria-label", title)
    // The printed card behind the words, at its real ratio, holding its
    // image where the host's art has one.
    plate.foreach { kind =>
      val box = element("span", s"card-plate card-plate-$kind")
      box.appendChild(text("span", "card-name printed", title))
      ArtLayer.attach(box, art)
      body.appendChild(box)
    }
    val panel = element("div", "card-overlay-details")
    panel.appendChild(text("h3", "card-overlay-name", title))
    lines.foreach(line => RulesTextRenderer.powers(line)
      .foreach(panel.appendChild))
    body.appendChild(panel)
    open(origin)

  /** The Darkest Secret's Festival face: its printed lines, then a
    * picture of the festival marker and where it lies, so players see it
    * while it is off the map without its being in the shared bank. The
    * picture is not the map's control, and the words beside it name it.
    * Below, the holder's consent preferences for the Festival (ruling 16). */
  def showFestival(title: String, lines: Vector[String], at: Option[String],
      grid: Option[ConsentGrid], origin: dom.html.Element,
      banner: Option[BannerState] = None): Unit =
    showText(title, lines, origin, banner = banner)
    val copy = element("div", "rules-power festival-copy")
    val plate = FestivalMarker.plate(withRule = false)
    plate.setAttribute("aria-hidden", "true")
    copy.appendChild(plate)
    copy.appendChild(text("p", "festival-copy-where",
      "Festival marker: " + at.fold("beside the map")(site => s"at $site")))
    body.querySelector(".card-overlay-details").appendChild(copy)
    grid.foreach(drawColumn)

  /** The festival marker as printed: the plate, its title and its text,
    * then the holder's consent preferences for it. */
  def showFestivalMarker(grid: Option[ConsentGrid],
      origin: dom.html.Element): Unit =
    clear()
    node.setAttribute("aria-label", "Festival marker")
    body.appendChild(FestivalMarker.plate(withRule = true))
    grid.foreach(drawColumn)
    open(origin)

  /** A board's Supply: the bank above the track, as the physical board
    * prints it, with where the rest of the warbands are on the bank's next
    * line, and what the next Rest returns under the track. */
  def showSupply(panel: SupplyAtRest.Panel, origin: dom.html.Element): Unit =
    showText(SupplyAtRest.title, Vector.empty, origin)
    val details = body.querySelector(".card-overlay-details")
    val bank = text("p", "rules-power", panel.bank)
    bank.appendChild(dom.document.createElement("br"))
    bank.appendChild(dom.document.createTextNode(panel.elsewhere))
    details.appendChild(bank)
    details.appendChild(SupplyTracker.render(panel.track))
    details.appendChild(text("p", "rules-power", panel.rest))

  /** A power's printed text, and below it the viewer's consent preferences
    * when they rule it (ruling 16). */
  def showPower(title: String, lines: Vector[String],
      grid: Option[ConsentGrid], origin: dom.html.Element,
      banner: Option[BannerState] = None): Unit =
    showText(title, lines, origin, banner = banner)
    grid.foreach(drawColumn)

  /** A card, and below it the viewer's consent preferences for the power it
    * prints that they rule (consent slice 3). */
  def showRuled(card: CardDetails, grid: ConsentGrid,
      origin: dom.html.Element): Unit =
    show(card, origin)
    drawColumn(grid)

  /** Cards in the order given, as a card list (catalog batch 2, N5): a log
    * line's "6 cards". Each is its face or its back as projected; a face
    * still opens that one card here.
    */
  def showCards(title: String, cards: Vector[CardDetails],
      origin: dom.html.Element): Unit =
    clear()
    node.setAttribute("aria-label", title)
    body.appendChild(CardList.render(title, cards))
    open(origin)

  /** A yes-or-no question (consent design, "Frontend"): Yes closes the
    * overlay and runs `confirm`; No only closes it. */
  def showConfirm(question: String, confirm: () => Unit,
      origin: dom.html.Element): Unit =
    clear()
    node.setAttribute("aria-label", question)
    val panel = element("div", "card-overlay-details overlay-confirm")
    panel.appendChild(text("p", "overlay-question", question))
    val yes = button("Yes", "overlay-confirm-yes")
    val no = button("No", "overlay-confirm-no")
    yes.onclick = _ => { hide(); confirm() }
    no.onclick = _ => hide()
    panel.appendChild(yes)
    panel.appendChild(no)
    body.appendChild(panel)
    open(origin)
    // The safe answer first: Enter on arrival asks nobody.
    no.focus()

  /** A question with one button per choice, then Cancel (catalog batch 8,
    * slice 15): a choice closes the overlay and runs; Cancel only closes
    * it. */
  def showChoices(question: String, choices: Vector[(String, () => Unit)],
      origin: dom.html.Element): Unit =
    clear()
    node.setAttribute("aria-label", question)
    val panel = element("div", "card-overlay-details overlay-confirm")
    panel.appendChild(text("p", "overlay-question", question))
    choices.foreach { (label, run) =>
      val choice = button(label, "overlay-confirm-choice")
      choice.onclick = _ => { hide(); run() }
      panel.appendChild(choice)
    }
    val cancel = button("Cancel", "overlay-confirm-no")
    cancel.onclick = _ => hide()
    panel.appendChild(cancel)
    body.appendChild(panel)
    open(origin)
    // The safe answer first: Enter on arrival asks nobody.
    cancel.focus()

  private def clear(): Unit =
    while body.firstChild != null do body.removeChild(body.firstChild)
    shownRequests = None
    changeSent = false

  private def open(origin: dom.html.Element): Unit =
    val pane = Option(origin.closest(".game-pane, .chronicle-details"))
    opener = Some(Opener(origin, pane,
      pane.map(PanelContent.place(_, origin)).filter(_.index >= 0)))
    node.removeAttribute("hidden")
    close.focus()

  def hide(): Unit =
    node.setAttribute("hidden", "")
    opener.flatMap(returnTarget) match
      case Some(target) => target.focus()
      // Nowhere to return to: at least do not leave focus on the hidden
      // close button.
      case None => close.blur()
    opener = None

  /** The opener if it is still in the document; else, when a poll rebuilt
    * it away while the overlay was open, its rebuilt twin in the same pane;
    * else that pane's heading.
    */
  private def returnTarget(from: Opener): Option[dom.html.Element] =
    val pane = from.pane.filter(dom.document.contains)
    Option(from.node).filter(dom.document.contains)
      .orElse(for p <- pane; at <- from.place; n <- PanelContent.find(p, at) yield n)
      .orElse(pane.flatMap(p => Option(p.querySelector(".pane-heading")))
        .map(_.asInstanceOf[dom.html.Element]))

  private def details(card: CardDetails): dom.Element =
    val panel = element("div", "card-overlay-details")
    panel.appendChild(text("h3", "card-overlay-name", card.name))
    val properties = element("dl", "card-overlay-properties")
    Vector(card.suit.map("Suit" -> _),
      card.legacyGoal.map("Goal" -> _.text),
      card.restrictions.filterNot(_ == "unrestricted").map("Restrictions" -> _),
      card.orientation.map("Orientation" -> _),
      card.side.map("Side" -> _),
      Option.when(card.favor > 0)("Favor" -> card.favor.toString),
      Option.when(card.secrets > 0)("Secrets" -> card.secrets.toString),
      card.relicValue.map(v => "Relic value" -> v.toString),
      card.defense.map(v => "Defense" -> v.toString),
      // The face's pawn marker is decoration; the record is read here.
      card.tracking.map(tracked =>
        "Tracking" -> tracked.color.key.capitalize)).flatten.foreach:
      case (label, value) =>
        // A goal is a sentence, printed as the card prints it; the other
        // values are single words, capitalised.
        val item = element("div", if label == "Goal" then
          "card-overlay-property card-overlay-sentence"
          else "card-overlay-property")
        item.appendChild(text("dt", "", label))
        item.appendChild(text("dd", "", value))
        properties.appendChild(item)
    if properties.childNodes.length > 0 then panel.appendChild(properties)
    // Each power under its braid, as the card prints it; a card the catalog
    // lays out no powers for (a Vision) keeps its rules text as written.
    if card.powers.nonEmpty then
      val rules = element("div", "card-overlay-rules")
      card.powers.foreach(power => rules.appendChild(powerFace(power)))
      panel.appendChild(rules)
    else card.rulesText.foreach { value =>
      val rules = element("div", "card-overlay-rules")
      RulesTextRenderer.powers(value).foreach(rules.appendChild)
      panel.appendChild(rules)
    }
    panel

  /** A power below its braid, which divides it from the one above: the
    * modifier tab at the braid's left, the cost on it, black for a
    * persistent power. */
  private def powerFace(power: PowerFace): dom.Element =
    val section = element("section", if power.persistent then
      "power-face power-face-persistent" else "power-face")
    val braid = element("div", "power-braid")
    if power.persistent then braid.setAttribute("aria-label", "Persistent")
    power.modifier.foreach(mark => PowerMarks.modifier(mark, power.persistent)
      .foreach(braid.appendChild))
    if power.cost.nonEmpty then
      val cost = element("span", "power-cost")
      power.cost.foreach(token =>
        cost.appendChild(RulesTextRenderer.glyph(token)))
      braid.appendChild(cost)
    section.appendChild(braid)
    section.appendChild(RulesTextRenderer.power(power.text))
    section

  private val dismiss: dom.MouseEvent => Unit = event =>
    // A click anywhere on the overlay dismisses it, except inside the card
    // itself -- selecting rules text should not close what you are reading.
    if event.target == node || event.target == close then hide()

  private val escape: dom.KeyboardEvent => Unit = event =>
    if event.key == "Escape" && isOpen then { event.preventDefault(); hide() }

  node.addEventListener("click", dismiss)
  node.addEventListener("keydown", escape)

  def dispose(): Unit =
    node.removeEventListener("click", dismiss)
    node.removeEventListener("keydown", escape)
    node.remove()
