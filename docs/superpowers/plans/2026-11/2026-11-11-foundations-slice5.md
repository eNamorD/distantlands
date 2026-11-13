# Foundations, Slice 5: Grand Council Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An altered Foundation V, Grand Council, starts the game with the
People's Favor on its Grand Council face. Its holder may play a card to
any Imperial site on the map, and may first discard a card from an
Imperial site the card goes to. A Challenge may take the face, and every
take of the People's Favor adjusts it by the face's printed ribbons: half
its favor, rounded down, back to the previous holder, the rest to the
favor banks.

**Architecture:** Game start reads Foundation V from the faces it already
resolved (`FirstGameSetupPlan.foundations`) when the materializer builds
the banners, and `StartingFoundations` drops the face's refusal. The power
is a new contribution, `PeoplesFavorGrandCouncil`, registered beside
`PeoplesFavorMob` in `BannerFacePowers`. It uses the seam Baron uses at the
`SearchPlayAdviser` window: one `PlaySites` and one `DiscardPermission`,
both holding where `SiteRulers.isImperialSite` holds. The ribbon moves
into `BannerRules.peoplesFavorRibbon`, which reads the face, and Challenge,
Favored Son, a Campaign Raid and the Conspiracy all call it; Challenge's
face gate then refuses only the Festival. The projection and the frontend
need no change: both faces are already projected and drawn.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../../specs/2026-11/2026-11-11-foundations-design.md),
sections "Rulings", "Approach", "Slice 3" ("Setup With No Empire", whose
erosion of Foundation V this slice must leave working), "Slice 5: Grand
Council" and "Slice 6: Festival" (the other banner face, which stays
refused). Slice 4's plan,
[Foundations slice 4](2026-11-11-foundations-slice4.md), is the format this
plan follows.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`). The
  largest file this slice grows is
  `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`, to
  about 385 lines; no production file passes 160.
- `gameplay` must not read a catalog `.text` or `rulesText`. `model`
  imports nothing from `gameplay`. `gameplay/actions` imports nothing from
  `gameplay/powers` (`scripts/check-architecture.py`). The new power lives
  in `gameplay/powers/banner` and reads `gameplay/actions` and
  `gameplay.SiteRulers`; the shared ribbon lives in
  `gameplay/actions/BannerRules.scala`, which the powers may read.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. A test that names a symbol the task has not built
  yet fails by not compiling, and that is the failure to see. Tests go in
  the repo's existing suites where one covers the subject. A test helper's
  name says what it builds.
- Wire and journal changes need not keep old journals decodable or
  replayable. This slice adds no wire field, no decision id and no
  operation. It adds the power id `banner.peoples-favor.grand-council`,
  which `RuleSourceIndex` already lists, and that power declares two note
  keys from the shared `CardPlayRules` set (`discard-first`,
  `bury-first`). A Raid on the People's Favor now records one `Move` of 1
  favor per favor returned to the banks, where it recorded one `Move` per
  suit. The recorded operations are the replay authority, so a new
  journal's bytes differ while the state reached is the same, and no Game
  Log line reads these moves (Plan-time findings 5 and 10).
- New Foundations rules only. The printed setup cards are
  `OATH-SETUP08a-NF` (Mob's Favor) and `OATH-SETUP08b-NF` (Grand Council)
  in `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (each entry's `text:` comes before its `name:`; read it by id). The
  rulebook is `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`:
  "Altered Foundation V: Grand Council" (about line 354), "Resolve raid
  victory" (about line 778), "Taking Banners" and "Banner Powers" (about
  lines 1102 to 1127) and "Imperial sites" (about line 1178). The banner's
  image is page 1 of
  `/Users/roman/projects/oathdigital/reference/2026-03-17-new-foundations-rev1/banners.pdf`
  (`reference/` is gitignored, so a worktree has no copy; read the main
  checkout's file and change nothing there), and the same
  face at full size is
  `~/projects/bgs-cards/content/card-images/oath/en-US/Banner of the People's Favor - Grand Council (NF).png`.
  Read them there; make no web call.
- If a rule is unclear while you work, do not guess silently and do not
  stop: take the most literal reading of the New Foundations text, add it
  to the spec's slice 5 readings (Task 4, Step 2), and name it in your
  report.
- An unavailable option is shown disabled with its reason, never hidden.
  This slice adds one kind of option, a site slot at a distant Imperial
  site; an intact edifice or a locked card there shows its Replace
  unavailable with "Locked: cannot be discarded", as at the pawn site. A
  site that is not Imperial is not offered: the Grand Council never opens
  it, and no other rule of the player's could.
- A Game Log line omits the actor on their own turn, and its wording stays
  parallel to the lines beside it. The Grand Council's discard writes the
  Mob's line, "May discard a card at Broken Peaks first.", credited to the
  Grand Council. The ribbons write no line, as the Mob's and the Wandering
  Flame's write none.
- Note keys change in Task 2 only, as that task shows. Game Log goldens do
  not change in this slice. The rewrite commands are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
  After a rewrite, read `git diff -- src/test/resources` and
  `git status --short src/test/resources` line by line against the text the
  task shows. If anything else changed, stop and report: do not commit a
  file you cannot explain. Never run the golden rewrite in this slice: if
  `GameLogGoldenSuite` fails, stop and report.
- Frontend work follows `DESIGN.md`. This slice changes no frontend file:
  `BannerFace.rules("grand-council")` already holds the printed text, and
  the projection already names the face `grand-council`.
- The words are "Imperial site" for a site the Empire's warbands hold,
  "holder" for the player who holds the banner, "previous holder" for the
  holder a take removes, "ribbon" for the printed column of icons that
  says how a taken banner's favor leaves it (left: Challenge; right:
  Campaign), and "distant site" for a site other than the pawn site.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `b3226f6f1` (slice 4's branch head) with the earlier tasks of this plan
  applied, and occurs once in its file. "Then replace" is the next block in
  the same file. A "with" block repeats the unchanged lines of its
  "Replace" block, so an edit that only adds lines still shows where they
  go. "Create" gives a file's full text. If a block does not match because
  slice 4's review fixes moved the text, find the same code by its content
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

Work in the worktree `/Users/roman/projects/oathdigital/.worktrees/foundations-s5`,
on the branch `foundations-slice5`, cut from `foundations-slice4` at
`b3226f6f1`. Slice 4's review fixes may have been merged into it since.
Never run a git command that changes state in the main checkout,
`/Users/roman/projects/oathdigital`, which holds another session's work.

In the worktree, link the main checkout's tooling before the first
`./sbtw`, unless the links are already there:

```bash
cd /Users/roman/projects/oathdigital/.worktrees/foundations-s5
test -e .tooling || ln -s /Users/roman/projects/oathdigital/.tooling .tooling
test -e node_modules || ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor f3136fb3e HEAD && echo slice3b-ok
git merge-base --is-ancestor b3226f6f1 HEAD && echo slice4-ok
git merge-base --is-ancestor 3a19d633a HEAD && echo slice4-fix-ok
git grep -n "object ImperialMaps" -- src/main/scala/distantlands/gameplay/setup/ImperialMaps.scala
git grep -n "AtlasPlacement.suitOf(catalog, _)" -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "PeoplesFavorState(PeoplesFavorFace.Mob, None, 1)," -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "FoundationNumber.V -> 5," -- src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala
git grep -n "Vector\[ContributingPower\](PeoplesFavorMob)" -- src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala
git grep -n "def isImperialSite(ready: ReadyGame, site: SiteId): Boolean" -- src/main/scala/distantlands/gameplay/SiteRulers.scala
git grep -n "def openSites(sites: PlaySites): CardPlayRules" -- src/main/scala/distantlands/gameplay/actions/CardPlayRules.scala
git grep -n "def peoplesFavorReturns(ready: ReadyGame)" -- src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala
git grep -n "banners.peoplesFavor.active == PeoplesFavorFace.Mob &&" -- src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala
git grep -n "Slices 1, 2, 3a, 3b and 4 landed" -- docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md
git grep -c "PeoplesFavorGrandCouncil" -- src/main/scala
```

The first must print `slice3b-ok`: `f3136fb3e` is the merge of slice 3b
into `0.2.0-alpha`, and a worktree without it was cut from a stale branch:
stop and report. The second must print `slice4-ok`: `b3226f6f1` is slice
4's head, which this plan was first applied to. The third should print
`slice4-fix-ok`: `3a19d633a` is slice 4's last review fix (tests and docs
only), merged into this branch at `2470926a3`, where the revised plan was
applied; if only it is missing, go on. Each grep but the last must print
one line. They name what this plan stands on: slice 4's `ImperialMaps` and
`AtlasPlacement.suitOf`, the banners game start builds, the refusal Task 1
deletes, the Mob's registration, the Imperial-site test, Baron's
`openSites` seam, the ribbon and the face gate Task 3 changes, and the
spec's status line. The last must print nothing and exit with status 1: no
Grand Council power exists yet. If a grep prints nothing while the others
print, slice 4's review moved or renamed that code: read the file, and go
on only if the code the tasks edit is still there in substance, adapting
the blocks that name it.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

The quiet table (`Table.start`) is the fixture's first game with nothing on
the map: p1, p2 and p3 stand at Ancient City, Broken Peaks and Buried
Giant, it is p1's Act, no site holds warbands, and both banners are unheld,
the People's Favor on the Mob. `Table.imperial(p2)` makes p2 the
Chancellor; `warbandsAt(site, p2, 1)` then makes that site Imperial.

## What the banner image shows

Page 1 of `banners.pdf` is one image of the four banner faces; rendered
(`qlmanage -t -s 2400`) and cropped, it matches the full-size card image
named in Global Constraints. The Grand Council face (bottom right) prints:

