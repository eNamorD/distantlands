# Catalog Batch 11, Slice 6: Desecrated Ground Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Desecrated Ground (edifice E08, ruined): "If an Exile
reveals a Vision while their pawn is at this site, they do not discard
their current revealed Vision. _They can have any number!_"

**Architecture:** Three seams, then the card.

1. **Several revealed Visions.** `PlayerState.revealedVision:
   Option[VisionState]` becomes `revealedVisions: Vector[VisionState]`, in
   reveal order, through every state write, the card index, the role
   change, card play, the projection and the frontend board. A reveal
   still discards every Vision already there; only Desecrated Ground keeps
   them.
2. **One victory read path.** `RevealedVisions.of(ready, player)` is what
   the victory reads ask, so slice 7 can add claimed Visions in one place.
3. **A win names every qualifying Vision.** `VisionVictory`,
   `WarExhaustionResolved` and `GameResult` carry `visions:
   Vector[VisionId]`. Swear an Oath asks the winner which one won when
   there are several (ruling 30).

The card is a rule, `DesecratedGroundRule.keeps`, that `CardSlots` and
`CardPlay` read. A registered power with no window makes the card count
as implemented, and the card play writes a "Kept" line (ruling 31).

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read rulings 13, 30 and 31, the Desecrated Ground bullet of "Readings",
and "Slice 6: Several revealed Visions".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, no non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`). This is why the rule lives in
  `gameplay`, not beside the Sacred Ground power.
- Statement coverage stays at or above 92%.
- Wire changes need not keep old journals decodable: the new fields are
  required.
- Log lines name the actor only when they are not the walk's actor. Power
  lines end with a period; procedure lines do not.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Prerequisite

This branch (`batch-11-slice-6`) was cut from `0.2.0-alpha` at
`33c7be12`, after slice 4 landed. It does not need slice 5: the spec's
slice order says only slice 7 reads this slice's seam. Slice 5's plan
(branch `batch-11-slice-5`) also edits the spec, `ROADMAP.md`,
`unimplemented.txt`, `declared-keys.txt` and `WalkerPowerCatalog`, in
other lines. Whichever slice lands second rebases and keeps both sides.
The spec's ruling header names rulings 26 to 29 as slice 5's, and this
slice's rulings are numbered 30 and 31, so no ruling number clashes.

## Plan-time findings

Inventoried at `33c7be12`.

1. **24 production sites read or write `revealedVision`.** Each is named in
   the task that changes it:

   | File | Line | Task |
   |---|---|---|
   | `model/GameState.scala` | 22 | 1 |
   | `model/CardIndex.scala` | 161 | 1 |
   | `gameplay/operations/OperationStateWrites.scala` | 85-88 | 1 |
   | `gameplay/operations/CardMovementOperations.scala` | 252, 379-381 | 1 |
   | `gameplay/operations/OperationExecutor.scala` | 81 | 1 |
   | `gameplay/roles/RoleChanges.scala` | 136 | 1 |
   | `gameplay/actions/CardPlay.scala` | 190, 205 | 1, 3 |
   | `gameplay/actions/cardplay/CardSlots.scala` | 344 | 1, 3 |
   | `gameplay/VisionVictoryEligibility.scala` | 11 | 1, 4 |
   | `gameplay/StateBasedEvaluation.scala` | 69, 214 | 1, 4 |
   | `gameplay/chronicle/SwearOath.scala` | 40 | 1, 4 |
   | `application/GamePresentationProjector.scala` | 187 | 2 |
   | `shared/.../ActionProjectionDtos.scala` | 323 | 2 |
   | `shared/.../WorldProjectionCodec.scala` | 223, 238, 249 | 2 |
   | `frontend/.../WorldBoardRenderer.scala` | 113 | 2 |

   `DecisionOptionRef.RevealedVision(owner)` and `PlayerCardArea.RevealedVision`
   name the slot, not one card. They stay.
2. **The movement write refuses a second Vision.**
   `CardMovementOperations.scala:378-381` fails with `ConflictingDeltas("revealed
   Vision slot is occupied")`. No test pins that message. Card play is the
   only producer of a faceup Vision move to a play area, and it discards
   the old Visions first in the same batch (`CardPlay.operations`,
   `fromPlay` before `plan.kept`). The guard goes, and the write appends.
3. **Imperials have no Vision slot** (`World.scala:10`, `hasVisionSlot =
   !isImperial`), so only an Exile can reveal a true Vision. The rule
   still checks for an Exile, because the card's text says so and slice 7
   adds reveals in other players' walks.
4. **Card play cannot import the Sacred Ground power.** `SacredGroundCard`
   lives in `gameplay/powers/cardplay/SacredGround.scala`, and `CardPlay`
   and `CardSlots` are in `gameplay.actions`. Grand Mask set the precedent
   (`gameplay/GrandMask.scala`): the card and its rule live in `gameplay`,
   and a power with an empty `contributions` map in `gameplay/powers` is
   registered in `WalkerPowerCatalog` so `PowerImplementationStatus`
   counts the card.
5. **`Replacing` wraps one card.** `DecisionOption.Replacing(option, card)`
   is built in four places and read in two: `DecisionOptionRows.scala:109`
   and `ChoiceWords.scala:80`. Several Visions are replaced by nesting
   one `Replacing` per Vision. `ChoiceWords` already unwraps recursively.
   `DecisionOptionRows` must collect the chain, or the outer wrapper's
   label names the last Vision only.
6. **The Wake win is not a walk.** `OathDriver.scala:136` journals
   `VisionVictory` from `StateBasedEvaluation.visionAtWake` with no
   decision seam. Ruling 30 therefore asks at Swear an Oath, which is a
   Chronicle walk step and already holds a `Decide`.
7. **`visionaryWin` checks the player, not the Vision.**
   `StateBasedEvaluation.scala:212-216` iterates Visions in printed
   priority and finds a player who has that Vision and `qualifies`. With
   one Vision per player the two agree. With several they would not: a
   player holding an unmet Conquest and a met Faith would be found at
   Conquest's priority. Task 4 checks the Vision itself.
8. **Swear an Oath reads the winner's revealed Vision.** `GameResult`
   records no Vision (`GameState.scala:186`). Task 4 adds `visions` to it,
   set from the event, so the Chronicle reads what won and not a state
   that the Chronicle may have changed.
9. **The play's tree has a fixed shape.** `CardPlayProcedure.played`
   (`:446-476`) and `settled` (`:497-512`) build `Sequence(lines, play,
   Note(playedNote))`, and no path addresses into the third child or
   beyond. The kept line is a fourth child in both. It reads only
   `states.now`: after the play, the player's revealed Visions other than
   the played card are exactly the kept ones, because a reveal elsewhere
   has just discarded them.
10. **Test churn.** 42 lines in 17 test files read or write the field;
    `Table.revealedVision` and `Look(...).player(...).revealedVision` carry
    most of them. `Table.revealedVision` appends, so a test can stack two
    Visions. 39 sites build `WarExhaustionResolved` and 5 build
    `VisionVictory`; the compiler lists each.

## File structure

- Create `src/main/scala/distantlands/gameplay/RevealedVisions.scala`: the
  victory read path.
- Create `src/main/scala/distantlands/gameplay/DesecratedGround.scala`:
  `SacredGroundCard` (moved) and `DesecratedGroundRule`.
- Create `src/main/scala/distantlands/gameplay/powers/cardplay/DesecratedGround.scala`:
  the registered power.
- Create `src/test/scala/distantlands/gameplay/powers/cardplay/DesecratedGroundSuite.scala`.
- Create `src/test/scala/distantlands/application/gamelog/DesecratedGroundLogSuite.scala`.
- Create `src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala`.
- Modify the files in finding 1, plus `model/GameEventProtocol.scala`,
  `serialization/EndingEventCodec.scala`, `application/DecisionOptionRows.scala`,
  `application/gamelog/GameLogFormatter.scala`,
  `gameplay/actions/cardplay/CardPlayProcedure.scala`,
  `gameplay/powers/cardplay/SacredGround.scala`,
  `gameplay/powers/WalkerPowerCatalog.scala`,
  `gameplay/cards/NewFoundations.scala`, and the test resources
  `powers/unimplemented.txt` and `notes/declared-keys.txt`.

---

### Task 1: Several revealed Visions in the model

The field becomes a list, and every bookkeeping site handles a list. No
rule changes yet: a reveal still discards every Vision already there.

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala:22`
- Modify: `src/main/scala/distantlands/model/CardIndex.scala:161-170`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala:85-88`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala:252-253, 378-383`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationExecutor.scala:80-82`
- Modify: `src/main/scala/distantlands/gameplay/roles/RoleChanges.scala:132-140`
- Modify: `src/main/scala/distantlands/gameplay/actions/CardPlay.scala:174-206`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala:334-346`
- Modify: `src/main/scala/distantlands/application/DecisionOptionRows.scala:107-113`
- Modify: `src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala:11`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala:69, 214`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/SwearOath.scala:40`
- Modify: `src/test/scala/distantlands/testkit/Table.scala:348-352`
- Modify: the other test files from finding 10.
- Test: `src/test/scala/distantlands/gameplay/operations/CardMovementOperationsSuite.scala` (or the suite that already tests `Location.PlayArea` moves; find it with `grep -rln "PlayArea" src/test/scala/distantlands/gameplay/operations`)
- Test: `src/test/scala/distantlands/gameplay/roles/RoleChangesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/CardSlotsSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/VisionPlaySuite.scala`
- Test: `src/test/scala/distantlands/application/SlotOptionRowsSuite.scala`
- Test: `src/test/scala/distantlands/testkit/TableSuite.scala`

**Interfaces:**
- Produces: `PlayerState.revealedVisions: Vector[VisionState]`;
  `Table.revealedVision(player, vision)` appends.

- [ ] **Step 1: Change the field and let the compiler list the sites**

In `model/GameState.scala`, replace the last field of `PlayerState`:

```scala
    relics: Vector[RelicState],
    /** Faceup, in reveal order. One unless Desecrated Ground kept the
      * earlier ones (catalog batch 11, ruling 13). */
    revealedVisions: Vector[VisionState]
```

Run: `./sbtw compile 2>&1 | grep -E "\[error\]" | head -40`
Expected: errors at the production sites of finding 1.

- [ ] **Step 2: Fix the bookkeeping sites**

`model/CardIndex.scala:161`, each Vision at its index:

```scala
      player.revealedVisions.zipWithIndex.foreach { (vision, at) =>
        addState(
          vision,
          CardContainer.Player(
            player.player,
            PlayerCardArea.RevealedVision
          ),
          at
        )
      }
```

`gameplay/operations/OperationStateWrites.scala:85-88`:

```scala
            case CardContainer.Player(player, PlayerCardArea.RevealedVision) =>
              updatePlayer(ready, player)(state => state.copy(revealedVisions =
                state.revealedVisions.map(value => if value.id == id then
                  f(value).asInstanceOf[VisionState] else value)))
```

`gameplay/operations/CardMovementOperations.scala:252-253` (the removal):

```scala
    case CardContainer.Player(player, PlayerCardArea.RevealedVision) => updatePlayer(ready,
      player)(state => state.copy(revealedVisions =
        state.revealedVisions.filterNot(_.id == located.id)))
```

`CardMovementOperations.scala:378-383` (the arrival). The guard goes
(finding 2):

```scala
        case state: VisionState => updatePlayer(ready, player)(value =>
          value.copy(revealedVisions = value.revealedVisions :+ state))
```

`gameplay/operations/OperationExecutor.scala:80-82`:

```scala
    val invalidRevealed = ready.game.current.players.exists(
      _.revealedVisions.exists(_.orientation != Orientation.FaceUp)
    )
```

`gameplay/roles/RoleChanges.scala:132-140`, every Vision:

```scala
  /** Every revealed Vision goes to the discard pile after the player's
    * pawn's region, as a played card's discard does. */
  private def vision(ready: ReadyGame, state: PlayerState)
      : Vector[CoreOperation] =
    CardPlay.discardRegion(ready, state.player).toVector.flatMap(region =>
      state.revealedVisions.map(held => Discard.Vision(held.id,
        PositionedLocation(Location.PlayArea(state.player)), region)))
```

- [ ] **Step 3: Fix card play so a reveal replaces every Vision**

`gameplay/actions/CardPlay.scala:186-193`, the hand or discard-pile case.
An answer may name any of the Visions it replaces:

```scala
    case Origin.TemporaryHand | _: Origin.DiscardPile
        if orientation == Orientation.FaceUp =>
      // A reveal displaces every revealed Vision. An answer that names one
      // of them is still accepted.
      val held = player.revealedVisions.map(_.id)
      Either.cond(replace.forall(named => held.contains(named)),
        revealed(player, origin, id),
        InvalidSearchPlacement("a Vision replaces only a revealed Vision"))
```

`CardPlay.scala:200-206`, `revealed`:

```scala
  /** `id` played faceup as a revealed Vision, displacing every one there. */
  private def revealed(player: PlayerState, origin: Origin, id: VisionId)
      : PlacementPlan =
    PlacementPlan(Some(Play(id, keptSource(origin, player.player),
      Location.PlayArea(player.player), Orientation.FaceUp, required = true)),
      Vector.empty, player.revealedVisions.map(_.id), Vector.empty)
```

`gameplay/actions/cardplay/CardSlots.scala:341-345`, one `Replacing` per
Vision (finding 5):

```scala
      case _: VisionId =>
        val spot = DecisionOption.RevealedVision(
          DecisionOptionRef.RevealedVision(player.player))
        Vector(player.revealedVisions.foldLeft[DecisionOption](spot)(
          (option, held) => DecisionOption.Replacing(option, held.id)) ->
          placement)
```

- [ ] **Step 4: Name every replaced card in the option row**

`application/DecisionOptionRows.scala:107-113`. Collect the chain of
`Replacing` wrappers, then label from the innermost option:

```scala
      // Named after the wrapped option is described, so the label says what
      // the play discards and where the card goes. A reveal that displaces
      // several Visions nests one `Replacing` per Vision; the row names
      // them all and carries the first.
      case replacing: DecisionOption.Replacing =>
        val (inner, cards) = replacedChain(replacing)
        val replaced = cards.map(shown(ready, viewer, index, _))
        project(ready, viewer, index, inner, details).map(row =>
          row.copy(label = replacingLabel(inner.ref,
            spokenList(replaced.map(_.name))), replaces = replaced.headOption))
```

Add beside `replacingLabel`:

```scala
  /** The option a chain of `Replacing` wraps, and the cards the chain
    * names, innermost first. */
  private def replacedChain(option: DecisionOption)
      : (DecisionOption, Vector[CardId]) = option match
    case DecisionOption.Replacing(inner, card) =>
      val (bottom, cards) = replacedChain(inner)
      (bottom, cards :+ card)
    case other => (other, Vector.empty)

  /** "A", "A and B", "A, B and C". */
  private def spokenList(names: Vector[String]): String = names match
    case Vector(only) => only
    case _ => names.init.mkString(", ") + " and " + names.last
```

- [ ] **Step 5: Keep the victory reads compiling, unchanged in effect**

Task 4 rewrites these. For now each reads the list where it read the
option.

`gameplay/VisionVictoryEligibility.scala:11`:

```scala
    current.tracks.visionsDrawn >= 3 && current.player(playerId).toVector
      .flatMap(_.revealedVisions).exists { revealed =>
```

`gameplay/StateBasedEvaluation.scala:69`:

```scala
        actor.revealedVisions.headOption.flatMap(v => Option.when(
```

`StateBasedEvaluation.scala:214`:

```scala
        p.revealedVisions.exists(_.id == vision) &&
```

`gameplay/chronicle/SwearOath.scala:40`:

```scala
      .toVector.flatMap(_.revealedVisions).headOption.flatMap(vision =>
```

Run: `./sbtw compile`
Expected: success.

- [ ] **Step 6: Move the test kit and the tests to the list**

`testkit/Table.scala:348-352`:

```scala
  /** `vision` added to `player`'s revealed Visions, faceup, taken from
    * wherever it lay. */
  def revealedVision(player: PlayerId, vision: VisionId): Table =
    moving(vision).onPlayer(player)(state => state.copy(revealedVisions =
      state.revealedVisions :+ VisionState(vision, Orientation.FaceUp)))
```

In every other test file from finding 10, `revealedVision = None` becomes
`revealedVisions = Vector.empty`, `revealedVision = Some(x)` becomes
`revealedVisions = Vector(x)`, and a read `_.revealedVision` that expects
an `Option` becomes `_.revealedVisions.headOption` only where the test
states one Vision. In `VisionPlaySuite`, change `revealedVisionOf` to:

```scala
  private def revealedVisionsOf(state: OathState): Vector[VisionState] =
    Look(state).player(actor).revealedVisions
```

and its assertions to compare `Vector(...)`. List the files with:

Run: `grep -rln "revealedVision\b" src/test/scala`
Expected after the edits: only uses of `Table.revealedVision`,
`DecisionOptionRef.RevealedVision` and `PlayerCardArea.RevealedVision`
remain.

Run: `./sbtw "Test/compile"`
Expected: success. (The frontend still compiles against the old DTO.
Task 2 changes it.)

- [ ] **Step 7: Write the failing tests for several Visions**

`testkit/TableSuite.scala`, beside the test at line 361:

```scala
  test("revealedVision adds to the Visions already revealed"):
    val current = Table.start.revealedVision(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready
    assertEquals(current.game.current.player(p1).map(_.revealedVisions),
      Some(Vector(VisionState(VisionRules.Faith, Orientation.FaceUp),
        VisionState(VisionRules.Conquest, Orientation.FaceUp))))
```

`gameplay/CardSlotsSuite.scala`, after the test at line 352 ("a Vision
never goes to a site..."):

```scala
  test("a faceup Vision over two revealed Visions replaces both"):
    val faith = VisionRules.Faith
    val over = atAncientCity.hand(p1, faith)
      .revealedVision(p1, VisionRules.Conquest)
      .revealedVision(p1, VisionRules.Sanctuary).ready
    assertEquals(options(slotsOf(over, faith)).filter(_.ref == spot.ref),
      Vector(DecisionOption.Replacing(replacing(spot, VisionRules.Conquest),
        VisionRules.Sanctuary)))
```

If `replacing(...)` in that suite returns a `DecisionOption`, this nests
as written. If it returns something else, build both levels with
`DecisionOption.Replacing`.

`application/SlotOptionRowsSuite.scala`, beside the Replace tests:

```scala
  test("a spot that replaces two Visions names both and carries the first"):
    val spot = DecisionOption.RevealedVision(
      DecisionOptionRef.RevealedVision(p1))
    val twoVisions = Table.from(board)
      .revealedVision(p1, VisionRules.Faith).ready
    val row = present(rows.project(twoVisions, Some(p1),
      CardIndex.from(twoVisions.game).toOption,
      DecisionOption.Replacing(DecisionOption.Replacing(spot,
        VisionRules.Conquest), VisionRules.Faith)), "a projected row")
    assertEquals(row.label,
      "Replace Vision of Conquest and Vision of Faith as revealed Vision")
    assertEquals(row.replaces.map(_.cardId), Some(VisionRules.Conquest.value))
```

`gameplay/VisionPlaySuite.scala`, a play over two Visions. Reuse the
suite's helpers; `visionInTemporaryHand` takes one Vision, so stack the
second through `Table` first:

```scala
  test("a Vision played faceup discards every revealed Vision"):
    val stacked = atHome.worldDeckTop(newVision)
      .revealedVision(actor, existingVision)
      .revealedVision(actor, VisionRules.Sanctuary).ready
    val parked = accepted(SearchPlay.searched(rules, Ready(stacked), actor))
      .state
    val played = answerPlacement(parked, faceup(parked).choice)
    assertEquals(revealedVisionsOf(played),
      Vector(VisionState(newVision, Orientation.FaceUp)))
    assertEquals(provincesDiscard(played).filter(Set[WorldCardId](
      existingVision, VisionRules.Sanctuary)).toSet,
      Set[WorldCardId](existingVision, VisionRules.Sanctuary))
```

If `faceup(parked)` already is the answer's ref type, pass it as the
suite's other tests do: read the test at the suite's "existing Vision"
case and copy its call exactly.

`gameplay/roles/RoleChangesSuite.scala`, beside its existing Vision test
(`grep -n "revealedVision" src/test/scala/distantlands/gameplay/roles/RoleChangesSuite.scala`):
the same setup with two Visions, asserting both are discarded and
`revealedVisions` is empty after becoming a Citizen. Copy the existing
test's setup and its assertion style, adding
`.revealedVision(p, VisionRules.Sanctuary)` and checking the two
`Discard.Vision` operations, or the two cards in the discard pile.

The movement suite: a faceup Vision moved to a play area that already
holds one now arrives beside it.

```scala
  test("a faceup Vision moved to a play area joins the revealed Visions"):
    // Card play discards the old Visions before the move; Desecrated
    // Ground keeps them, so the write itself must not refuse a second one.
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .worldDeckTop(VisionRules.Conquest).ready
    val moved = accepted(OperationExecutor.apply(ready, Vector(Play(
      VisionRules.Conquest, PositionedLocation(Location.WorldDeck),
      Location.PlayArea(p1), Orientation.FaceUp, required = true))))
    assertEquals(moved.game.current.player(p1).map(_.revealedVisions.map(_.id)),
      Some(Vector[VisionId](VisionRules.Faith, VisionRules.Conquest)))
```

Before writing it, read how the chosen suite applies operations, and
use its helper instead of `OperationExecutor.apply` if the names differ.
`Location.WorldDeck` and the `Play` source come from how
`CardPlay.keptSource` names a world-deck card; copy that.

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.testkit.TableSuite distantlands.gameplay.CardSlotsSuite distantlands.application.SlotOptionRowsSuite distantlands.gameplay.VisionPlaySuite distantlands.gameplay.roles.RoleChangesSuite"`
Expected: PASS. Then run the movement suite you chose.

Run: `./sbtw test`
Expected: PASS, except frontend tests, which Task 2 handles.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/model/CardIndex.scala \
  src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala \
  src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala \
  src/main/scala/distantlands/gameplay/operations/OperationExecutor.scala \
  src/main/scala/distantlands/gameplay/roles/RoleChanges.scala \
  src/main/scala/distantlands/gameplay/actions/CardPlay.scala \
  src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala \
  src/main/scala/distantlands/application/DecisionOptionRows.scala \
  src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala \
  src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala \
  src/main/scala/distantlands/gameplay/chronicle/SwearOath.scala
git add $(git diff --name-only -- src/test)
git commit -m "refactor(model): a player holds a list of revealed Visions" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Check `git status` first: `git diff --name-only -- src/test` must list
only the test files this task changed.

---

### Task 2: The projection and the board show several Visions

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala:323`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala:223, 238, 249` and the board's constructor call below line 249
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala:187-188`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala:108-120`
- Modify: `frontend/src/test/scala/distantlands/frontend/TestTablePieces.scala:47`
- Test: `frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: the projector suite that reads a board's Vision (`grep -rln "revealedVision\|revealedVisions" src/test/scala/distantlands/application`)

**Interfaces:**
- Consumes: `PlayerState.revealedVisions` (Task 1).
- Produces: `PlayerBoardProjection.revealedVisions:
  Vector[CardDetailsProjection]`, wire key `"revealedVisions"`.

- [ ] **Step 1: Write the failing codec test**

In `ProjectionProtocolSuite`, find the board round-trip test (`grep -n
"PlayerBoardProjection(" shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`)
and add one with two Visions, built the same way:

```scala
  test("a board with two revealed Visions round trips"):
    val faith = CardDetailsProjection("vision:vision-of-faith", "vision",
      "Vision of Faith", orientation = Some("face-up"))
    val conquest = faith.copy(cardId = "vision:vision-of-conquest",
      name = "Vision of Conquest")
    val board = sampleBoard.copy(revealedVisions = Vector(faith, conquest))
    assertEquals(WorldProjectionCodec.decodeBoard(
      WorldProjectionCodec.encodeBoard(board), "board"), Right(board))
```

Use the suite's own sample board in place of `sampleBoard`, and copy the
`CardDetailsProjection` constructor call from an existing test there.

- [ ] **Step 2: Change the DTO and the codec**

`ActionProjectionDtos.scala:323`:

```scala
    /** Faceup, in reveal order. */
    revealedVisions: Vector[CardDetailsProjection],
```

`WorldProjectionCodec.scala:223`:

```scala
    "revealedVisions" -> encoded(value.revealedVisions)(encodeCard),
```

Line 238: `"revealedVision"` becomes `"revealedVisions"` in the `exact`
key set. Line 249:

```scala
    visionRaws <- array(value, "revealedVisions", path)
    visions <- traverse(visionRaws, s"$path.revealedVisions")(decodeCard)
```

and pass `visions` where the constructor below passed `vision`.

`GamePresentationProjector.scala:187-188`:

```scala
        player.revealedVisions.map(card => cardDetails(card.id,
          Some(card.orientation), hidden = false)),
```

- [ ] **Step 3: Write the failing frontend tests**

In `PlayerBoardSuite`, change `revealedVision = Some(vision)` to
`revealedVisions = Vector(vision)` at lines 98, 212 and 251. Add:

```scala
  test("each revealed Vision has its own pill in the slots row"):
    val faith = CardDetails("vision:vision-of-faith", "vision",
      "Vision of Faith", orientation = Some("face-up"))
    val conquest = faith.copy(cardId = "vision:vision-of-conquest",
      name = "Vision of Conquest")
    val node = renderWith(board.copy(revealedVisions = Vector(faith, conquest)),
      None)
    assertEquals(all(node, ".player-slots .player-slot-vision")
      .map(_.textContent), Vector("Vision of Faith", "Vision of Conquest"))
```

Then add two tests for the lit spot. Find the suite's existing test that
lights the `revealed-vision` key (`grep -n "revealed-vision"
frontend/src/test/scala/distantlands/frontend/*.scala`) and copy its
decision setup:

- An option with `replaces` set (a reveal elsewhere): every Vision pill
  carries the `board-option-replace` class, and no empty spot is drawn.
- An option with `replaces` empty while Visions are shown (a reveal at
  Desecrated Ground): the pills are plain, and an empty spot labelled
  "Empty revealed Vision spot" is drawn after them and is the option.

In `TestTablePieces.scala:47`, `revealedVision = None` becomes
`revealedVisions = Vector.empty`.

- [ ] **Step 4: Draw the pills**

`WorldBoardRenderer.scala:108-120`. A lit option that replaces marks
every pill, because every one is discarded. A lit option that replaces
nothing gets its own empty spot:

```scala
      // The spot is drawn empty only while the decision lights it: the strip
      // never scrolls, so an empty spot may not spend a row on every board.
      // A reveal that replaces marks every Vision it discards; one that
      // keeps them (Desecrated Ground) lights an empty spot after them.
      val visionKey = Vector(BoardOptionViews.key("revealed-vision",
        player.playerId))
      val lit = options.view(visionKey)
      val replacing = lit.exists(_.option.replaces.nonEmpty)
      board.revealedVisions.foreach { card =>
        val pill = button(card.name, "player-slot-vision")
        pill.onclick = _ => CardInspection.open(card, pill)
        slots.appendChild(
          if replacing then options.control(pill, visionKey,
            Some(card.name -> (CardInspection.open(card, _))))
          else pill)
      }
      lit.filterNot(_ => replacing).foreach { _ =>
        val spot = button("Vision", "player-slot-vision player-slot-vision-empty")
        spot.setAttribute("aria-label", "Empty revealed Vision spot")
        slots.appendChild(options.control(spot, visionKey, None))
      }
```

Before this change, a pill was a control whenever the key was lit. A
lit key with Visions present always replaced, so the behaviour for one
Vision is unchanged.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "sharedJVM/testOnly distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.PlayerBoardSuite"`
Expected: PASS. If the project names differ, read `build.sbt` for the
shared and frontend project ids.

Run: `./sbtw test`
Expected: PASS, frontend included.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala \
  frontend/src/test/scala/distantlands/frontend/TestTablePieces.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala
git commit -m "feat(projection): a board shows every revealed Vision" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Desecrated Ground keeps the revealed Visions

**Files:**
- Create: `src/main/scala/distantlands/gameplay/DesecratedGround.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/cardplay/DesecratedGround.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/SacredGround.scala:1-32`
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:82-83`
- Modify: `src/main/scala/distantlands/gameplay/actions/CardPlay.scala` (`plan`, `planVision`, `revealed`)
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala` (`faceup`)
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala:470-476, 505-510`
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `edifice.e08.ruined`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Modify: `src/test/scala/distantlands/gameplay/powers/cardplay/SacredGroundSuite.scala` (import)
- Modify: `src/test/scala/distantlands/application/CardPlayReasonsSuite.scala:8` (import)
- Test: `src/test/scala/distantlands/gameplay/powers/cardplay/DesecratedGroundSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/DesecratedGroundLogSuite.scala`

**Interfaces:**
- Consumes: `PlayerState.revealedVisions` (Task 1).
- Produces: `distantlands.gameplay.SacredGroundCard` (moved);
  `private[gameplay] object DesecratedGroundRule` with `power: PowerId`,
  `kept: NoteKey`, `noteKeys: Vector[NoteKey]`,
  `keeps(ready: ReadyGame, player: PlayerId): Boolean` and
  `keptNote(card: WorldCardId, ref: DecisionOptionRef)(states: NoteStates):
  Option[LogNote]`; `case object DesecratedGround extends
  ContributingPower` in `gameplay.powers.cardplay`.

- [ ] **Step 1: Move the card and add the rule**

Create `src/main/scala/distantlands/gameplay/DesecratedGround.scala`. The
`SacredGroundCard` object moves here unchanged from
`powers/cardplay/SacredGround.scala:9-23`:

```scala
package distantlands.gameplay

import distantlands.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import distantlands.model._

object SacredGroundCard extends Edifice(EdificeId("E08"), Suit.Nomad):
  object intact extends EdificeFace("Sacred Ground") with Locked:
    val power = PrintedPower(PowerId("edifice.e08.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Search)),
      text = "Players **cannot** play Visions, except the Conspiracy, " +
        "faceup unless their pawn is at this site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Desecrated Ground"):
    val power = PrintedPower(PowerId("edifice.e08.ruined"),
      persistent = true, cost = Cost.free,
      text = "If an Exile reveals a Vision while their pawn is at this " +
        "site, they do not discard their current revealed Vision. " +
        "_They can have any number!_")
    val powers: Vector[PrintedPower] = Vector(power)

/** Desecrated Ground (E08's ruined face): an Exile who reveals a Vision
  * while their pawn is at its site keeps the Visions already revealed
  * (catalog batch 11, rulings 13 and 31). `CardSlots` and `CardPlay` read
  * `keeps`; the card play writes `kept`. It sits in `gameplay` because card
  * play may not import `gameplay.powers`; the power registered there hooks
  * no window.
  */
private[gameplay] object DesecratedGroundRule:
  val power: PowerId = SacredGroundCard.ruined.power.id

  private val notes = NoteSet(power)
  /** "Desecrated Ground: Kept Vision of Faith." */
  val kept: NoteKey = notes.key("kept", Vector(NotePart.Text("Kept "),
    NotePart.Arg(0), NotePart.Text(".")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Whether `player` keeps their revealed Visions when they reveal one
    * now: they are an Exile and their pawn is at the site where E08
    * stands ruined, in play. */
  def keeps(ready: ReadyGame, player: PlayerId): Boolean =
    val current = ready.game.current
    Imperials.role(ready, player).contains(Role.Exile) && current
      .edificeSite(SacredGroundCard.id, EdificeSide.Ruined)
      .filter(current.map.inPlay.contains)
      .exists(site => current.pawnSite(player).contains(site))

  /** The line after a reveal: the player's other revealed Visions, if any
    * are left. A reveal elsewhere has just discarded them, so only a kept
    * Vision is named. */
  def keptNote(card: WorldCardId, ref: DecisionOptionRef)(
      states: NoteStates): Option[LogNote] = ref match
    case DecisionOptionRef.RevealedVision(owner) => for
      player <- states.now.game.current.player(owner)
      others = player.revealedVisions.map(_.id).filterNot(_ == card)
      if others.nonEmpty
    yield kept(PowerSourceRef.Card(SacredGroundCard.id),
      NoteArg.Cards(others))
    case _ => None
```

Check that `current.pawnSite` and `Imperials.role` are the names
`SacredGround.blocked` and `GrandMaskRule.wearer` use; they are, at
`SacredGround.scala:52` and `GrandMask.scala:30`.

Edit `powers/cardplay/SacredGround.scala`: delete lines 9-23 (the card),
change the imports to:

```scala
import distantlands.gameplay.SacredGroundCard
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.powerresolver._
import distantlands.model._
```

and replace the doc comment's sentence "Its ruined face, Desecrated
Ground, is not implemented; E08 never appears in a generated game." with
"Its ruined face is `DesecratedGroundRule`."

In `gameplay/cards/NewFoundations.scala`, import `SacredGroundCard` from
`distantlands.gameplay` instead of `distantlands.gameplay.powers.cardplay`
(find the import with `grep -n "SacredGroundCard\|powers.cardplay"`).
Do the same in `SacredGroundSuite.scala` (add `import
distantlands.gameplay.SacredGroundCard`) and `CardPlayReasonsSuite.scala:8`.

- [ ] **Step 2: Register the power**

Create `src/main/scala/distantlands/gameplay/powers/cardplay/DesecratedGround.scala`:

```scala
package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.DesecratedGroundRule
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Desecrated Ground (E08's ruined face): "If an Exile reveals a Vision
  * while their pawn is at this site, they do not discard their current
  * revealed Vision. They can have any number!"
  *
  * The rule is `DesecratedGroundRule`, which `CardSlots` and `CardPlay`
  * read. This power hooks no window. It is registered so the card counts
  * as implemented, and it declares the kept line the card play writes.
  */
case object DesecratedGround extends ContributingPower:
  val id: PowerId = DesecratedGroundRule.power
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override def noteKeys: Vector[NoteKey] = DesecratedGroundRule.noteKeys
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

`DesecratedGroundRule` is `private[gameplay]`, and this file is in
`distantlands.gameplay.powers.cardplay`, so the access compiles.

In `WalkerPowerCatalog.scala:82-83`:

```scala
      Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown,
        GrandMask, DesecratedGround) ++
```

and add `DesecratedGround` to the file's `powers.cardplay` import.

Delete the line `edifice.e08.ruined` from
`src/test/resources/powers/unimplemented.txt`.

In `src/test/resources/notes/declared-keys.txt`, add after the line
`edifice.e08.intact no-faceup = Cannot play a Vision faceup.`:

```text
edifice.e08.ruined kept = Kept {0}.
```

- [ ] **Step 3: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/cardplay/DesecratedGroundSuite.scala`.
It follows `SacredGroundSuite`: `SearchFixture.staged` stands p1 at
Ancient City, with Broken Peaks elsewhere.

```scala
package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.{DesecratedGroundRule, SacredGroundCard}
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture,
  PowerImplementationStatus, SearchFixture}
import distantlands.model._
import distantlands.testkit.{Look, PlacementPicks, Table}
import distantlands.testkit.Unwrap.accepted

/** E08 never appears in a generated first game (the Nomad Homeland takes
  * E06), so each test places it by hand, ruined. */
class DesecratedGroundSuite extends munit.FunSuite:
  import PowerFixture._
  import VisionPlayFixture._

  private val home = AncientCitySite.id
  private val elsewhere = BrokenPeaksSite.id
  private val held = VisionRules.Conquest
  private val drawn = VisionRules.Faith

  /** p1 at Ancient City holds Conquest revealed and has drawn Faith in a
    * Search, with E08 ruined at `site`. */
  private def searchHolding(site: SiteId, ready: ReadyGame => ReadyGame =
      identity)(using munit.Location): OathTransition =
    val drawing = SearchFixture.staged(Vector(drawn), supply = 5)
    val table = Table.from(ready(drawing))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = site)
      .revealedVision(actor, held)
    searched(table.ready, drawn)

  private def revealed(from: OathTransition)(using munit.Location)
      : OathTransition =
    accepted(SearchFixture.place(from, drawn, PlayedAs.Faceup))

  private def visionsOf(after: OathTransition): Vector[VisionId] =
    Look(after.state).player(actor).revealedVisions.map(_.id)

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(DesecratedGround.id, DesecratedGround.noteKeys, events)
      .map(_.text)

  test("a reveal at Desecrated Ground keeps the revealed Vision"):
    assertEquals(visionsOf(revealed(searchHolding(home))),
      Vector[VisionId](held, drawn))

  test("at Desecrated Ground the spot replaces nothing"):
    val parked = searchHolding(home)
    val spot = PlacementPicks.parkedOptions(parked.state,
      distantlands.gameplay.powers.WalkerPowerCatalog.default(
        distantlands.gameplay.setup.FirstGameSetupFixture.catalog))
      .find(_.ref == DecisionOptionRef.RevealedVision(actor))
    assertEquals(spot.map(_.isInstanceOf[DecisionOption.Replacing]),
      Some(false))

  test("a reveal elsewhere discards the revealed Vision"):
    assertEquals(visionsOf(revealed(searchHolding(elsewhere))),
      Vector[VisionId](drawn))

  test("a third Vision at the site keeps both earlier ones"):
    val twoHeld = searchHolding(home, ready =>
      Table.from(ready).revealedVision(actor, VisionRules.Sanctuary).ready)
    assertEquals(visionsOf(revealed(twoHeld)),
      Vector[VisionId](VisionRules.Sanctuary, held, drawn))

  test("Desecrated Ground at a site out of play keeps nothing"):
    // Out of play: the rule reads `map.inPlay`, as Sacred Ground does.
    val ready = Table.from(SearchFixture.staged(Vector(drawn), supply = 5))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = home).ready
    val outOfPlay = ready.updateCurrent(current => current.copy(map =
      current.map.copy(inPlay = current.map.inPlay - home)))
    assert(!DesecratedGroundRule.keeps(outOfPlay, actor))

  test("an intact Sacred Ground keeps nothing"):
    val ready = Table.from(SearchFixture.staged(Vector(drawn), supply = 5))
      .edifice(SacredGroundCard.id, EdificeSide.Intact, at = home).ready
    assert(!DesecratedGroundRule.keeps(ready, actor))

  test("a facedown Vision turned faceup at the site keeps the revealed one"):
    val ready = Table.from(inPhase(quietTable, Phase.Act))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = home)
      .revealedVision(actor, held).ready
    val parked = fromAdvisers(ready, drawn)
    val played = accepted(SearchFixture.place(parked, drawn,
      PlayedAs.Faceup))
    assertEquals(visionsOf(played), Vector[VisionId](held, drawn))

  test("a kept reveal writes the kept line"):
    assertEquals(lines(revealed(searchHolding(home)).events),
      Vector(s"Kept ${held.value}."))

  test("a reveal elsewhere writes no kept line"):
    assertEquals(lines(revealed(searchHolding(elsewhere)).events),
      Vector.empty)

  test("the ruined face counts as implemented"):
    assert(PowerImplementationStatus.implemented(
      SacredGroundCard.ruined.power.id))
```

Notes for the implementer, checked against the fixtures at `33c7be12`:

- `quietTable` and `inPhase` come from `PowerFixture`, as
  `SacredGroundSuite:68` uses them. Check that `quietTable` stands p1 at
  Ancient City; if not, add `.pawn(actor, AncientCitySite)`.
- `SearchFixture.place` resolves the Search placement decision. The
  facedown-adviser play has its own decision id. If `place` refuses it,
  answer with `PlacementPicks.find` and `FacedownAdviserPlay`'s decision
  id, as `VisionPlayFixture.fromAdvisers` parks it.
- `MapState.inPlay` may be a `Vector` or a `Set`; adjust the out-of-play
  test's removal to match.
- The "spot replaces nothing" test reads the parked options. If
  `PlacementPicks.parkedOptions` returns options with the look-ahead's
  wrappers, unwrap them the way `PlacementPicks.offered` does.

Create `src/test/scala/distantlands/application/gamelog/DesecratedGroundLogSuite.scala`,
the golden line, following `HospitalityLogSuite`:

```scala
package distantlands.application.gamelog

import distantlands.application.{GameApplicationService,
  ParkedServiceFixture, TreeDecision}
import distantlands.gameplay.SacredGroundCard
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.model._
import distantlands.testkit.{Commands, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, present}
import LogScripts.formatter

/** The kept line as the Game Log shows it (catalog batch 11, ruling 31). */
class DesecratedGroundLogSuite extends munit.FunSuite:
  /** p1 holds Conquest revealed, stands at Ancient City where E08 lies
    * ruined, Searches the world deck and reveals Faith. The log as p2
    * sees it. */
  private def revealedFaithKeepingConquest: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite).supply(p1, 7)
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = AncientCitySite.id)
      .revealedVision(p1, VisionRules.Conquest)
      .worldDeckTop(VisionRules.Faith)
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    accepted(service.handle("g", picked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Faith),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))))
    texts(service)

  private def texts(service: GameApplicationService): Vector[String] =
    formatter.format(present(accepted(service.history("g")),
      "a stored game").steps, Some(p2)).map(LogScripts.text)

  test("a kept reveal reads as the reveal, then the kept Vision"):
    val log = revealedFaithKeepingConquest
    val reveal = log.indexWhere(_.startsWith("Revealed Vision of Faith"))
    assert(reveal >= 0, log.toString)
    assertEquals(log.lift(reveal + 1),
      Some("Desecrated Ground: Kept Vision of Conquest."), log.toString)
