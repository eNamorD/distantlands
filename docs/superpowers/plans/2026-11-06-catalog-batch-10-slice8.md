# Catalog Batch 10 Slice 8: Recover Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a Recover take the relic deck's top card at difficulty 5 when Drowned Library (E13, ruined) is picked, and implement Drowned Library.

**Architecture:**
- **No new primitive.** The fact "this Recover takes from the relic deck" is Drowned Library's pick, which the walk's answers already record. Answers survive a park and replay, so every reader works it out from them.
  - The card object `UndergroundLibraryCard` moves down beside `RecoverProcedure`, as `ActingTroupeCard` moved into `oathdigital.gameplay` in slice 7.
  - `RecoverProcedure.fromRelicDeck(answered)` is `ModifierPicks.isPicked(answered, RecoverModifierSelection, E13)`. `RecoverProcedure.relicDeckDifficulty` is 5.
  - Recover names this one card, as `EffectiveSuit` names Acting Troupe. A general seam waits for a second card that sets where a Recover's relic comes from.
- **`RecoverProcedure` reads the pick.**
  - `difficulty(catalog, ready, actor, answered)` is 5 once Drowned Library is picked, else the site's.
  - `succeeded`, the `Repeat` guard and `rollFeedback` read it. The tree's closures get the answers from the walk. `rollFeedback` gets them from the parked walk, `walkerPending`.
  - With the pick, the relic decision offers the relic deck as one option, `DecisionOption.Deck(Deck(Relic))`, so the relic stays unseen. The move takes the deck's top card.
- **The start refusal moves after the modifiers.**
  - Today `recoveryAt` refuses a site with no Recover difficulty when the tree is built. Half the sites have none, and Drowned Library may stand at one.
  - The refusal moves into `afterLoop`. With no difficulty, the `Repeat` runs no round and `afterLoop` declares a `BuildOps` that refuses with the same violation.
  - No node is added before the relic decision, so no existing walker path shifts.
  - "Start" and the Act menu already judge the tree after the picks (`ModifierChoice.refusal`, `QueuedStart.priced`). So a no-difficulty site refuses as before unless Drowned Library is picked.
- **Drowned Library is a `ContributingPower`, written like `CatacombsContribution`.** `SelectedModifier.card` names a denizen or a relic, never an edifice face.
  - **Picking it:** the secret is placed on the edifice.
  - **Its effect:** none of its own. It hooks no window, as Acting Troupe hooks none; the pick is the whole effect, and the Recover reads it.
  - **Selectable:** only with the user's pawn at its site and a relic in the deck.
- **Edifice faces in the modifier decision.** One edifice id names both faces, so `PowerAccess.locate` finds E13 whichever face is up.
  - A new capability trait, `OnEdificeFace`, names the face a power is printed on.
  - `ModifierChoice` lists such a power only while that face is up, as `RuleSourceIndex` lists an edifice's powers by its side.
  - So an intact Underground Library never lists Drowned Library.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Catalog corrections" (Drowned Library carries the Recover icon)
- "Readings" (Drowned Library)
- "Slice 8: Recover source"
- "Testing"

Card text (`UndergroundLibrary.scala`, `UndergroundLibraryCard.ruined`), 1 secret: "If your pawn is at this site, you may recover the top card of the relic deck instead. Its recover difficulty is 5."

**Decisions this plan takes** (Task 2 writes each into the spec):
- **No scratch primitive** (owner, 2026-11-06). The spec's `RecoverFromRelicDeck` primitive would record a fact the walk already records: the pick. Recover reads the pick instead, and names Drowned Library's card.
- **The refusal that moves.** The spec's slice 8 body says Recover may refuse "when the site holds no relic". It never does: a Recover with no relic to take is a legal wasted action. The refusal at the start is for a site with no Recover difficulty, and that is the one that moves after the modifier loop.
- **The relic stays unseen.** The relic decision offers the relic deck, not the relic. The existing line, "Recovered {relic} at {site}", names the relic only to a viewer who may identify it.
- **Hidden faces.** An edifice's power is listed in a modifier decision only while its face is up.
- **Other Recover rules still apply.** Relic Patrols and Bandit Patrols refuse a Recover at the site whatever its source. The Recover is still made at the pawn's site, which is the Library's.

This is the eighth of 14 slice plans, written against `0.2.0-alpha` at `0b419ab6`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-8` on branch `batch-10-slice-8`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports and unused values fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains. The files are `src/test/resources/notes/declared-keys.txt` and `src/test/resources/powers/unimplemented.txt`.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").
- **Modifier icon:** Drowned Library's printed power gains `modifier = Some(ModifierMark.Action(MajorActionType.Recover))` (spec, "Catalog corrections").

---

### Task 1: Recover reads Drowned Library's pick

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/recover/UndergroundLibraryCard.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/UndergroundLibrary.scala` (the card moves out)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import)
- Modify: `src/test/scala/distantlands/gameplay/powers/action/UndergroundLibrarySuite.scala` (import)
- Modify: `src/main/scala/distantlands/gameplay/actions/recover/RecoverProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/RecoverProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/RecoverEligibilitySuite.scala`

