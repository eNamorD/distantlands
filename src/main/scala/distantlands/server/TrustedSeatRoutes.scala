package distantlands.server

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader, SameSite, `Set-Cookie`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import distantlands.application._
import distantlands.application.chronicle.ChronicleConversion
import distantlands.protocol._

final class TrustedSeatRoutes(
    identities: IdentityRepository,
    provisioning: TrustedGameProvisioning,
    gateway: GameGateway,
    chronicles: ChronicleConversion,
    publicBaseUrl: URI,
    blockingExecutionContext: ExecutionContext,
    extraOrigins: Seq[URI] = Nil
):
  private val logger = LoggerFactory.getLogger(classOf[TrustedSeatRoutes])
  private val seats = new SeatCookieActor(identities, blockingExecutionContext)
  private def api(playerId: String) =
    new GameApiRoutes(seats.at(playerId), gateway, blockingExecutionContext)
  private val privateHeaders = List(RawHeader("Cache-Control", "no-store"),
    RawHeader("Referrer-Policy", "no-referrer"))

  val route: Route = respondWithHeaders(privateHeaders):
    path("games") {
      post { noQuery { sameOrigin {
        entity(as[String]) { body =>
          TrustedGameCreateRequestCodec.decode(body) match
            case Left(_) => complete(malformed)
            case Right(request) => async:
              provisioning.create(request, publicBaseUrl.toString) match
                case Right(created) => json(StatusCodes.Created, TrustedGameCreateResponseCodec.encode(created))
                case Left(TrustedGameFailure.InvalidRequest) => malformed
                case Left(TrustedGameFailure.Refused(reason)) => error(
                  StatusCodes.UnprocessableContent, "chronicle-refused", reason)
                case Left(TrustedGameFailure.DuplicateGame) => error(StatusCodes.Conflict,
                  "duplicate-game", "the game already exists")
                case Left(_) => internalError()
        }
      } } }
    } ~ path("chronicles" / "convert") {
      post { noQuery { sameOrigin {
        entity(as[String]) { body =>
          ChronicleConvertRequestCodec.decode(body) match
            case Left(_) => complete(malformed)
            case Right(request) => async:
              val converted = request match
                case ChronicleConvertRequest.FromJson(text) =>
                  chronicles.fromJson(text)
                case ChronicleConvertRequest.FromTts(text, name) =>
                  chronicles.fromTts(text, name)
              converted.fold(
                reason => error(StatusCodes.UnprocessableContent,
                  "invalid-chronicle", reason),
                done => json(StatusCodes.OK, ChronicleConvertResponseCodec
                  .encode(ChronicleConvertResponse(done.entry, done.tts))))
        }
      } } }
    } ~ path("s" / Segment) { raw =>
      get { noQuery { async {
        SeatCode.parse(raw) match
          case Left(_) => invalidLink
          case Right(code) => seats.seat(code) match
            case Right(seat) if validIdentifiers(seat.gameId, seat.playerId) =>
              // The cookie is scoped to the seat's own page, so another
              // seat's link in the same browser sets a second cookie
              // instead of replacing this one.
              val cookie = HttpCookie(seats.cookieName, code.raw, maxAge = Some(31536000L),
                path = Some(seatPath(seat.gameId, seat.playerId)),
                secure = publicBaseUrl.getScheme.equalsIgnoreCase("https"),
                httpOnly = true).withSameSite(SameSite.Lax)
              HttpResponse(StatusCodes.SeeOther, headers = List(
                seatLocation(seat.gameId, seat.playerId), `Set-Cookie`(cookie)))
            case Left(AccessRefusal.Storage) => internalError()
            case _ => invalidLink
      } } }
    } ~ pathPrefix("games" / Segment / "seats" / Segment) { (gameId, playerId) =>
      noQuery:
        if !validIdentifiers(gameId, playerId) then complete(malformed)
        else extractRequest { request =>
          pathEnd {
            get:
              // The page is served to the seat that can load its game; any
              // other holder is sent back to its seat link.
              onComplete(seats.at(playerId).resolve(request, gameId).map(_.left
                .map(GatewayFailure.Access.apply)
                .flatMap(gateway.load(gameId, _)))(using blockingExecutionContext)):
                case Success(Right(_)) => ProductionFrontendRoutes.gamePage
                case Success(Left(GatewayFailure.Access(AccessRefusal.Forbidden))) =>
                  complete(recovery)
                case Success(Left(GatewayFailure.Application(_: GameApplicationError.StreamNotFound))) =>
                  complete(recovery)
                case _ => complete(internalError())
          } ~ pathSingleSlash {
            get { complete(HttpResponse(StatusCodes.SeeOther,
              headers = List(seatLocation(gameId, playerId)))) }
          } ~ pathPrefix("api"):
            // Only a mutation passes the same-origin check; the API itself
            // is the shared one, with the seat's cookie as its actor.
            get { api(playerId).route(gameId) } ~
              post { sameOrigin { api(playerId).route(gameId) } }
        }
    } ~ pathPrefix("games" / Segment) { _ =>
      // A game's own path names no seat, so a bookmark of it is sent back
      // to its seat link.
      get { complete(recovery) }
    }

  private def validIdentifiers(gameId: String, playerId: String): Boolean =
    DevelopmentTrustBoundary.validateIdentifier(gameId, "$.gameId").isRight &&
      DevelopmentTrustBoundary.validateIdentifier(playerId, "$.playerId").isRight

  private def seatPath(gameId: String, playerId: String): String =
    "/games/" + URLEncoder.encode(gameId, StandardCharsets.UTF_8) +
      "/seats/" + URLEncoder.encode(playerId, StandardCharsets.UTF_8)

  // Preserve the same escaped path used by encodeURIComponent and cookie matching.
  // Rendering a modeled URI may normalize percent-encoded colons back to literals.
  private def seatLocation(gameId: String, playerId: String): RawHeader =
    RawHeader("Location", seatPath(gameId, playerId))

  private def noQuery(inner: => Route): Route = parameterMap { parameters =>
    if parameters.nonEmpty then complete(malformed) else inner
  }

  private def sameOrigin(inner: => Route): Route = extractRequest { request =>
    val origins = request.headers.filter(_.is("origin")).map(_.value)
    if origins.isEmpty || (origins.size == 1 && originMatches(origins.head)) then inner
    else complete(error(StatusCodes.Forbidden, "csrf-validation-failed", "request origin is invalid"))
  }

  private def originMatches(raw: String): Boolean = Try(new URI(raw)).toOption.exists { origin =>
    def port(uri: URI): Int = if uri.getPort >= 0 then uri.getPort
      else if uri.getScheme.equalsIgnoreCase("https") then 443 else 80
    origin.isAbsolute && origin.getHost != null && origin.getRawUserInfo == null &&
      origin.getRawQuery == null && origin.getRawFragment == null &&
      Option(origin.getRawPath).forall(_.isEmpty) &&
      (publicBaseUrl +: extraOrigins).exists(allowed =>
        origin.getScheme.equalsIgnoreCase(allowed.getScheme) &&
          origin.getHost.equalsIgnoreCase(allowed.getHost) && port(origin) == port(allowed))
  }

  private def async(operation: => HttpResponse): Route =
    onComplete(Future(operation)(using blockingExecutionContext)):
      case Success(response) => complete(response)
      case Failure(_) => complete(internalError())

  private def internalError(): HttpResponse = GameHttpWire.internalError(logger)

  private def invalidLink: HttpResponse = HttpResponse(StatusCodes.NotFound,
    entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
      "<!doctype html><html lang=\"en\"><title>Invalid seat link</title><body><h1>Invalid seat link</h1><p>Open the assigned seat link from your host.</p></body></html>"))
  private def recovery: HttpResponse = HttpResponse(StatusCodes.Forbidden,
    entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
      "<!doctype html><html lang=\"en\"><title>Open your seat link</title><body><h1>Open your seat link</h1><p>Open the assigned seat link from your host to return to your game.</p></body></html>"))
  private def malformed: HttpResponse = error(StatusCodes.BadRequest, "malformed-request", "the request is invalid")
  private def error(status: StatusCode, code: String, message: String): HttpResponse =
    GameHttpWire.errorResponse(status, code, message)
  private def json(status: StatusCode, body: String): HttpResponse =
    GameHttpWire.jsonResponse(status, body)
