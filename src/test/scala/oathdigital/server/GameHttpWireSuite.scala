package oathdigital.server

import akka.http.scaladsl.model.StatusCodes
import oathdigital.application.{GameApplicationError, GameIntentMapper,
  PlayerIntent, TreeDecision}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseOneAnswer, DistributeAnswer, PartitionAnswer}
import oathdigital.protocol.{DecisionAnswerWire, DecisionPlacementWire,
  DistributeAmountWire, GameIntent}
import oathdigital.testkit.Unwrap.rejected

class GameHttpWireSuite extends munit.FunSuite:
  test("domain conversion rejects unknown protocol identifiers without throwing"):
    val failure = rejected(GameIntentMapper.map(GameIntent.RequestConsent("travel.destination",
        oathdigital.protocol.WalkerStartArgWire("teleport", "x"))))
    assertEquals(failure.path, "$.intent.target")

  test("walker intents map to ResolveWalker intents"):
    // The intent carries no actor: the gateway binds the transport-resolved
    // one (TrustedSeatRoutesSuite), and the rules check it against the
    // parked position's owner before resuming anything.
    assertEquals(GameIntentMapper.map(GameIntent.ResolveWalker("recover.choice",
        DecisionAnswerWire.ChooseOneWire("button", "continue"))),
      Right(PlayerIntent.ResolveWalker(TreeDecision("recover.choice",
          ChooseOneAnswer(DecisionOptionRef.Button("continue"))))))
    assertEquals(GameIntentMapper.map(GameIntent.ResolveWalker("recover.relic",
        DecisionAnswerWire.ChooseOneWire("relic", "relic-1"))),
      Right(PlayerIntent.ResolveWalker(TreeDecision("recover.relic",
          ChooseOneAnswer(DecisionOptionRef.Relic(RelicId("relic-1")))))))
    assertEquals(GameIntentMapper.map(GameIntent.ResolveWalker("forge.assignment",
        DecisionAnswerWire.PartitionWire(Vector(
          DecisionPlacementWire("denizen", "d1", "pay-favor"),
          DecisionPlacementWire("denizen", "d2", "pay-secret"))))),
      Right(PlayerIntent.ResolveWalker(TreeDecision("forge.assignment", PartitionAnswer(Vector(
          DecisionPlacement(DecisionOptionRef.Denizen(DenizenId("d1")),
            "pay-favor"),
          DecisionPlacement(DecisionOptionRef.Denizen(DenizenId("d2")),
            "pay-secret")))))))
    assertEquals(GameIntentMapper.map(GameIntent.ResolveWalker("rest.distribution",
        DecisionAnswerWire.DistributeWire(Vector(
          DistributeAmountWire("favor-bank", "arcane", 0),
          DistributeAmountWire("favor-bank", "nomad", 3))))),
      Right(PlayerIntent.ResolveWalker(TreeDecision("rest.distribution", DistributeAnswer(Vector(
          DistributeAmount(DecisionOptionRef.FavorBank(Suit.Arcane), 0),
          DistributeAmount(DecisionOptionRef.FavorBank(Suit.Nomad), 3)))))))

  test("an unknown option kind is rejected without throwing"):
    val failure = rejected(GameIntentMapper.map(GameIntent.ResolveWalker("recover.choice",
        DecisionAnswerWire.ChooseOneWire("teleport", "x"))))
    assertEquals(failure.path, "$.intent.payload.option")

  test("a blank relic id is refused with a typed failure"):
    val failure = rejected(GameIntentMapper.map(GameIntent.ResolveWalker("recover.relic",
        DecisionAnswerWire.ChooseOneWire("relic", ""))))
    assertEquals(failure.path, "$.intent.payload.option")

  test("an unplaceable option inside a partition answer is rejected with " +
      "the placement path, not the choose-one path"):
    val failure = rejected(GameIntentMapper.map(GameIntent.ResolveWalker("forge.assignment",
        DecisionAnswerWire.PartitionWire(Vector(
          DecisionPlacementWire("warband", "w1", "pay-favor"))))))
    assertEquals(failure.path, "$.intent.payload.placements.option")

  test("malformed actorless requests retain typed paths"):
    assertEquals(rejected(GameHttpWire.decodeCommand("{")).path, "$")
    val missing = """{"expectedNextSequence":8,"intent":{"type":"requestConsent",""" +
      """"decisionId":"travel.destination","target":{"optionKind":"site"}}}"""
    assertEquals(rejected(GameHttpWire.decodeCommand(missing)).path,
      "$.intent.target[0].optionId")

  /** Every gateway failure has one public spelling. Only an invalid intent
    * names where it went wrong; a refusal that could carry a storage detail
    * is an internal error. */
  test("the one error table spells every gateway failure"):
    import GatewayFailure._
    def spelled(failure: GatewayFailure) = GameHttpWire.publicError(failure)
    assertEquals(spelled(Access(AccessRefusal.Forbidden)),
      (StatusCodes.Forbidden, "forbidden", "access is denied"))
    assertEquals(spelled(Access(AccessRefusal.Unauthenticated)),
      (StatusCodes.Unauthorized, "authentication-required",
        "valid authentication is required"))
    assertEquals(spelled(Access(AccessRefusal.Malformed)),
      (StatusCodes.BadRequest, "malformed-request", "the request is invalid"))
    assertEquals(spelled(Malformed), spelled(Access(AccessRefusal.Malformed)))
    assertEquals(spelled(InvalidIntent("$.intent.choice", "invalid")),
      (StatusCodes.BadRequest, "malformed-request", "$.intent.choice: invalid"))
    assertEquals(spelled(Application(GameApplicationError.StreamNotFound("g"))),
      (StatusCodes.NotFound, "stream-not-found",
        "the requested game does not exist"))
    assertEquals(spelled(Application(
      GameApplicationError.StaleClientPosition(1L, 2L)))._1, StatusCodes.Conflict)
    assertEquals(spelled(Application(
      GameApplicationError.SequenceConflict(1L, 2L)))._1, StatusCodes.Conflict)
    assertEquals(spelled(Application(GameApplicationError.DuplicateGame("g"))),
      (StatusCodes.Conflict, "duplicate-game", "the game already exists"))
    assertEquals(spelled(Application(GameApplicationError.CommandRejected(
      OathViolation.ConsentPending))),
      (StatusCodes.UnprocessableContent, "command-rejected",
        "Waiting for an answer to a consent request"))
    val internal = (StatusCodes.InternalServerError, "internal-error",
      GameHttpWire.internalErrorMessage)
    assertEquals(spelled(Access(AccessRefusal.Storage)), internal)
    assertEquals(spelled(Application(
      GameApplicationError.StorageFailure("the database path"))), internal)
