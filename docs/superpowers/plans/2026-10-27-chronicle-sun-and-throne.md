# Chronicle Sun and Throne Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the winner starts the Chronicle, the Sun task crowns the winner and assigns the tasks, the Throne task shapes the Empire, builds edifices, stocks the Reliquary, scatters relics, scores lineage points and swears an Oath. `ChronicleUpdated` then carries the evolved roles, era scores and Oathkeeper goal.

**Architecture:** Two new triggered procedures, `ChronicleSun` and `ChronicleThrone`, slot between `ChronicleBegin` and `ChronicleFinish` in `ChronicleSequence`. Each rule step is a `Sequence` node carrying its own power window, so legacy powers can later rewrite it. Steps that depend on Legacies or Citizenship are empty windows. Steps change live state through journaled operations: existing card and warband operations, plus six new Chronicle primitives. `ChronicleFinish` folds the campaign-level results (roles, era, goal) into the baseline. Map, deck and Reliquary extraction stay in the Stars slice.

**Tech Stack:** Scala 3.9, munit 1.0.4, sbt via `./sbtw`, Scala.js frontend (unchanged here).

**Spec:** `docs/superpowers/specs/2026-10-27-chronicle-steps-design.md`. Rules: `docs/rules/chronicle.md` (tasks 1 and 2) and `docs/rules/ambiguities.md` "Chronicle rulings" 1-14. Previous plan: `docs/superpowers/plans/2026-10-27-chronicle-steps-foundation.md` (merged at `2bb40e7e`).

**Not in this plan:** World, Beacon, Stars, extraction of map, decks, atlas and Reliquary into the Chronicle, the round trip through `GameStartRules`, Legacies and Foundations behaviour (empty windows only), Citizenship offers (empty window), era end and reset, and Persistence.

## Global Constraints

- Branch and worktree off local `0.2.0-alpha`, never `main`. Merge back into `0.2.0-alpha`.
- Scalac runs with `-Werror`. A new sealed case breaks every exhaustive match; fix each one the compiler names.
- Production Scala files stay at or under 800 lines (`scripts/check-architecture.py`, `BackendArchitectureSuite`). `model` must not import `gameplay`, `application` or `serialization`. `gameplay` must not import `application` or `serialization`. `gameplay/walker/ProcedureWalker.scala` is at 797 lines: add nothing to it.
- Tests run with `oathdigital.notes.strict=true`: a log note key not declared by its writer throws.
- Old journals need not decode. New fields and operations are required on the wire.
- Test helper names state what they build. A doc comment does not excuse a vague name.
- Never `git add -A` (the worktree symlinks show as untracked). Add files by path.
- Commit trailer, exactly: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. A subagent that commits uses this trailer, not its own model's.
- Docs: normal prose. Run `python3 scripts/check-markdown-links.py` after doc edits.
- Rule questions not answered by `docs/rules/chronicle.md` or rulings 1-14 go to the user. Never pick a default.

## Rules this plan implements

The step list (CR pp. 36-37, rulings in brackets):

| Task | Step | Actor | This plan |
|---|---|---|---|
| Sun | 1 Reveal facedown relics held by players | none | real |
| Sun | 2 Flip or discard dormant legacies | none | empty window |
| Sun | 3 Exiles' unmet active legacies go dormant | none | empty window |
| Sun | 4 Discard legacies over three | each player | empty window |
| Sun | 5 Shuffle the legacy deck | none | empty window |
| Sun | 6 Crown Chancellor [10] | none | real |
| Sun | 7 Assign Tasks [1, 2, 3] | winner; double holder | real |
| Throne | 1 Offer Citizenship [5] | Throne holder | empty window |
| Throne | 2 Shape Empire [6, 9, 11, 14] | Throne holder | real |
| Throne | 3 Build Edifice [9, 12] | Throne holder | real |
| Throne | 4 Stock Reliquary | Throne holder | real |
| Throne | 5 Scatter Relics | none | real |
| Throne | 6 Score Lineage Points [13] | winner | real, legacy points stubbed |
| Throne | 7 Swear an Oath | Throne holder | real |

Digital accounting choices made here, inside the rules:

- Crown, when the Empire breaks, first returns the winner's board warbands to their own bank. The winner swaps boards for the Chancellor's, and from then on `PlayerForceKind` counts that board's warbands as Imperial.
- Crown records an Imperial warband supply of one per site in play, only when the game has none. The Chronicle never places more than one Imperial warband on a site, and the CR says not to worry about running out (p. 37).
- Build Edifice buries an edifice to the edifice deck (`Bury`), not to the set-aside list. The step ends by returning the whole set-aside list to the deck (ruling 9: the whole deck is shuffled), so both paths end in the same place. Task 10 updates the rules doc's "Digital:" note to say so.

## File Structure

Paths below are relative to the repo root. `M/` is `src/main/scala/oathdigital/` and `T/` is `src/test/scala/oathdigital/`.

- Modify `M/model/ActionValues.scala`: add `ShufflePile`.
- Modify `M/model/CoreOperations.scala`: change `Shuffle` to take `ShufflePile` and `CardId`; add `Location.SetAsideEdifices`; send `Discard.RuinedEdifice` to the set-aside list.
- Create `M/model/ChronicleOperations.scala`: `ChronicleTask` and the six Chronicle primitives.
- Modify `M/model/GameState.scala`: `CurrentGameState.setAsideEdifices`, `CurrentGameState.chronicleTasks`, `CampaignState.boxRoles`.
- Modify `M/model/ProcedureRef.scala`: `ChronicleSun`, `ChronicleThrone`.
- Modify `M/model/PowerWindow.scala`: fourteen Chronicle windows.
- Modify `M/model/LogNotes.scala`: `NoteUnit.LineagePoint`.
- Modify `M/model/CardIndex.scala`: `CardContainer.SetAsideEdifices`.
- Modify `M/gameplay/operations/`: `PileOperations.scala`, `CardMovementOperations.scala`, `OperationStateWrites.scala`, `OperationStateAdapter.scala`, `CardKnowledgeMoves.scala`, `OperationApplication.scala`. Create `ChronicleStateOperations.scala`.
- Modify `M/gameplay/walker/WalkerShuffles.scala`, `M/gameplay/powers/action/ClayRattle.scala`.
- Create in `M/gameplay/chronicle/`: `ChronicleWindows.scala`, `ChronicleTasks.scala`, `ChronicleSunProcedure.scala`, `ChronicleCrown.scala`, `ChronicleTaskAssignment.scala`, `ChronicleThroneProcedure.scala`, `ShapeEmpire.scala`, `BuildEdifice.scala`, `ReliquaryAndRelics.scala`, `LineageScoring.scala`, `SwearOath.scala`, `ChronicleRecord.scala`.
- Modify `M/gameplay/chronicle/ChronicleSequence.scala`, `ChronicleFinishProcedure.scala`, `M/gameplay/walker/WalkerProcedureRegistry.scala`.
- Modify `M/application/gamelog/ProcedureLog.scala` (and any formatter match the compiler names).
- Modify serialization: `M/serialization/WalkerOperationCodec.scala`, `WalkerValueCodec.scala`, `GameEventJsonSupport.scala` (role codec fix).
- Tests: named per task. Docs: `docs/rules/chronicle.md`, `docs/rules/implementation-traceability.md`, `docs/architecture/game-setup.md`, `CONTEXT.md`, `docs/ROADMAP.md`.

---

### Task 0: Worktree

**Files:** none (git only)

- [ ] **Step 1: Create the worktree off the release branch**

```bash
cd /Users/roman/projects/oathdigital
git worktree add .worktrees/chronicle-sun-throne -b chronicle-sun-throne 0.2.0-alpha
cd .worktrees/chronicle-sun-throne
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
git status --short
```

Expected: clean tree apart from the two symlinks.

- [ ] **Step 2: Confirm the build runs**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleBeginSuite"`
Expected: PASS (5 tests). The first run is slow because of the Scala.js link.

---

### Task 1: `Shuffle` takes any shuffle pile

Build Edifice draws a random edifice per suit, and Scatter Relics shuffles the discarded relics. Both reuse the walker's recorded-order `Shuffle`. Today `Shuffle` only knows Search piles of world cards.

**Files:**
- Modify: `M/model/ActionValues.scala`, `M/model/CoreOperations.scala:632-634`
- Modify: `M/gameplay/operations/PileOperations.scala`, `M/gameplay/operations/OperationApplication.scala:196`, `M/gameplay/walker/WalkerShuffles.scala`, `M/gameplay/powers/action/ClayRattle.scala:42`
- Modify: `M/serialization/WalkerOperationCodec.scala:104-109, 380-396`
- Test: `T/gameplay/operations/ShufflePileSuite.scala` (create), plus whatever existing shuffle suites the compiler names

**Interfaces:**
- Produces: `ShufflePile` (`Search(source)`, `EdificeDeck`, `SetAsideRelics`), `ShufflePile.cards(current: CurrentGameState): Vector[CardId]`, and `Shuffle(pile: ShufflePile, order: Option[Vector[CardId]] = None)`.

- [ ] **Step 1: Write the failing test**

```scala
package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** `Shuffle` reorders the edifice deck and the set-aside relics as it does
  * a Search pile (Chronicle Sun and Throne plan, Task 1). */
class ShufflePileSuite extends munit.FunSuite:
  private def apply(ready: ReadyGame, op: CoreOperation) =
    OperationExecutor.execute(ready, Vector(op))

  test("a recorded order reverses the edifice deck"):
    val ready = Table.start.ready
    val deck = ready.game.current.commonCards.edificeDeck
    val after = accepted(apply(ready,
      Shuffle(ShufflePile.EdificeDeck, Some(deck.reverse))))
    assertEquals(after.game.current.commonCards.edificeDeck, deck.reverse)

  test("an order that drops an edifice is refused"):
    val ready = Table.start.ready
    val deck = ready.game.current.commonCards.edificeDeck
    rejected(apply(ready, Shuffle(ShufflePile.EdificeDeck, Some(deck.tail))))

  test("set-aside relics take the recorded order"):
    val table = Table.start
    val relics = table.ready.game.current.commonCards.relicDeck.take(2)
    val ready = table.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        relicDeck = current.commonCards.relicDeck.drop(2)),
      setAsideRelics = relics))).ready
    val after = accepted(apply(ready,
      Shuffle(ShufflePile.SetAsideRelics, Some(relics.reverse))))
    assertEquals(after.game.current.setAsideRelics, relics.reverse)
```

Before writing the test, check how existing operation suites apply one operation (`grep -rn "OperationExecutor\." T/gameplay/operations | head`). If the entry point is not `OperationExecutor.execute(ready, ops)`, use the one they use and keep the assertions.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ShufflePileSuite"`
Expected: compile failure, because `ShufflePile` is not defined.

- [ ] **Step 3: Add `ShufflePile` to `M/model/ActionValues.scala`**

Append after `SearchSource`:

```scala
/** A pile the walker can shuffle with a recorded order: a pile Search draws
  * from, the edifice deck (Build Edifice draws a random edifice per suit),
  * or the set-aside relics (Scatter Relics deals them out). */
sealed trait ShufflePile extends Product with Serializable
object ShufflePile:
  final case class Search(source: SearchSource) extends ShufflePile
  case object EdificeDeck extends ShufflePile
  case object SetAsideRelics extends ShufflePile

  /** The pile's cards in stored order. */
  def cards(pile: ShufflePile, current: CurrentGameState): Vector[CardId] =
    pile match
      case Search(source) => current.commonCards.pile(source)
      case EdificeDeck => current.commonCards.edificeDeck
      case SetAsideRelics => current.setAsideRelics

  /** How a failure names the pile. */
  def name(pile: ShufflePile): String = pile match
    case Search(source) => SearchSource.name(source)
    case EdificeDeck => "edifice deck"
    case SetAsideRelics => "set-aside relics"
```

