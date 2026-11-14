# Legacy Setup Powers, Slice 3: World Deck Implementation Plan

> Executed inline by the implementer.

**Goal:** Two legacies act on the world deck that game start built.
Secret Society's holder may take one Vision, except the Conspiracy, as a
facedown adviser; Visions Drawn advances and the rest of the Visions are
shuffled back into the card's layout. Seer's holder then places every
Vision in the deck at the position they choose, through a new
`DecisionQuery.ChooseAmounts` and its frontend control.

**Architecture:** A new `ChooseAmounts` query asks for one amount per
slot, each within its bounds and, with `distinct` set, no two equal. It
reuses `DistributeSlot` for its slots and `DistributeAnswer` and the
`DistributeWire` for its answer (R18), so no saved or wire answer form is
added; it gets its own projection DTO, codec and frontend panel. A new
empty `SetupWorldDeck` window, after `SetupBanks`, carries Secret Society's
step and then Seer's. Secret Society's reshuffle is a `Shuffle` of a new
pile, `ShufflePile.VisionPackets` (the world deck), which the walker fills
with the card's layout from its random source and journals (R19). Seer's
placement is a `Shuffle` of the world deck with its order already filled.
The projection names the world deck's Visions to a holder only while that
holder's Secret Society or Seer question is the parked decision, read as
R16 reads the keep (R20). Secret Society's excess advisers are discarded in
a step right after the holder's pawn (R21).

**Tech Stack:** Scala 3.9.0 (JVM and Scala.js), munit, `./sbtw` (sbt
wrapper); frontend suites with `./sbtw "frontend/test"`. Compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Setup powers design](../../specs/2026-11/2026-11-13-legacy-setup-powers-design.md),
rulings 1-4 and 14-15 and section "Slice 3: world deck".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md)
(R1-R17 stand; this plan proposes R18-R25). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (Seer
L22, Secret Society L28). The rulebook's world deck, CR p. 12
(`docs/rules/rules-reference.md#later-games`, step 6).

## Prerequisite

Slice 2 has landed. Check before Task 1:

```bash
grep -q "case object SetupBanks" src/main/scala/distantlands/model/PowerWindow.scala &&
grep -q "def keepPending" src/main/scala/distantlands/application/GamePresentationProjector.scala &&
grep -q "object LegacyKeep" src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala &&
echo ready
```

Expected: `ready`.

## Open questions (proposed rulings R18-R25)

The plan is written to the recommendation of each. Production code waits
for the orchestrator's rulings.

- **R18 (Task 1). `ChooseAmounts`' slots and answer.** *Recommended:*
  `DecisionQuery.ChooseAmounts(slots: Vector[DistributeSlot], distinct:
  Boolean, heading: Option[String], confirmLabel: String)`, answered by the
  existing `DecisionAnswer.DistributeAnswer` and sent as the existing
  `DecisionAnswerWire.DistributeWire`: "one amount per slot" is that
  answer's shape, so the journal codec, the command codec and
  `GameIntentMapper` are unchanged. The validator tells the two queries
  apart. *Alternative:* a new `ChooseAmountsAnswer` with its own journal
  codec, wire form and intent mapping.
- **R19 (Task 4). Secret Society's random reshuffle.** *Recommended:* a
  new `ShufflePile.VisionPackets`, whose cards are the world deck. The
  walker fills its order from its random source with the card's layout:
  the denizens keep their order; with 25 denizens or more two Visions go to
  random places among the first 12 cards and two among the next 17, which
  two going where chosen at random; with fewer, every Vision goes to a
  random place in the whole deck. The filled `Shuffle` is journaled like
  any shuffle. This adds one pile kind to the operation codec
  (`{"kind": "vision-packets"}`); older streams still decode. *Alternative:*
  a plain world-deck `Shuffle`, which reorders the denizens too, against
  spec ruling 14.
- **R20 (Task 4). How the holder sees the deck's Visions.** The options of
  both questions name Visions that lie in the world deck, which the
  projection names to nobody. *Recommended:* as R16 does for Matchmaker,
  the projection names the world deck's Visions to the holder only while
  their Secret Society or Seer question is the parked decision; before and
  after, the deck is as hidden as before for everyone. No `Peek` is
  recorded, so no lasting knowledge is kept. *Alternative:* a `Peek` of
  each Vision for the holder before the question, which leaves the holder
  knowing them in the deck afterwards (`CardKnowledge.advisers`) until a
  shuffle clears it, after a declined Secret Society included.
- **R21 (Task 5). Secret Society's excess discard.** *Recommended:* right
  after the holder's own `SetupPawnPlaced` window and before their keep,
  when their advisers exceed `AdviserLimit.of`, one `ChooseMany(k, k)` over
  all their advisers, `setup.legacy.secret-society.<player>.discard`, where
  `k` is the excess. The cards go to the discard pile of the region after
  the pawn's, top, as Setup's rejected advisers do. No line of its own: the
  moves' own lines and the discard pile's visibility apply. It has no
  Decline; the adviser limit is not a "may".
- **R22 (Task 4). Secret Society's question.** *Recommended:*
  `setup.legacy.secret-society.<player>`, a `ChooseOne` with Decline first
  and then the world deck's Visions except the Conspiracy, in deck order,
  headed "Secret Society: you may take a Vision as a facedown adviser". It
  is asked whenever a seated holder holds it active and the deck holds one
  such Vision; the reshuffle and Visions Drawn follow only a take.
- **R23 (Task 6). Seer's question.** *Recommended:*
  `setup.legacy.seer.<player>`, one `ChooseAmounts` with a slot per Vision
  in deck order, each `1 to n + v`, `distinct`, suggesting each Vision's
  current position, headed "Seer: place the Visions in the world deck",
  confirmed "Place the Visions". With no Decline (no "may"). It is skipped
  only when it would be malformed, a deck of one card and no other.
- **R24 (Task 4). One window for both.** Secret Society and Seer both
  hook `SetupWorldDeck`; Secret Society's step comes first because it is
  registered first in `LegacySetupPowers.all`, which a test pins.
  *Recommended:* as written; the alternative is two windows.
- **R25 (Task 3). The frontend control.** *Recommended:* one row per slot,
  the card drawn as on the Distribute panel, a `−` and a `+` stepper
  (Shift+click to the slot's bound), the amount, and a Confirm button
  disabled while two amounts are equal, with a line naming the clash ("Two
  cards share position 3"). It mirrors the Distribute panel, so its draft
  survives redraws through `SessionDrafts`. *Alternative:* a number field
  per row (`AmountCombobox`), quicker for a long deck but with no draft
  across redraws.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`); `gameplay/actions` imports nothing
  from `gameplay.powers`; `model` imports nothing from `gameplay`.
- A new test names cards by their card objects (`SeerCard`).
- Only seated players' active legacies act (spec ruling 1); every "you
  may" has its own id and a Decline (ruling 3); several cards are one
  choose-many (ruling 4).
- No `Branch`'s children or ids depend on state the walk changes: Seer's
  bounds read the deck at its own question, which only Secret Society's
  earlier answer changed.
- Every new `Decide` has a resume test; no two decisions share an id.
- Hidden information: Secret Society's Vision, Seer's Visions and their
  positions are the holder's only; each has a redaction test per viewer
  (holder, another seat, the public).
- Old saved streams still decode. The only new saved form is R19's pile
  kind. A game saved mid-Setup by an earlier build may not resume (R10).
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. One commit per task.
- Statement coverage at or above 92%, branch coverage at or above 84.4%.
- Each task that adds a note key or moves a card regenerates
  `src/test/resources/notes/declared-keys.txt` (`NOTE_KEYS_SNAPSHOT=write`)
  and `src/test/resources/powers/unimplemented.txt`
  (`UNIMPLEMENTED_SNAPSHOT=write`) with
  `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.powers.UnimplementedPowersSuite"`,
  reviews the diff and commits it in the task. A golden is generated only
  for its own script. `git status --short src/test/resources` is otherwise
  clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files with an explicit pathspec; run `git log -3` after each
  commit. Another session commits in this tree.

## The gate

Run after the last task:

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes; coverage at or above 92% statements and
84.4% branches; both scripts pass; the last command prints nothing. Then
the running-app check of Task 7, Step 4, with screenshots or page text in
the report.

---

### Task 1: `DecisionQuery.ChooseAmounts` in the model and the validator

**Files:**
- Modify: `src/main/scala/distantlands/model/Decisions.scala` (`ChooseAmounts`)
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` (`wellFormed`, `accepts`)
- Modify: every exhaustive match over `DecisionQuery` the compiler names (expected: `WalkerDecisionProjector`'s query projection, which returns `None` for the new shape until Task 2; walker helpers such as `EmptyChoices` or `DecideNarrowing` if they match on queries; `application/gamelog/DetailLines` or `ChoiceWords` if they do)
- Modify: `src/test/scala/distantlands/testkit/Situation.scala` (`defaultAnswer`)
- Create: `src/test/scala/distantlands/gameplay/walker/ChooseAmountsQuerySuite.scala`

**Interfaces:**
- Produces: `DecisionQuery.ChooseAmounts(slots: Vector[DistributeSlot], distinct: Boolean, heading: Option[String], confirmLabel: String)`, answered by `DecisionAnswer.DistributeAnswer(amounts: Vector[DistributeAmount])` (R18).

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.walker

import distantlands.model._

/** `ChooseAmounts`: one amount per slot, each within its bounds, and with
  * `distinct` no two equal (Legacy Setup powers design, ruling 15; R18). */
class ChooseAmountsQuerySuite extends munit.FunSuite:
  private val (a, b, c) = (DecisionOptionRef.Vision(VisionId("vision:a")),
    DecisionOptionRef.Vision(VisionId("vision:b")),
    DecisionOptionRef.Vision(VisionId("vision:c")))
  private def slot(ref: DecisionOptionRef, suggested: Int) =
    DistributeSlot(ref, 1, 5, Some(suggested))
  private val query = DecisionQuery.ChooseAmounts(
    Vector(slot(a, 1), slot(b, 2), slot(c, 3)), distinct = true,
    heading = Some("Place"), confirmLabel = "Place")
  private def answer(amounts: (DecisionOptionRef, Int)*) =
    DecisionAnswer.DistributeAnswer(amounts.toVector.map(DistributeAmount(_, _)))
  private val p1 = PlayerId("p1")

  private def accepts(q: DecisionQuery, given: DecisionAnswer) =
    DecisionQueries.accepts("d", q, given, p1)

  test("a well-formed query"):
    assertEquals(DecisionQueries.wellFormed("d", query), Right(()))

  test("distinct amounts within their bounds are accepted"):
    assertEquals(accepts(query, answer(a -> 5, b -> 1, c -> 3)), Right(()))

  test("two equal amounts are refused when distinct is set"):
    assert(accepts(query, answer(a -> 2, b -> 2, c -> 3)).isLeft)
    assertEquals(accepts(query.copy(distinct = false),
      answer(a -> 2, b -> 2, c -> 3)), Right(()))

  test("an amount outside its bounds, a missing or a repeated slot is " +
      "refused"):
    assert(accepts(query, answer(a -> 6, b -> 1, c -> 3)).isLeft)
    assert(accepts(query, answer(a -> 1, b -> 2)).isLeft)
    assert(accepts(query, answer(a -> 1, a -> 2, c -> 3)).isLeft)

  test("another shape's answer is refused"):
    assert(accepts(query, DecisionAnswer.ChooseAmountAnswer(1)).isLeft)

  test("a malformed query is refused"):
    Vector(
      query.copy(heading = None),
      query.copy(confirmLabel = " "),
      query.copy(slots = Vector.empty),
      query.copy(slots = Vector(slot(a, 1), slot(a, 2))),
      query.copy(slots = Vector(DistributeSlot(a, 3, 2, None))),
      // Three distinct amounts cannot fit in 1..2.
      query.copy(slots = Vector(a, b, c).map(DistributeSlot(_, 1, 2, None))),
      // Every slot fixed: nothing to decide.
      query.copy(slots = Vector(DistributeSlot(a, 1, 1, None))),
      query.copy(slots = Vector(slot(a, 1), slot(b, 1), slot(c, 3))),
      query.copy(slots = Vector(slot(a, 1), DistributeSlot(b, 1, 5, None)))
    ).foreach(bad => assert(DecisionQueries.wellFormed("d", bad).isLeft, bad))
```

Check `DecisionQueries.accepts`'s parameter order and name (it takes the
answerer, `by`) and adjust the helper.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.ChooseAmountsQuerySuite"`
Expected: compile error, `ChooseAmounts` is not a member of `DecisionQuery`.

- [ ] **Step 3: Implement**

`Decisions.scala`, after `Distribute`:

```scala
  /** One amount per slot, each within its own bounds, and with `distinct`
    * no two equal (Legacy Setup powers design, ruling 15): Seer places each
    * Vision at a position of the world deck. Answered by a
    * [[DecisionAnswer.DistributeAnswer]], one amount per slot (R18). Both
    * labels are required. Either every slot suggests an amount or none
    * does, and the suggestions must be an accepted answer.
    */
  final case class ChooseAmounts(slots: Vector[DistributeSlot],
      distinct: Boolean, heading: Option[String], confirmLabel: String)
      extends DecisionQuery
```

`DecisionQueries.wellFormed`:

```scala
    case DecisionQuery.ChooseAmounts(slots, distinct, heading,
        confirmLabel) =>
      val refs = slots.map(_.ref)
      val suggested = slots.flatMap(_.suggested)
      val lowest = slots.map(_.minimum).minOption.getOrElse(0).toLong
      val highest = slots.map(_.maximum).maxOption.getOrElse(0).toLong
      for
        _ <- require(heading.exists(_.trim.nonEmpty), decisionId,
          "declares no heading")
        _ <- require(confirmLabel.trim.nonEmpty, decisionId,
          "declares a blank confirm label")
        _ <- require(slots.nonEmpty, decisionId, "declares no slot")
        _ <- require(refs.distinct.size == refs.size, decisionId,
          "declares duplicate options")
        _ <- refs.find(DecisionOption.forRef(_).isEmpty) match
          case Some(ref) => reject(decisionId,
            s"declares slot ${label(ref)}, which has no presentable option")
          case None => Right(())
        _ <- slots.find(s => s.minimum < 0 || s.minimum > s.maximum) match
          case Some(s) => reject(decisionId, s"declares slot ${label(s.ref)} " +
            s"with bounds ${s.minimum}..${s.maximum}")
          case None => Right(())
        _ <- require(!distinct || slots.size.toLong <= highest - lowest + 1,
          decisionId, "declares more distinct slots than its bounds hold")
        _ <- require(slots.exists(s => s.minimum < s.maximum), decisionId,
          "declares no variable slot, leaving nothing to decide")
        _ <- require(suggested.isEmpty || suggested.size == slots.size,
          decisionId, "suggests amounts for some slots but not all")
        _ <- if suggested.isEmpty then Right(())
          else acceptsAmounts(decisionId, slots, distinct,
              slots.zip(suggested).map((s, n) => DistributeAmount(s.ref, n)))
            .fold(_ => reject(decisionId,
              "suggests amounts it would not accept"), Right(_))
      yield ()
```

`accepts`:

```scala
    case amounts: DecisionQuery.ChooseAmounts => answer match
      case DecisionAnswer.DistributeAnswer(given) =>
        acceptsAmounts(decisionId, amounts.slots, amounts.distinct, given)
      case _ =>
        reject(decisionId, "expects one amount per slot")
```

and, beside `acceptsDistribution`:

```scala
  /** One amount per declared slot, each within its bounds, and with
    * `distinct` no two equal. */
  private def acceptsAmounts(decisionId: String,
      slots: Vector[DistributeSlot], distinct: Boolean,
      amounts: Vector[DistributeAmount]): Either[OathViolation, Unit] =
    val declared = slots.map(_.ref)
    val named = amounts.map(_.ref)
    for
      _ <- require(named.forall(declared.contains), decisionId,
        "names an option it does not offer")
      _ <- require(named.distinct.size == named.size, decisionId,
        "names an option more than once")
      _ <- require(declared.forall(named.contains), decisionId,
        "leaves an option without an amount")
      _ <- slots.find(s => amounts.find(_.ref == s.ref)
          .exists(a => a.amount < s.minimum || a.amount > s.maximum)) match
        case Some(s) => reject(decisionId, s"gives ${label(s.ref)} an amount " +
          s"outside ${s.minimum}..${s.maximum}")
        case None => Right(())
      _ <- require(!distinct ||
          amounts.map(_.amount).distinct.size == amounts.size, decisionId,
        "gives two options the same amount")
    yield ()
```

Fix every other non-exhaustive match the compiler names; a projector or
helper with no meaning for the shape yet returns its "nothing" case and
says so in a comment ("projected in Task 2" for the decision projector).

`Situation.defaultAnswer` (testkit) gains:

```scala
    case Park(Decide(_, _, amounts: DecisionQuery.ChooseAmounts, _, _, _, _),
        _, _, _) if amounts.slots.forall(_.suggested.nonEmpty) =>
      DecisionAnswer.DistributeAnswer(amounts.slots.map(slot =>
        DistributeAmount(slot.ref, slot.suggested.get)))
```

and its scaladoc names the case ("`ChooseAmounts`: the suggested
amounts").

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.walker.* distantlands.testkit.*"`
Expected: every test passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/Decisions.scala \
  src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala \
  src/test/scala/distantlands/gameplay/walker/ChooseAmountsQuerySuite.scala \
  src/test/scala/distantlands/testkit/Situation.scala
git commit -m "feat(walker): a ChooseAmounts query of one amount per slot, distinct when asked

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Add to the pathspec every file whose match the compiler made you extend.

### Task 2: `ChooseAmounts` projected and on the wire

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala` (`DecisionQueryProjection.ChooseAmounts`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala` (encode, `decodeChooseAmounts`, the `"choose-amounts"` form)
- Modify: `src/main/scala/distantlands/application/WalkerDecisionProjector.scala` (project the query)
- Modify: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala` (round trip)
- Modify: frontend matches the compiler names on `DecisionQueryState` (`ParkedDecision.surface` returns `None` for the shape until Task 3; `BoardOptionViews.options` lists the slots' options)
- Modify: `src/test/scala/distantlands/application/WalkerDecisionProjectorSuite.scala` (a parked `ChooseAmounts`)

**Interfaces:**
- Consumes: `DecisionQuery.ChooseAmounts` (Task 1).
- Produces: `DecisionQueryProjection.ChooseAmounts(slots: Vector[DecisionSlotProjection], distinct: Boolean, confirmLabel: String, heading: Option[String] = None)` with `offeredOptions = slots.map(_.option)`; wire form `"choose-amounts"` with fields `slots`, `distinct`, `confirmLabel`, `heading`.

- [ ] **Step 1: Write the failing tests**

In `ProjectionProtocolSuite`, beside the Distribute round trip (read it for
the helpers):

```scala
  test("a choose-amounts query round-trips with its slots and distinct flag"):
    val query = DecisionQueryProjection.ChooseAmounts(
      Vector(DecisionSlotProjection(visionOption, 1, 7, Some(3))),
      distinct = true, confirmLabel = "Place the Visions",
      heading = Some("Seer"))
    assertRoundTrips(query)
```

using the suite's own option fixture and round-trip helper (rename
`visionOption`/`assertRoundTrips` to what the suite has).

`WalkerDecisionProjectorSuite`, beside its distribution tests (it has
`parked` and `projectorFor`):

```scala
  test("a parked choose-amounts projects its slots, bounds, suggestions " +
      "and distinct flag"):
    val context = parked(ActionRef.Recover)
    val slots = Vector(
      DistributeSlot(DecisionOptionRef.FavorBank(Suit.Arcane), 1, 3, Some(1)),
      DistributeSlot(DecisionOptionRef.FavorBank(Suit.Nomad), 1, 3, Some(2)))
    val tree = Sequence(Decide("test.amounts", p1,
      DecisionQuery.ChooseAmounts(slots, distinct = true,
        heading = Some("Place"), confirmLabel = "Place them")))
    val query = projectorFor(tree).project(context).flatMap(_.query) match
      case Some(amounts: DecisionQueryProjection.ChooseAmounts) => amounts
      case other => fail(s"expected one amount per slot, got $other")
    assertEquals(query.slots.map(s => (s.option.id, s.minimum, s.maximum,
      s.suggested)), Vector(("arcane", 1, 3, Some(1)),
      ("nomad", 1, 3, Some(2))))
    assert(query.distinct)
    assertEquals((query.heading, query.confirmLabel),
      (Some("Place"), "Place them"))
```

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.application.WalkerDecisionProjectorSuite"`
(the shared sources compile into the root project, `build.sbt`).
Expected: compile errors.

- [ ] **Step 3: Implement**

DTO, after `Distribute`:

```scala
  /** One amount per slot, each within its bounds; with `distinct` no two
    * equal (Seer's positions). Answered by a `DistributeWire`. */
  final case class ChooseAmounts(slots: Vector[DecisionSlotProjection],
      distinct: Boolean, confirmLabel: String,
      heading: Option[String] = None) extends DecisionQueryProjection:
    def offeredOptions: Vector[DecisionOptionProjection] = slots.map(_.option)
```

Codec, mirroring Distribute's:

```scala
      case DecisionQueryProjection.ChooseAmounts(slots, distinct,
          confirmLabel, heading) => ujson.Obj(
        "form" -> "choose-amounts",
        "slots" -> encoded(slots)(encodeSlot),
        "distinct" -> distinct,
        "confirmLabel" -> confirmLabel,
        "heading" -> stringOption(heading))
```

```scala
      case "choose-amounts" => decodeChooseAmounts(value, path)
```

```scala
  private def decodeChooseAmounts(value: ujson.Obj, path: String)
      : Result[DecisionQueryProjection] = for
    _ <- exact(value, Set("form", "slots", "distinct", "confirmLabel",
      "heading"), path)
    slotRaws <- array(value, "slots", path)
    slots <- traverse(slotRaws, s"$path.slots")(decodeSlot)
    distinct <- boolean(value, "distinct", path)
    confirmLabel <- string(value, "confirmLabel", path)
    heading <- optionalString(value, "heading", path)
  yield DecisionQueryProjection.ChooseAmounts(slots, distinct, confirmLabel,
    heading)
```

(use the codec's own boolean reader; add one beside `int` if it has none).

`WalkerDecisionProjector`, beside the Distribute case:

```scala
      case DecisionQuery.ChooseAmounts(slots, distinct, heading,
          confirmLabel) =>
        described(slots.flatMap(slot => DecisionOption.forRef(slot.ref)))
          .filter(_.size == slots.size).map(options =>
            DecisionQueryProjection.ChooseAmounts(
              slots.zip(options).map { case (slot, option) =>
                DecisionSlotProjection(option, slot.minimum, slot.maximum,
                  slot.suggested) },
              distinct, confirmLabel, heading))
```

A slot whose option the viewer may not identify suppresses the whole
decision, as Distribute's does.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.protocol.* distantlands.application.*" "frontend/test"`
Expected: every test passes.

- [ ] **Step 5: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala \
  src/main/scala/distantlands/application/WalkerDecisionProjector.scala \
  src/test/scala/distantlands/application/WalkerDecisionProjectorSuite.scala
git commit -m "feat(protocol): project a ChooseAmounts query to its owner

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Add the frontend files the compiler made you extend.

### Task 3: The ChooseAmounts panel

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ChooseAmountsDecisionState.scala` (state and draft)
- Create: `frontend/src/main/scala/distantlands/frontend/ChooseAmountsPanelRenderer.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala` (`Surface.ChooseAmounts`, its render call, the surface for the query)
- Modify: `frontend/src/main/scala/distantlands/frontend/SessionDrafts.scala` (`chooseAmounts` draft, `Draft.ChooseAmounts`)
- Modify: `frontend/styles.css` (rows as the Distribute panel's; the clash line)
- Create: `frontend/src/test/scala/distantlands/frontend/ChooseAmountsPanelRenderSuite.scala`

**Interfaces:**
- Consumes: `DecisionQueryState.ChooseAmounts` (the projection, Task 2), `DistributeSlotBounds`, `WalkerPartitionDraft.itemId`, `DecisionAnswerWire.DistributeWire`.
- Produces:
  - `ChooseAmountsDecisionState(slots: Vector[DistributeSlotBounds], distinct: Boolean, amounts: Map[String, Int])` with `amount`, `increment`, `decrement`, `fill`, `drain`, `clashes: Vector[Int]` (the amounts two or more slots share) and `canConfirm`.
  - `WalkerChooseAmountsDraft(context, decisionId, query, state)` with `command: Option[GameCommand.ResolveWalker]` and `reconcile`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, DistributeAmountWire,
  GameIntent => Intent}
import Dom.{click, one}
import ParkedDecision.Surface
import org.scalajs.dom

/** The ChooseAmounts panel at the DOM (Legacy Setup powers design, ruling
  * 15; R25): a row per slot with steppers, and Confirm disabled while two
  * amounts are equal. */
class ChooseAmountsPanelRenderSuite extends munit.FunSuite:
  private def vision(id: String, label: String) =
    DecisionOptionState("vision", id, label)
  private val query = DecisionQueryState.ChooseAmounts(
    Vector(DecisionSlotState(vision("vision:a", "Vision of Faith"), 1, 4,
        Some(1)),
      DecisionSlotState(vision("vision:b", "Vision of Conquest"), 1, 4,
        Some(3))),
    distinct = true, confirmLabel = "Place the Visions",
    heading = Some("Seer: place the Visions in the world deck"))
  private val parked = WalkerDecisionState("setup",
    "setup.legacy.seer.p2", query = Some(query))

  private def opened(): Option[WalkerChooseAmountsDraft] =
    WalkerChooseAmountsDraft.reconcile(None,
      BoardSelectionContext("game", "blue", 9), Some(parked))

  private def render(ui: RecordingControls,
      draft: Option[WalkerChooseAmountsDraft],
      canControl: Boolean = true): dom.Element =
    val panel = dom.document.createElement("div")
    ChooseAmountsPanelRenderer.render(Surface.ChooseAmounts(parked, query),
      draft, canControl, panel, ui)
    panel

  private def amount(panel: dom.Element, item: String): String =
    one(panel, s"""[data-option-id="$item"] .amounts-amount""").textContent

  test("a row per slot at its suggested amount, the heading above"):
    val panel = render(new RecordingControls(), opened())
    assertEquals(one(panel, "h2").textContent,
      "Seer: place the Visions in the world deck")
    assertEquals(amount(panel, "vision:vision:a"), "1")
    assertEquals(amount(panel, "vision:vision:b"), "3")

  test("a stepper stages the draft one step within the slot's bounds"):
    val ui = new RecordingControls()
    click(one(render(ui, opened()),
      """[data-option-id="vision:vision:a"] .amounts-increment"""))
    ui.lastStaged match
      case Some(Draft.ChooseAmounts(draft)) =>
        assertEquals(draft.state.amount("vision:vision:a"), 2)
      case other => fail(s"expected a staged draft, got $other")

  test("two equal amounts disable Confirm and name the clash"):
    val clashing = opened().map(draft => draft.copy(state =
      draft.state.increment("vision:vision:a").increment("vision:vision:a")))
    val panel = render(new RecordingControls(), clashing)
    assert(one(panel, ".amounts-confirm").asInstanceOf[dom.html.Button]
      .disabled)
    assertEquals(one(panel, ".amounts-clash").textContent,
      "Two cards share position 3")

  test("Confirm submits one amount per slot as a distribution"):
    val ui = new RecordingControls()
    click(one(render(ui, opened()), ".amounts-confirm"))
    assertEquals(ui.submitted, Vector(Intent.ResolveWalker(
      "setup.legacy.seer.p2", DecisionAnswerWire.DistributeWire(Vector(
        DistributeAmountWire("vision", "vision:a", 1),
        DistributeAmountWire("vision", "vision:b", 3))))))

  test("a viewer who may not answer sees the steppers disabled"):
    val panel = render(new RecordingControls(), opened(), canControl = false)
    assert(one(panel, ".amounts-increment").asInstanceOf[dom.html.Button]
      .disabled)
```

Read `DistributePanelRenderSuite` for `RecordingControls`' real member
names (`lastStaged`, `submitted`) and the item id format
(`WalkerPartitionDraft.itemId`), and use them. The label says "position"
because the one user is Seer; if the panel should stay generic, the clash
line reads "Two cards share 3" and the heading carries the meaning: pick
one and note it as a deviation.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ChooseAmountsPanelRenderSuite"`
Expected: compile error, `WalkerChooseAmountsDraft` is not found.

- [ ] **Step 3: Implement**

`ChooseAmountsDecisionState.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, DistributeAmountWire,
  GameIntent => GameCommand}

/** One amount per slot, each clamped to its own bounds; with `distinct`,
  * Confirm waits until no two are equal (Legacy Setup powers ruling 15). */
private[frontend] final case class ChooseAmountsDecisionState(
    slots: Vector[DistributeSlotBounds], distinct: Boolean,
    amounts: Map[String, Int]):
  def amount(item: String): Int = amounts.getOrElse(item, 0)
  def increment(item: String): ChooseAmountsDecisionState = step(item, 1)
  def decrement(item: String): ChooseAmountsDecisionState = step(item, -1)
  /** To the slot's maximum. */
  def fill(item: String): ChooseAmountsDecisionState = step(item, Int.MaxValue)
  /** To the slot's minimum. */
  def drain(item: String): ChooseAmountsDecisionState = step(item, Int.MinValue)

  /** The amounts two or more slots share, ascending. */
  def clashes: Vector[Int] =
    if !distinct then Vector.empty
    else slots.map(slot => amount(slot.item)).groupBy(identity)
      .collect { case (value, same) if same.size > 1 => value }
      .toVector.sorted

  def canConfirm: Boolean = clashes.isEmpty

  private def step(item: String, by: Int): ChooseAmountsDecisionState =
    slots.find(_.item == item).fold(this) { bounds =>
      val target = (amount(item).toLong + by).max(bounds.minimum.toLong)
        .min(bounds.maximum.toLong).toInt
      copy(amounts = amounts.updated(item, target))
    }

private[frontend] object ChooseAmountsDecisionState:
  /** Opens at the suggestions, else at each slot's minimum. */
  def opened(slots: Vector[DistributeSlotBounds], distinct: Boolean,
      suggested: Option[Vector[Int]]): ChooseAmountsDecisionState =
    ChooseAmountsDecisionState(slots, distinct, slots.map(_.item).zip(
      suggested.filter(_.size == slots.size)
        .getOrElse(slots.map(_.minimum))).toMap)

/** A draft answer to a parked `ChooseAmounts`, built from the projected
  * query alone, as [[WalkerDistributeDraft]] is. */
private[frontend] final case class WalkerChooseAmountsDraft(
    context: BoardSelectionContext, decisionId: String,
    query: DecisionQueryState.ChooseAmounts,
    state: ChooseAmountsDecisionState):
  def increment(item: String): WalkerChooseAmountsDraft =
    copy(state = state.increment(item))
  def decrement(item: String): WalkerChooseAmountsDraft =
    copy(state = state.decrement(item))
  def fill(item: String): WalkerChooseAmountsDraft =
    copy(state = state.fill(item))
  def drain(item: String): WalkerChooseAmountsDraft =
    copy(state = state.drain(item))
  def canConfirm: Boolean = state.canConfirm

  def command: Option[GameCommand.ResolveWalker] =
    Option.when(canConfirm)(GameCommand.ResolveWalker(decisionId,
      DecisionAnswerWire.DistributeWire(query.slots.map(slot =>
        DistributeAmountWire(slot.option.kind, slot.option.id,
          state.amount(WalkerPartitionDraft.itemId(slot.option)))))))

private[frontend] object WalkerChooseAmountsDraft:
  /** Adopts whichever parked decision projects a choose-amounts query, and
    * drops the draft when the decision, the query or the context changes. */
  def reconcile(previous: Option[WalkerChooseAmountsDraft],
      context: BoardSelectionContext, decision: Option[WalkerDecisionState])
      : Option[WalkerChooseAmountsDraft] =
    decision.flatMap(parked => parked.query.collect {
        case amounts: DecisionQueryState.ChooseAmounts =>
          parked.decisionId -> amounts })
      .map { case (decisionId, query) =>
        previous.filter(draft => draft.context == context &&
            draft.decisionId == decisionId && draft.query == query)
          .getOrElse(WalkerChooseAmountsDraft(context, decisionId, query,
            ChooseAmountsDecisionState.opened(query.slots.map(slot =>
              DistributeSlotBounds(WalkerPartitionDraft.itemId(slot.option),
                slot.minimum, slot.maximum)), query.distinct,
              Option.when(query.slots.nonEmpty &&
                query.slots.forall(_.suggested.nonEmpty))(
                query.slots.flatMap(_.suggested)))))
      }
```

`ChooseAmountsPanelRenderer.scala`: as `DistributePanelRenderer`, with
class names `amounts-slots`, `amounts-slot`, `amounts-label`,
`amounts-decrement`, `amounts-amount`, `amounts-increment`,
`amounts-maximum` (text "of {maximum}"), then, for each clash, a
`p.amounts-clash` "Two cards share position {n}", then the
`amounts-confirm` button labelled `query.confirmLabel`, disabled when
`!canControl || !draft.canConfirm`. Steppers stage
`Draft.ChooseAmounts(...)`, Shift+click calling `fill`/`drain`, with the
same `aria-label`s ("Earlier {label}" / "Later {label}") and tooltip.

`SessionDrafts`: a `chooseAmounts: Option[WalkerChooseAmountsDraft]`
field reconciled as `distribute` is, and `Draft.ChooseAmounts(value)`.
`ParkedDecision`: `Surface.ChooseAmounts(decision, query)`, the surface
for a `DecisionQueryState.ChooseAmounts`, and the render call with
`drafts.chooseAmounts`. `styles.css`: the `amounts-*` rules copy the
`distribute-*` ones; `.amounts-clash` uses the panel's warning colour.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "frontend/test"`
Expected: every frontend test passes.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ChooseAmountsDecisionState.scala \
  frontend/src/main/scala/distantlands/frontend/ChooseAmountsPanelRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala \
  frontend/src/main/scala/distantlands/frontend/SessionDrafts.scala \
  frontend/styles.css \
  frontend/src/test/scala/distantlands/frontend/ChooseAmountsPanelRenderSuite.scala
git commit -m "feat(frontend): a ChooseAmounts panel that refuses two equal amounts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 4: The SetupWorldDeck window and Secret Society's take

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala` (`SetupWorldDeck`)
- Modify: `src/main/scala/distantlands/model/ActionValues.scala` (`ShufflePile.VisionPackets`; `cards` is the world deck)
- Modify: `src/main/scala/distantlands/serialization/CardOperationCodec.scala` (`{"kind": "vision-packets"}`)
- Modify: `src/main/scala/distantlands/gameplay/operations/PileOperations.scala` (writes the world deck)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerShuffles.scala` (the packet layout)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (the window after `SetupBanks`)
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/SecretSociety.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `SecretSocietyCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala`, `SetupLegacyRules.scala` (`secretSociety`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala` (the world deck's Visions for a pending question, R20)
- Modify: tests importing `SecretSocietyCard` from `catalog.holding`, if any (`git grep -ln SecretSocietyCard -- src/test`)
- Modify: `src/test/scala/distantlands/gameplay/setup/SetupWindowsSuite.scala` (the new window)
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/walker/VisionPacketsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/SecretSocietySuite.scala`
- Create: `src/test/scala/distantlands/application/WorldDeckLegaciesProjectionSuite.scala`

**Interfaces:**
- Consumes: `SetupLegacyRules`, `ChronicleLegacyRules.holder`, `PowerAnswers.one`, `VisionRules` (the Conspiracy's id; check its name there), `keepPending`'s pattern (slice 2).
- Produces:
  - `PowerWindow.SetupWorldDeck` (key `"setup.world-deck"`).
  - `ShufflePile.VisionPackets`; `WalkerShuffles.packets(deck: Vector[CardId], dice: WalkerDice): Either[OathViolation, Vector[CardId]]` (package-private, tested directly).
  - `SetupLegacyRules.secretSociety(state: ReadyGame): Option[PlayerId]`.
  - `SecretSocietyCard`, `case object SecretSociety extends ContributingPower`; `SecretSociety.decisionId(player: PlayerId): String`; `SecretSociety.decline`; `SecretSociety.tookVision: NoteKey`; `SecretSociety.setupNoteKeys`.
  - `GamePresentationProjector.deckQuestionPending(ready: ReadyGame, viewer: PlayerId): Boolean` (private).

- [ ] **Step 1: Write the failing tests**

`VisionPacketsSuite.scala`:

```scala
package distantlands.gameplay.walker

import distantlands.gameplay.WalkerDiceFixture
import distantlands.model._

/** Secret Society's reshuffle (spec ruling 14; R19): the denizens keep
  * their order, two Visions go among the first 12 cards and two among the
  * next 17, or, with fewer than 25 denizens, anywhere. */
class VisionPacketsSuite extends munit.FunSuite:
  private val denizens: Vector[CardId] =
    (1 to 30).toVector.map(n => DenizenId(s"d$n"))
  private val visions: Vector[CardId] =
    (1 to 4).toVector.map(n => VisionId(s"vision:$n"))
  private def isVision(card: CardId) = card.isInstanceOf[VisionId]

  private def laid(deck: Vector[CardId]): Vector[CardId] =
    WalkerShuffles.packets(deck, WalkerDiceFixture.reversingShuffle)
      .fold(problem => fail(problem.toString), identity)

  test("two Visions in the top 12 cards, two in the next 17, the denizens " +
      "in their order"):
    val deck = laid(visions ++ denizens)
    assertEquals(deck.take(12).count(isVision), 2)
    assertEquals(deck.slice(12, 29).count(isVision), 2)
    assertEquals(deck.filterNot(isVision), denizens)
    assertEquals(deck.filter(isVision).toSet, visions.toSet)

  test("with fewer than 25 denizens every card stays, the denizens in order"):
    val short = denizens.take(10)
    val deck = laid(short ++ visions)
    assertEquals(deck.size, 14)
    assertEquals(deck.filterNot(isVision), short)

  test("a deck of Visions only is just reordered (Teeming World)"):
    assertEquals(laid(visions).toSet, visions.toSet)
```

Check `WalkerDiceFixture`'s shuffle source name (`reversingShuffle` is
used by `HiddenChronicleCardsSuite`) and that it answers repeated
`shuffle` calls.

`SecretSocietySuite.scala` (l2 a Citizen holds Secret Society; the
production rules, since the step hooks a window):

```scala
package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.{ProcedureNotes, VisionRules}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Secret Society: "Before building the world deck, you may secretly choose
  * a Vision except the Conspiracy to take as a facedown adviser (discard
  * excess advisers). If you do, advance the Visions Drawn marker once."
  * (spec ruling 14; R19, R22). */
class SecretSocietySuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    SecretSocietyCard.id -> true)
  private val rules = Situation.productionRules(catalog)

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def parked(answers: Answers = PartialFunction.empty)
      : Situation = Situation.start(rules.withAnswers(answers))
    .parkedAt(SecretSociety.decisionId(p2))(GameCommand.Begin(record,
      LaterGameFixture.orders(record, p1)))

  private def deckVisions(ready: ReadyGame): Vector[VisionId] =
    ready.game.current.commonCards.worldDeck.collect { case id: VisionId => id }

  test("the holder is offered every Vision but the Conspiracy, Decline " +
      "first"):
    val at = parked()
    val asked = decide(at)
    assertEquals(asked.owner, p2)
    asked.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), SecretSociety.decline +:
          deckVisions(at.ready).filterNot(_ == VisionRules.Conspiracy)
            .map(DecisionOptionRef.Vision(_)))
      case other => fail(s"expected a choose-one, got $other")

  test("a walk resumed at the question takes the Vision facedown, advances " +
      "Visions Drawn and rebuilds the deck in packets"):
    val at = parked()
    val vision = deckVisions(at.ready).filterNot(_ == VisionRules.Conspiracy)
      .head
    val drawn = at.ready.game.current.tracks.visionsDrawn
    val after = at.withAnswers {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Vision(vision))
    }.parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assert(after.game.current.player(p2).get.advisers.contains(
      VisionState(vision, Orientation.FaceDown, Tokens.empty)))
    assertEquals(after.game.current.tracks.visionsDrawn, drawn + 1)
    val deck = after.game.current.commonCards.worldDeck
    assertEquals(deck.take(12).count(_.isInstanceOf[VisionId]), 2)
    assertEquals(deck.slice(12, 29).count(_.isInstanceOf[VisionId]), 2)
    assertEquals(deck.filterNot(_.isInstanceOf[VisionId]),
      at.ready.game.current.commonCards.worldDeck
        .filterNot(_.isInstanceOf[VisionId]))

  test("declining changes nothing and writes no line"):
    val at = parked()
    val after = at.parkedAt(SetupProcedure.pawnDecisionId(p1))()
    assertEquals(after.ready.game.current.commonCards.worldDeck,
      at.ready.game.current.commonCards.worldDeck)
    assert(!NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, after.events).exists(_.text.contains(
      SecretSocietyCard.id.value)))

  test("the line names no Vision"):
    val woken = Situation.wake(rules.withAnswers {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    }, record, LaterGameFixture.orders(record, p1))
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text).contains(
      s"p2 took a Vision as a facedown adviser with " +
        SecretSocietyCard.id.value))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(SecretSociety))
    assert(PowerImplementationStatus.implemented(SecretSocietyCard.power.id))
```

The fixture's world deck must hold at least 25 denizens for the packet
test (`LaterGameFixture` stores two of the 60 at the Empire's sites; the
deal takes hands and discards first). If it holds fewer, the test checks
the fallback instead and a second test builds a deck with enough; read the
deck size in the parked state before writing the assertion. Add a fallback
test under Teeming World (altered Foundation IV): the deck holds only
Visions, and after the take it holds the other four.

`WorldDeckLegaciesProjectionSuite.scala` (R20; Task 6 adds Seer's
cases):

```scala
package distantlands.application

import distantlands.gameplay.setup.{ImperialMaps, LaterGameFixture,
  SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{SecretSociety,
  SecretSocietyCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Situation}

/** Who sees the world deck's Visions while Secret Society's and Seer's
  * holder chooses (spec rulings 14 and 15; R20): the holder, and only
  * while their question is the parked decision. l2, a Citizen, holds the
  * legacies. */
class WorldDeckLegaciesProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    SecretSocietyCard.id -> true)
  private val rules = Situation.productionRules(catalog)
  /** Secret Society takes the first Vision it offers. */
  private val taking: Answers =
    case park if park.decisionId == SecretSociety.decisionId(p2) =>
      park.decide.query match
        case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
        case other => fail(s"expected a choose-one, got $other")

  private def parkedAt(decisionId: String, answers: Answers =
      PartialFunction.empty): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(record, LaterGameFixture.orders(record, p1)))

  private def deckVisions(ready: ReadyGame): Vector[VisionId] =
    ready.game.current.commonCards.worldDeck.collect { case id: VisionId => id }

  private def seenBy(ready: ReadyGame, viewer: Option[PlayerId])
      : Vector[VisionId] = deckVisions(ready)
    .filter(presentation.identifiesAt(ready, viewer, _))

  test("before the question nobody identifies the deck's Visions"):
    val at = parkedAt(ImperialMaps.decisionId).ready
    Vector(Some(p2), Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))

  test("while Secret Society's question waits, only its holder does"):
    val at = parkedAt(SecretSociety.decisionId(p2))
    assertEquals(seenBy(at.ready, Some(p2)), deckVisions(at.ready))
    Vector(Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at.ready, viewer), Vector.empty, viewer))
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("deck", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 5)))
    assert(!projector.project("deck", loaded, p1).walkerDecision
      .exists(_.query.nonEmpty))

  test("after the take nobody identifies the deck's Visions, and only the " +
      "holder their new adviser"):
    val at = parkedAt(SetupProcedure.pawnDecisionId(p1), taking).ready
    Vector(Some(p2), Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at, viewer), Vector.empty, viewer))
    val taken = at.game.current.player(p2).get.advisers.collect {
      case VisionState(id, _, _) => id }
    assertEquals(taken.size, 1)
    assert(presentation.identifiesAt(at, Some(p2), taken.head))
    Vector(Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(at, viewer, taken.head), viewer))
```

`SetupWindowsSuite`: a power folded into `SetupWorldDeck` runs once, after
`SetupBanks`'s and before the first pawn; the key is `"setup.world-deck"`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.VisionPacketsSuite distantlands.gameplay.setup.legacies.SecretSocietySuite distantlands.gameplay.setup.SetupWindowsSuite distantlands.application.WorldDeckLegaciesProjectionSuite"`
Expected: compile errors.

- [ ] **Step 3: Implement**

`PowerWindow`, after `SetupBanks`:

```scala
  /** After the banks, before any pawn: where the world deck game start
    * built is changed (Legacy Setup powers design, rulings 14 and 15).
    * Secret Society and then Seer hook here. Empty with no power. */
  case object SetupWorldDeck extends OtherWindow { val key = "setup.world-deck" }
```

`SetupProcedure.tree`: `imperialMaps ++ Vector(banks, worldDeck) ++ steps`
with `val worldDeck = Sequence(Vector.empty, Some(PowerWindow.SetupWorldDeck))`
and the sketch line `Sequence(Vector.empty)  // window = SetupWorldDeck`.

`ShufflePile`:

```scala
  /** The world deck rebuilt in the card's layout after Secret Society's
    * take: the denizens keep their order and the Visions are placed at
    * random, two among the first 12 cards and two among the next 17, or
    * anywhere with fewer than 25 denizens (Legacy Setup powers ruling 14;
    * R19). The walker fills the order. */
  case object VisionPackets extends ShufflePile
```

`ShufflePile.cards(VisionPackets, game)` is the world deck, and
`PileOperations` writes it as it writes `Search(SearchSource.WorldDeck)`.
`CardOperationCodec` encodes `{"kind": "vision-packets"}` and decodes it.

`WalkerShuffles.ordered`, first case:

```scala
    if shuffle.pile == ShufflePile.VisionPackets then
      packets(cards, dice).map(order => shuffle.copy(order = Some(order)))
    else ...the existing body...
```

and:

```scala
  /** The card's layout for `deck` (R19): denizens in their order, the
    * Visions assigned at random, two into the first 10 denizens' packet of
    * 12 and two into the next 15's of 17, each at a random place in its
    * packet; with fewer than 25 denizens, every Vision at a random place in
    * the whole deck. */
  private[walker] def packets(deck: Vector[CardId], dice: WalkerDice)
      : Either[OathViolation, Vector[CardId]] =
    val (visions, denizens) = deck.partition(_.isInstanceOf[VisionId])
    def placed(cards: Vector[CardId], among: Vector[CardId])
        : Either[OathViolation, Vector[CardId]] =
      val size = cards.size + among.size
      if cards.isEmpty then Right(among)
      else permutation(size).map { order =>
        val at = order.take(cards.size).sorted
        (0 until size).foldLeft((Vector.empty[CardId], cards, among)) {
          case ((done, left, rest), slot) =>
            if at.contains(slot) then (done :+ left.head, left.tail, rest)
            else (done :+ rest.head, left, rest.tail)
        }._1
      }
    def permutation(size: Int): Either[OathViolation, Vector[Int]] =
      if size < 2 then Right((0 until size).toVector)
      else dice.shuffle(size)
    for
      mixed <- permutation(visions.size).map(_.map(visions))
      // Secret Society leaves four Visions; any other count, which no
      // Setup builds, falls back to the whole-deck placement.
      laid <-
        if denizens.size < 25 || mixed.size != 4 then placed(mixed, denizens)
        else for
          first <- placed(mixed.take(2), denizens.take(10))
          second <- placed(mixed.drop(2), denizens.slice(10, 25))
        yield first ++ second ++ denizens.drop(25)
    yield laid
```

Every branch returns the deck's cards exactly once. Check the order
returned against `ShufflePile.cards` as the existing body does.

`SetupLegacyRules.secretSociety`:

```scala
  /** Who may take a Vision before the world deck is built: Secret
    * Society's holder (ruling 14). */
  def secretSociety(state: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(state, SecretSocietyCard.id)
```

`SecretSociety.scala`: move `SecretSocietyCard` unchanged, then:

```scala
/** Secret Society (legacy L28), its SETUP power (Legacy Setup powers
  * design, ruling 14). In the `SetupWorldDeck` window, before Seer, its
  * holder may take one Vision but the Conspiracy from the world deck, as a
  * facedown adviser (R22). A take advances Visions Drawn once and rebuilds
  * the deck in the card's layout (`ShufflePile.VisionPackets`, R19). The
  * Vision is not a starting adviser, so Oaths of Loyalty does not bury it;
  * excess advisers are discarded after the holder's pawn (R21). Line: "p2
  * took a Vision as a facedown adviser with Secret Society". */
case object SecretSociety extends ContributingPower:
  val id: PowerId = SecretSocietyCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupWorldDeck -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.secret-society.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val tookVision: NoteKey = notes.key("secret-society-took", Vector(
    NotePart.Arg(0), NotePart.Text(" took a Vision as a facedown adviser with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** The Visions the holder may take, in deck order. */
  private def takeable(state: ReadyGame): Vector[VisionId] =
    state.game.current.commonCards.worldDeck.collect {
      case id: VisionId if id != VisionRules.Conspiracy => id }

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.secretSociety(state).toVector.flatMap { holder =>
      takeable(state) match
        case Vector() => Vector.empty
        case visions => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +: visions.map(id =>
              DecisionOption.Vision(DecisionOptionRef.Vision(id))),
            heading = Some(
              "Secret Society: you may take a Vision as a facedown adviser"),
            confirmVerb = Some("Take"))),
          Branch((_, pending) =>
            PowerAnswers.one(pending, decisionId(holder)).toVector.collect {
              case DecisionOptionRef.Vision(vision) => vision
            }.flatMap(vision => Vector(
              BuildOps((_, _) => Right(Vector(
                Move(Piece.Card(vision),
                  PositionedLocation(Location.Deck(CardDeck.World)),
                  PositionedLocation(Location.PlayArea(holder)),
                  resultingOrientation = Some(Orientation.FaceDown)),
                AdvanceVisionsDrawn)), required = true),
              Shuffle(ShufflePile.VisionPackets),
              Note(noteId, _ => Some(tookVision.sourceless(
                NoteArg.Player(holder), NoteArg.Card(SecretSocietyCard.id))))))))
    })
```

`gameplay/setup/legacies` must not import `gameplay.actions` if the
architecture forbids it; `VisionRules` lives in `gameplay.actions`, which
`setup` may import (only `actions` is barred from `powers`). Check that a
`Move` from the world deck to a play area as a Vision builds a
`VisionState` adviser (`OperationExecutor`); if a Vision adviser is added
another way (the Vision draw in Search), use that operation.

`LegacySetupPowers.all` appends `SecretSociety`; `noteKeys` appends
`SecretSociety.setupNoteKeys`.

`GamePresentationProjector.identifiesCard`, the world deck case:

```scala
      // Secret Society's or Seer's holder sees the deck's Visions while
      // their question is the parked decision (Legacy Setup powers
      // rulings 14 and 15; R20).
      case CardContainer.Deck(CardDeck.World) | CardContainer.RegionalDiscard(_) =>
        viewer.exists(player => ready.knowledge.advisers
          .getOrElse(player, Vector.empty).exists(_ == id) ||
          (container == CardContainer.Deck(CardDeck.World) &&
            id.isInstanceOf[VisionId] &&
            ready.game.current.turn.phase == Phase.Setup &&
            deckQuestionPending(ready, player)))
```

with

```scala
  /** Whether Setup is parked on `viewer`'s Secret Society or Seer question
    * (R20), read from Setup's open decisions as `keepPending` reads them. */
  private def deckQuestionPending(ready: ReadyGame, viewer: PlayerId)
      : Boolean =
    setupOpen(ready).exists(id => id == SecretSociety.decisionId(viewer) ||
      id == Seer.decisionId(viewer))
```

and `keepPending` rewritten on a shared `setupOpen(ready): Vector[String]`
(the open decision ids). `Seer.decisionId` lands in Task 6: until then the
case checks `SecretSociety.decisionId` alone. Guard the rebuild with the
cheap checks first (phase, a seated holder of either legacy) so a normal
projection never rebuilds Setup's tree.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.walker.* distantlands.gameplay.setup.* distantlands.application.* distantlands.serialization.*"`
Expected: every test passes; every golden unchanged.

- [ ] **Step 5: Regenerate the snapshots and commit**

Expected diff: `procedure.setup secret-society-took` added,
`legacy.secret-society` removed.

```bash
git add src/main/scala/distantlands/model \
  src/main/scala/distantlands/serialization/CardOperationCodec.scala \
  src/main/scala/distantlands/gameplay/operations/PileOperations.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerShuffles.scala \
  src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/gameplay/walker/VisionPacketsSuite.scala \
  src/test/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/application/WorldDeckLegaciesProjectionSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Secret Society takes a Vision and rebuilds the world deck in packets

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Add a codec round-trip test for the new pile kind beside the existing
shuffle codec test (`git grep -ln "set-aside-relics" -- src/test`) and
stage it.

### Task 5: Secret Society's excess discard

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SecretSociety.scala` (`excessStep`, `discardDecisionId`)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (`playerStep` calls it after the `SetupPawnPlaced` window)
- Modify: `src/test/scala/distantlands/gameplay/setup/legacies/SecretSocietySuite.scala`

**Interfaces:**
- Consumes: `AdviserLimit.of`, `AdviserLimit.towardLimit`, `CardPlay.nextRegion`.
- Produces: `SecretSociety.discardDecisionId(player: PlayerId): String` (`setup.legacy.secret-society.<player>.discard`); `SecretSociety.excessStep(catalog: ExecutableCatalog, player: PlayerId): Operation`.

- [ ] **Step 1: Write the failing tests**

In `SecretSocietySuite`:

```scala
  test("under Teeming World the holder discards the excess right after " +
      "their pawn, in one choose-many (R21)"):
    val teeming = record.copy(foundations = record.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)))
    val taking: Answers =
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    val at = Situation.start(rules.withAnswers(taking))
      .parkedAt(SecretSociety.discardDecisionId(p2))(GameCommand.Begin(
        teeming, LaterGameFixture.orders(teeming, p1)))
    val held = at.ready.game.current.player(p2).get
    assert(held.pawnSite.nonEmpty)
    decide(at).query match
      case many: DecisionQuery.ChooseMany =>
        val excess = held.advisers.size - 3
        assertEquals((many.min, many.max), (excess, excess))
      case other => fail(s"expected a choose-many, got $other")
    val after = at.parkedAt(LegacyKeep.decisionId(p2))().ready
    assertEquals(after.game.current.player(p2).get.advisers.size, 3)

  test("a holder within the limit is asked nothing"):
    // p2 holds no starting adviser and draws a hand, so the Vision leaves
    // them one adviser before Choose Adviser: a walk that never parks at
    // the discard settles through Setup.
    val taking: Answers =
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        ChooseOneAnswer(park.decide.query match
          case one: DecisionQuery.ChooseOne => one.options(1).ref
          case other => fail(s"expected a choose-one, got $other"))
    val done = Situation.start(rules.withAnswers(taking))
      .parkedAt(SecretSociety.discardDecisionId(p2))(GameCommand.Begin(
        record, LaterGameFixture.orders(record, p1)))
    assertNotEquals(done.ready.game.current.turn.phase, Phase.Setup)
    assertEquals(done.ready.game.current.player(p2).get.advisers.size, 2)
```

Under Teeming World check how many advisers a player is dealt
(`SetupDeal`) so the expected excess is right; if Teeming deals fewer than
three, give the holder stored starting advisers in the test's Chronicle
instead.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.SecretSocietySuite"`
Expected: compile error, `discardDecisionId` is not found.

- [ ] **Step 3: Implement**

```scala
  def discardDecisionId(player: PlayerId): String =
    s"${decisionId(player)}.discard"

  /** Right after `player`'s pawn, while Secret Society's holder holds more
    * advisers than their limit: discard the excess, one choose-many of
    * exactly that many, each to the discard pile of the region after the
    * pawn's, as Setup's rejected advisers go (R21). Read when reached: only
    * this player's own steps came between the take and here. */
  def excessStep(catalog: ExecutableCatalog, player: PlayerId): Operation =
    Branch((state, _) =>
      val excess = AdviserLimit.towardLimit(state, player) -
        AdviserLimit.of(catalog, state, player)
      if !SetupLegacyRules.secretSociety(state).contains(player) || excess <= 0
      then Vector.empty
      else
        val held = state.game.current.player(player).toVector
          .flatMap(_.advisers)
        Vector(
          Decide(discardDecisionId(player), player, DecisionQuery.ChooseMany(
            excess, excess, held.map(adviser => adviserOption(adviser)),
            heading = Some("Secret Society: discard your excess advisers"),
            confirm = Some(CountedConfirm("Discard", "adviser", "advisers")))),
          BuildOps(discard(player), required = true)))
```

`adviserOption` names a facedown adviser as the reveal step does, by slot
(`DecisionOption.AdviserSlot`), so the owner sees their own cards and the
answer survives redaction; `discard` reads the answer's slots, finds the
pawn's region (`PawnSiteMissing` if none) and moves each card to
`Location.RegionalDiscard(CardPlay.nextRegion(region))` on top, as
`SetupProcedure.chooseAdviser` moves rejects. Check `AdviserLimit` lives in
`gameplay.powers`, which `gameplay/setup/legacies` may import.

`SetupProcedure.playerStep`: after the `SetupPawnPlaced` sequence,
`SecretSociety.excessStep(catalog, participant.playerId)`, then the keep.
Update the scaladoc sketch.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: every test passes; goldens unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/gameplay/setup/legacies/SecretSocietySuite.scala
git commit -m "feat(legacies): Secret Society's holder discards excess advisers after their pawn

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

### Task 6: Seer

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/Seer.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `SeerCard`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/LegacySetupPowers.scala` (after `SecretSociety`), `SetupLegacyRules.scala` (`seer`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala` (`deckQuestionPending` names Seer's id)
- Modify: tests importing `SeerCard` from `catalog.holding` (`git grep -ln "SeerCard" -- src/test`: `LegacySetupSuite`, `OfferCitizenshipSuite`, `ResolveLegaciesSuite`, `SteadfastSuite`, `IronHandSuite`, `LegacyGoalsSuite` and others)
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/SeerSuite.scala`
- Modify: `src/test/scala/distantlands/application/WorldDeckLegaciesProjectionSuite.scala` (Seer's redaction)

**Interfaces:**
- Consumes: `DecisionQuery.ChooseAmounts` (Task 1), `SetupWorldDeck` (Task 4).
- Produces: `SeerCard`, `case object Seer extends ContributingPower`; `Seer.decisionId(player: PlayerId): String`; `Seer.placed(deck: Vector[WorldCardId], positions: Map[VisionId, Int]): Vector[WorldCardId]`; `Seer.placedVisions: NoteKey`; `SetupLegacyRules.seer(state): Option[PlayerId]`.

- [ ] **Step 1: Write the failing tests**

`SeerSuite.scala` (l2 a Citizen holds Seer; production rules):

```scala
package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, DistributeAnswer}
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Seer: "When building the world deck, peek at the Visions and secretly
  * place them in any positions." (spec ruling 15; R23, R24). */
class SeerSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val rules = Situation.productionRules(catalog)
  private def holding(chronicle: Chronicle,
      legacies: (LegacyId, Boolean)*): Chronicle =
    SetupLegacyFixture.holding(chronicle, l2, Role.Citizen, legacies*)
  private val record = holding(LaterGameFixture.chronicle(l1, l2),
    SeerCard.id -> true)
  private def teeming(chronicle: Chronicle): Chronicle = chronicle.copy(
    foundations = chronicle.foundations.updated(FoundationNumber.IV,
      FoundationState(FoundationFace.Altered, Set.empty)))

  private def parked(chronicle: Chronicle,
      answers: Answers = PartialFunction.empty): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(
      Seer.decisionId(p2))(GameCommand.Begin(chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def asked(situation: Situation): DecisionQuery.ChooseAmounts =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.query match
        case amounts: DecisionQuery.ChooseAmounts =>
          assertEquals(decide.owner, p2)
          amounts
        case other => fail(s"expected one amount per Vision, got $other")
      case other => fail(s"expected a decision, parked on $other")

  private def deck(ready: ReadyGame): Vector[WorldCardId] =
    ready.game.current.commonCards.worldDeck
  private def visions(ready: ReadyGame): Vector[VisionId] =
    deck(ready).collect { case id: VisionId => id }

  /** Each Vision at `positions`, in slot order. */
  private def placing(positions: Vector[Int]): Answers =
    case park if park.decisionId == Seer.decisionId(p2) =>
      park.decide.query match
        case amounts: DecisionQuery.ChooseAmounts =>
          DistributeAnswer(amounts.slots.zip(positions).map((slot, at) =>
            DistributeAmount(slot.ref, at)))
        case other => fail(s"expected one amount per Vision, got $other")

  test("the holder is asked one amount per Vision, 1 to the deck's size, " +
      "suggested where each lies"):
    val at = parked(record)
    val query = asked(at)
    val cards = deck(at.ready)
    assertEquals(query.slots, visions(at.ready).map(vision => DistributeSlot(
      DecisionOptionRef.Vision(vision), 1, cards.size,
      Some(cards.indexOf(vision) + 1))))
    assert(query.distinct)

  test("a walk resumed at the question lands each Vision exactly where " +
      "chosen, the denizens in their order"):
    val at = parked(record)
    val before = deck(at.ready)
    val count = visions(at.ready).size
    val after = at.withAnswers(placing((1 to count).toVector))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assertEquals(deck(after).take(count), visions(at.ready))
    assertEquals(deck(after).filterNot(_.isInstanceOf[VisionId]),
      before.filterNot(_.isInstanceOf[VisionId]))

  test("a duplicate position is refused"):
    val at = parked(record)
    val oath = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
    val query = asked(at)
    assert(oath.resolveWalker(at.state, p2, Seer.decisionId(p2),
      DistributeAnswer(query.slots.map(slot =>
        DistributeAmount(slot.ref, 1)))).isLeft)

  test("under Teeming World the holder sets the Visions' order"):
    val at = parked(teeming(record))
    assert(deck(at.ready).forall(_.isInstanceOf[VisionId]))
    val count = visions(at.ready).size
    val after = at.withAnswers(placing((1 to count).reverse.toVector))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))().ready
    assertEquals(deck(after), visions(at.ready).reverse)

  test("Secret Society then Seer for one holder: Seer places the Visions " +
      "left (R24)"):
    val both = holding(LaterGameFixture.chronicle(l1, l2),
      SecretSocietyCard.id -> true, SeerCard.id -> true)
    val at = parked(both, {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => fail(s"expected a choose-one, got $other")
    })
    val taken = at.ready.game.current.player(p2).get.advisers.collect {
      case VisionState(id, _, _) => id }
    assertEquals(taken.size, 1)
    assertEquals(asked(at).slots.map(_.ref),
      visions(at.ready).map(DecisionOptionRef.Vision(_)))
    assert(!visions(at.ready).contains(taken.head))

  test("the line names no Vision"):
    val woken = Situation.wake(rules, record,
      LaterGameFixture.orders(record, p1))
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text).contains(
      s"p2 placed the Visions with ${SeerCard.id.value}"))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Seer))
    assert(LegacySetupPowers.all.indexOf(SecretSociety) <
      LegacySetupPowers.all.indexOf(Seer))
    assert(PowerImplementationStatus.implemented(SeerCard.power.id))
