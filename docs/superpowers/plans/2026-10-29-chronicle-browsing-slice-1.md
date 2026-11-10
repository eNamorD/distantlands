# Chronicle Browsing Slice 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the server everything saved-campaign browsing needs: a named Chronicle with an era scorepad, canonical JSON with a digest and summary, a convert route, a create request that starts from a new or saved Chronicle, and a projection that hands every seat the evolved Chronicle.

**Architecture:**
- The model gains `Chronicle.name` and the era scorepad (`EraState.games`).
- `serialization/ChronicleJsonCodec` writes canonical, versioned JSON. The application layer reaches it through an `application/chronicle/ChronicleJson` trait loaded by `ServiceLoader`, as `GameEventCodec` already is, because `application` must not import `serialization`.
- `application/chronicle/ChronicleEntries` adds the SHA-256 digest and the summary. `ChronicleConversion` does the convert route's work: decode, card accounting, then encode as JSON and TTS.
- The shared protocol gains `ChronicleEntry`, the convert request and response, the create request's `chronicle` field, and the projection's `chronicle` field.
- The frontend changes only enough to keep creating games: it sends `New("Chronicle <date>")`. The Chronicle pages are slice 2.

**Tech Stack:** Scala 3, munit, ujson, Akka HTTP, Scala.js (frontend compile only).

**Spec:** `docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md`. The TTS codec spec, `docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md`, covers the `name` parameter and the omitted list.

## Global Constraints

- **Branch:** work in a worktree off `0.2.0-alpha`. Symlink the main checkout's `.tooling` and `node_modules` into it before running `./sbtw`. Merge back into `0.2.0-alpha` only when the user says so. Push nothing.
- **Commits:** never `git add -A`; add the files you changed by name. End every commit message with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Name:** `Chronicle.name` is trimmed and has 1 to 80 characters. `GameStarted` carries it inside the Chronicle, and `ChronicleUpdated` copies it forward unchanged.
- **Scorepad:** `EraState(targetScore: Int, games: Vector[EraGame])`, `EraGame(entries: Vector[EraEntry])`, `EraEntry(lineage: LineageId, totalScore: Int, endingRole: Role)`. A lineage's score is its latest entry's `totalScore`, or 0. `endingRole` is the role when the score is recorded, after the Sun has crowned (ruling 19).
- **JSON:** `"format": 1`. Encoding is canonical: one key order, no whitespace. Map and set fields are written sorted.
- **Digest:** SHA-256 of the canonical JSON, lowercase hex.
- **Summary:** `{name, game, eraTarget, scores: [{lineage, score}]}`. `game` is `games.size + 1`. It is absent when the scorepad has no games but the Chronicle has lineages. That case is a TTS import, whose game number is unknown. A first-game Chronicle has no lineages and gets `game` 1.
- **Convert:** `POST /chronicles/convert` is stateless. It takes `{json}` or `{tts, name}` and returns `{json, digest, summary, tts?}`. `tts` is absent when TTS cannot hold the Chronicle. A decode or accounting failure answers 422 with the reason.
- **Create:** the request's `chronicle` is `New(name)` or `Saved(json)`. A refusal answers 422 with the codec's or game start's reason. The response gains the starting Chronicle's `{json, digest, summary}`.
- **Projection:** every seat sees the Chronicle name and the starting Chronicle's digest from the start. Once `ChronicleUpdated` is journaled, every seat also sees the evolved Chronicle's `{json, digest, summary}`.
- **TTS:** `ChronicleTtsCodec.decode(text, name)`. The omitted list always starts with "the name". The scorepad entry reads "the era scorepad".
- **Old journals** need not decode. New fields are required.
- **Tests:** `BackendArchitectureSuite` forbids catalog id literals in tests, such as `RelicId("R01")`. Use `CatalogNames` or fixture values. Lineage ids like `LineageId("l9")` are not catalog ids and are fine.
- **Build rules:** the 800-line file cap, `-Werror`, `python3 scripts/check-architecture.py`, `python3 scripts/check-markdown-links.py`, and coverage of at least 92% statements (`./sbtw clean coverage test coverageReport`).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/GameState.scala` | `EraEntry`, `EraGame`, `EraState.games`, `EraState.score` |
| `src/main/scala/distantlands/model/Chronicle.scala` | `name`, `Chronicle.nameProblem` |
| `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala` | scoring writes the scorepad |
| `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` | the era is copied as it is |
| `src/main/scala/distantlands/serialization/GameEventJsonSupport.scala` | name, scorepad, sorted maps and sets |
| `src/main/scala/distantlands/serialization/ChronicleJsonCodec.scala` | new: canonical JSON |
| `src/main/resources/META-INF/services/distantlands.application.chronicle.ChronicleJson` | new: service registration |
| `src/main/scala/distantlands/application/chronicle/ChronicleJson.scala` | new: the JSON boundary trait |
| `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala` | new: digest and summary |
| `src/main/scala/distantlands/application/chronicle/ChronicleConversion.scala` | new: the convert route's work |
| `src/main/scala/distantlands/application/chronicle/ChronicleTtsCodec.scala` | `decode(text, name)`, omitted list |
| `src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala` | `name` parameter |
| `src/main/scala/distantlands/application/FirstGamePlanFactory.scala` | `ChronicleChoice` |
| `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala` | builds from a choice |
| `src/main/scala/distantlands/application/GameProjection.scala` | `chronicle` projection |
| `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala` | new: entry, summary, convert DTOs and codecs |
| `shared/src/main/scala/distantlands/protocol/TrustedGameProtocol.scala` | `ChronicleRequest`, request and response fields |
| `shared/src/main/scala/distantlands/protocol/TrustedGameProtocolCodec.scala` | encode and decode those fields |
| `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala` | `ChronicleProjection`, `GameProjection.chronicle` |
| `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala` | the `chronicle` field |
| `src/main/scala/distantlands/server/TrustedGameProvisioning.scala` | `New` and `Saved`, `Refused` |
| `src/main/scala/distantlands/server/TrustedSeatRoutes.scala` | convert route, refusal answer |
| `src/main/scala/distantlands/server/ServerRuntime.scala`, `ServerRoutes.scala` | wiring |
| `src/main/scala/distantlands/server/AuthenticatedGameRoutes.scala` | names its Chronicle |
| `frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala` | sends `New("Chronicle <date>")` |
| `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala` | new: the end-to-end walk, shared |

---

### Task 1: The era scorepad

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala:40-48`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala:65-74`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala:46-49`
- Modify: `src/main/scala/distantlands/serialization/GameEventJsonSupport.scala` (`encodeChronicle`'s `"era"`, `decodeEra`)
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleTtsCodec.scala` (`defaultEra`, `omitted`)
- Modify: `src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala:46`
- Create: `src/test/scala/distantlands/model/EraStateSuite.scala`
- Modify tests: `ChronicleStateOperationsSuite`, `GameStartRulesSuite`, `GameEventFactsWireSuite`, `ChronicleEndToEndSuite`, `ChronicleRecordSuite`, `ChronicleWindowsSuite`, `LineageScoringSuite`, `ChronicleTtsCodecSuite`, `FirstGameSetupFixture`, `TestGameFixtures`

**Interfaces:**
- Produces: `EraEntry(lineage: LineageId, totalScore: Int, endingRole: Role)`, `EraGame(entries: Vector[EraEntry])`, `EraState(targetScore: Int, games: Vector[EraGame])`, `EraState.score(lineage: LineageId): Int`.
- Wire shape of `era`: `{"targetScore": 20, "games": [{"entries": [{"lineage": "red-lineage", "totalScore": 3, "endingRole": "chancellor"}]}]}`.

**Scoring rule.** The Throne's `LineageScoring` emits one `LineagePointsScored` per player who scores. The first of them this game opens the game's `EraGame`. It holds one entry per seated lineage, in seating order (`current.players`), at that lineage's score before this game and its live role. Each `LineagePointsScored` then adds its points to its lineage's entry. "This game has opened its row" means `campaign.era.games.size > campaign.baseline.era.games.size`. Game start copies the Chronicle's era unchanged, so the two sizes are equal until the first points. Roles are read when the row opens, after the Sun has crowned (ruling 19).

- [ ] **Step 1: Write the failing model test**

Create `src/test/scala/distantlands/model/EraStateSuite.scala`:

```scala
package distantlands.model

/** The era scorepad (Chronicle browsing design, "The era scorepad"). */
class EraStateSuite extends munit.FunSuite:
  private val red = LineageId("red-lineage")
  private val blue = LineageId("blue-lineage")

  private def game(entries: (LineageId, Int, Role)*): EraGame =
    EraGame(entries.toVector.map(EraEntry.apply))

  test("a lineage's score is its latest entry's total, or 0 without one"):
    val era = EraState(20, Vector(
      game((red, 3, Role.Chancellor), (blue, 1, Role.Exile)),
      game((red, 5, Role.Chancellor))))
    assertEquals(era.score(red), 5)
    assertEquals(era.score(blue), 1)
    assertEquals(era.score(LineageId("pink-lineage")), 0)

  test("a game records each lineage once, with a non-negative total"):
    intercept[IllegalArgumentException](
      game((red, 3, Role.Exile), (red, 4, Role.Exile)))
    intercept[IllegalArgumentException](EraEntry(red, -1, Role.Exile))
    intercept[IllegalArgumentException](EraState(0, Vector.empty))
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./sbtw "testOnly distantlands.model.EraStateSuite"`
Expected: compile error, `Not found: type EraGame`.

- [ ] **Step 3: Replace `EraState` in `GameState.scala`**

```scala
/** One player's row for one game on the era scorepad: their lineage's
  * total after the game and their role when Score Lineage Points recorded
  * it, after the Sun has crowned (Chronicle ruling 19). */
final case class EraEntry(lineage: LineageId, totalScore: Int,
    endingRole: Role):
  require(totalScore >= 0, "lineage scores must be non-negative")

/** One game's column on the era scorepad: an entry per participating
  * player. */
final case class EraGame(entries: Vector[EraEntry]):
  require(entries.map(_.lineage).distinct.size == entries.size,
    "a game records each lineage once")

/** The era scorepad. Resetting `games` at an era's end belongs to the
  * Eras item. */
final case class EraState(targetScore: Int, games: Vector[EraGame]):
  require(targetScore > 0, "era target score must be positive")

  /** The lineage's latest total, or 0 before it has one. */
  def score(lineage: LineageId): Int =
    games.reverseIterator.flatMap(_.entries.find(_.lineage == lineage))
      .nextOption().fold(0)(_.totalScore)
```

- [ ] **Step 4: Run the model test to verify it passes**

Run: `./sbtw "testOnly distantlands.model.EraStateSuite"`
Expected: the suite compiles only once Step 5 fixes the main sources. If the project fails to compile, do Step 5 first, then rerun.

- [ ] **Step 5: Make scoring write the scorepad**

In `ChronicleStateOperations.scala`, replace `lineagePointsScored`:

```scala
  /** Only a seated lineage scores (gifts go to seated players, Chronicle
    * ruling 13). The game's first points open its scorepad row: an entry
    * per seated lineage, in seating order, at its total before this game
    * and its live role, which the Sun has already crowned (ruling 19). */
  def lineagePointsScored(ready: ReadyGame, lineage: LineageId, points: Int)
      : Either[OperationError, ReadyGame] =
    val campaign = ready.game.campaign
    if !campaign.lineages.contains(lineage) then
      Left(InvalidDescription(s"no seated lineage ${lineage.value} to score"))
    else
      val opened = campaign.era.games.size > campaign.baseline.era.games.size
      val era = if opened then campaign.era else campaign.era.copy(games =
        campaign.era.games :+ EraGame(ready.game.current.players
          .map(_.lineage).filter(campaign.lineages.contains).map(id =>
            EraEntry(id, campaign.era.score(id), campaign.lineages(id).role))))
      val game = era.games.last
      Right(ready.updateCampaign(_.copy(era = era.copy(games =
        era.games.init :+ EraGame(game.entries.map(entry =>
          if entry.lineage == lineage then
            entry.copy(totalScore = entry.totalScore + points)
          else entry))))))
```

In `GameStartRules.scala`, replace the era argument and its comment:

```scala
          chronicle.era,
```

Remove the comment line "A seated lineage the Chronicle has not scored yet starts at 0." A seated lineage with no entry already scores 0 through `EraState.score`.

In `FirstGameChronicleGenerator.scala`, `ChronicleTtsCodec.scala`, `FirstGameSetupFixture.scala` and `TestGameFixtures.scala`, replace every `EraState(20, Map.empty)` and `EraState(20, Map(lineageId -> 0))` with `EraState(20, Vector.empty)`.

- [ ] **Step 6: Encode and decode the scorepad**

In `GameEventJsonSupport.encodeChronicle`, replace the `"era"` entry:

```scala
      "era" -> ujson.Obj(
        "targetScore" -> chronicle.era.targetScore,
        "games" -> ujson.Arr.from(chronicle.era.games.map(game => ujson.Obj(
          "entries" -> ujson.Arr.from(game.entries.map(entry => ujson.Obj(
            "lineage" -> entry.lineage.value,
            "totalScore" -> entry.totalScore,
            "endingRole" -> encodeRole(entry.endingRole)))))))),
```

Replace `decodeEra`:

```scala
  private def decodeEra(value: ujson.Value, path: String)
      : Either[WireError, EraState] = for
    target <- safeIntField(value.obj, "targetScore", path)
    games <- traverse(value("games").arr.zipWithIndex.toVector):
      case (game, gameIndex) =>
        val gamePath = s"$path.games[$gameIndex]"
        for
          entries <- traverse(game("entries").arr.zipWithIndex.toVector):
            case (row, index) =>
              val rowPath = s"$gamePath.entries[$index]"
              for
                id <- stringField(row.obj, "lineage", rowPath)
                total <- safeIntField(row.obj, "totalScore", rowPath)
                _ <- Either.cond(total >= 0, (),
                  InvalidValue(s"$rowPath.totalScore", "must not be negative"))
                role <- decodeRole(row("endingRole").str, s"$rowPath.endingRole")
              yield EraEntry(LineageId(id), total, role)
          _ <- duplicate(entries.map(_.lineage)).toLeft(()).left.map(id =>
            InvalidValue(s"$gamePath.entries",
              s"duplicate lineage '${id.value}'"))
        yield EraGame(entries)
    era <- Either.cond(target > 0, EraState(target, games),
      InvalidValue(s"$path.targetScore", "must be positive"))
  yield era
```

- [ ] **Step 7: Update the TTS omitted list**

In `ChronicleTtsCodec.omitted`, replace the era scores entry:

```scala
    Option.when(chronicle.era.games.nonEmpty)("the era scorepad"),
```

- [ ] **Step 8: Update the tests that read `lineageScores`**

Make these exact changes.

`ChronicleStateOperationsSuite`, replace "lineage points add to the era score":

```scala
  test("the game's first lineage points open its scorepad row for every " +
      "seated lineage, with its role, and later points add to it"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, LineagePointsScored(lineage, 3),
      LineagePointsScored(lineage, 1)))
    val era = after.game.campaign.era
    assertEquals(era.games.size, ready.game.campaign.era.games.size + 1)
    assertEquals(era.games.last.entries.map(_.lineage),
      ready.game.current.players.map(_.lineage))
    assertEquals(era.score(lineage), 4)
    assertEquals(era.games.last.entries.find(_.lineage == lineage)
      .map(_.endingRole), Some(ready.game.campaign.lineages(lineage).role))
