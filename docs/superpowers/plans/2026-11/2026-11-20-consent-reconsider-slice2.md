# Reconsidering a Consent Answer, Slice 2: Ledger and Command Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** A granter can change an answer they gave this turn, in either
direction, with the `ReconsiderConsent` command, until the requester has
acted on it; the ledger, the journal and the Game Log record the change.
No screen offers the command yet (slice 3).

**Architecture:** The consent ledger (`ConsentState`) gains `history`, the
turn's settled requests, and `reconsidered`, which rewrites one answer.
A new consent event, `ConsentReconsidered`, folds through it; like every
consent event it opens no command batch. The lock is a **use mark** in
`CurrentGameState`, the part of the game undo rewinds: the fold of the
walker event that records an answer sets it, so undoing the answer removes
it with no undo code. `ConsentDesk.reconsider` checks the command and
answers with the one event; the service appends it as a consent batch.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler
flags including `-Werror`.

**Spec:** [Reconsidering a consent answer](../../specs/2026-11/2026-11-20-consent-reconsider-design.md),
rulings 20 to 26 and the sections "State", "Events", "Commands" and "Game
Log". It extends the
[consent design](../../specs/2026-10/2026-10-08-consent-system-design.md),
whose rulings 12 and 13 this slice amends. Rulings made while this plan
runs go in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md),
from R7.

## Global Constraints

- Ruling 20: the player who answered a request may change that answer,
  either way, an answer their preference gave included, at any time in
  the turn the answer belongs to, while they still rule every power they
  were asked about.
- Ruling 21: an answer is locked while the requester's choice of the
  request's option stands. Every answer of one request locks together.
  A refusal that stopped the requester is never locked.
- Ruling 22: a change rewrites what the ledger remembers for the
  changer's own powers on that request. A late grant only reopens the
  option; it never answers the requester's parked decision.
- Ruling 24: a change never touches a preference.
- Ruling 25: a change is never a command batch and is never undone.
- Ruling 26: no limit on changes; each writes a public log line.
- The log lines, word for word: "Red now gives consent to ignore Narrow
  Pass and select Deep Woods." and "Red no longer gives consent to ignore
  Narrow Pass and select Deep Woods."
- `ConsentReconsidered(granter, requester, decisionId, target, granted)`
  joins `ConsentEvent`. Its codec requires every field. Old journals and
  old clients need no support: no compatibility shim, no default.
- `ReconsiderConsent(requesterId, decisionId, target, allow)` is the wire
  intent. `ConsentDesk.reconsider` answers with one `ConsentReconsidered`
  or `ConsentUnavailable`, and resumes nothing.
