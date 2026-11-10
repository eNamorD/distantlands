# Chronicle steps foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A finished game lets its winner press "Start Chronicle", which enters `Phase.Chronicle` and journals `ChronicleUpdated(chronicle)` carrying the unchanged baseline Chronicle, with the groundwork the five Chronicle tasks need.

**Architecture:** `Chronicle` gains required `era` and `oathkeeperGoal`; game start keeps the whole Chronicle on `CampaignState`. A triggered `ChronicleBegin` procedure (a winner-owned parked decision) and a closing `ChronicleFinish` are chained by the driver through a `ChronicleSequence` table. Later plans insert Sun, Throne, World, Beacon and Stars between them.

**Tech Stack:** Scala 3.9, munit 1.0.4, sbt via `./sbtw`, Scala.js frontend (jsdom tests).

**Spec:** `docs/superpowers/specs/2026-10-27-chronicle-steps-design.md`

**Not in this plan:** the Sun and Throne procedures (they wait on the rules-doc rewrite in Task 1 and the rulings it collects), World, Beacon, Stars, and Persistence. A follow-up plan covers Sun and Throne.

## Global Constraints

- Branch and worktree off local `0.2.0-alpha`, never `main`. Merge back into `0.2.0-alpha`.
- Scalac runs with `-Werror`; a new sealed case breaks every exhaustive match, so fix each one the compiler names.
- Production Scala files stay at or under 800 lines (`scripts/check-architecture.py`, `BackendArchitectureSuite`); `model` must not import `gameplay`, `application` or `serialization`; `gameplay` must not import `application` or `serialization`.
- Tests run with `oathdigital.notes.strict=true`: a log note key not declared by its writer throws.
- Old journals need not decode: new `Chronicle` fields are required.
- Test helper names state what they build (no vague names).
- Never `git add -A` (the worktree symlinks show as untracked). Add files by path.
- Commit trailer, exactly: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Docs: normal prose; run `python3 scripts/check-markdown-links.py` after doc edits.

## File Structure

- Modify `src/main/scala/distantlands/model/Chronicle.scala`: `era`, `oathkeeperGoal`.
- Modify `src/main/scala/distantlands/model/GameState.scala`: `CampaignState.baseline`, `CampaignState.evolved`, `Phase.Chronicle`.
- Modify `src/main/scala/distantlands/model/ProcedureRef.scala`: `ChronicleBegin`, `ChronicleFinish`.
- Modify `src/main/scala/distantlands/model/CoreOperations.scala`: `ChronicleUpdated` operation.
- Modify `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`: read `era` and `oathkeeperGoal` from the Chronicle, keep the baseline.
- Create `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeginProcedure.scala`, `ChronicleFinishProcedure.scala`, `ChronicleSequence.scala`.
- Modify `src/main/scala/distantlands/gameplay/OathDriver.scala`: Chronicle branch.
- Modify `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`, `application/gamelog/ProcedureLog.scala`, `model/PowerWindow.scala` only if a window is needed (it is not in this plan).
- Modify serialization: `GameEventJsonSupport.scala` (Chronicle fields) and `WalkerOperationCodec.scala` (`ChronicleUpdated`).
- Modify frontend: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala`.
- Docs: `docs/rules/chronicle.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `CONTEXT.md`.
- Tests are named per task.

Paths below are relative to the repo root; `M/` = `src/main/scala/distantlands/`, `T/` = `src/test/scala/distantlands/`.

---

### Task 0: Worktree

**Files:** none (git only)

- [ ] **Step 1: Create the worktree off the release branch**

```bash
cd /Users/roman/projects/oathdigital
git worktree add .worktrees/chronicle-foundation -b chronicle-foundation 0.2.0-alpha
cd .worktrees/chronicle-foundation
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
git status --short
```

Expected: clean tree (the two symlinks are untracked or ignored).

- [ ] **Step 2: Confirm the build runs**

Run: `./sbtw "testOnly oathdigital.model.ChronicleSuite"`
Expected: PASS. The first run is slow (Scala.js link).

---

### Task 1: Rewrite `docs/rules/chronicle.md` from the rulebook (human-gated)

