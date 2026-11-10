# Chronicle World and Beacon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the Throne, the World task holder separates the Visions, gathers and deals the denizens, protects one faceup stack and keeps a starting adviser. The Beacon task holder then draws newcomers from three suits and keeps one of them as a starting adviser.

**Architecture:** Two new triggered procedures, `ChronicleWorld` and `ChronicleBeacon`, slot between `ChronicleThrone` and `ChronicleFinish` in `ChronicleSequence`. Each rule step is a `Sequence` node carrying its own power window, as the Sun and Throne steps do. Four new card piles hold the cards these steps move: the gathering pile (the nine dealt denizens), the newly Dispossessed pile (the six World dispossesses), a suited reserve per suit, and each lineage's starting advisers. One new primitive, `SuitedReserveFromBox`, brings a suit's boxed denizens into its live reserve before Beacon draws. Shape Foundation is an empty window. Extraction of these piles into the Chronicle stays in the Stars slice.

**Tech Stack:** Scala 3.9, munit 1.0.4, sbt via `./sbtw`, Scala.js frontend (unchanged here).

**Spec:** `docs/superpowers/specs/2026-10-27-chronicle-steps-design.md`. Rules: `docs/rules/chronicle.md` (tasks 3 and 4) and `docs/rules/ambiguities.md` "Chronicle rulings" 1-15. Previous plan: `docs/superpowers/plans/2026-10-27-chronicle-sun-and-throne.md` (merged at `951c7ae6`).

**Not in this plan:** Stars, extraction of map, decks, piles, starting advisers and the Reliquary into the Chronicle, the round trip through `GameStartRules`, Legacies and Foundations behaviour (Shape Foundation is an empty window), and Persistence.

## Global Constraints

- Branch and worktree off local `0.2.0-alpha`, never `main`. Merge back into `0.2.0-alpha`.
- Scalac runs with `-Werror`. A new sealed case breaks every exhaustive match; fix each one the compiler names.
- Production Scala files stay at or under 800 lines (`scripts/check-architecture.py`, `BackendArchitectureSuite`). `model` must not import `gameplay`, `application` or `serialization`. `gameplay` must not import `application` or `serialization`. `gameplay/walker/ProcedureWalker.scala` is at 797 lines: add nothing to it. `model/CoreOperations.scala` is at 778 lines: Task 1 moves `Location` out of it first.
- Tests run with `distantlands.notes.strict=true`: a log note key not declared by its writer throws.
- Tests never spell catalog ids (`BackendArchitectureSuite`). Use `CatalogNames`, `Table` helpers, or rules-owned constants such as `VisionRules.Conquest`.
- Old journals need not decode. New fields and operations are required on the wire.
- Test helper names state what they build. A doc comment does not excuse a vague name.
- Never `git add -A` (the worktree symlinks show as untracked). Add files by path.
- Commit trailer, exactly: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. A subagent that commits uses this trailer, not its own model's.
- Docs: normal prose. Run `python3 scripts/check-markdown-links.py` after doc edits.
- Rule questions not answered by `docs/rules/chronicle.md` or rulings 1-15 go to the user. Never pick a default.

## Rules this plan implements

The step list (CR pp. 38-39, rulings in brackets):

| Task | Step | Actor | This plan |
|---|---|---|---|
| World | 1 Separate Visions | none | real |
| World | 2 Gather Denizens | none | real |
| World | 3 Protect and Dispossess | World holder | real |
| Beacon | 1 Introduce Newcomers [15] | Beacon holder chooses suits; draws random | real |
| Beacon | 2 Welcome a Stranger | Beacon holder | real |
| Beacon | 3 Shape Foundation | Beacon holder | empty window |

Ruling 15 (user, 2026-10-28): if a chosen suit still has too few cards after the older Dispossessed return, draw what remains. The user notes this should never happen in play.

Digital accounting choices made here, inside the rules:

- **Visions leave play.** Separate Visions moves every Vision in play to the shared bank, the one place a card may leave the game (`OperationPipeline`). The Chronicle does not store Visions, and setup adds them again.
- **Advisers are discarded first.** Gather Denizens discards each adviser, actorless, to the Cradle discard pile before gathering the piles. `Discard.Denizen` returns the tokens on the card to their banks. Stars returns all pieces anyway (CR p. 39).
- **The gathering pile.** The nine dealt denizens sit in an ordered pile on the live state. Positions 0-2 and 3-5 are the two faceup stacks and positions 6-8 the facedown stack. The facedown stack is always the last three cards, also after the protected stack leaves, so every player may identify the cards before it. No view shows the pile; the protect decision names the faceup cards.
- **The newly Dispossessed pile.** The six unprotected denizens wait in the gathering pile while the holder chooses an adviser. When the step ends they move together to their own pile, not the Dispossessed, and one log line names them: the faceup three by name, the facedown three hidden. Beacon's return then takes "all Dispossessed cards, except the 6 the World task just added" without tracking which six. The Stars slice merges both piles into the Chronicle's Dispossessed.
- **Suited reserves come from the box.** Live suited reserves are empty at game start (`GameStartRules`). Before Beacon draws, `SuitedReserveFromBox` brings each chosen suit's catalog denizens that are nowhere in play into the live reserve. This follows the Grand Scepter precedent (`GrandScepterFromBox`, `OperationRun.unboxed`).
- **Drawn and kept cards use the temporary hand.** The protected stack and the newcomers go to the holder's temporary hand, which only that player can identify, as Setup's adviser choice does. The kept card goes facedown to the holder's lineage's starting advisers. The rest go to the world deck bottom.

## File Structure

Paths below are relative to the repo root. `M/` is `src/main/scala/distantlands/` and `T/` is `src/test/scala/distantlands/`.

- Create `M/model/Location.scala`: `Location` and its companion, moved unchanged from `CoreOperations.scala`, then four new cases.
- Modify `M/model/CoreOperations.scala`: lose `Location`; gain `SuitedReserveFromBox`.
- Modify `M/model/ActionValues.scala`: `ShufflePile.ChronicleGathering`, `ShufflePile.SuitedReserve`; `cards` reads the whole game.
- Modify `M/model/GameState.scala`: `CurrentGameState.chronicleGathering`, `CurrentGameState.newlyDispossessed`.
- Modify `M/model/CardIndex.scala`: `CardContainer.ChronicleGathering`, `CardContainer.NewlyDispossessed`.
- Modify `M/model/ProcedureRef.scala`: `ChronicleWorld`, `ChronicleBeacon`.
- Modify `M/model/PowerWindow.scala`: six Chronicle windows.
- Modify `M/model/OperationError.scala`: location names.
- Modify `M/gameplay/operations/`: `CardMovementOperations.scala`, `OperationStateWrites.scala`, `OperationStateAdapter.scala`, `CardKnowledgeMoves.scala`, `PileOperations.scala`, `ChronicleStateOperations.scala`, `OperationApplication.scala`, `OperationPipeline.scala`.
- Modify `M/gameplay/walker/WalkerShuffles.scala`.
- Create in `M/gameplay/chronicle/`: `ChronicleWorldProcedure.scala`, `SeparateVisions.scala`, `GatherDenizens.scala`, `ProtectAndDispossess.scala`, `ChronicleBeaconProcedure.scala`, `IntroduceNewcomers.scala`, `WelcomeStranger.scala`.
- Modify `M/gameplay/chronicle/ChronicleWindows.scala`, `ChronicleSequence.scala`, `M/gameplay/actions/ProcedureNotes.scala`, `M/gameplay/walker/WalkerProcedureRegistry.scala`.
- Modify `M/application/GamePresentationProjector.scala` (`identifiesCard`), `M/application/gamelog/ProcedureLog.scala`.
- Modify serialization: `M/serialization/WalkerValueCodec.scala`, `WalkerOperationCodec.scala`.
- Tests: named per task. Docs: `docs/rules/chronicle.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `CONTEXT.md`, `docs/ROADMAP.md`.

---

### Task 0: Worktree

**Files:** none (git only)

- [ ] **Step 1: Create the worktree from local `0.2.0-alpha`**

```bash
cd /Users/roman/projects/distantlands
git worktree add .worktrees/chronicle-world-beacon -b chronicle-world-beacon 0.2.0-alpha
ln -s /Users/roman/projects/distantlands/.tooling .worktrees/chronicle-world-beacon/.tooling
ln -s /Users/roman/projects/distantlands/node_modules .worktrees/chronicle-world-beacon/node_modules
```

- [ ] **Step 2: Confirm a green baseline**

Run (in the worktree, in the background; it takes about 15 minutes): `./sbtw test`
Expected: `Passed: Total 4459, Failed 0`.

---

### Task 1: The Chronicle card piles

Four new piles, each a `Location` a `Move` can reach, with the knowledge and identity rules the World and Beacon need.

**Files:**
- Create: `M/model/Location.scala`
- Modify: `M/model/CoreOperations.scala`, `M/model/GameState.scala`, `M/model/CardIndex.scala`, `M/model/OperationError.scala`
- Modify: `M/gameplay/operations/CardMovementOperations.scala`, `OperationStateWrites.scala`, `OperationStateAdapter.scala`, `CardKnowledgeMoves.scala`
- Modify: `M/application/GamePresentationProjector.scala`
- Modify: `M/serialization/WalkerValueCodec.scala`
- Test: `T/gameplay/operations/ChroniclePilesSuite.scala`, `T/application/ChroniclePilesIdentitySuite.scala`

**Interfaces:**
- Produces:
  - `Location.ChronicleGathering`, `Location.NewlyDispossessed`, `Location.SuitedReserve(suit: Suit)`, `Location.StartingAdvisers(lineage: LineageId)`.
  - `CurrentGameState.chronicleGathering: Vector[WorldCardId] = Vector.empty`, `CurrentGameState.newlyDispossessed: Vector[WorldCardId] = Vector.empty`.
  - `CardContainer.ChronicleGathering`, `CardContainer.NewlyDispossessed`. `CardContainer.SuitedReserve(suit)` and `CardContainer.Lineage(lineage, LineageCardArea.StartingAdvisers)` already exist.
  - Wire kinds: `chronicle-gathering`, `newly-dispossessed`, `suited-reserve` (with `suit`), `starting-advisers` (with `lineage`).

- [ ] **Step 1: Move `Location` into its own file**

Cut `sealed trait Location` and `object Location` (with `ownedBy`) from `CoreOperations.scala` into a new `M/model/Location.scala` in package `distantlands.model`, unchanged. Compile: `./sbtw compile`. Expected: success. Commit:

```bash
git add src/main/scala/distantlands/model/Location.scala src/main/scala/distantlands/model/CoreOperations.scala
git commit -m "refactor(model): move Location into its own file

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 2: Write the failing tests**

