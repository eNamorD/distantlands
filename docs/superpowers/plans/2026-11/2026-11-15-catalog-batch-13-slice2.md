# Catalog Batch 13, Slice 2: The Legacy Deck and Ancient Writ Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The game stores an ordered legacy deck, discarded legacies go to
its bottom, drawn legacies wait in the drawer's hand, and Ancient Writ (relic
R22) runs, with Matchmaker's holder drawing and choosing for another player.
Setup's Quiet Ambitions keep then draws the same way, so the Setup-only
Matchmaker visibility code goes.

**Architecture:** Today the legacy deck is derived and unstored, so a legacy
moved into it leaves play and nothing can be drawn from it during a game.
This slice stores it in `CardZones.legacyDeck`, top first. Game start fills
it with what Foundation III's deal leaves of the shuffled order, and
`CardIndex` indexes it like the edifice deck. Every move into it names the
bottom. Drawn legacies go to `CurrentGameState.legacyHands`, which shares
`Location.Hand` with the world-card `temporaryHands`, so the existing
"a hand is private to its owner" projection rule covers them. A new
`LegacyDraws` draws the top N with a `Draw`, which is a hidden outcome.
Ancient Writ is a `PaidAction`: bury the relic, draw 3 into the keeper's
hand, ask which to keep, then move one to the owner's lineage, dormant, and
the rest to the deck's bottom, which empties the hand again. The keeper is
`SetupLegacyRules.keeper`: Matchmaker's seated active holder, else the
owner. Last, game start stops dealing Quiet Ambitions' two legacies to each
lineage: `LegacyKeep.step` draws them at the keep into the keeper's hand,
so Matchmaker's holder sees another player's draw because it is in their
own hand, and `LegacyKeep.matchmade` and the projection's Matchmaker arm
are deleted.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
rulings 1, 2, 6, 8, 9 and 10, and section "Slice 2: the legacy deck and the
draw". The spec was amended on 2026-11-15, while this plan was written:
the owner chose a stored, ordered deck with discards at its bottom (in
place of a set-aside zone), drawn legacies in a hand, Setup's keep drawing
the same way, and Matchmaker's question naming the player it chooses for.

## Global Constraints

- Gameplay never reads card text: `scripts/check-architecture.py` forbids
  `rulesText` and `.text` under `gameplay/`.
- No production Scala file exceeds 800 lines (`check-architecture.py`).
- A card sits beside its power once a power of it is implemented, and in
  `catalog/holding/` exactly while none is (`CardCatalogSuite`).
- A `Branch` on the path to a parked decision is selected again against
  the live state when the walk resumes (`ProcedureWalker.walkBranch`). A
  step that changes what a `Branch` reads goes in an earlier sibling, so
  the `Branch` that holds the decision selects the same children on
  resume.
- A move into the legacy deck names `StackPosition.Bottom` (ruling 2) and
  never a facedown face (implementation ruling R6).
- `temporaryHands` keeps holding world cards only. Legacies in a hand live
  in `legacyHands`. Search and its powers are not touched.
- A line a power writes from a key another object owns is declared with
  `notes.borrow(...)`.
- Journals from before this slice need not replay: their moves into the
  legacy deck name no position.
- Docs, comments and commit messages are written in plain English.
  Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Files

- Model: `model/Cards.scala` (`CardZones.legacyDeck`),
  `model/Pieces.scala` (`PositionedLocation.legacyDeckBottom`),
  `model/GameState.scala` (`legacyHands`), `model/CardIndex.scala`,
  `model/DomainValidation.scala`.
- Operations: `gameplay/operations/CardMovementOperations.scala`,
  `gameplay/operations/OperationPipeline.scala`.
- Game start: `gameplay/setup/LegacyDeal.scala`,
  `gameplay/setup/GameStartRules.scala`.
- Setup's keep (Task 5): `gameplay/setup/LegacyKeep.scala` rewritten to
  draw into the keeper's hand; `LegacyDeal.drawn` deleted.
- Bottom placement: `gameplay/setup/LegacyKeep.scala`,
  `gameplay/powers/legacies/TheMouth.scala`,
  `gameplay/chronicle/ResolveLegacies.scala`,
  `gameplay/chronicle/legacies/IronHand.scala`.
- New: `gameplay/powers/LegacyDraws.scala`,
  `gameplay/powers/action/AncientWrit.scala`.
- Registration: `gameplay/powers/PhasePowerCatalog.scala`.
- Delete: `catalog/holding/HoldingRelics.scala`.
- Matchmaker comment: `gameplay/setup/legacies/Matchmaker.scala`.
- Projection: `application/GamePresentationProjector.scala` (the hand
  comment; Task 5 deletes the Matchmaker arm and `keepPending`).
- Tests: `testkit/Table.scala`; new
  `gameplay/operations/LegacyDeckSuite.scala`,
  `gameplay/operations/LegacyHandSuite.scala`,
  `application/LegacyHandProjectionSuite.scala`,
  `gameplay/powers/action/AncientWritSuite.scala`,
  `application/AncientWritProjectionSuite.scala`; updated
  `LegacyMovesSuite`, `CardIndexSuite`, `LegacySetupSuite`,
  `IronHandCitizenshipSuite`, and in Task 5 `LegacyDealSuite`,
  `GameStartRulesSuite`, `ExileOnlySetupSuite`, `MatchmakerSuite`,
  `MatchmakerProjectionSuite`, `GameApplicationServiceProjectionSuite`,
  `CombinedLegaciesRedactionSuite` and `SetupLegacyFixture`; snapshots
  `powers/unimplemented.txt` and `notes/declared-keys.txt`; the game-log
  goldens only if a drawn legacy's name moves.
- Docs: `docs/rules/ambiguities.md`, the Legacies and Legacy Setup powers
  implementation rulings files, `docs/rules/rules-reference.md`,
  `docs/rules/implementation-traceability.md`, `docs/ROADMAP.md`, the
  spec's status line.

All Scala paths are under `src/main/scala/distantlands/` or
`src/test/scala/distantlands/`.

---

### Task 1: The game stores an ordered legacy deck

**Files:**
- Modify: `src/main/scala/distantlands/model/Cards.scala:53-62`
- Modify: `src/main/scala/distantlands/model/Pieces.scala` (after `PositionedLocation`)
- Modify: `src/main/scala/distantlands/model/CardIndex.scala:137-140`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`
  (`isStack`, `stackCards`, `removeCard`, `insertDeck`, the `facedownLegacy` comment)
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationPipeline.scala:29-42`
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala:44-46`
- Modify: `LegacyKeep.scala:98`, `TheMouth.scala:161`, `ResolveLegacies.scala:152,184,206`, `IronHand.scala:76,141`
- Modify: `src/test/scala/distantlands/testkit/Table.scala` (`quiet`, `without`, new `legacyDeck`)
- Create: `src/test/scala/distantlands/gameplay/operations/LegacyDeckSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala`
- Modify: `src/test/scala/distantlands/model/CardIndexSuite.scala:131-146`
- Modify: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala:70-79`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/legacies/IronHandCitizenshipSuite.scala:29-31`

**Interfaces:**
- Consumes: `LegacyDeal(face, seats)` with `QuietDraw = 2`, `PublicRow = 4`;
  `Table` (`start`, `legacy`, `publicLegacies`, `moving`, `ready`);
  `RawOperationBatch.executeAll`.
- Produces:
  - `CardZones.legacyDeck: Vector[LegacyId]`, top first, default empty.
  - `PositionedLocation.legacyDeckBottom: PositionedLocation`, the
    bottom of `Location.Deck(CardDeck.Legacy)`.
  - `LegacyDeal#rest(order: Vector[LegacyId]): Vector[LegacyId]`.
  - `Table#legacyDeck(cards: LegacyRef*): Table`: the deck holds exactly
    `cards`, first named on top.
  - `Location.Deck(CardDeck.Legacy)` is a stack: a move into it names the
    top or the bottom, and a move out of it may name the top.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/operations/LegacyDeckSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LegacyDeal}
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** The stored legacy deck (catalog batch 13, rulings 1 and 2): game start
  * keeps what Foundation III's deal leaves, a draw takes from the top, and
  * a returned or discarded legacy goes to the bottom. */
class LegacyDeckSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def deckOf(ready: ReadyGame): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck

  test("Quiet Ambitions' game start keeps the order the draws leave"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(deckOf(ready),
      orders.legacyOrder.drop(LegacyDeal.QuietDraw * orders.participants.size))
    assertEquals(ready.game.cardIndex.toOption.get.get(deckOf(ready).head)
      .map(_.location), Some(CardLocation(CardContainer.Deck(CardDeck.Legacy), 0)))

  test("Public Ambitions' game start keeps the order the row leaves"):
    val altered = chronicle.copy(foundations = chronicle.foundations.updated(
      FoundationNumber.III, FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, altered, orders))
    assertEquals(deckOf(ready), orders.legacyOrder.drop(LegacyDeal.PublicRow))

  test("a legacy returned to the deck goes to its bottom"):
    val ready = Table.start.legacy(p1, SeerCard).legacyDeck(RivalCard).ready
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(SeerCard.id),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1))),
      PositionedLocation.legacyDeckBottom))))
    assertEquals(deckOf(after), Vector(RivalCard.id, SeerCard.id))
    assertEquals(after.game.campaign.lineages(lineageOf(ready, p1)).legacies,
      Vector.empty)

  test("a move into the legacy deck names the top or the bottom"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    assertEquals(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(SeerCard.id),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1))),
      PositionedLocation(Location.Deck(CardDeck.Legacy))))).left.toOption,
      Some(OperationError.InvalidStackPosition(Location.Deck(CardDeck.Legacy),
        "stack destination must specify top or bottom")))

  test("a legacy taken from the top of the deck joins a lineage dormant"):
    val ready = Table.start.legacyDeck(RivalCard, PopulistCard).ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(RivalCard.id),
      PositionedLocation(Location.Deck(CardDeck.Legacy), StackPosition.Top),
      PositionedLocation(Location.Legacies(lineage))))))
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = false)))
    assertEquals(deckOf(after), Vector(PopulistCard.id))

  test("only the top legacy can be taken from the top"):
    val ready = Table.start.legacyDeck(RivalCard, PopulistCard).ready
    assertEquals(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(PopulistCard.id),
      PositionedLocation(Location.Deck(CardDeck.Legacy), StackPosition.Top),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1)))))).left
      .toOption, Some(OperationError.InvalidStackPosition(
        Location.Deck(CardDeck.Legacy),
        "card does not match requested stack position")))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyDeckSuite"`
Expected: compilation FAILS: `legacyDeck` is not a member of `CardZones`
or `Table`, and `legacyDeckBottom` is not a member of `PositionedLocation`.

- [ ] **Step 3: Store the deck in the model**

In `model/Cards.scala`, replace the `CardZones` comment and add the field:

```scala
/** The shared card piles of a game. `publicLegacies` is the Public
  * Ambitions row, faceup. `legacyDeck` is the legacy deck, top first: game
  * start keeps what Foundation III's deal leaves of the shuffled deck, a
  * draw takes from the top, and a returned or discarded legacy goes to the
  * bottom (catalog batch 13, rulings 1 and 2). The next game derives its
  * deck from the Chronicle again (`LegacyDeal.deck`). */
final case class CardZones(
    worldDeck: Vector[WorldCardId],
    relicDeck: Vector[RelicId],
    edificeDeck: Vector[EdificeId],
    regionalDiscards: Map[Region, Vector[WorldCardId]],
    publicLegacies: Vector[LegacyId] = Vector.empty,
    legacyDeck: Vector[LegacyId] = Vector.empty
):
```

In `model/Pieces.scala`, directly after the `PositionedLocation` class body
(after its `require`), add its companion:

```scala
object PositionedLocation:
  /** The bottom of the legacy deck, where every returned or discarded
    * legacy goes (catalog batch 13, ruling 2). */
  val legacyDeckBottom: PositionedLocation =
    PositionedLocation(Location.Deck(CardDeck.Legacy), StackPosition.Bottom)
```

In `model/CardIndex.scala`, replace

```scala
    // The legacy deck is derived (Legacies design, ruling 3): no card is
    // indexed there.
    addIds(common.publicLegacies, CardContainer.PublicLegacies)
```

with

```scala
    addIds(common.legacyDeck, CardContainer.Deck(CardDeck.Legacy))
    addIds(common.publicLegacies, CardContainer.PublicLegacies)
```

- [ ] **Step 4: Make the legacy deck a stack in the operations**

In `gameplay/operations/CardMovementOperations.scala`:

Replace `isStack` and its comment with:

```scala
  private def isStack(location: Location): Boolean = location match
    case _: Location.Deck | _: Location.RegionalDiscard => true
    case _ => false
```

In `stackCards`, after the `CardDeck.Edifice` arm, add:

```scala
    case CardContainer.Deck(CardDeck.Legacy) =>
      Some(ready.game.current.commonCards.legacyDeck)
```

In `removeCard`, replace the two comment lines and
`case CardDeck.Legacy => cards` with:

```scala
      case CardDeck.Legacy => cards.copy(legacyDeck = cards.legacyDeck.filterNot(_ == located.id))
```

In `insertDeck`, replace the comment line and `case CardDeck.Legacy => cards`
with:

```scala
        case CardDeck.Legacy => cards.copy(legacyDeck = insertStack(
          cards.legacyDeck, id.asInstanceOf[LegacyId], position, topAtHead = true))
```

Replace the comment on `facedownLegacy` with:

```scala
  /** The public row is faceup and the legacy deck keeps no face, so a move
    * there may turn a legacy faceup or name no face, never facedown (R6). */
```

In `gameplay/operations/OperationPipeline.scala`, replace `boxed` and its
comment with:

```scala
  /** The cards this run took out of the game: every card whose executed move
    * ends at the shared bank, and every relic kept as a lineage's starting
    * relic, which no index holds (Heirloom; Legacies R2). The pipeline's
    * card-inventory check allows a card to be missing afterwards only if a
    * declared operation removed it.
    */
  def boxed(executed: Vector[CoreOperation]): Set[CardId] = executed.collect {
    case Move(Piece.Card(id), _, to, _, _)
        if to.location == Location.SharedBank =>
      id: CardId
    case StartingRelicKept(_, relic) => relic: CardId
  }.toSet
```

If `CardDeck` is now an unused import in `OperationPipeline.scala`, remove
it from that file's import list.

- [ ] **Step 5: Fill the deck at game start**

In `gameplay/setup/LegacyDeal.scala`, add after `publicRow`:

```scala
  /** What the deal leaves of `order`, top first: the game's legacy deck
    * (catalog batch 13, ruling 1). */
  def rest(order: Vector[LegacyId]): Vector[LegacyId] =
    order.drop(if public then LegacyDeal.PublicRow
      else LegacyDeal.QuietDraw * seats.size)
```

In `gameplay/setup/GameStartRules.scala`, the `commonCards = ...copy(...)`
call gains the deck:

```scala
      commonCards = material.commonCards.copy(edificeDeck =
        material.commonCards.edificeDeck.filterNot(storedEdifices),
        publicLegacies = legacyDeal.publicRow(orders.legacyOrder),
        legacyDeck = legacyDeal.rest(orders.legacyOrder))
```

In `LegacyDeal`'s `deck` comment, change "The derived legacy deck (ruling
3)" to "The legacy deck a game derives from `chronicle` (ruling 3), which
game start shuffles and deals from".

- [ ] **Step 6: Send every move into the deck to its bottom**

Each of these lines reads `PositionedLocation(Location.Deck(CardDeck.Legacy))`.
Replace each with `PositionedLocation.legacyDeckBottom`:

- `gameplay/setup/LegacyKeep.scala` (in `returnLegacies`)
- `gameplay/powers/legacies/TheMouth.scala` (the `ironHand` move)
- `gameplay/chronicle/ResolveLegacies.scala` (three moves: the cap's
  discard, `dormant`'s discard, `publicLegacy`'s return)
- `gameplay/chronicle/legacies/IronHand.scala` (`inGameDiscard` and
  `discard`)

Check none is left:

Run: `grep -rn "PositionedLocation(Location.Deck(CardDeck.Legacy))" src/main`
Expected: no output.

In `LegacyKeep.step`'s comment, change "return the other to the deck" to
"return the other to the bottom of the legacy deck".

- [ ] **Step 7: Teach `Table` the deck**

In `testkit/Table.scala`:

In `quiet`, the dealt legacies and the public row go back to the deck.
Add a `val` before `fresh.updateCurrent` and change the `publicLegacies`
line:

```scala
  private def quiet(fresh: ReadyGame): ReadyGame =
    val dealt = fresh.game.campaign.lineages.toVector.sortBy(_._1.value)
      .flatMap(_._2.legacies.map(_.id))
    fresh.updateCurrent { c =>
```

```scala
        publicLegacies = Vector.empty,
        legacyDeck = c.commonCards.legacyDeck ++
          c.commonCards.publicLegacies ++ dealt),
```

and change its comment's sentence "The legacies Foundation III dealt go
back to the derived deck, so no lineage holds one and the public row is
empty." to "The legacies Foundation III dealt and the public row go to the
bottom of the legacy deck, so no lineage holds one and the row is empty."

In `without`, add to the `zones.copy(...)`:

```scala
        legacyDeck = zones.legacyDeck.filter(keep),
```

After `publicLegacies` (the builder), add:

```scala
  /** The legacy deck holds exactly `cards`, first named on top. Every
    * other legacy in it leaves the game, and the table no longer expects
    * it, so a suite can draw from a short or empty deck. */
  def legacyDeck(cards: LegacyRef*): Table =
    val ids = cards.toVector.map(CardRefs.legacyId)
    val moved = ids.foldLeft(this)(_.moving(_))
    val boxed = moved.game.game.current.commonCards.legacyDeck
    Table(moved.game.updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(legacyDeck = ids))), moved.expected -- boxed)
```

If the compiler rejects `moved.expected` (the field is `private val`),
change it to `private[testkit] val expected`.

- [ ] **Step 8: Run the new suite**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyDeckSuite"`
Expected: PASS, 6 tests.

- [ ] **Step 9: Update the suites that pinned the old deck**

`gameplay/operations/LegacyMovesSuite.scala`:

- Change the class comment to "`Move` between a lineage's legacies, the
  public row and the legacy deck (Legacies design, slice 1; catalog batch
  13, ruling 2)."
- Replace the test "a legacy returned to the deck leaves play" with:

```scala
  test("a legacy returned to the deck goes to its bottom"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val lineage = lineageOf(ready, p1)
    val after = applied(ready, Move(Piece.Card(SeerCard.id),
      PositionedLocation(Location.Legacies(lineage)),
      PositionedLocation.legacyDeckBottom))
    assertEquals(after.game.campaign.lineages(lineage).legacies, Vector.empty)
    assertEquals(after.game.current.commonCards.legacyDeck.last, SeerCard.id)
```

- Replace "a public legacy returned to the deck leaves play" with:

```scala
  test("a public legacy returned to the deck goes to its bottom"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val after = applied(ready, Move(Piece.Card(RivalCard.id),
      PositionedLocation(Location.PublicLegacies),
      PositionedLocation.legacyDeckBottom))
    assertEquals(after.game.current.commonCards.publicLegacies, Vector.empty)
    assertEquals(after.game.current.commonCards.legacyDeck.last, RivalCard.id)
```

- Replace "a legacy goes to the public row or the deck faceup or with no
  face, never facedown" with:

```scala
  test("a legacy goes to the public row or the deck faceup or with no " +
      "face, never facedown"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val from = PositionedLocation(Location.Legacies(lineageOf(ready, p1)))
    Vector(PositionedLocation(Location.PublicLegacies),
      PositionedLocation.legacyDeckBottom).foreach { to =>
        def move(facing: Orientation) = Move(Piece.Card(SeerCard.id), from,
          to, resultingOrientation = Some(facing))
        assertEquals(RawOperationBatch.executeAll(ready,
          Vector(move(Orientation.FaceDown))),
          Left(OperationError.UnsupportedOrientation(SeerCard.id,
            to.location)), s"$to")
        assert(RawOperationBatch.executeAll(ready,
          Vector(move(Orientation.FaceUp))).isRight, s"$to")
    }
```

- Replace "the batch's card inventory check lets a legacy leave play into
  the legacy deck" with:

```scala
  test("the batch's card inventory check finds a legacy returned to the " +
      "deck"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val from = Location.Legacies(lineageOf(ready, p1))
    val run = OperationPipeline.run(ready, Vector(Move(Piece.Card(SeerCard.id),
      PositionedLocation(from), PositionedLocation.legacyDeckBottom)),
      OperationPolicy.Permissive, Vector.empty)(Right(_))
    assertEquals(run.map(_.state.game.cardIndex.toOption.get.get(SeerCard.id)
      .map(_.location.container)), Right(Some(CardContainer.Deck(CardDeck.Legacy))))
```

`model/CardIndexSuite.scala`, the test "a lineage's legacies and the
public row are indexed, and no legacy lies in the legacy deck": rename it
to "a lineage's legacies, the public row and the legacy deck are indexed",
and replace its last assertion (`assert(index.ids.flatMap(index.get).forall(...))`)
with:

```scala
    val top = ready.game.current.commonCards.legacyDeck.head
    assertEquals(index.locationOf(top),
      Some(CardLocation(CardContainer.Deck(CardDeck.Legacy), 0)))
```

`gameplay/setup/LegacySetupSuite.scala`, the test "in a first game each
player keeps one dormant legacy and returns the other to the deck": replace
`assertEquals(ready.game.cardIndex.toOption.get.get(returned), None)` with

```scala
      assertEquals(ready.game.cardIndex.toOption.get.get(returned)
        .map(_.location.container), Some(CardContainer.Deck(CardDeck.Legacy)))
```

and after the `seats.foreach { ... }` block add

