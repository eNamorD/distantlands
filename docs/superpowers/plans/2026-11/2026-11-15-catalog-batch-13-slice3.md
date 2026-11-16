# Catalog Batch 13, Slice 3: The Keep in Foundation III Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In a later game, the Quiet Ambitions keep runs while Foundation
III resolves: every seat keeps, in turn order, before any pawn is placed. A
first game keeps as it does today, right after each player's pawn.

**Architecture:** `SetupProcedure.tree` builds the Setup walk. Today each
player's step holds their pawn, their `SetupPawnPlaced` window, Secret
Society's excess discard, `LegacyKeep.step`, Quest to Distant Lands' swap,
and Choose Adviser and Reveal Cards. In a later game this slice takes
`LegacyKeep.step` out of each player's step. It places one keep per seat,
in turn order, between the `SetupBanks` window (Foundation II) and the
`SetupWorldDeck` window (Foundation IV). A game is a first game when
`EmpireSetupRules.isFirstGame(catalog, ready.game.campaign.baseline)`
holds, the same test `SetupPawnSites` uses. So an exile-only game, which
carries history and resolves its stored foundations, keeps in Foundation
III (owner, 2026-11-15). The predicate reads only the baseline, which
never changes during Setup, so a resumed walk rebuilds the same tree.
Since slice 2 the keep draws its own legacies, so the draw moves with it.
Secret Society and Seer hook `SetupWorldDeck`, so in a later game they now
follow the keep.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
ruling 3 and section "Slice 3: the keep in Foundation III".

## Global Constraints

