package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution}
import oathdigital.model._

/** Bandit Crown (R11): "You may use cards at sites ruled by bandits as if
  * you ruled them."
  *
  * The rule is `BanditCrownRule`, which `PowerAccess`, `PlanContext`,
  * `SiteRulers`' consent granters and `PayOrAsk.letOffBy` read. This power
  * hooks no window. It is registered so the card counts as implemented.
  * It writes no line.
  */
case object BanditCrown extends ContributingPower:
  val id: PowerId = BanditCrownCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
