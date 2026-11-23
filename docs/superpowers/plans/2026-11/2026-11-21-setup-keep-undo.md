# Setup Keep Undo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In a later game's Setup, a player who keeps a legacy can undo the keep until the next keeper answers.

**Architecture:** Setup deals every seat's legacies before the first keep. The deal records each seat's dealt pair with `Defer` under `setup.legacy-deal.<owner>`. Each keep reads its pair back with `Deferred.resolve`, so a keep holds no hidden outcome and closes as an ordinary command batch. A first game uses the same deal and keep; only the keeps' place differs.

**Tech Stack:** Scala 3, munit, sbt through `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11/2026-11-21-setup-keep-undo-design.md`. Read it before starting. Task 1 adds its ruling 8.

## Global Constraints

- Work only in the worktree `/Users/roman/projects/distantlands/.worktrees/setup-keep-undo`, on branch `setup-keep-undo`. Never commit on `0.2.0-alpha` or `main`.
- `.tooling` and `node_modules` in the worktree are symlinks to the main checkout. Never `git add -A`; add files by path.
- Decision id of the keep stays `setup.legacy-choice.<owner>` (`LegacyKeep.decisionId`). Its heading, confirm label ("Keep Legacy") and log lines do not change.
- The deal's record key is `DeferralKey("setup.legacy-deal.<owner>")`.
- First-game behaviour must not change: every first-game suite passes without edits.
- Old journals need not replay (no old replay support).
- Never write a bare `===` or `=word` in a shell command (zsh aborts the line).
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Docs, comments and commit messages are plain English prose.

### Protected

These must behave exactly as before. Do not edit them except where a step names the edit:

- `UndoBarrier`, `JournalFold`, `UndoCoupling`.
- `GamePresentationProjector`'s hand arm: a legacy in a hand is named to the hand's holder alone. The amended R16 relies on it as it is.
- The first-game Setup order: every seat's draw before the first pawn, each keep right after that player's pawn.
- `SetupLegacyFixture.drawn`: seat k's pair is `legacyOrder.slice(2k, 2k + 2)` in turn order. The deal produces exactly this when the deck is long enough.

---

### Task 1: Deal first, keep from the record

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala` (whole file)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala:20-45` (tree comment), `:119-137` (keeps and first-game draws)
- Test: `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala` (append)
- Test: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala` (new test; rewrite the short-deck test at `:115-145`)
- Test: `src/test/scala/distantlands/application/MatchmakerProjectionSuite.scala` (replace the test at `:33-38`, add one)
- Test: `src/test/scala/distantlands/gameplay/setup/legacies/MatchmakerSuite.scala` (rewrite the test at `:75-107`)
- Docs: the spec, `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md` (R16, line 107), `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md` (ruling 3, line 73), `docs/superpowers/specs/2026-10/2026-10-06-undo-design.md` ("Consequences the playtest should expect")

**Interfaces:**
- Produces: `LegacyKeep.deal(participants: Vector[FirstGameParticipant]): Operation`, `LegacyKeep.keep(participant: FirstGameParticipant): Operation` (same signature as today), `LegacyKeep.dealKey(owner: PlayerId): DeferralKey`.
- Removes: `LegacyKeep.step`, `LegacyKeep.draw`. Only `SetupProcedure` calls them.
- Consumes: `Deferred.resolve(key)(f)` and `Deferred.held` from `distantlands.gameplay.powerresolver.Deferred`; `Defer(key, ops)` from `distantlands.model`; `Draw(player, cards, source, destination, position)` from `distantlands.model` (`CardOperation.scala:185`).

- [ ] **Step 1: Write the failing undo acceptance tests**

Append to `UndoBarrierPlaySuite` (inside the class). Add imports:
`distantlands.gameplay.setup.{LegacyKeep, SetupLegacyFixture}` (merge into the existing `distantlands.gameplay.setup.{...}` import) and `distantlands.testkit.DecisionAnswers` (merge into the existing `distantlands.testkit.{...}` import).

```scala
  /** A later game's Setup parked at p1's keep. The Reliquary is empty, so
    * Imperial Maps asks p1, the Chancellor, first; the default answer
    * takes its first end, and the deal follows. */
  private def laterGameAtFirstKeep(gameId: String)
      : (GameApplicationService, Situation, SetupOrders) =
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val orders = LaterGameFixture.orders(record, p1)
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val atKeep = Situation.start(Situation.journaled(service, catalog,
        repository, gameId))
      .parkedAt(LegacyKeep.decisionId(p1))(GameCommand.Begin(record, orders))
    (service, atKeep, orders)

  /** `player`'s keep of the first legacy dealt to them. */
  private def keepFirst(orders: SetupOrders, player: PlayerId): GameCommand =
    val Vector(kept, returned) = SetupLegacyFixture.drawn(orders, player)
    Commands.resolveWalker(player, TreeDecision(LegacyKeep.decisionId(player),
      DecisionAnswers.keepLegacy(kept, returned)))

  test("a later game's keep can be taken back until the next keeper " +
      "answers, returning to the keep question"):
    val (service, atKeep, orders) = laterGameAtFirstKeep("keep-undo")
    val kept = atKeep.parkedAfter(keepFirst(orders, p1))
    val undone = accepted(service.handle("keep-undo", kept.nextSequence,
      Commands.undo(p1)))
    assertEquals(undone.state, atKeep.state)

  test("once the next keeper answers, the earlier keep cannot be taken " +
      "back"):
    val (service, atKeep, orders) = laterGameAtFirstKeep("keep-barrier")
    val second = atKeep.parkedAfter(keepFirst(orders, p1),
      keepFirst(orders, p2))
    nothingToUndo(service, "keep-barrier", second.nextSequence, p1)
