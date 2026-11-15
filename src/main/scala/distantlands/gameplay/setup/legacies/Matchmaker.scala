package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object MatchmakerCard extends Legacy(LegacyId("L14"), "Matchmaker",
    LegacyGoal.PeoplesFavor, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.matchmaker"),
    persistent = false, cost = Cost.free,
    text = "Whenever players draw legacies from the deck and choose one " +
      "to keep, you draw and choose for them instead. _The Amber " +
      "Doors edifice does not trigger this._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Matchmaker (legacy L14), its Setup half (Legacy Setup powers design,
  * ruling 13). In Quiet Ambitions' keep its holder answers every other
  * player's keep, under the keep's own id. The holder draws that player's
  * legacies into their own hand, so they see both and the owner only the
  * one kept for them (catalog batch 13, ruling 6). During a game its holder
  * draws Ancient Writ's legacies into their own hand and keeps one for the
  * Writ's holder (catalog batch 13, ruling 9; `AncientWrit`); Amber Doors
  * does not trigger it, and Lost Doors' discard is the owner's (ruling 10).
  * This power hooks no window. Line: "p3 kept a legacy for p2 with Matchmaker". */
case object Matchmaker extends ContributingPower:
  val id: PowerId = MatchmakerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  val keptFor: NoteKey = notes.key("matchmaker-kept-for", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy for "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared
