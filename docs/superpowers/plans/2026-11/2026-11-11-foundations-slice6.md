# Foundations, Slice 6: Festival Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An altered Foundation VI, Festival, starts the game with the
Darkest Secret on its Festival face and its marker beside the map. At the
start of each of their Rests, the holder puts the festival marker over the
cards at their pawn's site. While it lies there, the holder's enemies
cannot place favor or secrets on the denizens at that site or use their
non-persistent powers, unless the holder consents. Every take of the
Darkest Secret by its ribbons follows the Festival's printed ribbons, and
both of them remove the marker from the map. The marker is projected and
drawn on its site.

**Architecture:** Game start reads Foundation VI from the faces it already
resolved, as slice 5 does for Foundation V, and `StartingFoundations` loses
its last refusal. `DarkestSecretState` gains `festival: Option[SiteId]`,
changed only by a new journaled operation, `SetFestivalMarker`. One power
object, `DarkestSecretFestival` (id `banner.darkest-secret.festival`, which
`RuleSourceIndex` already lists for the face), registered beside the Grand
Council in `BannerFacePowers`, is both a contribution at the `RestStart`
window, which Begin Rest now folds, and a power-use restriction in City
Wall's shape whose waiver the holder grants. The Challenge's left ribbon
lives in `ChallengeRibbon`, the right ribbon a Raid and the Conspiracy walk
in `BannerRules.darkestSecretRibbon`. The site projection gains a required
`festival` flag, and the World pane draws a pill in that site's pawn row.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../../specs/2026-11/2026-11-11-foundations-design.md),
sections "Rulings", "Approach", "Slice 5: Grand Council" (its "As built"
and readings, whose `BannerRules.peoplesFavorRibbon` this slice mirrors for
the Darkest Secret), "Slice 6: Festival" and "Slice 7: Shape Foundation"
(and its "Docs" list, which keeps `CONTEXT.md` for the batch's last task).
Slice 5's plan, [Foundations slice 5](2026-11-11-foundations-slice5.md), is
the format this plan follows.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`). The closest
  this slice comes is `src/main/scala/distantlands/model/CoreOperations.scala`,
  which grows to 791 lines, and
  `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`, to
  761. Add nothing to either beyond the blocks below.
- `gameplay` must not read a catalog `.text` or `rulesText`. `model`
  imports nothing from `gameplay`. `gameplay/actions` imports nothing from
  `gameplay/powers` (`scripts/check-architecture.py`). The new power lives
  in `gameplay/powers/banner` and reads `gameplay/operations`,
  `gameplay/powers/action.PawnMoves` and `gameplay/powers/poweruse.Placements`
  (`private[powers]`); the shared ribbon lives in
  `gameplay/actions/BannerRules.scala`.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. A test that names a symbol the task has not built
  yet fails by not compiling, and that is the failure to see. Tests go in
  the repo's existing suites where one covers the subject. A test helper's
  name says what it builds.
- Wire and journal changes need not keep old journals decodable or
  replayable, and every new wire field is required. This slice adds one
  operation, `SetFestivalMarker` (wire kind `set-festival-marker`, its
  `siteId` a string or `null`, always present), and one required field of
  the site projection, `festival` (a boolean). It adds no decision id. The
  power id `banner.darkest-secret.festival` is already listed by
  `RuleSourceIndex`; the power declares the note keys Tasks 2 and 4 show.
- New Foundations rules only. The printed setup card is `OATH-SETUP09b-NF`
  ("Flip the Darkest Secret to its **Festival side**. Place [secret] on it
  from the shared bank. Place the Festival marker nearby. If the Oathkeeper
  goal is the Darkest Secret, give the banner to the Chancellor, if any.
  Otherwise, place it in the shared bank.") in
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (each entry's `text:` comes before its `name:`; read it by id), and the
  banner face is `OATH-B02b-NF` in the same file. The rulebook is
  `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`: "Altered
  Foundation VI: Festival" (about line 366), "Persistent Powers" (about line
  1009), "Taking Banners" (about line 1102), "Festival (Darkest Secret)"
  (about line 1134) and the glossary's "Enemy" (about line 1521). The
  banner's image is page 1 of
  `/Users/roman/projects/oathdigital/reference/2026-03-17-new-foundations-rev1/banners.pdf`
  (`reference/` is gitignored, so a worktree has no copy; read the main
  checkout's file and change nothing there). Read them there; make no web
  call.
- If a rule is unclear while you work, do not guess silently and do not
  stop: take the most literal reading of the New Foundations text, add it
  to the spec's slice 6 readings (Task 6, Step 2), and name it in your
  report.
- An unavailable option is shown disabled with its reason, never hidden.
  An enemy's use of a denizen at the festival shows unavailable and
  askable of the holder ("Cannot use the non-persistent powers of denizens
  at the festival without the Darkest Secret holder's consent."); where
  nobody may be asked, as inside a Campaign, it shows the plain reason.
- A Game Log line omits the actor on their own turn, and its wording stays
  parallel to the lines beside it. The Rest line is sourceless and names
  only the site, as Explore's "Explored Forgotten Site: Found Deep Woods"
  does: "Held the festival at Deep Woods". The ribbons write no line, as
  the other faces' ribbons write none.
- Note keys change in Tasks 2 and 4, and one golden pair is added in Task
  2, exactly as those tasks show. The rewrite commands are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
  After a rewrite, read `git diff -- src/test/resources` and
  `git status --short src/test/resources` line by line against the text the
  task shows. If anything else changed, stop and report: do not commit a
  file you cannot explain. Never run the golden rewrite except in Task 2,
  Step 6: if `GameLogGoldenSuite` fails anywhere else, stop and report.
- Frontend work follows `DESIGN.md` and uses only its existing tokens: the
  marker is the First player badge's pill (`#a98b58` border, no fill,
  `var(--brass-bright)`, 0.72rem, 700). Task 3 records it in `DESIGN.md`.