`T/gameplay/operations/ChroniclePilesSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.model._
import distantlands.testkit.{CatalogNames, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}

/** The Chronicle's card piles: the gathering pile, the newly Dispossessed,
  * the suited reserves and a lineage's starting advisers. */
class ChroniclePilesSuite extends munit.FunSuite:
  private def applied(table: Table, ops: CoreOperation*)(
      using munit.Location): ReadyGame =
    Outcomes.ready(OperationTestRun.applied(table.state, ops.toVector))

  private def topOfWorldDeck(table: Table): WorldCardId =
    table.ready.game.current.commonCards.worldDeck.head

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  private def moved(card: CardId, from: Location, to: Location,
      facing: Option[Orientation] = None): Move =
    Move(Piece.Card(card), PositionedLocation(from), PositionedLocation(to),
      resultingOrientation = facing)

  test("a world card moves into and out of the gathering pile"):
    val card = topOfWorldDeck(Table.start)
    val gathered = applied(Table.start,
      moved(card, Location.Deck(CardDeck.World), Location.ChronicleGathering))
    assertEquals(gathered.game.current.chronicleGathering, Vector(card))
    val back = applied(Table.from(gathered),
      moved(card, Location.ChronicleGathering, Location.Dispossessed))
    assertEquals(back.game.current.chronicleGathering, Vector.empty)
    assert(back.game.campaign.dispossessed.contains(card))

  test("a world card moves into the newly Dispossessed"):
    val card = topOfWorldDeck(Table.start)
    val after = applied(Table.start,
      moved(card, Location.Deck(CardDeck.World), Location.NewlyDispossessed))
    assertEquals(after.game.current.newlyDispossessed, Vector(card))

  test("a Dispossessed denizen returns to its suit's reserve and leaves it"):
    val card = Table.start.ready.game.campaign.dispossessed.collectFirst {
      case id: DenizenId => id }.get
    val suit = CatalogNames.suitOf(card)
    val reserved = applied(Table.start,
      moved(card, Location.Dispossessed, Location.SuitedReserve(suit)))
    assertEquals(reserved.game.campaign.suitedReserves.get(suit),
      Some(Vector(card)))
    val drawn = applied(Table.from(reserved),
      moved(card, Location.SuitedReserve(suit), Location.Hand(p1)))
    assertEquals(drawn.game.campaign.suitedReserves.get(suit),
      Some(Vector.empty))

  test("a denizen from a hand becomes a facedown starting adviser, which " +
      "its owner still knows"):
    val card = topOfWorldDeck(Table.start)
    val table = Table.start.hand(p2, card)
    val lineage = lineageOf(table.ready, p2)
    val after = applied(table, moved(card, Location.Hand(p2),
      Location.StartingAdvisers(lineage), Some(Orientation.FaceDown)))
    assertEquals(after.game.campaign.lineages(lineage).startingAdvisers,
      Vector(DenizenState(card.asInstanceOf[DenizenId], Orientation.FaceDown,
        Tokens.empty)))
    assert(after.knowledge.advisers.getOrElse(p2, Vector.empty)
      .contains(card))

  test("a starting adviser must be a denizen"):
    val relic = Table.start.ready.game.current.commonCards.relicDeck.head
    val lineage = lineageOf(Table.start.ready, p1)
    assert(OperationTestRun.applied(Table.start.state, Vector(moved(relic,
      Location.Deck(CardDeck.Relic), Location.StartingAdvisers(lineage))))
      .isLeft)
```

`OperationTestRun.applied` and `CatalogNames.suitOf` are the existing helpers if they exist under these names. Check first:

```bash
grep -rn "object OperationTestRun\|def suitOf" src/test/scala | head
```

If either is missing, add it in this task. `OperationTestRun.applied(state: OathState, ops: Vector[CoreOperation]): Either[OathViolation, OathState]` runs `OperationPipeline` on the ready state the way `ChronicleStateOperationsSuite` does; copy that suite's runner into `T/gameplay/operations/OperationTestRun.scala`. `CatalogNames.suitOf(id: DenizenId): Suit` reads `FirstGameSetupFixture.catalog.denizen(id).get.suit`; add it next to `CatalogNames.denizen` in `T/testkit/Table.scala`. Adjust the suite to the names you find.

`T/application/ChroniclePilesIdentitySuite.scala`:

```scala
package distantlands.application

import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Who may identify a card in the gathering pile or among a lineage's
  * starting advisers (Chronicle World and Beacon plan, Task 1). */
class ChroniclePilesIdentitySuite extends munit.FunSuite:
  private val presentation = GamePresentationProjector

  /** The quiet table with the top nine world cards dealt into the
    * gathering pile, in deck order. */
  private val nineGathered: Table = Table.start.update(ready =>
    val nine = ready.game.current.commonCards.worldDeck.take(9)
    ready.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.drop(9)),
      chronicleGathering = nine)))

  test("every player identifies the two faceup stacks, nobody the third"):
    val ready = nineGathered.ready
    val gathered = ready.game.current.chronicleGathering
    assert(gathered.take(6).forall(card =>
      presentation.identifiesAt(ready, Some(p1), card)))
    assert(gathered.drop(6).forall(card =>
      !presentation.identifiesAt(ready, Some(p2), card)))

  test("once a stack leaves, the facedown stack is still the hidden last three"):
    // The first stack goes back to the world deck, so every card is still
    // somewhere and the card index stays whole.
    val ready = nineGathered.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck ++ current.chronicleGathering.take(3)),
      chronicleGathering = current.chronicleGathering.drop(3)))).ready
    val six = ready.game.current.chronicleGathering
    assert(six.take(3).forall(card =>
      presentation.identifiesAt(ready, Some(p1), card)))
    assert(six.drop(3).forall(card =>
      !presentation.identifiesAt(ready, Some(p1), card)))

  test("only the lineage's player identifies a starting adviser"):
    val ready = Table.start.update(state =>
      val lineage = state.game.current.players.find(_.player == p2).get.lineage
      val card = state.game.current.commonCards.worldDeck.collectFirst {
        case id: DenizenId => id }.get
      state.updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck.filterNot(_ == card))))
        .updateCampaign(campaign => campaign.copy(lineages =
          campaign.lineages.updatedWith(lineage)(_.map(_.copy(
            startingAdvisers = Vector(DenizenState(card,
              Orientation.FaceDown, Tokens.empty))))))))
      .ready
    val lineage = ready.game.current.players.find(_.player == p2).get.lineage
    val card = ready.game.campaign.lineages(lineage).startingAdvisers.head.id
    assert(presentation.identifiesAt(ready, Some(p2), card))
    assert(!presentation.identifiesAt(ready, Some(p1), card))
```

If `GamePresentationProjector` is a class, not an object, build it the way `GamePresentationProjectorSuite` does.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChroniclePilesSuite distantlands.application.ChroniclePilesIdentitySuite"`
Expected: compile errors naming `Location.ChronicleGathering`, `chronicleGathering` and the other new names.

- [ ] **Step 4: Add the locations, state and containers**

In `M/model/Location.scala`, after `case object Dispossessed extends Location`:

```scala
  /** The nine denizens the World task deals, stack by stack (CR p. 38).
    * The facedown stack is always the last three; the cards before it
    * are faceup. */
  case object ChronicleGathering extends Location
  /** The six denizens the World task dispossessed, kept apart from the
    * older Dispossessed until the Chronicle is recorded (CR p. 39). */
  case object NewlyDispossessed extends Location
  /** A suit's section of the box (CR p. 39). */
  final case class SuitedReserve(suit: Suit) extends Location
  /** The facedown starting advisers a lineage keeps for the next game. */
  final case class StartingAdvisers(lineage: LineageId) extends Location
```

In `M/model/GameState.scala`, after `chronicleTasks`:

```scala
    // The World task's dealt denizens (CR p. 38), stack by stack. The
    // facedown stack stays last when the protected stack leaves.
    chronicleGathering: Vector[WorldCardId] = Vector.empty,
    // The six denizens the World task dispossessed. Beacon's return leaves
    // them alone (CR p. 39); the Stars slice records them as Dispossessed.
    newlyDispossessed: Vector[WorldCardId] = Vector.empty,
```

In `M/model/CardIndex.scala`, add `case object ChronicleGathering extends CardContainer` and `case object NewlyDispossessed extends CardContainer` after `Dispossessed`, and index both after `addIds(game.campaign.dispossessed, ...)`:

```scala
    addIds(game.current.chronicleGathering, CardContainer.ChronicleGathering)
    addIds(game.current.newlyDispossessed, CardContainer.NewlyDispossessed)
```

In `M/model/OperationError.scala`, name the new locations beside `case Location.Dispossessed => "Dispossessed"`:

```scala
    case Location.ChronicleGathering => "gathered denizens"
    case Location.NewlyDispossessed => "newly Dispossessed"
    case Location.SuitedReserve(suit) => s"${suit.key} reserve"
    case Location.StartingAdvisers(lineage) =>
      s"${lineage.value} starting advisers"
```

- [ ] **Step 5: Move cards in and out**

In `CardMovementOperations.cardDestinationViolation`, before `case _: Location.Site =>`:

```scala
      case Location.ChronicleGathering | Location.NewlyDispossessed =>
        if id.isInstanceOf[WorldCardId] then None
        else Some(InvalidDestination(transfer.piece, destination))
      case _: Location.SuitedReserve | _: Location.StartingAdvisers =>
        if id.isInstanceOf[DenizenId] then None
        else Some(InvalidDestination(transfer.piece, destination))
```

In the removal match (the one with `case CardContainer.Dispossessed => ...`), add:

```scala
    case CardContainer.ChronicleGathering => Right(ready.updateCurrent(current =>
      current.copy(chronicleGathering =
        current.chronicleGathering.filterNot(_ == located.id))))
    case CardContainer.NewlyDispossessed => Right(ready.updateCurrent(current =>
      current.copy(newlyDispossessed =
        current.newlyDispossessed.filterNot(_ == located.id))))
    case CardContainer.SuitedReserve(suit) => Right(ready.updateCampaign(
      campaign => campaign.copy(suitedReserves = campaign.suitedReserves
        .updated(suit, campaign.suitedReserves.getOrElse(suit, Vector.empty)
          .filterNot(_ == located.id)))))
```

In `insertCard`, beside `case Location.Dispossessed =>`:

```scala
      case Location.ChronicleGathering => ensureEmptyTokens(original.state, id)
        .map(_ => ready.updateCurrent(current => current.copy(
          chronicleGathering = current.chronicleGathering :+
            id.asInstanceOf[WorldCardId])))
      case Location.NewlyDispossessed => ensureEmptyTokens(original.state, id)
        .map(_ => ready.updateCurrent(current => current.copy(
          newlyDispossessed = current.newlyDispossessed :+
            id.asInstanceOf[WorldCardId])))
      case Location.SuitedReserve(suit) => ensureEmptyTokens(original.state, id)
        .map(_ => ready.updateCampaign(campaign => campaign.copy(
          suitedReserves = campaign.suitedReserves.updated(suit,
            campaign.suitedReserves.getOrElse(suit, Vector.empty) :+
              id.asInstanceOf[DenizenId]))))
      case Location.StartingAdvisers(lineage) =>
        ensureEmptyTokens(original.state, id).flatMap(_ =>
          ready.game.campaign.lineages.get(lineage).toRight(
            InvalidDestination(transfer.piece, transfer.to.location)).map(
            state => ready.updateCampaign(campaign => campaign.copy(
              lineages = campaign.lineages.updated(lineage, state.copy(
                startingAdvisers = state.startingAdvisers :+ DenizenState(
                  id.asInstanceOf[DenizenId],
                  transfer.resultingOrientation.getOrElse(
                    Orientation.FaceDown), Tokens.empty)))))))
```

If `ready.updateCampaign` is not visible here, use `ready.copy(game = ready.game.copy(campaign = ...))` as the `Dispossessed` arm does.