```

Check `ParkedServiceFixture.pickSearchSource`'s parameters, the
`SearchSource` case for the world deck, and how the Search draws one
card with 7 Supply. If the Search draws more than one card, set the deck
top with the extra cards the suite needs, or discard them in the same
command sequence, as `WayStationLogSuite` does for its Search. If the
"Revealed" line is split from the kept line by another line, assert that
the kept line follows the reveal line instead of `reveal + 1`.

Run: `./sbtw "testOnly distantlands.gameplay.powers.cardplay.DesecratedGroundSuite distantlands.application.gamelog.DesecratedGroundLogSuite"`
Expected: FAIL: Visions discarded at the site, no kept line.

- [ ] **Step 4: Read the rule in card play**

`CardPlay.scala:152-156`, pass `ready` on:

```scala
        else planVision(ready, player, origin, id, orientation, replace)
```

`CardPlay.scala:174-206`. `planVision` and `revealed` take `ready`, and
`revealed` discards nothing when the rule keeps:

```scala
  private def planVision(ready: ReadyGame, player: PlayerState,
      origin: Origin, id: VisionId, orientation: Orientation,
      replace: Option[CardId]): Either[OathViolation, PlacementPlan] =
    origin match
    case _ if id == VisionRules.Conspiracy && orientation == Orientation.FaceUp =>
      // A played Conspiracy is boxed by its WHEN PLAYED power, so from either
      // origin it takes no slot and replaces nothing.
      Either.cond(replace.isEmpty, nothing,
        InvalidSearchPlacement("a played Conspiracy replaces nothing"))
    case Origin.FacedownAdviser =>
      Either.cond(orientation == Orientation.FaceUp && replace.isEmpty,
        revealed(ready, player, origin, id), InvalidSearchPlacement(
          "facedown adviser must be played faceup to advisers or the pawn's site"))
    case Origin.TemporaryHand | _: Origin.DiscardPile
        if orientation == Orientation.FaceUp =>
      // A reveal displaces every revealed Vision, unless Desecrated Ground
      // keeps them. An answer that names one it displaces is still
      // accepted.
      val displaced = displacedBy(ready, player)
      Either.cond(replace.forall(displaced.contains),
        revealed(ready, player, origin, id),
        InvalidSearchPlacement("a Vision replaces only a revealed Vision"))
    case Origin.TemporaryHand | _: Origin.DiscardPile =>
      adviserHeld(player, replace).map(removed => PlacementPlan(
        Some(Play(id, keptSource(origin, player.player),
          Location.PlayArea(player.player), Orientation.FaceDown,
          required = true)), Vector.empty, removed.toVector, Vector.empty))

  /** `id` played faceup as a revealed Vision, displacing the ones there
    * unless Desecrated Ground keeps them. */
  private def revealed(ready: ReadyGame, player: PlayerState, origin: Origin,
      id: VisionId): PlacementPlan =
    PlacementPlan(Some(Play(id, keptSource(origin, player.player),
      Location.PlayArea(player.player), Orientation.FaceUp, required = true)),
      Vector.empty, displacedBy(ready, player), Vector.empty)

  /** The revealed Visions a reveal by `player` now discards. */
  private[gameplay] def displacedBy(ready: ReadyGame, player: PlayerState)
      : Vector[WorldCardId] =
    if DesecratedGroundRule.keeps(ready, player.player) then Vector.empty
    else player.revealedVisions.map(_.id)
