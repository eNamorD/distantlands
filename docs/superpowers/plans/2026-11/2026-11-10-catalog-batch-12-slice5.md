# Catalog Batch 12, Slice 5: A Remote, Private Deal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Whispering Stone: for 1 secret, its holder negotiates
with any one other player, wherever their pawn is, and players outside
the deal never see its terms or its promises.

**Architecture:** The Stone is a `PaidAction` that queues a Negotiation
`Run`, sourced on the relic, carrying one start argument: the private
marker. The Negotiation accepts the marker only from such an entry, and
then offers every other player as a candidate, asks for exactly one, and
sets `DealRules.privateDeal`, which the snapshot copies onto the
`Negotiate` query. The projector withholds a private deal's terms from
outsiders. `LogNote` gains `readers`: the promise lines of a private deal
are read only by its participants, and the Game Log formatter drops a
line for any other viewer after numbering every line.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../../specs/2026-11/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (21 to 23, 30), "The deal model" and "Slice 5: A
remote, private deal".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, parameters or values, no
  non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`). `gameplay.powers` may import
  `gameplay.actions`.
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire and journal changes need not keep old journals decodable. The new
  projection key `privateDeal` is required. The new note key `readers` is
  written only when set, as `source` is: its absence means every viewer.
- Ruling 21: the Stone costs 1 secret, and the actor chooses any one other
  player, wherever their pawn is.
- Ruling 22: otherwise a full Negotiation. Disclosures, the Citizenship
  offer and When Negotiating powers apply as in the Negotiate action.
- Ruling 23: players outside the deal see who is negotiating and when the
  deal closes, not its terms. The settlement's transfer and disclosure
  lines are public. Promise lines show only to the two participants.
- Ruling 30: the Citizenship offer line of a private deal is public.
- The package is `distantlands`. This plan anchors edits by code, not by
  line: slice 4 changes several of the files below.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Setup

Slice 4 (`docs/superpowers/plans/2026-11/2026-11-10-catalog-batch-12-slice4.md`)
must be merged into `0.2.0-alpha` first. This plan uses its
`NegotiationStartFacts.deedWriterUsers`, its `DealRules.siteTraders`, its
`DecisionQuery.Negotiate.shortOfWarbands`, the placement step it adds to
the procedure's settlement Branch, and its switch of the
NegotiationProcedureSuite unsupported-rule test to Whispering Stone.
Check with:

```bash
git grep -n "deedWriterUsers\|siteTraders\|shortOfWarbands" -- src/main/scala/distantlands/gameplay/actions/negotiation src/main/scala/distantlands/model/Decisions.scala
```

```bash
git grep -n "WhisperingStoneCard" -- src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala
```

Both must print matches. In a new worktree, symlink the main checkout's
`.tooling` and `node_modules` before running `./sbtw`.

## Plan-time findings

1. **The Stone queues the Negotiation.** A phase power's `build` receives
   neither the walk's power-use restrictions nor start facts, and its
   rebuild re-reads live state. The Negotiation walk has both, and every
   slice's terms (binding promises, advisers, sites, Citizenship). So the
   Stone queues a Negotiation `Run` at the front of the queue, as Wild
   Allies queues a Campaign (`CampaignFromSite`). The spec's slice 5
   section is revised to say so.
2. **Decision ids stay `negotiation.*`.** The private deal is a
   Negotiation walk of its own, so no answers of another deal can mix with
   its answers. The per-deal prefix moves to slice 6, the first slice
   with two deals in one walk. The spec is revised.
3. **Only a power passes the marker.** The Negotiation takes
   `NegotiationProcedure.privateArgs` only while the walk's entry is a
   sourced `Run` of the Negotiation for the actor carrying it, as Travel
   takes a queued destination (`TravelProcedure.startArgs`). A command
   that starts a Negotiation with the marker from the menu is refused.
   The registry passes no start arguments to `startFacts`, so it reads the
   entry (`queuedPrivately`). A rebuild reads its start arguments.
4. **One player, asked as the negotiators question.** The private start's
   candidates are every other seated player, in table order. The
   negotiators question becomes `ChooseMany(1, 1, ...)` and is omitted
   when there is one other player, as now.
5. **`privateDeal` is true for every viewer of a private deal.** The
   projector withholds the terms only from a viewer outside the deal, and
   the panel shows its "negotiating in private" line only to such a
   viewer.
6. **Readers on the wire.** `readers` is written only when set, as
   `source` is, its ids sorted so the journal is stable. An empty or
   repeated list is refused.
7. **Drop after numbering.** `formatLive` numbers a step's lines, then
   drops each one whose readers exclude the viewer, so every other line
   keeps its `(sequence, ordinal)`. The two reads of earlier entries
   (`quietWake` and the follow-up section) are unaffected: a promise line
   never stands next to a Wake headline, and its run's opening line is
   public.
8. **The properties.** "Every viewer receives the same entries" is judged
   on `formatEveryLine`, which keeps every line, worded for the viewer.
   Prefix stability stays on `format`.
9. **The unsupported-rule test goes.** Slice 4 left one test in
   NegotiationProcedureSuite proving that holding an unimplemented
   Negotiation card does not block a Negotiation, with Whispering Stone
   as its card. Once the Stone is implemented, no unimplemented card
   modifies a Negotiation by being held, so the test is deleted.
10. **The Stone writes no line.** The Game Log's "Used Whispering Stone"
    fallback names the card, and the Negotiation's opening line names the
    partner.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/LogNotes.scala` | `LogNote.readers`, `readBy` |
