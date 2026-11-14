package distantlands.gameplay.powers.travel

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import distantlands.model._

object AncestralLandsCard extends Legacy(LegacyId("L08"), "Ancestral Lands",
    LegacyGoal.GrandScepter, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.ancestral-lands"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **TRAVEL:** Traveling to the " +
      "Cradle costs you only 1 Supply.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ancestral Lands (legacy L08): "If you are an Imperial: Travel:
  * Traveling to the Cradle costs you only 1 Supply" (Legacy in-play powers
  * design, ruling 14).
  *
  * Its Imperial holder's Travel to a Cradle site spends at most 1 Supply.
  * It folds after every other Travel power, Special Envoy included, so it
  * caps what they leave and never raises a cost one of them made lower or
  * removed. Way Station folds `Late` and can only lower it further. Only
  * the cost changes: it hooks no restriction, so a Pass still restricts.
  * It writes no line (implementation ruling R8).
  */
case object AncestralLands extends ContributingPower:
  val id: PowerId = AncestralLandsCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** Folds after every other Travel power, Special Envoy included. */
  val Priority: Int = SpecialEnvoy.Priority + 1
  override def priority: Int = AncestralLands.Priority

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      TravelRoute.adjustSupply(ctx, operations)(_.min(1)))))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, AncestralLandsCard.id,
      ChronicleLegacyRules.imperialRoles).contains(ctx.actor) &&
      TravelRoute.pawnMove(ctx.operation).exists(route =>
        route.player == ctx.actor &&
          ctx.state.game.current.map.regionOf(route.destination)
            .contains(Region.Cradle))