In `OperationStateWrites.semanticLocation`, before `case _: CardContainer.AtlasSite`:

```scala
    case CardContainer.ChronicleGathering => Location.ChronicleGathering
    case CardContainer.NewlyDispossessed => Location.NewlyDispossessed
    case CardContainer.SuitedReserve(suit) => Location.SuitedReserve(suit)
    case CardContainer.Lineage(lineage, LineageCardArea.StartingAdvisers) =>
      Location.StartingAdvisers(lineage)
```

In `OperationStateAdapter.matches`:

```scala
      case (CardContainer.ChronicleGathering, Location.ChronicleGathering) => true
      case (CardContainer.NewlyDispossessed, Location.NewlyDispossessed) => true
      case (CardContainer.SuitedReserve(left), Location.SuitedReserve(right)) =>
        left == right
      case (CardContainer.Lineage(left, LineageCardArea.StartingAdvisers),
          Location.StartingAdvisers(right)) => left == right
```

In `CardKnowledgeMoves.hidden`, extend the hidden list (a starting adviser is not hidden: its owner keeps knowing it):

```scala
    case Location.Deck(_) | Location.Reliquary | Location.SetAsideRelics |
        Location.SetAsideEdifices | Location.Dispossessed | Location.Atlas |
        Location.ChronicleGathering | Location.NewlyDispossessed |
        _: Location.SuitedReserve =>
      true
```

Update that object's doc comment: list the new piles beside "the dispossessed".

- [ ] **Step 6: Identify gathered and starting-adviser cards**

In `GamePresentationProjector.identifiesCard`, before `case _ => false`:

```scala
      // The World task deals two stacks faceup and one facedown (CR p. 38).
      // The facedown stack is the last three gathered cards, also after
      // the protected stack leaves; every card before it is on the table
      // for all to see.
      case CardContainer.ChronicleGathering =>
        val pile = ready.game.current.chronicleGathering
        val at = pile.indexOf(id)
        at >= 0 && at < pile.size - 3
      // A starting adviser is kept facedown for the lineage's next player.
      case CardContainer.Lineage(lineage, LineageCardArea.StartingAdvisers) =>
        viewer.exists(player => ready.game.current.players
          .exists(seat => seat.player == player && seat.lineage == lineage))
```

Add both clauses to the doc comment's list.

- [ ] **Step 7: Encode the locations**

In `WalkerValueCodec.encodeLocation`:

```scala
    case Location.ChronicleGathering => ujson.Obj("kind" -> "chronicle-gathering")
    case Location.NewlyDispossessed => ujson.Obj("kind" -> "newly-dispossessed")
    case Location.SuitedReserve(suit) => ujson.Obj("kind" -> "suited-reserve",
      "suit" -> suit.key)
    case Location.StartingAdvisers(lineage) => ujson.Obj(
      "kind" -> "starting-advisers", "lineage" -> lineage.value)
```

In `decodeLocation`:

```scala
    case "chronicle-gathering" => Right(Location.ChronicleGathering)
    case "newly-dispossessed" => Right(Location.NewlyDispossessed)
    case "suited-reserve" => decodeSuit(value("suit").str, s"$path.suit")
      .map(Location.SuitedReserve.apply)
    case "starting-advisers" =>
      Right(Location.StartingAdvisers(LineageId(value("lineage").str)))
```

Add one round-trip case per new location to the existing location codec suite (find it with `grep -rln "set-aside-edifices" src/test/scala`).

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChroniclePilesSuite distantlands.application.ChroniclePilesIdentitySuite"` and the codec suite.
Expected: PASS. Fix every non-exhaustive match the compiler names.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/model/Location.scala src/main/scala/distantlands/model/GameState.scala src/main/scala/distantlands/model/CardIndex.scala src/main/scala/distantlands/model/OperationError.scala src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala src/main/scala/distantlands/gameplay/operations/OperationStateAdapter.scala src/main/scala/distantlands/gameplay/operations/CardKnowledgeMoves.scala src/main/scala/distantlands/application/GamePresentationProjector.scala src/main/scala/distantlands/serialization/WalkerValueCodec.scala src/test/scala/distantlands/gameplay/operations/ChroniclePilesSuite.scala src/test/scala/distantlands/application/ChroniclePilesIdentitySuite.scala
# plus any helper and codec suite this task touched, by path
git commit -m "feat(chronicle): add the gathering, newly Dispossessed, suited reserve and starting adviser piles

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Shuffle the new piles and bring reserves from the box

**Files:**
- Modify: `M/model/ActionValues.scala`, `M/model/CoreOperations.scala`
- Modify: `M/gameplay/operations/PileOperations.scala`, `ChronicleStateOperations.scala`, `OperationApplication.scala`, `OperationPipeline.scala`
- Modify: `M/gameplay/walker/WalkerShuffles.scala`
- Modify: `M/serialization/WalkerOperationCodec.scala`
- Test: `T/gameplay/operations/ShuffleOperationSuite.scala`, `T/gameplay/operations/ChronicleStateOperationsSuite.scala`, the walker operation codec suite

**Interfaces:**
- Consumes: Task 1's piles.
- Produces:
  - `ShufflePile.ChronicleGathering`, `ShufflePile.SuitedReserve(suit: Suit)`.
  - `ShufflePile.cards(pile: ShufflePile, game: OathGame): Vector[CardId]` (was `current: CurrentGameState`).
  - `final case class SuitedReserveFromBox(suit: Suit, cards: Vector[DenizenId]) extends PrimitiveOperation`, `cards` non-empty. Wire kind `suited-reserve-from-box`.
  - `OperationRun.unboxed` includes its cards.

- [ ] **Step 1: Write the failing tests**

Add to `ShuffleOperationSuite` (follow its existing edifice-deck test; reuse its runner):

```scala
  test("the gathering pile takes a recorded order"):
    val nine = Table.start.ready.game.current.commonCards.worldDeck.take(3)
    val table = Table.start.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.drop(3)),
      chronicleGathering = nine)))
    val after = shuffled(table, ShufflePile.ChronicleGathering, nine.reverse)
    assertEquals(after.game.current.chronicleGathering, nine.reverse)

  test("a suited reserve takes a recorded order"):
    val dispossessed = Table.start.ready.game.campaign.dispossessed
      .collect { case id: DenizenId => id }
    val suit = CatalogNames.suitOf(dispossessed.head)
    val ofSuit = dispossessed.filter(CatalogNames.suitOf(_) == suit)
    val table = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      dispossessed = campaign.dispossessed.filterNot(ofSuit.contains),
      suitedReserves = Map(suit -> ofSuit))))
    val after = shuffled(table, ShufflePile.SuitedReserve(suit), ofSuit.reverse)
    assertEquals(after.game.campaign.suitedReserves(suit), ofSuit.reverse)
```

Name the suite's existing apply-a-shuffle helper `shuffled(table, pile, order): ReadyGame` if it is not already; keep the helper's existing name if it states what it builds.

Add to `ChronicleStateOperationsSuite`:

```scala
  test("boxed denizens of a suit join its live reserve"):
    val inPlay = Table.start.ready.game.cardIndex.toOption.get.ids
    val boxed = catalog.denizens.filter(_.suit == Suit.Beast)
      .map(denizen => DenizenId(denizen.id.value))
      .filterNot(inPlay.contains).take(2)
    val after = appliedReady(Table.start,
      SuitedReserveFromBox(Suit.Beast, boxed))
    assertEquals(after.game.campaign.suitedReserves.get(Suit.Beast),
      Some(boxed))

  test("a card in play cannot come from the box"):
    val inPlay = Table.start.ready.game.current.commonCards.worldDeck
      .collectFirst { case id: DenizenId => id }.get
    assert(applied(Table.start, SuitedReserveFromBox(
      CatalogNames.suitOf(inPlay), Vector(inPlay))).isLeft)

  test("a boxed card of another suit cannot join the reserve"):
    val inPlay = Table.start.ready.game.cardIndex.toOption.get.ids
    val arcane = catalog.denizens.filter(_.suit == Suit.Arcane)
      .map(denizen => DenizenId(denizen.id.value))
      .filterNot(inPlay.contains).head
    assert(applied(Table.start,
      SuitedReserveFromBox(Suit.Beast, Vector(arcane))).isLeft)
```

`applied` and `appliedReady` stand for the suite's existing runners (it already applies `GrandScepterFromBox` through the pipeline). If the catalog's denizen id type is not `DenizenId`, adapt the conversion the way the suite's other tests do.

Add a round-trip case for `SuitedReserveFromBox` and for both shuffle piles to the walker operation codec suite.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ShuffleOperationSuite distantlands.gameplay.operations.ChronicleStateOperationsSuite"`
Expected: compile errors naming `ShufflePile.ChronicleGathering` and `SuitedReserveFromBox`.

- [ ] **Step 3: Extend `ShufflePile`**

In `M/model/ActionValues.scala`:

```scala
sealed trait ShufflePile extends Product with Serializable
object ShufflePile:
  final case class Search(source: SearchSource) extends ShufflePile
  case object EdificeDeck extends ShufflePile
  case object SetAsideRelics extends ShufflePile
  case object ChronicleGathering extends ShufflePile
  final case class SuitedReserve(suit: Suit) extends ShufflePile

  /** The pile's cards in stored order. */
  def cards(pile: ShufflePile, game: OathGame): Vector[CardId] =
    pile match
      case Search(source) => game.current.commonCards.pile(source)
      case EdificeDeck => game.current.commonCards.edificeDeck
      case SetAsideRelics => game.current.setAsideRelics
      case ChronicleGathering => game.current.chronicleGathering
      case SuitedReserve(suit) =>
        game.campaign.suitedReserves.getOrElse(suit, Vector.empty)

  /** How a failure names the pile. */
  def name(pile: ShufflePile): String = pile match
    case Search(source) => SearchSource.name(source)
    case EdificeDeck => "edifice deck"
    case SetAsideRelics => "set-aside relics"
    case ChronicleGathering => "gathered denizens"
    case SuitedReserve(suit) => s"${suit.key} reserve"
```

Change both callers (`PileOperations.shuffle`, `WalkerShuffles`) to pass `ready.game` / `state.game`. In `PileOperations.withCards`, add:

```scala
      case ShufflePile.ChronicleGathering => current.copy(chronicleGathering =
        cards.collect { case id: WorldCardId => id })
```

`withCards` uses `ready.updateCurrent`; the reserve lives on the campaign, so restructure it to match on `pile` first:

```scala
  private def withCards(ready: ReadyGame, pile: ShufflePile,
      cards: Vector[CardId]): ReadyGame = pile match
    case ShufflePile.SuitedReserve(suit) => ready.updateCampaign(campaign =>
      campaign.copy(suitedReserves = campaign.suitedReserves.updated(suit,
        cards.collect { case id: DenizenId => id })))
    case other => ready.updateCurrent(current => other match
      // the existing Search, EdificeDeck and SetAsideRelics arms, unchanged
      case ShufflePile.ChronicleGathering => current.copy(chronicleGathering =
        cards.collect { case id: WorldCardId => id })
      case ShufflePile.SuitedReserve(_) => current)
```

- [ ] **Step 4: Add `SuitedReserveFromBox`**

In `M/model/CoreOperations.scala`, beside `GrandScepterFromBox`:

```scala
/** Brings a suit's boxed denizens into its live suited reserve, so the
  * Beacon can draw from them (CR p. 39). Live reserves start empty; the
  * box is every denizen the catalog has and the game does not. */
final case class SuitedReserveFromBox(suit: Suit, cards: Vector[DenizenId])
    extends PrimitiveOperation:
  require(cards.nonEmpty, "a suited reserve takes at least one boxed card")
```

In `ChronicleStateOperations`:

```scala
  /** Each card must be of `suit` and nowhere in play. A broken card index
    * counts as every card being absent; other checks reject that state. */
  def suitedReserveFromBox(ready: ReadyGame, suit: Suit,
      cards: Vector[DenizenId]): Either[OperationError, ReadyGame] =
    val inPlay = ready.game.cardIndex.toOption.fold(Set.empty[CardId])(_.ids)
    val wrongSuit = cards.filterNot(id =>
      ready.game.catalog.denizen(id).exists(_.suit == suit))
    if cards.exists(inPlay.contains) then Left(InvalidDescription(
      s"a card for the ${suit.key} reserve is already in play"))
    else if wrongSuit.nonEmpty then Left(InvalidDescription(
      s"${wrongSuit.head.value} is not a ${suit.key} denizen"))
    else Right(ready.updateCampaign(campaign => campaign.copy(
      suitedReserves = campaign.suitedReserves.updated(suit,
        campaign.suitedReserves.getOrElse(suit, Vector.empty) ++ cards))))
```

If `ready.game.catalog` does not exist, find how operations reach the catalog (`grep -n "catalog" src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`) and pass it in from `OperationApplication`.

In `OperationApplication.mutate`, beside the `GrandScepterFromBox` case:

```scala
      case (result, SuitedReserveFromBox(suit, cards)) =>
        result.flatMap(ChronicleStateOperations.suitedReserveFromBox(_, suit,
          cards))
```

Match the surrounding cases' exact shape (how they lift `OperationError`).

In `OperationPipeline.OperationRun.unboxed`:

```scala
  def unboxed(executed: Vector[CoreOperation]): Set[CardId] = executed.collect {
    case GrandScepterFromBox(_) => Vector(TheGrandScepterCard.id: CardId)
    case SuitedReserveFromBox(_, cards) => cards
  }.flatten.toSet
```

Keep the existing return type; adapt if `unboxed` is built differently.

- [ ] **Step 5: Encode the operation and piles**

In `WalkerOperationCodec`, add `SuitedReserveFromBox` to the `ChronicleOperation` union, then:

```scala
    case SuitedReserveFromBox(suit, cards) => ujson.Obj(
      "kind" -> "suited-reserve-from-box", "suit" -> suit.key,
      "cards" -> cards.map(card => ujson.Str(card.value)))
```

and in `decodeChronicleOperation`:

```scala
    case "suited-reserve-from-box" => for
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      cards = value("cards").arr.toVector.map(card => DenizenId(card.str))
      _ <- Either.cond(cards.nonEmpty, (), InvalidValue(s"$path.cards",
        "a suited reserve takes at least one boxed card"))
    yield SuitedReserveFromBox(suit, cards)
```

In `encodeShufflePile` / `decodeShufflePile`:

```scala
    case ShufflePile.ChronicleGathering => ujson.Obj("kind" -> "chronicle-gathering")
    case ShufflePile.SuitedReserve(suit) => ujson.Obj("kind" -> "suited-reserve",
      "suit" -> suit.key)
```

```scala
      case Some("chronicle-gathering") => Right(ShufflePile.ChronicleGathering)
      case Some("suited-reserve") => decodeSuit(value("suit").str,
        s"$path.suit").map(ShufflePile.SuitedReserve.apply)
```

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.operations.* distantlands.serialization.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/ActionValues.scala src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/operations/PileOperations.scala src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/operations/OperationPipeline.scala src/main/scala/distantlands/gameplay/walker/WalkerShuffles.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/test/scala/distantlands/gameplay/operations/ShuffleOperationSuite.scala src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala
# plus the codec suite, by path
git commit -m "feat(chronicle): shuffle the gathered denizens and suited reserves, and bring reserves from the box

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: World and Beacon skeletons with their windows

**Files:**
- Modify: `M/model/ProcedureRef.scala`, `M/model/PowerWindow.scala`
- Modify: `M/gameplay/chronicle/ChronicleWindows.scala`, `ChronicleSequence.scala`
- Create: `M/gameplay/chronicle/ChronicleWorldProcedure.scala`, `ChronicleBeaconProcedure.scala`
- Modify: `M/gameplay/walker/WalkerProcedureRegistry.scala`, `M/gameplay/actions/ProcedureNotes.scala`, `M/application/gamelog/ProcedureLog.scala`
- Test: `T/gameplay/chronicle/ChronicleWindowsSuite.scala`, `ChronicleBeginSuite.scala` (sequence), `EnumWireKeySuite`, `ProcedureRefSuite`, `GameLogPropertiesSuite`

**Interfaces:**
- Produces:
  - `TriggeredProcedureRef.ChronicleWorld("chronicle-world")`, `TriggeredProcedureRef.ChronicleBeacon("chronicle-beacon")`.
  - Windows `PowerWindow.ChronicleSeparateVisions` (`chronicle.world.separate-visions`), `ChronicleGatherDenizens` (`chronicle.world.gather-denizens`), `ChronicleProtectAndDispossess` (`chronicle.world.protect-and-dispossess`), `ChronicleIntroduceNewcomers` (`chronicle.beacon.introduce-newcomers`), `ChronicleWelcomeStranger` (`chronicle.beacon.welcome-stranger`), `ChronicleShapeFoundation` (`chronicle.beacon.shape-foundation`).
  - `ChronicleWindows.world`, `ChronicleWindows.beacon: Vector[PowerWindow]`.
  - `ChronicleWorldProcedure.build`, `ChronicleBeaconProcedure.build` with the Throne's signature, and `noteKeys: Vector[NoteKey]` on each.
  - Order: Begin, Sun, Throne, World, Beacon, Finish.

- [ ] **Step 1: Write the failing tests**

In `ChronicleWindowsSuite`, extend the first test to fold the power into `ChronicleWindows.sun ++ ChronicleWindows.throne ++ ChronicleWindows.world ++ ChronicleWindows.beacon` and rename it "a power folded into every Chronicle window runs once per window". Extend the key test:

```scala
  test("World and Beacon window keys stay as power data spells them"):
    assertEquals(
      (ChronicleWindows.world.map(_.key), ChronicleWindows.beacon.map(_.key)), (
      Vector("chronicle.world.separate-visions",
        "chronicle.world.gather-denizens",
        "chronicle.world.protect-and-dispossess"),
      Vector("chronicle.beacon.introduce-newcomers",
        "chronicle.beacon.welcome-stranger",
        "chronicle.beacon.shape-foundation")))
```

In the suite that tests `ChronicleSequence.after` (find it with `grep -rln "ChronicleSequence" src/test/scala`), assert:

```scala
    assertEquals(ChronicleSequence.after(TriggeredProcedureRef.ChronicleThrone),
      Some(TriggeredProcedureRef.ChronicleWorld))
    assertEquals(ChronicleSequence.after(TriggeredProcedureRef.ChronicleWorld),
      Some(TriggeredProcedureRef.ChronicleBeacon))
    assertEquals(ChronicleSequence.after(TriggeredProcedureRef.ChronicleBeacon),
      Some(TriggeredProcedureRef.ChronicleFinish))
```

Add the two refs to `EnumWireKeySuite` and `ProcedureRefSuite` where `ChronicleThrone` appears.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: compile errors naming `ChronicleWindows.world` and `ChronicleWorld`.

- [ ] **Step 3: Add refs, windows and skeletons**

`ProcedureRef.scala`, after `ChronicleThrone`:

```scala
  /** The World task, resolved by the People's Favor's task holder. */
  case ChronicleWorld extends TriggeredProcedureRef("chronicle-world")
  /** The Beacon task, resolved by the Darkest Secret's task holder. */
  case ChronicleBeacon extends TriggeredProcedureRef("chronicle-beacon")
```

Add both to `TriggeredProcedureRef.all` after `ChronicleThrone`.

`PowerWindow.scala`, after `ChronicleSwearOath`:

```scala
  case object ChronicleSeparateVisions extends OtherWindow:
    val key = "chronicle.world.separate-visions"
  case object ChronicleGatherDenizens extends OtherWindow:
    val key = "chronicle.world.gather-denizens"
  case object ChronicleProtectAndDispossess extends OtherWindow:
    val key = "chronicle.world.protect-and-dispossess"
  case object ChronicleIntroduceNewcomers extends OtherWindow:
    val key = "chronicle.beacon.introduce-newcomers"
  case object ChronicleWelcomeStranger extends OtherWindow:
    val key = "chronicle.beacon.welcome-stranger"
  case object ChronicleShapeFoundation extends OtherWindow:
    val key = "chronicle.beacon.shape-foundation"
```

Add them wherever the compiler or `PowerWindow`'s own `all` lists the Throne windows.

`ChronicleWindows.scala`:

```scala
  val world: Vector[PowerWindow] = Vector(ChronicleSeparateVisions,
    ChronicleGatherDenizens, ChronicleProtectAndDispossess)
  val beacon: Vector[PowerWindow] = Vector(ChronicleIntroduceNewcomers,
    ChronicleWelcomeStranger, ChronicleShapeFoundation)
```

`ChronicleSequence.order`:

```scala
    Vector(TriggeredProcedureRef.ChronicleBegin,
      TriggeredProcedureRef.ChronicleSun, TriggeredProcedureRef.ChronicleThrone,
      TriggeredProcedureRef.ChronicleWorld, TriggeredProcedureRef.ChronicleBeacon,
      TriggeredProcedureRef.ChronicleFinish)
```

Update its doc comment: drop "World, Beacon and Stars are later inserted"; say "Stars is later inserted between Beacon and Finish".

`ChronicleWorldProcedure.scala` (Task 4 fills the steps):

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** The World task (CR p. 38, `docs/rules/chronicle.md` task 3), resolved by
  * the player holding its card. Each step is its own power window. */
object ChronicleWorldProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps(catalog)),
      OathViolation.InvalidEventOrder("the World takes no start selection"))

  /** The notes the World's steps write. */
  def noteKeys: Vector[NoteKey] = Vector.empty

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleSeparateVisions, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleGatherDenizens, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleProtectAndDispossess,
      Vector.empty))
```

`ChronicleBeaconProcedure.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** The Beacon task (CR p. 39, `docs/rules/chronicle.md` task 4), resolved
  * by the player holding its card. Each step is its own power window. */
object ChronicleBeaconProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps(catalog)),
      OathViolation.InvalidEventOrder("the Beacon takes no start selection"))

  /** The notes the Beacon's steps write. */
  def noteKeys: Vector[NoteKey] = Vector.empty

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleIntroduceNewcomers,
      Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleWelcomeStranger, Vector.empty),
    // Shape Foundation (CR p. 39) waits for Foundations.
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))
```

If `-Werror` flags the unused `catalog` parameter of `steps`, drop it here and add it back in the task that uses it.

`WalkerProcedureRegistry`, after the Throne entry:

```scala
    TriggeredProcedureRef.ChronicleWorld -> Entry(
      rollDecisionId = None,
      modifierWindow = None,
      build = ChronicleWorldProcedure.build,
      rebuild = ChronicleWorldProcedure.build),
    TriggeredProcedureRef.ChronicleBeacon -> Entry(
      rollDecisionId = None,
      modifierWindow = None,
      build = ChronicleBeaconProcedure.build,
      rebuild = ChronicleBeaconProcedure.build),
