package oathdigital.gameplay.actions.cardplay

import oathdigital.gameplay.actions.{DiscardPermission, SiteRemoval}
import oathdigital.model._

/** The causes of a card play's discards (catalog batch 11, slice 4;
  * rulings 20 and 22):
  *
  *  - a site discard names the power whose permission it spent, in the
  *    order `CardSlots.credited` spends them: the replaced card first, then
  *    the extras in answer order, against Homeland's permission first and
  *    then the powers'. A discard spending the Homeland rule's permission
  *    names the power that made the play, if any. Only `roomFor`, the
  *    replaced card of a site that was full, makes room; an extra discard
  *    never does;
  *  - a play-area adviser the play replaces names the power that made the
  *    play, if any, and always makes room;
  *  - the played card, discarded, names the power that made the play, if
  *    any.
  */
private[gameplay] object PlayDiscardCauses:
  def mark(operations: Vector[CoreOperation], played: WorldCardId,
      byPower: Option[PowerId], sitePermissions: Vector[DiscardPermission],
      roomFor: Option[CardId]): Vector[CoreOperation] =
    val discards = sitePermissions.filter(_.removal == SiteRemoval.Discard)
    def spent(permits: Vector[DiscardPermission], card: CardId)
        : Option[DiscardCause] =
      val makesRoom = roomFor.contains(card)
      permits.headOption.filter(_.fromPower).map(_.source).orElse(byPower)
        .map(DiscardCause(_, makesRoom = makesRoom))
    operations.foldLeft((Vector.empty[CoreOperation], discards)) {
      case ((done, permits), discard: Discard.Denizen)
          if atSite(discard.from) =>
        (done :+ discard.copy(cause = spent(permits, discard.card)),
          permits.drop(1))
      case ((done, permits), discard: Discard.RuinedEdifice)
          if atSite(discard.from) =>
        (done :+ discard.copy(cause = spent(permits, discard.card)),
          permits.drop(1))
      case ((done, permits), discard: Discard.Denizen)
          if discard.card != played =>
        (done :+ discard.copy(cause =
          byPower.map(DiscardCause(_, makesRoom = true))), permits)
      case ((done, permits), discard: Discard.Denizen) =>
        (done :+ discard.copy(cause = byPower.map(DiscardCause(_))), permits)
      case ((done, permits), other) => (done :+ other, permits)
    }._1

  private def atSite(from: PositionedLocation): Boolean =
    from.location match
      case _: Location.Site => true
      case _ => false
