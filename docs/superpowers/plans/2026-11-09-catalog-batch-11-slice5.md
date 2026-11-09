# Catalog Batch 11, Slice 5: Vow of Kinship Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Vow of Kinship (card 152): "Move all of your favor to
the Nomad bank. Any favor you gain or take is put in the Nomad bank. You
can use favor in the Nomad bank as if it is on your board."

**Architecture:** Five pieces, then the card.

1. **Credit.** The executor sends a favor move into the holder's board to
   the Nomad bank instead. One new operation, `DrawNomadFavor`, is exempt.
2. **Spends.** A spend is a favor move out of a player's board whose mover
   (`Move.by`) is that player. Before a walker batch runs, `KinshipDraws`
   puts a `DrawNomadFavor` in front of the first spend the holder's board
   cannot cover. The draw moves the favor from the bank onto the board,
   and the spend then runs unchanged. A take or a forced burn has another
   mover or none, so it reads the board only.
3. **Affordability.** `SpendableFavor.of` (the board plus the holder's
   Nomad bank) replaces the direct `board.favor` checks of spends.
4. **The source question.** When both the board and the bank hold favor,
   the Vow asks through slice 4's batch-question seam how much comes from
   the bank. The seam gains the batch's operations and asks about plain
   leaves too.
5. **Log.** `NoteSupport.favor` counts the holder's Nomad bank, and a gain
   line about a holder carries `NoteArg.ToNomadBank`, which the Game Log
   renders as ", to the Nomad bank".

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read rulings 9, 10, 12 and 26 to 29, the Vow of Kinship bullets of
"Readings", and "Slice 5: Favor redirection".

## Prerequisite

This branch (`batch-11-slice-5`) was cut from `batch-11-slice-4` at its
plan commit `1cd65b43`. Slice 5 uses slice 4's code: the batch-question
seam (`AsksBeforeBatch`, `BatchQuestions`) and Wild Pastures.

Before Task 1:

1. Execute slice 4 and merge it into `0.2.0-alpha`.
2. Rebase this branch onto `0.2.0-alpha`.

Git drops this branch's copy of the slice 4 plan commit, because the
merged slice carries the same patch. Task 5 and Task 8 edit slice 4's
code as this plan's author read it in the slice 4 plan. If slice 4 landed
with other names or shapes, keep slice 4's code and make this plan's
change in the same place.

## Plan-time findings

Inventoried at `1cd65b43` (slice 3 merged; slice 4 planned, not built).

1. **The holder.** "Holds the Vow" is
   `PowerAccess.holdsFaceupAdviser(ready, player, VowOfKinshipCard.id)`.
   It reads `advisersOf`, so for the walk's actor during a Master of
   Disguise Trade the chosen player's advisers decide (FAQ: Master of
   Disguise gets around Vow of Kinship). The state carries
   `walkerAsIfAdvisers`, so the executor and replay see the same holder.
2. **The executor applies one operation at a time.** `OperationPipeline`
   stages a batch per operation (`OperationPipeline.scala:82-127`), so a
   draw placed before a spend has run when the spend's guard reads the
   board.
3. **Why the draw is a pass and not an executor rule.** A cost's burn that
   Vow of Division takes becomes, in `BurnTakes.replacement`, a `Take`
   from the payer's board by the taker. An executor rule that drew on the
   bank only for the board owner's own moves would then read the holder's
   board only. `KinshipDraws` runs before `BurnTakes`, while the batch
   still holds the `PayCost`.
4. **Which favor moves carry their spender today.**
   - `Give`: its move carries `by = Some(giver)` (`CoreOperations.scala:287-296`).
   - `Take`: its move carries the taker, never the board's owner.
   - `PayCost`: its moves carry none (`OperationChildren.fromPlayArea`).
     Task 3 adds the payer to its favor moves.
   - `Burn`: its move carries none. Revelation's burn is the only chosen
     burn from a board (`Revelation.scala:76`). Task 3 makes it a
     `PayCost` burn, as `RulerToll.payment` already pays a bandit toll. The
     Campaign's defender burn (`CampaignRaid.scala:123-126`) and Defame
     (`Defame.scala:83`) stay burns, so they read the board only.
   - The Challenge's payment (`ChallengeProcedure.scala:222-226`) and the
     banner action's placement (`PlaceBannerResourceProcedure.scala:73-76`)
     are plain moves with no mover. Task 3 adds the actor (ruling 26).
   - Whispering Leaves' forced placement is a `Give` by the chosen player
     (`WhisperingLeaves.scala:71-74`). Ruling 28 reads the board only, so
     Task 3 makes it a plain move with no mover.
5. **A cost in a tree is walked as its child moves.** `walkComposite`
   (`ProcedureWalker.scala:282-300`) walks a `PayCost` or `Give` node as its
   child `Move`s, each a plain leaf recorded by `WalkerSteps.record`, which
   reaches `recordBatch`. Slice 4's seam asks only about `BuildOps` leaves,
   so Task 5 makes it ask about a plain leaf too, as a batch of one.
6. **Every walker batch goes through `WalkerSteps.recordBatch`**
   (`WalkerSteps.scala:56-81`), which runs `BurnTakes.replace` first.
   `KinshipDraws.before` goes in front of it. The other pipeline callers
   are `ModifierOffers.paying`'s dry run of selection payments (which gets
   the pass too), `ExecutedTriggers.held` (a `Defer`),
   `AdviserArrivalPass` follow-ups and `StateBasedEvaluation`, which pay
   nothing.
7. **The board-favor reads.** `grep -rn 'board\.favor' src/main/scala`
   lists 36. They sort as follows.
   - **Spends, which Task 4 moves to `SpendableFavor`:**
     - `Costs.plan` (`PowerOperations.scala:28-29`);
     - `PricedOptions.needs` (`:150-156`), with its callers
       `PricedOptions.shortfall` and `QueuedStart` (`:120`);
     - `ModifierChoice.shortfall` (`:149`);
     - `RerollChoice` (`:189-190`);
     - `PlanPrice` (`:72-73`);
     - `PlanAid.spare` and `couldCover` (`:148`, `:177`);
     - `PlanAidDesk.fits` (`:76`);
     - `PlanAidProjector` (`:48`);
     - `NegotiationDeal` (`:95`, `:150-151`);
     - `SelfExile` (`:40`);
     - `TollRoads.canPay` (`:173`);
     - `RulerToll.payOption` (`:76`);
     - `PeaceEnvoy.favor` (`:166`);
     - `WitchsBargain`'s give option (`:120`);
     - `Revelation`'s amount (`:62`, read through `NoteSupport.board`);
     - `BannerRules.playerResources` for the People's Favor (`:26`),
       which the Challenge and the banner action read (ruling 26).
   - **Done to the player, which stay on the board:**
     - Royal Tax (`:65`), Chaos Cult (`:59`), Charming Friend (`:81`),
       Naysayers (`:47`), Pied Piper (`:116`);
     - Witch's Bargain's take (`:149-150`), Defame (`:58`), the Campaign's
       defender burn (`CampaignRaid.scala:123-124`);
     - Vow of Poverty (`:64`, ruling 27) and Whispering Leaves (`:71`,
       ruling 28).
   - **Display, which stays:** `GameProjection` (`:107`) and
     `GamePresentationProjector` (`:170`) show the board. The Nomad bank
     shows on the map.
   - **Mechanics, which stay:** `OperationStateAdapter.favor` (`:94`),
     `ResourceOperations.adjustFavor`, and `PricedOptions.lift` (`:169`),
     which only adds favor for a preview.
8. **The log.** `NoteSupport.favor` reads the board change
   (`NoteSupport.scala:18`). Under the Vow, a gain moves no board favor, so
   the gain lines would go silent, and a spend from the bank would read as
   nothing paid. Task 7 counts the holder's Nomad bank. Lines render
   server-side (`application/gamelog/NoteLines.scala:57-62`), so one new
   argument, `NoteArg.ToNomadBank`, gives every marked line its ending.
   These lines write a favor gain and are marked:
   - through a `NoteSupport` helper: `gainedNote` and `gainNote` (Town
     Meeting, Alchemist), `gainedFromNote` (Book Binders, Fabled Feast,
     Saddle Makers, Gossip, Herald) and `tookNote` (Spirit Snare, Vow of
     Obedience, Silver Tongue);
   - by their own code: Blackmail, Chaos Cult, Vow of Poverty, Secret
     Signal, Moving Market, Book of Records, Gambling Hall, Pied Piper,
     Sticky Fire, Bog, Taming Charm, Witch's Bargain (both lines), Charming
     Friend, Peace Envoy, Royal Tax, Trade and Take Wealth.
9. **The card moves.** `VowOfKinshipCard` moves from
   `catalog/holding/HoldingNomadDenizens.scala:10-17` to a new
   `gameplay/VowOfKinship.scala`, as `GrandMaskCard` sits in
   `gameplay/GrandMask.scala`, so that `gameplay.operations` and
   `gameplay.actions` can read the rule. `NewFoundations.scala:5-6` imports
   it by name. Four test suites import it from `catalog.holding` as a plain
   Nomad card: `RuledCardsSuite`, `PledgeOfDefenseSuite`,
   `ImperialTacticsSuite` and `GreatCrusadeSuite`.
