package distantlands.gameplay.operations

import distantlands.gameplay.EmpireEndings
import distantlands.model._

/** Writes of the Chronicle's own primitives (Chronicle steps design, "Live
  * state extended"). */
private[operations] object ChronicleStateOperations:
  import OperationError.InvalidDescription

  /** A seated lineage's entry in this game's open scorepad row, if
    * Chronicler or Score Lineage Points has opened it, takes the new role
    * too: an entry's `endingRole` follows its lineage's role until the
    * Chronicle ends, since Chronicler can open the row in the Sun, before
    * the Crown (Legacy Chronicle powers implementation ruling R3). */
  def roleChanged(ready: ReadyGame, lineage: LineageId, role: Role)
      : Either[OperationError, ReadyGame] =
    val campaign = ready.game.campaign
    campaign.lineages.get(lineage) match
      case Some(state) => Right(ready.updateCampaign(_.copy(
        lineages = campaign.lineages.updated(lineage, state.copy(role = role)),
        era = withEndingRole(campaign, lineage, role))))
      case None if campaign.baseline.lineages.exists(_.id == lineage) =>
        Right(ready.updateCampaign(_.copy(
          boxRoles = campaign.boxRoles.updated(lineage, role))))
      case None => Left(InvalidDescription(
        s"no lineage ${lineage.value} to change the role of"))

  /** `campaign`'s era with `lineage`'s entry in this game's row, when the
    * row is open, ending as `role`. */
  private def withEndingRole(campaign: CampaignState, lineage: LineageId,
      role: Role): EraState =
    val era = campaign.era
    if era.games.size <= campaign.baseline.era.games.size then era
    else era.copy(games = era.games.init :+ EraGame(era.games.last.entries
      .map(entry => if entry.lineage == lineage then
        entry.copy(endingRole = role) else entry)))

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
    * and its live role; a later role change updates it (`roleChanged`), so
    * the row records the roles after the Crown (ruling 19). */
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

  /** Only an altered face turns back: restoring a normal face would be a
    * doubled or reordered journal. */
  def foundationRestored(ready: ReadyGame, number: FoundationNumber)
      : Either[OperationError, ReadyGame] =
    val foundations = ready.game.campaign.foundations
    Either.cond(
      foundations.get(number).exists(_.face == FoundationFace.Altered),
      ready.updateCampaign(_.copy(foundations = foundations.updated(number,
        FoundationState(FoundationFace.Normal, Set.empty)))),
      InvalidDescription(s"Foundation $number is not altered"))

  /** Foundation `number` altered, with `source` among its alteration
    * sources. A source the face already lists is a doubled operation. */
  def foundationAltered(ready: ReadyGame, number: FoundationNumber,
      source: LegacyId): Either[OperationError, ReadyGame] =
    val foundations = ready.game.campaign.foundations
    val sources = foundations.get(number).fold(Set.empty[LegacyId])(
      _.alterationSources)
    Either.cond(!sources(source),
      ready.updateCampaign(_.copy(foundations = foundations.updated(number,
        FoundationState(FoundationFace.Altered, sources + source)))),
      InvalidDescription(
        s"Foundation $number is already altered by ${source.value}"))

  /** `legacies` join the scored set; one already in it, or named twice, is
    * a doubled scoring. */
  def legaciesScored(ready: ReadyGame, legacies: Vector[LegacyId])
      : Either[OperationError, ReadyGame] =
    val scored = ready.game.current.scoredLegacies
    legacies.find(scored) match
      case Some(legacy) => Left(InvalidDescription(
        s"${legacy.value} has already scored this game"))
      case None if legacies.distinct.size != legacies.size =>
        Left(InvalidDescription("a scoring names each legacy once"))
      case None => Right(ready.updateCurrent(current => current.copy(
        scoredLegacies = current.scoredLegacies ++ legacies)))

  /** `legacies` join the kept-dormant set (Stone Baths); one already in it,
    * or named twice, is a doubled keep. */
  def legaciesKeptDormant(ready: ReadyGame, legacies: Vector[LegacyId])
      : Either[OperationError, ReadyGame] =
    val kept = ready.game.current.keptDormant
    legacies.find(kept) match
      case Some(legacy) => Left(InvalidDescription(
        s"${legacy.value} is already kept dormant"))
      case None if legacies.distinct.size != legacies.size =>
        Left(InvalidDescription("a keep names each legacy once"))
      case None => Right(ready.updateCurrent(current => current.copy(
        keptDormant = current.keptDormant ++ legacies)))

  /** `relics` turn facedown in their holders' play areas, and every
    * viewer's record of them is forgotten (R28). The Grand Scepter and a
    * relic no player holds are refused. */
  def relicsHidden(ready: ReadyGame, relics: Vector[RelicId])
      : Either[OperationError, ReadyGame] =
    val held = ready.game.current.players.flatMap(_.relics.map(_.id)).toSet
    relics.find(relic => relic == TheGrandScepterCard.id || !held(relic))
      match
        case Some(relic) => Left(InvalidDescription(
          s"${relic.value} is not a held relic to hide"))
        case None =>
          val hidden = relics.toSet
          Right(ready.updateCurrent(current => current.copy(players =
            current.players.map(state => state.copy(relics = state.relics.map(
              relic => if hidden(relic.id) then
                relic.copy(orientation = Orientation.FaceDown) else relic)))))
            .copy(knowledge = relics.foldLeft(ready.knowledge)(
              CardKnowledgeMoves.forget)))

  /** `relic` from `player`'s relics to their lineage's starting relics
    * (Heirloom; R20). The Grand Scepter, a relic the player does not hold
    * and a relic with tokens are refused. */
  def startingRelicKept(ready: ReadyGame, player: PlayerId, relic: RelicId)
      : Either[OperationError, ReadyGame] =
    val held = ready.game.current.player(player)
      .flatMap(_.relics.find(_.id == relic))
    val lineage = ready.game.current.player(player).map(_.lineage)
      .filter(ready.game.campaign.lineages.contains)
    (held, lineage) match
      case _ if relic == TheGrandScepterCard.id => Left(InvalidDescription(
        "the Grand Scepter is never kept as a starting relic"))
      case (None, _) | (_, None) => Left(InvalidDescription(
        s"${player.value} holds no ${relic.value} to keep"))
      case (Some(state), _) if !state.tokens.isEmpty => Left(
        InvalidDescription(s"${relic.value} still holds tokens"))
      case (Some(_), Some(id)) => Right(ready.updateCurrent(current =>
        current.copy(players = current.players.map(state =>
          if state.player == player then
            state.copy(relics = state.relics.filterNot(_.id == relic))
          else state))).updateCampaign(campaign => campaign.copy(lineages =
        campaign.lineages.updatedWith(id)(_.map(state => state.copy(
          startingRelics = state.startingRelics :+ relic))))))

  /** The game's result becomes `winner`'s win of `kind` (Royal Line;
    * R39). A game with no result, a winner not seated and a kind that is
    * not an Empire win are refused. */
  def winnerNamed(ready: ReadyGame, winner: PlayerId, kind: VictoryKind)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    if current.result.isEmpty then
      Left(InvalidDescription("a game with no result has no winner to name"))
    else if !current.players.exists(_.player == winner) then
      Left(InvalidDescription(s"${winner.value} is not seated"))
    else if !EmpireEndings.empireWins(kind) then
      Left(InvalidDescription(s"${kind.key} is not an Empire win"))
    else Right(ready.updateCurrent(_.copy(result =
      Some(GameResult(winner, kind)))))

