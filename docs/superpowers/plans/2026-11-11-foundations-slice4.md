# Foundations, Slice 4: Imperial Maps' Choice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In a later game with a Chancellor and Foundation I on its normal
face, Imperial Maps, game start leaves the slots past the Empire's sites
empty, and Setup's walk asks the Chancellor, right after the Reliquary
pick, whether to fill them from Recent or Forgotten Sites. Every empty slot
is then filled from that end going Hinterward, each placed site adding
Foundation II's favor for its denizens, and pawn placement offers the new
sites. This closes Empire ruling R27's deferral, which always took the
Recent end.

**Architecture:** A new Setup step, `ImperialMaps`, in `gameplay/setup`
beside `SetupProcedure`: a `Decide` owned by the Chancellor, a line naming
the end, and a `Repeat` that places one site per pass with slice 3b's
`AtlasPlacement.at` and `PlaceAtlasSite`, the same batch moving 1 favor
per placed denizen from the shared bank to its suit's bank
(`AtlasPlacement.suitFavor`). `GameStartRules` stops placing those sites,
so the relic deal and the banks' per-denizen favor for them move from the
materializer into the walk. The pawn question reads its sites when it is
reached instead of when the tree is built. The projection and the frontend
need no new shape.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings", "Approach", "Slice 3" (its "Empty slots", "Wide
Horizons at game start", "Explore" and "Setup With No Empire" subsections,
which this slice must leave unchanged) and "Slice 4: Imperial Maps'
choice". Slice 3b's plan,
[Foundations slice 3b](2026-11-11-foundations-slice3b.md), built what this
slice stands on; its "For slice 4" section names what is reused.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`). This slice
  touches neither `model/CoreOperations.scala` (780 lines after slice 3b)
  nor `serialization/WalkerOperationCodec.scala`: it adds no operation and
  no wire field. The largest file it grows is
  `gameplay/setup/SetupProcedure.scala`, to about 315 lines.
- `gameplay` must not read a catalog `.text` or `rulesText`. `model`
  imports nothing from `gameplay`. `gameplay/actions` imports nothing from
  `gameplay/powers` (`scripts/check-architecture.py`).
  `gameplay/actions/explore` already imports `gameplay/setup`
  (`AtlasPlacement`); this slice keeps that direction.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. A test that names a symbol the task has not built
  yet fails by not compiling, and that is the failure to see. Tests go in
  the repo's existing suites where one covers the subject. A test helper's
  name says what it builds.
- Wire and journal changes need not keep old journals decodable or
  replayable. This slice adds no wire field. It adds one decision id,
  `setup.imperial-maps`, answered with the buttons `recent` and
  `forgotten`, and three note keys under `procedure.setup`:
  `filled-from-recent`, `filled-from-forgotten` and `placed-site`. They are
  wire values: do not rename them. A journal of a later game under Imperial
  Maps recorded before this slice no longer replays: its `GameStarted` now
  evolves to a map with empty slots, and its Setup walk parks at the new
  question (Plan-time finding 7).
- New Foundations rules only. The printed setup cards are
  `OATH-SETUP04a-NF` (Imperial Maps), `OATH-SETUP05a-NF` (Powerful Tribes)
  and `OATH-SETUP05b-NF` (Movers and Shakers) in
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (each entry's `text:` comes before its `name:`; read it by id). The
  rulebook is `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`:
  "Empire" (about line 238), "Foundation I: Imperial Maps" (about line
  286), "Foundation II: Powerful Tribes" (about line 313) and "Public and
  Private Information" (about line 1464). Read them there; make no web
  call.
- If a rule is unclear while you work, do not guess silently and do not
  stop: take the most literal reading of the New Foundations text, add it
  to the spec's slice 4 readings (Task 4, Step 2), and name it in your
  report.
- An unavailable option is shown disabled with its reason, never hidden.
  This slice offers none: both ends are always available, and the question
  is not asked when it cannot matter (Design decision 5).
- A Game Log line omits the actor on their own turn, and its wording stays
  parallel to the lines beside it. Setup has no turn: every Setup line
  names its player ("p1 took a relic from the Reliquary", "p1 placed their
  pawn at Ancient City"), so this slice's lines name the Chancellor too
  (Design decision 6).
- Note keys change in Task 2 only, and Game Log goldens in Tasks 2 and 3
  only, and only as those tasks show. Their rewrite commands are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
  After a rewrite, read `git diff -- src/test/resources` and
  `git status --short src/test/resources` line by line against the text the
  task shows. If anything else changed, stop and report: do not commit a
  golden you cannot explain.
- Frontend work follows `DESIGN.md`. This slice changes no frontend file:
  the question is a plain choose-one the generic walker panel draws, and
  its lines sit on the Game Log's Neutral band.
- The words are "slot" for a place on the map a site can lie in, "empty
  slot" for one that holds none, "Hintermost" for the last site in
  Hinterward order, "Atlas" for the box of sites not on the map, "Recent"
  and "Forgotten" for its two ends, and "fill" for Imperial Maps placing a
  site in every empty slot.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `abc801d62` (slice 3b's branch head, with its first review fixes) with
  the earlier tasks of this plan applied, and occurs once in its file. "Then replace" is the next block in
  the same file. A "with" block repeats the unchanged lines of its
  "Replace" block, so an edit that only adds lines still shows where they
  go. "Create" gives a file's full text. If a block does not match because
  slice 3b's review fixes moved the text, find the same code by its content
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

Work in a git worktree cut from `0.2.0-alpha` after slice 3b has been
merged into it. In the worktree, link the main checkout's tooling before
the first `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor bbfb403a7 HEAD && echo slice3a-ok
git merge-base --is-ancestor abc801d62 HEAD && echo slice3b-ok
git grep -n "def at(catalog: ExecutableCatalog, ready: ReadyGame, end: AtlasEnd)" -- src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala
git grep -n "final case class PlaceAtlasSite(end: AtlasEnd, region: Region" -- src/main/scala/distantlands/model/CoreOperations.scala
git grep -n "def placedDenizens: Vector\[SiteDenizenState\]" -- src/main/scala/distantlands/model/World.scala
git grep -n "enum AtlasEnd(val key: String)" -- src/main/scala/distantlands/model/World.scala
git grep -n "_: PlaceAtlasSite => true" -- src/main/scala/distantlands/gameplay/walker/HiddenOutcomes.scala
git grep -n "private def endLabel(end: AtlasEnd): String = end match" -- src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala
git grep -n "atlasSites = chronicle.atlasBox.take(slots)" -- src/main/scala/distantlands/gameplay/setup/GameStartRules.scala
git grep -n "val reliquaryDecisionId: String = \"setup.reliquary-pick\"" -- src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala
git grep -n "def mapCutIntoAtlas(kept: Int)" -- src/test/scala/distantlands/testkit/Table.scala
git grep -n "Slices 1, 2, 3a and 3b landed" -- docs/superpowers/specs/2026-11-11-foundations-design.md
git grep -c "ImperialMaps" -- src/main/scala
```

The first must print `slice3a-ok`: `bbfb403a7` is the merge of slice 3a
into `0.2.0-alpha`, and a worktree without it was cut from a stale branch:
stop and report. The second should print `slice3b-ok`: `abc801d62` is the
slice 3b head this plan was applied to, and `0.2.0-alpha`'s merge of slice
3b (`f3136fb3e`) holds the same tree. If only `slice3b-ok` is missing while
every grep prints, slice 3b was merged again with later review fixes: go
on. Each grep but the last must print one line. They name
what this plan stands on: slice 3b's `AtlasPlacement.at`, `PlaceAtlasSite`,
`StoredSite.placedDenizens`, `AtlasEnd`, the placement as a hidden outcome,
Explore's end labels, which Task 1 moves, the line of game start that
Task 2 changes, Setup's Reliquary pick, the test table's Atlas step, and
the spec's status line. The last must print nothing and exit with status 1:
no Imperial Maps step exists yet. If a grep prints nothing while the others
print, slice 3b's review moved or renamed that code: read the file, and go
on only if the code the tasks edit is still there in substance, adapting
the blocks that name it.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

The later-game fixture (`LaterGameFixture.chronicle(l1, l2)`) is the
first-game fixture's Chronicle with its first two Atlas sites made the
Empire's, each storing one denizen: the Empire holds Ancient City and
Broken Peaks, which fill the Cradle, and the Atlas holds Buried Giant at
its Recent end, then Deep Woods, Desolate Shore, Dunes, Fair Isle, and
Golden Valley at its Forgotten end: exactly the six empty slots. l1 is the
seated Chancellor, l2 a Citizen and l3 an Exile, and p1 goes first.

## Plan-time findings

The spec was checked against slice 3b's code at `1a5c130dd`, and the
slice was prototyped there. The blocks below were then applied in order to
a fresh worktree at `abc801d62`, slice 3b's branch head once its first
review fixes landed (they touch none of this plan's files), where each
task's tests and the full gates pass. Where the spec and the code differ, this plan follows the code, and
Task 4 corrects the spec.

1. **Game start placed the fill.** `GameStartRules.buildPlan` took
   `atlasSlots` sites from the Atlas's Recent end and handed them to the
   materializer as `FirstGameSetupPlan.atlasSites`, which dealt their relic
   slots (`assignRelics`) and counted their denizens into the favor banks
   (`favorBanks`) before any Setup question. Task 2 gives the plan no Atlas
   site with an Empire: game start places the Empire's sites only, and the
   relic deal and the banks' per-denizen favor for the filled sites move to
   the walk. A first game's eight sites and Setup With No Empire's former
   Empire sites are still placed by the materializer, unchanged.
2. **The relic deal gives the same result for Recent.** With an Empire the
   materializer dealt no relic to the Empire's sites, so the relic deck at
   the fill is the deal's relic order whole, and `PlaceAtlasSite` deals
   each placed site's open slots from its top in slot order, as
   `assignRelics` did. A Recent fill therefore puts the same relics on the
   same sites as before; only the moment changes, after the Reliquary pick
   instead of at game start. The Reliquary is not the relic deck, so the
   pick never changes the deal.
3. **The banks give the same totals.** Game start's banks now count the
   Empire's denizens; each placement adds 1 favor per card it puts in its
   denizen slots, its ruined edifice included, which is what game start
   counted for those sites. Nothing moves bank favor between game start and
   the fill, so the totals after the fill equal the old game start's for
   Recent, under both faces of Foundation II.
4. **The card accounting holds.** Game start keeps every Atlas site in
   `campaign.atlas` with its stored cards, and slice 3b made the card index
   hold a stored site's edifice, so the inventory is the same before and
   after each placement (`OperationStateInvariant`). The edifice deck
   already leaves out every edifice the Chronicle stores. `DomainValidation`
   already accepts a map with empty slots (Wide Horizons).
5. **The pawn question read its sites when the tree was built.**
   `SetupProcedure.playerStep` computed `SetupPawnSites.legal(ready, ...)`
   from the state the tree was built on. The walk is rebuilt from live
   state at every command, and the first pawn question after the fill is
   always the Chancellor's, whose Empire sites the fill does not change, so
   the stale read never showed. But the fill and that question run in one
   command, and `SetupWalkDriver` walks one tree to the end, so an Exile's
   sites would miss the placed bandit sites there. Task 2 reads the sites
   when the question is reached, inside a `Branch` whose shape is always
   the one `Decide`. No test pins a Setup decision's path.
6. **A step's shape must not depend on the fill.** A parked walk re-selects
   every `Branch` on its resume path from live state. The Imperial Maps
   step is therefore declared from facts game start fixed:
   `campaign.baseline`, the Chronicle, gives the Empire's site count and
   the Atlas's size, and a Chancellor and Foundation I's face never change
   in Setup. The step is a plain `Sequence` with no `Branch`, and the fill
   is a `Repeat` whose guard reads the live map, so a walk resumed at a
   later pawn question finds every node where it was.
7. **What changes for existing games.** A later game under Imperial Maps
   now parks first at the Chancellor's question (or at the Reliquary pick
   before it), its first pawn question comes one park later, the Game Log
   gains a line for the end and one per site, and the Reliquary pick can
   no longer be undone once the map is filled (Design decision 8).
   Journals recorded before this slice do not replay. First games, Setup
   With No Empire, and Wide Horizons with an Empire are unchanged: their
   trees, logs and states are as before (`ImperialMapsSetupSuite`).
8. **Eight suites read the old game start.** The full suite at the
   prototype's Task 2 failed exactly these tests, each fixed in Task 2:
   `LaterGameSetupSuite` ("a remaining site keeps its stored cards ...
   (R27)", which moves to the new suite, and "each favor bank adds one per
   denizen ..."), `LaterGameSetupChoicesSuite` ("an empty Reliquary skips
   the pick" and the Exile pawn sites test), `AlteredFoundationSetupSuite`
   ("Movers and Shakers gives every board 6 favor ..."),
   `WideHorizonsSetupSuite` (its comparison with Imperial Maps' game
   start), `DeclaredNoteKeysSuite` and the `later-game-setup` golden.
   Everything else that starts a later game, the Chronicle suites
   included, already ran Setup to its end and passes unchanged.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **`ImperialMaps` is a Setup step of its own file.**
   `gameplay/setup/ImperialMaps.scala` holds the decision id, the heading,
   the three note keys, `applies(ready)`, `chosenEnd(answered)` and
   `step(catalog, ready, chancellor)`. `SetupProcedure.tree` puts the step
   right after the Reliquary pick, and `SetupProcedure.noteKeys` adds its
   keys, so the note-key registry needs no change.
2. **The favor is a `Move` beside each placement.** The fill's `BuildOps`
   returns `PlaceAtlasSite` and then `AtlasPlacement.suitFavor`: one
   `Move(Piece.Favor(n), SharedBank, FavorBank(suit))` per suit the placed
   site's denizen slots hold, in suit order. No flag on the operation and
   no new operation: Explore's placement still adds no favor (slice 3b
   ruling 6), and the shared bank, which has no count (Foundations ruling
   5), is an unbounded source the pipeline already knows.
3. **One home for the Atlas's end words and suits.** `AtlasPlacement`
   gains `endOption(end)`, `endOf(ref)` and `endLabel(end)`, which Explore
   used privately, and `suitOf(catalog, card)`, the suit Foundation II
   reads, which the materializer's `favorBanks` now calls. Explore keeps
   its public `ExploreProcedure.endOption`, which tests use, as a call to
   the shared one.
4. **The question.** `setup.imperial-maps`, owned by the Chancellor,
   heading "Imperial Maps: fill the map from", options "Recent Sites"
   (`recent`) and "Forgotten Sites" (`forgotten`), the words Explore uses.
   It names no site (Ruling 4).
5. **When nothing is asked.** The step runs only with a Chancellor seated,
   Foundation I normal and at least one slot past the Empire's sites. The
   question is asked while the Chronicle's Atlas holds two sites or more;
   with one, both ends give the same site, so the fill runs unasked from
   the Recent end, as Explore's one-site Atlas does.
6. **The lines.** "p1 filled the map from Forgotten Sites" (or Recent),
   then "p1 placed Golden Valley on the map" for each site in the order
   placed. Setup has no turn and every Setup line names its player, so
   these name the Chancellor, where the spec wrote "Filled the map from
   Forgotten Sites" (Task 4 corrects it). A note has no free-text
   argument, so each end has its key.
7. **Pawn sites are read when asked** (Plan-time finding 5).
8. **The fill stays an undo barrier.** `PlaceAtlasSite` is a hidden outcome
   wherever it runs (`HiddenOutcomes`): the fill shows the sites and what
   they store and deals relics nobody saw. Undo therefore cannot take back
   the Chancellor's answer, nor the Reliquary pick before it, which undo
   could take back until now. Making Setup an exception would let a
   Chancellor see one end's sites and choose again.
9. **Tests.** `AtlasPlacementSuite` takes the shared words and the favor
   moves (Task 1). `ImperialMapsSetupSuite` (new) takes the step: the
   question, each end, the stored cards, the relic deal and a short relic
   deck, the bank totals
   under both faces of Foundation II, card accounting, an Exile's pawn
   sites, the lines, a stand-in Chancellor, the one-site Atlas, a full
   Empire, the setups that are unchanged, and the short Atlas (Task 2).
   `LaterGameSetupChoicesSuite` takes the projected question and replay
   equals live through the service's journal, `UndoBarrierPlaySuite` the
   barrier, and a `later-game-forgotten-sites` golden the Forgotten log
   (Task 3).

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **An Atlas shorter than the empty slots is refused at game start, as
   today.** Card: "They must fill these slots with Recent Sites or
   Forgotten Sites from the Atlas". Rulebook: "They fill **all** these
   slots". Reading: a Chronicle whose Atlas cannot fill every slot past the
   Empire's sites is malformed; game start keeps its refusal "at least 6
   atlas sites are required, got 5" (Task 2). The fill's guard also stops
   on an empty Atlas, so a slot could only stay empty if that refusal were
   lifted.
2. **A Citizen standing in for an absent Chancellor answers.** Rulebook,
   Players: "If there's no Chancellor, a random Citizen becomes the
   Chancellor first." Reading: the stand-in is the Chancellor at Setup, so
   they take the Reliquary pick and answer Imperial Maps (Task 2).
3. **A relic deck that runs short leaves the last slots open.** Card:
   "Fill the site's empty relic slots by drawing relics from the relic
   deck." Rulebook, Components: "If you are prompted to gain or take
   components, but the source of that component does not have enough, gain
   or take as many as possible." Reading: the fill deals while the deck
   lasts, Cradlemost first, as Explore and Setup With No Empire do.
   **This changes game start:** a later game under Imperial Maps whose
   relic deck could not fill the Recent end's slots was refused with "not
   enough relics to fill site slots"; it now starts. A first game is still
   refused (Task 2, "a relic deck too short for the placed sites ...").
4. **The Chancellor chooses without seeing the Atlas.** Rulebook, Public
   and Private Information: "In the Atlas box, all information is private
   except the first site on the Recent Sites side and denizens and
   edifices stored with it." Reading: the question names the two ends and
   no site. The Recent end's first site is public by that text, but the
   table shows no Atlas yet, for the Chancellor or anyone; that view is a
   gap slice 3b left too ("a view of the Atlas for the explorer"), not a
   rule this slice withholds (Task 3).
5. **Foundation II's per-denizen favor counts ruined edifices.** Cards:
   "for each denizen on the map, add an extra [favor] to the matching
   favor bank" (Powerful Tribes) and "place [favor] on the matching favor
   bank" (Movers and Shakers). Reading: the owner's ruling that "denizens"
   covers edifices applies, and game start already counted a first game's
   ruined Homeland edifices. A placed site's ruined edifice adds 1 favor to
   its suit's bank; a stored relic adds none (Tasks 1 and 2).
6. **The favor is added as each site is placed.** Rulebook, Foundations:
   "Resolve the six foundation setup cards (4-9) in order". Reading:
   Foundation II's count follows the whole fill; adding each site's favor
   in the batch that places it gives the same totals, since nothing
   between the placements moves bank favor (Task 2).
7. **One answer fills every slot, Forgotten taking the Atlas's tail
   first.** Card: "but not both". Reading: the end is chosen once, before
   any site is placed, and the slots are filled going Hinterward one site
   at a time from that end, so under Forgotten the Atlas's last site takes
   the Cradlemost empty slot and the site before it the next. With one
   site in the Atlas nothing is asked and the line says "Recent Sites"
   (Task 2).
8. **An Empire of eight sites asks nothing.** "The Chancellor fills the
   remaining empty site slots": with none, there is no question and no
   line (Task 2).
9. **A placed site's SETUP half resolves at Setup's end, as before.** The
   fill is not an Explore, so `WhenExplored` is not folded; the edifice
   powers that hook `SetupEnd` find the placed sites on the map there, as
   they found game start's (Task 2; unchanged behaviour, pinned by the
   Broken Clock golden test in `GameLogHeadlineSuite`).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala` | `endOption`, `endOf`, `endLabel`, `suitOf`, `suitFavor` |