10. **One question per batch.** The Vow asks about the batch's first
    spend, for the whole batch's spend by that holder, so the seam's
    `BatchContext` gains `operations`, the batch as built. A
    `DrawNomadFavor` the answer adds counts toward the board, and the pass
    draws only what is still short.

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-5`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import or a
  non-exhaustive match fails it.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Test helper names say what they build.
- Log lines name only non-actors.
- Old journals need not decode (wire changes may require new fields).
- A predicate over a batch flattens each operation (`Operation.flatten`),
  since a `Take`'s leaving nests operations.

---

### Task 1: The draw and the Nomad-bank ending, on the wire and in the log

**Files:**
- Modify: `src/main/scala/oathdigital/model/CoreOperations.scala` (after `Give`, about line 297)
- Modify: `src/main/scala/oathdigital/model/LogNotes.scala` (`NoteArg`, about lines 13-50)
- Modify: `src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala` (`PieceOperation`, encode, decode)
- Modify: `src/main/scala/oathdigital/serialization/WalkerEventCodec.scala` (`encodeNoteArg`, the note-arg decoder)
- Modify: `src/main/scala/oathdigital/application/gamelog/NoteLines.scala` (`spoken`, `argument`)
- Modify: `src/test/scala/oathdigital/gameplay/powers/NoteText.scala` (`sentence`, `plain`)
- Test: `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`
- Test: `src/test/scala/oathdigital/serialization/LogNoteWireSuite.scala`
- Test: `src/test/scala/oathdigital/application/gamelog/ProcedureNoteLinesSuite.scala`

**Interfaces:**
- Produces, in `oathdigital.model`:
  - `final case class DrawNomadFavor(player: PlayerId, amount: Int) extends CoreOperation`, required, whose one child is `Move(Piece.Favor(amount), FavorBank(Nomad), PlayArea(player))` with no mover;
  - `case object NoteArg.ToNomadBank extends NoteArg`.
- Wire kinds: operation `"draw-nomad-favor"` (`playerId`, `amount`); note
  argument `"to-nomad-bank"`.

- [ ] **Step 1: Write the failing tests**

In `WalkerOperationWireSuite.scala`, after the test "an as-if adviser
holder round trips", add:

```scala
  test("a draw from the Nomad bank round trips"):
    val event = recordedOperation(DrawNomadFavor(PlayerId("p1"), 2))
    val encoded = accepted(GameEventWire.encodeEvent("walker", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
    assertEquals(ujson.read(encoded)("payload")("step")("ops")(0)("kind").str,
      "draw-nomad-favor")
```

In `LogNoteWireSuite.scala`, after the test "a log note's card list and
banner arguments round trip", add:

```scala
  test("a log note's Nomad bank ending round trips"):
    val event = noteEvent(PowerSourceRef.Card(DenizenId("denizen")),
      NoteArg.Number(2), NoteArg.ToNomadBank)
    val encoded = accepted(GameEventWire.encodeEvent("notes", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
```

In `ProcedureNoteLinesSuite.scala`, after the test "a Trade for favor
reads as the rule wrote it", add:

```scala
  test("a Trade's favor that went to the Nomad bank says so"):
    assertEquals(said(ActionRef.Trade, TradeProcedure.tradedForFavor,
      NoteArg.Card(roar), NoteArg.Number(3), NoteArg.ToNomadBank),
      "Traded with a Denizen for 3 favor, to the Nomad bank")

  test("a sentence that ends in a period takes the ending before it"):
    assertEquals(NoteLines.toNomadBank(Vector[LogSpan](Text("Gained "),
        Text("2"), Text(" favor."))),
      Vector[LogSpan](Text("Gained "), Text("2"),
        Text(" favor, to the Nomad bank.")))
```

Procedure lines carry no final period, and power lines do, so the second
case is tested on the spans directly.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.serialization.WalkerOperationWireSuite oathdigital.serialization.LogNoteWireSuite oathdigital.application.gamelog.ProcedureNoteLinesSuite"`
Expected: compile errors: `DrawNomadFavor` and `ToNomadBank` are not found.

- [ ] **Step 3: Add the operation**

In `CoreOperations.scala`, after the `Give` case class, add:

```scala
/** Moves `amount` favor from the Nomad bank onto `player`'s board, for the
  * spend that follows it in the same batch (Vow of Kinship, catalog batch
  * 11, slice 5). The favor lands on the board even while `player` holds
  * the Vow, which sends every other favor bound for their board to the
  * Nomad bank. Nobody moves it, so no restriction against a player holds.
  */
final case class DrawNomadFavor(player: PlayerId, amount: Int)
    extends CoreOperation:
  require(amount > 0, "a draw moves favor")
  override val required: Boolean = true
  val move: Move = Move(Piece.Favor(amount),
    PositionedLocation(Location.FavorBank(Suit.Nomad)),
    PositionedLocation(Location.PlayArea(player)))
  override val children: Vector[Operation] = Vector(move)
```

`CoreOperation` is sealed, so `-Werror` names every exhaustive match that
now lacks the case. Treat `DrawNomadFavor` as `Give` is treated at each
one: a composite of one counted move. The codec is one of them (Step 5).

- [ ] **Step 4: Add the note argument**

In `LogNotes.scala`, inside `object NoteArg`, after `Things`, add:

```scala
  /** The line's favor went to the Nomad bank, as Vow of Kinship sends it
    * (catalog batch 11, ruling 29). The Game Log ends the sentence ", to
    * the Nomad bank". It is never a template's `Arg`. */
  case object ToNomadBank extends NoteArg
```

- [ ] **Step 5: Encode both**

In `WalkerOperationCodec.scala`:

1. Add `DrawNomadFavor` to the `PieceOperation` union:
   `Move | Take | Kill | Replace | Sacrifice | Burn | Give | Exchange | PayCost | DrawNomadFavor`.
2. In `encodeOperation`, after the `case give: Give` line, add:

   ```scala
      case DrawNomadFavor(player, amount) => ujson.Obj(
        "kind" -> "draw-nomad-favor", "playerId" -> player.value,
        "amount" -> amount)
   ```

3. In the decoder, after `case "spend-supply" => ...`, add:

   ```scala
    case "draw-nomad-favor" =>
      decodePositiveInt(value("amount"), s"$path.amount")
        .map(amount => DrawNomadFavor(PlayerId(value("playerId").str), amount))
   ```

In `WalkerEventCodec.scala`:

1. In `encodeNoteArg`, after the `NoteArg.Things` case, add
   `case NoteArg.ToNomadBank => ujson.Obj("kind" -> "to-nomad-bank")`.
2. In the note-arg decoder, after `case "things" => ...`, add
   `case "to-nomad-bank" => Right(NoteArg.ToNomadBank)`.

- [ ] **Step 6: Render the ending**

In `NoteLines.scala`, in `spoken`, end the sentence when the note carries
the argument:

```scala
  def spoken(power: PowerId, note: LogNote, seen: Vector[ReadyGame],
      viewer: Option[PlayerId]): Option[Vector[LogSpan]] =
    wordings.template(power, note.key).map(template =>
      prefix(power, note, seen, viewer) ++
        ended(note, sentence(template, note.args, seen, viewer)))

  /** `spans`, ended for the Nomad bank when the note's favor went there
    * (catalog batch 11, ruling 29). */
  private def ended(note: LogNote, spans: Vector[LogSpan]): Vector[LogSpan] =
    if note.args.contains(NoteArg.ToNomadBank) then NoteLines.toNomadBank(spans)
    else spans
```

and add a companion object after the class:

```scala
private[gamelog] object NoteLines:
  /** `spans` ending ", to the Nomad bank", before the final period if the
    * sentence has one. */
  def toNomadBank(spans: Vector[LogSpan]): Vector[LogSpan] =
    spans.lastOption match
      case Some(Text(last)) if last.endsWith(".") =>
        spans.init :+ Text(last.dropRight(1) + ", to the Nomad bank.")
      case _ => spans :+ Text(", to the Nomad bank")
```

In `argument`, add `case NoteArg.ToNomadBank => Vector.empty`.

If `LogSpan.Text` is not a case class whose pattern `Text(last)` binds the
string, match it as the other code in this file does.

In `NoteText.scala`, mirror it: in `sentence`, after building the string,
apply the same ending when `note.args.contains(NoteArg.ToNomadBank)` (a
final "." becomes ", to the Nomad bank."; otherwise append ", to the
Nomad bank"). In `plain`, add `case NoteArg.ToNomadBank => ""`.

`NoteArg` is sealed, so `-Werror` names any other exhaustive match that
lacks the case. Give it the empty reading there too.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.serialization.* oathdigital.application.gamelog.ProcedureNoteLinesSuite"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/model/CoreOperations.scala src/main/scala/oathdigital/model/LogNotes.scala src/main/scala/oathdigital/serialization/WalkerOperationCodec.scala src/main/scala/oathdigital/serialization/WalkerEventCodec.scala src/main/scala/oathdigital/application/gamelog/NoteLines.scala src/test/scala/oathdigital/gameplay/powers/NoteText.scala src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala src/test/scala/oathdigital/serialization/LogNoteWireSuite.scala src/test/scala/oathdigital/application/gamelog/ProcedureNoteLinesSuite.scala
git commit -m "$(cat <<'EOF'
feat(model): a draw from the Nomad bank, and a line ending that says favor went there

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add any other file that `-Werror` made you touch in Steps 3 and 6.

---

### Task 2: The holder, their spendable favor, and favor redirected to the bank

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/VowOfKinship.scala`
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingNomadDenizens.scala:10-17` (delete the card)
- Modify: `src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala:5-6` (import)
- Modify: `src/main/scala/oathdigital/gameplay/operations/ResourceOperations.scala` (`applyCountedMoves`, lines 152-194)
- Modify: `src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala` (`applyFamilies`, about line 79)
- Modify: the four suites of finding 9 (imports)
- Test: `src/test/scala/oathdigital/gameplay/VowOfKinshipCreditSuite.scala` (create)

**Interfaces:**
- Consumes: `DrawNomadFavor` (Task 1).
- Produces, in `oathdigital.gameplay`:
  - `object VowOfKinshipCard` (moved; same id, name, suit, traits and power);
  - `private[gameplay] object KinshipRule` with
    `holds(ready: ReadyGame, player: PlayerId): Boolean`,
    `creditAt(ready: ReadyGame, to: Location): Location` and
    `bank(ready: ReadyGame): Int`;
  - `object SpendableFavor` with `of(ready: ReadyGame, player: PlayerId): Int`.
- Produces, in `ResourceOperations`:
  `applyCountedMoves(ready, leaves, redirect: Boolean = true)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/oathdigital/gameplay/VowOfKinshipCreditSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.operations.{OperationPipeline, OperationPolicy}
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Table.{p1, p2}

/** Vow of Kinship's credit (catalog batch 11, slice 5, ruling 9): favor
  * bound for its holder's board lands in the Nomad bank, however it was
  * built, and a draw from the bank lands on the board. */
class VowOfKinshipCreditSuite extends munit.FunSuite:
  /** p1 holds the Vow faceup with 1 favor on their board. The Nomad bank
    * holds 2 favor, the Order bank 3, and p2's board 3. */
  private def vowHolderWithOneFavor(facedown: Boolean = false): Table =
    Table.start.adviser(p1, VowOfKinshipCard, facedown = facedown)
      .favor(p1, 1).favor(p2, 3)
      .bankFavor(Suit.Nomad, 2).bankFavor(Suit.Order, 3)

  /** The state after `ops` run as one batch. */
  private def after(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): Look =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).fold(violation => fail(violation.toString),
      run => Look(run.state))

  test("favor the holder gains from a bank goes to the Nomad bank"):
    val look = after(vowHolderWithOneFavor().ready,
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Order), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 4)

  test("favor the holder takes, or is given, goes to the Nomad bank"):
    val look = after(vowHolderWithOneFavor().ready,
      Take(Piece.Favor(1), p1, Location.PlayArea(p2), Location.PlayArea(p1)),
      Give(Piece.Favor(1), p2, Location.PlayArea(p2), Location.PlayArea(p1)))
    assertEquals(look.favor(p2), 1)
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 4)

  test("a facedown Vow sends nothing to the bank, nor does another " +
      "player's gain"):
    val facedown = after(vowHolderWithOneFavor(facedown = true).ready,
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(facedown.favor(p1), 3)
    val other = after(vowHolderWithOneFavor().ready,
      Gain.Favor(p2, Suit.Order, 2))
    assertEquals(other.favor(p2), 5)
    assertEquals(other.bankFavor(Suit.Nomad), 2)

  test("a draw from the Nomad bank lands on the holder's board"):
    val look = after(vowHolderWithOneFavor().ready, DrawNomadFavor(p1, 2))
    assertEquals(look.favor(p1), 3)
    assertEquals(look.bankFavor(Suit.Nomad), 0)

  test("acting with another player's advisers lifts the Vow, and acting " +
      "with a holder's lends it"):
    // p1 is the walk's actor on Table.start, so the as-if advisers are
    // theirs.
    def actingWith(ready: ReadyGame, player: PlayerId): ReadyGame =
      ready.updateCurrent(_.copy(walkerAsIfAdvisers = Some(player)))
    val lifted = after(actingWith(vowHolderWithOneFavor().ready, p2),
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(lifted.favor(p1), 3)
    val lent = after(actingWith(Table.start.adviser(p2, VowOfKinshipCard)
      .favor(p1, 0).bankFavor(Suit.Nomad, 0).bankFavor(Suit.Order, 3).ready,
      p2), Gain.Favor(p1, Suit.Order, 2))
    assertEquals(lent.favor(p1), 0)
    assertEquals(lent.bankFavor(Suit.Nomad), 2)

  test("a holder's spendable favor counts the Nomad bank; another " +
      "player's does not"):
    val ready = vowHolderWithOneFavor().ready
    assertEquals(SpendableFavor.of(ready, p1), 3)
    assertEquals(SpendableFavor.of(ready, p2), 3)
    assertEquals(SpendableFavor.of(
      vowHolderWithOneFavor(facedown = true).ready, p1), 1)
```

If `Table.start`'s Nomad bank does not start at 0, `bankFavor` sets it,
so the numbers hold. If `ReadyGame` has no `updateCurrent`, use the helper
`PricedOptions.lift` uses (`state.updateCurrent(current => ...)`).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.VowOfKinshipCreditSuite"`
Expected: compile errors: `VowOfKinshipCard` (in `oathdigital.gameplay`)
and `SpendableFavor` are not found.

- [ ] **Step 3: Move the card and write the rule**

Delete `VowOfKinshipCard` from `HoldingNomadDenizens.scala` (lines
10-17, with the blank line after it).

Create `src/main/scala/oathdigital/gameplay/VowOfKinship.scala`:

```scala
package oathdigital.gameplay

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.model._

object VowOfKinshipCard extends Denizen(DenizenId("152"), "Vow of Kinship", Suit.Nomad) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.vow-of-kinship"),
    persistent = false, cost = Cost.free,
    text = "Move all of your [favor] to the [suit-nomad] bank. Any " +
      "[favor] you gain or take is put in the [suit-nomad] bank. " +
      "You can use [favor] in the [suit-nomad] bank as if it is on " +
      "your board.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Vow of Kinship (card 152, catalog batch 11, rulings 9, 10, 12 and 26 to
  * 29). Its holder is the player who acts with it faceup among their
  * advisers (`PowerAccess.holdsFaceupAdviser`), so Master of Disguise lifts
  * it, or lends it, for one Trade.
  *
  * Favor bound for the holder's board lands in the Nomad bank
  * (`creditAt`, applied by the executor). Favor the holder spends may come
  * from the bank: `KinshipDraws` draws it onto the board before the spend.
  * Anything done to the holder reads their board only.
  */
private[gameplay] object KinshipRule:
  def holds(ready: ReadyGame, player: PlayerId): Boolean =
    PowerAccess.holdsFaceupAdviser(ready, player, VowOfKinshipCard.id)

  /** Where favor moved to `to` lands: the Nomad bank instead of a holder's
    * board. */
  def creditAt(ready: ReadyGame, to: Location): Location = to match
    case Location.PlayArea(player) if holds(ready, player) =>
      Location.FavorBank(Suit.Nomad)
    case other => other

  /** The favor in the Nomad bank. */
  def bank(ready: ReadyGame): Int =
    ready.banks.favor.getOrElse(Suit.Nomad, 0)

/** The favor `player` may spend: their board's, and for Vow of Kinship's
  * holder the Nomad bank's too (ruling 9). A check of a spend reads this;
  * a check of something done to the player reads the board. */
object SpendableFavor:
  def of(ready: ReadyGame, player: PlayerId): Int =
    val board = ready.game.current.player(player).fold(0)(_.board.favor)
    if KinshipRule.holds(ready, player) then board + KinshipRule.bank(ready)
    else board
```

In `NewFoundations.scala`, add `VowOfKinshipCard` to the
`oathdigital.gameplay.{...}` import at lines 5-6.

In `RuledCardsSuite`, `PledgeOfDefenseSuite`, `ImperialTacticsSuite` and
`GreatCrusadeSuite`, import `oathdigital.gameplay.VowOfKinshipCard`
instead of `oathdigital.catalog.holding.VowOfKinshipCard` (in
`GreatCrusadeSuite`, keep `TheGatheringCard` from `catalog.holding`).

- [ ] **Step 4: Redirect the credit in the executor**

In `ResourceOperations.applyCountedMoves`, add the parameter and read the
destination through the rule:

```scala
  def applyCountedMoves(
      ready: ReadyGame,
      leaves: Vector[Operation],
      redirect: Boolean = true
  ): Either[OperationError, ReadyGame] =
```

and replace the `withFavor` fold with:

```scala
      // Vow of Kinship: favor bound for its holder's board lands in the
      // Nomad bank, judged before the operation runs. A draw from the bank
      // (`redirect = false`) lands on the board.
      withFavor <- favorMoves.foldLeft[Either[OperationError, ReadyGame]](
        Right(withoutFavor)) { (result, move) =>
        val piece = move.piece.asInstanceOf[Piece.Favor]
        val to =
          if redirect then KinshipRule.creditAt(ready, move.to.location)
          else move.to.location
        result.flatMap(adjustFavor(_, to, piece.amount))
      }
```

Import `oathdigital.gameplay.KinshipRule`.

In `OperationApplication.applyFamilies`, replace
`resources <- ResourceOperations.applyCountedMoves(ready, leaves)` with:

```scala
      resources <- ResourceOperations.applyCountedMoves(ready, leaves,
        redirect = !operation.isInstanceOf[DrawNomadFavor])
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.VowOfKinshipCreditSuite oathdigital.gameplay.powers.RuledCardsSuite oathdigital.gameplay.powers.campaign.*"`
Expected: PASS. If one of the four suites of finding 9 now fails because
the Vow, held as an adviser, sends a gain to the bank, give that suite
`TravelingNegotiatorCard` (another adviser-only Nomad card in
`catalog.holding`) in its place.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/VowOfKinship.scala src/main/scala/oathdigital/catalog/holding/HoldingNomadDenizens.scala src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala src/main/scala/oathdigital/gameplay/operations/ResourceOperations.scala src/main/scala/oathdigital/gameplay/operations/OperationApplication.scala src/test/scala/oathdigital/gameplay/VowOfKinshipCreditSuite.scala src/test/scala/oathdigital/gameplay/powers/RuledCardsSuite.scala src/test/scala/oathdigital/gameplay/powers/campaign/PledgeOfDefenseSuite.scala src/test/scala/oathdigital/gameplay/powers/campaign/ImperialTacticsSuite.scala src/test/scala/oathdigital/gameplay/powers/campaign/GreatCrusadeSuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): favor bound for a Vow of Kinship holder goes to the Nomad bank

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: A holder's spends draw on the Nomad bank

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/KinshipDraws.scala`
- Modify: `src/main/scala/oathdigital/model/OperationChildren.scala:48-57` (`fromPlayArea`, `positiveMove`)
- Modify: `src/main/scala/oathdigital/model/CoreOperations.scala` (`PayCost.favorMove`, about line 334)
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala:222-226` (`payment`)
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/PlaceBannerResourceProcedure.scala:73-76` (`placement`)
- Modify: `src/main/scala/oathdigital/gameplay/powers/whenplayed/Revelation.scala:60-80` (`ask`, `settle`)
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/WhisperingLeaves.scala:71-74` (`give`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerSteps.scala:61` (`recordBatch`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/ModifierOffers.scala:44` (`paying`)
- Test: `src/test/scala/oathdigital/gameplay/KinshipDrawsSuite.scala` (create)
- Test: `src/test/scala/oathdigital/gameplay/powers/whenplayed/RevelationSuite.scala`
- Test: `src/test/scala/oathdigital/gameplay/powers/whenplayed/RoyalTaxSuite.scala`
- Test: `src/test/scala/oathdigital/gameplay/powers/action/WhisperingLeavesSuite.scala`

**Interfaces:**
- Consumes: `DrawNomadFavor` (Task 1); `KinshipRule`, `SpendableFavor`,
  `VowOfKinshipCard` (Task 2).
- Produces, in `oathdigital.gameplay`:
  - `object KinshipDraws` with
    `before(ready: ReadyGame, operations: Vector[CoreOperation]): Vector[CoreOperation]`
    and `spent(operation: CoreOperation, player: PlayerId): Int`.
- Produces: `OperationChildren.fromPlayArea(player, amount, piece, to, by: Option[PlayerId] = None)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/oathdigital/gameplay/KinshipDrawsSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.powers.burn.{VowOfDivision, VowOfDivisionCard}
import oathdigital.gameplay.walker.{ProcedureWalker, WalkerPowers}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}

/** Vow of Kinship's draw (catalog batch 11, slice 5, ruling 9): before a
  * batch runs, a holder's spend that their board cannot cover draws the
  * rest from the Nomad bank, and nothing done to them draws at all. */
class KinshipDrawsSuite extends munit.FunSuite:
  /** p1 holds the Vow with `board` favor on their board and `bank` in the
    * Nomad bank; p2's board holds 3. It is p1's turn. */
  private def vowHolder(board: Int, bank: Int): Table =
    Table.start.adviser(p1, VowOfKinshipCard).favor(p1, board)
      .favor(p2, 3).bankFavor(Suit.Nomad, bank)

  private def burning(player: PlayerId, n: Int): PayCost =
    PayCost(player, Location.SharedBank, Cost(favorBurnt = n))

  private def giving(n: Int): Give = Give(Piece.Favor(n), p1,
    Location.PlayArea(p1), Location.PlayArea(p2), required = true)

  test("a cost the board cannot cover draws the rest first"):
    val ready = vowHolder(board = 1, bank = 4).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](DrawNomadFavor(p1, 2), burning(p1, 3)))

  test("a board that covers the spend, or a player without the Vow, " +
      "draws nothing"):
    val ready = vowHolder(board = 3, bank = 4).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](burning(p1, 3)))
    assertEquals(KinshipDraws.before(ready, Vector(burning(p2, 3))),
      Vector[CoreOperation](burning(p2, 3)))

  test("a take from the holder and a burn of their board draw nothing"):
    val ready = vowHolder(board = 0, bank = 4).ready
    val done: Vector[CoreOperation] = Vector(
      Take(Piece.Favor(2), p2, Location.PlayArea(p1), Location.PlayArea(p2)),
      Burn.favor(2, PositionedLocation(Location.PlayArea(p1)), Some(p1)))
    assertEquals(KinshipDraws.before(ready, done), done)

  test("one draw covers the later spends, and a draw already in the " +
      "batch counts"):
    val ready = vowHolder(board = 1, bank = 5).ready
    assertEquals(KinshipDraws.before(ready, Vector(giving(1), giving(2))),
      Vector[CoreOperation](giving(1), DrawNomadFavor(p1, 2), giving(2)))
    val answered: Vector[CoreOperation] =
      Vector(DrawNomadFavor(p1, 3), burning(p1, 3))
    assertEquals(KinshipDraws.before(ready, answered), answered)

  test("a draw is capped by the bank"):
    val ready = vowHolder(board = 0, bank = 1).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](DrawNomadFavor(p1, 1), burning(p1, 3)))

  test("a walked cost and a toll are paid out of the Nomad bank"):
    val ready = vowHolder(board = 0, bank = 4).ready
    val done = Outcomes.finished(ProcedureWalker.advance(ready,
      BuildOps((_, _) => Right(Vector[CoreOperation](burning(p1, 1),
        giving(2)))), None, WalkerPowers(Vector.empty)))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 0)
    assertEquals(look.bankFavor(Suit.Nomad), 1)
    assertEquals(look.favor(p2), 5)

  test("a cost's burn that Vow of Division takes comes out of the Nomad " +
      "bank"):
    val ready = vowHolder(board = 0, bank = 4)
      .adviser(p2, VowOfDivisionCard).ready
    val done = Outcomes.finished(ProcedureWalker.advance(ready,
      BuildOps((_, _) => Right(Vector[CoreOperation](burning(p1, 2)))),
      None, WalkerPowers(Vector(VowOfDivision))))
    val look = Look(done.treeless)
    assertEquals(look.bankFavor(Suit.Nomad), 2)
    assertEquals(look.favor(p2), 5)
```

If `ProcedureWalker.advance` needs the walker fixture's game rather than
`Table.start`'s, build the table with `Table.from(ProcedureWalkerFixture.ready)`
and its `actor` in place of `p1`. If `Outcomes.finished(...).treeless` is
named otherwise, use what `ExecutedTriggersSuite` reads (`done.treeless`).

In `RevelationSuite.scala`, add:

```scala
  test("a Vow of Kinship holder may burn the Nomad bank's favor"):
    // p1 holds the Vow, with no favor on their board and 3 in the Nomad
    // bank; p2 and p3 hold none.
    val ready = holdingFavor(0, 0, 0).adviser(p1, VowOfKinshipCard)
      .bankFavor(Suit.Nomad, 3).ready
    val walked = walkAnswering(ready, power, played, burn(2))
    assertEquals(walked.asked.map(_.owner), Vector(p1))
    assertEquals(amountMaximaAsked(walked), Vector(3))
    val end = Look(walked.end)
    assertEquals(end.bankFavor(Suit.Nomad), 1)
    assertEquals(end.faceUpSecrets(p1), 3)
    assertJournalRebuilds(ready, walked.events, walked.end)
```

Import `oathdigital.gameplay.VowOfKinshipCard`. The test does not assert
the line yet: it reads the bank's change only once Task 7 makes
`NoteSupport.favor` count it, and Task 7 adds that assertion.

In `RoyalTaxSuite.scala`, add a test modelled on "a player with no favor
is named, and nothing is taken from them": the same table, with that
player also holding `VowOfKinshipCard` and the Nomad bank holding 4. Assert
the same outcome, and that `Look(...).bankFavor(Suit.Nomad)` is still 4.
Name it "a Vow of Kinship holder with an empty board is taxed nothing:
the tax reads their board".

In `WhisperingLeavesSuite.scala`, add a test modelled on its test of a
chosen player with no favor: the chosen player holds `VowOfKinshipCard`,
their board holds none and the Nomad bank 3. Assert nothing is placed on
the card and the bank still holds 3. Name it "a Vow of Kinship holder
places only their board's favor (ruling 28)".

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.KinshipDrawsSuite oathdigital.gameplay.powers.whenplayed.RevelationSuite oathdigital.gameplay.powers.whenplayed.RoyalTaxSuite oathdigital.gameplay.powers.action.WhisperingLeavesSuite"`
Expected: compile error: `KinshipDraws` is not found.

- [ ] **Step 3: Mark the spender on cost moves**

In `OperationChildren.scala`:

```scala
  def fromPlayArea(player: PlayerId, amount: Int, piece: Int => Piece,
      to: Location, by: Option[PlayerId] = None): Vector[Operation] =
    positiveMove(amount)(piece,
      PositionedLocation(Location.PlayArea(player)), PositionedLocation(to),
      by)

  private def positiveMove(amount: Int)(piece: Int => Piece,
      from: PositionedLocation,
      to: PositionedLocation,
      by: Option[PlayerId] = None): Vector[Operation] =
    if amount == 0 then Vector.empty
    else Vector(Move(piece(amount), from, to, by = by))
```

In `PayCost`, the favor moves carry their payer (secrets do not change):

```scala
  // The payer moves the favor, so a Vow of Kinship holder's cost may draw
  // on the Nomad bank (`KinshipDraws`).
  private def favorMove(amount: Int, to: Location): Vector[Operation] =
    OperationChildren.fromPlayArea(player, amount, Piece.Favor.apply, to,
      by = Some(player))
```

In `ChallengeProcedure.payment` and `PlaceBannerResourceProcedure.placement`,
give the `Move` `by = Some(actor)` (ruling 26: a placement the holder
makes may come from the bank).

Some tests assert the recorded moves of a walked `PayCost`, which now
carry `by = Some(payer)`. Update those expected values; change nothing
else in them.

- [ ] **Step 4: Revelation burns as a cost, and Whispering Leaves places
  without a mover**

In `Revelation.settle`, replace

```scala
        Burn.favor(amount, PositionedLocation(Location.PlayArea(player)),
          Some(player)),
```

with

```scala
        // A chosen burn is the player's spend (ruling 9), paid as a cost's
        // burn so that a Vow of Kinship holder may draw on the Nomad bank.
        PayCost(player, Location.SharedBank, Cost(favorBurnt = amount)),
```

In `Revelation.ask`, read the amount with `SpendableFavor.of(ready, player)`
instead of `NoteSupport.board(ready, player).map(_.favor)`:

```scala
  private def ask(ready: ReadyGame, player: PlayerId): Vector[Operation] =
    Some(SpendableFavor.of(ready, player)).filter(_ > 0).toVector
      .map(favor => Decide(decisionId, player, DecisionQuery.ChooseAmount(0,
        favor, Some("Revelation: burn any favor to gain as many secrets"),
        "Burn favor")))
```

Find every other read of a player's board favor in `Revelation.scala` (the
"nobody had favor" check) and read `SpendableFavor.of` there too. Import
`oathdigital.gameplay.SpendableFavor`.

In `WhisperingLeaves.give`, replace the `Give` with a move nobody makes
(ruling 28: it is done to the chosen player, so it reads their board):

```scala
      else Vector(Move(Piece.Favor(amount),
        PositionedLocation(Location.PlayArea(target)),
        PositionedLocation(Location.OnCard(leaves))))
```

- [ ] **Step 5: Write the pass**

Create `src/main/scala/oathdigital/gameplay/KinshipDraws.scala`:

```scala
package oathdigital.gameplay

import oathdigital.model._

/** Vow of Kinship's draw on the Nomad bank (catalog batch 11, slice 5,
  * ruling 9).
  *
  * A spend is a favor move out of a player's board that the player makes:
  * its mover is the board's owner. A cost's favor moves carry their payer,
  * a gift its giver, and a Challenge's or banner action's placement its
  * actor. A take, a forced burn and a forced placement carry another mover
  * or none, so they draw nothing and read the board only.
  *
  * Before a batch runs, for each holder, the pass reads the batch in order
  * against the favor on the holder's board. In front of the first
  * operation whose spend the board no longer covers, it puts a
  * `DrawNomadFavor` for what the board lacks, capped by the bank. Favor
  * bound for the holder's board lands in the bank, so the board only falls
  * within a batch, and one draw covers every later spend. A draw already in
  * the batch, the source question's answer, adds to the board.
  *
  * It runs before `BurnTakes`, which turns a cost's taken burn into a
  * `Take` by the taker and so hides its payer.
  */
object KinshipDraws:
  def before(ready: ReadyGame, operations: Vector[CoreOperation])
      : Vector[CoreOperation] =
    ready.game.current.players.map(_.player)
      .filter(KinshipRule.holds(ready, _))
      .foldLeft(operations)((batch, holder) => drawn(ready, holder, batch))

  /** The favor `operation` spends from `player`'s board. */
  def spent(operation: CoreOperation, player: PlayerId): Int =
    Operation.flatten(operation).collect {
      case Move(Piece.Favor(amount), from, _, _, Some(by))
          if by == player && from.location == Location.PlayArea(player) =>
        amount
    }.sum

  private def drawn(ready: ReadyGame, holder: PlayerId,
      operations: Vector[CoreOperation]): Vector[CoreOperation] =
    val board = ready.game.current.player(holder).fold(0)(_.board.favor)
    operations.foldLeft((Vector.empty[CoreOperation], board,
        KinshipRule.bank(ready))) {
      case ((out, held, left), draw @ DrawNomadFavor(`holder`, amount)) =>
        (out :+ draw, held + amount, left - amount)
      case ((out, held, left), operation) =>
        val need = spent(operation, holder)
        val short = math.min(math.max(0, need - held), math.max(0, left))
        val drawing = Option.when(short > 0)(DrawNomadFavor(holder, short))
        (out ++ drawing :+ operation, math.max(0, held + short - need),
          left - short)
    }._1
```

If `Operation.flatten` returns `Vector[Operation]`, the `collect` matches
`Move` there as written; if it returns leaves of another type, match the
`Move` case the same way `PricedOptions.spent` does.

- [ ] **Step 6: Run the pass before every batch**

In `WalkerSteps.recordBatch`, replace

```scala
    val swapped = BurnTakes.replace(ctx.state, ctx.powers.powers, ops)
```

with

```scala
    // A Vow of Kinship holder's spend draws on the Nomad bank first, while
    // the batch still holds its costs (`KinshipDraws`).
    val swapped = BurnTakes.replace(ctx.state, ctx.powers.powers,
      KinshipDraws.before(ctx.state, ops))
```

Add one sentence to `recordBatch`'s doc comment, before "Each burn a
power claims": "A Vow of Kinship holder's spends first draw on the Nomad
bank ([[oathdigital.gameplay.KinshipDraws]])."

In `ModifierOffers.paying`, run the same pass on the dry run:

```scala
    else OperationPipeline.run(ready, KinshipDraws.before(ready, payments),
      OperationPolicy.Permissive,
```

Import `oathdigital.gameplay.KinshipDraws` in both files.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.KinshipDrawsSuite oathdigital.gameplay.powers.whenplayed.* oathdigital.gameplay.powers.action.WhisperingLeavesSuite oathdigital.gameplay.powers.burn.* oathdigital.gameplay.walker.*"`
Expected: PASS.

Then run the whole suite once, since `PayCost`'s moves changed:
`./sbtw test`. Expected: PASS, after the expected-value updates of Step 3.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/KinshipDraws.scala src/main/scala/oathdigital/model/OperationChildren.scala src/main/scala/oathdigital/model/CoreOperations.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/oathdigital/gameplay/actions/challenge/PlaceBannerResourceProcedure.scala src/main/scala/oathdigital/gameplay/powers/whenplayed/Revelation.scala src/main/scala/oathdigital/gameplay/powers/action/WhisperingLeaves.scala src/main/scala/oathdigital/gameplay/walker/WalkerSteps.scala src/main/scala/oathdigital/gameplay/walker/ModifierOffers.scala src/test/scala/oathdigital/gameplay/KinshipDrawsSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/RevelationSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/RoyalTaxSuite.scala src/test/scala/oathdigital/gameplay/powers/action/WhisperingLeavesSuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): a Vow of Kinship holder's spends draw on the Nomad bank

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add each test file whose expected `PayCost` moves you updated in Step 3.

**Checkpoint review after Task 3:** credit and spends are in. A reviewer
checks:
- that every favor move out of a board that a player chooses carries that
  player, and that nothing done to a player does (finding 4);
- that `KinshipDraws` runs before `BurnTakes` on every walker batch and on
  the modifier dry run, and nowhere else is a spend paid outside them
  (finding 6);
- that `./sbtw test` passes.

---

### Task 4: Spend checks count the Nomad bank

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/operations/PowerOperations.scala:28-29`
- Modify: `src/main/scala/oathdigital/gameplay/walker/PricedOptions.scala:136-156`
- Modify: `src/main/scala/oathdigital/gameplay/walker/QueuedStart.scala:116-121`
- Modify: `src/main/scala/oathdigital/gameplay/walker/ModifierChoice.scala:142-152`
- Modify: `src/main/scala/oathdigital/gameplay/actions/dice/RerollChoice.scala:185-191`
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/PlanPrice.scala:70-75`
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/PlanAid.scala:143-151,175-178`
- Modify: `src/main/scala/oathdigital/application/PlanAidDesk.scala:72-78`
- Modify: `src/main/scala/oathdigital/application/PlanAidProjector.scala:48`
- Modify: `src/main/scala/oathdigital/gameplay/actions/negotiation/NegotiationDeal.scala:90-96,145-151`
- Modify: `src/main/scala/oathdigital/gameplay/actions/minor/SelfExile.scala:40`
- Modify: `src/main/scala/oathdigital/gameplay/powers/travel/TollRoads.scala:171-173`
- Modify: `src/main/scala/oathdigital/gameplay/powers/RulerToll.scala:75-76`
- Modify: `src/main/scala/oathdigital/gameplay/powers/campaign/PeaceEnvoy.scala:165-166`
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/WitchsBargain.scala:120`
- Modify: `src/main/scala/oathdigital/gameplay/actions/BannerRules.scala:25-27`, and its six callers
- Test: `src/test/scala/oathdigital/gameplay/SpendableFavorChecksSuite.scala` (create)

**Interfaces:**
- Consumes: `SpendableFavor.of` (Task 2).
- Produces:
  - `BannerRules.playerResources(ready: ReadyGame, player: PlayerState, banner: Banner): Int`;
  - `PricedOptions.needs(cost: Spent, board: PlayerBoardState, favor: Int): Option[String]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/oathdigital/gameplay/SpendableFavorChecksSuite.scala`:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.challenge.{ChallengeProcedure,
  PlaceBannerResourceProcedure}
import oathdigital.gameplay.operations.Costs
import oathdigital.gameplay.powers.RulerToll
import oathdigital.gameplay.powers.economy.VowOfPoverty
import oathdigital.gameplay.powers.travel.TollRoads
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** The checks of a spend read a Vow of Kinship holder's Nomad bank with
  * their board (catalog batch 11, rulings 9 and 26); Vow of Poverty's
  * check reads the board only (ruling 27). */
class SpendableFavorChecksSuite extends munit.FunSuite:
  /** p1 holds the Vow with an empty board and 3 favor in the Nomad bank. */
  private val vowHolderWithBankFavor: Table =
    Table.start.adviser(p1, VowOfKinshipCard).favor(p1, 0)
      .bankFavor(Suit.Nomad, 3)

  test("a cost and a toll the holder can pay only from the bank are " +
      "affordable"):
    val ready = vowHolderWithBankFavor.ready
    assert(Costs.affordable(ready, p1, Location.SharedBank,
      Cost(favorBurnt = 3)))
    assert(!Costs.affordable(ready, p1, Location.SharedBank,
      Cost(favorBurnt = 4)))
    assert(TollRoads.canPay(ready, p1))
    assert(RulerToll.payOption(ready, p1, SiteRuler.Player(p2))
      .isInstanceOf[DecisionOption.Button])

  test("the People's Favor counts the bank, for the challenge and for " +
      "the banner's own placement"):
    val ready = vowHolderWithBankFavor.ready
    val held = ready.game.current.player(p1).get
    assertEquals(BannerRules.playerResources(ready, held,
      Banner.PeoplesFavor), 3)
    val holding = Table.from(ready).peoplesFavor(p1, favor = 1).ready
    assert(PlaceBannerResourceProcedure.heldBanners(holding, p1)
      .contains(Banner.PeoplesFavor))

  test("Vow of Poverty's REST reads the board only (ruling 27)"):
    val ready = vowHolderWithBankFavor.adviser(p1, VowOfPovertyCard)
      .bankFavor(Suit.Order, 2).turn(p1, Phase.Rest).ready
    assert(VowOfPoverty.usable(catalog, ready, p1,
      DecisionOptionRef.Denizen(VowOfPovertyCard.id)))
```

Import `VowOfPovertyCard` beside `VowOfPoverty`, and
`oathdigital.gameplay.setup.FirstGameSetupFixture.catalog`.

The banner test needs p1 to hold the People's Favor with some favor on
it. If `Table` has no `peoplesFavor` step, find the step other banner
tests use (`grep -rn "peoplesFavor" src/test/scala/oathdigital/testkit/Table.scala`),
or set the banner's holder and favor through `updateCurrent`. The Vow of
Poverty test asserts the existing behavior and must already pass: it pins
ruling 27 against a later change.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.SpendableFavorChecksSuite"`
Expected: compile error on the new `playerResources` signature, then,
once you add a stub, failures on the cost, toll and banner assertions.

- [ ] **Step 3: Read `SpendableFavor` at each spend check**

Import `oathdigital.gameplay.SpendableFavor` in each file below. In each
before/after, only the favor read changes.

1. `PowerOperations.scala` (`Costs.plan`):

   ```scala
        spendable = SpendableFavor.of(ready, actor)
        _ <- Either.cond(spendable >= favor, (),
          InsufficientFavor(favor, spendable))
   ```

2. `PricedOptions.scala`: `needs` takes the spendable favor:

   ```scala
  private[walker] def needs(cost: Spent, board: PlayerBoardState,
      favor: Int): Option[String] =
    if cost.supply > board.supply.supply then
      Some(s"Needs ${cost.supply} Supply; you have ${board.supply.supply}")
    else if cost.allFavor > favor then
      Some(s"Needs ${cost.allFavor} favor; you have $favor")
   ```

   (the secrets branch unchanged). In `shortfall`, pass
   `SpendableFavor.of(state, actor)`; in `QueuedStart`, pass
   `SpendableFavor.of(ready, entry.actor)`.
3. `ModifierChoice.shortfall`: compare and report
   `SpendableFavor.of(state, actor)` instead of `board.favor`.
4. `RerollChoice` (the `case _` branch): compare and report
   `SpendableFavor.of(ready, roller)` instead of `board.favor`.
5. `PlanPrice` (the `case _` branch): compare and report
   `SpendableFavor.of(ready, user)` instead of `board.favor`.
6. `PlanAid.spare`: `SpendableFavor.of(ready, ally) - left.favor - left.favorBurnt`;
   `PlanAid.couldCover`: `printed.favor + printed.favorBurnt - SpendableFavor.of(ready, window.user)`.
7. `PlanAidDesk.fits`: `left.favor + left.favorBurnt <= SpendableFavor.of(ready, ally)`.
8. `PlanAidProjector`: pass `SpendableFavor.of(ready, ally)` where it
   passes `held.board.favor` (the projection's "you hold" favor). Use the
   name the file has for the ally's id.
9. `NegotiationDeal.bounds` and `affordable`: read
   `SpendableFavor.of(state, author)` for the favor bound and the check.
10. `SelfExile`: `Either.cond(SpendableFavor.of(ready, actor) >= cost, ...`.
11. `TollRoads.canPay`: `SpendableFavor.of(ready, traveller) >= Favor`.
12. `RulerToll.payOption`: `val held = SpendableFavor.of(ready, payer)`.
13. `PeaceEnvoy.favor`: `SpendableFavor.of(ready, player)`.
14. `WitchsBargain`, the give option only:
    `Option.when(SpendableFavor.of(ready, actor) >= Favor)(...`. Leave the
    take at lines 149-150 on the board.
15. `BannerRules.playerResources`:

    ```scala
  /** What `player` holds toward `banner`: favor they may spend for the
    * People's Favor, a Vow of Kinship holder's Nomad bank included
    * (catalog batch 11, ruling 26), and faceup secrets for the Darkest
    * Secret. */
  def playerResources(ready: ReadyGame, player: PlayerState,
      banner: Banner): Int = banner match
    case Banner.PeoplesFavor => SpendableFavor.of(ready, player.player)
    case Banner.DarkestSecret => player.board.faceUpSecrets // CR p.26: facedown do not count.
    ```

    Pass the state at its six callers: `ChallengeModifiers.scala:36`,
    `ChallengeReach.scala:56`, `ChallengeProcedure.scala:89` and `:156`,
    `PlaceBannerResourceProcedure.scala:50` and `:108`. Each has the
    state in scope as `ready`, `state` or `ctx.state`. `BannerRules` is in
    `gameplay.actions`, which may import `oathdigital.gameplay`.

Then confirm none is left:

Run: `grep -rn 'board\.favor' src/main/scala`
Expected: only the reads finding 7 keeps (done to the player, display and
mechanics).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/operations/PowerOperations.scala src/main/scala/oathdigital/gameplay/walker/PricedOptions.scala src/main/scala/oathdigital/gameplay/walker/QueuedStart.scala src/main/scala/oathdigital/gameplay/walker/ModifierChoice.scala src/main/scala/oathdigital/gameplay/actions/dice/RerollChoice.scala src/main/scala/oathdigital/gameplay/actions/campaign/PlanPrice.scala src/main/scala/oathdigital/gameplay/actions/campaign/PlanAid.scala src/main/scala/oathdigital/application/PlanAidDesk.scala src/main/scala/oathdigital/application/PlanAidProjector.scala src/main/scala/oathdigital/gameplay/actions/negotiation/NegotiationDeal.scala src/main/scala/oathdigital/gameplay/actions/minor/SelfExile.scala src/main/scala/oathdigital/gameplay/powers/travel/TollRoads.scala src/main/scala/oathdigital/gameplay/powers/RulerToll.scala src/main/scala/oathdigital/gameplay/powers/campaign/PeaceEnvoy.scala src/main/scala/oathdigital/gameplay/powers/action/WitchsBargain.scala src/main/scala/oathdigital/gameplay/actions/BannerRules.scala src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/oathdigital/gameplay/actions/challenge/PlaceBannerResourceProcedure.scala src/test/scala/oathdigital/gameplay/SpendableFavorChecksSuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): spend checks count a Vow of Kinship holder's Nomad bank

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: The source question, and the card registered

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/powers/economy/VowOfKinship.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powerresolver/AsksBeforeBatch.scala` (`BatchContext`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/BatchQuestions.scala` (`of`; add `single`, `asked`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala` (`runNarrowed`, `resumeAsked`)
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala` (`resolveAt`)
- Modify: `src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala` (`contributing`)
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `denizen.vow-of-kinship`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Modify: any test that builds a `BatchContext` (`grep -rn 'BatchContext(' src/test/scala`)
- Test: `src/test/scala/oathdigital/gameplay/walker/BatchQuestionsSuite.scala`
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipSuite.scala` (create)

**Interfaces:**
- Consumes: `DrawNomadFavor` (Task 1); `KinshipRule`, `VowOfKinshipCard`
  (Task 2); `KinshipDraws.spent` (Task 3); slice 4's `AsksBeforeBatch`,
  `BatchAsk`, `Replacement`, `BatchQuestions.subtree` and `segment`.
- Produces:
  - `BatchContext(catalog, actor, ready, operations: Vector[CoreOperation])`;
  - `BatchQuestions.single(delta: CoreOperation, strict: Boolean): BuildOps`;
  - `BatchQuestions.asked(leaf: LeafOperation, strict: Boolean): Option[BuildOps]`;
  - `case object VowOfKinship extends AsksBeforeBatch` in
    `gameplay.powers.economy`, with `decisionId: String`
    (`"vow-of-kinship.source"`) and keys `spentFromBank`,
    `spentFromBankOffTurn`.

- [ ] **Step 1: Write the failing tests**

In `BatchQuestionsSuite.scala`, add a test that a plain leaf asks as a
batch of one. Use the suite's stub `AsksBeforeEachSupplySpend` and its
`SpendSupply` operation. Walk the `SpendSupply` itself as the tree (not
inside a `BuildOps`):

```scala
  test("a plain leaf a power asks about parks on the question under its " +
      "node, and runs as built on a decline"):
    val park = parked(ProcedureWalker.advance(ready, spendOneSupply, None,
      asking))
    assertEquals(park.tree.at, Vector(BatchQuestions.segment, "0"))
```

Name the walker powers value as the suite names it (`asking` or the
like). If the suite's stub needs the batch to hold the spend twice, adapt
the expected path: the questions sit at `asked.0` to `asked.(n-1)`.

Create `src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipSuite.scala`:

```scala
package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.VowOfKinshipCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.walker.{ProcedureWalker, WalkerPowers}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseAmountAnswer
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.{finished, parked}
import oathdigital.testkit.ProcedureWalkerFixture.applyEvents
import oathdigital.testkit.Table.{p1, p2}

/** Vow of Kinship's source question (catalog batch 11, ruling 10): with
  * favor on the holder's board and in the Nomad bank, a spend asks how
  * much comes from the bank. */
class VowOfKinshipSuite extends munit.FunSuite:
  private val powers = WalkerPowers(Vector(VowOfKinship))

  /** `holder` holds the Vow with `board` favor on their board and `bank`
    * in the Nomad bank. It is p1's turn. */
  private def vowHolder(holder: PlayerId, board: Int, bank: Int): Table =
    Table.start.adviser(holder, VowOfKinshipCard).favor(holder, board)
      .bankFavor(Suit.Nomad, bank)

  private def burning(player: PlayerId, n: Int): PayCost =
    PayCost(player, Location.SharedBank, Cost(favorBurnt = n))

  private def batchOf(ops: CoreOperation*): BuildOps =
    BuildOps((_, _) => Right(ops.toVector))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(VowOfKinship.id, VowOfKinship.noteKeys, events)

  test("with both sources, a spend asks how much comes from the bank, " +
      "and the answer draws it"):
    val ready = vowHolder(p1, board = 2, bank = 4).ready
    val tree = batchOf(burning(p1, 3))
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    assertEquals(park.decide.decisionId, VowOfKinship.decisionId)
    assertEquals(park.decide.owner, p1)
    assertEquals(park.decide.query, DecisionQuery.ChooseAmount(1, 3,
      Some("Vow of Kinship: how much of the 3 favor comes from the " +
        "Nomad bank?"), "Take from the Nomad bank"))
    val done = finished(ProcedureWalker.resolve(
      applyEvents(ready, park.events), tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(2), p1), powers))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 2)
    assertEquals(said(done.events), Vector(NoteText.Said("spent-from-bank",
      "Spent 2 favor from the Nomad bank.", covers = false)))
    Outcomes.assertWireRoundTrips(park.events ++ done.events)

  test("a cost walked as its moves asks too"):
    val ready = vowHolder(p1, board = 2, bank = 4).ready
    val park = parked(ProcedureWalker.advance(ready, burning(p1, 3), None,
      powers))
    assertEquals(park.decide.decisionId, VowOfKinship.decisionId)

  test("with only one source, nothing is asked"):
    val bankOnly = finished(ProcedureWalker.advance(
      vowHolder(p1, board = 0, bank = 4).ready, batchOf(burning(p1, 3)),
      None, powers))
    assertEquals(Look(bankOnly.treeless).bankFavor(Suit.Nomad), 1)
    val boardOnly = finished(ProcedureWalker.advance(
      vowHolder(p1, board = 4, bank = 0).ready, batchOf(burning(p1, 3)),
      None, powers))
    assertEquals(Look(boardOnly.treeless).favor(p1), 1)

  test("a board that must give all it has still asks only for the rest"):
    // Board 1, bank 4, a spend of 3: at least 2 come from the bank.
    val park = parked(ProcedureWalker.advance(
      vowHolder(p1, board = 1, bank = 4).ready, batchOf(burning(p1, 3)),
      None, powers))
    park.decide.query match
      case DecisionQuery.ChooseAmount(min, max, _, _, _) =>
        assertEquals((min, max), (2, 3))
      case other => fail(s"expected an amount, got $other")

  test("an off-turn holder's line names them"):
    val ready = vowHolder(p2, board = 1, bank = 4).ready
    val tree = batchOf(Give(Piece.Favor(2), p2, Location.PlayArea(p2),
      Location.PlayArea(p1), required = true))
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    assertEquals(park.decide.owner, p2)
    val done = finished(ProcedureWalker.resolve(
      applyEvents(ready, park.events), tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(2), p2), powers))
    assertEquals(said(done.events), Vector(NoteText.Said(
      "spent-from-bank-off-turn",
      s"${p2.value} spent 2 favor from the Nomad bank.", covers = false)))
```

`parked(...)` returns the park; if its decision is read another way in
this codebase (`park.tree` with the walker's parked decide), use what
`BatchQuestionsSuite` uses to read the parked `Decide`. Keep the
assertions.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.BatchQuestionsSuite oathdigital.gameplay.powers.economy.VowOfKinshipSuite"`
Expected: compile error: `VowOfKinship` is not found; the new
`BatchQuestionsSuite` test fails (a plain leaf asks nothing yet).

- [ ] **Step 3: Give the seam the batch, and plain leaves**

In `AsksBeforeBatch.scala`:

```scala
/** What a power reads when it looks at a walker batch before the batch
  * runs (catalog batch 11, slices 4 and 5): the walk's catalog, the walk's
  * actor, the state the batch was built in, and the batch as built. */
final case class BatchContext(catalog: ExecutableCatalog, actor: PlayerId,
    ready: ReadyGame, operations: Vector[CoreOperation])
```

In `BatchQuestions.of`, build it with the batch:
`val context = BatchContext(powers.catalog, actor, ready, operations)`.
Fix each test that builds a `BatchContext` by passing the operations it
asks about (`Vector(operation)`).

In `BatchQuestions`, add:

```scala
  /** A plain leaf, asked about as a batch of one. */
  def single(delta: CoreOperation, strict: Boolean): BuildOps =
    BuildOps((_, _) => Right(Vector(delta)), required = strict)

  /** The batch a leaf's questions are about: a `BuildOps` itself, or a
    * plain leaf as a batch of one. */
  def asked(leaf: LeafOperation, strict: Boolean): Option[BuildOps] =
    leaf match
      case build: BuildOps => Some(build)
      case delta: CoreOperation => Some(single(delta, strict))
      case _ => None
```

Add to the object's doc comment: "A plain leaf, such as a cost's move
when the cost is walked as its moves, is asked about as a batch of one
(catalog batch 11, slice 5)."

In `ProcedureWalker.runNarrowed`, in `case None =>`, replace

```scala
            case delta: CoreOperation =>
              WalkerSteps.record(delta, ctx, path, contributions, strict).map(Done(_))
```

with

```scala
            // A plain leaf a power asks about is walked as its questions,
            // then the answered batch of one ([[BatchQuestions]]); a search
            // records it as built.
            case delta: CoreOperation if !ctx.searching =>
              BatchQuestions.subtree(BatchQuestions.single(delta, strict),
                  ctx.state, ctx.answered, ctx.powers, ctx.actor, path)
                .flatMap {
                  case Some(asking) => walk(asking, ctx,
                    path :+ BatchQuestions.segment, None, resume,
                    WalkerHooks(contributions, Set.empty, strict))
                  case None => WalkerSteps.record(delta, ctx, path,
                    contributions, strict).map(Done(_))
                }
            case delta: CoreOperation =>
              WalkerSteps.record(delta, ctx, path, contributions, strict).map(Done(_))
```

Replace `resumeAsked`'s body so that it accepts both leaf kinds:

```scala
  private def resumeAsked(leaf: LeafOperation, ctx: WalkCtx,
      path: Vector[String], rest: Vector[String], resume: Resume,
      contributions: Vector[PowerId], strict: Boolean)
      : Either[OathViolation, Step] =
    BatchQuestions.asked(leaf, strict) match
      case Some(build) => BatchQuestions.subtree(build, ctx.state,
          ctx.answered, ctx.powers, ctx.actor, path).flatMap {
        case Some(asking) => walk(asking, ctx, path :+ BatchQuestions.segment,
          Some(rest), resume, WalkerHooks(contributions, Set.empty, strict))
        case None => contractViolation(s"resume path enters questions the " +
          s"batch at ${path.mkString(".")} no longer asks")
      }
      case None => contractViolation(s"resume path enters questions at " +
        s"leaf ${WalkerSteps.leafLabel(leaf)}, which asks none")
```

In `WalkerPowerGather.resolveAt`, next to slice 4's `BuildOps` case for
the segment, add the plain-leaf case, before the `ExecutedTriggers` case:

```scala
        // Under a plain leaf whose batch of one asked.
        case delta: CoreOperation if segment == BatchQuestions.segment =>
          BatchQuestions.subtree(BatchQuestions.single(delta, strict = false),
              state, pending.answered, powers,
              state.game.current.walkActor, consumed).toOption.flatten
            .flatMap(resolveAt(state, pending, powers, _, remaining.tail,
              consumed :+ segment, Set.empty))
```

Use the same argument names as slice 4's `BuildOps` case in that method.
A composite reaches this case only through a numbered child, so no
composite's children are mistaken for questions.

- [ ] **Step 4: Write the power**

Create `src/main/scala/oathdigital/gameplay/powers/economy/VowOfKinship.scala`:

```scala
package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.{KinshipDraws, KinshipRule, VowOfKinshipCard}
import oathdigital.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, Replacement}
import oathdigital.model._

/** Vow of Kinship (card 152, catalog batch 11, rulings 9, 10, 12 and 26 to
  * 29). The credit, the draw and the spend checks are rules
  * (`KinshipRule`, `KinshipDraws`, `SpendableFavor`). This power asks the
  * source question.
  *
  * When the holder's board and the Nomad bank both hold favor, a batch
  * whose spend could take from either asks the holder, at its first spend,
  * how much of the batch's spend comes from the bank (ruling 10). The
  * least is what the board cannot cover; the most is the spend, or the
  * bank if it holds less. The answer puts a `DrawNomadFavor` in front of
  * that spend, and the board pays the rest. A search walks the batch as
  * built, which draws only what the board lacks.
  *
  * The line reads "Spent {2 favor} from the Nomad bank.", naming the
  * holder when they are not the walk's actor. An answer of none writes no
  * line.
  */
case object VowOfKinship extends AsksBeforeBatch:
  val id: PowerId = VowOfKinshipCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  val decisionId: String = "vow-of-kinship.source"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Spent {2 favor} from the Nomad bank." */
  val spentFromBank: NoteKey = notes.key("spent-from-bank", Vector(
    NotePart.Text("Spent "), NotePart.Arg(0),
    NotePart.Text(" from the Nomad bank.")))
  /** "{Blue} spent {2 favor} from the Nomad bank.", for a holder who is
    * not the walk's actor. */
  val spentFromBankOffTurn: NoteKey = notes.key("spent-from-bank-off-turn",
    Vector(NotePart.Arg(0), NotePart.Text(" spent "), NotePart.Arg(1),
      NotePart.Text(" from the Nomad bank.")))

  def watching(ready: ReadyGame): Boolean =
    KinshipRule.bank(ready) > 0 && ready.game.current.players.exists(
      player => player.board.favor > 0 &&
        KinshipRule.holds(ready, player.player))

  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk] = for
    holder <- spender(batch.ready, operation)
    if batch.operations.find(KinshipDraws.spent(_, holder) > 0)
      .contains(operation)
    total = batch.operations.map(KinshipDraws.spent(_, holder)).sum
    board = batch.ready.game.current.player(holder).fold(0)(_.board.favor)
    bank = KinshipRule.bank(batch.ready)
    least = math.max(0, total - board)
    most = math.min(total, bank)
    if board > 0 && bank > 0 && least < most
  yield BatchAsk(
    Decide(decisionId, holder, DecisionQuery.ChooseAmount(least, most,
      Some(s"Vow of Kinship: how much of the $total favor comes from the " +
        "Nomad bank?"), "Take from the Nomad bank")),
    {
      case DecisionAnswer.ChooseAmountAnswer(drawn) if drawn > 0 =>
        Some(Replacement(Vector(DrawNomadFavor(holder, drawn), operation),
          line(holder, batch.actor, drawn)))
      case _ => None
    })

  /** The holder whose spend `operation` makes, if any. */
  private def spender(ready: ReadyGame, operation: CoreOperation)
      : Option[PlayerId] =
    ready.game.current.players.map(_.player).find(player =>
      KinshipRule.holds(ready, player) &&
        KinshipDraws.spent(operation, player) > 0)

  private def line(holder: PlayerId, actor: PlayerId, drawn: Int): LogNote =
    val card = PowerSourceRef.Card(VowOfKinshipCard.id)
    val amount = NoteArg.Amount(drawn, NoteUnit.Favor)
    if holder == actor then spentFromBank(card, amount)
    else spentFromBankOffTurn(card, NoteArg.Player(holder), amount)
```

`KinshipRule` is `private[gameplay]`, and this file is in
`oathdigital.gameplay.powers.economy`, inside `gameplay`, so it may read
it.

- [ ] **Step 5: Register the card**

1. In `WalkerPowerCatalog.contributing`, add `VowOfKinship` after
   `VowOfUnionAdvisers` in the vector that holds `FamilyWagon`. The file
   imports `oathdigital.gameplay.powers.economy._` already.
2. In `src/test/resources/powers/unimplemented.txt`, delete the line
   `denizen.vow-of-kinship`.
3. In `src/test/resources/notes/declared-keys.txt`, add these two lines in
   sorted position among the `denizen.v` keys:

```text
denizen.vow-of-kinship spent-from-bank = Spent {0} from the Nomad bank.
denizen.vow-of-kinship spent-from-bank-off-turn = {0} spent {1} from the Nomad bank.
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.* oathdigital.gameplay.powers.*"`
Expected: PASS, including the declared-keys and unimplemented checks.

- [ ] **Step 7: Check the line bound**

Run: `python3 scripts/check-architecture.py`
Expected: passes. `ProcedureWalker.scala` grows by about 15 lines and
`WalkerPowerGather.scala` by about 8.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/economy/VowOfKinship.scala src/main/scala/oathdigital/gameplay/powerresolver/AsksBeforeBatch.scala src/main/scala/oathdigital/gameplay/walker/BatchQuestions.scala src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/oathdigital/gameplay/walker/BatchQuestionsSuite.scala src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Vow of Kinship asks how much of a spend comes from the Nomad bank

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add each test file whose `BatchContext` you updated in Step 3.

**Checkpoint review after Task 5:** the card is registered. A reviewer
checks:
- the plain-leaf path in `runNarrowed`, `resumeAsked` and `resolveAt`,
  with a resume into a cost walked as its moves;
- that a search never asks (slice 4 finding 7);
- that `./sbtw "testOnly oathdigital.gameplay.walker.*"` passes.

---

### Task 6: The Vow's arrival moves the board's favor to the bank

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/powers/economy/VowOfKinship.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipSuite.scala`

**Interfaces:**
- Consumes: `FollowsArrivals`, `AdviserArrivals`, `ArrivalFollowUp`
  (`powerresolver`).
- Produces: `VowOfKinship extends AsksBeforeBatch, FollowsArrivals`, with
  keys `moved` and `movedOffTurn`.

- [ ] **Step 1: Write the failing test**

In `VowOfKinshipSuite.scala`, add:

```scala
  test("turned faceup, it moves its holder's board favor to the Nomad " +
      "bank"):
    // p1 holds the Vow facedown with 3 favor; revealing it is an arrival.
    val ready = Table.start.adviser(p1, VowOfKinshipCard, facedown = true)
      .favor(p1, 3).bankFavor(Suit.Nomad, 1).ready
    val done = finished(ProcedureWalker.advance(ready,
      Reveal(VowOfKinshipCard.id, Location.PlayArea(p1)), None, powers))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 0)
    assertEquals(look.bankFavor(Suit.Nomad), 4)
    assertEquals(said(done.events), Vector(NoteText.Said("moved",
      "Moved 3 favor to the Nomad bank.", covers = false)))

  test("an arrival with no favor on the board moves nothing and says " +
      "nothing"):
    val ready = Table.start.adviser(p1, VowOfKinshipCard, facedown = true)
      .favor(p1, 0).ready
    val done = finished(ProcedureWalker.advance(ready,
      Reveal(VowOfKinshipCard.id, Location.PlayArea(p1)), None, powers))
    assertEquals(said(done.events), Vector.empty)