- An automatic power's answer stays final (the request's `use` is set).
  The asks with no power overlay (Warband Transfers, a Palanquin target,
  a Toll Roads payer, the Chancellor's Eye) stay final too.
- Protected (do not change): every existing consent behaviour the spec
  does not name. In particular `ConsentState.answered`, `withdrawn`,
  `preferenceSet`, `cleared`, `granting`, `carried` and `turnRenamed`
  keep their results apart from the new `history` field;
  `ConsentDesk.request`, `answer`, `opened`, `withdraw`, `stale` and
  `settled`; `JournalFold.carried`; the wire of the five existing consent
  events and the four existing consent intents; every existing log line;
  every golden log. A changed golden log stops the work: report the diff,
  do not re-record.
- Work in
  `/Users/roman/projects/distantlands/.worktrees/consent-reconsider` on
  branch `consent-reconsider`. `.tooling` and `node_modules` are symlinks;
  never commit them. Never commit on `0.2.0-alpha` or `main`. Push
  nothing, merge nothing.
- Stage files by name. The hook blocks `git add -A`, `git add -u`,
  `git add .`, `git add --all`, `git commit -a` and `git stash`.
- Commits use Conventional Commits with a scope and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after each commit and report any commit that is not ours.
- Code, comments, docs and commit messages are plain English prose. Wrap
  Scala and Markdown at 80 columns. Scaladoc cites "reconsider design".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command. Quote globs.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## What the code says (read at `00caaa079`)

The spec leaves five points to the code. This is what the code says about
each; the rulings follow.

**The event that records an answer.** Every answer to a parked decision
is journalled as `WalkerStepRecorded(nodeId, ChoicePayload(decisionId,
answer, by, alone), contributions)` (`gameplay/walker/WalkerEvents.scala`,
written by `WalkerSteps`). Its fold is the `ChoicePayload` case of
`WalkerReplay.applyRecordedReady`, which appends the answer to
`walkerPending.answered`. That fold takes the whole `ReadyGame`, so it
reads `ready.consent` as it stands at that journal position. This is the
one place the mark is set. It covers a requester's own
`ResolveWalker`, the answer the consent desk makes for the requester on a
grant at a choose-one (`ConsentDesk.resumed` calls `rules.resolveWalker`,
whose events are folded the same way), and an answer the walker gives
alone.

**Replay sets the same marks as live play.** `JournalFold` evolves every
event in order, voided or not, and a marker rewinds to a state the fold
already holds (`carried(top.before, current)`: the game of the old state,
the ledger of the current one). So the `ReadyGame` a `ChoicePayload`
meets in a whole-journal replay is the one it met live: the same game and
the same ledger. The live path folds twice with the same result:
`walkerTransition` folds the step events through `evolve`, and
`appendBatch` folds the appended events through `journalFold.extend`
before it writes them. No fold reads a mark to decide anything, so marks
cannot change what a replay does.

**Readers of the ledger's answers.** Every production reader goes through
`ConsentState.answer(key, granter, now)`, which counts an answer only
when the key's present granter gave it (or a search supposes it):
`WalkerPowerGather` (three sites), `DecideNarrowing`, `AutomaticUses`,
`OperationConsent`, `ConsentedRestriction`, `PowerUseRules`, `PhaseMenu`,
`Palanquin`, `TollRoads`, `MinorActionProcedures` and `WarbandTransfers`.
`ConsentState.remembered(key, now)`, which ignores who answered, is read
by three test suites only. A required refusal writes every key of the
request as the refuser's (`refusedBy`). On another granter's key that
entry is inert: read with that key's granter it yields nothing. It
matters in one case only: when the refuser later comes to rule the other
power too. So dropping the changer's refusal from the request's other
keys, as the spec says, changes no reader today and removes a refusal
that could wrongly come alive later.

**The asks that stay final.** `ConsentPowers.granter(ready, power)` is
`None` for the Toll Roads payer power (its waiver's granter is `_ =>
None`), which the Palanquin target's ask uses too, and for the two
Imperial laws, which have no waiver. The desk's check "the issuer still
rules every power of their ask" therefore refuses all four with no
special case.

**The service path.** `GameApplicationService.routed` sends each consent
intent to the desk and appends the answer with `consentBatch`, which
calls `appendBatch`: the events are folded, then `consent.stale` and
`aid.stale` add any clearing, and all of it is one append. No
`CommandAccepted` is written, so nothing is undoable. `ReconsiderConsent`
takes the same path. Nothing else runs after it: no `consent.opened`, no
resume.

## Rulings

Recorded in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md);
the tasks below apply them.

- **R7.** Only a `ConsentReconsidered` whose `granted` is false empties
  the redo stack. A change to a grant leaves it, and the redo then
  succeeds. Tests cover both directions. Task 5.
- **R8.** The desk refuses a change while a waiting request names the
  same requester, decision and option. Slice 3's projection gives such an
  entry the `change` value `waiting`, with the hover note "A request
  about this option is waiting for an answer." Task 5.
- **R9.** When a change makes the entry granted, each other answered
  grant in it is remembered for its keys, unless the key already holds an
  answer. That grant stays its granter's own to reconsider, which a test
  shows. Task 2.
- **R10.** A later request takes over only the asks its own granters
  answered before; the earlier entry stays while another answered ask is
  left in it. Task 1.
- **R11.** A mark carries its `ConsentTurn`; it is set only when the
  ledger's turn is the game's turn; setting one drops other turns' marks;
  a round rename renames the marks with the ledger's turn. Task 4.
- The orchestrator also asked for one test of a deferred ask the "still
  rules" check refuses. Task 5 tests the Chancellor's Eye.

## Decisions already made in this plan

These are mechanical or follow from the code; each is listed so the
reviewer can reject it.

- The mark type is `UseMark(turn, requester, decisionId, target)` in
  `model/Consent.scala`, and the field is
  `CurrentGameState.useMarks: Set[UseMark]`. `ConsentUse` already names
  an automatic power's card, so the mark does not take that name.
- A granted request about an automatic power is marked like any other
  when its step is answered. Nothing reads that mark: the desk refuses an
  automatic power's answer before it looks at the lock.
- A request counts as granted for a mark when `ConsentRequest.granted`
  holds for some history entry about that requester, decision and option.
- An answer chooses options only as a `ChooseOneAnswer` or a
  `ChooseManyAnswer`. Every other answer shape marks nothing.
- `ConsentUseMarks` lives in `gameplay/consent`, beside
  `ConsentEvolution`. If `scripts/check-architecture.py` refuses the
  import from `gameplay/walker`, the object moves to `gameplay/walker`
  and the move is reported.
- The desk's refusals, in the spec's order, with these reasons:
  "the game is over"; "p2 answered no consent request about that option
  this turn"; "an automatic power's answer cannot be changed"; "p2 no
  longer rules site.narrow-pass.pass"; "p1 has acted on this consent";
  "p2's answer is already that"; and for R8 "a consent request about
  that option is waiting".
- The wire names: event type `consent.reconsidered` with the fields
  `granter`, `requester`, `decisionId`, `target` and `granted`; intent
  type `reconsiderConsent` with `requesterId`, `decisionId`, `target`
  and `allow`. `allow` is a JSON boolean.

## Files

- Modify: `src/main/scala/distantlands/model/Consent.scala` (`history`,
  `historyAt`, `asked`, `grantedTo`, `reconsidered`, `UseMark`)
- Modify: `src/main/scala/distantlands/model/GameState.scala`
  (`CurrentGameState.useMarks`)
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala`
  (`ConsentReconsidered`)
- Modify: `src/main/scala/distantlands/gameplay/consent/ConsentEvolution.scala`
- Create: `src/main/scala/distantlands/gameplay/consent/ConsentUseMarks.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala`
  (the `ChoicePayload` case)
- Modify: `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala`
  (`advanceRound`)
- Modify: `src/main/scala/distantlands/serialization/GameEventWire.scala`
  and `ConsentEventCodec.scala`
- Modify: `src/main/scala/distantlands/application/GameCommands.scala`,
  `ConsentDesk.scala`, `GameApplicationService.scala`, `JournalFold.scala`,
  `GameIntentMapper.scala`, `gamelog/ConsentLines.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`,
  `CommandIntentCodec.scala`, `CommandIntentDecoders.scala`
- Tests, modify: `model/ConsentStateSuite.scala`,
  `gameplay/consent/ConsentEvolutionSuite.scala`,
  `gameplay/OperationExecutorSuite.scala`,
  `serialization/ConsentEventWireSuite.scala`,
  `application/ConsentIntentMapperSuite.scala`,
  `application/PendingWalkerInvariantSuite.scala`,
  `application/GameApplicationServiceStreamSuite.scala`,
  `application/gamelog/GameLogConsentSuite.scala`,
  `testkit/Commands.scala`, all under `src/test/scala/distantlands`, and
  `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`
- Tests, create:
  `src/test/scala/distantlands/gameplay/consent/ConsentUseMarksSuite.scala`,
  `src/test/scala/distantlands/application/ConsentReconsiderServiceSuite.scala`
- Modify docs: the consent design (rulings 12 and 13, status), the
  reconsider spec's status, `docs/rules/implementation-traceability.md`,
  `docs/architecture/rule-resolution.md`.

## Running things

- Shared sources build in the root project, so the root runs their suites.
- One suite: `./sbtw "testOnly distantlands.model.ConsentStateSuite"`.
- The consent suites together:
  `./sbtw "testOnly distantlands.model.ConsentStateSuite distantlands.gameplay.consent.* distantlands.application.Consent* distantlands.application.*ConsentServiceSuite distantlands.application.gamelog.GameLogConsentSuite distantlands.serialization.ConsentEventWireSuite"`.
- Full gate at slice end, in the background or with a long timeout:
  `./sbtw test`, `./sbtw frontend/test`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory in this plan was read at `00caaa079`. Each task begins by
running its grep at HEAD. Treat what the grep shows as the list; handle a
hit missing from the task by the task's rule, and report it.

---

### Task 1: The ledger keeps the turn's settled requests

**Files:**
- Modify: `src/main/scala/distantlands/model/Consent.scala`
- Test: `src/test/scala/distantlands/model/ConsentStateSuite.scala`

**Interfaces:**
- Produces, on `ConsentState`:
  `history: Vector[ConsentRequest] = Vector.empty` (last parameter);
  `def historyAt(now: ConsentTurn): Vector[ConsentRequest]`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "turn = Some(now)" -e "ConsentState(" -- src/main/scala
```
Expected: four `turn = Some(now)` sites in `Consent.scala` (`answered`'s
alternative-refusal branch, `granting`, `settledBy`, `refusedBy`), and no
positional `ConsentState(` construction that passes more than named
arguments. A fifth site that sets the turn gets `history = historyAt(now)`
too.

- [ ] **Step 2: Write the failing tests**

Add to `src/test/scala/distantlands/model/ConsentStateSuite.scala`,
after the test "answers expire with their turn, and the next turn's
replace them":

```scala
  test("a settled request joins the turn's history with its asks as " +
      "answered, granted or refused"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    assertEquals(granted.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.historyAt(turn), Vector(request(
      redOnPass.copy(answer = Some(false)), greenOnPolice)))

  test("a request joins the history only once it settles, and a " +
      "withdrawn one never does"):
    val partial = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn))
    assertEquals(partial.historyAt(turn), Vector.empty)
    assertEquals(accepted(partial.withdrawn(blue)).historyAt(turn),
      Vector.empty)

  test("the history belongs to its turn: another turn reads none, and a " +
      "request settling in a new turn starts it empty"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val later = ConsentTurn(1, red)
    assertEquals(granted.historyAt(later), Vector.empty)
    val next = accepted(accepted(granted.requested(request(greenOnPolice)))
      .answered(green, granted = false, later))
    assertEquals(next.historyAt(later),
      Vector(request(greenOnPolice.copy(answer = Some(false)))))

  test("a later request about the same option takes over the asks its " +
      "granters answered before, and leaves another granter's entry"):
    val both = accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn)).answered(green, granted = true,
      turn))
    val again = accepted(accepted(both.requested(request(greenOnPolice)))
      .answered(green, granted = false, turn))
    assertEquals(again.historyAt(turn), Vector(
      request(redOnPass.copy(answer = Some(true))),
      request(greenOnPolice.copy(answer = Some(false)))))
    val alone = accepted(accepted(again.requested(request(redOnPass)))
      .answered(red, granted = false, turn))
    assertEquals(alone.historyAt(turn), Vector(
      request(greenOnPolice.copy(answer = Some(false))),
      request(redOnPass.copy(answer = Some(false)))))

  test("an undo or a redo carries the history with the answers"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    assertEquals(granted.carried.historyAt(turn), granted.historyAt(turn))
```

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.model.ConsentStateSuite"`
Expected: a compile error, `value historyAt is not a member of
distantlands.model.ConsentState`.

- [ ] **Step 4: Write the implementation**

In `src/main/scala/distantlands/model/Consent.scala`:

Replace the `ConsentState` doc comment's first sentence and the parameter
list with:

```scala
/** The consent ledger (consent design, "State"): preferences keyed by
  * granter, power and requester; the answers of `turn`; the request
  * waiting on its granters, if any; and `history`, the requests settled
  * in `turn`, each with its asks as answered (reconsider design,
  * "State"). It lives beside the game, not in it, because undo rewinds
  * the game to a saved state and the ledger must outlive that. */
final case class ConsentState(
    preferences: Map[(PlayerId, PowerId, PlayerId), ConsentPreference] =
      Map.empty,
    answers: Map[ConsentKey, Remembered] = Map.empty,
    pending: Option[ConsentRequest] = None,
    turn: Option[ConsentTurn] = None,
    history: Vector[ConsentRequest] = Vector.empty):
```

Add after `rememberedAt`:

```scala
  /** The requests settled this turn, oldest first: none once the turn
    * they belong to has ended. A withdrawn request is never among them
    * (ruling 14). */
  def historyAt(now: ConsentTurn): Vector[ConsentRequest] =
    if turn.contains(now) then history else Vector.empty
```

In `answered`, the alternative-refusal branch becomes:

```scala
        else if !granted then Right(copy(
          answers = answersAt(now) ++ answersOf(refusals(updated)), pending =
            Some(updated), turn = Some(now), history = historyAt(now)))
```

`granting` becomes:

```scala
  def granting(keys: Vector[ConsentKey], now: ConsentTurn): ConsentState =
    copy(answers = answersAt(now) ++ keys.map(_ ->
      Remembered(ConsentAnswer.Granted, Granter.Supposed)), turn = Some(now),
      history = historyAt(now))
```

`settledBy` and `refusedBy` become:

```scala
  private def settledBy(request: ConsentRequest, now: ConsentTurn)
      : ConsentState =
    val kept = if request.granted then request else refusals(request)
    copy(answers = answersAt(now) ++ answersOf(kept), pending = None,
      turn = Some(now), history = joined(request, now))
```

```scala
  private def refusedBy(refuser: PlayerId, request: ConsentRequest,
      now: ConsentTurn): ConsentState =
    copy(answers = answersAt(now) ++ request.keys.map(_ ->
      ConsentAnswer.Refused.by(refuser)), pending = None, turn = Some(now),
      history = joined(request, now))
```

Add after `answersAt`:

```scala
  /** Whether `entry` is about `requester` choosing `target` at
    * `decisionId`. */
  private def about(entry: ConsentRequest, requester: PlayerId,
      decisionId: String, target: DecisionOptionRef): Boolean =
    entry.requester == requester && entry.decisionId == decisionId &&
      entry.target == target

  /** The turn's history with the settled `request` joined to it
    * (reconsider design, "State"; implementation ruling R10). A granter
    * answers a requester's option in one entry at most: an ask of an
    * earlier entry about the same option whose granter answers again in
    * `request` leaves that entry, and an entry left with no answered ask
    * goes. */
  private def joined(request: ConsentRequest, now: ConsentTurn)
      : Vector[ConsentRequest] =
    val answering = request.asks.filter(_.answer.nonEmpty).map(_.granter)
    historyAt(now).flatMap { entry =>
      if !about(entry, request.requester, request.decisionId,
          request.target) then Some(entry)
      else
        val left = entry.copy(asks = entry.asks.filterNot(ask =>
          ask.answer.nonEmpty && answering.contains(ask.granter)))
        Option.when(left.asks.exists(_.answer.nonEmpty))(left)
    } :+ request
```

- [ ] **Step 5: Run the suite to verify it passes**

Run:
`./sbtw "testOnly distantlands.model.ConsentStateSuite distantlands.gameplay.consent.ConsentEvolutionSuite"`
Expected: all pass, the existing tests unchanged.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/Consent.scala \
  src/test/scala/distantlands/model/ConsentStateSuite.scala
git commit -m "feat(consent): keep the turn's settled requests in the ledger

The ledger gains history: each request that settles this turn, granted
or refused, with its asks as answered. It belongs to the turn, as the
answers do, and a withdrawn request never joins it.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The ledger changes an answer

**Files:**
- Modify: `src/main/scala/distantlands/model/Consent.scala`
- Test: `src/test/scala/distantlands/model/ConsentStateSuite.scala`

**Interfaces:**
- Consumes: Task 1's `history`, `historyAt`, `about`.
- Produces, on `ConsentState`:
  `def asked(granter: PlayerId, requester: PlayerId, decisionId: String, target: DecisionOptionRef, now: ConsentTurn): Option[(ConsentRequest, ConsentAsk)]`;
  `def grantedTo(requester: PlayerId, decisionId: String, target: DecisionOptionRef, now: ConsentTurn): Boolean`;
  `def reconsidered(granter: PlayerId, requester: PlayerId, decisionId: String, target: DecisionOptionRef, granted: Boolean, now: ConsentTurn): Either[OathViolation, ConsentState]`.

- [ ] **Step 1: Write the failing tests**

Add to `ConsentStateSuite`, after Task 1's tests:

```scala
  private val destination = "travel.destination"
  private def refusedByRedAlone: ConsentState =
    accepted(waitingOn(redOnPass).answered(red, granted = false, turn))
  private def grantedByRedAndGreen: ConsentState =
    accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn)).answered(green, granted = true,
      turn))

  test("a refusal changed to a grant is remembered as the changer's " +
      "grant, in the answers and in the history"):
    val changed = accepted(refusedByRedAlone.reconsidered(red, blue,
      destination, woods, granted = true, turn))
    assertEquals(changed.answer(key(pass), Some(red), turn),
      Some(ConsentAnswer.Granted))
    assertEquals(changed.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))
    assert(changed.grantedTo(blue, destination, woods, turn))

  test("a grant changed to a refusal rewrites the changer's own keys " +
      "only"):
    val changed = accepted(grantedByRedAndGreen.reconsidered(red, blue,
      destination, woods, granted = false, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Refused.by(red)))
    assertEquals(changed.answers.get(key(police)),
      Some(ConsentAnswer.Granted.by(green)))
    assertEquals(changed.historyAt(turn).map(_.asks.map(_.answer)),
      Vector(Vector(Some(false), Some(true))))
    assert(!changed.grantedTo(blue, destination, woods, turn))

  test("with several granters, a change to a grant takes the changer's " +
      "refusal off the other granter's keys, so only that granter is " +
      "still to ask"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(red)))
    val changed = accepted(refused.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))
    assertEquals(changed.answers.get(key(police)), None)
    assertEquals(changed.historyAt(turn).map(_.asks.map(_.answer)),
      Vector(Vector(Some(true), None)))
    assert(!changed.grantedTo(blue, destination, woods, turn))

  test("a change that makes the request granted remembers a co-granter's " +
      "grant that the refusal had kept back"):
    val refused = accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(green, granted = true, turn)).answered(red, granted = false,
      turn))
    assertEquals(refused.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(red)))
    val changed = accepted(refused.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))
    assertEquals(changed.answers.get(key(police)),
      Some(ConsentAnswer.Granted.by(green)))
    assert(changed.grantedTo(blue, destination, woods, turn))
    // The remembered grant is Green's own to change (R9).
    val greenRefuses = accepted(changed.reconsidered(green, blue,
      destination, woods, granted = false, turn))
    assertEquals(greenRefuses.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(green)))
    assertEquals(greenRefuses.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))

  test("a change leaves the preferences and the waiting request alone"):
    val set = refusedByRedAlone.preferenceSet(red, pass, blue,
      ConsentSetting.Never)
    val changed = accepted(set.reconsidered(red, blue, destination, woods,
      granted = true, turn))
    assertEquals(changed.preferences, set.preferences)
    assertEquals(changed.pending, set.pending)

  test("only the granter who answered changes an entry, for that " +
      "requester, decision and option, and nothing is found once the " +
      "turn has ended"):
    val refused = refusedByRedAlone
    val other = DecisionOptionRef.Site(SiteId("site:dunes"))
    assert(refused.reconsidered(green, blue, destination, woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, green, destination, woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, "campaign.targets", woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, destination, other,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, destination, woods,
      granted = true, ConsentTurn(1, red)).isLeft)
    assertEquals(refused.asked(red, blue, destination, woods,
      ConsentTurn(1, red)), None)

  test("a granter whose ask closed unanswered has no entry to change"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.asked(green, blue, destination, woods, turn), None)

  test("after a later request took over a granter's ask, a change " +
      "reaches the later entry"):
    val first = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val second = accepted(accepted(first.requested(request(redOnPass)))
      .answered(red, granted = false, turn))
    val changed = accepted(second.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.model.ConsentStateSuite"`
Expected: a compile error, `value reconsidered is not a member of
distantlands.model.ConsentState`.

- [ ] **Step 3: Write the implementation**

Add to `ConsentState` in `Consent.scala`, after `historyAt`:

```scala
  /** The settled request of this turn in which `granter` answered about
    * `requester` choosing `target` at `decisionId`, with their ask
    * (reconsider design, "State"). `joined` keeps it the only one. */
  def asked(granter: PlayerId, requester: PlayerId, decisionId: String,
      target: DecisionOptionRef, now: ConsentTurn)
      : Option[(ConsentRequest, ConsentAsk)] =
    historyAt(now).iterator
      .filter(about(_, requester, decisionId, target))
      .flatMap(entry => entry.asks.find(ask => ask.granter == granter &&
        ask.answer.nonEmpty).map(entry -> _)).nextOption()

  /** Whether a request settled this turn granted `requester` `target` at
    * `decisionId`: what a use mark is set on (reconsider design, ruling
    * 21). */
  def grantedTo(requester: PlayerId, decisionId: String,
      target: DecisionOptionRef, now: ConsentTurn): Boolean =
    historyAt(now).exists(entry =>
      about(entry, requester, decisionId, target) && entry.granted)
```

Add after `withdrawn`:

```scala
  /** `granter` changes the answer they gave about `requester` choosing
    * `target` at `decisionId` this turn (reconsider design, rulings 20 and
    * 22). The entry's ask takes the new answer, and the keys of that
    * ask's powers are remembered as `granter` now answers them.
    *
    * A required refusal wrote every key of its request as the refuser's
    * (ruling 9), so a change to a grant takes that refusal off the
    * request's other keys. When the change makes the request granted, a
    * co-granter's grant that the refusal kept from being remembered is
    * remembered for its keys, unless one already holds an answer
    * (implementation ruling R9). Preferences and a waiting request are
    * untouched (ruling 24). Fails when `granter` answered no such request
    * at `now`. */
  def reconsidered(granter: PlayerId, requester: PlayerId,
      decisionId: String, target: DecisionOptionRef, granted: Boolean,
      now: ConsentTurn): Either[OathViolation, ConsentState] =
    asked(granter, requester, decisionId, target, now) match
      case None => Left(OathViolation.InvalidEventOrder(
        s"${granter.value} answered no such consent request this turn"))
      case Some((entry, ask)) =>
        val changed = entry.copy(asks = entry.asks.map(other =>
          if other.granter == granter then other.copy(answer = Some(granted))
          else other))
        val own = entry.keysOf(ask)
        val mine = (if granted then ConsentAnswer.Granted
          else ConsentAnswer.Refused).by(granter)
        val kept = if !granted then answers else answers.filterNot(
          (key, answer) => entry.keys.contains(key) && !own.contains(key) &&
            answer == ConsentAnswer.Refused.by(granter))
        val restored = if granted && changed.granted then
          answersOf(changed).filterNot((key, _) => kept.contains(key))
        else Vector.empty
        Right(copy(answers = kept ++ restored ++ own.map(_ -> mine),
          history = history.map(other =>
            if other == entry then changed else other)))
```

- [ ] **Step 4: Run the suite to verify it passes**

Run: `./sbtw "testOnly distantlands.model.ConsentStateSuite"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/Consent.scala \
  src/test/scala/distantlands/model/ConsentStateSuite.scala
git commit -m "feat(consent): let the ledger change an answer given this turn

reconsidered finds the settled request a granter answered, sets their
answer and rewrites what is remembered for their own powers. A change to
a grant takes their refusal off the request's other keys.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: The event, its fold and its codec

**Files:**
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala`
- Modify: `src/main/scala/distantlands/gameplay/consent/ConsentEvolution.scala`
- Modify: `src/main/scala/distantlands/serialization/GameEventWire.scala`
  (the type name, beside `ConsentPreferencesClearedType`)
- Modify: `src/main/scala/distantlands/serialization/ConsentEventCodec.scala`
- Test: `src/test/scala/distantlands/gameplay/consent/ConsentEvolutionSuite.scala`
- Test: `src/test/scala/distantlands/serialization/ConsentEventWireSuite.scala`

**Interfaces:**
- Consumes: Task 2's `ConsentState.reconsidered`.
- Produces: `OathEvent.ConsentReconsidered(granter: PlayerId, requester: PlayerId, decisionId: String, target: DecisionOptionRef, granted: Boolean) extends ConsentEvent`;
  `GameEventWire.ConsentReconsideredType = "consent.reconsidered"`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "ConsentPreferencesCleared" -- src/main/scala
```
Expected: the event's declaration, `ConsentEvolution`, `ConsentDesk`,
`ConsentEventCodec` (discriminator, encoder, decoder), `ConsentLines`.
A match on consent events by case that is not in this list gets a
`ConsentReconsidered` arm in the task that owns its file, or here.

- [ ] **Step 2: Write the failing tests**

In `ConsentEvolutionSuite`, add `ConsentReconsidered` to the
`OathEvent` import and add:

```scala
  test("a changed answer rewrites the ledger and nothing else"):
    val key = ConsentKey(pass, p1, "travel.destination", dunes)
    val after = folded(ConsentRequested(request),
      ConsentAnswered(p2, granted = false),
      ConsentReconsidered(p2, p1, "travel.destination", dunes,
        granted = true))
    assertEquals(after.consent.answer(key, Some(p2), ConsentTurn.of(after)),
      Some(ConsentAnswer.Granted))
    assertEquals(after.game, Outcomes.ready(table).game)

  test("a change of an answer nobody gave is refused"):
    assert(rules.evolve(table, ConsentReconsidered(p2, p1,
      "travel.destination", dunes, granted = true)).isLeft)
```

In `ConsentEventWireSuite`, add `ConsentReconsidered` to the `OathEvent`
import, add these two entries to `events` after the
`ConsentPreferencesCleared` entry:

```scala
    ConsentReconsidered(p2, p1, "travel.destination",
      DecisionOptionRef.Site(SiteId("site:dunes")), granted = true) ->
      "consent.reconsidered",
    ConsentReconsidered(p2, p1, "campaign.targets",
      DecisionOptionRef.Button("confirm"), granted = false) ->
      "consent.reconsidered"
```

(the `ConsentPreferencesCleared` entry gains a trailing comma) and add:

```scala
  test("a changed answer requires every field"):
    val change = ConsentReconsidered(p2, p1, "travel.destination",
      DecisionOptionRef.Site(SiteId("site:dunes")), granted = true)
    Vector("granter", "requester", "decisionId", "target", "granted")
      .foreach { field =>
        val encoded = GameEventWire.encodeEvent("g", 0L, change)
          .map(value => { value("payload").obj.remove(field); value })
        assert(encoded.flatMap(GameEventWire.decode(_)).isLeft, field)
      }
```

- [ ] **Step 3: Run the suites to verify they fail**

Run:
`./sbtw "testOnly distantlands.gameplay.consent.ConsentEvolutionSuite distantlands.serialization.ConsentEventWireSuite"`
Expected: a compile error, `ConsentReconsidered` is not a member of
`OathEvent`.

- [ ] **Step 4: Write the implementation**

In `GameEventProtocol.scala`, after `ConsentPreferencesCleared`:

```scala
  /** `granter` changed the answer they gave `requester` about `target` at
    * `decisionId` this turn (reconsider design, "Events"): a grant when
    * `granted`, else a refusal. It answers no parked decision. */
  final case class ConsentReconsidered(granter: PlayerId,
      requester: PlayerId, decisionId: String, target: DecisionOptionRef,
      granted: Boolean) extends ConsentEvent
```

In `ConsentEvolution.scala`, add `ConsentReconsidered` to the import and
this case after `ConsentPreferencesCleared`'s:

```scala
      case ConsentReconsidered(granter, requester, decisionId, target,
          granted) => consent.reconsidered(granter, requester, decisionId,
        target, granted, ConsentTurn.of(ready))
```

In `GameEventWire.scala`, after `ConsentPreferencesClearedType`:

```scala
  val ConsentReconsideredType = "consent.reconsidered"
```

In `ConsentEventCodec.scala`, add `ConsentReconsidered` to the import;
to `consentDiscriminator`:

```scala
      case _: ConsentReconsidered => ConsentReconsideredType
```

to `consentEncoder`:

```scala
      case ConsentReconsidered(granter, requester, decisionId, target,
          granted) =>
        ujson.Obj("granter" -> granter.value,
          "requester" -> requester.value, "decisionId" -> decisionId,
          "target" -> DecisionAnswerCodec.encodeRef(target),
          "granted" -> granted)
```

and to `decoder` in `consentDecode`:

```scala
        // Every field is required (reconsider design, "Events").
        case ConsentReconsideredType =>
          DecisionAnswerCodec.decodeRef(payload("target"), s"$path.target")
            .map(ConsentReconsidered(player("granter"), player("requester"),
              payload("decisionId").str, _, payload("granted").bool))
```

A missing field makes `payload(name)` throw, which `GameEventWire.decode`
already turns into a `Left` for the other consent events (its `try` at
the decode dispatch). If the "requires every field" test shows a field
whose absence is not refused, read that field through an explicit check
and report it.

- [ ] **Step 5: Run the suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.gameplay.consent.ConsentEvolutionSuite distantlands.serialization.ConsentEventWireSuite"`
Expected: all pass. `./sbtw compile` reports no warning: `-Werror` would
fail on a consent match that lacks the new case.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/GameEventProtocol.scala \
  src/main/scala/distantlands/gameplay/consent/ConsentEvolution.scala \
  src/main/scala/distantlands/serialization/GameEventWire.scala \
  src/main/scala/distantlands/serialization/ConsentEventCodec.scala \
  src/test/scala/distantlands/gameplay/consent/ConsentEvolutionSuite.scala \
  src/test/scala/distantlands/serialization/ConsentEventWireSuite.scala
git commit -m "feat(consent): journal a changed answer as ConsentReconsidered

The event names the granter, the requester, the decision, the option and
the new answer, and folds through the ledger. Its codec requires every
field.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The use mark

**Files:**
- Modify: `src/main/scala/distantlands/model/Consent.scala` (`UseMark`)
- Modify: `src/main/scala/distantlands/model/GameState.scala`
  (`CurrentGameState.useMarks`, after `walkerPicks`)
- Create: `src/main/scala/distantlands/gameplay/consent/ConsentUseMarks.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala`
  (the `ChoicePayload` case)
- Modify: `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala`
  (`advanceRound`)
- Test: `src/test/scala/distantlands/gameplay/consent/ConsentUseMarksSuite.scala` (create)
- Test: `src/test/scala/distantlands/gameplay/OperationExecutorSuite.scala`

**Interfaces:**
- Consumes: Task 2's `ConsentState.grantedTo`.
- Produces: `final case class UseMark(turn: ConsentTurn, requester: PlayerId, decisionId: String, target: DecisionOptionRef)`
  with `UseMark.renamed(marks: Set[UseMark], from: ConsentTurn, to: ConsentTurn): Set[UseMark]`;
  `CurrentGameState.useMarks: Set[UseMark] = Set.empty`;
  `ConsentUseMarks.answered(ready: ReadyGame, decisionId: String, answer: DecisionAnswer, by: PlayerId): ReadyGame`;
  `ConsentUseMarks.locked(ready: ReadyGame, requester: PlayerId, decisionId: String, target: DecisionOptionRef): Boolean`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "ChoicePayload" -- src/main/scala
git grep -n -e "turnRenamed" -- src/main/scala
```
Expected: `ChoicePayload` is written in `WalkerSteps.scala` only and
folded in `WalkerReplay.scala` only (the other hits are the codec, the
undo barrier and the log, which read it). `turnRenamed` is called in
`TurnStateOperations.advanceRound` only. A second fold of the payload, or
a second caller of `turnRenamed`, gets the same edit and is reported.

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/consent/ConsentUseMarksSuite.scala`:

```scala
package distantlands.gameplay.consent

import distantlands.gameplay.OathRules
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ChoicePayload, WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer,
  ChooseManyAnswer, ChooseOneAnswer}
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The use mark (reconsider design, "State"; ruling 21): set where a
  * recorded answer chooses an option a request of this turn granted, and
  * kept in the game, which undo rewinds. */
class ConsentUseMarksSuite extends munit.FunSuite:
  private val pass = PowerId("site.narrow-pass.pass")
  private val dunes = DecisionOptionRef.Site(SiteId("site:dunes"))
  private val steppe = DecisionOptionRef.Site(SiteId("site:steppe"))
  private val destination = "travel.destination"
  private val table = Outcomes.ready(Table.start.state)
  private val now = ConsentTurn.of(table)

  /** The table after p2 answered p1's request about `target`. */
  private def tableWhereP2Answered(target: DecisionOptionRef,
      granted: Boolean): ReadyGame =
    val request = ConsentRequest(p1, destination, target,
      Vector(ConsentAsk(p2, Vector(pass))))
    table.copy(consent = accepted(accepted(
      table.consent.requested(request)).answered(p2, granted, now)))

  private def marks(ready: ReadyGame): Set[UseMark] =
    ready.game.current.useMarks

  test("choosing a granted option marks it and locks it"):
    val chosen = ConsentUseMarks.answered(tableWhereP2Answered(dunes,
      granted = true), destination, ChooseOneAnswer(dunes), p1)
    assertEquals(marks(chosen), Set(UseMark(now, p1, destination, dunes)))
    assert(ConsentUseMarks.locked(chosen, p1, destination, dunes))
    assert(!ConsentUseMarks.locked(chosen, p1, destination, steppe))

  test("no mark for a refused option, an option nobody asked about, " +
      "another player's answer, another decision or an amount"):
    val granted = tableWhereP2Answered(dunes, granted = true)
    val refused = tableWhereP2Answered(dunes, granted = false)
    assertEquals(marks(ConsentUseMarks.answered(refused, destination,
      ChooseOneAnswer(dunes), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseOneAnswer(steppe), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseOneAnswer(dunes), p2)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted,
      "campaign.targets", ChooseOneAnswer(dunes), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseAmountAnswer(2), p1)), Set.empty)

  test("a choose-many answer marks each granted option it includes"):
    val chosen = ConsentUseMarks.answered(tableWhereP2Answered(dunes,
      granted = true), destination,
      ChooseManyAnswer(Vector(steppe, dunes)), p1)
    assertEquals(marks(chosen), Set(UseMark(now, p1, destination, dunes)))

  test("a mark is of its turn: another turn's mark locks nothing and " +
      "goes when a new one is set"):
    val ofAnotherTurn = UseMark(ConsentTurn(now.round + 1, now.player),
      p1, destination, dunes)
    val stale = tableWhereP2Answered(dunes, granted = true)
      .updateCurrent(_.copy(useMarks = Set(ofAnotherTurn)))
    assert(!ConsentUseMarks.locked(stale, p1, destination, dunes))
    assertEquals(marks(ConsentUseMarks.answered(stale, destination,
      ChooseOneAnswer(dunes), p1)),
      Set(UseMark(now, p1, destination, dunes)))

  test("the fold of a recorded answer sets the mark from the ledger it " +
      "meets"):
    val rules = new OathRules(catalog)
    def folded(ready: ReadyGame): ReadyGame = Outcomes.ready(accepted(
      rules.evolve(OathState.Ready(ready), WalkerStepRecorded("0",
        ChoicePayload(destination, ChooseOneAnswer(dunes), p1,
          alone = true), Vector.empty))))
    assertEquals(marks(folded(tableWhereP2Answered(dunes, granted = true))),
      Set(UseMark(now, p1, destination, dunes)))
    assertEquals(marks(folded(tableWhereP2Answered(dunes,
      granted = false))), Set.empty)

  test("marks follow a turn that is renamed"):
    val mark = UseMark(now, p1, destination, dunes)
    val other = mark.copy(turn = ConsentTurn(now.round + 3, now.player))
    val later = ConsentTurn(now.round + 1, now.player)
    assertEquals(UseMark.renamed(Set(mark, other), now, later),
      Set(mark.copy(turn = later), other))
```

Add to `src/test/scala/distantlands/gameplay/OperationExecutorSuite.scala`,
after the test "a round advance carries the turn's consent answers to the
new round":

```scala
  test("a round advance carries the turn's use marks to the new round"):
    val base = Table.from(ready).roundMarkerAt(2).ready
    val mark = UseMark(ConsentTurn.of(base), PlayerId("p1"),
      "travel.destination", DecisionOptionRef.Site(SiteId("site:dunes")))
    val staged = base.updateCurrent(_.copy(useMarks = Set(mark)))
    val after = accepted(executor.execute(staged, AdvanceRound))
    assertEquals(after.game.current.useMarks,
      Set(mark.copy(turn = ConsentTurn.of(after))))
```

- [ ] **Step 3: Run the suites to verify they fail**

Run:
`./sbtw "testOnly distantlands.gameplay.consent.ConsentUseMarksSuite distantlands.gameplay.OperationExecutorSuite"`
Expected: a compile error, `Not found: ConsentUseMarks` and `Not found:
type UseMark`.

- [ ] **Step 4: Write the implementation**

In `Consent.scala`, after `ConsentTurn`'s companion:

```scala
/** A use mark (reconsider design, "State"; ruling 21): in `turn`,
  * `requester` chose `target` at `decisionId` on a granted request, and
  * that choice stands. It is kept in the game, which undo rewinds, so
  * undoing the choice removes the mark and unlocks the answer. */
final case class UseMark(turn: ConsentTurn, requester: PlayerId,
    decisionId: String, target: DecisionOptionRef)
object UseMark:
  /** `marks` with the marks of turn `from` kept as `to`'s, when the
    * round marker moves within a turn (Great Clock), as the ledger's
    * answers are (`ConsentState.turnRenamed`). */
  def renamed(marks: Set[UseMark], from: ConsentTurn, to: ConsentTurn)
      : Set[UseMark] =
    marks.map(mark => if mark.turn == from then mark.copy(turn = to)
      else mark)
```

In `GameState.scala`, after the `walkerPicks` field of
`CurrentGameState`:

```scala
    // The options chosen this turn on a granted consent request (`UseMark`;
    // reconsider design, "State"). Not walker scratch: a mark outlives
    // its walk and locks the answer until the choice is undone. The fold
    // of a recorded answer sets it; nothing clears it but an undo.
    useMarks: Set[UseMark] = Set.empty,
```

Create `src/main/scala/distantlands/gameplay/consent/ConsentUseMarks.scala`:

```scala
package distantlands.gameplay.consent

import distantlands.model._

/** The use marks (reconsider design, "State"; ruling 21). A mark records
  * that a requester acted on a granted request. The fold of the walker
  * event that records an answer sets it, from the ledger as it stands at
  * that journal position, so live play and a whole-journal replay set
  * the same marks. Undo returns to a game saved before the answer, so
  * the mark goes with no undo code. */
object ConsentUseMarks:
  /** `ready` after `by` answered `decisionId` with `answer`: marked for
    * each option the answer chooses that a request settled this turn
    * granted `by` there. A choose-many answer marks each such option it
    * includes. Marks of other turns are dropped when one is set. */
  def answered(ready: ReadyGame, decisionId: String, answer: DecisionAnswer,
      by: PlayerId): ReadyGame =
    val now = ConsentTurn.of(ready)
    val chosen = answer match
      case DecisionAnswer.ChooseOneAnswer(selected) => Vector(selected)
      case DecisionAnswer.ChooseManyAnswer(selected) => selected
      case _ => Vector.empty
    val used = chosen.filter(ready.consent.grantedTo(by, decisionId, _, now))
      .map(UseMark(now, by, decisionId, _))
    if used.isEmpty then ready
    else ready.updateCurrent(current => current.copy(
      useMarks = current.useMarks.filter(_.turn == now) ++ used))

  /** Whether the answers about `requester` choosing `target` at
    * `decisionId` are locked: the choice was made this turn and stands. */
  def locked(ready: ReadyGame, requester: PlayerId, decisionId: String,
      target: DecisionOptionRef): Boolean =
    ready.game.current.useMarks.contains(UseMark(ConsentTurn.of(ready),
      requester, decisionId, target))
```

In `WalkerReplay.scala`, add
`import distantlands.gameplay.consent.ConsentUseMarks` and change the
`ChoicePayload` case's `yield` from

```scala
        yield ready.copy(game = ready.game.copy(current =
          ready.game.current.copy(walkerPending = Some(answered))))
```

to

```scala
        // The answer may act on a granted consent request: the mark is
        // set here, from the ledger this replay position holds
        // (reconsider design, "State").
        yield ConsentUseMarks.answered(ready.copy(game = ready.game.copy(
          current = ready.game.current.copy(walkerPending =
            Some(answered)))), decisionId, payload, by)
```

In `TurnStateOperations.advanceRound`, replace

```scala
    Either.cond(tracks.round < 8,
      moved.copy(consent = ready.consent.turnRenamed(ConsentTurn.of(ready),
        ConsentTurn.of(moved))),
      InvalidDescription("the round marker is on its last space"))
```

with

```scala
    // The turn's use marks follow it, as its answers do.
    def renamed =
      val (from, to) = (ConsentTurn.of(ready), ConsentTurn.of(moved))
      moved.copy(consent = ready.consent.turnRenamed(from, to))
        .updateCurrent(current => current.copy(
          useMarks = UseMark.renamed(current.useMarks, from, to)))
    Either.cond(tracks.round < 8, renamed,
      InvalidDescription("the round marker is on its last space"))
```

- [ ] **Step 5: Run the suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.gameplay.consent.ConsentUseMarksSuite distantlands.gameplay.OperationExecutorSuite"`
then `python3 scripts/check-architecture.py`.
Expected: all pass. If the architecture check refuses
`gameplay/walker` importing `gameplay/consent`, move `ConsentUseMarks`
to `gameplay/walker` (and its suite to
`src/test/scala/distantlands/gameplay/walker`), rerun, and report the
move.

- [ ] **Step 6: Run the walker and replay suites**

Run: `./sbtw "testOnly distantlands.gameplay.walker.* distantlands.application.*Undo* distantlands.application.JournalFold*"`
Expected: all pass. A suite that compares a whole `CurrentGameState` and
fails on `useMarks` is reported with its diff before anything is changed.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/Consent.scala \
  src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/gameplay/consent/ConsentUseMarks.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala \
  src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala \
  src/test/scala/distantlands/gameplay/consent/ConsentUseMarksSuite.scala \
  src/test/scala/distantlands/gameplay/OperationExecutorSuite.scala
git commit -m "feat(consent): mark an option chosen on a granted request

The fold of a recorded answer sets a use mark when the answer chooses an
option a request of this turn granted. The mark lives in the game, so
undoing the answer removes it. A round advance inside a turn renames the
marks with the ledger's turn.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: The command: desk, service route and redo

**Files:**
- Modify: `src/main/scala/distantlands/application/GameCommands.scala`
  (`PlayerIntent.ReconsiderConsent`)
- Modify: `src/main/scala/distantlands/application/ConsentDesk.scala`
  (`reconsider`)
- Modify: `src/main/scala/distantlands/application/GameApplicationService.scala`
  (`routed`)
- Modify: `src/main/scala/distantlands/application/JournalFold.scala`
  (the consent case of `Progress.next`)
- Modify: `src/test/scala/distantlands/testkit/Commands.scala`
- Modify: `src/test/scala/distantlands/application/PendingWalkerInvariantSuite.scala`
  (`everyCommand`)
- Modify: `src/test/scala/distantlands/application/GameApplicationServiceStreamSuite.scala`
  (the intents of "ResolveWalker is the one intent that opens a batch")
- Test: `src/test/scala/distantlands/application/ConsentReconsiderServiceSuite.scala` (create)

**Interfaces:**
- Consumes: `ConsentState.asked` (Task 2), `OathEvent.ConsentReconsidered`
  (Task 3), `ConsentUseMarks.locked` (Task 4).
- Produces:
  `PlayerIntent.ReconsiderConsent(requester: PlayerId, decisionId: String, target: DecisionOptionRef, allow: Boolean) extends Consent`;
  `ConsentDesk.reconsider(state: OathState, granter: PlayerId, requester: PlayerId, decisionId: String, target: DecisionOptionRef, allow: Boolean): Either[OathViolation, Vector[OathEvent]]`;
  `Commands.reconsiderConsent(granter: PlayerId, requester: PlayerId, decisionId: String, target: DecisionOptionRef, allow: Boolean): GameCommand`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "PlayerIntent.AnswerConsent" -e "Commands.answerConsent(actor" -- src
```
Expected: `GameIntentMapper.scala`, `GameApplicationService.scala`
(`routed`), `testkit/Commands.scala`, and the two sample lists in
`PendingWalkerInvariantSuite.scala` and
`GameApplicationServiceStreamSuite.scala`. Another list of every intent
gains the new one.

- [ ] **Step 2: Write the failing tests**

Add to `src/test/scala/distantlands/testkit/Commands.scala`, after
`answerConsent`:

```scala
  def reconsiderConsent(granter: PlayerId, requester: PlayerId,
      decisionId: String, target: DecisionOptionRef, allow: Boolean)
      : GameCommand = GameCommand.Issued(granter,
    PlayerIntent.ReconsiderConsent(requester, decisionId, target, allow))
```

In `PendingWalkerInvariantSuite.everyCommand`, after the
`Commands.answerConsent` line:

```scala
    Commands.reconsiderConsent(actor, PlayerId("p2"), "not-parked",
      DecisionOptionRef.Button("go"), allow = true),
```

In `GameApplicationServiceStreamSuite`, in the `intents` vector of
"ResolveWalker is the one intent that opens a batch", after
`PlayerIntent.AnswerConsent(ConsentChoice.Yes)`:

```scala
      PlayerIntent.ReconsiderConsent(p2, "any",
        DecisionOptionRef.Button("go"), allow = true)
```

(the `AnswerConsent` line gains a trailing comma and loses its closing
parenthesis, which moves to the new line).

Create `src/test/scala/distantlands/application/ConsentReconsiderServiceSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.actions.ImperialLaws
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.NarrowPassSite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.travel.{TravelFixture, TyrantCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.OathEvent.{CommandAccepted, ConsentReconsidered}
import distantlands.testkit.{Commands, Look, MenuPlay, Outcomes,
  PlacementPicks}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Changing a consent answer through the service (reconsider design,
  * user story, rulings 20 to 26, "Commands"). p2 rules the Narrow Pass;
  * p1 travels and asks about Dunes. */
class ConsentReconsiderServiceSuite extends munit.FunSuite:
  import ConsentFixture._
  private val pass = NarrowPassSite.pass
  private val destination = TravelProcedure.destinationDecisionId
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  private def unavailable(reason: String) = Left(
    GameApplicationError.CommandRejected(
      OathViolation.ConsentUnavailable(reason)))

  private def change(allow: Boolean, granter: PlayerId = p2): GameCommand =
    Commands.reconsiderConsent(granter, p1, destination, dunes, allow)

  /** p1 starts a Travel and asks about Dunes, and p2 answers `choice`. */
  private def answeredBy(service: GameApplicationService,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    val started = startTravel(service)
    val asked = accepted(service.handle(gameId, started.nextSequence,
      requestDunes))
    accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p2, choice)))

  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence, change(allow)))

  private def pawnOfP1(at: GameAccepted)(using munit.Location)
      : Option[SiteId] =
    Look(Outcomes.ready(at.state)).player(p1).pawnSite

  private def parked(at: GameAccepted)(using munit.Location): Boolean =
    MenuPlay.pendingBeyondMenu(Outcomes.ready(at.state)).nonEmpty

  private def dunesOffered(at: GameAccepted)(using munit.Location)
      : DecisionOption =
    present(PlacementPicks.parkedOptions(at.state, powers)
      .find(_.ref == dunes), "Dunes among the destinations")

  private def refusedPlainly(option: DecisionOption): Boolean = option match
    case DecisionOption.Unavailable(_, OptionReason.Noted(_, note)) =>
      note.key == "no-travel"
    case _ => false

  private def marks(at: GameAccepted)(using munit.Location): Set[UseMark] =
    Outcomes.ready(at.state).game.current.useMarks

  /** `base` with p2's answer to p1's request about Dunes already in the
    * ledger: a position the service cannot be played to. */
  private def withP2sAnswerInTheLedger(base: ReadyGame, granted: Boolean,
      use: Option[ConsentUse] = None, power: PowerId = pass): ReadyGame =
    val request = ConsentRequest(p1, destination, dunes,
      Vector(ConsentAsk(p2, Vector(power))), use = use)
    base.copy(consent = accepted(accepted(base.consent.requested(request))
      .answered(p2, granted, ConsentTurn.of(base))))

  test("a refusal changed to a grant reopens Dunes, answers nothing and " +
      "opens no batch"):
    val (service, _) = ConsentFixture.service()
    val no = answeredBy(service, ConsentChoice.No)
    assert(refusedPlainly(dunesOffered(no)))
    val changed = changing(service, no, allow = true)
    assertEquals(changed.events, Vector(ConsentReconsidered(p2, p1,
      destination, dunes, granted = true)))
    assert(parked(changed))
    assert(dunesOffered(changed).available)
    assertEquals(pawnOfP1(changed), pawnOfP1(no))
    assertEquals(changed.undo, no.undo)
    assertEquals(marks(changed), Set.empty)

  test("a late grant can be changed back until the requester selects " +
      "Dunes, and is locked after"):
    val (service, _) = ConsentFixture.service()
    val granted = changing(service, answeredBy(service, ConsentChoice.No),
      allow = true)
    val back = changing(service, granted, allow = false)
    assert(refusedPlainly(dunesOffered(back)))
    val again = changing(service, back, allow = true)
    val picked = accepted(service.handle(gameId, again.nextSequence,
      ParkedServiceFixture.pickTravelDestination(p1,
        TravelFixture.provincePlains)))
    assertEquals(pawnOfP1(picked), Some(TravelFixture.provincePlains))
    assertEquals(marks(picked).map(mark => (mark.requester, mark.target)),
      Set((p1, dunes: DecisionOptionRef)))
    assertEquals(service.handle(gameId, picked.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))

  test("Yes at the prompt makes the move and locks the answer; the " +
      "requester's undo unlocks it; a refusal then greys Dunes"):
    val (service, repository) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    assertEquals(pawnOfP1(yes), Some(TravelFixture.provincePlains))
    assertEquals(service.handle(gameId, yes.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assertEquals(marks(undone), Set.empty)
    val refused = changing(service, undone, allow = false)
    assertEquals(refused.events, Vector(ConsentReconsidered(p2, p1,
      destination, dunes, granted = false)))
    assert(!refused.events.exists(_.isInstanceOf[CommandAccepted]))
    assert(parked(refused))
    assert(refusedPlainly(dunesOffered(refused)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo, game.redo))),
      Right(Some((refused.state, refused.undo, refused.redo))))

  test("a change to a refusal ends redo, as a new command does"):
    val (service, _) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assert(undone.redo.nonEmpty)
    val refused = changing(service, undone, allow = false)
    assertEquals(refused.redo, None)
    assertEquals(service.handle(gameId, refused.nextSequence,
      Commands.redo(p1)), Left(GameApplicationError.CommandRejected(
        OathViolation.NothingToRedo)))

  test("a change to a grant leaves redo, and the redo then succeeds"):
    val (service, repository) = ConsentFixture.service()
    val no = answeredBy(service, ConsentChoice.No)
    // p1 takes back the Travel's start; the refusal stays in the ledger.
    val undone = accepted(service.handle(gameId, no.nextSequence,
      Commands.undo(p1)))
    assert(undone.redo.nonEmpty)
    val granted = changing(service, undone, allow = true)
    assertEquals(granted.redo, undone.redo)
    val redone = accepted(service.handle(gameId, granted.nextSequence,
      Commands.redo(p1)))
    assert(parked(redone))
    assert(dunesOffered(redone).available)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo, game.redo))),
      Right(Some((redone.state, redone.undo, redone.redo))))

  test("a replay of a journal with a taken-back choice sets the same " +
      "marks as live play"):
    val (service, repository) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    val refused = changing(service, undone, allow = false)
    val granted = changing(service, refused, allow = true)
    val picked = accepted(service.handle(gameId, granted.nextSequence,
      ParkedServiceFixture.pickTravelDestination(p1,
        TravelFixture.provincePlains)))
    assertEquals(marks(picked).size, 1)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo))), Right(Some((picked.state, picked.undo))))

  test("a change needs an answer the issuer gave this turn, and one that " +
      "differs"):
    val (service, _) = ConsentFixture.service()
    assertEquals(service.handle(gameId, 0L, change(allow = true)),
      unavailable(
        "p2 answered no consent request about that option this turn"))
    val no = answeredBy(service, ConsentChoice.No)
    assertEquals(service.handle(gameId, no.nextSequence,
      change(allow = true, granter = p3)), unavailable(
        "p3 answered no consent request about that option this turn"))
    assertEquals(service.handle(gameId, no.nextSequence,
      change(allow = false)), unavailable("p2's answer is already that"))

  test("an answer a preference gave can be changed, and the preference " +
      "stays"):
    val (service, _) = ConsentFixture.service()
    val set = accepted(service.handle(gameId, 0L,
      setPreference(ConsentSetting.Never)))
    val started = startTravel(service, set.nextSequence)
    val asked = accepted(service.handle(gameId, started.nextSequence,
      requestDunes))
    val changed = changing(service, asked, allow = true)
    assert(dunesOffered(changed).available)
    assertEquals(consent(changed).preference(p2, pass, p1),
      Some(ConsentPreference.Never))

  test("a change is refused once the game is over, for an automatic " +
      "power, and for a ruler who lost the Pass"):
    val over = withP2sAnswerInTheLedger(passRuledByP2, granted = false)
      .updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.Usurper))))
    assertEquals(ConsentFixture.service(over)._1.handle(gameId, 0L,
      change(allow = true)), unavailable("the game is over"))
    val automatic = withP2sAnswerInTheLedger(passRuledByP2,
      granted = false, use = Some(ConsentUse(TyrantCard.id, p1)))
    assertEquals(ConsentFixture.service(automatic)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("an automatic power's answer cannot be changed"))
    // On the plain board the bandits hold the Pass.
    val lost = withP2sAnswerInTheLedger(TravelFixture.board(),
      granted = false)
    assertEquals(ConsentFixture.service(lost)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("p2 no longer rules site.narrow-pass.pass"))

  test("an ask nobody rules stays final: the Chancellor's Eye"):
    val law = withP2sAnswerInTheLedger(passRuledByP2, granted = false,
      power = ImperialLaws.chancellorsEye)
    assertEquals(ConsentFixture.service(law)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("p2 no longer rules rule.chancellors-eye"))

  test("a change waits while a request about the same option does"):
    val answered = withP2sAnswerInTheLedger(passRuledByP2, granted = true)
    val waiting = answered.copy(consent = answered.consent.copy(pending =
      Some(ConsentRequest(p1, destination, dunes,
        Vector(ConsentAsk(p3, Vector(pass)))))))
    assertEquals(ConsentFixture.service(waiting)._1.handle(gameId, 0L,
      change(allow = false)),
      unavailable("a consent request about that option is waiting"))
