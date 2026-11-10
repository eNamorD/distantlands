# Catalog Batch 11, Slice 7: False Prophet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement False Prophet (card 85, Discord, adviser-only,
locked): "When Played, if you're an Exile, gain one warband and put it on
any revealed Vision. You now also have it revealed. If it is ever
discarded, kill the warband and play or discard the Vision as if you had
searched."

**Architecture:** Three seams, then the card.

1. **The hook names its player.** `CardPlayedFaceup` gains `player`, as
   `CardPlayedFacedown` has it. The Vision readers (Secret Police, Sacred
   Ground, Vow of Obedience, Book Binders) read it through
   `VisionPlay.player`, so a reveal inside another player's walk is judged
   for the player who reveals.
2. **Warbands on a Vision.** A warband on a card is checked against the
   card's tokens, which a Vision has none of. A Vision's warbands are read
   from `warbandsOnCards` alone. `RevealedVisions.of` (slice 6) adds every
   other player's revealed Vision that carries a warband of the player's
   lineage, and the owner's board shows the warbands.
3. **Off-turn card-play lines.** The card play's own lines, and Desecrated
   Ground's kept line, name the player when they are not the walk's actor
   (ruling 34).

The card is two powers. `FalseProphet`, a `WhenPlayedPower`, places the
warband. `FalseProphetClaim`, an `ExecutedTrigger`, reacts to the discard
of a claimed Vision. It asks the current player which claimant plays the
Vision, when there are several (ruling 32), nests that claimant's card play
limited to faceup or discard (ruling 33), and then kills every claimant's
warband.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read rulings 14 and 30 to 34, the False Prophet bullets of "Readings",
and "Slice 7: A claimed Vision and an off-turn reveal". Read the
[slice 6 plan](2026-11-09-catalog-batch-11-slice6.md) for the names this
plan builds on: `revealedVisions`, `RevealedVisions.of`,
`VisionVictoryEligibility.qualifying`, `DesecratedGroundRule`.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, no non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`).
- Statement coverage stays at or above 92%.
- Wire changes need not keep old journals decodable.
- Log lines name the actor only when they are not the walk's actor. Power
  lines end with a period; procedure lines do not.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Prerequisite

This branch (`batch-11-slice-7`) was cut from the slice 6 plan commit
`d8c82be3c`, on top of `0.2.0-alpha` at `c26c91733` (slice 5 landed).
Slice 6 was being executed on `batch-11-slice-6` when this plan was
written.

**Before Task 1:** slice 6 must have landed on `0.2.0-alpha`. Rebase this
branch onto it (`git rebase 0.2.0-alpha`). The only commit is this plan
and its spec edits. Then check that the names below exist, and adjust the
code in this plan to what slice 6 actually built:

```bash
grep -rn "object RevealedVisions\|def qualifying\|object DesecratedGroundRule\|revealedVisions:" src/main/scala | head
```

## Plan-time findings

1. **`CardPlayedFaceup` carries no player.** It is
   `CardPlayedFaceup(card, resultingSource, byPower)`
   (`model/CardPlayHooks.scala:12-16`), built only in
   `CardPlayProcedure.hook` (`CardPlayProcedure.scala:524-530`), which
   has the player as `actor` and drops it. `VisionPlay` (`cardplay/VisionPlay.scala:8`)
   says "The player is the one whose action runs, `ctx.actor`", the walk's
   actor. Secret Police (`SecretPolice.scala:58-65`), Sacred Ground
   (`SacredGround.scala:47-54`), Vow of Obedience
   (`VowOfObedience.scala:45-47`) and Book Binders (`BookBinders.scala:71-73`)
   all read `ctx.actor`. Every reveal today is by the walk's actor; the
   claimant's is not.
2. **The hook has 11 test constructions,** all positional
   (`grep -rn "CardPlayedFaceup(" src/test/scala`). The new field goes
   second, as in `CardPlayedFacedown(card, player, byPower)`, so every
   construction and pattern changes.
3. **A warband cannot stand on a Vision today.**
   `OperationStateAdapter.warbands` (`:102-112`) reads
   `Location.OnCard(id)` through `cardTokens` (`:114-123`), which accepts
   only a `SiteDenizenState` or a `RelicState`. A `Move` to or from
   `OnCard(visionId)` fails with `IncompatibleLocation`. The write
   (`ResourceOperations.scala:289-298`) has no card-type check.
4. **The claim is the lineage's.** `warbandsOnCards` keys a card's
   warbands by `ForceKind`. An Exile's kind is `ForceKind.Exile(lineage)`
   (`PlayerForceKind.scala:8-12`). Becoming a Citizen turns the board's
   and sites' warbands purple but leaves a card's alone (`RoleChanges`
   has no `warbandsOnCards` write), so a Citizen claimant's warband stays
   `Exile(lineage)` and the claim survives the role change (FAQ).
5. **A follow-up must rebuild the same tree.** `ExecutedTrigger`'s doc
   (`powerresolver/ExecutedTrigger.scala:20-24`): a command that resumes
   inside a follow-up asks the trigger again with the held executed
   operations and the parked state, and must get the same tree up to the
   park. `FalseProphetClaim` reads the claimants from the warbands on the
   Vision, so nothing that removes them may run before the last question:
   the kill comes last, and its line follows the card play's lines. The
   Vision's discard region comes from the executed `Discard.Vision`'s
   `to`, not from where the Vision lies now, because the claimant's play
   moves it.
6. **A follow-up's own batches start no follow-up**
   (`ExecutedTrigger.scala:18-19`). A claimant who reveals the Vision
   discards their own revealed Vision (unless Desecrated Ground keeps it).
   If a third player claims that one, no follow-up runs for it, and that
   warband stays on a discarded Vision until the Chronicle returns every
   warband on a card (`ReliquaryAndRelics.returnFromCards`). See Known
   limits.
7. **Answers are walk-wide.** `PendingTree.answered` is flat, keyed by
   decision id, and the card play reads its placement answer with
   `collectFirst` on `cardplay.place.vision.{id}`. Two card plays of the
   same Vision in one walk would read one answer. Ruling 32 gives the play
   to one claimant only, so the Vision is played at most once per discard.
8. **Limiting a power's play has a precedent.** Temple of Ancestors
   contributes a `Transform` at `SearchPlayAdviser` that matches
   `CardPlayProcedure.PlacementTree` with its own `byPower` and adds a
   `PlacementLimit` (`action/TempleOfAncestors.scala:51-55, 94-100`).
   `CardSlots.limited` shows a refused placement unavailable, with the
   reason on hover.
9. **The owner's reveal line comes after the claimant's lines.** An
   executed trigger's follow-up walks under the play leaf's own path, and
   the owner's "Revealed Vision of Faith" line is the play's third child,
   after that leaf (`CardPlayProcedure.scala:470-476`, and the `settled`
   doc). So the log reads the claimant's play, then the kill, then the
   owner's reveal. That order is the engine's, and this slice does not
   change it.
10. **Only one option ref names a Vision card.** `DecisionOptionRef.RevealedVision(owner)`
    names a player's slot, not a card, and with slice 6 a player may hold
    several. The When Played offers `DecisionOption.Vision(DecisionOptionRef.Vision(id))`
    (`model/Decisions.scala:69, 281`), which Swear an Oath also uses after
    slice 6.
11. **Two powers share one printed power id.** `WhenPlayedPower` makes
    `applicable` and `contributions` final, so the trigger and its
    `SearchPlayAdviser` limit live in a second object. `DeclaredNotes.of`
    groups keys by power id and keeps them distinct by name
    (`DeclaredNotes.scala:34-40`), so two objects may declare different
    keys under `denizen.false-prophet`.

## File structure

- Modify `src/main/scala/distantlands/model/CardPlayHooks.scala`: the hook's
  `player`.
- Modify `gameplay/actions/cardplay/CardPlayProcedure.scala`: build the
  hook with the player; off-turn lines.
- Modify `gameplay/powers/cardplay/VisionPlay.scala`: `player`.
- Modify the readers: `SecretPolice`, `SacredGround`, `VowOfObedience`,
  `BookBinders` (player), and `WhenPlayedPower`, `ConspiracyWhenPlayed`,
  `SaddleMakers`, `WildCry`, `WelcomingParty` (pattern only).
- Modify `gameplay/operations/OperationStateAdapter.scala`: a Vision's
  warbands.
- Modify `gameplay/RevealedVisions.scala` (slice 6): the claim.
- Modify `gameplay/DesecratedGround.scala` (slice 6): the off-turn kept
  line.
- Modify `application/GamePresentationProjector.scala` and
  `frontend/.../WorldBoardRenderer.scala`, `frontend/styles.css`: the
  owner's pill shows the warbands.
- Create `gameplay/powers/whenplayed/FalseProphet.scala`: the card and
  the When Played.
- Create `gameplay/powers/whenplayed/FalseProphetClaim.scala`: the trigger
  and the placement limit.
- Modify `catalog/holding/HoldingDiscordDenizens.scala`: the card leaves.
- Modify `gameplay/powers/whenplayed/WhenPlayedPowers.scala`,
  `gameplay/powers/WalkerPowerCatalog.scala`: registration.
- Modify `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt`.
- Tests: `VisionPlayPlayerSuite` (new), `RevealedVisionsSuite`,
  `StateBasedEvaluationSuite`, `PlayerBoardSuite`, `FalseProphetSuite`
  (new), `FalseProphetClaimSuite` (new), `FalseProphetLogSuite` (new), and
  the 11 hook constructions.

---

### Task 1: The card-play hook names its player

**Files:**
- Modify: `src/main/scala/distantlands/model/CardPlayHooks.scala:12-16`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala:292, 524-530`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/VisionPlay.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/SecretPolice.scala:58-65`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/SacredGround.scala:47-54`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/VowOfObedience.scala:45-47`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/BookBinders.scala:71-73`
- Modify (pattern only): `whenplayed/WhenPlayedPower.scala:30`,
  `whenplayed/ConspiracyWhenPlayed.scala:41`, `cardplay/SaddleMakers.scala:57`,
  `cardplay/WildCry.scala:39`, `cardplay/WelcomingParty.scala:44`
