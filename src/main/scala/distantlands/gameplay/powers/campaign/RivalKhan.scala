package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.model._

object RivalKhanCard extends Denizen(DenizenId("156"), "Rival Khan", Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.rival-khan"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "±4 [attack-die] if your enemy has a [suit-nomad] adviser. " +
      "At end, discard Rival Khan.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Rival Khan (card 156), a battle plan for either side: "±4 [attack-die] if
  * your enemy has a [suit-nomad] adviser. At end, discard Rival Khan."
  *
  * It is free. An attacker adds four attack dice; a defender removes four. It
  * is offered only when the enemy has a faceup nomad adviser, so never against
  * bandits. The card is discarded through the standard discard once the
  * Campaign has resolved, whoever won, as Horse Archers is.
  */
case object RivalKhan extends DenizenPlan(RivalKhanCard,
    RivalKhanCard.power) with DiscardedAtEnd:
  val Dice: Int = 4

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId)
      .filter(_ => context.enemyHasAdviser(Suit.Nomad))
      .map(source => CampaignPlanOffer(source,
        PlanDice.label("Rival Khan", context.side, RivalKhan.Dice),
        Vector.empty, Vector(PlanDice.effect(context.side, RivalKhan.Dice))))