| `src/main/scala/distantlands/serialization/WalkerEventCodec.scala` | `readers` key |
| `src/main/scala/distantlands/application/gamelog/Posted.scala` | `readers` |
| `src/main/scala/distantlands/application/gamelog/NoteLines.scala` | passes `readers` on |
| `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala` | drop after numbering, `formatEveryLine` |
| `src/main/scala/distantlands/gameplay/powers/action/WhisperingStone.scala` | new: card and power |
| `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala` | Whispering Stone leaves |
| `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala` | registers the power |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | the private start, opening line, promise readers |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala` | `at(..., privately)` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala` | `privateDeal` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | snapshot copies `privateDeal` |
| `src/main/scala/distantlands/model/Decisions.scala` | `Negotiate.privateDeal` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala` | `NegotiationDealProjection.privateDeal` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala` | `privateDeal` key |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | withholds terms from outsiders |
| `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala` | the outsider's line |
| `src/test/resources/powers/unimplemented.txt` | drops `relic.whispering-stone` |
| `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`, `LogScripts.scala` | `negotiationWhispered` |
| `src/test/resources/gamelog/negotiation-whispered.{actor,other}.log` | new goldens |
| `docs/architecture/all-exile-negotiation.md`, `docs/ROADMAP.md`, the spec | docs |

---

### Task 1: Log lines with readers

**Files:**
- Modify: `src/main/scala/distantlands/model/LogNotes.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerEventCodec.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/Posted.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/NoteLines.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala`
- Test: `src/test/scala/distantlands/serialization/LogNoteWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogReadersSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces:
  - `LogNote(source, key, args, readers: Option[Set[PlayerId]] = None)`
    and `LogNote.readBy(players: Iterable[PlayerId]): LogNote`.
  - `GameLogFormatter.formatEveryLine(steps, viewer): Vector[LogEntry]`,
    `private[gamelog]`.

- [ ] **Step 1: Write the failing wire tests**

Append to `LogNoteWireSuite`:

```scala
  test("a note's readers round trip, sorted, and an unread note writes " +
      "no readers field"):
    val read = LogNoted(PowerId("procedure.negotiation"),
      LogNote(None, "promised", Vector(NoteArg.Promise("Peace.")))
        .readBy(Vector(PlayerId("p3"), PlayerId("p1"))), covers = false)
    val encoded = accepted(GameEventWire.encodeEvent("notes", 0, read))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(read))
    assertEquals(ujson.read(encoded)("payload")("note")("readers"),
      ujson.Arr("p1", "p3"))
    val unread = read.copy(note = read.note.copy(readers = None))
    val plain = accepted(GameEventWire.encodeEvent("notes", 0, unread))
    assert(!ujson.read(plain)("payload")("note").obj.contains("readers"),
      plain)

  test("an empty or repeated readers list is refused"):
    val read = LogNoted(PowerId("procedure.negotiation"),
      LogNote(None, "promised", Vector.empty).readBy(Vector(PlayerId("p1"))),
      covers = false)
    Vector(ujson.Arr(), ujson.Arr("p1", "p1")).foreach { readers =>
      val encoded = ujson.read(accepted(
        GameEventWire.encodeEvent("notes", 0, read)))
      encoded("payload")("note")("readers") = readers
      assertEquals(GameEventWire.decode(encoded).map(_.event),
        Left(WireError.InvalidValue("$.payload.note.readers",
          "readers must name each player once")))
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.serialization.LogNoteWireSuite"`
Expected: compile failure, `readBy` is not a member of `LogNote`.

- [ ] **Step 3: Give `LogNote` its readers**

In `LogNotes.scala`, replace the `LogNote` case class with:

```scala
/** One line a power or procedure writes. `readers`, when set, are the only
  * players whose Game Log shows it (catalog batch 12, ruling 23: a private
  * deal's promises); `None` shows it to every viewer, spectators too. */
final case class LogNote(source: Option[PowerSourceRef], key: String,
    args: Vector[NoteArg], readers: Option[Set[PlayerId]] = None):
  require(readers.forall(_.nonEmpty), "a note's readers name a player")

  /** This note, shown only to `players`. */
  def readBy(players: Iterable[PlayerId]): LogNote =
    copy(readers = Some(players.toSet))
```

Keep any doc comment the class already has above it, merged into this
one.

- [ ] **Step 4: Write and read the key**

In `WalkerEventCodec.scala`, `encodeNote` gains, after the `source` line:

```scala
    note.readers.foreach(readers => fields("readers") =
      ujson.Arr.from(readers.toVector.map(_.value).sorted.map(ujson.Str(_))))
```

`decodeNote` reads it after `args`, and yields it:

```scala
    readers <- value.obj.get("readers") match
      case None => Right(None)
      case Some(field) =>
        decodeReaders(field, s"$path.readers").map(Some.apply)
  yield LogNote(source, value("key").str, args, readers)
```

Add beside `decodeNote`:

```scala
  /** A note's readers: each player once, and at least one. */
  private def decodeReaders(value: ujson.Value,
      path: String): Either[WireError, Set[PlayerId]] =
    val ids = value.arr.toVector.map(_.str)
    Either.cond(ids.nonEmpty && ids.distinct == ids,
      ids.map(PlayerId(_)).toSet,
      InvalidValue(path, "readers must name each player once"))
```

Run: `./sbtw "testOnly distantlands.serialization.LogNoteWireSuite"`
Expected: PASS.

- [ ] **Step 5: Write the failing formatter tests**

Create `src/test/scala/distantlands/application/gamelog/GameLogReadersSuite.scala`:

```scala
package distantlands.application.gamelog

import distantlands.engine.ReplayStep
import distantlands.gameplay.actions.negotiation.NegotiationProcedure
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.protocol.projection.LogEntry
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts._

/** A note with readers posts only for them (catalog batch 12, ruling 23),
  * and every other line keeps its keys for every viewer. */
class GameLogReadersSuite extends munit.FunSuite:
  private val promise = s"${playerLabel(p1)} promised ${playerLabel(p2)}: " +
    "“I will not attack you this round.”"

  /** `negotiationPromised`'s journal, its promise line read only by the
    * two negotiators. p3 stands outside the deal. */
  private def promiseReadByTheNegotiators(using munit.Location)
      : Vector[ReplayStep[OathState, OathEvent]] =
    negotiationPromised.history.steps.map(step => step.event.event match
      case noted @ LogNoted(_, note, _)
          if note.key == NegotiationProcedure.promised.name =>
        step.copy(event = step.event.copy(
          event = noted.copy(note = note.readBy(Vector(p1, p2)))))
      case _ => step)

  private def keys(entries: Vector[LogEntry]): Vector[(Long, Int)] =
    entries.map(entry => (entry.sequence, entry.ordinal))

  test("a note's readers read its line, and nobody else does"):
    val steps = promiseReadByTheNegotiators
    Vector(Some(p1) -> 1, Some(p2) -> 1, Some(p3) -> 0, None -> 0).foreach {
      (viewer, count) =>
        val all = texts(formatter.format(steps, viewer))
        assertEquals(all.count(_ == promise), count, s"$viewer: $all")
    }

  test("the lines around a dropped line keep their keys"):
    val steps = promiseReadByTheNegotiators
    val reader = formatter.format(steps, Some(p1))
    assertEquals(keys(formatter.format(steps, Some(p3))),
      keys(reader.filterNot(entry => text(entry) == promise)))

  test("every line, read-restricted ones too, is what a reader receives"):
    val steps = promiseReadByTheNegotiators
    assertEquals(keys(formatter.formatEveryLine(steps, Some(p3))),
      keys(formatter.format(steps, Some(p1))))
```

- [ ] **Step 6: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogReadersSuite"`
Expected: compile failure, `formatEveryLine` is not a member.

- [ ] **Step 7: Carry readers to the line and drop it**

In `Posted.scala`, the case class gains a last field:

```scala
private[gamelog] final case class Posted(kind: LogKind,
    spans: Vector[LogSpan], section: Option[LogSection] = None,
    readers: Option[Set[PlayerId]] = None)
```

Import `distantlands.model.PlayerId` there, and add to its doc comment:
"`readers`, when set, are the only viewers it posts for (a note's
`readers`)."

