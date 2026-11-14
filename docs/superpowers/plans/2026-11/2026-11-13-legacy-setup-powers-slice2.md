# Legacy Setup Powers, Slice 2: Foundations Implementation Plan

> Executed inline by the implementer.

**Goal:** Three legacies change how the foundations resolve in Setup.
World Crafter's holder fills Imperial Maps' slots one by one from either
end of the Atlas, and chooses the end for every successful Explore under
Wide Horizons. Populist's holder may divide the six banks' favor evenly
and take the remainder. Matchmaker's holder keeps a legacy for every other
player in Quiet Ambitions.

**Architecture:** A new `SetupLegacyRules` (in `gameplay/setup/legacies`)
holds the queries the changed steps ask: `mapFiller`, `exploreEndChooser`,
`populist` and `keeper`. Each reads `ChronicleLegacyRules.holder` live,
which cannot change during Setup, so no step's shape depends on the walk.
`ImperialMaps.step` hands the fill to `WorldCrafter.mapStep` when a holder
is seated; Explore's end decision takes `exploreEndChooser` as its actor; a
new empty `SetupBanks` window after Imperial Maps carries Populist's step;
Quiet Ambitions' keep moves from `SetupProcedure` into a new `LegacyKeep`
and asks `keeper` for its actor. The projection names another lineage's
drawn legacies to Matchmaker's holder, and to nobody else, while that
player's keep is the parked decision: it rebuilds Setup's tree and reads
its open decisions, so no state is stored.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Setup powers design](../../specs/2026-11/2026-11-13-legacy-setup-powers-design.md),
rulings 1-4 and 10-13 and section "Slice 2: foundations".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md)
(slice 1's R1-R10 stand; this plan's are R11-R16). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (World
Crafter L13, Matchmaker L14, Populist L16). The rulebook's Foundations I-III,
CR pp. 10-11 (`docs/rules/rules-reference.md#later-games`), the
[Foundations rulings](../../../rules/ambiguities.md#foundations-rulings) and
the [Legacies rulings](../../../rules/ambiguities.md#legacies-rulings).

## Prerequisite

Slice 1 has landed. Check before Task 1:

```bash
grep -q "object StartingLegacies" src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala &&
grep -q "def emptySlotsAtStart" src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala &&
grep -q "object LegacySetupPowers" src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala &&
echo ready
```

Expected: `ready`.

## Rulings

This plan's choices are implementation rulings R11-R16
([rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md)):

- **R11** (Task 1): World Crafter's line, "p2 placed Deep Woods from
  Recent Sites with World Crafter", one per slot, replaces both the
  "filled the map from" line and the "placed X on the map" lines.
- **R12** (Task 1): World Crafter asks for a slot only while two or more
  Atlas sites remain, counted from what game start fixed less the earlier
  slots; a slot with one site left takes it with no question, and its line
  says "from Recent Sites".
- **R13** (Task 1): a World Crafter holder who is the Chancellor still
  fills slot by slot.
- **R14** (Task 3): Populist asks one choose-one, Decline first, whenever a
  seated holder holds it active, even when dividing changes nothing.
- **R15** (Task 3): with no remainder Populist's line is "p2 divided the
  favor evenly among the banks with Populist", a second key.
- **R16** (Task 5): Matchmaker's holder sees another player's two drawn
  legacies only while that player's keep decision,
  `setup.legacy-choice.<owner>`, is pending, which the projection reads
  from the parked decision; no field is saved. Before and after, the draw
  shows as backs to the holder and to the owner. Once one is kept, it is
  named to its owner alone, and the returned one to nobody. The question is
  headed "Matchmaker: keep a legacy for another player". If the projection
  cannot read the pending decision, stop and report it.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`.
- A new test names cards by their card objects (`WorldCrafterCard`).
- Only seated players' active legacies act (spec ruling 1); every "you
  may" is the holder's decision with its own id,
  `setup.legacy.<power>.<player>` and a per-item suffix (ruling 3).
- No `Branch`'s children or ids depend on state the walk changes: World
  Crafter's slot count and questions are read from what game start fixed
  (`ImperialMaps.emptySlotsAtStart`, `ImperialMaps.atlasAtStart`).
- Every new `Decide` has a resume test, and no two decisions share an id.
  Matchmaker's keep keeps its id, `setup.legacy-choice.<player>`, and
  Explore's end keeps `explore.end`; only the actor changes.
- Hidden information: Matchmaker's drawn legacies are named to the holder
  only, and only while that keep is pending (R16), each with a redaction
  test per viewer. World Crafter shows the
  Atlas as Imperial Maps does: the Recent option names the Atlas's front
  site, read when the question is asked.
- Old saved streams still decode: no wire format changes. A game saved
  mid-Setup by an earlier build may not resume (R10).
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. One commit per task.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- Each task that adds a note key or moves a card regenerates
  `src/test/resources/notes/declared-keys.txt` (`NOTE_KEYS_SNAPSHOT=write`)
  and `src/test/resources/powers/unimplemented.txt`
  (`UNIMPLEMENTED_SNAPSHOT=write`) with
  `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.powers.UnimplementedPowersSuite"`,
  reviews the diff line by line, and commits it in the task. A golden is
  generated only for its own script (`GAMELOG_GOLDEN=write ./sbtw 'testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *<name>*'`).
  `git status --short src/test/resources` is otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files with an explicit pathspec; run `git log -3` after each
  commit. Another session commits in this tree.

## The gate

Run after the last task:

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes; coverage at or above 92% statements and
84.4% branches; both scripts pass; the last command prints nothing.

---

### Task 1: World Crafter fills Imperial Maps slot by slot

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/SetupLegacyRules.scala`
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/WorldCrafter.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `WorldCrafterCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala` (`step` delegates; `atlasAtStart` and `optionLabel` become `private[setup]`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala` (`all`, `noteKeys`)
- Modify: `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala` only if it fails to compile (it imports both packages by wildcard)
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/WorldCrafterSetupSuite.scala`

**Interfaces:**
- Consumes: `ImperialMaps.emptySlotsAtStart(ready: ReadyGame): Int` (slice 1), `AtlasPlacement.at`, `AtlasPlacement.suitFavor`, `ChronicleLegacyRules.holder`.
- Produces:
  - `SetupLegacyRules.mapFiller(state: ReadyGame): Option[PlayerId]`.
  - `WorldCrafterCard` and `case object WorldCrafter extends ContributingPower` in `gameplay.setup.legacies`; `WorldCrafter.slotDecisionId(player: PlayerId, slot: Int): String` (slots count from 1); `WorldCrafter.mapStep(catalog: ExecutableCatalog, ready: ReadyGame, holder: PlayerId): Operation`; `WorldCrafter.setupNoteKeys: Vector[NoteKey]`.
  - `ImperialMaps.atlasAtStart(ready: ReadyGame): Int` and `ImperialMaps.optionLabel(catalog, ready, end): String`, both `private[setup]`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.gameplay.setup.{AtlasPlacement, GameStartRules,
  ImperialMaps, LaterGameFixture, SetupLegacyFixture, SetupProcedure,
  SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** World Crafter at Imperial Maps: "you fill site slots, using any mix of
  * Recent and Forgotten Sites (after resolving the Rival legacy)" (spec
  * ruling 10; R11-R13). l1 is the Chancellor, l2 a Citizen, l3 an Exile
  * holding World Crafter; the Empire holds two sites and the Atlas six. */
class WorldCrafterSetupSuite extends munit.FunSuite:
  private val (l1, l2, l3) = (LineageId("l1"), LineageId("l2"),
    LineageId("l3"))
  private val (p1, p3) = (PlayerId("p1"), PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
    WorldCrafterCard.id -> true)
  private val atlas = record.atlasBox.map(_.site)

  /** Slot 2 from the Forgotten end, every other slot from the Recent end. */
  private val mixing: Answers =
    case park if park.decisionId == WorldCrafter.slotDecisionId(p3, 2) =>
      ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))

  private def woken(answers: Answers): Situation =
    Situation.wake(Situation.rules(catalog).withAnswers(answers), record,
      LaterGameFixture.orders(record, p1))

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  /** The decision `situation` is parked on. */
  private def parkedDecide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  /** Every decision Setup asks, each answered by default. */
  private def asked: Vector[Decide] =
    val ready = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, WalkerPowers.empty).asked

  test("the holder fills each slot from the end chosen for it, an Exile " +
      "in the Chancellor's place"):
    val map = woken(mixing).ready.game.current.map
    // Slot 1 Recent, slot 2 Forgotten (the Atlas's last site), then Recent.
    assertEquals(map.inPlay.drop(2), Vector(atlas(0), atlas(5), atlas(1),
      atlas(2), atlas(3), atlas(4)))

  test("each slot is its own question for the holder while two or more " +
      "Atlas sites remain (R12)"):
    val crafter = asked.filter(_.decisionId.startsWith("setup.legacy.world"))
    assertEquals(crafter.map(_.decisionId),
      (1 to 5).toVector.map(WorldCrafter.slotDecisionId(p3, _)))
    assert(crafter.forall(_.owner == p3), crafter)
    assert(!asked.exists(_.decisionId == ImperialMaps.decisionId), asked)

  test("one line per slot names the site and its end (R11)"):
    val said = lines(woken(mixing))
    assertEquals(said.filter(_.contains(WorldCrafterCard.id.value)),
      Vector(atlas(0), atlas(5), atlas(1), atlas(2), atlas(3), atlas(4))
        .zip(Vector("Recent", "Forgotten", "Recent", "Recent", "Recent",
          "Recent")).map((site, end) => s"p3 placed ${site.value} from " +
          s"$end Sites with ${WorldCrafterCard.id.value}"))
    assert(!said.exists(_.contains("filled the map from")), said)
    assert(!said.exists(_.endsWith("on the map")), said)

  test("the Recent option names the Atlas's front site when asked"):
    val parked = Situation.start(Situation.rules(catalog).withAnswers(mixing))
      .parkedAt(WorldCrafter.slotDecisionId(p3, 3))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    val front = catalog.site(atlas(1)).get.name
    assertEquals(parked.ready.game.current.map.inPlay.size, 4)
    parkedDecide(parked).query match
      case one: DecisionQuery.ChooseOne => assertEquals(
        one.options.collect { case DecisionOption.Button(_, label) => label },
        Vector(s"Recent Sites ($front first)", "Forgotten Sites"))
      case other => fail(s"expected a choose-one, got $other")

  test("a walk resumed at a slot's question places the earlier slots once " +
      "and asks the next slot next"):
    val parked = Situation.start(Situation.rules(catalog).withAnswers(mixing))
      .parkedAt(WorldCrafter.slotDecisionId(p3, 2))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    assertEquals(parked.ready.game.current.map.inPlay.size, 3)
    val next = parked.parkedAt(WorldCrafter.slotDecisionId(p3, 3))()
    assertEquals(next.ready.game.current.map.inPlay.drop(2),
      Vector(atlas(0), atlas(5)))

  test("after Rival's sites the holder fills only the slots left (card: " +
      "\"after resolving the Rival legacy\")"):
    val both = SetupLegacyFixture.holding(record, l3, Role.Exile,
      WorldCrafterCard.id -> true,
      distantlands.gameplay.chronicle.legacies.RivalCard.id -> true)
    val map = Situation.wake(Situation.rules(catalog), both,
      LaterGameFixture.orders(both, p1)).ready.game.current.map
    assertEquals(map.inPlay.size, MapState.slots)
    assertEquals(map.inPlay.slice(2, 5), atlas.take(3))

  test("a dormant World Crafter leaves Imperial Maps to the Chancellor " +
      "(ruling 1)"):
    val dormant = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      WorldCrafterCard.id -> false)
    val situation = Situation.wake(Situation.rules(catalog), dormant,
      LaterGameFixture.orders(dormant, p1))
    assert(lines(situation).contains("p1 filled the map from Recent Sites"))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(WorldCrafter))
    assert(PowerImplementationStatus.implemented(WorldCrafterCard.power.id))
```

`SetupWalkDriver` answers every park by default, which keeps Setup's
shape: World Crafter's questions do not depend on the answers. Check that
`DecisionOption.Button` takes `(ref, label)` in that order, as
`ImperialMaps.step` builds it.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.WorldCrafterSetupSuite"`
Expected: compile error, `WorldCrafterCard` is not found in
`gameplay.setup.legacies`.

- [ ] **Step 3: Implement**

`SetupLegacyRules.scala`:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.model._

/** The seam the legacies' SETUP powers share in Setup's walk (Legacy Setup
  * powers design, "Approach"). Each query reads the holder live, through
  * `ChronicleLegacyRules.holder`: nothing in Setup changes who holds a
  * legacy active, so a step built from it keeps its shape on resume. */
object SetupLegacyRules:
  /** Who fills Imperial Maps' slots in the Chancellor's place: World
    * Crafter's holder (ruling 10). */
  def mapFiller(state: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(state, WorldCrafterCard.id)
```

`WorldCrafter.scala`: move `WorldCrafterCard` from `HoldingLegacies.scala`
unchanged, then:

```scala
/** World Crafter (legacy L13), its SETUP half (Legacy Setup powers design,
  * rulings 10 and 11). At Imperial Maps its holder fills the slots in the
  * Chancellor's place, choosing Recent or Forgotten Sites for each slot in
  * Hinterward order (`mapStep`); under Wide Horizons its holder chooses the
  * end for every successful Explore (`ExploreProcedure`). This power hooks
  * no window; the steps ask `SetupLegacyRules`. Lines: "p2 placed Deep
  * Woods from Recent Sites with World Crafter" (R11) and "p3 chose
  * Forgotten Sites with World Crafter". */
case object WorldCrafter extends ContributingPower:
  val id: PowerId = WorldCrafterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** The question for slot `slot`, counted from 1 in Hinterward order. */
  def slotDecisionId(player: PlayerId, slot: Int): String =
    s"setup.legacy.world-crafter.${player.value}.$slot"

  private val setupId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val setupNotes = NoteSet(setupId)
  val placedRecent: NoteKey = setupNotes.key("world-crafter-placed-recent",
    Vector(NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
      NotePart.Text(" from Recent Sites with "), NotePart.Arg(2)))
  val placedForgotten: NoteKey = setupNotes.key(
    "world-crafter-placed-forgotten", Vector(NotePart.Arg(0),
      NotePart.Text(" placed "), NotePart.Arg(1),
      NotePart.Text(" from Forgotten Sites with "), NotePart.Arg(2)))
  /** The Setup lines this power writes, on Setup's note id. */
  def setupNoteKeys: Vector[NoteKey] = setupNotes.declared

  private def placed(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => placedRecent
    case AtlasEnd.Forgotten => placedForgotten

  /** Imperial Maps filled by `holder`: one step per slot game start left
    * empty, each asked while two or more Atlas sites remain (R12). The
    * count and the questions are read from what game start fixed, so a
    * resumed walk finds every slot where it was. */
  def mapStep(catalog: ExecutableCatalog, ready: ReadyGame,
      holder: PlayerId): Operation =
    val atlas = ImperialMaps.atlasAtStart(ready)
    Sequence((1 to ImperialMaps.emptySlotsAtStart(ready)).toVector.map(
      slot => slotStep(catalog, holder, slot, asked = atlas - slot >= 1)))

  /** Slot `slot`: the question, the site from the chosen end with its
    * Foundation II favor, and its line. The question is a `Branch` so that
    * its Recent option names the Atlas's front site when it is asked. */
  private def slotStep(catalog: ExecutableCatalog, holder: PlayerId,
      slot: Int, asked: Boolean): Operation =
    val decisionId = slotDecisionId(holder, slot)
    def end(answered: Vector[Answered]): AtlasEnd = answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }.flatMap(AtlasPlacement.endOf).getOrElse(AtlasEnd.Recent)
    val question = Branch((state, _) =>
      if !asked then Vector.empty
      else Vector(Decide(decisionId, holder, DecisionQuery.ChooseOne(
        AtlasEnd.all.map(option => DecisionOption.Button(
          AtlasPlacement.endOption(option),
          ImperialMaps.optionLabel(catalog, state, option))),
        heading = Some(s"World Crafter: fill slot $slot from")))))
    val place = BuildOps((state, pending) =>
      AtlasPlacement.at(catalog, state, end(pending.answered))
        .map(planned => planned.operation +:
          AtlasPlacement.suitFavor(catalog, planned)),
      required = true)
    val line = Note(setupId, states =>
      states.now.game.current.map.hintermost.map(site =>
        placed(end(states.answered)).sourceless(NoteArg.Player(holder),
          NoteArg.Site(site), NoteArg.Card(WorldCrafterCard.id))))
    Sequence(question, place, line)
```

`asked = atlas - slot >= 1` is "two or more sites remain before slot
`slot`": before slot `n` the Atlas holds `atlas - (n - 1)` sites. Imports:
`distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal, PrintedPower}`,
`distantlands.gameplay.actions.ProcedureNotes`,
`distantlands.gameplay.powerresolver.{Contribution, ContributingPower}`,
`distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps}`,
`distantlands.model._`.

In `ImperialMaps`:
- make `atlasAtStart` and `optionLabel` `private[setup]`;
- at the top of `step`:

```scala
  def step(catalog: ExecutableCatalog, ready: ReadyGame, chancellor: PlayerId)
      : Operation =
    // World Crafter: its holder fills the slots in the Chancellor's place,
    // slot by slot (Legacy Setup powers ruling 10).
    SetupLegacyRules.mapFiller(ready).fold(chancellorFill(catalog, ready,
      chancellor))(WorldCrafter.mapStep(catalog, ready, _))
```

  with the old body renamed `private def chancellorFill`. Update the
  scaladoc tree sketch: "or, with World Crafter's holder seated,
  `WorldCrafter.mapStep`".

`LegacySetupPowers.all` appends `WorldCrafter`; `noteKeys` becomes
`StartLines.noteKeys ++ WorldCrafter.setupNoteKeys`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.catalog.*"`
Expected: every test passes; `ImperialMapsSetupSuite` and `RivalSetupSuite`
unchanged.

- [ ] **Step 5: Regenerate the snapshots and commit**

Regenerate both snapshots (Global Constraints). Expected diff: two
`procedure.setup world-crafter-placed-*` keys added and
`legacy.world-crafter` removed from the unimplemented list.

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/setup \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): World Crafter fills Imperial Maps slot by slot from either end

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 2: World Crafter chooses the end of every Explore

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SetupLegacyRules.scala` (`exploreEndChooser`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/WorldCrafter.scala` (explore keys)
- Modify: `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala` (`endChoice` actor and line; `noteKeys`)
- Modify: `src/test/resources/notes/declared-keys.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/WorldCrafterExploreSuite.scala`
- Create: `src/test/scala/distantlands/application/WorldCrafterExploreProjectionSuite.scala`

