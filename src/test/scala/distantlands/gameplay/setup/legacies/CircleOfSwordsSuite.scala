package distantlands.gameplay.setup.legacies

import distantlands.gameplay.powers.PowerImplementationStatus
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture, StartingLegacies}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Circle of Swords at game start: "You gain eight starting warbands
  * (instead of three). If you're Imperial and other Imperials would gain
  * fewer than three, you gain less." (spec ruling 5; R2, R3). */
class CircleOfSwordsSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  private def board(ready: ReadyGame, player: String): Int =
    ready.game.current.player(PlayerId(player)).get.board.warbands

  test("an Exile holder starts with 8 warbands, the others with 3"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> true))
    assertEquals(board(ready, "p3"), CircleOfSwords.warbands)
    assertEquals(board(ready, "p1"), 3)
    assertEquals(board(ready, "p2"), 3)

  test("a dormant Circle of Swords changes nothing (ruling 1)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> false))
    assertEquals(board(ready, "p3"), 3)

  test("an Imperial holder takes 8 purple and the Empire's sites share " +
      "the rest, Cradlemost first (R3)"):
    // Three Imperial boards: 3 + 3 + 8 = 14 of the 24 purple warbands,
    // so an Empire of 8 sites has 10 left: every site takes one.
    val chronicle = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector(l2, l3), 8), l3, Role.Citizen,
      CircleOfSwordsCard.id -> true)
    val ready = started(chronicle)
    assertEquals(board(ready, "p3"), 8)
    val purple = ready.game.current.map.sites.values
      .count(_.forces == SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(purple, 8)

  test("an Imperial holder takes less when the others would get fewer " +
      "than three (R3)"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2, l3), 2)
    val ready = started(SetupLegacyFixture.holding(record, l1,
      Role.Chancellor, CircleOfSwordsCard.id -> true))
    assertEquals(board(ready, "p1"), 8)
    assertEquals(StartingLegacies.imperialBoard(others = 5), 8)
    assertEquals(StartingLegacies.imperialBoard(others = 6), 6)
    assertEquals(StartingLegacies.imperialBoard(others = 7), 3)

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(CircleOfSwords))
    assert(PowerImplementationStatus.implemented(CircleOfSwordsCard.power.id))
