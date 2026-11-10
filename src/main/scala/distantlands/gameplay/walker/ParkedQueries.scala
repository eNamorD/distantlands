package distantlands.gameplay.walker

import distantlands.model.{Decide, Operation, PendingTree, PlayerId, PoolKey, ReadyGame, Roll, RollMode}

/** What a parked walk waits on, read off the rebuilt tree: the open
  * decisions, a parked roll, and who may answer. Split out of
  * [[ProcedureWalker]] to keep that file under the project's line bound;
  * `ProcedureWalker` forwards to it. */
private[walker] object ParkedQueries:
  /** When `pending` parks on a `Roll` node of `action`, reports the node's
    * `pool` and the face count that node requires (read from
    * `state.rollPools`), so the app layer can pre-roll exactly that many
    * faces for the next `roll` command (Task 6 consumes it). `None` when the
    * park is a Decide or the position does not resolve to a Roll. `powers`
    * (fix-round ruling J) must be the same vector the live walk that parked
    * here used -- `leafAt` folds every window on the path exactly like the
    * walk does, so a transform that inserts operations around a windowed
    * node does not make this address the wrong leaf.
    *
    * Always resolves with the restriction look-ahead off: the look-ahead
    * only narrows a `Decide`'s options, never a `Roll`'s, so probing here
    * could only be wasted work on a position that turns out to be a
    * `Decide` (discarded below either way).
    */
  def parkedRoll(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Option[(PoolKey, Int)] =
    WalkerPowerGather.leafAt(state, action, pending, powers.quiet)
      .collect:
        case roll: Roll if roll.mode == RollMode.Parked =>
          (roll.pool, WalkerRolls.poolCount(state, roll.pool))

  /** Every decision open at the park: one for a plain or co-owned `Decide`,
    * none for a `Roll` or a position that does not resolve. Reports the nodes
    * themselves so the caller can dispatch on their stable `decisionId`
    * rather than on the park's structural path, which shifts if the tree is
    * edited. A `Simultaneous` park returns one per open child.
    *
    * `probing` (default on) runs the restriction look-ahead, narrowing the
    * options a caller offers or validates against. A caller that only reads
    * the decision's identity, owner or labels -- never which options are
    * still legal -- passes `probing = false` to skip that work.
    */
  def openDecisions(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Vector[Decide] =
    val looked = if probing then powers else powers.quiet
    WalkerPowerGather.leavesAt(state, action, pending, looked).collect {
      case decide: Decide => decide
    }

  /** The one open decision, or `None` at a Roll park, at a position that
    * does not resolve to a Decide, or when several decisions are open (a
    * `Simultaneous` park): a caller that needs one of several looks it up by
    * decision id or owner in [[openDecisions]]. Symmetric to [[parkedRoll]].
    * See [[openDecisions]] for `probing`.
    */
  def parkedDecide(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Option[Decide] =
    openDecisions(state, action, pending, powers, probing) match
      case Vector(one) => Some(one)
      case _ => None

  /** The open decision `decisionId` names, at any park. */
  def openDecision(state: ReadyGame, action: Operation, pending: PendingTree,
      powers: WalkerPowers, decisionId: String,
      probing: Boolean = true): Option[Decide] =
    openDecisions(state, action, pending, powers, probing)
      .find(_.decisionId == decisionId)

  /** Everyone who may answer the parked position: the owners and co-owners of
    * its open decisions, or the walk's actor for a parked `Roll`. Skips the
    * restriction look-ahead: an owner set never depends on which options are
    * hidden.
    */
  def awaitedPlayers(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Set[PlayerId] =
    val open = openDecisions(state, action, pending, powers, probing = false)
    if open.nonEmpty then open.flatMap(_.owners).toSet
    else parkedRoll(state, action, pending, powers)
      .map(_ => Set(state.game.current.walkActor)).getOrElse(Set.empty)

  /** [[awaitedPlayers]] in seat order, for a projection that names them. */
  def awaitedInSeatOrder(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Vector[PlayerId] =
    val awaited = awaitedPlayers(state, action, pending, powers)
    state.game.current.players.map(_.player).filter(awaited)