- **Left ribbon**, under the scroll icon (Challenge): "HALF" over the
  return-favor icon (a favor with a curved arrow back,
  `banner-returnfavor`), a rule, then "REST" over the bank-favor icon (a
  favor above a circled down arrow, `banner-bankfavor`).
- **Right ribbon**, under the 3 defense die (Campaign): the same, "HALF"
  return-favor, then "REST" bank-favor.
- **Text:** "When playing a card, you may play it to any Imperial site on
  the map, and you may discard a denizen from that site first." and
  "ACTION: Place [favor] on the banner."

The Mob face (bottom left) prints "ALL" over the bank-favor icon on both
ribbons. The rulebook's key ("Taking Banners"): resolve the left ribbon in
a Challenge and the right in a Campaign, top to bottom, rounding halves
down. `banner-returnfavor`: "Return favor to the banner's previous holder.
If the banner was in the shared bank, return favor to the favor banks as
described below." `banner-bankfavor`: return favor one at a time to the
bank with the least favor, the leftmost on a tie.

So a take of the Grand Council gives the previous holder half the favor,
rounded down, then returns the rest to the banks one at a time; an unheld
banner's favor all goes to the banks. Favored Son ("adjust its [favor]
using its left ribbon") and the New Foundations Conspiracy ("as shown by
its right ribbon") read the same ribbons.

## Plan-time findings

The plan was revised after its review: Task 4 also corrects
`docs/rules/ambiguities.md`, `docs/architecture/game-setup.md` and one more
cell of the traceability table, Task 2 adds a Temple of Ancestors test
(Ruling 1), finding 10 says how replay treats the Raid's moves, and the
banner image's path is absolute. The revised blocks were applied in order
to a fresh export of `2470926a3` (this plan's first commit merged with
slice 4's last fix), where the touched suites and the note-key snapshot
pass.

The spec was checked against the code at `b3226f6f1`, and the slice was
prototyped there, one commit per task. The blocks below were then applied
in order to a fresh worktree at `b3226f6f1`, where each task's tests and
the full gates pass. Where the spec and the code differ, this plan follows
the code, and Task 4 records it in the spec.

1. **Game start already sets up the Mob as Grand Council needs.** The
   materializer starts the People's Favor on the Mob, unheld, with 1
   favor, and `LaterGameSetup.imperial` gives it to the Chancellor when the
   Oathkeeper goal is the People's Favor. `OATH-SETUP08b-NF` prints the
   same setup with "Flip the People's Favor to its **Grand Council**
   side", so Task 1 changes only the face, read from the plan's resolved
   faces (`FirstGameSetupPlan.altered`). Setup With No Empire erodes V to
   Mob's Favor before the plan is built, so it still starts the Mob.
2. **Every card play already walks `SearchPlayAdviser`.**
   `CardPlayProcedure.PlacementTree` is the one placement seam: a Search's
   play, the Play Facedown Adviser action and a power's own play (Search
   Party, Temple of Ancestors, Oracle) all build it, and its window is
   always `SearchPlayAdviser`. An automatic contribution there, as the
   Mob's is, reaches every one. A power's own play only keeps modifiers
   out (`CardPlayProcedure.playedByPower`).
3. **Baron's seam is the Grand Council's.** `CardPlayRules.openSites`
   with a `PlaySites` predicate, and `DiscardPermission.lined(...,
   appliesAt = ...)`, already give "play to these sites, and discard there
   first": `CardSlots.playSites` offers the pawn site and then each opened
   site in map order, each with the pawn site's rules, and
   `CardSlots.permissions` keeps a permission only where its `appliesAt`
   holds. `CardPlay.operations` already refuses a Vision or an adviser-only
   card at any site, and the discard rules already refuse an intact
   edifice or a locked card ("Locked: cannot be discarded"). No card-play
   code changes.
