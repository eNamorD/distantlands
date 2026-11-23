package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.{ConspiratorReveal,
  RoyalLine}
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The final winner, settled once the game has a result and before Start
  * Chronicle (Legacy in-play powers R36): Royal Line's choice, in the
  * `ChronicleFinalWinner` window, then Conspirator's comparison
  * (`ConspiratorReveal`). Then Ruined Temple's pledged winner takes the
  * bandit sites (`RuinedTempleWin`; catalog batch 13, ruling 18). Start
  * Chronicle then belongs to the winner this
  * leaves, and the winner banner shows them throughout. Nothing in the
  * rules happens between the game's end and the Sun's first window, so
  * only the digital Start Chronicle pause moves relative to them. The
  * driver starts it once (`ChronicleSequence.opening`).
  */
object ChronicleWinnerProcedure:
  /** The lines its steps write, on its note id. */
  def noteKeys: Vector[NoteKey] = RoyalLine.winnerNoteKeys ++
    ConspiratorReveal.noteKeys ++ RuinedTempleWin.noteKeys

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- ProcedureStart.noSelection("the final winner", args)
      _ <- ChronicleTasks.winner(ready)
    yield Sequence((ChronicleWindows.step(PowerWindow.ChronicleFinalWinner,
      Vector.empty) +: ConspiratorReveal.steps) :+ RuinedTempleWin.step)