```

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.application.ConsentReconsiderServiceSuite"`
Expected: a compile error, `ReconsiderConsent is not a member of
PlayerIntent`.

- [ ] **Step 4: Write the implementation**

In `GameCommands.scala`, after `AnswerConsent`:

```scala
  /** The actor changes the answer they gave `requester` about `target`
    * at `decisionId` this turn: a grant when `allow`, else a refusal
    * (reconsider design, "Commands"). */
  final case class ReconsiderConsent(requester: PlayerId,
      decisionId: String, target: DecisionOptionRef, allow: Boolean)
      extends Consent
```

In `ConsentDesk.scala`, add `ConsentReconsidered` to the `OathEvent`
import, add `import distantlands.gameplay.consent.ConsentUseMarks`, and
add after `answer`:

```scala
  /** `granter` changes the answer they gave `requester` about `target`
    * at `decisionId` this turn (reconsider design, "Commands"; rulings 20
    * to 22). It resumes nothing: a late grant only reopens the option.
    * Refused once the game is over; when `granter` answered no such
    * request this turn; for an automatic power's answer; when `granter`
    * no longer rules a power they were asked about, which also refuses
    * the asks nobody rules (a payer's, an Imperial law's); while the
    * answer is locked; when the answer is already that; and while a
    * request about the same option waits (implementation ruling R8). */
  def reconsider(state: OathState, granter: PlayerId, requester: PlayerId,
      decisionId: String, target: DecisionOptionRef, allow: Boolean)
      : Result =
    def refuse(reason: String): OathViolation =
      OathViolation.ConsentUnavailable(reason)
    for
      ready <- playing(state)
      found <- ready.consent.asked(granter, requester, decisionId, target,
        ConsentTurn.of(ready)).toRight(refuse(s"${granter.value} answered " +
        "no consent request about that option this turn"))
      entry = found._1
      ask = found._2
      _ <- Either.cond(entry.use.isEmpty, (),
        refuse("an automatic power's answer cannot be changed"))
      _ <- ask.powers.find(power =>
          !powers.granter(ready, power).contains(granter))
        .map(power => refuse(
          s"${granter.value} no longer rules ${power.value}")).toLeft(())
      _ <- Either.cond(!ConsentUseMarks.locked(ready, requester, decisionId,
        target), (),
        refuse(s"${requester.value} has acted on this consent"))
      _ <- Either.cond(!ask.answer.contains(allow), (),
        refuse(s"${granter.value}'s answer is already that"))
      _ <- Either.cond(!ready.consent.pending.exists(waiting =>
          waiting.requester == requester &&
            waiting.decisionId == decisionId && waiting.target == target),
        (), refuse("a consent request about that option is waiting"))
    yield Vector(ConsentReconsidered(granter, requester, decisionId, target,
      allow))
```

