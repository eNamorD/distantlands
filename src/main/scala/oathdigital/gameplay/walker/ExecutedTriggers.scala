package oathdigital.gameplay.walker

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.operations.{OperationPipeline, OperationPolicy,
  OperationRun}
import oathdigital.gameplay.powerresolver.{ContributingPower, Deferred,
  ExecutedBatch, ExecutedTrigger}
import oathdigital.model.{Defer, DeferralKey, OathViolation, PlayerId,
  ReadyGame}

/** The walker's side of triggers on executed operations (catalog batch 8,
  * slice 6). A batch that starts a follow-up holds its executed operations
  * under its level's key ([[keyAt]]), in its own journaled step, so a command that resumes
  * inside a follow-up asks the triggers again with the same operations.
  * `ProcedureWalker` walks the follow-ups and then takes what is held. It
  * names no card.
  */
private[walker] object ExecutedTriggers:
  /** The key a batch at follow-up depth `depth` holds its operations under.
    * Depth 0, the action's own batches, keeps the key saved games hold.
    * Each level down has its own, so a nested batch's operations never mix
    * with those of the batch whose follow-up it runs in (catalog batch 11,
    * slice 8). */
  def keyAt(depth: Int): DeferralKey =
    if depth == 0 then DeferralKey("walker.executed-triggers")
    else DeferralKey(s"walker.executed-triggers.$depth")

  /** How deep follow-ups may nest. A deeper chain is a trigger that
    * starts itself again. */
  val MaxDepth: Int = 8

  /** The triggers among `powers` that a batch at `depth` asks, in the order
    * contributions chain: every trigger for the action's own batches, and
    * only those that answer follow-ups below them. */
  def of(powers: WalkerPowers, depth: Int): Vector[ExecutedTrigger] =
    powers.powers.collect {
      case trigger: ExecutedTrigger
          if depth == 0 || trigger.answersFollowUps => trigger
    }.sortBy(ContributingPower.sortKey)

  /** What a trigger reads now: the operations held at the walk's level,
    * and the walk's state. */
  def batch(ctx: WalkCtx): ExecutedBatch =
    batch(ctx.powers.catalog, ctx.actor, ctx.state, ctx.depth)

  /** The same, for a lookup that has no walk, only the parked state. */
  def batch(catalog: ExecutableCatalog, actor: PlayerId, ready: ReadyGame,
      depth: Int): ExecutedBatch =
    ExecutedBatch(catalog, actor, Deferred.held(ready, keyAt(depth)), ready)

  /** Whether a batch at `depth` holds its operations for its follow-ups. */
  def holds(ready: ReadyGame, depth: Int): Boolean =
    Deferred.held(ready, keyAt(depth)).nonEmpty

  /** `run`, with its executed operations held at the walk's level when some
    * trigger starts a follow-up on them. Nothing is held for an empty
    * batch, or while triggers are off (a claimed burn or cost run whole,
    * the take of a held batch). A follow-up past `MaxDepth` is refused. */
  def held(ctx: WalkCtx, run: OperationRun)
      : Either[OathViolation, OperationRun] =
    val batch = ExecutedBatch(ctx.powers.catalog, ctx.actor, run.executed,
      run.state)
    if ctx.triggersOff || run.executed.isEmpty ||
        !of(ctx.powers, ctx.depth).exists(_.onExecuted(batch).nonEmpty) then
      Right(run)
    else if ctx.depth >= MaxDepth then Left(OathViolation.InvalidEventOrder(
      s"walker contract violation: follow-ups nested deeper than $MaxDepth"))
    else OperationPipeline.run(run.state,
        Vector(Defer(keyAt(ctx.depth), run.executed)),
        OperationPolicy.Permissive, Vector.empty, requireAll = true)(
        Right(_)).map(deferred => run.copy(state = deferred.state,
        executed = run.executed ++ deferred.executed))
