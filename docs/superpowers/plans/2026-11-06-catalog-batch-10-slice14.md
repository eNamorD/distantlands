# Catalog Batch 10, Slice 14: Memory Across Turns Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Tracker (221): an ACTION that records an enemy pawn
at the user's site and ends the Act phase, and an optional WAKE power that
places the user's pawn at that pawn's site on their next turn.

**Architecture:** `CurrentGameState` gains `trackedPawns`, keyed by the
Tracker's holder, written by two new primitives, `TrackPawn` and
`ClearTrackedPawn`, with their codec entries. Replay needs nothing new,
since replay applies recorded primitives. Tracker's single power id splits
into an ACTION and a WAKE `PhasePower`. End Wake clears the holder's
record as their Act phase begins. The projection hangs the record on the
Tracker card, and the frontend draws it as a coloured pawn marker.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`,
Scala.js frontend.

**Spec:** [Catalog batch 10 design](../specs/2026-11-03-catalog-batch-10-design.md):
read "Slice 14: Memory across turns", ruling 6 and the "Tracker" reading.

**Card text** (from `~/projects/cards`; Tracker has no FAQ):

- Tracker: "**ACTION:** Choose an enemy pawn at your site and end your Act
  Phase. **WAKE:** If you chose a pawn last turn, you may place your pawn
  at their site."

## Plan-time findings

1. **No state snapshot codec exists.** The game state is rebuilt by
   replaying the journal, so a new `CurrentGameState` field needs no codec
   of its own. Only the two primitives need codec entries. The newest
   field, `warbandsOnCards`, took the same path.
2. **The primitives must be in `CoreOperations.scala`.** `PrimitiveOperation`
   is sealed, so its cases cannot move to another file. The file has 779
   lines, and the two cases add about 9, which keeps it under the
   800-line cap. Keep their doc comments short.
3. **`OperationApplication` falls through quietly.** Its fold ends with
   `case (result, _) => result`, so a primitive missing there compiles but
   does nothing. The codec's `TurnOperation` union is exhaustive and fails
   to compile without the new cases.
4. **End Wake is the only way into the Act phase.** Only
   `EndWakeProcedure` emits `EnterPhase(Phase.Act)`. Its build adds
   `ClearTrackedPawn(actor)` after the phase change, but only when the
   actor holds a record. Its log is `Silent`, so the clear writes no line.
   A record made in Act therefore lasts through the Rest and the next
   Wake, and ends when the next Act begins, whether it was used or not.
5. **"Enemy" means `Imperials.enemies`:** two different players, unless
   both are Imperial. A Citizen cannot track the Chancellor.
6. **A refusal reason is a plain string.** `PhasePower.refusal` returns
   `Option[String]`, which the menu shows as `OptionReason.Written`, so a
   refusal cannot name a player in their colour. The spec says the Wake
   power's hover names the tracked player. Instead, the Tracker card
   carries the record ("Tracking Red", finding 9), and the reasons stay
   plain:
   - "No pawn was chosen last turn"
   - "Already at the chosen pawn's site"

   The ACTION's reason is the existing `PawnMoves.noValidEnemyPawn`.
   Reasons in this codebase carry no closing period.
7. **One line per power.** The ACTION writes "Tracked {Red}.", under the
   key `used`. Wizard School also writes "{p1}'s Act phase ended.", which
   names the actor, so Tracker writes no such line. Log lines name no
   actor, and the phase change already shows in the lines around it. The
   WAKE line is Passages' line, "{Blue} placed at {site}.", through
   `PawnMoves.placedKey`, so the two variants read the same.
8. **The Wake power is optional because the default is.**
   `PhasePower.forced` is `false` by default. A Wake menu with an
   unavailable Tracker still auto-ends (`PhaseMenu.onlyEnding`), and a
   usable one waits for the player.
9. **The record shows on the card.** `CardDetailsProjection` gains
   `tracking: Option[TrackedPawnProjection]`, holding the player id, their
   colour and the label "Tracking Red". The projector sets it on the
   holder's Tracker adviser, and every viewer sees it, since the choice
   was made openly. `CardFace` draws a "●" in the player's `force-` colour
   in the token row, with the label as its hover tip and accessible name.
   Obsidian Cage's warbands use the same row.
10. **Losing Tracker makes the record useless.** Phase-power access
    already requires the source to be the user's faceup adviser, so a
    record whose holder lost the card, or turned it facedown, finds no
    source. The record stays until the holder's next Act.
11. **New note keys** (snapshot):
    - `denizen.tracker.action used = Tracked {0}.`
    - `denizen.tracker.wake used = {0} placed at {1}.`

## Global Constraints

- **Slice 13 is independent.** Slice 13 (`batch-10-slice-13`, plan
  8ab5ba61) shares only the spec's status line, `declared-keys.txt` and the
  ROADMAP's engine-blocked list, on different lines. Whichever slice
  merges second rebases onto `0.2.0-alpha` first, keeps both sides of
  `declared-keys.txt` sorted, and writes the status "Slices 1 to 14
  landed."
- Work only in this worktree (`.worktrees/batch-10-slice-14`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import or a
  non-exhaustive match fails it.
- Log lines name no actor (they omit "you").
- Test helper names say what they build.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.

---

### Task 1: The tracked-pawn record and its primitives

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala` (end of
  `CurrentGameState`'s fields)
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (after
  `ActAsIf`, line 741)
- Modify: `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Modify: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/operations/TrackedPawnOperationsSuite.scala`

**Interfaces:**
- Produces: `CurrentGameState.trackedPawns: Map[PlayerId, PlayerId]`
  (holder to target, default empty); `TrackPawn(holder: PlayerId, target:
  PlayerId)` and `ClearTrackedPawn(holder: PlayerId)`, both
  `PrimitiveOperation`s; `Table.trackedPawn(holder: PlayerId, target:
  PlayerId): Table`.

- [ ] **Step 1: Write the failing primitive tests**

Create `src/test/scala/distantlands/gameplay/operations/TrackedPawnOperationsSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** The Tracker's record across turns (catalog batch 10, slice 14):
  * `TrackPawn` writes the holder's choice and `ClearTrackedPawn` removes
  * it. */
class TrackedPawnOperationsSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  test("tracking a pawn records it under its holder, replacing any older " +
      "choice"):
    val first = accepted(run(Table.start.ready, TrackPawn(p1, p2)))
    assertEquals(first.game.current.trackedPawns, Map(p1 -> p2))
    val again = accepted(run(first, TrackPawn(p1, p3), TrackPawn(p2, p1)))
    assertEquals(again.game.current.trackedPawns, Map(p1 -> p3, p2 -> p1))

  test("a player cannot track their own pawn, or an unseated one"):
    rejected(run(Table.start.ready, TrackPawn(p1, p1)))
    rejected(run(Table.start.ready, TrackPawn(p1, PlayerId("nobody"))))
    rejected(run(Table.start.ready, TrackPawn(PlayerId("nobody"), p1)))

  test("clearing removes only the holder's record, and needs one"):
    val both = Table.start.trackedPawn(p1, p2).trackedPawn(p2, p1).ready
    val cleared = accepted(run(both, ClearTrackedPawn(p1)))
    assertEquals(cleared.game.current.trackedPawns, Map(p2 -> p1))
    rejected(run(cleared, ClearTrackedPawn(p1)))
```

In `src/test/scala/distantlands/testkit/Table.scala`, add after
`warbandsOnCard` (line 194 onward):

```scala
  /** `holder` chose `target`'s pawn with Tracker's ACTION last turn. */
  def trackedPawn(holder: PlayerId, target: PlayerId): Table =
    update(_.updateCurrent(current => current.copy(
      trackedPawns = current.trackedPawns.updated(holder, target))))
```

In `WalkerOperationWireSuite`'s test "every CoreOperation variant
round-trips through the walker codec" (line 391), add to the `operations`
vector, after `SuitedReserveFromBox(...)`:

```scala
      TrackPawn(PlayerId("red"), PlayerId("blue")),
      ClearTrackedPawn(PlayerId("red")),
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.TrackedPawnOperationsSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: compile failure, `Not found: TrackPawn` and `value trackedPawns
is not a member of CurrentGameState`.

- [ ] **Step 3: Add the field and the primitives**

In `GameState.scala`, after `warbandsOnCards: Map[CardId, Map[ForceKind,
Int]] = Map.empty`, add a comma and:

```scala
    // The pawn each Tracker holder chose with its ACTION, by holder
    // (catalog batch 10, slice 14). Not walker scratch: it outlives the
    // turn, and the holder's next End Wake clears it. Public, as the choice
    // was made openly.
    trackedPawns: Map[PlayerId, PlayerId] = Map.empty
```

In `CoreOperations.scala`, after `final case class ActAsIf(site: SiteId)
extends PrimitiveOperation`:

```scala

/** Records the pawn `holder` chose with Tracker's ACTION in
  * `CurrentGameState.trackedPawns` (catalog batch 10, slice 14). */
final case class TrackPawn(holder: PlayerId, target: PlayerId)
    extends PrimitiveOperation

/** Removes `holder`'s Tracker record as their Act phase begins. */
final case class ClearTrackedPawn(holder: PlayerId) extends PrimitiveOperation
```

In `TurnStateOperations.scala`, after `actAsIf` (line 128):

```scala

  /** Records `target` as the pawn `holder` tracks. Both are seated, and
    * no player tracks their own pawn. */
  def trackPawn(ready: ReadyGame, holder: PlayerId, target: PlayerId)
      : Either[OperationError, ReadyGame] =
    val seated = ready.game.current.players.map(_.player).toSet
    if !seated(holder) || !seated(target) then Left(InvalidDescription(
      s"${holder.value} and ${target.value} must both be seated to track"))
    else if holder == target then Left(InvalidDescription(
      s"${holder.value} cannot track their own pawn"))
    else Right(ready.updateCurrent(current => current.copy(
      trackedPawns = current.trackedPawns.updated(holder, target))))

  /** Removes `holder`'s record, which must exist. */
  def clearTrackedPawn(ready: ReadyGame, holder: PlayerId)
      : Either[OperationError, ReadyGame] =
    Either.cond(ready.game.current.trackedPawns.contains(holder),
      ready.updateCurrent(current => current.copy(
        trackedPawns = current.trackedPawns - holder)),
      InvalidDescription(s"${holder.value} tracks no pawn"))
```

In `OperationApplication.scala`, after the `ActAsIf(site)` case:

```scala
      case (result, TrackPawn(holder, target)) =>
        result.flatMap(TurnStateOperations.trackPawn(_, holder, target))
      case (result, ClearTrackedPawn(holder)) =>
        result.flatMap(TurnStateOperations.clearTrackedPawn(_, holder))
```

In `WalkerOperationCodec.scala`:

- Add `| TrackPawn | ClearTrackedPawn` to the `TurnOperation` union, after
  `ClosePick`.
- In `encodeTurnOperation`, after the `ActAsIf` case:

  ```scala
      case TrackPawn(holder, target) => ujson.Obj("kind" -> "track-pawn",
        "holder" -> holder.value, "target" -> target.value)
      case ClearTrackedPawn(holder) => ujson.Obj(
        "kind" -> "clear-tracked-pawn", "holder" -> holder.value)
  ```

- In the decoder, after `case "act-as-if" => ...`:

  ```scala
    case "track-pawn" => Right(TrackPawn(PlayerId(value("holder").str),
      PlayerId(value("target").str)))
    case "clear-tracked-pawn" =>
      Right(ClearTrackedPawn(PlayerId(value("holder").str)))
  ```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.TrackedPawnOperationsSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: PASS. Then `wc -l src/main/scala/distantlands/model/CoreOperations.scala`
prints at most 800.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala src/test/scala/distantlands/gameplay/operations/TrackedPawnOperationsSuite.scala
git commit -m "$(cat <<'EOF'
feat(model): remember a tracked pawn across turns

TrackPawn records the pawn a Tracker's holder chose, and ClearTrackedPawn
removes it. Both are journalled, so replay rebuilds the record.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 2: Tracker's two powers and the clear at Act

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/wake/Tracker.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala:15-21`
  (remove `TrackerCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/wake/EndWakeProcedure.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (remove
  `denizen.tracker`)
- Modify: `src/test/resources/notes/declared-keys.txt` (snapshot)
- Create: `src/test/scala/distantlands/gameplay/powers/wake/TrackerSuite.scala`

**Interfaces:**
- Consumes: Task 1's `TrackPawn`, `ClearTrackedPawn`, `trackedPawns`,
  `Table.trackedPawn`.
- Produces: `TrackerCard` (in `distantlands.gameplay.powers.wake`, with
  `action` and `wake` printed powers); `TrackerAction` (`decisionId:
  String`, `tracked: NoteKey`, `targets(ready, player): Vector[PlayerId]`);
  `TrackerWake` (`placed: NoteKey`, `noPawnChosen: String`, `alreadyThere:
  String`, `tracked(ready: ReadyGame, holder: PlayerId, card: CardId):
  Option[PlayerId]`).

- [ ] **Step 1: Write the failing power tests**

Create `src/test/scala/distantlands/gameplay/powers/wake/TrackerSuite.scala`:

```scala
package distantlands.gameplay.powers.wake

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.phases.PhasePowerProcedure
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  PhasePowerHarness, Refusals, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Tracker (card 221, catalog batch 10, slice 14): ACTION, choose an enemy
  * pawn at your site and end your Act phase; WAKE, if you chose a pawn last
  * turn, you may place your pawn at their site (ruling 6). */
class TrackerSuite extends munit.FunSuite:
  private val tracker = TrackerCard.id
  private val source = DecisionOptionRef.Denizen(tracker)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val rules = ActPowerHarness.rules()

  /** p1's Act at Ancient City holding Tracker faceup, with p2 there too. */
  private def actingWithP2AtTrackersSite: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, home).pawn(p2, home)
      .adviser(p1, tracker)

  /** p1's Wake at Ancient City holding Tracker faceup, having tracked p2,
    * who now stands at `p2At`. */
  private def wakingAfterTrackingP2(p2At: SiteId): Table =
    Table.start.turn(p1, Phase.Wake).pawn(p1, home).pawn(p2, p2At)
      .adviser(p1, tracker).trackedPawn(p1, p2)

  private def startPower(ready: ReadyGame, power: PowerId) =
    rules.startWalker(Ready(MenuPlay.withoutMenu(ready)),
      ActionRef.UsePower(power), p1, Vector(source))

  private def chooseTarget(state: OathState, target: PlayerId) =
    rules.resolveWalker(state, p1, TrackerAction.decisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(target)))

  private def endWake(state: OathState) = rules.startWalker(
    MenuPlay.withoutMenu(state), PhaseTransitionRef.EndWake, p1)

  private def menuOption(ready: ReadyGame, power: PowerId)(
      using munit.Location): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.UsePower(power),
      Vector(source))

  /** The written reason a menu option is unavailable for, if it is. */
  private def writtenReason(option: DecisionOption): Option[String] =
    option match
      case DecisionOption.Unavailable(_, OptionReason.Written(text)) =>
        Some(text)
      case _ => None

  // ---- ACTION ----

  test("the ACTION records the chosen enemy and ends the Act phase, and " +
      "the record outlives the turn"):
    val ready = actingWithP2AtTrackersSite.ready
    val parked = accepted(startPower(ready, TrackerAction.id))
    val done = accepted(chooseTarget(parked.state, p2))
    val events = parked.events ++ done.events
    val atRest = ActPowerHarness.replayed(rules, ready,
      ActPowerHarness.untilWalkEnds(events))
    assertEquals(Look(atRest).phase, Phase.Rest)
    assertEquals(atRest.game.current.trackedPawns, Map(p1 -> p2))
    // No REST power is usable, so the driver finishes Rest and passes the
    // turn. The record stays until p1's next Act.
    val end = Outcomes.ready(done.state)
    assertNotEquals(Look(end).active, p1)
    assertEquals(end.game.current.trackedPawns, Map(p1 -> p2))
    assertEquals(NoteText.said(TrackerAction, events), Vector(
      NoteText.Said(NoteKey.Used, s"Tracked ${p2.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, events), end)
    assertWireRoundTrips(events)

  test("the ACTION offers only enemies at the user's site"):
    val ready = actingWithP2AtTrackersSite.pawn(p3, peaks).ready
    val parked = accepted(startPower(ready, TrackerAction.id))
    assertEquals(chooseTarget(parked.state, p3).left.toOption,
      Some(Refusals.notOffered(TrackerAction.decisionId)))

  test("with no enemy pawn at the site, the ACTION is unavailable with its " +
      "reason"):
    val alone = Table.start.turn(p1, Phase.Act).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker).ready
    assertEquals(writtenReason(menuOption(alone, TrackerAction.id)),
      Some(PawnMoves.noValidEnemyPawn))
    // A Citizen is no enemy of the Chancellor.
    val allied = actingWithP2AtTrackersSite.imperial(p1, p2).ready
    assertEquals(writtenReason(menuOption(allied, TrackerAction.id)),
      Some(PawnMoves.noValidEnemyPawn))

  // ---- WAKE ----

  test("the WAKE power is offered, not forced, and places the pawn at the " +
      "tracked pawn's site now, with its line"):
    val ready = wakingAfterTrackingP2(peaks).ready
    assert(!TrackerWake.forced)
    assert(menuOption(ready, TrackerWake.id).available)
    val done = accepted(startPower(ready, TrackerWake.id))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).pawn(p1), peaks)
    assertEquals(after.game.current.trackedPawns, Map(p1 -> p2))
    assertEquals(NoteText.said(TrackerWake, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} placed at ${peaks.value}.",
        covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), after)
    assertWireRoundTrips(done.events)

  test("the WAKE power is unavailable with no record, or with the pawn " +
      "already there"):
    val unrecorded = Table.start.turn(p1, Phase.Wake).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker).ready
    assertEquals(writtenReason(menuOption(unrecorded, TrackerWake.id)),
      Some(TrackerWake.noPawnChosen))
    assertEquals(writtenReason(menuOption(wakingAfterTrackingP2(home).ready,
      TrackerWake.id)), Some(TrackerWake.alreadyThere))

  test("with Tracker lost or facedown by Wake, the record has no source"):
    val lost = wakingAfterTrackingP2(peaks).withoutAdvisers(p1).ready
    val facedown = Table.start.turn(p1, Phase.Wake).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker, facedown = true)
      .trackedPawn(p1, p2).ready
    Vector(lost, facedown).foreach { ready =>
      assert(PhasePowerProcedure.sources(catalog, ready, p1, TrackerWake)
        .isEmpty)
      assert(startPower(ready, TrackerWake.id).isLeft)
    }

  // ---- The clear at Act ----

  test("End Wake clears the holder's record, used or not, and no other"):
    val ready = wakingAfterTrackingP2(peaks).trackedPawn(p2, p1).ready
    val unused = accepted(endWake(Ready(ready)))
    val afterUnused = Outcomes.ready(unused.state)
    assertEquals(Look(afterUnused).phase, Phase.Act)
    assertEquals(Look(afterUnused).pawn(p1), home)
    assertEquals(afterUnused.game.current.trackedPawns, Map(p2 -> p1))
    assertEquals(PhasePowerHarness.ops(unused.events).collect {
      case op: ClearTrackedPawn => op }, Vector(ClearTrackedPawn(p1)))
    assertEquals(ActPowerHarness.replayed(rules, ready, unused.events),
      afterUnused)
    val used = accepted(startPower(ready, TrackerWake.id))
    val ended = accepted(endWake(used.state))
    assertEquals(Outcomes.ready(ended.state).game.current.trackedPawns,
      Map(p2 -> p1))

  test("End Wake with no record clears nothing"):
    val ready = Table.start.turn(p1, Phase.Wake).pawn(p1, home).ready
    val ended = accepted(endWake(Ready(ready)))
    assert(!PhasePowerHarness.ops(ended.events)
      .exists(_.isInstanceOf[ClearTrackedPawn]))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.wake.TrackerSuite"`
