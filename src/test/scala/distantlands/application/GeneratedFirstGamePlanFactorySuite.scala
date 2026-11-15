package distantlands.application

import distantlands.gameplay.powers.PowerImplementationStatus
import distantlands.gameplay.setup.{EmpireSetupRules, FirstGameRulesData,
  FirstGameSetupFixture, GameStartRules, LaterGameFixture, LegacyDeal}
import distantlands.model._
import distantlands.testkit.ChronicleShuffles
import distantlands.testkit.Unwrap.{accepted, present}

class GeneratedFirstGamePlanFactorySuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val config = FirstGameBootstrapConfig(FirstGameSetupFixture.participants,
    PlayerId("p2"))
  private val factory = new GeneratedFirstGamePlanFactory(catalog)

  test("implemented relics are still dealt first after setup: site relic " +
      "slots, then the relic deck"):
    val status = PowerImplementationStatus.implemented
    val relics = ImplementedCardCatalog.ordinaryRelics(catalog, status)
    def implementedFirst[A](cards: Vector[A], implemented: A => Boolean): Boolean =
      cards.dropWhile(implemented).forall(card => !implemented(card))

    val plan = accepted(factory.build(config, ChronicleChoice.New("Test Chronicle")))
    val current = accepted(
      GameStartRules.evolve(catalog, plan.chronicle, plan.orders)).game.current

    val dealtRelics = current.map.inPlay.flatMap(site =>
      current.map.sites(site).relics.map(_.id)) ++ current.commonCards.relicDeck
    assertEquals(dealtRelics, plan.orders.relicOrder)
    assert(implementedFirst(dealtRelics, relics), dealtRelics.toString)
    assert(current.map.inPlay.flatMap(current.map.sites(_).denizens).forall {
      case edifice: EdificeState =>
        val printed = present(catalog.edifices
          .find(_.id.value == edifice.id.value), edifice.id.value)
        ImplementedCardCatalog.homelandEdifice(catalog, printed.suit, status)
          .forall(_ == edifice.id)
      case _ => true
    }, "a Homeland in play carries its suit's implemented edifice")

  test("a new Chronicle carries no history and starts a first game"):
    val plan = accepted(factory.build(config, ChronicleChoice.New("Fresh")))
    assert(!EmpireSetupRules.hasHistory(catalog, plan.chronicle))
    val ready = accepted(GameStartRules.evolve(catalog, plan.chronicle,
      plan.orders))
    assert(ready.setup.ignoreCitizenshipPowers)
    assertEquals(ready.game.current.map.inPlay,
      plan.chronicle.atlasBox.take(8).map(_.site))

  test("seating order and first player are shuffled, keeping every participant"):
    val plans = Vector.fill(30)(accepted(factory.build(config, ChronicleChoice.New("Test Chronicle"))).resolvedConfig)
    plans.foreach { resolved =>
      assertEquals(resolved.participants.toSet, config.participants.toSet)
      assertEquals(resolved.firstPlayer, resolved.participants.head.playerId)
    }
    assert(plans.map(_.firstPlayer).distinct.size > 1,
      "the first player should vary across generated plans")
    assert(plans.map(_.participants).distinct.size > 1,
      "the seating order should vary across generated plans")

  test("a new Chronicle carries its name, and a blank name is refused"):
    assertEquals(accepted(factory.build(config,
      ChronicleChoice.New("Ashen Crown"))).chronicle.name, "Ashen Crown")
    assertEquals(factory.build(config, ChronicleChoice.New("")).left.map(
      _.message), Left("a Chronicle needs a name"))

  test("a saved Chronicle is used as it is, with the seating shuffled"):
    val saved = FirstGameSetupFixture.chronicle
    val plan = accepted(factory.build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.chronicle, saved)
    assertEquals(plan.resolvedConfig.participants.toSet,
      config.participants.toSet)

  test("the deal is shuffled from the Chronicle's decks, not their order"):
    val saved = FirstGameSetupFixture.chronicle
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog,
      ChronicleShuffles.reversing).build(config, ChronicleChoice.Saved(saved)))
    assertNotEquals(plan.orders.denizenOrder, saved.worldDeck)
    assertEquals(plan.orders.denizenOrder.sortBy(_.value),
      saved.worldDeck.sortBy(_.value))
    assertNotEquals(plan.orders.relicOrder, saved.relicDeck)
    assertEquals(plan.orders.relicOrder.sortBy(_.value),
      saved.relicDeck.sortBy(_.value))
    val legacies = LegacyDeal.deck(catalog, saved)
    assertNotEquals(plan.orders.legacyOrder, legacies)
    assertEquals(plan.orders.legacyOrder.sortBy(_.value),
      legacies.sortBy(_.value))
    assertEquals(plan.chronicle, saved)

  test("a first game played exile-only deals its stored legacies back in " +
      "the legacy deck, and the game starts from that deal (R40)"):
    val stored = catalog.legacies.map(_.id).take(2)
    val saved = FirstGameSetupFixture.chronicle.copy(lineages = Vector(
      LineageState(FirstGameSetupFixture.participants.head.lineageId, None,
        Role.Exile, stored.map(LegacyState(_, active = true)), Vector.empty),
      LineageState(LineageId("boxed"), None, Role.Exile, Vector.empty,
        Vector.empty)))
    val plan = accepted(factory.build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.chronicle, saved)
    assertEquals(plan.orders.legacyOrder.sortBy(_.value),
      catalog.legacies.map(_.id).sortBy(_.value))
    accepted(GameStartRules.evolve(catalog, plan.chronicle, plan.orders))

  test("a Teeming World Chronicle is dealt a world deck of the five " +
      "Visions, shuffled, and the game starts from that deal"):
    val saved = FirstGameSetupFixture.chronicle.copy(foundations = Map(
      FoundationNumber.IV -> FoundationState(FoundationFace.Altered,
        Set.empty)))
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog,
      ChronicleShuffles.reversing).build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.orders.worldDeckOrder,
      (FirstGameRulesData.visions: Vector[WorldCardId]).reverse)
    val current = accepted(GameStartRules.evolve(catalog, plan.chronicle,
      plan.orders)).game.current
    assertEquals(current.commonCards.worldDeck, plan.orders.worldDeckOrder)
    assertEquals(current.players.map(_.advisers.size), Vector(3, 3, 3))

  private def lineage(id: String, role: Role) =
    LineageState(LineageId(id), None, role, Vector.empty, Vector.empty)

  private def seatedFirst(lineages: Vector[LineageState],
      random: distantlands.application.ChronicleRandomPort) =
    accepted(new GeneratedFirstGamePlanFactory(catalog, random).build(config,
      ChronicleChoice.Saved(FirstGameSetupFixture.chronicle.copy(
        lineages = lineages)))).resolvedConfig.firstPlayer

  test("the seated Chancellor goes first in a later game"):
    assertEquals(seatedFirst(Vector(lineage("l1", Role.Citizen),
      lineage("l2", Role.Chancellor)), ChronicleShuffles.unshuffled),
      PlayerId("p2"))

  test("with the Chancellor's lineage absent, a random seated Citizen goes " +
      "first"):
    // l9 is the Chancellor and not seated; l1 and l3 are Citizens. The
    // seats stay in order, and the port's pick is the head of its shuffle
    // of the seated Citizens.
    val citizens = Vector(lineage("l9", Role.Chancellor),
      lineage("l1", Role.Citizen), lineage("l3", Role.Citizen))
    val reversingCitizens = new distantlands.application.ChronicleRandomPort:
      def shuffle[A](values: Vector[A]): Vector[A] =
        if values.size == 2 then values.reverse else values
    assertEquals(seatedFirst(citizens, ChronicleShuffles.unshuffled),
      PlayerId("p1"))
    assertEquals(seatedFirst(citizens, reversingCitizens), PlayerId("p3"))

  test("a Chronicle that lists no Chancellor still sends a random seated " +
      "Citizen first"):
    val citizens = Vector(lineage("l1", Role.Citizen),
      lineage("l3", Role.Citizen))
    val reversingCitizens = new distantlands.application.ChronicleRandomPort:
      def shuffle[A](values: Vector[A]): Vector[A] =
        if values.size == 2 then values.reverse else values
    assertEquals(seatedFirst(citizens, ChronicleShuffles.unshuffled),
      PlayerId("p1"))
    assertEquals(seatedFirst(citizens, reversingCitizens), PlayerId("p3"))

  test("with no Chancellor and no Citizen seated the first seat of the " +
      "shuffle goes first, and the game starts with no Empire"):
    // l9 is the Chancellor and l8 a Citizen; p1 to p3 play l1 to l3.
    val saved = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog,
      ChronicleShuffles.reversing).build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.resolvedConfig.participants,
      FirstGameSetupFixture.participants.reverse)
    assertEquals(plan.resolvedConfig.firstPlayer, PlayerId("p3"))
    val ready = accepted(GameStartRules.evolve(catalog, plan.chronicle,
      plan.orders))
    assertEquals(Imperials.players(ready), Vector.empty)
    assertEquals(ready.game.current.turn.activePlayer, PlayerId("p3"))
    assertEquals(ready.game.current.map.inPlay, saved.world.map(_.site))
