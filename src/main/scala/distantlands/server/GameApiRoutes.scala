package distantlands.server

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory

import distantlands.application.{IdentityFailure, IdentityRepository,
  TrustedSeat}
import distantlands.model.PlayerId
import distantlands.protocol.projection.LogPageCodec

/** The game API every transport mounts under its own prefix: `GET` the
  * projection, `POST commands`, `GET log/{after}`. The transport resolves
  * the actor through `resolution` and keeps whatever else it checks (query
  * refusal, same origin, private headers) around these routes. */
final class GameApiRoutes(
    resolution: ActorResolution,
    gateway: GameGateway,
    blockingExecutionContext: ExecutionContext
):
  private val logger = LoggerFactory.getLogger(classOf[GameApiRoutes])

  def route(gameId: String): Route = extractRequest { request =>
    pathEndOrSingleSlash {
      get:
        answer(request, gameId)(access => gateway.load(gameId, access)
          .map(GameHttpWire.encodeProjection))
    } ~ path("commands") {
      post:
        entity(as[String]) { body =>
          answer(request, gameId)(access => GameHttpWire.decodeCommand(body)
            .left.map(error => GatewayFailure.InvalidIntent(error.path,
              error.message))
            .flatMap(gateway.submit(gameId, access, _))
            .map(GameHttpWire.encodeProjection))
        }
    } ~ path("log" / Segment) { raw =>
      get:
        answer(request, gameId)(access => raw.toLongOption.filter(_ >= 0)
          .toRight(GatewayFailure.Malformed)
          .flatMap(gateway.log(gameId, access, _))
          .map(LogPageCodec.encode))
    }
  }

  // The resolution runs first, so a request with no access learns nothing
  // about the game or its own body; the gateway call then runs on the
  // blocking dispatcher, as every journal call does.
  private def answer(request: HttpRequest, gameId: String)(
      operation: GameAccess => Either[GatewayFailure, String]): Route =
    given ExecutionContext = blockingExecutionContext
    val response = resolution.resolve(request, gameId).flatMap {
      case Left(refusal) => Future.successful(Left(GatewayFailure.Access(refusal)))
      case Right(access) => Future(operation(access))
    }
    onComplete(response):
      case Success(Right(body)) =>
        complete(GameHttpWire.jsonResponse(StatusCodes.OK, body))
      case Success(Left(failure)) =>
        complete(GameHttpWire.failureResponse(failure, logger))
      case Failure(_) => complete(GameHttpWire.internalError(logger))

/** The development transport's actor: the `playerId` query parameter, which
  * is a selector, not a credential. Both identifiers must pass the
  * development trust boundary's shape check. */
object QueryParameterActor extends ActorResolution:
  def resolve(request: HttpRequest, gameId: String)
      : Future[Either[AccessRefusal, GameAccess]] = Future.successful(
    for
      _ <- DevelopmentTrustBoundary.validateIdentifier(gameId, "$.gameId")
        .left.map(_ => AccessRefusal.Malformed)
      playerId <- request.uri.query().get("playerId")
        .toRight(AccessRefusal.Malformed)
      valid <- DevelopmentTrustBoundary.validateIdentifier(playerId,
        "$.playerId").left.map(_ => AccessRefusal.Malformed)
    yield GameAccess.Seat(PlayerId(valid)))

/** The trusted alpha's actor: the seat the `distantlands_seat` cookie opens, for
  * the game the seat belongs to. A missing, malformed, unknown or
  * wrong-game cookie is forbidden alike, so a response never tells a
  * holder which it was. */
final class SeatCookieActor(identities: IdentityRepository,
    blockingExecutionContext: ExecutionContext) extends ActorResolution:
  val cookieName = "distantlands_seat"

  def resolve(request: HttpRequest, gameId: String)
      : Future[Either[AccessRefusal, GameAccess]] = Future {
    request.cookies.filter(_.name == cookieName).map(_.value) match
      case Seq(raw) => SeatCode.parse(raw).left.map(_ => AccessRefusal.Forbidden)
        .flatMap(seat).filterOrElse(_.gameId == gameId, AccessRefusal.Forbidden)
        .map(seat => GameAccess.Seat(PlayerId(seat.playerId)))
      case _ => Left(AccessRefusal.Forbidden)
  }(using blockingExecutionContext)

  /** The seat `code` names, looked up by its digest; the code itself never
    * reaches storage. */
  def seat(code: SeatCode): Either[AccessRefusal, TrustedSeat] =
    identities.resolveTrustedSeat(code.digest).left.map:
      case IdentityFailure.TrustedSeatNotFound => AccessRefusal.Forbidden
      case _ => AccessRefusal.Storage