**Interfaces:**
- Produces:
  - `object UndergroundLibraryCard` in package `oathdigital.gameplay.actions.recover`, unchanged but for the ruined power's modifier icon.
  - `RecoverProcedure.relicDeckDifficulty: Int = 5`.
  - `RecoverProcedure.fromRelicDeck(answered: Vector[Answered]): Boolean`.
  - `RecoverProcedure.difficulty(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId, answered: Vector[Answered]): Option[Int]`.
  - `RecoverProcedure.noDifficulty: String = "site has no Recover Difficulty"`.
  - `RecoverProcedure.relicDeckOption: DecisionOptionRef.Deck`, which is `DecisionOptionRef.Deck(CardDeck.Relic)`.

- [ ] **Step 1: Write the failing tests**

In `RecoverProcedureSuite.scala`:
- Add `import oathdigital.gameplay.actions.recover.UndergroundLibraryCard` beside the `RecoverProcedure` import, and `import oathdigital.gameplay.cards.DeepWoodsSite` beside the `FairIsleSite` import.
- Add these members after `recoverBoard`:

```scala
  /** The modifier decision's answer that picked Drowned Library. */
  private val pickedDrownedLibrary: Answered = Answered("recover.modifiers",
    ChooseOneAnswer(DecisionOptionRef.Edifice(UndergroundLibraryCard.id)),
    actor)

  /** One shield and a blank: with a two-shield roll before it, 5. */
  private val oneShield: Vector[DieFace] =
    Vector(DefenseDieFace.OneShield, DefenseDieFace.Blank)

  /** Deep Woods has no Recover difficulty. */
  private val noDifficultySite = DeepWoodsSite.id
```

Add these tests after the test "a successful Recover with no facedown relic finishes without a relic decision". A walk seeded with `pickedDrownedLibrary` reads as one whose modifier decision picked Drowned Library; the walk keeps the seed in the answers it parks with.

```scala
  test("Deep Woods has no Recover difficulty, as the tests assume"):
    assertEquals(RecoverRules.difficulty(catalog, noDifficultySite), None)

  test("a Recover at a site with no Recover difficulty is refused after " +
      "its modifiers, before any Supply is spent"):
    val ready = Table.start.pawn(actor, at = noDifficultySite).ready
    val tree = accepted(RecoverProcedure.afterModifiers(catalog, ready, actor))
    assertEquals(ProcedureWalker.advance(ready, tree, None, noPowers,
        scripted(highRoll)).left.toOption,
      Some(OathViolation.RecoverUnavailable(RecoverProcedure.noDifficulty)))

  test("with Drowned Library picked, a Recover needs 5 and takes the relic " +
      "deck's top card facedown"):
    val ready = recoverBoard.ready
    val top = ready.game.current.commonCards.relicDeck.head
    val tree = accepted(RecoverProcedure.afterModifiers(catalog, ready, actor))
    assertEquals(RecoverProcedure.difficulty(catalog, ready, actor,
      Vector(pickedDrownedLibrary)), Some(RecoverProcedure.relicDeckDifficulty))

    // Four shields reach Fair Isle's 3 but not 5: the choice is asked.
    val choiceWalk = expectParked(
      ProcedureWalker.advance(ready, tree, None, noPowers,
        scripted(highRoll), seed = Vector(pickedDrownedLibrary)),
      Vector("1", "0", "2", "0"))
    val stateAtChoice = applyRecorded(ready, choiceWalk.events)
    val continue = Answered(RecoverProcedure.choiceDecisionId,
      ChooseOneAnswer(DecisionOptionRef.Button("continue")), actor)
    val relicWalk = expectParked(
      ProcedureWalker.resolve(stateAtChoice, tree, choiceWalk.tree, continue,
        noPowers, scripted(oneShield)),
      Vector("2", "0"))
    val stateAtRelic = applyRecorded(stateAtChoice, relicWalk.events)

    // The deck is offered, never the relic, and the site's relic is not.
    val decide = present(ProcedureWalker.parkedDecide(stateAtRelic, tree,
      relicWalk.tree, noPowers), "the relic decision")
    assertEquals(decide.query, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Deck(RecoverProcedure.relicDeckOption)),
      heading = Some("Take a relic"), confirmVerb = Some("Take")))

    val taking = Outcomes.finished(ProcedureWalker.resolve(stateAtRelic,
      tree, relicWalk.tree, Answered(RecoverProcedure.relicDecisionId,
        ChooseOneAnswer(RecoverProcedure.relicDeckOption), actor),
      noPowers))
    assertEquals(RecordedOps.of(recordedSteps(taking.events, notes = 1)(1)),
      Vector[CoreOperation](Move(Piece.Card(top),
        PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Top),
        PositionedLocation(Location.PlayArea(actor)),
        resultingOrientation = Some(Orientation.FaceDown),
        by = Some(actor))))
    // The recovered line names the deck's top card and the pawn's site.
    assertEquals(taking.events.collect { case LogNoted(_, note, _) =>
      note.args }, Vector(Vector(NoteArg.Card(top), NoteArg.Site(recoverSite))))
    val finalState = taking.treeless
    assertEquals(Look(finalState).player(actor).relics
      .map(held => held.id -> held.orientation),
      Vector(top -> Orientation.FaceDown))
    assertEquals(finalState.game.current.commonCards.relicDeck,
      ready.game.current.commonCards.relicDeck.tail)
    assertEquals(finalState.game.current.map.sites(recoverSite).relics,
      Vector(siteRelic))

  test("with Drowned Library picked, a site with no Recover difficulty is " +
      "not refused"):
    val ready = Table.start.pawn(actor, at = noDifficultySite).ready
    val tree = accepted(RecoverProcedure.afterModifiers(catalog, ready, actor))
    expectParked(ProcedureWalker.advance(ready, tree, None, noPowers,
      scripted(highRoll), seed = Vector(pickedDrownedLibrary)),
      Vector("1", "0", "2", "0"))

  test("with Drowned Library picked and the relic deck empty, a successful " +
      "Recover finishes without a relic decision"):
    val ready = recoverBoard.relicDeckEmptied.ready
    val tree = accepted(RecoverProcedure.afterModifiers(catalog, ready, actor))
    val choiceWalk = expectParked(ProcedureWalker.advance(ready, tree, None,
      noPowers, scripted(highRoll), seed = Vector(pickedDrownedLibrary)),
      Vector("1", "0", "2", "0"))
    val stateAtChoice = applyRecorded(ready, choiceWalk.events)
    val done = Outcomes.finished(ProcedureWalker.resolve(stateAtChoice, tree,
      choiceWalk.tree, Answered(RecoverProcedure.choiceDecisionId,
        ChooseOneAnswer(DecisionOptionRef.Button("continue")), actor),
      noPowers, scripted(oneShield)))
    assertEquals(Look(done.treeless).player(actor).relics, Vector.empty)

  test("the roll feedback shows 5 while a walk that picked Drowned Library " +
      "is parked"):
    def target(ready: ReadyGame) = RecoverProcedure.rollFeedback(catalog,
      ready, actor, RecoverProcedure.choiceDecisionId).flatMap(_.target)
    val parkedWith: Vector[Answered] => ReadyGame = answered =>
      recoverBoard.ready.updateCurrent(_.copy(walkerPending =
        Some(PendingTree(Vector("1", "0", "2", "0"), answered))))
    assertEquals(target(parkedWith(Vector.empty)), Some(difficulty))
    assertEquals(target(parkedWith(Vector(pickedDrownedLibrary))),
      Some(RecoverProcedure.relicDeckDifficulty))
```

