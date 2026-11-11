# Foundations, Slice 1: Faces Reach the Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A game starts with the foundation faces its Chronicle stores, the
Chronicle a game journals carries the faces the game ended with, and both
the table and the Chronicle page show the six foundation setup cards, read
only. An altered Foundation I, II, IV, V or VI is refused at game start by
name until its own slice lands; Foundation III is accepted on either face
and does nothing.

**Architecture:** A new catalog listing, `catalog.FoundationCards`, names
the twelve faces, gives each a power id (`foundation.i.wide-horizons`) and
holds its printed text. `gameplay.setup.StartingFoundations` reads the
Chronicle's faces for `GameStartRules` and holds the one table of refused
faces, a line per later slice. `ChronicleRecord` writes
`campaign.foundations` back. `application.FoundationPresentation` turns the
faces into `GameProjection.foundations` and `ChronicleSummary.foundations`,
and the frontend's `FoundationStrip` draws both.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Foundations design](../specs/2026-11-11-foundations-design.md),
sections "Rulings", "Approach" and "Slice 1: faces reach the game". Later
slices are read only for the seams they need from this one.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `gameplay` must not read a catalog `.text` or `rulesText`
  (`scripts/check-architecture.py`). Only `application` reads a foundation
  face's text. `catalog` imports nothing but `model`.
- The frontend and the shared protocol reference no package but `frontend`
  and `protocol` (`scripts/check-architecture.py`). A decoder in
  `shared/src/main` never calls ujson's raw string accessor; it reads
  strings through `ProjectionCodecSupport.string`.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- TDD: write the failing test, run it and see it fail, implement, run it and
  see it pass, commit. Tests go in the repo's existing suites where one
  covers the subject. A test helper's name says what it builds.
- Wire and journal changes need not keep old journals decodable. A new key
  is required, with one exception this plan makes on purpose: a Chronicle
  entry a browser saved earlier still decodes (design decision 3).
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
- Ruling 1 of the spec: Foundation III waits for Legacies. Both its faces
  are accepted at game start and do nothing.
- The word is "face" for a foundation card's two faces, as the model's
  `FoundationFace` has it. "Side" stays the word for a banner's and for the
  rulebook's own text.
- The package is `distantlands`. Every edit below is anchored by code, not
  by line number: each "Replace" block is cut from the file as it stands at
  `8fc747b8c`, with enough lines around the change that the block occurs
  once in its file.
- Shell is zsh: never write a bare `===` or `=word` token in a command, and
  quote globs (`--include='*.scala'`).
- Never `git add -A` or `git add .`; add files by path. Never push. No bare
  `git stash`.
- Commit messages and docs are plain prose. Every commit message ends with
  the trailer `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`.
  Use exactly that trailer whatever model you run on: the plan's trailer
  overrides your own.

---

## Setup

Work in a git worktree cut from `0.2.0-alpha`. In the worktree, link the
main checkout's tooling before the first `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

`.tooling` is ignored. If `git status` lists `node_modules` as untracked,
leave it untracked: never add it.

Check the base before Task 1:

```bash
git merge-base --is-ancestor 8fc747b8c HEAD && echo base-ok
git grep -n "FoundationState(FoundationFace.Normal, Set.empty)).toMap" -- src/main/scala/distantlands/gameplay/setup/GameStartRules.scala
git grep -n "foundation.altered" -- src/main/scala/distantlands/gameplay/RuleSourceIndex.scala
```

The first must print `base-ok`. If it does not, the worktree was cut from a
stale branch, such as `origin/main`: stop and report, even if the greps
pass, since they pass there too. Each grep must print one line. If either
prints nothing, the base has moved: stop and report.

The shared sources compile in the root project; there is no `sharedJVM`
project. A shared suite runs as `./sbtw "testOnly distantlands.protocol.X"`.
The frontend project is `frontend`, and it compiles the shared suites too.

## Plan-time findings

The spec was checked against the code at `8fc747b8c`. Where they differ,
this plan follows the code.

1. **`PrintedCards` cannot name a foundation.** The spec says that with
   the per-face ids "`PrintedCards` names the card a line comes from".
   `PrintedCards.printedOn` returns an `Option[CardId]`, and a foundation
   has no `CardId`; its own doc says "A banner face, a foundation and a
   game rule have none". The Game Log names a power through
   `LogWords.power`, which falls back from the printed card to the banner
   (`LogWords.bannerOf`). Task 1 adds the foundation face to that fallback.
   `PrintedCards` does not change. Task 7 corrects the spec's sentence.
2. **Normal faces get ids too.** `RuleSourceIndex` gave a normal face no
   power id and an altered one the shared `foundation.altered`. "One id per
   face" is read as all twelve. The index has two readers,
   `PhasePowerProcedure.sourcesFrom` and `PrintedCards.printedOn`, and both
   filter by a power id they already hold, so six more ids change nothing
   they do.
3. **`FoundationCards` lives in `distantlands.catalog`, beside
   `ExecutableCatalog`, not in it.** A face's rules text keeps the listing
   out of `gameplay`: `scripts/check-architecture.py` fails any `gameplay`
   file that reads `.text`. It stays out of `ExecutableCatalog` because the
   six cards are
   fixed, no test swaps one, and a new constructor field would touch every
   catalog a test builds. `ExecutableCatalog`'s doc says setup cards are
   "rules-owned code, not catalog data"; Task 1 rewords it.
4. **`FirstGameFoundationProfile` has one reader**, the gate this slice
   deletes (`MinorActionProcedures.supported`). The enum, the
   `FirstGameSupportState.foundationProfile` field and `ReadyGame.start`'s
   `profile` parameter go with it. Nothing serializes
   `FirstGameSupportState`. `MinorActionProcedures.started` then reads
   neither `ready` nor `actor`, so it loses both parameters; its five
   callers are in `gameplay/actions/minor`.
5. **There is no "game info area" in the frontend.** The spec's strip goes
   in the shared bank, the column beside the map that already holds the
   Oathkeeper line, the relic deck and the unheld banners
   (`WorldBoardRenderer.sharedBank`). Task 7 names it in the spec.
6. **The Chronicle page never decodes a Chronicle.** It lists
   `ChronicleSummary`, which the server computes
   (`ChronicleEntries.summary`). The summary gains `foundations`.
7. **`ChronicleRecord` now writes all six faces.** A first game's baseline
   lists none (`Chronicle.foundations` is empty), and the game holds six
   normal faces, so the journaled Chronicle lists six where the baseline
   listed none. `ChronicleRecordSuite`'s "every other field is the
   baseline's" resets `foundations` too.
8. **The TTS codec does not change.** `ChronicleTtsCodec.fromTts` already
   reads the mask into `FoundationState(Altered, Set.empty)` and
   `ChronicleTtsCodecSuite` already covers the round trip. Task 2 adds one
   test that an imported mask reaches the game.
9. **Two fixtures spell wire JSON by hand** and change with the wire:
   `TrustedGameProtocolSuite.seatResponseJson` (the summary) and
   `HttpGameClientSuite.projectionJson` (the projection). Both are in
   Task 5.
10. **`WarbandTransfersSuite`'s table cannot start a Move.** p1 rules no
    site there, so the old test compared two refusals. The new test places
    two of p1's warbands at Broken Peaks so the Move starts.

## Design decisions

These fill gaps the spec leaves. None is a rules question.

1. **The projection carries each face's rules text.** The spec lists
   "each number, its face and the face's name". The frontend cannot import
   the catalog, and without the text the catalog's text has no reader and a
   player cannot learn what "Wide Horizons" does. `BannerFace` solves the
   same need with a second copy of the rules in the frontend; projecting
   the text avoids the copy. A click on a table card opens it in the text
   overlay (`CardInspection.openText`). The Chronicle summary carries no
   text.
2. **`GameProjection.foundations` is a required key.** It is empty before a
   game starts.
