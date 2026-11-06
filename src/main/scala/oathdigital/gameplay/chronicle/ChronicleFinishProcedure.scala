package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.walker.ProcedureStart
import oathdigital.model._

/** The last Chronicle procedure: journals the evolved Chronicle (Chronicle
  * steps design, "Exit and extraction"), and refuses one that fails the
  * card accounting. */
object ChronicleFinishProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the Chronicle's end", args).map(_ =>
      // Rooted in a Sequence: the walker names nodes by path from a
      // container, so a bare leaf at the root has no node id.
      Sequence(Vector(BuildOps((state, _) =>
        ChronicleRecord.evolved(catalog, state)
          .map(chronicle => Vector(ChronicleUpdated(chronicle))),
        required = true))))
