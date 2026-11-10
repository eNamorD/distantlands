# Catalog Batch 11, Slice 8: Nested Follow-ups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an executed trigger's follow-up start follow-ups of its own,
so a Vision discarded inside False Prophet's claim is claimed in its turn.

**Architecture:** A batch that starts a follow-up holds its operations
under the key of its follow-up depth (`ExecutedTriggers.keyAt(depth)`);
depth 0 keeps today's key. `WalkCtx` replaces `following: Boolean` and
`followUp: Option[Operation]` with `followUps: Vector[Operation]` (its size
is the depth) and `triggersOff: Boolean` (a claimed burn or cost run whole,
and the take of a held batch). The walk, the resume check and the parked
lookup each read their own level's key, and the restriction look-ahead
folds every enclosing follow-up beside the action's tree. A chain deeper
than 8 levels is a contract violation.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md),
section "Slice 8: Nested follow-ups". Read "Slice 7" too, and the
[slice 7 plan](2026-11-09-catalog-batch-11-slice7.md)'s Known limits.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, no non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`).
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire changes need not keep old journals decodable. This slice changes no
  wire format: depth 0 keeps `walker.executed-triggers`.
- Log lines name the actor only when they are not the walk's actor.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Plan-time findings

1. **One key, one flag.** `ExecutedTriggers.key` is the only key
   (`walker/ExecutedTriggers.scala:19`). `Defer` adds to what a key holds
   (`model/CoreOperations.scala:709-722`), and `TakeDeferred` drops all of
   it. `ExecutedTriggers.held` holds nothing while `ctx.following`
   (`:45`).
2. **Three places ask "is anything held?"** with no level:
   `ProcedureWalker.runNarrowed` (`ProcedureWalker.scala:498`),
   `ProcedureWalker.triggered` (`:611`), and
   `WalkerPowerGather.resolveAt` (`WalkerPowerGather.scala:591`).
   `ExecutedTriggers.batch` rebuilds every trigger from the one key, from
   `walkTriggered` (`ProcedureWalker.scala:637`) and `followUpAt`
   (`WalkerPowerGather.scala:611`).
3. **`following` does two jobs.** `walkTriggered` sets it for the
   follow-ups and the take (`ProcedureWalker.scala:631-653`). The claimed
   burn or cost run whole sets it so that batch starts nothing
   (`:283-295`). Only the second job survives, as `triggersOff`; the take
   gets it too, because it ends a level and a follow-up after it could not
   be reached by a resume.
4. **Slice 7's `followUp`** (`WalkState.scala:36-45`) is one tree. Nested,
   a decision lies inside several, so it becomes a stack. `resolveAt`
   returns the follow-up it entered (`WalkerPowerGather.scala:563-614`);
   it returns the stack instead. `narrowParked` takes it
   (`ProcedureWalker.scala:465-472`).
5. **Every trigger stops on its own.** False Prophet's claim kills the
   warbands it reads; Pledge to Discord's follow-up buries Pledge
   (`cardplay/SuitOnlyAdvisers.scala:94-113`), and its `holderOf` then
   finds no holder; Brass Army's follow-up buries the relic or flips a
   secret and moves no pawn (`travel/BrassArmyMoves.scala:52-62`). So all
   triggers answer nested batches; the depth cap only stops a broken one.
   (Overturned while executing: nesting is opt-in. See Execution changes.)
6. **Pledge to Discord answers any batch.** Its `onExecuted` does not read
   `batch.executed`. With nesting it may now ask inside another trigger's
   follow-up rather than after it. Its outer ask then finds nothing
   waiting. The full suite shows whether any test pinned the old place.
7. **A test trigger loops.** `ExecutedTriggersSuite.MovesThePawnBack`
   (`:36-42`) answers every pawn move, its own move back included. Nested,
   it would run until the cap. It is gated on the pawn standing at S3.

## File structure

- Modify `src/main/scala/distantlands/gameplay/walker/ExecutedTriggers.scala`:
  `keyAt`, `MaxDepth`, level-aware `batch`, `holds` and `held`.
- Modify `src/main/scala/distantlands/gameplay/walker/WalkState.scala`:
  `triggersOff`, `followUps`, `depth`, `restrictionRoot`.