3. **`ChronicleSummary.foundations` decodes as empty when absent.** A
   summary is stored in the player's browser, and a required key would turn
   every Chronicle saved before this slice into an "Unreadable" row whose
   only control is Delete. `ChronicleScore.role` set the precedent ("An
   entry saved before roles were kept has none"). Such an entry shows no
   strip until the server summarizes it again.
4. **The refusal is one table.** `StartingFoundations.arrivesWith` maps each
   refused face to the slice that brings it: I to 3, II and IV to 2, V to 5,
   VI to 6. A later slice deletes its own line. With several refused faces,
   game start names the first in foundation order. The message reads
   "Wide Horizons (altered Foundation I) is not supported yet: it arrives
   with Foundations slice 3". The check sits in `GameStartRules.buildPlan`
   after the Legacies refusals. Slice 3's erosion changes faces after the
   Empire is resolved, so slice 3 moves the check below
   `EmpireSetupRules.resolve`.
5. **The strip is six one-line rows.** The map is a fixed surface scaled to
   fit by its height, and DESIGN.md forbids anything that changes that
   height. A row is one line of name and, on an altered card, one line of
   mark; a name too long for the column is cut with an ellipsis and stays
   whole in the hover note. On the compact map a row takes
   `--glance-text`, one of the two named glance sizes, and no size of its
   own. Measured on the prototype: at 1440x900 every name fits; at 1024x768
   only the shortest do, and the rest are cut. Whole names there would need
   a second line, and six two-line rows with their marks can outgrow the
   regions at phone scales, so the row stays one line.
6. **An altered card is marked by the word alone**: "Altered" under the
   name, in Parchment Dim. The frame stays the neutral Line: Brass on the
   board means an option (DESIGN.md, "Brass"), so the strip uses no brass.
7. **The Chronicle page shows the strip for the loaded Chronicle only**,
   under its summary line. Saved rows keep their one line. Its rows are
   plain text with no `aria-label`, which a `span` would ignore; a table
   row is a button and carries one.
8. **`Table.foundation(number, face)`** is a new test step. Several suites
   already reach into the campaign under the comment "No Table step reaches
   lineage roles or foundations", and every later slice starts its tests
   from an altered face. `RestSuite` takes the step. The suites that turn
   all six cards at once or set a card's sources keep their direct update,
   and Tasks 1 and 3 correct their comments.

## Rulings needing the owner's review

No reading in this slice changes play: every altered face that would is
refused at game start. The owner decided ruling 1 in review; rulings 2 and
3 still need a look.

1. **Face text follows the printed setup cards (bgs-cards), which differ
   in wording from the rulebook's Foundations section; mechanics follow the
   rulebook where it says more (for example what an Atlas site keeps).**
   The owner decided this in review; the spec had asked for the rulebook's
   text. Printed card, Foundation I: "Edifices start **ruined**." Rulebook,
   Foundation I: "Edifices stored with it flip to their **ruined side**
   [...]. Other denizens start faceup." and "Relics stored with it start
   facedown." The catalog holds the card's words; slices 3 and 4 implement
   the rulebook's.
2. **"Players usually start with 6."** Printed card and rulebook, Altered
   Foundation II:
   "Each player takes 5 more favor [...] from the shared bank. _Players
   usually start with 6._" The player board rule says each player "Places 1
   favor [...] and 1 secret [...] on their player board". Reading chosen:
   the text is kept word for word, and the reminder is read as the total
   under this face, 1 and 5 more. Nothing in slice 1 depends on it. Slice 2
   implements "5 more" and should have the owner's confirmation first.
3. **Alteration sources gate nothing.** Rule text: "When one is activated,
   flip the foundation in your setup cards to its altered side immediately.
   If this legacy is discarded, do not flip the foundation back!" The old
   minor-action gate refused any foundation with an alteration source, even
   on its normal face. Reading chosen: a face starts exactly as the
   Chronicle stores it, sources kept, and no action reads the sources. Only
   the face decides what game start refuses.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/catalog/FoundationCards.scala` | new: the six cards, twelve faces, ids, names, text |
| `src/main/scala/distantlands/catalog/CatalogModel.scala` | `ExecutableCatalog`'s doc |
| `src/main/scala/distantlands/gameplay/RuleSourceIndex.scala` | one id per face |
| `src/main/scala/distantlands/application/gamelog/LogWords.scala` | `power` names a foundation face |
| `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala` | new: the Chronicle's faces, the refusal table |
| `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` | reads and refuses faces |
| `src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala`, `WarbandTransfers.scala`, `SelfExile.scala` | the gate goes |
| `src/main/scala/distantlands/model/Setup.scala`, `GameStateProtocol.scala` | `FirstGameFoundationProfile` goes |
| `src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala` | writes `campaign.foundations` |
| `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`, `GameProjectionDto.scala`, `GameProjectionCodec.scala` | `FoundationProjection`, `GameProjection.foundations` |
| `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala` | `ChronicleFoundation`, `ChronicleSummary.foundations` |
| `src/main/scala/distantlands/application/FoundationPresentation.scala` | new: faces as a client reads them |
| `src/main/scala/distantlands/application/GameProjection.scala`, `chronicle/ChronicleEntries.scala` | project them |
| `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala` | new: the strip |
| `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`, `ChroniclePage.scala` | place it |
| `frontend/styles.css`, `DESIGN.md` | its styles and its entry in the design system |
| `src/test/scala/distantlands/testkit/Table.scala` | `foundation` step |
| `src/test/scala/distantlands/gameplay/RestSuite.scala`, `RecoverProcedureSuite.scala`, `NegotiationProcedureSuite.scala`, `TravelProcedureSuite.scala` | use the step, or say truly why not |
| `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala` | three builders a second game can use |
| `docs/architecture/*.md`, the spec | docs |

---

### Task 1: The foundation cards, their ids and their names

**Files:**
- Create: `src/main/scala/distantlands/catalog/FoundationCards.scala`
- Modify: `src/main/scala/distantlands/catalog/CatalogModel.scala`
- Modify: `src/main/scala/distantlands/gameplay/RuleSourceIndex.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/LogWords.scala`
- Test: `src/test/scala/distantlands/catalog/FoundationCardsSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/RuleSourceIndexSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/LogWordsSuite.scala`

**Interfaces:**
- Produces:
  - `FoundationCardFace(id: PowerId, name: String, text: String)`.
  - `FoundationCard(number: FoundationNumber, normal: FoundationCardFace,
    altered: FoundationCardFace)` with `face(which: FoundationFace)`.
  - `FoundationCards.all: Vector[FoundationCard]`,
    `FoundationCards.card(number)`,
    `FoundationCards.face(number: FoundationNumber, which: FoundationFace):
    FoundationCardFace`,
    `FoundationCards.faceWithId(id: PowerId): Option[FoundationCardFace]`.
  - Ids `foundation.<numeral>.<name>`: `foundation.i.imperial-maps`,
    `foundation.i.wide-horizons`, `foundation.ii.powerful-tribes`,
    `foundation.ii.movers-and-shakers`, `foundation.iii.quiet-ambitions`,
    `foundation.iii.public-ambitions`, `foundation.iv.ties-of-fellowship`,
    `foundation.iv.teeming-world`, `foundation.v.mobs-favor`,
    `foundation.v.grand-council`, `foundation.vi.wandering-flame`,
    `foundation.vi.festival`.
  - `RuleSourceIndex` lists each foundation with the id of its face in use.
  - `LogWords.power` names a foundation face's id by the face's name.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/catalog/FoundationCardsSuite.scala`:

```scala
package distantlands.catalog

import distantlands.model.{FoundationFace, FoundationNumber, PowerId}

/** The six foundation setup cards (Foundations design, slice 1). */
class FoundationCardsSuite extends munit.FunSuite:
  test("the six cards are listed I to VI, each face by its printed name"):
    assertEquals(FoundationCards.all.map(card =>
      (card.number, card.normal.name, card.altered.name)), Vector(
      (FoundationNumber.I, "Imperial Maps", "Wide Horizons"),
      (FoundationNumber.II, "Powerful Tribes", "Movers and Shakers"),
      (FoundationNumber.III, "Quiet Ambitions", "Public Ambitions"),
      (FoundationNumber.IV, "Ties of Fellowship", "Teeming World"),
      (FoundationNumber.V, "Mob's Favor", "Grand Council"),
      (FoundationNumber.VI, "Wandering Flame", "Festival")))

  test("each face has its own id, spelled from its numeral and its name"):
    assertEquals(FoundationCards.all.flatMap(card =>
      Vector(card.normal.id, card.altered.id)).map(_.value), Vector(
      "foundation.i.imperial-maps", "foundation.i.wide-horizons",
      "foundation.ii.powerful-tribes", "foundation.ii.movers-and-shakers",
      "foundation.iii.quiet-ambitions", "foundation.iii.public-ambitions",
      "foundation.iv.ties-of-fellowship", "foundation.iv.teeming-world",
      "foundation.v.mobs-favor", "foundation.v.grand-council",
      "foundation.vi.wandering-flame", "foundation.vi.festival"))

  test("a face is found by its card's number and which face lies up, and " +
      "by its id"):
    val festival = FoundationCards.face(FoundationNumber.VI,
      FoundationFace.Altered)
    assertEquals(festival.name, "Festival")
    assertEquals(FoundationCards.face(FoundationNumber.VI,
      FoundationFace.Normal).name, "Wandering Flame")
    assertEquals(FoundationCards.faceWithId(festival.id), Some(festival))
    // The banner's Festival side is another source with another id.
    assertEquals(FoundationCards.faceWithId(
      PowerId("banner.darkest-secret.festival")), None)

  test("a face's text is the printed card's, a blank line between " +
      "paragraphs"):
    assertEquals(FoundationCards.face(FoundationNumber.V,
      FoundationFace.Altered).text,
      "Flip the People's Favor to its **Grand Council** side. Place " +
        "[favor] on it from the shared bank.\n\n" +
        "If the Oathkeeper goal is the People's Favor, give the banner to " +
        "the Chancellor, if any. Otherwise, place it in the shared bank.")
    assertEquals(FoundationCards.face(FoundationNumber.I,
      FoundationFace.Altered).text.split("\n\n").last,
      "_Keep this card out as a reminder._")
    assertEquals(FoundationCards.all.map(card =>
      (card.normal.text.split("\n\n").length,
        card.altered.text.split("\n\n").length)),
      Vector((5, 5), (3, 2), (1, 2), (3, 2), (2, 2), (2, 2)))
```

In `src/test/scala/distantlands/gameplay/RuleSourceIndexSuite.scala`, in
the test "all six Foundations expose ordered identities, faces, and
state". Two comments there say no Table step sets foundations; Task 3 adds
one that sets a face, so they now say what stays true:

Replace

```scala
    val base = board
    val arbiter = ArbiterCard.id
    val ironHand = IronHandCard.id
    // No Table step sets foundations or legacies.
    val altered = base.updateCampaign(_.copy(foundations =
      base.game.campaign.foundations.updated(FoundationNumber.III,
        FoundationState(FoundationFace.Altered, Set(arbiter, ironHand)))))
```

with

```scala
    val base = board
    val arbiter = ArbiterCard.id
    val ironHand = IronHandCard.id
    // No Table step sets a foundation's sources.
    val altered = base.updateCampaign(_.copy(foundations =
      base.game.campaign.foundations.updated(FoundationNumber.III,
        FoundationState(FoundationFace.Altered, Set(arbiter, ironHand)))))
```

Then replace

```scala
    assertEquals(foundations.map(_.face), Vector(
      RuleSourceFace.Normal, RuleSourceFace.Normal, RuleSourceFace.Altered,
      RuleSourceFace.Normal, RuleSourceFace.Normal, RuleSourceFace.Normal))
    assertEquals(foundations(2).powerIds, Vector(PowerId("foundation.altered")))
    // Sorted by id: Iron Hand is L01, Arbiter L23.
    assertEquals(foundations(2).state, RuleSourceState.Foundation(
      Vector(ironHand, arbiter)))
    assert(foundations.zipWithIndex.filterNot(_._2 == 2).forall {
      case (source, _) => source.powerIds.isEmpty &&
        source.state == RuleSourceState.Foundation(Vector.empty)
    })
```

with

```scala
    assertEquals(foundations.map(_.face), Vector(
      RuleSourceFace.Normal, RuleSourceFace.Normal, RuleSourceFace.Altered,
      RuleSourceFace.Normal, RuleSourceFace.Normal, RuleSourceFace.Normal))
    // Each foundation carries the id of the face in use.
    assertEquals(foundations.map(_.powerIds), Vector(
      "foundation.i.imperial-maps", "foundation.ii.powerful-tribes",
      "foundation.iii.public-ambitions", "foundation.iv.ties-of-fellowship",
      "foundation.v.mobs-favor", "foundation.vi.wandering-flame")
      .map(id => Vector(PowerId(id))))
    // Sorted by id: Iron Hand is L01, Arbiter L23.
    assertEquals(foundations(2).state, RuleSourceState.Foundation(
      Vector(ironHand, arbiter)))
    assert(foundations.zipWithIndex.filterNot(_._2 == 2).forall {
      case (source, _) =>
        source.state == RuleSourceState.Foundation(Vector.empty)
    })
```

Then replace

```scala
    val legacyDefinition = catalog.legacies.head
    val legacy = LegacyState(LegacyId(legacyDefinition.id.value), active = false)
    val lineage = base.game.campaign.lineages(lineageId)
    // No Table step sets foundations or legacies.
    val changed = base.updateCampaign(_.copy(lineages = base.game.campaign.lineages.updated(
        lineageId, lineage.copy(legacies = Vector(legacy)))))
    val indexed = present(RuleSourceIndex.enumerate(catalog, changed).find(
```

with

```scala
    val legacyDefinition = catalog.legacies.head
    val legacy = LegacyState(LegacyId(legacyDefinition.id.value), active = false)
    val lineage = base.game.campaign.lineages(lineageId)
    // No Table step sets legacies.
    val changed = base.updateCampaign(_.copy(lineages = base.game.campaign.lineages.updated(
        lineageId, lineage.copy(legacies = Vector(legacy)))))
    val indexed = present(RuleSourceIndex.enumerate(catalog, changed).find(
```

In `src/test/scala/distantlands/application/gamelog/LogWordsSuite.scala`,
add the imports and append the test:

Replace

```scala
package distantlands.application.gamelog

import distantlands.model._
import distantlands.protocol.projection.LogSpan
import LogSpan.Text

class LogWordsSuite extends munit.FunSuite:
```

with

```scala
package distantlands.application.gamelog

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.LogSpan
import distantlands.testkit.Table
import distantlands.testkit.Table.p1
import LogSpan.Text

class LogWordsSuite extends munit.FunSuite:
```

Then replace

```scala
    assertEquals(LogWords.dice(Vector(AttackDieFace.OneSword,
      DefenseDieFace.Blank)), None)
    assertEquals(LogWords.dice(Vector.empty), None)
```

with

```scala
    assertEquals(LogWords.dice(Vector(AttackDieFace.OneSword,
      DefenseDieFace.Blank)), None)
    assertEquals(LogWords.dice(Vector.empty), None)

  test("a power printed on a foundation face reads as that face's name"):
    val words = new LogWords(catalog, LogScripts.presentation)
    def named(id: String): String = words.power(Table.start.ready, p1,
      PowerId(id), None).map(_.text).mkString
    assertEquals(named("foundation.i.wide-horizons"), "Wide Horizons")
    assertEquals(named("foundation.v.mobs-favor"), "Mob's Favor")
    // A banner side is still named by its banner.
    assertEquals(named("banner.peoples-favor.mob"), "People's Favor")
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.catalog.FoundationCardsSuite distantlands.gameplay.RuleSourceIndexSuite distantlands.application.gamelog.LogWordsSuite"`
Expected: compile failure, `Not found: FoundationCards` in
`FoundationCardsSuite`.

- [ ] **Step 3: The catalog listing**

Create `src/main/scala/distantlands/catalog/FoundationCards.scala`. Each
face's text is its printed setup card's, as every other card's text is
(ruling 1). The source is
`~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`,
ids `OATH-SETUP04a-NF` to `OATH-SETUP09b-NF`: `a` is the normal face and
`b` the altered one. In that file an entry's `text:` comes before its
`name:`, so read an entry by its `id`, never by the lines after a name. The
text below is already transcribed: `symbol:favor` and `symbol:secret` are
written `[favor]` and `[secret]`, bold and italic markup is kept, and a
blank line parts the paragraphs. Check it against the file if you doubt a
word; do not reword it.

```scala
package distantlands.catalog

import distantlands.model.{FoundationFace, FoundationNumber, PowerId}

/** One face of a foundation setup card: the id rule sources and the Game
  * Log know it by, its printed name, and its text as the card prints it, in
  * the catalog's markup with a blank line between paragraphs. Gameplay
  * never reads `text`: behaviour lives in setup and in the procedures that
  * read the face in use, and follows the rulebook where it says more than
  * the card.
  */
final case class FoundationCardFace(id: PowerId, name: String, text: String):
  require(name.trim.nonEmpty, "a foundation face needs its printed name")
  require(text.trim.nonEmpty, "a foundation face needs its rules text")

/** A foundation setup card: its number and its two faces. */
final case class FoundationCard(number: FoundationNumber,
    normal: FoundationCardFace, altered: FoundationCardFace):
  def face(which: FoundationFace): FoundationCardFace = which match
    case FoundationFace.Normal => normal
    case FoundationFace.Altered => altered

/** The six foundation setup cards of New Foundations, I to VI (Foundations
  * design, slice 1). They are fixed, so they stand beside
  * [[ExecutableCatalog]] and not in it: no test swaps one.
  */
object FoundationCards:
  private def printed(number: String, slug: String, name: String,
      paragraphs: String*): FoundationCardFace =
    FoundationCardFace(PowerId(s"foundation.$number.$slug"), name,
      paragraphs.mkString("\n\n"))

  /** Both faces of V, and of VI, end with the same paragraph. */
  private def bannerGoes(banner: String): String =
    s"If the Oathkeeper goal is the $banner, give the banner to the " +
      "Chancellor, if any. Otherwise, place it in the shared bank."

  val all: Vector[FoundationCard] = Vector(
    FoundationCard(FoundationNumber.I,
      printed("i", "imperial-maps", "Imperial Maps",
        "The Chancellor fills the remaining empty site slots going " +
          "Hinterward.",
        "They must fill these slots with Recent Sites or Forgotten Sites " +
          "from the Atlas, but not both.",
        "**Bandits** are placed on the site equal to its capacity.",
        "Edifices start **ruined**.",
        "**Fill the site's empty relic slots** by drawing relics from the " +
          "relic deck."),
      printed("i", "wide-horizons", "Wide Horizons",
        "**Do not fill empty site slots!**",
        "In this game, players can spend 1 Supply to **Explore** if their " +
          "pawn is at the Hintermost site card (rulebook page 9).",
        "Explore works like a Recover action with a **difficulty of 4**.",
        "If you succeed, place a site on the next Hinterward slot, " +
          "populate it as described on the back of this card, and then " +
          "place your pawn on the site.",
        "_Keep this card out as a reminder._")),
    FoundationCard(FoundationNumber.II,
      printed("ii", "powerful-tribes", "Powerful Tribes",
        "Place [favor] [favor] [favor] on each suit's favor bank.",
        "If you have five or more players, add an extra [favor] to each " +
          "favor bank.",
        "Then, for each denizen on the map, add an extra [favor] to the " +
          "matching favor bank."),
      printed("ii", "movers-and-shakers", "Movers and Shakers",
        "Each player takes 5 more favor [favor] [favor] [favor] [favor] " +
          "[favor] from the shared bank. _Players usually start with 6._",
        "Then, for each denizen on the map, place [favor] on the matching " +
          "favor bank.")),
    FoundationCard(FoundationNumber.III,
      printed("iii", "quiet-ambitions", "Quiet Ambitions",
        "Each player draws 2 legacy cards, keeps one facedown in their " +
          "play area, and returns the other to the box."),
      printed("iii", "public-ambitions", "Public Ambitions",
        "Deal 4 legacy cards to a faceup row near the map.",
        "Everyone competes to activate these public legacies in the " +
          "Chronicle Phase. For these, the outgoing Chancellor breaks ties " +
          "between Imperials alone. Discard public legacies that aren't " +
          "activated.")),
    FoundationCard(FoundationNumber.IV,
      printed("iv", "ties-of-fellowship", "Ties of Fellowship",
        "Each player who has no starting advisers draws 3 denizens " +
          "facedown from the starting denizen stack.",
        "_You will choose one as an adviser later. Players sometimes carry " +
          "over advisers from the previous game._",
        "Then, deal 2 denizens from the starting denizen stack facedown to " +
          "each discard pile."),
      printed("iv", "teeming-world", "Teeming World",
        "Each player draws facedown denizens from the starting denizen " +
          "stack until they have three starting advisers.",
        "Discard the rest of the starting denizen stack as evenly as " +
          "possible, going from Cradle to Provinces to Hinterland. _The " +
          "world deck will only have Visions!_")),
    FoundationCard(FoundationNumber.V,
      printed("v", "mobs-favor", "Mob's Favor",
        "Set the People's Favor on its **Mob** side. Place [favor] on it " +
          "from the shared bank.",
        bannerGoes("People's Favor")),
      printed("v", "grand-council", "Grand Council",
        "Flip the People's Favor to its **Grand Council** side. Place " +
          "[favor] on it from the shared bank.",
        bannerGoes("People's Favor"))),
    FoundationCard(FoundationNumber.VI,
      printed("vi", "wandering-flame", "Wandering Flame",
        "Set the Darkest Secret on its **Wandering Flame** side. Place " +
          "[secret] on it from the shared bank.",
        bannerGoes("Darkest Secret")),
      printed("vi", "festival", "Festival",
        "Flip the Darkest Secret to its **Festival side**. Place [secret] " +
          "on it from the shared bank. Place the Festival marker nearby.",
        bannerGoes("Darkest Secret"))))

  private val byNumber: Map[FoundationNumber, FoundationCard] =
    all.map(card => card.number -> card).toMap
  private val byId: Map[PowerId, FoundationCardFace] =
    all.flatMap(card => Vector(card.normal, card.altered))
      .map(face => face.id -> face).toMap

  def card(number: FoundationNumber): FoundationCard = byNumber(number)

  /** The face of `number` that lies up when the card shows `which`. */
  def face(number: FoundationNumber, which: FoundationFace)
      : FoundationCardFace = card(number).face(which)

  /** The face that carries `id`, if `id` is a foundation face's. */
  def faceWithId(id: PowerId): Option[FoundationCardFace] = byId.get(id)
```

In `src/main/scala/distantlands/catalog/CatalogModel.scala`, in
`ExecutableCatalog`'s doc:

Replace

```scala
/**
 * The complete runtime component catalog: card objects indexed by id and by
 * printed power. `gameplay.cards.NewFoundations.catalog` is the production
 * one; a test may `copy` in variant cards. Setup cards, player boards and
 * Visions are rules-owned code, not catalog data.
 */
final case class ExecutableCatalog(
    denizens: Vector[Denizen],
```

with

```scala
/**
 * The complete runtime component catalog: card objects indexed by id and by
 * printed power. `gameplay.cards.NewFoundations.catalog` is the production
 * one; a test may `copy` in variant cards. The six foundation setup cards
 * are fixed and listed in [[FoundationCards]]; the other setup cards, player
 * boards and Visions are rules-owned code, not catalog data.
 */
final case class ExecutableCatalog(
    denizens: Vector[Denizen],
```

- [ ] **Step 4: One id per face in the rule-source index**

In `src/main/scala/distantlands/gameplay/RuleSourceIndex.scala`:

Replace

```scala
package distantlands.gameplay

import distantlands.catalog.ExecutableCatalog
import distantlands.catalog.PrintedPower
import distantlands.model._

/** Orientation is a factual property only; callers still own activation rules. */
```

with

```scala
package distantlands.gameplay

import distantlands.catalog.{ExecutableCatalog, FoundationCards, PrintedPower}
import distantlands.model._

/** Orientation is a factual property only; callers still own activation rules. */
```

Then replace

```scala
      ready.game.campaign.foundations.get(number).map { foundation =>
        IndexedRuleSource(
          RuleSourceRef.Foundation(number),
          foundation.face match {
            case FoundationFace.Normal => Vector.empty
            case FoundationFace.Altered => rawIds(Vector("foundation.altered"))
          },
          foundation.face match {
            case FoundationFace.Normal => RuleSourceFace.Normal
            case FoundationFace.Altered => RuleSourceFace.Altered
```

with

```scala
      ready.game.campaign.foundations.get(number).map { foundation =>
        IndexedRuleSource(
          RuleSourceRef.Foundation(number),
          Vector(FoundationCards.face(number, foundation.face).id),
          foundation.face match {
            case FoundationFace.Normal => RuleSourceFace.Normal
            case FoundationFace.Altered => RuleSourceFace.Altered
```

`rawIds` stays: the banners still use it. The index reads only the face's
`id`, never its `text`.

- [ ] **Step 5: The Game Log names a foundation face**

In `src/main/scala/distantlands/application/gamelog/LogWords.scala`:

Replace

```scala
package distantlands.application.gamelog

import distantlands.application.{GamePresentationProjector, RolledFaces}
import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.BannerRules
import distantlands.model._
import distantlands.protocol.projection.LogSpan
```

with

```scala
package distantlands.application.gamelog

import distantlands.application.{GamePresentationProjector, RolledFaces}
import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.actions.BannerRules
import distantlands.model._
import distantlands.protocol.projection.LogSpan
```

Then replace

```scala

  /** A power named by the card it is printed on, as the modifier decision
    * names it; the card follows the visibility rule on `ready`. A banner's
    * power, printed on no card, is named by its banner. */
  def power(ready: ReadyGame, actor: PlayerId, id: PowerId,
      viewer: Option[PlayerId]): Vector[LogSpan] =
    printed.printedOn(ready, actor, id.value).fold(
      Vector[LogSpan](LogWords.bannerOf(id).fold[LogSpan](
        LogSpan.Text(presentation.safeLabel(id.value)))(banner)))(card =>
      one(this.card(card, ready, ready, viewer)))

private[gamelog] object LogWords:
```

with

```scala

  /** A power named by the card it is printed on, as the modifier decision
    * names it; the card follows the visibility rule on `ready`. A banner's
    * power, printed on no card, is named by its banner, and a foundation
    * face's by the face's printed name. */
  def power(ready: ReadyGame, actor: PlayerId, id: PowerId,
      viewer: Option[PlayerId]): Vector[LogSpan] =
    printed.printedOn(ready, actor, id.value).fold(
      Vector[LogSpan](LogWords.bannerOf(id).map(banner)
        .orElse(FoundationCards.faceWithId(id).map(face =>
          LogSpan.Text(face.name)))
        .getOrElse(LogSpan.Text(presentation.safeLabel(id.value)))))(card =>
      one(this.card(card, ready, ready, viewer)))

private[gamelog] object LogWords:
```

- [ ] **Step 6: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.catalog.FoundationCardsSuite distantlands.gameplay.RuleSourceIndexSuite distantlands.application.gamelog.LogWordsSuite"`
Expected: PASS, 15 tests.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/catalog/FoundationCards.scala src/main/scala/distantlands/catalog/CatalogModel.scala src/main/scala/distantlands/gameplay/RuleSourceIndex.scala src/main/scala/distantlands/application/gamelog/LogWords.scala src/test/scala/distantlands/catalog/FoundationCardsSuite.scala src/test/scala/distantlands/gameplay/RuleSourceIndexSuite.scala src/test/scala/distantlands/application/gamelog/LogWordsSuite.scala
git commit -m "$(cat <<'EOF'
feat(foundations): list the six foundation cards and give each face an id

FoundationCards names the twelve faces and holds their printed text.
The rule-source index lists each foundation by the id of its face in
use, in place of the shared foundation.altered id, and the Game Log
names such an id by its face.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Game start keeps the Chronicle's faces and refuses the gated ones

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/application/chronicle/ChronicleTtsCodecSuite.scala`

**Interfaces:**
- Consumes: `FoundationCards.face(number, which).name` (Task 1).
- Produces:
  - `StartingFoundations.of(chronicle: Chronicle):
    Map[FoundationNumber, FoundationState]`: all six numbers, a missing one
    as `FoundationState(FoundationFace.Normal, Set.empty)`.
  - `StartingFoundations.refusal(faces: Map[FoundationNumber,
    FoundationState]): Option[String]`: the message for the first altered
    I, II, IV, V or VI.
  - `GameStartRules.evolve` puts `StartingFoundations.of(chronicle)` in
    `CampaignState.foundations` and refuses with
    `UnsupportedChronicle(refusal)`.

- [ ] **Step 1: Write the failing tests**

In `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`,
before the test "a later game with a seated Chancellor starts":

Replace

```scala
    assertEquals(GameStartRules.evolve(catalog, keeping, orders),
      Left(OathViolation.UnsupportedChronicle("legacies require Legacies")))

  test("a later game with a seated Chancellor starts"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    accepted(GameStartRules.evolve(catalog, record,
```

with

```scala
    assertEquals(GameStartRules.evolve(catalog, keeping, orders),
      Left(OathViolation.UnsupportedChronicle("legacies require Legacies")))

  private val altered = FoundationState(FoundationFace.Altered, Set.empty)
  private val normal = FoundationState(FoundationFace.Normal, Set.empty)

  test("a Chronicle that lists no foundation starts with six normal faces"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(ready.game.campaign.foundations,
      FoundationNumber.all.map(_ -> normal).toMap)

  test("game start keeps the Chronicle's foundation faces and their " +
      "sources, and a number it does not list lies normal"):
    val sourced = FoundationState(FoundationFace.Altered,
      Set(LegacyId(catalog.legacies.head.id.value)))
    val stored = chronicle.copy(foundations = Map(
      FoundationNumber.III -> sourced, FoundationNumber.V -> normal))
    val ready = accepted(GameStartRules.evolve(catalog, stored, orders))
    assertEquals(ready.game.campaign.foundations,
      FoundationNumber.all.map(_ -> normal).toMap
        .updated(FoundationNumber.III, sourced))

  test("game start refuses each altered face but Foundation III's, naming " +
      "the face and the slice it arrives with"):
    def refusalFor(number: FoundationNumber) = rejected(GameStartRules.evolve(
      catalog, chronicle.copy(foundations = Map(number -> altered)), orders))
    def unsupported(face: String, slice: Int) =
      OathViolation.UnsupportedChronicle(
        s"$face is not supported yet: it arrives with Foundations slice $slice")
    assertEquals(refusalFor(FoundationNumber.I),
      unsupported("Wide Horizons (altered Foundation I)", 3))
    assertEquals(refusalFor(FoundationNumber.II),
      unsupported("Movers and Shakers (altered Foundation II)", 2))
    assertEquals(refusalFor(FoundationNumber.IV),
      unsupported("Teeming World (altered Foundation IV)", 2))
    assertEquals(refusalFor(FoundationNumber.V),
      unsupported("Grand Council (altered Foundation V)", 5))
    assertEquals(refusalFor(FoundationNumber.VI),
      unsupported("Festival (altered Foundation VI)", 6))

  test("with several altered faces, game start names the first in " +
      "foundation order"):
    val several = chronicle.copy(foundations = Map(
      FoundationNumber.VI -> altered, FoundationNumber.III -> altered,
      FoundationNumber.II -> altered))
    assertEquals(rejected(GameStartRules.evolve(catalog, several, orders)),
      OathViolation.UnsupportedChronicle("Movers and Shakers (altered " +
        "Foundation II) is not supported yet: it arrives with Foundations " +
        "slice 2"))

  test("a later game with a seated Chancellor starts"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    accepted(GameStartRules.evolve(catalog, record,
```

In `src/test/scala/distantlands/application/chronicle/ChronicleTtsCodecSuite.scala`:

Replace

```scala
import distantlands.gameplay.powers.action.WhistleCard
import distantlands.gameplay.powers.campaign.ScoutsCard
import distantlands.gameplay.powers.cardplay.LostTapestryCard
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.model._

/** TTS strings to and from Chronicles (Chronicle TTS codec design). */
```

with

```scala
import distantlands.gameplay.powers.action.WhistleCard
import distantlands.gameplay.powers.campaign.ScoutsCard
import distantlands.gameplay.powers.cardplay.LostTapestryCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules}
import distantlands.model._

/** TTS strings to and from Chronicles (Chronicle TTS codec design). */
```

Then replace

```scala
        Set.empty)).toMap)
    assertEquals(roundTripped(altered), altered)

  test("a version 3 goal digit of 0 reads as Supremacy"):
    val unset = TtsChronicle(foundations = Some(TtsFoundations(0, Some(0))))
    assertEquals(codec.fromTts(unset, "Imported").map(_.oathkeeperGoal),
```

with

```scala
        Set.empty)).toMap)
    assertEquals(roundTripped(altered), altered)

  test("an imported mask's altered face reaches the game its Chronicle " +
      "starts"):
    // Bit 2 is Foundation III, the one altered face game start accepts.
    val imported = codec.fromTts(TtsChronicle(foundations =
      Some(TtsFoundations(1 << 2, None))), "Imported").fold(fail(_), identity)
    val altered = FoundationState(FoundationFace.Altered, Set.empty)
    assertEquals(imported.foundations, Map(FoundationNumber.III -> altered))
    val ready = GameStartRules.evolve(FirstGameSetupFixture.catalog,
      chronicle.copy(foundations = imported.foundations),
      FirstGameSetupFixture.orders).fold(
        refusal => fail(s"game start refused: $refusal"), identity)
    assertEquals(ready.game.campaign.foundations(FoundationNumber.III),
      altered)

  test("a version 3 goal digit of 0 reads as Supremacy"):
    val unset = TtsChronicle(foundations = Some(TtsFoundations(0, Some(0))))
    assertEquals(codec.fromTts(unset, "Imported").map(_.oathkeeperGoal),
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.application.chronicle.ChronicleTtsCodecSuite"`
Expected: four tests FAIL. "game start keeps the Chronicle's foundation
faces" and "an imported mask's altered face reaches the game" find a
normal Foundation III; the two refusal tests report `expected a Left, got
Right`. "a Chronicle that lists no foundation starts with six normal
faces" passes already: it pins what must not change.

- [ ] **Step 3: The faces a game starts with**

Create `src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.FoundationCards
import distantlands.model._

/** The foundation faces a game starts with (Foundations design, slice 1). */
object StartingFoundations:
  /** The Chronicle's faces with their alteration sources. A number the
    * Chronicle does not list lies normal, with no source. */
  def of(chronicle: Chronicle): Map[FoundationNumber, FoundationState] =
    FoundationNumber.all.map(number => number ->
      chronicle.foundations.getOrElse(number,
        FoundationState(FoundationFace.Normal, Set.empty))).toMap

  /** The slice of the Foundations design each altered face arrives with.
    * The slice that makes a face work deletes its line. Foundation III has
    * none: both its faces wait for Legacies and do nothing (ruling 1). */
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.I -> 3,
    FoundationNumber.II -> 2,
    FoundationNumber.IV -> 2,
    FoundationNumber.V -> 5,
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

- [ ] **Step 4: Game start reads and refuses them**

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`:

Replace

```scala
 *
 * Validation here is only what Setup itself needs: known and unique ids
 * across every Chronicle field (`ChronicleAccounting`), a later game's
 * Empire (`EmpireSetupRules`), enough cards to deal, and, in a first game,
 * one edifice per Homeland in play (Empire ruling R26). The old
 * plan's suit-count and Vision-packet audit is the generator's own job now
 * (`FirstGameChronicleGenerator.validate`), checked once at generation
```

with

```scala
 *
 * Validation here is only what Setup itself needs: known and unique ids
 * across every Chronicle field (`ChronicleAccounting`), a later game's
 * Empire (`EmpireSetupRules`), foundation faces that work
 * (`StartingFoundations`), enough cards to deal, and, in a first game,
 * one edifice per Homeland in play (Empire ruling R26). The old
 * plan's suit-count and Vision-packet audit is the generator's own job now
 * (`FirstGameChronicleGenerator.validate`), checked once at generation
```

Then replace

```scala
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
      ).toMap
      foundations = FoundationNumber.all.map(number =>
        number -> FoundationState(FoundationFace.Normal, Set.empty)).toMap
      // The atlas past the sites in play keeps every stored card, the
      // inverse of `ChronicleExtraction.extract`'s `carried`.
      atlas = AtlasState(chronicle.atlasBox.drop(8 - chronicle.world.size)
```

with

```scala
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
      ).toMap
      // The faces the Chronicle stores; `buildPlan` refuses the altered
      // ones no slice has brought in yet.
      foundations = StartingFoundations.of(chronicle)
      // The atlas past the sites in play keeps every stored card, the
      // inverse of `ChronicleExtraction.extract`'s `carried`.
      atlas = AtlasState(chronicle.atlasBox.drop(8 - chronicle.world.size)
```

Then replace

```scala
      records = chronicle.lineages.filter(lineage => seated(lineage.id))
      _ <- Either.cond(records.forall(_.legacies.isEmpty), (),
        UnsupportedChronicle("legacies require Legacies"))
      _ <- validateParticipants(orders)
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
```

with

```scala
      records = chronicle.lineages.filter(lineage => seated(lineage.id))
      _ <- Either.cond(records.forall(_.legacies.isEmpty), (),
        UnsupportedChronicle("legacies require Legacies"))
      _ <- StartingFoundations.refusal(StartingFoundations.of(chronicle))
        .map(UnsupportedChronicle(_)).toLeft(())
      _ <- validateParticipants(orders)
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
```

- [ ] **Step 5: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.chronicle.ChronicleTtsCodecSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/StartingFoundations.scala src/main/scala/distantlands/gameplay/setup/GameStartRules.scala src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala src/test/scala/distantlands/application/chronicle/ChronicleTtsCodecSuite.scala
git commit -m "$(cat <<'EOF'
feat(foundations): start a game with the Chronicle's foundation faces

Game start copies the Chronicle's faces and their sources into the
campaign, a missing number as normal. An altered I, II, IV, V or VI is
refused by name with the slice it arrives with; Foundation III is
accepted on either face.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: The minor-action gate goes

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/minor/SelfExile.scala`
- Modify: `src/main/scala/distantlands/model/Setup.scala`
- Modify: `src/main/scala/distantlands/model/GameStateProtocol.scala`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Test: `src/test/scala/distantlands/testkit/TableSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/WarbandTransfersSuite.scala`
- Test: `src/test/scala/distantlands/model/ReadyGameSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/RestSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/RecoverProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/TravelProcedureSuite.scala`

**Interfaces:**
- Produces:
  - `Table.foundation(number: FoundationNumber, face: FoundationFace):
    Table`: that card on that face with no alteration source. Tasks 4 and 5
    use it.
  - `MinorActionProcedures.started(args: Vector[DecisionOptionRef],
    action: ActionRef): Either[OathViolation, Unit]`.
  - `FirstGameSupportState(firstPlayer: PlayerId,
    ignoreCitizenshipPowers: Boolean = true)`.
  - `ReadyGame.start(game, colors, firstPlayer, favorBanks)`.
  - `FirstGameFoundationProfile` no longer exists.

- [ ] **Step 1: Write the failing test and its table step**

In `src/test/scala/distantlands/testkit/Table.scala`, before
`knowsRelicAt`:

Replace

```scala
      c.banners.darkestSecret.copy(active = face, holder = holder,
        secrets = secrets)))))

  /** `viewer` knows `card`, a relic lying at `at`, as a peek records it. */
  def knowsRelicAt(viewer: PlayerId, card: RelicRef,
      at: SiteRef)(using munit.Location): Table =
```

with

```scala
      c.banners.darkestSecret.copy(active = face, holder = holder,
        secrets = secrets)))))

  /** Foundation `number` showing `face`, with no alteration source; the
    * quiet table shows every normal face. */
  def foundation(number: FoundationNumber, face: FoundationFace): Table =
    update(_.updateCampaign(campaign => campaign.copy(foundations =
      campaign.foundations.updated(number, FoundationState(face, Set.empty)))))

  /** `viewer` knows `card`, a relic lying at `at`, as a peek records it. */
  def knowsRelicAt(viewer: PlayerId, card: RelicRef,
      at: SiteRef)(using munit.Location): Table =