- [ ] **Step 4: Change `Shuffle` in `M/model/CoreOperations.scala`**

```scala
final case class Shuffle(pile: ShufflePile,
    order: Option[Vector[CardId]] = None) extends PrimitiveOperation:
  override val required: Boolean = true
```

Update the doc comment above it: "A pile Search draws from" becomes "A shuffle pile".

- [ ] **Step 5: Generalize the write in `PileOperations.scala`**

```scala
  def shuffle(ready: ReadyGame, pile: ShufflePile,
      order: Option[Vector[CardId]]): Either[OperationError, ReadyGame] =
    val held = ShufflePile.cards(pile, ready.game.current)
    val name = ShufflePile.name(pile)
    order match
      case None => Left(InvalidDescription(
        s"a shuffle of the $name has no order"))
      case Some(cards) if cards.size != held.size || cards.diff(held).nonEmpty =>
        Left(InvalidDescription(
          s"a shuffle of the $name must hold exactly its cards"))
      case Some(cards) => Right(withCards(ready, pile, cards).copy(
        knowledge = cards.foldLeft(ready.knowledge)(CardKnowledgeMoves.forget)))

  private def withCards(ready: ReadyGame, pile: ShufflePile,
      cards: Vector[CardId]): ReadyGame = ready.updateCurrent(current =>
    pile match
      case ShufflePile.Search(source) => current.copy(commonCards =
        current.commonCards.withPile(source,
          cards.collect { case id: WorldCardId => id }))
      case ShufflePile.EdificeDeck => current.copy(commonCards =
        current.commonCards.copy(
          edificeDeck = cards.collect { case id: EdificeId => id }))
      case ShufflePile.SetAsideRelics => current.copy(
        setAsideRelics = cards.collect { case id: RelicId => id }))
```

`CardKnowledgeMoves.forget` takes the fold's element type. If it only takes `WorldCardId`, fold over the `WorldCardId`s only (`cards.collect { case id: WorldCardId => id }`): edifices and set-aside relics carry no peek knowledge. Update the doc comment's first line to "The `Shuffle` write: a shuffle pile takes the order the walker generated."

- [ ] **Step 6: Fix the callers**

- `WalkerShuffles.ordered`: read `val cards = ShufflePile.cards(shuffle.pile, state.game.current)`. The rest is unchanged.
- `ClayRattle.scala:42`: `Shuffle(ShufflePile.Search(_))`.
- `OperationApplication.scala:196`: the case still reads `Shuffle(pile, order)`; check that it compiles.
- Any formatter that names the shuffled pile (`grep -rn "Shuffle(" M/application`): match `ShufflePile.Search(source)` and write today's line. Write a "shuffled the edifice deck" or "shuffled the set-aside relics" line only if the match must be exhaustive. Otherwise leave those piles silent: the Chronicle procedures write their own lines.

- [ ] **Step 7: Codec**

In `WalkerOperationCodec.scala`, encode the pile with a new helper, and the order with `encodeCardId`:

```scala
  private def encodeShufflePile(pile: ShufflePile): ujson.Value = pile match
    case ShufflePile.Search(source) => encodeSearchSource(source)
    case ShufflePile.EdificeDeck => ujson.Obj("kind" -> "edifice-deck")
    case ShufflePile.SetAsideRelics => ujson.Obj("kind" -> "set-aside-relics")

  private def decodeShufflePile(value: ujson.Value, path: String)
      : Either[WireError, ShufflePile] =
    value.obj.get("kind").map(_.str) match
      case Some("edifice-deck") => Right(ShufflePile.EdificeDeck)
      case Some("set-aside-relics") => Right(ShufflePile.SetAsideRelics)
      case _ => decodeSearchSource(value, path).map(ShufflePile.Search.apply)
```

`decodeShuffleOrder` decodes each element with `decodeCardId`. Search piles encode exactly as before.

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ShufflePileSuite oathdigital.serialization.* oathdigital.gameplay.powers.action.*"`
Expected: PASS. Fix any wire round-trip suite that builds a `Shuffle` (the compiler names them) by wrapping its source in `ShufflePile.Search`. Add one round-trip case for `Shuffle(ShufflePile.EdificeDeck, Some(Vector(<an edifice id>)))` to the suite that round-trips walker operations (`WalkerOperationWireSuite`).

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/oathdigital/model/ActionValues.scala src/main/scala/oathdigital/model/CoreOperations.scala src/main/scala/oathdigital/gameplay/operations/PileOperations.scala src/main/scala/oathdigital/gameplay/walker/WalkerShuffles.scala src/main/scala/oathdigital/gameplay/powers/action/ClayRattle.scala src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala src/test/scala/oathdigital/gameplay/operations/ShufflePileSuite.scala
# plus every other file this task touched, by path
git commit -m "feat(walker): shuffle the edifice deck and set-aside relics with a recorded order

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The set-aside edifice list

Ruling 9: edifices discarded during play or in Shape Empire go to an ordered set-aside list, empty at game start. This mirrors `setAsideRelics` and `Location.SetAsideRelics` exactly.

**Files:**
- Modify: `M/model/GameState.scala` (`CurrentGameState`), `M/model/CoreOperations.scala` (`Location`, `Discard.RuinedEdifice`), `M/model/CardIndex.scala:20, 200`, `M/model/OperationError.scala:197`
- Modify: `M/gameplay/operations/CardMovementOperations.scala:139, 252, 309`, `OperationStateWrites.scala:146`, `OperationStateAdapter.scala:179`, `CardKnowledgeMoves.scala:43`
- Modify: `M/serialization/WalkerValueCodec.scala:169, 195`
- Test: `T/gameplay/operations/SetAsideEdificesSuite.scala` (create); update every suite that expected a ruined edifice at the edifice deck's bottom

**Interfaces:**
- Produces: `CurrentGameState.setAsideEdifices: Vector[EdificeId] = Vector.empty`, `Location.SetAsideEdifices`, `CardContainer.SetAsideEdifices`. `Discard.RuinedEdifice` now moves the card to `PositionedLocation(Location.SetAsideEdifices)`.

- [ ] **Step 1: Write the failing test**

```scala
package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Ruling 9: a discarded ruined edifice joins the set-aside list, in
  * discard order, and not the edifice deck. */
class SetAsideEdificesSuite extends munit.FunSuite:
  test("a discarded ruined edifice joins the set-aside list"):
    val ready = Table.start
      .edifice("Bandit Market", EdificeSide.Ruined, at = "Ancient City").ready
    val edifice = Table.edifice("Bandit Market")
    val deck = ready.game.current.commonCards.edificeDeck
    val after = accepted(OperationExecutor.execute(ready, Vector(
      Discard.RuinedEdifice(edifice,
        PositionedLocation(Location.Site(Table.site("Ancient City"))),
        Suit.Discord, favor = 0, secrets = 0, actingPlayer = p1))))
    assertEquals(after.game.current.setAsideEdifices, Vector(edifice))
    assertEquals(after.game.current.commonCards.edificeDeck, deck)

  test("the card index finds a set-aside edifice"):
    val ready = Table.start
      .edifice("Bandit Market", EdificeSide.Ruined, at = "Ancient City").ready
    val edifice = Table.edifice("Bandit Market")
    val after = accepted(OperationExecutor.execute(ready, Vector(
      Discard.RuinedEdifice(edifice,
        PositionedLocation(Location.Site(Table.site("Ancient City"))),
        Suit.Discord, 0, 0, p1))))
    assertEquals(after.game.cardIndex.toOption.flatMap(_.locationOf(edifice))
      .map(_.container), Some(CardContainer.SetAsideEdifices))
```

Check the `Table.edifice` step's name argument (`Table.scala:168`) and the `CardLocation` field name (`model/CardIndex.scala`); "Bandit Market" is the ruined face of E02 in `GreatMarketRules.scala`. Use the operation entry point Task 1 settled on.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.SetAsideEdificesSuite"`
Expected: compile failure on `setAsideEdifices` and `CardContainer.SetAsideEdifices`.

- [ ] **Step 3: Add the field, location and container**

- `CurrentGameState`: after `setAsideRelics`, add
  ```scala
      // Edifices discarded this game, in discard order (Chronicle ruling 9).
      // Build Edifice offers one only when its suit has none left in the
      // edifice deck, and returns them all to the deck when it ends.
      setAsideEdifices: Vector[EdificeId] = Vector.empty,
  ```
- `Location`: `case object SetAsideEdifices extends Location` after `SetAsideRelics`.
- `CardContainer`: `case object SetAsideEdifices extends CardContainer`; in `CardIndex.scan` add `addIds(game.current.setAsideEdifices, CardContainer.SetAsideEdifices)` beside the set-aside relics line.
- `OperationError` location name: `case Location.SetAsideEdifices => "set-aside edifices"`.
- `CardMovementOperations`: copy each `SetAsideRelics` arm. The source check at :139 accepts `Location.SetAsideEdifices`. The removal at :252 filters `setAsideEdifices`. The insert at :309 appends `id.asInstanceOf[EdificeId]` after `ensureEmptyTokens`.
- `OperationStateWrites.scala:146`, `OperationStateAdapter.scala:179`, `CardKnowledgeMoves.scala:43`: add the matching `SetAsideEdifices` arm next to each `SetAsideRelics` one.
- `WalkerValueCodec`: `"set-aside-edifices"` in both directions.

- [ ] **Step 4: Send `Discard.RuinedEdifice` to the list**

In `CoreOperations.scala`, its `children` move becomes:

```scala
    override val children: Vector[Operation] = Vector(Move(
      Piece.Card(card), from,
      PositionedLocation(Location.SetAsideEdifices), by = by)) ++
      returnedResources(card, suit, favor, secrets, actingPlayer)
```

Update its doc comment: "A discarded ruined edifice is set aside in discard order, not buried, so Build Edifice offers it only when its suit has no other (Chronicle ruling 9)."

- [ ] **Step 5: Run the full suite and fix expectations**

Run: `./sbtw test`
Expected: the new suite passes. Suites that expected a discarded ruined edifice at the bottom of `edificeDeck` (homeland powers, Game Log goldens) fail. Change those expectations to `setAsideEdifices`. If a log line said "to the bottom of the edifice deck", change it to "aside" wording, and regenerate the golden with `GAMELOG_GOLDEN=write ./sbtw "testOnly oathdigital.application.gamelog.*"`, then read the diff. Do not change behaviour to make an old expectation pass.

- [ ] **Step 6: Commit**

