package distantlands.frontend

import distantlands.protocol.{GameIntent => GameCommand}
import org.scalajs.dom
import ServerUiSupport._

private[frontend] object WorldBoardRenderer:
  /** A resource as its glyph and its count, with the sentence the words used
    * to carry kept as the accessible name.
    */
  private def counted(token: String, value: String,
      accessible: String): dom.Element =
    val node = element("span", "resource")
    node.setAttribute("aria-label", accessible)
    node.appendChild(RulesTextRenderer.glyph(token))
    node.appendChild(dom.document.createTextNode(value))
    node

  /** Turn order read from the viewer's own seat. The seating vector is
    * already the cyclic turn order, so anchoring it on the viewer gives who
    * plays after them without tracking the first player -- and unlike an
    * active-player anchor, the strip does not rotate under them as turns pass.
    */
  private[frontend] def seatOrder(players: Vector[GamePlayer],
      viewer: Option[String]): Vector[GamePlayer] =
    viewer.map(id => players.indexWhere(_.playerId == id)).filter(_ > 0)
      .fold(players)(at => players.drop(at) ++ players.take(at))

  def playerBoards(value: GameProjection, playerId: String,
      options: BoardOptions = BoardOptions.none): dom.Element =
    val panel = element("section", "panel player-boards")
    // The seat the client holds, not the projection's viewer: a trusted
    // session adopts its seat from that viewer before it renders and refuses
    // to display a projection that disagrees with it (`ServerModeUi.store`),
    // so this is the one answer that is right in both modes.
    seatOrder(value.players, Some(playerId).filter(_.nonEmpty))
      .foreach { player =>
      val section = element("section", "player-board")
      section.setAttribute("data-player-id", player.playerId)
      if value.activeParticipantId.contains(player.playerId) then
        section.classList.add("player-board-active")
        section.setAttribute("aria-label", s"${player.displayName}, active player")
      // Everything left of the cards stands for the player, so a decision
      // that picks a player lights the whole block rather than the name
      // alone. A control inside it (Supply, a title, the Vision) still
      // answers for itself.
      val playerSummary = element("div", "player-summary")
      options.decorate(playerSummary,
        Vector(BoardOptionViews.key("player", player.playerId)))
      section.appendChild(playerSummary)
      // One line for who and what they hold: the pane is a strip across the
      // top of the table, so every line it spends is a line of card.
      val identity = element("h3", "player-identity")
      identity.appendChild(playerReference(value, player.playerId))
      identity.appendChild(RoleText.span(player.role, "player-role"))
      if value.tracks.exists(_.firstPlayerId == player.playerId) then
        identity.appendChild(text("span", "player-first-badge", "First Player"))
      playerSummary.appendChild(identity)
      value.playerBoards.find(_.playerId == player.playerId).foreach { board =>
      val resources = element("span", "resources")
      val warbands = text("span", "resource", s"Warbands ${board.warbands}")
      HoverTip.set(warbands, warbandsNote(board,
        own = player.playerId == playerId))
      resources.appendChild(warbands)
      val favor = counted("favor", board.favor.toString, s"Favor ${board.favor}")
      HoverTip.set(favor, "Favor")
      resources.appendChild(favor)
      val summary = secretSummaryLines(board.faceUpSecrets, board.totalSecrets,
        board.faceDownSecrets, board.committedSecrets)
      val secrets = counted("secret", s"${board.faceUpSecrets}/${board.totalSecrets}",
        summary.mkString(". "))
      HoverTip.set(secrets, summary.mkString("\n"))
      resources.appendChild(secrets)
      // Supply is the one figure with a rule behind it: the bank sets the
      // band, the band sets the Rest. The figure opens that rule.
      val supply = button(s"Supply ${board.supply}/${value.supplyMaximum}",
        "resource supply-open")
      HoverTip.set(supply, SupplyAtRest.note(player.role, board))
      supply.onclick = _ =>
        if SupplyAtRest.readsTrack(player.role) then
          CardInspection.openSupply(SupplyAtRest.panel(board,
            if board.supplyBands.nonEmpty then board.supplyBands
            else value.supplyBands, value.supplyMaximum, player.color,
            own = player.playerId == playerId), supply)
        else CardInspection.openText(SupplyAtRest.title,
          SupplyAtRest.citizenLines, supply)
      resources.appendChild(supply)
      identity.appendChild(resources)

      // A title and a Vision are states of the player, not cards in a hand:
      // in the physical game a Vision is played sideways, which is what this
      // row stands in for.
      val slots = element("div", "player-slots")
      value.oathkeeper.filter(_.holderPlayerId.contains(player.playerId))
        .foreach { oath =>
          val presented = distantlands.protocol.projection
            .OathkeeperPresentation.byGoal.get(oath.goal)
          val pill = button(oath.side.capitalize, s"player-title title-${oath.side}")
          val title = presented.fold(oath.goal)(_.title)
          HoverTip.set(pill, title)
          pill.onclick = _ => CardInspection.openText(title,
            presented.fold(Vector.empty[String])(_.lines), pill,
            Some("oathkeeper-title"))
          slots.appendChild(pill)
        }
      // The spot is drawn empty only while the decision lights it: the strip
      // never scrolls, so an empty spot may not spend a row on every board.
      // A reveal that replaces marks every Vision it discards; one that
      // keeps them (Desecrated Ground) lights an empty spot after them.
      val visionKey = Vector(BoardOptionViews.key("revealed-vision",
        player.playerId))
      val lit = options.view(visionKey)
      val replacing = lit.exists(_.option.replaces.nonEmpty)
      board.revealedVisions.foreach { card =>
        val pill = button(card.name, "player-slot-vision")
        pill.onclick = _ => CardInspection.open(card, pill)
        // The warbands on the Vision (False Prophet's claims), one mark in
        // each claimant's colour; the card face counts them. The marks are
        // hidden from assistive technology, so the pill's name says them.
        if card.warbands.nonEmpty then pill.setAttribute("aria-label",
          (card.name +: card.warbands.map(forceText)).mkString(", "))
        card.warbands.foreach { forces =>
          val mark = text("span", "player-slot-vision-warband " +
            forceCssClass(forces), "●")
          mark.setAttribute("aria-hidden", "true")
          HoverTip.set(mark, forceText(forces))
          pill.appendChild(mark)
        }
        slots.appendChild(
          if replacing then options.control(pill, visionKey,
            Some(card.name -> (CardInspection.open(card, _))))
          else pill)
      }
      lit.filterNot(_ => replacing).foreach { _ =>
        val spot = button("Vision", "player-slot-vision player-slot-vision-empty")
        spot.setAttribute("aria-label", "Empty revealed Vision spot")
        slots.appendChild(options.control(spot, visionKey, None))
      }
      if slots.childNodes.length > 0 then playerSummary.appendChild(slots)

      // One row, no headings: two labelled rows cost more height than the pane
      // has, and a relic's square box already says which card is which.
      val cards = element("div", "board-cards")
      cards.setAttribute("aria-label", "Cards in play")
      board.advisers.zipWithIndex.foreach((card, at) => cards.appendChild(
        options.card(CardFace.render(card), card,
          boardCardKeys("adviser-slot", player.playerId, at, card))))
      (board.advisers.size until board.adviserLimit).foreach(at =>
        cards.appendChild(openSlot(options, "denizen", "Empty adviser slot",
          at, BoardOptionViews.key("adviser-slot", s"${player.playerId}:$at"))))
      // A banner matters more than a relic, so it comes first of the two.
      board.banners.foreach(banner =>
        cards.appendChild(BannerFace.place(banner, options,
          BannerFace.festivalSite(value))))
      board.relics.zipWithIndex.foreach((card, at) => cards.appendChild(
        options.card(CardFace.render(card), card,
          boardCardKeys("relic-slot", player.playerId, at, card))))
      section.appendChild(cards)
      LegacyStrip.board(board, options).foreach(section.appendChild)
      }
      panel.appendChild(section)
    }
    panel

  /** Where a board's warbands are, one place a line. `own` is the viewer's
    * board, which is "your" board; any other is "their". */
  private[frontend] def warbandsNote(board: PlayerBoard, own: Boolean): String =
    Vector(s"Warbands on ${if own then "your" else "their"} board: ${board.warbands}",
      s"Warbands on sites: ${board.siteWarbands}",
      s"Warbands in bank: ${board.bankedWarbands}").mkString("\n")

  /** The keys a card on a player board answers to: its slot, and the card
    * itself when the viewer may know which card it is.
    */
  private def boardCardKeys(slot: String, playerId: String, at: Int,
      card: CardDetails): Vector[String] =
    BoardOptionViews.key(slot, s"$playerId:$at") +:
      Option.unless(card.hidden)(BoardOptions.cardKey(card)).toVector

  /** The parked decision's badge for the option on `keys`, drawn inside
    * `target`: what choosing it costs (travel destination and search source
    * designs, ruling 3). */
  private def badged(target: dom.Element, keys: Vector[String],
      options: BoardOptions): Unit =
    options.view(keys).flatMap(_.option.badge).foreach { badge =>
      val chip = text("span", "target-detail-badge", "")
      RulesTextRenderer.cost(badge, chip)
      target.appendChild(chip)
    }

  /** The World pane. `options` draws the parked board decision onto its
    * sites, cards, pawns, banks and banners. The map is one board with the
    * mat's proportions; every piece is placed from `MatLayout`.
    */
  def world(
      value: GameProjection,
      options: BoardOptions,
      submit: Option[GameCommand => Unit] = None
  ): dom.Element =
    val panel = element("section", "panel world")
    panel.setAttribute("aria-label", "The World")
    panel.appendChild(text("h2", "", "The World"))
    // The world deck is a Search's source and Clay Rattle's pile.
    val deckKeys = Vector(BoardOptionViews.key("deck", "world"))
    val deck = deckBody(value.worldDeckCount, value.worldDeckTopCardKind)
    options.decorate(deck, deckKeys)
    badged(deck, deckKeys, options)
    val regions = element("div", "regions")
    value.world.zipWithIndex.foreach { (region, at) =>
      val column = MatLayout.column(region.regionId, at)
      val section = element("section", "region")
      val name = region.regionId match
        case "cradle" => "Cradle"
        case "provinces" => "Provinces"
        case "hinterland" => "Hinterland"
        case other => other
      section.setAttribute("aria-label", name)
      val header = element("div", "region-header")
      header.appendChild(place(text("h3", "region-label printed", name),
        MatLayout.regionName(column)))
      val discardTitle = s"$name discard pile"
      val discardKeys = Vector(BoardOptionViews.key("discard-pile",
        region.regionId))
      val discard = openableDeck(region.discardCount,
        region.discardTopCardKind, discardTitle, region.discardCards)
      badged(discard, discardKeys, options)
      header.appendChild(place(deckFigure("Discard", options.control(discard,
        discardKeys, Option.when(region.discardCount > 0)(discardTitle ->
          (anchor => CardInspection.openCards(
            s"$discardTitle (${region.discardCount})", region.discardCards,
            anchor))))), MatLayout.discard(column)))
      section.appendChild(header)
      val sites = element("div", "sites")
      region.sites.zipWithIndex.foreach { (site, row) =>
        val pawns = value.pawnLocations.filter(_.siteId == site.siteId)
          .map(pawn => pawnMarker(value, pawn.playerId, options))
        sites.appendChild(siteRow(site, column, row, MatLayout.Mat, options,
          SitePowerLinks.of(value, submit), pawns))
      }
      // A slot no site fills is the frame the mat prints for it.
      (0 until region.emptySlots).foreach(extra =>
        sites.appendChild(place(emptySlot(name),
          MatLayout.siteFrame(column, region.sites.size + extra))))
      section.appendChild(sites)
      if region.regionId == "cradle" then
        section.appendChild(cradleFoot(value, deck))
      regions.appendChild(section)
    }
    val board = element("div", "mat-board")
    board.appendChild(regions)
    board.appendChild(favorBanks(value, options))
    panel.appendChild(board)
    panel.appendChild(tableEdge(value, options))
    panel

  /** One site's row on the mat (Chronicle Details design, slice 3): the
    * `article.site` placed at its row inside `within`, then its face,
    * heading, cost badge, `pawns`, festival marker and cards, each placed
    * relative to the row `MatLayout.siteRow(column, row)`. `world()`
    * passes the mat; the Chronicle Details overlay passes the row itself,
    * so the row fills a box of its ratio. `links` opens the site's powers;
    * `options` marks what the parked decision offers. */
  private[frontend] def siteRow(site: GameSite, column: Int, row: Int,
      within: MatRect, options: BoardOptions, links: SitePowerLinks,
      pawns: Vector[dom.Element]): dom.Element =
    val rowRect = MatLayout.siteRow(column, row)
    val faceRect = MatLayout.siteCard(column, row)
    val control = place(element("article", "site site-readonly"), rowRect,
      within)
    control.setAttribute("aria-label", site.label)
    // The site's words are laid over its face, so the stylesheet needs
    // the face's extent in the row.
    val faceBox = faceRect.percentOf(rowRect).toMap
    val style = control.asInstanceOf[dom.html.Element].style
    style.setProperty("--face-left", faceBox("left"))
    style.setProperty("--face-w", faceBox("width"))
    val siteKeys = Vector(BoardOptionViews.key("site", site.siteId))
    options.decorate(control, siteKeys)
    // The card itself: drawn until licensed art takes its place.
    val face = place(element("div", "site-face"), faceRect, rowRect)
    face.setAttribute("aria-hidden", "true")
    control.appendChild(face)
    control.appendChild(siteHeading(site))
    // What reaching this site costs, or why it cannot be reached, comes
    // from the decision that offers it.
    badged(control, siteKeys, options)
    val pawnBox = element("div", "site-pawns")
    pawns.foreach(pawnBox.appendChild)
    if site.festival then pawnBox.appendChild(festivalMarker)
    control.appendChild(pawnBox)
    val details = siteDetails(site, options, links)
    placeCards(details, column, row, rowRect)
    control.appendChild(details)
    control

  /** A pawn on the map. Both forms are in the DOM and the map's scale picks
    * one (ruling R6): the dot and the name at rest, the dot and the
    * initial zoomed out, the dot alone further out. The dot and the
    * initial are for the eye; the name stays the pawn's name. */
  private def pawnMarker(value: GameProjection, playerId: String,
      options: BoardOptions): dom.Element =
    val marker = element("span", "pawn")
    val player = playerReference(value, playerId)
    val colour = playerColorClass(value, playerId)
    val dot = text("span", s"pawn-dot $colour", "●")
    dot.setAttribute("aria-hidden", "true")
    marker.appendChild(dot)
    // The long form: the space and the name, as the pawn reads at rest.
    val long = element("span", "pawn-long")
    long.appendChild(dom.document.createTextNode(" "))
    long.appendChild(player)
    marker.appendChild(long)
    val initial = text("span", s"pawn-initial $colour",
      player.textContent.take(1))
    initial.setAttribute("aria-hidden", "true")
    marker.appendChild(initial)
    HoverTip.set(marker, player.textContent)
    options.decorate(marker, Vector(BoardOptionViews.key("player", playerId)))
    marker

  /** Places `node` at `rect`, each length a percentage of `parent`, which
    * is the rectangle of the element it is positioned in. */
  private def place[E <: dom.Element](node: E, rect: MatRect,
      parent: MatRect = MatLayout.Mat): E =
    node.classList.add("mat-slot")
    val style = node.asInstanceOf[dom.html.Element].style
    rect.percentOf(parent).foreach((property, value) =>
      style.setProperty(property, value))
    node

  /** Stands each of a site's cards in its slot, in the row's order: a
    * relic in the square top of its slot, anything else in the whole of
    * it. */
  private def placeCards(details: dom.Element, column: Int, row: Int,
      rowRect: MatRect): Unit =
    Option(details.querySelector(".site-cards")).foreach { cards =>
      (0 until cards.children.length).foreach { slot =>
        val card = cards.children(slot)
        val relic = card.classList.contains("card-face-relic") ||
          card.querySelector(".card-face-relic") != null
        place(card,
          if relic then MatLayout.relicSlot(column, row, slot)
          else MatLayout.cardSlot(column, row, slot), rowRect)
      }
    }

  /** The Cradle's foot, as the mat prints it: the round wheel, the Visions
    * Drawn track, the relic deck and the world deck. */
  private def cradleFoot(value: GameProjection, deck: dom.Element)
      : dom.Element =
    val row = element("div", "deck-row")
    value.tracks.foreach { track =>
      row.appendChild(roundTracker(value, track))
      row.appendChild(visionsDrawn(track.visionsDrawn))
    }
    // The relic deck first, as the mat prints it left of the world deck.
    row.appendChild(place(deckFigure("Relic deck",
      deckBody(value.relicDeckCount, Some("relic"))), MatLayout.relicDeck))
    row.appendChild(place(deckFigure("World deck", deck),
      MatLayout.worldDeck))
    row

  /** The mat's six cells, each printing its number, and the marker on the
    * cell of the count. A count past the last cell stands on the last. */
  private def visionsDrawn(count: Int): dom.Element =
    val track = element("div", "visions-drawn")
    track.setAttribute("role", "img")
    track.setAttribute("aria-label", s"Visions Drawn $count")
    MatLayout.visionCells.zipWithIndex.foreach((cell, at) =>
      track.appendChild(place(text("span", "vision-cell printed",
        at.toString), cell)))
    track.appendChild(place(text("span", "visions-drawn-label printed",
      "Visions Drawn"), MatLayout.visionsLabel))
    val current = math.max(0, math.min(count, MatLayout.visionCells.size - 1))
    track.appendChild(place(element("span", "vision-marker"),
      MatLayout.visionCells(current)))
    track

  /** What the physical table keeps beside the mat. */
  private def tableEdge(value: GameProjection, options: BoardOptions)
      : dom.Element =
    val edge = element("div", "table-edge")
    LegacyStrip.publicRow(value, options).foreach(edge.appendChild)
    edge.appendChild(sharedBank(value, options))
    // Beside the Shared Bank, while the snapshot carries the Chronicle
    // (Chronicle Details design, slice 4).
    if value.chronicleDetails.nonEmpty then
      edge.appendChild(chronicleDetailsButton)
    edge

  /** Opens the Chronicle Details overlay through the shell's hook. */
  private def chronicleDetailsButton: dom.Element =
    val node = button("Chronicle Details", "chronicle-details-button")
    node.`type` = "button"
    node.setAttribute("aria-haspopup", "dialog")
    node.setAttribute("aria-controls", "chronicle-details")
    node.onclick = _ => ChronicleDetails.open(node)
    node

  /** The festival marker over a site's cards (Foundations design, slice
    * 6): a pill naming it, its power in its hover note. It is a button, as
    * a player's title pill is, so a keyboard reaches it and its note shows
    * on focus, and pressing it opens the power in the inspector. It is no
    * board option. */
  private def festivalMarker: dom.Element =
    val marker = button("Festival", "festival-marker")
    marker.setAttribute("aria-label", "Festival marker")
    HoverTip.set(marker, festivalNote)
    marker.onclick = _ => CardInspection.openText("Festival marker",
      Vector(festivalNote), marker)
    marker

  private[frontend] val festivalNote: String = "Festival: enemies of the " +
    "Darkest Secret's holder cannot place resources on these cards or use " +
    "their powers, unless they're persistent powers."

  /** A site slot of the region `regionName` that holds no site: a frame
    * with nothing in it, and nothing to choose or inspect. */
  private def emptySlot(regionName: String): dom.Element =
    // The mat prints the frame, so its outline is printed content.
    val slot = element("div", "site-slot-empty printed")
    slot.setAttribute("role", "img")
    slot.setAttribute("aria-label", s"Empty site slot, $regionName")
    slot

  private[frontend] final case class RoundSegment(path: String, labelX: Double,
      labelY: Double, markerX: Double, markerY: Double)

  private[frontend] def roundSegment(round: Int): RoundSegment =
    val center = 100.0; val outer = 88.0; val inner = 49.0
    val start = Math.toRadians(-90.0 + (round - 1) * 45.0 + 2.0)
    val end = Math.toRadians(-90.0 + round * 45.0 - 2.0)
    def point(radius: Double, angle: Double) =
      (center + radius * Math.cos(angle), center + radius * Math.sin(angle))
    val (outerStartX, outerStartY) = point(outer, start)
    val (outerEndX, outerEndY) = point(outer, end)
    val (innerEndX, innerEndY) = point(inner, end)
    val (innerStartX, innerStartY) = point(inner, start)
    val middle = (start + end) / 2.0
    val (labelX, labelY) = point(68.0, middle)
    val (markerX, markerY) = point(94.0, middle)
    def n(value: Double) = f"$value%.2f"
    RoundSegment(s"M ${n(outerStartX)} ${n(outerStartY)} " +
      s"A ${n(outer)} ${n(outer)} 0 0 1 ${n(outerEndX)} ${n(outerEndY)} " +
      s"L ${n(innerEndX)} ${n(innerEndY)} " +
      s"A ${n(inner)} ${n(inner)} 0 0 0 ${n(innerStartX)} ${n(innerStartY)} Z",
      labelX, labelY, markerX, markerY)

  private def roundTracker(value: GameProjection,
      track: distantlands.protocol.projection.GameTracksProjection): dom.Element =
    val section = element("section", "round-tracker")
    val first = playerReference(value, track.firstPlayerId)
    // The lock is in the tracker's name as well as in its words, which the
    // far zoomed-out map takes out of sight.
    val lock = Option.when(track.usurperLimited)(
      s"Usurper locked until round ${track.limiterRound}")
    section.setAttribute("aria-label", s"Round ${track.round} of 8; " +
      s"Visions Drawn ${track.visionsDrawn}; " +
      lock.fold("")(words => s"$words; ") +
      s"first player ${first.textContent}")
    val svg = svgElement("svg")
      .asInstanceOf[dom.svg.SVG]
    svg.setAttribute("viewBox", "0 0 200 200")
    svg.setAttribute("role", "img")
    svg.setAttribute("aria-label", s"Eight-segment round tracker, current round ${track.round}")
    (1 to 8).foreach { round =>
      val segment = roundSegment(round)
      val group = svgElement("g")
      group.setAttribute("data-round", round.toString)
      val path = svgElement("path")
      path.setAttribute("d", segment.path)
      // The wheel as printed, every segment in one fill (ruling R8); the
      // current round is a wedge of its own laid over its segment.
      path.setAttribute("class", "round-segment printed")
      val label = svgElement("text")
      label.setAttribute("x", segment.labelX.toString)
      label.setAttribute("y", segment.labelY.toString)
      label.setAttribute("text-anchor", "middle"); label.setAttribute("dominant-baseline", "middle")
      label.setAttribute("class", "round-number printed")
      label.textContent = round.toString
      group.appendChild(path)
      if round == track.round then
        val current = svgElement("path")
        current.setAttribute("d", segment.path)
        current.setAttribute("class", "round-current")
        group.appendChild(current)
      group.appendChild(label)
      if track.usurperLimited && round == track.limiterRound then
        val marker = svgElement("text")
        marker.setAttribute("x", segment.markerX.toString)
        marker.setAttribute("y", segment.markerY.toString)
        marker.setAttribute("text-anchor", "middle"); marker.setAttribute("class", "limiter-marker")
        marker.textContent = "◆"
        lock.foreach(words => HoverTip.set(marker, words))
        group.appendChild(marker)
      svg.appendChild(group)
    }
    section.appendChild(place(svg, MatLayout.roundWheel))
    // The tracker's words, in the clear band the mat leaves over the
    // Cradle's foot.
    val note = place(element("div", "track-note"), MatLayout.trackNote)
    val summary = text("p", "track-summary",
      s"Visions Drawn ${track.visionsDrawn} · First player ")
    summary.appendChild(first)
    note.appendChild(summary)
    // Beside the marker the tracker already draws on that round, rather than
    // in the action panel, which is about this turn rather than the game.
    lock.foreach(words => note.appendChild(text("p", "usurper-notice", words)))
    section.appendChild(note)
    section

  private def favorBanks(value: GameProjection,
      options: BoardOptions): dom.Element =
    val banks = element("div", "favor-banks")
    banks.setAttribute("aria-label", "Favor banks")
    value.favorBanks.foreach { bank =>
      val box = text("span", s"favor-bank suit-${bank.suit}", "")
      // The symbol first, as on a card: a player matching a bank against a
      // card's suit reads the shape, not the word.
      val suit = RulesTextRenderer.glyph(s"suit-${bank.suit}")
      suit.classList.add("printed")
      box.appendChild(suit)
      // The suit's name is printed beside the mat's circle; the count is
      // play. Together they read as before: "Arcane: 3".
      box.appendChild(text("span", "printed", s"${bank.suit.capitalize}: "))
      box.appendChild(text("span", "favor-bank-count", bank.count.toString))
      // The count is favor, drawn as the favor token is everywhere else.
      box.appendChild(RulesTextRenderer.glyph("favor"))
      options.decorate(box,
        Vector(BoardOptionViews.key("favor-bank", bank.suit)))
      MatLayout.favorBank(bank.suit).foreach(rect => place(box, rect))
      banks.appendChild(box)
    }
    banks

  private def sharedBank(value: GameProjection,
      options: BoardOptions): dom.Element =
    val section = element("section", "shared-bank")
    section.appendChild(text("h3", "", "Shared Bank"))
    // The oath and its holder on lines of their own: run together, the
    // holder wrapped mid-phrase in the bank's column.
    value.oathkeeper.foreach { oath =>
      val status = element("p", "oathkeeper-status")
      // The Oath is a printed card: its goal opens the card's words, as the
      // title pill on its holder's board does, so an unheld Oath can be read
      // too (implementation ruling R2).
      val presented = distantlands.protocol.projection
        .OathkeeperPresentation.byGoal.get(oath.goal)
      val title = presented.fold(oath.goal)(_.title)
      val goal = button(title, "oathkeeper-goal card-link")
      goal.`type` = "button"
      goal.onclick = _ => CardInspection.openText(title,
        presented.fold(Vector.empty[String])(_.lines), goal, Some("oath-goal"))
      status.appendChild(goal)
      status.appendChild(text("span", "oathkeeper-holder",
        s"${oath.side.capitalize}: ${oath.holderPlayerId.getOrElse("unheld")}"))
      section.appendChild(status)
    }
    if value.banners.nonEmpty then
      val banners = element("div", "shared-banners")
      value.banners.foreach(banner =>
        banners.appendChild(BannerFace.place(banner, options,
          BannerFace.festivalSite(value))))
      section.appendChild(banners)
    // The Reliquary, facedown, while it holds a relic (Legacy in-play
    // powers ruling 9): a face only where its viewer may peek. Each relic
    // is keyed by its card when known, so Golden Key's question marks it.
    if value.reliquary.nonEmpty then
      val reliquary = element("div", "shared-reliquary")
      reliquary.setAttribute("aria-label", "Reliquary")
      reliquary.appendChild(text("h4", "shared-reliquary-label", "Reliquary"))
      value.reliquary.foreach(card => reliquary.appendChild(options.card(
        CardFace.render(card), card,
        Option.unless(card.hidden)(BoardOptions.cardKey(card)).toVector)))
      section.appendChild(reliquary)
    section
