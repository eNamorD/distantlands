package distantlands.gameplay.powers.campaign

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.model._

object FearsomeShieldCard extends Relic(RelicId("R27"), "Fearsome Shield", value = 52, defense = 1):
  val power = PrintedPower(PowerId("relic.fearsome-shield"),
    persistent = false, cost = Cost(favorBurnt = 2),
    modifier = Some(ModifierMark.DefensePlan),
    text = "+2 [defense-die]")
  val powers: Vector[PrintedPower] = Vector(power)

/** Fearsome Shield (relic R27), a defender's battle plan: "[favor-burnt]
  * [favor-burnt] +2 defense dice."
  *
  * The relic must be faceup in the defender's play area. Two favor are
  * burnt, and nothing is placed on the relic. A defender with
  * fewer than two favor cannot pay, and the plan is not offered.
  */
case object FearsomeShield extends BattlePlan:
  val relicId: RelicId = FearsomeShieldCard.id
  val id: PowerId = FearsomeShieldCard.power.id

  def cardRef: DecisionOptionRef = DecisionOptionRef.Relic(relicId)
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.relic(relicId).map(source => CampaignPlanOffer(source,
      "Fearsome Shield: burn 2 favor for 2 defense dice",
      Vector(CampaignPlanCost.FavorBurnt(2)),
      Vector(CampaignPlanEffect.AddDefenseDice(2))))