Expected: compile failure, `Not found: TrackerCard`.

- [ ] **Step 3: Write the card and its powers**

In `HoldingDiscordDenizens.scala`, delete the `TrackerCard` object (lines
15 to 21). Its imports stay in use by the other cards.

Create `src/main/scala/distantlands/gameplay/powers/wake/Tracker.scala`:

```scala
package distantlands.gameplay.powers.wake

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.{PhasePower, QueuedStarts}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.{PaidAction, PawnMoves}
import distantlands.model._

object TrackerCard extends Denizen(DenizenId("221"), "Tracker", Suit.Discord) with AdviserOnly:
  val action = PrintedPower(PowerId("denizen.tracker.action"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Choose an enemy pawn at your site and end your " +
      "Act Phase.")
  val wake = PrintedPower(PowerId("denizen.tracker.wake"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** If you chose a pawn last turn, you may place your " +
      "pawn at their site.")
  val powers: Vector[PrintedPower] = Vector(action, wake)

/** Tracker (card 221), ACTION: "Choose an enemy pawn at your site and end
  * your Act Phase." (catalog batch 10, slice 14).
  *
  * The choice is recorded with `TrackPawn`, under the user, and the Act
  * phase ends with `EnterPhase(Rest)`, as Wizard School ends it. The record
  * outlives the turn; the user's next End Wake clears it. An enemy is
  * another player who is not Imperial alongside the user. With no enemy
  * pawn at the site, the menu shows "No valid enemy pawn". The line is
  * "Tracked {Red}."; the phase change shows in the lines around it.
  */
case object TrackerAction extends PaidAction(TrackerCard.action):
  val decisionId: String = "power.tracker.target"
  private val notes = NoteSet(id)
  val tracked: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Tracked "), NotePart.Arg(0), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The enemies of `player` whose pawns stand at `player`'s site. */
  def targets(ready: ReadyGame, player: PlayerId): Vector[PlayerId] =
    ready.game.current.pawnSite(player).toVector.flatMap(site =>
      ready.game.current.players.collect {
        case other if other.pawnSite.contains(site) &&
            Imperials.enemies(ready, player, other.player) => other.player })

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(targets(ready, player).isEmpty)(PawnMoves.noValidEnemyPawn)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      Branch((live, _) => Vector(Decide(decisionId, player,
        DecisionQuery.ChooseOne(targets(live, player).map(target =>
          DecisionOption.Player(DecisionOptionRef.Player(target))),
          heading = Some("Tracker: choose an enemy pawn at your site"))))),
      BuildOps((_, pending) => PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Player(target) =>
          Vector[CoreOperation](TrackPawn(player, target))
      }.toRight(PowerAnswers.missing(decisionId))),
      Note(id, states => for
        card <- PowerSourceRef.of(source)
        case DecisionOptionRef.Player(target) <-
          NoteSupport.answer(states, decisionId)
      yield tracked(card, NoteArg.Player(target))),
      EnterPhase(Phase.Rest))))

/** Tracker (card 221), WAKE: "If you chose a pawn last turn, you may place
  * your pawn at their site." (catalog batch 10, slice 14, ruling 6).
  *
  * Optional: a usable power in the Wake menu, never run on its own. It
  * places the pawn at the tracked pawn's site at that moment with a plain
  * `Move`, as Passages does, so no Travel rule reads it. Phase-power access
  * already requires Tracker to be the user's faceup adviser. The line is
  * Passages': "{Blue} placed at {site}."
  */
case object TrackerWake extends PhasePower:
  val id: PowerId = TrackerCard.wake.id
  def timing: PowerTiming = PowerTiming.Wake
  /** The reason with no record from last turn. */
  val noPawnChosen: String = "No pawn was chosen last turn"
  /** The reason with the user's pawn already at the tracked pawn's site. */
  val alreadyThere: String = "Already at the chosen pawn's site"
  private val notes = NoteSet(id)
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** The pawn `holder` chose, when `card` is the Tracker that records it. */
  def tracked(ready: ReadyGame, holder: PlayerId, card: CardId)
      : Option[PlayerId] =
    if card == TrackerCard.id then ready.game.current.trackedPawns.get(holder)
    else None

  def usable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean = true

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    val current = ready.game.current
    current.trackedPawns.get(player) match
      case None => Some(noPawnChosen)
      case Some(target) => Option.when(
        current.pawnSite(target) == current.pawnSite(player))(alreadyThere)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      BuildOps((live, _) => for
        target <- live.game.current.trackedPawns.get(player).toRight(
          OathViolation.InvalidEventOrder(
            s"${player.value} chose no pawn last turn"))
        site <- PawnMoves.pawnSite(live, target)
        moves <- PawnMoves.relocate(live, player, site)
      yield moves),
      Note(id, PawnMoves.placedNote(placed, source, player)))))
```

