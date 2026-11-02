package oathdigital.server

import oathdigital.application._
import oathdigital.model.PlayerId
import oathdigital.protocol._
import oathdigital.protocol.projection.{GameProjection, LogPage}

sealed trait TrustedSeatFailure extends Product with Serializable
object TrustedSeatFailure:
  case object Forbidden extends TrustedSeatFailure
  case object InvalidIntent extends TrustedSeatFailure
  case object StorageFailure extends TrustedSeatFailure
  final case class Application(error: GameApplicationError) extends TrustedSeatFailure

/** The seat transport's view of `GameGateway`: a `TrustedSeat` resolved from
  * its cookie becomes the seat's access, for its own game only. */
final class TrustedGameGateway(service: GameApplicationService, projector: GameProjector):
  import TrustedSeatFailure._

  private val gateway = new GameGateway(service, projector)

  private def access(gameId: String, seat: TrustedSeat)
      : Either[TrustedSeatFailure, GameAccess] =
    Either.cond(gameId == seat.gameId, GameAccess.Seat(PlayerId(seat.playerId)),
      Forbidden)

  def load(gameId: String, seat: TrustedSeat): Either[TrustedSeatFailure, GameProjection] =
    access(gameId, seat).flatMap(gateway.load(gameId, _).left.map(failure))

  /** The seat's log page. A cursor past the journal's end is an invalid
    * request, which the route reports as malformed. */
  def log(gameId: String, seat: TrustedSeat, after: Long)
      : Either[TrustedSeatFailure, LogPage] =
    access(gameId, seat).flatMap(gateway.log(gameId, _, after).left.map(failure))

  def submit(gameId: String, seat: TrustedSeat, request: ActorlessCommandRequest)
      : Either[TrustedSeatFailure, GameProjection] =
    access(gameId, seat).flatMap(gateway.submit(gameId, _, request)
      .left.map(failure))

  private def failure(gatewayFailure: GatewayFailure): TrustedSeatFailure =
    gatewayFailure match
      case GatewayFailure.Application(error) => Application(error)
      case GatewayFailure.Access(_) => Forbidden
      case GatewayFailure.InvalidIntent(_, _) | GatewayFailure.Malformed =>
        InvalidIntent
