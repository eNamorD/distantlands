# Last Chronicle Powers, Slice 2: Start of Throne Secrecy and Hiding Place Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The held relics turn facedown at the start of the Throne instead
of at Stock Reliquary, and Hiding Place (E26 ruined) lets its site's ruler
keep one of them, secretly, for the next game.

**Architecture:** `ReliquaryAndRelics` splits its hide step out of `stock`
into `hide`, which becomes the base step of the `ChronicleStartOfThrone`
window. Heirloom's keep moves into a shared `RelicKeep` object. The
object returns the relic's secrets and any warbands on it, keeps the relic
with `StartingRelicKept`, and writes the public and private lines, which
name the card that kept the relic. `HidingPlace` is a `ContributingPower`
that appends a step to `ChronicleStartOfThrone`. The step asks the site's
ruling player to choose a relic or Decline, then calls `RelicKeep`. Its
priority of 1 folds it after Reformer.

**Why the priority.** Transforms of one phase fold by
`ContributingPower.sortKey`: priority, then the source's `stableKey`, then
the id. Both sources are `RuleSourceRef.GameRule`, whose key is
`game:<id>`, and `game:edifice.e26.ruined` sorts before
`game:legacy.reformer`. With the default priority, Hiding Place would fold
first and its step would run before Reformer's. Ruling 6 orders Reformer
first, so `HidingPlace.priority` is 1. Slice 3 gives Blasted Pastures 2.

**Why the keys are renamed.** Heirloom's `heirloom-kept` and
`heirloom-kept-named` lines already name their card as an argument, so
Hiding Place's lines would be the same words. `RelicKeep` owns one pair,
`relic-kept` and `relic-kept-named`, which both powers write. The text of
every line is unchanged. Old journals need not replay (no old replay
support), so renaming a key is allowed.

**Why `StartingRelicKept` gains a guard.** Heirloom keeps after Stock
Reliquary, which has already returned every warband standing on a card.
Hiding Place keeps before Stock Reliquary, so a relic it keeps, such as
Obsidian Cage, may still carry warbands. `RelicKeep` returns them first.
The operation now refuses a relic with warbands on it, as it already
refuses one with tokens, so a keep that forgot them fails loudly.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Last Chronicle powers design](../../specs/2026-11/2026-11-16-last-chronicle-powers-design.md),
rulings 3, 4 and 6, and section "Slice 2: Start of Throne secrecy and
Hiding Place".

## Global Constraints

- Ruling 3: the step that turns every relic players hold but the Grand
  Scepter facedown, and clears every viewer's record of it, runs at the
  start of the Throne, before any power of that window. Each holder still
  sees their own relics. Warbands on cards still return to their supply at
  Stock Reliquary.
- Ruling 4: Hiding Place's ruler is `SiteRule.rulingPlayer(state, site)`
  when it is `SiteRuler.Player`: a player's warbands, or the Chancellor
  for an Imperial site. The Grand Mask does not count. When bandits rule
  the site, nobody rules it, or the Empire has no Chancellor, nothing is
  asked.
- Ruling 4: the ruler may keep any relic they hold but the Grand Scepter.
  They choose one or Decline, even with a single relic. Decline is the
  first option.
