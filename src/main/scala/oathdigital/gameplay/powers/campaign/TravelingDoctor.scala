package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.powerresolver.{ContributingPower, PowerCtx}
import oathdigital.model._

object TravelingDoctorCard extends Denizen(DenizenId("51"), "Traveling Doctor", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.traveling-doctor"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "If you're defeated, kill no warbands in your force and " +
      "discard Traveling Doctor. **Ignore** powers that kill all " +
      "of your force.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Traveling Doctor (card 51), a battle plan for either side: "If you're
  * defeated, kill no warbands in your force and discard Traveling Doctor.
  * Ignore powers that kill all of your force."
  *
  * Free. The rule, its ignore and its line are `SparedForce`'s, shared with
  * Billowing Fog. When its user was defeated, the card takes the standard
  * discard once the Campaign has resolved (`PlanDiscard.afterCampaign`). A
  * bandit defender applies it too: bandits are no player, so the rule spares
  * nothing and every targeted bandit still dies, and the card is discarded.
  */
case object TravelingDoctor extends BattlePlan with DiscardedAtEnd:
  val id: PowerId = TravelingDoctorCard.power.id

  val card: Denizen = TravelingDoctorCard

  def cardRef: DecisionOptionRef = DecisionOptionRef.Denizen(cardId)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)
  override def priority: Int = SparedForce.Priority
  private val notes = NoteSet(id)
  notes.include(SparedForce.notes)
  override def planNoteKeys: Vector[NoteKey] = notes.declared

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Traveling Doctor: if defeated, kill no warbands in your force, then " +
        "discard it", Vector.empty, SparedForce.effects(context.side)))

  override def wrapping: Map[PowerWindow, BattlePlan.Wrap] = Map(
    PowerWindow.CampaignLosses -> ((ctx, use, losses) =>
      SparedForce.spare(ctx, id, cardId, use, losses)))

  override def endDiscard: PlanUse => Vector[Operation] = use =>
    if use.won.contains(false) then
      PlanDiscard.afterCampaign(id, use, card)
    else Vector.empty

  override def ignores(ctx: PowerCtx, other: ContributingPower): Boolean =
    SparedForce.ignores(ctx, this, other)
