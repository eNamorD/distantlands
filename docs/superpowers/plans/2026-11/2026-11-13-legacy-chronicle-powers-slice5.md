# Legacy Chronicle Powers, Slice 5: Royal Line Implementation Plan

> Executed inline by the implementer.

**Goal:** Royal Line chooses the winner of an Empire win. When the game
ended in a Chancellor's win or a Successor's win, and a Citizen held Royal
Line active when it ended, that Citizen chooses, in a new first step of
the Sun, which Imperial wins: any Citizen, who then wins as the Successor,
or the Chancellor, which is choosing no one, so the Chancellor wins by the
Empire win that ended the game. A new `ChronicleOperation` journals the
new winner, and every later step, the result display and the Game Log
read the winner from the game's result. The card counts as implemented,
and the ROADMAP's "Legacy powers: Chronicle" item is ticked.

**Architecture:** The seam carries on. A `Transform` on a new, empty
`ChronicleStartOfSun` window appends one `Branch` of constant shape, read
when reached: `ChronicleLegacyRules.holder(state, RoyalLineCard.id,
Set(Citizen))` and the result's kind decide whether it asks. The answer
journals `WinnerNamed(winner, kind)`, which rewrites the game's result.
The Sun no longer captures the winner when its walk is built: Crown
Chancellor and Assign Tasks read it when each is reached, as every later
procedure already does through `ChronicleTasks.winner`.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-4, 22 and 23 (owner-ruled: the holder chooses the winner from
every Imperial player, the Chancellor included), and section "Slice 5:
Royal Line".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)
R1-R36. Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Royal Line L10). `docs/rules/chronicle.md`, the Sun; the Empire endings
in `src/main/scala/distantlands/gameplay/EmpireEndings.scala` and their
rulings in `docs/rules/ambiguities.md` ("Empire rulings").

## Prerequisite

Slice 4 has landed (through `897cf25f8`). Check before Task 1:

```bash
grep -q "object IronHand" src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala &&
grep -q "def builders" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "def chancellorWins" src/main/scala/distantlands/gameplay/EmpireEndings.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R37-R42 ([rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)):

- **R37** (Task 1, low confidence): the Chancellor's Empire win is derived
  from the ended game's state by `EmpireEndings.empireWin`, which reuses
  the code that decided the ending: `rollsEndDie` and the kinds
  `stableRegime` and `warExhaustion` pass to `chancellorWins`, with the
  Successor substitution skipped. It is tested against each ending the
  engine produces.
- **R38** (Task 3): the owner's shape: every Imperial player, the
  Chancellor first, and no Decline; choosing the current winner keeps the
  result and journals nothing.
- **R39** (Task 1): `WinnerNamed(winner, kind)`, wire kind
  `"winner-named"`, fields `"winner"` and `"victory"`.
- **R40** (Task 2): `ChronicleStartOfSun` (`chronicle.sun.start-of-sun`).
- **R41** (Tasks 3 and 4): "p3 named p2 the winner with Royal Line", only
  when the winner changes.
- **R42** (Task 5): tick "Legacy powers: Chronicle".

The Sun reading the winner live is accepted, with an audit: every
Chronicle step and power that reads the winner reads it from the result
when reached (Task 2 lists them), and a test checks a Royal Line change
reaches the Crown, Reformer and Score Lineage Points (Task 3).

## Deviations from the spec

- `ChronicleSunProcedure.build` no longer passes the winner into the
  Sun's steps: Crown Chancellor and Assign Tasks read it from the result
  when reached (a `Branch` each), as the design's "every later step reads
  the winner from the game's result" requires. The build still refuses a
  game with no result.
- The Royal Line test of the Chancellor named in a Successor's place
  expected Stable Regime, but the fixture ends at War Exhaustion with no
  Imperial Oathkeeper, which gives exhaustion. The test now checks both
  cases: no Oathkeeper gives Chancellor Exhaustion, an Imperial
  Oathkeeper Stable Regime.
- The ROADMAP's convention drops landed items; R42 says tick, so the
  item is ticked and reworded as landed.

## Global Constraints

- Scala 3 with `-Werror`; every Scala file at or under 800 lines.
- `gameplay` never reads a catalog `.text` or `rulesText`.
- A new test names cards by their card objects.
- Royal Line's holder and their role are read when its window is reached,
  which is before the Crown, so a Citizen then was a Citizen when the game
  ended (rulings 1, 2 and 22).
- No two decisions share an id (ruling 3).
- Old saved streams still decode: `WinnerNamed` is a new wire kind.
- R15: the new window and the Sun's restructured Crown and Assign Tasks
  shift a walk parked mid-Sun by an earlier build.
- TDD, one commit per task, test results read before every commit;
  coverage at or above 92% statements and 84.4% branches, from a fresh
  `./sbtw clean coverage test coverageReport` whose report lines are
  quoted; snapshots and goldens reviewed and committed in their task;
  `git status --short src/test/resources` otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; stage only
  this plan's files; `git log -3` after each commit.

---

### Task 1: `WinnerNamed` and the Empire win

**Files:**
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
  `ChronicleStateOperations.scala` (`winnerNamed`)
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Modify: `src/main/scala/distantlands/gameplay/EmpireEndings.scala`
  (`empireWin`)
- Test: `ChronicleStateOperationsSuite`, `ChronicleOperationWireSuite`,
  `WalkerOperationWireSuite`, `src/test/scala/distantlands/gameplay/EmpireEndingsSuite.scala`

**Interfaces:**
- Produces: `WinnerNamed(winner: PlayerId, kind: VictoryKind)`;
  `EmpireEndings.empireWin(ready: ReadyGame): VictoryKind`, the
  Chancellor's win the ended game gave.

- [ ] **Step 1: Write the failing tests**

`ChronicleStateOperationsSuite`:

```scala
  test("a named winner replaces the game's result"):
    val ended = Table.start.imperial(p1, p2).update(_.updateCurrent(_.copy(
      result = Some(GameResult(p1, VictoryKind.StableRegime))))).ready
    val after = accepted(run(ended, WinnerNamed(p2, VictoryKind.Successor)))
    assertEquals(after.game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))

  test("a winner is named only for a game with an Empire result, a seated " +
      "player and an Empire kind"):
    val ended = Table.start.imperial(p1, p2).update(_.updateCurrent(_.copy(
      result = Some(GameResult(p1, VictoryKind.StableRegime))))).ready
    rejected(run(Table.start.ready, WinnerNamed(p2, VictoryKind.Successor)))
    rejected(run(ended, WinnerNamed(PlayerId("p9"), VictoryKind.Successor)))
    rejected(run(ended, WinnerNamed(p2, VictoryKind.Usurper)))
