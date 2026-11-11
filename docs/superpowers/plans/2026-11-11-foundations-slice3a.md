# Foundations, Slice 3a: Empty Slots, Wide Horizons at Game Start and Setup With No Empire Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A map may hold fewer than eight sites. A game whose Chronicle
stores Wide Horizons (altered Foundation I) starts with the Empire's sites
alone, a later game with no Chancellor and no Citizen at the table starts
with no Empire instead of being refused, and a game played on such a map
goes through the Chronicle Phase into the next game.

**Architecture:** A region's vector in `MapState` may be shorter than its
slots (`Region.slots`), and game start builds the map from the sites it
places (`MapState.hinterward`). `EmpireSetupRules.resolve` returns how the
game starts, a `SetupMode`: a first game, a game with an Empire, or Setup
With No Empire. `GameStartRules` reads the mode and Foundation I's face to
decide how many slots it fills from the atlas, `FirstGameSetupMaterializer`
populates the sites as before, and `LaterGameSetup` sets the facts off the
map. Everything is built directly at game start; nothing is journaled. The
projection gives each region its empty slots and the table draws them as
empty frames. The Explore action, which fills an empty slot in play, is
slice 3b and is not in this plan.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`; Scala.js
frontend under `frontend/`, tested with jsdom; shared protocol sources under
`shared/`.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings", "Approach" and "Slice 3: Wide Horizons, Explore and
Setup With No Empire" except its "Explore" subsection, which is slice 3b.
Slice 4 is read for the empty slots it reuses.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `gameplay` must not read a catalog `.text` or `rulesText`
  (`scripts/check-architecture.py`). `model` imports nothing from
  `gameplay`: `SetupMode` lives in `gameplay.setup`, and
  `FirstGameSetupPlan`, in `model`, carries only model types.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. Tests go in the repo's existing suites where one
  covers the subject. A test helper's name says what it builds.
- Wire and journal changes need not keep old journals decodable. This slice
  journals nothing new. The game projection's region gains `emptySlots`,
  which a payload without it decodes as 0.
- Owner rulings (spec, "Rulings" and slice 3), binding here:
  - No Empire sets the Reliquary aside. "Discard all relics from the
    Reliquary. They return in the Chronicle Phase." The Reliquary's relics
    stay `campaign.reliquary` all game and never enter play; the slot fill
    cannot draw them (ruling 3).
  - With no Empire, the Oathkeeper title and both banners start as in a
    first game.
  - Foundations erode: I becomes Wide Horizons and V becomes Mob's Favor in
    `campaign.foundations`, with no sources.
  - Every Citizen in the box becomes an Exile (`boxRoles`). The plan's
    random first player begins. The Usurper Limiter is on.
- New Foundations rules only. The rulebook's "Setup for Later Games"
  (Players, Empire, Setup With No Empire, Foundations, Player Choices) is in
  `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`, about
  lines 213 to 410. The printed setup cards are in
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`,
  ids `OATH-SETUP02b-NF`, `OATH-SETUP03a-NF`, `OATH-SETUP03b-NF`,
  `OATH-SETUP04a-NF` and `OATH-SETUP04b-NF` (each entry's `text:` comes
  before its `name:`). Read them there; make no web call. The Empire
  rulings R6 and R25 to R30 are in `docs/rules/ambiguities.md` and in the
  code that cites them.
- If a rule is unclear while you work, do not guess silently: take the most
  literal reading of the New Foundations text, add it to the spec's slice 3
  readings (Task 8, Step 4), and name it in your report.
- An unavailable option is shown disabled with its reason, never hidden.
- A Game Log line omits the actor on their own turn. This slice writes no
  line; the rule binds any you add.
- No note key and no Game Log golden changes in this slice. If
  `DeclaredNoteKeysSuite` or `GameLogGoldenSuite` fails, stop and read the
  diff before rewriting a snapshot. The rewrite commands, for a change you
  have understood, are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
- Frontend work follows `DESIGN.md`: read its "Layout", "Cards", "Sites"
  and "Do's and Don'ts" sections before Task 6, and use only its tokens
  (`--line-mid`, the 9px radius, `--site-h`). No `transition`, no
  `animation`, no new color.
- The words are "slot" for a place on the map a site can lie in, "empty
  slot" for one that holds none, "Hintermost" for the last site in
  Hinterward order, and "face" for a foundation card's two faces.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `d8cabe43c` (the merge of slice 2 into `0.2.0-alpha`) with the earlier
  tasks of this plan applied, and occurs once in its file. "Then replace"
  is the next block in the same file. "Add at the end of" appends the
  block after the file's last line, one blank line between them, at the
  indentation shown. "Create" and "Replace the whole of" give a file's
  full text. If a block does not match because a
  later fix moved the text, find the same code by its content and make the
  same change; if the code itself is different, stop and report.
- Shell is zsh: never write a bare `===` or `=word` token in a command, and
  quote globs (`--include='*.scala'`).
- Never `git add -A` or `git add .`; add files by path. Never push. No bare
  `git stash`.
- Commit messages and docs are plain prose. Every commit message ends with
  the trailer `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`.
  Use exactly that trailer whatever model you run on: the plan's trailer
  overrides your own. Each commit command below passes the subject, the
  body and the trailer as three `-m` arguments; keep that form, which needs
  no heredoc.

---

## Setup

Work in a git worktree cut from `0.2.0-alpha`, which holds slices 1 and 2.
In the worktree, link the main checkout's tooling before the first
`./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor 92fb2e773 HEAD && echo slice1-ok
git merge-base --is-ancestor d8cabe43c HEAD && echo slice2-ok
git grep -n "FoundationNumber.I -> 3" -- src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala
git grep -n "def altered(number: FoundationNumber)" -- src/main/scala/distantlands/model/Setup.scala
git grep -n "val deal = SetupDeal.of(chronicle, orders.participants)" -- src/main/scala/distantlands/gameplay/setup/GameStartRules.scala
git grep -n "val slots = 8 - chronicle.world.size" -- src/main/scala/distantlands/gameplay/setup/GameStartRules.scala
git grep -n "plan.orderedSites.slice(5, 8)," -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "if actual != expected then" -- src/main/scala/distantlands/model/DomainValidation.scala
git grep -n "if required && refs.nonEmpty && rejected.size == refs.size then" -- src/main/scala/distantlands/gameplay/walker/WalkerSearch.scala
git grep -n "def foundation(number: FoundationNumber, face: FoundationFace)" -- src/test/scala/distantlands/testkit/Table.scala
git grep -n "an Empire-less later game requires Wide Horizons" -- src
git grep -n "Slices 1 and 2 landed" -- docs/superpowers/specs/2026-11-11-foundations-design.md
```

The first must print `slice1-ok`: `92fb2e773` is the merge that brought
slice 1 into `0.2.0-alpha`. The second should print `slice2-ok`:
`d8cabe43c` is the merge of slice 2 this plan was cut from. Each grep must
print one line, except the Empire-less one, which prints three (the rule in
`EmpireSetupRules`, and its expectation in `GameStartRulesSuite` and
`TrustedGameProvisioningSuite`). They name the code this plan edits: the
refusal of an altered Foundation I it deletes, slice 2's
`FirstGameSetupPlan.altered` and `SetupDeal` it relies on, the two places
that count eight sites, the region-size check, the walker search's
refusal, the test table's foundation step, the refusal of an Empire-less
game, and the spec's status line. If `slice1-ok` is missing, the worktree
was cut from a stale branch: stop and report. If only `slice2-ok` is
missing while every grep prints, slice 2 was merged again under another
commit: go on. If a grep prints nothing while the others print, the base
has moved under that file: read the file, and go on only if the code the
task edits is still there in substance.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`, a frontend suite as
`./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

## Plan-time findings

The spec was checked against the code at `d8cabe43c`. Where they differ,
this plan follows the code, and Task 8 corrects the spec.

1. **More than the materializer counts eight.** The spec says "at design
   time only the materializer slices eight". `GameStartRules` computes
   `8 - chronicle.world.size` twice (the atlas sites it places, and the
   atlas it keeps), `EmpireSetupRules` refuses more than 8 Empire sites,
   `DomainValidation` reports any region that does not hold exactly 2, 3
   and 3 sites, and `GamePresentationProjector.readyWorld` sends each
   region's sites with no word of the slots. The materializer's own
   `take(2)`, `slice(2, 5)` and `slice(5, 8)` already give short vectors
   for fewer sites. Task 1 gives the slots one definition, `Region.slots`.
2. **Every reader in play counts the sites on the map.** Each of the
   readers of `MapState.inPlay`, `hinterward` and `regionOf` under
   `gameplay` (about ninety) either walks the sites that exist or starts
   from an existing site and reads its region, so none assumes eight sites
   or a full region. The discard piles belong to the regions, not to their
   sites, and stay three. Travel's costs read the two sites' regions.
   `ChronicleCrown` takes `inPlay.size` Imperial warbands, one per site on
   the map. Task 5 pins the two that could surprise (finding 3) and the
   Chronicle Phase is pinned whole by Task 7.
3. **Stone Portal does not wrap, and a map of one site left it a question
   with no answer.** The spec says "the Shattered Portal and Stone Portal
   wraps already walk `hinterward`". Shattered Portal's does
   (`ShatteredPortal.destination`), so it skips empty slots, and a roll
   that is a multiple of the number of sites returns the pawn to where it
   stands; its doc comment says "the map holds 8 sites". Stone Portal
   offers every other site (`PawnMoves.sitesOtherThan`). On a map of one
   site that list is empty, the Act menu still offered the Portal, and
   using it parked the walk on a choose-one with no option, which only Undo
   leaves. The other powers that offer "another site" (Passages, Magic
   Carpet, Quick Exit, Palanquin and more) were not each tried on one
   site. A map of one site is common once Wide Horizons is in play: a
   winner often rules one site. Task 5 closes the gap in the walker's
   search, for whichever of them has it (design decision 5).
4. **Travel is already safe.** On a map of one site the Act menu shows
   Travel unavailable with "No site to travel to" (`ActOffers.reason`): the
   search already refuses a decision that offers no answer when it reaches
   one from the menu. Task 5 pins it.
5. **The Chronicle Phase needs no change.** `ChronicleExtraction` archives
   and keeps sites in `hinterward` order and carries the atlas over in
   order, Shape Empire, Build Edifice, Offer Citizenship and Scatter Relics
   walk `inPlay`, and `ChronicleRecord` already writes
   `campaign.foundations` and the box roles forward. Task 7 is tests only.
6. **An Exile's win discards the Reliquary before Stock Reliquary.** Owner
   ruling 3 ends "the Chronicle's Stock Reliquary step adds to them".
   After Setup With No Empire the winner is an Exile unless a player became
   the Chancellor in play, and `ChronicleCrown.empireBreaks` then discards
   the Reliquary, as in every game an Exile wins; Scatter Relics deals
   those relics to the sites. Stock Reliquary adds to the set-aside relics
   only when a player who became the Chancellor in play wins. This plan
   keeps that behaviour (ruling 7 below) and Task 7 pins it.
7. **"Which here is every site" is not so.** The spec says an Exile may
   place their pawn "at any site ruled by bandits, which here is every
   site". A site of capacity 0 holds no warband and is ruled by nobody
   (R25), so it is not offered (ruling 6 below).
8. **`EmpireSetupRules.resolve` cannot say "no Empire".** It returns
   `Option[EmpireSetup]`, and `None` means a first game: `GameStartRules`
   reads it to check Homeland edifices and to refuse stored cards at atlas
   sites. Setup With No Empire is a third way to start, so `resolve`
   returns a `SetupMode` (design decision 2).
9. **The plan factory already picks a random first player.**
   `GeneratedFirstGamePlanFactory.seating` shuffles the seats and falls
   back on the first seat when no Chancellor and no Citizen is seated. Only
   its special case for a Chronicle that lists no Chancellor goes (Task 3).
10. **`SetupPawnSites.legal` offers every site whenever no site holds an
    Imperial warband,** which is right for a first game and wrong after
    Setup With No Empire, where a site with no warbands is not offered.
    Nothing in the live state says "first game" but the baseline: a later
    game's Chronicle always has a `world` (R28). Task 4 reads it.
11. **`ReliquaryDraws` would hand out a set-aside relic.** Bewitch, Ancient
    Pact and Royal Ambitions take a random Reliquary relic, and in a game
    with no Empire they make their user the Chancellor (CR p. 34). The
    first-game option ignores these powers today, and game start never
    turns it off, but a table can (`Table.allowingCitizenshipPowers`), and
    the Settings Feature phase will. Task 4 makes the draw find a set-aside
    Reliquary empty.
12. **Two server suites expect the old refusal.**
    `TrustedGameProvisioningSuite` and `TrustedSeatRoutesSuite` create a
    game from `EvolvedChronicleFixture.record`, whose Chancellor is not
    seated, and expect "an Empire-less later game requires Wide Horizons".
    That Chronicle now starts a game with no Empire. Task 3 gives both a
    Chronicle game start still refuses, an Empire with no site, and adds
    the start with no Empire as its own test.
13. **`GameStartRulesSuite`'s "a non-empty world is refused" tested the
    Empire-less refusal.** Its Chronicle has a `world` and no Chancellor,
    which the old rule refused as Empire-less. Under ruling 1 that is a game
    with no Empire, and what stops this Chronicle is that it stores the same
    site in `atlasBox` and in `world`. Task 3 rewrites the test to say so.
14. **The frontend reads no slot count.** `WorldBoardRenderer.world` draws
    `region.sites` and nothing else, the three regions are a 3-column grid
    and each region lists its sites in one column, so a region with
    fewer sites simply shows fewer boxes. `--site-h`, the height of a
    site, is declared on `.site` itself, where an empty frame beside it
    cannot read it. Task 6 moves it to `.sites`.
15. **R6 is cited in code and has no entry in `docs/rules/ambiguities.md`**
    (R5 is followed by R7). It is ruling R6 of the Empire phase design: a
    seated Citizen becomes the Chancellor when the Chancellor's lineage
    sits out. This plan cites it and does not restore the entry.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **The slots live on `Region`.** `Region.slots` is 2, 3 and 3,
   `MapState.slots` their sum, and `MapState.hinterward(placed)` the one
   place that lays sites out in slots. `MapState` gains `sitesIn(region)`,
   `emptySlots(region)`, `nextSlot: Option[Region]` and
   `hintermost: Option[SiteId]`. `DomainValidation` keeps
   `WrongRegionSize`, now only for a region with more sites than slots. It
   does not check that the empty slots follow the sites: test fixtures lay
   out maps with gaps, and nothing in play removes a site.
2. **`SetupMode`.** `EmpireSetupRules.resolve` returns
   `SetupMode.FirstGame`, `SetupMode.WithEmpire(empire)` or
   `SetupMode.NoEmpire(sites, exiled)`, in `gameplay.setup`.
   `GameStartRules.buildPlan` returns the plan and the mode, and
   `LaterGameSetup` takes the mode. `FirstGameSetupPlan` keeps its fields:
   with no Empire, `empire` is `None`, `orderedSites` the former Empire's
   sites, and `atlasSites` those same sites with their stored cards, which
   the materializer already sets up as bandit sites with ruined edifices
   and open relic slots filled (`atlasSite`).
3. **The Reliquary set aside is a flag.**
   `CampaignState.reliquarySetAside`, set by `LaterGameSetup` with no
   Empire. The relics stay in `campaign.reliquary` (owner ruling 3), so
   the card index and the Chronicle's accounting hold them as before, and
   the one reader in play, `ReliquaryDraws`, asks the flag.
4. **A first game is a baseline with an empty `world`.**
   `SetupPawnSites.legal` reads `campaign.baseline.world.isEmpty` (finding
   10) rather than carry the mode into the live game.
5. **The walker refuses a choose-one that declares no option.**
   `WalkerSearch.narrow`, which cuts a decision where it is asked, treats
   a required choose-one with no option at all as `reach` already treats
   it when a search arrives at one: refused with "the decision offers no
   answer". The command that would park there is refused and changes
   nothing, and the Act menu, which searches each of its options, shows
   the action unavailable: "Nothing to do now" for a power, the action's
   own reason for an action. This covers every "another site" power on a
   map of one site at once (finding 3). A choose-many is left alone: with
   nothing to choose it is cut to a choice of none, which its player can
   confirm.
6. **The projection counts the empty slots.**
   `SetupRegionProjection.emptySlots` is `MapState.emptySlots(region)`;
   the client draws that many frames after the region's sites and holds no
   slot count of its own. The frame is a `div.site-slot-empty` with
   `role="img"` and the label "Empty site slot, Provinces": the height and
   radius of a site, a dashed Line-mid border, no fill, nothing inside. It
   is not a `.site`, so no board option, pawn or selector that reads sites
   meets it.
7. **Game start with an Empire still refuses a relic deck too short for
   the atlas sites it places** ("not enough relics to fill site slots").
   Setup With No Empire deals while the deck lasts (ruling 5). The older
   refusal goes with slice 4, which turns that fill into `PlaceAtlasSite`.
8. **No Game Log line.** Erosion and Setup With No Empire act before the
   first event a player sees; slice 1's strip shows the faces, and the map
   shows the rest.
9. **Tests.** `WideHorizonsSetupSuite` (new) holds game start under Wide
   Horizons with an Empire; `NoEmpireSetupSuite` (new) holds Setup With No
   Empire, bullet by bullet; `GameStartRulesSuite` keeps the refusals and
   which Chronicles start which way; `ShortMapPlaySuite` (new) holds play
   on a short map; `EmptySlotChronicleSuite` (new) holds the Chronicle
   round trip. Two test-table steps are added: `Table.sitesInPlay(count)`
   cuts the map to its first sites, and `Table.reliquarySetAside` sets the
   flag.
10. **Docs.** The readings below go into the spec's slice 3 section, as
    slice 2's did, and slice 7 moves them to `docs/rules/ambiguities.md`.
    `CONTEXT.md` stays for slice 7, as the spec's Docs list says.
    `DESIGN.md` gets the empty frame with Task 6, the task that draws it.

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **"No Chancellor" means no Chancellor is seated.** Rulebook, Players:
   "**If there's no Chancellor,** a random Citizen becomes the Chancellor
   first. If there are no Citizens to become Chancellor, do not set up the
   Chancellor, skip the Empire setup [...] and use Setup With No Empire
   instead." Reading: there is no Chancellor when the Chancellor's lineage
   sits out, or when the Chronicle lists none (an imported Chronicle can).
   A seated Citizen then becomes the Chancellor, as R6 has it; with no
   Citizen seated either, the game is set up with no Empire. Only a
   Chronicle that lists no Chancellor, no Citizen and no Empire site starts
   a first game (Task 3, `EmpireSetupRules.resolve`).
2. **A former Empire with no site is refused.** Rulebook, Setup With No
   Empire: "Fill site slots going Hinterward [...] from the Recent Sites
   side of the Atlas box. Stop when you reach the Empire divider." With no
   site before the divider the map would be empty. Reading: a Chronicle
   that lists a Chancellor or a Citizen and no Empire site is malformed, as
   R28 already says of an Empire, and game start refuses it with the same
   words, "an Empire needs at least one site" (Task 3).