- The words are "holder" for the player who holds the Darkest Secret,
  "enemy" for every other player (rulebook glossary: "Other players and the
  bandits"), "festival marker" or "marker" for the token, "marked site"
  for the site it lies over, "beside the map" for the marker off the map
  (`None`), and "ribbon" for the printed column of icons that says how a
  taken banner's secrets leave it (left: Challenge; right: Campaign).
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `2fb28fa34` (slice 5's branch head) with the earlier tasks of this plan
  applied, and occurs once in its file. "Then replace" is the next block in
  the same file. A "with" block repeats the unchanged lines of its
  "Replace" block, so an edit that only adds lines still shows where they
  go; "with nothing" deletes the block's lines. "Create" gives a file's full
  text. If a block does not match because slice 5's review fixes moved the
  text, find the same code by its content and make the same change; if the
  code itself is different, stop and report.
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

Work in the worktree `/Users/roman/projects/oathdigital/.worktrees/foundations-s6`,
on the branch `foundations-slice6`, cut from `foundations-slice5` at
`2fb28fa34`. Slice 5's review fixes may have been merged into it since.
Never run a git command that changes state in the main checkout,
`/Users/roman/projects/oathdigital`, which holds another session's work,
and do not touch `/Users/roman/projects/oathdigital/.worktrees/foundations-s5`.

In the worktree, link the main checkout's tooling before the first
`./sbtw`, unless the links are already there:

```bash
cd /Users/roman/projects/oathdigital/.worktrees/foundations-s6
test -e .tooling || ln -s /Users/roman/projects/oathdigital/.tooling .tooling
test -e node_modules || ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor f3136fb3e HEAD && echo slice3b-ok
git merge-base --is-ancestor 2fb28fa34 HEAD && echo slice5-ok
git grep -n "FoundationNumber.VI -> 6)" -- src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala
git grep -n "private def peoplesFavorFace(plan: FirstGameSetupPlan)" -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1))," -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "Vector\[ContributingPower\](PeoplesFavorMob, PeoplesFavorGrandCouncil)" -- src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala
git grep -n "def peoplesFavorRibbon(ready: ReadyGame)" -- src/main/scala/distantlands/gameplay/actions/BannerRules.scala
git grep -n "banners.darkestSecret.active == DarkestSecretFace.WanderingFlame, ()," -- src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala
git grep -n "case object RestStart extends OtherWindow" -- src/main/scala/distantlands/model/PowerWindow.scala
git grep -n "yield Sequence(Vector(EnterPhase(Phase.Rest)))" -- src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala
git grep -n "case object CityWall extends ContributingPower with PowerUseRestriction" -- src/main/scala/distantlands/gameplay/powers/poweruse/CityWall.scala
git grep -n "def onto(ready: ReadyGame, operation: CoreOperation)" -- src/main/scala/distantlands/gameplay/powers/poweruse/Placements.scala
git grep -n "Slices 1, 2, 3a, 3b, 4 and 5 landed" -- docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md
git grep -c "SetFestivalMarker" -- src/main
```

The first must print `slice3b-ok`: `f3136fb3e` is the merge of slice 3b
into `0.2.0-alpha`, and a worktree without it was cut from a stale branch:
stop and report. The second must print `slice5-ok`: `2fb28fa34` is slice
5's head, which this plan was written and applied against. Each grep but
the last must print one line. They name what this plan stands on: the
Festival's game-start refusal Task 1 deletes, slice 5's face reader and the
banners game start builds, the face powers' registration, slice 5's
ribbon, the Challenge face gate Task 5 deletes, the Rest window and the
Begin Rest tree Task 2 changes, City Wall's restriction shape and the
placement reader Task 4 reuses, and the spec's status line. The last must
print nothing and exit with status 1: no festival marker exists yet. If a
grep prints nothing while the others print, slice 5's review moved or
renamed that code: read the file, and go on only if the code the tasks edit
is still there in substance, adapting the blocks that name it.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

The quiet table (`Table.start`) is the fixture's first game with nothing on
the map: p1, p2 and p3 stand at Ancient City, Broken Peaks and Buried
Giant, it is p1's Act, no site holds warbands, and both banners are unheld,
the Darkest Secret on the Wandering Flame with its marker beside the map.
`new OathRules(catalog)` carries no walker power, so a suite that needs the
festival's Rest uses `PowerRules.withWalkerPowers`.

## What the banner image shows

Page 1 of `banners.pdf` is one image of the four banner faces; rendered
(`qlmanage -t -s 2400`) and cropped to each ribbon, it shows the Darkest
Secret's two faces on the top row. The full-size PNGs in
`~/projects/bgs-cards/content/card-images/oath/en-US/` are cut off above
the ribbons' upper icons, so read the PDF. The Festival face (top right)
prints:

- **Left ribbon**, under the scroll icon (Challenge): "ONE" over the burn
  icon (an open, burnt secret, `secretb`), a rule, "REST" over the
  return-secrets icon (a secret under a curved arrow back,
  `banner-returnsecrets`), a rule, then the remove-festival icon (a bar
  with an arrow rising off it, `banner-removefestival`).
- **Right ribbon**, under the 3 defense die (Campaign): "HALF" over the
  burn icon, a rule, then the remove-festival icon.
- **Text:** "REST: You must place the festival marker over the cards at
  your site. Enemies cannot place resources on these cards or use their
  powers, unless they're persistent powers." and "ACTION: Place [secret] on
  the banner."

The Wandering Flame face (top left) prints "HALF" over the scatter-secrets
icon (`banner-scattersecrets`) and "REST" over the return-secrets icon on
the left, and "ALL" over the burn icon on the right, which is what
`ChallengeRibbon` and `CampaignRaid` already do.

The card database's `OATH-B02b-NF` spells the same ribbons out:
"**Challenge**: Burn one [secret] from the banner. Return the rest to the
banner's previous holder. If the banner was in the shared bank, burn those
instead. Remove the festival marker from the map." and "**Campaign**: Burn
half the [secret] on the banner, rounded down. Remove the festival marker
from the map." The rulebook's key ("Taking Banners"): resolve the left
ribbon in a Challenge and the right in a Campaign, top to bottom, rounding
halves down; `banner-removefestival`: "Remove the festival marker from the
map."

So a Challenge of the Festival burns one secret and returns the rest to the
previous holder (an unheld banner's rest burns too), and a Raid or the
Conspiracy burns half, rounded down, and leaves the rest on the banner its
taker gets. Both remove the marker.

## Plan-time findings

The spec was checked against the code at `2fb28fa34`, and the slice was
prototyped there, one commit per task. The blocks below were then applied
in order, step by step, to a fresh worktree at `2fb28fa34`, where each
"see it fail" step failed as written, each task's tree matched the
prototype's, and the full gates passed. Where the spec and the code differ,
this plan follows the code, and Task 6 records it in the spec.

1. **`RestStart` is declared and folded nowhere.** `PowerWindow.RestStart`
   exists, but Begin Rest's tree is one `EnterPhase(Phase.Rest)`, and no
   procedure names the window. Task 2 adds an empty windowed
   `Sequence(Vector.empty, Some(PowerWindow.RestStart))` after the phase
   change, as Setup declares `SetupEnd`. With nothing contributing, Begin
   Rest records the same steps as before: no golden changes.
2. **The Wandering Flame's actions already go with the face.**
   `RuleSourceIndex` lists `WanderingFlameMove` and `WanderingFlamePlace`
   only on the Wandering Flame face, and `BannerFaceProjectionSuite` already
   pins that the Festival face is offered no power. Place Banner Resource
   reads no face, so the Festival's "ACTION: Place [secret] on the banner"
   already works; Task 1 adds that assertion.
3. **Game start loses its last refusal.** With Foundation VI's line gone,
   `StartingFoundations.arrivesWith` is empty, so Task 1 deletes it and
   `refusal`, and their one caller in `GameStartRules`. The line it deletes
   there was the only 8-space continuation in that `for`, and without it
   Scala 3 rejects the `if ... then` enumerators that follow
   (`atlasSites`, `homelands`: "The start of this line does not match any
   of the previous indentation widths"), so Task 1 re-indents those two.
4. **The consent system takes a power-use restriction as it is.** City
   Wall, Forest Council, Giant Python and Bandit Forest are each a
   `ContributingPower` with `powerUseRestrictions` and an operation
   restriction on placements (`Placements.onto`, `Placements.byUse`).
   `ConsentPowers.default` finds a power's waiver through its
   `powerUseRestrictions`, `ConsentNames` names a banner source by the
   banner ("Darkest Secret"), `WalkerPowerCatalog.useRestrictions` and
   `PhasePowerCatalog` read every registered power's restrictions, and
   `PowerUseRules.waivable` already refuses consent inside a Campaign
   (`PowerUse.consent`). Registering the power in `BannerFacePowers` is all
   the wiring there is. City Wall's refusal builder (`RulerBans`) names a
   card ruler as the granter, so the festival builds its own, with the
   holder as granter.
5. **Three takes read the Darkest Secret's ribbons.** Challenge
   (`ChallengeRibbon`, left), `CampaignRaid.takes` and
   `ConspiracyWhenPlayed.take` (right, each a copy of "burn all"). Favored
   Son takes only the People's Favor. Wizard's Conclave prints "Don't
   remove secrets from it" and walks no ribbon. Hall of Mockery and Lost
   Guardians move a held banner to the shared bank (`BannerToSharedBank`),
   which is not a take. The Raid's and the Conspiracy's ribbons are their
   `Take`'s `leaving`, so a take Lost Tongue refuses leaves the marker
   where it lies.
6. **Secret Testament replaces the Challenge ribbon's children.** It folds
   the `ChallengeRibbon` window into "the holder takes every secret". Task
   5 walks the marker's removal after that window, so the marker still
   leaves the map under Secret Testament.
7. **The Challenge face gate goes.** With both faces of both banners
   handled, `ChallengeProcedure.supportedFaces` refuses nothing; Task 5
   deletes it and `OathViolation.UnsupportedBannerState`, which nothing
   else reads. Slice 5's test "the Festival face still refuses the start"
   becomes the Festival ribbon tests.
8. **The site projection is the in-play site.** `SetupSiteProjection` is
   what the World pane draws for every site in play, and slice 3a's
   `emptySlots` is the precedent for a required field: a default in the
   DTO, `int`/`bool` (required) in the decoder. `HttpGameClientSuite`'s
   hand-written site JSON gains the field.
9. **What changes for existing games.** None can hold the Festival: game
   start refused it until now. Begin Rest's tree gains an empty windowed
   node, which records nothing when nobody contributes.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **The face is read where the banners are built.**
   `FirstGameSetupMaterializer.darkestSecretFace(plan)` turns the Darkest
   Secret to `Festival` when the plan's Foundation VI is altered, with the
   marker `None`. `LaterGameSetup.imperial` already hands the banner to the
   Chancellor under the Devotion goal.
2. **One journaled operation moves the marker.** `SetFestivalMarker(site:
   Option[SiteId])`, applied by `BoardControlOperations.setFestivalMarker`,
   which rejects a site off the map, a placement while the Festival is
   down, and the removal of a marker beside the map: each would be a
   malformed journal.
3. **One power object.** `DarkestSecretFestival`, id
   `banner.darkest-secret.festival`, source
   `RuleSourceRef.Banner("darkest-secret")`, holds the Rest contribution
   (Task 2) and the persistent power (Task 4), so the consent grid, the
   note keys and `LogWords.power` all name one id. Its waiver's source is
   `PowerSourceRef.Banner(Banner.DarkestSecret)`, which the consent panels
   name "Darkest Secret".
4. **The Rest line is sourceless:** "Held the festival at Deep Woods", read
   from the board after the placement, so a holder with no pawn on the map
   writes none.
5. **The reasons** are City Wall's in shape: "Cannot use the
   non-persistent powers of denizens at the festival." and "Cannot place
   favor or secrets on denizens at the festival.", each with "without the
   Darkest Secret holder's consent." when the holder may be asked, after
   Bandit Forest's "without the Bandit Crown holder's consent.". The power
   borrows "{card} stays deactivated." for a refused automatic use, as City
   Wall does.
6. **Two ribbon helpers in `BannerRules`:** `darkestSecretRibbon(ready,
   taker)` (the right ribbon of both faces) and `festivalRemoval(ready)`
   (the remove-festival icon, nothing while the marker is beside the map).
   The left ribbon stays in `ChallengeRibbon`, whose only caller is
   Challenge.
7. **The marker on the map** is a pill reading "Festival" at the end of the
   marked site's pawn row (`.festival-marker`), the First player badge's
   tokens, with the Festival's power as its hover note. The pawn row keeps
   its line either way, so drawing it moves nothing on the fixed-height
   site.
8. **Tests.** `GameStartRulesSuite`, `AlteredFoundationSetupSuite`,
   `NoEmpireSetupSuite` and `BannerFaceProjectionSuite` take game start
   (Task 1); `FestivalMarkerOperationsSuite` and `FestivalRestSuite` (new),
   `WalkerOperationWireSuite` and a golden script take the Rest (Task 2);
   `ProjectionProtocolSuite`, `GamePresentationProjectorMapSuite` and
   `FestivalMarkerSuite` (new, frontend) take the marker's drawing (Task
   3); `FestivalRestrictionSuite` (new) takes the persistent power (Task
   4); `ChallengeProcedureSuite`, `SecretTestamentSuite`,
   `CampaignRaidSuite` and `VisionPlaySuite` take the ribbons (Task 5).

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **Every other player is an enemy, whatever their role.** Banner:
   "Enemies cannot place resources on these cards"; glossary: "Enemy: Other
   players and the bandits." Reading: a Chancellor holder's Citizens are
   bound, and so is every Exile; only the holder is free (Task 4, "it binds
   every enemy whatever their role, and never the holder").
2. **With nobody holding the banner, the festival binds nobody.** Rulebook:
   "Your enemies cannot ..." Reading: no holder, no enemies; the marker
   stays where it lies until a ribbon removes it or the next holder's Rest
   moves it (Task 4, "... the banner unheld ..."; Task 2, "nobody else's
   Rest moves it").
3. **"Their powers" are the powers of the denizens at the marked site.**
   Banner: "Enemies cannot place resources on these cards or use their
   powers"; rulebook: "on denizens at the site with the festival marker,
   and cannot use their powers". Reading: the cards guarded are the
   denizens at that site, edifices included (the owner's ruling that
   "denizens" covers edifices), not the enemies' own advisers or relics
   used there, not relics lying at the site ("They can still peek at and
   recover relics"), and not the site's own powers. An enemy is refused
   wherever they stand. An action modifier printed on a guarded denizen is
   one of its powers (Task 4, "it guards an edifice at the festival, and
   no card elsewhere, and no relic").
4. **"Unless they're persistent powers" is the power as printed.** Reading:
   a power printed persistent may still be used by an enemy, unless its
   cost places favor or secrets on the guarded card, which is a placement
   (Task 4, "an enemy may still use a persistent power at the festival,
   unless its cost places on the card").
5. **Placing favor or secrets is what City Wall's ban reads.** Rulebook:
   "cannot place favor or secrets on denizens at the site". Reading: a cost
   paid onto the card on the payer's turn, a gift onto it, and a forced
   placement from the placer's own board (`Placements.onto`), and a power
   whose cost places on it (`Placements.byUse`); a power that moves favor
   or secrets onto the card from elsewhere is not a placement by the
   enemy, as for City Wall (Task 4).
6. **The holder may consent outside a Campaign, and bandits never hold the
   banner.** Rulebook, "Persistent Powers": "You may give consent to
   enemies to let them ignore persistent powers you rule that harm or limit
   them, except in Campaign actions. ... Bandits always enforce them."
   Reading: the holder is the granter; inside a Campaign the refusal shows
   its plain reason and nobody is asked; a grant lasts the turn for that
   card and requester, as every grant does (Task 4).
7. **The marker starts beside the map and goes on it at the holder's first
   Rest.** Card: "Place the Festival marker nearby." Rulebook: "In your
   Rest Phase, you must place the festival marker over the cards at your
   site." Reading: until the first Rest of a holder nothing is guarded;
   each Rest of the holder places it at their pawn's site, at the start of
   the Rest, before REST powers, even where it already lies, and writes the
   line again (Tasks 1 and 2).
8. **A holder with no pawn on the map places nothing.** Reading: the marker
   stays where it lies, and no line is written (Task 2,
   `DarkestSecretFestival.placed`).
9. **Only the remove-festival icon removes the marker.** Reading: a take
   that walks no ribbon (Wizard's Conclave: "Don't remove secrets from
   it"), and Hall of Mockery's or Lost Guardians' move of the banner to the
   shared bank, leave the marker where it lies; it then guards for the new
   holder, or for nobody (Task 5, `BannerRules.festivalRemoval`'s
   callers).
10. **The Challenge ribbon: ONE burns, the REST returns, then the marker
    goes.** Banner image and `OATH-B02b-NF` as quoted above. Reading: an
    empty banner burns nothing; an unheld banner's rest burns; Secret
    Testament, which takes every secret in place of the ribbon's resource
    icons, still leaves the marker's removal (Task 5).
11. **The Campaign ribbon: HALF burns, the rest stays.** Reading: half the
    secrets, rounded down, burn, the rest stay on the banner the taker
    gets, and the marker goes, for a Raid and for the Conspiracy ("adjust
    its [favor]/[secret] as shown by its right ribbon") alike (Task 5).
12. **Game start: the Chancellor holds the Festival when the Oathkeeper
    goal is the Darkest Secret; otherwise, or with no Chancellor, the
    shared bank does.** Card `OATH-SETUP09b-NF` as quoted in Global
    Constraints. Reading: as Wandering Flame today, 1 secret; a first game
    with an altered VI starts the Festival unheld (Task 1).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/World.scala` | `DarkestSecretState.festival` (Task 1) |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | `darkestSecretFace` (Task 1) |
| `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`, `GameStartRules.scala` | the last refusal deleted (Task 1) |
| `src/main/scala/distantlands/model/CoreOperations.scala` | `SetFestivalMarker` (Task 2) |
| `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`, `BoardControlOperations.scala` | its application (Task 2) |
| `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` | its wire (Task 2) |
| `src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala` | the `RestStart` window (Task 2) |
| `src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala` | new: the Rest (Task 2), the persistent power (Task 4) |
| `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala` | registers it (Task 2) |
| `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`, `WorldProjectionCodec.scala`, `src/main/scala/distantlands/application/GamePresentationProjector.scala` | the site's `festival` flag (Task 3) |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`, `frontend/styles.css`, `DESIGN.md` | the marker drawn (Task 3) |
| `src/main/scala/distantlands/gameplay/actions/BannerRules.scala` | `darkestSecretRibbon`, `festivalRemoval` (Task 5) |
| `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala`, `ChallengeProcedure.scala`, `src/main/scala/distantlands/model/GameViolation.scala` | the Festival's left ribbon; the face gate deleted (Task 5) |
| `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala`, `src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala` | call the right ribbon (Task 5) |
| `src/test/scala/distantlands/model/TestGameFixtures.scala`, `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`, `AlteredFoundationSetupSuite.scala`, `NoEmpireSetupSuite.scala`, `src/test/scala/distantlands/application/BannerFaceProjectionSuite.scala` | game start (Task 1) |
| `src/test/scala/distantlands/testkit/Table.scala` | `festival(at)` (Task 2) |
| `src/test/scala/distantlands/gameplay/operations/FestivalMarkerOperationsSuite.scala`, `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestSuite.scala` | new (Task 2) |
| `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala` | the operation's wire (Task 2) |
| `src/test/scala/distantlands/application/gamelog/FestivalLogScripts.scala` (new), `GoldenScripts.scala`, `src/test/resources/gamelog/festival-rest.actor.log`, `festival-rest.other.log` | the golden (Task 2) |
| `src/test/resources/notes/declared-keys.txt` | rewritten by its command (Tasks 2 and 4) |
| `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`, `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala`, `frontend/src/test/scala/distantlands/frontend/FestivalMarkerSuite.scala` (new), `HttpGameClientSuite.scala` | the marker drawn (Task 3) |
| `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestrictionSuite.scala` | new (Task 4) |
| `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`, `CampaignRaidSuite.scala`, `VisionPlaySuite.scala`, `src/test/scala/distantlands/gameplay/powers/banner/SecretTestamentSuite.scala` | the ribbons (Task 5) |
| `docs/ROADMAP.md`, `docs/architecture/bounded-banners.md`, `docs/architecture/bounded-visions-and-conspiracy.md`, `docs/architecture/all-exile-round-endings.md`, `docs/architecture/game-setup.md`, `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`, the spec | docs (Task 6) |

---

### Task 1: Festival turns the Darkest Secret at game start

**Files:**
- Modify: `src/main/scala/distantlands/model/World.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Test: `src/test/scala/distantlands/model/TestGameFixtures.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`
- Test: `src/test/scala/distantlands/application/BannerFaceProjectionSuite.scala`

**Interfaces:**
- Consumes: `FirstGameSetupPlan.altered(number: FoundationNumber): Boolean`
  (`model/Setup.scala`), `LaterGameSetup.imperial`'s banner holder
  (unchanged).
- Produces: `DarkestSecretState(active: DarkestSecretFace, holder:
  Option[PlayerId], secrets: Int, festival: Option[SiteId])`, a required
  fourth field every later task reads; game start with an altered
  Foundation VI gives `DarkestSecretState(DarkestSecretFace.Festival,
  holder, 1, festival = None)`. `StartingFoundations.refusal` is deleted.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/model/TestGameFixtures.scala`:

Replace

```scala
        DarkestSecretFace.WanderingFlame,
        holder = None,
        secrets = 1
      )
```

with

```scala
        DarkestSecretFace.WanderingFlame,
        holder = None,
        secrets = 1,
        festival = None
      )
```

**In** `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`:

Replace

```scala
      DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1)))
```

with

```scala
      DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1,
        festival = None)))
```

**In** `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
(game start refuses no altered face any more):

Replace

```scala
  test("game start refuses each altered face no slice has brought in yet, " +
      "naming the face and the slice it arrives with"):
    def refusalFor(number: FoundationNumber) = rejected(GameStartRules.evolve(
      catalog, chronicle.copy(foundations = Map(number -> altered)), orders))
    def unsupported(face: String, slice: Int) =
      OathViolation.UnsupportedChronicle(
        s"$face is not supported yet: it arrives with Foundations slice $slice")
    assertEquals(refusalFor(FoundationNumber.VI),
      unsupported("Festival (altered Foundation VI)", 6))
```

with

```scala
  test("game start accepts an altered Foundation VI"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle.copy(
      foundations = Map(FoundationNumber.VI -> altered)), orders))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.VI), altered)
```

Then replace

```scala
  test("with several altered faces, game start names the first refused one " +
      "in foundation order"):
    // II, III and V are accepted, so VI is the first refused.
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Festival (altered " +
        "Foundation VI) is not supported yet: it arrives with Foundations " +
        "slice 6"))
```

with

```scala
  test("game start accepts several altered faces at once and keeps each"):
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    val ready = accepted(GameStartRules.evolve(catalog, several, orders))
    assertEquals(Vector(FoundationNumber.II, FoundationNumber.III,
      FoundationNumber.V, FoundationNumber.VI).map(
        ready.game.campaign.foundations), Vector.fill(4)(altered))
```

**In** `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`:

Replace

```scala
/** Game start under the altered faces that change setup: Movers and
  * Shakers (II) and Teeming World (IV) (Foundations design, slice 2), and
  * the face Grand Council (V) turns the People's Favor to (slice 5). Every
  * test starts from a Chronicle: game start is the subject. */
```

with

```scala
/** Game start under the altered faces that change setup: Movers and
  * Shakers (II) and Teeming World (IV) (Foundations design, slice 2), the
  * face Grand Council (V) turns the People's Favor to (slice 5), and the
  * face Festival (VI) turns the Darkest Secret to (slice 6). Every test
  * starts from a Chronicle: game start is the subject. */
```

Then replace

```scala
  private val (p2, p3) = (PlayerId("p2"), PlayerId("p3"))
```

with

```scala
  test("Festival flips the Darkest Secret to its Festival face with 1 " +
      "secret and its marker beside the map, and gives it to the " +
      "Chancellor when the Oathkeeper goal is the Darkest Secret"):
    val devotion = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Devotion)
    val festival = started(withAlteredFoundation(devotion, FoundationNumber.VI))
    assertEquals(festival.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p1), 1,
        festival = None))
    assertEquals(festival.game.campaign.foundations(FoundationNumber.VI),
      FoundationState(FoundationFace.Altered, Set.empty))
    assertEquals(started(devotion).game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.WanderingFlame, Some(p1), 1,
        festival = None))

  test("under another Oathkeeper goal, or with no Chancellor, the Festival " +
      "starts in the shared bank, and nothing else changes"):
    val supremacy = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Supremacy)
    val flame = started(supremacy)
    val festival = started(withAlteredFoundation(supremacy,
      FoundationNumber.VI))
    assertEquals(festival.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, None, 1,
        festival = None))
    assertEquals(festival.game.current.copy(banners =
      flame.game.current.banners), flame.game.current)
    assertEquals(festival.banks, flame.banks)
    val firstGame = started(withAlteredFoundation(firstGameChronicle.copy(
      oathkeeperGoal = OathkeeperGoal.Devotion), FoundationNumber.VI))
    assertEquals(firstGame.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, None, 1,
        festival = None))

  private val (p2, p3) = (PlayerId("p2"), PlayerId("p3"))
