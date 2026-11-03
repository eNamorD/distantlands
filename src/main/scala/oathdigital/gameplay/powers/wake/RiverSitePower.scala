package oathdigital.gameplay.powers.wake

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.cards.{AncientCitySite, HeadwatersSite,
  RiverbankSite, TidalMarshesSite}
import oathdigital.gameplay.powerresolver.PhasePower
import oathdigital.gameplay.powers.action.PawnMoves
import oathdigital.model._

/** The River site power (CR p. 31, NF p. 11), WAKE: "You may place your pawn
  * at another River. This is not a Travel action."
  *
  * It belongs to its site, so the player whose pawn is there may use it, once
  * per turn from each River; the engine enforces both. The placement is a
  * plain `Move`: no Supply is paid and no Travel window runs, so no Pass and
  * no Travel cost modifier applies. Its notes are written under "River",
  * since four sites share the power.
  */
final case class RiverSitePower(id: PowerId, site: SiteId)
    extends PhasePower:
  import RiverSitePower._

  def timing: PowerTiming = PowerTiming.Wake
  private val own = NoteSet(id)
  own.include(RiverSitePower.notes)
  override def noteKeys: Vector[NoteKey] = own.declared
  override def noteSource: Option[String] = Some(name)

  def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    destinations(ready).nonEmpty

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    PawnMoves.siteChoice(siteDecisionId, player, destinations(ready),
      "River: choose the River to place your pawn at"),
    BuildOps((state, pending) => PawnMoves.chosenSite(pending, siteDecisionId)
      .flatMap(PawnMoves.relocate(state, player, _))),
    Note(id, PawnMoves.placedNote(placed, source, player)))))

  /** Every River in play but this one, in map order. */
  private def destinations(ready: ReadyGame): Vector[SiteId] =
    ready.game.current.map.inPlay.filter(other =>
      other != site && isRiver(other))

object RiverSitePower:
  val name: String = "River"
  val siteDecisionId: String = "power.river.site"
  /** The keys the River powers share: one line written by whichever river
    * site's power ran. No one power owns it. */
  val notes: NoteSet = NoteSet.unowned("RiverSitePower")
  /** "{player} placed at {site}.", the power's own line. */
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)

  /** One River power for each River site. */
  val all: Vector[RiverSitePower] = Vector(
    RiverSitePower(AncientCitySite.river, AncientCitySite.id),
    RiverSitePower(HeadwatersSite.river, HeadwatersSite.id),
    RiverSitePower(RiverbankSite.river, RiverbankSite.id),
    RiverSitePower(TidalMarshesSite.river, TidalMarshesSite.id))

  def isRiver(site: SiteId): Boolean = all.exists(_.site == site)
