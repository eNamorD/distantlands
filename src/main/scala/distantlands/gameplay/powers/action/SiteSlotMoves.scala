package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.cardplay.CardSlots
import distantlands.model._

/** Where a site card that moves itself may go (Giant Python, Roving Terror;
  * catalog batch 8, slice 10): `slot` at `site`, replacing `replaced` with
  * its suit, or empty. `makesRoom` when the replace frees the only slot
  * the mover can take: the site was full (catalog batch 11, ruling 20). */
final case class SlotMove(site: SiteId, slot: DecisionOptionRef.SiteSlot,
    replaced: Option[(SiteDenizenState, Suit)], makesRoom: Boolean)

/** One question over the slots of the sites other than `mover`'s, as card
  * play asks it (`CardSlots`): each site's first empty slot, when `empty`
  * allows one and the site has room, then each card there that may be
  * replaced, marked Replace. A replaceable card is a faceup denizen or a
  * ruined edifice ([[SiteCards]]); an intact edifice is locked and stays.
  *
  * Each option is judged for the actor by the printed restrictions, so a
  * card the Hall of Ministers protects is askable of its ruler or shown
  * with its reason. A Replace discards the card the standard way, and the
  * mover takes the site's last slot with its tokens, since a row has no
  * gaps. The discard names `power`, the mover's power.
  */
private[powers] final class SiteSlotMoves(mover: DenizenId, power: PowerId,
    empty: Boolean):
  private val cards = new SiteCards(Suit.all.toSet)

  /** Every slot the mover may take, in map order then slot order. */
  def moves(catalog: ExecutableCatalog, ready: ReadyGame): Vector[SlotMove] =
    ready.game.current.faceUpDenizenSite(mover).toVector.flatMap(here =>
      ready.game.current.map.inPlay.filter(_ != here).flatMap(site =>
        movesAt(catalog, ready, site)))

  private def movesAt(catalog: ExecutableCatalog,
      ready: ReadyGame, site: SiteId): Vector[SlotMove] =
    val held = ready.game.current.map.sites.get(site).toVector
      .flatMap(_.denizens)
    val room = catalog.site(site).exists(held.size < _.capacity)
    val open = Option.when(empty && room)(SlotMove(site,
      DecisionOptionRef.SiteSlot(site, held.size), None, makesRoom = false))
    val replacing = cards.at(catalog, ready, site).flatMap((card, suit) =>
      val index = held.indexWhere(_.id == card.id)
      Option.when(index >= 0)(SlotMove(site,
        DecisionOptionRef.SiteSlot(site, index), Some(card -> suit),
        makesRoom = !room)))
    open.toVector ++ replacing

  private def option(move: SlotMove): DecisionOption =
    val slot = DecisionOption.SiteSlot(move.slot)
    move.replaced.fold[DecisionOption](slot)(replaced =>
      DecisionOption.Replacing(slot, replaced._1.id))

  /** Each move's option, judged for `actor`. */
  def options(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId): Vector[DecisionOption] =
    moves(catalog, ready).flatMap(move => CardSlots.judge(catalog, ready,
      actor, option(move), operations(ready, actor, move)))

  /** The move an answer names, read live. */
  def chosen(catalog: ExecutableCatalog,
      ready: ReadyGame, ref: DecisionOptionRef): Option[SlotMove] =
    moves(catalog, ready).find(_.slot == ref)

  /** The discard, if any, then the mover's move. */
  def operations(ready: ReadyGame, actor: PlayerId, move: SlotMove)
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      here <- ready.game.current.faceUpDenizenSite(mover).toRight(
        OathViolation.InvalidEventOrder(s"${mover.value} is not at a site"))
      discard <- move.replaced.fold[Either[OathViolation,
          Vector[CoreOperation]]](Right(Vector.empty))(replaced =>
        cards.discard(ready, move.site, replaced._1, replaced._2, actor,
          required = true, cause = Some(DiscardCause(power,
            makesRoom = move.makesRoom))).map(Vector(_)))
    yield discard :+ Move(Piece.Card(mover),
      PositionedLocation(Location.Site(here)),
      PositionedLocation(Location.Site(move.site)), by = Some(actor))