```

`Situation.wake` with no override answers Seer with the suggestions
(Task 1's default), which keeps the deck as it was. Also a unit test of
`Seer.placed`:

```scala
  test("placed inserts in ascending position so each lands where chosen"):
    val (v1, v2) = (VisionId("vision:1"), VisionId("vision:2"))
    val (d1, d2, d3) = (DenizenId("d1"), DenizenId("d2"), DenizenId("d3"))
    assertEquals(Seer.placed(Vector(v1, d1, d2, v2, d3),
      Map(v1 -> 5, v2 -> 2)), Vector(d1, v2, d2, d3, v1))
```

`WorldDeckLegaciesProjectionSuite` gains, with `record` now holding
`SeerCard.id -> true` too (Seer's question follows Secret Society's; the
earlier tests stand):

```scala
  test("while Seer's question waits, only its holder identifies the deck's " +
      "Visions and is shown their positions"):
    val at = parkedAt(Seer.decisionId(p2), taking)
    assertEquals(seenBy(at.ready, Some(p2)), deckVisions(at.ready))
    Vector(Some(p1), None).foreach(viewer =>
      assertEquals(seenBy(at.ready, viewer), Vector.empty, viewer))
    val loaded = LoadedGame(at.state, 1L)
    projector.project("deck", loaded, p2).walkerDecision
        .flatMap(_.query) match
      case Some(amounts: DecisionQueryProjection.ChooseAmounts) =>
        assertEquals(amounts.slots.flatMap(_.suggested),
          deckVisions(at.ready).map(vision =>
            at.ready.game.current.commonCards.worldDeck.indexOf(vision) + 1))
      case other => fail(s"expected the Seer's question, got $other")
    assert(!projector.project("deck", loaded, p1).walkerDecision
      .exists(_.query.nonEmpty))
