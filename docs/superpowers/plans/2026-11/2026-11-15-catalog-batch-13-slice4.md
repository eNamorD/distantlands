# Catalog Batch 13, Slice 4: Power Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When two or more powers apply in a player's `SetupPawnPlaced`
window, that player chooses which resolves first, then next, against the
live state, each power at most once.

**Architecture:** Today the window is the pawn's own `Sequence`, and each
power appends its steps with a `Transform`, in power order. This slice
adds a third host beside `OfferHost` (battle plans) and `RerollHost`
(rerolls): a `ResolutionHost`, whose window's powers each offer their
steps through a new `OfferResolution` contribution. The host is
`OrderedWindow`, one node per round. A round asks "Choose a power to
resolve" over the offers not yet resolved, runs the picked power's steps,
then holds the next round, which the walker folds when it reaches it, so
it gathers the window again against the live state.

**Why every round records its pick.** The spec says the host's shape is "a
pure function of the offers and the answers". A power that resolves
without a question leaves no answer. Without one, a walk resumed in a later
round could not tell which power the unasked round resolved. Slice 5 hits
this case: Amber Doors resolves alone, activates The Standard Bearer, and
the walk parks in The Standard Bearer's question. On resume, the first
round's offers are Amber Doors and The Standard Bearer, so it would ask
again. So the question is a `Decide` marked `answersAlone`. The walker
answers it itself when one option is choosable and records that answer
like any other. Nothing is asked, as ruling 4 requires, and every pick is
an answer. This is the walker's one new behavior. It works the way an
automatic roll records its faces.

**Changes made while executing.** A review found that replay refused the
answer the walker gives alone: replay checks every choice step against the
durable park, and none precedes that answer, so the live command failed too.
`ChoicePayload` gained `alone: Boolean = false`, as `RollPayload` has
`automatic`. Replay checks such a step's node id only and adds its answer
to the pending answers, seeding them when the walk has not parked yet. The
wire writes `alone` only when set. `AnswersAloneSuite` gained a replay test
and `WalkerOperationWireSuite` a wire test. Supply caps at 7, so the gains
in `OrderedWindowSuite` are 3, not 10. The replay test in
`LaterGameSetupChoicesSuite` reads the park with the default powers.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
rulings 4 and 5 and section "Slice 4: power order".

## Global Constraints

- Slices 3 and 4 change the Setup walk, so this slice runs the Setup,
  replay and golden suites in full before landing (spec, "Goal and
  scope"). In practice that means the full JVM suite.
- The pick writes no Game Log line (ruling 4). Setup's procedure log
  already narrates every `setup.` decision, so the id must start with
  `setup.`.
- Each power's own lines and decision ids are unchanged.
- `build` doubles as `rebuild`: every fold must be a pure function of
  state and answers.
- A power's resolution offer must stand while its own steps run: a walk
  resumed inside them finds them among the offers again. Every power this
  slice touches meets that, since its steps never change what it reads to
  apply.
- Out of scope (spec): ordering effects on several players, and ordering
  any window but `SetupPawnPlaced`. `WhenExplored` keeps its transforms.
- No production Scala file exceeds 800 lines (`check-architecture.py`).
  `ProcedureWalker.scala` is at 697 and `WalkerPowerGather.scala` at 688.
- Docs, comments and commit messages are written in plain English.
- Journals saved mid-Setup before this slice need not resume: the player
  step's children shift by one. Slice 2 already accepts that pre-slice
  journals need not replay.

## Files

- Modify: `src/main/scala/distantlands/model/Operation.scala`.
  Adds `Decide.answersAlone`.
- Modify: `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala`.
  The walker answers such a question alone.
- Create: `src/main/scala/distantlands/model/ResolutionOffers.scala`, with
  `ResolutionOffer` and `OfferedResolution`.
- Modify: `src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala`.
  Adds `OfferResolution` and `ResolutionHost`.
- Modify: `src/main/scala/distantlands/gameplay/powerresolver/ContributionCollector.scala`.
  Adds `resolutions`.
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala`.
  Adds the `ResolutionHost` case.
- Create: `src/main/scala/distantlands/gameplay/setup/OrderedWindow.scala`.
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`.
  The player step uses `OrderedWindow`.
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`SetupPawnPlaced` scaladoc).
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala`.
  Adds `option`.
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/ProvingGroundsRules.scala`
  and `GreatForgeRules.scala`. They offer at `SetupPawnPlaced` and fold at
  `WhenExplored`.
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/TheStandardBearer.scala`
  and `Wealthy.scala`. They offer, and check their holder at their step.
- Test, new:
  - `src/test/scala/distantlands/gameplay/walker/AnswersAloneSuite.scala`
  - `src/test/scala/distantlands/gameplay/setup/OrderedWindowSuite.scala`
    (stub powers)
  - `src/test/scala/distantlands/gameplay/setup/SetupPowerOrderSuite.scala`
    (real cards)
- Test, modified:
  - `src/test/scala/distantlands/testkit/Situation.scala`,
    `gameplay/TravelProcedureSuite.scala` and
    `gameplay/SearchProcedureSuite.scala`: the positional `Decide`
    patterns gain a field.
  - `src/test/scala/distantlands/gameplay/setup/SetupWalkDriver.scala`:
    takes answers.
  - `src/test/scala/distantlands/gameplay/powers/setup/GreatForgeRulesSuite.scala`:
    the in-window stub offers.
  - `src/test/scala/distantlands/gameplay/setup/legacies/TheStandardBearerSuite.scala`:
    the R27 order test moves to `SetupPowerOrderSuite`.
  - `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`:
    a replay test.
  - `src/test/scala/distantlands/application/gamelog/GameLogSetupSuite.scala`:
    the pick writes no line.
  - `src/test/scala/distantlands/application/gamelog/GameLogNoteLinesSuite.scala`:
    a comment.
  - Goldens under `src/test/resources/gamelog`, only if their event
    numbers shift.
- Docs:
  - `docs/rules/ambiguities.md`
  - `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`
  - `docs/rules/rules-reference.md`
  - `docs/rules/implementation-traceability.md`
  - `docs/ROADMAP.md`
  - the spec (approach and status)

---

### Task 1: A question the walker answers alone

**Files:** `Operation.scala`, `ProcedureWalker.scala`, the three suites
with positional `Decide` patterns, and the new `AnswersAloneSuite`.

**Interfaces:**
- Produces: `Decide(..., answersAlone: Boolean = false)`, the last field.
  When it is true the query must be a choose-one. If exactly one option
  is choosable (`WalkerSearch.choosable`), the walker answers the
  question with that option for `owner`, records the answer as a
  `ChoicePayload` step and walks on. With two or more options it parks as
  usual.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/walker/AnswersAloneSuite.scala`:

```scala
package distantlands.gameplay.walker

import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Outcomes.{finished, parked}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, present}

/** `Decide.answersAlone`: a choose-one the walker answers itself when one
  * option is choosable (catalog batch 13, ruling 4). p1 starts with 1
  * supply, and each answer the question has gains 1. */
class AnswersAloneSuite extends munit.FunSuite:
  private val start = Table.start.supply(p1, 1).ready
  private val powers = WalkerPowers(Vector.empty)
  private val id = "test.alone"

  private def button(key: String): DecisionOption =
    DecisionOption.Button(DecisionOptionRef.Button(key), key)

  /** The question, then a gain of 1 supply for each answer it has. */
  private def asking(options: Vector[DecisionOption], alone: Boolean)
      : Operation = Sequence(Vector(
    Decide(id, p1, DecisionQuery.ChooseOne(options), answersAlone = alone),
    Branch((_, pending) => pending.answered.filter(_.decisionId == id)
      .map(_ => GainSupply(p1, 1)))))

  private def choices(events: Vector[OathEvent])
      : Vector[(String, DecisionAnswer, PlayerId)] = events.collect {
    case WalkerStepRecorded(_, ChoicePayload(decision, answer, by), _) =>
      (decision, answer, by)
  }

  test("with one option the walker answers for the owner, records the " +
      "answer and walks on"):
    val done = finished(ProcedureWalker.advance(start,
      asking(Vector(button("only")), alone = true), None, powers))
    assertEquals(Look(done.treeless).supply(p1), 2)
    assertEquals(choices(done.events), Vector((id,
      ChooseOneAnswer(DecisionOptionRef.Button("only")), p1)))

  test("an unavailable option is not choosable, so the one available " +
      "option is answered alone"):
    val done = finished(ProcedureWalker.advance(start, asking(Vector(
      button("only"), DecisionOption.Unavailable(button("no"),
        OptionReason.Written("not now"))), alone = true), None, powers))
    assertEquals(choices(done.events).map(_._2), Vector(
      ChooseOneAnswer(DecisionOptionRef.Button("only"))))

  test("with two options it parks as any question does"):
    val tree = asking(Vector(button("a"), button("b")), alone = true)
    val pending = parked(ProcedureWalker.advance(start, tree, None, powers))
    assertEquals(present(ProcedureWalker.parkedDecide(start, tree,
      pending.tree, powers), "the question").decisionId, id)

  test("without the mark one option still parks"):
    val tree = asking(Vector(button("only")), alone = false)
    val pending = parked(ProcedureWalker.advance(start, tree, None, powers))
    assertEquals(present(ProcedureWalker.parkedDecide(start, tree,
      pending.tree, powers), "the question").decisionId, id)
    accepted(ProcedureWalker.resolve(start, tree, pending.tree,
      Answered(id, ChooseOneAnswer(DecisionOptionRef.Button("only")), p1),
      powers))
```

If `parkedDecide` returns the `Decide` rather than an `Option`, drop the
`present(...)` wrapper. `SetupWalkDriver` wraps it in `present`, so an
`Option` is expected.

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.AnswersAloneSuite"`
Expected: compile error, "answersAlone" is not a parameter of `Decide`.

- [ ] **Step 3: Add the field to `Decide`**

In `src/main/scala/distantlands/model/Operation.scala`, add a paragraph
to `Decide`'s scaladoc:

```scala
  * `answersAlone` marks a choose-one the walker answers itself when exactly
  * one option is choosable: it records that answer for `owner`, as if they
  * had given it, and walks on without parking. With more options it is
  * asked as usual (catalog batch 13, ruling 4).
```

Then add the field last, and its check after the existing one:

```scala
final case class Decide(decisionId: String, owner: PlayerId,
    query: DecisionQuery,
    override val window: Option[PowerWindow] = None,
    coOwners: Vector[PlayerId] = Vector.empty,
    passWhenEmpty: Boolean = false,
    presentation: DecisionPresentation = DecisionPresentation.Board,
    answersAlone: Boolean = false)
    extends LeafOperation:
  require(!passWhenEmpty || query.isInstanceOf[DecisionQuery.ChooseOne],
    "only a choose-one can be passed when empty")
  require(!answersAlone || query.isInstanceOf[DecisionQuery.ChooseOne],
    "only a choose-one can be answered alone")
```

- [ ] **Step 4: Answer it alone in the walker**

In `ProcedureWalker.runNarrowed`, in the `case None =>` match, insert this
case right before `case _: Decide | _: Roll => Right(Park(path, ctx))`:

```scala
            // A question answered alone records its one choosable option
            // for its owner and walks on (catalog batch 13, ruling 4).
            case decide: Decide if decide.answersAlone &&
                WalkerSearch.choosable(decide).sizeIs == 1 =>
              WalkerSteps.answerDecide(decide, ctx, path, Answered(
                  decide.decisionId, DecisionAnswer.ChooseOneAnswer(
                    WalkerSearch.choosable(decide).head), decide.owner),
                  contributions)
                .map(next => Done(next.copy(
                  powers = next.powers.withAnswers(next.answered))))
```

Add `DecisionAnswer` to the `distantlands.model.{...}` import. The
`withAnswers` copy mirrors the answer path in `resumeAt`, so the rest of
the walk sees the answer.

- [ ] **Step 5: Give the positional `Decide` patterns their new field**

Each of these patterns lists all seven fields. Add one more `_`:

- `src/test/scala/distantlands/testkit/Situation.scala`, lines ~122, ~134
  and ~138: `Decide(_, _, one: DecisionQuery.ChooseOne, _, _, _, _)`
  becomes `Decide(_, _, one: DecisionQuery.ChooseOne, _, _, _, _, _)`.
  Change the `ChooseMany` and `ChooseAmount` patterns the same way.
- `src/test/scala/distantlands/gameplay/TravelProcedureSuite.scala`, line
  ~254, and `src/test/scala/distantlands/gameplay/SearchProcedureSuite.scala`,
  line ~63: `_, _, _, _) => options.map(_.ref)` becomes
  `_, _, _, _, _) => options.map(_.ref)`.

Run `grep -rn "Decide(" src | grep "_, _, _, _"` to find any others.

- [ ] **Step 6: Run the suite**

