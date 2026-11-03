package oathdigital.gameplay.setup

import oathdigital.model._
import oathdigital.testkit.CatalogNames
import oathdigital.testkit.Unwrap.accepted

/** A later game's table, built by `GameStartRules` from a Chronicle with an
  * Empire: l1 is the seated Chancellor and p1 goes first. */
class LaterGameSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")
  private val l1 = LineageId("l1")
  private val record = LaterGameFixture.chronicle(l1)

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def relicSlots(site: SiteId): Int =
    catalog.site(site).get.relicSlots

  private def faceDown(relic: RelicId) =
    RelicState(relic, Orientation.FaceDown, Tokens.empty)

  /** `record` with `relic` taken from the relic deck and `denizen`, if any,
    * from the world deck. */
  private def withoutDeckCards(relic: RelicId,
      denizen: Option[DenizenId] = None): Chronicle =
    record.copy(relicDeck = record.relicDeck.filterNot(_ == relic),
      worldDeck = record.worldDeck.filterNot(denizen.contains))

  test("the Empire's sites fill the first slots Hinterward, each with one " +
      "Imperial warband, intact edifices and faceup denizens"):
    val map = started(record).game.current.map
    assertEquals(map.hinterward.take(2), record.world.map(_.site))
    record.world.foreach { stored =>
      val site = map.sites(stored.site)
      assertEquals(site.forces, SiteForces.Occupied(ForceKind.Imperial, 1))
      assertEquals(site.denizens.map(_.id), stored.items)
      assert(site.denizens.forall {
        case EdificeState(_, side, _) => side == EdificeSide.Intact
        case DenizenState(_, orientation, _) =>
          orientation == Orientation.FaceUp
      })
    }

  test("an Empire site keeps its stored relics facedown and fills no empty " +
      "slot"):
    val relic = record.relicDeck.head
    val empireSiteWithOneRelic = record.world.head.copy(
      items = record.world.head.items.take(2) :+ relic)
    val chronicle = withoutDeckCards(relic).copy(
      world = empireSiteWithOneRelic +: record.world.tail)
    val ready = started(chronicle)
    assertEquals(ready.game.current.map.sites(empireSiteWithOneRelic.site)
      .relics, Vector(faceDown(relic)))
    assertEquals(ready.game.current.map.sites(record.world(1).site).relics,
      Vector.empty)

  test("a remaining site keeps its stored cards, ruins its edifice and " +
      "fills only its empty relic slots (R27)"):
    val atlasSiteWithEdifice = record.atlasBox.find(stored =>
      stored.items.exists(_.isInstanceOf[EdificeId])).get
    val edifice = atlasSiteWithEdifice.items.collectFirst {
      case id: EdificeId => id }.get
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val stocked = atlasSiteWithEdifice.copy(
      items = Vector(edifice, denizen, relic))
    val chronicle = withoutDeckCards(relic, Some(denizen)).copy(
      atlasBox = record.atlasBox.map(stored =>
        if stored.site == stocked.site then stocked else stored))
    val site = started(chronicle).game.current.map.sites(stocked.site)
    val capacity = catalog.site(stocked.site).get.capacity
    assertEquals(site.forces, SiteForces.Occupied(ForceKind.Bandit, capacity))
    assertEquals(site.denizens, Vector(
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))
    assertEquals(site.relics.size, math.max(1, relicSlots(stocked.site)))
    assertEquals(site.relics.head, faceDown(relic))

  test("the edifice deck holds no edifice stored at any site"):
    val stored = (record.world ++ record.atlasBox).flatMap(_.items)
      .collect { case id: EdificeId => id }
    assert(stored.nonEmpty)
    assert(!started(record).game.current.commonCards.edificeDeck
      .exists(stored.contains))

  test("each favor bank adds one per denizen of its suit on the map"):
    val ready = started(record)
    val onMap = ready.game.current.map.sites.values.flatMap(_.denizens)
      .flatMap {
        case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
        case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
      }.toVector
    assert(onMap.size > 2, "the Empire's denizens are on the map")
    Suit.all.foreach(suit => assertEquals(ready.banks.favor(suit),
      3 + onMap.count(_ == suit)))

  test("the atlas keeps the unused sites with their stored cards"):
    // Great Slum is not one of the Chronicle's sites, and Hall of Debate is
    // stored at none of them.
    val unused = CatalogNames.site("Great Slum")
    val edifice = CatalogNames.edifice("Hall of Debate")
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val chronicle = withoutDeckCards(relic, Some(denizen)).copy(
      atlasBox = record.atlasBox :+ StoredSite(unused,
        Vector(edifice, denizen, relic)))
    assert(started(chronicle).game.campaign.atlas.entries.contains(
      AtlasEntry.StoredSite(unused,
        Vector(DenizenState(denizen, Orientation.FaceUp, Tokens.empty)),
        Vector(faceDown(relic)), Some(edifice))))
