package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.{BanditCrownRule, EffectiveSuit}
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.campaign.{CampaignAnswers, CampaignIds, CampaignPlans, CampaignSetup, PlanAid}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.banner.CardPowerBanners
import oathdigital.gameplay.powers.RuledCards
import oathdigital.model._

/** What a battle plan reads when it is asked whether it is usable now: the
  * state, the Campaign's setup and the side the window is for.
  *
  * A plan is used only by the ruler of its source, so a plan asks this for where
  * a card stands: an adviser of the plan's user, a relic held faceup by the
  * user, or a card at a site the user rules. The Bandit Crown's holder also
  * uses the cards at bandit sites. A bandit defender is the ruler of
  * the sites Bandits rule and holds nothing else.
  *
  * An Imperial user may also use what an Imperial ally lends (Imperial
  * Tactics, CR p. 35; Empire phase design, "1b-ii"): an ally's faceup
  * adviser or relic, a faceup card or edifice at a site the ally rules, the
  * title's defense when the ally holds the title, and the Darkest Secret or
  * a pawn the ally offers. The user's own sources are always found first.
  * `full` supposes every ally lends everything they could, which is how the
  * window finds the plans that wait on an offer.
  */
final case class PlanContext(catalog: ExecutableCatalog, ready: ReadyGame,
    setup: CampaignSetup, side: CampaignPlanSide, full: Boolean = false):
  /** The player who would use the plan; `None` for a bandit defender. */
  def user: Option[PlayerId] = CampaignPlans.userOf(setup, side)

  /** The plan's user as a side of the Campaign: the player, or bandits for a
    * bandit defender. */
  def ruler: CampaignDefender = user.fold[CampaignDefender](
    CampaignDefender.Bandits)(CampaignDefender.Player(_))

  /** Whether the plan's user rules `site`. */
  def rules(site: SiteId): Boolean =
    CampaignSetup.defenderAt(ready, site).contains(ruler)

  private def held: Option[PlayerState] =
    user.flatMap(ready.game.current.player)

  /** The sites whose cards the user's plans come from: the sites they
    * rule, and the bandits' sites when they hold the Bandit Crown (catalog
    * batch 11, ruling 3). A bandit defender keeps its own sites, so a
    * Crown plan may be used by both sides. */
  private def sitesRuled: Vector[(SiteId, SiteState)] =
    val crowned = user.exists(BanditCrownRule.holder(ready).contains)
    sitesOf(ruler) ++
      (if crowned then sitesOf(CampaignDefender.Bandits) else Vector.empty)

  private def sitesOf(who: CampaignDefender): Vector[(SiteId, SiteState)] =
    ready.game.current.map.inPlay
      .filter(CampaignSetup.defenderAt(ready, _).contains(who))
      .flatMap(site => ready.game.current.map.sites.get(site).map(site -> _))

  /** The window aid reaches, when the user is Imperial. */
  def aidWindow: Option[AidWindow] =
    user.flatMap(PlanAid.window(ready, setup.actor, side, _))

  /** The same context as if every ally offered every plan and requirement
    * they could: what the window shows as awaiting an offer. */
  def fullAid: PlanContext = copy(full = true)

  private def lenders: Vector[PlayerId] = aidWindow.fold(
    Vector.empty[PlayerId])(window => PlanAid.allies(ready, window.user))

  /** Whether `ally` lends the plan `ref`: by their saved offer, or, in
    * the full view, as any ally may. Requirements are lent through
    * `lentRequirement`. */
  private def lends(ally: PlayerId, ref: DecisionOptionRef): Boolean =
    aidWindow.exists(window =>
      if full then lenders.contains(ally)
      else PlanAid.offers(ready, window).exists((lender, offer) =>
        lender == ally && offer.plans.contains(ref)))

  /** The secrets to pay the allies offer and have not used, or, in the
    * full view, hold. */
  def aidSecrets: Int = aidWindow.fold(0)(window =>
    if full then PlanAid.spare(ready, window).secrets +
      lenders.map(PlanAid.remaining(ready, window, _).secrets).sum
    else lenders.map(PlanAid.remaining(ready, window, _).secrets).sum)

  /** Whether `holder`, an ally, lends the title's defense. */
  def lendsTitle(holder: PlayerId): Boolean = lenders.contains(holder) &&
    lends(holder, DecisionOptionRef.Button("title"))

  /** The ally whose source `source` is, when it is not the user's and aid
    * reaches the window. */
  def lentBy(source: CampaignPlanSource): Option[PlayerId] =
    val owner = aidWindow.flatMap(_ => source match
      case CampaignPlanSource.Adviser(player, _) => Some(player)
      case CampaignPlanSource.Relic(player, _) => Some(player)
      case CampaignPlanSource.Title(player) => Some(player)
      case CampaignPlanSource.SiteCard(site, _) => rulerOf(site)
      case CampaignPlanSource.SiteEdifice(site, _) => rulerOf(site))
    owner.filter(owner => !user.contains(owner))

  private def rulerOf(site: SiteId): Option[PlayerId] =
    CampaignSetup.defenderAt(ready, site).flatMap(_.player)

  /** An ally's requirement that meets `need`, with the ref that meets it:
    * what the plan's application records as used. */
  def lentRequirement(need: PlanNeed): Option[(PlayerId, DecisionOptionRef)] =
    aidWindow.flatMap(PlanAid.lentNeed(ready, _, need, full))

  private def faceUpAt(state: SiteState, id: DenizenId): Boolean =
    state.denizens.exists {
      case card: DenizenState =>
        card.id == id && card.orientation == Orientation.FaceUp
      case _ => false
    }

  /** A denizen the user holds as an adviser, in either orientation, or one that
    * is faceup at a site the user rules. Else an ally's that the ally lends:
    * a faceup adviser, or a faceup card at a site the ally rules. A
    * facedown card is never lent.
    */
  def denizen(id: DenizenId): Option[CampaignPlanSource] =
    val ref = DecisionOptionRef.Denizen(id)
    held.flatMap(player => player.advisers.collectFirst {
      case card: DenizenState if card.id == id =>
        CampaignPlanSource.Adviser(player.player, id): CampaignPlanSource
    }).orElse(sitesRuled.collectFirst {
      case (site, state) if faceUpAt(state, id) =>
        CampaignPlanSource.SiteCard(site, id): CampaignPlanSource
    }).orElse(lenders.filter(lends(_, ref)).iterator.flatMap { ally =>
      ready.game.current.player(ally).flatMap(_.advisers.collectFirst {
        case DenizenState(`id`, Orientation.FaceUp, _) =>
          CampaignPlanSource.Adviser(ally, id): CampaignPlanSource
      }).orElse(sitesOf(CampaignDefender.Player(ally)).collectFirst {
        case (site, state) if faceUpAt(state, id) =>
          CampaignPlanSource.SiteCard(site, id): CampaignPlanSource
      })
    }.nextOption())

  /** A relic the user holds faceup, else one an ally holds faceup and
    * lends. A relic at a site is facedown, and a plan cannot use it.
    */
  def relic(id: RelicId): Option[CampaignPlanSource] =
    def faceUp(player: PlayerState) = player.relics.collectFirst {
      case card if card.id == id && card.orientation == Orientation.FaceUp =>
        CampaignPlanSource.Relic(player.player, id): CampaignPlanSource
    }
    held.flatMap(faceUp).orElse(lenders
      .filter(lends(_, DecisionOptionRef.Relic(id)))
      .iterator.flatMap(ally => ready.game.current.player(ally)
        .flatMap(faceUp))
      .nextOption())

  /** An edifice on the given face at a site the user rules, else at a site
    * an ally rules who lends it. */
  def edifice(id: EdificeId, face: EdificeSide)
      : Option[CampaignPlanSource.SiteEdifice] =
    def at(sites: Vector[(SiteId, SiteState)]) = sites.collectFirst:
      case (site, state) if state.denizens.exists {
        case card: EdificeState => card.id == id && card.side == face
        case _ => false
      } => CampaignPlanSource.SiteEdifice(site, id)
    at(sitesRuled).orElse(lenders
      .filter(lends(_, DecisionOptionRef.Edifice(id)))
      .iterator.flatMap(ally => at(sitesOf(CampaignDefender.Player(ally))))
      .nextOption())

  /** Whether the plan's user has their pawn, or campaigns from a queued
    * origin, at `site`; or an ally lends their pawn there (ruling R16). */
  def pawnAt(site: SiteId): Boolean = user.exists(player =>
    CampaignSetup.originOf(ready, player).contains(site)) ||
    lentRequirement(PlanNeed.PawnAt(site)).nonEmpty

  /** Whether `site` is a target of the Campaign. */
  def targets(site: SiteId): Boolean = setup.targetSites.contains(site)

  /** Whether any target of a Conquest is in `region`. A Raid targets no site. */
  def targetsIn(region: Region): Boolean = setup.targetSites.exists(site =>
    ready.game.current.map.regionOf(site).contains(region))

  /** The other side: for an attacker's plan the defender, a player or
    * bandits; for a defender's plan the attacker. */
  def enemy: CampaignDefender = side match
    case CampaignPlanSide.Attacker => setup.defender
    case CampaignPlanSide.Defender => CampaignDefender.Player(setup.actor)

  private def enemyPlayer: Option[PlayerState] = enemy match
    case CampaignDefender.Player(player) =>
      ready.game.current.player(player)
    case CampaignDefender.Bandits => None

  /** Whether the enemy has a faceup adviser of `suit`, as the rules see it
    * (`EffectiveSuit`). Only a faceup card has a suit, and bandits hold no
    * advisers. */
  def enemyHasAdviser(suit: Suit): Boolean =
    enemyPlayer.exists(_.advisers.exists {
      case DenizenState(held, Orientation.FaceUp, _) =>
        EffectiveSuit.of(catalog, ready, held).contains(suit)
      case _ => false
    })

  /** Whether the enemy holds `banner`. Bandits hold none. */
  def enemyHolds(banner: Banner): Boolean = enemyPlayer.exists(held =>
    BannerRules.holder(ready.game.current, banner).contains(held.player))

  /** Whether the plan's user holds the Darkest Secret, as the card powers
    * they use see it (`CardPowerBanners`), or an ally lends it. A bandit
    * defender holds none. */
  def holdsDarkestSecret: Boolean =
    user.exists(CardPowerBanners.holdsDarkestSecret(ready, _)) ||
      lentRequirement(PlanNeed.DarkestSecret).nonEmpty

  /** The cards of `suit` that `who` rules (`RuledCards`): the plan's user
    * (`ruler`), the enemy or the Campaign's defender. */
  def cardsRuled(who: CampaignDefender,
      suit: Suit): Vector[CardId] =
    val asRuler: SiteRuler = who match
      case CampaignDefender.Player(player) => SiteRuler.Player(player)
      case CampaignDefender.Bandits => SiteRuler.Bandits
    RuledCards.of(catalog, ready, asRuler, suit)

