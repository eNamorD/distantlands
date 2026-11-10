# Catalog Batch 11, Slice 4: Wild Pastures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Wild Pastures (edifice E07, intact): "Whenever this
card's ruler would discard a Nomad card from a power, they may instead pay
1 favor to the Nomad bank."

**Architecture:** Two seams, then the card.

1. **A cause on the discard.** `Discard.Denizen` and
   `Discard.RuinedEdifice` gain `cause: Option[DiscardCause]`: the power
   whose use discards the card, and whether the discard makes room that
   power then fills. Every power's discard sets it; a rule's discard has
   none.
2. **Batch questions.** Before the walker runs a `BuildOps` leaf's batch,
   each `AsksBeforeBatch` power may ask about any of its operations. When
   one asks, the walker walks a generated subtree under the leaf's node:
   - the questions;
   - the batch, rebuilt from the unchanged state, with each taken answer's
     replacement in place of its operation;
   - each replacement's line.

   A resume rebuilds the same subtree, as a trigger's follow-up is rebuilt
   today.
3. **The card.** Wild Pastures asks about the discards of Nomad cards that
   come from its ruler's power uses, and replaces a "pay" answer with a
   `Give` of 1 favor to the Nomad bank.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read rulings 11, 12 and 20 to 25, the Wild Pastures bullet of "Readings",
and "Slice 4: A question before a batch".

## Prerequisite

This branch (`batch-11-slice-4`) was cut from `batch-11-slice-3` at its
plan commit `5e1bbd20`, so it carries ruling 19. Slice 4 uses none of slice
3's code, but both slices edit the spec, the codec
(`serialization/WalkerOperationCodec.scala`, in different cases), the
roadmap and `src/test/resources/powers/unimplemented.txt`.

Before Task 1:

1. Merge slice 3 into `0.2.0-alpha`.
2. Rebase this branch onto `0.2.0-alpha`.

Git drops this branch's copy of the slice 3 plan commit, because the
merged slice carries the same patch. If a "before" snippet below does not
match after the rebase, the difference comes from slice 3: keep slice 3's
lines, and apply this plan's change beside them.

## Plan-time findings

Inventoried at `5e1bbd20`, which includes slice 2's code
(`CardRule.rulerAt`).

1. **The walk context cannot say which power built a batch.** This is why
   the spec's fallback, a cause on the discard, is used.
   - `WalkCtx.procedure` is set once per walk. It names a power only for a
     phase power's own walk (`ActionRef.UsePower`). It is the same for a
     card play embedded in that walk: Temple of Ancestors playing
     Threatening Roar runs Roar's discard under Temple's procedure.
   - A leaf's recorded `contributions` are every power gathered at every
     enclosing window (`WalkerHooks.inherited`). A Search's own discard
     batch therefore lists a picked Bracken or Hunting Party, and every
     battle plan's id appears in the Campaign.
2. **Every power's discard is built inside a `BuildOps` batch.** No
   production tree holds a `Discard.Denizen` node outside one, and no
   `Take.leaving` holds a discard. The question therefore hooks only
   `BuildOps` leaves. It still reads a `Take`'s leaving, so that a later
   nesting is not missed (project rule: batch predicates flatten).
3. **The discards, and the cause each gets.** The power ids below are the
   `PowerId` values.

   | Builder | Callers | Cause |
   |---|---|---|
   | `RegionDiscard` (`powers/whenplayed/RegionDiscard.scala:72-83`) | Dazzle, Threatening Roar, Riots, Forest Horn | the caller's power |
   | `SiteCards.discard` (`powers/action/SiteCards.scala:34-46`) | Dark Enforcer, Great Feast, Taming Charm, Mounted Library | the caller's power |
   | `SiteCards.discard`, through `SiteSlotMoves.operations` (`:64-75`) | Roving Terror, Giant Python | the mover's power; makes room when the target site was full (ruling 20) |
   | Horned Mask (`powers/wake/HornedMask.scala:182`) | | `relic.horned-mask`; always makes room, since it discards only at the limit |
   | Armed Mob (`powers/action/ArmedMob.scala:163`) | | `denizen.armed-mob` |
   | Assassin (`powers/action/Assassin.scala:108`) | | `denizen.assassin` |
   | Squalid District (`powers/action/SqualidDistrict.scala:124`) | | `edifice.e21.ruined`. Its cards are facedown, so Wild Pastures never asks (ruling 25). |
   | Empty Grounds (`powers/setup/ProvingGroundsRules.scala:120-128`) | | `edifice.e22.ruined`. It is actorless, so no ruler is its actor. |
   | `PlanDiscard` (`powers/campaign/PlanDiscard.scala`) | the "at end" plans, Mercenaries, Wild Mounts' swap | the plan's power, only when the plan has a player user. A bandit defender's plan has none: the bandits use it, not the attacker who is its acting player. |
   | Spinning Bee (`powers/search/SpinningBee.scala:154-169`, through `CardPlay.operations`) | | `denizen.spinning-bee`. Its cards are in a hand, so Wild Pastures never asks (ruling 25). |
   | Card play (`actions/CardPlay.scala:301,303,333`), through `CardPlayProcedure`'s `play` Branch | Search, the facedown-adviser action, Search Party, Oracle, Temple | Task 4 |
   | Shape Empire (`chronicle/ShapeEmpire.scala:104,107`) | | none (Chronicle rule) |
   | Search (`actions/search/SearchProcedure.scala:223-232`) | | none (rule) |

   Bracken, Cracked Horn and Ring of Devotion rewrite the Search's own
   discards. They copy the op, so the copy keeps its missing cause: still a
   rule discard. Wild Mounts holds a plan's discard and later re-emits the
   op unchanged, so the discard keeps the plan's cause.
4. **Card play, by ruling 22.** These are the discards of a play:
   - **Site removals.** `CardSlots.credits` matches each removal, in
     order, to the permission it spends: Homeland (a rule), Baron, Crop
     Rotation or the Mob face. A removal spending a power's permission
     gets that power as its cause. It makes room when the site was full
     before the play.
   - Execution note: only the replaced card of a full site makes room; an
     extra discard does not. A removal on Homeland's permission takes
     `byPower` when a power made the play (ruling 22). `mark` takes
     `roomFor: Option[CardId]` in place of the plan's `siteFull`.
   - **A play-area adviser the play replaces.** This always makes room. It
     gets the power that made the play (`byPower`), if any.
   - **The played card discarded** (placement Discard). It gets `byPower`,
     if any.

   Search Party's played card comes from a discard pile, so Wild Pastures
   asks about it (ruling 25), and a "pay" leaves it in that pile.
5. **Riots needs no change.** Its burn counts the targets that no site
   holds after the discard step (`RegionDiscard.gone`), so a card paid for
   is not counted (ruling 24). Taming Charm's gain and Mounted Library's
   doubling sit in the same batch, or before it, and run as built.
6. **Where the subtree lives.**
   - A leaf whose batch asks is walked as a `Sequence` at
     `path :+ "asked"`: a `Branch` at `asked.0` holds the questions
     (`asked.0.0` to `asked.0.(n-1)`) and then the lines; the batch is at
     `asked.1`.
   - Execution note: the plan first put the questions at `asked.0` to
     `asked.(n-1)`, the batch at `asked.n` and the lines after it. A resume
     inside the batch's follow-ups rebuilt the questions from the state
     after the batch, where a power may no longer ask, so `n` moved. The
     fixed shape keeps the batch at `asked.1`, and the `Branch` selects its
     questions only when the walk enters it. The code and tests in Task 2
     below show the first shape; the committed code has the fixed one.
   - Execution note: inside `asked.0`, each operation has its own `Branch`
     (`asked.0.i`, its question at `asked.0.i.0`), selected after the
     answers before it, and `BatchContext.taken` carries the replacements
     those answers took. A ruler with 1 favor and two Nomad discards in one
     batch is asked once.
   - The batch directly under an `asked` segment asks nothing again
     (`BatchQuestions.answering`).
   - The segment is not a number, so `WalkerPowerGather.resolveAt` checks
     for it before its `ExecutedTriggers` case. That case also matches a
     leaf whose batch is held, which the batch at `asked.1` can be once it
     runs and starts a follow-up.
   - Only two places read past a leaf: `resolveAt` and
     `ProcedureWalker.runNarrowed` (`grep -rn 'ExecutedTriggers.holds' src/main/scala`).
7. **A search walks the batch as built.** A search is the look-ahead that
   narrows a decision's options. It does not ask; it treats every question
   as declined. Declining is always possible and leaves the batch as
   built.
8. **The answers.** The rebuilt batch and the lines read each question's
   latest answer (`answered.reverseIterator`), as a `Repeat` reads its
   latest pass. A decision id names its card
   (`wild-pastures.denizen.153`), so two questions on one batch differ.