```

(the projection DTO's package is `distantlands.protocol.projection`;
import `DecisionQueryProjection` and `Seer`).

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.SeerSuite distantlands.application.WorldDeckLegaciesProjectionSuite"`
Expected: compile error, `SeerCard` is not found in `gameplay.setup.legacies`.

- [ ] **Step 3: Implement**

`Seer.scala`: move `SeerCard` unchanged, then:

```scala
/** Seer (legacy L22), its SETUP power (Legacy Setup powers design, ruling
  * 15). In the `SetupWorldDeck` window, after Secret Society (R24), its
  * holder places every Vision in the world deck: one `ChooseAmounts`, a
  * slot per Vision, each 1 to the deck's size, distinct (R23). The Visions
  * are lifted out, the denizens keeping their order, and inserted in
  * ascending position, so each lands where chosen; the new order is a
  * filled `Shuffle` of the world deck. Line: "p2 placed the Visions with
  * Seer". */
case object Seer extends ContributingPower:
  val id: PowerId = SeerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupWorldDeck -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.seer.${player.value}"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val placedVisions: NoteKey = notes.key("seer-placed", Vector(
    NotePart.Arg(0), NotePart.Text(" placed the Visions with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** `deck` with its Visions at `positions` (1 from the top), the other
    * cards in their order. Inserting in ascending position puts each
    * exactly where chosen. */
  def placed(deck: Vector[WorldCardId], positions: Map[VisionId, Int])
      : Vector[WorldCardId] =
    val rest = deck.filterNot(card => positions.contains(card match
      case id: VisionId => id
      case _ => VisionId("")))
    positions.toVector.sortBy(_._2).foldLeft(rest) { case (built, (vision, at)) =>
      val (before, after) = built.splitAt(at - 1)
      (before :+ vision) ++ after
    }

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.seer(state).toVector.flatMap { holder =>
      val deck = state.game.current.commonCards.worldDeck
      val visions = deck.collect { case id: VisionId => id }
      val slots = visions.map(vision => DistributeSlot(
        DecisionOptionRef.Vision(vision), 1, deck.size,
        Some(deck.indexOf(vision) + 1)))
      val query = DecisionQuery.ChooseAmounts(slots, distinct = true,
        heading = Some("Seer: place the Visions in the world deck"),
        confirmLabel = "Place the Visions")
      if DecisionQueries.wellFormed(decisionId(holder), query).isLeft then
        Vector.empty
      else Vector(
        Decide(decisionId(holder), holder, query),
        Branch((live, pending) => answered(pending, holder).toVector.flatMap(
          positions => Vector(
            Shuffle(ShufflePile.Search(SearchSource.WorldDeck), Some(placed(
              live.game.current.commonCards.worldDeck, positions)
              .map(card => card: CardId))),
            Note(noteId, _ => Some(placedVisions.sourceless(
              NoteArg.Player(holder), NoteArg.Card(SeerCard.id))))))))
    })

  private def answered(pending: PendingTree, holder: PlayerId)
      : Option[Map[VisionId, Int]] = pending.answered.collectFirst {
    case Answered(id, DecisionAnswer.DistributeAnswer(amounts), _)
        if id == decisionId(holder) =>
      amounts.collect { case DistributeAmount(DecisionOptionRef.Vision(v), n) =>
        v -> n }.toMap
  }
```