4. **The Imperial-site test exists.** `SiteRulers.isImperialSite` (the
   Empire's warbands hold the site) is what Imperial Seal and the Grand
   Mask read. The Grand Mask changes who rules the cards at Imperial
   sites, not whether they are Imperial (catalog batch 11, ruling 7).
5. **Four takes read the People's Favor's ribbon, in three copies.**
   `ChallengeRibbon.peoplesFavorReturns` (Challenge, and Favored Son's
   `leaving`), `ConspiracyWhenPlayed.take` and `CampaignRaid.takes` each
   returned every favor to the banks, the Raid grouping its moves by suit.
   Task 3 makes one `BannerRules.peoplesFavorRibbon` that reads the face,
   and all four call it. The Raid's moves become one favor each, in the
   same order and totals. No Game Log line reads these moves: the Raid's
   lines read the state (`CampaignNotes.spoilsNote`), and a delta's own
   line (`DetailLines.delta`) does not name them.
6. **Challenge refused both altered faces at once.**
   `ChallengeProcedure.supportedFaces` refused the start unless the
   People's Favor showed the Mob and the Darkest Secret the Wandering
   Flame. Task 3 keeps only the Festival's half, which slice 6 removes. No
   test pinned the refusal; Task 3 adds one for the Festival.
7. **A Raid's burn follows the ribbon.** "Resolve raid victory" takes the
   banners and adjusts them by their right ribbons, then the defender
   "burns half of their favor". `CampaignRaid.consequences` already reads
   the board after the takes, so the favor the Grand Council's ribbon
   returns to the defender counts toward the half they burn. Only its
   comment changes.
8. **The frontend needs nothing.** `BannerFace.rules("grand-council")`
   prints the face's text, `GamePresentationProjector` names the face
   `grand-council`, and Baron already sends distant site slots through the
   same placement question. The Foundations strip shows V altered from
   slice 1.
9. **No-Empire games.** A real game with no Empire erodes V, so the Grand
   Council never starts there; with no Chancellor nobody holds Imperial
   warbands. The spec's "no Imperial sites in a no-Empire game" test is
   therefore a quiet table with the face set by hand, and Task 1 adds a
   game-start test that the erosion still starts the Mob.
10. **What changes for existing games.** None can have the face: game
    start refused it until now. Replay does not recompute a step's moves:
    a `DeltaPayload`'s recorded operations are the replay authority
    (`gameplay/walker/WalkerEvents.scala`, `WalkerStepRecorded`), so an old
    journal with a Raid on the Mob replays its grouped moves as recorded.
    A Raid recorded after this slice journals one 1-favor `Move` per favor
    instead: its journal bytes differ, the state it reaches does not, and
    no Game Log line reads these moves (`DetailLines.delta`).

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **The face is read where the banners are built.**
   `FirstGameSetupMaterializer.peoplesFavorFace(plan)` turns the People's
   Favor to `GrandCouncil` when the plan's Foundation V is altered. No
   step of Setup's walk is added.
2. **One power object, as the Mob's.** `PeoplesFavorGrandCouncil`
   (`gameplay/powers/banner`), id `banner.peoples-favor.grand-council`,
   source `RuleSourceRef.Banner("peoples-favor")`, applicable while the
   actor holds the banner on the Grand Council face. Its predicate
   `imperialSite` is a `val`, shared by its `PlaySites` and its permission,
   and read on the board each play is planned on.
3. **The discard's line is the Mob's.** `DiscardPermission.lined` writes
   "May discard a card at {site} first." for the discard it allowed,
   credited to the Grand Council's power id. The snapshot of declared note
   keys gains the two shared keys under the new id.
4. **One ribbon for every take.** `BannerRules.favorToPreviousHolder` says
   how much of the favor the face gives back (Mob none, Grand Council half,
   rounded down, and none when unheld), and
   `BannerRules.peoplesFavorRibbon(ready)` returns the moves: the previous
   holder's share in one `Move`, then one `Move` of 1 favor per favor to
   the least-favor bank, leftmost first (`raidFavorReturn`).
   `ChallengeRibbon.peoplesFavorReturns` is deleted; its one caller outside
   Challenge, Favored Son, calls the new helper. The Raid and the
   Conspiracy are in scope because they read the same face's right ribbon,
   which prints the same; left at the Mob's they would give the Grand
   Council a ribbon it does not print.
5. **Secret Testament is untouched.** It replaces the whole Challenge
   ribbon with "take all [favor]", on either face.
6. **Tests.** `AlteredFoundationSetupSuite` and `NoEmpireSetupSuite` take
   game start (Task 1); `PeoplesFavorGrandCouncilSuite` (new) takes the
   power (Task 2); `ChallengeProcedureSuite`, `FavoredSonSuite`,
   `CampaignRaidSuite` and `VisionPlaySuite` take the ribbons (Task 3).

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **"When playing a card" is every card play.** Banner: "When playing a
   card, you may play it to any Imperial site on the map". Reading: a
   Search's play, the Play Facedown Adviser action and a power's own play
   (Search Party, Oracle, Temple of Ancestors) all count, as they do for
   the Mob (Task 2, "it applies to a facedown adviser played to a site as
   well"). A power that limits where its own card goes still limits it:
   Temple of Ancestors plays only to the advisers (its `PlacementLimit`
   refuses every site placement), so under the Grand Council its distant
   Imperial sites are shown, disabled with the Temple's reason, as its
   pawn site is (Task 2, `TempleOfAncestorsSuite`).
2. **A card goes to a distant Imperial site only as it could go to the
   pawn site.** Reading: a Vision and an adviser-only card are never
   offered a site; a site-only card may go to any Imperial site; each site
   follows its own capacity, as Baron's and New Growth's sites do (Task
   2).
3. **A full Imperial site takes the card only in place of a card
   discarded there.** Rulebook: "When playing a card to an Imperial site,
   you may first discard a denizen _(even a ruined edifice)_ from the
   site." Reading: as the Mob, the discard makes a full Imperial site a
   place to play; a full site with nothing that may be discarded takes no
   card (Task 2, "a full Imperial site offers ...").
4. **The discard is optional, one card per play, and holds only at
   Imperial sites.** Reading: one permission, which stacks with the
   Homeland's and any other as the Mob's does, and holds at the pawn site
   only when the pawn site is Imperial; a play to a site the Empire does
   not hold discards nothing by this face (Task 2, "its discard holds only
   at Imperial sites ..." and "its discard holds at a pawn site the Empire
   holds too"). A ruined edifice may go; an intact edifice or a locked
   card may not.
5. **Nobody is asked to consent.** Reading: playing a card to a site and
   discarding a card there are not uses of another player's card power,
   so the Chancellor is not asked; printed protections (the Hall of
   Ministers, locks) still apply through the generic discard rules
   (Task 2).
6. **An Imperial site is one the Empire's warbands hold when the play is
   planned; the Grand Mask does not change it.** Rulebook: "Sites with
   Imperial warbands are called **Imperial sites**." Grand Mask FAQ: the
   sites stay Imperial. Reading: the same sites are Imperial on every
   turn, and a Grand Mask wearer who holds the face plays to them (Task 2,
   "the Grand Mask leaves the Imperial sites Imperial ...").
7. **The holder's role does not matter.** Reading: an Exile, a Citizen
   and the Chancellor holding the face all play to every Imperial site
   (Task 2 uses an Exile; nothing in the power reads the role).
8. **Half, rounded down, to the previous holder; an unheld banner's all
   to the banks.** Banner image: "HALF" return-favor, "REST" bank-favor.
   Rulebook: "**Round down** when adjusting by half"; return-favor: "If
   the banner was in the shared bank, return favor to the favor banks as
   described below." Reading: the previous holder's share moves first,
   then the rest one at a time to the least-favor bank (Task 3).
9. **The Raid and the Conspiracy use the same ribbon, and the defender
   then burns half their favor, the returned favor included.** Rulebook,
   "Resolve raid victory": take the banners, "adjust its resources
   following its right ribbon", then "The defender burns half of their
   favor". Reading: the printed order (Task 3, "a Raid victory over the
   Grand Council ...").
10. **Game start: the Chancellor holds the Grand Council when the
    Oathkeeper goal is the People's Favor; otherwise, or with no
    Chancellor, the shared bank does.** Card `OATH-SETUP08b-NF`: "Place
    [favor] on it from the shared bank. If the Oathkeeper goal is the
    People's Favor, give the banner to the Chancellor, if any. Otherwise,
    place it in the shared bank." Reading: as Mob's Favor today, 1 favor;
    a first game with an altered V starts the Grand Council unheld (Task
    1).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala` | Foundation V's refusal deleted |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | `peoplesFavorFace` |
| `src/main/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncil.scala` | new: the power |
| `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala` | registers it |
| `src/main/scala/distantlands/gameplay/actions/BannerRules.scala` | `favorToPreviousHolder`, `peoplesFavorRibbon` |
| `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala` | calls the shared ribbon; `peoplesFavorReturns` deleted |
| `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala` | the face gate refuses only the Festival |
| `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala` | calls the shared ribbon |
| `src/main/scala/distantlands/gameplay/powers/whenplayed/FavoredSon.scala`, `ConspiracyWhenPlayed.scala` | call the shared ribbon |
| `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`, `AlteredFoundationSetupSuite.scala`, `NoEmpireSetupSuite.scala` | game start (Task 1) |
| `src/test/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncilSuite.scala` | new: the power (Task 2) |
| `src/test/scala/distantlands/gameplay/powers/action/TempleOfAncestorsSuite.scala` | the Temple's own play under the Grand Council (Task 2) |
| `src/test/resources/notes/declared-keys.txt` | rewritten by its command (Task 2) |
| `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`, `CampaignRaidSuite.scala`, `VisionPlaySuite.scala`, `src/test/scala/distantlands/gameplay/powers/whenplayed/FavoredSonSuite.scala` | the ribbons (Task 3) |
| `docs/ROADMAP.md`, `docs/architecture/bounded-banners.md`, `docs/architecture/bounded-visions-and-conspiracy.md`, `docs/architecture/all-exile-round-endings.md`, `docs/architecture/game-setup.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, the spec | docs (Task 4) |

---

### Task 1: Grand Council turns the People's Favor at game start

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`

**Interfaces:**
- Consumes: `FirstGameSetupPlan.altered(number: FoundationNumber):
  Boolean` (`model/Setup.scala`), `StartingFoundations.refusal`,
  `LaterGameSetup.imperial`'s banner holder (unchanged).
- Produces: game start with an altered Foundation V gives
  `banners.peoplesFavor == PeoplesFavorState(PeoplesFavorFace.GrandCouncil,
  holder, 1)`, where `holder` is the Chancellor when the Oathkeeper goal is
  `ThePeople`, else `None`. Nothing later tasks call.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
(the refusal of V goes; Festival is now the first refused face):

Replace

```scala
    assertEquals(refusalFor(FoundationNumber.V),
      unsupported("Grand Council (altered Foundation V)", 5))
    assertEquals(refusalFor(FoundationNumber.VI),
```

with

```scala
    assertEquals(refusalFor(FoundationNumber.VI),
```

Then replace

```scala
    // II and III are accepted, so V is the first refused.
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Grand Council (altered " +
        "Foundation V) is not supported yet: it arrives with Foundations " +
        "slice 5"))
```

with

```scala
    // II, III and V are accepted, so VI is the first refused.
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Festival (altered " +
        "Foundation VI) is not supported yet: it arrives with Foundations " +
        "slice 6"))
```

**In** `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`:

Replace

```scala
/** Game start under the two altered faces that only change setup, Movers
  * and Shakers (II) and Teeming World (IV) (Foundations design, slice 2).
  * Every test starts from a Chronicle: game start is the subject. */
```

with

```scala
/** Game start under the altered faces that change setup: Movers and
  * Shakers (II) and Teeming World (IV) (Foundations design, slice 2), and
  * the face Grand Council (V) turns the People's Favor to (slice 5). Every
  * test starts from a Chronicle: game start is the subject. */
```

Then replace

```scala
    assertEquals(movers.game.current.banners, tribes.game.current.banners)
```

with

```scala
    assertEquals(movers.game.current.banners, tribes.game.current.banners)

  test("Grand Council flips the People's Favor to its Grand Council face " +
      "with 1 favor, and gives it to the Chancellor when the Oathkeeper " +
      "goal is the People's Favor, as Mob's Favor does"):
    val people = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople)
    val council = started(withAlteredFoundation(people, FoundationNumber.V))
    assertEquals(council.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p1), 1))
    assertEquals(council.game.campaign.foundations(FoundationNumber.V),
      FoundationState(FoundationFace.Altered, Set.empty))
    assertEquals(started(people).game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.Mob, Some(p1), 1))

  test("under another Oathkeeper goal, or with no Chancellor, the Grand " +
      "Council starts in the shared bank, and nothing else changes"):
    val supremacy = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Supremacy)
    val mob = started(supremacy)
    val council = started(withAlteredFoundation(supremacy, FoundationNumber.V))
    assertEquals(council.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, None, 1))
    assertEquals(council.game.current.copy(banners = mob.game.current.banners),
      mob.game.current)
    assertEquals(council.banks, mob.banks)
    val firstGame = started(withAlteredFoundation(firstGameChronicle.copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople), FoundationNumber.V))
    assertEquals(firstGame.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, None, 1))
```

**In** `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`
(this test passes before and after the change: it pins that the erosion
runs before the face is read):

Replace

```scala
  test("a Foundation I already on Wide Horizons keeps its sources"):
```

with

```scala
  test("an eroded Foundation V starts the People's Favor on the Mob in the " +
      "shared bank, whatever the Oathkeeper goal"):
    val record = absentEmpireChronicle().copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople,
      foundations = Map(FoundationNumber.V -> altered))
    assertEquals(started(record).game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.Mob, None, 1))

  test("a Foundation I already on Wide Horizons keeps its sources"):
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.NoEmpireSetupSuite"`
Expected: FAIL, 3 tests: "with several altered faces, game start names
the first refused one in foundation order" (it obtains the Grand Council
refusal), and both Grand Council tests of `AlteredFoundationSetupSuite`
(game start refuses the Chronicle: "Grand Council (altered Foundation V)
is not supported yet"). The new `NoEmpireSetupSuite` test passes.

- [ ] **Step 3: Implement**

**In** `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

Replace

```scala
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.V -> 5,
    FoundationNumber.VI -> 6)
```

with

```scala
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.VI -> 6)
```

**In** `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
        PeoplesFavorState(PeoplesFavorFace.Mob, None, 1),
```

with

```scala
        PeoplesFavorState(peoplesFavorFace(plan), None, 1),
