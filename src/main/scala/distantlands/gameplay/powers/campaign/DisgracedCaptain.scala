package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.model._

object DisgracedCaptainCard extends Denizen(DenizenId("20"), "Disgraced Captain", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.disgraced-captain"),
    persistent = false, cost = Cost(favor = 1, favorBurnt = 1),
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "±4 [attack-die] if the defender rules an [suit-order] card " +
      "_(even an adviser)_.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Disgraced Captain (card 20), a battle plan for either side: "[favor]
  * [favor-burnt] ±4 [attack-die] if the defender rules an [suit-order] card
  * (even an adviser)."
  *
  * A favor is placed onto the card and a favor is burnt. An attacker adds four
  * attack dice; a defender removes four. It is offered only while the
  * Campaign's defender rules an order card (`RuledCards`). It reads the
  * defender whichever side uses it, as printed, so a defender's use checks
  * their own cards. Against bandits it holds when an order card stands at any
  * site bandits rule. Bandits pay nothing, so a bandit defender never applies
  * it.
  */
case object DisgracedCaptain extends DenizenPlan(DisgracedCaptainCard,
    DisgracedCaptainCard.power):
  val Dice: Int = 4

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId)
      .filter(_ => context.cardsRuled(context.setup.defender,
        Suit.Order).nonEmpty)
      .map(source => CampaignPlanOffer(source,
        PlanDice.label("Disgraced Captain", context.side,
          DisgracedCaptain.Dice),
        Vector(CampaignPlanCost.Favor(1), CampaignPlanCost.FavorBurnt(1)),
        Vector(PlanDice.effect(context.side, DisgracedCaptain.Dice))))
