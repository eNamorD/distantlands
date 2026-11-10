# Catalog Batch 11, Slice 3: Master of Disguise Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Master of Disguise (card 78): a Trade modifier, picked
first, after which the holder acts for the rest of that Trade as if they
held another player's advisers instead of their own.

**Architecture:** A new primitive `ActAsIfAdvisers(player)`, a sibling of
`ActAsIf`, writes walk-scoped scratch (`walkerAsIfAdvisers`) that the walk
end clears, live and on replay. One reader, `PowerAccess.advisersOf`,
returns the chosen player's advisers for the walk's actor while the scratch
is set. The three reads that decide whether an adviser serves or binds a
player in a Trade (`PowerAccess.locate`, `holdsFaceupAdviser` and
`EffectiveSuit.count`) and the phase menu's `accessible` read through it.
Every physical read of `PlayerState.advisers` stays.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read "Rulings" 8 and 19, the Master of Disguise bullet of "Readings", and
"Slice 3: Another player's advisers for one Trade".

## Prerequisite

This branch (`batch-11-slice-3`) was cut from `batch-11-slice-2` at its
plan commit `07c7769a`, so it carries rulings 15 to 18. Slice 3 uses none
of slice 2's code, but both slices edit `gameplay/PowerAccess.scala` and
the spec. Merge slice 2 into `0.2.0-alpha` first, then rebase this branch
onto `0.2.0-alpha` before Task 1. Slice 2 changes the ruled-site half of
`PowerAccess` (`ruledSites`, `rulingSites`, `usableSites` and the object's
doc comment); this plan changes only the adviser half. If a "before"
snippet below does not match after the rebase, the difference must be
slice 2's: keep slice 2's lines and apply this plan's change to the adviser
lines only.

## Plan-time findings

Inventoried at `07c7769a`.

1. **The walker finds every power; access decides.** `WalkerPowers.selected`
   (`gameplay/walker/WalkerPowers.scala:151-155`) puts the whole catalog in
   the pool, and walker powers name `RuleSourceRef.GameRule`, not an
   adviser's owner. Whether a Trade modifier is offered is
   `ModifierChoice.candidates` (`gameplay/walker/ModifierChoice.scala:58`,
   `PowerAccess.locate`) and `SelectedModifier.selectable`
   (`gameplay/powers/SelectedModifier.scala:103`, `locate` again). A
   persistent adviser rule binds through `PowerAccess.holdsFaceupAdviser`
   (Vow of Poverty at `gameplay/powers/economy/VowOfPoverty.scala:85`,
   Acting Troupe at `ActingTroupe.scala:42`). The Trade's yield and Cup of
   Plenty count suits through `EffectiveSuit.count`
   (`MusterSource.matching`). Routing those three covers ruling 8 inside a
   Trade, with no change to the walker.
2. **`accessible`'s adviser case** (`PowerAccess.scala:91`) serves only the
   phase menu (`PhasePowerProcedure.sourcesFrom`), never a Trade walk. It
   reads through `advisingPlayer` anyway, so the access rule has one
   definition.
3. **`faceupAdviserHolder` keeps the real holder.** No Trade reads it: its
   callers are Vow of Wandering (Muster and warband placement), Lost
   Tongue, Chaos Cult, Diplomat, Book Binders, Saddle Makers, Gossip, Plan
   Surcharge, Enemy Plans, Herald, Vow of Union and Sneak Attack. Through
   `advisersOf` a chosen card would have two holders, the actor and its
   owner, and the single answer would have to drop one. The spec's slice 3
   bullet now says so.
4. **Raw reads that stay.** Every other read of `PlayerState.advisers` is
   physical or never runs in the holder's Trade for the holder:
   - adviser limits (`AdviserLimit`, `LostTapestry`, `HolderAdviserLimit`);
   - card moves, faces and knowledge (`CardMovementOperations`,
     `OperationStateWrites`, `CardKnowledgeMoves`, `CardFaceOperations`,
     `OperationStateAdapter`, `OperationExecutor`, `InPlayCardResources`);
   - arrivals (`AdviserArrivals`) and locks (`LockedCards`,
     `RestrictionIcons`);
   - card play, setup, Negotiation and gathering (`CardPlay`, `CardSlots`,
     `CardPlayProcedure`, `SetupProcedure`, `LaterGameSetup`,
     `NegotiationDeal`, `GatherDenizens`);
   - Campaign (`CampaignRaid`, `CampaignPlanApplication`, `CampaignNotes`,
     `PlanContext` and every plan);
   - non-Trade powers (action, Wake, travel, dice, burn and When Played
     powers, `SuitOnlyAdvisers`, `Hospitality`, `BurnVows`);
   - Muster only (`GolemLegions`, and `MusterSource.locate`'s adviser
     source, which no Trade offers);
   - the inventory, the log and the projection (`RuleSourceIndex`,
     `PrintedCards`, `CardIndex`, `DecisionOptionRows`,
     `GamePresentationProjector`, `ChoiceWords`);
   - `RuledCards.of`, which reads a ruler's cards for Curfew, not the
     actor's.