```

If `SetupOrders` is not in scope through `distantlands.model._`, find its package with `grep -rn "final case class SetupOrders" src/main/scala` and import it.

- [ ] **Step 2: Run the undo tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.application.UndoBarrierPlaySuite"`
Expected: the first new test FAILS with `expected a Right, got Left(CommandRejected(NothingToUndo))`, because p1's keep answer runs on into p2's draw. The second new test passes (it already holds today). Every other test passes.

- [ ] **Step 3: Write the failing deal and Matchmaker tests**

In `LegacySetupSuite`, add after the test "in a first game every seat draws its two legacies before the first pawn ..." (ends near line 199):

```scala
  test("in a later game every seat holds its dealt pair when the first " +
      "keep is asked (setup keep undo, ruling 1)"):
    val first = seats.head.playerId
    val laterOrders = LaterGameFixture.orders(later, first)
    val atKeep = Situation.start(Situation.rules(catalog))
      .parkedAt(LegacyKeep.decisionId(first))(
        GameCommand.Begin(later, laterOrders))
    seats.foreach(seat => assertEquals(
      atKeep.ready.game.current.legacyHands.getOrElse(seat.playerId,
        Vector.empty), SetupLegacyFixture.drawn(laterOrders, seat.playerId),
      seat.playerId.value))
```

In `MatchmakerProjectionSuite`, replace the test "before p2's keep is reached, nobody identifies p2's draw (R16)" (lines 33-38) with these two tests:

```scala
  test("from the deal on, the holder alone identifies p2's dealt pair " +
      "(setup keep undo, ruling 8, amending R16)"):
    val at = parkedAt(LegacyKeep.decisionId(p1))
    drawn.foreach { legacy =>
      assert(presentation.identifiesAt(at.ready, Some(p3), legacy), legacy)
      Vector(Some(p2), Some(p1), None).foreach(viewer =>
        assert(!presentation.identifiesAt(at.ready, viewer, legacy),
          s"$viewer $legacy"))
    }

  test("the holder's keep for p1 offers only p1's dealt pair, though the " +
      "holder holds every pair"):
    val at = parkedAt(LegacyKeep.decisionId(p1))
    val p1Pair = SetupLegacyFixture.drawn(orders, p1)
    assertEquals(at.ready.game.current.legacyHands.getOrElse(p3,
      Vector.empty).toSet, (p1Pair ++ drawn ++
        SetupLegacyFixture.drawn(orders, p3)).toSet)
    assertEquals(projector.project("match", LoadedGame(at.state, 1L), p3)
      .walkerDecision.flatMap(_.query).map(_.offeredOptions.map(_.id)),
      Some(p1Pair.map(_.value)))
```

