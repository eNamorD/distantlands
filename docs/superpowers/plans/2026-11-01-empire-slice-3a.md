# Empire Slice 3a Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players change roles in play. An Exile becomes a Citizen, or the
Chancellor when the game has no Empire, and a Citizen becomes an Exile. The
first two sources ship with the procedures: the Grand Scepter's Action and
the Citizen's self-exile minor action. The first-game option that ignores
"become a Citizen" powers is read, and a Chancellor who appears mid-round
leaves the round's order alone (R4).

**Architecture:**
- A new package, `gameplay/roles`, holds the role changes:
  - `RoleChanges` turns each change into the operations CR p. 34 lists, from
    state and the player's answer;
  - `Citizenship` wraps them as walker trees, asking which warbands turn
    purple only when the purple bank runs short.
- Every source builds its tree from `Citizenship`. Slice 3b's sources (the
  Scepter's battle plan and deal term, five denizens, the Throne's offer)
  reuse it unchanged.
- The round's first player is stored on `GameTracks` when the round begins.
  `TurnOrder.roundStart` reads it; the next round's start is computed from
  roles when the round ends.
- Two small operation changes: a new `UsurperLimiterRemoved` primitive, and
  `SetOathkeeper` may turn an unchanged holder's title back to its
  Oathkeeper side.
- The first-game option is a field on `FirstGameSupportState`, on by
  default. Game start never turns it off; tests turn it off with a `Table`
  step.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 3: Role changes in play", "Rulings" and
"Testing").

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/empire-slice-3a` on branch `empire-slice-3a`, off
    `0.2.0-alpha`. Never work in the main checkout.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and
    `node_modules` into the worktree.
  - Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused
  values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of
    at least 92%
  - `./sbtw frontend/test`
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines
    and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Imperial roles:** game start keeps refusing them. This slice is reached
  only through test tables (`Table.imperial`, `Table.relic`).
- **Empire:** a game has an Empire while a Chancellor is seated
  (`Imperials.chancellor(ready).nonEmpty`).
- **Wire:** old journals need not decode. New operations get a round-trip
  test.
- **Log lines** name only players other than the actor ("Gave 3 favor to
  p1 and became an Exile."), never the actor.
- **Hover reasons:** show an unavailable option disabled with a reason.
  Hide only an option the player's current role can never use.

## Rulings for this slice

Decided with the user on 2026-11-01, or by the controller where noted.
Task 8 records the user's rulings in `docs/rules/ambiguities.md`.

- **Split.** Slice 3 ships as 3a (this plan) and 3b: the Scepter's battle
  plan and deal term, the five denizens, and the Throne's offer.
- **R18, short purple bank (user).** When the purple bank holds fewer
  warbands than the player has on their board and at sites, the player
  splits the bank's warbands over their board and each site that holds
  their warbands. At each place, the warbands left over return to the
  player's own bank. A site can end with fewer warbands, or none.
- **R4 storage (user).** The round's first player is recorded when the
  round begins. The next round's first player is the Chancellor, or the
  first player when there is none.
- **R19, Supremacy title (controller).** Under the Oath of Supremacy a
  player who becomes a Citizen while holding the title passes it to the
  Chancellor, on either side. CR p. 35 says Citizens never hold that title,
  so the "also transfers" clause cannot be limited to the Usurper side. A
  player who becomes the Chancellor keeps it, turned to its Oathkeeper side.
- **Scepter Action listing (controller).** An Imperial holder's menu leaves
  the Scepter's Action out, since an Imperial role can never use it. Under
  the first-game option an Exile holder sees it disabled with "Citizenship
  is ignored due to setup option".
- **Self-exile listing (controller).** Only a Citizen's menu lists "Exile
  yourself". Self-exile is not a "become a Citizen" power, so the
  first-game option does not touch it.
- **Order of steps (controller).** Each role change runs in one operation
  batch, in this order, so that every intermediate state passes validation:
  1. create the purple bank;
  2. fix the title;
  3. discard the Vision;
  4. replace or return site warbands;
  5. return board warbands to their bank;
  6. change the role;
  7. take the new board warbands;
  8. take the Scepter and lift the Usurper Limiter (Chancellor only);
  9. end the Act phase;
  10. refresh Supply.

  The board must be emptied before `RoleChanged`, because a board's
  warband kind follows its owner's role (`PlayerForceKind.of`).
- **Frontend (controller).** No new frontend code. The menu, the
  Distribute picker and hover reasons are generic. Task 7 checks the
  projection rows the browser renders.

## File map

| File | Responsibility |
|---|---|
| `gameplay/TurnOrder.scala` | `roundStart` reads the stored starter; new `nextRoundStart` |
| `model/GameState.scala` | `GameTracks.roundStarter` |
| `gameplay/setup/FirstGameSetupMaterializer.scala` | sets `roundStarter` at setup |
| `gameplay/StateBasedEvaluation.scala` | `RoundEnded` records the next starter |
| `model/CoreOperations.scala` | `UsurperLimiterRemoved` |
| `gameplay/operations/TurnStateOperations.scala` | its write; `setOathkeeper` side flip |
| `gameplay/operations/OperationApplication.scala` | applies it |
| `serialization/WalkerOperationCodec.scala` | its wire form |
| `gameplay/operations/GrandScepter.scala` | `GrandScepterSource.take`, moved out of `ChronicleCrown` |
| `gameplay/chronicle/ChronicleCrown.scala` | calls `GrandScepterSource.take` |
| `gameplay/roles/RoleChanges.scala` (new) | the operations of each change |
| `gameplay/roles/Citizenship.scala` (new) | the walker trees and the first-game reason |
| `model/Setup.scala` | `FirstGameSupportState.ignoreCitizenshipPowers` |
| `gameplay/powerresolver/PhasePower.scala` | `listed` hook |
| `gameplay/phases/PhaseMenu.scala` | filters by `listed` |
| `gameplay/powers/action/GrandScepterAction.scala` (new) | the Scepter's Action |
| `gameplay/powers/PhasePowerCatalog.scala` | registers it |
| `gameplay/actions/minor/SelfExile.scala` (new) | the minor action |
| `model/ProcedureRef.scala`, `gameplay/phases/ActOffers.scala`, `gameplay/walker/WalkerProcedureRegistry.scala`, `gameplay/actions/ProcedureNotes.scala`, `application/gamelog/ProcedureLog.scala` | wire self-exile in |
| `src/test/scala/oathdigital/testkit/Table.scala` | `imperial` sets the starter; new `allowingCitizenshipPowers` and `roundStarter` |

Paths without a prefix are under `src/main/scala/oathdigital/`.

## Checkpoints

Dispatch a reviewer after Task 3 (state and operations), after Task 6
(both sources), and a final whole-branch review after Task 8.

---

### Task 1: The stored round starter (R4)

**Files:**
- Modify: `model/GameState.scala` (`GameTracks`, line ~159)
- Modify: `gameplay/setup/FirstGameSetupMaterializer.scala:46`
- Modify: `src/test/scala/oathdigital/model/TestGameFixtures.scala:148`
- Modify: `gameplay/TurnOrder.scala`
- Modify: `gameplay/StateBasedEvaluation.scala` (`RoundEnded` case, ~145-165)
- Modify: `src/test/scala/oathdigital/testkit/Table.scala` (`imperial`, ~285)
- Test: `src/test/scala/oathdigital/gameplay/TurnOrderSuite.scala` (create
  it if it does not exist; otherwise add to it)

**Interfaces:**
- Produces:
  - `GameTracks(round: Int, visionsDrawn: Int, usurperLimited: Boolean, roundStarter: PlayerId)`
  - `TurnOrder.roundStart(ready: ReadyGame): PlayerId`, now the stored starter
  - `TurnOrder.nextRoundStart(ready: ReadyGame): PlayerId`
  - `Table.roundStarter(player: PlayerId): Table`

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.gameplay

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** The order of a round (CR p. 17) and a Chancellor who appears mid-round
  * (ruling R4). */
class TurnOrderSuite extends munit.FunSuite:
  test("a round runs from the player recorded when it began"):
    val ready = Table.start.roundStarter(p2).ready
    assertEquals(TurnOrder.order(ready), Vector(p2, p3, p1))

  test("a Chancellor who appears mid-round leaves the round's order alone"):
    // p1 began the round; p3 becomes the Chancellor during it.
    val ready = Table.start.imperial(p3).roundStarter(p1).ready
    assertEquals(TurnOrder.order(ready), Vector(p1, p2, p3))
    assertEquals(TurnOrder.nextRoundStart(ready), p3)

  test("with no Chancellor the next round starts with the first player"):
    val ready = Table.start.roundStarter(p2).ready
    assertEquals(TurnOrder.nextRoundStart(ready), ready.setup.firstPlayer)
```