5. **Pledge to Discord stays physical.** `EffectiveSuit.pledged` asks
   whether the card's real holder has Pledge faceup. The chosen player's
   advisers come as a whole set, so their Pledge already turns them
   Discord, and the holder's own Pledge touches only advisers the Trade
   ignores. Routing it would give the same answers.
6. **A chosen adviser's cost lands on its card.** `SelectedModifier`
   places a pick's cost `Location.OnCard(cardId)` from the actor's board
   (`Costs.onCard`). The card index is global, so a card among another
   player's advisers takes the tokens, and the empty-card rule applies to
   it as to any card. The holder pays (ruling 8).
7. **Pick order.** Master of Disguise is unavailable once any modifier is
   picked (FAQ), so nothing is picked on the holder's own advisers first.
   Each later pass re-runs `candidates` on the pass's state, which holds
   the scratch: the chosen player's adviser modifiers appear and the
   holder's vanish. The picked Master of Disguise stays applicable
   through `ModifierPicks.isPicked`. Relics and site cards keep their
   modifiers (ruling 19), because `locate`'s site half and relic half do
   not change.
8. **Vow of Poverty and the start.** A holder whose own Vow of Poverty
   refuses every Trade for favor must still reach the pick. The start
   preview walks the modifier decision, so the pick that lifts the Vow
   should keep "Start" waiting rather than refusing the Trade, as Awaited
   Return's start does (`AwaitedReturnSuite`, "it lets a player with no
   Supply trade"). Task 2's test pins that. If the start is refused
   instead, stop and report; do not change the preview.
9. **The scratch.** It mirrors `walkerAsIfSite`: set by the primitive,
   cleared by `ProcedureWalker.finish` (`ProcedureWalker.scala:213`) and by
   `WalkerCompleted` in `WalkerReplay` (`WalkerReplay.scala:172`), never
   projected and never in a save. The codec lists the primitive in its
   `TurnOperation` union (`WalkerOperationCodec.scala:43`).
10. **The line.** "Acted as if holding the advisers of {p2}." names only the
    chosen player, a non-actor, as the log-line rule asks. It parallels
    Small Friends' "Acted as if at {site}."
