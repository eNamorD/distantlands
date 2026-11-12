# Foundations, Slice 7: Shape Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Beacon's third step, Shape Foundation, stops being an empty
window. While some altered foundation face may be restored, the Beacon task
holder chooses to keep every foundation as it lies or to restore one altered
face to its normal face. A face an active legacy alters is shown unavailable,
naming the legacy. The restore is journaled, the Game Log says "p3 restored
Foundation I to Imperial Maps", and the evolved Chronicle carries the
restored face into the next game. The plan's last task closes the
Foundations phase in the docs.

**Architecture:** One new primitive operation, `FoundationRestored(number)`,
turns a face normal and clears its alteration sources; it is applied in
`ChronicleStateOperations` and has the wire kind `foundation-restored`. One
new step object, `gameplay/chronicle/ShapeFoundation.scala`, in the shape of
`WelcomeStranger` and `SwearOath`: a `Branch` that reads the holder and the
altered faces, a choose-one `Decide`, a required `BuildOps` that turns the
answer into the restore, and a `Note`. `ChronicleBeaconProcedure` puts its
steps in the `ChronicleShapeFoundation` window and declares its note keys,
and the Game Log's Beacon entry maps them. `ChronicleRecord` already writes
`campaign.foundations` into `ChronicleUpdated`, so nothing changes there.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings" (ruling 1: the Beacon may restore an altered III),
"Slice 1: faces reach the game" ("Chronicle record"), "Slice 3: ... Setup
With No Empire" (erosion leaves a face with no source), "Slice 7: Shape
Foundation" and its "Docs" list, and every slice's "As built" and readings,
which Task 4 moves to `docs/rules/ambiguities.md`. Slice 6's plan,
[Foundations slice 6](2026-11-11-foundations-slice6.md), is the format this
plan follows. The rule text is the New Foundations rulebook,
`~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`, "Task 4:
The Beacon", "Third, Shape Foundation" (about line 1394): "You may flip one
foundation setup card from its altered side to its normal side unless it's
being altered by an active legacy." Read it there; make no web call.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`). The closest
  this slice comes is `src/main/scala/distantlands/model/CoreOperations.scala`,
  which grows from 791 to 796 lines, and
  `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`, from
  761 to 769; `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
  grows from 746 to 759. Add nothing to those three beyond the blocks below.
- `gameplay` must not read a catalog `.text` or `rulesText`, and `model`
  imports nothing from `gameplay` (`scripts/check-architecture.py`).
  `ShapeFoundation` reads only a foundation face's printed `name` from
  `FoundationCards` and a legacy's `name` from the catalog.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. A test that names a symbol the task has not built
  yet fails by not compiling, and that is the failure to see. Tests go in
  the repo's existing suites where one covers the subject. A test helper's
  name says what it builds.
- Wire and journal changes need not keep old journals decodable or
  replayable, and every new wire field is required. This slice adds one
  operation, `FoundationRestored` (wire kind `foundation-restored`, its
  `number` an integer from 1 to 6, always present), and one decision,
  `chronicle.beacon.shape-foundation`, with button options `keep` and
  `restore-i` to `restore-vi`. It adds no projection field.
- New Foundations rules only. The rulebook is quoted under **Spec** above;
  `docs/rules/chronicle.md` (task 4, step 3) says the same. A legacy's
  active face is `LegacyState.active` (`model/Cards.scala`), and a face's
  alteration sources are `FoundationState.alterationSources`
  (`model/World.scala`).
- If a rule is unclear while you work, do not guess silently and do not
  stop: take the most literal reading of the New Foundations text, add it
  to the spec's slice 7 readings and to item 14 of the new "Foundations
  rulings" in `docs/rules/ambiguities.md` (Task 4), and name it in your
  report.
- An unavailable option is shown disabled with its reason, never hidden: a
  face an active legacy alters is listed, unavailable, with "An active
  legacy alters it: The Needle". A face on its normal face is no option at
  all (there is nothing to restore), so it is not listed.
- A Game Log line keeps the wording of the lines beside it. Every Chronicle
  line names its player, the actor included ("p1 chose Discord, Arcane and
  Order", "p1 welcomed Naysayers as a starting adviser"), so the restore
  line names the Beacon task holder: "p3 restored Foundation I to Imperial
  Maps". Keeping every foundation writes no line, as offering Citizenship
  to nobody writes none.
- Note keys change in Task 2, and one golden pair is added in Task 3,
  exactly as those tasks show. The rewrite commands are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
  After a rewrite, read `git diff -- src/test/resources` and
  `git status --short src/test/resources` line by line against the text the
  task shows. If anything else changed, stop and report: do not commit a
  file you cannot explain. Never run the golden rewrite except in Task 3,
  Step 3: if `GameLogGoldenSuite` fails anywhere else, stop and report.
- `python3 scripts/check-markdown-links.py` reports 34 broken links that an
  unrelated docs move left in older specs (listed in Task 4, Step 3). Do not
  fix them, and add none: the run after Task 4 must print exactly those 34.
- The words are "face" for one of a foundation card's two sides, "altered"
  and "normal" for the two faces, "restore" for turning an altered face
  normal, "alteration source" for a legacy in
  `FoundationState.alterationSources`, "active legacy" for a `LegacyState`
  whose `active` is true, "holder" for the Beacon task holder, and "Keep"
  for the option that restores nothing.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `552aa27f6` (slice 6's branch head) with the earlier tasks of this plan
  applied, and occurs once in its file. "Then replace" is the next block in
  the same file. A "with" block repeats the unchanged lines of its
  "Replace" block, so an edit that only adds lines still shows where they
  go. "Create" gives a file's full text. If a block does not match because
  slice 6's review fixes moved the text, find the same code by its content
  and make the same change; if the code itself is different, stop and
  report.
- Shell is zsh: never write a bare `===` or `=word` token in a command, and
  quote globs (`--include='*.scala'`).
- Never `git add -A` or `git add .`; add files by path. Never push. No bare
  `git stash`.
- Commit messages and docs are plain prose. Every commit message ends with
  the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  Use exactly that trailer whatever model you run on: the plan's trailer
  overrides your own. Each commit command below passes the subject, the
  body and the trailer as three `-m` arguments; keep that form, which needs
  no heredoc.

---

## Setup

Work in the worktree `/Users/roman/projects/oathdigital/.worktrees/foundations-s7`,
on the branch `foundations-slice7`, cut from `foundations-slice6` at
`552aa27f6`. Slice 6's review fixes may have been merged into it since.
Never run a git command that changes state in the main checkout,
`/Users/roman/projects/oathdigital`, which holds another session's work,
and do not touch `/Users/roman/projects/oathdigital/.worktrees/foundations-s6`,
which a reviewer reads.

In the worktree, link the main checkout's tooling before the first
`./sbtw`, unless the links are already there:

```bash
cd /Users/roman/projects/oathdigital/.worktrees/foundations-s7
test -e .tooling || ln -s /Users/roman/projects/oathdigital/.tooling .tooling
test -e node_modules || ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

