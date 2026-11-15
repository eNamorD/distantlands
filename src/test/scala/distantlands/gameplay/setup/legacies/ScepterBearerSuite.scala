package distantlands.gameplay.setup.legacies

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.PowerImplementationStatus
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Scepter Bearer: "If you are an Imperial: Setup: You start with the
  * Grand Scepter (instead of the Chancellor)." (spec ruling 6). */
class ScepterBearerSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  private def scepter(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.filter(_.relics.exists(
      _.id == TheGrandScepterCard.id)).map(_.player)

  test("a Citizen holder starts with the Grand Scepter, the Chancellor " +
      "keeps the title"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(PlayerId("p2")))
    assertEquals(ready.game.current.title.holder, Some(p1))

  test("the Chancellor holding it changes nothing"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l1, Role.Chancellor,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(p1))

  test("an Exile holder changes nothing (ruling 2)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(p1))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(ScepterBearer))
    assert(PowerImplementationStatus.implemented(ScepterBearerCard.power.id))
