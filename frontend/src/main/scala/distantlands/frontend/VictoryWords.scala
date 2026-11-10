package oathdigital.frontend

/** What the winner banner and the game-over status call a win, from its
  * victory kind's wire key: "Usurper victory", "Successor to the
  * Chancellor". */
private[frontend] object VictoryWords:
  def of(key: Option[String]): String = key match
    case Some("usurper") => "Usurper victory"
    case Some("visionary") => "Vision victory"
    case Some("oathkeeper") => "Oathkeeper victory"
    case Some("random-selection") => "victory by random selection"
    case Some("stable-regime") => "Stable Regime victory"
    case Some("chancellor-exhaustion") => "War Exhaustion victory"
    case Some("successor") => "Successor to the Chancellor"
    case Some(other) => s"${other.replace('-', ' ')} victory"
    case None => "victory"
