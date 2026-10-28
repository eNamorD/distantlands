package oathdigital.gameplay.operations

import oathdigital.model._

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

  def imperialWarbandsTaken(ready: ReadyGame, count: Int): ReadyGame =
    ready.copy(banks = ready.banks.copy(warbandSupply =
      ready.banks.warbandSupply.updatedWith(ForceKind.Imperial)(
        _.orElse(Some(count)))))

  def lineagePointsScored(ready: ReadyGame, lineage: LineageId, points: Int)
      : ReadyGame = ready.updateCampaign(campaign => campaign.copy(
    era = campaign.era.copy(lineageScores = campaign.era.lineageScores
      .updated(lineage,
        campaign.era.lineageScores.getOrElse(lineage, 0) + points))))

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