Add to the existing round-end suite (`StateBasedEvaluationSuite`) a test
that the next round starts with a Chancellor who appeared mid-round. Use
the suite's existing helper that applies `RoundEnded` to a table at
`Phase.RoundEnd`; if it has none, apply
`StateBasedEvaluation.evolve(OathState.Ready(ready), RoundEnded(1, Some(2)))`
directly:

```scala
  test("the next round starts with a Chancellor who appeared mid-round"):
    // p1 began round 1; p3 became the Chancellor during it.
    val ready = Table.start.imperial(p3).roundStarter(p1)
      .turn(p1, Phase.RoundEnd).ready
    val after = Outcomes.ready(StateBasedEvaluation.evolve(
      OathState.Ready(ready), RoundEnded(1, Some(2))).toOption.get)
    assertEquals(after.game.current.tracks.roundStarter, p3)
    assertEquals(after.game.current.turn.activePlayer, p3)
    assertEquals(after.game.current.turn.phase, Phase.Wake)
```

Check the real name of the evolve entry point in `StateBasedEvaluation`
(`evolve` handles `RoundEnded` at ~line 145) and the import of `Outcomes`
from `oathdigital.testkit`.

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.TurnOrderSuite oathdigital.gameplay.StateBasedEvaluationSuite"`
Expected: compile failure, since `roundStarter` and `nextRoundStart` do not exist.

- [ ] **Step 3: Add the field**

In `model/GameState.scala`:

```scala
/** `roundStarter` is the player who began the round in progress. It is
  * recorded when the round begins, so a Chancellor who appears mid-round
  * leaves the round's order alone (ruling R4). */
final case class GameTracks(
    round: Int,
    visionsDrawn: Int,
    usurperLimited: Boolean,
    roundStarter: PlayerId
):
```

In `FirstGameSetupMaterializer.scala:46`:
`GameTracks(1, 0, usurperLimited = true, roundStarter = plan.firstPlayer)`.

In `TestGameFixtures.scala:148`, pass the fixture's first player as
`roundStarter` (the same `PlayerId` the fixture passes as `firstPlayer`).

- [ ] **Step 4: Read it in `TurnOrder`**

```scala
/** The order of a round (CR p. 17). A round runs from the player recorded
  * when it began (ruling R4); the next round begins with the Chancellor,
  * or with the first player when there is none. */
object TurnOrder:
  def roundStart(ready: ReadyGame): PlayerId =
    ready.game.current.tracks.roundStarter

  def nextRoundStart(ready: ReadyGame): PlayerId =
    Imperials.chancellor(ready).getOrElse(ready.setup.firstPlayer)

  def order(ready: ReadyGame): Vector[PlayerId] =
    val players = ready.game.current.players.map(_.player)
    val index = players.indexOf(roundStart(ready))
    players.drop(index) ++ players.take(index)
```

- [ ] **Step 5: Record the next starter at `RoundEnded`**

In the `RoundEnded` case of `StateBasedEvaluation.evolve`, compute
`val next = recorded.nextRound.fold(first)(_ => TurnOrder.nextRoundStart(ready))`
and write it:

```scala
          else update(state)(current => current.copy(
            tracks = current.tracks.copy(
              round = recorded.nextRound.getOrElse(round),
              usurperLimited = current.tracks.usurperLimited &&
                recorded.completedRound < 3,
              roundStarter = next),
            turn = current.turn.copy(activePlayer = next,
              phase = recorded.nextRound.fold[Phase](
                Phase.WarExhaustion)(_ => Phase.Wake))))
```

After round 8 `next` is `first`, so War Exhaustion still runs from the
last round's starter, as today.

- [ ] **Step 6: Seat the starter in `Table`**

`Table.imperial` seats a Chancellor as if from the round's start, so it
also sets the starter. Append to its body:

```scala
    }.update(ready => ready.copy(banks = ready.banks.copy(warbandSupply =
      ready.banks.warbandSupply.updated(ForceKind.Imperial,
        MaterialBankState.ImperialWarbands)))).roundStarter(chancellor)
```

Update its doc: "The round runs from the Chancellor, as one that began
with them."

Add the step:

```scala
  /** The round in progress began with `player`. */
  def roundStarter(player: PlayerId): Table =
    update(_.updateCurrent(current => current.copy(tracks =
      current.tracks.copy(roundStarter = player))))
```

- [ ] **Step 7: Run the suites, then the full backend tests**

Run: `./sbtw "testOnly oathdigital.gameplay.TurnOrderSuite oathdigital.gameplay.StateBasedEvaluationSuite"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS. A test that builds a Chancellor without `Table.imperial`
and expects turn order from them fails here; give it `.roundStarter(...)`.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/model/GameState.scala \
  src/main/scala/oathdigital/gameplay/setup/FirstGameSetupMaterializer.scala \
  src/main/scala/oathdigital/gameplay/TurnOrder.scala \
  src/main/scala/oathdigital/gameplay/StateBasedEvaluation.scala \
  src/test/scala/oathdigital/model/TestGameFixtures.scala \
  src/test/scala/oathdigital/testkit/Table.scala \
  src/test/scala/oathdigital/gameplay/TurnOrderSuite.scala \
  src/test/scala/oathdigital/gameplay/StateBasedEvaluationSuite.scala
git commit -m "feat(empire): a round runs from its recorded starter (R4)"
```

---

### Task 2: Two operation changes

**Files:**
- Modify: `model/CoreOperations.scala` (beside `AdvanceVisionsDrawn`, ~634)
- Modify: `gameplay/operations/TurnStateOperations.scala` (`setOathkeeper`, ~179)
- Modify: `gameplay/operations/OperationApplication.scala` (beside `AdvanceVisionsDrawn`, ~194)
- Modify: `serialization/WalkerOperationCodec.scala` (`TurnOperation` type
  at :41, encoder at :232, decoder at :578)
- Test: `src/test/scala/oathdigital/gameplay/operations/TurnStateOperationsSuite.scala`
  (create it if missing), `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces:
  - `case object UsurperLimiterRemoved extends PrimitiveOperation`
  - `SetOathkeeper(Some(holder))` with an unchanged holder succeeds when
    the title is on its Usurper side, and turns it to the Oathkeeper side.

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The title's side and the Usurper Limiter (Empire slice 3a). */
class TurnStateOperationsSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    executor.executeAll(ready, ops.toVector)

  test("an unchanged holder's Usurper title turns to its Oathkeeper side"):
    val ready = Table.start.oathkeeper(Some(p1), TitleSide.Usurper).ready
    val after = accepted(run(ready, SetOathkeeper(Some(p1))))
    assertEquals(after.game.current.title,
      OathkeeperState(Some(p1), TitleSide.Oathkeeper))

  test("an unchanged holder on the Oathkeeper side is still refused"):
    val ready = Table.start.oathkeeper(Some(p1)).ready
    rejected(run(ready, SetOathkeeper(Some(p1))))

  test("removing the Usurper Limiter ends its limit"):
    val after = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    assert(!after.game.current.tracks.usurperLimited)

  test("the Usurper Limiter cannot be removed twice"):
    val off = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    rejected(run(off, UsurperLimiterRemoved))
```

In `WalkerOperationWireSuite`, add `UsurperLimiterRemoved,` to the list of
operations that round-trip, after `ImperialWarbandsTaken(8),` (~line 432).

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.TurnStateOperationsSuite oathdigital.serialization.WalkerOperationWireSuite"`
Expected: compile failure on `UsurperLimiterRemoved`.

- [ ] **Step 3: Add the operation**

In `model/CoreOperations.scala`, after `AdvanceVisionsDrawn`:

```scala
/** Takes the Usurper Limiter off the round track. It is removed at the end
  * of round 3, or at once when an Empire starts in play (ruling R5). */
case object UsurperLimiterRemoved extends PrimitiveOperation
```

In `TurnStateOperations`:

```scala
  def usurperLimiterRemoved(ready: ReadyGame)
      : Either[OperationError, ReadyGame] =
    Either.cond(ready.game.current.tracks.usurperLimited,
      ready.updateCurrent(current => current.copy(tracks =
        current.tracks.copy(usurperLimited = false))),
      InvalidDescription("the Usurper Limiter is already off the round track"))
```

Use the `InvalidDescription` error `ChronicleStateOperations` uses; import
it the same way.

Relax `setOathkeeper`:

```scala
  /** Gives the title to `holder` on its Oathkeeper side. An unchanged
    * holder is refused, unless the title is on its Usurper side: becoming a
    * Citizen turns it back (CR p. 34). */
  def setOathkeeper(ready: ReadyGame,
      holder: Option[PlayerId]): Either[OperationError, ReadyGame] =
    val title = ready.game.current.title
    Either.cond(title.holder != holder || title.side == TitleSide.Usurper,
      ready.updateCurrent(current => current.copy(
        title = OathkeeperState(holder, TitleSide.Oathkeeper))),
      OathkeeperUnchanged(holder))
```

