# Catalog Batch 10 Slice 10: Shuffle Every Discard Pile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Autumn Wind (213), a WHEN PLAYED power that shuffles all three discard piles together and deals them back out as evenly as possible, its user deciding ties.

**Architecture:**
- The Chronicle's gathering pile becomes a general one. `Location.ChronicleGathering`, `ShufflePile.ChronicleGathering`, `CardContainer.ChronicleGathering` and the field `CurrentState.chronicleGathering` are renamed `CardGathering` / `cardGathering`, and the wire kind `chronicle-gathering` becomes `card-gathering`. It holds cards gathered for a shuffle, Visions included. Old journals need not decode (memory: "No old replay support").
- Autumn Wind is a `WhenPlayedPower` with no new primitive. One `Branch` reads the three piles' total. With no cards it does nothing. Otherwise:
  - When the total does not divide by 3, a `ChooseMany` asks for exactly the remainder's count of piles (1 or 2) to take an extra card. The question comes before the shuffle, so `HiddenOutcomes` never stops the legality search ahead of it.
  - A `BuildOps` moves every discard pile, bottom first, Cradle then Provinces then Hinterland, into the gathering pile.
  - `Shuffle(ShufflePile.CardGathering)` shuffles it; the walker records the order (`WalkerShuffles`).
  - A `BuildOps` deals the pile from its head, round-robin in region order. Each round gives one card to every pile still short of its share, so the named piles take one card more.
  - One line names each pile's count.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Readings" (the Autumn Wind bullet; "Pile names in lines")
- "Slice 10: Shuffle every discard pile"
- "Seams opened" (the `CardGathering` row)
- "Testing"

Card text (`HoldingBeastDenizens.scala`), Beast, free: "**WHEN PLAYED,** shuffle together all discard piles, then deal these cards to the discard piles as evenly as possible. You decide ties." The card database has no FAQ for it.

**Plan-time findings** (each goes into the spec in Task 2):
- **The rename goes past the spec's two names.** The spec renames `Location.ChronicleGathering` and `ShufflePile.ChronicleGathering`. The same pile is also `CardContainer.ChronicleGathering` and the state field `chronicleGathering`, and a failure names it "gathered denizens". All of them change, so one pile has one name. `GameState` is never written to the wire (games reload from the journal), so only the two codec kinds change on the wire.
- **The line names the piles.** The spec's line, "Shuffled the discard piles together and dealt 4, 4 and 3.", does not say which pile took which count. A reader cannot tell where the extra cards went. The plan's line names them: "Shuffled the discard piles together and dealt 4 to the Cradle, 4 to the Provinces and 3 to the Hinterland." Since every line names the answer, the question is in `narratedDecisions`, and the log posts no "Chose ..." line for it.
- **The question offers the piles themselves.** Its options are `DecisionOption.DiscardPile`, which the board already shows at each pile (a Search source offers the same). The client's board draft supports a choose-many over them.
- **A Search's own discards are dealt too.** A Search discards its unkept cards before it plays the kept one (`SearchProcedure`, `removeOthers` before `cardTree`). A searched Autumn Wind therefore gathers and deals those cards. One test pins this.
- **The gathering pile is never seen mid-deal.** The presentation names every gathering card except the last three to all viewers, for the World task's faceup stacks. Autumn Wind gathers, shuffles and deals with no question between, so no viewer ever sees its pile. The projector's comment says so; its rule does not change.