```

If a `Reveal` of an adviser is not accepted at `PlayArea`, reveal it the
way another arrival test does (`grep -rn "onArrival" src/test/scala` and
copy the setup of a `SuitOnlyAdvisers` arrival test).

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.economy.VowOfKinshipSuite"`
Expected: FAIL: the board still holds 3.

- [ ] **Step 3: Follow arrivals**

In `VowOfKinship.scala`, extend `FollowsArrivals` too
(`case object VowOfKinship extends AsksBeforeBatch, FollowsArrivals:`),
import `AdviserArrivals`, `ArrivalFollowUp` and `FollowsArrivals` from
`powerresolver`, and add:

```scala
  /** "Moved {3 favor} to the Nomad bank." */
  val moved: NoteKey = notes.key("moved", Vector(NotePart.Text("Moved "),
    NotePart.Arg(0), NotePart.Text(" to the Nomad bank.")))
  /** "{Blue} moved {3 favor} to the Nomad bank.", for a holder who is not
    * the walk's actor. */
  val movedOffTurn: NoteKey = notes.key("moved-off-turn", Vector(
    NotePart.Arg(0), NotePart.Text(" moved "), NotePart.Arg(1),
    NotePart.Text(" to the Nomad bank.")))

  /** The Vow turned faceup among a player's advisers: their board's favor
    * moves to the Nomad bank. Nobody moves it. */
  def onArrival(arrived: AdviserArrivals): Option[ArrivalFollowUp] = for
    arrival <- arrived.arrivals.find(_.card == VowOfKinshipCard.id)
    holder = arrival.player
    favor <- arrived.after.game.current.player(holder).map(_.board.favor)
    if favor > 0
  yield ArrivalFollowUp(Vector(Move(Piece.Favor(favor),
      PositionedLocation(Location.PlayArea(holder)),
      PositionedLocation(Location.FavorBank(Suit.Nomad)))),
    Note(id, _ => Some(movedLine(holder, arrived.actor, favor))))

  private def movedLine(holder: PlayerId, actor: PlayerId, favor: Int)
      : LogNote =
    val card = PowerSourceRef.Card(VowOfKinshipCard.id)
    val amount = NoteArg.Amount(favor, NoteUnit.Favor)
    if holder == actor then moved(card, amount)
    else movedOffTurn(card, NoteArg.Player(holder), amount)
```