```scala
    // Each returned legacy went to the bottom, below the deal's rest.
    assertEquals(ready.game.current.commonCards.legacyDeck
      .takeRight(seats.size).toSet, seats.map(seat => drawn(seat.playerId)(1)).toSet)
```

`gameplay/chronicle/legacies/IronHandCitizenshipSuite.scala`: in the
`discard` constant, replace `PositionedLocation(Location.Deck(CardDeck.Legacy))`
with `PositionedLocation.legacyDeckBottom`.

- [ ] **Step 10: Run the full suite**

Run: `./sbtw test`
Expected: PASS. Another failure that names the legacy deck is a test that
pinned the old rule: a legacy moved into the deck now stays indexed there,
and a move into it must name the bottom. Update that test to the new rule
and name it in the commit message. A failure elsewhere is a real
regression: stop and report it.

- [ ] **Step 11: Commit**

```bash
git add src/main src/test
git commit -m "feat(model): store the legacy deck in order and send returns to its bottom

Game start keeps what Foundation III's deal leaves of the shuffled deck, the
card index holds it, and every move into it names the bottom (catalog batch
13, rulings 1 and 2). A legacy returned to the deck no longer leaves play.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 2: Legacies can wait in a hand

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala` (`CurrentGameState`, last field)
- Modify: `src/main/scala/distantlands/model/CardIndex.scala:149-154`
- Modify: `src/main/scala/distantlands/model/DomainValidation.scala:129-130`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`
  (`cardDestinationViolation`, `removeCard`, `appendedToCurrent`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (the `Hand` comment in `identifiesCard`)
- Modify: `src/test/scala/distantlands/testkit/Table.scala` (`without`, new `legacyHand`)
- Create: `src/test/scala/distantlands/gameplay/operations/LegacyHandSuite.scala`
- Create: `src/test/scala/distantlands/application/LegacyHandProjectionSuite.scala`

**Interfaces:**
- Consumes: Task 1's `legacyDeck`, `legacyDeckBottom`, `Table#legacyDeck`.
- Produces:
  - `CurrentGameState.legacyHands: Map[PlayerId, Vector[LegacyId]]`,
    default empty, at `Location.Hand(player)` and
    `CardContainer.Player(player, PlayerCardArea.Hand)`.
  - A legacy may move into `Location.Hand(player)` and out of it. A
    `Draw(player, legacies, Location.Deck(CardDeck.Legacy), Location.Hand(player))`
    takes the top legacies in order.
  - `Table#legacyHand(player: PlayerId, cards: LegacyRef*): Table`.

- [ ] **Step 1: Write the failing suites**

Create `src/test/scala/distantlands/gameplay/operations/LegacyHandSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Legacies drawn into a hand (catalog batch 13, ruling 6): `legacyHands`,
  * at the same `Location.Hand` as the world cards a Search draws. */
class LegacyHandSuite extends munit.FunSuite:
  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def handOf(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.current.legacyHands.getOrElse(player, Vector.empty)

  test("a draw takes the top legacies into the drawer's hand, in order"):
    val ready = Table.start.legacyDeck(SeerCard, PopulistCard, RivalCard).ready
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Draw(p1,
      Vector(SeerCard.id, PopulistCard.id), Location.Deck(CardDeck.Legacy),
      Location.Hand(p1)))))
    assertEquals(handOf(after, p1), Vector(SeerCard.id, PopulistCard.id))
    assertEquals(after.game.current.commonCards.legacyDeck,
      Vector(RivalCard.id))
    assertEquals(after.game.current.temporaryHands.getOrElse(p1, Vector.empty),
      Vector.empty)
    assertEquals(after.game.cardIndex.toOption.get.locationOf(PopulistCard.id),
      Some(CardLocation(CardContainer.Player(p1, PlayerCardArea.Hand), 1)))

  test("a draw cannot skip the top legacy"):
    val ready = Table.start.legacyDeck(SeerCard, PopulistCard).ready
    assert(RawOperationBatch.executeAll(ready, Vector(Draw(p1,
      Vector(PopulistCard.id), Location.Deck(CardDeck.Legacy),
      Location.Hand(p1)))).isLeft)

  test("a legacy kept from a hand joins the lineage dormant, and one " +
      "discarded goes to the bottom of the deck"):
    val ready = Table.start.legacyDeck(RivalCard)
      .legacyHand(p1, SeerCard, PopulistCard).ready
    val lineage = lineageOf(ready, p1)
    val hand = PositionedLocation(Location.Hand(p1))
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(
      Move(Piece.Card(SeerCard.id), hand,
        PositionedLocation(Location.Legacies(lineage))),
      Move(Piece.Card(PopulistCard.id), hand,
        PositionedLocation.legacyDeckBottom))))
    assertEquals(handOf(after, p1), Vector.empty)
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))
    assertEquals(after.game.current.commonCards.legacyDeck,
      Vector(RivalCard.id, PopulistCard.id))
```

Create `src/test/scala/distantlands/application/LegacyHandProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** A legacy in a hand is named to the hand's holder alone (catalog batch
  * 13, ruling 6), as a world card in a hand is. */
class LegacyHandProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  test("only the hand's holder identifies a legacy in it"):
    val ready = Table.start.legacyHand(p2, SeerCard).ready
    assert(presentation.identifiesAt(ready, Some(p2), SeerCard.id))
    Vector(Some(p1), Some(p3), None).foreach(viewer => assert(
      !presentation.identifiesAt(ready, viewer, SeerCard.id), s"$viewer"))
```

Add to `testkit/Table.scala`, after `hand`:

```scala
  /** `cards` waiting in `player`'s hand, as a legacy draw leaves them
    * (catalog batch 13, ruling 6). */
  def legacyHand(player: PlayerId, cards: LegacyRef*): Table =
    val ids = cards.toVector.map(CardRefs.legacyId)
    ids.foldLeft(this)(_.moving(_)).update(_.updateCurrent(c => c.copy(
      legacyHands = c.legacyHands.updated(player,
        c.legacyHands.getOrElse(player, Vector.empty) ++ ids))))
```

and in `without`, after the `temporaryHands = ...` line:

```scala
      legacyHands = c.legacyHands.view.mapValues(_.filter(keep)).toMap,
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyHandSuite distantlands.application.LegacyHandProjectionSuite"`
Expected: compilation FAILS: `legacyHands` is not a member of
`CurrentGameState`.

- [ ] **Step 3: Add the hand to the model**

In `model/GameState.scala`, add a last parameter to `CurrentGameState`,
after `predictions`:

```scala
    predictions: Map[PlayerId, Prediction] = Map.empty,
    // Legacies drawn and not yet kept or discarded, by drawer (catalog
    // batch 13, ruling 6). The same `Location.Hand` as `temporaryHands`,
    // which holds world cards only, so a hand is private to its holder
    // whatever it holds. A draw's walk empties it.
    legacyHands: Map[PlayerId, Vector[LegacyId]] = Map.empty
):
```

In `model/CardIndex.scala`, replace the `temporaryHands` loop with one over
both maps, world cards first:

```scala
    val handOwners = (game.current.temporaryHands.keySet ++
      game.current.legacyHands.keySet).toVector.sortBy(_.value)
    handOwners.foreach(player =>
      addIds(
        game.current.temporaryHands.getOrElse(player, Vector.empty) ++
          game.current.legacyHands.getOrElse(player, Vector.empty),
        CardContainer.Player(player, PlayerCardArea.Hand)
      ))
```

In `model/DomainValidation.scala`, the owner check covers both maps:

```scala
    (game.current.temporaryHands.keySet ++ game.current.legacyHands.keySet
      -- playerIds).toVector
      .sortBy(_.value).foreach(player => problems += UnknownTemporaryHandOwner(player))
```

- [ ] **Step 4: Let legacies move through a hand**

In `gameplay/operations/CardMovementOperations.scala`:

In `cardDestinationViolation`, the `Location.Hand` arm becomes:

```scala
      case _: Location.Hand =>
        if id.isInstanceOf[WorldCardId] || id.isInstanceOf[LegacyId] then None
        else Some(InvalidDestination(transfer.piece, destination))
```

In `removeCard`, the hand arm becomes:

```scala
    case CardContainer.Player(player, PlayerCardArea.Hand) => Right(ready.updateCurrent {
      current => located.id match
        case legacy: LegacyId => current.copy(legacyHands =
          current.legacyHands.updated(player,
            current.legacyHands.getOrElse(player, Vector.empty)
              .filterNot(_ == legacy)))
        case _ => current.copy(temporaryHands = current.temporaryHands.updated(
          player, current.temporaryHands.getOrElse(player, Vector.empty)
            .filterNot(_ == located.id)))
    })
```

In `appendedToCurrent`, the hand arm becomes:

```scala
    case Location.Hand(player) => id match
      case legacy: LegacyId => current.copy(legacyHands =
        current.legacyHands.updated(player,
          current.legacyHands.getOrElse(player, Vector.empty) :+ legacy))
      case _ => current.copy(temporaryHands =
        current.temporaryHands.updated(player,
          current.temporaryHands.getOrElse(player, Vector.empty) :+
            id.asInstanceOf[WorldCardId]))
```

and its comment's list gains "a legacy hand" after "a temporary hand".

In `application/GamePresentationProjector.scala`, in `identifiesCard`, the
comment above `case CardContainer.Player(owner, PlayerCardArea.Hand)`
gains one sentence at its start: "A hand holds the world cards a player
has drawn and not yet resolved, and the legacies a legacy draw left there
(catalog batch 13, ruling 6)." Add to the scaladoc list above
`identifiesCard`, after the "A lineage's active legacy" bullet:

```scala
    *  - A legacy in a hand, which a legacy draw leaves there, is named only
    *    to the hand's holder: the drawer, or Matchmaker's holder drawing
    *    for another player (catalog batch 13, rulings 6 and 9).
```

- [ ] **Step 5: Run the suites**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyHandSuite distantlands.application.LegacyHandProjectionSuite distantlands.model.CardIndexSuite"`
Expected: PASS.

- [ ] **Step 6: Run the full suite**

Run: `./sbtw test`
Expected: PASS. Nothing outside this task's suites reads `legacyHands`.

- [ ] **Step 7: Commit**

```bash
git add src/main src/test
git commit -m "feat(model): let drawn legacies wait in a player's hand

legacyHands shares Location.Hand with the world-card temporaryHands, so a
drawn legacy is private to its holder through the existing hand rule
(catalog batch 13, ruling 6).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 3: Ancient Writ draws 3 legacies and keeps 1

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/LegacyDraws.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/action/AncientWrit.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/action/AncientWritSuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt` (regenerated)

**Interfaces:**
- Consumes: Tasks 1 and 2 (`legacyDeck`, `legacyHands`, `legacyDeckBottom`,
  `Table#legacyDeck`); `PaidAction`; `Bury.standard(card: BuryableCard,
  from: PositionedLocation, suit: Option[Suit], favor: Int, secrets: Int,
  actor: PlayerId)`; `PlayerFacts.player(ready, player): Either[OathViolation, PlayerState]`;
  `PowerAnswers.one(pending, decisionId): Option[DecisionOptionRef]` and
  `PowerAnswers.missing(decisionId): OathViolation`; `PhasePowerHarness`
  (`use`, `answer`, `after`, `awaits`, `replayed`, `pick`);
  `NoteText.said(power, events)`.
