package oathdigital.gameplay.chronicle

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._

/** Crown Chancellor, the Sun's sixth step (CR p. 36, Chronicle ruling 10).
  * The winner becomes Chancellor and takes the Grand Scepter. An Exile's
  * win breaks the Empire; a Citizen's starts a new dynasty. */
object ChronicleCrown:
  def operations(ready: ReadyGame, winner: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      player <- ready.game.current.players.find(_.player == winner).toRight(
        OathViolation.InvalidEventOrder(s"${winner.value} is not seated"))
      lineage <- ready.game.campaign.lineages.get(player.lineage).toRight(
        OathViolation.InvalidEventOrder(
          s"${player.lineage.value} has no lineage state"))
    yield scepter(ready, winner) ++ (lineage.role match
      case Role.Exile => empireBreaks(ready, player)
      case Role.Citizen => newDynasty(ready, player.lineage)
      case Role.Chancellor => Vector.empty)

  /** Every lineage's current role: seated ones from the live campaign, the
    * rest from the baseline as this Chronicle has changed them. */
  private def roles(ready: ReadyGame): Vector[(LineageId, Role)] =
    val campaign = ready.game.campaign
    val seated = campaign.lineages.values.toVector.map(l => l.id -> l.role)
    val boxed = campaign.baseline.lineages
      .filterNot(l => campaign.lineages.contains(l.id))
      .map(l => l.id -> campaign.boxRoles.getOrElse(l.id, l.role))
    (seated ++ boxed).sortBy(_._1.value)

  private def scepter(ready: ReadyGame, winner: PlayerId)
      : Vector[CoreOperation] =
    val id = TheGrandScepterCard.id
    val current = ready.game.current
    val held = current.players.find(_.relics.exists(_.id == id))
      .map(state => Location.PlayArea(state.player))
    val atSite = current.map.sites.collectFirst {
      case (site, state) if state.relics.exists(_.id == id) =>
        Location.Site(site)
    }
    held.orElse(atSite) match
      case Some(Location.PlayArea(`winner`)) => Vector.empty
      case Some(from) => Vector(Move(Piece.Card(id), PositionedLocation(from),
        PositionedLocation(Location.PlayArea(winner)),
        resultingOrientation = Some(Orientation.FaceUp)))
      case None => Vector(GrandScepterFromBox(winner))

  /** If an Exile won: the former Chancellor and every Citizen, even in the
    * box, become Exiles; the Reliquary is discarded; Imperial warbands leave
    * the map without bandits taking their place; each site the winner rules
    * keeps one Imperial warband. The winner's newly dormant legacies would
    * become active here; they arrive with the Legacies slice.
    *
    * The winner's board warbands go back to their own bank first: the
    * winner swaps boards for the Chancellor's, and once they are Imperial
    * `PlayerForceKind` would count that board as Imperial warbands. */
  private def empireBreaks(ready: ReadyGame, player: PlayerState)
      : Vector[CoreOperation] =
    val current = ready.game.current
    val own = ForceKind.Exile(player.lineage)
    val exiled = roles(ready).collect {
      case (lineage, role) if role.isImperial && lineage != player.lineage =>
        RoleChanged(lineage, Role.Exile)
    }
    val reliquary = ready.game.campaign.reliquary.map(id =>
      Move(Piece.Card(id), PositionedLocation(Location.Reliquary),
        PositionedLocation(Location.SetAsideRelics)))
    def at(site: SiteId) = PositionedLocation(Location.Site(site))
    val forces = current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(site -> _.forces))
    val imperialRemoved = forces.collect {
      case (site, SiteForces.Occupied(ForceKind.Imperial, n)) =>
        Kill(Piece.Warbands(ForceKind.Imperial, n), at(site))
    }
    val board = Option.when(player.board.warbands > 0)(Move(
      Piece.Warbands(own, player.board.warbands),
      PositionedLocation(Location.PlayArea(player.player)),
      PositionedLocation(Location.WarbandBank(own)))).toVector
    val replaced = forces.flatMap {
      case (site, SiteForces.Occupied(`own`, n)) =>
        Option.when(n > 1)(Kill(Piece.Warbands(own, n - 1), at(site)))
          .toVector :+ Replace(Piece.Warbands(own, 1),
            Piece.Warbands(ForceKind.Imperial, 1), at(site))
      case _ => Vector.empty
    }
    exiled ++ reliquary ++
      Vector(ImperialWarbandsTaken(current.map.inPlay.size)) ++
      imperialRemoved ++ board ++ replaced :+
      RoleChanged(player.lineage, Role.Chancellor)

  /** If a Citizen won: the former Chancellor becomes a Citizen. */
  private def newDynasty(ready: ReadyGame, winner: LineageId)
      : Vector[CoreOperation] =
    roles(ready).collect {
      case (lineage, Role.Chancellor) => RoleChanged(lineage, Role.Citizen)
    } :+ RoleChanged(winner, Role.Chancellor)