This is the tenth of 14 slice plans, written against `0.2.0-alpha` at `95838b1f`, after slice 8 merged and before slice 9 merged.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-10` on branch `batch-10-slice-10`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - **Slice 9 first.** Task 2 edits what slice 9 also edits: `HoldingBeastDenizens.scala` (slice 9 removes Bracken, this slice removes Autumn Wind, the file's last card), the four Campaign suites that import both cards, the spec's status line and the ROADMAP bullet that names both. Before Task 2, check that `batch-10-slice-9` has merged into local `0.2.0-alpha` (`git merge-base --is-ancestor batch-10-slice-9 0.2.0-alpha`, or the branch is gone and `git log 0.2.0-alpha` shows "implement Bracken"). If it has, rebase this branch onto `0.2.0-alpha` (`git rebase 0.2.0-alpha`). If it has not, stop after Task 1 and report. Task 1 touches none of slice 9's lines and may run before.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Shell:** zsh. Never write a bare `===` or `=word` in a command.
- **Build:** the compiler runs with `-Werror`, so unused imports and unused values fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, run in the background, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains. The files are `src/test/resources/notes/declared-keys.txt` and `src/test/resources/powers/unimplemented.txt`.
- **Log lines:** a line names only non-actors (memory: "Log lines omit actor").
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").

---

### Task 1: Rename the gathering pile `CardGathering`

**Files:**
- Modify (mechanical rename): every file `git grep` finds below. At `95838b1f` these are:
  - `src/main/scala/oathdigital/model/Location.scala`, `CardIndex.scala`, `ActionValues.scala`, `GameState.scala`, `OperationError.scala`
  - `src/main/scala/oathdigital/gameplay/operations/PileOperations.scala`, `OperationStateWrites.scala`, `OperationStateAdapter.scala`, `CardMovementOperations.scala`, `CardKnowledgeMoves.scala`
  - `src/main/scala/oathdigital/gameplay/chronicle/GatherDenizens.scala`, `ProtectAndDispossess.scala`
  - `src/main/scala/oathdigital/serialization/WalkerValueCodec.scala`, `WalkerOperationCodec.scala`
  - `src/main/scala/oathdigital/application/GamePresentationProjector.scala`
  - `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`
  - `src/test/scala/oathdigital/gameplay/operations/ShuffleOperationSuite.scala`, `ChroniclePilesSuite.scala`
  - `src/test/scala/oathdigital/gameplay/chronicle/ChronicleWalk.scala`, `WorldTaskSuite.scala`
  - `src/test/scala/oathdigital/application/gamelog/WorldLogLinesSuite.scala`
  - `src/test/scala/oathdigital/application/ChroniclePilesIdentitySuite.scala`
- Test: `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces:
  - `Location.CardGathering`, `ShufflePile.CardGathering`, `CardContainer.CardGathering`
  - `CurrentState.cardGathering: Vector[WorldCardId]` (the state class in `GameState.scala` that held `chronicleGathering`)
  - Wire kind `"card-gathering"` for the location and the shuffled pile.

- [ ] **Step 1: Write the failing test**

In `WalkerOperationWireSuite.scala`, add after the test "every Location variant round-trips through the walker codec":

```scala
  test("the gathering pile is card-gathering on the wire, as a location " +
      "and as a shuffled pile"):
    val events = Vector(
      recordedOperation(PayCost(PlayerId("red"), Location.CardGathering,
        Cost(favor = 1))),
      recordedOperation(Shuffle(ShufflePile.CardGathering,
        Some(Vector(DenizenId("denizen"))))))
    val encoded =
      ujson.read(encodedStreamThatRoundTrips("walker", events)).arr
    def op(at: Int): ujson.Value = encoded(at)("payload")("step")("ops")(0)
    assertEquals(op(0)("placedAt")("kind").str, "card-gathering")
    assertEquals(op(1)("pile")("kind").str, "card-gathering")
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./sbtw "testOnly oathdigital.serialization.WalkerOperationWireSuite"`
Expected: FAIL to compile, "value CardGathering is not a member of object oathdigital.model.Location".

- [ ] **Step 3: Rename every use**

Run from the worktree root:

```bash
git grep -lE 'ChronicleGathering|chronicleGathering|chronicle-gathering|"gathered denizens"' -- src \
  | xargs perl -pi -e 's/ChronicleGathering/CardGathering/g; s/chronicleGathering/cardGathering/g; s/chronicle-gathering/card-gathering/g; s/"gathered denizens"/"gathered cards"/g'
```

Then confirm nothing is left: `git grep -nE 'ChronicleGathering|chronicleGathering|chronicle-gathering' -- src` prints nothing. Docs under `docs/` keep the old names: they are history.

- [ ] **Step 4: Rewrite the comments that describe the pile**

The renamed pile is no longer the World task's alone. Replace these comments.

In `src/main/scala/oathdigital/model/Location.scala`, the comment above `case object CardGathering`:

```scala
  /** Cards gathered for a shuffle, Visions included: the nine denizens the
    * World task deals, stack by stack (CR p. 38), or every discard pile for
    * Autumn Wind. In the World task the facedown stack is always the last
    * three, and the cards before it are faceup. */
```

In `src/main/scala/oathdigital/model/GameState.scala`, the comment above `cardGathering`:

```scala
    // Cards gathered for a shuffle: the World task's dealt denizens (CR p.
    // 38), stack by stack, the facedown stack last when the protected stack
    // leaves; or the discard piles Autumn Wind deals out again.
```

In `src/main/scala/oathdigital/model/ActionValues.scala`, the `ShufflePile` doc comment:

```scala
/** A pile the walker can shuffle with a recorded order: a pile Search draws
  * from, the edifice deck (Build Edifice draws a random edifice per suit),
  * the set-aside relics (Scatter Relics deals them out), the gathering pile
  * (the World task, Autumn Wind), or a campaign pile: a suited reserve, the
  * Reliquary or the Dispossessed. */
```

In `src/main/scala/oathdigital/gameplay/operations/CardKnowledgeMoves.scala`, in the object's doc comment, change "the Chronicle's gathered, newly dispossessed and suited reserve piles" to "the gathering pile, the Chronicle's newly dispossessed and suited reserve piles". Re-wrap the paragraph to 80 columns.

In `src/main/scala/oathdigital/application/GamePresentationProjector.scala`:
- In the doc comment of `identifiesCard`, change the bullet "The World task's gathered denizens are named to every viewer except the last three, the facedown stack (CR p. 38)." to:

```scala
    *  - The gathering pile's cards are named to every viewer except the
    *    last three, the World task's facedown stack (CR p. 38). Autumn Wind
    *    gathers, shuffles and deals in one walk with no question between,
    *    so no viewer sees its pile.
```

- Leave the `case CardContainer.CardGathering =>` branch's code as it is.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.serialization.* oathdigital.gameplay.chronicle.* oathdigital.gameplay.operations.* oathdigital.application.*"`
Expected: PASS. The Chronicle suites show the World task's gathering is unchanged by the rename.

- [ ] **Step 6: Commit**

```bash
git add $(git diff --name-only)
git status --short
```

`git status --short` must list only the files Step 3 and Step 4 changed and `WalkerOperationWireSuite.scala`, all staged. Then:

```bash
git commit -m "$(cat <<'EOF'
refactor(model): rename the Chronicle's gathering pile CardGathering

Autumn Wind gathers every discard pile into it, so the pile is no
longer the Chronicle's alone. The wire kind becomes card-gathering.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Autumn Wind, docs and gates

Before this task, apply the "Slice 9 first" constraint above.

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/powers/whenplayed/AutumnWind.scala`
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingBeastDenizens.scala` (remove `AutumnWindCard`; delete the file if no card is left)
- Modify: `src/main/scala/oathdigital/gameplay/powers/whenplayed/WhenPlayedPowers.scala` (register `AutumnWind`)
- Modify: the four Campaign suites that import `AutumnWindCard` from `oathdigital.catalog.holding`: `BattleAxesSuite.scala`, `NatureWorshipSuite.scala`, `WalledGardenSuite.scala`, `WildMountsSuite.scala` under `src/test/scala/oathdigital/gameplay/powers/campaign/`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt`
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`, `docs/ROADMAP.md`
- Test: `src/test/scala/oathdigital/gameplay/powers/whenplayed/AutumnWindSuite.scala`

**Interfaces:**
- Consumes: `Location.CardGathering`, `ShufflePile.CardGathering`, `cardGathering` (Task 1).
- Produces:
  - `AutumnWindCard` in `oathdigital.gameplay.powers.whenplayed` (id `DenizenId("213")`, power `denizen.autumn-wind`)
  - `case object AutumnWind extends WhenPlayedPower` with `decisionId = "power.autumn-wind.extra"`, `dealt: NoteKey` and `heading(extra: Int): String`

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/oathdigital/gameplay/powers/whenplayed/AutumnWindSuite.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.WalkerDiceFixture
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.powers.{NoteText, SearchFixture}
import oathdigital.gameplay.walker.WalkerDice
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Autumn Wind (card 213, catalog batch 10, slice 10), WHEN PLAYED: every
  * discard pile is shuffled together and dealt back out as evenly as
  * possible. When the cards do not divide by 3, p1 names the piles that
  * take the extra cards, before the shuffle. */
class AutumnWindSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[AutumnWind.type]
  private val played = hook(AutumnWind.cardId)
  private val keepingOrder: WalkerDice =
    WalkerDiceFixture.shufflingBy(count => (0 until count).toVector)
  /** Eight Discord denizens with no power that applies in a pile. */
  private val cards: Vector[WorldCardId] = SearchFixture.discordCards

  /** p1 holds Autumn Wind faceup, and the Cradle, Provinces and Hinterland
    * discard piles hold `cradle`, `provinces` and `hinterland`, bottom
    * first. */
  private def p1HoldsAutumnWindOverPiles(cradle: Vector[WorldCardId],
      provinces: Vector[WorldCardId], hinterland: Vector[WorldCardId])
      : ReadyGame =
    p1AtHomeSite.adviser(p1, AutumnWindCard)
      .discarded(Region.Cradle, cradle*)
      .discarded(Region.Provinces, provinces*)
      .discarded(Region.Hinterland, hinterland*).ready

  /** The three discard piles, bottom first, in region order. */
  private def piles(ready: ReadyGame): Vector[Vector[WorldCardId]] =
    Region.all.map(Look(ready).discardPile)

  private def pilesAnswer(regions: Region*): DecisionAnswer =
    DecisionAnswer.ChooseManyAnswer(
      regions.toVector.map(DecisionOptionRef.DiscardPile.apply))

  private def dealtLine(cradle: Int, provinces: Int, hinterland: Int)
      : Vector[NoteText.Said] =
    Vector(NoteText.Said("dealt", "Shuffled the discard piles together " +
      s"and dealt $cradle to the Cradle, $provinces to the Provinces and " +
      s"$hinterland to the Hinterland.", covers = false))

  /** Seven cards: four in the Cradle, two in the Provinces, one in the
    * Hinterland. */
  private val sevenCards: ReadyGame = p1HoldsAutumnWindOverPiles(
    cards.take(4), cards.slice(4, 6), cards.slice(6, 7))

  test("an even total is dealt round-robin from the Cradle with no " +
      "question, a Vision among the cards"):
    val faith = VisionRules.Faith
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), cards.slice(3, 5),
      Vector(faith))
    val walked = walkShuffling(ready, power, played, keepingOrder)
    assertEquals(walked.asked, Vector.empty)
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3)), Vector(cards(1), cards(4)),
      Vector(cards(2), faith)))
    assertEquals(walked.end.game.current.cardGathering, Vector.empty)
    assertEquals(said(power, walked.events), dealtLine(2, 2, 2))

  test("with one card over, p1 names the one pile that takes it, before " +
      "anything is gathered"):
    val first = Outcomes.parked(fire(sevenCards, power, played,
      keepingOrder))
    assertEquals(ops(first.events), Vector.empty)
    val asked = question(sevenCards, power, played, first.tree)
    assertEquals(asked.decisionId, AutumnWind.decisionId)
    assertEquals(asked.query, DecisionQuery.ChooseMany(1, 1,
      Region.all.map(region =>
        DecisionOption.DiscardPile(DecisionOptionRef.DiscardPile(region))),
      heading = Some("Autumn Wind: choose the pile that takes the extra " +
        "card"),
      confirm = Some(CountedConfirm("Deal an extra card to", "pile",
        "piles"))))

  test("the named pile takes the extra card"):
    val walked = walkShuffling(sevenCards, power, played, keepingOrder,
      pilesAnswer(Region.Hinterland))
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3)), Vector(cards(1), cards(4)),
      Vector(cards(2), cards(5), cards(6))))
    assertEquals(said(power, walked.events), dealtLine(2, 2, 3))

  test("with two cards over, p1 names two piles"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(4), cards.slice(4, 8),
      Vector.empty)
    val walked = walkShuffling(ready, power, played, keepingOrder,
      pilesAnswer(Region.Cradle, Region.Hinterland))
    assertEquals(walked.asked.map(_.query).collect {
      case many: DecisionQuery.ChooseMany => (many.min, many.max, many.heading)
    }, Vector((2, 2,
      Some("Autumn Wind: choose the 2 piles that take the extra cards"))))
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3), cards(6)), Vector(cards(1), cards(4)),
      Vector(cards(2), cards(5), cards(7))))
    assertEquals(said(power, walked.events), dealtLine(3, 2, 3))

  test("an answer naming the wrong number of piles is refused"):
    val first = Outcomes.parked(fire(sevenCards, power, played,
      keepingOrder))
    assert(resumeBy(sevenCards, power, played, first.tree,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle, Region.Provinces),
      p1, keepingOrder).isLeft)

  test("fewer cards than piles go only to the named piles"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(2), Vector.empty,
      Vector.empty)
    val walked = walkShuffling(ready, power, played, keepingOrder,
      pilesAnswer(Region.Provinces, Region.Hinterland))
    assertEquals(piles(walked.end), Vector(
      Vector.empty, Vector(cards(0)), Vector(cards(1))))
    assertEquals(said(power, walked.events), dealtLine(0, 1, 1))

  test("the shuffle sets the order of the deal"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), cards.slice(3, 5),
      cards.slice(5, 6))
    val walked = walkShuffling(ready, power, played,
      WalkerDiceFixture.reversingShuffle)
    assertEquals(piles(walked.end), Vector(
      Vector(cards(5), cards(2)), Vector(cards(4), cards(1)),
      Vector(cards(3), cards(0))))

  test("with every pile empty nothing is asked, shuffled or said"):
    val ready = p1HoldsAutumnWindOverPiles(Vector.empty, Vector.empty,
      Vector.empty)
    val done = Outcomes.finished(fire(ready, power, played))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(said(power, done.events), Vector.empty)

  test("nobody knows a dealt card, not even one known in its pile"):
    val known = cards(0)
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), Vector.empty,
      Vector.empty)
    val knowing = ready.copy(knowledge = ready.knowledge.copy(
      advisers = Map(p2 -> Vector(known))))
    val walked = walkShuffling(knowing, power, played, keepingOrder)
    assert(!Look(walked.end).knowsAdviser(p2, known))

  test("the journal rebuilds the end, and every step crosses the wire"):
    val walked = walkShuffling(sevenCards, power, played, keepingOrder,
      pilesAnswer(Region.Cradle))
    assertJournalRebuilds(sevenCards, walked.events, walked.end)
    assertWireRoundTrips(walked.events)

  test("searched, the Search's own discards are among the cards dealt"):
    import SearchFixture.{after, keep, place, rules, staged, start}
    val drawnBeside = cards.take(2)
    val ready = staged(AutumnWind.cardId +: drawnBeside, supply = 5)
    val placed = accepted(for
      started <- start(ready)
      chosen <- keep(started, AutumnWind.cardId)
      placedFaceup <- place(chosen, AutumnWind.cardId, PlayedAs.Faceup)
    yield placedFaceup)
    // The two unkept cards went to the Provinces before the play, so two
    // cards are dealt: the question asks for two piles.
    val done = accepted(rules.resolveWalker(placed.state, p1,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle, Region.Hinterland)))
    val end = piles(after(done))
    assertEquals(end.map(_.size), Vector(1, 0, 1))
    assertEquals(end.flatten.toSet, drawnBeside.toSet)
```

