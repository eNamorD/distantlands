# Foundations, Slice 3b: The Explore Action Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Under Wide Horizons, the altered Foundation I, a player whose
pawn is at the Hintermost site can take the Explore action: roll like a
Recover against a difficulty of 4, and on success place a site from the
Recent or the Forgotten end of the Atlas in the next empty slot, with their
pawn on it. The site's WHEN EXPLORED power then resolves, the Game Log tells
it, the table draws the site, and the Chronicle Phase stores it.

**Architecture:** `PlaceAtlasSite`, a journaled operation, takes the site
at an end of the Atlas and puts it in the map's next slot, populated by
`SiteState.banditSite`, the builder game start now shares. An operation is
applied with no catalog, so `AtlasPlacement` copies the site's printed
capacity, relic slots and resources onto it. `ExploreProcedure` is a walker
tree in the shape of Recover's, on its own dice pool and its own windows,
registered like every action: an `ActionRef`, a registry entry, an Act menu
offer and a Game Log entry. After the placement it folds the `WhenExplored`
window on an `ExploredSite` node that names the site, and the setup rules
that print WHEN EXPLORED resolve there only for a face at that site. The
projection and the frontend need no new shape: the menu, the questions and
the map are drawn from what the engine declares.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings" (rulings 2 and 4 bind this slice), "Approach" and, in
"Slice 3: Wide Horizons, Explore and Setup With No Empire", the "Explore"
subsection and the Explore items of "Tests". Slice 4 is read for the
`PlaceAtlasSite` operation it reuses. Slice 3a's plan,
[Foundations slice 3a](2026-11-11-foundations-slice3a.md), built what this
slice stands on.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
  `model/CoreOperations.scala` ends this slice at 780 lines and
  `serialization/WalkerOperationCodec.scala` at 756: add nothing to either
  beyond what the tasks show.
- `gameplay` must not read a catalog `.text` or `rulesText`. `model`
  imports nothing from `gameplay`. `gameplay/actions` imports nothing from
  `gameplay/powers`: Explore reaches its powers only through power
  windows, and the powers import `ExploredSite` from the action
  (`scripts/check-architecture.py`).
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. A test that names a symbol the task has not built
  yet fails by not compiling, and that is the failure to see. Tests go in
  the repo's existing suites where one covers the subject. A test helper's
  name says what it builds.
- Wire and journal changes need not keep old journals decodable. This slice
  journals one new operation, `place-atlas-site`, and every field of it is
  required. `ActionRef.Explore`'s key, `explore`, the decision ids
  `explore.choice` and `explore.end`, the pool key `explore`, and the three
  note keys of `procedure.explore` are wire values: do not rename them.
- Owner rulings (spec, "Rulings"), binding here:
  - Ruling 2. Explore borrows only Recover's dice. No power that names
    Recover or recovering applies to Explore: Silver Charm, Relic Patrols
    and Drowned Library stay out, though Silver Charm's text names only
    spending Supply to roll defense dice. Jinx applies, through the generic
    `Reroll` window.
  - Ruling 4. Explore's line is "Explored Forgotten Site: Found Deep
    Woods" (or "Recent Site").
  - Any role may Explore.
  - Hall of Mockery and Lost Guardians move a held banner to the shared
    bank, as the cards say, when explored and at a later game's Setup
    alike (decided with the plan's review; ruling 8, Task 5).
- New Foundations rules only. The printed setup card is `OATH-SETUP04b-NF`
  (Wide Horizons) in
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (each entry's `text:` comes before its `name:`; read it by id). The
  rulebook is `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`:
  "Altered Foundation I: Wide Horizons" (about line 301), "Foundation I:
  Imperial Maps" (about line 287, how a placed site is populated), Recover
  (about line 836), "Setup / When Explored" (about line 974) and "Starting
  Resources" (about line 1057). Read them there; make no web call.
- If a rule is unclear while you work, do not guess silently and do not
  stop: take the most literal reading of the New Foundations text, add it
  to the spec's Explore readings (Task 8, Step 2), and name it in your
  report.
- An unavailable option is shown disabled with its reason, never hidden.
  Explore is listed in every Act menu, and shown unavailable with its
  reason wherever a gate fails.
- A Game Log line omits the actor on their own turn, and its wording stays
  parallel to the lines beside it: "Failed to explore" beside "Failed to
  recover at ...", "Continued exploring" beside "Continued recovering".
- Note keys change in Tasks 3 and 5 only, and Game Log goldens in Tasks 3
  and 4 only, and only as those tasks show. Their rewrite commands are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
  After a rewrite, read `git diff -- src/test/resources` and
  `git status --short src/test/resources` line by line against the text the
  task shows. If anything else changed, stop and report: do not commit a
  golden you cannot explain.
- Frontend work follows `DESIGN.md`. This slice adds no element, no class
  and no color: Explore's lines sit on the Game Log's Neutral band, which
  "every other action" already takes (`DESIGN.md`, "Log action bands").
- The words are "slot" for a place on the map a site can lie in, "empty
  slot" for one that holds none, "Hintermost" for the last site in
  Hinterward order, "Atlas" for the box of sites not on the map, "Recent"
  and "Forgotten" for its two ends, and "explorer" for the player taking
  the action.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `bbfb403a7` (the merge of slice 3a into `0.2.0-alpha`) with the earlier
  tasks of this plan applied, and occurs once in its file. "Then replace" is the next block in
  the same file. A "with" block repeats the unchanged lines of its
  "Replace" block, so an edit that only adds lines still shows where they
  go. "Create" gives a file's full text. If a block does not match because
  a later fix moved the text, find the same code by its content and make
  the same change; if the code itself is different, stop and report.
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

Work in a git worktree cut from `0.2.0-alpha`, which holds slices 1, 2 and
3a. In the worktree, link the main checkout's tooling before the first
`./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor d8cabe43c HEAD && echo slice2-ok
git merge-base --is-ancestor bbfb403a7 HEAD && echo slice3a-ok
git grep -n "def nextSlot: Option\[Region\]" -- src/main/scala/distantlands/model/World.scala
git grep -n "def hintermost: Option\[SiteId\]" -- src/main/scala/distantlands/model/World.scala
git grep -n "def inSlots(placed: Vector\[(SiteId, SiteState)\]): MapState" -- src/main/scala/distantlands/model/World.scala
git grep -n "final case class AtlasState(entries: Vector\[AtlasEntry\])" -- src/main/scala/distantlands/model/World.scala
git grep -n "private def atlasSite(stored: StoredSite, drawn: Vector\[RelicId\]): SiteState" -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "case object WhenExplored extends OtherWindow" -- src/main/scala/distantlands/model/PowerWindow.scala
git grep -n "def reliquaryInPlay: Vector\[RelicId\]" -- src/main/scala/distantlands/model/GameState.scala
git grep -n "emptySlots" -- shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala
git grep -n "def sitesInPlay(count: Int)" -- src/test/scala/distantlands/testkit/Table.scala
git grep -n "def foundation(number: FoundationNumber, face: FoundationFace)" -- src/test/scala/distantlands/testkit/Table.scala
git grep -n "def chronicle(chancellor: LineageId, citizens: Vector\[LineageId\]," -- src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala
git grep -n "def gameWonFrom(wakes: OathState): OathTransition" -- src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala
git grep -n "LineagePointsScored | EdificeBuilt | SuitedReserveFromBox" -- src/main/scala/distantlands/serialization/WalkerOperationCodec.scala
git grep -n "Slices 1, 2 and 3a landed" -- docs/superpowers/specs/2026-11-11-foundations-design.md
git grep -c "Explore" -- src/main/scala/distantlands/gameplay/phases/ActOffers.scala
```

The first must print `slice2-ok`: `d8cabe43c` is the merge that brought
slice 2 into `0.2.0-alpha`. The second should print `slice3a-ok`:
`bbfb403a7` is the merge of slice 3a this plan was cut from. Each grep but
the last must print one line, except the `emptySlots` one, which prints one
or more. They name what this plan stands on: slice 3a's map slots,
Hintermost site and `inSlots`, the Atlas it leaves in the campaign, the
materializer's atlas-site builder this plan shares, the `WhenExplored`
window, the set-aside Reliquary, the projection's `emptySlots`, the test
table's `sitesInPlay` and `foundation` steps, the later-game and Chronicle
fixtures, the operation codec's Chronicle group, and the spec's status
line. The last must print nothing and exit with status 1: no Explore is
offered yet.

If `slice2-ok` is missing, the worktree was cut from a stale branch: stop
and report. If only `slice3a-ok` is missing while every grep prints, slice
3a was merged again under another commit: go on. If a grep prints nothing
while the others print, a later fix moved or renamed that code: read the
file, and go on only if the code the tasks edit is still there in
substance, adapting the blocks that name it.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

The quiet table of the test kit (`Table.start`) has eight sites, in this
order Hinterward: Ancient City and Broken Peaks in the Cradle; Buried
Giant, Deep Woods and Desolate Shore in the Provinces; Dunes, Fair Isle and
Golden Valley in the Hinterland. Its Atlas is empty. p1, p2 and p3 stand at
Ancient City, Broken Peaks and Buried Giant, and it is p1's Act. Task 1
adds the table step that cuts this map to its first sites and stores the
rest in the Atlas, which most tests below start from.

## Plan-time findings

The spec was checked against the code of slice 3a and the slice was
prototyped on it. The blocks below were then applied in order to a fresh
worktree at `bbfb403a7`, the merge of slice 3a into `0.2.0-alpha`, where
each task's tests and the full gates pass. Where the spec and the code
differ, this plan follows the code, and Task 8 corrects the spec.

The plan was revised after its review. The placement became an undo
barrier (finding 13), the owner decided that Hall of Mockery and Lost
Guardians move their banner (ruling 8, Task 5), and the review's smaller
points were taken: a second Brass Army test, the placed site read as the
Hintermost site, a stale comment, two comments on the stored edifice, test
helpers named for what they build, and a golden for a stopped Explore. The
revised blocks were applied in order to a fresh worktree at `219f325fe`,
where each task's tests and the full gates pass.

1. **An operation is applied with no catalog.** The spec writes
   `PlaceAtlasSite(end, region)`. `OperationApplication.mutate(ready,
   operation)` and replay read the state and the operation alone, and a
   site's capacity, relic slots and printed resources are catalog facts.
   The operation therefore carries those three numbers, which the walk
   copies from the catalog (`AtlasPlacement`, design decision 2). Every
   other recorded operation is built the same way: `SuitedReserveFromBox`
   carries the cards the walk read from the catalog.
2. **The card index did not hold a stored site's edifice.** Game start
   keeps an Atlas site's edifice in `AtlasEntry.StoredSite.edifice`, an id
   with no state, and `CardIndex.scan` reads only a stored site's denizens
   and relics. Placing such a site puts the edifice on the map, where the
   index does read it, so the batch's card inventory would change and
   `OperationStateInvariant.validate` would refuse it
   (`CardInventoryChanged`). In the fixtures every Homeland site stores an
   edifice, so the first Explore of a real game would have failed. Task 1
   indexes the stored edifice.
3. **Every WHEN EXPLORED rule already names the window, and applied
   wherever its face stood.** `MarketRule`, `ForgeRule`, `ProvingGrounds`,
   `EmptyGrounds` and `RuinedSetupSteps` list `WhenExplored` beside their
   Setup window, with `applicable` true whenever the edifice is on the map
   on that side. Folding the window as it stood would resolve every such
   face on the map at every Explore: a Bandit Market at the Cradle would
   place favor again each time. Task 4 makes each resolve only at the
   explored site.
4. **"If you place your pawn here" read Setup's last answer.**
   `EdificeSetupSupport.pawnPlacement` takes the player and the site from
   the last answered decision, Setup's pawn question. At an Explore the
   last answer is a button, so Broken Forge would never resolve. Task 4
   reads the explorer and the explored site there.
5. **An explored edifice is always ruined.** "Edifices stored with it flip
   to their ruined side." The intact faces that print WHEN EXPLORED (Great
   Market, Great Forge, Proving Grounds) are never placed by an Explore, so
   their halves cannot resolve; they keep naming the window, and Task 4's
   filter keeps one that already stands on the map quiet. The ruined faces
   that resolve are Hall of Mockery (E01), Bandit Market (E02), Broken
   Forge (E06), Fallen Spire (E11), Broken Clock (E12), Lost Guardians
   (E18), Empty Grounds (E22), Ruined School (E27) and Broken Horn (E30).
   Lost Doors (E05) and Ruined Temple (E10) are not implemented at Setup
   either and wait for Legacies. No denizen and no relic prints WHEN
   EXPLORED.
6. **The modifier choice needs a window of its own.** The spec names
   `ExploreCost` and `ExploreRoll`. `ModifierChoice.loop` takes the
   action's modifier-selection window, and `SelectedModifier` maps each
   such window to a `MajorActionType`, so Explore gets
   `ExploreModifierSelection` and `MajorActionType.Explore`. Nothing is
   offered there: the decision asks only when a modifier can be picked.
7. **A power may ask between the placement and its line.** Brass Army asks
   its holder "whenever your pawn moves (travel or place)", as a trigger
   on the executed batch, before the rest of the action. The walk then
   returns to the nodes after the placement in a later command, where a
   note that read "the step before" would find Brass Army's step. The
   Explore line and the `WhenExplored` node read the placed site from the
   live state, as the map's Hintermost site: the placement takes the first
   empty slot going Hinterward, so the site it placed is the last one
   Hinterward until something else places a site. A `Branch` is
   selected again on every resume, so the node that asks Recent or
   Forgotten is a branch of its own and the nodes after it keep their
   places once the Atlas is one site shorter.
8. **The placement must place the bandits itself.** The check after every
   action fills a site that holds no warband with bandits, but Bandit
   Market and Ruined School read which sites the bandits rule while the
   action is still running.
9. **The Act menu, the questions and the map need no frontend code.** The
   menu draws the options the engine lists, as text buttons with no icon
   (`MenuPanel`); the action icons in `PowerMarks` are the modifier tabs
   printed on cards, and no card prints one for Explore. Recover's own
   panel is keyed on the action `recover`; Explore's two questions are
   plain choose-one decisions the generic panel draws, with the roll
   beside them. The one frontend change is the word for the pool:
   `WalkerPanelSupport.poolLabel` knows `recover`, `campaign.attack` and
   `campaign.defense`, and prints any other key as it arrives. The map
   draws `region.sites` and `region.emptySlots`, so the new site replaces
   an empty frame with no change.
10. **Six suites list the actions by hand.** `ProcedureRefSuite`,
    `EnumWireKeySuite`, `PhaseMenuSuite`, `FollowUpSupplyWaiverSuite` and
    `GameLogPropertiesSuite` (twice, and its "every ProcedureRef key
    appears in some script" needs a script that completes an Explore), and
    the note-key snapshot. `ProcedureLogs.rules` is an exhaustive match, so
    the new `ActionRef` does not compile until it has its Game Log entry.
    Task 3 changes all of them with the action.
11. **The quiet table's Atlas is empty.** The fixture's Chronicle stores
    exactly the eight sites a first game places, and slice 3a's
    `Table.sitesInPlay` drops the sites it cuts. Task 1 adds the steps
    that cut the map into the Atlas and store cards on an Atlas site.
12. **The Chronicle Phase needs no change.** `ChronicleExtraction` reads
    the map and the Atlas as they are, and the explored site is on the
    map. Task 7 is tests only.
13. **Undo would have leaked both ends of the Atlas** (found in the plan's
    review). Undo refuses a batch that shows a hidden outcome
    (`UndoBarrier.reveals`, which asks `HiddenOutcomes.hidesAnywhere`).
    `PlaceAtlasSite` puts a site and its stored cards on the map and deals
    relics nobody saw, but `HiddenOutcomes.hides` did not list it: an
    explorer could answer Forgotten, see the site, undo and answer Recent.
    Task 2 lists it, so the legality search stops before the placement and
    undo never takes it back, nor the end's answer it ran on.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **Explore is a major action of its own.** `ActionRef.Explore` (key
   `explore`), `MajorActionType.Explore`, and three windows:
   `ExploreModifierSelection`, `ExploreCost` and `ExploreRoll`. Its pool
   is `explore`, its decisions `explore.choice` and `explore.end`. It has
   no `ExploreActionEligibility` window: no power needs one.
   `FollowUpSupplyWaiver` learns `ExploreCost` with the other cost
   windows, though no power queues an Explore.
2. **`PlaceAtlasSite(end, region, capacity, relicSlots, resources)`.** The
   two fields the spec names say what happens; the other three are the
   site's printed facts (finding 1). Applying it takes the end's site out
   of the Atlas, checks that `region` holds the map's next slot, and
   builds the site with `SiteState.banditSite`, which
   `FirstGameSetupMaterializer.atlasSite` now calls too, so Setup and
   Explore populate a site through one function. It deals the open relic
   slots from the top of the relic deck while it lasts and takes the
   bandits the bank holds, up to the capacity. It does not name the site:
   the end and the state do.
3. **`AtlasPlacement.at(catalog, ready, end)`** in `gameplay.setup`, beside
   the materializer, returns the stored site and the operation that places
   it, or why it cannot. Explore calls it once; slice 4 calls it once per
   empty slot and reads the stored site's denizens for the favor it adds.
4. **The gates are start gates.** `ExploreProcedure.build` refuses with
   `OathViolation.StartUnavailable(reason)`, which the Act menu shows as
   the option's reason, in this order: "Explore needs the Wide Horizons
   foundation", "The map has no empty site slot", "The Atlas holds no
   site", "Your pawn is not at the Hintermost site". `rebuild`, which a
   resume uses, runs none of them: the Explore itself moves the pawn, and
   may fill the last slot or take the last site. A short Supply is the
   pipeline's refusal, which the menu already words: "Needs 1 Supply; you
   have 0".
5. **Explore is listed in every Act menu.** Under Imperial Maps it is
   shown unavailable with its reason, as the spec says, rather than
   hidden.
6. **The questions are worded by the engine.** After a short roll:
   "Roll 2 more dice (1 Supply)" or "Stop exploring", under the heading
   "Explore"; with no Supply left the first is shown unavailable, "No
   Supply left". After a success with two or more sites in the Atlas:
   "Recent Sites" or "Forgotten Sites", under "Explore: place a site
   from". Neither option shows the site it would give. With one site in
   the Atlas nothing is asked and the line says "Recent Site".
7. **`ExploredSite(site)`** is the node the `WhenExplored` window is
   folded on, declared with the procedure. A setup rule reads it from
   `PowerCtx.operation` (`EdificeSetupSupport.resolvesAt`), so the powers
   depend on the action and the action on no power.
8. **Three note keys.** `explored-recent`, `explored-forgotten` and
   `failed`, under `procedure.explore`: a note has no free-text argument,
   so each end has its key. The second and later payments write
   "Continued exploring", the twin of Recover's line (`StartLines`).
   Explore's lines have no band color of their own.
9. **Tests.** `WorldModelSuite`, `CardIndexSuite` and `TableSuite` take
   the model and table steps; `AtlasOperationsSuite` (new) the operation;
   `AtlasPlacementSuite` (new) its builder; `ExploreProcedureSuite` (new)
   the gates, the rolls, the ends and replay; `WhenExploredSuite` (new)
   each power on the explored site, and with `RuinedSetupRulesSuite` the
   banner's return; `UndoBarrierPlaySuite` the undo barrier;
   `ExploreLogScripts` (new) three goldens, `explore`, `explore-stopped`
   and `explore-bandit-market`; `ExploreGameSuite` (new) the
   projection of a game started from a Chronicle; and
   `ExploredSiteChronicleSuite` (new) the Chronicle round trip. Two
   test-table steps are added: `Table.mapCutIntoAtlas(kept)` and
   `Table.storedInAtlas(site, cards*)`.
10. **Docs.** The readings below go into the spec's Explore subsection, as
    slices 2 and 3a put theirs in the spec, and slice 7 moves them to
    `docs/rules/ambiguities.md`. `CONTEXT.md` stays for slice 7, as the
    spec's Docs list says. The roadmap loses what this slice finishes.

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **Only Explore's own windows change an Explore.** Card: "Explore works
   like a Recover action with a **difficulty of 4**." Rulebook, Major
   Action Modifiers: "Modifiers have an icon matching a major action on
   its braid." Reading: Explore is its own action, so no modifier is
   offered for it and no power changes its Supply cost, each of which
   hooks one action's windows. Owner ruling 2 already keeps the Recover
   powers out and Jinx in. What holds for every operation still holds: a
   restriction on operations, and a trigger on an executed batch, such as
   Brass Army's "Whenever your pawn moves (travel or place)", which asks
   its holder as their pawn is placed on the explored site (Task 3).
2. **The next Hinterward slot is the first empty slot in slot order.**
   Card: "place a site on the next Hinterward slot". Rulebook, Empire:
   "Fill site slots going **Hinterward**—top to bottom in the Cradle, then
   top to bottom in the Provinces, then top to bottom in the Hinterland."
   Reading: when the Hintermost site's region is full, the site goes to
   the top slot of the next region. The placed site is the new Hintermost,
   and the explorer's pawn stands on it, so they may Explore again with
   their next action (Tasks 2 and 3).
3. **Who rules the Hintermost site does not matter.** Rulebook: "You may
   spend 1 Supply to Explore if your pawn is at the **Hintermost** site."
   Reading: the text names only the pawn. An Exile at an Imperial site, a
   Citizen at a bandit site and a player at an enemy's site may all
   Explore (Task 3).
4. **The end is chosen after the roll succeeds, at each Explore, unseen.**
   Rulebook: "If you succeed, place a site _(from Recent or Forgotten)_ in
   the next Hinterward slot". Reading: the explorer chooses once the roll
   has succeeded, and a later Explore may take the other end: "not both"
   is Imperial Maps' limit on filling all the slots at Setup. The question
   names the two ends and shows neither site. With one site left it is not
   asked, and the line calls it a Recent Site. The placement is an undo
   barrier: once the site is seen, the end cannot be chosen again (finding
   13; Tasks 2 and 3).
5. **The site is populated as Foundation I prints, while supplies last.**
   Rulebook, Foundation I: "Edifices stored with it flip to their **ruined
   side** [...]. Other denizens start faceup. Relics stored with it start
   facedown. **Bandits** are placed equal to its capacity. **Fill all of
   its empty relic slots** by drawing relics from the relic deck."
   Rulebook, Starting Resources: "_(With the Wide Horizons foundation, it
   is placed when the site is Explored.)_" Reading: a relic deck that runs
   out leaves the slots it cannot fill open, as slice 3a's ruling 5 has it
   for Setup With No Empire. A bandit bank that runs out, which needs
   bandits standing on a card, leaves the site short. The dealt relics are
   facedown and nobody learns them (Task 2).
6. **An explored site's denizens add no favor to the suit banks.**
   Rulebook, Foundation II: "Then, for each denizen on the map, add 1 extra
   favor [...] to the matching favor bank." Reading: that is a setup step,
   resolved once after Foundation I. A site explored in play adds nothing.
   Slice 4 gives the favor for the sites Imperial Maps places at Setup
   (Task 2: `PlaceAtlasSite` moves no favor).
7. **WHEN EXPLORED resolves for the explored site's cards, after the pawn
   is placed.** Rulebook: "The **When Explored** text is used with the Wide
   Horizons foundation [...]: it triggers when the card is placed on the map
   by the Explore action." Card: "place a site on the next Hinterward slot,
   populate it [...], and then place your pawn on the site." Reading: a
   face that already stood on the map does not resolve again. The power
   resolves once the whole placement is done, the pawn included, so "If
   you place your pawn here" (Broken Forge) holds for the explorer, who is
   its "you". "This region" is the explored site's, and "each site ruled
   by the bandits" counts the explored site. No ruler is asked to consent:
   the automatic power consent design's ruling 7 says Setup rules are
   never refused and leaves an Explore's filtering to this change (Task
   4).
8. **Hall of Mockery and Lost Guardians move a held banner to the shared
   bank. Decided with the owner in the plan's review; implemented.**
   Cards: "Place [4 favor] on the People's Favor and move it to the shared
   bank—players **cannot** start with it", and the same for the Darkest
   Secret with 3 secrets. The banner leaves its holder for the shared
   bank, which holds a banner nobody holds, when explored and at Setup's
   end alike; one already there stays. Until now no operation returned a
   held banner (`BoardControlOperations.moveBanner` accepted a play area
   only), so the step was missing at a later game's Setup too (catalog
   batch 9, "Catalog and roadmap corrections"). **This changes a later
   game's Setup:** a Chancellor who starts with the People's Favor (goal
   the People) or the Darkest Secret (Devotion) loses it at Setup's end
   when the matching ruined edifice is on the map, as the card says. The
   first game, where both banners start in the shared bank, does not
   change, nor does any golden log. The lines say the move (Task 5).
9. **Broken Clock advances the round marker at once.** Card: "Advance the
   round marker once, if possible. _Do not roll [the round die]. This
   cannot end the game._" Reading: the marker moves in the middle of the
   round, as the intact Great Clock's action already moves it, and on
   round 8 nothing happens and no line is written (Task 4).
10. **An Explore is an action at the action boundary, failed or not.**
    Rulebook, Capacity: "After any action, if a site has no warbands on
    it, immediately place bandit warbands on it equal to its capacity."
    Reading: the boundary's bandit refill and Oathkeeper check follow an
    Explore as they follow a Recover, and a stopped Explore has spent its
    action's Supply (Task 3).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/World.scala` | `AtlasEnd`; `AtlasState.sites`, `site`, `without`; `StoredSite.placedDenizens`; `SiteState.banditSite`, `openRelicSlots`; `MapState.withSite` |
