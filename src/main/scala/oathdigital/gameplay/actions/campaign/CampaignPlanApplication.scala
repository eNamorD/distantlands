package oathdigital.gameplay.actions.campaign

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.operations.Costs
import oathdigital.model._

/** One chosen battle plan being paid for and applied. It carries the side, the
  * user and the source, in the window `CampaignPlanApplication`, so a power can
  * change what a plan costs or does by matching on it (as Silver Tongue matches
  * `PlacementTree`), and a dry run of it answers whether the user can pay.
  *
  * The children have a fixed shape, a slot each for revealing the source,
  * paying, sacrificing, then one per effect, then the amount question and what
  * its answer does, and every slot that depends on state is a `Branch`. A walk
  * that parks on a decision inside a plan resumes against the state the earlier
  * slots changed, so a slot must still select the same children then, and a
  * slot that has nothing to do selects none.
  *
  *  - A facedown adviser is revealed when it is used.
  *  - Costs are paid onto the source card, which may already hold resources. A
  *    plan paid outside its owner's turn settles at once: favor goes to the
  *    card's suit bank and secrets flip facedown (see `PayCost`). A bandit
  *    defender pays nothing, and records that it applied the plan.
  *  - A warband sacrifice asks which force pays when several could, then kills
  *    one warband. It comes before every effect, so a plan that cannot pay is
  *    refused before it does anything.
  *  - A plan with an amount asks it of its user last, so a dry run pays
  *    what is fixed and stops at the question, then pays what each unit
  *    costs onto the plan's card and applies what the answer does.
  *  - An Imperial user's Imperial allies pay first, from what they offered,
  *    in turn order after the user, and the user pays the rest (Imperial
  *    Tactics; rulings R11 and R12). What each ally paid, lent and met is
  *    recorded as Campaign pool markers (`PlanAid`), so undoing the pick
  *    undoes the use. A payment slot is never on a resume's path, so its
  *    split may read the state.
  */
