package distantlands.application

import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture,
  LegacyDeal, SetupDeal}
import distantlands.model._

class ChronicleFirstGamePlanSuite extends munit.FunSuite:
  private val config = FirstGameBootstrapConfig(
    FirstGameSetupFixture.participants, PlayerId("p2"))
  private val chronicle = FirstGameSetupFixture.chronicle

  test("dealing the fixture's equivalent Chronicle reproduces its exact deal order"):
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      chronicle.worldDeck, chronicle.relicDeck,
      LegacyDeal.deck(FirstGameSetupFixture.catalog, chronicle))
    assertEquals(orders, FirstGameSetupFixture.orders)

  test("dealOrder is total even when the Chronicle is too small for GameStartRules"):
    val short = chronicle.copy(atlasBox = chronicle.atlasBox.take(7))
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      short.worldDeck, short.relicDeck,
      LegacyDeal.deck(FirstGameSetupFixture.catalog, short))
    assertEquals(orders.participants, config.participants)
    assert(distantlands.gameplay.setup.GameStartRules
      .evolve(FirstGameSetupFixture.catalog, short, orders).isLeft)

  private val Vector(p1, p2, p3) = config.participants.map(_.playerId)

  test("a later game deals hands only to players with no starting advisers"):
    // 3 seats, one with a stored adviser: 6 discards and 2 hands of 3.
    val denizens = chronicle.worldDeck
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal(FoundationFace.Normal, Vector(p1 -> 3, p2 -> 0, p3 -> 3)),
      denizens, chronicle.relicDeck, Vector.empty, identity)
    assertEquals(orders.worldDeckOrder.collect { case id: DenizenId => id },
      denizens.drop(6 + 2 * 3))

  test("a deck of fewer than 25 denizens after the deal mixes all five " +
      "Visions through it"):
    val short = chronicle.worldDeck.take(6 + 3 * 3 + 20)
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal.firstGame(config.participants), short, chronicle.relicDeck,
      Vector.empty, _.reverse)
    val rest: Vector[WorldCardId] = short.drop(15)
    assertEquals(orders.worldDeckOrder,
      (rest ++ FirstGameRulesData.visions).reverse)

  test("a Teeming World deal leaves the world deck the five Visions, " +
      "mixed, and keeps the whole stack as the deal"):
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal(FoundationFace.Altered, Vector(p1 -> 3, p2 -> 3, p3 -> 3)),
      chronicle.worldDeck, chronicle.relicDeck, Vector.empty, _.reverse)
    assertEquals(orders.worldDeckOrder,
      (FirstGameRulesData.visions: Vector[WorldCardId]).reverse)
    assertEquals(orders.denizenOrder, chronicle.worldDeck)