```

**In** `src/test/scala/distantlands/application/BannerFaceProjectionSuite.scala`
(the face's banner action stays; this assertion passes before and after
the change):

Replace

```scala
  test("the Festival face is offered no power"):
    val festival = Table.start
      .darkestSecret(Some(p1), 1, face = DarkestSecretFace.Festival)
      .siteTokens(BrokenPeaksSite, secrets = 1).ready
    assertEquals(powerButtons(festival), Vector.empty)
    assert(!MenuPlay.options(Table.from(festival).phaseMenuParked.state,
      catalog).exists(_.ref == banner))
```

with

```scala
  test("the Festival face is offered no power, and its holder may still " +
      "place a secret on the banner"):
    val festival = Table.start
      .darkestSecret(Some(p1), 1, face = DarkestSecretFace.Festival)
      .siteTokens(BrokenPeaksSite, secrets = 1).secrets(p1, faceUp = 1).ready
    assertEquals(powerButtons(festival), Vector.empty)
    assert(!MenuPlay.options(Table.from(festival).phaseMenuParked.state,
      catalog).exists(_.ref == banner))
    assert(MenuPlay.availableInMenu(catalog, festival,
      ActionRef.PlaceBannerResource))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.NoEmpireSetupSuite distantlands.application.BannerFaceProjectionSuite"`
Expected: FAIL. The test sources do not compile: `DarkestSecretState`
has no parameter named `festival`.

- [ ] **Step 3: Implement**

**In** `src/main/scala/distantlands/model/World.scala`:

Replace

```scala
final case class DarkestSecretState(
    active: DarkestSecretFace,
    holder: Option[PlayerId],
    secrets: Int
):
```

with

```scala
/** The Darkest Secret: its face, its holder, the secrets on it, and
  * `festival`, the site the festival marker lies over while the Festival
  * face is up (Foundations design, slice 6). The marker starts beside the
  * map, `None`, and only the Festival face's holder places it, at their
  * Rest. */
final case class DarkestSecretState(
    active: DarkestSecretFace,
    holder: Option[PlayerId],
    secrets: Int,
    festival: Option[SiteId]
):
```

**In** `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
        DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1)),
```

with

```scala
        DarkestSecretState(darkestSecretFace(plan), None, 1, festival = None)),
```

Then replace

```scala
  /** The favor on each board: 1, and under Movers and Shakers (altered
```

with

```scala
  /** Foundation VI's face, on the Darkest Secret: Wandering Flame sets the
    * Wandering Flame, and Festival flips it to the Festival, its marker
    * beside the map. Both faces put 1 secret on it from the shared bank,
    * and `LaterGameSetup` gives it to the Chancellor when the Oathkeeper
    * goal is the Darkest Secret. */
  private def darkestSecretFace(plan: FirstGameSetupPlan): DarkestSecretFace =
    if plan.altered(FoundationNumber.VI) then DarkestSecretFace.Festival
    else DarkestSecretFace.WanderingFlame

  /** The favor on each board: 1, and under Movers and Shakers (altered
```

**In** `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

Replace

```scala
import distantlands.catalog.FoundationCards
import distantlands.model._
```

with

```scala
import distantlands.model._
```

Then replace (the last face's refusal, and with it the refusal itself)

```scala
        else turned.updated(number, FoundationState(face, Set.empty))
    }

  /** The slice of the Foundations design each altered face arrives with.
    * The slice that makes a face work deletes its line. Foundation III has
    * none: both its faces wait for Legacies and do nothing (ruling 1). */
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.VI -> 6)

  /** Why game start refuses `faces`, if it does: the first altered face, in
    * foundation order, that no slice has brought in yet. */
  def refusal(faces: Map[FoundationNumber, FoundationState]): Option[String] =
    arrivesWith.collectFirst:
      case (number, slice) if faces.get(number)
          .exists(_.face == FoundationFace.Altered) =>
        val name = FoundationCards.face(number, FoundationFace.Altered).name
        s"$name (altered Foundation $number) is not supported yet: it " +
          s"arrives with Foundations slice $slice"
```

with

```scala
        else turned.updated(number, FoundationState(face, Set.empty))
    }
```

**In** `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
 * starts, with an Empire or with none (`EmpireSetupRules`), foundation
 * faces that work (`StartingFoundations`), enough cards to deal, and, in a
```

with

```scala
 * starts, with an Empire or with none (`EmpireSetupRules`), the foundation
 * faces it starts with (`StartingFoundations`), enough cards to deal, and, in a
```

Then replace

```scala
      // resolved, so an eroded face is never refused.
```

with

```scala
      // resolved. Every face of every foundation starts a game.
```

Then replace

```scala
      _ <- StartingFoundations.refusal(faces)
        .map(UnsupportedChronicle(_)).toLeft(())
```

with nothing (delete the two lines).

Then replace (Plan-time finding 3: without the deleted line Scala 3
rejects this enumerator's indentation)

```scala
      atlasSites = if mode == SetupMode.FirstGame then
          chronicle.atlasBox.take(slots)
        else Vector.empty
```

with

```scala
      atlasSites =
        if mode == SetupMode.FirstGame then chronicle.atlasBox.take(slots)
        else Vector.empty
```

Then replace

```scala
      homelands <- if mode == SetupMode.FirstGame then
          homelandEdifices(catalog, atlasSites)
        else Right(Vector.empty)
```

with

```scala
      homelands <-
        if mode == SetupMode.FirstGame then homelandEdifices(catalog, atlasSites)
        else Right(Vector.empty)
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.NoEmpireSetupSuite distantlands.application.BannerFaceProjectionSuite"`
Expected: PASS (71 tests in the plan's own run).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/World.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/test/scala/distantlands/model/TestGameFixtures.scala src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala src/test/scala/distantlands/application/BannerFaceProjectionSuite.scala
git commit -m "feat(setup): Festival turns the Darkest Secret at game start" -m "An altered Foundation VI now starts the game with the Darkest Secret on its Festival face, with 1 secret from the shared bank, its marker beside the map, and in the Chancellor's hands when the Oathkeeper goal is the Darkest Secret, as Wandering Flame sets up its face. The Darkest Secret's state gains the site the festival marker lies over. Game start refused this face last; with it gone, the refusal is deleted." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The Festival's holder places its marker at Rest

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/FestivalMarkerOperationsSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestSuite.scala` (new)
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/FestivalLogScripts.scala` (new)
- Modify: `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`
- Rewritten: `src/test/resources/notes/declared-keys.txt`
- Written: `src/test/resources/gamelog/festival-rest.actor.log`, `src/test/resources/gamelog/festival-rest.other.log`

**Interfaces:**
- Consumes: `DarkestSecretState.festival` (Task 1);
  `PawnMoves.pawnSite(ready, player): Either[OathViolation, SiteId]`;
  `RawOperationBatch.executeAll`, `MenuPlay.started`,
  `PowerRules.withWalkerPowers`, `NoteText.said`, `ActPowerHarness.replayed`,
  `Outcomes.assertWireRoundTrips`, `LogScriptSupport.atTable`,
  `Situation.finishingRest`.
- Produces:
  - `final case class SetFestivalMarker(site: Option[SiteId]) extends PrimitiveOperation`;
  - `BoardControlOperations.setFestivalMarker(ready, site): Either[OperationError, ReadyGame]`;
  - `case object DarkestSecretFestival extends ContributingPower` with
    `val id: PowerId` (`banner.darkest-secret.festival`), `val held:
    NoteKey`, `def holder(ready: ReadyGame): Option[PlayerId]` (the holder
    while the Festival is up), which Task 4 extends;
  - `Table.festival(at: SiteRef): Table`, which Tasks 3 to 5 use.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/testkit/Table.scala`:

Replace

```scala
  /** Foundation `number` showing `face`, with no alteration source; the
```

with

```scala
  /** The festival marker over the cards at `at`; the quiet table keeps it
    * beside the map. */
  def festival(at: SiteRef): Table =
    update(_.updateCurrent(c => c.copy(banners = c.banners.copy(darkestSecret =
      c.banners.darkestSecret.copy(festival = Some(CardRefs.siteId(at)))))))

  /** Foundation `number` showing `face`, with no alteration source; the
```

Create `src/test/scala/distantlands/gameplay/operations/FestivalMarkerOperationsSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** The festival marker on the map (Foundations design, slice 6):
  * `SetFestivalMarker` puts it over a site's cards or takes it off. */
class FestivalMarkerOperationsSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  /** The quiet table with the Darkest Secret unheld on its Festival face,
    * its marker beside the map. */
  private def festivalUnplaced: Table =
    Table.start.darkestSecret(None, 1, DarkestSecretFace.Festival)

  private def marker(ready: ReadyGame): Option[SiteId] =
    ready.game.current.banners.darkestSecret.festival

  test("placing the marker puts it over a site on the map, and placing it " +
      "again moves it"):
    val placed = accepted(run(festivalUnplaced.ready,
      SetFestivalMarker(Some(home))))
    assertEquals(marker(placed), Some(home))
    assertEquals(marker(accepted(run(placed, SetFestivalMarker(Some(peaks))))),
      Some(peaks))
    assertEquals(marker(accepted(run(placed, SetFestivalMarker(Some(home))))),
      Some(home))

  test("removing takes it off the map, and needs it on the map"):
    val placed = festivalUnplaced.festival(home).ready
    val removed = accepted(run(placed, SetFestivalMarker(None)))
    assertEquals(marker(removed), None)
    rejected(run(removed, SetFestivalMarker(None)))

  test("it goes only on a site on the map, and only on the Festival face"):
    rejected(run(festivalUnplaced.ready,
      SetFestivalMarker(Some(SiteId("no-such-site")))))
    rejected(run(Table.start.ready, SetFestivalMarker(Some(home))))
```

Create `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestSuite.scala`:

```scala
package distantlands.gameplay.powers.banner

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, MenuPlay, Outcomes, PowerRules,
  Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Festival, the Darkest Secret's altered face: "REST: You must place the
  * festival marker over the cards at your site." (Foundations design,
  * slice 6). The holder's Rest places it, asking nothing. */
class FestivalRestSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerPowers
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p1's Act at Ancient City, holding the Darkest Secret on its Festival
    * face, its marker beside the map. */
  private def festivalHolderActingAtHome: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival)

  /** p1 ends the Act and rests, through to the next player's turn. */
  private def rested(ready: ReadyGame): OathTransition =
    accepted(MenuPlay.started(rules, Ready(ready), p1,
      PhaseTransitionRef.BeginRest))

  private def marker(transition: OathTransition): Option[SiteId] =
    Outcomes.ready(transition.state).game.current.banners.darkestSecret
      .festival

  private def festivalSaid(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(DarkestSecretFestival.id, DarkestSecretFestival.noteKeys,
      events)

  test("the holder's Rest places the marker over the cards at their " +
      "pawn's site, with its line"):
    val done = rested(festivalHolderActingAtHome.ready)
    assertEquals(marker(done), Some(home))
    assertEquals(festivalSaid(done.events), Vector(NoteText.Said("held",
      s"Held the festival at ${home.value}", covers = false)))

  test("a later Rest moves it to the site the holder's pawn stands at now"):
    val done = rested(festivalHolderActingAtHome.festival(peaks).ready)
    assertEquals(marker(done), Some(home))

  test("nobody else's Rest moves it: not while an enemy holds it, not " +
      "while it is unheld, and not on the Wandering Flame"):
    val others: Vector[(String, Table)] = Vector(
      "held by p2" -> festivalHolderActingAtHome.festival(peaks)
        .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival),
      "unheld" -> festivalHolderActingAtHome.festival(peaks)
        .darkestSecret(None, 1, DarkestSecretFace.Festival),
      "Wandering Flame" -> festivalHolderActingAtHome
        .darkestSecret(Some(p1), 1, DarkestSecretFace.WanderingFlame))
    others.foreach { case (label, table) =>
      val ready = table.ready
      val done = rested(ready)
      assertEquals(marker(done),
        ready.game.current.banners.darkestSecret.festival, label)
      assertEquals(festivalSaid(done.events), Vector.empty, label)
    }

  test("the Rest's events replay to the same marker and round-trip the " +
      "wire"):
    val ready = festivalHolderActingAtHome.festival(peaks).ready
    val done = rested(ready)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)