```

Then replace

```scala
  /** The favor on each board: 1, and under Movers and Shakers (altered
```

with

```scala
  /** Foundation V's face, on the People's Favor: Mob's Favor sets the
    * Mob, and Grand Council flips it to the Grand Council. Both faces put
    * 1 favor on it from the shared bank, and `LaterGameSetup` gives it to
    * the Chancellor when the Oathkeeper goal is the People's Favor. */
  private def peoplesFavorFace(plan: FirstGameSetupPlan): PeoplesFavorFace =
    if plan.altered(FoundationNumber.V) then PeoplesFavorFace.GrandCouncil
    else PeoplesFavorFace.Mob

  /** The favor on each board: 1, and under Movers and Shakers (altered
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.NoEmpireSetupSuite"`
Expected: PASS (66 tests in the plan's own run).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala
git commit -m "feat(setup): Grand Council turns the People's Favor at game start" -m "An altered Foundation V now starts the game with the People's Favor on its Grand Council face, with 1 favor from the shared bank and in the Chancellor's hands when the Oathkeeper goal is the People's Favor, as Mob's Favor sets up the Mob. Game start no longer refuses the face; Festival is the last altered face it refuses." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The Grand Council plays to any Imperial site

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncil.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncilSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/powers/action/TempleOfAncestorsSuite.scala`
- Rewritten: `src/test/resources/notes/declared-keys.txt`

**Interfaces:**
- Consumes: `SiteRulers.isImperialSite(ready: ReadyGame, site: SiteId):
  Boolean` and `SiteRulers.imperialSites(ready): Vector[SiteId]`
  (`private[gameplay]`, so tests under `distantlands.gameplay` may call
  them); `CardPlayRules.openSites(PlaySites)`,
  `CardPlayRules.permitSiteDiscard(DiscardPermission)`,
  `DiscardPermission.lined(source, from, removal, appliesAt)`,
  `CardPlayProcedure.PlacementTree.adjust`; the test kit's
  `SearchFixture.{start, keep, placeAt, placeReplacing, rules, after}`,
  `PlacementPicks`, `NoteText.said`, `ActPowerHarness.replayed`,
  `FacedownAdviserPlay.parkedOnPlacement`, `PowerFixture.asAdviser`.
- Produces: `object PeoplesFavorGrandCouncil extends ContributingPower`
  with `val id: PowerId` (`banner.peoples-favor.grand-council`) and
  `val imperialSite: (ReadyGame, SiteId) => Boolean`;
  `BannerFacePowers.contributions` lists it after `PeoplesFavorMob`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncilSuite.scala`:

```scala
package distantlands.gameplay.powers.banner

import distantlands.gameplay.{FacedownAdviserPlay, GrandMaskCard,
  RuleSourceIndex, SiteRulers}
import distantlands.gameplay.actions.CardPlayRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DesolateShoreSite, DunesSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.MessengerCard
import distantlands.gameplay.powers.campaign.{CodeOfHonorCard,
  ShieldWallCard, ToweringRampartCard}
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, PlacementPicks, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Grand Council, the People's Favor's altered face: "When playing a card,
  * you may play it to any Imperial site on the map, and you may discard a
  * denizen from that site first." (Foundations design, slice 5). */
class PeoplesFavorGrandCouncilSuite extends munit.FunSuite:
  import SearchFixture.{keep, placeAt, placeReplacing, rules, start}

  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  /** The Beast card each Search keeps and plays. */
  private val played = ErrandBoyCard.id
  private val fillers = Vector(CodeOfHonorCard.id, MessengerCard.id,
    ShieldWallCard.id)
  /** p1's pawn site: room for three, no Homeland. */
  private val home = AncientCitySite.id
  /** The Empire's sites, each with room for one and no Homeland. */
  private val peaks = BrokenPeaksSite.id
  private val shore = DesolateShoreSite.id
  private val dunes = DunesSite.id
  /** p3's site. */
  private val giant = BuriedGiantSite.id

  /** p1, an Exile, in Act at Ancient City with 5 Supply and `played` on the
    * world deck, holding the People's Favor on its Grand Council face. p2
    * is the Chancellor; the Empire holds Broken Peaks, where `atPeaks`
    * stand, and Desolate Shore. p3 rules Buried Giant. */
  private def councilHolderBesideTheEmpire(atPeaks: Vector[DenizenId])
      : Table =
    atPeaks.foldLeft(Table.start.imperial(p2).pawn(p1, home)
      .worldDeckTop(played).supply(p1, 5)
      .peoplesFavor(Some(p1), 1, PeoplesFavorFace.GrandCouncil)
      .warbandsAt(peaks, p2, 1).warbandsAt(shore, p2, 1)
      .warbandsAt(giant, p3, 1))((table, card) =>
        table.denizen(card, at = peaks))

  /** Searches with no modifier and keeps `played`, parked on its
    * placement. The events are the whole Search's so far. */
  private def searchedAndParkedOnPlacement(ready: ReadyGame): OathTransition = accepted(for
    started <- start(ready)
    kept <- keep(started, played)
  yield kept.copy(events = started.events ++ kept.events))

  /** The parked placement's site slots, each with the card it replaces. */
  private def slotsOf(at: OathTransition): Vector[(DecisionOptionRef,
      Option[CardId])] =
    PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(at.state,
      walkerPowers)).map(option => option.ref -> PlacementPicks.replaces(
      option))

  private def councilSaid(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(PeoplesFavorGrandCouncil.id,
      Vector(CardPlayRules.discardFirst), events)

  test("the source index names this power on the Grand Council face"):
    val favor = RuleSourceIndex.enumerate(catalog,
      councilHolderBesideTheEmpire(Vector.empty).ready).find(_.source ==
        RuleSourceRef.Banner(Banner.PeoplesFavor.key))
    assertEquals(favor.map(_.powerIds),
      Some(Vector(PeoplesFavorGrandCouncil.id)))

  test("its holder may play to every Imperial site after the pawn site, " +
      "and to no site another player or nobody rules"):
    val ready = councilHolderBesideTheEmpire(Vector.empty).ready
    assertEquals(SiteRulers.imperialSites(ready), Vector(peaks, shore))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("a full Imperial site offers its denizen and a ruined edifice as " +
      "Replace, and shows an intact edifice locked"):
    val ready = councilHolderBesideTheEmpire(fillers.take(1))
      .edifice(ToweringRampartCard, EdificeSide.Ruined, at = shore)
      .warbandsAt(dunes, p2, 1)
      .edifice(WildPasturesCard, EdificeSide.Intact, at = dunes).ready
    def slot(site: SiteId) =
      DecisionOption.SiteSlot(DecisionOptionRef.SiteSlot(site, 0))
    assertEquals(PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(
      searchedAndParkedOnPlacement(ready).state, walkerPowers)), Vector(slot(home),
      DecisionOption.Replacing(slot(peaks), fillers(0)),
      DecisionOption.Replacing(slot(shore), ToweringRampartCard.id),
      DecisionOption.Unavailable(DecisionOption.Replacing(slot(dunes),
        WildPasturesCard.id),
        OptionReason.Written("Locked: cannot be discarded"))))

  test("its discard holds only at Imperial sites: a full pawn site the " +
      "Empire does not hold offers no Replace"):
    val ready = fillers.foldLeft(councilHolderBesideTheEmpire(Vector.empty))(
      (table, card) => table.denizen(card, at = home)).ready
    assert(!SiteRulers.isImperialSite(ready, home))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("its discard holds at a pawn site the Empire holds too"):
    val ready = councilHolderBesideTheEmpire(Vector.empty)
      .warbandsAt(home, p2, 1).denizen(fillers(1), at = home).ready
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)).take(2), Vector(
      DecisionOptionRef.SiteSlot(home, 1) -> None,
      DecisionOptionRef.SiteSlot(home, 0) -> Some(fillers(1))))

  test("a play to a distant Imperial site in place of its card discards " +
      "that card to the pile after the site's region, with the Grand " +
      "Council's line"):
    val ready = councilHolderBesideTheEmpire(fillers.take(1)).ready
    val asked = searchedAndParkedOnPlacement(ready)
    assertEquals(councilSaid(asked.events), Vector.empty)
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite,
      fillers(0)))
    val end = SearchFixture.after(done)
    assertEquals(Look(end).cardsAt(peaks), Vector[CardId](played))
    assert(Look(end).discardAfter(peaks).contains(fillers(0)))
    assertEquals(Look(end).pawn(p1), home)
    assertEquals(councilSaid(done.events), Vector(NoteText.Said(
      "discard-first", s"May discard a card at ${peaks.value} first.",
      covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready,
      asked.events ++ done.events), end)
    assertWireRoundTrips(asked.events ++ done.events)

  test("a play to an Imperial site's empty slot discards nothing and " +
      "writes no line"):
    val asked = searchedAndParkedOnPlacement(councilHolderBesideTheEmpire(Vector.empty).ready)
    val done = accepted(placeAt(asked, played,
      DecisionOptionRef.SiteSlot(shore, 0)))
    assertEquals(Look(SearchFixture.after(done)).cardsAt(shore),
      Vector[CardId](played))
    assertEquals(councilSaid(done.events), Vector.empty)

  test("with no Imperial site on the map, as with no Empire, only the pawn " +
      "site is offered and nothing there may be replaced"):
    val ready = Table.start.pawn(p1, home).worldDeckTop(played).supply(p1, 5)
      .peoplesFavor(Some(p1), 1, PeoplesFavorFace.GrandCouncil)
      .denizen(fillers(0), at = home).ready
    assertEquals(SiteRulers.imperialSites(ready), Vector.empty)
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 1) -> None))

  test("nobody else plays to the Imperial sites: not another holder, not " +
      "an unheld banner, not the Mob face"):
    val others: Vector[(String, Table => Table)] = Vector(
      "held by p3" -> (_.peoplesFavor(Some(p3), 1,
        PeoplesFavorFace.GrandCouncil)),
      "unheld" -> (_.peoplesFavor(None, 1, PeoplesFavorFace.GrandCouncil)),
      "Mob face" -> (_.peoplesFavor(Some(p1), 1)))
    others.foreach { case (label, favor) =>
      val ready = favor(councilHolderBesideTheEmpire(Vector.empty)).ready
      assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)),
        Vector(DecisionOptionRef.SiteSlot(home, 0) -> None), label)
    }

  test("it applies to a facedown adviser played to a site as well"):
    val ready = PowerFixture.asAdviser(
      councilHolderBesideTheEmpire(fillers.take(1)).ready, played,
      Orientation.FaceDown)
    val started = accepted(FacedownAdviserPlay.parkedOnPlacement(rules,
      Ready(ready), p1, played))
    assertEquals(slotsOf(started), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> Some(fillers(0)),
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("the Grand Mask leaves the Imperial sites Imperial: its wearer may " +
      "still play to them"):
    val ready = councilHolderBesideTheEmpire(Vector.empty)
      .relic(p1, GrandMaskCard).ready
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))
```

**In** `src/test/scala/distantlands/gameplay/powers/action/TempleOfAncestorsSuite.scala`
(a power's own play that only goes to the advisers still shows the Grand
Council's sites, refused with the power's reason; Ruling 1):

Replace

```scala
import distantlands.gameplay.cards.AncientCitySite
```

with

```scala
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
```

Then replace

```scala
  test("played faceup it becomes an adviser; replay and the wire agree"):
