package oathdigital.gameplay.setup

import oathdigital.gameplay.cards.GreatSlumSite
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.setup.HallOfDebateCard
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._
import oathdigital.testkit.Unwrap.accepted

/** A later game's table, built by `GameStartRules` from a Chronicle with an
  * Empire: l1 is the seated Chancellor and p1 goes first. */
class LaterGameSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")
  private val p2 = PlayerId("p2")
  private val p3 = PlayerId("p3")
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val record = LaterGameFixture.chronicle(l1, l2)

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def relicSlots(site: SiteId): Int =
    catalog.site(site).get.relicSlots

  private def faceDown(relic: RelicId) =
    RelicState(relic, Orientation.FaceDown, Tokens.empty)

  /** `record` with `relic` taken from the relic deck and `denizen`, if any,
    * from the world deck. */
  private def withoutDeckCards(relic: RelicId,
      denizen: Option[DenizenId] = None): Chronicle =
    record.copy(relicDeck = record.relicDeck.filterNot(_ == relic),
      worldDeck = record.worldDeck.filterNot(denizen.contains))

  test("the Empire's sites fill the first slots Hinterward, each with one " +
      "Imperial warband, intact edifices and faceup denizens"):
    val map = started(record).game.current.map
    assertEquals(map.hinterward.take(2), record.world.map(_.site))
    record.world.foreach { stored =>
      val site = map.sites(stored.site)
      assertEquals(site.forces, SiteForces.Occupied(ForceKind.Imperial, 1))
      assertEquals(site.denizens.map(_.id), stored.items)
      assert(site.denizens.forall {
        case EdificeState(_, side, _) => side == EdificeSide.Intact
        case DenizenState(_, orientation, _) =>
          orientation == Orientation.FaceUp
      })
    }

  test("an Empire site keeps its stored relics facedown and fills no empty " +
      "slot"):
    val relic = record.relicDeck.head
    val empireSiteWithOneRelic = record.world.head.copy(
      items = record.world.head.items.take(2) :+ relic)
    val chronicle = withoutDeckCards(relic).copy(
      world = empireSiteWithOneRelic +: record.world.tail)
    val ready = started(chronicle)
    assertEquals(ready.game.current.map.sites(empireSiteWithOneRelic.site)
      .relics, Vector(faceDown(relic)))
    assertEquals(ready.game.current.map.sites(record.world(1).site).relics,
      Vector.empty)

  test("a remaining site keeps its stored cards, ruins its edifice and " +
      "fills only its empty relic slots (R27)"):
    val atlasSiteWithEdifice = record.atlasBox.find(stored =>
      stored.items.exists(_.isInstanceOf[EdificeId])).get
    val edifice = atlasSiteWithEdifice.items.collectFirst {
      case id: EdificeId => id }.get
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val stocked = atlasSiteWithEdifice.copy(
      items = Vector(edifice, denizen, relic))
    val chronicle = withoutDeckCards(relic, Some(denizen)).copy(
      atlasBox = record.atlasBox.map(stored =>
        if stored.site == stocked.site then stocked else stored))
    val site = started(chronicle).game.current.map.sites(stocked.site)
    val capacity = catalog.site(stocked.site).get.capacity
    assertEquals(site.forces, SiteForces.Occupied(ForceKind.Bandit, capacity))
    assertEquals(site.denizens, Vector(
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))
    assertEquals(site.relics.size, math.max(1, relicSlots(stocked.site)))
    assertEquals(site.relics.head, faceDown(relic))

  test("the edifice deck holds no edifice stored at any site"):
    val stored = (record.world ++ record.atlasBox).flatMap(_.items)
      .collect { case id: EdificeId => id }
    assert(stored.nonEmpty)
    assert(!started(record).game.current.commonCards.edificeDeck
      .exists(stored.contains))

  test("each favor bank adds one per denizen of its suit on the map"):
    val ready = started(record)
    val onMap = ready.game.current.map.sites.values.flatMap(_.denizens)
      .flatMap {
        case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
        case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
      }.toVector
    assert(onMap.size > 2, "the Empire's denizens are on the map")
    Suit.all.foreach(suit => assertEquals(ready.banks.favor(suit),
      3 + onMap.count(_ == suit)))

  test("the atlas keeps the unused sites with their stored cards"):
    // Great Slum is not one of the Chronicle's sites, and Hall of Debate is
    // stored at none of them.
    val unused = GreatSlumSite.id
    val edifice = HallOfDebateCard.id
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val chronicle = withoutDeckCards(relic, Some(denizen)).copy(
      atlasBox = record.atlasBox :+ StoredSite(unused,
        Vector(edifice, denizen, relic)))
    assert(started(chronicle).game.campaign.atlas.entries.contains(
      AtlasEntry.StoredSite(unused,
        Vector(DenizenState(denizen, Orientation.FaceUp, Tokens.empty)),
        Vector(faceDown(relic)), Some(edifice))))

  private def player(ready: ReadyGame, id: PlayerId): PlayerState =
    ready.game.current.players.find(_.player == id).get

  /** Six seats, l1 the Chancellor and l2 to l6 Citizens, and all 8 sites
    * the Empire's; the Hintermost keeps `shortSiteRelic`. */
  private lazy val shortSiteRelic = record.relicDeck.head

  private lazy val sixImperialPlayersEightEmpireSites: ReadyGame =
    val eight = LaterGameFixture.chronicle(l1,
      (2 to 6).map(n => LineageId(s"l$n")).toVector, world = 8)
    val hintermost = eight.world.last
    val chronicle = eight.copy(relicDeck = eight.relicDeck.tail,
      world = eight.world.init :+ hintermost.copy(
        items = hintermost.items :+ shortSiteRelic))
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1, LaterGameFixture.seats(6))))

  test("the Chancellor takes the Grand Scepter faceup and the Oathkeeper " +
      "title, and begins the round"):
    val ready = started(record)
    assert(player(ready, p1).relics.contains(RelicState(
      TheGrandScepterCard.id, Orientation.FaceUp, Tokens.empty)))
    assertEquals(ready.game.current.title,
      OathkeeperState(Some(p1), TitleSide.Oathkeeper))
    assertEquals(ready.game.current.turn.activePlayer, p1)
    assertEquals(ready.game.current.tracks.roundStarter, p1)
    assert(!ready.game.current.tracks.usurperLimited)

  test("every player starts with 1 favor, 1 secret, 3 board warbands and " +
      "Supply 7; Imperial boards hold purple"):
    val ready = started(record)
    ready.game.current.players.foreach(state => assertEquals(state.board,
      PlayerBoardState(1, 1, 0, 3, SupplyTrack.full)))
    assertEquals(Imperials.players(ready).toSet, Set(p1, p2))
    assertEquals(PlayerFacts.banked(ready, ForceKind.Imperial),
      MaterialBankState.ImperialWarbands - 3 * 2 - record.world.size)

  test("with too few purple warbands, the Hintermost Empire sites take " +
      "bandits up to their capacity (R30)"):
    // The boards take 18 of the 24, so only the 6 Cradlemost Empire sites
    // get a purple warband.
    val ready = sixImperialPlayersEightEmpireSites
    val map = ready.game.current.map
    val (purple, short) = map.hinterward.splitAt(6)
    purple.foreach(site => assertEquals(map.sites(site).forces,
      SiteForces.Occupied(ForceKind.Imperial, 1)))
    short.foreach { site =>
      val capacity = catalog.site(site).get.capacity
      assertEquals(map.sites(site).forces,
        if capacity == 0 then SiteForces.Empty
        else SiteForces.Occupied(ForceKind.Bandit, capacity))
    }
    assertEquals(ready.banks.warbandSupply(ForceKind.Imperial),
      MaterialBankState.ImperialWarbands)
    assertEquals(PlayerFacts.banked(ready, ForceKind.Imperial), 0)

  test("a short Empire site is otherwise set up as an Empire site (R30)"):
    val site = sixImperialPlayersEightEmpireSites.game.current.map.sites(
      sixImperialPlayersEightEmpireSites.game.current.map.hinterward.last)
    assert(site.denizens.nonEmpty)
    assert(site.denizens.forall {
      case EdificeState(_, side, _) => side == EdificeSide.Intact
      case DenizenState(_, orientation, _) => orientation == Orientation.FaceUp
    })
    assertEquals(site.relics, Vector(faceDown(shortSiteRelic)))

  test("an absent Chancellor's seated Citizen becomes the Chancellor and the " +
      "absent lineage a Citizen in the box (R6)"):
    val l9 = LineageId("l9")
    val chronicle = LaterGameFixture.chronicle(l9, l2)
    val ready = accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p2)))
    assertEquals(Imperials.chancellor(ready), Some(p2))
    assertEquals(ready.game.campaign.lineages(l2).role, Role.Chancellor)
    assertEquals(ready.game.campaign.boxRoles(l9), Role.Citizen)

  private lazy val adviser = DenizenState(record.worldDeck.last,
    Orientation.FaceUp, Tokens.empty)

  /** `record` with l2 keeping `adviser`, taken from the world deck. */
  private lazy val l2KeepsAnAdviser: ReadyGame = started(record.copy(
    worldDeck = record.worldDeck.init,
    lineages = record.lineages.map(lineage =>
      if lineage.id == l2 then
        lineage.copy(startingAdvisers = Vector(adviser))
      else lineage)))

  test("a seated lineage takes its starting advisers facedown and draws no " +
      "hand"):
    val ready = l2KeepsAnAdviser
    assertEquals(player(ready, p2).advisers,
      Vector(adviser.copy(orientation = Orientation.FaceDown)))
    assertEquals(ready.game.campaign.lineages(l2).startingAdvisers,
      Vector.empty)
    assert(ready.game.current.temporaryHands.getOrElse(p2, Vector.empty)
      .isEmpty)
    // p1 and p3 draw the two hands after the 6 seeded discards.
    val dealt = Region.all.flatMap(ready.game.current.commonCards.discard).size
    assertEquals(dealt, 6)
    val order = LaterGameFixture.orders(record.copy(
      worldDeck = record.worldDeck.init, lineages = record.lineages.map(
        lineage => if lineage.id == l2 then
          lineage.copy(startingAdvisers = Vector(adviser)) else lineage)),
      p1).denizenOrder
    assertEquals(ready.game.current.temporaryHands(p1), order.slice(6, 9))
    assertEquals(ready.game.current.temporaryHands(p3), order.slice(9, 12))

  test("the People's Favor goes to the Chancellor when the goal is The " +
      "People, and the Darkest Secret when it is Devotion"):
    def banners(goal: OathkeeperGoal) =
      started(record.copy(oathkeeperGoal = goal)).game.current.banners
    assertEquals(banners(OathkeeperGoal.ThePeople).peoplesFavor.holder,
      Some(p1))
    assertEquals(banners(OathkeeperGoal.ThePeople).darkestSecret.holder, None)
    assertEquals(banners(OathkeeperGoal.Devotion).darkestSecret.holder,
      Some(p1))
    assertEquals(banners(OathkeeperGoal.Supremacy).peoplesFavor.holder, None)

  test("the card index holds each card once"):
    assert(l2KeepsAnAdviser.game.cardIndex.isRight)
    assert(sixImperialPlayersEightEmpireSites.game.cardIndex.isRight)
