# Legacy Chronicle Powers, Slice 3: Relics and the Next Game Implementation Plan

> Executed inline by the implementer.

**Goal:** Four legacies act on relics, advisers and newcomers, some into
the next game. Heirloom's holder, before Scatter Relics, secretly keeps one
relic, which starts the next game with them facedown. The Ear's holder
chooses which scattered relic goes in each empty slot. Oaths of Loyalty's
holder keeps up to three faceup advisers as the next game's facedown
starting advisers, and every other starting adviser they would gain is
buried. The Bright Beacon's holder, resolving the Beacon, may take six
newcomers from the Dispossessed instead of the suited box. The four cards
count as implemented (R7 for The Ear, whose ACTION half stays uncovered).

**Architecture:** The seam of slices 1 and 2 carries on. Heirloom is an
added step: a `Transform` on a new, empty window before Scatter Relics,
journaling a new `ChronicleOperation`, `StartingRelicKept`, which moves the
relic into the lineage's `startingRelics`. The others change steps, each
asking `ChronicleLegacyRules` when reached: Scatter Relics reads
`scatterer`, Gather Denizens reads `keepsAdvisers`, the three starting
adviser keeps read `buriesStartingAdvisers`, `SetupDeal.of` reads the same
rule from the stored Chronicle, and Introduce Newcomers reads
`beaconFromDispossessed`. Hidden cards are shown to the holder alone by
`Peek`, which records knowledge, and two new `identifiesCard` cases (set
aside relics, the Dispossessed) read it.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-4 and 13-17, and section "Slice 3: relics and the next game".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)
R1-R16 (R7: a card with an unbuilt half leaves holding whole). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (The Ear
L19, Heirloom L20, The Bright Beacon L24, Oaths of Loyalty L25).
`docs/rules/chronicle.md`, the Throne steps 4-5, the World steps 2-3 and
the Beacon steps 1-2; Chronicle rulings 15 and 16; Legacies R2 (a boxed
lineage's starting relics). Slice 2's plan for the seam.

## Prerequisite

Slice 2 has landed (through `31c681d8a`). Check before Task 1:

```bash
grep -q "object Reformer" src/main/scala/distantlands/gameplay/chronicle/legacies/Reformer.scala &&
grep -q "def builder" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "ChronicleStartOfThrone" src/main/scala/distantlands/model/PowerWindow.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R17-R27 ([rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)),
each as the plan recommended:

- **R17** (Task 1): Heirloom's step after Stock Reliquary, so a Throne
  holder's Heirloom keeps nothing Stock Reliquary took.
- **R18** (Task 1): a choose-one, no Decline; one relic kept without a
  question; none asks nothing.
- **R19** (Task 1): a public "p2 kept a relic with Heirloom" plus a
  holder-only line naming the relic.
- **R20** (Task 1): `StartingRelicKept(player, relic)`, wire kind
  `"starting-relic-kept"`, refusing the Grand Scepter, a relic not held
  and a relic with tokens; the relic's secrets return first.
- **R21** (Task 2): The Ear asks slot by slot in Hinterward order under one
  reused id while two or more relics remain; the last is placed without a
  question; it stops when relics or empty slots run out; each placed relic
  is peeked for the holder at its site; a resume test parks mid-scatter
  after a first pick and the next pick is a different relic.
- **R22** (Tasks 2 and 5): hidden cards reach their holder by `Peek`, read
  by new `identifiesCard` cases; tests check another seat and the public
  view still see them hidden.
- **R23** (Task 3): Oaths keeps zero to three faceup denizen advisers
  before the gathering; the line names them.
- **R24** (Tasks 3 and 4): the keep questions stay and the kept card is
  buried; under Teeming World an Oaths holder draws nothing.
- **R25** (Task 5): The Bright Beacon's source question, box first; from
  the Dispossessed a peek, then exactly six, taken without a question when
  exactly six remain.
- **R26** (Task 1): `ChronicleBeforeScatterRelics`.
- **R27** (Task 6): two goldens.

## Global Constraints

- Scala 3 with `-Werror`; every Scala file at or under 800 lines.
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`.
- A new test names cards by their card objects.
- Every power reads its holder and role when its window is reached; a
  `Transform` appends a node of constant shape; a `Repeat`'s passes reuse
  their ids and read the latest answer.
- No two decisions share an id; each new id is
  `chronicle.<task>.legacy.<power>.<player>`, a suffix after it for a
  second question of the same power.
- Hidden information: Heirloom's relic, The Ear's examined relics and the
  Beacon's Dispossessed reach their holder alone; each task has a test
  that another seat and the public view do not name them.
- Old saved streams still decode: `StartingRelicKept` is a new wire kind.
- R15: inserted windows and wrapped steps shift a walk parked
  mid-Chronicle by an earlier build.
- TDD, one commit per task; coverage at or above 92% statements and 84.4%
  branches; snapshots and goldens reviewed and committed in their task;
  `git status --short src/test/resources` otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; stage only
  this plan's files; `git log -3` after each commit.

---

### Task 1: Heirloom

**Files:**
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
  (`StartingRelicKept`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
  `ChronicleStateOperations.scala` (`startingRelicKept`)
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`ChronicleBeforeScatterRelics`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`,
  `ChronicleThroneProcedure.scala` (the empty window)
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `HeirloomCard`)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`,
  `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`,
  `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
  `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/HeirloomSuite.scala`
- Modify (imports): tests importing `HeirloomCard` from holding
  (`LineageScoringSuite`, `LegacyCardDataSuite`, and any `grep -rln`
  lists)
- Modify: the two snapshots (rewritten)

**Interfaces:**
- Produces: `StartingRelicKept(player: PlayerId, relic: RelicId)`;
  `PowerWindow.ChronicleBeforeScatterRelics`, after
  `ChronicleStockReliquary` in `ChronicleWindows.throne`; `HeirloomCard`;
  `Heirloom` (power); `Heirloom.decisionId(player)` =
  `s"chronicle.throne.legacy.heirloom.${player.value}"`; notes
  `Heirloom.keptPublic` ("{0} kept a relic with {1}") and
  `Heirloom.keptPrivate` ("{0} kept {1} with {2}", read by the holder).

- [ ] **Step 1: Write the failing tests**

`ChronicleStateOperationsSuite`:

```scala
  test("a kept relic leaves its holder for the lineage's starting relics"):
    val ready = Table.start.relic(p1, CupOfPlentyCard).ready
    val after = accepted(run(ready, StartingRelicKept(p1, CupOfPlentyCard.id)))
    assert(!after.game.current.player(p1).get.relics
      .exists(_.id == CupOfPlentyCard.id))
    assertEquals(after.game.campaign.lineages(lineageOf(ready, p1))
      .startingRelics, Vector(CupOfPlentyCard.id))
    assert(after.game.cardIndex.toOption.get.get(CupOfPlentyCard.id).isEmpty)

  test("the Grand Scepter, a relic not held and a relic with tokens are " +
      "not kept"):
    val ready = Table.start.relic(p1, CupOfPlentyCard).ready
    rejected(run(ready, StartingRelicKept(p2, CupOfPlentyCard.id)))
    rejected(run(Table.start.relic(p1, TheGrandScepterCard).ready,
      StartingRelicKept(p1, TheGrandScepterCard.id)))
    rejected(run(Table.start.relic(p1, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 1).ready,
      StartingRelicKept(p1, CupOfPlentyCard.id)))
```

(If `Table.tokens` sets only site cards, set the relic's tokens through
`update` on the player's `relics`.)

`ChronicleOperationWireSuite`: `StartingRelicKept(PlayerId("red"),
RelicId("R1"))` round-trips as `{"kind": "starting-relic-kept",
"player": "red", "relic": "R1"}`. `WalkerOperationWireSuite.everyOperation`
gains `StartingRelicKept(PlayerId("red"), RelicId("R1"))`. (A catalog id
such as `R07` in a test trips `BackendArchitectureSuite`'s card-object
rule.)
`ChronicleWindowsSuite`: the Throne's keys gain
`"chronicle.throne.before-scatter-relics"` after
`"chronicle.throne.stock-reliquary"`.

In `ChronicleLogScripts`, a script and its registration in `named`:

```scala
  /** The game of [[chronicleStarted]] with Heirloom: p2, holding the
    * Darkest Secret, holds Heirloom, Cup of Plenty and Grand Mask, and at
    * the Throne keeps Cup of Plenty (Legacy Chronicle powers, slice 3). */
  def throneHeirloom(using munit.Location): Script =
    val (service, acting) = atTable("throne-heirloom", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .darkestSecret(Some(p2), 1).relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).legacy(p2, HeirloomCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == Heirloom.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-heirloom", service, p1)
```

In `ChronicleLegacyLogSuite`:

```scala
  test("Heirloom's relic is named to its holder alone (ruling 13; R19)"):
    val script = ChronicleLogScripts.throneHeirloom
    val public = s"${p2.value} kept a relic with ${HeirloomCard.name}"
    val named = s"${p2.value} kept ${CupOfPlentyCard.name} with " +
      HeirloomCard.name
    def heirloomLines(viewer: Option[PlayerId]) =
      format(script, viewer).map(text).filter(line =>
        line == public || line == named)
    assertEquals(heirloomLines(Some(p2)), Vector(public, named))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assertEquals(heirloomLines(viewer), Vector(public), viewer))
```

The script is a golden too (Task 6 writes its files).

`HeirloomSuite`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{GrandMaskCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Heirloom (L20): "Before Scatter Relics, secretly choose one of your
  * relics to keep facedown. You will start the next game with it."
  * (ruling 13; R17-R20). p1, the Chancellor, holds the Throne; p2 is an
  * Exile. */
class HeirloomSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def throne: Table = ChronicleWalk.throneHeldByChancellorP1

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(relic: RelicId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Heirloom.decisionId(p2) =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(relic))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  test("Heirloom is implemented"):
    assert(PowerImplementationStatus.implemented(HeirloomCard.power.id))

  test("the holder keeps one relic of their choice, never the Grand " +
      "Scepter, before Scatter Relics (ruling 13; R18)"):
    val table = throne.relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .relic(p2, TheGrandScepterCard).legacy(p2, HeirloomCard)
    val run = through(table, keeping(GrandMaskCard.id))
    val asked = run.asked.find(_.decisionId == Heirloom.decisionId(p2)).get
    assertEquals(asked.owner, p2)
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref).toSet, Set[DecisionOptionRef](
        DecisionOptionRef.Relic(CupOfPlentyCard.id),
        DecisionOptionRef.Relic(GrandMaskCard.id)))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(GrandMaskCard.id))
    // Scatter Relics took the rest; the Scepter stays with p2.
    assertEquals(run.ready.game.current.player(p2).get.relics.map(_.id),
      Vector(TheGrandScepterCard.id))

  test("a single relic is kept without a question, and none asks nothing " +
      "(R18)"):
    val one = through(throne.relic(p2, CupOfPlentyCard)
      .legacy(p2, HeirloomCard), PartialFunction.empty)
    assert(!one.asked.exists(_.decisionId == Heirloom.decisionId(p2)))
    assertEquals(lineage(one.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val none = through(throne.relic(p2, TheGrandScepterCard)
      .legacy(p2, HeirloomCard), PartialFunction.empty)
    assert(!none.asked.exists(_.decisionId == Heirloom.decisionId(p2)))
    assertEquals(lineage(none.ready, p2).startingRelics, Vector.empty)

  test("a Throne holder's relics are already in the Reliquary, so their " +
      "Heirloom keeps nothing (R17)"):
    val run = through(throne.relic(p1, CupOfPlentyCard)
      .legacy(p1, HeirloomCard), PartialFunction.empty)
    assertEquals(lineage(run.ready, p1).startingRelics, Vector.empty)

  test("a dormant Heirloom keeps nothing (ruling 1)"):
    val run = through(throne.relic(p2, CupOfPlentyCard)
      .legacy(p2, HeirloomCard, active = false), PartialFunction.empty)
    assertEquals(lineage(run.ready, p2).startingRelics, Vector.empty)

  test("a kept relic's secrets return to its holder first, facedown"):
    val table = throne.relic(p2, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 2).legacy(p2, HeirloomCard)
    val before = table.ready.game.current.player(p2).get.board
    val run = through(table, PartialFunction.empty)
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val after = run.ready.game.current.player(p2).get.board
    assertEquals(after.faceDownSecrets, before.faceDownSecrets + 2)

  test("a walk resumed at Heirloom's question keeps the chosen relic"):
    val table = throne.relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .legacy(p2, HeirloomCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      Heirloom.decisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))

  test("the kept relic starts the next game with its holder, facedown " +
      "(ruling 13)"):
    // p2 holds the Darkest Secret, so Heirloom stays active through the Sun.
    val run = ChronicleWalk.through(rules, ChronicleWalk.chronicleWonByP1
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .darkestSecret(Some(p2), 1).legacy(p2, HeirloomCard),
      TriggeredProcedureRef.ChronicleFinish, keeping(CupOfPlentyCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))
    val kept = lineage(run.ready, p2).id
    assertEquals(record.lineages.find(_.id == kept).map(_.startingRelics),
      Some(Vector(CupOfPlentyCard.id)))
    val next = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val seat = next.game.current.players.find(_.lineage == kept).get
    assert(seat.relics.contains(RelicState(CupOfPlentyCard.id,
      Orientation.FaceDown, Tokens.empty)), seat.relics)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.HeirloomSuite distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.ChronicleOperationWireSuite"`
Expected: FAIL to compile: `Not found: StartingRelicKept`.

- [ ] **Step 3: Implement**

The operation (model), its application and its codec as `LegaciesScored`'s
were in slice 1:

```scala
/** Keeps a held relic for the next game (Heirloom; Legacy Chronicle powers
  * design, ruling 13): it leaves the player's relics for their lineage's
  * `startingRelics`, outside play and the card index (Legacies R2), and
  * game start hands it back facedown. The Grand Scepter, a relic the
  * player does not hold and a relic with tokens are refused. */
final case class StartingRelicKept(player: PlayerId, relic: RelicId)
    extends ChronicleOperation, LeafOperation
```

`OperationPipeline.boxed` counts the relic a `StartingRelicKept` removes,
as it counts a card moved to the shared bank: the card-inventory check
otherwise refuses a card that leaves the index.

`ChronicleStateOperations.startingRelicKept(ready, player, relic)` checks
the three refusals (`TheGrandScepterCard.id`, the player's `relics`, an
empty `tokens`), then removes the `RelicState` and appends the id to the
lineage's `startingRelics`.

`Heirloom.scala`: move `HeirloomCard` verbatim, then:

```scala
/** Heirloom (legacy L20): "Before Scatter Relics, secretly choose one of
  * your relics to keep facedown. You will start the next game with it."
  * Its step is the window before Scatter Relics (Legacy Chronicle powers
  * design, ruling 13), after Stock Reliquary. The holder, read when the
  * step is reached, chooses one of their relics but the Grand Scepter; a
  * single one is kept without a question. Its secrets return to the
  * holder facedown, then `StartingRelicKept` keeps it. Lines: "p2 kept a
  * relic with Heirloom" for every viewer, and "p2 kept Cup of Plenty with
  * Heirloom" for the holder alone. */
case object Heirloom extends ContributingPower:
  val id: PowerId = HeirloomCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeScatterRelics -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.heirloom.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val keptPublic: NoteKey = notes.key("heirloom-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a relic with "), NotePart.Arg(1)))
  val keptPrivate: NoteKey = notes.key("heirloom-kept-named", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  val step: Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, HeirloomCard.id).toVector.flatMap(
      holder => keepable(state, holder) match
        case Vector() => Vector.empty
        case Vector(relic) => kept(holder, relic.id)
        case relics => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Heirloom: keep a relic for the next game"),
            confirmVerb = Some("Keep"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(holder))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(kept(holder, _))))))

  private def keepable(state: ReadyGame, holder: PlayerId): Vector[RelicState] =
    state.game.current.player(holder).toVector.flatMap(_.relics)
      .filterNot(_.id == TheGrandScepterCard.id)

  /** The relic's secrets back to `holder`, facedown, then the keep and its
    * two lines. */
  private def kept(holder: PlayerId, relic: RelicId): Vector[Operation] =
    Vector(BuildOps((live, _) => Right(returned(live, holder, relic) :+
        StartingRelicKept(holder, relic)), required = true),
      Note(noteId, _ => Some(keptPublic.sourceless(NoteArg.Player(holder),
        NoteArg.Card(HeirloomCard.id)))),
      Note(noteId, _ => Some(keptPrivate.sourceless(NoteArg.Player(holder),
        NoteArg.Card(relic), NoteArg.Card(HeirloomCard.id))
        .readBy(Vector(holder)))))
```

with `returned` the `Move(Piece.Secrets(n), OnCard(relic), PlayArea(holder))`
and `FlipSecrets(holder, n, FaceUp, FaceDown)` pair Gather Denizens uses
for an adviser's secrets, empty when the relic holds none.

The window, `LegacyChroniclePowers` (`all`, `throneNoteKeys`) and
`ProcedureLog` (both keys `LogKind.Decision`) as in slice 2.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Run the chronicle, catalog, operations, serialization and gamelog suites;
rewrite the unimplemented and note-keys snapshots (diffs:
`legacy.heirloom` removed; `heirloom-kept` and `heirloom-kept-named`
added). Run `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala src/test/scala src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Heirloom keeps a relic for the next game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

(`git add src/main/scala src/test/scala` stages only this task's files;
check `git status --short` first and add file by file if another
session's changes are present.)

---

### Task 2: The Ear

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/TheEar.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `TheEarCard`), `ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`scatterer`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala`
  (`scatter`, doc comment)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`identifiesCard`: `SetAsideRelics`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/TheEarSuite.scala`
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`
  (or a new `HiddenChronicleCardsSuite` if that suite nears its limit)
- Modify: the two snapshots

**Interfaces:**
- Produces: `TheEarCard` (whole text, R7); `TheEar` (power, no window);
  `ChronicleLegacyRules.scatterer(state): Option[PlayerId]`;
  `TheEar.decisionId(player)` =
  `s"chronicle.throne.legacy.the-ear.${player.value}"`; note
  `TheEar.placed` ("{0} chose where the relics went with {1}");
  `ReliquaryAndRelics.emptySlots(catalog, state): Vector[SiteId]`, one
  entry per empty relic slot in Hinterward order (the existing `deal`'s
  `slots`, made a named helper).

- [ ] **Step 1: Write the failing tests**

`TheEarSuite`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.application.{GamePresentationProjector, GameProjector,
  LoadedGame}
import distantlands.gameplay.{BanditCrownCard, GrandMaskCard, OathRules,
  WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, ReliquaryAndRelics}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The Ear (L19), its Chronicle half: "In Scatter Relics, you examine the
  * relics and choose which sites they go to." (ruling 14; R21, R22). p1,
  * the Chancellor, holds the Throne and the Empire rules Ancient City,
  * which has no relic slot; p2, an Exile, holds The Ear and three relics,
  * which Scatter Relics sets aside. */
class TheEarSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def earTable: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
    .relic(p2, BanditCrownCard).legacy(p2, TheEarCard)

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  /** The Ear's questions answered with `relics`, in turn. */
  private def placing(relics: RelicId*)
      : PartialFunction[Decide, DecisionAnswer] =
    var left = relics.toVector
    {
      case decide if decide.decisionId == TheEar.decisionId(p2) =>
        val next = left.head
        left = left.drop(1)
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(next))
    }

  private def asked(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId == TheEar.decisionId(p2))

  private def offered(decide: Decide): Vector[DecisionOptionRef] =
    decide.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref)

  private def relicsAt(ready: ReadyGame, site: SiteId): Vector[RelicId] =
    ready.game.current.map.sites(site).relics.map(_.id)

  private def slots: Vector[SiteId] =
    ReliquaryAndRelics.emptySlots(catalog, earTable.ready)

  test("The Ear is implemented"):
    assert(PowerImplementationStatus.implemented(TheEarCard.power.id))

  test("the holder chooses the relic for each empty slot in Hinterward " +
      "order while two or more remain, the last is placed without a " +
      "question, and nothing is left set aside (ruling 14; R21)"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    assertEquals(asked(run).size, 2)
    assert(asked(run).forall(_.owner == p2))
    assertEquals(relicsAt(run.ready, slots(0)).take(1), Vector(GrandMaskCard.id))
    val placed = slots.take(3).distinct.flatMap(relicsAt(run.ready, _))
    assertEquals(placed.toSet, Set(GrandMaskCard.id, CupOfPlentyCard.id,
      BanditCrownCard.id))
    assert(run.ready.game.current.setAsideRelics.isEmpty)
    val lines = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleThrone),
      LegacyChroniclePowers.throneNoteKeys, run.events).map(_.text)
    assert(lines.contains(s"p2 chose where the relics went with " +
      TheEarCard.id.value), lines)

  test("each question offers only the relics still set aside"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    assertEquals(offered(asked(run)(0)).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(GrandMaskCard.id),
      DecisionOptionRef.Relic(BanditCrownCard.id)))
    assertEquals(offered(asked(run)(1)).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(BanditCrownCard.id)))

  test("with fewer empty slots than relics the rest return to the relic " +
      "deck as before"):
    // Fill every empty slot but one with relics from the relic deck.
    val deck = earTable.ready.game.current.commonCards.relicDeck
    val filled = slots.drop(1).zip(deck).foldLeft(earTable)((table, pair) =>
      table.relicAt(pair._2, pair._1))
    val run = through(filled, PartialFunction.empty)
    // One slot, three relics: one question, then the deck takes the rest.
    assertEquals(asked(run).size, 1)
    assert(run.ready.game.current.setAsideRelics.isEmpty)

  test("without The Ear in effect the deal is the shuffled one, as before"):
    val run = through(ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .legacy(p2, TheEarCard, active = false), PartialFunction.empty)
    assertEquals(asked(run), Vector.empty)
    assert(run.ready.game.current.setAsideRelics.isEmpty)

  test("a walk parked mid-scatter, after a first pick, resumes to the same " +
      "question (R21)"):
    val started = accepted(rules.next(OathTransition(earTable.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val first = ChronicleWalk.walkedTo(rules, started, TheEar.decisionId(p2))
    val next = accepted(rules.resolveWalker(first.state, p2,
      TheEar.decisionId(p2), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(GrandMaskCard.id))))
    val parked = OathTransition(next.state, first.events ++ next.events)
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, TheEar.decisionId(p2))
    // The second pass offers a different set: Grand Mask is placed.
    assert(!offered(decide).contains(DecisionOptionRef.Relic(GrandMaskCard.id)))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(earTable.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    // The second pick is a different relic, and lands in the next slot.
    val done = ChronicleWalk.answeringEverything(rules, OathTransition(
      accepted(rules.resolveWalker(parked.state, p2, TheEar.decisionId(p2),
        DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Relic(BanditCrownCard.id)))).state, Vector.empty))
    val ready = Outcomes.ready(done.state)
    assertEquals(slots.take(2).flatMap(relicsAt(ready, _)).take(2),
      Vector(GrandMaskCard.id, BanditCrownCard.id))

  test("the examined relics are named to the holder alone, and the holder " +
      "still knows each where it lies; another seat and the public do not " +
      "(R22)"):
    val started = accepted(rules.next(OathTransition(earTable.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val atEar = ChronicleWalk.walkedTo(rules, started, TheEar.decisionId(p2))
    val ready = Outcomes.ready(atEar.state)
    val presentation = new GamePresentationProjector(catalog)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id,
      BanditCrownCard.id)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))
    val projector = new GameProjector(catalog)
    val loaded = LoadedGame(atEar.state, 1L)
    assert(projector.project("ear", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.nonEmpty)))
    Vector(projector.project("ear", loaded, p1),
        projector.projectPublic("ear", loaded)).foreach(view =>
      assert(!view.walkerDecision.exists(_.query.nonEmpty)))
    val placed = ChronicleWalk.answeringEverything(rules, atEar)
    val after = Outcomes.ready(placed.state)
    val site = slots.head
    val first = relicsAt(after, site).head
    assert(presentation.identifiesAt(after, Some(p2), first))
    assert(!presentation.identifiesAt(after, Some(p1), first))
```

As written, the suite's last test (the visibility test) moves to a new
`src/test/scala/distantlands/application/HiddenChronicleCardsSuite.scala`:
`GamePresentationProjector` is `private[application]`, and the driver's
`rules.next` is `private[gameplay]`, so the test starts the Throne through
a new `ChronicleWalk.started(rules, table, after)` and reads the Throne's
end through `ChronicleWalk.endOf`. Task 5's visibility test joins it.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: TheEarCard` in the new package.

- [ ] **Step 3: Implement**

`ChronicleLegacyRules.scatterer(state) = holder(state, TheEarCard.id)`.

`ReliquaryAndRelics.scatter`: after the existing discard and `Shuffle`,
a `Branch` reading `scatterer`: with no holder, the existing `deal`;
with one, a `BuildOps` of one `Peek(holder, relic,
Location.SetAsideRelics)` per set-aside relic, then

```scala
      Repeat((live, _) => live.game.current.setAsideRelics.nonEmpty &&
          emptySlots(catalog, live).nonEmpty,
        Sequence(Vector(
          Branch((live, _) => TheEar.ask(catalog, live, holder)),
          Branch((live, pending) => TheEar.place(catalog, live, holder,
            pending))))),
      Note(... TheEar.placed ...),
      BuildOps((live, _) => Right(deal(catalog, live)), required = true)
```

where `TheEar.ask` asks the choose-one over the set-aside relics for the
first empty slot, heading `s"The Ear: choose the relic for ${siteName}"`,
or nothing when one relic remains; and `TheEar.place` moves the latest
answer (or the only relic) facedown to that slot's site and peeks it there
for the holder (`Peek(holder, relic, Location.Site(site))`), once per pass
(a pass places when the count of placements made, read from the site
relics, is below the count of answers plus forced placements; follow
Reformer's `owed` pattern, counting the decision's answers). The final
`deal` returns what is left to the deck, since every slot is full.

`identifiesCard` gains:

```scala
      // A set-aside relic a player peeked: The Ear's holder examines the
      // scattered relics (Legacy Chronicle powers design, ruling 14).
      case CardContainer.SetAsideRelics =>
        viewer.exists(player => ready.knowledge.heldRelics
          .getOrElse(player, Vector.empty).contains(id))
```

`TheEar.scala`: move `TheEarCard` verbatim (whole text, R7), then the
power with no window, its ids, its note, `ask` and `place`.
`LegacyChroniclePowers`, `ProcedureLog` (`placed`, `LogKind.Decision`) as
before.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1 (diffs: `legacy.the-ear` removed; `the-ear-placed` added).
`./sbtw test`: PASS; no golden changes (no script holds The Ear).

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): The Ear chooses where the scattered relics go

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Oaths of Loyalty in the Chronicle

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/OathsOfLoyalty.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `OathsOfLoyaltyCard`), `ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`keepsAdvisers`, `buriesStartingAdvisers`, `oathsHolds(lineage)`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/GatherDenizens.scala`
  (the keep, then the gathering without the kept ones)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/StartingAdviserChoice.scala`
  (the kept card buried for an Oaths holder)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWorldProcedure.scala`
  (`noteKeys`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/OathsOfLoyaltySuite.scala`
- Modify (imports): tests importing `OathsOfLoyaltyCard` from holding
  (`LegacyGoalsSuite`, `LegacyCardDataSuite`, ...)
- Modify: the two snapshots

**Interfaces:**
- Produces: `OathsOfLoyaltyCard`; `OathsOfLoyalty` (power, no window);
  `ChronicleLegacyRules.keepsAdvisers(state): Option[PlayerId]`,
  `buriesStartingAdvisers(state, player): Boolean`, and
  `oathsHolds(lineage: LineageState): Boolean` (an active Oaths in a
  stored lineage, for Task 4's `SetupDeal`);
  `OathsOfLoyalty.keepDecisionId(player)` =
  `s"chronicle.world.legacy.oaths-of-loyalty.${player.value}"`; notes
  `OathsOfLoyalty.keptAdvisers` ("{0} kept {1} with {2}", the cards as
  `NoteArg.Cards`) and `OathsOfLoyalty.buried` ("{0} buried {1} with {2}"),
  each a note of the procedure it is written in (`buriedNotes(noteId)` per
  procedure: the World, the Beacon and, in Task 4, Setup).

- [ ] **Step 1: Write the failing tests**

`OathsOfLoyaltySuite`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{ActingTroupeCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.{BirdsongCard, GolemLegionsCard}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Oaths of Loyalty (L25) in the Chronicle: "When you would gain a starting
  * adviser, bury it, except for— CHRONICLE (WORLD): In Gather Denizens,
  * keep your faceup advisers. They will be your facedown starting advisers
  * in the next game." (rulings 15 and 16; R23, R24). p1 holds the Throne,
  * p2 the World and p3 the Beacon. */
class OathsOfLoyaltySuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val five = Vector(BirdsongCard, GolemLegionsCard, ActingTroupeCard,
    FaeMerchantCard, ThreateningRoarCard)

  /** p2 holds Oaths of Loyalty and the five advisers, faceup. */
  private def fiveAdvisers: Table =
    five.foldLeft(ChronicleWalk.throneHeldByChancellorP1)(_.adviser(p2, _))
      .legacy(p2, OathsOfLoyaltyCard)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers,
      after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(player: PlayerId, cards: distantlands.catalog.Denizen*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == OathsOfLoyalty.keepDecisionId(player) =>
      DecisionAnswer.ChooseManyAnswer(cards.toVector.map(card =>
        DecisionOptionRef.Denizen(card.id)))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef)
      : Vector[String] =
    NoteText.said(ProcedureNotes.id(procedure),
      OathsOfLoyalty.noteKeys(procedure), run.events).map(_.text)

  test("Oaths of Loyalty is implemented"):
    assert(PowerImplementationStatus.implemented(
      OathsOfLoyaltyCard.power.id))

  test("the holder keeps three of five faceup advisers as facedown " +
      "starting advisers, and the rest are gathered (ruling 15; R23)"):
    val run = through(fiveAdvisers, TriggeredProcedureRef.ChronicleWorld,
      keeping(p2, BirdsongCard, GolemLegionsCard, ActingTroupeCard))
    val asked = run.asked.find(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((asked.owner, query.min, query.max, query.options.size),
      (p2, 0, 3, 5))
    val kept = lineage(run.ready, p2).startingAdvisers
    assert(Vector(BirdsongCard, GolemLegionsCard, ActingTroupeCard)
      .forall(card => kept.exists(_.id == card.id)), kept)
    assert(kept.forall {
      case DenizenState(_, orientation, tokens) =>
        orientation == Orientation.FaceDown && tokens.isEmpty
      case _ => false })
    assert(!kept.exists(_.id == FaeMerchantCard.id))
    assert(lines(run, TriggeredProcedureRef.ChronicleWorld).contains(
      s"p2 kept ${BirdsongCard.id.value}, ${GolemLegionsCard.id.value} and " +
        s"${ActingTroupeCard.id.value} with ${OathsOfLoyaltyCard.id.value}"))

  test("a kept adviser's tokens return to their banks first"):
    val suit = catalog.denizen(BirdsongCard.id).get.suit
    val table = fiveAdvisers.tokens(BirdsongCard, favor = 2)
      .bankFavor(suit, 0)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld,
      keeping(p2, BirdsongCard))
    assertEquals(run.ready.banks.favor(suit), 2)

  test("facedown advisers are not offered, and none faceup asks nothing"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .adviser(p2, BirdsongCard, facedown = true)
      .legacy(p2, OathsOfLoyaltyCard)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld)
    assert(!run.asked.exists(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)))

  test("the holder's Protect and Dispossess keep is buried at the world " +
      "deck bottom (ruling 16; R24)"):
    // p2 keeps no adviser with Oaths, then keeps the plainest card in
    // Protect and Dispossess: it is buried, so p2's lineage stores none.
    val run = through(fiveAdvisers, TriggeredProcedureRef.ChronicleWorld)
    assertEquals(lineage(run.ready, p2).startingAdvisers, Vector.empty)
    val buried = lines(run, TriggeredProcedureRef.ChronicleWorld)
      .filter(_.startsWith("p2 buried "))
    assertEquals(buried.size, 1)
    assert(buried.head.endsWith(s" with ${OathsOfLoyaltyCard.id.value}"))
    val card = buried.head.stripPrefix("p2 buried ")
      .stripSuffix(s" with ${OathsOfLoyaltyCard.id.value}")
    // The protected stack's three cards all went to the bottom.
    assert(run.ready.game.current.commonCards.worldDeck.takeRight(3)
      .map(_.value).contains(card))

  test("the holder's Welcome a Stranger keep is buried (ruling 16)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .legacy(p3, OathsOfLoyaltyCard)
    val run = through(table, TriggeredProcedureRef.ChronicleBeacon)
    assertEquals(lineage(run.ready, p3).startingAdvisers, Vector.empty)
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleBeacon)
      .count(_.startsWith("p3 buried ")), 1)

  test("a dormant Oaths keeps nothing and buries nothing (ruling 1)"):
    val table = five.foldLeft(ChronicleWalk.throneHeldByChancellorP1)(
      _.adviser(p2, _)).legacy(p2, OathsOfLoyaltyCard, active = false)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld)
    assert(!run.asked.exists(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)))
    assertEquals(lineage(run.ready, p2).startingAdvisers.size, 1)

  test("a walk resumed at the keep question finds the same five advisers"):
    val started = accepted(rules.next(OathTransition(fiveAdvisers.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      OathsOfLoyalty.keepDecisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, OathsOfLoyalty.keepDecisionId(p2))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(fiveAdvisers.state))(
      (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: OathsOfLoyaltyCard` in the new
package.

- [ ] **Step 3: Implement**

`GatherDenizens.steps` becomes:

```scala
  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => OathsOfLoyalty.keep(catalog, state)),
    BuildOps((state, pending) => Right(gatherAdvisers(catalog, state,
      OathsOfLoyalty.kept(state, pending))), required = true),
    ...the rest unchanged)
```

where `OathsOfLoyalty.keep` reads `keepsAdvisers(state)` and asks the
choose-many over the holder's faceup `DenizenState` advisers, then, in a
`Branch` reading the answer, returns each chosen adviser's tokens (the
same favor and secret moves `gatherAdvisers` writes, made a shared
`GatherDenizens.returnTokens(catalog, player, card)`) and moves it to
`Location.StartingAdvisers(lineage)` with `resultingOrientation =
Some(FaceDown)`, with the `keptAdvisers` line; `OathsOfLoyalty.kept(state,
pending)` is the chosen ids, which `gatherAdvisers` then skips (by then
they have already left the player, so the skip is a guard).

