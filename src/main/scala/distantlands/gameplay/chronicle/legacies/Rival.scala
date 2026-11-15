package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object RivalCard extends Legacy(LegacyId("L17"), "Rival",
    LegacyGoal.PeoplesFavor, Suit.Order):
  val power = PrintedPower(PowerId("legacy.rival"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** In Shape Empire, you may prevent " +
      "any number of denizens from being discarded.\n\n**IF YOU ARE " +
      "AN EXILE:** **SETUP:** Before resolving foundations, fill " +
      "the three Cradlemost empty site slots using the Empire " +
      "setup rules, but place one warband of yours on each _(not " +
      "an Imperial warband)_. You may place your pawn on these " +
      "sites.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Rival (legacy L17), its Throne half: "In Shape Empire, you may prevent
  * any number of denizens from being discarded." Shape Empire asks
  * `ChronicleLegacyRules.protector` before its discard (Legacy Chronicle
  * powers design, ruling 9): the holder keeps any of the cards it would
  * discard, ruined edifices included (R8), in one choose-many. Its SETUP
  * half is read at game start (`StartingLegacies`, Legacy Setup powers
  * ruling 8) and by `SetupPawnSites`. This power hooks no window; it is
  * registered so the card counts as implemented. Line: "p1 protected 2
  * denizens with Rival" (R12). */
case object Rival extends ContributingPower:
  val id: PowerId = RivalCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.rival.${player.value}"

  private val notes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.ChronicleThrone))
  val protectedWith: NoteKey = notes.key("rival-protected", Vector(
    NotePart.Arg(0), NotePart.Text(" protected "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "denizen", "denizens"),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared
