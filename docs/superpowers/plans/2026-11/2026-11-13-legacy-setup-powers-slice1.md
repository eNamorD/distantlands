# Legacy Setup Powers, Slice 1: Seam and Game Start Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The four legacy SETUP powers with no choice act at game start.
Circle of Swords gives its holder 8 board warbands. Scepter Bearer gives an
Imperial holder the Grand Scepter. Iron Hand puts its holder's warbands on
the Empire's sites and lets every player start at any site. Rival fills
the three Cradlemost empty slots with its Exile holder's sites. Setup's
walk logs each of them first.

**Architecture:** Game start cannot ask questions, and it builds the
boards and the map in `FirstGameSetupMaterializer` from a
`FirstGameSetupPlan`. A new `StartingLegacies` (in `gameplay/setup`)
reads the Chronicle's stored lineages and the seats and works out what
these four powers change, as a `StartLegacies` value the plan carries.
The materializer and `LaterGameSetup` apply it. The walker's steps that
read the result later (`SetupPawnSites`, `ImperialMaps`) ask
`ChronicleLegacyRules.holder`, or `StartingLegacies` again from the
Chronicle, so no step's shape depends on state the walk changes. Each
card sits beside its power in a new package, `gameplay/setup/legacies`,
registered on the walker by `LegacySetupPowers.all`.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Setup powers design](../../specs/2026-11/2026-11-13-legacy-setup-powers-design.md),
rulings 1-9 and section "Slice 1: seam and game start".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md).
Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Circle of Swords L06, Scepter Bearer L09) and
`src/main/scala/distantlands/gameplay/chronicle/legacies/` (Iron Hand L01,
Rival L17). The rulebook's Setup for Later Games, CR pp. 8-13
(`docs/rules/rules-reference.md#later-games`), and the
[Empire rulings](../../../rules/ambiguities.md#empire-rulings) R25-R30.

## Prerequisite

The Legacy Chronicle powers batch has landed. Check before Task 1:

```bash
grep -q "def ironHand(state: ReadyGame)" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "object RivalCard" src/main/scala/distantlands/gameplay/chronicle/legacies/Rival.scala &&
grep -q "object CircleOfSwordsCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
echo ready
```

Expected: `ready`.

## Rulings

This plan's own choices are implementation rulings R1-R8
([rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md)):

- **R1** (Task 1): game start reads a holder from the Chronicle: a seated
  lineage whose stored record holds the legacy active. Its role is the
  stored role: Chancellor and Citizen are Imperial, and anything else,
  including a lineage the Chronicle does not list, is an Exile. The walk
  reads the holder with `ChronicleLegacyRules.holder` as the Chronicle
  powers do.
- **R2** (Tasks 2, 4 and 5): an Exile's 14 warbands go to their board
  first (3, or Circle of Swords' 8), then to Iron Hand's Empire sites,
  Cradlemost first, then to Rival's sites. A site that the holder's supply
  misses takes bandits equal to its capacity, as R30 does for the purple
  supply.
- **R3** (Task 2): an Imperial Circle of Swords holder takes
  `min(8, 24 - 3 × other Imperial boards)` purple warbands. The Empire's
  sites share what is left, Cradlemost first, and a site the supply misses
  takes bandits (R30, unchanged).
- **R4** (Task 4): only an Exile's Iron Hand acts at setup
  (`ChronicleLegacyRules.ironHand`). An Imperial holder would have
  discarded it in the Chronicle (Chronicle powers ruling 18), and its
  discard during a game is out of scope.
- **R5** (Task 5): Rival's sites take no relics from the relic deck (the
  Empire rules: "Do not fill empty relic slots"). Rival's holder may also
  place their pawn at any site their own warbands hold.
- **R6** (Task 5): Imperial Maps counts its empty slots from what game
  start fixed: `MapState.slots`, less the Empire's stored sites, less the
  sites Rival places, worked out again from the Chronicle and the seats.
  It never reads the live map, so a resumed walk keeps its shape.
- **R7** (Task 6): one line per power, written by Notes at the very start
  of Setup, before the Public Ambitions line: "p2 started with 8 warbands
  with Circle of Swords", "p1 started with the Grand Scepter with Scepter
  Bearer", and one line per site, "p3 placed a warband at Deep Woods with
  Iron Hand" and "p3 placed a warband at Deep Woods with Rival". Each Note
  is always in the tree and writes nothing when its power does not act.
- **R8** (Task 1): the game-start legacy pass lives in the plan and the
  materializer, not in `LaterGameSetup` alone as the spec's Approach
  says: the boards and the map are built there. Only Scepter Bearer is
  applied in `LaterGameSetup`, which hands out the Grand Scepter.

## Deviations from the spec

- R8 above.
- Iron Hand and Rival already left holding with the Chronicle batch; their
  power objects stay in `gameplay/chronicle/legacies` and only their doc
  comments change. The spec's Approach says Iron Hand leaves holding now;
  Task 6 corrects the spec.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`.
- A new test names cards by their card objects (`CircleOfSwordsCard`).
- Only seated players' active legacies act, and a first game asks nothing
  of any (spec rulings 1 and 2).
- Every walker step reads what game start fixed, never the live map, to
  choose its shape (the resume bug class of Legacies slice 3).
- Nothing in this slice is hidden: warbands, the Grand Scepter and sites
  are public.
- Old saved streams still decode: `FirstGameSetupPlan` is built at game
  start and never stored, and no wire format changes.
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

## The gate

Run after the last task, and before any task whose commit touches more
than its own suite:

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

### Task 1: `StartLegacies` and `StartingLegacies.holder`

**Files:**
- Modify: `src/main/scala/distantlands/model/Setup.scala` (add
  `StartLegacies`; `FirstGameSetupPlan` gains `legacies`)
- Create: `src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala`
- Create: `src/test/scala/distantlands/gameplay/setup/StartingLegaciesSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/setup/SetupLegacyFixture.scala`

**Interfaces:**
- Produces:
  - `final case class StartLegacies(boardWarbands: Map[PlayerId, Int], ironHand: Option[LineageId], rival: Option[LineageId], rivalSites: Vector[StoredSite], scepterBearer: Option[PlayerId])` in `model`, with `StartLegacies.none`.
  - `FirstGameSetupPlan.legacies: StartLegacies = StartLegacies.none` (last field).
  - `StartingLegacies.holder(chronicle: Chronicle, seated: Vector[LineageId], legacy: LegacyId, roles: Set[Role] = Role.values.toSet): Option[LineageId]`.
  - `StartingLegacies.imperialRoles: Set[Role]` and `StartingLegacies.exileRoles: Set[Role]`.
  - `SetupLegacyFixture.holding(chronicle: Chronicle, lineage: LineageId, role: Role, legacies: (LegacyId, Boolean)*): Chronicle`.

- [ ] **Step 1: Write the fixture and the failing test**

`SetupLegacyFixture.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.model._

/** A later game's Chronicle in which a lineage holds legacies, for the
  * legacies' SETUP powers. */
object SetupLegacyFixture:
  /** `chronicle` with `lineage` stored as `role`, holding `legacies`
    * (id, active), added when the Chronicle does not list it. */
  def holding(chronicle: Chronicle, lineage: LineageId, role: Role,
      legacies: (LegacyId, Boolean)*): Chronicle =
    val held = legacies.toVector.map((id, active) => LegacyState(id, active))
    val record = chronicle.lineages.find(_.id == lineage).fold(
      LineageState(lineage, None, role, held, Vector.empty))(
      _.copy(role = role, legacies = held))
    chronicle.copy(lineages =
      chronicle.lineages.filterNot(_.id == lineage) :+ record)
```

`StartingLegaciesSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.holding.CircleOfSwordsCard
import distantlands.model._

class StartingLegaciesSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l3 = LineageId("l3")
  private val seated = Vector(l1, LineageId("l2"), l3)
  private val base = LaterGameFixture.chronicle(l1, LineageId("l2"))

  test("a seated Exile holding the legacy active is its holder (ruling 1)"):
    val chronicle = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(chronicle, seated,
      CircleOfSwordsCard.id), Some(l3))

  test("a dormant legacy and a boxed lineage hold nothing (ruling 1)"):
    val dormant = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> false)
    assertEquals(StartingLegacies.holder(dormant, seated,
      CircleOfSwordsCard.id), None)
    val active = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(active, seated.take(2),
      CircleOfSwordsCard.id), None)

  test("the stored role decides a role condition (R1)"):
    val citizen = SetupLegacyFixture.holding(base, LineageId("l2"),
      Role.Citizen, CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(citizen, seated,
      CircleOfSwordsCard.id, StartingLegacies.imperialRoles),
      Some(LineageId("l2")))
    assertEquals(StartingLegacies.holder(citizen, seated,
      CircleOfSwordsCard.id, StartingLegacies.exileRoles), None)
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.StartingLegaciesSuite"`
Expected: compile error, `StartingLegacies` is not found.