Write `placed`'s filter without the sentinel (`card match { case id:
VisionId => positions.contains(id); case _ => false }`). Check that a
filled `Shuffle` leaf outside a `BuildOps` is run as recorded
(`ProcedureWalker` fills only an empty order) and that `DecisionQueries`
is importable from `gameplay/setup/legacies` (it is in `gameplay.walker`).
If a filled `Shuffle` writes a log line that names the deck, check the
golden in Task 7.

`SetupLegacyRules.seer` reads `SeerCard`'s holder. `LegacySetupPowers.all`
puts `Seer` right after `SecretSociety`; `noteKeys` appends
`Seer.setupNoteKeys`. `deckQuestionPending` adds `Seer.decisionId`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.chronicle.* distantlands.application.* distantlands.catalog.*"`
Expected: every test passes.

- [ ] **Step 5: Regenerate the snapshots and commit**

Expected diff: `procedure.setup seer-placed` added, `legacy.seer` removed.

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/gameplay/chronicle \
  src/test/scala/distantlands/application/WorldDeckLegaciesProjectionSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Seer places every Vision in the world deck

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

Stage every other test file whose `SeerCard` import changed.

### Task 7: The golden log, the docs and the running app

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala` (a `later-game-setup-world-deck` script)
- Create: `src/test/resources/gamelog/later-game-setup-world-deck.*` (generated, reviewed)
- Modify: `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `docs/rules/rules-reference.md`, `docs/ROADMAP.md`, `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md` (Status)