**Interfaces:**
- Consumes: `WorldCrafterCard`, `SetupLegacyRules` (Task 1); `ExploreProcedure.endDecisionId`, `ExploreProcedure.chosenEnd`.
- Produces:
  - `SetupLegacyRules.exploreEndChooser(state: ReadyGame, explorer: PlayerId): PlayerId`.
  - `WorldCrafter.choseRecent`, `WorldCrafter.choseForgotten: NoteKey`, `WorldCrafter.chose(end: AtlasEnd): NoteKey`, `WorldCrafter.exploreNoteKeys: Vector[NoteKey]`.

- [ ] **Step 1: Write the failing tests**

`WorldCrafterExploreSuite.scala`, built on `ExploreProcedureSuite`'s table
(copy its `p1AtHintermostOfThree` and helpers; they are private there):

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, GoldenValleySite}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{LogNoted, WalkerCompleted, WalkerDice}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Look, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** World Crafter under Wide Horizons: "when anyone explores, you fill the
  * slot with a Recent or Forgotten Site" (spec ruling 11). p1 explores
  * from Buried Giant, the Hintermost of three sites; p2 holds World
  * Crafter. */
class WorldCrafterExploreSuite extends munit.FunSuite:
  import DefenseDieFace.TwoShields
  private val fourShields: Vector[DieFace] = Vector(TwoShields, TwoShields)

  private def table(holder: PlayerId): ReadyGame = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    .legacy(holder, WorldCrafterCard).ready

  private def rules(dice: WalkerDice): OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = dice)

  private def rolled(ready: ReadyGame): (OathRules, OathTransition) =
    val explore = rules(scripted(fourShields))
    explore -> accepted(explore.startWalker(Ready(ready), ActionRef.Explore,
      p1))

  private def lines(events: Vector[OathEvent]): Vector[String] =
    events.collect { case LogNoted(_, note, _)
      if ExploreProcedure.noteKeys.exists(_.name == note.key) => note.key }

  test("the holder answers another player's Explore end, and a walk " +
      "resumed at it places the chosen site"):
    val (explore, at) = rolled(table(p2))
    val decide = EconomyActions.parkedDecision(at.state)
    assertEquals(decide.decisionId, ExploreProcedure.endDecisionId)
    assertEquals(decide.owner, p2)
    val placed = accepted(explore.resolveWalker(at.state, p2,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Forgotten))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)))
    val after = Outcomes.ready(placed.state)
    assertEquals(Look(after).pawn(p1), GoldenValleySite.id)
    assertEquals(lines(placed.events), Vector("world-crafter-chose-forgotten",
      "explored-forgotten"))

  test("the explorer may not answer in the holder's place"):
    val (explore, at) = rolled(table(p2))
    assert(explore.resolveWalker(at.state, p1, ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))).isLeft)

  test("a holder who explores answers as before, with no World Crafter " +
      "line"):
    val (_, at) = rolled(table(p1))
    assertEquals(EconomyActions.parkedDecision(at.state).owner, p1)
    assert(!at.events.exists {
      case LogNoted(_, note, _) => note.key.startsWith("world-crafter")
      case _ => false
    })
