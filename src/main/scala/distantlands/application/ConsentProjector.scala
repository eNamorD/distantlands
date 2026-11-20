package distantlands.application

import distantlands.gameplay.actions.{BannerRules, ImperialLaws}
import distantlands.gameplay.powers.{ConsentPowers, PayOrAsk}
import distantlands.gameplay.powers.economy.Curfew
import distantlands.gameplay.powers.search.ForcedLabor
import distantlands.gameplay.powers.travel.{GraspingVines, TollRoads}
import distantlands.model._
import distantlands.protocol.projection._

/** A waivable power's name as the panels write it: its source's name, the
  * Narrow Pass for the Pass. */
private[application] final class ConsentNames(
    presentation: GamePresentationProjector, powers: ConsentPowers):
  def power(id: PowerId): String = powers.source(id) match
    case Some(PowerSourceRef.Site(site)) => presentation.siteLabel(site)
    // A waivable edifice power is named for the face that prints it: the
    // Hall of Ministers on its intact face, Bandit Patrols on its ruined.
    case Some(PowerSourceRef.Card(edifice: EdificeId)) =>
      presentation.edificePowerLabel(edifice, id)
    case Some(PowerSourceRef.Card(card)) =>
      presentation.cardDetails(card, None, hidden = false).name
    case Some(PowerSourceRef.Banner(banner)) => BannerRules.displayName(banner)
    case None => ImperialLaws.names.getOrElse(id,
      presentation.safeLabel(id.value))

/** The consent parts of a viewer's projection (consent design,
  * "Projection"): the requester's waiting notice in place of the decision,
  * each asked granter's prompt, and the viewer's own preferences. Nothing
  * here says whether an answer was automatic, and no other player's
  * preferences leave the server. */