```

In `src/test/scala/distantlands/testkit/TableSuite.scala`:

Replace

```scala
    val returned = Table.start.boardWarbands(p1, 10).warbandsInBank(p1, 6)
    assertEquals(Look(returned.ready).boardWarbands(p1), 8)

  test("each step states one fact"):
    val ready = Table.start
      .turn(p2, Phase.Wake)
```

with

```scala
    val returned = Table.start.boardWarbands(p1, 10).warbandsInBank(p1, 6)
    assertEquals(Look(returned.ready).boardWarbands(p1), 8)

  test("foundation turns one setup card and leaves the other five"):
    val table = Table.start.foundation(FoundationNumber.III,
      FoundationFace.Altered)
    assertEquals(table.ready.game.campaign.foundations,
      Table.start.ready.game.campaign.foundations.updated(
        FoundationNumber.III,
        FoundationState(FoundationFace.Altered, Set.empty)))

  test("each step states one fact"):
    val ready = Table.start
      .turn(p2, Phase.Wake)
```

In `src/test/scala/distantlands/gameplay/WarbandTransfersSuite.scala`:

Replace

```scala
      .left.map(ActOffers.reason(ActionRef.TransferWarbands, _)),
      Left("Only an Imperial player transfers warbands"))

  test("an altered foundation refuses the transfer as it refuses a move"):
    val altered = imperialsTogether.update(ready => ready.updateCampaign(
      campaign => campaign.copy(foundations = campaign.foundations.map {
        case (number, foundation) =>
          number -> foundation.copy(face = FoundationFace.Altered)
      })))
    val refusal = rules.startWalker(altered.state, ActionRef.MoveWarbands, p1)
    assertEquals(refusal.left.toOption, Some(OathViolation.MinorActionUnavailable(
      "minor actions are limited to the fixed unaltered foundations")))
    assertEquals(rules.startWalker(altered.state, ActionRef.TransferWarbands,
      p1).left.toOption, refusal.left.toOption)

  test("with no Imperial at the site the transfer is unavailable"):
    val table = imperialsTogether.pawn(p2, at = AncientCitySite)
