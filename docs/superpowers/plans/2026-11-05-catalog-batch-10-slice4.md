# Catalog Batch 10 Slice 4: Own-Power Lock Exemption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a power's use move its own lock-icon card, and implement Giant's Path (E09 intact) on that exemption.

**Architecture:**
- A capability trait, `IgnoresOwnLock extends PhasePower`, names the card whose lock the power's use ignores (`ownCard`). `IgnoresOwnLock.exemptions` maps each such phase power's id to its card.
- `WalkerPowers` gains `lockExemptions: Map[PowerId, CardId]`, which `WalkerPowerCatalog.default` fills from `PhasePowerCatalog.default`. It also gains `restrictionsDuring(procedure)`:
  - For `ActionRef.UsePower(id)` with an exemption, it returns the operation restrictions with that card left out of `LockedCards`.
  - Otherwise it returns the unchanged `operationRestrictions`.
  - Each exempt card's set is built once per instance.
- `OperationRestrictions.active` takes `unlocked: Set[CardId]` and removes those cards from the printed `LockedCards`. The as-if rules (Ancient Bloodline) stay.
- `WalkCtx.operationRestrictions` reads `powers.restrictionsDuring(procedure)`. The three walker readers of `ctx.powers.operationRestrictions` switch to it. They are `ProcedureWalker`'s batch check, `WalkerSteps.recordBatch` and `AdviserArrivalPass`.
- Every walk already carries its procedure: a start, a resume, a menu dry run (`WalkerSimulation` gets `procedure`) and a park-time narrowing (`narrowParked` reads `walkerProcedure`). So the exemption holds wherever the use is walked, and only there. The search memo is keyed on the procedure, so verdicts never cross between exempt and plain walks.
- Replay applies recorded operations without restrictions, so nothing is journaled.
- Giant's Path is a `PaidAction` mixing in `IgnoresOwnLock`:
  - A `Decide` asks for a destination among `SiteRoom.open` less its own site.
  - A required `BuildOps` moves the card, with its tokens, by a plain `Move`, then places the pawn with `PawnMoves.relocate`.
  - Its `refusal` shows it unavailable when no other site has room.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Readings" (Giant's Path)
- "Slice 4: Own-power lock exemption"
- "Testing"
- "Out of scope"

Card text: `GiantsPathCard.intact` in `src/main/scala/distantlands/gameplay/powers/action/GiantsRampage.scala`, which reads "**ACTION:** Move this card to another site, then place your pawn there."

This is the fourth of 14 slice plans, written against `0.2.0-alpha` at `ec4cab5f`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-4` on branch `batch-10-slice-4`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports and unused values fail the build.
- **Line cap:** `ProcedureWalker.scala` has 797 lines, against a cap of 800. Its edit must replace a line, not add one.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write` (`src/test/resources/notes/declared-keys.txt`) and `UNIMPLEMENTED_SNAPSHOT=write` (`src/test/resources/powers/unimplemented.txt`), then read the diff. Commit only the lines the task explains.
- **Log line:** the spec's, "Giant's Path and {Blue} now stand at {site}." It is parallel with Giant's Rampage's "Giant's Rampage and {Blue} now stand there." It covers its moves (`covers = true`), as the Rampage's does.
- **Hover reasons:** show an unavailable option disabled with a reason (memory: "Prefer hover reasons").
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").

---

### Task 1: The own-power lock exemption

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powerresolver/IgnoresOwnLock.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationRestrictions.scala:26-31` (`active`) and its companion
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowers.scala` (field, `operationRestrictions`, `restrictionsDuring`, doc)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkState.scala` (`WalkCtx.operationRestrictions`)
- Modify: `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala:315`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerSteps.scala:66`
- Modify: `src/main/scala/distantlands/gameplay/walker/AdviserArrivalPass.scala:43`
- Test: `src/test/scala/distantlands/gameplay/walker/WalkerPowersLockExemptionSuite.scala`

