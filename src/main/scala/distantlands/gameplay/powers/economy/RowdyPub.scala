package distantlands.gameplay.powers.economy

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower, SiteOnly}
import distantlands.gameplay.actions.economy.MusterProcedure
import distantlands.gameplay.powerresolver.{Contribution, Transform}
import distantlands.gameplay.powers.{PlayerFacts, PowerAnswers, SelectedModifier}
import distantlands.model._

object RowdyPubCard extends Denizen(DenizenId("144"), "Rowdy Pub", Suit.Hearth) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.rowdy-pub"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Muster)),
    text = "Gain one more warband if mustering from Rowdy Pub.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Rowdy Pub (card 144), a selected Muster modifier: mustering from Rowdy Pub
  * gains one more warband, on top of the bonus for matching advisers.
  *
  * The card mustered from is the answer to the Muster's source decision, read
  * when the gain node runs, so the extra warband is added only when Rowdy Pub is
  * the source. The gain is best-effort like the base gain.
  */
case object RowdyPub extends SelectedModifier:
  val id: PowerId = RowdyPubCard.power.id
  val Warbands: Int = 1

  val card: Denizen = RowdyPubCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Muster)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.MusterGain -> Vector(Transform((ctx, operations) =>
      operations :+ bonus(ctx.actor))))

  private def bonus(actor: PlayerId): Operation = BuildOps((ready, pending) =>
    if PowerAnswers.one(pending, MusterProcedure.decisionId)
        .contains(DecisionOptionRef.Denizen(card.id)) then
      PlayerFacts.forceKind(ready, actor).map(kind =>
        Vector[CoreOperation](Gain.Warbands(actor, kind, RowdyPub.Warbands)))
    else Right(Vector.empty))
