package oathdigital.gameplay.powerresolver

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model.{CoreOperation, Decide, DecisionAnswer, LogNote,
  PlayerId, ReadyGame}

/** What a power reads when it looks at a walker batch before the batch
  * runs (catalog batch 11, slice 4): the walk's catalog, the walk's actor,
  * and the state the batch was built in. */
final case class BatchContext(catalog: ExecutableCatalog, actor: PlayerId,
    ready: ReadyGame)

/** What an answer puts in an operation's place, and the line that says
  * so. */
final case class Replacement(operations: Vector[CoreOperation],
    line: LogNote)

/** One question about one operation of a batch: `decide`, and the
  * replacement an answer takes. An answer `instead` maps to nothing leaves
  * the operation as built. */
final case class BatchAsk(decide: Decide,
    instead: DecisionAnswer => Option[Replacement])

/** A power that may ask about an operation of a walker batch before the
  * batch runs (catalog batch 11, slice 4), beside `TakesBurns` and
  * `ExecutedTrigger`. The walker asks each such power, in the order
  * contributions chain, about each operation of a `BuildOps` batch, a
  * `Take`'s leaving included; the first to ask about an operation asks the
  * only question about it. A question's decision id must name what it asks
  * about, so two questions on one batch differ and a rebuilt batch meets
  * the same answers. `watching` is a cheap test of whether the power can
  * ask at all: while no power watches, the walker builds nothing extra.
  */
trait AsksBeforeBatch extends ContributingPower:
  def watching(ready: ReadyGame): Boolean
  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk]
