package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
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
    IntroduceNewcomers.noteKeys ++ WelcomeStranger.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleIntroduceNewcomers,
      IntroduceNewcomers.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleWelcomeStranger,
      WelcomeStranger.steps),
    // Shape Foundation (CR p. 39) waits for Foundations.
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))
