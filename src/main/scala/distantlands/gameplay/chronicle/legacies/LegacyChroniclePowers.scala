package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.NoteKey

/** The legacies whose Chronicle powers are implemented (Legacy Chronicle
  * powers design), registered on the walker by `WalkerPowerCatalog`. */
object LegacyChroniclePowers:
  val all: Vector[ContributingPower] = Vector(Steadfast, Gatekeeper,
    Chronicler, Rival, GreatArchitect, Revolutionary, Reformer,
    Heirloom, TheEar)
  /** The lines the powers write in the Sun, on the Sun's note id, which
    * `ChronicleSunProcedure.noteKeys` declares. */
  def sunNoteKeys: Vector[NoteKey] =
    Gatekeeper.sunNoteKeys ++ Chronicler.sunNoteKeys
  /** The lines the powers write in the Throne, on the Throne's note id,
    * which `ChronicleThroneProcedure.noteKeys` declares. */
  def throneNoteKeys: Vector[NoteKey] =
    Rival.throneNoteKeys ++ Revolutionary.throneNoteKeys ++
      Reformer.throneNoteKeys ++ Heirloom.throneNoteKeys ++
      TheEar.throneNoteKeys