**Files:**
- Modify: `docs/rules/chronicle.md`
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/rules/implementation-traceability.md` (rows under "Chronicle and durable campaign", lines ~122-133)

**Interfaces:**
- Produces: per-task step lists with an **Actor** line each (a named holder, or "none: procedural"), and ruling entries in `ambiguities.md`. The follow-up Sun/Throne plan cites both.

- [ ] **Step 1: Extract the rulebook text for pp. 36-41**

```bash
cat > "$TMPDIR/pdftext.swift" <<'EOF'
import PDFKit
let d = PDFDocument(url: URL(fileURLWithPath: CommandLine.arguments[1]))!
for i in 35...40 { print("=== CR page \(i+1)"); print(d.page(at: i)!.string ?? "") }
EOF
swiftc -o "$TMPDIR/pdftext" "$TMPDIR/pdftext.swift"
"$TMPDIR/pdftext" "reference/Oath Combined Rulebook.pdf" > "$TMPDIR/cr-chronicle.txt"
```

Read the output in full. Page numbers equal printed page numbers for pp. 36-41.

- [ ] **Step 2: Read the task cards visually**

Use the Read tool on `reference/2026-03-17-new-foundations-rev1/chronicle-tasks.pdf` with `pages: "1-3"`. The PDF is image-only. Compare each card with the rulebook text and note differences (New Foundations may change a step).

- [ ] **Step 3: Rewrite each task section**

For every task (Sun, Throne, World, Beacon, Stars) write numbered steps in the rulebook's own order and add an `**Actor:**` line. Apply these known corrections (found by comparing the old summary against CR pp. 36-39; re-verify each against your extracted text):
- World steps are resolved by the World task holder (People's Favor holder). Beacon steps by the Beacon task holder (Darkest Secret holder).
- Most cleanup steps have no actor. Stars has no actor-bound step except "Writing in the Stars", which the digital version omits.
- Sun step 7 is two rules: if no one holds the People's Favor the winner chooses who takes the World task; if no one holds the Darkest Secret the winner chooses who takes the Beacon task. A player holding both the World and Beacon tasks chooses the recipient and which task to give.
- Shape Empire: you may remove warbands except your last warband on the map; if you rule no sites, remove all warbands from the site with your pawn and place one of yours there.
- Build Edifice: no one-edifice limit and no "intact" filter in the source text; ruined edifices go to the bottom of the edifice deck. Locked cards and edifices can be buried; a buried denizen goes to the bottom of the world deck.
- Beacon, Introduce Newcomers: if there are not enough cards of any desired suit, before drawing return all Dispossessed cards except the 6 just added by World to their suit sections.
- The three-active-legacy cap is per player.
- Crown: winner takes the Chancellor board, purple pieces and Grand Scepter; "do not add bandits" when Imperial warbands are removed.
- Add "facedown" wherever the source says cards go to the world deck facedown.
- Remove Stars' "play again or store" choice. State that the digital phase always ends by journaling `ChronicleUpdated`.

- [ ] **Step 4: Collect ambiguities and ask the user**

For every step where the source is silent or unclear, write the question with the citation, a recommended answer, and stop to ask the user (standing rule: consult, never default). Record each ruling in `docs/rules/ambiguities.md` as a new numbered entry in that file's existing style. Already ruled: seated-only task recipients; Sun step 7 as above; Stars actorless.

- [ ] **Step 5: Update traceability rows**

In `docs/rules/implementation-traceability.md`, keep the Sun/Throne/World/Beacon/Stars rows at their current status, update the "Normative source" cells to the new `chronicle.md` anchors, and set "Last reviewed" to the date of the commit.

- [ ] **Step 6: Check links and commit**

Run: `python3 scripts/check-markdown-links.py && git diff --check`
Expected: no output.

```bash
git add docs/rules/chronicle.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md
git commit -m "docs: rewrite the Chronicle rules from the rulebook, with actors and rulings

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `Chronicle` carries `era` and `oathkeeperGoal`

**Files:**
- Modify: `M/model/Chronicle.scala:26-35`
- Modify: `M/serialization/GameEventJsonSupport.scala` (`encodeChronicle` ~:142, `decodeChronicle` ~:155, constructor call ~:165)
- Modify: `M/application/FirstGameChronicleGenerator.scala:42`
- Modify: `T/gameplay/setup/FirstGameSetupFixture.scala:63`
- Test: `T/serialization/GameEventFactsWireSuite.scala`

**Interfaces:**
- Produces: `Chronicle(atlasBox, world = Vector.empty, worldDeck, relicDeck, era: EraState, oathkeeperGoal: OathkeeperGoal, dispossessed = Vector.empty, reliquary = Vector.empty, foundations = Map.empty, lineages = Vector.empty)`. Wire object gains `"era": {"targetScore": Int, "lineageScores": [{"lineageId": String, "score": Int}]}` and `"oathkeeperGoal": String` (the `OathkeeperGoal.key`).

- [ ] **Step 1: Write the failing round-trip test**

Add to `T/serialization/GameEventFactsWireSuite.scala`, next to the existing `GameStarted` round trip (read it first at lines ~43-54 and reuse its imports and encode/decode helpers):

```scala
test("a GameStarted Chronicle round-trips its era scores and Oathkeeper goal"):
  val scored = FirstGameSetupFixture.chronicle.copy(
    era = EraState(targetScore = 15, lineageScores = Map(LineageId("l1") -> 4, LineageId("l9") -> 7)),
    oathkeeperGoal = OathkeeperGoal.Devotion)
  val event = GameStarted(scored, FirstGameSetupFixture.orders)
  val encoded = GameEventWire.encodeEvent("setup", 0, event)
  assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
```