```

`ChronicleOperationWireSuite`: `WinnerNamed(PlayerId("red"),
VictoryKind.Successor)` round-trips as `{"kind": "winner-named",
"winner": "red", "victory": "successor"}`, and an unknown victory key
decodes to a typed `WireError` at `.victory` (the field is named
`victory`, since `kind` is the operation's own discriminator).
`WalkerOperationWireSuite.everyOperation` gains
`WinnerNamed(PlayerId("red"), VictoryKind.StableRegime)`.

`EmpireRoundEndSuite`, against each Empire ending the engine produces
(R37), on the suite's own `roundEnd`, `ended` and `replayed`:

```scala
  /** The game `ready`'s round end ends, through the engine, with the
    * Empire win `empireWin` reads back. */
  private def empireWinAfter(ready: ReadyGame, random: RoundEndRandomPort)
      : (Option[GameResult], VictoryKind) =
    val after = Outcomes.ready(accepted(replayed(ready, ended(ready, random))))
    (after.game.current.result, EmpireEndings.empireWin(after))

  test("the Empire win read back after each end-die ending (R37)"):
    Vector(5 -> 6, 6 -> 5, 7 -> 3).foreach((round, face) =>
      assertEquals(empireWinAfter(roundEnd(round), RoundEndChoices.endDie(face)),
        (Some(GameResult(p1, VictoryKind.StableRegime)),
          VictoryKind.StableRegime), clue(round)))

  test("the Empire win read back after each round-eight ending (R37)"):
    // An Imperial Oathkeeper gives Stable Regime; an Exile's gives
    // Chancellor Exhaustion.
    assertEquals(empireWinAfter(roundEnd(8), RoundEndChoices.endDie(1)),
      (Some(GameResult(p1, VictoryKind.StableRegime)),
        VictoryKind.StableRegime))
    assertEquals(empireWinAfter(roundEnd(8, holder = Some(p3)),
      RoundEndChoices.endDie(1)),
      (Some(GameResult(p1, VictoryKind.ChancellorExhaustion)),
        VictoryKind.ChancellorExhaustion))

  test("a Successor's result still reads back the Chancellor's Empire win " +
      "(R37)"):
    val atEight = Table.from(roundEnd(8, holder = None))
      .darkestSecret(Some(p2), secrets = 1).ready
    assertEquals(empireWinAfter(atEight, RoundEndChoices.endDie(1)),
      (Some(GameResult(p2, VictoryKind.Successor)),
        VictoryKind.ChancellorExhaustion))
    val onTheDie = Table.from(roundEnd(7)).darkestSecret(Some(p2),
      secrets = 1).ready
    assertEquals(empireWinAfter(onTheDie, RoundEndChoices.endDie(3)),
      (Some(GameResult(p2, VictoryKind.Successor)), VictoryKind.StableRegime))
