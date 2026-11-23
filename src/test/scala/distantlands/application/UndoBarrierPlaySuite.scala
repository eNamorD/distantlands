package distantlands.application

import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.chronicle.ChronicleBeginProcedure
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.testkit.MenuPlay
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps,
  LaterGameFixture, LegacyKeep, SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{DecisionAnswers, Dice, ParkedNode, Situation,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Commands
import distantlands.testkit.Unwrap.{accepted, present}

/** Real batches that undo never takes back (undo design, "Barriers"), and
  * the ones beside them it still may. */
class UndoBarrierPlaySuite extends munit.FunSuite:
  private def nothingToUndo(service: GameApplicationService, gameId: String,
      at: Long, player: PlayerId)(using munit.Location): Unit =
    assertEquals(service.handle(gameId, at, Commands.undo(player)),
      Left(GameApplicationError.CommandRejected(OathViolation.NothingToUndo)))

  test("a Recover's start rolls, so it cannot be taken back"):
    val park = ParkedServiceFixture.recoverChoicePark("recover")
    nothingToUndo(park.game.service, "recover",
      park.game.accepted.nextSequence, park.actor)

  test("peeking at a site's relics cannot be taken back"):
    val table = Table.start
      .pawn(p1, BrokenPeaksSite)
      .relicAt(StickyFireCard, BrokenPeaksSite)
      .banditsRefilled
    val (service, repository) = table.service()
    val act = table.situation(Situation.journaled(service, catalog,
      repository, "peek"))
    val peeked = act.after(MenuPlay.start(p1, ActionRef.PeekSiteRelics))
    nothingToUndo(service, "peek", peeked.nextSequence, p1)

  test("finishing Rest hands the turn over, so neither player can undo"):
    val table = Table.start.turn(p1, Phase.Act).banditsRefilled
    val (service, repository) = table.service()
    val act = table.situation(Situation.journaled(service, catalog,
      repository, "rest"))
    val passed = act.after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
    // Rest asks p1 nothing here, so Begin Rest runs on into Finish Rest.
    assertEquals(passed.ready.game.current.turn.activePlayer, p2)
    nothingToUndo(service, "rest", passed.nextSequence, p1)
    nothingToUndo(service, "rest", passed.nextSequence, p2)

  test("an Explore's placement shows the site and what it stores, so the " +
      "end chosen before it cannot be taken back"):
    // p1 explores under Wide Horizons from Buried Giant, the Hintermost of
    // three sites; the dice reach 4 at once, and five sites wait in the
    // Atlas.
    val table = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    val (service, repository) = table.service(campaignDice = Dice.steady)
    val act = table.situation(Situation.journaled(service, catalog,
      repository, "explore"))
    val asked = act.parkedAfter(MenuPlay.start(p1, ActionRef.Explore))
    val placed = asked.after(Commands.resolveWalker(p1, TreeDecision(
      ExploreProcedure.endDecisionId, DecisionAnswer.ChooseOneAnswer(
        ExploreProcedure.endOption(AtlasEnd.Forgotten)))))
    assertEquals(placed.ready.game.current.map.inPlay.size, 4)
    nothingToUndo(service, "explore", placed.nextSequence, p1)

  test("Imperial Maps' fill shows the placed sites, so the Chancellor's " +
      "choice of end cannot be taken back"):
    // A later game's Setup: p1, the Chancellor, is asked Recent or
    // Forgotten first, and the Reliquary is empty.
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val asked = Situation.start(Situation.journaled(service, catalog,
        repository, "imperial-maps"))
      .parkedAfter(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    val filled = asked.parkedAfter(Commands.resolveWalker(p1, TreeDecision(
      ImperialMaps.decisionId, DecisionAnswer.ChooseOneAnswer(
        AtlasPlacement.endOption(AtlasEnd.Forgotten)))))
    assertEquals(filled.ready.game.current.map.inPlay.size, 8)
    nothingToUndo(service, "imperial-maps", filled.nextSequence, p1)

  test("before the fill the Reliquary pick can still be taken back, " +
      "returning to the Reliquary question"):
    // p1, the Chancellor, keeps the Reliquary's one relic and is then asked
    // Recent or Forgotten; nothing has been placed yet.
    val base = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val relic = base.relicDeck.head
    val record = base.copy(relicDeck = base.relicDeck.tail,
      reliquary = Vector(relic))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val begun = Situation.start(Situation.journaled(service, catalog,
        repository, "reliquary"))
      .parkedAfter(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    def parkedDecision(state: OathState): String =
      present(accepted(ParkedNode.of(state, catalog)), "a park").decisionId
    assertEquals(parkedDecision(begun.state),
      SetupProcedure.reliquaryDecisionId)
    val picked = begun.parkedAfter(Commands.resolveWalker(p1, TreeDecision(
      SetupProcedure.reliquaryDecisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(relic)))))
    assertEquals(parkedDecision(picked.state), ImperialMaps.decisionId)
    val undone = accepted(service.handle("reliquary", picked.nextSequence,
      Commands.undo(p1)))
    assertEquals(undone.state, begun.state)
    assertEquals(parkedDecision(undone.state),
      SetupProcedure.reliquaryDecisionId)

  /** p3's Rest hands p1, the Usurper, a Wake that p1 wins: the game parks on
    * the winner's Start Chronicle decision. */
  private def usurperWon(gameId: String)
      : (GameApplicationService, Situation) =
    val table = Table.start.turn(p3, Phase.Act)
      .oathkeeper(Some(p1), TitleSide.Usurper).banditsRefilled
    val (service, repository) = table.service()
    val act = table.situation(Situation.journaled(service, catalog,
      repository, gameId))
    val won = act.parkedAfter(MenuPlay.start(p3,
      PhaseTransitionRef.BeginRest))
    assertEquals(won.ready.game.current.result.map(_.winner), Some(p1))
    (service, won)

  private val startChronicle: GameCommand = Commands.resolveWalker(p1,
    TreeDecision(ChronicleBeginProcedure.decisionId,
      DecisionAnswer.ChooseOneAnswer(ChronicleBeginProcedure.start)))

  test("the batch that wins the game cannot be taken back"):
    val (service, won) = usurperWon("won")
    nothingToUndo(service, "won", won.nextSequence, p1)
    nothingToUndo(service, "won", won.nextSequence, p3)

  test("once the game is won, the winner can still take back starting the " +
      "Chronicle"):
    val (service, won) = usurperWon("chronicle-start")
    val started = won.parkedAfter(startChronicle)
    assertEquals(started.ready.game.current.turn.phase, Phase.Chronicle)
    val undone = accepted(service.handle("chronicle-start",
      started.nextSequence, Commands.undo(p1)))
    assertEquals(undone.state, won.state)

  test("finishing the Chronicle shows the evolved Chronicle, so it cannot " +
      "be taken back"):
    val (service, won) = usurperWon("chronicle-finish")
    val done = won.after(startChronicle)
    assert(done.ready.game.campaign.evolved.nonEmpty)
    nothingToUndo(service, "chronicle-finish", done.nextSequence, p1)
    nothingToUndo(service, "chronicle-finish", done.nextSequence, p2)
    nothingToUndo(service, "chronicle-finish", done.nextSequence, p3)

  /** A later game's Setup parked at p1's keep. The Reliquary is empty, so
    * Imperial Maps asks p1, the Chancellor, first; the default answer
    * takes its first end, and the deal follows. */
  private def laterGameAtFirstKeep(gameId: String)
      : (GameApplicationService, Situation, SetupOrders) =
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val orders = LaterGameFixture.orders(record, p1)
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val atKeep = Situation.start(Situation.journaled(service, catalog,
        repository, gameId))
      .parkedAt(LegacyKeep.decisionId(p1))(GameCommand.Begin(record, orders))
    (service, atKeep, orders)

  /** `player`'s keep of the first legacy dealt to them. */
  private def keepFirst(orders: SetupOrders, player: PlayerId): GameCommand =
    val Vector(kept, returned) = SetupLegacyFixture.drawn(orders, player)
    Commands.resolveWalker(player, TreeDecision(LegacyKeep.decisionId(player),
      DecisionAnswers.keepLegacy(kept, returned)))

  test("a later game's keep can be taken back until the next keeper " +
      "answers, returning to the keep question"):
    val (service, atKeep, orders) = laterGameAtFirstKeep("keep-undo")
    val kept = atKeep.parkedAfter(keepFirst(orders, p1))
    val undone = accepted(service.handle("keep-undo", kept.nextSequence,
      Commands.undo(p1)))
    assertEquals(undone.state, atKeep.state)

  test("once the next keeper answers, the earlier keep cannot be taken " +
      "back, and the next keeper's can"):
    val (service, atKeep, orders) = laterGameAtFirstKeep("keep-barrier")
    val first = atKeep.parkedAfter(keepFirst(orders, p1))
    val second = first.parkedAfter(keepFirst(orders, p2))
    assertEquals(service.handle("keep-barrier", second.nextSequence,
      Commands.undo(p1)), Left(GameApplicationError.CommandRejected(
        OathViolation.UndoNotIssuer(p1, p2))))
    val undone = accepted(service.handle("keep-barrier", second.nextSequence,
      Commands.undo(p2)))
    assertEquals(undone.state, first.state)