```

`GameStartRulesSuite`, replace "game start takes the era and Oathkeeper goal from the Chronicle":

```scala
  test("game start takes the era and Oathkeeper goal from the Chronicle"):
    val l9 = LineageId("l9")
    val scored = chronicle.copy(
      era = EraState(15, Vector(EraGame(Vector(
        EraEntry(l9, 6, Role.Citizen))))),
      oathkeeperGoal = OathkeeperGoal.Protection)
    val campaign = accepted(GameStartRules.evolve(catalog, scored, orders))
      .game.campaign
    assertEquals(campaign.oathkeeperGoal, OathkeeperGoal.Protection)
    assertEquals(campaign.era, scored.era)
    assertEquals(campaign.era.score(l9), 6)
    assertEquals(orders.participants.map(p => campaign.era.score(p.lineageId)),
      Vector.fill(orders.participants.size)(0))
```

`GameEventFactsWireSuite`:
- In "a GameStarted Chronicle round trips its era scores and Oathkeeper goal", use `EraState(targetScore = 15, games = Vector(EraGame(Vector(EraEntry(LineageId("l1"), 4, Role.Chancellor), EraEntry(LineageId("l9"), 7, Role.Exile)))))`.
- In the refusal test, rename it "...a zero era target, a doubled scorepad lineage and a negative total". Build the event from `EraState(20, Vector(EraGame(Vector(EraEntry(LineageId("l1"), 2, Role.Exile)))))` and replace the last assertion with:

```scala
    assert(refusedPath { c =>
      val entries = c("era")("games")(0)("entries")
      entries.arr.append(entries(0))
    }.endsWith(".era.games[0].entries"))
    assert(refusedPath(_("era")("games")(0)("entries")(0)("totalScore") = -1)
      .endsWith(".totalScore"))
```

`ChronicleEndToEndSuite`, replace `assertEquals(record.era.lineageScores.get(lineage), Some(3))` with:

```scala
    assertEquals(record.era.score(lineage), 3)
    assertEquals(record.era.games.last.entries.find(_.lineage == lineage)
      .map(_.endingRole), Some(Role.Chancellor))