9. **The log.** The "Discard it" answer writes no line of the power's own,
   so the decision is not listed in `narratedDecisions` (see that
   member's doc), and the log posts its usual "Chose ..." line. The ids are
   per card in any case.
10. **Card move.** `WildPasturesCard` moves from
    `catalog/holding/HoldingEdifices.scala:38-51` into the new power file,
    in `gameplay.powers.economy`. `NewFoundations.scala:4,18` imports both
    packages with a wildcard, so nothing else changes. The ruined face,
    Blasted Pastures, is a Chronicle step and stays unimplemented.
11. **The ruler.** The ruler is read with `CardRule.rulerAt` at the
    edifice's site, as `BannerGuardians.scala:49-50` reads it. A Grand Mask
    wearer is therefore the ruler during their turn (ruling 11).

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-4`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Test helper names say what they build.
- Log lines name only non-actors.
- Old journals need not decode (wire changes may require new fields).

---

### Task 1: The discard's cause

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (above `sealed trait Discard`; `Discard.Denizen`; `Discard.RuinedEdifice`)
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` (encode at `:91-109`, decode at `:370-393`)
- Test: `src/test/scala/distantlands/model/DiscardCauseSuite.scala` (create)
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces:
  - `final case class DiscardCause(power: PowerId, makesRoom: Boolean = false)`;
  - `DiscardCause.mark(operations: Vector[CoreOperation], cause: DiscardCause): Vector[CoreOperation]`;
  - `Discard.Denizen(..., cause: Option[DiscardCause] = None)`;
  - `Discard.RuinedEdifice(..., cause: Option[DiscardCause] = None)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/model/DiscardCauseSuite.scala`:

```scala
package distantlands.model

import distantlands.testkit.Table.p1

/** A discard's cause (catalog batch 11, slice 4): the power whose use
  * discards a card, and whether the discard makes room that power fills. */
class DiscardCauseSuite extends munit.FunSuite:
  private val atSite = PositionedLocation(Location.Site(SiteId("site")))
  private val denizenDiscard = Discard.Denizen(DenizenId("D1"), atSite,
    Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val edificeDiscard = Discard.RuinedEdifice(EdificeId("E1"),
    atSite, Suit.Nomad, 0, 0, p1)
  private val visionDiscard = Discard.Vision(VisionId("vision:v1"),
    PositionedLocation(Location.PlayArea(p1)), Region.Cradle)
  private val cause = DiscardCause(PowerId("denizen.taming-charm"))

  test("a discard has no cause unless its builder gives one"):
    assertEquals(denizenDiscard.cause, None)
    assertEquals(edificeDiscard.cause, None)
    assertEquals(cause.makesRoom, false)

  test("marking gives every denizen and ruined-edifice discard the cause, " +
      "and leaves other operations alone"):
    val spend = SpendSupply(p1, 1)
    assertEquals(DiscardCause.mark(Vector(denizenDiscard, edificeDiscard,
      visionDiscard, spend), cause), Vector[CoreOperation](
      denizenDiscard.copy(cause = Some(cause)),
      edificeDiscard.copy(cause = Some(cause)), visionDiscard, spend))
```

In `WalkerOperationWireSuite.scala`, after the test "an actorless discard
says so on the wire", add:

```scala
  test("a discard's cause round trips, and a discard without one writes " +
      "none"):
    val atSite = PositionedLocation(Location.Site(SiteId("site")))
    val cause = DiscardCause(PowerId("denizen.roving-terror"),
      makesRoom = true)
    val caused = Vector[CoreOperation](
      Discard.Denizen(DenizenId("D1"), atSite, Region.Provinces, Suit.Nomad,
        0, 1, Table.p1, cause = Some(cause)),
      Discard.RuinedEdifice(EdificeId("E1"), atSite, Suit.Nomad, 1, 0,
        Table.p1, cause = Some(DiscardCause(PowerId("denizen.riots")))))
    caused.foreach { discard =>
      val encoded = encodedStream("walker", Vector(recordedOperation(discard)))
      assertEquals(accepted(GameEventWire.decodeStream(encoded))
        .map(_.event), Vector(recordedOperation(discard)))
    }
    val plain = Discard.Denizen(DenizenId("D1"), atSite, Region.Provinces,
      Suit.Nomad, 0, 0, Table.p1)
    val written = ujson.read(encodedStream("walker",
      Vector(recordedOperation(plain)))).arr
    assert(!written(0)("payload")("step")("ops")(0).obj.contains("cause"))
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.model.DiscardCauseSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: compile errors: `cause` is not a member of `Discard.Denizen`, and
`DiscardCause` is not found.

- [ ] **Step 3: Add the cause to the model**

In `model/CoreOperations.scala`, directly above `sealed trait Discard
extends CoreOperation`, add:

```scala
/** Why a power discards a card (catalog batch 11, slice 4): `power`, whose
  * use discards it, and whether the discard `makesRoom` that same power
  * then fills (a mover's full target site, an adviser area at its limit).
  * A discard without a cause is a rule's: a Search's own, a plain card
  * play's, the Chronicle's. Wild Pastures reads it, since the walk itself
  * cannot tell which power built a batch.
  */
final case class DiscardCause(power: PowerId, makesRoom: Boolean = false)

object DiscardCause:
  /** `operations` with every denizen and ruined-edifice discard among them
    * caused by `cause`. A power marks its own batch, which nests no
    * discard in a `Take`'s leaving. */
  def mark(operations: Vector[CoreOperation], cause: DiscardCause)
      : Vector[CoreOperation] = operations.map {
    case discard: Discard.Denizen => discard.copy(cause = Some(cause))
    case discard: Discard.RuinedEdifice => discard.copy(cause = Some(cause))
    case other => other
  }
```

Add the field last in `Discard.Denizen`'s parameter list, after
`position`:

```scala
      position: StackPosition = StackPosition.Top,
      cause: Option[DiscardCause] = None)
```

and last in `Discard.RuinedEdifice`'s, after `actorless`:

```scala
      override val required: Boolean = false, actorless: Boolean = false,
      cause: Option[DiscardCause] = None)
```

Add one sentence to each class's doc comment: "`cause` is the power whose
use discards it ([[DiscardCause]])."

- [ ] **Step 4: Put the cause on the wire**

In `WalkerOperationCodec.scala`, beside `withActorless` (line 162), add:

```scala
  /** A discard's cause, written only when it has one. */
  private def withCause(encoded: ujson.Obj, cause: Option[DiscardCause])
      : ujson.Obj =
    cause.foreach(found => encoded("cause") = ujson.Obj(
      "powerId" -> found.power.value, "makesRoom" -> found.makesRoom))
    encoded

  /** A discard's cause, when it names one. */
  private def decodeCause(value: ujson.Value, path: String)
      : Either[WireError, Option[DiscardCause]] =
    value.obj.get("cause").fold[Either[WireError, Option[DiscardCause]]](
        Right(None)) { found =>
      found.obj.get("powerId").map(_.str).flatMap(PowerId.fromValue)
        .toRight(WireError.InvalidValue(s"$path.cause.powerId",
          "a discard's cause names its power"))
        .map(power => Some(DiscardCause(power,
          found.obj.get("makesRoom").exists(_.bool))))
    }