In `RecoverEligibilitySuite.scala`, add `import oathdigital.gameplay.cards.DeepWoodsSite` and this test:

```scala
  test("Recover is not available at a site with no Recover difficulty"):
    assertEquals(RecoverRules.difficulty(catalog, DeepWoodsSite.id), None)
    assert(!recoverAvailable(Table.start.pawn(p1, at = DeepWoodsSite).ready))
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.RecoverProcedureSuite oathdigital.gameplay.RecoverEligibilitySuite"`
Expected: compile failure, "value UndergroundLibraryCard is not a member of oathdigital.gameplay.actions.recover" (and `relicDeckDifficulty`, `noDifficulty`, `relicDeckOption`).

- [ ] **Step 3: Move the card**

Create `src/main/scala/distantlands/gameplay/actions/recover/UndergroundLibraryCard.scala` holding `object UndergroundLibraryCard` exactly as it stands in `UndergroundLibrary.scala`, with two changes:
- The ruined face's `PrintedPower` gains `modifier = Some(ModifierMark.Action(MajorActionType.Recover)),` after its `cost`.
- A doc comment on the object: "Underground Library (E13). The card lives beside the Recover, which reads Drowned Library's pick (`RecoverProcedure.fromRelicDeck`); its powers are `UndergroundLibrary` (intact) and `DrownedLibrary` (ruined)."

Its imports: `oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark, PrintedPower}` and `oathdigital.model._`.

Then:
- In `UndergroundLibrary.scala`, delete the card object, import `oathdigital.gameplay.actions.recover.UndergroundLibraryCard`, and drop the catalog imports nothing uses any more (`ExecutableCatalog` stays: `build` takes it).
- In `UndergroundLibrary`'s doc, replace the last sentence ("The ruined face, Drowned Library, needs a Recover whose source a power sets and is not implemented.") with "The ruined face is [[oathdigital.gameplay.powers.recover.DrownedLibrary]]."
- In `NewFoundations.scala`, add `import oathdigital.gameplay.actions.recover.UndergroundLibraryCard`.
- In `UndergroundLibrarySuite.scala`, add the same import.

- [ ] **Step 4: Read the pick in `RecoverProcedure`**

In `RecoverProcedure.scala`:

1. In the header comment, update the tree and the semantics.
   - The `afterLoop` branch reads:

     ```
     *     Branch(if no difficulty -> Vector(BuildOps(refuse)),
     *       else if success ->
     *       Vector(Decide("recover.relic"),
     *         Sequence(BuildOps(move chosen relic facedown),
     *           Note(recovered))),              // window = RecoverAfterRelic
     *       else if stopped -> Vector(Note(failed)),
     *       else Vector.empty)))                 // succeeded with no relic
     ```

   - The `Repeat` line reads `Repeat(guard = has difficulty && not succeeded && lastChoice != Stop,`.
   - The success bullet ends "reaching [[difficulty]]: 5 once Drowned Library is picked ([[fromRelicDeck]]), else the site's. The site is the actor's pawn site."
   - Add a bullet: "With Drowned Library picked, the relic decision offers the relic deck instead of the site's relics, and the move takes its top card, unseen until recovered."
   - Replace the closing paragraph ("`build` validates the action-specific necessity...") with:

     ```
     * A Recover with no difficulty is refused after its modifier decision,
     * not when the tree is built, since Drowned Library makes any site
     * recoverable: with no difficulty the `Repeat` runs no round, and the
     * last branch refuses. "Start" and the Act menu judge the tree after the
     * picks, so they refuse it too. Generic supply feasibility belongs to
     * `OperationPipeline`; role, foundation state, and relic availability do
     * not gate Recover.
     ```

