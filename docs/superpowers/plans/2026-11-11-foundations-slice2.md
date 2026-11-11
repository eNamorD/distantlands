# Foundations, Slice 2: Movers and Shakers and Teeming World Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A game whose Chronicle stores an altered Foundation II or IV
starts instead of being refused. Under Movers and Shakers every board starts
with 6 favor and each suit's bank holds 1 favor per denizen of that suit on
the map. Under Teeming World each player starts with three facedown
advisers and no adviser choice, the rest of the starting denizen stack is
dealt out to the three discard piles, and the world deck is the five Visions.

**Architecture:** Both faces are setup only and are built directly at game
start; nothing is journaled. `FirstGameSetupPlan` carries the game's
foundation faces, and `FirstGameSetupMaterializer` reads Foundation II's for
the boards and the banks. Foundation IV's deal of the starting stack becomes
one value, `gameplay.setup.SetupDeal`, which the plan factory
(`ChronicleFirstGamePlan`), game start's validation (`GameStartRules`) and
the materializer all read, so the three cannot disagree about how many cards
are dealt before the deck. `LaterGameSetup` puts a lineage's stored
advisers before the ones Teeming World deals. Setup's walk and Search do
not change: the walk
already asks an adviser choice only of a player with a hand, and a Search
already stops at a Vision.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`. No
frontend, protocol or journal change.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings", "Approach" and "Slice 2: Movers and Shakers and Teeming
World". Slices 3 to 7 are read only for the seams they need from this one.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `gameplay` must not read a catalog `.text` or `rulesText`
  (`scripts/check-architecture.py`). `model` imports nothing from
  `gameplay`: `SetupDeal` lives in `gameplay.setup`, and
  `FirstGameSetupPlan`, in `model`, carries only model types.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. Tests go in the repo's existing suites where one
  covers the subject. A test helper's name says what it builds.
- Wire and journal changes need not keep old journals decodable. This slice
  changes neither: `SetupOrders` keeps its shape, and a Ties of Fellowship
  deal reads the stack exactly as before.
- Owner rulings (spec, "Rulings" and slice 2), binding here:
  - The shared bank has no count. Taking favor from it changes nothing but
    the taker's board (ruling 5).
  - Movers and Shakers gives each suit's bank 1 favor per denizen of that
    suit on the map, edifices included, with no base 3 and no fifth-player
    favor.
  - Teeming World: each player draws facedown denizens until they hold three
    facedown starting advisers, so a lineage with one stored adviser draws
    two. They keep them all: no hand, no Choose Adviser step. The rest of the
    starting stack is dealt one card at a time to the Cradle, Provinces and
    Hinterland discard piles in turn, with no seeding of 2 per pile. The
    world deck is the five Visions alone.
- New Foundations rules only. A face's printed text is in
  `~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
  (ids `OATH-SETUP05b-NF` and `OATH-SETUP07b-NF`; each entry's `text:` comes
  before its `name:`), and the rulebook's "Foundations" section is in
  `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`. Read them
  there; make no web call.
- An unavailable option is shown disabled with its reason, never hidden.
  This slice adds no option; the rule binds any control you add.
- A Game Log line omits the actor on their own turn. This slice writes no
  line; the rule binds any you add.
- No note key and no Game Log golden changes in this slice. If
  `DeclaredNoteKeysSuite` or `GameLogGoldenSuite` fails, stop and read the
  diff before rewriting a snapshot. The rewrite commands, for a change you
  have understood, are
  `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
  and
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
- The word is "face" for a foundation card's two faces, as the model's
  `FoundationFace` has it.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" or "Delete" block is cut from the file as
  it stands at `92fb2e773` with the earlier tasks of this plan applied, and
  occurs once in its file. If a block does not match because a later fix
  moved the text, find the same code by its content and make the same
  change; if the code itself is different, stop and report.
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

Work in a git worktree cut from `0.2.0-alpha`, which holds slice 1. In the
worktree, link the main checkout's tooling before the first `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1, by content:

```bash
git merge-base --is-ancestor 92fb2e773 HEAD && echo base-ok
git grep -n "FoundationNumber.II -> 2" -- src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala
git grep -n "FoundationNumber.IV -> 2" -- src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala
git grep -n "StartingFoundations.refusal(StartingFoundations.of(chronicle))" -- src/main/scala/distantlands/gameplay/setup/GameStartRules.scala
git grep -n "def dealt(drawers: Int)" -- src/main/scala/distantlands/gameplay/setup/SetupDeal.scala
git grep -n "adviserChoices: Vector" -- src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala
git grep -n "with several altered faces, game start names the first in" -- src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala
git grep -n "Slice 1 landed" -- docs/superpowers/specs/2026-11-11-foundations-design.md
```

The first must print `base-ok`: `92fb2e773` is the merge that brought slice
1 into `0.2.0-alpha`. Each grep must print one line. They name the slice 1
code this plan edits: the two refusals it deletes, the refusal's call site,
the deal arithmetic it replaces, the materializer's dead parameter, the test
slice 1's plan said slice 2 must rewrite, and the spec's status line. If the
first prints nothing, the worktree was cut from a stale branch: stop and
report. If a grep prints nothing while the others print, the base has moved
under that file: read the file, and go on only if the code the task edits is
still there in substance.

The shared sources compile in the root project. A JVM suite runs as
`./sbtw "testOnly distantlands.<package>.<Suite>"`.

## Plan-time findings

The spec was checked against the code at `92fb2e773`. Where they differ,
this plan follows the code.

1. **`SetupDeal.dealt` cannot learn the face.** The spec says
   "`SetupDeal.dealt` and the deal validation learn the face".
   `SetupDeal.dealt(drawers: Int)` returns `6 + drawers * 3`. Teeming World
   deals the whole stack, however long, and its players draw 0 to 3 cards
   each, so neither a drawer count nor a dealt count describes it.
   `SetupDeal` becomes a value built from the Chronicle and the seats
   (`SetupDeal.of`), with the stack as the argument of each question:
   `needed`, `hand`, `kept`, `discards`, `deck`. Task 4 corrects the spec's
   sentence.
2. **The deal is computed in three places.** `SetupDeal.drawers` and `dealt`
   serve the plan factory (`ChronicleFirstGamePlan.dealOrder`, called by
   `GeneratedFirstGamePlanFactory`) and game start's checks
   (`GameStartRules.buildPlan`, `validateDeal`).
   `FirstGameSetupMaterializer.handFor` works the drawers out again from
   `plan.startingAdvisers`, and `initialDiscards` hard-codes the six seeded
   cards. Task 2 moves all three onto `SetupDeal`. The test fixture's own
   `ChronicleFixtureDealOrder` and `FirstGameSetupFixture.dealtBeforeDeckTop`
   stay: they deal a first game, which is always Ties of Fellowship.
3. **Two of `materialize`'s three parameters are dead.** Both callers
   (`GameStartRules.evolve` and `FirstGameSetupMaterializerSuite`) pass
   `Vector.empty` for `placements` and for `adviserChoices`: Setup's walk
   places pawns and moves the chosen and rejected advisers itself. So the
   fold in `initialDiscards` that adds rejected hand cards never runs, nor
   does `regionOf`, and `model.PawnPlacement` has no other reader. Task 2
   deletes them with the arithmetic they wrap (design decision 2).
4. **`SetupProcedure` needs no change.** `playerStep` asks the adviser
   choice only when the player's temporary hand holds a card, and
   `revealCards` reads the live advisers. With no hand dealt, the Choose
   Adviser step is skipped and Reveal Cards offers the three facedown
   advisers. Task 3 pins both with tests and corrects one comment.
5. **Search needs no change.** A world-deck Search already stops at a Vision
   and advances the Visions Drawn track, and `SearchOptions` already shows an
   empty world deck disabled with "The world deck is empty". Task 3 pins the
   first Search of a Teeming World game.
6. **The owner already sees their own facedown advisers.**
   `GamePresentationProjector.identifiesCard` names a player's facedown cards
   to their owner, as it does for stored starting advisers today. There is no
   hand to preview, so the projection and the frontend do not change.
7. **The rulebook and the card word Movers and Shakers' second paragraph
   differently.** The card: "for each denizen on the map, place [favor] on
   the matching favor bank". The rulebook: "add 1 extra favor [...] to the
   matching favor bank", the normal face's words. The owner's ruling follows
   the card: no base 3 and no fifth-player favor.
8. **"Through the existing short-deck mix" holds.**
   `ChronicleFirstGamePlan.dealOrder` mixes `remaining ++ visions` when
   fewer than 25 denizens remain. With none remaining that is the five
   Visions through the factory's `random.shuffle`.
9. **The 5-argument `dealOrder` has four callers besides the factory:**
   `LaterGameFixture.orders`, `GameStartRulesSuite` (the short-deck test) and
   two tests of `ChronicleFirstGamePlanSuite`. The 3-argument first-game
   overload keeps its signature, so `ParkedServiceFixture`,
   `TrustedGameProvisioningSuite` and `GameApplicationServiceStreamSuite`
   do not change.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **`SetupDeal` is a value.** `SetupDeal(face: FoundationFace, draws:
   Vector[(PlayerId, Int)])`, every seated player listed in seat order.
   `SetupDeal.of(chronicle, participants)` reads the Chronicle's Foundation
   IV face through `StartingFoundations.of` and each lineage's stored
   advisers. `SetupDeal.firstGame(participants)` is a first game's.
2. **`materialize(plan, deal)`.** The two dead parameters go
   (finding 3), with `handFor`, `initialDiscards`, `regionOf` and
   `PawnPlacement`. The materializer has no branch on Foundation IV: it asks
   the deal for each player's `kept` advisers and `hand` and for the
   `discards`.
