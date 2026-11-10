package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.model._

object ExtraProvisionsCard extends Denizen(DenizenId("48"), "Extra Provisions", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.extra-provisions"),
    persistent = false, cost = Cost(favor = 1),
    modifier = Some(ModifierMark.DefensePlan),
    text = "+ [defense-die]")
  val powers: Vector[PrintedPower] = Vector(power)

/** Extra Provisions (card 48), a defender's battle plan: "[favor] +
  * [defense-die]".
  *
  * A favor is placed onto the card for one defense die. Bandits pay nothing,
  * so a bandit defender never applies it.
  */
case object ExtraProvisions extends DenizenPlan(ExtraProvisionsCard,
    ExtraProvisionsCard.power):
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Extra Provisions: add 1 defense die", Vector(CampaignPlanCost.Favor(1)),
      Vector(CampaignPlanEffect.AddDefenseDice(1))))
