# Legacies, Slice 4: Scoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Score Lineage Points adds the legacy bonus. The winner scores 3
lineage points plus, for each of their legacies, 1 point per card of that
legacy's printed suit at the sites they rule; advisers do not count. The
gift bound becomes half that total, rounded down. The Game Log says
"Blue scored 7 lineage points (3 base, 3 from Beast, 1 from Nomad)";
legacies of one suit add into one share, a suit that scores 0 is left out,
and with no bonus the line stays "Blue scored 3 lineage points". The
legacy lifecycle is complete, and the ROADMAP's Legacies item gives way to
the three power batches.

**Architecture:** `LineageScoring` gains a pure `bonus(ready, catalog,
winner): Vector[(Suit, Int)]`, the winner's shares by suit, and scores
`basePoints + bonus` where it scored 3. A new note argument,
`NoteArg.SuitShares`, carries the shares into one line, as
`NoteArg.Placements` carries warbands by site; the walker event codec and
the Game Log learn it. `LineageScoring.steps` takes the catalog.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacies design](../../specs/2026-11/2026-11-13-legacies-design.md),
ruling 10 and section "Slice 4: scoring".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md).
CR, the Throne, Score Lineage Points: "The winner scores 3 lineage points
for winning plus points from their legacies: each legacy scores 1 lineage
point per matching card at sites they rule. Do not include advisers."
`docs/rules/chronicle.md`, the Throne, step 6; Chronicle ruling 13.

## Prerequisite

Slices 1 to 3 have landed. Check before Task 1:

```bash
grep -q "object ResolveLegacies" src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala &&
grep -q "val winnerPoints" src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R28-R33 ([rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md)),
each as the plan recommended: the kept amount with "less N given" (R28);
a line but no record for a winner who keeps 0 (R29); `NoteArg.SuitShares`
(R30); shares in suit order, empty ones left out (R31); ruined edifices by
their printed suit (R32); every legacy the winner holds (R33).

The owner later re-ruled R28 and amended R29: the earned total before the
gift question, one "gave up N points to give X M points." line per gift,
and "keeps N points" after any gift, with no "less N given" ending. The
"less N given" text below records what this plan first built.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches. A method body that outgrows coverage
  instrumentation is a compile error under `coverage`; split it.
- Every Scala file stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `gameplay` never reads a catalog `.text` or `rulesText`; it may read a
  card's `suit`.
- A new test names cards by their card objects.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit, then `git log -3` to check the commit is yours
  (another session works in this checkout).
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- A rewritten golden file is reviewed line by line and committed in the
  task that changes it; every changed line must be one the task explains.
  No golden log should change: no scripted game scores a legacy.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: The bonus

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
  (`LineageScoring.steps(catalog)`)
- Test: `src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala`

**Interfaces:**
- Produces: `LineageScoring.basePoints: Int = 3` (renamed from
  `winnerPoints`); `LineageScoring.bonus(ready: ReadyGame, catalog:
  ExecutableCatalog, winner: PlayerId): Vector[(Suit, Int)]`, one share per
  suit with a positive count, in `Suit.all` order; `steps(catalog)`.

- [ ] **Step 1: Write the failing tests**

In `LineageScoringSuite`, on `ChronicleWalk.throneHeldByChancellorP1`
(p1 the Chancellor, so "the sites they rule" are the Empire's), testing
`bonus` directly on the table's `ready`:

- Two legacies of different suits: p1 holds `TheNeedleCard` (Beast) and
  `CircleOfSwordsCard` (Nomad); the Empire rules a site with two Beast
  denizens and a Nomad edifice, and another with a Beast denizen: `Vector(
  Suit.Beast -> 3, Suit.Nomad -> 1)`.
- Two legacies of one suit (`TheNeedleCard`, `HeirloomCard`, both Beast)
  over three Beast cards: one share, `Vector(Suit.Beast -> 6)`.
- Advisers do not count: p1's Beast adviser adds nothing.
- A site the Empire does not rule adds nothing, and a suit with no card
  is left out.
- A ruined edifice counts by its suit (R32).
- No legacy: `Vector.empty`.

Pick denizens of the needed suits by card object, as
`LegacyGoalsSuite` does (`VillageIdiotCard` is a site-only Hearth card);
find Beast and Nomad ones with
`grep -rhoE "object [A-Za-z]+Card extends Denizen\(DenizenId\(\"[0-9]+\"\), \"[^\"]+\", Suit\.Beast\)" src/main/scala`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.LineageScoringSuite"`
Expected: FAIL to compile: `value bonus is not a member of object
distantlands.gameplay.chronicle.LineageScoring`.

