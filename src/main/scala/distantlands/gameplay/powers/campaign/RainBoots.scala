package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.model._

object RainBootsCard extends Denizen(DenizenId("14"), "Rain Boots", Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.rain-boots"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.AttackPlan),
    text = "Ignore all your enemy's rolls of single shields [shield]. " +
      "At end, discard Rain Boots.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Rain Boots (card 14), an attacker's battle plan: "Ignore all your enemy's
  * rolls of single shields [shield]. At end, discard Rain Boots."
  *
  * It is free. Only the defender rolls shields, so it is the attacker's plan.
  * Once the defense is rolled, each single-shield die scores 0, exactly as for
  * Bag of Siegeworks (`SingleShields`); two shields and doublers are
  * unaffected. Unlike the Bag it is offered in a Raid too. With the Bag also
  * chosen, both write the same score. The card is discarded through the
  * standard discard once the Campaign has resolved, whoever won, as Horse
  * Archers is.
  */
case object RainBoots extends DenizenPlan(RainBootsCard,
    RainBootsCard.power) with DiscardedAtEnd:
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Attacker)
  private val notes = NoteSet(id)
  notes.include(SingleShields.notes)
  override def planNoteKeys: Vector[NoteKey] = notes.declared

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Rain Boots: ignore single shields", Vector.empty, Vector.empty))

  override def otherWindows
      : Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignDefenseResult -> (use =>
      SingleShields.ignore(use, id, PowerSourceRef.Card(cardId))))
