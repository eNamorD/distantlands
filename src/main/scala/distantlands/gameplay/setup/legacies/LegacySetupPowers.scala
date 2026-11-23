package distantlands.gameplay.setup.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.NoteKey

/** The legacies whose SETUP powers are implemented (Legacy Setup powers
  * design), registered on the walker by `WalkerPowerCatalog`. */
object LegacySetupPowers:
  val all: Vector[ContributingPower] = Vector(CircleOfSwords, ScepterBearer,
    WorldCrafter, Populist, Matchmaker, SecretSociety, Seer,
    TheStandardBearer, Wealthy, QuestToDistantLands)
  /** The lines the powers write in Setup, on Setup's note id, which
    * `SetupProcedure.noteKeys` declares. */
  def noteKeys: Vector[NoteKey] = StartLines.noteKeys ++
    WorldCrafter.setupNoteKeys ++ Populist.setupNoteKeys ++
    Matchmaker.setupNoteKeys ++ SecretSociety.setupNoteKeys ++
    Seer.setupNoteKeys ++ TheStandardBearer.setupNoteKeys ++
    Wealthy.setupNoteKeys ++ QuestToDistantLands.setupNoteKeys
