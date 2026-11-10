package distantlands.gameplay

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite, DesolateShoreSite, DunesSite, FairIsleSite,
  NarrowPassSite}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table

/** The Narrow Pass's map. Cradle: Ancient City, Broken Peaks. Provinces:
  * the Pass, Buried Giant, Deep Woods. Hinterland: Desolate Shore, Dunes,
  * Fair Isle. Each site holds its printed bandits. No Table step lays out
  * regions, so the map is built here.
  */
object PassMap:
  val pass: SiteId = NarrowPassSite.id
  val ancientCity: SiteId = AncientCitySite.id
  val brokenPeaks: SiteId = BrokenPeaksSite.id
  val buriedGiant: SiteId = BuriedGiantSite.id
  val deepWoods: SiteId = DeepWoodsSite.id
  val desolateShore: SiteId = DesolateShoreSite.id
  val dunes: SiteId = DunesSite.id
  val fairIsle: SiteId = FairIsleSite.id

  val map: MapState =
    val cradle = Vector(ancientCity, brokenPeaks)
    val provinces = Vector(pass, buriedGiant, deepWoods)
    val hinterland = Vector(desolateShore, dunes, fairIsle)
    val states = (cradle ++ provinces ++ hinterland).map { id =>
      val definition = catalog.sites.find(_.id == id).get
      id -> SiteState(
        if definition.capacity == 0 then SiteForces.Empty
        else SiteForces.Occupied(ForceKind.Bandit, definition.capacity),
        Vector.empty, Vector.empty, definition.startingResources)
    }.toMap
    MapState(cradle, provinces, hinterland, states)

  /** `ready` on `map`, with `player`'s pawn at `site`. */
  def withPawnAt(ready: ReadyGame, player: PlayerId, site: SiteId,
      on: MapState = map): ReadyGame =
    Table.from(ready.updateCurrent(_.copy(map = on))).pawn(player, at = site)
      .ready