```

(If the Successor goal under the suite's Oathkeeper goal is not the
Darkest Secret at round seven, give p2 whatever the existing round-eight
Successor test gives them.)

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: WinnerNamed`.

- [ ] **Step 3: Implement**

```scala
/** Royal Line's choice of an Empire win's winner (Legacy Chronicle powers
  * design, rulings 22 and 23): the game's result becomes `winner`'s win of
  * `kind`, `Successor` for a Citizen and the ended game's Empire win for
  * the Chancellor. A game with no result, a winner not seated and a kind
  * that is not an Empire win are refused. */
final case class WinnerNamed(winner: PlayerId, kind: VictoryKind)
    extends ChronicleOperation, LeafOperation
```

`ChronicleStateOperations.winnerNamed` checks the three refusals and sets
`current.result = Some(GameResult(winner, kind))`. The codec writes and
reads `winner` and `victory` (`VictoryKind.key`, decoded as the ending
codec decodes it). `EmpireEndings`: the kinds the endings pass to `chancellorWins` become
two named members the endings use, and `empireWin` reads the same
predicate the round end used to pick the ending:

```scala
  /** The Chancellor's win on the end die. */
  val endDieWin: VictoryKind = VictoryKind.StableRegime

  /** The Chancellor's win at War Exhaustion: an Imperial Oathkeeper's
    * Stable Regime, otherwise exhaustion. */
  def exhaustionWin(ready: ReadyGame): VictoryKind =
    if imperialOathkeeper(ready) then VictoryKind.StableRegime
    else VictoryKind.ChancellorExhaustion

  /** The Chancellor's win the ended game gave, before any Successor took
    * it: the same predicate and kinds the round end used (`rollsEndDie`,
    * `stableRegime`, `warExhaustion`), with the Successor substitution of
    * `chancellorWins` skipped. Royal Line hands it back to the Chancellor
    * (Legacy Chronicle powers R37). */
  def empireWin(ready: ReadyGame): VictoryKind =
    if rollsEndDie(ready) then endDieWin else exhaustionWin(ready)
```

`stableRegime` passes `endDieWin`; `warExhaustion` passes
`exhaustionWin(ready)` in both of its Chancellor branches.

- [ ] **Step 4: Run the tests to verify they pass**

Run the four suites, then `./sbtw test`, and read the results: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(chronicle): journal a named winner and read the Empire's win

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The Sun reads the winner when each step is reached

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
  (`build`, `steps`, the Crown and Assign Tasks windows)
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleSunSuite.scala`

- [ ] **Step 1: Write the failing test**

```scala
  test("Crown Chancellor and Assign Tasks read the winner when reached"):
    // A power at the Sun's first window that names p2 the winner; the
    // Crown and the Throne task then go to p2.
    final case class NamesP2() extends ContributingPower:
      val id: PowerId = PowerId("test.chronicle.names-p2")
      val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
      val contributions: Map[PowerWindow, Vector[Contribution]] = Map(
        PowerWindow.ChronicleStartOfSun -> Vector(Transform((_, ops) =>
          ops :+ BuildOps((_, _) => Right(Vector(
            WinnerNamed(p2, VictoryKind.Successor))), required = true))))
    val table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2)
      .update(_.updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.StableRegime)))))
    val custom = new OathRules(catalog, walkerPowerCatalog =
      WalkerPowers(Vector(NamesP2())), walkerDice =
      WalkerDiceFixture.reversingShuffle)
    val run = ChronicleWalk.through(custom, table,
      TriggeredProcedureRef.ChronicleSun)
    assertEquals(Imperials.chancellor(run.ready), Some(p2))
    assertEquals(run.ready.game.current.chronicleTasks
      .get(ChronicleTask.Throne), Some(p2))
