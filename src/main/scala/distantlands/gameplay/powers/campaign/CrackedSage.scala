package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.model._

object CrackedSageCard extends Denizen(DenizenId("83"), "Cracked Sage", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.cracked-sage"),
    persistent = false, cost = Cost(secret = 1, favorBurnt = 1),
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "±4 [attack-die] if your enemy has an [suit-arcane] adviser.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Cracked Sage (card 83), a battle plan for either side: "[secret]
  * [favor-burnt] ±4 [attack-die] if your enemy has an [suit-arcane] adviser."
  *
  * A secret is placed onto the card and a favor is burnt. An attacker adds
  * four attack dice; a defender removes four. It is offered only when the
  * enemy, the other side's player, has a faceup arcane adviser. Bandits hold
  * no advisers, so an attacker is never offered it against them.
  */
case object CrackedSage extends DenizenPlan(CrackedSageCard,
    CrackedSageCard.power):
  val Dice: Int = 4

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId)
      .filter(_ => context.enemyHasAdviser(Suit.Arcane))
      .map(source => CampaignPlanOffer(source,
        PlanDice.label("Cracked Sage", context.side, CrackedSage.Dice),
        Vector(CampaignPlanCost.Secret(1), CampaignPlanCost.FavorBurnt(1)),
        Vector(PlanDice.effect(context.side, CrackedSage.Dice))))