2. After `startLabel`, add:

```scala
  /** Why a Recover with no difficulty is refused. */
  val noDifficulty: String = "site has no Recover Difficulty"

  /** Drowned Library's difficulty: "Its recover difficulty is 5." */
  val relicDeckDifficulty: Int = 5

  /** Whether the walk's modifier decision picked Drowned Library (E13,
    * ruined), whose Recover takes the relic deck's top card. Read from the
    * answers, which a park keeps and replay rebuilds, as `EffectiveSuit`
    * reads Acting Troupe's pick. */
  def fromRelicDeck(answered: Vector[Answered]): Boolean =
    ModifierPicks.isPicked(answered, PowerWindow.RecoverModifierSelection,
      UndergroundLibraryCard.id)

  /** The relic decision's one option with Drowned Library picked: the deck,
    * so the relic stays unseen until recovered. */
  val relicDeckOption: DecisionOptionRef.Deck =
    DecisionOptionRef.Deck(CardDeck.Relic)

  /** The difficulty of the actor's Recover: Drowned Library's once it is
    * picked, else the actor's pawn site's, if it has one. */
  def difficulty(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, answered: Vector[Answered]): Option[Int] =
    if fromRelicDeck(answered) then Some(relicDeckDifficulty)
    else actorSite(ready, actor).flatMap(RecoverRules.difficulty(catalog, _))
```

3. `rollFeedback` reads the parked walk's answers. Replace `target = actorSite(ready, actor).flatMap(RecoverRules.difficulty(catalog, _))` with:

```scala
      target = difficulty(catalog, ready, actor,
        ready.game.current.walkerPending.fold(Vector.empty[Answered])(
          _.answered)))
```

   Update its doc: "the pool rolled so far and the Recover's difficulty, read with the parked walk's answers".

4. `recoveryAt` no longer refuses on the difficulty:

```scala
  /** The Recover at the actor's site. */
  private def recoveryAt(catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Either[OathViolation, Operation] = for
    siteId <- actorSite(state, actor).toRight(
      OathViolation.PawnSiteMissing(actor))
  yield recovery(catalog, actor, siteId)
```

5. In `recovery`, drop the `difficulty: Int` parameter. Update its doc: "Tree closes only over command-stable actor and site; the difficulty and the relic source are read live, with the walk's answers."
   - `succeeded` reads the difficulty with the answers:

```scala
    def target(ready: ReadyGame, answered: Vector[Answered]): Option[Int] =
      difficulty(catalog, ready, actor, answered)

    def succeeded(ready: ReadyGame, answered: Vector[Answered]): Boolean =
      target(ready, answered).exists(scoreOf(ready) >= _)
```

   - The choice `Branch` in `body` passes them: `Branch((ready, pending) => if succeeded(ready, pending.answered) then Vector.empty else Vector(choiceDecide))`.
   - Keep `relicMove` as it is. Add the deck's move and the options after it:

```scala
    /** The relic deck's top card's move to the actor, facedown. */
    def deckMove(relicId: RelicId): Move = Move(Piece.Card(relicId),
      PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Top),
      PositionedLocation(Location.PlayArea(actor)),
      resultingOrientation = Some(Orientation.FaceDown), by = Some(actor))

    /** What a successful Recover may take, each with the move that takes
      * it: the relic deck's top card, shown as the deck, with Drowned
      * Library picked; else each facedown relic at the actor's live site. */
    def takeable(ready: ReadyGame, answered: Vector[Answered])
        : Vector[(DecisionOption, Move)] =
      if fromRelicDeck(answered) then
        ready.game.current.commonCards.relicDeck.headOption.toVector.map(
          top => DecisionOption.Deck(relicDeckOption) -> deckMove(top))
      else actorFacedownRelics(ready, actor).map(relic =>
        DecisionOption.Relic(DecisionOptionRef.Relic(relic.id)) ->
          relicMove(relic.id))
```

   - `moveRelic` takes the move of the answered option:

```scala
    val moveRelic = BuildOps((ready, pending) =>
      pending.answered.lastOption match {
        case Some(Answered(_, ChooseOneAnswer(ref), _)) =>
          takeable(ready, pending.answered).collectFirst {
            case (option, move) if option.ref == ref =>
              Vector[CoreOperation](move)
          }.toRight(OathViolation.InvalidEventOrder(
            s"${ref.wireId} is not a relic this Recover may take"))
        case _ => Left(OathViolation.InvalidEventOrder(
          "no recovered relic answer is recorded"))
      })
```

   - `recoveredNote` names the deck's top card read before the move. The note's previous step is the move, since a power at `RecoverAfterRelic` appends after the note:

```scala
    /** The relic taken and the site it was recovered at. A site relic is
      * read from the relic decision's answer; the relic deck's top card from
      * the state before the move, the note's previous step, since a power
      * hooked at `RecoverAfterRelic` appends its leaves after the note. The
      * site is the one this tree closed over. */
    val recoveredNote = Note(ProcedureNotes.id(ActionRef.Recover), states =>
      states.answered.reverse.collectFirst {
        case Answered(`relicDecisionId`, ChooseOneAnswer(ref), _) => ref
      }.flatMap {
        case DecisionOptionRef.Relic(relic) => Some(relic)
        case `relicDeckOption` => states.previous.flatMap(
          _._1.game.current.commonCards.relicDeck.headOption)
        case _ => None
      }.map(relic => recovered.sourceless(NoteArg.Card(relic),
        NoteArg.Site(siteId))))
```

   - The `Repeat` guard runs no round without a difficulty:

```scala
    val repeatGuard: (ReadyGame, PendingTree) => Boolean =
      (ready, pending) => target(ready, pending.answered).nonEmpty &&
        !succeeded(ready, pending.answered) && !stopped(pending)
```

   - `afterLoop` refuses first, then offers `takeable`. Keep the long comment above it, and add one paragraph: "With no difficulty the `Repeat` ran no round, and this branch refuses, so the refusal is judged after the modifier decision." Replace the branch body with:

```scala
    val refused = BuildOps((_, _) =>
      Left(OathViolation.RecoverUnavailable(noDifficulty)))

    val afterLoop = Branch((ready, pending) => {
      val options = takeable(ready, pending.answered)
      if target(ready, pending.answered).isEmpty then Vector(refused)
      else if succeeded(ready, pending.answered) && options.nonEmpty then
        Vector(Decide(
          decisionId = relicDecisionId,
          owner = actor,
          query = DecisionQuery.ChooseOne(options.flatMap((option, move) =>
            CardSlots.judge(catalog, ready, actor, option,
              Right(Vector(move)))),
            heading = Some("Take a relic"), confirmVerb = Some("Take"))),
          Sequence(Vector(moveRelic, recoveredNote),
            Some(PowerWindow.RecoverAfterRelic)))
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty
    })
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.RecoverProcedureSuite oathdigital.gameplay.RecoverEligibilitySuite oathdigital.gameplay.CatacombsContributionSuite oathdigital.gameplay.powers.recover.* oathdigital.gameplay.powers.action.UndergroundLibrarySuite oathdigital.application.GameApplicationServiceRecoverSuite oathdigital.gameplay.WalkerReplayDriftSuite"`
Expected: all pass. The existing Recover suites must pass unchanged, since no walker path shifts.

Then run `python3 scripts/check-architecture.py`. `cards.NewFoundations` already imports `oathdigital.gameplay` and `gameplay.operations`; if the checker refuses its import of `gameplay.actions.recover`, stop and report rather than move the card elsewhere.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/recover/UndergroundLibraryCard.scala \
  src/main/scala/distantlands/gameplay/powers/action/UndergroundLibrary.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/test/scala/distantlands/gameplay/powers/action/UndergroundLibrarySuite.scala \
  src/main/scala/distantlands/gameplay/actions/recover/RecoverProcedure.scala \
  src/test/scala/distantlands/gameplay/RecoverProcedureSuite.scala \
  src/test/scala/distantlands/gameplay/RecoverEligibilitySuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): read Drowned Library's pick in the Recover

The Recover reads its difficulty from the walk's answers: 5 once Drowned
Library is picked, and then it offers the relic deck's top card unseen.
A site with no Recover difficulty is now refused after the modifier
decision, so the pick can make it recoverable. The Underground Library
card moves beside the Recover, which names it.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Drowned Library, docs and gates

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala` (after `AsksOnSelection`, line 272)
- Modify: `src/main/scala/distantlands/gameplay/walker/ModifierChoice.scala:53-59` (`candidates`)
- Create: `src/main/scala/distantlands/gameplay/powers/recover/DrownedLibrary.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:65`
- Create: `src/test/scala/distantlands/gameplay/powers/recover/DrownedLibrarySuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (regenerated)
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md:84`

**Interfaces:**
- Consumes: `UndergroundLibraryCard`, `RecoverProcedure.relicDeckOption`, `relicDecisionId`, `choiceDecisionId` (Task 1).
- Produces:
  - `trait OnEdificeFace extends ContributingPower` in `oathdigital.gameplay.powerresolver`, with `def edifice: EdificeId`, `def side: EdificeSide` and `final def faceUp(ready: ReadyGame): Boolean`.
  - `case object DrownedLibrary extends OnEdificeFace` in `oathdigital.gameplay.powers.recover`, with `awayFromLibrary: String` and `emptyRelicDeck: String`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/recover/DrownedLibrarySuite.scala`:

```scala
package oathdigital.gameplay.powers.recover

import oathdigital.gameplay.{ModifierPlay, OathRules}
import oathdigital.gameplay.WalkerDiceFixture.scripted
import oathdigital.gameplay.actions.recover.{RecoverProcedure,
  UndergroundLibraryCard}
import oathdigital.gameplay.cards.{BrokenPeaksSite, DeepWoodsSite,
  FairIsleSite}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Drowned Library (E13, ruined), a Recover modifier for 1 secret: with
  * the user's pawn at its site, the Recover takes the relic deck's top card
  * at difficulty 5 (catalog batch 10, slice 8). */
class DrownedLibrarySuite extends munit.FunSuite:
  import DefenseDieFace.{Blank, OneShield, TwoShields}

  private val library = UndergroundLibraryCard.id
  private val libraryRef = DecisionOptionRef.Edifice(library)
  private val modifierDecisionId = "recover.modifiers"

  /** Rules whose Recover rolls two shields and two shields, then one shield
    * and a blank: 4 on the first roll, 5 after the second. */
  private def rulesRollingFourThenFive: OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = scripted(Vector(TwoShields, TwoShields),
      Vector(OneShield, Blank)))

  /** p1 at `site`, where Drowned Library stands, with one faceup secret. */
  private def atDrownedLibrary(site: SiteId = FairIsleSite.id): Table =
    Table.start.pawn(p1, at = site)
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined, at = site)
      .secrets(p1, faceUp = 1)

  /** `table` with Relic Worship as p1's adviser and a second secret to pick
    * it with, so the modifier decision is asked whatever Drowned Library's
    * state. */
  private def withRelicWorship(table: Table): Table =
    table.adviser(p1, RelicWorshipCard).secrets(p1, faceUp = 2)

  /** p1's Recover from `table`, started with `picks` picked. */
  private def recoverStarted(rules: OathRules, table: Table,
      picks: Vector[PowerId]): OathTransition =
    accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Recover, p1, PowerWindow.RecoverModifierSelection, picks))

  private def answered(rules: OathRules, from: OathTransition,
      decisionId: String, ref: DecisionOptionRef): OathTransition =
    accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))

  /** The modifier decision's options at p1's first Recover park. */
  private def modifierOptions(table: Table): Vector[DecisionOption] =
    val begun = accepted(MenuPlay.started(rulesRollingFourThenFive,
      Ready(table.ready), p1, ActionRef.Recover, Vector.empty))
    ParkedNode.of(begun.state, catalog,
        WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _)))
          if decide.decisionId == modifierDecisionId =>
        decide.query match
          case one: DecisionQuery.ChooseOne => one.options
          case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected the modifier decision, got $other")

  private def libraryOption(table: Table): DecisionOption =
    modifierOptions(table).find(_.ref == libraryRef)
      .getOrElse(fail("Drowned Library is not listed"))

  test("its printed power carries the Recover icon"):
    assertEquals(UndergroundLibraryCard.ruined.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Recover)))

  test("picked, the secret goes onto the card, the Recover needs 5, and it " +
      "takes the relic deck's top card facedown"):
    val rules = rulesRollingFourThenFive
    val table = atDrownedLibrary()
    val ready = table.ready
    val top = ready.game.current.commonCards.relicDeck.head
    val started = recoverStarted(rules, table, Vector(DrownedLibrary.id))
    // Four shields reach Fair Isle's 3 but not 5, so the choice is asked.
    assertEquals(Outcomes.ready(started.state).game.current.walkerPending
      .map(_.at), Some(Vector("1", "1", "0", "2", "0")))
    val continued = answered(rules, started,
      RecoverProcedure.choiceDecisionId, DecisionOptionRef.Button("continue"))
    val taken = answered(rules, continued, RecoverProcedure.relicDecisionId,
      RecoverProcedure.relicDeckOption)
    val after = Outcomes.ready(taken.state)
    assertEquals(Look(after).player(p1).relics
      .map(held => held.id -> held.orientation),
      Vector(top -> Orientation.FaceDown))
    assertEquals(after.game.current.commonCards.relicDeck,
      ready.game.current.commonCards.relicDeck.tail)
    assertEquals(Look(after).tokensOn(library), Tokens(0, 1))
    val events = started.events ++ continued.events ++ taken.events
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    assertWireRoundTrips(events)

  test("picked, a Recover that stops short of 5 takes nothing"):
    val rules = rulesRollingFourThenFive
    val table = atDrownedLibrary()
    val started = recoverStarted(rules, table, Vector(DrownedLibrary.id))
    val stopped = answered(rules, started, RecoverProcedure.choiceDecisionId,
      DecisionOptionRef.Button("stop"))
    val after = Outcomes.ready(stopped.state)
    assertEquals(Look(after).player(p1).relics, Vector.empty)
    assertEquals(after.game.current.commonCards.relicDeck,
      table.ready.game.current.commonCards.relicDeck)

  test("it makes a site with no Recover difficulty recoverable"):
    val table = atDrownedLibrary(DeepWoodsSite.id)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Recover))
    assert(!MenuPlay.availableInMenu(catalog,
      table.secrets(p1, faceUp = 0).ready, ActionRef.Recover))

  test("it is offered for a Recover, at its secret"):
    assertEquals(libraryOption(withRelicWorship(atDrownedLibrary())),
      DecisionOption.Priced(DecisionOption.Edifice(libraryRef),
        OptionPrice(secrets = 1)))

  test("it is unavailable at a site its user rules but their pawn is not at"):
    val table = withRelicWorship(Table.start.pawn(p1, at = FairIsleSite)
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined,
        at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p1, 1))
    assertEquals(libraryOption(table), DecisionOption.Unavailable(
      DecisionOption.Edifice(libraryRef),
      OptionReason.Written(DrownedLibrary.awayFromLibrary)))

  test("it is unavailable with an empty relic deck"):
    assertEquals(libraryOption(withRelicWorship(atDrownedLibrary())
        .relicDeckEmptied),
      DecisionOption.Unavailable(DecisionOption.Edifice(libraryRef),
        OptionReason.Written(DrownedLibrary.emptyRelicDeck)))

  test("it is not listed while the edifice shows its intact face"):
    val table = withRelicWorship(Table.start.pawn(p1, at = FairIsleSite)
      .edifice(UndergroundLibraryCard, EdificeSide.Intact,
        at = FairIsleSite))
    assert(!modifierOptions(table).exists(_.ref == libraryRef),
      modifierOptions(table))
```