| `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala` | uses the shared end words |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | `favorBanks` uses `suitOf`; comments name the walk's fill |
| `src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala` | new: the step, its question and its lines |
| `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` | the step after the Reliquary pick; its note keys; pawn sites read when asked |
| `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` | game start places no Atlas site with an Empire |
| `src/main/scala/distantlands/model/Setup.scala` | `FirstGameSetupPlan.atlasSites`'s comment |
| `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala` | the shared words and the favor moves |
| `src/test/scala/distantlands/gameplay/setup/ImperialMapsSetupSuite.scala` | new: the step |
| `src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala`, `AlteredFoundationSetupSuite.scala`, `WideHorizonsSetupSuite.scala`, `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala` | read game start as it now is |
| `src/test/resources/notes/declared-keys.txt`, `src/test/resources/gamelog/later-game-setup.*.log` | rewritten by their commands (Task 2) |
| `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`, `UndoBarrierPlaySuite.scala` | the projected question, replay, the barrier (Task 3) |
| `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala`, `src/test/resources/gamelog/later-game-forgotten-sites.*.log` | the Forgotten golden (Task 3) |
| `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, `docs/architecture/game-setup.md`, `docs/ROADMAP.md`, the spec | docs (Task 4) |

---

### Task 1: The Atlas's end words and Foundation II's favor per placed denizen

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala`

