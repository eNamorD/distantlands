package distantlands.gameplay.powerresolver

import distantlands.catalog.ExecutableCatalog
import distantlands.model.{CoreOperation, Operation, PlayerId, ReadyGame}

/** What a trigger on executed operations reads (catalog batch 8, slice 6):
  * the walk's card catalog, the active player, the operations one walker
  * batch executed, and the state the trigger is asked in. */
final case class ExecutedBatch(catalog: ExecutableCatalog, actor: PlayerId,
    executed: Vector[CoreOperation], now: ReadyGame)

/** A power that starts a follow-up after a walker batch executes
  * operations it watches (catalog batch 8, slice 6), beside the arrival
  * rule `FollowsArrivals`.
  *
  * The walker walks the follow-up right after the batch, before the walk
  * goes on, and asks each trigger when it reaches it: after the follow-ups
  * of the triggers before it ran, in the order contributions chain. A
  * follow-up's own batches ask only the triggers that answer follow-ups
  * ([[answersFollowUps]]); their follow-ups walk one level down (catalog
  * batch 11, slice 8).
  *
  * A command that resumes inside the follow-up asks again, with the same
  * executed operations and the state it parked on, and must get the same
  * tree up to the park. So a follow-up runs nothing before its last
  * question.
  */
trait ExecutedTrigger extends ContributingPower:
  def onExecuted(batch: ExecutedBatch): Option[Operation]

  /** Whether this trigger answers a follow-up's batches, its own or
    * another trigger's, with a follow-up one level down. Off by default: a
    * trigger that turns it on must not start itself again from what its
    * own follow-up runs, and the walker refuses a chain deeper than
    * `ExecutedTriggers.MaxDepth`. */
  def answersFollowUps: Boolean = false