```bash
git add <every file touched, by path>
git commit -m "feat(model): set discarded ruined edifices aside in order instead of burying them

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Chronicle state and primitives

**Files:**
- Create: `M/model/ChronicleOperations.scala`, `M/gameplay/operations/ChronicleStateOperations.scala`
- Modify: `M/model/GameState.scala` (`CampaignState.boxRoles`, `CurrentGameState.chronicleTasks`), `M/gameplay/operations/OperationApplication.scala`, `M/serialization/WalkerOperationCodec.scala`, `M/serialization/GameEventJsonSupport.scala:115-131`
- Test: `T/gameplay/operations/ChronicleStateOperationsSuite.scala` (create), `T/serialization/WalkerOperationWireSuite.scala`, `T/serialization/GameEventFactsWireSuite.scala`

**Interfaces:**
- Produces, in `oathdigital.model`:
  ```scala
  enum ChronicleTask(val key: String):
    case Throne extends ChronicleTask("throne")
    case World extends ChronicleTask("world")
    case Beacon extends ChronicleTask("beacon")
  final case class RoleChanged(lineage: LineageId, role: Role) extends PrimitiveOperation
  final case class ChronicleTaskAssigned(task: ChronicleTask, player: PlayerId) extends PrimitiveOperation
  final case class GrandScepterFromBox(player: PlayerId) extends PrimitiveOperation
  final case class ImperialWarbandsTaken(count: Int) extends PrimitiveOperation
  final case class LineagePointsScored(lineage: LineageId, points: Int) extends PrimitiveOperation
  final case class EdificeBuilt(edifice: EdificeId, site: SiteId) extends PrimitiveOperation
  ```
- Produces: `CampaignState.boxRoles: Map[LineageId, Role] = Map.empty` and `CurrentGameState.chronicleTasks: Map[ChronicleTask, PlayerId] = Map.empty`.
- Produces: `Role` wire keys `"exile"`, `"citizen"`, `"chancellor"` (fixes the Chancellor-as-citizen bug).

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.gameplay.operations

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The Chronicle's own primitives (Chronicle Sun and Throne plan, Task 3). */
class ChronicleStateOperationsSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    OperationExecutor.execute(ready, ops.toVector)

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  /** The quiet table whose baseline Chronicle also lists `boxed`, a lineage
    * nobody seated, as a Citizen. */
  private def tableWithBoxedCitizen(boxed: LineageId): ReadyGame =
    Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(boxed,
        None, Role.Citizen, Vector.empty, Vector.empty)))))).ready

  test("a role change updates a seated lineage"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, RoleChanged(lineage, Role.Chancellor)))
    assertEquals(after.game.campaign.lineages(lineage).role, Role.Chancellor)

  test("a role change to a lineage in the box is kept apart"):
    val boxed = LineageId("lineage:boxed")
    val after = accepted(run(tableWithBoxedCitizen(boxed),
      RoleChanged(boxed, Role.Exile)))
    assertEquals(after.game.campaign.boxRoles, Map(boxed -> Role.Exile))
    assert(!after.game.campaign.lineages.contains(boxed))

  test("a role change to an unknown lineage is refused"):
    rejected(run(Table.start.ready,
      RoleChanged(LineageId("lineage:nobody"), Role.Exile)))

  test("a task goes to a seated player only"):
    val ready = Table.start.ready
    val after = accepted(run(ready, ChronicleTaskAssigned(ChronicleTask.World, p2)))
    assertEquals(after.game.current.chronicleTasks, Map(ChronicleTask.World -> p2))
    rejected(run(ready,
      ChronicleTaskAssigned(ChronicleTask.World, PlayerId("nobody"))))

  test("the Grand Scepter comes from the box faceup, once"):
    val after = accepted(run(Table.start.ready, GrandScepterFromBox(p1)))
    assertEquals(after.game.current.players.find(_.player == p1).get.relics
      .last, RelicState(TheGrandScepterCard.id, Orientation.FaceUp, Tokens.empty))
    rejected(run(after, GrandScepterFromBox(p2)))

  test("the Imperial supply is taken once and kept after"):
    val after = accepted(run(Table.start.ready, ImperialWarbandsTaken(8)))
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial), Some(8))
    val again = accepted(run(after, ImperialWarbandsTaken(3)))
    assertEquals(again.banks.warbandSupply.get(ForceKind.Imperial), Some(8))

  test("lineage points add to the era score"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, LineagePointsScored(lineage, 3),
      LineagePointsScored(lineage, 1)))
    assertEquals(after.game.campaign.era.lineageScores(lineage), 4)

  test("a built edifice comes from the deck intact"):
    val ready = Table.start.ready
    val edifice = ready.game.current.commonCards.edificeDeck.head
    val site = Table.site("Ancient City")
    val after = accepted(run(ready, EdificeBuilt(edifice, site)))
    assert(!after.game.current.commonCards.edificeDeck.contains(edifice))
    assertEquals(after.game.current.map.sites(site).denizens.last,
      EdificeState(edifice, EdificeSide.Intact, Tokens.empty))

  test("a built edifice may come from the set-aside list"):
    val table = Table.start
    val edifice = table.ready.game.current.commonCards.edificeDeck.head
    val ready = table.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        edificeDeck = current.commonCards.edificeDeck.tail),
      setAsideEdifices = Vector(edifice)))).ready
    val after = accepted(run(ready,
      EdificeBuilt(edifice, Table.site("Ancient City"))))
    assertEquals(after.game.current.setAsideEdifices, Vector.empty)
```

Also add `lineagePointsScored` with `points = 0` to the refused cases (`require` rejects it at construction, so assert `intercept[IllegalArgumentException]`).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ChronicleStateOperationsSuite"`
Expected: compile failure on the new operations.

- [ ] **Step 3: Model**

Create `M/model/ChronicleOperations.scala`:

```scala
package oathdigital.model

/** The three task cards a player can hold in the Chronicle (CR p. 36). The
  * Sun and the Stars are resolved by no holder. */
enum ChronicleTask(val key: String):
  case Throne extends ChronicleTask("throne")
  case World extends ChronicleTask("world")
  case Beacon extends ChronicleTask("beacon")
object ChronicleTask:
  val all: Vector[ChronicleTask] = Vector(Throne, World, Beacon)
  def fromKey(key: String): Option[ChronicleTask] = all.find(_.key == key)

/** Sets a lineage's role (Crown Chancellor, CR p. 36). A seated lineage
  * changes in `CampaignState.lineages`; a lineage only the baseline lists
  * ("even in the box") changes in `CampaignState.boxRoles`. */
final case class RoleChanged(lineage: LineageId, role: Role)
    extends PrimitiveOperation

/** Gives a seated player a Chronicle task (Assign Tasks, CR p. 36). */
final case class ChronicleTaskAssigned(task: ChronicleTask, player: PlayerId)
    extends PrimitiveOperation

/** The winner takes the Grand Scepter from the box, faceup, when no one
  * holds it (Chronicle ruling 10). */
final case class GrandScepterFromBox(player: PlayerId)
    extends PrimitiveOperation

/** The Chancellor's purple warbands join the table: `count` Imperial
  * warbands, unless the game already has an Imperial supply. */
final case class ImperialWarbandsTaken(count: Int) extends PrimitiveOperation:
  require(count > 0, "the Imperial supply must be positive")

/** Adds lineage points to a lineage's era score (Score Lineage Points,
  * CR p. 37). */
final case class LineagePointsScored(lineage: LineageId, points: Int)
    extends PrimitiveOperation:
  require(points > 0, "scored lineage points must be positive")

/** Places an edifice from the edifice deck or the set-aside list at a site,
  * intact (Build Edifice, CR p. 37, Chronicle ruling 12). A `Move` cannot do
  * this: a card with no state takes no edifice side. */
final case class EdificeBuilt(edifice: EdificeId, site: SiteId)
    extends PrimitiveOperation
```

In `GameState.scala`:
- `CampaignState`: after `evolved`, add
  ```scala
      // Roles this Chronicle gave to lineages nobody seated, which the
      // baseline holds (Crown Chancellor: "even in the box", CR p. 36).
      boxRoles: Map[LineageId, Role] = Map.empty
  ```
- `CurrentGameState`: after `setAsideEdifices`, add
  ```scala
      // Who holds each Chronicle task card (Assign Tasks, CR p. 36).
      chronicleTasks: Map[ChronicleTask, PlayerId] = Map.empty,
  ```

- [ ] **Step 4: Application**

Create `M/gameplay/operations/ChronicleStateOperations.scala`:

```scala
package oathdigital.gameplay.operations

import oathdigital.model._

/** Writes of the Chronicle's own primitives (Chronicle steps design, "Live
  * state extended"). */
private[operations] object ChronicleStateOperations:
  import OperationError.InvalidDescription

  def roleChanged(ready: ReadyGame, lineage: LineageId, role: Role)
      : Either[OperationError, ReadyGame] =
    val campaign = ready.game.campaign
    campaign.lineages.get(lineage) match
      case Some(state) => Right(ready.updateCampaign(_.copy(
        lineages = campaign.lineages.updated(lineage, state.copy(role = role)))))
      case None if campaign.baseline.lineages.exists(_.id == lineage) =>
        Right(ready.updateCampaign(_.copy(
          boxRoles = campaign.boxRoles.updated(lineage, role))))
      case None => Left(InvalidDescription(
        s"no lineage ${lineage.value} to change the role of"))

  def taskAssigned(ready: ReadyGame, task: ChronicleTask, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    Either.cond(ready.game.current.players.exists(_.player == player),
      ready.updateCurrent(current => current.copy(
        chronicleTasks = current.chronicleTasks.updated(task, player))),
      InvalidDescription(s"${player.value} is not seated to take a task"))

  def scepterFromBox(ready: ReadyGame, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    val scepter = TheGrandScepterCard.id
    val inPlay = ready.game.cardIndex.toOption
      .exists(_.locationOf(scepter).nonEmpty)
    if inPlay then Left(InvalidDescription(
      "the Grand Scepter is not in the box"))
    else if !ready.game.current.players.exists(_.player == player) then
      Left(InvalidDescription(s"${player.value} is not seated"))
    else Right(ready.updateCurrent(current => current.copy(
      players = current.players.map(state =>
        if state.player == player then state.copy(relics = state.relics :+
          RelicState(scepter, Orientation.FaceUp, Tokens.empty))
        else state))))

  def imperialWarbandsTaken(ready: ReadyGame, count: Int): ReadyGame =
    ready.copy(banks = ready.banks.copy(warbandSupply =
      ready.banks.warbandSupply.updatedWith(ForceKind.Imperial)(
        _.orElse(Some(count)))))

  def lineagePointsScored(ready: ReadyGame, lineage: LineageId, points: Int)
      : ReadyGame = ready.updateCampaign(campaign => campaign.copy(
    era = campaign.era.copy(lineageScores = campaign.era.lineageScores
      .updated(lineage,
        campaign.era.lineageScores.getOrElse(lineage, 0) + points))))

  def edificeBuilt(ready: ReadyGame, edifice: EdificeId, site: SiteId)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    val inDeck = current.commonCards.edificeDeck.contains(edifice)
    val setAside = current.setAsideEdifices.contains(edifice)
    if !inDeck && !setAside then Left(InvalidDescription(
      s"${edifice.value} is neither in the edifice deck nor set aside"))
    else current.map.sites.get(site).toRight(InvalidDescription(
        s"${site.value} is not in play")).map(state =>
      ready.updateCurrent(_.copy(
        commonCards = current.commonCards.copy(
          edificeDeck = current.commonCards.edificeDeck.filterNot(_ == edifice)),
        setAsideEdifices = current.setAsideEdifices.filterNot(_ == edifice),
        map = current.map.copy(sites = current.map.sites.updated(site,
          state.copy(denizens = state.denizens :+
            EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))))))
```

If `ready.game.cardIndex` reports problems (`Left`), `toOption` treats the Scepter as absent. That is wrong only on an already broken state, which other checks reject.

