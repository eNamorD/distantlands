package distantlands.gameplay.operations

import distantlands.model._

/** Pawn and banner moves. The guard threads the pawns' sites and the
  * banners' holders through one operation's leaves so two moves of one piece
  * in one operation are checked in sequence.
  */
private[operations] object BoardControlOperations:
  import OperationError._
  import OperationStateAdapter.{bannerHolder, playerState, siteState}
  import OperationStateWrites.updatePlayer

  private final case class MovedPieces(
      pawnSites: Map[PlayerId, Option[SiteId]],
      bannerHolders: Map[Banner, Option[PlayerId]]
  )

  def pawnAndBannerViolations(
      ready: ReadyGame,
      leaves: Vector[Operation]
  ): Vector[OperationError] =
    val pawnSites = ready.game.current.players.iterator.map { player =>
      player.player -> player.pawnSite
    }.toMap
    val bannerHolders = Map(
      Banner.PeoplesFavor -> bannerHolder(ready, Banner.PeoplesFavor),
      Banner.DarkestSecret -> bannerHolder(ready, Banner.DarkestSecret)
    )
    val (reasons, _) = leaves.foldLeft[(Vector[OperationError],
      MovedPieces)]((Vector.empty, MovedPieces(pawnSites, bannerHolders))):
      case ((result, state), Move(Piece.Pawn(player), from, to, _, _)) =>
        val (violations, updated) =
          pawnMoveViolation(ready, player, from.location, to.location, state)
        (result ++ violations, updated)
      case ((result, state), Move(Piece.Banner(banner), from, to, _, _)) =>
        val (violations, updated) =
          bannerMoveViolation(ready, banner, from.location, to.location, state)
        (result ++ violations, updated)
      case ((result, state), _) => (result, state)
    reasons

  private def pawnMoveViolation(
      ready: ReadyGame,
      player: PlayerId,
      from: Location,
      to: Location,
      state: MovedPieces
  ): (Vector[OperationError], MovedPieces) = (from, to) match
    case (Location.PlayArea(source), Location.Site(destination))
        if source == player =>
      playerState(ready, player) match
        case Left(error) => (Vector(error), state)
        case Right(_) =>
          val located = state.pawnSites.getOrElse(player, None)
          if located.nonEmpty then
            (Vector(MissingPiece(Piece.Pawn(player), from)), state)
          else siteState(ready, destination) match
            case Left(error) => (Vector(error), state)
            case Right(_) => (Vector.empty, state.copy(
              pawnSites = state.pawnSites.updated(player, Some(destination))))
    case (Location.Site(source), Location.Site(destination)) =>
      playerState(ready, player) match
        case Left(error) => (Vector(error), state)
        case Right(_) =>
          val located = state.pawnSites.getOrElse(player, None)
          if !located.contains(source) then
            (Vector(MissingPiece(Piece.Pawn(player), from)), state)
          else siteState(ready, destination) match
            case Left(error) => (Vector(error), state)
            case Right(_) => (Vector.empty, state.copy(
              pawnSites = state.pawnSites.updated(player, Some(destination))))
    case _ =>
      (Vector(IncompatibleLocation(Piece.Pawn(player), to)), state)

  private def bannerMoveViolation(
      ready: ReadyGame,
      banner: Banner,
      from: Location,
      to: Location,
      state: MovedPieces
  ): (Vector[OperationError], MovedPieces) = (from, to) match
    case (Location.PlayArea(source), Location.PlayArea(destination)) =>
      playerState(ready, destination) match
        case Left(error) => (Vector(error), state)
        case Right(_) =>
          val holder = state.bannerHolders.getOrElse(banner, None)
          if !holder.contains(source) then
            (Vector(MissingPiece(Piece.Banner(banner), from)), state)
          else (Vector.empty, state.copy(
            bannerHolders = state.bannerHolders.updated(banner,
              Some(destination))))
    case (Location.SharedBank, Location.PlayArea(destination)) =>
      playerState(ready, destination) match
        case Left(error) => (Vector(error), state)
        case Right(_) =>
          val holder = state.bannerHolders.getOrElse(banner, None)
          if holder.isEmpty then
            (Vector.empty, state.copy(
              bannerHolders = state.bannerHolders.updated(banner,
                Some(destination))))
          else (Vector(InsufficientPieces(Piece.Banner(banner), from, 0)),
            state)
    // Back to the shared bank, which holds a banner nobody holds.
    case (Location.PlayArea(source), Location.SharedBank) =>
      val holder = state.bannerHolders.getOrElse(banner, None)
      if !holder.contains(source) then
        (Vector(MissingPiece(Piece.Banner(banner), from)), state)
      else (Vector.empty, state.copy(
        bannerHolders = state.bannerHolders.updated(banner, None)))
    case _ =>
      (Vector(IncompatibleLocation(Piece.Banner(banner), to)), state)

  def applyPawnAndBannerMoves(
      ready: ReadyGame,
      leaves: Vector[Operation]
  ): Either[OperationError, ReadyGame] =
    leaves.foldLeft[Either[OperationError, ReadyGame]](Right(ready)):
      case (result, Move(Piece.Pawn(player), from, to, _, _)) =>
        result.flatMap(movePawn(_, player, from.location, to.location))
      case (result, Move(Piece.Banner(banner), from, to, _, _)) =>
        result.flatMap(moveBanner(_, banner, from.location, to.location))
      case (result, _) => result

  private def movePawn(ready: ReadyGame, player: PlayerId,
      from: Location, to: Location): Either[OperationError, ReadyGame] =
    (from, to) match
      // Whether the pawn is where `from` says is the guard's
      // (pawnMoveViolation); by the time this runs it is.
      case (Location.PlayArea(source), Location.Site(destination))
          if source == player =>
        siteState(ready, destination).flatMap(_ =>
          updatePlayer(ready, player)(_.copy(pawnSite = Some(destination))))
      case (Location.Site(_), Location.Site(destination)) =>
        siteState(ready, destination).flatMap(_ =>
          updatePlayer(ready, player)(_.copy(pawnSite = Some(destination))))
      case _ => Left(IncompatibleLocation(Piece.Pawn(player), to))

  private def moveBanner(ready: ReadyGame, banner: Banner,
      from: Location, to: Location): Either[OperationError, ReadyGame] =
    (from, to) match
      // Who holds the banner is the guard's (bannerMoveViolation).
      case (Location.PlayArea(_), Location.PlayArea(destination)) =>
        playerState(ready, destination).map(_ =>
          setBannerHolder(ready, banner, destination))
      case (Location.SharedBank, Location.PlayArea(destination)) =>
        playerState(ready, destination).map(_ =>
          setBannerHolder(ready, banner, destination))
      case (Location.PlayArea(_), Location.SharedBank) =>
        Right(holdBanner(ready, banner, None))
      case _ => Left(IncompatibleLocation(Piece.Banner(banner), to))

  /** The festival marker over `site`, or off the map. It goes only on a
    * site on the map, only while the Festival face is up, and comes off
    * only when it is on the map. */
  def setFestivalMarker(ready: ReadyGame, site: Option[SiteId])
      : Either[OperationError, ReadyGame] =
    val secret = ready.game.current.banners.darkestSecret
    site match
      case Some(at) if !ready.game.current.map.inPlay.contains(at) =>
        Left(UnknownSite(at))
      case Some(_) if secret.active != DarkestSecretFace.Festival =>
        Left(InvalidDescription(
          "the festival marker is placed only on the Festival face"))
      case None if secret.festival.isEmpty =>
        Left(InvalidDescription("the festival marker is not on the map"))
      case _ => Right(ready.updateCurrent(current => current.copy(
        banners = current.banners.copy(darkestSecret =
          secret.copy(festival = site)))))

  /** `banner` on its other face; the Festival marker comes off with the
    * Festival face (Legacy in-play powers ruling 20). */
  def flipBanner(ready: ReadyGame, banner: Banner): ReadyGame =
    ready.updateCurrent(current => current.copy(banners = banner match
      case Banner.PeoplesFavor =>
        val held = current.banners.peoplesFavor
        current.banners.copy(peoplesFavor = held.copy(active =
          held.active match
            case PeoplesFavorFace.Mob => PeoplesFavorFace.GrandCouncil
            case PeoplesFavorFace.GrandCouncil => PeoplesFavorFace.Mob))
      case Banner.DarkestSecret =>
        val held = current.banners.darkestSecret
        current.banners.copy(darkestSecret = held.active match
          case DarkestSecretFace.WanderingFlame =>
            held.copy(active = DarkestSecretFace.Festival)
          case DarkestSecretFace.Festival =>
            held.copy(active = DarkestSecretFace.WanderingFlame,
              festival = None))))

  private def setBannerHolder(ready: ReadyGame, banner: Banner,
      player: PlayerId): ReadyGame =
    holdBanner(ready, banner, Some(player))

  /** `banner` held by `holder`, or by nobody, in the shared bank. */
  private def holdBanner(ready: ReadyGame, banner: Banner,
      holder: Option[PlayerId]): ReadyGame =
    val current = ready.game.current
    val banners = banner match
      case Banner.PeoplesFavor => current.banners.copy(
        peoplesFavor = current.banners.peoplesFavor.copy(holder = holder))
      case Banner.DarkestSecret => current.banners.copy(
        darkestSecret = current.banners.darkestSecret.copy(holder = holder))
    ready.copy(game = ready.game.copy(current = current.copy(banners = banners)))
