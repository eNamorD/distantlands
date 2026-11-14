# Legacy Chronicle Powers, Slice 1: Seam and Sun Implementation Plan

> Executed inline by the implementer.

**Goal:** The first three legacy Chronicle powers act in the Sun, on a seam
the later slices reuse. `ChronicleLegacyRules.holder` names the seated
player holding a legacy active when its window is reached. Steadfast keeps
its Exile holder's other legacies active in Sun step 3. Gatekeeper, in a
new step after Reveal Relics, lets its holder take one active legacy from
an enemy whose goal they meet. Chronicler, in a new step after Sun step 5,
lets its holder declare that every seated player scores their legacies now.
A new `ChronicleOperation`, `LegaciesScored`, records the legacies scored
this Chronicle, and Score Lineage Points skips them. The three cards leave
holding and count as implemented.

**Architecture:** `ChronicleLegacyRules` (in `gameplay/chronicle`) is the
seam: `holder(state, legacy, roles)` is read live by every power when its
window is reached. Each legacy is a card object and a `ContributingPower`
in its own file in a new package, `gameplay/chronicle/legacies`, beside
its power (the "card sits beside its power" rule of `CardCatalogSuite`).
Added steps (Gatekeeper, Chronicler) are `Transform`s on two new, empty Sun
windows that append one `Branch` of constant shape, read when reached.
Changed steps (Steadfast) ask `ChronicleLegacyRules` from the existing
step. The powers' lines are Sun procedure lines, declared on the Sun's note
id as `ResolveLegacies` declares its own, so `ProcedureLog` gives each its
kind and the decisions are narrated by prefix.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-8 and section "Slice 1: seam and Sun".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md).
Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Chronicler L02, Steadfast L03, Gatekeeper L32). `docs/rules/chronicle.md`,
the Sun and the Throne step 6; the
[Legacies rulings](../../../rules/ambiguities.md#legacies-rulings) R18-R33
and their [implementation rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md).
The seam proof: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`.

## Prerequisite

The Legacies design has landed (through `3f432a130`). Check before Task 1:

```bash
grep -q "object ResolveLegacies" src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala &&
grep -q "def bonus(ready: ReadyGame" src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala &&
grep -q "object SteadfastCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
echo ready
```

Expected: `ready`.

## Open questions

These change behaviour, data or the public shape of the code, and wait for
the orchestrator's rulings. The tasks below are written to each
recommendation; a different ruling is folded in before execution.

1. **Do dormant legacies score under Chronicler?** At Chronicler's step,
   after Sun step 3, Exiles may hold newly dormant legacies (an Exile
   winner's come back only at Crown Chancellor, after Chronicler).
   Options: (a) every legacy held, dormant included, as ruling 7's "each
   legacy held" reads and as `LineageScoring.bonus` counts today;
   (b) only active legacies. Recommendation: (a). A dormant legacy has no
   power (ruling 1), but scoring is not its power; it is the Chronicler
   holder's.
2. **Which legacies `LegaciesScored` records.** Options: (a) every legacy
   each seated player holds when Chronicler resolves, including those whose
   share is 0; (b) only legacies that earned a point. Recommendation: (a):
   the card says all players "score their legacies", and a legacy that
   scores 0 has still been scored. A consequence: declaring early (before
   Shape Empire and Build Edifice) can deny the winner's bonus. Only
   Chronicler records in this slice; Score Lineage Points only skips
   (ruling 8 says it skips them), and slice 4 decides whether Iron Hand's
   scoring records.
3. **The scorepad's roles when Chronicler scores first.** Points recorded
   by Chronicler open the game's era row in the Sun, before Crown
   Chancellor, so `lineagePointsScored` writes each entry's `endingRole`
   from the roles before the Crown and Offer Citizenship. A winning Exile
   would be recorded as an Exile, against Chronicle ruling 19 ("circle the
   Chancellor's score"). Options: (a) `RoleChanged` also updates the open
   row's entry for that lineage, so `endingRole` follows the lineage's role
   until the Chronicle ends (today the row opens at Throne step 6, after
   every role change, so nothing else moves); (b) Chronicler holds its
   points aside and records them at Score Lineage Points; (c) accept the
   pre-Crown roles. Recommendation: (a), Task 5.
4. **Chronicler's lines.** Spec: "p2 declared Chronicler", then one line in
   the bonus format per player who scores. Proposed: "p2 scored 3 lineage
   points (1 from Discord, 2 from Arcane)", the bonus format without the
   base (`LineageScoring.scoredWithBonus` minus "N base"), posted as
   `Delta`. The declaration and Gatekeeper's "p2 took Seer from p3 with
   Gatekeeper" post as `Decision`. Each legacy named in a line is a card
   span. Recommendation: as proposed.
5. **The two new windows' names and keys.**
   `PowerWindow.ChronicleBeforeResolveLegacies`
   (`chronicle.sun.before-resolve-legacies`), between Reveal Relics and
   Flip Legacies, and `PowerWindow.ChronicleAfterResolveLegacies`
   (`chronicle.sun.after-resolve-legacies`), between Shuffle and Crown, after
   the cards' printed timings ("Before players resolve legacies", "After
   Resolve Legacies"). Window keys are not serialized. Recommendation: as
   proposed.
6. **The decisions' shapes.** Gatekeeper: `ChooseMany(0, 1)` over the
   takeable legacies as `DecisionOptionRef.Legacy` options, confirm
   "Take", asked only when at least one legacy is takeable (as Offer
   Citizenship's optional choose-many). Chronicler: `ChooseOne` of two
   buttons, Decline first then Declare (as Offer Citizenship's answer),
   asked whenever its holder holds it active. Decision ids
   `chronicle.sun.legacy.gatekeeper.<player>` and
   `chronicle.sun.legacy.chronicler.<player>` (ruling 3), narrated by the
   prefix `chronicle.sun.legacy.`, so a decline writes no line (as an empty
   Citizenship offer writes none). The plainest answer is a decline, so
   existing tests that hold these cards are unchanged. Recommendation: as
   proposed.

## Deviations from the spec

- `ChronicleLegacyRules.holder(state, legacy, roles)` takes no catalog: it
  reads only the lineages, and the role condition (ruling 2) is a `roles`
  set defaulting to every role. The spec's sketch names `holder(state,
  catalog, legacy)`.
- The power cards live in `gameplay/chronicle/legacies`, not beside the
  other powers in `gameplay/powers`, since their steps are Chronicle steps
  and read `LegacyGoals`, `LineageScoring` and `ResolveLegacies`;
  `WalkerPowerCatalog` registers them as it registers `CouncilSeat` from
  `gameplay/roles`.
- `ResolveLegaciesSuite`'s private Sun walk moves to `ChronicleWalk` as
  `ChronicleWalk.through`, so the new suites share it (the slice 3 plan's
  own advice).

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`. It may read a card's `name`, `suit`,
  `goal`, `alters` and a relic's `value`.
- A new test names cards by their card objects (`SteadfastCard`).
- Every power reads its holder when its window is reached, never when the
  walk is built; a `Transform` appends a node of constant shape (the
  resume bug class of Legacies slice 3).
- No two decisions share an id: each new id is
  `chronicle.sun.legacy.<power>.<player>` (ruling 3).
- In the Chronicle Phase every legacy is shown to every viewer (Legacies
  R18), so nothing in this slice is hidden.
- Old saved streams still decode: `LegaciesScored` is a new wire kind and
  `CurrentGameState.scoredLegacies` defaults to empty.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. One commit per task.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- A rewritten snapshot or golden file is reviewed line by line and
  committed in the task that changes it; every changed line must be one the
  task explains. `git status --short src/test/resources` is otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files; run `git log -3` after each commit.

---

### Task 1: `LegaciesScored`, the scored-legacies record

**Files:**
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/main/scala/distantlands/model/GameState.scala`
  (`CurrentGameState.scoredLegacies`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`
- Test: `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
  (`everyOperation`)

**Interfaces:**
- Produces: `final case class LegaciesScored(legacies: Vector[LegacyId])
  extends ChronicleOperation, LeafOperation`, requiring a non-empty
  vector; `CurrentGameState.scoredLegacies: Set[LegacyId]`, empty by
  default; wire kind `"legacies-scored"` with field `"legacies"`, an array
  of legacy ids.

- [ ] **Step 1: Write the failing tests**

In `ChronicleStateOperationsSuite` (import `SeerCard` from
`distantlands.catalog.holding`):

```scala
  test("scored legacies are recorded for the rest of the game (ruling 8)"):
    val after = accepted(run(Table.start.ready,
      LegaciesScored(Vector(WealthyCard.id, SeerCard.id))))
    assertEquals(after.game.current.scoredLegacies,
      Set(WealthyCard.id, SeerCard.id))

  test("a legacy scores once per game"):
    val once = accepted(run(Table.start.ready,
      LegaciesScored(Vector(WealthyCard.id))))
    rejected(run(once, LegaciesScored(Vector(SeerCard.id, WealthyCard.id))))
    rejected(run(Table.start.ready,
      LegaciesScored(Vector(SeerCard.id, SeerCard.id))))
```

In `ChronicleOperationWireSuite`:

```scala
  test("scored legacies name each legacy, and an empty list decodes to a " +
      "typed WireError"):
    val encoded = encodedStreamThatRoundTrips("legacies-scored",
      Vector(recordedOperation(LegaciesScored(Vector(LegacyId("L02"),
        LegacyId("L22"))))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "legacies-scored",
        "legacies" -> ujson.Arr("L02", "L22")))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("legacies") = ujson.Arr()
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].legacies",
        "scores at least one legacy")))