- Produces:
  - `LegacyDraws.steps(power: PowerId, drawer: PlayerId, count: Int,
    source: PowerSourceRef, drew: Int => LogNote): Vector[Operation]`,
    `LegacyDraws.top(ready: ReadyGame, count: Int): Vector[LegacyId]`,
    `LegacyDraws.draw(drawer: PlayerId, legacies: Vector[LegacyId]): Operation`,
    `LegacyDraws.hand(ready: ReadyGame, player: PlayerId): Vector[LegacyId]`,
    `LegacyDraws.empty: NoteKey` (key `"legacy-deck-empty"`). Task 5's
    Setup keep uses `top`, `draw` and `hand`; slice 5's Doors reuse them
    all.
  - `AncientWritCard` (`RelicId("R22")`, power id `relic.ancient-writ`).
  - `case object AncientWrit extends PaidAction` with `Drawn = 3`,
    `decisionId = "power.ancient-writ.keep"`, keys `drew` (`"used"`) and
    `kept` (`"kept"`), and `private def keeper(state, owner): PlayerId`,
    which Task 4 widens.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/action/AncientWritSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.powers.{LegacyDraws, NoteText}
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard,
  WorldCrafterCard}
import distantlands.model._
import distantlands.testkit.{Look, Outcomes, PhasePowerHarness, Table}
import distantlands.testkit.CardRefs.LegacyRef
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ancient Writ (relic R22), ACTION: bury it, draw 3 legacies, keep 1
  * dormant and discard the others to the bottom of the legacy deck
  * (catalog batch 13, rulings 1, 2, 6 and 8). */
class AncientWritSuite extends munit.FunSuite:
  import PhasePowerHarness.{after, answer, awaits, pick, use}

  private val writ = AncientWritCard.id
  private val source = DecisionOptionRef.Relic(writ)
  private val (seer, populist, crafter, rival) = (SeerCard.id,
    PopulistCard.id, WorldCrafterCard.id, RivalCard.id)

  /** p1's Act holding Ancient Writ faceup, the legacy deck holding `deck`,
    * first named on top. */
  private def holding(deck: LegacyRef*): Table =
    Table.start.turn(p1, Phase.Act).relic(p1, AncientWritCard)
      .legacyDeck(deck*)

  private def lineageOf(ready: ReadyGame): LineageId =
    ready.game.current.player(p1).get.lineage

  private def legaciesOf(ready: ReadyGame): Vector[LegacyState] =
    ready.game.campaign.lineages(lineageOf(ready)).legacies

  private def handOf(ready: ReadyGame): Vector[LegacyId] =
    LegacyDraws.hand(ready, p1)

  test("it buries itself and draws the top 3 legacies into the hand, then " +
      "asks which to keep"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard,
      RivalCard).ready, AncientWrit, source))
    assert(awaits(used, AncientWrit.decisionId))
    val now = after(used)
    assert(!Look(now).player(p1).relics.exists(_.id == writ))
    assertEquals(now.game.current.commonCards.relicDeck.last, writ)
    assertEquals(handOf(now), Vector(seer, populist, crafter))
    assertEquals(now.game.current.commonCards.legacyDeck, Vector(rival))

  test("the kept legacy joins the lineage dormant and the others go to the " +
      "bottom of the deck"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard,
      RivalCard).ready, AncientWrit, source))
    val kept = after(accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(populist)))))
    assertEquals(legaciesOf(kept), Vector(LegacyState(populist, active = false)))
    assertEquals(handOf(kept), Vector.empty)
    assertEquals(kept.game.current.commonCards.legacyDeck,
      Vector(rival, seer, crafter))

  test("a deck of two draws both and asks between them"):
    val used = accepted(use(holding(SeerCard, PopulistCard).ready,
      AncientWrit, source))
    assert(awaits(used, AncientWrit.decisionId))
    assertEquals(handOf(after(used)), Vector(seer, populist))

  test("a deck of one is kept without a question"):
    val used = accepted(use(holding(SeerCard).ready, AncientWrit, source))
    assert(!awaits(used, AncientWrit.decisionId))
    assertEquals(legaciesOf(after(used)),
      Vector(LegacyState(seer, active = false)))
    assertEquals(after(used).game.current.commonCards.legacyDeck, Vector.empty)

  test("an empty deck draws nothing, asks nothing and says so"):
    val used = accepted(use(holding().ready, AncientWrit, source))
    assert(!awaits(used, AncientWrit.decisionId))
    assertEquals(legaciesOf(after(used)), Vector.empty)
    assertEquals(NoteText.said(AncientWrit, used.events),
      Vector(NoteText.Said("legacy-deck-empty", "The legacy deck was empty.",
        covers = false)))

  test("its lines say how many legacies were drawn and that one was kept, " +
      "never which"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard)
      .ready, AncientWrit, source))
    val kept = accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(seer))))
    assertEquals(NoteText.said(AncientWrit, used.events ++ kept.events)
      .map(_.text), Vector("p1 drew 3 legacies.", "p1 kept a legacy."))
    val single = accepted(use(holding(SeerCard).ready, AncientWrit, source))
    assertEquals(NoteText.said(AncientWrit, single.events).map(_.text),
      Vector("p1 drew 1 legacy.", "p1 kept a legacy."))

  test("the journal replays to the same state"):
    val start = holding(SeerCard, PopulistCard, WorldCrafterCard).ready
    val used = accepted(use(start, AncientWrit, source))
    val kept = accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(crafter))))
    assertEquals(
      Outcomes.ready(accepted(PhasePowerHarness.replayed(start,
        used.events ++ kept.events))),
      after(kept))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.AncientWritSuite"`
Expected: compilation FAILS: `AncientWrit` and `LegacyDraws` are not
found. (`AncientWritCard` resolves from `catalog.holding`, which the suite
does not import, so it fails too.)

- [ ] **Step 3: Write the draw**

Create `src/main/scala/distantlands/gameplay/powers/LegacyDraws.scala`:

```scala
package distantlands.gameplay.powers

import distantlands.model._

/** A draw from the top of the legacy deck into the drawer's hand (catalog
  * batch 13, rulings 1 and 6): Ancient Writ's 3, and later the Doors'. The
  * deck is not shuffled during a game, so a discard at its bottom comes
  * back only once every legacy above it is drawn. The draw is a `Draw`, a
  * hidden outcome, so the legality search stops at it and undo never takes
  * it back. A short deck draws what remains. An empty deck draws nothing,
  * and its line says so.
  */
object LegacyDraws:
  /** The keys this object owns, which no one power owns. A power that
    * writes one borrows it. */
  val notes: NoteSet = NoteSet.unowned("LegacyDraws")
  /** "The legacy deck was empty." */
  val empty: NoteKey = notes.key("legacy-deck-empty",
    Vector(NotePart.Text("The legacy deck was empty.")))

  /** The top `count` legacies of the deck in `ready`, fewer from a short
    * deck. */
  def top(ready: ReadyGame, count: Int): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck.take(count)

  /** `drawer` takes `legacies`, the deck's top, into their hand. Callers
    * read `legacies` with [[top]] in the same `Branch`. */
  def draw(drawer: PlayerId, legacies: Vector[LegacyId]): Operation =
    BuildOps((_, _) => Right(Vector(Draw(drawer, legacies,
      Location.Deck(CardDeck.Legacy), Location.Hand(drawer)))),
      required = true)

  /** A power's draw: `drawer` draws up to `count` legacies, written under
    * `power` from `source`. `drew` is the line for the number actually
    * drawn. The deck is read when the step is reached, not when the tree
    * is built. Setup's keep uses [[top]] and [[draw]] alone: it writes no
    * draw line. */
  def steps(power: PowerId, drawer: PlayerId, count: Int,
      source: PowerSourceRef, drew: Int => LogNote): Vector[Operation] =
    Vector(Branch((live, _) =>
      top(live, count) match
        case Vector() => Vector(Note(power, _ => Some(empty(source))))
        case taken => Vector(draw(drawer, taken),
          Note(power, _ => Some(drew(taken.size))))))

  /** The legacies waiting in `player`'s hand, in draw order. */
  def hand(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.current.legacyHands.getOrElse(player, Vector.empty)
```

- [ ] **Step 4: Write Ancient Writ**

Create `src/main/scala/distantlands/gameplay/powers/action/AncientWrit.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.catalog.{ExecutableCatalog, PrintedPower, Relic}
import distantlands.gameplay.powers.{LegacyDraws, PlayerFacts, PowerAnswers}
import distantlands.model._

object AncientWritCard extends Relic(RelicId("R22"), "Ancient Writ", value = 81, defense = 1):
  val power = PrintedPower(PowerId("relic.ancient-writ"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Bury this to draw 3 legacy cards, keep 1 " +
      "facedown, and discard the other 2.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ancient Writ (relic R22), ACTION: bury this relic, draw 3 legacies, keep
  * 1 dormant and discard the others (catalog batch 13, rulings 8 to 10).
  *
  * The bury comes first and returns the relic's secrets to its holder, as
  * Magic Waterskin's does. The draw takes the top 3 of the legacy deck into
  * the keeper's hand (`LegacyDraws`); a short deck draws what remains. The
  * keep is its own `Branch`, after the draw's, so a walk resumed at the
  * question reads the same hand. With two or three drawn the keeper chooses
  * one; with one it is kept unasked; with none nothing is asked. The kept
  * legacy joins the owner's lineage, dormant, and the others go to the
  * bottom of the deck. The lines name no legacy: the kept one is dormant.
  *
  * The power id was `relic.ancient-grit` while the card was in holding
  * (catalog batch 10 design); it is fixed here.
  */
case object AncientWrit extends PaidAction(AncientWritCard.power):
  /** How many legacies the Writ draws. */
  val Drawn: Int = 3
  val decisionId: String = "power.ancient-writ.keep"
  private val notes = NoteSet(id)
  /** "p1 drew 3 legacies." */
  val drew: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" drew "), NotePart.Arg(1),
    NotePart.Plural(1, " legacy.", " legacies.")))
  /** "p1 kept a legacy." */
  val kept: NoteKey = notes.key("kept", Vector(NotePart.Arg(0),
    NotePart.Text(" kept a legacy.")))
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The kept line is the keep's narration: no "Chose" line names the
    * dormant legacy. */
  override def narratedDecisions: Set[String] = Set(decisionId)

  private val card: PowerSourceRef = PowerSourceRef.Card(AncientWritCard.id)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    source match
      case DecisionOptionRef.Relic(relicId) =>
        for
          held <- PlayerFacts.player(ready, player)
          relic <- held.relics.find(_.id == relicId).toRight(
            OathViolation.InvalidEventOrder(
              s"${relicId.value} is not held by ${player.value}"))
        yield Sequence(Bury.standard(BuryableCard.Relic(relicId),
          PositionedLocation(Location.PlayArea(player)), None, 0,
          relic.tokens.secrets, player) ++
          Vector(draw(player), keep(player, held.lineage)))
      case other => Left(OathViolation.InvalidEventOrder(
        s"${other.kind} is not a relic source"))

  /** Who draws and chooses for `owner`. */
  private def keeper(state: ReadyGame, owner: PlayerId): PlayerId = owner

  private def draw(owner: PlayerId): Operation = Branch((live, _) =>
    val drawer = keeper(live, owner)
    LegacyDraws.steps(id, drawer, Drawn, card, count =>
      drew(card, NoteArg.Player(drawer), NoteArg.Number(count))))

  private def keep(owner: PlayerId, lineage: LineageId): Operation =
    Branch((live, _) =>
      val chooser = keeper(live, owner)
      val line = Note(id, _ => Some(kept(card, NoteArg.Player(chooser))))
      LegacyDraws.hand(live, chooser) match
        case Vector() => Vector.empty
        case Vector(only) => Vector(BuildOps((_, _) =>
          Right(Vector(keptMove(only, chooser, lineage))), required = true),
          line)
        case drawn => Vector(
          Decide(decisionId, chooser, DecisionQuery.ChooseOne(
            drawn.map(legacy =>
              DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
            heading = Some("Ancient Writ: keep a legacy"),
            confirmVerb = Some("Keep"))),
          BuildOps((_, pending) =>
            settled(pending, drawn, chooser, lineage), required = true),
          line))

  /** The kept legacy, from the chooser's hand to the owner's lineage, with
    * no face: dormant. */
  private def keptMove(legacy: LegacyId, chooser: PlayerId,
      lineage: LineageId): Move =
    Move(Piece.Card(legacy), PositionedLocation(Location.Hand(chooser)),
      PositionedLocation(Location.Legacies(lineage)))

  private def settled(pending: PendingTree, drawn: Vector[LegacyId],
      chooser: PlayerId, lineage: LineageId)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId) match
      case Some(DecisionOptionRef.Legacy(chosen)) if drawn.contains(chosen) =>
        Right(keptMove(chosen, chooser, lineage) +: drawn.filterNot(_ == chosen)
          .map(legacy => Move(Piece.Card(legacy),
            PositionedLocation(Location.Hand(chooser)),
            PositionedLocation.legacyDeckBottom)))
      case _ => Left(PowerAnswers.missing(decisionId))
```

