package oathdigital.gameplay.cards

import oathdigital.catalog.Site
import oathdigital.gameplay.actions.RuleNotes
import oathdigital.model.{PowerId, Suit, SiteId, Tokens}

/** The eleven sites whose power no power object runs. The six Homelands'
  * `homeland-<suit>` handlers are card play's Homeland rule,
  * `CardPlayRules.homeland`, which writes its line as
  * `RuleNotes.homelandDiscard`; the five Plains' `plains` handlers print
  * "This site has no power". Both play correctly with nothing registered,
  * so `PowerImplementationStatus` counts `handlers` implemented from this
  * list.
  */
object HomelandsAndPlains:
  val homelands: Vector[Site] = Vector(DeepWoodsSite, GoldenValleySite,
    GreatSlumSite, PaintedTowersSite, StandingStonesSite, SteppeSite)
  val plains: Vector[Site] = Vector(BuriedGiantSite, DunesSite,
    SaltFlatsSite, ShroudedWoodsSite, SolitaryPillarSite)
  /** The rule that runs every Homeland handler. */
  val homelandRule: PowerId = RuleNotes.homelandDiscard
  val handlers: Set[PowerId] = (homelands ++ plains).flatMap(_.handlers).toSet

object BuriedGiantSite extends Site(SiteId("site:buried-giant"), "Buried Giant",
    defense = 1, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(6),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val plains = PowerId("site.buried-giant.plains")
  val handlers: Vector[PowerId] = Vector(plains)

object DeepWoodsSite extends Site(SiteId("site:deep-woods"), "Deep Woods",
    defense = 1, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(1, 2)),
    homeland = Some(Suit.Beast)):
  val homelandPower = PowerId("site.deep-woods.homeland-beast")
  val handlers: Vector[PowerId] = Vector(homelandPower)

object DunesSite extends Site(SiteId("site:dunes"), "Dunes",
    defense = 1, capacity = 1, relicSlots = 1,
    recoverDifficulty = Some(4),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val plains = PowerId("site.dunes.plains")
  val handlers: Vector[PowerId] = Vector(plains)

object GoldenValleySite extends Site(SiteId("site:golden-valley"), "Golden Valley",
    defense = 1, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(3, 0)),
    homeland = Some(Suit.Hearth)):
  val homelandPower = PowerId("site.golden-valley.homeland-hearth")
  val handlers: Vector[PowerId] = Vector(homelandPower)

object GreatSlumSite extends Site(SiteId("site:great-slum"), "Great Slum",
    defense = 1, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(2, 1)),
    homeland = Some(Suit.Discord)):
  val homelandPower = PowerId("site.great-slum.homeland-discord")
  val handlers: Vector[PowerId] = Vector(homelandPower)

object PaintedTowersSite extends Site(SiteId("site:painted-towers"), "Painted Towers",
    defense = 1, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(2, 1)),
    homeland = Some(Suit.Order)):
  val homelandPower = PowerId("site.painted-towers.homeland-order")
  val handlers: Vector[PowerId] = Vector(homelandPower)

object SaltFlatsSite extends Site(SiteId("site:salt-flats"), "Salt Flats",
    defense = 0, capacity = 2, relicSlots = 1,
    recoverDifficulty = Some(2),
    startingResources = Tokens(0, 0),
    forgeRequirements = None,
    homeland = None):
  val plains = PowerId("site.salt-flats.plains")
  val handlers: Vector[PowerId] = Vector(plains)

object ShroudedWoodsSite extends Site(SiteId("site:shrouded-woods"), "Shrouded Woods",
    defense = 2, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(5),
    startingResources = Tokens(1, 1),
    forgeRequirements = None,
    homeland = None):
  val plains = PowerId("site.shrouded-woods.plains")
  val handlers: Vector[PowerId] = Vector(plains)

object SolitaryPillarSite extends Site(SiteId("site:solitary-pillar"), "Solitary Pillar",
    defense = 1, capacity = 1, relicSlots = 2,
    recoverDifficulty = Some(6),
    startingResources = Tokens(0, 2),
    forgeRequirements = None,
    homeland = None):
  val plains = PowerId("site.solitary-pillar.plains")
  val handlers: Vector[PowerId] = Vector(plains)

object StandingStonesSite extends Site(SiteId("site:standing-stones"), "Standing Stones",
    defense = 1, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(0, 3)),
    homeland = Some(Suit.Arcane)):
  val homelandPower = PowerId("site.standing-stones.homeland-arcane")
  val handlers: Vector[PowerId] = Vector(homelandPower)

object SteppeSite extends Site(SiteId("site:steppe"), "Steppe",
    defense = 0, capacity = 3, relicSlots = 0,
    recoverDifficulty = None,
    startingResources = Tokens(0, 0),
    forgeRequirements = Some(Tokens(0, 3)),
    homeland = Some(Suit.Nomad)):
  val homelandPower = PowerId("site.steppe.homeland-nomad")
  val handlers: Vector[PowerId] = Vector(homelandPower)