- Modify `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala`:
  the burn/cost case, `narrowParked`, `runNarrowed`, `triggered`,
  `walkTriggered`.
- Modify `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala`:
  `leafAt`, `leavesAt`, `narrowedAt`, `nodeAt`, `resolveAt`, `followUpAt`.
- Modify `src/main/scala/distantlands/gameplay/powerresolver/ExecutedTrigger.scala`:
  the doc.
- Modify `src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphetClaim.scala`:
  the doc.
- Tests: `walker/ExecutedTriggersSuite.scala`,
  `gameplay/FalseProphetClaimSuite.scala`,
  `application/gamelog/FalseProphetLogSuite.scala`.
- Docs: the batch 11 spec status, `docs/architecture/bounded-visions-and-conspiracy.md`.

---

### Task 1: A held key per follow-up level

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/walker/ExecutedTriggers.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkState.scala:33-45`
- Modify: `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala:283-295, 462-472, 496-500, 609-655`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala:529-614`
- Modify: `src/main/scala/distantlands/gameplay/powerresolver/ExecutedTrigger.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/ExecutedTriggersSuite.scala`

**Interfaces:**
- Produces: `ExecutedTriggers.keyAt(depth: Int): DeferralKey`,
  `ExecutedTriggers.MaxDepth: Int` (8),
  `ExecutedTriggers.holds(ready: ReadyGame, depth: Int): Boolean`,
  `ExecutedTriggers.batch(catalog, actor, ready, depth: Int)`.
  `ExecutedTriggers.key` is removed.
- Produces: `WalkCtx.triggersOff: Boolean`, `WalkCtx.followUps:
  Vector[Operation]`, `WalkCtx.depth: Int`. `following` and `followUp`
  are removed.
- Produces: `ProcedureWalker.narrowParked(..., decide: Decide, followUps:
  Vector[Operation])`.

- [ ] **Step 1: Write the failing tests**

In `ExecutedTriggersSuite.scala`, add `import oathdigital.model.TestGameFixtures.sites`
(check that `ProcedureWalkerFixture` takes `sites` from there:
`grep -n "import" src/test/scala/distantlands/testkit/ProcedureWalkerFixture.scala`).

Replace `MovesThePawnBack` (lines 33-42) with a trigger that stops:

```scala
  /** Where the fixture's pawn goes, and where it starts. */
  private val s3: SiteId = sites(2)

  /** The move back from S3 to where the fixture's pawn starts. */
  private val movePawnBack: Move = Move(Piece.Pawn(actor), movePawnToS3.to,
    movePawnToS3.from)

  /** A test trigger: after a batch that leaves the actor's pawn at S3,
    * moves it back, asking nothing. Its own move back leaves the pawn
    * elsewhere, so it does not start itself again. */
  private object MovesThePawnBackFromS3 extends ExecutedTrigger:
    val id: PowerId = PowerId("test.moves-the-pawn-back-from-s3")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(movesThePawn(batch.executed) &&
          pawnSiteOf(batch.now).contains(s3))(
        BuildOps((_, _) => Right(Vector[CoreOperation](movePawnBack))))

  /** A test trigger: after a batch that moves the actor's pawn off S3,
    * asks the fixture's question, then spends a Supply. */
  private object AsksAfterTheReturn extends ExecutedTrigger:
    val id: PowerId = PowerId("test.asks-after-the-return")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.contains(movePawnBack))(Sequence(decide,
        BuildOps((_, _) => Right(Vector[CoreOperation](spendOneSupply)))))

  /** A test trigger that answers every pawn move with another, so its
    * follow-ups would nest without end. */
  private object MovesThePawnForever extends ExecutedTrigger:
    val id: PowerId = PowerId("test.moves-the-pawn-forever")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(movesThePawn(batch.executed))(BuildOps((now, _) =>
        Right(Vector[CoreOperation](
          if pawnSiteOf(now).contains(s3) then movePawnBack
          else movePawnToS3))))
```