Run: `./sbtw "testOnly distantlands.gameplay.walker.AnswersAloneSuite"`
Expected: 4 tests pass.

- [ ] **Step 7: Run the walker suites**

Run: `./sbtw "testOnly distantlands.gameplay.walker.*"`
Expected: all pass. Nothing sets the field yet.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/Operation.scala src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala src/test/scala
git commit -m "feat(walker): answer a one-option question alone when it is marked so"
```

---

### Task 2: The ordering host

**Files:** `ResolutionOffers.scala` (new), `ContributingPower.scala`,
`ContributionCollector.scala`, `WalkerPowerGather.scala`,
`OrderedWindow.scala` (new), `OrderedWindowSuite.scala` (new).

**Interfaces:**
- Consumes: `Decide.answersAlone` (Task 1).
- Produces:
  - `distantlands.model.ResolutionOffer(option: DecisionOption, steps: Vector[Operation])`
  - `distantlands.model.OfferedResolution(power: PowerId, offer: ResolutionOffer)`
  - `distantlands.gameplay.powerresolver.OfferResolution(offer: PowerCtx => Option[ResolutionOffer]) extends Contribution`
  - `trait ResolutionHost extends Operation { def expand(offers: Vector[OfferedResolution], pass: OfferHost.Pass): Vector[Operation] }`
  - `GatheredContributions.resolutions: Vector[(PowerId, OfferResolution)]`
  - `distantlands.gameplay.setup.OrderedWindow(placer: PlayerId, resolved: Vector[DecisionOptionRef]) extends ResolutionHost`,
    with `OrderedWindow.decisionId(placer: PlayerId, round: Int): String`
    (`setup.power-order.<player>.<round>`), `OrderedWindow.heading`
    (`"Choose a power to resolve"`) and `OrderedWindow.confirmVerb`
    (`"Resolve"`).

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/setup/OrderedWindowSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.WalkerRecordedOpsReducer
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution}
import distantlands.gameplay.walker.{ChoicePayload, ProcedureWalker,
  WalkerPowers, WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Outcomes.{finished, parked}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.present

/** `OrderedWindow`, the host of Setup's pawn window (catalog batch 13,
  * ruling 4), over stub powers that gain p1 supply. p1 starts with 1
  * supply, so each gain shows, and a power may stand only at some supply,
  * so what an earlier power gained decides whether it applies. */
class OrderedWindowSuite extends munit.FunSuite
    with WalkerRecordedOpsReducer:
  private val start = Table.start.supply(p1, 1).ready
  private val window = OrderedWindow(p1, Vector.empty)

  private def supply(ready: ReadyGame): Int = Look(ready).supply(p1)

  /** Stub of ContributingPower: offers `steps`, under the button `name`,
    * while `stands` holds of p1's supply. */
  private final case class Offering(name: String, stands: Int => Boolean,
      steps: Vector[Operation]) extends ContributingPower:
    def id: PowerId = PowerId(s"test.$name")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
      PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
        Option.when(stands(Look(ctx.state).supply(p1)))(ResolutionOffer(
          DecisionOption.Button(DecisionOptionRef.Button(name), name),
          steps)))))

  private def gaining(name: String, amount: Int,
      stands: Int => Boolean = _ => true): Offering =
    Offering(name, stands, Vector(GainSupply(p1, amount)))

  private def pick(round: Int, name: String): Answered =
    Answered(OrderedWindow.decisionId(p1, round),
      ChooseOneAnswer(DecisionOptionRef.Button(name)), p1)

  private def choices(events: Vector[OathEvent])
      : Vector[(String, DecisionAnswer)] = events.collect {
    case WalkerStepRecorded(_, ChoicePayload(id, answer, _), _) => id -> answer
  }

  test("two standing offers ask the placer which resolves first, in " +
      "power order"):
    val powers = WalkerPowers(Vector(gaining("b", 10), gaining("a", 1)))
    val pending = parked(ProcedureWalker.advance(start, window, None, powers))
    val decide = present(ProcedureWalker.parkedDecide(start, window,
      pending.tree, powers), "the question")
    assertEquals(decide.decisionId, OrderedWindow.decisionId(p1, 0))
    assertEquals(decide.owner, p1)
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(
          DecisionOptionRef.Button("a"), DecisionOptionRef.Button("b")))
        assertEquals(one.heading, Some(OrderedWindow.heading))
      case other => fail(s"expected a choose-one, got $other")

  test("the chosen power resolves, then the window is gathered again: a " +
      "power that no longer applies drops out"):
    val powers = WalkerPowers(Vector(gaining("a", 1),
      gaining("b", 10, stands = _ == 1)))
    val pending = parked(ProcedureWalker.advance(start, window, None, powers))
    val done = finished(ProcedureWalker.resolve(start, window, pending.tree,
      pick(0, "a"), powers))
    assertEquals(supply(done.treeless), 2)

  test("a power the first one made applicable joins; one power left " +
      "resolves without a question; each resolves once"):
    val powers = WalkerPowers(Vector(gaining("a", 1),
      gaining("c", 10, stands = _ >= 2)))
    val done = finished(ProcedureWalker.advance(start, window, None, powers))
    assertEquals(supply(done.treeless), 12)
    assertEquals(choices(done.events), Vector(
      OrderedWindow.decisionId(p1, 0) ->
        ChooseOneAnswer(DecisionOptionRef.Button("a")),
      OrderedWindow.decisionId(p1, 1) ->
        ChooseOneAnswer(DecisionOptionRef.Button("c"))))

  test("with nothing offered the window asks and records nothing"):
    val done = finished(ProcedureWalker.advance(start, window, None,
      WalkerPowers(Vector(gaining("a", 1, stands = _ => false)))))
    assertEquals(done.events, Vector.empty[OathEvent])

  test("a walk resumed inside a power that joined after one resolved alone " +
      "rebuilds every round the same way"):
    // At the resume p1 has 2 supply, so the first round now sees both
    // offers; its recorded pick still names the one it resolved.
    val asks = Offering("j", _ >= 2, Vector(
      Decide("test.j", p1, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(DecisionOptionRef.Button("x"), "x"),
        DecisionOption.Button(DecisionOptionRef.Button("y"), "y")))),
      GainSupply(p1, 10)))
    val powers = WalkerPowers(Vector(gaining("a", 1), asks))
    val parkedIn = parked(ProcedureWalker.advance(start, window, None,
      powers))
    val there = foldRecordedOps(start, parkedIn.events, "the walk to j")
    assertEquals(supply(there), 2)
    val done = finished(ProcedureWalker.resolve(there, window,
      parkedIn.tree, Answered("test.j",
        ChooseOneAnswer(DecisionOptionRef.Button("x")), p1), powers))
    assertEquals(supply(done.treeless), 12)
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.OrderedWindowSuite"`
Expected: compile error, `OfferResolution`, `ResolutionOffer` and
`OrderedWindow` not found.

