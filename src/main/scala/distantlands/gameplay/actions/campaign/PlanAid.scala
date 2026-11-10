package distantlands.gameplay.actions.campaign

import distantlands.gameplay.{SpendableFavor, TurnOrder}
import distantlands.gameplay.actions.BannerRules
import distantlands.model._

/** What Imperial Tactics lends a plan's user (CR p. 35; Empire phase
  * design, "1b-ii"; rulings R10 to R12 and R16).
  *
  * Offers stand in `ReadyGame.planAid`. What the window's plans have used
  * of them is recorded by their applications as Campaign pool markers,
  * as a bandit's applied plan is (`CampaignPlans.appliedMarker`): the pools
  * are walker scratch, so undoing the user's pick undoes the use, and the
  * walk's end clears it. The pools are never rolled or projected.
  */
object PlanAid:
  val darkestSecret: DecisionOptionRef =
    DecisionOptionRef.Banner(Banner.DarkestSecret)
  /** "When using card powers, you may act as if you hold the Darkest
    * Secret." Its holder meets the Darkest Secret for a plan. */
  val yewStaff: DecisionOptionRef = DecisionOptionRef.Relic(RelicId("R40"))
  val peoplesFavor: DecisionOptionRef =
    DecisionOptionRef.Banner(Banner.PeoplesFavor)

  /** The window `user`'s plans on `side` are chosen in, when aid can reach
    * it: only an Imperial user is aided. */
  def window(ready: ReadyGame, actor: PlayerId, side: CampaignPlanSide,
      user: PlayerId): Option[AidWindow] =
    Option.when(Imperials.isImperial(ready, user))(
      AidWindow(actor, side, user))

  /** The other Imperial players, in turn order from the one after `user`. */
  def allies(ready: ReadyGame, user: PlayerId): Vector[PlayerId] =
    val order = TurnOrder.order(ready)
    val at = order.indexOf(user)
    (order.drop(at + 1) ++ order.take(math.max(at, 0)))
      .filter(player => player != user && Imperials.isImperial(ready, player))

  /** The saved offers that reach `window`, in ally order. */
  def offers(ready: ReadyGame, window: AidWindow)
      : Vector[(PlayerId, AidOffer)] =
    if !ready.planAid.window.contains(window) then Vector.empty
    else allies(ready, window.user).flatMap(ally =>
      ready.planAid.offers.get(ally).map(ally -> _))

  private def player(ready: ReadyGame, id: PlayerId): Option[PlayerState] =
    ready.game.current.player(id)

  /** Whether `ally` meets `requirement` now. */
  def meets(ready: ReadyGame, ally: PlayerId,
      requirement: DecisionOptionRef): Boolean = requirement match
    case DecisionOptionRef.Banner(banner) =>
      BannerRules.holder(ready.game.current, banner).contains(ally)
    case ref @ DecisionOptionRef.Relic(id) if ref == yewStaff =>
      player(ready, ally).exists(_.relics.exists(held =>
        held.id == id && held.orientation == Orientation.FaceUp))
    case DecisionOptionRef.Player(id) if id == ally =>
      player(ready, ally).exists(_.pawnSite.nonEmpty)
    case _ => false

  /** What `ally` could offer as requirements now. */
  def requirementsOf(ready: ReadyGame, ally: PlayerId)
      : Vector[DecisionOptionRef] =
    Vector(darkestSecret, yewStaff, peoplesFavor,
      DecisionOptionRef.Player(ally)).filter(meets(ready, ally, _))

  private def sideKey(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"

  private def prefix(window: AidWindow, ally: PlayerId): String =
    s"campaign.aid.${sideKey(window.side)}.${ally.value}."

  /** The pool counting what `ally`'s `part` paid in `window`: `favor`,
    * `secrets`, `favor-burnt` or `secrets-burnt`. */
  def amountMarker(window: AidWindow, ally: PlayerId, part: String): PoolKey =
    PoolKey(prefix(window, ally) + part)

  private def usedMarker(window: AidWindow, ally: PlayerId,
      ref: DecisionOptionRef): PoolKey =
    PoolKey(prefix(window, ally) + s"used.${ref.kind}.${ref.wireId}")

  private def count(ready: ReadyGame, pool: PoolKey): Int =
    ready.game.current.rollPools.get(pool).fold(0)(_.count)

  def used(ready: ReadyGame, window: AidWindow, ally: PlayerId): AidAmounts =
    AidAmounts(count(ready, amountMarker(window, ally, "favor")),
      count(ready, amountMarker(window, ally, "secrets")),
      count(ready, amountMarker(window, ally, "favor-burnt")),
      count(ready, amountMarker(window, ally, "secrets-burnt")))

  /** The plans and requirements of `ally`'s that `window`'s plans used. */
  def usedRefs(ready: ReadyGame, window: AidWindow, ally: PlayerId)
      : Vector[DecisionOptionRef] =
    val start = prefix(window, ally) + "used."
    ready.game.current.rollPools.keys.toVector.map(_.value)
      .filter(_.startsWith(start)).sorted.flatMap(key =>
        key.stripPrefix(start).split("\\.", 2) match
          case Array(kind, wireId) => DecisionOptionRef.fromWire(kind, wireId)
          case _ => None)

  def remaining(ready: ReadyGame, window: AidWindow, ally: PlayerId)
      : AidAmounts =
    if !ready.planAid.window.contains(window) then AidAmounts.none
    else ready.planAid.offerOf(ally).amounts - used(ready, window, ally)

  /** Who pays `cost`, for `user` in `window`: each ally in order pays what
    * their offer has left of each part, and the user pays the rest. Parts
    * of nothing are left out. */
  def payments(ready: ReadyGame, window: Option[AidWindow], user: PlayerId,
      cost: Cost): Vector[(PlayerId, Cost)] =
    val owed = AidAmounts.of(cost)
    val (lent, rest) = window.fold((Vector.empty[(PlayerId, AidAmounts)],
      owed))(at => offers(ready, at).foldLeft(
      (Vector.empty[(PlayerId, AidAmounts)], owed)) {
        case ((paid, left), (ally, _)) =>
          val part = take(remaining(ready, at, ally), left)
          (if part.isEmpty then paid else paid :+ (ally -> part), left - part)
      })
    (lent ++ Option.unless(rest.isEmpty)(user -> rest))
      .map((payer, part) => payer -> part.cost)

  private def take(offered: AidAmounts, owed: AidAmounts): AidAmounts =
    AidAmounts(math.min(offered.favor, owed.favor),
      math.min(offered.secrets, owed.secrets),
      math.min(offered.favorBurnt, owed.favorBurnt),
      math.min(offered.secretsBurnt, owed.secretsBurnt))

  /** The markers recording what `ally` paid of `part` in `window`. */
  def spent(window: AidWindow, ally: PlayerId, part: Cost)
      : Vector[CoreOperation] =
    Vector("favor" -> part.favor, "secrets" -> part.secret,
      "favor-burnt" -> part.favorBurnt, "secrets-burnt" -> part.secretBurnt)
      .collect { case (name, amount) if amount > 0 =>
        ModifyDicePool(amountMarker(window, ally, name), amount) }

  /** The marker recording that `window`'s plans used `ally`'s `ref`. */
  def lentUse(window: AidWindow, ally: PlayerId, ref: DecisionOptionRef)
      : CoreOperation = ModifyDicePool(usedMarker(window, ally, ref), 1)

  /** What the allies hold beyond what they have offered and not yet used:
    * favor and secrets either way, so to pay or to burn. */
  def spare(ready: ReadyGame, window: AidWindow): AidAmounts =
    allies(ready, window.user).flatMap(ally => player(ready, ally).map {
      held =>
        val left = remaining(ready, window, ally)
        AidAmounts(favor = math.max(0,
          SpendableFavor.of(ready, ally) - left.favor - left.favorBurnt),
          secrets = math.max(0,
            held.board.faceUpSecrets - left.secrets - left.secretsBurnt))
    }).foldLeft(AidAmounts.none)(_ + _)

  /** What a plan prints it costs. */
  def printed(costs: Vector[CampaignPlanCost]): AidAmounts =
    costs.foldLeft(AidAmounts.none)((sum, cost) => sum + (cost match
      case CampaignPlanCost.Favor(n) => AidAmounts(favor = n)
      case CampaignPlanCost.Secret(n) => AidAmounts(secrets = n)
      case CampaignPlanCost.FavorBurnt(n) => AidAmounts(favorBurnt = n)
      case CampaignPlanCost.SecretBurnt(n) => AidAmounts(secretsBurnt = n)
      case _ => AidAmounts.none))

  /** Whether the allies could still cover what a dry run found short. A
    * payment that fails inside the operation pipeline reports only a code,
    * so the shortfall is then read off the user's board against `printed`,
    * what the plan prints, as `PlanPrice.shortfall` reads it. */
  def couldCover(ready: ReadyGame, window: AidWindow,
      violation: OathViolation, printed: AidAmounts = AidAmounts.none)
      : Boolean = violation match
    case OathViolation.InsufficientFavor(required, available) =>
      required - available <= spare(ready, window).favor
    case OathViolation.InsufficientSecrets(required, available) =>
      required - available <= spare(ready, window).secrets
    // The printed cost is the whole of it, so the allies' reach is what
    // they hold unoffered and what their offers have left.
    case _: OathViolation.CoreOperationRejected =>
      player(ready, window.user).exists { held =>
        val favor = printed.favor + printed.favorBurnt -
          SpendableFavor.of(ready, window.user)
        val secrets = printed.secrets + printed.secretsBurnt -
          held.board.faceUpSecrets
        val reach = allies(ready, window.user).map(remaining(ready, window, _))
          .foldLeft(spare(ready, window))(_ + _)
        (favor > 0 || secrets > 0) && favor <= reach.favor + reach.favorBurnt &&
          secrets <= reach.secrets + reach.secretsBurnt
      }
    case _ => false

  /** Whether `user` meets `need` with no aid: the Darkest Secret by the
    * banner or a faceup Yew Staff (as `CardPowerBanners` sees it; actions
    * cannot import the powers), the People's Favor by the banner, a pawn
    * by the Campaign's origin. */
  def meetsAlone(ready: ReadyGame, user: PlayerId, need: PlanNeed): Boolean =
    need match
      case PlanNeed.DarkestSecret => meets(ready, user, darkestSecret) ||
        meets(ready, user, yewStaff)
      case PlanNeed.PeoplesFavor => meets(ready, user, peoplesFavor)
      case PlanNeed.PawnAt(site) =>
        CampaignSetup.originOf(ready, user).contains(site)

  /** The first ally who meets `need` and lends what meets it, with that
    * ref; `full` supposes every ally lends everything. */
  def lentNeed(ready: ReadyGame, window: AidWindow, need: PlanNeed,
      full: Boolean): Option[(PlayerId, DecisionOptionRef)] =
    val lent = offers(ready, window).toMap
    allies(ready, window.user).iterator.flatMap { ally =>
      val refs = need match
        case PlanNeed.DarkestSecret => Vector(darkestSecret, yewStaff)
        case PlanNeed.PeoplesFavor => Vector(peoplesFavor)
        case PlanNeed.PawnAt(site) => Vector(DecisionOptionRef.Player(ally))
          .filter(_ => player(ready, ally).exists(_.pawnSite.contains(site)))
      refs.find(ref => PlanAid.meets(ready, ally, ref) && (full ||
        lent.get(ally).exists(_.requirements.contains(ref)))).map(ally -> _)
    }.nextOption()
