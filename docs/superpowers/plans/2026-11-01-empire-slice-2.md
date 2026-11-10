# Empire Slice 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Empire's endings. In a game with a Chancellor, the end of
rounds 5, 6 and 7 rolls the purple end die while an Imperial player holds
the Oathkeeper title, and round 8 resolves War Exhaustion in the Empire's
four steps. Whenever the Chancellor would win, a Citizen who meets the
Successor goal wins instead.

**Architecture:**
- A new rules object, `EmpireEndings`, answers every question about these
  endings from state alone:
  - whether this round's end rolls the die;
  - what a face gives;
  - War Exhaustion's winner;
  - who the Successor is.
- `StateBasedEvaluation.endRound` asks it when the game has an Empire. The
  no-Empire path is unchanged.
- The die's face comes from the round-end random port, which already picks
  War Exhaustion's random winner. The port is renamed `RoundEndRandomPort`
  and gains `endDie()`.
- A new event, `EndDieRolled(round, face, result)`, journals the roll and
  the win it gives. Replay recomputes the win from the face and refuses a
  mismatch, as it already does for `WarExhaustionResolved`.
- `VictoryKind` gains `StableRegime`, `ChancellorExhaustion` and
  `Successor`. The Game Log and the victory screen name them.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 2: Empire endings", "Rulings" and
"Testing").

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/empire-slice-2` on branch `empire-slice-2`, off
    `0.2.0-alpha`. Never work in the main checkout.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and
    `node_modules` into the worktree.
  - Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused
  values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of
    at least 92%
  - `./sbtw frontend/test`
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines
    and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Imperial roles:** game start keeps refusing them. This slice is reached
  only through test tables (`Table.imperial`).
- **Empire:** a game has an Empire while a Chancellor is seated
  (`Imperials.chancellor(ready).nonEmpty`). The no-Empire round end must
  stay exactly as it is.
- **Tests name cards, not ids:** `BackendArchitectureSuite` refuses
  spelled-out catalog ids in tests. Use names (`"Yew Staff"`) or card
  objects (`TheGrandScepterCard.id`).
- **Old journals:** they need not decode (no compatibility reader).

## Rulings for this plan

- **R17, the Grand Scepter counts (user ruling, 2026-11-01).** Under
  Supremacy, the Successor "holds more relics than each Imperial" (card
  OATH-265). The Grand Scepter is a relic, so it counts in its holder's
  total.
- **Which kind each Chancellor win takes.**
  - A win on the end die is `StableRegime`.
  - War Exhaustion's first step is printed "Stable Regime", so it is
    `StableRegime` too.
  - Its fourth step is `ChancellorExhaustion`.
  - A Citizen who takes any of these wins records `Successor`.
- **Round end after a winning roll.** A winning roll ends the game at once:
  `EndDieRolled` carries the result, and no `RoundEnded` follows. A losing
  roll is followed by `RoundEnded(round, Some(round + 1))`, as today.
- **Known limit: a missing roll.** A journal that leaves out an
  `EndDieRolled` before `RoundEnded` still replays. The round's end leaves
  no state that would show the roll was owed, and journals are written only
  by the server. Record this in the traceability row.

## File map

| File | Change |
|---|---|
| `gameplay/phases/rest/RoundEndRandomPort.scala` | renamed from `WarExhaustionRandomPort.scala`; gains `endDie()` |
| `model/GameState.scala` | three `VictoryKind` cases |
| `model/GameEventProtocol.scala` | `EndDieRolled` |
| `gameplay/EmpireEndings.scala` | new: thresholds, Stable Regime, War Exhaustion, Successor |
| `gameplay/StateBasedEvaluation.scala` | Empire branch at round end; `visionaryWin` shared; `EndDieRolled` replay |
| `gameplay/OathRules.scala`, `OathDriver.scala`, `phases/rest/TurnBoundary.scala` | port rename; route `EndDieRolled` |
| `serialization/EndingEventCodec.scala`, `GameEventWire.scala` | new kinds and the new event |
| `application/gamelog/GameLogFormatter.scala` | the die line and the new win words |
| `application/GameApplicationService.scala` | port rename |
| `frontend/VictoryWords.scala` | new: the win's name on the victory screen |
| `frontend/ActionDecisionRenderer.scala` | uses `VictoryWords` |
| test kit: `Table`, `Situation`, `SituationDriver`, `RoundEndChoices` | port rename and die stubs |
| docs | ambiguities R17, traceability rows 117, 119 and 141, spec status and rulings, ROADMAP |

All `src` paths are under `src/main/scala/distantlands/`, and frontend paths
are under `frontend/src/main/scala/distantlands/`.

## Pre-flight

- [ ] Create the worktree and link the tooling:

```bash
cd /Users/roman/projects/oathdigital
git worktree add .worktrees/empire-slice-2 -b empire-slice-2 0.2.0-alpha
ln -s ../../.tooling .worktrees/empire-slice-2/.tooling
ln -s ../../node_modules .worktrees/empire-slice-2/node_modules
```

All later commands run inside `.worktrees/empire-slice-2`.

---

### Task 1: The round-end random port rolls the end die

**Files:**
- Rename: `src/main/scala/distantlands/gameplay/phases/rest/WarExhaustionRandomPort.scala` to `RoundEndRandomPort.scala`
- Rename: `src/test/scala/distantlands/testkit/WarExhaustionChoices.scala` to `RoundEndChoices.scala`
- Rename: `src/test/scala/distantlands/gameplay/WarExhaustionRandomPortSuite.scala` to `RoundEndRandomPortSuite.scala`
- Modify (mechanical rename): every file that names `WarExhaustionRandomPort`, `warExhaustionRandomPort` or `WarExhaustionChoices`, and the `warExhaustion` parameters of `Table.service`, `Situation.rules`, `Situation.productionRules` and `SituationDriver`

**Interfaces:**
- Produces:
  - `trait RoundEndRandomPort { def choose(candidates: Vector[PlayerId]): PlayerId; def endDie(): Int }`
  - `RoundEndRandomPort.random`
  - `RoundEndChoices.picking(pick: Vector[PlayerId] => PlayerId): RoundEndRandomPort`
  - `RoundEndChoices.endDie(face: Int): RoundEndRandomPort`
  - `RoundEndChoices.firstCandidate` and `RoundEndChoices.lastCandidate`
  - the `roundEnd` parameter of `Table.service`, `Situation.rules` and
    `Situation.productionRules`
  - the `roundEndRandomPort` field of `OathRules` and constructor parameter
    of `GameApplicationService`

- [ ] **Step 1: Rename the files and the names**

```bash
git mv src/main/scala/distantlands/gameplay/phases/rest/WarExhaustionRandomPort.scala \
  src/main/scala/distantlands/gameplay/phases/rest/RoundEndRandomPort.scala
git mv src/test/scala/distantlands/testkit/WarExhaustionChoices.scala \
  src/test/scala/distantlands/testkit/RoundEndChoices.scala
git mv src/test/scala/distantlands/gameplay/WarExhaustionRandomPortSuite.scala \
  src/test/scala/distantlands/gameplay/RoundEndRandomPortSuite.scala
git grep -l -e WarExhaustionRandomPort -e warExhaustionRandomPort \
  -e WarExhaustionChoices -- src frontend shared docs/rules docs/architecture \
  | xargs sed -i '' -e 's/WarExhaustionRandomPort/RoundEndRandomPort/g' \
    -e 's/warExhaustionRandomPort/roundEndRandomPort/g' \
    -e 's/WarExhaustionChoices/RoundEndChoices/g'