- Ruling 4: the kept relic's secrets return to the ruler, facedown. Any
  warbands on it go back to their supply, as Stock Reliquary returns
  warbands on cards (owner's ruling, 2026-11-03). Then `StartingRelicKept`
  keeps it.
- Ruling 4: lines "p2 kept a relic with Hiding Place" for every viewer and
  "p2 kept Cup of Plenty with Hiding Place" for the ruler alone.
- Ruling 6: at the start of the Throne the relics are hidden, then
  Reformer acts, then Hiding Place.
- No new operation and no wire change. The new decision uses existing
  query and option types.
- Mechanics are never inferred from rules text or handler-name fragments
  (`docs/architecture/all-exile-round-endings.md`).
- Docs, comments and commits are plain English prose, lines under 80
  columns in Scala. Commits end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Stage files by name. Never `git add -A`, `-u`, `.`, `--all`, never
  `git commit -a`, never `git stash`.

## Files

- Modify: `src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala`.
  `hide` (the base step of the start of the Throne), `warbandsOff` (one
  card's warbands back to their supply); `stock` loses the hide.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`.
  The start-of-Throne window runs `ReliquaryAndRelics.hide`; `noteKeys`
  adds `RelicKeep.noteKeys`.
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`.
  Doc comments of `RelicsHidden` and `StartingRelicKept`.
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`. Doc
  comment of `ChronicleStartOfThrone`.
- Create: `src/main/scala/distantlands/gameplay/chronicle/RelicKeep.scala`.
  The shared keep and its two note keys.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala`.
  Calls `RelicKeep`; its own keys and keep go.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`.
  `throneNoteKeys` drops `Heirloom.throneNoteKeys`.
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`.
  `startingRelicKept` refuses a relic with warbands on it.
- Create: `src/main/scala/distantlands/gameplay/chronicle/HidingPlace.scala`.
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`.
  Registers `HidingPlace`.
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/HallowedSpring.scala`.
  Doc comment: the ruined face is implemented.
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt`.
- Create: `src/test/scala/distantlands/gameplay/chronicle/HidingPlaceSuite.scala`,
  `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`,
  `src/test/resources/gamelog/throne-hiding-place.actor.log`,
  `src/test/resources/gamelog/throne-hiding-place.other.log` (written by
  the golden suite).
- Modify: `src/test/scala/distantlands/application/HiddenChronicleCardsSuite.scala`,
  `src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala`,
  `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`,
  `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`.
- Modify (docs): `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`,
  `docs/rules/rules-reference.md`, `docs/rules/implementation-traceability.md`,
  `docs/ROADMAP.md`,
  `docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md`.

## Running things

- This plan starts from slice 1. Before Task 1, check that slice 1 is
  merged: `git log 0.2.0-alpha --oneline | grep -i "hall of bandits"`
  must print a commit. If it prints nothing, stop and report. Then, in
  this worktree, run `git rebase 0.2.0-alpha`. The branch holds only this
  plan's commit, so the rebase is clean.
- Until the rebase, the link check reports this plan's links to
  `ambiguities.md#last-chronicle-powers-rulings`, which slice 1 creates.
  After it, Task 5's gate must pass with no missing anchor.
- The worktree needs `.tooling` and `node_modules` symlinked from the main
  checkout before `./sbtw`. They are already linked.
- One suite: `./sbtw "testOnly distantlands.gameplay.chronicle.HidingPlaceSuite"`.
- Whole suite: `./sbtw test` takes about 7 minutes. Run it in the
  background.
- Declared-keys snapshot: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`.
- Log goldens: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
- Gates: `python3 scripts/check-architecture.py` and
  `python3 scripts/check-markdown-links.py`.

---

### Task 1: Hide the relics at the start of the Throne

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
- Test: `src/test/scala/distantlands/application/HiddenChronicleCardsSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala`

**Interfaces:**
- Produces: `ReliquaryAndRelics.hide: Vector[Operation]`, the base step of
  `ChronicleStartOfThrone`.
- Produces: `ReliquaryAndRelics.warbandsOff(ready: ReadyGame, card: CardId): Vector[CoreOperation]`,
  the moves that return every warband standing on `card` to its supply.
  Task 2 calls it.

- [ ] **Step 1: Write the failing test**

In `src/test/scala/distantlands/application/HiddenChronicleCardsSuite.scala`,
extend the imports:

```scala
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.{Heirloom, HeirloomCard,
  Reformer, ReformerCard, TheBrightBeacon, TheBrightBeaconCard, TheEar,
  TheEarCard}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
```

(replacing the existing `AncientCitySite` and legacies imports). Add this
test before the test named "Stock Reliquary hides every held relic but
the Grand Scepter...":

```scala
  test("the start of the Throne hides every held relic but the Grand " +
      "Scepter before Reformer's question: its holder still names their " +
      "own, and no other seat or the public does (ruling 3)"):
    // p1 won and is the Chancellor; the Empire rules Ancient City and
    // Broken Peaks, so Reformer has a card to move.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id)
    assert(relics.forall(presentation.identifiesAt(table.ready, Some(p1), _)))
    val atReformer = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(
      rules, table, TriggeredProcedureRef.ChronicleSun), Reformer.pickId(p1))
    assertEquals(ChronicleWalk.parked(atReformer.state).map(_.decisionId),
      Some(Reformer.pickId(p1)))
    val ready = Outcomes.ready(atReformer.state)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.application.HiddenChronicleCardsSuite"`
Expected: the new test FAILS on the `Vector(Some(p1), Some(p3), None)`
assertion, since the relics are still faceup at Reformer's question. The
other tests pass.

- [ ] **Step 3: Move the hide step**

In `src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala`,
replace the doc comment and value of `stock`, and `returnFromCards`, with:

```scala
  /** The base step at the start of the Throne (Last Chronicle powers
    * design, ruling 3): every relic players hold but the Grand Scepter
    * turns facedown and is forgotten by every viewer, so no relic is
    * deducible from here on; each owner still sees their own (Legacy
    * Chronicle powers R28, re-ruled by the owner 2026-11-14). It runs
    * before any power of the window, so a relic Hiding Place keeps is
    * kept secretly. */
  val hide: Vector[Operation] = Vector(
    BuildOps((state, _) => Right(hidden(state)), required = true))

  /** Warbands on a card go back to their supply, then the Throne holder
    * adds every relic they hold but the Grand Scepter to the Reliquary,
    * facedown, and discards down to five of their choice. */
  val stock: Vector[Operation] = Vector(
    BuildOps((state, _) => Right(returnFromCards(state)), required = true),
    Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder => Vector(
      BuildOps((live, _) => Right(held(live, holder).map(relic =>
        Move(Piece.Card(relic.id),
          PositionedLocation(Location.PlayArea(holder)),
          PositionedLocation(Location.Reliquary)))), required = true),
      Branch((live, _) => discardDown(live.game.campaign.reliquary, holder))))))

  /** Every warband standing on a card (Obsidian Cage) back to its supply,
    * since the relics leave their holders here and the game has ended
    * (owner's ruling, 2026-11-03). Nothing is asked. */
  private def returnFromCards(ready: ReadyGame): Vector[CoreOperation] =
    ready.game.current.warbandsOnCards.keys.toVector
      .flatMap(warbandsOff(ready, _))

  /** Every warband standing on `card` back to its supply. */
  def warbandsOff(ready: ReadyGame, card: CardId): Vector[CoreOperation] =
    ready.game.current.warbandsOnCards.get(card).toVector.flatMap(forces =>
      forces.toVector.filter(_._2 > 0).map((kind, n) =>
        Move(Piece.Warbands(kind, n),
          PositionedLocation(Location.OnCard(card)),
          PositionedLocation(Location.WarbandBank(kind)))))
```

Change the object's doc comment to:

```scala
/** The Throne's hidden relics, Stock Reliquary and Scatter Relics (CR
  * p. 37): the hide at the start of the Throne, then its fourth and fifth
  * steps. */
```

If `warbandsOnCards`' key type is not `CardId`, use its key type for
`card` and keep `Location.OnCard(card)` compiling; report the change.

In `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`,
replace

```scala
    ChronicleWindows.step(PowerWindow.ChronicleStartOfThrone, Vector.empty),
```

with

```scala
    ChronicleWindows.step(PowerWindow.ChronicleStartOfThrone,
      ReliquaryAndRelics.hide),
```

and replace the object's doc comment with:

```scala
/** The Throne task (CR p. 37, `docs/rules/chronicle.md` task 2), resolved
  * by the player holding its card. Each step is its own power window.
  * The window at the start hides the held relics (Last Chronicle powers
  * design, ruling 3), then holds Reformer's step; two empty windows hold
  * the other legacies' added steps: one after Build Edifice, where
  * Revolutionary acts, and one before Scatter Relics, where Heirloom does
  * (Legacy Chronicle powers design, rulings 11-13). */
```

- [ ] **Step 4: Update the doc comments the move touches**

In `src/main/scala/distantlands/model/ChronicleOperation.scala`, in the
`RelicsHidden` doc comment, replace "at the start of Stock Reliquary
(Legacy" with "at the start of the Throne (Last Chronicle powers design,
ruling 3; Legacy", re-wrapping to stay under 80 columns.

In `src/main/scala/distantlands/model/PowerWindow.scala`, replace

```scala
  /** The start of the Throne, before Offer Citizenship: Reformer's step
    * (Legacy Chronicle powers design, ruling 12). Empty with no power. */
```

with

```scala
  /** The start of the Throne, before Offer Citizenship: the held relics
    * are hidden (Last Chronicle powers design, ruling 3), then Reformer's
    * step (Legacy Chronicle powers design, ruling 12). */
```

- [ ] **Step 5: Rename the tests that named Stock Reliquary**

In `HiddenChronicleCardsSuite.scala`, rename

- "Stock Reliquary hides every held relic but the Grand Scepter: at " +
  "Heirloom's question its holder still names their own, and no other " +
  "seat or the public does (R28)" to "the start of the Throne hides every " +
  "held relic but the Grand Scepter: at Heirloom's question its holder " +
  "still names their own, and no other seat or the public does (R28)";
- "Stock Reliquary clears a seat's recorded knowledge of another " +
  "seat's relic: at Heirloom's question p1 no longer names it (R28)" to
  "the start of the Throne clears a seat's recorded knowledge of " +
  "another seat's relic: at Heirloom's question p1 no longer names it " +
  "(R28)".

In `src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala`,
rename "after Stock Reliquary no other seat or the public can name the "
+ "relics Scatter Relics sets aside (R28)" to "after the start of the "
+ "Throne no other seat or the public can name the relics Scatter "
+ "Relics sets aside (R28)".

Keep the test bodies unchanged.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.HiddenChronicleCardsSuite distantlands.application.gamelog.ChronicleLegacyLogSuite distantlands.gameplay.chronicle.ReliquaryAndRelicsSuite distantlands.gameplay.chronicle.legacies.* distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: PASS. If a golden differs, read the diff. A relic named in a
Throne line before Stock Reliquary, now read as "a Relic" by another
viewer, is the intended change: rewrite that golden with
`GAMELOG_GOLDEN=write` and name it in the commit body. Any other diff:
stop and report.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala \
  src/main/scala/distantlands/model/ChronicleOperation.scala \
  src/main/scala/distantlands/model/PowerWindow.scala \
  src/test/scala/distantlands/application/HiddenChronicleCardsSuite.scala \
  src/test/scala/distantlands/application/gamelog/ChronicleLegacyLogSuite.scala
git commit -F - <<'EOF'
feat(chronicle): hide the held relics at the start of the Throne

The hide step moves from Stock Reliquary to the base of the start-of-Throne
window, before Reformer, so a relic Hiding Place keeps is kept secretly.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

(Add any golden rewritten in Step 6 to the `git add` by name.)

---

### Task 2: The shared relic keep

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/RelicKeep.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`

**Interfaces:**
- Consumes: `ReliquaryAndRelics.warbandsOff(ready: ReadyGame, card: CardId): Vector[CoreOperation]` (Task 1).
- Produces: `RelicKeep.keepable(state: ReadyGame, holder: PlayerId): Vector[RelicState]`,
  every relic `holder` holds but the Grand Scepter.
- Produces: `RelicKeep.kept(holder: PlayerId, relic: RelicId, card: CardId): Vector[Operation]`,
  the keep and its two lines, naming `card`.
- Produces: `RelicKeep.noteKeys: Vector[NoteKey]`, keys `relic-kept` and
  `relic-kept-named` on the Throne's note id.

- [ ] **Step 1: Write the failing test**

In `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`,
add the import

```scala
import distantlands.gameplay.powers.campaign.ObsidianCageCard
```

and, after the test "the Grand Scepter, a relic not held and a relic with
tokens are not kept", add:

```scala
  test("a relic with warbands on it is not kept"):
    val ready = Table.start.relic(p1, ObsidianCageCard)
      .warbandsOnCard(ObsidianCageCard, p2, 1).ready
    val refusal = rejected(run(ready,
      StartingRelicKept(p1, ObsidianCageCard.id)))
    assert(refusal.toString.contains("still holds warbands"), refusal)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite"`
Expected: the new test FAILS with "expected a Left, got Right(...)".

- [ ] **Step 3: Refuse a relic with warbands on it**

In `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`,
in `startingRelicKept`, after the case

```scala
      case (Some(state), _) if !state.tokens.isEmpty => Left(
        InvalidDescription(s"${relic.value} still holds tokens"))
```

add

```scala
      case _ if ready.game.current.warbandsOnCards.get(relic)
          .exists(_.values.exists(_ > 0)) => Left(
        InvalidDescription(s"${relic.value} still holds warbands"))
```

In `src/main/scala/distantlands/model/ChronicleOperation.scala`, in the
`StartingRelicKept` doc comment, replace "Keeps a held relic for the next
game (Heirloom; Legacy Chronicle powers design, ruling 13; R20)" with
"Keeps a held relic for the next game (Heirloom, Legacy Chronicle powers
design, ruling 13, R20; Hiding Place, Last Chronicle powers design,
ruling 4)", and "and a relic with tokens are refused" with "and a relic
with tokens or warbands on it are refused". Re-wrap under 80 columns.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite"`
Expected: PASS.

- [ ] **Step 5: Extract the keep**

Create `src/main/scala/distantlands/gameplay/chronicle/RelicKeep.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** A relic a Throne power keeps for the next game: Heirloom's (Legacy
  * Chronicle powers design, ruling 13; R18-R20) and Hiding Place's (Last
  * Chronicle powers design, ruling 4). Any relic its holder has but the
  * Grand Scepter may be kept. Its secrets return to the holder facedown
  * and any warbands on it go back to their supply, as Stock Reliquary
  * returns them; then `StartingRelicKept` keeps it. Lines, naming the
  * card that keeps it: "p2 kept a relic with Heirloom" for every viewer,
  * and "p2 kept Cup of Plenty with Heirloom" for the holder alone. */