```

(The window lands in Task 3; this test, written now, needs
`ChronicleStartOfSun` to exist, so this task adds the empty window to
`PowerWindow`, `ChronicleWindows.sun` and the Sun's steps, with the
`ChronicleWindowsSuite` key list gaining `"chronicle.sun.start-of-sun"`
first.)

- [ ] **Step 2: Run the test to verify it fails**

Expected: FAIL: p1, captured when the walk was built, is crowned.

- [ ] **Step 3: Implement**

`build` keeps `ChronicleTasks.winner(ready)` as a refusal only and calls
`steps(catalog)`. The Crown window's children become one `Branch` reading
`ChronicleTasks.winner(state)` (a `Left` is `ChronicleTasks.failing`) and
returning today's Crown nodes for that winner; the Assign Tasks window's
children the same over `ChronicleTaskAssignment.steps(winner, tookNote)`.
`brokeTheEmpire` takes the winner the Branch read. Each `Branch`'s shape
reads only the result, which nothing in the Crown's own window changes,
so a walk resumed inside it finds the same nodes.

The audit (each reads the result when reached, so needs no change):
Reformer's "if you won" (`ChronicleTasks.winner(state)` in its step's
`Branch`); Score Lineage Points (`LineageScoring.steps`' `Branch`); the
Throne holder, from Assign Tasks' journaled task; the Grand Scepter goal's
"if none, win" (`LegacyGoals`, read per question); the Stars' and the
extraction's winner-ruled sites (`ChronicleExtraction.layout`, read at the
Stars and the Finish), with Iron Hand's holder in the winner's place;
Swear an Oath (the result's kind and Visions); the reactivation and the
cap at the Crown (inside the Crown's `Branch`). Write the list in the
Sun's doc comment.

- [ ] **Step 4: Run the tests to verify they pass**

Run `./sbtw test` and read the result: PASS; no golden changes (the
journaled events are the same).

- [ ] **Step 5: Commit**

```bash
git commit -m "refactor(chronicle): the Sun reads the winner when each step is reached

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Royal Line

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/RoyalLine.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `RoyalLineCard`), `ProcedureLog.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/RoyalLineSuite.scala`
- Create: `src/test/scala/distantlands/application/RoyalLineProjectionSuite.scala`
- Modify (imports): tests importing `RoyalLineCard` from holding
- Modify: the two snapshots

**Interfaces:**
- Produces: `RoyalLineCard`; `RoyalLine` (power);
  `RoyalLine.decisionId(player)` =
  `s"chronicle.sun.legacy.royal-line.${player.value}"`;
  `RoyalLine.named: NoteKey` ("{0} named {1} the winner with {2}").

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.{ChronicleWalk, LineageScoring}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Royal Line (L10): "If you are a Citizen: Chronicle (Sun): You may choose
  * which Imperial meets the Successor goal. Ignore the printed goal. You
  * may choose no one." (rulings 22 and 23; R37-R42). p1 is the Chancellor,
  * p2 and p3 Citizens. */
class RoyalLineSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** The game ended in `result`, p3 holding Royal Line active. */
  private def ended(result: GameResult, holder: PlayerId = p3): Table =
    ChronicleWalk.chronicleWonByP1.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result = Some(result))))
      .legacy(holder, RoyalLineCard)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers)

  private def naming(player: PlayerId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == RoyalLine.decisionId(p3) =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(player))

  private def asked(run: ChronicleWalk.Run): Option[Decide] =
    run.asked.find(_.decisionId == RoyalLine.decisionId(p3))

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.sunNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun), keys,
      run.events).filter(said => keys.exists(_.name == said.key)).map(_.text)

  private val stable = GameResult(p1, VictoryKind.StableRegime)

  test("Royal Line is implemented"):
    assert(PowerImplementationStatus.implemented(RoyalLineCard.power.id))

  test("the holder chooses from every Imperial, the Chancellor first, with " +
      "no Decline (ruling 22; R38)"):
    val decide = asked(through(ended(stable),
      TriggeredProcedureRef.ChronicleSun)).get
    assertEquals(decide.owner, p3)
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(p1, p2, p3).map(DecisionOptionRef.Player(_)))

  test("naming a Citizen makes them the Successor, whom the Crown and the " +
      "tasks follow (ruling 23)"):
    val run = through(ended(stable), TriggeredProcedureRef.ChronicleSun,
      naming(p2))
    assertEquals(run.ready.game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))
    assertEquals(Imperials.chancellor(run.ready), Some(p2))
    assertEquals(run.ready.game.current.chronicleTasks
      .get(ChronicleTask.Throne), Some(p2))
    assert(lines(run).contains(
      s"p3 named p2 the winner with ${RoyalLineCard.id.value}"))

  test("naming the Chancellor in a Successor's place hands back the " +
      "Empire's win the game ended in (ruling 23; R37)"):
    // Round 1, no end die: War Exhaustion, whose Chancellor's win is Stable
    // Regime with an Imperial Oathkeeper and exhaustion otherwise.
    val successor = GameResult(p2, VictoryKind.Successor)
    Vector(None -> VictoryKind.ChancellorExhaustion,
        Some(p2) -> VictoryKind.StableRegime).foreach((oathkeeper, kind) =>
      val run = through(ended(successor).oathkeeper(oathkeeper),
        TriggeredProcedureRef.ChronicleSun, naming(p1))
      assertEquals(run.ready.game.current.result, Some(GameResult(p1, kind)),
        oathkeeper)
      assertEquals(Imperials.chancellor(run.ready), Some(p1)))

  test("naming the current winner keeps the result, journals nothing and " +
      "writes nothing (R38, R41)"):
    val kept = through(ended(stable), TriggeredProcedureRef.ChronicleSun,
      naming(p1))
    assertEquals(kept.ready.game.current.result, Some(stable))
    assert(!distantlands.testkit.RecordedOps.all(kept.events).exists(
      _.isInstanceOf[WinnerNamed]))
    assertEquals(lines(kept).filter(_.contains(" named ")), Vector.empty)
    // So does the plainest answer, the Chancellor in a Chancellor's win.
    val plain = through(ended(stable), TriggeredProcedureRef.ChronicleSun)
    assertEquals(plain.ready.game.current.result, Some(stable))

  test("not asked on an Exile's or a Vision's win"):
    Vector(GameResult(p1, VictoryKind.Usurper),
        GameResult(p1, VictoryKind.Visionary)).foreach(result =>
      assertEquals(asked(through(ended(result),
        TriggeredProcedureRef.ChronicleSun)), None, result))

  test("not asked when its holder was an Exile at game end (ruling 22)"):
    val table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2)
      .update(_.updateCurrent(_.copy(result = Some(stable))))
      .legacy(p3, RoyalLineCard)
    assertEquals(asked(through(table, TriggeredProcedureRef.ChronicleSun)),
      None)

  test("a named winner reaches the Crown, Reformer and Score Lineage " +
      "Points (the audit)"):
    // p2 holds Reformer; the Empire rules Ancient City and Broken Peaks,
    // and Village Idiot lies at Ancient City, so Reformer has a move.
    val table = ended(stable).warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(VillageIdiotCard, at = AncientCitySite)
      .legacy(p2, ReformerCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone,
      naming(p2))
    assertEquals(Imperials.chancellor(run.ready), Some(p2))
    assert(run.asked.exists(_.decisionId == Reformer.pickId(p2)))
    val lineage = run.ready.game.current.player(p2).get.lineage
    assert(run.ready.game.campaign.era.score(lineage) >=
      LineageScoring.basePoints)

  test("a walk resumed at Royal Line's question applies the answer"):
    val table = ended(stable)
    val started = ChronicleWalk.started(rules, table,
      TriggeredProcedureRef.ChronicleBegin)
    val parked = ChronicleWalk.walkedTo(rules, started, RoyalLine.decisionId(p3))
    val decide = ChronicleWalk.parked(parked.state).get
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    val answered = accepted(rules.resolveWalker(replayed, p3,
      RoyalLine.decisionId(p3), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Player(p2))))
    assertEquals(distantlands.testkit.Outcomes.ready(answered.state).game
      .current.result, Some(GameResult(p2, VictoryKind.Successor)))