Also update the suite's doc comment (lines 13-15) to:

```scala
/** Who sees a draw Matchmaker's holder keeps (spec ruling 13; R16, amended
  * by setup keep undo ruling 8): the holder, and not the owner, from the
  * deal on; the kept legacy, its owner alone once kept; the returned one,
  * nobody. p3 holds Matchmaker. */
```

- [ ] **Step 4: Run the new tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.gameplay.setup.LegacySetupSuite distantlands.application.MatchmakerProjectionSuite"`
Expected:
- "in a later game every seat holds its dealt pair ..." FAILS: at p1's keep only p1's pair has been drawn.
- "from the deal on, the holder alone identifies p2's dealt pair ..." FAILS: p2's pair is still in the deck.
- "the holder's keep for p1 offers only p1's dealt pair ..." FAILS on the hand assertion: the holder holds only p1's pair.
- Every other test passes.

- [ ] **Step 5: Rewrite `LegacyKeep`**

Replace the whole of `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala` with:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.Deferred
import distantlands.gameplay.powers.{LegacyDraws, PowerAnswers}
import distantlands.gameplay.setup.legacies.{Matchmaker, MatchmakerCard,
  SetupLegacyRules}
import distantlands.model._

/** Foundation III's Quiet Ambitions keep in Setup's walk: each player draws
  * two legacies from the legacy deck, keeps one facedown and returns the
  * other to the bottom (Legacies design, slice 2; catalog batch 13, ruling
  * 6). Split from `SetupProcedure`, which calls [[deal]] once and then
  * [[keep]] for every seat. A later game deals and keeps while Foundation
  * III resolves; a first game deals before the first pawn and keeps in
  * each player's turn, after their pawn (catalog batch 13, rulings 3 and
  * 8; setup keep undo design, rulings 1 and 7). Matchmaker's holder
  * answers every other player's keep (Legacy Setup powers ruling 13;
  * R16). */
