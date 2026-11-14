package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.NoteKey

/** The legacies whose Chronicle powers are implemented (Legacy Chronicle
  * powers design), registered on the walker by `WalkerPowerCatalog`. */
object LegacyChroniclePowers:
  val all: Vector[ContributingPower] = Vector(Steadfast, Gatekeeper)
  /** The lines the powers write in the Sun, on the Sun's note id, which
    * `ChronicleSunProcedure.noteKeys` declares. */
  def sunNoteKeys: Vector[NoteKey] = Gatekeeper.sunNoteKeys
