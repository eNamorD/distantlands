package distantlands.gameplay.setup

import distantlands.model._
import distantlands.testkit.Situation
import distantlands.testkit.Unwrap.accepted

/** Game start under Wide Horizons, the altered Foundation I, with an Empire
  * (Foundations design, slice 3): the Empire's sites return and no slot
  * past them is filled. l1 is the seated Chancellor, l2 a Citizen and l3
  * an Exile; p1 goes first. Every test starts from a Chronicle: game start
  * is the subject. */
class WideHorizonsSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")
  private val wideHorizons = FoundationState(FoundationFace.Altered, Set.empty)

  /** A later game's Chronicle whose Empire holds `empireSites` sites, each
    * storing a denizen, with Foundation I on Wide Horizons. */
  private def wideHorizonsChronicle(empireSites: Int): Chronicle =
    LaterGameFixture.chronicle(LineageId("l1"), Vector(LineageId("l2")),
      empireSites).copy(
      foundations = Map(FoundationNumber.I -> wideHorizons))

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  test("under Wide Horizons the Empire's sites return as under Imperial " +
      "Maps, whose game start also leaves the slots past them empty"):
    val record = wideHorizonsChronicle(empireSites = 3)
    val wide = started(record).game
    val imperialMaps = started(record.copy(foundations = Map.empty)).game
    val map = wide.current.map
    assertEquals(map.hinterward, record.world.map(_.site))
    assertEquals(Region.all.map(map.sitesIn(_).size), Vector(2, 1, 0))
    assertEquals(map.nextSlot, Some(Region.Provinces))
    assertEquals(map.hintermost, Some(record.world.last.site))
    assertEquals(map.sites.keySet, record.world.map(_.site).toSet)
    record.world.foreach(stored => assertEquals(map.sites(stored.site),
      imperialMaps.current.map.sites(stored.site)))
    // Imperial Maps fills them in Setup's walk (ImperialMapsSetupSuite).
    assertEquals(imperialMaps.current.map, map)
    assertEquals(wide.campaign.foundations(FoundationNumber.I), wideHorizons)

  test("the atlas keeps every atlas site in order with its stored cards, " +
      "and no relic leaves the relic deck"):
    val record = wideHorizonsChronicle(empireSites = 3)
    val game = started(record).game
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, record.atlasBox.map(_.site))
    val atlasEdifices = record.atlasBox.flatMap(_.items)
      .collect { case id: EdificeId => id }
    assert(atlasEdifices.nonEmpty)
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.edifice
    }.flatten, atlasEdifices)
    assert(!game.current.commonCards.edificeDeck.exists(atlasEdifices.contains))
    // An Empire site fills no relic slot, and no other site is placed.
    assertEquals(game.current.commonCards.relicDeck, record.relicDeck)
    assert(game.cardIndex.isRight)

  test("each favor bank adds one per denizen on the map, and none for a " +
      "denizen left in the atlas"):
    val record = wideHorizonsChronicle(empireSites = 2)
    val ready = started(record)
    val onMap = record.world.flatMap(_.items).flatMap {
      case id: EdificeId => catalog.edifice(id).map(_.suit)
      case id: DenizenId => catalog.denizen(id).map(_.suit)
      case _ => None
    }
    assertEquals(onMap.size, 2)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> (3 + onMap.count(_ == suit))).toMap)

  test("an Empire of eight sites leaves no empty slot"):
    val map = started(wideHorizonsChronicle(empireSites = 8)).game.current.map
    assertEquals(map.inPlay.size, 8)
    assertEquals(map.nextSlot, None)

  test("on a one-site map Setup runs to the Chancellor's first turn with " +
      "every pawn at that site"):
    val record = wideHorizonsChronicle(empireSites = 1)
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p1))
    val current = woken.ready.game.current
    assertEquals(current.map.inPlay, record.world.map(_.site))
    assertEquals(current.players.map(_.pawnSite),
      Vector.fill(3)(Some(record.world.head.site)))
    assertEquals(current.turn.activePlayer, p1)
    assert(Set[Phase](Phase.Wake, Phase.Act)(current.turn.phase))

  test("a Chronicle with no Empire that stores Wide Horizons starts an " +
      "exile-only game, which places eight sites and keeps the face"):
    val exileOnly = FirstGameSetupFixture.chronicle.copy(
      foundations = Map(FoundationNumber.I -> wideHorizons))
    val game = accepted(GameStartRules.evolve(catalog, exileOnly,
      FirstGameSetupFixture.orders)).game
    assertEquals(game.current.map.inPlay,
      exileOnly.atlasBox.take(8).map(_.site))
    assertEquals(game.current.map.nextSlot, None)
    assertEquals(game.campaign.foundations(FoundationNumber.I), wideHorizons)
