package distantlands.gameplay.powers.campaign

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.model._

object BlackSwordCard extends Relic(RelicId("R35"), "Black Sword", value = 73, defense = 2):
  val power = PrintedPower(PowerId("relic.black-sword"),
    persistent = false, cost = Cost(favorBurnt = 2),
    modifier = Some(ModifierMark.AttackPlan),
    text = "+5 [attack-die]")
  val powers: Vector[PrintedPower] = Vector(power)

/** Black Sword (relic R35), an attacker's battle plan: "[favor-burnt]
  * [favor-burnt] +5 [attack-die]".
  *
  * The relic must be faceup in the attacker's play area. Two favor
  * are burnt, and nothing is placed on the relic. An
  * attacker with fewer than two favor cannot pay, and the plan is
  * not offered.
  */
case object BlackSword extends BattlePlan:
  val relicId: RelicId = BlackSwordCard.id
  val id: PowerId = BlackSwordCard.power.id
  val Dice: Int = 5

  def cardRef: DecisionOptionRef = DecisionOptionRef.Relic(relicId)
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Attacker)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.relic(relicId).map(source => CampaignPlanOffer(source,
      "Black Sword: burn 2 favor for 5 attack dice",
      Vector(CampaignPlanCost.FavorBurnt(2)),
      Vector(CampaignPlanEffect.AddAttackDice(BlackSword.Dice))))
