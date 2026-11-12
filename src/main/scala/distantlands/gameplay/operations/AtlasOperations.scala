package distantlands.gameplay.operations

import distantlands.model._

/** Writes of the operations that take sites out of the Atlas (Foundations
  * design, slice 3, "Explore"). */
private[operations] object AtlasOperations:
  import OperationError.InvalidDescription

  /** The site at the operation's end of the Atlas leaves it for the next
    * slot, populated by `SiteState.banditSite`. It takes the bandits its
    * capacity asks for, or as many as the bank holds, and the top cards of
    * the relic deck for its open relic slots, or as many as the deck
    * holds. */
  def placeSite(ready: ReadyGame, placement: PlaceAtlasSite)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    for
      stored <- ready.game.campaign.atlas.site(placement.end).toRight(
        InvalidDescription("the Atlas holds no site to place"))
      _ <- Either.cond(current.map.nextSlot.contains(placement.region), (),
        InvalidDescription(s"the next empty slot is not in the " +
          s"${placement.region.key}"))
      bank <- OperationStateAdapter.quantity(ready,
        Piece.Warbands(ForceKind.Bandit, 1),
        Location.WarbandBank(ForceKind.Bandit))
    yield
      val bandits = bank match
        case AvailableQuantity.Finite(held) =>
          math.min(placement.capacity, held)
        case AvailableQuantity.Unbounded => placement.capacity
      val dealt = current.commonCards.relicDeck.take(SiteState.openRelicSlots(
        placement.relicSlots, stored.relics.size))
      val site = SiteState.banditSite(bandits,
        placement.resources, stored.placedDenizens,
        stored.relics.map(_.id) ++ dealt)
      ready.updateCampaign(campaign => campaign.copy(
          atlas = campaign.atlas.without(stored.id)))
        .updateCurrent(_.copy(
          map = current.map.withSite(placement.region, stored.id, site),
          commonCards = current.commonCards.copy(relicDeck =
            current.commonCards.relicDeck.drop(dealt.size))))