```

**In** `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
(the test "every CoreOperation variant round-trips through the walker
codec"):

Replace

```scala
      ClearTrackedPawn(PlayerId("red")),
```

with

```scala
      ClearTrackedPawn(PlayerId("red")),
      SetFestivalMarker(Some(site)),
      SetFestivalMarker(None),
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.FestivalMarkerOperationsSuite distantlands.gameplay.powers.banner.FestivalRestSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: FAIL. The test sources do not compile: `SetFestivalMarker` and
`DarkestSecretFestival` are not found.

- [ ] **Step 3: Implement the operation**

**In** `src/main/scala/distantlands/model/CoreOperations.scala`:

Replace

```scala
/** Removes `holder`'s Tracker record as their Act phase begins. */
final case class ClearTrackedPawn(holder: PlayerId) extends PrimitiveOperation
```

with

```scala
/** Removes `holder`'s Tracker record as their Act phase begins. */
final case class ClearTrackedPawn(holder: PlayerId) extends PrimitiveOperation

/** Puts the festival marker over the cards at `site`, or takes it off the
  * map (`None`), in `DarkestSecretState.festival` (Foundations design,
  * slice 6). The Festival's holder places it at their Rest; a ribbon that
  * prints the remove-festival icon takes it off. Applying it rejects a
  * site that is not on the map, a placement while the Festival face is
  * down, and a removal of a marker that is not on the map: each is a
  * malformed journal, not a rule.
  */
final case class SetFestivalMarker(site: Option[SiteId])
    extends PrimitiveOperation
```

**In** `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`:

Replace

```scala
      case (result, ClearTrackedPawn(holder)) =>
        result.flatMap(TurnStateOperations.clearTrackedPawn(_, holder))
```

with

```scala
      case (result, ClearTrackedPawn(holder)) =>
        result.flatMap(TurnStateOperations.clearTrackedPawn(_, holder))
      case (result, SetFestivalMarker(site)) =>
        result.flatMap(BoardControlOperations.setFestivalMarker(_, site))
```

**In** `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala`:

Replace

```scala
  private def setBannerHolder(ready: ReadyGame, banner: Banner,
```

with

```scala
  /** The festival marker over `site`, or off the map. It goes only on a
    * site on the map, only while the Festival face is up, and comes off
    * only when it is on the map. */
  def setFestivalMarker(ready: ReadyGame, site: Option[SiteId])
      : Either[OperationError, ReadyGame] =
    val secret = ready.game.current.banners.darkestSecret
    site match
      case Some(at) if !ready.game.current.map.inPlay.contains(at) =>
        Left(UnknownSite(at))
      case Some(_) if secret.active != DarkestSecretFace.Festival =>
        Left(InvalidDescription(
          "the festival marker is placed only on the Festival face"))
      case None if secret.festival.isEmpty =>
        Left(InvalidDescription("the festival marker is not on the map"))
      case _ => Right(ready.updateCurrent(current => current.copy(
        banners = current.banners.copy(darkestSecret =
          secret.copy(festival = site)))))

  private def setBannerHolder(ready: ReadyGame, banner: Banner,
```

**In** `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala`:

Replace

```scala
    OpenPick | ClosePick | TrackPawn | ClearTrackedPawn
```

with

```scala
    OpenPick | ClosePick | TrackPawn | ClearTrackedPawn | SetFestivalMarker
```

Then replace

```scala
      case ClearTrackedPawn(holder) => ujson.Obj(
        "kind" -> "clear-tracked-pawn", "holder" -> holder.value)
```

with

```scala
      case ClearTrackedPawn(holder) => ujson.Obj(
        "kind" -> "clear-tracked-pawn", "holder" -> holder.value)
      case SetFestivalMarker(site) => ujson.Obj(
        "kind" -> "set-festival-marker",
        "siteId" -> site.fold[ujson.Value](ujson.Null)(at => ujson.Str(at.value)))
```

Then replace

```scala
    case "clear-tracked-pawn" =>
      Right(ClearTrackedPawn(PlayerId(value("holder").str)))
```

with

```scala
    case "clear-tracked-pawn" =>
      Right(ClearTrackedPawn(PlayerId(value("holder").str)))
    case "set-festival-marker" =>
      Right(SetFestivalMarker(value("siteId").strOpt.map(SiteId(_))))
```

- [ ] **Step 4: Implement the Rest**

Create `src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala`:

```scala
package distantlands.gameplay.powers.banner

import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.model._

/** Festival (the Banner of the Darkest Secret, Festival face): "REST: You
  * must place the festival marker over the cards at your site."
  * (Foundations design, slice 6).
  *
  * A rule of the banner's holder, automatic, at the `RestStart` window that
  * Begin Rest folds: it asks nothing, since the face says "must". The
  * marker goes to the site the holder's pawn stands at, by a journaled
  * `SetFestivalMarker`, wherever it lay before; a holder with no pawn on
  * the map places nothing and the marker stays. The Rest of a player who
  * does not hold the face, or of the holder of the Wandering Flame, places
  * nothing.
  *
  * Its line names the site and not the holder, whose turn it is: "Held the
  * festival at Deep Woods".
  */
case object DarkestSecretFestival extends ContributingPower:
  val id: PowerId = PowerId("banner.darkest-secret.festival")
  def source: RuleSourceRef = RuleSourceRef.Banner(Banner.DarkestSecret.key)

  private val notes = NoteSet(id)
  /** "Held the festival at {Deep Woods}". */
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Held the festival at "), NotePart.Arg(0)))
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** The player holding the Darkest Secret while its Festival face is up. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    val secret = ready.game.current.banners.darkestSecret
    secret.holder.filter(_ => secret.active == DarkestSecretFace.Festival)

  /** The resting player holds the Festival. */
  override def applicable(ctx: PowerCtx): Boolean =
    holder(ctx.state).contains(ctx.actor)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.RestStart -> Vector(Transform((ctx, ops) => ops ++ Vector(
      BuildOps((ready, _) => Right(placed(ready, ctx.actor))),
      Note(id, line(_, ctx.actor))))))

  /** The marker at `rester`'s pawn site, or nothing with no pawn there. */
  private def placed(ready: ReadyGame, rester: PlayerId)
      : Vector[CoreOperation] =
    PawnMoves.pawnSite(ready, rester).toOption.toVector.map(site =>
      SetFestivalMarker(Some(site)))

  /** The line, once the marker lies at `rester`'s pawn site. */
  private def line(states: NoteStates, rester: PlayerId): Option[LogNote] =
    PawnMoves.pawnSite(states.now, rester).toOption
      .filter(site => states.now.game.current.banners.darkestSecret.festival
        .contains(site))
      .map(site => held.sourceless(NoteArg.Site(site)))
```

**In** `src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala`:

Replace

```scala
    Vector[ContributingPower](PeoplesFavorMob, PeoplesFavorGrandCouncil)
```

with

```scala
    Vector[ContributingPower](PeoplesFavorMob, PeoplesFavorGrandCouncil,
      DarkestSecretFestival)
```

**In** `src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala`:

Replace

```scala
/** Begin Rest: the supported-state gate and the phase change, nothing else.
  *
  * Cleanup waits for Finish Rest so REST powers can be used in between.
  */
```

with

```scala
/** Begin Rest: the supported-state gate, the phase change and the
  * `RestStart` window, nothing else. The Festival's holder places its
  * marker at that window (`DarkestSecretFestival`).
  *
  * Cleanup waits for Finish Rest so REST powers can be used in between.
  */
```

Then replace

```scala
  /** One leaf, so it finishes in the command that starts it; resume never
    * reaches this.
    */
```

with

```scala
  /** Nothing here asks a question, so it finishes in the command that
    * starts it; resume never reaches this.
    */
```

Then replace

```scala
  yield Sequence(Vector(EnterPhase(Phase.Rest)))
```

with

```scala
  yield Sequence(Vector(EnterPhase(Phase.Rest),
    Sequence(Vector.empty, Some(PowerWindow.RestStart))))
```

- [ ] **Step 5: Run the tests to see them pass, and rewrite the note-key snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.operations.FestivalMarkerOperationsSuite distantlands.gameplay.powers.banner.FestivalRestSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: PASS (41 tests in the plan's own run).

Run: `./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: FAIL, "every power declares exactly the notes the snapshot
records": the new power declares a key the snapshot lacks.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS, 3 tests.

Run: `git diff -- src/test/resources` and `git status --short src/test/resources`
Expected: only `src/test/resources/notes/declared-keys.txt` changed, and
its diff is exactly this added first line:

```diff
+banner.darkest-secret.festival held = Held the festival at {0}
 banner.darkest-secret.wandering-flame.move used = {0} moved to {1}.
 banner.darkest-secret.wandering-flame.place used = {0} placed a secret at {1}.
```

- [ ] **Step 6: The golden log**

Create `src/test/scala/distantlands/application/gamelog/FestivalLogScripts.scala`:

```scala
package distantlands.application.gamelog

import distantlands.model._
import distantlands.testkit.MenuPlay
import distantlands.testkit.Table.p1
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** [[LogScripts]]' scripts for the Festival, the Darkest Secret's altered
  * face (Foundations design, slice 6). */
object FestivalLogScripts:
  /** p1 holds the Festival at Ancient City and ends the Act: the Rest
    * places the marker there, and the next player wakes. */
  def festivalRest(using munit.Location): Script =
    val (service, acting) = atTable("festival-rest", tableWithOnePawnPerSite
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival)
      .banditsRefilled)
    acting.after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
      .finishingRest(p1)
    Script("festival-rest", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "festival-rest" -> (() => festivalRest))
```

**In** `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`:

Replace

```scala
      ImperialLogScripts.named ++ LaterGameLogScripts.named ++
      ExploreLogScripts.named
```

with

```scala
      ImperialLogScripts.named ++ LaterGameLogScripts.named ++
      ExploreLogScripts.named ++ FestivalLogScripts.named
```

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: FAIL, exactly one test, "festival-rest reads exactly as its
golden log for the actor and another seat": "missing golden
src/test/resources/gamelog/festival-rest.actor.log". Every other golden
passes: the new `RestStart` node changes no other log.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: PASS.

Run: `git status --short src/test/resources` and `git diff -- src/test/resources/gamelog`
Expected: the diff is empty (no existing golden changed), and the status
lists, besides `declared-keys.txt`, exactly two new files,
`src/test/resources/gamelog/festival-rest.actor.log` and
`src/test/resources/gamelog/festival-rest.other.log`, each of which reads
exactly:

```text
0.0 round 0 | Round 1
0.1 turn 0 | [player:p1|p1]'s turn
0.2 phase 0 | Act
5.0 phase 0 | Rest
7.0 trigger 1 | Held the festival at [site:site:ancient-city|Ancient City]
9.0 turn 0 | [player:p2|p2]'s turn
9.1 phase 0 | Wake
11.0 action 1 | Nothing happened in Wake
11.1 phase 0 | Act
```

The line names only the site: it is p1's own turn.

- [ ] **Step 7: Run the neighbours**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.* distantlands.gameplay.operations.FestivalMarkerOperationsSuite distantlands.serialization.WalkerOperationWireSuite distantlands.gameplay.RestSuite distantlands.gameplay.RuleSourceIndexSuite distantlands.application.gamelog.* distantlands.gameplay.PhaseMenuSuite distantlands.gameplay.PhasePowerSuite distantlands.gameplay.BackendArchitectureSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS (481 tests in the plan's own run).

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala src/main/scala/distantlands/gameplay/powers/banner/BannerFacePowers.scala src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/gameplay/operations/FestivalMarkerOperationsSuite.scala src/test/scala/distantlands/gameplay/powers/banner/FestivalRestSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala src/test/scala/distantlands/application/gamelog/FestivalLogScripts.scala src/test/scala/distantlands/application/gamelog/GoldenScripts.scala src/test/resources/notes/declared-keys.txt src/test/resources/gamelog/festival-rest.actor.log src/test/resources/gamelog/festival-rest.other.log
git commit -m "feat(banner): the Festival's holder places its marker at Rest" -m "Begin Rest now folds the RestStart window, where the Festival's holder puts the festival marker over the cards at their pawn's site, asking nothing, since the face says must. The marker moves by a journaled SetFestivalMarker, and the Rest writes Held the festival at the site. A golden log pins the line." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Draw the festival marker on its site

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
- Modify: `frontend/styles.css`
- Modify: `DESIGN.md`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/FestivalMarkerSuite.scala` (new)
- Test: `frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala`

**Interfaces:**
- Consumes: `DarkestSecretState.festival` (Task 1), `Table.festival`
  (Task 2); the frontend test kit's `TestSites.quietSite`,
  `TestProjections.actingProjection`, `Dom.all`.
- Produces: `SetupSiteProjection.festival: Boolean` (wire `festival`,
  required); `WorldBoardRenderer.festivalNote: String`
  (`private[frontend]`).

- [ ] **Step 1: Write the failing tests**

**In** `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`:

Replace

```scala
  /** Task 5b: a decision's panel copy is two OPTIONAL strings, so the
```

with

```scala
  test("a site's festival marker round-trips, and a site payload without " +
      "it is refused"):
    val marked = projection.copy(world = projection.world.map(region =>
      region.copy(sites = region.sites.map(_.copy(festival = true)))))
    assertRoundTrips(marked)
    val raw = ujson.read(GameProjectionCodec.encode(marked))
    raw("world").arr.foreach(region =>
      region("sites").arr.foreach(site => site.obj.remove("festival")))
    assert(GameProjectionCodec.decode(ujson.write(raw)).isLeft)

  /** Task 5b: a decision's panel copy is two OPTIONAL strings, so the
```

**In** `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala`:

Replace

```scala
/** The map as the table draws it: each region's sites in slot order, and
  * the slots that hold none (Foundations design, slice 3). */
```

with

```scala
/** The map as the table draws it: each region's sites in slot order, the
  * slots that hold none (Foundations design, slice 3), and the site the
  * festival marker lies over (slice 6). */
```

Then replace

```scala
import distantlands.model._
import distantlands.testkit.Unwrap.accepted
```

with

```scala
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.accepted
```

Then replace (the file's last line)

```scala
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))
```

with

```scala
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))

  test("the festival marker is projected on the one site it lies over, " +
      "and on none while it is beside the map"):
    def marked(table: Table): Vector[String] =
      projector.readyWorld(table.ready, Some(p1)).flatMap(_.sites)
        .filter(_.festival).map(_.siteId)
    val festival = Table.start.darkestSecret(Some(p1), 1,
      DarkestSecretFace.Festival)
    assertEquals(marked(festival.festival(BrokenPeaksSite)),
      Vector(BrokenPeaksSite.id.value))
    assertEquals(marked(festival), Vector.empty)
```

**In** `frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala`
(the hand-written site JSON carries the new required field):

Replace

```scala
"forces":{"forceKind":"exile","count":2,"rulerKind":"player","rulerPlayerId":"red-exile","label":"Red Warbands","colorToken":"red"}}"""
```

with

```scala
"forces":{"forceKind":"exile","count":2,"rulerKind":"player","rulerPlayerId":"red-exile","label":"Red Warbands","colorToken":"red"},"festival":true}"""
```

Then replace

```scala
"relics":{"relics":[]},"forces":null}"""
```

with

```scala
"relics":{"relics":[]},"forces":null,"festival":false}"""
```

(Each block is the end of a long line in `siteJson`; leave the rest of the
line as it is.)

Create `frontend/src/test/scala/distantlands/frontend/FestivalMarkerSuite.scala`:

```scala
package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.all
import TestProjections.actingProjection
import TestSites.quietSite
import org.scalajs.dom

/** The festival marker over a site's cards, drawn at the end of the site's
  * pawn row (Foundations design, slice 6). */