```

with

```scala
      .left.map(ActOffers.reason(ActionRef.TransferWarbands, _)),
      Left("Only an Imperial player transfers warbands"))

  test("an altered Foundation III gates neither the transfer nor a move"):
    // p1's warbands hold Broken Peaks, so a move has a way to go.
    val altered = imperialsTogether.warbandsAt(BrokenPeaksSite, p1, 2)
      .foundation(FoundationNumber.III, FoundationFace.Altered)
    accepted(rules.startWalker(altered.state, ActionRef.MoveWarbands, p1))
    accepted(rules.startWalker(altered.state, ActionRef.TransferWarbands, p1))

  test("a normal foundation that carries an alteration source gates " +
      "neither the transfer nor a move"):
    val sourced = FoundationState(FoundationFace.Normal,
      Set(LegacyId("legacy:any")))
    // No Table step sets a foundation's sources.
    val kept = imperialsTogether.warbandsAt(BrokenPeaksSite, p1, 2)
      .update(_.updateCampaign(campaign => campaign.copy(foundations =
        campaign.foundations.updated(FoundationNumber.II, sourced))))
    accepted(rules.startWalker(kept.state, ActionRef.MoveWarbands, p1))
    accepted(rules.startWalker(kept.state, ActionRef.TransferWarbands, p1))

  test("with no Imperial at the site the transfer is unavailable"):
    val table = imperialsTogether.pawn(p2, at = AncientCitySite)
```

- [ ] **Step 2: Run them to make sure the gate fails the new test**

Run: `./sbtw "testOnly distantlands.testkit.TableSuite distantlands.gameplay.WarbandTransfersSuite"`
Expected: `TableSuite` passes. `WarbandTransfersSuite` fails in two
tests, "an altered Foundation III gates neither the transfer nor a move"
and "a normal foundation that carries an alteration source gates neither
the transfer nor a move", each with `expected a Right, got
Left(MinorActionUnavailable(minor actions are limited to the fixed
unaltered foundations))`. They are the old gate's two conditions: an
altered face, and any alteration source.

- [ ] **Step 3: Delete the gate**

In `src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala`:

Replace

```scala
  * and Move Warbands. Their rules are the legacy handler's, moved into
  * walker trees.
  *
  * A start checks the first-game rules gate, as the handler did, and
  * takes no start selection: what the player picks is
  * asked inside the walk. Every operation is required, so a restriction
  * that refuses one refuses the action with its own reason. A resume
  * rebuilds the same tree without the start's gates.
  */
```

with

```scala
  * and Move Warbands. Their rules are the legacy handler's, moved into
  * walker trees.
  *
  * A start takes no start selection: what the player picks is asked
  * inside the walk. Every operation is required, so a restriction
  * that refuses one refuses the action with its own reason. A resume
  * rebuilds the same tree without the start's gates.
  */
```

Then replace

```scala

  val buildPeek: Build = (_, ready, actor, args) =>
    for
      _ <- started(ready, actor, args, ActionRef.PeekSiteRelics)
      site <- pawnSite(ready, actor)
      _ <- Either.cond(site.relics.nonEmpty, (),
        MinorActionUnavailable("the pawn's site has no relics"))
```

with

```scala

  val buildPeek: Build = (_, ready, actor, args) =>
    for
      _ <- started(args, ActionRef.PeekSiteRelics)
      site <- pawnSite(ready, actor)
      _ <- Either.cond(site.relics.nonEmpty, (),
        MinorActionUnavailable("the pawn's site has no relics"))
```

Then replace

```scala

  val buildReveal: Build = (_, ready, actor, args) =>
    for
      _ <- started(ready, actor, args, ActionRef.RevealOwnedRelic)
      _ <- Either.cond(facedownRelics(ready, actor).nonEmpty, (),
        MinorActionUnavailable("no facedown relic to reveal"))
    yield revealTree(ready, actor)
```

with

```scala

  val buildReveal: Build = (_, ready, actor, args) =>
    for
      _ <- started(args, ActionRef.RevealOwnedRelic)
      _ <- Either.cond(facedownRelics(ready, actor).nonEmpty, (),
        MinorActionUnavailable("no facedown relic to reveal"))
    yield revealTree(ready, actor)
```

Then replace

```scala

  val buildMove: Build = (_, ready, actor, args) =>
    for
      _ <- started(ready, actor, args, ActionRef.MoveWarbands)
      _ <- pawnSite(ready, actor)
      blocked = Vector(toSite, toBoard).flatMap(way =>
        most(ready, actor, way).left.toOption)
```

with

```scala

  val buildMove: Build = (_, ready, actor, args) =>
    for
      _ <- started(args, ActionRef.MoveWarbands)
      _ <- pawnSite(ready, actor)
      blocked = Vector(toSite, toBoard).flatMap(way =>
        most(ready, actor, way).left.toOption)
```

Then replace

```scala
      .flatMap(_ => sitedPawn(ready, actor).map(_._2).toRight(
        MinorActionUnavailable("pawn site is not in play")))

  /** The legacy handler's gates: no start selection, and the first-game
    * rules. */
  private[minor] def started(ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef], action: ActionRef)
      : Either[OathViolation, Unit] =
    for
      _ <- Either.cond(args.isEmpty, (), InvalidEventOrder(
        s"walker procedure ${action.key} takes no start selection, got " +
          args.map(_.kind).mkString(", ")))
      _ <- Either.cond(supported(ready), (), MinorActionUnavailable(
        "minor actions are limited to the fixed unaltered foundations"))
    yield ()

  private def supported(ready: ReadyGame): Boolean =
    ready.setup.foundationProfile ==
      FirstGameFoundationProfile.FixedUnaltered &&
      ready.game.campaign.foundations.values.forall(foundation =>
        foundation.face == FoundationFace.Normal &&
          foundation.alterationSources.isEmpty)
```

with

```scala
      .flatMap(_ => sitedPawn(ready, actor).map(_._2).toRight(
        MinorActionUnavailable("pawn site is not in play")))

  /** The legacy handler's gate: a start takes no selection. Foundation
    * faces gate nothing here; game start refuses the ones that do not work
    * yet (`StartingFoundations`). */
  private[minor] def started(args: Vector[DecisionOptionRef],
      action: ActionRef): Either[OathViolation, Unit] =
    Either.cond(args.isEmpty, (), InvalidEventOrder(
      s"walker procedure ${action.key} takes no start selection, got " +
        args.map(_.kind).mkString(", ")))
```

In `src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala`:

Replace

```scala

  val build: MinorActionProcedures.Build = (_, ready, actor, args) =>
    for
      _ <- MinorActionProcedures.started(ready, actor, args,
        ActionRef.TransferWarbands)
      _ <- Either.cond(Imperials.isImperial(ready, actor), (),
        MinorActionUnavailable("Only an Imperial player transfers warbands"))
      _ <- Either.cond(partners(ready, actor).nonEmpty, (),
```

with

```scala

  val build: MinorActionProcedures.Build = (_, ready, actor, args) =>
    for
      _ <- MinorActionProcedures.started(args, ActionRef.TransferWarbands)
      _ <- Either.cond(Imperials.isImperial(ready, actor), (),
        MinorActionUnavailable("Only an Imperial player transfers warbands"))
      _ <- Either.cond(partners(ready, actor).nonEmpty, (),
```

In `src/main/scala/distantlands/gameplay/actions/minor/SelfExile.scala`:

Replace

```scala
      .fold(0)(_.board.warbands))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) => for
    _ <- MinorActionProcedures.started(ready, actor, args,
      ActionRef.SelfExile)
    _ <- Either.cond(Imperials.role(ready, actor).contains(Role.Citizen), (),
      MinorActionUnavailable("Only a Citizen exiles themself"))
    chancellor <- Imperials.chancellor(ready).toRight(
```

with

```scala
      .fold(0)(_.board.warbands))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) => for
    _ <- MinorActionProcedures.started(args, ActionRef.SelfExile)
    _ <- Either.cond(Imperials.role(ready, actor).contains(Role.Citizen), (),
      MinorActionUnavailable("Only a Citizen exiles themself"))
    chancellor <- Imperials.chancellor(ready).toRight(
```

- [ ] **Step 4: Run the minor-action suites to make sure they pass**

Run: `./sbtw "testOnly distantlands.gameplay.WarbandTransfersSuite distantlands.gameplay.MinorActionsSuite distantlands.gameplay.SelfExileSuite distantlands.testkit.TableSuite"`
Expected: PASS.

- [ ] **Step 5: The suites that reached past the table**

Four suites turn foundations by hand under a comment that no Table step
reaches them. `RestSuite` turns one card, so it takes the new step. The
other three turn all six at once, or set sources, which the step does not
do: they keep their update, and their comments say why.

In `src/test/scala/distantlands/gameplay/RestSuite.scala`:

Replace

```scala
      face = PeoplesFavorFace.GrandCouncil).ready
    accepted(rest(Ready(banner), p1))

    val number = base.game.campaign.foundations.keys.head
    // No Table step reaches foundations.
    val foundation = base.updateCampaign(_.copy(foundations =
      base.game.campaign.foundations.updated(number,
        FoundationState(FoundationFace.Altered, Set.empty))))
    accepted(rest(Ready(foundation), p1))

  test("last player of round eight finishes the game by War Exhaustion, and " +
```

with

```scala
      face = PeoplesFavorFace.GrandCouncil).ready
    accepted(rest(Ready(banner), p1))

    val foundation = Table.from(base).foundation(FoundationNumber.III,
      FoundationFace.Altered).ready
    accepted(rest(Ready(foundation), p1))

  test("last player of round eight finishes the game by War Exhaustion, and " +
```

In `src/test/scala/distantlands/gameplay/RecoverProcedureSuite.scala`:

Replace

```scala
    val lineage = campaign.lineages(Look(base).player(actor).lineage)
    val altered = FoundationState(FoundationFace.Altered,
      Set(LegacyId("legacy:recover-unrelated")))
    // No Table step reaches lineage roles or foundations.
    val ready = base.updateCampaign(_.copy(
      lineages = campaign.lineages.updated(lineage.id,
        lineage.copy(role = Role.Chancellor)),
```

with

```scala
    val lineage = campaign.lineages(Look(base).player(actor).lineage)
    val altered = FoundationState(FoundationFace.Altered,
      Set(LegacyId("legacy:recover-unrelated")))
    // No Table step reaches lineage roles or a foundation's sources.
    val ready = base.updateCampaign(_.copy(
      lineages = campaign.lineages.updated(lineage.id,
        lineage.copy(role = Role.Chancellor)),
```

In `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`:

Replace

```scala
        ActionRef.Negotiation, WalkerPowers.empty))

  test("no first-game gate: an Imperial player or an altered Foundation still negotiates"):
    // No Table step reaches lineage roles or foundations.
    val ready = gathered.ready
    val lineage = ready.game.campaign.lineages(Look(ready).player(p2).lineage)
    val campaign = ready.game.campaign
```

with

```scala
        ActionRef.Negotiation, WalkerPowers.empty))

  test("no first-game gate: an Imperial player or an altered Foundation still negotiates"):
    // No Table step reaches lineage roles, and none turns all six
    // foundations at once.
    val ready = gathered.ready
    val lineage = ready.game.campaign.lineages(Look(ready).player(p2).lineage)
    val campaign = ready.game.campaign
```

In `src/test/scala/distantlands/gameplay/TravelProcedureSuite.scala`:

Replace

```scala

  test("role and Foundation changes do not gate Travel"):
    val ready = passRuled(travelBoard(supply = 7))
    // No Table step reaches lineage roles or foundations.
    val citizen = ready.updateCampaign(_.copy(lineages = ready.game.campaign.lineages.map {
        case (id, lineage) => id -> lineage.copy(role = Role.Citizen)
      }))
```

with

```scala

  test("role and Foundation changes do not gate Travel"):
    val ready = passRuled(travelBoard(supply = 7))
    // No Table step reaches lineage roles, and none turns all six
    // foundations at once.
    val citizen = ready.updateCampaign(_.copy(lineages = ready.game.campaign.lineages.map {
        case (id, lineage) => id -> lineage.copy(role = Role.Citizen)
      }))
```

Run: `./sbtw "testOnly distantlands.gameplay.RestSuite distantlands.gameplay.RecoverProcedureSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.TravelProcedureSuite"`
Expected: PASS. These tests passed before too: each already held that a
foundation's face gates nothing in its procedure.

- [ ] **Step 6: Write the failing test for the profile's removal**

Nothing reads `FirstGameFoundationProfile` now. In
`src/test/scala/distantlands/model/ReadyGameSuite.scala`:

Replace

```scala
        ForceKind.Bandit -> 24))

  test("start stocks the seated lineages, the given banks and the first " +
      "player, and starts with the fixed unaltered first-game profile and " +
      "no knowledge"):
    val banks = Suit.all.map(_ -> 3).toMap
    val started = ReadyGame.start(twoSeats,
      Map(playerId -> PlayerColor.Red, second.player -> PlayerColor.Blue),
```

with

```scala
        ForceKind.Bandit -> 24))

  test("start stocks the seated lineages, the given banks and the first " +
      "player, and starts with no knowledge"):
    val banks = Suit.all.map(_ -> 3).toMap
    val started = ReadyGame.start(twoSeats,
      Map(playerId -> PlayerColor.Red, second.player -> PlayerColor.Blue),
```

Then replace

```scala
    assertEquals(started.banks.warbandSupply.keySet, Set[ForceKind](
      ForceKind.Exile(lineageId), ForceKind.Exile(second.lineage),
      ForceKind.Bandit))
    // start is given no profile, so it defaults to FixedUnaltered, and the
    // knowledge of a new ReadyGame defaults to empty.
    assertEquals(started.setup,
      FirstGameSupportState(FirstGameFoundationProfile.FixedUnaltered,
        second.player))
    assertEquals(started.knowledge, CardKnowledge())
```

with

```scala
    assertEquals(started.banks.warbandSupply.keySet, Set[ForceKind](
      ForceKind.Exile(lineageId), ForceKind.Exile(second.lineage),
      ForceKind.Bandit))
    // The knowledge of a new ReadyGame defaults to empty.
    assertEquals(started.setup, FirstGameSupportState(second.player))
    assertEquals(started.knowledge, CardKnowledge())
```

- [ ] **Step 7: Run it to make sure it fails**

Run: `./sbtw "testOnly distantlands.model.ReadyGameSuite"`
Expected: compile failure in `ReadyGameSuite`: `FirstGameSupportState`
still takes the profile first.

- [ ] **Step 8: Delete the profile**

In `src/main/scala/distantlands/model/Setup.scala`:

Replace

```scala
    relicOrder: Vector[RelicId]
)

enum FirstGameFoundationProfile { case FixedUnaltered }

/** `ignoreCitizenshipPowers` is the first-game option that ignores
  * "become a Citizen" powers (CR p. 34, ambiguity F8). It is on by default,
  * and game start never turns it off; the Settings Feature phase will let
  * players change it. */
final case class FirstGameSupportState(
    foundationProfile: FirstGameFoundationProfile,
    firstPlayer: PlayerId,
    ignoreCitizenshipPowers: Boolean = true
)
```

with

```scala
    relicOrder: Vector[RelicId]
)

/** `ignoreCitizenshipPowers` is the first-game option that ignores
  * "become a Citizen" powers (CR p. 34, ambiguity F8). It is on by default,
  * and game start never turns it off; the Settings Feature phase will let
  * players change it. */