`StartingAdviserChoice.steps` takes the state when its Branch builds it:
for a holder `buriesStartingAdvisers`, the `keepKey` placement moves to
the world deck bottom (`Bury`'s destination, `StackPosition.Bottom`,
facedown) and the line is `OathsOfLoyalty.buried` under the caller's note
id; the question is unchanged (open question 8).

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1. `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Oaths of Loyalty keeps advisers for the next game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Oaths of Loyalty in the next game's Setup

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`
  (`of`: an Oaths holder draws nothing under Teeming World)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`
  (`chooseAdviser`, the kept-adviser line: buried for an Oaths holder)
- Test: `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala`,
  and a new `src/test/scala/distantlands/gameplay/setup/OathsSetupSuite.scala`

**Interfaces:**
- Consumes: Task 3's `ChronicleLegacyRules.oathsHolds`,
  `buriesStartingAdvisers` and `OathsOfLoyalty.buried`.

- [ ] **Step 1: Write the failing tests**

`OathsSetupSuite` (the Teeming World deal is its last test, so `SetupDealSuite` is unchanged):

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** Oaths of Loyalty in the next game's Setup: "When you would gain a
  * starting adviser, bury it." (ruling 16; R24). l1, played by p1, holds
  * Oaths of Loyalty active and stores no starting adviser. */
class OathsSetupSuite extends munit.FunSuite:
  private val p1 = PlayerId("p1")
  private val l1 = LineageId("l1")

  private def holding(active: Boolean, face: FoundationFace): Chronicle =
    val record = LaterGameFixture.chronicle(l1, LineageId("l2"))
    record.copy(
      foundations = record.foundations.updated(FoundationNumber.IV,
        FoundationState(face, Set.empty)),
      lineages = record.lineages.map(lineage =>
        if lineage.id == l1 then lineage.copy(legacies =
          Vector(LegacyState(OathsOfLoyaltyCard.id, active)))
        else lineage))

  private def woken(chronicle: Chronicle)(using munit.Location): ReadyGame =
    Situation.wake(Situation.rules(catalog), chronicle,
      LaterGameFixture.orders(chronicle, p1)).ready

  test("under Ties of Fellowship the holder still picks from a hand of " +
      "three, and the kept card is buried at the world deck bottom"):
    val chronicle = holding(active = true, FoundationFace.Normal)
    val hand = SetupDeal.of(chronicle, FirstGameSetupFixture.participants)
      .hand(LaterGameFixture.orders(chronicle, p1).denizenOrder, p1)
    assertEquals(hand.size, 3)
    val ready = woken(chronicle)
    assertEquals(ready.game.current.player(p1).get.advisers, Vector.empty)
    assert(ready.game.current.commonCards.worldDeck.takeRight(3)
      .exists(card => hand.contains(card)))

  test("a dormant Oaths keeps the picked adviser as before (ruling 1)"):
    val ready = woken(holding(active = false, FoundationFace.Normal))
    assertEquals(ready.game.current.player(p1).get.advisers.size, 1)

  test("under Teeming World the holder draws no starting adviser"):
    val chronicle = holding(active = true, FoundationFace.Altered)
    val deal = SetupDeal.of(chronicle, FirstGameSetupFixture.participants)
    assertEquals(deal.draws.find(_._1 == p1).map(_._2), Some(0))
    assertEquals(woken(chronicle).game.current.player(p1).get.advisers,
      Vector.empty)
    val dormant = SetupDeal.of(holding(active = false,
      FoundationFace.Altered), FirstGameSetupFixture.participants)
    assertEquals(dormant.draws.find(_._1 == p1).map(_._2), Some(3))
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: p1 draws three under Teeming World; the kept card is an
adviser.

- [ ] **Step 3: Implement**

`SetupDeal.of`: under `FoundationFace.Altered`, a participant whose stored
lineage `ChronicleLegacyRules.oathsHolds` draws 0. `SetupProcedure`: the
adviser choice reads `buriesStartingAdvisers(ready, player)` when its
`BuildOps` and `Note` run; for the holder the chosen card moves to the
world deck bottom facedown and the line is `OathsOfLoyalty.buried` under
Setup's note id (declare it in Setup's note keys).

- [ ] **Step 4: Run the tests to verify they pass**

Run `./sbtw test`: PASS. The later-game goldens hold no Oaths and do not
change.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Oaths of Loyalty buries the next game's starting advisers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: The Bright Beacon

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/TheBrightBeacon.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `TheBrightBeaconCard`), `ProcedureLog.scala`
- Modify: `ChronicleLegacyRules.scala` (`beaconFromDispossessed`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/IntroduceNewcomers.scala`
  (the source question, then the ordinary step or the Dispossessed)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala`
  (`noteKeys`)
- Modify: `GamePresentationProjector.scala` (`identifiesCard`:
  `Dispossessed`, `NewlyDispossessed`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/TheBrightBeaconSuite.scala`