In `NoteLines.line`, the yield becomes:

```scala
      yield Posted(kind(run, power, note), spans, readers = note.readers)
```

In `GameLogFormatter.scala`:

- Rename the public `format(steps, viewer)` body into a private
  `format(steps, viewer, everyLine: Boolean)`, and pass `everyLine` to
  both `formatLive` calls in it.
- Add the two entry points in its place, with its doc comment kept on the
  first:

```scala
  def format(steps: Vector[ReplayStep[OathState, OathEvent]],
      viewer: Option[PlayerId]): Vector[LogEntry] =
    format(steps, viewer, everyLine = false)

  /** Every line, read-restricted ones too, each worded for `viewer`: what
    * `format` gives before it drops the lines `viewer` may not read. The
    * Game Log properties compare viewers on it. */
  private[gamelog] def formatEveryLine(
      steps: Vector[ReplayStep[OathState, OathEvent]],
      viewer: Option[PlayerId]): Vector[LogEntry] =
    format(steps, viewer, everyLine = true)
```

- `formatLive` takes `everyLine: Boolean` as its last parameter, and its
  numbering becomes:

```scala
        // Numbered first, then dropped for a viewer outside the line's
        // readers, so every other line keeps its (sequence, ordinal).
        (next, entries ++ posted.zipWithIndex.collect {
          case (entry, ordinal) if everyLine ||
              entry.readers.forall(readers => viewer.exists(readers)) =>
            LogEntry(journal.sequence(at), ordinal, entry.kind,
              entry.spans, entry.section) })
```

- [ ] **Step 8: Judge the same-entries property before the drop**

In `GameLogPropertiesSuite`, the test "every viewer receives the same
entries with the same keys" becomes:

```scala
  test("every viewer receives the same entries with the same keys, " +
      "before the read-restricted lines are dropped"):
    scripts.foreach { script =>
      val shapes = viewers(script).map(viewer => formatter
        .formatEveryLine(script.history.steps, viewer)
        .map(entry => (entry.sequence, entry.ordinal, entry.kind, entry.depth,
          entry.section)))
      shapes.tail.foreach(shape => assertEquals(shape, shapes.head,
        script.name))
    }
```

- [ ] **Step 9: Run the tests**

Run: `./sbtw "testOnly distantlands.serialization.LogNoteWireSuite distantlands.application.gamelog.GameLogReadersSuite distantlands.application.gamelog.GameLogPropertiesSuite distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: PASS. No golden changes: no note sets readers yet.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/model/LogNotes.scala src/main/scala/distantlands/serialization/WalkerEventCodec.scala src/main/scala/distantlands/application/gamelog/Posted.scala src/main/scala/distantlands/application/gamelog/NoteLines.scala src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala src/test/scala/distantlands/serialization/LogNoteWireSuite.scala src/test/scala/distantlands/application/gamelog/GameLogReadersSuite.scala src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala
git commit -m "$(cat <<'EOF'
feat(gamelog): show a note only to its readers

A log note may name its readers. The formatter numbers every line, then
drops a line for any viewer outside its readers, so the other lines keep
their keys. Catalog batch 12, ruling 23.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Whispering Stone and the private start

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/action/WhisperingStone.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/model/Decisions.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify: `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/action/WhisperingStoneSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala`

**Interfaces:**
- Consumes: Task 1's `LogNote.readBy`.
- Produces:
  - `WhisperingStoneCard` and `WhisperingStone` (a `PaidAction`) in
    `distantlands.gameplay.powers.action`.
  - `NegotiationProcedure.privateArgs: Vector[DecisionOptionRef]`,
    `NegotiationProcedure.queuedPrivately(state, actor): Boolean` and the
    note key `NegotiationProcedure.negotiatedPrivately`.
  - `NegotiationStartFacts.at(catalog, ready, actor, restrictions,
    privately: Boolean = false)`.
  - `DealRules.privateDeal: Boolean = false` and
    `DecisionQuery.Negotiate.privateDeal: Boolean = false`, both last.

- [ ] **Step 1: Write the failing power tests**

Create `src/test/scala/distantlands/gameplay/powers/action/WhisperingStoneSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  NegotiationDeal, NegotiationProcedure}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.ParkedDecisionAssertions
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseManyAnswer
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, ParkedNode,
  Table}
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Whispering Stone (relic R34): "**ACTION:** Negotiate with any one
  * player in private, even if their pawn is not at your site." (catalog
  * batch 12, rulings 21 to 23). */
