package oathdigital.application

import oathdigital.gameplay.{OathRules, SpendableFavor}
import oathdigital.gameplay.actions.campaign.{CampaignIds, PlanAid}
import oathdigital.model._
import oathdigital.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** The Imperial Tactics command (Empire phase design, "1b-ii"): an ally's
  * whole offer, accepted at any time while an Imperial user's plan
  * decision is parked. It opens no command batch, as a consent command
  * opens none, so nothing here is undoable; an ally takes an offer back by
  * saving a smaller one. */
private[application] final class PlanAidDesk(rules: OathRules):
  private type Result = Either[OathViolation, Vector[OathEvent]]

  def save(state: OathState, ally: PlayerId, offer: AidOffer): Result = for
    ready <- playing(state)
    window <- rules.planAidWindow(state).toRight(unavailable(
      "no Imperial battle-plan decision is open"))
    _ <- Either.cond(ally != window.user, (),
      unavailable("a player cannot aid their own plans"))
    offerable <- rules.planAidOfferable(state, ally)
      .toRight(unavailable(s"${ally.value} is not an Imperial ally"))
    (_, lendable, requirements) = offerable
    used = PlanAid.usedRefs(ready, window, ally)
    spent = PlanAid.used(ready, window, ally)
    _ <- Either.cond(offer.plans.distinct == offer.plans &&
      offer.requirements.distinct == offer.requirements, (),
      unavailable("an offer names each plan and requirement once"))
    _ <- offer.plans.find(ref => !used.contains(ref) &&
      !lendable.exists(_.ref == ref)).toLeft(()).left.map(ref =>
      unavailable(s"${ref.wireId} is not a plan ${ally.value} can lend"))
    _ <- offer.requirements.find(ref => !used.contains(ref) &&
      !requirements.contains(ref)).toLeft(()).left.map(ref =>
      unavailable(s"${ally.value} does not meet ${ref.wireId}"))
    _ <- used.find(ref => !keeps(offer, ready, window, ally, ref))
      .toLeft(()).left.map(ref =>
        unavailable(s"${ref.wireId} was used and cannot be withdrawn"))
    _ <- Either.cond(offer.amounts.atLeast(spent), (),
      unavailable("an offer cannot go below what was used"))
    _ <- fits(ready, ally, offer.amounts - spent)
  yield Vector(PlanAidSaved(ally, window, offer))

  /** The clearing appended after every command once the offers' window is
    * over: its walk has ended, or another Imperial plan decision is parked.
    * The walk goes on while its answers hold the window's plan decision, so
    * while it is parked elsewhere inside the Campaign, such as at a plan's
    * amount question, the offers stand, since the plan's payment still
    * reads them; nobody can change them until the plan decision is parked
    * again. A walk holds one Campaign, so the answers name only its
    * window. */
  def stale(state: OathState): Vector[OathEvent] = state match
    case OathState.Ready(ready) => ready.planAid.window.filter { window =>
      val parked = rules.planAidWindow(state)
      val answered = ready.game.current.walkerPending.exists(_.answered
        .exists(_.decisionId == CampaignIds.planDecision(window.side)))
      parked.exists(_ != window) || (parked.isEmpty && !answered)
    }.map(_ => PlanAidCleared).toVector
    case _ => Vector.empty

  /** Whether `offer` still holds `ref`, which was used, where it was
    * offered: a used plan among the plans, a used requirement among the
    * requirements. */
  private def keeps(offer: AidOffer, ready: ReadyGame, window: AidWindow,
      ally: PlayerId, ref: DecisionOptionRef): Boolean =
    if ready.planAid.window.contains(window) &&
        ready.planAid.offerOf(ally).requirements.contains(ref) then
      offer.requirements.contains(ref)
    else offer.plans.contains(ref)

  /** What is left to spend fits what the ally holds now. */
  private def fits(ready: ReadyGame, ally: PlayerId, left: AidAmounts)
      : Either[OathViolation, Unit] =
    ready.game.current.player(ally).toRight(
      unavailable(s"${ally.value} is not seated")).flatMap(held =>
      Either.cond(left.favor + left.favorBurnt <=
          SpendableFavor.of(ready, ally) &&
        left.secrets + left.secretsBurnt <= held.board.faceUpSecrets, (),
        unavailable(s"${ally.value} cannot offer more than they hold")))

  private def unavailable(reason: String) =
    OathViolation.PlanAidUnavailable(reason)

  private def playing(state: OathState): Either[OathViolation, ReadyGame] =
    state match
      case OathState.Ready(ready) if ready.game.current.result.isEmpty =>
        Right(ready)
      case OathState.Ready(_) => Left(unavailable("the game is over"))
      case _ => Left(OathViolation.GameNotStarted)