**Interfaces:**
- Produces:
  - `trait IgnoresOwnLock extends PhasePower { def ownCard: CardId }`
  - `IgnoresOwnLock.exemptions(powers: Vector[PhasePower]): Map[PowerId, CardId]`
  - `WalkerPowers.lockExemptions: Map[PowerId, CardId]` (default `Map.empty`)
  - `WalkerPowers.restrictionsDuring(procedure: Option[ProcedureRef]): Vector[OperationRestriction]`
  - `OperationRestrictions.active(powerRestrictions, modifiers, picked = Vector.empty, unlocked: Set[CardId] = Set.empty)`
  - `WalkCtx.operationRestrictions: Vector[OperationRestriction]`

- [ ] **Step 1: Write the failing suite**

Create `WalkerPowersLockExemptionSuite.scala`:

```scala
package distantlands.gameplay.walker

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.{HallOfMinistersCard,
  OperationRestrictions}
import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table

/** The own-power lock exemption (catalog batch 10, slice 4): while a power
  * that ignores its own lock is used, its card's lock does not bind; every
  * other lock does, and the card's lock binds every other power. */
class WalkerPowersLockExemptionSuite extends munit.FunSuite:
  private val hall: CardId = HallOfMinistersCard.id
  private val spring: CardId = HallowedSpringCard.id
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** A power whose use ignores the Hall of Ministers' lock. */
  private val hallMover = PowerId("test.hall-mover")
  private val powers = WalkerPowers(Vector.empty,
    restrictionSet = OperationRestrictions.forCatalog(catalog),
    lockExemptions = Map(hallMover -> hall))
  /** The Hall of Ministers and Hallowed Spring, both locked while intact,
    * at Ancient City. */
  private val twoLockedEdifices: ReadyGame = Table.start
    .edifice(HallOfMinistersCard, EdificeSide.Intact, at = home)
    .edifice(HallowedSpringCard, EdificeSide.Intact, at = home).ready

  private def moveToPeaks(card: CardId): CoreOperation =
    Move(Piece.Card(card), PositionedLocation(Location.Site(home)),
      PositionedLocation(Location.Site(peaks)), by = Some(Table.p1))

  /** The code of the first restriction refusing `card`'s move during
    * `procedure`. */
  private def refusal(procedure: Option[ProcedureRef],
      card: CardId): Option[String] =
    powers.restrictionsDuring(procedure)
      .flatMap(_.reason(twoLockedEdifices, moveToPeaks(card)))
      .headOption.map(_.code)

  private val hallMoverUse = Some(ActionRef.UsePower(hallMover))

  test("outside any power use both locked edifices refuse a move"):
    assertEquals(refusal(None, hall), Some("locked"))
    assertEquals(refusal(None, spring), Some("locked"))

  test("during the exempt power's use its own card moves and every other " +
      "lock binds"):
    assertEquals(refusal(hallMoverUse, hall), None)
    assertEquals(refusal(hallMoverUse, spring), Some("locked"))

  test("during another power's use the exempt card's lock still binds"):
    assertEquals(refusal(Some(ActionRef.UsePower(PowerId("test.other"))),
      hall), Some("locked"))

  test("the exempt set is built once, and a plain walk keeps the shared set"):
    assert(powers.restrictionsDuring(hallMoverUse) eq
      powers.restrictionsDuring(hallMoverUse))
    assert(powers.restrictionsDuring(None) eq powers.operationRestrictions)
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.WalkerPowersLockExemptionSuite"`
Expected: compile failure, because `lockExemptions` and `restrictionsDuring` do not exist.

- [ ] **Step 3: Add the trait**

Create `IgnoresOwnLock.scala`:

```scala
package distantlands.gameplay.powerresolver

import distantlands.model.{CardId, PowerId}

/** A phase power whose use may move or flip its own lock-icon card (catalog
  * batch 10, slice 4). While the walker runs the power's use
  * (`ActionRef.UsePower`), the lock on `ownCard` does not bind. Every other
  * card's lock still binds, and `ownCard`'s lock binds every other power.
  * The rulebook words such exemptions the same way: "Bury ignores locked".
  *
  * Operation restrictions run only at command time; replay applies the
  * recorded operations without them. The exemption is therefore never
  * journaled.
  */
trait IgnoresOwnLock extends PhasePower:
  /** The card whose lock this power's use ignores. */
  def ownCard: CardId

object IgnoresOwnLock:
  /** The card each of `powers` may move despite its lock, by power. */
  def exemptions(powers: Vector[PhasePower]): Map[PowerId, CardId] =
    powers.collect { case power: IgnoresOwnLock =>
      power.id -> power.ownCard }.toMap
```