class WhisperingStoneSuite extends munit.FunSuite:
  private val rules = ActPowerHarness.rules()
  private val parked = new ParkedDecisionAssertions(catalog)
  private val stone = WhisperingStoneCard.id
  private val source = DecisionOptionRef.Relic(stone)

  /** p1 at Ancient City holds Whispering Stone with 1 secret to pay; p2
    * stands at Broken Peaks and p3 at Buried Giant, so no pawn shares p1's
    * site. */
  private def strangers: Table = Table.start
    .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
    .pawn(p3, BuriedGiantSite).relic(p1, stone).secrets(p1, faceUp = 1)

  /** The query `state` is parked on. */
  private def parkedQuery(state: OathState)(using munit.Location)
      : DecisionQuery = ParkedNode.of(state, catalog) match
    case Right(Some(ParkedNode.Decision(_, decide, _))) => decide.query
    case other => fail(s"not parked on a decision: $other")

  /** The Stone used, then `partner` chosen: parked on the deal. */
  private def whisperingWith(table: Table, partner: PlayerId)(
      using munit.Location): OathState =
    val used = accepted(ActPowerHarness.use(rules, table.ready,
      WhisperingStone.id, source))
    accepted(rules.resolveWalker(used.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
        DecisionOptionRef.Player(partner))))).state

  test("the Stone asks for exactly one other player, wherever their pawn " +
      "is"):
    val used = accepted(ActPowerHarness.use(rules, strangers.ready,
      WhisperingStone.id, source))
    parked.assertParked(used.state, ActionRef.Negotiation,
      NegotiationDeal.negotiatorsDecisionId, p1)
    parkedQuery(used.state) match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max), (1, 1))
        assertEquals(many.options.map(_.ref),
          Vector(p2, p3).map(DecisionOptionRef.Player(_)))
      case other => fail(s"not the negotiators question: $other")

  test("the Stone costs 1 secret, paid onto it"):
    val used = accepted(ActPowerHarness.use(rules, strangers.ready,
      WhisperingStone.id, source))
    val look = Look(ready(used.state))
    assertEquals(look.faceUpSecrets(p1), 0)
    assertEquals(look.tokensOn(WhisperingStoneCard).secrets, 1)

  test("the chosen player joins a private deal"):
    val state = whisperingWith(strangers, p3)
    parked.assertParked(state, ActionRef.Negotiation,
      NegotiationDeal.dealDecisionId, p1)
    parkedQuery(state) match
      case deal: DecisionQuery.Negotiate =>
        assertEquals(deal.participants, Vector(p1, p3))
        assert(deal.privateDeal)
      case other => fail(s"not a deal: $other")

  test("a Negotiation from the menu may not carry the private marker"):
    assertEquals(rules.startWalker(
        OathState.Ready(MenuPlay.withoutMenu(strangers.ready)),
        ActionRef.Negotiation, p1, NegotiationProcedure.privateArgs)
      .left.toOption,
      Some(OathViolation.InvalidEventOrder("walker procedure negotiation " +
        "takes no start selection but a power's private marker, got " +
        "button")))

  test("Council Arbiter widens the private deal"):
    val state = whisperingWith(strangers.adviser(p1, CouncilArbiterCard.id),
      p3)
    parkedQuery(state) match
      case deal: DecisionQuery.Negotiate =>
        assertEquals(deal.bounds(p3).adviserRecipients, Vector(p1))
      case other => fail(s"not a deal: $other")
```

Append to `NegotiationStartFactsSuite`:

```scala
  test("a private start's candidates are every other player, wherever " +
      "their pawns stand"):
    val ready = distantlands.testkit.Table.start
      .pawn(p1, distantlands.gameplay.cards.AncientCitySite)
      .pawn(p2, distantlands.gameplay.cards.BrokenPeaksSite)
      .pawn(p3, distantlands.gameplay.cards.BuriedGiantSite).ready
    val catalog = distantlands.gameplay.setup.FirstGameSetupFixture.catalog
    assertEquals(NegotiationStartFacts.at(catalog, ready, p1, Vector.empty,
      privately = true).candidates, Vector(p2, p3))
    assertEquals(NegotiationStartFacts.at(catalog, ready, p1, Vector.empty)
      .candidates, Vector.empty)
```

Move the fully qualified names into the suite's imports.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.WhisperingStoneSuite distantlands.gameplay.NegotiationStartFactsSuite"`
Expected: compile failure, `WhisperingStone` not found.

- [ ] **Step 3: The card and its power**

Delete `WhisperingStoneCard` from `HoldingRelics.scala`. `AncientWritCard`
stays, with the file's imports unchanged.

Create `src/main/scala/distantlands/gameplay/powers/action/WhisperingStone.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.catalog.{ExecutableCatalog, PrintedPower, Relic}
import distantlands.gameplay.actions.negotiation.NegotiationProcedure
import distantlands.model._

object WhisperingStoneCard extends Relic(RelicId("R34"), "Whispering Stone",
    value = 13, defense = 2):
  val power = PrintedPower(PowerId("relic.whispering-stone"),
    persistent = false, cost = Cost(secret = 1),
    text = "**ACTION:** Negotiate with any one player in private, even " +
      "if their pawn is not at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Whispering Stone (relic R34), ACTION, 1 secret (catalog batch 12,
  * rulings 21 to 23).
  *
  * It pays its cost onto the relic and queues a Negotiation `Run` at the
  * front of the queue, sourced on the relic, carrying the private marker
  * ([[NegotiationProcedure.privateArgs]]). The queued Negotiation asks for
  * the one player, reads its When Negotiating powers as the Negotiate
  * action does, and keeps its terms from players outside the deal. The
  * Stone writes no line of its own: the Game Log's "Used Whispering Stone"
  * names it, and the Negotiation's opening line names the partner.
  */
case object WhisperingStone extends PaidAction(WhisperingStoneCard.power):
  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    source match
      case DecisionOptionRef.Relic(stone) =>
        Right(QueueProcedure(entry(player, stone), atFront = true))
      case other => Left(OathViolation.InvalidEventOrder(
        s"${other.kind} is not a relic source"))

  /** The private Negotiation the Stone queues for `player`. */
  def entry(player: PlayerId, stone: RelicId): QueuedProcedure =
    QueuedProcedure(player, ActionRef.Negotiation,
      NegotiationProcedure.privateArgs, Some(PowerSourceRef.Card(stone)),
      QueueKind.Run)
```