**Interfaces:**
- Consumes: slice 3b's `AtlasPlacement.at(catalog, ready, end):
  Either[OathViolation, AtlasPlacement.Planned]`, `Planned.stored:
  AtlasEntry.StoredSite`, `StoredSite.placedDenizens:
  Vector[SiteDenizenState]`, `AtlasEnd.all`, `AtlasEnd.key`.
- Produces, in `object AtlasPlacement`:
  - `def endOption(end: AtlasEnd): DecisionOptionRef.Button` (the button
    `end.key`);
  - `def endOf(ref: DecisionOptionRef): Option[AtlasEnd]`;
  - `def endLabel(end: AtlasEnd): String` ("Recent Sites", "Forgotten
    Sites");
  - `def suitOf(catalog: ExecutableCatalog, card: SiteDenizenState):
    Option[Suit]`;
  - `def suitFavor(catalog: ExecutableCatalog, planned: Planned):
    Vector[Move]`: one `Move(Piece.Favor(n),
    PositionedLocation(Location.SharedBank),
    PositionedLocation(Location.FavorBank(suit)))` per suit with `n > 0`,
    in `Suit.all` order.
  Task 2 calls `endOption`, `endOf`, `endLabel` and `suitFavor`; Task 3's
  tests call `endOption`.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala`:

Replace

```scala
import distantlands.gameplay.powers.setup.GreatMarketCard
```

with

```scala
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.MapLibraryCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.WizardsConclaveCard
```

Then replace

```scala
    assertEquals(rejected(AtlasPlacement.at(catalog, unprinted,
      AtlasEnd.Recent)), OathViolation.InvalidEventOrder(
        "site:nowhere is not a site the catalog prints"))
```

with

```scala
    assertEquals(rejected(AtlasPlacement.at(catalog, unprinted,
      AtlasEnd.Recent)), OathViolation.InvalidEventOrder(
        "site:nowhere is not a site the catalog prints"))

  test("an end is answered by its key and reads as Recent Sites or " +
      "Forgotten Sites"):
    assertEquals(AtlasEnd.all.map(AtlasPlacement.endOption), Vector(
      DecisionOptionRef.Button("recent"),
      DecisionOptionRef.Button("forgotten")))
    assertEquals(AtlasEnd.all.map(AtlasPlacement.endLabel),
      Vector("Recent Sites", "Forgotten Sites"))
    assertEquals(AtlasEnd.all.map(end =>
      AtlasPlacement.endOf(AtlasPlacement.endOption(end))),
      AtlasEnd.all.map(Some(_)))
    assertEquals(AtlasPlacement.endOf(DecisionOptionRef.Button("stop")), None)

  test("a placed site gives each suit's bank 1 favor from the shared bank " +
      "per card in its denizen slots, its edifice included, and none for " +
      "a relic"):
    // Deep Woods stores the Great Market (Discord), Wizard's Conclave and
    // Map Library (both Arcane) and Sticky Fire.
    val ready = threeSitesAndAnAtlas.storedInAtlas(DeepWoodsSite,
      GreatMarketCard, WizardsConclaveCard, MapLibraryCard, StickyFireCard)
      .ready
    val planned = accepted(AtlasPlacement.at(catalog, ready, AtlasEnd.Recent))
    def fromSharedBank(favor: Int, suit: Suit): Move = Move(Piece.Favor(favor),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.FavorBank(suit)))
    assertEquals(AtlasPlacement.suitFavor(catalog, planned), Vector(
      fromSharedBank(1, Suit.Discord), fromSharedBank(2, Suit.Arcane)))
    val bare = accepted(AtlasPlacement.at(catalog, threeSitesAndAnAtlas.ready,
      AtlasEnd.Recent))
    assertEquals(AtlasPlacement.suitFavor(catalog, bare), Vector.empty)
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.AtlasPlacementSuite"`
Expected: FAIL. The suite does not compile: `endOption`, `endLabel`,
`endOf` and `suitFavor` are not members of `AtlasPlacement`.

- [ ] **Step 3: Implement**

The words and the suit move to `AtlasPlacement`; Explore and the
materializer call them.

**In** `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala`:

Replace

```scala
  yield Planned(stored, PlaceAtlasSite(end, region, printed.capacity,
    printed.relicSlots, printed.startingResources))
```

with

```scala
  yield Planned(stored, PlaceAtlasSite(end, region, printed.capacity,
    printed.relicSlots, printed.startingResources))

  /** The answer that names `end`, wherever a player chooses one. */
  def endOption(end: AtlasEnd): DecisionOptionRef.Button =
    DecisionOptionRef.Button(end.key)

  /** The end an answer names, if it names one. */
  def endOf(ref: DecisionOptionRef): Option[AtlasEnd] =
    AtlasEnd.all.find(endOption(_) == ref)

  /** The words of `end`'s option. */
  def endLabel(end: AtlasEnd): String = end match
    case AtlasEnd.Recent => "Recent Sites"
    case AtlasEnd.Forgotten => "Forgotten Sites"

  /** The suit of a card in a site's denizen slots, an edifice's included:
    * the bank Foundation II adds its favor to. */
  def suitOf(catalog: ExecutableCatalog, card: SiteDenizenState)
      : Option[Suit] = card match
    case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
    case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)

  /** Foundation II's favor for a site Imperial Maps places: 1 from the
    * shared bank to the matching suit's bank per card the site puts in its
    * denizen slots, its ruined edifice included, one move per suit in suit
    * order. An Explore adds none: the count is a setup step. */
  def suitFavor(catalog: ExecutableCatalog, planned: Planned): Vector[Move] =
    val suits = planned.stored.placedDenizens.flatMap(suitOf(catalog, _))
    Suit.all.map(suit => suit -> suits.count(_ == suit)).collect {
      case (suit, count) if count > 0 => Move(Piece.Favor(count),
        PositionedLocation(Location.SharedBank),
        PositionedLocation(Location.FavorBank(suit)))
    }
```

**In** `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`:

Replace

```scala
  def endOption(end: AtlasEnd): DecisionOptionRef.Button =
    DecisionOptionRef.Button(end.key)
```

with

```scala
  def endOption(end: AtlasEnd): DecisionOptionRef.Button =
    AtlasPlacement.endOption(end)
```

Then replace

```scala
    }.flatMap(ref => AtlasEnd.all.find(endOption(_) == ref))
```

with

```scala
    }.flatMap(AtlasPlacement.endOf)
```

Then replace

```scala
          endLabel(end))), heading = Some(endHeading)))))
```

with

```scala
          AtlasPlacement.endLabel(end))), heading = Some(endHeading)))))
```

Then replace

```scala
      afterLoop)

  private def endLabel(end: AtlasEnd): String = end match
    case AtlasEnd.Recent => "Recent Sites"
    case AtlasEnd.Forgotten => "Forgotten Sites"
```

with

```scala
      afterLoop)
```

**In** `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
    val suits = map.sites.values.flatMap(_.denizens).flatMap {
      case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
      case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
    }.toVector
```

with

```scala
    val suits = map.sites.values.flatMap(_.denizens)
      .flatMap(AtlasPlacement.suitOf(catalog, _)).toVector
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.AtlasPlacementSuite distantlands.gameplay.ExploreProcedureSuite distantlands.application.ExploreGameSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.LaterGameSetupSuite"`
Expected: PASS. Explore's questions read as before, and the banks at game
start are unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala
git commit -m "feat(setup): share the Atlas's end words and Foundation II's favor per placed site" -m "AtlasPlacement now holds the Recent and Forgotten answers and their words, which Explore used privately, the suit Foundation II reads for a card in a site's denizen slots, and the favor moves from the shared bank that a site placed at Setup adds to its suits' banks. The materializer counts the banks through the same suit." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Imperial Maps fills the map in Setup's walk

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Modify: `src/main/scala/distantlands/model/Setup.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/ImperialMapsSetupSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala`
- Test: `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`
- Rewritten: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/gamelog/later-game-setup.actor.log`, `src/test/resources/gamelog/later-game-setup.other.log`

**Interfaces:**
- Consumes: Task 1's `AtlasPlacement.endOption`, `endOf`, `endLabel`,
  `suitOf`, `suitFavor`; slice 3b's `AtlasPlacement.at`; `Imperials.chancellor`;
  `campaign.baseline` (the Chronicle game start read); `MapState.slots`,
  `MapState.nextSlot`, `MapState.hintermost`; `AtlasState.sites`;
  `Situation.parkedAt`, `Situation.parkedAfter`, `ParkedNode.of`.
- Produces, in `object ImperialMaps` (`gameplay/setup`):
  - `val decisionId: String = "setup.imperial-maps"`, `val heading: String
    = "Imperial Maps: fill the map from"`;
  - `val filledFromRecent`, `val filledFromForgotten`, `val placedSite:
    NoteKey`, `def noteKeys: Vector[NoteKey]`, `def filledFrom(end:
    AtlasEnd): NoteKey`;
  - `def applies(ready: ReadyGame): Boolean`;
  - `def chosenEnd(answered: Vector[Answered]): AtlasEnd`;
  - `def step(catalog: ExecutableCatalog, ready: ReadyGame, chancellor:
    PlayerId): Operation`.
  `SetupProcedure.noteKeys` includes `ImperialMaps.noteKeys`. Task 3's
  tests use `decisionId` and the keys.

- [ ] **Step 1: Write the failing tests**

**Create** `src/test/scala/distantlands/gameplay/setup/ImperialMapsSetupSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.application.GameCommand
import distantlands.gameplay.cards.GreatSlumSite
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Imperial Maps, the normal Foundation I, at a later game's Setup
  * (Foundations design, slice 4): the Chancellor fills the slots past the
  * Empire's sites from the Recent or the Forgotten end of the Atlas. l1 is
  * the seated Chancellor, l2 a Citizen and l3 an Exile; p1 goes first. The
  * Empire holds Ancient City and Broken Peaks, which fill the Cradle, and
  * the Atlas holds Buried Giant at its Recent end, then Deep Woods,
  * Desolate Shore, Dunes, Fair Isle, Golden Valley, and Great Slum at its
  * Forgotten end: one site more than the six empty slots. */
class ImperialMapsSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))

  /** The Chronicle described above, Great Slum storing nothing. */
  private val chronicleWithGreatSlumAtForgottenEnd: Chronicle =
    val record = LaterGameFixture.chronicle(l1, l2)
    record.copy(atlasBox = record.atlasBox :+
      StoredSite(GreatSlumSite.id, Vector.empty))

  private def orders(chronicle: Chronicle, first: PlayerId = p1)
      : SetupOrders = LaterGameFixture.orders(chronicle, first)

  /** Answers Imperial Maps with `end`, and every other park as the
    * situation does by default. */
  private def answering(end: AtlasEnd): Answers =
    case park if park.decisionId == ImperialMaps.decisionId =>
      ChooseOneAnswer(AtlasPlacement.endOption(end))

  /** `chronicle`'s Setup run until it parks at `decisionId`, Imperial Maps
    * answered with `end`. */
  private def setupParkedAt(decisionId: String, end: AtlasEnd,
      chronicle: Chronicle = chronicleWithGreatSlumAtForgottenEnd,
      first: PlayerId = p1): Situation =
    Situation.start(Situation.rules(catalog).withAnswers(answering(end)))
      .parkedAt(decisionId)(GameCommand.Begin(chronicle,
        orders(chronicle, first)))

  /** The game once the map is filled from `end`: parked at the
    * Chancellor's pawn question. */
  private def filledFrom(end: AtlasEnd,
      chronicle: Chronicle = chronicleWithGreatSlumAtForgottenEnd)
      : ReadyGame =
    setupParkedAt(SetupProcedure.pawnDecisionId(p1), end, chronicle).ready

  /** The decision `situation` is parked on. */
  private def parkedDecide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  /** The sites a pawn question offers. */
  private def siteOptions(decide: Decide): Vector[DecisionOptionRef] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref)
      case other => fail(s"expected a choose-one, got $other")

  /** The suit of every denizen on `ready`'s map, edifices included. */
  private def suitsOnMap(ready: ReadyGame): Vector[Suit] =
    ready.game.current.map.sites.values.flatMap(_.denizens)
      .flatMap(AtlasPlacement.suitOf(catalog, _)).toVector

  private val atlasSites: Vector[SiteId] =
    chronicleWithGreatSlumAtForgottenEnd.atlasBox.map(_.site)
  private val empireSites: Vector[SiteId] =
    chronicleWithGreatSlumAtForgottenEnd.world.map(_.site)

  test("game start leaves the slots past the Empire's sites empty, and " +
      "the Chancellor's first question after the Reliquary pick is " +
      "Recent or Forgotten"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val relic = record.relicDeck.head
    val withReliquary = record.copy(relicDeck = record.relicDeck.tail,
      reliquary = Vector(relic))
    val started = accepted(GameStartRules.evolve(catalog, withReliquary,
      orders(withReliquary)))
    assertEquals(started.game.current.map.hinterward, empireSites)
    assertEquals(started.game.campaign.atlas.sites.map(_.id), atlasSites)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(withReliquary, orders(withReliquary)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.reliquaryDecisionId)
    val asked = parkedDecide(begun.parkedAt(ImperialMaps.decisionId)())
    assertEquals(asked.owner, p1)
    assertEquals(asked.query, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(DecisionOptionRef.Button("recent"),
        "Recent Sites"),
      DecisionOption.Button(DecisionOptionRef.Button("forgotten"),
        "Forgotten Sites")),
      heading = Some("Imperial Maps: fill the map from")))

  test("Recent Sites fill every empty slot going Hinterward from the " +
      "Atlas's head, and the rest stays in the Atlas"):
    val ready = filledFrom(AtlasEnd.Recent)
    val map = ready.game.current.map
    assertEquals(map.hinterward, empireSites ++ atlasSites.take(6))
    assertEquals(map.nextSlot, None)
    assertEquals(ready.game.campaign.atlas.sites.map(_.id),
      Vector(GreatSlumSite.id))

  test("Forgotten Sites take the Atlas's tail first"):
    val ready = filledFrom(AtlasEnd.Forgotten)
    assertEquals(ready.game.current.map.hinterward,
      empireSites ++ atlasSites.reverse.take(6))
    assertEquals(ready.game.campaign.atlas.sites.map(_.id),
      Vector(atlasSites.head))

  test("a placed site keeps its stored cards, its edifice ruined and its " +
      "denizens faceup, and takes bandits up to its capacity (R27)"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val withEdifice = record.atlasBox.find(_.items.exists(
      _.isInstanceOf[EdificeId])).get
    val edifice = withEdifice.items.collectFirst { case id: EdificeId => id }
      .get
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val stocked = withEdifice.copy(items = Vector(edifice, denizen, relic))
    val chronicle = record.copy(relicDeck = record.relicDeck.tail,
      worldDeck = record.worldDeck.tail, atlasBox = record.atlasBox.map(
        stored => if stored.site == stocked.site then stocked else stored))
    AtlasEnd.all.foreach { end =>
      val site = filledFrom(end, chronicle).game.current.map
        .sites(stocked.site)
      val capacity = catalog.site(stocked.site).get.capacity
      assertEquals(site.forces,
        SiteForces.Occupied(ForceKind.Bandit, capacity), end)
      assertEquals(site.denizens, Vector(
        EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
        DenizenState(denizen, Orientation.FaceUp, Tokens.empty)), end)
      assertEquals(site.relics.head,
        RelicState(relic, Orientation.FaceDown, Tokens.empty), end)
    }

  test("each placed site's empty relic slots are dealt facedown from the " +
      "top of the relic deck, in the order the sites are placed"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    AtlasEnd.all.foreach { end =>
      val ready = filledFrom(end)
      val placed = ready.game.current.map.hinterward.drop(2)
      val counts = placed.map(catalog.site(_).get.relicSlots)
      val dealt = counts.scanLeft(0)(_ + _).zip(counts).map((from, count) =>
        record.relicDeck.slice(from, from + count))
      placed.zip(dealt).foreach((site, relics) => assertEquals(
        ready.game.current.map.sites(site).relics, relics.map(
          RelicState(_, Orientation.FaceDown, Tokens.empty)), (end, site)))
      assertEquals(ready.game.current.commonCards.relicDeck,
        record.relicDeck.drop(counts.sum), end)
    }

  test("a relic deck too short for the placed sites leaves the last relic " +
      "slots open, and game start does not refuse it"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val oneRelic = record.copy(relicDeck = record.relicDeck.take(1))
    val ready = filledFrom(AtlasEnd.Recent, oneRelic)
    val placed = ready.game.current.map.hinterward.drop(2)
    val first = placed.find(catalog.site(_).get.relicSlots > 0).get
    assertEquals(placed.flatMap(ready.game.current.map.sites(_).relics),
      Vector(RelicState(oneRelic.relicDeck.head, Orientation.FaceDown,
        Tokens.empty)))
    assertEquals(ready.game.current.map.sites(first).relics.map(_.id),
      oneRelic.relicDeck)
    assertEquals(ready.game.current.commonCards.relicDeck, Vector.empty)

  test("each suit's bank ends with 3 favor and 1 per denizen of its suit " +
      "on the map, the placed sites' included, from either end"):
    AtlasEnd.all.foreach { end =>
      val ready = filledFrom(end)
      val onMap = suitsOnMap(ready)
      assert(onMap.size > 2, "the placed sites' edifices are on the map")
      assertEquals(ready.banks.favor, Suit.all.map(suit =>
        suit -> (3 + onMap.count(_ == suit))).toMap, end)
    }

  test("under Movers and Shakers each bank holds only the 1 favor per " +
      "denizen of its suit on the map"):
    val movers = chronicleWithGreatSlumAtForgottenEnd.copy(foundations = Map(
      FoundationNumber.II -> FoundationState(FoundationFace.Altered,
        Set.empty)))
    val ready = filledFrom(AtlasEnd.Forgotten, movers)
    val onMap = suitsOnMap(ready)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)

  test("the filled game holds every card once"):
    AtlasEnd.all.foreach(end =>
      assert(filledFrom(end).game.cardIndex.isRight, end))

  test("an Exile may place their pawn at the Hintermost Empire site or at " +
      "any placed site the bandits rule"):
    AtlasEnd.all.foreach { end =>
      val parked = setupParkedAt(SetupProcedure.pawnDecisionId(p3), end)
      val map = parked.ready.game.current.map
      val placed = map.hinterward.drop(2).filter(site =>
        catalog.site(site).get.capacity > 0)
      assertEquals(siteOptions(parkedDecide(parked)),
        (empireSites.last +: placed).map(DecisionOptionRef.Site(_)), end)
    }

  test("the log says which end filled the map, then names each site"):
    val parked = setupParkedAt(SetupProcedure.pawnDecisionId(p1),
      AtlasEnd.Forgotten)
    val lines = parked.events.collect {
      case LogNoted(_, note, _) if ImperialMaps.noteKeys.exists(
        _.name == note.key) => note.key -> note.args
    }
    assertEquals(lines, ("filled-from-forgotten",
        Vector[NoteArg](NoteArg.Player(p1))) +:
      atlasSites.reverse.take(6).map(site => ("placed-site",
        Vector[NoteArg](NoteArg.Player(p1), NoteArg.Site(site)))))

  test("a Citizen standing in for an absent Chancellor answers Imperial " +
      "Maps"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), l2)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record, p2)))
    assertEquals(parkedDecide(begun).decisionId, ImperialMaps.decisionId)
    assertEquals(parkedDecide(begun).owner, p2)

  test("with one site in the Atlas nothing is asked: it fills the last " +
      "slot"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2), 7)
    assertEquals(record.atlasBox.size, 1)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.pawnDecisionId(p1))
    assertEquals(begun.ready.game.current.map.hinterward,
      (record.world ++ record.atlasBox).map(_.site))
    assertEquals(begun.events.collect {
      case LogNoted(_, note, _) if ImperialMaps.noteKeys.exists(
        _.name == note.key) => note.key
    }, Vector("filled-from-recent", "placed-site"))

  test("an Empire of eight sites leaves no slot: nothing is asked or " +
      "placed"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2), 8)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.pawnDecisionId(p1))
    assert(!begun.events.exists {
      case LogNoted(_, note, _) => ImperialMaps.noteKeys.exists(
        _.name == note.key)
      case _ => false
    })

  test("Wide Horizons with an Empire, Setup With No Empire and a first " +
      "game ask no Imperial Maps question and fill no slot"):
    val wideHorizons = chronicleWithGreatSlumAtForgottenEnd.copy(
      foundations = Map(FoundationNumber.I -> FoundationState(
        FoundationFace.Altered, Set.empty)))
    val noEmpire = chronicleWithGreatSlumAtForgottenEnd.copy(
      lineages = Vector.empty)
    val firstGame = FirstGameSetupFixture.chronicle
    Vector(
      (wideHorizons, orders(wideHorizons), p1, empireSites),
      (noEmpire, orders(noEmpire), p1, empireSites),
      (firstGame, FirstGameSetupFixture.orders,
        FirstGameSetupFixture.firstPlayer, firstGame.atlasBox.map(_.site)))
      .foreach {
      (chronicle, setupOrders, first, sites) =>
        val begun = Situation.start(Situation.rules(catalog))
          .parkedAfter(GameCommand.Begin(chronicle, setupOrders))
        assertEquals(parkedDecide(begun).decisionId,
          SetupProcedure.pawnDecisionId(first), chronicle.name)
        assertEquals(begun.ready.game.current.map.hinterward, sites,
          chronicle.name)
    }

  test("game start still refuses an Atlas too short to fill the empty " +
      "slots"):
    val record = LaterGameFixture.chronicle(l1, l2)
    val short = record.copy(atlasBox = record.atlasBox.take(5))
    assertEquals(GameStartRules.evolve(catalog, short, orders(short)),
      Left(OathViolation.UnsupportedChronicle(
        "at least 6 atlas sites are required, got 5")))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.ImperialMapsSetupSuite"`