- Modify: the visibility suite of Task 2; the two snapshots

**Interfaces:**
- Produces: `TheBrightBeaconCard`; `TheBrightBeacon` (power, no window);
  `ChronicleLegacyRules.beaconFromDispossessed(state): Option[PlayerId]`,
  the Beacon holder when they hold The Bright Beacon in effect;
  `TheBrightBeacon.sourceId(player)` =
  `s"chronicle.beacon.legacy.the-bright-beacon.${player.value}"`,
  `TheBrightBeacon.cardsId(player)` = `sourceId(player) + ".cards"`,
  `TheBrightBeacon.box` and `TheBrightBeacon.dispossessed` (buttons); note
  `TheBrightBeacon.took` ("{0} took six newcomers from the Dispossessed
  with {1}").

- [ ] **Step 1: Write the failing tests**

`TheBrightBeaconSuite`:

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.application.GamePresentationProjector
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleWalk, IntroduceNewcomers,
  WelcomeStranger}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The Bright Beacon (L24): "If you're resolving the Beacon, you may take
  * any six cards from the Dispossessed (instead of choosing suits and
  * taking from their matching dividers)." (ruling 17; R25). p3 holds the
  * Beacon task and The Bright Beacon; five older Dispossessed and three
  * newly Dispossessed denizens wait. */
class TheBrightBeaconSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** Tasks assigned, no Vision left in the world deck (as
    * `BeaconTaskSuite`'s table), then `older` Dispossessed and `newly`
    * newly Dispossessed taken from its denizens. */
  private def beaconTable(older: Int, newly: Int): Table =
    val base = Table.from(ChronicleWalk.throneHeldByChancellorP1.ready
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck = current.commonCards.worldDeck
          .filterNot(_.isInstanceOf[VisionId])))))
    val denizens = base.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }
    val (old, fresh) = denizens.take(older + newly).splitAt(older)
    base.dispossessed(old*).update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(fresh.contains)),
      newlyDispossessed = fresh))).legacy(p3, TheBrightBeaconCard)

  private def all(table: Table): Vector[DenizenId] =
    (table.ready.game.campaign.dispossessed ++
      table.ready.game.current.newlyDispossessed)
      .collect { case id: DenizenId => id }

  private def beacon(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleBeacon,
      answers, after = TriggeredProcedureRef.ChronicleWorld)

  private def fromDispossessed(cards: Vector[DenizenId])
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == TheBrightBeacon.sourceId(p3) =>
      DecisionAnswer.ChooseOneAnswer(TheBrightBeacon.dispossessed)
    case decide if decide.decisionId == TheBrightBeacon.cardsId(p3) =>
      DecisionAnswer.ChooseManyAnswer(cards.map(DecisionOptionRef.Denizen(_)))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  test("The Bright Beacon is implemented"):
    assert(PowerImplementationStatus.implemented(
      TheBrightBeaconCard.power.id))

  test("the holder first chooses the source, the box first (ruling 17)"):
    val run = beacon(beaconTable(5, 3))
    val source = run.asked.head
    assertEquals((source.decisionId, source.owner),
      (TheBrightBeacon.sourceId(p3), p3))
    assertEquals(source.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(TheBrightBeacon.box, TheBrightBeacon.dispossessed))

  test("the box runs the ordinary step"):
    val run = beacon(beaconTable(5, 3))
    assert(run.asked.exists(_.decisionId ==
      IntroduceNewcomers.suitDecisionId(3)))
    assert(!run.asked.exists(_.decisionId == TheBrightBeacon.cardsId(p3)))

  test("from the Dispossessed the holder chooses exactly six of all of " +
      "them, the newly Dispossessed included, and Welcome a Stranger runs " +
      "on those six (R25)"):
    val table = beaconTable(5, 3)
    val six = all(table).drop(2)
    val run = beacon(table, fromDispossessed(six))
    val asked = run.asked.find(_.decisionId == TheBrightBeacon.cardsId(p3)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((query.min, query.max), (6, 6))
    assertEquals(query.options.map(_.ref),
      all(table).map(DecisionOptionRef.Denizen(_)))
    assert(!run.asked.exists(_.decisionId ==
      IntroduceNewcomers.suitDecisionId(3)))
    val welcome = run.asked.find(_.decisionId ==
      WelcomeStranger.keepDecisionId).get
    assertEquals(welcome.query.asInstanceOf[DecisionQuery.Partition].options
      .map(_.ref).toSet, six.map(DecisionOptionRef.Denizen(_)).toSet)
    assertEquals(lineage(run.ready, p3).startingAdvisers.size, 1)
    assert(six.contains(lineage(run.ready, p3).startingAdvisers.head.id))
    val lines = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleBeacon),
      LegacyChroniclePowers.beaconNoteKeys, run.events).map(_.text)
    assert(lines.contains(s"p3 took six newcomers from the Dispossessed " +
      s"with ${TheBrightBeaconCard.id.value}"), lines)

  test("exactly six Dispossessed are taken without a question (R25)"):
    val table = beaconTable(3, 3)
    val run = beacon(table, fromDispossessed(Vector.empty))
    assert(!run.asked.exists(_.decisionId == TheBrightBeacon.cardsId(p3)))
    val welcome = run.asked.find(_.decisionId ==
      WelcomeStranger.keepDecisionId).get
    assertEquals(welcome.query.asInstanceOf[DecisionQuery.Partition].options
      .map(_.ref).toSet, all(table).map(DecisionOptionRef.Denizen(_)).toSet)

  test("a holder not resolving the Beacon is not asked (ruling 17)"):
    val table = beaconTable(5, 3).update(_.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.view.mapValues(lineage =>
        lineage.copy(legacies = lineage.legacies.filterNot(
          _.id == TheBrightBeaconCard.id))).toMap)))
      .legacy(p2, TheBrightBeaconCard)
    val run = beacon(table)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.beacon.legacy.the-bright-beacon.")))

  test("the Dispossessed are named to the holder only after choosing them " +
      "as the source, and never to another seat or the public (R22)"):
    val table = beaconTable(5, 3)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleWorld))
    val atSource = ChronicleWalk.walkedTo(rules, started,
      TheBrightBeacon.sourceId(p3))
    val presentation = new GamePresentationProjector(catalog)
    val older = table.ready.game.campaign.dispossessed
    assert(older.forall(card => !presentation.identifiesAt(
      Outcomes.ready(atSource.state), Some(p3), card)))
    val chosen = accepted(rules.resolveWalker(atSource.state, p3,
      TheBrightBeacon.sourceId(p3), DecisionAnswer.ChooseOneAnswer(
        TheBrightBeacon.dispossessed)))
    val ready = Outcomes.ready(chosen.state)
    assertEquals(ChronicleWalk.parked(chosen.state).map(_.decisionId),
      Some(TheBrightBeacon.cardsId(p3)))
    assert(all(table).forall(presentation.identifiesAt(ready, Some(p3), _)))
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(older.forall(card =>
        !presentation.identifiesAt(ready, viewer, card)), viewer))

  test("a walk resumed at the six-card question finds the same cards"):
    val table = beaconTable(5, 3)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleWorld))
    val atSource = ChronicleWalk.walkedTo(rules, started,
      TheBrightBeacon.sourceId(p3))
    val next = accepted(rules.resolveWalker(atSource.state, p3,
      TheBrightBeacon.sourceId(p3), DecisionAnswer.ChooseOneAnswer(
        TheBrightBeacon.dispossessed)))
    val events = atSource.events ++ next.events
    val decide = ChronicleWalk.parked(next.state).get
    val replayed = accepted(events.foldLeft[Either[OathViolation, OathState]](
      Right(table.state))((state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: TheBrightBeaconCard` in the new
package.

- [ ] **Step 3: Implement**

`IntroduceNewcomers.steps(catalog)` becomes:

```scala
  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => TheBrightBeacon.askSource(state)),
    Branch((state, pending) =>
      if TheBrightBeacon.fromDispossessed(state, pending) then
        TheBrightBeacon.fromDispossessedSteps(state)
      else ordinary(catalog)))
```

where `ordinary(catalog)` is the existing body, and the answer read from
`pending` keeps the branch's shape on resume. `fromDispossessedSteps`:
one `BuildOps` of `Peek(holder, card, location)` for each Dispossessed
denizen (`Location.Dispossessed`, then `Location.NewlyDispossessed`);
then the choose-many of six, or all six without a question; then the
moves into `Location.Hand(holder)` and the `took` line. (Check that a move
out of `NewlyDispossessed` is supported by `CardMovementOperations`; if a
removal arm is missing, add it with a test, as the Stars' own moves read
that pile.)

`identifiesCard` gains:

```scala
      // A Dispossessed card a player peeked: The Bright Beacon's holder
      // examines them (Legacy Chronicle powers design, ruling 17).
      case CardContainer.Dispossessed | CardContainer.NewlyDispossessed =>
        viewer.exists(player => ready.knowledge.advisers
          .getOrElse(player, Vector.empty).contains(id))
```

(Use the `CardContainer` names the index defines for the two piles.)

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1. `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): The Bright Beacon takes newcomers from the Dispossessed

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Golden logs for Oaths and Heirloom

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
  (`chronicleOathsHeirloom`, `throneHeirloom` from Task 1 if not shared)
