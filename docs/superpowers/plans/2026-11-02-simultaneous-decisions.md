# Simultaneous Decisions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The walker gains a `Simultaneous` node, and the Throne's offered Exiles answer Citizenship in any order, each answer applying at once.

**Architecture:**
- A `Simultaneous(children)` operation parks at its own path with one open `Decide` per child.
- An answer resumes at the node. The walk answers the matching child, runs that child's tail, and parks again until every child is answered.
- `WalkerPowerGather.leavesAt` returns every open child decision, so `ProcedureWalker.openDecisions` returns several.
- Callers that need one decision look it up by decision id, or by owner.
- `WalkerWaitingProjection` names every awaited player in seat order.
- `OfferCitizenship` wraps the per-Exile answers in one `Simultaneous`.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-02-simultaneous-decisions-design.md`. The "Plan rulings" below change the spec where it met the code. Task 5 writes them back into the spec.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/simultaneous-decisions` on branch `simultaneous-decisions`, off `0.2.0-alpha`. Never work in the main checkout.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree.
  - Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`. Unused imports, unused values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%;
  - `./sbtw frontend/test`;
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules;
  - `python3 scripts/check-markdown-links.py`.
- **Line cap:** `ProcedureWalker.scala` is at 797 lines. Task 1 moves the park queries out before Task 2 adds the node. Run `check-architecture.py` after each of those tasks.
- **Wire:** old journals and old clients need not decode. Every changed wire field gets a round-trip test.
- **Answers (user):** Throne answers are public and apply at once. Only the forced seat order goes away.
- **Child shape (user):** each child is one `Decide`, or a `Sequence` that starts with one. Its tail never parks.
- **Search (user):** the search answers children in child order.
- **Views (user):**
  - A player who still owes an answer sees their own decision.
  - Everyone else sees the waiting line, which names every awaited player in seat order.
- **Prose:** docs, comments and commit messages are normal English.

## Plan rulings

These change the spec. Each says why, and what it costs if wrong.

- **P1, `parkedDecide` keeps its name.**
  - Spec: rename it `soleDecide`.
  - Plan: keep the name `parkedDecide`, and give it the sole semantics: `None` when more than one `Decide` is open.
  - Why: about 80 test call sites use it, and the rename buys nothing.
  - Cost if wrong: one mechanical rename later.
- **P2, child Decides carry no window.**
  - Spec: each child's Decide folds its own window.
  - Plan: a windowed child Decide is a malformed node.
  - Why: a fold may insert operations before the Decide, and the answer cursor into the child would then miss it. No power hooks the Throne answers.
  - Cost if wrong: lifting the rule needs a cursor that finds the Decide inside the fold.
- **P3, answer steps record the child Decide's own path.**
  - Spec: the answer step's `nodeId` is the node's path.
  - Plan: `WalkerSteps.answerDecide` records the Decide's own path, as everywhere else. `WalkerReplay.validateParkedStep` accepts a step whose `nodeId` equals the parked path or lies under it.
  - Why: no change to `answerDecide`, and the journal says which child was answered.
  - Cost if wrong: replay accepts a step under a parked composite that it rejected before. Only a `Simultaneous` parks at a composite.
- **P4, no search-key change.**
  - Spec: the `visited` key and the `verdict` resume carry the decision id.
  - Plan: no change. Child Decides have distinct paths, so a search that reaches them never collides. Narrowing a parked child resumes at the node's path, which the node handles. `SearchMemo.Base` already holds the decision id.
- **P5, a fresh live walk parks without narrowing.** Each child is narrowed when it is projected or answered, through `narrowParked`, as any parked Decide is. A required child with no option left parks, and the answer-time check refuses it. The Throne's two buttons are always open.
- **P6, no new golden log.**
  - Spec: a new golden-log script answers out of order.
  - Plan: `LogScripts` drivers answer the park `ParkedNode` reports, which is the first open decision in seat order, so a script cannot answer out of order. `OfferCitizenshipSuite` asserts the out-of-order lines instead. The existing `throne-offered` golden log must not change.
- **P7, sole-decision callers stay sole.**
  - `WalkerSimulation.reached`, `previewParked` and `waitsOnStep`, `ModifierChoice.explained` and `OathRulesWalker.parkedPlanDecision` keep `parkedDecide`.
  - They serve modifier picks, priced or playable options and Campaign plans. None of these is ever a `Simultaneous` child.
  - At a `Simultaneous` park they see `None`, as at a Roll park.
- **P8, Simultaneous inside a Repeat.** "Answered" is read from `pending.answered` by decision id. A node inside a `Repeat` therefore needs fresh decision ids on each pass. Nothing builds one yet. The node's doc comment says so.

## File map

- Create:
  - `src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala`: the park queries moved out of `ProcedureWalker`.
  - `src/main/scala/oathdigital/gameplay/walker/SimultaneousChildren.scala`: well-formedness, open children, the entry cursor.
  - `src/test/scala/oathdigital/gameplay/SimultaneousDecideSuite.scala`.
- Modify:
  - `src/main/scala/oathdigital/model/Operation.scala`: the node.
  - `src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala`: forwarders, `walkSimultaneous`.
  - `src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala`: `leavesAt`.
  - `src/main/scala/oathdigital/gameplay/walker/WalkerReplay.scala`: the parked-step check.
  - `src/main/scala/oathdigital/gameplay/OathRulesWalker.scala`: lookups by id.
  - `src/main/scala/oathdigital/gameplay/walker/PricedOptions.scala`: lookup by id.
  - `src/main/scala/oathdigital/application/WalkerDecisionProjector.scala`, `ConsentProjector.scala` and `gamelog/ChoiceWords.scala`.
  - `shared/src/main/scala/oathdigital/protocol/projection/ActionProjectionDtos.scala` and `ActionProjectionCodec.scala`.
  - `frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala` and `ActionDecisionRenderer.scala`.
  - `src/main/scala/oathdigital/gameplay/chronicle/OfferCitizenship.scala`.
  - `src/test/scala/oathdigital/testkit/ParkedNode.scala`.
  - Docs: the spec, `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`, the negotiation walker design, `implementation-traceability.md`, `docs/ROADMAP.md`.

---

### Task 1: Move the park queries out of `ProcedureWalker`

A refactor with no behaviour change, except that `awaitedPlayer` goes and `awaitedInSeatOrder` arrives. It frees room in `ProcedureWalker.scala` for Task 2.

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala`
- Modify: `src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala` (the block from `parkedRoll` to `awaitedPlayers`, about lines 162-240)
- Modify: `src/main/scala/oathdigital/application/WalkerDecisionProjector.scala:71-75`
- Modify: `src/test/scala/oathdigital/testkit/ParkedNode.scala:69`, `src/test/scala/oathdigital/gameplay/CoOwnedDecideSuite.scala`, `src/test/scala/oathdigital/gameplay/OathRulesWalkerPowerSuite.scala` (every `awaitedPlayer(` call)