object PlanContext:
  /** The context of the plan window `ctx` is gathered for; `None` at any other
    * window, or before the Campaign's force is known.
    */
  def of(ctx: PowerCtx): Option[PlanContext] = (ctx.window match {
    case PowerWindow.CampaignAttackerBattlePlans =>
      Some(CampaignPlanSide.Attacker)
    case PowerWindow.CampaignDefenderBattlePlans =>
      Some(CampaignPlanSide.Defender)
    case _ => None
  }).flatMap(side => CampaignSetup.setup(ctx.state, ctx.actor,
    PendingTree(ctx.nodePath, ctx.answered))
    .map(PlanContext(ctx.catalog, ctx.state, _, side)))

/** A plan that was used, as a later window sees it. `result` is the Campaign's
  * recorded result once the outcome is known (the windows from the losses on),
  * and is what a step after the losses must read, because the losses change the
  * board.
  */
final case class PlanUse(catalog: ExecutableCatalog, side: CampaignPlanSide,
    actor: PlayerId, result: Option[CampaignResult], ready: ReadyGame,
    defender: Option[CampaignDefender] = None):
  /** The player who used the plan; `None` for a bandit defender. A defender
    * is read from the recorded result once the outcome is known, and from
    * the Campaign's setup before it. */
  def user: Option[PlayerId] = side match
    case CampaignPlanSide.Attacker => Some(actor)
    case CampaignPlanSide.Defender =>
      result.map(_.defender).orElse(defender).flatMap(_.player)

  /** Whether the plan's user won. `None` before the outcome is known. */
  def won: Option[Boolean] = result.map(_.attackerWins == (
    side == CampaignPlanSide.Attacker))