| `src/main/scala/distantlands/model/CardIndex.scala` | a stored site's edifice is indexed |
| `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`, `OperationStateWrites.scala` | comments: no operation moves or changes a stored edifice |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | an atlas site is built by `SiteState.banditSite` |
| `src/main/scala/distantlands/model/CoreOperations.scala` | `PlaceAtlasSite` |
| `src/main/scala/distantlands/gameplay/operations/AtlasOperations.scala` | new: applies `PlaceAtlasSite` |
| `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala` | dispatches it |
| `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` | `place-atlas-site` on the wire |
| `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala` | new: builds the operation from the catalog |
| `src/main/scala/distantlands/gameplay/walker/HiddenOutcomes.scala` | the placement is a hidden outcome, so an undo barrier |
| `src/main/scala/distantlands/model/PowerWindow.scala` | `MajorActionType.Explore`; `ExploreModifierSelection`, `ExploreCost`, `ExploreRoll` |
| `src/main/scala/distantlands/model/ProcedureRef.scala` | `ActionRef.Explore` |
| `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala` | new: the tree, its gates and notes, and `ExploredSite` |
| `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`, `FollowUpSupplyWaiver.scala` | Explore's entry and cost window; a stale count in a comment |
| `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala`, `src/main/scala/distantlands/gameplay/phases/ActOffers.scala`, `src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala` | its note keys, its Act menu offer, its modifier window |
| `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`, `StartLines.scala` | its Game Log entry, tag anchor and "Continued exploring" |
| `src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala`, `GreatMarketRules.scala`, `ProvingGroundsRules.scala`, `BannerSetupRules.scala`, `RuinedSiteSetupRules.scala` | WHEN EXPLORED resolves at the explored site |
| `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala` | a held banner may return to the shared bank |
| `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala` | also: Hall of Mockery and Lost Guardians move a held banner (`BannerToSharedBank`) |
| `frontend/src/main/scala/distantlands/frontend/WalkerPanelSupport.scala` | the Explore pool reads as shields |
| `src/test/scala/distantlands/testkit/Table.scala`, `TableSuite.scala` | `mapCutIntoAtlas`, `storedInAtlas` |
| `src/test/scala/distantlands/model/WorldModelSuite.scala`, `CardIndexSuite.scala` | the model |
| `src/test/scala/distantlands/gameplay/operations/AtlasOperationsSuite.scala` | new: the operation |
| `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala` | new: its builder |
| `src/test/scala/distantlands/gameplay/walker/HiddenOutcomesSuite.scala` | the placement hides its outcome |
| `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala` | an Explore's end cannot be taken back |
| `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala` | its wire form |
| `src/test/scala/distantlands/gameplay/ExploreProcedureSuite.scala` | new: gates, rolls, ends, replay |
| `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala`, `GoldenScripts.scala` | new: the three Explore scripts, registered |
| `src/test/resources/gamelog/explore.*.log`, `explore-stopped.*.log`, `explore-bandit-market.*.log`, `src/test/resources/notes/declared-keys.txt` | goldens and the note-key snapshot, written by their commands |
| `src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala`, `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala`, `FollowUpSupplyWaiverSuite.scala`, `src/test/scala/distantlands/model/EnumWireKeySuite.scala`, `ProcedureRefSuite.scala` | their hand lists gain Explore |
| `src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala` | new: each power on the explored site |
| `src/test/scala/distantlands/gameplay/OperationApplicationSuite.scala`, `src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala` | the banner's return, at Setup too |
| `src/test/scala/distantlands/application/ExploreGameSuite.scala` | new: the projection of a game from a Chronicle |
| `frontend/src/test/scala/distantlands/frontend/WalkerChoicePanelRenderSuite.scala` | the Explore question's panel |
| `src/test/scala/distantlands/gameplay/chronicle/ExploredSiteChronicleSuite.scala` | new: the Chronicle round trip |
| `docs/architecture/game-setup.md`, `docs/architecture/gameplay-modules.md`, `docs/rules/implementation-traceability.md`, `docs/rules/ambiguities.md`, `docs/ROADMAP.md`, the spec | docs |

---

### Task 1: The Atlas's ends, the bandit site and the table steps

**Files:**
- Modify: `src/main/scala/distantlands/model/World.scala`
- Modify: `src/main/scala/distantlands/model/CardIndex.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Test: `src/test/scala/distantlands/model/WorldModelSuite.scala`
- Test: `src/test/scala/distantlands/model/CardIndexSuite.scala`
- Test: `src/test/scala/distantlands/testkit/TableSuite.scala`

**Interfaces:**
- Consumes: slice 3a's `MapState` (`nextSlot`, `hintermost`, `inSlots`,
  `sitesIn`), `AtlasState(entries)`, `AtlasEntry.StoredSite(id, denizens,
  relics, edifice)` and `AtlasEntry.EmpireDivider`; `Table.sitesInPlay`.
- Produces:
  - `enum AtlasEnd(val key: String)`: `Recent` ("recent") and `Forgotten`
    ("forgotten"); `AtlasEnd.all`, `AtlasEnd.fromKey(key): Option[AtlasEnd]`.
  - `AtlasState.sites: Vector[AtlasEntry.StoredSite]` (Recent end first),
    `AtlasState.site(end: AtlasEnd): Option[AtlasEntry.StoredSite]` and
    `AtlasState.without(id: SiteId): AtlasState`.
  - `AtlasEntry.StoredSite.placedDenizens: Vector[SiteDenizenState]`: the
    edifice ruined, then the denizens faceup.
  - `SiteState.banditSite(bandits: Int, resources: Tokens, denizens:
    Vector[SiteDenizenState], relics: Vector[RelicId]): SiteState` and
    `SiteState.openRelicSlots(slots: Int, stored: Int): Int`.
  - `MapState.withSite(region: Region, site: SiteId, state: SiteState):
    MapState`, the method slice 3a left for this slice.
  - `CardIndex` holds a stored site's edifice at
    `CardContainer.AtlasSite(position, site, SiteCardArea.Denizens)`, after
    the site's stored denizens, with no state.
  - Test table steps `Table.mapCutIntoAtlas(kept: Int)` and
    `Table.storedInAtlas(site: SiteRef, cards: CardRef*)`.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/model/WorldModelSuite.scala`:

Replace

```scala
    assertEquals(mapOfSites(0).hintermost, None)
    assertEquals(mapOfSites(0).nextSlot, Some(Region.Cradle))
```

with

```scala
    assertEquals(mapOfSites(0).hintermost, None)
    assertEquals(mapOfSites(0).nextSlot, Some(Region.Cradle))

  test("a site placed in a region takes its next slot, after its sites"):
    val four = mapOfSites(4)
    val placed = four.withSite(Region.Provinces, SiteId("S9"), banditHeldSite)
    assertEquals(placed.provinces.map(_.value), Vector("S3", "S4", "S9"))
    assertEquals(placed.hintermost, Some(SiteId("S9")))
    assertEquals(placed.sites(SiteId("S9")), banditHeldSite)
    assertEquals(placed.nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(1).withSite(Region.Cradle, SiteId("S9"),
      banditHeldSite).cradle.map(_.value), Vector("S1", "S9"))
    assertEquals(mapOfSites(5).withSite(Region.Hinterland, SiteId("S9"),
      banditHeldSite).hinterland.map(_.value), Vector("S9"))

  /** An Atlas of the sites A1 to A`count` around an Empire divider, A1 at
    * the Recent end. */
  private def atlasOfSites(count: Int): AtlasState = AtlasState(
    (1 to count).toVector.flatMap(index => Vector[AtlasEntry](
      AtlasEntry.StoredSite(SiteId(s"A$index"), Vector.empty, Vector.empty),
      AtlasEntry.EmpireDivider)))

  test("the Recent end of the Atlas is its first site and the Forgotten " +
      "end its last, and one site is both"):
    assertEquals(AtlasEnd.all.map(_.key), Vector("recent", "forgotten"))
    assertEquals(AtlasEnd.all.flatMap(end => AtlasEnd.fromKey(end.key)),
      AtlasEnd.all)
    assertEquals(AtlasEnd.fromKey("middle"), None)
    val three = atlasOfSites(3)
    assertEquals(three.sites.map(_.id.value), Vector("A1", "A2", "A3"))
    assertEquals(three.site(AtlasEnd.Recent).map(_.id.value), Some("A1"))
    assertEquals(three.site(AtlasEnd.Forgotten).map(_.id.value), Some("A3"))
    assertEquals(AtlasEnd.all.map(atlasOfSites(1).site(_).map(_.id.value)),
      Vector(Some("A1"), Some("A1")))
    assertEquals(AtlasEnd.all.map(atlasOfSites(0).site), Vector(None, None))
    val taken = three.without(SiteId("A2"))
    assertEquals(taken.sites.map(_.id.value), Vector("A1", "A3"))
    assertEquals(taken.entries.count(_ == AtlasEntry.EmpireDivider), 3)

  test("a bandit site holds its bandits, its printed tokens and its relics " +
      "facedown, and a stored site's edifice is placed ruined before its " +
      "denizens, faceup"):
    val edifice = EdificeId("E-test")
    val denizen = DenizenId("D-test")
    val stored = AtlasEntry.StoredSite(SiteId("A1"),
      Vector(DenizenState(denizen, Orientation.FaceDown, Tokens.empty),
        EdificeState(EdificeId("E-other"), EdificeSide.Intact, Tokens.empty)),
      Vector.empty, Some(edifice))
    assertEquals(stored.placedDenizens, Vector[SiteDenizenState](
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty),
      EdificeState(EdificeId("E-other"), EdificeSide.Ruined, Tokens.empty)))
    val relics = Vector(RelicId("R-a"), RelicId("R-b"))
    assertEquals(SiteState.banditSite(2, Tokens(3, 0), stored.placedDenizens,
      relics), SiteState(SiteForces.Occupied(ForceKind.Bandit, 2),
      stored.placedDenizens,
      relics.map(RelicState(_, Orientation.FaceDown, Tokens.empty)),
      Tokens(3, 0)))
    assertEquals(SiteState.banditSite(0, Tokens.empty, Vector.empty,
      Vector.empty).forces, SiteForces.Empty)
    assertEquals(Vector(0, 1, 2, 3).map(SiteState.openRelicSlots(2, _)),
      Vector(2, 1, 0, 0))

```

**In** `src/test/scala/distantlands/model/CardIndexSuite.scala`:

Replace

```scala
    )

  test("the derived index rejects a card present in two containers"):
    // Patched with copy, not a Table step: the test makes an illegal state
```

with

```scala
    )

  test("a stored site's edifice is indexed in the Atlas, after its " +
      "denizens, with no state"):
    val edifice = EdificeId("E-stored")
    // Patched with copy: no Table is built on this fixture's game.
    val storing = game.copy(campaign = game.campaign.copy(atlas = AtlasState(
      game.campaign.atlas.entries.map {
        case stored: AtlasEntry.StoredSite =>
          stored.copy(edifice = Some(edifice))
        case other => other
      })))
    val index = accepted(CardIndex.from(storing))
    assertEquals(index.locationOf(edifice), Some(CardLocation(
      CardContainer.AtlasSite(0, SiteId("S9"), SiteCardArea.Denizens), 1)))
    assertEquals(index.stateOf(edifice), None)

  test("the derived index rejects a card present in two containers"):
    // Patched with copy, not a Table step: the test makes an illegal state
```

**In** `src/test/scala/distantlands/testkit/Table.scala`:

Replace

```scala
      MapState.inSlots(kept.map(site => site -> map.sites(site))))))

  /** The Reliquary set aside for the game, as Setup With No Empire leaves
    * it: its relics stay where they are. */
```

with

```scala
      MapState.inSlots(kept.map(site => site -> map.sites(site))))))

  /** The map cut to its first `kept` sites Hinterward, and each site cut
    * stored in the Atlas with no card on it, the first one cut at the
    * Recent end: a map Explore can add to. A site that is cut must hold no
    * card and no pawn. */
  def mapCutIntoAtlas(kept: Int)(using munit.Location): Table =
    val cut = game.game.current.map.hinterward.drop(kept)
    sitesInPlay(kept).update(_.updateCampaign(campaign => campaign.copy(
      atlas = AtlasState(cut.map(AtlasEntry.StoredSite(_, Vector.empty,
        Vector.empty)) ++ campaign.atlas.entries))))

  /** `cards` stored on `site` in the Atlas, after any it already stores,
    * as a Chronicle stores them: an edifice, denizens faceup and relics
    * facedown. */
  def storedInAtlas(site: SiteRef, cards: CardRef*)(
      using munit.Location): Table =
    val id = CardRefs.siteId(site)
    if !game.game.campaign.atlas.sites.exists(_.id == id) then
      munit.Assertions.fail(s"${id.value} is not in the Atlas")
    def storing(stored: AtlasEntry.StoredSite, card: CardId)
        : AtlasEntry.StoredSite = card match
      case edifice: EdificeId => stored.copy(edifice = Some(edifice))
      case denizen: DenizenId => stored.copy(denizens = stored.denizens :+
        DenizenState(denizen, Orientation.FaceUp, Tokens.empty))
      case relic: RelicId => stored.copy(relics = stored.relics :+
        RelicState(relic, Orientation.FaceDown, Tokens.empty))
      case other => munit.Assertions.fail(s"$other cannot be stored on a site")
    cards.map(CardRefs.cardId).foldLeft(this)((table, card) =>
      table.moving(card).update(_.updateCampaign(campaign => campaign.copy(
        atlas = AtlasState(campaign.atlas.entries.map {
          case stored: AtlasEntry.StoredSite if stored.id == id =>
            storing(stored, card)
          case other => other
        })))))

  /** The Reliquary set aside for the game, as Setup With No Empire leaves
    * it: its relics stay where they are. */
```

**In** `src/test/scala/distantlands/testkit/TableSuite.scala`:

Replace

```scala
    assertEquals(ready.game.current.commonCards.worldDeck.lastOption,
      Some(EldersCard.id))
```

with

```scala
    assertEquals(ready.game.current.commonCards.worldDeck.lastOption,
      Some(EldersCard.id))

  test("a map cut into the Atlas keeps its first sites and stores the " +
      "rest, the first one cut at the Recent end, and a stored card leaves " +
      "its zone"):
    import distantlands.gameplay.powers.setup.GreatMarketCard
    val whole = Table.start.ready.game.current.map.hinterward
    val table = Table.start.mapCutIntoAtlas(3)
      .storedInAtlas(whole(3), GreatMarketCard, EldersCard, StickyFireCard)
    val ready = table.ready
    assertEquals(ready.game.current.map.hinterward, whole.take(3))
    assertEquals(ready.game.campaign.atlas.sites.map(_.id), whole.drop(3))
    assertEquals(ready.game.campaign.atlas.site(AtlasEnd.Recent),
      Some(AtlasEntry.StoredSite(whole(3),
        Vector(DenizenState(EldersCard.id, Orientation.FaceUp, Tokens.empty)),
        Vector(RelicState(StickyFireCard.id, Orientation.FaceDown,
          Tokens.empty)), Some(GreatMarketCard.id))))
    val zones = ready.game.current.commonCards
    assert(!zones.edificeDeck.contains(GreatMarketCard.id))
    assert(!zones.worldDeck.contains(EldersCard.id))
    assert(!zones.relicDeck.contains(StickyFireCard.id))
    intercept[munit.FailException](
      Table.start.storedInAtlas(whole(3), EldersCard))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.model.WorldModelSuite distantlands.model.CardIndexSuite distantlands.testkit.TableSuite"`
Expected: FAIL. The tests do not compile: `withSite`, `AtlasEnd`,
`placedDenizens` and `SiteState.banditSite` do not exist, and `AtlasState`
has no `sites`.

- [ ] **Step 3: Implement**

`World.scala` gains the builder of a bandit site, the method that puts a
site in a region's next slot, the Atlas's two ends and the stored site's
placed cards. The materializer builds an atlas site through the shared
builder, so Setup and `PlaceAtlasSite` (Task 2) cannot populate a site
differently. The card index reads a stored site's edifice (finding 2). The
two writers that read the index's Atlas locations ignore that edifice,
which no operation moves or changes on its own: each says so in a comment.

**In** `src/main/scala/distantlands/model/World.scala`:

Replace

```scala
)

/** The sites on the map, by region. A region may hold fewer sites than its
  * slots: under Wide Horizons and after Setup With No Empire the slots past
```

with

```scala
)

object SiteState:
  /** A site placed from the Atlas as a bandit site, populated as Foundation
    * I and Setup With No Empire both print: `denizens` as given, `relics`
    * facedown, `bandits` warbands, none for a site of capacity 0, and its
    * printed `resources`. */
  def banditSite(bandits: Int, resources: Tokens,
      denizens: Vector[SiteDenizenState], relics: Vector[RelicId]): SiteState =
    SiteState(
      if bandits == 0 then SiteForces.Empty
      else SiteForces.Occupied(ForceKind.Bandit, bandits),
      denizens, relics.map(RelicState(_, Orientation.FaceDown, Tokens.empty)),
      resources)

  /** The relic slots of a site with `slots` that `stored` relics leave
    * open, never fewer than 0. */
  def openRelicSlots(slots: Int, stored: Int): Int = math.max(0, slots - stored)

/** The sites on the map, by region. A region may hold fewer sites than its
  * slots: under Wide Horizons and after Setup With No Empire the slots past
```

Then replace

```scala
  def hintermost: Option[SiteId] = hinterward.lastOption

  def regionOf(site: SiteId): Option[Region] =
    if cradle.contains(site) then Some(Region.Cradle)
```

with

```scala
  def hintermost: Option[SiteId] = hinterward.lastOption

  /** The map with `site` in the next slot of `region`, after its sites. */
  def withSite(region: Region, site: SiteId, state: SiteState): MapState =
    val placed = copy(sites = sites.updated(site, state))
    region match
      case Region.Cradle => placed.copy(cradle = cradle :+ site)
      case Region.Provinces => placed.copy(provinces = provinces :+ site)
      case Region.Hinterland => placed.copy(hinterland = hinterland :+ site)

  def regionOf(site: SiteId): Option[Region] =
    if cradle.contains(site) then Some(Region.Cradle)
```

Then replace

```scala
      relics: Vector[RelicState],
      edifice: Option[EdificeId] = None
  ) extends AtlasEntry

  case object EmpireDivider extends AtlasEntry

/**
 * One ordered Atlas sequence. `entries.head` is the Recent end and
 * `entries.last` is the Forgotten end.
 */
final case class AtlasState(entries: Vector[AtlasEntry])

enum PeoplesFavorFace { case Mob, GrandCouncil }
```

with

```scala
      relics: Vector[RelicState],
      edifice: Option[EdificeId] = None
  ) extends AtlasEntry:
    /** The cards in the site's denizen slots once it is placed as a bandit
      * site: its edifice ruined, then its denizens faceup. */
    def placedDenizens: Vector[SiteDenizenState] =
      edifice.toVector.map(EdificeState(_, EdificeSide.Ruined, Tokens.empty)) ++
        denizens.map:
          case denizen: DenizenState =>
            denizen.copy(orientation = Orientation.FaceUp)
          case stored: EdificeState => stored.copy(side = EdificeSide.Ruined)

  case object EmpireDivider extends AtlasEntry

/** An end of the Atlas box: the Recent Sites side, its front, or the
  * Forgotten Sites side, its back. */
enum AtlasEnd(val key: String):
  case Recent extends AtlasEnd("recent")
  case Forgotten extends AtlasEnd("forgotten")
object AtlasEnd:
  val all: Vector[AtlasEnd] = Vector(Recent, Forgotten)
  def fromKey(key: String): Option[AtlasEnd] = all.find(_.key == key)

/**
 * One ordered Atlas sequence. `entries.head` is the Recent end and
 * `entries.last` is the Forgotten end.
 */
final case class AtlasState(entries: Vector[AtlasEntry]):
  /** The stored sites, Recent end first. */
  def sites: Vector[AtlasEntry.StoredSite] =
    entries.collect { case site: AtlasEntry.StoredSite => site }

  /** The site a player takes from `end`: the first stored site for Recent,
    * the last for Forgotten. None once the Atlas holds no site. */
  def site(end: AtlasEnd): Option[AtlasEntry.StoredSite] = end match
    case AtlasEnd.Recent => sites.headOption
    case AtlasEnd.Forgotten => sites.lastOption

  /** The Atlas with the stored site `id` taken out. */
  def without(id: SiteId): AtlasState = AtlasState(entries.filter:
    case site: AtlasEntry.StoredSite => site.id != id
    case AtlasEntry.EmpireDivider => true)

enum PeoplesFavorFace { case Mob, GrandCouncil }
```

**In** `src/main/scala/distantlands/model/CardIndex.scala`:

Replace

```scala
          )
        )
      case (AtlasEntry.EmpireDivider, _) => ()

```

with

```scala
          )
        )
        // The site's stored edifice lies with its denizens, after them: it
        // has no side until the site is placed.
        stored.edifice.foreach(edifice => addId(edifice,
          CardContainer.AtlasSite(atlasPosition, stored.id,
            SiteCardArea.Denizens), stored.denizens.size))
      case (AtlasEntry.EmpireDivider, _) => ()

```

**In** `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
      val slots = catalog.site(site).get.relicSlots
      val count = if empire(site) then 0
        else stored.get(site).fold(slots)(atlas => math.max(0,
          slots - atlas.items.count(_.isInstanceOf[RelicId])))
      val assigned = plan.relicOrder.slice(offset, offset + count)
      offset += count
```

with

```scala
      val slots = catalog.site(site).get.relicSlots
      val count = if empire(site) then 0
        else stored.get(site).fold(slots)(atlas => SiteState.openRelicSlots(
          slots, atlas.items.count(_.isInstanceOf[RelicId])))
      val assigned = plan.relicOrder.slice(offset, offset + count)
      offset += count
```

Then replace

```scala
    * Empire's site is set up the same way with no Empire. */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
    SiteState(bandits(stored.site), storedDenizens(stored, EdificeSide.Ruined),
      faceDown(storedRelics(stored) ++ drawn),
      catalog.site(stored.site).get.startingResources)

  private def storedDenizens(stored: StoredSite, side: EdificeSide)
```

with

```scala
    * Empire's site is set up the same way with no Empire. */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
    val printed = catalog.site(stored.site).get
    SiteState.banditSite(printed.capacity, printed.startingResources,
      storedDenizens(stored, EdificeSide.Ruined), storedRelics(stored) ++ drawn)

  private def storedDenizens(stored: StoredSite, side: EdificeSide)
```

**In** `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`:

Replace

```scala
        .updated(suit, campaign.suitedReserves.getOrElse(suit, Vector.empty)
          .filterNot(_ == located.id)))))
    case CardContainer.AtlasSite(position, _, area) =>
      val entries = ready.game.campaign.atlas.entries.updated(position,
```

with

```scala
        .updated(suit, campaign.suitedReserves.getOrElse(suit, Vector.empty)
          .filterNot(_ == located.id)))))
    // A stored site's edifice, which the card index also lists under its
    // denizens, leaves the Atlas only with its site (`PlaceAtlasSite`): no
    // operation moves it alone.
    case CardContainer.AtlasSite(position, _, area) =>
      val entries = ready.game.campaign.atlas.entries.updated(position,
```

**In** `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala`:

Replace

```scala
    }.toRight(AmbiguousLocation(Location.Atlas,
      "stored Atlas site is unavailable")).flatMap { stored =>
      val updated = container.area match
        case SiteCardArea.Denizens =>
```

with

```scala
    }.toRight(AmbiguousLocation(Location.Atlas,
      "stored Atlas site is unavailable")).flatMap { stored =>
      // A stored site's edifice has no state to update: it is found among
      // neither the denizens nor the relics, and no operation changes it.
      val updated = container.area match
        case SiteCardArea.Denizens =>
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.model.WorldModelSuite distantlands.model.CardIndexSuite distantlands.testkit.TableSuite"`
Expected: PASS.

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.model.*"`
Expected: PASS. Game start builds the same atlas sites as before through
`SiteState.banditSite`, and every game it starts still has a whole card
index with the stored edifices in it.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/main/scala/distantlands/model/CardIndex.scala src/main/scala/distantlands/model/World.scala src/test/scala/distantlands/model/CardIndexSuite.scala src/test/scala/distantlands/model/WorldModelSuite.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/testkit/TableSuite.scala
git commit -m "feat(atlas): the Atlas's ends and a shared bandit-site builder" -m "AtlasState gives the site at its Recent or Forgotten end, MapState puts a site in a region's next slot, and SiteState.banditSite populates a site placed from the Atlas, for game start too. The card index holds a stored site's edifice. The test table can cut its map into the Atlas and store cards there." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `PlaceAtlasSite`

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala`
- Create: `src/main/scala/distantlands/gameplay/operations/AtlasOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`
- Create: `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/HiddenOutcomes.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/AtlasOperationsSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala` (new)
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/HiddenOutcomesSuite.scala`

**Interfaces:**
- Consumes: Task 1's `AtlasEnd`, `AtlasState.site`/`without`,
  `StoredSite.placedDenizens`, `SiteState.banditSite`/`openRelicSlots`,
  `MapState.withSite`, `Table.mapCutIntoAtlas`/`storedInAtlas`;
  `MapState.nextSlot`; `OperationStateAdapter.quantity`.
- Produces:
  - `final case class PlaceAtlasSite(end: AtlasEnd, region: Region,
    capacity: Int, relicSlots: Int, resources: Tokens) extends
    PrimitiveOperation`. On the wire: `{"kind": "place-atlas-site", "end",
    "region", "capacity", "relicSlots", "resources": {"favor",
    "secrets"}}`, every field required.
  - Applying it: the site at `end` leaves the Atlas for the next slot of
    `region`, built by `SiteState.banditSite`; refused with no site in the
    Atlas or when `region` does not hold the map's next slot.
  - `AtlasPlacement.at(catalog: ExecutableCatalog, ready: ReadyGame, end:
    AtlasEnd): Either[OathViolation, AtlasPlacement.Planned]`, with
    `Planned(stored: AtlasEntry.StoredSite, operation: PlaceAtlasSite)`
    and `Planned.site: SiteId`. Task 3 calls it; slice 4 will.
  - `HiddenOutcomes.hides(PlaceAtlasSite(...))` is true: the placement
    shows a site and cards nobody saw, so the legality search stops before
    it and undo never takes it back (`UndoBarrier`, finding 13). Task 3
    pins the undo.

- [ ] **Step 1: Write the failing tests**