Add to the object's doc comment: "When the Vow becomes a faceup adviser,
its holder's board favor moves to the Nomad bank (Readings). When it
leaves, the favor stays in the bank."

If `Note`'s second argument is not optional, pass `covers = false`.

In `declared-keys.txt`, add in sorted position:

```text
denizen.vow-of-kinship moved = Moved {0} to the Nomad bank.
denizen.vow-of-kinship moved-off-turn = {0} moved {1} to the Nomad bank.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/economy/VowOfKinship.scala src/test/resources/notes/declared-keys.txt src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Vow of Kinship moves its holder's favor to the Nomad bank as it arrives

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Favor lines count the bank, and gain lines say where it went

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/NoteSupport.scala`
- Modify: the files of finding 8's second list (one line each, below)
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipLinesSuite.scala` (create)

**Interfaces:**
- Consumes: `NoteArg.ToNomadBank` (Task 1); `KinshipRule` (Task 2).
- Produces, in `NoteSupport`:
  - `favor(step, player)` counts a holder's Nomad bank;
  - `banked(step: Step, player: PlayerId): Boolean`;
  - `toBank(step: Step, player: PlayerId)(note: LogNote): LogNote`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipLinesSuite.scala`:

```scala
package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.{NoteSupport, VowOfKinshipCard}
import oathdigital.gameplay.operations.{OperationPipeline, OperationPolicy}
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** A Vow of Kinship holder's favor lines (catalog batch 11, ruling 29):
  * they count the Nomad bank with the board, and a gain line says the
  * favor went to the bank. */
class VowOfKinshipLinesSuite extends munit.FunSuite:
  /** p1 holds the Vow with an empty board; the Nomad bank holds 2 and the
    * Order bank 3. */
  private val vowHolder: Table = Table.start.adviser(p1, VowOfKinshipCard)
    .bankFavor(Suit.Nomad, 2).bankFavor(Suit.Order, 3)

  /** The states around `ops`, run as one batch. */
  private def step(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): NoteSupport.Step =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).fold(violation => fail(violation.toString),
      run => (ready, run.state))

  test("a gain counts as the holder's and is marked for the bank"):
    val gained = step(vowHolder.ready, Gain.Favor(p1, Suit.Order, 2))
    assertEquals(NoteSupport.favor(gained, p1), 2)
    assert(NoteSupport.banked(gained, p1))
    val note = LogNote(None, "gained", Vector(NoteArg.Number(2)))
    assertEquals(NoteSupport.toBank(gained, p1)(note).args,
      Vector(NoteArg.Number(2), NoteArg.ToNomadBank))

  test("a spend from the bank counts as paid, and is not marked"):
    val paid = step(vowHolder.ready, DrawNomadFavor(p1, 1),
      PayCost(p1, Location.SharedBank, Cost(favorBurnt = 1)))
    assertEquals(NoteSupport.favor(paid, p1), -1)
    assert(!NoteSupport.banked(paid, p1))

  test("another player's gain is neither counted for the holder nor " +
      "marked"):
    val gained = step(vowHolder.ready, Gain.Favor(p2, Suit.Order, 2))
    assertEquals(NoteSupport.favor(gained, p1), 0)
    assert(!NoteSupport.banked(gained, p2))
```