In `OperationApplication.applyNonMoveLeaves`, after the `AdvanceVisionsDrawn`
case:

```scala
      case (result, UsurperLimiterRemoved) =>
        result.flatMap(TurnStateOperations.usurperLimiterRemoved)
```

If the earlier fold at ~173 (the violations fold) must list every
primitive, add the operation there too: `case ((result, state), UsurperLimiterRemoved) => (result, state)`
is already covered by its catch-all, so no change is needed there.

- [ ] **Step 4: Add the wire form**

In `WalkerOperationCodec.scala`:
- add `| UsurperLimiterRemoved.type` to `TurnOperation` (line 41);
- in the encoder, beside `case AdvanceVisionsDrawn => ...` (line 232):
  `case UsurperLimiterRemoved => ujson.Obj("kind" -> "usurper-limiter-removed")`;
- in the decoder, beside `case "advance-visions-drawn" => ...` (line 578):
  `case "usurper-limiter-removed" => Right(UsurperLimiterRemoved)`.

- [ ] **Step 5: Run the suites, then the full backend tests**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.TurnStateOperationsSuite oathdigital.serialization.WalkerOperationWireSuite"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS. If a test relied on `OathkeeperUnchanged` for a Usurper
title, read it: refusal of an unchanged Usurper holder was never a rule,
so change the test's expectation.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/model/CoreOperations.scala \
  src/main/scala/oathdigital/gameplay/operations/TurnStateOperations.scala \
  src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala \
  src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala \
  src/test/scala/oathdigital/gameplay/operations/TurnStateOperationsSuite.scala \
  src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(empire): remove the Usurper Limiter and turn a Usurper title back"
```

---

### Task 3: The operations of a role change

**Files:**
- Modify: `gameplay/operations/GrandScepter.scala` (add `GrandScepterSource`)
- Modify: `gameplay/chronicle/ChronicleCrown.scala` (use it; delete its
  private `scepter`)
- Create: `gameplay/roles/RoleChanges.scala`
- Test: `src/test/scala/oathdigital/gameplay/roles/RoleChangesSuite.scala`

**Interfaces:**
- Consumes: `UsurperLimiterRemoved`, the relaxed `SetOathkeeper` (Task 2).
- Produces:
  - `GrandScepterSource.take(ready: ReadyGame, player: PlayerId): Vector[CoreOperation]`
  - `RoleChanges.Holdings(board: Int, sites: Vector[(SiteId, Int)])` with `total: Int`
  - `RoleChanges.Kept(board: Int, sites: Map[SiteId, Int])`
  - `RoleChanges.holdings(ready: ReadyGame, player: PlayerState): Holdings`
  - `RoleChanges.purpleAvailable(ready: ReadyGame): Int`
  - `RoleChanges.newRole(ready: ReadyGame): Role`
  - `RoleChanges.becomeImperial(ready: ReadyGame, player: PlayerId, kept: Kept): Either[OathViolation, Vector[CoreOperation]]`
  - `RoleChanges.keepAll(ready: ReadyGame, player: PlayerId): Either[OathViolation, Kept]`
  - `RoleChanges.exile(ready: ReadyGame, player: PlayerId): Either[OathViolation, Vector[CoreOperation]]`

- [ ] **Step 1: Move the Scepter's source**

Move the private `scepter` function from `ChronicleCrown` to
`GrandScepter.scala`, unchanged except for its name and doc:

```scala
/** Where a new Chancellor takes the Grand Scepter from: its holder, the
  * site it lies at, or faceup from the box when it is in neither (Chronicle
  * ruling 10, extended to every source by the Empire phase design, "Grand
  * Scepter source"). Nothing when `player` holds it already. */
object GrandScepterSource:
  def take(ready: ReadyGame, player: PlayerId): Vector[CoreOperation] =
    val id = TheGrandScepterCard.id
    val current = ready.game.current
    val held = current.players.find(_.relics.exists(_.id == id))
      .map(state => Location.PlayArea(state.player))
    val atSite = current.map.sites.collectFirst {
      case (site, state) if state.relics.exists(_.id == id) =>
        Location.Site(site)
    }
    held.orElse(atSite) match
      case Some(Location.PlayArea(`player`)) => Vector.empty
      case Some(from) => Vector(Move(Piece.Card(id), PositionedLocation(from),
        PositionedLocation(Location.PlayArea(player)),
        resultingOrientation = Some(Orientation.FaceUp)))
      case None => Vector(GrandScepterFromBox(player))
```

In `ChronicleCrown.operations`, replace `scepter(ready, winner)` with
`GrandScepterSource.take(ready, winner)` and change the import to
`import oathdigital.gameplay.operations.GrandScepterSource`. Run
`./sbtw "testOnly oathdigital.gameplay.chronicle.*"`; expected PASS.

- [ ] **Step 2: Write the failing tests**

```scala
package oathdigital.gameplay.roles

import oathdigital.gameplay.operations.{OperationExecutor, TheGrandScepterCard}
import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Look, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Becoming a Citizen or the Chancellor, and exile (CR p. 34; Empire phase
  * design, "Slice 3"). */
class RoleChangesSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def applied(ready: ReadyGame,
      ops: Either[OathViolation, Vector[CoreOperation]])(
      using munit.Location): ReadyGame =
    accepted(executor.executeAll(ready, present(ops.toOption, "the ops")))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageId =
    Look(ready).player(player).lineage

  private def everyPlaceKept(ready: ReadyGame, player: PlayerId)(
      using munit.Location): RoleChanges.Kept =
    present(RoleChanges.keepAll(ready, player).toOption, "the kept counts")

  /** p1 is the Chancellor and p3 an Exile in their Act, with 2 warbands
    * on their board and 3 at Broken Peaks, a revealed Vision and Supply 2. */
  private def exileInAct: Table = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = "Broken Peaks")
    .boardWarbands(p3, 2).warbandsAt("Broken Peaks", p3, 3)
    .revealedVision(p3, CatalogNames.vision("Conquest")).supply(p3, 2)

  test("becoming a Citizen turns every warband purple and ends the Act"):
    val ready = exileInAct.ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    val look = Look(after)
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(look.player(p3).board.warbands, 2)
    assertEquals(look.forces(CatalogNames.site("Broken Peaks")),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(look.player(p3).revealedVision, None)
    assertEquals(look.player(p3).board.supply.supply, 7)
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("off their turn the Act phase is left alone"):
    val ready = exileInAct.turn(p2, Phase.Act).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.turn,
      ready.game.current.turn)

  test("a short purple bank keeps only the counts chosen"):
    // The bank holds 2: p1's board takes the other 22.
    val ready = exileInAct.boardWarbands(p1, 22).ready
    assertEquals(RoleChanges.purpleAvailable(ready), 2)
    val kept = RoleChanges.Kept(board = 0,
      sites = Map(CatalogNames.site("Broken Peaks") -> 2))
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3, kept))
    val look = Look(after)
    assertEquals(look.player(p3).board.warbands, 0)
    assertEquals(look.forces(CatalogNames.site("Broken Peaks")),
      SiteForces.Occupied(ForceKind.Imperial, 2))

  test("kept counts beyond the bank are refused"):
    val ready = exileInAct.boardWarbands(p1, 22).ready
    assert(RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)).isLeft)

  test("a Usurper title turns to its Oathkeeper side"):
    val ready = exileInAct.oathkeeperGoal(OathkeeperGoal.ThePeople)
      .oathkeeper(Some(p3), TitleSide.Usurper).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.title,
      OathkeeperState(Some(p3), TitleSide.Oathkeeper))

  test("under Supremacy the title passes to the Chancellor (R19)"):
    val ready = exileInAct.oathkeeperGoal(OathkeeperGoal.Supremacy)
      .oathkeeper(Some(p3)).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.title.holder, Some(p1))

  /** No Empire: p3, an Exile in their Act, with 2 warbands on their board.
    * p2 holds the Grand Scepter. */
  private def noEmpire: Table = Table.start.turn(p3, Phase.Act)
    .pawn(p3, at = "Broken Peaks").boardWarbands(p3, 2)
    .relic(p2, "The Grand Scepter")

  test("with no Empire the player becomes the Chancellor"):
    val ready = noEmpire.ready
    assertEquals(RoleChanges.newRole(ready), Role.Chancellor)
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(Imperials.chancellor(after), Some(p3))
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial),
      Some(MaterialBankState.ImperialWarbands))
    assert(Look(after).player(p3).relics.exists(
      _.id == TheGrandScepterCard.id))
    assert(!after.game.current.tracks.usurperLimited)

  test("with no one holding the Scepter it comes from the box"):
    val ready = Table.start.turn(p3, Phase.Act)
      .pawn(p3, at = "Broken Peaks").ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assert(Look(after).player(p3).relics.exists(
      _.id == TheGrandScepterCard.id))

  test("only an Exile becomes Imperial"):
    val ready = exileInAct.imperial(p1, p3).ready
    assert(RoleChanges.becomeImperial(ready, p3,
      RoleChanges.Kept(0, Map.empty)).isLeft)

  /** p1 is the Chancellor and p3 a Citizen in their Act, with 4 purple
    * warbands on their board and 2 at Broken Peaks, and Supply 1. */
  private def citizenInAct: Table = Table.start.imperial(p1, p3)
    .turn(p3, Phase.Act).pawn(p3, at = "Broken Peaks")
    .boardWarbands(p3, 4).warbandsAt("Broken Peaks", p3, 2).supply(p3, 1)

  test("exile swaps the board's purple warbands and leaves the map's"):
    val ready = citizenInAct.ready
    val after = applied(ready, RoleChanges.exile(ready, p3))
    val look = Look(after)
    assertEquals(Imperials.role(after, p3), Some(Role.Exile))
    assertEquals(look.player(p3).board.warbands, 4)
    assertEquals(look.forces(CatalogNames.site("Broken Peaks")),
      SiteForces.Occupied(ForceKind.Imperial, 2))
    assertEquals(look.player(p3).board.supply.supply, 7)
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("exile takes no more of their own warbands than their bank holds"):
    // 12 of p3's 14 own warbands stand at Ancient City.
    val ready = citizenInAct.update(_.updateCurrent(current => current.copy(
      map = current.map.copy(sites = current.map.sites.updatedWith(
        CatalogNames.site("Ancient City"))(_.map(_.copy(forces =
        SiteForces.Occupied(ForceKind.Exile(lineage(citizenInAct.ready, p3)),
          12)))))))).ready
    val after = applied(ready, RoleChanges.exile(ready, p3))
    assertEquals(Look(after).player(p3).board.warbands, 2)

  test("only a Citizen is exiled"):
    val ready = citizenInAct.ready
    assert(RoleChanges.exile(ready, p1).isLeft)
    assert(RoleChanges.exile(exileInAct.ready, p3).isLeft)