```

Change the two encode cases (lines 91-109) to bind the cause and write it:

```scala
      case Discard.Denizen(card, from, to, suit, favor, secrets, actingPlayer,
          _, actorless, position, cause) =>
        withCause(withActorless(ujson.Obj("kind" -> "discard-denizen",
          "card" -> card.value,
          "from" -> encodePositionedLocation(from), "to" -> to.key,
          "position" -> encodeStackPosition(position),
          "suit" -> suit.key, "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value), actorless), cause)
```

```scala
      case Discard.RuinedEdifice(card, from, suit, favor, secrets,
          actingPlayer, _, actorless, cause) =>
        withCause(withActorless(ujson.Obj("kind" -> "discard-ruined-edifice",
          "card" -> card.value,
          "from" -> encodePositionedLocation(from), "suit" -> suit.key,
          "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value), actorless), cause)
```

In the two decode cases, add a generator and pass the cause:

```scala
    case "discard-denizen" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodeRegion(value("to").str, s"$path.to")
      position <- discardPosition(value, path)
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
      cause <- decodeCause(value, path)
    yield Discard.Denizen(DenizenId(value("card").str), from, to, suit,
      favor, secrets, PlayerId(value("actingPlayerId").str),
      actorless = decodeActorless(value), position = position, cause = cause)
```

```scala
    case "discard-ruined-edifice" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
      cause <- decodeCause(value, path)
    yield Discard.RuinedEdifice(EdificeId(value("card").str), from, suit,
      favor, secrets, PlayerId(value("actingPlayerId").str),
      actorless = decodeActorless(value), cause = cause)
```

If `PowerId.fromValue` is not visible in the codec, use the form line 665
already uses, `PowerId(found("powerId").str)`, inside the `fold`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.model.DiscardCauseSuite distantlands.serialization.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/test/scala/distantlands/model/DiscardCauseSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(model): a discard names the power whose use discards it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Batch questions in the walker

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powerresolver/AsksBeforeBatch.scala`
- Create: `src/main/scala/distantlands/gameplay/walker/BatchQuestions.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala` (`runNarrowed`, about lines 469-530)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala` (`resolveAt`, about lines 563-584)
- Test: `src/test/scala/distantlands/gameplay/walker/BatchQuestionsSuite.scala` (create)

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces, in `distantlands.gameplay.powerresolver`:
  - `final case class BatchContext(catalog: ExecutableCatalog, actor: PlayerId, ready: ReadyGame)`;
  - `final case class Replacement(operations: Vector[CoreOperation], line: LogNote)`;
  - `final case class BatchAsk(decide: Decide, instead: DecisionAnswer => Option[Replacement])`;
  - `trait AsksBeforeBatch extends ContributingPower` with
    `def watching(ready: ReadyGame): Boolean` and
    `def askBefore(batch: BatchContext, operation: CoreOperation): Option[BatchAsk]`.
- Produces, in `distantlands.gameplay.walker` (package-private):
  - `BatchQuestions.segment: String` (`"asked"`);
  - `BatchQuestions.answering(path: Vector[String]): Boolean`;
  - `BatchQuestions.subtree(build: BuildOps, ready: ReadyGame, answered: Vector[Answered], powers: WalkerPowers, actor: PlayerId, path: Vector[String]): Either[OathViolation, Option[Operation]]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/walker/BatchQuestionsSuite.scala`:

```scala
package distantlands.gameplay.walker

import distantlands.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, ExecutedBatch, ExecutedTrigger, Replacement}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Outcomes.{finished, parked}
import distantlands.testkit.ProcedureWalkerFixture._
import distantlands.testkit.RecordedOps
import distantlands.testkit.WalkerStubs.{continueOption, stopOption}

/** The walker's batch questions (catalog batch 11, slice 4): a power asks
  * about an operation of a `BuildOps` batch before it runs, and an answer
  * may put a replacement in the operation's place. */
class BatchQuestionsSuite extends munit.FunSuite:
  /** A test power: before a batch, asks about each of the actor's Supply
    * spends, by amount. Continue moves the pawn to S3 instead of the
    * spend; Stop keeps it. `watches` says whether it looks at all. */
  private final class AsksBeforeEachSupplySpend(watches: Boolean)
      extends AsksBeforeBatch:
    val id: PowerId = PowerId("test.asks-before-each-supply-spend")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    private val notes = NoteSet(id)
    val movedInstead: NoteKey = notes.key("moved-instead",
      Vector(NotePart.Text("Moved instead of spending.")))
    override def noteKeys: Vector[NoteKey] = notes.declared
    def watching(ready: ReadyGame): Boolean = watches
    def askBefore(batch: BatchContext, operation: CoreOperation)
        : Option[BatchAsk] = operation match
      case spend: SpendSupply if spend.player == actor => Some(BatchAsk(
        Decide(spendDecision(spend.amount), actor, DecisionQuery.ChooseOne(
          Vector(DecisionOption.Button(continueOption, "Move instead"),
            DecisionOption.Button(stopOption, "Spend")))),
        answer => Option.when(answer == ChooseOneAnswer(continueOption))(
          Replacement(Vector(movePawnToS3),
            movedInstead(PowerSourceRef.Card(DenizenId("D1")))))))
      case _ => None

  private def spendDecision(amount: Int): String = s"test.spend.$amount"

  /** A test trigger: after a batch that moves the actor's pawn, asks the
    * fixture's continue-or-stop question. */
  private object AsksAfterThePawnMoves extends ExecutedTrigger:
    val id: PowerId = PowerId("test.asks-after-the-pawn-moves")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.contains(movePawnToS3))(Sequence(decide,
        BuildOps((_, _) => Right(Vector[CoreOperation](spendOneSupply)))))

  private val watching = WalkerPowers(Vector(
    AsksBeforeEachSupplySpend(watches = true)))
  private val notWatching = WalkerPowers(Vector(
    AsksBeforeEachSupplySpend(watches = false)))

  /** One batch, at child 0, that spends `amounts` of Supply in order. */
  private def batchSpending(amounts: Int*): Operation =
    Sequence(Vector[Operation](BuildOps((_, _) => Right(
      amounts.toVector.map(SpendSupply(actor, _): CoreOperation)))))

  private def steps(events: Vector[OathEvent]): Vector[WalkerStepRecorded] =
    events.collect { case step: WalkerStepRecorded => step }

  private def answered(amount: Int, option: DecisionOptionRef): Answered =
    Answered(spendDecision(amount), ChooseOneAnswer(option), actor)

  test("a watched batch parks on its question under the leaf's node, and " +
      "nothing runs"):
    val park = parked(ProcedureWalker.advance(ready, batchSpending(1), None,
      watching))
    assertEquals(park.tree.at, Vector("0", BatchQuestions.segment, "0"))
    assertEquals(park.events, Vector.empty[OathEvent])
    assertEquals(ProcedureWalker.parkedDecide(ready, batchSpending(1),
      PendingTree(at = park.tree.at, answered = Vector.empty), watching)
      .map(_.decisionId), Some(spendDecision(1)))

  test("the answer that takes the replacement runs it in the operation's " +
      "place, then writes its line"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), watching))
    assertEquals(steps(done.events).map(_.nodeId),
      Vector("0.asked.0", "0.asked.1"))
    assertEquals(RecordedOps.of(steps(done.events)(1)),
      Vector[CoreOperation](movePawnToS3))
    assertEquals(done.events.count(_.isInstanceOf[LogNoted]), 1)
    assertEquals(pawnSiteOf(done.treeless), Some(sites(2)))
    assertEquals(supplyOf(done.treeless), supplyOf(ready))

  test("the other answer runs the batch as built, with no line"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, stopOption), watching))
    assertEquals(RecordedOps.of(steps(done.events)(1)),
      Vector[CoreOperation](spendOneSupply))
    assertEquals(done.events.count(_.isInstanceOf[LogNoted]), 0)
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 1)

  test("two watched operations ask in order, and each answer replaces its " +
      "own operation only"):
    val tree = batchSpending(1, 2)
    val first = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val second = parked(ProcedureWalker.resolve(applyEvents(ready,
      first.events), tree, first.tree, answered(1, continueOption), watching))
    assertEquals(second.tree.at, Vector("0", BatchQuestions.segment, "1"))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      first.events ++ second.events), tree, second.tree,
      answered(2, stopOption), watching))
    assertEquals(RecordedOps.of(steps(done.events).last),
      Vector[CoreOperation](movePawnToS3, SpendSupply(actor, 2)))
    assertEquals(steps(done.events).last.nodeId, "0.asked.2")

  test("the journal replays to the state the walk reached"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), watching))
    val replayed = applyEvents(ready, steps(park.events ++ done.events))
    assertEquals(pawnSiteOf(replayed), pawnSiteOf(done.treeless))
    assertEquals(supplyOf(replayed), supplyOf(done.treeless))

  test("with no power watching, the batch runs at its own node"):
    val done = finished(ProcedureWalker.advance(ready, batchSpending(1), None,
      notWatching))
    assertEquals(steps(done.events).map(_.nodeId), Vector("0"))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 1)

  test("a trigger's follow-up after the answered batch parks under that " +
      "batch's node, and its position resolves to its question"):
    val powers = WalkerPowers(Vector(
      AsksBeforeEachSupplySpend(watches = true), AsksAfterThePawnMoves))
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val followed = parked(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), powers))
    assertEquals(followed.tree.at,
      Vector("0", BatchQuestions.segment, "1", "0", "0"))
    val atPark = applyEvents(ready, steps(park.events ++ followed.events))
    assertEquals(ProcedureWalker.parkedDecide(atPark, tree, followed.tree,
      powers), Some(decide))
```

`applyEvents` folds recorded steps only, so the replay test passes it the
steps.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.BatchQuestionsSuite"`
Expected: compile errors: `AsksBeforeBatch`, `BatchAsk`, `BatchContext`,
`Replacement` and `BatchQuestions` are not found.

- [ ] **Step 3: Write the power-side types**

Create `src/main/scala/distantlands/gameplay/powerresolver/AsksBeforeBatch.scala`:

```scala
package distantlands.gameplay.powerresolver

import distantlands.catalog.ExecutableCatalog
import distantlands.model.{CoreOperation, Decide, DecisionAnswer, LogNote,
  PlayerId, ReadyGame}

/** What a power reads when it looks at a walker batch before the batch
  * runs (catalog batch 11, slice 4): the walk's catalog, the walk's actor,
  * and the state the batch was built in. */
final case class BatchContext(catalog: ExecutableCatalog, actor: PlayerId,
    ready: ReadyGame)

/** What an answer puts in an operation's place, and the line that says
  * so. */
final case class Replacement(operations: Vector[CoreOperation],
    line: LogNote)

/** One question about one operation of a batch: `decide`, and the
  * replacement an answer takes. An answer `instead` maps to nothing leaves
  * the operation as built. */
final case class BatchAsk(decide: Decide,
    instead: DecisionAnswer => Option[Replacement])

/** A power that may ask about an operation of a walker batch before the
  * batch runs (catalog batch 11, slice 4), beside `TakesBurns` and
  * `ExecutedTrigger`. The walker asks each such power, in the order
  * contributions chain, about each operation of a `BuildOps` batch, a
  * `Take`'s leaving included; the first to ask about an operation asks the
  * only question about it. A question's decision id must name what it asks
  * about, so two questions on one batch differ and a rebuilt batch meets
  * the same answers. `watching` is a cheap test of whether the power can
  * ask at all: while no power watches, the walker builds nothing extra.
  */
trait AsksBeforeBatch extends ContributingPower:
  def watching(ready: ReadyGame): Boolean
  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk]
```

- [ ] **Step 4: Write the walker side**

Create `src/main/scala/distantlands/gameplay/walker/BatchQuestions.scala`:

```scala
package distantlands.gameplay.walker

import distantlands.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, ContributingPower, Replacement}
import distantlands.model.{Answered, BuildOps, CoreOperation, Note,
  OathViolation, Operation, PendingTree, PlayerId, PowerId, ReadyGame,
  Sequence, Take}

/** The walker's side of batch questions (catalog batch 11, slice 4). Before
  * a `BuildOps` leaf's batch runs, each power that asks before a batch
  * looks at its operations, a `Take`'s leaving included. When one asks,
  * the leaf is walked as a subtree under `path :+ segment`:
  *
  *  - the questions, in operation order;
  *  - the batch, rebuilt from the unchanged state, with each taken
  *    answer's replacement in its operation's place;
  *  - each replacement's line.
  *
  * The batch in the subtree asks nothing again. A resume rebuilds the same
  * subtree, so a builder reads only state and answers, as every `BuildOps`
  * does, and the questions change nothing. A search does not ask: it walks
  * the batch as built, which declining every question leaves. It names no
  * card.
  */
private[walker] object BatchQuestions:
  /** The path segment under a leaf where its batch's questions are
    * walked. It is not a number, so no composite's child shares it. */
  val segment: String = "asked"

  /** A question one power asks about one operation. */
  private final case class Asked(power: PowerId, operation: CoreOperation,
      ask: BatchAsk)

  /** Whether the leaf at `path` is the batch a question subtree runs. */
  def answering(path: Vector[String]): Boolean =
    path.dropRight(1).lastOption.contains(segment)

  /** The subtree that asks about `build`'s batch at `path`, or `None` when
    * no power asks. */
  def subtree(build: BuildOps, ready: ReadyGame, answered: Vector[Answered],
      powers: WalkerPowers, actor: PlayerId, path: Vector[String])
      : Either[OathViolation, Option[Operation]] =
    val asking = askers(powers).filter(_.watching(ready))
    if asking.isEmpty || answering(path) then Right(None)
    else build.build(ready, PendingTree(at = path, answered = answered))
      .map { operations =>
        val asked = of(asking, powers, actor, ready, operations)
        Option.when(asked.nonEmpty)(Sequence(asked.map(_.ask.decide) ++
          Vector(BuildOps((now, pending) =>
            build.build(now, pending.copy(at = path)).map(rebuilt =>
              replaced(rebuilt, of(asking, powers, actor, now, rebuilt),
                pending.answered)), required = build.required)) ++
          asked.map(one => Note(one.power, states =>
            answer(states.answered, one).map(_.line)))))
      }

  private def askers(powers: WalkerPowers): Vector[AsksBeforeBatch] =
    powers.powers.collect { case power: AsksBeforeBatch => power }
      .sortBy(ContributingPower.sortKey)

  /** The first asking power's question on each operation. */
  private def of(asking: Vector[AsksBeforeBatch], powers: WalkerPowers,
      actor: PlayerId, ready: ReadyGame, operations: Vector[CoreOperation])
      : Vector[Asked] =
    val context = BatchContext(powers.catalog, actor, ready)
    flat(operations).flatMap(operation => asking.iterator.flatMap(power =>
      power.askBefore(context, operation).map(Asked(power.id, operation, _)))
      .nextOption())

  /** The replacement the latest answer to `asked` takes, if any. */
  private def answer(answered: Vector[Answered], asked: Asked)
      : Option[Replacement] =
    answered.reverseIterator.find(_.decisionId ==
      asked.ask.decide.decisionId).flatMap(found =>
      asked.ask.instead(found.answer))

  /** `operations` with each answered operation replaced, a `Take`'s
    * leaving included. */
  private def replaced(operations: Vector[CoreOperation],
      asked: Vector[Asked], answered: Vector[Answered])
      : Vector[CoreOperation] = operations.flatMap { operation =>
    asked.find(_.operation == operation).flatMap(answer(answered, _)) match
      case Some(replacement) => replacement.operations
      case None => operation match
        case take: Take if take.leaving.nonEmpty =>
          Vector(take.copy(leaving = replaced(take.leaving, asked, answered)))
        case other => Vector(other)
  }

  private def flat(operations: Vector[CoreOperation]): Vector[CoreOperation] =
    operations.flatMap {
      case take: Take => take +: flat(take.leaving)
      case other => Vector(other)
    }
```