```

In `WalkerOperationWireSuite.everyOperation`, after
`FoundationAltered(FoundationNumber.III, LegacyId("L32"))`, add
`LegaciesScored(Vector(LegacyId("L02"))),`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.ChronicleOperationWireSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: FAIL to compile: `Not found: LegaciesScored`.

- [ ] **Step 3: Implement**

In `ChronicleOperation.scala`, after `FoundationAltered`:

```scala
/** Records the legacies scored this game (Chronicler, CR "Each legacy may
  * only be scored once per game"; Legacy Chronicle powers design, ruling
  * 8). By legacy, not by player: Score Lineage Points and Iron Hand's
  * scoring skip them. A legacy already recorded is a doubled operation. */
final case class LegaciesScored(legacies: Vector[LegacyId])
    extends ChronicleOperation, LeafOperation:
  require(legacies.nonEmpty, "a scoring records at least one legacy")
```

In `CurrentGameState`, after `chronicleTasks`:

```scala
    // The legacies scored this game, which no later scoring counts again
    // (Legacy Chronicle powers design, ruling 8).
    scoredLegacies: Set[LegacyId] = Set.empty,
```

In `ChronicleStateOperations`:

```scala
  /** `legacies` join the scored set; one already in it, or named twice, is
    * a doubled scoring. */
  def legaciesScored(ready: ReadyGame, legacies: Vector[LegacyId])
      : Either[OperationError, ReadyGame] =
    val scored = ready.game.current.scoredLegacies
    legacies.find(scored) match
      case Some(legacy) => Left(InvalidDescription(
        s"${legacy.value} has already scored this game"))
      case None if legacies.distinct.size != legacies.size =>
        Left(InvalidDescription("a scoring names each legacy once"))
      case None => Right(ready.updateCurrent(current => current.copy(
        scoredLegacies = current.scoredLegacies ++ legacies)))
```

In `LeafApplication.chronicle`: `case LegaciesScored(legacies) =>
ChronicleStateOperations.legaciesScored(ready, legacies)`.

In `ChronicleOperationCodec`, encode:

```scala
      case LegaciesScored(legacies) => ujson.Obj("kind" -> "legacies-scored",
        "legacies" -> legacies.map(legacy => ujson.Str(legacy.value)))
```

and decode:

```scala
    case "legacies-scored" =>
      val legacies = value("legacies").arr.toVector.map(id => LegacyId(id.str))
      Either.cond(legacies.nonEmpty, LegaciesScored(legacies),
        InvalidValue(s"$path.legacies", "scores at least one legacy"))
```

Fix any other exhaustive match the compiler names.

- [ ] **Step 4: Run the tests to verify they pass**

Run the command of Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/ChronicleOperation.scala \
  src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/gameplay/operations/LeafApplication.scala \
  src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala \
  src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala \
  src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala \
  src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(chronicle): record the legacies scored this game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: `ChronicleLegacyRules.holder` and the shared Chronicle walk

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/ChronicleLegacyRulesSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala`
  (`Run`, `through`)
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`
  (uses `ChronicleWalk.through`)

**Interfaces:**
- Produces: `ChronicleLegacyRules.holder(state: ReadyGame, legacy:
  LegacyId, roles: Set[Role] = Role.values.toSet): Option[PlayerId]`, the
  seated player whose lineage holds `legacy` active and whose role is in
  `roles`, read from `state` as given (rulings 1 and 2).
- Produces (test): `ChronicleWalk.Run(ready: ReadyGame, events:
  Vector[OathEvent], asked: Vector[Decide])` and
  `ChronicleWalk.through(rules: OathRules, table: Table, procedure:
  TriggeredProcedureRef, answers: PartialFunction[Decide, DecisionAnswer]
  = PartialFunction.empty): Run`, the Chronicle started on `table`, each
  park answered by `answers` or plainly, up to `WalkerCompleted(procedure)`,
  with `ready` the state replayed to that completion.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.holding.SeerCard
import distantlands.model._
import distantlands.testkit.Table.{p2, p3}

/** Who holds a legacy in effect when its window is reached (Legacy
  * Chronicle powers design, rulings 1 and 2). */
class ChronicleLegacyRulesSuite extends munit.FunSuite:
  private def won = ChronicleWalk.chronicleWonByP1

  test("the seated player holding a legacy active holds it in effect"):
    assertEquals(ChronicleLegacyRules.holder(won.legacy(p2, SeerCard).ready,
      SeerCard.id), Some(p2))

  test("a dormant legacy, or one nobody holds, has no holder (ruling 1)"):
    assertEquals(ChronicleLegacyRules.holder(
      won.legacy(p2, SeerCard, active = false).ready, SeerCard.id), None)
    assertEquals(ChronicleLegacyRules.holder(won.ready, SeerCard.id), None)

  test("a boxed lineage's active legacy has no holder (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Exile,
      Vector(LegacyState(SeerCard.id, active = true)), Vector.empty)
    val table = won.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(boxed)))))
    assertEquals(ChronicleLegacyRules.holder(table.ready, SeerCard.id), None)

  test("a role condition is read from the roles as they stand (ruling 2)"):
    val imperial = won.imperial(p3, p2).legacy(p2, SeerCard).ready
    val citizens = Set(Role.Citizen)
    assertEquals(ChronicleLegacyRules.holder(imperial, SeerCard.id, citizens),
      Some(p2))
    assertEquals(ChronicleLegacyRules.holder(won.legacy(p2, SeerCard).ready,
      SeerCard.id, citizens), None)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleLegacyRulesSuite"`
