package distantlands.gameplay.setup

import distantlands.gameplay.OathRules
import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** A Chronicle shaped like a first game's (no Chancellor, no Citizen, no
  * Empire site) is a first game only when it is brand new. One that carries
  * history, an import for example, starts an exile-only game: eight Atlas
  * sites as bandit sites, and the stored foundations and starting advisers
  * as stored (owner, 2026-11-12). Every test starts from a Chronicle: game
  * start is the subject. */
class ExileOnlySetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")
  private val brandNew = FirstGameSetupFixture.chronicle
  private val altered = FoundationState(FoundationFace.Altered, Set.empty)

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def mode(chronicle: Chronicle): SetupMode =
    accepted(EmpireSetupRules.resolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  /** The Atlas's first eight sites, from the Recent end. */
  private val firstEight = brandNew.atlasBox.take(MapState.slots)

  /** `brandNew` with foundation `number` on its altered face. */
  private def withAltered(number: FoundationNumber): Chronicle =
    brandNew.copy(foundations = Map(number -> altered))

  private def exile(id: String) =
    LineageState(LineageId(id), None, Role.Exile, Vector.empty, Vector.empty)

  private def homeland(stored: StoredSite): Option[Suit] =
    catalog.site(stored.site).flatMap(_.homeland)

  /** The first of the first eight sites that is, or is not, a Homeland. */
  private def firstSite(isHomeland: Boolean): StoredSite =
    firstEight.find(homeland(_).isDefined == isHomeland).get

  /** An edifice of a suit other than `suit` that `brandNew` stores
    * nowhere. */
  private def unstoredEdifice(notOf: Option[Suit]): EdificeId =
    val stored = brandNew.atlasBox.flatMap(_.items).toSet[CardId]
    catalog.edifices.filterNot(edifice => notOf.contains(edifice.suit))
      .map(edifice => EdificeId(edifice.id.value))
      .find(id => !stored(id)).get

  /** `brandNew` with `site`'s stored cards replaced by `items`. */
  private def storing(site: StoredSite, items: Vector[CardId]): Chronicle =
    brandNew.copy(atlasBox = brandNew.atlasBox.map(stored =>
      if stored.site == site.site then stored.copy(items = items)
      else stored))

  test("a brand-new Chronicle is a first game, and so is one listing six " +
      "normal faces with no source, as a played Chronicle may list"):
    assertEquals(mode(brandNew), SetupMode.FirstGame)
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    assertEquals(mode(brandNew.copy(foundations =
      FoundationNumber.all.map(_ -> normal).toMap,
      lineages = Vector(exile("l1")))), SetupMode.FirstGame)
    assert(started(brandNew).setup.ignoreCitizenshipPowers)

  test("each kind of history makes the Chronicle an exile-only game"):
    val relic = brandNew.relicDeck.head
    val denizen = brandNew.worldDeck.last
    val adviser = DenizenState(denizen, Orientation.FaceUp, Tokens.empty)
    val legacy = LegacyId(catalog.legacies.head.id.value)
    val homelandSite = firstSite(isHomeland = true)
    val withHistory = Vector(
      "an altered face" -> withAltered(FoundationNumber.III),
      "a Homeland with no stored edifice" -> storing(homelandSite,
        Vector.empty),
      "a sourced normal face" -> brandNew.copy(foundations = Map(
        FoundationNumber.III ->
          FoundationState(FoundationFace.Normal, Set(legacy)))),
      "a previous player" -> brandNew.copy(lineages = Vector(
        exile("l1").copy(previousPlayer = Some(p1)))),
      "a starting adviser" -> brandNew.copy(
        worldDeck = brandNew.worldDeck.init,
        lineages = Vector(exile("l1").copy(startingAdvisers = Vector(adviser)))),
      "a legacy" -> brandNew.copy(lineages = Vector(exile("l1").copy(
        legacies = Vector(LegacyState(legacy, active = true))))),
      "a starting relic" -> brandNew.copy(relicDeck = brandNew.relicDeck.tail,
        lineages = Vector(exile("l1").copy(startingRelics = Vector(relic)))),
      "a scored game" -> brandNew.copy(era = EraState(20, Vector(EraGame(
        Vector(EraEntry(LineageId("l1"), 3, Role.Exile)))))),
      "another Oathkeeper goal" ->
        brandNew.copy(oathkeeperGoal = OathkeeperGoal.Protection),
      "a stocked Reliquary" -> brandNew.copy(
        relicDeck = brandNew.relicDeck.tail, reliquary = Vector(relic)),
      "a stored atlas denizen" -> brandNew.copy(
        worldDeck = brandNew.worldDeck.init,
        atlasBox = brandNew.atlasBox.updated(0, brandNew.atlasBox(0).copy(
          items = brandNew.atlasBox(0).items :+ denizen))))
    withHistory.foreach { (history, chronicle) =>
      assert(EmpireSetupRules.hasHistory(catalog, chronicle), history)
      assertEquals(mode(chronicle),
        SetupMode.FirstGameExileOnly(chronicle.atlasBox.take(MapState.slots)),
        history)
    }

  test("an exile-only game places the Atlas's first eight sites Hinterward " +
      "as bandit sites, keeps Movers and Shakers and gives every board 6 " +
      "favor, with the Citizenship option off"):
    val ready = started(withAltered(FoundationNumber.II))
    val current = ready.game.current
    assertEquals(current.map.inPlay, firstEight.map(_.site))
    assertEquals(current.map.nextSlot, None)
    assertEquals(ready.game.campaign.foundations(FoundationNumber.II), altered)
    // No Empire existed to erode Foundation I to Wide Horizons.
    assertEquals(ready.game.campaign.foundations(FoundationNumber.I).face,
      FoundationFace.Normal)
    assertEquals(current.players.map(_.board.favor), Vector(6, 6, 6))
    // A Homeland is populated as a later game's Atlas site: its stored
    // edifice ruined, its bandits, and a relic in each slot.
    val homelandSite = firstSite(isHomeland = true)
    val printed = catalog.site(homelandSite.site).get
    val placed = current.map.sites(homelandSite.site)
    assertEquals(placed.denizens, homelandSite.items.collect {
      case id: EdificeId => EdificeState(id, EdificeSide.Ruined, Tokens.empty)
    })
    assertEquals(placed.forces,
      SiteForces.Occupied(ForceKind.Bandit, printed.capacity))
    assertEquals(placed.relics.size, printed.relicSlots)
    assert(!ready.setup.ignoreCitizenshipPowers)
    assertEquals(Imperials.players(ready), Vector.empty)
    assert(ready.game.campaign.reliquarySetAside)

  test("a stored Wide Horizons stays, and the eight sites are placed"):
    val ready = started(withAltered(FoundationNumber.I))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.I), altered)
    assertEquals(ready.game.current.map.inPlay, firstEight.map(_.site))
    assertEquals(ready.game.current.map.nextSlot, None)

  test("a lineage's stored starting adviser goes facedown to its player"):
    val denizen = brandNew.worldDeck.last
    val chronicle = brandNew.copy(worldDeck = brandNew.worldDeck.init,
      lineages = Vector(exile("l2").copy(startingAdvisers = Vector(
        DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))))
    val ready = started(chronicle)
    assertEquals(ready.game.current.player(PlayerId("p2")).get.advisers,
      Vector(DenizenState(denizen, Orientation.FaceDown, Tokens.empty)))
    assertEquals(ready.game.current.map.inPlay, firstEight.map(_.site))
    assert(!ready.setup.ignoreCitizenshipPowers)

  test("an exile-only game's recorded events replay to the same state"):
    val rules = new OathRules(catalog)
    val chronicle = withAltered(FoundationNumber.IV)
    val transition = accepted(rules.beginGame(OathState.NoGame, chronicle,
      LaterGameFixture.orders(chronicle, p1)))
    val replayed = transition.events
      .foldLeft[Either[OathViolation, OathState]](Right(OathState.NoGame)):
        case (Right(state), event) => rules.evolve(state, event)
        case (left, _) => left
    assertEquals(replayed, Right(transition.state))

  test("an Exile places their pawn at a site the bandits hold, as in a " +
      "later game, where a first game offers every site"):
    /** `ready` with no warband at its Cradlemost site. */
    def cradlemostCleared(ready: ReadyGame): ReadyGame =
      val site = ready.game.current.map.inPlay.head
      ready.updateCurrent(current => current.copy(map = current.map.copy(
        sites = current.map.sites.updated(site, current.map.sites(site)
          .copy(forces = SiteForces.Empty)))))
    val exileOnly = cradlemostCleared(started(withAltered(FoundationNumber.III)))
    assertEquals(SetupPawnSites.legal(catalog, exileOnly, p1),
      exileOnly.game.current.map.inPlay.tail)
    val firstGame = cradlemostCleared(started(brandNew))
    assertEquals(SetupPawnSites.legal(catalog, firstGame, p1),
      firstGame.game.current.map.inPlay)
    // When stored legacies were its only history, the wipe leaves a
    // starting Chronicle with none, so its pawns are placed as in a first
    // game, though its sites were placed exile-only (R41).
    val legacyOnly = cradlemostCleared(started(brandNew.copy(lineages =
      Vector(LineageState(LineageId("l1"), None, Role.Exile,
        Vector(LegacyState(catalog.legacies.head.id, active = true)),
        Vector.empty)))))
    assertEquals(SetupPawnSites.legal(catalog, legacyOnly, p1),
      legacyOnly.game.current.map.inPlay)

  test("an edifice stored at a site that is no Homeland makes an " +
      "exile-only game, which keeps it ruined at its site"):
    val site = firstSite(isHomeland = false)
    val edifice = unstoredEdifice(notOf = None)
    val chronicle = storing(site, Vector(edifice))
    assertEquals(mode(chronicle), SetupMode.FirstGameExileOnly(
      chronicle.atlasBox.take(MapState.slots)))
    assertEquals(started(chronicle).game.current.map.sites(site.site)
      .denizens, Vector(EdificeState(edifice, EdificeSide.Ruined,
        Tokens.empty)))

  test("a Homeland storing an edifice of another suit makes an exile-only " +
      "game rather than a refusal"):
    val site = firstSite(isHomeland = true)
    val offSuit = unstoredEdifice(notOf = homeland(site))
    val chronicle = storing(site, Vector(offSuit))
    assertEquals(mode(chronicle), SetupMode.FirstGameExileOnly(
      chronicle.atlasBox.take(MapState.slots)))
    assertEquals(started(chronicle).game.current.map.sites(site.site)
      .denizens, Vector(EdificeState(offSuit, EdificeSide.Ruined,
        Tokens.empty)))

  test("a first game played exile-only wipes every stored legacy, seated " +
      "and boxed, back into the legacy deck before the deal (R40)"):
    val stored = catalog.legacies.map(_.id).take(3)
    val boxed = LineageState(LineageId("boxed"), None, Role.Exile,
      Vector(LegacyState(stored(2), active = true)), Vector.empty)
    val record = SetupLegacyFixture.holding(brandNew.copy(lineages =
      brandNew.lineages :+ boxed), LineageId("l1"), Role.Exile,
      stored(0) -> true, stored(1) -> false)
    assert(mode(record).isInstanceOf[SetupMode.FirstGameExileOnly])
    // The fixture deals from the starting Chronicle, as the plan factory
    // does; game start accepting that deal is the proof.
    val orders = LaterGameFixture.orders(record, p1)
    val ready = accepted(GameStartRules.evolve(catalog, record, orders))
    orders.participants.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies, Vector.empty))
    assertEquals(ready.game.campaign.baseline.lineages.flatMap(_.legacies),
      Vector.empty)

  test("a first game played exile-only keeps starting advisers and relics " +
      "(R40)"):
    val adviser = brandNew.worldDeck.head
    val relic = brandNew.relicDeck.head
    val record = brandNew.copy(worldDeck = brandNew.worldDeck.tail,
      relicDeck = brandNew.relicDeck.tail,
      lineages = Vector(LineageState(LineageId("l1"), None, Role.Exile,
        Vector.empty, Vector(DenizenState(adviser, Orientation.FaceDown,
          Tokens.empty)), startingRelics = Vector(relic))))
    val ready = started(record)
    assertEquals(ready.game.current.player(p1).get.advisers.map(_.id),
      Vector(adviser))
    assertEquals(ready.game.current.player(p1).get.relics.map(_.id),
      Vector(relic))