**Interfaces:**
- Produces:
  - `ProcedureWalker.parkedRoll`, `openDecisions`, `parkedDecide` and `awaitedPlayers`, with unchanged signatures, now forwarding to `ParkedQueries`.
  - `ProcedureWalker.awaitedInSeatOrder(state: ReadyGame, action: Operation, pending: PendingTree, powers: WalkerPowers): Vector[PlayerId]`.
  - `ProcedureWalker.awaitedPlayer` is deleted.

- [ ] **Step 1: Write the failing test.** In `CoOwnedDecideSuite`, replace the `awaitedPlayer` assertion in "the park names every owner and one open decision" with:

```scala
    assertEquals(ProcedureWalker.awaitedInSeatOrder(ready, loop, pending,
      noPowers), ready.game.current.players.map(_.player)
      .filter(Set(owner, guest)))
```

- [ ] **Step 2: Run it to see it fail.**

Run: `./sbtw "testOnly oathdigital.gameplay.CoOwnedDecideSuite"`
Expected: compile error, `awaitedInSeatOrder` is not a member of `ProcedureWalker`.

- [ ] **Step 3: Create `ParkedQueries.scala`.** Move the bodies and full doc comments of `parkedRoll`, `openDecisions`, `parkedDecide` and `awaitedPlayers` out of `ProcedureWalker` unchanged, except:
  - `awaitedPlayers` no longer calls `awaitedPlayer`, which is deleted;
  - `awaitedInSeatOrder` is new.

```scala
package oathdigital.gameplay.walker

import oathdigital.model._

/** What a parked walk waits on, read off the rebuilt tree: the open
  * decisions, a parked roll, and who may answer. Split out of
  * [[ProcedureWalker]] to keep that file under the project's line bound;
  * `ProcedureWalker` forwards to it. */
private[walker] object ParkedQueries:
  // parkedRoll, openDecisions, parkedDecide, awaitedPlayers: moved verbatim,
  // with their doc comments.

  /** [[awaitedPlayers]] in seat order, for a projection that names them. */
  def awaitedInSeatOrder(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Vector[PlayerId] =
    val awaited = awaitedPlayers(state, action, pending, powers)
    state.game.current.players.map(_.player).filter(awaited)
```

In `ProcedureWalker`, replace the moved block with one-line-doc forwarders:

```scala
  /** See [[ParkedQueries.parkedRoll]]. */
  def parkedRoll(state: ReadyGame, action: Operation, pending: PendingTree,
      powers: WalkerPowers): Option[(PoolKey, Int)] =
    ParkedQueries.parkedRoll(state, action, pending, powers)

  /** See [[ParkedQueries.openDecisions]]. */
  def openDecisions(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Vector[Decide] =
    ParkedQueries.openDecisions(state, action, pending, powers, probing)

  /** See [[ParkedQueries.parkedDecide]]. */
  def parkedDecide(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Option[Decide] =
    ParkedQueries.parkedDecide(state, action, pending, powers, probing)

  /** See [[ParkedQueries.awaitedPlayers]]. */
  def awaitedPlayers(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Set[PlayerId] =
    ParkedQueries.awaitedPlayers(state, action, pending, powers)

  /** See [[ParkedQueries.awaitedInSeatOrder]]. */
  def awaitedInSeatOrder(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers): Vector[PlayerId] =
    ParkedQueries.awaitedInSeatOrder(state, action, pending, powers)
```

  - `ParkedQueries.parkedRoll` needs `WalkerPowerGather.leafAt` and `WalkerRolls.poolCount`.
  - `awaitedPlayers` reads `state.game.current.walkActor` for a Roll park.
  - Keep the imports that each file still uses, and drop the rest.

- [ ] **Step 4: Replace the other `awaitedPlayer` calls.**
  - `WalkerDecisionProjector.parkedPosition`, lines 71-75: keep the shape, but read the primary player as `awaited <- ProcedureWalker.awaitedInSeatOrder(context.ready, tree, pending, powers).headOption`. Task 4 reworks this.
  - `ParkedNode.of`, line 69: `awaiting = ProcedureWalker.awaitedInSeatOrder(ready, tree, pending, powers).headOption`.
  - `OathRulesWalkerPowerSuite`: replace each `awaitedPlayer(...)` with `awaitedInSeatOrder(...).headOption`.
  - The `awaitedPlayer` doc reference at `WalkerDecisionProjector.scala:758` becomes `[[ProcedureWalker.awaitedInSeatOrder]]`.

  In a co-owned Negotiation, the primary player was the deal's actor. The first player in seat order may be someone else. Task 4 removes the primary player, so only a test that compared it would notice. Fix any such test to compare the seat-order head.

- [ ] **Step 5: Run the tests.**

Run: `./sbtw "testOnly oathdigital.gameplay.*Walker* oathdigital.gameplay.CoOwnedDecideSuite oathdigital.application.WalkerDecisionProjectorSuite oathdigital.application.Negotiation*"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: no findings. `ProcedureWalker.scala` is under 750 lines.

- [ ] **Step 6: Commit.**

```bash
git add src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala \
  src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala \
  src/main/scala/oathdigital/application/WalkerDecisionProjector.scala \
  src/test/scala/oathdigital/testkit/ParkedNode.scala \
  src/test/scala/oathdigital/gameplay/CoOwnedDecideSuite.scala \
  src/test/scala/oathdigital/gameplay/OathRulesWalkerPowerSuite.scala
git commit -m "refactor(walker): move the park queries out of ProcedureWalker

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The `Simultaneous` node