- [ ] **Step 3: Implement**

In `src/main/scala/distantlands/model/Setup.scala`, after `EmpireSetup`:

```scala
/** What the legacies' SETUP powers change at game start (Legacy Setup
  * powers design, slice 1), worked out from the Chronicle and the seats by
  * `StartingLegacies`. `boardWarbands` holds only the boards that start
  * with other than 3; `ironHand` and `rival` are the holders' lineages;
  * `rivalSites` are the Atlas sites Rival places, Cradlemost first. */
final case class StartLegacies(
    boardWarbands: Map[PlayerId, Int],
    ironHand: Option[LineageId],
    rival: Option[LineageId],
    rivalSites: Vector[StoredSite],
    scepterBearer: Option[PlayerId])

object StartLegacies:
  val none: StartLegacies =
    StartLegacies(Map.empty, None, None, Vector.empty, None)
```

Add the last field of `FirstGameSetupPlan`:

```scala
    /** What the legacies' SETUP powers change (`StartingLegacies`). */
    legacies: StartLegacies = StartLegacies.none
```

`src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.model._

/** The legacies' SETUP powers that change what game start builds (Legacy
  * Setup powers design, slice 1). Game start asks no question, so these
  * read the Chronicle's stored lineages and the seats, never a walk. A
  * later walker step that needs the same answer asks again from the
  * Chronicle, which keeps its shape fixed on resume. */
object StartingLegacies:
  val imperialRoles: Set[Role] = Role.values.filter(_.isImperial).toSet
  val exileRoles: Set[Role] = Set(Role.Exile)

  /** The seated lineage whose stored record holds `legacy` active, if its
    * stored role is one of `roles`. A lineage the Chronicle does not list
    * holds nothing (R1). */
  def holder(chronicle: Chronicle, seated: Vector[LineageId],
      legacy: LegacyId, roles: Set[Role] = Role.values.toSet)
      : Option[LineageId] =
    chronicle.lineages.find(lineage => seated.contains(lineage.id) &&
      roles(lineage.role) &&
      lineage.legacies.contains(LegacyState(legacy, active = true)))
      .map(_.id)
```

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.StartingLegaciesSuite"`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/Setup.scala \
  src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala \
  src/test/scala/distantlands/gameplay/setup/StartingLegaciesSuite.scala \
  src/test/scala/distantlands/gameplay/setup/SetupLegacyFixture.scala