git grep -n -e 'warExhaustion:' -e 'warExhaustion =' -e 'warExhaustion,' \
  -e 'warExhaustion)' -- src/test
```

Rename each `warExhaustion` parameter and argument that the last command
lists to `roundEnd`. Leave `docs/superpowers/` alone: the plans and specs
there are records.

- [ ] **Step 2: Write the port**

`src/main/scala/distantlands/gameplay/phases/rest/RoundEndRandomPort.scala`:

```scala
package oathdigital.gameplay.phases.rest

import oathdigital.model.PlayerId

/** The randomness of a round's end: the purple end die of a Stable Regime
  * check (CR p. 19), and the winner War Exhaustion picks in a game with no
  * Empire and no Oathkeeper. Production rolls; tests fix the answers. */
trait RoundEndRandomPort:
  def choose(candidates: Vector[PlayerId]): PlayerId
  /** One roll of the end die, from 1 to 6. */
  def endDie(): Int
object RoundEndRandomPort:
  val random: RoundEndRandomPort = new RoundEndRandomPort:
    private val rng = new scala.util.Random()
    def choose(candidates: Vector[PlayerId]): PlayerId =
      candidates(rng.nextInt(candidates.size))
    def endDie(): Int = rng.nextInt(6) + 1
```

- [ ] **Step 3: Write the stubs**

`src/test/scala/distantlands/testkit/RoundEndChoices.scala`:

```scala
package oathdigital.testkit

import oathdigital.gameplay.phases.rest.RoundEndRandomPort
import oathdigital.model.PlayerId

/** Stubs of RoundEndRandomPort (canned answers, no record of calls). A stub
  * fails the test when asked for what it does not fix, so a test that
  * fixes the tied winner proves no die was rolled, and one that fixes the
  * die proves no winner was drawn. `RoundEndRandomPortSuite` holds them to
  * the port's promise. */
object RoundEndChoices:
  /** Picks the tied winner with `pick`; rolls no end die. */
  def picking(pick: Vector[PlayerId] => PlayerId): RoundEndRandomPort =
    new RoundEndRandomPort:
      def choose(candidates: Vector[PlayerId]): PlayerId = pick(candidates)
      def endDie(): Int =
        munit.Assertions.fail("this test rolls no end die")

  /** Rolls `face` on the end die; draws no tied winner. */
  def endDie(face: Int): RoundEndRandomPort = new RoundEndRandomPort:
    def choose(candidates: Vector[PlayerId]): PlayerId =
      munit.Assertions.fail("this test draws no tied winner")
    def endDie(): Int = face

  /** Always picks the first candidate. */
  val firstCandidate: RoundEndRandomPort = picking(_.head)

  /** Always picks the last candidate. */
  val lastCandidate: RoundEndRandomPort = picking(_.last)
```

- [ ] **Step 4: Add the die to the port's suite**

In `RoundEndRandomPortSuite`, keep the existing tests on `choose`. Rename
the class and its doc to the new port, and drop the stubs from any loop
that would now call `endDie` on them. Add:

```scala
  test("the random end die rolls every face from 1 to 6 and nothing else"):
    val faces = Vector.fill(600)(RoundEndRandomPort.random.endDie())
    assertEquals(faces.toSet, (1 to 6).toSet)
```

- [ ] **Step 5: Run the affected suites**

Run: `./sbtw "testOnly oathdigital.gameplay.RoundEndRandomPortSuite oathdigital.gameplay.RestSuite oathdigital.gameplay.StateBasedEvaluationSuite oathdigital.application.GameApplicationServiceStreamSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -u src frontend shared docs/rules docs/architecture
git add src/main/scala/distantlands/gameplay/phases/rest/RoundEndRandomPort.scala \
  src/test/scala/distantlands/testkit/RoundEndChoices.scala \
  src/test/scala/distantlands/gameplay/RoundEndRandomPortSuite.scala
git commit -m "refactor(empire): the round-end random port also rolls the end die

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The Empire's victory kinds

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala` (`VictoryKind`, about line 168)
- Modify: `src/main/scala/distantlands/serialization/EndingEventCodec.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala` (`exhaustion`, about line 298)
- Test: `src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala`

**Interfaces:**
- Produces:
  - `VictoryKind.StableRegime` (key `"stable-regime"`)
  - `VictoryKind.ChancellorExhaustion` (key `"chancellor-exhaustion"`)
  - `VictoryKind.Successor` (key `"successor"`)
  - `EndingEventCodec.victoryKind(value: ujson.Value, path: String): Either[WireError, VictoryKind]`, which Task 4 reuses

- [ ] **Step 1: Write the failing tests**

In `GameEventFactsWireSuite`, add:

```scala
  test("War Exhaustion round trips every victory kind"):
    VictoryKind.values.toVector.zipWithIndex.foreach { case (kind, index) =>
      val event = WarExhaustionResolved(PlayerId("p1"), kind, None,
        Vector.empty)
      val encoded = accepted(GameEventWire.encodeEvent("war", index, event))
      assertEquals(accepted(GameEventWire.decode(encoded)).event, event,
        clue(kind))
    }
```

In `GameLogHeadlineSuite`, add three rows to the `headlines` vector of
"each victory posts one victory headline naming the winner":

```scala
      OathEvent.WarExhaustionResolved(winner, VictoryKind.StableRegime, None,
        Vector.empty) -> " won by Stable Regime",
      OathEvent.WarExhaustionResolved(winner,
        VictoryKind.ChancellorExhaustion, None, Vector.empty) ->
        " won as the Chancellor",
      OathEvent.WarExhaustionResolved(winner, VictoryKind.Successor, None,
        Vector.empty) -> " won as Successor to the Chancellor",
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite oathdigital.application.gamelog.GameLogHeadlineSuite"`
Expected: compile failure, `StableRegime is not a member of VictoryKind`.

- [ ] **Step 3: Add the kinds**

In `GameState.scala`:

```scala
enum VictoryKind(val key: String):
  case Usurper extends VictoryKind("usurper")
  case Visionary extends VictoryKind("visionary")
  case Oathkeeper extends VictoryKind("oathkeeper")
  case RandomSelection extends VictoryKind("random-selection")
  /** The Chancellor's win on the end die, or at round eight while an
    * Imperial player holds the Oathkeeper title (CR p. 19). */
  case StableRegime extends VictoryKind("stable-regime")
  /** The Chancellor's win at round eight when no earlier step wins. */
  case ChancellorExhaustion extends VictoryKind("chancellor-exhaustion")
  /** A Citizen's win in the Chancellor's place (Successor to the
    * Chancellor). */
  case Successor extends VictoryKind("successor")
```

- [ ] **Step 4: Decode a kind by its key**

In `EndingEventCodec`, add:

```scala
  protected final def victoryKind(value: ujson.Value, path: String)
      : Either[WireError, VictoryKind] =
    VictoryKind.values.find(_.key == value.str).toRight(
      InvalidValue(path, s"unknown victory kind ${value.str}"))
```

Replace the string match in the `WarExhaustionResolvedType` decoder with:

```scala
          kind <- victoryKind(payload("victoryKind"), s"$path.victoryKind")
```

- [ ] **Step 5: Word the kinds in the log**

In `GameLogFormatter.exhaustion`, add:

```scala
    case VictoryKind.StableRegime => Vector(Text(" won by Stable Regime"))
    case VictoryKind.ChancellorExhaustion =>
      Vector(Text(" won as the Chancellor"))
    case VictoryKind.Successor =>
      Vector(Text(" won as Successor to the Chancellor"))
```

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite oathdigital.application.gamelog.GameLogHeadlineSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/serialization/EndingEventCodec.scala \
  src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala \
  src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala \
  src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala
git commit -m "feat(empire): Stable Regime, Chancellor and Successor victory kinds

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The Empire's endings as rules

**Files:**
- Create: `src/main/scala/distantlands/gameplay/EmpireEndings.scala`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala` (share the Visionary step)
- Test: `src/test/scala/distantlands/gameplay/EmpireEndingsSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala` (the no-Empire tie order)

**Interfaces:**
- Consumes: the `VictoryKind` cases from Task 2.
- Produces:
  - `EmpireEndings.hasEmpire(ready: ReadyGame): Boolean`
  - `EmpireEndings.threshold(round: Int): Option[Int]`
  - `EmpireEndings.rollsEndDie(ready: ReadyGame): Boolean`
  - `EmpireEndings.stableRegime(ready: ReadyGame, round: Int, face: Int): Option[GameResult]`
  - `EmpireEndings.warExhaustion(ready: ReadyGame): Option[OathEvent.WarExhaustionResolved]`
  - `EmpireEndings.chancellorWins(ready: ReadyGame, kind: VictoryKind): Option[GameResult]`
  - `EmpireEndings.successor(ready: ReadyGame): Option[PlayerId]`
  - `StateBasedEvaluation.visionaryWin(ready: ReadyGame): Option[OathEvent.WarExhaustionResolved]`, `private[gameplay]`

- [ ] **Step 1: Write the failing suite**

`src/test/scala/distantlands/gameplay/EmpireEndingsSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._
import oathdigital.model.OathEvent.WarExhaustionResolved
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** The Empire's endings as rules (CR p. 19; Empire phase design, "Slice
  * 2"). p1 is the Chancellor and p2 a Citizen; p3 is an Exile unless a
  * test makes them a Citizen. */
class EmpireEndingsSuite extends munit.FunSuite:
  /** The Empire's table at `round`, with `holder` holding the title on
    * `side` under `goal`. */
  private def empire(round: Int = 5, holder: Option[PlayerId] = Some(p1),
      side: TitleSide = TitleSide.Oathkeeper,
      goal: OathkeeperGoal = OathkeeperGoal.ThePeople,
      citizens: Vector[PlayerId] = Vector(p2)): Table =
    Table.start.imperial(p1, citizens*).oathkeeper(holder, side)
      .oathkeeperGoal(goal)
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = round, usurperLimited = false))))

  /** `player` reveals `vision` and meets its goal: rules Dunes for
    * Conquest, holds the People's Favor for Rebellion, a relic for
    * Sanctuary, or the Darkest Secret for Faith. */
  private def meetingVision(table: Table, player: PlayerId,
      vision: VisionId): Table =
    val met = vision match
      case VisionRules.Conquest => table.warbandsAt("Dunes", player, 1)
      case VisionRules.Rebellion => table.peoplesFavor(Some(player), favor = 1)
      case VisionRules.Sanctuary =>
        table.relic(player, "Dowsing Sticks", facedown = true)
      case _ => table.darkestSecret(Some(player), secrets = 1)
    met.revealedVision(player, vision)

  private val chancellorWin = WarExhaustionResolved(p1,
    VictoryKind.ChancellorExhaustion, None, Vector.empty)

  test("the end die needs a 6, a 5 and a 3 at rounds 5, 6 and 7"):
    assertEquals((1 to 8).map(EmpireEndings.threshold).toVector,
      Vector(None, None, None, None, Some(6), Some(5), Some(3), None))

  test("the die is rolled only with an Empire, an Imperial Oathkeeper and " +
      "a round from 5 to 7"):
    assert(EmpireEndings.rollsEndDie(empire().ready))
    assert(EmpireEndings.rollsEndDie(empire(holder = Some(p2)).ready))
    assert(!EmpireEndings.rollsEndDie(empire(holder = Some(p3)).ready))
    assert(!EmpireEndings.rollsEndDie(empire(holder = None).ready))
    assert(!EmpireEndings.rollsEndDie(empire(round = 4).ready))
    assert(!EmpireEndings.rollsEndDie(empire(round = 8).ready))
    assert(!EmpireEndings.rollsEndDie(Table.start.oathkeeper(Some(p1))
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = 5)))).ready))

  test("each face wins from its round's threshold up"):
    for
      (round, least) <- Vector(5 -> 6, 6 -> 5, 7 -> 3)
      face <- 1 to 6
    do
      val ready = empire(round = round).ready
      assertEquals(EmpireEndings.stableRegime(ready, round, face),
        Option.when(face >= least)(GameResult(p1, VictoryKind.StableRegime)),
        clue((round, face)))

  test("a Successor takes the Stable Regime win"):
    val ready = empire().darkestSecret(Some(p2), secrets = 1).ready
    assertEquals(EmpireEndings.stableRegime(ready, 5, 6),
      Some(GameResult(p2, VictoryKind.Successor)))

  test("the Successor goal follows the Oathkeeper goal"):
    val people = empire(goal = OathkeeperGoal.ThePeople)
    assertEquals(EmpireEndings.successor(
      people.darkestSecret(Some(p2), secrets = 1).ready), Some(p2))
    assertEquals(EmpireEndings.successor(
      people.peoplesFavor(Some(p2), favor = 1).ready), None)
    val protection = empire(goal = OathkeeperGoal.Protection)
    assertEquals(EmpireEndings.successor(
      protection.peoplesFavor(Some(p2), favor = 1).ready), Some(p2))
    val devotion = empire(goal = OathkeeperGoal.Devotion)
    assertEquals(EmpireEndings.successor(
      devotion.relic(p2, TheGrandScepterCard.id).ready), Some(p2))
    assertEquals(EmpireEndings.successor(
      devotion.darkestSecret(Some(p2), secrets = 1).ready), None)

  test("only a Citizen can be the Successor"):
    assertEquals(EmpireEndings.successor(empire()
      .darkestSecret(Some(p3), secrets = 1).ready), None)
    assertEquals(EmpireEndings.successor(empire()
      .darkestSecret(Some(p1), secrets = 1).ready), None)

  test("Supremacy's Successor holds more relics than each Imperial, the " +
      "Grand Scepter counted"):
    // R17: the Chancellor's Grand Scepter is one of their relics.
    val supremacy = empire(goal = OathkeeperGoal.Supremacy,
      citizens = Vector(p2, p3)).relic(p1, TheGrandScepterCard.id)
    val tied = supremacy.relic(p2, "Yew Staff")
    assertEquals(EmpireEndings.successor(tied.ready), None)
    val ahead = tied.relic(p2, "Dowsing Sticks")
    assertEquals(EmpireEndings.successor(ahead.ready), Some(p2))
    val matched = ahead.relic(p3, "Cracked Horn").relic(p3, "Cup of Plenty")
    assertEquals(EmpireEndings.successor(matched.ready), None)

  test("War Exhaustion: an Imperial Oathkeeper gives the Chancellor the " +
      "win"):
    val ready = empire(round = 8, holder = Some(p2)).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p1, VictoryKind.StableRegime, None,
        Vector.empty)))

  test("War Exhaustion: an Exile Usurper wins next"):
    val ready = empire(round = 8, holder = Some(p3),
      side = TitleSide.Usurper).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p3, VictoryKind.Usurper, None, Vector.empty)))

  test("War Exhaustion: an Exile meeting their Vision wins next"):
    val ready = empire(round = 8, holder = None)
      .revealedVision(p3, VisionRules.Faith).visionsDrawn(3)
      .darkestSecret(Some(p3), secrets = 1).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p3, VictoryKind.Visionary,
        Some(VisionRules.Faith), Vector.empty)))

  test("War Exhaustion: tied Visions go to Conquest, then Rebellion, " +
      "Sanctuary and Faith"):
    // p1 is the Chancellor with no Citizens; p2 and p3 are Exiles who both
    // meet their Visions. Each seat holds the earlier Vision in turn, so
    // the printed order decides, not the seat.
    val order = Vector(VisionRules.Conquest, VisionRules.Rebellion,
      VisionRules.Sanctuary, VisionRules.Faith)
    for
      (earlier, i) <- order.zipWithIndex
      later <- order.drop(i + 1)
      (first, second) <- Vector(p2 -> p3, p3 -> p2)
    do
      val base = empire(round = 8, holder = None, citizens = Vector.empty)
      val table = meetingVision(meetingVision(base, first, earlier),
        second, later).visionsDrawn(3)
      assertEquals(EmpireEndings.warExhaustion(table.ready), Some(
        WarExhaustionResolved(first, VictoryKind.Visionary, Some(earlier),
          Vector.empty)), clue((earlier, later, first)))

  test("War Exhaustion: otherwise the Chancellor wins, or the Successor"):
    assertEquals(EmpireEndings.warExhaustion(
      empire(round = 8, holder = Some(p3)).ready), Some(chancellorWin))
    assertEquals(EmpireEndings.warExhaustion(empire(round = 8, holder = None)
      .darkestSecret(Some(p2), secrets = 1).ready), Some(
      WarExhaustionResolved(p2, VictoryKind.Successor, None, Vector.empty)))

  test("a game with no Chancellor has no Empire ending"):
    val ready = Table.start.oathkeeper(Some(p1)).ready
    assert(!EmpireEndings.hasEmpire(ready))
    assertEquals(EmpireEndings.chancellorWins(ready,
      VictoryKind.StableRegime), None)