If `staged` draws more than 3 cards, so that the world deck's own cards join the hand, read the unkept cards from `after(started).game.current.temporaryHands(p1)` instead of `drawnBeside`, and expect the counts the dealt total gives. Report the change.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.AutumnWindSuite"`
Expected: FAIL to compile, "Not found: AutumnWind" (and `AutumnWindCard` still in `oathdigital.catalog.holding`).

- [ ] **Step 3: Write Autumn Wind**

Remove `AutumnWindCard` from `src/main/scala/oathdigital/catalog/holding/HoldingBeastDenizens.scala`. If slice 9 has already moved `BrackenCard` out, the file holds no card: delete it with `git rm`.

Create `src/main/scala/oathdigital/gameplay/powers/whenplayed/AutumnWind.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object AutumnWindCard extends Denizen(DenizenId("213"), "Autumn Wind", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.autumn-wind"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** shuffle together all discard piles, then " +
      "deal these cards to the discard piles as evenly as " +
      "possible. You decide ties.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Autumn Wind (card 213), WHEN PLAYED: "shuffle together all discard
  * piles, then deal these cards to the discard piles as evenly as
  * possible. You decide ties." (catalog batch 10, slice 10).
  *
  * The effect is one `Branch` that reads the piles' total. With no cards
  * nothing happens and no line is written. When the total does not divide
  * by 3, the user names as many piles as the remainder to take one card
  * more. The question comes first: the shuffle shows what nobody may see
  * yet, so the legality search stops there (`HiddenOutcomes`). Then every
  * pile, bottom first and in region order, goes to the gathering pile, the
  * walker shuffles it and records the order, and the pile is dealt from
  * its head. Each round of the deal gives one card to every pile still
  * short of its share, in region order, and each card goes on top.
  *
  * Nothing parks between the gathering and the deal, so no viewer ever
  * sees the gathering pile. Every dealt card passed through it, so nobody
  * knows any of them (`CardKnowledgeMoves`). The line names each pile's
  * count, and so the answer.
  */
case object AutumnWind extends WhenPlayedPower:
  val cardId: DenizenId = AutumnWindCard.id
  val id: PowerId = AutumnWindCard.power.id
  val decisionId: String = "power.autumn-wind.extra"

  private val notes = NoteSet(id)
  /** "Shuffled the discard piles together and dealt {4} to the Cradle, {4}
    * to the Provinces and {3} to the Hinterland." */
  val dealt: NoteKey = notes.key("dealt", Vector(
    NotePart.Text("Shuffled the discard piles together and dealt "),
    NotePart.Arg(0), NotePart.Text(" to the Cradle, "), NotePart.Arg(1),
    NotePart.Text(" to the Provinces and "), NotePart.Arg(2),
    NotePart.Text(" to the Hinterland.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The question's heading for `extra` extra cards. */
  def heading(extra: Int): String =
    if extra == 1 then "Autumn Wind: choose the pile that takes the extra card"
    else s"Autumn Wind: choose the $extra piles that take the extra cards"

  private val gathering = PositionedLocation(Location.CardGathering)

  def effect(ctx: PowerCtx): Vector[Operation] = Vector(Branch((live, _) =>
    val total = Region.all.map(region =>
      live.game.current.commonCards.discard(region).size).sum
    val extra = total % Region.all.size
    if total == 0 then Vector.empty
    else
      val ask = if extra == 0 then Vector.empty
        else Vector(question(ctx.actor, extra))
      ask ++ Vector[Operation](
        BuildOps((ready, _) => Right(gathered(ready, ctx.actor)),
          required = true),
        Shuffle(ShufflePile.CardGathering),
        BuildOps((ready, pending) => takers(pending, extra)
          .map(deal(ready, _, ctx.actor)), required = true),
        Note(id, states => Some(dealt(PowerSourceRef.Card(cardId),
          Region.all.map(region => NoteArg.Number(
            states.now.game.current.commonCards.discard(region).size))*))))))

  private def question(actor: PlayerId, extra: Int): Decide =
    Decide(decisionId, actor, DecisionQuery.ChooseMany(extra, extra,
      Region.all.map(region =>
        DecisionOption.DiscardPile(DecisionOptionRef.DiscardPile(region))),
      heading = Some(heading(extra)),
      confirm = Some(CountedConfirm("Deal an extra card to", "pile",
        "piles"))))

  /** Every discard pile, bottom first, in region order, to the gathering
    * pile. */
  private def gathered(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    Region.all.flatMap(region =>
      ready.game.current.commonCards.discard(region).map(card =>
        Move(Piece.Card(card),
          PositionedLocation(Location.RegionalDiscard(region)), gathering,
          by = Some(actor))))

  /** The piles that take an extra card: none when the total divides by 3,
    * otherwise the `extra` different piles answered. */
  private def takers(pending: PendingTree, extra: Int)
      : Either[OathViolation, Set[Region]] =
    if extra == 0 then Right(Set.empty)
    else PowerAnswers.many(pending, decisionId)
      .toRight(PowerAnswers.missing(decisionId)).flatMap { refs =>
        val regions = refs.collect {
          case DecisionOptionRef.DiscardPile(region) => region }
        Either.cond(refs.size == extra && regions.distinct.size == extra,
          regions.toSet, OathViolation.InvalidEventOrder(
            s"Autumn Wind deals its extra cards to $extra different piles"))
      }

  /** The gathering pile dealt from its head. Each round gives one card to
    * every pile still short of its share, in region order, so `takers`
    * each take one card more than the others. */
  private def deal(ready: ReadyGame, takers: Set[Region], actor: PlayerId)
      : Vector[CoreOperation] =
    val pile = ready.game.current.cardGathering
    val share = pile.size / Region.all.size
    def quota(region: Region): Int =
      share + (if takers.contains(region) then 1 else 0)
    val seats = (0 to share).toVector.flatMap(round =>
      Region.all.filter(region => round < quota(region)))
    pile.zip(seats).map((card, region) => Move(Piece.Card(card), gathering,
      PositionedLocation(Location.RegionalDiscard(region), StackPosition.Top),
      by = Some(actor)))
```