Change `heldMove` to `Defer(ExecutedTriggers.keyAt(0), Vector(movePawnToS3))`,
`assert(ExecutedTriggers.holds(atPark))` to
`assert(ExecutedTriggers.holds(atPark, 0))`, and both
`TakeDeferred(ExecutedTriggers.key)` to
`TakeDeferred(ExecutedTriggers.keyAt(0))`.

Rename the test "a follow-up's own batches, and a batch no trigger watches,
start no follow-up" to "a follow-up's batch that no trigger watches starts
no follow-up", and make it walk `WalkerPowers(Vector(MovesThePawnBackFromS3))`.
Its expected events stay as they are.

Add:

```scala
  /** The pawn moves to S3; the first trigger moves it back, and that
    * return starts the second trigger's question, one level down. */
  private val nesting = WalkerPowers(Vector(MovesThePawnBackFromS3,
    AsksAfterTheReturn))
  private def indexOf(trigger: ExecutedTrigger): String =
    ExecutedTriggers.of(nesting).indexOf(trigger).toString
  private val triggerCount: String =
    ExecutedTriggers.of(nesting).size.toString

  test("the depth-0 key is the one saved games hold"):
    assertEquals(ExecutedTriggers.keyAt(0),
      DeferralKey("walker.executed-triggers"))
    assertNotEquals(ExecutedTriggers.keyAt(1), ExecutedTriggers.keyAt(0))

  test("a follow-up's batch that a trigger watches starts a follow-up one " +
      "level down, holding its operations under that level's key"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val back = indexOf(MovesThePawnBackFromS3)
    val asks = indexOf(AsksAfterTheReturn)
    assertEquals(park.tree.at, Vector("0", back, asks, "0"))
    assertEquals(park.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector[CoreOperation](movePawnToS3, heldMove),
      Vector[CoreOperation](movePawnBack,
        Defer(ExecutedTriggers.keyAt(1), Vector(movePawnBack)))))
    assertEquals(park.events.map(recorded(_).nodeId), Vector("0", s"0.$back"))
    val atPark = applyEvents(ready, park.events)
    assert(ExecutedTriggers.holds(atPark, 0))
    assert(ExecutedTriggers.holds(atPark, 1))

  test("an answer two levels down resumes there and takes each level's " +
      "batch on the way out"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val atPark = applyEvents(ready, park.events)
    val done = finished(ProcedureWalker.resolve(atPark, movedThenSpent,
      park.tree, Answered(decide.decisionId, ChooseOneAnswer(continueOption),
        actor), nesting))
    val back = indexOf(MovesThePawnBackFromS3)
    val asks = indexOf(AsksAfterTheReturn)
    assertEquals(done.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector.empty[CoreOperation], Vector[CoreOperation](spendOneSupply),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(1))),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(0))),
      Vector[CoreOperation](spendOneSupply)))
    assertEquals(done.events.map(recorded(_).nodeId), Vector(
      s"0.$back.$asks.0", s"0.$back.$asks.1", s"0.$back.$triggerCount",
      s"0.$triggerCount", "1"))
    assertEquals(pawnSiteOf(done.treeless), pawnSiteOf(ready))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 2)

  test("a position parked two levels down resolves to its question"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val atPark = applyEvents(ready, park.events)
    val pending = PendingTree(at = park.tree.at, answered = Vector.empty)
    assertEquals(ProcedureWalker.parkedDecide(atPark, movedThenSpent, pending,
      nesting), Some(decide))

  test("follow-ups nested deeper than the cap stop the walk"):
    val refused = ProcedureWalker.advance(ready, movedThenSpent, None,
      WalkerPowers(Vector(MovesThePawnForever)))
    assertEquals(refused.left.toOption, Some(OathViolation.InvalidEventOrder(
      "walker contract violation: follow-ups nested deeper than " +
        s"${ExecutedTriggers.MaxDepth}")))
```

`ExecutedTriggers` is `private[walker]`; the suite is in package
`oathdigital.gameplay.walker`, so it reaches it. If the event list for the
cap test is not a `Left` because a batch below the cap fails first, read
the actual result and keep the assertion on the cap's message.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.ExecutedTriggersSuite"`
Expected: compile errors: `keyAt`, `MaxDepth` and `holds(_, _)` do not
exist.