In `OperationApplication.scala`, add one case per primitive next to `ChronicleUpdated`, in the same style (`Either` results go through the match's error mapping; plain `ReadyGame` results are wrapped in `Right`). The compiler then names any other exhaustive match over primitives (planner, hidden outcomes, log formatter): give each new primitive the arm `ChronicleUpdated` already has there.

- [ ] **Step 5: Codec**

- `GameEventJsonSupport`: add protected `encodeRole`/`decodeRole` and use them for `LineageState.role` (lines 119 and 130):
  ```scala
  protected final def encodeRole(role: Role): String = role match
    case Role.Exile => "exile"
    case Role.Citizen => "citizen"
    case Role.Chancellor => "chancellor"
  protected final def decodeRole(value: String, path: String)
      : Either[WireError, Role] = value match
    case "exile" => Right(Role.Exile)
    case "citizen" => Right(Role.Citizen)
    case "chancellor" => Right(Role.Chancellor)
    case other => Left(InvalidValue(path, s"unknown role '$other'"))
  ```
- `WalkerOperationCodec`: add the six primitives to the `TurnOperation` union and encode them beside `chronicle-updated`:
  - `"role-changed"`: `lineage`, `role` (via `encodeRole`)
  - `"chronicle-task-assigned"`: `task` (its key), `player`
  - `"grand-scepter-from-box"`: `player`
  - `"imperial-warbands-taken"`: `count`
  - `"lineage-points-scored"`: `lineage`, `points`
  - `"edifice-built"`: `edifice`, `site`
  Decode mirrors encode. An unknown task key is `InvalidValue`, and a non-positive count or points is `InvalidValue` (catch the `require` with the codec's usual pattern, or check before constructing).
- Add a round-trip case for each primitive to `WalkerOperationWireSuite`. Add a `Role.Chancellor` lineage to the `GameEventFactsWireSuite` Chronicle round trip, plus a refusal for `"role": "regent"`.

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ChronicleStateOperationsSuite oathdigital.serialization.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/model/ChronicleOperations.scala src/main/scala/oathdigital/model/GameState.scala src/main/scala/oathdigital/gameplay/operations/ChronicleStateOperations.scala src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala src/main/scala/oathdigital/serialization/GameEventJsonSupport.scala src/test/scala/oathdigital/gameplay/operations/ChronicleStateOperationsSuite.scala src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala src/test/scala/oathdigital/serialization/GameEventFactsWireSuite.scala
# plus every other file the compiler made you touch
git commit -m "feat(chronicle): journal roles, tasks, the Scepter, Imperial supply, lineage points and built edifices

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Sun and Throne skeletons with their windows

Both procedures exist, are chained, and run every step as an empty window. A fake power proves each window is a seam.

**Files:**
- Modify: `M/model/PowerWindow.scala` (after `SetupEnd`, line ~183), `M/model/ProcedureRef.scala:84-86, 139-140`
- Create: `M/gameplay/chronicle/ChronicleWindows.scala`, `ChronicleSunProcedure.scala`, `ChronicleThroneProcedure.scala`
- Modify: `M/gameplay/chronicle/ChronicleSequence.scala`, `M/gameplay/walker/WalkerProcedureRegistry.scala:336-345`, `M/application/gamelog/ProcedureLog.scala:315-323`
- Test: `T/gameplay/chronicle/ChronicleWindowsSuite.scala` (create), `T/model/ProcedureRefSuite.scala`, `T/model/EnumWireKeySuite.scala` (if it lists window keys)

**Interfaces:**
- Produces: `TriggeredProcedureRef.ChronicleSun("chronicle-sun")` and `ChronicleThrone("chronicle-throne")`; `ChronicleSequence` order Begin, Sun, Throne, Finish.
- Produces the windows, all `OtherWindow`:

| Object | Key |
|---|---|
| `ChronicleRevealRelics` | `chronicle.sun.reveal-relics` |
| `ChronicleFlipLegacies` | `chronicle.sun.flip-legacies` |
| `ChronicleExileDormancy` | `chronicle.sun.exile-dormancy` |
| `ChronicleLegacyCap` | `chronicle.sun.legacy-cap` |
| `ChronicleShuffleLegacies` | `chronicle.sun.shuffle-legacies` |
| `ChronicleCrown` | `chronicle.sun.crown` |
| `ChronicleAssignTasks` | `chronicle.sun.assign-tasks` |
| `ChronicleOfferCitizenship` | `chronicle.throne.offer-citizenship` |
| `ChronicleShapeEmpire` | `chronicle.throne.shape-empire` |
| `ChronicleBuildEdifice` | `chronicle.throne.build-edifice` |
| `ChronicleStockReliquary` | `chronicle.throne.stock-reliquary` |
| `ChronicleScatterRelics` | `chronicle.throne.scatter-relics` |
| `ChronicleScoreLineage` | `chronicle.throne.score-lineage` |
| `ChronicleSwearOath` | `chronicle.throne.swear-oath` |

  That is fourteen windows: the Sun's seven steps and the Throne's seven.
- Produces: `ChronicleWindows.sun: Vector[PowerWindow]`, `ChronicleWindows.throne: Vector[PowerWindow]` (in step order), and `ChronicleWindows.step(window: PowerWindow, ops: Vector[Operation]): Operation` = `Sequence(ops, Some(window))`.

- [ ] **Step 1: Write the failing test**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower, Transform}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.testkit.{Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Every Sun and Throne step runs inside its own power window, so a later
  * legacy power can add to it (Chronicle steps design, "Windows, not
  * hardcoded actors"). The fake power gives p2 one lineage point per
  * window it is folded into. */
class ChronicleWindowsSuite extends munit.FunSuite:
  private final case class PointPerWindow(lineage: LineageId,
      windows: Vector[PowerWindow]) extends ContributingPower:
    val id: PowerId = PowerId("test.chronicle.point-per-window")
    val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    val contributions: Map[PowerWindow, Vector[Contribution]] =
      windows.map(window => window -> Vector[Contribution](Transform(
        (_, ops) => ops :+ BuildOps((_, _) =>
          Right(Vector(LineagePointsScored(lineage, 1))))))).toMap

  /** The Chronicle phase of a game p1 won, with the Begin decision already
    * answered: the driver's next step starts the Sun. */
  private def chronicleWonByP1: OathState = Table.start
    .turn(p1, Phase.Chronicle)
    .update(_.updateCurrent(_.copy(result = Some(GameResult(p1))))).state

  private def lineageOf(state: OathState, player: PlayerId): LineageId =
    Outcomes.ready(state).game.current.players.find(_.player == player)
      .get.lineage

  test("a power folded into every Sun and Throne window runs once per window"):
    val state = chronicleWonByP1
    val lineage = lineageOf(state, p2)
    val windows = ChronicleWindows.sun ++ ChronicleWindows.throne
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowers(Vector(PointPerWindow(lineage, windows))),
      walkerDice = WalkerDiceFixture.blanks)
    val done = accepted(rules.next(OathTransition(state, Vector.empty),
      TriggeredProcedureRef.ChronicleBegin))
    assertEquals(Outcomes.ready(done.state).game.campaign.era
      .lineageScores.get(lineage), Some(windows.size))
```

This test only passes while every window is empty. Tasks 5-9 add decisions that park the walk, so each of them must keep it passing by answering what parks (see Task 9, Step 5). If `new OathRules(...)` does not take `walkerPowerCatalog` as `WalkerPowers`, copy the constructor call from `ChronicleBeginSuite` and wrap the fake power the way `EmptyWindowModifier.powersFor` does.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleWindowsSuite"`
Expected: compile failure on `ChronicleWindows`.

- [ ] **Step 3: Windows and refs**

- Add the fourteen `case object`s to `PowerWindow` after `SetupEnd`, each `extends OtherWindow { val key = "<key>" }`. If `PowerWindow` keeps an `all` list (`grep -n "val all" M/model/PowerWindow.scala`), append them in step order.
- Add to `TriggeredProcedureRef` after `ChronicleBegin`:
  ```scala
  /** The Sun task: reveal relics, legacies, crown the winner, assign tasks. */
  case ChronicleSun extends TriggeredProcedureRef("chronicle-sun")
  /** The Throne task, resolved by its holder. */
  case ChronicleThrone extends TriggeredProcedureRef("chronicle-throne")
  ```
  and add both to `TriggeredProcedureRef.all` between `ChronicleBegin` and `ChronicleFinish`.

- [ ] **Step 4: `ChronicleWindows` and the two skeletons**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.model._

/** The power window each Chronicle step runs in, in step order (Chronicle
  * steps design, "Windows, not hardcoded actors"). A step is its window's
  * node: the base rule's operations are its children, and a power's
  * `Transform` adds to them or replaces them. */
object ChronicleWindows:
  import PowerWindow._
  val sun: Vector[PowerWindow] = Vector(ChronicleRevealRelics,
    ChronicleFlipLegacies, ChronicleExileDormancy, ChronicleLegacyCap,
    ChronicleShuffleLegacies, ChronicleCrown, ChronicleAssignTasks)
  val throne: Vector[PowerWindow] = Vector(ChronicleOfferCitizenship,
    ChronicleShapeEmpire, ChronicleBuildEdifice, ChronicleStockReliquary,
    ChronicleScatterRelics, ChronicleScoreLineage, ChronicleSwearOath)

  def step(window: PowerWindow, ops: Vector[Operation]): Operation =
    Sequence(ops, Some(window))
```

```scala
package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** The Sun task (CR p. 36, `docs/rules/chronicle.md` task 1). Steps 2-5
  * resolve legacies, which arrive with the Legacies slice: they are empty
  * windows here. */
object ChronicleSunProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "the Sun takes no start selection"))
      _ <- ready.game.current.result.toRight(
        OathViolation.InvalidEventOrder("the game has no result"))
    yield Sequence(ChronicleWindows.sun.map(ChronicleWindows.step(_, Vector.empty)))
```

`ChronicleThroneProcedure` is the same with `ChronicleWindows.throne` and the message "the Throne takes no start selection". Later tasks replace one empty step at a time, so build each step through a `steps(...)` vector you can edit in place.

- [ ] **Step 5: Chain, register, log**

- `ChronicleSequence.order`: `Vector(ChronicleBegin, ChronicleSun, ChronicleThrone, ChronicleFinish)`. Update its doc comment: World, Beacon and Stars are later inserted between Throne and Finish.
- `WalkerProcedureRegistry`: two entries shaped like `ChronicleFinish`'s, with `build = rebuild = ChronicleSunProcedure.build` and `ChronicleThroneProcedure.build`.
- `ProcedureLog`: for now both get
  ```scala
  ProcedureLog(tag = None, isSection = false,
    lines = ActionLineSource.Silent("the Sun's steps write no lines yet"))
  ```
  (the Throne says "the Throne's"). Tasks 5 and 9 replace these with note lines.
- `ProcedureRefSuite` and any wire-key suite: add the two refs and fourteen window keys where they list them.

- [ ] **Step 6: Run the Chronicle and Game Log suites**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.* oathdigital.application.gamelog.* oathdigital.model.*"`
Expected: PASS. `ChronicleBeginSuite` still passes, because empty windows ask nothing. `GameLogPropertiesSuite` requires a script to complete every `ProcedureRef`. The existing `chronicle-started` script now runs Sun and Throne, so it covers both. If a golden changes, regenerate it and read the diff: it should change only where the new procedures appear.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/model/PowerWindow.scala src/main/scala/oathdigital/model/ProcedureRef.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleWindows.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleSunProcedure.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleThroneProcedure.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleSequence.scala src/main/scala/oathdigital/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/oathdigital/application/gamelog/ProcedureLog.scala src/test/scala/oathdigital/gameplay/chronicle/ChronicleWindowsSuite.scala
# plus the updated ref and golden files, by path
git commit -m "feat(chronicle): chain the Sun and Throne tasks with one power window per step

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The Sun: reveal relics, crown the winner, assign tasks