11. **The card.** `MasterOfDisguiseCard` moves out of
    `catalog/holding/HoldingArcaneDenizens.scala` and gains
    `modifier = Some(ModifierMark.Action(MajorActionType.Trade))` (the
    batch's catalog corrections). `CardCatalogSuite`'s "printed modifier
    marks agree with the engine's modifiers and plans" checks the mark
    against the power's `actions`. `NewFoundations` imports
    `gameplay.powers.economy._`, so it needs no change, and no test
    imports the card from holding.
12. **For slice 5.** Vow of Kinship must read its holder through
    `holdsFaceupAdviser`, so Master of Disguise gets around it (FAQ). The
    spec's slice 3 bullet records this.

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-3`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- Physical reads of `PlayerState.advisers` do not change (finding 4), and
  `faceupAdviserHolder` keeps the real holder (finding 3).
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Test helper names say what they build.
- Log lines name only non-actors.

---

### Task 1: The as-if-advisers record and its readers

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (after `ActAsIf`, line 692)
- Modify: `src/main/scala/distantlands/model/GameState.scala` (after `walkerAsIfSite`, line 277)
- Modify: `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala` (after `actAsIf`, line 129)
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala` (after the `ActAsIf` case, line 216)
- Modify: `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala:213`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala:172`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` (lines 43, 318, 696)
- Modify: `src/main/scala/distantlands/gameplay/PowerAccess.scala`
- Modify: `src/main/scala/distantlands/gameplay/EffectiveSuit.scala` (`count`)
- Create: `src/test/scala/distantlands/gameplay/ActAsIfAdvisersSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/PowerAccessSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/EffectiveSuitSuite.scala`
- Modify: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces: `final case class ActAsIfAdvisers(player: PlayerId) extends PrimitiveOperation` in `distantlands.model`; `CurrentGameState.walkerAsIfAdvisers: Option[PlayerId]` (default `None`); wire kind `"act-as-if-advisers"` with field `"playerId"`; `PowerAccess.advisingPlayer(ready: ReadyGame, player: PlayerId): PlayerId`; `PowerAccess.advisersOf(ready: ReadyGame, player: PlayerId): Vector[AdviserState]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/ActAsIfAdvisersSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.walker.{ProcedureWalker, WalkerCompleted}
import distantlands.model._
import distantlands.testkit.{Outcomes, RawOperationBatch, RecordedOps, Table}
import distantlands.testkit.Outcomes.{finished, parked, recorded}
import distantlands.testkit.ProcedureWalkerFixture._
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `ActAsIfAdvisers` (catalog batch 11, slice 3): it records the player
  * whose advisers the walk's actor acts with, a parked walk keeps it, and
  * the walk's end clears it, live and on replay. */
class ActAsIfAdvisersSuite extends munit.FunSuite:
  /** A seated player of the walker fixture other than its walk's actor. */
  private val chosen = ready.game.current.players.map(_.player)
    .find(_ != ready.game.current.walkActor).get

  private def run(state: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(state, ops.toVector)

  test("it records the chosen player"):
    val acting = accepted(run(Table.start.ready, ActAsIfAdvisers(p2)))
    assertEquals(acting.game.current.walkerAsIfAdvisers, Some(p2))

  test("it refuses the walk's actor and an unseated player"):
    // p1 acts on Table.start, which has no walk entry.
    rejected(run(Table.start.ready, ActAsIfAdvisers(p1)))
    rejected(run(Table.start.ready, ActAsIfAdvisers(PlayerId("nobody"))))

  test("a walk parked after it holds the player, and its events rebuild it"):
    val tree: Operation = Sequence(ActAsIfAdvisers(chosen), decide)
    val park = parked(ProcedureWalker.advance(ready, tree, None, noPowers))
    assertEquals(RecordedOps.of(recorded(park.events.head)),
      Vector[CoreOperation](ActAsIfAdvisers(chosen)))
    assertEquals(applyEvents(ready, park.events).game.current
      .walkerAsIfAdvisers, Some(chosen))

  test("the walk's end clears it, live and on replay"):
    val done = finished(ProcedureWalker.advance(ready,
      ActAsIfAdvisers(chosen), None, noPowers))
    assertEquals(done.treeless.game.current.walkerAsIfAdvisers, None)
    val acting = accepted(run(ready, ActAsIfAdvisers(chosen)))
    val completed = accepted(ProcedureWalker.applyRecorded(
      OathState.Ready(acting), WalkerCompleted(ActionRef.Trade)))
    assertEquals(Outcomes.ready(completed).game.current.walkerAsIfAdvisers,
      None)
```

In `src/test/scala/distantlands/gameplay/PowerAccessSuite.scala`, append
after the last test (it uses the suite's `actor` = p1, `other` = p2,
`card` = Errand Boy and `wrestlers`):

```scala

  /** `state` with `actor`, the walk's actor on their own turn, acting as if
    * holding `chosen`'s advisers, as `ActAsIfAdvisers` leaves it. */
  private def actingWithAdvisersOf(state: ReadyGame, chosen: PlayerId)
      : ReadyGame =
    state.updateCurrent(_.copy(walkerAsIfAdvisers = Some(chosen)))

  /** p1 on their Act holding Wrestlers, and p2 holding Errand Boy, both
    * faceup advisers. */
  private val eachHoldingOneAdviser = Table.start.turn(actor, Phase.Act)
    .adviser(actor, wrestlers).adviser(other, card).ready

  test("acting with another player's advisers, the actor uses theirs and " +
      "not their own"):
    val state = actingWithAdvisersOf(eachHoldingOneAdviser, other)
    val othersAdvisers = state.game.current.player(other).get.advisers
    assertEquals(PowerAccess.advisersOf(state, actor), othersAdvisers)
    assertEquals(PowerAccess.locate(state, actor, card),
      Some(PowerAccess.Held.InPlayArea))
    assertEquals(PowerAccess.locate(state, actor, wrestlers), None)
    assert(PowerAccess.holdsFaceupAdviser(state, actor, card))
    assert(!PowerAccess.holdsFaceupAdviser(state, actor, wrestlers))
    assert(PowerAccess.accessible(RuleSourceRef.Adviser(other, card),
      RuleSourceFace.FaceUp, state, actor))
    assert(!PowerAccess.accessible(RuleSourceRef.Adviser(actor, wrestlers),
      RuleSourceFace.FaceUp, state, actor))

  test("the chosen player keeps their advisers, and the card keeps its " +
      "real holder"):
    val state = actingWithAdvisersOf(eachHoldingOneAdviser, other)
    assertEquals(PowerAccess.advisersOf(state, other),
      state.game.current.player(other).get.advisers)
    assert(PowerAccess.holdsFaceupAdviser(state, other, card))
    assertEquals(PowerAccess.faceupAdviserHolder(state, card), Some(other))

  test("without the record, each player acts with their own advisers"):
    assertEquals(PowerAccess.locate(eachHoldingOneAdviser, actor, wrestlers),
      Some(PowerAccess.Held.InPlayArea))
    assertEquals(PowerAccess.locate(eachHoldingOneAdviser, actor, card), None)
```

In `src/test/scala/distantlands/gameplay/EffectiveSuitSuite.scala`, append
after the last test (it already imports `Table`, `p1`, `p2`, `catalog`,
and names `wrestlers`, an Order card, and `homesteaders`, a Hearth card):

```scala

  test("a player acting with another player's advisers counts theirs"):
    val state = Table.start.turn(p1, Phase.Act).adviser(p1, wrestlers)
      .adviser(p2, homesteaders).ready
      .updateCurrent(_.copy(walkerAsIfAdvisers = Some(p2)))
    assertEquals(EffectiveSuit.count(catalog, state, p1, Suit.Order), 0)
    assertEquals(EffectiveSuit.count(catalog, state, p1, Suit.Hearth), 1)
```

In `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
after the test "an as-if site round trips":

```scala

  test("an as-if adviser holder round trips"):
    val event = recordedOperation(ActAsIfAdvisers(PlayerId("p2")))
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0)("kind").str,
      "act-as-if-advisers")
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.ActAsIfAdvisersSuite"`
Expected: compilation fails: `ActAsIfAdvisers` and `walkerAsIfAdvisers`
are not found.