```

Add `DesecratedGroundRule` to the `distantlands.gameplay` import at
`CardPlay.scala:4`. Keep the indentation of the `origin match` cases as
the file has it.

`CardSlots.scala:341-345`, the spot replaces what `displacedBy` names:

```scala
      case _: VisionId =>
        val spot = DecisionOption.RevealedVision(
          DecisionOptionRef.RevealedVision(player.player))
        Vector(CardPlay.displacedBy(ready, player).foldLeft[DecisionOption](
          spot)((option, held) => DecisionOption.Replacing(option, held)) ->
          placement)
```

- [ ] **Step 5: Write the kept line in the play's tree**

`CardPlayProcedure.scala:470-476`, in `played`, a fourth child after the
play's own line (finding 9):

```scala
          Vector(Sequence(lines), apply,
            Note(ProcedureNotes.id(ActionRef.PlayFacedownAdviser),
              playedNote(card, slot.ref)),
            Note(DesecratedGroundRule.power,
              DesecratedGroundRule.keptNote(card, slot.ref)))
```

`CardPlayProcedure.scala:505-510`, the same fourth child in `settled`:

```scala
      val play = Sequence(Vector(Sequence(Vector.empty),
        BuildOps((_, _) => Left(OathViolation.InvalidSearchPlacement(
          "the card has already been played"))),
        Note(ProcedureNotes.id(ActionRef.PlayFacedownAdviser),
          playedNote(card, ref)),
        Note(DesecratedGroundRule.power,
          DesecratedGroundRule.keptNote(card, ref))))
