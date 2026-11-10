package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The Throne task (CR p. 37, `docs/rules/chronicle.md` task 2), resolved
  * by the player holding its card. Each step is its own power window. */
object ChronicleThroneProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the Throne", args)
      .map(_ => Sequence(steps(catalog)))

  /** The notes the Throne's steps write. */
  def noteKeys: Vector[NoteKey] = OfferCitizenship.noteKeys ++
    ShapeEmpire.noteKeys ++
    BuildEdifice.noteKeys ++ LineageScoring.noteKeys ++ SwearOath.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleOfferCitizenship,
      OfferCitizenship.steps),
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
