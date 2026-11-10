package distantlands.server

import scala.concurrent.Future

import akka.http.scaladsl.model.HttpRequest

import distantlands.application._
import distantlands.model.PlayerId
import distantlands.protocol.ActorlessCommandRequest
import distantlands.protocol.projection.{GameProjection, LogPage}

/** Who a request is, for one game: the seat it holds, or a view with no
  * seat. A transport resolves it before any service call. */
enum GameAccess:
  case Seat(player: PlayerId)
  case Public

/** Why a transport could not resolve a request's access. */
enum AccessRefusal:
  case Forbidden, Unauthenticated, Malformed, Storage

/** How a transport resolves the actor. The gateway never sees a cookie, a
  * header or a query parameter. */
trait ActorResolution:
  def resolve(request: HttpRequest, gameId: String)
      : Future[Either[AccessRefusal, GameAccess]]

/** What a gateway call can fail with. `Malformed` is a request the gateway
  * itself refuses, such as a log cursor past the journal's end. */
enum GatewayFailure:
  case Access(refusal: AccessRefusal)
  case Application(error: GameApplicationError)
  case InvalidIntent(path: String, message: String)
  case Malformed

/** Load, submit and log for every transport, given the access a transport
  * resolved. The `Public` arms are the redaction boundary: a view with no
  * seat sees the public projection, reads the log with no private lines and
  * issues no command. */
final class GameGateway(service: GameApplicationService,
    projector: GameProjector):
  import GatewayFailure._

  def load(gameId: String, access: GameAccess)
      : Either[GatewayFailure, GameProjection] =
    loaded(gameId).map(view(gameId, _, access))

  def submit(gameId: String, access: GameAccess,
      request: ActorlessCommandRequest)
      : Either[GatewayFailure, GameProjection] = access match
    case GameAccess.Public => Left(Access(AccessRefusal.Forbidden))
    case GameAccess.Seat(player) =>
      for
        intent <- GameIntentMapper.map(request.intent).left.map(failure =>
          InvalidIntent(failure.path, failure.message))
        accepted <- service.handle(gameId, request.expectedNextSequence,
          GameCommand.Issued(player, intent)).left.map(Application.apply)
      yield view(gameId, accepted.loaded, access)

  def log(gameId: String, access: GameAccess, after: Long)
      : Either[GatewayFailure, LogPage] = for
    history <- service.history(gameId).left.map(Application.apply)
    present <- history.toRight(
      Application(GameApplicationError.StreamNotFound(gameId)))
    page <- projector.logPage(gameId, present, after, viewer(access))
      .toRight(Malformed)
  yield page

  private def loaded(gameId: String): Either[GatewayFailure, LoadedGame] =
    service.load(gameId).left.map(Application.apply).flatMap(
      _.toRight(Application(GameApplicationError.StreamNotFound(gameId))))

  // A seat's projection names its viewer, so the seat client can tell whose
  // seat the cookie opened; the public view has no viewer to name.
  private def view(gameId: String, game: LoadedGame, access: GameAccess)
      : GameProjection = viewer(access) match
    case Some(player) => projector.project(gameId, game, player)
      .copy(viewerPlayerId = Some(player.value))
    case None => projector.projectPublic(gameId, game)

  private def viewer(access: GameAccess): Option[PlayerId] = access match
    case GameAccess.Seat(player) => Some(player)
    case GameAccess.Public => None
