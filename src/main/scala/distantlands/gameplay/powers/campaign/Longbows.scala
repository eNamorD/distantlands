package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.model._

object LongbowsCard extends Denizen(DenizenId("4"), "Longbows", Suit.Order):
  val power = PrintedPower(PowerId("denizen.longbows"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "± [attack-die]")
  val powers: Vector[PrintedPower] = Vector(power)

/** Longbows (card 4), a battle plan for either side: "± [attack-die]".
  *
  * It is free. An attacker adds one attack die; a defender removes one. A
  * bandit defender applies it when it stands faceup at a site the bandits
  * rule, as it applies every free plan.
  */
case object Longbows extends DenizenPlan(LongbowsCard, LongbowsCard.power):
  val Dice: Int = 1

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      PlanDice.label("Longbows", context.side, Longbows.Dice), Vector.empty,
      Vector(PlanDice.effect(context.side, Longbows.Dice))))