- [ ] **Step 1: The golden script**

```scala
  /** As [[laterGameWoken]], with the world deck's legacy powers: l2 a
    * Citizen holding Secret Society and Seer active (Legacy Setup powers,
    * slice 3). Secret Society takes the first Vision it is offered; Seer
    * puts every Vision left on top of the deck, in its order. */
  def laterGameSetupWorldDeck(using munit.Location): Script =
    val p2 = PlayerId("p2")
    laterGameWokenFrom("later-game-setup-world-deck", chronicle =>
      SetupLegacyFixture.holding(chronicle, LineageId("l2"), Role.Citizen,
        SecretSocietyCard.id -> true, SeerCard.id -> true), {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => throw new AssertionError(s"unexpected query $other")
      case park if park.decisionId == Seer.decisionId(p2) =>
        park.decide.query match
          case amounts: DecisionQuery.ChooseAmounts =>
            DecisionAnswer.DistributeAnswer(amounts.slots.zipWithIndex.map(
              (slot, n) => DistributeAmount(slot.ref, n + 1)))
          case other => throw new AssertionError(s"unexpected query $other")
    })
```

Register it in `named`, generate with
`GAMELOG_GOLDEN=write ./sbtw 'testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *later-game-setup-world-deck*'`,
and review both files line by line: "p2 took a Vision as a facedown
adviser with Secret Society" and "p2 placed the Visions with Seer" with no
Vision named in the other seat's log (p2's own log may name the Vision it
took, as it names its own advisers); no line names a position; the
excess-discard question does not appear (p2 drew a hand). A shuffle line,
if the log writes one, names no card.