```

Before running, confirm these testkit names and adjust the test (not the
production code) if one differs:
- `Look.forces(site)`, `Look.player(p).relics`;
- `CatalogNames.vision("Conquest")` and `Table.revealedVision`;
- `Table.supply`, `Table.relic(player, name)`.

`Table.warbandsAt` must run after `imperial` for a Citizen, since it reads
the owner's role. Ancient City may hold bandits in `Table.start`; if it
does, pick an empty in-play site.

- [ ] **Step 3: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.RoleChangesSuite"`
Expected: compile failure, since `RoleChanges` does not exist.

- [ ] **Step 4: Write `RoleChanges`**

```scala
package oathdigital.gameplay.roles

import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.operations.GrandScepterSource
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._
import oathdigital.model.OathViolation.InvalidEventOrder

/** A role change in play as operations (CR p. 34; Empire phase design,
  * "Slice 3"): becoming a Citizen, becoming the Chancellor in a game with
  * no Empire, and exile. Each change is one batch, ordered so that every
  * step leaves a valid state: a board's warbands follow its owner's role,
  * so the board is emptied before the role changes and refilled after. */
object RoleChanges:
  /** A player's own warbands: on their board, and at each site. */
  final case class Holdings(board: Int, sites: Vector[(SiteId, Int)]):
    def total: Int = board + sites.map(_._2).sum

  /** How many of them turn purple at each place (ruling R18). The rest
    * return to the player's own bank. */
  final case class Kept(board: Int, sites: Map[SiteId, Int])

  def holdings(ready: ReadyGame, player: PlayerState): Holdings =
    val own = ForceKind.Exile(player.lineage)
    val current = ready.game.current
    Holdings(player.board.warbands, current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(_.forces).collect {
        case SiteForces.Occupied(`own`, count) => site -> count }))

  /** The purple warbands a player can take: the Imperial bank's, or all
    * of them when this change creates the bank. */
  def purpleAvailable(ready: ReadyGame): Int =
    if ready.banks.warbandSupply.contains(ForceKind.Imperial) then
      PlayerFacts.banked(ready, ForceKind.Imperial)
    else MaterialBankState.ImperialWarbands

  /** A "become a Citizen" power makes the player the Chancellor in a game
    * with no Empire (CR p. 34). */
  def newRole(ready: ReadyGame): Role =
    if Imperials.chancellor(ready).isEmpty then Role.Chancellor
    else Role.Citizen

  /** Every warband kept, when the bank has room for them all. */
  def keepAll(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Kept] =
    PlayerFacts.player(ready, player).map { state =>
      val held = holdings(ready, state)
      Kept(held.board, held.sites.toMap)
    }

  def becomeImperial(ready: ReadyGame, player: PlayerId, kept: Kept)
      : Either[OathViolation, Vector[CoreOperation]] = for
    state <- PlayerFacts.player(ready, player)
    _ <- Either.cond(Imperials.role(ready, player).contains(Role.Exile), (),
      InvalidEventOrder(s"${player.value} is not an Exile"))
    held = holdings(ready, state)
    _ <- Either.cond(fits(held, kept, purpleAvailable(ready)), (),
      InvalidEventOrder(s"${player.value} cannot keep those warbands"))
  yield
    val role = newRole(ready)
    val own = ForceKind.Exile(state.lineage)
    val chancellor = role == Role.Chancellor
    val bank = Option.when(
      !ready.banks.warbandSupply.contains(ForceKind.Imperial))(
      ImperialWarbandsTaken(MaterialBankState.ImperialWarbands))
    val sites = held.sites.flatMap((site, count) =>
      val keep = kept.sites.getOrElse(site, 0)
      val at = PositionedLocation(Location.Site(site))
      Option.when(count > keep)(Move(Piece.Warbands(own, count - keep), at,
        PositionedLocation(Location.WarbandBank(own)))).toVector ++
        Option.when(keep > 0)(Replace(Piece.Warbands(own, keep),
          Piece.Warbands(ForceKind.Imperial, keep), at)).toVector)
    val board = Option.when(held.board > 0)(Move(
      Piece.Warbands(own, held.board),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.WarbandBank(own))))
    val gained = Option.when(kept.board > 0)(
      Gain.Warbands(player, ForceKind.Imperial, kept.board))
    val scepter =
      if chancellor then GrandScepterSource.take(ready, player)
      else Vector.empty
    val limiter = Option.when(chancellor &&
      ready.game.current.tracks.usurperLimited)(UsurperLimiterRemoved)
    bank.toVector ++ title(ready, player, role) ++ vision(ready, state) ++
      sites ++ board.toVector ++
      Vector(RoleChanged(state.lineage, role)) ++ gained.toVector ++
      scepter ++ limiter.toVector ++ finish(ready, state)

  def exile(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    state <- PlayerFacts.player(ready, player)
    _ <- Either.cond(Imperials.role(ready, player).contains(Role.Citizen),
      (), InvalidEventOrder(s"${player.value} is not a Citizen"))
  yield
    val own = ForceKind.Exile(state.lineage)
    val purple = state.board.warbands
    // Read before the change: the board still counts as purple, so the
    // own bank's count leaves it out.
    val gained = math.min(purple, PlayerFacts.banked(ready, own))
    Option.when(purple > 0)(Move(Piece.Warbands(ForceKind.Imperial, purple),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.WarbandBank(ForceKind.Imperial))))
      .toVector ++ Vector(RoleChanged(state.lineage, Role.Exile)) ++
      Option.when(gained > 0)(Gain.Warbands(player, own, gained)).toVector ++
      finish(ready, state)

  private def fits(held: Holdings, kept: Kept, available: Int): Boolean =
    val places = kept.sites.keySet.subsetOf(held.sites.map(_._1).toSet)
    val within = kept.board >= 0 && kept.board <= held.board &&
      held.sites.forall((site, count) =>
        kept.sites.get(site).forall(n => n >= 0 && n <= count))
    val total = kept.board + kept.sites.values.sum
    places && within && total == math.min(held.total, available)

  /** CR p. 34: a Usurper title turns to its Oathkeeper side. Under
    * Supremacy it passes to the Chancellor, on either side (R19). */
  private def title(ready: ReadyGame, player: PlayerId, role: Role)
      : Vector[CoreOperation] =
    val current = ready.game.current.title
    if !current.holder.contains(player) then Vector.empty
    else
      val supremacy =
        ready.game.campaign.oathkeeperGoal == OathkeeperGoal.Supremacy
      val heir =
        if role == Role.Chancellor then Some(player)
        else Imperials.chancellor(ready)
      if supremacy && heir != Some(player) then Vector(SetOathkeeper(heir))
      else if current.side == TitleSide.Usurper then
        Vector(SetOathkeeper(Some(player)))
      else Vector.empty

  /** The revealed Vision goes to the discard pile after the player's
    * pawn's region, as a played card's discard does. */
  private def vision(ready: ReadyGame, state: PlayerState)
      : Vector[CoreOperation] = (for
    held <- state.revealedVision
    region <- CardPlay.discardRegion(ready, state.player)
  yield Discard.Vision(held.id,
    PositionedLocation(Location.PlayArea(state.player)), region)).toVector

  /** End their Act phase if it is their turn, then refresh their Supply to
    * its leftmost space. */
  private def finish(ready: ReadyGame, state: PlayerState)
      : Vector[CoreOperation] =
    val turn = ready.game.current.turn
    val ending = Option.when(turn.activePlayer == state.player &&
      turn.phase == Phase.Act)(EnterPhase(Phase.Rest))
    val missing = SupplyTrack.Maximum - state.board.supply.supply
    ending.toVector ++
      Option.when(missing > 0)(GainSupply(state.player, missing)).toVector
```

