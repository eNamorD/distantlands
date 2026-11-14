package distantlands.model

/** Operations on the turn and its tracks, and the walk's scratch:
  * phases and turns, the round and Visions Drawn tracks, the
  * Oathkeeper title and goal, power uses, campaign results, queued
  * procedures, deferrals, as-if records, the Tracker record, the
  * festival marker, and pick records.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait TurnOperation extends OperationBase

/** Records one use-limited power instance as used for the current turn, by
  * adding `power` to `TurnState.usedPowers` (batch-1 Task 7).
  *
  * The write side of a use limit has to be an operation rather than a state
  * callback beside one, because replay applies recorded operations and
  * nothing else: a limit written any other way would be absent from a
  * reloaded game and the same site could be used again. The read side is a
  * power's own restriction, so nothing here knows which power this is.
  *
  * Adding a `PowerUseRef` the turn already holds is a no-op rather than a
  * rejection, matching the set semantics of the field it writes. Whether a
  * second use is legal at all is a question for whoever declared the limit,
  * asked before the walk; this operation only records the answer.
  */
final case class RecordPowerUse(power: PowerUseRef) extends TurnOperation, LeafOperation

/** Moves the turn into `phase` (batch-1 Task 7).
  *
  * A phase change is a state write like any other, so the procedure that
  * performs one declares it as an operation and replay restores it from the
  * journal -- the same argument [[RecordPowerUse]] makes. Ending Wake is the
  * only procedure that declares it today.
  *
  * Which phase may follow which is NOT stated here. The phase order is a rule
  * about the turn, and the turn's procedures are where it is written; an
  * operation that encoded the order would state the same rule a second time,
  * in the vocabulary every future power can reach for. The one thing applying
  * this does reject is entering the phase the turn is already in
  * (`OperationError.PhaseAlreadyEntered`), which is a doubled or reordered
  * journal rather than a rule about order.
  */
final case class EnterPhase(phase: Phase) extends TurnOperation, LeafOperation

/** Sets who holds the Oathkeeper title: a player, or the shared bank (`None`).
  *
  * The holder and the side change together: any change of holder puts the
  * title back on its Oathkeeper side, as every title change in the rules does.
  * Which holder the title should go to is not decided here; that is
  * `OathkeeperRules.outcome`. Applying this rejects leaving the holder as
  * it is (`OperationError.OathkeeperUnchanged`), a doubled or reordered
  * journal rather than a rule about the title, unless the title is on its
  * Usurper side: becoming a Citizen turns it back (CR p. 34).
  */
final case class SetOathkeeper(holder: Option[PlayerId]) extends TurnOperation, LeafOperation

/** Sets the campaign's Oathkeeper goal (catalog batch 8, slice 2).
  *
  * Which goal a power may choose is the power's rule. The title follows at
  * the next action boundary, where `OathkeeperRules.outcome` reads the goal.
  * The one thing applying this rejects is leaving the goal as it is
  * (`OperationError.OathkeeperGoalUnchanged`), which is a doubled or
  * reordered journal, as for [[SetOathkeeper]].
  */
final case class SetOathkeeperGoal(goal: OathkeeperGoal)
    extends TurnOperation, LeafOperation

/** Records the result of the Campaign just fought as the public
  * `lastCampaignResult`, replacing the previous one.
  */
final case class RecordCampaignResult(result: CampaignResult)
    extends TurnOperation, LeafOperation

/** Puts `entry` on the queue (walker-driven game loop design, "The
  * queue"): at the back, or in front when `atFront`. A source power emits
  * it inside the origin action's tree. */
final case class QueueProcedure(entry: QueuedProcedure,
    atFront: Boolean = false) extends TurnOperation, LeafOperation

/** Hands the turn to `player` in `phase`, clearing every power use recorded
  * against the turn that ended.
  *
  * Two procedures declare it: Finish Rest hands the turn to the next player,
  * and Setup's last step hands the first turn to the first player. As with
  * [[EnterPhase]], which player follows whom is not stated here: that is the
  * turn order Finish Rest reads. What applying this does reject is a player
  * who is not seated, and a phase no turn begins in. A turn begins in Wake,
  * or in RoundEnd when the last player has rested.
  */
final case class BeginTurn(player: PlayerId, phase: Phase)
    extends TurnOperation, LeafOperation

/** Takes the Usurper Limiter off the round track when an Empire starts in
  * play (ruling R5). The end of round 3 removes it without an operation,
  * as `RoundEnded` is applied. */
case object UsurperLimiterRemoved extends TurnOperation, LeafOperation

/** Advances Visions Drawn by one after a world-deck Vision is drawn. */
case object AdvanceVisionsDrawn extends TurnOperation, LeafOperation:
  override val required: Boolean = true