- [ ] **Step 3: Add the primitive and the scratch**

In `src/main/scala/distantlands/model/CoreOperations.scala`, after the
`ActAsIf` case class:

```scala

/** Records that the walk's actor acts as if they held `player`'s advisers
  * instead of their own for the rest of the walk, in
  * `CurrentGameState.walkerAsIfAdvisers` (Master of Disguise, catalog
  * batch 11, slice 3). The walker clears it when the walk ends. The
  * journal records it, so a game reloaded from its events holds the same
  * player. It moves no card and writes no Game Log line.
  */
final case class ActAsIfAdvisers(player: PlayerId) extends PrimitiveOperation
```

In `src/main/scala/distantlands/model/GameState.scala`, after
`walkerAsIfSite: Option[SiteId] = None,`:

```scala
    // The player whose advisers the walk's actor acts as if they held, for
    // the rest of the walk (Master of Disguise, catalog batch 11, slice 3).
    // Walker scratch, set by `ActAsIfAdvisers`: replay rebuilds it from
    // the recorded `ActAsIfAdvisers`, and the walker and `WalkerCompleted`
    // clear it with the dice pools. Never projected.
    walkerAsIfAdvisers: Option[PlayerId] = None,
```

In `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala`,
after `actAsIf`:

```scala

  /** Records the player whose advisers the walk's actor acts with; the
    * walker clears it when the walk ends. The player is seated and is not
    * the walk's actor. */
  def actAsIfAdvisers(ready: ReadyGame, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    if !current.players.exists(_.player == player) then Left(
      InvalidDescription(s"${player.value} is not seated"))
    else if current.walkActor == player then Left(InvalidDescription(
      s"${player.value} cannot act as if holding their own advisers"))
    else Right(ready.updateCurrent(_.copy(walkerAsIfAdvisers = Some(player))))
```

In `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`,
after the `ActAsIf` case:

```scala
      case (result, ActAsIfAdvisers(player)) =>
        result.flatMap(TurnStateOperations.actAsIfAdvisers(_, player))
```

In `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala`,
`finish`, change

```scala
        walkerAsIfSite = None, walkerPicks = Vector.empty,
```

to

```scala
        walkerAsIfSite = None, walkerAsIfAdvisers = None,
        walkerPicks = Vector.empty,
```

In `src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala`, the
`WalkerCompleted` case, after `walkerAsIfSite = None,`:

```scala
            walkerAsIfAdvisers = None,
```

In `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`:
- In `TurnOperation`, end the union's last two lines as

```scala
    QueueProcedure | Defer | TakeDeferred | ActAsIf | ActAsIfAdvisers |
    OpenPick | ClosePick | TrackPawn | ClearTrackedPawn
```
- In the encoder, after the `ActAsIf` case:

```scala
      case ActAsIfAdvisers(player) => ujson.Obj(
        "kind" -> "act-as-if-advisers", "playerId" -> player.value)
```

- In the decoder, after `case "act-as-if" => ...`:

```scala
    case "act-as-if-advisers" =>
      Right(ActAsIfAdvisers(PlayerId(value("playerId").str)))
```

- [ ] **Step 4: Read the advisers through one reader**

In `src/main/scala/distantlands/gameplay/PowerAccess.scala`:

Replace the adviser helpers

```scala
  /** The player holding `card` faceup among their advisers, if any. */
  def faceupAdviserHolder(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    ready.game.current.players.find(hasFaceupAdviser(_, card)).map(_.player)

  /** Whether `player` holds `card` faceup among their advisers. */
  def holdsFaceupAdviser(ready: ReadyGame, player: PlayerId, card: DenizenId)
      : Boolean =
    ready.game.current.player(player)
      .exists(hasFaceupAdviser(_, card))

  private def hasFaceupAdviser(player: PlayerState, card: DenizenId): Boolean =
    player.advisers.exists {
      case DenizenState(held, Orientation.FaceUp, _) => held == card
      case _ => false
    }
```

with