Match the exact `encodeEvent`/`decode` call shape of the neighbouring test.

- [ ] **Step 2: Run it to see the compile failure**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite"`
Expected: compile error, `era` is not a member of `Chronicle`.

- [ ] **Step 3: Add the fields**

In `M/model/Chronicle.scala` change the case class to:

```scala
final case class Chronicle(
    atlasBox: Vector[StoredSite],
    world: Vector[StoredSite] = Vector.empty,
    worldDeck: Vector[DenizenId],
    relicDeck: Vector[RelicId],
    era: EraState,
    oathkeeperGoal: OathkeeperGoal,
    dispossessed: Vector[DenizenId] = Vector.empty,
    reliquary: Vector[RelicId] = Vector.empty,
    foundations: Map[FoundationNumber, FoundationState] = Map.empty,
    lineages: Vector[LineageState] = Vector.empty
)
```

Extend the doc comment: "`era` and `oathkeeperGoal` are required; a first game starts at `EraState(20, Map.empty)` and `OathkeeperGoal.Supremacy`. The TTS codec has no era field, and gains an Oath digit in encoding version 3; Persistence decides the wire format."

- [ ] **Step 4: Encode and decode them**

In `encodeChronicle` add two keys (lineage scores sorted by id for a stable wire):

```scala
"era" -> ujson.Obj(
  "targetScore" -> c.era.targetScore,
  "lineageScores" -> ujson.Arr.from(c.era.lineageScores.toVector.sortBy(_._1.value).map {
    case (id, score) => ujson.Obj("lineageId" -> id.value, "score" -> score) })),
"oathkeeperGoal" -> c.oathkeeperGoal.key
```

In `decodeChronicle` add `era <- decodeEra(requiredField(value, "era", path), s"$path.era")` and `goal <- decodeOathkeeperGoal(...)` into the for-comprehension, build with named arguments (`Chronicle(atlasBox = ..., era = era, oathkeeperGoal = goal, ...)`), and add:

```scala
private def decodeEra(value: ujson.Value, path: String): Either[WireError, EraState] =
  for
    target <- safeIntField(value, "targetScore", path)
    scores <- traverse(value("lineageScores").arr.toVector.zipWithIndex) { case (row, i) =>
      for score <- safeIntField(row, "score", s"$path.lineageScores[$i]")
      yield LineageId(row("lineageId").str) -> score }
  yield EraState(target, scores.toMap)

private def decodeOathkeeperGoal(value: ujson.Value, path: String): Either[WireError, OathkeeperGoal] =
  OathkeeperGoal.all.find(_.key == value.str).toRight(WireError.InvalidValue(path, s"unknown Oathkeeper goal ${value.str}"))
```

Use the real `requiredField`/`safeIntField`/`traverse`/`WireError` names found near `GameEventJsonSupport.scala:230`; adapt the error constructor to what exists.

- [ ] **Step 5: Fix the other two construction sites**

In `FirstGameChronicleGenerator.scala:42` and `FirstGameSetupFixture.scala:63` add `era = EraState(20, Map.empty), oathkeeperGoal = OathkeeperGoal.Supremacy`.

- [ ] **Step 6: Run the suite and the wider setup tests**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite oathdigital.gameplay.setup.* oathdigital.application.FirstGameChronicleGeneratorSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add M/model/Chronicle.scala M/serialization/GameEventJsonSupport.scala M/application/FirstGameChronicleGenerator.scala T/gameplay/setup/FirstGameSetupFixture.scala T/serialization/GameEventFactsWireSuite.scala
git commit -m "feat(model): Chronicle carries the era scorepad and the Oathkeeper goal

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

(Expand `M/` and `T/` to the full `src/main/scala/distantlands/` and `src/test/scala/distantlands/` paths in the real command.)

---

### Task 3: Game start keeps the baseline Chronicle

**Files:**
- Modify: `M/model/GameState.scala:48` (`CampaignState`)
- Modify: `M/gameplay/setup/GameStartRules.scala:34-46`
- Modify: `T/testkit/TestGameFixtures.scala:111-113` and any other `CampaignState(` construction the compiler names
- Test: `T/gameplay/setup/GameStartRulesSuite.scala`

**Interfaces:**
- Consumes: `Chronicle` from Task 2.
- Produces: `CampaignState.baseline: Chronicle` (required), `CampaignState.evolved: Option[Chronicle] = None`. After `GameStartRules.evolve`, `campaign.baseline == chronicle`, `campaign.oathkeeperGoal == chronicle.oathkeeperGoal`, and `campaign.era.lineageScores` keeps the Chronicle's scores plus a zero for each seated lineage that has none.