- [ ] **Step 4: Let `OperationRestrictions.active` lift locks**

In `OperationRestrictions.scala`, replace `active` with:

```scala
  def active(powerRestrictions: Vector[OperationRestriction],
      modifiers: Vector[PowerId], picked: Vector[CardId] = Vector.empty,
      unlocked: Set[CardId] = Set.empty)
      : Vector[OperationRestriction] =
    printed.map(restriction => ConsentedRestriction(
        OperationRestrictions.unlocking(restriction, unlocked))) ++ catalog
      .filter(_ => modifiers.nonEmpty || picked.nonEmpty)
      .map(ActiveModifier(_, modifiers, picked)).toVector ++
      powerRestrictions.map(ConsentedRestriction(_))
```

Add to the class doc, after the sentence about `active` adding the `ActiveModifier` rule:

```
  * `unlocked` are the cards whose lock does not bind for this walk: the
  * own card of a power whose use ignores its lock
  * ([[distantlands.gameplay.powerresolver.IgnoresOwnLock]]).
```

Add to the companion `object OperationRestrictions`, after `inPlay`:

```scala
  /** `restriction` with the locks on `unlocked` lifted: a [[LockedCards]]
    * without those cards, its as-if rules kept. Any other restriction is
    * returned unchanged. */
  private def unlocking(restriction: OperationRestriction,
      unlocked: Set[CardId]): OperationRestriction = restriction match
    case locks: LockedCards if unlocked.nonEmpty =>
      locks.copy(cards = locks.cards -- unlocked)
    case other => other
```

- [ ] **Step 5: Give `WalkerPowers` the exemptions**

In `WalkerPowers.scala`:

1. Import `ActionRef`, `CardId` and `ProcedureRef` from `distantlands.model`, beside the existing model imports.
2. Add a constructor field after `catalog`:

```scala
    catalog: ExecutableCatalog = NewFoundations.catalog,
    /** The card each power that ignores its own lock may move, by power
      * ([[distantlands.gameplay.powerresolver.IgnoresOwnLock]]).
      * `WalkerPowerCatalog.default` fills it from the phase powers; a
      * `WalkerPowers` assembled by hand exempts nothing. */
    lockExemptions: Map[PowerId, CardId] = Map.empty):
```

3. Replace the `operationRestrictions` lazy val with:

```scala
  /** The operation restrictions every step of this command runs with,
    * outside a use that ignores its own lock. A selected power that
    * discards its own card is not held in play by the active-modifier
    * rule. */
  lazy val operationRestrictions: Vector[OperationRestriction] =
    restrictionsUnlocking(Set.empty)

  /** The operation restrictions a walk of `procedure` runs with: for the
    * use of a power that ignores its own lock, every restriction but that
    * card's lock (catalog batch 10, slice 4); otherwise
    * [[operationRestrictions]]. Each exempt card's set is built once per
    * instance. */
  def restrictionsDuring(procedure: Option[ProcedureRef])
      : Vector[OperationRestriction] = procedure match
    case Some(ActionRef.UsePower(id)) =>
      lockExemptions.get(id).fold(operationRestrictions)(card =>
        exempted.computeIfAbsent(card, own => restrictionsUnlocking(Set(own))))
    case _ => operationRestrictions

  private lazy val exempted =
    new java.util.concurrent.ConcurrentHashMap[CardId,
      Vector[OperationRestriction]]

  private def restrictionsUnlocking(unlocked: Set[CardId])
      : Vector[OperationRestriction] =
    restrictionSet.active(powers.flatMap(_.operationRestrictions(catalog)),
      modifiers.filterNot(id => powers.exists(power =>
        power.id == id && power.discardsItself)), unlocked = unlocked)
```

