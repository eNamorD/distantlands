package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Welcome a Stranger, the Beacon's second step (CR p. 39): the holder
  * keeps one drawn newcomer as a facedown starting adviser and the rest go
  * to the world deck bottom. */
object WelcomeStranger:
  val keepDecisionId: String = "chronicle.beacon.keep"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  val keptAdviser: NoteKey = notes.key("kept-newcomer", Vector(
    NotePart.Arg(0), NotePart.Text(" welcomed "), NotePart.Arg(1),
    NotePart.Text(" as a starting adviser")))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
      val hand = state.game.current.temporaryHands
        .getOrElse(holder, Vector.empty).collect { case id: DenizenId => id }
      ChronicleTasks.lineageOf(state, holder) match
        case _ if hand.isEmpty => Vector.empty
        case None => Vector(ChronicleTasks.failing(OathViolation
          .InvalidEventOrder(s"${holder.value} has no lineage")))
        case Some(lineage) => StartingAdviserChoice.steps(holder, lineage,
          hand, keepDecisionId, "Welcome a stranger", keptAdviser, noteId))))