Expected: FAIL to compile: `Not found: ChronicleLegacyRules`.

- [ ] **Step 3: Implement**

```scala
package distantlands.gameplay.chronicle

import distantlands.model._

/** The seam the legacies' Chronicle powers share (Legacy Chronicle powers
  * design, "In effect is read live"). Every power, and every step a power
  * changes, asks it when its window is reached, never when the walk is
  * built: a `Branch` whose children depended on state the walk itself
  * changes would lose its shape on resume (Legacies slice 3). */
object ChronicleLegacyRules:
  /** The seated player whose lineage holds `legacy` active, if their role
    * is one of `roles`. A boxed lineage's legacies never act and a dormant
    * legacy has no power (ruling 1); a role condition ("if you are an
    * Imperial") is read now, so the Crown and Offer Citizenship change it
    * for every later step (ruling 2). */
  def holder(state: ReadyGame, legacy: LegacyId,
      roles: Set[Role] = Role.values.toSet): Option[PlayerId] =
    state.game.current.players.find(player =>
      state.game.campaign.lineages.get(player.lineage).exists(lineage =>
        roles(lineage.role) &&
          lineage.legacies.contains(LegacyState(legacy, active = true))))
      .map(_.player)
```

Move `ResolveLegaciesSuite`'s `Sun` and `sun` to `ChronicleWalk` as `Run`
and `through`, the body unchanged but for `WalkerCompleted(procedure)` in
place of `WalkerCompleted(TriggeredProcedureRef.ChronicleSun)` and `rules`
as a parameter. In `ResolveLegaciesSuite`, keep a one-line
`private def sun(table, answers) = ChronicleWalk.through(rules, table,
TriggeredProcedureRef.ChronicleSun, answers)` and replace the type `Sun`
with `ChronicleWalk.Run`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleLegacyRulesSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala \
  src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala
git commit -m "feat(chronicle): read who holds a legacy in effect

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Steadfast

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Steadfast.scala`
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `SteadfastCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (import)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
  (register `LegacyChroniclePowers.all`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`keepsOthersActive`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala`
  (`dormancy`, doc comment)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/SteadfastSuite.scala`
- Modify (imports): `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`,
  `src/test/scala/distantlands/gameplay/chronicle/OfferCitizenshipSuite.scala`,
  `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (rewritten)

**Interfaces:**
- Consumes: Task 2's `ChronicleLegacyRules.holder`; Task 2's
  `ChronicleWalk.through`.
- Produces: `distantlands.gameplay.chronicle.legacies.SteadfastCard` (the
  card, text unchanged) and `Steadfast`, a `ContributingPower` hooking no
  window; `ChronicleLegacyRules.keepsOthersActive(state: ReadyGame,
  player: PlayerId): Boolean`; `LegacyChroniclePowers.all:
  Vector[ContributingPower]`, which later tasks extend.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.holding.{IronHandCard, MatchmakerCard, RivalCard,
  SeerCard}
import distantlands.gameplay.OathRules
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleWalk,
  ResolveLegacies}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}

/** Steadfast (L03): "Your other legacies cannot become dormant—they always
  * stay active." Read at the start of Sun step 3 (ruling 5). Every game here
  * is p1's win; p2 and p3 are Exiles who rule no site. */
class SteadfastSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun)

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies

  test("Steadfast is implemented"):
    assert(PowerImplementationStatus.implemented(SteadfastCard.power.id))

  test("an Exile holding Steadfast keeps their other unmet legacies " +
      "active, while Steadfast itself goes dormant (ruling 5)"):
    // p2 rules no site and holds no banner: every goal here is unmet.
    val run = sun(won.legacy(p2, SteadfastCard).legacy(p2, IronHandCard)
      .legacy(p2, SeerCard))
    assertEquals(legaciesOf(run.ready, p2), Vector(
      LegacyState(SteadfastCard.id, active = false),
      LegacyState(IronHandCard.id, active = true),
      LegacyState(SeerCard.id, active = true)))

  test("Steadfast protects only its holder's legacies"):
    val run = sun(won.legacy(p2, SteadfastCard).legacy(p3, IronHandCard))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(IronHandCard.id, active = false)))

  test("Steadfast does not lift the cap: discarding is not going dormant " +
      "(ruling 5)"):
    val asked = sun(won.legacy(p2, SteadfastCard).legacy(p2, IronHandCard)
      .legacy(p2, SeerCard).legacy(p2, MatchmakerCard)
      .legacy(p2, RivalCard)).asked
      .find(_.decisionId == ResolveLegacies.capDecisionId(p2)).get
    // Steadfast went dormant: four active, one over the cap.
    val many = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((many.min, many.max), (1, 1))

  test("a dormant Steadfast protects nothing"):
    val run = sun(won.legacy(p2, SteadfastCard, active = false)
      .legacy(p2, IronHandCard))
    assert(!legaciesOf(run.ready, p2).exists(legacy =>
      legacy.id == IronHandCard.id && legacy.active))

  test("a boxed lineage's active Steadfast does nothing (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Exile,
      Vector(LegacyState(SteadfastCard.id, active = true)), Vector.empty)
    val table = won.legacy(p2, IronHandCard).update(_.updateCampaign(
      campaign => campaign.copy(baseline = campaign.baseline.copy(
        lineages = Vector(boxed)))))
    assert(table.ready.game.current.players.forall(player =>
      !ChronicleLegacyRules.keepsOthersActive(table.ready, player.player)))
    assertEquals(legaciesOf(sun(table).ready, p2),
      Vector(LegacyState(IronHandCard.id, active = false)))
```

Update the three test files' imports: `SteadfastCard` now comes from
`distantlands.gameplay.chronicle.legacies`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.SteadfastSuite"`
Expected: FAIL to compile: `Not found: SteadfastCard` in the new package.

- [ ] **Step 3: Implement**

`Steadfast.scala`: move `SteadfastCard` from `HoldingLegacies.scala`
verbatim (id, name, goal, suit, `PrintedPower` and its text), then:

```scala
/** Steadfast (legacy L03): "Your other legacies cannot become dormant—they
  * always stay active." Only Sun step 3 makes a legacy dormant, and it
  * asks `ChronicleLegacyRules.keepsOthersActive` at its start (Legacy
  * Chronicle powers design, ruling 5). Steadfast itself can go dormant,
  * and the cap still discards, as a discard is not a dormancy. This power
  * hooks no window; it is registered so the card counts as implemented. */
case object Steadfast extends ContributingPower:
  val id: PowerId = SteadfastCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

`LegacyChroniclePowers.scala`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.powerresolver.ContributingPower

/** The legacies whose Chronicle powers are implemented (Legacy Chronicle
  * powers design), registered on the walker by `WalkerPowerCatalog`. */
object LegacyChroniclePowers:
  val all: Vector[ContributingPower] = Vector(Steadfast)
```

In `WalkerPowerCatalog.contributing`, append `++ LegacyChroniclePowers.all`
(import `distantlands.gameplay.chronicle.legacies.LegacyChroniclePowers`),
and add a sentence to its doc comment: the legacies' Chronicle powers are
inert until a Chronicle walks their windows.

In `ChronicleLegacyRules`:

```scala
  /** Whether `player` holds Steadfast in effect, so none of their other
    * legacies go dormant (ruling 5). */
  def keepsOthersActive(state: ReadyGame, player: PlayerId): Boolean =
    holder(state, SteadfastCard.id).contains(player)
```

In `ResolveLegacies.dormancy`, read it in the same `Branch`, at the step's
start:

```scala
      val lapsed = for
        player <- LegacyGoals.turnOrder(state)
          if Imperials.role(state, player).contains(Role.Exile)
        steadfast = ChronicleLegacyRules.keepsOthersActive(state, player)
        legacy <- legaciesOf(state, player)
          if legacy.active && !meets(catalog, state, legacy.id, player) &&
            !(steadfast && legacy.id != SteadfastCard.id)
      yield player -> legacy.id
```

and extend `dormancy`'s doc comment: a Steadfast holder's other legacies
stay active. In `NewFoundations.scala`, import `SteadfastCard` from the new
package (the `legacies` vector is unchanged).

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.*"`
Expected: PASS.
Run: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite"`
and read the diff: exactly `legacy.steadfast` removed.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/distantlands/gameplay/chronicle \
  src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Steadfast keeps its holder's other legacies active

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Gatekeeper

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`ChronicleBeforeResolveLegacies`, `ChronicleAfterResolveLegacies`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
  (the two empty windows; `noteKeys`; doc comment)
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Gatekeeper.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `GatekeeperCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (import)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
  (`chronicleSun`)
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/GatekeeperSuite.scala`
- Modify (imports): `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt` (rewritten)

**Interfaces:**
- Consumes: `ChronicleLegacyRules.holder`, `LegacyGoals.meeting`,
  `LegacyGoals.turnOrder`, `Imperials.enemies`.
- Produces: the two windows, both empty in the base procedure, in
  `ChronicleWindows.sun` as `ChronicleRevealRelics,
  ChronicleBeforeResolveLegacies, ChronicleFlipLegacies,
  ChronicleExileDormancy, ChronicleLegacyCap, ChronicleShuffleLegacies,
  ChronicleAfterResolveLegacies, ChronicleCrown, ChronicleAssignTasks`;
  `GatekeeperCard`; `Gatekeeper` (power); `Gatekeeper.decisionId(player)`
  = `s"chronicle.sun.legacy.gatekeeper.${player.value}"`;
  `Gatekeeper.took: NoteKey` ("{0} took {1} from {2} with {3}");
  `LegacyChroniclePowers.sunNoteKeys: Vector[NoteKey]`, the powers' Sun
  lines, which `ChronicleSunProcedure.noteKeys` adds.

- [ ] **Step 1: Write the failing tests**

In `ChronicleWindowsSuite`, the Sun's keys become:

```scala
      Vector("chronicle.sun.reveal-relics",
        "chronicle.sun.before-resolve-legacies", "chronicle.sun.flip-legacies",
        "chronicle.sun.exile-dormancy", "chronicle.sun.legacy-cap",
        "chronicle.sun.shuffle-legacies",
        "chronicle.sun.after-resolve-legacies", "chronicle.sun.crown",
        "chronicle.sun.assign-tasks"),
```

(the fake power's test then counts both new windows unchanged).

`GatekeeperSuite`, on `ChronicleWalk.chronicleWonByP1` (p1 won; p2 and p3
are Exiles):

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.holding.{IronHandCard, RivalCard, SeerCard,
  WealthyCard}
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, ResolveLegacies}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Gatekeeper (L32): "Before players resolve legacies, you may take one
  * active legacy from an enemy if you meet its goal." A step between Sun
  * steps 1 and 2 (ruling 6). */
class GatekeeperSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun,
      answers)

  private def taking(legacy: LegacyId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Gatekeeper.decisionId(p2) =>
      DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Legacy(legacy)))

  private def asked(run: ChronicleWalk.Run): Option[Decide] =
    run.asked.find(_.decisionId == Gatekeeper.decisionId(p2))

  private def offered(run: ChronicleWalk.Run): Vector[DecisionOptionRef] =
    asked(run).get.query.asInstanceOf[DecisionQuery.ChooseMany]
      .options.map(_.ref)

  private def legaciesOf(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.map(_.id)

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.sunNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun), keys,
      run.events).filter(said => keys.exists(_.name == said.key)).map(_.text)

  /** p2 holds Gatekeeper and the Darkest Secret, so meets Seer's goal. */
  private def secretKeeper: Table =
    won.darkestSecret(Some(p2), 1).legacy(p2, GatekeeperCard)

  test("Gatekeeper is implemented"):
    assert(PowerImplementationStatus.implemented(GatekeeperCard.power.id))

  test("the holder is offered exactly the enemies' active legacies whose " +
      "goal they meet, at most one, or none"):
    val run = sun(secretKeeper.legacy(p3, SeerCard)
      .legacy(p3, IronHandCard).legacy(p1, RivalCard, active = false))
    val query = asked(run).get.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((query.min, query.max), (0, 1))
    assertEquals(offered(run), Vector(DecisionOptionRef.Legacy(SeerCard.id)))
    assertEquals(asked(run).get.owner, p2)

  test("the taken legacy joins the holder active, with a line"):
    val run = sun(secretKeeper.legacy(p3, SeerCard), taking(SeerCard.id))
    assert(legaciesOf(run.ready, p2).contains(SeerCard.id))
    assert(!legaciesOf(run.ready, p3).contains(SeerCard.id))
    assert(lines(run).contains(s"p2 took ${SeerCard.id.value} from p3 " +
      s"with ${GatekeeperCard.id.value}"))

  test("declining takes nothing and writes no line (ruling 3)"):
    val run = sun(secretKeeper.legacy(p3, SeerCard))
    assert(legaciesOf(run.ready, p3).contains(SeerCard.id))
    assertEquals(lines(run), Vector.empty)

  test("an Imperial ally's legacy is not an enemy's"):
    val run = sun(secretKeeper.imperial(p2, p3).legacy(p3, SeerCard))
    assertEquals(asked(run), None)

  test("nothing takeable asks nothing, and a dormant Gatekeeper asks " +
      "nothing (ruling 1)"):
    assertEquals(asked(sun(secretKeeper.legacy(p3, IronHandCard))), None)
    assertEquals(asked(sun(won.darkestSecret(Some(p2), 1)
      .legacy(p2, GatekeeperCard, active = false).legacy(p3, SeerCard))), None)

  test("a taken foundation-altering legacy alters its foundation in step 2, " +
      "and the cap then asks (ruling 6)"):
    // p2 is the Chancellor, ruling Ancient City and its Hearth denizen, so
    // meets Wealthy's goal; p3, an Exile, holds Wealthy. p2 then holds four
    // active legacies, and an Imperial keeps them through step 3.
    val table = won.imperial(p2).warbandsAt(AncientCitySite, p2, 1)
      .denizen(VillageIdiotCard, AncientCitySite)
      .legacy(p2, GatekeeperCard).legacy(p2, SeerCard)
      .legacy(p2, IronHandCard).legacy(p3, WealthyCard)
    val run = sun(table, taking(WealthyCard.id))
    assertEquals(run.ready.game.campaign.foundations(FoundationNumber.II),
      FoundationState(FoundationFace.Altered, Set(WealthyCard.id)))
    val cap = run.asked.find(_.decisionId == ResolveLegacies.capDecisionId(p2))
      .get.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((cap.min, cap.max), (1, 1))
    assert(cap.options.map(_.ref).contains(
      DecisionOptionRef.Legacy(WealthyCard.id)))

  test("a walk resumed at the Gatekeeper's question applies the answer, " +
      "and a later question resumes past it"):
    // The walk parks at Gatekeeper, then, after the take changed the
    // lineages, at the cap; each answer is given to a walk rebuilt from the
    // journal.
    val table = won.imperial(p2).warbandsAt(AncientCitySite, p2, 1)
      .denizen(VillageIdiotCard, AncientCitySite)
      .legacy(p2, GatekeeperCard).legacy(p2, SeerCard)
      .legacy(p2, IronHandCard).legacy(p3, WealthyCard)
    val run = sun(table, taking(WealthyCard.id).orElse {
      case decide if decide.decisionId == ResolveLegacies.capDecisionId(p2) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Legacy(IronHandCard.id))) })
    assertEquals(legaciesOf(run.ready, p2).toSet,
      Set(GatekeeperCard.id, SeerCard.id, WealthyCard.id))
    assertEquals(run.asked.map(_.decisionId).filter(_.startsWith(
      "chronicle.sun.legacy")), Vector(Gatekeeper.decisionId(p2),
      ResolveLegacies.capDecisionId(p2)))