3. **The plan carries the faces.** `FirstGameSetupPlan.foundations` holds
   the faces `GameStartRules.buildPlan` read and did not refuse, and
   `altered(number)` asks one. `GameStartRules.evolve` puts
   `plan.foundations` in the campaign, so the faces are read from the
   Chronicle once. Slices 5 and 6 read the same field for the banners'
   faces, and slice 3 changes it for erosion.
4. **Where a Teeming World deal reads the stack.** The players' draws come
   first, in seat order, each player's cards together; the discards follow.
   That is the card's order ("Each player draws [...]. Discard the rest").
   A Ties of Fellowship deal keeps today's layout, the six seeded cards
   first and then the hands, so no existing deal reads differently. The
   alpha's `ShufflePolicy.implementedFirst` puts unimplemented denizens last
   in the stack, so today the one unimplemented denizen
   (`denizen.reliquary-raid`) ends on top of a discard pile under Teeming
   World; that is accepted.
5. **A stack too short for the draws is refused** with the existing "not
   enough world-deck cards to deal". A world deck holds dozens of denizens
   against at most 18 drawn.
6. **A Teeming World deal whose world deck holds a denizen is refused** by
   the existing check, "the world deck is not the rest of the deal": the
   rest is empty.
7. **No Game Log line.** Both faces act before the first event a player
   sees, and slice 1's strip already names them on the table.
8. **Tests.** `SetupDealSuite` (new) tests the arithmetic of both faces.
   `AlteredFoundationSetupSuite` (new) tests the games the two faces start,
   Setup's walk and the first Search. No existing suite has altered-face
   setup as its subject; `GameStartRulesSuite` keeps only the refusal table.
9. **Docs.** The readings below go into the spec's slice 2 section. The
   spec leaves `docs/rules/ambiguities.md` to slice 7 for the whole batch,
   but its Docs list names only "rulings 1 to 3" for that file, so Task 4
   adds slice 2's readings to that list. `docs/ROADMAP.md` loses the words
   for finished work.

## Rulings needing the owner's review

Each is the most literal reading of the New Foundations text. The code and
tests below follow them; if the owner decides otherwise, the task named
changes.

1. **Every board starts with 6 favor under Movers and Shakers.** Card and
   rulebook: "Each player takes 5 more favor [...] from the shared bank.
   _Players usually start with 6._" Rulebook, Players: "Place 1 favor, 1
   secret, and 3 warbands on their board." Reading: 1 and 5 more, for every
   player whatever their role, the Chancellor included. Secrets, warbands
   and Supply do not change (Task 1).
2. **A lineage that stores three or more advisers draws none and keeps them
   all.** Rulebook: "Each player draws facedown denizens from the starting
   denizen stack until they have three facedown starting advisers."
   Reading: a player already holding three or more draws nothing, and
   nothing is discarded down to three at setup (Task 2, `SetupDeal.of`).
3. **Players draw in seat order, each all their cards at once.** The text
   gives no order. The stack is shuffled, so the order changes no
   probability (Task 2).
4. **A site-only denizen drawn is kept as a facedown adviser.** The text
   makes no exception: every denizen drawn becomes a starting adviser.
   Rulebook, Reveal Cards: "You may reveal any number of your relics and
   advisers, except site-only [...] advisers." So it stays facedown through
   Setup, as a site-only card chosen under Ties of Fellowship does today
   (Task 3).
5. **Choose Adviser is skipped and Reveal Cards still runs.** Rulebook,
   Choose Adviser: "If you drew denizens in the Ties of Fellowship
   foundation (IV), choose one of your drawn denizens [...]". Under Teeming
   World no one did. Reveal Cards names no foundation, so each player is
   asked it with their three advisers (Task 3).
6. **"As evenly as possible": an uneven rest leaves the Cradle pile, then
   the Provinces pile, one card more, and the last card dealt tops its
   pile.** Card: "Discard the rest of the starting denizen stack as evenly
   as possible, going from Cradle to Provinces to Hinterland." Reading: one
   card at a time in that order, starting with the Cradle, each card onto
   the top of its pile, facedown and known to no one (Task 2).
7. **A Search of the Visions-only deck draws one Vision and stops; once the
   five are drawn the world deck cannot be searched.** Rulebook, Search:
   "Draw them one at a time. **If you draw a Vision card from the world
   deck**, stop drawing cards and advance the Visions Drawn marker one
   space." The rulebook says nothing of an empty world deck, and names no
   reshuffle. Reading: nothing special. The engine already shows an empty
   world deck as a disabled source, "The world deck is empty", and the
   regional discard piles stay searchable (Task 3 pins the first Search; no
   code changes).
8. **A lineage's stored advisers come first, the drawn ones after.**
   Decided in the plan's review. Rulebook, Players: "Take its player board,
   warbands, and any legacies and starting advisers stored with it."
   Rulebook, private information: "You can't change the order of facedown
   advisers." Reading: a player takes their stored advisers at the Players
   step, before Foundation IV deals, so they lie first and the drawn cards
   follow in the order drawn. `LaterGameSetup` appended the stored advisers
   after whatever the materializer had placed, which would have put the
   drawn ones first; Task 3 turns that around.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/Setup.scala` | `FirstGameSetupPlan.foundations` and `altered`; `PawnPlacement` goes |
| `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala` | the II and IV refusals go |
| `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala` | rewritten: Foundation IV's deal as a value, both faces |
| `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` | reads the faces once; validates against the deal |
| `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` | Foundation II's boards and banks; deals through `SetupDeal` |
| `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala` | stored advisers come before drawn ones |
| `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` | one comment |
| `src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala`, `GeneratedFirstGamePlanFactory.scala` | deal through `SetupDeal` |
| `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala` | new: the games the two faces start |
| `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala` | new: the deal's arithmetic |
| `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala` | the refusal table shrinks |
| `src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala`, `FirstGameSetupMaterializerSuite.scala` | new signatures |
| `src/test/scala/distantlands/application/ChronicleFirstGamePlanSuite.scala`, `GeneratedFirstGamePlanFactorySuite.scala` | new signature; a Teeming World deal |
| `docs/architecture/game-setup.md`, `docs/rules/implementation-traceability.md`, `docs/ROADMAP.md`, the spec | docs |

---

### Task 1: Movers and Shakers

**Files:**
- Modify: `src/main/scala/distantlands/model/Setup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`

**Interfaces:**
- Consumes (slice 1): `StartingFoundations.of(chronicle):
  Map[FoundationNumber, FoundationState]`, `StartingFoundations.refusal`,
  `LaterGameFixture.chronicle`, `LaterGameFixture.seats`,
  `LaterGameFixture.orders(chronicle, firstPlayer, participants)`.
- Produces:
  - `FirstGameSetupPlan.foundations: Map[FoundationNumber, FoundationState]`
    (last field, default `Map.empty`) and
    `FirstGameSetupPlan.altered(number: FoundationNumber): Boolean`.
  - `GameStartRules.buildPlan` fills `foundations` from
    `StartingFoundations.of(chronicle)`, and `evolve` puts
    `plan.foundations` in `CampaignState.foundations`.
  - Game start accepts an altered Foundation II: boards start with 6 favor,
    and `ReadyGame.banks.favor(suit)` is the number of denizens of `suit` on
    the map, edifices included.
  - `AlteredFoundationSetupSuite` with the helpers `withAlteredFoundation`,
    `chronicleWithEmpire`, `firstGameChronicle`, `started` and `suitsOnMap`,
    which Task 3 extends.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** Game start under the two altered faces that only change setup, Movers
  * and Shakers (II) and Teeming World (IV) (Foundations design, slice 2).
  * Every test starts from a Chronicle: game start is the subject. */
class AlteredFoundationSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")

  /** `chronicle` with foundation `number` turned to its altered face. */
  private def withAlteredFoundation(chronicle: Chronicle,
      number: FoundationNumber): Chronicle =
    chronicle.copy(foundations = chronicle.foundations.updated(number,
      FoundationState(FoundationFace.Altered, Set.empty)))

  /** A later game's Chronicle for three seats: l1 is the Chancellor and l2
    * a Citizen, and each of the Empire's two sites stores a denizen. */
  private val chronicleWithEmpire =
    LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))

  /** The fixture's first-game Chronicle: no lineage listed and no Empire. */
  private val firstGameChronicle = FirstGameSetupFixture.chronicle

  /** The game `chronicle` starts for `seats`, p1 first, dealt from the
    * Chronicle's decks in their own order. */
  private def started(chronicle: Chronicle,
      seats: Vector[FirstGameParticipant] = FirstGameSetupFixture.participants)
      : ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1, seats)))

  /** The suit of every denizen on the map, edifices included. */
  private def suitsOnMap(ready: ReadyGame): Vector[Suit] =
    ready.game.current.map.sites.values.flatMap(_.denizens).flatMap {
      case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
      case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
    }.toVector

  test("Movers and Shakers gives every board 6 favor, and each bank 1 " +
      "favor per denizen of its suit on the map and no more"):
    val ready = started(
      withAlteredFoundation(chronicleWithEmpire, FoundationNumber.II))
    assertEquals(ready.game.current.players.map(_.board),
      Vector.fill(3)(PlayerBoardState(6, 1, 0, 3, SupplyTrack.full)))
    val onMap = suitsOnMap(ready)
    assert(onMap.size > 2, "the Empire's denizens are on the map")
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)

  test("with five players Powerful Tribes adds 1 favor to each bank, and " +
      "Movers and Shakers adds none"):
    val fiveSeats = LaterGameFixture.seats(5)
    val tribes = started(firstGameChronicle, fiveSeats)
    val movers = started(withAlteredFoundation(firstGameChronicle,
      FoundationNumber.II), fiveSeats)
    val onMap = suitsOnMap(movers)
    assert(onMap.nonEmpty, "the Homeland edifices are on the map")
    assertEquals(tribes.banks.favor,
      Suit.all.map(suit => suit -> (4 + onMap.count(_ == suit))).toMap)
    assertEquals(movers.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)
    assertEquals(tribes.game.current.players.map(_.board.favor),
      Vector.fill(5)(1))
    assertEquals(movers.game.current.players.map(_.board.favor),
      Vector.fill(5)(6))

  test("Movers and Shakers changes nothing else a game starts with"):
    val tribes = started(chronicleWithEmpire)
    val movers = started(
      withAlteredFoundation(chronicleWithEmpire, FoundationNumber.II))
    assertEquals(movers.game.current.commonCards,
      tribes.game.current.commonCards)
    assertEquals(movers.game.current.map, tribes.game.current.map)
    assertEquals(movers.game.current.temporaryHands,
      tribes.game.current.temporaryHands)
    assertEquals(movers.game.current.banners, tribes.game.current.banners)
```