Then add one golden line through a real gain line: in `TradeProcedure`'s
log test suite, or in `VowOfKinshipSuite`, Trade with a Vow holder and
assert the Trade's line ends ", to the Nomad bank". The shortest path is
`EconomyActions.trade(rules, ready, p1, Vector.empty, "favor", DecisionOptionRef.Denizen(card))`
as `GrandMaskRulesSuite` uses it, with p1 holding the Vow and an Order
denizen at their site; assert
`NoteText.said(ProcedureNotes.id(ActionRef.Trade), TradeProcedure.noteKeys, done.journal.events)`
holds a line whose text ends with ", to the Nomad bank". Use the names
`TradeProcedure` exposes for its key set.

In `RevelationSuite`'s Vow of Kinship test from Task 3, add the line,
which counts the bank's change from this task on:

```scala
    assertEquals(said(power, walked.events), Vector(burned(p1, 2)))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.economy.VowOfKinshipLinesSuite"`
Expected: compile errors: `banked` and `toBank` are not found.

- [ ] **Step 3: Count the bank and mark gains**

In `NoteSupport.scala`:

```scala
  /** `player`'s favor change in the step: their board's, and for a Vow of
    * Kinship holder the Nomad bank's too, so a gain into the bank or a
    * spend from it reads as theirs (catalog batch 11, slice 5). */
  def favor(step: Step, player: PlayerId): Int =
    change(step, player)(_.favor) +
      (if holder(step, player) then nomadBank(step._2) - nomadBank(step._1)
       else 0)

  /** Whether favor went to the Nomad bank for `player` in the step: they
    * hold the Vow, and the bank rose (ruling 29). */
  def banked(step: Step, player: PlayerId): Boolean =
    holder(step, player) && nomadBank(step._2) > nomadBank(step._1)

  /** `note`, ending ", to the Nomad bank" when the favor it tells of went
    * there for `player`. */
  def toBank(step: Step, player: PlayerId)(note: LogNote): LogNote =
    if banked(step, player) then note.copy(args = note.args :+ NoteArg.ToNomadBank)
    else note

  private def holder(step: Step, player: PlayerId): Boolean =
    KinshipRule.holds(step._1, player) || KinshipRule.holds(step._2, player)

  private def nomadBank(ready: ReadyGame): Int =
    ready.banks.favor.getOrElse(Suit.Nomad, 0)
```