```

Check during execution that `Decide` names its actor `owner` (as
`ImperialMapsSetupSuite` reads `asked.owner`) and that `Table.legacy`
accepts a card object (`LegacyRef`). `WalkerDiceFixture` is imported only
if `scripted` needs it; drop unused imports.

`WorldCrafterExploreProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.WorldCrafterCard
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** World Crafter's holder answers another player's Explore end off turn
  * (spec ruling 11): the holder is shown the question, the explorer and
  * the public who it waits on. */
class WorldCrafterExploreProjectionSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  test("the holder is shown the end question, the explorer waits on them"):
    import DefenseDieFace.TwoShields
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = scripted(Vector(TwoShields, TwoShields)))
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
      .legacy(p2, WorldCrafterCard).ready
    val at = accepted(rules.startWalker(Ready(ready), ActionRef.Explore, p1))
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("explore", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 2)))
    Vector(projector.project("explore", loaded, p1),
        projector.projectPublic("explore", loaded)).foreach(view =>
      assertEquals(view.walkerWaiting.map(_.players), Some(Vector("p2"))))
```

Check the waiting projection's field name for the awaited players
(`WalkerWaitingProjection`) during execution.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.WorldCrafterExploreSuite distantlands.application.WorldCrafterExploreProjectionSuite"`
Expected: the owner is `p1` where `p2` is expected.

- [ ] **Step 3: Implement**

`SetupLegacyRules`:

```scala
  /** Who chooses the end of `explorer`'s successful Explore: World
    * Crafter's holder, who may answer on another player's turn, else the
    * explorer (ruling 11). */
  def exploreEndChooser(state: ReadyGame, explorer: PlayerId): PlayerId =
    ChronicleLegacyRules.holder(state, WorldCrafterCard.id)
      .getOrElse(explorer)
```

`WorldCrafter`:

```scala
  private val exploreId = ProcedureNotes.id(ActionRef.Explore)
  private val exploreNotes = NoteSet(exploreId)
  val choseRecent: NoteKey = exploreNotes.key("world-crafter-chose-recent",
    Vector(NotePart.Arg(0), NotePart.Text(" chose Recent Sites with "),
      NotePart.Arg(1)))
  val choseForgotten: NoteKey = exploreNotes.key(
    "world-crafter-chose-forgotten", Vector(NotePart.Arg(0),
      NotePart.Text(" chose Forgotten Sites with "), NotePart.Arg(1)))
  /** The Explore lines this power writes, on Explore's note id. */
  def exploreNoteKeys: Vector[NoteKey] = exploreNotes.declared

  def chose(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => choseRecent
    case AtlasEnd.Forgotten => choseForgotten
```