- Test: `src/test/scala/distantlands/gameplay/powers/cardplay/VisionPlayPlayerSuite.scala` (new)
- Test (constructions): the 11 files `grep -rln "CardPlayedFaceup(" src/test/scala` lists

**Interfaces:**
- Produces: `CardPlayedFaceup(card: WorldCardId, player: PlayerId,
  resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)`.
- Produces: `VisionPlay.player(ctx: PowerCtx): PlayerId`, the hook's
  player, or `ctx.actor` for any other operation.

- [ ] **Step 1: Write the failing test**

Create `src/test/scala/distantlands/gameplay/powers/cardplay/VisionPlayPlayerSuite.scala`:

```scala
package oathdigital.gameplay.powers.cardplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.operations.OperationRestrictions
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ProcedureWalker, WalkerPowers}
import oathdigital.model._
import oathdigital.testkit.{PowerRules, Table}
import oathdigital.testkit.Table.{p1, p2}

/** A faceup Vision play is judged for the player the hook names, not for
  * the walk's actor (catalog batch 11, slice 7): False Prophet's claimant
  * reveals inside the Vision owner's walk. */
class VisionPlayPlayerSuite extends munit.FunSuite:
  private val vision = VisionRules.Faith
  private val e08 = SacredGroundCard.id

  private def powers: WalkerPowers = WalkerPowers(Vector(SacredGround),
    restrictionSet = OperationRestrictions.forCatalog(catalog),
    declared = PowerRules.declared)

  /** p1's turn, Sacred Ground intact at `sacred`; p1 stands at Ancient
    * City and p2 at Broken Peaks; Faith lies in the Provinces discard. */
  private def table(sacred: SiteId): ReadyGame = Table.start
    .pawn(p1, at = AncientCitySite).pawn(p2, at = BrokenPeaksSite)
    .edifice(e08, EdificeSide.Intact, at = sacred)
    .discardPile(Region.Provinces, vision).ready

  private def hook(player: PlayerId): CardPlayedFaceup = CardPlayedFaceup(
    vision, player, RuleSourceRef.GameRule("revealed-vision"))

  test("p2's reveal on p1's turn is refused where p2's pawn is not"):
    // Sacred Ground stands at p1's site: p1 could reveal, p2 cannot.
    val result = ProcedureWalker.advance(table(AncientCitySite.id), hook(p2),
      None, powers)
    assertEquals(result.left.toOption,
      Some(VisionPlay.forbidden("Sacred Ground")))

  test("p2's reveal on p1's turn is allowed where p2's pawn is"):
    val result = ProcedureWalker.advance(table(BrokenPeaksSite.id), hook(p2),
      None, powers)
    assert(result.isRight, result.toString)
```

Check three names before running: the Table step that puts a card in a
discard pile (`grep -n "def discard" src/test/scala/distantlands/testkit/Table.scala`),
the source a revealed Vision's hook carries (`grep -n "def playedSource" -A12
src/main/scala/distantlands/gameplay/actions/CardPlay.scala`; use what it
returns for `SearchPlacement.Adviser(Orientation.FaceUp, None)`), and how
`ProcedureWalker.advance` reports a restriction's refusal (`grep -n
"forbidden" src/test/scala/distantlands/gameplay/powers/cardplay/*.scala`).
If a refusal is reported in another shape, assert on that shape instead.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.cardplay.VisionPlayPlayerSuite"`
Expected: compile error: `CardPlayedFaceup` takes three arguments.

- [ ] **Step 3: Add the player to the hook**

`CardPlayHooks.scala:12-16`:

```scala
/** A card played faceup, by `player`: the hook's readers judge the play
  * for them. A play inside another player's walk names its own player
  * (False Prophet's claimant, catalog batch 11, slice 7). */
final case class CardPlayedFaceup(card: WorldCardId, player: PlayerId,
    resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)
    extends Operation:
  override val window = Some(PowerWindow.ActionCardPlayedFaceup)
```

Keep the existing doc text, adding the sentence about `player`.

`CardPlayProcedure.scala:528-529`:

```scala
    case _ => CardPlay.playedSource(ready, actor, card, placement)
      .map(CardPlayedFaceup(card, actor, _, byPower)).toVector
```

`CardPlayProcedure.scala:292`: `case CardPlayedFaceup(_, _, _, by) =>
by.nonEmpty`.

- [ ] **Step 4: Read the player in `VisionPlay`**

Replace the doc and `played` in `VisionPlay.scala`, and use `player` in
`pending`:

```scala
/** Reads a faceup Vision play off the card-play hook
  * (`ActionCardPlayedFaceup`), for the cards that forbid or reward one. The
  * player is the one the hook names, who may not be the walk's actor
  * (False Prophet's claimant, catalog batch 11, slice 7).
  */
private[cardplay] object VisionPlay:

  /** The Vision the hook plays faceup, the Conspiracy included. */
  def played(ctx: PowerCtx): Option[VisionId] = ctx.operation match
    case CardPlayedFaceup(card: VisionId, _, _, _) => Some(card)
    case _ => None

  /** The player who plays the card: the hook's, else the walk's actor. */
  def player(ctx: PowerCtx): PlayerId = ctx.operation match
    case CardPlayedFaceup(_, player, _, _) => player
    case _ => ctx.actor

  // (pending's doc unchanged)
  def pending(ctx: PowerCtx): Option[VisionId] =
    played(ctx).filter(held(ctx.state, player(ctx), _))
```

- [ ] **Step 5: Make the readers read it**

`SecretPolice.scala:58-65`, in `blocked`:

```scala
  private def blocked(ctx: PowerCtx): Option[OathViolation] = for
    _ <- VisionPlay.pending(ctx)
    player = VisionPlay.player(ctx)
    ruler <- CardRule.rulerOf(ctx.state, cardId)
    if SiteRule.enemies(ruler, SiteRuler.Player(player))
    pawn <- ctx.state.game.current.pawnSite(player)
    if SiteRulers.rulerOf(ctx.state, pawn).contains(ruler)
  yield VisionPlay.forbidden("Secret Police")
