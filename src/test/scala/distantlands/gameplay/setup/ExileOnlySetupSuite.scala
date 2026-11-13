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
    accepted(EmpireSetupRules.resolve(chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  /** The Atlas's first eight sites, from the Recent end. */
  private val firstEight = brandNew.atlasBox.take(MapState.slots)

  /** `brandNew` with foundation `number` on its altered face. */
  private def withAltered(number: FoundationNumber): Chronicle =
    brandNew.copy(foundations = Map(number -> altered))

  private def exile(id: String) =
    LineageState(LineageId(id), None, Role.Exile, Vector.empty, Vector.empty)

  test("a brand-new Chronicle is a first game, and so is one listing six " +
      "normal faces with no source, as a TTS import writes"):
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
    val withHistory = Vector(
      "an altered face" -> withAltered(FoundationNumber.III),
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
      assert(EmpireSetupRules.hasHistory(chronicle), history)
      assertEquals(mode(chronicle),
        SetupMode.ExileOnly(chronicle.atlasBox.take(MapState.slots)),
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
    assertEquals(current.players.map(_.board.favor), Vector(6, 6, 6))
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
    assertEquals(SetupPawnSites.legal(exileOnly, p1),
      exileOnly.game.current.map.inPlay.tail)
    val firstGame = cradlemostCleared(started(brandNew))
    assertEquals(SetupPawnSites.legal(firstGame, p1),
      firstGame.game.current.map.inPlay)
