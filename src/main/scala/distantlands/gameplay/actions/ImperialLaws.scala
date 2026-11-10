package distantlands.gameplay.actions

import distantlands.model._

/** The Imperial laws that need another player's consent (CR p. 35). A law
  * is not a card, so it is named here for the consent panels, and its
  * notes are declared through `RuleNotes`. */
object ImperialLaws:
  /** Chancellor's Eye: a Citizen moves Imperial warbands from their site to
    * their board only with the Chancellor's consent. */
  val chancellorsEye: PowerId = PowerId("rule.chancellors-eye")
  /** Warband Transfers: an Imperial player moves warbands to or from
    * another Imperial player's board with that player's consent. */
  val warbandTransfers: PowerId = PowerId("rule.warband-transfers")

  val names: Map[PowerId, String] = Map(
    chancellorsEye -> "Chancellor's Eye",
    warbandTransfers -> "Warband Transfers")

  val notes: NoteSet = NoteSet.unowned("ImperialLaws")
  /** "Needs the Chancellor's consent." */
  val needsChancellor: NoteKey = notes.key("needs-chancellor", Vector(
    NotePart.Text("Needs the Chancellor's consent.")))
  /** "Needs {Blue}'s consent." */
  val needsPartner: NoteKey = notes.key("needs-partner", Vector(
    NotePart.Text("Needs "), NotePart.Arg(0), NotePart.Text("'s consent.")))
  /** "{Blue} refused this turn." */
  val refused: NoteKey = notes.key("refused", Vector(
    NotePart.Arg(0), NotePart.Text(" refused this turn.")))
