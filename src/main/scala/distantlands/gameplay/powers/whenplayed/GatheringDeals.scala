package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.negotiation.{DealRules, DealWalk,
  NegotiationDeal, NegotiationDealTree, NegotiationProcedure,
  NegotiationReach, NegotiationSites}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.AdviserLimit
import distantlands.model._
import distantlands.model.DecisionAnswer.{OpenDeal, PassGathering,
  UnpassGathering}

/** The Gathering's deals (catalog batch 12, rulings 3 to 5 and 32), after
  * the pawns:
  *
  * {{{
  * Repeat(two or more at the site, one of them not passed) {
  *   Branch -> Decide(gathering.open, co-owned by the players at the site)
  *   Branch(latest open answer is OpenDeal ->
  *     NegotiationDealTree under gathering.<n>, n counting the deals)
  * }
  * }}}
  *
  * The open question keeps one id, so the answer it collects never renames
  * the decision the walk parked at; each deal's answers live under its own
  * prefix, which the deal's `Branch` computes after the answer. Passes are
  * folded from every answer under `gathering.`: a Pass adds its player, and
  * an Unpass or an opened deal removes them (ruling 32).
  *
  * Nothing is stored. The reach and each deal's rules are read live, under
  * the walk's power-use restrictions (`PowerCtx.useRestrictions`): no state
  * changes while the question or a deal is parked, and once a deal settles
  * its walk reads no rule but `privateDeal`, which a Gathering never sets.
  */
object GatheringDeals:
  val Prefix: String = "gathering"
  val openDecisionId: String = s"$Prefix.open"

  /** The prefix of the `n`-th deal, from 1. */
  def dealPrefix(n: Int): String = s"$Prefix.$n"

  /** Whether `decisionId` is a Gathering's: its open question or one of its
    * deals' decisions. */
  def isGathering(decisionId: String): Boolean =
    decisionId.startsWith(s"$Prefix.")

  /** Who has passed, folded from every Gathering answer in order. */
  def passed(answered: Vector[Answered]): Set[PlayerId] =
    answered.filter(answer => isGathering(answer.decisionId))
      .foldLeft(Set.empty[PlayerId]):
        case (done, Answered(_, PassGathering, by)) => done + by
        case (done, Answered(_, UnpassGathering, by)) => done - by
        case (done, Answered(_, OpenDeal(_), by)) => done - by
        case (done, _) => done

  def loop(ctx: PowerCtx, site: SiteId, order: Vector[PlayerId]): Operation =
    Repeat((live, pending) => going(live, site, order, pending.answered),
      Sequence(Vector[Operation](
        Branch((live, pending) =>
          Vector(ask(ctx, live, site, order, pending.answered))),
        Branch((live, pending) =>
          dealt(ctx, live, site, order, pending.answered)))))

  /** Two or more players at the site, and one of them has not passed. */
  private def going(ready: ReadyGame, site: SiteId, order: Vector[PlayerId],
      answered: Vector[Answered]): Boolean =
    val here = TheGathering.present(ready, site, order)
    val out = passed(answered)
    here.size >= 2 && !here.forall(out)

  /** The open question: the actor owns it when at the site, else the first
    * player there; each player's reach is the Negotiate action's, from the
    * site, under the walk's restrictions (ruling 5). */
  private def ask(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], answered: Vector[Answered]): Decide =
    val here = TheGathering.present(ready, site, order)
    val out = passed(answered)
    val owner = if here.contains(ctx.actor) then ctx.actor else here.head
    val name = ctx.catalog.site(site).fold(site.value)(_.name)
    Decide(openDecisionId, owner, DecisionQuery.Gathering(here,
      here.map(player => player -> NegotiationDeal.eligible(ctx.catalog,
        ready, player, ctx.useRestrictions)).toMap,
      here.filter(out), heading = Some(s"The Gathering at $name")),
      coOwners = here.filter(_ != owner))

  /** The deal the latest open answer opened, if it opened one. */
  private def dealt(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], answered: Vector[Answered])
      : Vector[Operation] =
    answered.reverse.find(_.decisionId == openDecisionId) match
      case Some(Answered(_, OpenDeal(named), opener)) =>
        val n = answered.count {
          case Answered(`openDecisionId`, OpenDeal(_), _) => true
          case _ => false
        }
        NegotiationDealTree(ctx.catalog, AdviserLimit, walk(ctx, ready, site,
          order, dealPrefix(n), opener, named))
      case _ => Vector.empty

  private def walk(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], prefix: String, opener: PlayerId,
      named: Vector[PlayerId]): DealWalk =
    DealWalk(prefix, opener,
      participants = (now, _) => opener +: now.game.current.players
        .map(_.player).filter(player => player != opener &&
          named.contains(player)),
      rules = participants => dealRules(ctx, ready, participants),
      opened = _ =>
        if opener == ctx.actor then
          NegotiationProcedure.negotiated.sourceless(NoteArg.Players(named))
        else NegotiationProcedure.negotiatedBy.sourceless(
          NoteArg.Player(opener), NoteArg.Players(named)),
      bystanders = (now, _) => TheGathering.present(now, site, order)
        .filter(player => player != opener && !named.contains(player)),
      passed = pending => passed(pending.answered).toVector)

  /** A Gathering deal's rules (ruling 5): every printed kind between any
    * pair, sites under Deed Writer, and binding promises when a
    * participant may use The Tribunal, both judged under the walk's
    * power-use restrictions. */
  private def dealRules(ctx: PowerCtx, ready: ReadyGame,
      participants: Vector[PlayerId]): DealRules =
    DealRules(advisersOpen = true, secretsOpen = true,
      bindingPromises = NegotiationReach.tribunalUsers(ctx.catalog, ready,
        participants, ctx.useRestrictions).nonEmpty,
      siteTraders = NegotiationSites.deedWriterUsers(ctx.catalog, ready,
        participants, ctx.useRestrictions))