In `GameApplicationService.routed`, after the `SetConsentPreference`
case:

```scala
      case Issued(granter, PlayerIntent.ReconsiderConsent(requester,
          decisionId, target, allow)) =>
        consentBatch(gameId, journal, consent.reconsider(journal.state,
          granter, requester, decisionId, target, allow), expected,
          nextSequence)
```

In `JournalFold.scala`, replace the consent case of `Progress.next`

```scala
        case event @ (_: ConsentEvent | _: PlanAidEvent) =>
          val settled = closedAt(record.index)
          evolve(settled.state, event).left
            .map(EventReplayFailure(record.index, _))
            .map(after => Progress(settled.recorded(record, after), None))
```

with

```scala
        case event @ (_: ConsentEvent | _: PlanAidEvent) =>
          val settled = closedAt(record.index)
          evolve(settled.state, event).left
            .map(EventReplayFailure(record.index, _))
            .map { after =>
              val recorded = settled.recorded(record, after)
              // An answer changed to a refusal ends redo, as a new batch
              // does: a taken-back choice may have relied on the grant.
              // A change to a grant only permits more, so it leaves redo
              // (reconsider design, implementation ruling R7).
              Progress(event match
                case OathEvent.ConsentReconsidered(_, _, _, _, false) =>
                  recorded.copy(redo = Nil)
                case _ => recorded, None)
            }
```