4. In the class doc, replace "Together with the offered powers' own restrictions they make `operationRestrictions`, which every walker step runs with (global operation restrictions design)." with "Together with the offered powers' own restrictions they make `operationRestrictions`, which every walker step runs with (global operation restrictions design). A walk reads them through `restrictionsDuring`, which lifts `lockExemptions`' lock for that power's own use."

- [ ] **Step 6: Route the walker through `WalkCtx`**

In `WalkState.scala`, import `OperationRestriction` from `distantlands.model` and add a method to `WalkCtx`, after the last field's closing parenthesis turns the case class body into a block:

```scala
):
  /** The operation restrictions this walk's batches run with: its powers'
    * restrictions for the procedure it walks, which lift a power's own lock
    * during that power's use (catalog batch 10, slice 4). */
  def operationRestrictions: Vector[OperationRestriction] =
    powers.restrictionsDuring(procedure)
```

The case class currently ends with `following: Boolean = false` then `)`. Change the `)` to `):` and add the method beneath it, indented two spaces.

Replace `ctx.powers.operationRestrictions` with `ctx.operationRestrictions` in exactly three places. Each is a one-line swap that adds no line:
- `ProcedureWalker.scala:315`
- `WalkerSteps.scala:66`
- `AdviserArrivalPass.scala:43`

Leave `ModifierOffers.scala:46` on `powers.operationRestrictions`. That check prices a modifier pick before any procedure's batch runs, and no power that ignores its lock is a modifier.

- [ ] **Step 7: Run the suite and the walker suites**

Run: `./sbtw "testOnly distantlands.gameplay.walker.* distantlands.gameplay.OperationRestrictionsSuite distantlands.gameplay.operations.*"`
Expected: PASS, with the four new tests among them.

Run: `wc -l src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala`
Expected: `797`.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powerresolver/IgnoresOwnLock.scala \
  src/main/scala/distantlands/gameplay/operations/OperationRestrictions.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerPowers.scala \
  src/main/scala/distantlands/gameplay/walker/WalkState.scala \
  src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerSteps.scala \
  src/main/scala/distantlands/gameplay/walker/AdviserArrivalPass.scala \
  src/test/scala/distantlands/gameplay/walker/WalkerPowersLockExemptionSuite.scala
git commit -m "feat(walker): let a power's own use ignore its card's lock

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint:** a review subagent reads Task 1's diff against this task and the spec before Task 2 starts.

---