```

(`ChronicleWalk.through` answers each park through `rules.resolveWalker`,
which rebuilds the walk from the journaled state: every answer here is a
resumed walk.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.GatekeeperSuite distantlands.gameplay.chronicle.ChronicleWindowsSuite"`
Expected: FAIL to compile: `Not found: GatekeeperCard` in the new package.

- [ ] **Step 3: Implement**

`PowerWindow.scala`, in the Chronicle block:

```scala
  /** Before players resolve legacies, after Reveal Relics: Gatekeeper's
    * step (Legacy Chronicle powers design, ruling 6). Empty with no power. */
  case object ChronicleBeforeResolveLegacies extends OtherWindow:
    val key = "chronicle.sun.before-resolve-legacies"
  /** After Resolve Legacies, before Crown Chancellor: Chronicler's step
    * (ruling 7). Empty with no power. */
  case object ChronicleAfterResolveLegacies extends OtherWindow:
    val key = "chronicle.sun.after-resolve-legacies"
```

`ChronicleWindows.sun` as the interface says; `ChronicleSunProcedure.steps`
adds `ChronicleWindows.step(PowerWindow.ChronicleBeforeResolveLegacies,
Vector.empty)` after Reveal Relics and the after-window, empty, after
Shuffle; `noteKeys` becomes `notes.declared ++ ResolveLegacies.noteKeys ++
LegacyChroniclePowers.sunNoteKeys`; the doc comment names the two windows.