If `WalkerPowers` has no `catalog` member under that name, use the one
`ExecutedTriggers.batch` reads (`ctx.powers.catalog`).

- [ ] **Step 5: Walk the subtree in `ProcedureWalker.runNarrowed`**

In the `case None =>` block of `runNarrowed`, replace

```scala
            case build: BuildOps => WalkerSteps.runBuildOps(build, ctx, path, contributions)
```

with

```scala
            // A batch a power asks about is walked as its questions, then
            // the answered batch ([[BatchQuestions]]); a search walks it as
            // built.
            case build: BuildOps if !ctx.searching =>
              BatchQuestions.subtree(build, ctx.state, ctx.answered,
                  ctx.powers, ctx.actor, path).flatMap {
                case Some(asking) => walk(asking, ctx,
                  path :+ BatchQuestions.segment, None, resume,
                  WalkerHooks(contributions, Set.empty, strict))
                case None =>
                  WalkerSteps.runBuildOps(build, ctx, path, contributions)
              }
            case build: BuildOps => WalkerSteps.runBuildOps(build, ctx, path, contributions)
```

In the `case Some(remaining) =>` block, replace

```scala
        if remaining.nonEmpty then
          // The leaf ran in an earlier command, and its follow-ups parked.
```

with

```scala
        if remaining.headOption.contains(BatchQuestions.segment) then
          // The leaf's batch asked, and a question, or the follow-ups of
          // the answered batch, parked.
          resumeAsked(leaf, ctx, path, remaining.tail, resume, contributions,
            strict)
        else if remaining.nonEmpty then
          // The leaf ran in an earlier command, and its follow-ups parked.
```

and add, after `runNarrowed`:

```scala
  /** Resumes inside the subtree a leaf's batch questions made, rebuilt as
    * the walk first built it ([[BatchQuestions]]). */
  private def resumeAsked(leaf: LeafOperation, ctx: WalkCtx,
      path: Vector[String], rest: Vector[String], resume: Resume,
      contributions: Vector[PowerId], strict: Boolean)
      : Either[OathViolation, Step] = leaf match
    case build: BuildOps => BatchQuestions.subtree(build, ctx.state,
        ctx.answered, ctx.powers, ctx.actor, path).flatMap {
      case Some(asking) => walk(asking, ctx, path :+ BatchQuestions.segment,
        Some(rest), resume, WalkerHooks(contributions, Set.empty, strict))
      case None => contractViolation(s"resume path enters questions the " +
        s"batch at ${path.mkString(".")} no longer asks")
    }
    case other => contractViolation(s"resume path enters questions at leaf " +
      s"${WalkerSteps.leafLabel(other)}, which asks none")
```

Add one sentence to the object's doc comment, after the `BuildOps` bullet:
"A `BuildOps` batch a power asks about first walks the power's questions
under the leaf ([[BatchQuestions]])."

- [ ] **Step 6: Navigate into the subtree in `WalkerPowerGather.resolveAt`**

In `resolveAt`, add this case as the first case of `node match`, before
the `ExecutedTriggers` case:

```scala
        // Under a leaf whose batch asked, the path enters its questions.
        case build: BuildOps if segment == BatchQuestions.segment &&
            build.window.forall(gathered.contains) =>
          BatchQuestions.subtree(build, state, pending.answered, powers,
              state.game.current.walkActor, consumed).toOption.flatten
            .flatMap(resolveAt(state, pending, powers, _, remaining.tail,
              consumed :+ segment, Set.empty))
```

Import `BuildOps` in `WalkerPowerGather.scala` if it is not imported yet.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.walker.*"`
Expected: PASS, the new suite and every existing walker suite.

- [ ] **Step 8: Check the line bound**

Run: `python3 scripts/check-architecture.py`
Expected: passes. `ProcedureWalker.scala` grows by about 35 lines from
641, and `WalkerPowerGather.scala` by about 8 from 666.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powerresolver/AsksBeforeBatch.scala src/main/scala/distantlands/gameplay/walker/BatchQuestions.scala src/main/scala/distantlands/gameplay/walker/ProcedureWalker.scala src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala src/test/scala/distantlands/gameplay/walker/BatchQuestionsSuite.scala
git commit -m "$(cat <<'EOF'
feat(walker): a power may ask about a batch's operations before it runs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Checkpoint review after Task 2:** the model field and the walker seam
are in. A reviewer checks:
- the resume and navigation paths (findings 6 and 7);
- that the replay test covers the journal;
- that `./sbtw "testOnly distantlands.gameplay.walker.*"` passes.

---

### Task 3: Every power's discard names its power

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/action/SiteCards.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/SiteSlotMoves.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/{DarkEnforcer,GreatFeast,TamingCharm,RovingTerror,GiantPython,ArmedMob,Assassin,SqualidDistrict}.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/MountedLibrary.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/{RegionDiscard,Dazzle,ThreateningRoar,Riots}.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/ForestHorn.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/wake/HornedMask.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/ProvingGroundsRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/{PlanDiscard,Mercenaries,WildMounts}.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/search/SpinningBee.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/PowerDiscardCausesSuite.scala` (create)

**Interfaces:**
- Consumes: `DiscardCause`, `DiscardCause.mark` and the `cause` field from
  Task 1.
- Produces:
  - `SiteCards.discard(ready, site, card, suit, actor, required, cause: Option[DiscardCause])`, a new required last parameter;
  - `SlotMove(site, slot, replaced, makesRoom: Boolean)`, a new field;
  - `new SiteSlotMoves(mover: DenizenId, power: PowerId, empty: Boolean)`;
  - `new RegionDiscard(power: PowerId, kept, regionOf)`, a new first parameter;
  - `PlanDiscard.denizen(user, card, cause: Option[DiscardCause])` and
    `PlanDiscard.denizen(catalog, user, card, cause: Option[DiscardCause])`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/PowerDiscardCausesSuite.scala`:

```scala
package distantlands.gameplay.powers

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard,
  SiteSlotMoves, TamingCharm, TamingCharmCard}
import distantlands.gameplay.powers.campaign.WildMountsCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{PhasePowerHarness, RecordedOps, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Each power's discard names the power whose use discards it (catalog
  * batch 11, slice 4), so Wild Pastures can tell a power's discard from a
  * rule's. */
class PowerDiscardCausesSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val wildMounts = WildMountsCard.id
  private val errandBoy = ErrandBoyCard.id

  /** Every discard the events recorded. */
  private def discards(events: Vector[OathEvent]): Vector[Discard.Denizen] =
    RecordedOps.all(events).collect { case d: Discard.Denizen => d }

  test("Taming Charm's discard names Taming Charm, making no room"):
    val ready = Table.start.turn(p1, Phase.Act).pawn(p1, home)
      .adviser(p1, TamingCharmCard.id).denizen(wildMounts, at = home)
      .bankFavor(Suit.Nomad, 5).ready
    val started = accepted(use(ready, TamingCharm,
      DecisionOptionRef.Denizen(TamingCharmCard.id)))
    val done = accepted(answer(started, p1, TamingCharm.decisionId,
      ChooseOneAnswer(DecisionOptionRef.Denizen(wildMounts))))
    assertEquals(discards(done.events).map(_.cause),
      Vector(Some(DiscardCause(TamingCharm.id))))

  test("a mover's replace makes room only at a full site"):
    val slots = new SiteSlotMoves(RovingTerrorCard.id, RovingTerror.id,
      empty = false)
    val capacity = catalog.site(peaks).map(_.capacity).getOrElse(0)
    val fillers = SearchFixture.anyPlainDenizensOf(Suit.Hearth)
      .filterNot(_ == errandBoy).take(capacity - 1)
    val roomy = Table.start.denizen(RovingTerrorCard.id, at = home)
      .denizen(errandBoy, at = peaks).ready
    val full = fillers.foldLeft(Table.from(roomy))(_.denizen(_, at = peaks))
      .ready
    def makesRoom(ready: ReadyGame): Vector[Boolean] =
      slots.moves(catalog, ready).filter(_.site == peaks).map(_.makesRoom)
    assert(makesRoom(roomy).nonEmpty)
    assert(makesRoom(roomy).forall(!_))
    assert(makesRoom(full).nonEmpty)
    assert(makesRoom(full).forall(identity))
```