3. **The absent Chancellor's lineage becomes an Exile.** Rulebook:
   "All Citizens in the box change into Exiles." The text does not name
   the Chancellor's lineage. Reading: with no Empire nobody is Imperial, so
   the Chancellor's lineage in the box becomes an Exile with the Citizens.
   R6 already makes an absent Chancellor a Citizen in the box when a
   Citizen takes its place; and if it stayed the Chancellor, a player who
   became the Chancellor in play (CR p. 34) would leave the Chronicle with
   two (Task 3, `SetupMode.NoEmpire.exiled`).
4. **Erosion leaves a face that already lies eroded.** Rulebook: "Set
   Foundation I to Wide Horizons and Foundation V to Mob's Favor."
   Reading: a face that turns gets no alteration source, as the spec says.
   A Foundation I already on Wide Horizons is not set again and keeps its
   sources, so slice 7's Beacon cannot restore a face an active legacy
   holds (Task 3, `StartingFoundations.eroded`).
5. **A relic deck that runs out leaves slots empty.** Rulebook: "**Fill
   all empty relic slots** at sites by drawing relics from the relic
   deck." Reading: the former Empire's sites are dealt Cradlemost first,
   each site's open slots together, while the deck lasts; the slots left
   stay empty and the game starts. The Reliquary's relics are never dealt
   (owner ruling 3) (Task 3).
6. **With no Empire an Exile places their pawn at a site that holds
   bandits.** Rulebook, Place Pawn: "If you are an Exile, place your pawn
   on the Hintermost site ruled by the Empire **or** any site ruled by
   bandits." Reading: a site with no warbands, such as one of capacity 0,
   is ruled by nobody (R25) and is not offered. If no site on the map
   holds bandits, every site is offered, since Setup cannot go on without a
   pawn; the same fallback serves an Imperial player whose Empire holds no
   site with a purple warband, which 24 warbands and six seats cannot
   produce (Task 4, `SetupPawnSites.legal`).