```

`RoyalLineProjectionSuite` (package `distantlands.application`): after
the Sun of `ended(stable)` with p3 naming p2 (walked with
`ChronicleWalk.started` and `ChronicleWalk.walkedTo` and answered), the
projection for every viewer and the public view has
`oathkeeper.winnerPlayerId == Some("p2")` and `winnerVictoryKind ==
Some("successor")`:

```scala
package distantlands.application

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The result display follows Royal Line's named winner (Legacy Chronicle
  * powers design, ruling 23). */
class RoyalLineProjectionSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val projector = new GameProjector(catalog)

  test("every viewer's result shows the named winner as the Successor"):
    val table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.StableRegime)))))
      .legacy(p3, RoyalLineCard)
    val started = ChronicleWalk.started(rules, table,
      TriggeredProcedureRef.ChronicleBegin)
    val parked = ChronicleWalk.walkedTo(rules, started,
      RoyalLine.decisionId(p3))
    val named = accepted(rules.resolveWalker(parked.state, p3,
      RoyalLine.decisionId(p3), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Player(p2))))
    val loaded = LoadedGame(named.state, 1L)
    (Vector(p1, p2, p3).map(projector.project("royal", loaded, _)) :+
        projector.projectPublic("royal", loaded)).foreach { view =>
      val oathkeeper = view.oathkeeper.get
      assertEquals((oathkeeper.winnerPlayerId, oathkeeper.winnerVictoryKind),
        (Some(p2.value), Some(VictoryKind.Successor.key)))
    }
```

(`resolveWalker` is reachable from `application`; if it is not, walk the
answer through a `ChronicleWalk` helper as `started` does.)

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: RoyalLineCard` in the new package.

- [ ] **Step 3: Implement**

`RoyalLine.scala`: move `RoyalLineCard` verbatim, then:

```scala
/** Royal Line (legacy L10): "If you are a Citizen: Chronicle (Sun): You may
  * choose which Imperial meets the Successor goal. Ignore the printed goal.
  * You may choose no one." Its step is the Sun's first window (Legacy
  * Chronicle powers design, ruling 22; R39). Only after a Chancellor's or a
  * Successor's win, a seated Citizen holding it active, read when reached,
  * chooses any Imperial, the Chancellor first, with no Decline (R38). A Citizen
  * then wins as the Successor; the Chancellor wins by the ended game's
  * Empire win (`EmpireEndings.empireWin`, R37). `WinnerNamed` journals a
  * changed winner (ruling 23); every later step reads it from the result.
  * Line: "p3 named p2 the winner with Royal Line" (R40). */
case object RoyalLine extends ContributingPower:
  val id: PowerId = RoyalLineCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfSun -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.sun.legacy.royal-line.${player.value}"

  private val empireWins: Set[VictoryKind] = Set(VictoryKind.Successor,
    VictoryKind.StableRegime, VictoryKind.ChancellorExhaustion)

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val named: NoteKey = notes.key("royal-line-named", Vector(NotePart.Arg(0),
    NotePart.Text(" named "), NotePart.Arg(1), NotePart.Text(" the winner with "),
    NotePart.Arg(2)))
  def sunNoteKeys: Vector[NoteKey] = notes.declared

  val step: Operation = Branch((state, _) =>
    (for
      result <- state.game.current.result if empireWins(result.kind)
      holder <- ChronicleLegacyRules.holder(state, RoyalLineCard.id,
        Set(Role.Citizen))
      chancellor <- Imperials.chancellor(state)
    yield Vector(
      Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
        ChronicleTasks.playerOptions(chancellor +:
          Imperials.players(state).filterNot(_ == chancellor)),
        heading = Some("Royal Line: you may choose the winner"),
        confirmVerb = Some("Name"))),
      Branch((live, pending) => ChronicleTasks.chosenPlayer(pending,
          decisionId(holder)).toOption.toVector.flatMap(chosen =>
        val kind = if chosen == chancellor then
          if result.kind == VictoryKind.Successor then
            EmpireEndings.empireWin(live) else result.kind
          else VictoryKind.Successor
        if GameResult(chosen, kind) == result then Vector.empty
        else Vector(BuildOps((_, _) => Right(Vector(WinnerNamed(chosen, kind))),
            required = true),
          Note(noteId, _ => Some(named.sourceless(NoteArg.Player(holder),
            NoteArg.Player(chosen), NoteArg.Card(RoyalLineCard.id))))))))
    ).getOrElse(Vector.empty))
```