**Create** `src/test/scala/distantlands/gameplay/operations/AtlasOperationsSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.gameplay.cards.{DeepWoodsSite, DesolateShoreSite,
  FairIsleSite, GoldenValleySite}
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.WizardsConclaveCard
import distantlands.model._
import distantlands.testkit.{Look, RawOperationBatch, Table}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `PlaceAtlasSite` (Foundations design, slice 3, "Explore"): a site
  * leaves an end of the Atlas for the next slot and is populated as a
  * bandit site. */
class AtlasOperationsSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  /** The quiet map cut to Ancient City, Broken Peaks and Buried Giant. The
    * Atlas holds Deep Woods at its Recent end, then Desolate Shore, Dunes,
    * Fair Isle, and Golden Valley at its Forgotten end. The next slot is
    * in the Provinces. */
  private def threeSitesAndAnAtlas: Table = Table.start.mapCutIntoAtlas(3)

  /** The operation that places `site` from `end` as its printed facts say:
    * what a walk builds from the catalog. */
  private def placing(end: AtlasEnd, site: distantlands.catalog.Site,
      region: Region = Region.Provinces): PlaceAtlasSite =
    PlaceAtlasSite(end, region, site.capacity, site.relicSlots,
      site.startingResources)

  test("the Recent end's site takes the next slot with its stored cards: " +
      "its edifice ruined, its denizens faceup, its relics facedown"):
    val ready = threeSitesAndAnAtlas.storedInAtlas(DeepWoodsSite,
      GreatMarketCard, WizardsConclaveCard).ready
    val after = accepted(run(ready, placing(AtlasEnd.Recent, DeepWoodsSite)))
    val map = after.game.current.map
    assertEquals(map.provinces.last, DeepWoodsSite.id)
    assertEquals(map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(map.sites(DeepWoodsSite.id).denizens, Vector[SiteDenizenState](
      EdificeState(GreatMarketCard.id, EdificeSide.Ruined, Tokens.empty),
      DenizenState(WizardsConclaveCard.id, Orientation.FaceUp, Tokens.empty)))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).tail)

  test("the Forgotten end's site is the Atlas's last"):
    val ready = threeSitesAndAnAtlas.ready
    val after = accepted(run(ready,
      placing(AtlasEnd.Forgotten, GoldenValleySite)))
    assertEquals(after.game.current.map.hintermost, Some(GoldenValleySite.id))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).init)

  test("the placed site takes bandits up to its capacity, its printed " +
      "favor, and the relic deck's top cards for its open relic slots"):
    // Fair Isle: capacity 2, 1 relic slot, 3 favor.
    val ready = threeSitesAndAnAtlas.ready
    val recentFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.reverse.drop(1))))
    assertEquals(recentFirst.game.campaign.atlas.site(AtlasEnd.Recent)
      .map(_.id), Some(FairIsleSite.id))
    val top = recentFirst.game.current.commonCards.relicDeck.head
    val after = accepted(run(recentFirst,
      placing(AtlasEnd.Recent, FairIsleSite)))
    val look = Look(after)
    assertEquals(look.forces(FairIsleSite),
      SiteForces.Occupied(ForceKind.Bandit, 2))
    assertEquals(look.siteTokens(FairIsleSite), Tokens(3, 0))
    assertEquals(after.game.current.map.sites(FairIsleSite.id).relics,
      Vector(RelicState(top, Orientation.FaceDown, Tokens.empty)))
    assertEquals(after.game.current.commonCards.relicDeck,
      recentFirst.game.current.commonCards.relicDeck.tail)

  test("a stored relic fills a slot, so only the open slots are dealt, " +
      "the stored relics first"):
    // Desolate Shore has 2 relic slots; one stored relic leaves one open.
    val ready = threeSitesAndAnAtlas
      .storedInAtlas(DesolateShoreSite, StickyFireCard).ready
    val shoreFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.drop(1))))
    val top = shoreFirst.game.current.commonCards.relicDeck.head
    val after = accepted(run(shoreFirst,
      placing(AtlasEnd.Recent, DesolateShoreSite)))
    assertEquals(Look(after).relicsAt(DesolateShoreSite),
      Vector(StickyFireCard.id, top))
    assert(after.game.current.map.sites(DesolateShoreSite.id).relics
      .forall(_.orientation == Orientation.FaceDown))

  test("a relic deck that runs out leaves the slots it cannot fill open, " +
      "and a site of capacity 0 takes no bandits"):
    val ready = threeSitesAndAnAtlas.relicDeckEmptied
      .relicDeckTop(DowsingSticksCard).ready
    val shoreFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.drop(1))))
    val after = accepted(run(shoreFirst, PlaceAtlasSite(AtlasEnd.Recent,
      Region.Provinces, capacity = 0, relicSlots = 2, Tokens.empty)))
    assertEquals(Look(after).relicsAt(DesolateShoreSite),
      Vector(DowsingSticksCard.id))
    assertEquals(after.game.current.commonCards.relicDeck, Vector.empty)
    assertEquals(Look(after).forces(DesolateShoreSite), SiteForces.Empty)

  test("a placement is refused with no site in the Atlas, into a region " +
      "that does not hold the next slot, and on a full map"):
    val ready = threeSitesAndAnAtlas.ready
    rejected(run(Table.start.mapCutIntoAtlas(8).ready,
      placing(AtlasEnd.Recent, DeepWoodsSite)))
    rejected(run(ready, placing(AtlasEnd.Recent, DeepWoodsSite,
      Region.Hinterland)))
    val emptyAtlas = ready.updateCampaign(_.copy(atlas =
      AtlasState(Vector.empty)))
    rejected(run(emptyAtlas, placing(AtlasEnd.Recent, DeepWoodsSite)))

  test("the pipeline's card inventory holds after a placement"):
    val ready = threeSitesAndAnAtlas.storedInAtlas(DeepWoodsSite,
      GreatMarketCard, WizardsConclaveCard).ready
    val run = accepted(OperationPipeline.run(ready,
      Vector(placing(AtlasEnd.Recent, DeepWoodsSite)),
      OperationPolicy.Permissive, Vector.empty)(Right(_)))
    assertEquals(run.state.game.cardIndex.map(_.ids),
      ready.game.cardIndex.map(_.ids))

  test("a placed site's capacity and relic slots cannot be negative"):
    intercept[IllegalArgumentException](PlaceAtlasSite(AtlasEnd.Recent,
      Region.Cradle, -1, 0, Tokens.empty))
    intercept[IllegalArgumentException](PlaceAtlasSite(AtlasEnd.Recent,
      Region.Cradle, 0, -1, Tokens.empty))
```

**Create** `src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.cards.{DeepWoodsSite, FairIsleSite,
  GoldenValleySite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `AtlasPlacement` reads the site an end of the Atlas gives and copies
  * its printed facts onto the operation that places it (Foundations
  * design, slice 3). The quiet map is cut to three sites: the Atlas holds
  * Deep Woods at its Recent end and Golden Valley at its Forgotten end,
  * and the next slot is in the Provinces. */
class AtlasPlacementSuite extends munit.FunSuite:
  private def threeSitesAndAnAtlas: Table = Table.start.mapCutIntoAtlas(3)

  test("each end gives its site, what it stores, and an operation " +
      "carrying the site's printed capacity, relic slots and resources"):
    val ready = threeSitesAndAnAtlas
      .storedInAtlas(DeepWoodsSite, GreatMarketCard).ready
    val recent = accepted(AtlasPlacement.at(catalog, ready, AtlasEnd.Recent))
    assertEquals(recent.site, DeepWoodsSite.id)
    assertEquals(recent.stored.edifice, Some(GreatMarketCard.id))
    assertEquals(recent.operation, PlaceAtlasSite(AtlasEnd.Recent,
      Region.Provinces, capacity = 3, relicSlots = 0, Tokens.empty))
    val forgotten = accepted(AtlasPlacement.at(catalog, ready,
      AtlasEnd.Forgotten))
    assertEquals(forgotten.site, GoldenValleySite.id)
    assertEquals(forgotten.operation.end, AtlasEnd.Forgotten)

  test("the operation names the region of the next slot and the printed " +
      "favor of a site that has some"):
    // Fair Isle alone in the Atlas, on a map whose Provinces are full.
    val ready = Table.start.mapCutIntoAtlas(5).update(_.updateCampaign(
      campaign => campaign.copy(atlas = AtlasState(campaign.atlas.entries
        .filter(_ == AtlasEntry.StoredSite(FairIsleSite.id, Vector.empty,
          Vector.empty)))))).ready
    assertEquals(accepted(AtlasPlacement.at(catalog, ready,
      AtlasEnd.Recent)).operation, PlaceAtlasSite(AtlasEnd.Recent,
        Region.Hinterland, capacity = 2, relicSlots = 1, Tokens(3, 0)))

  test("a placement is refused on a full map, with no site in the Atlas, " +
      "and for a site the catalog does not print"):
    rejected(AtlasPlacement.at(catalog, Table.start.ready, AtlasEnd.Recent))
    val emptyAtlas = threeSitesAndAnAtlas.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector.empty)))).ready
    rejected(AtlasPlacement.at(catalog, emptyAtlas, AtlasEnd.Recent))
    val unprinted = threeSitesAndAnAtlas.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector(AtlasEntry.StoredSite(
        SiteId("site:nowhere"), Vector.empty, Vector.empty)))))).ready
    assertEquals(rejected(AtlasPlacement.at(catalog, unprinted,
      AtlasEnd.Recent)), OathViolation.InvalidEventOrder(
        "site:nowhere is not a site the catalog prints"))
```

**In** `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`:

Replace

```scala
      .endsWith(".count"))

  test("a draw written before it had a position decodes as a draw from the " +
      "top, and a draw of no position is refused"):
```

with

```scala
      .endsWith(".count"))

  test("a site placement names its Atlas end, its region and the site's " +
      "printed facts, and an unknown end, an unknown region or a negative " +
      "capacity decodes to a typed WireError"):
    val placement = PlaceAtlasSite(AtlasEnd.Recent, Region.Hinterland,
      capacity = 3, relicSlots = 2, Tokens(1, 2))
    val encoded = encodedStreamThatRoundTrips("place-atlas-site",
      Vector(recordedOperation(placement)))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "place-atlas-site", "end" -> "recent",
        "region" -> "hinterland", "capacity" -> 3, "relicSlots" -> 2,
        "resources" -> ujson.Obj("favor" -> 1, "secrets" -> 2)))
    def refusedPath(edit: ujson.Value => Unit): String =
      val injected = ujson.read(encoded).arr
      edit(injected(0)("payload")("step")("ops")(0))
      GameEventWire.decodeStream(ujson.write(injected)) match
        case Left(WireError.InvalidValue(path, _)) => path
        case other => fail(other.toString)
    assert(refusedPath(_("end") = "middle").endsWith(".end"))
    assert(refusedPath(_("region") = "sea").endsWith(".region"))
    assert(refusedPath(_("capacity") = -1).endsWith(".capacity"))
    assert(refusedPath(_("relicSlots") = -1).endsWith(".relicSlots"))

  test("a draw written before it had a position decodes as a draw from the " +
      "top, and a draw of no position is refused"):
```

Then replace

```scala
      LineagePointsScored(LineageId("lineage:red"), 3),
      EdificeBuilt(GreatMarketCard.id, site),
      SuitedReserveFromBox(Suit.Beast,
        Vector(FaeMerchantCard.id)),
```

with

```scala
      LineagePointsScored(LineageId("lineage:red"), 3),
      EdificeBuilt(GreatMarketCard.id, site),
      PlaceAtlasSite(AtlasEnd.Forgotten, Region.Provinces, capacity = 2,
        relicSlots = 1, Tokens(3, 0)),
      SuitedReserveFromBox(Suit.Beast,
        Vector(FaeMerchantCard.id)),
```

**In** `src/test/scala/distantlands/gameplay/walker/HiddenOutcomesSuite.scala`:

Replace

```scala
    assert(!HiddenOutcomes.hides(GainSupply(p1, 1)))

  test("a composite carrying a hidden operation hides at no node of its " +
      "own, and hides anywhere beneath it"):
```

with

```scala
    assert(!HiddenOutcomes.hides(GainSupply(p1, 1)))

  test("a site placed from the Atlas hides its outcome: its stored cards " +
      "and the relics dealt to it"):
    assert(HiddenOutcomes.hides(PlaceAtlasSite(AtlasEnd.Recent,
      Region.Provinces, capacity = 1, relicSlots = 2, Tokens.empty)))

  test("a composite carrying a hidden operation hides at no node of its " +
      "own, and hides anywhere beneath it"):
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.AtlasOperationsSuite distantlands.gameplay.setup.AtlasPlacementSuite distantlands.serialization.WalkerOperationWireSuite distantlands.gameplay.walker.HiddenOutcomesSuite"`
Expected: FAIL. The tests do not compile: `PlaceAtlasSite` and
`AtlasPlacement` do not exist.

- [ ] **Step 3: Implement**

The operation, its application and its wire form, then the builder that
copies a site's printed facts onto it. `CoreOperation` is sealed and the
codec's groups are unions of its cases, so the operation compiles only
once the codec's Chronicle group names it; `OperationApplication` matches
with a wildcard, so its line is the one the compiler will not ask for.
Last, `HiddenOutcomes` lists the placement beside the draws and the dice.

**In** `src/main/scala/distantlands/model/CoreOperations.scala`:

Replace

```scala
    extends PrimitiveOperation

/** Records the result of the Campaign just fought as the public
  * `lastCampaignResult`, replacing the previous one.
```

with

```scala
    extends PrimitiveOperation

/** Takes the site at `end` of the Atlas, with the cards stored on it, and
  * places it in the next slot, which is in `region`, as a bandit site
  * (Foundation I, "populate"): its edifice ruined, its denizens faceup, its
  * stored relics facedown, bandits up to `capacity`, its printed
  * `resources`, and the relic slots its stored relics leave open, of
  * `relicSlots`, dealt facedown from the top of the relic deck while it
  * lasts. Applying an operation reads no catalog, so the walk that builds
  * this copies the site's three printed facts onto it. */
final case class PlaceAtlasSite(end: AtlasEnd, region: Region, capacity: Int,
    relicSlots: Int, resources: Tokens) extends PrimitiveOperation:
  require(capacity >= 0 && relicSlots >= 0,
    "a placed site's capacity and relic slots must be non-negative")

/** Records the result of the Campaign just fought as the public
  * `lastCampaignResult`, replacing the previous one.
```

**Create** `src/main/scala/distantlands/gameplay/operations/AtlasOperations.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.model._

/** Writes of the operations that take sites out of the Atlas (Foundations
  * design, slice 3, "Explore"). */
private[operations] object AtlasOperations:
  import OperationError.InvalidDescription

  /** The site at the operation's end of the Atlas leaves it for the next
    * slot, populated by `SiteState.banditSite`. It takes the bandits its
    * capacity asks for, or as many as the bank holds, and the top cards of
    * the relic deck for its open relic slots, or as many as the deck
    * holds. */
  def placeSite(ready: ReadyGame, placement: PlaceAtlasSite)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    for
      stored <- ready.game.campaign.atlas.site(placement.end).toRight(
        InvalidDescription("the Atlas holds no site to place"))
      _ <- Either.cond(current.map.nextSlot.contains(placement.region), (),
        InvalidDescription(s"the next empty slot is not in the " +
          s"${placement.region.key}"))
      bank <- OperationStateAdapter.quantity(ready,
        Piece.Warbands(ForceKind.Bandit, 1),
        Location.WarbandBank(ForceKind.Bandit))
    yield
      val bandits = bank match
        case AvailableQuantity.Finite(held) =>
          math.min(placement.capacity, held)
        case AvailableQuantity.Unbounded => placement.capacity
      val dealt = current.commonCards.relicDeck.take(SiteState.openRelicSlots(
        placement.relicSlots, stored.relics.size))
      val site = SiteState.banditSite(bandits,
        placement.resources, stored.placedDenizens,
        stored.relics.map(_.id) ++ dealt)
      ready.updateCampaign(campaign => campaign.copy(
          atlas = campaign.atlas.without(stored.id)))
        .updateCurrent(_.copy(
          map = current.map.withSite(placement.region, stored.id, site),
          commonCards = current.commonCards.copy(relicDeck =
            current.commonCards.relicDeck.drop(dealt.size))))
```

**In** `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`:

Replace

```scala
      case (result, EdificeBuilt(edifice, site)) =>
        result.flatMap(ChronicleStateOperations.edificeBuilt(_, edifice, site))
      case (result, RecordCampaignResult(fact)) =>
        result.map(TurnStateOperations.recordCampaignResult(_, fact))
```

with

```scala
      case (result, EdificeBuilt(edifice, site)) =>
        result.flatMap(ChronicleStateOperations.edificeBuilt(_, edifice, site))
      case (result, placement: PlaceAtlasSite) =>
        result.flatMap(AtlasOperations.placeSite(_, placement))
      case (result, RecordCampaignResult(fact)) =>
        result.map(TurnStateOperations.recordCampaignResult(_, fact))
```

**In** `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`:

Replace

```scala
  private type ChronicleOperation = RoleChanged | ChronicleTaskAssigned |
    GrandScepterFromBox | ImperialWarbandsTaken | WarbandsAdded |
    LineagePointsScored | EdificeBuilt | SuitedReserveFromBox

  protected final def encodeOperation(operation: CoreOperation): ujson.Value =
```

with

```scala
  private type ChronicleOperation = RoleChanged | ChronicleTaskAssigned |
    GrandScepterFromBox | ImperialWarbandsTaken | WarbandsAdded |
    LineagePointsScored | EdificeBuilt | SuitedReserveFromBox | PlaceAtlasSite

  protected final def encodeOperation(operation: CoreOperation): ujson.Value =
```

Then replace

```scala
      "kind" -> "suited-reserve-from-box", "suit" -> suit.key,
      "cards" -> cards.map(card => ujson.Str(card.value)))

  private def encodeCardOperation(operation: CardOperation): ujson.Value =
```

with

```scala
      "kind" -> "suited-reserve-from-box", "suit" -> suit.key,
      "cards" -> cards.map(card => ujson.Str(card.value)))
    case PlaceAtlasSite(end, region, capacity, relicSlots, resources) =>
      ujson.Obj("kind" -> "place-atlas-site", "end" -> end.key,
        "region" -> region.key, "capacity" -> capacity,
        "relicSlots" -> relicSlots, "resources" -> encodeTokens(resources))

  private def encodeCardOperation(operation: CardOperation): ujson.Value =
```

Then replace

```scala
        "a suited reserve takes at least one boxed card"))
    yield SuitedReserveFromBox(suit, cards)

  private def positive(value: ujson.Value, name: String, path: String)
```

with

```scala
        "a suited reserve takes at least one boxed card"))
    yield SuitedReserveFromBox(suit, cards)
    case "place-atlas-site" =>
      val end = value("end").str
      for
        from <- AtlasEnd.fromKey(end).toRight(
          InvalidValue(s"$path.end", s"unknown Atlas end '$end'"))
        region <- decodeRegion(value("region").str, s"$path.region")
        capacity <- nonNegative(value, "capacity", path)
        relicSlots <- nonNegative(value, "relicSlots", path)
        resources <- decodeTokens(value("resources"), s"$path.resources")
      yield PlaceAtlasSite(from, region, capacity, relicSlots, resources)

  private def positive(value: ujson.Value, name: String, path: String)
```

Then replace

```scala
      number > 0, number, InvalidValue(s"$path.$name", "must be positive")))

  private def decodeTurnOperation(value: ujson.Value,
      path: String): DecodedOperation =
```

with

```scala
      number > 0, number, InvalidValue(s"$path.$name", "must be positive")))

  private def nonNegative(value: ujson.Value, name: String, path: String)
      : Either[WireError, Int] =
    safeIntField(value.obj, name, path).flatMap(number => Either.cond(
      number >= 0, number,
      InvalidValue(s"$path.$name", "must not be negative")))

  private def decodeTurnOperation(value: ujson.Value,
      path: String): DecodedOperation =
```

**Create** `src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** Builds the placement of an Atlas site (Foundations design, slice 3).
  * `PlaceAtlasSite` is applied with no catalog, so the walk that places a
  * site copies its printed capacity, relic slots and resources onto the
  * operation here. Explore places one site in play; Imperial Maps' choice
  * places one for each empty slot at Setup. */
object AtlasPlacement:
  /** A site about to leave the Atlas: what the Atlas stores of it, and the
    * operation that places it in the next slot. */
  final case class Planned(stored: AtlasEntry.StoredSite,
      operation: PlaceAtlasSite):
    def site: SiteId = stored.id

  /** The site `end` of the Atlas gives now, with the operation that places
    * it. Refused on a full map, with no site in the Atlas, and for a site
    * the catalog does not print. */
  def at(catalog: ExecutableCatalog, ready: ReadyGame, end: AtlasEnd)
      : Either[OathViolation, Planned] = for
    region <- ready.game.current.map.nextSlot.toRight(
      OathViolation.InvalidEventOrder("the map has no empty site slot"))
    stored <- ready.game.campaign.atlas.site(end).toRight(
      OathViolation.InvalidEventOrder("the Atlas holds no site"))
    printed <- catalog.site(stored.id).toRight(
      OathViolation.InvalidEventOrder(
        s"${stored.id.value} is not a site the catalog prints"))
  yield Planned(stored, PlaceAtlasSite(end, region, printed.capacity,
    printed.relicSlots, printed.startingResources))
```

**In** `src/main/scala/distantlands/gameplay/walker/HiddenOutcomes.scala`:

Replace

```scala
package distantlands.gameplay.walker

import distantlands.model.{Draw, Flip, Operation, Orientation, Peek, Reroll,
  Reveal, Roll, Shuffle}

/** Operations that show a player something the chooser cannot see yet:
  * dice, a reroll, a draw, a shuffle, a peek, or a card turned faceup. The
  * legality search stops there, since pruning past one would leak the
  * outcome, and undo never takes one back (undo design, "Barriers").
```

with

```scala
package distantlands.gameplay.walker

import distantlands.model.{Draw, Flip, Operation, Orientation, Peek,
  PlaceAtlasSite, Reroll, Reveal, Roll, Shuffle}

/** Operations that show a player something the chooser cannot see yet:
  * dice, a reroll, a draw, a shuffle, a peek, a card turned faceup, or a
  * site placed from the Atlas, whose stored cards and dealt relics nobody
  * saw before (Foundations design, slice 3). The
  * legality search stops there, since pruning past one would leak the
  * outcome, and undo never takes one back (undo design, "Barriers").
```

Then replace

```scala
  /** Whether `operation` itself shows something the chooser cannot see. */
  private[walker] def hides(operation: Operation): Boolean = operation match
    case _: Roll | _: Reroll | _: Shuffle | _: Draw | _: Peek | _: Reveal =>
      true
    case flip: Flip => flip.orientation == Orientation.FaceUp
    case _ => false
```

with

```scala
  /** Whether `operation` itself shows something the chooser cannot see. */
  private[walker] def hides(operation: Operation): Boolean = operation match
    case _: Roll | _: Reroll | _: Shuffle | _: Draw | _: Peek | _: Reveal |
        _: PlaceAtlasSite => true
    case flip: Flip => flip.orientation == Orientation.FaceUp
    case _ => false
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.AtlasOperationsSuite distantlands.gameplay.setup.AtlasPlacementSuite distantlands.serialization.WalkerOperationWireSuite distantlands.gameplay.walker.*"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`. `CoreOperations.scala` is at 780
lines and `WalkerOperationCodec.scala` at 756, both under 800.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/AtlasOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/setup/AtlasPlacement.scala src/main/scala/distantlands/gameplay/walker/HiddenOutcomes.scala src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/test/scala/distantlands/gameplay/operations/AtlasOperationsSuite.scala src/test/scala/distantlands/gameplay/setup/AtlasPlacementSuite.scala src/test/scala/distantlands/gameplay/walker/HiddenOutcomesSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(atlas): place an Atlas site in the next slot" -m "PlaceAtlasSite, a journaled operation, takes the site at an end of the Atlas with its stored cards and puts it in the map's next slot as a bandit site. It carries the site's printed capacity, relic slots and resources, which AtlasPlacement copies from the catalog, since an operation is applied with none. It shows what nobody saw, so it is a hidden outcome and an undo barrier." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The Explore action

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala`
- Create: `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/FollowUpSupplyWaiver.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/ActOffers.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/StartLines.scala`
- Test: `src/test/scala/distantlands/gameplay/ExploreProcedureSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/FollowUpSupplyWaiverSuite.scala`
- Test: `src/test/scala/distantlands/model/EnumWireKeySuite.scala`
- Test: `src/test/scala/distantlands/model/ProcedureRefSuite.scala`
- Test: `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala`
- Written by their commands: `src/test/resources/notes/declared-keys.txt`,
  `src/test/resources/gamelog/explore.actor.log`, `explore.other.log`,
  `explore-stopped.actor.log`, `explore-stopped.other.log`

**Interfaces:**
- Consumes: Task 2's `AtlasPlacement.at` and `PlaceAtlasSite`; Task 1's
  `AtlasEnd`, `AtlasState.sites` and the table steps; `MapState.nextSlot`
  and `hintermost`; `ModifierChoice.loop`, `RollStep`, `ProcedureNotes.id`,
  `WalkerRollFeedback`, as `RecoverProcedure` uses them.
- Produces:
  - `MajorActionType.Explore` (key `explore`), last in
    `MajorActionType.all`; `PowerWindow.ExploreModifierSelection`
    (`explore.modifier-selection`), `ExploreCost` (`explore.cost`) and
    `ExploreRoll` (`explore.roll`).
  - `ActionRef.Explore` (key `explore`), after `Challenge` in
    `ActionRef.all`.
  - `ExploreProcedure`: `explorePool` (`PoolKey("explore")`),
    `choiceDecisionId` (`"explore.choice"`), `endDecisionId`
    (`"explore.end"`), `difficulty` (4), `continueOption`, `stopOption`,
    `endOption(end: AtlasEnd): DecisionOptionRef.Button`, the note keys
    `exploredRecent`, `exploredForgotten` and `failed`, `noteKeys`,
    `refusal(state, actor): Option[String]` with the four reasons
    `needsWideHorizons`, `noEmptySlot`, `noAtlasSite` and
    `notAtHintermost`, `rollFeedback`, and `build` and `rebuild`, each
    `(ExecutableCatalog, ReadyGame, PlayerId) => Either[OathViolation,
    Operation]`.
  - The Act menu's option `explore`, labelled "Explore", in the major
    band.
  - The Game Log's tag "Explore", its "Continued exploring" line, and the
    lines "Explored Recent Site: Found {site}", "Explored Forgotten Site:
    Found {site}" and "Failed to explore".
  - Task 4 adds the `WhenExplored` node to the tree this task builds.

The tree's shape is the load-bearing part. Read the file's doc comment and
findings 6 and 7 before changing it: the modifier loop stands first though
it asks nothing, the Supply is spent before each roll, the end question is
a branch of its own, and the Explore line reads the site as the map's
Hintermost site, live: the placed site takes the first empty slot going
Hinterward, and Brass Army may park the walk between the placement and the
line.

- [ ] **Step 1: Write the failing tests**

`ExploreProcedureSuite` drives `OathRules` with scripted dice from
`p1AtHintermostOfThree`: the quiet map cut to three sites under Wide
Horizons, p1 at the Hintermost. `ExploreLogScripts` plays an Explore that
succeeds and one that stops through the service for the golden logs, and
`GoldenScripts` registers them. The five suites after them list the
actions by hand and gain Explore. `UndoBarrierPlaySuite` pins finding 13:
once the end is answered, undo is refused.

**Create** `src/test/scala/distantlands/gameplay/ExploreProcedureSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.actions.dice.RerollChoice
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, DeepWoodsSite,
  GoldenValleySite}