### Task 2: Giant's Path

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/action/GiantsPath.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/GiantsRampage.scala:26-42` (doc)
- Modify: `src/main/scala/distantlands/gameplay/powers/action/OtherActionPowers.scala:19`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` (`lockExemptions`, `default`)
- Modify: `src/test/scala/distantlands/testkit/PhasePowerHarness.scala:90-93` (`usableNow`)
- Create: `src/test/scala/distantlands/gameplay/powers/action/GiantsPathSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (snapshots)
- Modify: `docs/ROADMAP.md:71`, `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md:3`

**Interfaces:**
- Consumes: `IgnoresOwnLock`, `IgnoresOwnLock.exemptions`, `WalkerPowers.lockExemptions`, `WalkerPowers.restrictionsDuring` (Task 1).
- Produces:
  - `case object GiantsPath extends PaidAction(GiantsPathCard.intact.power) with IgnoresOwnLock`, with `decisionId: String`, `NoRoom: String`, `used: NoteKey` and `destinations(catalog: ExecutableCatalog, ready: ReadyGame): Vector[SiteId]`
  - `WalkerPowerCatalog.lockExemptions: Map[PowerId, CardId]`

- [ ] **Step 1: Write the failing suite**

Create `GiantsPathSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.catalog.AdviserOnly
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.HallOfMinistersCard
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.{NoteText, PhasePowerCatalog,
  PowerFixture, SiteRoom, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay,
  PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.{assertWireRoundTrips, ready}
import distantlands.testkit.Unwrap.accepted

/** Giant's Path (catalog batch 10, slice 4): move the locked Path to
  * another site with room, tokens and all, then place the user's pawn
  * there. The Path's own use ignores its lock; nothing else does. */
class GiantsPathSuite extends munit.FunSuite:
  import ActPowerHarness.{answer, replayed, rulesWithPrintedRestrictions,
    use}
  import PowerFixture._

  private val path = GiantsPathCard.id
  private val source = DecisionOptionRef.Edifice(path)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** The production walker powers, so the printed locks hold. */
  private val rules0 = rulesWithPrintedRestrictions()
  private val walkerPowers = WalkerPowerCatalog.default(catalog)

  /** p1's Act with 1 faceup secret, the intact Path at Ancient City and
    * p1's pawn at `pawnAt`. */
  private def pathTable(pawnAt: SiteId = home): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, pawnAt)
      .edifice(path, EdificeSide.Intact, at = home).secrets(actor, 1)

  /** A site in play that is neither Ancient City nor Broken Peaks. */
  private def thirdSite(ready: ReadyGame): SiteId =
    ready.game.current.map.inPlay.filterNot(Set(home, peaks)).head

  /** `table` with every empty slot at `sites` filled by a denizen that has
    * no persistent power and is not adviser-only. */
  private def withSitesFilled(table: Table, sites: Vector[SiteId]): Table =
    val fillers = catalog.denizens.iterator.filter(card =>
      !card.isInstanceOf[AdviserOnly] && card.powers.forall(!_.persistent))
    sites.foldLeft(table)((filled, site) =>
      SiteRoom.emptySlots(catalog, filled.ready, site).foldLeft(filled)(
        (more, _) => more.denizen(fillers.next().id, at = site)))

  private def toSite(site: SiteId) = DecisionOptionRef.Site(site)

  /** The Path's option in p1's Act menu, as the menu offers it. */
  private def menuOption(state: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, state, actor,
      PhasePowerCatalog.default, walkerPowers)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)

  private def lockRefusal(procedure: ProcedureRef, card: CardId,
      state: ReadyGame): Option[String] =
    val move = Move(Piece.Card(card), PositionedLocation(Location.Site(home)),
      PositionedLocation(Location.Site(peaks)), by = Some(actor))
    walkerPowers.restrictionsDuring(Some(procedure))
      .flatMap(_.reason(state, move)).headOption.map(_.code)

  test("it pays a secret, moves the Path with its tokens to the chosen " +
      "site and places the pawn there, with the line, and the journal " +
      "replays and round trips"):
    val start = pathTable().ready
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    ActPowerHarness.parkedWithWalkerPowers.assertParked(parked.state,
      ActionRef.UsePower(GiantsPath.id), GiantsPath.decisionId, actor)
    assertEquals(Look(ready(parked.state)).tokensOn(path), Tokens(0, 1))
    val done = accepted(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(peaks)))
    val after = ready(done.state)
    assertEquals(Look(after).cardsAt(peaks), Vector[CardId](path))
    assertEquals(Look(after).cardsAt(home), Vector.empty)
    assertEquals(Look(after).tokensOn(path), Tokens(0, 1))
    assertEquals(Look(after).pawn(actor), peaks)
    PhasePowerHarness.parked.assertResumed(done.state, Phase.Act, actor)
    assertEquals(NoteText.said(GiantsPath, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"Giant's Path and ${actor.value} now " +
        s"stand at ${peaks.value}.", covers = true)))
    assertEquals(replayed(rules0, start, parked.events ++ done.events), after)
    assertWireRoundTrips(parked.events ++ done.events)

  test("used from afar at a site the user rules, the pawn is placed from " +
      "where it stood"):
    val start = pathTable(pawnAt = peaks).warbandsAt(home, actor, 1).ready
    val third = thirdSite(start)
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    val after = ready(accepted(answer(rules0, parked.state,
      GiantsPath.decisionId, toSite(third))).state)
    assertEquals(Look(after).cardsAt(third), Vector[CardId](path))
    assertEquals(Look(after).pawn(actor), third)

  test("its own site and a full site are not destinations"):
    val start = withSitesFilled(pathTable(), Vector(peaks)).ready
    assertEquals(GiantsPath.destinations(catalog, start),
      start.game.current.map.inPlay.filterNot(Set(home, peaks)))
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    assert(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(home)).isLeft, "its own site")
    assert(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(peaks)).isLeft, "a full site")

  test("with no other site in room it is shown unavailable with the " +
      "reason, and a direct use is refused"):
    val open = pathTable().ready
    assert(menuOption(open).exists {
      case _: DecisionOption.Unavailable => false
      case _ => true
    }, "available while a site has room")
    val full = withSitesFilled(pathTable(),
      open.game.current.map.inPlay.filterNot(_ == home)).ready
    assertEquals(menuOption(full).collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(GiantsPath.NoRoom)))
    assertEquals(use(rules0, full, GiantsPath.id, source).left.toOption,
      Some(OathViolation.StartUnavailable(GiantsPath.NoRoom)))

  test("the Path's lock binds every other power, and its own use still " +
      "cannot move another locked card"):
    assertEquals(walkerPowers.lockExemptions,
      Map[PowerId, CardId](GiantsPath.id -> path))
    val state = pathTable()
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = home).ready
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsRampage.id), path,
      state), Some("locked"))
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsPath.id),
      HallOfMinistersCard.id, state), Some("locked"))
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsPath.id), path, state),
      None)
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.GiantsPathSuite"`
Expected: compile failure, because `GiantsPath` and `WalkerPowerCatalog.lockExemptions` do not exist.

