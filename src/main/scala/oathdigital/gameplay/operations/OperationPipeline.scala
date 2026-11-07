package oathdigital.gameplay.operations

import oathdigital.model._

final case class SkippedOperation(requested: CoreOperation,
    reasons: Vector[OperationReason])

final case class OperationRun(state: ReadyGame,
    executed: Vector[CoreOperation], skipped: Vector[SkippedOperation]):
  /** A replayed event must not accept a different effect than it recorded. */
  def expectEffects(requested: Vector[CoreOperation],
      detail: String): Either[OathViolation, ReadyGame] =
    if executed == requested.map(OperationRun.canonical) then Right(state)
    else Left(OathViolation.InvalidEventOrder(detail))

object OperationRun:
  def canonical(operation: CoreOperation): CoreOperation = operation match
    case value: SpendSupply => value.copy(required = true)
    case value: Discard.Denizen => value.copy(required = false)
    case value: Discard.Buried => value.copy(required = false)
    case value: Play => value.copy(required = false)
    case value: Replace => value.copy(required = false)
    case value: Give => value.copy(required = false)
    case value: Take => value.copy(required = false)
    case value: PayCost => value.copy(offTurn = false)
    case value: Defer => value.copy(ops = value.ops.map(canonical))
    case other => other

  /** The cards this run took out of the game: every card whose executed move
    * ends at the shared bank. The pipeline's card-inventory check allows a
    * card to be missing afterwards only if a declared operation removed it.
    */
  def boxed(executed: Vector[CoreOperation]): Set[CardId] = executed.collect {
    case Move(Piece.Card(id), _, to, _, _)
        if to.location == Location.SharedBank =>
      id
  }.toSet

  /** The cards this run brought into the game from the box: the Grand
    * Scepter, taken by a winner when no one holds it (Chronicle ruling 10),
    * and the boxed denizens a suited reserve takes before the Beacon draws.
    * The inventory check expects them afterwards. */
  def unboxed(executed: Vector[CoreOperation]): Set[CardId] = executed.flatMap {
    case GrandScepterFromBox(_) => Vector(TheGrandScepterCard.id: CardId)
    case SuitedReserveFromBox(_, cards) => cards
    case _ => Vector.empty
  }.toSet

  /** The cards expected after the run, in operation order: a card boxed
    * and later unboxed is expected, and one unboxed and later boxed is not.
    */
  def expectedAfter(expected: Set[CardId], executed: Vector[CoreOperation])
      : Set[CardId] = executed.foldLeft(expected)((cards, operation) =>
    cards -- boxed(Vector(operation)) ++ unboxed(Vector(operation)))

/** Sole public orchestrator of an operation batch. It folds each operation
  * through settlement, staged resolution ([[OperationResolution]], which
  * concatenates the caller's `allowlist` policy, the shape guard and the
  * restrictions vector) and the [[OperationExecutor]], applies the owning
  * procedure's direct `update`, and runs the post-state invariant.
  *
  * Whole-batch rejection must stay staged: an operation later in a batch can
  * be satisfiable only after earlier operations ran (for example a Campaign
  * losing-force return that moves warbands out of a bank which
  * in-batch `Kill`s replenish). Validating every operation against the initial
  * state would reject those trajectory batches, so the authoritative
  * rejection is the staged per-operation resolution: first fail, and the
  * whole batch is rejected.
  *
  * A card may leave the game only through a `Move` to `Location.SharedBank`
  * that the batch declares; its id is then removed from the expected card
  * set. Any other change to the card inventory still fails the batch.
  *
  * `requireAll` treats every operation as `required` (a reduced or skipped
  * effect rejects the batch). The walker sets it for the `Move` children of a
  * required composite, which carry no flag of their own.
  *
  * `restrictions` has no default, so a new caller cannot run operations outside
  * the global restrictions (global operation restrictions design).
  */
object OperationPipeline:
  def run(
      ready: ReadyGame,
      operations: Vector[CoreOperation],
      allowlist: OperationPolicy,
      restrictions: Vector[OperationRestriction],
      requireAll: Boolean = false
  )(
      update: ReadyGame => Either[OathViolation, ReadyGame]
  ): Either[OathViolation, OperationRun] =
    if operations.isEmpty then Left(OperationError.EmptyOperationBatch.toViolation)
    else
      // The batch's operations happen at the same time, so a grant for one
      // lifts its power for all of them (consent design, slice 3).
      val batch = OperationConsent.together(ready, operations, restrictions)
      for
        expected <- OperationStateInvariant.cardIds(ready)
          .left.map(_.toViolation)
        staged <- operations.foldLeft[Either[OathViolation, OperationRun]](
          Right(OperationRun(ready, Vector.empty, Vector.empty))):
          (result, operation) => result.flatMap { current =>
            PayCostSettlement.prepare(current.state, operation).flatMap { prepared =>
              OperationResolution.resolve(current.state, prepared, allowlist,
                batch, requireAll)
                .flatMap:
                  case OperationResolution.Skip(reasons) =>
                    Right(current.copy(skipped = current.skipped :+
                      SkippedOperation(operation, reasons)))
                  case OperationResolution.Execute(actual) =>
                    new OperationExecutor().execute(current.state, actual)
                      .left.map(_.toViolation).map(state => current.copy(
                        state = state,
                        executed = current.executed :+
                          OperationRun.canonical(actual)))
            }
          }
        updated <- OperationError.describe(update(staged.state))
          .left.map(_.toViolation).flatMap(identity)
        // Decision 5: the invariant runs once after the module update, not
        // between operations — intermediate states within a legal batch are
        // not invariant-checked.
        _ <- OperationStateInvariant.validate(updated,
          OperationRun.expectedAfter(expected, staged.executed))
          .left.map(_.toViolation)
      yield staged.copy(state = updated)
