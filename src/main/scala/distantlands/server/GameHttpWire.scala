package distantlands.server

import java.util.UUID

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse,
  StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import org.slf4j.Logger

import distantlands.application.GameApplicationError
import distantlands.model.OathViolation
import distantlands.protocol.{
  ActorlessCommandCodec,
  ActorlessCommandRequest,
  FirstGameBootstrapCodec,
  FirstGameBootstrapRequest,
  ProtocolDecodeFailure
}
import distantlands.protocol.projection.{GameProjection, GameProjectionCodec}

final case class HttpInputError(path: String, message: String)

private[server] object CommandRejectionMessage:
  def text(violation: OathViolation): String =
    violation match
      case OathViolation.NegotiationUnavailable(detail) => detail
      case OathViolation.InsufficientFavor(required, available) =>
        s"required favor $required exceeds available $available"
      case OathViolation.InsufficientSupply(required, available) =>
        s"required Supply $required exceeds available $available"
      case OathViolation.InsufficientSecrets(required, available) =>
        s"required secrets $required exceed available $available"
      case OathViolation.ConsentPending =>
        "Waiting for an answer to a consent request"
      case OathViolation.ConsentUnavailable(reason) => reason
      case OathViolation.PlanAidUnavailable(reason) => reason
      // Worded as the unwaivable rejection is: the waiver is for the search.
      case OathViolation.WaivableOperationRejected(code, detail, _) =>
        OathViolation.CoreOperationRejected(code, detail).toString
      case other => other.toString

/** The wire the game API speaks, whichever transport mounts it: request
  * decoding, projection encoding and the one table of public errors. */
object GameHttpWire:
  val internalErrorMessage = "the server could not complete the request"

  def decodeBootstrap(json: String): Either[HttpInputError,
      FirstGameBootstrapRequest] =
    FirstGameBootstrapCodec.decode(json).left.map(inputError)

  def decodeCommand(json: String): Either[HttpInputError,
      ActorlessCommandRequest] =
    ActorlessCommandCodec.decode(json).left.map(inputError)

  def encodeProjection(projection: GameProjection): String =
    GameProjectionCodec.encode(projection)

  def encodeError(code: String, message: String): String =
    ujson.write(ujson.Obj("error" -> code, "message" -> message))

  /** The status, code and message a client is told for `failure`. Only an
    * invalid intent names where it went wrong; every other message is a
    * fixed phrase, and whatever may hide a credential or a storage detail is
    * an internal error. */
  def publicError(failure: GatewayFailure): (StatusCode, String, String) =
    failure match
      case GatewayFailure.Access(AccessRefusal.Forbidden) =>
        (StatusCodes.Forbidden, "forbidden", "access is denied")
      case GatewayFailure.Access(AccessRefusal.Unauthenticated) =>
        (StatusCodes.Unauthorized, "authentication-required",
          "valid authentication is required")
      case GatewayFailure.Access(AccessRefusal.Malformed) |
          GatewayFailure.Malformed => malformed
      case GatewayFailure.Access(AccessRefusal.Storage) => internal
      case GatewayFailure.InvalidIntent(path, message) =>
        (StatusCodes.BadRequest, "malformed-request", s"$path: $message")
      case GatewayFailure.Application(error) => error match
        case _: GameApplicationError.StreamNotFound =>
          (StatusCodes.NotFound, "stream-not-found",
            "the requested game does not exist")
        case _: GameApplicationError.StaleClientPosition =>
          (StatusCodes.Conflict, "stale-client-position",
            "the client position is stale; refresh and retry")
        case _: GameApplicationError.SequenceConflict =>
          (StatusCodes.Conflict, "sequence-conflict",
            "the game changed while the command was handled")
        case _: GameApplicationError.DuplicateGame =>
          (StatusCodes.Conflict, "duplicate-game", "the game already exists")
        case GameApplicationError.CommandRejected(violation) =>
          (StatusCodes.UnprocessableContent, "command-rejected",
            CommandRejectionMessage.text(violation))
        case _ => internal

  private val malformed =
    (StatusCodes.BadRequest, "malformed-request", "the request is invalid")
  private val internal =
    (StatusCodes.InternalServerError, "internal-error", internalErrorMessage)

  def errorResponse(status: StatusCode, code: String, message: String)
      : HttpResponse = jsonResponse(status, encodeError(code, message))

  def jsonResponse(status: StatusCode, body: String): HttpResponse =
    HttpResponse(status,
      entity = HttpEntity(ContentTypes.`application/json`, body))

  /** The one internal-error answer. Exception messages and request URIs can
    * contain credentials, so `logger` gets a generated reference and
    * nothing else; the response carries the same reference. */
  def internalError(logger: Logger): HttpResponse =
    val reference = UUID.randomUUID().toString
    logger.error("Request failed; reference={}", reference)
    errorResponse(StatusCodes.InternalServerError, "internal-error",
      internalErrorMessage).addHeader(RawHeader("X-Request-ID", reference))

  /** `failure` as the client sees it; an internal error is logged by
    * reference through `logger`. */
  def failureResponse(failure: GatewayFailure, logger: Logger): HttpResponse =
    publicError(failure) match
      case (StatusCodes.InternalServerError, _, _) => internalError(logger)
      case (status, code, message) => errorResponse(status, code, message)

  private[server] def inputError(error: ProtocolDecodeFailure): HttpInputError =
    HttpInputError(error.path, error.message)