- [ ] **Step 1: Write the failing tests**

Add to `T/gameplay/setup/GameStartRulesSuite.scala` (reuse its `catalog`, `orders` and `accepted`-style helpers):

```scala
test("game start keeps the whole Chronicle, including lineages of colors that did not play"):
  val absent = LineageState(LineageId("l9"), None, Role.Exile, Vector.empty, Vector.empty)
  val chronicle = FirstGameSetupFixture.chronicle.copy(lineages = Vector(absent))
  val ready = GameStartRules.evolve(catalog, chronicle, FirstGameSetupFixture.orders).toOption.get
  assertEquals(ready.game.campaign.baseline, chronicle)

test("game start takes the era and Oathkeeper goal from the Chronicle"):
  val chronicle = FirstGameSetupFixture.chronicle.copy(
    era = EraState(15, Map(LineageId("l9") -> 6)), oathkeeperGoal = OathkeeperGoal.Protection)
  val campaign = GameStartRules.evolve(catalog, chronicle, FirstGameSetupFixture.orders)
    .toOption.get.game.campaign
  assertEquals(campaign.oathkeeperGoal, OathkeeperGoal.Protection)
  assertEquals(campaign.era.targetScore, 15)
  assertEquals(campaign.era.lineageScores(LineageId("l9")), 6)
  assertEquals(campaign.era.lineageScores(LineageId("l1")), 0)
```

- [ ] **Step 2: Run to see the failure**

Run: `./sbtw "testOnly oathdigital.gameplay.setup.GameStartRulesSuite"`
Expected: compile error, `baseline` is not a member of `CampaignState`.

- [ ] **Step 3: Add the fields to `CampaignState`**

```scala
final case class CampaignState(
    atlas: AtlasState,
    foundations: Map[FoundationNumber, FoundationState],
    lineages: Map[LineageId, LineageState],
    reliquary: Vector[RelicId],
    dispossessed: Vector[WorldCardId],
    suitedReserves: Map[Suit, Vector[DenizenId]],
    oathkeeperGoal: OathkeeperGoal,
    era: EraState,
    baseline: Chronicle,
    evolved: Option[Chronicle] = None
)
```

(Keep the existing field order and `require`s; only append `baseline` and `evolved`.)

- [ ] **Step 4: Set them in `GameStartRules`**

Where the `CampaignState` is built (around `GameStartRules.scala:34-46`) replace the hard-coded era and goal:

```scala
val seatedIds = lineages.keys.toVector
val scores = seatedIds.map(_ -> 0).toMap ++ chronicle.era.lineageScores
// in the CampaignState(...) call:
oathkeeperGoal = chronicle.oathkeeperGoal,
era = chronicle.era.copy(lineageScores = scores),
baseline = chronicle
```

- [ ] **Step 5: Fix other `CampaignState(` constructions**

Run `./sbtw test:compile`. For each error (start with `T/testkit/TestGameFixtures.scala:111-113`) add `baseline = FirstGameSetupFixture.chronicle`. Do not change production behaviour to satisfy a fixture.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.setup.* oathdigital.testkit.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/testkit/TestGameFixtures.scala
git commit -m "feat(setup): game start keeps the baseline Chronicle on the campaign

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: `Phase.Chronicle`, the two procedure refs, and the `ChronicleUpdated` operation

**Files:**
- Modify: `M/model/GameState.scala:64-76` (`Phase`)
- Modify: `M/model/ProcedureRef.scala:57-81,132`
- Modify: `M/model/CoreOperations.scala` (near `EnterPhase`, :560)
- Modify: `M/gameplay/operations/TurnStateOperations.scala`, `OperationApplication.scala`, `M/serialization/WalkerOperationCodec.scala`, and the other exhaustive matches the compiler names (`OperationRestrictions`, `application/OperationDetails`, `powerresolver/OperationRewrite`, `walker/HiddenOutcomes`)
- Modify: `M/application/PendingProjector.scala:37-43`, `M/application/gamelog/GameLogFormatter.scala:276-279`
- Test: `T/model/EnumWireKeySuite.scala`, `T/model/ProcedureRefSuite.scala`, `T/serialization/GameEventFactsWireSuite.scala`

**Interfaces:**
- Produces:
  - `Phase.Chronicle` with key `"chronicle"`, in `Phase.all`.
  - `TriggeredProcedureRef.ChronicleBegin` (key `"chronicle-begin"`) and `ChronicleFinish` (key `"chronicle-finish"`), in `TriggeredProcedureRef.all`.
  - `final case class ChronicleUpdated(chronicle: Chronicle) extends PrimitiveOperation`, a core operation recorded in a walker step, not a standalone event. Applying it sets `campaign.evolved = Some(chronicle)`. Wire: `{"kind": "chronicle-updated", "chronicle": <encodeChronicle>}` in `WalkerOperationCodec`.