`LaterGameFixture.chronicle(l1, l2)` makes l1 the Chancellor, so p1 must go
first; `LaterGameFixture.orders` deals the Chronicle's decks in their own
order. `LaterGameFixture.seats(5)` seats p1 to p5 on l1 to l5; the fixture's
first-game Chronicle lists no lineage, so all five are fresh Exiles.

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`,
the refusal table loses Foundation II's row, and the test slice 1 worded
"with several altered faces" names the first face still refused:

Replace

```scala
  test("game start refuses each altered face but Foundation III's, naming " +
      "the face and the slice it arrives with"):
```

with

```scala
  test("game start refuses each altered face no slice has brought in yet, " +
      "naming the face and the slice it arrives with"):
```

Delete

```scala
    assertEquals(refusalFor(FoundationNumber.II),
      unsupported("Movers and Shakers (altered Foundation II)", 2))
```

Then replace

```scala
  test("with several altered faces, game start names the first in " +
      "foundation order"):
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Movers and Shakers (altered " +
        "Foundation II) is not supported yet: it arrives with Foundations " +
        "slice 2"))
```

with

```scala
  test("game start accepts an altered Foundation II"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle.copy(
      foundations = Map(FoundationNumber.II -> altered)), orders))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.II), altered)

  test("with several altered faces, game start names the first refused one " +
      "in foundation order"):
    // II and III are accepted, so V is the first refused.
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered, FoundationNumber.V -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Grand Council (altered " +
        "Foundation V) is not supported yet: it arrives with Foundations " +
        "slice 5"))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.GameStartRulesSuite"`
Expected: five tests FAIL. The three of `AlteredFoundationSetupSuite` and
"game start accepts an altered Foundation II" report `expected a Right, got
Left(UnsupportedChronicle(Movers and Shakers (altered Foundation II) is not
supported yet: it arrives with Foundations slice 2))`. "with several
altered faces" obtains that same Movers and Shakers refusal where it
expects Grand Council's. The refusal-table test passes: it only lost a row.

- [ ] **Step 3: The plan carries the faces**

In `src/main/scala/distantlands/model/Setup.scala`:

Replace

```scala
    /** Each seated player's stored starting advisers (CR p. 11). */
    startingAdvisers: Map[PlayerId, Vector[AdviserState]] = Map.empty
)
```

with

```scala
    /** Each seated player's stored starting advisers (CR p. 11). */
    startingAdvisers: Map[PlayerId, Vector[AdviserState]] = Map.empty,
    /** The foundation faces the game starts with. A number the map does
      * not list lies normal. */
    foundations: Map[FoundationNumber, FoundationState] = Map.empty
):
  /** Whether foundation `number` starts on its altered face. */
  def altered(number: FoundationNumber): Boolean =
    foundations.get(number).exists(_.face == FoundationFace.Altered)
```

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`, the
faces are read once, in `buildPlan`, and `evolve` takes them from the plan:

Delete

```scala
      // The faces the Chronicle stores; `buildPlan` refuses the altered
      // ones no slice has brought in yet.
      foundations = StartingFoundations.of(chronicle)
```

Then replace

```scala
        CampaignState(atlas, foundations, lineages, chronicle.reliquary,
```

with

```scala
        // The faces `buildPlan` read from the Chronicle and did not refuse.
        CampaignState(atlas, plan.foundations, lineages, chronicle.reliquary,
```

Then replace

```scala
    val drawers = SetupDeal.drawers(chronicle, orders.participants)
    for
```

with

```scala
    val drawers = SetupDeal.drawers(chronicle, orders.participants)
    val faces = StartingFoundations.of(chronicle)
    for
```

Then replace

```scala
      _ <- StartingFoundations.refusal(StartingFoundations.of(chronicle))
```

with

```scala
      _ <- StartingFoundations.refusal(faces)
```

Then replace

```scala
        else atlasSites, startingAdvisers(chronicle, orders))
```

with

```scala
        else atlasSites, startingAdvisers(chronicle, orders), faces)
```

- [ ] **Step 4: Movers and Shakers at game start**

In `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`,
the refusal goes:

Replace

```scala
    FoundationNumber.I -> 3,
    FoundationNumber.II -> 2,
```

with

```scala
    FoundationNumber.I -> 3,
```

In `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`:

Replace

```scala
        PlayerBoardState(1, 1, 0, 3, SupplyTrack.full),
```

with

```scala
        PlayerBoardState(startingFavor(plan), 1, 0, 3, SupplyTrack.full),
```

Then replace

```scala
  /** Foundation II: 3 favor per suit, 1 more with 5 or more players, and 1
    * more per denizen of that suit on the map, edifices included. */
  private def favorBanks(plan: FirstGameSetupPlan, map: MapState)
      : Map[Suit, Int] =
    val bonus = if plan.participants.size >= 5 then 1 else 0
```

with

```scala
  /** The favor on each board: 1, and under Movers and Shakers (altered
    * Foundation II) 5 more from the shared bank, which has no count. */
  private def startingFavor(plan: FirstGameSetupPlan): Int =
    if plan.altered(FoundationNumber.II) then 6 else 1

  /** Foundation II. Powerful Tribes: 3 favor per suit, 1 more with 5 or
    * more players, and 1 more per denizen of that suit on the map,
    * edifices included. Movers and Shakers prints neither the 3 nor the
    * fifth-player favor: each bank holds 1 per denizen of its suit on the
    * map. */
  private def favorBanks(plan: FirstGameSetupPlan, map: MapState)
      : Map[Suit, Int] =
    val printed =
      if plan.altered(FoundationNumber.II) then 0
      else if plan.participants.size >= 5 then 4
      else 3
```

Then replace

```scala
    Suit.all.map(suit => suit -> (3 + bonus + suits.count(_ == suit))).toMap
```

with

```scala
    Suit.all.map(suit => suit -> (printed + suits.count(_ == suit))).toMap
