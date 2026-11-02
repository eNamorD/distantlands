package oathdigital.gameplay.cards

import oathdigital.catalog.Site
import oathdigital.model.{PowerId, SiteId, Tokens}

/** The thirteen sites whose power a power object runs: the Travel terrain
  * handlers (`mountain`, `island`, `coast`, `pass`) belong to
  * `powers.travel.TravelSitePowers`, the `river` handlers to
  * `powers.wake.RiverSitePower`. The other eleven sites are in
  * `HomelandsAndPlains.scala`.
  */
object AncientCitySite extends Site(SiteId("site:ancient-city"), "Ancient City",
    defense = 2, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(3, 0)),
    homeland = None):
  val enduring = PowerId("site.ancient-city.enduring")
  val river = PowerId("site.ancient-city.river")
  val handlers: Vector[PowerId] = Vector(enduring, river)

object BrokenPeaksSite extends Site(SiteId("site:broken-peaks"), "Broken Peaks",
    defense = 2, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(4),
    startingResources = Tokens(0, 2),
    forgeRequirements = None,
    homeland = None):
  val mountain = PowerId("site.broken-peaks.mountain")
  val handlers: Vector[PowerId] = Vector(mountain)

object DesolateShoreSite extends Site(SiteId("site:desolate-shore"), "Desolate Shore",
    defense = 1, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(5),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val coast = PowerId("site.desolate-shore.coast")
  val handlers: Vector[PowerId] = Vector(coast)

object FairIsleSite extends Site(SiteId("site:fair-isle"), "Fair Isle",
    defense = 2, capacity = 2, relicSlots = 1,
    recoverDifficulty = Some(3),
    startingResources = Tokens(3, 0),
    forgeRequirements = None,
    homeland = None):
  val coast = PowerId("site.fair-isle.coast")
  val island = PowerId("site.fair-isle.island")
  val handlers: Vector[PowerId] = Vector(coast, island)

object GreenShoreSite extends Site(SiteId("site:green-shore"), "Green Shore",
    defense = 1, capacity = 2, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val coast = PowerId("site.green-shore.coast")
  val handlers: Vector[PowerId] = Vector(coast)

object HeadwatersSite extends Site(SiteId("site:headwaters"), "Headwaters",
    defense = 1, capacity = 2, relicSlots = 1,
    recoverDifficulty = Some(4),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val river = PowerId("site.headwaters.river")
  val mountain = PowerId("site.headwaters.mountain")
  val handlers: Vector[PowerId] = Vector(river, mountain)

object HiddenPlaceSite extends Site(SiteId("site:hidden-place"), "Hidden Place",
    defense = 2, capacity = 2, relicSlots = 1,
    recoverDifficulty = Some(4),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val mountain = PowerId("site.hidden-place.mountain")
  val handlers: Vector[PowerId] = Vector(mountain)

object MinesSite extends Site(SiteId("site:mines"), "Mines",
    defense = 1, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(5),
    startingResources = Tokens(3, 0),
    forgeRequirements = None,
    homeland = None):
  val mountain = PowerId("site.mines.mountain")
  val handlers: Vector[PowerId] = Vector(mountain)

object NarrowPassSite extends Site(SiteId("site:narrow-pass"), "Narrow Pass",
    defense = 2, capacity = 1, relicSlots = 1,
    recoverDifficulty = Some(5),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val pass = PowerId("site.narrow-pass.pass")
  val handlers: Vector[PowerId] = Vector(pass)

object RiverbankSite extends Site(SiteId("site:riverbank"), "Riverbank",
    defense = 1, capacity = 2, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val river = PowerId("site.riverbank.river")
  val handlers: Vector[PowerId] = Vector(river)

object RockyCoastSite extends Site(SiteId("site:rocky-coast"), "Rocky Coast",
    defense = 2, capacity = 1, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val coast = PowerId("site.rocky-coast.coast")
  val handlers: Vector[PowerId] = Vector(coast)

object SunkenIslesSite extends Site(SiteId("site:sunken-isles"), "Sunken Isles",
    defense = 2, capacity = 0, relicSlots = 3,
    recoverDifficulty = Some(6),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val coast = PowerId("site.sunken-isles.coast")
  val island = PowerId("site.sunken-isles.island")
  val handlers: Vector[PowerId] = Vector(coast, island)

object TidalMarshesSite extends Site(SiteId("site:tidal-marshes"), "Tidal Marshes",
    defense = 1, capacity = 2, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val river = PowerId("site.tidal-marshes.river")
  val coast = PowerId("site.tidal-marshes.coast")
  val handlers: Vector[PowerId] = Vector(river, coast)