- [ ] **Step 1: Write the failing tests**

In `EnumWireKeySuite` add `Phase.Chronicle` to the Phase round-trip list; in `ProcedureRefSuite` add both refs to its hand-written key list and assert key uniqueness still holds. In `T/serialization/WalkerOperationWireSuite.scala`, using that suite's `recordedOperation(op)` round-trip helper:

```scala
test("ChronicleUpdated round-trips its Chronicle"):
  val op = ChronicleUpdated(FirstGameSetupFixture.chronicle.copy(oathkeeperGoal = OathkeeperGoal.ThePeople))
  assertEquals(roundTrip(recordedOperation(op)), Right(op))
```

In `T/gameplay/operations/OperationExecutorSuite.scala`, using `executor.execute(ready, op)`:

```scala
test("ChronicleUpdated records the evolved Chronicle on the campaign"):
  val evolved = FirstGameSetupFixture.chronicle.copy(oathkeeperGoal = OathkeeperGoal.Devotion)
  val after = executor.execute(Table.start.ready, ChronicleUpdated(evolved)).toOption.get
  assertEquals(after.game.campaign.evolved, Some(evolved))
```

- [ ] **Step 2: Run to see the failures**

Run: `./sbtw "testOnly oathdigital.model.EnumWireKeySuite oathdigital.model.ProcedureRefSuite oathdigital.serialization.WalkerOperationWireSuite oathdigital.gameplay.operations.OperationExecutorSuite"`
Expected: compile errors for the missing members.

- [ ] **Step 3: Add the phase and the refs**

```scala
case object Chronicle extends Phase { val key = "chronicle" }
val all: Vector[Phase] = Vector(Setup, Wake, Act, Rest, RoundEnd, WarExhaustion, Chronicle)
```

```scala
case ChronicleBegin extends TriggeredProcedureRef("chronicle-begin")
case ChronicleFinish extends TriggeredProcedureRef("chronicle-finish")
// and in object TriggeredProcedureRef: append both to `all`
```

- [ ] **Step 4: Add the operation**

`CoreOperations.scala`: `final case class ChronicleUpdated(chronicle: Chronicle) extends PrimitiveOperation` with `required = true`, as `EnterPhase` does. `TurnStateOperations`:

```scala
def chronicleUpdated(ready: ReadyGame, chronicle: Chronicle): ReadyGame =
  ready.updateCampaign(_.copy(evolved = Some(chronicle)))
```

`OperationApplication.applyNonMoveLeaves`: `case (result, ChronicleUpdated(chronicle)) => result.map(TurnStateOperations.chronicleUpdated(_, chronicle))`. Without this case the fallthrough silently ignores the op. `WalkerOperationCodec`: encode as `ujson.Obj("kind" -> "chronicle-updated", "chronicle" -> encodeChronicle(chronicle))`, decode that kind with `decodeChronicle`, and add the type to the `TurnOperation` union.

- [ ] **Step 5: Fix exhaustive matches the compiler names**

`PendingProjector.scala:37-43`: map `Phase.Chronicle` to `"chronicle"` (the `"game-over"` branch at :23 still wins while `result` is set; leave it). `GameLogFormatter.scala:276-279` `phaseWords`: the wildcard falls to Rest; add an explicit `case Phase.Chronicle` that yields the Chronicle headline, or the narrowest equivalent the match allows. Add the same for any other match `./sbtw compile` flags.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "testOnly oathdigital.model.* oathdigital.serialization.* oathdigital.gameplay.operations.*"`
Expected: PASS. (`WalkerProcedureRegistrySuite` and the Game Log suites fail until Task 5; do not run them yet.)

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model src/main/scala/distantlands/serialization src/main/scala/distantlands/gameplay/operations src/main/scala/distantlands/application/PendingProjector.scala src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala src/test/scala/distantlands/model src/test/scala/distantlands/serialization src/test/scala/distantlands/gameplay/operations
git commit -m "feat(model): Chronicle phase, its two procedure refs and the ChronicleUpdated operation

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: `ChronicleBegin` and `ChronicleFinish` procedures, driver chaining

**Files:**
- Create: `M/gameplay/chronicle/ChronicleSequence.scala`
- Create: `M/gameplay/chronicle/ChronicleBeginProcedure.scala`
- Create: `M/gameplay/chronicle/ChronicleFinishProcedure.scala`
- Modify: `M/gameplay/OathDriver.scala:48-56`
- Modify: `M/gameplay/walker/WalkerProcedureRegistry.scala` (near the Setup entry, :329)
- Modify: `M/application/gamelog/ProcedureLog.scala` (`rules`, exhaustive)
- Test: `T/gameplay/chronicle/ChronicleBeginSuite.scala`, `T/gameplay/OathDriverSuite.scala:79`, `T/gameplay/walker/WalkerProcedureRegistrySuite.scala`, game log properties suites

**Interfaces:**
- Consumes: Task 4's refs, phase and event; `GameResult.winner`.
- Produces:
  - `ChronicleBeginProcedure.decisionId = "chronicle.begin"`; `build(catalog, ready, actor, args)` returns a tree that parks one `Decide` owned by the winner, then `EnterPhase(Phase.Chronicle)`.
  - `ChronicleFinishProcedure.build(...)` returns a tree whose one `BuildOps` yields `ChronicleUpdated(ready.game.campaign.baseline)`.
  - `ChronicleSequence.after(completed: ProcedureRef): Option[TriggeredProcedureRef]` (`ChronicleBegin` → `ChronicleFinish`; later plans insert tasks) and `ChronicleSequence.opening(current: CurrentGameState): Option[TriggeredProcedureRef]` (`ChronicleBegin` when `result` is set, the phase is not `Chronicle`, and no walk is pending).

- [ ] **Step 1: Read the driver and an existing menu build**

Read `M/gameplay/OathDriver.scala:40-160`, `M/gameplay/phases/PhaseMenu.scala:75-82,180-197` and `T/gameplay/OathDriverSuite.scala:60-95`. Note how `next` receives the completed procedure, how `startTriggered(transition, ref)` is called, and how PhaseMenu builds a button option and heading. Reuse those exact constructors below.

- [ ] **Step 2: Write the failing driver tests**

Replace the `"a result stops the driver"` test (OathDriverSuite:79) with these, built from that file's existing helpers (a ready game with `result` set, run through the driver):

```scala
test("a result parks the winner's Start Chronicle decision"):
  val won = driverAfter(readyWithResult(winner = p2))
  parked.assertParked(won.state, TriggeredProcedureRef.ChronicleBegin,
    ChronicleBeginProcedure.decisionId, p2)