`ExploreProcedure.endChoice`:

```scala
    /** Recent or Forgotten, asked while the Atlas holds two sites or more,
      * of World Crafter's holder when one is seated (Legacy Setup powers
      * ruling 11), with their line after the answer. A branch of its own:
      * once the site is placed the Atlas is shorter, and a walk resumed
      * past this node must still find the nodes after it where they were. */
    val endChoice = Branch((ready, _) =>
      if ready.game.campaign.atlas.sites.sizeIs < 2 then Vector.empty
      else
        val chooser = SetupLegacyRules.exploreEndChooser(ready, actor)
        Vector(Decide(endDecisionId, chooser, DecisionQuery.ChooseOne(
          AtlasEnd.all.map(end => DecisionOption.Button(endOption(end),
            AtlasPlacement.endLabel(end))), heading = Some(endHeading)))) ++
          Option.when(chooser != actor)(Note(ProcedureNotes.id(
            ActionRef.Explore), states => Some(WorldCrafter.chose(
            chosenEnd(states.answered)).sourceless(NoteArg.Player(chooser),
            NoteArg.Card(WorldCrafterCard.id))))).toVector)
```

`noteKeys` becomes `notes.declared ++ WorldCrafter.exploreNoteKeys`.
Imports: `distantlands.gameplay.setup.legacies.{SetupLegacyRules,
WorldCrafter, WorldCrafterCard}`. Update the tree sketch:
`Branch(Decide("explore.end", explorer or World Crafter's holder), Note(chose))`.
Update `WorldCrafter`'s scaladoc if it needs to name `ExploreProcedure`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.* distantlands.gameplay.ExploreProcedureSuite distantlands.gameplay.powers.setup.WhenExploredSuite distantlands.application.*Explore*"`
Expected: every test passes; the Explore suites and goldens are unchanged.

- [ ] **Step 5: Regenerate the note snapshot and commit**

Expected diff: two `action.explore world-crafter-chose-*` keys (check the
prefix the snapshot uses for Explore).

```bash
git add src/main/scala/distantlands/gameplay/setup/legacies \
  src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala \
  src/test/scala/distantlands/gameplay/setup/legacies/WorldCrafterExploreSuite.scala \
  src/test/scala/distantlands/application/WorldCrafterExploreProjectionSuite.scala \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): World Crafter chooses the end of every Explore

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 3: The SetupBanks window and Populist

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala` (`SetupBanks`)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (the window after Imperial Maps; tree sketch)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SetupLegacyRules.scala` (`populist`)
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/Populist.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `PopulistCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/setup/SetupWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/PopulistSuite.scala`

**Interfaces:**
- Consumes: `SetupLegacyRules` (Task 1), `PowerAnswers.one`, `Transform`.
- Produces:
  - `PowerWindow.SetupBanks` (key `"setup.banks"`).
  - `SetupLegacyRules.populist(state: ReadyGame): Option[PlayerId]`.
  - `PopulistCard`, `case object Populist extends ContributingPower`; `Populist.decisionId(player: PlayerId): String`; `Populist.decline`, `Populist.divide: DecisionOptionRef.Button`; `Populist.shares(favor: Map[Suit, Int]): (Int, Int)` (each bank's share, the remainder); `Populist.moves(ready: ReadyGame, holder: PlayerId): Vector[CoreOperation]`; `Populist.setupNoteKeys`.

- [ ] **Step 1: Write the failing tests**

`SetupWindowsSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.walker.{LogNoted, WalkerPowers}
import distantlands.model._
import distantlands.testkit.Situation

/** Setup's windows (Legacy Setup powers design, "Walker steps"): a power
  * folded into each runs where the design places it. */
class SetupWindowsSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))

  private case object MarkBanks extends ContributingPower:
    val id: PowerId = PowerId("test.setup.mark-banks")
    val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
    val marked: NoteKey = notes.key("test-banks",
      Vector(NotePart.Text("banks")))
    val contributions: Map[PowerWindow, Vector[Contribution]] = Map(
      PowerWindow.SetupBanks -> Vector(Transform((_, ops) => ops :+ Note(
        ProcedureNotes.id(TriggeredProcedureRef.Setup),
        _ => Some(marked.sourceless()))))))

  private def keys(chronicle: Chronicle): Vector[String] =
    Situation.wake(Situation.rules(catalog, WalkerPowers(Vector(MarkBanks))),
      chronicle, LaterGameFixture.orders(chronicle, PlayerId("p1")))
      .events.collect { case LogNoted(_, note, _) => note.key }

  test("SetupBanks runs once, after Imperial Maps and before the first " +
      "pawn"):
    val said = keys(LaterGameFixture.chronicle(l1, l2))
    assertEquals(said.count(_ == "test-banks"), 1)
    assert(said.lastIndexOf("placed-site") < said.indexOf("test-banks"), said)
    assert(said.indexOf("test-banks") < said.indexOf("placed-pawn"), said)

  test("SetupBanks runs where Imperial Maps would, under Wide Horizons"):
    val record = LaterGameFixture.chronicle(l1, l2)
    val said = keys(record.copy(foundations = record.foundations.updated(
      FoundationNumber.I, FoundationState(FoundationFace.Altered, Set.empty))))
    assertEquals(said.count(_ == "test-banks"), 1)
    assert(said.indexOf("test-banks") < said.indexOf("placed-pawn"), said)

  test("Setup window keys stay as power data spells them"):
    assertEquals(PowerWindow.SetupBanks.key, "setup.banks")
```

`PopulistSuite.scala`:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}
import distantlands.application.GameCommand

/** Populist: "When setting up the favor banks, you may choose to divide the
  * favor among the banks evenly. If you do, take any remainder." (spec
  * ruling 12; R14, R15). l2, a Citizen, holds it. */
class PopulistSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))

  private def holding(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.holding(chronicle, l2, Role.Citizen,
      PopulistCard.id -> true)
  private val record = holding(LaterGameFixture.chronicle(l1, l2))
  private def moversAndShakers(chronicle: Chronicle): Chronicle =
    chronicle.copy(foundations = chronicle.foundations.updated(
      FoundationNumber.II, FoundationState(FoundationFace.Altered, Set.empty)))

  private val dividing: Answers =
    case park if park.decisionId == Populist.decisionId(p2) =>
      ChooseOneAnswer(Populist.divide)

  /** Populist hooks a window, so the walk needs the real power catalog. */
  private val rules = Situation.productionRules(catalog)

  /** Setup parked at p1's pawn, the first question after the banks. */
  private def atPawn(chronicle: Chronicle, answers: Answers): Situation =
    Situation.start(rules.withAnswers(answers))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        chronicle, LaterGameFixture.orders(chronicle, p1)))

  /** The banks and p2's favor just before Populist's question. */
  private def before(chronicle: Chronicle): (Map[Suit, Int], Int) =
    val ready = Situation.start(rules)
      .parkedAt(Populist.decisionId(p2))(GameCommand.Begin(chronicle,
        LaterGameFixture.orders(chronicle, p1))).ready
    ready.banks.favor -> ready.game.current.player(p2).get.board.favor

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  Vector("Powerful Tribes" -> record,
      "Movers and Shakers" -> moversAndShakers(record)).foreach {
    case (face, chronicle) =>
      test(s"dividing under $face leaves every bank the even share and " +
          "gives the holder the remainder"):
        val (banks, favor) = before(chronicle)
        val (each, rest) = Populist.shares(banks)
        val ready = atPawn(chronicle, dividing).ready
        assertEquals(ready.banks.favor, Suit.all.map(_ -> each).toMap)
        assertEquals(ready.game.current.player(p2).get.board.favor,
          favor + rest)
  }

  test("the line names the remainder taken (R15)"):
    val (banks, _) = before(record)
    val (_, rest) = Populist.shares(banks)
    assert(rest > 0, s"the fixture's banks divide evenly: $banks")
    assert(lines(atPawn(record, dividing)).contains(
      s"p2 divided the favor evenly among the banks with " +
        s"${PopulistCard.id.value} and took $rest favor"))

  test("declining leaves the banks as they were and writes no line"):
    val (banks, _) = before(record)
    val declined = atPawn(record, PartialFunction.empty)
    assertEquals(declined.ready.banks.favor, banks)
    assert(!lines(declined).exists(_.contains(PopulistCard.id.value)))

  test("the holder is asked once, Decline first, and a walk resumed at the " +
      "question divides on the answer"):
    val parked = Situation.start(rules.withAnswers(dividing))
      .parkedAt(Populist.decisionId(p2))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    present(accepted(ParkedNode.of(parked.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.query match
        case one: DecisionQuery.ChooseOne => assertEquals(
          one.options.map(_.ref), Vector(Populist.decline, Populist.divide))
        case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected a decision, parked on $other")
    val (each, _) = Populist.shares(parked.ready.banks.favor)
    assertEquals(parked.parkedAt(SetupProcedure.pawnDecisionId(p1))()
      .ready.banks.favor.values.toSet, Set(each))

  test("shares divides by six, rounding down"):
    assertEquals(Populist.shares(Suit.all.map(_ -> 3).toMap
      .updated(Suit.all.head, 4)), (3, 1))
    assertEquals(Populist.shares(Map.empty), (0, 0))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Populist))
    assert(PowerImplementationStatus.implemented(PopulistCard.power.id))
```