If `Location.PlayArea(player)` is not where `Discard.Vision` finds a
revealed Vision, use the location `Inquisitor` passes
(`gameplay/powers/action/Inquisitor.scala:132`, also `PlayArea(owner)`).
If `Replace` takes its location as a plain `Location`, pass
`Location.Site(site)` instead; `ChronicleCrown` shows the type it takes.

- [ ] **Step 5: Run the suite until it passes**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.RoleChangesSuite"`
Expected: PASS. If the executor refuses a batch, the refusal names the
step: check the batch order against "Order of steps" above, never by
splitting the change into two batches.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/operations/GrandScepter.scala \
  src/main/scala/oathdigital/gameplay/chronicle/ChronicleCrown.scala \
  src/main/scala/oathdigital/gameplay/roles/RoleChanges.scala \
  src/test/scala/oathdigital/gameplay/roles/RoleChangesSuite.scala
git commit -m "feat(empire): the operations of becoming a Citizen, the Chancellor, or an Exile"
```

**Checkpoint review** after this task: Tasks 1-3 as one diff.

---

### Task 4: Citizenship trees and the first-game option

**Files:**
- Modify: `model/Setup.scala` (`FirstGameSupportState`)
- Create: `gameplay/roles/Citizenship.scala`
- Modify: `src/test/scala/oathdigital/testkit/Table.scala` (new step)
- Test: `src/test/scala/oathdigital/gameplay/roles/CitizenshipSuite.scala`

**Interfaces:**
- Consumes: `RoleChanges` (Task 3).
- Produces:
  - `FirstGameSupportState(foundationProfile, firstPlayer, ignoreCitizenshipPowers: Boolean = true)`
  - `Citizenship.replaceDecisionId: String` = `"role.replace-warbands"`
  - `Citizenship.ignoredReason: String` = `"Citizenship is ignored due to setup option"`
  - `Citizenship.ignored(ready: ReadyGame): Boolean`
  - `Citizenship.become(actor: PlayerId): Operation`
  - `Citizenship.exile(actor: PlayerId): Operation`
  - `Table.allowingCitizenshipPowers: Table`

- [ ] **Step 1: Write the failing tests**

The tree is driven through a power in Tasks 5-7. This suite checks the
query the tree asks and what an answer does, through the walker harness
the Scepter suite will use. Read
`src/test/scala/oathdigital/testkit/PhasePowerHarness.scala` first. If it
runs an arbitrary `Operation` tree, use it. If not, test the two private
pieces through package-private access instead:
- the query, `Citizenship.query(ready, actor): Option[DecisionQuery]`;
- the kept counts, `Citizenship.kept(ready, actor, rows): Either[OathViolation, RoleChanges.Kept]`.

```scala
package oathdigital.gameplay.roles

import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Table}
import oathdigital.testkit.Table.{p1, p3}

/** The question a short purple bank asks (ruling R18) and the first-game
  * option (ambiguity F8). */
class CitizenshipSuite extends munit.FunSuite:
  private val peaks = CatalogNames.site("Broken Peaks")

  /** p1 is the Chancellor; p3 an Exile with 2 board warbands and 3 at
    * Broken Peaks. `purple` is what the purple bank holds. */
  private def exileWith(purple: Int): ReadyGame = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = "Broken Peaks")
    .boardWarbands(p3, 2).warbandsAt("Broken Peaks", p3, 3)
    .boardWarbands(p1, MaterialBankState.ImperialWarbands - purple).ready

  test("a bank with room for every warband asks nothing"):
    assertEquals(Citizenship.query(exileWith(5), p3), None)

  test("a short bank asks for a split over the board and each site"):
    val query = Citizenship.query(exileWith(4), p3)
    assertEquals(query, Some(DecisionQuery.Distribute.exactly(Vector(
      DistributeSlot(DecisionOptionRef.Player(p3), 0, 2, None),
      DistributeSlot(DecisionOptionRef.Site(peaks), 0, 3, None)), 4,
      Some("Too few purple warbands: choose where yours turn purple"),
      "Replace warbands")))

  test("the split's rows become the kept counts"):
    val rows = Vector(DistributeAmount(DecisionOptionRef.Player(p3), 1),
      DistributeAmount(DecisionOptionRef.Site(peaks), 3))
    assertEquals(Citizenship.kept(exileWith(4), p3, rows),
      Right(RoleChanges.Kept(1, Map(peaks -> 3))))

  test("the first-game option is on unless a table turns it off"):
    assert(Citizenship.ignored(Table.start.ready))
    assert(!Citizenship.ignored(Table.start.allowingCitizenshipPowers.ready))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.CitizenshipSuite"`
Expected: compile failure.

- [ ] **Step 3: Add the option**

In `model/Setup.scala`:

```scala
/** `ignoreCitizenshipPowers` is the first-game option that ignores
  * "become a Citizen" powers (CR p. 34, ambiguity F8). It is on by default,
  * and game start never turns it off; the Settings Feature phase will let
  * players change it. */
final case class FirstGameSupportState(
    foundationProfile: FirstGameFoundationProfile,
    firstPlayer: PlayerId,
    ignoreCitizenshipPowers: Boolean = true
)
```

In `Table.scala`, beside `imperial`:

```scala
  /** The first-game option that ignores "become a Citizen" powers is off. */
  def allowingCitizenshipPowers: Table = update(ready => ready.copy(setup =
    ready.setup.copy(ignoreCitizenshipPowers = false)))
```

- [ ] **Step 4: Write `Citizenship`**

```scala
package oathdigital.gameplay.roles

import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._
import oathdigital.model.OathViolation.InvalidEventOrder

/** Role changes as walker trees, shared by every source (Empire phase
  * design, "Slice 3"). Becoming Imperial asks the player which warbands
  * turn purple only when the purple bank is short (ruling R18). */
object Citizenship:
  val replaceDecisionId: String = "role.replace-warbands"
  val ignoredReason: String = "Citizenship is ignored due to setup option"

  /** Whether the first-game option ignores "become a Citizen" powers. */
  def ignored(ready: ReadyGame): Boolean =
    ready.setup.ignoreCitizenshipPowers

  /** `actor` becomes a Citizen, or the Chancellor in a game with no
    * Empire. */
  def become(actor: PlayerId): Operation = Sequence(Vector(
    Branch((live, _) => query(live, actor).toVector.map(asked =>
      Decide(replaceDecisionId, actor, asked))),
    BuildOps((live, pending) => for
      kept <- answered(live, actor, pending)
      ops <- RoleChanges.becomeImperial(live, actor, kept)
    yield ops, required = true)))

  /** `actor`, a Citizen, becomes an Exile. */
  def exile(actor: PlayerId): Operation =
    BuildOps((live, _) => RoleChanges.exile(live, actor), required = true)

  private[roles] def query(ready: ReadyGame, actor: PlayerId)
      : Option[DecisionQuery] = for
    state <- PlayerFacts.player(ready, actor).toOption
    held = RoleChanges.holdings(ready, state)
    available = RoleChanges.purpleAvailable(ready)
    if held.total > available
  yield DecisionQuery.Distribute.exactly(
    DistributeSlot(DecisionOptionRef.Player(actor), 0, held.board, None) +:
      held.sites.map((site, count) =>
        DistributeSlot(DecisionOptionRef.Site(site), 0, count, None)),
    available,
    Some("Too few purple warbands: choose where yours turn purple"),
    "Replace warbands")

  private[roles] def kept(ready: ReadyGame, actor: PlayerId,
      rows: Vector[DistributeAmount]): Either[OathViolation, RoleChanges.Kept] =
    val board = rows.collect {
      case DistributeAmount(DecisionOptionRef.Player(`actor`), n) => n }
    val sites = rows.collect {
      case DistributeAmount(DecisionOptionRef.Site(site), n) => site -> n }
    Either.cond(board.size == 1 && board.size + sites.size == rows.size,
      RoleChanges.Kept(board.head, sites.toMap),
      InvalidEventOrder(s"$replaceDecisionId names an unknown place"))

  /** Every warband kept when nothing was asked; otherwise the split. */
  private def answered(ready: ReadyGame, actor: PlayerId,
      pending: PendingTree): Either[OathViolation, RoleChanges.Kept] =
    if query(ready, actor).isEmpty then RoleChanges.keepAll(ready, actor)
    else PowerAnswers.distribution(pending, replaceDecisionId)
      .toRight(PowerAnswers.missing(replaceDecisionId))
      .flatMap(kept(ready, actor, _))
```