Notes on the fixtures:
- `ParkedNode` is used as `CatacombsGame.modifierOption` uses it.
- `secrets(p1, faceUp = 2)` replaces the first table's count, so `withRelicWorship` leaves p1 with 2 secrets.
- Relic Worship is there only so the modifier decision is asked when Drowned Library cannot be picked. If Relic Worship is itself unavailable on one of these tables, the decision is not asked. In that case, add a facedown relic at the pawn's site (`.relicAt(DowsingSticksCard, at = ...)`) as `RecoverModifierChoiceSuite.atFairIsle` does. Do not change the assertions.
- The park path in the first test is the Recover's choice behind the modifier decision (`"1"`), with no node inserted, as `CatacombsContributionSuite`'s "without the Catacombs modifier" test reads it.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.recover.DrownedLibrarySuite"`
Expected: compile failure, "Not found: DrownedLibrary".

- [ ] **Step 3: Add `OnEdificeFace` and list by face**

In `ContributingPower.scala`, after `AsksOnSelection`:

```scala
/** A power printed on one face of an edifice. One edifice id names both
  * faces, so the card is found whichever face is up; the modifier decision
  * lists such a power only while its face is up, as `RuleSourceIndex`
  * lists an edifice's powers by its side (catalog batch 10, slice 8). */
trait OnEdificeFace extends ContributingPower:
  def edifice: EdificeId
  def side: EdificeSide
  /** Whether the face this power is printed on is up somewhere on the
    * map. */
  final def faceUp(ready: ReadyGame): Boolean =
    ready.game.current.edificeSite(edifice, side).nonEmpty
```

`ContributingPower.scala` imports model names one by one: add `EdificeId` and `EdificeSide` to its `oathdigital.model.{...}` import.

In `ModifierChoice.candidates`, add the face to the filter:

```scala
  private def candidates(pass: ModifierChoice.Pass)
      : Vector[ContributingPower] = pass.powers.pool.filter(power =>
    power.resolution == PowerResolution.PlayerSelected &&
      power.selectionWindows(modifierWindow) &&
      power.selectionCard.exists(card =>
        PowerAccess.locate(pass.state, actor, card).isDefined) &&
      (power match
        case faced: OnEdificeFace => faced.faceUp(pass.state)
        case _ => true))
    .sortBy(ContributingPower.sortKey)
```

Add `OnEdificeFace` to the `powerresolver` import. In the class doc, after "then every player-selected power that may be picked at the window and whose card the actor may use", add ", on its face for an edifice".

- [ ] **Step 4: Write the power**

Create `src/main/scala/distantlands/gameplay/powers/recover/DrownedLibrary.scala`:

```scala
package oathdigital.gameplay.powers.recover

import oathdigital.gameplay.PowerAccess
import oathdigital.gameplay.actions.recover.UndergroundLibraryCard
import oathdigital.gameplay.operations.Costs
import oathdigital.gameplay.powerresolver.{Contribution, OfferReason,
  OnEdificeFace, PowerCtx, PowerUse}
import oathdigital.gameplay.powers.{CatalogResolution, SelectedModifier}
import oathdigital.model._

/** Drowned Library (E13, ruined), a Recover modifier for 1 secret: "If your
  * pawn is at this site, you may recover the top card of the relic deck
  * instead. Its recover difficulty is 5." (catalog batch 10, slice 8).
  *
  * It is written as Catacombs is, since a selected modifier's card is a
  * denizen or a relic, never an edifice face. The pick places its secret on
  * the edifice, and the pick is the whole effect: it hooks no window. The
  * Recover reads the pick from the walk's answers
  * (`RecoverProcedure.fromRelicDeck`), then needs 5 and offers the relic
  * deck, so the relic stays unseen until recovered; a site with no Recover
  * difficulty of its own becomes recoverable.
  *
  * It is listed only while the ruined face is up (`OnEdificeFace`), and
  * wherever its user may use the edifice. It is unavailable, in this order,
  * away from the user's pawn, and with an empty relic deck. It writes no
  * line: the start line names it, and the Recover's own line names the
  * relic.
  */
case object DrownedLibrary extends OnEdificeFace:
  private val printed = UndergroundLibraryCard.ruined.power
  val id: PowerId = printed.id
  val edifice: EdificeId = UndergroundLibraryCard.id
  val side: EdificeSide = EdificeSide.Ruined
  /** Why it cannot be picked away from the user's pawn. */
  val awayFromLibrary: String = "Your pawn is not at this site"
  /** Why it cannot be picked with no relic left to recover. */
  val emptyRelicDeck: String = "The relic deck is empty"

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution = CatalogResolution.of(printed)
  override def selectionCard: Option[CardId] = Some(edifice)
  override def selectionWindows: Set[PowerWindow] =
    Set(PowerWindow.RecoverModifierSelection)

  /** Its use at its selection window: its pick places one secret on the
    * edifice. */
  override def use(ctx: PowerCtx): Option[PowerUse] =
    SelectedModifier.selectionAction(ctx.window).map(_ =>
      PowerUse.of(ctx.actor, printed, edifice, secret = true, places = true))

  /** At a selection window, for a Recover only; everywhere, while the
    * ruined face is up and the user's pawn stands at it. Stable across the
    * action: the pawn does not move during a Recover. */
  override def applicable(ctx: PowerCtx): Boolean =
    SelectedModifier.selectionAction(ctx.window).forall(
      _ == MajorActionType.Recover) && atLibrary(ctx.state, ctx.actor)

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    val reason =
      if !atLibrary(ctx.state, ctx.actor) then Some(awayFromLibrary)
      else Option.when(
        ctx.state.game.current.commonCards.relicDeck.isEmpty)(emptyRelicDeck)
    reason.map(OfferReason.Unavailable(_))

  override def selectionPayments(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    Vector(Costs.onCard(actor, edifice, Cost(secret = 1),
      Some(UndergroundLibraryCard.suit), intoOccupied = false))

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** Whether the ruined face stands at the site the user acts from. */
  private def atLibrary(ready: ReadyGame, actor: PlayerId): Boolean =
    ready.game.current.edificeSite(edifice, side).exists(site =>
      PowerAccess.presentSite(ready, actor).contains(site))
```