If `-Werror` flags `card == TrackerCard.id` as comparing unrelated types,
write `card == (TrackerCard.id: CardId)`, as `OperationPipeline` writes
`TheGrandScepterCard.id: CardId`.

In `PhasePowerCatalog.scala`, change the import
`import distantlands.gameplay.powers.wake.{HiddenPassages, Hunger,
MarbleFountains, Quartermaster, RiverSitePower, SinkingPassages}` to add
`TrackerAction, TrackerWake`, and the vector line
`Quartermaster, StonePortal, SinkingPassages, HiddenPassages) ++` to
`Quartermaster, StonePortal, SinkingPassages, HiddenPassages,
TrackerAction, TrackerWake) ++`.

`NewFoundations.scala` lists `TrackerCard` by its bare name and imports
both `catalog.holding._` and `gameplay.powers.wake._`, so it needs no
change.

- [ ] **Step 4: Clear the record at Act**

In `EndWakeProcedure.scala`, replace the tree comment and the closing
yield. The doc's tree block becomes:

```scala
  * {{{
  * Sequence(EnterPhase(Act), ClearTrackedPawn?)       // no window
  * }}}
```

Add this paragraph after the "**No window.**" paragraph:

```scala
  *
  * **A Tracker's record ends here** (catalog batch 10, slice 14). The
  * actor's Act phase begins, so the pawn they chose last turn is
  * forgotten, used or not. Only the actor's own record is cleared.
```