Check `PowerAnswers.distribution`'s return type
(`gameplay/powers/PowerAnswers.scala:47`). If it returns the rows rather
than an `Option`, drop the `.toRight`. `RoleChanges.becomeImperial`
refuses a split that breaks the slots, so the walker's own Distribute check
and this one agree.

- [ ] **Step 5: Run it until it passes**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.CitizenshipSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/model/Setup.scala \
  src/main/scala/oathdigital/gameplay/roles/Citizenship.scala \
  src/test/scala/oathdigital/testkit/Table.scala \
  src/test/scala/oathdigital/gameplay/roles/CitizenshipSuite.scala
git commit -m "feat(empire): Citizenship trees and the first-game option"
```

---

### Task 5: The Grand Scepter's Action

**Files:**
- Modify: `gameplay/powerresolver/PhasePower.scala` (add `listed`)
- Modify: `gameplay/phases/PhaseMenu.scala` (the `sourced` for, ~126)
- Create: `gameplay/powers/action/GrandScepterAction.scala`
- Modify: `gameplay/powers/PhasePowerCatalog.scala` (register it)
- Modify: `src/test/resources/powers/unimplemented.txt` (delete
  `relic.the-grand-scepter.action`)
- Test: `src/test/scala/oathdigital/gameplay/powers/action/GrandScepterActionSuite.scala`

**Interfaces:**
- Consumes: `Citizenship.become`, `Citizenship.ignored`,
  `Citizenship.ignoredReason`, `Citizenship.replaceDecisionId` (Task 4).
- Produces:
  - `PhasePower.listed(ready: ReadyGame, player: PlayerId): Boolean`, default `true`
  - `GrandScepterAction` (a `PaidAction`), with `becameCitizen` and `becameChancellor` note keys

- [ ] **Step 1: Write the failing tests**

Follow `WhistleSuite`'s harness (`PowerFixture.use`, `choose`,
`PhasePowerHarness.{awaits, replayed}`, `ActPowerHarness.usableIds`).
Read `WhistleSuite` and `PowerFixture` for exact signatures before writing.

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.PowerFixture
import oathdigital.model._
import oathdigital.testkit.{PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.ready
import oathdigital.testkit.Table.{p1, p3}
import oathdigital.testkit.Unwrap.accepted

/** The Grand Scepter's Action: "You may become a Citizen if an Exile"
  * (CR p. 34; Empire slice 3a). */
class GrandScepterActionSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, replayed}

  private val scepter = TheGrandScepterCard.id

  /** p1 is the Chancellor; p3, an Exile in their Act, holds the Scepter. */
  private def exileHolder: Table = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = "Broken Peaks")
    .relic(p3, "The Grand Scepter").boardWarbands(p3, 2)
    .allowingCitizenshipPowers

  test("an Exile holder becomes a Citizen and their Act ends"):
    val start = exileHolder.ready
    val done = accepted(use(start, GrandScepterAction.id, scepter, p3))
    val after = ready(done.state)
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(after.game.current.turn.phase, Phase.Rest)
    assertEquals(replayed(start, done.events), Right(done.state))

  test("with no Empire the holder becomes the Chancellor"):
    val start = Table.start.turn(p3, Phase.Act)
      .pawn(p3, at = "Broken Peaks").relic(p3, "The Grand Scepter")
      .allowingCitizenshipPowers.ready
    val after = ready(accepted(use(start, GrandScepterAction.id, scepter,
      p3)).state)
    assertEquals(Imperials.chancellor(after), Some(p3))

  test("a short purple bank asks which warbands turn purple"):
    val start = exileHolder.boardWarbands(p1, 23).ready
    val parked = accepted(use(start, GrandScepterAction.id, scepter, p3))
    assert(awaits(parked, Citizenship.replaceDecisionId))

  test("the first-game option shows the Action disabled with its reason"):
    val start = Table.from(exileHolder.ready).update(ready => ready.copy(
      setup = ready.setup.copy(ignoreCitizenshipPowers = true))).ready
    assertEquals(menuReason(start, p3, GrandScepterAction.id),
      Some(Citizenship.ignoredReason))

  test("an Imperial holder's menu leaves the Action out"):
    val start = Table.start.imperial(p1).turn(p1, Phase.Act)
      .relic(p1, "The Grand Scepter").allowingCitizenshipPowers.ready
    assert(!menuLists(start, p1, GrandScepterAction.id))
```

`use(state, powerId, relicId, player)` stands for `PowerFixture`'s own
start helper; use its real arity. For `menuReason` and `menuLists`,
reuse the helpers `PhaseMenuSuite` uses to read an option's state and
reason. If there are none, add them to this suite as private helpers over
`PhaseMenu.offers`. They should find the option whose ref is
`DecisionOptionRef.Relic(scepter)` in the Act menu's `Power` group.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GrandScepterActionSuite"`
Expected: compile failure.

- [ ] **Step 3: Add the `listed` hook**

In `PhasePower`, after `refusal`:

```scala
  /** Whether `player`'s menus list the power at all. A power their role
    * can never use is left out, as an Exile's menu leaves out Warband
    * Transfers. `true` by default. */
  def listed(ready: ReadyGame, player: PlayerId): Boolean = true
```

In `PhaseMenu.offers`, extend the guard of the `sourced` for:

```scala
      if !power.forced && power.listed(ready, actor) &&
        PhasePowerProcedure.timingOf(menu.phase).contains(power.timing)
```

- [ ] **Step 4: Write the power**

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powerresolver.QueuedStarts
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._

/** The Grand Scepter's ACTION: "You may become a Citizen if an Exile"
  * (CR p. 34). In a game with no Empire the holder becomes the Chancellor
  * instead. An Imperial holder's menu leaves it out; under the first-game
  * option an Exile holder sees it disabled (ambiguity F8).
  *
  * Its line names the new role: "Became a Citizen." */
