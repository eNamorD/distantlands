package distantlands.gameplay.powers.travel

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction, Transform}
import distantlands.model._

object PathfinderCard extends Legacy(LegacyId("L36"), "Pathfinder",
    LegacyGoal.MostOfSuit, Suit.Nomad, Some(FoundationNumber.I)):
  val power = PrintedPower(PowerId("legacy.pathfinder"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation I to Wide Horizons. **TRAVEL:** You may " +
      "ignore the Mountain, Island, and Pass powers.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Pathfinder (legacy L36): "Travel: You may ignore the Mountain, Island,
  * and Pass powers" (Legacy in-play powers design, ruling 15). Its
  * foundation, Wide Horizons, is the Legacies batch's.
  *
  * Its holder's Travel ignores them always, with no question, since
  * ignoring them is never worse. The ignore is the collector's named
  * ignore, so this power hooks each window those powers hook: `TravelCost`
  * (the Mountain and Island adds, Lost Lagoon's included, implementation
  * ruling R6) and `TravelActionEligibility` (the Pass's restriction),
  * contributing nothing there but its vote. The Pass's campaign-targeting
  * limit hooks `CampaignTargetSelection`, which this power does not, so it
  * still applies. It writes no line (R8).
  */
case object Pathfinder extends ContributingPower:
  val id: PowerId = PathfinderCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((_, operations) => operations)),
    PowerWindow.TravelActionEligibility -> Vector(
      Restriction((_, _) => None)))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, PathfinderCard.id)
      .contains(ctx.actor) &&
      TravelRoute.pawnMove(ctx.operation).exists(_.player == ctx.actor)

  /** The powers Pathfinder's holder ignores. */
  def ignored(other: ContributingPower): Boolean = other match
    case _: MountainSitePower | _: IslandSitePower | LostLagoon |
        _: NarrowPassSitePower => true
    case _ => false

  override def shouldIgnore(other: ContributingPower): Boolean =
    Pathfinder.ignored(other)