7. **The set-aside Reliquary is empty to play, and an Exile's win
   scatters it.** Rulebook: "Discard all relics from the Reliquary. _They
   return in the Chronicle Phase._" Reading, beside owner ruling 3: a power
   that takes a Reliquary relic (Bewitch, Ancient Pact, Royal Ambitions)
   finds none and says "The Reliquary was empty." (R22), even once its
   user is the Chancellor (Task 4). In the Chronicle Phase the relics
   return as the Chronicle's own steps have it: Crown Chancellor discards
   the Reliquary when an Exile wins, so Scatter Relics deals them; they
   stay in the Reliquary for Stock Reliquary to add to only when a player
   who became the Chancellor in play wins (finding 6; Task 7 pins the
   Exile's win).
8. **A first game places its eight sites whatever Foundation I's face.**
   Rulebook, Setup for First Game: "Taking from the Recent Sites side of
   the Atlas box, place one site each in the eight site slots on the map."
   Rulebook, Wide Horizons: "**Do not fill the remaining empty site
   slots!**" Reading: a first game's Chronicle that stores Wide Horizons,
   which only a hand-built or imported one can, still places eight sites;
   none remains. The face stays in the campaign (Task 2).
9. **Shattered Portal counts sites, not slots.** Card: "roll [round-die]
   and place your pawn that many sites Hinterward. The bottom Hinterland
   site wraps to the top Cradle site." Reading: an empty slot is not a
   site and is passed over; the last site on the map wraps to the first;
   a roll that is a multiple of the number of sites places the pawn where
   it stands, and the secret stays paid. A power that names a region ("a
   site in your region", "in the Hinterland") reads the regions of the
   sites on the map: an empty slot holds nothing for it to count or
   affect (Task 5 pins the Portal; no code changes).

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/Cards.scala` | `Region.slots` |
| `src/main/scala/distantlands/model/World.scala` | `MapState`'s slots: `sitesIn`, `emptySlots`, `nextSlot`, `hintermost`, `MapState.slots`, `MapState.hinterward` |
| `src/main/scala/distantlands/model/DomainValidation.scala` | a region may be short, never overfull |
| `src/main/scala/distantlands/model/Setup.scala` | the plan's doc comments |
| `src/main/scala/distantlands/model/GameState.scala` | `CampaignState.reliquarySetAside` |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | builds the map with `MapState.hinterward` |
| `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala` | Foundation I's refusal goes; `eroded` |
| `src/main/scala/distantlands/gameplay/setup/EmpireSetupRules.scala` | rewritten: `SetupMode` and `resolve` |
| `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` | the atlas slots it fills, the atlas it keeps, the mode |
| `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala` | takes the mode; `withoutEmpire` |
| `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala` | pawn sites with no Empire |
| `src/main/scala/distantlands/gameplay/powers/ReliquaryDraws.scala` | a set-aside Reliquary gives nothing |
| `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala` | the first player with no Chancellor listed |
| `src/main/scala/distantlands/gameplay/walker/WalkerSearch.scala` | a choose-one with no option is refused |
| `src/main/scala/distantlands/gameplay/powers/action/ShatteredPortal.scala` | one comment |
| `src/main/scala/distantlands/application/GamePresentationProjector.scala` | each region's `emptySlots` |
| `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`, `WorldProjectionCodec.scala` | `SetupRegionProjection.emptySlots` |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`, `frontend/styles.css` | the empty frame |
| `src/test/scala/distantlands/model/WorldModelSuite.scala`, `DomainValidationSuite.scala` | the slots |
| `src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala` | new: Wide Horizons with an Empire |
| `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala` | new: Setup With No Empire |
| `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala` | the refusal table shrinks; which Chronicles start which way |
| `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala` | the first player |
| `src/test/scala/distantlands/server/TrustedGameProvisioningSuite.scala`, `TrustedSeatRoutesSuite.scala` | another refusal; the start with no Empire |
| `src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala` | Bewitch and the set-aside Reliquary |
| `src/test/scala/distantlands/testkit/Table.scala` | `reliquarySetAside`, `sitesInPlay` |
| `src/test/scala/distantlands/gameplay/walker/WalkerSearchSuite.scala`, `src/test/scala/distantlands/gameplay/ProcedureWalkerParkSuite.scala` | the search's refusal |
| `src/test/scala/distantlands/gameplay/ShortMapPlaySuite.scala` | new: play on a short map |
| `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala` | new: the projected map |
| `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala` | the codec |
| `frontend/src/test/scala/distantlands/frontend/EmptySiteSlotSuite.scala` | new: the frame |
| `src/test/scala/distantlands/gameplay/chronicle/EmptySlotChronicleSuite.scala` | new: the Chronicle round trip |
| `DESIGN.md`, `docs/architecture/game-setup.md`, `docs/rules/implementation-traceability.md`, `docs/rules/ambiguities.md`, `docs/ROADMAP.md`, the spec | docs |

---

### Task 1: The map's slots

**Files:**
- Modify: `src/main/scala/distantlands/model/Cards.scala`
- Modify: `src/main/scala/distantlands/model/World.scala`
- Modify: `src/main/scala/distantlands/model/DomainValidation.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Test: `src/test/scala/distantlands/model/WorldModelSuite.scala`
- Test: `src/test/scala/distantlands/model/DomainValidationSuite.scala`

**Interfaces:**
- Consumes: `MapState(cradle, provinces, hinterland, sites)`, `inPlay`,
  `hinterward`, `regionOf`; `Region.all`.
- Produces:
  - `Region.slots: Int`: 2 for the Cradle, 3 for the Provinces, 3 for the
    Hinterland.
  - `MapState.sitesIn(region: Region): Vector[SiteId]`,
    `MapState.emptySlots(region: Region): Int`,
    `MapState.nextSlot: Option[Region]` (the first region not yet full),
    `MapState.hintermost: Option[SiteId]` (the last site in `hinterward`).
  - `MapState.slots: Int` (8) and
    `MapState.hinterward(placed: Vector[(SiteId, SiteState)]): MapState`,
    which lays sites out in slots going Hinterward.
  - `DomainProblem.WrongRegionSize` is reported only for a region with more
    sites than slots.
  - Slice 3b's `PlaceAtlasSite` reads `nextSlot`; its Explore gate reads
    `hintermost`.

- [ ] **Step 1: Write the failing tests**

Add at the end of `src/test/scala/distantlands/model/WorldModelSuite.scala`:

```scala
  private val banditHeldSite = SiteState(
    SiteForces.Occupied(ForceKind.Bandit, 1), Vector.empty, Vector.empty,
    Tokens.empty)

  /** A map of the sites S1 to S`count`, placed going Hinterward. */
  private def mapOfSites(count: Int): MapState = MapState.hinterward(
    (1 to count).toVector.map(index => SiteId(s"S$index") -> banditHeldSite))

  test("sites fill the Cradle's 2 slots, then the Provinces' 3, then the " +
      "Hinterland's 3"):
    assertEquals(Region.all.map(_.slots), Vector(2, 3, 3))
    val full = mapOfSites(8)
    assertEquals(full.cradle.map(_.value), Vector("S1", "S2"))
    assertEquals(full.provinces.map(_.value), Vector("S3", "S4", "S5"))
    assertEquals(full.hinterland.map(_.value), Vector("S6", "S7", "S8"))
    assertEquals(full.sites.keySet, full.inPlay.toSet)
    val three = mapOfSites(3)
    assertEquals(three.cradle.map(_.value), Vector("S1", "S2"))
    assertEquals(three.provinces.map(_.value), Vector("S3"))
    assertEquals(three.hinterland, Vector.empty)
    assertEquals(Region.all.map(three.sitesIn), Vector(three.cradle,
      three.provinces, three.hinterland))

  test("the next slot is in the first region not yet full, and a full map " +
      "has none"):
    assertEquals(mapOfSites(1).nextSlot, Some(Region.Cradle))
    assertEquals(mapOfSites(2).nextSlot, Some(Region.Provinces))
    assertEquals(mapOfSites(4).nextSlot, Some(Region.Provinces))
    assertEquals(mapOfSites(5).nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(7).nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(8).nextSlot, None)
    assertEquals(Region.all.map(mapOfSites(4).emptySlots), Vector(0, 1, 3))
    assertEquals(Region.all.map(mapOfSites(8).emptySlots), Vector(0, 0, 0))

  test("the Hintermost site is the last one Hinterward, and a map with no " +
      "site has none"):
    assertEquals(mapOfSites(8).hintermost, Some(SiteId("S8")))
    assertEquals(mapOfSites(4).hintermost, Some(SiteId("S4")))
    assertEquals(mapOfSites(2).hintermost, Some(SiteId("S2")))
    assertEquals(mapOfSites(0).hintermost, None)
    assertEquals(mapOfSites(0).nextSlot, Some(Region.Cradle))
```

In `src/test/scala/distantlands/model/DomainValidationSuite.scala`:

Replace

```scala
  private def imperialUsurpers(game: OathGame): Vector[DomainProblem] =
```

with

```scala
  test("a region may hold fewer sites than its slots, never more"):
    // Patched with copy, not a Table step: no step lays out regions.
    def regionProblems(map: MapState): Vector[DomainProblem] =
      DomainValidation.validate(game.copy(current = game.current.copy(
        map = map))).collect {
        case problem: DomainProblem.WrongRegionSize => problem }
    val short = map.copy(provinces = map.provinces.take(1),
      hinterland = Vector.empty,
      sites = map.sites -- map.provinces.drop(1) -- map.hinterland)
    assertEquals(DomainValidation.validate(game.copy(current =
      game.current.copy(map = short))), Vector.empty)
    val extra = SiteId("S10")
    val crowded = map.copy(cradle = map.cradle :+ extra,
      sites = map.sites.updated(extra, map.sites(sites.head)))
    assertEquals(regionProblems(crowded),
      Vector(DomainProblem.WrongRegionSize(Region.Cradle, 2, 3)))

  private def imperialUsurpers(game: OathGame): Vector[DomainProblem] =
```

`game`, `map` and `sites` are `TestGameFixtures`': a hand-built game on
the eight sites S1 to S8, with no catalog.

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.model.WorldModelSuite distantlands.model.DomainValidationSuite"`
Expected: the test sources do not compile. The companion's
`MapState.hinterward`, `Region.slots`, `sitesIn`, `nextSlot`, `emptySlots`
and `hintermost` do not exist yet.

- [ ] **Step 3: Give the regions their slots**

In `src/main/scala/distantlands/model/Cards.scala`:

Replace

```scala
enum Region(val key: String):
  case Cradle extends Region("cradle")
  case Provinces extends Region("provinces")
  case Hinterland extends Region("hinterland")
object Region:
```

with

```scala
/** A region of the map. `slots` is the number of sites it has room for
  * (rulebook, "The Map at Large"). */
enum Region(val key: String, val slots: Int):
  case Cradle extends Region("cradle", 2)
  case Provinces extends Region("provinces", 3)
  case Hinterland extends Region("hinterland", 3)
object Region:
```

In `src/main/scala/distantlands/model/World.scala`:

Replace

```scala
final case class MapState(
```

with

```scala
/** The sites on the map, by region. A region may hold fewer sites than its
  * slots: under Wide Horizons and after Setup With No Empire the slots past
  * the last site placed are empty (Foundations design, slice 3). Slots fill
  * going Hinterward, so the empty ones follow every site. */
final case class MapState(
```

Then replace

```scala
  def inPlay: Vector[SiteId] = cradle ++ provinces ++ hinterland
```

with

```scala
  def inPlay: Vector[SiteId] = cradle ++ provinces ++ hinterland

  /** The sites in `region`, top slot first. */
  def sitesIn(region: Region): Vector[SiteId] = region match
    case Region.Cradle => cradle
    case Region.Provinces => provinces
    case Region.Hinterland => hinterland

  /** The slots of `region` that hold no site. */
  def emptySlots(region: Region): Int =
    math.max(0, region.slots - sitesIn(region).size)

  /** The region of the next empty slot going Hinterward: the first region
    * not yet full. None on a full map. */
  def nextSlot: Option[Region] = Region.all.find(emptySlots(_) > 0)

  /** The Hintermost site: the last site in Hinterward order. None on a map
    * with no site. */
  def hintermost: Option[SiteId] = hinterward.lastOption
```

Then replace

```scala
  def hinterward: Vector[SiteId] = inPlay
```

with

```scala
  def hinterward: Vector[SiteId] = inPlay

object MapState:
  /** The site slots on the map. */
  val slots: Int = Region.all.map(_.slots).sum

  /** The map with `placed` in its slots going Hinterward: top to bottom in
    * the Cradle, then the Provinces, then the Hinterland. Slots past the
    * last site stay empty. More sites than the map has slots overfill the
    * Hinterland, which `DomainValidation` reports. */
  def hinterward(placed: Vector[(SiteId, SiteState)]): MapState =
    val (cradle, outer) = placed.map(_._1).splitAt(Region.Cradle.slots)
    val (provinces, hinterland) = outer.splitAt(Region.Provinces.slots)
    MapState(cradle, provinces, hinterland, placed.toMap)
```

The companion's `hinterward` builds a map; the method of the same name on a
`MapState` reads one, in the same order.

- [ ] **Step 4: Let a region be short**

In `src/main/scala/distantlands/model/DomainValidation.scala`:

Replace

```scala
object DomainProblem:
  final case class WrongRegionSize(
```

with

```scala
object DomainProblem:
  /** `region` holds more sites than its `expected` slots. Fewer is no
    * problem: a slot may be empty. */
  final case class WrongRegionSize(
```

Then replace

```scala
    Vector(
      (Region.Cradle: Region, 2, map.cradle.size),
      (Region.Provinces: Region, 3, map.provinces.size),
      (Region.Hinterland: Region, 3, map.hinterland.size)
    ).foreach { case (region, expected, actual) =>
      if actual != expected then
        problems += WrongRegionSize(region, expected, actual)
    }
```

with

```scala
    Region.all.foreach { region =>
      val actual = map.sitesIn(region).size
      if actual > region.slots then
        problems += WrongRegionSize(region, region.slots, actual)
    }
```

- [ ] **Step 5: Build the map from the sites placed**

In `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
    val atlasSites = plan.atlasSites.map(stored => stored.site -> stored).toMap
    val map = MapState(
      plan.orderedSites.take(2),
      plan.orderedSites.slice(2, 5),
      plan.orderedSites.slice(5, 8),
      plan.orderedSites.map { siteId =>
        val drawn = relicsBySite.getOrElse(siteId, Vector.empty)
        siteId -> empireSites.get(siteId).map(empireSite(_, purple(siteId)))
          .orElse(atlasSites.get(siteId).map(atlasSite(_, drawn)))
          .getOrElse(firstGameSite(siteId, edifices.get(siteId), drawn))
      }.toMap)
    val usedRelics = relicsBySite.valuesIterator.flatten.toSet
```

with

```scala
    val atlasSites = plan.atlasSites.map(stored => stored.site -> stored).toMap
    val map = MapState.hinterward(plan.orderedSites.map { siteId =>
      val drawn = relicsBySite.getOrElse(siteId, Vector.empty)
      siteId -> empireSites.get(siteId).map(empireSite(_, purple(siteId)))
        .orElse(atlasSites.get(siteId).map(atlasSite(_, drawn)))
        .getOrElse(firstGameSite(siteId, edifices.get(siteId), drawn))
    })
    val usedRelics = relicsBySite.valuesIterator.flatten.toSet
```

`plan.orderedSites` still holds eight sites in every game that starts
today, so no game changes; Tasks 2 and 3 pass fewer.

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.model.* distantlands.gameplay.setup.*"`
Expected: PASS. The four new tests pass, and every setup suite passes
unchanged: the same eight sites lie in the same slots.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/Cards.scala src/main/scala/distantlands/model/World.scala src/main/scala/distantlands/model/DomainValidation.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/test/scala/distantlands/model/WorldModelSuite.scala src/test/scala/distantlands/model/DomainValidationSuite.scala
git commit -m "feat(map): let a region hold fewer sites than its slots" -m "Region carries its number of slots, 2, 3 and 3. MapState can say which
slots are empty, where the next one is and which site is Hintermost, and
MapState.hinterward lays placed sites out in slots, which the materializer
now uses. Domain validation accepts a short region and still reports an
overfull one." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 2: Wide Horizons at game start

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`

**Interfaces:**
- Consumes (Task 1): `MapState.slots`, `MapState.hinterward(placed)`,
  `sitesIn`, `nextSlot`, `hintermost`. From the base:
  `StartingFoundations.of(chronicle)`, `EmpireSetupRules.resolve(chronicle,
  orders): Either[OathViolation, Option[EmpireSetup]]`,
  `LaterGameFixture.chronicle(chancellor: LineageId, citizens:
  Vector[LineageId], world: Int)`, `LaterGameFixture.orders(chronicle,
  firstPlayer)`, `Situation.wake(driver, chronicle, orders)`.
- Produces:
  - Game start accepts an altered Foundation I. With an Empire it places
    the Empire's sites and no other: `plan.orderedSites` is
    `chronicle.world`'s sites and `plan.atlasSites` is empty.
  - `campaign.atlas` holds every atlas site game start did not place, in
    the Chronicle's order.
  - `GameStartRules.atlasSlots(empire: Option[EmpireSetup], faces)`,
    private; Task 3 changes its first parameter to the `SetupMode`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.model._
import distantlands.testkit.Situation
import distantlands.testkit.Unwrap.accepted

/** Game start under Wide Horizons, the altered Foundation I, with an Empire
  * (Foundations design, slice 3): the Empire's sites return and no slot
  * past them is filled. l1 is the seated Chancellor, l2 a Citizen and l3
  * an Exile; p1 goes first. Every test starts from a Chronicle: game start
  * is the subject. */
class WideHorizonsSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")
  private val wideHorizons = FoundationState(FoundationFace.Altered, Set.empty)

  /** A later game's Chronicle whose Empire holds `empireSites` sites, each
    * storing a denizen, with Foundation I on Wide Horizons. */
  private def wideHorizonsChronicle(empireSites: Int): Chronicle =
    LaterGameFixture.chronicle(LineageId("l1"), Vector(LineageId("l2")),
      empireSites).copy(
      foundations = Map(FoundationNumber.I -> wideHorizons))

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  test("under Wide Horizons the Empire's sites return as under Imperial " +
      "Maps and no slot past them is filled"):
    val record = wideHorizonsChronicle(empireSites = 3)
    val wide = started(record).game
    val imperialMaps = started(record.copy(foundations = Map.empty)).game
    val map = wide.current.map
    assertEquals(map.hinterward, record.world.map(_.site))
    assertEquals(Region.all.map(map.sitesIn(_).size), Vector(2, 1, 0))
    assertEquals(map.nextSlot, Some(Region.Provinces))
    assertEquals(map.hintermost, Some(record.world.last.site))
    assertEquals(map.sites.keySet, record.world.map(_.site).toSet)
    record.world.foreach(stored => assertEquals(map.sites(stored.site),
      imperialMaps.current.map.sites(stored.site)))
    assertEquals(imperialMaps.current.map.inPlay.size, 8)
    assertEquals(wide.campaign.foundations(FoundationNumber.I), wideHorizons)

  test("the atlas keeps every atlas site in order with its stored cards, " +
      "and no relic leaves the relic deck"):
    val record = wideHorizonsChronicle(empireSites = 3)
    val game = started(record).game
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, record.atlasBox.map(_.site))
    val atlasEdifices = record.atlasBox.flatMap(_.items)
      .collect { case id: EdificeId => id }
    assert(atlasEdifices.nonEmpty)
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.edifice
    }.flatten, atlasEdifices)
    assert(!game.current.commonCards.edificeDeck.exists(atlasEdifices.contains))
    // An Empire site fills no relic slot, and no other site is placed.
    assertEquals(game.current.commonCards.relicDeck, record.relicDeck)
    assert(game.cardIndex.isRight)

  test("each favor bank adds one per denizen on the map, and none for a " +
      "denizen left in the atlas"):
    val record = wideHorizonsChronicle(empireSites = 2)
    val ready = started(record)
    val onMap = record.world.flatMap(_.items).flatMap {
      case id: EdificeId => catalog.edifice(id).map(_.suit)
      case id: DenizenId => catalog.denizen(id).map(_.suit)
      case _ => None
    }
    assertEquals(onMap.size, 2)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> (3 + onMap.count(_ == suit))).toMap)

  test("an Empire of eight sites leaves no empty slot"):
    val map = started(wideHorizonsChronicle(empireSites = 8)).game.current.map
    assertEquals(map.inPlay.size, 8)
    assertEquals(map.nextSlot, None)

  test("on a one-site map Setup runs to the Chancellor's first turn with " +
      "every pawn at that site"):
    val record = wideHorizonsChronicle(empireSites = 1)
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p1))
    val current = woken.ready.game.current
    assertEquals(current.map.inPlay, record.world.map(_.site))
    assertEquals(current.players.map(_.pawnSite),
      Vector.fill(3)(Some(record.world.head.site)))
    assertEquals(current.turn.activePlayer, p1)
    assert(Set[Phase](Phase.Wake, Phase.Act)(current.turn.phase))

  test("a first game places its eight sites whatever Foundation I's face"):
    val firstGame = FirstGameSetupFixture.chronicle.copy(
      foundations = Map(FoundationNumber.I -> wideHorizons))
    val game = accepted(GameStartRules.evolve(catalog, firstGame,
      FirstGameSetupFixture.orders)).game
    assertEquals(game.current.map.inPlay,
      firstGame.atlasBox.take(8).map(_.site))
    assertEquals(game.current.map.nextSlot, None)
    assertEquals(game.campaign.foundations(FoundationNumber.I), wideHorizons)
```

`LaterGameFixture.chronicle(l1, Vector(l2), n)` takes the first `n` of the
fixture's eight sites as the Empire's, each storing one denizen from the
world deck, and leaves the rest in the atlas; two of those are Homelands
that store their edifice. The fixture seats p1, p2 and p3 on l1, l2 and l3.

The refusal table loses Foundation I's row:

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`:

Replace

```scala
        s"$face is not supported yet: it arrives with Foundations slice $slice")
    assertEquals(refusalFor(FoundationNumber.I),
      unsupported("Wide Horizons (altered Foundation I)", 3))
    assertEquals(refusalFor(FoundationNumber.V),
```

with

```scala
        s"$face is not supported yet: it arrives with Foundations slice $slice")
    assertEquals(refusalFor(FoundationNumber.V),
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.WideHorizonsSetupSuite distantlands.gameplay.setup.GameStartRulesSuite"`
Expected: the six tests of `WideHorizonsSetupSuite` FAIL, each with
`UnsupportedChronicle(Wide Horizons (altered Foundation I) is not
supported yet: it arrives with Foundations slice 3)`; five report
`expected a Right, got Left(...)`, and the one-site test fails at its
`Begin` step. `GameStartRulesSuite` passes: it only lost a row.

- [ ] **Step 3: Delete the refusal**

In `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

Replace

```scala
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.I -> 3,
    FoundationNumber.V -> 5,
```

with

```scala
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.V -> 5,
```

- [ ] **Step 4: Fill no slot past the Empire's sites**

Game start counted eight in two places. The atlas it keeps becomes the
atlas sites it did not place, and the number of slots it fills from the
atlas becomes a question of the Empire and Foundation I's face.

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
      // in the edifice deck. The materializer only knows the edifices at the
      // 8 sites in play.
      storedEdifices = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
```

with

```scala
      // in the edifice deck. The materializer only knows the edifices at the
      // sites in play.
      storedEdifices = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
```

Then replace

```scala
      ).toMap
      // The atlas past the sites in play keeps every stored card, the
      // inverse of `ChronicleExtraction.extract`'s `carried`.
      atlas = AtlasState(chronicle.atlasBox.drop(8 - chronicle.world.size)
        .map(stored => AtlasEntry.StoredSite(stored.site,
```

with

```scala
      ).toMap
      // The atlas sites not placed on the map stay in the atlas, in order,
      // with every stored card: the inverse of
      // `ChronicleExtraction.extract`'s `carried`.
      onMap = plan.orderedSites.toSet
      atlas = AtlasState(chronicle.atlasBox.filterNot(stored =>
          onMap(stored.site))
        .map(stored => AtlasEntry.StoredSite(stored.site,
```

Then replace

```scala
      : Either[OathViolation, FirstGameSetupPlan] =
    val slots = 8 - chronicle.world.size
    val faces = StartingFoundations.of(chronicle)
```

with

```scala
      : Either[OathViolation, FirstGameSetupPlan] =
    val faces = StartingFoundations.of(chronicle)
```

Then replace

```scala
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
```

with

```scala
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      slots = atlasSlots(empire, faces)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
```

Then replace

```scala
        else atlasSites, startingAdvisers(chronicle, orders), faces)
```

with

```scala
        else atlasSites, startingAdvisers(chronicle, orders), faces)

  /** The slots game start fills from the Recent end of the atlas. A first
    * game fills all eight. With an Empire, Imperial Maps fills the slots
    * past the Empire's sites (R27), and Wide Horizons fills none. */
  private def atlasSlots(empire: Option[EmpireSetup],
      faces: Map[FoundationNumber, FoundationState]): Int = empire match
    case None => MapState.slots
    case Some(empire)
        if faces(FoundationNumber.I).face == FoundationFace.Normal =>
      MapState.slots - empire.sites.size
    case Some(_) => 0
```

A first game still fills eight slots whatever the face (ruling 8). With an
Empire under Wide Horizons `atlasSites` is empty, so the check of the relic
deck against the open slots asks for nothing, the plan's `orderedSites` are
the Empire's sites alone, and the materializer (Task 1) leaves the other
slots empty.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.LaterGameSetupChoicesSuite distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/test/scala/distantlands/gameplay/setup/WideHorizonsSetupSuite.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala
git commit -m "feat(foundations): start a game under Wide Horizons" -m "With an Empire, an altered Foundation I returns the Empire's sites and
fills no slot past them: the atlas keeps every site game start did not
place, in order. A first game still places its eight sites. The refusal of
the face goes. The Explore action is slice 3b, so the map does not grow
yet." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 3: Setup With No Empire starts a game

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/EmpireSetupRules.scala` (rewritten)
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` (comments)
- Modify: `src/main/scala/distantlands/model/Setup.scala` (comments)
- Modify: `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`
- Test: `src/test/scala/distantlands/server/TrustedGameProvisioningSuite.scala`
- Test: `src/test/scala/distantlands/server/TrustedSeatRoutesSuite.scala`

**Interfaces:**
- Consumes (Tasks 1 and 2): `MapState.slots`, `MapState.hinterward`,
  `sitesIn`, `nextSlot`; `GameStartRules.atlasSlots`. From the base:
  `EmpireSetup(chancellor, imperial, sites, absentChancellor)`,
  `FirstGameSetupPlan.empire`, `atlasSites` and `foundations`,
  `CampaignState.boxRoles`, `StartingFoundations.of` and `refusal`,
  `EvolvedChronicleFixture.record` (the Chronicle a first game journals:
  l1 is its Chancellor).
- Produces:
  - `gameplay.setup.SetupMode`: `SetupMode.FirstGame`,
    `SetupMode.WithEmpire(empire: EmpireSetup)`,
    `SetupMode.NoEmpire(sites: Vector[StoredSite], exiled:
    Vector[LineageId])`.
  - `EmpireSetupRules.resolve(chronicle, orders): Either[OathViolation,
    SetupMode]`.
  - `StartingFoundations.eroded(faces): Map[FoundationNumber,
    FoundationState]`.
  - `LaterGameSetup.apply(catalog, ready, plan, mode: SetupMode)`, with a
    private `withoutEmpire(ready, exiled)` that Task 4 extends.
  - A later game with no Chancellor and no Citizen seated starts: the
    former Empire's sites as bandit sites, Foundations I and V eroded, the
    box's Imperial lineages Exiles, the table as in a first game.
  - `NoEmpireSetupSuite` with the helper `absentEmpireChronicle(empireSites)`
    and the fixtures `stockedFormerEmpire` and `formerEmpireWithReliquary`,
    which Task 4 extends.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._
import distantlands.testkit.{Answers, Situation}
import distantlands.testkit.Unwrap.accepted

/** Setup With No Empire (rulebook, "Setup With No Empire"; Foundations
  * design, slice 3). The Chronicle's Chancellor, l9, and its Citizen, l8,
  * sit out, so p1 to p3 play l1 to l3 as Exiles, and p2 goes first. Every
  * test starts from a Chronicle: game start is the subject. */
class NoEmpireSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private val (l8, l9) = (LineageId("l8"), LineageId("l9"))
  private val normal = FoundationState(FoundationFace.Normal, Set.empty)
  private val altered = FoundationState(FoundationFace.Altered, Set.empty)

  /** A Chronicle whose Empire nobody at the table belongs to: l9 is the
    * Chancellor and l8 a Citizen, and the Empire holds `empireSites` sites,
    * each storing a denizen. */
  private def absentEmpireChronicle(empireSites: Int = 2): Chronicle =
    LaterGameFixture.chronicle(l9, Vector(l8), empireSites)

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p2)))

  private def faceDown(relic: RelicId) =
    RelicState(relic, Orientation.FaceDown, Tokens.empty)

  test("Foundations erode: I turns to Wide Horizons and V to Mob's Favor, " +
      "with no sources, and the other faces stay"):
    val legacy = LegacyId(catalog.legacies.head.id.value)
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy))
    val campaign = started(absentEmpireChronicle().copy(foundations = Map(
      FoundationNumber.III -> sourced, FoundationNumber.V -> sourced)))
      .game.campaign
    assertEquals(campaign.foundations, FoundationNumber.all.map(_ -> normal)
      .toMap.updated(FoundationNumber.I, altered)
      .updated(FoundationNumber.III, sourced))

  test("a Foundation I already on Wide Horizons keeps its sources"):
    val legacy = LegacyId(catalog.legacies.head.id.value)
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy))
    assertEquals(started(absentEmpireChronicle().copy(foundations = Map(
      FoundationNumber.I -> sourced))).game.campaign
      .foundations(FoundationNumber.I), sourced)

  test("the former Empire's sites fill the first slots Hinterward, no " +
      "other site is placed, and the atlas keeps its sites in order"):
    val record = absentEmpireChronicle(empireSites = 3)
    val game = started(record).game
    val map = game.current.map
    assertEquals(map.hinterward, record.world.map(_.site))
    assertEquals(Region.all.map(map.sitesIn(_).size), Vector(2, 1, 0))
    assertEquals(map.sites.keySet, record.world.map(_.site).toSet)
    assertEquals(map.nextSlot, Some(Region.Provinces))
    assertEquals(game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, record.atlasBox.map(_.site))

  /** `absentEmpireChronicle(4)` with the fourth Empire site, a Homeland
    * that stores its edifice and a denizen, and the second, which has two
    * relic slots, also storing `storedRelic` from the relic deck. */
  private lazy val storedRelic = absentEmpireChronicle(4).relicDeck.head
  private lazy val stockedFormerEmpire: Chronicle =
    val record = absentEmpireChronicle(empireSites = 4)
    record.copy(relicDeck = record.relicDeck.tail,
      world = record.world.updated(1, record.world(1).copy(
        items = record.world(1).items :+ storedRelic)))

  test("a former Empire site has its edifice ruined, its denizens faceup, " +
      "its stored relics facedown, bandits up to its capacity and its " +
      "printed tokens"):
    val record = stockedFormerEmpire
    val map = started(record).game.current.map
    val homeland = record.world(3)
    val edifice = homeland.items.collectFirst { case id: EdificeId => id }.get
    val denizen = homeland.items.collectFirst { case id: DenizenId => id }.get
    assertEquals(map.sites(homeland.site).denizens, Vector(
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty)))
    record.world.foreach { stored =>
      val printed = catalog.site(stored.site).get
      val site = map.sites(stored.site)
      assert(printed.capacity > 0)
      assertEquals(site.forces,
        SiteForces.Occupied(ForceKind.Bandit, printed.capacity))
      assertEquals(site.tokens, printed.startingResources)
    }
    assertEquals(map.sites(record.world(1).site).relics.head,
      faceDown(storedRelic))

  test("every empty relic slot is filled facedown from the relic deck, " +
      "Cradlemost first, and a stored relic takes a slot"):
    val record = stockedFormerEmpire
    val current = started(record).game.current
    val slots = record.world.map(stored =>
      catalog.site(stored.site).get.relicSlots)
    // The second site keeps its stored relic in one of its two slots.
    assertEquals(slots, Vector(0, 2, 2, 0))
    assertEquals(record.world.map(stored =>
      current.map.sites(stored.site).relics), Vector(
      Vector.empty,
      Vector(faceDown(storedRelic), faceDown(record.relicDeck(0))),
      Vector(faceDown(record.relicDeck(1)), faceDown(record.relicDeck(2))),
      Vector.empty))
    assertEquals(current.commonCards.relicDeck, record.relicDeck.drop(3))

  test("a relic deck that runs out leaves the last slots empty"):
    val record = stockedFormerEmpire
    val short = record.copy(relicDeck = record.relicDeck.take(2))
    val current = started(short).game.current
    assertEquals(short.world.map(stored =>
      current.map.sites(stored.site).relics.size), Vector(0, 2, 1, 0))
    assertEquals(current.commonCards.relicDeck, Vector.empty)

  /** `absentEmpireChronicle()` with two relics from the relic deck in the
    * Reliquary. */
  private lazy val reliquaryRelics = absentEmpireChronicle().relicDeck.take(2)
  private lazy val formerEmpireWithReliquary: Chronicle =
    val record = absentEmpireChronicle()
    record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = reliquaryRelics)

  test("the Grand Scepter is set aside and no one is asked for a " +
      "Reliquary relic"):
    val record = formerEmpireWithReliquary
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    val woken = Situation.wake(Situation.rules(catalog)
      .withAnswers(recording), record, LaterGameFixture.orders(record, p2))
    assert(asked.nonEmpty)
    assert(!asked.contains(SetupProcedure.reliquaryDecisionId), asked)
    assertEquals(woken.ready.game.campaign.reliquary, reliquaryRelics)
    assert(!accepted(woken.ready.game.cardIndex).ids
      .contains(TheGrandScepterCard.id))

  test("every Citizen in the box becomes an Exile, and so does the absent " +
      "Chancellor's lineage"):
    val exile = LineageState(LineageId("l7"), None, Role.Exile, Vector.empty,
      Vector.empty)
    val record = absentEmpireChronicle()
    val ready = started(record.copy(lineages = record.lineages :+ exile))
    assertEquals(ready.game.campaign.boxRoles,
      Map(l9 -> Role.Exile, l8 -> Role.Exile))
    assertEquals(ready.game.campaign.lineages.values.map(_.role).toSet,
      Set[Role](Role.Exile))
    assertEquals(Imperials.players(ready), Vector.empty)
    assertEquals(Imperials.chancellor(ready), None)
    assert(!ready.banks.warbandSupply.contains(ForceKind.Imperial))

  test("the plan's first player begins, the Usurper Limiter is on, and " +
      "the title and both banners start as in a first game"):
    // The People's Favor goes to the Chancellor under this goal, and there
    // is none.
    val current = started(absentEmpireChronicle().copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople)).game.current
    assertEquals(current.turn, TurnState(p2, Phase.Setup, Set.empty))
    assertEquals(current.tracks, GameTracks(1, 0, usurperLimited = true,
      roundStarter = p2))
    assertEquals(current.title, OathkeeperState(None, TitleSide.Oathkeeper))
    assertEquals(current.banners, BannersState(
      PeoplesFavorState(PeoplesFavorFace.Mob, None, 1),
      DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1)))
    assertEquals(current.players.map(_.board),
      Vector.fill(3)(PlayerBoardState(1, 1, 0, 3, SupplyTrack.full)))

  test("Setup runs to the first player's turn on the former Empire's sites"):
    val record = absentEmpireChronicle()
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p2))
    val current = woken.ready.game.current
    assertEquals(current.players.map(_.player), Vector(p1, p2, p3))
    assertEquals(current.turn.activePlayer, p2)
    assert(Set[Phase](Phase.Wake, Phase.Act)(current.turn.phase))
    assert(current.players.forall(_.pawnSite.exists(
      record.world.map(_.site).contains)))
    assertEquals(current.players.map(_.advisers.size), Vector(1, 1, 1))
    assertEquals(DomainValidation.validate(woken.ready.game), Vector.empty)