```

- [ ] **Step 5: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/Setup.scala src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala
git commit -m "feat(foundations): set up Movers and Shakers" -m "Under an altered Foundation II every board starts with 6 favor, and
each suit's bank holds 1 favor per denizen of that suit on the map, with
no base 3 and no fifth-player favor. The setup plan carries the game's
foundation faces, read from the Chronicle once, and game start no longer
refuses the face." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

---

### Task 2: Foundation IV's deal in one place

A refactor with new arithmetic behind it. After this task a Ties of
Fellowship game starts exactly as before, and `SetupDeal` also knows how
Teeming World deals, which game start still refuses until Task 3.

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala` (rewritten)
- Modify: `src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala`
- Modify: `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Modify: `src/main/scala/distantlands/model/Setup.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/ChronicleFirstGamePlanSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/FirstGameSetupMaterializerSuite.scala`

**Interfaces:**
- Consumes: `StartingFoundations.of(chronicle)` (slice 1);
  `FirstGameSetupPlan` as Task 1 left it.
- Produces:
  - `SetupDeal(face: FoundationFace, draws: Vector[(PlayerId, Int)])`, a
    case class in `distantlands.gameplay.setup`, with
    - `needed: Int`, the fewest denizens the stack must hold;
    - `hand(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId]`,
      the cards a player chooses an adviser from (Ties of Fellowship only);
    - `kept(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId]`,
      the cards a player keeps as advisers at once (Teeming World only);
    - `discards(stack: Vector[DenizenId]): Map[Region, Vector[WorldCardId]]`,
      each pile in stored order, top last;
    - `deck(stack: Vector[DenizenId]): Vector[DenizenId]`, the denizens left
      for the world deck.
  - `SetupDeal.of(chronicle: Chronicle, participants:
    Vector[FirstGameParticipant]): SetupDeal` and
    `SetupDeal.firstGame(participants: Vector[FirstGameParticipant]):
    SetupDeal`.
  - `ChronicleFirstGamePlan.dealOrder(config, deal: SetupDeal, denizenOrder,
    relicOrder, mix)`; the 3-argument first-game overload is unchanged.
  - `FirstGameSetupMaterializer.materialize(plan: FirstGameSetupPlan, deal:
    SetupDeal): FirstGameSetupMaterial`.
  - `SetupDeal.drawers`, `SetupDeal.dealt`,
    `FirstGameSetupMaterializer.handFor` and `model.PawnPlacement` no longer
    exist.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.model._

/** Foundation IV's deal of the starting denizen stack, on both faces
  * (Foundations design, slice 2). */
class SetupDealSuite extends munit.FunSuite:
  private val seats = FirstGameSetupFixture.participants
  private val Vector(p1, p2, p3) = seats.map(_.playerId)
  private val stack = FirstGameSetupFixture.denizens

  /** The fixture's Chronicle with lineage l`n` storing `count` starting
    * advisers, taken from the end of its world deck, for each pair given,
    * and Foundation IV on `face`. */
  private def chronicleStoringAdvisers(face: FoundationFace,
      stored: (Int, Int)*): Chronicle =
    val base = FirstGameSetupFixture.chronicle
    val (deck, lineages) = stored.foldLeft(
        (base.worldDeck, Vector.empty[LineageState])) {
      case ((deck, lineages), (lineage, count)) =>
        (deck.dropRight(count), lineages :+ LineageState(
          LineageId(s"l$lineage"), None, Role.Exile, Vector.empty,
          deck.takeRight(count).map(DenizenState(_, Orientation.FaceDown,
            Tokens.empty))))
    }
    base.copy(worldDeck = deck, lineages = lineages, foundations =
      Map(FoundationNumber.IV -> FoundationState(face, Set.empty)))

  test("Ties of Fellowship seeds 2 denizens in each discard pile, deals a " +
      "hand of 3 to each player with no stored adviser, and leaves the rest " +
      "for the world deck"):
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Normal, 2 -> 1), seats)
    assertEquals(deal, SetupDeal(FoundationFace.Normal,
      Vector(p1 -> 3, p2 -> 0, p3 -> 3)))
    assertEquals(deal.needed, 12)
    assertEquals(deal.discards(stack), Map[Region, Vector[WorldCardId]](
      Region.Cradle -> stack.slice(0, 2),
      Region.Provinces -> stack.slice(2, 4),
      Region.Hinterland -> stack.slice(4, 6)))
    assertEquals(seats.map(seat => deal.hand(stack, seat.playerId)),
      Vector(stack.slice(6, 9), Vector.empty, stack.slice(9, 12)))
    assertEquals(seats.map(seat => deal.kept(stack, seat.playerId)),
      Vector.fill(3)(Vector.empty))
    assertEquals(deal.deck(stack), stack.drop(12))

  test("a first game's deal is Ties of Fellowship with every player drawing"):
    assertEquals(SetupDeal.firstGame(seats),
      SetupDeal.of(FirstGameSetupFixture.chronicle, seats))
    assertEquals(SetupDeal.firstGame(seats).needed, 15)

  test("under Teeming World each player draws until they hold three " +
      "starting advisers, and keeps every card drawn"):
    // l2 stores one adviser and l3 stores three.
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Altered, 2 -> 1, 3 -> 3), seats)
    assertEquals(deal, SetupDeal(FoundationFace.Altered,
      Vector(p1 -> 3, p2 -> 2, p3 -> 0)))
    assertEquals(deal.needed, 5)
    assertEquals(seats.map(seat => deal.kept(stack, seat.playerId)),
      Vector(stack.slice(0, 3), stack.slice(3, 5), Vector.empty))
    assertEquals(seats.map(seat => deal.hand(stack, seat.playerId)),
      Vector.fill(3)(Vector.empty))

  test("under Teeming World a lineage storing more than three advisers " +
      "draws none"):
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Altered, 1 -> 4), seats)
    assertEquals(deal.draws, Vector(p1 -> 0, p2 -> 3, p3 -> 3))

  test("Teeming World deals the rest of the stack to the Cradle, the " +
      "Provinces and the Hinterland in turn, the last card dealt on top, " +
      "and leaves the world deck no denizen"):
    val deal = SetupDeal(FoundationFace.Altered,
      Vector(p1 -> 3, p2 -> 3, p3 -> 3))
    // 9 drawn of 19 leaves 10: the Cradle takes the odd card.
    val nineteen = stack.take(19)
    assertEquals(deal.discards(nineteen), Map[Region, Vector[WorldCardId]](
      Region.Cradle -> Vector(9, 12, 15, 18).map(nineteen),
      Region.Provinces -> Vector(10, 13, 16).map(nineteen),
      Region.Hinterland -> Vector(11, 14, 17).map(nineteen)))
    assertEquals(deal.deck(nineteen), Vector.empty)

  test("a player the deal does not seat draws nothing"):
    val deal = SetupDeal.firstGame(seats.take(2))
    assertEquals(deal.hand(stack, p3), Vector.empty)
    assertEquals(SetupDeal(FoundationFace.Altered, Vector(p1 -> 3))
      .kept(stack, p3), Vector.empty)
```

In `src/test/scala/distantlands/application/ChronicleFirstGamePlanSuite.scala`,
the two tests that named a drawer count name a deal, and a third deals
Teeming World:

Replace

```scala
import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture}
```

with

```scala
import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture,
  SetupDeal}
```

Then replace

```scala
  test("a later game deals hands only to players with no starting advisers"):
    // 3 seats, one with a stored adviser: 6 discards and 2 hands of 3.
    val denizens = chronicle.worldDeck
    val orders = ChronicleFirstGamePlan.dealOrder(config, 2, denizens,
      chronicle.relicDeck, identity)
```

with

```scala
  private val Vector(p1, p2, p3) = config.participants.map(_.playerId)

  test("a later game deals hands only to players with no starting advisers"):
    // 3 seats, one with a stored adviser: 6 discards and 2 hands of 3.
    val denizens = chronicle.worldDeck
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal(FoundationFace.Normal, Vector(p1 -> 3, p2 -> 0, p3 -> 3)),
      denizens, chronicle.relicDeck, identity)
```

Then replace

```scala
    val orders = ChronicleFirstGamePlan.dealOrder(config, 3, short,
      chronicle.relicDeck, _.reverse)
    val rest: Vector[WorldCardId] = short.drop(15)
    assertEquals(orders.worldDeckOrder,
      (rest ++ FirstGameRulesData.visions).reverse)
```

with

```scala
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal.firstGame(config.participants), short, chronicle.relicDeck,
      _.reverse)
    val rest: Vector[WorldCardId] = short.drop(15)
    assertEquals(orders.worldDeckOrder,
      (rest ++ FirstGameRulesData.visions).reverse)

  test("a Teeming World deal leaves the world deck the five Visions, " +
      "mixed, and keeps the whole stack as the deal"):
    val orders = ChronicleFirstGamePlan.dealOrder(config,
      SetupDeal(FoundationFace.Altered, Vector(p1 -> 3, p2 -> 3, p3 -> 3)),
      chronicle.worldDeck, chronicle.relicDeck, _.reverse)
    assertEquals(orders.worldDeckOrder,
      (FirstGameRulesData.visions: Vector[WorldCardId]).reverse)
    assertEquals(orders.denizenOrder, chronicle.worldDeck)
```

In `src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala`:

Replace

```scala
      SetupDeal.drawers(chronicle, participants), chronicle.worldDeck,
```

with

```scala
      SetupDeal.of(chronicle, participants), chronicle.worldDeck,
```

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`:

Replace

```scala
      FirstGameBootstrapConfig(orders.participants, PlayerId("p1")), 3,
      short.worldDeck, short.relicDeck, _.reverse)
```

with

```scala
      FirstGameBootstrapConfig(orders.participants, PlayerId("p1")),
      SetupDeal.of(short, orders.participants), short.worldDeck,
      short.relicDeck, _.reverse)
```

In `src/test/scala/distantlands/gameplay/setup/FirstGameSetupMaterializerSuite.scala`:

Replace

```scala
    val material = materializer.materialize(plan, Vector.empty, Vector.empty)
```

with

```scala
    val material = materializer.materialize(plan,
      SetupDeal.firstGame(plan.participants))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupDealSuite"`
Expected: the test sources do not compile. The errors name `SetupDeal`'s
missing members (`of`, `firstGame`, `needed`, `hand`, `kept`, `discards`,
`deck`, and `SetupDeal` used as a constructor) and `materialize`'s
arguments. No test runs.

- [ ] **Step 3: The deal as a value**

Replace the whole of `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala` with:

```scala
package distantlands.gameplay.setup

import distantlands.model._

/** Foundation IV's deal of the starting denizen stack: the one arithmetic
  * the plan factory, game start and the materializer share (CR p. 11;
  * Foundations design, slice 2). `draws` lists every seated player in seat
  * order with the number of denizens they draw; `stack` below is always the
  * deal's `denizenOrder`.
  *
  * Ties of Fellowship, the normal face: the first 6 cards seed the discard
  * piles, 2 each, then each player with no starting adviser draws a hand of
  * 3 to choose one adviser from, and the rest of the stack goes under the
  * Visions as the world deck.
  *
  * Teeming World, the altered face: each player draws until they hold three
  * starting advisers and keeps every card drawn, then the whole rest of the
  * stack is dealt to the discard piles one card at a time, and the world
  * deck gets no denizen.
  */
final case class SetupDeal(face: FoundationFace,
    draws: Vector[(PlayerId, Int)]):
  private val teeming = face == FoundationFace.Altered
  /** Ties of Fellowship seeds each discard pile before the hands. */
  private val seeded = if teeming then 0 else 2 * Region.all.size
  private val drawn = draws.map(_._2).sum

  /** The fewest denizens the stack must hold for this deal. */
  def needed: Int = seeded + drawn

  /** The cards `player` draws, in the order drawn: players draw in seat
    * order, each all their cards at once. */
  private def drawnBy(stack: Vector[DenizenId], player: PlayerId)
      : Vector[DenizenId] =
    val seat = draws.indexWhere(_._1 == player)
    if seat < 0 then Vector.empty
    else
      val from = seeded + draws.take(seat).map(_._2).sum
      stack.slice(from, from + draws(seat)._2)

  /** The hand `player` chooses one adviser from in Setup: their draws under
    * Ties of Fellowship, nothing under Teeming World. */
  def hand(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId] =
    if teeming then Vector.empty else drawnBy(stack, player)

  /** The denizens `player` keeps as facedown advisers without a choice:
    * their draws under Teeming World, nothing under Ties of Fellowship. */
  def kept(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId] =
    if teeming then drawnBy(stack, player) else Vector.empty

  /** The discard piles the deal leaves, each in stored order, top last.
    * Teeming World deals the rest of the stack to the Cradle, the Provinces
    * and the Hinterland in turn, so the last card dealt tops its pile. */
  def discards(stack: Vector[DenizenId]): Map[Region, Vector[WorldCardId]] =
    val piles = Region.all.zipWithIndex
    if teeming then
      val rest = stack.drop(drawn).zipWithIndex
      piles.map((region, pile) => region -> rest.collect {
        case (card, dealt) if dealt % piles.size == pile => card
      }).toMap
    else piles.map((region, pile) =>
      region -> stack.slice(2 * pile, 2 * pile + 2)).toMap

  /** The denizens left for the world deck, in stack order. */
  def deck(stack: Vector[DenizenId]): Vector[DenizenId] =
    if teeming then Vector.empty else stack.drop(needed)

object SetupDeal:
  /** The deal of the game `chronicle` starts for `participants`, under the
    * Chronicle's Foundation IV face. A lineage's stored starting advisers
    * count toward Teeming World's three, and stop a Ties of Fellowship
    * hand. */
  def of(chronicle: Chronicle, participants: Vector[FirstGameParticipant])
      : SetupDeal =
    val stored = chronicle.lineages.map(lineage =>
      lineage.id -> lineage.startingAdvisers.size).toMap
    val face = StartingFoundations.of(chronicle)(FoundationNumber.IV).face
    SetupDeal(face, participants.map { participant =>
      val held = stored.getOrElse(participant.lineageId, 0)
      participant.playerId -> (face match
        case FoundationFace.Normal => if held == 0 then 3 else 0
        case FoundationFace.Altered => math.max(0, 3 - held))
    })

  /** A first game's deal: Ties of Fellowship, and every player draws. */
  def firstGame(participants: Vector[FirstGameParticipant]): SetupDeal =
    SetupDeal(FoundationFace.Normal, participants.map(_.playerId -> 3))
```