```

`ChronicleWindowsSuite:45`: `.lineageScores.get(lineage), Some(windows.size)` becomes `.score(lineage), windows.size`.

`LineageScoringSuite:35`: `.lineageScores.getOrElse(lineage, 0)` becomes `.score(lineage)`. Do the same for every other `lineageScores.getOrElse(x, 0)` in the file.

`ChronicleRecordSuite:21`: the test builds a campaign era with `lineageScores = ...`. Replace it with `games = Vector(EraGame(Vector(EraEntry(<same lineage>, <same score>, <that lineage's role>))))`. Keep the test's assertion that the record carries the live era.

`ChronicleTtsCodecSuite`:
- `assertEquals(full.era, EraState(20, Map.empty))` becomes `EraState(20, Vector.empty)`.
- At line 59, `EraState(15, Map(lineageOf(PlayerColor.Red) -> 6))` becomes `EraState(15, Vector(EraGame(Vector(EraEntry(lineageOf(PlayerColor.Red), 6, Role.Exile)))))`.
- In that test's expected omitted list, "the era scores" becomes "the era scorepad".

Then search for any use the steps above missed:

Run: `grep -rn "lineageScores" src shared frontend`
Expected: no output.

- [ ] **Step 9: Run the affected suites**

Run: `./sbtw "testOnly distantlands.model.EraStateSuite distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.gameplay.setup.GameStartRulesSuite distantlands.serialization.GameEventFactsWireSuite distantlands.gameplay.chronicle.* distantlands.application.chronicle.*"`
Expected: all pass.

- [ ] **Step 10: Run the whole suite**

Run: `./sbtw test`
Expected: all pass.

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala \
  src/main/scala/distantlands/gameplay/setup/GameStartRules.scala \
  src/main/scala/distantlands/serialization/GameEventJsonSupport.scala \
  src/main/scala/distantlands/application/chronicle/ChronicleTtsCodec.scala \
  src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala \
  src/test/scala/distantlands/model/EraStateSuite.scala
git add -u src/test
git commit -m "feat(chronicle): record the era as a scorepad of games with each player's total and ending role

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

`git add -u src/test` stages only tracked test files you modified. Check `git status` before committing.

---

### Task 2: The Chronicle's name and the plan factory's choice

**Files:**
- Modify: `src/main/scala/distantlands/model/Chronicle.scala`
- Modify: `src/main/scala/distantlands/serialization/GameEventJsonSupport.scala` (`encodeChronicle`, `decodeChronicle`)
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleTtsCodec.scala`
- Modify: `src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala`
- Modify: `src/main/scala/distantlands/application/FirstGamePlanFactory.scala`
- Modify: `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`
- Modify: `src/main/scala/distantlands/server/TrustedGameProvisioning.scala:33`
- Modify: `src/main/scala/distantlands/server/AuthenticatedGameRoutes.scala:92`
- Create: `src/test/scala/distantlands/model/ChronicleSuite.scala`
- Modify tests: `FirstGameSetupFixture`, `TestGameFixtures`, `GameEventFactsWireSuite`, `ChronicleTtsCodecSuite`, `GeneratedFirstGamePlanFactorySuite`, `GameServerGatewaySubmitBeginSuite`, `ChronicleEndToEndSuite`, and every caller of `FirstGameChronicleGenerator.generate` (`grep -rn "FirstGameChronicleGenerator.generate" src/test`).

**Interfaces:**
- Produces:
  - `Chronicle(name: String, atlasBox, ...)`, with `name` as the first parameter.
  - `Chronicle.nameProblem(name: String): Option[String]` and `Chronicle.maxNameLength: Int = 80`.
  - `ChronicleTtsCodec.decode(text: String, name: String): Either[String, Chronicle]`.
  - `FirstGameChronicleGenerator.generate(catalog, name: String, implemented, random, policy)`.
  - `enum ChronicleChoice { case New(name: String); case Saved(chronicle: Chronicle) }` in `distantlands.application`.
  - `FirstGamePlanFactory.build(config: FirstGameBootstrapConfig, choice: ChronicleChoice): Either[BootstrapPlanFailure, FirstGamePlan]`.
- Wire: `"name"` is the first key of an encoded Chronicle.

- [ ] **Step 1: Write the failing name test**

Create `src/test/scala/distantlands/model/ChronicleSuite.scala`:

```scala
package distantlands.model

import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle

/** A Chronicle's name (Chronicle browsing design, "Model"). */
class ChronicleSuite extends munit.FunSuite:
  test("a name is trimmed and has 1 to 80 characters"):
    assertEquals(Chronicle.nameProblem("a" * 80), None)
    assertEquals(Chronicle.nameProblem(""), Some("a Chronicle needs a name"))
    assertEquals(Chronicle.nameProblem(" Ash"),
      Some("a Chronicle name has no leading or trailing spaces"))
    assertEquals(Chronicle.nameProblem("a" * 81),
      Some("a Chronicle name has at most 80 characters"))

  test("a Chronicle refuses a name with a problem"):
    intercept[IllegalArgumentException](chronicle.copy(name = "Ash "))
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./sbtw "testOnly distantlands.model.ChronicleSuite"`
Expected: compile error, `value nameProblem is not a member of object distantlands.model.Chronicle`.

- [ ] **Step 3: Add the name to the model**

In `Chronicle.scala`:
- Add `name: String,` as the first parameter of `Chronicle`.
- Add this body after the parameter list:

```scala
):
  Chronicle.nameProblem(name).foreach(problem =>
    throw new IllegalArgumentException(problem))

object Chronicle:
  val maxNameLength: Int = 80

  /** Why `name` cannot name a Chronicle, if it cannot: it is trimmed and
    * has 1 to 80 characters (Chronicle browsing design, "Model"). */
  def nameProblem(name: String): Option[String] =
    if name != name.trim then
      Some("a Chronicle name has no leading or trailing spaces")
    else if name.isEmpty then Some("a Chronicle needs a name")
    else if name.length > maxNameLength then
      Some(s"a Chronicle name has at most $maxNameLength characters")
    else None
```

- Add one sentence to the class scaladoc: "`name` is the Chronicle's identity in a browser, which keeps its names unique; `ChronicleUpdated` copies it forward unchanged."

Update the constructor calls:
- `ChronicleTtsCodec.fromTts`'s positional `Chronicle(...)` gets `name` first.
- `FirstGameSetupFixture.chronicle` gets `name = "Fixture Chronicle"`.
- `TestGameFixtures`' baseline gets `name = "Test Chronicle"`.

- [ ] **Step 4: Encode and decode the name**

In `encodeChronicle`, make `"name" -> chronicle.name` the first entry of the `ujson.Obj`. In `decodeChronicle`, add these generator lines before `era <-`:

```scala
    name <- stringField(value.obj, "name", path)
    _ <- Chronicle.nameProblem(name).toLeft(()).left.map(problem =>
      InvalidValue(s"$path.name", problem))
```

Pass `name = name` to the `Chronicle(...)` in the `yield`.

Add to `GameEventFactsWireSuite`:

```scala
  test("a GameStarted Chronicle round trips its name and refuses a blank one"):
    val event = OathEvent.GameStarted(chronicle.copy(name = "Ashen Crown"),
      orders)
    val encoded = accepted(GameEventWire.encodeEvent("named", 0L, event))
    assertEquals(encoded("payload")("chronicle")("name").str, "Ashen Crown")
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)
    encoded("payload")("chronicle")("name") = ""
    rejected(GameEventWire.decode(encoded)) match
      case WireError.InvalidValue(path, message) =>
        assert(path.endsWith(".name"), path)
        assertEquals(message, "a Chronicle needs a name")
      case other => fail(other.toString)
```

- [ ] **Step 5: Give the TTS decoder the name**

In `ChronicleTtsCodec`:

```scala
  /** `name` becomes the Chronicle's name: TTS carries none. */
  def decode(text: String, name: String): Either[String, Chronicle] =
    TtsWire.decodeExport(text).flatMap((_, tts) => fromTts(tts, name))
```

- `fromTts` gains a `name: String` parameter.
- Its `for` starts with `_ <- Chronicle.nameProblem(name).toLeft(())`.
- Its `yield` passes `name` first.
- `omitted` starts its vector with `Some("the name"),`.
- Update the class scaladoc. Decoding takes the name from the caller, and the omitted list always names it.

In `ChronicleTtsCodecSuite`:
- `codec.decode(x)` becomes `codec.decode(x, "Imported")`.
- In `roundTripped(c)`, decode with `c.name`, so round trips still compare equal.
- In "the first-game fixture round trips and loses nothing", rename the test to "the first-game fixture round trips, losing only its name". It asserts `assertEquals(exported.omitted, Vector("the name"))`.
- Every other expected omitted list gains "the name" first.
- Add this test:

```scala
  test("an import takes its name from the caller and refuses a blank one"):
    assertEquals(codec.decode(fullExport, "Harsch's game").map(_.name),
      Right("Harsch's game"))
    assertEquals(codec.decode(fullExport, " "),
      Left("a Chronicle name has no leading or trailing spaces"))
```

- [ ] **Step 6: Generate a named Chronicle and build from a choice**

`FirstGameChronicleGenerator`:
- `generate` gains `name: String` after `catalog`.
- Add the failure `final case class InvalidName(reason: String) extends ChronicleGeneratorFailure`.
- Start the `for` with `_ <- Chronicle.nameProblem(name).map(InvalidName(_)).toLeft(())`.
- Pass `name` first to `Chronicle(...)`.

`FirstGamePlanFactory.scala`, add the choice and change `build`:

```scala
/** What a new game starts from (Chronicle browsing design, "Create
  * request"): a fresh first-game Chronicle under a name, or a saved one. */
enum ChronicleChoice:
  case New(name: String)
  case Saved(chronicle: Chronicle)

trait FirstGamePlanFactory:
  def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan]
```

`GeneratedFirstGamePlanFactory.build`:

```scala
  override def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan] =
    val resolvedConfig = shuffledSeating(config)
    val chronicle = choice match
      case ChronicleChoice.New(name) =>
        FirstGameChronicleGenerator.generate(catalog, name,
            PowerImplementationStatus.implemented(catalog), random, policy)
          .left.map {
            case ChronicleGeneratorFailure.InvalidName(reason) =>
              BootstrapPlanFailure(reason)
            case failure => BootstrapPlanFailure(failure.toString)
          }
      case ChronicleChoice.Saved(saved) => Right(saved)
    chronicle.map(FirstGamePlan(_, resolvedConfig))
```

Update the class scaladoc. A saved Chronicle is used as it is, and only the seating is shuffled.

Callers:
- `TrustedGameProvisioning.scala:33` becomes `planFactory.build(config, ChronicleChoice.New("Chronicle"))`. Task 5 replaces the placeholder with the request's choice.
- `AuthenticatedGameRoutes.scala:92` becomes `planFactory.build(config, ChronicleChoice.New(s"Chronicle $gameId"))`. That route has no browser to name it.
- `GameServerGatewaySubmitBeginSuite:38` becomes `.build(config, ChronicleChoice.New("Gateway Chronicle"))`.
- Other tests calling `build(config)` or `generate(...)` pass `ChronicleChoice.New("Test Chronicle")` or `name = "Test Chronicle"`.

Add to `GeneratedFirstGamePlanFactorySuite`:

```scala
  test("a new Chronicle carries its name, and a blank name is refused"):
    assertEquals(accepted(factory.build(config,
      ChronicleChoice.New("Ashen Crown"))).chronicle.name, "Ashen Crown")
    assertEquals(factory.build(config, ChronicleChoice.New("")).left.map(
      _.message), Left("a Chronicle needs a name"))

  test("a saved Chronicle is used as it is, with the seating shuffled"):
    val saved = FirstGameSetupFixture.chronicle
    val plan = accepted(factory.build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.chronicle, saved)
    assertEquals(plan.resolvedConfig.participants.toSet,
      config.participants.toSet)
```

Import `distantlands.gameplay.setup.FirstGameSetupFixture` if the suite does not already.

In `ChronicleEndToEndSuite`, after the `record` line, add:

```scala
    assertEquals(record.name, ready.game.campaign.baseline.name)
```

- [ ] **Step 7: Run the affected suites**

Run: `./sbtw "testOnly distantlands.model.* distantlands.serialization.GameEventFactsWireSuite distantlands.application.* distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: all pass.

- [ ] **Step 8: Run the whole suite**

Run: `./sbtw test`
Expected: all pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/model/Chronicle.scala \
  src/main/scala/distantlands/serialization/GameEventJsonSupport.scala \
  src/main/scala/distantlands/application/chronicle/ChronicleTtsCodec.scala \
  src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala \
  src/main/scala/distantlands/application/FirstGamePlanFactory.scala \
  src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala \
  src/main/scala/distantlands/server/TrustedGameProvisioning.scala \
  src/main/scala/distantlands/server/AuthenticatedGameRoutes.scala \
  src/test/scala/distantlands/model/ChronicleSuite.scala
git add -u src/test
git commit -m "feat(chronicle): name every Chronicle, and start a game from a new or saved one

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Canonical JSON, digest and summary