- [ ] **Step 3: Add the offer types**

Create `src/main/scala/distantlands/model/ResolutionOffers.scala`:

```scala
package distantlands.model

/** One power's steps, offered to the node that orders a window's powers
  * (catalog batch 13, ruling 4): the option that names the power in the
  * placer's question, by its card, and the steps that resolve it.
  *
  * The offer is rebuilt at every fold. It must stand while its own steps
  * run, since a walk resumed inside them finds them among the offers
  * again. */
final case class ResolutionOffer(option: DecisionOption,
    steps: Vector[Operation])

/** A resolution offer with the power that made it. */
final case class OfferedResolution(power: PowerId, offer: ResolutionOffer)
```

- [ ] **Step 4: Add the contribution and the host trait**

In `ContributingPower.scala`, add `OfferedResolution` and
`ResolutionOffer` to the `distantlands.model.{...}` import. After
`OfferReroll`, add:

```scala
/** Offers one power's steps to the node its window hooks, when that node is
  * a [[ResolutionHost]] (Setup's pawn window, catalog batch 13, ruling 4).
  * Like [[Offer]], it is asked again at every fold, so it must be a pure
  * function of state, and its offer must stand while its own steps run.
  */
final case class OfferResolution(
    offer: PowerCtx => Option[ResolutionOffer]
) extends Contribution
```

After `RerollHost`, add:

```scala
/** A windowed node that orders the powers its window offers: the walker
  * gathers the window's [[OfferResolution]]s, asks each for its offer, and
  * hands those that stand to `expand` in the deterministic power order,
  * with the same contract as [[OfferHost]]. The window's transforms fold
  * over the node's own children, which a host declares empty, so a power
  * hooked at a host's window offers its steps and folds no `Transform`
  * there.
  */
trait ResolutionHost extends Operation:
  def expand(offers: Vector[OfferedResolution], pass: OfferHost.Pass)
      : Vector[Operation]
```

- [ ] **Step 5: Gather the resolutions**

In `ContributionCollector.scala`:

- add `resolutions: Vector[(PowerId, OfferResolution)] = Vector.empty` as
  the last field of `GatheredContributions`;
- add `val resolutions = Vector.newBuilder[(PowerId, OfferResolution)]`
  beside `rerolls`;
- add `case resolution: OfferResolution => resolutions += id -> resolution`
  to the per-contribution match;
- pass `resolutions = resolutions.result()` to `GatheredContributions`.

Leave step 2b alone. A power that a power-use restriction refuses keeps
only its offers and rerolls, so its resolution is dropped: a refused power
does not resolve.

- [ ] **Step 6: Expand the host in the walker's fold**

In `WalkerPowerGather.applyWindow`, after the `case host: RerollHost =>`
arm, add:

```scala
          case host: ResolutionHost =>
            val offered = gathered.resolutions.flatMap {
              case (powerId, resolution) =>
                resolution.offer(ctxFor(byId(powerId))).map(
                  OfferedResolution(powerId, _))
            }
            (folded ++ host.expand(offered, OfferHost.Pass(state, answered,
              resuming, WalkerSimulation.applies(_, state, powers, procedure))),
              gathered.order)
```

Add `ResolutionHost` to the `powerresolver` import. Check whether
`OfferedResolution` is imported through `distantlands.model._` or needs
adding to an explicit import list. Update `applyWindow`'s scaladoc with
one sentence: "When the node is a [[ResolutionHost]], the resolutions the
window offered are turned into its children after the transforms have
run."

- [ ] **Step 7: Write `OrderedWindow`**

Create `src/main/scala/distantlands/gameplay/setup/OrderedWindow.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.powerresolver.{OfferHost, ResolutionHost}
import distantlands.model._

/** Setup's `SetupPawnPlaced` window for `placer` (catalog batch 13, ruling
  * 4): the powers that apply to the placer offer their steps, and the
  * placer orders them.
  *
  * One node is one round, and `resolved` names, by their options, the
  * powers the earlier rounds resolved. A round walks three slots:
  *
  *  - the placer's question, "Choose a power to resolve", over the offers
  *    not yet resolved, in power order. With one left it is answered alone
  *    (`Decide.answersAlone`), so every round records its pick;
  *  - the picked power's steps;
  *  - the next round, folded when the walk reaches it, so it gathers the
  *    window again against the live state: a power that no longer applies
  *    drops out, and a newly applicable one joins.
  *
  * A round with nothing left and no pick expands to nothing, which ends the
  * window. The shape reads only the offers and the answers, so a walk
  * resumed in any round rebuilds the rounds before it the same way, even
  * when what is offered has changed since. The pick is read when its slot
  * is reached, so the answer that resumes the question is seen.
  */
final case class OrderedWindow(placer: PlayerId,
    resolved: Vector[DecisionOptionRef]) extends ResolutionHost:
  override val window: Option[PowerWindow] =
    Some(PowerWindow.SetupPawnPlaced)
  override val children: Vector[Operation] = Vector.empty

  private val questionId: String =
    OrderedWindow.decisionId(placer, resolved.size)

  private def picked(answered: Vector[Answered]): Option[DecisionOptionRef] =
    answered.collectFirst {
      case Answered(`questionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }

  def expand(offers: Vector[OfferedResolution], pass: OfferHost.Pass)
      : Vector[Operation] =
    val remaining = offers.filterNot(offered =>
      resolved.contains(offered.offer.option.ref))
    if remaining.isEmpty && picked(pass.answered).isEmpty then Vector.empty
    else Vector(
      Sequence(Option.when(remaining.nonEmpty)(Decide(questionId, placer,
        DecisionQuery.ChooseOne(remaining.map(_.offer.option),
          heading = Some(OrderedWindow.heading),
          confirmVerb = Some(OrderedWindow.confirmVerb)),
        answersAlone = true)).toVector),
      Branch((_, pending) => picked(pending.answered).toVector.flatMap(ref =>
        offers.find(_.offer.option.ref == ref).toVector
          .flatMap(_.offer.steps))),
      Branch((_, pending) => picked(pending.answered).toVector.map(ref =>
        OrderedWindow(placer, resolved :+ ref))))