Expected: FAIL. The suite does not compile: `ImperialMaps` does not exist.

- [ ] **Step 3: Implement the step**

**Create** `src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Foundation I on its normal face, Imperial Maps, as a step of Setup's
  * walk (Foundations design, slice 4; setup card `OATH-SETUP04a-NF`): "The
  * Chancellor fills the remaining empty site slots going Hinterward. They
  * must fill these slots with Recent Sites or Forgotten Sites from the
  * Atlas, but not both."
  *
  * {{{
  * Sequence(
  *   Decide(setup.imperial-maps, chancellor),  // with 2 or more Atlas sites
  *   Note(filled the map from ... Sites),
  *   Repeat(guard = a slot is empty and the Atlas holds a site,
  *     Sequence(
  *       BuildOps(PlaceAtlasSite, 1 favor per denizen to its suit's bank),
  *       Note(placed the site))))
  * }}}
  *
  * The step's shape reads only what game start fixed, the Chronicle's
  * Empire and Atlas, so a walk resumed past it finds every node where it
  * was once the map is full and the Atlas shorter.
  */
object ImperialMaps:
  val decisionId: String = "setup.imperial-maps"
  val heading: String = "Imperial Maps: fill the map from"

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p1 filled the map from Recent Sites". */
  val filledFromRecent: NoteKey = notes.key("filled-from-recent", Vector(
    NotePart.Arg(0), NotePart.Text(" filled the map from Recent Sites")))
  /** "p1 filled the map from Forgotten Sites". */
  val filledFromForgotten: NoteKey = notes.key("filled-from-forgotten",
    Vector(NotePart.Arg(0),
      NotePart.Text(" filled the map from Forgotten Sites")))
  /** "p1 placed Deep Woods on the map", once per site the fill places. */
  val placedSite: NoteKey = notes.key("placed-site", Vector(
    NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
    NotePart.Text(" on the map")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The line that names the end the map was filled from. */
  def filledFrom(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => filledFromRecent
    case AtlasEnd.Forgotten => filledFromForgotten

  /** The slots past the Empire's sites, which the Chancellor fills: those
    * the Chronicle's Empire leaves empty. */
  private def emptySlots(ready: ReadyGame): Int =
    math.max(0, MapState.slots - ready.game.campaign.baseline.world.size)

  /** Whether Imperial Maps' step runs: a Chancellor is seated, Foundation I
    * lies on its normal face, and a slot is left past the Empire's sites.
    * Under Wide Horizons and with no Empire nothing is filled at Setup,
    * and a first game has placed its eight sites. */
  def applies(ready: ReadyGame): Boolean =
    Imperials.chancellor(ready).nonEmpty && emptySlots(ready) > 0 &&
      ready.game.campaign.foundations.get(FoundationNumber.I)
        .forall(_.face == FoundationFace.Normal)

  /** The end the Chancellor chose, or Recent when the Atlas held one site
    * and nothing was asked. */
  def chosenEnd(answered: Vector[Answered]): AtlasEnd =
    answered.collectFirst {
      case Answered(`decisionId`, ChooseOneAnswer(ref), _) => ref
    }.flatMap(AtlasPlacement.endOf).getOrElse(AtlasEnd.Recent)

  /** The step for `chancellor`. Recent or Forgotten is asked while the
    * Chronicle's Atlas holds two sites or more; either answer fills every
    * empty slot from that end, one site at a time going Hinterward. */
  def step(catalog: ExecutableCatalog, ready: ReadyGame, chancellor: PlayerId)
      : Operation =
    val atlas = ready.game.campaign.baseline.atlasBox
    val question = Option.when(atlas.sizeIs >= 2)(
      Decide(decisionId, chancellor, DecisionQuery.ChooseOne(
        AtlasEnd.all.map(end => DecisionOption.Button(
          AtlasPlacement.endOption(end), AtlasPlacement.endLabel(end))),
        heading = Some(heading))))
    val filledNote = Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      states => Some(filledFrom(chosenEnd(states.answered))
        .sourceless(NoteArg.Player(chancellor))))
    Sequence(question.toVector ++ Vector(filledNote,
      Repeat((state, _) => state.game.current.map.nextSlot.nonEmpty &&
          state.game.campaign.atlas.sites.nonEmpty,
        Sequence(place(catalog), placedNote(chancellor)))))

  /** The next site from the chosen end goes to the next slot, and each
    * suit's bank takes 1 favor from the shared bank per denizen it places:
    * Foundation II resolves after Foundation I and counts every denizen on
    * the map. */
  private def place(catalog: ExecutableCatalog): BuildOps =
    BuildOps((state, pending) =>
      AtlasPlacement.at(catalog, state, chosenEnd(pending.answered))
        .map(planned => planned.operation +:
          AtlasPlacement.suitFavor(catalog, planned)),
      required = true)

  /** The site just placed: the new Hintermost site, since it took the first
    * empty slot going Hinterward. */
  private def placedNote(chancellor: PlayerId): Note =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), states =>
      states.now.game.current.map.hintermost.map(site =>
        placedSite.sourceless(NoteArg.Player(chancellor), NoteArg.Site(site))))
```

**In** `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`:

Replace

```scala
 *     Note(took a relic))
 *   Sequence(                                            // per participant
 *     Decide(pawn-placement, participant),               // legal sites only
```

with

```scala
 *     Note(took a relic))
 *   ImperialMaps.step(chancellor)                        // with a Chancellor and
 *                                                        // Foundation I normal
 *   Sequence(                                            // per participant
 *     Branch(Decide(pawn-placement, participant)),       // legal sites only
```

Then replace

```scala
  def noteKeys: Vector[NoteKey] = notes.declared
```

with

```scala
  def noteKeys: Vector[NoteKey] = notes.declared ++ ImperialMaps.noteKeys
```

Then replace

```scala
    Sequence(Imperials.chancellor(ready).map(reliquaryPick).toVector ++
      steps :+ setupEnd :+ BeginTurn(participants.head.playerId, Phase.Wake))
```

with

```scala
    val chancellor = Imperials.chancellor(ready)
    val imperialMaps = chancellor.filter(_ => ImperialMaps.applies(ready))
      .map(ImperialMaps.step(catalog, ready, _))
    Sequence(chancellor.map(reliquaryPick).toVector ++ imperialMaps ++
      steps :+ setupEnd :+ BeginTurn(participants.head.playerId, Phase.Wake))
```

Then replace

```scala
    val siteOptions = SetupPawnSites.legal(ready, participant.playerId)
      .map(site => DecisionOption.Site(DecisionOptionRef.Site(site)))
    Sequence(Vector(
      Decide(pawnId, participant.playerId,
        DecisionQuery.ChooseOne(siteOptions,
          heading = Some("Choose your starting site"),
          confirmVerb = Some("Start at"))),
```

with

```scala
    Sequence(Vector(
      // The sites are read when the question is reached: Imperial Maps
      // fills the map earlier in the same walk.
      Branch((state, _) => Vector(Decide(pawnId, participant.playerId,
        DecisionQuery.ChooseOne(SetupPawnSites.legal(state,
            participant.playerId).map(site =>
            DecisionOption.Site(DecisionOptionRef.Site(site))),
          heading = Some("Choose your starting site"),
          confirmVerb = Some("Start at"))))),
```

- [ ] **Step 4: Game start stops placing the fill**

**In** `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
      atlasSites = chronicle.atlasBox.take(slots)
```

with

```scala
      // Only a first game's sites are placed here; Imperial Maps fills its
      // slots in Setup's walk.
      atlasSites = if mode == SetupMode.FirstGame then
          chronicle.atlasBox.take(slots)
        else Vector.empty
```

Then replace

```scala
      mode match
        case SetupMode.FirstGame => Vector.empty
        case _: SetupMode.WithEmpire => atlasSites
        case SetupMode.NoEmpire(sites, _) => sites,
```

with

```scala
      mode match
        case SetupMode.NoEmpire(sites, _) => sites
        case _ => Vector.empty,
```

Then replace

```scala
  /** The slots game start fills from the Recent end of the atlas. A first
    * game fills all eight. With an Empire, Imperial Maps fills the slots
    * past the Empire's sites (R27), and Wide Horizons fills none. With no
    * Empire the former Empire's sites are all that is placed. */
```

with

```scala
  /** The slots filled from the atlas. A first game's eight are placed at
    * game start from the Recent end. With an Empire, Imperial Maps fills
    * the slots past the Empire's sites in Setup's walk, from the end the
    * Chancellor chooses (Foundations design, slice 4), and Wide Horizons
    * fills none. With no Empire the former Empire's sites are all that is
    * placed. */
```

The atlas-size check above the first block still reads `slots`, so a
Chronicle whose Atlas cannot fill Imperial Maps' slots is still refused
(Ruling 1). The relic check below it reads `atlasSites`, which is now
empty with an Empire, so only a first game is refused for a short relic
deck (Ruling 3).

**In** `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
  /** A first game deals every relic slot; a later game deals only the
    * slots an atlas site's stored relics leave open, and none at the
    * Empire's sites (Empire ruling R27). Slots are dealt Cradlemost first,
    * and a relic deck that runs out leaves the last ones empty. */
```

with

```scala
  /** A first game deals every relic slot. With no Empire, the former
    * Empire's sites take the slots their stored relics leave open, and an
    * Empire's sites take none (Empire ruling R27); Imperial Maps' sites
    * are dealt theirs as Setup's walk places them (`PlaceAtlasSite`). Slots
    * are dealt Cradlemost first, and a relic deck that runs out leaves the
    * last ones empty. */
```

Then replace

```scala
  /** An atlas site in a later game keeps its stored cards, its edifice
    * ruined, and fills its open relic slots (Empire ruling R27). A former
    * Empire's site is set up the same way with no Empire. */
```