```

The fixture's first four sites are Ancient City (capacity 3, no relic
slot), Broken Peaks and Buried Giant (capacity 1, two relic slots each) and
Deep Woods (a Homeland of capacity 3 that stores its edifice, no relic
slot). `LaterGameFixture.orders` deals the Chronicle's decks in their own
order, so the relic a slot gets is the next of `record.relicDeck`.

Which Chronicles start which way belongs to `GameStartRulesSuite`. The
Empire-less refusal becomes a start, a Chronicle that lists no Chancellor
gets its own test, and "a non-empty world is refused" is rewritten: its
Chronicle is a game with no Empire now, and what stops it is the site it
stores twice.

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`:

Replace

```scala
import distantlands.model._
import distantlands.testkit.Unwrap.{accepted, as, rejected}
```

with

```scala
import distantlands.model._
import distantlands.testkit.Unwrap.{accepted, rejected}
```

Then replace

```scala
  test("game start refuses an Empire-less later game until Wide Horizons"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire-less later game requires Wide Horizons")))
```

with

```scala
  test("with no Chancellor and no Citizen seated, a later game starts " +
      "with no Empire, whoever goes first"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
    Vector("p1", "p2", "p3").foreach { first =>
      val ready = accepted(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId(first))))
      assertEquals(Imperials.players(ready), Vector.empty)
      assertEquals(ready.game.current.turn.activePlayer, PlayerId(first))
    }

  test("a Chronicle that lists no Chancellor makes a seated Citizen the " +
      "Chancellor, who must go first, and with no Citizen seated starts " +
      "with no Empire"):
    val citizen = LineageState(LineageId("l2"), None, Role.Citizen,
      Vector.empty, Vector.empty)
    val record = LaterGameFixture.chronicle(LineageId("l9"), world = 2)
    val seatedCitizen = record.copy(lineages = Vector(citizen))
    val ready = accepted(GameStartRules.evolve(catalog, seatedCitizen,
      LaterGameFixture.orders(seatedCitizen, PlayerId("p2"))))
    assertEquals(Imperials.chancellor(ready), Some(PlayerId("p2")))
    assertEquals(ready.game.campaign.boxRoles, Map.empty)
    assertEquals(GameStartRules.evolve(catalog, seatedCitizen,
        LaterGameFixture.orders(seatedCitizen, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "the first player must be a seated Citizen")))
    val nobodyListed = record.copy(lineages = Vector.empty)
    val exiles = accepted(GameStartRules.evolve(catalog, nobodyListed,
      LaterGameFixture.orders(nobodyListed, PlayerId("p3"))))
    assertEquals(Imperials.players(exiles), Vector.empty)
    assertEquals(exiles.game.current.map.inPlay, record.world.map(_.site))

  test("game start refuses a former Empire with no site, as it does an " +
      "Empire's (R28)"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), world = 0)
    assertEquals(GameStartRules.evolve(catalog, record,
        LaterGameFixture.orders(record, PlayerId("p1"))),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire needs at least one site")))
```

Then replace

```scala
  test("a non-empty world is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val withWorld = chronicle.copy(world = chronicle.atlasBox.take(1))
    as[OathViolation.UnsupportedChronicle](
      rejected(GameStartRules.evolve(catalog, withWorld, orders)))
```

with

```scala
  test("a site stored in both the atlas and the Empire is refused"):
    // Starts from the Chronicle itself: the start rule is the subject.
    val stored = chronicle.atlasBox.head
    val twice = chronicle.copy(world = Vector(stored))
    assertEquals(rejected(GameStartRules.evolve(catalog, twice, orders)),
      OathViolation.UnsupportedChronicle(
        s"duplicate atlas site ${stored.site.value}"))
```

The plan factory picks the first player:

In `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture,
  GameStartRules}
import distantlands.model._
```

with

```scala
import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture,
  GameStartRules, LaterGameFixture}
import distantlands.model._
```

Add at the end of `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`:

```scala
  test("a Chronicle that lists no Chancellor still sends a random seated " +
      "Citizen first"):
    val citizens = Vector(lineage("l1", Role.Citizen),
      lineage("l3", Role.Citizen))
    val reversingCitizens = new distantlands.application.ChronicleRandomPort:
      def shuffle[A](values: Vector[A]): Vector[A] =
        if values.size == 2 then values.reverse else values
    assertEquals(seatedFirst(citizens, ChronicleShuffles.unshuffled),
      PlayerId("p1"))
    assertEquals(seatedFirst(citizens, reversingCitizens), PlayerId("p3"))

  test("with no Chancellor and no Citizen seated the first seat of the " +
      "shuffle goes first, and the game starts with no Empire"):
    // l9 is the Chancellor and l8 a Citizen; p1 to p3 play l1 to l3.
    val saved = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog,
      ChronicleShuffles.reversing).build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.resolvedConfig.participants,
      FirstGameSetupFixture.participants.reverse)
    assertEquals(plan.resolvedConfig.firstPlayer, PlayerId("p3"))
    val ready = accepted(GameStartRules.evolve(catalog, plan.chronicle,
      plan.orders))
    assertEquals(Imperials.players(ready), Vector.empty)
    assertEquals(ready.game.current.turn.activePlayer, PlayerId("p3"))
    assertEquals(ready.game.current.map.inPlay, saved.world.map(_.site))
```

The two server suites created a game from the evolved fixture to see game
start refuse it. That Chronicle now starts, so each takes an Empire with no
site for the refusal, and provisioning gains the start as its own test:

In `src/test/scala/distantlands/server/TrustedGameProvisioningSuite.scala`:

Replace

```scala
  test("an evolved Chronicle is refused with game start's reason, and " +
      "unreadable JSON and an overlong name with theirs, storing nothing"):
    withDatabase { (owner, connection) =>
      val service = provision(owner, () => codes.head)
      val evolved = ChronicleEntries().entry(EvolvedChronicleFixture.record)
      assertEquals(service.create(
        request.copy(chronicle = ChronicleRequest.Saved(evolved.json)),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused(
          "an Empire-less later game requires Wide Horizons")))
      service.create(request.copy(chronicle = ChronicleRequest.Saved("{")),
```

with

```scala
  test("an evolved Chronicle whose Chancellor and Citizens sit out starts " +
      "a game with no Empire"):
    withDatabase { (owner, _) =>
      val generated = codes.iterator
      val evolved = ChronicleEntries().entry(EvolvedChronicleFixture.record)
      val created = accepted(provision(owner, () => generated.next())
        .create(request.copy(chronicle = ChronicleRequest.Saved(evolved.json)),
          "http://127.0.0.1:8080"))
      assertEquals(created.chronicle, evolved)
    }

  test("a Chronicle game start refuses is refused with its reason, and " +
      "unreadable JSON and an overlong name with theirs, storing nothing"):
    withDatabase { (owner, connection) =>
      val service = provision(owner, () => codes.head)
      // The crowned winner's sites go back to the atlas: an Empire with
      // no site, which game start refuses (R28).
      val record = EvolvedChronicleFixture.record
      val evolved = ChronicleEntries().entry(record.copy(
        atlasBox = record.world ++ record.atlasBox, world = Vector.empty))
      assertEquals(service.create(
        request.copy(chronicle = ChronicleRequest.Saved(evolved.json)),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused(
          "an Empire needs at least one site")))
      service.create(request.copy(chronicle = ChronicleRequest.Saved("{")),
```

In `src/test/scala/distantlands/server/TrustedSeatRoutesSuite.scala`:

Replace

```scala
  test("creation refuses an evolved Chronicle with game start's reason"):
    withServer() { (server, _) =>
      val body = TrustedGameCreateRequestCodec.encode(TrustedGameCreateRequest(
        Vector(BootstrapParticipantRequest("p1", PlayerColor.Red),
          BootstrapParticipantRequest("p2", PlayerColor.Blue)),
        ChronicleRequest.Saved(conversionEntries.entry(
          EvolvedChronicleFixture.record).json)))
      val response = send(server, "/games", Some(body))
```

with

```scala
  test("creation refuses a Chronicle game start refuses, with its reason"):
    withServer() { (server, _) =>
      // The crowned winner's sites go back to the atlas: an Empire with
      // no site, which game start refuses (R28).
      val record = EvolvedChronicleFixture.record
      val body = TrustedGameCreateRequestCodec.encode(TrustedGameCreateRequest(
        Vector(BootstrapParticipantRequest("p1", PlayerColor.Red),
          BootstrapParticipantRequest("p2", PlayerColor.Blue)),
        ChronicleRequest.Saved(conversionEntries.entry(record.copy(
          atlasBox = record.world ++ record.atlasBox,
          world = Vector.empty)).json)))
      val response = send(server, "/games", Some(body))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.NoEmpireSetupSuite distantlands.gameplay.setup.GameStartRulesSuite distantlands.application.GeneratedFirstGamePlanFactorySuite distantlands.server.TrustedGameProvisioningSuite distantlands.server.TrustedSeatRoutesSuite"`
Expected: 16 tests FAIL, and the rest pass. All ten of
`NoEmpireSetupSuite` and the two new tests of `GameStartRulesSuite` get
`UnsupportedChronicle(an Empire-less later game requires Wide Horizons)`
where they expect a game. "a site stored in both the atlas and the Empire
is refused" gets that same refusal where it expects `duplicate atlas
site`. In `GeneratedFirstGamePlanFactorySuite`, "a Chronicle that lists no
Chancellor still sends a random seated Citizen first" obtains p1 where it
expects p3, and the no-Empire test is refused at game start.
`TrustedGameProvisioningSuite`'s "an evolved Chronicle whose Chancellor and
Citizens sit out starts a game with no Empire" is refused. The two
rewritten refusal tests pass already: an Empire with no site was refused
before this task too.

- [ ] **Step 3: Say how a game starts**

Replace the whole of `src/main/scala/distantlands/gameplay/setup/EmpireSetupRules.scala` with:

```scala
package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.OathViolation.UnsupportedChronicle

/** How a Chronicle's game sets up its map and its roles (rulebook, "Setup
  * for First Game" and "Setup for Later Games": Players, Empire, Setup With
  * No Empire). */
sealed trait SetupMode extends Product with Serializable
object SetupMode:
  /** A first game: the Chronicle lists no Chancellor and no Citizen, and
    * the Empire holds no site. */
  case object FirstGame extends SetupMode

  /** A later game with a Chancellor at the table. */
  final case class WithEmpire(empire: EmpireSetup) extends SetupMode

  /** Setup With No Empire: a later game where no Chancellor and no Citizen
    * is seated. `sites` are the former Empire's, Cradlemost first, and
    * `exiled` the Imperial lineages in the box, which become Exiles. */
  final case class NoEmpire(sites: Vector[StoredSite],
      exiled: Vector[LineageId]) extends SetupMode

/** Resolves how a game starts from its Chronicle and seats. */
object EmpireSetupRules:
  /** The seated Chancellor leads the Empire. With none seated, because the
    * Chancellor's lineage sits out or the Chronicle lists none, a seated
    * Citizen stands in, and must be the first player (R6); with no Citizen
    * seated either, the game is set up with no Empire. A later game's
    * Empire, kept or former, holds 1 to 8 sites (R28). */
  def resolve(chronicle: Chronicle, orders: SetupOrders)
      : Either[OathViolation, SetupMode] =
    val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id)
    val chancellors = lineages(Role.Chancellor)
    val citizens = lineages(Role.Citizen)
    if chancellors.isEmpty && citizens.isEmpty && chronicle.world.isEmpty then
      Right(SetupMode.FirstGame)
    else
      val seatedCitizens = citizens.flatMap(seated.get)
      def empire(chancellor: PlayerId) = SetupMode.WithEmpire(EmpireSetup(
        chancellor, seatedCitizens.toSet + chancellor, chronicle.world,
        chancellors.headOption.filterNot(seated.contains)))
      for
        _ <- Either.cond(chronicle.world.nonEmpty, (),
          UnsupportedChronicle("an Empire needs at least one site"))
        _ <- Either.cond(chronicle.world.size <= MapState.slots, (),
          UnsupportedChronicle("an Empire cannot hold more than 8 sites"))
        mode <- chancellors.headOption.flatMap(seated.get) match
          case Some(player) => Either.cond(orders.firstPlayer == player,
            empire(player), UnsupportedChronicle(
              "the first player must be the Chancellor"))
          case None if seatedCitizens.nonEmpty =>
            Either.cond(seatedCitizens.contains(orders.firstPlayer),
              empire(orders.firstPlayer), UnsupportedChronicle(
                "the first player must be a seated Citizen"))
          case None => Right(SetupMode.NoEmpire(chronicle.world,
            chancellors ++ citizens))
      yield mode
```

The messages are the old ones. "an Empire-less later game requires Wide
Horizons" is gone: that Chronicle is `SetupMode.NoEmpire`, or, with a
seated Citizen and no Chancellor listed, an Empire whose Citizen stands in
(rulings 1 to 3).

- [ ] **Step 4: Erode the foundations**

In `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

Replace

```scala
        FoundationState(FoundationFace.Normal, Set.empty))).toMap
```

with

```scala
        FoundationState(FoundationFace.Normal, Set.empty))).toMap

  /** `faces` after Setup With No Empire's "Foundations Erode": Foundation I
    * lies on Wide Horizons and Foundation V on Mob's Favor. A face that
    * turns has no alteration source; a face that already lies there is
    * kept, sources and all. */
  def eroded(faces: Map[FoundationNumber, FoundationState])
      : Map[FoundationNumber, FoundationState] =
    Vector(FoundationNumber.I -> FoundationFace.Altered,
      FoundationNumber.V -> FoundationFace.Normal).foldLeft(faces) {
      case (turned, (number, face)) =>
        if turned.get(number).exists(_.face == face) then turned
        else turned.updated(number, FoundationState(face, Set.empty))
    }
```

- [ ] **Step 5: Read the mode at game start**

`buildPlan` resolves the mode before it reads the faces, since erosion
comes first, and returns the mode with the plan.

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
 * Validation here is only what Setup itself needs: known and unique ids
 * across every Chronicle field (`ChronicleAccounting`), a later game's
 * Empire (`EmpireSetupRules`), foundation faces that work
 * (`StartingFoundations`), enough cards to deal, and, in a first game,
 * one edifice per Homeland in play (Empire ruling R26). The old
 * plan's suit-count and Vision-packet audit is the generator's own job now
```

with

```scala
 * Validation here is only what Setup itself needs: known and unique ids
 * across every Chronicle field (`ChronicleAccounting`), how a later game
 * starts, with an Empire or with none (`EmpireSetupRules`), foundation
 * faces that work (`StartingFoundations`), enough cards to deal, and, in a
 * first game, one edifice per Homeland in play (Empire ruling R26). The old
 * plan's suit-count and Vision-packet audit is the generator's own job now
```

Then replace

```scala
    for
      plan <- buildPlan(catalog, chronicle, orders, deal)
      material = new FirstGameSetupMaterializer(catalog)
```

with

```scala
    for
      planned <- buildPlan(catalog, chronicle, orders, deal)
      (plan, mode) = planned
      material = new FirstGameSetupMaterializer(catalog)
```

Then replace

```scala
        plan.participants.map(p => p.playerId -> p.color).toMap,
        orders.firstPlayer, material.favorBanks), plan)
      problems = DomainValidation.validate(ready.game)
      _ <- Either.cond(problems.isEmpty, (), InvalidAggregate(problems))
    yield ready

  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders, deal: SetupDeal)
      : Either[OathViolation, FirstGameSetupPlan] =
    val faces = StartingFoundations.of(chronicle)
    for
```

with

```scala
        plan.participants.map(p => p.playerId -> p.color).toMap,
        orders.firstPlayer, material.favorBanks), plan, mode)
      problems = DomainValidation.validate(ready.game)
      _ <- Either.cond(problems.isEmpty, (), InvalidAggregate(problems))
    yield ready

  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders, deal: SetupDeal)
      : Either[OathViolation, (FirstGameSetupPlan, SetupMode)] =
    for
```

Then replace

```scala
        UnsupportedChronicle("legacies require Legacies"))
      _ <- StartingFoundations.refusal(faces)
        .map(UnsupportedChronicle(_)).toLeft(())
      _ <- validateParticipants(orders)
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      slots = atlasSlots(empire, faces)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
        UnsupportedChronicle(
          s"at least $slots atlas sites are required, got ${chronicle.atlasBox.size}"))
      atlasSites = chronicle.atlasBox.take(slots)
      // A later game keeps every card stored at its sites (R27); a first
      // game's sites hold only their Homeland edifices (R26).
      _ <- Either.cond(empire.nonEmpty ||
        chronicle.atlasBox.forall(_.items.forall(_.isInstanceOf[EdificeId])), (),
```

with

```scala
        UnsupportedChronicle("legacies require Legacies"))
      _ <- validateParticipants(orders)
      mode <- EmpireSetupRules.resolve(chronicle, orders)
      // Setup With No Empire erodes the faces before the foundations are
      // resolved, so an eroded face is never refused.
      faces = mode match
        case _: SetupMode.NoEmpire =>
          StartingFoundations.eroded(StartingFoundations.of(chronicle))
        case _ => StartingFoundations.of(chronicle)
      _ <- StartingFoundations.refusal(faces)
        .map(UnsupportedChronicle(_)).toLeft(())
      slots = atlasSlots(mode, faces)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
        UnsupportedChronicle(
          s"at least $slots atlas sites are required, got ${chronicle.atlasBox.size}"))
      atlasSites = chronicle.atlasBox.take(slots)
      // A later game keeps every card stored at its sites (R27); a first
      // game's sites hold only their Homeland edifices (R26).
      _ <- Either.cond(mode != SetupMode.FirstGame ||
        chronicle.atlasBox.forall(_.items.forall(_.isInstanceOf[EdificeId])), (),
```

Then replace

```scala
        UnsupportedChronicle("not enough world-deck cards to deal"))
      homelands <- if empire.isEmpty then homelandEdifices(catalog, atlasSites)
        else Right(Vector.empty)
```

with

```scala
        UnsupportedChronicle("not enough world-deck cards to deal"))
      homelands <- if mode == SetupMode.FirstGame then
          homelandEdifices(catalog, atlasSites)
        else Right(Vector.empty)
```

Then replace

```scala
      orders.worldDeckOrder, orders.relicOrder, homelands,
      chronicle.oathkeeperGoal, empire, if empire.isEmpty then Vector.empty
        else atlasSites, startingAdvisers(chronicle, orders), faces)

  /** The slots game start fills from the Recent end of the atlas. A first
    * game fills all eight. With an Empire, Imperial Maps fills the slots
    * past the Empire's sites (R27), and Wide Horizons fills none. */
  private def atlasSlots(empire: Option[EmpireSetup],
      faces: Map[FoundationNumber, FoundationState]): Int = empire match
    case None => MapState.slots
    case Some(empire)
        if faces(FoundationNumber.I).face == FoundationFace.Normal =>
      MapState.slots - empire.sites.size
    case Some(_) => 0
