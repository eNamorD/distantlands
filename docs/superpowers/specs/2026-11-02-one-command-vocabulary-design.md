# One command vocabulary

> Status: implemented, 2026-11-02, in three commits on the day it was
> written, one per slice below, with the recommended defaults for every
> question for the owner: `Begin` stays a `GameCommand` beside `Issued`,
> the test aliases were retired through `testkit.Commands`, and
> `PlayerIntent` is a top-level trait in `GameCommands.scala`. The counts
> below are as measured at `0d1ae92`, before the change; at the sweep the
> test construction sites had grown to 221 in 56 files. Written for the
> roadmap's architecture candidate "One command vocabulary" (architecture
> scans of 2026-10-23). It re-measures the candidate at `0d1ae92`, names
> the seven files a command is written in, defines "an actor plus a
> model-typed intent", shows one command before and after, and slices the
> change. Companion notes: the
> [wire codecs](2026-11-02-derive-wire-codecs-design.md) and the
> [HTTP gateway](2026-11-02-one-http-gateway-design.md).

Vocabulary: the [undo design](2026-10-06-undo-design.md) defines **command
batch** and **issuer**. The [consent design](2026-10-08-consent-system-design.md)
defines **requester** and **granter**. A **wire intent** is a case of
`protocol.GameIntent`: actorless, every identifier an opaque string. A
**command** is a case of `application.GameCommand`: the actor bound, every
identifier a model type. The **actor** is the player a transport resolved
the request to, which `CONTEXT.md` and the codebase-structure doc call the
one fact the server binds and the client never sends.

## The numbers, at `0d1ae92`

| measure | value | how counted |
|---|---|---|
| wire intents | 6: `Undo`, `Redo`, `ResolveWalker`, `RequestConsent`, `AnswerConsent`, `SetConsentPreference` | `CommandIntents.scala` |
| commands | 7: the six above plus `Begin` | `GameCommands.scala` |
| files a wire intent is written in | 7 | listed below |
| mapper arms | 6 | `GameIntentMapper.bind` |
| `AuthorizedPlayer` builders | 6 | `Authorization.scala:54-66` |
| callers of `AuthorizedPlayer.forPlayer` | 1 (`GameIntentMapper.bind`) | `grep -rn forPlayer src/main` |
| callers of `MembershipAuthorizationService.authorizeCommand` | 1 (`AuthenticatedGameGateway.submit`), which then reads only `.access.playerId` | `grep -rn authorizeCommand src/main` |
| `GameCommand.issuer` arms answering `None` for a command that carries an actor | 3 (`Undo`, `Redo`, every `ConsentCommand`) | `GameCommands.scala:39-44` |
| frontend construction sites | 22 (17 `ResolveWalker`, one each of the other five) | `grep -rhoE 'GameCommand\.(…)' frontend/src/main` |
| test construction sites of `GameCommand.*` | 221 in 44 files (80 `Undo`, 68 `ResolveWalker`, 23 `Redo`, 18 `AnswerConsent`, 17 `Begin`, 8 `SetConsentPreference`, 7 `RequestConsent`) | `grep -rhoE 'GameCommand\.(…)' src/test` |
| test references to `AuthorizedPlayer` | 0 | `grep -rln AuthorizedPlayer src/test` |

### The seven files, for `AnswerConsent`

| step | file | what it says |
|---|---|---|
| 1 wire intent | `shared/.../protocol/CommandIntents.scala:24` | `final case class AnswerConsent(choice: String) extends GameIntent` |
| 2 encoder | `shared/.../protocol/CommandIntentCodec.scala:17` | `case AnswerConsent(choice) => tagged("answerConsent", "choice" -> choice)` |
| 3 decoder | `shared/.../protocol/CommandIntentDecoders.scala:34` | `case "answerConsent" => one(value, path, "choice")(AnswerConsent.apply)` |
| 4 mapper | `src/.../application/GameIntentMapper.scala:23-25` | `ConsentChoice.fromKey(value)…map(actor.answerConsent)` |
| 5 command | `src/.../application/GameCommands.scala:32` | `final case class AnswerConsent(granter: PlayerId, choice: ConsentChoice) extends ConsentCommand` |
| 6 authorization builder | `src/.../application/Authorization.scala:61-62` | `def answerConsent(choice: ConsentChoice): GameCommand = GameCommand.AnswerConsent(access.playerId, choice)` |
| 7 service arm | `src/.../application/GameApplicationService.scala:218-220` | `case GameCommand.AnswerConsent(granter, choice) => consentBatch(…consent.answer(…))` |

