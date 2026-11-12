package distantlands.gameplay.setup

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._
import distantlands.testkit.{Answers, Situation}
import distantlands.testkit.Unwrap.accepted

/** Setup With No Empire (rulebook, "Setup With No Empire"; Foundations
  * design, slice 3). The Chronicle's Chancellor, l9, and its Citizen, l8,
  * sit out, so p1 to p3 play l1 to l3 as Exiles, and p2 goes first. Every
  * test starts from a Chronicle: game start is the subject. */
class NoEmpireSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private val (l8, l9) = (LineageId("l8"), LineageId("l9"))
  private val normal = FoundationState(FoundationFace.Normal, Set.empty)
  private val altered = FoundationState(FoundationFace.Altered, Set.empty)

  /** A Chronicle whose Empire nobody at the table belongs to: l9 is the
    * Chancellor and l8 a Citizen, and the Empire holds `empireSites` sites,
    * each storing a denizen. */
  private def absentEmpireChronicle(empireSites: Int = 2): Chronicle =
    LaterGameFixture.chronicle(l9, Vector(l8), empireSites)

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p2)))

  private def faceDown(relic: RelicId) =
    RelicState(relic, Orientation.FaceDown, Tokens.empty)

  test("Foundations erode: I turns to Wide Horizons and V to Mob's Favor, " +
      "with no sources, and the other faces stay"):
    val legacy = LegacyId(catalog.legacies.head.id.value)
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy))
    val campaign = started(absentEmpireChronicle().copy(foundations = Map(
      FoundationNumber.III -> sourced, FoundationNumber.V -> sourced)))
      .game.campaign
    assertEquals(campaign.foundations, FoundationNumber.all.map(_ -> normal)
      .toMap.updated(FoundationNumber.I, altered)
      .updated(FoundationNumber.III, sourced))

  test("an eroded Foundation V starts the People's Favor on the Mob in the " +
      "shared bank, whatever the Oathkeeper goal"):
    val record = absentEmpireChronicle().copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople,
      foundations = Map(FoundationNumber.V -> altered))
    assertEquals(started(record).game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.Mob, None, 1))

  test("a Foundation I already on Wide Horizons keeps its sources"):
    val legacy = LegacyId(catalog.legacies.head.id.value)
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy))
    assertEquals(started(absentEmpireChronicle().copy(foundations = Map(
      FoundationNumber.I -> sourced))).game.campaign
      .foundations(FoundationNumber.I), sourced)

  test("the former Empire's sites fill the first slots Hinterward, no " +
      "other site is placed, and the atlas keeps its sites in order"):
    val record = absentEmpireChronicle(empireSites = 3)
    val game = started(record).game
    val map = game.current.map
    assertEquals(map.hinterward, record.world.map(_.site))
    assertEquals(Region.all.map(map.sitesIn(_).size), Vector(2, 1, 0))
    assertEquals(map.sites.keySet, record.world.map(_.site).toSet)
    assertEquals(map.nextSlot, Some(Region.Provinces))
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, record.atlasBox.map(_.site))

  /** `absentEmpireChronicle(4)` with the fourth Empire site, a Homeland
    * that stores its edifice and a denizen, and the second, which has two
    * relic slots, also storing `storedRelic` from the relic deck. */
  private lazy val storedRelic = absentEmpireChronicle(4).relicDeck.head
  private lazy val stockedFormerEmpire: Chronicle =
    val record = absentEmpireChronicle(empireSites = 4)
    record.copy(relicDeck = record.relicDeck.tail,
      world = record.world.updated(1, record.world(1).copy(
        items = record.world(1).items :+ storedRelic)))

  test("a former Empire site has its edifice ruined, its denizens faceup, " +
      "its stored relics facedown, bandits up to its capacity and its " +
      "printed tokens"):
    val record = stockedFormerEmpire
    val map = started(record).game.current.map
    val homeland = record.world(3)
    val edifice = homeland.items.collectFirst { case id: EdificeId => id }.get
    val denizen = homeland.items.collectFirst { case id: DenizenId => id }.get
    assertEquals(map.sites(homeland.site).denizens, Vector(
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))
    record.world.foreach { stored =>
      val printed = catalog.site(stored.site).get
      val site = map.sites(stored.site)
      assert(printed.capacity > 0)
      assertEquals(site.forces,
        SiteForces.Occupied(ForceKind.Bandit, printed.capacity))
      assertEquals(site.tokens, printed.startingResources)
    }
    assertEquals(map.sites(record.world(1).site).relics.head,
      faceDown(storedRelic))

  test("every empty relic slot is filled facedown from the relic deck, " +
      "Cradlemost first, and a stored relic takes a slot"):
    val record = stockedFormerEmpire
    val current = started(record).game.current
    val slots = record.world.map(stored =>
      catalog.site(stored.site).get.relicSlots)
    // The second site keeps its stored relic in one of its two slots.
    assertEquals(slots, Vector(0, 2, 2, 0))
    assertEquals(record.world.map(stored =>
      current.map.sites(stored.site).relics), Vector(
      Vector.empty,
      Vector(faceDown(storedRelic), faceDown(record.relicDeck(0))),
      Vector(faceDown(record.relicDeck(1)), faceDown(record.relicDeck(2))),
      Vector.empty))
    assertEquals(current.commonCards.relicDeck, record.relicDeck.drop(3))

  test("a relic deck that runs out leaves the last slots empty"):
    val record = stockedFormerEmpire
    val short = record.copy(relicDeck = record.relicDeck.take(2))
    val current = started(short).game.current
    assertEquals(short.world.map(stored =>
      current.map.sites(stored.site).relics.size), Vector(0, 2, 1, 0))
    assertEquals(current.commonCards.relicDeck, Vector.empty)

  /** `absentEmpireChronicle()` with two relics from the relic deck in the
    * Reliquary. */
  private lazy val reliquaryRelics = absentEmpireChronicle().relicDeck.take(2)
  private lazy val formerEmpireWithReliquary: Chronicle =
    val record = absentEmpireChronicle()
    record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = reliquaryRelics)

  test("the Reliquary's relics stay set aside: no site, deck or player " +
      "holds one, and the card index holds every card once"):
    val record = formerEmpireWithReliquary
    val ready = started(record)
    val game = ready.game
    assertEquals(game.campaign.reliquary, reliquaryRelics)
    assert(game.campaign.reliquarySetAside)
    assertEquals(game.campaign.reliquaryInPlay, Vector.empty)
    val inPlay = game.current.map.sites.values.flatMap(_.relics.map(_.id)) ++
      game.current.commonCards.relicDeck ++
      game.current.players.flatMap(_.relics.map(_.id))
    assert(!inPlay.exists(reliquaryRelics.contains))
    // Every relic slot on the map is filled, from the relic deck alone.
    assertEquals(game.current.map.sites.values.map(_.relics.size).sum,
      record.world.map(stored => catalog.site(stored.site).get.relicSlots).sum)
    val index = accepted(game.cardIndex)
    reliquaryRelics.foreach(relic => assertEquals(
      index.get(relic).map(_.location.container),
      Some(CardContainer.Reliquary)))
    assertEquals(DomainValidation.validate(game), Vector.empty)

  test("a game with an Empire does not set the Reliquary aside: its " +
      "relics may be taken in play"):
    val empire = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val record = empire.copy(relicDeck = empire.relicDeck.drop(2),
      reliquary = empire.relicDeck.take(2))
    val campaign = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1))).game.campaign
    assert(!campaign.reliquarySetAside)
    assertEquals(campaign.reliquaryInPlay, record.reliquary)
    assert(!FirstGameSetupFixture.freshReady.game.campaign.reliquarySetAside)

  // There is no Chancellor, so Setup has no one to ask for a Reliquary
  // pick. This does not show that the set-aside flag stops a pick.
  test("the Grand Scepter is set aside, and with no Chancellor no one is " +
      "asked for a Reliquary relic"):
    val record = formerEmpireWithReliquary
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    val woken = Situation.wake(Situation.rules(catalog)
      .withAnswers(recording), record, LaterGameFixture.orders(record, p2))
    assert(asked.nonEmpty)
    assert(!asked.contains(SetupProcedure.reliquaryDecisionId), asked)
    assertEquals(woken.ready.game.campaign.reliquary, reliquaryRelics)
    assert(!accepted(woken.ready.game.cardIndex).ids
      .contains(TheGrandScepterCard.id))

  test("every Citizen in the box becomes an Exile, and so does the absent " +
      "Chancellor's lineage"):
    val exile = LineageState(LineageId("l7"), None, Role.Exile, Vector.empty,
      Vector.empty)
    val record = absentEmpireChronicle()
    val ready = started(record.copy(lineages = record.lineages :+ exile))
    assertEquals(ready.game.campaign.boxRoles,
      Map(l9 -> Role.Exile, l8 -> Role.Exile))
    assertEquals(ready.game.campaign.lineages.values.map(_.role).toSet,
      Set[Role](Role.Exile))
    assertEquals(Imperials.players(ready), Vector.empty)
    assertEquals(Imperials.chancellor(ready), None)
    assert(!ready.banks.warbandSupply.contains(ForceKind.Imperial))

  test("each favor bank holds 3 favor and 1 more per denizen of its suit " +
      "on the former Empire's sites, ruined edifices included"):
    val record = stockedFormerEmpire
    val onMap = record.world.flatMap(_.items).flatMap {
      case id: EdificeId => catalog.edifice(id).map(_.suit)
      case id: DenizenId => catalog.denizen(id).map(_.suit)
      case _ => None
    }
    // Four stored denizens and Deep Woods' edifice.
    assertEquals(onMap.size, 5)
    assertEquals(started(record).banks.favor,
      Suit.all.map(suit => suit -> (3 + onMap.count(_ == suit))).toMap)

  test("the option that ignores citizenship powers is off, as in every " +
      "game but the first"):
    assertEquals(started(absentEmpireChronicle()).setup
      .ignoreCitizenshipPowers, false)

  test("the plan's first player begins, the Usurper Limiter is on, and " +
      "the title and both banners start as in a first game"):
    // The People's Favor goes to the Chancellor under this goal, and there
    // is none.
    val current = started(absentEmpireChronicle().copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople)).game.current
    assertEquals(current.turn, TurnState(p2, Phase.Setup, Set.empty))
    assertEquals(current.tracks, GameTracks(1, 0, usurperLimited = true,
      roundStarter = p2))
    assertEquals(current.title, OathkeeperState(None, TitleSide.Oathkeeper))
    assertEquals(current.banners, BannersState(
      PeoplesFavorState(PeoplesFavorFace.Mob, None, 1),
      DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1,
        festival = None)))
    assertEquals(current.players.map(_.board),
      Vector.fill(3)(PlayerBoardState(1, 1, 0, 3, SupplyTrack.full)))

  test("an Exile may place their pawn at any site the bandits rule, and " +
      "at no site without warbands while one has bandits"):
    val ready = started(absentEmpireChronicle(empireSites = 3))
    val sites = ready.game.current.map.hinterward
    Vector(p1, p2, p3).foreach(player =>
      assertEquals(SetupPawnSites.legal(ready, player), sites))
    /** `ready` with `emptied` holding no warbands, as a capacity-0 site
      * holds none. */
    def withoutWarbandsAt(emptied: Set[SiteId]): ReadyGame =
      ready.updateCurrent(current => current.copy(map = current.map.copy(
        sites = current.map.sites.map((site, state) => site ->
          (if emptied(site) then state.copy(forces = SiteForces.Empty)
          else state)))))
    assertEquals(SetupPawnSites.legal(withoutWarbandsAt(Set(sites(1))), p1),
      Vector(sites(0), sites(2)))
    // With no bandits anywhere, any site: Setup must not stall.
    assertEquals(SetupPawnSites.legal(withoutWarbandsAt(sites.toSet), p1),
      sites)

  test("Setup runs to the first player's turn on the former Empire's sites"):
    val record = absentEmpireChronicle()
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p2))
    val current = woken.ready.game.current
    assertEquals(current.players.map(_.player), Vector(p1, p2, p3))
    assertEquals(current.turn.activePlayer, p2)
    assert(Set[Phase](Phase.Wake, Phase.Act)(current.turn.phase))
    assert(current.players.forall(_.pawnSite.exists(
      record.world.map(_.site).contains)))
    assertEquals(current.players.map(_.advisers.size), Vector(1, 1, 1))
    assertEquals(DomainValidation.validate(woken.ready.game), Vector.empty)
