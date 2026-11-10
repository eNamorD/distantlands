# Catalog Batch 11, Slice 0: Room in CoreOperations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring `src/main/scala/distantlands/model/CoreOperations.scala`
from 787 lines to at most 720, with no change in behaviour, so that
slices 3 and 7 have room for their operations under the 800-line cap.

**Architecture:** The sealed `CoreOperation` family must stay in its file,
so only code that is not a direct subtype of a sealed trait moves. The
helpers that build composites' children move to a new
`private[model] object OperationChildren`. The two card-play hooks, which
extend the unsealed `Operation`, move to `CardPlayHooks.scala`. A stale
16-line banner comment is rewritten in 7 lines. Everything stays in package
`oathdigital.model`, so no import anywhere changes.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read "Goal and scope" and "Slice 0: Room in CoreOperations".

## Plan-time findings

1. **What may move.** Scala 3 keeps every direct subtype of a sealed trait
   in the trait's file. `CoreOperation`, `PrimitiveOperation`, `Burn`,
   `Discard` and `Gain` are sealed in `CoreOperations.scala`, so their cases
   stay, and so do the companions of case classes (`object Bury`). Free to
   move: the private helpers at the end of `object Discard` (lines
   214-251), `PayCost`'s two move builders (lines 370-378), and
   `CardPlayedFaceup` and `CardPlayedFacedown` (lines 767-786), which
   extend the unsealed `Operation`.
2. **The helpers have no callers outside `model`.** `discardWorld`,
   `returnedResources`, `returnedSecrets` and `positiveMove` are private to
   `object Discard`. `Discard.returns` is `private[model]` and is called by
   `Bury.standard`, `Discard.Buried` and `RemoveFromMap` only. (The
   `returnedSecrets` in `RestCleanup` and `ChallengeRibbon` are unrelated
   names.)
3. **The hooks need no import changes.** The 28 files that name
   `CardPlayedFaceup` or `CardPlayedFacedown` import `oathdigital.model._`
   or sit in the package, and the classes stay in that package.
4. **The banner at lines 481-496 is stale.** It says power windows are "not
   wired in this slice", but `Roll` and `ModifyDicePool` now take a
   `window`. The rewrite keeps the two facts that still hold: these cases
   must stay in the file, and each is a leaf.
5. **A precedent.** `BuryableCard.scala` was split out of this file for
   the same reason, and its doc comment says so.
6. **Coverage of the moved code.** `CoreOperationsSuite` pins the leaves of
   `Discard.Denizen` and `Discard.RuinedEdifice`, and
   `OperationVocabularySuite` pins `Bury.standard`, but no test pins
   `PayCost`'s burnt or off-turn leaves, `Discard.Buried` or
   `Discard.Relic`. Task 1 adds a characterization test for them before
   anything moves. It passes on today's code; its job is to fail if the
   move changes a leaf.
7. **Line budget.** Removing lines 213-251 saves 39, the `PayCost`
   builders 3, the hooks 22 (with the blank lines around them) and the
   banner 9. The import in `object Discard` and the two rewrapped
   `returns` calls add 3. That leaves the file at about 717 lines, 83 under
   the cap.

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-0`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- No behaviour changes: every existing test passes unchanged.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.

---

### Task 1: Move the child builders to OperationChildren

**Files:**
- Create: `src/main/scala/distantlands/model/OperationChildren.scala`
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala`
  (`object Bury` 109-122, `object Discard` 125-251, `RemoveFromMap`
  260-268, `PayCost` 370-378)
- Test: `src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala`

**Interfaces:**
- Produces: `private[model] object OperationChildren` with
  `discardWorld(card: WorldCardId, from: PositionedLocation, destination:
  Region, by: Option[PlayerId], position: StackPosition):
  Vector[Operation]`, `returns(card: CardId, suit: Option[Suit], favor:
  Int, secrets: Int, actingPlayer: PlayerId): Vector[CoreOperation]`,
  `returnedResources(card: CardId, suit: Suit, favor: Int, secrets: Int,
  actingPlayer: PlayerId): Vector[Operation]`, `returnedSecrets(card:
  CardId, amount: Int, actingPlayer: PlayerId): Vector[Operation]` and
  `fromPlayArea(player: PlayerId, amount: Int, piece: Int => Piece, to:
  Location): Vector[Operation]`. Nothing outside `model` may call them.