```

with

```scala
  test("a Grand Council holder's distant Imperial site is shown, refused " +
      "with the Temple's reason as the pawn site is"):
    val ready = Table.from(staged()).imperial(Table.p2)
      .peoplesFavor(Some(actor), 1, PeoplesFavorFace.GrandCouncil)
      .warbandsAt(BrokenPeaksSite, Table.p2, 1).ready
    val t = accepted(start(ready))
    val refused = OptionReason.Written(TempleOfAncestors.toAdvisers)
    assertEquals(PlacementPicks.siteSlotOptions(options(t)).map {
      case DecisionOption.Unavailable(option, reason) =>
        option.ref -> Some(reason)
      case option => option.ref -> None
    }, Vector(
      DecisionOptionRef.SiteSlot(AncientCitySite.id, 1) -> Some(refused),
      DecisionOptionRef.SiteSlot(BrokenPeaksSite.id, 0) -> Some(refused)))

  test("played faceup it becomes an adviser; replay and the wire agree"):
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.PeoplesFavorGrandCouncilSuite distantlands.gameplay.powers.action.TempleOfAncestorsSuite"`
Expected: FAIL. The test sources do not compile: `PeoplesFavorGrandCouncil`
is not found. (Once they compile without the power, the Temple test fails
too: only Ancient City's slot is shown.)

- [ ] **Step 3: Implement**

Create `src/main/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncil.scala`:

```scala
package distantlands.gameplay.powers.banner

import distantlands.gameplay.SiteRulers
import distantlands.gameplay.actions.{CardPlayRules, DiscardPermission,
  PlaySites}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.model._

/** Grand Council (the Banner of the People's Favor, Grand Council face):
  * "When playing a card, you may play it to any Imperial site on the map,
  * and you may discard a denizen from that site first." (Foundations
  * design, slice 5).
  *
  * A persistent rule of the banner's holder, automatic, at the
  * `SearchPlayAdviser` window beside the Mob's, so it reaches every card
  * play the engine plans: a Search's, the facedown-adviser play's and a
  * power's own. It opens every Imperial site as a place to play the card,
  * after the pawn site, and adds one discard that holds only at an
  * Imperial site, the pawn site included when it is one. The generic
  * discard rules decide which cards may go: a ruined edifice may, an
  * intact edifice or a locked card may not. A full Imperial site takes the
  * play through that discard.
  *
  * An Imperial site is one the Empire's warbands hold
  * (`SiteRulers.isImperialSite`), read on the board the play is planned
  * on. The Grand Mask changes who rules the cards there, not the site.
  *
  * Its line, "May discard a card at {site} first.", travels in the
  * permission, and card play writes it for the discard it allowed, as for
  * the Mob's.
  */
object PeoplesFavorGrandCouncil extends ContributingPower:
  val id: PowerId = PowerId("banner.peoples-favor.grand-council")
  def source: RuleSourceRef = RuleSourceRef.Banner(Banner.PeoplesFavor.key)

  /** The player plays a card while holding the banner, Grand Council face
    * up. */
  override def applicable(ctx: PowerCtx): Boolean =
    val favor = ctx.state.game.current.banners.peoplesFavor
    favor.active == PeoplesFavorFace.GrandCouncil &&
      favor.holder.contains(ctx.actor)

  /** Whether the Empire's warbands hold `site` on the board the play is
    * planned on. */
  val imperialSite: (ReadyGame, SiteId) => Boolean =
    (ready, site) => SiteRulers.isImperialSite(ready, site)

  private val notes = NoteSet(id)
  notes.include(CardPlayRules.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree =>
          tree.adjust(children)(_.openSites(PlaySites(imperialSite))
            .permitSiteDiscard(DiscardPermission.lined(id,
              PowerSourceRef.Banner(Banner.PeoplesFavor),
              appliesAt = imperialSite)))
        case _ => children)))
```

**In** `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala`:

Replace

```scala
    Vector[ContributingPower](PeoplesFavorMob)
```

with

```scala
    Vector[ContributingPower](PeoplesFavorMob, PeoplesFavorGrandCouncil)
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.PeoplesFavorGrandCouncilSuite distantlands.gameplay.powers.action.TempleOfAncestorsSuite"`
Expected: PASS (11 Grand Council tests and every Temple test).

- [ ] **Step 5: Rewrite the note-key snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: FAIL, "every power declares exactly the notes the snapshot
records": the new power declares two keys the snapshot lacks.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS, 3 tests.

Run: `git diff -- src/test/resources` and `git status --short src/test/resources`
Expected: only `src/test/resources/notes/declared-keys.txt` changed, and
its diff is exactly these two added lines, after the Wandering Flame's and
before the Mob's:

```diff
 banner.darkest-secret.wandering-flame.move used = {0} moved to {1}.
 banner.darkest-secret.wandering-flame.place used = {0} placed a secret at {1}.
+banner.peoples-favor.grand-council bury-first = May bury a card at {0} first.
+banner.peoples-favor.grand-council discard-first = May discard a card at {0} first.
 banner.peoples-favor.mob bury-first = May bury a card at {0} first.
 banner.peoples-favor.mob discard-first = May discard a card at {0} first.
```

- [ ] **Step 6: Run the neighbours**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.* distantlands.gameplay.powers.action.TempleOfAncestorsSuite distantlands.gameplay.powers.search.BaronSuite distantlands.gameplay.PhasePowerSuite distantlands.gameplay.RuleSourceIndexSuite distantlands.gameplay.BackendArchitectureSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS. `PeoplesFavorMobSuite`'s "nobody else may replace: ... not
the Grand Council face" still passes: its pawn site is not Imperial.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncil.scala src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala src/test/scala/distantlands/gameplay/powers/banner/PeoplesFavorGrandCouncilSuite.scala src/test/scala/distantlands/gameplay/powers/action/TempleOfAncestorsSuite.scala src/test/resources/notes/declared-keys.txt
git commit -m "feat(banner): the Grand Council plays to any Imperial site" -m "The People's Favor's Grand Council face is a persistent rule of its holder at the card play's SearchPlayAdviser window, beside the Mob's: every Imperial site opens as a place to play the card, and a discard there is permitted, a ruined edifice included and an intact edifice or a locked card not. The discard writes the Grand Council's line, as the Mob's writes its own." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The Grand Council's ribbons, for Challenge and every other take

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/BannerRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/FavoredSon.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala`
- Test: `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/FavoredSonSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/VisionPlaySuite.scala`

**Interfaces:**
- Consumes: `BannerRules.raidFavorReturn(banks, amount): Vector[Suit]`,
  `BannerRules.addFavor`; the test kit's `Table.peoplesFavor(holder, favor,
  face)`, `Table.darkestSecret(holder, secrets, face)`,
  `CampaignFixture.{raidBoard, on, RaidBoard}`, `ActPowerHarness.replayed`.
- Produces, in `object BannerRules`:
  - `def favorToPreviousHolder(favor: PeoplesFavorState): Int`;
  - `def peoplesFavorRibbon(ready: ReadyGame): Vector[CoreOperation]`.
  `ChallengeRibbon.peoplesFavorReturns` is deleted.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`:

Replace

```scala
import distantlands.testkit.{EmptyWindowModifier, Journal, Look, MenuPlay,
  Outcomes, Table}
```

with

```scala
import distantlands.testkit.{ActPowerHarness, EmptyWindowModifier, Journal,
  Look, MenuPlay, Outcomes, Table}
```

Then replace

```scala
  test("Wandering Flame: an unclaimed banner places half on a unique least site one at a time and burns the rest"):
```

with

```scala
  test("Grand Council: an enemy holder takes back half the favor, rounded " +
      "down, and the rest returns to the least banks; on the Mob they take " +
      "back none"):
    def enemyHolds(face: PeoplesFavorFace): ReadyGame =
      challenger.peoplesFavor(Some(p2), favor = 5, face = face)
        .pawn(p2, at = Table.quietPawnSite(p1)).ready
    val board = enemyHolds(PeoplesFavorFace.GrandCouncil)
    val started = accepted(start(board))
    val chosen = accepted(chooseBanner(started.state, peoplesFavorOption))
    val done = accepted(chooseAmount(chosen.state, 6))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).favor(p2), Look(board).favor(p2) + 5 / 2)
    assertEquals(after.banks.favor, BannerRules.addFavor(board.banks.favor,
      BannerRules.raidFavorReturn(board.banks.favor, 5 - 5 / 2)))
    assertEquals(after.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p1), 6))
    assertEquals(Look(after).favor(p1), 0)
    assertEquals(ActPowerHarness.replayed(rules, board,
      started.events ++ chosen.events ++ done.events), after)
    val mob = enemyHolds(PeoplesFavorFace.Mob)
    val mobDone = Outcomes.ready(accepted(chooseAmount(
      atAmount(mob, peoplesFavorOption), 6)).state)
    assertEquals(Look(mobDone).favor(p2), Look(mob).favor(p2))

  test("Grand Council: an unclaimed banner has no previous holder, so all " +
      "its favor returns to the least banks"):
    val board = challenger.peoplesFavor(None, favor = 3,
      face = PeoplesFavorFace.GrandCouncil).ready
    val after = Outcomes.ready(accepted(chooseAmount(
      atAmount(board, peoplesFavorOption), 4)).state)
    assertEquals(after.banks.favor, BannerRules.addFavor(board.banks.favor,
      BannerRules.raidFavorReturn(board.banks.favor, 3)))
    assertEquals(after.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p1), 4))

  test("the Festival face still refuses the start"):
    val board = challenger.peoplesFavor(None, favor = 2)
      .darkestSecret(None, 1, face = DarkestSecretFace.Festival).ready
    assertEquals(start(board).left.toOption, Some(
      OathViolation.UnsupportedBannerState("unsupported active banner face")))

  test("Wandering Flame: an unclaimed banner places half on a unique least site one at a time and burns the rest"):
```

**In** `src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala`:

Replace

```scala
  test("the pawn cannot be relocated to its own site"):
```

with

```scala
  test("a Raid victory over the Grand Council gives the defender back " +
      "half its favor, rounded down, before they burn half of theirs"):
    val RaidBoard(raided, relic) = raidBoard()
    val b = on(raided)(_.peoplesFavor(Some(raided.rival), favor = 3,
      face = PeoplesFavorFace.GrandCouncil))
    val sacrificed = walk(b, relic, attack = 4).sacrificed
    val after = ready(sacrificed.state)
    assertEquals(after.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(b.actor), 0))
    // 3 / 2 = 1 of the banner's favor goes back to the rival, 2 to the banks.
    assertEquals(favorInBanks(sacrificed.state),
      favorInBanks(Ready(b.ready)) + 2)
    // The rival's 5 favor and the 1 returned: they burn half of 6.
    assertEquals(Look(sacrificed.state).player(b.rival).board.favor, 6 - 6 / 2)

  test("the pawn cannot be relocated to its own site"):
```

**In** `src/test/scala/distantlands/gameplay/powers/whenplayed/FavoredSonSuite.scala`:

Replace

```scala
  test("takes an unheld People's Favor from the shared bank"):
```

with

```scala
  test("takes the Grand Council from its holder, who gets back half its " +
      "favor, rounded down, by its left ribbon"):
    val ready = Table.start.adviser(p1, son).peoplesFavor(Some(p2),
      favor = 3, face = PeoplesFavorFace.GrandCouncil).ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(Look(done.treeless).favor(p2), Look(ready).favor(p2) + 1)
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 2))
    assertJournalRebuilds(ready, done.events, done.treeless)

  test("takes an unheld People's Favor from the shared bank"):
```

**In** `src/test/scala/distantlands/gameplay/VisionPlaySuite.scala`:

Replace

```scala
  test("Reveal is a facedown Vision played faceup: it replaces the revealed " +
```

with

```scala
  test("a Conspiracy that takes the Grand Council gives its holder back " +
      "half the favor, rounded down, by its right ribbon"):
    // p2, the enemy, shares p1's site and holds the People's Favor on its
    // Grand Council face with 3 favor.
    val enemy = p2
    val initial = atHome.pawn(enemy, at = home)
      .peoplesFavor(Some(enemy), favor = 3,
        face = PeoplesFavorFace.GrandCouncil)
      .darkestSecret(None, secrets = 1).worldDeckTop(conspiracy).ready
    val started = accepted(
      SearchPlay.searched(rules, OathState.Ready(initial), actor))
    val parked = accepted(
      rules.resolveWalker(started.state, actor, placeId(conspiracy),
        faceup(started.state)))
    val resolved = accepted(rules.resolveWalker(parked.state, actor,
      "cardplay.conspiracy.target", DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Banner(Banner.PeoplesFavor))))
    val after = Outcomes.ready(resolved.state)
    assertEquals(after.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(actor), 0))
    assertEquals(Look(after).favor(enemy), Look(initial).favor(enemy) + 1)
    assertEquals(after.banks.favor.values.sum,
      initial.banks.favor.values.sum + 2)

  test("Reveal is a facedown Vision played faceup: it replaces the revealed " +
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.ChallengeProcedureSuite distantlands.gameplay.CampaignRaidSuite distantlands.gameplay.powers.whenplayed.FavoredSonSuite distantlands.gameplay.VisionPlaySuite"`
Expected: FAIL, 5 tests: both Grand Council Challenge tests (the start is
refused with "unsupported active banner face"), the Raid test, the
Favored Son test and the Conspiracy test (each obtains the Mob's ribbon:
the holder gets no favor back and the banks get all of it). "the Festival
face still refuses the start" passes before and after.

- [ ] **Step 3: Implement the shared ribbon**

**In** `src/main/scala/distantlands/gameplay/actions/BannerRules.scala`:

Replace

```scala
/** Shared printed banner invariants. Mob and Raid both use the printed
  * deterministic leftmost policy. Wandering Flame site ties belong to the player.
  */
```

with

```scala
/** Shared printed banner invariants. The People's Favor's ribbons, on
  * both faces, use the printed deterministic leftmost policy. Wandering
  * Flame site ties belong to the player.
  */
```

Then replace

```scala
  def raidFavorReturn(banks: Map[Suit, Int], amount: Int): Vector[Suit] =
    (0 until amount).foldLeft(Vector.empty[Suit]) { (order, _) =>
      val current = addFavor(banks, order)
      order :+ leastFavorBanks(current).head
    }
```

with

```scala
  def raidFavorReturn(banks: Map[Suit, Int], amount: Int): Vector[Suit] =
    (0 until amount).foldLeft(Vector.empty[Suit]) { (order, _) =>
      val current = addFavor(banks, order)
      order :+ leastFavorBanks(current).head
    }

  /** The favor the People's Favor's ribbon gives back to the banner's
    * previous holder. The Mob's ribbons print ALL to the banks, so none.
    * The Grand Council's print HALF to the previous holder, then REST to
    * the banks, so half, rounded down. Each face prints the same left and
    * right ribbon. An unheld banner has no previous holder: the returned
    * half goes to the banks with the rest. */
  def favorToPreviousHolder(favor: PeoplesFavorState): Int =
    favor.active match
      case PeoplesFavorFace.Mob => 0
      case PeoplesFavorFace.GrandCouncil =>
        if favor.holder.isEmpty then 0 else favor.favor / 2

  /** The People's Favor's ribbon, as whoever takes the banner adjusts it:
    * a Challenge and Favored Son by its left ribbon, a Raid and the
    * Conspiracy by its right. The previous holder's share moves first
    * (`favorToPreviousHolder`), then every other favor goes to the
    * least-favor banks one at a time, leftmost first. */
  def peoplesFavorRibbon(ready: ReadyGame): Vector[CoreOperation] =
    val favor = ready.game.current.banners.peoplesFavor
    val kept = favorToPreviousHolder(favor)
    val banner = PositionedLocation(Location.OnBanner(Banner.PeoplesFavor))
    favor.holder.filter(_ => kept > 0).toVector.map(holder =>
      Move(Piece.Favor(kept), banner,
        PositionedLocation(Location.PlayArea(holder))): CoreOperation) ++
      raidFavorReturn(ready.banks.favor, favor.favor - kept).map(suit =>
        Move(Piece.Favor(1), banner,
          PositionedLocation(Location.FavorBank(suit))))
```

**In** `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala`:

Replace

```scala
  * People's Favor returns every favor to the least-favor banks, leftmost
  * first. Wandering Flame first gives the previous holder the rest beyond
```

with

```scala
  * People's Favor follows its face's ribbon (`BannerRules.peoplesFavorRibbon`):
  * the Mob returns every favor to the least-favor banks, leftmost first,
  * and the Grand Council gives half to the previous holder first. Wandering
  * Flame first gives the previous holder the rest beyond
```

Then replace

```scala
      Vector(BuildOps((ready, _) => Right(peoplesFavorReturns(ready))))
```

with

```scala
      Vector(BuildOps((ready, _) =>
        Right(BannerRules.peoplesFavorRibbon(ready))))
```

Then replace (the whole method and the blank line after it, so that the
next doc comment follows the `steps` method's last line after one blank
line)

```scala
  /** The People's Favor's ribbon: every favor on it returns to the
    * least-favor banks, leftmost first, one at a time. Favored Son's take
    * carries it as its `leaving` (catalog batch 10, slice 11). */
  def peoplesFavorReturns(ready: ReadyGame): Vector[CoreOperation] =
    val amount = BannerRules.resources(ready.game.current, Banner.PeoplesFavor)
    BannerRules.raidFavorReturn(ready.banks.favor, amount).map(suit =>
      Move(Piece.Favor(1),
        PositionedLocation(Location.OnBanner(Banner.PeoplesFavor)),
        PositionedLocation(Location.FavorBank(suit))))

```

with nothing.

**In** `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala`:

Replace

```scala
  /** The ribbon rules below are the printed ones for People's Favor `Mob` and
    * Darkest Secret `WanderingFlame` only.
    */
  private def supportedFaces(state: ReadyGame): Either[OathViolation, Unit] =
    val banners = state.game.current.banners
    Either.cond(banners.peoplesFavor.active == PeoplesFavorFace.Mob &&
      banners.darkestSecret.active == DarkestSecretFace.WanderingFlame, (),
```

with

```scala
  /** The ribbon rules below are the printed ones for both People's Favor
    * faces and for the Darkest Secret's `WanderingFlame` only: the Festival
    * arrives with Foundations slice 6.
    */
  private def supportedFaces(state: ReadyGame): Either[OathViolation, Unit] =
    val banners = state.game.current.banners
    Either.cond(
      banners.darkestSecret.active == DarkestSecretFace.WanderingFlame, (),
```

(The line after the block, `OathViolation.UnsupportedBannerState("unsupported active banner face"))`,
stays as it is.)

- [ ] **Step 4: Point the other takes at it**

**In** `src/main/scala/distantlands/gameplay/powers/whenplayed/FavoredSon.scala`:

Replace

```scala
import distantlands.gameplay.actions.challenge.ChallengeRibbon
```

with nothing (delete the line).

Then replace

```scala
  * Darkest Secret. Its `leaving` is the People's Favor ribbon a Challenge
  * walks (`ChallengeRibbon.peoplesFavorReturns`), so the favor returns to
  * the banks only if the take happens. It is not walked under the
  * `ChallengeRibbon` window, so Secret Testament, a Challenge modifier,
```

with

```scala
  * Darkest Secret. Its `leaving` is the People's Favor ribbon a Challenge
  * walks (`BannerRules.peoplesFavorRibbon`), so the favor returns to the
  * banks, and on the Grand Council half of it to the previous holder, only
  * if the take happens. It is not walked under the `ChallengeRibbon`
  * window, so Secret Testament, a Challenge modifier,
```

Then replace

```scala
        leaving = ChallengeRibbon.peoplesFavorReturns(live))))),
```

with

```scala
        leaving = BannerRules.peoplesFavorRibbon(live))))),
```

**In** `src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala`:

Replace

```scala
            case Banner.PeoplesFavor => BannerRules.raidFavorReturn(
              ready.banks.favor, BannerRules.resources(current, held))
              .map(suit => Move(Piece.Favor(1),
                PositionedLocation(Location.OnBanner(held)),
                PositionedLocation(Location.FavorBank(suit))))
```

with

```scala
            case Banner.PeoplesFavor => BannerRules.peoplesFavorRibbon(ready)
```

**In** `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala`:

Replace

```scala
          case Banner.PeoplesFavor =>
            val returned = BannerRules.raidFavorReturn(ready.banks.favor,
              BannerRules.resources(current, banner)).groupBy(identity)
              .view.mapValues(_.size).toMap
            Suit.all.flatMap(suit => returned.get(suit).filter(_ > 0).map(count =>
              Move(Piece.Favor(count),
                PositionedLocation(Location.OnBanner(banner)),
                PositionedLocation(Location.FavorBank(suit))): CoreOperation))
```

with

```scala
          case Banner.PeoplesFavor => BannerRules.peoplesFavorRibbon(ready)
```

Then replace

```scala
    * aside, half the favor burnt, and a facedown Conspiracy returned to
    * the box. Read after the takes, which change none of it. */
```

with

```scala
    * aside, half the favor burnt, and a facedown Conspiracy returned to
    * the box. Read after the takes: the Grand Council's ribbon gives the
    * defender back half its favor first, and that favor counts toward the
    * half they burn. */
```

- [ ] **Step 5: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.ChallengeProcedureSuite distantlands.gameplay.CampaignRaidSuite distantlands.gameplay.powers.whenplayed.FavoredSonSuite distantlands.gameplay.VisionPlaySuite distantlands.gameplay.powers.banner.SecretTestamentSuite"`
Expected: PASS (45 tests in the plan's own run).

Run: `./sbtw "testOnly distantlands.application.gamelog.* distantlands.gameplay.Campaign* distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS (424 tests in the plan's own run). No golden changes: the
Raid's lines read the state, not its moves. If `GameLogGoldenSuite`
fails, stop and report; do not rewrite a golden.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/BannerRules.scala src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/distantlands/gameplay/powers/whenplayed/FavoredSon.scala src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala src/test/scala/distantlands/gameplay/powers/whenplayed/FavoredSonSuite.scala src/test/scala/distantlands/gameplay/VisionPlaySuite.scala
git commit -m "feat(challenge): the Grand Council's ribbons return half its favor to its holder" -m "Both ribbons of the Grand Council print HALF to the banner's previous holder, then REST to the favor banks; the Mob's print ALL to the banks. BannerRules.peoplesFavorRibbon now reads the face, and a Challenge, Favored Son, a Raid and the Conspiracy all adjust the banner through it. Challenge no longer refuses the Grand Council face; it still refuses the Festival." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Docs and verification

**Files:**
- Modify: `docs/ROADMAP.md`
- Modify: `docs/architecture/bounded-banners.md`
- Modify: `docs/architecture/bounded-visions-and-conspiracy.md`
- Modify: `docs/architecture/all-exile-round-endings.md`
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md`

**Interfaces:**
- Consumes: everything Tasks 1 to 3 built; nothing new is produced.

Docs are plain prose. `docs/rules/ambiguities.md` and
`docs/rules/implementation-traceability.md` hold very long lines (numbered
items and table rows), so their blocks below are parts of a line, not
whole lines, and several blocks in the other docs start or end inside a
line too: replace exactly the text shown and leave the rest of the line.
The roadmap deletes finished work outright: no checked box and no "landed"
note.

- [ ] **Step 1: The roadmap, the architecture docs, the ambiguities and the traceability table**

**In** `docs/ROADMAP.md`:

Replace

~~~markdown
- [ ] **The Grand Council and Festival banner faces.** Altered Foundations V
  and VI turn the banners to these faces. Their ids are in
  `RuleSourceIndex`, and the faces have no behaviour.
~~~

with

~~~markdown
- [ ] **The Festival banner face.** Altered Foundation VI turns the
  Darkest Secret to it. Its id is in `RuleSourceIndex`, and the face has
  no behaviour.
~~~

**In** `docs/architecture/bounded-banners.md`:

Replace

~~~markdown
pp. 12 and 15 for the fixed first-game banner faces: Mob and Wandering Flame.
The face identities are typed state. The Grand Council and Festival faces have
no implemented power: `RuleSourceIndex` lists their handler ids, and they are
ignored and recorded like any other unimplemented power.
~~~

with

~~~markdown
pp. 12 and 15 for the first-game banner faces, Mob and Wandering Flame, and
for the Grand Council: an altered Foundation V starts the People's Favor on
its Grand Council face
([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md),
slice 5). The face identities are typed state. The Festival face has no
implemented power: `RuleSourceIndex` lists its handler id, and it is
ignored and recorded like any other unimplemented power.
~~~

Then replace

~~~markdown
and the one-at-a-time favor return. The People's Favor ribbon, Campaign Raid
and Conspiracy share that favor return (`raidFavorReturn`), including the
printed leftmost tied-bank rule.
~~~

with

~~~markdown
and the one-at-a-time favor return. The People's Favor's ribbon
(`peoplesFavorRibbon`) reads the face, and Challenge, Favored Son, Campaign
Raid and Conspiracy all adjust the banner through it, including the printed
leftmost tied-bank rule.
~~~

Then replace

~~~markdown
Its start gates refuse any face other than Mob and Wandering
Flame (`UnsupportedBannerState`)
~~~

with

~~~markdown
Its start gates refuse the Festival face
(`UnsupportedBannerState`)
~~~

(The block ends inside its sentence: leave " and a start with no legal
banner." after it as it is.)

Then replace

~~~markdown
- People's Favor returns every favor to the least-favor banks, leftmost first.
~~~

with

~~~markdown
- People's Favor on the Mob returns every favor to the least-favor banks,
  leftmost first. On the Grand Council the previous holder first takes back
  half, rounded down; an unheld banner's favor all goes to the banks.
~~~

Then replace

~~~markdown
The Mob face is a contribution (`PeoplesFavorMob`): when its holder plays a
card to a site, they may first discard a card there.
~~~

with

~~~markdown
The Mob face is a contribution (`PeoplesFavorMob`): when its holder plays a
card to a site, they may first discard a card there. The Grand Council face is
another (`PeoplesFavorGrandCouncil`): its holder may also play a card to any
Imperial site, and may first discard a card there, but at no other site.
~~~

(The paragraph goes on with " The Wandering Flame face" on the same line;
leave it.)

**In** `docs/architecture/bounded-visions-and-conspiracy.md`:

Replace

~~~markdown
Altered Foundations, Empire-only Vision rules, and the alternate banner faces
(Grand Council, Festival) have no behaviour.
~~~

with

~~~markdown
Foundation III's faces, Empire-only Vision rules, and the Festival banner
face have no behaviour. A Conspiracy that takes the People's Favor adjusts it
by its face's right ribbon (`BannerRules.peoplesFavorRibbon`).
~~~

(The paragraph goes on with " `RuleSourceIndex` lists the" on the same
line; leave it.)

**In** `docs/architecture/all-exile-round-endings.md`:

Replace

~~~markdown
The Grand Council and Festival
banner faces and the Foundation faces have no implemented power
~~~

with

~~~markdown
The Festival
banner face and the Foundation faces have no implemented Rest power
~~~

**In** `docs/architecture/game-setup.md`:

Replace

~~~markdown
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled. The sites Imperial Maps places in
~~~

with

~~~markdown
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled. An altered Foundation V, Grand
Council, starts the People's Favor on its Grand Council face, as Mob's
Favor starts it on the Mob. The sites Imperial Maps places in
~~~

**In** `docs/rules/ambiguities.md` (item 2 of "Component-data
dependencies"):

Replace

~~~markdown
The altered faces of V and VI are refused at game start until their slices of the [Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md), and
~~~

with

~~~markdown
An altered V, Grand Council, starts the People's Favor on its Grand Council face; the altered face of VI is refused at game start until its slice of the [Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md), and
~~~

**In** `docs/rules/implementation-traceability.md`:

Replace

~~~markdown
The altered faces of Foundations V and VI; legacies
~~~

with

~~~markdown
The altered face of Foundation VI; legacies
~~~

Then replace

~~~markdown
its discard piles dealt in turn and its Visions-only world deck (`gameplay/setup/SetupDealSuite.scala`, `AlteredFoundationSetupSuite.scala`).
~~~

with

~~~markdown
its discard piles dealt in turn and its Visions-only world deck (`gameplay/setup/SetupDealSuite.scala`, `AlteredFoundationSetupSuite.scala`). Grand Council, altered Foundation V, starts the People's Favor on its Grand Council face (`FirstGameSetupMaterializer`, `AlteredFoundationSetupSuite.scala`).
~~~

Then replace

~~~markdown
Altered Foundations V and VI are refused at game start until their slices of the
~~~

with

~~~markdown
Altered Foundation VI is refused at game start until its slice of the
~~~

Then replace

~~~markdown
| Challenge and banner resource actions | **Tested (fixed first-game Mob and Wandering Flame faces)** |
~~~

with

~~~markdown
| Challenge and banner resource actions | **Tested (Mob, Grand Council and Wandering Flame faces)** |
~~~

Then replace

~~~markdown
deterministically returns Mob favor one at a time with leftmost ties,
~~~

with

~~~markdown
deterministically returns the People's Favor's favor one at a time with leftmost ties, the Grand Council first giving half, rounded down, to its previous holder (`BannerRules.peoplesFavorRibbon`),
~~~

Then replace

~~~markdown
The two printed placement powers are typed 0-Supply minor actions. Campaign Raid and Challenge share banner distribution helpers
~~~

with

~~~markdown
The two printed placement powers are typed 0-Supply minor actions. The Grand Council lets its holder play a card to any Imperial site and discard a card at one first (`PeoplesFavorGrandCouncil`, `PeoplesFavorGrandCouncilSuite`). Campaign Raid, Conspiracy, Favored Son and Challenge share banner distribution helpers
~~~

Then replace

~~~markdown
| Altered banner faces, mutable Foundation selection,
~~~

with

~~~markdown
| The Festival face, mutable Foundation selection,
~~~

Then replace

~~~markdown
| Altered Foundations, alternate banner faces and other Vision/reveal modifiers remain deferred;
~~~

with

~~~markdown
| Foundation III's faces, the Festival banner face and other Vision/reveal modifiers remain deferred;
~~~

- [ ] **Step 2: The spec**

The status line, and the slice 5 section: the ribbons as read, what was
built, and the readings taken, for the owner to confirm.

**In** `docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b and 4 landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b, 4 and 5 landed.
~~~

Then replace

~~~markdown
- **Challenge.** `ChallengeProcedure.supportedFaces` stops refusing the
  face. The plan reads the face's ribbon icons from
  `reference/2026-03-17-new-foundations-rev1/banners.pdf`.
- **Tests.** Playing to a distant Imperial site; a discard there and none
  elsewhere; no Imperial sites in a no-Empire game; a Challenge of the face.
~~~

with

~~~markdown
- **Challenge.** `ChallengeProcedure.supportedFaces` stops refusing the
  face; it still refuses the Festival. The face's ribbons, read from
  `reference/2026-03-17-new-foundations-rev1/banners.pdf`, print the same
  on the left (Challenge) and the right (Campaign): "HALF" over the
  return-favor icon, then "REST" over the bank-favor icon. Half the favor,
  rounded down, goes back to the banner's previous holder, and the rest
  to the least-favor banks one at a time. The Mob prints "ALL" over the
  bank-favor icon on both ribbons.
- **Tests.** Playing to a distant Imperial site; a discard there and none
  elsewhere; no Imperial sites in a no-Empire game; a Challenge of the face.
- **As built.** Game start reads Foundation V from the faces it resolved
  (`FirstGameSetupMaterializer`), so Setup With No Empire, which erodes V
  first, always starts the Mob. `PeoplesFavorGrandCouncil` sits beside
  `PeoplesFavorMob` in `BannerFacePowers` and uses Baron's seam: one
  `PlaySites` and one `DiscardPermission`, both holding at Imperial sites
  (`SiteRulers.isImperialSite`). Its discard writes the Mob's line,
  credited to the Grand Council. The ribbon is
  `BannerRules.peoplesFavorRibbon`, which reads the face, so a Challenge,
  Favored Son ("its left ribbon"), a Raid and the Conspiracy ("its right
  ribbon") all follow it, and a Raid's defender then burns half their
  favor, the returned half included. The frontend already drew and read
  both faces.
- **Readings taken with the plan,** for the owner to confirm. Every card
  play the engine plans is "playing a card": a Search's, the facedown
  adviser play's and a power's own, as for the Mob. A card goes to a
  distant Imperial site only as it could go to the pawn site: a Vision or
  an adviser-only card never, a site-only card yes. A full Imperial site
  takes the card only in place of one discarded there. The discard is
  optional, one card per play, stacks with the Homeland's, and holds at
  the pawn site only when it is Imperial. Nobody is asked to consent, and
  printed protections and locks still apply. An Imperial site is one the
  Empire's warbands hold when the play is planned; the Grand Mask changes
  who rules its cards, not the site. The holder's role does not matter.
  An unheld Grand Council has no previous holder, so all its favor goes to
  the banks. A first game with an altered V starts the Grand Council in
  the shared bank.
~~~

If the owner has ruled on any reading in "Rulings needing the owner's
review" by the time you run this task, write the ruling in place of the
reading, and say so in your report. If you took a reading of your own
while working, add it to the "Readings taken with the plan" item.

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%. The plan's own run of these tasks at `b3226f6f1` passed
5862 tests at 95.32% and 89.64%.

Run: `./sbtw "frontend/test"` (the frontend suites, which the full run
skips)
Expected: PASS (930 tests in the plan's own run).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. The note-key snapshot was committed with Task 2, and
nothing else changed.

Run: `git grep -n "Grand Council and Festival\|alternate banner faces\|fixed first-game Mob and Wandering Flame\|fixed first-game banner faces\|Altered Foundations V and VI\|altered faces of Foundations V and VI\|altered faces of V and VI\|peoplesFavorReturns" -- docs/rules docs/architecture docs/ROADMAP.md src`
Expected: no output. Nothing still says the Grand Council is missing, and
nothing names the deleted method.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/architecture/bounded-banners.md docs/architecture/bounded-visions-and-conspiracy.md docs/architecture/all-exile-round-endings.md docs/architecture/game-setup.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 5, the Grand Council" -m "The roadmap keeps only the Festival face, the banner and Conspiracy docs describe the Grand Council's power and ribbons, the setup doc says what an altered V starts, the ambiguities and the traceability table stop calling the face refused or missing, and the spec records slice 5 landed, the ribbons as printed, what was built and the readings taken." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## For slice 6 and later

- The Festival's Challenge ribbon removes the marker
  (`banner-removefestival`); `ChallengeProcedure.supportedFaces` then
  refuses nothing and can go, and `ChallengeRibbon.steps`' Darkest Secret
  branch reads the face as `BannerRules.peoplesFavorRibbon` does. The
  Festival's right ribbon matters to `CampaignRaid.takes` and the
  Conspiracy too.
- Legacies that set Foundation V (the catalog's "Set Foundation V to Grand
  Council") add alteration sources only; game start reads the face as
  this slice does.

## Self-review

**Spec coverage** (slice 5):

| Spec statement | Task |
|---|---|
| An altered V sets People's Favor to `GrandCouncil`; its refusal goes | 1 (`peoplesFavorFace`, `StartingFoundations`) |
| Check `OATH-SETUP08b-NF` against Mob's game start: holder, favor, goal | 1 (both Grand Council tests; finding 1; ruling 10) |
| `PeoplesFavorGrandCouncil` at `SearchPlayAdviser` beside the Mob's | 2 |
| `openSites` adds every Imperial site | 2 ("its holder may play to every Imperial site ...") |
| A site discard only when the card goes to an Imperial site | 2 (the two "its discard holds ..." tests) |
| A ruined edifice may go; an intact edifice or a locked card may not | 2 ("a full Imperial site offers ...") |
| The Mob's discard at other sites goes with the Mob face | 2 (the pawn-site test; `PeoplesFavorMobSuite` unchanged) |
| `supportedFaces` stops refusing the face | 3 |
| Ribbon icons read from `banners.pdf` | "What the banner image shows"; 3 |
| Tests: distant Imperial site; discard there and none elsewhere; no Imperial sites with no Empire; a Challenge of the face | 2, 3 |
| The face at game start (holder, favor, goal) | 1 |
| Docs and the full gates | 4 |

**Verified by running:** the prototype was committed per task in a
scratch worktree at `b3226f6f1`; the plan's blocks were then applied in
order, step by step, to a second fresh worktree at `b3226f6f1`. Each
"see it fail" step failed exactly as written, each task's tree matched the
prototype's, and the final tree passed `./sbtw clean coverage test
coverageReport` (5862 tests, 95.32% statements, 89.64% branches),
`./sbtw "frontend/test"` (930 tests) and `scripts/check-architecture.py`,
with `src/test/resources` clean.

**Left for later, on purpose:** the Festival (slice 6); `CONTEXT.md`
(slice 7, as the spec's Docs list says).

**Placeholders:** none. Every step that changes code shows the code, and
the file written by a command shows its expected diff.

**Names used across tasks:** `peoplesFavorFace` (Task 1);
`PeoplesFavorGrandCouncil.id`, `imperialSite` (Task 2);
`BannerRules.favorToPreviousHolder`, `BannerRules.peoplesFavorRibbon`
(Task 3). The suites' helpers name what they build:
`councilHolderBesideTheEmpire`, `searchedAndParkedOnPlacement`, `slotsOf`, `councilSaid`,
`enemyHolds`, and the Temple suite's existing `staged`. Each is spelled the same wherever it appears.
