package oathdigital.server

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory

import oathdigital.application.GameApplicationService

/** The loopback development transport: the game API with the actor taken
  * from `?playerId=`, the raw event history, and the development shell. */
object DevelopmentRoutes:
  private val logger = LoggerFactory.getLogger(DevelopmentRoutes.getClass)
  private val DevelopmentAssetCacheControl = RawHeader(
    "Cache-Control",
    "no-store, no-cache, must-revalidate, max-age=0"
  )

  def route(
      service: GameApplicationService,
      gateway: GameGateway,
      blockingExecutionContext: ExecutionContext,
      serveFrontend: Boolean = true
  ): Route =
    val game = new GameApiRoutes(QueryParameterActor, gateway,
      blockingExecutionContext)
    val api = pathPrefix("api" / "dev" / "first-games" / Segment) { gameId =>
      path("events") {
        get:
          parameter("limit".as[Int].withDefault(25)) { limit =>
            rawHistory(service, gameId, limit, blockingExecutionContext)
          }
      } ~ game.route(gameId)
    }

    if !serveFrontend then api
    else
      api ~
        respondWithHeader(DevelopmentAssetCacheControl):
          pathEndOrSingleSlash {
            getFromFile("frontend/index.html")
          } ~
          path("new-game") {
            getFromFile("frontend/index.html")
          } ~
          getFromDirectory("frontend")

  /** The newest `limit` raw envelopes, a privileged development view: they
    * may reveal hidden outcomes, and the response says so. */
  private def rawHistory(service: GameApplicationService, gameId: String,
      limit: Int, blockingExecutionContext: ExecutionContext): Route =
    def malformed(message: String) = GameHttpWire.errorResponse(
      StatusCodes.BadRequest, "malformed-request", message)
    DevelopmentTrustBoundary.validateIdentifier(gameId, "$.gameId") match
      case Left(error) => complete(malformed(s"${error.path}: ${error.message}"))
      case Right(_) if limit < 1 || limit > 100 =>
        complete(malformed("$.limit: must be between 1 and 100"))
      case Right(validGameId) =>
        onComplete(Future(service.rawEventHistory(validGameId, limit))(
            using blockingExecutionContext)):
          case Success(Right(records)) =>
            complete(GameHttpWire.jsonResponse(StatusCodes.OK, ujson.write(
              ujson.Obj(
                "warning" -> "Raw authoritative events may reveal hidden outcomes.",
                "events" -> ujson.Arr.from(records.map(ujson.read(_)))))))
          case Success(Left(error)) => complete(GameHttpWire.failureResponse(
            GatewayFailure.Application(error), logger))
          case Failure(_) => complete(GameHttpWire.internalError(logger))
