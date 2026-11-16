package distantlands.gameplay.powers.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution, PowerCtx, Transform}
import distantlands.model._

/** A SETUP / WHEN EXPLORED edifice face for "you" who placed your pawn at
  * its site (catalog batch 13, rulings 11 and 17), shaped as `ForgeRule`
  * is: the placer in Setup's pawn window, the explorer after an Explore
  * (`EdificeSetupSupport.pawnPlacement`).
  *
  * Setup is one walk in which several players may place at the site, so
  * there a face's decision ids name the placer, under `setup.`, which
  * Setup's log narrates. An Explore has one explorer, so there the ids are
  * fixed and each face narrates them itself.
  *
  * Setup's pawn window orders its powers and gathers the offers again
  * after each one resolves (ruling 4), so `offers` must stay true for a
  * player while their own steps run.
  */
abstract class PlacedPawnEdificeRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide
  /** The face's name in its decision ids. */
  protected def slug: String

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  protected final def card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** The id of this face's `step` question: at Setup, for `placer`; at an
    * Explore, `None`. */
  protected final def decision(step: String,
      placer: Option[PlayerId]): String = placer match
    case Some(player) => s"setup.$slug.$step.${player.value}"
    case None => s"power.$slug.$step"

  /** Whether the face offers itself to `actor`, who placed their pawn at
    * its site. Every face does but one that needs something of them. */
  protected def offers(ready: ReadyGame, actor: PlayerId): Boolean = true

  private def at(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    for
      edificeSite <- ctx.state.game.current.edificeSite(edifice, side)
      placement <- EdificeSetupSupport.pawnPlacement(ctx)
      if placement._2 == edificeSite && offers(ctx.state, placement._1)
    yield placement

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The face's steps for `actor`, asking under `placer`'s ids. */
  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation]

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (ruling 4). An Explore folds them in. */
  final def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, _) => ResolutionOffer(
        EdificeSetupSupport.option(edifice),
        steps(ctx.catalog, actor, Some(actor)))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, _) =>
        ops ++ steps(ctx.catalog, actor, None)))))