```

with

```scala
      orders.worldDeckOrder, orders.relicOrder, homelands,
      chronicle.oathkeeperGoal, mode match
        case SetupMode.WithEmpire(empire) => Some(empire)
        case _ => None,
      mode match
        case SetupMode.FirstGame => Vector.empty
        case _: SetupMode.WithEmpire => atlasSites
        case SetupMode.NoEmpire(sites, _) => sites,
      startingAdvisers(chronicle, orders), faces) -> mode

  /** The slots game start fills from the Recent end of the atlas. A first
    * game fills all eight. With an Empire, Imperial Maps fills the slots
    * past the Empire's sites (R27), and Wide Horizons fills none. With no
    * Empire the former Empire's sites are all that is placed. */
  private def atlasSlots(mode: SetupMode,
      faces: Map[FoundationNumber, FoundationState]): Int = mode match
    case SetupMode.FirstGame => MapState.slots
    case SetupMode.WithEmpire(empire)
        if faces(FoundationNumber.I).face == FoundationFace.Normal =>
      MapState.slots - empire.sites.size
    case _ => 0
```

With no Empire `atlasSlots` is 0: nothing is taken from the atlas, the
plan's `orderedSites` are `chronicle.world`'s sites, and its `atlasSites`
are those same sites with their stored cards. The materializer already
sets an `atlasSites` entry up as the rulebook asks here: bandits up to its
capacity, its edifice ruined, its denizens faceup, its stored relics
facedown, its printed favor and secrets, and its open relic slots dealt
from the relic deck, Cradlemost first; `plan.relicOrder.slice` simply
returns fewer relics once the deck runs out (ruling 5). The Reliquary's
relics are not in `relicOrder`, so no slot can draw them.

- [ ] **Step 6: Set up the box**

In `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`:

Replace

```scala
  * later game, the Empire's roles, purple warbands, Grand Scepter, title and
  * banners. The state is built directly, as game start builds the whole
  * game; nothing here is journaled. */
object LaterGameSetup:
  /** Each seated lineage's starting advisers move to its player; with an
    * Empire, its table facts follow. */
  def apply(catalog: ExecutableCatalog, ready: ReadyGame,
      plan: FirstGameSetupPlan): ReadyGame =
    val advised = startingAdvisers(ready, plan)
    plan.empire.fold(advised)(empire =>
      shortSitesTakeBandits(catalog, imperial(advised, plan, empire)))
```

with

```scala
  * later game, the Empire's roles, purple warbands, Grand Scepter, title and
  * banners, or, with no Empire, the box's Exiles. The state is built
  * directly, as game start builds the whole game; nothing here is
  * journaled. */
object LaterGameSetup:
  /** Each seated lineage's starting advisers move to its player; with an
    * Empire, its table facts follow, and with none, the box's. */
  def apply(catalog: ExecutableCatalog, ready: ReadyGame,
      plan: FirstGameSetupPlan, mode: SetupMode): ReadyGame =
    val advised = startingAdvisers(ready, plan)
    mode match
      case SetupMode.FirstGame => advised
      case SetupMode.WithEmpire(empire) =>
        shortSitesTakeBandits(catalog, imperial(advised, plan, empire))
      case SetupMode.NoEmpire(_, exiled) => withoutEmpire(advised, exiled)

  /** Setup With No Empire, off the map: every Imperial lineage in the box
    * becomes an Exile. The title, the banners, the Usurper Limiter and the
    * first player stay as the materializer set them, as in a first game;
    * the Grand Scepter stays in the box. */
  private def withoutEmpire(ready: ReadyGame, exiled: Vector[LineageId])
      : ReadyGame =
    ready.updateCampaign(campaign => campaign.copy(
      boxRoles = campaign.boxRoles ++ exiled.map(_ -> Role.Exile)))
```

Nothing else is needed off the map. The materializer starts every game
with an unheld Oathkeeper title, both banners unheld on their Mob and
Wandering Flame faces, the Usurper Limiter on and the plan's first player
beginning the round; only `imperial`, which does not run here, changes
them. No purple supply is created, and the Grand Scepter is in no zone, as
in a first game.

- [ ] **Step 7: Pick the first player**

In `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`:

Replace

```scala
 * A saved Chronicle is used as it is. Either way the seating order is
 * shuffled and the requested `firstPlayer` is ignored. With an Empire, the
 * seated Chancellor goes first, or a random seated Citizen when the
 * Chancellor's lineage sits out (Empire ruling R6); otherwise the first
 * seat does. Both decks are shuffled into the deal by
 * `policy`, since a Chronicle's own order is shown to players. The
```

with

```scala
 * A saved Chronicle is used as it is. Either way the seating order is
 * shuffled and the requested `firstPlayer` is ignored. The seated
 * Chancellor goes first, or a random seated Citizen when no Chancellor is
 * seated (Empire ruling R6); otherwise, in a first game and in a later game
 * with no Empire, the first seat does, which the shuffle makes a random
 * player. Both decks are shuffled into the deal by
 * `policy`, since a Chronicle's own order is shown to players. The
```

Then replace

```scala
    // A random seated Citizen stands in for an absent Chancellor (R6).
    val first = if lineages(Role.Chancellor).isEmpty then seats.headOption
      else chancellor.orElse(random.shuffle(citizens).headOption)
        .orElse(seats.headOption)
    first.fold(config)(p => FirstGameBootstrapConfig(seats, p.playerId))
```

with

```scala
    // A random seated Citizen stands in for an absent Chancellor (R6).
    val first = chancellor.orElse(
      if citizens.isEmpty then seats.headOption
      else random.shuffle(citizens).headOption)
    first.fold(config)(p => FirstGameBootstrapConfig(seats, p.playerId))
```

- [ ] **Step 8: Say so in the plan's and the materializer's comments**

In `src/main/scala/distantlands/model/Setup.scala`:

Replace

```scala
    firstPlayer: PlayerId,
    orderedSites: Vector[SiteId],
    denizenOrder: Vector[DenizenId],
    worldDeckOrder: Vector[WorldCardId],
    relicOrder: Vector[RelicId],
    homelandEdifices: Vector[(SiteId, EdificeId)],
    oathkeeperGoal: OathkeeperGoal,
    /** The Empire a later game starts with; a first game has none. */
    empire: Option[EmpireSetup] = None,
    /** The atlas sites that fill the slots the Empire leaves, with their
      * stored cards, Recent end first. Empty for a first game, whose sites
      * are all in `orderedSites`. */
    atlasSites: Vector[StoredSite] = Vector.empty,
```

with

```scala
    firstPlayer: PlayerId,
    /** The sites game start places, in slot order going Hinterward: eight
      * on a full map, fewer when slots stay empty. */
    orderedSites: Vector[SiteId],
    denizenOrder: Vector[DenizenId],
    worldDeckOrder: Vector[WorldCardId],
    relicOrder: Vector[RelicId],
    homelandEdifices: Vector[(SiteId, EdificeId)],
    oathkeeperGoal: OathkeeperGoal,
    /** The Empire a later game starts with; a first game has none, nor has
      * a game set up with no Empire. */
    empire: Option[EmpireSetup] = None,
    /** The sites placed as bandit sites with their stored cards, their
      * edifices ruined: the atlas sites that fill the slots the Empire
      * leaves, Recent end first, or, with no Empire, the former Empire's
      * sites. Empty for a first game, whose sites are all in
      * `orderedSites`, and under Wide Horizons with an Empire, which fills
      * no slot. */
    atlasSites: Vector[StoredSite] = Vector.empty,
```

In `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
    * slots an atlas site's stored relics leave open, and none at the
    * Empire's sites (Empire ruling R27). */
  private def assignRelics(plan: FirstGameSetupPlan): Map[SiteId, Vector[RelicId]] =
```

with

```scala
    * slots an atlas site's stored relics leave open, and none at the
    * Empire's sites (Empire ruling R27). Slots are dealt Cradlemost first,
    * and a relic deck that runs out leaves the last ones empty. */
  private def assignRelics(plan: FirstGameSetupPlan): Map[SiteId, Vector[RelicId]] =
```

Then replace

```scala
  /** An atlas site in a later game keeps its stored cards, its edifice
    * ruined, and fills its open relic slots (Empire ruling R27). */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
```

with

```scala
  /** An atlas site in a later game keeps its stored cards, its edifice
    * ruined, and fills its open relic slots (Empire ruling R27). A former
    * Empire's site is set up the same way with no Empire. */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
```

- [ ] **Step 9: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.GeneratedFirstGamePlanFactorySuite distantlands.application.LaterGameSetupChoicesSuite distantlands.server.TrustedGameProvisioningSuite distantlands.server.TrustedSeatRoutesSuite distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/EmpireSetupRules.scala src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/main/scala/distantlands/model/Setup.scala src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala src/test/scala/distantlands/server/TrustedGameProvisioningSuite.scala src/test/scala/distantlands/server/TrustedSeatRoutesSuite.scala
git commit -m "feat(foundations): set up a later game with no Empire" -m "A later game with no Chancellor and no Citizen at the table is no longer
refused. Foundations I and V erode, the former Empire's sites return as
bandit sites with ruined edifices and their open relic slots filled while
the relic deck lasts, the box's Imperial lineages become Exiles, and the
title, the banners, the Usurper Limiter and the first player start as in a
first game. EmpireSetupRules now returns a SetupMode, and a Chronicle that
lists no Chancellor lets a seated Citizen take the crown." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 4: The Reliquary set aside, and pawn sites with no Empire

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/ReliquaryDraws.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala`
- Test: `src/test/scala/distantlands/testkit/Table.scala`

**Interfaces:**
- Consumes (Task 3): `LaterGameSetup.withoutEmpire`, `NoEmpireSetupSuite`
  and its fixtures. From the base: `ReliquaryDraws.steps`,
  `SetupPawnSites.legal(ready, player)`, `CampaignState.baseline`,
  `Table.allowingCitizenshipPowers`, `Table.reliquaryFromDeck(n)`,
  `WhenPlayedHarness`.
- Produces:
  - `CampaignState.reliquarySetAside: Boolean` (last field, default
    `false`), `true` after Setup With No Empire.
  - `ReliquaryDraws.steps` takes nothing from a set-aside Reliquary and
    writes "The Reliquary was empty."
  - `SetupPawnSites.legal`: in a later game with no Empire, the sites that
    hold bandits; every site when none qualifies.
  - `Table.reliquarySetAside: Table`.

- [ ] **Step 1: Write the failing tests**

In `src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala`:

Replace

```scala
  test("the Grand Scepter is set aside and no one is asked for a " +
```

with

```scala
  test("the Reliquary's relics stay set aside: no site, deck or player " +
      "holds one, and the card index holds every card once"):
    val record = formerEmpireWithReliquary
    val ready = started(record)
    val game = ready.game
    assertEquals(game.campaign.reliquary, reliquaryRelics)
    assert(game.campaign.reliquarySetAside)
    val inPlay = game.current.map.sites.values.flatMap(_.relics.map(_.id)) ++
      game.current.commonCards.relicDeck ++
      game.current.players.flatMap(_.relics.map(_.id))
    assert(!inPlay.exists(reliquaryRelics.contains))
    // Every relic slot on the map is filled, from the relic deck alone.
    assertEquals(game.current.map.sites.values.map(_.relics.size).sum,
      record.world.map(stored => catalog.site(stored.site).get.relicSlots).sum)
    val index = accepted(game.cardIndex)
    reliquaryRelics.foreach(relic => assertEquals(
      index.get(relic).map(_.location.container),
      Some(CardContainer.Reliquary)))
    assertEquals(DomainValidation.validate(game), Vector.empty)

  test("a game with an Empire does not set the Reliquary aside"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    assert(!accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1))).game.campaign.reliquarySetAside)
    assert(!FirstGameSetupFixture.freshReady.game.campaign.reliquarySetAside)

  test("the Grand Scepter is set aside and no one is asked for a " +
```

Then replace

```scala
  test("Setup runs to the first player's turn on the former Empire's sites"):
```

with

```scala
  test("an Exile may place their pawn at any site the bandits rule, and " +
      "at no site without warbands while one has bandits"):
    val ready = started(absentEmpireChronicle(empireSites = 3))
    val sites = ready.game.current.map.hinterward
    Vector(p1, p2, p3).foreach(player =>
      assertEquals(SetupPawnSites.legal(ready, player), sites))
    /** `ready` with `emptied` holding no warbands, as a capacity-0 site
      * holds none. */
    def withoutWarbandsAt(emptied: Set[SiteId]): ReadyGame =
      ready.updateCurrent(current => current.copy(map = current.map.copy(
        sites = current.map.sites.map((site, state) => site ->
          (if emptied(site) then state.copy(forces = SiteForces.Empty)
          else state)))))
    assertEquals(SetupPawnSites.legal(withoutWarbandsAt(Set(sites(1))), p1),
      Vector(sites(0), sites(2)))
    // With no bandits anywhere, any site: Setup must not stall.
    assertEquals(SetupPawnSites.legal(withoutWarbandsAt(sites.toSet), p1),
      sites)

  test("Setup runs to the first player's turn on the former Empire's sites"):
```

In `src/test/scala/distantlands/testkit/Table.scala`:

Replace

```scala
  /** A walker procedure for `action`, parked at step `path` of its tree
```

with

```scala
  /** The Reliquary set aside for the game, as Setup With No Empire leaves
    * it: its relics stay where they are. */
  def reliquarySetAside: Table =
    update(_.updateCampaign(_.copy(reliquarySetAside = true)))

  /** A walker procedure for `action`, parked at step `path` of its tree
```

In `src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala`:

Replace

```scala
  test("Ancient Pact asks only the holder of the Darkest Secret"):
```

with

```scala
  test("Bewitch: a Reliquary set aside gives no relic, and the player " +
      "still becomes the Chancellor"):
    // Setup With No Empire set the Reliquary aside, so no one is Imperial.
    val start = p1AtHomeSite.turn(p1, Phase.Act).allowingCitizenshipPowers
      .secrets(p1, 3).secrets(p2, 0).secrets(p3, 0).reliquaryFromDeck(2)
      .reliquarySetAside.ready
    val stocked = start.game.campaign.reliquary
    assertEquals(stocked.size, 2)
    val walked = played(start, bewitch, chooseOne(bewitch.become))
    assertEquals(Imperials.chancellor(walked.end), Some(p1))
    assertEquals(walked.end.game.campaign.reliquary, stocked)
    assert(!Look(walked.end).player(p1).relics.exists(relic =>
      stocked.contains(relic.id)))
    assertEquals(said(bewitch, walked.events), Vector(
      line("reliquary-empty", "The Reliquary was empty."),
      line("used.chancellor", "Became the Chancellor.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("Ancient Pact asks only the holder of the Darkest Secret"):
```

- [ ] **Step 2: Add the flag, so the tests compile**

In `src/main/scala/distantlands/model/GameState.scala`:

Replace

```scala
    // baseline holds (Crown Chancellor: "even in the box", CR p. 36).
    boxRoles: Map[LineageId, Role] = Map.empty
)
```

with

```scala
    // baseline holds (Crown Chancellor: "even in the box", CR p. 36).
    boxRoles: Map[LineageId, Role] = Map.empty,
    // Setup With No Empire discards the Reliquary until the Chronicle
    // Phase: its relics stay in `reliquary`, and no one can take one in
    // play (Foundations ruling 3).
    reliquarySetAside: Boolean = false
)
```

- [ ] **Step 3: Run the tests to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.NoEmpireSetupSuite distantlands.gameplay.powers.whenplayed.CitizenshipWhenPlayedSuite"`
Expected: three tests FAIL. "the Reliquary's relics stay set aside"
fails its `assert(game.campaign.reliquarySetAside)`. "an Exile may place
their pawn at any site the bandits rule" is offered the site without
warbands. "Bewitch: a Reliquary set aside gives no relic" finds a relic
gone from the Reliquary.

- [ ] **Step 4: Set the Reliquary aside**

In `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`:

Replace

```scala
  * later game, the Empire's roles, purple warbands, Grand Scepter, title and
  * banners, or, with no Empire, the box's Exiles. The state is built
  * directly, as game start builds the whole game; nothing here is
  * journaled. */
object LaterGameSetup:
```

with

```scala
  * later game, the Empire's roles, purple warbands, Grand Scepter, title and
  * banners, or, with no Empire, the box's Exiles and the Reliquary set
  * aside. The state is built directly, as game start builds the whole
  * game; nothing here is journaled. */
object LaterGameSetup:
```

Then replace

```scala
  /** Setup With No Empire, off the map: every Imperial lineage in the box
    * becomes an Exile. The title, the banners, the Usurper Limiter and the
    * first player stay as the materializer set them, as in a first game;
    * the Grand Scepter stays in the box. */
  private def withoutEmpire(ready: ReadyGame, exiled: Vector[LineageId])
      : ReadyGame =
    ready.updateCampaign(campaign => campaign.copy(
      boxRoles = campaign.boxRoles ++ exiled.map(_ -> Role.Exile)))
```

with

```scala
  /** Setup With No Empire, off the map: every Imperial lineage in the box
    * becomes an Exile, and the Reliquary is set aside until the Chronicle
    * Phase (Foundations ruling 3). The title, the banners, the Usurper
    * Limiter and the first player stay as the materializer set them, as in
    * a first game; the Grand Scepter stays in the box. */
  private def withoutEmpire(ready: ReadyGame, exiled: Vector[LineageId])
      : ReadyGame =
    ready.updateCampaign(campaign => campaign.copy(
      boxRoles = campaign.boxRoles ++ exiled.map(_ -> Role.Exile),
      reliquarySetAside = true))
```

In `src/main/scala/distantlands/gameplay/powers/ReliquaryDraws.scala`:

Replace

```scala
  * journals its order, then takes the first relic. An empty Reliquary takes
  * nothing, and its line says so (ruling R22).
  */
```

with

```scala
  * journals its order, then takes the first relic. An empty Reliquary takes
  * nothing, and its line says so (ruling R22). So does a Reliquary that
  * Setup With No Empire set aside: its relics return in the Chronicle Phase
  * and cannot be taken in play (Foundations ruling 3).
  */
```

Then replace

```scala
      : Vector[Operation] = Vector(Branch((live, _) =>
    if live.game.campaign.reliquary.isEmpty then
      Vector(Note(power, _ => Some(empty(source))))
```

with

```scala
      : Vector[Operation] = Vector(Branch((live, _) =>
    if live.game.campaign.reliquary.isEmpty ||
        live.game.campaign.reliquarySetAside then
      Vector(Note(power, _ => Some(empty(source))))
```

The relics stay in `campaign.reliquary`, so `CardIndex` holds them in
`CardContainer.Reliquary` and the Chronicle's accounting conserves them
with no change. Setup's Reliquary pick is asked only of a Chancellor
(`SetupProcedure.tree`), and there is none.

- [ ] **Step 5: Offer an Exile the sites that hold bandits**

In `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala`:

Replace

