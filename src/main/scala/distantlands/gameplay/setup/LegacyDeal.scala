package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** Foundation III's deal of the legacy deck: the one arithmetic the plan
  * factory and game start share (Legacies design, slice 2; implementation
  * ruling R16). `seats` lists every seated player in seat order; `order`
  * below is always the deal's `legacyOrder`, the derived deck shuffled.
  *
  * Quiet Ambitions, the normal face and every first game's deal (ruling
  * 4): each player draws 2, in seat order, and keeps one in Setup. Public
  * Ambitions, the altered face: the first 4 are dealt faceup to the public
  * row and nobody draws. A short deck draws what remains (ruling 5).
  */
final case class LegacyDeal(face: FoundationFace, seats: Vector[PlayerId]):
  private val public = face == FoundationFace.Altered

  /** The legacies `player` draws, dormant: 2 under Quiet Ambitions, fewer
    * from a short deck, none under Public Ambitions. */
  def drawn(order: Vector[LegacyId], player: PlayerId): Vector[LegacyId] =
    val seat = seats.indexOf(player)
    if public || seat < 0 then Vector.empty
    else order.slice(LegacyDeal.QuietDraw * seat,
      LegacyDeal.QuietDraw * (seat + 1))

  /** The Public Ambitions row, faceup in deal order. */
  def publicRow(order: Vector[LegacyId]): Vector[LegacyId] =
    if public then order.take(LegacyDeal.PublicRow) else Vector.empty

  /** What the deal leaves of `order`, top first: the game's legacy deck
    * (catalog batch 13, ruling 1). */
  def rest(order: Vector[LegacyId]): Vector[LegacyId] =
    order.drop(if public then LegacyDeal.PublicRow
      else LegacyDeal.QuietDraw * seats.size)

object LegacyDeal:
  val QuietDraw: Int = 2
  val PublicRow: Int = 4

  /** The deal of the game `chronicle` starts for `participants`, under the
    * Chronicle's Foundation III face. Setup With No Empire does not erode
    * Foundation III. */
  def of(chronicle: Chronicle, participants: Vector[FirstGameParticipant])
      : LegacyDeal =
    LegacyDeal(StartingFoundations.of(chronicle)(FoundationNumber.III).face,
      participants.map(_.playerId))

  /** The legacy deck a game derives from `chronicle` (ruling 3), which
    * game start shuffles and deals from: every catalog legacy no lineage
    * of `chronicle` holds, seated or in the box, in catalog order. A
    * Chronicle stores no public row. */
  def deck(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Vector[LegacyId] =
    val held = chronicle.lineages.flatMap(_.legacies).map(_.id).toSet
    catalog.legacies.map(_.id).filterNot(held)