object LegacyKeep:
  def decisionId(player: PlayerId): String =
    s"setup.legacy-choice.${player.value}"

  /** The key the deal records `owner`'s dealt pair under, which `owner`'s
    * keep reads and takes (setup keep undo design, "The deal"). */
  def dealKey(owner: PlayerId): DeferralKey =
    DeferralKey(s"setup.legacy-deal.${owner.value}")

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p2 kept a legacy". It names no card: the kept legacy is dormant, and
    * only its owner may know it (Legacies ruling 1). */
  val keptLegacy: NoteKey = notes.key("kept-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy")))
  /** The lines the keep writes, on Setup's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Quiet Ambitions' deal: every seat in `participants`' order draws the
    * next 2 legacies from the top of the deck into its keeper's hand, before
    * any keep (setup keep undo design, rulings 1 to 3). The keeper is the
    * seat itself or Matchmaker's holder (Legacy Setup powers ruling 13),
    * and a hand is private to its holder, so only the keeper sees the
    * draw. A short deck gives the later seats fewer, and a seat it cannot
    * reach draws none. Each draw is recorded under [[dealKey]] first, then
    * run, so [[keep]] reads the seat's own pair however the deck and the
    * keeper's hand change before it. Public Ambitions deals nothing. */
  def deal(participants: Vector[FirstGameParticipant]): Operation =
    Sequence(participants.map(dealTo))

  /** One seat's part of [[deal]]: its draw, read off the deck when the step
    * is reached, after every earlier seat's. */
  private def dealTo(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    Branch((state, _) =>
      if !quiet(state) then Vector.empty
      else LegacyDraws.top(state, LegacyDeal.QuietDraw) match
        case Vector() => Vector.empty
        case pair =>
          val keeper = SetupLegacyRules.keeper(state, owner)
          val draw = Draw(keeper, pair, Location.Deck(CardDeck.Legacy),
            Location.Hand(keeper))
          Vector(BuildOps((_, _) => Right(Vector[CoreOperation](
            Defer(dealKey(owner), Vector(draw)), draw)), required = true)))

  /** The keep of the pair [[deal]] recorded for this player: the keeper
    * sorts the legacies into Keep, one, and Discard, the rest. A seat dealt
    * one legacy keeps it without a question, and a seat dealt none is asked
    * nothing (R9). A walk resumed at the question finds the same pair still
    * recorded. */
  def keep(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    val lineage = participant.lineageId
    val decisionId = LegacyKeep.decisionId(owner)
    Deferred.resolve(dealKey(owner))((state, _, held) =>
      val keeper = SetupLegacyRules.keeper(state, owner)
      dealt(held) match
        case Vector() => Vector.empty
        case Vector(only) => Vector(
          BuildOps((_, _) => Right(Vector(keptMove(only, keeper, lineage))),
            required = true),
          keptNote(keeper, owner))
        case drawn => Vector(
          LegacyDraws.pick(decisionId, keeper, drawn, LegacyDraws.Pick.keep,
            heading =
              if keeper == owner then "Keep a legacy"
              else s"Matchmaker: keep a legacy for ${owner.value}",
            confirmLabel = "Keep Legacy"),
          BuildOps(settled(lineage, keeper, drawn, decisionId),
            required = true),
          keptNote(keeper, owner)))

  /** The legacies the recorded draws took, in draw order. */
  private def dealt(held: Vector[CoreOperation]): Vector[LegacyId] =
    held.flatMap {
      case draw: Draw => draw.cards.collect { case id: LegacyId => id }
      case _ => Vector.empty
    }

  /** Whether Foundation III shows Quiet Ambitions, whose keep draws. */
  private def quiet(state: ReadyGame): Boolean =
    state.game.campaign.foundations.get(FoundationNumber.III)
      .forall(_.face == FoundationFace.Normal)

  private def keptNote(keeper: PlayerId, owner: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ => Some(
      if keeper == owner then keptLegacy.sourceless(NoteArg.Player(owner))
      else Matchmaker.keptFor.sourceless(NoteArg.Player(keeper),
        NoteArg.Player(owner), NoteArg.Card(MatchmakerCard.id))))

  /** The kept legacy, from the keeper's hand to the owner's lineage, with
    * no face: dormant. */
  private def keptMove(legacy: LegacyId, keeper: PlayerId,
      lineage: LineageId): Move =
    Move(Piece.Card(legacy), PositionedLocation(Location.Hand(keeper)),
      PositionedLocation(Location.Legacies(lineage)))

  private def settled(lineage: LineageId, keeper: PlayerId,
      drawn: Vector[LegacyId], decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      LegacyDraws.picked(pending.answered, decisionId, LegacyDraws.Pick.keep)
        .filter(drawn.contains).map { kept =>
          keptMove(kept, keeper, lineage) +: drawn.filterNot(_ == kept)
            .map(id => Move(Piece.Card(id),
              PositionedLocation(Location.Hand(keeper)),
              PositionedLocation.legacyDeckBottom))
        }.toRight(PowerAnswers.missing(decisionId))
```

Notes for the implementer:
- `dealTo` keeps today's `draw` shape (one `Branch` per seat reading the deck top when reached), so each seat's `Draw` takes the cards then on top. Do not merge the seats into one `BuildOps`: a single batch would read the deck once, before any draw ran.
- The `Defer` is recorded before the `Draw` it holds, the recording form `Deferred` documents.
- If the compiler rejects `case id: LegacyId` (for example because `CardId` is not a sealed union), use the pattern `SetupProcedure` uses for denizens (`collect { case id: DenizenId => id }`) as the model and report what changed.

- [ ] **Step 6: Call the deal from `SetupProcedure`**

In `SetupProcedure.tree`, replace lines 119-137 (from the comment "A first game draws every seat's legacies before the first pawn and" down to `participants.map(playerStep(catalog, ready, _, firstGame))`) with:

```scala
    // Every seat's legacies are dealt before the first keep (setup keep
    // undo design, ruling 1). A first game deals before the first pawn and
    // keeps in each player's turn, after their pawn (CR p. 7); a later game
    // deals and keeps while Foundation III resolves, every seat before any
    // pawn (CR "Later games", step 5; catalog batch 13, ruling 3). An
    // exile-only game resolves its stored foundations, so it keeps in
    // Foundation III, unless its wiped baseline carries no history: then
    // it keeps after each pawn, as it places its pawns, as a first game
    // does (R41). The baseline never changes in Setup, so a resume
    // rebuilds the same tree.
    val firstGame = EmpireSetupRules.isFirstGame(catalog,
      ready.game.campaign.baseline)
    val deal = LegacyKeep.deal(participants)
    val keeps =
      if firstGame then Vector.empty
      else deal +: participants.map(LegacyKeep.keep)
    // A first game's deal comes before the first pawn, so each player
    // places holding their two (CR p. 7).
    val firstGameDeal = if firstGame then Vector(deal) else Vector.empty
    val steps = firstGameDeal ++
      participants.map(playerStep(catalog, ready, _, firstGame))
```

Before editing, open the file and confirm lines 119-137 hold exactly that span; the old code there defines `firstGame`, `keeps`, `firstGameDraws` and `steps`.

Then update the tree comment at lines 31-38 so it matches:

```scala
 *   Sequence(                                            // a later game's
 *     LegacyKeep.deal                                    // every seat's draw,
 *       Branch(Defer(deal), Draw(2 into the keeper's     // recorded, in turn
 *         hand)), ...                                    // order
 *     Deferred.resolve(deal)(Decide(legacy-choice,       // LegacyKeep.keep, per
 *       keeper), ...), ...)                              // seat (Foundation III)
 *   Sequence(Vector.empty)                               // window = SetupWorldDeck
 *   LegacyKeep.deal                                      // a first game's, every
 *                                                        // seat in turn order
```

Search the rest of the file's doc comment for any other line naming `LegacyKeep.step` or `LegacyKeep.draw` and reword it the same way: `grep -n "LegacyKeep\.\(step\|draw\)" src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` must print nothing.

- [ ] **Step 7: Run the targeted tests**

Run: `./sbtw "root/testOnly distantlands.application.UndoBarrierPlaySuite distantlands.gameplay.setup.LegacySetupSuite distantlands.application.MatchmakerProjectionSuite"`
Expected: all new tests PASS. One existing test FAILS: `LegacySetupSuite` "a short deck draws in keep order, and a later keep can draw a legacy an earlier one returned ...". Step 8 rewrites it.

- [ ] **Step 8: Rewrite the two short-deck tests for the deal**

In `LegacySetupSuite`, replace the whole short-deck test (lines 115-145, from `test("a short deck draws in keep order` to the `kept.toSet` assertion) with:

```scala
  test("a short deck deals in turn order: a later seat gets fewer, and no " +
      "seat draws a legacy an earlier one returned (R9; setup keep undo, " +
      "ruling 2)"):
    // Box lineages hold every legacy but 3, so the derived deck is short.
    // A later game with a Chancellor keeps them; a first game played
    // exile-only would wipe them back into the deck (R40).
    val held = catalog.legacies.map(_.id).dropRight(3)
    val boxed = held.grouped(12).zipWithIndex.map((ids, n) => LineageState(
      LineageId(s"boxed-$n"), None, Role.Exile,
      ids.map(LegacyState(_, active = true)), Vector.empty)).toVector
    val short = later.copy(lineages = later.lineages ++ boxed)
    val shortOrders = LaterGameFixture.orders(short, seats.head.playerId)
      .copy(legacyOrder = LegacyDeal.deck(catalog, short))
    val Vector(a, b, c) = shortOrders.legacyOrder
    val started = accepted(GameStartRules.evolve(catalog, short, shortOrders))
    val Vector(first, second, third) = seats
    // The deal gives the first seat a and b, the second c, and the third
    // nothing. The default answer keeps the first option: the first keep
    // takes a and returns b to the bottom; the second keeps c unasked; the
    // third is asked nothing.
    val setup = walked(started)
    assertEquals(legacyQuestions(setup.asked).map(_.owner),
      Vector(first.playerId))
    def holding(seat: FirstGameParticipant) =
      setup.ready.game.campaign.lineages(seat.lineageId).legacies
    assertEquals(holding(first), Vector(LegacyState(a, active = false)))
    assertEquals(holding(second), Vector(LegacyState(c, active = false)))
    assertEquals(holding(third), Vector.empty)
    assertEquals(setup.ready.game.current.commonCards.legacyDeck, Vector(b))
    val kept = notes(setup.events).filter(_.key ==
      LegacyKeep.keptLegacy.name).map(_.args)
    assertEquals(kept.toSet, Vector(first, second).map(seat =>
      Vector[NoteArg](NoteArg.Player(seat.playerId))).toSet)
```

In `MatchmakerSuite`, replace the test "a player who drew one legacy keeps it with no question" (lines 75-107) with:

```scala
  test("a player dealt one legacy keeps it with no question"):
    // Box lineages hold every legacy but Matchmaker and 3 others, so the
    // derived deck is short. The deal gives p1 two, p2 the one left and
    // p3 none (Legacies R9; setup keep undo, ruling 2). Matchmaker's
    // holder p3 answers p1's question and keeps p2's single legacy
    // unasked. A later game with a Chancellor keeps them; a first game
    // played exile-only would wipe them back into the deck (R40).
    val held = catalog.legacies.map(_.id).filterNot(_ == MatchmakerCard.id)
      .dropRight(3)
    val boxed = held.grouped(12).zipWithIndex.map((ids, n) => LineageState(
      LineageId(s"boxed-$n"), None, Role.Exile,
      ids.map(LegacyState(_, active = true)), Vector.empty)).toVector
    val later = LaterGameFixture.chronicle(l1, l2)
    val short = SetupLegacyFixture.holding(
      later.copy(lineages = later.lineages ++ boxed), l3, Role.Exile,
      MatchmakerCard.id -> true)
    val shortOrders = LaterGameFixture.orders(short, p1).copy(
      legacyOrder = LegacyDeal.deck(catalog, short))
    assertEquals(shortOrders.legacyOrder.size, 3)
    val ready = accepted(GameStartRules.evolve(catalog, short, shortOrders))
    val setup = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.empty)
    assertEquals(setup.asked.filter(_.decisionId.startsWith(
      "setup.legacy-choice.")).map(decide => decide.decisionId ->
      decide.owner), Vector(LegacyKeep.decisionId(p1) -> p3))
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, setup.events).map(_.text)
    Vector("p1", "p2").foreach(owner => assert(said.contains(
      s"p3 kept a legacy for $owner with ${MatchmakerCard.id.value}"), said))
    assert(!said.contains("p3 kept a legacy"), said)
```

`said` holds whole lines, so `said.contains("p3 kept a legacy")` checks for p3's own line exactly; the Matchmaker lines are longer and do not match it. Check that by reading `NoteText.said` before relying on it; if `said` is not a collection of whole lines, assert instead that no line equals "p3 kept a legacy".

- [ ] **Step 9: Run the Setup, Matchmaker and undo suites**

Run: `./sbtw "root/testOnly distantlands.gameplay.setup.* distantlands.application.UndoBarrierPlaySuite distantlands.application.MatchmakerProjectionSuite distantlands.application.LaterGameSetupChoicesSuite distantlands.application.GameApplicationServiceProjectionSuite distantlands.server.*"`
Expected: PASS. If a suite fails, read the failure:
- A test that asserts *when* a later game's draw happens (a hand checked before a keep, a deck size between keeps) is expected to need the deal's new timing. Update its expectation and its comment to the deal, citing setup keep undo ruling 1.
- A golden log under `src/test/resources/gamelog/` should not change, because the deal writes no line. If one does, stop and report the diff instead of editing the golden file.
- Any first-game failure is a bug in this task: fix the code, never the first-game test.

- [ ] **Step 10: Update the docs**

1. In the spec `docs/superpowers/specs/2026-11/2026-11-21-setup-keep-undo-design.md`, add after ruling 7:

```markdown
8. **Matchmaker's holder sees every dealt pair from the deal on.** The
   deal puts every other seat's dealt pair in the holder's hand at once,
   and a legacy in a hand is named to the hand's holder. The holder may
   therefore choose one seat's keep knowing another seat's pair. This
   amends Legacy Setup powers implementation ruling R16, under which the
   holder saw another player's draw only while that player's keep was
   pending. The owner still never sees their dealt pair, the kept legacy
   is still named to its owner alone, and the returned one to nobody. The
   deal still comes after World Crafter's and Populist's choices, which
   R16 kept the holder from informing.
```

   In the same spec's "Testing" section, replace the "A short deck" bullet with:

```markdown
- A short deck, in a later game: 3 legacies for three seats. The first
  seat is asked over 2, the second keeps their one with no question, and
  the third is asked nothing.
- Matchmaker, from the deal on: the holder identifies p2's dealt pair at
  p1's keep, and the owner does not (ruling 8).
```

2. In `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`, at the end of the R16 entry (the paragraph starting at line 107), add a new paragraph:

```markdown
  *Amended 2026-11-21:* the holder now sees every other player's dealt
  pair from the deal on, before any keep
  ([setup keep undo design](2026-11-21-setup-keep-undo-design.md),
  ruling 8).
```

3. In `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md`, at the end of ruling 3 (the item starting at line 73, ending "... and Explore needs Foundation I altered."), add a sentence on its own line, indented as the ruling's text:

```markdown
   *Amended 2026-11-21:* every seat's legacies are drawn before the first
   keep ([setup keep undo design](2026-11-21-setup-keep-undo-design.md),
   ruling 1).
```

4. In `docs/superpowers/specs/2026-10/2026-10-06-undo-design.md`, add a bullet to the end of the list under "Consequences the playtest should expect:":

```markdown
- A later game's legacy keep can be undone until the next keeper answers;
  the last keep cannot when Secret Society or Seer acts on the world
  deck ([setup keep undo design](../2026-11/2026-11-21-setup-keep-undo-design.md)).
```

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala src/test/scala/distantlands/application/MatchmakerProjectionSuite.scala src/test/scala/distantlands/gameplay/setup/legacies/MatchmakerSuite.scala docs/superpowers/specs/2026-11/2026-11-21-setup-keep-undo-design.md docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md docs/superpowers/specs/2026-10/2026-10-06-undo-design.md
```

Add any other test file Step 9 changed, by path. Then:

```bash
git commit -F - <<'EOF'
feat(setup): deal every seat's legacies before the first keep

A later game's keep ran on into the next seat's legacy draw in the same
command batch, so every keep but the last was an undo barrier. The deal
now draws every seat's pair first and records each with Defer; each keep
reads its own pair back, holds no hidden outcome, and can be undone
until the next keeper answers. A first game uses the same deal and keep.

Amends catalog batch 13 ruling 3 and Legacy Setup powers R16.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 2: Full gate and merge

**Files:** none changed unless the gate fails.

- [ ] **Step 1: Run the full gate**

Run, in the background if your tool allows: `./sbtw root/test frontend/test`
Expected: both report `Failed 0, Errors 0`. Record the two `Passed: Total` counts.

If anything fails, apply Task 1 Step 9's rules, rerun the gate, and commit the fix by path with its own message.

- [ ] **Step 2: Check the main checkout before merging**

Run: `git -C /Users/roman/projects/distantlands status --short` and `git -C /Users/roman/projects/distantlands branch --show-current`
Expected: clean, on `0.2.0-alpha`. Other sessions share that checkout: if it is dirty or on another branch, stop and report.

- [ ] **Step 3: Merge**

```bash
git -C /Users/roman/projects/distantlands merge --no-ff setup-keep-undo -m "Merge branch 'setup-keep-undo' into 0.2.0-alpha"
```

- [ ] **Step 4: Remove the worktree and branch**

From the worktree, remove the two symlinks, then from the main checkout remove the worktree and delete the branch:

```bash
rm /Users/roman/projects/distantlands/.worktrees/setup-keep-undo/.tooling /Users/roman/projects/distantlands/.worktrees/setup-keep-undo/node_modules
```

```bash
git -C /Users/roman/projects/distantlands worktree remove .worktrees/setup-keep-undo
```

```bash
git -C /Users/roman/projects/distantlands branch -d setup-keep-undo
```

Report the merge commit hash, the gate counts, and any test whose expectation Step 9 changed, with the reason.