`piles.size` is 3. A Ties of Fellowship deal reads the stack as the code it
replaces did: cards 0 to 5 seed the piles, two each, and the hands follow in
seat order, a player with a stored adviser taking none.

- [ ] **Step 4: The plan factory deals through it**

In `src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala`:

Replace

```scala
 * packets, dealt after 6 cards for the seeded regional discards and 3 for
 * each player with no starting advisers are set aside (Foundation IV). A
 * deck of fewer than 25 denizens after the deal is instead shuffled with
 * every Vision by `mix` (CR p. 12). The caller shuffles `denizenOrder` and
```

with

```scala
 * packets, dealt after Foundation IV has taken its cards from the stack
 * (`SetupDeal`). A deck of fewer than 25 denizens after the deal is instead
 * shuffled with every Vision by `mix` (CR p. 12); Teeming World leaves no
 * denizen, so its deck is the five Visions, mixed. The caller shuffles
 * `denizenOrder` and
```

Then replace

```scala
    dealOrder(config, config.participants.size, denizenOrder, relicOrder,
      identity)

  def dealOrder(config: FirstGameBootstrapConfig, drawers: Int,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId],
      mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders =
    val remaining = denizenOrder.drop(SetupDeal.dealt(drawers))
```

with

```scala
    dealOrder(config, SetupDeal.firstGame(config.participants),
      denizenOrder, relicOrder, identity)

  def dealOrder(config: FirstGameBootstrapConfig, deal: SetupDeal,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId],
      mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders =
    val remaining = deal.deck(denizenOrder)
```

In `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`:

Replace

```scala
        seated, SetupDeal.drawers(chronicle, seated.participants),
```

with

```scala
        seated, SetupDeal.of(chronicle, seated.participants),
```

- [ ] **Step 5: Game start validates against it**

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
    for
      plan <- buildPlan(catalog, chronicle, orders)
      material = new FirstGameSetupMaterializer(catalog)
        .materialize(plan, Vector.empty, Vector.empty)
```

with

```scala
    val deal = SetupDeal.of(chronicle, orders.participants)
    for
      plan <- buildPlan(catalog, chronicle, orders, deal)
      material = new FirstGameSetupMaterializer(catalog)
        .materialize(plan, deal)
```

Then replace

```scala
  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders): Either[OathViolation, FirstGameSetupPlan] =
    val slots = 8 - chronicle.world.size
    val drawers = SetupDeal.drawers(chronicle, orders.participants)
```

with

```scala
  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders, deal: SetupDeal)
      : Either[OathViolation, FirstGameSetupPlan] =
    val slots = 8 - chronicle.world.size
```

Then replace

```scala
        chronicle.worldDeck.size >= SetupDeal.dealt(drawers), (),
```

with

```scala
        chronicle.worldDeck.size >= deal.needed, (),
```

Then replace

```scala
      _ <- validateDeal(chronicle, orders, drawers)
```

with

```scala
      _ <- validateDeal(chronicle, orders, deal)
```

Then replace

```scala
  /** The deal holds the Chronicle's decks, each card once, in any order; the
    * world deck below the dealt cards holds the rest of `denizenOrder`, in
    * order unless a short deck was mixed with the Visions (CR p. 12). */
  private def validateDeal(chronicle: Chronicle, orders: SetupOrders,
      drawers: Int): Either[OathViolation, Unit] =
    val rest = orders.denizenOrder.drop(SetupDeal.dealt(drawers))
```

with

```scala
  /** The deal holds the Chronicle's decks, each card once, in any order; the
    * world deck holds the denizens Foundation IV leaves of `denizenOrder`,
    * in order unless a short deck was mixed with the Visions (CR p. 12).
    * Teeming World leaves none. */
  private def validateDeal(chronicle: Chronicle, orders: SetupOrders,
      deal: SetupDeal): Either[OathViolation, Unit] =
    val rest = deal.deck(orders.denizenOrder)
```

- [ ] **Step 6: The materializer deals through it**

In `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`,
the two dead parameters go with everything that read them (plan-time
finding 3):

Replace

```scala
  def materialize(plan: FirstGameSetupPlan,
      placements: Vector[PawnPlacement],
      adviserChoices: Vector[(PlayerId, DenizenId)]): FirstGameSetupMaterial =
    val placementMap = placements.map(p => p.playerId -> p.siteId).toMap
    val adviserMap = adviserChoices.toMap
    val relicsBySite = assignRelics(plan)
```

with

```scala
  /** The table `plan` sets up, with Foundation IV's cards dealt as `deal`
    * says. No pawn is placed and no adviser chosen: Setup's walk does
    * both. */
  def materialize(plan: FirstGameSetupPlan, deal: SetupDeal)
      : FirstGameSetupMaterial =
    val relicsBySite = assignRelics(plan)
```

Then replace

```scala
      PlayerState(participant.playerId, participant.lineageId,
        placementMap.get(participant.playerId),
        PlayerBoardState(startingFavor(plan), 1, 0, 3, SupplyTrack.full),
        adviserMap.get(participant.playerId).toVector.map(id =>
          DenizenState(id, Orientation.FaceDown, Tokens.empty)),
```

with

```scala
      PlayerState(participant.playerId, participant.lineageId, None,
        PlayerBoardState(startingFavor(plan), 1, 0, 3, SupplyTrack.full),
        deal.kept(plan.denizenOrder, participant.playerId).map(id =>
          DenizenState(id, Orientation.FaceDown, Tokens.empty)),
```

Then replace

```scala
        initialDiscards(plan, placementMap, adviserMap)),
```

with

```scala
        deal.discards(plan.denizenOrder)),
```

Then replace

```scala
      temporaryHands = plan.participants.map(p =>
        p.playerId -> handFor(plan, p.playerId)).toMap)

  /** A player's 3 cards in Foundation IV, dealt in seat order to the
    * players with no starting advisers; none for a player with them. */
  def handFor(plan: FirstGameSetupPlan, playerId: PlayerId): Vector[DenizenId] =
    val index = plan.participants
      .filterNot(p => plan.startingAdvisers.contains(p.playerId))
      .indexWhere(_.playerId == playerId)
    if index < 0 then Vector.empty
    else plan.denizenOrder.slice(6 + index * 3, 9 + index * 3)
```

with

```scala
      temporaryHands = plan.participants.map(p => p.playerId ->
        deal.hand(plan.denizenOrder, p.playerId)).toMap)
```

Then delete `initialDiscards` and `regionOf`, both private and now unread:

Delete

```scala
  private def initialDiscards(plan: FirstGameSetupPlan,
      placements: Map[PlayerId, SiteId],
      choices: Map[PlayerId, DenizenId]): Map[Region, Vector[WorldCardId]] =
    val seeded = Map[Region, Vector[WorldCardId]](
      Region.Cradle -> plan.denizenOrder.slice(0, 2),
      Region.Provinces -> plan.denizenOrder.slice(2, 4),
      Region.Hinterland -> plan.denizenOrder.slice(4, 6))
    plan.participants.foldLeft(seeded) { (discards, participant) =>
      (placements.get(participant.playerId), choices.get(participant.playerId)) match
        case (Some(site), Some(choice)) =>
          val rejected = handFor(plan, participant.playerId).filterNot(_ == choice)
          val destination = regionOf(plan, site) match
            case Region.Cradle => Region.Provinces
            case Region.Provinces => Region.Hinterland
            case Region.Hinterland => Region.Cradle
          discards.updated(destination, discards(destination) ++ rejected)
        case _ => discards
    }

  private def regionOf(plan: FirstGameSetupPlan, site: SiteId): Region =
    if plan.orderedSites.take(2).contains(site) then Region.Cradle
    else if plan.orderedSites.slice(2, 5).contains(site) then Region.Provinces
    else Region.Hinterland

```

The block ends with the blank line after `regionOf`, so one blank line is
left between `edificesOn` and `startingFavor`.

In `src/main/scala/distantlands/model/Setup.scala`, `PawnPlacement` goes
with the blank line after it:

Delete

```scala
final case class PawnPlacement(playerId: PlayerId, siteId: SiteId)

```

Then replace

```scala
  * `denizenOrder` deals hands and the six seeded regional discards (CR pp.
  * 6-7); it never contains a Vision. `worldDeckOrder` is `denizenOrder`
  * with the five fixed Vision identities spliced into the 10+2/15+3
  * packets, and becomes the draw deck once dealing is done.
