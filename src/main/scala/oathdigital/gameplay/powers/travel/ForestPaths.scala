package oathdigital.gameplay.powers.travel

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.operations.SupplyStrip
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution, PowerCtx, Transform}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object ForestPathsCard extends Denizen(DenizenId("43"), "Forest Paths", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.forest-paths"),
    persistent = false, cost = Cost(favor = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
    text = "Spend no Supply and ignore the powers of sites if you're " +
      "traveling to a site with a [suit-beast] card.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Forest Paths (card 43), a selected Travel modifier. Cost: 1 favor placed on
  * the card. If the destination holds a beast denizen or edifice, Travel costs
  * no Supply and the powers of sites are ignored for that Travel.
  *
  * The player owns the choice to select it. Picking Forest Paths places the
  * favor at once, in the modifier decision. Only when the destination holds a
  * beast card does it remove the Supply payment and ignore the site powers.
  *
  * Ignoring a power is a named ignore, decided per Travel (`ignores` reads the
  * route from the context): while the destination qualifies, every power whose
  * source is a site is dropped from the fold. So is a `LiveTerrain` (Grand
  * Canal, Lost Lagoon), which gives a site its terrain from an edifice. The
  * terrain adds and the Narrow Pass restriction both hook a Travel window,
  * so Forest Paths hooks both
  * windows too (the second with a transform that changes nothing), because the
  * collector only lets a power ignore what is gathered beside it.
  *
  * While it ignores the site powers it writes "Ignoring site powers."; its
  * Supply waiver writes nothing (power log lines design, "No line").
  */
case object ForestPaths extends SelectedModifier:
  val id: PowerId = ForestPathsCard.power.id
  val Favor: Int = 1
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Ignoring site powers." */
  val ignoring: NoteKey = notes.key("ignoring",
    Vector(NotePart.Text("Ignoring site powers.")))

  val card: Denizen = ForestPathsCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Travel)
  override def cost: Cost = Cost(favor = ForestPaths.Favor)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      if beastAtDestination(ctx) then
        Note(id, _ => Some(ForestPaths.ignoring(PowerSourceRef.Card(cardId)))) +:
          SupplyStrip(ctx, ctx.actor, operations)
      else operations)),
    PowerWindow.TravelActionEligibility ->
      Vector(Transform((_, operations) => operations)))

  override def appliesAt(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).nonEmpty

  private def beastAtDestination(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      TravelPayments.holdsSuit(ctx.state, route.destination, Suit.Beast,
        ctx.catalog.suitOf(_)))

  override def ignores(ctx: PowerCtx, other: ContributingPower): Boolean =
    beastAtDestination(ctx) && (other match
      case _: LiveTerrain => true
      case _ => other.source match
        case _: RuleSourceRef.Site => true
        case _ => false)