- [ ] **Step 3: Write the power**

Create `GiantsPath.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.powerresolver.{IgnoresOwnLock, QueuedStarts}
import distantlands.gameplay.powers.{PowerAnswers, SiteRoom}
import distantlands.gameplay.powers.setup.EdificeSetupSupport
import distantlands.model._

/** Giant's Path (E09, intact), ACTION: place 1 secret on this card, move it
  * to another site, then place the user's pawn there (catalog batch 10,
  * slice 4).
  *
  * The Path shows the lock icon. Its use ignores its own lock
  * ([[IgnoresOwnLock]]), and nothing else does. The destination needs an
  * empty slot (batch 4, "Site capacity"), so the question lists
  * `SiteRoom.open` less the Path's own site. With no such site, the option
  * shows unavailable with [[NoRoom]]. The card moves by a plain `Move`,
  * keeping its tokens, and the pawn is placed, not moved by Travel, as
  * Giant's Rampage places it. Its line: "Giant's Path and {Blue} now stand
  * at {site}."
  */
case object GiantsPath extends PaidAction(GiantsPathCard.intact.power)
    with IgnoresOwnLock:
  val ownCard: CardId = GiantsPathCard.id
  val decisionId: String = "power.giants-path.site"
  val heading: String = "Giant's Path: move to which site?"
  val NoRoom: String = "No other site has room for Giant's Path."

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Giant's Path and {Blue} now stand at {site}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Giant's Path and "), NotePart.Arg(0),
    NotePart.Text(" now stand at "), NotePart.Arg(1), NotePart.Text(".")))

  private def pathSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, GiantsPathCard.id, EdificeSide.Intact)

  /** The sites in play with room for the Path, less its own, in map
    * order. */
  def destinations(catalog: ExecutableCatalog, ready: ReadyGame)
      : Vector[SiteId] =
    val here = pathSite(ready)
    SiteRoom.open(catalog, ready).filterNot(here.contains)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(destinations(catalog, ready).isEmpty)(NoRoom)

  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      Branch((live, _) => Vector(Decide(decisionId, player,
        DecisionQuery.ChooseOne(destinations(catalog, live).map(site =>
          DecisionOption.Site(DecisionOptionRef.Site(site))),
          heading = Some(heading))))),
      BuildOps((live, pending) => moved(live, player, pending),
        required = true),
      Note(id, line(_, player), covers = true))))

  /** The Path's move to the answered site, then the pawn's placement. */
  private def moved(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, Vector[CoreOperation]] =
    for
      to <- PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Site(site) => site }.toRight(
        OathViolation.InvalidEventOrder("Giant's Path's site is unanswered"))
      from <- pathSite(ready).toRight(OathViolation.InvalidEventOrder(
        "Giant's Path is not on the map"))
      placed <- PawnMoves.relocate(ready, player, to)
    yield Move(Piece.Card(GiantsPathCard.id),
      PositionedLocation(Location.Site(from)),
      PositionedLocation(Location.Site(to)), by = Some(player)) +: placed

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    PowerAccess.pawnSite(states.now, player).map(site =>
      used(PowerSourceRef.Card(GiantsPathCard.id), NoteArg.Player(player),
        NoteArg.Site(site)))
```