```

with

```scala
  * `denizenOrder` is the starting denizen stack Foundation IV deals from
  * (`SetupDeal`; CR pp. 6-7 and 11); it never contains a Vision.
  * `worldDeckOrder` is what that deal leaves of it, with the five fixed
  * Vision identities spliced into the 10+2/15+3 packets or mixed through a
  * short deck, and becomes the draw deck once dealing is done.
```

- [ ] **Step 7: Run the tests to make sure they pass**

Run: `./sbtw "Test/compile"`
Expected: success. Every caller of the old signatures is in this task; a
compile error names one this plan missed, and it takes the same change as
its neighbours above.

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.ChronicleFirstGamePlanSuite distantlands.application.GeneratedFirstGamePlanFactorySuite distantlands.application.LaterGameSetupChoicesSuite"`
Expected: PASS. `GameStartRulesSuite`'s "setup deals from the recorded
deal" and `LaterGameSetupSuite`'s "a seated lineage takes its starting
advisers facedown and draws no hand" pass unchanged: they pin that a Ties
of Fellowship deal reads the stack as before.

Run: `git grep -n "PawnPlacement\|handFor\|SetupDeal.drawers\|SetupDeal.dealt" -- src shared frontend`
Expected: no output.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/SetupDeal.scala src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala src/main/scala/distantlands/model/Setup.scala src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala src/test/scala/distantlands/application/ChronicleFirstGamePlanSuite.scala src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/gameplay/setup/FirstGameSetupMaterializerSuite.scala
git commit -m "refactor(setup): hold Foundation IV's deal in one value" -m "SetupDeal says how the starting denizen stack is dealt on either face of
Foundation IV: the hands, the advisers kept at once, the discard piles
and what is left for the world deck. The plan factory, game start's
validation and the materializer all read it, where each worked the deal
out for itself. The materializer loses two parameters no caller filled.
Teeming World's arithmetic is in place; game start still refuses the
face." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

---

### Task 3: Teeming World starts a game

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (a comment)
- Test: `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`

**Interfaces:**
- Consumes:
  - Task 2: `SetupDeal.of`, which `GameStartRules`, the plan factory and
    `LaterGameFixture.orders` already deal through, on either face.
  - Task 1: `AlteredFoundationSetupSuite`'s `withAlteredFoundation(chronicle,
    number)`, `chronicleWithEmpire`, `firstGameChronicle`,
    `started(chronicle, seats)`, `suitsOnMap(ready)`, `catalog` and `p1`.
  - Test kit: `Situation.wake(driver, chronicle, orders)`,
    `Situation.rules(catalog).withAnswers(answers)`, `Answers`, `Park`,
    `Outcomes.ready(state)`, `SearchPlay.searched(rules, state, actor,
    pile)`, `SetupProcedure.pawnDecisionId`, `revealDecisionId`,
    `FirstGameRulesData.visions`.
- Produces: game start accepts an altered Foundation IV, and a player's
  stored starting advisers come before the advisers Teeming World deals
  them (ruling 8). No new production name: the task deletes the refusal
  that hid Task 2's arithmetic and turns one concatenation around.

- [ ] **Step 1: Write the failing tests**

In `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`:

Replace

```scala
import distantlands.model._
import distantlands.testkit.Unwrap.accepted
```

with

```scala
import distantlands.catalog.SiteOnly
import distantlands.gameplay.{OathRules, SearchPlay}
import distantlands.model._
import distantlands.testkit.{Answers, Outcomes, Situation}
import distantlands.testkit.Unwrap.{accepted, as, rejected}
```

Append to the end of `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`,
after a blank line and inside the class (two spaces of indentation, as
written):

```scala
  private val (p2, p3) = (PlayerId("p2"), PlayerId("p3"))

  /** The first-game Chronicle with Foundation IV on Teeming World. */
  private val teemingWorldChronicle =
    withAlteredFoundation(firstGameChronicle, FoundationNumber.IV)

  private def faceDown(denizens: Vector[DenizenId]): Vector[AdviserState] =
    denizens.map(DenizenState(_, Orientation.FaceDown, Tokens.empty))

  private def advisers(ready: ReadyGame, player: PlayerId)
      : Vector[AdviserState] = ready.game.current.player(player).get.advisers

  test("under Teeming World each player with no stored adviser starts with " +
      "three facedown advisers from the top of the stack, and no hand"):
    val ready = started(teemingWorldChronicle)
    val stack = firstGameChronicle.worldDeck
    assertEquals(Vector(p1, p2, p3).map(advisers(ready, _)), Vector(
      faceDown(stack.slice(0, 3)), faceDown(stack.slice(3, 6)),
      faceDown(stack.slice(6, 9))))
    assertEquals(ready.game.current.temporaryHands.values.flatten.toVector,
      Vector.empty)

  /** The adviser l2 stores in `teemingWorldChronicleWithStoredAdviser`:
    * the last card of `chronicleWithEmpire`'s world deck. */
  private val storedAdviser = DenizenState(
    chronicleWithEmpire.worldDeck.last, Orientation.FaceUp, Tokens.empty)

  /** `chronicleWithEmpire` with Foundation IV on Teeming World and l2
    * storing `storedAdviser`, which leaves the world deck. */
  private val teemingWorldChronicleWithStoredAdviser = withAlteredFoundation(
    chronicleWithEmpire.copy(worldDeck = chronicleWithEmpire.worldDeck.init,
      lineages = chronicleWithEmpire.lineages.map(lineage =>
        if lineage.id == LineageId("l2") then
          lineage.copy(startingAdvisers = Vector(storedAdviser))
        else lineage)), FoundationNumber.IV)

  test("under Teeming World a lineage with one stored adviser draws two " +
      "and holds all three facedown, the stored one first"):
    val ready = started(teemingWorldChronicleWithStoredAdviser)
    val stack = teemingWorldChronicleWithStoredAdviser.worldDeck
    assertEquals(advisers(ready, p1), faceDown(stack.slice(0, 3)))
    // A facedown adviser's place cannot change, and the stored one was
    // taken before Foundation IV dealt the other two (ruling 8).
    assertEquals(advisers(ready, p2),
      faceDown(storedAdviser.id +: stack.slice(3, 5)))
    assertEquals(advisers(ready, p3), faceDown(stack.slice(5, 8)))
    assertEquals(ready.game.campaign.lineages(LineageId("l2"))
      .startingAdvisers, Vector.empty)

  test("Teeming World deals the rest of the stack to the Cradle, the " +
      "Provinces and the Hinterland discard piles in turn, with no pile " +
      "seeded"):
    val ready = started(teemingWorldChronicle)
    val rest = firstGameChronicle.worldDeck.drop(9)
    def everyThird(from: Int): Vector[WorldCardId] =
      rest.indices.filter(_ % 3 == from).map(rest).toVector
    assertEquals(rest.size, 51)
    assertEquals(ready.game.current.commonCards.regionalDiscards, Map(
      Region.Cradle -> everyThird(0), Region.Provinces -> everyThird(1),
      Region.Hinterland -> everyThird(2)))
    // A pile's top is its last card: the last three dealt top the piles.
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).last), rest.takeRight(3))

  test("a Teeming World stack that does not divide by three leaves the " +
      "Cradle pile the odd card"):
    // 57 denizens less the 8 drawn leave 49.
    val ready = started(teemingWorldChronicleWithStoredAdviser)
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).size), Vector(17, 16, 16))

  test("under Teeming World the world deck is the five Visions alone"):
    val ready = started(teemingWorldChronicle)
    assertEquals(ready.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions: Vector[WorldCardId])

  test("a Teeming World game holds every card once"):
    Vector(teemingWorldChronicle, teemingWorldChronicleWithStoredAdviser)
        .foreach { chronicle =>
      val ready = started(chronicle)
      assert(ready.game.cardIndex.isRight, ready.game.cardIndex)
      // Every denizen of the stack and every stored adviser is an adviser
      // or in a discard pile.
      val stored = chronicle.lineages.flatMap(_.startingAdvisers).map(_.id)
      val held = ready.game.current.players.flatMap(_.advisers.map(_.id)) ++
        Region.all.flatMap(ready.game.current.commonCards.discard)
      assertEquals(held.map(_.value).sorted,
        (chronicle.worldDeck ++ stored).map(_.value).sorted)
    }

  test("Teeming World refuses a stack too short for every player's draws"):
    val short = teemingWorldChronicle.copy(
      worldDeck = firstGameChronicle.worldDeck.take(8))
    assertEquals(rejected(GameStartRules.evolve(catalog, short,
        LaterGameFixture.orders(short, p1))),
      OathViolation.UnsupportedChronicle("not enough world-deck cards to deal"))

  test("Movers and Shakers and Teeming World together each set up their " +
      "own part"):
    val ready = started(withAlteredFoundation(
      teemingWorldChronicleWithStoredAdviser, FoundationNumber.II))
    assertEquals(ready.game.current.players.map(_.board.favor),
      Vector(6, 6, 6))
    val onMap = suitsOnMap(ready)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)
    assertEquals(ready.game.current.players.map(_.advisers.size),
      Vector(3, 3, 3))
    assertEquals(ready.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions: Vector[WorldCardId])

  /** Every decision id the walk parked on, in order, while it answers each
    * by default. */
  private def decisionsAskedDuringSetup(chronicle: Chronicle): Vector[String] =
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    Situation.wake(Situation.rules(catalog).withAnswers(recording), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    asked

  test("under Teeming World Setup asks each player for a pawn site and a " +
      "reveal, and no one to choose an adviser"):
    val asked = decisionsAskedDuringSetup(teemingWorldChronicle)
    assertEquals(asked, Vector(p1, p2, p3).flatMap(player => Vector(
      SetupProcedure.pawnDecisionId(player),
      SetupProcedure.revealDecisionId(player))))

  test("under Teeming World Reveal Cards offers a player's three facedown " +
      "advisers, and Setup ends with each player holding three"):
    var offered = Vector.empty[Int]
    val countingOffersAndRevealingNothing: Answers =
      case park if park.decisionId ==
          SetupProcedure.revealDecisionId(park.awaiting) =>
        offered :+= as[DecisionQuery.ChooseMany](park.decide.query)
          .options.size
        DecisionAnswer.ChooseManyAnswer(Vector.empty)
    val woken = Situation.wake(Situation.rules(catalog)
      .withAnswers(countingOffersAndRevealingNothing), teemingWorldChronicle,
      LaterGameFixture.orders(teemingWorldChronicle, p1))
    val siteOnly = Vector(p1, p2, p3).map(player =>
      advisers(woken.ready, player).count {
        case DenizenState(id, _, _) =>
          catalog.denizen(id).exists(_.isInstanceOf[SiteOnly])
        case _ => false
      })
    assertEquals(offered, siteOnly.map(3 - _))
    assertEquals(woken.ready.game.current.players.map(_.advisers.size),
      Vector(3, 3, 3))
    assertNotEquals(woken.ready.game.current.turn.phase, Phase.Setup)

  test("the first Search of a Teeming World game draws a Vision from the " +
      "world deck and stops"):
    val woken = Situation.wake(Situation.rules(catalog), teemingWorldChronicle,
      LaterGameFixture.orders(teemingWorldChronicle, p1)).endingWake(p1)
    val searched = Outcomes.ready(accepted(SearchPlay.searched(
      new OathRules(catalog), woken.state, p1, SearchSource.WorldDeck)).state)
    assertEquals(searched.game.current.temporaryHands(p1),
      Vector[WorldCardId](FirstGameRulesData.visions.head))
    assertEquals(searched.game.current.tracks.visionsDrawn, 1)
    assertEquals(searched.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions.tail: Vector[WorldCardId])
```

The fixture's world deck holds 60 denizens. A first game of three under
Teeming World draws 9 and deals 51, 17 to each pile. `chronicleWithEmpire`
moved 2 denizens to the Empire's sites and
`teemingWorldChronicleWithStoredAdviser` stores 1, so its stack holds 57:
p1 draws 3, p2 draws 2, p3 draws 3, and 49 are dealt. p2's stored adviser
lies first and the two drawn follow (ruling 8). `LaterGameFixture.orders`
mixes with `identity`, so the world deck is the five Visions in
`FirstGameRulesData.visions`' order. "Movers and Shakers and Teeming World
together each set up their own part" turns Foundation II on that same
Chronicle, so both altered faces set up one game. `recording` in
`decisionsAskedDuringSetup` never answers: its guard notes the decision and
fails, so `Situation.defaultAnswer` answers every park.

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`,
the refusal table loses Foundation IV's row:

Delete

```scala
    assertEquals(refusalFor(FoundationNumber.IV),
      unsupported("Teeming World (altered Foundation IV)", 2))
```

Then replace

```scala
    assertEquals(ready.game.campaign.foundations(FoundationNumber.II), altered)
```

with

```scala
    assertEquals(ready.game.campaign.foundations(FoundationNumber.II), altered)

  test("game start accepts an altered Foundation IV dealt as Teeming World " +
      "deals, and refuses it dealt as Ties of Fellowship deals"):
    val teemingChronicle = chronicle.copy(
      foundations = Map(FoundationNumber.IV -> altered))
    val ready = accepted(GameStartRules.evolve(catalog, teemingChronicle,
      LaterGameFixture.orders(teemingChronicle, orders.firstPlayer)))
    assertEquals(ready.game.campaign.foundations(FoundationNumber.IV), altered)
    // `orders` leaves 45 denizens in the world deck.
    assertEquals(rejected(GameStartRules.evolve(catalog, teemingChronicle,
        orders)),
      OathViolation.UnsupportedChronicle(
        "the world deck is not the rest of the deal"))
```

In `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules}
```

with

```scala
import distantlands.gameplay.setup.{FirstGameRulesData, FirstGameSetupFixture,
  GameStartRules}
```

Then replace

```scala
  private def lineage(id: String, role: Role) =
```

with

```scala
  test("a Teeming World Chronicle is dealt a world deck of the five " +
      "Visions, shuffled, and the game starts from that deal"):
    val saved = FirstGameSetupFixture.chronicle.copy(foundations = Map(
      FoundationNumber.IV -> FoundationState(FoundationFace.Altered,
        Set.empty)))
    val plan = accepted(new GeneratedFirstGamePlanFactory(catalog,
      ChronicleShuffles.reversing).build(config, ChronicleChoice.Saved(saved)))
    assertEquals(plan.orders.worldDeckOrder,
      (FirstGameRulesData.visions: Vector[WorldCardId]).reverse)
    val current = accepted(GameStartRules.evolve(catalog, plan.chronicle,
      plan.orders)).game.current
    assertEquals(current.commonCards.worldDeck, plan.orders.worldDeckOrder)
    assertEquals(current.players.map(_.advisers.size), Vector(3, 3, 3))

  private def lineage(id: String, role: Role) =
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.AlteredFoundationSetupSuite distantlands.gameplay.setup.GameStartRulesSuite distantlands.application.GeneratedFirstGamePlanFactorySuite"`
Expected: thirteen tests FAIL. The eleven new tests of
`AlteredFoundationSetupSuite`, "game start accepts an altered Foundation
IV" and the plan factory's test meet `UnsupportedChronicle(Teeming World
(altered Foundation IV) is not supported yet: it arrives with Foundations
slice 2)`. The factory's test fails at game start, after its world-deck
assertion has passed: Task 2 already deals the face.

- [ ] **Step 3: Stop refusing the face; stored advisers first**

In `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