git commit -m "feat(setup): read a legacy's holder from the Chronicle at game start

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 2: Circle of Swords

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/CircleOfSwords.scala`
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `CircleOfSwordsCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import `gameplay.setup.legacies._`)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` (append `LegacySetupPowers.all`)
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala` (`of`)
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` (`buildPlan` sets `legacies`)
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` (board warbands, purple count)
- Modify: tests importing `distantlands.catalog.holding.CircleOfSwordsCard` (`StartingLegaciesSuite`, `LegacyCardDataSuite` if it imports by name)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/CircleOfSwordsSuite.scala`

**Interfaces:**
- Consumes: `StartingLegacies.holder`, `StartLegacies`, `SetupLegacyFixture.holding` (Task 1).
- Produces:
  - `CircleOfSwordsCard` and `case object CircleOfSwords extends ContributingPower` in `gameplay.setup.legacies`; `CircleOfSwords.warbands: Int = 8`.
  - `LegacySetupPowers.all: Vector[ContributingPower]` and `LegacySetupPowers.noteKeys: Vector[NoteKey]` (empty until Task 6).
  - `StartingLegacies.of(chronicle: Chronicle, seats: Vector[FirstGameParticipant], mode: SetupMode): StartLegacies`, filling `boardWarbands` here and the other fields in Tasks 3-5.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture, SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Circle of Swords at game start: "You gain eight starting warbands
  * (instead of three). If you're Imperial and other Imperials would gain
  * fewer than three, you gain less." (spec ruling 5; R2, R3). */
class CircleOfSwordsSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  private def board(ready: ReadyGame, player: String): Int =
    ready.game.current.player(PlayerId(player)).get.board.warbands

  test("an Exile holder starts with 8 warbands, the others with 3"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> true))
    assertEquals(board(ready, "p3"), CircleOfSwords.warbands)
    assertEquals(board(ready, "p1"), 3)
    assertEquals(board(ready, "p2"), 3)

  test("a dormant Circle of Swords changes nothing (ruling 1)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> false))
    assertEquals(board(ready, "p3"), 3)

  test("an Imperial holder takes 8 purple and the Empire's sites share " +
      "the rest, Cradlemost first (R3)"):
    // Three Imperial boards: 3 + 3 + 8 = 14 of the 24 purple warbands,
    // so an Empire of 8 sites has 10 left: every site takes one.
    val chronicle = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector(l2, l3), 8), l3, Role.Citizen,
      CircleOfSwordsCard.id -> true)
    val ready = started(chronicle)
    assertEquals(board(ready, "p3"), 8)
    val purple = ready.game.current.map.sites.values
      .count(_.forces == SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(purple, 8)

  test("an Imperial holder takes less when the others would get fewer " +
      "than three (R3)"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2, l3), 2)
    val ready = started(SetupLegacyFixture.holding(record, l1,
      Role.Chancellor, CircleOfSwordsCard.id -> true))
    assertEquals(board(ready, "p1"), 8)
    assertEquals(StartingLegaciesProbe.imperialShare(others = 7), 3)

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(CircleOfSwords))
    assert(distantlands.gameplay.powers.PowerImplementationStatus
      .implemented(CircleOfSwordsCard.power.id))
```

The last-but-one test's probe checks the cap formula directly; add it to
the same file, beside the suite:

```scala
/** The cap of R3 for a count of other Imperial boards. */
private object StartingLegaciesProbe:
  def imperialShare(others: Int): Int =
    distantlands.gameplay.setup.StartingLegacies.imperialBoard(others)
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.CircleOfSwordsSuite"`
Expected: compile error, `CircleOfSwords` is not found.

- [ ] **Step 3: Implement**

Move `CircleOfSwordsCard` from `HoldingLegacies.scala` into
`gameplay/setup/legacies/CircleOfSwords.scala`, unchanged, with its power:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object CircleOfSwordsCard extends Legacy(LegacyId("L06"), "Circle of Swords",
    LegacyGoal.MostSites, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.circle-of-swords"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** You gain eight starting warbands _(instead of " +
      "three)_. If you're Imperial and other Imperials would gain " +
      "fewer than three, you gain less.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Circle of Swords (legacy L06), its SETUP power (Legacy Setup powers
  * design, ruling 5). Game start reads it (`StartingLegacies.of`): the
  * holder's board starts with 8 warbands, an Imperial's from the purple
  * supply after every other Imperial board's 3 (R3). This power hooks no
  * window; it is registered so the card counts as implemented. */
case object CircleOfSwords extends ContributingPower:
  val id: PowerId = CircleOfSwordsCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  val warbands: Int = 8
```

`gameplay/setup/legacies/LegacySetupPowers.scala`:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.model.NoteKey

/** The legacies whose SETUP powers are implemented (Legacy Setup powers
  * design), registered on the walker by `WalkerPowerCatalog`. */
object LegacySetupPowers:
  val all: Vector[ContributingPower] = Vector(CircleOfSwords)
  /** The lines the powers write in Setup, on Setup's note id, which
    * `SetupProcedure.noteKeys` declares. */
  def noteKeys: Vector[NoteKey] = Vector.empty
```

In `NewFoundations.scala` add `import distantlands.gameplay.setup.legacies._`.
In `WalkerPowerCatalog.contributing` append `++ LegacySetupPowers.all`
after `LegacyChroniclePowers.all`, and add the sentence "The legacies'
SETUP powers (`LegacySetupPowers`) hook no window: game start and Setup's
steps read them." to its doc comment.

In `StartingLegacies`:

```scala
  /** The purple warbands an Imperial Circle of Swords holder takes when
    * `others` other Imperial boards each take 3 first (R3). */
  def imperialBoard(others: Int): Int =
    math.max(0, math.min(CircleOfSwords.warbands,
      MaterialBankState.ImperialWarbands - 3 * others))

  /** What the SETUP powers change for `chronicle` seated as `seats` in
    * `mode`. A first game's legacies are all dormant, so nothing acts. */
  def of(chronicle: Chronicle, seats: Vector[FirstGameParticipant],
      mode: SetupMode): StartLegacies =
    val seated = seats.map(_.lineageId)
    val playerOf = seats.map(seat => seat.lineageId -> seat.playerId).toMap
    val imperial = mode match
      case SetupMode.WithEmpire(empire) => empire.imperial
      case _ => Set.empty[PlayerId]
    val circle = holder(chronicle, seated, CircleOfSwordsCard.id)
      .map(playerOf).map(player => player ->
        (if imperial(player) then imperialBoard(imperial.size - 1)
        else CircleOfSwords.warbands))
    StartLegacies.none.copy(boardWarbands = circle.toMap)
```

Import `distantlands.gameplay.setup.legacies.CircleOfSwords` and
`CircleOfSwordsCard`.

In `GameStartRules.buildPlan`, pass `legacies = StartingLegacies.of(
chronicle, orders.participants, mode)` as the plan's last argument.

In `FirstGameSetupMaterializer.materialize`:

```scala
    val boards = plan.participants.map(p => p.playerId ->
      plan.legacies.boardWarbands.getOrElse(p.playerId, 3)).toMap
    val purpleBoards = plan.empire.fold(0)(_.imperial.toVector
      .map(boards).sum)
    val purple = plan.empire.fold(Set.empty[SiteId])(empire =>
      empire.sites.take(math.max(0,
        MaterialBankState.ImperialWarbands - purpleBoards))
        .map(_.site).toSet)
```

and build each board with `boards(participant.playerId)` in place of `3`.
Update the `purple` doc in `empireSite` to "after the Imperial boards".

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.CircleOfSwordsSuite distantlands.gameplay.setup.* distantlands.catalog.*"`
Expected: every test passes, `CardCatalogSuite` and `LegacyCardDataSuite`
included.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/distantlands/gameplay/setup src/test/scala/distantlands/catalog
git commit -m "feat(legacies): Circle of Swords starts its holder with eight warbands

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 3: Scepter Bearer

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/ScepterBearer.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `ScepterBearerCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala` (add `ScepterBearer`)
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala` (`scepterBearer`)
- Modify: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala` (`imperial` hands the Grand Scepter to `plan.legacies.scepterBearer`)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/ScepterBearerSuite.scala`

**Interfaces:**
- Consumes: `StartingLegacies.of`, `StartingLegacies.imperialRoles` (Tasks 1-2).
- Produces: `ScepterBearerCard`, `case object ScepterBearer extends ContributingPower`; `StartLegacies.scepterBearer` filled.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Scepter Bearer: "If you are an Imperial: Setup: You start with the
  * Grand Scepter (instead of the Chancellor)." (spec ruling 6). */
class ScepterBearerSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  private def scepter(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.filter(_.relics.exists(
      _.id == TheGrandScepterCard.id)).map(_.player)

  test("a Citizen holder starts with the Grand Scepter, the Chancellor " +
      "keeps the title"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(PlayerId("p2")))
    assertEquals(ready.game.current.title.holder, Some(p1))

  test("the Chancellor holding it changes nothing"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l1, Role.Chancellor,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(p1))

  test("an Exile holder changes nothing (ruling 2)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      ScepterBearerCard.id -> true))
    assertEquals(scepter(ready), Vector(p1))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(ScepterBearer))
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.ScepterBearerSuite"`
Expected: compile error, `ScepterBearerCard` is not found in
`gameplay.setup.legacies`.

- [ ] **Step 3: Implement**

Move `ScepterBearerCard` into `ScepterBearer.scala`, unchanged, with:

```scala
/** Scepter Bearer (legacy L09), its SETUP power (Legacy Setup powers
  * design, ruling 6). An Imperial holder starts with the Grand Scepter in
  * place of the Chancellor (`LaterGameSetup`); the Chancellor keeps the
  * title and the Reliquary pick. With no Empire the Grand Scepter stays in
  * the box. This power hooks no window. */
case object ScepterBearer extends ContributingPower:
  val id: PowerId = ScepterBearerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

Add `ScepterBearer` to `LegacySetupPowers.all`. In `StartingLegacies.of`:

```scala
    val bearer = holder(chronicle, seated, ScepterBearerCard.id,
      imperialRoles).map(playerOf).filter(imperial)
    StartLegacies.none.copy(boardWarbands = circle.toMap,
      scepterBearer = bearer)
```

The `filter(imperial)` keeps a stored Citizen whose Empire was not set up
(Setup With No Empire) from acting. In `LaterGameSetup.imperial`, give the
scepter to `plan.legacies.scepterBearer.getOrElse(chancellor)`:

```scala
    val bearer = plan.legacies.scepterBearer.getOrElse(chancellor)
    ...
        players = current.players.map(state =>
          if state.player == bearer then
            state.copy(relics = state.relics :+ scepter)
          else state),