object RelicKeep:
  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val keptPublic: NoteKey = notes.key("relic-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a relic with "), NotePart.Arg(1)))
  val keptPrivate: NoteKey = notes.key("relic-kept-named", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The lines a keep writes, on the Throne's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Every relic `holder` holds but the Grand Scepter. */
  def keepable(state: ReadyGame, holder: PlayerId): Vector[RelicState] =
    state.game.current.player(holder).toVector.flatMap(_.relics)
      .filterNot(_.id == TheGrandScepterCard.id)

  /** The relic's secrets back to `holder`, facedown, and its warbands to
    * their supply, then the keep and its two lines, naming `card`. */
  def kept(holder: PlayerId, relic: RelicId, card: CardId)
      : Vector[Operation] =
    Vector(BuildOps((live, _) => Right(secretsBack(live, holder, relic) ++
        ReliquaryAndRelics.warbandsOff(live, relic) :+
        StartingRelicKept(holder, relic)), required = true),
      Note(noteId, _ => Some(keptPublic.sourceless(NoteArg.Player(holder),
        NoteArg.Card(card)))),
      Note(noteId, _ => Some(keptPrivate.sourceless(NoteArg.Player(holder),
        NoteArg.Card(relic), NoteArg.Card(card)).readBy(Vector(holder)))))

  private def secretsBack(state: ReadyGame, holder: PlayerId,
      relic: RelicId): Vector[CoreOperation] =
    val secrets = state.game.current.player(holder).toVector
      .flatMap(_.relics.find(_.id == relic)).map(_.tokens.secrets).sum
    if secrets == 0 then Vector.empty
    else Vector(Move(Piece.Secrets(secrets),
        PositionedLocation(Location.OnCard(relic)),
        PositionedLocation(Location.PlayArea(holder))),
      FlipSecrets(holder, secrets, SecretSide.FaceUp, SecretSide.FaceDown))
```

In `src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala`:

- Replace the imports with:

```scala
import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, RelicKeep}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._
```

- Delete `noteId`, `notes`, `keptPublic`, `keptPrivate`, `throneNoteKeys`,
  `keepable`, `kept` and `returned`.
- Replace `step` with:

```scala
  val step: Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, HeirloomCard.id).toVector.flatMap(
      holder => RelicKeep.keepable(state, holder) match
        case Vector() => Vector.empty
        case Vector(relic) => keep(holder, relic.id)
        case relics => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Heirloom: keep a relic for the next game"),
            confirmVerb = Some("Keep"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(holder))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(keep(holder, _))))))

  private def keep(holder: PlayerId, relic: RelicId): Vector[Operation] =
    RelicKeep.kept(holder, relic, HeirloomCard.id)