`Situation.rules(catalog)` carries no powers, so a window's power never
runs there; Populist's suites use `Situation.productionRules`. If the
fixture's banks happen to
divide evenly (`rest == 0`), store one more denizen at an Empire site in
the test's Chronicle so the line test has a remainder, and add a test of
R15's no-remainder line.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupWindowsSuite distantlands.gameplay.setup.legacies.PopulistSuite"`
Expected: compile errors, `PowerWindow.SetupBanks` and `PopulistCard` are
not found.

- [ ] **Step 3: Implement**

`PowerWindow.scala`, after `SetupPawnPlaced`'s neighbour `SetupEnd`'s
group, before `SetupPawnPlaced`:

```scala
  /** Right after Imperial Maps, or where it would run, before any pawn:
    * when the favor banks are set up (Legacy Setup powers design, ruling
    * 12). Populist hooks here. Empty with no power. */
  case object SetupBanks extends OtherWindow { val key = "setup.banks" }
```

`SetupProcedure.tree`:

```scala
    val banks = Sequence(Vector.empty, Some(PowerWindow.SetupBanks))
    Sequence(LegacySetupPowers.startNotes ++ publicRow(ready) ++
      chancellor.map(reliquaryPick).toVector ++
      imperialMaps ++ Vector(banks) ++ steps :+ setupEnd :+
      BeginTurn(participants.head.playerId, Phase.Wake))
```

and the sketch gains `Sequence(Vector.empty)  // window = SetupBanks`
after `ImperialMaps.step`.

`SetupLegacyRules.populist`:

```scala
  /** Who may divide the banks' favor evenly: Populist's holder (ruling
    * 12). */
  def populist(state: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(state, PopulistCard.id)
```

`Populist.scala`: move `PopulistCard` from `HoldingLegacies.scala`
unchanged, then:

```scala
/** Populist (legacy L16), its SETUP power (Legacy Setup powers design,
  * ruling 12). In the `SetupBanks` window its holder may divide all the
  * favor on the six suit banks evenly: each bank ends with the total
  * divided by six, rounded down, and the holder takes the remainder. It
  * applies under both faces of Foundation II; favor edifices add at
  * `SetupEnd` comes later. One `ChooseOne`, Decline first (R14). Lines:
  * "p2 divided the favor evenly among the banks with Populist and took 1
  * favor", or with no remainder the same without its last words (R15). */
case object Populist extends ContributingPower:
  val id: PowerId = PopulistCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupBanks -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.populist.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  val divide: DecisionOptionRef.Button = DecisionOptionRef.Button("divide")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val dividedAndTook: NoteKey = notes.key("populist-divided-took", Vector(
    NotePart.Arg(0),
    NotePart.Text(" divided the favor evenly among the banks with "),
    NotePart.Arg(1), NotePart.Text(" and took "), NotePart.Arg(2)))
  val divided: NoteKey = notes.key("populist-divided", Vector(
    NotePart.Arg(0),
    NotePart.Text(" divided the favor evenly among the banks with "),
    NotePart.Arg(1)))
  /** The Setup lines this power writes, on Setup's note id. */
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** Each bank's even share of `favor` and the remainder the holder takes. */
  def shares(favor: Map[Suit, Int]): (Int, Int) =
    val total = Suit.all.map(favor.getOrElse(_, 0)).sum
    (total / Suit.all.size, total % Suit.all.size)

  /** The moves that leave every bank its share: each bank's surplus goes to
    * the banks short of it in suit order, and what is left to the holder. */
  def moves(ready: ReadyGame, holder: PlayerId): Vector[CoreOperation] =
    val held = Suit.all.map(suit => suit -> ready.banks.favor.getOrElse(suit, 0))
    val (each, _) = shares(ready.banks.favor)
    val surplus = held.collect { case (suit, n) if n > each => suit -> (n - each) }
    val short = held.collect { case (suit, n) if n < each => suit -> (each - n) }
    def bank(suit: Suit) = PositionedLocation(Location.FavorBank(suit))
    @annotation.tailrec
    def pair(from: List[(Suit, Int)], to: List[(Suit, Int)],
        done: Vector[CoreOperation]): Vector[CoreOperation] = (from, to) match
      case ((giver, give) :: givers, (taker, take) :: takers) =>
        val moved = math.min(give, take)
        pair(if give > moved then (giver, give - moved) :: givers else givers,
          if take > moved then (taker, take - moved) :: takers else takers,
          done :+ Move(Piece.Favor(moved), bank(giver), bank(taker)))
      case (rest, _) => done ++ rest.map((giver, left) => Move(
        Piece.Favor(left), bank(giver),
        PositionedLocation(Location.PlayArea(holder))))
    pair(surplus.toList, short.toList, Vector.empty)

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.populist(state).toVector.flatMap { holder =>
      val (each, rest) = shares(state.banks.favor)
      Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(decline, "Decline"),
          DecisionOption.Button(divide,
            s"Divide evenly: $each favor a bank, $rest for you")),
          heading = Some("Populist: you may divide the banks' favor evenly"))),
        Branch((live, pending) =>
          if !PowerAnswers.one(pending, decisionId(holder)).contains(divide)
          then Vector.empty
          else
            val (_, taken) = shares(live.banks.favor)
            Vector(BuildOps((ready, _) => Right(moves(ready, holder)),
                required = true),
              Note(noteId, _ => Some(
                if taken == 0 then divided.sourceless(NoteArg.Player(holder),
                  NoteArg.Card(PopulistCard.id))
                else dividedAndTook.sourceless(NoteArg.Player(holder),
                  NoteArg.Card(PopulistCard.id),
                  NoteArg.Amount(taken, NoteUnit.Favor))))))
    })
```

`moves` writes no `Move` of 0 favor: a pair moves `min(give, take) > 0`,
and a leftover entry is never 0 because the list drops spent givers. A
`BuildOps` with an empty vector is fine when every bank already holds its
share. Imports: `distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}`,
`distantlands.gameplay.actions.ProcedureNotes`,
`distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
Transform}`, `distantlands.gameplay.powers.PowerAnswers`,
`distantlands.model._`.

`LegacySetupPowers.all` appends `Populist`; `noteKeys` appends
`Populist.setupNoteKeys`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.gamelog.*"`
Expected: every test passes; every existing golden is unchanged (an empty
window writes nothing).

- [ ] **Step 5: Regenerate the snapshots and commit**

Expected diff: `procedure.setup populist-divided` and
`populist-divided-took` added, `legacy.populist` removed.

