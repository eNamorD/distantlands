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
  * what the ally meets now. Each row is worded by [[DecisionOptionRows]],
  * the same module the parked window's own rows come from. */
private[application] final class PlanAidProjector(rules: OathRules,
    rows: DecisionOptionRows):
  def prompt(context: ScopedProjectionContext)
      : Option[PlanAidPromptProjection] =
    val ready = context.ready
    val state = OathState.Ready(ready)
    val index = ready.game.cardIndex.toOption
    def row(option: DecisionOption) =
      rows.project(ready, context.viewer, index, option)
    for
      ally <- context.viewer
      (window, plans, requirements) <- rules.planAidOfferable(state, ally)
      held <- ready.game.current.player(ally)
    yield
      val saved = if ready.planAid.window.contains(window) then
        ready.planAid.offerOf(ally) else AidOffer()
      val used = PlanAid.usedRefs(ready, window, ally)
      // A saved or used item the window no longer lists (a plan the user
      // chose, a requirement no longer met) stays in the panel, so it can
      // be shown locked or taken out.
      def kept(listed: Vector[DecisionOption], refs: Vector[DecisionOptionRef])
          : Vector[DecisionOption] = listed ++ refs.distinct
        .filterNot(ref => listed.exists(_.ref == ref)).flatMap(standalone)
      val offerablePlans = plans.map(offerable)
      PlanAidPromptProjection(window.user.value, side(window.side),
        kept(offerablePlans, saved.plans ++ used.filter(saved.plans.contains))
          .flatMap(row),
        kept(requirements.flatMap(DecisionOption.forRef),
          saved.requirements).flatMap(row),
        offer(saved), offer(AidOffer(used.filter(saved.plans.contains),
          used.filter(saved.requirements.contains),
          PlanAid.used(ready, window, ally))),
        held.board.favor, held.board.faceUpSecrets)

  /** An item's option without the window's: the title's button keeps its
    * name. */
  private def standalone(ref: DecisionOptionRef): Option[DecisionOption] =
    ref match
      case button: DecisionOptionRef.Button =>
        Some(DecisionOption.Button(button, "Oathkeeper title's defense"))
      case other => DecisionOption.forRef(other)

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