```scala
  /** The player whose advisers `player` acts with: the one Master of
    * Disguise chose, for the walk's actor while `ActAsIfAdvisers` holds
    * (`CurrentGameState.walkerAsIfAdvisers`), and `player` otherwise. */
  def advisingPlayer(ready: ReadyGame, player: PlayerId): PlayerId =
    val current = ready.game.current
    current.walkerAsIfAdvisers.filter(_ => current.walkActor == player)
      .getOrElse(player)

  /** The advisers `player` acts with ([[advisingPlayer]]). A reader that
    * decides whether an adviser's power serves or binds a player reads
    * these. A physical fact, such as a move, the adviser limit or the
    * board shown, reads `PlayerState.advisers`. */
  def advisersOf(ready: ReadyGame, player: PlayerId): Vector[AdviserState] =
    ready.game.current.player(advisingPlayer(ready, player)).toVector
      .flatMap(_.advisers)

  /** The player who really holds `card` faceup among their advisers, if
    * any. It reads no [[advisersOf]]: no Trade reads it, and a card a
    * Master of Disguise holder acts with would otherwise have two
    * holders. */
  def faceupAdviserHolder(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    ready.game.current.players.find(player => faceUp(player.advisers, card))
      .map(_.player)

  /** Whether `player` acts with `card` faceup among their advisers
    * ([[advisersOf]]). */
  def holdsFaceupAdviser(ready: ReadyGame, player: PlayerId, card: DenizenId)
      : Boolean =
    faceUp(advisersOf(ready, player), card)

  private def faceUp(advisers: Vector[AdviserState], card: DenizenId)
      : Boolean =
    advisers.exists {
      case DenizenState(held, Orientation.FaceUp, _) => held == card
      case _ => false
    }
```

In `accessible`, change the adviser case

```scala
      case RuleSourceRef.Adviser(owner, _) => owner == actor &&
```

to

```scala
      case RuleSourceRef.Adviser(owner, _) =>
        owner == advisingPlayer(ready, actor) &&
```

keeping its next two lines as they are.

In `locate`, replace

```scala
    lazy val inPlay = current.player(actor).exists(player =>
      player.advisers.exists {
        case d: DenizenState => d.id == card && d.orientation == Orientation.FaceUp
        case _ => false
      } || player.relics.exists(relic =>
        relic.id == card && relic.orientation == Orientation.FaceUp))
```

with

```scala
    lazy val inPlay = advisersOf(ready, actor).exists {
        case d: DenizenState => d.id == card && d.orientation == Orientation.FaceUp
        case _ => false
      } || current.player(actor).exists(_.relics.exists(relic =>
        relic.id == card && relic.orientation == Orientation.FaceUp))
```

and in its doc comment change "A faceup adviser or relic in the actor's
play area is `InPlayArea`." to "A faceup adviser the actor acts with
([[advisersOf]]) or a faceup relic in their play area is `InPlayArea`."

At the end of the object's doc comment, after the as-if site paragraph,
add:

```scala
  *
  * Master of Disguise makes its user act as if they held another player's
  * advisers instead of their own for the rest of a Trade. `ActAsIfAdvisers`
  * records the player as walker scratch
  * (`CurrentGameState.walkerAsIfAdvisers`), and the walk's end clears it.
  * For the walk's actor, that player's advisers then take theirs' place in
  * `locate`, `accessible` and `holdsFaceupAdviser` ([[advisersOf]]).
```

In `src/main/scala/distantlands/gameplay/EffectiveSuit.scala`, `count`,
replace

```scala
    ready.game.current.player(player).toVector.flatMap(_.advisers).collect {
```

with

```scala
    PowerAccess.advisersOf(ready, player).collect {
```

and change its doc comment's first sentence to "`player`'s faceup advisers
of `suit`, as they act with them ([[PowerAccess.advisersOf]]), each by its
weight."

- [ ] **Step 5: Run the tests to verify they pass**

Run:

```bash
./sbtw "testOnly distantlands.gameplay.ActAsIfAdvisersSuite distantlands.gameplay.ActAsIfSuite distantlands.gameplay.PowerAccessSuite distantlands.gameplay.EffectiveSuitSuite distantlands.serialization.WalkerOperationWireSuite distantlands.gameplay.powers.economy.*"
```

Expected: PASS. The economy suites pass unchanged: with no record, every
reader answers as before.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/model/GameState.scala src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala src/main/scala/distantlands/gameplay/walker/WalkerReplay.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/main/scala/distantlands/gameplay/PowerAccess.scala src/main/scala/distantlands/gameplay/EffectiveSuit.scala src/test/scala/distantlands/gameplay/ActAsIfAdvisersSuite.scala src/test/scala/distantlands/gameplay/PowerAccessSuite.scala src/test/scala/distantlands/gameplay/EffectiveSuitSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): act as if holding another player's advisers for a walk

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Master of Disguise

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/economy/MasterOfDisguise.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala:17-22` (remove the card)
- Modify: `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (remove `denizen.master-of-disguise`)
- Modify: `src/test/resources/notes/declared-keys.txt` (snapshot)
- Create: `src/test/scala/distantlands/gameplay/powers/economy/MasterOfDisguiseSuite.scala`