The quotas sum to the pile's size (3 × share + extra), so `seats` has exactly one seat per card.

In `WhenPlayedPowers.scala`, add `AutumnWind` after `Pilgrimage` in `all`:

```scala
      LongLostHeir, Bewitch, AncientPact, RoyalAmbitions,
      Pilgrimage, AutumnWind)
```

In each of the four Campaign suites, import `AutumnWindCard` from `oathdigital.gameplay.powers.whenplayed` instead of `oathdigital.catalog.holding`. Leave `BrackenCard`'s import as slice 9 left it. For example, if slice 9 left `BattleAxesSuite.scala` with:

```scala
import oathdigital.catalog.holding.AutumnWindCard
import oathdigital.gameplay.powers.search.BrackenCard
```

it becomes:

```scala
import oathdigital.gameplay.powers.search.BrackenCard
import oathdigital.gameplay.powers.whenplayed.AutumnWindCard
```

Keep each file's imports in the order its neighbours use. `WalledGardenSuite.scala` imports a third name from `oathdigital.catalog.holding`; keep that one there.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.* oathdigital.gameplay.powers.campaign.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected, and nothing else:
- `unimplemented.txt` loses `denizen.autumn-wind`.
- `declared-keys.txt` gains `denizen.autumn-wind dealt = Shuffled the discard piles together and dealt {0} to the Cradle, {1} to the Provinces and {2} to the Hinterland.`