`LegacyChroniclePowers` (`all`, `sunNoteKeys`), `ProcedureLog`
(`LogKind.Decision`) as before. The `GameResult` captured in the outer
`Branch` is read when the question is reached; the result does not change
before the answer, so a resumed walk finds the same options.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Rewrite the unimplemented and note-keys snapshots (diffs:
`legacy.royal-line` removed; `royal-line-named` added). Run `./sbtw test`
and read the result: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Royal Line names the winner of an Empire win

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: A golden log for Royal Line

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
  (`chronicleRoyalLine`, `named`)
- Create: `src/test/resources/gamelog/chronicle-royal-line.actor.log`,
  `chronicle-royal-line.other.log`

- [ ] **Step 1: Write the script**

In `ChronicleLogScripts`, registered in `named`:

```scala
  /** An Empire game that ends at round eight: p1 is the Chancellor and the
    * Oathkeeper, p2 and p3 Citizens, and p3 holds Royal Line. p3 rests last
    * in the round, the Empire wins (Stable Regime, or a Successor's win if
    * a Citizen meets the goal), and in the Sun p3 names p2 the winner
    * (Legacy Chronicle powers, slice 5). */
  def chronicleRoyalLine(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-royal-line", Table.start
      .imperial(p1, p2, p3)
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Oathkeeper)
      .roundMarkerAt(8).legacy(p3, RoyalLineCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == RoyalLine.decisionId(p3) =>
        ChooseOneAnswer(DecisionOptionRef.Player(p2))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-royal-line", service, p1)
```

- [ ] **Step 2: Write and review the goldens**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`,
then `./sbtw "testOnly distantlands.application.gamelog.*"`, and read the
results. Read both files: the game's end line names the Chancellor's win;
the Sun opens with "p3 named p2 the winner with Royal Line"; then "p2
became Chancellor" and p2 takes the Throne task; Score Lineage Points
scores p2. No existing golden changes.

- [ ] **Step 3: Commit**

```bash
git commit -m "test(gamelog): a golden Chronicle where Royal Line names the winner

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: Docs, the UI check and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (the Sun: a Digital note before step
  1), `docs/rules/ambiguities.md` (design rulings 22-23 and this slice's
  implementation rulings), `docs/rules/implementation-traceability.md`
  (Sun row), `docs/ROADMAP.md` (tick "Legacy powers: Chronicle"; the setup
  and in-play items keep the other halves), the design's status line.

- [ ] **Step 1: Docs**, as slice 4's Task 6.

- [ ] **Step 2: The running app.** Royal Line's question is a choose-one
  of a button and players, kinds the app shows; the result banner reads
  the projection. Reaching it in the running app needs a game ended in an
  Empire win with a Citizen holding Royal Line, which no fixture offers;
  if the server can load the golden script's stream, open it and quote
  the banner's text, and otherwise say so in the report.

- [ ] **Step 3: Run the verification gate**, in the background, from a
  fresh `./sbtw clean coverage test coverageReport`, quoting its
  "Statement coverage" and "Branch coverage" lines; then
  `./sbtw "frontend/test"`, `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`, `git status --short
  src/test/resources`, reading every result.

- [ ] **Step 4: Commit**

```bash
git commit -m "docs(legacies): record Royal Line and close the Chronicle powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 5: Royal Line"):
- The Royal Line window: Tasks 2 and 3; the winner operation: Task 1;
  every step and the presentation reading the winner from the result:
  Task 2 (the Sun) and Task 3 (the projection test); Royal Line leaves
  holding: Task 3.
- Tests the spec lists: naming a different Citizen, and the Chancellor;
  not asked on an Exile's or Vision win, nor when its holder was an Exile
  at game end; the Crown, Assign Tasks and Score Lineage Points following
  the new winner (Task 3); the result display (Task 3) and log naming the
  new winner, and a golden log (Task 4). A resume test (Task 3).
- Docs and the ROADMAP tick: Task 5.

**Placeholder scan:** every step's code is written out. Task 1 and Task 4
use `Table.roundMarkerAt`, the testkit's round setter.

**Type consistency:** `WinnerNamed(PlayerId, VictoryKind)`;
`EmpireEndings.empireWin(ReadyGame): VictoryKind`; `RoyalLine.decisionId`,
`RoyalLine.named`; `PowerWindow.ChronicleStartOfSun`;
`EmpireEndings.endDieWin` and `exhaustionWin`.