```

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.* distantlands.gameplay.setup.LaterGameSetupSuite distantlands.catalog.*"`
Expected: every test passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/setup
git commit -m "feat(legacies): Scepter Bearer starts an Imperial holder with the Grand Scepter

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 4: Iron Hand's SETUP half

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala` (`ironHand`; `exileSupply`)
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` (`empireSite`, `atlasSite` forces)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala` (every site under Iron Hand)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala` (doc comment)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/IronHandSetupSuite.scala`

**Interfaces:**
- Consumes: `StartingLegacies.of`, `boards` in the materializer (Task 2), `ChronicleLegacyRules.ironHand(state: ReadyGame): Option[PlayerId]`.
- Produces:
  - `StartLegacies.ironHand` filled with the Exile holder's lineage.
  - `StartingLegacies.exileSupply(board: Int): Iterator[Boolean]`: one entry per site in Iron Hand's then Rival's order, true while the holder's 14 warbands last after the `board` their board took (R2).

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture, SetupPawnSites}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Iron Hand at setup: "you place one warband on all sites coming from the
  * Atlas until the Empire divider, and players may place their pawn at any
  * site." It always applies (spec ruling 7; R2, R4). */
class IronHandSetupSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  private def holding(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.holding(chronicle, l3, Role.Exile,
      IronHandCard.id -> true)

  test("the Empire's sites take the holder's warband, not purple"):
    val record = holding(LaterGameFixture.chronicle(l1, l2))
    val ready = started(record)
    val empire = record.world.map(_.site)
    empire.foreach(site => assertEquals(
      ready.game.current.map.sites(site).forces,
      SiteForces.Occupied(ForceKind.Exile(l3), 1)))
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Imperial) > 0), 0)

  test("every player may place their pawn at any site"):
    val ready = started(holding(LaterGameFixture.chronicle(l1, l2)))
    Vector("p1", "p2", "p3").foreach(player => assertEquals(
      SetupPawnSites.legal(catalog, ready, PlayerId(player)).toSet,
      ready.game.current.map.inPlay.toSet))

  test("with no Empire the former Empire's sites take the holder's " +
      "warband in place of bandits"):
    val base = LaterGameFixture.chronicle(l1, l2)
    // No Chancellor and no Citizen seated: only p3 and p4 on l3 and l4.
    val seats = LaterGameFixture.seats(4).drop(2)
    val record = holding(base)
    val ready = GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p3"), seats)).fold(
      problem => fail(problem.toString), identity)
    record.world.map(_.site).foreach(site => assertEquals(
      ready.game.current.map.sites(site).forces,
      SiteForces.Occupied(ForceKind.Exile(l3), 1)))

  test("a dormant Iron Hand changes nothing (ruling 1)"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> false)
    val ready = started(record)
    record.world.map(_.site).foreach(site => assertEquals(
      ready.game.current.map.sites(site).forces,
      SiteForces.Occupied(ForceKind.Imperial, 1)))
```

Also add the R2 shortfall test, which needs an Empire of 8 sites and
Circle of Swords on the same holder (8 + 8 = 16 > 14):

```scala
  test("a site the holder's supply misses takes bandits (R2)"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector.empty, 8), l3, Role.Exile,
      IronHandCard.id -> true, CircleOfSwordsCard.id -> true)
    val ready = started(record)
    val forces = record.world.map(site =>
      ready.game.current.map.sites(site.site).forces)
    assertEquals(forces.take(6).distinct,
      Vector(SiteForces.Occupied(ForceKind.Exile(l3), 1)))
    // A capacity-0 site takes no bandits and stays empty.
    assert(forces.drop(6).forall(_.count(ForceKind.Exile(l3)) == 0), forces)
```

The fixture's eight Atlas sites all move into the Empire with
`chronicle(l1, Vector.empty, 8)`; check the Chronicle validates (an Empire
holds 1 to 8 sites) before relying on it.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.IronHandSetupSuite"`
Expected: the Empire-site tests fail with `Occupied(Imperial,1)` where the
Exile warband is expected; the pawn-site test fails.

- [ ] **Step 3: Implement**

In `StartingLegacies.of` add, before the result:

```scala
    val ironHand = holder(chronicle, seated, IronHandCard.id, exileRoles)
```

and `ironHand = ironHand` on the result. Add:

```scala
  /** One answer per site that `lineage`'s warbands are placed on at game
    * start, Iron Hand's Empire sites first, then Rival's: whether the
    * lineage's 14 warbands still reach it once its board has taken its own
    * (R2). */
  def exileSupply(board: Int): Iterator[Boolean] =
    Iterator.from(1).map(_ <= MaterialBankState.ExileWarbandsPerLineage -
      board)
```

In the materializer, keep one iterator per holder lineage so Iron Hand's
sites draw before Rival's (Task 5 reads the same iterator):

```scala
    val lineageBoards = plan.participants.map(p => p.lineageId ->
      boards(p.playerId)).toMap
    val supply = (plan.legacies.ironHand ++ plan.legacies.rival).distinct
      .map(lineage => lineage -> StartingLegacies.exileSupply(
        lineageBoards.getOrElse(lineage, 3))).toMap
    /** The forces at a site Iron Hand or Rival holds for `lineage`: one
      * of its warbands while its supply lasts, else bandits (R2). */
    def held(lineage: LineageId, site: SiteId): SiteForces =
      if supply(lineage).next() then SiteForces.Occupied(ForceKind.Exile(lineage), 1)
      else bandits(site)
```

Pass `plan.legacies.ironHand` into `empireSite` and `atlasSite` and use
`held(lineage, stored.site)` for their forces when it is set; with Iron
Hand an Empire site takes no purple warband, so build `purple` as the
empty set when `plan.legacies.ironHand` is set. Sites are built in
`plan.orderedSites` order, Cradlemost first, so the iterator meets them
in R2's order; keep `MapState.inSlots(plan.orderedSites.map ...)` strict
(a `Vector`), never a view.

In `SetupPawnSites.legal`, wrap the existing choice of `open`:

```scala
    val open =
      // Iron Hand: "players may place their pawn at any site" (ruling 7).
      if ChronicleLegacyRules.ironHand(ready).nonEmpty then map.inPlay
      else if firstGame then map.inPlay
      else if Imperials.isImperial(ready, player) then empire
      else map.hinterward.filter(site =>
        empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
```

and import `ChronicleLegacyRules` from `gameplay.chronicle`. Update the doc comment: "Under Iron Hand every
player may place at any site (Legacy Setup powers ruling 7)."