**Files:**
- Modify: `src/main/scala/distantlands/serialization/GameEventJsonSupport.scala` (`encodeChronicle`'s `"foundations"`, `encodeFoundationState`)
- Create: `src/main/scala/distantlands/application/chronicle/ChronicleJson.scala`
- Create: `src/main/scala/distantlands/serialization/ChronicleJsonCodec.scala`
- Create: `src/main/resources/META-INF/services/distantlands.application.chronicle.ChronicleJson`
- Create: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`
- Create: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala`
- Create: `src/test/scala/distantlands/serialization/ChronicleJsonCodecSuite.scala`
- Create: `src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala`
- Create: `shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala`

**Interfaces:**
- Consumes: `Chronicle.name`, `EraState.score`, `EraState.games` (Tasks 1-2).
- Produces:
  - `trait ChronicleJson { def encode(chronicle: Chronicle): String; def decode(json: String): Either[String, Chronicle] }` and `ChronicleJson.default: ChronicleJson`, in `distantlands.application.chronicle`.
  - `ChronicleScore(lineage: String, score: Int)`, `ChronicleSummary(name: String, game: Option[Int], eraTarget: Int, scores: Vector[ChronicleScore])` and `ChronicleEntry(json: String, digest: String, summary: ChronicleSummary)`, in `distantlands.protocol`.
  - `ChronicleEntryCodec.fields: Set[String]`, `encodeValue(entry): ujson.Obj`, `decodeValue(raw, path): Either[ProtocolDecodeFailure, ChronicleEntry]` and `decodeFields(value: ujson.Obj, path): Either[ProtocolDecodeFailure, ChronicleEntry]`. `decodeFields` reads without checking for extra keys.
  - `final class ChronicleEntries(json: ChronicleJson = ChronicleJson.default)` with `entry(chronicle): ChronicleEntry` and `json: ChronicleJson`. Its companion has `digest(text): String` and `summary(chronicle): ChronicleSummary`.
  - `EvolvedChronicleFixture.rules`, `.won`, `.done` and `.record: Chronicle`, in test package `distantlands.gameplay.chronicle`.

- [ ] **Step 1: Share the end-to-end walk**

Create `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala`. Move `rules`, `usurperWakes` and the walk out of `ChronicleEndToEndSuite`'s first test into it:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** A game won for real: p1 is the Usurper as their Wake begins, wins, and
  * answers every Chronicle task to the journaled Chronicle. */
object EvolvedChronicleFixture:
  val rules: OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val usurperWakes: OathState = Table.start.turn(p1, Phase.Wake)
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  /** The Rest that ends the game, up to Start Chronicle. */
  lazy val won: OathTransition = accepted(rules.next(
    OathTransition(usurperWakes, Vector.empty), PhaseTransitionRef.FinishRest))

  /** Start Chronicle and every task, answered. */
  lazy val done: OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.resolveWalker(
      won.state, p1, ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start))))

  /** The Chronicle the walk journals. */
  lazy val record: Chronicle = RecordedOps.all(done.events).collect {
    case ChronicleUpdated(chronicle) => chronicle
  }.last
```

Adjust the types to what `rules.next` and `ChronicleWalk.answeringEverything` return. Check them in the suite, and keep the suite's assertions unchanged. In `ChronicleEndToEndSuite`, import `EvolvedChronicleFixture.*` and drop the moved lines. The suite's other tests may use `rules`, so keep that name.

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: PASS.

- [ ] **Step 2: Write the failing codec test**

Create `src/test/scala/distantlands/serialization/ChronicleJsonCodecSuite.scala`:

```scala
package distantlands.serialization

import distantlands.application.chronicle.ChronicleJson
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._

/** Canonical Chronicle JSON (Chronicle browsing design, "ChronicleJsonCodec"). */
class ChronicleJsonCodecSuite extends munit.FunSuite:
  private val codec = ChronicleJsonCodec()

  test("the first-game and the evolved Chronicle round trip"):
    assertEquals(codec.decode(codec.encode(chronicle)), Right(chronicle))
    val evolved = EvolvedChronicleFixture.record
    assertEquals(codec.decode(codec.encode(evolved)), Right(evolved))

  test("encoding is compact, versioned and starts with the name"):
    val text = codec.encode(chronicle)
    assert(text.startsWith("""{"format":1,"name":"Fixture Chronicle","""),
      text)
    assert(!text.contains(" \"") && !text.contains("\n"), text)

  test("encoding does not depend on map or set insertion order"):
    val altered = FoundationState(FoundationFace.Altered, Set.empty)
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    val one = chronicle.copy(foundations = Map(
      FoundationNumber.IV -> altered, FoundationNumber.I -> normal))
    val other = chronicle.copy(foundations = Map(
      FoundationNumber.I -> normal, FoundationNumber.IV -> altered))
    assertEquals(codec.encode(one), codec.encode(other))

  test("decoding refuses another format, malformed text and a blank name"):
    val text = codec.encode(chronicle)
    assertEquals(codec.decode(text.replace("\"format\":1", "\"format\":2")),
      Left("Chronicle format 2 is not supported"))
    assert(codec.decode("{").left.exists(_.startsWith("not a Chronicle file")))
    assertEquals(codec.decode(text.replace("Fixture Chronicle", "")),
      Left("$.name: a Chronicle needs a name"))

  test("the application finds this codec through ServiceLoader"):
    assert(ChronicleJson.default.isInstanceOf[ChronicleJsonCodec])
```

If `FoundationNumber.I` is not a case name, use the enum's real names. Check `World.scala:138`. The test needs two Foundations whose insertion order differs from number order. Also check whether `alterationSources` is a `Set` of a type with `.value`. If so, add a third map in the test with two sources inserted in both orders.

- [ ] **Step 3: Run it to verify it fails**

Run: `./sbtw "testOnly distantlands.serialization.ChronicleJsonCodecSuite"`
Expected: compile error, `Not found: ChronicleJsonCodec`.

- [ ] **Step 4: Add the boundary trait**

Create `src/main/scala/distantlands/application/chronicle/ChronicleJson.scala`:

```scala
package distantlands.application.chronicle

import distantlands.model.Chronicle

/** The application's boundary to canonical Chronicle JSON (Chronicle
  * browsing design, "ChronicleJsonCodec"). `serialization` implements it;
  * `application` may not import that package, so it finds the
  * implementation through `ServiceLoader`, as it finds `GameEventCodec`. */
trait ChronicleJson:
  /** Canonical: one key order, no whitespace, so the digest is stable. */
  def encode(chronicle: Chronicle): String
  /** The reason is shown to the player who sent the text. */
  def decode(json: String): Either[String, Chronicle]

object ChronicleJson:
  lazy val default: ChronicleJson =
    val providers = java.util.ServiceLoader.load(classOf[ChronicleJson])
      .iterator()
    if providers.hasNext then providers.next()
    else throw new IllegalStateException("no ChronicleJson adapter is installed")
```

- [ ] **Step 5: Sort maps and sets in the Chronicle encoding**

In `GameEventJsonSupport.encodeChronicle`, sort the Foundations by number:

```scala
      "foundations" -> ujson.Arr.from(chronicle.foundations.toVector
        .sortBy(_._1.value)
        .map { case (number, state) => encodeFoundationState(number, state) }),
```

In `encodeFoundationState`, sort the sources: `state.alterationSources.toVector.map(_.value).sorted`.

- [ ] **Step 6: Write the codec and register it**

Create `src/main/scala/distantlands/serialization/ChronicleJsonCodec.scala`:

```scala
package distantlands.serialization

import scala.util.control.NonFatal

import distantlands.application.chronicle.ChronicleJson
import distantlands.model.Chronicle

/** A Chronicle as a file a player keeps (Chronicle browsing design,
  * "ChronicleJsonCodec"): the event wire's Chronicle object behind a
  * `"format"` key, written compact. */
final class ChronicleJsonCodec extends ChronicleJson with GameEventJsonSupport:
  def encode(chronicle: Chronicle): String =
    ujson.write(ujson.Obj.from(
      ("format" -> ujson.Num(ChronicleJsonCodec.format)) +:
        encodeChronicle(chronicle).obj.toSeq))

  def decode(json: String): Either[String, Chronicle] =
    try
      val value = ujson.read(json)
      for
        format <- value.objOpt.flatMap(_.get("format")).flatMap(_.numOpt)
          .toRight("a Chronicle file names its format")
        _ <- Either.cond(format == ChronicleJsonCodec.format, (),
          s"Chronicle format ${format.toInt} is not supported")
        chronicle <- decodeChronicle(value, "$").left.map(error =>
          s"${error.path}: ${error.message}")
      yield chronicle
    catch
      case NonFatal(error) => Left("not a Chronicle file: " +
        Option(error.getMessage).getOrElse("malformed JSON"))

object ChronicleJsonCodec:
  val format: Int = 1
```

Create `src/main/resources/META-INF/services/distantlands.application.chronicle.ChronicleJson` with one line:

```
distantlands.serialization.ChronicleJsonCodec
```

If `GameEventJsonSupport` declares abstract members, implement them the way `LifecycleEventCodec` does.

- [ ] **Step 7: Run the codec test to verify it passes**

Run: `./sbtw "testOnly distantlands.serialization.ChronicleJsonCodecSuite"`
Expected: PASS.

- [ ] **Step 8: Write the failing protocol and entries tests**

Create `shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala`:

```scala
package distantlands.protocol

/** The Chronicle entry a browser keeps (Chronicle browsing design,
  * "Summary"). */
class ChronicleProtocolSuite extends munit.FunSuite:
  private val entry = ChronicleEntry("{}", "ab12", ChronicleSummary(
    "Ashen Crown", Some(2), 20, Vector(ChronicleScore("red-lineage", 3))))

  private def roundTripped(value: ChronicleEntry) =
    ChronicleEntryCodec.decodeValue(
      ujson.read(ujson.write(ChronicleEntryCodec.encodeValue(value))), "$")

  test("an entry round trips, with and without a game number"):
    assertEquals(roundTripped(entry), Right(entry))
    val imported = entry.copy(summary = entry.summary.copy(game = None))
    assertEquals(roundTripped(imported), Right(imported))
    assert(!ChronicleEntryCodec.encodeValue(imported)("summary").obj
      .contains("game"))

  test("an entry refuses an unknown field"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("extra") = 1
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)
```

Create `src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala`:

```scala
package distantlands.application.chronicle

import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._
import distantlands.protocol.ChronicleScore

