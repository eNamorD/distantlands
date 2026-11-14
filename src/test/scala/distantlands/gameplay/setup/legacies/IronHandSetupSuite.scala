package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture, SetupLegacyFixture, SetupPawnSites}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Iron Hand at setup: "you place one warband on all sites coming from the
  * Atlas until the Empire divider, and players may place their pawn at any
  * site." It always applies (spec ruling 7; R2, R4). */
class IronHandSetupSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")
  private val held = SiteForces.Occupied(ForceKind.Exile(l3), 1)

  private def started(chronicle: Chronicle,
      first: PlayerId = p1,
      seats: Vector[FirstGameParticipant] = LaterGameFixture.seats(3))
      : ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, first, seats)).fold(
      problem => fail(problem.toString), identity)

  private def holding(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.holding(chronicle, l3, Role.Exile,
      IronHandCard.id -> true)

  private def forces(ready: ReadyGame, record: Chronicle)
      : Vector[SiteForces] =
    record.world.map(stored => ready.game.current.map.sites(stored.site)
      .forces)

  test("the Empire's sites take the holder's warband, not purple"):
    val record = holding(LaterGameFixture.chronicle(l1, l2))
    val ready = started(record)
    assertEquals(forces(ready, record).distinct, Vector(held))
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Imperial) > 0), 0)

  test("every player may place their pawn at any site"):
    val ready = started(holding(LaterGameFixture.chronicle(l1, l2)))
    Vector("p1", "p2", "p3").foreach(player => assertEquals(
      SetupPawnSites.legal(catalog, ready, PlayerId(player)).toSet,
      ready.game.current.map.inPlay.toSet))

  test("with no Empire the former Empire's sites take the holder's " +
      "warband in place of bandits"):
    // No Chancellor and no Citizen seated: only p3 and p4 on l3 and l4.
    val record = holding(LaterGameFixture.chronicle(l1, l2))
    val ready = started(record, PlayerId("p3"),
      LaterGameFixture.seats(4).drop(2))
    assertEquals(forces(ready, record).distinct, Vector(held))
    Vector("p3", "p4").foreach(player => assertEquals(
      SetupPawnSites.legal(catalog, ready, PlayerId(player)).toSet,
      ready.game.current.map.inPlay.toSet))

  test("a dormant Iron Hand changes nothing (ruling 1)"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> false)
    val ready = started(record)
    assertEquals(forces(ready, record).distinct,
      Vector(SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("a site the holder's supply misses takes bandits (R2)"):
    // Circle of Swords' 8 on the board leave 6 of the 14 for 8 sites.
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector.empty, 8), l3, Role.Exile,
      IronHandCard.id -> true, CircleOfSwordsCard.id -> true)
    val ready = started(record)
    val all = forces(ready, record)
    assertEquals(all.take(6).distinct, Vector(held))
    assertEquals(all.drop(6), record.world.drop(6).map(stored =>
      val capacity = catalog.site(stored.site).get.capacity
      if capacity == 0 then SiteForces.Empty
      else SiteForces.Occupied(ForceKind.Bandit, capacity)))

  test("an exile-only game has no Empire divider, so no site takes the " +
      "holder's warband"):
    val record = holding(FirstGameSetupFixture.chronicle)
    val ready = started(record)
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Exile(l3)) > 0), 0)