In `IronHand.scala`'s doc comment, replace "Its SETUP half and its discard
during a game wait for their batches (R7)." with "Its SETUP half is read
at game start (`StartingLegacies`, Legacy Setup powers ruling 7) and by
`SetupPawnSites`; its discard during a game waits for its batch (R7)."

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.*"`
Expected: every test passes, the Empire, no-Empire and exile-only suites
unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala \
  src/test/scala/distantlands/gameplay/setup
git commit -m "feat(legacies): Iron Hand holds the Empire's sites and frees every pawn at setup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 5: Rival's SETUP half

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingLegacies.scala` (`rivalSites`)
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` (`orderedSites` takes Rival's sites after the Empire's)
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` (`rivalSite`; no relics dealt to Rival's sites)
- Modify: `src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala` (`emptySlots`)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala` (Rival's holder at their own sites)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/Rival.scala` (doc comment)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/RivalSetupSuite.scala`

**Interfaces:**
- Consumes: `exileSupply` and `held` (Task 4); `ChronicleLegacyRules.holder(state, legacy, roles)`.
- Produces:
  - `StartingLegacies.rivalSites(chronicle: Chronicle, seated: Vector[LineageId], placed: Int): (Option[LineageId], Vector[StoredSite])`: the Exile holder and the Atlas's next `min(3, MapState.slots - placed, atlasBox.size)` sites from the Recent end.
  - `StartLegacies.rival`, `StartLegacies.rivalSites` filled.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.{GameStartRules, ImperialMaps,
  LaterGameFixture, SetupLegacyFixture, SetupPawnSites}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** Rival at setup: "If you are an Exile: Setup: Before resolving
  * foundations, fill the three Cradlemost empty site slots using the Empire
  * setup rules, but place one warband of yours on each. You may place your
  * pawn on these sites." (spec ruling 8; R2, R5, R6). */
class RivalSetupSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile, RivalCard.id -> true)
  private val rivalSites = record.atlasBox.take(3)

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  test("the next three Recent Sites follow the Empire's, each with one of " +
      "the holder's warbands and no relic dealt (R5)"):
    val ready = started(record)
    val map = ready.game.current.map
    assertEquals(map.inPlay, (record.world ++ rivalSites).map(_.site))
    rivalSites.foreach { stored =>
      val site = map.sites(stored.site)
      assertEquals(site.forces, SiteForces.Occupied(ForceKind.Exile(l3), 1))
      assertEquals(site.relics.map(_.id),
        stored.items.collect { case id: RelicId => id })
    }
    assert(!ready.game.campaign.atlas.sites.exists(entry =>
      rivalSites.exists(_.site == entry.id)))

  test("Imperial Maps fills only the slots still empty after them (R6)"):
    val ready = started(record)
    assertEquals(ImperialMaps.emptySlotsAtStart(ready), 3)
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p1)).ready
    assertEquals(woken.game.current.map.inPlay.size, MapState.slots)

  test("their denizens add favor to the banks (ruling 8)"):
    val without = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      RivalCard.id -> false))
    val ready = started(record)
    val denizens = rivalSites.flatMap(_.items).count {
      case _: DenizenId | _: EdificeId => true
      case _ => false
    }
    assertEquals(ready.banks.favor.values.sum,
      without.banks.favor.values.sum + denizens)

  test("the holder may place their pawn at their own sites (R5)"):
    val ready = started(record)
    val legal = SetupPawnSites.legal(catalog, ready, PlayerId("p3")).toSet
    assert(rivalSites.map(_.site).toSet.subsetOf(legal), legal)

  test("an Imperial holder places nothing (ruling 8)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
      RivalCard.id -> true))
    assertEquals(ready.game.current.map.inPlay, record.world.map(_.site))
```

Before writing
the favor test, check that Foundation II counts edifices
(`AtlasPlacement.suitOf` does) and the fixture's Rival sites store at
least one card; if none does, store one in the test's Chronicle.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.RivalSetupSuite"`
Expected: compile error, `ImperialMaps.emptySlotsAtStart` is not found.

- [ ] **Step 3: Implement**

In `StartingLegacies`:

```scala
  /** Rival's Exile holder and the sites it places before the foundations:
    * the Atlas's next sites from the Recent end, into the three Cradlemost
    * slots left empty once `placed` sites are on the map (ruling 8). */
  def rivalSites(chronicle: Chronicle, seated: Vector[LineageId],
      placed: Int): (Option[LineageId], Vector[StoredSite]) =
    holder(chronicle, seated, RivalCard.id, exileRoles) match
      case None => None -> Vector.empty
      case some => some -> chronicle.atlasBox.take(math.min(3,
        math.max(0, MapState.slots - placed)))

  /** The sites game start places before Rival in `mode`. */
  def placedBefore(chronicle: Chronicle, mode: SetupMode): Int = mode match
    case SetupMode.FirstGame | SetupMode.ExileOnly(_) => MapState.slots
    case _ => chronicle.world.size
```

`of` sets `rival` and `rivalSites` from
`rivalSites(chronicle, seated, placedBefore(chronicle, mode))`.

In `GameStartRules.buildPlan`, compute `legacies` before the plan and put
`legacies.rivalSites` right after `chronicle.world` in `orderedSites`:

```scala
      legacies = StartingLegacies.of(chronicle, orders.participants, mode)
    yield FirstGameSetupPlan(orders.participants, orders.firstPlayer,
      (chronicle.world ++ legacies.rivalSites ++ atlasSites ++ ...
```

`GameStartRules.evolve` already keeps out of the Atlas every site in
`plan.orderedSites`, so no other change there.

In the materializer:
- `assignRelics`: a Rival site takes 0, as an Empire site does: add
  `plan.legacies.rivalSites.map(_.site)` to its `empire` set (rename the
  set `noDraw`).
