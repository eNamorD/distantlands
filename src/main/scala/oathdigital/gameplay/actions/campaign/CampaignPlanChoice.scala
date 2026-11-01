package oathdigital.gameplay.actions.campaign

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.OfferHost
import oathdigital.model._

/** One side's battle-plan window: the node the powers' `Offer`s are folded into.
  * A pass of it, inside a `Repeat`, asks the user to choose one plan and then
  * pays for and applies it, so the next pass offers what is still usable after
  * the payment.
  *
  * What a pass walks:
  *
  *  - A player asks a decision listing the plans still unchosen: those the
  *    user can pay for (a dry run of each says so, with every power's changes
  *    to its cost, and prices the option from what it recorded), the rest
  *    shown unavailable with why, then "Finish", worded with how many plans
  *    are chosen (`finishWith`). A plan its user can pay for
  *    shows its offer's warning. Nothing usable means nothing
  *    is asked, the pass does nothing, and the `Repeat` ends.
  *  - An Imperial user's window is asked, even when they can use nothing
  *    alone, when some plan awaits an ally's offer or could be paid with
  *    the allies' help (Imperial Tactics).
  *  - The chosen plan is applied as a [[CampaignPlanApplication]].
  *  - A bandit defender applies every cost-free plan it is offered and can
  *    pay for, without asking.
  *
  * A walk resuming inside a pass keeps the two slots of the pass whatever is
  * offered, because the plan it is paying for has been chosen and is no longer
  * listed.
  */
