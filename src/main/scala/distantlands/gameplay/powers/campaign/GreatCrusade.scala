package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.{EffectiveSuit, PledgeToDiscordCard}
import oathdigital.model._

object GreatCrusadeCard extends Denizen(DenizenId("164"), "Great Crusade", Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.great-crusade"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "± [attack-die] per [suit-nomad] card you rule. At end, " +
      "discard Great Crusade.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Great Crusade (card 164), a battle plan for either side: "± [attack-die]
  * per [suit-nomad] card you rule. At end, discard Great Crusade."
  *
  * It is free. An attacker adds one attack die per nomad card they rule; a
  * defender removes one per card. It counts itself, so it is always worth at
  * least one die. A bandit defender counts the nomad cards at every site
  * bandits rule. The card is discarded through the standard discard once the
  * Campaign has resolved, whoever won, as Horse Archers is.
  */
case object GreatCrusade extends DenizenPlan(GreatCrusadeCard,
    GreatCrusadeCard.power) with DiscardedAtEnd:
  /** The nomad cards the plan's user rules, counting `card` once whatever its
    * orientation: choosing a plan reveals a facedown adviser, so it is faceup
    * and ruled once chosen. A bandit defender counts the nomad cards at every
    * site bandits rule. `card` counts nothing when it is an adviser of a user
    * who has Pledge to Discord faceup: revealed, it is Discord. Pledge of
    * Defense counts the same.
    */
  private[campaign] def nomads(context: PlanContext,
      card: DenizenId): Int =
    context.cardsRuled(context.ruler, Suit.Nomad).filter(_ != card)
      .map(EffectiveSuit.weight).sum +
      (if pledged(context, card) then 0 else 1)

  /** Whether `card` is an adviser of the plan's user, who has Pledge to
    * Discord faceup. */
  private def pledged(context: PlanContext, card: DenizenId): Boolean =
    context.user.flatMap(context.ready.game.current.player).exists(player =>
      player.advisers.exists(_.id == card) && player.advisers.exists {
        case DenizenState(held, Orientation.FaceUp, _) =>
          held == PledgeToDiscordCard.id
        case _ => false
      })

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map { source =>
      val dice = GreatCrusade.nomads(context, cardId)
      CampaignPlanOffer(source, PlanDice.label("Great Crusade", context.side,
        dice), Vector.empty, Vector(PlanDice.effect(context.side, dice)))
    }
