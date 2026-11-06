package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.{ProcedureNotes, VisionRules}
import oathdigital.gameplay.powers.OathkeeperGoalChoice
import oathdigital.model._

/** Swear an Oath, the Throne's last step (CR p. 37). After a win by a
  * Vision, the Oathkeeper goal becomes that Vision's goal. Otherwise the
  * Throne holder chooses a goal different from the current one. */
object SwearOath:
  val goalDecisionId: String = "chronicle.swear-oath.goal"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val swore: NoteKey = notes.key("swore", Vector(
    NotePart.Text("The Oathkeeper goal became "), NotePart.Arg(0)))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(
    Branch((state, _) => state.game.current.result match
      case Some(GameResult(winner, VictoryKind.Visionary)) =>
        visionGoal(state, winner)
      case _ => ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
        Vector(
          Decide(goalDecisionId, holder, DecisionQuery.ChooseOne(
            OathkeeperGoalChoice.options(state, Map.empty),
            heading = Some("Swear an Oath"))),
          BuildOps((live, pending) => OathkeeperGoalChoice.set(live, pending,
            goalDecisionId, Set.empty), required = true)))),
    Note(noteId, states => states.previous.collect {
      case (before, after) if before.game.campaign.oathkeeperGoal !=
          after.game.campaign.oathkeeperGoal =>
        swore.sourceless(NoteArg.Goal(after.game.campaign.oathkeeperGoal))
    }))

  /** The winning Vision's goal, set only when it is not already the goal. */
  private def visionGoal(state: ReadyGame, winner: PlayerId)
      : Vector[Operation] =
    state.game.current.player(winner)
      .flatMap(_.revealedVision).flatMap(vision =>
        VisionRules.trueGoal(vision.id)) match
      case None => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${winner.value} won by no Vision with a goal")))
      case Some(goal) if goal == state.game.campaign.oathkeeperGoal =>
        Vector.empty
      case Some(goal) => Vector(BuildOps((_, _) =>
        Right(Vector(SetOathkeeperGoal(goal))), required = true))