```

- In the doc comment, replace "Its secrets return to the holder facedown,
  then `StartingRelicKept` keeps it (R20). Lines (R19): "p2 kept a relic
  with Heirloom" for every viewer, and "p2 kept Cup of Plenty with
  Heirloom" for the holder alone." with "`RelicKeep` keeps it and writes
  its lines (R19, R20)."

In `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`,
change `throneNoteKeys` to:

```scala
  def throneNoteKeys: Vector[NoteKey] =
    Rival.throneNoteKeys ++ Revolutionary.throneNoteKeys ++
      Reformer.throneNoteKeys ++ TheEar.throneNoteKeys
```

In `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`,
change `noteKeys` to:

```scala
  def noteKeys: Vector[NoteKey] = OfferCitizenship.noteKeys ++
    ShapeEmpire.noteKeys ++
    BuildEdifice.noteKeys ++ LineageScoring.noteKeys ++ SwearOath.noteKeys ++
    LegacyChroniclePowers.throneNoteKeys ++ RelicKeep.noteKeys ++
    IronHand.noteKeys(TriggeredProcedureRef.ChronicleThrone)
```

- [ ] **Step 6: Rewrite the declared-keys snapshot**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Then `git diff src/test/resources/notes/declared-keys.txt`.
Expected: exactly the two lines `procedure.chronicle-throne heirloom-kept
...` and `procedure.chronicle-throne heirloom-kept-named ...` are gone,
and `procedure.chronicle-throne relic-kept = {0} kept a relic with {1}`
and `procedure.chronicle-throne relic-kept-named = {0} kept {1} with {2}`
appear. Any other change: stop and report.

- [ ] **Step 7: Run the affected suites**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.HeirloomSuite distantlands.application.HiddenChronicleCardsSuite distantlands.application.gamelog.* distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.chronicle.ReliquaryAndRelicsSuite"`
Expected: PASS, the goldens unchanged (the line text is the same).

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/RelicKeep.scala \
  src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala \
  src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala \
  src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala \
  src/main/scala/distantlands/model/ChronicleOperation.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala
git commit -F - <<'EOF'
refactor(chronicle): share Heirloom's relic keep as RelicKeep