**Files:**
- Create: `M/gameplay/chronicle/ChronicleTasks.scala`, `ChronicleCrown.scala`, `ChronicleTaskAssignment.scala`
- Modify: `M/gameplay/chronicle/ChronicleSunProcedure.scala`, `M/application/gamelog/ProcedureLog.scala`
- Test: `T/gameplay/chronicle/ChronicleSunSuite.scala` (create); update `ChronicleBeginSuite`, `GameApplicationServiceStreamSuite`, `ChronicleWindowsSuite`, the `chronicle-started` goldens

**Interfaces:**
- Consumes: Task 3 primitives, `ChronicleWindows.step`.
- Produces in `ChronicleTasks`:
  ```scala
  def winner(ready: ReadyGame): Either[OathViolation, PlayerId]
  def holder(ready: ReadyGame, task: ChronicleTask): Either[OathViolation, PlayerId]
  def seated(ready: ReadyGame): Vector[PlayerId]
  def playerOptions(players: Vector[PlayerId]): Vector[DecisionOption]
  def chosenPlayer(pending: PendingTree, decisionId: String): Either[OathViolation, PlayerId]
  def failing(violation: OathViolation): Operation
  /** Whether `player`'s side rules `site`: the Empire for an Imperial,
    * the player for an Exile (ruling 11). */
  def rules(ready: ReadyGame, player: PlayerId, site: SiteId): Boolean
  def noteId(ref: TriggeredProcedureRef): PowerId
  ```
- Produces decision ids: `chronicle.assign.world`, `chronicle.assign.beacon`, `chronicle.assign.give-task`, `chronicle.assign.give-recipient`, and buttons `chronicle.give-world` and `chronicle.give-beacon`.
- Produces note keys in `ChronicleSunProcedure`: `crowned` ("{0} became Chancellor"), `empireBroke` ("The Empire broke"), `tookTask(task)` ("{0} took the Throne task" / "World" / "Beacon").

- [ ] **Step 1: Write the failing tests**

Create `T/gameplay/chronicle/ChronicleSunSuite.scala`. The fixture is the same as in `ChronicleWindowsSuite`: phase Chronicle, p1 won, Begin answered, and `rules.next(..., ChronicleBegin)` starts the Sun. On `Table.start`, p1 is at Ancient City, p2 at Broken Peaks and p3 at Buried Giant, nobody holds a banner, and every board has 3 warbands.

Tests (write each in full):

