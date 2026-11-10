package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Council Arbiter (200): "**WHEN NEGOTIATING:** You may give, receive,
  * and swap advisers." (catalog batch 12, rulings 14, 16 and 17).
  *
  * The rule is `NegotiationAdvisers` and `DealRules`, which the
  * Negotiation reads. This power hooks no window. It is registered so the
  * card counts as implemented. Its moves write the Negotiation's own
  * "gave" lines.
  */
case object CouncilArbiter extends ContributingPower:
  val id: PowerId = CouncilArbiterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Festival District (E21, intact): "**WHEN NEGOTIATING:** Players may
  * give, receive, and swap advisers." (catalog batch 12, rulings 15 to
  * 17).
  *
  * The rule is `NegotiationAdvisers` and `DealRules`. This power hooks no
  * window. It is registered so the card counts as implemented.
  */
case object FestivalDistrict extends ContributingPower:
  val id: PowerId = FestivalDistrictCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
