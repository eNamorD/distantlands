package distantlands.gameplay.chronicle

import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.testkit.{MenuPlay, Outcomes, Situation, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** A game on a map with empty slots, through the Chronicle Phase and into
  * the next game (Foundations design, slice 3, "Chronicle"). The game is
  * set up with no Empire on a former Empire of three sites, with two relics
  * in the Reliquary; p1 wakes as the Usurper, wins, and every task is
  * answered plainly. */
class EmptySlotChronicleSuite extends munit.FunSuite:
  import EvolvedChronicleFixture.rules

  private val (l8, l9) = (LineageId("l8"), LineageId("l9"))

  /** A Chronicle whose Empire of three sites nobody at the table belongs
    * to, with the first two relics of its relic deck in the Reliquary. */
  private val absentEmpireChronicle: Chronicle =
    val record = LaterGameFixture.chronicle(l9, Vector(l8), 3)
    record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = record.relicDeck.take(2))

  /** The no-Empire game after Setup, as p1's Wake begins with the Usurper
    * side of the title. */
  private lazy val usurperWakesWithNoEmpire: OathState =
    val setUp = Situation.wake(Situation.rules(catalog),
      absentEmpireChronicle,
      LaterGameFixture.orders(absentEmpireChronicle, p1)).ready
    Table.from(MenuPlay.withoutMenu(setUp)).turn(p1, Phase.Wake)
      .oathkeeper(Some(p1), TitleSide.Usurper).state

  private lazy val won = EvolvedChronicleFixture.gameWonFrom(
    usurperWakesWithNoEmpire)
  private lazy val done = EvolvedChronicleFixture.chronicleAnsweredAfter(won)
  private lazy val journaled = EvolvedChronicleFixture.chronicleJournaledBy(
    done)

  test("the Chronicle Phase runs every task on a map of three sites to a " +
      "record that passes game start's accounting, and replays"):
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    val ended = Outcomes.ready(done.state)
    assertEquals(ended.game.current.map.inPlay,
      absentEmpireChronicle.world.map(_.site))
    assertEquals(ended.game.campaign.evolved, Some(journaled))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, journaled),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(absentEmpireChronicle,
      journaled), Right(()))
    assertEquals(accepted((won.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](
        Right(usurperWakesWithNoEmpire))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)

  test("the Stars archive the sites the winner does not rule ahead of the " +
      "unexplored atlas sites, which keep their order"):
    val layout = accepted(ChronicleExtraction.layout(
      Outcomes.ready(done.state)))
    // The Throne gives a winner who rules no site their pawn's site.
    assertEquals(layout.empire.size, 1)
    assertEquals(layout.rivals.size, 2)
    assertEquals(journaled.world.map(_.site), layout.empire)
    assertEquals(journaled.atlasBox.map(_.site),
      layout.rivals ++ absentEmpireChronicle.atlasBox.map(_.site))
    assertEquals((layout.empire ++ layout.rivals).toSet,
      absentEmpireChronicle.world.map(_.site).toSet)

  test("erosion carries forward, the crowned Exile is the Chancellor and " +
      "the box stays exiled"):
    assertEquals(journaled.foundations.get(FoundationNumber.I),
      Some(FoundationState(FoundationFace.Altered, Set.empty)))
    assertEquals(journaled.foundations.get(FoundationNumber.V),
      Some(FoundationState(FoundationFace.Normal, Set.empty)))
    val roles = journaled.lineages.map(lineage => lineage.id -> lineage.role)
      .toMap
    assertEquals(roles(LineageId("l1")), Role.Chancellor)
    assertEquals(roles(l9), Role.Exile)
    assertEquals(roles(l8), Role.Exile)

  test("the set-aside Reliquary's relics return in the Chronicle Phase: " +
      "an Exile's win discards them to be scattered, and none is lost"):
    val reliquary = absentEmpireChronicle.reliquary
    assertEquals(Outcomes.ready(won.state).game.campaign.reliquary, reliquary)
    val scattered = journaled.relicDeck ++
      (journaled.world ++ journaled.atlasBox).flatMap(_.items)
    reliquary.foreach(relic => assert(scattered.contains(relic), relic))
    assert(!journaled.reliquary.exists(reliquary.contains))

  /** The no-Empire game p1 has just won, with each player's lineage given
    * the role `roles` names, as though it had become that in play, and p1
    * holding the relic deck's first relic for Stock Reliquary to add. A role
    * made in play brings the Imperial supply an Empire in play would have. */
  private def wonWithRolesMadeInPlay(roles: Map[PlayerId, Role])
      : (OathTransition, RelicId) =
    val table = Table.from(Outcomes.ready(won.state))
    val held = table.ready.game.current.commonCards.relicDeck.head
    val state = table.relic(p1, held).update(ready => ready
      .updateCampaign(campaign => campaign.copy(lineages =
        campaign.lineages.map((id, lineage) => id -> ready.game.current
          .players.find(_.lineage == id).flatMap(seat => roles.get(
            seat.player)).fold(lineage)(role => lineage.copy(role = role)))))
      .copy(banks = ready.banks.copy(warbandSupply =
        ready.banks.warbandSupply.updated(ForceKind.Imperial, 24)))).state
    (OathTransition(state, Vector.empty), held)

  /** The set-aside Reliquary's relics are scattered, the journaled
    * Reliquary holds only the relic Stock Reliquary added, every card is
    * accounted for once, and the Chronicle's events replay to its state.
    * The roles are set on the won state itself, which no event records, so
    * the replay starts there. */
  private def assertSetAsideRelicsScattered(roles: Map[PlayerId, Role])(
      using munit.Location): Unit =
    val (wonByRole, held) = wonWithRolesMadeInPlay(roles)
    val answered = EvolvedChronicleFixture.chronicleAnsweredAfter(wonByRole)
    val record = EvolvedChronicleFixture.chronicleJournaledBy(answered)
    assertEquals(accepted(answered.events
      .foldLeft[Either[OathViolation, OathState]](Right(wonByRole.state))(
        (state, event) => state.flatMap(rules.evolve(_, event)))),
      answered.state)
    val scattered = record.relicDeck ++
      (record.world ++ record.atlasBox).flatMap(_.items)
    absentEmpireChronicle.reliquary.foreach(relic =>
      assert(scattered.contains(relic), relic))
    assertEquals(record.reliquary, Vector(held))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(absentEmpireChronicle,
      record), Right(()))

  test("with no Empire, a Chancellor made in play who wins discards the " +
      "set-aside Reliquary as an Exile's win does"):
    assertSetAsideRelicsScattered(Map(p1 -> Role.Chancellor))

  test("with no Empire, a Citizen who wins discards the set-aside " +
      "Reliquary as an Exile's win does"):
    assertSetAsideRelicsScattered(Map(p1 -> Role.Citizen,
      p2 -> Role.Chancellor))

  test("the next game starts from the journaled Chronicle under Wide " +
      "Horizons: the Empire's one site returns and no other slot is filled"):
    val orders = LaterGameFixture.orders(journaled, p1)
    val next = accepted(GameStartRules.evolve(catalog, journaled, orders))
    assertEquals(Imperials.chancellor(next), Some(p1))
    assertEquals(next.game.current.map.inPlay, journaled.world.map(_.site))
    assertEquals(next.game.current.map.nextSlot, Some(Region.Cradle))
    assertEquals(next.game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, journaled.atlasBox.map(_.site))
    assert(!next.game.campaign.reliquarySetAside)
    val woken = Situation.wake(Situation.rules(catalog), journaled, orders)
    assertEquals(woken.ready.game.current.turn.activePlayer, p1)
    assert(woken.ready.game.current.players.forall(
      _.pawnSite == journaled.world.headOption.map(_.site)))