```

The same tie order holds with no Empire. In `StateBasedEvaluationSuite`,
add a test with the same loop and its own copy of `meetingVision`. The loop builds on
`preparedTable(Vector.empty, round = 8)` and reads
`lastEventAtRoundEnd(table, pickFirstCandidate)`:

```scala
  test("tied War Exhaustion Visions go to Conquest, then Rebellion, " +
      "Sanctuary and Faith"):
    val order = Vector(VisionRules.Conquest, VisionRules.Rebellion,
      VisionRules.Sanctuary, VisionRules.Faith)
    for
      (earlier, i) <- order.zipWithIndex
      later <- order.drop(i + 1)
      (first, second) <- Vector(p2 -> p3, p3 -> p2)
    do
      val table = meetingVision(meetingVision(
        preparedTable(Vector.empty, round = 8), first, earlier),
        second, later).visionsDrawn(3)
      assertEquals(lastEventAtRoundEnd(table, pickFirstCandidate),
        WarExhaustionResolved(first, VictoryKind.Visionary, Some(earlier),
          Vector.empty), clue((earlier, later, first)))
```

The four relics are catalog names: `Yew Staff` and `Dowsing Sticks` are
used by existing suites, and `Cracked Horn` and `Cup of Plenty` are relic
cards in `src/main/scala/distantlands`.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.EmpireEndingsSuite"`
Expected: compile failure, `Not found: EmpireEndings`.

- [ ] **Step 3: Share the Visionary step**

In `StateBasedEvaluation`, add this method and use it in place of the two
`visionPriority.iterator...headOption` blocks in `endRound` and
`endRoundWinner`. It also leaves out Imperial players, whose Visions are
discarded when they become Imperial; a no-Empire game has none, so that
path is unchanged.

```scala
  /** War Exhaustion's Visionary step: an Exile with a revealed Vision whose
    * goal they meet, in printed priority order (CR p. 19). */
  private[gameplay] def visionaryWin(ready: ReadyGame)
      : Option[WarExhaustionResolved] =
    val current = ready.game.current
    visionPriority.iterator.flatMap { vision =>
      current.players.find(p => !Imperials.isImperial(ready, p.player) &&
        p.revealedVision.exists(_.id == vision) &&
        VisionVictoryEligibility.qualifies(p.player, ready))
        .map(p => WarExhaustionResolved(p.player, VictoryKind.Visionary,
          Some(vision), Vector.empty))
    }.nextOption()
```

- [ ] **Step 4: Write the rules**

`src/main/scala/distantlands/gameplay/EmpireEndings.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._
import oathdigital.model.OathEvent.WarExhaustionResolved

/** The Empire's round-end endings (CR p. 19; Empire phase design, "Slice
  * 2"): Stable Regime on the end die at the end of rounds 5 to 7, War
  * Exhaustion's four steps at the end of round 8, and the Successor, who
  * takes any win of the Chancellor's. A game has an Empire while a
  * Chancellor is seated. */
object EmpireEndings:
  def hasEmpire(ready: ReadyGame): Boolean =
    Imperials.chancellor(ready).nonEmpty

  /** The lowest end-die face that gives the Chancellor the win at the end
    * of `round`. */
  def threshold(round: Int): Option[Int] = round match
    case 5 => Some(6)
    case 6 => Some(5)
    case 7 => Some(3)
    case _ => None

  /** Whether this round's end rolls the end die: an Empire, an Imperial
    * Oathkeeper, and a round from 5 to 7. */
  def rollsEndDie(ready: ReadyGame): Boolean =
    hasEmpire(ready) && imperialOathkeeper(ready) &&
      threshold(ready.game.current.tracks.round).nonEmpty

  /** The win `face` gives at the end of `round`, or none. */
  def stableRegime(ready: ReadyGame, round: Int, face: Int)
      : Option[GameResult] =
    threshold(round).filter(face >= _).flatMap(_ =>
      chancellorWins(ready, VictoryKind.StableRegime))

  /** War Exhaustion with an Empire, in printed order: an Imperial
    * Oathkeeper gives the Chancellor the win; then an Exile Usurper wins;
    * then an Exile meeting their Vision; otherwise the Chancellor. */
  def warExhaustion(ready: ReadyGame): Option[WarExhaustionResolved] =
    def resolved(result: GameResult) =
      WarExhaustionResolved(result.winner, result.kind, None, Vector.empty)
    if imperialOathkeeper(ready) then
      chancellorWins(ready, VictoryKind.StableRegime).map(resolved)
    else ready.game.current.title match
      case OathkeeperState(Some(player), TitleSide.Usurper) =>
        Some(WarExhaustionResolved(player, VictoryKind.Usurper, None,
          Vector.empty))
      case _ => StateBasedEvaluation.visionaryWin(ready).orElse(
        chancellorWins(ready, VictoryKind.ChancellorExhaustion).map(resolved))

  /** The Chancellor's win of `kind`, or the Successor's in its place. */
  def chancellorWins(ready: ReadyGame, kind: VictoryKind)
      : Option[GameResult] =
    Imperials.chancellor(ready).map(chancellor => successor(ready).fold(
      GameResult(chancellor, kind))(GameResult(_, VictoryKind.Successor)))

  /** The Citizen who meets the Successor goal under the current Oathkeeper
    * goal; at most one can. Supremacy's goal counts every relic held, the
    * Grand Scepter included (ruling R17). */
  def successor(ready: ReadyGame): Option[PlayerId] =
    val current = ready.game.current
    val imperials = Imperials.players(ready)
    val citizens =
      imperials.filter(Imperials.role(ready, _).contains(Role.Citizen))
    def relics(player: PlayerId): Int =
      current.players.find(_.player == player).fold(0)(_.relics.size)
    def holdsScepter(player: PlayerId): Boolean =
      current.players.exists(state => state.player == player &&
        state.relics.exists(_.id == TheGrandScepterCard.id))
    ready.game.campaign.oathkeeperGoal match
      case OathkeeperGoal.Supremacy => citizens.find(citizen =>
        imperials.filterNot(_ == citizen).forall(relics(citizen) > relics(_)))
      case OathkeeperGoal.ThePeople =>
        current.banners.darkestSecret.holder.filter(citizens.contains)
      case OathkeeperGoal.Protection =>
        current.banners.peoplesFavor.holder.filter(citizens.contains)
      case OathkeeperGoal.Devotion => citizens.find(holdsScepter)

  private def imperialOathkeeper(ready: ReadyGame): Boolean =
    ready.game.current.title.holder.exists(Imperials.isImperial(ready, _))
```