```

Update the comment above `played`'s `Branch` and the doc on `settled` to
say "The play's own line follows it as a third child, and the kept line
as a fourth; no path addresses into either." Add `import
distantlands.gameplay.DesecratedGroundRule`.

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.powers.cardplay.DesecratedGroundSuite distantlands.application.gamelog.DesecratedGroundLogSuite distantlands.gameplay.powers.cardplay.SacredGroundSuite distantlands.gameplay.powers.UnimplementedPowersSuite"`
Expected: PASS.

Run: `./sbtw "testOnly *DeclaredNoteKeys* *CardPlayProcedure* *VisionPlay* *CardSlots*"`
Expected: PASS. A failing tree-shape assertion in a card-play suite
means a test counts the play node's children: update it to four and say
why in the test.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/DesecratedGround.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/DesecratedGround.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/SacredGround.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/gameplay/actions/CardPlay.scala \
  src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala \
  src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/cardplay/DesecratedGroundSuite.scala \
  src/test/scala/distantlands/gameplay/powers/cardplay/SacredGroundSuite.scala \
  src/test/scala/distantlands/application/CardPlayReasonsSuite.scala \
  src/test/scala/distantlands/application/gamelog/DesecratedGroundLogSuite.scala
git commit -m "feat(powers): Desecrated Ground keeps every revealed Vision" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 8: Checkpoint review**

