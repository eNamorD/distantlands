package oathdigital.gameplay.walker

import oathdigital.model.{Answered, OathEvent, Operation,
  OperationRestriction, PlayerId, ReadyGame, Sequence}

/** Command-local walk state: the state threaded through executed deltas,
  * the events recorded so far, the walk's actor
  * (`CurrentGameState.walkActor`), and every decision already answered
  * during this action.
  */
private[walker] final case class WalkCtx(
    state: ReadyGame,
    events: Vector[OathEvent],
    actor: PlayerId,
    answered: Vector[Answered],
    powers: WalkerPowers,
    dice: WalkerDice,
    /** The procedure of the parked position being resumed. */
    procedure: Option[oathdigital.model.ProcedureRef],
    /** The whole action tree, which the search reads. */
    root: Operation,
    /** The states before and after the leaf this command ran last, which
      * a note reads. A leaf that changed nothing gives the same state
      * twice. */
    previous: Option[(ReadyGame, ReadyGame)] = None,
    /** Set on a search's walk (global operation restrictions design, "Lazy
      * pruning"): it stops at hidden information and at a decision some
      * answer leads on from, and its events are thrown away. */
    searching: Boolean = false,
    /** The decisions this search stands at, with the state at each, as
      * `WalkCtx.searchKey` reads it. */
    visited: Set[(Vector[String], ReadyGame)] = Set.empty,
    /** Set while a claimed burn or cost runs whole, and while a level's
      * held batch is taken: those batches start no follow-up, since a
      * resume could not reach one there (`ExecutedTriggers`). */
    triggersOff: Boolean = false,
    /** The follow-up trees this walk is inside, outermost first. Their
      * count is the walk's follow-up depth, whose key a batch holds its
      * operations under (catalog batch 11, slice 8). None is part of
      * `root`, so the restriction look-ahead reads them beside it: a
      * follow-up may nest a card play whose hook a restriction judges
      * (False Prophet's claim, slice 7). */
    followUps: Vector[Operation] = Vector.empty
):
  /** How many follow-ups this walk is inside. */
  def depth: Int = followUps.size

  /** The tree the restriction look-ahead folds: `root`, and every
    * follow-up this walk is inside. */
  def restrictionRoot: Operation =
    if followUps.isEmpty then root else Sequence(root +: followUps)

  /** The operation restrictions this walk's batches run with: its powers'
    * restrictions for the procedure it walks, which lift a power's own lock
    * during that power's use (catalog batch 10, slice 4). */
  def operationRestrictions: Vector[OperationRestriction] =
    powers.restrictionsDuring(procedure)

private[walker] object WalkCtx:
  /** The state a search compares to tell whether it has stood at a decision
    * before: the state less its pick records (`walkerPicks`). The records
    * tally what picks paid, for display, and never change what can happen,
    * so a free pick leaves the search where it was, as it did before picks
    * were recorded. */
  def searchKey(state: ReadyGame): ReadyGame =
    if state.game.current.walkerPicks.isEmpty then state
    else state.updateCurrent(_.copy(walkerPicks = Vector.empty))

private[walker] sealed trait Step extends Product with Serializable
/** The walked region completed; `ctx` carries the result state/events. */
private[walker] final case class Done(ctx: WalkCtx) extends Step
/** A park bubbled up from a Decide/Roll leaf at `position`. */
private[walker] final case class Park(position: Vector[String], ctx: WalkCtx)
    extends Step
/** A search stopped here and counts the path as legal: at hidden
  * information, at a decision some answer leads on from, or back at a
  * decision it already stands at in the same state. Only a search's walk
  * returns it. */
private[walker] final case class Stopped(ctx: WalkCtx) extends Step
/** The walk reached `EndWalk`: the procedure finishes here, and every
  * enclosing node stops walking its later children. */
private[walker] final case class Ended(ctx: WalkCtx) extends Step