- [ ] **Step 1: Write the characterization test**

Add this test to `CoreOperationsSuite`, after the test "Discard routes
cards and their resources by glossary rules". The suite already defines
`red`, `redArea` and `site`, and imports `oathdigital.model._`.

```scala
  test("a payment, a buried discard and a relic discard keep their leaves"):
    val card = DenizenId("denizen:paid")
    val relic = RelicId("relic:set-aside")
    val from = PositionedLocation(site)
    val onCard = PositionedLocation(Location.OnCard(card))
    val payerArea = PositionedLocation(redArea)

    assertEquals(Operation.flatten(PayCost(red, Location.OnCard(card),
      Cost(favor = 1, secret = 1, favorBurnt = 2, secretBurnt = 1))), Vector(
      Move(Piece.Favor(1), payerArea, onCard),
      Move(Piece.Secrets(1), payerArea, onCard),
      Move(Piece.Favor(2), payerArea,
        PositionedLocation(Location.SharedBank)),
      Move(Piece.Secrets(1), payerArea,
        PositionedLocation(Location.SharedBank))))

    assertEquals(Operation.flatten(PayCost(red, Location.OnCard(card),
      Cost(favor = 1, secret = 2), matchingBank = Some(Suit.Order),
      offTurn = true)), Vector(
      Move(Piece.Favor(1), payerArea,
        PositionedLocation(Location.FavorBank(Suit.Order))),
      FlipSecrets(red, 2, SecretSide.FaceUp, SecretSide.FaceDown)))

    assertEquals(Operation.flatten(Discard.Buried(BuryableCard.Denizen(card),
      from, Some(Suit.Order), favor = 1, secrets = 1, red)), Vector(
      Move(Piece.Favor(1), onCard,
        PositionedLocation(Location.FavorBank(Suit.Order))),
      Move(Piece.Secrets(1), onCard, payerArea),
      FlipSecrets(red, 1, SecretSide.FaceUp, SecretSide.FaceDown),
      Bury(BuryableCard.Denizen(card), from)))

    assertEquals(Operation.flatten(Discard.Relic(relic, from, secrets = 1,
      red)), Vector(
      Move(Piece.Card(relic), from,
        PositionedLocation(Location.SetAsideRelics), by = Some(red)),
      Move(Piece.Secrets(1), PositionedLocation(Location.OnCard(relic)),
        payerArea),
      FlipSecrets(red, 1, SecretSide.FaceUp, SecretSide.FaceDown)))
```

- [ ] **Step 2: Run it on today's code**

Run: `./sbtw "testOnly oathdigital.gameplay.CoreOperationsSuite"`
Expected: PASS. If it fails, the expected leaves above misread today's
code: fix the test to match today's leaves, never the code, and say so in
the commit message.

- [ ] **Step 3: Create `OperationChildren.scala`**

```scala
package oathdigital.model

/** Builds the children of the composite operations in
  * `CoreOperations.scala`: a world card's discard move, the favor and
  * secrets a card returns as it leaves play, and a payment out of a
  * player's play area. Split out of that file, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound
  * (catalog batch 11, slice 0).
  */
private[model] object OperationChildren:
  def discardWorld(card: WorldCardId, from: PositionedLocation,
      destination: Region, by: Option[PlayerId],
      position: StackPosition): Vector[Operation] =
    Vector(Move(Piece.Card(card), from,
      PositionedLocation(Location.RegionalDiscard(destination), position),
      resultingOrientation = Some(Orientation.FaceDown), by = by))

  // The suit fallback is never reached with a positive favor: positiveMove
  // returns empty for zero and Bury.standard requires the suit.
  /** The returns a discard makes: favor to `suit`'s bank and secrets to
    * `actingPlayer`, facedown. */
  def returns(card: CardId, suit: Option[Suit], favor: Int,
      secrets: Int, actingPlayer: PlayerId): Vector[CoreOperation] =
    (positiveMove(favor)(Piece.Favor.apply,
      PositionedLocation(Location.OnCard(card)),
      PositionedLocation(Location.FavorBank(suit.getOrElse(Suit.Arcane)))) ++
      returnedSecrets(card, secrets, actingPlayer)).collect:
      case operation: CoreOperation => operation

  def returnedResources(card: CardId, suit: Suit, favor: Int,
      secrets: Int, actingPlayer: PlayerId): Vector[Operation] =
    positiveMove(favor)(Piece.Favor.apply,
      PositionedLocation(Location.OnCard(card)),
      PositionedLocation(Location.FavorBank(suit))) ++
    returnedSecrets(card, secrets, actingPlayer)

  def returnedSecrets(card: CardId, amount: Int,
      actingPlayer: PlayerId): Vector[Operation] =
    if amount == 0 then Vector.empty
    else Vector(
      Move(Piece.Secrets(amount), PositionedLocation(Location.OnCard(card)),
        PositionedLocation(Location.PlayArea(actingPlayer))),
      FlipSecrets(actingPlayer, amount, SecretSide.FaceUp,
        SecretSide.FaceDown))

  /** `amount` of `piece` leaving `player`'s play area for `to`, or nothing
    * for a zero amount. */
  def fromPlayArea(player: PlayerId, amount: Int, piece: Int => Piece,
      to: Location): Vector[Operation] =
    positiveMove(amount)(piece,
      PositionedLocation(Location.PlayArea(player)), PositionedLocation(to))

  private def positiveMove(amount: Int)(piece: Int => Piece,
      from: PositionedLocation,
      to: PositionedLocation): Vector[Operation] =
    if amount == 0 then Vector.empty
    else Vector(Move(piece(amount), from, to))
```

