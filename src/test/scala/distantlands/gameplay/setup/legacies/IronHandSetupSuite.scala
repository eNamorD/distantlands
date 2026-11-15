package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture, SetupLegacyFixture, SetupPawnSites}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Iron Hand at setup: "If you were in the previous game, you place one
  * warband on all sites coming from the Atlas until the Empire divider,
  * and players may place their pawn at any site." Both clauses apply only
  * when the holder's lineage was in the previous game (spec ruling 7 as
  * amended by R39; R2, R4). */
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

  test("a first game played exile-only wipes a held Iron Hand, which does " +
      "nothing (R40)"):
    val record = holding(FirstGameSetupFixture.chronicle)
    val ready = started(record)
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Exile(l3)) > 0), 0)
    assert(!ready.game.campaign.lineages(l3).legacies.exists(
      _.id == IronHandCard.id))

  /** `holding`, the holder's lineage having sat out the previous game. */
  private def satOut(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.satOut(holding(chronicle), l3)

  /** The pawn sites `ready` offers each of `players`, by player. */
  private def pawnSites(ready: ReadyGame, players: String*)
      : Vector[Set[SiteId]] =
    players.toVector.map(player =>
      SetupPawnSites.legal(catalog, ready, PlayerId(player)).toSet)

  test("a holder who sat out the previous game places no warband: the " +
      "Empire's sites keep their purple warbands, and pawns their usual " +
      "sites (R39)"):
    val record = satOut(LaterGameFixture.chronicle(l1, l2))
    val ready = started(record)
    assertEquals(forces(ready, record).distinct,
      Vector(SiteForces.Occupied(ForceKind.Imperial, 1)))
    val usual = pawnSites(started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> false)), "p1", "p2", "p3")
    assertEquals(pawnSites(ready, "p1", "p2", "p3"), usual)
    assert(usual.exists(_ != ready.game.current.map.inPlay.toSet), usual)

  test("with no Empire, a holder who sat out the previous game leaves the " +
      "former Empire's sites to the bandits (R39)"):
    val record = satOut(LaterGameFixture.chronicle(l1, l2))
    val seats = LaterGameFixture.seats(4).drop(2)
    val ready = started(record, PlayerId("p3"), seats)
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Exile(l3)) > 0), 0)
    val usual = pawnSites(started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> false), PlayerId("p3"), seats), "p3", "p4")
    assertEquals(pawnSites(ready, "p3", "p4"), usual)

  test("a holder who sat out keeps Circle of Swords' warbands and Rival's " +
      "sites as without Iron Hand (R2, R39)"):
    val legacies = Vector(CircleOfSwordsCard.id -> true, RivalCard.id -> true)
    def record(ironHand: Boolean) = SetupLegacyFixture.satOut(
      SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2), l3,
        Role.Exile, (legacies :+ (IronHandCard.id -> ironHand))*), l3)
    val withIt = started(record(ironHand = true))
    val without = started(record(ironHand = false))
    assertEquals(withIt.game.current.map.sites, without.game.current.map.sites)
    assertEquals(withIt.game.current.player(PlayerId("p3")).get.board,
      without.game.current.player(PlayerId("p3")).get.board)
    assertEquals(withIt.banks, without.banks)