- Modify: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala`
  (`laterGameAfterOaths`, the next game's Setup from that Chronicle's
  record)
- Create: the four golden files (`chronicle-oaths-heirloom.*.log`,
  `later-game-after-oaths.*.log`)

- [ ] **Step 1: Write the scripts**

In `ChronicleLogScripts` (registered in `named`, with Task 1's
`throne-heirloom`):

```scala
  /** The game of [[chronicleStarted]] with Oaths of Loyalty and Heirloom:
    * p2 holds the Darkest Secret, Cup of Plenty (the highest relic) and
    * Grand Mask, two faceup advisers, Oaths of Loyalty and Heirloom. At the
    * Throne p2 keeps Cup of Plenty with Heirloom; at the World p2 keeps both
    * advisers with Oaths. The journaled Chronicle comes back too, for the
    * next game's script. */
  def oathsHeirloomPlayed(using munit.Location): (Script, Chronicle) =
    val (service, acting) = atTable("chronicle-oaths-heirloom", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .darkestSecret(Some(p2), 1).relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).adviser(p2, BirdsongCard)
      .adviser(p2, GolemLegionsCard).legacy(p2, OathsOfLoyaltyCard)
      .legacy(p2, HeirloomCard).turn(p3, Phase.Act).banditsRefilled)
    val played = acting.withAnswers {
      case park if park.decisionId == Heirloom.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
      case park if park.decisionId == OathsOfLoyalty.keepDecisionId(p2) =>
        ChooseManyAnswer(Vector(BirdsongCard.id, GolemLegionsCard.id)
          .map(DecisionOptionRef.Denizen(_)))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    (Script("chronicle-oaths-heirloom", service, p1),
      played.ready.game.campaign.evolved.get)

  def chronicleOathsHeirloom(using munit.Location): Script =
    oathsHeirloomPlayed._1
```

In `LaterGameLogScripts` (registered in `named`):

```scala
  /** The next game, from the Chronicle [[ChronicleLogScripts.oathsHeirloomPlayed]]
    * journals: p2's lineage starts with its two kept advisers and Cup of
    * Plenty, facedown, and draws no hand (Legacy Chronicle powers, R27). */
  def laterGameAfterOaths(using munit.Location): Script =
    val record = ChronicleLogScripts.oathsHeirloomPlayed._2
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, "later-game-after-oaths"), record,
      LaterGameFixture.orders(record, p1))
    Script("later-game-after-oaths", service, Look(situation.ready).active)
