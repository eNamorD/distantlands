package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.DeedWriterCard
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Deed Writer (146): "**WHEN NEGOTIATING:** You may give, receive, and
  * swap sites. Old ruler moves warbands from site to their board, and new
  * ruler moves warbands to site from their board." (catalog batch 12,
  * rulings 18 to 20 and 27 to 29).
  *
  * The rule is `NegotiationSites` and `DealRules`, which the Negotiation
  * reads. This power hooks no window. Its moves write the Negotiation's own
  * lines.
  */
case object DeedWriter extends ContributingPower:
  val id: PowerId = DeedWriterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