In `PhasePowerCatalog.scala`, add `WhisperingStone` to the
`distantlands.gameplay.powers.action` import, and to the list after
`TrackerWake`:

```scala
        TrackerAction, TrackerWake, WhisperingStone) ++
```

`NewFoundations.scala` imports `distantlands.gameplay.powers.action._`,
so its `WhisperingStoneCard` entry needs no change. Drop the line
`relic.whispering-stone` from `src/test/resources/powers/unimplemented.txt`.

- [ ] **Step 4: The private start's facts**

In `NegotiationStartFacts.scala`, `at` gains a last parameter and its
candidates follow it:

```scala
  /** The facts at a fresh start. A private start (Whispering Stone,
    * ruling 21) offers every other player, wherever their pawn stands. */
  def at(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction],
      privately: Boolean = false): NegotiationStartFacts =
    val candidates =
      if privately then
        ready.game.current.players.map(_.player).filter(_ != actor)
      else NegotiationDeal.eligible(catalog, ready, actor, restrictions)
```

The rest of `at` is unchanged: the users are read over the actor and
these candidates.

- [ ] **Step 5: `privateDeal` on the rules and the query**

In `DealRules.scala`, the case class gains a last field
`privateDeal: Boolean = false`, and its doc comment gains: "`privateDeal`
keeps the terms from players outside the deal (Whispering Stone, ruling
23)."

In `Decisions.scala`, `DecisionQuery.Negotiate` gains a last field:

```scala
      /** The deal is private (Whispering Stone, ruling 23): a viewer
        * outside it is shown who negotiates, never the terms. */
      privateDeal: Boolean = false
```

In `NegotiationDeal.snapshot`, the `DecisionQuery.Negotiate(...)` call
gains `privateDeal = rules.privateDeal` as its last argument.

- [ ] **Step 6: The private start in the procedure**

In `NegotiationProcedure.scala`:

Add beside `negotiated`:

```scala
  /** "Negotiated with p3, in private." (Whispering Stone, ruling 23). */
  val negotiatedPrivately: NoteKey = notes.key("negotiated-private", Vector(
    NotePart.Text("Negotiated with "), NotePart.Arg(0),
    NotePart.Text(", in private.")))
```

Add after `noteKeys`:

```scala
  /** The one start argument of a private Negotiation, which only a
    * power's queued `Run` passes (Whispering Stone, ruling 21). */
  val privateArgs: Vector[DecisionOptionRef] =
    Vector(DecisionOptionRef.Button("negotiation.private"))

  /** Whether `actor`'s Negotiation walk is a power's private one: its
    * entry is a sourced `Run` of the Negotiation carrying the private
    * marker. A menu Negotiation has no entry. */
  def queuedPrivately(state: ReadyGame, actor: PlayerId): Boolean =
    state.game.current.walkerEntry.exists(entry =>
      entry.procedure == ActionRef.Negotiation && entry.actor == actor &&
        entry.source.nonEmpty && entry.kind == QueueKind.Run &&
        entry.startArgs == privateArgs)
```

`build` becomes:

```scala
  /** A fresh start: the reach is read once, under the walk's power-use
    * restrictions, and refused when it holds nobody. A private start
    * offers every other player. */
  def build(limits: AdviserLimits,
      restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      privately <- marked(args)
      _ <- Either.cond(!privately || queuedPrivately(state, actor), (),
        refusedArgs(args))
      facts = NegotiationStartFacts.at(catalog, state, actor, restrictions,
        privately)
      _ <- Either.cond(facts.candidates.nonEmpty, (),
        OathViolation.NegotiationUnavailable(
          "no other player is at your site or within reach"))
    yield tree(catalog, limits, actor, facts, privately)
```

`rebuild` becomes:

```scala
  /** A resume rebuilds what the start built, from the facts it stored and
    * the start arguments. */
  def rebuild(limits: AdviserLimits)(catalog: ExecutableCatalog,
      state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      privately <- marked(args)
      facts <- NegotiationStartFacts.read(state.game.current.walkerStartFacts)
    yield tree(catalog, limits, actor, facts, privately)
```

`startFacts` passes the entry's privacy, since the registry passes it no
start arguments:

```scala
  def startFacts(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Vector[DecisionOptionRef] =
    NegotiationStartFacts.at(catalog, state, actor, restrictions,
      queuedPrivately(state, actor)).refs
```

Replace `noStartArgs` with:

```scala
  /** What the start arguments say: none for a menu Negotiation, the
    * private marker for a power's private one. */
  private def marked(args: Vector[DecisionOptionRef])
      : Either[OathViolation, Boolean] =
    if args.isEmpty then Right(false)
    else if args == privateArgs then Right(true)
    else Left(refusedArgs(args))

  private def refusedArgs(args: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} takes no start " +
        "selection but a power's private marker, got " +
        args.map(_.kind).mkString(", "))
```

`tree` takes `privately: Boolean` as its last parameter. Inside it:

- The negotiators question asks for exactly one player in private:

```scala
    val choose: Vector[Operation] =
      if candidates.size < 2 then Vector.empty
      else Vector(Decide(NegotiationDeal.negotiatorsDecisionId, actor,
        DecisionQuery.ChooseMany(1, if privately then 1 else candidates.size,
          candidates.map(id =>
            DecisionOption.Player(DecisionOptionRef.Player(id))),
          heading = Some(if privately then
            "Choose who to negotiate with in private"
            else "Choose who to negotiate with"),
          confirm = Some(CountedConfirm("Negotiate with", "player",
            "players"))),
        window = Some(PowerWindow.NegotiationEligibility)))
```

- The snapshot's rules carry the privacy:
  `facts.rules(deal.participants).copy(privateDeal = privately)`.
- The opening note names the key by the privacy:

```scala
            else _ => Some((if privately then negotiatedPrivately
              else negotiated).sourceless(NoteArg.Players(
              NegotiationDeal.chosen(candidates, pending))))))
```

- The promise lines are read only by the participants in private:

```scala
        // Each promise's line, last: the deal's pieces have all moved and
        // its roles changed by now. The engine enforces none of them. In
        // private, only the participants read them (ruling 23); every
        // other line of the settlement is public (rulings 23 and 30).
        val promises: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else promisedLines(deal).map(note =>
            val read = if privately then note.readBy(deal.participants)
              else note
            Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(read)))
```

Add to the object's doc comment, after its tree: "Whispering Stone
queues a private one: its start argument is the private marker, its
candidates are every other player, its negotiators question takes one,
and its promise lines are read only by the participants."

- [ ] **Step 7: Delete the unsupported-rule test**

In `NegotiationProcedureSuite`, delete the test "an unsupported
Negotiation rule does not block the Negotiation" and the
`WhisperingStoneCard` import slice 4 gave it (finding 9).

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.WhisperingStoneSuite distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.walker.WalkerProcedureRegistrySuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.PowerKindsCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite distantlands.gameplay.cards.CardCatalogSuite"`
Expected: PASS. If a catalog suite names Whispering Stone among the
holding cards or the unimplemented ones, drop it there.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/action/WhisperingStone.scala src/main/scala/distantlands/catalog/holding/HoldingRelics.scala src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala src/main/scala/distantlands/model/Decisions.scala src/test/resources/powers/unimplemented.txt src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala src/test/scala/distantlands/gameplay/powers/action/WhisperingStoneSuite.scala src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): negotiate in private through Whispering Stone

Whispering Stone queues a Negotiation carrying the private marker. That
Negotiation offers every other player, takes one, marks the deal private
and lets only its participants read its promises. Catalog batch 12,
rulings 21 to 23.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: An outsider's view of a private deal

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/NegotiationDealProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`

**Interfaces:**
- Consumes: Task 2's `DecisionQuery.Negotiate.privateDeal`,
  `WhisperingStone` and `WhisperingStoneCard`.
- Produces: `NegotiationDealProjection.privateDeal: Boolean = false`,
  last, on the wire as the required key `"privateDeal"`.

- [ ] **Step 1: Write the failing projection tests**

In `NegotiationDealProjectionSuite`, add the imports
`distantlands.gameplay.powers.action.{WhisperingStone, WhisperingStoneCard}`
and `distantlands.testkit.ActPowerHarness`, and append:

```scala
  /** p1's private Negotiation with p3 through Whispering Stone, with p1's
    * offer of 2 favor and a promise proposed. p2 stands outside it. */
  private def whisperedDeal: OathState =
    val stoneRules = ActPowerHarness.rules()
    val table = gathered.relic(p1, WhisperingStoneCard.id)
      .secrets(p1, faceUp = 1)
    val used = ActPowerHarness.use(stoneRules, table.ready,
      WhisperingStone.id, DecisionOptionRef.Relic(WhisperingStoneCard.id))
      .getOrElse(fail("the Stone must start"))
    val chosen = stoneRules.resolveWalker(used.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
        DecisionOptionRef.Player(p3)))).getOrElse(fail("negotiators"))
    stoneRules.resolveWalker(chosen.state, p1, dealId, ProposeTerms(
      NegotiationTerms(Vector(NegotiationTransfer(p3, 2, Vector.empty)),
        promises = Vector("I will not attack you this round."))))
      .getOrElse(fail("terms")).state

  test("a player outside a private deal sees who negotiates and who has " +
      "accepted, never the terms"):
    val state = whisperedDeal
    Vector(deal(view(state, p2)),
        deal(projector.projectPublic("negotiation", LoadedGame(state, 30))))
      .foreach { seen =>
        assertEquals(seen.participantPlayerIds, Vector(p1.value, p3.value))
        assertEquals(seen.acceptedPlayerIds, Vector.empty)
        assertEquals(seen.transfers, Vector.empty)
        assertEquals(seen.disclosures, Vector.empty)
        assertEquals(seen.citizenship, Vector.empty)
        assertEquals(seen.promises, Vector.empty)
        assertEquals(seen.editing, None)
        assert(seen.privateDeal)
      }

  test("a participant in a private deal sees its terms"):
    val seen = deal(view(whisperedDeal, p3))
    assertEquals(seen.transfers.map(_.favor), Vector(2))
    assertEquals(seen.promises.map(_.text),
      Vector("I will not attack you this round."))
    assert(seen.privateDeal)
    assert(seen.editing.nonEmpty)
```

In `ProjectionProtocolSuite`, find the round trip built on
`NegotiationDealProjection(Vector("red", "blue"), ...)` and add
`privateDeal = true` to that value, so the key's round trip is checked.

In `NegotiationDealPanelSuite`, append:

```scala
  test("a player outside a private deal reads who negotiates, not the " +
      "terms"):
    val hidden = NegotiationDealState(
      participantPlayerIds = Vector("red", "blue"),
      acceptedPlayerIds = Vector("blue"), transfers = Vector.empty,
      disclosures = Vector.empty, privateDeal = true)
    val panel = render(spectator(hidden), new RecordingControls(),
      canControl = false, playerId = "white")
    assertEquals(Dom.one(panel, ".negotiation-private").textContent,
      "red and blue are negotiating in private.")
    assertEquals(Dom.one(panel, ".negotiation-status").textContent,
      "red: reviewing · blue: accepted")
    assertEquals(Dom.all(panel, ".negotiation-transfer").length, 0)

  test("a participant in a private deal is not told it is outside"):
    val panel = render(owner(dealWhereBlueHasAccepted().copy(
      privateDeal = true)), new RecordingControls())
    assertEquals(Dom.all(panel, ".negotiation-private").length, 0)
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: compile failure, `privateDeal` is not a member of
`NegotiationDealProjection`.