**Files:**
- Modify: `src/main/scala/oathdigital/model/Operation.scala` (after `Branch`)
- Create: `src/main/scala/oathdigital/gameplay/walker/SimultaneousChildren.scala`
- Modify: `src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala` (`walk` dispatch at about line 293; a new `walkSimultaneous`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala` (`leafAt`, about line 526; a new `leavesAt`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala` (`openDecisions`, `parkedDecide`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerReplay.scala:72-76`
- Test: `src/test/scala/oathdigital/gameplay/SimultaneousDecideSuite.scala`

**Interfaces:**
- Consumes: the Task 1 forwarders and `awaitedInSeatOrder`.
- Produces:
  - `final case class Simultaneous(children: Vector[Operation]) extends Operation`, in package `oathdigital.model`;
  - `SimultaneousChildren.decideOf(child: Operation): Option[Decide]`;
  - `SimultaneousChildren.open(node: Simultaneous, answered: Vector[Answered]): Vector[(Int, Decide)]`;
  - `WalkerPowerGather.leavesAt(state, action, pending, powers): Vector[Operation]`;
  - `ProcedureWalker.openDecisions`, which returns every open child;
  - `ProcedureWalker.parkedDecide`, which is `None` when more than one decision is open.

- [ ] **Step 1: Write the failing tests.** Create `SimultaneousDecideSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.walker.{ProcedureWalker, WalkerOutcome, WalkerPowers}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.TestGameFixtures._

/** Two decisions open at once, answered in any order: the `Simultaneous`
  * node (simultaneous decisions design), exercised with a synthetic tree.
  * Each child's tail adds one Imperial warband to the bank, so a test can
  * see which tails ran. */
class SimultaneousDecideSuite extends munit.FunSuite:
  private val red = playerId
  private val blue = PlayerId("player-blue")
  private val noPowers = WalkerPowers.empty
  private val yes = DecisionOptionRef.Button("yes")
  private val ready: ReadyGame = ReadyGames.of(game)

  private def ask(id: String, owner: PlayerId, heading: String = "Answer") =
    Decide(id, owner, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(yes, "Yes"),
      DecisionOption.Button(DecisionOptionRef.Button("no"), "No")),
      heading = Some(heading)))

  private val redAsk = ask("sim.red", red)
  private val blueAsk = ask("sim.blue", blue)
  private val addsWarband = BuildOps((_, _) =>
    Right(Vector[CoreOperation](WarbandsAdded(ForceKind.Imperial, 1))))
  private val afterwards = ask("sim.after", red, "Afterwards")

  /** Both children, then a decision after the node: the node is child 0. */
  private def tree(children: Operation*): Operation =
    Sequence(Simultaneous(children.toVector), afterwards)

  private val both = tree(Sequence(redAsk, addsWarband),
    Sequence(blueAsk, addsWarband))

  private def answer(decide: Decide, by: PlayerId): Answered =
    Answered(decide.decisionId, ChooseOneAnswer(yes), by)

  private def parkedAt(outcome: Either[OathViolation, WalkerOutcome])
      (using munit.Location): (PendingTree, ReadyGame) = outcome match
    case Right(WalkerOutcome.Parked(pending, events)) =>
      val after = events.foldLeft(ready)((state, event) =>
        ProcedureWalker.applyRecorded(OathState.Ready(state), event)
          .toOption.collect { case OathState.Ready(next) => next }
          .getOrElse(fail(s"could not apply $event")))
      (pending, after)
    case other => fail(s"expected a park, got $other")

  private def imperialBank(state: ReadyGame): Int =
    state.banks.warbandSupply.getOrElse(ForceKind.Imperial, 0)

  test("the node parks at its own path with every child open"):
    val (pending, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    assertEquals(pending.at, Vector("0"))
    assertEquals(ProcedureWalker.openDecisions(ready, both, pending,
      noPowers), Vector(redAsk, blueAsk))
    assertEquals(ProcedureWalker.awaitedPlayers(ready, both, pending,
      noPowers), Set(red, blue))
    assertEquals(ProcedureWalker.parkedDecide(ready, both, pending,
      noPowers), None)

  test("awaitedInSeatOrder follows the seats, not the children"):
    val reversed = tree(Sequence(blueAsk, addsWarband),
      Sequence(redAsk, addsWarband))
    val (pending, _) = parkedAt(ProcedureWalker.advance(ready, reversed,
      None, noPowers))
    assertEquals(ProcedureWalker.awaitedInSeatOrder(ready, reversed, pending,
      noPowers), ready.game.current.players.map(_.player)
      .filter(Set(red, blue)))

  test("children are answered in any order; each tail runs at its answer"):
    val (first, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    val (second, afterBlue) = parkedAt(ProcedureWalker.resolve(ready, both,
      first, answer(blueAsk, blue), noPowers))
    assertEquals(second.at, Vector("0"))
    assertEquals(imperialBank(afterBlue), imperialBank(ready) + 1)
    assertEquals(ProcedureWalker.openDecisions(afterBlue, both, second,
      noPowers), Vector(redAsk))
    assertEquals(ProcedureWalker.parkedDecide(afterBlue, both, second,
      noPowers), Some(redAsk))
    val (third, afterRed) = parkedAt(ProcedureWalker.resolve(afterBlue, both,
      second, answer(redAsk, red), noPowers))
    // The node completed, and the walk went on to the decision after it.
    assertEquals(third.at, Vector("1"))
    assertEquals(imperialBank(afterRed), imperialBank(ready) + 2)
    assertEquals(third.answered.map(_.decisionId),
      Vector("sim.blue", "sim.red"))

  test("a plain advance at the node re-parks it"):
    val (pending, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    val (again, _) = parkedAt(ProcedureWalker.advance(ready, both,
      Some(pending), noPowers))
    assertEquals(again, pending)

  test("a child answered twice, or an unknown id, is rejected"):
    val (first, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    val (second, afterBlue) = parkedAt(ProcedureWalker.resolve(ready, both,
      first, answer(blueAsk, blue), noPowers))
    assert(ProcedureWalker.resolve(afterBlue, both, second,
      answer(blueAsk, blue), noPowers).isLeft)
    assert(ProcedureWalker.resolve(ready, both, first,
      Answered("sim.unknown", ChooseOneAnswer(yes), red), noPowers).isLeft)

  test("a player who does not own the child is rejected"):
    val (first, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    assertEquals(ProcedureWalker.resolve(ready, both, first,
      answer(redAsk, blue), noPowers),
      Left(OathViolation.WrongPlayer(red, blue)))

  test("a child whose tail parks is rejected"):
    val parking = tree(Sequence(redAsk, ask("sim.more", red)),
      Sequence(blueAsk, addsWarband))
    val (first, _) = parkedAt(ProcedureWalker.advance(ready, parking, None,
      noPowers))
    assert(ProcedureWalker.resolve(ready, parking, first,
      answer(redAsk, red), noPowers).isLeft)

  test("a malformed node is rejected when the walk reaches it"):
    val malformed = Vector(
      "no children" -> tree(),
      "a child with no Decide first" -> tree(Sequence(addsWarband, redAsk)),
      "a shared decision id" -> tree(redAsk, blueAsk.copy(decisionId =
        redAsk.decisionId)),
      "a shared owner" -> tree(redAsk, blueAsk.copy(owner = red)),
      "co-owners" -> tree(redAsk.copy(coOwners = Vector(blue)), blueAsk),
      "a windowed Decide" -> tree(redAsk.copy(window =
        Some(PowerWindow.PayOrAsk)), blueAsk),
      "differing headings" -> tree(redAsk, ask("sim.blue", blue, "Other")))
    malformed.foreach((label, operation) => assert(
      ProcedureWalker.advance(ready, operation, None, noPowers).isLeft,
      label))
```

  Check these names against the code before you run the suite:
  - `ProcedureWalker.applyRecorded`, `OathState.Ready`, `ReadyGame.banks.warbandSupply` and `OathViolation.WrongPlayer`.
  - Fix import paths if the compiler disagrees: `WalkerOutcome` and `ProcedureWalker` are in `oathdigital.gameplay.walker`.

- [ ] **Step 2: Run it to see it fail.**

Run: `./sbtw "testOnly oathdigital.gameplay.SimultaneousDecideSuite"`
Expected: compile error, `Simultaneous` not found.

- [ ] **Step 3: Add the node to `Operation.scala`**, after `Branch`:

```scala
/** Opens one decision per child at once (simultaneous decisions design).
  * Each child is a `Decide`, or a `Sequence` that starts with one; each
  * decision has its own owner, no co-owners and no window, and all share
  * one heading. The players answer in any order: a child's tail runs right
  * after its answer and must not park. The node parks at its own path and
  * completes once every child is answered.
  *
  * A child counts as answered when the walk's answers hold its decision id,
  * so a node inside a `Repeat` needs fresh ids on each pass. */
final case class Simultaneous(override val children: Vector[Operation])
    extends Operation
```

- [ ] **Step 4: Create `SimultaneousChildren.scala`.**

```scala
package oathdigital.gameplay.walker

import oathdigital.model._

/** The children of a [[Simultaneous]] node: each is a `Decide`, or a
  * `Sequence` that starts with one, and a child is open until the walk's
  * answers hold its decision id. */
private[walker] object SimultaneousChildren:
  /** The decision `child` opens with. */
  def decideOf(child: Operation): Option[Decide] = child match
    case decide: Decide => Some(decide)
    case sequence: Sequence if sequence.window.isEmpty =>
      sequence.children.headOption.collect { case decide: Decide => decide }
    case _ => None

  /** The resume cursor from `child` down to its decision. */
  def entry(child: Operation): Vector[String] = child match
    case _: Decide => Vector.empty
    case _ => Vector("0")

  /** Why `node` cannot be walked, or `None` when it can. */
  def malformed(node: Simultaneous): Option[String] =
    val decides = node.children.map(decideOf)
    val found = decides.flatten
    if node.children.isEmpty then Some("it has no children")
    else if decides.contains(None) then
      Some("a child does not start with a Decide")
    else if found.exists(_.window.nonEmpty) then
      Some("a child's Decide has a window")
    else if found.exists(_.coOwners.nonEmpty) then
      Some("a child's Decide has co-owners")
    else if found.map(_.decisionId).distinct.size < found.size then
      Some("two children share a decision id")
    else if found.map(_.owner).distinct.size < found.size then
      Some("two children share an owner")
    else if found.map(_.query.heading).distinct.size > 1 then
      Some("the children's headings differ")
    else None

  /** The open children, each with its index and decision, in child order. */
  def open(node: Simultaneous, answered: Vector[Answered])
      : Vector[(Int, Decide)] =
    val ids = answered.map(_.decisionId).toSet
    node.children.zipWithIndex.flatMap((child, index) =>
      decideOf(child).filterNot(decide => ids(decide.decisionId))
        .map(index -> _))
```

- [ ] **Step 5: Walk the node in `ProcedureWalker`.** In `walk`, add an arm before `case leaf: LeafOperation`:

```scala
      case node: Simultaneous =>
        walkSimultaneous(node, ctx, path, cursor, resume, hooks)
```

Add after `walkBranch`:

```scala
  /** A [[Simultaneous]] node. Fresh, it parks at its own path with every
    * child open; a search walks its children in order instead. An answer
    * resumes at the node: it answers the child whose decision it names,
    * runs that child's tail, and parks again while a child is open. A
    * cursor into a child, which only a search's verdict sends, walks on as
    * a `Sequence` would. */
  private def walkSimultaneous(node: Simultaneous, ctx: WalkCtx,
      path: Vector[String], cursor: Option[Vector[String]], resume: Resume,
      hooks: WalkerHooks): Either[OathViolation, Step] =
    val at = s"the Simultaneous node at ${path.mkString(".")}"
    SimultaneousChildren.malformed(node) match
      case Some(reason) =>
        Left(OathViolation.InvalidEventOrder(s"$at is malformed: $reason"))
      case None => cursor match
        case None if ctx.searching =>
          continue(node.children, 0, ctx, path, None, resume, hooks)
        case None => Right(Park(path, ctx))
        case Some(rest) if rest.nonEmpty =>
          walkChildren(node.children, ctx, path, cursor, resume, hooks)
        case Some(_) => resume match
          case PlainResume => Right(Park(path, ctx))
          case RollResume(_) => Left(OathViolation.InvalidEventOrder(
            s"roll() resumed at $at; expected a Roll"))
          case AnswerResume(answer) =>
            SimultaneousChildren.open(node, ctx.answered)
                .find(_._2.decisionId == answer.decisionId) match
              case None => Left(OathViolation.InvalidEventOrder(
                s"resolve() answer ${answer.decisionId} is not open at $at"))
              case Some((index, _)) =>
                val child = node.children(index)
                walk(child, ctx, path :+ index.toString,
                    Some(SimultaneousChildren.entry(child)), resume, hooks)
                  .flatMap:
                    case Done(next) =>
                      answeredOne(node, next, path, resume, hooks)
                    case Park(_, _) => Left(OathViolation.InvalidEventOrder(
                      s"a child's tail parked inside $at"))
                    case other => Right(other)

  /** After one child's answer: done when no child is open, a park at the
    * node while one is, or, in a search, the open children in order. */
  private def answeredOne(node: Simultaneous, next: WalkCtx,
      path: Vector[String], resume: Resume, hooks: WalkerHooks)
      : Either[OathViolation, Step] =
    val open = SimultaneousChildren.open(node, next.answered).map(_._1)
    if open.isEmpty then Right(Done(next))
    else if !next.searching then Right(Park(path, next))
    else open.foldLeft[Either[OathViolation, Step]](Right(Done(next))):
      case (Right(Done(at)), index) => walk(node.children(index), at,
        path :+ index.toString, None, resume, hooks)
      case (stop, _) => stop
```

- [ ] **Step 6: Open every child at the park.** In `WalkerPowerGather`, refactor `leafAt` and add `leavesAt` beside it:

```scala
  def leafAt(state: ReadyGame, action: Operation, pending: PendingTree,
      powers: WalkerPowers): Option[Operation] =
    val picked = powers.withAnswers(pending.answered)
    resolveAt(state, pending, picked, action, pending.at, Vector.empty,
      Set.empty).map(narrowedAt(state, action, pending, picked))

  /** Every leaf open at the park: the open children's decisions of a
    * [[Simultaneous]] node parked at its own path, or else the one leaf
    * [[leafAt]] returns. Each `Decide` is narrowed as [[leafAt]] narrows. */
  def leavesAt(state: ReadyGame, action: Operation, pending: PendingTree,
      powers: WalkerPowers): Vector[Operation] =
    val picked = powers.withAnswers(pending.answered)
    resolveAt(state, pending, picked, action, pending.at, Vector.empty,
        Set.empty) match
      case Some(node: Simultaneous) =>
        SimultaneousChildren.open(node, pending.answered).map((_, decide) =>
          narrowedAt(state, action, pending, picked)(decide))
      case found => found.map(narrowedAt(state, action, pending, picked))
        .toVector

  private def narrowedAt(state: ReadyGame, action: Operation,
      pending: PendingTree, picked: WalkerPowers)(node: Operation): Operation =
    node match
      case decide: Decide => ProcedureWalker.narrowParked(state, action,
        pending, picked, decide)
      case other => other
```

Extend `leafAt`'s doc comment with one line: "A `Simultaneous` park resolves to the node itself; [[leavesAt]] opens its children."

In `ParkedQueries`:

```scala
  def openDecisions(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Vector[Decide] =
    val looked = if probing then powers else powers.quiet
    WalkerPowerGather.leavesAt(state, action, pending, looked).collect {
      case decide: Decide => decide
    }

  def parkedDecide(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers,
      probing: Boolean = true): Option[Decide] =
    openDecisions(state, action, pending, powers, probing) match
      case Vector(one) => Some(one)
      case _ => None
```

Update `parkedDecide`'s doc comment: "The one open decision, or `None` at a Roll park or when several decisions are open (a `Simultaneous` park): a caller that needs one of several looks it up by decision id or owner in [[openDecisions]]." Drop the "A future `Simultaneous` node would…" sentence from `openDecisions`'s doc comment, and say instead that a `Simultaneous` park returns one decision per open child.

- [ ] **Step 7: Let replay accept a step under the parked path.** In `WalkerReplay.validateParkedStep`, replace the equality check:

```scala
      at = pending.at.mkString(".")
      // A `Simultaneous` node parks at its own path, and the answer step
      // records the path of the child decision it answered.
      _ <- Either.cond(step.nodeId == at || step.nodeId.startsWith(s"$at."),
        (), OathViolation.InvalidEventOrder(
          s"walker step ${step.nodeId} does not match pending position $at"))
```

- [ ] **Step 8: Run the tests.**

Run: `./sbtw "testOnly oathdigital.gameplay.SimultaneousDecideSuite oathdigital.gameplay.CoOwnedDecideSuite oathdigital.gameplay.ProcedureWalker*"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: no findings.

- [ ] **Step 9: Commit.**

```bash
git add src/main/scala/oathdigital/model/Operation.scala \
  src/main/scala/oathdigital/gameplay/walker/SimultaneousChildren.scala \
  src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala \
  src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerReplay.scala \
  src/test/scala/oathdigital/gameplay/SimultaneousDecideSuite.scala
git commit -m "feat(walker): add the Simultaneous node

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Callers look up the decision they need

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala` and the `ProcedureWalker` forwarders (add `openDecision`)
- Modify: `src/main/scala/oathdigital/gameplay/OathRulesWalker.scala` (`consentAsk` at about line 189, `grantAnswers` at about 209, `checkAnswerable` at about 479)
- Modify: `src/main/scala/oathdigital/gameplay/walker/PricedOptions.scala:30`
- Modify: `src/main/scala/oathdigital/application/WalkerDecisionProjector.scala` (`parkedDecide(ready)` at about line 83)
- Modify: `src/main/scala/oathdigital/application/ConsentProjector.scala:88`
- Modify: `src/main/scala/oathdigital/application/gamelog/ChoiceWords.scala:60`
- Modify: `src/test/scala/oathdigital/testkit/ParkedNode.scala`
- Test: `src/test/scala/oathdigital/gameplay/SimultaneousDecideSuite.scala`

**Interfaces:**
- Consumes: Task 2's `openDecisions` and `parkedDecide`.
- Produces:
  - `ProcedureWalker.openDecision(state, action, pending, powers, decisionId: String, probing: Boolean = true): Option[Decide]`;
  - `WalkerDecisionProjector.openDecisions(ready: ReadyGame): Vector[Decide]`, which replaces `parkedDecide(ready)`;
  - `ParkedNode.of`, which reports the first open decision in its owner's seat order.

- [ ] **Step 1: Write the failing test.** Add to `SimultaneousDecideSuite`:

```scala
  test("openDecision finds a child by its decision id"):
    val (pending, _) = parkedAt(ProcedureWalker.advance(ready, both, None,
      noPowers))
    assertEquals(ProcedureWalker.openDecision(ready, both, pending, noPowers,
      blueAsk.decisionId), Some(blueAsk))
    assertEquals(ProcedureWalker.openDecision(ready, both, pending, noPowers,
      "sim.after"), None)
```

- [ ] **Step 2: Run it to see it fail.**

Run: `./sbtw "testOnly oathdigital.gameplay.SimultaneousDecideSuite"`
Expected: compile error, `openDecision` is not a member.

- [ ] **Step 3: Add `openDecision`.** In `ParkedQueries`:

```scala
  /** The open decision `decisionId` names, at any park. */
  def openDecision(state: ReadyGame, action: Operation, pending: PendingTree,
      powers: WalkerPowers, decisionId: String,
      probing: Boolean = true): Option[Decide] =
    openDecisions(state, action, pending, powers, probing)
      .find(_.decisionId == decisionId)
```

In `ProcedureWalker`, add a forwarder in the Task 1 style.

- [ ] **Step 4: Move the callers.**
  - **`OathRulesWalker.consentAsk` and `grantAnswers`.** Replace `ProcedureWalker.parkedDecide(ready, tree, pending, powers)` with `ProcedureWalker.openDecision(ready, tree, pending, powers, decisionId)`. Keep the `owner == requester` filter. Drop the now-redundant `found.decisionId == decisionId` and `decide.decisionId == decisionId` tests.
  - **`OathRulesWalker.checkAnswerable`.** Replace the `parkedDecide(...) match { case Some(_) => …; case None => … }` with `if ProcedureWalker.openDecisions(ready, tree, pending, powers).nonEmpty then Right(()) else Left(…)`. Keep the same `Left`.
  - **`PricedOptions.priced`, line 30.** Replace `ProcedureWalker.parkedDecide(state, tree, pending, powers, probing = false)` with `ProcedureWalker.openDecision(state, tree, pending, powers, decide.decisionId, probing = false)`.
  - **`WalkerDecisionProjector.parkedDecide(ready)`.** Rename it `openDecisions(ready): Vector[Decide]`. It returns `ProcedureWalker.openDecisions(…, probing = false)`, with the same rebuild, or `Vector.empty`. Keep the doc comment, reworded for "every open decision".
  - **`ConsentProjector.target`.** Use `decisions.openDecisions(context.ready).filter(_.decisionId == request.decisionId)`.
  - **`ChoiceWords.shown`.** Use `decisions.openDecisions(before).flatMap(_.query match …)`. The labels of every open decision are searched. At a `Simultaneous` park they are the same buttons.
  - **`ParkedNode.of`.** Replace `ProcedureWalker.parkedDecide(ready, tree, pending, powers)` with the first open decision in seat order:

```scala
              seats = ready.game.current.players.map(_.player)
              first = ProcedureWalker.openDecisions(ready, tree, pending,
                  powers).sortBy(decide => seats.indexOf(decide.owner))
                .headOption
```

    Match on `first` where the code matched on the `parkedDecide` result. Report `decide.owner` as the awaited player for a `Decision`. For a `Roll`, keep the `awaiting` value from Task 1. Add one line to the doc comment: "At a `Simultaneous` park it reports the first open decision in seat order, so a driver answers the children in seat order."

  - **Unchanged (ruling P7).** Leave `WalkerSimulation`, `ModifierChoice` and `OathRulesWalker.parkedPlanDecision` as they are.

- [ ] **Step 5: Run the tests.**

Run: `./sbtw "testOnly oathdigital.gameplay.* oathdigital.application.*Consent* oathdigital.application.gamelog.*"`
Expected: PASS. The golden logs are unchanged.

- [ ] **Step 6: Commit.**

```bash
git add src/main/scala/oathdigital/gameplay/walker/ParkedQueries.scala \
  src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala \
  src/main/scala/oathdigital/gameplay/OathRulesWalker.scala \
  src/main/scala/oathdigital/gameplay/walker/PricedOptions.scala \
  src/main/scala/oathdigital/application/WalkerDecisionProjector.scala \
  src/main/scala/oathdigital/application/ConsentProjector.scala \
  src/main/scala/oathdigital/application/gamelog/ChoiceWords.scala \
  src/test/scala/oathdigital/testkit/ParkedNode.scala \
  src/test/scala/oathdigital/gameplay/SimultaneousDecideSuite.scala
git commit -m "feat(walker): look up open decisions by id where several may be open

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Project several open decisions, and name every awaited player

**Files:**
- Modify: `shared/src/main/scala/oathdigital/protocol/projection/ActionProjectionDtos.scala:291-301`
- Modify: `shared/src/main/scala/oathdigital/protocol/projection/ActionProjectionCodec.scala:60-75`
- Modify: `src/main/scala/oathdigital/application/WalkerDecisionProjector.scala` (`parkedPosition`, `project`, `waiting`, `parked`, and `Parked` at about line 770)
- Modify: `frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala:272-320`
- Modify: `frontend/src/main/scala/oathdigital/frontend/ActionDecisionRenderer.scala:5-52`
- Modify every test that builds or reads `WalkerWaitingProjection` or `ViewerPresentation`. Find them with:
  - `grep -rn "WalkerWaitingProjection(\|WalkerWaitingState(\|coOwnerPlayerIds\|walkerWaiting.map(_.playerId)\|waitingForPlayerId\|waitingForDisplayName" src/test shared/src/test frontend/src/test`
- Test: `shared/src/test/scala/oathdigital/protocol/ProjectionProtocolSuite.scala`, `src/test/scala/oathdigital/application/WalkerDecisionProjectorSuite.scala`, `frontend/src/test/scala/oathdigital/frontend/ViewerPresentationSuite.scala`

**Interfaces:**
- Consumes: Task 1's `awaitedInSeatOrder`; Task 2's `openDecisions`.
- Produces:
  - `WalkerWaitingProjection(playerIds: Vector[String], heading: Option[String] = None, deal: Option[NegotiationDealProjection] = None, status: Vector[WalkerStatusProjection] = Vector.empty)`. Its wire keys are `playerIds`, `heading`, `deal` and `status`.
  - `ViewerPresentation(showGameplayControls: Boolean, waitingForPlayerIds: Vector[String], waitingForDisplayNames: Vector[String], playerId: String)`.

- [ ] **Step 1: Write the failing tests.**
  - **`ProjectionProtocolSuite`:**

```scala
  test("walkerWaiting round-trips every awaited player in order"):
    val several = projection.copy(walkerWaiting = Some(
      WalkerWaitingProjection(Vector("red", "blue"),
        Some("The new Chancellor offers you Citizenship"))))
    assertEquals(roundTrip(several), Right(several))
```

    Use the suite's existing round-trip helper; check its name. Also convert the existing cases:
    - `WalkerWaitingProjection("blue", …)` becomes `WalkerWaitingProjection(Vector("blue"), …)`.
    - A case that passed `coOwnerPlayerIds = Vector(x)` lists `x` in `playerIds` after the primary player.

  - **`WalkerDecisionProjectorSuite`.** Model the setup on the suite's tests at about lines 433 and 469, which use a stubbed `TreeSource`. Add a test with a `TreeSource` that returns a `Simultaneous` of two choose-ones owned by two players:
    - each owner's `project` is that owner's own decision;
    - a third player's `waiting` has `playerIds` naming both owners in seat order, and the shared heading.
    - After one owner answers, that owner sees `waiting` naming the other owner only.

  - **`ViewerPresentationSuite`:**

```scala
  test("the waiting line names every awaited player"):
    val value = wonByBlue.copy(oathkeeper = None, walkerWaiting =
      Some(WalkerWaitingState(Vector("blue-exile", "red-exile"),
        Some("Answer"))))
    val presentation = viewerPresentation(value, "green-exile")
    assertEquals(presentation.waitingForPlayerIds,
      Vector("blue-exile", "red-exile"))
```

    Adjust the fixture names to the ones the suite defines, such as `wonByBlue` and the player ids.

- [ ] **Step 2: Run them to see them fail.**

Run: `./sbtw "testOnly *ProjectionProtocolSuite *WalkerDecisionProjectorSuite" "frontend/testOnly *ViewerPresentationSuite"`
Expected: compile errors on the new fields.

- [ ] **Step 3: Change the DTO and codec.**

```scala
/** Public: who a parked walker position waits on, every awaited player in
  * seat order, and the question's heading when it has one (`None` for a
  * roll). Every viewer who owes no answer receives this; a viewer who owes
  * one receives `WalkerDecisionProjection`. `status` is the facts the
  * awaited players see beside the question, so the table can follow along.
  */
final case class WalkerWaitingProjection(playerIds: Vector[String],
    heading: Option[String] = None,
    deal: Option[NegotiationDealProjection] = None,
    status: Vector[WalkerStatusProjection] = Vector.empty)
```

```scala
  def encodeWalkerWaiting(value: WalkerWaitingProjection): ujson.Value = ujson.Obj(
    "playerIds" -> encoded(value.playerIds)(ujson.Str(_)),
    "heading" -> stringOption(value.heading),
    "deal" -> option(value.deal)(encodeDeal),
    "status" -> encoded(value.status)(encodeStatusLine))
  def decodeWalkerWaiting(raw: ujson.Value, path: String): Result[WalkerWaitingProjection] = for
    value <- obj(raw, path)
    _ <- exact(value, Set("playerIds", "heading", "deal", "status"), path)
    playerIds <- strings(value, "playerIds", path)
    heading <- optionalString(value, "heading", path)
    deal <- optionalAbsent(value, "deal", path)(decodeDeal)
    statusRaws <- array(value, "status", path)
    status <- traverse(statusRaws, s"$path.status")(decodeStatusLine)
  yield WalkerWaitingProjection(playerIds, heading, deal, status)
```

- [ ] **Step 4: Rework `WalkerDecisionProjector`.**
  - **`Parked`.** Its last two fields become `awaited: Vector[PlayerId]`, in seat order. Update its doc comment to "every player who may answer, in seat order, read off it by [[ProcedureWalker.awaitedInSeatOrder]]".
  - **`parkedPosition`.** Compute `awaited = ProcedureWalker.awaitedInSeatOrder(context.ready, tree, pending, powers)`, guard with `if awaited.nonEmpty`, and yield `Parked(procedure, tree, pending, powers, awaited)`.
  - **`parked(...)`.** Replace the `awaited: PlayerId` parameter with `owner: PlayerId`, the player whose open decision is projected. Find the decision with `ProcedureWalker.openDecisions(ready, tree, pending, powers).find(_.owners.contains(owner))` instead of `parkedDecide`. Pass `decide.owner` to `rollOutcome`.
  - **`project`.** Guard `if context.viewer.exists(parked.awaited.contains)`. Call `this.parked(…, owner = viewer, viewer = context.viewer)`, where `viewer <- context.viewer`.
  - **`waiting`.** Guard `if !context.viewer.exists(parked.awaited.contains)`. Gate on the first awaited player's prompt: `this.parked(…, owner = parked.awaited.head, viewer = Some(parked.awaited.head), previewed = false)`. Then build:

```scala
  yield
    val open = ProcedureWalker.openDecisions(context.ready, parked.tree,
      parked.pending, parked.powers)
    WalkerWaitingProjection(parked.awaited.map(_.value),
      open.headOption.flatMap(_.query.heading),
      open.headOption.map(_.query).collect {
        case negotiate: DecisionQuery.Negotiate =>
          deals.project(context.ready, context.viewer, negotiate)
      },
      // Keep the existing Roll-park fallback in this argument.
      open.flatMap(found => status(parked.procedure, context.ready,
        found.decisionId, withRoll = true)).distinct)
```

  Keep any Roll-park branch the old `status` argument had. Read the current code first and carry its `None` decision case over unchanged.

  - **Doc comments.** In `project`, `waiting` and `parked`, replace "the awaited player" with "the players who may answer".

- [ ] **Step 5: The frontend.** In `ServerUiSupport`:
  - `ViewerPresentation` takes `waitingForPlayerIds: Vector[String]` and `waitingForDisplayNames: Vector[String]`.
  - Every construction passes `Vector.empty` where it passed `None`, and `Vector(x)` where it passed `Some(x)`.
  - The `walkerWaiting` branch passes `waiting.playerIds` and `waiting.playerIds.map(playerDisplayName(value, _))`.
  - Reword the Task 5 comment there: everyone else is projected `walkerWaiting`, naming every player who may answer.

  In `ActionDecisionRenderer.status`, keep the file's one-space indentation and replace the `presentation.waitingForPlayerId match` with:

```scala
   val waitingFor = presentation.waitingForPlayerIds
   if waitingFor.nonEmpty then
     node.appendChild(dom.document.createTextNode("Waiting for "))
     waitingFor.zipWithIndex.foreach((playerId, index) =>
       if index > 0 then node.appendChild(dom.document.createTextNode(", "))
       node.appendChild(playerReference(value, playerId)))
     // The question a parked decision asks rides on the same line, so the
     // pane names the players waited on once.
     value.walkerWaiting.filter(_.playerIds == waitingFor).flatMap(_.heading)
       .foreach(heading =>
         node.appendChild(dom.document.createTextNode(s": $heading")))
   else if value.walkerDecision.nonEmpty then
     // (keep the existing comment and body of this case)
   else if value.phase == "game-over" then
     // (keep the existing body)
   else
     // (keep the existing body)
```

  In `actionsPanel`, use `value.walkerWaiting.filter(_.playerIds == presentation.waitingForPlayerIds)`.

- [ ] **Step 6: Update the other tests.** Run the grep in **Files**, and convert each hit:
  - `WalkerWaitingProjection("x", …)` becomes `WalkerWaitingProjection(Vector("x"), …)`.
  - `.walkerWaiting.map(_.playerId)` with `Some(x)` becomes `.walkerWaiting.map(_.playerIds)` with `Some(Vector(x))`.
  - `waitingForPlayerId` with `Some(x)` or `None` becomes `waitingForPlayerIds` with `Vector(x)` or `Vector.empty`. Do the same for display names.
  - `NegotiationDealProjectionSuite`, line 84: `coOwnerPlayerIds == Vector(p2)` becomes `playerIds` == the deal's owners in seat order.
  - `FacedownAdviserPickServiceSuite`, line 41: convert as above.

- [ ] **Step 7: Run the tests.**

Run: `./sbtw test frontend/test`
Expected: PASS.

- [ ] **Step 8: Commit.** Add the main files listed in **Files**, plus each test file the grep found, by name:

```bash
git add shared/src/main/scala/oathdigital/protocol/projection/ActionProjectionDtos.scala \
  shared/src/main/scala/oathdigital/protocol/projection/ActionProjectionCodec.scala \
  src/main/scala/oathdigital/application/WalkerDecisionProjector.scala \
  frontend/src/main/scala/oathdigital/frontend/ServerUiSupport.scala \
  frontend/src/main/scala/oathdigital/frontend/ActionDecisionRenderer.scala
# then: git add <each converted test file>
git commit -m "feat(projection): name every awaited player while decisions are open

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The Throne's offered Exiles answer at once, and docs

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/chronicle/OfferCitizenship.scala` (the object comment and `answers`)
- Test: `src/test/scala/oathdigital/gameplay/chronicle/OfferCitizenshipSuite.scala`
- Modify docs:
  - `docs/superpowers/specs/2026-11-02-simultaneous-decisions-design.md`
  - `docs/rules/ambiguities.md` (Chronicle ruling 21)
  - `docs/rules/chronicle.md` (task 2, step 1, the Digital line)
  - `docs/superpowers/specs/2026-09-19-negotiation-walker-design.md` ("Simultaneous decisions")
  - `docs/implementation-traceability.md` (find it with `find docs -name implementation-traceability.md`)
  - `docs/ROADMAP.md`

**Interfaces:**
- Consumes: Tasks 2-4. `ProcedureWalker.openDecisions` and `ParkedNode`.

- [ ] **Step 1: Write the failing tests.** In `OfferCitizenshipSuite`, replace the test "each offered Exile answers in seat order, Decline first" with:

```scala
  test("the offered Exiles answer at once, Decline first"):
    val asked = offered(throneStarted(exilesHoldSites), p3, p2)
    val open = openAnswers(asked)
    assertEquals(open.map(decide => (decide.decisionId, decide.owner)),
      Vector(OfferCitizenship.answerDecisionId(p2) -> p2,
        OfferCitizenship.answerDecisionId(p3) -> p3))
    open.foreach(_.query match
      case one: DecisionQuery.ChooseOne => assertEquals(
        one.options.map(_.ref).head, OfferCitizenship.decline)
      case other => fail(other.toString))

  test("a later Exile may answer first, and each answer applies at once"):
    val asked = offered(throneStarted(exilesHoldSites), p2, p3)
    val third = replied(asked, p3, OfferCitizenship.accept)
    assertEquals(Imperials.role(ready(third), p3), Some(Role.Citizen))
    assertEquals(openAnswers(third).map(_.owner), Vector(p2))
    val done = replied(third, p2, OfferCitizenship.decline)
    assertEquals(lines(done.events), Vector("p1 offered Citizenship to p2, p3",
      "p3 accepted Citizenship", "p2 declined Citizenship"))
    val replayed = done.events.foldLeft[Either[OathViolation, OathState]](
      Right(exilesHoldSites.state))((state, event) =>
      state.flatMap(rules.evolve(_, event)))
    assertEquals(replayed.map(Outcomes.ready), Right(ready(done)))
```

Add the helper:

```scala
  /** The Throne answers still open, in seat order. */
  private def openAnswers(step: OathTransition)(using munit.Location)
      : Vector[Decide] =
    ParkedNode.openDecisions(step.state, catalog,
      WalkerPowerCatalog.default(catalog)).filter(_.decisionId
      .startsWith("chronicle.throne.citizenship"))
```

Add `ParkedNode.openDecisions` to `src/test/scala/oathdigital/testkit/ParkedNode.scala`. It rebuilds the tree as `of` does, and returns the open decisions sorted by their owners' seat order. If no walker is pending, it returns `Vector.empty`:

```scala
  /** Every open decision at the park, in their owners' seat order. */
  def openDecisions(state: OathState, catalog: ExecutableCatalog,
      walkerPowers: WalkerPowers = WalkerPowers.empty)
      : Vector[Decide] = state match
    case Ready(ready) =>
      val current = ready.game.current
      val seats = current.players.map(_.player)
      (for
        pending <- current.walkerPending
        procedure <- current.walkerProcedure
        powers = WalkerPowers.selected(walkerPowers, Vector.empty)
        tree <- WalkerProcedureRegistry.rebuild(procedure, catalog, ready,
          current.walkActor, current.walkerStartArgs, PhasePowers.empty,
          walkerPowers = powers).toOption
      yield ProcedureWalker.openDecisions(ready, tree, pending, powers)
        .sortBy(decide => seats.indexOf(decide.owner)))
        .getOrElse(Vector.empty)
    case _ => Vector.empty
```

Add the `ParkedNode` import to the suite.

- [ ] **Step 2: Run them to see them fail.**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.OfferCitizenshipSuite"`
Expected: FAIL. Only p2's decision is open, and p3's answer is rejected as not matching the parked Decide.

- [ ] **Step 3: Wrap the answers.** In `OfferCitizenship.answers`:

```scala
  private def answers(state: ReadyGame, holder: PlayerId,
      offeredTo: Vector[PlayerId]): Vector[Operation] =
    // Seat order of the players offered, read from the seats alone: an Exile
    // who accepts is no longer one, and the tree must keep its shape. The
    // answers are open at once; seat order is only the search's order.
    val ordered = ChronicleTasks.seated(state).filter(offeredTo.contains)
    if ordered.isEmpty then Vector.empty
    else Vector(Note(noteId, _ => Some(offered.sourceless(
        NoteArg.Player(holder), NoteArg.Players(ordered)))),
      Simultaneous(ordered.map(exile => new Sequence(answer(exile)))))
```

In the object's doc comment, replace "each offered Exile answers in seat order, Decline first" with "the offered Exiles answer at once, in any order, Decline first, and each answer applies when it is given". Do the same in the `Throne` description, if `ChronicleThroneProcedure` repeats it.

- [ ] **Step 4: Run the Chronicle and log suites.**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.* oathdigital.application.gamelog.*"`
Expected: PASS, and the `throne-offered` golden log is unchanged (ruling P6).

- [ ] **Step 5: Update the docs.**
  - **The spec.** Under "Rulings", add "Plan rulings (2026-11-02)" with P1-P8 from this plan, each as one or two sentences. Then edit the sections they change:
    - "The node": child Decides carry no window; answer steps record the child's path; replay accepts a step under the parked path.
    - "Callers": `parkedDecide` keeps its name, and `openDecision` is added.
    - "Tests": no new golden log.
    - Change the status line to "Status: designed, 2026-11-02. Implemented, 2026-11-02."
  - **`ambiguities.md`, Chronicle ruling 21.** Replace the seat-order bullet with: "The offered Exiles answer at once, in any order, with "Decline" first. Each answer applies when it is given and is public (user, 2026-11-02)."
  - **`chronicle.md`, task 2, step 1.** In the Digital line, replace "each answers in seat order, Decline first" with "the offered Exiles answer at once, in any order, Decline first".
  - **The negotiation walker design.** Rename the heading "Simultaneous decisions (not built, kept possible)" to "Simultaneous decisions". Add a first line: "Built: see the [simultaneous decisions design](../specs/2026-11-02-simultaneous-decisions-design.md)."
  - **`implementation-traceability.md`.** Add a row in the file's format for the Throne's Offer Citizenship answers:
    - code: `OfferCitizenship.answers`, `Simultaneous`, `ProcedureWalker.walkSimultaneous`;
    - tests: `SimultaneousDecideSuite`, `OfferCitizenshipSuite`.

    If a row for ruling 21 exists, update it instead.
  - **`ROADMAP.md`.** Delete the "Simultaneous Throne answers" item. If the "Phase - Empire" section is then empty of items, delete the section too, including its intro paragraph.
  - Search for other docs that still say the Throne answers in seat order, and fix each one: `grep -rn "seat order" docs/rules docs/architecture`.

- [ ] **Step 6: Run the gates.**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage ≥ 92%.

Run: `./sbtw frontend/test`
Expected: PASS.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: no findings.

- [ ] **Step 7: Commit.**

```bash
git add src/main/scala/oathdigital/gameplay/chronicle/OfferCitizenship.scala \
  src/test/scala/oathdigital/gameplay/chronicle/OfferCitizenshipSuite.scala \
  src/test/scala/oathdigital/testkit/ParkedNode.scala \
  docs/superpowers/specs/2026-11-02-simultaneous-decisions-design.md \
  docs/rules/ambiguities.md docs/rules/chronicle.md \
  docs/superpowers/specs/2026-09-19-negotiation-walker-design.md \
  docs/ROADMAP.md
# plus the traceability file, by its path
git commit -m "feat(chronicle): offered Exiles answer Citizenship at once

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