final case class FirstGameSupportState(
    firstPlayer: PlayerId,
    ignoreCitizenshipPowers: Boolean = true
)
```

In `src/main/scala/distantlands/model/GameStateProtocol.scala`:

Replace

```scala
      game: OathGame,
      colors: Map[PlayerId, PlayerColor],
      firstPlayer: PlayerId,
      favorBanks: Map[Suit, Int],
      profile: FirstGameFoundationProfile =
        FirstGameFoundationProfile.FixedUnaltered
  ): ReadyGame =
    ReadyGame(
      game,
      colors,
      FirstGameSupportState(profile, firstPlayer),
      MaterialBankState(
        favorBanks,
        MaterialBankState.printedWarbandSupply(
```

with

```scala
      game: OathGame,
      colors: Map[PlayerId, PlayerColor],
      firstPlayer: PlayerId,
      favorBanks: Map[Suit, Int]
  ): ReadyGame =
    ReadyGame(
      game,
      colors,
      FirstGameSupportState(firstPlayer),
      MaterialBankState(
        favorBanks,
        MaterialBankState.printedWarbandSupply(
```

Check that no reader is left:

```bash
git grep -n "FoundationProfile\|foundationProfile\|fixed unaltered" -- src shared frontend
```

Expected: no output.

- [ ] **Step 9: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.model.ReadyGameSuite distantlands.gameplay.WarbandTransfersSuite distantlands.gameplay.MinorActionsSuite distantlands.gameplay.SelfExileSuite distantlands.testkit.TableSuite"`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala src/main/scala/distantlands/gameplay/actions/minor/SelfExile.scala src/main/scala/distantlands/model/Setup.scala src/main/scala/distantlands/model/GameStateProtocol.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/testkit/TableSuite.scala src/test/scala/distantlands/gameplay/WarbandTransfersSuite.scala src/test/scala/distantlands/model/ReadyGameSuite.scala src/test/scala/distantlands/gameplay/RestSuite.scala src/test/scala/distantlands/gameplay/RecoverProcedureSuite.scala src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala src/test/scala/distantlands/gameplay/TravelProcedureSuite.scala
git commit -m "$(cat <<'EOF'
feat(foundations): let minor actions run under an altered foundation

The minor actions no longer refuse a game whose foundations are
altered or sourced: game start is where an unsupported face is
refused. FirstGameFoundationProfile had no other reader and goes. The
test table gains a foundation step.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: The Chronicle record carries the game's faces

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala`

**Interfaces:**
- Consumes: `Table.foundation` (Task 3), `GameStartRules.evolve` keeping
  faces (Task 2).
- Produces:
  - `ChronicleRecord.campaignFields(ready).foundations ==
    ready.game.campaign.foundations`.
  - `EvolvedChronicleFixture.gameWonFrom(wakes: OathState): OathTransition`,
    `chronicleAnsweredAfter(won: OathTransition): OathTransition`,
    `chronicleJournaledBy(done: OathTransition): Chronicle`. The existing
    `won`, `done` and `record` keep their values.

- [ ] **Step 1: Write the failing tests**

In `src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala`:

Replace

```scala
import distantlands.testkit.Table.{p1, p2, p3}

/** What the journaled Chronicle carries from the campaign: the live roles,
  * era scores, Oathkeeper goal and starting advisers over the baseline.
  * `ChronicleExtraction` supplies the table fields. */
class ChronicleRecordSuite extends munit.FunSuite:
  private val boxed = LineageId("lineage:boxed")
```

with

```scala
import distantlands.testkit.Table.{p1, p2, p3}

/** What the journaled Chronicle carries from the campaign: the live roles,
  * era scores, Oathkeeper goal, foundation faces and starting advisers over
  * the baseline.
  * `ChronicleExtraction` supplies the table fields. */
class ChronicleRecordSuite extends munit.FunSuite:
  private val boxed = LineageId("lineage:boxed")
```

Then replace

```scala
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.copy(era = baseline.era,
      oathkeeperGoal = baseline.oathkeeperGoal,
      lineages = baseline.lineages), baseline)

  test("a seated lineage takes the starting advisers this Chronicle kept"):
    val start = Table.start.ready
    val lineage = lineageOf(start, p2)
```

with

```scala
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.copy(era = baseline.era,
      oathkeeperGoal = baseline.oathkeeperGoal,
      foundations = baseline.foundations,
      lineages = baseline.lineages), baseline)

  test("the record keeps the live foundation faces, a changed one and its " +
      "sources included"):
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy.id))
    val ready = Table.start
      .foundation(FoundationNumber.III, FoundationFace.Altered)
      .update(_.updateCampaign(campaign => campaign.copy(foundations =
        campaign.foundations.updated(FoundationNumber.VI, sourced)))).ready
    // The baseline lists no foundation: the record's faces are the game's.
    assertEquals(ready.game.campaign.baseline.foundations, Map.empty)
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    assertEquals(ChronicleRecord.campaignFields(ready).foundations,
      FoundationNumber.all.map(_ -> normal).toMap
        .updated(FoundationNumber.III,
          FoundationState(FoundationFace.Altered, Set.empty))
        .updated(FoundationNumber.VI, sourced))

  test("a seated lineage takes the starting advisers this Chronicle kept"):
    val start = Table.start.ready
    val lineage = lineageOf(start, p2)
```

In `src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala`,
split the three values into builders a second game can call:

Replace

```scala
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  /** The Rest that ends the game, up to Start Chronicle. */
  lazy val won: OathTransition = accepted(rules.next(
    OathTransition(usurperWakes, Vector.empty), PhaseTransitionRef.FinishRest))

  /** Start Chronicle and every task, answered. */
  lazy val done: OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.resolveWalker(
      won.state, p1, ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start))))

  /** The Chronicle the walk journals. */
  lazy val record: Chronicle = RecordedOps.all(done.events).collect {
    case ChronicleUpdated(chronicle) => chronicle
  }.last