In `build`'s doc, change "End Wake declares one leaf" to "End Wake
declares at most two leaves". The yield becomes:

```scala
  yield Sequence(Vector[Operation](EnterPhase(Phase.Act)) ++
    Option.when(state.game.current.trackedPawns.contains(actor))(
      ClearTrackedPawn(actor)))
```

- [ ] **Step 5: Run the power tests**

Run: `./sbtw "testOnly distantlands.gameplay.powers.wake.TrackerSuite"`
Expected: PASS.

- [ ] **Step 6: Update the snapshots**

Delete the `denizen.tracker` line from
`src/test/resources/powers/unimplemented.txt`. Then:

```bash
NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"
git diff --stat src/test/resources
```

Expected: `declared-keys.txt` gains exactly the two lines of finding 11,
and `unimplemented.txt` loses `denizen.tracker`. If anything else changes,
stop and report.

- [ ] **Step 7: Run the touched procedures' suites**

Run: `./sbtw "testOnly distantlands.gameplay.powers.* distantlands.gameplay.WakeAutoEndSuite distantlands.gameplay.phases.* distantlands.gameplay.MinorActionsSuite distantlands.gameplay.StateBasedEvaluationSuite"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/wake/Tracker.scala src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala src/main/scala/distantlands/gameplay/phases/wake/EndWakeProcedure.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/wake/TrackerSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): implement Tracker

Its ACTION records an enemy pawn at the user's site and ends the Act
phase. Its optional WAKE power places the user's pawn at that pawn's site
on their next turn. End Wake clears the record as the Act phase begins.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 1:** review Tasks 1 and 2 (spec slice 14, findings 1
to 8 and 10).

### Task 3: The record on the table, docs and gates

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`
- Modify: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
- Create: `src/test/scala/distantlands/application/TrackerProjectionSuite.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala`
- Modify: `frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala`
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Consumes: Task 2's `TrackerCard` and `TrackerWake.tracked`, and Task 1's
  `Table.trackedPawn`.