`Gatekeeper.scala`: move `GatekeeperCard` verbatim (its text keeps "Set
Foundation III to Public Ambitions", which `alters` already implements),
then:

```scala
/** Gatekeeper (legacy L32): "Before players resolve legacies, you may take
  * one active legacy from an enemy if you meet its goal." Its step is the
  * window before Flip Legacies (ruling 6). The holder, read when the step
  * is reached, may take one active legacy of a seated enemy whose goal
  * they meet as `LegacyGoals` counts it then, or decline (ruling 3). The
  * legacy joins them active; step 2 alters its foundation if it alters
  * one, and step 4 applies the cap. The Branch's shape reads only the
  * state at its own question, so a walk resumed there finds the same
  * options. Line: "p2 took Seer from p3 with Gatekeeper". */
case object Gatekeeper extends ContributingPower:
  val id: PowerId = GatekeeperCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeResolveLegacies -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.sun.legacy.gatekeeper.${player.value}"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val took: NoteKey = notes.key("gatekeeper-took", Vector(NotePart.Arg(0),
    NotePart.Text(" took "), NotePart.Arg(1), NotePart.Text(" from "),
    NotePart.Arg(2), NotePart.Text(" with "), NotePart.Arg(3)))
  /** The Sun lines this power writes, on the Sun's note id. */
  def sunNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, GatekeeperCard.id).toVector.flatMap(
      holder => takeable(catalog, state, holder) match
        case Vector() => Vector.empty
        case options => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseMany(0, 1,
            options.map((_, legacy) =>
              DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
            heading = Some("Gatekeeper: you may take an active legacy " +
              "from an enemy whose goal you meet"),
            confirm = Some(CountedConfirm("Take", "legacy", "legacies")))),
          Branch((_, pending) => taken(holder, options, pending)))))

  /** Each seated enemy's active legacies whose goal `holder` meets, with
    * their owner, in turn order (R21). */
  private def takeable(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(PlayerId, LegacyId)] = for
    enemy <- LegacyGoals.turnOrder(state)
      if Imperials.enemies(state, holder, enemy)
    lineage <- ChronicleTasks.lineageOf(state, enemy).toVector
    legacy <- state.game.campaign.lineages.get(lineage).toVector
      .flatMap(_.legacies) if legacy.active
      if catalog.legacy(legacy.id).exists(card =>
        LegacyGoals.meeting(state, catalog, card)(holder))
  yield enemy -> legacy.id

  private def taken(holder: PlayerId, options: Vector[(PlayerId, LegacyId)],
      pending: PendingTree): Vector[Operation] =
    val chosen = PowerAnswers.many(pending, decisionId(holder))
      .getOrElse(Vector.empty).collect { case DecisionOptionRef.Legacy(id) => id }
    options.filter((_, legacy) => chosen.contains(legacy)).flatMap(
      (enemy, legacy) => Vector(
        BuildOps((live, _) => Right(Vector(Move(Piece.Card(legacy),
          PositionedLocation(Location.Legacies(lineage(live, enemy))),
          PositionedLocation(Location.Legacies(lineage(live, holder))),
          resultingOrientation = Some(Orientation.FaceUp)))), required = true),
        Note(noteId, _ => Some(took.sourceless(NoteArg.Player(holder),
          NoteArg.Card(legacy), NoteArg.Player(enemy),
          NoteArg.Card(GatekeeperCard.id))))))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage
```

`LegacyChroniclePowers`: `all = Vector(Steadfast, Gatekeeper)`;
`sunNoteKeys = Gatekeeper.sunNoteKeys`.

`ProcedureLog.chronicleSun`: add `Gatekeeper.took -> LogKind.Decision`
and the prefix `"chronicle.sun.legacy."` to `narratedPrefixes`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.* distantlands.application.gamelog.*"`
Expected: PASS except the two snapshot suites.
Run: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite"`
(diff: `legacy.gatekeeper` removed) and
`NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
(diff: the one `gatekeeper-took` key under `procedure.chronicle-sun`).
Run: `./sbtw test`. Expected: PASS; `git status --short src/test/resources`
shows only the two snapshots.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala/distantlands/gameplay/chronicle \
  src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Gatekeeper takes an enemy's legacy before Resolve Legacies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: The open scorepad row follows role changes (open question 3)

Written to recommendation (a); dropped or rewritten if the ruling differs.

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`
  (`roleChanged`, `lineagePointsScored`'s doc comment)
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`

**Interfaces:**
- Produces: after this game's era row is open, `RoleChanged(lineage,
  role)` for a seated lineage also sets that lineage's entry's
  `endingRole`; before it opens, the era is untouched.

- [ ] **Step 1: Write the failing tests**

```scala
  test("a role change after this game's first points updates the open " +
      "scorepad row (Chronicle ruling 19)"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, LineagePointsScored(lineage, 2),
      RoleChanged(lineage, Role.Chancellor)))
    assertEquals(after.game.campaign.era.games.last.entries
      .find(_.lineage == lineage).map(_.endingRole), Some(Role.Chancellor))

  test("a role change before any points leaves the scorepad alone"):
    val ready = Table.start.ready
    val after = accepted(run(ready,
      RoleChanged(lineageOf(ready, p1), Role.Chancellor)))
    assertEquals(after.game.campaign.era, ready.game.campaign.era)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite"`
Expected: FAIL: the entry keeps `Exile`.

- [ ] **Step 3: Implement**

In `roleChanged`'s seated branch, when `campaign.era.games.size >
campaign.baseline.era.games.size`, replace the last game's entry for
`lineage` with `entry.copy(endingRole = role)`. Say in both doc comments
that an entry's `endingRole` follows its lineage's role until the
Chronicle ends, since Chronicler can open the row in the Sun (Legacy
Chronicle powers implementation ruling, open question 3).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.* distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala \
  src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala
git commit -m "feat(chronicle): keep the open scorepad row's roles current

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Chronicler, and Score Lineage Points skipping scored legacies

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Chronicler.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `ChroniclerCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (import)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`
  (`unscored`; `bonus` for any player, skipping scored legacies)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/ChroniclerSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala`
- Modify (imports): `ResolveLegaciesSuite.scala`, `OfferCitizenshipSuite.scala`,
  `LegacyCardDataSuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt` (rewritten)

**Interfaces:**
- Consumes: Task 1's `LegaciesScored`; Task 4's
  `ChronicleAfterResolveLegacies`; `LineageScoring.bonus`.
- Produces: `ChroniclerCard`; `Chronicler` (power);
  `Chronicler.decisionId(player)` =
  `s"chronicle.sun.legacy.chronicler.${player.value}"`; `Chronicler.declare`
  and `Chronicler.decline` (`DecisionOptionRef.Button("declare")`,
  `Button("decline")`); notes `Chronicler.declared` ("{0} declared {1}")
  and `Chronicler.scored` ("{0} scored {1} ({2})");
  `LineageScoring.unscored(ready, player): Vector[LegacyId]`, the legacies
  the player's lineage holds that `scoredLegacies` does not list;
  `LineageScoring.bonus(ready, catalog, player)` counts only those.

- [ ] **Step 1: Write the failing tests**

In `LineageScoringSuite`:

```scala
  test("a legacy already scored this game adds no bonus (ruling 8)"):
    val scored = empireWithCards.legacy(p1, TheNeedleCard)
      .legacy(p1, CircleOfSwordsCard).update(_.updateCurrent(_.copy(
        scoredLegacies = Set(TheNeedleCard.id))))
    assertEquals(bonusOf(scored), Vector(Suit.Nomad -> 1))
```

`ChroniclerSuite`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.holding.{SeerCard, TheNeedleCard}
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, LineageScoring}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.TheOldOakCard
import distantlands.gameplay.powers.poweruse.ForestCouncilCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Chronicler (L02): "After Resolve Legacies, you may declare that all
  * players score their legacies now. Each legacy may only be scored once
  * per game." A step after Sun step 5 (rulings 7 and 8). p1, the
  * Chancellor, won; p2 is a Citizen; the Empire rules Ancient City and its
  * two Beast denizens. */
class ChroniclerSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))

  private def empire: Table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2)
    .warbandsAt(AncientCitySite, p1, 1)
    .denizen(ForestCouncilCard, AncientCitySite)
    .denizen(TheOldOakCard, AncientCitySite)

  private val declaring: PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Chronicler.decisionId(p2) =>
      DecisionAnswer.ChooseOneAnswer(Chronicler.declare)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = declaring)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers)

  private def sunLines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.sunNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun), keys,
      run.events).filter(said => keys.exists(_.name == said.key)).map(_.text)

  private def scoreOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.campaign.era.score(ready.game.current.player(player).get.lineage)

  /** p2 holds Chronicler; p1 holds The Needle (Beast). */
  private def chronicled: Table =
    empire.legacy(p2, ChroniclerCard).legacy(p1, TheNeedleCard)

  test("Chronicler is implemented"):
    assert(PowerImplementationStatus.implemented(ChroniclerCard.power.id))

  test("the holder is asked, Decline first, and declining scores nothing"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleSun,
      PartialFunction.empty)
    val decide = run.asked.find(_.decisionId == Chronicler.decisionId(p2)).get
    assertEquals(decide.owner, p2)
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(Chronicler.decline, Chronicler.declare))
    assertEquals(run.ready.game.current.scoredLegacies, Set.empty)
    assertEquals(sunLines(run), Vector.empty)

  test("declaring scores each seated player's legacy bonus now, with no " +
      "base points, and records every legacy held (rulings 7 and 8)"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleSun)
    // p1's Needle scores the Empire's two Beast denizens; p2's Chronicler
    // (Arcane) scores nothing and writes no line.
    assertEquals(scoreOf(run.ready, p1), 2)
    assertEquals(scoreOf(run.ready, p2), 0)
    assertEquals(run.ready.game.current.scoredLegacies,
      Set(TheNeedleCard.id, ChroniclerCard.id))
    assertEquals(sunLines(run), Vector(
      s"p2 declared ${ChroniclerCard.id.value}",
      "p1 scored 2 lineage points (2 from Beast)"))

  test("Score Lineage Points then scores only the base for the winner's " +
      "legacies Chronicler scored"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleThrone)
    assertEquals(scoreOf(run.ready, p1), 2 + LineageScoring.basePoints)

  test("a Chronicler gone dormant in step 3 is not asked (ruling 1)"):
    // p3, an Exile, rules no site, so misses Chronicler's goal.
    val run = through(empire.legacy(p3, ChroniclerCard)
      .legacy(p1, TheNeedleCard), TriggeredProcedureRef.ChronicleSun)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.sun.legacy.chronicler.")))

  test("a walk resumed at Chronicler's question applies the answer once"):
    // The declaration journals one LegaciesScored, replayed into `ready`.
    val run = through(chronicled.legacy(p2, SeerCard),
      TriggeredProcedureRef.ChronicleSun)
    assertEquals(RecordedOps.all(run.events).collect {
      case scored: LegaciesScored => scored.legacies.toSet },
      Vector(Set(TheNeedleCard.id, ChroniclerCard.id, SeerCard.id)))