object OrderedWindow:
  val heading: String = "Choose a power to resolve"
  val confirmVerb: String = "Resolve"

  /** `placer`'s question in round `round` of their window, from 0. */
  def decisionId(placer: PlayerId, round: Int): String =
    s"setup.power-order.${placer.value}.$round"
```

If `Operation` declares `window` or `children` as `def`, keep the
`override val`, as `CampaignPlanChoice` does.

- [ ] **Step 8: Run the suite**

Run: `./sbtw "testOnly distantlands.gameplay.setup.OrderedWindowSuite"`
Expected: 5 tests pass. If the compiler reports a non-exhaustive match on
`Contribution` somewhere else, add an `OfferResolution` case beside that
match's `OfferReroll` case, with the same treatment.

- [ ] **Step 9: Check the new tests bite**

Temporarily replace `answersAlone = true` with `answersAlone = false` in
`OrderedWindow`, and run the suite. Expected: "a power the first one made
applicable joins" and "a walk resumed inside a power" fail, because the
walk parks. Restore the line.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala src/test/scala/distantlands/gameplay/setup/OrderedWindowSuite.scala
git commit -m "feat(walker): host a window whose placer orders its powers"
```

---

### Task 3: Setup's pawn window orders its powers

**Files:** `SetupProcedure.scala`, `PowerWindow.scala`,
`EdificeSetupSupport.scala`, `ProvingGroundsRules.scala`,
`GreatForgeRules.scala`, `TheStandardBearer.scala`, `Wealthy.scala`, and
the test files listed under "Test, modified" and `SetupPowerOrderSuite`.

**Interfaces:**
- Consumes: `OfferResolution`, `ResolutionOffer` and `OrderedWindow`
  (Task 2).
- Produces:
  - `EdificeSetupSupport.option(edifice: EdificeId): DecisionOption`
  - `TheStandardBearer.option: DecisionOption` and `Wealthy.option: DecisionOption`
  - `SetupWalkDriver.driveWithEvents(..., answers: Answers = PartialFunction.empty)`

- [ ] **Step 1: Let the Setup driver take answers**

In `src/test/scala/distantlands/gameplay/setup/SetupWalkDriver.scala`,
give `driveWithEvents` a last parameter
`answers: Answers = PartialFunction.empty` (import
`distantlands.testkit.Answers`), and answer with it before the default:

```scala
          val answer = Answered(decide.decisionId,
            answers.orElse(Situation.defaultAnswer).applyOrElse(park,
              _ => fail(s"SetupWalkDriver cannot auto-answer ${decide.query}")),
            decide.owner)
```

Mention `answers` in the scaladoc: "`answers` answers a park first; the
rest get `Situation.defaultAnswer`."

- [ ] **Step 2: Write the failing suite with real cards**

Create `src/test/scala/distantlands/gameplay/setup/SetupPowerOrderSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.powerresolver.{OfferResolution, PowerCtx}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.setup.{GreatForge, GreatForgeCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{TheStandardBearer,
  TheStandardBearerCard, Wealthy, WealthyCard}
import distantlands.gameplay.walker.{ChoicePayload, LogNoted, WalkerPowers,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Table}
import distantlands.testkit.Unwrap.{accepted, present}

/** The pawn window's power order with real cards (catalog batch 13,
  * rulings 4 and 5). l2, a Citizen, holds The Standard Bearer. Great Forge
  * stands intact at the first site in play, where the driver places every
  * pawn, so p1's window offers Great Forge alone and p2's offers both. */
class SetupPowerOrderSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    TheStandardBearerCard.id -> true)
  private val forge = DecisionOptionRef.Edifice(GreatForgeCard.id)
  private val bearer = DecisionOptionRef.Legacy(TheStandardBearerCard.id)
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  private val ready: ReadyGame =
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    Table.from(started).edifice(GreatForgeCard.id, EdificeSide.Intact,
      at = started.game.current.map.inPlay.head).ready

  private def walked(answers: Answers = PartialFunction.empty)
      : SetupWalkDriver.SetupWalked =
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private def forgeLine(note: LogNoted, player: PlayerId): Boolean =
    note.power == GreatForge.id &&
      note.note.args.headOption.contains(NoteArg.Player(player))

  /** Where in `events` Great Forge wrote its line for `player`. */
  private def forged(events: Vector[OathEvent], player: PlayerId): Int =
    events.indexWhere {
      case note: LogNoted => forgeLine(note, player)
      case _ => false
    }

  /** Where in `events` p2 answered The Standard Bearer. */
  private def bore(events: Vector[OathEvent]): Int = events.indexWhere {
    case WalkerStepRecorded(_, ChoicePayload(id, _, _), _) =>
      id == TheStandardBearer.decisionId(p2)
    case _ => false
  }

  test("with two powers applying, the placer chooses which resolves " +
      "first, listed edifice first (ruling 4; R27)"):
    val asked = walked().asked.filter(_.decisionId.startsWith(
      "setup.power-order."))
    assertEquals(asked.map(_.decisionId),
      Vector(OrderedWindow.decisionId(p2, 0)))
    assertEquals(asked.head.owner, p2)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(forge, bearer))
        assertEquals(one.heading, Some(OrderedWindow.heading))
      case other => fail(s"expected a choose-one, got $other")

  test("the first option keeps R27's order: Great Forge acts before The " +
      "Standard Bearer"):
    val events = walked().events
    val (atForge, atBearer) = (forged(events, p2), bore(events))
    assert(atForge >= 0 && atBearer >= 0, s"forge $atForge, bearer $atBearer")
    assert(atForge < atBearer, s"forge $atForge, bearer $atBearer")

  test("the power chosen first resolves first, and each resolves once " +
      "(ruling 4)"):
    val events = walked({
      case park if park.decisionId == OrderedWindow.decisionId(p2, 0) =>
        ChooseOneAnswer(bearer)
    }).events
    val (atForge, atBearer) = (forged(events, p2), bore(events))
    assert(atForge >= 0 && atBearer >= 0, s"forge $atForge, bearer $atBearer")
    assert(atBearer < atForge, s"forge $atForge, bearer $atBearer")
    assertEquals(events.count {
      case note: LogNoted => forgeLine(note, p2)
      case _ => false
    }, 1)

  test("one power left resolves without a question, and its pick is " +
      "recorded"):
    val run = walked()
    assert(!run.asked.exists(_.decisionId ==
      OrderedWindow.decisionId(p1, 0)), run.asked.map(_.decisionId))
    assert(run.events.exists {
      case WalkerStepRecorded(_, ChoicePayload(id, ChooseOneAnswer(ref),
          by), _) =>
        id == OrderedWindow.decisionId(p1, 0) && ref == forge && by == p1
      case _ => false
    })
    assert(forged(run.events, p1) >= 0)

  test("every power at the pawn window offers its steps and folds no " +
      "transform there"):
    val hooked = WalkerPowerCatalog.default(catalog).powers.flatMap(power =>
      power.contributions.getOrElse(PowerWindow.SetupPawnPlaced,
        Vector.empty).map(power.id -> _))
    assert(hooked.map(_._1).contains(TheStandardBearer.id), hooked)
    assertEquals(hooked.filterNot(_._2.isInstanceOf[OfferResolution]),
      Vector.empty)

  test("The Standard Bearer and Wealthy read their holder at their own " +
      "step: steps offered while p2 held them do nothing once p2 does not " +
      "(ruling 5)"):
    val both = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Citizen, TheStandardBearerCard.id -> true,
      WealthyCard.id -> true)
    val plain = LaterGameFixture.chronicle(l1, l2)
    val holding = accepted(GameStartRules.evolve(catalog, both,
      LaterGameFixture.orders(both, p1)))
    val without = accepted(GameStartRules.evolve(catalog, plain,
      LaterGameFixture.orders(plain, p1)))
    val placed = Vector(Answered(SetupProcedure.pawnDecisionId(p2),
      ChooseOneAnswer(DecisionOptionRef.Site(
        holding.game.current.map.inPlay.head)), p2))
    Vector(TheStandardBearer, Wealthy).foreach { power =>
      val ctx = PowerCtx(catalog, holding, p1, power.source,
        PowerWindow.SetupPawnPlaced, Vector.empty,
        OrderedWindow(p2, Vector.empty), answered = placed)
      val steps = present(power.contributions(PowerWindow.SetupPawnPlaced)
        .collectFirst { case resolution: OfferResolution =>
          resolution.offer(ctx) }.flatten, s"${power.id.value}'s offer").steps
      val run = SetupWalkDriver.driveWithEvents(without, Sequence(steps),
        TriggeredProcedureRef.Setup, powers)
      assertEquals(run.asked, Vector.empty[Decide], power.id.value)
      assertEquals(run.events, Vector.empty[OathEvent], power.id.value)
    }
```