- Produces: `TrackedPawnProjection(playerId: String, color: PlayerColor,
  label: String)` and `CardDetailsProjection.tracking:
  Option[TrackedPawnProjection] = None`.

- [ ] **Step 1: Write the failing projection tests**

Create `src/test/scala/distantlands/application/TrackerProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.powers.wake.TrackerCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.{PlayerId, ReadyGame}
import distantlands.protocol.projection.{CardDetailsProjection,
  TrackedPawnProjection}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The pawn a Tracker's holder chose reaches the table on the card, for
  * every viewer, since the choice was made openly (catalog batch 10, slice
  * 14). */
class TrackerProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  /** p1's Tracker as `viewer` sees it on p1's board. */
  private def trackerOfP1(ready: ReadyGame, viewer: PlayerId)(
      using munit.Location): CardDetailsProjection =
    presentation.playerBoards(ready, Some(viewer))
      .find(_.playerId == p1.value).get.advisers
      .find(_.cardId == TrackerCard.id.value).get

  test("p1's Tracker shows the pawn p1 chose, in its colour, to every " +
      "viewer"):
    val ready = Table.start.adviser(p1, TrackerCard.id).trackedPawn(p1, p2)
      .ready
    val color = ready.playerColors(p2)
    Vector(p1, p2, p3).foreach(viewer =>
      assertEquals(trackerOfP1(ready, viewer).tracking,
        Some(TrackedPawnProjection(p2.value, color,
          s"Tracking ${color.key.capitalize}"))))

  test("a Tracker with no record shows none"):
    val ready = Table.start.adviser(p1, TrackerCard.id).ready
    assertEquals(trackerOfP1(ready, p1).tracking, None)
```