- [ ] **Step 5: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.EmpireEndingsSuite oathdigital.gameplay.StateBasedEvaluationSuite oathdigital.gameplay.RestSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/EmpireEndings.scala \
  src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala \
  src/test/scala/distantlands/gameplay/EmpireEndingsSuite.scala \
  src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala
git commit -m "feat(empire): Stable Regime, War Exhaustion and Successor rules

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The end-die event, its wire and its log line

**Files:**
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala` (beside `RoundEnded`)
- Modify: `src/main/scala/distantlands/serialization/GameEventWire.scala`
- Modify: `src/main/scala/distantlands/serialization/EndingEventCodec.scala`
- Modify: `src/main/scala/distantlands/gameplay/OathRules.scala` (the `evolve` match, about line 69)
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala` (`eventLines`, about line 170)
- Test: `src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala`

**Interfaces:**
- Consumes: `EmpireEndings.threshold` (Task 3); `EndingEventCodec.victoryKind` (Task 2).
- Produces: `OathEvent.EndDieRolled(round: Int, face: Int, result: Option[GameResult])`, with the wire type `"gameplay.end-die-rolled"`.

- [ ] **Step 1: Write the failing tests**

In `GameEventFactsWireSuite`, add:

```scala
  test("the end die round trips with and without a win"):
    val events = Vector[OathEvent](OathEvent.EndDieRolled(5, 4, None),
      OathEvent.EndDieRolled(7, 3,
        Some(GameResult(PlayerId("p2"), VictoryKind.Successor))))
    assertStreamRoundTrips("end-die", events)

  test("an end die naming a winner without a kind is refused"):
    val encoded = accepted(GameEventWire.encodeEvent("end-die", 0,
      OathEvent.EndDieRolled(6, 5,
        Some(GameResult(PlayerId("p1"), VictoryKind.StableRegime)))))
    encoded("payload")("victoryKind") = ujson.Null
    assert(GameEventWire.decode(encoded).isLeft)
```

In `GameLogHeadlineSuite`, add:

```scala
  test("the end die posts its face and the face it needed"):
    val script = recordedEventsBoard
    val missed = formatAppended(script,
      Vector(OathEvent.EndDieRolled(6, 4, None))).last
    assertEquals(missed.kind, LogKind.Roll)
    assertEquals(text(missed),
      "The end die rolled 4. Stable Regime needed 5 or more.")
    val five = formatAppended(script,
      Vector(OathEvent.EndDieRolled(5, 2, None))).last
    assertEquals(text(five), "The end die rolled 2. Stable Regime needed 6.")

  test("a winning end die posts the roll, then the victory"):
    val script = recordedEventsBoard
    val winner = script.actor
    val lines = formatAppended(script, Vector(OathEvent.EndDieRolled(7, 3,
      Some(GameResult(winner, VictoryKind.Successor))))).takeRight(2)
    assertEquals(lines.map(_.kind), Vector(LogKind.Roll, LogKind.Victory))
    assertEquals(text(lines.last),
      playerLabel(winner) + " won as Successor to the Chancellor")
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite oathdigital.application.gamelog.GameLogHeadlineSuite"`
Expected: compile failure, `EndDieRolled is not a member of object OathEvent`.

- [ ] **Step 3: Add the event**

In `GameEventProtocol.scala`, after `RoundEnded`:

```scala
  /** The purple end die at the end of round 5, 6 or 7, rolled while an
    * Imperial player holds the Oathkeeper title (CR p. 19). `result` is the
    * win the face gives, the Chancellor's or a Successor's; a winning roll
    * ends the game, and after a losing one `RoundEnded` follows. */
  final case class EndDieRolled(round: Int, face: Int,
      result: Option[GameResult]) extends OathEvent
```

- [ ] **Step 4: Put it on the wire**

In `GameEventWire`, add `val EndDieRolledType = "gameplay.end-die-rolled"`
after `WarExhaustionResolvedType`.

In `EndingEventCodec`:
- discriminator: `case _: EndDieRolled => EndDieRolledType`
- encoder:

```scala
      case EndDieRolled(round, face, result) => ujson.Obj(
        "round" -> round,
        "face" -> face,
        "winnerPlayerId" -> result.fold[ujson.Value](ujson.Null)(r =>
          ujson.Str(r.winner.value)),
        "victoryKind" -> result.fold[ujson.Value](ujson.Null)(r =>
          ujson.Str(r.kind.key)))
```

- decoder:

```scala
        case EndDieRolledType => for
          round <- safeIntField(payload.obj, "round", path)
          face <- safeIntField(payload.obj, "face", path)
          result <- (payload("winnerPlayerId"), payload("victoryKind")) match
            case (ujson.Null, ujson.Null) => Right(None)
            case (ujson.Null, _) | (_, ujson.Null) => Left(InvalidValue(path,
              "an end die names a winner and a victory kind together"))
            case (winner, kind) =>
              victoryKind(kind, s"$path.victoryKind").map(k =>
                Some(GameResult(PlayerId(winner.str), k)))
        yield EndDieRolled(round, face, result)
```

- [ ] **Step 5: Route it to the round-end rules**

In `OathRules.evolve`, beside the `RoundEnded` arm:

```scala
      case event: EndDieRolled => StateBasedEvaluation.evolve(catalog, state, event, outsideActions)
```

`StateBasedEvaluation.evolve` refuses it through its catch-all arm until
Task 5 adds its replay.

- [ ] **Step 6: Write the log line**

In `GameLogFormatter.eventLines`, after the `WarExhaustionResolved` arm:

```scala
      case OathEvent.EndDieRolled(round, face, result) =>
        (Posted(LogKind.Roll, Vector(Text(endDie(round, face)))) +:
          result.map(won => victory(won.winner,
            exhaustion(won.kind, None))).toVector, run)
```

and beside `exhaustion`:

```scala
  /** "The end die rolled 4. Stable Regime needed 5 or more." */
  private def endDie(round: Int, face: Int): String =
    val needed = EmpireEndings.threshold(round).fold("") {
      case 6 => " Stable Regime needed 6."
      case least => s" Stable Regime needed $least or more."
    }
    s"The end die rolled $face.$needed"
```

Import `oathdigital.gameplay.EmpireEndings`. If
`check-architecture.py` refuses that import from `application/gamelog`,
move `threshold` into `model/` as `StableRegime.threshold`, and have
`EmpireEndings.threshold` call it.

- [ ] **Step 7: Run the tests**

Run: `./sbtw "testOnly oathdigital.serialization.GameEventFactsWireSuite oathdigital.application.gamelog.GameLogHeadlineSuite"` and `python3 scripts/check-architecture.py`
Expected: PASS, and the architecture check passes.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/GameEventProtocol.scala \
  src/main/scala/distantlands/serialization/GameEventWire.scala \
  src/main/scala/distantlands/serialization/EndingEventCodec.scala \
  src/main/scala/distantlands/gameplay/OathRules.scala \
  src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala \
  src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala \
  src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala
git commit -m "feat(empire): journal and log the end die

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The round's end with an Empire

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala` (`endRound`, `evolve`, `endRoundWinner`)
- Modify: `src/main/scala/distantlands/gameplay/phases/rest/TurnBoundary.scala`
- Test: `src/test/scala/distantlands/gameplay/EmpireRoundEndSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala` (callers of the new `endRound` signature)

**Interfaces:**
- Consumes: `RoundEndRandomPort` and `RoundEndChoices` (Task 1); `EmpireEndings` (Task 3); `EndDieRolled` (Task 4).
- Produces: `StateBasedEvaluation.endRound(state: OathState, random: RoundEndRandomPort): Either[OathViolation, Vector[OathEvent]]`

- [ ] **Step 1: Write the failing suite**

`src/test/scala/distantlands/gameplay/EmpireRoundEndSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.OathEvent._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Outcomes, RoundEndChoices, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The end of a round in a game with an Empire (CR p. 19; Empire phase
  * design, "Slice 2"), from `endRound` and through replay. p1 is the
  * Chancellor, so their seat starts each round; p2 is a Citizen and p3 an
  * Exile. */
class EmpireRoundEndSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)

  /** The Empire's table at Round End of `round`, with `holder` holding the
    * title. */
  private def roundEnd(round: Int, holder: Option[PlayerId] = Some(p2))
      : ReadyGame =
    Table.start.imperial(p1, p2).oathkeeper(holder)
      .oathkeeperGoal(OathkeeperGoal.ThePeople).turn(p1, Phase.RoundEnd)
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = round, usurperLimited = false)))).ready

  private def ended(ready: ReadyGame, random: RoundEndRandomPort)
      : Vector[OathEvent] =
    accepted(StateBasedEvaluation.endRound(Ready(ready), random))

  /** The state after applying `events` to `ready` through the rules. */
  private def replayed(ready: ReadyGame, events: Vector[OathEvent])
      : Either[OathViolation, OathState] =
    events.foldLeft[Either[OathViolation, OathState]](Right(Ready(ready)))(
      (state, event) => state.flatMap(rules.evolve(_, event)))

  test("rounds 1 to 4 roll no end die"):
    (1 to 4).foreach { round =>
      assertEquals(ended(roundEnd(round), RoundEndChoices.firstCandidate),
        Vector(RoundEnded(round, Some(round + 1))), clue(round))
    }

  test("an Exile Oathkeeper rolls no end die"):
    assertEquals(ended(roundEnd(5, holder = Some(p3)),
      RoundEndChoices.firstCandidate), Vector(RoundEnded(5, Some(6))))

  test("a losing roll is journaled before the next round"):
    val events = ended(roundEnd(6), RoundEndChoices.endDie(4))
    assertEquals(events, Vector(EndDieRolled(6, 4, None),
      RoundEnded(6, Some(7))))
    val after = Outcomes.ready(accepted(replayed(roundEnd(6), events)))
    assertEquals(after.game.current.tracks.round, 7)
    assertEquals(after.game.current.result, None)

  test("a winning roll ends the game with no round after it"):
    val events = ended(roundEnd(7), RoundEndChoices.endDie(3))
    val won = GameResult(p1, VictoryKind.StableRegime)
    assertEquals(events, Vector(EndDieRolled(7, 3, Some(won))))
    val after = Outcomes.ready(accepted(replayed(roundEnd(7), events)))
    assertEquals(after.game.current.result, Some(won))

  test("replay refuses a changed face or win"):
    val ready = roundEnd(5)
    assert(replayed(ready, Vector(EndDieRolled(5, 5,
      Some(GameResult(p1, VictoryKind.StableRegime))))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(5, 6, None))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(5, 7, None))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(6, 6, None))).isLeft)

  test("replay refuses an end die where none is rolled"):
    assert(replayed(roundEnd(4), Vector(EndDieRolled(4, 6, None))).isLeft)
    assert(replayed(roundEnd(5, holder = Some(p3)),
      Vector(EndDieRolled(5, 6, None))).isLeft)
    val inAct = Table.from(roundEnd(5)).turn(p1, Phase.Act).ready
    assert(replayed(inAct, Vector(EndDieRolled(5, 6, None))).isLeft)

  test("round 8 resolves the Empire's War Exhaustion and replays it"):
    val ready = roundEnd(8, holder = Some(p3))
    val events = ended(ready, RoundEndChoices.endDie(1))
    val won = WarExhaustionResolved(p1, VictoryKind.ChancellorExhaustion,
      None, Vector.empty)
    assertEquals(events, Vector(RoundEnded(8, None), won))
    val after = Outcomes.ready(accepted(replayed(ready, events)))
    assertEquals(after.game.current.result,
      Some(GameResult(p1, VictoryKind.ChancellorExhaustion)))

  test("round 8 gives the Successor the Chancellor's win"):
    val ready = Table.from(roundEnd(8, holder = None))
      .darkestSecret(Some(p2), secrets = 1).ready
    val events = ended(ready, RoundEndChoices.endDie(1))
    assertEquals(events.last, WarExhaustionResolved(p2,
      VictoryKind.Successor, None, Vector.empty))
    assert(replayed(ready, events).isRight)

  test("replay refuses an Empire's War Exhaustion with another winner"):
    val ready = roundEnd(8, holder = Some(p3))
    assertEquals(rejected(replayed(ready, Vector(RoundEnded(8, None),
      WarExhaustionResolved(p3, VictoryKind.Oathkeeper, None,
        Vector.empty)))).getClass, classOf[OathViolation.InvalidEventOrder])
```

Check before running:
- `Table.from(ready)` exists: `RestSuite` uses it.
- `rejected` returns the `Left` value: see `testkit/Unwrap.scala`. If it
  returns something else, assert `.isLeft` instead.
- A Round End seat: if `Table.turn(p1, Phase.RoundEnd)` is refused because
  the Chancellor's seat does not start the round, read
  `TurnOrder.roundStart` and use the player it gives.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.EmpireRoundEndSuite"`
Expected: compile failure, because `endRound` takes a function, not a
`RoundEndRandomPort`.

- [ ] **Step 3: Take the port in `endRound`**

Change the signature to:

```scala
  def endRound(state: OathState, random: RoundEndRandomPort)
      : Either[OathViolation, Vector[OathEvent]]
```

Replace `randomWinner(candidates)` with `random.choose(candidates)`. In
`TurnBoundary.finishRound`, pass `randomPort` itself:
`StateBasedEvaluation.endRound(transition.state, randomPort)`.

In `StateBasedEvaluationSuite`, keep the three `pick...` functions and
pass `RoundEndChoices.picking(pick)` wherever `endRound` received a
function.

- [ ] **Step 4: Branch on the Empire**

In `endRound`, after the phase check and before `current.tracks.round < 8`:

```scala
    else if EmpireEndings.rollsEndDie(ready) then
      val round = current.tracks.round
      val face = random.endDie()
      val result = EmpireEndings.stableRegime(ready, round, face)
      Right(EndDieRolled(round, face, result) +: Option.when(
        result.isEmpty)(RoundEnded(round, Some(round + 1))).toVector)
```

and before the existing round-8 block:

```scala
    else if current.tracks.round == 8 && EmpireEndings.hasEmpire(ready) then
      EmpireEndings.warExhaustion(ready).toRight(InvalidEventOrder(
        "an Empire's War Exhaustion found no winner"))
        .map(won => Vector(RoundEnded(8, None), won))
```

