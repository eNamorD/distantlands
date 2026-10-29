package oathdigital.gameplay.setup

import oathdigital.model._
import oathdigital.testkit.CatalogNames
import oathdigital.testkit.Unwrap.{accepted, as, rejected}

class GameStartRulesSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders

  test("a valid Chronicle evolves into a Ready game in Phase.Setup with the " +
      "first player active and no pawns or advisers"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(ready.game.current.turn.phase, Phase.Setup)
    assertEquals(ready.game.current.turn.activePlayer, orders.firstPlayer)
    assertEquals(ready.game.current.players.map(player =>
      (player.pawnSite, player.advisers)),
      Vector.fill(orders.participants.size)((None, Vector.empty)))

  test("a started game deals each participant 3 temporary cards"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    // The 3 denizens each player chooses an adviser from during Setup.
    assert(orders.participants.forall(p =>
      ready.game.current.temporaryHands.getOrElse(p.playerId, Vector.empty).size == 3))

  test("an edifice stored at an atlas site beyond the 8 in play stays in the " +
      "atlas and is not also in the edifice deck"):
    // Great Slum is not one of the Chronicle's 8 sites, and Hall of Debate,
    // its Homeland edifice, is stored at none of them.
    val site = CatalogNames.site("Great Slum")
    val edifice = CatalogNames.edifice("Hall of Debate")
    assert(!chronicle.atlasBox.exists(_.site == site))
    assert(!chronicle.atlasBox.exists(_.items.contains(edifice)))
    assertEquals(catalog.sites.find(_.id == site).flatMap(_.homeland),
      catalog.edifices.find(_.id.value == edifice.value).map(_.suit))
    val withStorage = chronicle.copy(
      atlasBox = chronicle.atlasBox :+ StoredSite(site, Vector(edifice)))
    val ready = accepted(GameStartRules.evolve(catalog, withStorage, orders))
    assert(ready.game.campaign.atlas.entries.contains(
      AtlasEntry.StoredSite(site, Vector.empty, Vector.empty, Some(edifice))))
    assert(!ready.game.current.commonCards.edificeDeck.contains(edifice))

  test("game start keeps the whole Chronicle, including lineages of colors " +
      "that did not play"):
    val absent = LineageState(LineageId("l9"), None, Role.Exile, Vector.empty,
      Vector.empty)
    val withAbsentLineage = chronicle.copy(lineages = Vector(absent))
    val ready = accepted(GameStartRules.evolve(catalog, withAbsentLineage, orders))
    assertEquals(ready.game.campaign.baseline, withAbsentLineage)
    assertEquals(ready.game.campaign.evolved, None)

  test("game start refuses a lineage's starting relics until Legacies"):
    val relic = chronicle.relicDeck.head
    val keeping = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(LineageState(LineageId("l9"), None, Role.Exile,
        Vector.empty, Vector.empty, startingRelics = Vector(relic))))
    assertEquals(GameStartRules.evolve(catalog, keeping, orders),
      Left(OathViolation.UnsupportedChronicle(
        "starting relics require Legacies")))

  test("game start takes the era and Oathkeeper goal from the Chronicle"):
    val l9 = LineageId("l9")
    val scored = chronicle.copy(
      era = EraState(15, Vector(EraGame(Vector(
        EraEntry(l9, 6, Role.Citizen))))),
      oathkeeperGoal = OathkeeperGoal.Protection)
    val campaign = accepted(GameStartRules.evolve(catalog, scored, orders))
      .game.campaign
    assertEquals(campaign.oathkeeperGoal, OathkeeperGoal.Protection)
    assertEquals(campaign.era, scored.era)
    assertEquals(campaign.era.score(l9), 6)
    assertEquals(orders.participants.map(p => campaign.era.score(p.lineageId)),
      Vector.fill(orders.participants.size)(0))

  test("setup deals from the recorded deal, not the Chronicle's order"):
    val swapped = orders.denizenOrder(1) +: orders.denizenOrder(0) +:
      orders.denizenOrder.drop(2)
    val current = accepted(GameStartRules.evolve(catalog, chronicle,
      orders.copy(denizenOrder = swapped))).game.current
    assertEquals(current.commonCards.discard(Region.Cradle), swapped.take(2))
    assertNotEquals(swapped.take(2), chronicle.worldDeck.take(2))

  test("a deal that does not hold the Chronicle's decks is refused"):
    def refusal(dealt: SetupOrders): OathViolation =
      rejected(GameStartRules.evolve(catalog, chronicle, dealt))
    assertEquals(refusal(orders.copy(denizenOrder = orders.denizenOrder.tail)),
      OathViolation.UnsupportedChronicle(
        "the deal does not hold the Chronicle's world deck"))
    assertEquals(refusal(orders.copy(relicOrder = orders.relicOrder.tail)),
      OathViolation.UnsupportedChronicle(
        "the deal does not hold the Chronicle's relic deck"))
    assertEquals(refusal(orders.copy(worldDeckOrder =
        orders.worldDeckOrder.reverse)),
      OathViolation.UnsupportedChronicle(
        "the world deck is not the rest of the deal"))

  test("a non-empty world is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val withWorld = chronicle.copy(world = chronicle.atlasBox.take(1))
    as[OathViolation.UnsupportedChronicle](
      rejected(GameStartRules.evolve(catalog, withWorld, orders)))

  test("a stored denizen on an atlas site is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val denizen = chronicle.worldDeck.head
    val polluted = chronicle.copy(atlasBox = chronicle.atlasBox.updated(
      0, chronicle.atlasBox.head.copy(items = Vector(denizen))))
    val result = GameStartRules.evolve(catalog, polluted, orders)
    assertEquals(result.left.toOption, Some(OathViolation.UnsupportedChronicle(
      "stored denizens or relics on an atlas site are not supported yet")))

  test("too few atlas sites is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val short = chronicle.copy(atlasBox = chronicle.atlasBox.take(7))
    val result = GameStartRules.evolve(catalog, short, orders)
    assertEquals(result, Left(OathViolation.UnsupportedChronicle(
      "at least 8 atlas sites are required, got 7")))

  test("an unknown denizen id is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val polluted = chronicle.copy(
      worldDeck = chronicle.worldDeck.updated(0, DenizenId("no-such-denizen")))
    assertEquals(GameStartRules.evolve(catalog, polluted, orders).left.toOption,
      Some(OathViolation.UnsupportedChronicle("unknown denizen no-such-denizen")))