`SiteSlotMoves` is `private[powers]`, which this suite's package reaches.
If `RovingTerror`'s registered object has another name, read it from
`RovingTerrorSuite` (`private val power = RovingTerror`).

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.PowerDiscardCausesSuite"`
Expected: compile errors: the `SiteSlotMoves` constructor takes no power,
and `SlotMove` has no `makesRoom`.

- [ ] **Step 3: Give the helpers a cause**

1. **`SiteCards.discard`.** Add `cause: Option[DiscardCause]` as its last
   parameter, and pass `cause = cause` to both `Discard.Denizen(...)` and
   `Discard.RuinedEdifice(...)` there. In `judged`, which only judges
   restrictions, pass `cause = None`. Add to its doc: "`cause` is the
   power's use that discards it; a judgment passes none."
2. **`SiteSlotMoves`.**
   - Add the `makesRoom` field: `final case class SlotMove(site: SiteId,
     slot: DecisionOptionRef.SiteSlot, replaced: Option[(SiteDenizenState,
     Suit)], makesRoom: Boolean)`. Its doc says: "`makesRoom` when the
     replace frees the only slot the mover can take: the site was full
     (catalog batch 11, ruling 20)."
   - Change the class to `final class SiteSlotMoves(mover: DenizenId,
     power: PowerId, empty: Boolean)`.
   - In `movesAt`, build the open move with `makesRoom = false` and each
     replacing move with `makesRoom = !room`.
   - In `operations`, pass
     `cause = Some(DiscardCause(power, makesRoom = move.makesRoom))` to
     `cards.discard`.
3. **The `SiteSlotMoves` callers** pass their own power id:
   `new SiteSlotMoves(RovingTerrorCard.id, this.id, empty = false)` in
   `RovingTerror.scala`, and the same in `GiantPython.scala` with its own
   card and its action power's id (the id its `UsePower` names).
4. **`SiteCards.discard` callers** pass `cause = Some(DiscardCause(id))`,
   where `id` is the object's own power id:
   - `DarkEnforcer.scala:47`;
   - `GreatFeast.scala:65`;
   - `TamingCharm.scala:70`;
   - `MountedLibrary.scala:101`, as a named argument after
     `required = true`.
5. **`RegionDiscard`.**
   - Add `power: PowerId` as the class's first parameter.
   - In its private `discard` helper, pass
     `cause = Some(DiscardCause(power))` to both constructions.
   - Add to the class doc: "`power` is the power whose use discards, which
     each discard names."
6. **The `RegionDiscard` callers.**
   - `new RegionDiscard(id, Dazzle.suits)` in Dazzle.
   - `new RegionDiscard(id, ThreateningRoar.suits)` in Threatening Roar.
   - `new RegionDiscard(id, _ => true)` in Riots.
   - `new RegionDiscard(id, _ != Suit.Beast, (_, _) => Some(region))` in
     Forest Horn, where `id` is Forest Horn's power id in scope there.
7. **The direct constructions** add a named `cause` argument:
   - `HornedMask.scala:182`:
     `cause = Some(DiscardCause(id, makesRoom = true))`. Horned Mask
     discards only when the adviser area is full.
   - `ArmedMob.scala:163`: `cause = Some(DiscardCause(id))`.
   - `Assassin.scala:108`: `cause = Some(DiscardCause(id))`.
   - `SqualidDistrict.scala:124`: `cause = Some(DiscardCause(id))`.
   - `ProvingGroundsRules.scala:120` and `:125` (Empty Grounds):
     `cause = Some(DiscardCause(id))`, where `id` is the Empty Grounds
     power's id in that object. Read the object to find its name.
8. **`PlanDiscard`.**
   - Add `cause: Option[DiscardCause]` as the last parameter of both
     `denizen` overloads and of the private `operations`, and pass it to
     `Discard.Denizen(...)`.
   - In `afterCampaign`, pass
     `cause = use.user.map(_ => DiscardCause(power))`.
   - Add to the object doc: "A plan's discard names the plan's power when
     a player used it. A bandit defender's plan names none: the attacker
     is only its acting player."
9. **The `PlanDiscard` callers.**
   - `Mercenaries.scala:54`:
     `PlanDiscard.denizen(user, MercenariesCard, Some(DiscardCause(id)))`.
   - `WildMounts.scala:176`:
     `PlanDiscard.denizen(catalog, acting, card, use.user.map(_ => DiscardCause(id)))`.
10. **Spinning Bee.** In `discardedUnlessMatched`, mark the play's
    operations:
    `operations <- CardPlay.operations(catalog, ready, actor, card, SearchPlacement.Discard, CardPlay.Origin.TemporaryHand).map(DiscardCause.mark(_, DiscardCause(id)))`.

`DiscardCause` lives in `distantlands.model`, which these files import with
`distantlands.model._`. A file that imports `model` names one by one adds
`DiscardCause` to its list.

- [ ] **Step 4: Run the tests and every power suite**

Run: `./sbtw "testOnly distantlands.gameplay.powers.*"`
Expected: PASS. Existing suites compare whole operations only where they
build them through the same helpers, so they pass unchanged. A suite that
compares a hand-built `Discard.Denizen` with a recorded one, and now fails
on the missing cause, gets `cause = Some(DiscardCause(<power>.id))` added
to its hand-built discard, and nothing else.

- [ ] **Step 5: Commit**

Add every file the steps touched, by name. Then:

```bash
git commit -m "$(cat <<'EOF'
feat(powers): each power's discard names the power that discards

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: A card play's discards name their power

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/CardPlayRules.scala` (`DiscardPermission`, `homeland`)
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala` (`credits`, at `:198-218`)
- Create: `src/main/scala/distantlands/gameplay/actions/cardplay/PlayDiscardCauses.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala` (the `play` Branch, at `:444-461`)
- Test: `src/test/scala/distantlands/gameplay/PlayDiscardCausesSuite.scala` (create)
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartySuite.scala`

**Interfaces:**
- Consumes: `DiscardCause` and the `cause` field from Task 1.
- Produces:
  - `DiscardPermission(..., fromPower: Boolean = true)`, a new last field;
  - `CardSlots.credited(catalog, ready, actor, card, site, rules, discarded, buried): Either[OathViolation, Vector[DiscardPermission]]`;
  - `PlayDiscardCauses.mark(operations: Vector[CoreOperation], played: WorldCardId, byPower: Option[PowerId], sitePermissions: Vector[DiscardPermission], siteFull: Boolean): Vector[CoreOperation]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/PlayDiscardCausesSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.{CardPlayRules, DiscardPermission}
import distantlands.gameplay.actions.cardplay.PlayDiscardCauses
import distantlands.model._
import distantlands.testkit.Table.p1

/** The causes of a card play's discards (catalog batch 11, rulings 20 and
  * 22). */
class PlayDiscardCausesSuite extends munit.FunSuite:
  private val site = SiteId("site")
  private val played = DenizenId("played")
  private val replaced = DenizenId("replaced")
  private val atSite = PositionedLocation(Location.Site(site))
  private val crop = PowerId("denizen.crop-rotation")
  private val searchParty = PowerId("denizen.search-party")

  private def siteDiscard(card: String): Discard.Denizen = Discard.Denizen(
    DenizenId(card), atSite, Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val adviserReplaced = Discard.Denizen(replaced,
    PositionedLocation(Location.PlayArea(p1)), Region.Provinces, Suit.Nomad,
    0, 0, p1)
  private val playedDiscarded = Discard.Denizen(played,
    PositionedLocation(Location.RegionalDiscard(Region.Cradle)),
    Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val cropPermission = DiscardPermission.lined(crop,
    PowerSourceRef.Card(DenizenId("128")))

  test("a site discard names the power whose permission it spends, " +
      "making room when the site was full"):
    val ops = Vector[CoreOperation](siteDiscard("a"), siteDiscard("b"))
    val permits = Vector(CardPlayRules.homeland(site), cropPermission)
    assertEquals(PlayDiscardCauses.mark(ops, played, None, permits,
      siteFull = false), Vector[CoreOperation](siteDiscard("a"),
      siteDiscard("b").copy(cause = Some(DiscardCause(crop)))))
    assertEquals(PlayDiscardCauses.mark(Vector(siteDiscard("a")), played,
      None, Vector(cropPermission), siteFull = true),
      Vector[CoreOperation](siteDiscard("a").copy(cause =
        Some(DiscardCause(crop, makesRoom = true)))))

  test("a play a power made names that power on its replaced adviser, " +
      "making room, and on its played card"):
    assertEquals(PlayDiscardCauses.mark(
      Vector(adviserReplaced, playedDiscarded), played, Some(searchParty),
      Vector.empty, siteFull = false), Vector[CoreOperation](
      adviserReplaced.copy(cause =
        Some(DiscardCause(searchParty, makesRoom = true))),
      playedDiscarded.copy(cause = Some(DiscardCause(searchParty)))))

  test("a plain play's discards name nothing"):
    val ops = Vector[CoreOperation](adviserReplaced, playedDiscarded)
    assertEquals(PlayDiscardCauses.mark(ops, played, None, Vector.empty,
      siteFull = false), ops)
```

In `SearchPartySuite.scala`, after the test "discarded, the card goes on
top of the next region's pile, even the pile it came from", add:

```scala
  test("discarded, the card's discard names Search Party"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
      plainHearth.take(3)*))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, plainHearth(0)), plainHearth(0), PlayedAs.Discarded)
    assertEquals(RecordedOps.all(done.events).collect {
      case discard: Discard.Denizen if discard.card == plainHearth(0) =>
        discard.cause }, Vector(Some(DiscardCause(SearchParty.id))))
```

Import `distantlands.testkit.RecordedOps` in that suite if it does not
already.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.PlayDiscardCausesSuite distantlands.gameplay.powers.whenplayed.SearchPartySuite"`
Expected: a compile error, because `PlayDiscardCauses` is not found.

- [ ] **Step 3: Mark the Homeland permission as a rule's**

In `CardPlayRules.scala`:
- add a last field `fromPower: Boolean = true` to `DiscardPermission`;
- add to its doc: "`fromPower` is false for a rule's leave (Homeland), whose
  discard names no power."