import distantlands.gameplay.phases.ActOffers
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.action.StonePortalCard
import distantlands.gameplay.powers.campaign.BrassArmyCard
import distantlands.gameplay.powers.dice.JinxCard
import distantlands.gameplay.powers.recover.SilverCharmCard
import distantlands.gameplay.powers.travel.BrassArmyMoves
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{DeltaPayload, LogNoted, WalkerCompleted,
  WalkerDice, WalkerProcedureRegistry, WalkerRollFeedback,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Look, MenuPlay, Outcomes, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** Explore, the action Wide Horizons gives (Foundations design, slice 3,
  * "Explore"): its gates, its rolls against a difficulty of 4, the end of
  * the Atlas it takes a site from, and where the site and the pawn go. */
class ExploreProcedureSuite extends munit.FunSuite:
  import DefenseDieFace.{Blank, OneShield, TwoShields}

  private val pool = ExploreProcedure.explorePool
  private val blanks: Vector[DieFace] = Vector(Blank, Blank)
  private val oneShield: Vector[DieFace] = Vector(OneShield, Blank)
  private val fourShields: Vector[DieFace] = Vector(TwoShields, TwoShields)

  private def rulesRolling(dice: WalkerDice): OathRules = new OathRules(
    catalog, walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = dice)

  /** p1's Act under Wide Horizons, with full Supply, on a map cut to
    * Ancient City, Broken Peaks and Buried Giant. p1's pawn stands at
    * Buried Giant, the Hintermost site. The Atlas holds Deep Woods at its
    * Recent end, then Desolate Shore, Dunes, Fair Isle, and Golden Valley
    * at its Forgotten end. Every site on the map holds its bandits, so the
    * check after an action places none. */
  private def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled

  /** [[p1AtHintermostOfThree]] with one site, Deep Woods, left in the
    * Atlas. */
  private def p1AtHintermostAtlasOfDeepWoods: ReadyGame =
    // No Table step shortens the Atlas.
    p1AtHintermostOfThree.update(_.updateCampaign(campaign =>
      campaign.copy(atlas = AtlasState(campaign.atlas.entries.take(1)))))
      .ready

  private def started(rules: OathRules, ready: ReadyGame): OathTransition =
    accepted(rules.startWalker(Ready(ready), ActionRef.Explore, p1))

  private def resolved(rules: OathRules, from: OathTransition,
      decisionId: String, option: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(option)))
    next.copy(events = from.events ++ next.events)

  private def parkedDecisionId(at: OathTransition): String =
    EconomyActions.parkedDecision(at.state).decisionId

  private def completed(at: OathTransition): Boolean =
    at.events.contains(WalkerCompleted(ActionRef.Explore))

  /** The lines the Explore itself journaled, by key. */
  private def exploreLines(events: Vector[OathEvent]): Vector[String] =
    events.collect { case LogNoted(_, note, _)
      if ExploreProcedure.noteKeys.exists(_.name == note.key) => note.key }

  private def menuOption(ready: ReadyGame): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.Explore)

  private val exploreButton =
    DecisionOption.Button(DecisionOptionRef.Button("explore"), "Explore")

  private def unavailable(reason: String): DecisionOption =
    DecisionOption.Unavailable(exploreButton, OptionReason.Written(reason))

  test("the Act menu offers Explore for 1 Supply at the Hintermost site " +
      "under Wide Horizons"):
    val ready = p1AtHintermostOfThree.ready
    assertEquals(ExploreProcedure.refusal(ready, p1), None)
    assertEquals(menuOption(ready),
      DecisionOption.Badged(exploreButton, "1 Supply"))

  test("without Wide Horizons the Act menu shows Explore unavailable " +
      "with its reason"):
    val ready = p1AtHintermostOfThree
      .foundation(FoundationNumber.I, FoundationFace.Normal).ready
    assertEquals(menuOption(ready),
      unavailable("Explore needs the Wide Horizons foundation"))
    assertEquals(rejected(new OathRules(catalog).startWalker(
      Ready(MenuPlay.withoutMenu(ready)), ActionRef.Explore, p1)),
      OathViolation.StartUnavailable(ExploreProcedure.needsWideHorizons))

  test("a pawn away from the Hintermost site cannot Explore, and the " +
      "player whose pawn is there can"):
    // p2 stands at Broken Peaks, p1 at Buried Giant, on p2's turn.
    val ready = p1AtHintermostOfThree.turn(p2, Phase.Act).ready
    assertEquals(menuOption(ready),
      unavailable("Your pawn is not at the Hintermost site"))
    assertEquals(ExploreProcedure.refusal(ready, p1), None)

  test("a full map refuses Explore"):
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .pawn(p1, GoldenValleySite).ready
    assertEquals(ready.game.current.map.nextSlot, None)
    assertEquals(menuOption(ready),
      unavailable("The map has no empty site slot"))

  test("an Atlas with no site refuses Explore"):
    val ready = p1AtHintermostOfThree.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector.empty)))).ready
    assertEquals(menuOption(ready), unavailable("The Atlas holds no site"))

  test("without Supply the Act menu says what Explore needs"):
    assertEquals(menuOption(p1AtHintermostOfThree.supply(p1, 0).ready),
      unavailable("Needs 1 Supply; you have 0"))

  test("a refusal that carries no reason of its own reads as Explore's"):
    assertEquals(ActOffers.reason(ActionRef.Explore,
      OathViolation.PawnSiteMissing(p1)), "Cannot explore now")

  test("any role may Explore: a Chancellor and a Citizen at the " +
      "Hintermost site are offered it"):
    val imperial = p1AtHintermostOfThree.imperial(p1, p2)
      .pawn(p2, BuriedGiantSite)
    assert(menuOption(imperial.ready).available)
    assert(menuOption(imperial.turn(p2, Phase.Act).ready).available)

  test("a roll that reaches 4 asks Recent or Forgotten, and Forgotten " +
      "places the Atlas's last site in the next slot with the explorer's " +
      "pawn on it"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(fourShields))
    val rolled = started(rules, ready)
    assertEquals(parkedDecisionId(rolled), ExploreProcedure.endDecisionId)
    assertEquals(EconomyActions.parkedDecision(rolled.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(DecisionOptionRef.Button("recent"),
          "Recent Sites"),
        DecisionOption.Button(DecisionOptionRef.Button("forgotten"),
          "Forgotten Sites")),
        heading = Some("Explore: place a site from")): DecisionQuery)
    val placed = resolved(rules, rolled, ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Forgotten))
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    val look = Look(after)
    assertEquals(after.game.current.map.provinces,
      Vector(BuriedGiantSite.id, GoldenValleySite.id))
    assertEquals(look.pawn(p1), GoldenValleySite.id)
    assertEquals(look.supply(p1), 6)
    // Golden Valley: capacity 3, no relic slot, no printed resources.
    assertEquals(look.forces(GoldenValleySite),
      SiteForces.Occupied(ForceKind.Bandit, 3))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).init)
    assertEquals(exploreLines(placed.events), Vector("explored-forgotten"))
    assertWireRoundTrips(placed.events)

  test("the site and the pawn's placement are one batch: the placement, " +
      "then the pawn's move, which is no Travel"):
    val rules = rulesRolling(scripted(fourShields))
    val placed = resolved(rules,
      started(rules, p1AtHintermostOfThree.ready),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    val batches = placed.events.collect {
      case WalkerStepRecorded(_, DeltaPayload(ops), _) => ops }
    assertEquals(batches.last, Vector[CoreOperation](
      PlaceAtlasSite(AtlasEnd.Recent, Region.Provinces,
        DeepWoodsSite.capacity, DeepWoodsSite.relicSlots,
        DeepWoodsSite.startingResources),
      Move(Piece.Pawn(p1), PositionedLocation(Location.Site(
        BuriedGiantSite.id)), PositionedLocation(Location.Site(
        DeepWoodsSite.id)))))
    assertEquals(exploreLines(placed.events), Vector("explored-recent"))
    assertEquals(Look(placed.state).pawn(p1), DeepWoodsSite.id)

  test("with one site in the Atlas nothing is asked: the roll places it"):
    val rules = rulesRolling(scripted(fourShields))
    val placed = started(rules, p1AtHintermostAtlasOfDeepWoods)
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    assertEquals(after.game.current.map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(Look(after).pawn(p1), DeepWoodsSite.id)
    assertEquals(after.game.campaign.atlas.sites, Vector.empty)
    assertEquals(exploreLines(placed.events), Vector("explored-recent"))

  test("a short roll asks to roll again or stop, showing the dice and " +
      "the difficulty of 4; another Supply adds 2 dice to the same total"):
    val rules = rulesRolling(scripted(oneShield, oneShield, fourShields))
    val short = started(rules, p1AtHintermostOfThree.ready)
    assertEquals(parkedDecisionId(short), ExploreProcedure.choiceDecisionId)
    assertEquals(EconomyActions.parkedDecision(short.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(ExploreProcedure.continueOption,
          "Roll 2 more dice (1 Supply)"),
        DecisionOption.Button(ExploreProcedure.stopOption,
          "Stop exploring")), heading = Some("Explore")): DecisionQuery)
    assertEquals(WalkerProcedureRegistry.rollFeedback(ActionRef.Explore,
      catalog, Outcomes.ready(short.state), p1,
      ExploreProcedure.choiceDecisionId),
      Some(WalkerRollFeedback(pool, target = Some(4))))
    val again = resolved(rules, short, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    // 1 shield, then 1 more: 2 of the 4 needed, so it asks again.
    assertEquals(parkedDecisionId(again), ExploreProcedure.choiceDecisionId)
    assertEquals(Outcomes.ready(again.state).game.current.rollOutcomes(pool)
      .score, 2)
    assertEquals(Look(again.state).supply(p1), 5)
    val reached = resolved(rules, again, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(reached), ExploreProcedure.endDecisionId)
    assertEquals(Look(reached.state).supply(p1), 4)

  test("with no Supply left, rolling again is shown unavailable with " +
      "its reason, and answering it is refused"):
    val rules = rulesRolling(scripted(blanks))
    val broke = started(rules, p1AtHintermostOfThree.supply(p1, 1).ready)
    assertEquals(Look(broke.state).supply(p1), 0)
    assertEquals(EconomyActions.parkedDecision(broke.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Unavailable(
          DecisionOption.Button(ExploreProcedure.continueOption,
            "Roll 2 more dice (1 Supply)"),
          OptionReason.Written("No Supply left")),
        DecisionOption.Button(ExploreProcedure.stopOption,
          "Stop exploring")), heading = Some("Explore")): DecisionQuery)
    assert(rules.resolveWalker(broke.state, p1,
      ExploreProcedure.choiceDecisionId,
      ChooseOneAnswer(ExploreProcedure.continueOption)).isLeft)

  test("stopping fails the Explore: nothing is placed and the Supply " +
      "stays spent"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(blanks))
    val stopped = resolved(rules, started(rules, ready),
      ExploreProcedure.choiceDecisionId, ExploreProcedure.stopOption)
    assert(completed(stopped), stopped.events)
    val after = Outcomes.ready(stopped.state)
    assertEquals(after.game.current.map, ready.game.current.map)
    assertEquals(after.game.campaign.atlas, ready.game.campaign.atlas)
    assertEquals(Look(after).pawn(p1), BuriedGiantSite.id)
    assertEquals(Look(after).supply(p1), 6)
    assertEquals(exploreLines(stopped.events), Vector("failed"))

  test("Jinx rerolls an Explore roll through the generic reroll choice"):
    val jinxed = p1AtHintermostOfThree.adviser(p1, JinxCard)
      .secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(blanks, fourShields))
    val rolled = started(rules, jinxed)
    val reroll = RerollChoice.decisionId(pool, 1)
    assertEquals(reroll, "explore.reroll.1")
    assertEquals(parkedDecisionId(rolled), reroll)
    val rerolled = resolved(rules, rolled, reroll,
      RerollChoice.optionRef(JinxCard.id))
    assertEquals(parkedDecisionId(rerolled), ExploreProcedure.endDecisionId)
    assertEquals(Look(rerolled.state).faceUpSecrets(p1), 0)

  test("Silver Charm is not offered: Explore asks for no modifier and no " +
      "earlier die, though its holder pays Supply for defense dice"):
    val charmed = p1AtHintermostOfThree.relic(p1, SilverCharmCard).ready
    val rules = rulesRolling(scripted(blanks, blanks, fourShields))
    // No modifier decision: the start goes straight to the first roll.
    val short = started(rules, charmed)
    assertEquals(parkedDecisionId(short), ExploreProcedure.choiceDecisionId)
    // The second payment rolls at once, with no Silver Charm question.
    val again = resolved(rules, short, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(again), ExploreProcedure.choiceDecisionId)
    val reached = resolved(rules, again, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(reached), ExploreProcedure.endDecisionId)

  test("a question asked as the pawn is placed comes before the " +
      "Explore's line, which still names the site"):
    // Brass Army asks its holder whenever their pawn moves or is placed.
    val armed = Table.from(p1AtHintermostAtlasOfDeepWoods)
      .relic(p1, BrassArmyCard).secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(fourShields))
    val asked = started(rules, armed)
    assertEquals(parkedDecisionId(asked), BrassArmyMoves.decisionId)
    assertEquals(Look(asked.state).pawn(p1), DeepWoodsSite.id)
    assertEquals(exploreLines(asked.events), Vector.empty)
    val placed = resolved(rules, asked, BrassArmyMoves.decisionId,
      BrassArmyMoves.flip)
    assert(completed(placed), placed.events)
    assertEquals(placed.events.collect {
      case LogNoted(_, LogNote(_, "explored-recent", args, _), _) => args
    }, Vector(Vector[NoteArg](NoteArg.Site(DeepWoodsSite.id))))

  test("after the end is chosen, a question asked as the pawn is placed " +
      "still leaves the chosen site, its stored edifice ruined, and the " +
      "line naming it"):
    // Two sites in the Atlas, so the end is asked; Deep Woods stores the
    // Stone Portal.
    val armed = p1AtHintermostOfThree.update(_.updateCampaign(campaign =>
        campaign.copy(atlas = AtlasState(campaign.atlas.entries.take(2)))))
      .storedInAtlas(DeepWoodsSite, StonePortalCard)
      .relic(p1, BrassArmyCard).secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(fourShields))
    val rolled = started(rules, armed)
    assertEquals(parkedDecisionId(rolled), ExploreProcedure.endDecisionId)
    val asked = resolved(rules, rolled, ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    assertEquals(parkedDecisionId(asked), BrassArmyMoves.decisionId)
    val placed = resolved(rules, asked, BrassArmyMoves.decisionId,
      BrassArmyMoves.flip)
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    assertEquals(after.game.current.map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(after.game.current.map.sites(DeepWoodsSite.id).denizens,
      Vector[SiteDenizenState](EdificeState(StonePortalCard.id,
        EdificeSide.Ruined, Tokens.empty)))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      armed.game.campaign.atlas.sites.map(_.id).tail)
    assertEquals(placed.events.collect {
      case LogNoted(_, LogNote(_, "explored-recent", args, _), _) => args
    }, Vector(Vector[NoteArg](NoteArg.Site(DeepWoodsSite.id))))

  test("a replayed Explore reaches the same map without rolling or " +
      "choosing again"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(blanks, fourShields))
    val short = started(rules, ready)
    val placed = resolved(rules, resolved(rules, short,
      ExploreProcedure.choiceDecisionId, ExploreProcedure.continueOption),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Forgotten))
    // These rules have no dice source, so a replay that rolled would fail.
    val silent = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
    val replayed = placed.events.foldLeft[Either[OathViolation, OathState]](
      Right(Ready(ready)))((state, event) =>
      state.flatMap(silent.evolve(_, event)))
    assertEquals(accepted(replayed), placed.state)
```

**Create** `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala`:

```scala
package distantlands.application.gamelog

import distantlands.application._
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Dice, MenuPlay, Situation, Table}
import distantlands.testkit.Table.p1
import LogScriptSupport.atTable

/** [[LogScripts]]' scripts for Explore, the action Wide Horizons gives
  * (Foundations design, slice 3). */
object ExploreLogScripts:
  import DefenseDieFace.{Blank, TwoShields}

  /** p1's Act under Wide Horizons on a map cut to Ancient City, Broken
    * Peaks and Buried Giant, where p1's pawn stands. The Atlas holds Deep
    * Woods at its Recent end, then Desolate Shore, Dunes, Fair Isle, and
    * Golden Valley at its Forgotten end. */
  private[gamelog] def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite)

  /** An Explore from `table` on stream `name`: a roll of blanks, a second
    * Supply for a roll of four shields, then a site from `end`. */
  private[gamelog] def exploring(name: String, table: Table, end: AtlasEnd)(
      using munit.Location): (GameApplicationService, Situation) =
    val (service, act) = atTable(name, table.banditsRefilled,
      new Dice.ScriptedDefense(Vector(Vector(Blank, Blank),
        Vector(TwoShields, TwoShields))))
    (service, act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.continueOption)
      case park if park.decisionId == ExploreProcedure.endDecisionId =>
        ChooseOneAnswer(ExploreProcedure.endOption(end))
    }.after(MenuPlay.start(p1, ActionRef.Explore)))

  /** p1 explores the Forgotten end and finds Golden Valley. */
  def explore(using munit.Location): Script =
    Script("explore", exploring("explore", p1AtHintermostOfThree,
      AtlasEnd.Forgotten)._1, p1)

  /** p1's dice show blanks, and p1 stops after the first roll. */
  def exploreStopped(using munit.Location): Script =
    val (service, act) = atTable("explore-stopped",
      p1AtHintermostOfThree.banditsRefilled, Dice.blanks)
    act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.stopOption)
    }.after(MenuPlay.start(p1, ActionRef.Explore))
    Script("explore-stopped", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "explore" -> (() => explore),
    "explore-stopped" -> (() => exploreStopped))
```

**In** `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`:

Replace

```scala
  val named: Vector[(String, () => Script)] =
    LogScripts.named ++ CampaignLogScripts.named ++ ChronicleLogScripts.named ++
      ImperialLogScripts.named ++ LaterGameLogScripts.named

  def all: Vector[Script] = named.map(_._2())
```

with

```scala
  val named: Vector[(String, () => Script)] =
    LogScripts.named ++ CampaignLogScripts.named ++ ChronicleLogScripts.named ++
      ImperialLogScripts.named ++ LaterGameLogScripts.named ++
      ExploreLogScripts.named

  def all: Vector[Script] = named.map(_._2())
```

**In** `src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala`:

Replace

```scala

  private val tagged = Set("search", "play-facedown-adviser", "recover",
    "forge", "travel", "muster", "trade", "challenge", "campaign")

  test("an action with a tag opens with it, once; no other action has one"):
```

with

```scala

  private val tagged = Set("search", "play-facedown-adviser", "recover",
    "forge", "travel", "muster", "trade", "challenge", "campaign",
    "explore")

  test("an action with a tag opens with it, once; no other action has one"):
```

Then replace

```scala
      Vector("search", "play-facedown-adviser", "recover", "forge", "travel",
        "take-wealth",
        "muster", "trade", "challenge", "place-banner-resource",
        "negotiation", "campaign", "peek-site-relics", "reveal-owned-relic",
        "move-warbands", "transfer-warbands", "self-exile", "finish-rest",
```

with

```scala
      Vector("search", "play-facedown-adviser", "recover", "forge", "travel",
        "take-wealth",
        "muster", "trade", "challenge", "explore", "place-banner-resource",
        "negotiation", "campaign", "peek-site-relics", "reveal-owned-relic",
        "move-warbands", "transfer-warbands", "self-exile", "finish-rest",
```

**In** `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala`:

Replace

```scala
    assertEquals(ActOffers.all.map(_.ref.wireId), Vector("search", "travel",
      "campaign", "muster", "trade.favor", "trade.secret", "forge",
      "recover", "challenge", "place-banner-resource", "negotiation",
      "play-facedown-adviser", "peek-site-relics", "reveal-owned-relic",
      "move-warbands", "transfer-warbands", "self-exile"))
```

with

```scala
    assertEquals(ActOffers.all.map(_.ref.wireId), Vector("search", "travel",
      "campaign", "muster", "trade.favor", "trade.secret", "forge",
      "recover", "challenge", "explore", "place-banner-resource",
      "negotiation",
      "play-facedown-adviser", "peek-site-relics", "reveal-owned-relic",
      "move-warbands", "transfer-warbands", "self-exile"))
```

**In** `src/test/scala/distantlands/gameplay/FollowUpSupplyWaiverSuite.scala`:

Replace

```scala
    assertEquals(Vector(ActionRef.Campaign, ActionRef.Travel, ActionRef.Search,
      ActionRef.Muster, ActionRef.Trade, ActionRef.Forge, ActionRef.Challenge,
      ActionRef.Recover).map(costWindow), Vector(
      Some(PowerWindow.CampaignCost), Some(PowerWindow.TravelCost),
      Some(PowerWindow.SearchCost), Some(PowerWindow.MusterCost),
      Some(PowerWindow.TradeCost), Some(PowerWindow.ForgeCost),
      Some(PowerWindow.ChallengeCost), Some(PowerWindow.RecoverCost)))

  test("a queued Recover spends no Supply on its roll"):
```

with

```scala
    assertEquals(Vector(ActionRef.Campaign, ActionRef.Travel, ActionRef.Search,
      ActionRef.Muster, ActionRef.Trade, ActionRef.Forge, ActionRef.Challenge,
      ActionRef.Recover, ActionRef.Explore).map(costWindow), Vector(
      Some(PowerWindow.CampaignCost), Some(PowerWindow.TravelCost),
      Some(PowerWindow.SearchCost), Some(PowerWindow.MusterCost),
      Some(PowerWindow.TradeCost), Some(PowerWindow.ForgeCost),
      Some(PowerWindow.ChallengeCost), Some(PowerWindow.RecoverCost),
      Some(PowerWindow.ExploreCost)))

  test("a queued Recover spends no Supply on its roll"):
```

**In** `src/test/scala/distantlands/model/EnumWireKeySuite.scala`:

Replace

```scala
      Vector("cradle", "provinces", "hinterland"))
    assertEquals(MajorActionType.all.map(_.key), Vector("search", "travel",
      "campaign", "muster", "trade", "forge", "recover", "challenge"))

  test("triggered procedure keys stay as journaled walker events spell them"):
```

with

```scala
      Vector("cradle", "provinces", "hinterland"))
    assertEquals(MajorActionType.all.map(_.key), Vector("search", "travel",
      "campaign", "muster", "trade", "forge", "recover", "challenge",
      "explore"))

  test("triggered procedure keys stay as journaled walker events spell them"):