- [ ] **Step 3: Make `ExecutedTriggers` level-aware**

Replace the body of `ExecutedTriggers.scala` from `val key` to the end:

```scala
  /** The key a batch at follow-up depth `depth` holds its operations under.
    * Depth 0, the action's own batches, keeps the key saved games hold.
    * Each level down has its own, so a nested batch's operations never mix
    * with those of the batch whose follow-up it runs in (catalog batch 11,
    * slice 8). */
  def keyAt(depth: Int): DeferralKey =
    if depth == 0 then DeferralKey("walker.executed-triggers")
    else DeferralKey(s"walker.executed-triggers.$depth")

  /** How deep follow-ups may nest. Each card's follow-up removes what
    * started it, so a deeper chain is a trigger that restarts itself. */
  val MaxDepth: Int = 8

  /** The triggers among `powers`, in the order contributions chain. */
  def of(powers: WalkerPowers): Vector[ExecutedTrigger] =
    powers.powers.collect { case trigger: ExecutedTrigger => trigger }
      .sortBy(ContributingPower.sortKey)

  /** What a trigger reads now: the operations held at the walk's level,
    * and the walk's state. */
  def batch(ctx: WalkCtx): ExecutedBatch =
    batch(ctx.powers.catalog, ctx.actor, ctx.state, ctx.depth)

  /** The same, for a lookup that has no walk, only the parked state. */
  def batch(catalog: ExecutableCatalog, actor: PlayerId, ready: ReadyGame,
      depth: Int): ExecutedBatch =
    ExecutedBatch(catalog, actor, Deferred.held(ready, keyAt(depth)), ready)

  /** Whether a batch at `depth` holds its operations for its follow-ups. */
  def holds(ready: ReadyGame, depth: Int): Boolean =
    Deferred.held(ready, keyAt(depth)).nonEmpty

  /** `run`, with its executed operations held at the walk's level when some
    * trigger starts a follow-up on them. Nothing is held for an empty
    * batch, or while triggers are off (a claimed burn or cost run whole,
    * the take of a held batch). A follow-up past `MaxDepth` is refused. */
  def held(ctx: WalkCtx, run: OperationRun)
      : Either[OathViolation, OperationRun] =
    val batch = ExecutedBatch(ctx.powers.catalog, ctx.actor, run.executed,
      run.state)
    if ctx.triggersOff || run.executed.isEmpty ||
        !of(ctx.powers).exists(_.onExecuted(batch).nonEmpty) then Right(run)
    else if ctx.depth >= MaxDepth then Left(OathViolation.InvalidEventOrder(
      s"walker contract violation: follow-ups nested deeper than $MaxDepth"))
    else OperationPipeline.run(run.state,
        Vector(Defer(keyAt(ctx.depth), run.executed)),
        OperationPolicy.Permissive, Vector.empty, requireAll = true)(
        Right(_)).map(deferred => run.copy(state = deferred.state,
        executed = run.executed ++ deferred.executed))
```

Update the object's doc: replace "A batch that starts a follow-up holds its
executed operations under [[key]]" with "A batch that starts a follow-up
holds its executed operations under its level's key ([[keyAt]])".

- [ ] **Step 4: Replace the walk's flag and tree with a level stack**

`WalkState.scala`, replace the `following` and `followUp` fields and
`restrictionRoot`:

```scala
    /** Set while a claimed burn or cost runs whole, and while a level's
      * held batch is taken: those batches start no follow-up, since a
      * resume could not reach one there (`ExecutedTriggers`). */
    triggersOff: Boolean = false,
    /** The follow-up trees this walk is inside, outermost first. Their
      * count is the walk's follow-up depth, whose key a batch holds its
      * operations under (catalog batch 11, slice 8). None is part of
      * `root`, so the restriction look-ahead reads them beside it: a
      * follow-up may nest a card play whose hook a restriction judges
      * (False Prophet's claim, slice 7). */
    followUps: Vector[Operation] = Vector.empty
):
  /** How many follow-ups this walk is inside. */
  def depth: Int = followUps.size

  /** The tree the restriction look-ahead folds: `root`, and every
    * follow-up this walk is inside. */
  def restrictionRoot: Operation =
    if followUps.isEmpty then root else Sequence(root +: followUps)
```

