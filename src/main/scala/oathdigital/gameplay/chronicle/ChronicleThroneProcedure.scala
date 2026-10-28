package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The Throne task (CR p. 37, `docs/rules/chronicle.md` task 2), resolved
  * by the player holding its card. Each step is its own power window. */
object ChronicleThroneProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps(catalog)),
      OathViolation.InvalidEventOrder("the Throne takes no start selection"))

  /** The notes the Throne's steps write. */
  def noteKeys: Vector[NoteKey] = ShapeEmpire.noteKeys ++
    BuildEdifice.noteKeys ++ LineageScoring.noteKeys ++ SwearOath.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    // Offer Citizenship (CR p. 37) waits for the Empire phase.
    ChronicleWindows.step(PowerWindow.ChronicleOfferCitizenship, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleShapeEmpire,
      ShapeEmpire.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleBuildEdifice,
      BuildEdifice.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleStockReliquary,
      ReliquaryAndRelics.stock),
    ChronicleWindows.step(PowerWindow.ChronicleScatterRelics,
      ReliquaryAndRelics.scatter(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleScoreLineage,
      LineageScoring.steps),
    ChronicleWindows.step(PowerWindow.ChronicleSwearOath, SwearOath.steps))