**Interfaces:**
- Consumes: `ActAsIfAdvisers(player: PlayerId)` and `CurrentGameState.walkerAsIfAdvisers` (Task 1).
- Produces: `MasterOfDisguiseCard` and `case object MasterOfDisguise` in `distantlands.gameplay.powers.economy`; `MasterOfDisguise.decisionId: String` (`"master-of-disguise.player"`); `MasterOfDisguise.notFirst: String`; note key `acted`.

- [ ] **Step 1: Write the failing suite**

Create
`src/test/scala/distantlands/gameplay/powers/economy/MasterOfDisguiseSuite.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.gameplay.actions.economy.TradeProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.{BattleHonorsCard, LongbowsCard,
  ScoutsCard, WrestlersCard}
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look, MenuPlay,
  Outcomes, PhasePowerHarness, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Master of Disguise (card 78, ruling 8): picked first in a Trade, it asks
  * whose advisers its holder acts with, and for the rest of that Trade
  * those advisers are the holder's and the holder's own do nothing. */
class MasterOfDisguiseSuite extends munit.FunSuite:
  private val disguise = MasterOfDisguiseCard.id
  private val power = MasterOfDisguise
  /** p1's pawn site. */
  private val home = AncientCitySite.id
  /** A site in play in another region. */
  private val away = Table.siteInAnotherRegionThan(home)
  /** A plain Order denizen to Trade with. */
  private val wrestlers = WrestlersCard.id
  /** Plain Order denizens, held as advisers. */
  private val longbows = LongbowsCard.id
  private val scouts = ScoutsCard.id
  private val battleHonors = BattleHonorsCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  private val signal = SecretSignalCard.id
  private val familiar = FriendlyFamiliarCard.id
  private val poverty = VowOfPovertyCard.id
  private val cup = CupOfPlentyCard.id
  private val modifiers =
    ModifierPicks.decisionId(PowerWindow.TradeModifierSelection)
  private val rules = PowerRules.withWalkerPowers

  /** p1 at Ancient City holding Master of Disguise, with Wrestlers beside
    * them, `secrets` faceup secrets, and the Order bank holding 5 favor. */
  private def disguiseHolderBesideWrestlers(secrets: Int = 2): Table =
    Table.start.pawn(p1, at = home).adviser(p1, disguise)
      .denizen(wrestlers, at = home).secrets(p1, faceUp = secrets)
      .bankFavor(Suit.Order, 5)

  /** p1 starts a Trade for favor, parked on the modifier decision. */
  private def tradeForFavorBegun(ready: ReadyGame): OathTransition =
    accepted(MenuPlay.started(rules, Ready(ready), p1, ActionRef.Trade,
      Vector(DecisionOptionRef.Button("favor"))))

  /** `from`, then p1's answer `ref` at `decisionId`; the events run from
    * the Trade's start. */
  private def answeredWith(from: OathTransition, decisionId: String,
      ref: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))
    next.copy(events = from.events ++ next.events)

  /** `from` with Master of Disguise picked and `chosen` named. */
  private def disguisedAs(from: OathTransition, chosen: PlayerId)
      : OathTransition =
    answeredWith(answeredWith(from, modifiers,
      DecisionOptionRef.Denizen(disguise)), MasterOfDisguise.decisionId,
      DecisionOptionRef.Player(chosen))

  /** Each option's id and state at the parked decision, by id. */
  private def optionStates(at: OathTransition): Option[Map[String, String]] =
    PhasePowerHarness.queryOf(at, p1).map(_.offeredOptions
      .map(option => option.id -> option.state).toMap)

  /** The reason the parked decision gives for the option `id`. */
  private def reasonFor(at: OathTransition, id: String): Option[String] =
    PhasePowerHarness.queryOf(at, p1)
      .flatMap(_.offeredOptions.find(_.id == id)).flatMap(_.reason)

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  test("it is offered only as the first pick, and asks which other " +
      "player"):
    val begun = tradeForFavorBegun(disguiseHolderBesideWrestlers()
      .adviser(p1, signal).ready)
    assertEquals(optionStates(begun), Some(Map("start" -> "available",
      disguise.value -> "available", signal.value -> "available")))
    val signalled = answeredWith(begun, modifiers,
      DecisionOptionRef.Denizen(signal))
    assertEquals(optionStates(signalled).flatMap(_.get(disguise.value)),
      Some("unavailable"))
    assertEquals(reasonFor(signalled, disguise.value),
      Some(MasterOfDisguise.notFirst))
    val asked = answeredWith(begun, modifiers,
      DecisionOptionRef.Denizen(disguise))
    assertEquals(PhasePowerHarness.offered(asked, p1),
      Some(Vector("player" -> p2.value, "player" -> p3.value)))

  test("the chosen player's advisers count toward the Trade and the " +
      "holder's own do not, and the line names the player"):
    // p1's own Battle Honors is an Order card; so are p2's Longbows and
    // Scouts.
    val ready = disguiseHolderBesideWrestlers().adviser(p1, battleHonors)
      .adviser(p2, longbows).adviser(p2, scouts).ready
    // No other modifier to pick, so the Trade goes on to its source.
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(Outcomes.ready(acting.state).game.current.walkerAsIfAdvisers,
      Some(p2))
    val done = answeredWith(acting, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(wrestlers))
    val end = Look(done.state)
    // 1, and 1 more for each of p2's two Order advisers.
    assertEquals(end.favor(p1), Look(ready).favor(p1) + 3)
    assertEquals(end.adviserTokens(p1, disguise), Tokens(0, 1))
    assertEquals(Outcomes.ready(done.state).game.current.walkerAsIfAdvisers,
      None)
    assertEquals(said(done.events), Vector(NoteText.Said("acted",
      s"Acted as if holding the advisers of ${p2.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)

  test("the chosen player's Trade modifier is offered and the holder pays " +
      "for it, and the holder's own is not offered"):
    // Three secrets: Master of Disguise, Friendly Familiar and the Trade.
    val ready = disguiseHolderBesideWrestlers(secrets = 3).adviser(p1, signal)
      .adviser(p2, familiar).denizen(errandBoy, at = away).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(optionStates(acting), Some(Map("start" -> "available",
      familiar.value -> "available")))
    val picked = answeredWith(acting, modifiers,
      DecisionOptionRef.Denizen(familiar))
    val now = Look(picked.state)
    assertEquals(now.adviserTokens(p2, familiar), Tokens(0, 1))
    assertEquals(now.faceUpSecrets(p1), 1)
    assertEquals(now.faceUpSecrets(p2), Look(ready).faceUpSecrets(p2))
    assertEquals(PhasePowerHarness.offered(picked, p1),
      Some(Vector("site" -> away.value)))

  test("the holder's relic modifier stays, and reads the chosen advisers"):
    // p1's own Battle Honors would match Wrestlers; p2's Secret Signal, an
    // Arcane card, does not.
    val ready = disguiseHolderBesideWrestlers().adviser(p1, battleHonors)
      .relic(p1, cup).adviser(p2, signal).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(optionStates(acting), Some(Map("start" -> "available",
      cup.value -> "available", signal.value -> "available")))
    val cupPicked = answeredWith(acting, modifiers,
      DecisionOptionRef.Relic(cup))
    val started = answeredWith(cupPicked, modifiers, ModifierPicks.start)
    val done = answeredWith(started, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(wrestlers))
    val end = Look(done.state)
    // No adviser of p2 matches the Order card: no Supply spent, and 1
    // favor gained.
    assertEquals(end.supply(p1), Look(ready).supply(p1))
    assertEquals(end.favor(p1), Look(ready).favor(p1) + 1)

  test("the chosen player's Vow of Poverty binds the holder"):
    val ready = disguiseHolderBesideWrestlers().adviser(p2, poverty)
      .adviser(p2, signal).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    // Every Trade for favor is refused, so the Trade cannot start.
    assertEquals(optionStates(acting).flatMap(_.get("start")),
      Some("unavailable"))

  test("the holder's own Vow of Poverty is lifted"):
    val ready = disguiseHolderBesideWrestlers().adviser(p1, poverty).ready
    // The Vow refuses every Trade for favor without the pick, so the
    // start waits for it.
    assertEquals(EconomyActions.optionStatesAtStart(rules, ready, p1,
      ActionRef.Trade, Vector(DecisionOptionRef.Button("favor"))),
      Some(Vector("start" -> "unavailable", disguise.value -> "available")))
    val done = answeredWith(disguisedAs(tradeForFavorBegun(ready), p2),
      TradeProcedure.decisionId, DecisionOptionRef.Denizen(wrestlers))
    assertEquals(Look(done.state).favor(p1), Look(ready).favor(p1) + 1)

  test("a game parked after the choice and rebuilt from its events keeps " +
      "it"):
    val ready = disguiseHolderBesideWrestlers().adviser(p2, longbows).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    val rebuilt = ActPowerHarness.replayed(rules, ready, acting.events)
    assertEquals(rebuilt.game.current.walkerAsIfAdvisers, Some(p2))
    assertEquals(rebuilt, Outcomes.ready(acting.state))
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.MasterOfDisguiseSuite"`
Expected: compilation fails: `MasterOfDisguiseCard` is not in
`distantlands.gameplay.powers.economy`, and `MasterOfDisguise` is not found.