private[application] final class ConsentProjector(
    presentation: GamePresentationProjector,
    decisions: WalkerDecisionProjector, rows: DecisionOptionRows,
    powers: ConsentPowers):
  private val names = new ConsentNames(presentation, powers)

  def applied(context: ScopedProjectionContext, projection: GameProjection)
      : GameProjection =
    val ready = context.ready
    val request = ready.consent.pending
    val requesting = request.filter(waiting =>
      context.viewer.contains(waiting.requester))
    val prompt = for
      waiting <- request
      viewer <- context.viewer
      ask <- waiting.open.find(_.granter == viewer)
    yield
      // A payer is asked to travel to the site their key names (catalog
      // batch 8, slice 15).
      val paying = ask.powers.forall(powers.paying)
      val site = Option.when(paying)(waiting.keysOf(ask).collectFirst {
        case ConsentKey(_, _, _, DecisionOptionRef.Site(site)) => site })
        .flatten
      ConsentPromptProjection(waiting.requester.value,
        ask.powers.map(names.power), site.fold(target(context, waiting))(
          presentation.siteLabel), paying, waiting.use.map(used),
        // What the granter may adjust later from the power's overlay
        // (implementation ruling R21).
        adjust = if !ask.powers.forall(power =>
            powers.granter(ready, power).contains(viewer)) then "none"
          else if waiting.use.nonEmpty then "preferences" else "answer")
    projection.copy(
      walkerDecision =
        if requesting.nonEmpty then None else projection.walkerDecision,
      consentWaiting = requesting.map { waiting =>
        val open = waiting.open
        // "A's or B's" while only alternatives wait (catalog batch 8,
        // slice 15).
        ConsentWaitingProjection(open.map(_.granter.value),
          either = open.size > 1 && open.forall(_.alternative),
          use = waiting.use.map(used))
      },
      consentPrompt = prompt,
      consentPreferences = context.viewer.toVector.flatMap(
        preferences(context, _)))

  private def preferences(context: ScopedProjectionContext, viewer: PlayerId)
      : Vector[ConsentPreferencesProjection] =
    val ready = context.ready
    powers.ruledBy(ready, viewer).map(power => ConsentPreferencesProjection(
      power.value, names.power(power),
      ready.game.current.players.map(_.player).filter(_ != viewer).map(
        other => ConsentPreferenceRowProjection(other.value,
          ready.consent.preference(viewer, power, other)
            .fold(ConsentSetting.CaseByCase.key)(_.key))),
      powers.source(power).collect { case PowerSourceRef.Card(card) =>
        card.value },
      powers.source(power).collect { case PowerSourceRef.Banner(banner) =>
        banner.key },
      requests(context, viewer, power)))

  /** The turn's requests about `power` that `viewer` answered, newest
    * first (reconsider design, "Projection"). Only the viewer's own
    * answers leave the server, and an entry whose option the viewer may
    * not identify is left out, so its id never does (implementation
    * ruling R19). */
  private def requests(context: ScopedProjectionContext, viewer: PlayerId,
      power: PowerId): Vector[ConsentRequestRowProjection] =
    val ready = context.ready
    // An answer given to a request that still waits is not in the history
    // yet; its giver sees it first, as waiting (implementation ruling
    // R26).
    def mine(entry: ConsentRequest): Option[ConsentAsk] =
      entry.asks.find(ask => ask.granter == viewer && ask.answer.nonEmpty)
    val waiting = ready.consent.pending.filter(mine(_).nonEmpty)
    // The client keys an entry by requester, decision and option, so an
    // earlier answer about the option the waiting one names is left out.
    val settled = ready.consent.historyAt(ConsentTurn.of(ready)).reverse
      .filterNot(entry => waiting.exists(open =>
        open.requester == entry.requester &&
          open.decisionId == entry.decisionId && open.target == entry.target))
    for
      entry <- waiting.toVector ++ settled
      ask <- mine(entry).filter(_.powers.contains(power))
      label <- optionLabel(context, entry)
    yield ConsentRequestRowProjection(entry.requester.value,
      entry.decisionId, entry.target.kind, entry.target.wireId, label,
      if ask.answer.contains(true) then "allowed" else "refused",
      ask.powers.filterNot(_ == power).map(names.power),
      entry.use.map(used),
      ConsentChange.of(ready, powers, viewer, entry, ask).key)

  /** The option asked about, named as its decision row names it
    * ([[DecisionOptionRows]]). A button has no name of its own, so its
    * label is read from the parked decision the request is about, which is
    * the one thing this still needs the walker projector for. */
  private def target(context: ScopedProjectionContext,
      request: ConsentRequest): String =
    named(context, request).getOrElse(request.target match
      // A card this viewer may not identify: its wire id is its catalog
      // id, so it is not shown (implementation ruling R25).
      case _: DecisionOptionRef.Denizen | _: DecisionOptionRef.Relic |
          _: DecisionOptionRef.Vision | _: DecisionOptionRef.Legacy =>
        "a hidden card"
      case other => other.wireId)

  /** The option of a settled request, for the requests list, worded from
    * the state now (implementation ruling R23). Its decision may be parked
    * no longer, so a button falls back to the log's label for its key.
    * `None` for any other option this viewer cannot be shown by name. */
  private def optionLabel(context: ScopedProjectionContext,
      request: ConsentRequest): Option[String] =
    named(context, request).orElse(request.target match
      // A pay-or-ask step's free option keeps the label its step gave it.
      case button: DecisionOptionRef.Button if PayOrAsk.isFree(button) =>
        ConsentProjector.payOrAskSteps
          .find(_.decisionId == request.decisionId).map(_.freeLabel)
          .orElse(Some(presentation.safeLabel(button.key)))
      case DecisionOptionRef.Button(key) => Some(presentation.safeLabel(key))
      case _ => None)

  private def named(context: ScopedProjectionContext,
      request: ConsentRequest): Option[String] =
    val ref = request.target
    val option = ref match
      case button: DecisionOptionRef.Button => decisions
        .openDecisions(context.ready)
        .filter(_.decisionId == request.decisionId)
        .flatMap(offered).find(_.ref == button)
      case _ => DecisionOption.forRef(ref)
    option.flatMap(rows.project(context.ready, context.viewer,
        context.ready.game.cardIndex.toOption, _)).map(_.label)

  /** The name of the card whose automatic power a request is about, as
    * the panels write it (catalog batch 8, item 10b). */
  private def used(use: ConsentUse): String =
    presentation.cardDetails(use.card, None, hidden = false).name

  /** The options of a choose-one or a choose-many, as offered. */
  private def offered(decide: Decide): Vector[DecisionOption] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options
      case many: DecisionQuery.ChooseMany => many.options
      case _ => Vector.empty

private[application] object ConsentProjector:
  /** Every pay-or-ask step: each offers a free option its ruler may be
    * asked about. */
  val payOrAskSteps: Vector[PayOrAsk] = Vector(TollRoads.step,
    GraspingVines.step, Curfew.step, ForcedLabor.step)
  val withdrawNote = "Withdraw your consent request"