```

with

```scala
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  /** The Rest that ends the game, up to Start Chronicle. */
  lazy val won: OathTransition = gameWonFrom(usurperWakes)

  /** Start Chronicle and every task, answered. */
  lazy val done: OathTransition = chronicleAnsweredAfter(won)

  /** The Chronicle the walk journals. */
  lazy val record: Chronicle = chronicleJournaledBy(done)

  /** The Rest that ends the game `wakes` begins, up to Start Chronicle.
    * p1 must wake as the Usurper there, as in [[usurperWakes]]. */
  def gameWonFrom(wakes: OathState): OathTransition = accepted(rules.next(
    OathTransition(wakes, Vector.empty), PhaseTransitionRef.FinishRest))

  /** Start Chronicle and every task of the game `won` ended, answered. */
  def chronicleAnsweredAfter(won: OathTransition): OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.resolveWalker(
      won.state, p1, ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start))))

  /** The Chronicle the answered walk `done` journals. */
  def chronicleJournaledBy(done: OathTransition): Chronicle =
    RecordedOps.all(done.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.last
```

In `src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala`:

Replace

```scala
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.testkit.{Outcomes, Situation}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted
```

with

```scala
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.testkit.{Outcomes, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted
```

Then replace

```scala
    assertEquals(woken.ready.game.current.tracks.round, 1)
    assert(woken.ready.game.current.players.forall(_.pawnSite.nonEmpty))

  test("game start refuses a crowned Chancellor whose Empire has no site"):
    val withoutEmpire = record.copy(atlasBox = record.world ++ record.atlasBox,
      world = Vector.empty)
```

with

```scala
    assertEquals(woken.ready.game.current.tracks.round, 1)
    assert(woken.ready.game.current.players.forall(_.pawnSite.nonEmpty))

  test("a foundation face game 1 holds is journaled, and game 2 starts " +
      "with it"):
    val altered = FoundationState(FoundationFace.Altered, Set.empty)
    // The fixture's game, with Foundation III turned before p1 wakes.
    val alteredWakes = Table.from(Outcomes.ready(usurperWakes))
      .foundation(FoundationNumber.III, FoundationFace.Altered).state
    val journaled = EvolvedChronicleFixture.chronicleJournaledBy(
      EvolvedChronicleFixture.chronicleAnsweredAfter(
        EvolvedChronicleFixture.gameWonFrom(alteredWakes)))
    assertEquals(journaled.foundations.get(FoundationNumber.III),
      Some(altered))
    val next = accepted(GameStartRules.evolve(catalog, journaled,
      LaterGameFixture.orders(journaled, p1)))
    assertEquals(next.game.campaign.foundations(FoundationNumber.III),
      altered)

  test("game start refuses a crowned Chancellor whose Empire has no site"):
    val withoutEmpire = record.copy(atlasBox = record.world ++ record.atlasBox,
      world = Vector.empty)
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleRecordSuite distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: two tests FAIL. "the record keeps the live foundation faces"
gets an empty map, and "a foundation face game 1 holds is journaled"
gets `None` for Foundation III. Every other test passes, the fixture's
builders included.

- [ ] **Step 3: Write the faces into the record**

In `src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala`:

Replace

```scala
import distantlands.model._

/** The Chronicle the Chronicle Phase journals (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores, the Oathkeeper goal and
  * starting advisers come from the live campaign; sites, decks, the
  * Dispossessed and the Reliquary from the table (`ChronicleExtraction`). */
object ChronicleRecord:
  /** The evolved Chronicle, when it passes game start's card accounting
    * and keeps every baseline site and relic. */
```

with

```scala
import distantlands.model._

/** The Chronicle the Chronicle Phase journals (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores, the Oathkeeper goal, the
  * foundation faces and starting advisers come from the live campaign;
  * sites, decks, the Dispossessed and the Reliquary from the table
  * (`ChronicleExtraction`). */
object ChronicleRecord:
  /** The evolved Chronicle, when it passes game start's card accounting
    * and keeps every baseline site and relic. */
```

Then replace

```scala
      .map(live => LineageState(live.id, seatedBy.get(live.id), live.role,
        Vector.empty, live.startingAdvisers, live.startingRelics))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal, lineages = kept ++ added)
```

with

```scala
      .map(live => LineageState(live.id, seatedBy.get(live.id), live.role,
        Vector.empty, live.startingAdvisers, live.startingRelics))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal,
      foundations = campaign.foundations, lineages = kept ++ added)
```

- [ ] **Step 4: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.application.chronicle.* distantlands.application.ChronicleProjectionSuite distantlands.serialization.*Chronicle*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala src/test/scala/distantlands/gameplay/chronicle/EvolvedChronicleFixture.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala
git commit -m "$(cat <<'EOF'
feat(foundations): journal the game's foundation faces in its Chronicle

The Chronicle record takes its foundations from the campaign, not the
baseline, so a face a game changes reaches the next game.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: The projection and the Chronicle summary carry the faces

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`
- Create: `src/main/scala/distantlands/application/FoundationPresentation.scala`
- Modify: `src/main/scala/distantlands/application/GameProjection.scala`
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/TrustedGameProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/FoundationProjectionSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala`

**Interfaces:**
- Consumes: `FoundationCards.face(number, which)` with `.name` and `.text`
  (Task 1), `Table.foundation` (Task 3).
- Produces:
  - `FoundationProjection(number: Int, face: String, name: String,
    rulesText: String)`; `face` is `normal` or `altered`.
  - `GameProjection.foundations: Vector[FoundationProjection]`, the last
    field, default empty; wire key `foundations`, required, each element
    `{"number","face","name","rulesText"}`.
  - `ChronicleFoundation(number: Int, face: String, name: String)`,
    `ChronicleFoundation.faces: Set[String]`.
  - `ChronicleSummary.foundations: Vector[ChronicleFoundation]`, the last
    field, default empty; wire key `summary.foundations`, always written,
    read as empty when absent, each element `{"number","face","name"}`.
  - `FoundationPresentation.faceKey(face: FoundationFace): String`,
    `FoundationPresentation.ofGame(foundations: Map[FoundationNumber,
    FoundationState]): Vector[FoundationProjection]`,
    `FoundationPresentation.ofChronicle(chronicle: Chronicle):
    Vector[ChronicleFoundation]`.

- [ ] **Step 1: Write the failing tests**

In `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`,
add two foundations to the fully populated fixture, so the existing round
trips carry them, and add the refusal test:

Replace

```scala
      rollOutcome = Some(WalkerRollOutcomeProjection("campaign.attack",
        Vector(DieFace.TwoSwordsSkull), 2, None, Vector("1 skull loss"))))),
    walkerWaiting = Some(WalkerWaitingProjection(Vector("blue"), Some("Choose the Oathkeeper"))),
    supplyMaximum = 7)


  test("the projection round trips its Chronicle, with and without the " +
```

with

```scala
      rollOutcome = Some(WalkerRollOutcomeProjection("campaign.attack",
        Vector(DieFace.TwoSwordsSkull), 2, None, Vector("1 skull loss"))))),
    walkerWaiting = Some(WalkerWaitingProjection(Vector("blue"), Some("Choose the Oathkeeper"))),
    supplyMaximum = 7,
    foundations = Vector(
      FoundationProjection(1, "normal", "Imperial Maps", "First.\n\nSecond."),
      FoundationProjection(3, "altered", "Public Ambitions",
        "Deal 4 legacy cards.")))


  test("the projection round trips its Chronicle, with and without the " +
```

Then replace

```scala
    assertRoundTrips(projection.copy(playerBoards = projection.playerBoards
      .map(_.copy(revealedVisions = Vector(faith, conquest)))))

  test("a board payload without its revealed Visions is refused"):
    val raw = ujson.read(GameProjectionCodec.encode(projection))
    raw("playerBoards").arr.foreach(board => board.obj.remove("revealedVisions"))
```

with

```scala
    assertRoundTrips(projection.copy(playerBoards = projection.playerBoards
      .map(_.copy(revealedVisions = Vector(faith, conquest)))))

  test("a payload without its foundations is refused, and so is a " +
      "foundation with a field it does not declare"):
    val missing = ujson.read(GameProjectionCodec.encode(projection))
    missing.obj.remove("foundations")
    assertEquals(
      Unwrap.rejected(GameProjectionCodec.decode(ujson.write(missing))).path,
      "$.foundations")
    val extra = ujson.read(GameProjectionCodec.encode(projection))
    extra("foundations")(1)("sources") = ujson.Arr()
    assertEquals(
      Unwrap.rejected(GameProjectionCodec.decode(ujson.write(extra))).path,
      "$.foundations[1].sources")

  test("a board payload without its revealed Visions is refused"):
    val raw = ujson.read(GameProjectionCodec.encode(projection))
    raw("playerBoards").arr.foreach(board => board.obj.remove("revealedVisions"))
```

In `shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala`:

Replace

```scala
    raw("summary")("scores")(0)("role") = "usurper"
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)

  test("an entry refuses an unknown field"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("extra") = 1
```

with

```scala
    raw("summary")("scores")(0)("role") = "usurper"
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)

  test("a summary's foundations round trip, and an entry saved before " +
      "they were kept has none"):
    val listed = entry.copy(summary = entry.summary.copy(foundations = Vector(
      ChronicleFoundation(1, "normal", "Imperial Maps"),
      ChronicleFoundation(3, "altered", "Public Ambitions"))))
    assertEquals(roundTripped(listed), Right(listed))
    val saved = ChronicleEntryCodec.encodeValue(listed)
    saved("summary").obj.remove("foundations")
    assertEquals(ChronicleEntryCodec.decodeValue(saved, "$"), Right(entry))

  test("a summary's foundation refuses an unknown face"):
    val raw = ChronicleEntryCodec.encodeValue(entry.copy(summary =
      entry.summary.copy(foundations = Vector(
        ChronicleFoundation(1, "normal", "Imperial Maps")))))
    raw("summary")("foundations")(0)("face") = "ruined"
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)

  test("an entry refuses an unknown field"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("extra") = 1
```

In `shared/src/test/scala/distantlands/protocol/TrustedGameProtocolSuite.scala`,
the summary's hand-written JSON gains the key:

Replace

```scala
      """{"playerId":"p2","url":"https://example.test/s/code2"},""" +
      """{"playerId":"p1","url":"https://example.test/s/code1"}],""" +
      """"chronicle":{"json":"{}","digest":"ab12","summary":""" +
      """{"name":"Ashen Crown","eraTarget":20,"scores":[],"game":1}}}"""

  test("response writes ordered game and seat links and decodes them back"):
    assertEquals(TrustedGameCreateResponseCodec.encode(seatResponse),
```

with

```scala
      """{"playerId":"p2","url":"https://example.test/s/code2"},""" +
      """{"playerId":"p1","url":"https://example.test/s/code1"}],""" +
      """"chronicle":{"json":"{}","digest":"ab12","summary":""" +
      """{"name":"Ashen Crown","eraTarget":20,"scores":[],""" +
      """"foundations":[],"game":1}}}"""

  test("response writes ordered game and seat links and decodes them back"):
    assertEquals(TrustedGameCreateResponseCodec.encode(seatResponse),
```

In `src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala`,
add the import and append the test:

Replace

```scala
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._

/** Digest and summary (Chronicle browsing design, "Digest" and "Summary"). */
class ChronicleEntriesSuite extends munit.FunSuite:
```

with

```scala
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._
import distantlands.protocol.ChronicleFoundation

/** Digest and summary (Chronicle browsing design, "Digest" and "Summary"). */
class ChronicleEntriesSuite extends munit.FunSuite:
```

Then replace

```scala
      lineage("yellow-lineage", Role.Exile))))
    assertEquals(summary.scores.map(_.role),
      Vector(Some("chancellor"), Some("citizen"), None))
```

with

```scala
      lineage("yellow-lineage", Role.Exile))))
    assertEquals(summary.scores.map(_.role),
      Vector(Some("chancellor"), Some("citizen"), None))

  test("a summary lists the six foundations by the face the next game " +
      "starts on, a number the Chronicle does not list as normal"):
    val summary = ChronicleEntries.summary(chronicle.copy(foundations = Map(
      FoundationNumber.III -> FoundationState(FoundationFace.Altered,
        Set.empty))))
    assertEquals(summary.foundations, Vector(
      ChronicleFoundation(1, "normal", "Imperial Maps"),
      ChronicleFoundation(2, "normal", "Powerful Tribes"),
      ChronicleFoundation(3, "altered", "Public Ambitions"),
      ChronicleFoundation(4, "normal", "Ties of Fellowship"),
      ChronicleFoundation(5, "normal", "Mob's Favor"),
      ChronicleFoundation(6, "normal", "Wandering Flame")))
```

Create `src/test/scala/distantlands/application/FoundationProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.catalog.FoundationCards
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.{FoundationFace, FoundationNumber, OathState}
import distantlands.protocol.projection.FoundationProjection
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The six foundation setup cards every seat sees (Foundations design,
  * slice 1, "Projection and UI"). */
class FoundationProjectionSuite extends munit.FunSuite:
  private val projector = GameProjector(catalog)

  test("every seat and the public view see the six cards, I to VI, each by " +
      "its face in use"):
    val state = Table.start.state
    val expected = Vector("Imperial Maps", "Powerful Tribes",
      "Quiet Ambitions", "Ties of Fellowship", "Mob's Favor",
      "Wandering Flame")
    val views = Vector(p1, p2).map(seat =>
      projector.project("g", LoadedGame(state, 1L), seat)) :+
      projector.projectPublic("g", LoadedGame(state, 1L))
    views.foreach { view =>
      assertEquals(view.foundations.map(_.number), Vector(1, 2, 3, 4, 5, 6))
      assertEquals(view.foundations.map(_.face), Vector.fill(6)("normal"))
      assertEquals(view.foundations.map(_.name), expected)
    }

  test("an altered card shows its altered face's name and text"):
    val state = Table.start.foundation(FoundationNumber.III,
      FoundationFace.Altered).state
    val third = projector.project("g", LoadedGame(state, 1L), p1)
      .foundations(2)
    assertEquals(third, FoundationProjection(3, "altered", "Public Ambitions",
      FoundationCards.face(FoundationNumber.III, FoundationFace.Altered)
        .text))

  test("a game not yet started shows no foundations"):
    assertEquals(projector.project("g", LoadedGame(OathState.NoGame, 0L),
      p1).foundations, Vector.empty)
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.protocol.ChronicleProtocolSuite distantlands.protocol.TrustedGameProtocolSuite distantlands.application.chronicle.ChronicleEntriesSuite distantlands.application.FoundationProjectionSuite"`
Expected: compile failure, `Not found: FoundationProjection` and
`Not found: ChronicleFoundation`.

- [ ] **Step 3: The DTOs**

In `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`:

Replace

```scala
    secrets: Int
)
final case class FavorBankProjection(suit: String, count: Int)
final case class GameTracksProjection(round: Int, visionsDrawn: Int,
    usurperLimited: Boolean, limiterRound: Int, firstPlayerId: String)
```

with

```scala
    secrets: Int
)
final case class FavorBankProjection(suit: String, count: Int)
/** One foundation setup card as the table shows it (Foundations design,
  * slice 1): its number, 1 to 6; the face in use, `normal` or `altered`;
  * that face's printed name; and its rules text in the catalog's markup,
  * a blank line between paragraphs. */
final case class FoundationProjection(number: Int, face: String, name: String,
    rulesText: String)
final case class GameTracksProjection(round: Int, visionsDrawn: Int,
    usurperLimited: Boolean, limiterRound: Int, firstPlayerId: String)
```

In `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`:

Replace

```scala
    ,consentPreferences: Vector[ConsentPreferencesProjection] = Vector.empty
    ,chronicle: Option[ChronicleProjection] = None
    ,planAidPrompt: Option[PlanAidPromptProjection] = None
)

/** Every seat's view of the game's Chronicle (Chronicle browsing design,
```

with

```scala
    ,consentPreferences: Vector[ConsentPreferencesProjection] = Vector.empty
    ,chronicle: Option[ChronicleProjection] = None
    ,planAidPrompt: Option[PlanAidPromptProjection] = None
    /** The six foundation setup cards, I to VI; empty before a game. */
    ,foundations: Vector[FoundationProjection] = Vector.empty
)

/** Every seat's view of the game's Chronicle (Chronicle browsing design,
```

- [ ] **Step 4: The projection codec**

In `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala`.
The key is required, so the decoder reads it with `array`, not `default`:

Replace

```scala
    "walkerDecision", "walkerWaiting", "viewerPlayerId",
    "supplyMaximum", "supplyBands", "undo", "redo", "logRevision",
    "consentWaiting", "consentPrompt", "consentPreferences", "chronicle",
    "planAidPrompt")

  def encode(value: GameProjection): String = ujson.write(encodeValue(value))
  def decode(json: String): Either[ProtocolDecodeFailure, GameProjection] =
```

with

```scala
    "walkerDecision", "walkerWaiting", "viewerPlayerId",
    "supplyMaximum", "supplyBands", "undo", "redo", "logRevision",
    "consentWaiting", "consentPrompt", "consentPreferences", "chronicle",
    "planAidPrompt", "foundations")

  def encode(value: GameProjection): String = ujson.write(encodeValue(value))
  def decode(json: String): Either[ProtocolDecodeFailure, GameProjection] =
```

Then replace

```scala
      ConsentProjectionCodec.encodePrompt),
    "consentPreferences" -> encoded(value.consentPreferences)(
      ConsentProjectionCodec.encodePreferences),
    "chronicle" -> option(value.chronicle)(encodeChronicle))
    value.viewerPlayerId.foreach(player => result("viewerPlayerId") = ujson.Str(player))
    value.planAidPrompt.foreach(prompt => result("planAidPrompt") =
      PlanAidProjectionCodec.encodePrompt(prompt))
```

with

```scala
      ConsentProjectionCodec.encodePrompt),
    "consentPreferences" -> encoded(value.consentPreferences)(
      ConsentProjectionCodec.encodePreferences),
    "chronicle" -> option(value.chronicle)(encodeChronicle),
    "foundations" -> encoded(value.foundations)(f => ujson.Obj(
      "number" -> f.number, "face" -> f.face, "name" -> f.name,
      "rulesText" -> f.rulesText)))
    value.viewerPlayerId.foreach(player => result("viewerPlayerId") = ujson.Str(player))
    value.planAidPrompt.foreach(prompt => result("planAidPrompt") =
      PlanAidProjectionCodec.encodePrompt(prompt))
```

Then replace

```scala
    chronicle <- optionalAbsent(value, "chronicle", path)(decodeChronicle)
    planAidPrompt <- optionalAbsent(value, "planAidPrompt", path)(
      PlanAidProjectionCodec.decodePrompt)
  yield GameProjection(game, sequence, phase, active, players, world, pawns,
    ready, completed, resources, siteResources,
    deckCount, deckTop, boards, oathkeeper, banners,
```

with

```scala
    chronicle <- optionalAbsent(value, "chronicle", path)(decodeChronicle)
    planAidPrompt <- optionalAbsent(value, "planAidPrompt", path)(
      PlanAidProjectionCodec.decodePrompt)
    foundationRaws <- array(value, "foundations", path)
    foundations <- traverse(foundationRaws, s"$path.foundations")(
      decodeFoundation)
  yield GameProjection(game, sequence, phase, active, players, world, pawns,
    ready, completed, resources, siteResources,
    deckCount, deckTop, boards, oathkeeper, banners,
```

Then replace

```scala
    walkerDecision, walkerWaiting, viewer,
    supplyMaximum, bands, undo, redo, logRevision,
    consentWaiting, consentPrompt, consentPreferences, chronicle,
    planAidPrompt)

  private def encodeChronicle(value: ChronicleProjection): ujson.Value =
    val result = ujson.Obj("name" -> value.name,
```

with

```scala
    walkerDecision, walkerWaiting, viewer,
    supplyMaximum, bands, undo, redo, logRevision,
    consentWaiting, consentPrompt, consentPreferences, chronicle,
    planAidPrompt, foundations)

  private def decodeFoundation(raw: ujson.Value, path: String)
      : Result[FoundationProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("number", "face", "name", "rulesText"), path)
    number <- int(row, "number", path); face <- string(row, "face", path)
    name <- string(row, "name", path)
    rulesText <- string(row, "rulesText", path)
  yield FoundationProjection(number, face, name, rulesText)

  private def encodeChronicle(value: ChronicleProjection): ujson.Value =
    val result = ujson.Obj("name" -> value.name,
```

- [ ] **Step 5: The Chronicle summary and its codec**

In `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`.
The decoder reads the key with `default`, so an entry a browser saved
earlier still decodes (design decision 3):

Replace

```scala
object ChronicleScore:
  val roles: Set[String] = Set("chancellor", "citizen")

/** What a browser lists for a Chronicle without decoding it (Chronicle
  * browsing design, "Summary"). `game` is the next game's number; it is
  * absent when the Chronicle has lineages but no scorepad, as a TTS import
  * does. */
final case class ChronicleSummary(name: String, game: Option[Int],
    eraTarget: Int, scores: Vector[ChronicleScore])

/** A Chronicle as a browser keeps it: canonical JSON, the JSON's SHA-256
  * digest, and its summary. */
```

with

```scala
object ChronicleScore:
  val roles: Set[String] = Set("chancellor", "citizen")

/** One foundation setup card as a Chronicle lists it (Foundations design,
  * slice 1): its number, 1 to 6; the face the next game starts on, `normal`
  * or `altered`; and that face's printed name. */
final case class ChronicleFoundation(number: Int, face: String, name: String)

object ChronicleFoundation:
  val faces: Set[String] = Set("normal", "altered")

/** What a browser lists for a Chronicle without decoding it (Chronicle
  * browsing design, "Summary"). `game` is the next game's number; it is
  * absent when the Chronicle has lineages but no scorepad, as a TTS import
  * does. `foundations` is the six setup cards, I to VI; an entry saved
  * before they were kept has none. */
final case class ChronicleSummary(name: String, game: Option[Int],
    eraTarget: Int, scores: Vector[ChronicleScore],
    foundations: Vector[ChronicleFoundation] = Vector.empty)

/** A Chronicle as a browser keeps it: canonical JSON, the JSON's SHA-256
  * digest, and its summary. */
```

Then replace

```scala
        val row = ujson.Obj("lineage" -> score.lineage, "score" -> score.score)
        score.role.foreach(role => row("role") = role)
        row
      })
    entry.summary.game.foreach(game => summary("game") = game)
    ujson.Obj("json" -> entry.json, "digest" -> entry.digest,
      "summary" -> summary)
```

with

```scala
        val row = ujson.Obj("lineage" -> score.lineage, "score" -> score.score)
        score.role.foreach(role => row("role") = role)
        row
      },
      "foundations" -> encoded(entry.summary.foundations)(foundation =>
        ujson.Obj("number" -> foundation.number, "face" -> foundation.face,
          "name" -> foundation.name)))
    entry.summary.game.foreach(game => summary("game") = game)
    ujson.Obj("json" -> entry.json, "digest" -> entry.digest,
      "summary" -> summary)
```

Then replace

```scala
      digest <- string(value, "digest", path)
      summaryRaw <- field(value, "summary", path)
      summary <- obj(summaryRaw, summaryPath)
      _ <- exact(summary, Set("name", "game", "eraTarget", "scores"),
        summaryPath)
      name <- string(summary, "name", summaryPath)
      game <- optionalAbsent(summary, "game", summaryPath)(int)
      target <- int(summary, "eraTarget", summaryPath)
```

with

```scala
      digest <- string(value, "digest", path)
      summaryRaw <- field(value, "summary", path)
      summary <- obj(summaryRaw, summaryPath)
      _ <- exact(summary, Set("name", "game", "eraTarget", "scores",
        "foundations"), summaryPath)
      name <- string(summary, "name", summaryPath)
      game <- optionalAbsent(summary, "game", summaryPath)(int)
      target <- int(summary, "eraTarget", summaryPath)
```

Then replace

```scala
              ProtocolDecodeFailure.InvalidValue(at, "unknown role")))
        yield ChronicleScore(lineage, score, role)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores))

/** `POST /chronicles/convert`'s body: a saved Chronicle's JSON, or a TTS
  * string with the name it is imported under. */
```

with

```scala
              ProtocolDecodeFailure.InvalidValue(at, "unknown role")))
        yield ChronicleScore(lineage, score, role)
      }
      // An entry a browser saved before foundations were kept has none.
      foundationRaws <- default(summary, "foundations", summaryPath,
        Vector.empty[ujson.Value])(array)
      foundations <- traverse(foundationRaws, s"$summaryPath.foundations") {
        (raw, child) =>
          for
            row <- obj(raw, child)
            _ <- exact(row, Set("number", "face", "name"), child)
            number <- int(row, "number", child)
            face <- string(row, "face", child).filterOrElse(
              ChronicleFoundation.faces, ProtocolDecodeFailure.InvalidValue(
                s"$child.face", "unknown face"))
            name <- string(row, "name", child)
          yield ChronicleFoundation(number, face, name)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores, foundations))

/** `POST /chronicles/convert`'s body: a saved Chronicle's JSON, or a TTS
  * string with the name it is imported under. */
```

- [ ] **Step 6: Project the faces**

Create `src/main/scala/distantlands/application/FoundationPresentation.scala`:

```scala
package distantlands.application

import distantlands.catalog.FoundationCards
import distantlands.model.{Chronicle, FoundationFace, FoundationNumber,
  FoundationState}
import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection

/** The six foundation setup cards as a client reads them (Foundations
  * design, slice 1): each by the face in use, named from
  * [[FoundationCards]]. */
object FoundationPresentation:
  /** The face's wire spelling. */
  def faceKey(face: FoundationFace): String = face match
    case FoundationFace.Normal => "normal"
    case FoundationFace.Altered => "altered"

  /** A game's six cards, I to VI, each with the text of its face in use. */
  def ofGame(foundations: Map[FoundationNumber, FoundationState])
      : Vector[FoundationProjection] =
    FoundationNumber.all.flatMap(number => foundations.get(number).map {
      state =>
        val printed = FoundationCards.face(number, state.face)
        FoundationProjection(number.value, faceKey(state.face), printed.name,
          printed.text)
    })

  /** A Chronicle's six cards, I to VI. A number the Chronicle does not
    * list lies normal, as game start reads it. */
  def ofChronicle(chronicle: Chronicle): Vector[ChronicleFoundation] =
    FoundationNumber.all.map { number =>
      val face = chronicle.foundations.get(number)
        .fold(FoundationFace.Normal)(_.face)
      ChronicleFoundation(number.value, faceKey(face),
        FoundationCards.face(number, face).name)
    }
```

In `src/main/scala/distantlands/application/GameProjection.scala`
(`FoundationPresentation` is in the same package, so it needs no import):

Replace

```scala
      relicDeckCount = current.commonCards.relicDeck.size)
      .copy(walkerDecision = pending.walkerDecision,
        chronicle = Some(chronicleView(context.ready.game.campaign)),
        walkerWaiting = pending.walkerWaiting,
        temporaryHandPreview = handPreview(context, pending.walkerDecision),
        supplyMaximum = SupplyTrack.Maximum,
```

with

```scala
      relicDeckCount = current.commonCards.relicDeck.size)
      .copy(walkerDecision = pending.walkerDecision,
        chronicle = Some(chronicleView(context.ready.game.campaign)),
        foundations = FoundationPresentation.ofGame(
          context.ready.game.campaign.foundations),
        walkerWaiting = pending.walkerWaiting,
        temporaryHandPreview = handPreview(context, pending.walkerDecision),
        supplyMaximum = SupplyTrack.Maximum,
```

In `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`:

Replace

```scala
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import distantlands.model.{Chronicle, Role}
import distantlands.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}
```

with

```scala
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import distantlands.application.FoundationPresentation
import distantlands.model.{Chronicle, Role}
import distantlands.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}
```

Then replace

```scala
          lineage.role match
            case Role.Chancellor => Some("chancellor")
            case Role.Citizen => Some("citizen")
            case Role.Exile => None)))
```

with

```scala
          lineage.role match
            case Role.Chancellor => Some("chancellor")
            case Role.Citizen => Some("citizen")
            case Role.Exile => None)),
      FoundationPresentation.ofChronicle(chronicle))
```

- [ ] **Step 7: The frontend's hand-written projection**

`HttpGameClientSuite.projectionJson` spells a projection by hand and now
lacks a required key. In
`frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala`:

Replace

```scala
       |],
       |"pawnLocations":[],
       |"ready":$ready,
       |"completed":$completed
       |}""".stripMargin

  private def siteJson(
```

with

```scala
       |],
       |"pawnLocations":[],
       |"ready":$ready,
       |"completed":$completed,
       |"foundations":[]
       |}""".stripMargin

  private def siteJson(
```

- [ ] **Step 8: Run the tests to make sure they pass**

Run: `./sbtw "testOnly distantlands.protocol.* distantlands.application.FoundationProjectionSuite distantlands.application.ChronicleProjectionSuite distantlands.application.chronicle.* distantlands.server.*" "frontend/testOnly distantlands.frontend.HttpGameClientSuite distantlands.frontend.ServerModeSessionSuite distantlands.protocol.ProjectionProtocolSuite distantlands.protocol.ChronicleProtocolSuite"`
Expected: PASS in both projects.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

- [ ] **Step 9: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala src/main/scala/distantlands/application/FoundationPresentation.scala src/main/scala/distantlands/application/GameProjection.scala src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala shared/src/test/scala/distantlands/protocol/ChronicleProtocolSuite.scala shared/src/test/scala/distantlands/protocol/TrustedGameProtocolSuite.scala src/test/scala/distantlands/application/FoundationProjectionSuite.scala src/test/scala/distantlands/application/chronicle/ChronicleEntriesSuite.scala frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala
git commit -m "$(cat <<'EOF'
feat(foundations): project the six foundation cards

The game projection lists each foundation by number, face, name and
rules text, and a Chronicle's summary by number, face and name. A
summary a browser saved earlier still decodes, with none.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: The foundation strip

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala`
- Modify: `frontend/styles.css`
- Modify: `DESIGN.md`
- Modify: `frontend/src/test/scala/distantlands/frontend/ChronicleFixtures.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/FoundationStripSuite.scala` (new)
- Test: `frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ChroniclePageSuite.scala`

**Interfaces:**
- Consumes: `FoundationProjection`, `GameProjection.foundations`,
  `ChronicleFoundation`, `ChronicleSummary.foundations` (Task 5);
  `CardInspection.openText(title, lines, origin)`, `HoverTip.set`,
  `ServerUiSupport.element`, `text`, `button` (existing).
- Produces:
  - `FoundationStrip.Card(number: Int, altered: Boolean, name: String,
    rules: Option[String])`.
  - `FoundationStrip.ofGame(Vector[FoundationProjection]): Vector[Card]`,
    `FoundationStrip.ofChronicle(Vector[ChronicleFoundation]):
    Vector[Card]`, `FoundationStrip.render(cards): dom.Element`,
    `FoundationStrip.numeral`, `title`, `label`.
  - DOM: `section.foundations` holding `h3.foundations-title` and
    `ol.foundation-strip`; each `li` holds one `.foundation-card`, a
    `button` with an `aria-label` when it has rules and a plain `span`
    with none when not, with `.foundation-number`, `.foundation-name` and,
    when altered, a `.foundation-mark`. An altered card has no class of
    its own.
  - `ChronicleFixtures.listingFoundations(entry, altered: Int*)`.

Read `DESIGN.md` before this task: "Primary" and "Named Rules" under
Colors, "Named Rules" under Typography, "Layout", and "Hover Note". The
strip follows them: no new color and no brass, since Brass on the board
means an option; Georgia for the card's name; no transition; a hover note
in place of `title`; one of the two named glance sizes on the compact map;
and nothing that changes the map's height.

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/FoundationStripSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, click, one}
import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection

/** The six foundation setup cards as a strip (Foundations design, slice 1):
  * read-only, the altered faces marked, and on the table a click reads a
  * face's rules.
  */
class FoundationStripSuite extends munit.FunSuite:
  private val projected = Vector(
    FoundationProjection(1, "normal", "Imperial Maps", "Fill the map."),
    FoundationProjection(2, "normal", "Powerful Tribes", "Place 3 [favor]."),
    FoundationProjection(3, "altered", "Public Ambitions",
      "Deal 4 legacy cards.\n\nEveryone competes."),
    FoundationProjection(4, "normal", "Ties of Fellowship", "Draw 3."),
    FoundationProjection(5, "normal", "Mob's Favor", "Set the Mob side."),
    FoundationProjection(6, "normal", "Wandering Flame", "Set the Flame."))

  /** The title and rules lines of every text the inspector was asked to
    * open, in order. */
  private var opened = Vector.empty[(String, Vector[String])]

  override def beforeEach(context: BeforeEach): Unit =
    opened = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _) =>
        opened :+= title -> lines
      case _ => ()

  override def afterEach(context: AfterEach): Unit = CardInspection.clear()

  private def tableStrip = FoundationStrip.render(
    FoundationStrip.ofGame(projected))

  test("the strip lists the six cards in order, each by its numeral and " +
      "the name of its face in use"):
    val strip = tableStrip
    assertEquals(strip.getAttribute("aria-label"), "Foundations")
    assertEquals(one(strip, ".foundations-title").textContent, "Foundations")
    assertEquals(all(strip, ".foundation-strip > li").size, 6)
    assertEquals(all(strip, ".foundation-number").map(_.textContent),
      Vector("I", "II", "III", "IV", "V", "VI"))
    assertEquals(all(strip, ".foundation-name").map(_.textContent),
      Vector("Imperial Maps", "Powerful Tribes", "Public Ambitions",
        "Ties of Fellowship", "Mob's Favor", "Wandering Flame"))

  test("an altered card is marked by word and label, never by a class of " +
      "its own; a normal card carries no mark"):
    val cards = all(tableStrip, ".foundation-card")
    assertEquals(cards.map(_.getAttribute("class")).distinct,
      Vector("foundation-card"))
    assertEquals(cards.map(card => all(card, ".foundation-mark")
      .map(_.textContent)), Vector(Vector.empty, Vector.empty,
      Vector("Altered"), Vector.empty, Vector.empty, Vector.empty))
    assertEquals(cards(2).getAttribute("aria-label"),
      "Foundation III, Public Ambitions, altered")
    assertEquals(cards(0).getAttribute("aria-label"),
      "Foundation I, Imperial Maps")
    assertEquals(cards(2).getAttribute(HoverTip.Attribute),
      "Foundation III · Public Ambitions")

  test("a click on a table card reads the rules of its face in use"):
    val cards = all(tableStrip, "button.foundation-card")
    assertEquals(cards.size, 6)
    assertEquals(cards.map(_.getAttribute("type")).distinct, Vector("button"))
    click(cards(2))
    assertEquals(opened, Vector("Foundation III · Public Ambitions" ->
      Vector("Deal 4 legacy cards.\n\nEveryone competes.")))

  test("a Chronicle's cards show the same names and marks and are not " +
      "buttons"):
    val strip = FoundationStrip.render(FoundationStrip.ofChronicle(Vector(
      ChronicleFoundation(1, "altered", "Wide Horizons"),
      ChronicleFoundation(2, "normal", "Powerful Tribes"))))
    assertEquals(all(strip, "button").size, 0)
    val cards = all(strip, "span.foundation-card")
    // A plain row is read as its text, so it carries no label to ignore.
    assertEquals(cards.map(_.hasAttribute("aria-label")),
      Vector(false, false))
    assertEquals(cards.map(card => all(card, "span").map(_.textContent)),
      Vector(Vector("I", "Wide Horizons", "Altered"),
        Vector("II", "Powerful Tribes")))
    click(cards.head)
    assertEquals(opened, Vector.empty)

  test("a number no card prints reads as its digits"):
    assertEquals(FoundationStrip.numeral(4), "IV")
    assertEquals(FoundationStrip.numeral(7), "7")
```

In `frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala`:

Replace

```scala
    * it is tall, so a column costs nothing at fit and a row below costs the
    * whole map's scale.
    */
  test("the shared bank stands beside the regions and their favor banks"):
    val layout = Dom.firstMatch(world(projection()), ".world-layout")
      .getOrElse(fail("no world layout"))
```

with

```scala
    * it is tall, so a column costs nothing at fit and a row below costs the
    * whole map's scale.
    */
  test("the foundations close the shared bank, and a projection with " +
      "none draws no strip"):
    val foundations = Vector(
      distantlands.protocol.projection.FoundationProjection(1, "normal",
        "Imperial Maps", "Fill the map."),
      distantlands.protocol.projection.FoundationProjection(3, "altered",
        "Public Ambitions", "Deal 4 legacy cards."))
    val bank = Dom.firstMatch(world(projection().copy(
      foundations = foundations)), ".shared-bank")
      .getOrElse(fail("no shared bank"))
    assertEquals(bank.lastElementChild.getAttribute("class"), "foundations")
    assertEquals(Dom.all(bank, ".foundation-name").map(_.textContent),
      Vector("Imperial Maps", "Public Ambitions"))
    assertEquals(Dom.all(world(projection()), ".foundations").size, 0)

  test("the shared bank stands beside the regions and their favor banks"):
    val layout = Dom.firstMatch(world(projection()), ".world-layout")
      .getOrElse(fail("no world layout"))
```

In `frontend/src/test/scala/distantlands/frontend/ChronicleFixtures.scala`:

Replace

```scala

import distantlands.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec, ChronicleConvertResponse,
  ChronicleConvertResponseCodec, ChronicleEntry, ChronicleScore,
  ChronicleSummary, ChronicleEntryCodec}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
```

with

```scala

import distantlands.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec, ChronicleConvertResponse,
  ChronicleConvertResponseCodec, ChronicleEntry, ChronicleFoundation,
  ChronicleScore, ChronicleSummary, ChronicleEntryCodec}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
```

Then replace

```scala
      ChronicleSummary(name, game, 20, Vector(
        ChronicleScore("red-lineage", 9), ChronicleScore("blue-lineage", 6))))

  def converted(entry: ChronicleEntry, tts: Option[String] = None)
      : TransportResponse =
    TransportResponse(200, ChronicleConvertResponseCodec.encode(
```

with

```scala
      ChronicleSummary(name, game, 20, Vector(
        ChronicleScore("red-lineage", 9), ChronicleScore("blue-lineage", 6))))

  /** `entry` listing the six foundations, those numbered in `altered` on
    * their altered face. */
  def listingFoundations(entry: ChronicleEntry, altered: Int*)
      : ChronicleEntry =
    val faces = Vector("Imperial Maps" -> "Wide Horizons",
      "Powerful Tribes" -> "Movers and Shakers",
      "Quiet Ambitions" -> "Public Ambitions",
      "Ties of Fellowship" -> "Teeming World",
      "Mob's Favor" -> "Grand Council", "Wandering Flame" -> "Festival")
    entry.copy(summary = entry.summary.copy(foundations =
      faces.zipWithIndex.map { case ((normal, turned), index) =>
        if altered.contains(index + 1) then
          ChronicleFoundation(index + 1, "altered", turned)
        else ChronicleFoundation(index + 1, "normal", normal)
      }))

  def converted(entry: ChronicleEntry, tts: Option[String] = None)
      : TransportResponse =
    TransportResponse(200, ChronicleConvertResponseCodec.encode(
```

In `frontend/src/test/scala/distantlands/frontend/ChroniclePageSuite.scala`:

Replace

```scala
      assert(!browser.byClass("export-chronicle-json").head
        .hasAttribute("aria-disabled"))

  test("the sections run in the spec's order"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
```

with

```scala
      assert(!browser.byClass("export-chronicle-json").head
        .hasAttribute("aria-disabled"))

  test("a loaded entry lists its six foundations, the altered ones " +
      "marked, and none is a button"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      accepted(new ChronicleShelf(storage, ChronicleFixtures.clock).put(
        ChronicleFixtures.listingFoundations(entry("Ashen Crown"), 3)))
      renderChroniclePage(browser, storage)
      val loaded = browser.byClass("chronicle-loaded").head
      assertEquals(Dom.all(loaded, ".foundation-name").map(_.textContent),
        Vector("Imperial Maps", "Powerful Tribes", "Public Ambitions",
          "Ties of Fellowship", "Mob's Favor", "Wandering Flame"))
      assertEquals(Dom.all(loaded, ".foundation-card")
        .filter(card => Dom.all(card, ".foundation-mark").nonEmpty)
        .map(card => Dom.one(card, ".foundation-number").textContent),
        Vector("III"))
      assertEquals(Dom.all(loaded, "button.foundation-card").size, 0)

  test("an entry saved before foundations were kept shows no strip"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
        "Ashen Crown"))
      assert(!shows(browser, "foundations"))

  test("the sections run in the spec's order"):
    TestBrowser.useSynchronously(): browser =>
      renderChroniclePage(browser, storageHolding(Some("Ashen Crown"),
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.FoundationStripSuite distantlands.frontend.PanelPlacementSuite distantlands.frontend.ChroniclePageSuite"`
Expected: compile failure, `Not found: FoundationStrip`.

- [ ] **Step 3: The strip**

Create `frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** The six foundation setup cards as a read-only strip (Foundations design,
  * slice 1): one row a card, its numeral, the name of the face in use and,
  * on an altered card, the word. The table's strip stands in the shared
  * bank, and a click on a card reads its face's rules in the overlay; the
  * Chronicle page lists the same six with no rules to read.
  */
private[frontend] object FoundationStrip:
  /** One card as the strip draws it. `rules` is the text of the face in
    * use, which a click reads; the Chronicle page carries none. */
  final case class Card(number: Int, altered: Boolean, name: String,
      rules: Option[String])

  private val Numerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** "IV" for 4; a number no card prints reads as its digits. */
  def numeral(number: Int): String =
    Numerals.lift(number - 1).getOrElse(number.toString)

  def ofGame(foundations: Vector[FoundationProjection]): Vector[Card] =
    foundations.map(foundation => Card(foundation.number,
      foundation.face == "altered", foundation.name,
      Some(foundation.rulesText)))

  def ofChronicle(foundations: Vector[ChronicleFoundation]): Vector[Card] =
    foundations.map(foundation => Card(foundation.number,
      foundation.face == "altered", foundation.name, None))

  /** "Foundation I · Wide Horizons": the overlay's title and the hover
    * note. */
  def title(card: Card): String =
    s"Foundation ${numeral(card.number)} · ${card.name}"

  /** "Foundation I, Wide Horizons, altered": a table card's label. */
  def label(card: Card): String =
    s"Foundation ${numeral(card.number)}, ${card.name}" +
      (if card.altered then ", altered" else "")

  def render(cards: Vector[Card]): dom.Element =
    val section = element("section", "foundations")
    section.setAttribute("aria-label", "Foundations")
    section.appendChild(text("h3", "foundations-title", "Foundations"))
    val strip = element("ol", "foundation-strip")
    cards.foreach { card =>
      val item = element("li", "")
      item.appendChild(face(card))
      strip.appendChild(item)
    }
    section.appendChild(strip)
    section

  private def face(card: Card): dom.Element =
    val node = card.rules match
      case Some(rules) =>
        val button = ServerUiSupport.button("", "foundation-card")
        button.`type` = "button"
        // A control is named in one phrase; its parts alone open with "I".
        button.setAttribute("aria-label", label(card))
        button.onclick = _ =>
          CardInspection.openText(title(card), Vector(rules), button)
        button
      // A plain row is read as its own text: numeral, name and mark. A
      // label on a span is ignored, so it carries none.
      case None => element("span", "foundation-card")
    // The name is cut to one line on a small map; the note keeps it whole.
    HoverTip.set(node, title(card))
    node.appendChild(text("span", "foundation-number", numeral(card.number)))
    node.appendChild(text("span", "foundation-name", card.name))
    if card.altered then
      node.appendChild(text("span", "foundation-mark", "Altered"))
    node
```

- [ ] **Step 4: Place it on the table and on the Chronicle page**

In `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`,
at the end of `sharedBank`:

Replace

```scala
      value.banners.foreach(banner =>
        banners.appendChild(BannerFace.place(banner, options)))
      section.appendChild(banners)
    section
```

with

```scala
      value.banners.foreach(banner =>
        banners.appendChild(BannerFace.place(banner, options)))
      section.appendChild(banners)
    // The setup cards close the bank: they are read once or twice a game,
    // so they stand under what changes every turn.
    if value.foundations.nonEmpty then
      section.appendChild(FoundationStrip.render(
        FoundationStrip.ofGame(value.foundations)))
    section
```

In `frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala`, in
`loadedSummary`:

Replace

```scala
    section.appendChild(heading)
    section.appendChild(text("p", "chronicle-summary-line",
      ChronicleText.summaryLine(saved.entry.summary)))
    section.appendChild(status)
    section
```

with

```scala
    section.appendChild(heading)
    section.appendChild(text("p", "chronicle-summary-line",
      ChronicleText.summaryLine(saved.entry.summary)))
    // The faces the next game starts on. An entry saved before foundations
    // were kept lists none, and shows no strip.
    val foundations = saved.entry.summary.foundations
    if foundations.nonEmpty then
      section.appendChild(FoundationStrip.render(
        FoundationStrip.ofChronicle(foundations)))
    section.appendChild(status)
    section
```

- [ ] **Step 5: Run the tests to make sure they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.FoundationStripSuite distantlands.frontend.PanelPlacementSuite distantlands.frontend.ChroniclePageSuite"`
Expected: PASS.

- [ ] **Step 6: The styles**

In `frontend/styles.css`, before the favor banks' rule:

Replace

```css
  gap: 10px; margin-top: 10px; }
.shared-banners .board-option-card { width: 100%; }
.shared-banners .banner-face { width: 100%; height: auto; aspect-ratio: 1; }
/* Under the regions, where the physical board keeps them. Their text is set
   larger than the map's own, as the map is drawn at about half size. */
.favor-banks { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr));
```

with

```css
  gap: 10px; margin-top: 10px; }
.shared-banners .board-option-card { width: 100%; }
.shared-banners .banner-face { width: 100%; height: auto; aspect-ratio: 1; }
/* The six foundation setup cards (Foundations design, slice 1), a card a
   row: its numeral, the name of the face in use in the serif that names
   things, and on an altered card the word. The word alone marks it: brass
   on the board means an option, so the frame stays the neutral line. A row
   is one line of name and at most one of mark, so the strip's height is
   fixed and can never raise the map's. The table's cards are buttons that
   read the face's rules; the Chronicle page's are not. */
.foundations { margin-top: 12px; }
.foundations-title { margin: 0 0 6px; font-size: 1rem; }
.foundation-strip { list-style: none; margin: 0; padding: 0; display: grid;
  gap: 4px; }
.foundation-card { box-sizing: border-box; display: grid;
  grid-template-columns: 1.8em minmax(0, 1fr); column-gap: 6px;
  align-items: baseline; width: 100%; padding: 4px 8px;
  border: 1px solid var(--line); border-radius: 4px; background: var(--base);
  color: var(--cream); font: inherit; text-align: left; }
button.foundation-card { cursor: pointer; }
button.foundation-card:hover { background: var(--pressed);
  border-color: var(--cream-bright); }
.foundation-number { color: var(--ink-dim); font-weight: 700;
  text-align: center; }
.foundation-name { font-family: Georgia, serif; font-weight: 700;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.foundation-mark { grid-column: 2; color: var(--parchment-dim);
  font-size: max(0.72em, 0.7rem); font-weight: 700; letter-spacing: 0.1em;
  text-transform: uppercase; }
/* On the zoomed-out map the cards hold the glance text size, as a site's
   details do; a name the bank's column cannot hold is cut, never wrapped. */
.map-compact .foundation-card { font-size: var(--glance-text); }
/* On the Chronicle page the six stand three abreast. */
.chronicle-loaded .foundation-strip {
  grid-template-columns: repeat(auto-fill, minmax(11rem, 1fr)); gap: 0.5rem; }
/* Under the regions, where the physical board keeps them. Their text is set
   larger than the map's own, as the map is drawn at about half size. */
.favor-banks { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr));
```

Every custom property used here is already declared on `:root`. The
table's focus ring comes from the existing `.game-table
button:focus-visible` rule. Do not let a row wrap or grow, and do not
give the strip a compact size of its own: a taller shared bank can raise
the map's height and move its Fit scale (design decision 5, and the
measurements in Self-review).

- [ ] **Step 7: Record the component in the design system**

In `DESIGN.md`, after "Player Boards":

Replace

~~~markdown
figure and a region's discard pile are buttons on the pill hover that open
the text and card-list overlays: Supply at Rest and the pile's cards.

## Do's and Don'ts

### Do:
~~~

with

~~~markdown
figure and a region's discard pile are buttons on the pill hover that open
the text and card-list overlays: Supply at Rest and the pile's cards.

### Foundation Strip
The six foundation setup cards, read-only (`FoundationStrip`). On the table
they close the shared bank's column under the banners, one row a card: Base
fill, 1px Line border, 4px radius, `4px 8px` padding, the numeral in Ink Dim
700 and the name of the face in use in Georgia 700. The name holds one line
and is cut with an ellipsis where the column is too narrow; the whole name
stays in the hover note and, on the table, in the row's label. An altered
card adds the word "Altered" under its name, in Parchment Dim uppercase at
`max(0.72em, 0.7rem)`, 700, 0.1em tracking. The word alone marks it: the
frame stays Line, because brass on the board means an option. A row is a
button that opens its face's rules in the text overlay, and answers hover as
a Control does (Pressed fill, Bright Cream border). On the compact map the
row takes the glance text size (`--glance-text`), as a site's details do. A
row never wraps, so the strip's height is fixed and cannot raise the map's
(The Glance Layer Rule). The Chronicle page lists the same six under the
loaded Chronicle's summary line, three abreast, as plain rows with nothing
to open.

## Do's and Don'ts

### Do:
~~~

- [ ] **Step 8: Run the frontend suites and the boundary check**

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/FoundationStrip.scala frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala frontend/styles.css DESIGN.md frontend/src/test/scala/distantlands/frontend/ChronicleFixtures.scala frontend/src/test/scala/distantlands/frontend/FoundationStripSuite.scala frontend/src/test/scala/distantlands/frontend/PanelPlacementSuite.scala frontend/src/test/scala/distantlands/frontend/ChroniclePageSuite.scala
git commit -m "$(cat <<'EOF'
feat(frontend): show the six foundation cards

The shared bank closes with a strip of the six foundation setup cards,
the altered ones marked, and a click reads a face's rules. The
Chronicle page lists the same six for the loaded Chronicle. Both are
read only.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Docs and verification

**Files:**
- Modify: `docs/architecture/game-setup.md`
- Modify: `docs/architecture/bounded-visions-and-conspiracy.md`
- Modify: `docs/architecture/all-exile-round-endings.md`
- Modify: `docs/superpowers/specs/2026-11-11-foundations-design.md`

The spec leaves the batch's rules docs, roadmap and `CONTEXT.md` to slice
7. This task corrects only what slice 1 made false.

- [ ] **Step 1: The architecture docs**

In `docs/architecture/game-setup.md`:

Replace

~~~markdown
The completed aggregate includes site resources and bandits, facedown site
relics, ruined Homeland edifices, regional discards, player advisers, World and
relic decks, starting Exile boards and Supply, banners, tracks, the Oathkeeper
goal, favor banks, and six unaltered Foundations.

The scope follows Combined Rulebook pp. 6-7 and the New Foundations first-game
clarification on p. 8. It excludes Legacy selection/effects,
Chancellor/Citizen/Imperial setup, restoring the rest of a stored campaign
(roles, legacies, the Empire's sites), and altered Foundations.

## Application and transport
~~~

with

~~~markdown
The completed aggregate includes site resources and bandits, facedown site
relics, ruined Homeland edifices, regional discards, player advisers, World and
relic decks, starting Exile boards and Supply, banners, tracks, the Oathkeeper
goal, favor banks, and the Chronicle's six Foundation faces
(`StartingFoundations`; a number the Chronicle does not list lies normal).

The scope follows Combined Rulebook pp. 6-7 and the New Foundations first-game
clarification on p. 8. It excludes Legacy selection/effects,
Chancellor/Citizen/Imperial setup, restoring the rest of a stored campaign
(roles, legacies, the Empire's sites), and the altered Foundation faces no
slice of the
[Foundations design](../superpowers/specs/2026-11-11-foundations-design.md)
has brought in yet, which game start refuses by name. Foundation III is
accepted on either face and does nothing until Legacies.

## Application and transport
~~~

In `docs/architecture/bounded-visions-and-conspiracy.md`:

Replace

~~~markdown

Altered Foundations, Empire-only Vision rules, and the alternate banner faces
(Grand Council, Festival) have no behaviour. `RuleSourceIndex` lists the
handler ids of those faces and of an altered Foundation. An applicable handler
the engine does not support is ignored, not blocking, and nothing records
it.
~~~

with

~~~markdown

Altered Foundations, Empire-only Vision rules, and the alternate banner faces
(Grand Council, Festival) have no behaviour. `RuleSourceIndex` lists the
handler ids of those faces and of each Foundation's face in use
(`catalog.FoundationCards`). An applicable handler
the engine does not support is ignored, not blocking, and nothing records
it.
~~~

In `docs/architecture/all-exile-round-endings.md`:

Replace

~~~markdown
nothing about it. Mechanics are never inferred from
rules text or handler-name fragments. Banner faces and Foundations are rule
sources in `RuleSourceIndex` like any card. The Grand Council and Festival
banner faces and an altered Foundation have no implemented power, so their
handlers are ignored and recorded the same way at Rest start.
~~~

with

~~~markdown
nothing about it. Mechanics are never inferred from
rules text or handler-name fragments. Banner faces and Foundations are rule
sources in `RuleSourceIndex` like any card. The Grand Council and Festival
banner faces and the Foundation faces have no implemented power, so their
handlers are ignored and recorded the same way at Rest start.
~~~

- [ ] **Step 2: The spec**

In `docs/superpowers/specs/2026-11-11-foundations-design.md`: the status
line, the catalog sentence (a face's text follows its printed card, ruling
1) and the two sentences of slice 1 the code contradicts (plan-time
findings 1 and 5, design decision 1):

Replace

~~~markdown
# Foundations

**Status:** designed 2026-11-11.

**Builds on** the [Chronicle steps design](2026-10-27-chronicle-steps-design.md),
the [Chronicle setup design](2026-09-21-chronicle-setup-design.md) and
~~~

with

~~~markdown
# Foundations

**Status:** designed 2026-11-11. Slice 1 landed.

**Builds on** the [Chronicle steps design](2026-10-27-chronicle-steps-design.md),
the [Chronicle setup design](2026-09-21-chronicle-setup-design.md) and
~~~

Then replace

~~~markdown
  with "minor actions are limited to the fixed unaltered foundations".
  `FirstGameFoundationProfile` goes too if nothing else reads it.
- **Catalog.** `FoundationCards` lists I to VI, each face with its printed
  name and its text from the rulebook's Foundations section:

  | | Normal | Altered |
  |---|---|---|
~~~

with

~~~markdown
  with "minor actions are limited to the fixed unaltered foundations".
  `FirstGameFoundationProfile` goes too if nothing else reads it.
- **Catalog.** `FoundationCards` lists I to VI, each face with its printed
  name and its text as the setup card prints it:

  | | Normal | Altered |
  |---|---|---|
~~~

Then replace

~~~markdown
  | VI | Wandering Flame | Festival |

  `RuleSourceIndex` replaces the generic `foundation.altered` id with one
  id per face (`foundation.i.wide-horizons`), so `PrintedCards` names the
  card a line comes from.
- **Projection and UI.** The game projection gains `foundations`: each
  number, its face and the face's name. The game info area shows a strip of
  six setup cards with the altered faces marked, and the Chronicle page lists
  the same six. Both are read-only; editing waits for **Edit Chronicle**.
- **Tests.** Game start keeps stored faces and refuses the gated ones;
  minor actions run under an altered III; the projection and its codec round
  trip; the TTS mask still imports to faces; a frontend suite for the strip;
~~~

with

~~~markdown
  | VI | Wandering Flame | Festival |

  `RuleSourceIndex` replaces the generic `foundation.altered` id with one
  id per face, normal faces included (`foundation.i.wide-horizons`), so
  `LogWords.power` names the face a line comes from. `PrintedCards` names
  only cards, and a foundation is none.
- **Projection and UI.** The game projection gains `foundations`: each
  number, its face, the face's name and its rules text. The shared bank, the
  table's game info column, shows a strip of six setup cards with the altered
  faces marked; a click reads a face's rules. The Chronicle page lists the
  same six from the Chronicle's summary. Both are read-only; editing waits
  for **Edit Chronicle**.
- **Tests.** Game start keeps stored faces and refuses the gated ones;
  minor actions run under an altered III; the projection and its codec round
  trip; the TTS mask still imports to faces; a frontend suite for the strip;
~~~

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92% and branch coverage at
or above 84.4%.

Run: `./sbtw "frontend/test"` (the frontend suites, if the full run
skipped them)
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: no violation; every Scala file stays under 800 lines.

Run: `git status --short src/test/resources`
Expected: clean. No Game Log golden and no note-key snapshot changes in
this slice.

Run: `git grep -n "foundation\.altered\|FoundationProfile" -- src shared frontend`
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/game-setup.md docs/architecture/bounded-visions-and-conspiracy.md docs/architecture/all-exile-round-endings.md docs/superpowers/specs/2026-11-11-foundations-design.md
git commit -m "$(cat <<'EOF'
docs(foundations): record slice 1, faces reach the game

The setup doc says where a game's foundation faces come from and which
are refused, and the spec records slice 1 landed and names where the
strip and the Game Log's face names live.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage, slice 1.** Game start copies the Chronicle's faces, a
  missing number normal, sources kept, and refuses an altered I, II, IV, V
  or VI by face and slice while accepting III (Task 2). The Chronicle
  record writes `campaign.foundations` (Task 4). The minor-action gate and
  `FirstGameFoundationProfile` go (Task 3). `FoundationCards` lists I to VI
  with names and printed text, and `RuleSourceIndex` carries one id per
  face (Task 1). The projection gains `foundations` (Task 5); the strip in
  the shared bank and the six on the Chronicle page, read only (Task 6).
  The spec's tests: game start keeps stored faces and refuses the gated
  ones (Task 2); minor actions run under an altered III and under a
  sourced normal face (Task 3); the
  projection and its codec round trip (Task 5); the TTS mask still imports
  to faces (Task 2, beside the existing round trip); a frontend suite for
  the strip (Task 6); the Chronicle record keeps a changed face (Task 4,
  and a game journaled into the next game's start).
- **Seams for later slices.** Slices 2, 3, 5 and 6 each delete a line of
  `StartingFoundations.arrivesWith` and its row of the refusal test. Slice
  3 changes `StartingFoundations.of` for erosion and moves the refusal
  below `EmpireSetupRules.resolve`. Slice 7 reads
  `campaign.foundations` and names a restored face from
  `FoundationCards`. The strip redraws from the projection, so a face that
  changes in play needs no frontend work. Slices 3 to 6 start their tests
  from `Table.foundation`.
- **Tests later slices must touch.** Slice 2 rewrites
  `GameStartRulesSuite`'s "with several altered faces, game start names the
  first in foundation order": it expects Foundation II's refusal, which
  slice 2 deletes. Slice 7's Shape Foundation question may change
  `ChronicleEndToEndSuite`'s "a foundation face game 1 holds is journaled,
  and game 2 starts with it": it answers every Chronicle task through
  `ChronicleWalk.answeringEverything` with Foundation III altered, and an
  answer that restores a face would journal III as normal.
- **Placeholders.** None.
- **Verified against a prototype.** Every block here was cut from a
  scratch worktree built on `8fc747b8c`, and applying the blocks to that
  commit in plan order reproduces the worktree file for file (54 files).
  There the gates of Task 7 passed: 5669 JVM tests, 923 frontend tests,
  statement coverage 95.22%, branch coverage 89.36%, the architecture check,
  and no change under `src/test/resources`. The five suites whose comments
  Tasks 1 and 3 correct were edited after that run and rerun on their own.
  After the plan's review the face texts, the strip's marking, labels and
  compact size, and the sourced-face test changed; the eight suites they
  touch were rerun (59 tests) and the architecture check passed, but the
  full gates were not run again.
  The revised strip was looked at on a scratch server. At 1024x768 (Fit
  0.34) the map's layout stayed as tall as its regions, 1278px, against a
  778px shared bank with two cards marked altered; the glance text is
  11.6px on screen there and cuts every name but the shortest. At 1440x900
  (Fit 0.51) all twelve names fit their row. A click opened a face's rules
  in the text overlay, and the Chronicle page drew the six three abreast
  (seen before the review's changes). The prototype ran each task's tests
  after its
  code, so the "make sure they fail" outputs are read from the code, not
  observed. If a red step fails for another reason, trust the run and say
  so in the task's report.
- **Type consistency.** `FoundationCardFace(id, name, text)`;
  `FoundationCard(number, normal, altered).face(which)`;
  `FoundationCards.all`, `card`, `face(number, which)`, `faceWithId`;
  `StartingFoundations.of`, `refusal`; `Table.foundation(number, face)`;
  `MinorActionProcedures.started(args, action)`;
  `FirstGameSupportState(firstPlayer, ignoreCitizenshipPowers)`;
  `EvolvedChronicleFixture.gameWonFrom`, `chronicleAnsweredAfter`,
  `chronicleJournaledBy`; `FoundationProjection(number, face, name,
  rulesText)`; `ChronicleFoundation(number, face, name)`;
  `FoundationPresentation.faceKey`, `ofGame`, `ofChronicle`;
  `FoundationStrip.Card(number, altered, name, rules)`, `ofGame`,
  `ofChronicle`, `render`, `numeral`, `title`, `label`;
  `ChronicleFixtures.listingFoundations`. Each is used with the same name
  and shape in every task that reads it.
