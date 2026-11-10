package distantlands.gameplay.powers.cardplay

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.EffectiveSuit
import distantlands.gameplay.powerresolver.{Contribution, PowerCtx, Transform}
import distantlands.gameplay.powers.{PlayerFacts, SelectedModifier}
import distantlands.model._

object WildCryCard extends Denizen(DenizenId("189"), "Wild Cry", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.wild-cry"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "If you play a [suit-beast] card, gain 1 Supply and 2 " +
      "warbands.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Wild Cry (card 189), a selected Search modifier: when you play a beast
  * denizen faceup, to a site or as a faceup adviser, gain 1 Supply and 2
  * warbands. A facedown play does not trigger it, and a card does not trigger
  * on its own play.
  *
  * The trigger reads only the played card, so the fold is the same on every
  * resume. Gaining warbands is best-effort: an empty warband bank gives what
  * it holds.
  */
case object WildCry extends SelectedModifier:
  val id: PowerId = WildCryCard.power.id
  val Supply: Int = 1
  val Warbands: Int = 2

  val card: Denizen = WildCryCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Search)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ActionCardPlayedFaceup -> Vector(Transform((ctx, children) =>
      children ++ effects(ctx.actor))))

  override def appliesAt(ctx: PowerCtx): Boolean = ctx.operation match
    case CardPlayedFaceup(card: DenizenId, _, _, _) =>
      card != cardId &&
        EffectiveSuit.of(ctx.catalog, ctx.state, card).contains(Suit.Beast)
    case _ => false

  private def effects(actor: PlayerId): Vector[Operation] = Vector(
    GainSupply(actor, WildCry.Supply),
    BuildOps((ready, _) => PlayerFacts.forceKind(ready, actor).map(kind =>
      Vector(Gain.Warbands(actor, kind, WildCry.Warbands)))))