If `SetupLegacyRules.wealthy` refuses a Citizen holder, give l2 the
`Role.Exile` in `both` and say so in the test's comment.

- [ ] **Step 3: Run it to see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupPowerOrderSuite"`
Expected: compile passes. The first, third, fourth, fifth and sixth tests
fail: nothing asks, nothing records a pick, the powers fold transforms,
and the steps act for a player who no longer holds the legacy. The second
test may already pass, since it pins today's order.

- [ ] **Step 4: Host the window in `SetupProcedure`**

In `playerStep`, replace the windowed pawn `Sequence` and its comment:

```scala
      // The pawn's line is written before the window, so it reads before
      // every power's. The arrival is read off the live state: this
      // player's pawn site is settled, since the next participant has not
      // placed yet.
      Sequence(Vector(
        BuildOps(placePawn(participant.playerId, pawnId)),
        Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
          placedPawnNote(participant.playerId)))),
      // The placer orders the powers that apply to them (catalog batch 13,
      // ruling 4).
      OrderedWindow(participant.playerId, Vector.empty),
```

In the scaladoc tree, replace the line
`BuildOps(place the pawn)                           // window = SetupPawnPlaced`
with:

```scala
 *     Sequence(BuildOps(place the pawn), Note(placed))
 *     OrderedWindow(participant)                         // window =
 *                                                        // SetupPawnPlaced; the
 *                                                        // placer orders its powers
```

In `PowerWindow.scala`, reword `SetupPawnPlaced`'s scaladoc:

```scala
  /** Where the placing player's site is fixed for this player's Setup turn
    * (2026-09-21 Chronicle design, "Setup powers"). Its node is an
    * `OrderedWindow`: the edifice powers, The Standard Bearer and Wealthy
    * offer their steps there, and the placer orders them (catalog batch
    * 13, ruling 4). */
```

- [ ] **Step 5: Offer the edifice powers' steps**

In `EdificeSetupSupport`, add:

```scala
  /** The option naming `edifice` in the placer's power-order question
    * (catalog batch 13, ruling 4). */
  def option(edifice: EdificeId): DecisionOption =
    DecisionOption.Edifice(DecisionOptionRef.Edifice(edifice))
```

In `ProvingGroundsRules.scala`, replace `ProvingGrounds.contributions`:

```scala
  /** The warbands, then the line. */
  private def steps(actor: PlayerId): Vector[Operation] = Vector(
    BuildOps((ready, _) => build(ready, actor)),
    Note(id, NoteSupport.gainedNote(ProvingGrounds.gained,
      PowerSourceRef.Card(edifice), actor, NoteUnit.Warband,
      NoteSupport.warbands)))

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (catalog batch 13, ruling 4). An Explore folds them in.
    */
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, _) => ResolutionOffer(
        EdificeSetupSupport.option(edifice), steps(actor))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, _) => ops ++ steps(actor)))))
```

In `GreatForgeRules.scala`, replace `ForgeRule.contributions`:

```scala
  /** The face's effect, then its line. */
  private def steps(actor: PlayerId, site: SiteId): Vector[Operation] =
    Vector(BuildOps((ready, _) => build(ready, actor, site)),
      note(actor, site))

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (catalog batch 13, ruling 4). An Explore folds them in.
    */
  final def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, site) => ResolutionOffer(
        EdificeSetupSupport.option(edifice), steps(actor, site))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, site) => ops ++ steps(actor, site)))))
```

Add `OfferResolution` to both files' `powerresolver` import.
`ResolutionOffer` comes in with `distantlands.model._`. Empty Grounds,
in `ProvingGroundsRules.scala`, hooks `SetupEnd` and is unchanged.

- [ ] **Step 6: Offer the legacies' steps and read the holder at the step**

In `TheStandardBearer.scala`, replace `contributions`, and add `option`
below it:

```scala
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      SetupPawnPlacement.latest(ctx.answered).map(_._1)
        .filter(player => SetupLegacyRules.standardBearer(ctx.state)
          .contains(player))
        .map(holder => ResolutionOffer(option, Vector(step(holder)))))))

  /** This legacy in the placer's power-order question. */
  val option: DecisionOption =
    DecisionOption.Legacy(DecisionOptionRef.Legacy(TheStandardBearerCard.id))
```

Then wrap the body of `step` in a holder check:

```scala
  // The holder is read again at the step, so a legacy lost earlier in the
  // window does not resolve (catalog batch 13, ruling 5).
  private def step(holder: PlayerId): Operation = Branch((state, pending) =>
    if !SetupLegacyRules.standardBearer(state).contains(holder) then
      Vector.empty
    else offered(state, pending, holder) match
      case Vector() => Vector.empty
      case banners => Vector(
        ...the existing Decide and Branch, unchanged...))
```

In the scaladoc, replace "In the holder's own `SetupPawnPlaced` window,
after the pawn's line," with "In the holder's own `SetupPawnPlaced`
window, in the order the holder chooses among its powers (catalog batch
13, ruling 4),". The Needle paragraph stays true: after the move the
holder still holds the legacy.

In `Wealthy.scala`, the same: `contributions` offers
`ResolutionOffer(option, Vector(step(holder)))` read through
`SetupLegacyRules.wealthy`, and `option` names `WealthyCard.id`:

```scala
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      SetupPawnPlacement.latest(ctx.answered).map(_._1)
        .filter(player => SetupLegacyRules.wealthy(ctx.state)
          .contains(player))
        .map(holder => ResolutionOffer(option, Vector(step(holder)))))))

  /** This legacy in the placer's power-order question. */
  val option: DecisionOption =
    DecisionOption.Legacy(DecisionOptionRef.Legacy(WealthyCard.id))
```

and `step` opens with the check:

```scala
  private def step(holder: PlayerId): Operation = Branch((state, _) =>
    // The holder is read again at the step, so a legacy lost earlier in
    // the window does not resolve (catalog batch 13, ruling 5).
    if !SetupLegacyRules.wealthy(state).contains(holder) then Vector.empty
    else
      val holding = givers(state, holder)
      ...the existing body, indented one level...)
```

In Wealthy's scaladoc, replace "after The Standard Bearer (R27)" with
"in the order the holder chooses among its powers (catalog batch 13,
ruling 4)". In both files, replace `Transform` with `OfferResolution` in
the `powerresolver` import.

- [ ] **Step 7: Move the stub in `GreatForgeRulesSuite` to an offer**

`AskInWindow` folds a `Transform` at `SetupPawnPlaced`. Under the host
that transform would fold again at every round. Replace it:

```scala
  /** A power that asks twice inside the first player's pawn window, after
    * Great Forge in power order, so the walk resumes there after a window
    * answer (R27). */
  private case object AskInWindow extends ContributingPower:
    val id: PowerId = PowerId("test.setup.ask-in-window")
    val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    val contributions: Map[PowerWindow, Vector[Contribution]] = Map(
      PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
        Option.when(SetupPawnPlacement.latest(ctx.answered)
            .exists(_._1 == firstPlayer))(ResolutionOffer(
          DecisionOption.Button(DecisionOptionRef.Button("ask"), "Ask"),
          Vector("test.window.ask", "test.window.ask2").map(id =>
            Decide(id, firstPlayer, DecisionQuery.ChooseOne(Vector(
              DecisionOption.Button(DecisionOptionRef.Button("a"), "A"),
              DecisionOption.Button(DecisionOptionRef.Button("b"), "B"))))))))))
```

Import `OfferResolution` in place of `Transform`, if nothing else uses
it, and `distantlands.gameplay.setup.SetupPawnPlacement`. The test still
expects the two asks and one relic. The power-order question comes first,
and the driver picks Great Forge, the first option.

In "the pawn's own line comes before the edifice's", reword the scaladoc:
"The pawn's line is written in the node before the window, so it reads
before any power's line, as the rule it replaced did."

- [ ] **Step 8: Move the R27 order test out of `TheStandardBearerSuite`**

Delete the test "in the holder's pawn window Great Forge acts before The
Standard Bearer (R27)". `SetupPowerOrderSuite` now pins the order the
question lists, and the default first answer keeps it. Remove the imports
the compiler then reports unused (likely `GreatForge`, `GreatForgeCard`,
`ChoicePayload`, `LogNoted`, `WalkerStepRecorded` and `Table`).

- [ ] **Step 9: Add the replay and log tests**

In `LaterGameSetupChoicesSuite`, import `OrderedWindow` and
`SetupLegacyFixture` from `distantlands.gameplay.setup`, and
`{TheStandardBearerCard, Wealthy, WealthyCard}` from
`distantlands.gameplay.setup.legacies`. Then add after the slice 3 replay
test:

```scala
  test("a later game saved inside a pawn window, after a power resolved " +
      "alone, replays its journal to the same game (catalog batch 13, " +
      "ruling 4)"):
    val l3 = LineageId("l3")
    val holding = SetupLegacyFixture.holding(record, l3, Role.Exile,
      TheStandardBearerCard.id -> true, WealthyCard.id -> true)
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    // p3 resolves The Standard Bearer first and declines it, the first
    // option. Wealthy, left alone, then resolves unasked and asks.
    val saved = Situation.start(Situation.journaled(service, catalog,
        repository, "ordering").withAnswers {
        case park if park.decisionId == OrderedWindow.decisionId(p3, 0) =>
          DecisionAnswer.ChooseOneAnswer(
            DecisionOptionRef.Legacy(TheStandardBearerCard.id))
      }).parkedAt(Wealthy.decisionId(p3))(GameCommand.Begin(holding,
        LaterGameFixture.orders(holding, p1, LaterGameFixture.seats(3))))
    parkedOn(saved) match
      case ParkedNode.Decision(_, decide, _) =>
        assertEquals(decide.decisionId, Wealthy.decisionId(p3))
      case other => fail(s"expected Wealthy's question, parked on $other")
    val loaded = present(accepted(service.load("ordering")), "the game")
    assertEquals(loaded.state, saved.state)
```

In `GameLogSetupSuite`, import `distantlands.gameplay.setup.OrderedWindow`
and add:

```scala
  test("the power-order pick writes no line of its own (catalog batch 13, " +
      "ruling 4)"):
    assert(ProcedureLog.of(TriggeredProcedureRef.Setup).narrates(
      OrderedWindow.decisionId(PlayerId("p2"), 0)))
```

In `GameLogNoteLinesSuite` (line ~393), reword the comment "a power hooked
at `SetupPawnPlaced` appends to the window's children, after the pawn's
own note" as "a power hooked at `SetupPawnPlaced` runs in the window,
after the pawn's own note".

- [ ] **Step 10: Run the Setup suites**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.powers.setup.* distantlands.application.LaterGameSetupChoicesSuite distantlands.application.gamelog.GameLogSetupSuite"`
Expected: all pass.