```bash
git add src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/setup \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Populist may divide the banks' favor evenly in a new SetupBanks window

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 4: Move Quiet Ambitions' keep into `LegacyKeep`

A refactor with no behaviour change, so Task 5 changes one small file and
`SetupProcedure` stays well under its size limit.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (`keepLegacy`, `keptLegacyNote`, `drawnLegacies`, `returnLegacies` move out; `legacyDecisionId` and `keptLegacy` delegate)

**Interfaces:**
- Produces:
  - `LegacyKeep.decisionId(player: PlayerId): String` (`setup.legacy-choice.<player>`, unchanged).
  - `LegacyKeep.keptLegacy: NoteKey` (key `kept-legacy`, unchanged, still declared through `SetupProcedure.noteKeys`).
  - `LegacyKeep.drawn(ready: ReadyGame, lineage: LineageId): Vector[LegacyId]`.
  - `LegacyKeep.step(participant: FirstGameParticipant): Operation`.
  - `LegacyKeep.noteKeys: Vector[NoteKey]`.

- [ ] **Step 1: Run the guarding suites green before the move**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.*Setup* distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: every test passes.

- [ ] **Step 2: Move**

`LegacyKeep.scala` takes, unchanged in behaviour, `keepLegacy` (as
`step`), `keptLegacyNote`, `drawnLegacies` (as public `drawn`) and
`returnLegacies`, with their scaladoc, and its own
`NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))` holding
`keptLegacy`. The key's name and template stay `kept-legacy` and
"{0} kept a legacy". In `SetupProcedure`:

```scala
  def legacyDecisionId(player: PlayerId): String =
    LegacyKeep.decisionId(player)