class FestivalMarkerSuite extends munit.FunSuite:
  private val woods = quietSite("site:woods", "Deep Woods")
  private val peaks = quietSite("site:peaks", "Broken Peaks")

  /** The World pane of a table whose Provinces hold `sites`, with Red's
    * pawn at Deep Woods. */
  private def world(sites: GameSite*): dom.Element =
    WorldBoardRenderer.world(actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      world = Vector(GameRegion("provinces", sites.toVector)))
      .copy(pawnLocations = Vector(GamePawn("red", "site:woods"))),
      BoardOptions.none)

  /** The class of each item in the pawn row of the site named `label`. */
  private def pawnRow(root: dom.Element, label: String): Vector[String] =
    all(root, s".site[aria-label='$label'] .site-pawns > *")
      .map(_.getAttribute("class"))

  test("the site it lies over ends its pawn row with the marker, which " +
      "names itself and holds the Festival's power in its note"):
    val root = world(woods.copy(festival = true), peaks)
    assertEquals(pawnRow(root, "Deep Woods"), Vector("pawn", "festival-marker"))
    assertEquals(pawnRow(root, "Broken Peaks"), Vector.empty)
    val marker = all(root, ".festival-marker")
    assertEquals(marker.map(_.textContent), Vector("Festival"))
    assertEquals(marker.map(_.getAttribute("aria-label")),
      Vector("Festival marker"))
    assertEquals(marker.map(_.getAttribute("data-tip")),
      Vector(WorldBoardRenderer.festivalNote))

  test("it stands alone in the row of a site no pawn stands at"):
    val root = world(woods, peaks.copy(festival = true))
    assertEquals(pawnRow(root, "Broken Peaks"), Vector("festival-marker"))

  test("with the marker beside the map no site draws it"):
    assertEquals(all(world(woods, peaks), ".festival-marker").size, 0)

  test("the marker is no board option"):
    val root = world(woods.copy(festival = true))
    assertEquals(all(root, ".festival-marker[data-option-ref]").size, 0)
    assertEquals(all(root, ".festival-marker button").size, 0)
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.application.GamePresentationProjectorMapSuite"`
Expected: FAIL. The test sources do not compile: `festival` is not a
member of `SetupSiteProjection`.

- [ ] **Step 3: Implement the projection**

**In** `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`:

Replace

```scala
    powers: Vector[SitePowerProjection] = Vector.empty,
    forces: Option[SiteForcesProjection] = None
)
```

with

```scala
    powers: Vector[SitePowerProjection] = Vector.empty,
    forces: Option[SiteForcesProjection] = None,
    /** Whether the festival marker lies over this site's cards (Foundations
      * design, slice 6). Public, as the marker is on the table. */
    festival: Boolean = false
)
```

**In** `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`:

Replace

```scala
    "forces" -> option(value.forces)(encodeForces),
    "denizens" -> encoded(value.denizens)(encodeSiteCard), "relics" -> encodeRelics(value.relics))
```

with

```scala
    "forces" -> option(value.forces)(encodeForces),
    "festival" -> value.festival,
    "denizens" -> encoded(value.denizens)(encodeSiteCard), "relics" -> encodeRelics(value.relics))
```

Then replace

```scala
      "powers", "forces", "denizens", "relics"), path)
```

with

```scala
      "powers", "forces", "festival", "denizens", "relics"), path)
```

Then replace

```scala
    forces <- optionalAbsent(value, "forces", path)(decodeForces)
    denizenRaws <- array(value, "denizens", path)
```

with

```scala
    forces <- optionalAbsent(value, "forces", path)(decodeForces)
    festival <- bool(value, "festival", path)
    denizenRaws <- array(value, "denizens", path)
```

Then replace

```scala
    relicCapacity, denizens, relics, defense, recover, forge, powers, forces)
```

with

```scala
    relicCapacity, denizens, relics, defense, recover, forge, powers, forces,
    festival)
```

**In** `src/main/scala/distantlands/application/GamePresentationProjector.scala`:

Replace

```scala
      state.flatMap(_.forces match {
        case value: SiteForces.Occupied => Some(forceProjection(value, ready))
        case SiteForces.Empty => None
      }))
```

with

```scala
      state.flatMap(_.forces match {
        case value: SiteForces.Occupied => Some(forceProjection(value, ready))
        case SiteForces.Empty => None
      }),
      ready.game.current.banners.darkestSecret.festival.contains(siteId))
```

- [ ] **Step 4: Run the JVM tests to see them pass**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.application.GamePresentationProjectorMapSuite"`
Expected: PASS (59 tests in the plan's own run).

Run: `./sbtw "frontend/testOnly distantlands.frontend.FestivalMarkerSuite"`
Expected: FAIL. The frontend test sources do not compile:
`festivalNote` is not a member of `WorldBoardRenderer`.

- [ ] **Step 5: Draw the marker**

**In** `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`:

Replace

```scala
          pawns.appendChild(marker)
        }
        // Appended whether or not anyone stands here: the row holds its line
```

with

```scala
          pawns.appendChild(marker)
        }
        // The festival marker lies over this site's cards; it ends the pawn
        // row, so drawing it moves nothing on the board.
        if site.festival then pawns.appendChild(festivalMarker)
        // Appended whether or not anyone stands here: the row holds its line
```

Then replace

```scala
  /** A site slot of the region `regionName` that holds no site: a frame
```

with

```scala
  /** The festival marker over a site's cards (Foundations design, slice
    * 6): a pill naming it, its power in its hover note. It is not an
    * option and answers nothing. */
  private def festivalMarker: dom.Element =
    val marker = text("span", "festival-marker", "Festival")
    marker.setAttribute("aria-label", "Festival marker")
    HoverTip.set(marker, festivalNote)
    marker

  private[frontend] val festivalNote: String = "Festival: enemies of the " +
    "Darkest Secret's holder cannot place resources on these cards or use " +
    "their powers, unless they're persistent powers."

  /** A site slot of the region `regionName` that holds no site: a frame
```

**In** `frontend/styles.css`:

Replace

```css
.pawn { display: inline-block; font-size: 0.9rem; }
```

with

```css
.pawn { display: inline-block; font-size: 0.9rem; }
/* The festival marker over a site's cards: the first player badge's pill,
   at the end of the pawn row, one line tall. */
.festival-marker { align-self: center; border: 1px solid #a98b58;
  border-radius: 999px; padding: 0 0.45em; color: var(--brass-bright);
  font-size: 0.72rem; font-weight: 700; line-height: 1.15;
  white-space: nowrap; }
```

Then replace

```css
.map-compact .pawn { font-size: inherit; }
```

with

```css
.map-compact .pawn, .map-compact .festival-marker { font-size: inherit; }
```

**In** `DESIGN.md`:

Replace

~~~markdown
- **First player badge**: the target detail badge's pill (`#a98b58` border,
  Bright Brass 0.72rem 700) with no fill, inline after the role on the first
  player's identity line, with no separator dot before it.
~~~

with

~~~markdown
- **First player badge**: the target detail badge's pill (`#a98b58` border,
  Bright Brass 0.72rem 700) with no fill, inline after the role on the first
  player's identity line, with no separator dot before it.
- **Festival marker**: the first player badge's pill, at the end of a site's
  pawn row (Sites).
~~~

Then replace

~~~markdown
answers nothing; its label names it for a screen reader.
~~~

with

~~~markdown
answers nothing; its label names it for a screen reader. The festival
marker, on the site whose cards it lies over, ends the pawn row as a pill
(`.festival-marker`): the First player badge's `#a98b58` border, no fill,
Bright Brass 0.72rem 700, reading "Festival", with the Festival's power in
its hover note. The pawn row keeps its line, so the marker moves nothing; on
the compact map it takes the glance size with the pawns. It is not an
option and answers nothing.
~~~