```

If open question 3 is ruled (a), add:

```scala
  test("an Exile winner's scorepad entry is the Chancellor's though " +
      "Chronicler opened the row in the Sun (Chronicle ruling 19)"):
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p2, 1)
      .denizen(ForestCouncilCard, AncientCitySite)
      .legacy(p2, ChroniclerCard).legacy(p2, TheNeedleCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone)
    val lineage = run.ready.game.current.player(p1).get.lineage
    assertEquals(run.ready.game.campaign.era.games.last.entries
      .find(_.lineage == lineage).map(_.endingRole), Some(Role.Chancellor))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.ChroniclerSuite distantlands.gameplay.chronicle.LineageScoringSuite"`
Expected: FAIL to compile: `Not found: ChroniclerCard` in the new package.

- [ ] **Step 3: Implement**

`LineageScoring`:

```scala
  /** `player`'s legacies that no scoring has counted yet this game
    * (Legacy Chronicle powers design, ruling 8). */
  def unscored(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ChronicleTasks.lineageOf(ready, player)
      .flatMap(ready.game.campaign.lineages.get).toVector.flatMap(_.legacies)
      .map(_.id).filterNot(ready.game.current.scoredLegacies)
```

`bonus(ready, catalog, player)` reads `unscored(ready, player).flatMap(
catalog.legacy)` in place of the lineage's legacies; rename its parameter
`winner` to `player` and say in its doc comment that Chronicler reads it
for every seated player, and that a scored legacy adds nothing.

`Chronicler.scala`: move `ChroniclerCard` verbatim, then:

```scala
/** Chronicler (legacy L02): "After Resolve Legacies, you may declare that
  * all players score their legacies now. Each legacy may only be scored
  * once per game." Its step is the window after Sun step 5 (ruling 7).
  * The holder, read when the step is reached, declares or declines
  * (ruling 3). On a declaration every seated player, in turn order (R21),
  * scores `LineageScoring.bonus`, with no base and no gifts, and one
  * `LegaciesScored` records every unscored legacy they hold (ruling 8).
  * Lines: "p2 declared Chronicler", then "p1 scored 2 lineage points (2
  * from Beast)" for each player who scores a point. */
case object Chronicler extends ContributingPower:
  val id: PowerId = ChroniclerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleAfterResolveLegacies -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.sun.legacy.chronicler.${player.value}"
  val declare: DecisionOptionRef.Button = DecisionOptionRef.Button("declare")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val declared: NoteKey = notes.key("chronicler-declared", Vector(
    NotePart.Arg(0), NotePart.Text(" declared "), NotePart.Arg(1)))
  val scored: NoteKey = notes.key("chronicler-scored", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2), NotePart.Text(")")))
  def sunNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, ChroniclerCard.id).toVector.flatMap(
      holder => Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(decline, "Decline"),
          DecisionOption.Button(declare, "Declare")),
          heading = Some("Chronicler: you may declare that all players " +
            "score their legacies now"))),
        Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId(holder)).contains(declare)
          then Vector(Note(noteId, _ => Some(declared.sourceless(
              NoteArg.Player(holder), NoteArg.Card(ChroniclerCard.id)))),
            scoring(catalog))
          else Vector.empty))))

  /** Read when reached, after the declaration. */
  private def scoring(catalog: ExecutableCatalog): Operation =
    Branch((state, _) =>
      val players = LegacyGoals.turnOrder(state)
      val legacies = players.flatMap(LineageScoring.unscored(state, _))
      val shares = players.map(player =>
        player -> LineageScoring.bonus(state, catalog, player))
        .filter(_._2.nonEmpty)
      val ops = Option.when(legacies.nonEmpty)(LegaciesScored(legacies))
        .toVector ++ shares.flatMap((player, share) =>
          ChronicleTasks.lineageOf(state, player).map(
            LineagePointsScored(_, share.map(_._2).sum)))
      Option.when(ops.nonEmpty)(BuildOps((_, _) => Right(ops),
        required = true)).toVector ++
        shares.map((player, share) => Note(noteId, _ => Some(
          scored.sourceless(NoteArg.Player(player),
            NoteArg.Amount(share.map(_._2).sum, NoteUnit.LineagePoint),
            NoteArg.SuitShares(share.map((suit, n) => n -> suit)))))))
```

`LegacyChroniclePowers`: `all = Vector(Steadfast, Gatekeeper, Chronicler)`;
`sunNoteKeys = Gatekeeper.sunNoteKeys ++ Chronicler.sunNoteKeys`.

`ProcedureLog.chronicleSun`: `Chronicler.declared -> LogKind.Decision`,
`Chronicler.scored -> LogKind.Delta`.

If open question 1 is ruled (b), `unscored` keeps only active legacies for
Chronicler's reading; if open question 2 is ruled (b), `LegaciesScored`
lists only the legacies whose suit share is positive. Both change only
`scoring`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.* distantlands.application.gamelog.*"`
Expected: PASS except the snapshot suites; rewrite both as in Task 4
(diffs: `legacy.chronicler` removed; the two `chronicler-*` keys added).
Run: `./sbtw test`. Expected: PASS, with only the two snapshots in
`git status --short src/test/resources`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala/distantlands/gameplay/chronicle \
  src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Chronicler scores every player's legacies in the Sun

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: A golden log where Gatekeeper and Chronicler both act

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
  (`sunGatekeeperChronicler`, `named`)
