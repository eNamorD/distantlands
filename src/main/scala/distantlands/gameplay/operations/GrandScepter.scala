package distantlands.gameplay.operations

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.model._

object TheGrandScepterCard extends Relic(RelicId("grand-scepter"), "The Grand Scepter", value = 0, defense = 3):
  val restriction = PrintedPower(PowerId("relic.the-grand-scepter.restriction"),
    persistent = false, cost = Cost.free,
    text = "This relic **cannot** be removed from play or added to a " +
      "lineage.")
  val campaign = PrintedPower(PowerId("relic.the-grand-scepter.campaign"),
    persistent = false, cost = Cost(favorBurnt = 3),
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "[favor-burnt][favor-burnt][favor-burnt] If you are Imperial " +
      "and victorious against a Citizen, you may exile them.")
  val action = PrintedPower(PowerId("relic.the-grand-scepter.action"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** You may become a Citizen if an Exile. **IN " +
      "NEGOTIATION:** May offer Citizenship.")
  val negotiation = PrintedPower(PowerId("relic.the-grand-scepter.negotiation"),
    persistent = false, cost = Cost.free,
    text = "**IN NEGOTIATION:** May offer Citizenship.")
  val powers: Vector[PrintedPower] = Vector(restriction, campaign, action, negotiation)

/** The Grand Scepter cannot be removed from play (global operation
  * restrictions design, "Rules"). A move that takes it from a site or a play
  * area to anywhere else is refused: a discard to the set-aside relics or a
  * return to the relic deck. A `Bury` of it is refused too. Passing it between
  * players, by a `Take` or a `Give`, keeps it in play and is allowed. The
  * scepter is never facedown, so a refusal reveals nothing.
  *
  * There is one Grand Scepter, so the restriction names its card and holds
  * whichever relics a catalog lists.
  */
case object GrandScepter extends OperationRestriction with PrintedRule:
  val relic: RelicId = TheGrandScepterCard.id
  val id: PowerId = TheGrandScepterCard.restriction.id
  val source: PowerSourceRef = PowerSourceRef.Card(relic)

  override def reason(ready: ReadyGame,
      operation: CoreOperation): Option[OperationReason] =
    Option.when(Operation.flatten(operation).exists(removes))(OperationReason(
      "grand-scepter", s"${relic.value} cannot be removed from play",
      OperationReasonKind.Impossible))

  private def removes(step: Operation): Boolean = step match
    case Move(Piece.Card(moved), from, to, _, _) if moved == relic =>
      OperationRestrictions.inPlay(from.location) &&
        !OperationRestrictions.inPlay(to.location)
    case Bury(BuryableCard.Relic(buried), _, _) => buried == relic
    case _ => false

/** The Grand Scepter's "IN NEGOTIATION: May offer Citizenship." No power
  * runs it: the Negotiation procedure's bounds offer its holder the
  * Citizenship term (`NegotiationDeal`), and the deal's settlement makes
  * each named Exile a Citizen. Listed among the printed rules so that the
  * power reads as implemented. */
case object GrandScepterNegotiation extends PrintedRule:
  val id: PowerId = TheGrandScepterCard.negotiation.id
  val source: PowerSourceRef = PowerSourceRef.Card(TheGrandScepterCard.id)

/** Where a new Chancellor takes the Grand Scepter from: its holder, the
  * site it lies at, or faceup from the box when it is in neither (Chronicle
  * ruling 10, extended to every source by the Empire phase design, "Grand
  * Scepter source"). Nothing when `player` holds it already. */
object GrandScepterSource:
  def take(ready: ReadyGame, player: PlayerId): Vector[CoreOperation] =
    val id = TheGrandScepterCard.id
    val current = ready.game.current
    val held = current.players.find(_.relics.exists(_.id == id))
      .map(state => Location.PlayArea(state.player))
    val atSite = current.map.sites.collectFirst {
      case (site, state) if state.relics.exists(_.id == id) =>
        Location.Site(site)
    }
    held.orElse(atSite) match
      case Some(Location.PlayArea(`player`)) => Vector.empty
      case Some(from) => Vector(Move(Piece.Card(id), PositionedLocation(from),
        PositionedLocation(Location.PlayArea(player)),
        resultingOrientation = Some(Orientation.FaceUp)))
      case None => Vector(GrandScepterFromBox(player))