and add `OathEvent` to the file's `distantlands.model` import if the
wildcard import does not already bring it (it does: `import
distantlands.model._`).

- [ ] **Step 5: Run the suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.application.ConsentReconsiderServiceSuite distantlands.application.PendingWalkerInvariantSuite distantlands.application.GameApplicationServiceStreamSuite distantlands.application.Consent* distantlands.application.*ConsentServiceSuite distantlands.application.JournalFold* distantlands.application.Redo* distantlands.application.Undo*"`
Expected: all pass. In "a change is refused once the game is over...",
if a staged game the service cannot start from is refused for another
reason (a finished game with no stream, for example), build the desk
directly, as `GrandMaskConsentPreferenceSuite` does, and call
`desk.reconsider` on `OathState.Ready(staged)`; report the change.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/application/GameCommands.scala \
  src/main/scala/distantlands/application/ConsentDesk.scala \
  src/main/scala/distantlands/application/GameApplicationService.scala \
  src/main/scala/distantlands/application/JournalFold.scala \
  src/test/scala/distantlands/testkit/Commands.scala \
  src/test/scala/distantlands/application/PendingWalkerInvariantSuite.scala \
  src/test/scala/distantlands/application/GameApplicationServiceStreamSuite.scala \
  src/test/scala/distantlands/application/ConsentReconsiderServiceSuite.scala
