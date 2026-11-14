package distantlands.gameplay.setup

import distantlands.model._

/** The placement a `SetupPawnPlaced` window belongs to (R27): the latest
  * answer to a pawn-placement decision, whatever the window has asked
  * since. A power hooked there reads it, so a question it asks does not
  * change which player the window is for when the walk is rebuilt. */
object SetupPawnPlacement:
  val prefix: String = "setup.pawn-placement."

  def isPawnDecision(id: String): Boolean = id.startsWith(prefix)

  def latest(answered: Vector[Answered])
      : Option[(PlayerId, Option[SiteId])] =
    answered.reverse.collectFirst {
      case Answered(id, DecisionAnswer.ChooseOneAnswer(ref), by)
          if isPawnDecision(id) => by -> (ref match
            case DecisionOptionRef.Site(site) => Some(site)
            case _ => None)
    }