- change `homeland` to end
  `DiscardPermission.lined(RuleNotes.homelandDiscard, PowerSourceRef.Site(site), appliesAt = (_, at) => at == site).copy(fromPower = false)`.

- [ ] **Step 4: Expose the credited permissions**

In `CardSlots.scala`, split `credits` into a new `credited`, which returns
the permissions, and `credits`, which maps them to their lines:

```scala
  /** The permission each removal at `site` spent: discards spend the
    * discard permissions and a bury a bury permission, each in the order
    * `permissions` gives them, and every one covers any site card. Refused
    * when more cards are removed than there are permissions for. */
  def credited(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      card: WorldCardId, site: SiteId, rules: CardPlayRules,
      discarded: Vector[CardId], buried: Option[CardId] = None)
      : Either[OathViolation, Vector[DiscardPermission]] =
    val permitted = permissions(catalog, ready, actor, card, site, rules)
    val discards = permitted.filter(_.removal == SiteRemoval.Discard)
    val buries = permitted.filter(_.removal == SiteRemoval.Bury)
    for
      _ <- Either.cond(discarded.size <= discards.size, (),
        OathViolation.InvalidSearchPlacement("more site cards are " +
          "discarded than the permissions in play allow"))
      _ <- Either.cond(buried.size <= buries.size, (),
        OathViolation.InvalidSearchPlacement("more site cards are buried " +
          "than the permissions in play allow"))
    yield discards.take(discarded.size) ++ buries.take(buried.size)

  /** The line of the permission each removal at `site` spent
    * ([[credited]]). */
  def credits(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      card: WorldCardId, site: SiteId, rules: CardPlayRules,
      discarded: Vector[CardId], buried: Option[CardId] = None)
      : Either[OathViolation, Vector[Note]] =
    credited(catalog, ready, actor, card, site, rules, discarded, buried)
      .map(_.map(_.note(site)))
```

- [ ] **Step 5: Write the marking**

Create `src/main/scala/distantlands/gameplay/actions/cardplay/PlayDiscardCauses.scala`:

```scala
package distantlands.gameplay.actions.cardplay

import distantlands.gameplay.actions.{DiscardPermission, SiteRemoval}
import distantlands.model._

/** The causes of a card play's discards (catalog batch 11, slice 4;
  * rulings 20 and 22):
  *
  *  - a site discard names the power whose permission it spent, in the
  *    order `CardSlots.credited` spends them, and makes room when the site
  *    was full before the play. The Homeland rule's permission names
  *    nothing;
  *  - a play-area adviser the play replaces names the power that made the
  *    play, if any, and always makes room;
  *  - the played card, discarded, names the power that made the play, if
  *    any.
  */
private[gameplay] object PlayDiscardCauses:
  def mark(operations: Vector[CoreOperation], played: WorldCardId,
      byPower: Option[PowerId], sitePermissions: Vector[DiscardPermission],
      siteFull: Boolean): Vector[CoreOperation] =
    val discards = sitePermissions.filter(_.removal == SiteRemoval.Discard)
    def spent(permits: Vector[DiscardPermission]): Option[DiscardCause] =
      permits.headOption.filter(_.fromPower).map(permit =>
        DiscardCause(permit.source, makesRoom = siteFull))
    operations.foldLeft((Vector.empty[CoreOperation], discards)) {
      case ((done, permits), discard: Discard.Denizen)
          if atSite(discard.from) =>
        (done :+ discard.copy(cause = spent(permits)), permits.drop(1))
      case ((done, permits), discard: Discard.RuinedEdifice)
          if atSite(discard.from) =>
        (done :+ discard.copy(cause = spent(permits)), permits.drop(1))
      case ((done, permits), discard: Discard.Denizen)
          if discard.card != played =>
        (done :+ discard.copy(cause =
          byPower.map(DiscardCause(_, makesRoom = true))), permits)
      case ((done, permits), discard: Discard.Denizen) =>
        (done :+ discard.copy(cause = byPower.map(DiscardCause(_))), permits)
      case ((done, permits), other) => (done :+ other, permits)
    }._1

  private def atSite(from: PositionedLocation): Boolean =
    from.location.isInstanceOf[Location.Site]
```

If `SiteRemoval` lives in another package, import it from where
`CardSlots.scala:6` imports it.

- [ ] **Step 6: Mark the play's batch**

In `CardPlayProcedure.scala`, the `play` Branch (lines 444-461) becomes:

```scala
    val play = Branch((_, pending) => {
      val extra = discarded(pending)
      val buried =
        if buriesReplaced(pending) then slot.replacedAtSite
        else slot.buried.orElse(buriedMore(pending))
      val credited = slot.site.fold[Either[OathViolation,
          Vector[DiscardPermission]]](Right(Vector.empty))(site =>
        CardSlots.credited(catalog, ready, actor, card, site, rules,
          slot.replacedAtSite.filterNot(buried.contains).toVector ++ extra,
          buried))
      // A site that was full needed the discard to take the card.
      val siteFull = slot.site.exists(site => (for
        state <- ready.game.current.map.sites.get(site)
        printed <- catalog.site(site)
      yield state.denizens.size >= printed.capacity).getOrElse(false))
      credited match
        case Left(error) => Vector(Sequence(Vector.empty),
          BuildOps((_, _) => Left(error)))
        case Right(permits) =>
          val apply = BuildOps((state, _) => CardPlay.operations(catalog,
            state, actor, card, slot.placement, from, extra, buried)
            .map(PlayDiscardCauses.mark(_, card, byPower, permits, siteFull)))
          val lines = slot.site.toVector.flatMap(site =>
            permits.map(_.note(site)))
          Vector(Sequence(lines), apply,
            Note(ProcedureNotes.id(ActionRef.PlayFacedownAdviser),
              playedNote(card, slot.ref)))
    })
```

Keep the comment block above it, and add one line to it: "The play's
discards name their power ([[PlayDiscardCauses]])." Import
`DiscardPermission` there if it is not imported yet.

- [ ] **Step 7: Run the tests and every card-play suite**

Run: `./sbtw "testOnly distantlands.gameplay.PlayDiscardCausesSuite distantlands.gameplay.CardSlotsSuite distantlands.gameplay.powers.whenplayed.* distantlands.gameplay.powers.search.* distantlands.gameplay.actions.*"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/CardPlayRules.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala src/main/scala/distantlands/gameplay/actions/cardplay/PlayDiscardCauses.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala src/test/scala/distantlands/gameplay/PlayDiscardCausesSuite.scala src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartySuite.scala
git commit -m "$(cat <<'EOF'
feat(cardplay): a play's discards name the power that permits or makes them

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Checkpoint review after Task 4:** every discard now has its cause. A
reviewer checks:
- finding 3's table against the code;
- that no rule discard got a cause;
- that `./sbtw test` passes.

---

### Task 5: Wild Pastures

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala` (delete lines 38-51)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:72-73`
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `edifice.e07.intact`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/economy/WildPasturesSuite.scala` (create)

**Interfaces:**
- Consumes:
  - `AsksBeforeBatch`, `BatchAsk`, `BatchContext` and `Replacement` (Task 2);
  - `DiscardCause` (Task 1) and the causes from Tasks 3 and 4;
  - `CardRule.rulerAt` (slice 2).
- Produces:
  - `WildPasturesCard`, moved;
  - `case object WildPastures extends AsksBeforeBatch, OnEdificeFace`, with:
    - `pay` and `discard` (`DecisionOptionRef.Button`);
    - `decisionId(card: CardId): String`;
    - `ruler(ready: ReadyGame): Option[PlayerId]`;
    - the note keys `paid` and `paidOffTurn`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/economy/WildPasturesSuite.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powerresolver.BatchContext
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard,
  TamingCharm, TamingCharmCard}
