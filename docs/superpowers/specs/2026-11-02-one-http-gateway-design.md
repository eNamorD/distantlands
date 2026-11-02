# One HTTP gateway

> Status: design note, 2026-11-02, for the roadmap's architecture candidate
> "One HTTP gateway" (architecture scans of 2026-10-23). It re-measures the
> candidate at `0d1ae92`, describes the three transports and the service
> calls each writes, defines the seam for actor resolution, settles the
> public-only projection scope and `SearchDrawPort`, and slices the change.
> Slice 0 (the command vocabulary note) and slice 1 (the `SearchDrawPort`
> fold) landed on 2026-11-02; slices 2 to 5 are not implemented. Companion
> notes: the [command vocabulary](2026-11-02-one-command-vocabulary-design.md),
> which landed first, and the [wire codecs](2026-11-02-derive-wire-codecs-design.md).

Vocabulary: a **transport** is one set of HTTP routes plus the gateway
class behind it that turns a request into service calls. The **actor** is
the player a transport resolves a request to. A **seat** is the trusted
alpha's per-player credential (`TrustedSeat(gameId, playerId)`, exchanged
from an `/s/{code}` link into a path-scoped cookie). The **public-only
scope** is `ProjectionScope.PublicOnly`: a projection with no viewer, which
`GameProjector.projectPublic` builds by passing `None` as the requesting
player to `ScopedProjectionContext`.

## The numbers, at `0d1ae92`

### The three transports

| | development | trusted seat | authenticated |
|---|---|---|---|
| routes | `GameRoutes` (256 lines) | `TrustedSeatRoutes` (179) | `AuthenticatedGameRoutes` (302, with its gateway) |
| gateway | `GameServerGateway` (in `GameRoutes.scala`) | `TrustedGameGateway` (46) | `AuthenticatedGameGateway` (in `AuthenticatedGameRoutes.scala`) |
| URL | `/api/dev/first-games/{gameId}` `?playerId=` | `/games/{gameId}/api` | `/api/authenticated/first-games/{gameId}` |
| actor from | the `playerId` query parameter, `DevelopmentTrustBoundary.validateIdentifier` | the `oath_seat` cookie, `IdentityRepository.resolveTrustedSeat`, game id checked | the session cookie, `SessionCookieAuthenticator`, then `MembershipAuthorizationService` |
| mounted | development mode only | both modes; the only transport in trusted alpha | development mode, only when `OATH_SESSION_COOKIE_NAME` and `OATH_AUTHENTICATED_PUBLIC_ORIGIN` are both set |
| frontend client | `HttpGameClient` | `TrustedHttpGameClient` | none |
| extras | `/events` (raw history), the development shell | `/s/{code}`, `POST /games`, page redirects, cookie issue | `/bootstrap` |
| route tests | 4 (`GameRoutesSuite`) + 1 (`GameTrustBoundaryRoutesSuite`) | 18 (`TrustedSeatRoutesSuite`) | 5 (`AuthenticatedGameRoutesSuite`) + 7 (`AuthenticatedGameBootstrapRoutesSuite`) |

Counted from `ServerRoutes.route`, `GameClient.scala` and
`grep -c 'test(' src/test/scala/oathdigital/server/*.scala`.

### The service calls each writes

| call | development | trusted seat | authenticated |
|---|---|---|---|
| `service.load` | 1 | 1 | 1 |
| `service.handle` | 1 | 1 | 2 (submit, bootstrap) |
| `service.history` | 1 | 1 | 0 |
| `service.rawEventHistory` | 1 | 0 | 0 |
| `projector.project` | 2 | 2 | 2 |
| `projector.projectPublic` | 0 | 0 | 2 (spectator load, bootstrap) |
| `projector.logPage` | 1 | 1 | 0 |
| `GameIntentMapper.bind` | 1 | 1 | 1 |

Counted with `grep -oE 'service\.[a-zA-Z]+|projector\.[a-zA-Z]+|
GameIntentMapper\.bind'` over the three files. Load, submit and log are the
same three sequences three times, differing only in how the actor arrives
and how a failure is spelled:

| duplicated piece | lines |
|---|---|
| `publicError` table (`GameRoutes`) | 57 |
| `publicError` table (`AuthenticatedGameRoutes`) | 56 |
| `publicError` (`TrustedSeatRoutes`) | 31 |
| failure types | `GameApplicationError` wrapped three ways: directly, as `TrustedSeatFailure.Application`, as `AuthenticatedGameFailure.Application` |
| error codes spelled by hand | `stream-not-found`, `stale-client-position`, `sequence-conflict`, `command-rejected`, `duplicate-game`, `internal-error`, `malformed-request`, `forbidden`, `csrf-validation-failed`, in two or three files each |
| `completeAsync` / `async` wrappers | 3 |

