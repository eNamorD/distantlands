package distantlands.gameplay.operations

import distantlands.model._

/** Writes of the Chronicle's own primitives (Chronicle steps design, "Live
  * state extended"). */
private[operations] object ChronicleStateOperations:
  import OperationError.InvalidDescription

  def roleChanged(ready: ReadyGame, lineage: LineageId, role: Role)
      : Either[OperationError, ReadyGame] =
    val campaign = ready.game.campaign
    campaign.lineages.get(lineage) match
      case Some(state) => Right(ready.updateCampaign(_.copy(
        lineages = campaign.lineages.updated(lineage, state.copy(role = role)))))
      case None if campaign.baseline.lineages.exists(_.id == lineage) =>
        Right(ready.updateCampaign(_.copy(
          boxRoles = campaign.boxRoles.updated(lineage, role))))
      case None => Left(InvalidDescription(
        s"no lineage ${lineage.value} to change the role of"))

  def taskAssigned(ready: ReadyGame, task: ChronicleTask, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    Either.cond(ready.game.current.players.exists(_.player == player),
      ready.updateCurrent(current => current.copy(
        chronicleTasks = current.chronicleTasks.updated(task, player))),
      InvalidDescription(s"${player.value} is not seated to take a task"))

  /** A broken card index counts as the Scepter being absent; other checks
    * reject that state. */
  def scepterFromBox(ready: ReadyGame, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    val scepter = TheGrandScepterCard.id
    val inPlay = ready.game.cardIndex.toOption
      .exists(_.locationOf(scepter).nonEmpty)
    if inPlay then Left(InvalidDescription(
      "the Grand Scepter is not in the box"))
    else if !ready.game.current.players.exists(_.player == player) then
      Left(InvalidDescription(s"${player.value} is not seated"))
    else Right(ready.updateCurrent(current => current.copy(
      players = current.players.map(state =>
        if state.player == player then state.copy(relics = state.relics :+
          RelicState(scepter, Orientation.FaceUp, Tokens.empty))
        else state))))

  /** None of `cards` may be in play, and none twice. A broken card index counts as every
    * card being absent; other checks reject that state. */
  def suitedReserveFromBox(ready: ReadyGame, suit: Suit,
      cards: Vector[DenizenId]): Either[OperationError, ReadyGame] =
    val inPlay = ready.game.cardIndex.toOption.fold(Set.empty[CardId])(_.ids)
    cards.find(inPlay.contains) match
      case Some(card) => Left(InvalidDescription(
        s"${card.value} is in play, not in the ${suit.key} box"))
      case None if cards.distinct.size != cards.size => Left(
        InvalidDescription(s"the ${suit.key} box holds each card once"))
      case None => Right(ready.updateCampaign(campaign => campaign.copy(
        suitedReserves = campaign.suitedReserves.updated(suit,
          campaign.suitedReserves.getOrElse(suit, Vector.empty) ++ cards))))

  def imperialWarbandsTaken(ready: ReadyGame, count: Int): ReadyGame =
    ready.copy(banks = ready.banks.copy(warbandSupply =
      ready.banks.warbandSupply.updatedWith(ForceKind.Imperial)(
        _.orElse(Some(count)))))

  /** Raises an existing supply of `kind` by `count` (Chronicle rulings 20
    * and 22). */
  def warbandsAdded(ready: ReadyGame, kind: ForceKind, count: Int)
      : Either[OperationError, ReadyGame] =
    ready.banks.warbandSupply.get(kind).toRight(
      InvalidDescription(s"there is no $kind supply to add to"))
      .map(supply => ready.copy(banks = ready.banks.copy(warbandSupply =
        ready.banks.warbandSupply.updated(kind, supply + count))))

  /** Only a seated lineage scores (gifts go to seated players, Chronicle
    * ruling 13). The game's first points open its scorepad row: an entry
    * per seated lineage, in seating order, at its total before this game
    * and its live role, which the Sun has already crowned (ruling 19). */
  def lineagePointsScored(ready: ReadyGame, lineage: LineageId, points: Int)
      : Either[OperationError, ReadyGame] =
    val campaign = ready.game.campaign
    if !campaign.lineages.contains(lineage) then
      Left(InvalidDescription(s"no seated lineage ${lineage.value} to score"))
    else
      val opened = campaign.era.games.size > campaign.baseline.era.games.size
      val era = if opened then campaign.era else campaign.era.copy(games =
        campaign.era.games :+ EraGame(ready.game.current.players
          .map(_.lineage).filter(campaign.lineages.contains).map(id =>
            EraEntry(id, campaign.era.score(id), campaign.lineages(id).role))))
      val game = era.games.last
      Right(ready.updateCampaign(_.copy(era = era.copy(games =
        era.games.init :+ EraGame(game.entries.map(entry =>
          if entry.lineage == lineage then
            entry.copy(totalScore = entry.totalScore + points)
          else entry))))))

  def edificeBuilt(ready: ReadyGame, edifice: EdificeId, site: SiteId)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    val inDeck = current.commonCards.edificeDeck.contains(edifice)
    val setAside = current.setAsideEdifices.contains(edifice)
    if !inDeck && !setAside then Left(InvalidDescription(
      s"${edifice.value} is neither in the edifice deck nor set aside"))
    else current.map.sites.get(site).toRight(InvalidDescription(
        s"${site.value} is not in play")).map(state =>
      ready.updateCurrent(_.copy(
        commonCards = current.commonCards.copy(
          edificeDeck = current.commonCards.edificeDeck.filterNot(_ == edifice)),
        setAsideEdifices = current.setAsideEdifices.filterNot(_ == edifice),
        map = current.map.copy(sites = current.map.sites.updated(site,
          state.copy(denizens = state.denizens :+
            EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))))))
