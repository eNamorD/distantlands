package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The World task (CR p. 38, `docs/rules/chronicle.md` task 3), resolved by
  * the player holding its card. Each step is its own power window. */
object ChronicleWorldProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps),
      OathViolation.InvalidEventOrder("the World takes no start selection"))

  /** The notes the World's steps write. */
  def noteKeys: Vector[NoteKey] = Vector.empty

  private def steps: Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleSeparateVisions, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleGatherDenizens, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleProtectAndDispossess,
      Vector.empty))
