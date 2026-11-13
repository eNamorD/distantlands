package distantlands.gameplay.setup

import distantlands.application.{ChronicleFirstGamePlan, FirstGameBootstrapConfig}
import distantlands.catalog.holding.{ArbiterCard, SeerCard}
import distantlands.gameplay.cards.GreatSlumSite
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.model._
import distantlands.testkit.Unwrap.{accepted, rejected}

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
    val site = GreatSlumSite.id
    val edifice = HallOfDebateCard.id
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

  test("a seated lineage's starting relics go facedown to its player"):
    val participant = orders.participants.head
    val relic = chronicle.relicDeck.head
    val keeping = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(LineageState(participant.lineageId, None, Role.Exile,
        Vector.empty, Vector.empty, startingRelics = Vector(relic))))
    val ready = accepted(GameStartRules.evolve(catalog, keeping, orders.copy(
      relicOrder = orders.relicOrder.filterNot(_ == relic))))
    assertEquals(ready.game.current.player(participant.playerId).get.relics
      .filter(_.id == relic),
      Vector(RelicState(relic, Orientation.FaceDown, Tokens.empty)))
    assertEquals(
      ready.game.campaign.lineages(participant.lineageId).startingRelics,
      Vector.empty)

  test("a seated lineage the Chronicle lists starts from its record, and an " +
      "unlisted one starts as a fresh Exile"):
    val Vector(listed, unlisted) = orders.participants.take(2).map(_.lineageId)
    val record = LineageState(listed, Some(PlayerId("p9")), Role.Exile,
      Vector.empty, Vector.empty)
    val ready = accepted(GameStartRules.evolve(catalog,
      chronicle.copy(lineages = Vector(record)), orders))
    assertEquals(ready.game.campaign.lineages(listed), record)
    assertEquals(ready.game.campaign.lineages(unlisted), LineageState(unlisted,
      None, Role.Exile, Vector.empty, Vector.empty))

  test("a seated lineage's legacies start with their stored faces"):
    val seated = orders.participants.head.lineageId
    val legacies = Vector(LegacyState(SeerCard.id, active = true),
      LegacyState(ArbiterCard.id, active = false))
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated, None,
      Role.Exile, legacies, Vector.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, keeping, orders))
    assertEquals(ready.game.campaign.lineages(seated).legacies, legacies)

  private val altered = FoundationState(FoundationFace.Altered, Set.empty)
  private val normal = FoundationState(FoundationFace.Normal, Set.empty)

  test("a Chronicle that lists no foundation starts with six normal faces"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(ready.game.campaign.foundations,
      FoundationNumber.all.map(_ -> normal).toMap)

  test("game start keeps the Chronicle's foundation faces and their " +
      "sources, and a number it does not list lies normal"):
    val sourced = FoundationState(FoundationFace.Altered,
      Set(LegacyId(catalog.legacies.head.id.value)))
    val stored = chronicle.copy(foundations = Map(
      FoundationNumber.III -> sourced, FoundationNumber.V -> normal))
    val ready = accepted(GameStartRules.evolve(catalog, stored, orders))
    assertEquals(ready.game.campaign.foundations,
      FoundationNumber.all.map(_ -> normal).toMap
        .updated(FoundationNumber.III, sourced))

  test("game start refuses a foundation whose alteration source is no " +
      "legacy it knows, and starts with one it knows"):
    def sourcedBy(legacy: LegacyId): Chronicle = chronicle.copy(
      foundations = Map(FoundationNumber.III ->
        FoundationState(FoundationFace.Altered, Set(legacy))))
    assertEquals(rejected(GameStartRules.evolve(catalog,
      sourcedBy(LegacyId("no-such-legacy")), orders)),
      OathViolation.UnsupportedChronicle("unknown legacy no-such-legacy"))
    val known = LegacyId(catalog.legacies.head.id.value)
    val ready = accepted(GameStartRules.evolve(catalog, sourcedBy(known),
      orders))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.III),
      FoundationState(FoundationFace.Altered, Set(known)))

  test("game start accepts an altered Foundation VI"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle.copy(
      foundations = Map(FoundationNumber.VI -> altered)), orders))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.VI), altered)

  test("game start accepts an altered Foundation II"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle.copy(
      foundations = Map(FoundationNumber.II -> altered)), orders))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.II), altered)

  test("game start accepts an altered Foundation IV dealt as Teeming World " +
      "deals, and refuses it dealt as Ties of Fellowship deals"):
    val teemingChronicle = chronicle.copy(
      foundations = Map(FoundationNumber.IV -> altered))
    val ready = accepted(GameStartRules.evolve(catalog, teemingChronicle,
      LaterGameFixture.orders(teemingChronicle, orders.firstPlayer)))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.IV), altered)
    // `orders` leaves 45 denizens in the world deck.
    assertEquals(rejected(GameStartRules.evolve(catalog, teemingChronicle,
        orders)),
      OathViolation.UnsupportedChronicle(
        "the world deck is not the rest of the deal"))

  test("game start accepts several altered faces at once and keeps each"):
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    val ready = accepted(GameStartRules.evolve(catalog, several, orders))
    assertEquals(Vector(FoundationNumber.II, FoundationNumber.III,
      FoundationNumber.V, FoundationNumber.VI).map(
        ready.game.campaign.foundations), Vector.fill(4)(altered))

  test("a later game with a seated Chancellor starts"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p1"))))

  test("game start refuses a later game whose first player is not the " +
      "Chancellor"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p2"))),
      Left(OathViolation.UnsupportedChronicle(
        "the first player must be the Chancellor")))

  test("with the Chancellor absent, the first player must be a seated " +
      "Citizen"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l2"))
    accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p2"))))
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "the first player must be a seated Citizen")))

  test("with no Chancellor and no Citizen seated, a later game starts " +
      "with no Empire, whoever goes first"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
    Vector("p1", "p2", "p3").foreach { first =>
      val ready = accepted(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId(first))))
      assertEquals(Imperials.players(ready), Vector.empty)
      assertEquals(ready.game.current.turn.activePlayer, PlayerId(first))
    }

  test("a Chronicle that lists no Chancellor makes a seated Citizen the " +
      "Chancellor, who must go first, and with no Citizen seated starts " +
      "with no Empire"):
    val citizen = LineageState(LineageId("l2"), None, Role.Citizen,
      Vector.empty, Vector.empty)
    val record = LaterGameFixture.chronicle(LineageId("l9"), world = 2)
    val seatedCitizen = record.copy(lineages = Vector(citizen))
    val ready = accepted(GameStartRules.evolve(catalog, seatedCitizen,
      LaterGameFixture.orders(seatedCitizen, PlayerId("p2"))))
    assertEquals(Imperials.chancellor(ready), Some(PlayerId("p2")))
    assertEquals(ready.game.campaign.boxRoles, Map.empty)
    assertEquals(GameStartRules.evolve(catalog, seatedCitizen,
        LaterGameFixture.orders(seatedCitizen, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "the first player must be a seated Citizen")))
    val nobodyListed = record.copy(lineages = Vector.empty)
    val exiles = accepted(GameStartRules.evolve(catalog, nobodyListed,
      LaterGameFixture.orders(nobodyListed, PlayerId("p3"))))
    assertEquals(Imperials.players(exiles), Vector.empty)
    assertEquals(exiles.game.current.map.inPlay, record.world.map(_.site))

  test("game start refuses a former Empire with no site, as it does an " +
      "Empire's (R28)"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), world = 0)
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire needs at least one site")))

  test("game start refuses an Empire with no site (R28)"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), world = 0)
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire needs at least one site")))

  test("game start refuses an Empire of more than 8 sites"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), world = 8)
    val extra = record.copy(world = record.world :+
      StoredSite(GreatSlumSite.id))
    assertEquals(GameStartRules.evolve(catalog, extra,
        LaterGameFixture.orders(extra, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire cannot hold more than 8 sites")))

  test("a later game's short deck is accepted mixed with the Visions, and " +
      "refused when it is not the rest of the deal"):
    // Every card but 15 dealt and 20 left leaves a deck under 25.
    val record = LaterGameFixture.chronicle(LineageId("l1"))
    val short = record.copy(worldDeck = record.worldDeck.take(6 + 9 + 20))
    val mixed = ChronicleFirstGamePlan.dealOrder(
      FirstGameBootstrapConfig(orders.participants, PlayerId("p1")),
      SetupDeal.of(short, orders.participants), short.worldDeck,
      short.relicDeck, _.reverse)
    accepted(GameStartRules.evolve(catalog, short, mixed))
    assertEquals(GameStartRules.evolve(catalog, short, mixed.copy(
        worldDeckOrder = mixed.worldDeckOrder.init)),
      Left(OathViolation.UnsupportedChronicle(
        "the world deck is not the rest of the deal")))

  test("a later game skips the Homeland edifice check (R26)"):
    val record = LaterGameFixture.chronicle(LineageId("l1"))
    val bare = record.copy(atlasBox = record.atlasBox.map(site =>
      site.copy(items = site.items.filterNot(_.isInstanceOf[EdificeId]))))
    accepted(GameStartRules.evolve(catalog, bare,
      LaterGameFixture.orders(bare, PlayerId("p1"))))

  test("a later game accepts stored denizens and relics at atlas sites (R27)"):
    val record = LaterGameFixture.chronicle(LineageId("l1"))
    val relic = record.relicDeck.head
    val denizen = record.worldDeck.head
    val atlas = record.atlasBox.head
    val stored = record.copy(relicDeck = record.relicDeck.tail,
      worldDeck = record.worldDeck.tail,
      atlasBox = atlas.copy(items = atlas.items ++ Vector(denizen, relic)) +:
        record.atlasBox.tail)
    accepted(GameStartRules.evolve(catalog, stored,
      LaterGameFixture.orders(stored, PlayerId("p1"))))

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

  test("a site stored in both the atlas and the Empire is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val stored = chronicle.atlasBox.head
    val twice = chronicle.copy(world = Vector(stored))
    assertEquals(rejected(GameStartRules.evolve(catalog, twice, orders)),
      OathViolation.UnsupportedChronicle(
        s"duplicate atlas site ${stored.site.value}"))

  test("a stored denizen on an atlas site starts an exile-only game, " +
      "which keeps it at its site"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val denizen = chronicle.worldDeck.last
    val stored = chronicle.copy(worldDeck = chronicle.worldDeck.init,
      atlasBox = chronicle.atlasBox.updated(0, chronicle.atlasBox.head.copy(
        items = chronicle.atlasBox.head.items :+ denizen)))
    val ready = accepted(GameStartRules.evolve(catalog, stored,
      LaterGameFixture.orders(stored, orders.firstPlayer)))
    assert(!ready.setup.ignoreCitizenshipPowers)
    assert(ready.game.current.map.sites(stored.atlasBox.head.site).denizens
      .exists(_.id == denizen), ready.game.current.map)

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
