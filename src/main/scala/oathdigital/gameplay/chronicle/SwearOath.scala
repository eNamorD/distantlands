package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.{ProcedureNotes, VisionRules}
import oathdigital.gameplay.powers.{OathkeeperGoalChoice, PowerAnswers}
import oathdigital.model._

/** Swear an Oath, the Throne's last step (CR p. 37). After a win by a
  * Vision, the Oathkeeper goal becomes that Vision's goal; a winner who met
  * several Visions at once chooses which one won (catalog batch 11, ruling
  * 30). Otherwise the Throne holder chooses a goal different from the
  * current one. */
object SwearOath:
  val goalDecisionId: String = "chronicle.swear-oath.goal"
  /** Which Vision won, when the winner met several at once. */
  val visionDecisionId: String = "chronicle.swear-oath.vision"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val swore: NoteKey = notes.key("swore", Vector(
    NotePart.Text("The Oathkeeper goal became "), NotePart.Arg(0)))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(
    Branch((state, _) => state.game.current.result match
      case Some(GameResult(winner, VictoryKind.Visionary, visions)) =>
        visionGoal(state, winner, visions)
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

  /** The winning Vision's goal, set only when it is not already the goal.
    * With several winning Visions the winner chooses which one won. */
  private def visionGoal(state: ReadyGame, winner: PlayerId,
      visions: Vector[VisionId]): Vector[Operation] =
    visions.filter(VisionRules.trueGoal(_).isDefined) match
      case Vector() => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${winner.value} won by no Vision with a goal")))
      case Vector(only) => swear(state, only)
      case several => Vector(
        Decide(visionDecisionId, winner, DecisionQuery.ChooseOne(
          several.map(vision => DecisionOption.Vision(
            DecisionOptionRef.Vision(vision))),
          heading = Some("Swear an Oath: which Vision won?"))),
        BuildOps((live, pending) => chosenVision(pending)
          .filter(several.contains)
          .toRight(PowerAnswers.missing(visionDecisionId))
          .map(vision => swearing(live, vision)), required = true))

  /** The answered Vision, when the answer names one. */
  private def chosenVision(pending: PendingTree): Option[VisionId] =
    PowerAnswers.one(pending, visionDecisionId).collect {
      case DecisionOptionRef.Vision(vision) => vision
    }

  private def swear(state: ReadyGame, vision: VisionId): Vector[Operation] =
    swearing(state, vision) match
      case Vector() => Vector.empty
      case ops => Vector(BuildOps((_, _) => Right(ops), required = true))

  /** `vision`'s goal as the new Oathkeeper goal, unless it already is. */
  private def swearing(state: ReadyGame, vision: VisionId)
      : Vector[CoreOperation] =
    VisionRules.trueGoal(vision)
      .filter(_ != state.game.campaign.oathkeeperGoal)
      .map(SetOathkeeperGoal(_)).toVector
