# Catalog Batch 10 Slice 2: Round Advance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `AdvanceRound` primitive and implement Great Clock (E12 intact) and Broken Clock (E12 ruined) on it.

**Architecture:**
- `AdvanceRound` is a `PrimitiveOperation` modelled on `AdvanceVisionsDrawn`. It moves `tracks.round` forward one space and takes the Usurper Limiter off when the new round is past 3. Applying it on round 8 is an error.
- `RoundMarker`, beside the card in the setup package, holds what both faces share: the advance "if possible", and the round a step reached, for the line.
- Great Clock is a `PaidAction` whose `refusal` disables it on round 8. Broken Clock is a `RuinedSetupRule`, like Ruined School.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read "Goal and scope", "Rulings" (1), "Readings" (Fallen Spire and Broken Clock), "Slice 2: Round advance", "Testing" and "Out of scope". Card text: `~/projects/cards/content`.

This is the second of 14 slice plans, written against `0.2.0-alpha` at `f28e3f48`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-2` on branch `batch-10-slice-2`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules. `model/CoreOperations.scala` is at 779 lines; Task 1 adds about 8.
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write` (`src/test/resources/notes/declared-keys.txt`) and `UNIMPLEMENTED_SNAPSHOT=write` (`src/test/resources/powers/unimplemented.txt`), then read the diff. Commit only the lines the task explains.
- **Moving a card out of holding:** delete its object from `src/main/scala/oathdigital/catalog/holding/HoldingEdifices.scala` and define it, unchanged, beside its power. `gameplay/cards/NewFoundations.scala` imports every `gameplay.powers.*` package by wildcard, so it needs no edit.
- **Log lines:** a power's line names only non-actors (memory: "Log lines omit actor").
- **Hover reasons:** show an unavailable option disabled with a reason.
- **Wire:** one new operation kind, `advance-round`. Old journals need not decode.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").

---

### Task 1: `AdvanceRound`

**Files:**
- Modify: `src/main/scala/oathdigital/model/CoreOperations.scala:651-653` (add after `AdvanceVisionsDrawn`)
- Modify: `src/main/scala/oathdigital/gameplay/operations/TurnStateOperations.scala:190` (add before `usurperLimiterRemoved`)
- Modify: `src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala:194-195`
- Modify: `src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala:41, 236, 590`
- Modify: `src/test/scala/oathdigital/testkit/Table.scala:318` (add after `visionsDrawn`)
- Test: `src/test/scala/oathdigital/gameplay/OperationExecutorSuite.scala`
- Test: `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces: `case object AdvanceRound extends PrimitiveOperation` in `oathdigital.model`, with `required = true`.
- Produces: `Table.roundMarkerAt(n: Int): Table` and `Table.usurperLimiter(onTrack: Boolean): Table`.

- [ ] **Step 1: Add the Table helpers**

In `Table.scala`, after `visionsDrawn(n: Int)`:

```scala
  /** The round marker on round `n`; no other track moves. */
  def roundMarkerAt(n: Int): Table =
    update(_.updateCurrent(c => c.copy(tracks = c.tracks.copy(round = n))))

  /** The Usurper Limiter on the round track when `onTrack`, off it
    * otherwise. */
  def usurperLimiter(onTrack: Boolean): Table =
    update(_.updateCurrent(c => c.copy(tracks =
      c.tracks.copy(usurperLimited = onTrack))))