In `ProjectionProtocolSuite`, after the test "warbands caged on a held
relic round-trip, one entry per force" (line 171):

```scala
  test("a Tracker's chosen pawn on a held adviser round-trips"):
    val tracker = known.copy(tracking = Some(TrackedPawnProjection("p2",
      PlayerColor.Red, "Tracking Red")))
    assertRoundTrips(projection.copy(playerBoards = projection.playerBoards
      .map(_.copy(advisers = Vector(tracker)))))
```

Add `TrackedPawnProjection` to that suite's projection imports if they are
not a wildcard.

In `CardFaceSuite`, after the test "warbands caged on a card show as
counted warbands in their colour":

```scala
  test("a Tracker's chosen pawn shows on the card in its player's colour"):
    val tracker = knowable.copy(orientation = Some("face-up"), tracking =
      Some(TrackedPawnProjection("p2", distantlands.model.PlayerColor.Red,
        "Tracking Red")))
    val marker = firstMatch(CardFace.render(tracker),
      ".card-tokens .card-tracking.force-red")
    assertEquals(marker.map(_.getAttribute("data-tip")), Some("Tracking Red"))
    assertEquals(marker.map(_.getAttribute("aria-label")),
      Some("Tracking Red"))
```

with `import distantlands.protocol.projection.TrackedPawnProjection` at the
top.

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.application.TrackerProjectionSuite distantlands.protocol.ProjectionProtocolSuite"`
Expected: compile failure, `Not found: TrackedPawnProjection`.

