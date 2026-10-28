package oathdigital.gameplay.chronicle

import oathdigital.model._

/** The fixed order of the Chronicle Phase (Chronicle steps design,
  * "Chaining"). The queue only holds startable procedures, so the driver
  * chains these triggered ones: it starts the successor of whichever just
  * completed. World, Beacon and Stars are later inserted between Throne and
  * Finish.
  */
object ChronicleSequence:
  private val order: Vector[TriggeredProcedureRef] =
    Vector(TriggeredProcedureRef.ChronicleBegin,
      TriggeredProcedureRef.ChronicleSun, TriggeredProcedureRef.ChronicleThrone,
      TriggeredProcedureRef.ChronicleFinish)

  /** The procedure that follows `completed`, when it is a Chronicle
    * procedure with a successor. */
  def after(completed: ProcedureRef): Option[TriggeredProcedureRef] =
    order.indexOf(completed) match
      case -1 => None
      case at => order.lift(at + 1)

  /** The Chronicle opens once: the game has a result, the phase is not yet
    * Chronicle, and no walk is pending. */
  def opening(current: CurrentGameState): Option[TriggeredProcedureRef] =
    Option.when(current.result.nonEmpty &&
      current.turn.phase != Phase.Chronicle &&
      current.walkerPending.isEmpty && current.walkerProcedure.isEmpty)(
      TriggeredProcedureRef.ChronicleBegin)