Delete `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala`
(its only card was `AncientWritCard`). `NewFoundations.scala` already lists
`AncientWritCard` and imports `distantlands.gameplay.powers.action._`, so it
resolves from its new package with no edit.

In `gameplay/powers/PhasePowerCatalog.scala`, import `AncientWrit` from
`distantlands.gameplay.powers.action` and add it after `MagicWaterskin`:

```scala
      Vector[PhasePower](WaysideInn, Elders, MagicWaterskin, AncientWrit,
        MarbleFountains,
```

- [ ] **Step 5: Run the suite**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.AncientWritSuite"`
Expected: PASS, 7 tests.

If "the journal replays" fails because the recorded `Draw` leaves are
rejected on replay, read the failure before changing anything: the replay
must apply the recorded `Move`s from the top of the stored deck, which
replay rebuilds from game start through Task 1.

- [ ] **Step 6: Regenerate the snapshots**

Run: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite"`
Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: `git diff src/test/resources` shows exactly `relic.ancient-grit`
removed from `powers/unimplemented.txt`, and the keys `used`, `kept` and
`legacy-deck-empty` under `relic.ancient-writ` added to
`notes/declared-keys.txt`. Anything else in the diff is a regression: stop
and report it.

- [ ] **Step 7: Run the full suite**

Run: `./sbtw test`
Expected: PASS. `CardCatalogSuite` finds Ancient Writ outside
`catalog.holding` with its power implemented.

- [ ] **Step 8: Commit**

```bash
git add src/main src/test
git commit -m "feat(catalog): Ancient Writ draws 3 legacies, keeps 1 and discards the rest

The draw takes the top of the legacy deck into the holder's hand; the keep
moves one to the lineage, dormant, and the others to the bottom (catalog
batch 13, rulings 8 to 10). Its power id is fixed to relic.ancient-writ.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 4: Matchmaker's holder draws and chooses during a game

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/action/AncientWrit.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/Matchmaker.scala` (scaladoc)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SetupLegacyRules.scala` (`keeper` scaladoc)
- Modify: `src/test/scala/distantlands/gameplay/powers/action/AncientWritSuite.scala`
- Create: `src/test/scala/distantlands/application/AncientWritProjectionSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt` (regenerated)

**Interfaces:**
- Consumes: Task 3's `AncientWrit` (`keeper`, `draw`, `keep`, `drew`,
  `kept`); `SetupLegacyRules.keeper(state: ReadyGame, owner: PlayerId): PlayerId`;
  `MatchmakerCard.id`; `PhasePowerHarness.parked.parkedDecision`
  (`.decision`, `.awaiting`) and `PhasePowerHarness.offered`.
- Produces: `AncientWrit.drewFor: NoteKey` (key `"used.matchmaker"`) and
  `AncientWrit.keptFor: NoteKey` (key `"kept-for"`).

- [ ] **Step 1: Write the failing tests**

Append to `AncientWritSuite` (add `MatchmakerCard` to the
`setup.legacies` import, `p3` to the `Table` import, and
`import distantlands.protocol.projection.DecisionQueryProjection`):

```scala
  /** The parked decision's id and who it waits on. */
  private def waiting(used: OathTransition): Option[(String, PlayerId)] =
    PhasePowerHarness.parked.parkedDecision(used.state)
      .map(facts => facts.decision -> facts.awaiting)

  test("another player's active Matchmaker draws into their own hand and " +
      "chooses for the holder"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard)
      .legacy(p3, MatchmakerCard).ready, AncientWrit, source))
    assertEquals(waiting(used), Some(AncientWrit.decisionId -> p3))
    assertEquals(LegacyDraws.hand(after(used), p3), Vector(seer, populist,
      crafter))
    assertEquals(handOf(after(used)), Vector.empty)
    val kept = after(accepted(answer(used, p3, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(crafter)))))
    assertEquals(legaciesOf(kept), Vector(LegacyState(crafter, active = false)))
    assertEquals(LegacyDraws.hand(kept, p3), Vector.empty)
    assertEquals(kept.game.current.commonCards.legacyDeck.takeRight(2),
      Vector(seer, populist))

  test("Matchmaker's lines name both players"):
    val used = accepted(use(holding(SeerCard, PopulistCard)
      .legacy(p3, MatchmakerCard).ready, AncientWrit, source))
    val kept = accepted(answer(used, p3, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(seer))))
    assertEquals(NoteText.said(AncientWrit, used.events ++ kept.events)
      .map(_.text), Vector("p3 drew 2 legacies for p1 with Matchmaker.",
        "p3 kept a legacy for p1 with Matchmaker."))

  test("Matchmaker's question names the player it keeps for"):
    val used = accepted(use(holding(SeerCard, PopulistCard)
      .legacy(p3, MatchmakerCard).ready, AncientWrit, source))
    assertEquals(PhasePowerHarness.queryOf(used, p3).collect {
      case one: DecisionQueryProjection.ChooseOne => one.heading }.flatten,
      Some("Matchmaker: keep a legacy for p1"))

  test("a dormant Matchmaker chooses for nobody"):
    val used = accepted(use(holding(SeerCard, PopulistCard)
      .legacy(p3, MatchmakerCard, active = false).ready, AncientWrit, source))
    assertEquals(waiting(used), Some(AncientWrit.decisionId -> p1))
    assertEquals(handOf(after(used)), Vector(seer, populist))

  test("Matchmaker's own holder using Ancient Writ chooses for themself"):
    val used = accepted(use(holding(SeerCard, PopulistCard)
      .legacy(p1, MatchmakerCard).ready, AncientWrit, source))
    assertEquals(waiting(used), Some(AncientWrit.decisionId -> p1))
    val kept = accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(seer))))
    assertEquals(NoteText.said(AncientWrit, used.events ++ kept.events)
      .map(_.text), Vector("p1 drew 2 legacies.", "p1 kept a legacy."))

  test("Matchmaker's keep replays to the same state"):
    val start = holding(SeerCard, PopulistCard, WorldCrafterCard)
      .legacy(p3, MatchmakerCard).ready
    val used = accepted(use(start, AncientWrit, source))
    val kept = accepted(answer(used, p3, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(seer))))
    assertEquals(
      Outcomes.ready(accepted(PhasePowerHarness.replayed(start,
        used.events ++ kept.events))),
      after(kept))