Dispatch a review subagent (no higher model or effort than the main
agent) over `git diff 33c7be12..HEAD`, with the spec's slice 6 section
and rulings 13 and 31. Ask it to check:

- every revealed-Vision site from finding 1;
- the movement write's dropped guard;
- that a reveal elsewhere still discards every Vision;
- the nested `Replacing` label;
- the frontend's two lit states;
- the kept line's place in both tree shapes.

Fix what it confirms before Task 4.

---

### Task 4: A win names every qualifying Vision

**Files:**
- Create: `src/main/scala/distantlands/gameplay/RevealedVisions.scala`
- Modify: `src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala:63-74, 145-150, 178-186, 207-218`
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala:44-53`
- Modify: `src/main/scala/distantlands/model/GameState.scala:186-187`
- Modify: `src/main/scala/distantlands/serialization/EndingEventCodec.scala:23-26, 41-42, 60-66, 87-88`
- Modify: `src/main/scala/distantlands/gameplay/EmpireEndings.scala:40, 45`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala:183-187, 318-323`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/SwearOath.scala`
- Modify: the test files that build the events (finding 10).
- Test: `src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/SwearOathSuite.scala`
- Test: `src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala`

**Interfaces:**
- Consumes: `PlayerState.revealedVisions` (Task 1).
- Produces: `RevealedVisions.of(ready: ReadyGame, player: PlayerId):
  Vector[VisionId]`; `VisionVictoryEligibility.qualifying(playerId:
  PlayerId, ready: ReadyGame): Vector[VisionId]`;
  `OathEvent.VisionVictory(playerId, visions: Vector[VisionId])`;
  `OathEvent.WarExhaustionResolved(winner, kind, visions:
  Vector[VisionId], randomCandidates)`; `GameResult(winner, kind,
  visions: Vector[VisionId] = Vector.empty)`;
  `SwearOath.visionDecisionId = "chronicle.swear-oath.vision"`. Slice 7
  extends `RevealedVisions.of`.

- [ ] **Step 1: Write the failing read-path and victory tests**

Create `src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.VisionRules
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