```scala
  /** Where a player may place their pawn in Setup (CR p. 13, Empire ruling
    * R25). With no Empire, any site; an Imperial player, any Empire site; an
    * Exile, the Hintermost Empire site or a site the bandits rule. A site
    * with no warbands is ruled by nobody. */
  def legal(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
```

with

```scala
  /** Where a player may place their pawn in Setup (CR p. 13, Empire ruling
    * R25). In a first game, any site. In a later game an Imperial player
    * places at any Empire site, and an Exile at the Hintermost Empire site
    * or a site the bandits rule, which after Setup With No Empire is every
    * site that holds bandits. A site with no warbands is ruled by nobody.
    * A player with no such site may place anywhere, so Setup never
    * stalls. */
  def legal(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
```

Then replace

```scala
    val empire = map.hinterward.filter(held(ForceKind.Imperial))
    if empire.isEmpty then map.inPlay
    else if Imperials.isImperial(ready, player) then empire
    else map.hinterward.filter(site =>
      empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
```

with

```scala
    val empire = map.hinterward.filter(held(ForceKind.Imperial))
    // Game start refuses a later game whose Empire, kept or former, holds
    // no site (R28), so only a first game's Chronicle has an empty world.
    val firstGame = ready.game.campaign.baseline.world.isEmpty
    val open =
      if firstGame then map.inPlay
      else if Imperials.isImperial(ready, player) then empire
      else map.hinterward.filter(site =>
        empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
    if open.isEmpty then map.inPlay else open
```

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.powers.whenplayed.CitizenshipWhenPlayedSuite distantlands.application.LaterGameSetupChoicesSuite distantlands.testkit.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/GameState.scala src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala src/main/scala/distantlands/gameplay/powers/ReliquaryDraws.scala src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala src/test/scala/distantlands/gameplay/setup/NoEmpireSetupSuite.scala src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala src/test/scala/distantlands/testkit/Table.scala
git commit -m "feat(foundations): set the Reliquary aside with no Empire" -m "Setup With No Empire marks the Reliquary set aside: its relics stay in the
campaign's Reliquary, and a power that would take one finds it empty. An
Exile places their pawn at a site that holds bandits, not at one with no
warbands, and anywhere when no site qualifies." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 5: Play on a short map

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerSearch.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/ShatteredPortal.scala` (comment)
- Test: `src/test/scala/distantlands/testkit/Table.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/WalkerSearchSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/ProcedureWalkerParkSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/ShortMapPlaySuite.scala` (new)

**Interfaces:**
- Consumes (Task 1): `MapState.hinterward(placed)`. From the base:
  `WalkerSearch.narrow` and `reach`, `PhaseMenu.offers`,
  `MenuPlay.withoutBand` and `withoutMenu`, `ActPowerHarness.use`, `rules`
  and `roundDie`, `PowerFixture.quietTable` (p1's Act, p1's pawn at
  Ancient City, nothing at any site), `StonePortal`, `ShatteredPortal`,
  `StonePortalCard`.
- Produces:
  - `WalkerSearch.narrow` returns
    `Left(InvalidEventOrder("the decision offers no answer"))` for a
    required choose-one that declares no option. A walk never parks on one.
  - `Table.sitesInPlay(count: Int): Table`: the map cut to its first
    `count` sites Hinterward.

- [ ] **Step 1: Write the failing tests**

The test table gains a step that shortens the map:

In `src/test/scala/distantlands/testkit/Table.scala`:

Replace

```scala
  /** The Reliquary set aside for the game, as Setup With No Empire leaves
```

with

```scala
  /** The map cut to its first `count` sites Hinterward, with the slots
    * past them empty, as Wide Horizons leaves it. A site that is cut must
    * hold no card and no pawn. */
  def sitesInPlay(count: Int)(using munit.Location): Table =
    val map = game.game.current.map
    val (kept, cut) = map.hinterward.splitAt(count)
    cut.foreach { site =>
      val state = map.sites(site)
      if state.denizens.nonEmpty || state.relics.nonEmpty ||
          game.game.current.players.exists(_.pawnSite.contains(site)) then
        munit.Assertions.fail(
          s"${site.value} holds a card or a pawn and cannot be cut")
    }
    update(_.updateCurrent(_.copy(map =
      MapState.hinterward(kept.map(site => site -> map.sites(site))))))

  /** The Reliquary set aside for the game, as Setup With No Empire leaves
```

The search's own suite says what `narrow` does with a choose-one that
declares nothing:

In `src/test/scala/distantlands/gameplay/walker/WalkerSearchSuite.scala`:

Replace

```scala
    assertEquals(WalkerSearch.narrow(asked, only(_ => false)), Left(refused))
```

with

```scala
    assertEquals(WalkerSearch.narrow(asked, only(_ => false)), Left(refused))

  test("a required choose-one that declares no option is refused where " +
      "it is asked, as a search refuses it, and one passed when empty is " +
      "not asked"):
    val noAnswer = OathViolation.InvalidEventOrder(
      "the decision offers no answer")
    val nothing = DecisionQuery.ChooseOne(Vector.empty)
    assertEquals(WalkerSearch.narrow(decide(nothing), only(_ => true)),
      Left(noAnswer))
    assertEquals(WalkerSearch.reach(decide(nothing), only(_ => true)),
      Left(noAnswer))
    assertEquals(WalkerSearch.narrow(passable(nothing), only(_ => true)),
      Right(None))
```

`ProcedureWalkerParkSuite` parked a walk on an empty choose-one to show
that its answer is refused. Such a walk no longer parks, so the empty query
leaves that test and gets its own:

In `src/test/scala/distantlands/gameplay/ProcedureWalkerParkSuite.scala`:

Replace

```scala
  test("a malformed query is rejected as a contract failure before the " +
      "submitted answer is even looked at"):
    // An empty option set, and a partition whose single section takes every
    // option: both are queries no answer could make meaningful, so the
    // rejection names the query rather than the submission.
    val empty = decide.copy(
      query = DecisionQuery.ChooseOne(Vector.empty))
    val forced = decide.copy(query = DecisionQuery.Partition(
      Vector(DecisionSection("all", "All", 1)),
      Vector(DecisionOption.Button(continueOption, "Continue"))))

    Vector(
      empty -> "declares no options",
      forced -> "declares fewer than two sections"
    ).foreach { case (node, detail) =>
```

with

```scala
  test("a malformed query is rejected as a contract failure before the " +
      "submitted answer is even looked at"):
    // A partition whose single section takes every option is a query no
    // answer could make meaningful, so the rejection names the query
    // rather than the submission.
    val forced = decide.copy(query = DecisionQuery.Partition(
      Vector(DecisionSection("all", "All", 1)),
      Vector(DecisionOption.Button(continueOption, "Continue"))))

    Vector(
      forced -> "declares fewer than two sections"
    ).foreach { case (node, detail) =>
```

Then replace

```scala
          : Either[OathViolation, WalkerOutcome])
    }

  test("a Decide answered by anyone but its owner is rejected as " +
```

with

```scala
          : Either[OathViolation, WalkerOutcome])
    }

  test("a walk does not park on a required choose-one that declares no " +
      "option"):
    // Nothing could answer it, so the command that reaches it is refused
    // and the state stays as it was.
    val empty = decide.copy(query = DecisionQuery.ChooseOne(Vector.empty))
    val tree: Operation = Sequence(empty, spendOneSupply)
    assertEquals(ProcedureWalker.advance(ready, tree, None, noPowers),
      Left(OathViolation.InvalidEventOrder("the decision offers no answer"))
        : Either[OathViolation, WalkerOutcome])

  test("a Decide answered by anyone but its owner is rejected as " +
```

And play on a short map:

Create `src/test/scala/distantlands/gameplay/ShortMapPlaySuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.{PhasePowerCatalog, PowerFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{ShatteredPortal, StonePortal,
  StonePortalCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Table}
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Unwrap.accepted

/** Play on a map with empty slots (Foundations design, slice 3, "Empty
  * slots"): what reads the map's shape counts the sites in play, and a
  * map of one site offers nothing that needs another. p1 acts. */
class ShortMapPlaySuite extends munit.FunSuite:
  import PowerFixture.{actor, quietTable}

  private val home = Look(quietTable).pawn(actor)
  private val portal = DecisionOptionRef.Edifice(StonePortalCard.id)
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = walkerPowers,
    phasePowerCatalog = PhasePowerCatalog.default)

  /** p1's Act on a map cut to its first `sites` sites, every pawn at the
    * first, with 1 faceup secret and the Stone Portal there on `side`. */
  private def portalOnMapOf(sites: Int, side: EdificeSide): ReadyGame =
    Table.from(quietTable).pawn(Table.p2, home).pawn(Table.p3, home)
      .sitesInPlay(sites).turn(actor, Phase.Act)
      .edifice(StonePortalCard.id, side, at = home).secrets(actor, 1).ready

  /** The Act menu's option `ref` as p1 is offered it. */
  private def offered(state: ReadyGame, ref: DecisionOptionRef)
      : DecisionOption =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, state, actor,
      PhasePowerCatalog.default, walkerPowers)
      .map(offer => MenuPlay.withoutBand(offer.option)).find(_.ref == ref)
      .getOrElse(fail(s"the Act menu does not list $ref"))

  test("on a map of one site, Travel is shown unavailable with its reason"):
    val oneSite = portalOnMapOf(1, EdificeSide.Ruined)
    assertEquals(oneSite.game.current.map.inPlay, Vector(home))
    assertEquals(offered(oneSite,
      PhaseMenu.optionFor(ActionRef.Travel, Vector.empty).get),
      DecisionOption.Unavailable(
        DecisionOption.Button(DecisionOptionRef.Button("travel"), "Travel"),
        OptionReason.Written("No site to travel to")))

  test("on a map of one site, Stone Portal has no other site to offer: it " +
      "is shown unavailable, and starting it is refused"):
    val oneSite = portalOnMapOf(1, EdificeSide.Intact)
    assert(!offered(oneSite, portal).available, offered(oneSite, portal))
    assertEquals(rules.startWalker(Ready(MenuPlay.withoutMenu(oneSite)),
      ActionRef.UsePower(StonePortal.id), actor, Vector(portal)).left.toOption,
      Some(OathViolation.InvalidEventOrder("the decision offers no answer")))
    // With a second site it is offered again.
    assert(offered(portalOnMapOf(2, EdificeSide.Intact), portal).available)

  test("Shattered Portal counts the sites on the map, not its slots: on " +
      "three sites a 1 moves one site on, a 2 wraps to the last, and a 3 " +
      "comes back to where it started"):
    val threeSites = portalOnMapOf(3, EdificeSide.Ruined)
    val sites = threeSites.game.current.map.hinterward
    assertEquals(sites.size, 3)
    def landing(face: RoundDieFace): SiteId =
      Look(ready(accepted(ActPowerHarness.use(ActPowerHarness.rules(
        ActPowerHarness.roundDie(face)), threeSites, ShatteredPortal.id,
        portal)).state)).pawn(actor)
    assertEquals(RoundDieFace.values.toVector.map(landing), Vector(
      sites(1), sites(2), sites(0), sites(1), sites(2), sites(0)))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.WalkerSearchSuite distantlands.gameplay.ProcedureWalkerParkSuite distantlands.gameplay.ShortMapPlaySuite"`
Expected: three tests FAIL. `WalkerSearchSuite`'s new test obtains
`Right(Some(...))` from `narrow` where it expects the refusal.
`ProcedureWalkerParkSuite`'s "a walk does not park on a required
choose-one that declares no option" obtains a park. `ShortMapPlaySuite`'s
Stone Portal test finds the Portal offered as available. Its Travel and
Shattered Portal tests pass already: they pin what holds today (findings 3
and 4).

- [ ] **Step 3: Refuse a choose-one that declares no option**

In `src/main/scala/distantlands/gameplay/walker/WalkerSearch.scala`:

Replace

```scala
    * narrowed. `Left` is a required decision with nothing left, carrying the
    * first option's rejection. `Right(None)` is an optional one with nothing
    * left, which is not asked: a choose-many whose minimum is zero, or a
    * choose-one passed when empty.
    */
```

with

```scala
    * narrowed. `Left` is a required decision with nothing left, carrying the
    * first option's rejection, or a required choose-one that declares no
    * option, which nothing could answer. `Right(None)` is an optional one
    * with nothing left, which is not asked: a choose-many whose minimum is
    * zero, or a choose-one passed when empty.
    */
```

Then replace

```scala
      Left(rejected.head)
    else Right(narrowed(decide, ref => verdicts(ref).isRight))
```

with

```scala
      Left(rejected.head)
    else if required && declaresNothing(decide) then Left(noAnswer)
    else Right(narrowed(decide, ref => verdicts(ref).isRight))

  /** Whether `decide` is a choose-one that declares no option at all,
    * available or not: a question nothing can answer, where a walk that
    * parked would stay. A choose-many left with nothing is cut to a choice
    * of none ([[narrowed]]), which its player can confirm. */
  private def declaresNothing(decide: Decide): Boolean = decide.query match
    case one: DecisionQuery.ChooseOne => one.options.isEmpty
    case _ => false

  private val noAnswer: OathViolation =
    OathViolation.InvalidEventOrder("the decision offers no answer")
```

Then replace

```scala
      first: Option[OathViolation]): Either[OathViolation, Unit] =
    if !answers.hasNext then Left(first.getOrElse(
      OathViolation.InvalidEventOrder("the decision offers no answer")))
    else verdict(answers.next()) match
```

with

```scala
      first: Option[OathViolation]): Either[OathViolation, Unit] =
    if !answers.hasNext then Left(first.getOrElse(noAnswer))
    else verdict(answers.next()) match
```

`reach` already ends in that violation for a decision whose answers run
out, through `survivor`; `narrow` now agrees with it where a decision is
asked. The Act menu searches each of its options, so an action that leads
straight to such a question is shown unavailable: `ActOffers.reason` words
it for an action ("No site to travel to"), and a power shows "Nothing to
do now".

- [ ] **Step 4: Correct Shattered Portal's comment**

In `src/main/scala/distantlands/gameplay/powers/action/ShatteredPortal.scala`:

Replace

```scala
  * defense dice, so no reroll is offered. The placement is a plain `Move`,
  * as Stone Portal's is. The map holds 8 sites, so a roll of 1 to 6 never
  * lands on the starting site. With the pawn elsewhere (the user rules the
  * site), the option shows unavailable with [[NotHere]]. Its line: "Rolled
  * 4; {Blue} placed at {site}."
  */
```

with

```scala
  * defense dice, so no reroll is offered. The placement is a plain `Move`,
  * as Stone Portal's is. A full map holds 8 sites, so a roll of 1 to 6
  * never lands on the starting site. A map with empty slots holds fewer:
  * the wrap counts the sites on it, and a roll that is a multiple of their
  * number leaves the pawn where it stands. With the pawn elsewhere (the
  * user rules the site), the option shows unavailable with [[NotHere]]. Its
  * line: "Rolled 4; {Blue} placed at {site}."
  */
```

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.walker.* distantlands.gameplay.ProcedureWalkerParkSuite distantlands.gameplay.ShortMapPlaySuite distantlands.gameplay.powers.action.StonePortalSuite distantlands.gameplay.powers.action.ShatteredPortalSuite distantlands.testkit.*"`
Expected: PASS.

Every walk goes through the search, so run the whole JVM suite before
committing:

Run: `./sbtw test`
Expected: PASS. If another suite fails because a walk it parked on an
empty choose-one is now refused, that suite was parking a walk no player
could continue: read the test, and report it rather than weaken the
search.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/walker/WalkerSearch.scala src/main/scala/distantlands/gameplay/powers/action/ShatteredPortal.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/gameplay/walker/WalkerSearchSuite.scala src/test/scala/distantlands/gameplay/ProcedureWalkerParkSuite.scala src/test/scala/distantlands/gameplay/ShortMapPlaySuite.scala
git commit -m "fix(walker): refuse a choose-one that declares no option" -m "On a map of one site Stone Portal had no other site to offer, and using
it parked the walk on a question nothing could answer. The search now
refuses such a choose-one where it is asked, as it already did when it
reached one, so the action is shown unavailable. Tests pin Travel and
Shattered Portal on a short map: the wrap counts the sites in play." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 6: The projection and the empty frame

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
- Modify: `frontend/styles.css`
- Modify: `DESIGN.md`
- Test: `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala` (new)
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/EmptySiteSlotSuite.scala` (new)

**Interfaces:**
- Consumes (Tasks 1 and 2): `MapState.sitesIn(region)`,
  `MapState.emptySlots(region)`, `Region.key`; a Wide Horizons game start.
  From the base: `GamePresentationProjector.readyWorld(ready, viewer)`,
  `WorldBoardRenderer.world(projection, options)`, the frontend's
  `GameRegion` (an alias of `SetupRegionProjection`), `TestSites.quietSite`,
  `TestProjections.actingProjection`, `Dom.all`.
- Produces:
  - `SetupRegionProjection.emptySlots: Int` (last field, default 0), on the
    wire as `emptySlots`; a payload without it decodes as 0.
  - The World pane draws `region.emptySlots` elements
    `div.site-slot-empty` after the region's sites.
  - Slice 3b needs nothing more here: once a site is placed, the
    projection sends one site more and one frame fewer.

- [ ] **Step 1: Read the design system**

Read `DESIGN.md`: "Layout" (the paragraph on the map's regions and
`--site-h`), "Cards" (the border of an empty card slot), "Sites" and
"Do's and Don'ts". The frame uses only what is there: the site's height
and 9px radius, a 1px dashed Line-mid border as an empty card slot has, no
fill.

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** The map as the table draws it: each region's sites in slot order, and
  * the slots that hold none (Foundations design, slice 3). */
class GamePresentationProjectorMapSuite extends munit.FunSuite:
  private val projector = new GamePresentationProjector(catalog)
  private val p1 = PlayerId("p1")

  test("a full map projects eight sites and no empty slot"):
    val world = projector.readyWorld(FirstGameSetupFixture.freshReady,
      Some(p1))
    assertEquals(world.map(_.regionId),
      Vector("cradle", "provinces", "hinterland"))
    assertEquals(world.map(_.sites.size), Vector(2, 3, 3))
    assertEquals(world.map(_.emptySlots), Vector(0, 0, 0))

  test("a Wide Horizons map projects each region's sites and the slots " +
      "past them"):
    // l1 is the Chancellor of an Empire of four sites, and no slot past
    // them is filled.
    val record = LaterGameFixture.chronicle(LineageId("l1"), world = 4).copy(
      foundations = Map(FoundationNumber.I ->
        FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val world = projector.readyWorld(ready, Some(p1))
    assertEquals(world.flatMap(_.sites.map(_.siteId)),
      record.world.map(_.site.value))
    assertEquals(world.map(_.sites.size), Vector(2, 2, 0))
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))
```

In `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`:

Replace

```scala
      Right(projection.world.map(_ => Vector.empty)))
```

with

```scala
      Right(projection.world.map(_ => Vector.empty)))

  test("a region's empty site slots round-trip, and a payload without " +
      "them decodes to none"):
    val short = projection.copy(world = projection.world.map(
      _.copy(emptySlots = 2)))
    assertRoundTrips(short)
    val raw = ujson.read(GameProjectionCodec.encode(short))
    raw("world").arr.foreach(region => region.obj.remove("emptySlots"))
    assertEquals(GameProjectionCodec.decode(ujson.write(raw))
      .map(_.world.map(_.emptySlots)), Right(projection.world.map(_ => 0)))
```