In the same file, mark the helpers' lines:
- `gainedNote`: `yield toBank(step, player)(key(card, ...))` when `unit`
  is `NoteUnit.Favor`, else unchanged;
- `gainedFromNote` and `tookNote`: wrap the yielded note in
  `toBank(step, player)`.

`gainNote` goes through `gainedNote`, so it needs no change.

- [ ] **Step 4: Mark each power's and procedure's own gain line**

At each site below, wrap the gain line in `NoteSupport.toBank(step,
receiver)(...)`, where `step` is the step the line reads and `receiver`
is the player who gained. A site that reads
`states.previous.fold(0)(...)` binds no `step`; there, take
`states.previous` and wrap with it when it is defined.

| File | Line | Receiver |
|---|---|---|
| `powers/whenplayed/Blackmail.scala` | `tookNote` (`:141-145`) | `actor` |
| `powers/title/ChaosCult.scala` | `tookNote` (`:63-70`) | `holder` |
| `powers/economy/VowOfPoverty.scala` | `tookNote` (`:88-95`) | `player` |
| `powers/economy/SecretSignal.scala` | `gainedNote` (`:87-92`) | `actor` |
| `powers/economy/MovingMarket.scala` | `:130-132` | `actor` |
| `powers/action/BookOfRecords.scala` | `tookNote`, People's Favor only (`:93-107`) | `actor` |
| `powers/action/GamblingHall.scala` | `:67-70` | `player` |
| `powers/action/PiedPiper.scala` | `:126-128` | `actor` |
| `powers/campaign/StickyFire.scala` | `:91-98` (both lines) | `loser` |
| `powers/action/Bog.scala` | `:64-66` | `player` |
| `powers/action/TamingCharm.scala` | `:103-106` | `player` |
| `powers/action/WitchsBargain.scala` | `:181` (gained) | `actor` |
| `powers/action/WitchsBargain.scala` | `:183` (gave) | `other` |
| `powers/action/CharmingFriend.scala` | `:51-53` | `actor` |
| `powers/campaign/PeaceEnvoy.scala` | `:202-204` | `enemy` |
| `powers/whenplayed/RoyalTax.scala` | `tookNote` (`:73-77`) | `actor` |
| `actions/economy/TradeProcedure.scala` | `tradedNote`, `tradedForFavor` only (`:86-95`) | `actor` |
| `phases/wake/TakeWealthProcedure.scala` | `tookNote`, favor only (`:121-126`) | `actor` |
| `powers/RulerToll.scala` | `note` (`:99-105`), a toll to a player | the owner |
| `powers/travel/TollRoads.scala` | its paid line (`:320-325`), a toll to a player | the owner |