```

`keptLegacy` is removed from `SetupProcedure`'s own `notes`; its callers
use `LegacyKeep.keptLegacy`. `SetupProcedure.noteKeys` appends
`LegacyKeep.noteKeys`. `playerStep` calls `LegacyKeep.step(participant)`.
Update every test reference to `SetupProcedure.keptLegacy`
(`grep -rn "SetupProcedure.keptLegacy" src/test`).

- [ ] **Step 3: Run the guarding suites green after the move**

Run the Step 1 command, then the snapshot suites:
`./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`.
Expected: every test passes with no snapshot or golden rewritten (the key
keeps its name and its note id; check the snapshot sorts by key, so its
place does not move).

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala \
  src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala
git commit -m "refactor(setup): move Quiet Ambitions' keep into LegacyKeep

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Add any test file the grep in Step 2 changed to the pathspec.

### Task 5: Matchmaker keeps for the other players

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SetupLegacyRules.scala` (`keeper`)
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/Matchmaker.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `MatchmakerCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala` (actor, heading, line; `matchmade`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala` (`identifiesCard`, the lineage legacies case and its scaladoc)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala`
- Modify: tests importing `MatchmakerCard` from `catalog.holding`: `ResolveLegaciesSuite`, `LegacyGoalsSuite`, `SteadfastSuite`, `IronHandSuite` (`LegacyCardDataSuite` uses wildcards)
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/MatchmakerSuite.scala`
- Create: `src/test/scala/distantlands/application/MatchmakerProjectionSuite.scala`

**Interfaces:**
- Consumes: `LegacyKeep.drawn`, `LegacyKeep.decisionId`, `LegacyKeep.step` (Task 4); `SetupLegacyRules` (Task 1).
- Produces:
  - `SetupLegacyRules.keeper(state: ReadyGame, owner: PlayerId): PlayerId`.
  - `MatchmakerCard`, `case object Matchmaker extends ContributingPower`; `Matchmaker.keptFor: NoteKey`; `Matchmaker.setupNoteKeys`.
  - `LegacyKeep.matchmade(ready: ReadyGame, lineage: LineageId, legacy: LegacyId): Option[(PlayerId, PlayerId)]`: the owner and Matchmaker's holder, when `legacy` is part of a two-legacy draw that the holder keeps for that owner (R16).
  - `GamePresentationProjector.keepPending(ready: ReadyGame, owner: PlayerId): Boolean` (private): whether Setup is parked on `LegacyKeep.decisionId(owner)`.

- [ ] **Step 1: Write the failing tests**

`MatchmakerSuite.scala`:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.gameplay.setup.{LaterGameFixture, LegacyDeal, LegacyKeep,
  SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Matchmaker in Quiet Ambitions: "Whenever players draw legacies from the
  * deck and choose one to keep, you draw and choose for them instead."
  * (spec ruling 13). l3, an Exile, holds it. */
class MatchmakerSuite extends munit.FunSuite:
  private val (l1, l2, l3) = (LineageId("l1"), LineageId("l2"),
    LineageId("l3"))
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"),
    PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
    MatchmakerCard.id -> true)
  private val orders = LaterGameFixture.orders(record, p1)
  private val deal = LegacyDeal.of(record, orders.participants)
  private def drawn(player: PlayerId): Vector[LegacyId] =
    deal.drawn(orders.legacyOrder, player)

  private def parkedAt(decisionId: String): Situation =
    Situation.start(Situation.rules(catalog)).parkedAt(decisionId)(
      GameCommand.Begin(record, orders))

  private def owner(situation: Situation): PlayerId =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.owner
      case other => fail(s"expected the keep, parked on $other")

  test("the holder answers every other player's keep, under its own id"):
    Vector(p1, p2).foreach(player => assertEquals(
      owner(parkedAt(LegacyKeep.decisionId(player))), p3))

  test("the holder keeps their own draw"):
    assertEquals(owner(parkedAt(LegacyKeep.decisionId(p3))), p3)

  test("a walk resumed at the keep keeps the holder's choice for its owner"):
    val kept = drawn(p2).last
    val done = parkedAt(LegacyKeep.decisionId(p2)).withAnswers {
      case park if park.decisionId == LegacyKeep.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(kept))
    }.after()
    val legacies = done.ready.game.campaign.lineages(l2).legacies
    assert(legacies.contains(LegacyState(kept, active = false)), legacies)
    assert(!legacies.exists(_.id == drawn(p2).head), legacies)

  test("the line names the keeper and the owner, never the legacy"):
    val woken = Situation.wake(Situation.rules(catalog), record, orders)
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text)
    assert(said.contains(s"p3 kept a legacy for p1 with " +
      MatchmakerCard.id.value), said)
    assert(said.contains(s"p3 kept a legacy for p2 with " +
      MatchmakerCard.id.value), said)
    assert(said.contains("p3 kept a legacy"), said)

  test("a player who drew one legacy keeps it with no question"):
    // A legacy deck of four leaves p3's draw one card short (Legacies R9).
    val short = orders.copy(legacyOrder = orders.legacyOrder.take(5))
    val ready = accepted(distantlands.gameplay.setup.GameStartRules.evolve(
      catalog, record, short))
    val asked = distantlands.gameplay.setup.SetupWalkDriver.driveWithEvents(
      ready, accepted(SetupProcedure.build(catalog, ready,
        ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup,
      distantlands.gameplay.walker.WalkerPowers.empty).asked
    assert(!asked.exists(_.decisionId == LegacyKeep.decisionId(p3)), asked)

  test("under Public Ambitions nobody draws, so the power does nothing"):
    val public = record.copy(foundations = record.foundations.updated(
      FoundationNumber.III, FoundationState(FoundationFace.Altered,
        Set.empty)))
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, Situation.wake(Situation.rules(catalog),
        public, LaterGameFixture.orders(public, p1)).events).map(_.text)
    assert(!said.exists(_.contains(MatchmakerCard.id.value)), said)

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Matchmaker))
    assert(PowerImplementationStatus.implemented(MatchmakerCard.power.id))
```

The one-legacy test must be checked against game start: `GameStartRules`
refuses a `legacyOrder` that is not the derived legacy deck
(`validateDeal`), so a shorter deck needs a Chronicle whose lineages hold
most of the catalog's legacies, as `LegacySetupSuite`'s short-deck test
builds it; reuse that test's setup in place of `take(5)`, and pick the
player who draws one by the deal order. Move the fully qualified names to
imports when writing the file.

`MatchmakerProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.setup.{LaterGameFixture, LegacyDeal, LegacyKeep,
  SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.MatchmakerCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Situation

/** Who sees a draw Matchmaker's holder keeps (spec ruling 13; R16): the
  * holder, and not the owner, while it waits; the kept legacy, its owner
  * alone once kept; the returned one, nobody. p3 holds Matchmaker. */
class MatchmakerProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)
  private val (l3, p1, p2, p3) = (LineageId("l3"), PlayerId("p1"),
    PlayerId("p2"), PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2")), l3,
    Role.Exile, MatchmakerCard.id -> true)
  private val orders = LaterGameFixture.orders(record, p1)
  private val drawn = LegacyDeal.of(record, orders.participants)
    .drawn(orders.legacyOrder, p2)

  private def parkedAt(decisionId: String) =
    Situation.start(Situation.rules(catalog).withAnswers {
      case park if park.decisionId == LegacyKeep.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(drawn.last))
    }).parkedAt(decisionId)(GameCommand.Begin(record, orders))

  test("before p2's keep is reached, nobody identifies p2's draw (R16)"):
    val at = parkedAt(LegacyKeep.decisionId(p1))
    drawn.foreach(legacy => Vector(Some(p3), Some(p2), Some(p1), None)
      .foreach(viewer => assert(
        !presentation.identifiesAt(at.ready, viewer, legacy),
        s"$viewer $legacy")))

  test("while p2's keep waits, only the holder identifies p2's draw"):
    val at = parkedAt(LegacyKeep.decisionId(p2))
    drawn.foreach { legacy =>
      assert(presentation.identifiesAt(at.ready, Some(p3), legacy), legacy)
      Vector(Some(p2), Some(p1), None).foreach(viewer =>
        assert(!presentation.identifiesAt(at.ready, viewer, legacy),
          s"$viewer $legacy"))
    }
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("match", loaded, p3).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 2)))
    assert(!projector.project("match", loaded, p2).walkerDecision
      .exists(_.query.nonEmpty))

  test("once kept, the owner alone identifies the kept legacy and nobody " +
      "the returned one"):
    val after = parkedAt(LegacyKeep.decisionId(p3)).ready
    assert(presentation.identifiesAt(after, Some(p2), drawn.last))
    Vector(Some(p3), Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(after, viewer, drawn.last),
        viewer))
    Vector(Some(p2), Some(p3), Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(after, viewer, drawn.head), viewer))

  test("without Matchmaker the owner identifies their own draw"):
    val plain = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val plainOrders = LaterGameFixture.orders(plain, p1)
    val own = LegacyDeal.of(plain, plainOrders.participants)
      .drawn(plainOrders.legacyOrder, p2)
    val at = Situation.start(Situation.rules(catalog))
      .parkedAt(LegacyKeep.decisionId(p2))(GameCommand.Begin(plain,
        plainOrders)).ready
    own.foreach(legacy => assert(presentation.identifiesAt(at, Some(p2),
      legacy), legacy))
```

`drawn` is read from the deal of `record`, so it matches what game start
dealt p2.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.MatchmakerSuite distantlands.application.MatchmakerProjectionSuite"`
Expected: compile error, `MatchmakerCard` is not found in
`gameplay.setup.legacies`.

- [ ] **Step 3: Implement**

`SetupLegacyRules.keeper`:

```scala
  /** Who answers `owner`'s Quiet Ambitions keep: Matchmaker's holder, for
    * every player but themselves, else the owner (ruling 13). */
  def keeper(state: ReadyGame, owner: PlayerId): PlayerId =
    ChronicleLegacyRules.holder(state, MatchmakerCard.id).getOrElse(owner)
```

`Matchmaker.scala`: move `MatchmakerCard` unchanged, then:

```scala
/** Matchmaker (legacy L14), its Setup half (Legacy Setup powers design,
  * ruling 13). In Quiet Ambitions' keep its holder answers every other
  * player's keep, under the keep's own id; the holder sees both drawn
  * legacies while that keep is open, and the owner only the one kept for
  * them (`LegacyKeep.matchmade`, R16). Draws during a game (Amber Doors,
  * Lost Doors, Ancient Writ) wait for their batch. This power hooks no
  * window. Line: "p3 kept a legacy for p2 with Matchmaker". */
case object Matchmaker extends ContributingPower:
  val id: PowerId = MatchmakerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  val keptFor: NoteKey = notes.key("matchmaker-kept-for", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy for "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared
```

`LegacyKeep.step`: the `Decide`'s actor and the line come from
`SetupLegacyRules.keeper(state, participant.playerId)`, read in the same
`Branch` that reads the draw:

```scala
        case drawn =>
          val keeper = SetupLegacyRules.keeper(state, participant.playerId)
          Vector(
            Decide(decisionId, keeper, DecisionQuery.ChooseOne(
              drawn.map(id =>
                DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
              heading = Some(
                if keeper == participant.playerId then "Keep a legacy"
                else "Matchmaker: keep a legacy for another player"),
              confirmVerb = Some("Keep"))),
            BuildOps(returnLegacies(participant.lineageId, drawn, decisionId)),
            keptNote(keeper, participant.playerId))
```

with

```scala
  private def keptNote(keeper: PlayerId, owner: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ => Some(
      if keeper == owner then keptLegacy.sourceless(NoteArg.Player(owner))
      else Matchmaker.keptFor.sourceless(NoteArg.Player(keeper),
        NoteArg.Player(owner), NoteArg.Card(MatchmakerCard.id))))
```

A one-legacy draw still writes "p2 kept a legacy" with no question. Add:

```scala
  /** The owner and Matchmaker's holder when `legacy` is in a two-legacy
    * draw, during Setup, that the holder keeps for that owner (R16). The
    * owner never identifies such a card; the holder does only while the
    * owner's keep is the parked decision, which the projection checks. None
    * when the owner keeps for themselves, once one is kept, and outside
    * Setup. */
  def matchmade(ready: ReadyGame, lineage: LineageId, legacy: LegacyId)
      : Option[(PlayerId, PlayerId)] =
    val waiting = drawn(ready, lineage)
    for
      owner <- ready.game.current.players.find(_.lineage == lineage)
        .map(_.player)
      if ready.game.current.turn.phase == Phase.Setup &&
        waiting.sizeIs >= 2 && waiting.contains(legacy)
      keeper = SetupLegacyRules.keeper(ready, owner)
      if keeper != owner
    yield owner -> keeper
```

`GamePresentationProjector.identifiesCard`, the lineage legacies case:

```scala
      // Matchmaker's holder chooses another player's kept legacy: the
      // draw is named to the holder alone, and only while that keep is the
      // parked decision; the owner sees backs until one is kept (Legacy
      // Setup powers ruling 13; R16).
      case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
        val matchmade = id match
          case legacy: LegacyId if !faceup =>
            LegacyKeep.matchmade(ready, lineage, legacy)
          case _ => None
        matchmade.fold(
          faceup || ready.game.current.turn.phase == Phase.Chronicle ||
            viewer.exists(player => ready.game.current.players
              .exists(seat => seat.player == player &&
                seat.lineage == lineage)))((owner, keeper) =>
          viewer.contains(keeper) && keepPending(ready, owner))
```

with, in the same class:

```scala
  /** The full walker catalog, for reading Setup's open decisions. */
  private lazy val setupPowers: WalkerPowers =
    WalkerPowers.selected(WalkerPowerCatalog.default(catalog), Vector.empty)

  /** Whether Setup's walk is parked on `owner`'s keep (R16): the tree is
    * rebuilt from the state and its open decisions read, as the decision
    * projection reads them. Asked only for a card in a matchmade draw. */
  private def keepPending(ready: ReadyGame, owner: PlayerId): Boolean =
    val current = ready.game.current
    (for
      pending <- current.walkerPending
      if current.walkerProcedure.contains(TriggeredProcedureRef.Setup)
      tree <- SetupProcedure.build(catalog, ready, current.walkActor,
        current.walkerStartArgs).toOption
    yield ProcedureWalker.openDecisions(ready, tree, pending, setupPowers,
      probing = false).exists(_.decisionId == LegacyKeep.decisionId(owner)))
      .getOrElse(false)
```

Check during execution how `WalkerDecisionProjector.rebuild` builds Setup's
tree (it may go through `WalkerProcedureRegistry`); build it the same way.
If the parked state does not carry what the rebuild needs, stop and report
it (R16). Add the Matchmaker sentence to the scaladoc's legacy bullet.
Import `distantlands.gameplay.setup.{LegacyKeep, SetupProcedure}`,
`distantlands.gameplay.powers.WalkerPowerCatalog` and
`distantlands.gameplay.walker.{ProcedureWalker, WalkerPowers}`. Check the legacy option rows in
`DecisionOptionRows` read `identifiesCard` for the lineage container (the
keep's options must project for the holder, as the projection test
checks); if they read a dormant legacy some other way, route that read
through `identifiesCard` too.

`LegacySetupPowers.all` appends `Matchmaker`; `noteKeys` appends
`Matchmaker.setupNoteKeys`. Change the four tests' imports of
`MatchmakerCard` to `distantlands.gameplay.setup.legacies.MatchmakerCard`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.chronicle.* distantlands.application.* distantlands.catalog.*"`
Expected: every test passes; `LegacySetupSuite`, the projection suites and
every golden are unchanged.

- [ ] **Step 5: Regenerate the snapshots and commit**

Expected diff: `procedure.setup matchmaker-kept-for` added,
`legacy.matchmaker` removed.

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/application/MatchmakerProjectionSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/LegacyGoalsSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/legacies/SteadfastSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/legacies/IronHandSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Matchmaker keeps a legacy for every other player

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 6: The golden logs and the docs

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala` (a `later-game-setup-foundations` script)
- Modify: `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala` (an `explore-world-crafter` script) and the registry that lists Explore's scripts (`GoldenScripts` or `ExploreLogScripts.named`)
- Create: `src/test/resources/gamelog/later-game-setup-foundations.*`, `src/test/resources/gamelog/explore-world-crafter.*` (generated, reviewed)
- Modify: `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `docs/rules/rules-reference.md`, `docs/ROADMAP.md`, `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md` (Status), `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md` (confirm R11-R16 as ruled)

- [ ] **Step 1: Add the Setup golden script**

In `LaterGameLogScripts`, beside `laterGameSetupLegacies`:

```scala
  /** As [[laterGameWoken]], with the foundations' legacy powers: l3 an
    * Exile holding World Crafter and Matchmaker active, l2 a Citizen
    * holding Populist active (Legacy Setup powers, slice 2). World Crafter
    * fills slot 2 from Forgotten Sites and every other slot from Recent
    * Sites, Populist divides, and Matchmaker keeps each draw's first
    * legacy for p1 and p2. */
  def laterGameSetupFoundations(using munit.Location): Script =
    laterGameWokenFrom("later-game-setup-foundations", chronicle =>
      SetupLegacyFixture.holding(SetupLegacyFixture.holding(chronicle,
        LineageId("l3"), Role.Exile, WorldCrafterCard.id -> true,
        MatchmakerCard.id -> true),
        LineageId("l2"), Role.Citizen, PopulistCard.id -> true), {
      case park if park.decisionId ==
          WorldCrafter.slotDecisionId(PlayerId("p3"), 2) =>
        ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))
      case park if park.decisionId == Populist.decisionId(PlayerId("p2")) =>
        ChooseOneAnswer(Populist.divide)
    })
```

and `"later-game-setup-foundations" -> (() => laterGameSetupFoundations)`
in `named`.

- [ ] **Step 2: Add the Explore golden script**

In `ExploreLogScripts`:

```scala
  /** p1 explores while p2 holds World Crafter: p2 chooses the Forgotten
    * end on p1's turn and p1 finds Golden Valley (Legacy Setup powers
    * ruling 11). */
  def exploreWorldCrafter(using munit.Location): Script =
    Script("explore-world-crafter", exploring("explore-world-crafter",
      p1AtHintermostOfThree.legacy(Table.p2, WorldCrafterCard),
      AtlasEnd.Forgotten)._1, p1)
```

registered where `explore` is. `exploring`'s answer for `explore.end` is
keyed on the decision id, so it answers p2's park too.

- [ ] **Step 3: Generate and review the goldens**

```bash
GAMELOG_GOLDEN=write ./sbtw 'testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *later-game-setup-foundations* *explore-world-crafter*'
git status --short src/test/resources
```

Expected: only the four new files. Review each line by line:
- `later-game-setup-foundations`: the Reliquary pick; six World Crafter
  lines, slot 2 from Forgotten Sites; no "filled the map from" and no
  "placed ... on the map" line; Populist's line; then each player's pawn,
  "p3 kept a legacy for p1 with Matchmaker" and "p3 kept a legacy for p2
  with Matchmaker", and "p3 kept a legacy". No line names a legacy drawn
  for another player, in any seat's log. If a detail line names one the
  holder saw while choosing, in the holder's log or the owner's, stop and
  report it: R16 gives the holder no view outside the pending keep.
- `explore-world-crafter`: "p2 chose Forgotten Sites with World Crafter"
  before "Explored Forgotten Site: Found Golden Valley", both in p1's turn.

- [ ] **Step 4: Docs**

- `docs/rules/ambiguities.md`, "Legacy Setup powers rulings": items 20 to
  29 (the design's rulings 10 to 13, then R11 to R16) were recorded with
  the rulings; check they still match what landed.
- `docs/rules/implementation-traceability.md`: extend the "Legacy SETUP
  powers at game start" row, or add a "Legacy SETUP powers in the
  foundations" row after it, for World Crafter (`WorldCrafterSetupSuite`,
  `WorldCrafterExploreSuite`, `WorldCrafterExploreProjectionSuite`, the
  `explore-world-crafter` golden), Populist (`PopulistSuite`,
  `SetupWindowsSuite`) and Matchmaker (`MatchmakerSuite`,
  `MatchmakerProjectionSuite`), with the `later-game-setup-foundations`
  golden. The Explore row and the "Six foundation setup effects" row name
  World Crafter and Populist in their notes.
- `docs/rules/rules-reference.md`, later games step 5: a "Digital:" note
  for World Crafter (Foundation I), Populist (Foundation II) and Matchmaker
  (Foundation III).
- `docs/ROADMAP.md`, "Legacy powers: setup": drop the clause that says the
  next game restores Iron Hand's stored sites as the Empire's until its
  SETUP half lands (slice 1 landed it), and drop World Crafter and
  Matchmaker's Setup deal from the list. The "in play" item names
  Matchmaker's draws during a game (Amber Doors, Lost Doors, Ancient Writ).
  The setup item stays unticked until slice 4.
- The spec's Status: slices 1 and 2 have landed, with slice 2's commit
  range.

- [ ] **Step 5: Run the gate and commit**

Run the gate (top of this plan). Then:

```bash
git add src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala \
  src/test/resources/gamelog/later-game-setup-foundations.actor.log \
  src/test/resources/gamelog/later-game-setup-foundations.other.log \
  src/test/resources/gamelog/explore-world-crafter.actor.log \
  src/test/resources/gamelog/explore-world-crafter.other.log \
  docs/rules docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md
git commit -m "test(legacies): goldens for the foundations' legacy powers, and their docs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Add the scripts registry file to the pathspec if Step 2 changed one.

## Self-review notes

- Spec rulings 1-4: `SetupLegacyRules` (seated, active, read live), the
  decision ids (`setup.legacy.world-crafter.<player>.<n>`,
  `setup.legacy.populist.<player>`; Matchmaker and Explore keep theirs),
  Decline first on Populist. Ruling 10: Task 1. Ruling 11: Task 2. Ruling
  12: Task 3. Ruling 13: Tasks 4 and 5. The slice 2 test list: World
  Crafter mixing slot by slot as an Exile (Task 1), choosing another
  player's Explore end (Task 2), Populist under both faces and declining
  (Task 3), Matchmaker keeping for two players with the owner's and a third
  viewer's projections and a one-legacy draw (Task 5); a resume test for
  each new decision (Tasks 1, 2, 3, 5); goldens (Task 6).
- The `SetupBanks` window is listed in `SetupWindowsSuite`, the Setup
  counterpart of `ChronicleWindowsSuite`.
- Shapes fixed from game start: World Crafter's slots and questions
  (`emptySlotsAtStart`, `atlasAtStart`); Populist's and Matchmaker's
  holders cannot change in Setup; Explore's end question already reads the
  live Atlas at its own branch.
- Names used across tasks: `SetupLegacyRules.mapFiller`,
  `.exploreEndChooser`, `.populist`, `.keeper`; `WorldCrafter.slotDecisionId`,
  `.mapStep`, `.setupNoteKeys`, `.exploreNoteKeys`, `.chose`;
  `Populist.decisionId`, `.decline`, `.divide`, `.shares`, `.moves`,
  `.setupNoteKeys`; `PowerWindow.SetupBanks`; `LegacyKeep.decisionId`,
  `.keptLegacy`, `.drawn`, `.step`, `.noteKeys`, `.matchmade`;
  `Matchmaker.keptFor`, `.setupNoteKeys`.