Steps 5 and 7 also carry an `issuer` arm (`GameCommands.scala:44`, via
`ConsentCommand`) and an exhaustiveness arm in `applyUnblockedCommand`
(`GameApplicationService.scala:505-509`), so the honest count for a consent
command is seven files and nine sites. `ResolveWalker` has one more: the
Search source arm at `GameApplicationService.scala:483-499`.

## What exists, and what duplicates what

Steps 1 to 3 are the wire. Steps 5 to 7 are the application. Step 4 is the
one boundary that turns strings into model types, and it is where the two
accidents the roadmap names live.

**`AuthorizedPlayer.forPlayer` mints a fake access context.**
`Authorization.scala:43-44`:

```scala
private[application] def forPlayer(playerId: PlayerId): AuthorizedPlayer =
  AuthorizedPlayer(GameAccessContext.Player("transport", UserId("transport"), playerId))
```

`GameAccessContext.Player(gameId, userId, playerId)` is the membership
authorization's answer: this user holds this seat in this game. The mapper
has a `PlayerId` and nothing else, so it fabricates a game id and a user id
to reach the six builder methods, which read only `access.playerId`. The
one real producer, `MembershipAuthorizationService.authorizeCommand`, is
used by `AuthenticatedGameGateway.submit`, which extracts
`actor.access.playerId` and passes it to `GameIntentMapper.bind`, which
mints the fake context again. So every command in every transport is built
through the fake context, and the real one is thrown away two lines before.
`AuthorizedPlayer` has no test and no caller outside the mapper. It is a
builder of six one-line constructors, reached by a lie about where the
player came from.

**`GameCommand.issuer` answers `None` for commands that carry an actor.**
That is not an accident of the type, it is a rule of the undo design: only
a command that opens a batch gets a `CommandAccepted(issuer)`, and `Undo`,
`Redo` and the consent commands append their markers or ledger events alone
(`GameCommands.scala:35-44`, `GameApplicationService.routed`). The roadmap
reads the `None` as a smell because the function is named for the player and
the player is right there in the case. The real property is **opens a
batch**, and it is a property of the intent kind, not of who sent it.
Keeping it as `issuer: Option[PlayerId]` next to an `actor: PlayerId` is
what makes it read wrong.

