package oathdigital.testkit

import oathdigital.gameplay.operations.OperationExecutor
import oathdigital.model.{CoreOperation, OperationError, ReadyGame}

/** Folds a batch of operations through the raw [[OperationExecutor]], one
  * after another, stopping at the first rejection.
  *
  * This bypasses resolution, consent and the post-state invariant on
  * purpose: a mutation suite asserts what one family mutation does to the
  * state, not what [[oathdigital.gameplay.operations.OperationPipeline]]
  * would make of the batch. Production code never folds this way; the
  * pipeline is the sole orchestrator.
  */
object RawOperationBatch:
  def executeAll(ready: ReadyGame,
      operations: Vector[CoreOperation]): Either[OperationError, ReadyGame] =
    if operations.isEmpty then Left(OperationError.EmptyOperationBatch)
    else
      val executor = new OperationExecutor
      operations.foldLeft[Either[OperationError, ReadyGame]](Right(ready)):
        (result, operation) => result.flatMap(executor.execute(_, operation))
