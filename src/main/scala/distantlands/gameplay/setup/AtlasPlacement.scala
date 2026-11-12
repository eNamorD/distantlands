package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** Builds the placement of an Atlas site (Foundations design, slice 3).
  * `PlaceAtlasSite` is applied with no catalog, so the walk that places a
  * site copies its printed capacity, relic slots and resources onto the
  * operation here. Explore places one site in play; Imperial Maps' choice
  * places one for each empty slot at Setup. */
object AtlasPlacement:
  /** A site about to leave the Atlas: what the Atlas stores of it, and the
    * operation that places it in the next slot. */
  final case class Planned(stored: AtlasEntry.StoredSite,
      operation: PlaceAtlasSite):
    def site: SiteId = stored.id

  /** The site `end` of the Atlas gives now, with the operation that places
    * it. Refused on a full map, with no site in the Atlas, and for a site
    * the catalog does not print. */
  def at(catalog: ExecutableCatalog, ready: ReadyGame, end: AtlasEnd)
      : Either[OathViolation, Planned] = for
    region <- ready.game.current.map.nextSlot.toRight(
      OathViolation.InvalidEventOrder("the map has no empty site slot"))
    stored <- ready.game.campaign.atlas.site(end).toRight(
      OathViolation.InvalidEventOrder("the Atlas holds no site"))
    printed <- catalog.site(stored.id).toRight(
      OathViolation.InvalidEventOrder(
        s"${stored.id.value} is not a site the catalog prints"))
  yield Planned(stored, PlaceAtlasSite(end, region, printed.capacity,
    printed.relicSlots, printed.startingResources))
