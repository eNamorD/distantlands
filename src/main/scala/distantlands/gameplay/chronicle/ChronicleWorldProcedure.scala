package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The World task (CR p. 38, `docs/rules/chronicle.md` task 3), resolved by
  * the player holding its card. Each step is its own power window. */
object ChronicleWorldProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the World", args)
      .map(_ => Sequence(steps(catalog)))

  /** The notes the World's steps write. */
  def noteKeys: Vector[NoteKey] = ProtectAndDispossess.noteKeys ++
    ChronicleSteps.noteKeys(TriggeredProcedureRef.ChronicleWorld) ++
    OathsOfLoyalty.noteKeys(TriggeredProcedureRef.ChronicleWorld)

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleSeparateVisions,
      SeparateVisions.steps),
    ChronicleWindows.step(ChronicleSteps.gatherDenizens,
      PowerWindow.ChronicleGatherDenizens,
      GatherDenizens.steps(catalog)),
    ChronicleWindows.step(ChronicleSteps.protectAndDispossess,
      PowerWindow.ChronicleProtectAndDispossess,
      ProtectAndDispossess.steps))
