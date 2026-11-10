package distantlands.gameplay.powers.travel

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.operations.SupplyStrip
import distantlands.gameplay.powerresolver.{Contribution, PowerCtx, Transform}
import distantlands.gameplay.powers.{PlayerFacts, SelectedModifier}
import distantlands.model._

object AFastSteedCard extends Denizen(DenizenId("172"), "A Fast Steed", Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.old-fast-steed"),
    persistent = false, cost = Cost(favor = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
    text = "Spend no Supply if you have three or fewer warbands on your " +
      "board.")
  val powers: Vector[PrintedPower] = Vector(power)

/** A Fast Steed (card 172), a selected Travel modifier. Cost: 1 favor placed
  * on the card. Travel spends no Supply while the player has three or fewer
  * warbands on their board.
  *
  * The warbands are read when the cost node folds, after the favor is
  * placed and before anything else changes the board. The favor is paid
  * whether or not the condition holds, as for Tents: the player owns the
  * choice to select it. The start line's cost span shows what was paid, so
  * it writes no line. The catalog's id carries a typo, `old-fast-steed`.
  */
case object AFastSteed extends SelectedModifier:
  val id: PowerId = AFastSteedCard.power.id
  val Favor: Int = 1
  /** The most warbands on the board for which Travel is free. */
  val MostWarbands: Int = 3

  val card: Denizen = AFastSteedCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Travel)
  override def cost: Cost = Cost(favor = AFastSteed.Favor)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      if fewWarbands(ctx.state, ctx.actor) then
        SupplyStrip(ctx, ctx.actor, operations)
      else operations)))

  override def appliesAt(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).nonEmpty

  private def fewWarbands(ready: ReadyGame, actor: PlayerId): Boolean =
    PlayerFacts.player(ready, actor)
      .exists(_.board.warbands <= AFastSteed.MostWarbands)
