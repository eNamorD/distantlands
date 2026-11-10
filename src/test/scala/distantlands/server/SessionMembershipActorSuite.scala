package distantlands.server

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Cookie

import distantlands.application._
import distantlands.application.MembershipRole._
import distantlands.model.PlayerId
import distantlands.testkit.{HttpTestServer, IdentityRepositoryStub}

/** The session resolution X6 will mount: a session cookie names a user, and
  * the user's membership in the game is their access. */
class SessionMembershipActorSuite extends munit.FunSuite:
  private val cookieName = "oath_session"
  private val owner = UserId("owner")
  private val player = UserId("player")
  private val spectator = UserId("spectator")
  private val outsider = UserId("outsider")

  /** One live session per user: the raw token is the user's name padded to
    * the shortest accepted length. */
  private def token(user: UserId): String =
    user.value.padTo(43, 't')
  private def digestOf(raw: String): SessionTokenDigest =
    SessionTokenDigest.fromBytes(SensitiveTokenDigest.sha256(raw))
      .fold(error => throw new IllegalStateException(error), identity)
  private val sessions = Vector(owner, player, spectator, outsider)
    .map(user => digestOf(token(user)) -> user).toMap
  private val memberships = Map(
    owner -> GameMembership("game-1", owner, Owner, None),
    player -> GameMembership("game-1", player, Player, Some("p1")),
    spectator -> GameMembership("game-1", spectator, Spectator, None))

  /** Stub of IdentityRepository: the four sessions above, and the three
    * memberships in "game-1". Every other game fails as `otherGames` says. */
  private def identities(otherGames: Either[IdentityFailure, Option[GameMembership]]) =
    new IdentityRepositoryStub:
      override def resolveSession(digest: SessionTokenDigest, now: Long) =
        sessions.get(digest).toRight(IdentityFailure.SessionNotFound).map(user =>
          StoredSession(digest, user, 0L, 0L, 300L, 600L, None,
            Some(HttpTestServer.csrfDigest)))
      override def findMembership(gameId: String, userId: UserId) =
        if gameId == "game-1" then Right(memberships.get(userId)) else otherGames

  private def resolved(repository: IdentityRepository, gameId: String,
      cookie: Option[String]): Either[AccessRefusal, GameAccess] =
    val actor = new SessionMembershipActor(
      new SessionCookieAuthenticator(repository, cookieName, () => 150L,
        ExecutionContext.global),
      new MembershipAuthorizationService(repository), ExecutionContext.global)
    val request = HttpRequest(headers =
      cookie.map(value => Cookie(cookieName, value)).toList)
    Await.result(actor.resolve(request, gameId), 5.seconds)

  test("a player's session is its seat; an owner's or a spectator's is a " +
      "public view; anyone else is refused"):
    val repository = identities(Right(None))
    assertEquals(resolved(repository, "game-1", Some(token(player))),
      Right(GameAccess.Seat(PlayerId("p1"))))
    assertEquals(resolved(repository, "game-1", Some(token(owner))),
      Right(GameAccess.Public))
    assertEquals(resolved(repository, "game-1", Some(token(spectator))),
      Right(GameAccess.Public))
    assertEquals(resolved(repository, "game-1", Some(token(outsider))),
      Left(AccessRefusal.Forbidden))
    assertEquals(resolved(repository, "game-2", Some(token(player))),
      Left(AccessRefusal.Forbidden))

  test("no session, an unknown session and a storage failure are told " +
      "apart, and the raw token never reaches the repository"):
    val repository = identities(Left(IdentityFailure.StorageFailure("disk")))
    assertEquals(resolved(repository, "game-1", None),
      Left(AccessRefusal.Unauthenticated))
    assertEquals(resolved(repository, "game-1", Some("u" * 43)),
      Left(AccessRefusal.Unauthenticated))
    assertEquals(resolved(repository, "game-2", Some(token(player))),
      Left(AccessRefusal.Storage))
    assert(!sessions.contains(digestOf("")))