test("answering Start Chronicle enters the Chronicle phase and journals ChronicleUpdated"):
  val answered = driverAfter(readyWithResult(winner = p2)).resolved(
    p2, ChronicleBeginProcedure.decisionId, startChronicleAnswer)
  val ready = Outcomes.ready(answered.state)
  assertEquals(ready.game.current.turn.phase, Phase.Chronicle)
  assertEquals(ready.game.campaign.evolved, Some(ready.game.campaign.baseline))
  assert(RecordedOps.of(answered.events).contains(ChronicleUpdated(ready.game.campaign.baseline)))

test("the driver stops once the Chronicle is journaled"):
  val finished = driverAfter(readyWithResult(winner = p2)).resolved(
    p2, ChronicleBeginProcedure.decisionId, startChronicleAnswer)
  assertEquals(driverAfter(finished.state), finished)

test("only the winner may answer Start Chronicle"):
  val won = driverAfter(readyWithResult(winner = p2))
  assert(rules.resolveWalker(won.state, p1, ChronicleBeginProcedure.decisionId, startChronicleAnswer).isLeft)
```

Define `driverAfter`, `readyWithResult`, `resolved` and `startChronicleAnswer` as small named helpers in the suite, modelled on the helpers already there (`Table.start.update(...)` with a comment explaining the `result` arrangement is acceptable until a `Table.won(p)` step is added; add that step if two suites need it).

- [ ] **Step 3: Run to see the failures**

Run: `./sbtw "testOnly oathdigital.gameplay.OathDriverSuite"`
Expected: FAIL (compile error or the driver returns without parking).

- [ ] **Step 4: Write `ChronicleSequence`**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.model.*

/** The fixed order of the Chronicle Phase. The driver starts the successor of whatever just completed. */
object ChronicleSequence:
  private val order: Vector[TriggeredProcedureRef] =
    Vector(TriggeredProcedureRef.ChronicleBegin, TriggeredProcedureRef.ChronicleFinish)

  def after(completed: ProcedureRef): Option[TriggeredProcedureRef] =
    order.indexOf(completed) match
      case -1 => None
      case at => order.lift(at + 1)

  /** The Chronicle opens once: a result is set, the phase is not yet Chronicle, and nothing is pending. */
  def opening(current: CurrentGameState): Option[TriggeredProcedureRef] =
    Option.when(current.result.nonEmpty && current.turn.phase != Phase.Chronicle
      && current.walkerPending.isEmpty)(TriggeredProcedureRef.ChronicleBegin)
```

- [ ] **Step 5: Write the two procedures**

`ChronicleBeginProcedure.scala` (adapt `DecisionOption`/`DecisionQuery` construction to the constructors you read in PhaseMenu):