```

**In** `src/test/scala/distantlands/model/ProcedureRefSuite.scala`:

Replace

```scala
    assertEquals(ActionRef.all.map(_.key),
      Vector("search", "play-facedown-adviser", "recover", "forge",
        "travel", "take-wealth", "muster", "trade", "challenge",
        "place-banner-resource", "negotiation", "campaign",
        "peek-site-relics", "reveal-owned-relic", "move-warbands",
```

with

```scala
    assertEquals(ActionRef.all.map(_.key),
      Vector("search", "play-facedown-adviser", "recover", "forge",
        "travel", "take-wealth", "muster", "trade", "challenge", "explore",
        "place-banner-resource", "negotiation", "campaign",
        "peek-site-relics", "reveal-owned-relic", "move-warbands",
```

**In** `src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala`:

Replace

```scala
package distantlands.application

import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.testkit.MenuPlay
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Situation, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Commands
```

with

```scala
package distantlands.application

import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.testkit.MenuPlay
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Dice, Situation, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Commands
```

Then replace

```scala
    nothingToUndo(service, "rest", passed.nextSequence, p1)
    nothingToUndo(service, "rest", passed.nextSequence, p2)
```

with

```scala
    nothingToUndo(service, "rest", passed.nextSequence, p1)
    nothingToUndo(service, "rest", passed.nextSequence, p2)

  test("an Explore's placement shows the site and what it stores, so the " +
      "end chosen before it cannot be taken back"):
    // p1 explores under Wide Horizons from Buried Giant, the Hintermost of
    // three sites; the dice reach 4 at once, and five sites wait in the
    // Atlas.
    val table = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    val (service, repository) = table.service(campaignDice = Dice.steady)
    val act = table.situation(Situation.journaled(service, catalog,
      repository, "explore"))
    val asked = act.parkedAfter(MenuPlay.start(p1, ActionRef.Explore))
    val placed = asked.after(Commands.resolveWalker(p1, TreeDecision(
      ExploreProcedure.endDecisionId, DecisionAnswer.ChooseOneAnswer(
        ExploreProcedure.endOption(AtlasEnd.Forgotten)))))
    assertEquals(placed.ready.game.current.map.inPlay.size, 4)
    nothingToUndo(service, "explore", placed.nextSequence, p1)
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.ExploreProcedureSuite"`
Expected: FAIL. The tests do not compile: `ExploreProcedure`,
`ActionRef.Explore` and `PowerWindow.ExploreCost` do not exist.

- [ ] **Step 3: Implement**

First the vocabulary: the action type, its three windows and the action
reference.

**In** `src/main/scala/distantlands/model/PowerWindow.scala`:

Replace

```scala
  case Recover extends MajorActionType("recover")
  case Challenge extends MajorActionType("challenge")
object MajorActionType:
  val all: Vector[MajorActionType] = Vector(
    Search, Travel, Campaign, Muster, Trade, Forge, Recover, Challenge)

/** A procedure point, not a persistence or relevance category. The stable key
```

with

```scala
  case Recover extends MajorActionType("recover")
  case Challenge extends MajorActionType("challenge")
  /** The action Wide Horizons, altered Foundation I, adds (Foundations
    * design, slice 3). No card prints a modifier for it. */
  case Explore extends MajorActionType("explore")
object MajorActionType:
  val all: Vector[MajorActionType] = Vector(
    Search, Travel, Campaign, Muster, Trade, Forge, Recover, Challenge,
    Explore)

/** A procedure point, not a persistence or relevance category. The stable key
```

Then replace

```scala
  sealed trait RecoverWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = Some(MajorActionType.Recover) }
  sealed trait ChallengeWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = Some(MajorActionType.Challenge) }
  sealed trait OtherWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = None }

```

with

```scala
  sealed trait RecoverWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = Some(MajorActionType.Recover) }
  sealed trait ChallengeWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = Some(MajorActionType.Challenge) }
  sealed trait ExploreWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = Some(MajorActionType.Explore) }
  sealed trait OtherWindow extends PowerWindow { final val associatedMajorAction: Option[MajorActionType] = None }

```

Then replace

```scala
  case object RecoverRoll extends RecoverWindow:
    val key = "recover.roll"
  case object RestStart extends OtherWindow { val key = "rest.start" }
  case object RestReturnFavor extends OtherWindow { val key = "rest.return-favor" }
```

with

```scala
  case object RecoverRoll extends RecoverWindow:
    val key = "recover.roll"
  /** Explore's modifier decision. No card prints an Explore modifier, so
    * nothing is offered there yet (Foundations design, slice 3). */
  case object ExploreModifierSelection extends ExploreWindow:
    val key = "explore.modifier-selection"
  /** The Supply each Explore roll costs. */
  case object ExploreCost extends ExploreWindow { val key = "explore.cost" }
  /** Each Explore roll, before its reroll choice. Explore borrows only
    * Recover's dice: a power that names Recover hooks `RecoverRoll`, never
    * this (Foundations ruling 2). */
  case object ExploreRoll extends ExploreWindow { val key = "explore.roll" }
  case object RestStart extends OtherWindow { val key = "rest.start" }
  case object RestReturnFavor extends OtherWindow { val key = "rest.return-favor" }
```

**In** `src/main/scala/distantlands/model/ProcedureRef.scala`:

Replace

```scala
  case object Trade extends ActionRef { val key = "trade" }
  case object Challenge extends ActionRef { val key = "challenge" }
  case object PlaceBannerResource extends ActionRef:
    val key = "place-banner-resource"
```

with

```scala
  case object Trade extends ActionRef { val key = "trade" }
  case object Challenge extends ActionRef { val key = "challenge" }
  /** Wide Horizons' action (Foundations design, slice 3). */
  case object Explore extends ActionRef { val key = "explore" }
  case object PlaceBannerResource extends ActionRef:
    val key = "place-banner-resource"
```

Then replace

```scala
    */
  val all: Vector[ActionRef] = Vector(Search, PlayFacedownAdviser,
    Recover, Forge, Travel, TakeWealth, Muster, Trade, Challenge,
    PlaceBannerResource, Negotiation, Campaign, PeekSiteRelics,
    RevealOwnedRelic, MoveWarbands,
```

with

```scala
    */
  val all: Vector[ActionRef] = Vector(Search, PlayFacedownAdviser,
    Recover, Forge, Travel, TakeWealth, Muster, Trade, Challenge, Explore,
    PlaceBannerResource, Negotiation, Campaign, PeekSiteRelics,
    RevealOwnedRelic, MoveWarbands,
```

**In** `src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala`:

Replace

```scala
    PowerWindow.ForgeModifierSelection -> MajorActionType.Forge,
    PowerWindow.RecoverModifierSelection -> MajorActionType.Recover,
    PowerWindow.ChallengeModifierSelection -> MajorActionType.Challenge)

  /** The action a modifier-selection window belongs to, or `None` for any
```

with

```scala
    PowerWindow.ForgeModifierSelection -> MajorActionType.Forge,
    PowerWindow.RecoverModifierSelection -> MajorActionType.Recover,
    PowerWindow.ChallengeModifierSelection -> MajorActionType.Challenge,
    PowerWindow.ExploreModifierSelection -> MajorActionType.Explore)

  /** The action a modifier-selection window belongs to, or `None` for any
```

Then the procedure itself.

**Create** `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`:

```scala
package distantlands.gameplay.actions.explore

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.dice.RollStep
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.setup.AtlasPlacement
import distantlands.gameplay.walker.{ModifierChoice, WalkerRollFeedback}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Declared Explore procedure tree for the
  * [[distantlands.gameplay.walker.ProcedureWalker]]: the action Wide
  * Horizons, altered Foundation I, gives every player (Foundations design,
  * slice 3, "Explore").
  *
  * {{{
  * Sequence(
  *   Repeat(ModifierChoice),                  // window = ExploreModifierSelection;
  *                                            // no card prints an Explore
  *                                            // modifier, so nothing is asked
  *   Sequence(
  *     ModifyDicePool("explore", +2),
  *     Repeat(guard = not succeeded && lastChoice != Stop,
  *       Sequence(
  *         SpendSupply(actor, 1),             // window = ExploreCost
  *         Roll("explore", Defense, Automatic), // window = ExploreRoll;
  *                                              // then the reroll choice
  *         Branch(choice when not yet success))), // Decide("explore.choice")
  *     Branch(if success ->
  *       Vector(Branch(Decide("explore.end")), // asked with 2 or more sites
  *         BuildOps(PlaceAtlasSite, pawn Move),
  *         Note(explored)),
  *       else if stopped -> Vector(Note(failed)))))
  * }}}
  *
  * It works like a Recover against a difficulty of 4 (rulebook, "Altered
  * Foundation I: Wide Horizons"): each roll is 2 defense dice for 1 Supply,
  * the shields of every roll count together, and after a short roll the
  * player continues or stops. It borrows only Recover's dice: every window
  * here is Explore's own, so no power that names Recover hooks it
  * (Foundations ruling 2), and the generic reroll choice still follows
  * each roll.
  *
  * The gates are start gates. A resume rebuilds the tree without them,
  * since the Explore itself moves the pawn and may fill the last slot or
  * take the last site.
  */
object ExploreProcedure:
  val explorePool: PoolKey = PoolKey("explore")
  val choiceDecisionId: String = "explore.choice"
  val endDecisionId: String = "explore.end"

  /** Every Explore decision's id starts here. */
  val decisionPrefix: String = "explore."

  /** "Explore works like a Recover action with a difficulty of 4." */
  val difficulty: Int = 4

  private val supplyCost: Int = 1

  /** "Start Explore": the modifier decision's button, were one asked. */
  val startLabel: String = "Start Explore"

  /** The gates' reasons, as the Act menu shows them. */
  val needsWideHorizons: String = "Explore needs the Wide Horizons foundation"
  val noEmptySlot: String = "The map has no empty site slot"
  val noAtlasSite: String = "The Atlas holds no site"
  val notAtHintermost: String = "Your pawn is not at the Hintermost site"

  val continueOption: DecisionOptionRef.Button =
    DecisionOptionRef.Button("continue")
  val stopOption: DecisionOptionRef.Button = DecisionOptionRef.Button("stop")
  val continueLabel: String = "Roll 2 more dice (1 Supply)"
  val stopLabel: String = "Stop exploring"
  /** Why rolling again is unavailable to a player with no Supply. */
  val noSupply: String = "No Supply left"
  /** The heading of the `explore.end` decision. */
  val endHeading: String = "Explore: place a site from"

  /** The two answers of the `explore.end` decision. */
  def endOption(end: AtlasEnd): DecisionOptionRef.Button =
    DecisionOptionRef.Button(end.key)

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Explore))
  /** "Explored Recent Site: Found Deep Woods". */
  val exploredRecent: NoteKey = notes.key("explored-recent", Vector(
    NotePart.Text("Explored Recent Site: Found "), NotePart.Arg(0)))
  /** "Explored Forgotten Site: Found Deep Woods" (Foundations ruling 4). */
  val exploredForgotten: NoteKey = notes.key("explored-forgotten", Vector(
    NotePart.Text("Explored Forgotten Site: Found "), NotePart.Arg(0)))
  /** "Failed to explore": the player answered Stop. */
  val failed: NoteKey = notes.key("failed", Vector(
    NotePart.Text("Failed to explore")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The line of a successful Explore from `end`. */
  def explored(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => exploredRecent
    case AtlasEnd.Forgotten => exploredForgotten

  /** Whether Foundation I lies on Wide Horizons, its altered face. */
  def wideHorizons(state: ReadyGame): Boolean =
    state.game.campaign.foundations.get(FoundationNumber.I)
      .exists(_.face == FoundationFace.Altered)

  /** Why `actor` cannot start an Explore now, the first gate that fails:
    * Foundation I is Wide Horizons, a slot is empty, the Atlas holds a
    * site, and the actor's pawn is at the Hintermost site. Any role may
    * Explore. */
  def refusal(state: ReadyGame, actor: PlayerId): Option[String] =
    val current = state.game.current
    if !wideHorizons(state) then Some(needsWideHorizons)
    else if current.map.nextSlot.isEmpty then Some(noEmptySlot)
    else if state.game.campaign.atlas.sites.isEmpty then Some(noAtlasSite)
    else if !current.map.hintermost.exists(
        current.pawnSite(actor).contains) then Some(notAtHintermost)
    else None

  /** Every park of an Explore but its modifier decision shows the pool
    * rolled so far against the difficulty. */
  def rollFeedback(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, decisionId: String): Option[WalkerRollFeedback] =
    Option.when(decisionId.startsWith(decisionPrefix) &&
        !ModifierPicks.isDecision(decisionId))(
      WalkerRollFeedback(explorePool, target = Some(difficulty)))

  /** A fresh start: the gates, then the tree. */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId)
      : Either[OathViolation, Operation] =
    refusal(state, actor).map(OathViolation.StartUnavailable(_)).toLeft(())
      .flatMap(_ => rebuild(catalog, state, actor))

  /** The same tree for an Explore already started, with no gate. */
  def rebuild(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId)
      : Either[OathViolation, Operation] =
    val rest = exploration(catalog, actor)
    Right(Sequence(Vector(ModifierChoice.loop(actor,
      PowerWindow.ExploreModifierSelection, rest, startLabel), rest)))

  /** The end the explorer chose, or Recent when the Atlas held one site and
    * nothing was asked. */
  def chosenEnd(answered: Vector[Answered]): AtlasEnd =
    answered.reverse.collectFirst {
      case Answered(`endDecisionId`, ChooseOneAnswer(ref), _) => ref
    }.flatMap(ref => AtlasEnd.all.find(endOption(_) == ref))
      .getOrElse(AtlasEnd.Recent)

  /** Tree closes only over the command-stable actor; everything else is
    * read live. */
  private def exploration(catalog: ExecutableCatalog, actor: PlayerId)
      : Operation =
    def succeeded(ready: ReadyGame): Boolean =
      ready.game.current.rollOutcomes.get(explorePool)
        .exists(_.score >= difficulty)

    def stopped(pending: PendingTree): Boolean =
      pending.answered.lastOption.exists:
        case Answered(_, ChooseOneAnswer(selected), _) =>
          selected == stopOption
        case _ => false

    /** Roll again or stop. Without the Supply to roll again that option is
      * shown unavailable, with its reason. */
    def choice(ready: ReadyGame): Decide =
      val again = DecisionOption.Button(continueOption, continueLabel)
      val affordable = ready.game.current.player(actor)
        .exists(_.board.supply.supply >= supplyCost)
      Decide(choiceDecisionId, actor, DecisionQuery.ChooseOne(Vector(
        if affordable then again
        else DecisionOption.Unavailable(again,
          OptionReason.Written(noSupply)),
        DecisionOption.Button(stopOption, stopLabel)),
        heading = Some("Explore")))

    // Payment precedes the roll, so the pipeline rejects a short Supply
    // before any die is asked for.
    val round = Sequence(
      Sequence(Vector(SpendSupply(actor, supplyCost)),
        Some(PowerWindow.ExploreCost)),
      RollStep(explorePool, DiceSpec(DiceKind.Defense), Some(actor),
        Some(PowerWindow.ExploreRoll)),
      Branch((ready, _) =>
        if succeeded(ready) then Vector.empty else Vector(choice(ready))))

    /** Recent or Forgotten, asked while the Atlas holds two sites or more.
      * A branch of its own: once the site is placed the Atlas is shorter,
      * and a walk resumed past this node must still find the nodes after
      * it where they were. */
    val endChoice = Branch((ready, _) =>
      if ready.game.campaign.atlas.sites.sizeIs < 2 then Vector.empty
      else Vector(Decide(endDecisionId, actor, DecisionQuery.ChooseOne(
        AtlasEnd.all.map(end => DecisionOption.Button(endOption(end),
          endLabel(end))), heading = Some(endHeading)))))

    /** The site leaves its end of the Atlas for the next slot, and the
      * explorer's pawn is placed there. This is not a Travel. */
    val place = BuildOps((ready, pending) =>
      for
        planned <- AtlasPlacement.at(catalog, ready,
          chosenEnd(pending.answered))
        from <- ready.game.current.pawnSite(actor)
          .toRight(OathViolation.PawnSiteMissing(actor))
      yield Vector[CoreOperation](planned.operation,
        Move(Piece.Pawn(actor), PositionedLocation(Location.Site(from)),
          PositionedLocation(Location.Site(planned.site)))),
      required = true)

    /** The site the Explore placed: the new Hintermost site, since it took
      * the first empty slot going Hinterward. It is read from the live
      * state, never from the step before: a power may ask its own question
      * as the pawn is placed (Brass Army), and the walk then comes back to
      * the note below in a later command. */
    def placedSite(ready: ReadyGame): Option[SiteId] =
      ready.game.current.map.hintermost

    /** "Explored Forgotten Site: Found Deep Woods". */
    val exploredNote = Note(ProcedureNotes.id(ActionRef.Explore), states =>
      placedSite(states.now).map(site => explored(chosenEnd(states.answered))
        .sourceless(NoteArg.Site(site))))

    val failedNote = Note(ProcedureNotes.id(ActionRef.Explore),
      _ => Some(failed.sourceless()))

    val afterLoop = Branch((ready, pending) =>
      if succeeded(ready) then Vector(endChoice, place, exploredNote)
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty)

    Sequence(
      ModifyDicePool(explorePool, +2),
      Repeat((ready, pending) => !succeeded(ready) && !stopped(pending),
        round),
      afterLoop)

  private def endLabel(end: AtlasEnd): String = end match
    case AtlasEnd.Recent => "Recent Sites"
    case AtlasEnd.Forgotten => "Forgotten Sites"
```

Then its registrations: the walker's registry, the follow-up waiver's cost
window, the note keys, and the Act menu.

**In** `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`:

Replace

```scala
import distantlands.gameplay.actions.challenge.{ChallengeProcedure, PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.campaign.CampaignProcedure
```

with

```scala
import distantlands.gameplay.actions.challenge.{ChallengeProcedure, PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.campaign.CampaignProcedure
```

Then replace

```scala
    *
    * It is `Option` because not every procedure has such a window:
    * `PowerWindow` carries a `*ModifierSelection` case for each of the eight
    * [[distantlands.model.MajorActionType]]s and Take Wealth
    * is not one of them -- its only window is `WakeTakeWealth`, an
    * `OtherWindow` whose `associatedMajorAction` is `None`. `None` here means
```

with

```scala
    *
    * It is `Option` because not every procedure has such a window:
    * `PowerWindow` carries a `*ModifierSelection` case for each
    * [[distantlands.model.MajorActionType]] and Take Wealth
    * is not one of them -- its only window is `WakeTakeWealth`, an
    * `OtherWindow` whose `associatedMajorAction` is `None`. `None` here means
```

Then replace

```scala
      rebuild = ChallengeProcedure.rebuild),

    /** Place Banner Resource is not a major action: it has no fallback kind
      * and, like Take Wealth, no modifier window.
```

with

```scala
      rebuild = ChallengeProcedure.rebuild),

    /** Explore. Its gates are start gates, and its first step after the
      * modifier decision spends the Supply, so it cannot use the
      * playable-option gate. Its rolls are automatic, so no roll parks. */
    ActionRef.Explore -> Entry(
      modifierWindow = Some(PowerWindow.ExploreModifierSelection),
      build = withoutStartArgs(ActionRef.Explore, ExploreProcedure.build),
      rebuild = withoutStartArgs(ActionRef.Explore, ExploreProcedure.rebuild),
      rollFeedback = ExploreProcedure.rollFeedback),

    /** Place Banner Resource is not a major action: it has no fallback kind
      * and, like Take Wealth, no modifier window.
```

**In** `src/main/scala/distantlands/gameplay/walker/FollowUpSupplyWaiver.scala`:

Replace

```scala
      case ActionRef.Challenge => Some(PowerWindow.ChallengeCost)
      case ActionRef.Recover => Some(PowerWindow.RecoverCost)
      case _ => None

```

with

```scala
      case ActionRef.Challenge => Some(PowerWindow.ChallengeCost)
      case ActionRef.Recover => Some(PowerWindow.RecoverCost)
      case ActionRef.Explore => Some(PowerWindow.ExploreCost)
      case _ => None

```

Then replace

```scala
    PowerWindow.TravelCost, PowerWindow.SearchCost, PowerWindow.MusterCost,
    PowerWindow.TradeCost, PowerWindow.ForgeCost, PowerWindow.ChallengeCost,
    PowerWindow.RecoverCost)

  lazy val contributions: Map[PowerWindow, Vector[Contribution]] =
```

with

```scala
    PowerWindow.TravelCost, PowerWindow.SearchCost, PowerWindow.MusterCost,
    PowerWindow.TradeCost, PowerWindow.ForgeCost, PowerWindow.ChallengeCost,
    PowerWindow.RecoverCost, PowerWindow.ExploreCost)

  lazy val contributions: Map[PowerWindow, Vector[Contribution]] =
```

**In** `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala`:

Replace

```scala
  PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.minor.{MinorActionProcedures,
```

with

```scala
  PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.minor.{MinorActionProcedures,
```

Then replace

```scala
      case ActionRef.Trade => TradeProcedure.noteKeys
      case ActionRef.Recover => RecoverProcedure.noteKeys
      case ActionRef.PlayFacedownAdviser => CardPlayProcedure.noteKeys
      case ActionRef.Search => SearchProcedure.noteKeys
```

with

```scala
      case ActionRef.Trade => TradeProcedure.noteKeys
      case ActionRef.Recover => RecoverProcedure.noteKeys
      case ActionRef.Explore => ExploreProcedure.noteKeys
      case ActionRef.PlayFacedownAdviser => CardPlayProcedure.noteKeys
      case ActionRef.Search => SearchProcedure.noteKeys
```

**In** `src/main/scala/distantlands/gameplay/phases/ActOffers.scala`:

Replace

```scala
    major(ActionRef.Recover, "Recover"),
    major(ActionRef.Challenge, "Challenge"),
    minor(ActionRef.PlaceBannerResource,
      "Place favor or secrets on a banner"),
```

with

```scala
    major(ActionRef.Recover, "Recover"),
    major(ActionRef.Challenge, "Challenge"),
    major(ActionRef.Explore, "Explore"),
    minor(ActionRef.PlaceBannerResource,
      "Place favor or secrets on a banner"),
```

Then replace

```scala
      case (ActionRef.Recover, _) => "Cannot recover here"
      case (ActionRef.Challenge, _) => "Cannot challenge now"
      case (ActionRef.PlaceBannerResource, _) => "No banner to place on"
      case (ActionRef.Negotiation, _) => "Cannot negotiate now"
```

with

```scala
      case (ActionRef.Recover, _) => "Cannot recover here"
      case (ActionRef.Challenge, _) => "Cannot challenge now"
      case (ActionRef.Explore, _) => "Cannot explore now"
      case (ActionRef.PlaceBannerResource, _) => "No banner to place on"
      case (ActionRef.Negotiation, _) => "Cannot negotiate now"
```

Last the Game Log. `ProcedureLogs.rules` is an exhaustive match, so the
code does not compile until Explore has its entry. Its tag posts where
Recover's does, at the step that opens the dice pool, and a later payment
writes "Continued exploring".

**In** `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`:

Replace

```scala
  PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.minor.{MinorActionProcedures,
```

with

```scala
  PlaceBannerResourceProcedure}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.actions.forge.ForgeProcedure
import distantlands.gameplay.actions.minor.{MinorActionProcedures,
```

Then replace

```scala
        Told(RecoverProcedure.relicDecisionId,
          Some(RecoverProcedure.recovered))))
    // The payment line tells the assignment, in one of three wordings, so
    // no single key names it.
```

with

```scala
        Told(RecoverProcedure.relicDecisionId,
          Some(RecoverProcedure.recovered))))
    // The explored line names the end the site came from, in one of two
    // wordings, so no single key tells that pick. The continue-or-stop
    // answer is told by the band's "Continued exploring" line and, for a
    // stop, by the failed line.
    case ActionRef.Explore => ProcedureLog(tag = Some("Explore"),
      lines = ActionLineSource.Notes(),
      anchor = starts.opensDicePool, continued = starts.continuedExplore,
      narrated = Vector(Told(ExploreProcedure.choiceDecisionId),
        Told(ExploreProcedure.endDecisionId)))
    // The payment line tells the assignment, in one of three wordings, so
    // no single key names it.
```

**In** `src/main/scala/distantlands/application/gamelog/StartLines.scala`:

Replace

```scala
import distantlands.gameplay.actions.campaign.CampaignSetup
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.recover.RecoverProcedure
import distantlands.gameplay.powerresolver.ModifierPicks
```

with

```scala
import distantlands.gameplay.actions.campaign.CampaignSetup
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.actions.recover.RecoverProcedure
import distantlands.gameplay.powerresolver.ModifierPicks
```

Then replace

```scala
  def continuedRecover(journal: LogJournal, run: Run, at: Int,
      viewer: Option[PlayerId]): Option[Posted] =
    if !run.started.exists(_ != journal.segmentEnd(at)) then None
    else
      val spent = journal.supplySpent(at, run.actor)
      Option.when(spent > 0)(Posted(LogKind.Action,
        Vector(Text("Continued recovering"), LogSpan.Cost(spent))))

  private def completing(journal: LogJournal, at: Int): Boolean =
```

with

```scala
  def continuedRecover(journal: LogJournal, run: Run, at: Int,
      viewer: Option[PlayerId]): Option[Posted] =
    continuedRolling("Continued recovering", journal, run, at)

  /** Explore spends Supply again as Recover does. */
  def continuedExplore(journal: LogJournal, run: Run, at: Int,
      viewer: Option[PlayerId]): Option[Posted] =
    continuedRolling("Continued exploring", journal, run, at)

  private def continuedRolling(words: String, journal: LogJournal, run: Run,
      at: Int): Option[Posted] =
    if !run.started.exists(_ != journal.segmentEnd(at)) then None
    else
      val spent = journal.supplySpent(at, run.actor)
      Option.when(spent > 0)(Posted(LogKind.Action,
        Vector(Text(words), LogSpan.Cost(spent))))

  private def completing(journal: LogJournal, at: Int): Boolean =
```

Then replace

```scala
    paysCostAt(journal, run, at) || completing(journal, at)

  /** Recover: the step that opens its dice pool, after its modifier
    * decision. */
  val opensDicePool: Anchor = (journal, run, at) =>
    opensDicePoolAt(journal, at) || completing(journal, at)
```

with

```scala
    paysCostAt(journal, run, at) || completing(journal, at)

  /** Recover and Explore: the step that opens the action's dice pool,
    * after its modifier decision. */
  val opensDicePool: Anchor = (journal, run, at) =>
    opensDicePoolAt(journal, at) || completing(journal, at)
```

Then replace

```scala
    journal.ops(at).exists {
      case OpStep(pool: ModifyDicePool, _, _) =>
        pool.pool == RecoverProcedure.recoverPool
      case _ => false
    }
```

with

```scala
    journal.ops(at).exists {
      case OpStep(pool: ModifyDicePool, _, _) =>
        pool.pool == RecoverProcedure.recoverPool ||
          pool.pool == ExploreProcedure.explorePool
      case _ => false
    }
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.ExploreProcedureSuite distantlands.gameplay.PhaseMenuSuite distantlands.gameplay.FollowUpSupplyWaiverSuite distantlands.model.EnumWireKeySuite distantlands.model.ProcedureRefSuite distantlands.gameplay.walker.* distantlands.application.UndoBarrierPlaySuite"`
Expected: PASS. The undo test passes only because Task 2 made the
placement a hidden outcome: without it, undo of the end's answer is
accepted and the player may answer the other end having seen this one.

- [ ] **Step 5: Write the note-key snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: FAIL in "every power declares exactly the notes the snapshot
records": the snapshot lacks Explore's three keys.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`

Run: `git diff -- src/test/resources/notes/declared-keys.txt`
Expected: exactly these three added lines, between the
`procedure.chronicle-world` lines and `procedure.finish-rest`, and nothing
removed:

```text
+procedure.explore explored-forgotten = Explored Forgotten Site: Found {0}
+procedure.explore explored-recent = Explored Recent Site: Found {0}
+procedure.explore failed = Failed to explore
```

- [ ] **Step 6: Write the golden log**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL in "explore reads exactly as its golden log for the actor
and another seat" and in the same test of `explore-stopped`: `missing
golden src/test/resources/gamelog/explore.actor.log`, and the same for
`explore-stopped.actor.log`.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Run: `git status --short src/test/resources`
Expected: exactly five lines, the modified snapshot and the four new
goldens:

```text
 M src/test/resources/notes/declared-keys.txt
?? src/test/resources/gamelog/explore-stopped.actor.log
?? src/test/resources/gamelog/explore-stopped.other.log
?? src/test/resources/gamelog/explore.actor.log
?? src/test/resources/gamelog/explore.other.log
```

No other golden may change. Read `explore.actor.log`; it must be exactly:

```text
0.0 round 0 | Round 1
0.1 turn 0 | [player:p1|p1]'s turn
0.2 phase 0 | Act
5.0 section 1 explore@4 | Explore[cost:1 Supply]
7.0 roll 1 explore@4 | Rolled [dice:defense blank blank|blank, blank]
11.0 action 1 explore@4 | Continued exploring[cost:1 Supply]
12.0 roll 1 explore@4 | Rolled [dice:defense two-shields two-shields|two shields, two shields]
17.0 action 1 explore@4 | Explored Forgotten Site: Found [site:site:golden-valley|Golden Valley]
```

A blank roll, a second Supply, four shields, then the site from the
Forgotten end, with no line for the pawn or the bandits: the Explore line
tells the placement. `explore.other.log` must be the same text: nothing in
an Explore is hidden from the other seat. Check it:

Run: `diff src/test/resources/gamelog/explore.actor.log src/test/resources/gamelog/explore.other.log && echo same`
Expected: `same`.

Read `explore-stopped.actor.log`; it must be exactly:

```text
0.0 round 0 | Round 1
0.1 turn 0 | [player:p1|p1]'s turn
0.2 phase 0 | Act
5.0 section 1 explore@4 | Explore[cost:1 Supply]
7.0 roll 1 explore@4 | Rolled [dice:defense blank blank|blank, blank]
11.0 action 1 explore@4 | Failed to explore
```

A blank roll, then Stop: the failed line, and nothing placed. Its other
seat's file is the same text too:

Run: `diff src/test/resources/gamelog/explore-stopped.actor.log src/test/resources/gamelog/explore-stopped.other.log && echo same`
Expected: `same`.

- [ ] **Step 7: Run the Game Log suites**

Run: `./sbtw "testOnly distantlands.application.gamelog.* distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS. `GameLogPropertiesSuite` now finds a script that completes
an Explore, and its tag opens its band.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/main/scala/distantlands/application/gamelog/StartLines.scala src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala src/main/scala/distantlands/gameplay/phases/ActOffers.scala src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala src/main/scala/distantlands/gameplay/walker/FollowUpSupplyWaiver.scala src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/distantlands/model/PowerWindow.scala src/main/scala/distantlands/model/ProcedureRef.scala src/test/resources/gamelog/explore-stopped.actor.log src/test/resources/gamelog/explore-stopped.other.log src/test/resources/gamelog/explore.actor.log src/test/resources/gamelog/explore.other.log src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/application/UndoBarrierPlaySuite.scala src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala src/test/scala/distantlands/application/gamelog/GoldenScripts.scala src/test/scala/distantlands/gameplay/ExploreProcedureSuite.scala src/test/scala/distantlands/gameplay/FollowUpSupplyWaiverSuite.scala src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala src/test/scala/distantlands/model/EnumWireKeySuite.scala src/test/scala/distantlands/model/ProcedureRefSuite.scala
git commit -m "feat(explore): the Explore action under Wide Horizons" -m "A major action offered at the Hintermost site while Foundation I is Wide Horizons, a slot is empty and the Atlas holds a site, to any role. It rolls like a Recover against a difficulty of 4 on its own pool and windows, so no Recover power applies and the generic reroll does. On success the explorer chooses Recent or Forgotten, the site is placed with PlaceAtlasSite and their pawn on it. The Game Log tells it in its own lines." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: WHEN EXPLORED

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/GreatMarketRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/ProvingGroundsRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala`
- Written by its command: `src/test/resources/gamelog/explore-bandit-market.actor.log`,
  `explore-bandit-market.other.log`

**Interfaces:**
- Consumes: Task 3's `ExploreProcedure` tree and `ExploreLogScripts`;
  `PowerWindow.WhenExplored`; `PowerCtx.operation` and `PowerCtx.actor`;
  the setup rules `MarketRule`, `ForgeRule`, `ProvingGrounds`,
  `EmptyGrounds`, `RuinedSetupSteps`.
- Produces:
  - `final case class ExploredSite(site: SiteId) extends Operation`, in
    `distantlands.gameplay.actions.explore`, with `window =
    Some(PowerWindow.WhenExplored)` and no children. The Explore tree walks
    it after its own line.
  - `EdificeSetupSupport.resolvesAt(ctx: PowerCtx, site: SiteId): Boolean`:
    true at Setup; at an `ExploredSite` node, true only for that site.
  - `EdificeSetupSupport.pawnPlacement(ctx)` gives the explorer and the
    explored site at an `ExploredSite` node.
  - Every rule that names `WhenExplored` resolves there only for a face at
    the explored site.

`gameplay/powers/setup` imports `ExploredSite` from `gameplay/actions`,
which is the allowed direction. Do not make the procedure import a power.

- [ ] **Step 1: Write the failing tests**

**Create** `src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{DowsingSticksCard, EldersCard,
  MessengerCard, ScryerCard}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.whenplayed.{DazzleCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{LogNoted, WalkerCompleted, WalkerDice}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** WHEN EXPLORED (rulebook, "Setup / When Explored"): the power of a card
  * an Explore places on the map resolves right after the site is placed,
  * for that site alone. An explored site's edifice is placed ruined, so
  * these are the ruined faces. p1 explores Deep Woods, the Recent end of
  * the Atlas, into the Provinces beside Buried Giant. */
class WhenExploredSuite extends munit.FunSuite:
  /** Stub of WalkerDice: every defense die two shields, so an Explore
    * succeeds on its first roll, and every shuffle reverses its pile. */
  private val shieldsAndReversedShuffles: WalkerDice = new WalkerDice:
    def roll(kind: DiceKind, count: Int)
        : Either[OathViolation, Vector[DieFace]] =
      Right(Vector.fill(count)(DefenseDieFace.TwoShields))
    override def shuffle(count: Int): Either[OathViolation, Vector[Int]] =
      Right((0 until count).reverse.toVector)

  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = shieldsAndReversedShuffles)

  /** p1's Act under Wide Horizons at Buried Giant, the Hintermost of three
    * sites, each holding its bandits. The Atlas holds Deep Woods at its
    * Recent end and four more sites behind it. */
  private def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled

  /** [[p1AtHintermostOfThree]] with `edifice` stored on Deep Woods. */
  private def deepWoodsStoring(edifice: EdificeId): Table =
    p1AtHintermostOfThree.storedInAtlas(DeepWoodsSite, edifice)

  /** p1's Explore of the Recent end, from its start to its end. */
  private def exploredDeepWoods(table: Table): OathTransition =
    val rolled = accepted(rules.startWalker(Ready(table.ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
    assertEquals(Look(placed.state).pawn(p1), DeepWoodsSite.id)
    placed.copy(events = rolled.events ++ placed.events)

  private def said(id: PowerId, keys: Vector[NoteKey],
      events: Vector[OathEvent]): Vector[String] =
    NoteText.said(id, keys, events).map(_.text)

  test("Bandit Market on the explored site places 1 favor on each bandit " +
      "site, the new one too, and burns 1 favor from each bank, after the " +
      "Explore's own line"):
    val table = deepWoodsStoring(GreatMarketCard.id)
    val banks = table.ready.banks.favor
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.cardsAt(DeepWoodsSite), Vector[CardId](GreatMarketCard.id))
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite, DeepWoodsSite)
      .foreach(site => assertEquals(look.siteTokens(site).favor, 1, site.name))
    Suit.all.foreach(suit =>
      assertEquals(look.bankFavor(suit), banks(suit) - 1))
    assertEquals(said(BanditMarket.id, BanditMarket.noteKeys, explored.events),
      Vector("Placed 4 favor on the bandit sites and burned 6 favor from " +
        "the banks."))
    assertEquals(explored.events.collect { case LogNoted(power, note, _) =>
      power.value -> note.key }, Vector(
        "procedure.explore" -> "explored-recent",
        BanditMarket.id.value -> "placed-and-burned"))

  test("a Bandit Market that already stands on the map stays quiet when " +
      "another site is explored"):
    val table = p1AtHintermostOfThree
      .edifice(GreatMarketCard, EdificeSide.Ruined, at = AncientCitySite)
    val banks = table.ready.banks.favor
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).siteTokens(AncientCitySite), Tokens.empty)
    assertEquals(Outcomes.ready(explored.state).banks.favor, banks)
    assertEquals(said(BanditMarket.id, BanditMarket.noteKeys, explored.events),
      Vector.empty)

  test("Broken Forge on the explored site: the explorer places their pawn " +
      "there, so every relic at a site in its region is discarded"):
    val table = deepWoodsStoring(GreatForgeCard.id)
      .relicAt(StickyFireCard, BuriedGiantSite)
      .relicAt(DowsingSticksCard, BrokenPeaksSite)
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    // Buried Giant shares the Provinces with Deep Woods; Broken Peaks is in
    // the Cradle.
    assertEquals(look.relicsAt(BuriedGiantSite), Vector.empty)
    assertEquals(look.relicsAt(BrokenPeaksSite), Vector(DowsingSticksCard.id))
    assertEquals(said(BrokenForge.id, BrokenForge.noteKeys, explored.events),
      Vector(s"Discarded ${StickyFireCard.id.value}."))

  test("a Proving Grounds that already stands where the explorer started " +
      "gives no warbands: the pawn is placed at the explored site"):
    val table = p1AtHintermostOfThree
      .edifice(ProvingGroundsCard, EdificeSide.Intact, at = BuriedGiantSite)
    val before = Look(table.ready).boardWarbands(p1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).boardWarbands(p1), before)

  test("Empty Grounds on the explored site discards every other denizen " +
      "in its region and stays"):
    val table = deepWoodsStoring(ProvingGroundsCard.id)
      .storedInAtlas(DeepWoodsSite, WizardsConclaveCard)
      .denizen(EldersCard, at = BuriedGiantSite)
      .denizen(MessengerCard, at = AncientCitySite)
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.cardsAt(DeepWoodsSite),
      Vector[CardId](ProvingGroundsCard.id))
    assertEquals(look.cardsAt(BuriedGiantSite), Vector.empty)
    assertEquals(look.cardsAt(AncientCitySite), Vector[CardId](MessengerCard.id))

  test("Hall of Mockery on the explored site places 4 favor on the " +
      "People's Favor and burns 1 favor from each bank"):
    val explored = exploredDeepWoods(deepWoodsStoring(HallOfDebateCard.id))
    val look = Look(explored.state)
    assertEquals(look.favorOnPeoplesFavor, 1 + HallOfMockery.Favor)
    assertEquals(said(HallOfMockery.id, HallOfMockery.noteKeys,
      explored.events), Vector("Placed 4 favor on the People's Favor and " +
        "burned 6 favor from the banks."))

  test("Lost Guardians on the explored site places 3 secrets on the " +
      "Darkest Secret"):
    val explored = exploredDeepWoods(deepWoodsStoring(TomeGuardiansCard.id))
    assertEquals(Look(explored.state).secretsOnDarkestSecret,
      1 + LostGuardians.Secrets)

  test("Ruined School on the explored site places 1 secret on each bandit " +
      "site"):
    val explored = exploredDeepWoods(deepWoodsStoring(SchoolOfVinesCard.id))
    val look = Look(explored.state)
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite, DeepWoodsSite)
      .foreach(site => assertEquals(look.siteTokens(site).secrets, 1,
        site.name))
    assertEquals(said(RuinedSchool.id, RuinedSchool.noteKeys, explored.events),
      Vector("Placed 4 secrets on the bandit sites."))

  test("Broken Horn on the explored site buries its region's discard " +
      "pile"):
    val table = deepWoodsStoring(ForestHornCard.id)
      .discarded(Region.Provinces, EldersCard, MessengerCard)
      .discarded(Region.Cradle, ScryerCard)
    val deck = table.ready.game.current.commonCards.worldDeck
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.discardPile(Region.Provinces), Vector.empty)
    assertEquals(look.discardPile(Region.Cradle),
      Vector[WorldCardId](ScryerCard.id))
    assertEquals(Outcomes.ready(explored.state).game.current.commonCards
      .worldDeck.size, deck.size + 2)
    assertEquals(said(BrokenHorn.id, BrokenHorn.noteKeys, explored.events),
      Vector("Buried the Provinces discard pile (2 cards)."))

  test("Broken Clock on the explored site advances the round marker once, " +
      "and on round 8 does nothing"):
    val third = exploredDeepWoods(deepWoodsStoring(GreatClockCard.id)
      .roundMarkerAt(3))
    assertEquals(Outcomes.ready(third.state).game.current.tracks.round, 4)
    assertEquals(said(BrokenClock.id, BrokenClock.noteKeys, third.events),
      Vector("Advanced the round marker to round 4."))
    val last = exploredDeepWoods(deepWoodsStoring(GreatClockCard.id)
      .roundMarkerAt(RoundMarker.LastRound))
    assertEquals(Outcomes.ready(last.state).game.current.tracks.round, 8)
    assertEquals(Outcomes.ready(last.state).game.current.result, None)
    assertEquals(said(BrokenClock.id, BrokenClock.noteKeys, last.events),
      Vector.empty)

  test("Fallen Spire on the explored site swaps the denizens of its " +
      "region's discard pile with the Dispossessed"):
    val table = deepWoodsStoring(GreatSpireCard.id)
      .discarded(Region.Provinces, EldersCard, MessengerCard)
      .dispossessed(DazzleCard, ScryerCard)
    val explored = exploredDeepWoods(table)
    val after = Outcomes.ready(explored.state)
    assertEquals(after.game.current.commonCards.discard(Region.Provinces)
      .toSet, Set[WorldCardId](DazzleCard.id, ScryerCard.id))
    assertEquals(after.game.campaign.dispossessed.toSet,
      Set[WorldCardId](EldersCard.id, MessengerCard.id))
    assertEquals(said(FallenSpire.id, FallenSpire.noteKeys, explored.events),
      Vector("Swapped 2 cards between the Dispossessed and the Provinces " +
        "discard pile, then shuffled the pile."))

  test("an explored site with no edifice resolves nothing"):
    val explored = exploredDeepWoods(p1AtHintermostOfThree)
    assertEquals(explored.events.collect { case LogNoted(power, _, _) =>
      power.value }, Vector("procedure.explore"))