git commit -m "feat(consent): let a granter change an answer until it is acted on

ReconsiderConsent goes to the consent desk, which answers with one
ConsentReconsidered or says why not: no such answer this turn, an
automatic power, a power no longer ruled, a locked answer, or no change.
It opens no batch and resumes nothing. A change to a refusal ends redo.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: The wire intent and its mapping

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntentCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntentDecoders.scala`
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala`
- Test: `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/ConsentIntentMapperSuite.scala`

**Interfaces:**
- Consumes: Task 5's `PlayerIntent.ReconsiderConsent`.
- Produces:
  `GameIntent.ReconsiderConsent(requesterId: String, decisionId: String, target: WalkerStartArgWire, allow: Boolean)`,
  on the wire `{"type":"reconsiderConsent","requesterId":…,"decisionId":…,"target":{"optionKind":…,"optionId":…},"allow":true}`.
  Slice 3's frontend sends it.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "SetConsentPreference" -- shared/src/main frontend/src/main src/main
```
Expected: `CommandIntents.scala`, `CommandIntentCodec.scala`,
`CommandIntentDecoders.scala`, `GameIntentMapper.scala`,
`GameCommands.scala`, `GameApplicationService.scala` and the frontend's
`SitePowerLinks.scala`, which only builds one. A match over every
`GameIntent` that is not in this list gains the new case.

- [ ] **Step 2: Write the failing tests**

In `CommandProtocolSuite`, add to `examples` after the
`SetConsentPreference` entry:

```scala
    ReconsiderConsent("blue", "travel.destination",
      WalkerStartArgWire("site", "site:deep-woods"), allow = true),
    ReconsiderConsent("blue", "campaign.targets",
      WalkerStartArgWire("button", "confirm"), allow = false),