`CommandRejectionMessage` is the one piece already shared.

### Two facts the roadmap did not record

- **No session issuer exists.** `IdentityRepository.createSession` is
  declared and implemented in `HsqldbIdentityRepository`, and nothing in
  `src/main` calls it (`grep -rn createSession src/main` finds only the
  declaration, the implementation and the owner's delegation). The
  authenticated stack can be reached only by a test that seeds a session
  row. `DevelopmentIdentityShim`, the header-based authenticator meant to
  stand in for one, is not mounted anywhere; its three tests are its only
  callers.
- **`SearchDrawPort` is checked against itself.**
  `GameApplicationService` compares `searchDrawPort.prepare(ready, source)`
  with `SearchRules.draw(ready, source)` and refuses a mismatch; the default
  and only production port, `SearchDrawPort.authoritative`, is
  `SearchRules.draw`. The comparison is a function against itself. Its two
  tests are `SearchDrawPortContract` (the authoritative port draws from the
  right pile) and one test in `GameApplicationServiceActionsSuite` that
  injects a tampering port to see the refusal.

## What exists

`ServerRoutes.route` assembles the mode. Development mounts
`DevelopmentRoutes` (the dev API plus the shell), then the trusted seat
routes, then, only with both environment variables, the authenticated
routes. Trusted alpha mounts the trusted seat routes and the production
frontend. The trusted seat transport is therefore in production and in
development; the dev transport is what a developer's browser uses from the
shell (`HttpGameClient` with a seat selector); the authenticated transport
is a stub kept for **X6** (OIDC, session issuance), with the membership
authorization it would need already written and tested
(`MembershipAuthorizationService`, 3 tests).

What each gateway does with the actor once resolved is identical:
`GameIntentMapper.bind(player, request.intent)`, `service.handle(gameId,
request.expectedNextSequence, command)`, `projector.project(gameId,
accepted.loaded, player)`. `TrustedGameGateway` additionally sets
`viewerPlayerId = Some(player.value)` on the projection, which
`GameServerGateway` does not (the dev client knows its own selection) and
`AuthenticatedGameGateway` does not either.

The one behaviour only the authenticated transport has:
`authorizeProjection` returns `PlayerPrivate(playerId)` for a player
membership and `PublicOnly` for an owner or spectator membership, and
`load` picks `project` or `projectPublic` on it. `bootstrap` also projects
publicly, since the owner who starts a game holds no seat. No other
transport can produce a viewer who is not a player, so no other transport
needs the branch today.

## The target

### The seam

```scala
// server/GameAccess.scala
/** Who a request is, for one game: the seat it holds, or a view with no
  * seat. Resolved by the transport, before any service call. */
enum GameAccess:
  case Seat(player: PlayerId)
  case Public

sealed trait AccessRefusal
object AccessRefusal:
  case object Forbidden extends AccessRefusal          // 403
  case object Unauthenticated extends AccessRefusal    // 401
  case object Malformed extends AccessRefusal          // 400
  final case class Storage(reference: String) extends AccessRefusal  // 500

/** How a transport resolves the actor. The gateway never sees a cookie,
  * a header or a query parameter. */
trait ActorResolution:
  def resolve(request: HttpRequest, gameId: String)
      : Future[Either[AccessRefusal, GameAccess]]
```

Three implementations, each a few lines of code that exist today:

- `QueryParameterActor`: `?playerId=` validated by
  `DevelopmentTrustBoundary.validateIdentifier`, always a `Seat`.
- `SeatCookieActor`: the `oath_seat` cookie, `SeatCode.parse`,
  `resolveTrustedSeat`, `seat.gameId == gameId`, always a `Seat`;
  `Forbidden` otherwise. This is `TrustedSeatRoutes.authenticate` and
  `TrustedGameGateway.actor` moved behind the trait.
- `SessionMembershipActor`: `SessionCookieAuthenticator`, then
  `MembershipAuthorizationService.resolve`; `Player` → `Seat`, `Owner` or
  `Spectator` → `Public`. Not mounted until X6 issues a session.

### The gateway

```scala
final class GameGateway(service: GameApplicationService, projector: GameProjector):
  def load(gameId: String, access: GameAccess): Either[GameApplicationError, GameProjection]
  def submit(gameId: String, access: GameAccess, request: ActorlessCommandRequest)
      : Either[GatewayFailure, GameProjection]
  def log(gameId: String, access: GameAccess, after: Long)
      : Either[GatewayFailure, LogPage]
```

`load` with `Seat(player)` is `projector.project(…, player)` with
`viewerPlayerId` set; with `Public` it is `projector.projectPublic`. The
public-only scope stops being the one behaviour a transport has and becomes
one arm of the gateway that every transport reaches when its resolution
says `Public`. `submit` with `Public` is `Forbidden`: a view with no seat
issues no command. `log` with `Public` is a question (below); the default
is to page the log as a viewer with no private lines, which
`GameLogProjector.page` can take as `Option[PlayerId]` the way
`ScopedProjectionContext` already does.

`GatewayFailure` is one type: `Access(AccessRefusal)`,
`Application(GameApplicationError)`, `InvalidIntent(path, message)`,
`Malformed`. One `publicError` table in one file, `GameHttpWire`, maps it
to status, code and message, and the three tables above go.

### The routes

One `GameApiRoutes(resolution: ActorResolution, gateway: GameGateway,
blockingExecutionContext)` serves `GET {prefix}`, `POST {prefix}/commands`
and `GET {prefix}/log/{after}` under a prefix the caller gives. What stays
transport-specific stays in its own file and composes around it:

- `TrustedSeatRoutes`: `POST /games`, `GET /s/{code}` (cookie issue),
  `GET /games/{gameId}` (page or recovery), the slash redirect, `noQuery`,
  `sameOrigin`, the private headers. Its `pathPrefix("api")` arm becomes
  `GameApiRoutes(SeatCookieActor, …)`.
- `DevelopmentRoutes`: `/events` and the shell; its API arm becomes
  `GameApiRoutes(QueryParameterActor, …)` at the existing dev prefix.
- `AuthenticatedGameRoutes`: either deleted down to `SessionMembershipActor`
  plus the bootstrap route, or deleted entirely (question 1).

The two frontend clients differ only in URL and the query parameter. Once
the dev prefix serves the same three endpoints through the same gateway,
`HttpGameClient` can become `TrustedHttpGameClient` with an optional
`?playerId=`, or the dev shell can mount at `/games/{gameId}/api` with the
query parameter resolution and the second client goes. That is the last
slice and is optional.

### `SearchDrawPort`

Fold it away with the service arm it guards. Delete the trait, its
`authoritative` object, the constructor parameter, the Search source arm in
`applyUnblockedCommand` (`GameApplicationService.scala:483-499`), the
`SearchDrawPortContract` and the tampering test. `OathViolation.
SearchDrawMismatch` stays: `SearchProcedure.scala:118` raises it inside the
walker for the real check, that a prepared draw's cards are the pile's.
`SearchSourceServiceSuite` and the Search suites in `gameplay` keep the
behaviour pinned; the deleted test proved only that a port the production
code never constructs would be refused. This is independent of the gateway
and can be the first slice.

## Slices

Each slice is one session, independently mergeable, gated by the full gate
(`./sbtw -batch test frontend/test frontend/fullOptJS`, the architecture
and link checks, `git diff --check`).

0. **Prerequisite.** The command vocabulary note's slices 1 and 2:
   `GameCommand.Issued(actor, intent)` and a mapper without an actor. The
   gateway's `submit` is then `Issued(player, mapped)` with no
   `AuthorizedPlayer`.
1. **`SearchDrawPort` fold.** As above. Pins: `SearchSourceServiceSuite`,
   the `gameplay` Search suites, `GameApplicationServiceActionsSuite` minus
   the tampering test. Production files touched: `GameRandomPorts.scala`,
   `GameApplicationService.scala`.
2. **`GameGateway` and `GameAccess`, behind the existing routes.** Add the
   gateway and the seam; make `TrustedGameGateway` and `GameServerGateway`
   thin delegates to it (keeping their names and failure types for this
   slice) so no route and no route test changes. Pins: all 18 + 4 + 5 route
   tests unchanged; one new `GameGatewaySuite` with three tests: a `Seat`
   load carries `viewerPlayerId`, a `Public` load has none and no private
   cards (the redaction `GameServerGatewaySubmitBeginSuite` already
   asserts, moved here), a `Public` submit is `Forbidden`.
3. **`GameApiRoutes` and one error table.** Replace the three API arms with
   `GameApiRoutes` and the three resolutions; delete `TrustedGameGateway`,
   `GameServerGateway`, `TrustedSeatFailure.Application`,
   `AuthenticatedGameFailure.Application` and the three `publicError`
   tables; `GameHttpWire` holds the one table. URLs unchanged. Pins: the
   same route tests, which assert status codes and bodies; a table test in
   `GameHttpWireSuite` over every `GatewayFailure` case.
4. **The authenticated stack.** Per question 1: delete
   `AuthenticatedGameRoutes`, `AuthenticatedGameGateway`,
   `AuthenticatedGameHttpWire` and `DevelopmentIdentityShim` with their
   suites and the two environment variables, keeping
   `SessionCookieAuthenticator`, `MembershipAuthorizationService`,
   `SessionMembershipActor` (new, 20 lines, unit-tested with a seeded
   session) and the bootstrap logic as a `GameGateway.bootstrap` with no
   route. X6 then adds a session issuer and mounts
   `GameApiRoutes(SessionMembershipActor, …)` plus a bootstrap route. Pins:
   `MembershipAuthorizationServiceSuite`, `SessionSecuritySuite`, a new
   `SessionMembershipActorSuite`, `ServerRoutesSuite` (the authenticated
   mount test is deleted with the mount).
5. **One frontend client (optional).** Mount the dev API at
   `/games/{gameId}/api` with `QueryParameterActor`; delete
   `HttpGameClient`; `TrustedHttpGameClient` takes an optional player
   selection for the query. Pins: `HttpGameClientSuite`,
   `ServerModeSessionSuite`, the jsdom renderer suites. Wire formats may
   change in place before public release (standing rules), and the dev
   URL is not a published one, but the seat links in the hands of alpha
   testers point at `/s/{code}`, which does not move.

## Risks

- **Cookie and CSRF behaviour is security-sensitive.** `TrustedSeatRoutes`'
  `noQuery`, `sameOrigin`, `Secure`/`HttpOnly`/`SameSite=Lax`, the private
  headers and "never log a credential" rules are each pinned by one of its
  18 tests. Slice 3 moves the API arm only, not the cookie exchange, and
  keeps the directives around `GameApiRoutes`; nothing in the gateway reads
  a header. A regression here would show as a failing test, which is the
  point of leaving the URL and the tests alone through slices 2 and 3.
- **A `Public` view reaching a seat transport.** Today it cannot; after the
  seam a future resolution could return `Public` for a seat-less link. The
  gateway's `Public` arms are the redaction boundary and must be tested on
  their own (slice 2), not through a transport.
- **Deleting the authenticated stack loses tested code X6 wants.** The
  parts X6 needs (`MembershipAuthorizationService`,
  `SessionCookieAuthenticator`, `validateSeats`) are kept with their tests;
  only the routes, the second gateway and the unmounted shim go. The
  deleted route tests are the kind X6 writes again against the one
  `GameApiRoutes`.
- **Error wording.** The three tables disagree in places (`duplicate-game`
  reads "the game already exists" in two and "the game event stream already
  exists" in one; the trusted table hides `InvalidIntent` as `malformed`
  where the others give a path). One table means one wording; slice 3 picks
  the trusted alpha's, since it is the one players see, and the route
  tests that assert the other wordings change in the same commit.

## Recommendation

Do it: slices 1 to 4, in that order, after the command vocabulary's first
two slices. Three copies of load/submit/log and three error tables, for a
server whose only production transport is the seat one, is the clearest
duplication of the three candidates, and the seam is small and already
implied by the code (`TrustedGameGateway.actor` and
`TrustedSeatRoutes.authenticate` are the seat resolution; the authenticated
gateway's `scope match` is the public arm). `SearchDrawPort` is a deletion
with no design in it. Slice 5 is worth doing only if the dev shell is kept
long term; leave it unless someone edits the clients for another reason.

## Dependencies

- After the [command vocabulary note](2026-11-02-one-command-vocabulary-design.md)
  (slices 1 and 2 of it), for `Issued(actor, intent)`.
- Independent of the [wire codecs note](2026-11-02-derive-wire-codecs-design.md).
- Slice 4 shapes **X6**: the roadmap's "complete production authentication"
  item would then read "issue sessions and mount `GameApiRoutes` with
  `SessionMembershipActor`", and the item's note about loopback-only
  development routes still holds.

## Questions for the owner

1. **Delete the authenticated routes now (slice 4), or re-express them as a
   third resolution and keep them mounted in development?** Recommended
   default: delete the routes and keep the parts; nothing can obtain a
   session, so the mounted stack is dead code with twelve tests, and X6
   will write the routes against the one gateway.
2. **May a `Public` view read the log?** Recommended default: yes, as a
   viewer with no private lines, since the projection is already public;
   the log projector takes the viewer as `Option[PlayerId]`. If no, the
   gateway answers `Forbidden` and the decision is one arm.
3. **Should the dev shell move to `/games/{gameId}/api?playerId=` and the
   second frontend client go (slice 5)?** Recommended default: not now; the
   dev prefix is unpublished and the gain is one client class, but it is a
   frontend edit with its own jsdom suites and belongs with a frontend
   session.
4. **Which error wording wins when the three tables disagree?**
   Recommended default: the trusted seat's, except that `InvalidIntent`
   keeps its path and message as the other two tables give it, since a
   developer is the one who sees it.
5. **Fold `SearchDrawPort` away?** Recommended default: yes, first; the only
   port is the function it is compared against, and the walker's own
   `SearchDrawMismatch` check is the real one.