/** Digest and summary (Chronicle browsing design, "Digest" and "Summary"). */
class ChronicleEntriesSuite extends munit.FunSuite:
  private val entries = ChronicleEntries()

  test("the digest is the SHA-256 of the text, in lowercase hex"):
    assertEquals(ChronicleEntries.digest("abc"),
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")

  test("an entry's digest is stable across a decode and encode"):
    val entry = entries.entry(chronicle)
    val reread = entries.json.decode(entry.json).fold(fail(_), identity)
    assertEquals(entries.entry(reread), entry)
    assertEquals(entry.digest, ChronicleEntries.digest(entry.json))

  test("a first-game Chronicle's summary is game 1 with no scores"):
    val summary = ChronicleEntries.summary(chronicle)
    assertEquals(summary.name, "Fixture Chronicle")
    assertEquals(summary.game, Some(1))
    assertEquals(summary.eraTarget, 20)
    assertEquals(summary.scores, Vector.empty)

  test("an evolved Chronicle's summary is game 2 with each lineage's score"):
    val evolved = EvolvedChronicleFixture.record
    val summary = ChronicleEntries.summary(evolved)
    assertEquals(summary.game, Some(2))
    assertEquals(summary.scores, evolved.lineages.map(lineage =>
      ChronicleScore(lineage.id.value, evolved.era.score(lineage.id))))
    assert(summary.scores.exists(_.score == 3), summary.scores)

  test("a Chronicle with lineages but no scorepad has no game number"):
    val imported = EvolvedChronicleFixture.record.copy(
      era = EraState(20, Vector.empty))
    assertEquals(ChronicleEntries.summary(imported).game, None)
```

- [ ] **Step 9: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleProtocolSuite distantlands.application.chronicle.ChronicleEntriesSuite"`
Expected: compile error, `Not found: ChronicleEntry`.

- [ ] **Step 10: Add the protocol types and codec**

Create `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`:

```scala
package distantlands.protocol

import distantlands.protocol.projection.ProjectionCodecSupport._

final case class ChronicleScore(lineage: String, score: Int)

/** What a browser lists for a Chronicle without decoding it (Chronicle
  * browsing design, "Summary"). `game` is the next game's number; it is
  * absent when the Chronicle has lineages but no scorepad, as a TTS import
  * does. */
final case class ChronicleSummary(name: String, game: Option[Int],
    eraTarget: Int, scores: Vector[ChronicleScore])

/** A Chronicle as a browser keeps it: canonical JSON, the JSON's SHA-256
  * digest, and its summary. */
final case class ChronicleEntry(json: String, digest: String,
    summary: ChronicleSummary)

object ChronicleEntryCodec:
  val fields: Set[String] = Set("json", "digest", "summary")

  def encodeValue(entry: ChronicleEntry): ujson.Obj =
    val summary = ujson.Obj("name" -> entry.summary.name,
      "eraTarget" -> entry.summary.eraTarget,
      "scores" -> encoded(entry.summary.scores)(score => ujson.Obj(
        "lineage" -> score.lineage, "score" -> score.score)))
    entry.summary.game.foreach(game => summary("game") = game)
    ujson.Obj("json" -> entry.json, "digest" -> entry.digest,
      "summary" -> summary)

  def decodeValue(raw: ujson.Value, path: String): Result[ChronicleEntry] =
    for
      value <- obj(raw, path); _ <- exact(value, fields, path)
      entry <- decodeFields(value, path)
    yield entry

  /** The entry's fields, leaving any other keys to the caller. */
  def decodeFields(value: ujson.Obj, path: String): Result[ChronicleEntry] =
    for
      json <- string(value, "json", path)
      digest <- string(value, "digest", path)
      summaryRaw <- field(value, "summary", path)
      summaryPath = s"$path.summary"
      summary <- obj(summaryRaw, summaryPath)
      _ <- exact(summary, Set("name", "game", "eraTarget", "scores"),
        summaryPath)
      name <- string(summary, "name", summaryPath)
      game <- optionalAbsent(summary, "game", summaryPath)(int)
      target <- int(summary, "eraTarget", summaryPath)
      scoreRaws <- array(summary, "scores", summaryPath)
      scores <- traverse(scoreRaws, s"$summaryPath.scores") { (raw, child) =>
        for
          row <- obj(raw, child)
          _ <- exact(row, Set("lineage", "score"), child)
          lineage <- string(row, "lineage", child)
          score <- int(row, "score", child)
        yield ChronicleScore(lineage, score)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores))
```

`ProjectionCodecSupport` is `private[protocol]`, so it can be used from `distantlands.protocol`. If `exact` also refuses missing keys, the optional `game` breaks it. In that case, check only for unexpected keys here: `summary.value.keys.find(!allowed(_))`.

- [ ] **Step 11: Add digest and summary**

Create `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`:

```scala
package distantlands.application.chronicle

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import distantlands.model.Chronicle
import distantlands.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}

/** A Chronicle as a browser keeps it (Chronicle browsing design, "Digest"
  * and "Summary"). */
final class ChronicleEntries(val json: ChronicleJson = ChronicleJson.default):
  def entry(chronicle: Chronicle): ChronicleEntry =
    val text = json.encode(chronicle)
    ChronicleEntry(text, ChronicleEntries.digest(text),
      ChronicleEntries.summary(chronicle))

object ChronicleEntries:
  /** SHA-256 of `text` as UTF-8, in lowercase hex. */
  def digest(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF_8))
      .map(byte => f"${byte & 0xff}%02x").mkString

  def summary(chronicle: Chronicle): ChronicleSummary =
    val era = chronicle.era
    ChronicleSummary(chronicle.name,
      // A TTS import keeps lineages but no scorepad: its game is unknown.
      Option.when(era.games.nonEmpty || chronicle.lineages.isEmpty)(
        era.games.size + 1),
      era.targetScore,
      chronicle.lineages.map(lineage =>
        ChronicleScore(lineage.id.value, era.score(lineage.id))))
```

- [ ] **Step 12: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleProtocolSuite distantlands.application.chronicle.* distantlands.serialization.* distantlands.gameplay.chronicle.*"`
Expected: all pass.

Run: `python3 scripts/check-architecture.py`
Expected: no errors. `application/chronicle` imports only `model` and `protocol`.

- [ ] **Step 13: Commit**

```bash
git add src/main/scala/distantlands/serialization/GameEventJsonSupport.scala \
  src/main/scala/distantlands/serialization/ChronicleJsonCodec.scala \
  src/main/resources/META-INF/services/distantlands.application.chronicle.ChronicleJson \
  src/main/scala/distantlands/application/chronicle/ChronicleJson.scala \
  src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala \
  shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala \
  shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala \
  src/test/scala/distantlands/serialization/ChronicleJsonCodecSuite.scala \
  src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala
git commit -m "feat(chronicle): write Chronicles as canonical JSON with a digest and summary

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The convert route

**Files:**
- Create: `src/main/scala/distantlands/application/chronicle/ChronicleConversion.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala` (convert request and response)
- Modify: `src/main/scala/distantlands/server/TrustedSeatRoutes.scala`
- Modify: `src/main/scala/distantlands/server/ServerRuntime.scala`, `src/main/scala/distantlands/server/ServerRoutes.scala`
- Create: `src/test/scala/distantlands/application/chronicle/ChronicleConversionSuite.scala`
- Modify: `shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala`
- Modify: `src/test/scala/distantlands/server/TrustedSeatRoutesSuite.scala`

**Interfaces:**
- Consumes: `ChronicleEntries`, `ChronicleJson`, `ChronicleTtsCodec.decode(text, name)`, `ChronicleAccounting.knownAndUnique(catalog, chronicle): Either[String, Unit]`.
- Produces:
  - `final case class ConvertedChronicle(entry: ChronicleEntry, tts: Option[String])`.
  - `final class ChronicleConversion(catalog: ExecutableCatalog, json: ChronicleJson = ChronicleJson.default)` with:
    - `fromJson(text: String): Either[String, ConvertedChronicle]`;
    - `fromTts(text: String, name: String): Either[String, ConvertedChronicle]`;
    - `decode(json: String): Either[String, Chronicle]`, which runs the accounting;
    - `entry(chronicle: Chronicle): ChronicleEntry`.
  - `enum ChronicleConvertRequest { case FromJson(json: String); case FromTts(tts: String, name: String) }`, `ChronicleConvertResponse(entry: ChronicleEntry, tts: Option[String])`, `ChronicleConvertRequestCodec` and `ChronicleConvertResponseCodec`, each with `encode` and `decode`.
  - `ServerRuntime.chronicles: ChronicleConversion`.
  - `TrustedSeatRoutes(identities, provisioning, gateway, chronicles, publicBaseUrl, blockingExecutionContext, extraOrigins)`.

- [ ] **Step 1: Write the failing conversion test**

Create `src/test/scala/distantlands/application/chronicle/ChronicleConversionSuite.scala`:

```scala
package distantlands.application.chronicle

import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle

/** The convert route's work (Chronicle browsing design, "POST
  * /chronicles/convert"). */
class ChronicleConversionSuite extends munit.FunSuite:
  private val conversion = ChronicleConversion(FirstGameSetupFixture.catalog)
  private val entries = ChronicleEntries()
  private val tts = ChronicleTtsCodec(FirstGameSetupFixture.catalog)

  test("JSON converts to its entry and its TTS string"):
    val json = entries.entry(chronicle).json
    val converted = conversion.fromJson(json).fold(fail(_), identity)
    assertEquals(converted.entry, entries.entry(chronicle))
    assertEquals(converted.tts, tts.encode(chronicle).toOption.map(_.text))

  test("a TTS string converts under the name it is given, trimmed"):
    val text = tts.encode(chronicle).fold(fail(_), _.text)
    val converted = conversion.fromTts(text, "  Imported ")
      .fold(fail(_), identity)
    assertEquals(converted.entry.summary.name, "Imported")
    assertEquals(converted.entry, entries.entry(chronicle.copy(
      name = "Imported")))

  test("a Chronicle TTS cannot hold converts without a TTS string"):
    val unscored = chronicle.copy(lineages = Vector(
      distantlands.model.LineageState(distantlands.model.LineageId("purple"),
        None, distantlands.model.Role.Citizen, Vector.empty, Vector.empty)))
    val converted = conversion.fromJson(entries.entry(unscored).json)
      .fold(fail(_), identity)
    assertEquals(converted.tts, None)

  test("a Chronicle that stores a card twice is refused with the reason"):
    val doubled = chronicle.copy(relicDeck =
      chronicle.relicDeck :+ chronicle.relicDeck.head)
    val reason = conversion.fromJson(entries.entry(doubled).json)
    assert(reason.isLeft, reason)
    assertEquals(conversion.decode(entries.entry(doubled).json), reason.map(_ =>
      doubled))

  test("text that is not a Chronicle is refused with the codec's reason"):
    assert(conversion.fromJson("{").left.exists(
      _.startsWith("not a Chronicle file")))
    assert(conversion.fromTts("nonsense", "Imported").isLeft)
```

A lineage id with no TTS color makes TTS encoding fail, as in "lineage x has no TTS color". Check that the accounting accepts that lineage. If it does not, use a lineage that keeps a Vision instead. The codec suite has one.

- [ ] **Step 2: Run it to verify it fails**

Run: `./sbtw "testOnly distantlands.application.chronicle.ChronicleConversionSuite"`
Expected: compile error, `Not found: ChronicleConversion`.

- [ ] **Step 3: Write the conversion**

Create `src/main/scala/distantlands/application/chronicle/ChronicleConversion.scala`:

```scala
package distantlands.application.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.setup.ChronicleAccounting
import distantlands.model.Chronicle
import distantlands.protocol.ChronicleEntry

/** A converted Chronicle: the entry a browser keeps, and the TTS string
  * when TTS can hold the Chronicle. */
final case class ConvertedChronicle(entry: ChronicleEntry, tts: Option[String])

/** The convert route's work (Chronicle browsing design, "POST
  * /chronicles/convert"): read a Chronicle from JSON or TTS, check that
  * every card is known and stored once, and write it back both ways. */
final class ChronicleConversion(catalog: ExecutableCatalog,
    json: ChronicleJson = ChronicleJson.default):
  private val entries = ChronicleEntries(json)
  private val tts = ChronicleTtsCodec(catalog)

  def fromJson(text: String): Either[String, ConvertedChronicle] =
    decode(text).map(converted)

  /** TTS carries no name, so the caller gives one; it is trimmed. */
  def fromTts(text: String, name: String): Either[String, ConvertedChronicle] =
    tts.decode(text, name.trim).flatMap(checked).map(converted)

  /** A saved Chronicle's JSON, checked as the convert route checks it. */
  def decode(text: String): Either[String, Chronicle] =
    json.decode(text).flatMap(checked)

  def entry(chronicle: Chronicle): ChronicleEntry = entries.entry(chronicle)

  private def checked(chronicle: Chronicle): Either[String, Chronicle] =
    ChronicleAccounting.knownAndUnique(catalog, chronicle).map(_ => chronicle)

  private def converted(chronicle: Chronicle): ConvertedChronicle =
    ConvertedChronicle(entries.entry(chronicle),
      tts.encode(chronicle).toOption.map(_.text))
```

Run: `./sbtw "testOnly distantlands.application.chronicle.ChronicleConversionSuite"`
Expected: PASS.

- [ ] **Step 4: Add the convert protocol, test first**

Add to `ChronicleProtocolSuite`:

```scala
  test("a convert request is JSON alone, or a TTS string with a name"):
    Vector(ChronicleConvertRequest.FromJson("{}"),
      ChronicleConvertRequest.FromTts("03abc", "Imported")).foreach(request =>
      assertEquals(ChronicleConvertRequestCodec.decode(
        ChronicleConvertRequestCodec.encode(request)), Right(request)))
    assert(ChronicleConvertRequestCodec.decode("""{"tts":"03abc"}""").isLeft)
    assert(ChronicleConvertRequestCodec.decode(
      """{"json":"{}","name":"x"}""").isLeft)

  test("a convert response is the entry's fields and an optional TTS string"):
    Vector(Some("03abc"), None).foreach(tts =>
      val response = ChronicleConvertResponse(entry, tts)
      assertEquals(ChronicleConvertResponseCodec.decode(
        ChronicleConvertResponseCodec.encode(response)), Right(response)))
    assert(ujson.read(ChronicleConvertResponseCodec.encode(
      ChronicleConvertResponse(entry, None))).obj.keySet == Set("json",
      "digest", "summary"))
```

Append to `ChronicleProtocol.scala`:

```scala
/** `POST /chronicles/convert`'s body: a saved Chronicle's JSON, or a TTS
  * string with the name it is imported under. */
enum ChronicleConvertRequest:
  case FromJson(json: String)
  case FromTts(tts: String, name: String)

/** The convert route's answer: `{json, digest, summary, tts?}`. */
final case class ChronicleConvertResponse(entry: ChronicleEntry,
    tts: Option[String])

object ChronicleConvertRequestCodec:
  import TrustedGameCodecFields.{exact => exactKeys, parse, text}

  def encode(request: ChronicleConvertRequest): String =
    ujson.write(request match
      case ChronicleConvertRequest.FromJson(json) => ujson.Obj("json" -> json)
      case ChronicleConvertRequest.FromTts(tts, name) =>
        ujson.Obj("tts" -> tts, "name" -> name))

  def decode(json: String)
      : Either[ProtocolDecodeFailure, ChronicleConvertRequest] =
    parse(json) {
      case raw: ujson.Obj if raw.value.contains("json") =>
        exactKeys(raw, Vector("json"), "$").flatMap(root =>
          text(root, "json", "$")).map(ChronicleConvertRequest.FromJson(_))
      case raw => for
        root <- exactKeys(raw, Vector("tts", "name"), "$")
        tts <- text(root, "tts", "$")
        name <- text(root, "name", "$")
      yield ChronicleConvertRequest.FromTts(tts, name)
    }

object ChronicleConvertResponseCodec:
  def encode(response: ChronicleConvertResponse): String =
    val value = ChronicleEntryCodec.encodeValue(response.entry)
    response.tts.foreach(tts => value("tts") = tts)
    ujson.write(value)

  def decode(json: String)
      : Either[ProtocolDecodeFailure, ChronicleConvertResponse] =
    TrustedGameCodecFields.parse(json) { raw => for
      value <- obj(raw, "$")
      _ <- exact(value, ChronicleEntryCodec.fields + "tts", "$")
      entry <- ChronicleEntryCodec.decodeFields(value, "$")
      tts <- optionalAbsent(value, "tts", "$")(string)
    yield ChronicleConvertResponse(entry, tts) }
```

The file already imports `ProjectionCodecSupport._`. That import supplies `obj`, `exact`, `optionalAbsent` and `string`. `TrustedGameCodecFields` has its own `exact`, so the request codec imports it under the name `exactKeys`. If the `optionalAbsent(...)(string)` overload is ambiguous, write `(raw, child) => string(raw, child)`.

Run: `./sbtw "testOnly distantlands.protocol.ChronicleProtocolSuite"`
Expected: PASS.

- [ ] **Step 5: Write the failing route tests**

In `TrustedSeatRoutesSuite`, find where `withServer` builds `TrustedSeatRoutes`. Pass it `new ChronicleConversion(catalog)` in the new `chronicles` position. Add:

```scala
  private val conversionEntries = ChronicleEntries()

  test("convert answers a Chronicle's JSON with its entry and TTS string"):
    withServer() { (server, _) =>
      val json = conversionEntries.entry(chronicle).json
      val response = send(server, "/chronicles/convert", Some(
        ChronicleConvertRequestCodec.encode(
          ChronicleConvertRequest.FromJson(json))))
      assertEquals(response.statusCode(), 200, response.body())
      val converted = accepted(ChronicleConvertResponseCodec.decode(
        response.body()))
      assertEquals(converted.entry, conversionEntries.entry(chronicle))
      assert(converted.tts.nonEmpty)
    }

  test("convert refuses a Chronicle it cannot read with the reason, and " +
      "a malformed body as malformed"):
    withServer() { (server, _) =>
      val refused = send(server, "/chronicles/convert", Some(
        ChronicleConvertRequestCodec.encode(
          ChronicleConvertRequest.FromTts("nonsense", "Imported"))))
      assertEquals(refused.statusCode(), 422, refused.body())
      assert(refused.body().contains("invalid-chronicle"), refused.body())
      val malformed = send(server, "/chronicles/convert", Some("""{"x":1}"""))
      assertEquals(malformed.statusCode(), 400, malformed.body())
    }
```

Add `import distantlands.application.chronicle.{ChronicleConversion, ChronicleEntries}`. If `withServer` builds its routes through `ServerRoutes` and a `ServerRuntime`, the runtime wiring in Step 7 covers this test.

Run: `./sbtw "testOnly distantlands.server.TrustedSeatRoutesSuite"`
Expected: compile error on the new `TrustedSeatRoutes` parameter, or 404 for `/chronicles/convert`.

- [ ] **Step 6: Add the route**

In `TrustedSeatRoutes`:
- Add `chronicles: ChronicleConversion,` after `gateway`.
- Import `distantlands.application.chronicle.ChronicleConversion`.
- Insert this branch after the `path("games")` branch:

```scala
    } ~ path("chronicles" / "convert") {
      post { noQuery { sameOrigin {
        entity(as[String]) { body =>
          ChronicleConvertRequestCodec.decode(body) match
            case Left(_) => complete(malformed)
            case Right(request) => async:
              val converted = request match
                case ChronicleConvertRequest.FromJson(text) =>
                  chronicles.fromJson(text)
                case ChronicleConvertRequest.FromTts(text, name) =>
                  chronicles.fromTts(text, name)
              converted.fold(
                reason => error(StatusCodes.UnprocessableEntity,
                  "invalid-chronicle", reason),
                done => json(StatusCodes.OK, ChronicleConvertResponseCodec
                  .encode(ChronicleConvertResponse(done.entry, done.tts))))
        }
      } } }
```

- [ ] **Step 7: Wire it**

`ServerRuntime`:
- Add `val chronicles: distantlands.application.chronicle.ChronicleConversion` after `trustedGame`.
- In `open`, build `new distantlands.application.chronicle.ChronicleConversion(catalog)` and pass it.

`ServerRoutes.scala:46`: pass `runtime.chronicles` after `runtime.trustedGame`.

Run: `./sbtw "testOnly distantlands.server.*"`
Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/application/chronicle/ChronicleConversion.scala \
  shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala \
  shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala \
  src/main/scala/distantlands/server/TrustedSeatRoutes.scala \
  src/main/scala/distantlands/server/ServerRuntime.scala \
  src/main/scala/distantlands/server/ServerRoutes.scala \
  src/test/scala/distantlands/application/chronicle/ChronicleConversionSuite.scala \
  src/test/scala/distantlands/server/TrustedSeatRoutesSuite.scala
git commit -m "feat(chronicle): convert a Chronicle between JSON and TTS, checking its cards

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Create from a new or saved Chronicle

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/TrustedGameProtocol.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/TrustedGameProtocolCodec.scala`
- Modify: `src/main/scala/distantlands/server/TrustedGameProvisioning.scala`
- Modify: `src/main/scala/distantlands/server/TrustedSeatRoutes.scala` (create answer)
- Modify: `src/main/scala/distantlands/server/ServerRuntime.scala` (provisioning gets `chronicles`)
- Modify: `frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala:163`
- Modify tests: `TrustedGameProvisioningSuite`, `TrustedSeatRoutesSuite`, `ServerRuntimeSuite`, `frontend/src/test/scala/distantlands/frontend/ServerModeHostFormSuite.scala`, and a shared protocol suite for the create codec (find it with `grep -rln TrustedGameCreateRequestCodec shared/src/test`; if none exists, add the tests to `ChronicleProtocolSuite`).

**Interfaces:**
- Consumes: `ChronicleChoice`, `ChronicleConversion.decode` and `.entry`, `ChronicleEntryCodec`.
- Produces:
  - `enum ChronicleRequest { case New(name: String); case Saved(json: String) }` in `distantlands.protocol`.
  - `TrustedGameCreateRequest(participants, chronicle: ChronicleRequest)`.
  - `TrustedGameCreateResponse(gameId, seats, chronicle: ChronicleEntry)`.
  - `TrustedGameFailure.Refused(reason: String)`.
  - `TrustedGameProvisioning(service, planFactory, store, chronicles: ChronicleConversion, generateCode, nowMillis, generateGameId)`.
- Wire: `"chronicle": {"kind": "new", "name": "..."}` or `{"kind": "saved", "json": "..."}`. The response gains `"chronicle": {json, digest, summary}`.

- [ ] **Step 1: Write the failing protocol tests**

```scala
  test("a create request carries a new Chronicle's name or a saved one's JSON"):
    val participants = Vector(BootstrapParticipantRequest("p1",
      distantlands.model.PlayerColor.Red))
    Vector(ChronicleRequest.New("Ashen Crown"), ChronicleRequest.Saved("{}"))
      .foreach(chronicle =>
        val request = TrustedGameCreateRequest(participants, chronicle)
        assertEquals(TrustedGameCreateRequestCodec.decode(
          TrustedGameCreateRequestCodec.encode(request)), Right(request)))
    assert(TrustedGameCreateRequestCodec.decode(
      """{"participants":[{"playerId":"p1","color":"red"}]}""").isLeft)

  test("a create response carries the starting Chronicle's entry"):
    val response = TrustedGameCreateResponse("game-a",
      Vector(TrustedSeatLink("p1", "https://oath.test/s/x")), entry)
    assertEquals(TrustedGameCreateResponseCodec.decode(
      TrustedGameCreateResponseCodec.encode(response)), Right(response))
```

If this goes into a suite other than `ChronicleProtocolSuite`, define `entry` there as `ChronicleProtocolSuite` does.

Run: `./sbtw "testOnly distantlands.protocol.*"`
Expected: compile error, `Not found: ChronicleRequest`.

- [ ] **Step 2: Add the fields**

`TrustedGameProtocol.scala`:

```scala
/** What the game starts from (Chronicle browsing design, "Create
  * request"): a new first-game Chronicle under a name, or a saved
  * Chronicle's JSON. */
enum ChronicleRequest:
  case New(name: String)
  case Saved(json: String)

/** The server generates the game ID and derives each lineage, so a request
  * names only the players, their colors and the Chronicle. */
final case class TrustedGameCreateRequest(
    participants: Vector[BootstrapParticipantRequest],
    chronicle: ChronicleRequest
)

final case class TrustedSeatLink(playerId: String, url: String)
/** `chronicle` is the starting Chronicle, for the browser to keep. */
final case class TrustedGameCreateResponse(gameId: String,
    seats: Vector[TrustedSeatLink], chronicle: ChronicleEntry)
```

`TrustedGameCreateRequestCodec.encode` adds:

```scala
    "chronicle" -> (request.chronicle match
      case ChronicleRequest.New(name) =>
        ujson.Obj("kind" -> "new", "name" -> name)
      case ChronicleRequest.Saved(json) =>
        ujson.Obj("kind" -> "saved", "json" -> json))
```

`decode`:
- Change the root to `exact(raw, Vector("participants", "chronicle"), "$")`.
- Add this generator before the participant check:

```scala
      chronicle <- root("chronicle") match
        case obj: ujson.Obj if obj.value.get("kind").contains(ujson.Str("new")) =>
          exact(obj, Vector("kind", "name"), "$.chronicle")
            .flatMap(text(_, "name", "$.chronicle")).map(ChronicleRequest.New(_))
        case obj: ujson.Obj if obj.value.get("kind").contains(ujson.Str("saved")) =>
          exact(obj, Vector("kind", "json"), "$.chronicle")
            .flatMap(text(_, "json", "$.chronicle")).map(ChronicleRequest.Saved(_))
        case _ => Left(InvalidValue("$.chronicle.kind",
          "expected \"new\" or \"saved\""))
```

- Yield `TrustedGameCreateRequest(participants, chronicle)`.

`TrustedGameCreateResponseCodec`:
- `encode` adds `"chronicle" -> ChronicleEntryCodec.encodeValue(response.chronicle)`.
- `decode` adds `"chronicle"` to the exact keys and `chronicle <- ChronicleEntryCodec.decodeValue(root("chronicle"), "$.chronicle")`.
- Yield `TrustedGameCreateResponse(game, seats, chronicle)`.

Run: `./sbtw "testOnly distantlands.protocol.*"`
Expected: PASS. The server and frontend do not compile yet.

- [ ] **Step 3: Write the failing provisioning tests**

In `TrustedGameProvisioningSuite`:
- `request` gains `ChronicleRequest.New("Ashen Crown")`.
- `provision` passes `new ChronicleConversion(catalog)` after `owner.trustedGames`.

Add:

```scala
  test("a new Chronicle starts the game under its name, and the response " +
      "carries its entry"):
    withDatabase { (owner, _) =>
      val created = accepted(provision(owner, codes).create(request,
        "http://127.0.0.1:8080"))
      assertEquals(created.chronicle.summary.name, "Ashen Crown")
      assertEquals(created.chronicle.summary.game, Some(1))
      assertEquals(created.chronicle.digest,
        ChronicleEntries.digest(created.chronicle.json))
    }

  test("a saved first-game Chronicle starts the game as it is"):
    withDatabase { (owner, _) =>
      val saved = ChronicleEntries().entry(FirstGameSetupFixture.chronicle)
      val created = accepted(provision(owner, codes).create(
        request.copy(chronicle = ChronicleRequest.Saved(saved.json)),
        "http://127.0.0.1:8080"))
      assertEquals(created.chronicle, saved)
    }

  test("an evolved Chronicle is refused with game start's reason, and " +
      "unreadable JSON with the codec's, storing nothing"):
    withDatabase { (owner, connection) =>
      val evolved = ChronicleEntries().entry(EvolvedChronicleFixture.record)
      assertEquals(provision(owner, codes).create(
        request.copy(chronicle = ChronicleRequest.Saved(evolved.json)),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused("a non-empty world requires the Empire")))
      assert(provision(owner, codes).create(
        request.copy(chronicle = ChronicleRequest.Saved("{")),
        "http://127.0.0.1:8080") match
          case Left(TrustedGameFailure.Refused(reason)) =>
            reason.startsWith("not a Chronicle file")
          case _ => false)
      assertEquals(rows(connection, gameId), nothingStored)
    }

  test("a blank new name is refused"):
    withDatabase { (owner, _) =>
      assertEquals(provision(owner, codes).create(
        request.copy(chronicle = ChronicleRequest.New("   ")),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused("a Chronicle needs a name")))
    }
```

`codes` stands for whatever seat-code generator the suite's existing tests pass to `provision`. Use that same one.

The evolved record's first refusal may be different. `GameStartRules.evolve` checks the world first, then starting relics, and so on. Whichever check fires first, assert the reason it actually gives. The test is about getting game start's reason back, not about that particular reason.

Run: `./sbtw "testOnly distantlands.server.TrustedGameProvisioningSuite"`
Expected: compile failure on `TrustedGameFailure.Refused` and the new parameter.

- [ ] **Step 4: Provision from the request's Chronicle**

`TrustedGameProvisioning`:
- `enum TrustedGameFailure { case InvalidRequest, DuplicateGame, CodeCollision, StorageFailure; case Refused(reason: String) }`.
- Add the constructor parameter `chronicles: ChronicleConversion` after `store`.
- Replace `create`'s `for`:

```scala
      for
        valid <- TrustedGameCreateRequestCodec.decode(TrustedGameCreateRequestCodec.encode(request))
          .left.map(_ => InvalidRequest)
        origin <- validatedOrigin(publicBaseUrl)
        choice <- valid.chronicle match
          case ChronicleRequest.New(name) => Right(ChronicleChoice.New(name.trim))
          case ChronicleRequest.Saved(json) =>
            chronicles.decode(json).map(ChronicleChoice.Saved(_)).left.map(Refused(_))
        // The plan factory shuffles the seating and picks the first player,
        // so the first participant is only a placeholder.
        config = FirstGameBootstrapMapper.map(FirstGameBootstrapRequest(
          0L, valid.participants, valid.participants.head.playerId))
        plan <- planFactory.build(config, choice).left.map(failure =>
          Refused(failure.message))
        placed <- place(valid.participants, plan, None, 1)
        (gameId, codes) = placed
      yield TrustedGameCreateResponse(gameId, codes.map { case (player, code) =>
        TrustedSeatLink(player, s"$origin/s/${code.raw}") },
        chronicles.entry(plan.chronicle))
```

In `place`, map game start's refusal before the general rejection:

```scala
        case GameApplicationError.CommandRejected(
            OathViolation.UnsupportedChronicle(reason)) => Refused(reason)
        case _: GameApplicationError.CommandRejected => InvalidRequest
```

Import `distantlands.application.chronicle.ChronicleConversion` and `distantlands.model.OathViolation`.

`ServerRuntime.open`: build the `ChronicleConversion` once. Pass it to both `TrustedGameProvisioning` (after `database.trustedGames`) and the runtime.

`TrustedSeatRoutes`' create branch adds:

```scala
                case Left(TrustedGameFailure.Refused(reason)) => error(
                  StatusCodes.UnprocessableEntity, "chronicle-refused", reason)
```

- [ ] **Step 5: Update the route and runtime tests**

`TrustedSeatRoutesSuite.requestBody` and `ServerRuntimeSuite.request` gain `ChronicleRequest.New("Route Chronicle")`. Add to `TrustedSeatRoutesSuite`:

```scala
  test("creation refuses an evolved Chronicle with game start's reason"):
    withServer() { (server, _) =>
      val body = TrustedGameCreateRequestCodec.encode(TrustedGameCreateRequest(
        Vector(BootstrapParticipantRequest("p1", PlayerColor.Red),
          BootstrapParticipantRequest("p2", PlayerColor.Blue)),
        ChronicleRequest.Saved(ChronicleEntries().entry(
          EvolvedChronicleFixture.record).json)))
      val response = send(server, "/games", Some(body))
      assertEquals(response.statusCode(), 422, response.body())
      assert(response.body().contains("chronicle-refused"), response.body())
    }
```

- [ ] **Step 6: Keep the frontend creating games**

In `TrustedHostUi`, replace the request line:

```scala
      val request = TrustedGameCreateRequest(participants,
        ChronicleRequest.New(TrustedHostUi.defaultChronicleName()))
```

Add to `object TrustedHostUi`, or create one if the file has none:

```scala
  /** "Chronicle 2026-10-29", today's date, until slice 2's name field. */
  def defaultChronicleName(): String =
    "Chronicle " + new scala.scalajs.js.Date().toISOString().take(10)
```

In `ServerModeHostFormSuite`, `seatLinksResponse` gains a `"chronicle"` entry. Insert it before the closing brace:

```scala
  private val seatLinksResponse = """{"gameId":"host-game","seats":[""" +
    """{"playerId":"Red","url":"https://oath.test/s/red-code"},""" +
    """{"playerId":"Blue","url":"https://oath.test/s/blue-code"}],""" +
    """"chronicle":{"json":"{}","digest":"ab12","summary":""" +
    """{"name":"Chronicle 2026-10-29","game":1,"eraTarget":20,"scores":[]}}}"""
```

Add one assertion to the suite's existing request test. Every decoded request's `chronicle` is a `ChronicleRequest.New` whose name starts with `"Chronicle "`.

- [ ] **Step 7: Run the suites**

Run: `./sbtw "testOnly distantlands.server.* distantlands.protocol.*"`
Expected: all pass.

Run: `./sbtw frontend/test`
Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/TrustedGameProtocol.scala \
  shared/src/main/scala/distantlands/protocol/TrustedGameProtocolCodec.scala \
  src/main/scala/distantlands/server/TrustedGameProvisioning.scala \
  src/main/scala/distantlands/server/TrustedSeatRoutes.scala \
  src/main/scala/distantlands/server/ServerRuntime.scala \
  frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala
git add -u src/test shared/src/test frontend/src/test
git commit -m "feat(chronicle): create a game from a new or saved Chronicle, returning its entry

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The projection's Chronicle

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/GameProjection.scala`
- Modify: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Create: `src/test/scala/distantlands/application/ChronicleProjectionSuite.scala`

**Interfaces:**
- Consumes: `ChronicleEntries`, `ChronicleEntryCodec`, `CampaignState.baseline`, `CampaignState.evolved`.
- Produces: `ChronicleProjection(name: String, startingDigest: String, evolved: Option[ChronicleEntry])`, `GameProjection.chronicle: Option[ChronicleProjection] = None` and `GameProjector(catalog, phasePowers, entries: ChronicleEntries = ChronicleEntries())`.
- Wire: `"chronicle": {"name": "...", "startingDigest": "...", "evolved": {json, digest, summary}}`. `evolved` is absent until journaled. `chronicle` is absent before the game starts.

- [ ] **Step 1: Write the failing codec test**

Add to `ProjectionProtocolSuite`. Use the suite's existing minimal `GameProjection` value; call it `base` here.

```scala
  test("the projection round trips its Chronicle, with and without the " +
      "evolved one"):
    val entry = ChronicleEntry("{}", "ab12", ChronicleSummary("Ashen Crown",
      Some(2), 20, Vector(ChronicleScore("red-lineage", 3))))
    Vector(None, Some(entry)).foreach(evolved =>
      val projection = base.copy(chronicle = Some(ChronicleProjection(
        "Ashen Crown", "cd34", evolved)))
      assertEquals(GameProjectionCodec.decode(
        GameProjectionCodec.encode(projection)), Right(projection)))
```

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite"`
Expected: compile error, `Not found: ChronicleProjection`.

- [ ] **Step 2: Add the DTO and codec field**

`GameProjectionDto.scala`: add `,chronicle: Option[ChronicleProjection] = None` after `consentPreferences`. Then add:

```scala
/** Every seat's view of the game's Chronicle (Chronicle browsing design,
  * "Projection"): its name and the starting Chronicle's digest from the
  * start, and the evolved Chronicle once the Chronicle Phase journals it. */
final case class ChronicleProjection(name: String, startingDigest: String,
    evolved: Option[distantlands.protocol.ChronicleEntry])
```

`GameProjectionCodec`:
- Add `"chronicle"` to `Fields`.
- In `encodeValue`, add `"chronicle" -> option(value.chronicle)(encodeChronicle)` to the `ujson.Obj`.
- In `decodeValue`, add `chronicle <- optionalAbsent(value, "chronicle", path)(decodeChronicle)` and pass it last to `GameProjection(...)`.
- Add these helpers:

```scala
  private def encodeChronicle(value: ChronicleProjection): ujson.Value =
    val result = ujson.Obj("name" -> value.name,
      "startingDigest" -> value.startingDigest)
    value.evolved.foreach(entry =>
      result("evolved") = ChronicleEntryCodec.encodeValue(entry))
    result

  private def decodeChronicle(raw: ujson.Value, path: String)
      : Result[ChronicleProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("name", "startingDigest", "evolved"), path)
    name <- string(row, "name", path)
    digest <- string(row, "startingDigest", path)
    evolved <- optionalAbsent(row, "evolved", path)(
      ChronicleEntryCodec.decodeValue)
  yield ChronicleProjection(name, digest, evolved)
```

Import `distantlands.protocol.ChronicleEntryCodec`.

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite"`
Expected: PASS.

- [ ] **Step 3: Write the failing projector test**

Create `src/test/scala/distantlands/application/ChronicleProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.application.chronicle.ChronicleEntries
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}

/** The Chronicle every seat sees (Chronicle browsing design, "Projection"). */
class ChronicleProjectionSuite extends munit.FunSuite:
  private val projector = GameProjector(catalog)
  private val entries = ChronicleEntries()

  test("every seat sees the name and the starting digest from the start, " +
      "and no evolved Chronicle"):
    val baseline = Table.start.ready.game.campaign.baseline
    Vector(p1, p2).foreach(seat =>
      val chronicle = projector.project("g", LoadedGame(Table.start.state,
        1L), seat).chronicle
      assertEquals(chronicle.map(_.name), Some(baseline.name))
      assertEquals(chronicle.map(_.startingDigest),
        Some(entries.entry(baseline).digest))
      assertEquals(chronicle.flatMap(_.evolved), None))

  test("once the Chronicle is journaled, every seat sees the evolved one"):
    val state = EvolvedChronicleFixture.done.state
    val expected = entries.entry(EvolvedChronicleFixture.record)
    Vector(p1, p2).foreach(seat =>
      assertEquals(projector.project("g", LoadedGame(state, 1L), seat)
        .chronicle.flatMap(_.evolved), Some(expected)))
    assertEquals(projector.projectPublic("g", LoadedGame(state, 1L))
      .chronicle.flatMap(_.evolved), Some(expected))

  test("a game not yet started has no Chronicle"):
    assertEquals(projector.project("g", LoadedGame(
      distantlands.model.OathState.NoGame, 0L), p1).chronicle, None)
```

If `Table.start.state` is not an `OathState`, use whatever `PhasePowerHarness` passes to `LoadedGame`. If the finished game's projection throws, because a completed game's walker state projects differently, read the error and fix the projector, not the test.

Run: `./sbtw "testOnly distantlands.application.ChronicleProjectionSuite"`
Expected: FAIL, `chronicle` is `None`.

- [ ] **Step 4: Project the Chronicle**

In `GameProjection.scala`:
- Change the primary constructor to `final class GameProjector(catalog: ExecutableCatalog, phasePowers: PhasePowers, entries: ChronicleEntries = ChronicleEntries())`.
- Keep `def this(catalog: ExecutableCatalog)`.
- Import `distantlands.application.chronicle.ChronicleEntries`.
- In `readyProjection`'s trailing `.copy(...)`, add `chronicle = Some(chronicleView(context.ready.game.campaign)),`.
- Add:

```scala
  /** Every seat's Chronicle (Chronicle browsing design, "Projection"): the
    * name and starting digest from the start, the evolved Chronicle once
    * the Chronicle Phase journals it. The relic deck's order shows; that is
    * accepted. */
  private def chronicleView(campaign: CampaignState): ChronicleProjection =
    ChronicleProjection(campaign.baseline.name,
      entries.entry(campaign.baseline).digest,
      campaign.evolved.map(entries.entry))
```

Run: `./sbtw "testOnly distantlands.application.ChronicleProjectionSuite distantlands.application.* distantlands.server.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala \
  shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala \
  src/main/scala/distantlands/application/GameProjection.scala \
  src/test/scala/distantlands/application/ChronicleProjectionSuite.scala
git commit -m "feat(chronicle): show every seat the Chronicle's name, starting digest and evolved Chronicle

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Docs and gates

**Files:**
- Modify: `docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md` (status)
- Modify: `docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md` (lines 69-75 and 99-103)
- Modify: `docs/ROADMAP.md:218`
- Modify: `docs/rules/implementation-traceability.md` (the Chronicle codec row)

- [ ] **Step 1: Update the docs**

- **Browsing spec status line:** "Status: slice 1 implemented, 2026-10-29: the server side. Next step: slice 2, the browser pages and storage."
- **TTS codec spec, lines 99-103:** replace "Browsing slice 1 adds the `name` parameter and the name entry in the omitted list, when `Chronicle` gains `name`." with "Decoding takes the name from the caller, and the omitted list always starts with the name."
- **TTS codec spec, line 75:** "any non-zero era score" becomes "any game on the era scorepad".
- **ROADMAP:** add a sub-bullet under "Saved-campaign browsing":

```markdown
  - Slice 1 landed: `Chronicle.name`, the era scorepad, canonical Chronicle
    JSON with a digest and summary, `POST /chronicles/convert`, game
    creation from a new or saved Chronicle, and the projection's Chronicle.
    Slice 2 builds the browser pages and storage.
```

  Leave the box unticked.
- **Traceability:** in the row that names `ChronicleTtsCodec`, add that the convert route uses it and that the scorepad is not in the TTS string.

- [ ] **Step 2: Run every gate**

Run: `python3 scripts/check-architecture.py`
Expected: no errors.

Run: `python3 scripts/check-markdown-links.py`
Expected: no errors.

Run: `./sbtw clean coverage test coverageReport`
Expected: all tests pass, statement coverage at least 92%.

Run: `./sbtw frontend/test`
Expected: all pass.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-10-28-chronicle-browsing-design.md \
  docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md \
  docs/ROADMAP.md docs/rules/implementation-traceability.md
git commit -m "docs(chronicle): record browsing slice 1 as landed

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