import distantlands.gameplay.powers.campaign.{HorseArchersCard, WildMounts,
  WildMountsCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Look, MenuPlay, Outcomes, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Wild Pastures (E07, intact): "Whenever this card's ruler would discard
  * a Nomad card from a power, they may instead pay 1 favor to the Nomad
  * bank." (catalog batch 11, slice 4; rulings 11 and 20 to 25) */
class WildPasturesSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerAndPhasePowers
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** A Nomad battle plan with no other use here. */
  private val nomad = WildMountsCard.id
  private val pastures = WildPasturesCard

  /** p1's Act at Ancient City with `favor` on their board. Wild Pastures
    * stands at Broken Peaks, which `ruler` holds with 2 warbands, or nobody
    * rules. The Nomad bank holds 5. The quiet table leaves Broken Peaks
    * unruled. */
  private def pasturesAtPeaksRuledBy(ruler: Option[PlayerId],
      favor: Int = 2): Table =
    ruler.foldLeft(Table.start.turn(p1, Phase.Act).pawn(p1, home)
      .favor(p1, favor).edifice(pastures, EdificeSide.Intact, at = peaks)
      .bankFavor(Suit.Nomad, 5))((table, player) =>
      table.warbandsAt(peaks, player, 2))

  private def p1RulesPastures: Table = pasturesAtPeaksRuledBy(Some(p1))

  /** p1 starts the phase power `power` printed on `card`. */
  private def use(ready: ReadyGame, power: PowerId, card: DenizenId)
      : OathTransition = accepted(rules.startWalker(
    OathState.Ready(MenuPlay.withoutMenu(ready)), ActionRef.UsePower(power),
    p1, Vector(DecisionOptionRef.Denizen(card))))

  private def answer(from: OathTransition, by: PlayerId, decision: String,
      option: DecisionOptionRef): OathTransition = accepted(
    rules.resolveWalker(from.state, by, decision, ChooseOneAnswer(option)))

  private def after(t: OathTransition): ReadyGame = Outcomes.ready(t.state)

  private def awaits(t: OathTransition, decision: String): Boolean =
    PowerRules.parkedUnderWalkerAndPhasePowers.parkedDecision(t.state)
      .exists(_.decisionId == decision)

  /** p1 uses Taming Charm on the Nomad card at their site, and the walk
    * parks on Wild Pastures' question or ends. */
  private def tamedTheNomad(ready: ReadyGame): OathTransition =
    answer(use(ready, TamingCharm.id, TamingCharmCard.id), p1,
      TamingCharm.decisionId, DecisionOptionRef.Denizen(nomad))

  /** `discard` as a power p1 uses builds it. */
  private def byP1sPower(discard: Discard.Denizen): Discard.Denizen =
    discard.copy(cause = Some(DiscardCause(TamingCharm.id)))

  private def nomadDiscardFrom(from: Location): Discard.Denizen =
    byP1sPower(Discard.Denizen(nomad, PositionedLocation(from),
      Region.Provinces, Suit.Nomad, 0, 0, p1))

  private def asks(ready: ReadyGame, discard: CoreOperation): Boolean =
    WildPastures.askBefore(BatchContext(catalog, p1, ready), discard)
      .nonEmpty

  test("a phase power's Nomad discard asks its ruler, and paying keeps " +
      "the card and still gains"):
    val ready = p1RulesPastures.adviser(p1, TamingCharmCard.id)
      .denizen(nomad, at = home).ready
    val asked = tamedTheNomad(ready)
    assert(awaits(asked, WildPastures.decisionId(nomad)))
    val paid = answer(asked, p1, WildPastures.decisionId(nomad),
      WildPastures.pay)
    val end = after(paid)
    assertEquals(Look(end).cardsAt(home), Vector[CardId](nomad))
    // 2 favor, less 1 paid, plus Taming Charm's 2 (ruling 24).
    assertEquals(Look(end).favor(p1), 3)
    // The bank's 5, plus 1 paid, less 2 gained.
    assertEquals(Look(end).bankFavor(Suit.Nomad), 4)
    assertEquals(NoteText.said(WildPastures, paid.events), Vector(
      NoteText.Said("paid", "Paid 1 favor to the Nomad bank instead of " +
        s"discarding ${nomad.value}.", covers = false)))
    assertWireRoundTrips(asked.events ++ paid.events)

  test("declining discards the card as built"):
    val ready = p1RulesPastures.adviser(p1, TamingCharmCard.id)
      .denizen(nomad, at = home).ready
    val declined = answer(tamedTheNomad(ready), p1,
      WildPastures.decisionId(nomad), WildPastures.discard)
    assertEquals(Look(after(declined)).cardsAt(home), Vector.empty[CardId])
    assertEquals(NoteText.said(WildPastures, declined.events), Vector.empty)

  test("a ruler with no favor, another player's Wild Pastures, or an " +
      "unruled one asks nothing"):
    for table <- Vector(pasturesAtPeaksRuledBy(Some(p1), favor = 0),
        pasturesAtPeaksRuledBy(Some(p2)), pasturesAtPeaksRuledBy(None)) do
      val done = tamedTheNomad(table.adviser(p1, TamingCharmCard.id)
        .denizen(nomad, at = home).ready)
      assert(!awaits(done, WildPastures.decisionId(nomad)))
      assertEquals(Look(after(done)).cardsAt(home), Vector.empty[CardId])

  test("only a power's discard of a faceup Nomad card in play, or in a " +
      "discard pile, by the ruler, making no room, is asked about"):
    val ready = p1RulesPastures.denizen(nomad, at = home).ready
    val atHome = nomadDiscardFrom(Location.Site(home))
    assert(asks(ready, atHome))
    assert(asks(ready, nomadDiscardFrom(Location.RegionalDiscard(
      Region.Cradle))))
    // A rule's discard, one making room, another player's, a Discord one.
    assert(!asks(ready, atHome.copy(cause = None)))
    assert(!asks(ready, atHome.copy(cause =
      Some(DiscardCause(TamingCharm.id, makesRoom = true)))))
    assert(!asks(ready, atHome.copy(actingPlayer = p2)))
    assert(!asks(ready, atHome.copy(suit = Suit.Discord)))
    // A card in a hand (ruling 25).
    assert(!asks(ready, nomadDiscardFrom(Location.Hand(p1))))

  test("a ruler who is not the walk's actor is named in the line, and " +
      "declining replaces nothing"):
    val ready = p1RulesPastures.denizen(nomad, at = home).ready
    val ask = WildPastures.askBefore(BatchContext(catalog, p2, ready),
      nomadDiscardFrom(Location.Site(home)))
    assertEquals(ask.flatMap(_.instead(ChooseOneAnswer(WildPastures.pay)))
      .map(_.line), Some(WildPastures.paidOffTurn(
        PowerSourceRef.Card(WildPasturesCard.id), NoteArg.Player(p1),
        NoteArg.Card(nomad))))
    assertEquals(ask.flatMap(_.instead(
      ChooseOneAnswer(WildPastures.discard))), None)

  test("a facedown adviser has no suit, and a faceup one is asked about"):
    val held = p1RulesPastures.adviser(p1, nomad).ready
    val hidden = p1RulesPastures.adviser(p1, nomad, facedown = true).ready
    val fromBoard = nomadDiscardFrom(Location.PlayArea(p1))
    assert(asks(held, fromBoard))
    assert(!asks(hidden, fromBoard))

  test("a ruined Nomad edifice is asked about as a denizen is"):
    // Great Forge (E06) is a Nomad edifice.
    val ruined = p1RulesPastures
      .edifice(GreatForgeCard, EdificeSide.Ruined, at = home).ready
    assert(asks(ruined, Discard.RuinedEdifice(GreatForgeCard.id,
      PositionedLocation(Location.Site(home)), Suit.Nomad, 0, 0, p1,
      cause = Some(DiscardCause(TamingCharm.id)))))

  test("Roving Terror moving to a site with room pays, keeps the card, and " +
      "still moves"):
    val ready = p1RulesPastures.denizen(RovingTerrorCard.id, at = home)
      .denizen(nomad, at = peaks).ready
    val started = use(ready, RovingTerror.id, RovingTerrorCard.id)
    val asked = answer(started, p1, RovingTerror.decisionId,
      DecisionOptionRef.SiteSlot(peaks, 0))
    val paid = answer(asked, p1, WildPastures.decisionId(nomad),
      WildPastures.pay)
    assertEquals(Look(after(paid)).cardsAt(peaks).filter(card =>
      card == nomad || card == RovingTerrorCard.id),
      Vector[CardId](nomad, RovingTerrorCard.id))

  test("the ruler is the card-level ruler: a Grand Mask wearer, during " +
      "their turn"):
    val masked = Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .warbandsAt(home, p2, 2)
      .edifice(pastures, EdificeSide.Intact, at = home)
      .relic(p3, GrandMaskCard).ready
    assertEquals(WildPastures.ruler(masked), Some(p3))
    assertEquals(WildPastures.ruler(pasturesAtPeaksRuledBy(None).ready), None)
```

Notes for the executor:
- `Table.adviser(..., facedown = true)` and `warbandsAt` are used as
  `TamingCharmSuite` and `CampaignFixture` use them.
- If the quiet table rules Broken Peaks, make `pasturesAtPeaksRuledBy`
  clear the site's forces first. Read `Table` for the step that does.
- The Roving Terror test filters the site's cards, since Broken Peaks may
  hold other cards on the quiet table.

Add three integration tests in the same suite, reusing existing fixtures:

```scala
  test("a When Played Nomad discard asks its ruler"):
    import distantlands.gameplay.powers.whenplayed.WhenPlayedHarness
    import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
    import distantlands.gameplay.walker.ProcedureWalker
    import distantlands.gameplay.powers.WalkerPowerCatalog
    val ready = WhenPlayedHarness.p1AtHomeSite.favor(p1, 1)
      .adviser(p1, ThreateningRoarCard.id)
      .denizen(nomad, at = WhenPlayedHarness.homeSite)
      .warbandsAt(WhenPlayedHarness.awaySite, p1, 2)
      .edifice(pastures, EdificeSide.Intact, at = WhenPlayedHarness.awaySite)
      .ready
    val park = Outcomes.parked(ProcedureWalker.advance(ready,
      WhenPlayedHarness.hook(ThreateningRoarCard.id), None,
      WalkerPowerCatalog.default(catalog)))
    assertEquals(park.tree.at.lastOption, Some("0"))
    assert(park.tree.at.contains("asked"))
```

`p1AtHomeSite` is a `Table`, as `ThreateningRoarSuite` chains it.
`ProcedureWalker` parks on Wild Pastures' question under the discard
batch's node.

```scala
  test("Wild Mounts asks first; skipped, the Nomad plan's discard asks " +
      "Wild Pastures"):
    import distantlands.gameplay.CampaignFixture
    import distantlands.gameplay.actions.campaign.CampaignIds
    import distantlands.gameplay.powers.campaign.PlanDriver
    import distantlands.gameplay.powers.search.BrackenCard
    val bracken = PlanDriver.planlessCard(BrackenCard)
    val horseArchers = HorseArchersCard.id
    val start = Vector(nomad, bracken, horseArchers).foldLeft(
      CampaignFixture.board(warbands = 5, supply = 7))((b, id) =>
      CampaignFixture.withAdviser(b, id, Orientation.FaceUp))
    val site = start.ready.game.current.map.inPlay
      .filterNot(_ == start.origin).last
    val b = CampaignFixture.on(CampaignFixture.withEdifice(
      CampaignFixture.actorRules(start, site), site, pastures,
      EdificeSide.Intact))(_.favor(start.actor, 2))
    val asked = Vector(DecisionOptionRef.Denizen(nomad),
      DecisionOptionRef.Denizen(horseArchers)).foldLeft(
      PlanDriver.commit(CampaignFixture.rules(PlanDriver.winningDice), b, 4))(
      (run, plan) => run.pick(b.actor, CampaignIds.attackerPlan, plan))
      .finishPlanWindows
    assert(PlanDriver.awaits(asked, b.actor, WildMounts.decisionId))
    val skipped = asked.pick(b.actor, WildMounts.decisionId, WildMounts.skip)
    assert(PlanDriver.awaits(skipped, b.actor,
      WildPastures.decisionId(horseArchers)))
    val paid = skipped.pick(b.actor, WildPastures.decisionId(horseArchers),
      WildPastures.pay)
    assert(Look(paid.state).advisers(b.actor).contains(horseArchers))
```

Here `nomad` is Wild Mounts itself, picked as a plan. The helpers are the
ones `WildMountsSuite` reads from `CampaignFixture` and `PlanDriver`,
named in full: their wildcard imports would clash with this suite's own
`awaits`. If `Look(...)` takes a `ReadyGame`, read `paid.state` the way
`WildMountsSuite` does.

```scala
  test("Search Party's discarded card asks, and paying leaves it in its " +
      "pile"):
    import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
    import distantlands.gameplay.powers.whenplayed.SearchPartyFixture._
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
        nomad).favor(p1, 1).warbandsAt(peaks, p1, 2)
      .edifice(pastures, EdificeSide.Intact, at = peaks))
    val asked = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, nomad), nomad, PlayedAs.Discarded)
    val pending = PowerRules.parkedUnderWalkerPowers.parkedDecision(
      asked.state)
    assertEquals(pending.map(_.decisionId),
      Some(WildPastures.decisionId(nomad)))
