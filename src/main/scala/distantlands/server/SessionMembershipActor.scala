package distantlands.server

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.model.HttpRequest

import distantlands.application.{AuthenticationFailure, AuthorizationFailure,
  GameAccessContext, MembershipAuthorizationService}

/** The authenticated transport's actor, for when X6 issues sessions: the
  * session cookie names a user, whose membership in the game is a seat
  * (a player) or a public view (the owner, a spectator). Not mounted until
  * a session can be obtained. */
final class SessionMembershipActor(
    authenticator: HttpSessionAuthenticator,
    authorization: MembershipAuthorizationService,
    blockingExecutionContext: ExecutionContext
) extends ActorResolution:
  def resolve(request: HttpRequest, gameId: String)
      : Future[Either[AccessRefusal, GameAccess]] =
    authenticator.authenticate(request).map {
      case Left(AuthenticationFailure.StorageFailure(_)) =>
        Left(AccessRefusal.Storage)
      case Left(_) => Left(AccessRefusal.Unauthenticated)
      case Right(session) =>
        authorization.resolve(gameId, session.principal) match
          case Right(GameAccessContext.Player(_, _, playerId)) =>
            Right(GameAccess.Seat(playerId))
          case Right(_) => Right(GameAccess.Public)
          case Left(_: AuthorizationFailure.NotMember) =>
            Left(AccessRefusal.Forbidden)
          case Left(_: AuthorizationFailure.Forbidden) =>
            Left(AccessRefusal.Forbidden)
          case Left(_) => Left(AccessRefusal.Storage)
    }(using blockingExecutionContext)
