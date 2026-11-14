package distantlands.gameplay.setup.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.{NoteKey, Operation}

/** The legacies whose SETUP powers are implemented (Legacy Setup powers
  * design), registered on the walker by `WalkerPowerCatalog`. */
object LegacySetupPowers:
  val all: Vector[ContributingPower] = Vector(CircleOfSwords, ScepterBearer,
    WorldCrafter, Populist, Matchmaker)
  /** The lines the powers write in Setup, on Setup's note id, which
    * `SetupProcedure.noteKeys` declares. */
  def noteKeys: Vector[NoteKey] = StartLines.noteKeys ++
    WorldCrafter.setupNoteKeys ++ Populist.setupNoteKeys ++
    Matchmaker.setupNoteKeys
  /** The lines that open Setup for what game start's powers did. */
  def startNotes: Vector[Operation] = StartLines.operations