```

`ProcedureNotes.keys`:

```scala
      case TriggeredProcedureRef.ChronicleWorld =>
        ChronicleWorldProcedure.noteKeys
      case TriggeredProcedureRef.ChronicleBeacon =>
        ChronicleBeaconProcedure.noteKeys
```

`ProcedureLog`, after the Throne entry (Tasks 4 and 5 fill the note maps):

```scala
    case TriggeredProcedureRef.ChronicleWorld => ProcedureLog(tag = None,
      lines = ActionLineSource.Notes(Map.empty),
      narratedPrefixes = Vector("chronicle.world."), isSection = false)
    case TriggeredProcedureRef.ChronicleBeacon => ProcedureLog(tag = None,
      lines = ActionLineSource.Notes(Map.empty),
      narratedPrefixes = Vector("chronicle.beacon."), isSection = false)
```

If `ActionLineSource.Notes` rejects an empty map, use `ActionLineSource.Silent("the World's notes land with its steps")` until Task 4. Add both refs to `GameLogPropertiesSuite`'s list of procedures that write notes once Tasks 4 and 5 give them notes; until then, follow whatever the suite requires for a silent procedure.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.model.* distantlands.application.gamelog.*"`
Expected: PASS. `ChronicleSunThroneEndToEndSuite` still passes: World and Beacon have no steps yet.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/ProcedureRef.scala src/main/scala/distantlands/model/PowerWindow.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleSequence.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleWorldProcedure.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala src/main/scala/distantlands/application/gamelog/ProcedureLog.scala
# plus each test file this task touched, by path
git commit -m "feat(chronicle): chain World and Beacon after the Throne, each step in its own window

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The World

**Files:**
- Create: `M/gameplay/chronicle/SeparateVisions.scala`, `GatherDenizens.scala`, `ProtectAndDispossess.scala`
- Modify: `M/gameplay/chronicle/ChronicleWorldProcedure.scala`, `M/application/gamelog/ProcedureLog.scala`
- Modify: `T/gameplay/chronicle/ChronicleWalk.scala` (partition answers)
- Test: `T/gameplay/chronicle/WorldTaskSuite.scala`

**Interfaces:**
- Consumes: Task 1's piles, Task 2's `ShufflePile.ChronicleGathering`, Task 3's skeleton, `ChronicleTasks.forHolder`, `ChronicleTasks.lineageOf`.
- Produces:
  - `SeparateVisions.steps: Vector[Operation]`.
  - `GatherDenizens.steps(catalog: ExecutableCatalog): Vector[Operation]`; `GatherDenizens.dealt: Int = 9`.
  - `ProtectAndDispossess.protectDecisionId = "chronicle.world.protect"`, `keepDecisionId = "chronicle.world.keep"`, `keepKey = "keep"`, `deckKey = "world-deck"`, `steps: Vector[Operation]`.
  - Notes (on `ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld)`): `GatherDenizens.gathered`, `ProtectAndDispossess.protectedStack`, `ProtectAndDispossess.keptAdviser`, `ProtectAndDispossess.dispossessed`.
  - `ChronicleWalk.plainestAnswer` answers a `Partition`.

- [ ] **Step 1: Teach `ChronicleWalk` to answer a partition**

In `plainestAnswer`, before `case other =>`:

```scala
      // Each section takes its minimum in option order; the rest go to the
      // first section with no maximum.
      case split: DecisionQuery.Partition =>
        val available = split.options.filter(_.available).map(_.ref)
        val (placed, rest) = split.sections.foldLeft(
          (Vector.empty[DecisionPlacement], available)) {
          case ((done, left), section) =>
            (done ++ left.take(section.minRequired).map(
              DecisionPlacement(_, section.key)), left.drop(section.minRequired))
        }
        val overflow = split.sections.find(_.maxAllowed.isEmpty).get.key
        DecisionAnswer.PartitionAnswer(
          placed ++ rest.map(DecisionPlacement(_, overflow)))
```

Update the doc comment: "and, in a partition, each section's minimum".

- [ ] **Step 2: Write the failing tests**

`T/gameplay/chronicle/WorldTaskSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.application.GamePresentationProjector
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, PartitionAnswer}
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The World task (CR p. 38). p1 is the Chancellor; p2 holds the World. */
class WorldTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val tasksAssigned: Table = ChronicleWalk.throneHeldByChancellorP1

  /** The walk from the end of the Throne: the driver starts the World. */
  private def worldStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleThrone))

  private def parkedOnProtect(table: Table)(using munit.Location)
      : OathTransition = ChronicleWalk.walkedTo(rules, worldStarted(table),
    ProtectAndDispossess.protectDecisionId)

  private def gathered(transition: OathTransition): Vector[WorldCardId] =
    Outcomes.ready(transition.state).game.current.chronicleGathering

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  test("every Vision in play leaves it"):
    val table = tasksAssigned.revealedVision(p1, VisionRules.Conquest)
      .discarded(Region.Cradle, VisionRules.Faith)
    val after = Outcomes.ready(parkedOnProtect(table).state)
    val ids = after.game.cardIndex.toOption.get.ids
    assert(!ids.contains(VisionRules.Conquest))
    assert(!ids.contains(VisionRules.Faith))

  test("advisers and discards are gathered; nine are dealt and the rest " +
      "go to the world deck"):
    val deck = tasksAssigned.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }
    val discards = deck.take(8)
    val table = tasksAssigned.discarded(Region.Provinces, discards*)
      .adviser(p1, deck(8)).adviser(p2, deck(9))
    val walked = parkedOnProtect(table)
    val ready = Outcomes.ready(walked.state)
    val pooled = (discards :+ deck(8) :+ deck(9)).toSet[WorldCardId]
    assertEquals(gathered(walked).size, 9)
    assert(gathered(walked).forall(pooled.contains))
    val leftOver = pooled -- gathered(walked)
    assert(leftOver.forall(ready.game.current.commonCards.worldDeck.contains))
    assert(ready.game.current.players.forall(_.advisers.isEmpty))
    assert(Region.all.forall(region =>
      ready.game.current.commonCards.discard(region).isEmpty))

  test("with fewer than nine, the difference comes from the world deck top"):
    val top = tasksAssigned.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }
    val table = tasksAssigned.discarded(Region.Cradle, top.slice(20, 24)*)
    val walked = parkedOnProtect(table)
    assertEquals(gathered(walked).toSet,
      (top.slice(20, 24) ++ top.take(5)).toSet[WorldCardId])

  test("the World holder protects a faceup stack, keeps one secretly and " +
      "the other six are dispossessed"):
    val walked = parkedOnProtect(tasksAssigned)
    val decide = ChronicleWalk.parked(walked.state).get
    assertEquals(decide.owner, p2)
    val nine = gathered(walked)
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), nine.take(6).map(card =>
          DecisionOptionRef.Denizen(card.asInstanceOf[DenizenId])))
      case other => fail(other.toString)
    val second = nine.slice(3, 6)
    val protectedState = accepted(rules.resolveWalker(walked.state, p2,
      ProtectAndDispossess.protectDecisionId, ChooseOneAnswer(
        DecisionOptionRef.Denizen(second(1).asInstanceOf[DenizenId]))))
    val ready = Outcomes.ready(protectedState.state)
    assertEquals(ready.game.current.temporaryHands.get(p2), Some(second))
    // The other six wait in the pile, the facedown stack still last and
    // still unknown to p1.
    assertEquals(ready.game.current.chronicleGathering,
      nine.take(3) ++ nine.drop(6))
    assert(nine.take(3).forall(card =>
      GamePresentationProjector.identifiesAt(ready, Some(p1), card)))
    assert(nine.drop(6).forall(card =>
      !GamePresentationProjector.identifiesAt(ready, Some(p1), card)))
    val kept = second.last
    val done = accepted(rules.resolveWalker(protectedState.state, p2,
      ProtectAndDispossess.keepDecisionId, PartitionAnswer(second.map(card =>
        DecisionPlacement(DecisionOptionRef.Denizen(
          card.asInstanceOf[DenizenId]),
          if card == kept then ProtectAndDispossess.keepKey
          else ProtectAndDispossess.deckKey)))))
    val after = Outcomes.ready(done.state)
    assertEquals(after.game.campaign.lineages(lineageOf(after, p2))
      .startingAdvisers.map(_.id), Vector(kept))
    assertEquals(after.game.current.commonCards.worldDeck.takeRight(2).toSet,
      second.init.toSet)
    assertEquals(after.game.current.temporaryHands.getOrElse(p2, Vector.empty),
      Vector.empty)
    assertEquals(after.game.current.chronicleGathering, Vector.empty)
    assertEquals(after.game.current.newlyDispossessed,
      nine.take(3) ++ nine.drop(6))
    assert(GamePresentationProjector.identifiesAt(after, Some(p2), kept))
    assert(!GamePresentationProjector.identifiesAt(after, Some(p1), kept))

  test("other seats see the faceup stacks, never the third"):
    val walked = parkedOnProtect(tasksAssigned)
    val ready = Outcomes.ready(walked.state)
    val nine = gathered(walked)
    assert(nine.take(6).forall(card =>
      GamePresentationProjector.identifiesAt(ready, Some(p1), card)))
    assert(nine.drop(6).forall(card =>
      !GamePresentationProjector.identifiesAt(ready, Some(p1), card)))
```

Add one Game Log test in the shape `ChronicleSunSuite` uses for its lines (find its log assertion with `grep -n "lines\|LogLine" src/test/scala/distantlands/gameplay/chronicle/ChronicleSunSuite.scala`): after the walk, p2's log reads "kept {card} as a starting adviser" with the card's name, and p1's log reads the same line with the card hidden. A second line closes the step: "{cards} were dispossessed". p1 sees the three faceup cards by name and the three facedown ones hidden.

If `discarded` cannot take a Vision, put the Vision in the discard with `update(_.updateCurrent(...))` instead.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.WorldTaskSuite"`
Expected: compile errors naming `ProtectAndDispossess`.

- [ ] **Step 4: Separate Visions**

`M/gameplay/chronicle/SeparateVisions.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.model._

/** Separate Visions, the World's first step (CR p. 38): every Vision in
  * play leaves it. The Chronicle does not store Visions, and setup adds all
  * five again. */
object SeparateVisions:
  val steps: Vector[Operation] = Vector(BuildOps((state, _) =>
    state.game.cardIndex.left.map(problems => OathViolation.InvalidEventOrder(
      s"the card index is broken: $problems")).flatMap(index =>
      index.ids.toVector.collect { case id: VisionId => id }
        .sortBy(_.value).traverse(vision =>
          index.locationOf(vision).flatMap(at => from(at.container))
            .toRight(OathViolation.InvalidEventOrder(
              s"${vision.value} lies where a Vision cannot be moved from"))
            .map(location => Move(Piece.Card(vision),
              PositionedLocation(location),
              PositionedLocation(Location.SharedBank))))),
    required = true))

  private def from(container: CardContainer): Option[Location] =
    container match
      case CardContainer.Deck(deck) => Some(Location.Deck(deck))
      case CardContainer.RegionalDiscard(region) =>
        Some(Location.RegionalDiscard(region))
      case CardContainer.Player(player, PlayerCardArea.Hand) =>
        Some(Location.Hand(player))
      case CardContainer.Player(player, _) => Some(Location.PlayArea(player))
      case CardContainer.Dispossessed => Some(Location.Dispossessed)
      case _ => None
```