```scala
package oathdigital.gameplay.chronicle

object ChronicleBeginProcedure:
  val decisionId: String = "chronicle.begin"
  private val startOption = DecisionOptionRef.Button("chronicle.start")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), InvalidEventOrder("Start Chronicle selects nothing"))
      result <- ready.game.current.result.toRight(InvalidEventOrder("the game has no result"))
    yield Sequence(Vector(
      Decide(decisionId, result.winner,
        DecisionQuery.ChooseOne(Vector(startOptionFor(startOption, "Start Chronicle")),
          heading = Some("Start Chronicle")),
        presentation = DecisionPresentation.Panel),
      BuildOps((_, _) => Right(Vector(EnterPhase(Phase.Chronicle))))))
```

`startOptionFor` is a private helper that builds the available option exactly as `PhaseMenu` does for its ending button.

`ChronicleFinishProcedure.scala`:

```scala
object ChronicleFinishProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Right(BuildOps((state, _) => Right(Vector(ChronicleUpdated(state.game.campaign.baseline)))))
```

- [ ] **Step 6: Register them**

In `WalkerProcedureRegistry.scala` next to Setup:

```scala
TriggeredProcedureRef.ChronicleBegin -> Entry(rollDecisionId = None, modifierWindow = None,
  build = ChronicleBeginProcedure.build, rebuild = ChronicleBeginProcedure.build),
TriggeredProcedureRef.ChronicleFinish -> Entry(rollDecisionId = None, modifierWindow = None,
  build = ChronicleFinishProcedure.build, rebuild = ChronicleFinishProcedure.build),
```

In `ProcedureLog.rules` add arms (reason text is required by `ActionLineSource.Silent`):

```scala
case TriggeredProcedureRef.ChronicleBegin =>
  ProcedureLog(tag = None, lines = ActionLineSource.Silent("the decision and the phase change narrate themselves"), isSection = false)
case TriggeredProcedureRef.ChronicleFinish =>
  ProcedureLog(tag = None, lines = ActionLineSource.Silent("the Chronicle update is a campaign record, not table play"), isSection = false)
```

Match the real `ProcedureLog(...)` parameter list from the neighbouring Setup arm (:297-303).

- [ ] **Step 7: Chain them in the driver**

In `OathDriver.next`, add two arms **before** the early return on `result`:

```scala
case Ready(ready) if ChronicleSequence.opening(ready.game.current).isDefined =>
  startTriggered(transition, ChronicleSequence.opening(ready.game.current).get)
case Ready(ready) if ready.game.current.turn.phase == Phase.Chronicle && completed.flatMap(ChronicleSequence.after).isDefined =>
  startTriggered(transition, completed.flatMap(ChronicleSequence.after).get)
```

`completed` is the procedure that `next` already receives for the `WalkerCompleted` case; use the real parameter name from the code you read. When neither arm matches, the existing `result.nonEmpty => Right(transition)` arm still stops the driver, which makes the "driver stops after the Chronicle is journaled" test pass.

- [ ] **Step 8: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.OathDriverSuite oathdigital.gameplay.chronicle.* oathdigital.gameplay.walker.WalkerProcedureRegistrySuite oathdigital.model.ProcedureRefSuite oathdigital.application.gamelog.*"`
Expected: PASS. If `GameLogPropertiesSuite` scripts every ref, fix what it names (it needs a script entry for each new ref or an explicit exemption with a reason).

- [ ] **Step 9: Replay equals live**

Add to `ChronicleBeginSuite` (model on `ActPowerHarness.replayed`, or the fold in `GameStartToWakeSuite`):

```scala
test("replaying the recorded events reproduces the Chronicle phase state"):
  val live = completedChronicleTransition()
  val replayed = live.events.foldLeft[Either[OathViolation, OathState]](Right(startOfChronicleState())):
    case (Right(state), event) => rules.evolve(state, event)
    case (left, _) => left
  assertEquals(replayed, Right(live.state))
```

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleBeginSuite"`. Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay src/main/scala/distantlands/model src/main/scala/distantlands/serialization src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/test/scala/distantlands/gameplay src/test/scala/distantlands/application/gamelog
git commit -m "feat(chronicle): the winner starts the Chronicle and the driver journals the update

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The frontend shows the winner's Start Chronicle decision

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala:278-317`
- Test: `frontend/src/test/scala/distantlands/frontend/` (the suite that covers `viewerPresentation`; find it with `grep -rl viewerPresentation frontend/src/test`)

**Interfaces:**
- Consumes: a projection with `oathkeeper.winnerPlayerId` set **and** `walkerDecision` (owner) or `walkerWaiting` (others) present.
- Produces: `viewerPresentation` returns `showGameplayControls = true` for the owner, and `waitingForPlayerId = Some(winner)` for everyone else, whenever a walk is parked, even after a winner exists.

- [ ] **Step 1: Write the failing tests**

Using the suite's existing projection builders:

```scala
test("the winner sees controls for a parked Start Chronicle decision"):
  val presentation = ServerUiSupport.viewerPresentation(projectionWithWinnerAndDecision(owner = p2), p2)
  assert(presentation.showGameplayControls)

