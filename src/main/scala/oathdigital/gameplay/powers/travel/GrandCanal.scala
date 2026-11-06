package oathdigital.gameplay.powers.travel

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

object GrandCanalCard extends Edifice(EdificeId("E03"), Suit.Discord):
  object intact extends EdificeFace("Grand Canal") with Locked:
    val power = PrintedPower(PowerId("edifice.e03.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is a Coast along with its other powers.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Lagoon"):
    val power = PrintedPower(PowerId("edifice.e03.ruined"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is an Island along with its other " +
        "powers.")
    val powers: Vector[PrintedPower] = Vector(power)

/** A face of E03 that gives the site it stands at a printed terrain
  * (catalog batch 10, slice 5). The site changes as the card moves or
  * flips, so it is read from the state on every use, never stored.
  *
  * `source` is static and cannot name that site, so it is the face's game
  * rule, as Hallowed Spring's is. The terrain is still the site's: Forest
  * Paths ignores a `LiveTerrain` as it ignores the powers of sites.
  */
sealed trait LiveTerrain extends ContributingPower:
  /** The face this power is printed on. */
  def side: EdificeSide
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  /** The site this face stands at, if it is in play. */
  final def site(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, GrandCanalCard.id, side)

/** Grand Canal (E03, intact): "Act as if this site is a Coast along with
  * its other powers." Its site joins the live Coast set
  * ([[TravelTerrain]]), so a Coast route reaches it for 1 Supply. This
  * power is the Coast power at its site: a Coast route from there costs 1
  * Supply and ignores the terrain adds, as `CoastSitePower` does. It
  * writes no line.
  */
case object GrandCanal extends LiveTerrain:
  val id: PowerId = GrandCanalCard.intact.power.id
  val side: EdificeSide = EdificeSide.Intact

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      TravelRoute.adjustSupply(ctx, operations)(_ => 1))))

  override def applicable(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.source) &&
        TravelTerrain.coastRoute(ctx.state, route))

  override def shouldIgnore(other: ContributingPower): Boolean =
    TravelTerrain.ignoredOnCoastRoute(other)

/** Lost Lagoon (E03, ruined): "Act as if this site is an Island along with
  * its other powers." An Island costs 2 Supply more to reach "unless
  * you're traveling from a Coast or Island": a Coast route into the site
  * skips the add, since every Coast power ignores this power. An Island
  * "has the Coast power" (ruling 12), so the site joins the live Coast
  * set, and a Coast route from it costs 1 Supply and ignores the terrain
  * adds. Arriving adds 2; leaving by a Coast route sets 1. A route never
  * does both, since it cannot leave and reach the same site. At a printed
  * Island the site is still one Island, so arriving adds nothing more
  * (ruling 13). It writes no line.
  */
case object LostLagoon extends LiveTerrain:
  val id: PowerId = GrandCanalCard.ruined.power.id
  val side: EdificeSide = EdificeSide.Ruined
  /** The Island's add. */
  val Add: Int = 2

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      if leavesByCoastRoute(ctx) then
        TravelRoute.adjustSupply(ctx, operations)(_ => 1)
      else TravelRoute.adjustSupply(ctx, operations)(_ + Add))))

  override def applicable(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.destination) &&
        !TravelSitePowers.printedIslandSites.contains(route.destination)) ||
      leavesByCoastRoute(ctx)

  /** The other terrain adds, only while it is the Coast power. It never
    * ignores itself. */
  override def ignores(ctx: PowerCtx, other: ContributingPower): Boolean =
    other != LostLagoon && leavesByCoastRoute(ctx) &&
      TravelTerrain.ignoredOnCoastRoute(other)

  private def leavesByCoastRoute(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.source) &&
        TravelTerrain.coastRoute(ctx.state, route))
