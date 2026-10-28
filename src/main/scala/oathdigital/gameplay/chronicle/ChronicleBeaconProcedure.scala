package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The Beacon task (CR p. 39, `docs/rules/chronicle.md` task 4), resolved
  * by the player holding its card. Each step is its own power window. */
object ChronicleBeaconProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps),
      OathViolation.InvalidEventOrder("the Beacon takes no start selection"))

  /** The notes the Beacon's steps write. */
  def noteKeys: Vector[NoteKey] = Vector.empty

  private def steps: Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleIntroduceNewcomers,
      Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleWelcomeStranger, Vector.empty),
    // Shape Foundation (CR p. 39) waits for Foundations.
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))
