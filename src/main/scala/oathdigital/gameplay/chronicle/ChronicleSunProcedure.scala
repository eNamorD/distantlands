package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The Sun task (CR p. 36, `docs/rules/chronicle.md` task 1). Each step is
  * its own power window. */
object ChronicleSunProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "the Sun takes no start selection"))
      _ <- ready.game.current.result.toRight(
        OathViolation.InvalidEventOrder("the game has no result"))
    yield Sequence(steps)

  private def steps: Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleRevealRelics, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleFlipLegacies, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleExileDormancy, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleLegacyCap, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleShuffleLegacies, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleCrown, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleAssignTasks, Vector.empty))