Replace

```scala
    FoundationNumber.I -> 3,
    FoundationNumber.IV -> 2,
```

with

```scala
    FoundationNumber.I -> 3,
```

In `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`, a
lineage's stored advisers go before the advisers the materializer dealt
(ruling 8). Under Ties of Fellowship the materializer deals none, so
nothing changes there:

Replace

```scala
    * record, so the card index holds each card once (CR p. 11). */
```

with

```scala
    * record, so the card index holds each card once (CR p. 11). They come
    * before any adviser Teeming World dealt the player: a player takes
    * their stored advisers before Foundation IV, and the order of facedown
    * advisers cannot change. */
```

Then replace

```scala
        state => state.copy(advisers = state.advisers ++ plan.startingAdvisers
          .getOrElse(state.player, Vector.empty).map(faceDown)))))
```

with

```scala
        state => state.copy(advisers = plan.startingAdvisers
          .getOrElse(state.player, Vector.empty).map(faceDown) ++
          state.advisers))))
```

In `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`, the
walk is right as it stands (plan-time finding 4); its comment says why:

Replace

```scala
      // A player with starting advisers drew no hand (CR p. 11).
```

with

```scala
      // A player with starting advisers drew no hand (CR p. 11), and under
      // Teeming World no one did: every card drawn is already an adviser.
```

- [ ] **Step 4: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.GeneratedFirstGamePlanFactorySuite distantlands.application.ChronicleFirstGamePlanSuite distantlands.application.LaterGameSetupChoicesSuite"`
Expected: PASS. `LaterGameSetupSuite`'s "a seated lineage takes its starting
advisers facedown and draws no hand" passes unchanged: under Ties of
Fellowship a stored adviser is the player's only one.

If "under Teeming World Setup asks each player for a pawn site and a
reveal" lists a reveal fewer for some player, that player drew three
site-only denizens and was rightly asked nothing; the fixture's first nine
denizens do not do that at `92fb2e773`. Do not change the walk: compute the
expected list from the advisers, as the next test does.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala
git commit -m "feat(foundations): set up Teeming World" -m "Under an altered Foundation IV each player draws until they hold three
facedown starting advisers and keeps them all, a stored adviser before
the ones drawn, so Setup asks no adviser choice. The rest of the starting stack is dealt one card at a time to the
Cradle, Provinces and Hinterland discard piles, and the world deck is the
five Visions. Game start no longer refuses the face." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

---

### Task 4: Docs and verification

**Files:**
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

The spec leaves the batch's rulings in `docs/rules/ambiguities.md`,
`docs/rules/chronicle.md` and `CONTEXT.md` to slice 7. This task corrects
only what slice 2 made false, and records its readings in the spec.

- [ ] **Step 1: The setup architecture doc**

In `docs/architecture/game-setup.md`:

Replace

~~~markdown
Setup deals from these orders, never from the Chronicle's deck order: a
Chronicle is shown to players, so its order must not be the deal.
~~~

with

~~~markdown
Setup deals from these orders, never from the Chronicle's deck order: a
Chronicle is shown to players, so its order must not be the deal.

Foundation IV decides how `denizenOrder` is dealt, and `SetupDeal` holds that
arithmetic for the plan factory, game start's validation and the
materializer. Under Ties of Fellowship the first 6 cards seed the regional
discards, each player with no stored adviser draws a hand of 3, and the rest
goes under the Visions as the world deck. Under Teeming World each player
draws until they hold three starting advisers and keeps every card drawn
facedown, after any adviser their lineage stored, with no hand and no
adviser choice; the rest of the stack is dealt one card at a time to the
Cradle, Provinces and Hinterland discard piles; and the world deck is the
five Visions, shuffled. Foundation II decides the favor: Powerful Tribes
gives each bank 3, 1 more with five or more players, and 1 per denizen of
its suit on the map; Movers and Shakers gives each board 6 favor and each
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled.
~~~

- [ ] **Step 2: The traceability table**

In `docs/rules/implementation-traceability.md`, the foundations row, one
long line, stops being Blocked:

Replace