```

`SacredGround.scala:53`: `if !ctx.state.game.current.pawnSite(
VisionPlay.player(ctx)).contains(site)`.

`VowOfObedience.scala:46`: `VisionPlay.pending(ctx).filter(_ =>
holds(ctx.state, VisionPlay.player(ctx)))`.

`BookBinders.scala:73`: `holder <- holderOf(ctx.state).filter(_ !=
VisionPlay.player(ctx))`. Update the doc above `reward` from "whenever
another player plays" to "whenever a player other than its holder plays".
If Book Binders names the player elsewhere in `reward` (its line or its
decision), use `VisionPlay.player(ctx)` there too.

Pattern-only changes, adding one `_` to each `CardPlayedFaceup(...)`
pattern: `WhenPlayedPower.scala:30`, `ConspiracyWhenPlayed.scala:41`,
`SaddleMakers.scala:57`, `WildCry.scala:39`, `WelcomingParty.scala:44`.
These read denizen plays and the Conspiracy, which only the walk's actor
makes, so they keep `ctx.actor`.

Run: `grep -rn "CardPlayedFaceup(" src/main/scala` and confirm no
three-field pattern is left.

- [ ] **Step 6: Update the test constructions**

For each file `grep -rln "CardPlayedFaceup(" src/test/scala` lists, insert
the player as the second argument: the actor the suite plays as
(`actor`, `p1`, or the adviser's holder named in the
`RuleSourceRef.Adviser(holder, card)` beside it). `WhenPlayedHarness.hook`
and `hookAt` take `actor`. Patterns on the hook in tests gain one `_`.

- [ ] **Step 7: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.cardplay.*" "testOnly oathdigital.gameplay.powers.whenplayed.*" "testOnly oathdigital.gameplay.powers.targeting.*" "testOnly oathdigital.gameplay.VisionPlaySuite oathdigital.gameplay.ProcedureWalkerWindowSuite oathdigital.gameplay.powers.poweruse.AutomaticUseStepWiringSuite"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/CardPlayHooks.scala \
  src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/VisionPlay.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/SecretPolice.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/SacredGround.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/VowOfObedience.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/BookBinders.scala \
  src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPower.scala \
  src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/SaddleMakers.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/WildCry.scala \
  src/main/scala/distantlands/gameplay/powers/cardplay/WelcomingParty.scala \
  src/test/scala/distantlands/gameplay/powers/cardplay/VisionPlayPlayerSuite.scala
git add $(grep -rln "CardPlayedFaceup(" src/test/scala)
git commit -m "feat(cardplay): the faceup play hook names the player who plays" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: A warband on a Vision, the claim and the board

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateAdapter.scala:102-112`
- Modify: `src/main/scala/distantlands/gameplay/RevealedVisions.scala` (slice 6)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala:187-188`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala` (slice 6's pill loop)
- Modify: `frontend/styles.css` (after `.player-slot-vision-empty`, about line 1222)
- Test: `src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala`
- Test: an operations suite for the move (see Step 1)

**Interfaces:**
- Consumes: `RevealedVisions.of(ready, player): Vector[VisionId]` and
  `VisionVictoryEligibility.qualifying(player, ready)` from slice 6.
- Produces: `RevealedVisions.of` returns the player's own revealed
  Visions, then the Visions they claim, in seat order of their owners.
- Produces: `RevealedVisions.claimants(ready, vision): Vector[PlayerId]`,
  the players with a warband of their lineage on `vision`, in seat order.

- [ ] **Step 1: Write the failing tests**

Add to `RevealedVisionsSuite`:

```scala
  test("a Vision that carries a player's warband is theirs too"):
    // p1's Faith carries one of p2's warbands.
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1).ready
    assertEquals(RevealedVisions.of(ready, p2),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(RevealedVisions.of(ready, p1),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(RevealedVisions.claimants(ready, VisionRules.Faith),
      Vector(p2))

  test("a claim is the claimant's lineage's, so a Citizen keeps it"):
    // p2's warband goes on before p2 turns Citizen: the card keeps it.
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1).imperial(p3, p2).ready
    assertEquals(RevealedVisions.of(ready, p2),
      Vector[VisionId](VisionRules.Faith))

  test("a warband on a Vision no one has revealed claims nothing"):
    val ready = Table.start.warbandsOnCard(VisionRules.Faith, p2, 1).ready
    assertEquals(RevealedVisions.of(ready, p2), Vector.empty)
```

Import `p3` from `Table`.

Add to `StateBasedEvaluationSuite`, beside its Wake Vision test (`grep -n
"VisionVictory" src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala`),
copying that test's turn and draw setup:

```scala
  test("an Exile wins at Wake with a Vision they claim"):
    // p2 claims p1's Faith and holds the Darkest Secret.
    val ready = Table.start.visionsDrawn(3).turn(p2, Phase.Wake)
      .revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1)
      .darkestSecret(Some(p2), 1).ready
    assertEquals(StateBasedEvaluation.visionAtWake(Ready(ready)),
      Right(Some(VisionVictory(p2, Vector(VisionRules.Faith)))))

  test("a Citizen cannot win with a Vision they claim"):
    val ready = Table.start.visionsDrawn(3)
      .revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1).imperial(p3, p2)
      .turn(p2, Phase.Wake).darkestSecret(Some(p2), 1).ready
    assertEquals(StateBasedEvaluation.visionAtWake(Ready(ready)), Right(None))
```

If `Table.turn` resets the round starter or `imperial` changes the turn,
order the steps as the suite's other tests do. If slice 6 left
`visionAtWake` reading the player's slot rather than `qualifying`, the
first test fails for that reason; switch it to `qualifying` in Step 3.

For the move, find the suite that covers Obsidian Cage's warbands on a
card (`grep -rln "OnCard" src/test/scala/distantlands/gameplay/operations`)
and add, following its style:

```scala
  test("a warband moves onto a revealed Vision and back to its bank"):
    val table = Table.start.revealedVision(p1, VisionRules.Faith)
    val ready = table.ready
    val kind = PlayerForceKind.of(ready,
      present(ready.game.current.player(p2), "p2")).get
    val vision = PositionedLocation(Location.OnCard(VisionRules.Faith))
    val onCard = accepted(OperationPipeline.run(ready, Vector(
      Gain.Warbands(p2, kind, 1),
      Move(Piece.Warbands(kind, 1), PositionedLocation(Location.PlayArea(p2)),
        vision)), OperationPolicy.Permissive, Vector.empty,
      requireAll = true)(Right(_))).state
    assertEquals(onCard.game.current.warbandsOn(VisionRules.Faith, kind), 1)
    val back = accepted(OperationPipeline.run(onCard, Vector(
      Kill(Piece.Warbands(kind, 1), vision)), OperationPolicy.Permissive,
      Vector.empty, requireAll = true)(Right(_))).state
    assertEquals(back.game.current.warbandsOn(VisionRules.Faith, kind), 0)
```

Match `OperationPipeline.run`'s call to how that suite runs operations;
the `ExecutedTriggers.held` call in `walker/ExecutedTriggers.scala:43-49`
shows the signature.

In `PlayerBoardSuite`, beside slice 6's "each revealed Vision has its own
pill" test:

```scala
  test("a revealed Vision's pill shows the warbands on it"):
    val faith = CardDetails("vision:vision-of-faith", "vision",
      "Vision of Faith", orientation = Some("face-up"),
      warbands = Vector(SiteForces.Exile(1, "p2", PlayerColor.Blue,
        "Blue Warbands")))
    val node = renderWith(board.copy(revealedVisions = Vector(faith)), None)
    val marks = all(node, ".player-slot-vision .player-slot-vision-warband")
    assertEquals(marks.size, 1)
    assert(marks.head.classList.contains("force-blue"))
```

Check the frontend card type's warband field and `SiteForces.Exile`'s
constructor (`grep -n "warbands\|case class Exile" frontend/src/main/scala/distantlands/frontend/*.scala | head`),
and the colour type's name; match them.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.RevealedVisionsSuite oathdigital.gameplay.StateBasedEvaluationSuite"`
Expected: FAIL: the claim is not counted, `claimants` does not exist, and
the move is refused with `IncompatibleLocation`.

- [ ] **Step 3: Read a Vision's warbands, and the claim**

`OperationStateAdapter.scala:110-111`, before the `OnCard(id)` case:

```scala
    // A Vision has no tokens: its warbands (False Prophet's) are only in
    // `warbandsOnCards`, wherever the card lies, so a claim discarded with
    // its Vision can still be killed.
    case Location.OnCard(id: VisionId) =>
      Right(ready.game.current.warbandsOn(id, kind))
    case Location.OnCard(id) => cardTokens(ready, id).map(_ =>
      ready.game.current.warbandsOn(id, kind))
```

`RevealedVisions.scala`, replacing slice 6's object body:

```scala
/** The Visions a player has revealed, as the victory rules read them: their
  * own, in reveal order, then every other player's revealed Vision that
  * carries a warband of their lineage (False Prophet, catalog batch 11,
  * slice 7). The kind is the lineage's, not the role's, so a claimant who
  * becomes a Citizen keeps the claim; the victory reads' Exile check stops
  * them winning with it (FAQ). The slot's own bookkeeping reads
  * `PlayerState.revealedVisions`, because a claim never puts a Vision in
  * the claimant's slot. */
object RevealedVisions:
  def of(ready: ReadyGame, player: PlayerId): Vector[VisionId] =
    val current = ready.game.current
    current.player(player).toVector.flatMap { state =>
      val own = state.revealedVisions.map(_.id)
      val claimed = current.players.filterNot(_.player == player)
        .flatMap(_.revealedVisions.map(_.id))
        .filter(vision => holds(ready, state, vision))
      own ++ claimed
    }

  /** The players with a warband of their lineage on `vision`, in seat
    * order. The card need not be revealed: a discarded claim is read too. */
  def claimants(ready: ReadyGame, vision: VisionId): Vector[PlayerId] =
    ready.game.current.players.filter(holds(ready, _, vision)).map(_.player)

  private def holds(ready: ReadyGame, state: PlayerState, vision: VisionId)
      : Boolean =
    ready.game.current.warbandsOn(vision, ForceKind.Exile(state.lineage)) > 0
```

If `visionAtWake` or `visionaryWin` still read `revealedVisions` directly
after slice 6, make them read `VisionVictoryEligibility.qualifying`, which
reads `RevealedVisions.of`. Keep their Exile filter.

- [ ] **Step 4: Show the warbands on the owner's board**

`GamePresentationProjector.scala`, in slice 6's `revealedVisions` line:

```scala
        player.revealedVisions.map(card => withWarbands(cardDetails(card.id,
          Some(card.orientation), hidden = false), ready, card.id)),
```

`WorldBoardRenderer.scala`, inside slice 6's
`board.revealedVisions.foreach { card =>` loop, right after the pill is
made:

```scala
        // The warbands on the Vision (False Prophet's claims), one mark in
        // each claimant's colour; the inspection overlay counts them.
        card.warbands.foreach { forces =>
          val mark = text("span", s"player-slot-vision-warband " +
            ServerUiSupport.forceCssClass(forces), "●")
          mark.setAttribute("aria-hidden", "true")
          HoverTip.set(mark, ServerUiSupport.forceText(forces))
          pill.appendChild(mark)
        }
```

`frontend/styles.css`, after `.player-slot-vision-empty`:

```css
.player-slot-vision-warband { margin-left: 0.3em; font-size: 0.8em; }
```

Check that `force-{colour}` classes already colour text, as
`.card-tracking` relies on (`grep -n "force-blue\|card-tracking" frontend/styles.css`).
If they only set a background, add a `color` rule for the mark the way
`.card-tracking` gets its colour.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.RevealedVisionsSuite oathdigital.gameplay.StateBasedEvaluationSuite oathdigital.gameplay.operations.*" "frontend/testOnly oathdigital.frontend.PlayerBoardSuite"`
Expected: PASS. If the frontend project id differs, read `build.sbt`.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/OperationStateAdapter.scala \
  src/main/scala/distantlands/gameplay/RevealedVisions.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/styles.css \
  src/test/scala/distantlands/gameplay/RevealedVisionsSuite.scala \
  src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala \
  frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala
git add <the operations suite edited in Step 1>
git commit -m "feat(gameplay): a warband on another player's Vision claims it" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint review** after this task: dispatch a reviewer with Tasks 1
and 2's diff (`git diff <rebase base>..HEAD`), this plan's findings 1 to
4, and the spec's slice 7 section.

---

### Task 3: False Prophet's When Played

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphet.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala:15-22`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala:9-19`
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `denizen.false-prophet`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/FalseProphetSuite.scala` (new)

**Interfaces:**
- Consumes: `RevealedVisions.of` (Task 2), the `OnCard(VisionId)` read
  (Task 2).
- Produces: `FalseProphetCard` (moved, same id `85`, same power),
  `FalseProphet: WhenPlayedPower` with `decisionId: String =
  "power.false-prophet.vision"` and `put: NoteKey`.

- [ ] **Step 1: Write the failing tests**

Create `FalseProphetSuite.scala`, following `WizardsConclaveSuite`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.RevealedVisions
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.powers.NoteText
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

class FalseProphetSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[FalseProphet.type]
  private val card = power.cardId
  private val played = hook(card)

  private def visionOptions(visions: VisionId*): Vector[DecisionOption] =
    visions.toVector.map(vision =>
      DecisionOption.Vision(DecisionOptionRef.Vision(vision)))

  private def chose(vision: VisionId): DecisionAnswer =
    chooseOne(DecisionOptionRef.Vision(vision))

  /** p1 holds False Prophet; p2 has revealed Faith. */
  private def faithRevealed: Table = Table.start.adviser(p1, card)
    .revealedVision(p2, VisionRules.Faith)

  test("an Exile gains a warband and puts it on another player's Vision, " +
      "which is then theirs too"):
    val ready = faithRevealed.ready
    val board = Look(ready).player(p1).board.warbands
    val first = Outcomes.parked(play(ready, power, card))
    val asked = question(ready, power, played, first.tree)
    assertEquals(asked.owner, p1)
    assertEquals(chooseOneOptions(asked), visionOptions(VisionRules.Faith))
    val done = Outcomes.finished(resume(ready, power, played, first.tree,
      FalseProphet.decisionId, chose(VisionRules.Faith)))
    val after = done.treeless
    assertEquals(Look(after).player(p1).board.warbands, board)
    assertEquals(after.game.current.warbandsOn(VisionRules.Faith,
      ForceKind.Exile(Look(after).player(p1).lineage)), 1)
    assertEquals(RevealedVisions.of(after, p1),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(said(power, done.events), Vector(NoteText.Said("put",
      s"Put a warband on ${p2.value}'s Vision of Faith.", covers = true)))
    assertJournalRebuilds(ready, done.events, after)

  test("every other player's revealed Vision is offered, never the " +
      "player's own"):
    val ready = faithRevealed.revealedVision(p1, VisionRules.Conquest)
      .revealedVision(p3, VisionRules.Sanctuary).ready
    val first = Outcomes.parked(play(ready, power, card))
    assertEquals(chooseOneOptions(question(ready, power, played, first.tree)),
      visionOptions(VisionRules.Faith, VisionRules.Sanctuary))

  test("a Citizen gains nothing"):
    val ready = faithRevealed.imperial(p3, p1).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(said(power, done.events), Vector.empty)

  test("an Exile with no warband in their bank gains nothing"):
    val ready = faithRevealed.warbandsInBank(p1, 0).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)

  test("with no other player's revealed Vision nothing happens"):
    val ready = Table.start.adviser(p1, card)
      .revealedVision(p1, VisionRules.Conquest).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)
```

Read `WhenPlayedHarness` for `chooseOne`, `chooseOneOptions`, `ops`,
`said` and `assertJournalRebuilds`, and `NoteText.Said` for how `covers`
is recorded; the put note is written with `covers = true`, as
`KeyToTheCity`'s placed note is. If `Look(...).player(p1).lineage` does
not exist, read the lineage from `ready.game.current.player(p1)`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.FalseProphetSuite"`
Expected: compile error: `FalseProphet` not found.

- [ ] **Step 3: Move the card and write the power**

Delete `FalseProphetCard` (lines 15-22) from `HoldingDiscordDenizens.scala`;
`ReliquaryRaidCard` stays. Create `whenplayed/FalseProphet.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._

object FalseProphetCard extends Denizen(DenizenId("85"), "False Prophet", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.false-prophet"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** if you're an Exile, gain one warband and " +
      "put it on any revealed Vision. You now also have it " +
      "revealed. If it is ever discarded, kill the warband and " +
      "play or discard the Vision as if you had searched.")
  val powers: Vector[PrintedPower] = Vector(power)

/** False Prophet's When Played (card 85; catalog batch 11, ruling 14 and
  * the False Prophet readings): an Exile with a warband in their bank
  * chooses another player's revealed Vision, gains one warband and puts it
  * on that Vision. The warband claims it: `RevealedVisions.of` counts it
  * as theirs too. With no such Vision, no warband or no Exile role it does
  * nothing and writes nothing. What a discard of the Vision does is
  * [[FalseProphetClaim]]'s.
  *
  * The Branch keeps its shape once the question is answered, so a walk
  * that resumes after the placement, when the bank may be empty, finds
  * the same nodes.
  */
case object FalseProphet extends WhenPlayedPower:
  val cardId: DenizenId = FalseProphetCard.id
  val id: PowerId = FalseProphetCard.power.id
  val decisionId: String = "power.false-prophet.vision"

  private val notes = NoteSet(id)
  /** "Put a warband on {Blue}'s {Vision of Faith}." */
  val put: NoteKey = notes.key("put", Vector(
    NotePart.Text("Put a warband on "), NotePart.Arg(0),
    NotePart.Text("'s "), NotePart.Arg(1), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    val actor = ctx.actor
    Vector(Branch((live, pending) =>
      if chosen(pending.answered).isEmpty && targets(live, actor).isEmpty
      then Vector.empty
      else Vector[Operation](question(live, actor), place(actor),
        Note(id, putNote, covers = true))))

  /** The Visions `actor` may put the warband on: every other player's
    * revealed Vision, in seat order, when `actor` is an Exile with a
    * warband in their bank. */
  private def targets(ready: ReadyGame, actor: PlayerId): Vector[VisionId] =
    val exile = Imperials.role(ready, actor).contains(Role.Exile)
    val banked = PlayerFacts.forceKind(ready, actor).toOption
      .exists(kind => PlayerFacts.banked(ready, kind) > 0)
    if !exile || !banked then Vector.empty
    else ready.game.current.players.filterNot(_.player == actor)
      .flatMap(_.revealedVisions.map(_.id))

  private def question(ready: ReadyGame, actor: PlayerId): Decide =
    Decide(decisionId, actor, DecisionQuery.ChooseOne(
      targets(ready, actor).map(vision =>
        DecisionOption.Vision(DecisionOptionRef.Vision(vision))),
      heading = Some("False Prophet: put a warband on a revealed Vision")))

  private def chosen(answered: Vector[Answered]): Option[VisionId] =
    PowerAnswers.last(answered, decisionId).collect {
      case DecisionOptionRef.Vision(vision) => vision }

  /** The gain and the move onto the chosen Vision. */
  private def place(actor: PlayerId): Operation = BuildOps((live, pending) =>
    chosen(pending.answered).fold[Either[OathViolation,
        Vector[CoreOperation]]](Right(Vector.empty))(vision =>
      PlayerFacts.forceKind(live, actor).map(kind => Vector(
        Gain.Warbands(actor, kind, 1),
        Move(Piece.Warbands(kind, 1),
          PositionedLocation(Location.PlayArea(actor)),
          PositionedLocation(Location.OnCard(vision)))))))

  private def putNote(states: NoteStates): Option[LogNote] = for
    vision <- chosen(states.answered)
    owner <- states.now.game.current.players
      .find(_.revealedVisions.exists(_.id == vision)).map(_.player)
  yield put(PowerSourceRef.Card(cardId), NoteArg.Player(owner),
    NoteArg.Card(vision))
```

Check the names against their uses: `Imperials.role` and `Role.Exile`
(`roles/RoleChanges.scala:54`), `PlayerFacts.banked`
(`powers/PlayerFacts.scala:28-34`), `Gain.Warbands`
(`ASmallFavor.scala:17-24`), `NoteStates.answered` and `NoteArg.Player`
(`ObsidianCage.cagedNote`), and `DecisionOption.Vision` (finding 10). If
`BuildOps` takes a different closure type, follow `KeyToTheCity.scala:101-108`.

Append `FalseProphet` to `WhenPlayedPowers.all`.

`unimplemented.txt`: delete the line `denizen.false-prophet`.

`declared-keys.txt`, in sorted position:

```text
denizen.false-prophet put = Put a warband on {0}'s {1}.
```

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.FalseProphetSuite oathdigital.gameplay.powers.UnimplementedPowersSuite oathdigital.gameplay.powers.DeclaredNotesSuite"`
Expected: PASS. If the declared-keys suite has another name, find it with
`grep -rln "declared-keys.txt" src/test/scala`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphet.scala \
  src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala \
  src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/whenplayed/FalseProphetSuite.scala
git commit -m "feat(powers): False Prophet puts a warband on another player's Vision" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: A claimed Vision's discard

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphetClaim.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:81-82`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala` (notes, `playedNote`)
- Modify: `src/main/scala/distantlands/gameplay/DesecratedGround.scala` (slice 6's `keptNote`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/FalseProphetClaimSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/FalseProphetLogSuite.scala` (new)

**Interfaces:**
- Consumes: `RevealedVisions.claimants` (Task 2), `FalseProphet.id` and
  `FalseProphetCard` (Task 3), `CardPlayProcedure.unchecked` and
  `Origin.DiscardPile` (`CardPlayProcedure.scala:45-47, 278-283`).
- Produces: `FalseProphetClaim: ExecutedTrigger` with
  `claimantDecisionId(vision: VisionId): String =
  s"power.false-prophet.claimant.${vision.value}"`, `killed: NoteKey`,
  `killedOwn: NoteKey`, `claimRule: PlacementLimit`.
- Produces: `CardPlayProcedure.revealedVisionOffTurn`,
  `discardedToOffTurn`, `discardedOffTurn` and
  `DesecratedGroundRule.keptOffTurn`.

- [ ] **Step 1: Write the failing walk tests**

Create `src/test/scala/distantlands/gameplay/FalseProphetClaimSuite.scala`,
in package `oathdigital.gameplay` so it reaches `SearchPlay` and
`PlacementFixture` as `VisionPlaySuite` does:

```scala
package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.powers.whenplayed.FalseProphetClaim
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker}
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, Outcomes, PlacementPicks, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** A claimed Vision's discard (catalog batch 11, rulings 14, 32 and 33):
  * p1 owns Conquest, which p2 claims with a warband. p1 stands at Ancient
  * City, in the Cradle, so p1's discards go to the Provinces. */
class FalseProphetClaimSuite extends munit.FunSuite:
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = walkerPowers)
  private val parked = new ParkedDecisionAssertions(catalog, walkerPowers)
  private val claimed = VisionRules.Conquest
  private val drawn = VisionRules.Faith

  private def placeId(card: VisionId): String =
    CardPlayProcedure.placementDecisionId(card)

  /** p1 at Ancient City owns Conquest, which p2 claims; Faith tops the
    * world deck. */
  private def claimedTable: Table = Table.start.pawn(p1, at = AncientCitySite)
    .pawn(p2, at = BrokenPeaksSite)
    .revealedVision(p1, claimed).warbandsOnCard(claimed, p2, 1)
    .worldDeckTop(drawn)

  /** `table`'s p1 Searches and reveals Faith over Conquest: the walk parks
    * on what the discard asks next. */
  private def revealedOverClaim(table: Table): OathState =
    val searched = accepted(SearchPlay.searched(rules, Ready(table.ready), p1))
    accepted(rules.resolveWalker(searched.state, p1, placeId(drawn),
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))
      .state

  private def answer(state: OathState, by: PlayerId, decisionId: String,
      ref: DecisionOptionRef): OathState =
    accepted(rules.resolveWalker(state, by, decisionId,
      DecisionAnswer.ChooseOneAnswer(ref))).state

  private def warbandsOnClaimed(state: OathState, owner: PlayerId): Int =
    val ready = Outcomes.ready(state)
    ready.game.current.warbandsOn(claimed,
      ForceKind.Exile(Look(ready).player(owner).lineage))

  test("the owner's reveal over a claimed Vision asks the claimant, who " +
      "reveals it; their warband is killed"):
    val asked = revealedOverClaim(claimedTable)
    parked.assertParked(asked, ActionRef.Search, placeId(claimed), p2)
    val done = answer(asked, p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    val after = Outcomes.ready(done)
    assertEquals(Look(after).player(p2).revealedVisions.map(_.id),
      Vector[VisionId](claimed))
    assertEquals(Look(after).player(p1).revealedVisions.map(_.id),
      Vector[VisionId](drawn))
    assertEquals(warbandsOnClaimed(done, p2), 0)

  test("the claimant may discard it, and may not play it facedown"):
    val asked = revealedOverClaim(claimedTable)
    val offered = PlacementPicks.parkedOptions(asked, walkerPowers)
    val facedown = offered.collectFirst {
      case DecisionOption.Unavailable(inner, reason)
          if inner.ref.isInstanceOf[DecisionOptionRef.AdviserPlay] => reason }
    assertEquals(facedown, Some(OptionReason.Written(
      FalseProphetClaim.toFaceupOrDiscard)))
    val discard = offered.map(_.ref).collectFirst {
      case pile: DecisionOptionRef.DiscardPile => pile }
    val done = answer(asked, p2, placeId(claimed), discard.get)
    assert(Look(Outcomes.ready(done)).player(p2).revealedVisions.isEmpty)
    assertEquals(warbandsOnClaimed(done, p2), 0)

  test("a Citizen claimant can only discard it"):
    val asked = revealedOverClaim(claimedTable.imperial(p3, p2))
    val offered = PlacementPicks.parkedOptions(asked, walkerPowers)
    assertEquals(offered.filter(_.available).map(_.ref).collect {
      case pile: DecisionOptionRef.DiscardPile => pile }.size, 1)
    assertEquals(offered.count(_.available), 1)

  test("the owner turning Citizen discards the claimed Vision and asks " +
      "the claimant on the owner's turn"):
    val ready = claimedTable.imperial(p3).turn(p1, Phase.Act).ready
    val outcome = Outcomes.parked(ProcedureWalker.advance(ready,
      Citizenship.become(p1), None, walkerPowers))
    val decide = present(ProcedureWalker.parkedDecide(ready,
      Citizenship.become(p1), outcome.tree, walkerPowers), "the question")
    assertEquals((decide.decisionId, decide.owner), (placeId(claimed), p2))

  test("with two claimants the current player chooses who plays it, and " +
      "both warbands are killed"):
    val asked = revealedOverClaim(claimedTable.warbandsOnCard(claimed, p3, 1))
    val chooser = FalseProphetClaim.claimantDecisionId(claimed)
    parked.assertParked(asked, ActionRef.Search, chooser, p1)
    val chosen = answer(asked, p1, chooser, DecisionOptionRef.Player(p3))
    parked.assertParked(chosen, ActionRef.Search, placeId(claimed), p3)
    val done = answer(chosen, p3, placeId(claimed),
      DecisionOptionRef.RevealedVision(p3))
    assertEquals(Look(Outcomes.ready(done)).player(p3).revealedVisions
      .map(_.id), Vector[VisionId](claimed))
    assertEquals(warbandsOnClaimed(done, p2), 0)
    assertEquals(warbandsOnClaimed(done, p3), 0)

  test("the claimant's reveal is judged for the claimant"):
    // Sacred Ground stands at p1's Ancient City: p1 may reveal there,
    // p2 at Broken Peaks may not.
    val table = claimedTable.edifice(SacredGroundCard.id, EdificeSide.Intact,
      at = AncientCitySite.id)
    val offered = PlacementPicks.parkedOptions(revealedOverClaim(table),
      walkerPowers)
    assert(!offered.filter(_.available).map(_.ref)
      .contains(DecisionOptionRef.RevealedVision(p2)), offered.toString)

  test("the claimant's reveal at Desecrated Ground keeps their Visions"):
    val table = claimedTable.revealedVision(p2, VisionRules.Sanctuary)
      .edifice(SacredGroundCard.id, EdificeSide.Ruined,
        at = BrokenPeaksSite.id)
    val done = answer(revealedOverClaim(table), p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    assertEquals(Look(Outcomes.ready(done)).player(p2).revealedVisions
      .map(_.id), Vector[VisionId](VisionRules.Sanctuary, claimed))
```

Check these names before running: `ParkedDecisionAssertions.assertParked`'s
parameters (`VisionPlaySuite:96-97`), `PlacementPicks.parkedOptions` (`PlacementPicks.scala:45`), `ProcedureWalker.parkedDecide`'s
arguments (`VisionPlaySuite:63-64`), and `Look(...).player(p).revealedVisions`
after slice 6. The Search may draw more than one card with the quiet
table's Supply: if it does, stack the deck as `VisionPlaySuite` does, or
set p1's Supply as `DesecratedGroundLogSuite` does. If the role-change
test's p1 is offered the purple-bank split first, give the purple bank
room (`CitizenshipSuite.exileWith`).

- [ ] **Step 2: Write the failing log test**

Create `src/test/scala/distantlands/application/gamelog/FalseProphetLogSuite.scala`,
following slice 6's `DesecratedGroundLogSuite`:

```scala
package oathdigital.application.gamelog

import oathdigital.application.{GameApplicationService,
  ParkedServiceFixture, TreeDecision}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.model._
import oathdigital.testkit.{Commands, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}
import LogScripts.formatter

/** False Prophet's lines as the Game Log shows them (catalog batch 11,
  * ruling 34). */
class FalseProphetLogSuite extends munit.FunSuite:
  /** p1 owns Conquest, which p2 claims; p1 Searches, reveals Faith over
    * it, and p2 reveals Conquest. The log as p3 sees it, replayed from
    * the stored history. */
  private def claimRevealed: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).supply(p1, 7)
      .revealedVision(p1, VisionRules.Conquest)
      .warbandsOnCard(VisionRules.Conquest, p2, 1)
      .worldDeckTop(VisionRules.Faith)
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    val faith = accepted(service.handle("g", picked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Faith),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))))
    accepted(service.handle("g", faith.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Conquest),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))))
    texts(service)

  private def texts(service: GameApplicationService): Vector[String] =
    formatter.format(present(accepted(service.history("g")),
      "a stored game").steps, Some(p3)).map(LogScripts.text)

  test("the claimant's reveal names them, then the kill names them"):
    val log = claimRevealed
    val reveal = log.indexWhere(_.endsWith(" revealed Vision of Conquest"))
    assert(reveal >= 0, log.toString)
    val killed = log.lift(reveal + 1).getOrElse("")
    assert(killed.startsWith("False Prophet: Killed "), log.toString)
    assert(killed.endsWith("'s warband on Vision of Conquest."), log.toString)
    assert(!log.exists(_.startsWith("Revealed Vision of Conquest")),
      log.toString)
```

The line is matched by its ends because the formatter renders a player by
their colour name; if `LogScripts` has a helper that renders a player's
name, assert the whole line with it instead.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.FalseProphetClaimSuite oathdigital.application.gamelog.FalseProphetLogSuite"`
Expected: compile error: `FalseProphetClaim` not found.

- [ ] **Step 4: Write the trigger**

Create `whenplayed/FalseProphetClaim.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.{RevealedVisions, TurnOrder}
import oathdigital.gameplay.actions.PlacementLimit
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver._
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** False Prophet's claim, when its Vision is discarded (card 85; catalog
  * batch 11, rulings 14, 32 and 33): "If it is ever discarded, kill the
  * warband and play or discard the Vision as if you had searched."
  *
  * After a batch that discards a Vision carrying warbands, the follow-up,
  * for each such Vision in the batch's order:
  *
  *  1. asks the current player which claimant plays it, when there are
  *     several (rule 15.3; ruling 32);
  *  2. nests the claimant's card play of the Vision from the pile it went
  *     to, made by this power, which Search Party's play also is;
  *     `claimRule` leaves it faceup or discard (ruling 33), and the hook
  *     names the claimant, so the Vision rules judge the play for them;
  *  3. kills every claimant's warband on it, each with its line.
  *
  * A command that resumes inside the follow-up asks again with the held
  * operations and the parked state, and must get the same tree
  * (`ExecutedTrigger`). The claimants are read from the warbands on the
  * Vision, so the kill comes last; and the pile from the executed
  * discard, since the claimant's play moves the card.
  */
case object FalseProphetClaim extends ExecutedTrigger:
  val cardId: DenizenId = FalseProphetCard.id
  val id: PowerId = FalseProphetCard.power.id
  val toFaceupOrDiscard: String =
    "False Prophet: reveal the Vision or discard it"

  def claimantDecisionId(vision: VisionId): String =
    s"power.false-prophet.claimant.${vision.value}"

  /** Only the faceup play, or the discard, stays open (ruling 33). */
  val claimRule: PlacementLimit = PlacementLimit(
    OptionReason.Written(toFaceupOrDiscard), {
      case SearchPlacement.Adviser(Orientation.FaceDown, _) => false
      case _ => true
    })

  private val notes = NoteSet(id)
  /** "Killed {Blue}'s warband on {Vision of Faith}." */
  val killed: NoteKey = notes.key("killed", Vector(
    NotePart.Text("Killed "), NotePart.Arg(0),
    NotePart.Text("'s warband on "), NotePart.Arg(1), NotePart.Text(".")))
  /** "Killed the warband on {Vision of Faith}.", the walk's actor's. */
  val killedOwn: NoteKey = notes.key("killed.own", Vector(
    NotePart.Text("Killed the warband on "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree
            if tree.byPower.contains(id) =>
          tree.adjust(children)(_.limitPlacements(claimRule))
        case _ => children)))

  def onExecuted(batch: ExecutedBatch): Option[Operation] =
    val claims = discarded(batch.executed).flatMap((vision, pile) =>
      Option.when(RevealedVisions.claimants(batch.now, vision).nonEmpty)(
        (vision, pile)))
    Option.when(claims.nonEmpty)(Sequence(claims.map((vision, pile) =>
      Sequence(follow(batch, vision, pile)))))

  /** Each Vision the batch discarded, with the pile it went to, at any
    * depth of the batch's operations. */
  private def discarded(executed: Vector[CoreOperation])
      : Vector[(VisionId, Region)] =
    def all(operation: Operation): Vector[Operation] =
      operation +: operation.children.flatMap(all)
    executed.flatMap(all).collect {
      case discard: Discard.Vision => (discard.card, discard.to) }.distinct

  private def follow(batch: ExecutedBatch, vision: VisionId, pile: Region)
      : Vector[Operation] =
    val claimants = RevealedVisions.claimants(batch.now, vision)
    val current = batch.now.game.current.turn.activePlayer
    val chooserId = claimantDecisionId(vision)
    // In turn order from the current player, who chooses (rule 15.3).
    val ordered = rotated(TurnOrder.order(batch.now), current)
      .filter(claimants.contains)
    val choose = Option.when(ordered.size > 1)(Decide(chooserId, current,
      DecisionQuery.ChooseOne(ordered.map(player =>
        DecisionOption.Player(DecisionOptionRef.Player(player))),
        heading = Some("False Prophet: choose who plays the Vision")))).toVector
    def player(pending: PendingTree): Option[PlayerId] =
      if ordered.size == 1 then ordered.headOption
      else PowerAnswers.one(pending, chooserId).collect {
        case DecisionOptionRef.Player(chosen) => chosen }
    val play = Branch((now, pending) => player(pending).toVector.map(claimant =>
      CardPlayProcedure.unchecked(batch.catalog, now, claimant, vision,
        CardPlayProcedure.Origin.DiscardPile(pile), byPower = Some(id))))
    val kills = claimants.flatMap(claimant => Vector[Operation](
      BuildOps((live, _) => Right(live.game.current.player(claimant).toVector
        .flatMap { state =>
          val kind = ForceKind.Exile(state.lineage)
          val count = live.game.current.warbandsOn(vision, kind)
          Option.when(count > 0)(Kill(Piece.Warbands(kind, count),
            PositionedLocation(Location.OnCard(vision)))).toVector
        })),
      Note(id, killedNote(claimant, vision), covers = true)))
    choose ++ Vector(play) ++ kills

  private def rotated(players: Vector[PlayerId], first: PlayerId)
      : Vector[PlayerId] =
    val index = math.max(0, players.indexOf(first))
    players.drop(index) ++ players.take(index)

  private def killedNote(claimant: PlayerId, vision: VisionId)(
      states: NoteStates): Option[LogNote] =
    val card = PowerSourceRef.Card(cardId)
    Some(if claimant == states.now.game.current.walkActor then
      killedOwn(card, NoteArg.Card(vision))
    else killed(card, NoteArg.Player(claimant), NoteArg.Card(vision)))
```

Check these names: `PlacementLimit`'s package (`gameplay/actions/CardPlayRules.scala:63`),
`Discard.Vision`'s fields `card` and `to` (`model/CoreOperations.scala:176-183`),
`Kill`'s parameters (`CoreOperations.scala:385-389`),
`current.turn.activePlayer` and `walkActor` (`model/GameState.scala:105, 311`),
`TurnOrder.order` (`gameplay/TurnOrder.scala:15`), and how `BrassArmyMoves`
declares `source` and `contributions` (`travel/BrassArmyMoves.scala:48-49`).
`TurnOrder.order` starts at the round starter, which is why the list is
rotated to start at the current player. If `ExecutedTrigger` requires
`applicable`, keep the default.

Register it in `WalkerPowerCatalog.scala`, in the vector that holds
`GrandMask` (and slice 6's `DesecratedGround`):

```scala
      Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown,
        GrandMask, DesecratedGround, FalseProphetClaim) ++
```

The catalog imports `whenplayed._` already if `ASmallFavor` resolves
unqualified; otherwise add the import.

- [ ] **Step 5: Write the off-turn card-play lines**

In `CardPlayProcedure.scala`, after `playedFacedownAdviser`:

```scala
  /** "{Blue} revealed Vision of Faith": a reveal by a player who is not
    * the walk's actor (False Prophet's claimant, catalog batch 11,
    * ruling 34). */
  val revealedVisionOffTurn: NoteKey = notes.key("revealed-vision-off-turn",
    Vector(NotePart.Arg(0), NotePart.Text(" revealed "), NotePart.Arg(1)))
  /** "{Blue} discarded Vision of Faith to the Provinces discard". */
  val discardedToOffTurn: NoteKey = notes.key("discarded-to-off-turn",
    Vector(NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
      NotePart.Text(" to the "), NotePart.Arg(2), NotePart.Text(" discard")))
  /** "{Blue} discarded Vision of Faith", outside any regional pile. */
  val discardedOffTurn: NoteKey = notes.key("discarded-off-turn", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1)))
```

Change `playedNote` to take the player and choose the variant. Only a
reveal and a discard are made off turn (ruling 33), so the site and
adviser lines keep one form:

```scala
  private def playedNote(card: WorldCardId, actor: PlayerId,
      ref: DecisionOptionRef)(states: NoteStates): Option[LogNote] =
    val offTurn = actor != states.now.game.current.walkActor
    val who = NoteArg.Player(actor)
    playedAs(ref).flatMap {
      case PlayedAs.Discarded => Some(discardPile(states.now, card).fold(
        if offTurn then discardedOffTurn.sourceless(who, NoteArg.Card(card))
        else discarded.sourceless(NoteArg.Card(card)))(region =>
        if offTurn then discardedToOffTurn.sourceless(who,
          NoteArg.Card(card), NoteArg.RegionName(region))
        else discardedTo.sourceless(NoteArg.Card(card),
          NoteArg.RegionName(region))))
      case PlayedAs.AtSite => siteOf(ref).map(site =>
        playedAtSite.sourceless(NoteArg.Card(card), NoteArg.Site(site)))
      case PlayedAs.Faceup if card.isInstanceOf[VisionId] =>
        Some(if offTurn then revealedVisionOffTurn.sourceless(who,
          NoteArg.Card(card))
        else revealedVision.sourceless(NoteArg.Card(card)))
      case PlayedAs.Faceup =>
        Some(playedFaceupAdviser.sourceless(NoteArg.Card(card)))
      case PlayedAs.Facedown =>
        Some(playedFacedownAdviser.sourceless(NoteArg.Card(card)))
    }
```

Pass `actor` at both call sites, `played` and `settled`:
`playedNote(card, actor, slot.ref)` and `playedNote(card, actor, ref)`.

In slice 6's `DesecratedGroundRule`, add the off-turn key and choose it
in `keptNote`:

```scala
  /** "Desecrated Ground: {Blue} kept Vision of Faith.", for a player who
    * is not the walk's actor (ruling 34). */
  val keptOffTurn: NoteKey = notes.key("kept-off-turn", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(".")))
```

and in `keptNote`'s `yield`:

```scala
    yield
      val source = PowerSourceRef.Card(SacredGroundCard.id)
      if owner == states.now.game.current.walkActor then
        kept(source, NoteArg.Cards(others))
      else keptOffTurn(source, NoteArg.Player(owner), NoteArg.Cards(others))
```

`declared-keys.txt`, in sorted position:

```text
denizen.false-prophet killed = Killed {0}'s warband on {1}.
denizen.false-prophet killed.own = Killed the warband on {0}.
edifice.e08.ruined kept-off-turn = {0} kept {1}.
procedure.play-facedown-adviser discarded-off-turn = {0} discarded {1}
procedure.play-facedown-adviser discarded-to-off-turn = {0} discarded {1} to the {2} discard
procedure.play-facedown-adviser revealed-vision-off-turn = {0} revealed {1}
```

Add a test for the off-turn kept line to `FalseProphetClaimSuite`'s
Desecrated Ground test, reading the line with `NoteText.said(...)` on
`DesecratedGroundRule`'s keys as slice 6's `DesecratedGroundSuite` does:
the line is `s"${p2.value} kept Vision of Sanctuary."`.

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.FalseProphetClaimSuite oathdigital.application.gamelog.FalseProphetLogSuite oathdigital.application.gamelog.DesecratedGroundLogSuite oathdigital.gameplay.VisionPlaySuite oathdigital.gameplay.powers.DeclaredNotesSuite"`
Expected: PASS.

Run the walker and card-play suites in full (the spec asks it of slices
4 to 7): `./sbtw "testOnly oathdigital.gameplay.walker.* oathdigital.gameplay.actions.* oathdigital.gameplay.powers.cardplay.* oathdigital.gameplay.powers.whenplayed.*"`
Expected: PASS.

If the walk tests park somewhere else, read finding 5 first: the
follow-up must give the same tree from the parked state. Print the
parked path (`state.game.current.walkerPending`) before changing the
tree's shape.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/FalseProphetClaim.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala \
  src/main/scala/distantlands/gameplay/DesecratedGround.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/FalseProphetClaimSuite.scala \
  src/test/scala/distantlands/application/gamelog/FalseProphetLogSuite.scala
git commit -m "feat(powers): a claimed Vision's discard lets its claimant play it" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint review** after this task: dispatch a reviewer with Tasks 3
and 4's diff, findings 5 to 9, rulings 14 and 32 to 34, and
`ExecutedTrigger.scala`'s doc.

---

### Task 5: Close the slice and the batch

**Files:**
- Modify: `docs/ROADMAP.md:87-97, 429-431`
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md:3`
- Modify: `docs/architecture/bounded-visions-and-conspiracy.md:63-84`
- Modify: `docs/rules/implementation-traceability.md:118`

- [ ] **Step 1: Update the documents**

`docs/ROADMAP.md`: slice 7 empties **Phase - Catalog batch -
engine-blocked** (the spec's "Out of scope"). Delete the whole phase, the
heading through the False Prophet line (and slice 6's Desecrated Ground
line, if slice 6 left it). In L6 (about line 429-431), replace "the next
denizens and relics come from **Phase - Catalog batch - engine-blocked**"
with "the remaining cards come with **Phase - Negotiation terms and
promises** and **Phase - Chronicle steps**". Finished work is deleted, not
marked.

Spec status line 3: `**Status:** designed 2026-11-07. Slices 0 to 7
landed.`

`bounded-visions-and-conspiracy.md`: change "Three cards forbid a faceup
Vision play and one rewards it. Each is a power on the card-play hook
`ActionCardPlayedFaceup`:" to end "... `ActionCardPlayedFaceup`, judged
for the player the hook names:". After slice 6's Desecrated Ground
paragraph, add:

```markdown
False Prophet (`denizen.false-prophet`) puts a warband on another
player's revealed Vision, which then counts as the warband owner's too
(`RevealedVisions.of`; a Citizen keeps the claim but cannot win with it).
When that Vision is discarded, the current player chooses among several
claimants, the chosen one reveals or discards it inside the walk that
discarded it, and every claimant's warband is killed
(`FalseProphetClaim`; catalog batch 11, slice 7). The play's hook names
the claimant, so the rules above judge the reveal for them.
```

`implementation-traceability.md:118`: add to the evidence column "False
Prophet's claim counts for victory and its discard nests the claimant's
play (`FalseProphetSuite`, `FalseProphetClaimSuite`)." and, in the last
column, drop any mention of False Prophet as deferred if one exists.

- [ ] **Step 2: Run every check**

Run: `./sbtw test`
Expected: PASS, all projects.

Run: `python3 scripts/check-architecture.py`
Expected: no violation. `CardPlayProcedure.scala` grows by about 30
lines from slice 6's size; check it stays under 800 (`wc -l`).

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken link (the deleted ROADMAP phase may be linked from
elsewhere: `grep -rn "engine-blocked" docs`).

Run the coverage command the project uses (`grep -n "coverage"
scripts/*.sh build.sbt README.md | head`).
Expected: statement coverage at or above 92%.

- [ ] **Step 3: Commit**

```bash
git add docs/ROADMAP.md \
  docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md \
  docs/architecture/bounded-visions-and-conspiracy.md \
  docs/rules/implementation-traceability.md
git commit -m "docs(catalog): close catalog batch 11 slice 7, False Prophet" \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Known limits

- **A claim discarded inside a follow-up is not resolved.** A follow-up's
  own batches start no follow-up (finding 6). When the claimant's own
  reveal discards a Vision a third player claims, that warband stays on
  the discarded card, claiming nothing, until the Chronicle returns it.
- **The kill line follows the play's lines,** and the owner's reveal
  line follows both (findings 5 and 9).
- **A later claimant gets no second chance.** Ruling 32 gives the Vision
  to one claimant; the others' warbands die even when that claimant
  discards it.
- **Only the reveal and discard lines have off-turn variants.** No other
  card play happens off turn.
- **The claimant's own board shows no claim.** The owner's pill shows the
  warband in the claimant's colour.