- Slices 3 and 4 change the Setup walk, so this slice runs the Setup,
  replay and golden suites in full before landing (spec, "Goal and
  scope"). In practice that means the full JVM suite.
- A first game's walk is unchanged: no first-game golden or test changes.
- The keep's lines, decision ids and questions are unchanged. Only their
  position in a later game's walk changes.
- `build` doubles as `rebuild`: the tree must be a pure function of state
  that Setup never changes.
- No production Scala file exceeds 800 lines (`check-architecture.py`).
- Docs, comments and commit messages are written in plain English.

## Files

- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`.
  Covers the tree, `playerStep` and the scaladoc tree.
- Modify: `src/main/scala/distantlands/gameplay/setup/LegacyKeep.scala`
  (scaladoc only).
- Test: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala`
  (new position tests).
- Test: `src/test/scala/distantlands/gameplay/setup/legacies/MatchmakerSuite.scala`
  (new test).
- Test: `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`
  (new replay test).
- Test, expectations updated:
  - `gameplay/setup/ImperialMapsSetupSuite.scala` (3 tests)
  - `gameplay/setup/legacies/RivalSetupSuite.scala` (1 test)
  - `gameplay/setup/AlteredFoundationSetupSuite.scala` (1 test)
- Test, stale anchors moved: these suites park at `LegacyKeep.decisionId`
  after the holder's pawn window, a park that no longer comes after it:
  - `gameplay/setup/legacies/TheStandardBearerSuite.scala`
  - `gameplay/setup/legacies/WealthySuite.scala`
  - `gameplay/setup/legacies/SecretSocietySuite.scala`
- Goldens, regenerated: the 8 `later-game-*` goldens in
  `src/test/resources/gamelog/`.
- Docs:
  - `docs/rules/ambiguities.md`
  - `docs/superpowers/specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md`
  - `docs/rules/rules-reference.md`
  - `docs/rules/implementation-traceability.md`
  - the spec's status line

A spike of this change, run on 2026-11-15 against the full JVM suite,
failed exactly these 13 tests: 8 goldens, 3 in `ImperialMapsSetupSuite`, 1
in `RivalSetupSuite` and 1 in `AlteredFoundationSetupSuite`. Every other
test passed, including the stream suite's event counts: the walk records
the same events, in a new order.

---

### Task 1: The keep moves to Foundation III in later games

**Files:** the `SetupProcedure`, `LegacyKeep` and test files listed above.

**Interfaces:**
- Consumes: `LegacyKeep.step(participant: FirstGameParticipant): Operation`
  and `EmpireSetupRules.isFirstGame(catalog, chronicle): Boolean`.
- Produces: no new names. `SetupProcedure.playerStep` gains a
  `firstGame: Boolean` parameter (private).

- [ ] **Step 1: Write the failing position tests in `LegacySetupSuite`**

Add these imports: `distantlands.gameplay.setup.legacies.{PopulistCard,
Populist, Seer}` next to `SeerCard`, plus `distantlands.testkit.Answers`,
which is already imported. Add a helper and three tests after the
"each player's keep is logged" test:

```scala
  /** `seats` from the first player round the table. */
  private def inTurnOrder(first: PlayerId): Vector[PlayerId] =
    val ids = seats.map(_.playerId)
    val at = ids.indexOf(first)
    ids.drop(at) ++ ids.take(at)

  private def isKeepOrPawn(id: String, player: PlayerId): Boolean =
    id == SetupProcedure.legacyDecisionId(player) ||
      id == SetupProcedure.pawnDecisionId(player)

  test("in a first game each player keeps right after placing their pawn " +
      "(CR p. 7)"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    val order = inTurnOrder(orders.firstPlayer)
    val asked = walked(ready).asked.map(_.decisionId)
      .filter(id => order.exists(isKeepOrPawn(id, _)))
    assertEquals(asked, order.flatMap(player => Vector(
      SetupProcedure.pawnDecisionId(player),
      SetupProcedure.legacyDecisionId(player))))

  test("in a later game every seat keeps while Foundation III resolves, " +
      "in turn order, before any pawn (catalog batch 13, ruling 3)"):
    val first = seats(1).playerId
    val ready = accepted(GameStartRules.evolve(catalog, later,
      LaterGameFixture.orders(later, first)))
    val order = inTurnOrder(first)
    val asked = walked(ready).asked.map(_.decisionId)
      .filter(id => order.exists(isKeepOrPawn(id, _)))
    assertEquals(asked, order.map(SetupProcedure.legacyDecisionId) ++
      order.map(SetupProcedure.pawnDecisionId))

  test("in a later game the keeps follow the banks and come before the " +
      "world deck: Populist asks first and Seer after"):
    val holder = LineageId("l3")
    val record = SetupLegacyFixture.holding(later, holder, Role.Exile,
      PopulistCard.id -> true, SeerCard.id -> true)
    val holderSeat = seats.find(_.lineageId == holder).get.playerId
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    Situation.wake(Situation.productionRules(catalog).withAnswers(recording),
      record, LaterGameFixture.orders(record, seats.head.playerId))
    val keeps = seats.map(seat => asked.indexOf(
      SetupProcedure.legacyDecisionId(seat.playerId)))
    assert(keeps.forall(_ >= 0), asked)
    assert(asked.indexOf(Populist.decisionId(holderSeat)) < keeps.min, asked)
    assert(asked.indexOf(Seer.decisionId(holderSeat)) > keeps.max, asked)
```

If the fixture seats no `l3`, use the lineage the third seat plays, read
from `seats`. If Populist asks nothing for this record (it is a "may", but
the question should still be asked), check `PopulistSuite`'s record and
copy its setup. If `Situation.productionRules` is not the name in use,
use the one `WealthySuite` uses.

- [ ] **Step 2: Write the failing Matchmaker test**

In `MatchmakerSuite`, after "the holder keeps their own draw":

```scala
  test("the holder answers every keep in Foundation III, before any pawn " +
      "(catalog batch 13, ruling 3)"):
    val at = parkedAt(LegacyKeep.decisionId(p2))
    assertEquals(owner(at), p3)
    Vector(p1, p2, p3).foreach(player =>
      assertEquals(at.ready.game.current.pawnSite(player), None))
    assert(at.ready.game.campaign.lineages(l1).legacies
      .exists(!_.active), "p1's keep came first")
```

- [ ] **Step 3: Write the failing replay test**

In `LaterGameSetupChoicesSuite`, after "a Setup filled from Forgotten
Sites replays":

```scala
  test("a later game saved mid-Foundation III, between two keeps, replays " +
      "its journal to the same game (catalog batch 13, ruling 3)"):
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val keeping = Situation.start(Situation.journaled(service, catalog,
        repository, "keeping"))
      .parkedAt(SetupProcedure.legacyDecisionId(p2))(GameCommand.Begin(
        record, LaterGameFixture.orders(record, p1)))
    assert(keeping.ready.game.campaign.lineages(l1).legacies.nonEmpty,
      "p1 kept before p2's keep")
    assertEquals(keeping.ready.game.current.pawnSite(p1), None)
    val loaded = present(accepted(service.load("keeping")), "the game")
    assertEquals(loaded.state, keeping.state)
```

Under Quiet Ambitions the keep is always asked here: the deck holds every
legacy no lineage holds, at least 2 per seat.

- [ ] **Step 4: Run the new tests and check they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacySetupSuite distantlands.gameplay.setup.legacies.MatchmakerSuite distantlands.application.LaterGameSetupChoicesSuite"`

Expected: the first-game test passes, since it pins today's order. The
later-game order, the Populist/Seer test, the Matchmaker test and the
replay test FAIL: today a later game asks p1's pawn before p2's keep. The
replay test may pass with a failing "p1 kept" assertion only; any failure
of the new assertions is the expected red.

- [ ] **Step 5: Move the keep**

In `SetupProcedure.tree`, replace

```scala
    val participants = turnOrder(ready)
    val steps = participants.map(playerStep(catalog, ready, _))
```

with

```scala
    val participants = turnOrder(ready)
    // A first game keeps in each player's turn, after their pawn (CR p. 7);
    // a later game keeps while Foundation III resolves, every seat before
    // any pawn (CR "Later games", step 5; catalog batch 13, ruling 3). An
    // exile-only game resolves its stored foundations, so it keeps in
    // Foundation III, and its first-game test is the one pawn placement
    // reads (R41).
    val firstGame = EmpireSetupRules.isFirstGame(catalog,
      ready.game.campaign.baseline)
    val keeps =
      if firstGame then Vector.empty else participants.map(LegacyKeep.step)
    val steps = participants.map(playerStep(catalog, ready, _, firstGame))
```

and replace

```scala
      imperialMaps ++ Vector(banks, worldDeck) ++ steps :+ setupEnd :+
```

with

```scala
      imperialMaps ++ (banks +: keeps :+ worldDeck) ++ steps :+ setupEnd :+
```

Then update the comment above `worldDeck`: "the world deck game start
built is changed here, after the keeps in a later game". Change
`playerStep`'s signature to

```scala
  private def playerStep(catalog: ExecutableCatalog, ready: ReadyGame,
      participant: FirstGameParticipant, firstGame: Boolean): Operation =
```

and build its children so the keep appears only in a first game:

```scala
    Sequence(Vector(
      <the pawn Branch, unchanged>,
      <the SetupPawnPlaced Sequence, unchanged>,
      <SecretSociety.excessStep, unchanged>) ++
      Option.when(firstGame)(LegacyKeep.step(participant)) ++ Vector(
      QuestToDistantLands.swapStep(participant.playerId),
      <the adviser Branch, unchanged>))
```

Keep the existing comments on each child.

- [ ] **Step 6: Update the scaladoc trees**

In `SetupProcedure`'s scaladoc, insert the keep between the two windows,
and mark the per-player keep as a first game's:

```
 *   Sequence(Vector.empty)                               // window = SetupBanks
 *   Sequence(                                            // a later game's
 *     Branch(Draw(2 into the keeper's hand)),            // LegacyKeep.step,
 *     Branch(Decide(legacy-choice, keeper), ...))        // per seat in turn
 *   ...                                                  // order (Foundation III)
 *   Sequence(Vector.empty)                               // window = SetupWorldDeck
```

and in the per-participant part, annotate the keep's `Sequence` with
`// a first game's LegacyKeep.step`. In `LegacyKeep`'s object scaladoc,
replace "Split from `SetupProcedure`, which calls `step` in each player's
turn." with "Split from `SetupProcedure`, which calls `step` for every seat
while Foundation III resolves in a later game, and in each player's turn,
after their pawn, in a first game (catalog batch 13, ruling 3)."

- [ ] **Step 7: Run the new tests and check they pass**

Run the Step 4 command. Expected: PASS.

- [ ] **Step 8: Update the tests whose expectations the move changes**

`ImperialMapsSetupSuite`, "with one site in the Atlas" (line ~270) and "an
Empire of eight sites" (line ~300): the first park after Imperial Maps is
now p1's keep:

```scala
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.legacyDecisionId(p1))
```

"Wide Horizons with an Empire, Setup With No Empire and a first game":
the two later games first park on the keep, and the first game on the
pawn. Give each row its expected first decision:

```scala
    Vector(
      (wideHorizons, orders(wideHorizons), empireSites,
        SetupProcedure.legacyDecisionId(p1)),
      (noEmpire, orders(noEmpire), empireSites,
        SetupProcedure.legacyDecisionId(p1)),
      (firstGame, FirstGameSetupFixture.orders,
        firstGame.atlasBox.map(_.site),
        SetupProcedure.pawnDecisionId(FirstGameSetupFixture.firstPlayer)))
      .foreach {
      (chronicle, setupOrders, sites, first) =>
        val begun = Situation.start(Situation.rules(catalog))
          .parkedAfter(GameCommand.Begin(chronicle, setupOrders))
        assertEquals(parkedDecide(begun).decisionId, first, chronicle.name)
        assertEquals(begun.ready.game.current.map.hinterward, sites,
          chronicle.name)
    }
```

Rename the test to "... ask no Imperial Maps question and fill no slot:
the first question is the keep in a later game and the pawn in a first".

`RivalSetupSuite`, "Imperial Maps asks nothing when Rival leaves one Atlas
site (R6)" (line ~70): expect `SetupProcedure.legacyDecisionId(p1)`.

`AlteredFoundationSetupSuite`, "under Teeming World Setup asks each player
for a pawn site, a kept legacy and a reveal" (line ~326): Teeming World is
history, so this is a later game:

```scala
    assertEquals(asked, Vector(p1, p2, p3).map(
      SetupProcedure.legacyDecisionId) ++ Vector(p1, p2, p3).flatMap(player =>
      Vector(SetupProcedure.pawnDecisionId(player),
        SetupProcedure.revealDecisionId(player))))
```

and rename it "... asks every player to keep a legacy in Foundation III,
then each for a pawn site and a reveal, and no one to choose an adviser".

- [ ] **Step 9: Move the stale anchors**

These tests walk on with `parkedAt(LegacyKeep.decisionId(p))()` after p's
pawn window. In a later game that keep has already run, so the walk now
settles to the end of Setup and the test passes for the wrong reason. Park
at the next step after the window instead, and assert the walk stopped
there:

- `TheStandardBearerSuite`, 3 uses (lines ~71, ~82, ~94):
  `parkedAt(SetupProcedure.adviserDecisionId(p2))()`.
- `WealthySuite`, 1 use (line ~81):
  `parkedAt(SetupProcedure.adviserDecisionId(p3))()`.
- `SecretSocietySuite`, 1 use (line ~130), under Teeming World, which asks
  no adviser: `parkedAt(SetupProcedure.revealDecisionId(p2))()`.

After each, add `assertEquals(decide(after).decisionId, <the same id>)`,
reading the parked situation through the suite's own `decide` helper. If
the suite reads `.ready` in the same expression, split it to keep the
situation first. If an anchor is not reached because the fixture gives
that player a starting adviser, use their reveal id instead, and check
that one is reached.

- [ ] **Step 10: Regenerate the goldens and check only the order changed**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Then check that each changed golden holds the same lines as before,
ignoring sequence numbers, and that the keep lines moved ahead of the
first "placed their pawn" line. A scratch script, run from the worktree:

```python
import re, subprocess
files = subprocess.run(['git', 'diff', '--name-only',
    'src/test/resources/gamelog'], capture_output=True, text=True,
    check=True).stdout.split()
def lines(text):
    return sorted(re.sub(r'@\d+', '@N', re.sub(r'^\d+\.\d+ ', '', l))
                  for l in text.splitlines())
for f in files:
    old = subprocess.run(['git', 'show', f'HEAD:{f}'], capture_output=True,
                         text=True, check=True).stdout
    print('SAME LINES' if lines(old) == lines(open(f).read())
          else 'DIFFERS', f)
```

Expected: the 8 `later-game-*` goldens (actor and other, so 16 files)
change, and all print SAME LINES. Read one diff, `later-game-setup`, to
confirm that every keep line now precedes every pawn line.

- [ ] **Step 11: Run the full JVM suite and the checks**

Run: `./sbtw test`, then `python3 scripts/check-architecture.py`.
Expected: all tests pass, and the check reports no violation.

- [ ] **Step 12: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup src/test/scala src/test/resources/gamelog
git commit -m "feat(setup): keep a legacy while Foundation III resolves in later games"
```

---

### Task 2: Record the ruling and close the slice

**Files:** the docs listed above.

- [ ] **Step 1: Add the ruling to `docs/rules/ambiguities.md`**

In "Catalog batch 13 rulings", extend the intro's item list: "item 8 is
ruling 3, with the exile-only game ruled by the owner on 2026-11-15". Then
add:

```markdown
8. **The keep moves to Foundation III in later games.** The Quiet Ambitions keep runs while Foundation III resolves, after the banks and before the world deck, for every seated player in turn order from the first player, before any pawn is placed. Matchmaker's holder answers every other player's keep, as before. A first game's keep stays right after each player's pawn (CR p. 7). An exile-only game resolves its stored foundations, so it keeps in Foundation III; the test is the one pawn placement uses ([R41](#legacy-setup-powers-rulings)). Secret Society and Seer then follow the keep, and a Quest to Distant Lands holder who places no pawn keeps with everyone else.
```

Amend Legacy Setup powers item 36 (line ~305): "comes after the holder's
pawn and before their keep" becomes "comes right after the holder's pawn,
and before their keep in a first game".

- [ ] **Step 2: Amend R21 in the Legacy Setup powers implementation rulings**

"After the holder's own `SetupPawnPlaced` window and before their keep"
becomes "After the holder's own `SetupPawnPlaced` window, and before their
keep in a first game (in a later game the keep ran in Foundation III;
catalog batch 13, ruling 3)".

- [ ] **Step 3: Add the Digital note to `docs/rules/rules-reference.md`**

On Later games step 5, append to the existing note about Matchmaker:

```markdown
Digital: the Quiet Ambitions keep runs here, every seat in turn order before any pawn; a first game keeps after each pawn ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), item 8).
```

- [ ] **Step 4: Update `docs/rules/implementation-traceability.md`**

In the "Legacies on the table" row, after the sentence on legacies in a
hand, add:

```markdown
In a later game the keep runs while Foundation III resolves, before any pawn, and in a first game after each pawn (`setup/LegacySetupSuite`, `setup/legacies/MatchmakerSuite`, `application/LaterGameSetupChoicesSuite`; [Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), item 8).
```

- [ ] **Step 5: Update the spec's status line**

After "Slice 2 landed 2026-11-15." add "Slice 3 landed 2026-11-15."

- [ ] **Step 6: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`. Expected: no broken link.

```bash
git add docs
git commit -m "docs(catalog): record the keep in Foundation III and close batch 13 slice 3"
```