```

- [ ] **Step 2: Write the failing executor tests**

In `OperationExecutorSuite.scala`, after the test "Visions Drawn advances once and rejects integer overflow":

```scala
  test("the round marker advances once and is refused on its last space"):
    val second = Table.from(ready).roundMarkerAt(1).ready
    assertEquals(accepted(executor.execute(second, AdvanceRound))
      .game.current.tracks.round, 2)
    val last = Table.from(ready).roundMarkerAt(8).ready
    assertEquals(rejectionCode(last, AdvanceRound), "invalid-description")

  test("passing round 3 takes the Usurper Limiter off; earlier advances " +
      "leave it on"):
    def limitedAfter(round: Int): Boolean =
      val staged = Table.from(ready).roundMarkerAt(round)
        .usurperLimiter(onTrack = true).ready
      accepted(executor.execute(staged, AdvanceRound))
        .game.current.tracks.usurperLimited
    assert(limitedAfter(1))
    assert(limitedAfter(2))
    assert(!limitedAfter(3))
    val off = Table.from(ready).roundMarkerAt(5)
      .usurperLimiter(onTrack = false).ready
    assert(!accepted(executor.execute(off, AdvanceRound))
      .game.current.tracks.usurperLimited)
```

- [ ] **Step 3: Write the failing wire test**

In `WalkerOperationWireSuite.scala`, after the test "recorded Visions Drawn advancement round trips without an amount and replays as one more Vision drawn":

```scala
  test("a recorded round advance round trips and replays as one more round"):
    val event = recordedOperation(AdvanceRound)
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "advance-round"))
    val decoded = as[WalkerStepRecorded](
      accepted(GameEventWire.decode(encoded)).event)
    val base = Table.start.roundMarkerAt(4).ready
    val replayed = accepted(OperationPipeline.run(base, RecordedOps.of(decoded),
      OperationPolicy.Permissive, Vector.empty)(Right(_))).state
    assertEquals(replayed.game.current.tracks.round, 5)
```

- [ ] **Step 4: Run the tests to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.OperationExecutorSuite oathdigital.serialization.WalkerOperationWireSuite"`
Expected: compile error, `Not found: AdvanceRound`.

- [ ] **Step 5: Add the operation**

In `CoreOperations.scala`, after `AdvanceVisionsDrawn`:

```scala
/** Moves the round marker one space forward, as Great Clock and Broken
  * Clock do. Passing round 3 takes the Usurper Limiter off, as the end of
  * round 3 does (catalog batch 10, ruling 1). It rolls no end die; applying
  * it on round 8 is an error. */
case object AdvanceRound extends PrimitiveOperation:
  override val required: Boolean = true
```

In `TurnStateOperations.scala`, before `usurperLimiterRemoved`:

```scala
  def advanceRound(ready: ReadyGame): Either[OperationError, ReadyGame] =
    val tracks = ready.game.current.tracks
    val next = tracks.round + 1
    Either.cond(tracks.round < 8,
      ready.updateCurrent(current => current.copy(tracks = tracks.copy(
        round = next, usurperLimited = tracks.usurperLimited && next <= 3))),
      InvalidDescription("the round marker is on its last space"))
```

`Either.cond` takes both branches by name, so `tracks.copy(round = 9)` and its `require` never run on round 8.

In `OperationApplication.scala`, `applyNonMoveLeaves`, after the `AdvanceVisionsDrawn` case:

```scala
      case (result, AdvanceRound) =>
        result.flatMap(TurnStateOperations.advanceRound)
```

- [ ] **Step 6: Add the codec entry**

In `WalkerOperationCodec.scala`:
- Line 41: `private type TurnOperation = AdvanceVisionsDrawn.type | AdvanceRound.type |`
- After the encode case `case AdvanceVisionsDrawn => ujson.Obj("kind" -> "advance-visions-drawn")`:
  ```scala
      case AdvanceRound => ujson.Obj("kind" -> "advance-round")
  ```
- After the decode case `case "advance-visions-drawn" => Right(AdvanceVisionsDrawn)`:
  ```scala
    case "advance-round" => Right(AdvanceRound)
  ```

- [ ] **Step 7: Run the tests to see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.OperationExecutorSuite oathdigital.serialization.WalkerOperationWireSuite"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/model/CoreOperations.scala \
  src/main/scala/oathdigital/gameplay/operations/TurnStateOperations.scala \
  src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala \
  src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala \
  src/test/scala/oathdigital/testkit/Table.scala \
  src/test/scala/oathdigital/gameplay/OperationExecutorSuite.scala \
  src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(operations): AdvanceRound moves the round marker one space