**Two intent vocabularies that mirror each other case for case.**
`protocol.GameIntent` and `application.GameCommand` have the same six
cases with the same fields, one in strings and one in model types, and the
mapper is the isomorphism. That is deliberate (the codebase-structure doc:
"All identifiers remain opaque strings until the application mapping
boundary validates them") and this note keeps it: the shared module cannot
see `DecisionOptionRef`, `ConsentChoice` or `TreeDecision`, and should not.
What it does not need is a *third* spelling, the builders.

**`Begin` is a command with no intent and no actor.** It is built by
`AuthenticatedGameGateway.bootstrap` and, through `prepareBootstrap`, by
`TrustedGameProvisioning`, from `FirstGameBootstrapRequest`. It is the one
case that keeps `GameCommand` from being "actor plus intent" outright.

## The target: an actor plus a model-typed intent

```scala
// application/GameCommands.scala
/** What a player asks the game to do, every identifier a model type.
  * Mirrors `protocol.GameIntent` case for case; `GameIntentMapper` is the
  * only conversion. */
sealed trait PlayerIntent extends Product with Serializable:
  /** Whether a batch opens for this intent, so a `CommandAccepted(actor)`
    * is written and Undo can take the batch back (undo design, "Terms").
    * Undo, Redo and the consent intents append their own events alone. */
  def opensBatch: Boolean = false
object PlayerIntent:
  case object Undo extends PlayerIntent
  case object Redo extends PlayerIntent
  final case class ResolveWalker(treeDecision: TreeDecision) extends PlayerIntent:
    override def opensBatch = true
  sealed trait Consent extends PlayerIntent
  final case class RequestConsent(decisionId: String, target: DecisionOptionRef,
      granters: Vector[PlayerId] = Vector.empty) extends Consent
  final case class AnswerConsent(choice: ConsentChoice) extends Consent
  final case class SetConsentPreference(power: PowerId, requester: PlayerId,
      setting: ConsentSetting) extends Consent

sealed trait GameCommand extends Product with Serializable
object GameCommand:
  /** The host starts a game. No actor: a journal still begins with
    * `GameStarted`. */
  final case class Begin(chronicle: Chronicle, orders: SetupOrders) extends GameCommand
  /** `actor` asks for `intent`. The transport resolved the actor; the
    * mapper typed the intent; nothing else is needed to build one. */
  final case class Issued(actor: PlayerId, intent: PlayerIntent) extends GameCommand
```

"An actor plus a model-typed intent" is `Issued(actor, intent)`: the actor
is a `PlayerId` a transport resolved (a seat cookie, a query parameter, a
membership) and the intent is a `PlayerIntent`, which carries no player
field that means "me". The granter of `AnswerConsent` and the requester of
`RequestConsent` *are* the actor, so they leave the case; the requester
named in `SetConsentPreference` is another player and stays.

Consequences:

- `GameIntentMapper.bind(actorId, intent)` becomes `map(intent):
  Either[GameIntentMappingFailure, PlayerIntent]`, with no actor; the
  transport wraps `Issued(actor, mapped)`. The actor-injection check stays
  on the wire (`CommandJsonSupport.rejectActorFields`), unchanged.
- `AuthorizedPlayer`, its six builders and `forPlayer` are deleted.
  `MembershipAuthorizationService.authorizeCommand` returns the
  `GameAccessContext.Player` itself; the gateway reads its `playerId`.
- `GameCommand.issuer(command)` becomes
  `case Issued(actor, intent) if intent.opensBatch => Some(actor)`; the
  three `None` arms become one `false` default.
- `GameApplicationService.routed` and `applyUnblockedCommand` match on
  `Issued(actor, intent)` and then on `intent`. `handle`'s `Begin` guards
  stay as they are. The Search source arm matches
  `Issued(actor, ResolveWalker(TreeDecision(SearchProcedure.sourceDecisionId,
  answer)))`.
- `ConsentDesk`'s signatures take `actor: PlayerId` plus the intent's
  fields; nothing in it changes otherwise.

### `AnswerConsent`, after

| step | file | what it says |
|---|---|---|
| 1 wire intent | `CommandIntents.scala` | unchanged |
| 2 encoder | `CommandIntentCodec.scala` | unchanged (gone if codecs are derived) |
| 3 decoder | `CommandIntentDecoders.scala` | unchanged (gone if codecs are derived) |
| 4 mapper | `GameIntentMapper.scala` | `case Intent.AnswerConsent(value) => ConsentChoice.fromKey(value).toRight(…).map(PlayerIntent.AnswerConsent.apply)` |
| 5 intent | `GameCommands.scala` | `final case class AnswerConsent(choice: ConsentChoice) extends Consent` |
| 6 service arm | `GameApplicationService.scala` | `case Issued(granter, AnswerConsent(choice)) => consentBatch(…consent.answer(journal.state, granter, choice)…)` |

Seven files and nine sites become six files and six sites; four files and
four sites once the wire codecs are derived. The deleted sites are the
builder, the `issuer` arm and the exhaustiveness arm, each of which was a
place a new command could be forgotten or mis-bound (the builder is where
`SetConsentPreference`'s comment has to warn that the field is not
`playerId`).

What this does not do: it does not merge the wire intent and the model
intent into one type. The shared module compiles into the frontend, which
must never hold `DecisionOptionRef` or `ConsentChoice`, and the string
boundary is a feature.

## Slices

Each slice is one session, independently mergeable, gated by the full gate
(`./sbtw -batch test frontend/test frontend/fullOptJS`, the architecture
and link checks, `git diff --check`).

1. **`PlayerIntent` and `Issued`, with the old cases kept as aliases.**
   Add `PlayerIntent` and `GameCommand.Issued`; add
   `GameCommand.Undo(actor)`-style `apply`/`unapply` on the companion so
   the 221 test sites and the 22 frontend-independent service arms compile
   unchanged (`object Undo { def apply(actor: PlayerId) = Issued(actor,
   PlayerIntent.Undo); def unapply(c: GameCommand): Option[PlayerId] = … }`).
   Rewrite `issuer` as `opensBatch`; rewrite `routed` and
   `applyUnblockedCommand` over `Issued`. Pins: every service suite
   (`UndoServiceSuite`, `RedoServiceSuite`, the seven consent service
   suites, `GameApplicationServiceActionsSuite`), `JournalFoldSuite` for
   the `CommandAccepted` placement, plus one new test in
   `GameApplicationServiceStreamSuite`: `opensBatch` is true for exactly
   `ResolveWalker`.
2. **Mapper without an actor; delete `AuthorizedPlayer`.** `bind(actor,
   intent)` becomes `map(intent)`; the three gateways wrap `Issued`. Delete
   `AuthorizedPlayer`, `forPlayer` and the `"transport"` literal;
   `authorizeCommand` returns `GameAccessContext.Player`. Pins:
   `ConsentIntentMapperSuite` ("a consent request binds its sender as the
   requester" becomes "a consent request maps to its model intent" with the
   binding asserted at the gateway), `GameIntentMapperUndoSuite`,
   `GameIntentMapperNegotiationSuite`, `GameHttpWireSuite` ("walker intents
   bind to ResolveWalker commands for the requester"),
   `TrustedSeatRoutesSuite` ("the trusted gateway binds the command actor
   to the seat"), `AuthenticatedGameRoutesSuite` ("actorless commands reach
   the rules and a playerId on …"), `MembershipAuthorizationServiceSuite`.
3. **Retire the aliases.** A scripted edit turns `GameCommand.Undo(p1)`
   into `GameCommand.Issued(p1, PlayerIntent.Undo)` or, better, a testkit
   `Commands.undo(p1)` family in `distantlands.testkit`, across the 44 test
   files; `-Werror`'s unused-import check proves the sweep as it did for
   the power harness consolidation. Delete the companion `apply`/`unapply`
   shims. No production behaviour changes. Optional: it can be left in
   place indefinitely, at the cost of two spellings in tests.

## Risks

- **A match that stops being exhaustive.** `routed` today matches six
  concrete cases and a `case _`; after slice 1 it matches `Issued(actor,
  intent)` and then `intent`, which the compiler checks over the sealed
  `PlayerIntent`. The `applyUnblockedCommand` arms that exist "only to keep
  the match exhaustive" (`GameApplicationService.scala:503-509`) remain
  needed in the same form; the slice must not turn them into a wildcard.
- **Undo semantics move with `opensBatch`.** The undo design's rulings
  rest on exactly one intent opening a batch. `opensBatch` is a default on
  the trait so a new intent is batchless until it says otherwise, which is
  the safer default (a batchless intent cannot be undone, but it also
  cannot be mis-attributed). The new test in slice 1 pins the set.
- **The sweep is wide but shallow.** 221 construction sites in tests; the
  alias shim in slice 1 is what keeps slice 1 a one-session change, and
  slice 3 is a rename, not a refactor.
- **Wire compatibility.** None: the wire intents and their JSON are
  untouched. The standing rule on in-place format change is not even
  needed.

## Recommendation

Do it, fully, and first of the three candidates. It is small (slices 1 and
2 are a session each, slice 3 is scripted), it deletes a lie (`"transport"`)
and a builder nobody tests, it renames a function whose `None` answers
mislead readers into a property that says what it means, and it is the
precondition for the HTTP gateway note's seam: a gateway that resolves an
actor and wraps `Issued(actor, intent)` needs no `AuthorizedPlayer` and no
mapper that takes one. The seven-to-four reduction per command is modest on
its own; the reason to do it is the two accidents, not the count.

## Dependencies

- The [HTTP gateway note](2026-11-02-one-http-gateway-design.md) should
  follow this one: its `ActorResolution` yields a `PlayerId`, and `Issued`
  is how that becomes a command without the fake access context.
- The [wire codecs note](2026-11-02-derive-wire-codecs-design.md) is
  independent; if it lands first, nothing here changes, and if this lands
  first, its slice 4 has two fewer per-command arms to derive.

## Questions for the owner

1. **Keep `Begin` as a `GameCommand` beside `Issued`, or make
   `prepareBootstrap`/`bootstrap` call a dedicated `begin` on the service
   and drop `Begin` from the command type?** Recommended default: keep
   `Begin` as a command; `handle`'s stream-existence rules
   (`StreamNotFound` for a non-`Begin` on an absent stream, `DuplicateGame`
   for `Begin` on a present one) are written against the command, and the
   `GameServerGatewaySubmitBeginSuite` pins them. A later note can revisit.
2. **Retire the `GameCommand.Undo(actor)` aliases in tests (slice 3), or
   leave the shim?** Recommended default: retire them through a testkit
   `Commands` object; two spellings of the same command in 44 files is the
   kind of drift the harness consolidation just removed.
3. **Name: `PlayerIntent` in `application`, or `Intent` nested in
   `GameCommand`?** Recommended default: `PlayerIntent`, a top-level sealed
   trait in `GameCommands.scala`, because the service and the consent desk
   match on it directly and `protocol.GameIntent` is already imported there
   under the alias `Intent`.