```

A test beside them, in `ChronicleLegacyLogSuite`:

```scala
  test("the next game starts p2's lineage with what Oaths and Heirloom " +
      "kept, facedown (R27)"):
    val record = ChronicleLogScripts.oathsHeirloomPlayed._2
    val lineage = record.lineages.find(_.startingRelics.nonEmpty).get
    assertEquals(lineage.startingRelics, Vector(CupOfPlentyCard.id))
    assertEquals(lineage.startingAdvisers.map(_.id).toSet,
      Set[CardId](BirdsongCard.id, GolemLegionsCard.id))
```

- [ ] **Step 2: Write and review the goldens**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
Read all four files line by line: the Heirloom lines (the public one for
the other seat; both for the actor if the actor is p2), the Oaths keep
line naming both advisers, p2 starting the next game with two facedown
advisers and a facedown relic, drawing no hand. No existing golden
changes.

- [ ] **Step 3: Commit**

```bash
git commit -m "test(gamelog): goldens for an Oaths and Heirloom Chronicle and the next Setup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (Throne steps 4-5, World steps 2-3,
  Beacon steps 1-2), `docs/rules/ambiguities.md` (design rulings 13-17 and
  this slice's implementation rulings), `docs/rules/implementation-traceability.md`
  (Throne, World and Beacon rows; the later-game row for Oaths and the
  kept relic), `docs/ROADMAP.md` (the in-play item names The Ear's ACTION
  half, R7; the "Legacy powers: Chronicle" item drops the four powers this
  slice delivers, as slice 2's review asked), the design's status line.

- [ ] **Step 1: Docs**, as slice 2's Task 5.

- [ ] **Step 2: Run the verification gate**, as slice 2's Task 5, in the
  background. UI check: The Ear's relic picks, Heirloom's pick and the
  Beacon's six-card pick are board or panel picks of kinds the app shows;
  reaching them in the running app needs a finished game holding these
  legacies, which no fixture offers.

- [ ] **Step 3: Commit**

```bash
git commit -m "docs(legacies): record the relic, adviser and newcomer legacy powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 3: relics and the next game"):
- The Heirloom window: Task 1; Scatter Relics reads `scatterer`: Task 2;
  Gather Denizens, Protect and Dispossess and Welcome a Stranger read
  Oaths' queries: Task 3; Setup's adviser pick and Teeming World: Task 4;
  Introduce Newcomers reads `beaconFromDispossessed`: Task 5.
- Heirloom, The Ear's Chronicle half, Oaths of Loyalty and The Bright
  Beacon leave holding: Tasks 1, 2, 3 and 5 (R7 for The Ear).
- Tests the spec lists: Heirloom's relic reaching the next game's start
  facedown, and its redaction (Task 1); The Ear placing chosen relics and
  the rest returning (Task 2); Oaths keeping three of five, burying a
  Protect keep (Task 3), the next Setup burying the hand pick and Teeming
  World dealing nothing (Task 4); The Bright Beacon from the box and from
  the Dispossessed with Welcome a Stranger on the six (Task 5); the golden
  logs (Task 6, open question 11). Resume tests for each new question.
- Hidden choices: Heirloom's relic (Task 1), The Ear's relics (Task 2),
  the Dispossessed (Task 5), each with a viewer test.
- Journal: `StartingRelicKept` (Task 1), replayed by `LeafApplication` and
  covered by both codec suites; Oaths' keeps use `StartingAdvisers`, as
  Protect and Dispossess's do.
- Docs: Task 7.

**Placeholder scan:** every test is written out. The Ear's `place` names
its pattern (Reformer's `owed`), which Task 2 writes against this plan's
tests.

**Type consistency:** `StartingRelicKept(PlayerId, RelicId)`;
`ChronicleLegacyRules.scatterer`, `keepsAdvisers`, `beaconFromDispossessed`
(`ReadyGame => Option[PlayerId]`), `buriesStartingAdvisers(ReadyGame,
PlayerId): Boolean`, `oathsHolds(LineageState): Boolean`; decision ids as
each task's interface defines; `ReliquaryAndRelics.emptySlots` in Task 2.