- [ ] **Step 2: Docs**

- `docs/rules/ambiguities.md`, "Legacy Setup powers rulings": the design's
  rulings 14 and 15 and R18-R25 as ruled, numbered on from 30; the opening
  sentence names them.
- `docs/rules/implementation-traceability.md`: a "Legacy SETUP powers in
  the world deck" row (Secret Society, Seer, `ChooseAmounts`, the panel,
  the suites, the golden and the running-app check); the "Six foundation
  setup effects and world-deck construction" row's note points to it.
- `docs/rules/rules-reference.md`, later games step 6: a "Digital:" note
  for Secret Society and Seer.
- `docs/ROADMAP.md`, "Legacy powers: setup": Secret Society and Seer move
  to the landed list.
- The spec's Status: slice 3 has landed, with its commit range.

- [ ] **Step 3: Run the gate**

The gate at the top of this plan.

- [ ] **Step 4: The running app**

No code unless the check finds a fault; a fault gets a failing test and a
fix commit, then the check is repeated.

1. Build a later game's Chronicle in which l2, a Citizen, holds Secret
   Society and Seer active: a scratch test (not committed) that writes
   `SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2), l2,
   Role.Citizen, SecretSocietyCard.id -> true, SeerCard.id -> true)` to
   the scratchpad in the format the start page imports (find the import in
   `frontend/.../ChronicleTransfer.scala` and the codec it reads).