class RevealedVisionsSuite extends munit.FunSuite:
  test("a player's revealed Visions are theirs, in reveal order"):
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready
    assertEquals(RevealedVisions.of(ready, p1),
      Vector[VisionId](VisionRules.Faith, VisionRules.Conquest))
    assertEquals(RevealedVisions.of(ready, p2), Vector.empty)

  test("only the Visions whose goal is met qualify"):
    // Faith's goal is met (the Darkest Secret); Conquest's is not.
    val ready = Table.start.visionsDrawn(3)
      .revealedVisionMet(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready
    assertEquals(VisionVictoryEligibility.qualifying(p1, ready),
      Vector[VisionId](VisionRules.Faith))
```

`Table.visionsDrawn` may be named differently: find the step that sets
`tracks.visionsDrawn` (`grep -n "visionsDrawn" src/test/scala/distantlands/testkit/Table.scala`)
and use it, or the suite's `update` fallback with a comment.

In `StateBasedEvaluationSuite`, add beside its Wake Vision test (`grep -n
"VisionVictory" src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala`),
copying that test's setup:

- Two revealed Visions, both met: `visionAtWake` returns
  `Some(VisionVictory(p1, Vector(first, second)))`, in reveal order.
- Two revealed Visions, only the second met: the event names only the
  second.
- War Exhaustion: p1 holds an unmet Conquest and a met Faith; p2 holds a
  met Sanctuary. p2 wins, with `Vector(VisionRules.Sanctuary)`
  (finding 7). Sanctuary outranks Faith in printed priority.

In `GameEventFactsWireSuite.scala:53-56`, change the Vision round trip to
two Visions:

```scala
  test("a Vision victory event round trips"):
    val events = Vector[OathEvent](OathEvent.VisionVictory(PlayerId("red"),
      Vector(VisionId("vision:vision-of-faith"),
        VisionId("vision:vision-of-conquest"))))
    assertStreamRoundTrips("visions", events)
```

and add the War Exhaustion case with `Vector(VisionId("vision:vision-of-faith"))`
beside the existing ones at lines 17 and 28.

In `GameLogHeadlineSuite.scala:146-152`, change the rows to the new
fields and add one row:

```scala
      OathEvent.VisionVictory(winner, Vector(vision)) ->
        " won with Vision of Faith",
      OathEvent.VisionVictory(winner, Vector(VisionRules.Conquest, vision)) ->
        " won with Vision of Conquest and Vision of Faith",
```

with `Some(vision)` becoming `Vector(vision)` and `None` becoming
`Vector.empty` in the `WarExhaustionResolved` rows.

In `SwearOathSuite`, `wonByConquest` records its Vision:

```scala
  private def wonByConquest(goal: OathkeeperGoal): Table = empire
    .revealedVision(p1, VisionRules.Conquest).oathkeeperGoal(goal)
    .update(_.updateCurrent(_.copy(result = Some(GameResult(p1,
      VictoryKind.Visionary, Vector(VisionRules.Conquest))))))
```

and add:

```scala
  /** `empire` won by p1's Visions of Conquest and Faith at once, with
    * `goal` current. */
  private def wonByTwo(goal: OathkeeperGoal): Table = empire
    .revealedVision(p1, VisionRules.Conquest)
    .revealedVision(p1, VisionRules.Faith).oathkeeperGoal(goal)
    .update(_.updateCurrent(_.copy(result = Some(GameResult(p1,
      VictoryKind.Visionary, Vector(VisionRules.Conquest,
        VisionRules.Faith))))))

  test("a win by several Visions asks the winner which one won"):
    val at = ChronicleWalk.walkedTo(rules,
      throneStarted(wonByTwo(OathkeeperGoal.Protection)),
      SwearOath.visionDecisionId)
    val parked = ChronicleWalk.parked(at.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector[DecisionOptionRef](
          DecisionOptionRef.Vision(VisionRules.Conquest),
          DecisionOptionRef.Vision(VisionRules.Faith)))
      case other => fail(other.toString)
    val next = accepted(rules.resolveWalker(at.state, p1,
      SwearOath.visionDecisionId, DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Vision(VisionRules.Faith))))
    assertEquals(Outcomes.ready(next.state).game.campaign.oathkeeperGoal,
      OathkeeperGoal.Devotion)
    assert(lines(next.events).contains("The Oathkeeper goal became Devotion"),
      next.events)

  test("choosing the Vision of the current goal changes nothing"):
    val at = ChronicleWalk.walkedTo(rules,
      throneStarted(wonByTwo(OathkeeperGoal.Supremacy)),
      SwearOath.visionDecisionId)
    val next = accepted(rules.resolveWalker(at.state, p1,
      SwearOath.visionDecisionId, DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Vision(VisionRules.Conquest))))
    assert(!RecordedOps.all(next.events).exists(_.isInstanceOf[
      SetOathkeeperGoal]), next.events)
