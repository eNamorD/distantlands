package distantlands.frontend

import distantlands.protocol._
import org.scalajs.dom
import scala.scalajs.js

/** A trusted seat's page: the game it is in and the player whose seat it is. */
private[frontend] final case class SeatPage(gameId: String, playerId: String)

private[frontend] object ServerUiSupport:
  /** The count a player hovers to learn, then why the rest are unavailable. */
  private[frontend] def secretSummaryLines(available: Int, total: Int,
      facedown: Int, committed: Int): Vector[String] =
    Vector(s"$available/$total secrets available",
      s"$facedown facedown on board, $committed committed to cards")

  /** Name row and the two upper corners: what a site holds on the left, what
    * it costs to walk into on the right.
    */
  private[frontend] def siteHeading(site: GameSite): dom.Element =
    val presentation = SiteCardPresentation.from(site)
    val heading = element("div", "site-heading")
    val tokens = element("span", "site-tokens")
    if presentation.looseFavor > 0 then
      siteToken("favor", presentation.looseFavor).foreach(tokens.appendChild)
    if presentation.looseSecrets > 0 then
      siteToken("secret", presentation.looseSecrets).foreach(tokens.appendChild)
    // Always appended, empty or not: the name is the middle cell of three,
    // and a missing cell would slide it off centre.
    heading.appendChild(tokens)
    heading.appendChild(text("span", "site-name printed", site.label))
    val defense = element("span", "site-defense printed")
    defense.setAttribute("aria-label", s"Defense ${presentation.defense}")
    // Drawn as dice rather than a number because defense is rolled, and no
    // site in the catalog exceeds two. An undefended site says so in words,
    // since no die at all would read as missing information.
    if presentation.defense == 0 then
      defense.appendChild(text("span", "site-defense-none", "0"))
    else (0 until presentation.defense)
      .foreach(_ => defense.appendChild(RulesTextRenderer.glyph("defense-die")))
    heading.appendChild(defense)
    // The form the far zoomed-out map shows in its place (implementation
    // ruling R5): one die and the count. It is for the eye; the long form
    // keeps the accessible name and the hover note carries it.
    val short = element("span", "site-defense-short printed")
    short.setAttribute("aria-hidden", "true")
    HoverTip.set(short, s"Defense ${presentation.defense}")
    short.appendChild(RulesTextRenderer.glyph("defense-die"))
    short.appendChild(text("span", "site-defense-count",
      presentation.defense.toString))
    heading.appendChild(short)
    heading

  private def siteToken(token: String, count: Int): Vector[dom.Element] =
    Vector(RulesTextRenderer.glyph(token),
      text("span", "site-token-count", count.toString))

  /** A card's face and what opening it does: a card printing a power the
    * viewer rules opens with their consent grid (consent slice 3), on a
    * site or on a board (Ancient Bloodline); any other opens as itself. */
  private[frontend] def ruledCard(details: CardDetails, links: SitePowerLinks)
      : (dom.html.Button, Option[dom.html.Element => Unit]) =
    val face = CardFace.render(details)
    val ruled = links.cardGrid(details).map(grid =>
      (origin: dom.html.Element) =>
        CardInspection.openRuled(details, grid, origin))
    ruled.foreach(open => face.onclick = _ => open(face))
    (face, ruled)

  private[frontend] def siteDetails(site: GameSite,
      options: BoardOptions = BoardOptions.none,
      links: SitePowerLinks = SitePowerLinks.none): dom.Element =
    val presentation = SiteCardPresentation.from(site)
    val details = element("div", "site-details")
    // Denizens and relics share one row: capacity and relic slots always sum
    // to three, so one row holds every card a site can ever have.
    val cards = element("div", "site-cards")
    site.denizens.zipWithIndex.foreach { (denizen, at) =>
      denizen.details match
        case Some(details) =>
          val (face, ruled) = ruledCard(details, links)
          face.setAttribute("data-denizen-id", denizen.denizenId)
          cards.appendChild(options.card(face, details,
            Vector(BoardOptions.cardKey(details), siteSlot(site, at)), ruled))
        case None =>
          val back = facedownCard("denizen")
          back.setAttribute("data-denizen-id", denizen.denizenId)
          cards.appendChild(back)
    }
    (site.denizens.size until site.denizenCapacity).foreach(at =>
      cards.appendChild(openSlot(options, "denizen", "Empty denizen slot", at,
        siteSlot(site, at))))
    // Each relic in its place answers to its slot: a known one is its face,
    // an unknown one its back. A peeked relic is a real card sitting
    // face-down: CardFace gives it the knowable pip and the hover reveal.
    presentation.relics.zipWithIndex.foreach((card, at) =>
      val slot = siteRelicSlot(site, at)
      cards.appendChild(
        if card.hidden then
          val back = facedownCard("relic")
          options.decorate(back, Vector(slot))
          back
        else options.card(CardFace.render(card), card,
          Vector(BoardOptions.cardKey(card), slot))))
    (presentation.relics.size until site.relicCapacity)
      .foreach(_ => cards.appendChild(emptySlot("relic")))
    details.appendChild(cards)
    site.forces.foreach { forces =>
      // The words cut inside the face and the count never does; together
      // they read as forceText.
      val row = element("p", s"site-forces ${forceCssClass(forces)}")
      row.appendChild(text("span", "site-forces-label", s"${forces.label} "))
      row.appendChild(text("span", "site-forces-count", s"x${forces.count}"))
      forces match
        case exile: SiteForces.Exile =>
          row.setAttribute("data-ruler-kind", "player")
          row.setAttribute("data-ruler-player-id", exile.rulerPlayerId)
        case _: SiteForces.Imperial =>
          row.setAttribute("data-ruler-kind", "empire")
        case _: SiteForces.Bandit =>
          row.setAttribute("data-ruler-kind", "bandit")
      details.appendChild(row)
      // The short form for the far zoomed-out map (ruling R5): the warband
      // in the force's colour and its count, as a card shows warbands.
      val short = element("span", s"site-forces-short ${forceCssClass(forces)}")
      short.setAttribute("aria-hidden", "true")
      HoverTip.set(short, forceText(forces))
      short.appendChild(RulesTextRenderer.glyph("warband"))
      short.appendChild(text("span", "site-token-count", forces.count.toString))
      details.appendChild(short)
    }
    val footer = element("div", "site-footer")
    // Two lines of labels at most (`.site-powers`). Each label keeps its
    // rules text on its hover title, and opens the power in the inspector.
    // A button, so the site box it sits in leaves the click to it
    // (`BoardOptions.own`).
    val powers = element("p", "site-powers")
    site.powers.zipWithIndex.foreach { case (power, index) =>
      if index > 0 then powers.appendChild(dom.document.createTextNode(" · "))
      // The control is not printed; the label it shows is (ruling R9).
      val item = button("", "site-power")
      item.setAttribute("aria-label", power.label)
      item.appendChild(text("span", "printed", power.label))
      HoverTip.set(item, power.description.fold(power.label)(
        description => s"${power.label}: $description"))
      item.onclick = (event: dom.MouseEvent) =>
        event.stopPropagation()
        CardInspection.openPower(power.label, power.description.toVector,
          links.grid(power), item)
      powers.appendChild(item)
    }
    footer.appendChild(powers)
    presentation.requirement.foreach(value =>
      footer.appendChild(requirementCorner(value)))
    details.appendChild(footer)
    details

  /** Forge keeps its word because the catalog has no forge glyph; its price is
    * glyphs because favor and secrets do. Recover is a die-roll target rather
    * than a price, so it stays a number.
    */
  private def requirementCorner(value: SiteRequirement): dom.Element =
    val node = element("span", "site-requirement printed")
    value match
      case SiteRequirement.Forge(favor, secrets) =>
        node.appendChild(text("span", "site-requirement-label", "Forge"))
        (0 until favor).foreach(_ =>
          node.appendChild(RulesTextRenderer.glyph("favor")))
        (0 until secrets).foreach(_ =>
          node.appendChild(RulesTextRenderer.glyph("secret")))
      case SiteRequirement.Recover(difficulty) =>
        node.appendChild(text("span", "site-requirement-label",
          s"Recover $difficulty"))
    node

  private[frontend] def forceText(forces: SiteForces): String =
    s"${forces.label} x${forces.count}"

  private[frontend] def forceCssClass(forces: SiteForces): String = forces match
    case exile: SiteForces.Exile => s"force-${exile.color.key}"
    case _: SiteForces.Imperial => "force-empire"
    case _: SiteForces.Bandit => "force-bandit"

  /** A site relic the viewer cannot identify. Same box as a real relic, so
    * learning what it is -- which a negotiation can do mid-game -- swaps the
    * contents of a box that does not move.
    */
  private[frontend] def facedownCard(cardKind: String): dom.Element =
    val node = element("span", s"card-face ${CardFace.boxClass(cardKind)} card-face-down")
    node.setAttribute("role", "img")
    node.setAttribute("aria-label", s"Facedown $cardKind")
    node.appendChild(text("span", "card-back-letter printed", CardFace.backLetter(cardKind)))
    ArtLayer.attach(node, art.ArtCatalog.cardBack(cardKind))
    node

  private[frontend] def emptySlot(cardKind: String, label: String = "")
      : dom.Element =
    val node = element("span",
      s"card-face ${CardFace.boxClass(cardKind)} card-slot-empty")
    node.setAttribute("role", "img")
    node.setAttribute("aria-label",
      if label.isEmpty then s"Empty $cardKind slot" else label)
    node

  /** The key of slot `at` in `site`'s card row. The row's order is the
    * slot order, so a card's index is its slot. */
  private def siteSlot(site: GameSite, at: Int): String =
    BoardOptionViews.key("site-slot", s"${site.siteId}:$at")

  private def siteRelicSlot(site: GameSite, at: Int): String =
    BoardOptionViews.key("site-relic-slot", s"${site.siteId}:$at")

  /** Empty slot `at`, which a card play may fill: it carries its index and
    * is marked when an option names `key`. */
  private[frontend] def openSlot(options: BoardOptions, cardKind: String,
      label: String, at: Int, key: String): dom.Element =
    val node = emptySlot(cardKind, label)
    node.setAttribute("data-slot-index", at.toString)
    options.decorate(node, Vector(key))
    node

  /** A deck drawn as a card, with the top card's back letter and the count
    * on its face: the part a decision rings, with its cost badge under the
    * card. `deckFigure` hangs the caption below it. A deck opens nothing: a
    * deck's order is nobody's to see.
    */
  private[frontend] def deckBody(count: Int,
      topCardKind: Option[String]): dom.Element =
    val body = element("div", "deck-body")
    body.appendChild(deckCard(count, topCardKind))
    body

  /** A pile whose cards a player may list, drawn as `deckBody` is: a button
    * that opens them top first in the card overlay, the way a log line's
    * card list opens (`CardInspection.openCards`), disabled while the pile
    * is empty.
    */
  private[frontend] def openableDeck(count: Int, topCardKind: Option[String],
      title: String, cards: Vector[CardDetails]): dom.html.Button =
    val node = button("", "deck-body pile-open")
    node.setAttribute("aria-label",
      s"$title, $count ${if count == 1 then "card" else "cards"}")
    node.disabled = count == 0
    node.onclick = _ => CardInspection.openCards(s"$title ($count)", cards, node)
    node.appendChild(deckCard(count, topCardKind))
    node

  /** `body` (or the box holding it and its inspect button) with `caption`
    * under it, outside what a decision rings.
    */
  private[frontend] def deckFigure(caption: String,
      body: dom.Element): dom.Element =
    val figure = element("div", "deck-figure")
    figure.appendChild(body)
    // The mat prints the slot's name, so the caption is printed content.
    figure.appendChild(text("span", "deck-caption printed", caption))
    figure

  private def deckCard(count: Int, topCardKind: Option[String]): dom.Element =
    val card = element("span", s"deck-card ${pileCardClasses(count)}")
    val symbol = pileSymbol(count, topCardKind)
    val letter = text("span", "deck-letter printed",
      if symbol.isEmpty then "\u00a0" else symbol)
    letter.setAttribute("role", "img")
    letter.setAttribute("aria-label", topCardLabel(count, topCardKind))
    card.appendChild(letter)
    card.appendChild(text("span", "pile-count", s"×$count"))
    card

  private def topCardLabel(count: Int, topCardKind: Option[String]): String =
    if count == 0 then "Empty pile"
    else topCardKind match
      case Some("denizen") => "Denizen card on top"
      case Some("vision") => "Vision card on top"
      case Some("relic") => "Relic card on top"
      case _ => "Facedown card; type hidden"

  /** The pile shows the same back letter a facedown card of that kind would,
    * so `CardFace` owns the mapping. An empty pile, or one whose top the
    * viewer may not identify, shows nothing.
    */
  private[frontend] def pileSymbol(count: Int, topCardKind: Option[String]): String =
    if count == 0 then "" else topCardKind.fold("")(CardFace.backLetter)

  private[frontend] def pileCardClasses(count: Int): String =
    if count == 0 then "pile-empty" else "pile-back"

  private[frontend] final case class ViewerPresentation(
      showGameplayControls: Boolean,
      waitingForPlayerIds: Vector[String],
      waitingForDisplayNames: Vector[String],
      playerId: String
  )

  private[frontend] def viewerPresentation(
      value: GameProjection,
      playerId: String
  ): ViewerPresentation =
    val finished = value.oathkeeper.exists(_.winnerPlayerId.nonEmpty) &&
      value.walkerDecision.isEmpty && value.walkerWaiting.isEmpty
    val (showGameplayControls, waitingFor) =
      // A finished game hides every control, except for a walk parked after
      // it: the winner's Start Chronicle decision, and the Chronicle's own.
      if finished then (false, Vector.empty[String])
      // A parked walker `Decide`'s owner is projected `walkerDecision`
      // regardless of whose turn it is, and everyone else is projected
      // `walkerWaiting` naming every player who may answer (see
      // WalkerDecisionProjector.project/waiting). Reading
      // `activeParticipantId` instead of these two fields would leave an
      // off-turn owner with no panel -- and the active player waiting on
      // them right back -- since neither side is the other's active
      // participant.
      else if value.walkerDecision.nonEmpty then (true, Vector.empty[String])
      else value.walkerWaiting match
        case Some(waiting) => (false, waiting.playerIds)
        case None => value.activeParticipantId match
          case Some(active) if active != playerId => (false, Vector(active))
          case _ => (true, Vector.empty[String])
    ViewerPresentation(showGameplayControls, waitingFor,
      waitingFor.map(playerDisplayName(value, _)), playerId)

  /** A banner's printed name from its key. */
  private[frontend] def bannerLabel(key: String): String = key match
    case "peoples-favor" => "People's Favor"
    case "darkest-secret" => "Darkest Secret"
    case other => other

  private[frontend] def playerColorClass(value: GameProjection,
      playerId: String): String =
    PlayerColorCss.of(value.players.find(_.playerId == playerId).map(_.color))

  private[frontend] def winnerBanner(value: GameProjection,
      winner: String, victory: String): dom.Element =
    val banner = element("div", s"victory-banner ${playerColorClass(value, winner)}")
    banner.setAttribute("role", "status")
    banner.appendChild(playerReference(value, winner))
    banner.appendChild(dom.document.createTextNode(s" wins — $victory"))
    banner

  private[frontend] def playerReference(
      value: GameProjection,
      playerId: String
  ): dom.Element =
    val player = value.players.find(_.playerId == playerId)
    val node = text(
      "span",
      s"player-ref ${PlayerColorCss.of(player.map(_.color))}",
      player.fold(playerId)(_.displayName)
    )
    node.setAttribute("data-player-id", playerId)
    node

  private[frontend] def protocolNegotiationTerms(value: NegotiationTermsInput): NegotiationTerms =
    NegotiationTerms(
      value.transfers.map(v => NegotiationTransfer(
        v.recipientPlayerId, v.favor, v.relicIds, v.advisers, v.siteIds,
        v.secrets)),
      value.disclosures.map { disclosure =>
        val information: NegotiationInformation = disclosure.kind match {
          case "adviser" => NegotiationInformation.Adviser(
            disclosure.ownerPlayerId.get,
            WorldCard(disclosure.cardKind.get, disclosure.cardId))
          case "held-relic" => NegotiationInformation.HeldRelic(
            disclosure.ownerPlayerId.get, disclosure.cardId)
          case "site-relic" => NegotiationInformation.SiteRelic(
            disclosure.siteId.get, disclosure.cardId)
        }
        NegotiationDisclosure(disclosure.recipientPlayerId, information)
      },
      value.citizenshipPlayerIds, value.promises, value.bindingPromises)

  private[frontend] def playerDisplayName(
      value: GameProjection,
      playerId: String
  ): String =
    value.players.find(_.playerId == playerId)
      .fold(playerId)(_.displayName)

  private[frontend] def freshGameId(): String =
    s"manual-${js.Date.now().toLong}-${(js.Math.random() * 1000000).toInt}"

  /** The seat whose page `pathname` is: `/games/{game}/seats/{player}`,
    * each segment percent-encoded and non-empty. */
  private[frontend] def seatPage(pathname: String): Option[SeatPage] =
    def decoded(encoded: String): Option[String] =
      try Option(js.URIUtils.decodeURIComponent(encoded)).filter(_.nonEmpty)
      catch { case scala.util.control.NonFatal(_) => None }
    pathname.split("/", -1).toVector match
      case Vector("", "games", game, "seats", player) =>
        for gameId <- decoded(game); playerId <- decoded(player)
        yield SeatPage(gameId, playerId)
      case _ => None

  private[frontend] def queryParameter(name: String): Option[String] =
    dom.window.location.search.stripPrefix("?").split("&").toVector
      .flatMap { pair =>
        pair.split("=", 2).toVector match
          case Vector(key, value) if key == name =>
            Some(js.URIUtils.decodeURIComponent(value))
          case _ => None
      }.headOption.filter(_.nonEmpty)

  private[frontend] def sessionUrl(gameId: String, playerId: String): String =
    s"/?mode=server&gameId=${js.URIUtils.encodeURIComponent(gameId)}" +
      s"&playerId=${js.URIUtils.encodeURIComponent(playerId)}"

  private[frontend] def updateUrl(gameId: String, playerId: String): Unit =
    dom.window.history.replaceState(null, "", sessionUrl(gameId, playerId))

  /** A text box named `label` for assistive technology, holding `value`. */
  private[frontend] def textField(className: String, label: String,
      value: String): dom.html.Input =
    val node = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    node.className = className
    node.setAttribute("aria-label", label)
    node.setAttribute("autocomplete", "off")
    node.value = value
    node

  private[frontend] def button(label: String, className: String): dom.html.Button =
    val node =
      dom.document.createElement("button").asInstanceOf[dom.html.Button]
    node.className = className
    node.textContent = label
    node

  /** A card's name, as a run of words that opens the card in the overlay. */
  private[frontend] def cardLink(card: CardDetails): dom.html.Button =
    val node = button(card.name, "card-link")
    node.onclick = _ => CardInspection.open(card, node)
    node

  private[frontend] def text(tag: String, className: String, value: String): dom.Element =
    val node = element(tag, className)
    node.textContent = value
    node

  private[frontend] def svgElement(tag: String): dom.Element =
    dom.document.createElementNS("http://www.w3.org/2000/svg", tag)

  private[frontend] def element(tag: String, className: String): dom.Element =
    val node = dom.document.createElement(tag)
    if className.nonEmpty then node.setAttribute("class", className)
    node