- [ ] **Step 3: Implement**

```scala
  /** The winner's legacy bonus by suit (CR, Score Lineage Points; Legacies
    * ruling 10): for each legacy the winner holds, 1 point per card of its
    * printed suit, denizens and edifices, intact or ruined, at the sites
    * the winner rules; advisers do not count. Legacies of one suit add into
    * one share; a suit that scores nothing is left out; shares come in suit
    * order. After Crown Chancellor the winner is the Chancellor, so these
    * are the Empire's sites (Chronicle ruling 11). */
  def bonus(ready: ReadyGame, catalog: ExecutableCatalog, winner: PlayerId)
      : Vector[(Suit, Int)] =
    val current = ready.game.current
    val cards = current.map.inPlay
      .filter(ChronicleTasks.rules(ready, winner, _))
      .flatMap(site => current.map.sites.get(site).toVector.flatMap(_.denizens))
      .flatMap(card => catalog.suitOf(card.id))
    val legacies = ChronicleTasks.lineageOf(ready, winner)
      .flatMap(ready.game.campaign.lineages.get).toVector
      .flatMap(_.legacies).flatMap(legacy => catalog.legacy(legacy.id))
    Suit.all.map(suit => suit ->
      legacies.count(_.suit == suit) * cards.count(_ == suit))
      .filter(_._2 > 0)
```

Rename `winnerPoints` to `basePoints` and make `steps` a
`def steps(catalog: ExecutableCatalog)`; the Throne passes its catalog.
Update the object's doc comment: the bonus is in, and the gifts' bound is
half the total.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala
git commit -m "feat(chronicle): count the winner's legacy bonus by suit

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3 --oneline
```

---

### Task 2: `NoteArg.SuitShares`

**Files:**
- Modify: `src/main/scala/distantlands/model/LogNotes.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerEventCodec.scala`
  (the note argument encoder and decoder)
- Modify: `src/main/scala/distantlands/application/gamelog/NoteLines.scala`
  (the argument renderer)
- Modify: `src/test/scala/distantlands/gameplay/powers/NoteText.scala`
  (`plain`)
- Test: the walker event wire suite that round-trips note arguments
  (`git grep -ln "NoteArg.Placements" -- src/test`) and a `NoteLines` or
  Game Log suite that renders one (the same grep).

**Interfaces:**
- Produces: `final case class SuitShares(entries: Vector[(Int, Suit)])
  extends NoteArg`, rendered "3 from Beast, 1 from Nomad" (its entries
  joined with ", ", since Task 3's template continues the list), and on
  the wire `{"kind": "suit-shares", "entries": [{"points": 3, "suit":
  "beast"}, ...]}`.

- [ ] **Step 1: Write the failing tests**

- Wire: a `LogNoted` whose note carries `NoteArg.SuitShares(Vector(3 ->
  Suit.Beast, 1 -> Suit.Nomad))` round-trips, and its JSON is the shape
  above; an unknown suit decodes to a typed `WireError`.
- Rendering: a note with that argument renders "3 from Beast, 1 from
  Nomad" as text spans (Task 3's template supplies the commas around it,
  so the argument joins its own entries with ", ").

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `value SuitShares is not a member of object
distantlands.model.NoteArg`.

- [ ] **Step 3: Implement** beside `Placements` in each file; `plain` in
  `NoteText` writes "3 from Beast, 1 from Nomad".

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.serialization.* distantlands.application.gamelog.*"`.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(gamelog): carry lineage points by suit in a note

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3 --oneline
```

---

### Task 3: Score with the bonus, and its line

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`
  (`score`, `award`, two note keys)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
  (`chronicleThrone`: the new keys' `LogKind.Delta`)
- Test: `src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt` (rewritten)

**Interfaces:**
- Produces: the winner earns `basePoints + bonus.map(_._2).sum`; the gift
  bound is half of that, rounded down; new note keys `scoredWithBonus`
  ("{0} scored {1} ({2} base, {3})") and `scoredWithBonusLessGifts`
  ("{0} scored {1} ({2} base, {3}, less {4} given)"), `{1}` and `{4}`
  `Amount`s in lineage points, `{2}` a `Number`, `{3}` the `SuitShares`.
  With no bonus the existing `scored` line is written, unchanged.

- [ ] **Step 1: Write the failing tests**

- p1 holding `TheNeedleCard` with the Empire ruling 3 Beast cards earns 6:
  the gift question's bound is 3 (`maxTotal`), and keeping every point
  adds 6 to p1's era score.
- Gifts bounded by the new total: giving 3 keeps 0; p1's score is
  unchanged, p2's and p3's rise, and no `LineagePointsScored` names p1
  (R29).
- The lines: with the bonus and no gift, "p1 scored 6 lineage points (3
  base, 3 from Beast)"; with one gift, "p1 scored 4 lineage points (3 base,
  3 from Beast, less 2 given)" then the gift lines; with no legacy, "p1
  scored 3 lineage points", as today.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: the bound is still 1 and the line has no breakdown.

