package distantlands.application

import distantlands.gameplay.consent.ConsentUseMarks
import distantlands.gameplay.powers.ConsentPowers
import distantlands.model._

/** Whether a granter may change one answer of the turn now, and if not,
  * why (reconsider design, "Commands" and "Projection"). One judgement
  * serves the desk, which refuses by it, and the projection, which shows
  * the entry's button withheld, so the two cannot disagree. The cases are
  * judged in the desk's order. */
private[application] enum ConsentChange(val key: String):
  /** Nothing stands in the way. */
  case Open extends ConsentChange("open")
  /** The game is over (implementation ruling R24). */
  case Over extends ConsentChange("over")
  /** A request about the same option waits on an answer (implementation
    * rulings R8 and R14). */
  case Waiting extends ConsentChange("waiting")
  /** The request was about an automatic power; its answer is final. */
  case Automatic extends ConsentChange("automatic")
  /** The granter no longer rules `power`, which the answer covers. This
    * is also every ask nobody rules: a payer's, an Imperial law's. */
  case Lapsed(power: PowerId) extends ConsentChange("lapsed")
  /** The requester chose the option, and that choice stands (ruling
    * 21). */
  case Used extends ConsentChange("used")

private[application] object ConsentChange:
  /** Whether a request about `requester` choosing `target` at
    * `decisionId` waits on an answer. */
  def waiting(ready: ReadyGame, requester: PlayerId, decisionId: String,
      target: DecisionOptionRef): Boolean =
    ready.consent.pending.exists(request =>
      request.requester == requester && request.decisionId == decisionId &&
        request.target == target)

  def of(ready: ReadyGame, powers: ConsentPowers, granter: PlayerId,
      entry: ConsentRequest, ask: ConsentAsk): ConsentChange =
    val lost = ask.powers.find(power =>
      !powers.granter(ready, power).contains(granter))
    if ready.game.current.result.nonEmpty then Over
    else if waiting(ready, entry.requester, entry.decisionId,
        entry.target) then Waiting
    else if entry.use.nonEmpty then Automatic
    else lost match
      case Some(power) => Lapsed(power)
      case None if ConsentUseMarks.locked(ready, entry.requester,
          entry.decisionId, entry.target) => Used
      case None => Open
