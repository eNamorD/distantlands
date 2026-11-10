package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.model._

object WatchdogCard extends Denizen(DenizenId("234"), "Watchdog", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.watchdog"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.DefensePlan),
    text = "+[defense-die] if any target is in the Cradle.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Watchdog (card 234), a defender's battle plan: "+1 defense die if any target
  * is in the Cradle."
  *
  * It is used by the ruler of its card, from an adviser or a ruled site. A
  * bandit defender uses it from a site Bandits rule, without choosing. A Raid
  * targets no site, so it never applies to one.
  */
case object Watchdog extends DenizenPlan(WatchdogCard, WatchdogCard.power):
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    if !context.targetsIn(Region.Cradle) then None
    else context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Watchdog: add 1 defense die", Vector.empty,
      Vector(CampaignPlanEffect.AddDefenseDice(1))))
