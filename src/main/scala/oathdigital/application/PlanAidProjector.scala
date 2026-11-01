package oathdigital.application

import oathdigital.gameplay.OathRules
import oathdigital.gameplay.actions.campaign.PlanAid
import oathdigital.model._
import oathdigital.protocol.projection.{PlanAidOfferProjection,
  PlanAidPromptProjection}

/** The offer panel's projection for an Imperial ally while another
  * Imperial player's plan window is parked (Empire phase design, "Offer
  * panel"). Offerable plans are the user's own options the ally lends,
  * which the window already lists, available or awaiting the offer, and
  * each is shown available to the ally, who can offer it; requirements are
  * what the ally meets now. */
private[application] final class PlanAidProjector(rules: OathRules,
    decisions: WalkerDecisionProjector):
  def prompt(context: ScopedProjectionContext)
      : Option[PlanAidPromptProjection] =
    val ready = context.ready
    val state = OathState.Ready(ready)
    val index = ready.game.cardIndex.toOption
    def row(option: DecisionOption) =
      decisions.optionProjection(ready, context.viewer, index, option)
    for
      ally <- context.viewer
      window <- rules.planAidWindow(state)
      (plans, requirements) <- rules.planAidOfferable(state, ally)
      held <- ready.game.current.players.find(_.player == ally)
    yield
      val saved = if ready.planAid.window.contains(window) then
        ready.planAid.offerOf(ally) else AidOffer()
      val used = PlanAid.usedRefs(ready, window, ally)
      PlanAidPromptProjection(window.user.value, side(window.side),
        plans.map(offerable).flatMap(row),
        requirements.flatMap(DecisionOption.forRef).flatMap(row),
        offer(saved), offer(AidOffer(used.filter(saved.plans.contains),
          used.filter(saved.requirements.contains),
          PlanAid.used(ready, window, ally))),
        held.board.favor, held.board.faceUpSecrets)

  /** The user's option as the ally may offer it: whether the user may
    * choose it yet, and whose it is, are the user's concern. */
  private def offerable(option: DecisionOption): DecisionOption =
    option match
      case DecisionOption.Unavailable(inner, _) => offerable(inner)
      case DecisionOption.Lent(inner, _) => offerable(inner)
      case DecisionOption.Badged(inner, badge) =>
        DecisionOption.Badged(offerable(inner), badge)
      case DecisionOption.Priced(inner, _) => offerable(inner)
      case DecisionOption.Warned(inner, _) => offerable(inner)
      case other => other

  private def side(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"

  private def offer(value: AidOffer): PlanAidOfferProjection =
    PlanAidOfferProjection(value.plans.map(key), value.requirements.map(key),
      value.amounts.favor, value.amounts.secrets, value.amounts.favorBurnt,
      value.amounts.secretsBurnt)

  private def key(ref: DecisionOptionRef): String =
    s"${ref.kind}:${ref.wireId}"
