package distantlands.model

/** Favor and secrets an Imperial ally offers towards another Imperial
  * player's battle plans (Imperial Tactics, CR p. 35; Empire phase design,
  * "1b-ii"). Favor to pay is placed onto a plan's card and favor to burn is
  * burnt, from the ally's board; secrets likewise. */
final case class AidAmounts(favor: Int = 0, secrets: Int = 0,
    favorBurnt: Int = 0, secretsBurnt: Int = 0):
  require(favor >= 0 && secrets >= 0 && favorBurnt >= 0 && secretsBurnt >= 0,
    "aid amounts are never negative")

  def +(other: AidAmounts): AidAmounts = AidAmounts(favor + other.favor,
    secrets + other.secrets, favorBurnt + other.favorBurnt,
    secretsBurnt + other.secretsBurnt)

  /** What is left after `other`, never below zero. */
  def -(other: AidAmounts): AidAmounts = AidAmounts(
    math.max(0, favor - other.favor), math.max(0, secrets - other.secrets),
    math.max(0, favorBurnt - other.favorBurnt),
    math.max(0, secretsBurnt - other.secretsBurnt))

  /** Whether every part is at least `other`'s. */
  def atLeast(other: AidAmounts): Boolean = favor >= other.favor &&
    secrets >= other.secrets && favorBurnt >= other.favorBurnt &&
    secretsBurnt >= other.secretsBurnt

  def isEmpty: Boolean = this == AidAmounts.none

  def cost: Cost = Cost(favor = favor, secret = secrets,
    favorBurnt = favorBurnt, secretBurnt = secretsBurnt)

object AidAmounts:
  val none: AidAmounts = AidAmounts()
  def of(cost: Cost): AidAmounts = AidAmounts(cost.favor, cost.secret,
    cost.favorBurnt, cost.secretBurnt)

/** One ally's offer: the plans they lend, named by the ref the plan
  * decision answers with; the requirements they meet, named by the thing
  * that meets them (the Darkest Secret or the Yew Staff, the People's
  * Favor, or the ally as a player for their pawn); and the amounts. The
  * offer is the consent: nobody approves anything. */
final case class AidOffer(plans: Vector[DecisionOptionRef] = Vector.empty,
    requirements: Vector[DecisionOptionRef] = Vector.empty,
    amounts: AidAmounts = AidAmounts.none):
  def isEmpty: Boolean = plans.isEmpty && requirements.isEmpty &&
    amounts.isEmpty

/** The plan window offers reach: the Campaign's actor, the side and the
  * Imperial player choosing that side's plans. */
final case class AidWindow(actor: PlayerId, side: CampaignPlanSide,
    user: PlayerId)

/** The standing offers of one plan window. An offer for another window
  * starts the state afresh, and `PlanAidCleared` empties it once no
  * Imperial plan decision is parked. Spent aid is not here: the plan's
  * application records it in the Campaign's pools (`PlanAid` in
  * gameplay). */
final case class PlanAidState(window: Option[AidWindow] = None,
    offers: Map[PlayerId, AidOffer] = Map.empty):
  def saved(ally: PlayerId, at: AidWindow, offer: AidOffer): PlanAidState =
    if window.contains(at) then copy(offers = offers.updated(ally, offer))
    else PlanAidState(Some(at), Map(ally -> offer))

  def offerOf(ally: PlayerId): AidOffer = offers.getOrElse(ally, AidOffer())

  def cleared: PlanAidState = PlanAidState()

/** A requirement an Imperial ally may meet for a plan's user (ruling R12):
  * holding the Darkest Secret, holding the People's Favor (checked by
  * Hearts and Minds at the end), or having a pawn at a site. */
enum PlanNeed:
  case DarkestSecret
  case PeoplesFavor
  case PawnAt(site: SiteId)
