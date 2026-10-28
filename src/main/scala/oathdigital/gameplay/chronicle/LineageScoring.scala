package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Score Lineage Points, the Throne's sixth step (CR p. 37, Chronicle ruling
  * 13). The winner scores 3 lineage points. Any number of times, they may
  * spend 2 of the points they are scoring to let another seated player score
  * 1. Legacy points (1 per matching card at sites the winner rules) arrive
  * with the Legacies slice through this step's window. */
object LineageScoring:
  val giftsDecisionId: String = "chronicle.score-lineage.gifts"
  val winnerPoints: Int = 3

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val scored: NoteKey = notes.key("scored", Vector(NotePart.Arg(0),
    NotePart.Text(" scored "), NotePart.Arg(1)))
  val gave: NoteKey = notes.key("gave", Vector(NotePart.Arg(0),
    NotePart.Text(" gave "), NotePart.Arg(1), NotePart.Text(" to "),
    NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.winner(state).fold(violation =>
      Vector(ChronicleTasks.failing(violation)), score(state, _))))

  private def points(amount: Int): NoteArg =
    NoteArg.Amount(amount, NoteUnit.LineagePoint)

  private def score(state: ReadyGame, winner: PlayerId): Vector[Operation] =
    val others = ChronicleTasks.seated(state).filterNot(_ == winner)
    val giftable = winnerPoints / 2
    if giftable == 0 || others.isEmpty then
      Vector(award(state, winner, Vector.empty))
    else Vector(
      Decide(giftsDecisionId, winner, DecisionQuery.Distribute(
        others.map(player => DistributeSlot(DecisionOptionRef.Player(player),
          0, giftable, None)), 0, giftable,
        heading = Some("Give lineage points: 2 of yours for 1 of theirs"),
        confirmLabel = "Score")),
      Branch((live, pending) => PowerAnswers.distribution(pending,
        giftsDecisionId) match
        case Some(rows) => Vector(award(live, winner, rows.collect {
          case DistributeAmount(DecisionOptionRef.Player(player), amount)
              if amount > 0 => player -> amount
        }))
        case None => Vector(ChronicleTasks.failing(
          PowerAnswers.missing(giftsDecisionId)))))

  /** The winner's points less 2 per gift, then each gift, and their lines. */
  private def award(state: ReadyGame, winner: PlayerId,
      gifts: Vector[(PlayerId, Int)]): Operation =
    val kept = winnerPoints - 2 * gifts.map(_._2).sum
    val scoring = Option.when(kept > 0)(winner -> kept).toVector ++ gifts
    val ops = scoring.flatMap((player, amount) =>
      ChronicleTasks.lineageOf(state, player).map(
        LineagePointsScored(_, amount)))
    Sequence(BuildOps((_, _) => Right(ops), required = true) +:
      (scoring.map((player, amount) => Note(noteId, _ => Some(
        scored.sourceless(NoteArg.Player(player), points(amount))))) ++
        gifts.map((player, amount) => Note(noteId, _ => Some(
          gave.sourceless(NoteArg.Player(winner), points(amount),
            NoteArg.Player(player)))))))