```

`SearchPartyFixture` drives `SearchFixture.rules`. If that rule set
carries different walker powers, read the parked decision with the
`ParkedDecisionAssertions` `SearchFixture` exposes.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.WildPasturesSuite"`
Expected: a compile error, because `WildPastures` is not found.

- [ ] **Step 3: Move the card and write the power**

Delete `WildPasturesCard` (lines 38-51) from
`catalog/holding/HoldingEdifices.scala`. Create
`src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import distantlands.gameplay.CardRule
import distantlands.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, OnEdificeFace, Replacement}
import distantlands.model._

object WildPasturesCard extends Edifice(EdificeId("E07"), Suit.Nomad):
  object intact extends EdificeFace("Wild Pastures") with Locked:
    val power = PrintedPower(PowerId("edifice.e07.intact"),
      persistent = true, cost = Cost.free,
      text = "Whenever this card's ruler would discard a [suit-nomad] " +
        "card from a power, they may instead pay [favor] to the " +
        "[suit-nomad] bank.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Blasted Pastures"):
    val power = PrintedPower(PowerId("edifice.e07.ruined"),
      persistent = true, cost = Cost.free,
      text = "**CHRONICLE (START OF THRONE):** Bury all [suit-nomad] " +
        "cards on the map unless the winner rules this card.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Wild Pastures (E07, intact), a persistent rule: "Whenever this card's
  * ruler would discard a Nomad card from a power, they may instead pay
  * favor to the Nomad bank." (catalog batch 11, slice 4)
  *
  * Its ruler is the card-level ruler (`CardRule.rulerAt`), so a Grand Mask
  * wearer rules it during their turn. Before a walker batch runs, it asks
  * that ruler about each discard in the batch that meets all of these:
  *
  *  - it is a denizen or ruined edifice discard;
  *  - its card is Nomad by its effective suit (rulings 21 and 23);
  *  - the ruler is its acting player;
  *  - its cause names a power and makes no room (rulings 20 and 22);
  *  - its card is faceup in play, or in a discard pile (ruling 25).
  *
  * The ruler is asked only while they have favor on their board. A "pay"
  * puts a `Give` of 1 favor to the Nomad bank in the discard's place, and
  * the card stays where it is. The rest of the batch runs as built (ruling
  * 24).
  *
  * The line reads "Paid 1 favor to the Nomad bank instead of discarding
  * {card}." For a ruler who is not the walk's actor (a defender's plan), it
  * reads "{Blue} paid ...".
  *
  * The ruined face, Blasted Pastures, is a Chronicle step and is not
  * implemented.
  */
case object WildPastures extends AsksBeforeBatch, OnEdificeFace:
  val edifice: EdificeId = WildPasturesCard.id
  val side: EdificeSide = EdificeSide.Intact
  val id: PowerId = WildPasturesCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  val pay: DecisionOptionRef.Button = DecisionOptionRef.Button("pay")
  val discard: DecisionOptionRef.Button = DecisionOptionRef.Button("discard")
  /** One question per card, so two in one batch differ. */
  def decisionId(card: CardId): String =
    s"wild-pastures.${card.kind}.${card.value}"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Paid 1 favor to the Nomad bank instead of discarding {card}." */
  val paid: NoteKey = notes.key("paid", Vector(
    NotePart.Text("Paid 1 favor to the Nomad bank instead of discarding "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "{Blue} paid 1 favor to the Nomad bank instead of discarding
    * {card}.", for a ruler who is not the walk's actor. */
  val paidOffTurn: NoteKey = notes.key("paid-off-turn", Vector(
    NotePart.Arg(0),
    NotePart.Text(" paid 1 favor to the Nomad bank instead of discarding "),
    NotePart.Arg(1), NotePart.Text(".")))

  /** The player who rules Wild Pastures, while it stands intact. */
  def ruler(ready: ReadyGame): Option[PlayerId] = for
    site <- ready.game.current.edificeSite(edifice, side)
    ruling <- CardRule.rulerAt(ready, site, edifice)
    player <- ruling match
      case SiteRuler.Player(player) => Some(player)
      case _ => None
  yield player

  def watching(ready: ReadyGame): Boolean = ruler(ready).nonEmpty

  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk] = for
    ruler <- ruler(batch.ready)
    (card, from) <- discarded(operation, ruler)
    if standsWithAFace(batch.ready, card, from)
    if batch.ready.game.current.player(ruler).exists(_.board.favor >= 1)
  yield BatchAsk(question(batch.catalog, ruler, card), answer =>
    Option.when(answer == DecisionAnswer.ChooseOneAnswer(pay))(Replacement(
      Vector(Give(Piece.Favor(1), ruler, Location.PlayArea(ruler),
        Location.FavorBank(Suit.Nomad), required = true)),
      line(ruler, batch.actor, card))))

  /** The card a discard of a Nomad card by `ruler`, from a power's use
    * that makes no room, takes, and where from. */
  private def discarded(operation: CoreOperation, ruler: PlayerId)
      : Option[(CardId, Location)] = operation match
    case discard: Discard.Denizen if discard.suit == Suit.Nomad &&
        discard.by.contains(ruler) && fromAPower(discard.cause) =>
      Some(discard.card -> discard.from.location)
    case discard: Discard.RuinedEdifice if discard.suit == Suit.Nomad &&
        discard.by.contains(ruler) && fromAPower(discard.cause) =>
      Some(discard.card -> discard.from.location)
    case _ => None

  private def fromAPower(cause: Option[DiscardCause]): Boolean =
    cause.exists(!_.makesRoom)

  /** Whether `card` stands faceup in play where `from` says, or lies in a
    * discard pile (ruling 25). */
  private def standsWithAFace(ready: ReadyGame, card: CardId,
      from: Location): Boolean = from match
    case Location.PlayArea(holder) => ready.game.current.player(holder)
      .exists(_.advisers.exists {
        case DenizenState(id, Orientation.FaceUp, _) => id == card
        case _ => false
      })
    case Location.Site(site) => ready.game.current.map.sites.get(site)
      .exists(_.denizens.exists {
        case DenizenState(id, Orientation.FaceUp, _) => id == card
        case EdificeState(id, EdificeSide.Ruined, _) => id == card
        case _ => false
      })
    case _: Location.RegionalDiscard => true
    case _ => false

  private def question(catalog: ExecutableCatalog, ruler: PlayerId,
      card: CardId): Decide =
    Decide(decisionId(card), ruler, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(pay, "Pay 1 favor"),
      DecisionOption.Button(discard, "Discard it")),
      heading = Some("Wild Pastures: pay 1 favor to the Nomad bank instead " +
        s"of discarding ${named(catalog, card)}?")))

  private def named(catalog: ExecutableCatalog, card: CardId): String =
    card match
      case id: DenizenId => catalog.denizen(id).fold(id.value)(_.name)
      case id: EdificeId => catalog.edifice(id).fold(id.value)(_.ruined.name)
      case other => other.value

  private def line(ruler: PlayerId, actor: PlayerId, card: CardId): LogNote =
    val source = PowerSourceRef.Card(edifice)
    if ruler == actor then paid(source, NoteArg.Card(card))
    else paidOffTurn(source, NoteArg.Player(ruler), NoteArg.Card(card))
```

If `PlayerState.advisers` holds a type other than `DenizenState` for
denizens, match the form `PlanDiscard.operations` uses (`case held:
DenizenState if held.id == card`).

- [ ] **Step 4: Register it and record its status**

1. In `WalkerPowerCatalog.scala:72-73`, add `WildPastures` after
   `HallowedSpring`: `Vector(GrandCanal, LostLagoon, BoilingLakeTravel,
   ForbiddenForest, HallowedSpring, WildPastures)`.
2. In `src/test/resources/powers/unimplemented.txt`, delete the line
   `edifice.e07.intact`. Keep `edifice.e07.ruined`.
3. In `src/test/resources/notes/declared-keys.txt`, add these two lines,
   in sorted position among the `edifice.e0` keys:

```text
edifice.e07.intact paid = Paid 1 favor to the Nomad bank instead of discarding {0}.
edifice.e07.intact paid-off-turn = {0} paid 1 favor to the Nomad bank instead of discarding {1}.
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.WildPasturesSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.powers.PowerCatalogUniquenessSuite distantlands.gameplay.powers.PowerImplementationStatusSuite"`
Expected: PASS.

Then run every suite that drives the full catalog:

Run: `./sbtw test`
Expected: PASS. No existing fixture stages Wild Pastures, so no existing
walk asks a new question.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/economy/WildPasturesSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Wild Pastures pays instead of discarding a Nomad card

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Close the slice

**Files:**
- Modify: `docs/ROADMAP.md` (the engine-blocked phase)
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md` (status)

- [ ] **Step 1: Update the roadmap and the spec**

In `docs/ROADMAP.md`, under **Phase - Catalog batch - engine-blocked**,
delete the line:

```markdown
- **Discard replacement with a question:** Wild Pastures.
```

In the spec, change the status line to

```markdown
**Status:** designed 2026-11-07. Slices 0 to 4 landed.
```

(If slice 3 has not landed, stop: see Prerequisite.) Then run:

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
docs(catalog): close catalog batch 11 slice 4, Wild Pastures

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