final class CampaignPlanChoice(catalog: ExecutableCatalog, val actor: PlayerId,
    val side: CampaignPlanSide) extends OfferHost:
  override val window: Option[PowerWindow] = Some(side match {
    case CampaignPlanSide.Attacker => PowerWindow.CampaignAttackerBattlePlans
    case CampaignPlanSide.Defender => PowerWindow.CampaignDefenderBattlePlans
  })
  override val children: Vector[Operation] = Vector.empty

  def expand(offers: Vector[OfferedPlan], pass: OfferHost.Pass)
      : Vector[Operation] =
    val pending = PendingTree(Vector.empty, pass.answered)
    CampaignSetup.setup(pass.state, actor, pending).fold(
      Vector.empty[Operation])(setup => CampaignPlans.userOf(setup, side) match {
      case None => banditPlans(setup, CampaignPlans.sorted(offers), pass)
      case Some(user) => userPass(setup, user, CampaignPlans.sorted(offers),
        pending, pass)
    })

  private def applicationOf(setup: CampaignSetup, offered: OfferedPlan)
      : CampaignPlanApplication =
    new CampaignPlanApplication(catalog, setup, side, offered)

  /** The option, with the offer's warning when it has one. */
  private def warned(offered: OfferedPlan, option: DecisionOption)
      : DecisionOption =
    offered.offer.warning.fold(option)(DecisionOption.Warned(option, _))

  /** Bandits pay nothing, so a plan that costs is never applied, and neither is
    * one a power makes unpayable for them (a surcharge in secrets, which bandits
    * do not hold). A plan used alone (Code of Honor) is applied with no other
    * when it would be applied.
    */
  private def banditPlans(setup: CampaignSetup, offers: Vector[OfferedPlan],
      pass: OfferHost.Pass): Vector[Operation] =
    val applied = offers
      .filter(offered => offered.offer.costs.isEmpty &&
        offered.offer.withheld.isEmpty)
      .map(applicationOf(setup, _))
      .filter(application => pass.applies(application).isRight)
    val chosen = applied.find(_.offered.offer.alone).fold(applied)(Vector(_))
    chosen.flatMap(application => Vector[Operation](
      CampaignNotes.leaf(CampaignNotes.banditActivationNote(
        application.offered.offer.source)), application))

  private def userPass(setup: CampaignSetup, user: PlayerId,
      offers: Vector[OfferedPlan], pending: PendingTree, pass: OfferHost.Pass)
      : Vector[Operation] =
    val decisionId = CampaignIds.planDecision(side)
    val chosen = CampaignAnswers.picks(pending, decisionId)
    def unavailable(offered: OfferedPlan, reason: OptionReason)
        : DecisionOption =
      DecisionOption.Unavailable(CampaignPlans.optionOf(offered), reason)
    // An Imperial ally may lend one plan to both sides; the defender cannot
    // use one the attacker used (Imperial Tactics, CR p. 35).
    val attackerUsed: Set[DecisionOptionRef] =
      if side != CampaignPlanSide.Defender then Set.empty
      else CampaignAnswers.picks(pending,
        CampaignIds.planDecision(CampaignPlanSide.Attacker)).toSet
    val listed = offers.filterNot(offered =>
      chosen.contains(CampaignPlans.refOf(offered.offer.source)))
      .map(offered => offered.offer.withheld match
        case _ if attackerUsed(CampaignPlans.refOf(offered.offer.source)) =>
          unavailable(offered,
            OptionReason.Written(CampaignPlanChoice.attackerUsed))
        case Some(reason) => unavailable(offered, reason)
        case None => pass.applies(applicationOf(setup, offered)) match
          case Right(operations) => warned(offered, PlanPrice.priced(
            CampaignPlans.optionOf(offered), offered.offer, operations))
          case Left(violation) =>
            unavailable(offered, OptionReason.Written(PlanPrice.shortfall(
              pass.state, user, offered.offer, violation))))
    // A plan that awaits an offer, or one the allies could still pay for,
    // keeps the window open for them (Empire phase design, "The window").
    // While the decision waits only the offers change, and this reads the
    // allies' holdings and the full-aid view, not what they offered, so the
    // parked decision stays put as they offer and withdraw.
    val unchosen = offers.filterNot(offered =>
      chosen.contains(CampaignPlans.refOf(offered.offer.source)))
    val aidable = PlanAid.window(pass.state, actor, side, user).exists(at =>
      unchosen.exists(offered => offered.offer.awaitsAid &&
        !attackerUsed(CampaignPlans.refOf(offered.offer.source))) ||
      unchosen.exists(offered => offered.offer.withheld.isEmpty &&
        pass.applies(applicationOf(setup, offered)).left.exists(
          PlanAid.couldCover(pass.state, at, _,
            PlanAid.printed(offered.offer.costs)))))
    val playable = listed.exists(_.available)
    val asked = playable || aidable
    if !asked && !pass.resuming then Vector.empty
    else Vector[Operation](
      Branch((_, _) => if !asked then Vector.empty
        else Vector(decision(decisionId, user, listed, chosen.size))),
      Branch((_, tree) => CampaignAnswers.lastPick(tree, decisionId) match {
        case None => Vector.empty
        case Some(ref) => offers.find(offered =>
          CampaignPlans.refOf(offered.offer.source) == ref) match {
          // The application is recorded as one pick (`OpenPick`), so what
          // it pays, however a power changed the price, is what the plan
          // paid. A button plan, the title's, keeps its words.
          case Some(offered) => Vector[Operation](
            CampaignNotes.leaf(CampaignNotes.activationNote(
              offered.offer.source, user, actor)),
            OpenPick(decisionId, ref, offered.offer.source match
              case _: CampaignPlanSource.Title => Some(offered.offer.label)
              case _ => None),
            applicationOf(setup, offered),
            ClosePick())
          case None => Vector(BuildOps((_, _) => Left(
            OathViolation.InvalidEventOrder(
              "a chosen Campaign battle plan is no longer available"))))
        }
      }))

  private def decision(id: String, user: PlayerId,
      listed: Vector[DecisionOption], picks: Int): Decide = Decide(id, user,
    DecisionQuery.ChooseOne(listed :+
      DecisionOption.Button(CampaignIds.finish,
        CampaignPlanChoice.finishWith(picks)),
    heading = Some(side match {
      case CampaignPlanSide.Attacker => "Choose a battle plan, or finish"
      case CampaignPlanSide.Defender =>
        "Defender: choose a battle plan, or finish"
    })))

object CampaignPlanChoice:
  /** Why the defender cannot use a plan the attacker used (CR p. 35). */
  val attackerUsed: String = "The attacker used this plan"

  /** "Finish with no battle plans", or "with 2 battle plans": "Finish" says
    * how many plans the side fights with, as the modifier decision's "Start"
    * says how many modifiers the action starts with. */
  def finishWith(picks: Int): String = picks match
    case 0 => "Finish with no battle plans"
    case 1 => "Finish with 1 battle plan"
    case _ => s"Finish with $picks battle plans"