`ProcedureWalker.scala`, the claimed burn or cost (lines 283-295): change
`ctx.copy(following = true)` to `ctx.copy(triggersOff = true)` and
`Done(next.copy(following = ctx.following))` to
`Done(next.copy(triggersOff = ctx.triggersOff))`.

`narrowParked` (lines 462-472): rename the parameter and the field:

```scala
    * `followUps` are the follow-up trees the park lies inside, outermost
    * first. */
  private[walker] def narrowParked(state: ReadyGame, action: Operation,
      pending: PendingTree, powers: WalkerPowers, decide: Decide,
      followUps: Vector[Operation]): Decide =
    val ctx = WalkCtx(strip(state), Vector.empty,
      state.game.current.walkActor, pending.answered,
      powers.withAnswers(pending.answered), WalkerDice.placeholder,
      state.game.current.walkerProcedure, action, followUps = followUps)
```

`runNarrowed` (line 498): `if ExecutedTriggers.holds(ctx.state, ctx.depth) then`.

`triggered` (lines 609-614):

```scala
  /** After a batch, the follow-ups its triggers started, when it held its
    * operations for them at the walk's level. */
  private def triggered(path: Vector[String], resume: Resume)(step: Step)
      : Either[OathViolation, Step] = step match
    case Done(ctx) if !ctx.triggersOff &&
        ExecutedTriggers.holds(ctx.state, ctx.depth) =>
      walkTriggered(ctx, path, None, resume,
        ctx.previous.fold(ctx.state)(_._1))
    case other => Right(other)
```

`walkTriggered` (lines 616-655), keeping its doc and adding "A follow-up's
own batches may start follow-ups one level down (catalog batch 11, slice
8)." at its end:

```scala
  private def walkTriggered(ctx: WalkCtx, path: Vector[String],
      cursor: Option[Vector[String]], resume: Resume, before: ReadyGame)
      : Either[OathViolation, Step] =
    val triggers = ExecutedTriggers.of(ctx.powers)
    def from(index: Int, at: WalkCtx, rest: Option[Vector[String]])
        : Either[OathViolation, Step] =
      if index == triggers.size then
        // The take ends this level, so it starts no follow-up.
        WalkerSteps.record(TakeDeferred(ExecutedTriggers.keyAt(ctx.depth)),
          at.copy(triggersOff = true), path :+ index.toString, Vector.empty,
          strict = true).map(done =>
          Done(done.copy(triggersOff = ctx.triggersOff,
            previous = Some((before, done.state)))))
      else
        val trigger = triggers(index)
        (trigger.onExecuted(ExecutedTriggers.batch(at)), rest) match
          case (None, None) => from(index + 1, at, None)
          case (None, Some(_)) => contractViolation(
            s"resume path enters follow-up $index, which did not start")
          case (Some(tree), _) => walk(tree,
              at.copy(followUps = at.followUps :+ tree),
              path :+ index.toString, rest, resume,
              WalkerHooks(Vector(trigger.id), Set.empty))
            .flatMap:
              case Done(next) =>
                from(index + 1, next.copy(followUps = ctx.followUps), None)
              case stop => Right(stop)
    cursor.flatMap(_.headOption) match
      case None => from(0, ctx, None)
      case Some(segment) =>
        segment.toIntOption.filter(i => i >= 0 && i < triggers.size) match
          case Some(index) => from(index, ctx, cursor.map(_.tail))
          case None => contractViolation(
            s"invalid follow-up segment '$segment' at ${path.mkString(".")}")
```

`ExecutedTriggers.batch(at)` reads `at.depth`, the level of the batch that
started these follow-ups; the follow-up's own leaves walk one level down.

- [ ] **Step 5: Count levels in the parked lookup**

`WalkerPowerGather.scala`. In `leafAt`, `leavesAt` and `narrowedAt`
(lines 529-555), rename `followUp` to `followUps` and its type to
`Vector[Operation]`; `narrowedAt` passes `followUps` to
`ProcedureWalker.narrowParked`.