with

```scala
  /** A former Empire's site with no Empire keeps its stored cards, its
    * edifice ruined, and fills its open relic slots, as an atlas site
    * Imperial Maps places does (Empire ruling R27). */
```

Then replace

```scala
    * fifth-player favor: each bank holds 1 per denizen of its suit on the
    * map. */
```

with

```scala
    * fifth-player favor: each bank holds 1 per denizen of its suit on the
    * map. A site Imperial Maps places in Setup's walk adds its own
    * (`AtlasPlacement.suitFavor`). */
```

**In** `src/main/scala/distantlands/model/Setup.scala`:

Replace

```scala
    /** The sites placed as bandit sites with their stored cards, their
      * edifices ruined: the atlas sites that fill the slots the Empire
      * leaves, Recent end first, or, with no Empire, the former Empire's
      * sites. Empty for a first game, whose sites are all in
      * `orderedSites`, and under Wide Horizons with an Empire, which fills
      * no slot. */
```

with

```scala
    /** The sites game start places as bandit sites with their stored
      * cards, their edifices ruined: with no Empire, the former Empire's
      * sites. Empty for a first game, whose sites are all in
      * `orderedSites`, and with an Empire, whose slots Imperial Maps fills
      * in Setup's walk and Wide Horizons leaves empty. */
```

- [ ] **Step 5: Run the new suite to see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.ImperialMapsSetupSuite"`
Expected: PASS, 16 tests.

- [ ] **Step 6: Run the suites that read game start, and see the old reads fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.LaterGameSetupChoicesSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL, exactly these eight (Plan-time finding 8):
`LaterGameSetupSuite` "a remaining site keeps its stored cards, ruins its
edifice and fills only its empty relic slots (R27)" and "each favor bank
adds one per denizen of its suit on the map";
`LaterGameSetupChoicesSuite` "an empty Reliquary skips the pick" and "an
Exile may start at the Hintermost Empire site or a site the bandits rule,
never at an empty site (R25)"; `AlteredFoundationSetupSuite` "Movers and
Shakers gives every board 6 favor, and each bank 1 favor per denizen of its
suit on the map and no more"; `WideHorizonsSetupSuite` "under Wide Horizons
the Empire's sites return as under Imperial Maps and no slot past them is
filled"; `DeclaredNoteKeysSuite`; and `GameLogGoldenSuite`'s
`later-game-setup`. Any other failure: stop and report.

- [ ] **Step 7: Read game start as it now is**

