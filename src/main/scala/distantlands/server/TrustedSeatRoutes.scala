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
  private val api = new GameApiRoutes(seats, gateway, blockingExecutionContext)
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
            case Right(seat) if DevelopmentTrustBoundary.validateIdentifier(seat.gameId, "$.gameId").isRight =>
              val path = canonicalPath(seat.gameId)
              val cookie = HttpCookie(seats.cookieName, code.raw, maxAge = Some(31536000L),
                path = Some(path), secure = publicBaseUrl.getScheme.equalsIgnoreCase("https"),
                httpOnly = true).withSameSite(SameSite.Lax)
              HttpResponse(StatusCodes.SeeOther, headers = List(canonicalLocation(seat.gameId), `Set-Cookie`(cookie)))
            case Left(AccessRefusal.Storage) => internalError()
            case _ => invalidLink
      } } }
    } ~ pathPrefix("games" / Segment) { gameId =>
      noQuery:
        if DevelopmentTrustBoundary.validateIdentifier(gameId, "$.gameId").isLeft then complete(malformed)
        else extractRequest { request =>
          pathEnd {
            get:
              // The page is served to a seat that can load its game; any
              // other holder is sent back to its seat link.
              onComplete(seats.resolve(request, gameId).map(_.left
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
              headers = List(canonicalLocation(gameId)))) }
          } ~ pathPrefix("api"):
            // Only a mutation passes the same-origin check; the API itself
            // is the shared one, with the seat cookie as its actor.
            get { api.route(gameId) } ~ post { sameOrigin { api.route(gameId) } }
        }
    }

  private def canonicalPath(gameId: String): String =
    "/games/" + URLEncoder.encode(gameId, StandardCharsets.UTF_8)

  // Preserve the same escaped path used by encodeURIComponent and cookie matching.
  // Rendering a modeled URI may normalize percent-encoded colons back to literals.
  private def canonicalLocation(gameId: String): RawHeader =
    RawHeader("Location", canonicalPath(gameId))

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