In `WalkerPowerCatalog.scala:65`, register it after `CatacombsContribution`: `Vector(CatacombsContribution, DrownedLibrary, RelicPatrols, BanditPatrols,`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.recover.* oathdigital.gameplay.CatacombsContributionSuite oathdigital.gameplay.RecoverProcedureSuite oathdigital.gameplay.powers.economy.*ModifierChoiceSuite"`
Expected: all pass.

If "it makes a site with no Recover difficulty recoverable" fails because the menu refuses, check that `QueuedStart.priced` reaches the pick: `WalkerSimulation.started` searches the modifier decision, and the Recover after it must see the pick in the answers it walks with. Do not move the refusal back to `recoveryAt`.

- [ ] **Step 6: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected:
- `unimplemented.txt` loses `edifice.e13.ruined`.
- `declared-keys.txt` is unchanged, since Drowned Library writes no line.

Any other change means something unplanned moved: stop and find out why before committing.

- [ ] **Step 7: Update the spec and the roadmap**

In `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`:
- Status line: "Slices 1 to 8 landed."
- Under "## Rulings", add:

```markdown
15. **No scratch primitive for Drowned Library** (owner, 2026-11-06). The
    Recover reads Drowned Library's pick from the walk's answers instead
    of a recorded `RecoverFromRelicDeck`. The pick is already recorded,
    so the Recover names the card, as `EffectiveSuit` names Acting
    Troupe. A general seam waits for a second card that sets a Recover's
    source.
```

- Replace the body of "## Slice 8: Recover source" with:

```markdown
**Drowned Library.** A Recover modifier for 1 secret, placed on the card,
usable with the user's pawn at its site. A selected modifier's card is a
denizen or a relic, so the power is written as Catacombs is. The pick is
its whole effect (ruling 15): the card moves beside `RecoverProcedure`,
which reads the pick from the walk's answers with
`ModifierPicks.isPicked`.

`RecoverProcedure` reads its difficulty live: 5 once the Library is
picked, else the site's. `succeeded`, the `Repeat` guard and
`rollFeedback` read it; `rollFeedback` reads the parked walk's answers.
With the pick, the relic decision offers the relic deck, and the move
takes its top card from `Location.Deck(CardDeck.Relic)`. The relic stays
unseen until recovered. With an empty relic deck, the modifier is
unavailable with a hover reason.

Recover refused a site with no Recover difficulty when its tree was built,
before the modifier loop. The refusal moves after the loop: with no
difficulty the `Repeat` runs no round and the last branch refuses. "Start"
and the Act menu judge the tree after the picks, so Drowned Library makes
such a site recoverable. Recover never refused a site with no relic; a
Recover with nothing to take is a legal wasted action.

One edifice id names both faces, so a new capability trait,
`OnEdificeFace`, names the face a power is printed on. The modifier
decision lists such a power only while its face is up.

Tests: success and failure against difficulty 5; the relic moved from the
deck; the pick unavailable away from the site or with an empty deck, and
unlisted on the intact face; a Recover at a site with no difficulty,
refused without the pick and allowed with it; replay.
```

- Under "### Readings", replace the Drowned Library bullet with:

```markdown
- **Drowned Library** needs the user's pawn at its site. The relic is the
  relic deck's top card, at difficulty 5, unseen until recovered: the
  relic decision offers the deck, and the Recover's line names the relic
  only to a viewer who may identify it. Relic Patrols and Bandit Patrols
  still refuse the Recover at their sites, whatever its source.
```

- In "## Seams opened", the slice 8 row reads `| Drowned Library's pick read by Recover, \`OnEdificeFace\` | 8 | Drowned Library |`.

In `docs/ROADMAP.md`, delete the line `- **Recover source:** Drowned Library.` (memory: "Roadmap deletes done work").

- [ ] **Step 8: Run the gates**

Run in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes, and statement coverage is at least 92%.

Run: `python3 scripts/check-architecture.py`
Expected: no violation.

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken link.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala \
  src/main/scala/distantlands/gameplay/walker/ModifierChoice.scala \
  src/main/scala/distantlands/gameplay/powers/recover/DrownedLibrary.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/distantlands/gameplay/powers/recover/DrownedLibrarySuite.scala \
  src/test/resources/powers/unimplemented.txt \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): implement Drowned Library

Drowned Library (E13, ruined) is a Recover modifier for 1 secret: with
the user's pawn at its site, the Recover takes the relic deck's top card
at difficulty 5, unseen until recovered. An edifice face's power is now
listed in a modifier decision only while that face is up.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
