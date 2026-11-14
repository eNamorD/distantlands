package distantlands.gameplay.operations

import distantlands.model._

/** The non-move mutation of every Oath leaf, one exhaustive match per
  * operation family (operation families design, "The applier").
  * [[OperationApplication]] calls [[apply]] for each leaf after its move
  * passes, so a new leaf fails to compile here until it says what it
  * changes, or that it changes nothing in this step.
  *
  * Composites are not leaves: [[Operation.flatten]] never yields them, and
  * [[PrimitiveOperation]] (`CoreOperation & LeafOperation`) excludes them,
  * so no arm names them.
  */
private[operations] object LeafApplication:
  def apply(ready: ReadyGame,
      leaf: PrimitiveOperation): Either[OperationError, ReadyGame] =
    leaf match
      case leaf: PieceOperation => piece(ready, leaf)
      case leaf: ResourceOperation => resource(ready, leaf)
      case leaf: CardOperation => card(ready, leaf)
      case leaf: DiceOperation => dice(ready, leaf)
      case leaf: TurnOperation => turn(ready, leaf)
      case leaf: ChronicleOperation => chronicle(ready, leaf)

  private def piece(ready: ReadyGame, leaf: PieceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    // The counted, pawn-and-banner and card passes applied it.
    case _: Move => Right(ready)

  private def resource(ready: ReadyGame,
      leaf: ResourceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case FlipSecrets(player, amount, from, to) =>
      ResourceOperations.flipPlayerSecrets(ready, player, amount, from, to)
    case SpendSupply(player, amount, _) =>
      TurnStateOperations.adjustSupply(ready, player, -amount)
    case GainSupply(player, amount) =>
      TurnStateOperations.adjustSupply(ready, player, amount)

  private def card(ready: ReadyGame, leaf: CardOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case Flip(id, at, orientation) =>
      CardFaceOperations.flipCard(ready, id, at, orientation)
    case Peek(viewer, id, at) => CardFaceOperations.peek(ready, viewer, id, at)
    case Shuffle(pile, order) => PileOperations.shuffle(ready, pile, order)
    // The card pass applied it.
    case _: Bury => Right(ready)

  private def dice(ready: ReadyGame, leaf: DiceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case ModifyDicePool(pool, delta, _) =>
      TurnStateOperations.adjustDicePool(ready, pool, delta)
    case ModifyRollOutcome(pool, skulls, score) =>
      Right(TurnStateOperations.modifyRollOutcome(ready, pool, skulls, score))
    case Reroll(pool, indices, faces) =>
      RollOperations.reroll(ready, pool, indices, faces)
    case SetDieFaces(pool, faces) => RollOperations.setFaces(ready, pool, faces)
    // Records that a roll happens; the walker or the next command supplies
    // its faces.
    case _: Roll => Right(ready)

  private def turn(ready: ReadyGame, leaf: TurnOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case AdvanceVisionsDrawn => TurnStateOperations.advanceVisionsDrawn(ready)
    case AdvanceRound => TurnStateOperations.advanceRound(ready)
    case UsurperLimiterRemoved =>
      TurnStateOperations.usurperLimiterRemoved(ready)
    case RecordPowerUse(power) =>
      Right(TurnStateOperations.recordPowerUse(ready, power))
    case Defer(key, ops) => Right(TurnStateOperations.defer(ready, key, ops))
    case TakeDeferred(key) => Right(TurnStateOperations.takeDeferred(ready, key))
    case ActAsIf(site) => Right(TurnStateOperations.actAsIf(ready, site))
    case ActAsIfAdvisers(player) =>
      TurnStateOperations.actAsIfAdvisers(ready, player)
    case TrackPawn(holder, target) =>
      TurnStateOperations.trackPawn(ready, holder, target)
    case ClearTrackedPawn(holder) =>
      TurnStateOperations.clearTrackedPawn(ready, holder)
    case SetFestivalMarker(site) =>
      BoardControlOperations.setFestivalMarker(ready, site)
    case FlipBanner(banner) =>
      Right(BoardControlOperations.flipBanner(ready, banner))
    case SetEndDieModifier(amount) =>
      Right(TurnStateOperations.setEndDieModifier(ready, amount))
    case OpenPick(decisionId, option, label) =>
      Right(TurnStateOperations.openPick(ready, decisionId, option, label))
    case ClosePick() => Right(TurnStateOperations.closePick(ready))
    case EnterPhase(phase) => TurnStateOperations.enterPhase(ready, phase)
    case SetOathkeeper(holder) => TurnStateOperations.setOathkeeper(ready, holder)
    case SetOathkeeperGoal(goal) =>
      TurnStateOperations.setOathkeeperGoal(ready, goal)
    case RecordCampaignResult(fact) =>
      Right(TurnStateOperations.recordCampaignResult(ready, fact))
    case QueueProcedure(entry, atFront) =>
      Right(TurnStateOperations.queueProcedure(ready, entry, atFront))
    case BeginTurn(player, phase) =>
      TurnStateOperations.beginTurn(ready, player, phase)

  private def chronicle(ready: ReadyGame,
      leaf: ChronicleOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case ChronicleUpdated(evolved) =>
      Right(TurnStateOperations.chronicleUpdated(ready, evolved))
    case RoleChanged(lineage, role) =>
      ChronicleStateOperations.roleChanged(ready, lineage, role)
    case ChronicleTaskAssigned(task, player) =>
      ChronicleStateOperations.taskAssigned(ready, task, player)
    case GrandScepterFromBox(player) =>
      ChronicleStateOperations.scepterFromBox(ready, player)
    case SuitedReserveFromBox(suit, cards) =>
      ChronicleStateOperations.suitedReserveFromBox(ready, suit, cards)
    case ImperialWarbandsTaken(count) =>
      Right(ChronicleStateOperations.imperialWarbandsTaken(ready, count))
    case WarbandsAdded(kind, count) =>
      ChronicleStateOperations.warbandsAdded(ready, kind, count)
    case LineagePointsScored(lineage, points) =>
      ChronicleStateOperations.lineagePointsScored(ready, lineage, points)
    case EdificeBuilt(edifice, site) =>
      ChronicleStateOperations.edificeBuilt(ready, edifice, site)
    case FoundationRestored(number) =>
      ChronicleStateOperations.foundationRestored(ready, number)
    case FoundationAltered(number, source) =>
      ChronicleStateOperations.foundationAltered(ready, number, source)
    case WinnerNamed(winner, kind) =>
      ChronicleStateOperations.winnerNamed(ready, winner, kind)
    case StartingRelicKept(player, relic) =>
      ChronicleStateOperations.startingRelicKept(ready, player, relic)
    case RelicsHidden(relics) =>
      ChronicleStateOperations.relicsHidden(ready, relics)
    case LegaciesScored(legacies) =>
      ChronicleStateOperations.legaciesScored(ready, legacies)
    case placement: PlaceAtlasSite => AtlasOperations.placeSite(ready, placement)