~~~markdown
| Six foundation setup effects and world-deck construction | **Blocked** | [CR pp. 10-12](rules-reference.md#later-games); mutable foundations [NF p. 9](new-foundations-delta.md#campaign-structure) | Foundation face/source state and reviewed catalog records exist; no typed handler executes a foundation. | Executable Foundation handlers B2; catalog text must not be mistaken for behavior. |
~~~

with

~~~markdown
| Six foundation setup effects and world-deck construction | **Partial** | [CR pp. 10-12](rules-reference.md#later-games); mutable foundations [NF p. 9](new-foundations-delta.md#campaign-structure) | Game start reads the Chronicle's faces (`StartingFoundations`) and builds both faces of Foundation II and of Foundation IV directly. Movers and Shakers gives each board 6 favor and each bank 1 favor per denizen of its suit on the map (`FirstGameSetupMaterializer`). `SetupDeal` deals Ties of Fellowship's seeded discards and hands, or Teeming World's kept advisers, its discard piles dealt in turn and its Visions-only world deck (`gameplay/setup/SetupDealSuite.scala`, `AlteredFoundationSetupSuite.scala`). | Altered Foundations I, V and VI are refused at game start until their slices of the [Foundations design](../superpowers/specs/2026-11-11-foundations-design.md). Imperial Maps always fills from Recent Sites ([R27](ambiguities.md#empire-rulings)). Foundation III's faces wait for Legacies. |
~~~

Then, in the row "Starting adviser choice, pawn placement, and Reveal
Cards", replace

~~~markdown
is not asked to choose one (`application/LaterGameSetupChoicesSuite.scala`, golden `later-game-setup`).
~~~

with

~~~markdown
is not asked to choose one (`application/LaterGameSetupChoicesSuite.scala`, golden `later-game-setup`). Under Teeming World no player is asked to choose one, and Reveal Cards offers each player the facedown advisers they drew (`gameplay/setup/AlteredFoundationSetupSuite.scala`).
~~~

- [ ] **Step 3: The roadmap**

In `docs/ROADMAP.md`, the Foundations item loses the two faces this slice
finished and the sentence slice 1 finished. The roadmap deletes finished
work outright, with no checked box and no "landed" note. If the item's
wording has changed since `92fb2e773`, delete only the words that name
finished work:

Replace

~~~markdown
- [ ] **Foundations.** Game start uses the Chronicle's stored faces, and
  every face but Foundation III's works: Movers and Shakers, Teeming World,
  Imperial Maps' choice of Recent or Forgotten, Wide Horizons with its
  Explore action and Setup With No Empire, and the Beacon's Shape Foundation.
~~~

with

~~~markdown
- [ ] **Foundations.** Every face but Foundation III's works: Imperial
  Maps' choice of Recent or Forgotten, Wide Horizons with its Explore action
  and Setup With No Empire, and the Beacon's Shape Foundation.
~~~

- [ ] **Step 4: The spec**

In `docs/superpowers/specs/2026-11-11-foundations-design.md`, the status
line, the sentence plan-time finding 1 corrects, the readings, and a
pointer for slice 7:

Replace

~~~markdown
**Status:** designed 2026-11-11. Slice 1 landed.
~~~

with

~~~markdown
**Status:** designed 2026-11-11. Slices 1 and 2 landed.
~~~

Then replace

~~~markdown
  through the existing short-deck mix (fewer than 25 denizens).
  `SetupDeal.dealt` and the deal validation learn the face.
~~~

with

~~~markdown
  through the existing short-deck mix (fewer than 25 denizens).
  `SetupDeal` holds both faces' arithmetic for the plan factory, the deal
  validation and the materializer.
- **Readings taken with the plan,** for the owner to confirm. Under Movers
  and Shakers every board starts with 6 favor, the Chancellor's included.
  Under Teeming World a lineage that stores three or more advisers draws
  none and keeps them all. Players draw in seat order, and a lineage's
  stored advisers come before the ones it draws. A site-only denizen drawn
  is kept and cannot be revealed in Setup. Reveal Cards still runs. An
  uneven rest leaves the Cradle pile, then the Provinces pile, one card
  more, and the last card dealt tops its pile. A Search of the Visions-only
  deck draws one Vision and stops; once the five are drawn the world deck is
  empty and cannot be searched.
~~~

Then, in the spec's last section, "Docs", point slice 7 at those readings.
Its list sends only "rulings 1 to 3" to `docs/rules/ambiguities.md`; slice
7 must move slice 2's readings there too, as the owner confirms or changes
them:

Replace

~~~markdown
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  and R27 closed) and `docs/rules/implementation-traceability.md`.
~~~

with

~~~markdown
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 3,
  slice 2's readings, and R27 closed) and
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
Expected: PASS. This slice changes no frontend or shared source.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`; every Scala file stays under 800
lines.

Run: `git status --short src/test/resources`
Expected: clean. No Game Log golden and no note-key snapshot changes in
this slice.

Run: `git grep -n "FoundationNumber.II -> 2\|FoundationNumber.IV -> 2" -- src/main`
Expected: no output. Neither face is still refused.

- [ ] **Step 6: Commit**

```bash
git add docs/architecture/game-setup.md docs/rules/implementation-traceability.md docs/ROADMAP.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "docs(foundations): record slice 2, the setup-only faces" -m "The setup doc says how each face of Foundations II and IV deals favor and
the starting stack, the traceability table stops calling the foundations
blocked, the roadmap drops the finished faces, and the spec records
slice 2 landed with the readings its plan took, which slice 7 moves to
the ambiguities doc." -m "Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
```

---

## Self-review

- **Spec coverage, slice 2.** Movers and Shakers: each player takes 5 more
  favor, and each bank gets 1 favor per denizen of its suit on the map,
  edifices included, with no base 3 and no fifth-player favor; its refusal
  goes (Task 1). Teeming World: each player draws until they hold three
  facedown starting advisers and keeps them, with no hand and no Choose
  Adviser step; the rest of the stack is dealt in turn to the three discard
  piles with nothing seeded; the world deck is the five Visions through the
  short-deck mix; the deal validation knows the face; its refusal goes
  (Tasks 2 and 3). Nothing is journaled. The spec's tests: bank and board
  totals at 3 and 5 players (Task 1); Teeming World with and without stored
  advisers, the discard piles' order, a world deck of Visions only, a first
  Search drawing a Vision, and card accounting holding every card once
  (Task 3, with the arithmetic in Task 2's `SetupDealSuite`). One test
  turns both faces in one game (Task 3).
- **Review changes.** A lineage's stored advisers come before the drawn
  ones (ruling 8; `LaterGameSetup`, Task 3). Ruling 2 no longer speaks of a
  stored Vision: one would duplicate a Vision of the world deck, so the case
  cannot arise, and `SetupDeal.of` simply counts what a lineage stores. The
  suite's Chronicle helpers are named as Chronicles.
- **The seam slice 1 left.** `GameStartRulesSuite`'s "with several altered
  faces" named Foundation II; Task 1 rewrites it to expect Grand Council,
  the first face still refused.
- **Seams for later slices.** Slices 3, 5 and 6 each still delete their own
  line of `StartingFoundations.arrivesWith` and their row of the refusal
  test; slice 5 must rewrite "with several altered faces" again, since it
  expects Foundation V's refusal. `FirstGameSetupPlan.foundations` and
  `altered` are where the materializer reads a face: slice 5 reads V for the
  People's Favor and slice 6 reads VI for the Darkest Secret. Slice 3's
  erosion changes I and V after the Empire is resolved, so it sets
  `foundations` in `buildPlan` below `EmpireSetupRules.resolve`; the
  campaign already reads `plan.foundations`. `SetupDeal.of` reads the
  Chronicle's Foundation IV through `StartingFoundations.of(chronicle)`;
  erosion never touches IV, so if slice 3 gives `of` another parameter,
  `SetupDeal.of` keeps reading the Chronicle's own face. Slice 3 rebuilds
  the map's region vectors, which this slice does not touch. Slice 4's
  Imperial Maps fill adds 1 favor per denizen it places on either face of
  Foundation II, which `favorBanks` already counts for the sites placed at
  game start. Slice 7 writes the batch's rulings into
  `docs/rules/ambiguities.md`; the spec's Docs list named only "rulings 1
  to 3", so Task 4 adds slice 2's readings to that list, and slice 7 must
  move them with whatever the owner decided on each.
- **Placeholders.** None.
- **Verified against a prototype.** Every block here was cut from a scratch
  worktree built on `0286b8462`, whose code is `92fb2e773`'s, and applying
  the blocks to that commit in plan order reproduces the worktree file for
  file (20 files). There, after the review's changes, the gates of Task 4
  passed: 5697 JVM tests, 924 frontend tests, statement coverage 95.25%,
  branch coverage 89.47%, the architecture check, and no change under
  `src/test/resources`. The red steps of Tasks 1 and 3 were observed: five
  failures and thirteen, as their steps say; Task 1's was observed before
  the review renamed its helpers. Task 2's red step is a compile failure
  and was read from the code, not run. If a red step
  fails for another reason, trust the run and say so in the task's report.
  Not checked in a browser: no frontend or projection source changes, and a
  player's own facedown advisers were already shown to them.
- **Type consistency.** `FirstGameSetupPlan.foundations`, `altered(number)`;
  `SetupDeal(face, draws)`, `needed`, `hand(stack, player)`, `kept(stack,
  player)`, `discards(stack)`, `deck(stack)`; `SetupDeal.of(chronicle,
  participants)`, `SetupDeal.firstGame(participants)`;
  `ChronicleFirstGamePlan.dealOrder(config, deal, denizenOrder, relicOrder,
  mix)`; `FirstGameSetupMaterializer.materialize(plan, deal)`;
  `GameStartRules.buildPlan(catalog, chronicle, orders, deal)` and
  `validateDeal(chronicle, orders, deal)`; the suite helpers
  `withAlteredFoundation`, `chronicleWithEmpire`, `firstGameChronicle`,
  `started`, `suitsOnMap`, `faceDown`, `advisers`, `teemingWorldChronicle`,
  `storedAdviser`, `teemingWorldChronicleWithStoredAdviser`,
  `decisionsAskedDuringSetup` and `chronicleStoringAdvisers`. Each is used
  with the same name and shape in every task that reads it.