- Build a Rival site Empire-style, with the holder's forces:

```scala
  /** A site Rival places (ruling 8): the Empire rules, its edifice intact,
    * its denizens faceup and its stored relics facedown, with one of the
    * holder's warbands while their supply lasts (R2). */
  private def rivalSite(stored: StoredSite, forces: SiteForces): SiteState =
    SiteState(forces, storedDenizens(stored, EdificeSide.Intact),
      faceDown(storedRelics(stored)),
      catalog.site(stored.site).get.startingResources)
```

and look Rival sites up before `atlasSites` in the `orderedSites.map`,
with `held(plan.legacies.rival.get, site)` as their forces.
`favorBanks(plan, map)` counts every denizen on the built map, so Rival's
are counted with no change.

In `ImperialMaps`, replace `emptySlots`:

```scala
  /** The slots past the Empire's and Rival's sites, which the Chancellor
    * fills. Read from what game start fixed, the Chronicle and the seats,
    * never from the live map (R6). */
  def emptySlotsAtStart(ready: ReadyGame): Int =
    val baseline = ready.game.campaign.baseline
    val seated = ready.game.current.players.map(_.lineage)
    val placed = baseline.world.size
    math.max(0, MapState.slots - placed -
      StartingLegacies.rivalSites(baseline, seated, placed)._2.size)
```

and use it in `applies`. `step`'s `Repeat` guard already reads the live
map for its iterations; leave it.

In `SetupPawnSites.legal`, for an Exile holding Rival:

```scala
    val rival = ChronicleLegacyRules.holder(ready, RivalCard.id,
      Set(Role.Exile)).filter(_ == player).flatMap(ready.game.current
      .player).map(_.lineage)
    val own = rival.toVector.flatMap(lineage =>
      map.inPlay.filter(held(ForceKind.Exile(lineage))))
```

and return `(open ++ own).distinct` (keeping `map.inPlay` when both are
empty). Update the doc comment with R5.

In `Rival.scala`'s doc comment, replace "Its SETUP half waits for the
setup batch (R7)." with "Its SETUP half is read at game start
(`StartingLegacies`, Legacy Setup powers ruling 8) and by
`SetupPawnSites`."

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.*Setup*"`
Expected: every test passes; `ImperialMapsSetupSuite` and
`WideHorizonsSetupSuite` unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/gameplay/chronicle/legacies/Rival.scala \
  src/test/scala/distantlands/gameplay/setup
git commit -m "feat(legacies): Rival places three sites for its Exile holder at setup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 6: The lines, the golden log and the docs

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala` (`noteKeys`, `startNotes`)
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/StartLines.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (`tree` starts with `LegacySetupPowers.startNotes`; `noteKeys` adds `LegacySetupPowers.noteKeys`)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/StartLinesSuite.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala` (a `later-game-setup-legacies` script)
- Create: `src/test/resources/gamelog/later-game-setup-legacies.*` (generated, reviewed)
- Modify: `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `docs/rules/rules-reference.md`, `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md`, `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`

**Interfaces:**
- Consumes: every power of Tasks 2-5; `ChronicleLegacyRules.holder`.
- Produces: `LegacySetupPowers.startNotes: Vector[Operation]`, four Notes of constant shape.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** The lines Setup writes first for game start's legacy powers (R7). */
class StartLinesSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def lines(chronicle: Chronicle): Vector[String] =
    val situation = Situation.wake(Situation.rules(catalog), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  test("Circle of Swords and Scepter Bearer open Setup's log"):
    val record = SetupLegacyFixture.holding(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> true), l2, Role.Citizen,
      ScepterBearerCard.id -> true)
    assertEquals(lines(record).take(2), Vector(
      s"p3 started with 8 warbands with ${CircleOfSwordsCard.id.value}",
      s"p2 started with the Grand Scepter with ${ScepterBearerCard.id.value}"))

  test("Iron Hand and Rival write one line per site"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> true, RivalCard.id -> true)
    val said = lines(record)
    record.world.foreach(stored => assert(said.contains(
      s"p3 placed a warband at ${stored.site.value} with " +
        IronHandCard.id.value), said))
    record.atlasBox.take(3).foreach(stored => assert(said.contains(
      s"p3 placed a warband at ${stored.site.value} with " +
        RivalCard.id.value), said))

  test("with no legacy acting Setup's log is unchanged"):
    val record = LaterGameFixture.chronicle(l1, l2)
    assert(!lines(record).exists(_.contains(" with L")))
```

`NoteText` renders a card and a site by their ids, as `OathsSetupSuite`
relies on; if it renders sites by name, compare with
`catalog.site(id).get.name`.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.StartLinesSuite"`
Expected: compile error or no such line.

- [ ] **Step 3: Implement**

`StartLines.scala`:

```scala
package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** The lines that open Setup for what game start's legacy powers did
  * (R7). Each Note is always in the tree, so Setup's shape never depends
  * on them, and writes nothing when its power did not act. They read the
  * table as game start left it: nothing in Setup moves a warband or the
  * Grand Scepter before them. */
object StartLines:
  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  val startedWithWarbands: NoteKey = notes.key("legacy-started-warbands",
    Vector(NotePart.Arg(0), NotePart.Text(" started with "), NotePart.Arg(1),
      NotePart.Text(" "), NotePart.Plural(1, "warband", "warbands"),
      NotePart.Text(" with "), NotePart.Arg(2)))
  val startedWithScepter: NoteKey = notes.key("legacy-started-scepter",
    Vector(NotePart.Arg(0),
      NotePart.Text(" started with the Grand Scepter with "), NotePart.Arg(1)))
  val placedWarband: NoteKey = notes.key("legacy-placed-warband", Vector(
    NotePart.Arg(0), NotePart.Text(" placed a warband at "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  private def note(line: ReadyGame => Option[LogNote]): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      states => line(states.now))

  /** Two Notes for the boards and the Grand Scepter, then one per map slot
    * for Iron Hand and one per slot for Rival: a constant shape, each Note
    * writing nothing when its power did not act there. */
  def operations: Vector[Operation] =
    Vector(note(circle), note(scepter)) ++
      (0 until MapState.slots).map(slot =>
        note(site(IronHandCard.id, world = true, slot))) ++
      (0 until MapState.slots).map(slot =>
        note(site(RivalCard.id, world = false, slot)))

  private def circle(state: ReadyGame): Option[LogNote] =
    ChronicleLegacyRules.holder(state, CircleOfSwordsCard.id)
      .flatMap(player => state.game.current.player(player)).map(held =>
        startedWithWarbands.sourceless(NoteArg.Player(held.player),
          NoteArg.Number(held.board.warbands),
          NoteArg.Card(CircleOfSwordsCard.id)))

  private def scepter(state: ReadyGame): Option[LogNote] =
    ChronicleLegacyRules.holder(state, ScepterBearerCard.id,
      ChronicleLegacyRules.imperialRoles).filter(player =>
      state.game.current.player(player).exists(_.relics.exists(
        _.id == TheGrandScepterCard.id))).map(player =>
      startedWithScepter.sourceless(NoteArg.Player(player),
        NoteArg.Card(ScepterBearerCard.id)))

  /** The line for the site in map slot `slot`, Hinterward, when the
    * holder's warband holds it: the Empire's stored sites for Iron Hand,
    * the others for Rival. */
  private def site(legacy: LegacyId, world: Boolean, slot: Int)(
      state: ReadyGame): Option[LogNote] =
    val stored = state.game.campaign.baseline.world.map(_.site).toSet
    for
      player <- ChronicleLegacyRules.holder(state, legacy, Set(Role.Exile))
      held <- state.game.current.player(player)
      site <- state.game.current.map.inPlay.lift(slot)
      if stored(site) == world && state.game.current.map.sites(site).forces
        .count(ForceKind.Exile(held.lineage)) > 0
    yield placedWarband.sourceless(NoteArg.Player(player),
      NoteArg.Site(site), NoteArg.Card(legacy))
```

The Notes read the table when the walk reaches them, the first thing it
does, so nothing has moved a warband or the Grand Scepter yet, and on
resume they are already in the journal. Their number never changes, so
Setup's shape does not depend on them (the Legacies slice 3 bug class).

`LegacySetupPowers`: `def noteKeys = StartLines.noteKeys` and
`def startNotes: Vector[Operation] = StartLines.operations`.

`SetupProcedure.tree`: `Sequence(LegacySetupPowers.startNotes ++
publicRow(ready) ++ ...`. `SetupProcedure.noteKeys`: `notes.declared ++
ImperialMaps.noteKeys ++ LegacySetupPowers.noteKeys`. Update the tree
sketch in its scaladoc: a first line `Note(the legacies' game-start
lines)`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.gamelog.*"`
Expected: `StartLinesSuite` passes; every existing golden log is
unchanged (no legacy acts in them).

- [ ] **Step 5: Add the golden log**

In `LaterGameLogScripts`, add a `later-game-setup-legacies` script: the
`later-game-setup` Chronicle with l3 an Exile holding Circle of Swords,
Iron Hand and Rival active, and l2 a Citizen holding Scepter Bearer
active, woken through Setup. Generate its golden file the way the
existing goldens are generated (read the suite's header for the
regeneration flag), and review it line by line: the four legacy lines
come first, then the Public Ambitions line if any, the Reliquary pick,
Imperial Maps placing the three slots Rival left, and each player's
Setup lines. Every pawn line names a site the player may use under Iron
Hand.

- [ ] **Step 6: Docs**

- `docs/rules/ambiguities.md`: open a "Legacy Setup powers rulings"
  section with the spec's rulings 1-9, then "Implementation rulings" R1-R8
  of this plan.
- `docs/rules/implementation-traceability.md`: rows for Circle of Swords,
  Scepter Bearer, Iron Hand's SETUP half and Rival's SETUP half, citing
  their suites; the later-game restoration row's dependency note names
  them.
- `docs/rules/rules-reference.md`, Setup for later games steps 2, 3 and 5:
  "Digital:" notes for Circle of Swords and Scepter Bearer (Players),
  Iron Hand (Empire) and Rival (before the foundations).
- The spec's Approach: replace "Iron Hand kept its SETUP half in holding
  and leaves now" with "Iron Hand left holding whole too; only its status
  changes", and note R8 under the game-start bullet.
- The implementation rulings file: confirm R1-R8 are recorded as above.

- [ ] **Step 7: Run the gate and commit**

Run the gate (top of this plan). Then:

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala \
  src/test/resources/gamelog docs/rules \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md
git commit -m "feat(legacies): log game start's legacy powers first in Setup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

## Self-review notes

- Spec rulings 1-2: Task 1 (game start) and `ChronicleLegacyRules`
  (walker). Ruling 5: Task 2. Ruling 6: Task 3. Ruling 7: Task 4. Ruling
  8: Task 5. Ruling 9: Task 6. The slice 1 test list: Tasks 2-6 (the
  boxed-lineage case is `StartingLegaciesSuite`; a first game asks
  nothing because no first-game Chronicle holds a legacy).
- Names used across tasks: `StartLegacies`, `StartingLegacies.holder`,
  `.of`, `.imperialBoard`, `.exileSupply`, `.rivalSites`, `.placedBefore`,
  `ImperialMaps.emptySlotsAtStart`, `LegacySetupPowers.all`,
  `.noteKeys`, `.startNotes`, `StartLines`.