object PlanUse:
  /** The sides of `sides` the plan named by `ref` was used on in this
    * Campaign, attacker first: chosen by a player, or applied by a bandit
    * defender, which the application recorded
    * (`CampaignPlans.appliedMarker`). Both sides use one plan when the
    * Bandit Crown's holder attacks the bandits with a bandit card's plan
    * (catalog batch 11, ruling 3). */
  def sidesUsed(ready: ReadyGame, pending: PendingTree,
      ref: DecisionOptionRef, sides: Set[CampaignPlanSide])
      : Vector[CampaignPlanSide] =
    val picked = CampaignPlanSide.values.toVector.filter(side => sides(side) &&
      CampaignAnswers.picks(pending, CampaignIds.planDecision(side))
        .contains(ref))
    val banditApplied = Option.when(sides(CampaignPlanSide.Defender) &&
      ready.game.current.rollPools.contains(CampaignPlans.appliedMarker(ref)))(
      CampaignPlanSide.Defender)
    (picked ++ banditApplied).distinct

  /** Each use of the plan named by `ref` on one of `sides` in this Campaign
    * ([[sidesUsed]]), as a later window reads it. `afterOutcome` says
    * whether `lastCampaignResult` is this Campaign's already.
    */
  def chosen(catalog: ExecutableCatalog, ready: ReadyGame,
      pending: PendingTree, actor: PlayerId, ref: DecisionOptionRef,
      sides: Set[CampaignPlanSide], afterOutcome: Boolean): Vector[PlanUse] =
    sidesUsed(ready, pending, ref, sides).map(side => PlanUse(catalog, side,
      actor, Option.when(afterOutcome)(ready.game.current.lastCampaignResult)
        .flatten.filter(_.attacker == actor), ready,
      CampaignSetup.setup(ready, actor, pending).map(_.defender)))

  /** The plans used on `side` in this Campaign: those a player picked, and
    * for the defender those bandits applied, which the application recorded
    * (`CampaignPlans.appliedMarker`). */
  def plans(ready: ReadyGame, pending: PendingTree, side: CampaignPlanSide)
      : Vector[DecisionOptionRef] =
    val applied =
      if side != CampaignPlanSide.Defender then Vector.empty
      else ready.game.current.rollPools.keys.toVector
        .flatMap(CampaignPlans.markedRef)
    CampaignAnswers.picks(pending, CampaignIds.planDecision(side)) ++ applied