If `cardIndex` is not an `Either` with a printable left, or the codebase has no `traverse`, write the fold the way `ReliquaryAndRelics.scatter` collects its moves.

- [ ] **Step 5: Gather Denizens**

`M/gameplay/chronicle/GatherDenizens.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Gather Denizens, the World's second step (CR p. 38). Every adviser is
  * discarded to the Cradle first, so `Discard.Denizen` returns its tokens.
  * Then every discard pile goes into the gathering pile, which is shuffled.
  * All but nine go to the world deck; with fewer than nine, the world deck
  * top makes up the difference. */
object GatherDenizens:
  val dealt: Int = 9

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld)
  private val notes = NoteSet(noteId)
  val gathered: NoteKey = notes.key("gathered", Vector(
    NotePart.Arg(0, NoteUnit.Card), NotePart.Text(" were gathered")))
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    BuildOps((state, _) => Right(discardAdvisers(catalog, state)),
      required = true),
    BuildOps((state, _) => Right(gatherDiscards(state)), required = true),
    Note(noteId, states => Some(gathered.sourceless(NoteArg.Count(
      states.now.game.current.chronicleGathering.size)))),
    Shuffle(ShufflePile.ChronicleGathering),
    BuildOps((state, _) => Right(deal(state)), required = true))

  private def discardAdvisers(catalog: ExecutableCatalog, state: ReadyGame)
      : Vector[CoreOperation] =
    state.game.current.players.flatMap(player => player.advisers.collect {
      case DenizenState(id, _, tokens) => catalog.denizen(id).map(denizen =>
        Discard.Denizen(id, PositionedLocation(Location.PlayArea(player.player)),
          Region.Cradle, denizen.suit, tokens.favor, tokens.secrets,
          player.player, required = true, actorless = true))
    }.flatten)

  private def gatherDiscards(state: ReadyGame): Vector[CoreOperation] =
    Region.all.flatMap(region =>
      state.game.current.commonCards.discard(region).map(card =>
        Move(Piece.Card(card),
          PositionedLocation(Location.RegionalDiscard(region)),
          PositionedLocation(Location.ChronicleGathering))))

  /** The cards past the ninth go to the world deck bottom, unseen; a short
    * pile takes the world deck's top cards. */
  private def deal(state: ReadyGame): Vector[CoreOperation] =
    val pile = state.game.current.chronicleGathering
    val surplus = pile.drop(dealt).map(card => Move(Piece.Card(card),
      PositionedLocation(Location.ChronicleGathering),
      PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Bottom)))
    val shortfall = state.game.current.commonCards.worldDeck
      .take(dealt - pile.size.min(dealt)).map(card => Move(Piece.Card(card),
        PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Top),
        PositionedLocation(Location.ChronicleGathering)))
    surplus ++ shortfall
```

The note's parts and arg types must match what `NoteSet` supports. Read `ShapeEmpire.discarded` and `LineageScoring.scored` and copy their shape for a count: if there is no `NoteArg.Count`, use the same arg `LineageScoring` uses for its point count with a plain unit. Word the line "{n} denizens were gathered". If `Shuffle` takes an `order` argument with no default, write `Shuffle(ShufflePile.ChronicleGathering, None)`. A `Shuffle` must be a leaf of its own, as it is here.

- [ ] **Step 6: Protect and Dispossess**

`M/gameplay/chronicle/ProtectAndDispossess.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Protect and Dispossess, the World's third step (CR p. 38). The World
  * task holder protects one of the two faceup stacks by naming a card in
  * it; the stack goes to their temporary hand. They keep one secretly as a
  * facedown starting adviser and the other two go to the world deck bottom.
  * The other six gathered cards wait in the gathering pile until the step
  * ends, then are newly Dispossessed with one log line. Nobody knows the
  * facedown three, so the line names only the faceup ones. */
object ProtectAndDispossess:
  val protectDecisionId: String = "chronicle.world.protect"
  val keepDecisionId: String = "chronicle.world.keep"
  val keepKey: String = "keep"
  val deckKey: String = "world-deck"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld)
  private val notes = NoteSet(noteId)
  val protectedStack: NoteKey = notes.key("protected", Vector(
    NotePart.Arg(0), NotePart.Text(" protected the stack of "),
    NotePart.Arg(1)))
  val keptAdviser: NoteKey = notes.key("kept-adviser", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" as a starting adviser")))
  val dispossessed: NoteKey = notes.key("dispossessed", Vector(
    NotePart.Arg(0), NotePart.Text(" were dispossessed")))
  def noteKeys: Vector[NoteKey] = notes.declared

  private val stackSize = 3

  val steps: Vector[Operation] = Vector(
    Branch((state, _) => ChronicleTasks.forHolder(state, ChronicleTask.World)(
      holder => protect(state, holder))),
    Branch((state, _) => ChronicleTasks.forHolder(state, ChronicleTask.World)(
      holder => keep(state, holder))),
    // The note reads the pile before the move, while the faceup cards are
    // still identifiable where they lie.
    Note(noteId, states => Option.when(
      states.now.game.current.chronicleGathering.nonEmpty)(
      dispossessed.sourceless(NoteArg.Cards(
        states.now.game.current.chronicleGathering)))),
    BuildOps((state, _) => Right(state.game.current.chronicleGathering.map(
      card => Move(Piece.Card(card),
        PositionedLocation(Location.ChronicleGathering),
        PositionedLocation(Location.NewlyDispossessed)))), required = true))

  private def faceup(state: ReadyGame): Vector[DenizenId] =
    state.game.current.chronicleGathering.take(2 * stackSize)
      .collect { case id: DenizenId => id }

  private def protect(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    if faceup(state).isEmpty then Vector.empty
    else Vector(
      Decide(protectDecisionId, holder, DecisionQuery.ChooseOne(
        faceup(state).map(card =>
          DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
        heading = Some("Protect a stack: choose any card in it"),
        confirmVerb = Some("Protect"))),
      BuildOps((live, pending) => chosen(pending).map(card =>
        split(live, holder, card)), required = true),
      Note(noteId, states => states.previous.flatMap((_, after) =>
        after.game.current.temporaryHands.get(holder).map(cards =>
          protectedStack.sourceless(NoteArg.Player(holder),
            NoteArg.Cards(cards))))))

  private def chosen(pending: PendingTree): Either[OathViolation, DenizenId] =
    PowerAnswers.one(pending, protectDecisionId).collect {
      case DecisionOptionRef.Denizen(card) => card
    }.toRight(PowerAnswers.missing(protectDecisionId))

  /** The protected stack to the holder's hand. The rest stay in the
    * gathering pile, the facedown stack still last. */
  private def split(state: ReadyGame, holder: PlayerId, card: DenizenId)
      : Vector[CoreOperation] =
    val pile = state.game.current.chronicleGathering
    val stack = pile.indexOf(card) / stackSize
    pile.zipWithIndex.collect { case (id, at) if at / stackSize == stack =>
      Move(Piece.Card(id), PositionedLocation(Location.ChronicleGathering),
        PositionedLocation(Location.Hand(holder)))
    }

  private def keep(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    val hand = state.game.current.temporaryHands.getOrElse(holder, Vector.empty)
      .collect { case id: DenizenId => id }
    ChronicleTasks.lineageOf(state, holder) match
      case _ if hand.isEmpty => Vector.empty
      case None => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${holder.value} has no lineage")))
      case Some(lineage) => StartingAdviserChoice.steps(holder, lineage, hand,
        keepDecisionId, keepKey, deckKey, "Keep a starting adviser",
        keptAdviser, noteId)
```

The keep decision is shared with Beacon's Welcome a Stranger, so it lives in its own object. Create `M/gameplay/chronicle/StartingAdviserChoice.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.model._

/** Keeping one card of a temporary hand as a facedown starting adviser,
  * the rest going to the world deck bottom: the World's Protect and the
  * Beacon's Welcome a Stranger (CR pp. 38-39). The choice is the holder's
  * alone, as Setup's adviser choice is. */
object StartingAdviserChoice:
  def steps(holder: PlayerId, lineage: LineageId, hand: Vector[DenizenId],
      decisionId: String, keepKey: String, deckKey: String, heading: String,
      kept: NoteKey, noteId: String): Vector[Operation] = Vector(
    Decide(decisionId, holder, DecisionQuery.Partition(
      Vector(DecisionSection(keepKey, "Keep", 1, Some(1)),
        DecisionSection(deckKey, "World deck", 0)),
      hand.map(card => DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
      heading = Some(heading), confirmLabel = Some("Confirm Adviser"))),
    BuildOps((_, pending) => placements(pending, decisionId).map(_.map {
      case (card, section) if section == keepKey => Move(Piece.Card(card),
        PositionedLocation(Location.Hand(holder)),
        PositionedLocation(Location.StartingAdvisers(lineage)),
        resultingOrientation = Some(Orientation.FaceDown))
      case (card, _) => Move(Piece.Card(card),
        PositionedLocation(Location.Hand(holder)),
        PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Bottom))
    }), required = true),
    Note(noteId, states => states.answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.PartitionAnswer(placed), _) =>
        placed.collectFirst {
          case DecisionPlacement(DecisionOptionRef.Denizen(card), `keepKey`) =>
            kept.sourceless(NoteArg.Player(holder), NoteArg.Card(card))
        }
    }.flatten))

  private def placements(pending: PendingTree, decisionId: String)
      : Either[OathViolation, Vector[(DenizenId, String)]] =
    pending.answered.find(_.decisionId == decisionId).map(_.answer) match
      case Some(DecisionAnswer.PartitionAnswer(placed)) => Right(placed.collect {
        case DecisionPlacement(DecisionOptionRef.Denizen(card), section) =>
          (card, section)
      })
      case _ => Left(OathViolation.InvalidEventOrder(
        s"no starting adviser answer is recorded for $decisionId"))
```

Use the `noteId` type `ProcedureNotes.id` returns if it is not `String`. Add `StartingAdviserChoice.scala` to this task's commit.

- [ ] **Step 7: Wire the World**

In `ChronicleWorldProcedure`:

```scala
  def noteKeys: Vector[NoteKey] =
    GatherDenizens.noteKeys ++ ProtectAndDispossess.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleSeparateVisions,
      SeparateVisions.steps),
    ChronicleWindows.step(PowerWindow.ChronicleGatherDenizens,
      GatherDenizens.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleProtectAndDispossess,
      ProtectAndDispossess.steps))
```

In `ProcedureLog`'s World entry:

```scala
      lines = ActionLineSource.Notes(Map(
        GatherDenizens.gathered -> LogKind.Delta,
        ProtectAndDispossess.protectedStack -> LogKind.Decision,
        ProtectAndDispossess.keptAdviser -> LogKind.Decision,
        ProtectAndDispossess.dispossessed -> LogKind.Delta)),