`resolveAt` (lines 563-600): the last parameter becomes
`followUps: Vector[Operation] = Vector.empty`, the result type
`Option[(Operation, Vector[Operation])]`, the doc "The node `remaining`
addresses under `node`, with the follow-up trees the path entered on the
way, outermost first.", and each recursive call passes `followUps`. The
follow-up case:

```scala
        // Past a leaf whose batch is held at this level, the path enters
        // a follow-up.
        case leaf: LeafOperation
            if leaf.window.forall(gathered.contains) &&
              ExecutedTriggers.holds(state, followUps.size) =>
          followUpAt(state, pending, powers, segment, remaining.tail,
            consumed, followUps)
```

`followUpAt` (lines 602-614):

```scala
  /** The node at `rest` inside the follow-up that trigger `segment` started
    * after a batch held at the level `followUps` names: the tree it returns
    * now, asked as the walk asked it (`ExecutedTrigger`). */
  private def followUpAt(state: ReadyGame, pending: PendingTree,
      powers: WalkerPowers, segment: String, rest: Vector[String],
      consumed: Vector[String], followUps: Vector[Operation])
      : Option[(Operation, Vector[Operation])] =
    val triggers = ExecutedTriggers.of(powers)
    for
      index <- segment.toIntOption if index >= 0 && index < triggers.size
      tree <- triggers(index).onExecuted(ExecutedTriggers.batch(
        powers.catalog, state.game.current.walkActor, state, followUps.size))
      found <- resolveAt(state, pending, powers, tree, rest,
        consumed :+ segment, Set.empty, followUps :+ tree)
    yield found
```

`nodeAt` keeps `.map(_._1)`.

Run: `grep -rn "following\|followUp\b\|followUp =\|ExecutedTriggers.key\b" src/main/scala/distantlands/gameplay/walker`
and confirm nothing is left. (`PricedOptions`, `QueuedStart` and
`WalkerSimulation` have an unrelated `followUp: Boolean` for priced
follow-up options; leave them.)

- [ ] **Step 6: Update the trigger's contract**

`powerresolver/ExecutedTrigger.scala`, in the trait's doc, replace "A
follow-up's own batches start no follow-up." with:

```scala
  * after the follow-ups of the triggers before it ran, in the order
  * contributions chain. A follow-up's own batches ask the triggers too, and
  * their follow-ups walk one level down (catalog batch 11, slice 8), so a
  * follow-up must remove what started it: the walker refuses a chain
  * deeper than `ExecutedTriggers.MaxDepth`.
```

Keep the rest of the doc as it is.

- [ ] **Step 7: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.*"`
Expected: PASS.

Run the whole backend suite, since every trigger now answers nested
batches (finding 6): `./sbtw test`
Expected: PASS. If a test fails because Pledge to Discord or Brass Army
now asks inside another trigger's follow-up, read the new log or event
order first. Change the test only if the new place is where the card's
text puts it ("after", "whenever"); otherwise stop and report.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/walker/ExecutedTriggers.scala \
  src/main/scala/distantlands/gameplay/walker/WalkState.scala \
  src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala \
  src/main/scala/distantlands/gameplay/powerresolver/ExecutedTrigger.scala \
  src/test/scala/distantlands/gameplay/walker/ExecutedTriggersSuite.scala
git commit -m "feat(walker): a follow-up's batches may start follow-ups one level down" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint review** after this task: dispatch a reviewer with this
task's diff, findings 1 to 7, the spec's slice 8 section, and
`ExecutedTrigger.scala`'s doc. Ask especially about resume through two
levels, the take's `triggersOff`, and `resolveAt`'s level count.

---

