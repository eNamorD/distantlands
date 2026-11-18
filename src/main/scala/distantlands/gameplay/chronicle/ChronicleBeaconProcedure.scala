package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.{LegacyChroniclePowers,
  OathsOfLoyalty}
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The Beacon task (CR p. 39, `docs/rules/chronicle.md` task 4), resolved
  * by the player holding its card. Each step is its own power window. */
object ChronicleBeaconProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the Beacon", args)
      .map(_ => Sequence(steps(catalog)))

  /** The notes the Beacon's steps write. */
  def noteKeys: Vector[NoteKey] =
    ChronicleSteps.noteKeys(TriggeredProcedureRef.ChronicleBeacon) ++
    IntroduceNewcomers.noteKeys ++ WelcomeStranger.noteKeys ++
      ShapeFoundation.noteKeys ++
      OathsOfLoyalty.noteKeys(TriggeredProcedureRef.ChronicleBeacon) ++
      LegacyChroniclePowers.beaconNoteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(ChronicleSteps.introduceNewcomers,
      PowerWindow.ChronicleIntroduceNewcomers,
      IntroduceNewcomers.steps(catalog)),
    ChronicleWindows.step(ChronicleSteps.welcomeStranger,
      PowerWindow.ChronicleWelcomeStranger,
      WelcomeStranger.steps),
    ChronicleWindows.step(ChronicleSteps.shapeFoundation,
      PowerWindow.ChronicleShapeFoundation,
      ShapeFoundation.steps(catalog)))