Passing round 3 takes the Usurper Limiter off, as the end of round 3
does. Round 8 refuses the advance.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Great Clock and Broken Clock

**Files:**
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingEdifices.scala:86-99` (delete `GreatClockCard`)
- Modify: `src/main/scala/oathdigital/gameplay/powers/setup/RuinedSiteSetupRules.scala` (add `GreatClockCard`, `RoundMarker`, `BrokenClock`)
- Create: `src/main/scala/oathdigital/gameplay/powers/action/GreatClock.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/OtherActionPowers.scala:19`
- Modify: `src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala:116`
- Modify: `src/test/scala/oathdigital/gameplay/powers/campaign/SaltTheEarthSuite.scala:3` (import)
- Create: `src/test/scala/oathdigital/gameplay/powers/action/GreatClockSuite.scala`
- Modify: `src/test/scala/oathdigital/gameplay/powers/setup/RuinedSetupRulesSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt`
- Modify: `docs/ROADMAP.md:69-70`, `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md:3`

**Interfaces:**
- Consumes: `AdvanceRound` and `Table.roundMarkerAt`, `Table.usurperLimiter` from Task 1.
- Produces: `GreatClockCard` in `oathdigital.gameplay.powers.setup`; `RoundMarker.advance(ready): Vector[CoreOperation]`, `RoundMarker.reached(states: NoteStates): Option[Int]`; `case object GreatClock extends PaidAction` with `val LastSpace: String`; `case object BrokenClock extends RuinedSetupRule`.

- [ ] **Step 1: Move the card**

Delete `object GreatClockCard` (lines 86 to 99) from `HoldingEdifices.scala`. Paste it unchanged into `RuinedSiteSetupRules.scala`, after `ForestHornCard`.

In `SaltTheEarthSuite.scala`, line 3 becomes:

```scala
import oathdigital.catalog.holding.SearchPartyCard
import oathdigital.gameplay.powers.setup.GreatClockCard
```

- [ ] **Step 2: Write the failing Broken Clock tests**

In `RuinedSetupRulesSuite.scala`, add a section at the end of the class:

```scala
  // ---- Broken Clock ----

  test("Broken Clock advances the round marker once at Setup's end"):
    val staged = stagedWith(GreatClockCard.id).ready
    val round = staged.game.current.tracks.round
    val walked = finish(staged)
    assertEquals(walked.ready.game.current.tracks.round, round + 1)
    assertEquals(said(BrokenClock, walked.events), Vector(NoteText.Said(
      "advanced", s"Advanced the round marker to round ${round + 1}.",
      covers = false)))

  test("the intact Great Clock moves no marker at Setup's end"):
    val staged = stagedWith(GreatClockCard.id, EdificeSide.Intact).ready
    val walked = finish(staged)
    assertEquals(walked.ready.game.current.tracks.round,
      staged.game.current.tracks.round)
    assertEquals(said(BrokenClock, walked.events), Vector.empty)
```

- [ ] **Step 3: Write the failing Great Clock suite**

Create `src/test/scala/oathdigital/gameplay/powers/action/GreatClockSuite.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog, PowerFixture}
import oathdigital.gameplay.powers.setup.GreatClockCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.model.OathEvent.{EndDieRolled, RoundEnded}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Great Clock (catalog batch 10, slice 2): advance the round marker once,
  * if possible, without the end die. */
class GreatClockSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{backToActing, replayed, rules}

  private val clock = GreatClockCard.id
  private val source = DecisionOptionRef.Edifice(clock)
  private val home = AncientCitySite.id

  /** p1's Act at Ancient City beside the intact Great Clock, with 1 favor
    * and 3 faceup secrets to pay its cost, the round marker on `round` and
    * the Usurper Limiter on the track. */
  private def clockAtRound(round: Int): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(GreatClockCard, EdificeSide.Intact, at = home)
      .favor(actor, 1).secrets(actor, 3)
      .roundMarkerAt(round).usurperLimiter(onTrack = true)

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)), ActionRef.UsePower(GreatClock.id),
    actor, Vector(source))

  /** The Clock's option on the actor's Act menu, or `None` when the menu
    * leaves it out. */
  private def clockOnMenu(ready: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ready, actor,
      PhasePowerCatalog.default, WalkerPowers.empty)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)

  test("it pays its cost and advances the marker from round 2 to 3, " +
      "leaving the Usurper Limiter on"):
    val start = clockAtRound(2).ready
    val done = accepted(use(start))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(after.game.current.tracks.round, 3)
    assert(after.game.current.tracks.usurperLimited)
    assertEquals(Look(after).tokensOn(clock), Tokens(1, 2))
    assertEquals(Look(after).favor(actor), 0)
    assertEquals(NoteText.said(GreatClock, done.events), Vector(NoteText.Said(
      NoteKey.Used, "Advanced the round marker to round 3.", covers = false)))
    assertEquals(replayed(start, done.events), Right(done.state))
    assertWireRoundTrips(done.events)

  test("advancing from round 3 to 4 takes the Usurper Limiter off"):
    val after = ready(accepted(use(clockAtRound(3).ready)).state)
    assertEquals(after.game.current.tracks.round, 4)
    assert(!after.game.current.tracks.usurperLimited)

  test("a jump past round 5 under an Imperial Oathkeeper rolls no end die " +
      "and ends no round"):
    val start = clockAtRound(5).usurperLimiter(onTrack = false)
      .imperial(actor, Table.p2).oathkeeper(Some(actor), TitleSide.Oathkeeper)
      .ready
    val done = accepted(use(start))
    assertEquals(ready(done.state).game.current.tracks.round, 6)
    assertEquals(ready(done.state).game.current.result, None)
    assert(!done.events.exists {
      case _: EndDieRolled | _: RoundEnded => true
      case _ => false
    })

  test("on round 8 the Clock is shown unavailable with the reason"):
    val last = clockAtRound(8).ready
    assertEquals(clockOnMenu(last).collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(GreatClock.LastSpace)))
```

- [ ] **Step 4: Run the tests to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GreatClockSuite oathdigital.gameplay.powers.setup.RuinedSetupRulesSuite"`
Expected: compile error, `Not found: GreatClock` and `Not found: BrokenClock`.

- [ ] **Step 5: Add `RoundMarker` and Broken Clock**

In `RuinedSiteSetupRules.scala`, after `GreatClockCard`:

```scala
/** The round marker's advance that both Great Clock faces make "if
  * possible" (catalog batch 10, slice 2): none on round 8, the last space.
  */
object RoundMarker:
  val LastRound: Int = 8

  /** The advance, or none on the last space. */
  def advance(ready: ReadyGame): Vector[CoreOperation] =
    Option.when(ready.game.current.tracks.round < LastRound)(AdvanceRound)
      .toVector

  /** The round the step before a line reached, when it moved the marker. */
  def reached(states: NoteStates): Option[Int] = for
    (before, after) <- states.previous
    round = after.game.current.tracks.round
    if round > before.game.current.tracks.round
  yield round
```

After `BrokenHorn`, at the end of the file:

```scala
/** Broken Clock (E12, ruined): "SETUP (END): Advance the round marker
  * once, if possible. Do not roll the round die. This cannot end the game."
  *
  * The advance is [[RoundMarker]]'s. At Setup the marker is on round 1, so
  * it always moves. Its line reads the step: "Advanced the round marker to
  * round 2." The WHEN EXPLORED half waits for an Explore procedure. Great
  * Clock, the intact face, is an action (catalog batch 10, slice 2).
  */
case object BrokenClock extends RuinedSetupRule:
  val edifice: EdificeId = GreatClockCard.id
  val id: PowerId = GreatClockCard.ruined.power.id
  private val notes = NoteSet(id)
  /** "Advanced the round marker to round {2}." */
  val advanced: NoteKey = notes.key("advanced", Vector(
    NotePart.Text("Advanced the round marker to round "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    RoundMarker.advance(ready)

  protected def note(at: SiteId): Note = Note(id, states =>
    RoundMarker.reached(states).map(round =>
      advanced(PowerSourceRef.Card(edifice), NoteArg.Number(round))))
```