final class CampaignPlanApplication(catalog: ExecutableCatalog,
    val setup: CampaignSetup, val side: CampaignPlanSide,
    val offered: OfferedPlan) extends Operation:
  def source: CampaignPlanSource = offered.offer.source
  /** The player who uses the plan; `None` for a bandit defender. */
  def user: Option[PlayerId] = CampaignPlans.userOf(setup, side)

  override val window: Option[PowerWindow] =
    Some(PowerWindow.CampaignPlanApplication)

  override val children: Vector[Operation] = Vector[Operation](
    Branch((ready, _) => reveal(ready)),
    Branch((ready, _) => pay(ready)),
    Branch((ready, _) => sacrifice(ready))) ++
    offered.offer.effects.map(effect) ++ Vector[Operation](
      Branch((ready, _) => ask(ready)),
      Branch((ready, pending) => payAmount(ready, pending)),
      Branch((_, pending) => chosenEffects(pending)),
      Branch((ready, _) => lent(ready)),
      Branch((_, _) => marker))

  /** The plan's amount question, asked of its user, with its bounds read
    * from the state after what the plan and any surcharge have paid. When
    * no amount is left to ask for, the plan is refused. A bandit defender is
    * asked nothing. */
  private def ask(ready: ReadyGame): Vector[Operation] =
    (offered.offer.amount, user) match
      case (Some(amount), Some(player)) => Vector(
        amount.query(ready, player).fold(
          violation => BuildOps((_, _) => Left(violation)),
          query => Decide(amount.decisionId, player, query)))
      case _ => Vector.empty

  /** What the answered amount costs, placed onto the plan's card. */
  private def payAmount(ready: ReadyGame, pending: PendingTree)
      : Vector[Operation] =
    val owed = for
      amount <- offered.offer.amount
      if amount.secretsEach > 0
      player <- user
      chosen <- PlanAmounts.chosen(pending, amount)
    yield player -> Cost(secret = chosen * amount.secretsEach)
    owed.toVector.flatMap { case (player, cost) =>
      CampaignPlans.cardOf(source) match
        case Some(card) => split(ready, player, cost, Some(card))
        case None =>
          Vector(refuse("the title has no card to place a plan's cost on"))
    }

  /** The window aid reaches, when the user is Imperial. */
  private def aidWindow(ready: ReadyGame): Option[AidWindow] =
    user.flatMap(PlanAid.window(ready, setup.actor, side, _))

  /** `cost`, paid onto `card` (or, for the title, from the payer's play
    * area) by the allies first and the user last, with the markers of what
    * each ally paid. */
  private def split(ready: ReadyGame, player: PlayerId, cost: Cost,
      card: Option[CardId]): Vector[Operation] =
    val window = aidWindow(ready)
    PlanAid.payments(ready, window, player, cost).flatMap { (payer, part) =>
      val paid = card match
        case Some(id) => payment(Costs.onCard(payer, id, part, catalog,
          intoOccupied = true))
        case None if part.favor + part.secret == 0 =>
          payment(PayCost(payer, Location.PlayArea(payer), part))
        case None => refuse("the title has no card to place a plan's cost on")
      val markers = window.filter(_ => payer != player).toVector
        .flatMap(PlanAid.spent(_, payer, part))
      paid +: markers
    }

  /** The markers of what this plan used of its lenders: the plan itself,
    * when an ally lent it, and each requirement an ally met for it that
    * the user does not meet alone (ruling R12), Hearts and Minds' People's
    * Favor among them, so its end reads who lent it. */
  private def lent(ready: ReadyGame): Vector[Operation] =
    (for
      window <- aidWindow(ready)
      player <- user
    yield offered.offer.lender.map(PlanAid.lentUse(window, _,
        CampaignPlans.refOf(source))).toVector ++
      offered.offer.requires.filterNot(PlanAid.meetsAlone(ready, player, _))
        .flatMap(PlanAid.lentNeed(ready, window, _, full = false))
        .map((ally, ref) => PlanAid.lentUse(window, ally, ref)))
      .getOrElse(Vector.empty)

  /** What the answered amount does. */
  private def chosenEffects(pending: PendingTree): Vector[Operation] =
    offered.offer.amount.toVector.flatMap(amount =>
      PlanAmounts.chosen(pending, amount).toVector.flatMap(chosen =>
        amount.effects(chosen).map(effect)))

  /** A bandit defender chooses nothing, so a later window learns it applied the
    * plan from this record.
    */
  private def marker: Vector[Operation] =
    if user.nonEmpty then Vector.empty
    else Vector(ModifyDicePool(CampaignPlans.appliedMarker(
      CampaignPlans.refOf(source)), 1))

  private def reveal(ready: ReadyGame): Vector[Operation] = source match
    case CampaignPlanSource.Adviser(player, id)
        if ready.game.current.player(player).exists(_.advisers.exists {
          case held: DenizenState =>
            held.id == id && held.orientation == Orientation.FaceDown
          case _ => false }) =>
      Vector(Move(Piece.Card(id), PositionedLocation(Location.PlayArea(player)),
        PositionedLocation(Location.PlayArea(player)),
        resultingOrientation = Some(Orientation.FaceUp)))
    case _ => Vector.empty

  private def pay(ready: ReadyGame): Vector[Operation] =
    val costs = offered.offer.costs
    def total(pick: PartialFunction[CampaignPlanCost, Int]): Int =
      costs.collect(pick).sum
    val cost = Cost(
      favor = total { case CampaignPlanCost.Favor(count) => count },
      secret = total { case CampaignPlanCost.Secret(count) => count },
      favorBurnt = total { case CampaignPlanCost.FavorBurnt(count) => count },
      secretBurnt = total { case CampaignPlanCost.SecretBurnt(count) => count })
    if cost == Cost.free then Vector.empty
    else user.fold(Vector.empty[Operation])(player =>
      split(ready, player, cost, CampaignPlans.cardOf(source)))

  /** The payment runs as a batch, not as the composite's own moves, so that
    * the pipeline settles it at once when its payer is not the active player.
    */
  private def payment(pay: PayCost): Operation =
    BuildOps((_, _) => Right(Vector[CoreOperation](pay)))

  private def refuse(detail: String): Operation =
    BuildOps((_, _) => Left(OathViolation.InvalidEventOrder(detail)))

  /** The forces a warband can be sacrificed from: a Raid defender's board, or
    * each target site the defender rules and holds a warband on, but not
    * a queued origin whose warbands are the attacker's (ruling R13).
    */
  private def sacrificeFrom(ready: ReadyGame, player: PlayerId)
      : Vector[Location] = setup.kind match
    case CampaignKind.Raid => ready.game.current.player(player)
      .filter(_.board.warbands > 0)
      .map(_ => Location.PlayArea(player): Location).toVector
    case CampaignKind.Conquest => CampaignBattle.defendedSites(ready,
        setup.actor, setup.targetSites).filter(site =>
      CampaignSetup.defenderAt(ready, site).contains(
        CampaignDefender.Player(player)) &&
        ready.game.current.map.sites.get(site).exists(_.forces match {
          case SiteForces.Occupied(_, count) => count > 0
          case SiteForces.Empty => false
        })).map(site => Location.Site(site): Location)

  private def sacrifice(ready: ReadyGame): Vector[Operation] =
    if !offered.offer.costs.contains(CampaignPlanCost.SacrificeWarband) then
      Vector.empty
    else user match
      case Some(player) if side == CampaignPlanSide.Defender =>
        sacrificeFrom(ready, player) match
          case Vector() => Vector(refuse(
            "the plan needs a warband in the defender's force to sacrifice"))
          case Vector(only) => Vector(kill(ready, player, only))
          case several => Vector(
            Decide(CampaignIds.planSacrifice, player, DecisionQuery.ChooseOne(
              several.collect { case Location.Site(site) =>
                DecisionOption.Site(DecisionOptionRef.Site(site)): DecisionOption },
              heading = Some("Choose the site to sacrifice a warband from"),
              confirmVerb = Some("Sacrifice from"))),
            BuildOps((state, pending) => PlanAnswers.site(pending)
              .toRight(OathViolation.InvalidEventOrder(
                "no site is chosen for the plan's sacrifice"))
              .flatMap(site => killOps(state, player, Location.Site(site)))))
      case _ => Vector(refuse(
        "only a player defender can pay a warband sacrifice"))

  private def kill(ready: ReadyGame, player: PlayerId, from: Location)
      : Operation = BuildOps((state, _) => killOps(state, player, from))

  private def killOps(ready: ReadyGame, player: PlayerId, from: Location)
      : Either[OathViolation, Vector[CoreOperation]] =
    val kind = from match
      case Location.Site(site) => ready.game.current.map.sites.get(site)
        .map(_.forces).collect { case SiteForces.Occupied(force, _) => force }
      case _ => ready.game.current.player(player)
        .flatMap(PlayerForceKind.of(ready, _))
    kind.toRight(OathViolation.InvalidEventOrder(
      "the plan's sacrifice has no warband to kill")).map(force =>
      Vector[CoreOperation](Sacrifice(player, Piece.Warbands(force, 1),
        PositionedLocation(from))))

  private def effect(effect: CampaignPlanEffect): Operation = effect match
    case CampaignPlanEffect.AddAttackDice(count) =>
      ModifyDicePool(CampaignIds.attackPool, count)
    // Empty Attack Pool (rules, "Use battle plans"): each die the attack
    // pool lacks becomes a defense die added to the defense pool.
    case CampaignPlanEffect.RemoveAttackDice(count) =>
      BuildOps((state, _) =>
        val held = state.game.current.rollPools.get(CampaignIds.attackPool)
          .fold(0)(_.count)
        val removed = count min held
        Right(Vector(ModifyDicePool(CampaignIds.attackPool, -removed),
          ModifyDicePool(CampaignIds.defensePool, count - removed))
          .filter(_.delta != 0)))
    case CampaignPlanEffect.AddDefenseDice(count) =>
      ModifyDicePool(CampaignIds.defensePool, count)
    case CampaignPlanEffect.Run(operations) => Sequence(operations)

/** Reads of the choices a plan's own decisions recorded. */
private[campaign] object PlanAnswers:
  def site(pending: PendingTree): Option[SiteId] = pending.answered.reverse
    .collectFirst:
      case Answered(CampaignIds.planSacrifice,
          DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Site(site)), _) => site