```

**In** `src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala`:

Replace

```scala
import distantlands.application._
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
```

with

```scala
import distantlands.application._
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
```

Then replace

```scala
    Script("explore-stopped", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "explore" -> (() => explore),
    "explore-stopped" -> (() => exploreStopped))
```

with

```scala
    Script("explore-stopped", service, p1)

  /** p1 explores the Recent end and finds Deep Woods, which stores the
    * Great Market: placed ruined, Bandit Market resolves WHEN EXPLORED. */
  def exploreBanditMarket(using munit.Location): Script =
    Script("explore-bandit-market", exploring("explore-bandit-market",
      p1AtHintermostOfThree.storedInAtlas(DeepWoodsSite, GreatMarketCard),
      AtlasEnd.Recent)._1, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "explore" -> (() => explore),
    "explore-stopped" -> (() => exploreStopped),
    "explore-bandit-market" -> (() => exploreBanditMarket))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.WhenExploredSuite"`
Expected: FAIL, nine tests of twelve. Nothing folds the `WhenExplored`
window yet, so no power resolves on the explored site. The three that pass
expect nothing to happen: a Bandit Market already on the map, a Proving
Grounds where the explorer started, and a site with no edifice. They keep
Step 3 honest: a node folded with no filter would fail the first of them.

- [ ] **Step 3: Implement**

The tree gains the node and walks it last.

**In** `src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala`:

Replace

```scala
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Declared Explore procedure tree for the
  * [[distantlands.gameplay.walker.ProcedureWalker]]: the action Wide
```

with

```scala
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** The node the `WhenExplored` window is folded on, right after an Explore
  * places a site: "it triggers when the card is placed on the map by the
  * Explore action" (rulebook, "Setup / When Explored"). It names the placed
  * site, so a power resolves only for a card that lies there, and it walks
  * nothing but what the powers add. */
final case class ExploredSite(site: SiteId) extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.WhenExplored)
  val children: Vector[Operation] = Vector.empty

/** Declared Explore procedure tree for the
  * [[distantlands.gameplay.walker.ProcedureWalker]]: the action Wide
```

Then replace

```scala
  *       Vector(Branch(Decide("explore.end")), // asked with 2 or more sites
  *         BuildOps(PlaceAtlasSite, pawn Move),
  *         Note(explored)),
  *       else if stopped -> Vector(Note(failed)))))
  * }}}
```

with

```scala
  *       Vector(Branch(Decide("explore.end")), // asked with 2 or more sites
  *         BuildOps(PlaceAtlasSite, pawn Move),
  *         Note(explored),
  *         Branch(ExploredSite(site))),        // window = WhenExplored
  *       else if stopped -> Vector(Note(failed)))))
  * }}}
```

Then replace

```scala
      * state, never from the step before: a power may ask its own question
      * as the pawn is placed (Brass Army), and the walk then comes back to
      * the note below in a later command. */
    def placedSite(ready: ReadyGame): Option[SiteId] =
      ready.game.current.map.hintermost
```

with

```scala
      * state, never from the step before: a power may ask its own question
      * as the pawn is placed (Brass Army), and the walk then comes back to
      * the nodes below in a later command. */
    def placedSite(ready: ReadyGame): Option[SiteId] =
      ready.game.current.map.hintermost
```

Then replace

```scala
        .sourceless(NoteArg.Site(site))))

    val failedNote = Note(ProcedureNotes.id(ActionRef.Explore),
      _ => Some(failed.sourceless()))

    val afterLoop = Branch((ready, pending) =>
      if succeeded(ready) then Vector(endChoice, place, exploredNote)
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty)
```

with

```scala
        .sourceless(NoteArg.Site(site))))

    val whenExplored = Branch((ready, _) =>
      placedSite(ready).toVector.map(ExploredSite(_)))

    val failedNote = Note(ProcedureNotes.id(ActionRef.Explore),
      _ => Some(failed.sourceless()))

    val afterLoop = Branch((ready, pending) =>
      if succeeded(ready) then
        Vector(endChoice, place, exploredNote, whenExplored)
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty)
```

**In** `src/main/scala/distantlands/model/PowerWindow.scala`:

Replace

```scala
  case object ChronicleStore extends OtherWindow:
    val key = "chronicle.stars.store"
  /** Where an explored site's effects resolve, once an explore procedure
    * exists to fire it (2026-09-21 Chronicle design, "Setup powers" --
    * out of scope until then). Slice 3's E02/E06/E22 powers already name
    * this window alongside their SETUP window so the same contribution
    * serves both without branching; nothing folds it yet.
    */
  case object WhenExplored extends OtherWindow { val key = "explore.when-explored" }
```

with

```scala
  case object ChronicleStore extends OtherWindow:
    val key = "chronicle.stars.store"
  /** Where a WHEN EXPLORED power resolves: Explore folds it on its
    * `ExploredSite` node, right after it places a site (Foundations design,
    * slice 3). A SETUP power with a WHEN EXPLORED half names this window
    * beside its Setup window, so one contribution serves both.
    */
  case object WhenExplored extends OtherWindow { val key = "explore.when-explored" }
```

The setup rules read the node. `resolvesAt` is the one filter, and each
rule applies it where it finds its face.

**In** `src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala`:

Replace

```scala
package distantlands.gameplay.powers.setup

import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.model._
```

with

```scala
package distantlands.gameplay.powers.setup

import distantlands.gameplay.actions.explore.ExploredSite
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.model._
```

Then replace

```scala
  */
object EdificeSetupSupport:
  /** The player and site of the pawn placement that was just answered,
    * reached at a `PowerWindow.SetupPawnPlaced` fold -- always the LAST
    * entry of `ctx.answered`, since `SetupProcedure`'s tree runs a
    * `BuildOps(placePawn, window = Some(SetupPawnPlaced))` immediately
    * after that player's own pawn `Decide`, with nothing else answered in
    * between.
    *
    * `ctx.actor` is NOT this: it stays `ready.setup.firstPlayer` for
    * the whole Setup walk (`PowerCtx`'s own doc, "not a parked decision's
    * owner"), so a power that placed the actor from `ctx.actor`
    * would attribute every player's placement to the first player alone.
    */
  def pawnPlacement(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    ctx.answered.lastOption.collect:
      case Answered(_, DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Site(site)), by) => by -> site

  /** The keys this object owns, which no one power owns. A power that
```

with

```scala
  */
object EdificeSetupSupport:
  /** Whether a face standing at `site` resolves at the node `ctx` hooks.
    * At Setup it resolves wherever it stands. WHEN EXPLORED "triggers when
    * the card is placed on the map by the Explore action" (rulebook, "Setup
    * / When Explored"), so at an Explore it resolves only at the site that
    * Explore placed, and a face that already stood on the map stays quiet.
    */
  def resolvesAt(ctx: PowerCtx, site: SiteId): Boolean = ctx.operation match
    case ExploredSite(explored) => explored == site
    case _ => true

  /** The player who just placed their pawn, and the site they placed it at.
    *
    * At an Explore it is the explorer, the walk's actor, at the site the
    * Explore placed: "place your pawn on that site".
    *
    * At Setup it is the pawn placement that was just answered, reached at a
    * `PowerWindow.SetupPawnPlaced` fold -- always the LAST entry of
    * `ctx.answered`, since `SetupProcedure`'s tree runs a
    * `BuildOps(placePawn, window = Some(SetupPawnPlaced))` immediately
    * after that player's own pawn `Decide`, with nothing else answered in
    * between. `ctx.actor` is NOT this at Setup: it stays
    * `ready.setup.firstPlayer` for the whole Setup walk (`PowerCtx`'s own
    * doc, "not a parked decision's owner"), so a power that placed the
    * actor from `ctx.actor` would attribute every player's placement to
    * the first player alone.
    */
  def pawnPlacement(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    ctx.operation match
      case ExploredSite(site) => Some(ctx.actor -> site)
      case _ => ctx.answered.lastOption.collect:
        case Answered(_, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Site(site)), by) => by -> site

  /** The keys this object owns, which no one power owns. A power that
```

**In** `src/main/scala/distantlands/gameplay/powers/setup/GreatMarketRules.scala`:

Replace

```scala

/** E02, both faces (2026-09-21 Chronicle design, "Setup powers"). Each names
  * `PowerWindow.WhenExplored` alongside `SetupEnd` so the same contribution
  * will serve WHEN EXPLORED once an explore procedure exists to fire it;
  * nothing folds that window yet, so only the SETUP path runs or is tested.
  */
sealed abstract class MarketRule extends ContributingPower:
```

with

```scala

/** E02, both faces (2026-09-21 Chronicle design, "Setup powers"). Each names
  * `PowerWindow.WhenExplored` alongside `SetupEnd`, so the same contribution
  * serves WHEN EXPLORED: an Explore folds that window for the site it
  * places, where a face resolves only if it stands there
  * (`EdificeSetupSupport.resolvesAt`).
  */
sealed abstract class MarketRule extends ContributingPower:
```

Then replace

```scala
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(edifice, side)

  override def applicable(ctx: PowerCtx): Boolean = at(ctx.state).isDefined

  protected def build(ready: ReadyGame, at: SiteId)
```

with

```scala
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ctx: PowerCtx): Option[SiteId] =
    ctx.state.game.current.edificeSite(edifice, side)
      .filter(EdificeSetupSupport.resolvesAt(ctx, _))

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  protected def build(ready: ReadyGame, at: SiteId)
```

Then replace

```scala

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx.state) match {
      case Some(site) =>
        ops :+ BuildOps((ready, _) => build(ready, site)) :+ note(site)
```

with

```scala

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx) match {
      case Some(site) =>
        ops :+ BuildOps((ready, _) => build(ready, site)) :+ note(site)
```

**In** `src/main/scala/distantlands/gameplay/powers/setup/ProvingGroundsRules.scala`:

Replace

```scala
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(edifice, EdificeSide.Ruined)

  override def applicable(ctx: PowerCtx): Boolean = at(ctx.state).isDefined

  private val notes = NoteSet(id)
```

with

```scala
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ctx: PowerCtx): Option[SiteId] =
    ctx.state.game.current.edificeSite(edifice, EdificeSide.Ruined)
      .filter(EdificeSetupSupport.resolvesAt(ctx, _))

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  private val notes = NoteSet(id)
```

Then replace

```scala

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx.state) match {
      case Some(site) => ops :+
        BuildOps((ready, _) => build(ctx.catalog, ready, site)) :+
```

with

```scala

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx) match {
      case Some(site) => ops :+
        BuildOps((ready, _) => build(ctx.catalog, ready, site)) :+
```

**In** `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala`:

Replace

```scala
/** A ruined edifice face that adds steps at SETUP (END), in the shape of
  * E02's `MarketRule` (catalog batch 9, slice 1). It names `WhenExplored`
  * beside `SetupEnd`, so the same contribution serves an explore procedure
  * once one folds that window. It applies only while its face stands on the
  * map.
  *
  * Most faces are a [[RuinedSetupRule]]: one batch, then a line. Fallen
```

with

```scala
/** A ruined edifice face that adds steps at SETUP (END), in the shape of
  * E02's `MarketRule` (catalog batch 9, slice 1). It names `WhenExplored`
  * beside `SetupEnd`, so the same contribution serves an Explore, which
  * folds that window for the site it places. It applies only while its
  * face stands on the map, and at an Explore only at the explored site
  * (`EdificeSetupSupport.resolvesAt`).
  *
  * Most faces are a [[RuinedSetupRule]]: one batch, then a line. Fallen
```

Then replace

```scala
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  protected def at(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(edifice, EdificeSide.Ruined)

  override def applicable(ctx: PowerCtx): Boolean = at(ctx.state).isDefined

  /** The steps this face adds at its window, for the face at `site`. */
```

with

```scala
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ctx: PowerCtx): Option[SiteId] =
    ctx.state.game.current.edificeSite(edifice, EdificeSide.Ruined)
      .filter(EdificeSetupSupport.resolvesAt(ctx, _))

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The steps this face adds at its window, for the face at `site`. */
```

Then replace

```scala

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx.state) match {
      case Some(site) => ops ++ steps(site)
      case None => ops
```

with

```scala

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx) match {
      case Some(site) => ops ++ steps(site)
      case None => ops
```

**In** `src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala`:

Replace

```scala
  * The advance is [[RoundMarker]]'s. At Setup the marker is on round 1, so
  * it always moves. Its line reads the step: "Advanced the round marker to
  * round 2." The WHEN EXPLORED half waits for an Explore procedure. Great
  * Clock, the intact face, is an action (catalog batch 10, slice 2).
  */
case object BrokenClock extends RuinedSetupRule:
```

with

```scala
  * The advance is [[RoundMarker]]'s. At Setup the marker is on round 1, so
  * it always moves. Its line reads the step: "Advanced the round marker to
  * round 2." WHEN EXPLORED it advances the marker the same way, and on
  * round 8 it does nothing and writes no line. Great Clock, the intact
  * face, is an action (catalog batch 10, slice 2).
  */
case object BrokenClock extends RuinedSetupRule:
```

Then replace

```scala
  *
  * Its line: "Swapped 2 cards between the Dispossessed and the Cradle
  * discard pile, then shuffled the pile." The WHEN EXPLORED half waits for
  * an Explore procedure. Great Spire, the intact face, is an action.
  */
case object FallenSpire extends RuinedSetupSteps:
```

with

```scala
  *
  * Its line: "Swapped 2 cards between the Dispossessed and the Cradle
  * discard pile, then shuffled the pile." WHEN EXPLORED it swaps the pile
  * of the explored site's region the same way. Great Spire, the intact
  * face, is an action.
  */
case object FallenSpire extends RuinedSetupSteps:
```

`ForgeRule` (`GreatForgeRules.scala`) needs no edit: it finds its site
through `pawnPlacement`, which now answers at an Explore.

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.* distantlands.gameplay.ExploreProcedureSuite distantlands.gameplay.setup.*"`
Expected: PASS. The Setup suites pass unchanged: at Setup every face
resolves wherever it stands.