```

and add:

```scala
  test("a change of answer needs every field, and a true or false"):
    val whole = ujson.Obj("type" -> "reconsiderConsent",
      "requesterId" -> "blue", "decisionId" -> "travel.destination",
      "target" -> ujson.Obj("optionKind" -> "site",
        "optionId" -> "site:deep-woods"), "allow" -> true)
    def decoded(intent: ujson.Obj) = ActorlessCommandCodec.decode(
      ujson.Obj("expectedNextSequence" -> 0, "intent" -> intent).toString)
    assertEquals(decoded(whole), Right(ActorlessCommandRequest(0L,
      ReconsiderConsent("blue", "travel.destination",
        WalkerStartArgWire("site", "site:deep-woods"), allow = true))))
    Vector("requesterId", "decisionId", "target", "allow").foreach { field =>
      val partial = ujson.Obj.from(whole.value.filterNot(_._1 == field))
      assert(decoded(partial).isLeft, field)
    }
    val worded = ujson.Obj.from(whole.value.toMap.updated("allow",
      ujson.Str("yes")))
    assertEquals(rejected(decoded(worded)),
      ProtocolDecodeFailure.InvalidValue("$.intent.allow",
        "expected boolean"))
    val extra = ujson.Obj.from(whole.value.toMap.updated("granterId",
      ujson.Str("red")))
    assert(decoded(extra).isLeft)
```

In `ConsentIntentMapperSuite`, add:

```scala
  test("a change of answer maps its requester, decision, option and " +
      "answer"):
    assertEquals(GameIntentMapper.map(GameIntent.ReconsiderConsent("blue",
      "travel.destination", WalkerStartArgWire("site", "site:deep-woods"),
      allow = false)),
      Right(PlayerIntent.ReconsiderConsent(PlayerId("blue"),
        "travel.destination",
        DecisionOptionRef.Site(SiteId("site:deep-woods")), allow = false)))
    assertEquals(GameIntentMapper.map(GameIntent.ReconsiderConsent("blue",
      "travel.destination", WalkerStartArgWire("moon", "x"), allow = true)),
      failure("$.intent.target", "unknown decision option 'moon/x'"))
```

- [ ] **Step 3: Run the suites to verify they fail**

Run:
`./sbtw "testOnly distantlands.protocol.CommandProtocolSuite distantlands.application.ConsentIntentMapperSuite"`
Expected: a compile error, `ReconsiderConsent` is not a member of
`GameIntent`.

- [ ] **Step 4: Write the implementation**

In `CommandIntents.scala`, after `SetConsentPreference`:

```scala
  /** Changes the answer the sender gave `requesterId` about `target` at
    * `decisionId` this turn: a grant when `allow`, else a refusal
    * (reconsider design, "Commands"). Not `playerId`: the codec refuses a
    * field that could name the sender. */
  final case class ReconsiderConsent(requesterId: String,
      decisionId: String, target: WalkerStartArgWire, allow: Boolean)
      extends GameIntent
```

In `CommandIntentCodec.encode`, after the `SetConsentPreference` case:

```scala
    case ReconsiderConsent(requester, id, target, allow) =>
      tagged("reconsiderConsent", "requesterId" -> requester,
        "decisionId" -> id,
        "target" -> CommandNestedCodecs.encodeStartArgWire(target),
        "allow" -> allow)
```

In `CommandIntentDecoders.decode`, after the `"setConsentPreference"`
case:

```scala
    case "reconsiderConsent" =>
      for
        _ <- exact(value, Set("type", "requesterId", "decisionId", "target",
          "allow"), path)
        requester <- string(value, "requesterId", path)
        id <- string(value, "decisionId", path)
        target <- field(value, "target", path).flatMap(raw =>
          CommandNestedCodecs.decodeStartArgsWire(ujson.Arr(raw),
            s"$path.target"))
          .flatMap:
            case Vector(one) => Right(one)
            case _ => Left(InvalidValue(s"$path.target",
              "expected one consent target"))
        allow <- field(value, "allow", path).flatMap(
          boolean(_, s"$path.allow"))
      yield ReconsiderConsent(requester, id, target, allow)
```

In `GameIntentMapper.map`, after the `SetConsentPreference` case:

```scala
    case Intent.ReconsiderConsent(requester, id, target, allow) =>
      optionRef(target.optionKind, target.optionId, "$.intent.target")
        .map(PlayerIntent.ReconsiderConsent(PlayerId(requester), id, _,
          allow))