```

Add `ChronicleWorld` to `GameLogPropertiesSuite`'s list of procedures that write notes. Regenerate the note snapshot: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly *NoteKeys*"`, then check the diff shows only the three World keys.

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.application.gamelog.*"`
Expected: PASS. If `ChronicleSunThroneEndToEndSuite` now fails because p2 or p3 holds no lineage or the walk answers a partition differently, fix the cause, not the test.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/SeparateVisions.scala src/main/scala/distantlands/gameplay/chronicle/GatherDenizens.scala src/main/scala/distantlands/gameplay/chronicle/ProtectAndDispossess.scala src/main/scala/distantlands/gameplay/chronicle/StartingAdviserChoice.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleWorldProcedure.scala src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala src/test/scala/distantlands/gameplay/chronicle/WorldTaskSuite.scala src/test/resources/notes/declared-keys.txt
# plus GameLogPropertiesSuite, by path
git commit -m "feat(chronicle): the World separates Visions, deals nine denizens and keeps a starting adviser

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The Beacon

**Files:**
- Create: `M/gameplay/chronicle/IntroduceNewcomers.scala`, `WelcomeStranger.scala`
- Modify: `M/gameplay/chronicle/ChronicleBeaconProcedure.scala`, `M/application/gamelog/ProcedureLog.scala`
- Test: `T/gameplay/chronicle/BeaconTaskSuite.scala`

**Interfaces:**
- Consumes: Tasks 1-4, `StartingAdviserChoice.steps`.
- Produces:
  - `IntroduceNewcomers.suitDecisionId(draws: Int): String` = `s"chronicle.beacon.suit.$draws"`, with `draws` 3, 2, 1 in that order; `IntroduceNewcomers.steps(catalog: ExecutableCatalog): Vector[Operation]`.
  - `WelcomeStranger.keepDecisionId = "chronicle.beacon.keep"`, `keepKey = "keep"`, `deckKey = "world-deck"`, `steps: Vector[Operation]`.
  - Notes: `IntroduceNewcomers.choseSuits`, `IntroduceNewcomers.returned`, `WelcomeStranger.keptAdviser`.

- [ ] **Step 1: Write the failing tests**

`T/gameplay/chronicle/BeaconTaskSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{CatalogNames, Outcomes, Table}
import distantlands.testkit.Table.p3
import distantlands.testkit.Unwrap.accepted

/** The Beacon task (CR p. 39). p3 holds the Beacon. */
class BeaconTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val tasksAssigned: Table = ChronicleWalk.throneHeldByChancellorP1

  /** The walk from the end of the World: the driver starts the Beacon. */
  private def beaconStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleWorld))

  private def suitsChosen(table: Table, suits: Vector[Suit])(
      using munit.Location): OathTransition =
    suits.zip(Vector(3, 2, 1)).foldLeft(beaconStarted(table)) {
      case (step, (suit, draws)) =>
        val id = IntroduceNewcomers.suitDecisionId(draws)
        val at = ChronicleWalk.walkedTo(rules, step, id)
        accepted(rules.resolveWalker(at.state, p3, id,
          ChooseOneAnswer(DecisionOptionRef.FavorBank(suit))))
    }

  private def hand(transition: OathTransition): Vector[WorldCardId] =
    Outcomes.ready(transition.state).game.current.temporaryHands
      .getOrElse(p3, Vector.empty)

  private def boxed(ready: ReadyGame, suit: Suit): Vector[DenizenId] =
    val inPlay = ready.game.cardIndex.toOption.get.ids
    catalog.denizens.filter(_.suit == suit)
      .map(denizen => DenizenId(denizen.id.value))
      .filterNot(inPlay.contains)

  test("each suit choice offers only suits not yet chosen"):
    val first = ChronicleWalk.walkedTo(rules, beaconStarted(tasksAssigned),
      IntroduceNewcomers.suitDecisionId(3))
    val picked = accepted(rules.resolveWalker(first.state, p3,
      IntroduceNewcomers.suitDecisionId(3),
      ChooseOneAnswer(DecisionOptionRef.FavorBank(Suit.Beast))))
    val second = ChronicleWalk.parked(picked.state).get
    assertEquals(second.decisionId, IntroduceNewcomers.suitDecisionId(2))
    second.query match
      case one: DecisionQuery.ChooseOne => assertEquals(one.options.map(_.ref),
        Suit.all.filterNot(_ == Suit.Beast).map(DecisionOptionRef.FavorBank(_)))
      case other => fail(other.toString)

  test("the holder draws three, two and one boxed denizens of the chosen " +
      "suits"):
    val before = tasksAssigned.ready
    val drawn = hand(suitsChosen(tasksAssigned,
      Vector(Suit.Beast, Suit.Arcane, Suit.Order)))
    assertEquals(drawn.size, 6)
    assertEquals(drawn.map(card =>
      CatalogNames.suitOf(card.asInstanceOf[DenizenId])).groupMapReduce(
      identity)(_ => 1)(_ + _),
      Map(Suit.Beast -> 3, Suit.Arcane -> 2, Suit.Order -> 1))
    assert(drawn.forall(card => Suit.all.exists(suit =>
      boxed(before, suit).contains(card))))

  test("a short suit first takes back the older Dispossessed, never the " +
      "newly Dispossessed"):
    val shortSuit = Suit.Nomad
    val newly = tasksAssigned.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }.take(6)
    // Every boxed Nomad but two is put in play at the world deck bottom, so
    // the Nomad box holds two and the three draws need the Dispossessed.
    val table = tasksAssigned.update(ready =>
      val spare = boxed(ready, shortSuit).drop(2)
      ready.updateCurrent(current => current.copy(
        commonCards = current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck.filterNot(newly.contains) ++ spare),
        newlyDispossessed = newly)))
    val older = table.ready.game.campaign.dispossessed
    val after = Outcomes.ready(suitsChosen(table,
      Vector(shortSuit, Suit.Arcane, Suit.Order)).state)
    assertEquals(after.game.campaign.dispossessed, Vector.empty)
    assertEquals(after.game.current.newlyDispossessed, newly)
    assert(older.forall(card =>
      after.game.campaign.suitedReserves.values.exists(_.contains(card)) ||
        after.game.current.temporaryHands.getOrElse(p3, Vector.empty)
          .contains(card)))

  test("a suit still short after the return draws what remains (ruling 15)"):
    // Every boxed Nomad but one, and every Dispossessed Nomad, goes to the
    // world deck bottom: the Nomad box keeps one and the return adds none.
    // Taking a card out of the state instead would put it back in the box.
    val table = tasksAssigned.update(ready =>
      val nomads = ready.game.campaign.dispossessed.filter {
        case id: DenizenId => CatalogNames.suitOf(id) == Suit.Nomad
        case _ => false
      }
      val spare = boxed(ready, Suit.Nomad).drop(1)
      ready.updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck ++ spare ++ nomads)))
        .updateCampaign(campaign => campaign.copy(dispossessed =
          campaign.dispossessed.filterNot(nomads.contains))))
    val drawn = hand(suitsChosen(table,
      Vector(Suit.Nomad, Suit.Arcane, Suit.Order)))
    // One Nomad, two Arcane, one Order.
    assertEquals(drawn.size, 4)

  test("the holder keeps one newcomer facedown; five go to the world deck"):
    val drawn = suitsChosen(tasksAssigned,
      Vector(Suit.Beast, Suit.Arcane, Suit.Order))
    val done = ChronicleWalk.answeringEverything(rules, drawn)
    val after = Outcomes.ready(done.state)
    val lineage = after.game.current.players.find(_.player == p3).get.lineage
    val kept = after.game.campaign.lineages(lineage).startingAdvisers
    assertEquals(kept.map(_.id), Vector(hand(drawn).head))
    assertEquals(after.game.current.commonCards.worldDeck.takeRight(5),
      hand(drawn).tail)
```

`ChronicleWalk.answeringEverything` keeps the first available partition options for the "Keep" section, so the kept card is the hand's first. Add a Game Log test the way Task 4 does: p3 sees the kept newcomer named and p1 sees it hidden; every seat sees "chose Beast, Arcane and Order".

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.BeaconTaskSuite"`
Expected: compile errors naming `IntroduceNewcomers`.

- [ ] **Step 3: Introduce Newcomers**

`M/gameplay/chronicle/IntroduceNewcomers.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Introduce Newcomers, the Beacon's first step (CR p. 39). The Beacon task
  * holder chooses three different suits, one per draw count. Each chosen
  * suit's boxed denizens join its live reserve. If any chosen suit then has
  * too few, every older Dispossessed card returns to its suit's reserve
  * first; the newly Dispossessed stay. Each reserve is shuffled and the
  * holder draws from its top into their temporary hand. A suit still short
  * draws what remains (Chronicle ruling 15). */
object IntroduceNewcomers:
  val draws: Vector[Int] = Vector(3, 2, 1)
  def suitDecisionId(count: Int): String = s"chronicle.beacon.suit.$count"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  val choseSuits: NoteKey = notes.key("chose-suits", Vector(
    NotePart.Arg(0), NotePart.Text(" chose "), NotePart.Arg(1),
    NotePart.Text(", "), NotePart.Arg(2), NotePart.Text(" and "),
    NotePart.Arg(3)))
  val returned: NoteKey = notes.key("returned-dispossessed", Vector(
    NotePart.Text("The Dispossessed returned to the box")))
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] =
    draws.map(count => Branch((state, pending) =>
      ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder => Vector(
        Decide(suitDecisionId(count), holder, DecisionQuery.ChooseOne(
          Suit.all.filterNot(chosen(pending).contains).map(suit =>
            DecisionOption.FavorBank(DecisionOptionRef.FavorBank(suit))),
          heading = Some(s"Choose the suit to draw $count from"))))))) ++
    Vector(
      Branch((state, pending) =>
        ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
          Vector(Note(noteId, _ => chosen(pending) match
            case Vector(first, second, third) => Some(choseSuits.sourceless(
              NoteArg.Player(holder), NoteArg.Suit(first),
              NoteArg.Suit(second), NoteArg.Suit(third)))
            case _ => None)))),
      BuildOps((state, pending) => Right(fromBox(catalog, state,
        chosen(pending))), required = true),
      BuildOps((state, pending) => Right(returnIfShort(catalog, state,
        chosen(pending))), required = true),
      Note(noteId, states => states.previous.collect {
        case (before, after) if before.game.campaign.dispossessed.nonEmpty &&
            after.game.campaign.dispossessed.isEmpty => returned.sourceless()
      }),
      // One shuffle leaf per suit; the walker fills each order.
      Branch((_, pending) => chosen(pending).map(suit =>
        Shuffle(ShufflePile.SuitedReserve(suit)))),
      BuildOps((state, pending) =>
        ChronicleTasks.holder(state, ChronicleTask.Beacon).map(holder =>
          draw(state, holder, chosen(pending))), required = true))

  /** The suits answered so far, in draw order. */
  private def chosen(pending: PendingTree): Vector[Suit] =
    draws.flatMap(count => pending.answered.collectFirst {
      case Answered(id, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.FavorBank(suit)), _)
          if id == suitDecisionId(count) => suit
    })

  private def fromBox(catalog: ExecutableCatalog, state: ReadyGame,
      suits: Vector[Suit]): Vector[CoreOperation] =
    val inPlay = state.game.cardIndex.toOption.fold(Set.empty[CardId])(_.ids)
    suits.flatMap(suit =>
      val boxed = catalog.denizens.filter(_.suit == suit)
        .map(denizen => DenizenId(denizen.id.value))
        .filterNot(inPlay.contains)
      Option.when(boxed.nonEmpty)(SuitedReserveFromBox(suit, boxed)))

  private def returnIfShort(catalog: ExecutableCatalog, state: ReadyGame,
      suits: Vector[Suit]): Vector[CoreOperation] =
    val reserves = state.game.campaign.suitedReserves
    val short = suits.zip(draws).exists((suit, count) =>
      reserves.getOrElse(suit, Vector.empty).size < count)
    if !short then Vector.empty
    else state.game.campaign.dispossessed.flatMap {
      case card: DenizenId => catalog.denizen(card).map(denizen =>
        Move(Piece.Card(card), PositionedLocation(Location.Dispossessed),
          PositionedLocation(Location.SuitedReserve(denizen.suit))))
      case _ => None
    }

  private def draw(state: ReadyGame, holder: PlayerId, suits: Vector[Suit])
      : Vector[CoreOperation] =
    suits.zip(draws).flatMap((suit, count) =>
      state.game.campaign.suitedReserves.getOrElse(suit, Vector.empty)
        .take(count).map(card => Move(Piece.Card(card),
          PositionedLocation(Location.SuitedReserve(suit)),
          PositionedLocation(Location.Hand(holder)))))
```