- [ ] **Step 3: Implement**

`score` reads `bonus` once (with the catalog) and passes the total and the
shares to the gift question and to `award`. `award` keeps `total - 2 *
given`; it writes `LineagePointsScored` only when that is positive, and
the winner's line always when there is a bonus, `scored` otherwise, as
today.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`, then
`NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
(exactly the two new `procedure.chronicle-throne` keys), then
`./sbtw test`. `git status --short src/test/resources` shows only the
snapshot.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala src/test
git commit -m "feat(chronicle): score the winner's legacy bonus and name it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3 --oneline
```

---

### Task 4: Docs and the gate

**Files:**
- Modify: `docs/ROADMAP.md` (the Chronicle steps phase)
- Modify: `docs/rules/chronicle.md` (the Throne, step 6: a "Digital" note)
- Modify: `docs/rules/implementation-traceability.md` (the Throne row, or
  the row naming Score Lineage Points)
- Modify: `docs/rules/ambiguities.md` (the Legacies rulings: this plan's
  rulings)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md`
  (status line)

- [ ] **Step 1: Docs**

- ROADMAP: the design's "Docs" section. Replace the **Legacies** item with
  three items, **Legacy powers: setup**, **Legacy powers: Chronicle** and
  **Legacy powers: in play**, which take High Priest's `DealRules` term and
  the powers the design's "Out of scope" names (Iron Hand and Chronicler
  scoring others, Steadfast, Gatekeeper, Royal Line, The Mouth,
  Matchmaker), each where it hooks in. The lifecycle half is deleted. Keep
  the frontend's legacy strip with slice 5 (R27), not here.
- `chronicle.md`, step 6: "Digital: the bonus counts printed suits, edifices
  included, at the Empire's sites; the line reads 'p1 scored 7 lineage
  points (3 base, 3 from Beast, 1 from Nomad)', ending 'less N given' after
  gifts" and links to the Legacies rulings.
- Traceability: Score Lineage Points' legacy bonus moves from the gaps to
  the evidence (`LineageScoringSuite`).
- `ambiguities.md`: append R28-R33 to the Legacies rulings, numbered on
  from 21, each with its R number.
- Design status: "Slices 1 to 4 landed (plans: ...)".

- [ ] **Step 2: Run the verification gate**

`./sbtw clean coverage test coverageReport` (statements at or above 92%,
branches at or above 84.4%), `./sbtw "frontend/test"`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`; `git status --short
src/test/resources` is clean.

- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "docs(legacies): record slice 4

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3 --oneline
```