Both links are ignored by git. If `git status` lists `node_modules` as
untracked, leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor f3136fb3e HEAD && echo slice3b-ok
git merge-base --is-ancestor 552aa27f6 HEAD && echo slice6-ok
git grep -n "ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))" -- src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala
git grep -n "foundations = campaign.foundations, lineages = kept ++ added)" -- src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala
git grep -n "final case class EdificeBuilt(edifice: EdificeId, site: SiteId)" -- src/main/scala/distantlands/model/CoreOperations.scala
git grep -n "LineagePointsScored | EdificeBuilt | SuitedReserveFromBox | PlaceAtlasSite" -- src/main/scala/distantlands/serialization/WalkerOperationCodec.scala
git grep -n "WelcomeStranger.keptAdviser -> LogKind.Decision))," -- src/main/scala/distantlands/application/gamelog/ProcedureLog.scala
git grep -n "final case class LegacyState(id: LegacyId, active: Boolean)" -- src/main/scala/distantlands/model/Cards.scala
git grep -n "def foundation(number: FoundationNumber, face: FoundationFace): Table" -- src/test/scala/distantlands/testkit/Table.scala
git grep -n "Slices 1, 2, 3a, 3b, 4, 5 and 6 landed" -- docs/superpowers/specs/2026-11-11-foundations-design.md
git grep -n "Digital: an empty window until Foundations exist." -- docs/rules/chronicle.md
git grep -n "Phase - Foundations" -- docs/ROADMAP.md
wc -l src/main/scala/distantlands/model/CoreOperations.scala
git grep -c "FoundationRestored" -- src/main
```

The first must print `slice3b-ok`: `f3136fb3e` is the merge of slice 3b
into `0.2.0-alpha`, and a worktree without it was cut from a stale branch:
stop and report. The second must print `slice6-ok`: `552aa27f6` is slice
6's head, which this plan was written and applied against. The next nine
greps must each print one line, and the `Phase - Foundations` grep two (the
"Now" intro and the phase heading). They name what this plan stands on:
the empty Shape Foundation window Task 2 fills, the Chronicle record that
already writes the live faces, the operation Task 1 adds beside, its wire
group, the Game Log's Beacon entry, the legacy state the offer reads, the
test table's face setter, the spec's status line, and the two docs lines
Task 4 replaces. `wc` must print 791 lines (slice 6's review may have
moved this by a line or two; more than 795 leaves no room for Task 1's
five lines, so stop and report). The last must print nothing and exit
with status 1: no restore exists yet. If a grep prints nothing while the
others print, slice 6's review moved or renamed that code: read the file,
and go on only if the code the tasks edit is still there in substance,
adapting the blocks that name it.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

The Chronicle fixture is `ChronicleWalk.throneHeldByChancellorP1`
(`src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala`): p1
won and is Chancellor, p1 holds the Throne, p2 the World and p3 the Beacon,
and every foundation lies normal with no source. `ChronicleWalk.walkedTo`
answers every parked decision with its first available option until the
walk parks on the decision named or on nothing; the driver chains the
Beacon, the Stars and the Finish, so a walk that parks on nothing has
journaled `ChronicleUpdated`. Because the plainest answer is the first
available option, Keep comes first: a walk that answers plainly restores
nothing, and no existing Chronicle test changes.

## Plan-time findings

The spec was checked against the code at `552aa27f6`, and the slice was
prototyped there, one commit per task. The blocks below were then applied
in order, step by step, to a fresh worktree at `552aa27f6`, where each "see
it fail" step failed as written, each task's tree matched the prototype's,
and the full gates passed. Where the spec and the code differ, this plan
follows the code, and Task 4 records it in the spec.

1. **The window is there and empty.** `ChronicleBeaconProcedure.steps` ends
   with `ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation,
   Vector.empty)`, and `ChronicleWindowsSuite` already pins the window's key
   `chronicle.beacon.shape-foundation`. Task 2 puts the step's nodes in it;
   no window, procedure or ref is added.
2. **The Chronicle record already carries the faces.** Slice 1 made
   `ChronicleRecord.campaignFields` write `campaign.foundations` into the
   evolved Chronicle (`ChronicleRecordSuite`, "the record keeps the live
   foundation faces"), so a restore that changes `campaign.foundations`
   reaches `ChronicleUpdated` with no change to the record.
3. **No operation changes a face.** Erosion is applied at game start, not
   journaled. A restore needs a new `PrimitiveOperation`; the Chronicle's
   own primitives (`RoleChanged`, `EdificeBuilt`, ...) are applied in
   `ChronicleStateOperations` and belong to the codec's
   `ChronicleOperation` group, and Task 1 follows them. `OperationApplication`
   ends with `case (result, _) => result`, so a missing application line
   would silently do nothing: Task 1's operation test catches that.
4. **Legacies are state already.** `LineageState.legacies` holds
   `LegacyState(id, active)` for every lineage, seated ones in
   `campaign.lineages` and box ones in `campaign.baseline.lineages`; the TTS
   import maps a flipped legacy to `active = true`. Game start refuses a
   seated lineage that holds legacies ("legacies require Legacies"), but a
   lineage in the box may hold them. `ChronicleRecord.campaignFields(ready)
   .lineages` is every lineage the Chronicle records, seated and boxed, so
   the offer reads active legacies there.
5. **Chronicle lines name their player.** Every Beacon line names the
   holder ("{0} chose {1}, {2} and {3}", "{0} welcomed {1} as a starting
   adviser"), as Setup's lines do (slice 4), even for the winner who is the
   active player. The spec's "Restored Foundation I to Imperial Maps" omits
   the actor; this plan writes "p3 restored Foundation I to Imperial Maps",
   and Task 4 says so in the spec's "As built".
6. **No note argument names a foundation.** `NoteArg` has no foundation or
   face case, and adding one would add a wire case and a Game Log renderer.
   A restore always reaches the card's one normal face, so each foundation
   gets its own fixed key, as Imperial Maps has one key per Atlas end
   (`filledFromRecent`, `filledFromForgotten`): six keys,
   `restored-foundation-i` to `restored-foundation-vi`.
7. **The Game Log needs the keys mapped.** The Beacon's `ProcedureLog`
   entry maps each of its note keys to a `LogKind`; its
   `narratedPrefixes = Vector("chronicle.beacon.")` already covers the new
   decision id, so the generic "Chose ..." line never shows for it.
8. **The plainest answer restores nothing.** `ChronicleWalk.plainestAnswer`
   takes the first available option. With Keep first, every existing walk
   with an altered face (`ChronicleEndToEndSuite`'s Foundation III,
   `ExploredSiteChronicleSuite`, `EmptySlotChronicleSuite`) keeps its faces
   and its assertions. Each Chronicle golden starts from the quiet table,
   whose faces are all normal, so no golden changes.
9. **What changes for existing games.** A game whose Chronicle Phase is
   parked on the Beacon's earlier questions resumes into a new question
   when a face is altered; old journals need not replay.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **One operation, applied with no catalog:** `FoundationRestored(number:
   FoundationNumber)`. Applying it sets the face to
   `FoundationState(Normal, Set.empty)` and refuses a face already normal
   (`InvalidDescription("Foundation II is not altered")`), which would be a
   doubled or reordered journal. Its wire is
   `{"kind": "foundation-restored", "number": 1}`; an unknown number is a
   typed `WireError` at `.number`.
2. **The step object.** `ShapeFoundation` in `gameplay/chronicle`, beside
   `WelcomeStranger`: `decisionId`, `heading`, `keep`, `keepLabel`,
   `restore(number)`, `restored: Map[FoundationNumber, NoteKey]`,
   `noteKeys`, `steps(catalog)` and `lockedBy(state, number)`.
3. **The question.** Heading "Shape Foundation: restore one altered
   foundation". Options, in order: Keep ("Keep every foundation as it
   lies", ref `Button("keep")`), then each altered face, I to VI ("Restore
   Foundation I to Imperial Maps", ref `Button("restore-i")`). A face an
   active legacy alters is wrapped `DecisionOption.Unavailable(...,
   OptionReason.Written("An active legacy alters it: The Needle"))`, the
   legacies named by their catalog names, sorted by id and joined with
   ", ". The answer check refuses an unavailable option, and the restore's
   `BuildOps` checks again.
4. **When nothing is asked.** No altered face, or every altered face held
   by an active legacy: the step builds no node, as Imperial Maps asks
   nothing with one Atlas site.
5. **The line** is built from the recorded answer, inside the `Branch`
   that knows the holder, as Imperial Maps' line reads `states.answered`.
6. **A test helper for the walk.** `ChronicleWalk.beaconWalkedTo(rules,
   table, decisionId)` starts the Beacon where the World ends and walks to
   `decisionId`, because `OathRules.next` is `private[gameplay]` and the
   projection suite in `application` cannot call it.
7. **Tests.** `ChronicleStateOperationsSuite` and `WalkerOperationWireSuite`
   take the operation (Task 1); `ShapeFoundationSuite` (new) takes each
   offer, the skip, the restore reaching `ChronicleUpdated`, the active
   legacy and replay (Task 2); `ShapeFoundationProjectionSuite` (new) and a
   golden script take the question as the table and the Game Log see it,
   and the golden's log property reloads its journal (Task 3).

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text, "You may flip
one foundation setup card from its altered side to its normal side unless
it's being altered by an active legacy." The code and tests below follow
them; if the owner decides otherwise, the task named changes.

1. **An active legacy is one some recorded lineage holds active.**
   Reading: a face is "being altered by an active legacy" when one of its
   alteration sources is a legacy that a lineage this Chronicle records,
   seated or in the box, holds with `active = true`. A source legacy that
   no lineage holds, or holds dormant, does not stop the restore (Task 2,
   "a face an active legacy alters ...", whose Foundation VI is altered by
   a dormant Iron Hand).
2. **Foundation III may be restored before Legacies.** Spec ruling 1 says
   so already; recorded here because its faces do nothing yet: restoring
   Public Ambitions turns it to Quiet Ambitions in the next game's
   Chronicle and changes nothing else (Task 2, "... III included ...").
3. **A face with no source may be restored.** Setup With No Empire erodes
   Foundations I and V with no source, and an imported Chronicle's altered
   face has none, so before Legacies every altered face may be restored
   (Task 2).
4. **Nothing is asked when nothing may be restored.** "You may flip one"
   is a choice only while some altered face is free. Reading: with no
   altered face, or with every altered face held by an active legacy, the
   holder is not asked (Task 2, "with every altered face held by an active
   legacy the holder is not asked"). The alternative is to ask with only
   Keep available, showing the held faces' reasons.
5. **Keep comes first, and keeping writes no line.** As Decline comes
   first at the Throne (Chronicle ruling 21). Reading: the holder may
   always decline the flip ("You may"), and declining tells nothing (Task
   2, "keeping every foundation restores none and writes no line").
6. **A restore clears the face's alteration sources.** Reading: the card is
   flipped to its normal side, so nothing alters it any more; a legacy that
   later turns it again adds itself anew (Task 1).
7. **The step runs third, after Welcome a Stranger.** The rulebook's
   order: "First, Introduce Newcomers ... Second, Welcome a Stranger ...
   Third, Shape Foundation." The restore takes effect in the campaign at
   once, and since the game is over it changes only the next game (Task 2).
8. **The holder is always a seated player.** Chronicle ruling 1 gives tasks
   only to players seated for the game, so a lineage in the box never
   restores; its active legacies still hold a face (ruling 1 above).
9. **The line names the holder.** As every Chronicle line names its player:
   "p3 restored Foundation I to Imperial Maps". The spec wrote it without
   the actor; Task 4 corrects the spec.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/CoreOperations.scala` | `FoundationRestored` (Task 1) |