Paths are under `src/main/scala/oathdigital/gameplay/`. The toll lines
name the ruler who received the favor; mark them for that ruler. A line
about a loss or a payment (Revelation, Dissent, Defame, Plague Engines,
Whispering Leaves, the Campaign's burn, Challenge, the banner action)
stays unmarked.

Then confirm nothing is missed:

Run: `grep -rn 'NoteSupport.favor' src/main/scala`
Expected: each positive read (a gain) sits in a line the table or the
helpers mark; each negative read (`-NoteSupport.favor`) is a loss or a
payment.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw test`
Expected: PASS, including Revelation's line.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/NoteSupport.scala src/test/scala/oathdigital/gameplay/powers/economy/VowOfKinshipLinesSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/RevelationSuite.scala
git add src/main/scala/oathdigital/gameplay/powers/whenplayed/Blackmail.scala src/main/scala/oathdigital/gameplay/powers/title/ChaosCult.scala src/main/scala/oathdigital/gameplay/powers/economy/VowOfPoverty.scala src/main/scala/oathdigital/gameplay/powers/economy/SecretSignal.scala src/main/scala/oathdigital/gameplay/powers/economy/MovingMarket.scala src/main/scala/oathdigital/gameplay/powers/action/BookOfRecords.scala src/main/scala/oathdigital/gameplay/powers/action/GamblingHall.scala src/main/scala/oathdigital/gameplay/powers/action/PiedPiper.scala src/main/scala/oathdigital/gameplay/powers/campaign/StickyFire.scala src/main/scala/oathdigital/gameplay/powers/action/Bog.scala src/main/scala/oathdigital/gameplay/powers/action/TamingCharm.scala src/main/scala/oathdigital/gameplay/powers/action/WitchsBargain.scala src/main/scala/oathdigital/gameplay/powers/action/CharmingFriend.scala src/main/scala/oathdigital/gameplay/powers/campaign/PeaceEnvoy.scala src/main/scala/oathdigital/gameplay/powers/whenplayed/RoyalTax.scala src/main/scala/oathdigital/gameplay/actions/economy/TradeProcedure.scala src/main/scala/oathdigital/gameplay/phases/wake/TakeWealthProcedure.scala src/main/scala/oathdigital/gameplay/powers/RulerToll.scala src/main/scala/oathdigital/gameplay/powers/travel/TollRoads.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): favor lines count the Nomad bank, and say when favor went there

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add any test file holding the Trade golden line from Step 1.

---

### Task 8: Wild Pastures and Master of Disguise, then close the slice

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/powers/economy/WildPastures.scala` (`askBefore`)
- Modify: `docs/ROADMAP.md:96`
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md:3`
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/WildPasturesSuite.scala`
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/MasterOfDisguiseSuite.scala`

**Interfaces:**
- Consumes: `SpendableFavor.of` (Task 2); slice 4's `WildPastures`.

- [ ] **Step 1: Write the failing tests**

In `WildPasturesSuite.scala`, add a test modelled on its Taming Charm
"pay" test, with the ruler also holding `VowOfKinshipCard`, an empty
board, and 2 favor in the Nomad bank:

```scala
  test("a Vow of Kinship holder pays out of the Nomad bank into it " +
      "(ruling 12)"):
    // As the Taming Charm pay test, with the ruler holding the Vow, an
    // empty board and 2 favor in the Nomad bank.
```

Build it with the suite's `pasturesAtPeaksRuledBy` helper, then
`Table.from(...).adviser(ruler, VowOfKinshipCard).favor(ruler, 0)
.bankFavor(Suit.Nomad, 2)`. Assert, after the "pay" answer:
- the question was asked (with the board empty, it would not be without
  this task);
- the Nomad card stayed;
- the Nomad bank still holds 2 and the ruler's board 0, except for what
  the power's own reward adds (Taming Charm's 2 favor go to the bank, so
  the bank ends at 4);
- the line is still "Paid 1 favor to the Nomad bank instead of
  discarding {card}.".

In `MasterOfDisguiseSuite.scala`, add:

```scala
  test("a Vow of Kinship holder disguised as another player gains the " +
      "Trade's favor on their board, and one disguised as a holder " +
      "gains it in the Nomad bank"):
    // p1 holds Master of Disguise and Vow of Kinship; p2 holds neither.
    // Disguised as p2, the Vow is lifted for the Trade.
    val lifted = disguiseHolderBesideWrestlers()
      .adviser(p1, oathdigital.gameplay.VowOfKinshipCard).ready
    val liftedEnd = Outcomes.ready(disguisedAs(tradeForFavorBegun(lifted),
      p2))
    // ...finish the Trade as the suite's other disguised Trades do...
    assert(Look(liftedEnd).favor(p1) > 0)
    // p2 holds the Vow; disguised as p2, p1 acts with it.
    val lent = Table.from(disguiseHolderBesideWrestlers().ready)
      .adviser(p2, oathdigital.gameplay.VowOfKinshipCard).ready
    val lentEnd = Outcomes.ready(disguisedAs(tradeForFavorBegun(lent), p2))
    assertEquals(Look(lentEnd).favor(p1), 0)
    assert(Look(lentEnd).bankFavor(Suit.Nomad) > 0)
```

Finish each Trade the way the suite's other disguised-Trade tests do
(answer the remaining decisions until the Trade completes), then make the
assertions on the finished state. Replace the `> 0` with the exact favor
the Trade gives, once you see the number.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.economy.WildPasturesSuite oathdigital.gameplay.powers.economy.MasterOfDisguiseSuite"`
Expected: Wild Pastures' new test fails (no question with an empty
board). Master of Disguise's test may already pass, since Tasks 2 and 3
read the as-if advisers; it pins the FAQ.

