package oathdigital.gameplay.roles

import oathdigital.model._

/** The lines a "become a Citizen" power writes under its own name: "Became
  * a Citizen." or, in a game that had no Empire, "Became the Chancellor." */
final case class RoleNotes(citizen: NoteKey, chancellor: NoteKey):
  /** The line for `player`'s new role, once the change has run. */
  def became(states: NoteStates, player: PlayerId, source: PowerSourceRef)
      : Option[LogNote] =
    Imperials.role(states.now, player).collect {
      case Role.Chancellor => chancellor(source)
      case Role.Citizen => citizen(source)
    }

object RoleNotes:
  def declare(notes: NoteSet): RoleNotes = RoleNotes(
    notes.key(NoteKey.Used, Vector(NotePart.Text("Became a Citizen."))),
    notes.key("used.chancellor",
      Vector(NotePart.Text("Became the Chancellor."))))