case object GrandScepterAction extends PaidAction(TheGrandScepterCard.action):
  private val notes = NoteSet(id)
  val becameCitizen: NoteKey = notes.key("used.citizen", Vector(
    NotePart.Text("Became a Citizen.")))
  val becameChancellor: NoteKey = notes.key("used.chancellor", Vector(
    NotePart.Text("Became the Chancellor.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(Citizenship.replaceDecisionId)

  override def listed(ready: ReadyGame, player: PlayerId): Boolean =
    !Imperials.isImperial(ready, player)

  override def usable(ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean = listed(ready, player)

  override def refusal(ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef, starts: QueuedStarts): Option[String] =
    Option.when(Citizenship.ignored(ready))(Citizenship.ignoredReason)

  def build(ready: ReadyGame, player: PlayerId, source: DecisionOptionRef)
      : Either[OathViolation, Operation] = source match
    case DecisionOptionRef.Relic(TheGrandScepterCard.id) =>
      Right(Sequence(Vector(Citizenship.become(player),
        Note(id, becameNote(_, player, source)))))
    case other => Left(OathViolation.InvalidEventOrder(
      s"${other.kind} is not the Grand Scepter"))

  private def becameNote(states: NoteStates, player: PlayerId,
      source: DecisionOptionRef): Option[LogNote] = for
    card <- PowerSourceRef.of(source)
    role <- Imperials.role(states.now, player)
    if role.isImperial
  yield if role == Role.Chancellor then becameChancellor(card)
    else becameCitizen(card)
```

Check that `refusal` blocks a direct start as well as the menu row
(`PhasePowerProcedure.check`, ~114). If it only shapes the menu, also
refuse in `build`:
`if Citizenship.ignored(ready) then Left(OathViolation.StartUnavailable(Citizenship.ignoredReason))`.

- [ ] **Step 5: Register it**

In `PhasePowerCatalog`, add `GrandScepterAction` to the
`Vector[PhasePower](WaysideInn, Elders, ...)` line. Delete the line
`relic.the-grand-scepter.action` from
`src/test/resources/powers/unimplemented.txt`.

- [ ] **Step 6: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GrandScepterActionSuite oathdigital.gameplay.powers.UnimplementedPowersSuite oathdigital.gameplay.PhaseMenuSuite"`
Expected: PASS. `UnimplementedPowersSuite` may live in another package;
find it by name.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powerresolver/PhasePower.scala \
  src/main/scala/oathdigital/gameplay/phases/PhaseMenu.scala \
  src/main/scala/oathdigital/gameplay/powers/action/GrandScepterAction.scala \
  src/main/scala/oathdigital/gameplay/powers/PhasePowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/oathdigital/gameplay/powers/action/GrandScepterActionSuite.scala
git commit -m "feat(empire): the Grand Scepter's Action makes its Exile holder a Citizen"
```

---

### Task 6: Self-exile

**Files:**
- Create: `gameplay/actions/minor/SelfExile.scala`
- Modify: `model/ProcedureRef.scala` (~121 and `ActionRef.all` at ~137)
- Modify: `gameplay/phases/ActOffers.scala` (`all`, `offeredTo`, `reason`)
- Modify: `gameplay/walker/WalkerProcedureRegistry.scala` (~275)
- Modify: `gameplay/actions/ProcedureNotes.scala` (~94)
- Modify: `application/gamelog/ProcedureLog.scala` (~274)
- Test: `src/test/scala/oathdigital/gameplay/SelfExileSuite.scala`

**Interfaces:**
- Consumes: `Citizenship.exile` (Task 4).
- Produces:
  - `ActionRef.SelfExile` with key `"self-exile"`
  - `SelfExile.Minimum: Int` = `3`
  - `SelfExile.price(ready: ReadyGame, actor: PlayerId): Int`
  - `SelfExile.build`, `SelfExile.rebuild: MinorActionProcedures.Build`
  - `SelfExile.keys: Vector[NoteKey]`, with key `left`

- [ ] **Step 1: Write the failing tests**

Follow `WarbandTransfersSuite` for the service and menu helpers.

```scala
package oathdigital.gameplay

import oathdigital.application._
import oathdigital.gameplay.actions.minor.SelfExile
import oathdigital.gameplay.phases.ActOffers
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Self-exile (CR p. 34): as a minor action a Citizen gives the Chancellor
  * 1 favor per Imperial warband on their board, at least 3, and becomes an
  * Exile (Empire slice 3a). */
class SelfExileSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val gameId = "self-exile"

  /** p1 is the Chancellor and p2 a Citizen in their Act, with `warbands`
    * on their board and `favor` favor. */
  private def citizen(warbands: Int, favor: Int): Table =
    Table.start.imperial(p1, p2).turn(p2, Phase.Act)
      .boardWarbands(p2, warbands).favor(p2, favor)

  test("the price is one favor per board warband, at least 3"):
    assertEquals(SelfExile.price(citizen(1, 5).ready, p2), 3)
    assertEquals(SelfExile.price(citizen(5, 5).ready, p2), 5)

  test("a Citizen pays the Chancellor and becomes an Exile"):
    val table = citizen(4, 6)
    val before = Look(table.ready).player(p1).board.favor
    val (service, _) = table.service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    val after = Outcomes.ready(done.state)
    assertEquals(Imperials.role(after, p2), Some(Role.Exile))
    assertEquals(Look(after).player(p2).board.favor, 2)
    assertEquals(Look(after).player(p1).board.favor, before + 4)
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("a Citizen short of favor sees why"):
    assertEquals(rules.startWalker(citizen(4, 3).state, ActionRef.SelfExile,
      p2).left.map(ActOffers.reason(ActionRef.SelfExile, _)),
      Left("You need 4 favor to give the Chancellor"))

  test("only a Citizen's menu lists it"):
    val table = citizen(4, 6)
    assert(ActOffers.offeredTo(table.ready, p2)
      .exists(_.procedure == ActionRef.SelfExile))
    assert(!ActOffers.offeredTo(table.ready, p1)
      .exists(_.procedure == ActionRef.SelfExile))
    assert(!ActOffers.offeredTo(table.ready, p3)
      .exists(_.procedure == ActionRef.SelfExile))
```

Check `Table.favor(player, n)` and `table.service()` (used in
`EmpireEndingsScenarioSuite` with a `roundEnd` argument, which has a
default) before running.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.SelfExileSuite"`
Expected: compile failure.

- [ ] **Step 3: Add the reference and the offer**

In `model/ProcedureRef.scala`, beside `TransferWarbands`:
`case object SelfExile extends ActionRef { val key = "self-exile" }`.
Append `SelfExile` to `ActionRef.all`.

In `ActOffers.all`, after the `TransferWarbands` entry:
`minor(ActionRef.SelfExile, "Exile yourself")`.

Replace `offeredTo`:

```scala
  /** The options `actor`'s Act menu lists. Warband Transfers is an
    * Imperial law and self-exile a Citizen's, so a menu leaves out an
    * option its player's role can never use. */
  def offeredTo(ready: ReadyGame, actor: PlayerId): Vector[ActOffer] =
    all.filter(offer => offer.procedure match
      case ActionRef.TransferWarbands => Imperials.isImperial(ready, actor)
      case ActionRef.SelfExile =>
        Imperials.role(ready, actor).contains(Role.Citizen)
      case _ => true)
```

In `reason`, after the `TransferWarbands` case:

```scala
      case (ActionRef.SelfExile,
          OathViolation.MinorActionUnavailable(detail)) => detail
```

- [ ] **Step 4: Write the action**

```scala
package oathdigital.gameplay.actions.minor

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._
import oathdigital.model.OathViolation._

/** Self-exile (CR p. 34): as a minor action a Citizen gives the Chancellor
  * 1 favor per Imperial warband on their board, but no less than 3, and is
  * exiled. A Citizen without that much favor sees the option disabled. */
object SelfExile:
  val Minimum: Int = 3
  private val notes = NoteSet(ProcedureNotes.id(ActionRef.SelfExile))
  /** "Gave {3 favor} to {Purple} and became an Exile." */
  val left: NoteKey = notes.key("left", Vector(NotePart.Text("Gave "),
    NotePart.Arg(0), NotePart.Text(" to "), NotePart.Arg(1),
    NotePart.Text(" and became an Exile.")))
  def keys: Vector[NoteKey] = notes.declared

  def price(ready: ReadyGame, actor: PlayerId): Int =
    PlayerFacts.player(ready, actor).fold(_ => Minimum,
      state => math.max(Minimum, state.board.warbands))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) => for
    _ <- MinorActionProcedures.started(ready, actor, args,
      ActionRef.SelfExile)
    _ <- Either.cond(Imperials.role(ready, actor).contains(Role.Citizen), (),
      MinorActionUnavailable("Only a Citizen exiles themself"))
    chancellor <- Imperials.chancellor(ready).toRight(
      MinorActionUnavailable("There is no Chancellor to pay"))
    state <- PlayerFacts.player(ready, actor)
    cost = price(ready, actor)
    _ <- Either.cond(state.board.favor >= cost, (), MinorActionUnavailable(
      s"You need $cost favor to give the Chancellor"))
  yield tree(actor, chancellor, cost)

  val rebuild: MinorActionProcedures.Build = (_, ready, actor, _) =>
    Imperials.chancellor(ready).toRight(MinorActionUnavailable(
      "There is no Chancellor to pay")).map(chancellor =>
      tree(actor, chancellor, price(ready, actor)))

  private def tree(actor: PlayerId, chancellor: PlayerId, cost: Int)
      : Operation = Sequence(Vector(
    BuildOps((_, _) => Right(Vector(Give(Piece.Favor(cost), actor,
      Location.PlayArea(actor), Location.PlayArea(chancellor)))),
      required = true),
    Citizenship.exile(actor),
    Note(ProcedureNotes.id(ActionRef.SelfExile), states =>
      Option.when(Imperials.role(states.now, actor).contains(Role.Exile))(
        left.sourceless(NoteArg.Amount(cost, NoteUnit.Favor),
          NoteArg.Player(chancellor))))))
```

`rebuild` reads the price from live state. The tree never parks, so
rebuild runs only on replay of the starting command, when the board is
still as it was.

`MinorActionProcedures.started` is `private[minor]`, which this object can
call, since it is in the same package.

- [ ] **Step 5: Wire it in**

- `WalkerProcedureRegistry`, after the `TransferWarbands` entry:

```scala
    ActionRef.SelfExile -> Entry(
      rollDecisionId = None,
      modifierWindow = None,
      build = SelfExile.build,
      rebuild = SelfExile.rebuild),
```

- `ProcedureNotes`, after the `TransferWarbands` case:
  `case ActionRef.SelfExile => SelfExile.keys`.
- `ProcedureLog`, after the `TransferWarbands` case:

```scala
    // The line names the favor and the Chancellor it went to.
    case ActionRef.SelfExile => ProcedureLog(tag = None,
      lines = ActionLineSource.Notes())
```

- [ ] **Step 6: Run the suites, then the full backend tests**

Run: `./sbtw "testOnly oathdigital.gameplay.SelfExileSuite oathdigital.gameplay.PhaseMenuSuite"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS. A suite that lists every `ActionRef` or every Act offer
needs the new one added; update its expected list.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/actions/minor/SelfExile.scala \
  src/main/scala/oathdigital/model/ProcedureRef.scala \
  src/main/scala/oathdigital/gameplay/phases/ActOffers.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerProcedureRegistry.scala \
  src/main/scala/oathdigital/gameplay/actions/ProcedureNotes.scala \
  src/main/scala/oathdigital/application/gamelog/ProcedureLog.scala \
  src/test/scala/oathdigital/gameplay/SelfExileSuite.scala
git commit -m "feat(empire): a Citizen may exile themself as a minor action"
```

Add any suite updated in Step 6 to the `git add` by name.

**Checkpoint review** after this task: Tasks 4-6 as one diff.

---

### Task 7: Scenario, projection and log

**Files:**
- Create: `src/test/scala/oathdigital/application/RoleChangesScenarioSuite.scala`

**Interfaces:**
- Consumes: everything above.

The spec's slice 3 scenario ("an Exile accepts Citizenship in a deal and
later exiles themself") needs the deal term, which is slice 3b. This
slice's scenario uses the Scepter's Action instead; 3b adds the deal
version.

- [ ] **Step 1: Write the scenario**

```scala
package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.GrandScepterAction
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.testkit.{MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** An Exile holding the Grand Scepter becomes a Citizen, and on a later
  * turn exiles themself (CR p. 34; Empire slice 3a), played through the
  * service. */
class RoleChangesScenarioSuite extends munit.FunSuite:
  private val gameId = "role-changes"
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)

  /** p1 is the Chancellor. p3, an Exile, holds the Scepter, 2 board
    * warbands and 5 favor, and acts at Broken Peaks. */
  private val table: Table = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = "Broken Peaks")
    .relic(p3, "The Grand Scepter").boardWarbands(p3, 2).favor(p3, 5)
    .allowingCitizenshipPowers

  test("an Exile becomes a Citizen by the Scepter and later exiles themself"):
    val (service, repository) = table.service()
    val citizen = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, ActionRef.UsePower(GrandScepterAction.id),
        Vector(scepter))))
    val afterScepter = Outcomes.ready(citizen.state)
    assertEquals(Imperials.role(afterScepter, p3), Some(Role.Citizen))
    // Their Act ended: Rest runs next.
    assertEquals(afterScepter.game.current.turn.phase, Phase.Rest)

    // Bring the turn back to p3's Act, as a later turn would.
    val later = Table.from(afterScepter).turn(p3, Phase.Act)
    val (laterService, _) = later.service()
    val exiled = accepted(laterService.handle(gameId, 0L,
      MenuPlay.starting(p3, ActionRef.SelfExile)))
    val afterExile = Outcomes.ready(exiled.state)
    assertEquals(Imperials.role(afterExile, p3), Some(Role.Exile))

    val lines = texts(formatter.format(present(accepted(
      laterService.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.exists(_.contains("Gave 3 favor to p1 and became an Exile")),
      lines)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, citizen.state)

  test("the Scepter's line names the new role"):
    val (service, _) = table.service()
    accepted(service.handle(gameId, 0L, MenuPlay.starting(p3,
      ActionRef.UsePower(GrandScepterAction.id), Vector(scepter))))
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.exists(_.contains("Became a Citizen.")), lines)

  test("the projection shows the Scepter disabled under the first-game option"):
    val ignoring = Table.from(table.ready).update(ready => ready.copy(
      setup = ready.setup.copy(ignoreCitizenshipPowers = true)))
    val (service, _) = ignoring.service()
    val loaded = present(accepted(service.load(gameId)), "a stored game")
    val row = present(new GameProjector(catalog)
      .project(gameId, loaded, p3).walkerDecision.flatMap(_.query)
      .toVector.flatMap(_.offeredOptions).find(_.id == scepter.wireId),
      "the Scepter row")
    assertEquals(row.reason, Some(Citizenship.ignoredReason))
```

Match the helpers to the codebase before running:
- `MenuPlay.starting`'s `StartableRef` for a power. `PhaseMenu.optionFor`
  maps `ActionRef.UsePower(id)` with the source in `args`; see how
  `PhaseMenuSuite` starts a relic power from the menu.
- `service.load`, `GameProjector`'s constructor (it takes the catalog;
  `WarbandTransfersSuite` builds one from `FirstGameSetupFixture.catalog`),
  and the reason field of `DecisionOptionProjection`
  (`ActionProjectionDtos.scala:~160`).
- The second half drives a later turn from a new table built on the
  first half's state. If `Table.from(ready).service()` cannot start from
  a non-initial state, instead end p3's turn through the service and play
  the other players' turns with `MenuPlay`, as `EmpireEndingsScenarioSuite`
  does for its round-end turn.
- The log line's player name: `formatter` writes `p1` for player `p1` in
  `EmpireEndingsScenarioSuite`; keep the same viewer.

- [ ] **Step 2: Run it until it passes**

Run: `./sbtw "testOnly oathdigital.application.RoleChangesScenarioSuite"`
Expected: PASS. A failure on the Rest phase means Rest's own procedures
did not start after `EnterPhase(Phase.Rest)`. Compare with
`MurkyFountainSuite`, which ends the Act the same way, before changing
any production code.

- [ ] **Step 3: Commit**

```bash
git add src/test/scala/oathdigital/application/RoleChangesScenarioSuite.scala
git commit -m "test(empire): an Exile becomes a Citizen by the Scepter and exiles themself"
```

---

### Task 8: Docs and gates

**Files:**
- Modify: `docs/rules/ambiguities.md` (Empire rulings, ~41-72)
- Modify: `docs/rules/implementation-traceability.md` (rows at lines 110 and 120)
- Modify: `docs/rules/rules-reference.md` (Empire section, if it lists
  implemented rules)
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
  (status line; "Slices" list)
- Modify: `docs/ROADMAP.md` (Phase - Empire)

- [ ] **Step 1: Record the rulings**

In `ambiguities.md`, after R17, add in the same format:
- **R18, short purple bank.** Quote the ruling from "Rulings for this
  slice" above, and cite CR p. 34.
- **R19, Supremacy title.** Quote it, and cite CR pp. 34-35.

- [ ] **Step 2: Update traceability**

- Row 110 (core minor actions): add self-exile
  (`gameplay/actions/minor/SelfExile.scala`, `SelfExileSuite`).
- Row 120 (Chancellor/Citizen roles):
  - Replace "The Grand Scepter's powers and Citizenship remain
    unimplemented (Empire slice 3)" with: "Empire slice 3a landed
    becoming a Citizen or the Chancellor and exile (`gameplay/roles`,
    `RoleChangesSuite`, `RoleChangesScenarioSuite`), the Scepter's Action,
    self-exile, the first-game option and R4's stored round starter. The
    Scepter's battle plan and deal term, the five 'become a Citizen'
    denizens and the Throne's offer remain (Empire slice 3b)."
  - In its gap column, change "first-game policy F8" to "first-game
    option F8 is read; changing it is the Settings Feature phase".
- Row 111 (power framework): change "first-game citizenship policy F8
  remain" to "the Settings Feature toggle for F8 remains".

- [ ] **Step 3: Update the spec and roadmap**

- Spec status line: append "Slice 3a implemented, 2026-11-01."
- Spec "Slices" item 4: change to "**3, Role changes in play**, as two
  plans: 3a, the role changes with self-exile and the Scepter's Action;
  3b, the Scepter's battle plan and deal term, the five denizens and the
  Throne's offer."
- ROADMAP, Phase - Empire: replace the slice 3 item with:

```markdown
- [ ] **Slice 3b, Citizenship sources.** The Grand Scepter's battle plan
  and its "becomes a Citizen" deal term, the five "become a Citizen"
  denizens, and the Throne's Offer Citizenship step, now an empty window.
```

- [ ] **Step 4: Run every gate**

Run, in the background, and read the results:
```bash
./sbtw clean coverage test coverageReport
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```
Expected: all pass, statement coverage at least 92%.

- [ ] **Step 5: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/implementation-traceability.md \
  docs/rules/rules-reference.md \
  docs/superpowers/specs/2026-10-29-empire-phase-design.md docs/ROADMAP.md
git commit -m "docs(empire): record slice 3a and rulings R18-R19"
```

Leave `rules-reference.md` out of the `git add` if it did not change.

**Final review** after this task: the whole branch against the spec's
slice 3 section and this plan.
