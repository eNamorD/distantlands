package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.{IronHand,
  LegacyChroniclePowers}
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The Throne task (CR p. 37, `docs/rules/chronicle.md` task 2), resolved
  * by the player holding its card. Each step is its own power window.
  * Three empty windows hold the legacies' added steps: one at the start,
  * where Reformer acts, one after Build Edifice, where Revolutionary does,
  * and one before Scatter Relics, where Heirloom does (Legacy Chronicle
  * powers design, rulings 11-13). */
object ChronicleThroneProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the Throne", args)
      .map(_ => Sequence(steps(catalog)))

  /** The notes the Throne's steps write. */
  def noteKeys: Vector[NoteKey] = OfferCitizenship.noteKeys ++
    ShapeEmpire.noteKeys ++
    BuildEdifice.noteKeys ++ LineageScoring.noteKeys ++ SwearOath.noteKeys ++
    LegacyChroniclePowers.throneNoteKeys ++
    IronHand.noteKeys(TriggeredProcedureRef.ChronicleThrone)

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleStartOfThrone, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleOfferCitizenship,
      OfferCitizenship.steps),
    ChronicleWindows.step(PowerWindow.ChronicleShapeEmpire,
      ShapeEmpire.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleBuildEdifice,
      BuildEdifice.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleAfterBuildEdifice,
      Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleStockReliquary,
      ReliquaryAndRelics.stock),
    ChronicleWindows.step(PowerWindow.ChronicleBeforeScatterRelics,
      Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleScatterRelics,
      ReliquaryAndRelics.scatter(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleScoreLineage,
      LineageScoring.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleSwearOath, SwearOath.steps))