- [ ] **Step 3: Move the card and write the power**

Delete lines 17 to 22 of
`src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala`
(`object MasterOfDisguiseCard` and its body) and the blank line before
them. Council Arbiter still uses every import.

Create `src/main/scala/distantlands/gameplay/powers/economy/MasterOfDisguise.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  ModifierMark, PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.{AsksOnSelection, Contribution,
  ModifierPicks, OfferReason, PowerCtx}
import distantlands.gameplay.powers.{PowerAnswers, SelectedModifier}
import distantlands.model._

object MasterOfDisguiseCard extends Denizen(DenizenId("78"), "Master of Disguise", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.master-of-disguise"),
    persistent = false, cost = Cost(secret = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Act as if you had another player's advisers instead. _(You " +
      "can't use your other advisers.)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Master of Disguise (card 78, adviser-only), a selected Trade modifier
  * costing one secret, paid onto it when picked: "Act as if you had another
  * player's advisers instead. (You can't use your other advisers.)"
  *
  * Once picked and paid for, it asks which other player, and records the
  * answer as state with `ActAsIfAdvisers` (catalog batch 11, ruling 8).
  * For the rest of the Trade the chosen player's advisers take the
  * holder's place wherever a rule reads whom an adviser serves or binds
  * (`PowerAccess.advisersOf`): their suits count, their Trade modifiers are
  * offered and the holder pays their costs, and their vows bind the
  * holder. The holder's own advisers do nothing, this card included once
  * picked. Their relics and the site cards they may use still offer their
  * modifiers (ruling 19).
  *
  * Unavailable once another modifier is picked (FAQ). Its line names the
  * player: "Acted as if holding the advisers of {p2}."
  */
case object MasterOfDisguise extends SelectedModifier, AsksOnSelection:
  val id: PowerId = MasterOfDisguiseCard.power.id

  val card: Denizen = MasterOfDisguiseCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)
  override def cost: Cost = MasterOfDisguiseCard.power.cost

  /** The question its pick asks: whose advisers the holder acts with. */
  val decisionId: String = "master-of-disguise.player"
  /** Why it is unavailable once another modifier is picked. */
  val notFirst: String = "Only as your first modifier"

  private val notes = NoteSet(id)
  /** "Acted as if holding the advisers of {p2}." */
  val acted: NoteKey = notes.key("acted", Vector(
    NotePart.Text("Acted as if holding the advisers of "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    Option.when(ModifierPicks.cards(ctx.answered).nonEmpty)(
      OfferReason.Unavailable(notFirst))

  /** The player question, then the answer recorded as state, then the
    * line. The pick and its payment come before the question, so they are
    * judged on the holder's own advisers. */
  def selectionQuestions(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[Operation] = Vector(
    Decide(decisionId, actor, DecisionQuery.ChooseOne(
      ready.game.current.players.map(_.player).filterNot(_ == actor)
        .map(player => DecisionOption.Player(DecisionOptionRef.Player(player))),
      heading = Some("Master of Disguise: act with whose advisers?"))),
    BuildOps((_, tree) => Right(PowerAnswers.last(tree.answered, decisionId)
      .toVector.collect { case DecisionOptionRef.Player(player) =>
        ActAsIfAdvisers(player) })),
    Note(id, actedNote))

  private def actedNote(states: NoteStates): Option[LogNote] =
    NoteSupport.answer(states, decisionId).collect {
      case DecisionOptionRef.Player(player) =>
        acted(PowerSourceRef.Card(cardId), NoteArg.Player(player))
    }
```