```

Create `src/test/scala/distantlands/application/AncientWritProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.powers.action.{AncientWrit, AncientWritCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{MatchmakerCard, PopulistCard,
  SeerCard, WorldCrafterCard}
import distantlands.model._
import distantlands.testkit.{PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Who sees the legacies Matchmaker's holder draws for Ancient Writ's
  * holder (catalog batch 13, rulings 6 and 9): the drawer alone while they
  * choose; then the kept legacy its owner alone, and the discarded ones
  * nobody. p1 uses the Writ and p3 holds Matchmaker. */
class AncientWritProjectionSuite extends munit.FunSuite:
  import PhasePowerHarness.{after, answer, pick, use}

  private val presentation = new GamePresentationProjector(catalog)
  private val drawn = Vector(SeerCard.id, PopulistCard.id, WorldCrafterCard.id)
  private val viewers = Vector(Some(p1), Some(p2), Some(p3), None)

  private val used = accepted(use(Table.start.turn(p1, Phase.Act)
    .relic(p1, AncientWritCard)
    .legacyDeck(SeerCard, PopulistCard, WorldCrafterCard)
    .legacy(p3, MatchmakerCard).ready, AncientWrit,
    DecisionOptionRef.Relic(AncientWritCard.id)))

  test("while Matchmaker's holder chooses, only they identify the draw"):
    drawn.foreach { legacy =>
      assert(presentation.identifiesAt(after(used), Some(p3), legacy), legacy)
      Vector(Some(p1), Some(p2), None).foreach(viewer => assert(
        !presentation.identifiesAt(after(used), viewer, legacy),
        s"$viewer $legacy"))
    }
    assertEquals(PhasePowerHarness.offered(used, p3).map(_.map(_._2)),
      Some(drawn.map(_.value)))

  test("once kept, the owner alone identifies the kept legacy and nobody " +
      "the discarded ones"):
    val kept = after(accepted(answer(used, p3, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(PopulistCard.id)))))
    assert(presentation.identifiesAt(kept, Some(p1), PopulistCard.id))
    Vector(Some(p2), Some(p3), None).foreach(viewer => assert(
      !presentation.identifiesAt(kept, viewer, PopulistCard.id), s"$viewer"))
    Vector(SeerCard.id, WorldCrafterCard.id).foreach(legacy =>
      viewers.foreach(viewer => assert(
        !presentation.identifiesAt(kept, viewer, legacy), s"$viewer $legacy")))
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.AncientWritSuite distantlands.application.AncientWritProjectionSuite"`
Expected: the new Matchmaker tests FAIL: the decision waits on p1 and the
draw is in p1's hand. The Task 3 tests and "a dormant Matchmaker chooses
for nobody" PASS.

- [ ] **Step 3: Let Matchmaker's holder keep**

In `AncientWrit.scala`, import the Matchmaker pieces:

```scala
import distantlands.gameplay.setup.legacies.{MatchmakerCard, SetupLegacyRules}
```

Add the two keys after `kept`:

```scala
  /** "p3 drew 3 legacies for p1 with Matchmaker." */
  val drewFor: NoteKey = notes.key(s"${NoteKey.Used}.matchmaker", Vector(
    NotePart.Arg(0), NotePart.Text(" drew "), NotePart.Arg(1),
    NotePart.Plural(1, " legacy", " legacies"), NotePart.Text(" for "),
    NotePart.Arg(2), NotePart.Text(" with "), NotePart.Arg(3),
    NotePart.Text(".")))
  /** "p3 kept a legacy for p1 with Matchmaker." */
  val keptFor: NoteKey = notes.key("kept-for", Vector(NotePart.Arg(0),
    NotePart.Text(" kept a legacy for "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2), NotePart.Text(".")))
```

Replace `keeper` and its comment:

```scala
  /** Who draws and chooses for `owner`: another seated player holding
    * Matchmaker active, else `owner` (ruling 9). Read where each step is
    * reached; nothing between the draw and the keep changes who holds
    * Matchmaker. */
  private def keeper(state: ReadyGame, owner: PlayerId): PlayerId =
    SetupLegacyRules.keeper(state, owner)
```

In `draw`, the line names both players when they differ:

```scala
  private def draw(owner: PlayerId): Operation = Branch((live, _) =>
    val drawer = keeper(live, owner)
    LegacyDraws.steps(id, drawer, Drawn, card, count =>
      if drawer == owner then
        drew(card, NoteArg.Player(drawer), NoteArg.Number(count))
      else drewFor(card, NoteArg.Player(drawer), NoteArg.Number(count),
        NoteArg.Player(owner), NoteArg.Card(MatchmakerCard.id))))
```

In `keep`, replace the `line` and the heading:

```scala
      val line = Note(id, _ => Some(
        if chooser == owner then kept(card, NoteArg.Player(chooser))
        else keptFor(card, NoteArg.Player(chooser), NoteArg.Player(owner),
          NoteArg.Card(MatchmakerCard.id))))
```

```scala
            heading = Some(
              if chooser == owner then "Ancient Writ: keep a legacy"
              else s"Matchmaker: keep a legacy for ${owner.value}"),
```

A heading names a player by its id, as `GamePresentationProjector.playerLabel`
and the log lines do.

In the object's scaladoc, add after "the keeper's hand (`LegacyDraws`)":
"The keeper is the owner, or another seated player who holds Matchmaker
active and draws and chooses for them off turn (ruling 9). A hand is
private to its holder, so the owner never sees the legacies drawn for
them, only the kept one once it is on their lineage."

In `setup/legacies/Matchmaker.scala`, replace the scaladoc sentence "Draws
during a game (Amber Doors, Lost Doors, Ancient Writ) wait for their
batch." with "During a game its holder draws Ancient Writ's legacies into
their own hand and keeps one for the Writ's holder (catalog batch 13,
ruling 9; `AncientWrit`); Amber Doors does not trigger it, and Lost Doors'
discard is the owner's (ruling 10)."

In `setup/legacies/SetupLegacyRules.scala`, the `keeper` scaladoc becomes:
"Who answers `owner`'s keep of drawn legacies: Matchmaker's holder, for
every player but themselves, else the owner. Quiet Ambitions' keep in Setup
(ruling 13) and Ancient Writ's during a game (catalog batch 13, ruling 9)
both ask it."

- [ ] **Step 4: Run the suites**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.AncientWritSuite distantlands.application.AncientWritProjectionSuite"`
Expected: PASS, 13 and 2 tests.

- [ ] **Step 5: Regenerate the note keys and run the full suite**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: `git diff src/test/resources` adds exactly `used.matchmaker` and
`kept-for` under `relic.ancient-writ`.

Run: `./sbtw test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main src/test
git commit -m "feat(catalog): Matchmaker's holder draws and keeps for Ancient Writ's holder

The draw goes to the keeper's own hand, so the owner never sees the legacies
not kept (catalog batch 13, ruling 9).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 5: Setup's Quiet Ambitions keep draws into the keeper's hand

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala:47-60`
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (the lineage-legacies arm of `identifiesCard`, its scaladoc, `keepPending`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/Matchmaker.scala` (scaladoc)
- Modify: `src/test/scala/distantlands/gameplay/setup/SetupLegacyFixture.scala` (new `drawn`)
- Modify: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala`,
  `LegacyDealSuite.scala`, `GameStartRulesSuite.scala:97-112`,
  `ExileOnlySetupSuite.scala:219-226`,
  `src/test/scala/distantlands/gameplay/setup/legacies/MatchmakerSuite.scala:29-31`,
  `src/test/scala/distantlands/application/MatchmakerProjectionSuite.scala:26-27,71-72`,
  `src/test/scala/distantlands/application/GameApplicationServiceProjectionSuite.scala:326-329`,
  `src/test/scala/distantlands/application/gamelog/CombinedLegaciesRedactionSuite.scala:38-41`,
  `src/test/scala/distantlands/gameplay/operations/LegacyDeckSuite.scala` (first test)
- Modify: `src/test/resources/gamelog/*.log` only if the goldens move (Step 7)

**Interfaces:**
- Consumes: `LegacyDraws.top`, `LegacyDraws.draw`, `LegacyDraws.hand`
  (Task 3); `PositionedLocation.legacyDeckBottom` (Task 1);
  `SetupLegacyRules.keeper`; `LegacyKeep.decisionId`, `keptLegacy`,
  `Matchmaker.keptFor` (unchanged).
- Produces:
  - Game start deals no legacy to a lineage under Quiet Ambitions:
    `CardZones.legacyDeck` holds the whole shuffled order less the public
    row (`LegacyDeal#rest`).
  - `LegacyKeep.step(participant)` draws `LegacyDeal.QuietDraw` into the
    keeper's hand, then keeps. Its decision id, lines and answers are
    unchanged; its Matchmaker heading names the owner.
  - Deleted: `LegacyDeal#drawn`, `LegacyKeep.drawn`, `LegacyKeep.matchmade`,
    `GamePresentationProjector.keepPending`.
  - Test helper `SetupLegacyFixture.drawn(orders: SetupOrders, player: PlayerId): Vector[LegacyId]`.

Setup's keeps run in turn order from the first player
(`SetupProcedure.turnOrder`), so each keep draws the next 2 from the top in
that order, not in seat order. With a deck of at least 2 per player, player
`k` in that order draws `legacyOrder.slice(2k, 2k + 2)`, which is what
`SetupLegacyFixture.drawn` computes. A short deck differs: a legacy an
earlier keep returned to the bottom can reach a later player (spec ruling
6).

- [ ] **Step 1: Write the test helper and the failing tests**

In `src/test/scala/distantlands/gameplay/setup/SetupLegacyFixture.scala`,
add to the object:

```scala
  /** The legacies `player` draws at their Quiet Ambitions keep, when the
    * deck holds at least 2 per player: the keeps run in turn order from
    * the first player, and each draws the next 2 from the top of
    * `orders.legacyOrder` (catalog batch 13, ruling 6). */
  def drawn(orders: SetupOrders, player: PlayerId): Vector[LegacyId] =
    val seats = orders.participants.map(_.playerId)
    val start = seats.indexOf(orders.firstPlayer)
    val at = (seats.drop(start) ++ seats.take(start)).indexOf(player)
    if at < 0 then Vector.empty
    else orders.legacyOrder.slice(LegacyDeal.QuietDraw * at,
      LegacyDeal.QuietDraw * (at + 1))
```

In `LegacySetupSuite.scala`:

- Replace the test "Quiet Ambitions deals each seated lineage 2 dormant
  legacies, after those it holds" with:

```scala
  test("Quiet Ambitions' game start deals no legacy: each seated lineage " +
      "starts from its record and the deck holds the whole order"):
    val seated = seats.head.lineageId
    val kept = LegacyState(SeerCard.id, active = true)
    val keeping = later.copy(lineages = later.lineages.map(lineage =>
      if lineage.id == seated then lineage.copy(legacies = Vector(kept))
      else lineage))
    val order = LegacyDeal.deck(catalog, keeping)
    val ready = accepted(GameStartRules.evolve(catalog, keeping,
      LaterGameFixture.orders(keeping, seats.head.playerId)
        .copy(legacyOrder = order)))
    seats.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies,
      if seat.lineageId == seated then Vector(kept) else Vector.empty,
      seat.playerId.value))
    assertEquals(ready.game.current.commonCards.legacyDeck, order)
    assertEquals(ready.game.current.commonCards.publicLegacies, Vector.empty)
```

- Replace `private val deal = LegacyDeal.of(chronicle, seats)` and the
  `drawn` helper below it with:

```scala
  private def drawn(player: PlayerId): Vector[LegacyId] =
    SetupLegacyFixture.drawn(orders, player)
```

- Replace the test "a short deck draws 2, 1 and 0 in seat order: ..."
  with (the setup lines that build `short` and `shortOrders` stay as they
  are):

```scala
  test("a short deck draws in keep order, and a later keep can draw a " +
      "legacy an earlier one returned (R9; catalog batch 13, ruling 6)"):
    // Box lineages hold every legacy but 3, so the derived deck is short.
    // A later game with a Chancellor keeps them; a first game played
    // exile-only would wipe them back into the deck (R40).
    val held = catalog.legacies.map(_.id).dropRight(3)
    val boxed = held.grouped(12).zipWithIndex.map((ids, n) => LineageState(
      LineageId(s"boxed-$n"), None, Role.Exile,
      ids.map(LegacyState(_, active = true)), Vector.empty)).toVector
    val short = later.copy(lineages = later.lineages ++ boxed)
    val shortOrders = LaterGameFixture.orders(short, seats.head.playerId)
      .copy(legacyOrder = LegacyDeal.deck(catalog, short))
    val Vector(a, b, c) = shortOrders.legacyOrder
    val started = accepted(GameStartRules.evolve(catalog, short, shortOrders))
    val Vector(first, second, third) = seats
    // The default answer keeps the first option: the first keep takes a
    // and returns b to the bottom, under c; the second draws c and b,
    // keeps c and returns b; the third draws b alone and keeps it unasked.
    val setup = walked(started)
    assertEquals(legacyQuestions(setup.asked).map(_.owner),
      Vector(first.playerId, second.playerId))
    def holding(seat: FirstGameParticipant) =
      setup.ready.game.campaign.lineages(seat.lineageId).legacies
    assertEquals(holding(first), Vector(LegacyState(a, active = false)))
    assertEquals(holding(second), Vector(LegacyState(c, active = false)))
    assertEquals(holding(third), Vector(LegacyState(b, active = false)))
    assertEquals(setup.ready.game.current.commonCards.legacyDeck, Vector.empty)
    val kept = notes(setup.events).filter(_.key ==
      SetupProcedure.keptLegacy.name).map(_.args)
    assertEquals(kept.toSet, seats.map(seat =>
      Vector[NoteArg](NoteArg.Player(seat.playerId))).toSet)
```

- In "in a first game each player keeps one dormant legacy and returns
  the other to the deck", add after the `seats.foreach { ... }` block,
  before Task 1's bottom-of-deck assertion:

```scala
    assert(seats.forall(seat =>
      ready.game.current.legacyHands.getOrElse(seat.playerId, Vector.empty)
        .isEmpty), "every hand is empty after the keeps")
```

In `LegacyDealSuite.scala`, replace the four tests that call `drawn` (the
Quiet Ambitions, Public Ambitions and short-deck ones) with:

```scala
  test("Quiet Ambitions deals no public row and leaves the whole order " +
      "as the deck"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Normal), seats)
    assertEquals(deal.publicRow(order), Vector.empty)
    assertEquals(deal.rest(order), order)

  test("Public Ambitions deals the first 4 faceup and leaves the rest"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Altered), seats)
    assertEquals(deal.publicRow(order), order.take(4))
    assertEquals(deal.rest(order), order.drop(4))

  test("a short deck deals what remains to the public row"):
    val short = order.take(3)
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .publicRow(short), short)
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .rest(short), Vector.empty)
```

Keep "a Chronicle that lists no Foundation III deals Quiet Ambitions" and
"the deck is every legacy no lineage holds, seated or in the box". If
`p2` and `p3` are now unused, change the `val Vector(p1, p2, p3)` line to
`private val p1 = seats.head.playerId`. Change the class comment to
"Foundation III's deal of the legacy deck, on both faces (Legacies design,
slice 2, rulings 3-5; catalog batch 13, ruling 6): the public row, and the
rest the game keeps as its deck."