```

Run: `./sbtw "Test/compile"`
Expected: compile errors for the missing names and fields.

- [ ] **Step 2: Add the read path and the qualifying Visions**

Create `src/main/scala/distantlands/gameplay/RevealedVisions.scala`:

```scala
package distantlands.gameplay

import distantlands.model._

/** The Visions a player has revealed, as the victory rules read them
  * (catalog batch 11, slice 6): their own, in reveal order. Slice 7 adds
  * the Visions they claim with False Prophet here, so every victory read
  * sees a claim at once. The slot's own bookkeeping reads
  * `PlayerState.revealedVisions`, because a claim never puts a Vision in
  * the claimant's slot. */
object RevealedVisions:
  def of(ready: ReadyGame, player: PlayerId): Vector[VisionId] =
    ready.game.current.player(player).toVector
      .flatMap(_.revealedVisions.map(_.id))
```

Rewrite `VisionVictoryEligibility.qualifies` as `qualifying`, and keep
`qualifies` for any other caller (`grep -rn "qualifies(" src/main` lists
only `StateBasedEvaluation` today, so delete `qualifies` if Step 3 leaves
no caller):

```scala
  /** `playerId`'s revealed Visions whose goal they meet, in reveal order.
    * Several can qualify at once only through Desecrated Ground. */
  def qualifying(playerId: PlayerId, ready: ReadyGame): Vector[VisionId] =
    val current = ready.game.current
    if current.tracks.visionsDrawn < 3 then Vector.empty
    else RevealedVisions.of(ready, playerId).filter(vision =>
      VisionRules.trueGoal(vision).exists:
        case OathkeeperGoal.Supremacy => uniquePositiveLeader(playerId,
          SupremacySites.counts(ready))
        case OathkeeperGoal.Protection => uniquePositiveLeader(playerId,
          current.players.map(player => player.player -> player.relics.size))
        case OathkeeperGoal.ThePeople =>
          current.banners.peoplesFavor.holder.contains(playerId)
        case OathkeeperGoal.Devotion =>
          current.banners.darkestSecret.holder.contains(playerId))
```

- [ ] **Step 3: Carry the Visions on the events and the result**

`GameEventProtocol.scala:44-53`:

```scala
  final case class WarExhaustionResolved(
      winner: PlayerId,
      kind: VictoryKind,
      /** Every revealed Vision whose goal the winner meets, for a
        * Visionary win; empty otherwise. */
      visions: Vector[VisionId],
      randomCandidates: Vector[PlayerId]
  ) extends OathEvent
  final case class UsurperFlipped(playerId: PlayerId) extends OathEvent
  final case class UsurperVictory(playerId: PlayerId) extends OathEvent
  /** `visions` is every revealed Vision whose goal the winner meets, in
    * reveal order; Swear an Oath asks which won when there are several. */
  final case class VisionVictory(playerId: PlayerId,
      visions: Vector[VisionId]) extends OathEvent
```

`GameState.scala:186-187`:

```scala
/** `visions` is a Visionary win's qualifying Visions, which Swear an Oath
  * reads (catalog batch 11, ruling 30); empty for every other win. */
final case class GameResult(winner: PlayerId,
    kind: VictoryKind = VictoryKind.Usurper,
    visions: Vector[VisionId] = Vector.empty)
```

`StateBasedEvaluation.scala:63-74`, `visionAtWake`:

```scala
        .flatMap { actor =>
        val won = VisionVictoryEligibility.qualifying(actor.player, ready)
        Option.when(won.nonEmpty)(VisionVictory(actor.player, won))
      }
```

`StateBasedEvaluation.scala:145-150`:

```scala
            update(state)(current => current.copy(result = Some(GameResult(
              recorded.playerId, VictoryKind.Visionary, recorded.visions))))
```

`StateBasedEvaluation.scala:178-186`: both `GameResult(recorded.winner,
recorded.kind)` become `GameResult(recorded.winner, recorded.kind,
recorded.visions)`, and `recorded.visionId.isEmpty` becomes
`recorded.visions.isEmpty`.

`StateBasedEvaluation.scala:207-218`, `visionaryWin` checks the Vision
(finding 7):

```scala
  /** War Exhaustion's Visionary step: an Exile with a revealed Vision whose
    * goal they meet, in printed priority order (CR p. 19). The winner's
    * every qualifying Vision is recorded (ruling 30). */
  private[gameplay] def visionaryWin(ready: ReadyGame)
      : Option[WarExhaustionResolved] =
    val current = ready.game.current
    val exiles = current.players.filterNot(p =>
      Imperials.isImperial(ready, p.player)).map(p =>
      p.player -> VisionVictoryEligibility.qualifying(p.player, ready))
    visionPriority.iterator.flatMap { vision =>
      exiles.find(_._2.contains(vision)).map((player, won) =>
        WarExhaustionResolved(player, VictoryKind.Visionary, won,
          Vector.empty))
    }.nextOption()
```

Every other `WarExhaustionResolved(..., None, ...)` in production
(`StateBasedEvaluation.scala:98, 101-105, 248, 251`,
`EmpireEndings.scala:40, 45`) passes `Vector.empty`.

`EndingEventCodec.scala:23-26`:

```scala
      case WarExhaustionResolved(winner, kind, visions, candidates) => ujson.Obj(
        "winnerPlayerId" -> winner.value,
        "victoryKind" -> kind.key,
        "visionIds" -> stringArray(visions.map(_.value)),
        "randomCandidatePlayerIds" -> stringArray(candidates.map(_.value)))
```

`EndingEventCodec.scala:41-42`:

```scala
      case VisionVictory(player, visions) => ujson.Obj(
        "playerId" -> player.value,
        "visionIds" -> stringArray(visions.map(_.value)))
```

`EndingEventCodec.scala:60-66`:

```scala
        case WarExhaustionResolvedType => for
          kind <- victoryKind(payload("victoryKind"), s"$path.victoryKind")
        yield WarExhaustionResolved(PlayerId(payload("winnerPlayerId").str),
          kind, payload("visionIds").arr.toVector.map(v => VisionId(v.str)),
          payload("randomCandidatePlayerIds").arr.toVector.map(v => PlayerId(v.str)))
```

`EndingEventCodec.scala:87-88`:

```scala
        case VisionVictoryType => Right(VisionVictory(
          PlayerId(payload("playerId").str),
          payload("visionIds").arr.toVector.map(v => VisionId(v.str))))