- [ ] **Step 5: Write the golden log**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL in "explore-bandit-market reads exactly as its golden log
for the actor and another seat": `missing golden
src/test/resources/gamelog/explore-bandit-market.actor.log`. Every other
golden passes: the `explore` golden does not change.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Run: `git status --short src/test/resources`
Expected: exactly the two new goldens, and no golden modified:

```text
?? src/test/resources/gamelog/explore-bandit-market.actor.log
?? src/test/resources/gamelog/explore-bandit-market.other.log
```

Read `explore-bandit-market.actor.log`; it must be exactly:

```text
0.0 round 0 | Round 1
0.1 turn 0 | [player:p1|p1]'s turn
0.2 phase 0 | Act
5.0 section 1 explore@4 | Explore[cost:1 Supply]
7.0 roll 1 explore@4 | Rolled [dice:defense blank blank|blank, blank]
11.0 action 1 explore@4 | Continued exploring[cost:1 Supply]
12.0 roll 1 explore@4 | Rolled [dice:defense two-shields two-shields|two shields, two shields]
17.0 action 1 explore@4 | Explored Recent Site: Found [site:site:deep-woods|Deep Woods]
19.0 trigger 1 explore@4 | [card:E02|Bandit Market]: Placed [amount:4 favor] on the bandit sites and burned [amount:6 favor] from the banks.
```

The site's own power line follows the Explore line, as the spec asks, and
names its card. The other seat's file is the same text:

Run: `diff src/test/resources/gamelog/explore-bandit-market.actor.log src/test/resources/gamelog/explore-bandit-market.other.log && echo same`
Expected: `same`.

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/explore/ExploreProcedure.scala src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala src/main/scala/distantlands/gameplay/powers/setup/GreatMarketRules.scala src/main/scala/distantlands/gameplay/powers/setup/ProvingGroundsRules.scala src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala src/main/scala/distantlands/model/PowerWindow.scala src/test/resources/gamelog/explore-bandit-market.actor.log src/test/resources/gamelog/explore-bandit-market.other.log src/test/scala/distantlands/application/gamelog/ExploreLogScripts.scala src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala
git commit -m "feat(explore): resolve WHEN EXPLORED at the explored site" -m "An Explore folds the WhenExplored window on a node naming the site it placed. A setup rule with a WHEN EXPLORED half resolves there only for a face at that site, so a face already on the map stays quiet, and the explorer is the player who placed their pawn." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Hall of Mockery and Lost Guardians move their banner

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala`
- Test: `src/test/scala/distantlands/gameplay/OperationApplicationSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala`
- Written by its command: `src/test/resources/notes/declared-keys.txt`

**Interfaces:**
- Consumes: Task 4's `WhenExploredSuite` and its `exploredDeepWoods`;
  `HallOfMockery`, `LostGuardians`, `RuinedSetupRule`; the banner moves
  of `BoardControlOperations`; `Location.SharedBank`, which holds a banner
  nobody holds (`OperationStateAdapter.quantity`).
- Produces:
  - `Move(Piece.Banner(banner), PlayArea(holder), SharedBank)`: the
    holder's banner returns to the shared bank; refused, as
    `missing-piece`, from a player who does not hold it.
  - `BannerToSharedBank.move(ready, banner): Option[CoreOperation]` and
    `BannerToSharedBank.moved(before, after, banner): Boolean`, in
    `BannerSetupRules.scala`.
  - Hall of Mockery and Lost Guardians move a held banner to the shared
    bank, at Setup's end and when explored. Three new note keys:
    `edifice.e01.ruined placed-and-moved` and `placed-moved-and-burned`,
    and `edifice.e18.ruined placed-and-moved`.

The owner decided this with the plan's review (ruling 8). It changes a
later game's Setup for these two cards: a Chancellor who starts with the
People's Favor (Oathkeeper goal the People) or the Darkest Secret
(Devotion) now loses it at Setup's end when the matching ruined edifice is
on the map, as the card says. In a first game both banners start in the
shared bank, so nothing moves and no line changes.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/OperationApplicationSuite.scala`:

Replace

```scala
        Location.SharedBank, 0)))

  test("moving a pawn that is not at the source site is a missing-piece"):
    // Red's pawn stands at S1, not S3.
```

with

```scala
        Location.SharedBank, 0)))

  test("a held banner returns to the shared bank, and only from its " +
      "holder"):
    // Red holds the People's Favor.
    def returned(from: PlayerId) = Move(Piece.Banner(Banner.PeoplesFavor),
      PositionedLocation(Location.PlayArea(from)),
      PositionedLocation(Location.SharedBank))
    val after = accepted(RawOperationBatch.executeAll(ready,
      Vector(returned(playerId))))
    assertEquals(after.game.current.banners.peoplesFavor.holder, None)
    assertEquals(codes(OperationApplication.validate(ready,
      returned(blueId))), Vector("missing-piece"))

  test("moving a pawn that is not at the source site is a missing-piece"):
    // Red's pawn stands at S1, not S3.
```

**In** `src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala`:

Replace

```scala
  ThreateningRoarCard}
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, SetupProcedure,
  SetupWalkDriver}
import distantlands.gameplay.walker.{WalkerDice, WalkerPowers}
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Unwrap.accepted

```

with

```scala
  ThreateningRoarCard}
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, LaterGameFixture,
  SetupProcedure, SetupWalkDriver}
import distantlands.gameplay.walker.{WalkerDice, WalkerPowers}
import distantlands.model._
import distantlands.testkit.{Look, Situation, Table}
import distantlands.testkit.Unwrap.accepted

```

Then replace

```scala
    assertEquals(said(HallOfMockery, walked.events), Vector.empty)

  // ---- Lost Guardians ----

```

with

```scala
    assertEquals(said(HallOfMockery, walked.events), Vector.empty)

  /** A later game's Setup, run to the first turn, from a Chronicle whose
    * Oathkeeper goal is `goal` and whose first atlas site, placed ruined,
    * stores `edifice`: l1, seated as p1, is the Chancellor, who starts with
    * the goal's banner. */
  private def laterGameSetUpWith(edifice: EdificeId,
      goal: OathkeeperGoal): ReadyGame =
    val record = LaterGameFixture.chronicle(LineageId("l1"),
      LineageId("l2"))
    val chronicle = record.copy(oathkeeperGoal = goal,
      atlasBox = record.atlasBox.updated(0,
        record.atlasBox(0).copy(items = Vector(edifice))))
    Situation.wake(Situation.rules(catalog,
        WalkerPowerCatalog.default(catalog)), chronicle,
      LaterGameFixture.orders(chronicle, PlayerId("p1"))).ready

  test("in a later game Hall of Mockery takes the People's Favor from the " +
      "Chancellor, who started with it, back to the shared bank"):
    val ready = laterGameSetUpWith(HallOfDebateCard.id,
      OathkeeperGoal.ThePeople)
    assertEquals(Look(ready).peoplesFavorHolder, None)
    assertEquals(Look(ready).favorOnPeoplesFavor, 1 + HallOfMockery.Favor)

  test("in a later game Lost Guardians takes the Darkest Secret from the " +
      "Chancellor back to the shared bank"):
    val ready = laterGameSetUpWith(TomeGuardiansCard.id,
      OathkeeperGoal.Devotion)
    assertEquals(Look(ready).darkestSecretHolder, None)
    assertEquals(Look(ready).secretsOnDarkestSecret, 1 + LostGuardians.Secrets)

  test("Hall of Mockery's line says it moved a held banner"):
    val staged = stagedWith(HallOfDebateCard.id)
      .peoplesFavor(Some(PlayerId("p1")), favor = 1).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).peoplesFavorHolder, None)
    assertEquals(said(HallOfMockery, walked.events), Vector(NoteText.Said(
      "placed-moved-and-burned", "Placed 4 favor on the People's Favor, " +
        s"moved it to the shared bank and burned ${Suit.all.size} favor " +
        "from the banks.", covers = false)))
    val unburned = Suit.all.foldLeft(stagedWith(HallOfDebateCard.id)
      .peoplesFavor(Some(PlayerId("p1")), favor = 1))(
      (table, suit) => table.bankFavor(suit, 0)).ready
    assertEquals(said(HallOfMockery, finish(unburned).events),
      Vector(NoteText.Said("placed-and-moved", "Placed 4 favor on the " +
        "People's Favor and moved it to the shared bank.", covers = false)))

  test("Lost Guardians' line says it moved a held banner"):
    val staged = stagedWith(TomeGuardiansCard.id)
      .darkestSecret(Some(PlayerId("p1")), secrets = 1).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).darkestSecretHolder, None)
    assertEquals(said(LostGuardians, walked.events), Vector(NoteText.Said(
      "placed-and-moved", "Placed 3 secrets on the Darkest Secret and " +
        "moved it to the shared bank.", covers = false)))

  // ---- Lost Guardians ----

```

**In** `src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala`:

Replace

```scala
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

```

with

```scala
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

```

Then replace

```scala
        "burned 6 favor from the banks."))

  test("Lost Guardians on the explored site places 3 secrets on the " +
      "Darkest Secret"):
```

with

```scala
        "burned 6 favor from the banks."))

  test("Hall of Mockery on the explored site takes the People's Favor from " +
      "its holder back to the shared bank"):
    val table = deepWoodsStoring(HallOfDebateCard.id)
      .peoplesFavor(Some(p2), favor = 1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).peoplesFavorHolder, None)
    assertEquals(said(HallOfMockery.id, HallOfMockery.noteKeys,
      explored.events), Vector("Placed 4 favor on the People's Favor, moved " +
        "it to the shared bank and burned 6 favor from the banks."))

  test("Lost Guardians on the explored site takes the Darkest Secret from " +
      "its holder, the explorer, back to the shared bank"):
    val table = deepWoodsStoring(TomeGuardiansCard.id)
      .darkestSecret(Some(p1), secrets = 1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).darkestSecretHolder, None)
    assertEquals(said(LostGuardians.id, LostGuardians.noteKeys,
      explored.events), Vector("Placed 3 secrets on the Darkest Secret and " +
        "moved it to the shared bank."))

  test("Lost Guardians on the explored site places 3 secrets on the " +
      "Darkest Secret"):
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.OperationApplicationSuite distantlands.gameplay.powers.setup.RuinedSetupRulesSuite distantlands.gameplay.powers.setup.WhenExploredSuite"`
Expected: FAIL, seven tests of 52: the executor refuses the return as an
incompatible location, both banners stay with their holders at Setup and
when explored, and the new lines are not written.

- [ ] **Step 3: Implement**

The guard and the move accept a banner going back to the shared bank.

**In** `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala`:

Replace

```scala
          else (Vector(InsufficientPieces(Piece.Banner(banner), from, 0)),
            state)
    case _ =>
      (Vector(IncompatibleLocation(Piece.Banner(banner), to)), state)
```

with

```scala
          else (Vector(InsufficientPieces(Piece.Banner(banner), from, 0)),
            state)
    // Back to the shared bank, which holds a banner nobody holds.
    case (Location.PlayArea(source), Location.SharedBank) =>
      val holder = state.bannerHolders.getOrElse(banner, None)
      if !holder.contains(source) then
        (Vector(MissingPiece(Piece.Banner(banner), from)), state)
      else (Vector.empty, state.copy(
        bannerHolders = state.bannerHolders.updated(banner, None)))
    case _ =>
      (Vector(IncompatibleLocation(Piece.Banner(banner), to)), state)
```

Then replace

```scala
        playerState(ready, destination).map(_ =>
          setBannerHolder(ready, banner, destination))
      case _ => Left(IncompatibleLocation(Piece.Banner(banner), to))

  private def setBannerHolder(ready: ReadyGame, banner: Banner,
      player: PlayerId): ReadyGame =
    val current = ready.game.current
    val banners = banner match
      case Banner.PeoplesFavor => current.banners.copy(
        peoplesFavor = current.banners.peoplesFavor.copy(holder = Some(player)))
      case Banner.DarkestSecret => current.banners.copy(
        darkestSecret = current.banners.darkestSecret.copy(holder = Some(player)))
    ready.copy(game = ready.game.copy(current = current.copy(banners = banners)))
```

with

```scala
        playerState(ready, destination).map(_ =>
          setBannerHolder(ready, banner, destination))
      case (Location.PlayArea(_), Location.SharedBank) =>
        Right(holdBanner(ready, banner, None))
      case _ => Left(IncompatibleLocation(Piece.Banner(banner), to))

  private def setBannerHolder(ready: ReadyGame, banner: Banner,
      player: PlayerId): ReadyGame =
    holdBanner(ready, banner, Some(player))

  /** `banner` held by `holder`, or by nobody, in the shared bank. */
  private def holdBanner(ready: ReadyGame, banner: Banner,
      holder: Option[PlayerId]): ReadyGame =
    val current = ready.game.current
    val banners = banner match
      case Banner.PeoplesFavor => current.banners.copy(
        peoplesFavor = current.banners.peoplesFavor.copy(holder = holder))
      case Banner.DarkestSecret => current.banners.copy(
        darkestSecret = current.banners.darkestSecret.copy(holder = holder))
    ready.copy(game = ready.game.copy(current = current.copy(banners = banners)))
```

The two rules move a held banner after placing their tokens, and their
lines say so.

**In** `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala`:

Replace

```scala
  * The favor comes from the shared supply, as Bandit Market's does, and each
  * favor bank burns one with no burner, a bank already empty burning none.
  * The banner is not moved: in the all-Exile first game both banners start
  * in the shared bank, and "players cannot start with it" undoes a
  * later-game setup step the engine does not have (catalog batch 9, "Catalog
  * and roadmap corrections").
  *
  * Its line reads the step: "Placed 4 favor on the People's Favor and burned
  * 6 favor from the banks.", or only the placement when no bank held favor.
  */
case object HallOfMockery extends RuinedSetupRule:
```

with

```scala
  * The favor comes from the shared supply, as Bandit Market's does, and each
  * favor bank burns one with no burner, a bank already empty burning none.
  * A held banner goes back to the shared bank: in a later game the
  * Chancellor may start with it, and when explored anyone may hold it. A
  * banner already in the shared bank stays there.
  *
  * Its line reads the step: "Placed 4 favor on the People's Favor, moved it
  * to the shared bank and burned 6 favor from the banks.", leaving out the
  * move when nobody held the banner and the burn when no bank held favor.
  */
case object HallOfMockery extends RuinedSetupRule:
```

Then replace

```scala
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the People's Favor.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Move(Piece.Favor(Favor), PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor))) +:
      Suit.all.map(bank =>
        Burn.favor(1, PositionedLocation(Location.FavorBank(bank)), None))
```

with

```scala
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the People's Favor.")))
  /** "Placed {4 favor} on the People's Favor, moved it to the shared bank
    * and burned {6 favor} from the banks." */
  val placedMovedAndBurned: NoteKey = notes.key("placed-moved-and-burned",
    Vector(NotePart.Text("Placed "), NotePart.Arg(0),
      NotePart.Text(" on the People's Favor, moved it to the shared bank " +
        "and burned "), NotePart.Arg(1), NotePart.Text(" from the banks.")))
  /** "Placed {4 favor} on the People's Favor and moved it to the shared
    * bank." */
  val placedAndMoved: NoteKey = notes.key("placed-and-moved", Vector(
    NotePart.Text("Placed "), NotePart.Arg(0),
    NotePart.Text(" on the People's Favor and moved it to the shared bank.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    (Move(Piece.Favor(Favor), PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor))) +:
      BannerToSharedBank.move(ready, Banner.PeoplesFavor).toVector) ++
      Suit.all.map(bank =>
        Burn.favor(1, PositionedLocation(Location.FavorBank(bank)), None))
```

Then replace

```scala
      val burned = Suit.all.map(suit => before.banks.favor.getOrElse(suit, 0) -
        after.banks.favor.getOrElse(suit, 0)).sum
      val card = PowerSourceRef.Card(edifice)
      if gained > 0 && burned > 0 then Some(placedAndBurned(card,
        MarketRule.favor(gained), MarketRule.favor(burned)))
      else if gained > 0 then Some(placed(card, MarketRule.favor(gained)))
      else None
    })

/** Lost Guardians (E18, ruined): "SETUP (END): Place 3 secrets on the
  * Darkest Secret and move it to the shared bank. Players cannot start with
  * it."
  *
  * The secrets come from the shared supply. The banner is not moved, for
  * the reason Hall of Mockery's doc gives. Its line reads the step: "Placed
  * 3 secrets on the Darkest Secret."
  */
case object LostGuardians extends RuinedSetupRule:
```

with

```scala
      val burned = Suit.all.map(suit => before.banks.favor.getOrElse(suit, 0) -
        after.banks.favor.getOrElse(suit, 0)).sum
      val moved = BannerToSharedBank.moved(before, after, Banner.PeoplesFavor)
      val card = PowerSourceRef.Card(edifice)
      val (favor, bank) = (MarketRule.favor(gained), MarketRule.favor(burned))
      if gained == 0 then None
      else if moved && burned > 0 then
        Some(placedMovedAndBurned(card, favor, bank))
      else if moved then Some(placedAndMoved(card, favor))
      else if burned > 0 then Some(placedAndBurned(card, favor, bank))
      else Some(placed(card, favor))
    })

/** "Move it to the shared bank", as Hall of Mockery and Lost Guardians
  * print: a held banner leaves its holder for the shared bank, which holds
  * a banner nobody holds. One the shared bank already holds stays. */
object BannerToSharedBank:
  /** The move, while a player holds `banner`. */
  def move(ready: ReadyGame, banner: Banner): Option[CoreOperation] =
    holder(ready, banner).map(player => Move(Piece.Banner(banner),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.SharedBank)))

  /** Whether the step from `before` to `after` returned `banner`. */
  def moved(before: ReadyGame, after: ReadyGame, banner: Banner): Boolean =
    holder(before, banner).nonEmpty && holder(after, banner).isEmpty

  private def holder(ready: ReadyGame, banner: Banner): Option[PlayerId] =
    banner match
      case Banner.PeoplesFavor => ready.game.current.banners.peoplesFavor.holder
      case Banner.DarkestSecret =>
        ready.game.current.banners.darkestSecret.holder

/** Lost Guardians (E18, ruined): "SETUP (END): Place 3 secrets on the
  * Darkest Secret and move it to the shared bank. Players cannot start with
  * it."
  *
  * The secrets come from the shared supply, and a held banner goes back to
  * the shared bank, as Hall of Mockery's does. Its line reads the step:
  * "Placed 3 secrets on the Darkest Secret and moved it to the shared
  * bank.", or only the placement when nobody held the banner.
  */
case object LostGuardians extends RuinedSetupRule:
```

Then replace

```scala
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the Darkest Secret.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Vector(Move(Piece.Secrets(Secrets),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret))))

  protected def note(at: SiteId): Note = Note(id, states => for
```

with

```scala
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the Darkest Secret.")))
  /** "Placed {3 secrets} on the Darkest Secret and moved it to the shared
    * bank." */
  val placedAndMoved: NoteKey = notes.key("placed-and-moved", Vector(
    NotePart.Text("Placed "), NotePart.Arg(0),
    NotePart.Text(" on the Darkest Secret and moved it to the shared bank.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Move(Piece.Secrets(Secrets),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret))) +:
      BannerToSharedBank.move(ready, Banner.DarkestSecret).toVector

  protected def note(at: SiteId): Note = Note(id, states => for
```

Then replace

```scala
      before.game.current.banners.darkestSecret.secrets
    if gained > 0
  yield placed(PowerSourceRef.Card(edifice),
    NoteArg.Amount(gained, NoteUnit.Secret)))
```

with

```scala
      before.game.current.banners.darkestSecret.secrets
    if gained > 0
  yield
    val secrets = NoteArg.Amount(gained, NoteUnit.Secret)
    val card = PowerSourceRef.Card(edifice)
    if BannerToSharedBank.moved(before, after, Banner.DarkestSecret) then
      placedAndMoved(card, secrets)
    else placed(card, secrets))
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.OperationApplicationSuite distantlands.gameplay.powers.setup.* distantlands.gameplay.setup.*"`
Expected: PASS.

- [ ] **Step 5: Write the note-key snapshot**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`

Run: `git diff -- src/test/resources/notes/declared-keys.txt`
Expected: exactly these three added lines, and nothing removed:

```text
+edifice.e01.ruined placed-and-moved = Placed {0} on the People's Favor and moved it to the shared bank.
+edifice.e01.ruined placed-moved-and-burned = Placed {0} on the People's Favor, moved it to the shared bank and burned {1} from the banks.
+edifice.e18.ruined placed-and-moved = Placed {0} on the Darkest Secret and moved it to the shared bank.
```

Run: `./sbtw "testOnly distantlands.application.gamelog.* distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS, with no golden changed: no golden script holds a banner
when one of these faces resolves.

Run: `git status --short src/test/resources`
Expected: only ` M src/test/resources/notes/declared-keys.txt`.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/OperationApplicationSuite.scala src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala src/test/scala/distantlands/gameplay/powers/setup/WhenExploredSuite.scala
git commit -m "feat(setup): Hall of Mockery and Lost Guardians move their banner" -m "Both cards say to move the banner to the shared bank. A held banner now returns there, at Setup's end and when explored, and their lines say so. In a later game a Chancellor who starts with the banner loses it, as the card says." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Explore at the table

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WalkerPanelSupport.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/WalkerChoicePanelRenderSuite.scala`
- Test: `src/test/scala/distantlands/application/ExploreGameSuite.scala` (new)

**Interfaces:**
- Consumes: Tasks 1 to 5; `GameProjector.project`, `LoadedGame`,
  `GameApplicationService`, `Situation.journaled`, `Situation.pawnsAt`,
  `LaterGameFixture`, `Dice.ScriptedDefense`; the frontend's
  `WalkerPanelSupport.renderChooseOnePanel` and `poolLabel`.
- Produces: `WalkerPanelSupport.poolLabel("explore")` is "Shields", and the
  roll summary counts "shields so far" for it. Nothing else is produced:
  the menu, the questions and the map are already drawn from the
  projection (finding 9).

Read `DESIGN.md`'s "Log action bands" before this task, and add no class
and no color.

- [ ] **Step 1: Write the failing frontend test**

**In** `frontend/src/test/scala/distantlands/frontend/WalkerChoicePanelRenderSuite.scala`:

Replace

```scala
    assertEquals(all(panel, ".die-face-defense").size, 2)

  test("an unavailable option is a withheld control that says why in its note"):
    val ui = new RecordingControls()
```

with

```scala
    assertEquals(all(panel, ".die-face-defense").size, 2)

  test("an Explore question draws its dice as shields against the " +
      "difficulty, then the engine's two buttons"):
    val outcome = WalkerRollOutcomeState("explore",
      Vector(DieFace.OneShield, DieFace.Blank), 1, Some(4))
    val query = DecisionQueryState.ChooseOne(Vector(
      DecisionOptionState("button", "continue", "Roll 2 more dice (1 Supply)"),
      DecisionOptionState("button", "stop", "Stop exploring")),
      heading = Some("Explore"))
    val panel = draw(WalkerDecisionState("explore", "explore.choice",
      query = Some(query), rollOutcome = Some(outcome)))
    assertEquals(all(panel, ".walker-roll-totals").map(_.textContent),
      Vector("Shields 1 · need 4"))
    assertEquals(all(panel, ".walker-roll-faces").map(
      _.getAttribute("aria-label")),
      Vector("Rolled shield, blank -- 1 shields so far (need 4)."))
    assertEquals(all(panel, ".die-face-defense").size, 2)
    assertEquals(Dom.one(panel, "h2").textContent, "Explore")
    assertEquals(all(panel, ".walker-choice").map(_.textContent),
      Vector("Roll 2 more dice (1 Supply)", "Stop exploring"))

  test("an unavailable option is a withheld control that says why in its note"):
    val ui = new RecordingControls()
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.WalkerChoicePanelRenderSuite"`
Expected: FAIL in "an Explore question draws its dice as shields against
the difficulty, then the engine's two buttons": the totals read `explore 1
· need 4`, the pool's wire key.

- [ ] **Step 3: Implement**

**In** `frontend/src/main/scala/distantlands/frontend/WalkerPanelSupport.scala`:

Replace

```scala
  private[frontend] def rollOutcomeSummary(outcome: WalkerRollOutcomeState): String =
    val total =
      if outcome.pool == "recover" then s"${outcome.score} shields so far"
      else s"${poolLabel(outcome.pool)} ${outcome.score}"
    s"Rolled ${outcome.faces.map(_.word).mkString(", ")} -- $total" +
```

with

```scala
  private[frontend] def rollOutcomeSummary(outcome: WalkerRollOutcomeState): String =
    val total =
      if poolLabel(outcome.pool) == ShieldsLabel then
        s"${outcome.score} shields so far"
      else s"${poolLabel(outcome.pool)} ${outcome.score}"
    s"Rolled ${outcome.faces.map(_.word).mkString(", ")} -- $total" +
```

Then replace

```scala
      outcome.detail.map(", " + _).mkString + "."

  /** What a roll is called in a total line. A pool key is a wire string, so an
    * unknown one is printed as it arrives rather than guessed at.
    */
  private[frontend] def poolLabel(pool: String): String = pool match
    case "recover" => "Shields"
    case "campaign.attack" => "Attack"
    case "campaign.defense" => "Defense"
```

with

```scala
      outcome.detail.map(", " + _).mkString + "."

  private val ShieldsLabel = "Shields"

  /** What a roll is called in a total line. A pool key is a wire string, so an
    * unknown one is printed as it arrives rather than guessed at.
    */
  private[frontend] def poolLabel(pool: String): String = pool match
    // Explore rolls as a Recover does, against its own difficulty.
    case "recover" | "explore" => ShieldsLabel
    case "campaign.attack" => "Attack"
    case "campaign.defense" => "Defense"
```

- [ ] **Step 4: Run the frontend tests to see them pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.WalkerChoicePanelRenderSuite distantlands.frontend.WalkerDecisionDraftSuite distantlands.frontend.RecoverPanelSuite distantlands.frontend.MenuPanelSuite distantlands.frontend.EmptySiteSlotSuite"`
Expected: PASS.

- [ ] **Step 5: Pin Explore as the table sees it**

This suite pins what Tasks 1 to 5 built, through the projection: the Act
menu's option with its cost or its reason, the roll beside the question,
and the map after an Explore in a game started from a Chronicle and
journaled by the service. In that game the Recent site stores an edifice,
which is the case finding 2 is about. It passes as written; if it fails,
an earlier task is wrong.