The keep also returns warbands on the kept relic, and StartingRelicKept
refuses a relic that still carries warbands. Heirloom's two note keys
become relic-kept and relic-kept-named, with the same text.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 3: Hiding Place

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/HidingPlace.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/HallowedSpring.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/chronicle/HidingPlaceSuite.scala`

**Interfaces:**
- Consumes: `RelicKeep.keepable`, `RelicKeep.kept` (Task 2).
- Produces: `case object HidingPlace extends ContributingPower` with
  `id = HallowedSpringCard.ruined.power.id`, `priority = 1`,
  `decline: DecisionOptionRef.Button` and
  `decisionId(player: PlayerId): String`. Task 4 answers its decision.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/chronicle/HidingPlaceSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{GrandMaskCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.{Reformer, ReformerCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.{PlayerFacts, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.ObsidianCageCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard}
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Hiding Place (E26 ruined): "CHRONICLE (THRONE): This site's ruler may
  * secretly keep a relic they have facedown. They start the next game
  * with it." (Last Chronicle powers design, rulings 3, 4 and 6). p1, the
  * Chancellor, won and holds the Throne; Hiding Place stands at Broken
  * Peaks. */
class HidingPlaceSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def hidingPlaceAtBrokenPeaks: Table =
    ChronicleWalk.throneHeldByChancellorP1
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)

  private def hidingPlaceRuledByP2: Table =
    hidingPlaceAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p2, 1)

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def answering(ruler: PlayerId, ref: DecisionOptionRef)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == HidingPlace.decisionId(ruler) =>
      DecisionAnswer.ChooseOneAnswer(ref)

  private def keeping(ruler: PlayerId, relic: RelicId)
      : PartialFunction[Decide, DecisionAnswer] =
    answering(ruler, DecisionOptionRef.Relic(relic))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  private def askedHidingPlace(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId.startsWith("chronicle.throne.hiding-place"))

  private def options(decide: Decide): Set[DecisionOptionRef] =
    decide.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref).toSet

  test("Hiding Place is implemented"):
    assert(PowerImplementationStatus.implemented(
      HallowedSpringCard.ruined.power.id))
    assertEquals(HidingPlace.id, HallowedSpringCard.ruined.power.id)

  test("its ruler keeps one relic of their choice, never the Grand " +
      "Scepter, and may Decline (ruling 4)"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).relic(p2, TheGrandScepterCard)
    val run = through(table, keeping(p2, GrandMaskCard.id))
    val asked = askedHidingPlace(run)
    assertEquals(asked.map(_.owner), Vector(p2))
    assertEquals(options(asked.head), Set[DecisionOptionRef](HidingPlace.decline,
      DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(GrandMaskCard.id)))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(GrandMaskCard.id))
    // Scatter Relics took the rest; the Scepter stays with p2.
    assertEquals(run.ready.game.current.player(p2).get.relics.map(_.id),
      Vector(TheGrandScepterCard.id))

  test("a single relic still asks, and Decline keeps nothing (ruling 4)"):
    val run = through(hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard),
      answering(p2, HidingPlace.decline))
    assertEquals(askedHidingPlace(run).map(_.owner), Vector(p2))
    assertEquals(lineage(run.ready, p2).startingRelics, Vector.empty)

  test("a ruler with only the Grand Scepter is not asked"):
    val run = through(hidingPlaceRuledByP2.relic(p2, TheGrandScepterCard),
      PartialFunction.empty)
    assertEquals(askedHidingPlace(run), Vector.empty)

  test("the Chancellor keeps for an Imperial site, before Stock Reliquary " +
      "takes their relics (ruling 4)"):
    // p1 is the Chancellor, so their warband at Broken Peaks is Imperial.
    val table = hidingPlaceAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p1, 1)
      .relic(p1, CupOfPlentyCard)
    assertEquals(SiteRule.rulingPlayer(table.ready, BrokenPeaksSite.id),
      Some(SiteRuler.Player(p1)))
    val run = through(table, keeping(p1, CupOfPlentyCard.id))
    assertEquals(askedHidingPlace(run).map(_.owner), Vector(p1))
    assertEquals(lineage(run.ready, p1).startingRelics,
      Vector(CupOfPlentyCard.id))
    assert(!run.ready.game.campaign.reliquary.contains(CupOfPlentyCard.id))

  test("a site bandits rule, or nobody rules, asks nobody (ruling 4)"):
    val bandits = hidingPlaceAtBrokenPeaks.bandits(BrokenPeaksSite, 2)
      .relic(p2, CupOfPlentyCard)
    assertEquals(askedHidingPlace(through(bandits, PartialFunction.empty)),
      Vector.empty)
    val unruled = hidingPlaceAtBrokenPeaks.relic(p2, CupOfPlentyCard)
    assertEquals(SiteRule.rulingPlayer(unruled.ready, BrokenPeaksSite.id),
      Some(SiteRuler.Unruled))
    assertEquals(askedHidingPlace(through(unruled, PartialFunction.empty)),
      Vector.empty)

  test("the intact Hallowed Spring asks nobody"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .edifice(HallowedSpringCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1).relic(p2, CupOfPlentyCard)
    assertEquals(askedHidingPlace(through(table, PartialFunction.empty)),
      Vector.empty)

  test("a kept relic's secrets return to its ruler first, facedown"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 2)
    val before = table.ready.game.current.player(p2).get.board
    val run = through(table, keeping(p2, CupOfPlentyCard.id))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val after = run.ready.game.current.player(p2).get.board
    assertEquals(after.faceDownSecrets, before.faceDownSecrets + 2)

  test("warbands on a kept relic go back to their supply first"):
    // p1 already rules Ancient City, so Shape Empire places no Imperial
    // warband from the supply this test counts.
    val cage = ObsidianCageCard.id
    val table = hidingPlaceRuledByP2.warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, ObsidianCageCard).warbandsOnCard(ObsidianCageCard, p1, 1)
    val kinds = table.ready.game.current.warbandsOnCards(cage).keys.toVector
    val supply = (ready: ReadyGame) =>
      kinds.map(PlayerFacts.banked(ready, _)).sum
    val run = through(table, keeping(p2, cage))
    assertEquals(lineage(run.ready, p2).startingRelics, Vector(cage))
    assertEquals(run.ready.game.current.warbandsOnCards, Map.empty)
    assertEquals(supply(run.ready), supply(table.ready) + 1)

  test("Hiding Place asks after Reformer, once the relics are hidden " +
      "(rulings 3 and 6)"):
    // p1 won, holds Reformer and rules Ancient City and Broken Peaks, so
    // both powers ask p1.
    val table = hidingPlaceAtBrokenPeaks.warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard).relic(p1, CupOfPlentyCard)
    val run = through(table, keeping(p1, CupOfPlentyCard.id))
    val ids = run.asked.map(_.decisionId)
    val reformer = ids.indexOf(Reformer.pickId(p1))
    val hiding = ids.indexOf(HidingPlace.decisionId(p1))
    assert(reformer >= 0 && hiding > reformer, ids)

  test("a walk resumed at Hiding Place's question keeps the chosen relic"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      HidingPlace.decisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, HidingPlace.decisionId(p2))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    val answered = accepted(rules.resolveWalker(replayed, p2,
      HidingPlace.decisionId(p2), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(GrandMaskCard.id))))
    val ready = ChronicleWalk.endOf(rules, replayed,
      OathTransition(answered.state, answered.events),
      TriggeredProcedureRef.ChronicleThrone)
    assertEquals(lineage(ready, p2).startingRelics, Vector(GrandMaskCard.id))

  test("the kept relic starts the next game with its ruler, facedown"):
    val run = ChronicleWalk.through(rules, ChronicleWalk.chronicleWonByP1
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard),
      TriggeredProcedureRef.ChronicleFinish,
      keeping(p2, CupOfPlentyCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))
    val kept = lineage(run.ready, p2).id
    assertEquals(record.lineages.find(_.id == kept).map(_.startingRelics),
      Some(Vector(CupOfPlentyCard.id)))
    val next = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val seat = next.game.current.players.find(_.lineage == kept).get
    assert(seat.relics.contains(RelicState(CupOfPlentyCard.id,
      Orientation.FaceDown, Tokens.empty)), seat.relics)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.HidingPlaceSuite"`
