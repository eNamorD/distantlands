package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.walker.ProcedureStart
import oathdigital.model._

/** The World task (CR p. 38, `docs/rules/chronicle.md` task 3), resolved by
  * the player holding its card. Each step is its own power window. */
object ChronicleWorldProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the World", args)
      .map(_ => Sequence(steps(catalog)))

  /** The notes the World's steps write. */
  def noteKeys: Vector[NoteKey] = ProtectAndDispossess.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleSeparateVisions,
      SeparateVisions.steps),
    ChronicleWindows.step(PowerWindow.ChronicleGatherDenizens,
      GatherDenizens.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleProtectAndDispossess,
      ProtectAndDispossess.steps))