- [ ] **Step 3: Add the DTO, its codec and the projector**

In `WorldProjectionDtos.scala`, add to `CardDetailsProjection`, after
`warbands`:

```scala
    warbands: Vector[SiteForcesProjection] = Vector.empty,
    /** The pawn the Tracker's holder chose last turn (Tracker). */
    tracking: Option[TrackedPawnProjection] = None
)
/** A Tracker's record, as its card shows it: the player whose pawn was
  * chosen, their colour, and the card's words ("Tracking Red"). */
final case class TrackedPawnProjection(playerId: String,
    color: distantlands.model.PlayerColor, label: String)
```

In `WorldProjectionCodec.scala`:

- `encodeCard` adds `"tracking" -> option(value.tracking)(encodeTracked)`
  after `"warbands" -> ...`.
- `decodeCard` adds `"tracking"` to its `exact` set, reads
  `tracking <- optionalAbsent(value, "tracking", path)(decodeTracked)`
  after `warbands`, and passes `tracking` last to `CardDetailsProjection`.
- Add, after `decodePowerFace`:

  ```scala
  private def encodeTracked(value: TrackedPawnProjection): ujson.Value =
    ujson.Obj("playerId" -> value.playerId, "colorToken" -> value.color.key,
      "label" -> value.label)
  private def decodeTracked(raw: ujson.Value, path: String)
      : Result[TrackedPawnProjection] = for
    value <- obj(raw, path)
    _ <- exact(value, Set("playerId", "colorToken", "label"), path)
    playerId <- string(value, "playerId", path)
    color <- playerColor(value, "colorToken", path)
    label <- string(value, "label", path)
  yield TrackedPawnProjection(playerId, color, label)
  ```

In `GamePresentationProjector.scala`, import
`distantlands.gameplay.powers.wake.TrackerWake`. In `playerBoards`, wrap the
faceup adviser's details:

```scala
          if identifies(card.id, adviserOrientation(card),
              PlayerCardArea.Advisers) then
            withTracking(withTokens(cardDetails(card.id,
              Some(adviserOrientation(card)), hidden = false), card match {
                case value: DenizenState => value.tokens
                case _ => Tokens.empty
              }), ready, player.player, card.id)
```

and add, after `withWarbands`:

```scala
  /** The pawn `holder` chose with the Tracker they hold, on that card
    * (catalog batch 10, slice 14). The choice was made openly, so every
    * viewer who sees the card sees it. */
  private def withTracking(details: CardDetailsProjection, ready: ReadyGame,
      holder: PlayerId, card: CardId): CardDetailsProjection =
    TrackerWake.tracked(ready, holder, card).flatMap(target =>
      ready.playerColors.get(target).map(color => details.copy(tracking =
        Some(TrackedPawnProjection(target.value, color,
          s"Tracking ${color.key.capitalize}")))))
      .getOrElse(details)
```

- [ ] **Step 4: Draw the marker**

In `CardFace.scala`'s `summary`, after the `card.warbands.foreach { ... }`
block:

```scala
    // The pawn a Tracker's holder chose last turn, in its player's colour.
    card.tracking.foreach { tracked =>
      val node = text("span", s"card-tracking force-${tracked.color.key}",
        "●")
      node.setAttribute("aria-label", tracked.label)
      HoverTip.set(node, tracked.label)
      tokens.appendChild(node)
    }
```

- [ ] **Step 5: Run the tests to see them pass**

```bash
./sbtw "testOnly distantlands.application.TrackerProjectionSuite distantlands.application.CardWarbandsProjectionSuite distantlands.protocol.ProjectionProtocolSuite"
```

```bash
./sbtw "frontend/testOnly distantlands.frontend.CardFaceSuite"
```

Expected: PASS.

- [ ] **Step 6: Update the spec**

In the spec, replace the body of "## Slice 14: Memory across turns" with:

```markdown
Tracker's power ids split into `denizen.tracker.action` and
`denizen.tracker.wake`, as Obsidian Cage's are.

`CurrentGameState` gains `trackedPawns: Map[PlayerId, PlayerId]`, keyed by
the Tracker's holder. The primitives `TrackPawn` and `ClearTrackedPawn`
write it, and are journalled, so replay rebuilds it.

- **Action.** Free. The user picks an enemy pawn at their site
  (`Imperials.enemies`), records `TrackPawn`, then `EnterPhase(Rest)`.
  With no enemy pawn there, the menu shows "No valid enemy pawn". Line:
  "Tracked {Red}." No phase-end line, since log lines name no actor.
- **Wake.** An optional `PhasePower` (ruling 6), usable while the user
  holds Tracker faceup and has a record. It places the pawn at the
  target's current site with `PawnMoves.relocate`, as Passages does. It is
  unavailable with "No pawn was chosen last turn" or "Already at the
  chosen pawn's site". Line: "{Blue} placed at {site}."
- **Clear.** End Wake, the only way into the Act phase, adds
  `ClearTrackedPawn` for the actor's record, used or not.

The projection hangs the record on the Tracker card
(`CardDetailsProjection.tracking`), for every viewer, since the choice was
open. The frontend draws it as a pawn marker in the player's colour, with
"Tracking Red" as its hover. A refusal reason is a plain string and names
no player.

Tests: the Action ending Act and its record outliving the turn; enemies
only; the Wake power offered, optional and placing the pawn at a moved
target's site; both Wake reasons; Tracker lost or facedown before Wake;
the record cleared at Act; the primitives, codec, replay and projection.
```

Set the status line to "Slices 1 to 12 and 14 landed." If
`git log --oneline 0.2.0-alpha | grep "the Patrols' ruler may let an
enemy recover"` prints a line, rebase onto `0.2.0-alpha` first (keep both
sides of `declared-keys.txt`, sorted) and write "Slices 1 to 14 landed."

- [ ] **Step 7: Update the ROADMAP**

In `docs/ROADMAP.md`, replace

```markdown
- **Unchanged from batch 8:** Master of Disguise (acting as another
  player's advisers), Tracker and False Prophet (a player-aware off-turn
  card play).
```

with

```markdown
- **Unchanged from batch 8:** Master of Disguise (acting as another
  player's advisers) and False Prophet (a player-aware off-turn card
  play).
```

- [ ] **Step 8: Run the gates**

```bash
python3 scripts/check-architecture.py
```

```bash
python3 scripts/check-markdown-links.py
```

Expected: both pass. Then run the frontend suite, and the full suite with
coverage in the background:

```bash
./sbtw frontend/test
```

```bash
./sbtw clean coverage test coverageReport
```

Expected: every test passes, and statement coverage is at least 92%.

- [ ] **Step 9: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala src/main/scala/distantlands/application/GamePresentationProjector.scala src/test/scala/distantlands/application/TrackerProjectionSuite.scala frontend/src/main/scala/distantlands/frontend/CardFace.scala frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(projection): show a Tracker's chosen pawn on the card

The holder's Tracker carries the pawn they chose, in that player's
colour, for every viewer, since the choice was made openly.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 2:** review Task 3 (spec slice 14, finding 9).