Expected: compilation fails with "Not found: HidingPlace".

- [ ] **Step 3: Write the power**

Create `src/main/scala/distantlands/gameplay/chronicle/HidingPlace.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.model._

/** Hiding Place (E26 ruined): "CHRONICLE (THRONE): This site's ruler may
  * secretly keep a relic they have facedown. They start the next game
  * with it." Its step is at the start of the Throne, after the relics are
  * hidden and after Reformer, before Shape Empire can discard it (Last
  * Chronicle powers design, rulings 3, 4 and 6). The ruler is read when
  * the step is reached, by `SiteRule.rulingPlayer`: a player's warbands,
  * or the Chancellor for an Imperial site, never the Grand Mask's wearer.
  * Bandits, nobody, or an Empire with no Chancellor asks nothing. The
  * ruler chooses one of their relics but the Grand Scepter, or Decline,
  * even with a single relic. The relic is facedown already;
  * `RelicKeep` keeps it and writes its lines. */
case object HidingPlace extends ContributingPower:
  val id: PowerId = HallowedSpringCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** After Reformer (ruling 6). Transforms of one phase fold by priority,
    * then by source key, and this power's key sorts before Reformer's,
    * whose priority is the default 0. */
  override def priority: Int = 1
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (_, ops) => ops :+ step)))

  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.hiding-place.${player.value}"

  val step: Operation = Branch((state, _) =>
    ruler(state).toVector.flatMap(ruler =>
      RelicKeep.keepable(state, ruler) match
        case Vector() => Vector.empty
        case relics => Vector(
          Decide(decisionId(ruler), ruler, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +: relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Hiding Place: you may keep a relic for the " +
              "next game"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(ruler))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(RelicKeep.kept(ruler, _,
              HallowedSpringCard.id))))))

  /** The player who rules Hiding Place's site, while it is on the map. */
  private def ruler(state: ReadyGame): Option[PlayerId] =
    state.game.current.edificeSite(HallowedSpringCard.id, EdificeSide.Ruined)
      .flatMap(SiteRule.rulingPlayer(state, _)).collect {
        case SiteRuler.Player(player) => player }
```

If `SiteRuler.Player` is not the constructor's path, use the one
`SiteRule.governed` builds (`model/World.scala`).

- [ ] **Step 4: Register it**

In `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`,
change the import

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring, HallOfBandits,
  RuinedBaths, StoneBaths}
```

to

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring, HallOfBandits,
  HidingPlace, RuinedBaths, StoneBaths}
```

and insert `HidingPlace` after `HallOfBandits` in the contributions list,
re-wrapping under 80 columns:

```scala
        RuinedBaths, AncientCityEnduring, HallOfBandits, HidingPlace,
        RuinedTemple, HallOfMockery, LostGuardians, RuinedSchool,
```

