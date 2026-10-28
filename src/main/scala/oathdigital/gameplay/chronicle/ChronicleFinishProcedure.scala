package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The last Chronicle procedure: journals the evolved Chronicle (Chronicle
  * steps design, "Exit and extraction"), as `ChronicleRecord` reads it from
  * the live campaign.
  */
object ChronicleFinishProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty,
      // Rooted in a Sequence: the walker names nodes by path from a
      // container, so a bare leaf at the root has no node id.
      Sequence(Vector(BuildOps((state, _) =>
        Right(Vector(ChronicleUpdated(
          ChronicleRecord.campaignFields(state)))), required = true))),
      OathViolation.InvalidEventOrder(
        "the Chronicle's end takes no start selection"))