In `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`, add
`MasterOfDisguise` to the `economy` import (alphabetically, after
`MapLibrary`) and to `all` after `SmallFriends,`:

```scala
      SkilledMerchants, SmallFriends, MasterOfDisguise,
```

Remove the line `denizen.master-of-disguise` from
`src/test/resources/powers/unimplemented.txt`.

Add this line to `src/test/resources/notes/declared-keys.txt` in sorted
order (the file is sorted by key; it goes before the first
`denizen.m...` key that sorts after it):

```
denizen.master-of-disguise acted = Acted as if holding the advisers of {0}.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./sbtw "testOnly distantlands.gameplay.powers.economy.* distantlands.gameplay.cards.CardCatalogSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.powers.PowerImplementationStatusSuite distantlands.gameplay.powers.PowerCatalogUniquenessSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.ModifierMenuOptionsSuite distantlands.gameplay.TradeProcedureSuite"
```

Expected: PASS. If "the holder's own Vow of Poverty is lifted" fails at
the start, read finding 8 and stop.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/economy/MasterOfDisguise.scala src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/economy/MasterOfDisguiseSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Master of Disguise trades with another player's advisers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Close the slice

**Files:**
- Modify: `docs/ROADMAP.md` (the engine-blocked phase)
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md` (status)

- [ ] **Step 1: Update the roadmap and the spec**

In `docs/ROADMAP.md`, under **Phase - Catalog batch - engine-blocked**,
delete the line:

```markdown
- **Acting as another player's advisers:** Master of Disguise.
```

In the spec, change the status line to

```markdown
**Status:** designed 2026-11-07. Slices 0 to 3 landed.
```

(If slice 2 has not landed, stop: see Prerequisite.) Then run:

```bash
python3 scripts/check-markdown-links.py
```

Expected: passes.

- [ ] **Step 2: Run the full gates**

```bash
python3 scripts/check-architecture.py
```

```bash
./sbtw frontend/test
```

```bash
./sbtw clean coverage test coverageReport
```

Run the last in the background. Expected: every test passes, and
statement coverage is at least 92%.

- [ ] **Step 3: Commit**

```bash
git add docs/ROADMAP.md docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 11 slice 3, Master of Disguise

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