1. "facedown relics held by players are revealed; relics at sites stay facedown". Give p2 a facedown relic (`Table.relic(p2, <name>, Orientation.FaceDown)`; check the step's signature at `Table.scala:147`) and put one facedown at a site (`relicAt`). After the Sun, p2's relic is `FaceUp` and the site relic is still `FaceDown`.
2. "an Exile winner becomes Chancellor and the Empire's warbands replace theirs". Give p1 2 warbands at Ancient City (`warbandsAt("Ancient City", p1, 2)`). After the Sun:
   - p1's lineage role is `Chancellor`;
   - Ancient City holds `SiteForces.Occupied(ForceKind.Imperial, 1)`;
   - p1's board warbands are 0;
   - `banks.warbandSupply(ForceKind.Imperial)` is 8 (sites in play).
3. "the winner takes the Grand Scepter from the box". After the Sun, p1's relics end with the faceup Scepter.
4. "the Grand Scepter moves from the player who holds it". p2 holds the Scepter (`Table.relic(p2, "The Grand Scepter", Orientation.FaceUp)`). After the Sun, p1 holds it and p2 does not.
5. "a Citizen lineage in the box becomes an Exile when the Empire breaks". Seed the baseline with a boxed Citizen lineage, as in Task 3. After the Sun, `boxRoles` maps it to `Exile`.
6. "Reliquary relics are discarded when the Empire breaks". Seed `campaign.reliquary` with one relic taken out of the relic deck. After the Sun, the relic is in `setAsideRelics` and the Reliquary is empty.
7. "a Citizen winner starts a new dynasty". Set p1's lineage to `Citizen`, and a boxed lineage to `Chancellor`, via `update(_.updateCampaign(...))`. After the Sun, p1 is `Chancellor`, `boxRoles` maps the boxed lineage to `Citizen`, and no warband moved. Do not give p1 warbands on the map in this test: Imperial play is unsupported, and Table's inventory check needs an Imperial supply.
8. "the winner takes the Throne; unheld banners park the winner's World then Beacon choice":
   - after the Sun starts, the walk parks on `chronicle.assign.world`, owned by p1;
   - answer `ChooseOneAnswer(DecisionOptionRef.Player(p3))`; it then parks on `chronicle.assign.beacon`;
   - answer p2. `chronicleTasks == Map(Throne -> p1, World -> p3, Beacon -> p2)`.
9. "banner holders take their tasks without a choice". p2 holds the People's Favor and p3 the Darkest Secret (`peoplesFavor(Some(p2), 1)`, `darkestSecret(Some(p3), 1)`). The Sun parks on nothing inside the Sun. The first parked decision belongs to the Throne, or the walk completes while the Throne steps are empty. Tasks are Throne p1, World p2, Beacon p3.
10. "one holder of World and Beacon gives one away". p2 holds both banners. The walk parks on `chronicle.assign.give-task`, owned by p2. Answer `chronicle.give-beacon`; it parks on `chronicle.assign.give-recipient`. Its options are p1 and p3, not p2. Answer p3. World is p2 and Beacon is p3.
11. "the Sun writes its lines". The Game Log for test 8's walk has "p1 became Chancellor", "The Empire broke", "p1 took the Throne task", "p3 took the World task" and "p2 took the Beacon task". Read the lines the way `ChronicleLogScripts` does, or assert the recorded notes with `NoteText` as `GreatMarketRulesSuite` does.
12. "replaying the Sun's events reproduces its state", as in `ChronicleBeginSuite`'s replay test, after test 8's two answers.

Write the answer helper once: `answerPlayer(state, owner, decisionId, player)` calls `rules.resolveWalker(state, owner, decisionId, ChooseOneAnswer(DecisionOptionRef.Player(player)))`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleSunSuite"`
Expected: FAIL. The Sun's windows are empty, so no role, task or relic changes.

- [ ] **Step 3: `ChronicleTasks`**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Who acts in the Chronicle, and the questions its steps share. */
object ChronicleTasks:
  def winner(ready: ReadyGame): Either[OathViolation, PlayerId] =
    ready.game.current.result.map(_.winner).toRight(
      OathViolation.InvalidEventOrder("the game has no result"))

  def holder(ready: ReadyGame, task: ChronicleTask)
      : Either[OathViolation, PlayerId] =
    ready.game.current.chronicleTasks.get(task).toRight(
      OathViolation.InvalidEventOrder(s"no one holds the ${task.key} task"))

  def seated(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.map(_.player)

  def playerOptions(players: Vector[PlayerId]): Vector[DecisionOption] =
    players.map(player => DecisionOption.Player(DecisionOptionRef.Player(player)))

  def chosenPlayer(pending: PendingTree, decisionId: String)
      : Either[OathViolation, PlayerId] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Player(player) => player
    }.toRight(PowerAnswers.missing(decisionId))

  /** A node that refuses the walk, for a `Branch` that finds the state it
    * needs missing. */
  def failing(violation: OathViolation): Operation =
    BuildOps((_, _) => Left(violation), required = true)

  def rules(ready: ReadyGame, player: PlayerId, site: SiteId): Boolean =
    val current = ready.game.current
    val imperial = current.players.find(_.player == player)
      .flatMap(state => ready.game.campaign.lineages.get(state.lineage))
      .exists(_.role.isImperial)
    current.map.sites.get(site).flatMap(state =>
      SiteRule.ruler(state.forces, current.players).toOption).exists {
        case SiteRuler.Empire => imperial
        case SiteRuler.Player(ruler) => !imperial && ruler == player
        case _ => false
      }

  def noteId(ref: TriggeredProcedureRef): PowerId = ProcedureNotes.id(ref)
```

If `PowerAnswers.one` returns something other than `Option[DecisionOptionRef]`, adapt `chosenPlayer`. Check `M/gameplay/powers/PowerAnswers.scala:11`.

- [ ] **Step 4: `ChronicleCrown`**

`ChronicleCrown.operations(ready: ReadyGame, winner: PlayerId): Either[OathViolation, Vector[CoreOperation]]` returns, in this order:

1. The Scepter (ruling 10). Find it in a player's relics, else in a site's relics. Held by the winner: nothing. Held elsewhere: `Move(Piece.Card(scepter), PositionedLocation(from), PositionedLocation(Location.PlayArea(winner)), resultingOrientation = Some(Orientation.FaceUp))`. Nowhere: `GrandScepterFromBox(winner)`.
2. By the winner's lineage role in `campaign.lineages`:
   - **Exile: the Empire breaks.**
     - `RoleChanged(l, Role.Exile)` for every seated Imperial lineage other than the winner's, sorted by id.
     - `RoleChanged(l, Role.Exile)` for every baseline lineage not seated whose current role is Imperial, sorted by id. Its current role is `boxRoles.getOrElse(id, baseline role)`.
     - Each Reliquary relic, in order, moves from `Location.Reliquary` to `Location.SetAsideRelics`.
     - `ImperialWarbandsTaken(map.inPlay.size)`.
     - For each site in `map.inPlay` with Imperial warbands: `Kill(Piece.Warbands(ForceKind.Imperial, n), at)`.
     - The winner's board warbands, if any, move from `Location.PlayArea(winner)` to `Location.WarbandBank(ForceKind.Exile(lineage))`.
     - For each site in `map.inPlay` with `Occupied(ForceKind.Exile(lineage), n)` of the winner's lineage: `Kill(Piece.Warbands(kind, n - 1), at)` when `n > 1`, then `Replace(Piece.Warbands(kind, 1), Piece.Warbands(ForceKind.Imperial, 1), at)`.
     - `RoleChanged(lineage, Role.Chancellor)`.
   - **Citizen: a new dynasty.** `RoleChanged(former, Role.Citizen)` for the current Chancellor (seated, or boxed by current role), if any, then `RoleChanged(lineage, Role.Chancellor)`.
   - **Chancellor:** nothing more.

Here `at = PositionedLocation(Location.Site(site))`. Check that board warbands live at `Location.PlayArea(player)`: `grep -rn "board.warbands" M/gameplay/operations/ResourceOperations.scala`. If a different location reads the board, use it.

Write the Exile branch's "newly dormant legacies become active" as a comment citing the Legacies slice. There are no legacies to flip.

- [ ] **Step 5: `ChronicleTaskAssignment`**

`ChronicleTaskAssignment.steps(winner: PlayerId): Vector[Operation]` returns:

```scala
Vector(
  BuildOps((_, _) => Right(Vector(
    ChronicleTaskAssigned(ChronicleTask.Throne, winner))), required = true),
  bannerTask(ChronicleTask.World, worldDecisionId,
    _.banners.peoplesFavor.holder, winner, "World"),
  bannerTask(ChronicleTask.Beacon, beaconDecisionId,
    _.banners.darkestSecret.holder, winner, "Beacon"),
  Branch((state, _) => doubledHolder(state).toVector.flatMap(giveAway)))
```

- `bannerTask`: a `Branch` reading the holder from live state. With a holder, it returns one `BuildOps` assigning the task. Without one, it returns `Decide(id, winner, ChooseOne(playerOptions(seated), heading = Some(s"Give the $name task"), confirmVerb = Some("Give to")))`, then `BuildOps` that assigns `chosenPlayer(pending, id)`.
- `doubledHolder`: `Some(p)` when `chronicleTasks(World) == chronicleTasks(Beacon) == p`.
- `giveAway(p)`:
  - `Decide(giveTaskDecisionId, p, ChooseOne(Vector(Button(giveWorld, "Give the World task"), Button(giveBeacon, "Give the Beacon task")), heading = Some("Give one task away")))`;
  - `Decide(giveRecipientDecisionId, p, ChooseOne(playerOptions(seated.filterNot(_ == p)), heading = Some("Choose who takes it"), confirmVerb = Some("Give to")))`;
  - `BuildOps` that reads both answers and assigns the chosen task to the recipient.

Each assignment is followed by a `Note` writing `tookTask(task)` with the holder read from `states.now`.

- [ ] **Step 6: Assemble the Sun**

In `ChronicleSunProcedure.build`, read the winner from the result, and build the steps:

```scala
Vector(
  ChronicleWindows.step(PowerWindow.ChronicleRevealRelics, Vector(
    BuildOps((state, _) => Right(revealHeldRelics(state)), required = true))),
  ChronicleWindows.step(PowerWindow.ChronicleFlipLegacies, Vector.empty),
  ChronicleWindows.step(PowerWindow.ChronicleExileDormancy, Vector.empty),
  ChronicleWindows.step(PowerWindow.ChronicleLegacyCap, Vector.empty),
  ChronicleWindows.step(PowerWindow.ChronicleShuffleLegacies, Vector.empty),
  ChronicleWindows.step(PowerWindow.ChronicleCrown, Vector(
    BuildOps((state, _) => ChronicleCrown.operations(state, winner),
      required = true),
    Note(noteId, crownedNote(winner)))),
  ChronicleWindows.step(PowerWindow.ChronicleAssignTasks,
    ChronicleTaskAssignment.steps(winner)))
```

- `revealHeldRelics` returns `Reveal(id, Location.PlayArea(player))` for every `RelicState(id, Orientation.FaceDown, _)` a player holds, in seat order.
- `crownedNote` writes "The Empire broke" when the winner's role in `states.previous` was Exile, then "{winner} became Chancellor". Use one `Note` per line if a `Note` writes one line.
- Each empty step's comment cites its rule (CR p. 36, Sun steps 2-5) and says it waits for the Legacies slice.
- Declare the note keys in a `NoteSet(ChronicleTasks.noteId(TriggeredProcedureRef.ChronicleSun))`, as `SetupProcedure` does.

Update `ProcedureLog`'s `ChronicleSun` entry:

```scala
case TriggeredProcedureRef.ChronicleSun => ProcedureLog(tag = None,
  lines = ActionLineSource.Notes(Map(
    ChronicleSunProcedure.empireBroke -> LogKind.Delta,
    ChronicleSunProcedure.crowned -> LogKind.Delta) ++
    ChronicleTask.all.map(task =>
      ChronicleSunProcedure.tookTask(task) -> LogKind.Decision)),
  narratedPrefixes = Vector("chronicle.assign."), isSection = false)
```

- [ ] **Step 7: Run the tests, then fix the suites the new decisions break**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.* oathdigital.application.*"`

Expected:
- `ChronicleSunSuite` passes.
- `ChronicleBeginSuite`'s "starting the Chronicle enters its phase and journals the baseline" now parks on `chronicle.assign.world`. Change it to assert the phase is `Chronicle`, the walk parks on `chronicle.assign.world` owned by p1, and `evolved` is `None`. The `ChronicleUpdated` assertion moves to Task 10's end-to-end test. Its "runs once" test answers both assign decisions first: write a helper `startedAndTasksAssigned` that answers the World and Beacon choices with p2 and p3.
- `GameApplicationServiceStreamSuite`'s round-eight test: after the winner starts, assert the winner owns `chronicle.assign.world` and the others wait. Replace `evolved == baseline` on reload with "reload is still parked on `chronicle.assign.world`".
- `ChronicleWindowsSuite` answers the two assign decisions with the same helper. Move the helper into a test-side `ChronicleWalk` object under `T/gameplay/chronicle/` so all three suites share it.
- The `chronicle-started` log goldens: the harness answers parked decisions by default. Regenerate and read the diff.

- [ ] **Step 8: Commit**

```bash
git add <the files above, by path>
git commit -m "feat(chronicle): the Sun reveals held relics, crowns the winner and assigns the tasks

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Throne: Shape Empire

**Files:**
- Create: `M/gameplay/chronicle/ShapeEmpire.scala`
- Modify: `M/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Test: `T/gameplay/chronicle/ShapeEmpireSuite.scala` (create)

**Interfaces:**
- Consumes: `ChronicleTasks.holder`, `ChronicleTasks.rules`, `PlayerForceKind.of`.
- Produces: `ShapeEmpire.steps(catalog: ExecutableCatalog): Vector[Operation]` and decision id `chronicle.shape-empire.remove`. Note key `discarded` ("{0} was discarded from {1}").

Rules (CR p. 37, rulings 6, 9, 11 and 14): the Throne holder may remove any of their warbands from sites, keeping at least one on the map. If they rule no sites, they remove every warband at their pawn's site and place one of theirs there. Then every denizen at a site they do not rule is discarded, except intact edifices and every card at an Enduring site. A discarded ruined edifice is set aside (Task 2), and other denizens go to the discard pile of their site's region.

- [ ] **Step 1: Write the failing tests**

Fixture: Sun done and tasks assigned. Start the Throne directly with `rules.next(OathTransition(state, Vector.empty), TriggeredProcedureRef.ChronicleSun)` on a Table already in Chronicle phase. Set `chronicleTasks = Map(Throne -> p1, World -> p2, Beacon -> p3)`, p1's lineage `Chancellor`, an Imperial supply of 8, and Imperial warbands where each test needs them. Build it with a helper `throneHeldByChancellorP1(sites: SiteId*)` that places one Imperial warband on each listed site via `update`. `Table.ready` then accepts the Imperial warbands, because the supply exists.

Tests:
1. "the Chancellor may remove Imperial warbands but keeps the last one". Imperial at Ancient City and Broken Peaks. The walk parks on `chronicle.shape-empire.remove`, owned by p1: a `Distribute` with one slot per Empire site, each max 1, and total max 1. Answer 1 at Broken Peaks. Broken Peaks is now `Empty`, and Ancient City keeps its warband.
2. "one Imperial warband on the map asks nothing". Imperial only at Ancient City: no `chronicle.shape-empire.remove` decision.
3. "with no Empire sites the pawn's site gets one Imperial warband". No Imperial warbands, 2 bandits at Ancient City (p1's pawn). The bandits are killed, and Ancient City holds `Occupied(Imperial, 1)`.
4. "denizens at sites the Empire does not rule are discarded to their region". Imperial at Ancient City. A faceup denizen at Ancient City stays. A faceup denizen at Broken Peaks, which has no warbands, goes to `regionalDiscards(regionOf(Broken Peaks))`.
5. "intact edifices stay and ruined ones are set aside". An intact edifice and a ruined edifice at unruled sites. The intact one stays, and the ruined one is in `setAsideEdifices`.
6. "cards at an Enduring site stay". Put a denizen at an Enduring site the Empire does not rule. Ancient City is Enduring, so make Broken Peaks the only Empire site and put the denizen at Ancient City. It stays.
7. "the discard writes one line per card", using `discarded`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ShapeEmpireSuite"`
Expected: FAIL (the step is empty).

- [ ] **Step 3: Implement `ShapeEmpire`**

```scala
object ShapeEmpire:
  val removeDecisionId: String = "chronicle.shape-empire.remove"

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => ChronicleTasks.holder(state, ChronicleTask.Throne)
      .fold(violation => Vector(ChronicleTasks.failing(violation)),
        holder => removal(state, holder))),
    Branch((state, _) => ChronicleTasks.holder(state, ChronicleTask.Throne)
      .fold(violation => Vector(ChronicleTasks.failing(violation)),
        holder => discards(catalog, state, holder))))
```

- `removal(state, holder)`:
  - `kind = PlayerForceKind.of(state, holderState)`.
  - `ruled` = sites in `map.inPlay` where `ChronicleTasks.rules(state, holder, site)`, each with its count of `kind`.
  - If `ruled` is empty, return the fallback `BuildOps`: `Kill` whatever force stands at the pawn site, then `Move(Piece.Warbands(kind, 1), PositionedLocation(Location.WarbandBank(kind)), PositionedLocation(Location.Site(pawnSite)))`.
  - Else, with `total = ruled.map(_._2).sum`: when `total <= 1`, ask nothing. Otherwise `Decide(removeDecisionId, holder, DecisionQuery.Distribute(ruled.map((site, n) => DistributeSlot(DecisionOptionRef.Site(site), 0, n, None)), 0, total - 1, heading = Some("Remove warbands from the Empire"), confirmLabel = "Remove"))`, then a `BuildOps` that returns, per slot amount `a > 0`, `Move(Piece.Warbands(kind, a), PositionedLocation(Location.Site(site)), PositionedLocation(Location.WarbandBank(kind)))`.
  - Read the answer as `DecisionAnswer.DistributeAnswer(amounts)`; check `DistributeAmount`'s fields in `model/Decisions.scala`.
- `discards(catalog, state, holder)`: one `BuildOps` and one `Note` per discarded card. For each site in `map.inPlay` where `!ChronicleTasks.rules(state, holder, site)` and the site is not Enduring:
  - `DenizenState(id, _, tokens)`: `Discard.Denizen(id, at, region, suit, tokens.favor, tokens.secrets, actingPlayer = holder)`.
  - `EdificeState(id, EdificeSide.Ruined, tokens)`: `Discard.RuinedEdifice(id, at, suit, tokens.favor, tokens.secrets, holder)`.
  - `EdificeState(_, EdificeSide.Intact, _)`: kept.
  - Suits come from `catalog.denizen(id)` and `catalog.edifice(id)`.
  - A site is Enduring when `catalog.site(site).exists(_.handlers.exists(_.value.endsWith(".enduring")))`. Ancient City's handler is `site.ancient-city.enduring` (`RiverSitePower.scala:14`). Put this in a named helper `enduring(catalog, site)` with a comment citing CR p. 37.
- Add a comment at the discard: "Some legacies also protect denizens (ruling 6); they arrive with legacy powers through this step's window."

Replace the Throne's `ChronicleShapeEmpire` empty step with `ChronicleWindows.step(PowerWindow.ChronicleShapeEmpire, ShapeEmpire.steps(catalog))`.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.*"`
Expected: PASS. The shared `ChronicleWalk` helper and the windows suite now answer `chronicle.shape-empire.remove` whenever it parks. On `Table.start` it does not: p1 has one Imperial warband or none after Crown.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/chronicle/ShapeEmpire.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleThroneProcedure.scala src/test/scala/oathdigital/gameplay/chronicle/ShapeEmpireSuite.scala
git commit -m "feat(chronicle): Shape Empire removes the Chancellor's warbands and discards unruled denizens

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Throne: Build Edifice

**Files:**
- Create: `M/gameplay/chronicle/BuildEdifice.scala`
- Modify: `M/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Test: `T/gameplay/chronicle/BuildEdificeSuite.scala` (create)

**Interfaces:**
- Consumes: `Shuffle(ShufflePile.EdificeDeck)` (Task 1), `setAsideEdifices` (Task 2), `EdificeBuilt` (Task 3), `ChronicleTasks.rules`.
- Produces: `BuildEdifice.steps(catalog): Vector[Operation]`, `BuildEdifice.available(catalog, current): Vector[(Suit, EdificeId)]`, decision ids `chronicle.build-edifice.<suit key>`, and note key `built` ("{0} built {1} at {2}").

Rules (CR p. 37, rulings 9 and 12): the available edifice of a suit is the first edifice of that suit in the shuffled edifice deck, or, when none is left, the first of that suit in the set-aside list. For each available edifice, the Throne holder may bury one denizen of that suit at a site they rule, edifices included. The edifice is then placed there intact. When the step ends, every set-aside edifice returns to the edifice deck.

- [ ] **Step 1: Write the failing tests**

Fixture: as in Task 6, with Imperial at Ancient City. `WalkerDiceFixture.reversingShuffle` gives a known order. Use `available` itself to name the expected edifice, after the same reversal.

1. "each suit with a buriable card offers its available edifice". Put a faceup Order denizen at Ancient City. The walk parks on `chronicle.build-edifice.order` (check `Suit.Order.key`), owned by p1: a `ChooseMany(0, 1, ...)` whose only option is that denizen. No other suit asks.
2. "burying a denizen builds the edifice intact". Answer with the denizen. It is at the world deck's bottom, and Ancient City holds `EdificeState(<available Order edifice>, Intact, Tokens.empty)`.
3. "an intact edifice can be buried for another of its suit". Put an intact Order edifice at Ancient City and answer with it. It goes to the edifice deck's bottom, and the new edifice stands at Ancient City.
4. "declining builds nothing". The answer `ChooseManyAnswer(Vector.empty)` leaves Ancient City unchanged.
5. "a suit with no edifice left in the deck offers its first set-aside one". Move every Order edifice out of the deck into `setAsideEdifices`, in a known order, via `update`. The offered edifice (named in the decision's heading, or read from `available`) is the first of them.
6. "the set-aside list returns to the deck when the step ends". After the step, `setAsideEdifices` is empty and the deck holds those edifices.
7. "cards at sites the Empire does not rule are not offered".
8. "building writes its line" (`built`).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.BuildEdificeSuite"`
Expected: FAIL.

- [ ] **Step 3: Implement**

```scala
object BuildEdifice:
  def decisionId(suit: Suit): String = s"chronicle.build-edifice.${suit.key}"

  /** The edifice each suit offers (ruling 9): the first of its suit in the
    * edifice deck, else the first of its suit set aside. */
  def available(catalog: ExecutableCatalog, current: CurrentGameState)
      : Vector[(Suit, EdificeId)] =
    def suitOf(id: EdificeId) = catalog.edifice(id).map(_.suit)
    Suit.all.flatMap(suit =>
      (current.commonCards.edificeDeck ++ current.setAsideEdifices)
        .find(id => suitOf(id).contains(suit)).map(suit -> _))

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Shuffle(ShufflePile.EdificeDeck),
    Branch((state, _) => ChronicleTasks.holder(state, ChronicleTask.Throne)
      .fold(violation => Vector(ChronicleTasks.failing(violation)),
        holder => available(catalog, state.game.current)
          .flatMap((suit, edifice) => offer(catalog, state, holder, suit, edifice)))),
    BuildOps((state, _) => Right(state.game.current.setAsideEdifices.map(id =>
      Move(Piece.Card(id), PositionedLocation(Location.SetAsideEdifices),
        PositionedLocation(Location.Deck(CardDeck.Edifice), StackPosition.Bottom)))),
      required = true))
```

- `offer` returns nothing when no candidate exists. A candidate is a denizen or edifice of `suit` at a site where `ChronicleTasks.rules(state, holder, site)`. Otherwise it returns:
  - `Decide(decisionId(suit), holder, ChooseMany(0, 1, candidates, heading = Some(s"Bury a card to build ${name}"), confirm = Some(CountedConfirm("Bury", "card", "cards", Some("Build nothing")))))`, where `name` is the edifice's intact face name from the catalog. Check `CountedConfirm`'s parameters in `model/Decisions.scala`, as `SetupProcedure` uses it.
  - A `BuildOps` that, for the chosen card at `site`, returns `Bury.standard(BuryableCard.Denizen(id) or BuryableCard.Edifice(id), PositionedLocation(Location.Site(site)), Some(suit), tokens.favor, tokens.secrets, holder) :+ EdificeBuilt(edifice, site)`.
  - A `Note` for `built`.
- Options are `DecisionOption.Denizen` and `DecisionOption.Edifice`.
- The `Shuffle` must be a leaf of its own, never inside a `BuildOps` (`CoreOperations.scala` doc on `Shuffle`). Leave a comment saying the shuffle draws the random edifice per suit, since the deck's order is otherwise the catalog's.
- A `Branch` built before the first answer reads `available` from state after the shuffle. Each later `offer` reads the same state, because building does not change another suit's availability.
- `Suit.all` is the printed order; check the name in `model`.

Replace the Throne's `ChronicleBuildEdifice` empty step with this one.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.*"`
Expected: PASS. Extend `ChronicleWalk` so it answers any `chronicle.build-edifice.*` with the empty selection.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/chronicle/BuildEdifice.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleThroneProcedure.scala src/test/scala/oathdigital/gameplay/chronicle/BuildEdificeSuite.scala src/test/scala/oathdigital/gameplay/chronicle/ChronicleWalk.scala
git commit -m "feat(chronicle): Build Edifice offers a random edifice per suit and falls back to set-aside ones

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Throne: Stock Reliquary and Scatter Relics

**Files:**
- Create: `M/gameplay/chronicle/ReliquaryAndRelics.scala`
- Modify: `M/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Test: `T/gameplay/chronicle/ReliquaryAndRelicsSuite.scala` (create)

**Interfaces:**
- Produces: `ReliquaryAndRelics.stock: Vector[Operation]`, `ReliquaryAndRelics.scatter(catalog): Vector[Operation]`, and decision id `chronicle.stock-reliquary.discard`.

Rules (CR p. 37):
- Stock: the Throne holder adds every relic they hold, except the Grand Scepter, to the Reliquary facedown. Over 5, they discard relics of their choice until 5 remain.
- Scatter: set aside the Scepter and the Reliquary. Shuffle every discarded relic together with every relic players hold. Deal them facedown to empty relic slots, going Hinterward through `map.inPlay` (Cradle, Provinces, Hinterland, each top to bottom). Return the rest to the relic deck.

- [ ] **Step 1: Write the failing tests**

1. "the Throne holder's relics join the Reliquary, the Scepter does not". p1 holds the Scepter and two relics. After Stock, the Reliquary holds the two and p1 holds the Scepter.
2. "over five, the holder discards down to five". The Reliquary already holds 4 and p1 holds 3. The walk parks on `chronicle.stock-reliquary.discard`, owned by p1: `ChooseMany(2, 2, <7 relic options>)`. Answer two. They are in `setAsideRelics`, and the Reliquary holds 5.
3. "five or fewer asks nothing".
4. "Scatter deals discarded and held relics facedown to empty slots Hinterward". p2 holds one relic, `setAsideRelics` holds two, and every site's relic slots are empty except as noted. With `WalkerDiceFixture.reversingShuffle`, the first slots Hinterward receive the shuffled relics in order. Assert the first site's relics, and that the relic deck's bottom holds whatever is left over. Use a table where fewer empty slots than relics remain, so some relics return.
5. "Scatter leaves the Scepter with its holder and the Reliquary alone".

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ReliquaryAndRelicsSuite"`
Expected: FAIL.

- [ ] **Step 3: Implement**

- `stock`: a `Branch` on the Throne holder that returns:
  - a `BuildOps` moving each held relic other than the Scepter from `Location.PlayArea(holder)` to `Location.Reliquary`, in held order;
  - a `Branch` that, when `reliquary.size > 5`, returns `Decide(discardId, holder, ChooseMany(n - 5, n - 5, reliquary.map(id => DecisionOption.Relic(DecisionOptionRef.Relic(id))), heading = Some("Discard relics from the Reliquary"), confirm = Some(CountedConfirm("Discard", "relic", "relics", None))))`, then a `BuildOps` moving each chosen relic from `Location.Reliquary` to `Location.SetAsideRelics`.
- `scatter(catalog)`:
  1. A `BuildOps` that returns `Discard.Relic(id, PositionedLocation(Location.PlayArea(player)), tokens.secrets, player)` for every relic every player holds, except the Scepter.
  2. `Shuffle(ShufflePile.SetAsideRelics)`, as a leaf of its own.
  3. A `BuildOps` that deals. For each site in `map.inPlay`, `free = catalog.site(site).map(_.relicSlots).getOrElse(0) - siteState.relics.size`. Take that many from the head of `setAsideRelics` and move each with `Move(Piece.Card(id), PositionedLocation(Location.SetAsideRelics), PositionedLocation(Location.Site(site)), resultingOrientation = Some(Orientation.FaceDown))`. Every relic left moves to `PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Bottom)`.
  - Comment: "Set aside the Grand Scepter and the Reliquary": neither is touched. The Scepter stays with its holder, and the Reliquary is a separate zone.

  Before implementing, check two things:
  - Which end of `setAsideRelics` a Move takes "the top" from does not matter: deal from the head and say so in a comment.
  - `Discard.Relic`'s `actingPlayer` takes the secrets. The owner takes them here, because the relic leaves their area.
- Replace the Throne's `ChronicleStockReliquary` and `ChronicleScatterRelics` empty steps.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.*"`
Expected: PASS. Extend `ChronicleWalk` to answer `chronicle.stock-reliquary.discard` with the first `n - 5` options.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/chronicle/ReliquaryAndRelics.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleThroneProcedure.scala src/test/scala/oathdigital/gameplay/chronicle/ReliquaryAndRelicsSuite.scala src/test/scala/oathdigital/gameplay/chronicle/ChronicleWalk.scala
git commit -m "feat(chronicle): stock the Reliquary and scatter relics Hinterward

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Throne: Score Lineage Points and Swear an Oath

**Files:**
- Create: `M/gameplay/chronicle/LineageScoring.scala`, `M/gameplay/chronicle/SwearOath.scala`
- Modify: `M/gameplay/chronicle/ChronicleThroneProcedure.scala`, `M/model/LogNotes.scala` (`NoteUnit.LineagePoint`), `M/application/gamelog/ProcedureLog.scala`
- Test: `T/gameplay/chronicle/LineageScoringSuite.scala`, `T/gameplay/chronicle/SwearOathSuite.scala` (create)

**Interfaces:**
- Produces: decision ids `chronicle.score-lineage.gifts` and `chronicle.swear-oath.goal`; `LineageScoring.winnerPoints = 3`.
- Produces note keys: `scored` ("{0} scored {1}", a `NoteArg.Amount(n, NoteUnit.LineagePoint)`), `gave` ("{0} gave {1} to {2}"), `swore` ("The Oathkeeper goal became {0}", a `NoteArg.Goal`).
- Produces: `NoteUnit.LineagePoint` with key `"lineage-point"`, one `"lineage point"`, many `"lineage points"`.

Rules (CR p. 37, ruling 13):
- Score: the winner scores 3 lineage points plus legacy points (stubbed: the window). Any number of times, they may spend 2 of the points they are scoring to let another seated player score 1.
- Swear: after a Visionary win, the Oathkeeper goal becomes the winning Vision's goal. Otherwise the Throne holder chooses a goal different from the current one.

- [ ] **Step 1: Write the failing tests**

LineageScoring:
1. "the winner may give one point for two". The walk parks on `chronicle.score-lineage.gifts`, owned by p1: a `Distribute` with slots for p2 and p3 only (no p1 slot), each max 1, and total from 0 to 1. Answer p2 = 1. p1's lineage score rises by 1 and p2's by 1.
2. "keeping every point scores 3". Answer all zeros: p1 +3.
3. "the lines name the points" (`scored`, `gave`).

SwearOath:
1. "a Visionary win swears the Vision's goal". Set the result to `GameResult(p1, VictoryKind.Visionary)` and give p1 a revealed Vision of Conquest (`Table.revealedVision`; `VisionRules.trueGoal` gives `Supremacy`), with the current goal `Devotion`. Nothing parks, and the goal is `Supremacy`.
2. "a Visionary win whose goal is already current changes nothing". Goal `Supremacy`, same Vision: no `SetOathkeeperGoal` is recorded.
3. "otherwise the Throne holder chooses a different goal". With a Usurper win, the walk parks on `chronicle.swear-oath.goal`, owned by the Throne holder. The current goal is offered `Unavailable`. Answer Protection; the goal is `Protection`.
4. "the line names the new goal".

Find the Vision of Conquest's id with `CatalogNames` or `Table`'s Vision lookup. Check `VisionRules.scala`'s `goals` map for the exact card.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.LineageScoringSuite oathdigital.gameplay.chronicle.SwearOathSuite"`
Expected: FAIL.

- [ ] **Step 3: Implement `LineageScoring`**

Steps: a `Branch` on the winner (`ChronicleTasks.winner`):
- `others = seated.filterNot(_ == winner)` and `giftable = winnerPoints / 2`.
- When `giftable == 0` or `others` is empty, return a `BuildOps` scoring `LineagePointsScored(winnerLineage, winnerPoints)`.
- Otherwise return:
  - `Decide(giftsId, winner, DecisionQuery.Distribute(others.map(p => DistributeSlot(DecisionOptionRef.Player(p), 0, giftable, None)), 0, giftable, heading = Some("Give lineage points: 2 of yours for 1 of theirs"), confirmLabel = "Score"))`;
  - a `BuildOps`: `given = amounts.sum`, then `LineagePointsScored(winnerLineage, winnerPoints - 2 * given)` (only when positive), then `LineagePointsScored(lineageOf(p), a)` for each `a > 0`;
  - one `Note` for `scored` per lineage that scored, and one `gave` per gift.

Comment: "Legacy points (1 per matching card at sites the winner rules) arrive with the Legacies slice through this step's window."

- [ ] **Step 4: Implement `SwearOath`**

A `Branch`:
- When the result is `VictoryKind.Visionary`, the winner's `revealedVision` maps through `VisionRules.trueGoal` to a goal. Return `SetOathkeeperGoal(goal)` only if it differs from `campaign.oathkeeperGoal`; otherwise return nothing. A Visionary result with no true goal is a violation: return `ChronicleTasks.failing(...)`.
- Otherwise, on the Throne holder: `Decide(goalId, holder, DecisionQuery.ChooseOne(OathkeeperGoalChoice.options(state, Map.empty), heading = Some("Swear an Oath")))`, then `BuildOps((state, pending) => OathkeeperGoalChoice.set(state, pending, goalId, Set.empty))`.

In both branches, follow with a `Note` writing `swore` when `states.previous` shows the goal changed. Build the key with `OathkeeperGoalChoice.changed(notes, "swore")` if its wording ("Changed the Oathkeeper goal to {0}.") reads right in the log. Otherwise declare the plan's own wording. Either way, call it sourceless.

- [ ] **Step 5: Assemble, log, and keep the earlier suites passing**

- Replace the Throne's `ChronicleScoreLineage` and `ChronicleSwearOath` empty steps.
- `ProcedureLog`'s `ChronicleThrone` entry becomes `ActionLineSource.Notes` over:
  - `ShapeEmpire.discarded -> LogKind.Delta`;
  - `BuildEdifice.built -> LogKind.Decision`;
  - `LineageScoring.scored -> LogKind.Delta`;
  - `LineageScoring.gave -> LogKind.Decision`;
  - `SwearOath.swore -> LogKind.Decision`.

  Set `narratedPrefixes = Vector("chronicle.shape-empire.", "chronicle.build-edifice.", "chronicle.stock-reliquary.", "chronicle.score-lineage.", "chronicle.swear-oath.")`.
- Add `NoteUnit.LineagePoint` and fix any match the compiler names (the note text formatter).
- `ChronicleWalk` answers the gifts decision with all zeros, and the goal decision with the first available goal.
- `ChronicleWindowsSuite`: every window now has base operations, so the fake power's points add to what Score Lineage Points scores. Assert p2's score, since p2 receives nothing from the base rule. Keep the expectation at `windows.size`.

- [ ] **Step 6: Run the Chronicle and log suites**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.* oathdigital.application.gamelog.*"`
Expected: PASS after regenerating the `chronicle-started` goldens. Read the diff: it should show the Throne's lines.

- [ ] **Step 7: Commit**

```bash
git add <the files above, by path>
git commit -m "feat(chronicle): score lineage points with 2-for-1 gifts and swear the next Oath

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: `ChronicleUpdated` carries roles, era and goal; end-to-end and docs

**Files:**
- Create: `M/gameplay/chronicle/ChronicleRecord.scala`
- Modify: `M/gameplay/chronicle/ChronicleFinishProcedure.scala`
- Test: `T/gameplay/chronicle/ChronicleRecordSuite.scala`, `T/gameplay/chronicle/ChronicleSunThroneEndToEndSuite.scala` (create)
- Docs: `docs/rules/chronicle.md` (Build Edifice "Digital:" note), `docs/rules/implementation-traceability.md`, `docs/architecture/game-setup.md`, `CONTEXT.md`, `docs/ROADMAP.md`

**Interfaces:**
- Produces: `ChronicleRecord.campaignFields(ready: ReadyGame): Chronicle`. It returns the baseline with `era`, `oathkeeperGoal` and `lineages` taken from the live campaign. Every other field stays as in the baseline; Stars extracts the rest.

- [ ] **Step 1: Write the failing tests**

ChronicleRecordSuite:
1. "the record keeps the live era and goal". Live `era.lineageScores` and `oathkeeperGoal` differ from the baseline; the record holds the live ones.
2. "seated lineages take their live role; a seated lineage new to the Chronicle is added". The baseline lists no lineages and p1's live role is `Chancellor`. The record lists p1's, p2's and p3's lineages, after any baseline lineages in baseline order and sorted by id, with `previousPlayer = Some(player)`, the live role, and empty legacies and starting advisers.
3. "a boxed lineage takes its role from `boxRoles`, keeping legacies and advisers". Its baseline entry has one legacy; `boxRoles` maps it to `Exile`. The record has role `Exile` and the legacy.
4. "every other field is the baseline's": `atlasBox`, `world`, `worldDeck`, `relicDeck`, `dispossessed`, `reliquary`, `foundations`.

End-to-end:
- From `ChronicleBeginSuite`'s `usurperWon`, start the Chronicle and answer every parked decision with `ChronicleWalk`. Assert:
  - the walk completes with `WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)`;
  - the recorded `ChronicleUpdated(c)` has p1's lineage `Chancellor`;
  - p1's lineage scores 3, since `ChronicleWalk` gives no gifts;
  - `c.oathkeeperGoal` differs from the baseline's;
  - `campaign.evolved == Some(c)`;
  - replaying every event reproduces the state.
- The redaction part of the spec's testing list (newcomers, retained adviser, unread World cards) belongs to World and Beacon. Sun and Throne decisions are public, so nothing to redact here.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleRecordSuite oathdigital.gameplay.chronicle.ChronicleSunThroneEndToEndSuite"`
Expected: FAIL.

- [ ] **Step 3: Implement**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.model._

/** The Chronicle as the tasks so far leave it (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores and the Oathkeeper goal come
  * from the live campaign. Sites, decks, the Reliquary and the Dispossessed
  * stay the baseline's until the Stars slice extracts them from the table:
  * moving a card in play does not yet change the record. */
object ChronicleRecord:
  def campaignFields(ready: ReadyGame): Chronicle =
    val campaign = ready.game.campaign
    val seatedBy = ready.game.current.players.map(p => p.lineage -> p.player).toMap
    val kept = campaign.baseline.lineages.map(lineage =>
      campaign.lineages.get(lineage.id).map(live => lineage.copy(role = live.role))
        .getOrElse(lineage.copy(role =
          campaign.boxRoles.getOrElse(lineage.id, lineage.role))))
    val known = kept.map(_.id).toSet
    val added = campaign.lineages.values.toVector.filterNot(l => known(l.id))
      .sortBy(_.id.value).map(live => LineageState(live.id,
        seatedBy.get(live.id), live.role, Vector.empty, Vector.empty))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal, lineages = kept ++ added)