test("other players see who the Chronicle is waiting on"):
  val presentation = ServerUiSupport.viewerPresentation(projectionWithWinnerAndWaiting(on = p2), p1)
  assertEquals(presentation.waitingForPlayerId, Some(p2))

test("a finished game with nothing parked still hides every control"):
  val presentation = ServerUiSupport.viewerPresentation(projectionWithWinnerOnly(), p1)
  assert(!presentation.showGameplayControls)
```

- [ ] **Step 2: Run to see the failure**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.*ServerUiSupport*"` (use the real suite name)
Expected: the first two FAIL.

- [ ] **Step 3: Let a parked walk through**

At the first branch (`:283-286`) change the condition so it only fires when nothing is parked:

```scala
if value.oathkeeper.exists(_.winnerPlayerId.nonEmpty)
    && value.walkerDecision.isEmpty && value.walkerWaiting.isEmpty then
  return ViewerPresentation(showGameplayControls = false, waitingForPlayerId = None,
    waitingForDisplayName = None, playerId = playerId)
```

Keep the existing return-value shape of that branch.

- [ ] **Step 4: Check the status line and banner**

`ActionDecisionRenderer.status` already shows the waiting heading ("Waiting for {winner}: Start Chronicle") and the winner banner. Add one assertion to the existing renderer suite that a waiting viewer sees `"Waiting for"` and `"Start Chronicle"`.

- [ ] **Step 5: Run the frontend suite**

Run: `./sbtw frontend/test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala frontend/src/test
git commit -m "feat(frontend): let a parked decision through after the game has a winner

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Server round trip, docs and full verification

**Files:**
- Test: `T/application/GameApplicationServiceStreamSuite.scala` (or the nearest service-level suite that can drive a command)
- Modify: `docs/rules/implementation-traceability.md` (Chronicle rows), `CONTEXT.md`, `docs/architecture/game-setup.md`, `docs/ROADMAP.md`

- [ ] **Step 1: Service-level end-to-end test**

Through `GameApplicationService` (`Table.start.service(...)`-style, with `result` arranged), assert: the projection's `phase` is `"game-over"` with a `walkerDecision` for the winner and `walkerWaiting` for the others; `ResolveWalker` from the winner succeeds; a second `ResolveWalker` is refused; reloading the stream (recover) reproduces the state with `evolved.isDefined`.

Run: `./sbtw "testOnly oathdigital.application.GameApplicationServiceStreamSuite"`
Expected: PASS.

- [ ] **Step 2: Update docs**

- `CONTEXT.md`: add **Baseline** (the Chronicle a game started from, kept on `CampaignState`), **Chronicle Phase** (the post-game tasks, run as walker procedures), **Evolved Chronicle** (what `ChronicleUpdated` records).
- `docs/architecture/game-setup.md`: replace "campaign restoration ... excluded" sentences; state that game start keeps the baseline and takes `era` and `oathkeeperGoal` from it.
- `docs/rules/implementation-traceability.md`: update "Campaign-state representation" and "Full campaign serialization" rows with the real evidence paths (`GameStartRules`, `ChronicleBeginSuite`, `GameEventFactsWireSuite`), status `Partial`.
- `docs/ROADMAP.md`: keep "End-of-game Chronicle steps" unchecked; add under it the sub-bullet "Foundation landed: Begin decision, driver chaining, baseline, `ChronicleUpdated`; remaining: Sun, Throne, World, Beacon, Stars."

- [ ] **Step 3: Run everything**

```bash
./sbtw test
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git diff --check
```

Expected: all pass; coverage ratchet holds.

- [ ] **Step 4: Commit**

```bash
git add CONTEXT.md docs
git commit -m "docs: record the Chronicle foundation in the architecture notes and traceability

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-review

- **Spec coverage:** required `era`/`oathkeeperGoal` (Task 2); baseline kept (3); `Phase.Chronicle`, refs, `ChronicleUpdated` (4); winner-only Begin, driver chaining, "only once", exit event, replay (5); frontend "Waiting on" (6); docs, traceability, end-to-end (7); rules rewrite with actors and rulings (1). Deferred to the next plan by design: Sun, Throne and their windows, fake-power window tests, scorepad awards, shuffle extension, World, Beacon, Stars, derived edifice deck and suited reserves (arrive with the tasks that read them).
- **Known adaptation points:** Tasks 2, 5 and 6 name helper and constructor shapes from a code survey (`requiredField`, `safeIntField`, `startTriggered`, `DecisionOption` for a button). Each step says which file to read and match; `-Werror` will flag any mismatch.
- **Design choice:** `ChronicleUpdated` is a core operation recorded in the `ChronicleFinish` walker step, not a standalone event, so it replays through the normal walker path and needs no lifecycle codec. The spec's "journaling `ChronicleUpdated(chronicle)`" still holds.