- [ ] **Step 6: Run the frontend tests to see them pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.FestivalMarkerSuite distantlands.frontend.HttpGameClientSuite distantlands.frontend.EmptySiteSlotSuite"`
Expected: PASS (22 tests in the plan's own run).

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala src/main/scala/distantlands/application/GamePresentationProjector.scala frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala frontend/styles.css DESIGN.md shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala frontend/src/test/scala/distantlands/frontend/FestivalMarkerSuite.scala frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala
git commit -m "feat(table): draw the festival marker on its site" -m "The projection gives each site a required festival flag, true for the one site the festival marker lies over, and the World pane ends that site's pawn row with a Festival pill in the first player badge's tokens, its power in its hover note. The pawn row keeps its line, so the marker moves nothing on the board." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The festival bars enemies from the cards it lies over

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestrictionSuite.scala` (new)
- Rewritten: `src/test/resources/notes/declared-keys.txt`

**Interfaces:**
- Consumes: `DarkestSecretFestival.holder` (Task 2), `Table.festival`
  (Task 2); `PowerUseRules.{useKey, target, cardRef, refusals, waivable,
  shown}`, `Placements.{onto, byUse}`, `PowerUseStep.staysDeactivated`,
  `ConsentPowers.default`; the test kit's `MenuPlay.menuOptionFor`,
  `PhasePowerHarness.{use, awaits}`.
- Produces: `DarkestSecretFestival` becomes `ContributingPower with
  PowerUseRestriction`, with `val usePlain`, `useAsked`, `placingPlain`,
  `placingAsked`, `staysDeactivated: NoteKey`, `def refusal(catalog, ready,
  use): Option[PowerUseRefusal]`, `override val waiver: Option[Waiver]`,
  and one operation restriction on placements.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/banner/FestivalRestrictionSuite.scala`:

```scala
package distantlands.gameplay.powers.banner

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.PowerUse
import distantlands.gameplay.powers.ConsentPowers
import distantlands.gameplay.powers.action.{DowsingSticksCard, RovingTerror,
  RovingTerrorCard}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{MenuPlay, PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The festival marker's persistent power: "Your enemies cannot place favor
  * or secrets on denizens at the site with the festival marker, and cannot
  * use their powers unless they're persistent powers. They can still peek
  * at and recover relics. You may give enemies consent to ignore the
  * festival" (rulebook, "Festival (Darkest Secret)"; Foundations design,
  * slice 6). */
class FestivalRestrictionSuite extends munit.FunSuite:
  private val festival = DarkestSecretFestival
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val terror = RovingTerrorCard.id
  private val terrorRef = DecisionOptionRef.Denizen(terror)
  private val source = PowerSourceRef.Banner(Banner.DarkestSecret)

  /** p1's Act at Ancient City, where Roving Terror stands under the
    * festival marker; Wrestlers stand at Broken Peaks for the Terror to
    * replace. p2, at Broken Peaks, holds the Darkest Secret on its Festival
    * face. */
  private def terrorAtTheFestivalOfP2: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .denizen(terror, at = home).denizen(WrestlersCard, at = peaks)
      .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival).festival(home)

  private def useOf(card: CardId, by: PlayerId, persistent: Boolean = false,
      places: Boolean = false): PowerUse =
    PowerUse(by, PowerId("test.power"), Some(card), persistent = persistent,
      costsSecret = false, places = places)

  private def terrorOption(ready: ReadyGame)(using munit.Location)
      : DecisionOption = MenuPlay.menuOptionFor(catalog, ready,
    ActionRef.UsePower(RovingTerror.id), Vector(terrorRef))

  test("it refuses an enemy a non-persistent power of a denizen at the " +
      "festival, waivable by the holder"):
    val refusal = festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p1))
    assertEquals(refusal.map(_.note), Some(festival.usePlain(source)))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter),
      Some(p2))
    assertEquals(refusal.flatMap(_.reason.waiver).map(_.covers), Some(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, terrorRef)))

  test("it binds every enemy whatever their role, and never the holder"):
    val imperial = terrorAtTheFestivalOfP2.imperial(p2, p1).ready
    assert(festival.refusal(catalog, imperial, useOf(terror, by = p1))
      .nonEmpty)
    assert(festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p3)).nonEmpty)
    assertEquals(festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p2)), None)

  test("an enemy may still use a persistent power at the festival, unless " +
      "its cost places on the card"):
    val ready = terrorAtTheFestivalOfP2.ready
    assertEquals(festival.refusal(catalog, ready,
      useOf(terror, by = p1, persistent = true)), None)
    assertEquals(festival.refusal(catalog, ready, useOf(terror, by = p1,
      persistent = true, places = true)).map(_.note),
      Some(festival.placingPlain(source)))

  test("it guards an edifice at the festival, and no card elsewhere, and " +
      "no relic"):
    val ready = terrorAtTheFestivalOfP2
      .edifice(HallOfDebateCard, EdificeSide.Intact, at = home)
      .relicAt(DowsingSticksCard, at = home).ready
    assert(festival.refusal(catalog, ready,
      useOf(HallOfDebateCard.id, by = p1)).nonEmpty)
    assertEquals(festival.refusal(catalog, ready,
      useOf(WrestlersCard.id, by = p1)), None)
    assertEquals(festival.refusal(catalog, ready,
      useOf(DowsingSticksCard.id, by = p1)), None)

  test("it refuses an enemy's payment onto a denizen at the festival, " +
      "waivable by the holder, and lets the holder's through"):
    val ban = festival.operationRestrictions(catalog).head
    val ready = terrorAtTheFestivalOfP2.ready
    val enemyPays = PayCost(p1, Location.OnCard(terror), Cost(favor = 1))
    val waiver = ban.reason(ready, enemyPays).flatMap(_.waiver)
    assertEquals(waiver.map(_.covers), Some(ConsentKey(festival.id, p1,
      PowerUseRules.useKey, terrorRef)))
    assertEquals(waiver.flatMap(_.granter), Some(p2))
    assertEquals(waiver.map(_.plain), Some(festival.placingPlain(source)))
    val holderTurn = Table.from(ready).turn(p2, Phase.Act).ready
    assertEquals(ban.reason(holderTurn,
      PayCost(p2, Location.OnCard(terror), Cost(favor = 1))), None)

  test("with the marker beside the map, the banner unheld or the Wandering " +
      "Flame up, nothing is refused"):
    val free: Vector[(String, Table)] = Vector(
      "beside the map" -> terrorAtTheFestivalOfP2.update(_.updateCurrent(c =>
        c.copy(banners = c.banners.copy(darkestSecret =
          c.banners.darkestSecret.copy(festival = None))))),
      "unheld" -> terrorAtTheFestivalOfP2
        .darkestSecret(None, 1, DarkestSecretFace.Festival),
      "Wandering Flame" -> terrorAtTheFestivalOfP2
        .darkestSecret(Some(p2), 1, DarkestSecretFace.WanderingFlame))
    free.foreach { case (label, table) =>
      assertEquals(festival.refusal(catalog, table.ready,
        useOf(terror, by = p1)), None, label)
    }

  test("the Act menu shows an enemy's ACTION at the festival disabled, " +
      "askable of the holder"):
    terrorOption(terrorAtTheFestivalOfP2.ready) match
      case DecisionOption.Unavailable(_, OptionReason.Askable(power, note,
          asks, _)) =>
        assertEquals(power, festival.id)
        assertEquals(note, festival.useAsked(source))
        assertEquals(asks, Vector(ConsentAsk(p2, Vector(festival.id))))
      case other => fail(s"not askable: $other")

  test("the holder's grant opens the ACTION for the turn"):
    val ready = terrorAtTheFestivalOfP2.ready
    val granted = ready.copy(consent = ConsentState(answers = Map(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, terrorRef) ->
        ConsentAnswer.Granted.by(p2)), turn = Some(ConsentTurn.of(ready))))
    assert(terrorOption(granted).available)
    assert(PhasePowerHarness.awaits(accepted(PhasePowerHarness.use(granted,
      RovingTerror, terrorRef)), RovingTerror.decisionId))

  test("where nobody may be asked, as in a Campaign, it shows its plain " +
      "reason"):
    val ready = terrorAtTheFestivalOfP2.ready
    val use = useOf(terror, by = p1).copy(consent = false)
    val refusals = PowerUseRules.refusals(catalog, ready, use,
      Vector(festival))
    assert(!PowerUseRules.waivable(use, refusals))
    assertEquals(PowerUseRules.shown(ready, p1, refusals, use.consent),
      OptionReason.Noted(festival.id, festival.usePlain(source)))

  test("the holder's consent grid lists it, named for the banner"):
    val ready = terrorAtTheFestivalOfP2.ready
    val powers = ConsentPowers.default
    assert(powers.ruledBy(ready, p2).contains(festival.id))
    assert(!powers.ruledBy(ready, p1).contains(festival.id))
    assertEquals(powers.source(festival.id), Some(source))
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.FestivalRestrictionSuite"`
Expected: FAIL. The test sources do not compile: `refusal`, `usePlain`,
`useAsked` and `placingPlain` are not members of `DarkestSecretFestival`.

- [ ] **Step 3: Implement**

**In** `src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala`:

Replace

```scala
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.model._
```

with

```scala
import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, PowerUse, PowerUseRefusal, PowerUseRestriction, PowerUseStep,
  Transform, Waiver}
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.gameplay.powers.poweruse.Placements
import distantlands.model._
```

Then replace

```scala
  * Its line names the site and not the holder, whose turn it is: "Held the
  * festival at Deep Woods".
  */
case object DarkestSecretFestival extends ContributingPower:
```

with

```scala
  * Its line names the site and not the holder, whose turn it is: "Held the
  * festival at Deep Woods".
  *
  * The marker's persistent power, from the rulebook ("Festival (Darkest
  * Secret)"): "Your enemies cannot place favor or secrets on denizens at
  * the site with the festival marker, and cannot use their powers unless
  * they're persistent powers. They can still peek at and recover relics."
  * Every player but the holder is an enemy. It guards the denizens at the
  * marked site, edifices included, while the Festival face is up and a
  * player holds it: an enemy's use of a guarded card's non-persistent
  * power is refused, as is a persistent use whose cost places on it
  * ([[Placements.byUse]]) and any placement onto one ([[Placements.onto]]),
  * as City Wall's are. Relics, the site's own powers and the cards
  * elsewhere are untouched. The holder is the granter: they may consent to
  * an enemy ignoring it, outside a Campaign, and one grant under the card's
  * use key covers both kinds of refusal. It writes no line of its own.
  */
case object DarkestSecretFestival extends ContributingPower
    with PowerUseRestriction:
```

Then replace

```scala
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Held the festival at "), NotePart.Arg(0)))
  override def noteKeys: Vector[NoteKey] = notes.declared
```

with

```scala
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Held the festival at "), NotePart.Arg(0)))
  /** "Cannot use the non-persistent powers of denizens at the festival." */
  val usePlain: NoteKey = notes.key("no-powers-here", Vector(NotePart.Text(
    "Cannot use the non-persistent powers of denizens at the festival.")))
  /** "... without the Darkest Secret holder's consent." */
  val useAsked: NoteKey = notes.key("no-powers-here-consent", Vector(
    NotePart.Text("Cannot use the non-persistent powers of denizens at the " +
      "festival without the Darkest Secret holder's consent.")))
  /** "Cannot place favor or secrets on denizens at the festival." */
  val placingPlain: NoteKey = notes.key("no-placing-here", Vector(
    NotePart.Text("Cannot place favor or secrets on denizens at the " +
      "festival.")))
  /** "... without the Darkest Secret holder's consent." */
  val placingAsked: NoteKey = notes.key("no-placing-here-consent", Vector(
    NotePart.Text("Cannot place favor or secrets on denizens at the " +
      "festival without the Darkest Secret holder's consent.")))
  /** "{card} stays deactivated." (automatic power consent design). */
  val staysDeactivated: NoteKey = notes.borrow(PowerUseStep.staysDeactivated)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private val banner = PowerSourceRef.Banner(Banner.DarkestSecret)
```

Then replace (the file's last line)

```scala
      .map(site => held.sourceless(NoteArg.Site(site)))
```

with

```scala
      .map(site => held.sourceless(NoteArg.Site(site)))

  // ---- the marker's persistent power -------------------------------------

  override val waiver: Option[Waiver] =
    Some(Waiver(holder, banner, (_, _) => useAsked(banner)))
  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override def operationRestrictions(catalog: ExecutableCatalog)
      : Vector[OperationRestriction] =
    Vector(placing)

  /** The holder, when `card` is a denizen at the marked site and `player`
    * is their enemy. */
  private def guardedFrom(ready: ReadyGame, player: PlayerId, card: CardId)
      : Option[PlayerId] =
    for
      granter <- holder(ready)
      if granter != player
      site <- ready.game.current.banners.darkestSecret.festival
      state <- ready.game.current.map.sites.get(site)
      if state.denizens.exists(_.id == card)
    yield granter

  /** The plain and asked reasons for `use`: the use's own when it is
    * non-persistent, the placing ones when its cost places now. */
  private def reasons(ready: ReadyGame, use: PowerUse)
      : Option[(NoteKey, NoteKey)] =
    if !use.persistent then Some(usePlain -> useAsked)
    else Placements.byUse(ready, use).map(_ => placingPlain -> placingAsked)

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    for
      card <- use.card
      granter <- guardedFrom(ready, use.user, card)
      keys <- reasons(ready, use)
    yield PowerUseRefusal(id, keys._1(banner), refused("festival", use.user,
      granter, PowerUseRules.target(use), keys))

  /** The refusal of `player`, waivable by `granter` under the use key of
    * `target`. */
  private def refused(code: String, player: PlayerId, granter: PlayerId,
      target: DecisionOptionRef, keys: (NoteKey, NoteKey)): OperationReason =
    OperationReason(code, s"the festival refuses ${player.value}",
      OperationReasonKind.Impossible, Some(OperationWaiver(id, Some(granter),
        ConsentKey(id, player, PowerUseRules.useKey, target), keys._2(banner),
        keys._1(banner))))

  private object placing extends OperationRestriction:
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] =
      Placements.onto(ready, operation).flatMap((player, card) =>
        guardedFrom(ready, player, card).map(granter => refused(
          "festival-placing", player, granter, PowerUseRules.cardRef(card),
          placingPlain -> placingAsked)))
```

- [ ] **Step 4: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.FestivalRestrictionSuite"`
Expected: PASS (10 tests).

- [ ] **Step 5: Rewrite the note-key snapshot**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS, 3 tests.

Run: `git diff -- src/test/resources` and `git status --short src/test/resources`
Expected: only `src/test/resources/notes/declared-keys.txt` changed, and
its diff is exactly these five added lines, after Task 2's:

```diff
 banner.darkest-secret.festival held = Held the festival at {0}
+banner.darkest-secret.festival no-placing-here = Cannot place favor or secrets on denizens at the festival.
+banner.darkest-secret.festival no-placing-here-consent = Cannot place favor or secrets on denizens at the festival without the Darkest Secret holder's consent.
+banner.darkest-secret.festival no-powers-here = Cannot use the non-persistent powers of denizens at the festival.
+banner.darkest-secret.festival no-powers-here-consent = Cannot use the non-persistent powers of denizens at the festival without the Darkest Secret holder's consent.
+banner.darkest-secret.festival stays-deactivated = {0} stays deactivated.
 banner.darkest-secret.wandering-flame.move used = {0} moved to {1}.
```

- [ ] **Step 6: Run the neighbours**

Run: `./sbtw "testOnly distantlands.gameplay.powers.poweruse.* distantlands.gameplay.powers.banner.* distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.application.*Consent* distantlands.gameplay.*Consent* distantlands.gameplay.BackendArchitectureSuite distantlands.gameplay.PhasePowerSuite"`
Expected: PASS (372 tests in the plan's own run). The consent suites list
the grid's powers by ruler, so a holder now sees the festival there.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/banner/DarkestSecretFestival.scala src/test/scala/distantlands/gameplay/powers/banner/FestivalRestrictionSuite.scala src/test/resources/notes/declared-keys.txt
git commit -m "feat(banner): the festival bars enemies from the cards it lies over" -m "While the festival marker lies at a site, the Darkest Secret's holder's enemies cannot use the non-persistent powers of the denizens there, edifices included, or place favor or secrets on them. It is a power-use restriction in City Wall's shape whose granter is the holder, so each refused option is shown askable of the holder, or with its plain reason inside a Campaign, and a grant opens it for the turn. Relics and the site's own powers are untouched." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The Festival's ribbons burn its secrets and remove its marker

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/BannerRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala`
- Modify: `src/main/scala/distantlands/model/GameViolation.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala`
- Test: `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/banner/SecretTestamentSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/VisionPlaySuite.scala`

**Interfaces:**
- Consumes: `SetFestivalMarker` (Task 2), `Table.festival` (Task 2),
  `Burn.secrets(amount, from, by)`; the test kit's
  `CampaignFixture.{raidBoard, on, RaidBoard}`, `SearchPlay.searched`.
- Produces, in `object BannerRules`:
  - `def darkestSecretRibbon(ready: ReadyGame, taker: PlayerId): Vector[CoreOperation]`;
  - `def festivalRemoval(ready: ReadyGame): Vector[CoreOperation]`.
  `ChallengeProcedure.supportedFaces` and
  `OathViolation.UnsupportedBannerState` are deleted.

- [ ] **Step 1: Write the failing tests**

**In** `src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala`:

Replace

```scala
  test("the Festival face still refuses the start"):
    val board = challenger.peoplesFavor(None, favor = 2)
      .darkestSecret(None, 1, face = DarkestSecretFace.Festival).ready
    assertEquals(start(board).left.toOption, Some(
      OathViolation.UnsupportedBannerState("unsupported active banner face")))
```

with

```scala
  test("Festival: an enemy holder loses one secret to the fire and takes " +
      "back the rest, nothing goes to sites, and the marker leaves the map"):
    val board = challenger.darkestSecret(Some(p2), secrets = 4,
        face = DarkestSecretFace.Festival).festival(BuriedGiantSite)
      .pawn(p2, at = Table.quietPawnSite(p1)).secrets(p2, faceUp = 1).ready
    val started = accepted(start(board))
    val chosen = accepted(chooseBanner(started.state, darkestSecretOption))
    val done = accepted(chooseAmount(chosen.state, 5))
    val after = Outcomes.ready(done.state)
    // 4 secrets: ONE burns and the REST, 3, return to the holder.
    assertEquals(Look(after).faceUpSecrets(p2), 1 + 3)
    assertEquals(Look(after).faceUpSecrets(p1), 6 - 5)
    assertEquals(after.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p1), 5,
        festival = None))
    assertEquals(after.game.current.map.sites.view.mapValues(_.tokens).toMap,
      board.game.current.map.sites.view.mapValues(_.tokens).toMap)
    assertEquals(MenuPlay.pendingBeyondMenu(after), None)
    assertEquals(ActPowerHarness.replayed(rules, board,
      started.events ++ chosen.events ++ done.events), after)

  test("Festival: an unclaimed banner burns every secret, and with its " +
      "marker beside the map there is no marker to remove"):
    val board = challenger.darkestSecret(None, secrets = 3,
      face = DarkestSecretFace.Festival).ready
    val done = accepted(chooseAmount(atAmount(board, darkestSecretOption), 4))
    val after = Outcomes.ready(done.state)
    assertEquals(after.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p1), 4,
        festival = None))
    assertEquals(Look(after).faceUpSecrets(p1), 6 - 4)
    assertEquals(after.game.current.map.sites.view.mapValues(_.tokens).toMap,
      board.game.current.map.sites.view.mapValues(_.tokens).toMap)
```

**In** `src/test/scala/distantlands/gameplay/powers/banner/SecretTestamentSuite.scala`:

Replace

```scala
import distantlands.gameplay.actions.campaign.CampaignIds
```

with

```scala
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.AncientCitySite
```

Then replace

```scala
import distantlands.testkit.{ActPowerHarness, Look, Outcomes}
```

with

```scala
import distantlands.testkit.{ActPowerHarness, Look, Outcomes, Table}
```

Then replace

```scala
  test("a facedown Secret Testament leaves the ribbon as it is"):
```

with

```scala
  test("on the Festival the holder takes every secret, and the marker " +
      "still leaves the map"):
    val board = Table.from(challengingTestamentHolder())
      .darkestSecret(Some(p2), secrets = 3, DarkestSecretFace.Festival)
      .festival(AncientCitySite).ready
    val done = challenged(board, Banner.DarkestSecret, 4)
    val end = Outcomes.ready(done.state)
    assertEquals(end.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p1), 4,
        festival = None))
    assertEquals(Look(end).faceUpSecrets(p2),
      Look(board).faceUpSecrets(p2) + 3)

  test("a facedown Secret Testament leaves the ribbon as it is"):
```

**In** `src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala`:

Replace

```scala
  /** Raids the rival with `attack` swords against blank defense dice,
    * sacrificing `sacrifice`, under `rulesFor`'s rules. */
  private def walk(b: Board, relic: RelicId, attack: Int, sacrifice: Int = 0,
      rulesFor: WalkerDice => OathRules = rules(_)): RaidWalk =
```

with

```scala
  /** Raids the rival's relic and `banner` with `attack` swords against
    * blank defense dice, sacrificing `sacrifice`, under `rulesFor`'s
    * rules. */
  private def walk(b: Board, relic: RelicId, attack: Int, sacrifice: Int = 0,
      rulesFor: WalkerDice => OathRules = rules(_),
      banner: Banner = Banner.PeoplesFavor): RaidWalk =
```

Then replace

```scala
        DecisionOptionRef.Relic(relic),
        DecisionOptionRef.Banner(Banner.PeoplesFavor)))))
```

with

```scala
        DecisionOptionRef.Relic(relic),
        DecisionOptionRef.Banner(banner)))))
```

Then replace

```scala
  test("the pawn cannot be relocated to its own site"):
```

with

```scala
  test("a Raid victory over the Festival burns half its secrets, rounded " +
      "down, keeps the rest on it and removes the marker from the map"):
    val RaidBoard(raided, relic) = raidBoard()
    val b = on(raided)(_.darkestSecret(Some(raided.rival), secrets = 5,
      face = DarkestSecretFace.Festival).festival(raided.origin))
    val after = ready(walk(b, relic, attack = 4,
      banner = Banner.DarkestSecret).sacrificed.state)
    // 5 / 2 = 2 burn, and 3 stay on the banner the attacker takes.
    assertEquals(after.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(b.actor), 3,
        festival = None))

  test("a Raid victory over the Wandering Flame still burns every secret"):
    val RaidBoard(raided, relic) = raidBoard()
    val b = on(raided)(_.darkestSecret(Some(raided.rival), secrets = 5))
    val after = ready(walk(b, relic, attack = 4,
      banner = Banner.DarkestSecret).sacrificed.state)
    assertEquals(after.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.WanderingFlame, Some(b.actor), 0,
        festival = None))

  test("the pawn cannot be relocated to its own site"):