```

A seated lineage already in the baseline keeps its baseline legacies and advisers: the live state has none yet (`GameStartRules` seats every lineage fresh). Say so in a comment.

`ChronicleFinishProcedure`: `ChronicleUpdated(ChronicleRecord.campaignFields(state))`. Update its doc comment.

- [ ] **Step 4: Docs**

- `docs/rules/chronicle.md`, Build Edifice "Digital:" note becomes: `each suit offers a random edifice from those not set aside, and the first set-aside edifice of that suit only when none is left. Buried edifices go to the edifice deck, and the step ends by returning the set-aside list to the deck; a built edifice is placed intact ([rulings 9 and 12](ambiguities.md#chronicle-rulings)).`
- `implementation-traceability.md`: the Sun and Throne rows say what is implemented (steps 1, 6, 7 and Throne 2-7) and what waits (Sun 2-5 for Legacies, Throne 1 for Citizenship, legacy points, map extraction for Stars).
- `game-setup.md`: a sentence that the Chronicle Phase now runs Sun and Throne, and that `ChronicleUpdated` carries roles, era and goal.
- `CONTEXT.md`: "Task holder" (the seated player who resolves a Chronicle task card) and "Set-aside edifices" (ruling 9).
- `ROADMAP.md`: under the Chronicle steps phase, a "Sun and Throne landed" sub-bullet.
- Run `python3 scripts/check-markdown-links.py`.

- [ ] **Step 5: Full verification**

Run, in order:

```bash
./sbtw test
python3 scripts/check-architecture.py
./sbtw coverage test coverageReport
```

Expected: all pass, and the coverage ratchet holds. If coverage drops, add tests for the uncovered branches the report names. Do not lower the ratchet.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/chronicle/ChronicleRecord.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleFinishProcedure.scala src/test/scala/oathdigital/gameplay/chronicle/ChronicleRecordSuite.scala src/test/scala/oathdigital/gameplay/chronicle/ChronicleSunThroneEndToEndSuite.scala docs/rules/chronicle.md docs/rules/implementation-traceability.md docs/architecture/game-setup.md CONTEXT.md docs/ROADMAP.md
git commit -m "feat(chronicle): the journaled Chronicle carries the Sun and Throne's roles, scores and Oath

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Self-review notes

- **Spec coverage.** Covered here:
  - Sun and Throne as triggered procedures chained by `ChronicleSequence` (Task 4).
  - A window per step and a fake-power seam test (4).
  - Empty windows for legacy and Citizenship steps (4, 5, 6).
  - Crown, with Empire state and the Scepter (5).
  - Task holders as seated players, including the double holder (5).
  - Shape Empire, Build Edifice with ruling 9, Stock and Scatter with recorded shuffles (6-8).
  - 3 points plus 2-for-1 gifts, and Swear an Oath (9).
  - `ChronicleUpdated` evolving roles, era and goal (10).
  - The Chancellor role codec fix (3).
- **Deferred by design:** World, Beacon, Stars, map/deck/Reliquary extraction, round trip, legacy points and goals, Citizenship offers, era end.
- **Names used across tasks:** `ShufflePile`, `setAsideEdifices`, `chronicleTasks`, `boxRoles`, `ChronicleTasks.{winner, holder, seated, playerOptions, chosenPlayer, failing, rules, noteId}`, `ChronicleWindows.{sun, throne, step}`, and `ChronicleWalk` (the test-side answering helper, created in Task 5 and extended in 6-9).