| `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`, `ChronicleStateOperations.scala` | its application (Task 1) |
| `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` | its wire (Task 1) |
| `src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala` | new: the step (Task 2) |
| `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala` | fills the window, declares the keys (Task 2) |
| `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala` | maps the keys (Task 2) |
| `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`, `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala` | the operation (Task 1) |
| `src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala` | `beaconWalkedTo` (Task 2) |
| `src/test/scala/distantlands/gameplay/chronicle/ShapeFoundationSuite.scala` | new (Task 2) |
| `src/test/resources/notes/declared-keys.txt` | rewritten by its command (Task 2) |
| `src/test/scala/distantlands/application/ShapeFoundationProjectionSuite.scala` | new (Task 3) |
| `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`, `src/test/resources/gamelog/beacon-restored-foundation.actor.log`, `beacon-restored-foundation.other.log` | the golden (Task 3) |
| `docs/ROADMAP.md`, `docs/rules/chronicle.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `CONTEXT.md`, the spec | docs (Task 4) |

---

### Task 1: A journaled operation restores a foundation face

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Consumes: `FoundationNumber`, `FoundationFace`, `FoundationState`
  (`model/World.scala`); `RawOperationBatch.executeAll` (test kit).
- Produces: `final case class FoundationRestored(number: FoundationNumber)
  extends PrimitiveOperation`; `ChronicleStateOperations.foundationRestored(
  ready: ReadyGame, number: FoundationNumber): Either[OperationError,
  ReadyGame]` (`private[operations]`); wire kind `foundation-restored`.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`:

Replace

```scala
  /** Denizens of `suit` the quiet table does not have in play. */
```

with

```scala
  test("a restored foundation turns to its normal face and loses its " +
      "alteration sources"):
    val sourced = FoundationState(FoundationFace.Altered,
      Set(LegacyId("L05")))
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      foundations = campaign.foundations.updated(FoundationNumber.I,
        sourced)))).ready
    val after = accepted(run(ready, FoundationRestored(FoundationNumber.I)))
    assertEquals(after.game.campaign.foundations,
      ready.game.campaign.foundations.updated(FoundationNumber.I,
        FoundationState(FoundationFace.Normal, Set.empty)))

  test("a foundation on its normal face cannot be restored"):
    rejected(run(Table.start.ready, FoundationRestored(FoundationNumber.II)))

  /** Denizens of `suit` the quiet table does not have in play. */
```

**In** `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`:

Replace

```scala
  test("a site placement names its Atlas end, its region and the site's " +
```

with

```scala
  test("a foundation restore names its number, and an unknown number " +
      "decodes to a typed WireError"):
    val encoded = encodedStreamThatRoundTrips("foundation-restored",
      Vector(recordedOperation(FoundationRestored(FoundationNumber.I))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "foundation-restored", "number" -> 1))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("number") = 7
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].number",
        "unknown Foundation number 7")))

  test("a site placement names its Atlas end, its region and the site's " +
```

Then replace

```scala
      EdificeBuilt(GreatMarketCard.id, site),
      PlaceAtlasSite(AtlasEnd.Forgotten,
```

with

```scala
      EdificeBuilt(GreatMarketCard.id, site),
      FoundationRestored(FoundationNumber.III),
      PlaceAtlasSite(AtlasEnd.Forgotten,
```

(That list is the test "every CoreOperation variant round-trips through
the walker codec".)

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: FAIL to compile, with `Not found: FoundationRestored` in both
suites.

- [ ] **Step 3: Add the operation, its application and its wire**

**In** `src/main/scala/distantlands/model/CoreOperations.scala`:

Replace

```scala
final case class EdificeBuilt(edifice: EdificeId, site: SiteId)
    extends PrimitiveOperation
```

with

```scala
final case class EdificeBuilt(edifice: EdificeId, site: SiteId)
    extends PrimitiveOperation

/** Turns Foundation `number` from its altered face to its normal face and
  * clears its alteration sources (Shape Foundation, CR p. 39). */
final case class FoundationRestored(number: FoundationNumber)
    extends PrimitiveOperation
```

**In** `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`:

Replace

```scala
      case (result, EdificeBuilt(edifice, site)) =>
        result.flatMap(ChronicleStateOperations.edificeBuilt(_, edifice, site))
```

with

```scala
      case (result, EdificeBuilt(edifice, site)) =>
        result.flatMap(ChronicleStateOperations.edificeBuilt(_, edifice, site))
      case (result, FoundationRestored(number)) =>
        result.flatMap(ChronicleStateOperations.foundationRestored(_, number))
```

**In** `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`:

Replace (the file's last lines, the end of `edificeBuilt`)

```scala
        map = current.map.copy(sites = current.map.sites.updated(site,
          state.copy(denizens = state.denizens :+
            EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))))))
```

with

```scala
        map = current.map.copy(sites = current.map.sites.updated(site,
          state.copy(denizens = state.denizens :+
            EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))))))

  /** Only an altered face turns back: restoring a normal face would be a
    * doubled or reordered journal. */
  def foundationRestored(ready: ReadyGame, number: FoundationNumber)
      : Either[OperationError, ReadyGame] =
    val foundations = ready.game.campaign.foundations
    Either.cond(
      foundations.get(number).exists(_.face == FoundationFace.Altered),
      ready.updateCampaign(_.copy(foundations = foundations.updated(number,
        FoundationState(FoundationFace.Normal, Set.empty)))),
      InvalidDescription(s"Foundation $number is not altered"))
```

**In** `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`:

Replace

```scala
    LineagePointsScored | EdificeBuilt | SuitedReserveFromBox | PlaceAtlasSite
```

with

```scala
    LineagePointsScored | EdificeBuilt | SuitedReserveFromBox | PlaceAtlasSite |
    FoundationRestored
```

Then replace

```scala
    case EdificeBuilt(edifice, site) => ujson.Obj("kind" -> "edifice-built",
      "edifice" -> edifice.value, "site" -> site.value)
```

with

```scala
    case EdificeBuilt(edifice, site) => ujson.Obj("kind" -> "edifice-built",
      "edifice" -> edifice.value, "site" -> site.value)
    case FoundationRestored(number) => ujson.Obj(
      "kind" -> "foundation-restored", "number" -> number.value)
```

Then replace

```scala
    case "edifice-built" => Right(EdificeBuilt(
      EdificeId(value("edifice").str), SiteId(value("site").str)))
```

with

```scala
    case "edifice-built" => Right(EdificeBuilt(
      EdificeId(value("edifice").str), SiteId(value("site").str)))
    case "foundation-restored" => for
      number <- safeIntField(value.obj, "number", path)
      foundation <- FoundationNumber.all.find(_.value == number).toRight(
        InvalidValue(s"$path.number", s"unknown Foundation number $number"))
    yield FoundationRestored(foundation)
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: PASS.

Run: `wc -l src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`
Expected: 796 and 769 lines (each at most 800).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(chronicle): a journaled operation restores a foundation face" -m "FoundationRestored turns an altered foundation to its normal face and clears its alteration sources. Applying it refuses a face that is already normal, and its wire kind is foundation-restored with a required number." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The Beacon holder may restore one altered foundation

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ShapeFoundationSuite.scala` (new)
- Written: `src/test/resources/notes/declared-keys.txt` (by its command)

**Interfaces:**
- Consumes: Task 1's `FoundationRestored`; `ChronicleTasks.forHolder`,
  `ChronicleRecord.campaignFields`, `FoundationCards.face`,
  `ExecutableCatalog.legacy`, `ProcedureNotes.id`, `NoteSet`.
- Produces: `ShapeFoundation.decisionId: String`
  (`"chronicle.beacon.shape-foundation"`), `ShapeFoundation.heading: String`,
  `ShapeFoundation.keep: DecisionOptionRef.Button`,
  `ShapeFoundation.keepLabel: String`,
  `ShapeFoundation.restore(number: FoundationNumber): DecisionOptionRef.Button`,
  `ShapeFoundation.restored: Map[FoundationNumber, NoteKey]`,
  `ShapeFoundation.noteKeys: Vector[NoteKey]`,
  `ShapeFoundation.steps(catalog: ExecutableCatalog): Vector[Operation]`,
  `ShapeFoundation.lockedBy(state: ReadyGame, number: FoundationNumber):
  Vector[LegacyId]`; test helper `ChronicleWalk.beaconWalkedTo(rules:
  OathRules, table: Table, decisionId: String): OathTransition`.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala`:

Replace

```scala
  /** The state when `procedure` completed: `transition`, walked from
```

with

```scala
  /** `table`'s Beacon, which the driver starts where the World ends,
    * walked as [[walkedTo]] walks to `decisionId`. */
  def beaconWalkedTo(rules: OathRules, table: Table, decisionId: String)(
      using munit.Location): OathTransition =
    walkedTo(rules, accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleWorld)), decisionId)

  /** The state when `procedure` completed: `transition`, walked from
```

**Create** `src/test/scala/distantlands/gameplay/chronicle/ShapeFoundationSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Outcomes, RecordedOps, Table}
import distantlands.testkit.Table.p3
import distantlands.testkit.Unwrap.{accepted, rejected}

/** Shape Foundation, the Beacon's third step (CR p. 39). p3 holds the
  * Beacon. */
class ShapeFoundationSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** Tasks assigned, and the World's Separate Visions run: no Vision is
    * left in play, so the Chronicle can be recorded at the end. */
  private val tasksAssigned: Table = Table.from(
    ChronicleWalk.throneHeldByChancellorP1.ready.updateCurrent(current =>
      current.copy(commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(_.isInstanceOf[VisionId])))))

  /** `tasksAssigned` with Foundations I and III on their altered faces. */
  private val wideHorizonsAndPublicAmbitions: Table = tasksAssigned
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .foundation(FoundationNumber.III, FoundationFace.Altered)

  private val needle = LegacyId("L05")
  private val ironHand = LegacyId("L01")

  /** `tasksAssigned` with a lineage in the box holding The Needle active
    * and Iron Hand inactive; Foundation I is altered by The Needle and
    * Foundation VI by Iron Hand. */
  private val alteredByBoxedLegacies: Table = tasksAssigned.update(
    _.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        LineageId("lineage:boxed"), None, Role.Exile,
        Vector(LegacyState(needle, active = true),
          LegacyState(ironHand, active = false)), Vector.empty))),
      foundations = campaign.foundations
        .updated(FoundationNumber.I,
          FoundationState(FoundationFace.Altered, Set(needle)))
        .updated(FoundationNumber.VI,
          FoundationState(FoundationFace.Altered, Set(ironHand))))))

  /** The walk from the end of the World, every Beacon question before
    * Shape Foundation answered plainly: parked on Shape Foundation, or on
    * nothing when it asks nothing. */
  private def shaping(table: Table)(using munit.Location): OathTransition =
    ChronicleWalk.beaconWalkedTo(rules, table, ShapeFoundation.decisionId)

  /** `table`'s Shape Foundation answered `answer`, then the rest of the
    * Chronicle answered plainly to its record. */
  private def answeredThenRecorded(table: Table, answer: DecisionOptionRef)(
      using munit.Location): OathTransition =
    val at = shaping(table)
    val next = accepted(rules.resolveWalker(at.state, p3,
      ShapeFoundation.decisionId, ChooseOneAnswer(answer)))
    ChronicleWalk.answeringEverything(rules,
      OathTransition(next.state, at.events ++ next.events))

  private def options(transition: OathTransition)(using munit.Location)
      : Vector[DecisionOption] =
    ChronicleWalk.parked(transition.state).get.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(other.toString)

  private def labelOf(option: DecisionOption): String = option match
    case DecisionOption.Button(_, label) => label
    case DecisionOption.Unavailable(inner, _) => labelOf(inner)
    case other => fail(other.toString)

  /** The "restored" lines the walk journaled, as plain text. */
  private def restoredLines(transition: OathTransition): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon),
      ChronicleBeaconProcedure.noteKeys, transition.events)
      .filter(_.key.startsWith("restored-foundation-")).map(_.text)

  private def journaled(transition: OathTransition): Chronicle =
    RecordedOps.all(transition.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.last

  test("with no foundation altered the holder is not asked"):
    val walked = shaping(tasksAssigned)
    assertEquals(ChronicleWalk.parked(walked.state), None)
    assert(!RecordedOps.all(walked.events)
      .exists(_.isInstanceOf[FoundationRestored]), walked.events)

  test("the holder may keep every face or restore any altered one, III " +
      "included, and Keep comes first"):
    val walked = shaping(wideHorizonsAndPublicAmbitions)
    val parked = ChronicleWalk.parked(walked.state).get
    assertEquals(parked.owner, p3)
    assertEquals(options(walked).map(_.ref), Vector(ShapeFoundation.keep,
      ShapeFoundation.restore(FoundationNumber.I),
      ShapeFoundation.restore(FoundationNumber.III)))
    assertEquals(options(walked).map(labelOf), Vector(
      "Keep every foundation as it lies",
      "Restore Foundation I to Imperial Maps",
      "Restore Foundation III to Quiet Ambitions"))
    assert(options(walked).forall(_.available))

  test("restoring Wide Horizons turns Foundation I to Imperial Maps, says " +
      "so, and the journaled Chronicle carries it"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.restore(FoundationNumber.I))
    assertEquals(RecordedOps.all(done.events)
      .collect { case op: FoundationRestored => op },
      Vector(FoundationRestored(FoundationNumber.I)))
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    val altered = FoundationState(FoundationFace.Altered, Set.empty)
    assertEquals(Outcomes.ready(done.state).game.campaign
      .foundations(FoundationNumber.I), normal)
    assertEquals(restoredLines(done),
      Vector("p3 restored Foundation I to Imperial Maps"))
    val record = journaled(done)
    assertEquals(record.foundations.get(FoundationNumber.I), Some(normal))
    assertEquals(record.foundations.get(FoundationNumber.III), Some(altered))

  test("keeping every foundation restores none and writes no line"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.keep)
    assert(!RecordedOps.all(done.events)
      .exists(_.isInstanceOf[FoundationRestored]), done.events)
    assertEquals(restoredLines(done), Vector.empty)
    assertEquals(journaled(done).foundations,
      wideHorizonsAndPublicAmbitions.ready.game.campaign.foundations)

  test("a face an active legacy alters is shown unavailable with the " +
      "legacy's name, and cannot be restored; an inactive legacy's face can"):
    val walked = shaping(alteredByBoxedLegacies)
    assertEquals(options(walked), Vector(
      DecisionOption.Button(ShapeFoundation.keep,
        "Keep every foundation as it lies"),
      DecisionOption.Unavailable(DecisionOption.Button(
        ShapeFoundation.restore(FoundationNumber.I),
        "Restore Foundation I to Imperial Maps"),
        OptionReason.Written("An active legacy alters it: The Needle")),
      DecisionOption.Button(ShapeFoundation.restore(FoundationNumber.VI),
        "Restore Foundation VI to Wandering Flame")))
    rejected(rules.resolveWalker(walked.state, p3, ShapeFoundation.decisionId,
      ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.I))))
    val restored = accepted(rules.resolveWalker(walked.state, p3,
      ShapeFoundation.decisionId,
      ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.VI))))
    assertEquals(Outcomes.ready(restored.state).game.campaign
      .foundations(FoundationNumber.VI),
      FoundationState(FoundationFace.Normal, Set.empty))

  test("with every altered face held by an active legacy the holder is " +
      "not asked"):
    val needleOnly = alteredByBoxedLegacies
      .foundation(FoundationNumber.VI, FoundationFace.Normal)
    val walked = shaping(needleOnly)
    assertEquals(ChronicleWalk.parked(walked.state), None)
    assertEquals(journaled(walked).foundations(FoundationNumber.I),
      FoundationState(FoundationFace.Altered, Set(needle)))

  test("a restore replays from its journal to the same game"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.restore(FoundationNumber.III))
    assertEquals(accepted(done.events.foldLeft[
        Either[OathViolation, OathState]](
      Right(wideHorizonsAndPublicAmbitions.state))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)
```

The suite's tables: `tasksAssigned` is `BeaconTaskSuite`'s (Visions out of
the world deck, so the Chronicle can be recorded); The Needle (`L05`) and
Iron Hand (`L01`) are real legacies, so the reason names The Needle from
the catalog. The lineage in the box is the only place a legacy can be
before Legacies: game start refuses a seated lineage's legacies.

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeFoundationSuite"`
Expected: FAIL to compile, with `Not found: ShapeFoundation`.

- [ ] **Step 3: Write the step and put it in the Beacon**

**Create** `src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Shape Foundation, the Beacon's third step (CR p. 39): "You may flip one
  * foundation setup card from its altered side to its normal side unless
  * it's being altered by an active legacy." While some altered face may be
  * restored, the Beacon task holder chooses one altered face to restore,
  * or keeps them all, which is the first option. A face whose alteration
  * sources name a legacy that a lineage of this Chronicle holds active is
  * shown unavailable, naming that legacy. Before Legacies no face has such
  * a source, so every altered face may be restored, III included
  * (Foundations ruling 1).
  *
  * Line, as the Beacon's other steps name their player: "p3 restored
  * Foundation I to Imperial Maps".
  */
object ShapeFoundation:
  val decisionId: String = "chronicle.beacon.shape-foundation"
  val heading: String = "Shape Foundation: restore one altered foundation"
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val keepLabel: String = "Keep every foundation as it lies"

  def restore(number: FoundationNumber): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"restore-${slug(number)}")

  private def slug(number: FoundationNumber): String =
    number.toString.toLowerCase

  private def normalName(number: FoundationNumber): String =
    FoundationCards.face(number, FoundationFace.Normal).name

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  /** "p3 restored Foundation I to Imperial Maps", one key per foundation,
    * since a restore always turns the card to its one normal face. */
  val restored: Map[FoundationNumber, NoteKey] =
    FoundationNumber.all.map(number => number -> notes.key(
      s"restored-foundation-${slug(number)}", Vector(NotePart.Arg(0),
        NotePart.Text(s" restored Foundation $number to " +
          normalName(number))))).toMap
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) =>
      ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
        altered(state) match
          case faces if faces.forall(lockedBy(state, _).nonEmpty) =>
            Vector.empty
          case faces => Vector(
            Decide(decisionId, holder, DecisionQuery.ChooseOne(
              DecisionOption.Button(keep, keepLabel) +:
                faces.map(option(catalog, state, _)),
              heading = Some(heading))),
            BuildOps((live, pending) => restoring(live, chosen(pending)),
              required = true),
            Note(noteId, states => chosen(states.answered).map(number =>
              restored(number).sourceless(NoteArg.Player(holder))))))))

  /** The altered foundations, I to VI. */
  private def altered(state: ReadyGame): Vector[FoundationNumber] =
    FoundationNumber.all.filter(number => state.game.campaign.foundations
      .get(number).exists(_.face == FoundationFace.Altered))

  /** The active legacies among `number`'s alteration sources: those a
    * lineage this Chronicle records, seated or in the box, holds active. */
  def lockedBy(state: ReadyGame, number: FoundationNumber): Vector[LegacyId] =
    val active = ChronicleRecord.campaignFields(state).lineages
      .flatMap(_.legacies).filter(_.active).map(_.id).toSet
    state.game.campaign.foundations.get(number).toVector
      .flatMap(_.alterationSources.toVector.sortBy(_.value))
      .filter(active)

  private def option(catalog: ExecutableCatalog, state: ReadyGame,
      number: FoundationNumber): DecisionOption =
    val shown = DecisionOption.Button(restore(number),
      s"Restore Foundation $number to ${normalName(number)}")
    lockedBy(state, number) match
      case Vector() => shown
      case legacies => DecisionOption.Unavailable(shown, OptionReason.Written(
        "An active legacy alters it: " + legacies.map(legacy =>
          catalog.legacy(legacy).fold(legacy.value)(_.name)).mkString(", ")))

  /** The foundation the answer restores; none for Keep. */
  private def chosen(pending: PendingTree): Option[FoundationNumber] =
    chosen(pending.answered)

  private def chosen(answered: Vector[Answered]): Option[FoundationNumber] =
    answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }.flatMap(ref => FoundationNumber.all.find(restore(_) == ref))

  /** The restore, while the face is still altered and no active legacy
    * holds it. */
  private def restoring(state: ReadyGame, number: Option[FoundationNumber])
      : Either[OathViolation, Vector[CoreOperation]] = number match
    case None => Right(Vector.empty)
    case Some(foundation) => Either.cond(
      altered(state).contains(foundation) &&
        lockedBy(state, foundation).isEmpty,
      Vector(FoundationRestored(foundation)),
      OathViolation.InvalidEventOrder(
        s"Foundation $foundation cannot be restored"))
```

**In** `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala`:

Replace

```scala
    IntroduceNewcomers.noteKeys ++ WelcomeStranger.noteKeys
```

with

```scala
    IntroduceNewcomers.noteKeys ++ WelcomeStranger.noteKeys ++
      ShapeFoundation.noteKeys
```

Then replace

```scala
    // Shape Foundation (CR p. 39) waits for Foundations.
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation, Vector.empty))
```

with

```scala
    ChronicleWindows.step(PowerWindow.ChronicleShapeFoundation,
      ShapeFoundation.steps(catalog)))
```

**In** `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`:

Replace

```scala
  OfferCitizenship, ProtectAndDispossess, ShapeEmpire, SwearOath,
  WelcomeStranger,
  ChronicleStarsProcedure}
```

with

```scala
  OfferCitizenship, ProtectAndDispossess, ShapeEmpire, ShapeFoundation,
  SwearOath, WelcomeStranger,
  ChronicleStarsProcedure}
```

Then replace

```scala
        WelcomeStranger.keptAdviser -> LogKind.Decision)),
      narratedPrefixes = Vector("chronicle.beacon."), isSection = false)
```

with

```scala
        WelcomeStranger.keptAdviser -> LogKind.Decision) ++
        ShapeFoundation.restored.values.map(_ -> LogKind.Decision)),
      narratedPrefixes = Vector("chronicle.beacon."), isSection = false)
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeFoundationSuite"`
Expected: PASS (7 tests).

- [ ] **Step 5: Rewrite the note-key snapshot and read it**

Run: `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: FAIL, only "every power declares exactly the notes the snapshot
records": the six new keys are not in the snapshot yet.

Run:
`NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
then `git diff -- src/test/resources` and `git status --short src/test/resources`.
Expected status: exactly ` M src/test/resources/notes/declared-keys.txt`.
Expected diff: exactly these six lines added, after `kept-newcomer` and
before `returned-dispossessed`, and nothing else:

```text
+procedure.chronicle-beacon restored-foundation-i = {0} restored Foundation I to Imperial Maps
+procedure.chronicle-beacon restored-foundation-ii = {0} restored Foundation II to Powerful Tribes
+procedure.chronicle-beacon restored-foundation-iii = {0} restored Foundation III to Quiet Ambitions
+procedure.chronicle-beacon restored-foundation-iv = {0} restored Foundation IV to Ties of Fellowship
+procedure.chronicle-beacon restored-foundation-v = {0} restored Foundation V to Mob's Favor
+procedure.chronicle-beacon restored-foundation-vi = {0} restored Foundation VI to Wandering Flame
```

- [ ] **Step 6: Run the Chronicle, setup and Game Log suites**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.setup.* distantlands.application.gamelog.* distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS (584 tests in the plan's own run). Every existing
Chronicle walk keeps its faces, because the plainest answer is Keep, and
no golden changes. If a golden fails here, stop and report.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala src/test/scala/distantlands/gameplay/chronicle/ShapeFoundationSuite.scala src/test/resources/notes/declared-keys.txt
git commit -m "feat(chronicle): the Beacon holder may restore one altered foundation" -m "Shape Foundation, the Beacon's last step, asks the Beacon task holder to keep every foundation or restore one altered face to normal, while one may be restored. A face an active legacy alters is shown unavailable, naming the legacy. The restore is journaled, the evolved Chronicle carries it, and the line names the holder: p3 restored Foundation I to Imperial Maps." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The question at the table and its golden log

**Files:**
- Test: `src/test/scala/distantlands/application/ShapeFoundationProjectionSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
- Written: `src/test/resources/gamelog/beacon-restored-foundation.actor.log`, `src/test/resources/gamelog/beacon-restored-foundation.other.log`

**Interfaces:**
- Consumes: Task 2's `ShapeFoundation.{decisionId, heading, keep, restore}`
  and `ChronicleWalk.beaconWalkedTo`; `WalkerDecisionProjector`,
  `GamePresentationProjector`, `ScopedProjectionContext`; the Game Log
  script kit (`LogScripts.atTable`, `Script`, `MenuPlay`).
- Produces: nothing new; tests and one golden pair.

This task pins what Task 2 built as the table and the Game Log see it. The
projection tests pass when written: run them to see them pass, and if one
fails, the fault is in Task 2's code, not the test. The golden is the
failing step: it is missing until Step 3 writes it. `GameLogPropertiesSuite`
then reloads the script's journal through the service and compares it with
the live game ("scan ends on the state load reconstructs"), which is the
replay of a restore through the wire.

- [ ] **Step 1: Write the tests and the script**

**Create** `src/test/scala/distantlands/application/ShapeFoundationProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.{ChronicleWalk, ShapeFoundation}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Outcomes
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.present

/** The Shape Foundation question as the table sees it (Foundations design,
  * slice 7). p3 holds the Beacon. */
class ShapeFoundationProjectionSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** p1's won game after the Sun, Foundation I on Wide Horizons and
    * Foundation VI on the Festival, VI altered by The Needle, which a
    * lineage in the box holds active; walked to Shape Foundation. */
  private val parkedOnShapeFoundation: ReadyGame =
    val needle = LegacyId("L05")
    val table = ChronicleWalk.throneHeldByChancellorP1
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .update(_.updateCampaign(campaign => campaign.copy(
        baseline = campaign.baseline.copy(lineages = Vector(LineageState(
          LineageId("lineage:boxed"), None, Role.Exile,
          Vector(LegacyState(needle, active = true)), Vector.empty))),
        foundations = campaign.foundations.updated(FoundationNumber.VI,
          FoundationState(FoundationFace.Altered, Set(needle))))))
    Outcomes.ready(ChronicleWalk.beaconWalkedTo(rules, table,
      ShapeFoundation.decisionId).state)

  private val projector = new WalkerDecisionProjector(catalog,
    new GamePresentationProjector(catalog))

  test("the holder sees the question: Keep, then each altered face, the " +
      "one an active legacy holds unavailable with its reason"):
    val own = present(projector.project(ScopedProjectionContext(
      parkedOnShapeFoundation, Some(p3))), "p3's decision")
    assertEquals(own.decisionId, ShapeFoundation.decisionId)
    assertEquals(own.query.flatMap(_.heading), Some(ShapeFoundation.heading))
    assertEquals(own.query.toVector.flatMap(_.offeredOptions)
      .map(option => (option.id, option.label, option.state, option.reason)),
      Vector(
        ("keep", "Keep every foundation as it lies", "available", None),
        ("restore-i", "Restore Foundation I to Imperial Maps", "available",
          None),
        ("restore-vi", "Restore Foundation VI to Wandering Flame",
          "unavailable", Some("An active legacy alters it: The Needle"))))

  test("no one else is asked"):
    Vector(p1, p2).foreach(viewer => assertEquals(projector.project(
      ScopedProjectionContext(parkedOnShapeFoundation, Some(viewer))), None))
```

**In** `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`:

Replace

```scala
import distantlands.gameplay.chronicle.OfferCitizenship
```

with

```scala
import distantlands.gameplay.chronicle.{OfferCitizenship, ShapeFoundation}
```

Then replace

```scala
  val named: Vector[(String, () => Script)] =
    Vector("chronicle-started" -> (() => chronicleStarted),
      "throne-offered" -> (() => throneOffered))
```

with

```scala
  /** The first game, with Foundation I on Wide Horizons: at the Beacon
    * p1, who holds it, restores it to Imperial Maps (Foundations design,
    * slice 7). */
  def beaconRestoredFoundation(using munit.Location): Script =
    val (service, acting) = atTable("beacon-restored-foundation", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == ShapeFoundation.decisionId =>
        ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.I))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("beacon-restored-foundation", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("chronicle-started" -> (() => chronicleStarted),
      "throne-offered" -> (() => throneOffered),
      "beacon-restored-foundation" -> (() => beaconRestoredFoundation))
```

- [ ] **Step 2: Run the projection tests, and the golden suite to see it fail**

Run: `./sbtw "testOnly distantlands.application.ShapeFoundationProjectionSuite"`
Expected: PASS (2 tests).

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL, only for `beacon-restored-foundation`: "missing golden
src/test/resources/gamelog/beacon-restored-foundation.actor.log; run with
GAMELOG_GOLDEN=write and review it".

- [ ] **Step 3: Write the golden and read it**

Run:
`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
then `git status --short src/test/resources`.
Expected status: exactly
`?? src/test/resources/gamelog/beacon-restored-foundation.actor.log` and
`?? src/test/resources/gamelog/beacon-restored-foundation.other.log`.

Expected `beacon-restored-foundation.actor.log` in full. It is
`chronicle-started.actor.log` with the restore line after the welcome line,
and each later line's sequence 5 higher:

```text
0.0 round 0 | Round 1
0.1 turn 0 | [player:p3|p3]'s turn
0.2 phase 0 | Act
5.0 phase 0 | Rest
9.0 round 0 | Round 2
9.1 turn 0 | [player:p1|p1]'s turn
9.2 phase 0 | Wake
10.0 victory 0 | [player:p1|p1] won as the Usurper
14.0 phase 0 | Chronicle
17.0 delta 1 | The Empire broke
18.0 delta 1 | [player:p1|p1] became Chancellor
20.0 decision 1 | [player:p1|p1] took the Throne task
25.0 decision 1 | [player:p1|p1] took the World task
30.0 decision 1 | [player:p1|p1] took the Beacon task
38.0 decision 1 | [player:p2|p2] took the World task
43.0 delta 1 | Moved 1 Imperial warband to [site:site:ancient-city|Ancient City]
50.0 delta 1 | [player:p1|p1] scored [amount:3 lineage points]
55.0 decision 1 | The Oathkeeper goal became Protection
64.0 decision 1 | [player:p2|p2] protected the stack of [card:179|Threatening Roar], [card:180|Fae Merchant] and [card:181|Second Chance]
69.0 decision 1 | [player:p2|p2] kept a Denizen as a starting adviser
70.0 delta 1 | [cards:182 183 100 back denizen back denizen back denizen|6 cards] were dispossessed
82.0 decision 1 | [player:p1|p1] chose Discord, Arcane and Order
92.0 decision 1 | [player:p1|p1] welcomed [card:21|Naysayers] as a starting adviser
97.0 decision 1 | [player:p1|p1] restored Foundation I to Imperial Maps
99.0 delta 1 | [site:site:broken-peaks|Broken Peaks] was archived
100.0 delta 1 | [site:site:buried-giant|Buried Giant] was archived
101.0 delta 1 | [site:site:deep-woods|Deep Woods] was archived
102.0 delta 1 | [site:site:desolate-shore|Desolate Shore] was archived
103.0 delta 1 | [site:site:dunes|Dunes] was archived
104.0 delta 1 | [site:site:fair-isle|Fair Isle] was archived
105.0 delta 1 | [site:site:golden-valley|Golden Valley] was archived
106.0 delta 1 | [site:site:ancient-city|Ancient City] stays in the Empire
```

`beacon-restored-foundation.other.log` is the same but for two lines, as
in `chronicle-started.other.log`:
`69.0 decision 1 | [player:p2|p2] kept [card:179|Threatening Roar] as a starting adviser`
and
`92.0 decision 1 | [player:p1|p1] welcomed a Denizen as a starting adviser`.
The restore line reads the same for every viewer.

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS (320 tests in the plan's own run), the property suites
included: the new script's journal reloads to the same game, and its
decision is told by the restore line.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/application/ShapeFoundationProjectionSuite.scala src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala src/test/resources/gamelog/beacon-restored-foundation.actor.log src/test/resources/gamelog/beacon-restored-foundation.other.log
git commit -m "test(chronicle): pin Shape Foundation at the table" -m "The projected question is the Beacon task holder's alone and shows a face an active legacy alters unavailable with its reason, and a golden log records a restore, whose journal the log properties reload to the same game." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Close the Foundations phase in the docs, and verify

**Files:**
- Modify: `docs/ROADMAP.md`
- Modify: `docs/rules/chronicle.md`
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `CONTEXT.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

**Interfaces:**
- Consumes: everything Tasks 1 to 3 built; nothing new is produced.

Docs are plain prose. `docs/rules/ambiguities.md` and
`docs/rules/implementation-traceability.md` hold very long lines (numbered
items and table rows), so some blocks below are parts of a line: replace
exactly the text shown and leave the rest of the line. The roadmap deletes
finished work outright: no checked box and no "landed" note. This is the
batch's last task, so it carries the spec's whole "Docs" list: the
roadmap, the three rules docs, `CONTEXT.md` and the spec.

- [ ] **Step 1: The roadmap**

**In** `docs/ROADMAP.md`:

Replace

~~~markdown
**Phase - Cleanup tasks** holds what finished phases left behind.
**Phase - Foundations** is next.
~~~

with

~~~markdown
**Phase - Cleanup tasks** holds what finished phases left behind.
~~~

(Before the Foundations design added it, the "Now" intro was this one
sentence.)

Then replace

~~~markdown
### Phase - Foundations

[Design](superpowers/specs/2026-11-11-foundations-design.md), seven slices.

- [ ] **Foundations.** Every face but Foundation III's works: the Beacon's
  Shape Foundation.

### Phase - Campaign battle summary
~~~

with

~~~markdown
### Phase - Campaign battle summary
~~~

Then replace

~~~markdown
  opens advisers. Foundation III's two faces (Quiet Ambitions, Public
  Ambitions) deal legacies, so game start skips them until then.
~~~

with

~~~markdown
  opens advisers. Foundation III's two faces (Quiet Ambitions, Public
  Ambitions) deal legacies, so game start skips them until then. A legacy
  that alters a foundation adds itself to the face's alteration sources,
  which the Beacon's Shape Foundation already reads.
~~~

That last item is the **Legacies** item of **Phase - Chronicle steps**: it
still says Foundation III waits for it, and now also names the one seam
Shape Foundation leaves for it.

- [ ] **Step 2: The rules docs, `CONTEXT.md` and the spec**

**In** `docs/rules/chronicle.md` (the Beacon, step 3):

Replace

~~~markdown
   - Digital: an empty window until Foundations exist.
~~~

with

~~~markdown
   - Digital: the holder is asked while some altered face may be restored. Keep every foundation comes first, then each altered face, I to VI. A face an active legacy alters is shown unavailable, naming the legacy ([Foundations rulings](ambiguities.md#foundations-rulings), items 1 and 14). The restored face is normal in the journaled Chronicle, with no alteration source, and one log line names it: "p3 restored Foundation I to Imperial Maps".
~~~

**In** `docs/rules/ambiguities.md`:

Replace (part of item 2 of "Component-data dependencies")

~~~markdown
 Foundation III's faces are skipped until Legacies. The [traceability table]
~~~

with

~~~markdown
 The Beacon's Shape Foundation restores one altered face in the Chronicle. Foundation III's faces are skipped until Legacies. The [Foundations rulings](#foundations-rulings) hold the batch's rulings and readings, and the [traceability table]
~~~

Then replace

~~~markdown
## Source relationship cautions
~~~

with

~~~markdown
## Foundations rulings

Rulings and readings for the six foundation setup cards ([Foundations design](../superpowers/specs/2026-11-11-foundations-design.md)). Items 1 to 7 were taken with the project owner on 2026-11-10 and 2026-11-11. Items 8 to 14 are the readings each slice's plan took, the most literal reading of the New Foundations text; they await the owner's confirmation.

1. **Foundation III waits for Legacies.** Both faces are accepted at game start and do nothing. The Beacon may still restore an altered III (Foundations design, ruling 1).
2. **Explore borrows only Recover's dice.** No power that names Recover or recovering applies to Explore: Silver Charm, Relic Patrols and Drowned Library stay out, though Silver Charm's text names only spending Supply to roll defense dice. Jinx ("after you roll [attack-die] or [defense-die] for any reason") applies, through the generic reroll window (ruling 2).
3. **No Empire sets the Reliquary aside.** "Discard all relics from the Reliquary. They return in the Chronicle Phase." The Reliquary's relics stay in the campaign all game and never enter play; the slot fill cannot draw them, and the Chronicle's Stock Reliquary step adds to them (ruling 3).
4. **The shared bank has no count.** Taking favor from it changes nothing but the taker's board (ruling 5).
5. **Explore, the Festival and a game with no Empire.** Any role may Explore. The Festival's marker is placed automatically at each of its holder's Rests, with no question, since the face says "must". With no Empire, the Oathkeeper title and both banners start as in a first game.
6. **A banner moved to the shared bank.** Hall of Mockery and Lost Guardians "move it to the shared bank": a held banner goes back to the shared bank, when the site is explored and at Setup alike. A Chancellor who starts with the People's Favor or the Darkest Secret loses it at Setup's end when the matching ruined edifice is on the map (slice 3b).
7. **Imperial Maps' question.** The Recent option names the Atlas's first site, which the rulebook makes public: "Recent Sites (Buried Giant first)". The Forgotten end's site is not named, and nothing else about the Atlas is shown before the fill. This closes the deferral [R27](#empire-rulings) recorded (slice 4).
8. **Movers and Shakers and Teeming World** (slice 2). Under Movers and Shakers every board starts with 6 favor, the Chancellor's included. Under Teeming World a lineage that stores three or more advisers draws none and keeps them all. Players draw in seat order, and a lineage's stored advisers come before the ones it draws. A site-only denizen drawn is kept and cannot be revealed in Setup. Reveal Cards still runs. An uneven rest leaves the Cradle pile, then the Provinces pile, one card more, and the first card dealt to a pile is its top. A Search of the Visions-only deck draws one Vision and stops; once the five are drawn the world deck is empty and cannot be searched.
9. **Setup With No Empire** (slice 3a). The absent Chancellor's lineage becomes an Exile in the box with the Citizens. A former Empire with no site is refused, as an Empire's is ([R28](#empire-rulings)). A Foundation I already on Wide Horizons keeps its sources. Relic slots are filled Cradlemost first while the relic deck lasts, and the rest stay empty. If no site holds bandits, an Exile may place their pawn at any site. A power that takes a Reliquary relic finds a set-aside Reliquary empty. A first game places its eight sites whatever Foundation I's face. Powers that make a player a Citizen are ignored as in a later game with an Empire, a known gap until the Settings Feature phase. The Sun's Crown Chancellor still discards the Reliquary when an Exile wins, the set-aside relics with it, which narrows item 3's Stock Reliquary clause to a Chancellor's win; the alternative is for Crown Chancellor to skip the discard while the Reliquary is set aside.
10. **Explore** (slice 3b). Only a power that hooks one of Explore's own windows changes an Explore, and none does; Brass Army still asks its holder as their pawn is placed. The next slot is the first empty slot going Hinterward. Who rules the Hintermost site does not matter. The explorer chooses Recent or Forgotten after the roll succeeds, without seeing either site, and a later Explore may take the other end; one site left is not asked about and is logged as a Recent Site. The site is populated as Foundation I prints while the relic deck and the bandit bank last, and its denizens add no favor to the suit banks. WHEN EXPLORED resolves after the pawn is placed, and no ruler is asked to consent. Broken Clock advances the round marker at once and does nothing on round 8. An Explore, stopped or not, is an action. Lost Doors and Ruined Temple wait for Legacies.
11. **Imperial Maps** (slice 4). An Atlas too short to fill every slot is refused at game start. A Citizen standing in for an absent Chancellor answers. A relic deck that runs short leaves the last relic slots open. Foundation II's favor counts a placed site's ruined edifice and is added as each site is placed. One answer fills every slot, the Forgotten end placing the Atlas's last site in the Cradlemost empty slot. With one site in the Atlas nothing is asked and the line says Recent; an Empire of eight sites asks nothing. A placed site's SETUP half resolves at Setup's end.
12. **Grand Council** (slice 5). Every card play the engine plans is "playing a card". A card goes to a distant Imperial site only as it could go to the pawn site, and a full Imperial site takes it only in place of one discarded there. The discard is optional, one card per play, stacks with the Homeland's, and holds at the pawn site only when it is Imperial. Nobody is asked to consent, and printed protections and locks still apply. An Imperial site is one the Empire's warbands hold when the play is planned. The holder's role does not matter. An unheld Grand Council has no previous holder, so all its favor goes to the banks. A first game with an altered V starts the Grand Council in the shared bank.
13. **Festival** (slice 6). Every other player is the holder's enemy, whatever their role. "Their powers" are the powers of the denizens at the marked site, edifices included; relics and the site's own powers are untouched, and a denizen's action modifier is one of its powers. "Persistent" is the power as printed. Placing favor or secrets on a denizen is what City Wall's ban reads. Consent is asked outside a Campaign only. The marker starts beside the map, first goes on it at the holder's first Rest, and each Rest places it at the pawn's site, even where it already lies; a holder with no pawn on the map places nothing. With nobody holding the banner the marker guards nothing and stays where it lies. A take that walks no ribbon leaves the marker, which then guards for the new holder. "ONE" on an empty banner burns nothing. Under Secret Testament the holder takes every secret, and the marker still leaves the map. A first game with an altered VI starts the Festival in the shared bank. An automatic power of a guarded card is refused for an enemy like any other non-persistent use, and Jinx at the festival is refused to an enemy who rules it. Open: whether a Conspiracy that takes the Festival also removes the marker; the code removes it, as a Raid does.
14. **Shape Foundation** (slice 7). "You may flip one foundation setup card from its altered side to its normal side unless it's being altered by an active legacy." An active legacy is one that a lineage this Chronicle records, seated or in the box, holds active; a source legacy that no lineage holds active does not stop the restore. The holder is asked only while some altered face may be restored, and Keep every foundation comes first. A restore clears the face's alteration sources. A face Setup With No Empire eroded has no source and may be restored. The step runs after Welcome a Stranger, in the printed order.

## Source relationship cautions
~~~

The new section moves the spec's rulings 1 to 3 and 5, the design calls
the owner accepted, the two calls decided with the owner in slices 3b and
4, and every slice's readings to the rules docs, as the spec's "Docs" list
asks. Ruling 4 (Explore's log line) is wording, not a rule, and stays in
the spec. [R27](#empire-rulings) was already closed by slice 4; item 7
points at it.

**In** `docs/rules/implementation-traceability.md`:

Replace (the Beacon task row, a whole line)

~~~markdown
| Beacon task | **Partial** | [CR p. 39](chronicle.md#4-the-beacon) | Introduce Newcomers (with [ruling 15](ambiguities.md#chronicle-rulings)) and Welcome a Stranger are tested in `gameplay/chronicle/BeaconTaskSuite.scala`; the whole Chronicle runs end to end and replays in `gameplay/chronicle/ChronicleEndToEndSuite.scala`. | Shape Foundation is an empty window until Foundations (B2/B5). Recorded in the Chronicle by the Stars task. |
~~~

with

~~~markdown
| Beacon task | **Tested** | [CR p. 39](chronicle.md#4-the-beacon) | Introduce Newcomers (with [ruling 15](ambiguities.md#chronicle-rulings)) and Welcome a Stranger are tested in `gameplay/chronicle/BeaconTaskSuite.scala`. Shape Foundation restores one altered foundation face unless an active legacy alters it ([Foundations rulings](ambiguities.md#foundations-rulings), item 14): `gameplay/chronicle/ShapeFoundation.scala`, tested in `gameplay/chronicle/ShapeFoundationSuite.scala` and `application/ShapeFoundationProjectionSuite.scala`. The whole Chronicle runs end to end and replays in `gameplay/chronicle/ChronicleEndToEndSuite.scala`. | Legacies add the alteration sources Shape Foundation reads. Recorded in the Chronicle by the Stars task. |
~~~

Then replace (the end of the Setup table's "Six foundation setup effects" row)

~~~markdown
Imperial Maps fills from the end the Chancellor chooses ([R27](ambiguities.md#empire-rulings)). Foundation III's faces wait for Legacies. |
~~~

with

~~~markdown
Imperial Maps fills from the end the Chancellor chooses ([R27](ambiguities.md#empire-rulings)). The Beacon's Shape Foundation restores one altered face (the Beacon task row). Foundation III's faces wait for Legacies; the batch's rulings are the [Foundations rulings](ambiguities.md#foundations-rulings). |
~~~

**In** `CONTEXT.md`, a new subsection before "Undo":

Replace

~~~markdown
### Undo
~~~

with

~~~markdown
### Foundations

**Face**:
One of a foundation setup card's two sides, normal or altered. The campaign
keeps one for each of the six cards, and the next game's Setup reads them.
Shape Foundation restores an altered face to normal.
_Avoid_: side (a banner has sides), variant

**Alteration source**:
A legacy that keeps a foundation's face altered. Shape Foundation cannot
restore a face while one of its sources is active. A face that Setup With No
Empire eroded, or that an imported Chronicle stored altered, has none.
_Avoid_: lock, altering legacy

**Empty slot**:
A place on the map that holds no site. The Cradle has two slots and the
Provinces and the Hinterland three each. The table draws an empty slot as an
empty frame, and Explore or Imperial Maps fills the next one going
Hinterward.
_Avoid_: gap, hole, missing site

**Hintermost site**:
The last site in Hinterward order; empty slots do not count. Under Wide
Horizons a pawn there may Explore, and with an Empire an Exile may start at
the Hintermost Empire site.
_Avoid_: last site, edge site

**Explore**:
The major action Wide Horizons gives. From the Hintermost site the explorer
rolls 2 defense dice per Supply against a difficulty of 4, then places the
site at the Recent or Forgotten end of the Atlas in the next empty slot and
moves their pawn there. It borrows Recover's dice and nothing else: only
powers on its own windows change it.
_Avoid_: recover a site, expand

**Festival marker**:
The token the Festival's holder places over the cards at their pawn's site
at the start of each Rest; that site is the marked site. A marker beside the
map guards nothing. Both of the Festival's ribbons remove it.
_Avoid_: festival token, festival site

**Set-aside Reliquary**:
The Reliquary of a game Setup With No Empire started. Its relics stay in the
campaign all game, no one can take one in play, and the Chronicle's Stock
Reliquary step adds to them.
_Avoid_: discarded Reliquary, empty Reliquary

### Undo
~~~

**In** `docs/superpowers/specs/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b, 4, 5 and 6 landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Every slice landed: 1, 2, 3a, 3b, 4, 5, 6
and 7.
~~~

Then replace

~~~markdown
- **Tests.** Each offer; the skip; the restored face reaching
  `ChronicleUpdated`; a hand-built face with an active legacy as its source
  is not offered.
~~~

with

~~~markdown
- **Tests.** Each offer; the skip; the restored face reaching
  `ChronicleUpdated`; a hand-built face with an active legacy as its source
  is not offered.
- **As built.** `ShapeFoundation` builds the step in the Beacon's last
  window. Keep every foundation is the first option, as Decline is at the
  Throne, and each altered face follows, I to VI; a face an active legacy
  alters is shown unavailable, naming the legacy. The restore is a
  journaled `FoundationRestored`, which turns the face normal and clears
  its alteration sources, and `ChronicleRecord` carries it into
  `ChronicleUpdated`. Each foundation has its own line, since a restore
  always reaches the one normal face, and the line names the holder, as
  every Beacon line names its player: "p3 restored Foundation I to Imperial
  Maps". Keeping writes no line.
- **Readings taken with the plan,** for the owner to confirm. An active
  legacy is one that a lineage this Chronicle records, seated or in the box,
  holds active; a source legacy that no lineage holds active does not stop
  the restore. The holder is asked only while some altered face may be
  restored. A restore clears the face's sources. A face Setup With No
  Empire eroded has no source and may be restored. The step runs after
  Welcome a Stranger, in the printed order.
~~~

Then replace

~~~markdown
- `docs/ROADMAP.md`: **Phase - Foundations** is deleted with the batch, and
  the **Grand Council and Festival banner faces** item goes with slice 6.
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  the readings of slices 2 and 3a, and R27 closed) and
  `docs/rules/implementation-traceability.md`.
~~~

with

~~~markdown
- `docs/ROADMAP.md`: **Phase - Foundations** is deleted with the batch, and
  the **Grand Council and Festival banner faces** item goes with slice 6.
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  the readings of slices 2 to 7, and R27 closed) and
  `docs/rules/implementation-traceability.md`.
~~~

If the owner has ruled on any item of "Rulings needing the owner's review"
by the time you run this task, write the ruling in place of the reading,
in the spec's slice 7 readings and in item 14 of the "Foundations rulings",
and say so in your report. If you took a reading of your own while
working, add it to both.

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%. The plan's own run of these tasks at `552aa27f6` passed
5915 tests at 95.34% statements and 89.68% branches.

One test is slow under coverage: in the plan's first full run,
`GameLogDecisionSuite`'s "decisions an action line already tells post no
Chose line" took 37 seconds and hit munit's default 30-second timeout. It
formats only older scripts (Search, Muster, Trade, Recover, Negotiation,
Oathkeeper and Setup), none of which this slice touches, and the rerun
passed. If that one test times out, run the gate again; if it fails a
second time, or any other test fails, stop and report. Do not raise its
timeout in this slice.

Run: `./sbtw "frontend/test"` (the frontend suites, which the full run
skips)
Expected: PASS (935 tests in the plan's own run).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. The note-key snapshot and the golden pair were committed
with Tasks 2 and 3, and nothing else changed.

Run: `git grep -n "Phase - Foundations\|an empty window until Foundations\|Shape Foundation is an empty window\|waits for Foundations" -- docs/ROADMAP.md docs/rules docs/architecture CONTEXT.md src`
Expected: no output. Nothing still calls Shape Foundation empty or points
at the deleted phase.

Run: `python3 scripts/check-markdown-links.py`
Expected: it fails with exactly these 34 pre-existing broken links, all in
older specs that an unrelated docs move left behind, and no other line:

```text
Markdown link check failed:
- docs/superpowers/specs/2026-09/2026-09-19-campaign-walker-design.md: missing ../../architecture/bounded-campaign.md
- docs/superpowers/specs/2026-09/2026-09-19-negotiation-walker-design.md: missing ../../architecture/all-exile-negotiation.md
- docs/superpowers/specs/2026-09/2026-09-20-powers-design.md: missing ../../ROADMAP.md
- docs/superpowers/specs/2026-09/2026-09-22-card-shape-and-inspection-design.md: missing ../../operations/phase-5-follow-ups.md
- docs/superpowers/specs/2026-09/2026-09-22-card-shape-and-inspection-design.md: missing ../../testing/card-shape-verification.md
- docs/superpowers/specs/2026-09/2026-09-24-parked-decision-route-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-24-walker-continuation-removal-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-25-decision-form-tag-separation-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-25-modifier-flow-module-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-25-session-drafts-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-25-table-session-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-09/2026-09-25-typed-decision-form-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-06-undo-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-07-travel-destination-walker-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-08-consent-system-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-08-follow-up-actions-design.md: missing ../../architecture/follow-up-actions.md
- docs/superpowers/specs/2026-10/2026-10-15-parked-decision-query-read-sites-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-15-typed-log-span-wire-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-16-game-log-wording-ledger.md: missing ../../testing/audits/game-log-wording-audit.md
- docs/superpowers/specs/2026-10/2026-10-16-procedures-journal-their-notes-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-17-campaign-kills-at-their-moment-design.md: missing ../../ROADMAP.md
- docs/superpowers/specs/2026-10/2026-10-17-campaign-kills-at-their-moment-design.md: missing ../../architecture/bounded-campaign.md
- docs/superpowers/specs/2026-10/2026-10-18-decision-id-vocabulary-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-19-hidden-outcome-depth-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-19-log-operation-strictness-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-22-card-driver-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-23-parked-roll-retirement-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-23-power-harness-consolidation-design.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-23-reviewed-catalog-retirement-decision.md: missing ../../../CONTEXT.md
- docs/superpowers/specs/2026-10/2026-10-23-reviewed-catalog-retirement-decision.md: missing ../../architecture/rule-resolution.md
- docs/superpowers/specs/2026-10/2026-10-28-chronicle-browsing-design.md: missing ../../rules/ambiguities.md#chronicle-rulings
- docs/superpowers/specs/2026-10/2026-10-29-empire-phase-design.md: missing ../../rules/ambiguities.md#empire-rulings
- docs/superpowers/specs/2026-11/2026-11-09-distantlands-rename-design.md: missing ../../operations/data-policy.md
- docs/superpowers/specs/2026-11/2026-11-09-distantlands-rename-design.md: missing ../../operations/releases.md#versions-and-compatibility
```

If any other line appears, a link this task wrote is broken: fix it.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/rules/chronicle.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md CONTEXT.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 7 and close the phase" -m "The roadmap drops Phase - Foundations, the Chronicle rules describe Shape Foundation, the ambiguities gather the batch's rulings and readings, the traceability table marks the Beacon tested, CONTEXT.md gains the batch's terms, and the spec records every slice landed with slice 7 as built and its readings." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## For later

- **Legacies** (roadmap **Phase - Chronicle steps**): a legacy that alters
  a foundation adds itself to `FoundationState.alterationSources` when the
  Sun's Flip Legacies step flips the face, and Shape Foundation's offer
  (`ShapeFoundation.lockedBy`) already refuses a face an active source
  holds. Foundation III's faces wait there too.
- **Edit Chronicle** may edit a face directly; it needs no Beacon.

## Self-review

**Spec coverage** (slice 7 and the Docs list):

| Spec statement | Task |
|---|---|
| `ChronicleShapeFoundation` gets the Beacon task holder's question: restore one altered face, or keep them all | 2 (`ShapeFoundation.steps`, `ChronicleBeaconProcedure`) |
| Offer: every altered face whose sources hold no active legacy; III included before Legacies | 2 ("... III included ...", "a face an active legacy alters ..."; rulings 1 to 3) |
| No altered face, no question | 2 ("with no foundation altered the holder is not asked"; ruling 4 extends it) |
| Log: "Restored Foundation I to Imperial Maps" | 2, 3 (the line names the holder; finding 5, ruling 9) |
| The restored face reaches `ChronicleUpdated` | 2 ("... the journaled Chronicle carries it") |
| Tests: each offer; the skip; `ChronicleUpdated`; a hand-built active-legacy source is not offered | 2 |
| Replay equals live | 2 (fold of the journal), 3 (the golden's reload) |
| The projection of the question for the holder and others | 3 |
| Docs: roadmap, `chronicle.md`, `ambiguities.md`, traceability, `CONTEXT.md`, the status line | 4 |
| Full gates | 4, Step 3 |

**Verified by running:** the prototype was committed per task in a
scratch worktree at `552aa27f6`; the plan's blocks were then applied in
order, step by step, to a second fresh worktree at `552aa27f6`. Each "see
it fail" step failed exactly as written, each task's tree matched the
prototype's, and the final tree passed `./sbtw clean coverage test
coverageReport` (5915 tests at 95.34% statements and 89.68% branches), `./sbtw "frontend/test"`
(935 tests), `scripts/check-architecture.py` and the link
check's 34 pre-existing failures, with `src/test/resources` clean.

**Placeholders:** none. Every step that changes code shows the code, and
each file written by a command shows its expected text or diff.

**Names used across tasks:** `FoundationRestored`,
`ChronicleStateOperations.foundationRestored` (Task 1);
`ShapeFoundation.{decisionId, heading, keep, keepLabel, restore, restored,
noteKeys, steps, lockedBy}`, `ChronicleWalk.beaconWalkedTo` (Task 2, used
again in Task 3). The suites' helpers name what they build:
`tasksAssigned`, `wideHorizonsAndPublicAmbitions`, `alteredByBoxedLegacies`,
`shaping`, `answeredThenRecorded`, `restoredLines`, `journaled`,
`parkedOnShapeFoundation`, `beaconRestoredFoundation`. Each is spelled the
same wherever it appears.