- [ ] **Step 3: Ask with spendable favor**

In `WildPastures.askBefore`, replace

```scala
    if batch.ready.game.current.player(ruler).exists(_.board.favor >= 1)
```

with

```scala
    // A Vow of Kinship ruler may pay from the Nomad bank (ruling 12).
    if SpendableFavor.of(batch.ready, ruler) >= 1
```

and import `oathdigital.gameplay.SpendableFavor`. Update the doc
comment's "while they have favor on their board" to "while they have
favor they may spend". The pay answer's `Give` from the ruler's board
draws on the bank through `KinshipDraws`, and the credit lands back in
it, so nothing moves.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.economy.*"`
Expected: PASS.

- [ ] **Step 5: Close the docs**

1. In `docs/ROADMAP.md`, delete the line
   `- **Favor source redirection:** Vow of Kinship.`
2. In the spec, change the status line to
   `**Status:** designed 2026-11-07. Slices 0 to 5 landed.`

- [ ] **Step 6: Run every check**

Run, in this order:

```bash
python3 scripts/check-markdown-links.py
```

```bash
python3 scripts/check-architecture.py
```

```bash
./sbtw frontend/test
```

```bash
./sbtw clean coverage test coverageReport
```

Run the last one in the background; it takes several minutes.
Expected: each passes, and coverage stays at 92% or more.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/economy/WildPastures.scala src/test/scala/oathdigital/gameplay/powers/economy/WildPasturesSuite.scala src/test/scala/oathdigital/gameplay/powers/economy/MasterOfDisguiseSuite.scala docs/ROADMAP.md docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 11 slice 5, Vow of Kinship

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

## Known limits

- **One question per batch.** A batch with spends by two holders asks only
  the first holder found. No production batch spends for two players.
- **Wild Pastures' payment is not asked about.** The batch under a
  question asks nothing again (slice 4), so a ruler with both sources pays
  Wild Pastures' favor from the board first.
- **A line in a busy step.** `NoteSupport.favor` and `banked` read the
  Nomad bank's change over the whole step. A step where another player
  also moves Nomad-bank favor would count that change for the holder. No
  production step does both.