/** Moves the round marker one space forward, as Great Clock and Broken
  * Clock do. Passing round 3 takes the Usurper Limiter off, as the end of
  * round 3 does (catalog batch 10, ruling 1). It rolls no end die; applying
  * it on round 8 is an error. */
case object AdvanceRound extends TurnOperation, LeafOperation:
  override val required: Boolean = true

/** Holds `ops` under `key` in `CurrentGameState.walkerDeferred`, after any
  * held there already, without running them. A later step of the same
  * action runs them, replaces them or drops them, and takes them with
  * [[TakeDeferred]]; the walker drops whatever is left when the action
  * ends. They are held as the journal records them, so a game reloaded from
  * its events holds the same ops. It writes no Game Log line.
  *
  * It is a leaf, so [[Operation.flatten]] stops here and never descends
  * into `ops`. That is what holding them means: nothing under a `Defer`
  * runs, is screened, or counts as a hidden outcome until a later step
  * returns those ops as operations of its own (hidden outcome depth
  * decision).
  */
final case class Defer(key: DeferralKey, ops: Vector[CoreOperation])
    extends TurnOperation, LeafOperation

/** Drops what [[Defer]] holds under `key`. Taking a key that holds nothing
  * changes nothing. It writes no Game Log line. */
final case class TakeDeferred(key: DeferralKey) extends TurnOperation, LeafOperation

/** Records that the walk's actor acts as if their pawn were at `site` for
  * the rest of the walk, in `CurrentGameState.walkerAsIfSite` (catalog
  * batch 8, item 13c). The walker clears it when the walk ends. The journal
  * records it, so a game reloaded from its events holds the same site. It
  * moves no pawn and writes no Game Log line.
  */
final case class ActAsIf(site: SiteId) extends TurnOperation, LeafOperation

/** Records that the walk's actor acts as if they held `player`'s advisers
  * instead of their own for the rest of the walk, in
  * `CurrentGameState.walkerAsIfAdvisers` (Master of Disguise, catalog
  * batch 11, slice 3). The walker clears it when the walk ends. The
  * journal records it, so a game reloaded from its events holds the same
  * player. It moves no card and writes no Game Log line.
  */
final case class ActAsIfAdvisers(player: PlayerId) extends TurnOperation, LeafOperation

/** Records the pawn `holder` chose with Tracker's ACTION in
  * `CurrentGameState.trackedPawns` (catalog batch 10, slice 14). */
final case class TrackPawn(holder: PlayerId, target: PlayerId)
    extends TurnOperation, LeafOperation

/** Removes `holder`'s Tracker record as their Act phase begins. */
final case class ClearTrackedPawn(holder: PlayerId) extends TurnOperation, LeafOperation

/** Puts the festival marker over the cards at `site`, or takes it off the
  * map (`None`), in `DarkestSecretState.festival` (Foundations design,
  * slice 6). The Festival's holder places it at their Rest; a ribbon that
  * prints the remove-festival icon takes it off. Applying it rejects a
  * site that is not on the map, a placement while the Festival face is
  * down, and a removal of a marker that is not on the map: each is a
  * malformed journal, not a rule.
  */
final case class SetFestivalMarker(site: Option[SiteId])
    extends TurnOperation, LeafOperation

/** Turns `banner` to its other face (Legacy in-play powers ruling 20):
  * the People's Favor between the Mob and the Grand Council, the Darkest
  * Secret between the Wandering Flame and the Festival. Its holder and
  * tokens stay. Flipping to the Festival places no marker; flipping away
  * from it takes the marker off. */
final case class FlipBanner(banner: Banner) extends TurnOperation,
    LeafOperation

/** Sets `CurrentGameState.endDieModifier`, Arbiter's change to the coming
  * end die, from -1 to +3 (Legacy in-play powers ruling 21; R11). The end
  * die reads and clears it. The journal records it, so a reloaded game
  * rolls with the same modifier. */
final case class SetEndDieModifier(amount: Int) extends TurnOperation,
    LeafOperation:
  require(amount >= -1 && amount <= 3,
    s"an end die modifier runs from -1 to +3, not $amount")

/** Opens the record of a pick a pick-and-pay menu took, in
  * `CurrentGameState.walkerPicks`: every `PayCost` and `Sacrifice` applied
  * until [[ClosePick]] adds to it, so the record holds what the pick really
  * paid, after every power's change to its price. `label` names a button
  * pick.
  * The journal records it, so a reloaded game holds the same record. It
  * writes no Game Log line.
  */
final case class OpenPick(decisionId: String, option: DecisionOptionRef,
    label: Option[String] = None) extends TurnOperation, LeafOperation

/** Closes the open [[OpenPick]] record, so later payments are not added to
  * it. Closing with none open changes nothing. It writes no Game Log line.
  */
final case class ClosePick() extends TurnOperation, LeafOperation