- [ ] **Step 11: Check the new tests bite**

Each check is temporary. Restore the code after each run.

- In `TheStandardBearer.step`, replace the holder check with `if false`.
  Expected: the ruling 5 test fails for `legacy.the-standard-bearer`.
- In `OrderedWindow`, replace `remaining.map(_.offer.option)` with
  `remaining.reverse.map(_.offer.option)`. Expected: the first and second
  `SetupPowerOrderSuite` tests fail.

- [ ] **Step 12: Run the goldens**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

A Setup whose placer meets one SETUP power now records one more step, its
answered-alone pick. If no golden changes, go to Step 13. If some fail,
regenerate them:

`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Then check that each changed golden holds the same lines in the same
order, ignoring sequence numbers. A scratch script, run from the worktree:

```python
import re, subprocess
files = subprocess.run(['git', 'diff', '--name-only',
    'src/test/resources/gamelog'], capture_output=True, text=True,
    check=True).stdout.split()
def lines(text):
    return [re.sub(r'@\d+', '@N', re.sub(r'^\d+\.\d+ ', '', l))
            for l in text.splitlines()]
for f in files:
    old = subprocess.run(['git', 'show', f'HEAD:{f}'], capture_output=True,
                         text=True, check=True).stdout
    print('SAME LINES' if lines(old) == lines(open(f).read())
          else 'DIFFERS', f)
```

Expected: every changed file prints SAME LINES. A DIFFERS is a defect,
since the pick writes no line and the default answer keeps R27's order.
Stop and read that diff before going on.

- [ ] **Step 13: Run the full JVM suite and the checks**

Run: `./sbtw test`, then `python3 scripts/check-architecture.py`.
Expected: all tests pass, and the check reports no violation. A test that
counts a Setup walk's recorded steps may now see one more step per window
that resolved a power. Update its expectation only after checking that
the extra step is the `setup.power-order.` pick.

- [ ] **Step 14: Commit**

```bash
git add src/main/scala src/test/scala src/test/resources/gamelog
git commit -m "feat(setup): let the placer order the powers of their pawn window"
```

---

### Task 4: Record the rulings and close the slice

**Files:** the docs listed above.

- [ ] **Step 1: Add the rulings to `docs/rules/ambiguities.md`**

In "Catalog batch 13 rulings", extend the intro's item list: after "item 8
is ruling 3, with the exile-only game ruled by the owner on 2026-11-15"
add "; items 9 and 10 are rulings 4 and 5". Then add after item 8:

```markdown
9. **The placer orders the powers in their pawn window.** Whenever two or more powers apply in a player's `SetupPawnPlaced` window, that player is asked "Choose a power to resolve", each power named by its card: the edifice at their site, The Standard Bearer, Wealthy, listed in that order ([Legacy Setup powers rulings](#legacy-setup-powers-rulings), item 47). The chosen power resolves. Then the window is gathered again against the live state, so a power that no longer applies drops out and a newly applicable one joins. Each power resolves at most once per window. With one power left it resolves without a question, and the walk records it as that round's answer, so a game resumed inside the window knows every power that resolved. The pick writes no line; each power's own line appears in the order chosen. Effects on several players are not ordered yet (a ROADMAP item).
10. **The Standard Bearer and Wealthy read their holder at their own step.** A legacy the holder lost earlier in the window does not resolve, and one activated earlier in the window resolves in it. No card does either yet; the Doors will (slice 5).
```

Amend Legacy Setup powers item 47. Replace "the edifice powers come
first, then The Standard Bearer, then Wealthy (R27)." with:

```markdown
the placer orders its powers whenever two or more apply, which are listed with the edifice powers first, then The Standard Bearer, then Wealthy ([Catalog batch 13 rulings](#catalog-batch-13-rulings), item 9; R27).
```

- [ ] **Step 2: Amend R27 in the Legacy Setup powers implementation rulings**

In `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`,
replace:

```markdown
  stayed off. Inside the window the edifice powers come first, then The
  Standard Bearer, then Wealthy, by registration order, which a test pins.
```

with:

```markdown
  stayed off. Inside the window the edifice powers, then The Standard
  Bearer, then Wealthy, by registration order, are the order in which the
  placer's power-order question lists them. The placer orders them
  whenever two or more apply (catalog batch 13, ruling 4), and
  `SetupPowerOrderSuite` pins the list.
```

- [ ] **Step 3: Add the Digital note to `docs/rules/rules-reference.md`**

On Later games step 7, after "(items 42, 43 and 47)." insert:

```markdown
When two or more of these powers apply, the edifice at the player's site among them, the player chooses which resolves first, then next ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 9 and 10).
```

- [ ] **Step 4: Update `docs/rules/implementation-traceability.md`**

In the "Legacy SETUP powers in the player choices" row, replace "The
Standard Bearer and Wealthy hook the holder's own `SetupPawnPlaced` window
after the edifice powers;" with:

```markdown
The Standard Bearer and Wealthy hook the holder's own `SetupPawnPlaced` window, whose placer orders its powers when two or more apply (`OrderedWindow`; `setup/OrderedWindowSuite`, `setup/SetupPowerOrderSuite`, `walker/AnswersAloneSuite`; [Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 9 and 10);
```

- [ ] **Step 5: Narrow the ROADMAP item**

In `docs/ROADMAP.md`, replace the **Simultaneous setup effects** item's
text with:

```markdown
- [ ] **Simultaneous setup effects.** The Chancellor or first player should
  resolve simultaneous setup effects on several players; site order is used
  until then. A player orders the powers of their own pawn window (catalog
  batch 13, ruling 4).
```

- [ ] **Step 6: Update the spec**

In the spec's "Slice 4: power order" approach section, replace "A new
`OfferHost`, `OrderedWindow`, wraps the `SetupPawnPlaced` fold." with "A
new host beside `OfferHost` and `RerollHost`, the `ResolutionHost`
`OrderedWindow`, is the `SetupPawnPlaced` window, and its powers offer
their steps through `OfferResolution`." After "(the `OfferHost`
contract).", add: "A power that resolves alone is still recorded as its
round's answer, through a choose-one the walker answers itself
(`Decide.answersAlone`), so every round's pick is an answer."

In the status line, after "Slice 3 landed 2026-11-15." add "Slice 4
landed 2026-11-15." Use the date the slice actually lands.

- [ ] **Step 7: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`. Expected: no broken link.

```bash
git add docs
git commit -m "docs(catalog): record the power order and close batch 13 slice 4"
```