```

If `exact` refuses a missing field rather than only an unknown one, or
`decodeStartArgsWire` words its failure differently, the tests above
still hold: they assert `isLeft` for the missing fields and the exact
failure only for the worded `allow`.

- [ ] **Step 5: Run the suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.protocol.CommandProtocolSuite distantlands.application.ConsentIntentMapperSuite distantlands.gameplay.BackendArchitectureSuite"`
then `./sbtw frontend/compile`.
Expected: all pass, and the frontend compiles with the new intent.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/CommandIntents.scala \
  shared/src/main/scala/distantlands/protocol/CommandIntentCodec.scala \
  shared/src/main/scala/distantlands/protocol/CommandIntentDecoders.scala \
  src/main/scala/distantlands/application/GameIntentMapper.scala \
  shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala \
  src/test/scala/distantlands/application/ConsentIntentMapperSuite.scala
git commit -m "feat(protocol): carry ReconsiderConsent on the command wire

The intent names the requester, the decision, the option and whether to
allow. Every field is required and allow is a boolean.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: The two log lines

**Files:**
- Modify: `src/main/scala/distantlands/application/gamelog/ConsentLines.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogConsentSuite.scala`

**Interfaces:**
- Consumes: `OathEvent.ConsentReconsidered` (Task 3),
  `ConsentState.asked` (Task 2), `Commands.reconsiderConsent` (Task 5).

- [ ] **Step 1: Write the failing tests**

Add to `GameLogConsentSuite`:

```scala
  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.reconsiderConsent(p2, p1,
        distantlands.gameplay.actions.travel.TravelProcedure
          .destinationDecisionId, dunes, allow)))

  test("a changed answer is written for every player, in both " +
      "directions, and names the granter"):
    val (service, _) = ConsentFixture.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    val granted = changing(service, no, allow = true)
    changing(service, granted, allow = false)
    Vector(p1, p2, p3).foreach { viewer =>
      val lines = texts(log(service, viewer))
      val gives = lines.indexOf(
        "p2 now gives consent to ignore Narrow Pass and select Dunes.")
      val withdraws = lines.indexOf("p2 no longer gives consent to " +
        "ignore Narrow Pass and select Dunes.")
      assert(gives >= 0, lines)
      assert(withdraws > gives, lines)
      assert(lines.indexOf("p2 gave no consent.") < gives, lines)
    }

  test("a changed answer's line is never struck through by the " +
      "requester's undo"):
    val (service, _) = ConsentFixture.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    val refused = changing(service, undone, allow = false)
    accepted(service.handle(gameId, refused.nextSequence,
      Commands.undo(p1)))
    val line = present(log(service, p1).find(entry =>
      entry.spans.map(_.text).mkString == "p2 no longer gives consent " +
        "to ignore Narrow Pass and select Dunes."), "the change's line")
    assertEquals(line.takenBack, None)
```

Move the `TravelProcedure` name into the suite's imports
(`import distantlands.gameplay.actions.travel.TravelProcedure`) and use
`TravelProcedure.destinationDecisionId` in `changing`.

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogConsentSuite"`
Expected: the two new tests fail: the log holds no such lines
(`gives >= 0` fails; "the change's line" is absent).

- [ ] **Step 3: Write the implementation**

In `ConsentLines.scala`, add `ConsentReconsidered` to the `OathEvent`
import, extend the class doc with "A changed answer is public and names
its granter, in two parallel wordings (reconsider design, "Game Log").",
and add this case after `ConsentAnswered`'s:

```scala
        // "{Red} now gives consent to ignore {Narrow Pass} and select
        // {Deep Woods}." or "{Red} no longer gives consent to ignore
        // {Narrow Pass} and select {Deep Woods}." The powers are the
        // changer's own on that request; the option reads as the
        // request's line reads it.
        case ConsentReconsidered(granter, requester, decisionId, target,
            granted) =>
          val own = before.consent.asked(granter, requester, decisionId,
            target, ConsentTurn.of(before)).toVector.flatMap(_._2.powers)
          line((words.player(granter) +: Text(
            if granted then " now gives consent to ignore "
            else " no longer gives consent to ignore ") +: named(own)) ++
            (Text(" and select ") +: choices.option(target, before, after,
              viewer)) :+ Text("."))
```

- [ ] **Step 4: Run the log suites to verify they pass**

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: all pass, with no golden log changed. A changed golden log
stops the work: report the diff.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/gamelog/ConsentLines.scala \
  src/test/scala/distantlands/application/gamelog/GameLogConsentSuite.scala
git commit -m "feat(log): say when a granter changes a consent answer

Two public lines, worded in parallel: the granter now gives consent, or
no longer gives it, to ignore their powers and select the option.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 8: The designs and the rule map

**Files:**
- Modify: `docs/superpowers/specs/2026-10/2026-10-08-consent-system-design.md`
  (rulings 12 and 13)
- Modify: `docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md`
  (status)
- Modify: `docs/architecture/rule-resolution.md` (the consent paragraph)
- Modify: `docs/rules/implementation-traceability.md` (the "Enemies
  cannot" consent row)

- [ ] **Step 1: Amend rulings 12 and 13 in the consent design**

In ruling 12 replace the two sentences

> A refused target stays unavailable. A granted target can be selected
> again without asking.

with

> A refused target stays unavailable, and a granted target stays
> selectable without asking, until its granter reconsiders (amended by
> the reconsider design, rulings 20 to 26).

There "reconsider design" is a link to
`../2026-11/2026-11-20-consent-reconsider-design.md`.

Replace ruling 13 with

> 13. **Answers never block undo.** The requester may undo past a consent
>     answer, as far as the ordinary barriers allow. The answer stays
>     remembered and its log lines are never struck through. An undo may
>     unlock an answer: one is locked while the move it decided stands,
>     and its granter may change it again once the requester undoes that
>     move (amended by the reconsider design, ruling 21).

- [ ] **Step 2: Update the reconsider spec's status**

Replace the sentence that begins "Slice 1 is implemented" and ends
"slices 2 and 3 are not." with

```markdown
Slices 1 and 2 are implemented
([slice 1 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice1.md),
[slice 2 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice2.md));
slice 3 is not.
```

and "amends its rulings 12 and 13" stays as it is.

- [ ] **Step 3: Note the change in the rule map**

In `docs/architecture/rule-resolution.md`, at the end of the paragraph
that ends with `gameplay/consent/ConsentEvolution.scala`, add:

> A granter may change an answer of the turn until the requester acts on
> it (`ConsentState.reconsidered`, `ConsentDesk.reconsider`). The lock is
> a use mark the fold of a recorded answer sets in the game
> (`gameplay/consent/ConsentUseMarks.scala`), so an undo of that answer
> removes it.

In `docs/rules/implementation-traceability.md`, in the "“Enemies
cannot” consent enforcement" row's evidence cell, after "answers last
the turn and survive undo," add "and their granter may change one until
the requester acts on it (`ConsentReconsiderServiceSuite`,
`ConsentUseMarksSuite`),".

- [ ] **Step 4: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`
Expected: passed.

```bash
git add docs/superpowers/specs/2026-10/2026-10-08-consent-system-design.md \
  docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md \
  docs/architecture/rule-resolution.md \
  docs/rules/implementation-traceability.md
git commit -m "docs(consent): amend rulings 12 and 13 for reconsidering

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 9: The full gate

- [ ] **Step 1: Run the full gate**

Run, in the background or with a long timeout:
`./sbtw test`, `./sbtw frontend/test`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass. Report the test counts sbt prints. This slice has no
screen, so there is no browser check.

---

## Self-Review

- **Spec coverage.**
  - Rulings 20 and 22 to 24: Task 2 (`reconsidered`), Task 5 (desk).
    Ruling 21 (lock and unlock): Task 4 (mark), Task 5 (the lock check
    and the undo tests). Ruling 25: Task 5 (no batch; the requester
    undoes past it) and Task 7 (the line is never struck). Ruling 26:
    Task 5's "changed back" test and Task 7.
  - "State": `history` and its turn (Task 1); one entry per requester,
    decision, option and answering granter (Task 1, R10); `reconsidered`
    (Task 2); the use mark in `CurrentGameState`, its fold, choose-many,
    undo (Task 4, Task 5).
  - "Events": Task 3. "Commands": Task 5 (each refusal reason has a
    test) and Task 6 (the wire). "Game Log": Task 7.
  - "Testing": ledger (Tasks 1 and 2); use mark and undo (Tasks 4 and
    5); desk (Task 5); codecs for the event and the intent (Tasks 3 and
    6); Game Log (Task 7). The projection, the frontend and the browser
    belong to slice 3.
  - "Slices": the amendment of rulings 12 and 13 (Task 8).
  - "Not reconsidered" and "Deferred": the automatic power's refusal
    (Task 5); the asks nobody rules, refused by the rule check (Task 5
    tests the Chancellor's Eye; the other three are read, not tested).
- **The orchestrator's five points.** The answer event and its fold, and
  why a replay sets the same marks: "What the code says" and Task 4, with
  the replay test in Task 5. The mark's turn: R11. The keys a change
  rewrites and their readers: "What the code says" and R9. The entry's
  identity: R10. The service path and what runs after: "What the code
  says" and Task 5; R7 and R8 settle the two cases the path left open.
- **Placeholder scan.** Every code step shows its code. Three steps name
  a fallback for a fact only a run can show (the architecture check on
  the import, staging a finished game through the service, a missing
  wire field); each says what to do and to report it.
- **Type consistency.** `historyAt`, `asked`, `grantedTo` and
  `reconsidered` keep one signature across Tasks 1, 2, 4, 5 and 7.
  `UseMark(turn, requester, decisionId, target)` and
  `CurrentGameState.useMarks` are spelled the same in Tasks 4 and 5.
  `ConsentReconsidered(granter, requester, decisionId, target, granted)`
  is the same in Tasks 3, 5 and 7. `PlayerIntent.ReconsiderConsent(
  requester, decisionId, target, allow)` and
  `GameIntent.ReconsiderConsent(requesterId, decisionId, target, allow)`
  are the same in Tasks 5 and 6.