- Create: `src/test/resources/gamelog/sun-gatekeeper-chronicler.actor.log`,
  `src/test/resources/gamelog/sun-gatekeeper-chronicler.other.log`
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala`

**Interfaces:**
- Consumes: `Gatekeeper.decisionId`, `Chronicler.decisionId`,
  `Chronicler.declare`.

- [ ] **Step 1: Write the failing tests**

In `ChronicleLogScripts`:

```scala
  /** The game of [[throneOffered]] with legacies: p2, ruling Broken Peaks
    * (a Discord and an Arcane denizen) and holding the Darkest Secret,
    * holds Gatekeeper and Chronicler; p3 holds Seer. In the Sun p2 takes
    * Seer with Gatekeeper, then declares Chronicler (Legacy Chronicle
    * powers, slice 1). */
  def sunGatekeeperChronicler(using munit.Location): Script =
    val (service, acting) = atTable("sun-gatekeeper-chronicler", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BrokenPeaksSite, p2, 2)
      .denizen(DowntroddenCard, BrokenPeaksSite)
      .denizen(MapLibraryCard, BrokenPeaksSite)
      .darkestSecret(Some(p2), 1)
      .legacy(p2, GatekeeperCard).legacy(p2, ChroniclerCard)
      .legacy(p3, SeerCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == Gatekeeper.decisionId(p2) =>
        ChooseManyAnswer(Vector(DecisionOptionRef.Legacy(SeerCard.id)))
      case park if park.decisionId == Chronicler.decisionId(p2) =>
        ChooseOneAnswer(Chronicler.declare)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("sun-gatekeeper-chronicler", service, p1)
```

and add `"sun-gatekeeper-chronicler" -> (() => sunGatekeeperChronicler)` to
`named`. In `ChronicleLegacyLogSuite`:

```scala
  test("Gatekeeper's and Chronicler's lines read the same to every viewer"):
    val script = ChronicleLogScripts.sunGatekeeperChronicler
    Vector(Some(p1), Some(p2), None).foreach { viewer =>
      val said = format(script, viewer).map(text)
      assert(said.contains(s"${p2.value} took ${SeerCard.name} from " +
        s"${p3.value} with ${GatekeeperCard.name}"), viewer)
      assert(said.contains(s"${p2.value} declared ${ChroniclerCard.name}"),
        viewer)
      assert(said.exists(_.startsWith(s"${p2.value} scored ")), viewer)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.gamelog.ChronicleLegacyLogSuite distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL: the golden files are missing.

- [ ] **Step 3: Write the golden files and review them**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Read both new files line by line. Expected in the Sun, in this order:
"p2 took Seer from p3 with Gatekeeper" (decision);
"Gatekeeper set Foundation III to Public Ambitions" (delta); "p2 declared
Chronicler" (decision); "p2 scored 3 lineage points (1 from Discord, 2
from Arcane)" (delta, by open question 1's recommendation: Gatekeeper,
Chronicler and Seer, all active); then the Crown and the tasks as in
`throne-offered`, and at the Throne "p1 scored 3 lineage points". No
existing golden file may change (`git status --short src/test/resources`
lists only the two new files).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala \
  src/test/resources/gamelog/sun-gatekeeper-chronicler.actor.log \
  src/test/resources/gamelog/sun-gatekeeper-chronicler.other.log
git commit -m "test(gamelog): a Sun golden where Gatekeeper and Chronicler act

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 8: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (the Sun, steps 1-5 and after; the
  Throne, step 6)
- Modify: `docs/rules/ambiguities.md` (a new "Legacy Chronicle powers
  rulings" section)
- Modify: `docs/rules/implementation-traceability.md` (the "Sun task" and
  "Throne task" rows)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md`
  (status line)

- [ ] **Step 1: Docs**

- `chronicle.md`: a "Digital:" note after step 1 for Gatekeeper's step
  (who is asked, the choice, the line, ruling 6); on step 3 for Steadfast
  (read at the step's start, Steadfast itself can go dormant, ruling 5);
  after step 5 for Chronicler (the declaration, the bonus with no base,
  the lines, rulings 7 and 8); on the Throne's step 6, that legacies
  already scored this game add nothing (ruling 8), and the scorepad's
  roles (open question 3's ruling). Each links the new rulings section.
- `ambiguities.md`: after "Legacies rulings", a "Legacy Chronicle powers
  rulings" section introduced as the Legacies section is (the design and
  its implementation rulings), with the design's rulings 1-8 as items 1-8
  and this slice's implementation rulings after them, each with its R
  number.
- Traceability "Sun task" row: Gatekeeper, Steadfast and Chronicler move
  to the evidence (`ChronicleLegacyRulesSuite`, `SteadfastSuite`,
  `GatekeeperSuite`, `ChroniclerSuite`, the `sun-gatekeeper-chronicler`
  golden); the gap keeps the other legacy powers. "Throne task" row: Score
  Lineage Points skips scored legacies (`LineageScoringSuite`).
- Design status: "Slice 1 landed (plan:
  [slice 1](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice1.md))."

UI check: the two decisions use option kinds the app already shows and
answers (a legacy choose-many on the strip, Legacies R27 and slice 5; a
choose-one of buttons). Reaching a Chronicle with these legacies held in
the running app needs a finished game holding them, which no fixture
offers, so the slice is checked through the golden log and the decision
tests; the report says so.

- [ ] **Step 2: Run the verification gate**

In the background: `./sbtw clean coverage test coverageReport`
(statements at or above 92%, branches at or above 84.4%); then
`./sbtw "frontend/test"`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`; `git status --short
src/test/resources` is clean.

- [ ] **Step 3: Commit**

```bash
git add docs/rules/chronicle.md docs/rules/ambiguities.md \
  docs/rules/implementation-traceability.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md
git commit -m "docs(legacies): record the Sun's legacy powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 1: seam and Sun"):
- `ChronicleLegacyRules` and its `holder`: Task 2 (rulings 1 and 2).
- The scored-legacies operation: Task 1 (ruling 8), replayed by
  `LeafApplication`, covered by both codec suites.
- The Gatekeeper and Chronicler windows: Task 4, listed in
  `ChronicleWindowsSuite`.
- Steadfast, Gatekeeper and Chronicler leave holding and count as
  implemented: Tasks 3, 4 and 6 (`CardCatalogSuite`, the unimplemented
  snapshot).
- Score Lineage Points skips scored legacies: Task 6.
- Tests the spec lists: each power's suite from a Chronicle state at its
  step (Tasks 3, 4, 6); a resume test for each new `Decide` (Gatekeeper's
  and Chronicler's resume tests); Steadfast going dormant itself while
  protecting the others (Task 3); Gatekeeper taking a foundation-altering
  legacy, then the cap (Task 4); Chronicler followed by Score Lineage
  Points scoring only the base (Task 6); a boxed lineage's active
  Steadfast doing nothing (Task 3); a golden log where Gatekeeper and
  Chronicler both act (Task 7).
- General rulings: 1 (`holder` reads only seated, active), 2 (`roles`
  read live), 3 (each decision its own id, declining always possible), 4
  (Gatekeeper's single pick is one choose-many; Chronicler picks no card).
- Docs: Task 8 (chronicle.md, ambiguities.md, traceability; the ROADMAP
  item is ticked only after slice 5).

**Placeholder scan:** the one conditional, Task 5 and the Chronicler
scorepad test, depends on open question 3 and says what changes if the
ruling differs. No other step defers its content.

**Type consistency:** `ChronicleLegacyRules.holder(ReadyGame, LegacyId,
Set[Role]): Option[PlayerId]` in Tasks 2-6; `LegaciesScored(Vector[LegacyId])`
and `CurrentGameState.scoredLegacies: Set[LegacyId]` in Tasks 1 and 6;
`ChronicleWalk.through(rules, table, procedure, answers): Run` in Tasks
2-6; `LegacyChroniclePowers.all` and `sunNoteKeys` in Tasks 3-7;
`Gatekeeper.decisionId`, `Chronicler.decisionId`, `Chronicler.declare` in
Tasks 4, 6 and 7.