In `endRoundWinner`, start with the Empire:

```scala
    if EmpireEndings.hasEmpire(ready) then
      EmpireEndings.warExhaustion(ready).map(ExactWar(_)).toRight(
        InvalidEventOrder("an Empire's War Exhaustion found no winner"))
    else
      // the existing match, unchanged
```

- [ ] **Step 5: Replay the end die**

In `StateBasedEvaluation.evolve`, before the catch-all arm:

```scala
      case recorded: EndDieRolled => supported(state).flatMap { ready =>
        val current = ready.game.current
        val round = current.tracks.round
        if current.turn.phase != Phase.RoundEnd || current.result.nonEmpty ||
            current.turn.activePlayer != TurnOrder.roundStart(ready) then
          Left(InvalidEventOrder("the end die is rolled only at a round's end"))
        else if !EmpireEndings.rollsEndDie(ready) then
          Left(InvalidEventOrder("this round's end rolls no end die"))
        else if recorded.round != round || recorded.face < 1 ||
            recorded.face > 6 then
          Left(InvalidEventOrder(s"end die mismatch: recorded $recorded"))
        else
          val expected = EmpireEndings.stableRegime(ready, round,
            recorded.face)
          if expected != recorded.result then Left(InvalidEventOrder(
            s"Stable Regime mismatch: expected $expected, recorded " +
              s"${recorded.result}"))
          else update(state)(_.copy(result = recorded.result))
      }
```

Update the object's doc comment. It no longer defers the Empire's
endings: it now says that `EmpireEndings` owns them and that this object
journals and replays them.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.EmpireRoundEndSuite oathdigital.gameplay.StateBasedEvaluationSuite oathdigital.gameplay.RestSuite oathdigital.gameplay.EmpireEndingsSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala \
  src/main/scala/distantlands/gameplay/phases/rest/TurnBoundary.scala \
  src/test/scala/distantlands/gameplay/EmpireRoundEndSuite.scala \
  src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala
git commit -m "feat(empire): roll the end die and resolve the Empire's War Exhaustion

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The victory screen names the win

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/VictoryWords.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ActionDecisionRenderer.scala` (about lines 27 and 56)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` (`winnerBanner`, about line 340)
- Test: `frontend/src/test/scala/distantlands/frontend/VictoryWordsSuite.scala`

**Interfaces:**
- Consumes: the wire keys of every `VictoryKind` (Task 2).
- Produces: `VictoryWords.of(key: Option[String]): String`; `winnerBanner(value, winner, victory)` now writes `" wins — " + victory` with no added word.

- [ ] **Step 1: Write the failing suite**

`frontend/src/test/scala/distantlands/frontend/VictoryWordsSuite.scala`:

```scala
package oathdigital.frontend

/** What the victory screen calls each kind of win (Empire phase design,
  * "Slice 2", "UI"). */
class VictoryWordsSuite extends munit.FunSuite:
  test("each victory kind has its name"):
    val names = Vector(
      "usurper" -> "Usurper victory",
      "visionary" -> "Vision victory",
      "oathkeeper" -> "Oathkeeper victory",
      "random-selection" -> "victory by random selection",
      "stable-regime" -> "Stable Regime victory",
      "chancellor-exhaustion" -> "War Exhaustion victory",
      "successor" -> "Successor to the Chancellor")
    names.foreach { case (key, name) =>
      assertEquals(VictoryWords.of(Some(key)), name, clue(key))
    }

  test("an unknown or missing kind still reads as a victory"):
    assertEquals(VictoryWords.of(Some("grand-council")),
      "grand council victory")
    assertEquals(VictoryWords.of(None), "victory")
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.VictoryWordsSuite"`
Expected: compile failure, `Not found: VictoryWords`.

- [ ] **Step 3: Write the words**

`frontend/src/main/scala/distantlands/frontend/VictoryWords.scala`:

```scala
package oathdigital.frontend

/** What the winner banner and the game-over status call a win, from its
  * victory kind's wire key: "Usurper victory", "Successor to the
  * Chancellor". */
private[frontend] object VictoryWords:
  def of(key: Option[String]): String = key match
    case Some("usurper") => "Usurper victory"
    case Some("visionary") => "Vision victory"
    case Some("oathkeeper") => "Oathkeeper victory"
    case Some("random-selection") => "victory by random selection"
    case Some("stable-regime") => "Stable Regime victory"
    case Some("chancellor-exhaustion") => "War Exhaustion victory"
    case Some("successor") => "Successor to the Chancellor"
    case Some(other) => s"${other.replace('-', ' ')} victory"
    case None => "victory"
```

- [ ] **Step 4: Use them**

In `ActionDecisionRenderer`:
- The game-over status becomes
  `s"Game over — $winner wins (${VictoryWords.of(value.oathkeeper.flatMap(_.winnerVictoryKind))})."`.
  Drop the old `reason` value.
- The banner becomes
  `winnerBanner(value, winner, VictoryWords.of(value.oathkeeper.flatMap(_.winnerVictoryKind)))`.
  Drop the old `.getOrElse("winner").replace('-', ' ')` value.

In `ServerUiSupport.winnerBanner`, write
`s" wins — $victory"`, without the trailing `victory`.

Then search the frontend tests for any assertion on the old text, and
update it to the new words:

```bash
git grep -n -e ' victory"' -e 'wins (' -- frontend/src/test
```

- [ ] **Step 5: Run the frontend tests**

Run: `./sbtw frontend/test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/VictoryWords.scala \
  frontend/src/main/scala/distantlands/frontend/ActionDecisionRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  frontend/src/test/scala/distantlands/frontend/VictoryWordsSuite.scala
git commit -m "feat(empire): the victory screen names Stable Regime and the Successor

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Add to that `git add` any frontend test file the search in Step 4 led you
to change.

---

### Task 7: Scenarios through the service

**Files:**
- Create: `src/test/scala/distantlands/application/EmpireEndingsScenarioSuite.scala`
- Modify: `src/test/scala/distantlands/application/PlanAidServiceSuite.scala` (the 1b-ii gap)

**Interfaces:**
- Consumes: `Table.service(..., roundEnd = ...)` (Task 1); everything above.

- [ ] **Step 1: Write the Stable Regime scenario**

`src/test/scala/distantlands/application/EmpireEndingsScenarioSuite.scala`:

```scala
package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Outcomes, RoundEndChoices, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Stable Regime with a Successor taking the win (CR p. 19; Empire phase
  * design, "Slice 2"), played through the service. */
class EmpireEndingsScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-endings"

  /** p1, the Chancellor, holds the Oathkeeper title of the People with the
    * People's Favor. p2, a Citizen, holds the Darkest Secret, which is the
    * Successor goal. p3, an Exile, acts last in round 5. */
  private val table: Table = Table.start.imperial(p1, p2)
    .oathkeeperGoal(OathkeeperGoal.ThePeople).oathkeeper(Some(p1))
    .peoplesFavor(Some(p1), favor = 1).darkestSecret(Some(p2), secrets = 1)
    .turn(p3, Phase.Act)
    // No Table step sets the round.
    .update(_.updateCurrent(c => c.copy(tracks =
      c.tracks.copy(round = 5, usurperLimited = false))))

  test("a 6 on the end die gives the Successor the Chancellor's win"):
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    val rested = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    assertEquals(Outcomes.ready(rested.state).game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.contains("The end die rolled 6. Stable Regime needed 6."),
      lines)
    assert(lines.contains("p2 won as Successor to the Chancellor"), lines)

  test("a 5 on the end die starts round 6"):
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(5))
    val rested = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    val current = Outcomes.ready(rested.state).game.current
    assertEquals(current.result, None)
    assertEquals(current.tracks.round, 6)
