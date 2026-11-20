package distantlands.gameplay.consent

import distantlands.model._

/** The use marks (reconsider design, "State"; ruling 21). A mark records
  * that a requester acted on a granted request. The fold of the walker
  * event that records an answer sets it, from the ledger as it stands at
  * that journal position, so live play and a whole-journal replay set
  * the same marks. Undo returns to a game saved before the answer, so
  * the mark goes with no undo code. */
object ConsentUseMarks:
  /** `ready` after `by` answered `decisionId` with `answer`: marked for
    * each option the answer chooses that a request settled this turn
    * granted `by` there. A choose-many answer marks each such option it
    * includes. Marks of other turns are dropped when one is set
    * (implementation ruling R11). */
  def answered(ready: ReadyGame, decisionId: String, answer: DecisionAnswer,
      by: PlayerId): ReadyGame =
    val now = ConsentTurn.of(ready)
    val chosen = answer match
      case DecisionAnswer.ChooseOneAnswer(selected) => Vector(selected)
      case DecisionAnswer.ChooseManyAnswer(selected) => selected
      case _ => Vector.empty
    val used = chosen.filter(ready.consent.grantedTo(by, decisionId, _, now))
      .map(UseMark(now, by, decisionId, _))
    if used.isEmpty then ready
    else ready.updateCurrent(current => current.copy(
      useMarks = current.useMarks.filter(_.turn == now) ++ used))

  /** Whether the answers about `requester` choosing `target` at
    * `decisionId` are locked: the choice was made this turn and stands. */
  def locked(ready: ReadyGame, requester: PlayerId, decisionId: String,
      target: DecisionOptionRef): Boolean =
    ready.game.current.useMarks.contains(UseMark(ConsentTurn.of(ready),
      requester, decisionId, target))