The R27 test moves to `ImperialMapsSetupSuite` ("a placed site keeps its
stored cards ..."), with the helper only it used. The bank tests keep
checking game start, which now counts the Empire's denizens alone. The
choices suite reads pawn sites once the map is filled.

**In** `src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala`:

Replace

```scala
  private def relicSlots(site: SiteId): Int =
    catalog.site(site).get.relicSlots

  private def faceDown(relic: RelicId) =
```

with

```scala
  private def faceDown(relic: RelicId) =
```

Then replace

```scala
  test("a remaining site keeps its stored cards, ruins its edifice and " +
      "fills only its empty relic slots (R27)"):
    val atlasSiteWithEdifice = record.atlasBox.find(stored =>
      stored.items.exists(_.isInstanceOf[EdificeId])).get
    val edifice = atlasSiteWithEdifice.items.collectFirst {
      case id: EdificeId => id }.get
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val stocked = atlasSiteWithEdifice.copy(
      items = Vector(edifice, denizen, relic))
    val chronicle = withoutDeckCards(relic, Some(denizen)).copy(
      atlasBox = record.atlasBox.map(stored =>
        if stored.site == stocked.site then stocked else stored))
    val site = started(chronicle).game.current.map.sites(stocked.site)
    val capacity = catalog.site(stocked.site).get.capacity
    assertEquals(site.forces, SiteForces.Occupied(ForceKind.Bandit, capacity))
    assertEquals(site.denizens, Vector(
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))
    assertEquals(site.relics.size, math.max(1, relicSlots(stocked.site)))
    assertEquals(site.relics.head, faceDown(relic))

  test("the edifice deck holds no edifice stored at any site"):
```

with

```scala
  test("the edifice deck holds no edifice stored at any site"):
```

Then replace

```scala
  test("each favor bank adds one per denizen of its suit on the map"):
```

with

```scala
  test("each favor bank adds one per denizen of its suit on the map, the " +
      "Empire's at game start"):
```

Then replace

```scala
    assert(onMap.size > 2, "the Empire's denizens are on the map")
    Suit.all.foreach
```

with

```scala
    // Imperial Maps fills the other slots in Setup's walk, and their
    // denizens add their own favor (ImperialMapsSetupSuite).
    assertEquals(ready.game.current.map.hinterward, record.world.map(_.site))
    assert(onMap.nonEmpty, "the Empire's denizens are on the map")
    Suit.all.foreach
```

**In** `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`:

Replace

```scala
    val onMap = suitsOnMap(ready)
    assert(onMap.size > 2, "the Empire's denizens are on the map")
```

with

```scala
    // Game start places the Empire's sites; the sites Imperial Maps adds
    // in Setup's walk add their own (ImperialMapsSetupSuite).
    val onMap = suitsOnMap(ready)
    assert(onMap.nonEmpty, "the Empire's denizens are on the map")
```

**In** `src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala`:

Replace

```scala
  test("under Wide Horizons the Empire's sites return as under Imperial " +
      "Maps and no slot past them is filled"):
```

with

```scala
  test("under Wide Horizons the Empire's sites return as under Imperial " +
      "Maps, whose game start also leaves the slots past them empty"):
```

Then replace

```scala
    assertEquals(imperialMaps.current.map.inPlay.size, 8)
```

with

```scala
    // Imperial Maps fills them in Setup's walk (ImperialMapsSetupSuite).
    assertEquals(imperialMaps.current.map, map)
```

**In** `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture, SetupPawnSites, SetupProcedure}
```

with

```scala
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  ImperialMaps, LaterGameFixture, SetupPawnSites, SetupProcedure}
```

Then replace

```scala
  private def parkedOn(situation: Situation): ParkedNode =
```

with

```scala
  /** `chronicle`'s Setup once Imperial Maps has filled the map from the
    * Recent end: parked at the Chancellor's pawn question. */
  private def filledFromRecent(chronicle: Chronicle): ReadyGame =
    Situation.start(Situation.rules(catalog))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        chronicle, LaterGameFixture.orders(chronicle, p1))).ready

  private def parkedOn(situation: Situation): ParkedNode =
```

Then replace

```scala
  test("an empty Reliquary skips the pick"):
    assertEquals(parkedOn(begin(record)).decisionId,
      SetupProcedure.pawnDecisionId(p1))

  test("Imperial players may start only at Empire sites"):
    val ready = started(record)
```

with

```scala
  test("an empty Reliquary skips the pick: Imperial Maps asks first"):
    assertEquals(parkedOn(begin(record)).decisionId,
      ImperialMaps.decisionId)

  test("Imperial players may start only at Empire sites"):
    val ready = filledFromRecent(record)
```

Then replace

```scala
    // The first atlas site is emptied, as Sunken Isles' capacity 0 leaves
    // it.
    val ready = started(record)
```

with

```scala
    // The first site Imperial Maps placed is emptied, as Sunken Isles'
    // capacity 0 leaves it.
    val ready = filledFromRecent(record)
```

`started` stays: the Reliquary-identification test still reads game
start.

- [ ] **Step 8: Rewrite the note keys and the golden, and read the diff**

Run:
`NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
then
`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
then `git status --short src/test/resources` and
`git diff -- src/test/resources`.

Expected status: exactly
` M src/test/resources/gamelog/later-game-setup.actor.log`,
` M src/test/resources/gamelog/later-game-setup.other.log` and
` M src/test/resources/notes/declared-keys.txt`.

Expected diff of `declared-keys.txt`: three added lines, in place:

```diff
 procedure.self-exile stayed = Gave {0} to {1} and stayed a Citizen.
+procedure.setup filled-from-forgotten = {0} filled the map from Forgotten Sites
+procedure.setup filled-from-recent = {0} filled the map from Recent Sites
 procedure.setup kept-adviser = {0} kept {1}
 procedure.setup placed-pawn = {0} placed their pawn at {1}
+procedure.setup placed-site = {0} placed {1} on the map
 procedure.setup revealed-card = {0} revealed {1}
```

Expected `later-game-setup.actor.log` in full (the default answer is the
first option, Recent Sites, so the sites and the pawns are where they were;
every sequence after the Reliquary line moves on by 16):

```text
0.0 round 0 | Setup
5.0 action 1 | [player:p1|p1] took a relic from the Reliquary
9.0 action 1 | [player:p1|p1] filled the map from Recent Sites
11.0 action 1 | [player:p1|p1] placed [site:site:buried-giant|Buried Giant] on the map
13.0 action 1 | [player:p1|p1] placed [site:site:deep-woods|Deep Woods] on the map
15.0 action 1 | [player:p1|p1] placed [site:site:desolate-shore|Desolate Shore] on the map
17.0 action 1 | [player:p1|p1] placed [site:site:dunes|Dunes] on the map
19.0 action 1 | [player:p1|p1] placed [site:site:fair-isle|Fair Isle] on the map
21.0 action 1 | [player:p1|p1] placed [site:site:golden-valley|Golden Valley] on the map
26.0 action 1 | [player:p1|p1] placed their pawn at [site:site:ancient-city|Ancient City]
31.0 decision 1 | [player:p1|p1] kept [card:207|Glamor]
39.0 action 1 | [player:p2|p2] placed their pawn at [site:site:ancient-city|Ancient City]
44.0 decision 1 | [player:p2|p2] kept a Denizen
52.0 action 1 | [player:p3|p3] placed their pawn at [site:site:broken-peaks|Broken Peaks]
57.0 decision 1 | [player:p3|p3] kept a Denizen
58.0 round 0 | Round 1
58.1 turn 0 | [player:p1|p1]'s turn
58.2 phase 0 | Wake
60.0 action 1 | Nothing happened in Wake
60.1 phase 0 | Act
```

`later-game-setup.other.log` is the same file but for its two kept lines,
which stay as they were for that seat: `31.0 decision 1 | [player:p1|p1]
kept a Denizen` and `44.0 decision 1 | [player:p2|p2] kept [card:175|Nature
Worship]`.

- [ ] **Step 9: Run the suites to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.LaterGameSetupChoicesSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.application.gamelog.* distantlands.gameplay.chronicle.* distantlands.gameplay.powers.setup.*"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS, every JVM suite (the frontend suites run in Task 4).

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/main/scala/distantlands/model/Setup.scala src/test/scala/distantlands/gameplay/setup/ImperialMapsSetupSuite.scala src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala src/test/resources/notes/declared-keys.txt src/test/resources/gamelog/later-game-setup.actor.log src/test/resources/gamelog/later-game-setup.other.log
git commit -m "feat(setup): the Chancellor fills the map from Recent or Forgotten Sites" -m "Under Imperial Maps game start now places only the Empire's sites. Right after the Reliquary pick, Setup asks the Chancellor which end of the Atlas fills the empty slots, places one site per slot going Hinterward from that end, deals their relic slots and adds Foundation II's favor for their denizens, and logs the end and each site. The pawn question reads its sites when it is asked, so an Exile is offered the placed bandit sites." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The question at the table, replay and the undo barrier

**Files:**
- Test: `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`
- Test: `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala`
- Written: `src/test/resources/gamelog/later-game-forgotten-sites.actor.log`, `src/test/resources/gamelog/later-game-forgotten-sites.other.log`

**Interfaces:**
- Consumes: Task 1's `AtlasPlacement.endOption`; Task 2's
  `ImperialMaps.decisionId`; `WalkerDecisionProjector`,
  `GamePresentationProjector`, `GameProjector`,
  `InMemoryEventStreamRepository`, `GameApplicationService.load`,
  `Commands.resolveWalker`, `TreeDecision`.
- Produces: nothing new; tests and one golden pair.

This task pins what Task 2 built as the table sees it. Its tests pass
when written: run each to see it pass, and if one fails, the fault is in
Task 2's code, not the test.

- [ ] **Step 1: Write the tests**

**In** `src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  ImperialMaps, LaterGameFixture, SetupPawnSites, SetupProcedure}
```

with

```scala
import distantlands.gameplay.setup.{AtlasPlacement, FirstGameSetupFixture,
  GameStartRules, ImperialMaps, LaterGameFixture, SetupPawnSites,
  SetupProcedure}
```

Then replace

```scala
    assertEquals(own.query.toVector.flatMap(_.offeredOptions).size, 2)
    assertEquals(projector.project(ScopedProjectionContext(ready, Some(p2))),
      None)
```

with

```scala
    assertEquals(own.query.toVector.flatMap(_.offeredOptions).size, 2)
    assertEquals(projector.project(ScopedProjectionContext(ready, Some(p2))),
      None)

  test("the parked Imperial Maps question is the Chancellor's, and names " +
      "the two ends and no site"):
    val parked = begin(record).ready
    val projector = new WalkerDecisionProjector(catalog,
      new GamePresentationProjector(catalog))
    val own = present(projector.project(ScopedProjectionContext(parked,
      Some(p1))), "p1's decision")
    assertEquals(own.decisionId, ImperialMaps.decisionId)
    assertEquals(own.query.toVector.flatMap(_.offeredOptions)
      .map(option => option.id -> option.label), Vector(
        "recent" -> "Recent Sites", "forgotten" -> "Forgotten Sites"))
    assertEquals(projector.project(ScopedProjectionContext(parked, Some(p2))),
      None)

  test("a Setup filled from Forgotten Sites replays its journal to the " +
      "same game, and the projected map has no empty slot"):
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val filled = Situation.start(Situation.journaled(service, catalog,
        repository, "forgotten").withAnswers {
        case park if park.decisionId == ImperialMaps.decisionId =>
          DecisionAnswer.ChooseOneAnswer(
            AtlasPlacement.endOption(AtlasEnd.Forgotten))
      }).parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        record, LaterGameFixture.orders(record, p1)))
    val loaded = present(accepted(service.load("forgotten")), "the game")
    assertEquals(loaded.state, filled.state)
    assertEquals(filled.ready.game.current.map.hinterward,
      (record.world ++ record.atlasBox.reverse).map(_.site))
    val world = new GameProjector(catalog).project("forgotten", loaded, p1)
      .world
    assertEquals(world.map(_.sites.size), Vector(2, 3, 3))
    assertEquals(world.map(_.emptySlots), Vector(0, 0, 0))
```

**In** `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
```

with

```scala
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
```

Then replace

```scala
    assertEquals(placed.ready.game.current.map.inPlay.size, 4)
    nothingToUndo(service, "explore", placed.nextSequence, p1)
```

with

```scala
    assertEquals(placed.ready.game.current.map.inPlay.size, 4)
    nothingToUndo(service, "explore", placed.nextSequence, p1)

  test("Imperial Maps' fill shows the placed sites, so the Chancellor's " +
      "choice of end cannot be taken back"):
    // A later game's Setup: p1, the Chancellor, is asked Recent or
    // Forgotten first, and the Reliquary is empty.
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val asked = Situation.start(Situation.journaled(service, catalog,
        repository, "imperial-maps"))
      .parkedAfter(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    val filled = asked.parkedAfter(Commands.resolveWalker(p1, TreeDecision(
      ImperialMaps.decisionId, DecisionAnswer.ChooseOneAnswer(
        AtlasPlacement.endOption(AtlasEnd.Forgotten)))))
    assertEquals(filled.ready.game.current.map.inPlay.size, 8)
    nothingToUndo(service, "imperial-maps", filled.nextSequence, p1)
```

The barrier test is not empty: in this same Setup an undo after the
Reliquary pick, before the fill, is accepted (checked at plan time).

**In** `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala`:

Replace

```scala
import distantlands.gameplay.setup.LaterGameFixture
import distantlands.model._
import distantlands.testkit.{Dice, Look, Situation}
```

with

```scala
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps,
  LaterGameFixture}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Dice, Look, Situation}
```

Then replace

```scala
  /** A later game's Setup on stream `name`, from the fixture's Chronicle as
    * `changed` leaves it. */
  private def laterGameWokenFrom(name: String,
      changed: Chronicle => Chronicle)(using munit.Location): Script =
```

with

```scala
  /** As [[laterGameWoken]], with the Chancellor filling the map from
    * Forgotten Sites: Golden Valley, the Atlas's last site, takes the top
    * slot of the Provinces. */
  def laterGameFilledFromForgotten(using munit.Location): Script =
    laterGameWokenFrom("later-game-forgotten-sites", identity, {
      case park if park.decisionId == ImperialMaps.decisionId =>
        ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))
    })

  /** A later game's Setup on stream `name`, from the fixture's Chronicle as
    * `changed` leaves it, each park answered by `answers` where it names
    * one and as the situation does by default otherwise. */
  private def laterGameWokenFrom(name: String,
      changed: Chronicle => Chronicle,
      answers: Answers = PartialFunction.empty)(using munit.Location)
      : Script =
```

Then replace

```scala
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, name), chronicle, LaterGameFixture.orders(chronicle, p1))
```

with

```scala
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, name).withAnswers(answers), chronicle,
      LaterGameFixture.orders(chronicle, p1))
```

Then replace

```scala
    "later-game-setup" -> (() => laterGameWoken))
```

with

```scala
    "later-game-setup" -> (() => laterGameWoken),
    "later-game-forgotten-sites" -> (() => laterGameFilledFromForgotten))
```

- [ ] **Step 2: Run the tests, then write the golden and read it**

Run: `./sbtw "testOnly distantlands.application.LaterGameSetupChoicesSuite distantlands.application.UndoBarrierPlaySuite"`
Expected: PASS.

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL, only for `later-game-forgotten-sites`, which has no golden
yet.

Run:
`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
then `git status --short src/test/resources`.
Expected status: exactly
`?? src/test/resources/gamelog/later-game-forgotten-sites.actor.log` and
`?? src/test/resources/gamelog/later-game-forgotten-sites.other.log`.

Expected `later-game-forgotten-sites.actor.log` in full: Golden Valley,
the Atlas's last site, is placed first:

```text
0.0 round 0 | Setup
5.0 action 1 | [player:p1|p1] took a relic from the Reliquary
9.0 action 1 | [player:p1|p1] filled the map from Forgotten Sites
11.0 action 1 | [player:p1|p1] placed [site:site:golden-valley|Golden Valley] on the map
13.0 action 1 | [player:p1|p1] placed [site:site:fair-isle|Fair Isle] on the map
15.0 action 1 | [player:p1|p1] placed [site:site:dunes|Dunes] on the map
17.0 action 1 | [player:p1|p1] placed [site:site:desolate-shore|Desolate Shore] on the map
19.0 action 1 | [player:p1|p1] placed [site:site:deep-woods|Deep Woods] on the map
21.0 action 1 | [player:p1|p1] placed [site:site:buried-giant|Buried Giant] on the map
26.0 action 1 | [player:p1|p1] placed their pawn at [site:site:ancient-city|Ancient City]
31.0 decision 1 | [player:p1|p1] kept [card:207|Glamor]
39.0 action 1 | [player:p2|p2] placed their pawn at [site:site:ancient-city|Ancient City]
44.0 decision 1 | [player:p2|p2] kept a Denizen
52.0 action 1 | [player:p3|p3] placed their pawn at [site:site:broken-peaks|Broken Peaks]
57.0 decision 1 | [player:p3|p3] kept a Denizen
58.0 round 0 | Round 1
58.1 turn 0 | [player:p1|p1]'s turn
58.2 phase 0 | Wake
60.0 action 1 | Nothing happened in Wake
60.1 phase 0 | Act
```

`later-game-forgotten-sites.other.log` is the same but for its two kept
lines: `31.0 decision 1 | [player:p1|p1] kept a Denizen` and `44.0
decision 1 | [player:p2|p2] kept [card:175|Nature Worship]`.

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS, the property suites included ("every decision a script
asks is told by some line": the end's line tells the Imperial Maps
answer).

- [ ] **Step 3: Commit**

```bash
git add src/test/scala/distantlands/application/LaterGameSetupChoicesSuite.scala src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala src/test/resources/gamelog/later-game-forgotten-sites.actor.log src/test/resources/gamelog/later-game-forgotten-sites.other.log
git commit -m "test(setup): pin Imperial Maps' question, replay and undo at the table" -m "The projected question is the Chancellor's alone and names the two ends and no site, a Setup filled from Forgotten Sites replays its journal to the same game with no empty slot, the fill is an undo barrier, and a golden log records a Forgotten fill." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Docs and verification

**Files:**
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

**Interfaces:**
- Consumes: everything Tasks 1 to 3 built; nothing new is produced.

Docs are plain prose. Two of these files hold very long lines (a table row,
a numbered item), so their blocks below are parts of a line, not whole
lines: replace exactly the text shown and leave the rest of the line. The
roadmap deletes finished work outright: no checked box and no "landed"
note.

- [ ] **Step 1: The rulings, the traceability table, the setup doc and the roadmap**

**In** `docs/rules/ambiguities.md` (R27 is closed):

Replace

~~~markdown
are filled from the relic deck. Sites always come from the Recent end; the Chancellor's choice of Recent or Forgotten sites waits for simultaneous setup effects (Empire slice 4).
~~~

with

~~~markdown
are filled from the relic deck. Under Imperial Maps the Chancellor chooses at Setup, right after the Reliquary pick, whether every empty slot is filled from the Recent or the Forgotten end; the sites are placed one at a time going Hinterward, each adding Foundation II's favor for its denizens (Empire slice 4; [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md), slice 4).
~~~

**In** `docs/rules/implementation-traceability.md`:

Replace

~~~markdown
then the atlas sites with their stored cards, filling only open relic slots; Foundation II counts every denizen on the map.
~~~

with

~~~markdown
then, under Imperial Maps, Setup's walk asks the Chancellor for Recent or Forgotten Sites and fills every empty slot from that end with its stored cards, filling only open relic slots (`ImperialMaps`, `PlaceAtlasSite`); Foundation II counts every denizen on the map, a placed site's as it is placed (`ImperialMapsSetupSuite`).
~~~

Then replace

~~~markdown
legacies, starting relics, and the Chancellor's Recent-or-Forgotten choice. |
~~~

with

~~~markdown
legacies and starting relics. |
~~~

Then replace

~~~markdown
 Imperial Maps always fills from Recent Sites ([R27](ambiguities.md#empire-rulings)).
~~~

with

~~~markdown
 Imperial Maps fills from the end the Chancellor chooses ([R27](ambiguities.md#empire-rulings)).
~~~

Then replace

~~~markdown
Imperial Maps' choice, which fills the slots at Setup, waits for slice 4 of the [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md). |
~~~

with

~~~markdown
Imperial Maps' choice fills every empty slot at Setup from one end (`ImperialMaps`). |
~~~

**In** `docs/architecture/game-setup.md`:

Replace

~~~markdown
walker's per-player tree -- `Decide` a site, place the pawn, `Decide` an
~~~

with

~~~markdown
walker's tree -- in a later game the Chancellor's Reliquary pick and
Imperial Maps' fill (`gameplay/setup/ImperialMaps.scala`), then per player
`Decide` a site, place the pawn, `Decide` an
~~~

Then replace

~~~markdown
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled.
~~~

with

~~~markdown
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled. The sites Imperial Maps places in
Setup's walk add their denizens' favor as they are placed
(`AtlasPlacement.suitFavor`).
~~~

Then replace

~~~markdown
Chronicle: known and unique atlas/denizen/relic ids, an atlas site for every
slot it fills from the atlas (8 in a first game),
enough World Deck and relic cards to deal, a deal holding exactly the
~~~

with

~~~markdown
Chronicle: known and unique atlas/denizen/relic ids, an atlas site for every
slot the atlas fills (8 in a first game; under Imperial Maps, every slot
past the Empire's sites), enough World Deck cards to deal and, in a first
game, enough relics for every relic slot, a deal holding exactly the
~~~

Then replace

~~~markdown
Citizen standing in for one, the Empire's sites return; Imperial Maps then
fills the slots past them from the atlas's Recent end, and Wide Horizons,
the altered Foundation I, fills none.
~~~

with

~~~markdown
Citizen standing in for one, the Empire's sites return. Under Imperial Maps
Setup's walk then asks the Chancellor, right after the Reliquary pick,
whether the Recent or the Forgotten end fills the slots past them, and
places one site per slot going Hinterward from that end with
`PlaceAtlasSite`, populated as a bandit site (`SiteState.banditSite`) and
dealt its relic slots while the relic deck lasts; the placement is an undo
barrier. Wide Horizons, the
altered Foundation I, fills none.
~~~

**In** `docs/ROADMAP.md`:

Replace

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, and the Beacon's Shape Foundation.
~~~

with

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: the Beacon's
  Shape Foundation.
~~~

Then replace

~~~markdown
  add: player choices earlier in setup once foundations and legacies exist,
  such as the Chancellor choosing Recent or Forgotten sites.
~~~

with

~~~markdown
  add: player choices earlier in setup once legacies exist.
~~~

- [ ] **Step 2: The spec**

The status line, the Approach's materializer sentence, which named
Foundation I's fill, and the slice 4 section: what the plan found against
the code, and the readings it took, for the owner to confirm.

**In** `docs/superpowers/specs/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a and 3b landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b and 4 landed.
~~~

Then replace

~~~markdown
then erosion). The materializer branches per face for II, IV, V and VI and
for Foundation I's fill. The setup walker gains only the one choice a face
needs, Imperial Maps'.
~~~

(The block ends inside its paragraph's third line: leave the rest of that
line, " In play, Explore is offered while Foundation I is", as it is.)

with

~~~markdown
then erosion). The materializer branches per face for II, IV, V and VI.
The setup walker gains only the one choice a face needs, Imperial Maps',
and the fill that follows it.
~~~

Then replace

~~~markdown
- **Log.** "Filled the map from Forgotten Sites", then a line per site.
- **Tests.** Each end; bank totals; the relic deal; pawn options; replay
  equals live.
~~~

with

~~~markdown
- **Log.** "p1 filled the map from Forgotten Sites", then "p1 placed Golden
  Valley on the map" per site. Setup has no turn and every Setup line names
  its player, so these name the Chancellor.
- **Tests.** Each end; bank totals; the relic deal; pawn options; replay
  equals live.
- **As built.** Game start places only the Empire's sites, so the fill's
  relic deal and its denizens' favor move from the materializer into the
  walk; a Recent fill gives the same sites, relics and bank totals as the
  old game start. The step (`ImperialMaps`) is declared from what game
  start fixed, so a resumed walk finds its nodes; the pawn question reads
  its sites when it is asked. The favor is a `Move` from the shared bank
  beside each `PlaceAtlasSite`, so an Explore still adds none. The fill is
  an undo barrier, as an Explore is, so the Reliquary pick before it can no
  longer be undone once the map is filled.
- **Readings taken with the plan,** for the owner to confirm. An Atlas too
  short to fill every slot is refused at game start, as before. A Citizen
  standing in for an absent Chancellor answers. A relic deck that runs
  short leaves the last relic slots open, so game start no longer refuses
  such a game. The question names the two ends and no site; the Recent
  end's first site is public by the rulebook, but the table shows no Atlas
  yet. Foundation II's favor counts a placed site's ruined edifice, as
  "denizens" covers edifices, and is added as each site is placed, which
  gives the same totals as counting after the fill. One answer fills every
  slot, the Forgotten end placing the Atlas's last site in the Cradlemost
  empty slot. With one site in the Atlas nothing is asked and the line says
  Recent; an Empire of eight sites asks nothing. A placed site's SETUP half
  resolves at Setup's end, as before.
~~~

If the owner has ruled on any reading in "Rulings needing the owner's
review" by the time you run this task, write the ruling in place of the
reading, and say so in your report. If you took a reading of your own
while working, add it to the "Readings taken with the plan" item.

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%. The plan's own run of these tasks at `abc801d62` passed
5841 tests at 95.31% and 89.63%.

Run: `./sbtw "frontend/test"` (the frontend suites, if the full run
skipped them)
Expected: PASS (930 tests in the plan's own run).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. The goldens and the snapshot were committed with Tasks 2
and 3, and nothing else changed.

Run: `git grep -n "Sites always come from the Recent end\|waits for slice 4\|Recent-or-Forgotten choice\|such as the Chancellor choosing Recent" -- docs/rules docs/architecture docs/ROADMAP.md`
Expected: no output. Nothing still says Imperial Maps' choice is missing.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/architecture/game-setup.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 4, Imperial Maps' choice" -m "Empire ruling R27 is closed: the Chancellor chooses the end of the Atlas that fills the map. The setup doc says where the fill now runs, the traceability table stops calling the choice missing, the roadmap drops it, and the spec records slice 4 landed, what its plan found against the code and the readings it took." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## For slice 5 and later

- World Crafter (legacy L13) prints "For the Imperial Maps foundation, you
  fill site slots, using any mix of Recent and Forgotten Sites". When
  Legacies arrive, its holder's question is asked per pass of the fill's
  `Repeat`, and `ImperialMaps.chosenEnd` reads that pass's answer.
- A view of the Atlas's Recent end, which the rulebook makes public, would
  serve both Imperial Maps and Explore.

## Self-review

**Spec coverage** (slice 4):

| Spec statement | Task |
|---|---|
| With a Chancellor and Foundation I normal, game start leaves the slots past the Empire's sites empty | 2 (`GameStartRules`; the first test of `ImperialMapsSetupSuite`) |
| The first setup question after the Reliquary pick is the Chancellor's `setup.imperial-maps`: Recent or Forgotten Sites | 2 (`ImperialMaps.step`, `SetupProcedure.tree`), 3 (the projected question) |
| One `PlaceAtlasSite` per empty slot going Hinterward | 2 (the `Repeat`; the two end tests) |
| Each placement gives 1 favor from the shared bank per denizen to the matching suit's bank | 1 (`suitFavor`), 2 (the bank totals under both faces of Foundation II) |
| The Forgotten end takes the Atlas's tail first | 2 ("Forgotten Sites take the Atlas's tail first"), 3 (the golden) |
| Pawn placement follows and offers the new sites | 2 (`SetupProcedure.playerStep`; the Exile test) |
| Log: the end, then a line per site | 2 (the log test, `later-game-setup`), 3 (`later-game-forgotten-sites`) |
| Tests: each end; bank totals; the relic deal; pawn options; replay equals live | 2, 3 |
| No Chancellor, first games, Wide Horizons with an Empire unchanged | 2 (the unchanged-setups test; `WideHorizonsSetupSuite`, `NoEmpireSetupSuite` unchanged) |
| R27 closed | 4 |

**Left for later, on purpose:** World Crafter's mixed fill (Legacies); a
view of the Atlas; `CONTEXT.md` (slice 7, as the spec's Docs list says).

**Placeholders:** none. Every step that changes code shows the code, and
every file written by a command shows its expected text.

**Names used across tasks:** `AtlasPlacement.endOption`, `endOf`,
`endLabel`, `suitOf`, `suitFavor` (Task 1); `ImperialMaps.decisionId`,
`heading`, `filledFromRecent`, `filledFromForgotten`, `placedSite`,
`noteKeys`, `filledFrom`, `applies`, `chosenEnd`, `step` (Task 2). The
suites' helpers name what they build: `chronicleWithGreatSlumAtForgottenEnd`,
`setupParkedAt`, `filledFrom`, `filledFromRecent`, `parkedDecide`,
`siteOptions`, `suitsOnMap`, `laterGameFilledFromForgotten`. Each is
spelled the same wherever it appears.