Check these against the code before relying on them:

- Whether `NoteArg.Suit` exists. If it does not, add it beside `NoteArg.Goal`, with a `NoteLines` case that writes the suit's name. Without it the line cannot name the suits.
- Whether `sourceless()` with no args is legal.
- The suit decisions: each `Branch` builds one `Decide`, and its options depend on earlier answers. That shape is fixed for the replay rule ("the shape of a Branch must not change before a parked Decide"), because every Branch always yields exactly one node.
- An empty reserve: if the walker rejects a `Shuffle` of an empty pile, the Branch must skip suits with nothing to shuffle. The answers and the live state are both known at that point, so the Branch still has a fixed shape when it is walked.

- [ ] **Step 4: Welcome a Stranger**

`M/gameplay/chronicle/WelcomeStranger.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Welcome a Stranger, the Beacon's second step (CR p. 39): the holder
  * keeps one drawn newcomer as a facedown starting adviser and the rest go
  * to the world deck bottom. */
object WelcomeStranger:
  val keepDecisionId: String = "chronicle.beacon.keep"
  val keepKey: String = "keep"
  val deckKey: String = "world-deck"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  val keptAdviser: NoteKey = notes.key("kept-newcomer", Vector(
    NotePart.Arg(0), NotePart.Text(" welcomed "), NotePart.Arg(1),
    NotePart.Text(" as a starting adviser")))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
      val hand = state.game.current.temporaryHands
        .getOrElse(holder, Vector.empty).collect { case id: DenizenId => id }
      ChronicleTasks.lineageOf(state, holder) match
        case _ if hand.isEmpty => Vector.empty
        case None => Vector(ChronicleTasks.failing(OathViolation
          .InvalidEventOrder(s"${holder.value} has no lineage")))
        case Some(lineage) => StartingAdviserChoice.steps(holder, lineage,
          hand, keepDecisionId, keepKey, deckKey, "Welcome a stranger",
          keptAdviser, noteId))))
```

- [ ] **Step 5: Wire the Beacon**

In `ChronicleBeaconProcedure`:

```scala
  def noteKeys: Vector[NoteKey] =
    IntroduceNewcomers.noteKeys ++ WelcomeStranger.noteKeys

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleIntroduceNewcomers,
      IntroduceNewcomers.steps(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleWelcomeStranger,
      WelcomeStranger.steps),
    // Shape Foundation (CR p. 39) waits for Foundations.
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))
```

In `ProcedureLog`'s Beacon entry:

```scala
      lines = ActionLineSource.Notes(Map(
        IntroduceNewcomers.choseSuits -> LogKind.Decision,
        IntroduceNewcomers.returned -> LogKind.Delta,
        WelcomeStranger.keptAdviser -> LogKind.Decision)),
```

Add `ChronicleBeacon` to `GameLogPropertiesSuite`. Regenerate the note snapshot and check that the diff shows only the three Beacon keys.

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/IntroduceNewcomers.scala src/main/scala/distantlands/gameplay/chronicle/WelcomeStranger.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/test/scala/distantlands/gameplay/chronicle/BeaconTaskSuite.scala src/test/resources/notes/declared-keys.txt
# plus GameLogPropertiesSuite and any NoteArg/NoteLines change, by path
git commit -m "feat(chronicle): the Beacon draws newcomers from three suits and welcomes a stranger

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: End to end, replay and docs

**Files:**
- Rename: `T/gameplay/chronicle/ChronicleSunThroneEndToEndSuite.scala` to `ChronicleEndToEndSuite.scala`
- Modify: `docs/rules/chronicle.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `CONTEXT.md`, `docs/ROADMAP.md`

- [ ] **Step 1: Extend the end-to-end test**

Rename the suite with `git mv` and the class to `ChronicleEndToEndSuite`. Its doc comment becomes "A game won for real, through Start Chronicle, the Sun, Throne, World and Beacon, to the journaled Chronicle." In the existing test, after the role and score assertions, add:

```scala
    val worldHolder = ready.game.current.chronicleTasks(ChronicleTask.World)
    val beaconHolder = ready.game.current.chronicleTasks(ChronicleTask.Beacon)
    def startingAdvisers(player: PlayerId): Vector[CardId] =
      val lineage = ready.game.current.players.find(_.player == player)
        .get.lineage
      ready.game.campaign.lineages(lineage).startingAdvisers.map(_.id)
    assertEquals(startingAdvisers(worldHolder).size, 1)
    assertEquals(startingAdvisers(beaconHolder).size, 1)
    assertEquals(ready.game.current.newlyDispossessed.size, 6)
    assert(!ready.game.cardIndex.toOption.get.ids.exists(_.isInstanceOf[VisionId]))
    assert(ready.game.current.temporaryHands.values.forall(_.isEmpty))
```

Rename the test to "the winner's Chronicle runs every task to the journaled record, and replays". The existing replay assertion now covers World and Beacon. If the Sun gave both tasks to one player who then gave one away, `worldHolder` and `beaconHolder` differ anyway. If they are the same player, assert 2 starting advisers instead and say why in a comment.

- [ ] **Step 2: Run the full suite in the background**

Run: `./sbtw test` (about 15 minutes), then `python3 scripts/check-architecture.py` and the coverage ratchet the previous plan ran.
Expected: every test passes; architecture and coverage pass.

- [ ] **Step 3: Docs**

`docs/rules/ambiguities.md`, after ruling 14:

```markdown
15. **Newcomers from a short suit.** If a suit the Beacon task holder chose still has too few cards after all older Dispossessed return to their suit sections, the holder draws what remains of that suit. The project owner notes this should not happen in play (CR p. 39).
```

`docs/rules/chronicle.md`, World step 2, add:

```markdown
   - Digital: advisers are discarded first, which returns the tokens on them to their banks. The nine dealt denizens sit in an ordered gathering pile: the facedown stack is always the last three cards and the faceup stacks come before it.
```

World step 3, add:

```markdown
   - Digital: the holder protects a stack by naming any card in it. When the step ends, the six unprotected denizens move to a newly Dispossessed pile, and one log line names the faceup three. They stay there until the Chronicle is recorded, so the Beacon's return leaves them alone.
```

World step 1, add: `   - Digital: Visions leave play; setup adds all five again.`

Beacon step 1, add:

```markdown
   - Digital: the holder chooses the suit for three draws, then two, then one. Each chosen suit's boxed denizens join its live reserve before the draw. A suit still short after the return draws what remains (ruling 15, linked as `[ruling 15](ambiguities.md#chronicle-rulings)`).
```

Beacon step 3, add: `   - Digital: an empty window until Foundations exist.`

`docs/rules/implementation-traceability.md`: set the World and Beacon rows to **Partial**. World evidence: "Separate Visions, Gather Denizens and Protect and Dispossess, each in its own power window, are tested in `gameplay/chronicle/WorldTaskSuite.scala`; faceup stacks and starting advisers are identified per seat (`application/ChroniclePilesIdentitySuite.scala`)." World gap: "Starting advisers and the newly Dispossessed reach the Chronicle only with the Stars slice's extraction." Beacon evidence: "Introduce Newcomers (with ruling 15, linked as in the other rows) and Welcome a Stranger are tested in `gameplay/chronicle/BeaconTaskSuite.scala`." Beacon gap: "Shape Foundation is an empty window until Foundations (B2/B5); extraction waits for Stars." Update the Throne row's `ChronicleSunThroneEndToEndSuite` to `ChronicleEndToEndSuite`.

`CONTEXT.md`, beside "Set-aside edifices":

```markdown
- **Gathering pile**: the nine denizens the World task deals into two faceup stacks and one facedown stack.
- **Newly Dispossessed**: the six denizens the World task dispossessed this Chronicle, held apart from the older Dispossessed until the Chronicle is recorded.
```

`docs/ROADMAP.md`, under the Sun and Throne sub-bullet:

```markdown
  - World and Beacon landed: Visions leave play, nine denizens are dealt and
    one stack protected, newcomers are drawn from three suits, and each task
    holder keeps a starting adviser. Shape Foundation is an empty window.
    Remaining: Stars and the extraction of the table into the Chronicle.
```

Edit the Sun and Throne sub-bullet's "Remaining:" sentence to drop World and Beacon.

Run: `python3 scripts/check-markdown-links.py`. Expected: no broken links.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala docs/rules/chronicle.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md CONTEXT.md docs/ROADMAP.md
git commit -m "test(chronicle): run every task end to end; document World and Beacon

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

`git mv` already staged the old path's removal.

---

## Self-review notes

- **Spec coverage.** World and Beacon are triggered procedures chained by `ChronicleSequence` (spec "Procedure shape", "Chaining"). Each step is a window, with Shape Foundation empty ("Stubs"). The holders act ("Actors"). The draws and shuffles are recorded walker shuffles ("Randomness and secrecy"). The newcomers, the retained adviser and the facedown stack are hidden from other seats, and replay equals live (spec "Testing"). Extraction and the round trip stay in slice 3, as the spec's order of work says.
- **Rule coverage.** Every World and Beacon step in `docs/rules/chronicle.md` has a task, and the Beacon return follows the CR with ruling 15.
- **Names used across tasks.** `Location.ChronicleGathering`, `Location.NewlyDispossessed`, `Location.SuitedReserve`, `Location.StartingAdvisers`, `ShufflePile.ChronicleGathering`, `ShufflePile.SuitedReserve`, `SuitedReserveFromBox`, `StartingAdviserChoice.steps`, `ProtectAndDispossess.protectDecisionId` / `keepDecisionId` / `keepKey` / `deckKey`, `IntroduceNewcomers.suitDecisionId`, `WelcomeStranger.keepDecisionId`.
- **Check against the code before relying on it.** `OperationTestRun`, `CatalogNames.suitOf`, `NoteArg.Count`, `NoteArg.Suit`, `ready.game.catalog`, `traverse`, and whether the walker shuffles an empty pile. Each step that uses one of these says what to do if it is missing.
