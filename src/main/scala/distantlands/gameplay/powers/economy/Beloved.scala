package distantlands.gameplay.powers.economy

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import distantlands.model._

object BelovedCard extends Legacy(LegacyId("L33"), "Beloved",
    LegacyGoal.MostOfSuit, Suit.Beast, Some(FoundationNumber.IV)):
  val power = PrintedPower(PowerId("legacy.beloved"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation IV to Teeming World. **WHEN TRADING:** You " +
      "may trade with advisers held by other players whose pawns " +
      "are at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Beloved (legacy L33), its WHEN TRADING power: "You may trade with
  * advisers held by other players whose pawns are at your site" (Legacy
  * in-play powers design, ruling 18). Its foundation is the Legacies
  * batch's.
  *
  * Automatic for its holder, as no one is asked: the Trade's sources widen
  * to the faceup denizen advisers of every other player whose pawn stands
  * at the site the holder acts from, after the site's own cards
  * (`EconomySources.widened`). A facedown adviser is not offered, since its
  * suit is hidden, and one holding tokens is shown unavailable, as at a
  * site. Trade's own rules are unchanged: the cost goes on that adviser,
  * the favor comes from its suit's bank, and the bonus counts the trader's
  * own matching advisers. Tokens leave the card at the next Rest, as
  * every card's do (implementation ruling R21). It
  * writes no line: the Trade's line names the card (implementation ruling
  * R8).
  */
case object Beloved extends ContributingPower:
  val id: PowerId = BelovedCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TradeSourceSelection -> Vector(Transform((ctx, operations) =>
      EconomySources.widened(ctx, operations,
        advisersAtSite(ctx.catalog, ctx.state, ctx.actor)))))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, BelovedCard.id).contains(ctx.actor)

  /** The faceup denizen advisers, with a suit, of every other player whose
    * pawn stands where `actor` acts from, in seat then adviser order. */
  def advisersAtSite(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[SiteDenizenState] =
    PowerAccess.presentSite(ready, actor).toVector.flatMap(site =>
      ready.game.current.players.filter(other => other.player != actor &&
        other.pawnSite.contains(site)).flatMap(_.advisers.collect {
          case card @ DenizenState(_, Orientation.FaceUp, _)
              if catalog.suitOf(card.id).nonEmpty => card
        }))
