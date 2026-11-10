package distantlands.server

import java.nio.file.Files

import distantlands.application._
import distantlands.application.MembershipRole._
import distantlands.gameplay.setup.FirstGameSetupFixture._
import distantlands.persistence.HsqldbDatabaseOwner
import distantlands.protocol.FirstGameBootstrapRequest
import distantlands.testkit.Outcomes
import distantlands.testkit.ChronicleShuffles.unshuffled
import distantlands.testkit.Unwrap.{accepted, present, rejected}

/** The owner's bootstrap of a provisioned game, kept for X6 without a
  * route: who may begin it, which seats it may name, and what it writes. */
class MembershipBootstrapSuite extends munit.FunSuite:
  private val owner = UserId("owner")
  private val otherOwner = UserId("other-owner")
  private val playerUsers = Vector("p1", "p2", "p3").map(id =>
    id -> UserId(s"user-$id"))
  private val spectator = UserId("spectator")
  private val outsider = UserId("outsider")

  /** A bootstrap request seating p2, p3 and p1, the provisioned seats, with
    * p2 first. */
  private val seating = request(Vector("p2", "p3", "p1"), "p2")

  /** "bootstrap-game" is provisioned but not yet begun: `owner` owns it,
    * user-p1 to user-p3 hold seats p1 to p3, and `spectator` watches.
    * `otherOwner` owns "other-game"; `outsider` belongs to neither. The
    * journal counts its appends. */
  private final case class ProvisionedGame(database: HsqldbDatabaseOwner,
      bootstrap: MembershipBootstrap, service: GameApplicationService,
      events: CountingEventStreamRepository)

  private val provisioned = FunFixture[ProvisionedGame](
    setup = _ =>
      val database = accepted(HsqldbDatabaseOwner.open(
        Files.createTempDirectory("membership-bootstrap-").resolve("database")))
      try
        val identities = database.identities
        (Vector(owner, otherOwner, spectator, outsider) ++
          playerUsers.map(_._2)).foreach(user =>
          identities.createUser(user, user.value, 0L))
        identities.createGame("bootstrap-game", owner, 0L)
        identities.createGame("other-game", otherOwner, 0L)
        playerUsers.foreach { case (playerId, userId) =>
          identities.addMembership(GameMembership(
            "bootstrap-game", userId, Player, Some(playerId)), 0L)
        }
        identities.addMembership(GameMembership(
          "bootstrap-game", spectator, Spectator, None), 0L)
        val events = new CountingEventStreamRepository
        val service = new GameApplicationService(catalog, events)
        // The plan must keep the exact participant order and first player it
        // was sent, so seating must not shuffle.
        val bootstrap = new MembershipBootstrap(service,
          new GameProjector(catalog),
          new MembershipAuthorizationService(identities), identities,
          new GeneratedFirstGamePlanFactory(catalog, unshuffled))
        ProvisionedGame(database, bootstrap, service, events)
      catch
        case error: Throwable =>
          database.close()
          throw error,
    teardown = _.database.close())

  provisioned.test("only the game's owner may begin it"):
    case ProvisionedGame(_, bootstrap, _, events) =>
      Vector(playerUsers.head._2, spectator, outsider, otherOwner).foreach:
        user => assertEquals(
          bootstrap.begin("bootstrap-game", AuthenticatedUser(user), seating),
          Left(BootstrapFailure.Authorization(
            if user == outsider || user == otherOwner then
              AuthorizationFailure.NotMember("bootstrap-game", user)
            else AuthorizationFailure.Forbidden("bootstrap"))))
      assertEquals(events.appendCalls, 0)

  provisioned.test("a seat set that differs from the provisioned seats is " +
      "refused and writes nothing"):
    case ProvisionedGame(_, bootstrap, service, events) =>
      Vector(
        request(Vector("p1", "p2"), "p1"),
        request(Vector("p1", "p2", "p3", "p4"), "p1"),
        request(Vector("p1", "p1", "p3"), "p1"),
        request(Vector("p1", "p2", "unmapped"), "p1")
      ).foreach { invalid =>
        rejected(bootstrap.begin("bootstrap-game", AuthenticatedUser(owner),
          invalid)) match
          case BootstrapFailure.Configuration(_) => ()
          case other => fail(s"expected a configuration refusal, got $other")
      }
      assertEquals(service.load("bootstrap-game"), Right(None))
      assertEquals(events.appendCalls, 0)

  provisioned.test("the owner's bootstrap begins the game in the sent seat " +
      "order, publicly projected, and a second one conflicts"):
    case ProvisionedGame(_, bootstrap, service, events) =>
      val begun = accepted(bootstrap.begin("bootstrap-game",
        AuthenticatedUser(owner), seating))
      assertEquals(begun.players.map(_.playerId), Vector("p2", "p3", "p1"))
      assertEquals(begun.viewerPlayerId, None)
      val json = GameHttpWire.encodeProjection(begun)
      Vector("worldDeckOrder", "relicOrder", "denizenOrder", "orderedSites",
        "userId", "actor").foreach(field => assert(!json.contains(field)))
      val replayed = present(accepted(service.load("bootstrap-game")),
        "the bootstrapped game")
      // GameStarted plus the WalkerParked fact from Setup's immediate first
      // park (2026-09-21 Chronicle design, slice 2).
      assertEquals(replayed.nextSequence, 2L)
      val ready = Outcomes.ready(replayed.state)
      assertEquals(ready.game.current.players.map(_.player.value),
        Vector("p2", "p3", "p1"))
      assertEquals(ready.setup.firstPlayer.value, "p2")
      val firstHistory = present(accepted(events.load("bootstrap-game")),
        "the bootstrapped journal")
      // The second request still expects position 0, so it is stale before
      // it could be a duplicate; either way it is a conflict that writes
      // nothing.
      assertEquals(bootstrap.begin("bootstrap-game", AuthenticatedUser(owner),
        seating), Left(BootstrapFailure.Application(
          GameApplicationError.StaleClientPosition(0L, 2L))))
      assertEquals(events.appendCalls, 1)
      assertEquals(present(accepted(events.load("bootstrap-game")),
        "the bootstrapped journal"), firstHistory)

  test("a bootstrap request carrying a hidden plan field is refused on the " +
      "wire"):
    val body = ujson.read(encoded(seating)).obj
    body("worldDeckOrder") = ujson.Arr("card:injected")
    assertEquals(rejected(GameHttpWire.decodeBootstrap(ujson.write(body))).path,
      "$.worldDeckOrder")

  private def request(playerIds: Vector[String], firstPlayer: String)
      : FirstGameBootstrapRequest =
    accepted(GameHttpWire.decodeBootstrap(ujson.write(ujson.Obj(
      "expectedNextSequence" -> 0,
      "participants" -> ujson.Arr.from(playerIds.zipWithIndex.map {
        case (playerId, index) => ujson.Obj(
          "playerId" -> playerId,
          "color" -> distantlands.model.PlayerColor.all(index).key)
      }),
      "firstPlayer" -> firstPlayer))))

  private def encoded(value: FirstGameBootstrapRequest): String =
    distantlands.protocol.FirstGameBootstrapCodec.encode(value)

  /** Spy of EventStreamRepository: delegates to an
    * InMemoryEventStreamRepository, which is a fake, and counts the appends
    * in `appendCalls`. */
  private final class CountingEventStreamRepository
      extends EventStreamRepository:
    private val delegate = new InMemoryEventStreamRepository
    var appendCalls: Int = 0

    override def load(gameId: String) = delegate.load(gameId)
    override def append(gameId: String, expected: ExpectedStream,
        records: Vector[String]) =
      appendCalls += 1
      delegate.append(gameId, expected, records)