- [ ] **Step 3: The projection field and its key**

In `ActionProjectionDtos.scala`, `NegotiationDealProjection` gains a last
field:

```scala
    /** The deal is private (Whispering Stone): a viewer outside it gets
      * no terms. True for every viewer of such a deal. */
    privateDeal: Boolean = false
```

In `ActionProjectionCodec.scala`:

- `encodeDeal` gains `"privateDeal" -> value.privateDeal` as its last
  entry.
- `decodeDeal` adds `"privateDeal"` to the top-level `exact` key set,
  reads `privateDeal <- bool(value, "privateDeal", path)` after the
  promises, and yields it as the last argument
  (`..., promises, privateDeal = privateDeal)`).

- [ ] **Step 4: Withhold the terms from an outsider**

In `NegotiationDealProjector.project`, before `transfers`:

```scala
    // Outside a private deal a viewer sees who negotiates and who has
    // accepted, never the terms (catalog batch 12, ruling 23).
    val authors =
      if query.privateDeal && !viewer.exists(query.participants.contains)
      then Vector.empty[PlayerId]
      else query.participants
```

Then `transfers`, `disclosures`, `promises` and the Citizenship rows
iterate `authors` where they iterate `query.participants` today. The
projection's own participants and accepted marks keep
`query.participants`. The constructor call gains
`privateDeal = query.privateDeal` as its last argument. The class doc
comment gains: "In a private deal, a viewer outside it gets the
participants and the accepted marks only."

- [ ] **Step 5: The panel's outsider line**

In `NegotiationDealPanel.scala`, `render` calls
`summary(surface.deal, playerId, panel)`, and `summary` becomes:

```scala
  private def summary(deal: NegotiationDealState, playerId: String,
      panel: dom.Element): Unit =
    panel.appendChild(text("h2", "", "Negotiation"))
    panel.appendChild(text("p", "negotiation-status",
      deal.participantPlayerIds.map(id => s"$id: ${if deal.acceptedPlayerIds
        .contains(id) then "accepted" else "reviewing"}").mkString(" · ")))
    if deal.privateDeal && !deal.participantPlayerIds.contains(playerId) then
      panel.appendChild(text("p", "negotiation-private",
        s"${deal.participantPlayerIds.mkString(" and ")} are negotiating " +
          "in private."))
```

followed by its existing transfer, disclosure, Citizenship and promise
lines, unchanged. Add to the object's doc comment: "A viewer outside a
private deal reads only who negotiates and who has accepted."

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite distantlands.application.WalkerDecisionProjectorSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite distantlands.frontend.ParkedDecisionSuite"`
Expected: PASS. If a protocol fixture spells a deal's JSON in full,
add `"privateDeal":false` to it; find such fixtures with
`git grep -n '"acceptedPlayerIds"'`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala src/main/scala/distantlands/application/NegotiationDealProjector.scala frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): keep a private deal's terms from outsiders

A viewer outside a private deal is projected its participants and
accepted marks only, and the panel tells them the two are negotiating in
private. Catalog batch 12, ruling 23.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add any fixture file Step 6 changed to the `git add` line.

---

### Task 4: Game Log, docs, verification

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`
- Create: `src/test/resources/gamelog/negotiation-whispered.actor.log`, `negotiation-whispered.other.log`
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md`

**Interfaces:**
- Consumes: Tasks 1 to 3.
- Produces: the log script `negotiationWhispered`.

- [ ] **Step 1: The script**

In `NegotiationLogScripts.scala`, add the imports
`distantlands.gameplay.powers.action.{WhisperingStone, WhisperingStoneCard}`,
`distantlands.testkit.Table.p3` (beside `p1, p2`) and
`DecisionAnswer.ChooseManyAnswer` (beside `AcceptDeal`), name the new
script in the object's doc comment ("a private deal through Whispering
Stone"), and add:

```scala
  /** p1 uses Whispering Stone to negotiate in private with p3, whose pawn
    * stands at Buried Giant, away from p1's. p1 gives p3 1 favor and makes
    * a promise. p2, outside the deal and the script's other seat, reads
    * the opening line and the transfer, never the promise (catalog batch
    * 12, rulings 21 to 23). */
  def negotiationWhispered(using munit.Location): Script =
    val stone = WhisperingStoneCard.id
    val (service, act) = atTable("negotiation-whispered",
      tableWithOnePawnPerSite.relic(p1, stone).secrets(p1, faceUp = 1)
        .banditsRefilled)
    act
      .parkedAfter(MenuPlay.start(p1, ActionRef.UsePower(WhisperingStone.id),
        Vector(DecisionOptionRef.Relic(stone))))
      .parkedAfter(Commands.resolveWalker(p1, TreeDecision(
        NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
          DecisionOptionRef.Player(p3))))))
      .parkedAfter(deal(p1, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p3, 1, Vector.empty)),
        promises = Vector("I will not attack you this round.")))))
      .parkedAfter(deal(p3, AcceptDeal))
      .after(deal(p1, AcceptDeal))
    Script("negotiation-whispered", service, p1)
```

In `LogScripts.scala`, add `negotiationWhispered` to the
`NegotiationLogScripts` export, and
`"negotiation-whispered" -> (() => negotiationWhispered),` to the named
scripts after `"negotiation-binding-promised"`.

- [ ] **Step 2: Write the failing log tests**

Append to `GameLogExchangeSuite`:

```scala
  test("a private deal opens in private, and its transfer reads to all"):
    val script = negotiationWhispered
    val opened = s"Negotiated with ${playerLabel(p3)}, in private."
    val gave = s"${playerLabel(script.actor)} gave 1 favor to " +
      s"${playerLabel(p3)}"
    Vector(None, Some(p1), Some(p2), Some(p3)).foreach { viewer =>
      val all = texts(format(script, viewer).filter(_.depth == 1))
      assertEquals(all.count(_ == opened), 1, s"$viewer: $all")
      assertEquals(all.count(_ == gave), 1, s"$viewer: $all")
    }

  test("a private deal's promise reads only to its participants"):
    val script = negotiationWhispered
    val promise = s"${playerLabel(script.actor)} promised " +
      s"${playerLabel(p3)}: “I will not attack you this round.”"
    Vector(Some(p1) -> 1, Some(p3) -> 1, Some(p2) -> 0, None -> 0).foreach {
      (viewer, count) =>
        val all = texts(format(script, viewer))
        assertEquals(all.count(_ == promise), count, s"$viewer: $all")
    }
```

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: PASS, since Tasks 1 to 3 implement the lines. If either fails,
the failure names the lines each viewer read: fix the procedure, not the
test.

- [ ] **Step 3: The golden logs**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Review both new files line by line:

- `negotiation-whispered.actor.log` (p1) has a line naming
  `[card:R34|Whispering Stone]` in a `use-power:relic.whispering-stone`
  run, then in a `negotiation` run "Negotiated with [player:p3|p3], in
  private.", "[player:p1|p1] gave 1 favor to [player:p3|p3]" and the
  promise line.
- `negotiation-whispered.other.log` (p2) has the same lines with the same
  `(sequence, ordinal)` keys, less the promise line.

Then confirm `git status --short src/test/resources/gamelog` lists only
the two new files. A change to any other golden is a regression: revert
it and fix its cause.

- [ ] **Step 4: The docs**

In `docs/architecture/all-exile-negotiation.md`:

- In the opening paragraph, replace "and it takes no start selection"
  with "and it takes no start selection, except the private marker
  Whispering Stone's queued Negotiation carries".
- Add after the section "Settlement and disclosure":

```markdown
## Private deals

Whispering Stone pays 1 secret onto the relic and queues a Negotiation `Run`, sourced on the relic, whose one start argument is the private marker (`NegotiationProcedure.privateArgs`). The Negotiation accepts the marker only from such an entry (`queuedPrivately`). Its candidates are then every other player, its negotiators question takes exactly one, and `DealRules.privateDeal` marks the `Negotiate` query private. Every other rule of the deal holds unchanged, its When Negotiating powers and their blockers included.

A viewer outside a private deal is projected its participants and accepted marks only. The opening line reads "Negotiated with p3, in private." The settlement's lines are public, but each promise line is a `LogNote` read only by the participants (`LogNote.readers`). The formatter numbers every line before it drops one for a viewer outside its readers, so the other lines keep their keys.
```

- In "Visibility", add: "A private deal shows a viewer outside it only
  who negotiates and who has accepted (see Private deals)."
- In "Unsupported Negotiation rules", delete the sentence "Separate
  action powers such as Whispering Stone do not modify a base
  Negotiation merely by being held."
- In "Deferred", delete "Whispering Stone's remote, private
  Negotiation, " from the list.

In `docs/ROADMAP.md`, in the phase "Negotiation terms and promises":

- In its opening paragraph, delete "private Negotiation, " from the list
  of what to add.
- In the item **Negotiation cards**, replace "The Gathering and
  Whispering Stone need a nested Negotiation with its own participants."
  with "The Gathering needs a nested Negotiation with its own
  participants." Leave the rest of the item as slice 4 left it.

In the spec, set the status line to
`**Status:** designed 2026-11-09. Slices 1 to 5 landed.`

- [ ] **Step 5: Full verification**

Run: `./sbtw clean coverage test coverageReport`
Expected: every suite passes, the walker suites among them (the spec
asks slices 5 and 6 to run them in full), and statement coverage is at
or above 92%.

Run: `python3 scripts/check-architecture.py`
Expected: no violation. `NegotiationProcedure.scala` stays under 800
lines.

- [ ] **Step 6: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala src/test/scala/distantlands/application/gamelog/LogScripts.scala src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala src/test/resources/gamelog/negotiation-whispered.actor.log src/test/resources/gamelog/negotiation-whispered.other.log docs/architecture/all-exile-negotiation.md docs/ROADMAP.md docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
test(gamelog): log a private deal through Whispering Stone

A golden log for a deal p1 makes in private with p3: p2 reads the
opening and the transfer, not the promise. Docs close catalog batch 12
slice 5.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage.** The action and its cost (ruling 21): Task 2's power
  and its cost test. Any one player, wherever their pawn: Task 2's
  question test and start-facts test. A full Negotiation (ruling 22):
  the queued walk is the Negotiate action's, and Task 2 tests Council
  Arbiter widening it. Privacy (ruling 23): Task 3's projection and
  panel, Task 1's readers, Task 2's promise readers, Task 4's log tests
  and goldens. Ruling 30: Task 2 sets readers on the promise lines only,
  so the Citizenship offer line stays public. The marker refused from the
  menu: Task 2. Catalog: Task 2 moves the card and drops it from
  `unimplemented.txt`. The spec's test list: all covered, with the
  outsider's settlement lines in Task 4.
- **Placeholders.** None. The test helpers used (`ActPowerHarness`,
  `ParkedNode`, `ParkedDecisionAssertions`, `Look.tokensOn`,
  `Table.secrets`, `LogEntry.sequence: Long`) were checked at plan time.
- **Names.** `privateArgs`, `queuedPrivately`, `negotiatedPrivately`,
  `NegotiationStartFacts.at(..., privately)`, `DealRules.privateDeal`,
  `DecisionQuery.Negotiate.privateDeal`,
  `NegotiationDealProjection.privateDeal`, `LogNote.readBy`,
  `Posted.readers` and `formatEveryLine` are spelled the same in every
  task.
