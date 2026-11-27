package distantlands.frontend

import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom
import ServerUiSupport._

/** What stands on a site's card in play, and the shield the card prints
  * its powers in (game art rulings R48 to R55). Each is a box `MatLayout`
  * places on the card, and every length inside it is a share of that box,
  * so a piece is the same share of its site at every zoom, with art on and
  * off. The markup is the same in both; art adds its own nodes alone. */
private[frontend] object SitePieces:

  /** A pawn on a site: its piece, a disc of its player's colour that holds
    * the pawn's picture where the host's art has one, and its player's
    * name under it. The name is the pawn's name; the piece is for the
    * eye. A decision that offers the player marks the whole pawn. */
  def pawn(value: GameProjection, playerId: String,
      options: BoardOptions): dom.Element =
    val marker = element("span", "pawn")
    val player = playerReference(value, playerId)
    val piece = element("span",
      s"pawn-piece ${playerColorClass(value, playerId)}")
    piece.setAttribute("aria-hidden", "true")
    marker.appendChild(piece)
    ArtLayer.attach(piece, value.players.find(_.playerId == playerId)
      .flatMap(player => ArtCatalog.pawn(player.color.key)))
    val name = element("span", "pawn-name")
    name.appendChild(player)
    marker.appendChild(name)
    HoverTip.set(marker, player.textContent)
    options.decorate(marker, Vector(BoardOptionViews.key("player", playerId)))
    marker

  /** The row a site's pawns stand in, from its left: `MatLayout`'s six
    * places, of which each pawn takes one whoever else is there (R48). */
  def pawnRow(pawns: Vector[dom.Element]): dom.Element =
    val row = element("div", "site-pawns")
    pawns.foreach(row.appendChild)
    row

  /** The row a site's warbands stand in: the ruler's piece and how many,
    * "x2". The site holds one force (R49); the row is there, empty,
    * when it holds none, so the site's shape does not depend on it. The
    * force's words ("Red Exile x2") are its name and its hover note. */
  def warbandRow(site: GameSite): dom.Element =
    val row = element("div", "site-warbands")
    site.forces.foreach { forces =>
      val group = element("span", s"site-force ${forceCssClass(forces)}")
      group.setAttribute("role", "img")
      group.setAttribute("aria-label", forceText(forces))
      HoverTip.set(group, forceText(forces))
      forces match
        case exile: SiteForces.Exile =>
          group.setAttribute("data-ruler-kind", "player")
          group.setAttribute("data-ruler-player-id", exile.rulerPlayerId)
        case _: SiteForces.Imperial =>
          group.setAttribute("data-ruler-kind", "empire")
        case _: SiteForces.Bandit =>
          group.setAttribute("data-ruler-kind", "bandit")
      val piece = RulesTextRenderer.warband(forceKey(forces))
      piece.setAttribute("aria-hidden", "true")
      group.appendChild(piece)
      val count = text("span", "site-force-count", s"x${forces.count}")
      count.setAttribute("aria-hidden", "true")
      group.appendChild(count)
      row.appendChild(group)
    }
    row

  /** The shield a site's card prints its powers in, for the site in
    * `column` and `row`: one button a power, on the part of the shield
    * the card prints that power in (R55), in the projection's order for
    * reading and the keyboard. A button is not printed; its label is
    * (ruling R9), so over the site's art the label hides and the button
    * stays on the print. Pressing it opens the power in the inspector,
    * with the viewer's consent grid from `links`, and leaves the site's
    * own click alone. */
  def powerShield(site: GameSite, links: SitePowerLinks, column: Int,
      row: Int): dom.Element =
    val shield = element("div", "site-shield")
    val within = MatLayout.sitePowerShield(column, row)
    val regions = MatLayout.sitePowerRegions(site.powers.map(_.kind), column,
      row)
    site.powers.zip(regions).foreach { (power, region) =>
      val item = button("", "site-power")
      item.setAttribute("aria-label", power.label)
      item.appendChild(text("span", "printed", power.label))
      HoverTip.set(item, power.description.fold(power.label)(
        description => s"${power.label}: $description"))
      item.onclick = (event: dom.MouseEvent) =>
        event.stopPropagation()
        CardInspection.openPower(power.label, power.description.toVector,
          links.grid(power), item)
      shield.appendChild(WorldBoardRenderer.place(item, region, within))
    }
    shield