```

**In** `src/test/scala/distantlands/gameplay/VisionPlaySuite.scala`:

Replace

```scala
  test("Reveal is a facedown Vision played faceup: it replaces the revealed " +
```

with

```scala
  test("a Conspiracy that takes the Festival burns half its secrets, " +
      "rounded down, by its right ribbon and removes the marker"):
    // p2, the enemy, shares p1's site and holds the Darkest Secret on its
    // Festival face with 3 secrets, its marker at their site.
    val enemy = p2
    val initial = atHome.pawn(enemy, at = home)
      .darkestSecret(Some(enemy), secrets = 3,
        face = DarkestSecretFace.Festival).festival(home)
      .worldDeckTop(conspiracy).ready
    val started = accepted(
      SearchPlay.searched(rules, OathState.Ready(initial), actor))
    val parked = accepted(
      rules.resolveWalker(started.state, actor, placeId(conspiracy),
        faceup(started.state)))
    val resolved = accepted(rules.resolveWalker(parked.state, actor,
      "cardplay.conspiracy.target", DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Banner(Banner.DarkestSecret))))
    val after = Outcomes.ready(resolved.state)
    // 3 / 2 = 1 burns, and 2 stay on the banner.
    assertEquals(after.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(actor), 2,
        festival = None))

  test("Reveal is a facedown Vision played faceup: it replaces the revealed " +
```

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.ChallengeProcedureSuite distantlands.gameplay.CampaignRaidSuite distantlands.gameplay.VisionPlaySuite distantlands.gameplay.powers.banner.SecretTestamentSuite"`
Expected: FAIL, 5 tests: both Festival Challenge tests and the Secret
Testament Festival test (the start is refused with "unsupported active
banner face"), the Raid over the Festival and the Conspiracy that takes
the Festival (each burns every secret and leaves the marker: the banner
holds 0 secrets and the marker lies where it was). "a Raid victory over
the Wandering Flame still burns every secret" passes before and after.

- [ ] **Step 3: Implement the shared ribbon**

**In** `src/main/scala/distantlands/gameplay/actions/BannerRules.scala`:

Replace

```scala
/** Shared printed banner invariants. The People's Favor's ribbons, on
  * both faces, use the printed deterministic leftmost policy. Wandering
  * Flame site ties belong to the player.
  */
```

with

```scala
/** Shared printed banner invariants. The People's Favor's ribbons, on
  * both faces, use the printed deterministic leftmost policy. Wandering
  * Flame site ties belong to the player. The Festival's ribbons remove its
  * marker from the map.
  */
```

Then replace (the file's last two lines)

```scala
        Move(Piece.Favor(1), banner,
          PositionedLocation(Location.FavorBank(suit))))
```

with

```scala
        Move(Piece.Favor(1), banner,
          PositionedLocation(Location.FavorBank(suit))))

  /** The Darkest Secret's right ribbon, as a Raid and the Conspiracy
    * adjust it: the Wandering Flame prints ALL over the burn icon, so every
    * secret on it burns; the Festival prints HALF over the burn icon, then
    * the remove-festival icon, so half its secrets, rounded down, burn and
    * the marker leaves the map. What is not burnt stays on the banner.
    * `taker` burns them. */
  def darkestSecretRibbon(ready: ReadyGame, taker: PlayerId)
      : Vector[CoreOperation] =
    val secret = ready.game.current.banners.darkestSecret
    val burned = secret.active match
      case DarkestSecretFace.WanderingFlame => secret.secrets
      case DarkestSecretFace.Festival => secret.secrets / 2
    Option.when(burned > 0)(Burn.secrets(burned,
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret)),
      Some(taker)): CoreOperation).toVector ++ festivalRemoval(ready)

  /** The remove-festival icon both Festival ribbons end with: the marker
    * leaves the map, when it is on it. */
  def festivalRemoval(ready: ReadyGame): Vector[CoreOperation] =
    Option.when(ready.game.current.banners.darkestSecret.festival.nonEmpty)(
      SetFestivalMarker(None)).toVector
```

**In** `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala`:

Replace

```scala
  * while it can, and when fewer secrets remain than tied sites the player
  * picks which of them, once.
  */
```

with

```scala
  * while it can, and when fewer secrets remain than tied sites the player
  * picks which of them, once. The Festival burns one secret and gives the
  * previous holder the rest, or burns them too when the banner was
  * unclaimed, so no secret is left to place; its last icon, the marker's
  * removal, is [[markerRemoval]], walked after this window.
  */
```

Then replace

```scala
    case Banner.DarkestSecret => Vector(
      BuildOps((ready, _) => Right(returnedSecrets(ready, actor))),
      Repeat((ready, _) => BannerRules.resources(ready.game.current,
        Banner.DarkestSecret) > 0,
        Branch((ready, _) => pass(ready, actor))))
```

with

```scala
    case Banner.DarkestSecret => Vector(
      BuildOps((ready, _) => Right(
        ready.game.current.banners.darkestSecret.active match
          case DarkestSecretFace.WanderingFlame => returnedSecrets(ready, actor)
          case DarkestSecretFace.Festival => festivalReturns(ready, actor))),
      Repeat((ready, _) => BannerRules.resources(ready.game.current,
        Banner.DarkestSecret) > 0,
        Branch((ready, _) => pass(ready, actor))))

  /** The Festival's last left-ribbon icon: the marker leaves the map. It is
    * walked after the `ChallengeRibbon` window, so a power that replaces
    * the ribbon's resources there (Secret Testament) leaves it be. */
  private[challenge] def markerRemoval(banner: Banner): Vector[Operation] =
    banner match
      case Banner.PeoplesFavor => Vector.empty
      case Banner.DarkestSecret => Vector(BuildOps((ready, _) =>
        Right(BannerRules.festivalRemoval(ready))))
```

Then replace

```scala
  private def pass(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
```

with

```scala
  /** The Festival's left ribbon: ONE over the burn icon, then REST over the
    * return-secrets icon. One secret burns, and the rest go to the
    * previous holder, or burn when the banner was unclaimed. */
  private def festivalReturns(ready: ReadyGame,
      actor: PlayerId): Vector[CoreOperation] =
    val current = ready.game.current
    val total = BannerRules.resources(current, Banner.DarkestSecret)
    val burned = math.min(1, total)
    val rest = total - burned
    Option.when(burned > 0)(Burn.secrets(burned, secretBanner,
      Some(actor)): CoreOperation).toVector ++
      Option.when(rest > 0)(
        BannerRules.holder(current, Banner.DarkestSecret) match
          case Some(holder) => Move(Piece.Secrets(rest), secretBanner,
            PositionedLocation(Location.PlayArea(holder)))
          case None => Burn.secrets(rest, secretBanner, Some(actor))
      ).toVector

  private def pass(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
```

**In** `src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala`:

Replace

```scala
  /** The start checks only the banner faces: whether a banner is in reach
    * depends on the modifiers the walk picks first, so the banner question
    * refuses the walk instead (`bannerQuestion`). */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] = for
    _ <- noStartArgs(args)
    _ <- supportedFaces(state)
  yield tree(actor)
```

with

```scala
  /** The start checks nothing of the board: whether a banner is in reach
    * depends on the modifiers the walk picks first, so the banner question
    * refuses the walk instead (`bannerQuestion`). */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    noStartArgs(args).map(_ => tree(actor))
```

Then replace

```scala
  /** The ribbon rules below are the printed ones for both People's Favor
    * faces and for the Darkest Secret's `WanderingFlame` only: the Festival
    * arrives with Foundations slice 6.
    */
  private def supportedFaces(state: ReadyGame): Either[OathViolation, Unit] =
    val banners = state.game.current.banners
    Either.cond(
      banners.darkestSecret.active == DarkestSecretFace.WanderingFlame, (),
      OathViolation.UnsupportedBannerState("unsupported active banner face"))

  private def challenge(actor: PlayerId): Operation = Sequence(Vector[Operation](
```

with

```scala
  private def challenge(actor: PlayerId): Operation = Sequence(Vector[Operation](
```

Then replace

```scala
      Vector[Operation](
        Sequence(ChallengeRibbon.steps(actor, banner),
          Some(PowerWindow.ChallengeRibbon)),
        Sequence(gained(actor, bonus) ++ Vector[Operation](
```

with

```scala
      Vector[Operation](
        Sequence(ChallengeRibbon.steps(actor, banner),
          Some(PowerWindow.ChallengeRibbon))) ++
      ChallengeRibbon.markerRemoval(banner) ++ Vector[Operation](
        Sequence(gained(actor, bonus) ++ Vector[Operation](
```

**In** `src/main/scala/distantlands/model/GameViolation.scala`:

Replace

```scala
  final case class UnsupportedBannerState(reason: String) extends OathViolation
```

with nothing (delete the line).

- [ ] **Step 4: Point the other takes at it**

**In** `src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala`:

Replace

```scala
  /** What the attacker targeted, taken: the relics, then the banners. A
    * banner's favor and secrets leave with it. */
```

with

```scala
  /** What the attacker targeted, taken: the relics, then the banners, each
    * banner adjusted by its face's right ribbon as it leaves. */
```

Then replace

```scala
    defeated(ready, result).map { (defender, _) =>
      val current = ready.game.current
      val defenderId = defender.player
```

with

```scala
    defeated(ready, result).map { (defender, _) =>
      val defenderId = defender.player
```

Then replace

```scala
      // A banner's favor and secrets leave as its Take's children, so a
      // refused Take leaves the banner whole.
```

with

```scala
      // A banner's ribbon runs as its Take's children, so a refused Take
      // leaves the banner whole, and the festival marker where it lies.
```

Then replace

```scala
          case Banner.DarkestSecret =>
            val secrets = BannerRules.resources(current, banner)
            Option.when(secrets > 0)(Burn.secrets(secrets,
              PositionedLocation(Location.OnBanner(banner)),
              Some(attacker))).toVector
```

with

```scala
          case Banner.DarkestSecret =>
            BannerRules.darkestSecretRibbon(ready, attacker)
```

**In** `src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala`:

Replace

```scala
            case Banner.DarkestSecret =>
              val secrets = BannerRules.resources(current, held)
              Option.when(secrets > 0)(Burn.secrets(secrets,
                PositionedLocation(Location.OnBanner(held)), Some(actor)))
                .toVector
```

with

```scala
            case Banner.DarkestSecret =>
              BannerRules.darkestSecretRibbon(ready, actor)
```

- [ ] **Step 5: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.ChallengeProcedureSuite distantlands.gameplay.CampaignRaidSuite distantlands.gameplay.VisionPlaySuite distantlands.gameplay.powers.banner.SecretTestamentSuite"`
Expected: PASS (44 tests in the plan's own run).

Run: `./sbtw "testOnly distantlands.gameplay.Campaign* distantlands.application.gamelog.* distantlands.gameplay.powers.whenplayed.* distantlands.gameplay.powers.banner.* distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS (703 tests in the plan's own run). No golden changes: the
ribbons write no line. If `GameLogGoldenSuite` fails, stop and report; do
not rewrite a golden.

Run: `git grep -n "UnsupportedBannerState\|supportedFaces" -- src shared frontend/src`
Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/BannerRules.scala src/main/scala/distantlands/gameplay/actions/challenge/ChallengeRibbon.scala src/main/scala/distantlands/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/distantlands/model/GameViolation.scala src/main/scala/distantlands/gameplay/actions/campaign/CampaignRaid.scala src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala src/test/scala/distantlands/gameplay/ChallengeProcedureSuite.scala src/test/scala/distantlands/gameplay/powers/banner/SecretTestamentSuite.scala src/test/scala/distantlands/gameplay/CampaignRaidSuite.scala src/test/scala/distantlands/gameplay/VisionPlaySuite.scala
git commit -m "feat(challenge): the Festival's ribbons burn its secrets and remove its marker" -m "The Festival's left ribbon prints ONE over the burn icon and REST over the return-secrets icon: a Challenge burns one secret and gives the previous holder the rest, or burns them too from the shared bank. Its right ribbon prints HALF over the burn icon: a Raid or the Conspiracy burns half, rounded down, and the rest stay on the banner. Both end with the remove-festival icon, so the marker leaves the map; a Challenge walks that after the ribbon window, so Secret Testament leaves it. Challenge no longer refuses any banner face, and the refusal is deleted." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Docs and verification

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
- Consumes: everything Tasks 1 to 5 built; nothing new is produced.

Docs are plain prose. `docs/rules/ambiguities.md` and
`docs/rules/implementation-traceability.md` hold very long lines (numbered
items and table rows), so their blocks below are parts of a line, not
whole lines, and several blocks in the other docs start or end inside a
line too: replace exactly the text shown and leave the rest of the line.
The roadmap deletes finished work outright: no checked box and no "landed"
note. `CONTEXT.md` waits for slice 7, the batch's last task, as the spec's
Docs list says.

- [ ] **Step 1: The roadmap, the architecture docs, the ambiguities and the traceability table**

**In** `docs/ROADMAP.md`:

Replace

~~~markdown
- [ ] **The Festival banner face.** Altered Foundation VI turns the
  Darkest Secret to it. Its id is in `RuleSourceIndex`, and the face has
  no behaviour.
~~~

with nothing (delete the three lines).

**In** `docs/architecture/bounded-banners.md`:

Replace

~~~markdown
pp. 12 and 15 for the first-game banner faces, Mob and Wandering Flame, and
for the Grand Council: an altered Foundation V starts the People's Favor on
its Grand Council face
([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md),
slice 5). The face identities are typed state. The Festival face has no
implemented power: `RuleSourceIndex` lists its handler id, and it is
ignored and recorded like any other unimplemented power.
~~~

with

~~~markdown
pp. 12 and 15 for the first-game banner faces, Mob and Wandering Flame, and
for the altered ones: an altered Foundation V starts the People's Favor on
its Grand Council face, and an altered Foundation VI the Darkest Secret on
its Festival face
([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md),
slices 5 and 6). The face identities are typed state, and so is the site
the festival marker lies over (`DarkestSecretState.festival`).
~~~

Then replace

~~~markdown
Raid and Conspiracy all adjust the banner through it, including the printed
leftmost tied-bank rule. Only Wandering Flame site ties create a
player decision.
~~~

with

~~~markdown
Raid and Conspiracy all adjust the banner through it, including the printed
leftmost tied-bank rule. The Darkest Secret's right ribbon
(`darkestSecretRibbon`) reads the face too, for a Raid and the Conspiracy,
and both Festival ribbons end by removing the marker (`festivalRemoval`).
Only Wandering Flame site ties create a player decision.
~~~

(The paragraph goes on with " Nothing derives behaviour" on the same line;
leave it.)

Then replace

~~~markdown
walker tree. Its start gates refuse the Festival face
(`UnsupportedBannerState`) and a start with no legal banner.
~~~

with

~~~markdown
walker tree. Its start gate refuses a start with no legal banner.
~~~

(The paragraph goes on with " A banner is" on the same line; leave it.)

Then replace

~~~markdown
  It then places the half on the least-stocked sites one pass at a time. When
  fewer secrets remain than tied sites, the challenger picks which sites.
~~~

with

~~~markdown
  It then places the half on the least-stocked sites one pass at a time. When
  fewer secrets remain than tied sites, the challenger picks which sites.
- Darkest Secret on the Festival burns one secret and gives the previous
  holder the rest, or burns them too when the banner was unclaimed. After
  the ribbon window, so a power that replaces the ribbon's resources there
  leaves it be, the festival marker leaves the map.
~~~

Then replace

~~~markdown
move the pawn to another site with a secret on it, or place a faceup secret on
the pawn's site. `BannerFacePowers` registers them.
~~~

with

~~~markdown
move the pawn to another site with a secret on it, or place a faceup secret on
the pawn's site. The Festival face is a contribution and a power-use
restriction (`DarkestSecretFestival`): at the start of its holder's Rest
(`RestStart`) the marker goes to the holder's pawn site, and while it lies
there the holder's enemies cannot place favor or secrets on the denizens at
that site or use their non-persistent powers, unless the holder consents.
`BannerFacePowers` registers them.
~~~

**In** `docs/architecture/bounded-visions-and-conspiracy.md`:

Replace

~~~markdown
Foundation III's faces, Empire-only Vision rules, and the Festival banner
face have no behaviour. A Conspiracy that takes the People's Favor adjusts it
by its face's right ribbon (`BannerRules.peoplesFavorRibbon`).
~~~

with

~~~markdown
Foundation III's faces and Empire-only Vision rules have no behaviour. A
Conspiracy that takes a banner adjusts it by its face's right ribbon
(`BannerRules.peoplesFavorRibbon`, `BannerRules.darkestSecretRibbon`).
~~~

(The paragraph goes on with " `RuleSourceIndex` lists the" on the same
line; leave it.)

**In** `docs/architecture/all-exile-round-endings.md`:

Replace

~~~markdown
The Festival
banner face and the Foundation faces have no implemented Rest power, so their
handlers are ignored and recorded the same way at Rest start.
~~~

with

~~~markdown
The Foundation
faces have no implemented Rest power, so their handlers are ignored and
recorded the same way at Rest start. Begin Rest folds the `RestStart` window,
where the Festival's holder places its marker (`DarkestSecretFestival`).
~~~

**In** `docs/architecture/game-setup.md`:

Replace

~~~markdown
Council, starts the People's Favor on its Grand Council face, as Mob's
Favor starts it on the Mob.
~~~

with

~~~markdown
Council, starts the People's Favor on its Grand Council face, as Mob's
Favor starts it on the Mob, and an altered Foundation VI, Festival, starts
the Darkest Secret on its Festival face with its marker beside the map, as
Wandering Flame starts it on the Wandering Flame.
~~~

(The paragraph goes on with " The sites Imperial Maps places in" on the
same line; leave it.)

**In** `docs/rules/ambiguities.md` (item 2 of "Component-data
dependencies"):

Replace

~~~markdown
An altered V, Grand Council, starts the People's Favor on its Grand Council face; the altered face of VI is refused at game start until its slice of the [Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md), and Foundation III's faces
~~~

with

~~~markdown
An altered V, Grand Council, starts the People's Favor on its Grand Council face, and an altered VI, Festival, the Darkest Secret on its Festival face ([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md), slices 5 and 6). Foundation III's faces
~~~

**In** `docs/rules/implementation-traceability.md`:

Replace

~~~markdown
Grand Council, altered Foundation V, starts the People's Favor on its Grand Council face (`FirstGameSetupMaterializer`, `AlteredFoundationSetupSuite.scala`).
~~~

with

~~~markdown
Grand Council, altered Foundation V, starts the People's Favor on its Grand Council face, and Festival, altered Foundation VI, the Darkest Secret on its Festival face with its marker beside the map (`FirstGameSetupMaterializer`, `AlteredFoundationSetupSuite.scala`).
~~~

Then replace

~~~markdown
 Altered Foundation VI is refused at game start until its slice of the [Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md).
~~~

with nothing (delete exactly this text, its leading space included, and
keep the rest of the line: " Imperial Maps fills from the end" follows
the sentence before it).

Then replace

~~~markdown
| Challenge and banner resource actions | **Tested (Mob, Grand Council and Wandering Flame faces)** |
~~~

with

~~~markdown
| Challenge and banner resource actions | **Tested (Mob, Grand Council, Wandering Flame and Festival faces)** |
~~~

Then replace

~~~markdown
The Grand Council lets its holder play a card to any Imperial site and discard a card at one first (`PeoplesFavorGrandCouncil`, `PeoplesFavorGrandCouncilSuite`).
~~~

with

~~~markdown
The Grand Council lets its holder play a card to any Imperial site and discard a card at one first (`PeoplesFavorGrandCouncil`, `PeoplesFavorGrandCouncilSuite`). The Festival's holder places its marker at their pawn's site at each Rest, its enemies cannot place favor or secrets on the denizens there or use their non-persistent powers without the holder's consent, and both its ribbons remove the marker (`DarkestSecretFestival`, `FestivalRestSuite`, `FestivalRestrictionSuite`, `BannerRules.darkestSecretRibbon`).
~~~

Then replace

~~~markdown
| The Festival face, mutable Foundation selection,
~~~

with

~~~markdown
| Mutable Foundation selection,
~~~

Then replace

~~~markdown
| Foundation III's faces, the Festival banner face and other Vision/reveal modifiers remain deferred;
~~~

with

~~~markdown
| Foundation III's faces and other Vision/reveal modifiers remain deferred;
~~~

Then replace

~~~markdown
Narrow Pass is waivable on Travel and on campaign targeting, Secret Police on a faceup Vision,
~~~

with

~~~markdown
Narrow Pass is waivable on Travel and on campaign targeting, Secret Police on a faceup Vision, the festival marker on an enemy's use of a denizen at its site (`FestivalRestrictionSuite`),
~~~

- [ ] **Step 2: The spec**

The status line, and the slice 6 section: the ribbons as read, what was
built, and the readings taken, for the owner to confirm.

**In** `docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b, 4 and 5 landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2, 3a, 3b, 4, 5 and 6 landed.
~~~

Then replace

~~~markdown
  secrets on the banner stays. Its refusal goes.
~~~

with

~~~markdown
  secrets on the banner stays. Its refusal goes, the last one: game start
  now refuses no altered face.
~~~

Then replace

~~~markdown
- **Removal.** The Challenge ribbon that removes the festival marker clears
  it; the plan reads the face's ribbons from `banners.pdf`, as in slice 5.
- **Tests.** Placement each Rest and moving with the pawn; each blocked use;
  persistent powers still working; consent waiving it; a Challenge clearing
  it; a golden log.
~~~

with

~~~markdown
- **Removal.** Both of the face's ribbons, read from
  `reference/2026-03-17-new-foundations-rev1/banners.pdf`, end with the
  remove-festival icon. The left (Challenge) prints "ONE" over the burn
  icon, then "REST" over the return-secrets icon: one secret burns, and the
  rest go to the previous holder, or burn when the banner was in the shared
  bank. The right (Campaign) prints "HALF" over the burn icon: half the
  secrets, rounded down, burn, and the rest stay on the banner. The
  Wandering Flame prints "HALF" over the scatter icon and "REST" over the
  return-secrets icon on the left, and "ALL" over the burn icon on the
  right.
- **Tests.** Placement each Rest and moving with the pawn; each blocked use;
  persistent powers still working; consent waiving it; a Challenge clearing
  it; a golden log.
- **As built.** Game start reads Foundation VI from the faces it resolved
  (`FirstGameSetupMaterializer`), so the marker starts beside the map, and
  `StartingFoundations` has no refusal left. Begin Rest now folds the
  `RestStart` window, declared since the Rest walker and folded by no
  procedure until now. `DarkestSecretFestival`, beside the Grand Council in
  `BannerFacePowers`, is both the Rest contribution and the power-use
  restriction, in City Wall's shape: one `PowerUseRestriction` and one
  operation restriction on placements, its waiver granted by the holder.
  The marker moves by a journaled `SetFestivalMarker`. Challenge walks the
  left ribbon (`ChallengeRibbon`), and the marker's removal after the ribbon
  window, so Secret Testament, which replaces the ribbon's resources there,
  leaves it; a Raid and the Conspiracy walk the right ribbon
  (`BannerRules.darkestSecretRibbon`). `ChallengeProcedure.supportedFaces`
  and `OathViolation.UnsupportedBannerState` are gone. The projection gives
  each site a required `festival` flag, and the World pane ends that site's
  pawn row with a Festival pill.
- **Readings taken with the plan,** for the owner to confirm. Every other
  player is the holder's enemy, whatever their role. "Their powers" are the
  powers of the denizens at the marked site, edifices included, not the
  enemies' own powers used there; relics and the site's own powers are
  untouched, and a denizen's action modifier is one of its powers.
  "Persistent" is the power as printed. Placing favor or secrets on a
  denizen is what City Wall's ban reads: a cost paid onto it on the payer's
  turn, a gift onto it, or a forced placement from the placer's board, and
  a persistent power whose cost places on it. Consent is asked outside a
  Campaign only. The marker starts beside the map and first goes on it at
  the holder's first Rest; each Rest places it at the pawn's site, even
  where it already lies. A holder with no pawn on the map places nothing.
  With nobody holding the banner the marker guards nothing and stays where
  it lies. A take that walks no ribbon (Wizard's Conclave, or Hall of
  Mockery and Lost Guardians moving the banner to the shared bank) leaves
  the marker, which then guards for the new holder. "ONE" on an empty banner
  burns nothing. Under Secret Testament the holder takes every secret, and
  the marker still leaves the map. A first game with an altered VI starts
  the Festival in the shared bank.
~~~

If the owner has ruled on any reading in "Rulings needing the owner's
review" by the time you run this task, write the ruling in place of the
reading, and say so in your report. If you took a reading of your own
while working, add it to the "Readings taken with the plan" item.

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%. The plan's own run of these tasks at `2fb28fa34` passed
5893 tests at 95.33% and 89.65%.

Run: `./sbtw "frontend/test"` (the frontend suites, which the full run
skips)
Expected: PASS (935 tests in the plan's own run).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. The note-key snapshot and the golden pair were committed
with Tasks 2 and 4, and nothing else changed.

Run: `git grep -n "UnsupportedBannerState\|supportedFaces\|StartingFoundations.refusal\|arrivesWith\|Festival face has no\|the Festival banner face\|Festival banner face and\|The Festival banner face\|altered face of VI is refused\|Altered Foundation VI is refused" -- docs/rules docs/architecture docs/ROADMAP.md src shared frontend/src`
Expected: no output. Nothing still says the Festival is missing or
refused, and nothing names the deleted code.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/architecture/bounded-banners.md docs/architecture/bounded-visions-and-conspiracy.md docs/architecture/all-exile-round-endings.md docs/architecture/game-setup.md docs/rules/ambiguities.md docs/rules/implementation-traceability.md docs/superpowers/specs/2026-11/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 6, the Festival" -m "The roadmap drops the Festival face, the banner, Conspiracy, Rest and setup docs describe the Festival's marker, its persistent power and its ribbons, the ambiguities and the traceability table stop calling the face refused or missing, and the spec records slice 6 landed, the ribbons as printed, what was built and the readings taken." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## For slice 7 and later

- `CONTEXT.md` gains "Festival marker" with the batch's other terms
  (Explore, Hintermost, empty slot), in slice 7's docs task.
- The Needle ("After you take a banner in any way, you may flip it to its
  other side") and the legacies that set Foundation VI add a way for the
  face to change in play; `SetFestivalMarker` then needs a rule for a
  marker left on the map when the face turns to the Wandering Flame. Today
  the face never changes during a game.

## Self-review

**Spec coverage** (slice 6):

| Spec statement | Task |
|---|---|
| An altered VI sets Darkest Secret to `Festival`; its refusal goes | 1 (`darkestSecretFace`, `StartingFoundations`) |
| Check `OATH-SETUP09b-NF`: holder, secret, goal, marker nearby | 1 (both Festival tests; ruling 12) |
| The Wandering Flame's actions go with the face; placing secrets on the banner stays | 1 (`BannerFaceProjectionSuite`; finding 2) |
| `DarkestSecretState.festival: Option[SiteId]` | 1 |
| Projected (required wire field) and drawn on the map | 3 |
| At `RestStart` the holder places the marker at their pawn's site, automatically | 2 (`DarkestSecretFestival`, `BeginRestProcedure`) |
| "Held the festival at Deep Woods", the actor omitted | 2 (the golden log) |
| Enemies cannot place favor or secrets on denizens at the site | 4 (placing restriction) |
| ... and cannot use their powers unless persistent | 4 (power-use restriction) |
| Peeking at and recovering relics untouched | 4 ("... no relic"; ruling 3) |
| The holder is the granter; bandits always enforce | 4 (consent tests; ruling 6) |
| Each refused option shows disabled with its reason | 4 (the Act menu test, the Campaign test) |
| The ribbon that removes the marker clears it; ribbons read from `banners.pdf` | "What the banner image shows"; 5 |
| Every take by ribbons follows the face; `supportedFaces` stops refusing | 5 (Challenge, Raid, Conspiracy; finding 5) |
| Tests: each Rest and moving with the pawn; each blocked use; persistent powers working; consent; a Challenge clearing it; a golden log; game start with the face | 1 to 5 |
| Docs and the full gates | 6 |

**Verified by running:** the prototype was committed per task in a
scratch worktree at `2fb28fa34`; the plan's blocks were then applied in
order, step by step, to a second fresh worktree at `2fb28fa34`. Each "see
it fail" step failed exactly as written, each task's tree matched the
prototype's, and the final tree passed `./sbtw clean coverage test
coverageReport` (5893 tests, 95.33% statements, 89.65% branches),
`./sbtw "frontend/test"` (935 tests) and `scripts/check-architecture.py`,
with `src/test/resources` clean.

**Left for later, on purpose:** `CONTEXT.md` (slice 7, as the spec's Docs
list says); the marker when the face turns in play (no rule turns it yet).

**Placeholders:** none. Every step that changes code shows the code, and
each file written by a command shows its expected text or diff.

**Names used across tasks:** `DarkestSecretState.festival` (Task 1);
`SetFestivalMarker`, `BoardControlOperations.setFestivalMarker`,
`DarkestSecretFestival.{id, held, holder}`, `Table.festival` (Task 2);
`SetupSiteProjection.festival`, `WorldBoardRenderer.festivalNote` (Task
3); `DarkestSecretFestival.{usePlain, useAsked, placingPlain,
placingAsked, staysDeactivated, refusal}` (Task 4);
`BannerRules.darkestSecretRibbon`, `BannerRules.festivalRemoval`,
`ChallengeRibbon.markerRemoval` (Task 5). The suites' helpers name what
they build: `festivalUnplaced`, `festivalHolderActingAtHome`, `rested`,
`terrorAtTheFestivalOfP2`, `useOf`, `terrorOption`, `pawnRow`, `world`,
`marked`. Each is spelled the same wherever it appears.