2. `./sbtw frontend/fastLinkJS`, then `preview_start` with
   `.claude/launch.json`'s `oath-server` (port 8080), and open
   `http://127.0.0.1:8080/?mode=server`.
3. Import the Chronicle, seat three players with p1 first on l1, Create
   game. As p1, answer the Reliquary pick and Imperial Maps.
4. Switch the toolbar to p2: Secret Society's panel offers Decline and
   four Visions drawn as cards. Switch to p1: the panel says it waits on
   p2 and names no Vision. Back as p2, take a Vision.
5. As p2: Seer's panel shows one row per Vision with its position,
   steppers, "of {deck size}", and "Place the Visions". Step one Vision
   onto another's position: Confirm is disabled and "Two cards share
   position N" shows. Step it away and confirm.
6. As p1 and as p2, the Game Log says "p2 took a Vision as a facedown
   adviser with Secret Society" and "p2 placed the Visions with Seer"; as
   p1, p2's board shows one more facedown adviser.
7. Record what was seen, with screenshots or the page's text for steps 4
   to 6, in the commit message of Step 5 and in the report; stop the
   server.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala \
  src/test/resources/gamelog/later-game-setup-world-deck.actor.log \
  src/test/resources/gamelog/later-game-setup-world-deck.other.log \
  docs/rules docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-design.md
git commit -m "test(legacies): a golden for the world deck's legacy powers, and their docs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

## Self-review notes

- Spec ruling 14: Task 4 (take, Visions Drawn, the packet rebuild and its
  fallback, hidden draw), Task 5 (excess discard), the Oaths note (the
  Vision is never a starting adviser: no bury, by construction), the line
  (Task 4) and "one more facedown adviser" (Task 7's check). Ruling 15:
  Task 6 (one question, no Decline, exact landing, duplicate refused,
  Teeming World) on Tasks 1-3's query, projection and panel. Rulings 1-4:
  holders read live through `SetupLegacyRules`; Decline on Secret Society;
  one choose-many for the discard.
- The slice 3 test list: Secret Society's Vision, Visions Drawn, the 12/17
  layout (Task 4); the fallback under 25 denizens (Tasks 4 and the packet
  suite); the excess discard under Teeming World (Task 5); Seer's exact
  positions, a duplicate refused, Teeming World (Task 6); Secret Society
  then Seer for one holder (Task 6); redaction of both (Tasks 4 and 6); the
  frontend control (Task 3); a golden (Task 7); resume tests for each new
  decision (Tasks 4, 5, 6).
- Shapes: Seer's slots and bounds are read at its own question, after
  Secret Society's answer, the only change before it; Secret Society's
  options read the deck game start built; the discard's count is read
  after the holder's own pawn.
- Names across tasks: `DecisionQuery.ChooseAmounts`,
  `DecisionQueryProjection.ChooseAmounts`, `ChooseAmountsDecisionState`,
  `WalkerChooseAmountsDraft`, `ChooseAmountsPanelRenderer`,
  `Surface.ChooseAmounts`, `Draft.ChooseAmounts`;
  `PowerWindow.SetupWorldDeck`; `ShufflePile.VisionPackets`,
  `WalkerShuffles.packets`; `SetupLegacyRules.secretSociety`, `.seer`;
  `SecretSociety.decisionId`, `.decline`, `.tookVision`,
  `.discardDecisionId`, `.excessStep`; `Seer.decisionId`, `.placed`,
  `.placedVisions`; `GamePresentationProjector.deckQuestionPending`,
  `setupOpen`.
