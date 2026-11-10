package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{TheTribunalCard,
  TravelingNegotiatorCard}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Traveling Negotiator (242): "**WHEN NEGOTIATING:** You may negotiate
  * with any player." (catalog batch 12, ruling 7).
  *
  * The rule is `NegotiationReach`, which the Negotiation reads at its
  * start. This power hooks no window. It is registered so the card counts
  * as implemented. It writes no line.
  */
case object TravelingNegotiator extends ContributingPower:
  val id: PowerId = TravelingNegotiatorCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** The Tribunal (E19, intact): "**WHEN NEGOTIATING:** You may negotiate
  * with players at any sites. Players may make binding promises, even into
  * later games." (catalog batch 12, rulings 8 and 11).
  *
  * The rule is `NegotiationReach`: its users reach any player, and a deal
  * holding one opens binding promises, which the engine records in the Game
  * Log and never enforces. "Even into later games" is not tracked. This
  * power hooks no window. It is registered so the card counts as
  * implemented.
  */
case object TheTribunal extends ContributingPower:
  val id: PowerId = TheTribunalCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Bandit Tribunal (E19, ruined): "**WHEN NEGOTIATING:** Players at sites
  * ruled by bandits may negotiate with each other." (catalog batch 12,
  * ruling 9). Persistent, so it applies to every player, ignoring access.
  *
  * The rule is `NegotiationReach`. This power hooks no window. It is
  * registered so the card counts as implemented.
  */
case object BanditTribunal extends ContributingPower:
  val id: PowerId = TheTribunalCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