Create `frontend/src/test/scala/distantlands/frontend/EmptySiteSlotSuite.scala`:

```scala
package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.all
import TestProjections.actingProjection
import TestSites.quietSite
import org.scalajs.dom

/** A region's site slots that hold no site, drawn as frames after its
  * sites (Foundations design, slice 3). */
class EmptySiteSlotSuite extends munit.FunSuite:
  private val woods = quietSite("site:woods", "Deep Woods")

  /** The World pane of a table whose map is `regions`. */
  private def world(regions: GameRegion*): dom.Element =
    WorldBoardRenderer.world(actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      world = regions.toVector), BoardOptions.none)

  /** The class of each box in the region's column of sites, top first. */
  private def slotClasses(root: dom.Element): Vector[String] =
    all(root, ".sites > *").map(_.getAttribute("class"))

  test("a region draws a frame for each slot that holds no site, after " +
      "its sites"):
    val root = world(GameRegion("provinces", Vector(woods), emptySlots = 2))
    assertEquals(slotClasses(root),
      Vector("site site-readonly", "site-slot-empty", "site-slot-empty"))
    val frames = all(root, ".site-slot-empty")
    assertEquals(frames.map(_.getAttribute("aria-label")),
      Vector.fill(2)("Empty site slot, Provinces"))
    assertEquals(frames.map(_.getAttribute("role")), Vector.fill(2)("img"))
    assertEquals(frames.map(_.childElementCount), Vector(0, 0))
    assertEquals(frames.map(_.textContent), Vector("", ""))

  test("a region with no site draws only its frames"):
    val root = world(GameRegion("hinterland", Vector.empty, emptySlots = 3))
    assertEquals(slotClasses(root), Vector.fill(3)("site-slot-empty"))
    assertEquals(all(root, ".site").size, 0)

  test("a full region draws no frame"):
    val root = world(GameRegion("cradle", Vector(woods)))
    assertEquals(slotClasses(root), Vector("site site-readonly"))

  test("an empty frame is no board option"):
    val root = world(GameRegion("hinterland", Vector.empty, emptySlots = 1))
    assertEquals(all(root, ".site-slot-empty[data-option-ref]").size, 0)
    assertEquals(all(root, ".site-slot-empty button").size, 0)
```

- [ ] **Step 3: Add the field, so the tests compile**

In `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`:

Replace

```scala
      * its faces are not. */
    discardCards: Vector[CardDetailsProjection] = Vector.empty
)
```

with

```scala
      * its faces are not. */
    discardCards: Vector[CardDetailsProjection] = Vector.empty,
    /** The region's site slots that hold no site, which follow `sites`:
      * slots fill going Hinterward. */
    emptySlots: Int = 0
)
```

- [ ] **Step 4: Run the tests to make sure they fail**

Run: `./sbtw "testOnly distantlands.application.GamePresentationProjectorMapSuite distantlands.protocol.ProjectionProtocolSuite"`
Expected: two tests FAIL. "a Wide Horizons map projects each region's
sites and the slots past them" obtains `Vector(0, 0, 0)` empty slots, and
"a region's empty site slots round-trip" loses them in the codec.

Run: `./sbtw "frontend/testOnly distantlands.frontend.EmptySiteSlotSuite"`
Expected: two tests FAIL, "a region draws a frame for each slot that holds
no site, after its sites" and "a region with no site draws only its
frames": no frame is drawn. The other two pass, since they expect none.

- [ ] **Step 5: Send the empty slots**

In `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`:

Replace

```scala
    "discardCards" -> encoded(value.discardCards)(encodeCard),
    "sites" -> encoded(value.sites)(encodeSite))
  def decodeRegion(raw: ujson.Value, path: String): Result[SetupRegionProjection] = for
    value <- obj(raw, path)
    _ <- exact(value, Set("regionId", "discardCount", "discardTopCardKind",
      "discardCards", "sites"), path)
    region <- string(value, "regionId", path); count <- intOr(value, "discardCount", path, 0)
    top <- optionalAbsent(value, "discardTopCardKind", path)(string)
    cardRaws <- default(value, "discardCards", path, Vector.empty[ujson.Value])(array)
    cards <- traverse(cardRaws, s"$path.discardCards")(decodeCard)
    raws <- array(value, "sites", path); sites <- traverse(raws, s"$path.sites")(decodeSite)
  yield SetupRegionProjection(region, sites, count, top, cards)
```

with

```scala
    "discardCards" -> encoded(value.discardCards)(encodeCard),
    "sites" -> encoded(value.sites)(encodeSite),
    "emptySlots" -> value.emptySlots)
  def decodeRegion(raw: ujson.Value, path: String): Result[SetupRegionProjection] = for
    value <- obj(raw, path)
    _ <- exact(value, Set("regionId", "discardCount", "discardTopCardKind",
      "discardCards", "sites", "emptySlots"), path)
    region <- string(value, "regionId", path); count <- intOr(value, "discardCount", path, 0)
    top <- optionalAbsent(value, "discardTopCardKind", path)(string)
    cardRaws <- default(value, "discardCards", path, Vector.empty[ujson.Value])(array)
    cards <- traverse(cardRaws, s"$path.discardCards")(decodeCard)
    raws <- array(value, "sites", path); sites <- traverse(raws, s"$path.sites")(decodeSite)
    emptySlots <- intOr(value, "emptySlots", path, 0)
  yield SetupRegionProjection(region, sites, count, top, cards, emptySlots)
```

In `src/main/scala/distantlands/application/GamePresentationProjector.scala`:

Replace

```scala
  def readyWorld(ready: ReadyGame, viewer: Option[PlayerId]) =
    val current = ready.game.current
    Vector(
      region("cradle", Region.Cradle, current.map.cradle, ready, viewer),
      region("provinces", Region.Provinces, current.map.provinces, ready, viewer),
      region("hinterland", Region.Hinterland, current.map.hinterland, ready, viewer))

  private def region(id: String, where: Region, sites: Vector[SiteId],
      ready: ReadyGame, viewer: Option[PlayerId]): SetupRegionProjection =
    val current = ready.game.current
```

with

```scala
  def readyWorld(ready: ReadyGame, viewer: Option[PlayerId]) =
    Region.all.map(region(_, ready, viewer))

  private def region(where: Region, ready: ReadyGame,
      viewer: Option[PlayerId]): SetupRegionProjection =
    val current = ready.game.current
```

Then replace

```scala
    }
    SetupRegionProjection(id, sites.map(site =>
      siteProjection(site, current.map.sites.get(site), ready, viewer)),
      cards.size, cards.headOption.map(_.cardKind), cards)
```

with

```scala
    }
    SetupRegionProjection(where.key, current.map.sitesIn(where).map(site =>
      siteProjection(site, current.map.sites.get(site), ready, viewer)),
      cards.size, cards.headOption.map(_.cardKind), cards,
      current.map.emptySlots(where))
```

`Region.key` is the region id the projection already sent: `cradle`,
`provinces`, `hinterland`.

- [ ] **Step 6: Draw the frames**

In `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`:

Replace

```scala
        sites.appendChild(control)
      }
      section.appendChild(sites)
      // The world deck shares the Cradle's foot with the round tracker, the
```

with

```scala
        sites.appendChild(control)
      }
      // The slots no site fills follow the sites, each a frame the size of
      // a site, so a region keeps its height whatever it holds.
      (1 to region.emptySlots).foreach(_ => sites.appendChild(emptySlot(name)))
      section.appendChild(sites)
      // The world deck shares the Cradle's foot with the round tracker, the
```

Then replace

```scala
    panel.appendChild(layout)
    panel

  private[frontend] final case class RoundSegment(path: String, labelX: Double,
```

with

```scala
    panel.appendChild(layout)
    panel

  /** A site slot of the region `regionName` that holds no site: a frame
    * with nothing in it, and nothing to choose or inspect. */
  private def emptySlot(regionName: String): dom.Element =
    val slot = element("div", "site-slot-empty")
    slot.setAttribute("role", "img")
    slot.setAttribute("aria-label", s"Empty site slot, $regionName")
    slot

  private[frontend] final case class RoundSegment(path: String, labelX: Double,
```

`name` is the region's printed name, which the loop already holds. The
frame is not a `.site`: it carries no option, pawn or card, and nothing
that selects sites meets it.

`--site-h` moves from `.site` up to `.sites`, where a frame beside a site
inherits it too:

In `frontend/styles.css`:

Replace

```css
.sites { display: grid; grid-template-columns: 1fr; gap: 9px; }
/* One size for every site, whatever it holds this turn. A pawn arriving, a
```

with

```css
/* The height of a site slot, filled or empty, set where both kinds of box
   inherit it. */
.sites { display: grid; grid-template-columns: 1fr; gap: 9px;
  --site-h: 21.5rem; }
/* One size for every site, whatever it holds this turn. A pawn arriving, a
```

Then replace

```css
  --card-w: min(8.6rem, calc((100% - 2 * var(--card-gap)) / 3));
  /* Heading, pawns, three cards and two lines of powers; the box is the
     same height on the zoomed-out map, where the powers hold the glance
     size and clamp to one line. */
  --site-h: 21.5rem;
}
/* Sized from the width rather than a length of their own, so a card keeps its
```

with

```css
  --card-w: min(8.6rem, calc((100% - 2 * var(--card-gap)) / 3));
  /* Its height, `--site-h` on `.sites`, holds the heading, pawns, three
     cards and two lines of powers; the box is the same height on the
     zoomed-out map, where the powers hold the glance size and clamp to one
     line. */
}
/* A slot that holds no site: a frame the size of a site, dashed as every
   place that is not filled yet, so a short region keeps the height of a
   full one. */
.site-slot-empty { min-width: 0; height: var(--site-h);
  border: 1px dashed var(--line-mid); border-radius: 9px;
  background: transparent; }
/* Sized from the width rather than a length of their own, so a card keeps its
```

- [ ] **Step 7: Record the frame in the design system**

In `DESIGN.md`:

Replace

~~~markdown
(`--site-h: 21.5rem`) whatever it holds, so a pawn arriving or a preview
opening never shifts the board. The card row inside a site is three cards
wide: `--card-w: min(8.6rem, (100% - 2 gaps) / 3)`.
~~~

with

~~~markdown
(`--site-h: 21.5rem`) whatever it holds, so a pawn arriving or a preview
opening never shifts the board. An empty site slot is a frame of that
height, so a region with fewer sites than slots is as tall as a full one. The card row inside a site is three cards
wide: `--card-w: min(8.6rem, (100% - 2 gaps) / 3)`.
~~~

Then replace

~~~markdown
Bright Cream with the target halo; selected turns Selection blue on Selection
Fill.
~~~

with

~~~markdown
Bright Cream with the target halo; selected turns Selection blue on Selection
Fill. A slot of a region that holds no site is an empty frame after the
region's sites (`.site-slot-empty`): the site's height and radius, a 1px
dashed Line-mid border, no fill and nothing inside. It is not an option and
answers nothing; its label names it for a screen reader.
~~~

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.application.* distantlands.protocol.*"`
Expected: PASS.

Run: `./sbtw "frontend/test"`
Expected: PASS, the four tests of `EmptySiteSlotSuite` among them.

- [ ] **Step 9: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala src/main/scala/distantlands/application/GamePresentationProjector.scala frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala frontend/styles.css DESIGN.md src/test/scala/distantlands/application/GamePresentationProjectorMapSuite.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala frontend/src/test/scala/distantlands/frontend/EmptySiteSlotSuite.scala
git commit -m "feat(table): draw an empty site slot as an empty frame" -m "The projection gives each region the number of its slots that hold no
site, and the World pane draws that many frames after the region's sites:
the height and radius of a site, a dashed border, nothing inside. A short
region keeps the height of a full one." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 7: The Chronicle Phase on a map with empty slots

**Files:**
- Test: `src/test/scala/distantlands/gameplay/chronicle/EmptySlotChronicleSuite.scala` (new)

**Interfaces:**
- Consumes (Tasks 1 to 4): a game set up with no Empire, with
  `campaign.reliquarySetAside`. From the base:
  `EvolvedChronicleFixture.rules`, `gameWonFrom(wakes: OathState)`,
  `chronicleAnsweredAfter(won)` and `chronicleJournaledBy(done)`,
  `ChronicleExtraction.layout`, `ChronicleAccounting.knownAndUnique` and
  `conserves`, `MenuPlay.withoutMenu`, `Table.from`, `Situation.wake`.
- Produces: nothing new. This task changes no production code (finding 5).

- [ ] **Step 1: Write the tests**

Create `src/test/scala/distantlands/gameplay/chronicle/EmptySlotChronicleSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.testkit.{MenuPlay, Outcomes, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** A game on a map with empty slots, through the Chronicle Phase and into
  * the next game (Foundations design, slice 3, "Chronicle"). The game is
  * set up with no Empire on a former Empire of three sites, with two relics
  * in the Reliquary; p1 wakes as the Usurper, wins, and every task is
  * answered plainly. */
class EmptySlotChronicleSuite extends munit.FunSuite:
  import EvolvedChronicleFixture.rules

  private val (l8, l9) = (LineageId("l8"), LineageId("l9"))

  /** A Chronicle whose Empire of three sites nobody at the table belongs
    * to, with the first two relics of its relic deck in the Reliquary. */
  private val absentEmpireChronicle: Chronicle =
    val record = LaterGameFixture.chronicle(l9, Vector(l8), 3)
    record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = record.relicDeck.take(2))

  /** The no-Empire game after Setup, as p1's Wake begins with the Usurper
    * side of the title. */
  private lazy val usurperWakesWithNoEmpire: OathState =
    val setUp = Situation.wake(Situation.rules(catalog),
      absentEmpireChronicle,
      LaterGameFixture.orders(absentEmpireChronicle, p1)).ready
    Table.from(MenuPlay.withoutMenu(setUp)).turn(p1, Phase.Wake)
      .oathkeeper(Some(p1), TitleSide.Usurper).state

  private lazy val won = EvolvedChronicleFixture.gameWonFrom(
    usurperWakesWithNoEmpire)
  private lazy val done = EvolvedChronicleFixture.chronicleAnsweredAfter(won)
  private lazy val journaled = EvolvedChronicleFixture.chronicleJournaledBy(
    done)

  test("the Chronicle Phase runs every task on a map of three sites to a " +
      "record that passes game start's accounting, and replays"):
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    val ended = Outcomes.ready(done.state)
    assertEquals(ended.game.current.map.inPlay,
      absentEmpireChronicle.world.map(_.site))
    assertEquals(ended.game.campaign.evolved, Some(journaled))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, journaled),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(absentEmpireChronicle,
      journaled), Right(()))
    assertEquals(accepted((won.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](
        Right(usurperWakesWithNoEmpire))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)

  test("the Stars archive the sites the winner does not rule ahead of the " +
      "unexplored atlas sites, which keep their order"):
    val layout = accepted(ChronicleExtraction.layout(
      Outcomes.ready(done.state)))
    // The Throne gives a winner who rules no site their pawn's site.
    assertEquals(layout.empire.size, 1)
    assertEquals(layout.rivals.size, 2)
    assertEquals(journaled.world.map(_.site), layout.empire)
    assertEquals(journaled.atlasBox.map(_.site),
      layout.rivals ++ absentEmpireChronicle.atlasBox.map(_.site))
    assertEquals((layout.empire ++ layout.rivals).toSet,
      absentEmpireChronicle.world.map(_.site).toSet)

  test("erosion carries forward, the crowned Exile is the Chancellor and " +
      "the box stays exiled"):
    assertEquals(journaled.foundations.get(FoundationNumber.I),
      Some(FoundationState(FoundationFace.Altered, Set.empty)))
    assertEquals(journaled.foundations.get(FoundationNumber.V),
      Some(FoundationState(FoundationFace.Normal, Set.empty)))
    val roles = journaled.lineages.map(lineage => lineage.id -> lineage.role)
      .toMap
    assertEquals(roles(LineageId("l1")), Role.Chancellor)
    assertEquals(roles(l9), Role.Exile)
    assertEquals(roles(l8), Role.Exile)

  test("the set-aside Reliquary's relics return in the Chronicle Phase: " +
      "an Exile's win discards them to be scattered, and none is lost"):
    val reliquary = absentEmpireChronicle.reliquary
    assertEquals(Outcomes.ready(won.state).game.campaign.reliquary, reliquary)
    val scattered = journaled.relicDeck ++
      (journaled.world ++ journaled.atlasBox).flatMap(_.items)
    reliquary.foreach(relic => assert(scattered.contains(relic), relic))
    assert(!journaled.reliquary.exists(reliquary.contains))

  test("the next game starts from the journaled Chronicle under Wide " +
      "Horizons: the Empire's one site returns and no other slot is filled"):
    val orders = LaterGameFixture.orders(journaled, p1)
    val next = accepted(GameStartRules.evolve(catalog, journaled, orders))
    assertEquals(Imperials.chancellor(next), Some(p1))
    assertEquals(next.game.current.map.inPlay, journaled.world.map(_.site))
    assertEquals(next.game.current.map.nextSlot, Some(Region.Cradle))
    assertEquals(next.game.campaign.atlas.entries.collect {
      case stored: AtlasEntry.StoredSite => stored.id
    }, journaled.atlasBox.map(_.site))
    assert(!next.game.campaign.reliquarySetAside)
    val woken = Situation.wake(Situation.rules(catalog), journaled, orders)
    assertEquals(woken.ready.game.current.turn.activePlayer, p1)
    assert(woken.ready.game.current.players.forall(
      _.pawnSite == journaled.world.headOption.map(_.site)))
```

`gameWonFrom` finishes the Rest of a player who woke as the Usurper, which
ends the game; `chronicleAnsweredAfter` starts the Chronicle and answers
every task with its plainest answer. p1 rules no site, so the Throne's
Shape Empire gives them the site their pawn stands on, and the other two
former Empire sites are archived.

- [ ] **Step 2: Run them**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.EmptySlotChronicleSuite"`
Expected: PASS, five tests. They pass at once: the Chronicle Phase reads
the map through `inPlay` and `hinterward`, and Tasks 1 to 4 built the game
they walk. If one fails, a Chronicle step assumed a full map or lost a
card: stop, read the failure, and report it before changing production
code.

- [ ] **Step 3: Run the Chronicle suites**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/gameplay/chronicle/EmptySlotChronicleSuite.scala
git commit -m "test(chronicle): pin the Chronicle Phase on a map with empty slots" -m "A game set up with no Empire on three sites runs every Chronicle task to
a record that passes game start's accounting and replays. The sites the
winner does not rule are archived ahead of the unexplored atlas sites,
erosion and the exiled box carry forward, the set-aside Reliquary's relics
are scattered, and the next game starts under Wide Horizons on the
Empire's one site." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

### Task 8: Docs and verification

**Files:**
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

**Interfaces:**
- Consumes: everything Tasks 1 to 7 built; nothing new is produced.