(The list's order does not order the folds; `sortKey` does.) In the doc
comment, after the sentence ending "which Shape Empire reads.", add:

```scala
  * Hiding Place adds a step at the start of the Throne, after Reformer's.
```

- [ ] **Step 5: Drop the snapshot line and update the comments**

In `src/test/resources/powers/unimplemented.txt`, delete the line
`edifice.e26.ruined`.

In `src/main/scala/distantlands/gameplay/powers/economy/HallowedSpring.scala`,
replace

```scala
  * The ruined face, Hiding Place, is a Chronicle step and is not
  * implemented.
```

with

```scala
  * The ruined face, Hiding Place, is a Throne step: `HidingPlace` in
  * `gameplay/chronicle`.
```

In `src/main/scala/distantlands/model/PowerWindow.scala`, change the
`ChronicleStartOfThrone` doc comment to:

```scala
  /** The start of the Throne, before Offer Citizenship: the held relics
    * are hidden (Last Chronicle powers design, ruling 3), then Reformer's
    * step (Legacy Chronicle powers design, ruling 12), then Hiding
    * Place's (Last Chronicle powers design, ruling 4). */
```

In `ChronicleThroneProcedure.scala`'s doc comment, replace "then holds
Reformer's step;" with "then holds Reformer's and Hiding Place's steps;".

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.HidingPlaceSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.chronicle.legacies.ReformerSuite distantlands.gameplay.chronicle.legacies.HeirloomSuite"`
Expected: PASS. If "Hiding Place asks after Reformer" fails with Hiding
Place first, the priority is not ordering the folds: stop and report
rather than reordering the list.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/HidingPlace.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/gameplay/powers/economy/HallowedSpring.scala \
  src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/chronicle/HidingPlaceSuite.scala
git commit -F - <<'EOF'
feat(chronicle): Hiding Place keeps a relic for its site's ruler

At the start of the Throne, after Reformer, the player who rules Hiding
Place's site may keep one of their relics but the Grand Scepter for the
next game, or Decline.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 4: Hiding Place's lines

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
- Create: `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`
- Create: `src/test/resources/gamelog/throne-hiding-place.actor.log`,
  `src/test/resources/gamelog/throne-hiding-place.other.log`

**Interfaces:**
- Consumes: `HidingPlace.decisionId(player: PlayerId): String` (Task 3).
- Produces: `ChronicleLogScripts.throneHidingPlace: Script`, named
  `throne-hiding-place`. Slice 3 adds its Blasted Pastures test to
  `ChronicleEdificeLogSuite`.

- [ ] **Step 1: Write the script**

In `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`,
add after `throneHeirloom`:

```scala
  /** The game of [[chronicleStarted]] with Hiding Place: p2 rules Broken
    * Peaks, where Hiding Place stands, and holds Cup of Plenty and Grand
    * Mask; at the start of the Throne p2 keeps Cup of Plenty (Last
    * Chronicle powers, slice 2). */
  def throneHidingPlace(using munit.Location): Script =
    val (service, acting) = atTable("throne-hiding-place", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == HidingPlace.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-hiding-place", service, p1)
```

Add `HidingPlace` to the existing `distantlands.gameplay.chronicle.{...}`
import and `HallowedSpringCard` to the existing
`distantlands.gameplay.powers.economy.{...}` import, and add
`"throne-hiding-place" -> (() => throneHidingPlace),` to `named` after
`"throne-heirloom"`.

- [ ] **Step 2: Write the failing test**

Create `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`:

```scala
package distantlands.application.gamelog

import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard}
import distantlands.model.PlayerId
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts.{format, text}

/** The Chronicle lines of ruined edifices, as each viewer reads them (Last
  * Chronicle powers design, "Lines"). */
class ChronicleEdificeLogSuite extends munit.FunSuite:
  test("Hiding Place's relic is named to its ruler alone (ruling 4)"):
    val script = ChronicleLogScripts.throneHidingPlace
    val name = HallowedSpringCard.ruined.name
    val public = s"${p2.value} kept a relic with $name"
    val named = s"${p2.value} kept ${CupOfPlentyCard.name} with $name"
    def keptLines(viewer: Option[PlayerId]) =
      format(script, viewer).map(text).filter(line =>
        line == public || line == named)
    assertEquals(keptLines(Some(p2)), Vector(public, named))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assertEquals(keptLines(viewer), Vector(public), viewer))
```

- [ ] **Step 3: Run it and write the goldens**

Run: `./sbtw "testOnly distantlands.application.gamelog.ChronicleEdificeLogSuite"`
Expected: PASS (Task 3 already writes the lines). If p2 is never asked,
check that p2 still rules Broken Peaks when the Throne starts and report.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Then `git status --short src/test/resources/gamelog/`.
Expected: only the two new `throne-hiding-place` files. Read both: the
actor file (p1's view) has "p2 kept a relic with Hiding Place" and no
"Cup of Plenty" after the start of the Throne; the other file follows
the suite's convention for the second viewer. Any change to an existing
golden: stop and report.

- [ ] **Step 4: Run the log suites**

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS, the properties suite included.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala \
  src/test/resources/gamelog/throne-hiding-place.actor.log \
  src/test/resources/gamelog/throne-hiding-place.other.log
git commit -F - <<'EOF'
test(gamelog): Hiding Place's lines and golden

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 5: Docs and the full gate

**Files:**
- Modify: `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`,
  `docs/rules/rules-reference.md`, `docs/rules/implementation-traceability.md`,
  `docs/ROADMAP.md`,
  `docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md`

Every replacement below quotes the text slice 1 left. If a quoted text is
not found, read the surrounding lines, make the equivalent edit and
report it.

- [ ] **Step 1: The rulings**

In `docs/rules/ambiguities.md`, append to the "## Last Chronicle powers
rulings" section, after item 2:

```markdown
3. **Relics are hidden at the start of the Throne.** The step that turns every relic players hold but the Grand Scepter facedown, and clears every viewer's record of it, runs at the start of the Throne, before any power of that window, instead of at the start of Stock Reliquary. Each holder still sees their own relics. Warbands on cards still return to their supply at Stock Reliquary. So Hiding Place's keep is secret.
4. **Hiding Place.** At the start of the Throne, after the relics are hidden and after Reformer, the player who rules its site may keep any relic they hold but the Grand Scepter for the next game. The ruler is a player's warbands, or the Chancellor for an Imperial site. The Grand Mask does not count, since it is nobody's turn in the Chronicle. When bandits rule the site, or nobody does, nothing is asked. The ruler chooses one relic or Declines, even with a single relic. The relic's secrets return to them facedown and any warbands on it go back to their supply, then it joins their lineage's starting relics. Lines: "p2 kept a relic with Hiding Place" for every viewer, and "p2 kept Cup of Plenty with Hiding Place" for the ruler alone.
```

In item 40 of "Legacy Chronicle powers rulings", replace "Stock Reliquary
first turns every held relic but the Grand Scepter facedown" with "the
start of the Throne first turns every held relic but the Grand Scepter
facedown ([Last Chronicle powers rulings](#last-chronicle-powers-rulings), item 3)".

In item 1 of "Component-data dependencies", replace "Two printed powers
still have no executable behavior. They are listed in
`src/test/resources/powers/unimplemented.txt`: the ruined faces Blasted
Pastures (E07) and Hiding Place (E26)." with "One printed power still has
no executable behavior. It is listed in
`src/test/resources/powers/unimplemented.txt`: the ruined face Blasted
Pastures (E07)." In item 5, replace "the two powers item 1 lists" with
"the power item 1 lists".

- [ ] **Step 2: The Chronicle rules**

In `docs/rules/chronicle.md`, section "### 2. The Throne":

- Before the bullet that begins "- Digital: first, if the winner holds
  Reformer active", insert:

```markdown
- Digital: first, every relic players hold but the Grand Scepter turns facedown and no viewer's record of it remains; each holder still sees their own. Scatter Relics' lines then name a relic only to a viewer who knows it, "p2 set aside a Relic" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 40; [Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), item 3).
```

- In the Reformer bullet, replace "Digital: first, if the winner" with
  "Digital: then, if the winner".
- After the Reformer bullet, insert:

```markdown
- Digital: then, while Hiding Place (E26 ruined) is on the map, the player who rules its site, the Chancellor for an Imperial site, may keep one of their relics but the Grand Scepter for the next game, or Decline; bandits or nobody asks nothing. Its secrets return to them facedown and warbands on it to their supply, and the relic joins their lineage's starting relics. Every viewer reads "p2 kept a relic with Hiding Place"; the ruler alone also reads which relic ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), item 4).
```

- Under step 4, **Stock Reliquary**, delete the bullet that begins
  "   - Digital: first, every relic players hold but the Grand Scepter
  turns facedown", and in the Heirloom bullet below it replace
  "Digital: then a seated player holding Heirloom" with "Digital: after
  this step, a seated player holding Heirloom".

- [ ] **Step 3: The rules reference and traceability**

In `docs/rules/rules-reference.md`, after the **Ancient City and Hall of
Bandits.** bullet, add:

```markdown
- **Hiding Place.** CHRONICLE (THRONE): this site's ruler may secretly keep a relic they have facedown and start the next game with it. Digital: at the start of the Throne, after the held relics turn facedown and after Reformer; the Chancellor rules an Imperial site and the Grand Mask does not count; any relic but the Grand Scepter, or Decline ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 3 and 4).
```

In `docs/rules/implementation-traceability.md`, in the Throne task row,
replace "Heirloom keeps a relic for the next game through
`StartingRelicKept`, and The Ear places the scattered relics, its ACTION
half in the Legacy ACTION powers row (`legacies/HeirloomSuite`,
`legacies/TheEarSuite`, `application/HiddenChronicleCardsSuite`, the
`throne-heirloom` and `chronicle-ear-beacon` goldens)" with "The held
relics turn facedown at the start of the Throne; Heirloom and Hiding
Place keep a relic for the next game through `RelicKeep` and
`StartingRelicKept`, and The Ear places the scattered relics, its ACTION
half in the Legacy ACTION powers row ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings),
items 3 and 4; `legacies/HeirloomSuite`, `HidingPlaceSuite`,
`legacies/TheEarSuite`, `application/HiddenChronicleCardsSuite`, the
`throne-heirloom`, `throne-hiding-place` and `chronicle-ear-beacon`
goldens)".

- [ ] **Step 4: The roadmap and the spec status**

In `docs/ROADMAP.md`, replace the **The last Chronicle powers.** item with:

```markdown
- [ ] **The last Chronicle powers.** One printed power still does
  nothing: Blasted Pastures (E07 ruined) buries the Nomad cards on the map
  at the start of the Throne unless the winner rules it
  ([Last Chronicle powers design](superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md),
  slice 3). With it `src/test/resources/powers/unimplemented.txt` is
  empty, which unblocks **Retire the unimplemented-card machinery across
  the stack**.
```

In the **Retire the unimplemented-card machinery** item, replace "Two
powers remain" with "One power remains".

In the spec, change "Slice 1 landed 2026-11-16." to "Slice 1 landed
2026-11-16. Slice 2 landed 2026-11-16." Use the merge date instead if the
slice merges later.

- [ ] **Step 5: The full gate**

Run in the background: `./sbtw test`.
Expected: every test passes.
Run: `python3 scripts/check-architecture.py` and
`python3 scripts/check-markdown-links.py`.
Expected: both pass, with no missing anchor.

- [ ] **Step 6: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/chronicle.md \
  docs/rules/rules-reference.md docs/rules/implementation-traceability.md \
  docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md
git commit -F - <<'EOF'
docs(chronicle): record the hidden relics and Hiding Place and close slice 2

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

## Self-review

- Spec coverage:
  - ruling 3, the hide moved to the start of the Throne: Tasks 1 and 5;
  - ruling 4: the ruler by `rulingPlayer`, the Chancellor for an
    Imperial site, bandits and unruled asking nobody, Decline even with
    one relic, the Grand Scepter never offered, secrets and warbands
    returned, `StartingRelicKept`, the next game facedown: Task 3;
  - ruling 4, both lines and their readers: Task 4;
  - ruling 6, Reformer before Hiding Place: Task 3 (priority and its
    test);
  - the shared keep extracted from Heirloom, with Heirloom's suite green:
    Task 2;
  - the snapshot line, the declared-keys rewrite, a replay round trip for
    the new decision, and the docs the spec lists for this slice: Tasks
    2, 3 and 5.
- Ruling 6 itself, which names Blasted Pastures, is recorded in
  `ambiguities.md` by slice 3.
- Names, used the same way in every task:
  - `ReliquaryAndRelics.hide` and `ReliquaryAndRelics.warbandsOff`;
  - `RelicKeep.keepable`, `RelicKeep.kept` and `RelicKeep.noteKeys`;
  - `HidingPlace.decisionId`, `HidingPlace.decline` and
    `HidingPlace.priority`;
  - `ChronicleLogScripts.throneHidingPlace`.
