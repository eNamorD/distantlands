package distantlands.gameplay.setup.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.NoteKey

/** The legacies whose SETUP powers are implemented (Legacy Setup powers
  * design), registered on the walker by `WalkerPowerCatalog`. */
object LegacySetupPowers:
  val all: Vector[ContributingPower] = Vector(CircleOfSwords, ScepterBearer)
  /** The lines the powers write in Setup, on Setup's note id, which
    * `SetupProcedure.noteKeys` declares. */
  def noteKeys: Vector[NoteKey] = Vector.empty