### Task 2: False Prophet's nested claim

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphetClaim.scala` (doc)
- Test: `src/test/scala/distantlands/gameplay/FalseProphetClaimSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/FalseProphetLogSuite.scala`

**Interfaces:**
- Consumes: Task 1's nested follow-ups. No new production names.

- [ ] **Step 1: Write the walk tests**

In `FalseProphetClaimSuite`, after the "two claimed Visions" test, add:

```scala
  /** `conquestClaimedByP2`, where p2 owns Sanctuary, which p3 claims. */
  private def sanctuaryClaimedByP3: Table = conquestClaimedByP2
    .revealedVision(p2, VisionRules.Sanctuary)
    .warbandsOnCard(VisionRules.Sanctuary, p3, 1)

  test("a claimant's reveal that discards a Vision another player claims " +
      "asks that player in turn, then kills both warbands"):
    val sanctuary = VisionRules.Sanctuary
    val asked = revealedOverClaim(sanctuaryClaimedByP3)
    parked.assertParked(asked, ActionRef.Search, placeId(claimed), p2)
    val nested = answer(asked, p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    parked.assertParked(nested, ActionRef.Search, placeId(sanctuary), p3)
    val done = answer(nested, p3, placeId(sanctuary),
      DecisionOptionRef.RevealedVision(p3))
    assertEquals(revealedBy(done, p1), Vector[VisionId](drawn))
    assertEquals(revealedBy(done, p2), Vector[VisionId](claimed))
    assertEquals(revealedBy(done, p3), Vector[VisionId](sanctuary))
    val ready = Outcomes.ready(done)
    def on(vision: VisionId, owner: PlayerId): Int =
      ready.game.current.warbandsOn(vision,
        ForceKind.Exile(Look(ready).player(owner).lineage))
    assertEquals((on(claimed, p2), on(sanctuary, p3)), (0, 0))

  test("the nested claimant's reveal is judged for them"):
    // Sacred Ground stands at Ancient City, where p1 and p2 stand, so both
    // reveal; p3 at Broken Peaks may only discard Sanctuary.
    val table = sanctuaryClaimedByP3.pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BrokenPeaksSite)
      .edifice(SacredGroundCard.id, EdificeSide.Intact,
        at = AncientCitySite.id)
    val nested = answer(revealedOverClaim(table), p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    val offered = PlacementPicks.parkedOptions(nested, walkerPowers)
      .filter(_.available).map(_.ref)
    assert(!offered.contains(DecisionOptionRef.RevealedVision(p3)),
      offered.toString)
    assert(offered.exists(_.isInstanceOf[DecisionOptionRef.DiscardPile]),
      offered.toString)
```

Check before running that `Table.revealedVision(p2, ...)` keeps p2's slot
as a list of one (slice 6), and that the quiet table seats p3 (the suite
already uses `p3`).

- [ ] **Step 2: Write the log test**

In `FalseProphetLogSuite`, add a walk that goes one level deeper, beside
`claimRevealed`:

```scala
  /** `claimRevealed`, where p2 owns Sanctuary, which p3 claims, so p2's
    * reveal of Conquest discards it and p3 reveals it. */
  private def nestedClaimRevealed: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).supply(p1, 5)
      .revealedVision(p1, VisionRules.Conquest)
      .warbandsOnCard(VisionRules.Conquest, p2, 1)
      .revealedVision(p2, VisionRules.Sanctuary)
      .warbandsOnCard(VisionRules.Sanctuary, p3, 1)
      .adviser(p2, FalseProphetCard.id).worldDeckTop(VisionRules.Faith)
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    val faith = accepted(service.handle("g", picked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Faith),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))))
    val conquest = accepted(service.handle("g", faith.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Conquest),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))))
    accepted(service.handle("g", conquest.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Sanctuary),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p3))))))
    texts(service)

  test("a nested claim reads in walk order: each reveal, the inner kill, " +
      "the outer kill, then the owner's reveal"):
    val log = nestedClaimRevealed
    def at(line: String => Boolean): Int =
      val index = log.indexWhere(line)
      assert(index >= 0, log.toString)
      index
    val conquest = at(_.endsWith(" revealed Vision of Conquest"))
    val sanctuary = at(_.endsWith(" revealed Vision of Sanctuary"))
    val innerKill = at(_.endsWith("'s warband on Vision of Sanctuary."))
    val outerKill = at(_.endsWith("'s warband on Vision of Conquest."))
    val faith = at(_.startsWith("Revealed Vision of Faith"))
    assertEquals(Vector(conquest, sanctuary, innerKill, outerKill, faith),
      Vector(conquest, sanctuary, innerKill, outerKill, faith).sorted,
      log.toString)