- [ ] **Step 4: Register it**

1. In `OtherActionPowers.scala`, add `GiantsPath` before `GiantsRampage`.
2. In `WalkerPowerCatalog.scala`, import `IgnoresOwnLock` from `distantlands.gameplay.powerresolver` and `CardId` and `PowerId` from `distantlands.model`. Add, before `default`:

```scala
  /** The card each phase power that ignores its own lock may move, by
    * power (catalog batch 10, slice 4). Lazy, so that reading
    * `PhasePowerCatalog.default` never runs during this object's own
    * initialisation, which `PhasePowerCatalog` reads for
    * `useRestrictions`. */
  lazy val lockExemptions: Map[PowerId, CardId] =
    IgnoresOwnLock.exemptions(PhasePowerCatalog.default.powers)
```

3. Pass it in `default`:

```scala
  def default(catalog: ExecutableCatalog): WalkerPowers =
    WalkerPowers(contributing,
      restrictionSet = OperationRestrictions.forCatalog(catalog),
      declared = Some(DeclaredNotes.of(contributing)), catalog = catalog,
      lockExemptions = lockExemptions)
```

4. In `PhasePowerHarness.usableNow`, pass `lockExemptions = WalkerPowerCatalog.lockExemptions` to its `WalkerPowers(...)`, so its menu holds the printed locks as production does. Without the exemption, the Path would read as unusable there. Import `WalkerPowerCatalog` if the file lacks it.

5. In `GiantsRampage.scala`, replace the doc's last sentence ("The intact face, Giant's Path, is locked and cannot move itself (catalog batch 9, "Out of scope").") with "The intact face is [[GiantsPath]]."

- [ ] **Step 5: Run the suites to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.GiantsPathSuite distantlands.gameplay.powers.action.GiantsRampageSuite"`
Expected: PASS. The Rampage's test "the intact Giant's Path offers no action" still passes: it checks only `GiantsRampage.id`.

- [ ] **Step 6: Check that the exemption carries the suite**

Remove `with IgnoresOwnLock` from `GiantsPath`, and turn `ownCard` into a plain `val`. Rerun `GiantsPathSuite`.

Expected: the first three tests fail. The lock refuses the Path's move, so the search hides every destination and the start or the answer is refused. The fifth test fails on `lockExemptions`.

Restore the file with `git checkout -- src/main/scala/distantlands/gameplay/powers/action/GiantsPath.scala` if it is committed, or else undo the edit by hand. Rerun the suite and confirm it passes.

- [ ] **Step 7: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`
Then `git diff src/test/resources`. Expected:
- `declared-keys.txt` gains `edifice.e09.intact used = Giant's Path and {0} now stand at {1}.`
- `unimplemented.txt` drops `edifice.e09.intact`.

Nothing else changes.

- [ ] **Step 8: Update the docs**

- `docs/ROADMAP.md`: delete the line `- **Own-power lock exemption:** Giant's Path.` (memory: "Roadmap deletes done work").
- Spec status line: `**Status:** designed 2026-11-03. Slices 1 to 4 landed.`

- [ ] **Step 9: Run the gates**

Run: `./sbtw clean coverage test coverageReport` (background; about 10 minutes)
Expected: all tests pass, statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/action/GiantsPath.scala \
  src/main/scala/distantlands/gameplay/powers/action/GiantsRampage.scala \
  src/main/scala/distantlands/gameplay/powers/action/OtherActionPowers.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/distantlands/testkit/PhasePowerHarness.scala \
  src/test/scala/distantlands/gameplay/powers/action/GiantsPathSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt \
  docs/ROADMAP.md docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md
git commit -m "feat(powers): Giant's Path moves itself despite its lock and places the pawn there

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint:** a review subagent reads the whole slice diff against this plan and the spec before the merge.