In `LegacyDeckSuite.scala`, replace the first test's first assertion with
`assertEquals(deckOf(ready), orders.legacyOrder)` and rename the test
"Quiet Ambitions' game start keeps the whole order as the deck".

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacySetupSuite distantlands.gameplay.setup.LegacyDealSuite distantlands.gameplay.operations.LegacyDeckSuite"`
Expected: FAIL: game start still deals 2 legacies to each lineage, so the
deck is short of them and the short-deck walk still asks only the first
keeper.

- [ ] **Step 3: Stop dealing the draws at game start**

In `gameplay/setup/LegacyDeal.scala`:

- Delete `drawn` and its scaladoc.
- `rest` becomes:

```scala
  /** What the deal leaves of `order`, top first: the game's legacy deck.
    * Quiet Ambitions deals nothing at game start, since each keep draws its
    * own (catalog batch 13, rulings 1 and 6). */
  def rest(order: Vector[LegacyId]): Vector[LegacyId] =
    if public then order.drop(LegacyDeal.PublicRow) else order
```

- Replace the class scaladoc with:

```scala
/** Foundation III's deal of the legacy deck at game start (Legacies
  * design, slice 2; implementation ruling R16; catalog batch 13, ruling
  * 6). `seats` lists every seated player in seat order; `order` below is
  * always the deal's `legacyOrder`, the derived deck shuffled.
  *
  * Public Ambitions, the altered face: the first 4 are dealt faceup to the
  * public row and nobody draws. Quiet Ambitions, the normal face and every
  * first game's (ruling 4): game start deals nothing, and each player's
  * keep in Setup draws 2 from the deck (`LegacyKeep`). A short deck deals
  * or draws what remains (ruling 5).
  */
```

`seats` stays a field: `LegacyDeal.of` and the tests pass it. If the
compiler reports it unused, leave it; it is a public constructor field.

In `gameplay/setup/GameStartRules.scala`, the lineages no longer gain the
draw:

```scala
      // A seated lineage the Chronicle lists starts from its record, its
      // legacies with their stored faces; `LaterGameSetup` hands its
      // starting relics to its player. Quiet Ambitions' draw waits for the
      // keep in Setup (`LegacyKeep`).
      recorded = chronicle.lineages.map(lineage => lineage.id -> lineage).toMap
      lineages = plan.participants.map { participant =>
        participant.lineageId -> recorded.getOrElse(participant.lineageId,
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
      }.toMap
```

- [ ] **Step 4: Draw at the keep**

Replace the body of `object LegacyKeep` in
`gameplay/setup/LegacyKeep.scala` from the `step` scaladoc to the end of
the file with:

```scala
  /** Quiet Ambitions: draw 2 legacies from the top of the legacy deck,
    * keep one facedown and return the other to the bottom (CR "Set Up
    * Players"; Legacies design, slice 2; catalog batch 13, rulings 2 and
    * 6). The draw goes to the keeper's hand: Matchmaker's holder draws and
    * keeps for every other player (Legacy Setup powers ruling 13), and a
    * hand is private to its holder, so only the keeper sees the draw. The
    * keep is its own `Branch`, after the draw's, so a walk resumed at the
    * question reads the same hand. A short deck's single legacy is kept
    * without a question, and a player who draws none is asked nothing
    * (R9). Public Ambitions draws nothing. */
  def step(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    val lineage = participant.lineageId
    val decisionId = LegacyKeep.decisionId(owner)
    Sequence(Vector(
      Branch((state, _) =>
        if !quiet(state) then Vector.empty
        else LegacyDraws.top(state, LegacyDeal.QuietDraw) match
          case Vector() => Vector.empty
          case top => Vector(LegacyDraws.draw(
            SetupLegacyRules.keeper(state, owner), top))),
      Branch((state, _) =>
        val keeper = SetupLegacyRules.keeper(state, owner)
        LegacyDraws.hand(state, keeper) match
          case Vector() => Vector.empty
          case Vector(only) => Vector(
            BuildOps((_, _) => Right(Vector(keptMove(only, keeper, lineage))),
              required = true),
            keptNote(keeper, owner))
          case drawn => Vector(
            Decide(decisionId, keeper, DecisionQuery.ChooseOne(
              drawn.map(id =>
                DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
              heading = Some(
                if keeper == owner then "Keep a legacy"
                else s"Matchmaker: keep a legacy for ${owner.value}"),
              confirmVerb = Some("Keep"))),
            BuildOps(settled(lineage, keeper, drawn, decisionId),
              required = true),
            keptNote(keeper, owner)))))

  /** Whether Foundation III shows Quiet Ambitions, whose keep draws. */
  private def quiet(state: ReadyGame): Boolean =
    state.game.campaign.foundations.get(FoundationNumber.III)
      .forall(_.face == FoundationFace.Normal)

  private def keptNote(keeper: PlayerId, owner: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ => Some(
      if keeper == owner then keptLegacy.sourceless(NoteArg.Player(owner))
      else Matchmaker.keptFor.sourceless(NoteArg.Player(keeper),
        NoteArg.Player(owner), NoteArg.Card(MatchmakerCard.id))))

  /** The kept legacy, from the keeper's hand to the owner's lineage, with
    * no face: dormant. */
  private def keptMove(legacy: LegacyId, keeper: PlayerId,
      lineage: LineageId): Move =
    Move(Piece.Card(legacy), PositionedLocation(Location.Hand(keeper)),
      PositionedLocation(Location.Legacies(lineage)))

  private def settled(lineage: LineageId, keeper: PlayerId,
      drawn: Vector[LegacyId], decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      pending.answered.find(_.decisionId == decisionId).map(_.answer) match
        case Some(DecisionAnswer.ChooseOneAnswer(
            DecisionOptionRef.Legacy(kept))) if drawn.contains(kept) =>
          Right(keptMove(kept, keeper, lineage) +: drawn.filterNot(_ == kept)
            .map(id => Move(Piece.Card(id),
              PositionedLocation(Location.Hand(keeper)),
              PositionedLocation.legacyDeckBottom)))
        case _ => Left(InvalidEventOrder(
          s"no legacy answer is recorded for $decisionId"))
```

Add `import distantlands.gameplay.powers.LegacyDraws` to the file's
imports, and change the object's scaladoc first sentence to "Foundation
III's Quiet Ambitions keep in Setup's walk: each player draws two legacies
from the legacy deck, keeps one facedown and returns the other to the
bottom (Legacies design, slice 2; catalog batch 13, ruling 6)."

- [ ] **Step 5: Drop the projection's Matchmaker arm**

In `application/GamePresentationProjector.scala`:

- The lineage-legacies arm of `identifiesCard` becomes:

```scala
      // An active legacy is faceup for all; a dormant one is facedown in its
      // owner's play area, and only they know it (Legacies design, ruling 1).
      // In the Chronicle Phase the game is over and goals are checked
      // openly, so every legacy is named to everyone (implementation ruling
      // R18); after Resolve Legacies the only dormant ones were active. A
      // legacy Matchmaker's holder draws for another player waits in the
      // holder's hand, which the hand arm names to them alone (catalog
      // batch 13, ruling 6).
      case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
        faceup || ready.game.current.turn.phase == Phase.Chronicle ||
          viewer.exists(player => ready.game.current.players
            .exists(seat => seat.player == player && seat.lineage == lineage))
```

- In the scaladoc list above `identifiesCard`, delete the sentence "A
  draw Matchmaker's holder keeps for another player is named to the holder
  alone while that keep is the parked decision, and to nobody before
  (R16)."
- Delete `keepPending` and its scaladoc. Keep `setupPowers` and
  `setupOpen`: `deckQuestionPending` still reads them.
- Remove `LegacyKeep` from the file's imports if nothing else uses it.

In `setup/legacies/Matchmaker.scala`, replace "the holder sees both drawn
legacies while that keep is open, and the owner only the one kept for them
(`LegacyKeep.matchmade`, R16)." with "the holder draws them into their own
hand, so they see both and the owner only the one kept for them (catalog
batch 13, ruling 6)."

- [ ] **Step 6: Move the remaining tests to the helper**

Each of these computes a player's draw with `LegacyDeal...drawn(order,
player)`. Replace it with `SetupLegacyFixture.drawn(orders, player)`, where
`orders` is the `SetupOrders` the test starts the game with (pass
`orders.copy(legacyOrder = order)` when the test overrides the order), and
delete any `LegacyDeal.of(...)` value left unused:

- `gameplay/setup/legacies/MatchmakerSuite.scala` (`deal`, `drawn`)
- `application/MatchmakerProjectionSuite.scala` (`drawn`; `own` in
  "without Matchmaker the owner identifies their own draw", with
  `plainOrders`)
- `application/GameApplicationServiceProjectionSuite.scala`
  (`drawnLegacies`)
- `application/gamelog/CombinedLegaciesRedactionSuite.scala` (`deal`,
  `drawn` in "no log names a legacy Matchmaker's holder kept for another
  player")

In `gameplay/setup/GameStartRulesSuite.scala`, "a seated lineage the
Chronicle lists starts from its record, and an unlisted one starts as a
fresh Exile": delete the `drawn` helper and its comment, and assert

```scala
    assertEquals(ready.game.campaign.lineages(listed), record)
    assertEquals(ready.game.campaign.lineages(unlisted), LineageState(unlisted,
      None, Role.Exile, Vector.empty, Vector.empty))