```

The order follows the walk: p2's play runs inside the outer follow-up,
its batch's follow-up (p3's play and the Sanctuary kill) runs before the
outer follow-up's kills, and the owner's reveal line is the play's third
child (slice 7 plan, finding 9). If the log puts a line elsewhere, print
the log and read the walk before changing the expected order.

- [ ] **Step 3: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.FalseProphetClaimSuite oathdigital.application.gamelog.FalseProphetLogSuite"`
Expected: PASS, with Task 1 in place. To see that the tests prove the
nesting, check out Task 1's parent commit's walker files in a scratch
copy only if a test passes suspiciously; do not revert committed work.

- [ ] **Step 4: Update the claim's doc**

`FalseProphetClaim.scala`, at the end of the object's doc, add:

```scala
  *
  * A claimant who reveals the Vision discards their own revealed Vision
  * inside this follow-up. When another player claims that one, its
  * discard starts this trigger's follow-up again, one level down (catalog
  * batch 11, slice 8), and that claim resolves before this one's kills.
```

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphetClaim.scala \
  src/test/scala/distantlands/gameplay/FalseProphetClaimSuite.scala \
  src/test/scala/distantlands/application/gamelog/FalseProphetLogSuite.scala
git commit -m "test(powers): a Vision discarded inside False Prophet's claim is claimed in turn" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Close the slice

**Files:**
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md:3`
- Modify: `docs/architecture/bounded-visions-and-conspiracy.md` (the False Prophet paragraph)

- [ ] **Step 1: Update the documents**

Spec status line 3: `**Status:** designed 2026-11-07. Slices 0 to 8 landed.`

`bounded-visions-and-conspiracy.md`, at the end of the False Prophet
paragraph (after "...reads that follow-up beside the action's tree."),
add:

```markdown
A claimant's reveal discards their own revealed Vision inside that
follow-up; when another player claims it, its claim resolves one level
down before the outer kills (slice 8).
```

- [ ] **Step 2: Run every check**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92%.

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py` and
`python3 scripts/check-markdown-links.py`
Expected: no violation, no broken link. `wc -l` on `ProcedureWalker.scala`
and `WalkerPowerGather.scala`: both under 800.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md \
  docs/architecture/bounded-visions-and-conspiracy.md
git commit -m "docs(catalog): close catalog batch 11 slice 8, nested follow-ups" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Execution changes

Decided while executing, the first with the owner:

- **Nesting is opt-in.** With every trigger answering nested batches, the
  claim restarted itself (its claimant's discard of the claimed Vision, or
  a swap of two claimed Visions, is a discard while its warband is still
  on the card), and Pledge to Discord, which answers every batch, could
  park inside any follow-up where a resume rebuilds a different tree.
  `ExecutedTrigger.answersFollowUps` (default false) selects the triggers
  a follow-up's batch asks; only False Prophet's claim turns it on.
  `ExecutedTriggers.of` takes the depth.
- **The claim skips its own discards:** one from a discard pile, and one
  from the slot of a player whose warband is still on the Vision. So a
  walk never plays one Vision twice, which the walk-wide placement
  answers could not tell apart.
- **The narrowing search starts at depth 0.** It walks again from the
  root and re-enters each level by its path, so its context resets
  `followUps` and `triggersOff` (`DecideNarrowing.verdict`).
- **Test triggers read the batch, not the state,** so a resume rebuilds
  them after their own follow-up has run.
- **Log order:** a nested claim's lines read before the outer claimant's
  reveal line, since a play's own line follows the follow-ups walked
  under its batch (slice 7, finding 9).

## Known limits

- **The owner's reveal line still comes last** (slice 7, finding 9): the
  walk writes the play's own line after its follow-ups.
- **A chain deeper than 8 levels stops the walk.** No card's chain comes
  near it; a trigger that hits it is a bug in that trigger.
- **Only False Prophet's claim answers a follow-up's batches.** Brass Army
  and Pledge to Discord still answer the action's own batches alone.
- **The claimant's discard of their own Vision names no one.** The
  generic "Discarded Vision of Sanctuary to the Provinces discard" line
  does not say whose it was when the claimant is off turn.