- [ ] **Step 4: Point `CoreOperations.scala` at it**

Make these edits, each an exact replacement:

1. In `object Bury`, replace

   ```scala
       Discard.returns(card.id, suit, favor, secrets, actingPlayer) :+
   ```

   with

   ```scala
       OperationChildren.returns(card.id, suit, favor, secrets,
         actingPlayer) :+
   ```

2. Replace the first line of `object Discard`

   ```scala
   object Discard:
     /** Discards a denizen facedown to the specified regional pile and returns
   ```

   with

   ```scala
   object Discard:
     import OperationChildren.*
     /** Discards a denizen facedown to the specified regional pile and returns
   ```

   The calls inside `Denizen`, `Vision`, `Buried`, `RuinedEdifice` and
   `Relic` (`discardWorld`, `returnedResources`, `returns`,
   `returnedSecrets`) then resolve to `OperationChildren` unchanged.

3. Delete the blank line after `Discard.Relic`'s `returnedSecrets(card,
   secrets, actingPlayer)` and the five private helpers that follow it,
   from `  private def discardWorld(card: WorldCardId, from:
   PositionedLocation,` through `    else Vector(Move(piece(amount), from,
   to))`, ending the object at `Relic`. Keep one blank line between the
   object and the `RemoveFromMap` doc comment.

4. In `RemoveFromMap`, replace

   ```scala
       Discard.returns(card, Some(suit), favor, secrets, actingPlayer) :+
   ```

   with

   ```scala
       OperationChildren.returns(card, Some(suit), favor, secrets,
         actingPlayer) :+
   ```

5. In `PayCost`, replace

   ```scala
     private def favorMove(amount: Int, to: Location): Vector[Operation] =
       if amount == 0 then Vector.empty
       else Vector(Move(Piece.Favor(amount),
         PositionedLocation(Location.PlayArea(player)), PositionedLocation(to)))

     private def secretMove(amount: Int, to: Location): Vector[Operation] =
       if amount == 0 then Vector.empty
       else Vector(Move(Piece.Secrets(amount),
         PositionedLocation(Location.PlayArea(player)), PositionedLocation(to)))
   ```

   with

   ```scala
     private def favorMove(amount: Int, to: Location): Vector[Operation] =
       OperationChildren.fromPlayArea(player, amount, Piece.Favor.apply, to)

     private def secretMove(amount: Int, to: Location): Vector[Operation] =
       OperationChildren.fromPlayArea(player, amount, Piece.Secrets.apply, to)
   ```

The import inside `object Discard` brings `returns` into scope beside
`Discard`'s own members. No member of `Discard` is named `returns`,
`discardWorld`, `returnedResources` or `returnedSecrets` after step 3, so
nothing is shadowed.

- [ ] **Step 5: Run the operation suites**

Run: `./sbtw "testOnly oathdigital.gameplay.CoreOperationsSuite oathdigital.gameplay.OperationVocabularySuite oathdigital.gameplay.operations.* oathdigital.serialization.*"`
Expected: PASS, with no compiler warning (`-Werror`).

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/OperationChildren.scala src/main/scala/distantlands/model/CoreOperations.scala src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala
git commit -m "$(cat <<'EOF'
refactor(model): build composite operations' children in OperationChildren

The discard, return and payment move builders leave CoreOperations.scala,
which is close to the 800-line cap. A characterization test pins the
leaves of a payment, a buried discard and a relic discard first.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

### Task 2: Move the card-play hooks, trim the banner, close the item

**Files:**
- Create: `src/main/scala/distantlands/model/CardPlayHooks.scala`
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (the
  walker banner at 481-496, the hooks at the end of the file)
- Modify: `docs/ROADMAP.md`, `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `CardPlayedFaceup` and `CardPlayedFacedown`, unchanged in
  name, package, fields and behaviour.

- [ ] **Step 1: Create `CardPlayHooks.scala`**

Move the two classes verbatim, doc comments included:

```scala
package oathdigital.model

// The card-play hooks are power windows, not Oath operations: they extend
// the unsealed [[Operation]], so they live outside `CoreOperations.scala`
// (catalog batch 11, slice 0).

/** Semantic window for a card played faceup, to a site or as a faceup
  * adviser. `byPower` is the power whose effect made the play (Search
  * Party, Temple of Ancestors, Oracle), `None` for an action's own play.
  * Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFaceup(card: WorldCardId,
    resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFaceup)
  override val children: Vector[Operation] = Vector.empty

/** Semantic window for a card (a denizen or a Vision) placed facedown as an
  * adviser by `player`. `byPower` is as for [[CardPlayedFaceup]]. A discard
  * emits neither hook. Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFacedown(card: WorldCardId, player: PlayerId,
    byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFacedown)
  override val children: Vector[Operation] = Vector.empty
```

Then delete both classes, their doc comments and the blank lines around
them from the end of `CoreOperations.scala`, so the file ends with
`ClosePick` and one newline.

- [ ] **Step 2: Rewrite the walker banner**

In `CoreOperations.scala`, replace the 16 lines from the first
`// ------...` line after `Take` through the closing `// ------...` line
(they begin "Walker leaves and composites." and end "once power windows
land.") with:

```scala
// ---------------------------------------------------------------------------
// Walker leaves. Each extends PrimitiveOperation, so it is a leaf
// (`children = Vector(this)`), and must stay in this file: Scala keeps a
// sealed trait's direct subtypes in the trait's file. Dice-pool counts live
// in `CurrentGameState.rollPools`; a `Roll` records that a roll happens,
// and the walker or the next command supplies its faces.
// ---------------------------------------------------------------------------
```

- [ ] **Step 3: Check the line count and the gates**

```bash
wc -l src/main/scala/distantlands/model/CoreOperations.scala
```

Expected: at most 720 (about 717).

```bash
python3 scripts/check-architecture.py
```

Expected: passes.

- [ ] **Step 4: Close the roadmap item and mark the slice**

In `docs/ROADMAP.md`, delete the whole "Left behind by catalog batch 10" group under **Phase - Cleanup tasks**: its intro line and the
**Make room in `CoreOperations.scala`** item, which is its only item, plus
the blank line after it (memory: "Roadmap deletes done work").

In `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md`, replace
the status line

```markdown
**Status:** designed 2026-11-07. No slice has landed.
```

with

```markdown
**Status:** designed 2026-11-07. Slice 0 landed.
```

Then run:

```bash
python3 scripts/check-markdown-links.py
```

Expected: passes.

- [ ] **Step 5: Run the full gates**

```bash
./sbtw frontend/test
```

```bash
./sbtw clean coverage test coverageReport
```

Run the second in the background. Expected: every test passes, and
statement coverage is at least 92%.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/CardPlayHooks.scala src/main/scala/distantlands/model/CoreOperations.scala docs/ROADMAP.md docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md
git commit -m "$(cat <<'EOF'
refactor(model): move the card-play hooks out of CoreOperations.scala

CardPlayedFaceup and CardPlayedFacedown extend the unsealed Operation,
so they need not sit with the sealed family. The walker banner loses its
stale note about unwired windows. CoreOperations.scala now has room for
batch 11's operations, which closes the roadmap's clean-up item.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