```

In `gameplay/setup/ExileOnlySetupSuite.scala`, the test at line 219 asserts
each seated lineage's legacies at game start: replace the `deal` value and
the `foreach` with

```scala
    orders.participants.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies, Vector.empty))
```

and keep its baseline assertion.

`MatchmakerProjectionSuite`'s assertions keep their meaning: before p2's
keep the draw is still in the deck, while it waits it is in p3's hand, and
once kept it is on p2's lineage. They need no other change.

- [ ] **Step 7: Run the full suite and check the goldens**

Run: `./sbtw test`
Expected: PASS, except possibly `GameLogGoldenSuite`. The keep's lines are
unchanged, but a player now draws other legacies than before when the
first player is not the first seat, so a golden that names a kept legacy
to its owner, or an active legacy dealt in a later game, may move. If
`GameLogGoldenSuite` fails, regenerate:

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Then read `git diff src/test/resources/gamelog` line by line. Accept only
lines whose legacy names changed because a different legacy was drawn.
Any new, lost or reordered line is a regression: stop and report it.

Any other failure that reads a drawn legacy off a lineage before the keep
pinned the old deal: point it at `SetupLegacyFixture.drawn` or at the
keeper's hand, and name it in the commit message.

- [ ] **Step 8: Commit**

```bash
git add src/main src/test
git commit -m "feat(setup): draw Quiet Ambitions' legacies into the keeper's hand at the keep

Game start deals no legacy to a lineage; each keep draws 2 from the top of
the legacy deck into the keeper's hand, as Ancient Writ does, so
Matchmaker's holder sees another player's draw in their own hand. The
Setup-only Matchmaker visibility rule is deleted, and the Matchmaker
question names the player it keeps for (catalog batch 13, ruling 6).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 6: Record the rulings and close the slice

**Files:**
- Modify: `docs/rules/ambiguities.md` ("Legacies rulings" items 3 and 5;
  "Legacy Setup powers rulings" item 29; "Catalog batch 13 rulings")
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacies-implementation-rulings.md`
  (R3, R6, R13)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`
  (R16)
- Modify: `docs/rules/rules-reference.md` (after the Reliquary Raid bullet)
- Modify: `docs/rules/implementation-traceability.md` (row "Legacy SETUP
  powers in the foundations", last column; row "Legacies on the table")
- Modify: `docs/ROADMAP.md` ("Cards that act on legacies"; "Retire the
  unimplemented-card machinery")
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md`
  (status line)

**Interfaces:**
- Consumes: Tasks 1 to 5's behaviour and lines.
- Produces: "Catalog batch 13 rulings" items 2 to 7, which slices 3 to 6
  continue from item 8.

- [ ] **Step 1: Amend Legacies ruling 3**

In `docs/rules/ambiguities.md`, replace Legacies rulings item 3 with:

```markdown
3. **The legacy deck is derived between games and stored during one.** Between games it is every catalog legacy that no lineage holds, seated or in the box, and that is not in the public row. Game start shuffles it, deals the public row from it, and keeps the rest in order; a returned or discarded legacy goes to its bottom, and a draw during a game takes from its top ([Catalog batch 13 rulings](#catalog-batch-13-rulings), items 2 and 3). The Chronicle stores no deck, and Sun step 5, Shuffle, has nothing to do (design, ruling 3; amended by catalog batch 13, ruling 2).
```

- [ ] **Step 2: Add the batch 13 slice 2 rulings**

In `docs/rules/ambiguities.md`, under `## Catalog batch 13 rulings`, change
the intro's "item 1 is ruling 7." to "item 1 is ruling 7; items 2 to 7 are
rulings 1, 2, 6, 8, 9 and 10, as amended by the owner on 2026-11-15." and
append after item 1:

```markdown
2. **The game stores the legacy deck.** Game start keeps what Foundation III's deal leaves of the shuffled deck. A draw during a game takes the top legacies into the drawer's hand; a short deck draws what remains, and an empty deck draws nothing and writes "The legacy deck was empty." The deck is not shuffled during a game. A draw is a hidden outcome, so undo never takes it back.
3. **Discarded legacies go to the bottom of the legacy deck:** Quiet Ambitions' return, Iron Hand's discard during a game and The Mouth's, Ancient Writ's two, and the Sun's returns. A discarded legacy comes back only once every legacy above it is drawn.
4. **Drawn legacies wait in the drawer's hand,** which only the drawer sees. The kept one joins the owner's lineage, dormant, and the hand is empty again. The legacy deck is not shown. Quiet Ambitions' keep draws the same way: game start deals no legacy to a lineage, and each keep draws 2 from the top into the keeper's hand, Matchmaker's holder's when they keep for another player, whose question names that player ("Matchmaker: keep a legacy for p2"). The keeps draw in turn order from the first player, so a short deck can give a later player a legacy an earlier keep returned.
5. **Ancient Writ.** ACTION: bury it, draw 3 legacies, keep 1 dormant and discard the others to the bottom of the legacy deck. A single drawn legacy is kept without a question, and an empty deck asks nothing. Lines: "p1 drew 3 legacies." and "p1 kept a legacy.", never naming the legacy.
6. **Matchmaker during a game.** When another seated player holds Matchmaker active, they draw Ancient Writ's legacies into their own hand and keep one for the Writ's holder, off turn; the question names that player, "Matchmaker: keep a legacy for p1". The owner never identifies the legacies not kept. A holder using Ancient Writ themself chooses for themself. Lines: "p3 drew 3 legacies for p1 with Matchmaker." and "p3 kept a legacy for p1 with Matchmaker."
7. **Only Ancient Writ triggers Matchmaker** during a game: Amber Doors is excluded by Matchmaker's printed text, and Lost Doors' discard is the owner's choice.
```

- [ ] **Step 3: Amend R3 and R6**

In `docs/superpowers/specs/2026-11/2026-11-13-legacies-implementation-rulings.md`,
read the paragraphs under `**R3. A move into the legacy deck names no
position.**` and `**R6. A face on a move into the public row or the legacy
deck.**`. Add one paragraph at the end of R3:

```markdown
*Superseded 2026-11-15* by [catalog batch 13](2026-11-14-catalog-batch-13-design.md), ruling 2: the game stores the deck in order, and a move into it names the bottom.
```

and one at the end of R6:

```markdown
*Amended 2026-11-15* by [catalog batch 13](2026-11-14-catalog-batch-13-design.md), ruling 2: the move into the deck names the bottom; it still never names a facedown face.
```

In the same file, add at the end of R13 (`**R13. Which legacies the keep
decision offers.**`):

```markdown
*Superseded 2026-11-15* by [catalog batch 13](2026-11-14-catalog-batch-13-design.md), ruling 6: the keep draws its own legacies into the keeper's hand and offers what that hand holds.
```

In `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`,
add at the end of the R16 bullet (`- **R16. When Matchmaker's holder sees
another player's draw.**`), as its last sentence:

```markdown
*Superseded 2026-11-15* by [catalog batch 13](2026-11-14-catalog-batch-13-design.md), ruling 6: the holder draws into their own hand, so the hand rule names the draw to them alone.
```

In `docs/rules/ambiguities.md`, Legacies rulings item 5, replace "in seat
order," with:

```markdown
in turn order from the first player, each keep drawing from the top as it is reached ([Catalog batch 13 rulings](#catalog-batch-13-rulings), item 4),
```

and replace Legacy Setup powers rulings item 29 with:

```markdown
29. **Matchmaker's holder sees another player's draw because it is in their hand:** they draw it there at that player's keep, and nobody else sees a hand. Once kept, the legacy is named to its owner alone, and the returned one to nobody (R16, superseded by [Catalog batch 13 rulings](#catalog-batch-13-rulings), item 4).
```

- [ ] **Step 4: Add the Digital note**

In `docs/rules/rules-reference.md`, after the bullet beginning
`- **Reliquary Raid.**`, add:

```markdown
- **Ancient Writ.** ACTION: bury it to draw 3 legacies, keep 1 facedown and discard the other 2. Digital: the draw takes the top of the legacy deck into the drawer's hand, and the discards go to its bottom; a short deck draws what remains; another seated player holding Matchmaker active draws and keeps for the Writ's holder ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 2 to 7).
```

- [ ] **Step 5: Update the traceability rows**

In `docs/rules/implementation-traceability.md`:

- Row "Legacy SETUP powers in the foundations", last column: replace
  "Matchmaker on draws during a game (Amber Doors, Lost Doors, Ancient
  Writ) waits for its batch." with "Matchmaker during a game keeps
  Ancient Writ's draw for its holder (`powers/action/AncientWritSuite`,
  `application/AncientWritProjectionSuite`), and draws Setup's keep into
  their own hand (`setup/LegacySetupSuite`,
  `application/MatchmakerProjectionSuite`); the Doors wait for catalog
  batch 13, slice 5."
- Same row, third column: replace "seeing that draw only while the keep is
  the parked decision" with "drawing it into their own hand".
- Row "Legacies on the table", third column: append this sentence (one
  line in the table cell):

```markdown
The game stores the legacy deck in order, a returned legacy goes to its bottom, and a drawn legacy waits in its drawer's hand, named to them alone (`operations/LegacyDeckSuite`, `operations/LegacyHandSuite`, `application/LegacyHandProjectionSuite`; [Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 2 to 4).
```

- [ ] **Step 6: Update the ROADMAP**

In `docs/ROADMAP.md`:

- "Cards that act on legacies": replace its text after the bold title
  with:

```markdown
The non-legacy cards whose powers name legacies, still in
  holding: Amber Doors and Lost Doors (E05, draw legacies when explored or
  at setup), Ruined Temple (E10, double the legacy points), and Stone Baths
  and Ruined Baths (E04, change Resolve Legacies).
  [Catalog batch 13](superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md)
  plans them; Ancient Writ and Matchmaker's draw during a game landed in its
  slice 2.
```
- "Retire the unimplemented-card machinery across the stack": change "Ten
  powers remain" to "Nine powers remain", and "the E07, E16 and E26 ruined
  faces, Ancient Grit and the Ancient City's enduring power, which no
  design covers yet" to "the E07, E16 and E26 ruined faces and the Ancient
  City's enduring power, which no design covers yet".

- [ ] **Step 7: Mark the slice landed in the spec**

In the spec's `**Status:**` paragraph, after "with it every printed denizen
power is implemented.", add "Slice 2 landed 2026-11-15." (keep the rest of
the paragraph).

- [ ] **Step 8: Check the links and the architecture**

Run: `python3 scripts/check-markdown-links.py`
Expected: "Markdown link check passed".
Run: `python3 scripts/check-architecture.py`
Expected: "architecture check passed".

- [ ] **Step 9: Commit**

```bash
git add docs
git commit -m "docs(catalog): record the legacy deck and Ancient Writ rulings and close batch 13 slice 2

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