In `WalkerPowerCatalog.scala:116`, register it beside the others: `RuinedSchool, BrokenHorn, BrokenClock, TakeWealthLimit,`. Keep the line under 80 columns; wrap if needed.

- [ ] **Step 6: Add Great Clock**

Create `src/main/scala/oathdigital/gameplay/powers/action/GreatClock.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.QueuedStarts
import oathdigital.gameplay.powers.setup.{GreatClockCard, RoundMarker}
import oathdigital.model._

/** Great Clock (E12, intact), ACTION: advance the round marker once, if
  * possible. "Do not roll the round die. This cannot end the game."
  * (catalog batch 10, slice 2).
  *
  * The advance is [[RoundMarker]]'s, which takes the Usurper Limiter off
  * past round 3 (ruling 1). On round 8 the option shows unavailable with
  * [[LastSpace]]. The end die is rolled only at a round's end, from the
  * round then, so a jump past rounds 5 to 7 rolls none. Its line: "Advanced
  * the round marker to round 4."
  */
case object GreatClock extends PaidAction(GreatClockCard.intact.power):
  val LastSpace: String = "The round marker is on its last space."

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Advanced the round marker to round {4}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Advanced the round marker to round "), NotePart.Arg(0),
    NotePart.Text(".")))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(RoundMarker.advance(ready).isEmpty)(LastSpace)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      BuildOps((live, _) => Right(RoundMarker.advance(live))),
      Note(id, states => RoundMarker.reached(states).map(round =>
        used(PowerSourceRef.Card(GreatClockCard.id),
          NoteArg.Number(round)))))))
```

In `OtherActionPowers.scala:19`: `ForestHorn, GiantsRampage, GreatClock,`.

- [ ] **Step 7: Run the tests to see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GreatClockSuite oathdigital.gameplay.powers.setup.RuinedSetupRulesSuite oathdigital.gameplay.powers.campaign.SaltTheEarthSuite"`
Expected: PASS.

- [ ] **Step 8: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw test`
Then `git diff src/test/resources`. Expected:
- `declared-keys.txt` gains `edifice.e12.intact` key `used` and `edifice.e12.ruined` key `advanced`.
- `unimplemented.txt` loses `edifice.e12.intact` and `edifice.e12.ruined`, and nothing else.

- [ ] **Step 9: Update the docs**

In `docs/ROADMAP.md`, lines 69 and 70 become:

```markdown
- **Round die:** Shattered Portal.
```

In the spec, line 3 becomes `**Status:** designed 2026-11-03. Slices 1 and 2 landed.`

- [ ] **Step 10: Run the gates**

Run:
- `./sbtw clean coverage test coverageReport` (statement coverage at least 92%)
- `python3 scripts/check-architecture.py`
- `python3 scripts/check-markdown-links.py`

Expected: all pass.

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/oathdigital/catalog/holding/HoldingEdifices.scala \
  src/main/scala/oathdigital/gameplay/powers/setup/RuinedSiteSetupRules.scala \
  src/main/scala/oathdigital/gameplay/powers/action/GreatClock.scala \
  src/main/scala/oathdigital/gameplay/powers/action/OtherActionPowers.scala \
  src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/oathdigital/gameplay/powers/campaign/SaltTheEarthSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/action/GreatClockSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/setup/RuinedSetupRulesSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt \
  docs/ROADMAP.md docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md
git commit -m "$(cat <<'EOF'
feat(powers): Great Clock and Broken Clock advance the round marker

Great Clock is an action, unavailable on round 8. Broken Clock advances
the marker at Setup's end; its WHEN EXPLORED half waits for Explore.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
