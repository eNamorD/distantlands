package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower, SiteOnly}
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.campaign.PlanAid
import oathdigital.model._

object HeartsAndMindsCard extends Denizen(DenizenId("138"),
    "Hearts and Minds", Suit.Hearth) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.hearts-and-minds"),
    persistent = false, cost = Cost(favor = 3),
    modifier = Some(ModifierMark.DefensePlan),
    text = "As defender, you're victorious now. At end, discard Hearts " +
      "and Minds unless you hold the People's Favor.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Hearts and Minds (card 138, site only), a defender's battle plan: "As
  * defender, you're victorious now. At end, discard Hearts and Minds unless
  * you hold the People's Favor."
  *
  * Three favor are placed onto the card, so bandits never use it. Once
  * used, it decides the battle for the defender (`VictoriousNow`): nothing
  * is rolled or sacrificed, and the defender wins. The defeated attacker's
  * losses are the printed ones, half the surviving force. At the end of the
  * Campaign the card is discarded unless its user holds the People's Favor
  * then, a People's Favor won in this Campaign included. An Imperial ally
  * may meet that for its user (ruling R12): it declares the People's Favor
  * as its requirement, which is checked at the end, not to choose it, and
  * the card stays when the ally who lent the People's Favor holds it then.
  *
  * It writes no line of its own, only the discard's "Discarded after the
  * Campaign.".
  */
case object HeartsAndMinds extends DenizenPlan(HeartsAndMindsCard,
    HeartsAndMindsCard.power) with DiscardedAtEnd:
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Hearts and Minds: win the battle", Vector(CampaignPlanCost.Favor(3)),
      Vector.empty, requires = Vector(PlanNeed.PeoplesFavor)))

  override def wrapping: Map[PowerWindow, BattlePlan.Wrap] =
    VictoriousNow.silent

  override def endDiscard: PlanUse => Vector[Operation] = use =>
    if use.user.exists(favored(use.ready, _)) || lentFavor(use) then
      Vector.empty
    else PlanDiscard.afterCampaign(id, use, card)

  /** Whether an ally lent the People's Favor for the plan and holds it
    * now. */
  private def lentFavor(use: PlanUse): Boolean = use.user.exists(user =>
    PlanAid.allies(use.ready, user).exists(ally => favored(use.ready, ally) &&
      PlanAid.usedRefs(use.ready, AidWindow(use.actor,
        CampaignPlanSide.Defender, user), ally).contains(PlanAid.peoplesFavor)))

  private def favored(ready: ReadyGame, player: PlayerId): Boolean =
    BannerRules.holder(ready.game.current, Banner.PeoplesFavor)
      .contains(player)