```

Check before running:
- If p3 is not the last seat of the round with p1 as the Chancellor, read
  `TurnOrder.order` and rest whichever seat ends the round.
- If p3's Rest parks a decision before the round ends, answer it, as
  `RestSuite` does.
- The log's player labels: `ImperialTacticsScenarioSuite` asserts
  `"p2 offered ..."`, so labels are player ids. Match whatever the
  formatter writes.

- [ ] **Step 2: Write the 1b-ii gap test**

1b-ii left one case untested: an Imperial attacker and an Imperial
defender in one Campaign. The defender's window opening clears the offers
made to the attacker. Add it to `PlanAidServiceSuite`:

```scala
  test("the defender's window clears the offers made to the attacker"):
    // p1 and p3 are Citizens and p2 the Chancellor. p1 raids p3. p2 holds
    // Encirclement and Shield Wall, offers Encirclement to p1, and can
    // lend Shield Wall to p3.
    val encirclement = DecisionOptionRef.Denizen(EncirclementCard.id)
    val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
    val (service, _) = Table.start.imperial(p2, p1, p3)
      .turn(p1, Phase.Act).pawn(p3, at = "Ancient City")
      .boardWarbands(p1, 5).boardWarbands(p3, 2).favor(p2, 2)
      .adviser(p2, EncirclementCard.id).adviser(p2, ShieldWall.cardId)
      .oathkeeper(None).service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    val raid = awaiting(started) match
      case Some((CampaignIds.kind, _)) => resolve(service, started, p1,
        CampaignIds.kind, ChooseOneAnswer(DecisionOptionRef.Button("raid")))
      case _ => started
    val forced = resolve(service, raid, p1, CampaignIds.force,
      ChooseAmountAnswer(4))
    assertEquals(awaiting(forced), Some((CampaignIds.attackerPlan, p1)))
    val offered = accepted(save(service, forced, p2,
      AidOffer(plans = Vector(encirclement))))
    assertEquals(Outcomes.ready(offered.state).planAid.window,
      Some(attacking))
    val finished = resolve(service, offered, p1, CampaignIds.attackerPlan,
      ChooseOneAnswer(CampaignIds.finish))
    assertEquals(awaiting(finished), Some((CampaignIds.defenderPlan, p3)))
    assert(finished.events.contains(OathEvent.PlanAidCleared),
      finished.events)
    assertEquals(Outcomes.ready(finished.state).planAid, PlanAidState())
    val defended = accepted(save(service, finished, p2,
      AidOffer(plans = Vector(shieldWall))))
    assertEquals(Outcomes.ready(defended.state).planAid.window,
      Some(AidWindow(p1, CampaignPlanSide.Defender, p3)))
```

Check before running:
- `AidWindow(actor, side, user)`: p1 is the Campaign's actor in both
  windows, so the suite's own `defending` value (p1's Campaign, p3
  defending) is the same window, and you may use it.
- Import `EncirclementCard` from `oathdigital.gameplay.powers.campaign`.
- `save`, `resolve` and `awaiting` are the suite's own helpers.
- If `finished.events` does not hold the clearing, look in the events of
  the step the service journaled for it, as "the offers clear when the
  Campaign's walk ends" does with `toCampaignEnd`.

- [ ] **Step 3: Run the suites**

Run: `./sbtw "testOnly oathdigital.application.EmpireEndingsScenarioSuite oathdigital.application.PlanAidServiceSuite"`
Expected: PASS. If the gap test fails, treat it as a defect in 1b-ii's
clearing (`PlanAidDesk.stale`), not in the test. Read the rule in
`PlanAidDesk.stale` and fix the code.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/application/EmpireEndingsScenarioSuite.scala \
  src/test/scala/distantlands/application/PlanAidServiceSuite.scala
git commit -m "test(empire): a Successor takes a Stable Regime win; aid clears between windows

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Docs and gates

**Files:**
- Modify: `docs/rules/ambiguities.md` (R17 after R16)
- Modify: `docs/rules/implementation-traceability.md` (rows 117, 119 and 141)
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md` (status line; R17 under "Rulings")
- Modify: `docs/ROADMAP.md` (delete the slice 2 item)

- [ ] **Step 1: Record the ruling**

In `docs/rules/ambiguities.md`, after R16:

```markdown
R17. **The Grand Scepter counts.** Under Supremacy, the Successor holds more relics than each other Imperial player (card OATH-265). The Grand Scepter is a relic, so it counts in its holder's total (CR p. 19; Empire slice 2).
```

In the spec's "Rulings" section, after R16:

```markdown
- **R17, the Grand Scepter counts.** Supremacy's Successor counts every
  relic each Imperial player holds, the Grand Scepter included.
```

Append to the spec's status line: ` Slice 2 implemented, 2026-11-01.`

- [ ] **Step 2: Update the traceability rows**

- **Row 117:** replace the closing "the Imperial endings are Empire slice
  2" with "the Imperial endings are in row 119".
- **Row 119:**
  - Retitle it "Rounds 5-8, Stable Regime, War Exhaustion and Successor".
  - Status: **Tested (bounded)**.
  - Evidence: `gameplay/EmpireEndings.scala` owns the end die's
    thresholds, War Exhaustion's four Empire steps and the Successor goals
    (R17). `StateBasedEvaluation.scala` journals `EndDieRolled` and
    replays it against the face. `RoundEndRandomPort.scala` rolls the die.
    The suites are `EmpireEndingsSuite`, `EmpireRoundEndSuite` and
    `EmpireEndingsScenarioSuite`.
  - Gaps: altered Foundations; legacies that change the roll or the
    Successor (Arbiter, Royal Line, Conspirator); and a journal that leaves
    out an owed `EndDieRolled`, which replay does not detect.
- **Row 141:** in the ports list, replace "the all-Exile War Exhaustion
  fallback" with "the end die and the all-Exile War Exhaustion fallback".

- [ ] **Step 3: Delete the roadmap item**

In `docs/ROADMAP.md`, under "Phase - Empire", delete the bullet
"**Slice 2, Empire endings.** ...".

- [ ] **Step 4: Run every gate**

```bash
./sbtw clean coverage test coverageReport
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```

Expected:
- every test passes;
- statement coverage is at least 92%;
- both checks pass.

- [ ] **Step 5: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/implementation-traceability.md \
  docs/superpowers/specs/2026-10-29-empire-phase-design.md docs/ROADMAP.md
git commit -m "docs(empire): record slice 2 and ruling R17

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Self-review notes

- Spec coverage:
  - Stable Regime: Tasks 3 and 5.
  - War Exhaustion's four steps: Tasks 3 and 5.
  - The no-Empire path stays unchanged: Task 5 keeps the old branch;
    `StateBasedEvaluationSuite` and `RestSuite` still run.
  - Successor and its table: Task 3.
  - The three `VictoryKind` cases: Task 2.
  - The end die in the Game Log: Task 4.
  - The victory screen: Task 6.
  - Traceability row 119: Task 8.
  - The spec's scenario: Task 7.
  - Wire round trips: Tasks 2 and 4.
- Crown Chancellor already makes a winning Citizen the Chancellor
  (`ChronicleCrown.newDynasty`). Swear an Oath reads only a Visionary win.
  Neither needs a change for the new kinds.
- "An Empire that starts in play (from the next end of round)" is slice
  3's. This slice checks for a Chancellor at the round's end, which is
  that rule's outcome.
