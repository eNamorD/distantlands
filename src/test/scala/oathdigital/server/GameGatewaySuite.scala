package oathdigital.server

import java.nio.file.{Files, Path}

import oathdigital.application._
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.setup.SetupProcedure
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.model.OathState
import oathdigital.persistence.OwnedHsqldbEventStreamRepository
import oathdigital.protocol.{ActorlessCommandRequest, DecisionAnswerWire,
  GameIntent}
import oathdigital.protocol.projection.{DecisionQueryProjection, GameProjection,
  SiteForcesProjection}
import oathdigital.testkit.ChronicleShuffles.unshuffled
import oathdigital.testkit.Table.{p2, p3}
import oathdigital.testkit.Unwrap.{accepted, as, present}
import ParkedServiceFixture.loaded

/** The gateway every transport reaches once it has resolved an access: a
  * seat's own view, the public view with nothing private in it, and what a
  * game begun through the service looks like through either. This suite
  * asserts the exact requested first player, so seating must not shuffle. */
class GameGatewaySuite extends munit.FunSuite:
  // GeneratedFirstGamePlanFactory always makes the head of the (possibly
  // shuffled) seating the first player, ignoring the requested
  // `firstPlayer` -- so with an identity shuffle, "p2" must already be
  // first in `participants` to end up first here.
  private val config = FirstGameBootstrapConfig(
    participants.sortBy(p => if p.playerId == p2 then 0 else 1), p2)

  /** A game begun through the service and journaled in HSQL at `path`: the
    * plan it was dealt from, the gateway over it and p2's seat view. */
  private final case class BegunGame(path: Path, plan: FirstGamePlan,
      gateway: GameGateway, projection: GameProjection)

  private def begun(using munit.Location): BegunGame =
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog, unshuffled)
      .build(config))
    val dealt = ChronicleFirstGamePlan.dealOrder(plan.chronicle, plan.resolvedConfig)
    val path = Files.createTempDirectory("oathdigital-bootstrap-")
      .resolve("journal")
    val repository = accepted(OwnedHsqldbEventStreamRepository.open(path))
    val service = new GameApplicationService(catalog, repository)
    val gateway = new GameGateway(service, new GameProjector(catalog))
    val projection =
      try
        accepted(service.handle("bootstrap-game", 0L,
          GameCommand.Begin(plan.chronicle, dealt)))
        accepted(gateway.load("bootstrap-game", GameAccess.Seat(p2)))
      finally repository.close()
    BegunGame(path, plan, gateway, projection)

  test("beginning a game persists history and reloads it"):
    val game = begun
    // GameStarted plus the WalkerParked fact from Setup's immediate first
    // park (2026-09-21 Chronicle design, slice 2): two records for one
    // Begin command.
    assertEquals(game.projection.nextSequence, 2L)
    val reopened = accepted(OwnedHsqldbEventStreamRepository.open(game.path))
    try
      val stored = loaded(new GameApplicationService(catalog, reopened)
        .load("bootstrap-game"))
      assertEquals(stored.nextSequence, 2L)
      as[OathState.Ready](stored.state)
    finally reopened.close()

  test("a seat load names its viewer; a public load names none and hides " +
      "the parked decision"):
    val game = begun
    val reopened = accepted(OwnedHsqldbEventStreamRepository.open(game.path))
    try
      val gateway = new GameGateway(
        new GameApplicationService(catalog, reopened), new GameProjector(catalog))
      assertEquals(game.projection.viewerPlayerId, Some(p2.value))
      assert(game.projection.walkerDecision.nonEmpty)
      val other = accepted(gateway.load("bootstrap-game", GameAccess.Seat(p3)))
      assertEquals(other.viewerPlayerId, Some(p3.value))
      assertEquals(other.walkerDecision, None)
      // Nobody but the awaited player sees the parked decision's owner-private
      // projection (WalkerDecisionProjector.project); the public view instead
      // sees who it is waiting on, and names no viewer.
      val public = accepted(gateway.load("bootstrap-game", GameAccess.Public))
      assertEquals(public.viewerPlayerId, None)
      assertEquals(public.walkerDecision, None)
      assertEquals(public.walkerWaiting.map(_.playerId), Some(p2.value))
      assert(!GameHttpWire.encodeProjection(public).contains("viewerPlayerId"))
    finally reopened.close()

  test("a public view issues no command and reads the log without private " +
      "lines"):
    val game = begun
    val reopened = accepted(OwnedHsqldbEventStreamRepository.open(game.path))
    try
      val gateway = new GameGateway(
        new GameApplicationService(catalog, reopened), new GameProjector(catalog))
      val pawn = ActorlessCommandRequest(game.projection.nextSequence,
        GameIntent.ResolveWalker(SetupProcedure.pawnDecisionId(p2),
          DecisionAnswerWire.ChooseOneWire("site", "site:any")))
      assertEquals(gateway.submit("bootstrap-game", GameAccess.Public, pawn),
        Left(GatewayFailure.Access(AccessRefusal.Forbidden)))
      assertEquals(loaded(reopened.load("bootstrap-game")).nextSequence, 2L)
      val page = accepted(gateway.log("bootstrap-game", GameAccess.Public, 0L))
      assertEquals(page.nextSequence, 2L)
      assert(page.entries.nonEmpty)
      assertEquals(gateway.log("bootstrap-game", GameAccess.Public, 3L),
        Left(GatewayFailure.Malformed))
    finally reopened.close()

  test("a begun game projects its dealt world and tracks"):
    val BegunGame(_, plan, _, projection) = begun
    assertEquals(projection.phase, "setup-walker-decision")
    assertEquals(projection.playerBoards.size, config.participants.size)
    assert(projection.world.flatMap(_.sites).exists(_.looseFavor > 0))
    assert(projection.world.flatMap(_.sites).exists(_.relics.facedownCount > 0))
    assert(projection.world.flatMap(_.sites).exists(_.forces.exists(_.isInstanceOf[SiteForcesProjection.Bandit])))
    assert(projection.world.flatMap(_.sites).exists(_.denizens.exists(
      _.details.exists(details => details.cardKind == "edifice" &&
        details.side.contains("ruined")))))
    val projectedEdifices = projection.world.flatMap(_.sites).flatMap(_.denizens)
      .flatMap(_.details).filter(_.cardKind == "edifice")
    assert(projectedEdifices.nonEmpty)
    projectedEdifices.foreach { details =>
      val definition = present(
        catalog.edifices.find(_.id.value == details.cardId), details.cardId)
      assertEquals(details.restrictions, Some("unrestricted"))
      assertEquals(details.rulesText, Some(definition.ruined.rulesText))
    }
    // Setup seeds 2 discards in each region.
    assertEquals(projection.world.map(_.discardCount), Vector(2, 2, 2))
    assertEquals(projection.favorBanks.map(_.suit).toSet,
      oathdigital.model.Suit.all.map(_.key).toSet)
    // The first round, no Visions drawn, the Usurper limiter at round 4, and
    // p2 first.
    assertEquals(projection.tracks.map(track =>
      (track.round, track.visionsDrawn, track.usurperLimited, track.limiterRound,
        track.firstPlayerId)), Some((1, 0, true, 4, "p2")))
    assertEquals(projection.relicDeckCount +
      projection.world.flatMap(_.sites).map(_.relics.facedownCount).sum,
      plan.chronicle.relicDeck.size)

  test("the first Setup decision offers p2 every in-play site"):
    val projection = begun.projection
    // The generic walker decision panel renders Setup's first pawn placement
    // with no bespoke code (2026-09-21 Chronicle design, slice 2): just the
    // parked `Decide`.
    val decision = present(projection.walkerDecision, "p2's Setup decision")
    assertEquals(decision.action, "setup")
    assertEquals(decision.decisionId, SetupProcedure.pawnDecisionId(p2))
    val siteOptions = decision.query match
      case Some(one: DecisionQueryProjection.ChooseOne) => one.options
      case other => fail(s"expected a choose-one, got $other")
    // The 8 in-play sites.
    assertEquals(siteOptions.size, 8)
    assert(siteOptions.forall(_.kind == "site"))

  test("the redacted projection hides deck orders and relic ids"):
    val json = GameHttpWire.encodeProjection(begun.projection)
    assert(!json.contains("relicOrder"))
    assert(!json.contains("worldDeckOrder"))
    assert(!json.contains("denizenOrder"))
    catalog.relics.filter(_.id != TheGrandScepterCard.id)
      .foreach(relic => assert(!json.contains(relic.id.value)))