```

If the decoder checks exact keys elsewhere (`grep -n "visionId"
src/main/scala/distantlands/serialization`), rename them there too.

`GameLogFormatter.scala:183-187`:

```scala
      case OathEvent.VisionVictory(player, visions) =>
        (Vector(victory(player,
          wonBy(VictoryKind.Visionary, visions))), run)
      case OathEvent.WarExhaustionResolved(winner, kind, visions, _) =>
        (Vector(victory(winner, wonBy(kind, visions))), run)
```

and the `EndDieRolled` case's `wonBy(won.kind, None)` becomes
`wonBy(won.kind, won.visions)`. Then `wonBy` at line 318:

```scala
  private def wonBy(kind: VictoryKind, visions: Vector[VisionId])
      : Vector[LogSpan] = kind match
    case VictoryKind.Usurper => Vector(Text(" won as the Usurper"))
    case VictoryKind.Visionary =>
      if visions.isEmpty then Vector[LogSpan](Text(" won as a Visionary"))
      else Text(" won with ") +: LogWords.join(visions.map(id =>
        Vector[LogSpan](words.vision(id))))
```

Check that `LogWords.join` is reachable from the formatter as
`LogWords.join` (it is defined on the `LogWords` companion at
`LogWords.scala:151`; if it is on the class, call it through `words`).

Fix the test sites the compiler lists (finding 10): `None` becomes
`Vector.empty`, `Some(v)` becomes `Vector(v)`, and `VisionVictory(p, v)`
becomes `VisionVictory(p, Vector(v))`.

- [ ] **Step 4: Ask the winner at Swear an Oath**

`SwearOath.scala`. The Visionary branch reads the result's Visions:

```scala
  val goalDecisionId: String = "chronicle.swear-oath.goal"
  /** Which Vision won, when the winner met several at once (catalog batch
    * 11, ruling 30). */
  val visionDecisionId: String = "chronicle.swear-oath.vision"
```

```scala
  val steps: Vector[Operation] = Vector(
    Branch((state, _) => state.game.current.result match
      case Some(GameResult(winner, VictoryKind.Visionary, visions)) =>
        visionGoal(state, winner, visions)
      case _ => ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
        ...unchanged...
```

```scala
  /** The winning Vision's goal, set only when it is not already the goal.
    * With several winning Visions the winner chooses which one won. */
  private def visionGoal(state: ReadyGame, winner: PlayerId,
      visions: Vector[VisionId]): Vector[Operation] =
    visions.filter(VisionRules.trueGoal(_).isDefined) match
      case Vector() => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${winner.value} won by no Vision with a goal")))
      case Vector(only) => swear(state, only)
      case several => Vector(
        Decide(visionDecisionId, winner, DecisionQuery.ChooseOne(
          several.map(vision => DecisionOption.Vision(
            DecisionOptionRef.Vision(vision))),
          heading = Some("Swear an Oath: which Vision won?"))),
        BuildOps((live, pending) => chosenVision(pending)
          .filter(several.contains)
          .toRight(PowerAnswers.missing(visionDecisionId))
          .map(vision => swearing(live, vision)), required = true))

  /** The answered Vision, when the answer names one. */
  private def chosenVision(pending: PendingTree): Option[VisionId] =
    PowerAnswers.one(pending, visionDecisionId).collect {
      case DecisionOptionRef.Vision(vision) => vision
    }

  private def swear(state: ReadyGame, vision: VisionId): Vector[Operation] =
    swearing(state, vision) match
      case Vector() => Vector.empty
      case ops => Vector(BuildOps((_, _) => Right(ops), required = true))

  /** `vision`'s goal as the new Oathkeeper goal, unless it already is. */
  private def swearing(state: ReadyGame, vision: VisionId)
      : Vector[CoreOperation] =
    VisionRules.trueGoal(vision)
      .filter(_ != state.game.campaign.oathkeeperGoal)
      .map(SetOathkeeperGoal(_)).toVector
```

Import `PowerAnswers` from `distantlands.gameplay.powers` beside
`OathkeeperGoalChoice`. Check `ChooseOne`'s parameter names against the
`goalDecisionId` `Decide` above it, which uses `heading = Some(...)`. The
option's label is the card's name, as `DecisionOptionRows` projects a
`DecisionOption.Vision`; confirm it renders "Vision of Faith" in the
projector suite, or add a row test if none covers `Vision` options.

Delete the temporary `headOption` reads that Task 1 left in
`SwearOath.scala` and `StateBasedEvaluation.scala`.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.RevealedVisionsSuite distantlands.gameplay.StateBasedEvaluationSuite distantlands.gameplay.chronicle.SwearOathSuite distantlands.serialization.GameEventFactsWireSuite distantlands.application.gamelog.GameLogHeadlineSuite distantlands.gameplay.EmpireEndingsSuite distantlands.gameplay.EmpireRoundEndSuite distantlands.gameplay.EmpireTurnSuite"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS. A replay or golden-journal test that stores
`"visionId"` fails here: regenerate the fixture the way its suite says,
and review the diff before committing.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/RevealedVisions.scala \
  src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala \
  src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala \
  src/main/scala/distantlands/gameplay/EmpireEndings.scala \
  src/main/scala/distantlands/model/GameEventProtocol.scala \
  src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/serialization/EndingEventCodec.scala \
  src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala \
  src/main/scala/distantlands/gameplay/chronicle/SwearOath.scala \
  src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala
git add $(git diff --name-only -- src/test)
git commit -m "feat(victory): a win names every qualifying Vision and Swear an Oath asks among them" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Check `git status` first, as in Task 1.

- [ ] **Step 7: Checkpoint review**

Dispatch a review subagent over the Task 4 commit, with ruling 30 and
findings 6 to 8. Ask it to check:

- the War Exhaustion priority, which reads each Exile's qualifying
  Visions, not the player;
- that `GameResult.visions` is set on every Visionary path;
- the codec's renamed keys;
- the Swear an Oath branch for zero, one and several Visions;
- the headline.

---

### Task 5: Close the slice

**Files:**
- Modify: `docs/ROADMAP.md:98, 211-217`
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md:3`
- Modify: `docs/architecture/bounded-visions-and-conspiracy.md:83-84`
- Modify: `docs/rules/implementation-traceability.md:118`

- [ ] **Step 1: Update the documents**

`docs/ROADMAP.md`: delete the line `- **Vision collection:** Desecrated
Ground.` under "Phase - Catalog batch - engine-blocked", and delete the
whole deferred item that starts `- [ ] **Desecrated Ground.** E08's
ruined face lets an Exile` (seven lines, ending with the rule gaps design
link). Finished work is deleted, not marked.

Spec status line 3: add slice 6 to whatever the line says when this
slice lands. With slice 5 not yet landed it reads `**Status:** designed
2026-11-07. Slices 0 to 4 and 6 landed.`; with slice 5 landed, `Slices 0
to 6 landed.`

`bounded-visions-and-conspiracy.md:83-84`: replace "Desecrated Ground
(`edifice.e08.ruined`) is not implemented; E08 never appears in a
generated game." with:

```markdown
Desecrated Ground (`edifice.e08.ruined`) lets an Exile at its site keep
every revealed Vision (`DesecratedGroundRule`, read by `CardSlots` and
`CardPlay`). A player's revealed Visions are a list, and a win records
every one whose goal is met; Swear an Oath asks which won when there are
several (catalog batch 11, slice 6).
```

`implementation-traceability.md:118`: in the last column, delete
"Desecrated Ground and " from "Altered Foundations, alternate banner
faces, Desecrated Ground and other Vision/reveal modifiers remain
deferred", and add to the evidence column: "Desecrated Ground keeps every
revealed Vision at its site (`DesecratedGroundSuite`), and a win by
several asks at Swear an Oath (`SwearOathSuite`)."

- [ ] **Step 2: Run every check**

Run: `./sbtw test`
Expected: PASS, all projects.

Run: `python3 scripts/check-architecture.py`
Expected: no violation. `CardPlayProcedure.scala` was 544 lines and
`CardSlots.scala` 410; both stay well under 800.

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken link.

Run the coverage command the project uses (`grep -n "coverage"
scripts/*.sh build.sbt README.md | head`).
Expected: statement coverage at or above 92%.

- [ ] **Step 3: Commit**

```bash
git add docs/ROADMAP.md \
  docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md \
  docs/architecture/bounded-visions-and-conspiracy.md \
  docs/rules/implementation-traceability.md
git commit -m "docs(catalog): close catalog batch 11 slice 6, Desecrated Ground" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Known limits

- **The kept line names the walk's actor's own Visions only.** Every
  reveal today is by the walk's actor. Slice 7 adds a reveal inside
  another player's walk and gives the line an off-turn variant then.
- **The Wake win does not ask.** Ruling 30 moves the choice to Swear an
  Oath; the win's headline names every qualifying Vision.
- **A pick that changes nothing writes no line.** When the winner picks
  the Vision whose goal is already current, Swear an Oath sets nothing and
  the log does not say which Vision they picked.