Docs are plain prose. Two of these files hold very long lines (a table row,
a numbered item), so their blocks below are parts of a line, not whole
lines: replace exactly the text shown and leave the rest of the line.

- [ ] **Step 1: The setup architecture doc**

In `docs/architecture/game-setup.md`:

Replace

~~~markdown
`GameStartRules.evolve` validates only what Setup itself needs from the
Chronicle: known and unique atlas/denizen/relic ids, at least 8 atlas sites,
enough World Deck and relic cards to deal, a deal holding exactly the
Chronicle's decks, and one matching edifice per Homeland in play. The deeper suit-count and Vision-packet audit a Chronicle
must satisfy is the generator's own job, checked once at generation time
~~~

with

~~~markdown
`GameStartRules.evolve` validates only what Setup itself needs from the
Chronicle: known and unique atlas/denizen/relic ids, an atlas site for every
slot it fills from the atlas (8 in a first game),
enough World Deck and relic cards to deal, a deal holding exactly the
Chronicle's decks, and, in a first game, one matching edifice per Homeland in play. The deeper suit-count and Vision-packet audit a Chronicle
must satisfy is the generator's own job, checked once at generation time
~~~

Then replace

~~~markdown
The completed aggregate includes site resources and bandits, facedown site
~~~

with

~~~markdown
`EmpireSetupRules.resolve` reads how the game starts, a `SetupMode`. A
Chronicle that lists no Chancellor, no Citizen and no Empire site starts a
first game on eight atlas sites. With a Chancellor seated, or a seated
Citizen standing in for one, the Empire's sites return; Imperial Maps then
fills the slots past them from the atlas's Recent end, and Wide Horizons,
the altered Foundation I, fills none. With no Chancellor and no Citizen
seated, Setup With No Empire erodes Foundation I to Wide Horizons and
Foundation V to Mob's Favor, places only the former Empire's sites, as
bandit sites with ruined edifices and every open relic slot filled while
the relic deck lasts, turns the box's Imperial lineages into Exiles
(`CampaignState.boxRoles`) and sets the Reliquary aside
(`CampaignState.reliquarySetAside`): its relics stay in the campaign's
Reliquary and no one can take one in play.

The map's slots fill going Hinterward, 2 in the Cradle and 3 in each other
region (`Region.slots`), and a region's vector may be shorter than its
slots (`MapState.hinterward`, `nextSlot`, `hintermost`). The atlas keeps
every atlas site game start did not place, in order. The projection gives
each region its `emptySlots`, which the table draws as empty frames. No
action fills an empty slot yet: Explore is slice 3b of the
[Foundations design](../superpowers/specs/2026-11-11-foundations-design.md).

The completed aggregate includes site resources and bandits, facedown site
~~~

Then replace

~~~markdown
The scope follows Combined Rulebook pp. 6-7 and the New Foundations first-game
clarification on p. 8. It excludes Legacy selection/effects,
Chancellor/Citizen/Imperial setup, restoring the rest of a stored campaign
(roles, legacies, the Empire's sites), and the altered Foundation faces no
slice of the
~~~

with

~~~markdown
The scope follows Combined Rulebook pp. 6-7 and the New Foundations first-game
clarification on p. 8, and for later games the New Foundations rulebook's
"Setup for Later Games". It excludes Legacy selection/effects, the
Explore action, and the altered Foundation faces no
slice of the
~~~

- [ ] **Step 2: The traceability table**

In `docs/rules/implementation-traceability.md`:

Replace

~~~markdown
Of the altered Foundations, II (Movers and Shakers) and IV (Teeming World) are built; I, V and VI are refused at game start, and III is skipped until Legacies (see the foundations row below).
~~~

with

~~~markdown
Of the altered Foundations, I (Wide Horizons, without its Explore action), II (Movers and Shakers) and IV (Teeming World) are built; V and VI are refused at game start, and III is skipped until Legacies (see the foundations row below).
~~~

Then replace

~~~markdown
With a Chancellor in the Chronicle, game start seats the Chancellor first, or a random seated Citizen for an absent one (`EmpireSetupRules`, `GeneratedFirstGamePlanFactory`).
~~~

with

~~~markdown
Game start seats the Chancellor first, or a random seated Citizen when none is seated (`EmpireSetupRules`, `GeneratedFirstGamePlanFactory`).
~~~

Then replace

~~~markdown
`application/LaterGameSetupChoicesSuite.scala`). | The Empire-less setup (Wide Horizons), the altered faces of Foundations I, V and VI, legacies, starting relics, and the Chancellor's Recent-or-Forgotten choice. |
~~~

with

~~~markdown
`application/LaterGameSetupChoicesSuite.scala`). Under Wide Horizons the Empire's sites return and the slots past them stay empty (`WideHorizonsSetupSuite`). With no Chancellor and no Citizen seated, Setup With No Empire erodes Foundations I and V, places only the former Empire's sites as bandit sites with ruined edifices and every open relic slot filled while the relic deck lasts, exiles the box's Imperial lineages, sets the Reliquary aside and starts the title, the banners and the Usurper Limiter as a first game does (`SetupMode`, `NoEmpireSetupSuite`). A game on a map with empty slots runs the Chronicle Phase into the next game (`gameplay/chronicle/EmptySlotChronicleSuite.scala`). | The Explore action, so a Wide Horizons map never grows; the altered faces of Foundations V and VI; legacies, starting relics, and the Chancellor's Recent-or-Forgotten choice. |
~~~

Then replace

~~~markdown
| Altered Foundations I, V and VI are refused at game start until their slices of the [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md). Imperial Maps always fills from Recent Sites
~~~

with

~~~markdown
| Wide Horizons leaves the slots past the Empire's sites empty at game start (`WideHorizonsSetupSuite`), but its Explore action is not offered yet. Altered Foundations V and VI are refused at game start until their slices of the [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md). Imperial Maps always fills from Recent Sites
~~~

Then replace

~~~markdown
| Cradlemost/Hintermost vocabulary and full Atlas geography use | **Unimplemented** | [NF pp. 12, 16](new-foundations-delta.md#empire-and-other-changes); normative CR pp. 9, 45 | No explicit Cradlemost/Hintermost selector or setup/Chronicle rule consumes the full vocabulary. | Define tie/selection behavior where multiple sites share the relevant region. |
~~~

with

~~~markdown
| Cradlemost/Hintermost vocabulary and full Atlas geography use | **Partial** | [NF pp. 12, 16](new-foundations-delta.md#empire-and-other-changes); normative CR pp. 9, 45 | A region may hold fewer sites than its slots (`Region.slots`). `MapState.hintermost` is the last site Hinterward and `MapState.nextSlot` the region of the next empty slot (`model/WorldModelSuite.scala`); Setup's pawn sites read the Hintermost Empire site (`SetupPawnSites`). The table draws an empty slot as an empty frame (`EmptySiteSlotSuite`). | Nothing places a site in an empty slot until the Explore action and Imperial Maps' choice. |
~~~

In `docs/rules/ambiguities.md`:

Replace

~~~markdown
The altered faces of Foundations II (Movers and Shakers) and IV (Teeming World) are built at setup. The altered faces of I, V and VI are refused at game start until their slices
~~~

with

~~~markdown
The altered faces of Foundations II (Movers and Shakers) and IV (Teeming World) are built at setup, and so is Wide Horizons (I), which leaves the map's remaining slots empty; its Explore action is not offered yet. The altered faces of V and VI are refused at game start until their slices
~~~

- [ ] **Step 3: The roadmap**

Finished work is deleted from the roadmap, not ticked. Setup With No
Empire is done; Explore is not.

In `docs/ROADMAP.md`:

Replace

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, Wide Horizons with its Explore action
  and Setup With No Empire, and the Beacon's Shape Foundation.
- [ ] **The Grand Council and Festival banner faces.** Altered Foundations V
~~~

with

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, Wide Horizons' Explore action, and
  the Beacon's Shape Foundation.
- [ ] **The Grand Council and Festival banner faces.** Altered Foundations V
~~~

- [ ] **Step 4: The spec**

The status line, the split of slice 3, the statements finding 1, 3 and 7
correct, and the readings this plan took.

In `docs/superpowers/specs/2026-11-11-foundations-design.md`:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slices 1 and 2 landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1, 2 and 3a landed.
~~~

Then replace

~~~markdown
### Empty slots
~~~

with

~~~markdown
Planned in two parts. Slice 3a, landed, is the empty slots, Wide Horizons
at game start, Setup With No Empire and the Chronicle. Slice 3b is Explore,
so until it lands a map with empty slots never grows.

### Empty slots
~~~

Then replace

~~~markdown
draws an empty slot as an empty frame. One task audits the other readers of
the map's shape; at design time only the materializer slices eight, and the
Shattered Portal and Stone Portal wraps already walk `hinterward`.
~~~

with

~~~markdown
draws an empty slot as an empty frame. One task audits the other readers of
the map's shape. Slice 3a's audit found more than the materializer:
`GameStartRules` counted eight twice, `DomainValidation` required full
regions, and the projection read the region vectors alone. In play every
reader counts the sites on the map. Shattered Portal's wrap walks
`hinterward`, so it skips empty slots, and a roll that is a multiple of the
number of sites leaves the pawn where it stands. Stone Portal has no wrap:
it offers every other site, and on a map of one site it parked the walk on
a question with no option. The walker now refuses a required choose-one
that declares no option, so such an action is shown unavailable.
~~~

Then replace

~~~markdown
any Citizen is seated, game start no longer refuses (`EmpireSetupRules`).
~~~

with

~~~markdown
any Citizen is seated, game start no longer refuses (`EmpireSetupRules`).
The same holds for a Chronicle that lists no Chancellor: a seated Citizen
becomes the Chancellor, and with none seated the game has no Empire. Only a
Chronicle with no Chancellor, no Citizen and no Empire site starts a first
game.
~~~

Then replace

~~~markdown
- **Pawns.** An Exile may place their pawn at any site ruled by bandits,
  which here is every site.
~~~

with

~~~markdown
- **Pawns.** An Exile may place their pawn at any site ruled by bandits,
  which here is every site that holds bandits: a site of capacity 0 holds
  no warband and is ruled by nobody (R25).
- **Readings taken with the plan,** for the owner to confirm. The absent
  Chancellor's lineage becomes an Exile in the box with the Citizens. A
  former Empire with no site is refused, as an Empire's is (R28). A
  Foundation I already on Wide Horizons keeps its sources. Relic slots are
  filled Cradlemost first while the relic deck lasts, and the rest stay
  empty. If no site holds bandits, an Exile may place their pawn at any
  site. A power that takes a Reliquary relic finds a set-aside Reliquary
  empty. An Exile's win discards the Reliquary at Crown Chancellor as in
  any game, so the set-aside relics are scattered; Stock Reliquary adds to
  them only when a player who became the Chancellor in play wins. A first
  game places its eight sites whatever Foundation I's face.
~~~

Then replace

~~~markdown
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  slice 2's readings, and R27 closed) and
  `docs/rules/implementation-traceability.md`.
~~~

with

~~~markdown
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  the readings of slices 2 and 3a, and R27 closed) and
  `docs/rules/implementation-traceability.md`.
~~~

If the owner has ruled on any reading in "Rulings needing the owner's
review" by the time you run this task, write the ruling in place of the
reading, and say so in your report.

- [ ] **Step 5: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%.

Run: `./sbtw "frontend/test"` (the frontend suites, if the full run
skipped them)
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. No Game Log golden and no note-key snapshot changes in
this slice.

Run: `git grep -n "FoundationNumber.I -> 3\|requires Wide Horizons" -- src`
Expected: no output. Neither refusal is left.

- [ ] **Step 6: Commit**

```bash
git add docs/architecture/game-setup.md docs/rules/implementation-traceability.md docs/rules/ambiguities.md docs/ROADMAP.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 3a, empty slots and no Empire" -m "The setup doc says how a game starts with an Empire, under Wide Horizons
and with no Empire, and how the map's slots work. The traceability table
and the ambiguities note stop calling Wide Horizons refused, the roadmap
drops Setup With No Empire, and the spec records slice 3a landed, what its
audit found and the readings its plan took, which slice 7 moves to the
ambiguities doc." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

---

## Self-review

- **Scope, slice 3a.** Empty slots: `Region.slots`, `MapState.hintermost`
  and `nextSlot`, and the map built from the sites placed (Task 1); the
  audit of the map's readers in the engine (findings 1 to 5; Tasks 2, 4, 5
  and 7), the projection and the frontend's empty frame (Task 6). Wide
  Horizons at game start: with a Chancellor the Empire's sites return and
  no slot past them is filled, no Imperial Maps fill runs, and the refusal
  goes (Task 2). Setup With No Empire: the refusal goes; Foundations erode
  into `campaign.foundations` with no sources; the former Empire's sites
  fill slots going Hinterward with edifices ruined, denizens faceup, relics
  facedown, bandits up to capacity and every empty relic slot filled from
  the relic deck, and no other site is placed; the Grand Scepter stays in
  the box; every Citizen in the box becomes an Exile; the plan's random
  first player begins; the Usurper Limiter is on; the title and both
  banners start as in a first game (Task 3); the Reliquary's relics stay
  `campaign.reliquary` and never enter play, and an Exile places their
  pawn at any bandit-ruled site (Task 4). Chronicle: a game on a map with
  empty slots runs Stars, extraction and `ChronicleRecord` into a
  Chronicle that starts the next game, the unexplored atlas sites keep
  their order, and erosion carries forward (Task 7). Docs and the full
  gates (Tasks 6 and 8).
- **The spec's tests, for 3a.** Game start under Wide Horizons with an
  Empire (`WideHorizonsSetupSuite`) and without one (`NoEmpireSetupSuite`);
  the no-Empire state, one test a bullet (`NoEmpireSetupSuite`); pawn
  options (`NoEmpireSetupSuite`, with `LaterGameSetupChoicesSuite` already
  holding the Empire's); card accounting with the Reliquary set aside
  (`NoEmpireSetupSuite`'s card index and domain validation, and
  `EmptySlotChronicleSuite`'s `conserves`); a Chronicle round trip from a
  map with empty slots (`EmptySlotChronicleSuite`). The spec's other slice
  3 tests are Explore's and wait for slice 3b.
- **Left for slice 3b.** The Explore action, the journaled `PlaceAtlasSite`
  operation, the `WhenExplored` window, the Act menu entry, its log lines
  and its golden. Until then a Wide Horizons map never grows, which the
  roadmap and the traceability table say. What 3b finds ready:
  `MapState.hintermost` for the gate, `nextSlot` for the region a site goes
  to, `emptySlots` and the projection's frames, which need no change when
  a site is placed, and `campaign.atlas`, whose head is the Recent end and
  whose last entry is the Forgotten end, holding every unplaced site with
  its stored cards. `MapState` has no method that adds a site to the next
  slot; 3b writes it beside `MapState.hinterward`. The population of a
  placed atlas site (ruined edifice, faceup denizens, facedown stored
  relics, bandits, printed tokens, open slots dealt while the relic deck
  lasts) is `FirstGameSetupMaterializer.atlasSite` and `assignRelics`,
  private to game start; 3b's operation states the same in its own terms.
- **Seams for later slices.** Slice 4 turns Imperial Maps' fill into
  `PlaceAtlasSite`: `GameStartRules.atlasSlots` then returns 0 for an
  Empire on either face, and the relic-deck refusal goes with the fill
  (design decision 7). Slices 5 and 6 each still delete their own line of
  `StartingFoundations.arrivesWith` and their row of the refusal test;
  slice 5 must rewrite "with several altered faces", and its planned
  test of Grand Council in a no-Empire game cannot start from Setup With
  No Empire, where erosion turns Foundation V back to Mob's Favor: it
  needs `Table.foundation`. Slice 7 moves the spec's readings, slices 2's and 3a's, to
  `docs/rules/ambiguities.md` with whatever the owner decided, writes
  "empty slot" and "Hintermost" into `CONTEXT.md`, and offers the eroded
  Foundation I, which has no source, at Shape Foundation. Ruling 4 keeps
  the sources of a face that was already altered, for that offer.
- **Placeholders.** None.
- **Verified against a prototype.** The slice was built in a scratch
  worktree on `d8cabe43c`, then rebuilt as one commit per task; every block
  here was cut from those commits, and applying the plan's blocks in order
  to a fresh `d8cabe43c` reproduces each task's commit file for file (43
  files in all). On the final tree the gates of Task 8 passed: 5745 JVM
  tests, 929 frontend tests, statement coverage 95.28%, branch coverage
  89.57%, the architecture check, and no change under
  `src/test/resources`. The red steps were observed on the per-task
  commits: Task 2's six failures, Task 3's
  sixteen, Task 4's three with the flag added, Task 5's three and Task 6's
  two and two, as their steps say. Task 1's red step is a compile failure
  and was read from the code, not run. Task 7 has no red step: its tests
  pass at once. If a red step fails for another reason, trust the run and
  say so in the task's report.
- **Checked in a browser as a static page only.** The frame's CSS was
  loaded, with the slice's stylesheet, into a hand-written page of three
  regions: a frame is 344px tall, as a site is, with the dashed Line-mid
  border, the 9px radius and no fill, and the three regions are equally
  tall. The live table was not opened, and jsdom applies no stylesheet,
  so whoever reviews Task 6 should look at a Wide Horizons table at the
  Fit zoom. That needs a Chronicle with an altered Foundation I and an
  Empire of fewer than eight sites.
- **Risks.** Task 5 changes the walker's search for every action: it was
  run against the whole JVM suite, and only `ProcedureWalkerParkSuite`'s
  deliberately malformed query noticed, but a power outside the tests that
  parked on an empty choose-one now refuses instead. Task 3 reorders
  `buildPlan`'s checks: the seats are validated and the Empire resolved
  before the foundation faces are refused, so a Chronicle wrong in two
  ways may name the other one first. The base may have moved since
  `d8cabe43c`: the Setup greps say where.
- **Type consistency.** `Region.slots`; `MapState.sitesIn(region)`,
  `emptySlots(region)`, `nextSlot`, `hintermost`, `MapState.slots`,
  `MapState.hinterward(placed)`; `SetupMode.FirstGame`,
  `SetupMode.WithEmpire(empire)`, `SetupMode.NoEmpire(sites, exiled)`;
  `EmpireSetupRules.resolve(chronicle, orders): Either[OathViolation,
  SetupMode]`; `StartingFoundations.eroded(faces)`;
  `GameStartRules.atlasSlots(empire, faces)` in Task 2 and
  `atlasSlots(mode, faces)` from Task 3; `LaterGameSetup.apply(catalog,
  ready, plan, mode)`; `CampaignState.reliquarySetAside`;
  `Table.reliquarySetAside`, `Table.sitesInPlay(count)`;
  `SetupRegionProjection.emptySlots`; the CSS class `site-slot-empty`.
  The test helpers keep one name each: `wideHorizonsChronicle(empireSites)`
  in `WideHorizonsSetupSuite`, `absentEmpireChronicle(empireSites)` in
  `NoEmpireSetupSuite`, and the value `absentEmpireChronicle` in
  `EmptySlotChronicleSuite`.
