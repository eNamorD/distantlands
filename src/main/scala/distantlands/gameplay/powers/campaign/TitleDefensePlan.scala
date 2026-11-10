package oathdigital.gameplay.powers.campaign

import oathdigital.model._

/** The title's defense, a defender's battle plan that no card prints: the holder
  * of the Oathkeeper title, when defending, adds one defense die, and a Usurper
  * adds two. Only a player defender who holds the title has it, and it has no
  * card, so its option is a button and an added cost cannot be placed on it.
  *
  * Like the rulebook's own rules it is always present, and it hooks the plan
  * window until the defender uses it. An Imperial defender may use it when
  * an Imperial ally holds the title and lends it (Imperial Tactics, CR
  * p. 35: "the Oathkeeper title is a defensive battle plan").
  */
final case class TitleDefensePlan private () extends BattlePlan:
  def id: PowerId = TitleDefensePlan.id
  def cardRef: DecisionOptionRef = DecisionOptionRef.Button("title")
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    val title = context.ready.game.current.title
    context.user.filter(user =>
      context.setup.defender == CampaignDefender.Player(user)).flatMap { user =>
      title.holder.filter(holder => holder == user ||
        context.lendsTitle(holder)).map { holder =>
        val dice = title.side match
          case TitleSide.Oathkeeper => 1
          case TitleSide.Usurper => 2
        CampaignPlanOffer(CampaignPlanSource.Title(holder),
          s"${title.side} title: add $dice defense ${if dice == 1 then "die" else "dice"}",
          Vector.empty, Vector(CampaignPlanEffect.AddDefenseDice(dice)))
      }
    }

object TitleDefensePlan:
  val id: PowerId = PowerId("title.oathkeeper-defense")
  val plan: TitleDefensePlan = new TitleDefensePlan()