- [ ] **Step 6: Update the spec and the roadmap**

In the spec:
- Change the status line to "**Status:** designed 2026-11-03. Slices 1 to 10 landed."
- Replace the body of "Slice 10: Shuffle every discard pile", above "Tests:", with:

```markdown
The gathering pile is renamed `CardGathering` wherever it is named:
`Location`, `ShufflePile`, `CardContainer` and the state field
`cardGathering`. A play-time power now uses it, and it holds cards gathered
for a shuffle, Visions included, so `DenizenGathering` would misname it. The
wire kind becomes `card-gathering`; old journals need not decode.

**Autumn Wind.** WHEN PLAYED, with no new primitive. When the cards across
the three piles do not divide by 3, a `ChooseMany` of the three discard
piles asks for exactly the remainder's count of piles to take the extra
cards; otherwise nothing is asked. Then every discard pile, Visions
included, moves to the gathering pile, which is shuffled (`WalkerShuffles`
records the order), and a `BuildOps` deals it from its head, round-robin in
region order: each round gives one card to every pile still short of its
share. The question is asked before the shuffle, which `HiddenOutcomes`
hides. Nothing parks between the gathering and the deal, so no viewer sees
the gathering pile. A searched Autumn Wind also deals the Search's own
discards, which the Search makes before the play. Line: "Shuffled the
discard piles together and dealt 4 to the Cradle, 4 to the Provinces and 3
to the Hinterland." It names the answer, so the question posts no "Chose"
line.
```

- Keep the "Tests:" paragraph, and add "a searched Autumn Wind" before "replay": "Tests: even and uneven totals; the answer honoured; empty piles; the Chronicle's gathering unchanged after the rename; a searched Autumn Wind; replay."

In `docs/ROADMAP.md`, in the "**Unchanged from batch 8:**" bullet, delete "Autumn Wind," and the space after it. Re-wrap the bullet to 80 columns as the file does. Leave the rest of the bullet alone: its other cards are later slices' work.

- [ ] **Step 7: Run the gates**

Run, in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes; statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/whenplayed/AutumnWind.scala \
  src/main/scala/oathdigital/gameplay/powers/whenplayed/WhenPlayedPowers.scala \
  src/test/scala/oathdigital/gameplay/powers/whenplayed/AutumnWindSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/campaign/BattleAxesSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/campaign/NatureWorshipSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/campaign/WalledGardenSuite.scala \
  src/test/scala/oathdigital/gameplay/powers/campaign/WildMountsSuite.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
```

If `HoldingBeastDenizens.scala` still holds a card, also `git add src/main/scala/oathdigital/catalog/holding/HoldingBeastDenizens.scala`; if Step 3 deleted it, `git rm` already staged the deletion. Then:

```bash
git commit -m "$(cat <<'EOF'
feat(powers): implement Autumn Wind

Every discard pile is shuffled together and dealt back out as evenly as
possible, the user naming the piles that take any extra cards before
the shuffle.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 1 and Task 2 diffs against this plan and the spec.