**Create** `src/test/scala/distantlands/application/ExploreGameSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.LaterGameFixture
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Dice, MenuPlay, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, present}

/** Explore as a table sees it (Foundations design, slice 3): the Act
  * menu's option, the roll beside its questions, and the map after it, in a
  * game started from a Chronicle and journaled by the service. */
class ExploreGameSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  /** p1's Act menu, parked, under Wide Horizons on a map of three sites
    * with p1's pawn at Buried Giant, the Hintermost. */
  private def actMenuAtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    .phaseMenuParked

  private def exploreOption(table: Table, stream: String) =
    val projected = projector.project(stream, LoadedGame(table.state, 1L), p1)
    present(present(projected.walkerDecision.flatMap(_.query),
      "the Act menu").offeredOptions.find(_.id == "explore"),
      "the menu's Explore option")

  test("the projected Act menu lists Explore among the major actions with " +
      "its cost"):
    val option = exploreOption(actMenuAtHintermostOfThree, "explore-menu")
    assertEquals(option.label, "Explore")
    assertEquals(option.group, Some("major"))
    assertEquals(option.badge, Some("1 Supply"))
    assertEquals(option.state, "available")

  test("without Wide Horizons the projected option is unavailable and " +
      "says why"):
    val option = exploreOption(Table.start.phaseMenuParked, "explore-refused")
    assertEquals(option.group, Some("major"))
    assertEquals(option.state, "unavailable")
    assertEquals(option.reason,
      Some("Explore needs the Wide Horizons foundation"))

  /** A later game's Chronicle under Wide Horizons: l1 is the Chancellor of
    * an Empire of three sites, and five sites wait in the Atlas, each with
    * what the Chronicle stores on it. */
  private val wideHorizonsChronicle: Chronicle =
    LaterGameFixture.chronicle(LineageId("l1"), 3).copy(foundations = Map(
      FoundationNumber.I -> FoundationState(FoundationFace.Altered,
        Set.empty)))
  private val hintermost = wideHorizonsChronicle.world.last.site
  private val recent = wideHorizonsChronicle.atlasBox.head

  /** The game set up from [[wideHorizonsChronicle]] on stream `name`, every
    * pawn placed at the Hintermost site, as the Chancellor's Act begins.
    * The defense dice roll blanks, then two shields each. */
  private def chancellorActs(name: String)
      : (GameApplicationService, Situation) =
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = new Dice.ScriptedDefense(Vector(
        Vector(DefenseDieFace.Blank, DefenseDieFace.Blank),
        Vector(DefenseDieFace.TwoShields, DefenseDieFace.TwoShields))))
    val woken = Situation.wake(Situation.journaled(service, catalog,
        repository, name).withAnswers(Situation.pawnsAt(
          Vector.fill(3)(hintermost))),
      wideHorizonsChronicle, LaterGameFixture.orders(wideHorizonsChronicle,
        p1))
    (service, woken.endingWake(p1))

  test("in a game started under Wide Horizons, the Chancellor's short " +
      "roll shows the Explore dice against 4 beside the question"):
    val (service, act) = chancellorActs("explore-short-roll")
    val short = act.parkedAfter(MenuPlay.start(p1, ActionRef.Explore))
    val decision = present(projector.project("explore-short-roll",
      present(accepted(service.load("explore-short-roll")), "the game"), p1)
      .walkerDecision, "the continue-or-stop question")
    assertEquals(decision.action, "explore")
    assertEquals(decision.decisionId, ExploreProcedure.choiceDecisionId)
    assertEquals(decision.rollOutcome.map(roll =>
      (roll.pool, roll.score, roll.target)), Some(("explore", 0, Some(4))))
    assertEquals(present(decision.query, "its query").offeredOptions
      .map(option => option.id -> option.label), Vector(
        "continue" -> "Roll 2 more dice (1 Supply)",
        "stop" -> "Stop exploring"))
    assertEquals(short.ready.game.current.map.inPlay.size, 3)

  test("the explored site fills the empty frame: the projected region " +
      "gains the site and loses a slot, and the journal replays to the " +
      "same game"):
    val (service, act) = chancellorActs("explore-game")
    val before = projector.project("explore-game",
      present(accepted(service.load("explore-game")), "the game"), p1).world
    assertEquals(before.map(_.sites.size), Vector(2, 1, 0))
    assertEquals(before.map(_.emptySlots), Vector(0, 2, 3))
    val explored = act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.continueOption)
      case park if park.decisionId == ExploreProcedure.endDecisionId =>
        ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))
    }.after(MenuPlay.start(p1, ActionRef.Explore))
    val loaded = present(accepted(service.load("explore-game")), "the game")
    // The service folds its journal to the state the commands reached.
    assertEquals(loaded.state, explored.state)
    val game = explored.ready.game
    assertEquals(game.current.map.provinces, Vector(hintermost, recent.site))
    assertEquals(game.current.pawnSite(p1), Some(recent.site))
    // The Chronicle stores an edifice on the site: it is placed ruined.
    val stored = recent.items.collect { case id: EdificeId => id }
    assert(stored.nonEmpty)
    assertEquals(game.current.map.sites(recent.site).denizens.collect {
      case EdificeState(id, side, _) => id -> side
    }, stored.map(_ -> EdificeSide.Ruined))
    assert(game.cardIndex.isRight, game.cardIndex)
    val world = projector.project("explore-game", loaded, p1).world
    assertEquals(world.map(_.sites.size), Vector(2, 2, 0))
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))
    assertEquals(world(1).sites.last.siteId, recent.site.value)
```

Run: `./sbtw "testOnly distantlands.application.ExploreGameSuite"`
Expected: PASS, four tests.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/WalkerPanelSupport.scala frontend/src/test/scala/distantlands/frontend/WalkerChoicePanelRenderSuite.scala src/test/scala/distantlands/application/ExploreGameSuite.scala
git commit -m "feat(table): show Explore's dice as shields, and pin Explore at the table" -m "The panel words the Explore pool as it words Recover's. A suite plays Explore in a game started from a Chronicle and reads the Act menu's option, the roll beside the question and the map from the projection." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: The Chronicle Phase after an Explore

**Files:**
- Test: `src/test/scala/distantlands/gameplay/chronicle/ExploredSiteChronicleSuite.scala` (new)

**Interfaces:**
- Consumes: Tasks 1 to 5; slice 3a's Setup With No Empire;
  `EvolvedChronicleFixture.gameWonFrom`, `chronicleAnsweredAfter` and
  `chronicleJournaledBy`; `ChronicleExtraction.layout`;
  `ChronicleAccounting.knownAndUnique` and `conserves`.
- Produces: nothing new. The Chronicle Phase reads the map and the Atlas
  as they are (finding 12), so this task is tests only.

- [ ] **Step 1: Write the tests**

A game set up with no Empire erodes Foundation I to Wide Horizons, so its
Exiles may Explore. p1 explores the Forgotten end, wins as the Usurper,
and every Chronicle task is answered.

**Create** `src/test/scala/distantlands/gameplay/chronicle/ExploredSiteChronicleSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{MenuPlay, Outcomes, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** A game in which a site was explored, through the Chronicle Phase and
  * into the next game (Foundations design, slice 3, "Tests"). The game is
  * set up with no Empire on a former Empire of three sites, which erodes
  * Foundation I to Wide Horizons. p1 explores the Forgotten end from the
  * Hintermost site, then wakes as the Usurper, wins, and every task is
  * answered plainly. */
class ExploredSiteChronicleSuite extends munit.FunSuite:
  import EvolvedChronicleFixture.rules

  /** A Chronicle whose Empire of three sites nobody at the table belongs
    * to. */
  private val absentEmpireChronicle: Chronicle =
    LaterGameFixture.chronicle(LineageId("l9"), Vector(LineageId("l8")), 3)

  private val forgotten = absentEmpireChronicle.atlasBox.last

  /** The no-Empire game after Setup, in p1's Act with p1's pawn at the
    * Hintermost of its three sites. */
  private lazy val exilesAtTheFrontier: ReadyGame =
    val setUp = Situation.wake(Situation.rules(catalog),
      absentEmpireChronicle,
      LaterGameFixture.orders(absentEmpireChronicle, p1)).ready
    Table.from(MenuPlay.withoutMenu(setUp)).turn(p1, Phase.Act)
      .pawn(p1, absentEmpireChronicle.world.last.site).ready

  /** p1's Explore of the Forgotten end, rolled as four shields. */
  private lazy val explored: ReadyGame =
    val rolling = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.shields)
    val rolled = accepted(rolling.startWalker(Ready(exilesAtTheFrontier),
      ActionRef.Explore, p1))
    val placed = accepted(rolling.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Forgotten))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
    Outcomes.ready(placed.state)

  /** The game after the Explore, as p1's Wake begins with the Usurper side
    * of the title. */
  private lazy val usurperWakesAfterExploring: OathState =
    Table.from(MenuPlay.withoutMenu(explored)).turn(p1, Phase.Wake)
      .oathkeeper(Some(p1), TitleSide.Usurper).state

  private lazy val won = EvolvedChronicleFixture.gameWonFrom(
    usurperWakesAfterExploring)
  private lazy val done = EvolvedChronicleFixture.chronicleAnsweredAfter(won)
  private lazy val journaled = EvolvedChronicleFixture.chronicleJournaledBy(
    done)

  test("Setup With No Empire erodes Foundation I, so its Exiles may " +
      "Explore, and the explored site joins the map with its stored cards"):
    assertEquals(ExploreProcedure.refusal(exilesAtTheFrontier, p1), None)
    val map = explored.game.current.map
    assertEquals(map.hinterward,
      absentEmpireChronicle.world.map(_.site) :+ forgotten.site)
    assertEquals(map.sites(forgotten.site).denizens.map(_.id),
      forgotten.items.collect {
        case id: EdificeId => id: CardId
        case id: DenizenId => id: CardId
      })
    assertEquals(explored.game.campaign.atlas.sites.map(_.id),
      absentEmpireChronicle.atlasBox.init.map(_.site))

  test("the Chronicle Phase stores the explored site like any other: " +
      "every card is conserved, and the journal replays"):
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, journaled),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(absentEmpireChronicle,
      journaled), Right(()))
    assertEquals(accepted((won.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](
        Right(usurperWakesAfterExploring))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)

  test("the Stars keep or archive the explored site with the map's other " +
      "sites, ahead of the Atlas sites nobody explored, which keep their " +
      "order"):
    val layout = accepted(ChronicleExtraction.layout(
      Outcomes.ready(done.state)))
    val onMap = absentEmpireChronicle.world.map(_.site) :+ forgotten.site
    assertEquals((layout.empire ++ layout.rivals).toSet, onMap.toSet)
    assert((layout.empire ++ layout.rivals).contains(forgotten.site))
    assertEquals(journaled.world.map(_.site), layout.empire)
    assertEquals(journaled.atlasBox.map(_.site),
      layout.rivals ++ absentEmpireChronicle.atlasBox.init.map(_.site))
    assertEquals((journaled.world ++ journaled.atlasBox).map(_.site).toSet,
      (absentEmpireChronicle.world ++ absentEmpireChronicle.atlasBox)
        .map(_.site).toSet)

  test("the next game starts from the journaled Chronicle under Wide " +
      "Horizons with the explored site in its Atlas or its Empire"):
    val orders = LaterGameFixture.orders(journaled, p1)
    val next = accepted(GameStartRules.evolve(catalog, journaled, orders))
    assertEquals(next.game.campaign.foundations(FoundationNumber.I).face,
      FoundationFace.Altered)
    assertEquals(next.game.current.map.inPlay, journaled.world.map(_.site))
    val everywhere = next.game.current.map.inPlay ++
      next.game.campaign.atlas.sites.map(_.id)
    assert(everywhere.contains(forgotten.site), everywhere)
    assertEquals(everywhere.size, 8)
```

- [ ] **Step 2: Run them**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ExploredSiteChronicleSuite distantlands.gameplay.chronicle.EmptySlotChronicleSuite"`
Expected: PASS. If a test of the new suite fails, do not weaken it: the
explored site is not stored as the map's other sites are, which is a
defect in Task 1 or Task 2. Stop and report what it shows.

- [ ] **Step 3: Commit**

```bash
git add src/test/scala/distantlands/gameplay/chronicle/ExploredSiteChronicleSuite.scala
git commit -m "test(chronicle): pin the Chronicle Phase after an Explore" -m "A site explored in play is kept or archived with the map's other sites, every card is conserved, the journal replays, and the next game starts from the record." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Docs and verification

**Files:**
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/architecture/gameplay-modules.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

**Interfaces:**
- Consumes: everything Tasks 1 to 7 built; nothing new is produced.

Docs are plain prose. Two of these files hold very long lines (a table row,
a numbered item), so their blocks below are parts of a line, not whole
lines: replace exactly the text shown and leave the rest of the line. The
roadmap deletes finished work outright: no checked box and no "landed"
note.

- [ ] **Step 1: The architecture docs, the traceability table, the
  ambiguities note and the roadmap**

The traceability table's new row goes between the Recover row and the
Challenge row, under "Major and minor actions": its block replaces the end
of the Recover row and the start of the Challenge row with the same text
around the new row.

**In** `docs/architecture/game-setup.md`:

Replace

~~~markdown
each region its `emptySlots`, which the table draws as empty frames. No
action fills an empty slot yet: Explore is slice 3b of the
[Foundations design](../superpowers/specs/2026-11-11-foundations-design.md).
~~~

with

~~~markdown
each region its `emptySlots`, which the table draws as empty frames. In
play, under Wide Horizons, the Explore action fills the next empty slot:
`PlaceAtlasSite` takes the site at the Recent or the Forgotten end of the
atlas and populates it as game start populates an atlas site
(`SiteState.banditSite`), and the `WhenExplored` window then resolves the
site's ruined edifice
([Foundations design](../superpowers/specs/2026-11-11-foundations-design.md),
slice 3).
~~~

Then replace

~~~markdown
"Setup for Later Games". It excludes Legacy selection/effects, the
Explore action, and the altered Foundation faces no
slice of the
~~~

with

~~~markdown
"Setup for Later Games". It excludes Legacy selection/effects and the
altered Foundation faces no slice of the
~~~

**In** `docs/architecture/gameplay-modules.md`:

Replace

~~~markdown
                           recover/, challenge/, negotiation/, minor/,
                           cardplay/, dice/; shared rules beside them
~~~

with

~~~markdown
                           recover/, explore/, challenge/, negotiation/,
                           minor/, cardplay/, dice/; shared rules beside them
~~~

**In** `docs/rules/implementation-traceability.md`:

Replace

~~~markdown
I (Wide Horizons, without its Explore action), II (Movers and Shakers)
~~~

with

~~~markdown
I (Wide Horizons), II (Movers and Shakers)
~~~

Then replace

~~~markdown
| The Explore action, so a Wide Horizons map never grows; the altered faces of Foundations V and VI;
~~~

with

~~~markdown
| The altered faces of Foundations V and VI;
~~~

Then replace

~~~markdown
(`WideHorizonsSetupSuite`), but its Explore action is not offered yet. Altered Foundations V and VI
~~~

with

~~~markdown
(`WideHorizonsSetupSuite`), and its Explore action fills them in play (the Explore row under "Major and minor actions"). Altered Foundations V and VI
~~~

Then replace

~~~markdown
unaudited vocabulary rejects. Nested/action-granting boundaries remain F1. |
| Challenge and banner resource actions |
~~~

with

~~~markdown
unaudited vocabulary rejects. Nested/action-granting boundaries remain F1. |
| Explore (Wide Horizons) | **Tested (bounded)** | New Foundations rulebook, "Altered Foundation I: Wide Horizons" and "Setup / When Explored"; setup card `OATH-SETUP04b-NF` | `gameplay/actions/explore/ExploreProcedure.scala` offers the action while Foundation I is altered, a slot is empty, the Atlas holds a site and the actor's pawn is at the Hintermost site, to any role. It rolls 2 defense dice per 1 Supply against a difficulty of 4 through its own windows (`ExploreCost`, `ExploreRoll`), so no Recover power applies and the generic reroll choice does ([Foundations ruling 2](../superpowers/specs/2026-11-11-foundations-design.md#rulings)). On success the explorer chooses Recent or Forgotten, `PlaceAtlasSite` puts that site in the next slot as a bandit site through the builder game start shares (`SiteState.banditSite`), the pawn is placed there, and the `WhenExplored` window resolves the site's ruined edifice (`EdificeSetupSupport.resolvesAt`). The placement is a hidden outcome (`HiddenOutcomes`), so undo never takes it back. Hall of Mockery and Lost Guardians move a held banner to the shared bank, at Setup and when explored. `ExploreProcedureSuite`, `AtlasOperationsSuite`, `AtlasPlacementSuite`, `WhenExploredSuite`, `RuinedSetupRulesSuite`, `UndoBarrierPlaySuite`, `ExploreGameSuite`, `ExploredSiteChronicleSuite` and the three `explore` goldens cover the gates, the rolls, both ends, the powers, undo, the projection, replay and the Chronicle Phase. | The readings in the Foundations design, slice 3, "Explore". Lost Doors and Ruined Temple wait for Legacies. |
| Challenge and banner resource actions |
~~~

Then replace

~~~markdown
| Nothing places a site in an empty slot until the Explore action and Imperial Maps' choice. |
~~~

with

~~~markdown
| Explore places an Atlas site in the next slot (`PlaceAtlasSite`). Imperial Maps' choice, which fills the slots at Setup, waits for slice 4 of the [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md). |
~~~

**In** `docs/rules/ambiguities.md`:

Replace

~~~markdown
which leaves the map's remaining slots empty; its Explore action is not offered yet.
~~~

with

~~~markdown
which leaves the map's remaining slots empty for its Explore action to fill in play.
~~~

**In** `docs/ROADMAP.md`:

Replace

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, Wide Horizons' Explore action, and
  the Beacon's Shape Foundation.
~~~

with

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, and the Beacon's Shape Foundation.
~~~

Then replace

~~~markdown
  add: player choices earlier in setup once foundations and legacies exist,
  such as the Chancellor choosing Recent or Forgotten sites; and WHEN
  EXPLORED triggers once an explore procedure exists.
~~~

with

~~~markdown
  add: player choices earlier in setup once foundations and legacies exist,
  such as the Chancellor choosing Recent or Forgotten sites.
~~~

- [ ] **Step 2: The spec**

The status line, slice 3's opening paragraph, and the Explore subsection:
what the plan found against the code, and the readings it took, for the
owner to confirm.

**In** `docs/superpowers/specs/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2 and 3a landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a and 3b landed.
~~~

Then replace

~~~markdown
Planned in two parts. Slice 3a, landed, is the empty slots, Wide Horizons
at game start, Setup With No Empire and the Chronicle. Slice 3b is Explore,
so until it lands a map with empty slots never grows.
~~~

with

~~~markdown
Landed in two parts. Slice 3a is the empty slots, Wide Horizons at game
start, Setup With No Empire and the Chronicle. Slice 3b is Explore, which
fills an empty slot in play.
~~~

Then replace

~~~markdown
  short roll. Its own windows are `ExploreCost` and `ExploreRoll`; the
  generic `Reroll` window still runs (ruling 2).
~~~

with

~~~markdown
  short roll. Its own windows are `ExploreModifierSelection`, which the
  modifier choice needs and where no card offers anything, `ExploreCost`
  and `ExploreRoll`; the generic `Reroll` window still runs (ruling 2).
  Explore is a `MajorActionType` of its own, so a modifier of another
  action is never offered for it.
~~~

Then replace

~~~markdown
  relic slots dealt from the relic deck while it lasts. The explorer's pawn
  goes there; this is not a Travel. Then the `WhenExplored` window runs for
  that site and its cards. That fills in the When Explored halves the code
  marks as waiting for an Explore procedure (Ruined Site setup rules), beside
  the Market, Forge, Proving Grounds and banner setup rules that already
  share the window.
~~~

with

~~~markdown
  relic slots dealt from the relic deck while it lasts. An operation is
  applied with no catalog, so `PlaceAtlasSite` also carries the site's
  printed capacity, relic slots and resources, which `AtlasPlacement`
  copies from the catalog; `SiteState.banditSite` populates the site, for
  game start too. The card index now holds a stored site's edifice:
  placing one it did not hold would have changed the card inventory, which
  the operation pipeline refuses. The explorer's pawn goes there; this is
  not a Travel. Then the `WhenExplored` window runs on a node that names the
  explored site, `ExploredSite`. Every setup rule with a WHEN EXPLORED half
  already named the window, but applied wherever its face stood on the
  map; each now resolves at an Explore only for a face at the explored
  site (`EdificeSetupSupport.resolvesAt`). An explored edifice is placed
  ruined, so only ruined faces resolve: Hall of Mockery, Bandit Market,
  Broken Forge, Fallen Spire, Broken Clock, Lost Guardians, Empty Grounds,
  Ruined School and Broken Horn. The placement shows the site and what it
  stores, so it is a hidden outcome (`HiddenOutcomes`): undo never takes it
  back, nor the choice of end before it.
~~~

Then replace

~~~markdown
- **Log.** "Explored Forgotten Site: Found Deep Woods" (ruling 4); the new
  site's own power lines follow.
~~~

with

~~~markdown
- **Log.** "Explored Forgotten Site: Found Deep Woods" (ruling 4); the new
  site's own power lines follow. A second Supply writes "Continued
  exploring", as Recover writes "Continued recovering". The lines sit on
  the Game Log's Neutral band.
- **Readings taken with the plan,** for the owner to confirm. Only a power
  that hooks one of Explore's own windows changes an Explore, and none
  does: no modifier is offered, and no power changes its Supply cost. What
  holds for every operation still holds, so Brass Army asks its holder as
  their pawn is placed. The next slot is the first empty slot going
  Hinterward, so when the Hintermost site's region is full the site goes
  to the top slot of the next region. Who rules the Hintermost site does
  not matter. The explorer chooses the end after the roll succeeds, at
  each Explore, without seeing either site, and a later Explore may take
  the other end; one site left is not asked about and is logged as a
  Recent Site. The site is populated as Foundation I prints: a relic deck
  that runs out leaves slots open, and a bandit bank that runs out leaves
  the site short. Its denizens add no favor to the suit banks, since
  Foundation II's count is a setup step. WHEN EXPLORED resolves after the
  pawn is placed, so "if you place your pawn here" holds for the explorer,
  and no ruler is asked to consent, as at Setup. Broken Clock advances the
  round marker at once and does nothing on round 8. An Explore, stopped or
  not, is an action: the action boundary follows it. Lost Doors and Ruined
  Temple wait for Legacies.
- **The banner move, decided with the owner.** Hall of Mockery and Lost
  Guardians "move it to the shared bank": a held banner goes back to the
  shared bank, when explored and at Setup alike. This changes a later
  game's Setup: a Chancellor who starts with the People's Favor or the
  Darkest Secret loses it at Setup's end when the matching ruined edifice
  is on the map, as the card says. The first game, where both banners
  start in the shared bank, does not change.
~~~

If the owner has ruled on any reading in "Rulings needing the owner's
review" by the time you run this task, write the ruling in place of the
reading, and say so in your report. If you took a reading of your own
while working, add it to the "Readings taken with the plan" item.

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%. The plan's own run of these tasks at `bbfb403a7` passed
5816 tests at 95.31% and 89.62%.

Run: `./sbtw "frontend/test"` (the frontend suites, if the full run
skipped them)
Expected: PASS (930 tests in the plan's own run).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. The six goldens and the snapshot were committed with
Tasks 3, 4 and 5, and nothing else changed.

Run: `git grep -n "its Explore action is not offered yet\|once an explore procedure exists\|waits for an Explore procedure" -- src docs/rules docs/architecture docs/ROADMAP.md`
Expected: no output. Nothing still says Explore is missing.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/architecture/game-setup.md docs/architecture/gameplay-modules.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 3b, the Explore action" -m "The setup doc says how Explore fills an empty slot, the traceability table gains an Explore row and stops calling the action missing, the roadmap drops what is done and keeps the banner move it leaves, and the spec records slice 3b landed, what its plan found against the code and the readings it took." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## For slice 4

Slice 4, Imperial Maps' choice, fills the empty slots at Setup. It reuses:

- `AtlasPlacement.at(catalog, ready, end)`, once per empty slot going
  Hinterward. `PlaceAtlasSite` is an undo barrier wherever it runs. Each call reads the state the placement before it left, so
  the Forgotten end takes the Atlas's last site, then the one before it.
- `PlaceAtlasSite`, journaled by the Setup walk, which populates the site
  as Explore does.
- `AtlasPlacement.Planned.stored.placedDenizens`, the cards the site puts
  on the map, for its one addition: 1 favor from the shared bank to the
  matching suit's bank per denizen placed, as a `Move` beside each
  placement.
- `AtlasEnd`, for the Chancellor's `setup.imperial-maps` answer, and
  `ExploreProcedure.endOption`'s spelling of the two ends.

It does not fold `WhenExplored`: a site placed at Setup resolves its SETUP
half at Setup's end, as today.

## Self-review

**Spec coverage** (slice 3, "Explore", and the Explore items of "Tests"):

| Spec statement | Task |
|---|---|
| A new major action, `explore` | 3 (`ActionRef.Explore`, `MajorActionType.Explore`, the Act menu) |
| Offered under Wide Horizons, at the Hintermost site, with an empty slot and an Atlas site; any role; otherwise disabled with its reason | 3 (`ExploreProcedure.refusal`, `build`; the gate tests and the menu's reasons) |
| The tree mirrors Recover's: modifier choice, pool `explore`, 2 defense dice per 1 Supply against 4, continue or stop | 3 |
| Own windows; the generic `Reroll` window still runs (ruling 2) | 3 (the Jinx and Silver Charm tests) |
| The explorer chooses Recent or Forgotten (`explore.end`), unasked with one site | 3 |
| `PlaceAtlasSite(end, region)`, journaled: the end's site with its stored cards, edifice ruined, denizens faceup, relics facedown, bandits, printed tokens, open relic slots dealt | 1 and 2 |
| The explorer's pawn goes there; not a Travel | 3 (the batch test) |
| The `WhenExplored` window runs for that site and its cards | 4 |
| Stop: "Failed to explore"; the Supply stays spent | 3 |
| Log: "Explored Forgotten Site: Found Deep Woods"; the site's own power lines follow | 3 and 4 (the two goldens) |
| Frontend: the action in the Act menu; the map shows the new site | 6 |
| Tests: each gate; rolls with a Jinx reroll, with Silver Charm not offered, with a stop; each end and the one-site Atlas | 3 |
| Tests: When Explored powers firing on the new site | 4 |
| Tests: a full map refusing Explore | 3 |
| Tests: an Explore golden log | 3 and 4 (succeeded, stopped, and with a power line) |
| Tests: a Chronicle round trip after an Explore | 7 |
| Docs | 8 |
| Review: the placement is an undo barrier | 2 (`HiddenOutcomes`) and 3 (`UndoBarrierPlaySuite`) |
| Owner decision: Hall of Mockery and Lost Guardians move a held banner to the shared bank | 5 |

**Left for later, on purpose:** Imperial Maps' choice at Setup (slice 4);
Lost Doors and Ruined Temple (Legacies); a view of the Atlas for the
explorer; `CONTEXT.md` (slice 7).

**Placeholders:** none. Every step that changes code shows the code, and
every file written by a command shows its expected text.

**Names used across tasks:** `AtlasEnd`, `AtlasState.site`,
`SiteState.banditSite`, `MapState.withSite` (Task 1); `PlaceAtlasSite`,
`AtlasPlacement.at`, `AtlasPlacement.Planned` (Task 2);
`ExploreProcedure`, its `endOption`, `continueOption`, `stopOption`,
`choiceDecisionId`, `endDecisionId` and `refusal`, `ActionRef.Explore`
(Task 3); `ExploredSite`, `EdificeSetupSupport.resolvesAt` (Task 4);
`BannerToSharedBank` (Task 5);
`Table.mapCutIntoAtlas`, `Table.storedInAtlas` (Task 1, used by every
later suite). The suites' table helpers name what they build:
`p1AtHintermostOfThree`, `p1AtHintermostAtlasOfDeepWoods`,
`deepWoodsStoring`, `actMenuAtHintermostOfThree`. Each is spelled the same
wherever it appears.
