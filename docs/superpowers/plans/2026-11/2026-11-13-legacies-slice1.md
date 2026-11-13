# Legacies, Slice 1: Data and State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Legacies become cards the engine can hold, move and show. Every
legacy carries its printed goal and suit, and six carry the foundation they
alter. The legacy deck stops being stored and is derived. A public row
exists for Public Ambitions. `Move` and `Flip` reach a lineage's legacies
and the public row. A new operation, `FoundationAltered`, records that a
legacy alters a foundation. Game start accepts a seated lineage's legacies
and starting relics instead of refusing them. A dormant legacy is
identified only to its owner. Nothing yet deals, resolves or scores a
legacy; slices 2 to 4 do.

**Architecture:** The catalog's `Legacy` gains `goal`, `suit` and `alters`.
`CardZones.legacyDeck` is deleted and `CardZones.publicLegacies` added, with
`CardContainer.PublicLegacies`. Two new `Location`s,
`Location.Legacies(lineage)` and `Location.PublicLegacies`, give the card
movement code its write paths; a legacy moved to `Location.Deck(CardDeck.Legacy)`
leaves play. `Flip` sets `LegacyState.active`. `FoundationAltered` is a new
case of the `ChronicleOperation` family. `LaterGameSetup` moves each seated
lineage's starting relics to its player facedown.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`;
Python 3 for one data rewrite and `scripts/check-architecture.py`.

**Spec:** [Legacies design](../../specs/2026-11/2026-11-13-legacies-design.md),
sections "Rulings" (1, 3 and 8), "Approach", "Card data" and "Slice 1: data
and state". Read the
[operation families design](../../specs/2026-11/2026-11-12-operation-families-design.md),
"The families", "The applier" and "The codec", for where a new operation
goes.

## Prerequisite

The [operation families plan](2026-11-12-operation-families.md) has landed.
Check before Task 1:

```bash
test -f src/main/scala/distantlands/model/ChronicleOperation.scala &&
test -f src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala &&
test -f src/main/scala/distantlands/gameplay/operations/LeafApplication.scala &&
test -f src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala &&
echo ready
```

Expected: `ready`. If any file is missing, stop: Task 5 cannot be built.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a catalog
  `.text` or `rulesText` (`scripts/check-architecture.py`).
- A new test names cards and sites by their card objects (`SeerCard`,
  `AncientCitySite`), never by a printed name looked up at run time
  (ROADMAP standing rules, `BackendArchitectureSuite`). The legacy objects
  are in `distantlands.catalog.holding`.
- Before the first public release, event formats may change in place
  (ROADMAP standing rules): no migration for the new `Location` kinds.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. A test that names a symbol the task has not
  built yet fails by not compiling; that counts as the failing run.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- The verification gate, run at the end of the plan: the complete JVM and
  Scala.js suites, the optimized Scala.js linker, the card catalog suite,
  `python3 scripts/check-architecture.py` and
  `python3 scripts/check-markdown-links.py`.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: Legacy goal, suit and foundation

**Files:**
- Create: `src/main/scala/distantlands/catalog/LegacyGoal.scala`
- Modify: `src/main/scala/distantlands/catalog/Cards.scala:82`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (every `object ... extends Legacy(...)` line and the imports)
- Modify: `reference/catalog-ingestion/legacy-transcriptions.json`
- Create: `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala`

**Interfaces:**
- Produces: `enum LegacyGoal(val key: String)` with cases `MostSites`,
  `GrandScepter`, `PeoplesFavor`, `DarkestSecret`, `HighestRelic`,
  `MostOfSuit`, and `LegacyGoal.all: Vector[LegacyGoal]`.
- Produces: `abstract class Legacy(val id: LegacyId, val name: String,
  val goal: LegacyGoal, val suit: Suit,
  val alters: Option[FoundationNumber] = None)`.

- [ ] **Step 1: Write the failing test**

Create `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala`:

```scala
package distantlands.catalog

import distantlands.catalog.holding.{BelovedCard, GatekeeperCard,
  IronHandCard, PathfinderCard, ReformerCard, SeerCard, TheMouthCard,
  WealthyCard}
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.model.{FoundationNumber, Suit}

/** The goal and suit each legacy prints (CR section 12; Legacies design,
  * "Card data"). The goal is the ribbon's icon and the suit its seal. */
class LegacyCardDataSuite extends munit.FunSuite:
  private val legacies = FirstGameSetupFixture.catalog.legacies

  test("each goal is printed on six legacies, one of each suit"):
    val byGoal = legacies.groupBy(_.goal)
    assertEquals(byGoal.keySet, LegacyGoal.all.toSet)
    byGoal.foreach((goal, cards) =>
      assertEquals(cards.map(_.suit).sortBy(_.ordinal), Suit.all.sortBy(_.ordinal),
        s"$goal"))

  test("each goal covers a run of six ids"):
    val ids = LegacyGoal.all.map(goal =>
      legacies.filter(_.goal == goal).map(_.id.value).sorted)
    assertEquals(ids, (1 to 36).map(n => f"L$n%02d").grouped(6)
      .map(_.toVector).toVector)

  test("cards read from their seals"):
    assertEquals((IronHandCard.goal, IronHandCard.suit),
      (LegacyGoal.MostSites, Suit.Order))
    assertEquals((SeerCard.goal, SeerCard.suit),
      (LegacyGoal.DarkestSecret, Suit.Arcane))

  test("only the six suit-goal legacies alter a foundation, each a " +
      "different one"):
    val altering: Map[String, FoundationNumber] = legacies.flatMap(card =>
      card.alters.map(card.id.value -> _)).toMap
    assertEquals(altering, Map(
      WealthyCard.id.value -> FoundationNumber.II,
      GatekeeperCard.id.value -> FoundationNumber.III,
      BelovedCard.id.value -> FoundationNumber.IV,
      TheMouthCard.id.value -> FoundationNumber.VI,
      ReformerCard.id.value -> FoundationNumber.V,
      PathfinderCard.id.value -> FoundationNumber.I))
    assert(legacies.filter(_.alters.nonEmpty)
      .forall(_.goal == LegacyGoal.MostOfSuit))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./sbtw "testOnly distantlands.catalog.LegacyCardDataSuite"`
Expected: FAIL to compile: `Not found: LegacyGoal` and `value goal is not
a member of distantlands.catalog.Legacy`.

- [ ] **Step 3: Add `LegacyGoal`**

Create `src/main/scala/distantlands/catalog/LegacyGoal.scala`:

```scala
package distantlands.catalog

/** A legacy's goal: the white icon at the top of its ribbon (CR section
  * 12, and the Sun task card's legend). Resolve Legacies checks it. A
  * `MostOfSuit` goal reads the legacy's own suit. */
enum LegacyGoal(val key: String):
  /** "Rule the most sites." */
  case MostSites extends LegacyGoal("most-sites")
  /** "Hold Grand Scepter. If none, win." */
  case GrandScepter extends LegacyGoal("grand-scepter")
  /** "Hold People's Favor." */
  case PeoplesFavor extends LegacyGoal("peoples-favor")
  /** "Hold Darkest Secret." */
  case DarkestSecret extends LegacyGoal("darkest-secret")
  /** "Hold relic of highest value (under name)." */
  case HighestRelic extends LegacyGoal("highest-relic")
  /** "Rule most cards of suit (even advisers)." */
  case MostOfSuit extends LegacyGoal("most-of-suit")

object LegacyGoal:
  /** In printed id order: L01-L06 first, L31-L36 last. */
  val all: Vector[LegacyGoal] = Vector(MostSites, GrandScepter,
    PeoplesFavor, DarkestSecret, HighestRelic, MostOfSuit)
```

- [ ] **Step 4: Widen `Legacy`**

In `src/main/scala/distantlands/catalog/Cards.scala`, add `FoundationNumber`
to the `distantlands.model` import, keeping it alphabetical, and replace
line 82,

```scala
abstract class Legacy(val id: LegacyId, val name: String) extends PrintsPowers
```

with

```scala
/** A legacy: a goal, a suit and a power (CR section 12). `alters` is the
  * foundation it prints "Set Foundation X to ..." for; that face is always
  * the altered one. */
abstract class Legacy(val id: LegacyId, val name: String,
    val goal: LegacyGoal, val suit: Suit,
    val alters: Option[FoundationNumber] = None) extends PrintsPowers
```

- [ ] **Step 5: Give all 36 legacies their data**

Save this script as
`/private/tmp/claude-501/legacy_data.py` (any scratch path outside the
repository) and run it with `python3 -I <path>` from the repository root.
It rewrites each `extends Legacy(...)` line of `HoldingLegacies.scala` and
the transcriptions JSON from one table, then checks that it touched 36 of
each.

```python
import json, re, pathlib

GOALS = ["MostSites", "GrandScepter", "PeoplesFavor", "DarkestSecret",
         "HighestRelic", "MostOfSuit"]
GOAL_KEYS = {"MostSites": "most-sites", "GrandScepter": "grand-scepter",
             "PeoplesFavor": "peoples-favor", "DarkestSecret": "darkest-secret",
             "HighestRelic": "highest-relic", "MostOfSuit": "most-of-suit"}
SUITS = ["Order", "Arcane", "Discord", "Hearth", "Beast", "Nomad",
         "Hearth", "Nomad", "Discord", "Beast", "Order", "Arcane",
         "Nomad", "Hearth", "Discord", "Beast", "Order", "Arcane",
         "Hearth", "Beast", "Order", "Arcane", "Discord", "Nomad",
         "Hearth", "Discord", "Beast", "Arcane", "Order", "Nomad",
         "Hearth", "Discord", "Beast", "Arcane", "Order", "Nomad"]
ALTERS = {31: "II", 32: "III", 33: "IV", 34: "VI", 35: "V", 36: "I"}

def data(n):
    return GOALS[(n - 1) // 6], SUITS[n - 1], ALTERS.get(n)

scala = pathlib.Path(
    "src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala")
text = scala.read_text()
pattern = re.compile(
    r'extends Legacy\(LegacyId\("L(\d\d)"\), ("[^"]+")\):')
def replace(match):
    n = int(match.group(1))
    goal, suit, alters = data(n)
    extra = f", Some(FoundationNumber.{alters})" if alters else ""
    return (f'extends Legacy(LegacyId("L{n:02d}"), {match.group(2)},\n'
            f'    LegacyGoal.{goal}, Suit.{suit}{extra}):')
text, count = pattern.subn(replace, text)
assert count == 36, count
text = text.replace(
    "import distantlands.catalog.{Legacy, PrintedPower}",
    "import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}")
text = text.replace(
    "import distantlands.model.{Cost, LegacyId, PowerId}",
    "import distantlands.model.{Cost, FoundationNumber, LegacyId, PowerId,\n"
    "  Suit}")
scala.write_text(text)

path = pathlib.Path("reference/catalog-ingestion/legacy-transcriptions.json")
cards = json.loads(path.read_text())
assert len(cards) == 36
for card in cards:
    n = int(card["id"][1:])
    goal, suit, alters = data(n)
    card["goal"] = GOAL_KEYS[goal]
    card["suit"] = suit.lower()
    if alters:
        card["alters"] = alters
path.write_text(json.dumps(cards, indent=1, ensure_ascii=False) + "\n")
print("rewrote 36 legacies")
```

Expected output: `rewrote 36 legacies`. Then check the two imports changed:

Run: `head -6 src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
Expected: the first import names `LegacyGoal`, the second `FoundationNumber`
and `Suit`. If either import did not change, the file's import differs from
the strings above: edit it by hand to add those names.

Add one sentence to `reference/catalog-ingestion/README.md`, after the
bullet on `legacy-transcriptions.json`:

```markdown
  Each legacy's `goal` and `suit` were read from the ribbon icon and wax
  seal of the card images in the `bgs-cards` repository
  (`content/card-images/oath/en-US/<Name> (NF).png`), with the goal legend
  from the Sun task card; `alters` is the foundation it sets.
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./sbtw "testOnly distantlands.catalog.LegacyCardDataSuite distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.ExecutableCatalogSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/catalog/LegacyGoal.scala \
  src/main/scala/distantlands/catalog/Cards.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  reference/catalog-ingestion/legacy-transcriptions.json \
  reference/catalog-ingestion/README.md \
  src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala
git commit -m "feat(catalog): give each legacy its printed goal and suit

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Derived legacy deck and the public row

**Files:**
- Modify: `src/main/scala/distantlands/model/Cards.scala:53-59` (`CardZones`)
- Modify: `src/main/scala/distantlands/model/CardIndex.scala:11-24,136`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala:200-201,237,438-439`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala:36-39`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala`
  (doc comment of `storable` only)
- Modify: `src/test/scala/distantlands/model/TestGameFixtures.scala:127-133`
- Modify: `src/test/scala/distantlands/testkit/CardRefs.scala`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Test: `src/test/scala/distantlands/model/CardIndexSuite.scala`

**Interfaces:**
- Consumes: Task 1's `Legacy`.
- Produces: `CardZones.publicLegacies: Vector[LegacyId]` (no
  `legacyDeck`); `CardContainer.PublicLegacies`; test-kit
  `CardRefs.legacyId(card: LegacyRef): LegacyId`,
  `Table.legacy(player: PlayerId, card: LegacyRef, active: Boolean = true)`
  and `Table.publicLegacies(cards: LegacyRef*)`.

- [ ] **Step 1: Write the failing test**

Append to `src/test/scala/distantlands/model/CardIndexSuite.scala` (add the
imports it needs: `distantlands.catalog.holding.{ArbiterCard, RivalCard,
SeerCard}`, `distantlands.testkit.Table` and `distantlands.testkit.Table.p1`
if absent):

```scala
  test("a lineage's legacies and the public row are indexed, and no " +
      "legacy lies in the legacy deck"):
    val ready = Table.start.legacy(p1, SeerCard)
      .legacy(p1, ArbiterCard, active = false)
      .publicLegacies(RivalCard).ready
    val index = ready.game.cardIndex.toOption.get
    val lineage = ready.game.current.player(p1).get.lineage
    assertEquals(index.get(SeerCard.id).map(_.location.container),
      Some(CardContainer.Lineage(lineage, LineageCardArea.Legacies)))
    assertEquals(index.get(ArbiterCard.id).flatMap(_.state),
      Some(LegacyState(ArbiterCard.id, active = false)))
    assertEquals(index.get(RivalCard.id).map(_.location.container),
      Some(CardContainer.PublicLegacies))
    assert(index.ids.flatMap(index.get).forall(
      _.location.container != CardContainer.Deck(CardDeck.Legacy)))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./sbtw "testOnly distantlands.model.CardIndexSuite"`
Expected: FAIL to compile: `value legacy is not a member of
distantlands.testkit.Table`.

- [ ] **Step 3: Change `CardZones`**

In `src/main/scala/distantlands/model/Cards.scala`, replace the field
`legacyDeck: Vector[LegacyId],` with nothing, and add a last field after
`regionalDiscards`, so the class reads:

```scala
/** The shared card piles of a game. The legacy deck is not stored: it is
  * every legacy no lineage holds and the public row does not show
  * (Legacies design, ruling 3), and every legacy draw happens at game
  * start. `publicLegacies` is the Public Ambitions row, faceup. */
final case class CardZones(
    worldDeck: Vector[WorldCardId],
    relicDeck: Vector[RelicId],
    edificeDeck: Vector[EdificeId],
    regionalDiscards: Map[Region, Vector[WorldCardId]],
    publicLegacies: Vector[LegacyId] = Vector.empty
):
```

Keep the existing doc comment's facts if `CardZones` has one; merge rather
than duplicate.

- [ ] **Step 4: Index the public row, not the deck**

In `src/main/scala/distantlands/model/CardIndex.scala`:

1. In `enum`/`sealed trait CardContainer` (lines 11-24), after
   `case object NewlyDispossessed extends CardContainer`, add:

```scala
  /** The Public Ambitions row of faceup legacies (Foundation III). */
  case object PublicLegacies extends CardContainer
```

2. Replace line 136,

```scala
    addIds(common.legacyDeck, CardContainer.Deck(CardDeck.Legacy))
```

with

```scala
    // The legacy deck is derived (Legacies design, ruling 3): no card is
    // indexed there.
    addIds(common.publicLegacies, CardContainer.PublicLegacies)
```

- [ ] **Step 5: Make the legacy deck a destination only**

In `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`:

1. In `stackCards`, delete the two lines

```scala
    case CardContainer.Deck(CardDeck.Legacy) =>
      Some(ready.game.current.commonCards.legacyDeck)
```

2. In `removeCard`, replace

```scala
      case CardDeck.Legacy => cards.copy(legacyDeck = cards.legacyDeck.filterNot(_ == located.id))
```

with

```scala
      // Nothing is indexed in the derived legacy deck, so nothing leaves it.
      case CardDeck.Legacy => cards
```

3. In `insertDeck`, replace

```scala
        case CardDeck.Legacy => cards.copy(legacyDeck = insertStack(
          cards.legacyDeck, id.asInstanceOf[LegacyId], position, topAtHead = true))
```

with

```scala
        // A legacy returned to the deck leaves play: the deck is derived.
        case CardDeck.Legacy => cards
```

- [ ] **Step 6: Fix the constructors**

In `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`,
the `CardZones(...)` call passes `Vector.empty` for the legacy deck after
the edifice deck. Delete that argument, so the call reads:

```scala
      CardZones(plan.worldDeckOrder, plan.relicOrder.filterNot(usedRelics),
        catalog.edifices.map(e => EdificeId(e.id.value))
          .filterNot(edificesOn(map)),
        deal.discards(plan.denizenOrder)),
```

In `src/test/scala/distantlands/model/TestGameFixtures.scala`, delete the
line `legacyDeck = Vector.empty,`.

In `src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala`,
`storable` already refuses `CardContainer.PublicLegacies` through its
`case _ => false`. Add one line to its doc comment, or above the match if it
has none:

```scala
  // The public row is empty once the Sun has resolved legacies (slice 3);
  // a legacy still there cannot be stored.
```

- [ ] **Step 7: Test-kit steps**

In `src/test/scala/distantlands/testkit/CardRefs.scala`, after `edificeId`,
add:

```scala
  def legacyId(card: LegacyRef): LegacyId = card match
    case legacy: Legacy => legacy.id
    case id: LegacyId => id
```

and in `nameOf`, before `case other => other.toString`, add:

```scala
      case LegacyId(id) => catalog.legacies.find(_.id.value == id)
        .fold(id)(_.name)
```

In `src/test/scala/distantlands/testkit/Table.scala`:

1. After the `relic` step, add:

```scala
  /** `card` held by `player`'s lineage, active (faceup) unless `active` is
    * false (dormant, facedown). */
  def legacy(player: PlayerId, card: LegacyRef, active: Boolean = true)(
      using munit.Location): Table =
    val id = CardRefs.legacyId(card)
    val lineage = playerOf(player).lineage
    moving(id).update(_.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.updatedWith(lineage)(_.map(state => state.copy(
        legacies = state.legacies :+ LegacyState(id, active)))))))

  /** `cards` dealt faceup to the Public Ambitions row, in order. */
  def publicLegacies(cards: LegacyRef*): Table =
    val ids = cards.map(CardRefs.legacyId).toVector
    ids.foldLeft(this)(_.moving(_)).update(_.updateCurrent(c => c.copy(
      commonCards = c.commonCards.copy(publicLegacies =
        c.commonCards.publicLegacies ++ ids))))
```

2. In `without`, replace `legacyDeck = zones.legacyDeck.filter(keep),` with
   `publicLegacies = zones.publicLegacies.filter(keep),`.

- [ ] **Step 8: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.model.CardIndexSuite distantlands.gameplay.operations.* distantlands.gameplay.setup.*"`
Expected: PASS. If the compiler reports another use of `legacyDeck`, it is
a reader of the deleted field: a legacy deck reader becomes "every catalog
legacy no lineage holds and the public row does not show", and a writer is
deleted. `git grep -n legacyDeck -- src shared frontend` must print nothing.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/scala/distantlands/model src/main/scala/distantlands/gameplay \
  src/test/scala/distantlands/model src/test/scala/distantlands/testkit
git commit -m "feat(model): derive the legacy deck and add the public legacy row

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Moving legacies

**Files:**
- Modify: `src/main/scala/distantlands/model/Location.scala`
- Modify: `src/main/scala/distantlands/model/OperationError.scala:~205`
  (`locationSummary`)
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala:~154`
  (`semanticLocation`)
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateAdapter.scala:~189`
- Modify: `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`
  (`cardDestinationViolation`, `removeCard`, `insertCard`)
- Modify: `src/main/scala/distantlands/serialization/WalkerValueCodec.scala:~180,~214`
- Create: `src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala`
- Modify: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala:~255`
  (test "every Location variant round-trips through the walker codec")

**Interfaces:**
- Consumes: Task 2's `CardContainer.PublicLegacies`, `Table.legacy`,
  `Table.publicLegacies`.
- Produces: `Location.Legacies(lineage: LineageId)` and
  `Location.PublicLegacies`; wire kinds `"legacies"` (with `"lineage"`) and
  `"public-legacies"`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala`:

```scala
package distantlands.gameplay.operations

import distantlands.catalog.holding.{RivalCard, SeerCard}
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted
import distantlands.testkit.RawOperationBatch

/** `Move` between a lineage's legacies, the public row and the derived
  * legacy deck (Legacies design, slice 1). */
class LegacyMovesSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): ReadyGame =
    accepted(RawOperationBatch.executeAll(ready, ops.toVector))

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def moved(card: CardId, from: Location, to: Location,
      facing: Option[Orientation] = None): Move =
    Move(Piece.Card(card), PositionedLocation(from), PositionedLocation(to),
      resultingOrientation = facing)

  test("a public legacy taken faceup joins the lineage active"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val lineage = lineageOf(ready, p2)
    val after = applied(ready, moved(RivalCard.id, Location.PublicLegacies,
      Location.Legacies(lineage), Some(Orientation.FaceUp)))
    assertEquals(after.game.current.commonCards.publicLegacies, Vector.empty)
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = true)))

  test("a legacy entering a lineage with no orientation is dormant"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val lineage = lineageOf(ready, p2)
    val after = applied(ready, moved(RivalCard.id, Location.PublicLegacies,
      Location.Legacies(lineage)))
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = false)))

  test("a legacy returned to the deck leaves play"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val lineage = lineageOf(ready, p1)
    val after = applied(ready, moved(SeerCard.id, Location.Legacies(lineage),
      Location.Deck(CardDeck.Legacy)))
    assertEquals(after.game.campaign.lineages(lineage).legacies, Vector.empty)
    assertEquals(after.game.cardIndex.toOption.get.get(SeerCard.id), None)

  test("only a legacy enters a lineage's legacies or the public row"):
    val ready = Table.start.hand(p1, AlchemistCard).ready
    val lineage = lineageOf(ready, p1)
    Vector(Location.Legacies(lineage), Location.PublicLegacies).foreach(to =>
      assert(RawOperationBatch.executeAll(ready, Vector(moved(
        AlchemistCard.id, Location.Hand(p1), to))).left.toOption
        .exists(_.isInstanceOf[OperationError.InvalidDestination]), s"$to"))
```

In `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
in the test "every Location variant round-trips through the walker codec",
add after `Location.StartingAdvisers(LineageId("lineage")))` (moving the
closing parenthesis):

```scala
      Location.StartingAdvisers(LineageId("lineage")),
      Location.Legacies(LineageId("lineage")),
      Location.PublicLegacies)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyMovesSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: FAIL to compile: `value Legacies is not a member of object
distantlands.model.Location`.

- [ ] **Step 3: Add the locations**

In `src/main/scala/distantlands/model/Location.scala`, after
`StartingAdvisers`, add:

```scala
  /** A lineage's legacies, active (faceup) or dormant (facedown). */
  final case class Legacies(lineage: LineageId) extends Location
  /** The Public Ambitions row of faceup legacies. */
  case object PublicLegacies extends Location
```

In `src/main/scala/distantlands/model/OperationError.scala`,
`locationSummary`, after the `StartingAdvisers` arm, add:

```scala
    case Location.Legacies(lineage) => s"${lineage.value} legacies"
    case Location.PublicLegacies => "public legacies"
```

In `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala`,
`semanticLocation`, after the `StartingAdvisers` arm, add:

```scala
    case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
      Location.Legacies(lineage)
    case CardContainer.PublicLegacies => Location.PublicLegacies
```

In `src/main/scala/distantlands/gameplay/operations/OperationStateAdapter.scala`,
in the container-to-location match, after the `StartingAdvisers` arm, add:

```scala
      case (CardContainer.Lineage(left, LineageCardArea.Legacies),
          Location.Legacies(right)) => left == right
      case (CardContainer.PublicLegacies, Location.PublicLegacies) => true
```

- [ ] **Step 4: Encode them on the wire**

In `src/main/scala/distantlands/serialization/WalkerValueCodec.scala`,
in the location encoder after the `StartingAdvisers` arm, add:

```scala
    case Location.Legacies(lineage) => ujson.Obj(
      "kind" -> "legacies", "lineage" -> lineage.value)
    case Location.PublicLegacies => ujson.Obj("kind" -> "public-legacies")
```

and in `decodeLocation`, after `case "starting-advisers" => ...`, add:

```scala
    case "legacies" =>
      Right(Location.Legacies(LineageId(value("lineage").str)))
    case "public-legacies" => Right(Location.PublicLegacies)
```

- [ ] **Step 5: Write paths in card movement**

In `src/main/scala/distantlands/gameplay/operations/CardMovementOperations.scala`:

1. `cardDestinationViolation`, before `case _: Location.Site => id match`,
   add:

```scala
      case _: Location.Legacies | Location.PublicLegacies =>
        if id.isInstanceOf[LegacyId] then None
        else Some(InvalidDestination(transfer.piece, destination))
```

2. `removeCard`, before `case CardContainer.SuitedReserve(suit) =>`, add:

```scala
    case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
      Right(ready.updateCampaign(campaign => campaign.copy(lineages =
        campaign.lineages.updatedWith(lineage)(_.map(state => state.copy(
          legacies = state.legacies.filterNot(_.id == located.id)))))))
    case CardContainer.PublicLegacies => updateCommonCards(ready)(cards =>
      cards.copy(publicLegacies =
        cards.publicLegacies.filterNot(_ == located.id)))
```

3. `insertCard`, before `case Location.SharedBank =>`, add:

```scala
      // A legacy keeps its face unless the move turns it; one with no face
      // yet, from the public row, enters dormant unless turned faceup.
      case Location.Legacies(lineage) =>
        val legacy = original.state match
          case Some(state: LegacyState) => transfer.resultingOrientation
            .fold(state)(face => state.copy(active = face == Orientation.FaceUp))
          case _ => LegacyState(id.asInstanceOf[LegacyId],
            active = transfer.resultingOrientation.contains(Orientation.FaceUp))
        ready.game.campaign.lineages.get(lineage).toRight(
          InvalidDestination(transfer.piece, transfer.to.location)).map(state =>
          ready.updateCampaign(campaign => campaign.copy(lineages =
            campaign.lineages.updated(lineage,
              state.copy(legacies = state.legacies :+ legacy)))))
      case Location.PublicLegacies => Right(ready.updateCurrent(current =>
        current.copy(commonCards = current.commonCards.copy(publicLegacies =
          current.commonCards.publicLegacies :+ id.asInstanceOf[LegacyId]))))
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyMovesSuite distantlands.serialization.WalkerOperationWireSuite distantlands.gameplay.operations.ChroniclePilesSuite"`
Expected: PASS. If the compiler reports a non-exhaustive match over
`Location` elsewhere, add the two cases there, grouped with
`StartingAdvisers` and with the same result `StartingAdvisers` has.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model src/main/scala/distantlands/gameplay/operations \
  src/main/scala/distantlands/serialization/WalkerValueCodec.scala \
  src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(operations): move legacies between lineages, the public row and the deck

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Flipping a legacy

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/operations/CardFaceOperations.scala:11-56`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala:70-102`
  (`updateCardState`)
- Test: `src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala`

**Interfaces:**
- Consumes: Task 3's `Location.Legacies`.
- Produces: `Flip(legacy, Location.Legacies(lineage), FaceUp)` makes it
  active; `FaceDown` makes it dormant.

- [ ] **Step 1: Write the failing tests**

Append to `LegacyMovesSuite`:

```scala
  test("a dormant legacy flipped faceup becomes active, and back"):
    val ready = Table.start.legacy(p1, SeerCard, active = false).ready
    val lineage = lineageOf(ready, p1)
    val active = applied(ready, Flip(SeerCard.id, Location.Legacies(lineage),
      Orientation.FaceUp))
    assertEquals(active.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = true)))
    val dormant = applied(active, Flip(SeerCard.id,
      Location.Legacies(lineage), Orientation.FaceDown))
    assertEquals(dormant.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))

  test("a public legacy has no face to flip"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    assert(RawOperationBatch.executeAll(ready, Vector(Flip(RivalCard.id,
      Location.PublicLegacies, Orientation.FaceDown))).isLeft)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.LegacyMovesSuite"`
Expected: FAIL: the first test is refused with `UnsupportedOrientation`.

- [ ] **Step 3: Accept and apply a legacy flip**

In `src/main/scala/distantlands/gameplay/operations/CardFaceOperations.scala`:

1. In `flipViolation`, change

```scala
      case Some(_: DenizenState) | Some(_: VisionState) |
          Some(_: RelicState) => Vector.empty
```

to

```scala
      case Some(_: DenizenState) | Some(_: VisionState) |
          Some(_: RelicState) | Some(_: LegacyState) => Vector.empty
```

2. In `flipCard`, change the first arm to include legacies:

```scala
      case Some(_: DenizenState) | Some(_: VisionState) | Some(_: RelicState) |
          Some(_: LegacyState) =>
        updateCardState(ready, id):
          case value: DenizenState => value.copy(orientation = orientation)
          case value: VisionState => value.copy(orientation = orientation)
          case value: RelicState => value.copy(orientation = orientation)
          // Faceup is active, facedown dormant (CR section 12).
          case value: LegacyState =>
            value.copy(active = orientation == Orientation.FaceUp)
          case value => value
```

In `src/main/scala/distantlands/gameplay/operations/OperationStateWrites.scala`,
`updateCardState`, before `case atlas: CardContainer.AtlasSite =>`, add:

```scala
            case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
              Right(ready.updateCampaign(campaign => campaign.copy(lineages =
                campaign.lineages.updatedWith(lineage)(_.map(state =>
                  state.copy(legacies = state.legacies.map(value =>
                    if value.id == id then f(value).asInstanceOf[LegacyState]
                    else value)))))))
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations \
  src/test/scala/distantlands/gameplay/operations/LegacyMovesSuite.scala
git commit -m "feat(operations): flip a legacy between active and dormant

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: `FoundationAltered`

**Files:**
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala:~115`
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
  (the `chronicle` match)
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`
- Test: `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
  (test "every CoreOperation variant round-trips through the walker codec")

**Interfaces:**
- Produces: `final case class FoundationAltered(number: FoundationNumber,
  source: LegacyId) extends ChronicleOperation with LeafOperation`, wire
  kind `"foundation-altered"` with fields `"number"` (Int) and `"source"`
  (the legacy id string). Slice 3 emits it in Sun step 2.

- [ ] **Step 1: Write the failing tests**

Append to `ChronicleStateOperationsSuite` (add
`import distantlands.catalog.holding.{PathfinderCard, WealthyCard}`):

```scala
  test("an altering legacy turns its foundation altered and joins its " +
      "sources"):
    val after = accepted(run(Table.start.ready,
      FoundationAltered(FoundationNumber.II, WealthyCard.id)))
    assertEquals(after.game.campaign.foundations(FoundationNumber.II),
      FoundationState(FoundationFace.Altered, Set(WealthyCard.id)))

  test("a foundation already altered keeps its sources and adds the new " +
      "one"):
    val eroded = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered).ready
    val after = accepted(run(eroded,
      FoundationAltered(FoundationNumber.I, PathfinderCard.id)))
    assertEquals(after.game.campaign.foundations(FoundationNumber.I),
      FoundationState(FoundationFace.Altered, Set(PathfinderCard.id)))

  test("a legacy that already alters a foundation cannot alter it again"):
    val once = accepted(run(Table.start.ready,
      FoundationAltered(FoundationNumber.II, WealthyCard.id)))
    rejected(run(once, FoundationAltered(FoundationNumber.II,
      WealthyCard.id)))
```

In `ChronicleOperationWireSuite`, add (copy the helpers the
`foundation-restored` test there uses, `encodedStreamThatRoundTrips` and
`recordedOperation`):

```scala
  test("a foundation alteration names its number and source legacy"):
    val encoded = encodedStreamThatRoundTrips("foundation-altered",
      Vector(recordedOperation(FoundationAltered(FoundationNumber.II,
        LegacyId("L31")))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "foundation-altered", "number" -> 2,
        "source" -> "L31"))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("number") = 7
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].number",
        "unknown Foundation number 7")))
```

In `WalkerOperationWireSuite`, test "every CoreOperation variant
round-trips through the walker codec", add after
`FoundationRestored(FoundationNumber.III),`:

```scala
      FoundationAltered(FoundationNumber.III, LegacyId("L32")),
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.*"`
Expected: FAIL to compile: `Not found: FoundationAltered`.

- [ ] **Step 3: Add the operation**

In `src/main/scala/distantlands/model/ChronicleOperation.scala`, after
`FoundationRestored`, add (match the `extends` clause `FoundationRestored`
uses in that file; after the families split it is
`ChronicleOperation with LeafOperation`):

```scala
/** Turns Foundation `number` to its altered face, if it is not already, and
  * adds `source`, the active legacy that sets it, to its alteration
  * sources (Sun step 2, CR p. 36). The inverse of `FoundationRestored`. */
final case class FoundationAltered(number: FoundationNumber,
    source: LegacyId) extends ChronicleOperation with LeafOperation
```

- [ ] **Step 4: Apply it**

In `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`,
after `foundationRestored`, add:

```scala
  /** Foundation `number` altered, with `source` among its alteration
    * sources. A source the face already lists is a doubled operation. */
  def foundationAltered(ready: ReadyGame, number: FoundationNumber,
      source: LegacyId): Either[OperationError, ReadyGame] =
    val foundations = ready.game.campaign.foundations
    val sources = foundations.get(number).fold(Set.empty[LegacyId])(
      _.alterationSources)
    Either.cond(!sources(source),
      ready.updateCampaign(_.copy(foundations = foundations.updated(number,
        FoundationState(FoundationFace.Altered, sources + source)))),
      InvalidDescription(
        s"Foundation $number is already altered by ${source.value}"))
```

In `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
in the `chronicle` match, after the `FoundationRestored` arm, add:

```scala
    case FoundationAltered(number, source) =>
      ChronicleStateOperations.foundationAltered(ready, number, source)
```

- [ ] **Step 5: Encode it**

In `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`,
in the encode match after the `FoundationRestored` arm, add:

```scala
    case FoundationAltered(number, source) => ujson.Obj(
      "kind" -> "foundation-altered", "number" -> number.value,
      "source" -> source.value)
```

and in the decode partial function after the `"foundation-restored"` arm,
add:

```scala
    case "foundation-altered" => for
      number <- safeIntField(value.obj, "number", path)
      foundation <- FoundationNumber.all.find(_.value == number).toRight(
        InvalidValue(s"$path.number", s"unknown Foundation number $number"))
    yield FoundationAltered(foundation, LegacyId(value("source").str))
```

If the `"foundation-restored"` arm reads the number through a shared helper
after the split, use that helper for both arms instead of repeating it.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.*"`
Expected: PASS, including the kind-disjointness check in
`WalkerOperationWireSuite`.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/ChronicleOperation.scala \
  src/main/scala/distantlands/gameplay/operations \
  src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala \
  src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala \
  src/test/scala/distantlands/serialization
git commit -m "feat(chronicle): journal a legacy altering a foundation

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Game start accepts legacies and starting relics

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala:42-48,85-91`
- Modify: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/ChronicleAccounting.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala:57-84`
- Test: `src/test/scala/distantlands/gameplay/setup/ChronicleAccountingSuite.scala`

**Interfaces:**
- Consumes: Tasks 1-4 (a lineage's `LegacyState`s are indexed and movable).
- Produces: a game started from a Chronicle whose seated lineage holds
  legacies has them, with their stored faces, in
  `campaign.lineages(lineage).legacies`; its starting relics are the
  player's facedown relics and the lineage keeps none.

- [ ] **Step 1: Write the failing tests**

In `GameStartRulesSuite`, add `import distantlands.catalog.holding.{ArbiterCard,
SeerCard}`, then replace the two refusal tests, "game start refuses a
lineage's starting relics until Legacies" and "game start refuses a seated
lineage's legacies until Legacies", with:

```scala
  test("a seated lineage's legacies start with their stored faces"):
    val seated = orders.participants.head.lineageId
    val legacies = Vector(LegacyState(SeerCard.id, active = true),
      LegacyState(ArbiterCard.id, active = false))
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated, None,
      Role.Exile, legacies, Vector.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, keeping, orders))
    assertEquals(ready.game.campaign.lineages(seated).legacies, legacies)

  test("a seated lineage's starting relics go facedown to its player"):
    val participant = orders.participants.head
    val relic = chronicle.relicDeck.head
    val keeping = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(LineageState(participant.lineageId, None, Role.Exile,
        Vector.empty, Vector.empty, startingRelics = Vector(relic))))
    val ready = accepted(GameStartRules.evolve(catalog, keeping, orders))
    assertEquals(ready.game.current.player(participant.playerId).get.relics
      .filter(_.id == relic),
      Vector(RelicState(relic, Orientation.FaceDown, Tokens.empty)))
    assertEquals(
      ready.game.campaign.lineages(participant.lineageId).startingRelics,
      Vector.empty)
```

In `ChronicleAccountingSuite`, add `import distantlands.catalog.holding.SeerCard`
and append (use the suite's existing `chronicle` and `catalog` values and
its call to `ChronicleAccounting.knownAndUnique`):

```scala
  test("a legacy two lineages hold is refused, and so is an unknown one"):
    def holding(legacies: Vector[LegacyId]*): Chronicle = chronicle.copy(
      lineages = legacies.zipWithIndex.map((held, n) => LineageState(
        LineageId(s"lineage:$n"), None, Role.Exile,
        held.map(LegacyState(_, active = true)), Vector.empty)).toVector)
    assertEquals(ChronicleAccounting.knownAndUnique(catalog,
      holding(Vector(SeerCard.id), Vector(SeerCard.id))),
      Left(s"legacy ${SeerCard.id.value} is held twice"))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog,
      holding(Vector(LegacyId("no-such-legacy")))),
      Left("unknown legacy no-such-legacy"))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog,
      holding(Vector(SeerCard.id))), Right(()))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite distantlands.gameplay.setup.ChronicleAccountingSuite"`
Expected: FAIL: game start returns `UnsupportedChronicle("legacies require
Legacies")` and `UnsupportedChronicle("starting relics require Legacies")`;
accounting returns `Right(())` for the doubled and unknown legacies.

- [ ] **Step 3: Drop the refusals**

In `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`,
`buildPlan`, delete

```scala
      _ <- Either.cond(chronicle.lineages.forall(_.startingRelics.isEmpty), (),
        UnsupportedChronicle("starting relics require Legacies"))
```

and

```scala
      _ <- Either.cond(records.forall(_.legacies.isEmpty), (),
        UnsupportedChronicle("legacies require Legacies"))
```

then delete the `seated` and `records` bindings if nothing else reads them
(`-Werror` reports unused locals). In `evolve`, replace the comment

```scala
      // A seated lineage the Chronicle lists starts from its record;
      // `buildPlan` refuses what that record needs Legacies for.
```

with

```scala
      // A seated lineage the Chronicle lists starts from its record, its
      // legacies with their stored faces; `LaterGameSetup` hands its
      // starting relics to its player.
```

- [ ] **Step 4: Hand out starting relics**

In `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`:

1. In `apply`, change `val advised = startingAdvisers(ready, plan)` to
   `val advised = startingRelics(startingAdvisers(ready, plan), plan)`.
2. After `startingAdvisers`, add:

```scala
  /** Each seated lineage's starting relics go facedown to its player and
    * leave the lineage's record, as its starting advisers do (Legacies
    * design, slice 1). Heirloom keeps one; an import may carry them. */
  private def startingRelics(ready: ReadyGame, plan: FirstGameSetupPlan)
      : ReadyGame =
    val seats = plan.participants.map(p => p.lineageId -> p.playerId).toMap
    val kept = ready.game.campaign.lineages.collect {
      case (lineage, state) if seats.contains(lineage) &&
          state.startingRelics.nonEmpty =>
        seats(lineage) -> state.startingRelics }
    ready.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.map((id, lineage) => id -> (if seats.contains(id)
        then lineage.copy(startingRelics = Vector.empty) else lineage))))
      .updateCurrent(current => current.copy(players = current.players.map(
        state => state.copy(relics = state.relics ++ kept.getOrElse(
          state.player, Vector.empty).map(
          RelicState(_, Orientation.FaceDown, Tokens.empty))))))
```

3. Update the object's doc comment: "every seated lineage's starting
   advisers" becomes "every seated lineage's starting advisers and starting
   relics".

- [ ] **Step 5: Account for held legacies**

In `src/main/scala/distantlands/gameplay/setup/ChronicleAccounting.scala`,
`knownAndUnique`, add after `val keptRelics = ...`:

```scala
    val held = chronicle.lineages.flatMap(_.legacies).map(_.id)
```

and in the `first(Vector(...))` list, before the last entry
(`sources.find(...)`), add:

```scala
      held.find(!legacyIds(_)).map(id => s"unknown legacy ${id.value}"),
      duplicate(held).map(id => s"legacy ${id.value} is held twice"),
```

Update its doc comment's second sentence: "A foundation's alteration sources
and the legacies a lineage holds are known legacies, and no legacy is held
twice."

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup \
  src/test/scala/distantlands/gameplay/setup
git commit -m "feat(setup): start a game with a lineage's legacies and starting relics

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Who identifies a legacy

**Files:**
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala:~280-345,452-457`
- Create: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`

**Interfaces:**
- Consumes: Task 2's `Table.legacy`, `Table.publicLegacies`.
- Produces: `identifiesCard` names an active legacy and a public legacy to
  every viewer, and a dormant legacy only to the player seated with its
  lineage (Legacies design, ruling 1). The Game Log reads this now. The
  projection carries no legacy yet: slice 5 adds the player-area legacy
  strip and the public row, and they read this rule to choose a face or a
  legacy back.

- [ ] **Step 1: Write the failing test**

Create `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`:

```scala
package distantlands.application

import distantlands.catalog.holding.{ArbiterCard, RivalCard, SeerCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Who may identify a legacy (Legacies design, ruling 1): everyone an
  * active or public one, only its owner a dormant one. */
class LegacyIdentitySuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val ready = Table.start.legacy(p1, SeerCard)
    .legacy(p1, ArbiterCard, active = false)
    .publicLegacies(RivalCard).ready
  private val viewers = Vector(Some(p1), Some(p2), None)

  test("every viewer identifies an active legacy and a public one"):
    assert(viewers.forall(viewer =>
      presentation.identifiesAt(ready, viewer, SeerCard.id) &&
        presentation.identifiesAt(ready, viewer, RivalCard.id)))

  test("only its owner identifies a dormant legacy"):
    assert(presentation.identifiesAt(ready, Some(p1), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, Some(p2), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, None, ArbiterCard.id))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.application.LegacyIdentitySuite"`
Expected: FAIL: the first test, since `identifiesCard` names no legacy.

- [ ] **Step 3: Teach the projector legacies**

In `src/main/scala/distantlands/application/GamePresentationProjector.scala`:

1. `orientationOf`, before `case _ => None`, add:

```scala
      case Some(LegacyState(_, active)) =>
        Some(if active then Orientation.FaceUp else Orientation.FaceDown)
```

2. `identifiesCard`, after the `StartingAdvisers` arm, add:

```scala
      // An active legacy is faceup for all; a dormant one is facedown in its
      // owner's play area, and only they know it (Legacies design, ruling 1).
      case CardContainer.Lineage(lineage, LineageCardArea.Legacies) =>
        faceup || viewer.exists(player => ready.game.current.players
          .exists(seat => seat.player == player && seat.lineage == lineage))
      // The Public Ambitions row is faceup.
      case CardContainer.PublicLegacies => true
```

3. In the doc comment's list above `identifiesCard`, after the starting
   advisers bullet, add:

```scala
    *  - A lineage's active legacy is named to every viewer, a dormant one
    *    only to the player seated with that lineage; the public row to
    *    every viewer.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/application/LegacyIdentitySuite.scala
git commit -m "feat(presentation): show a dormant legacy only to its owner

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Docs and the gate

**Files:**
- Modify: `CONTEXT.md`
- Modify: `docs/rules/ambiguities.md` (item 1)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md`
  (status line)

- [ ] **Step 1: `CONTEXT.md`**

Add a `### Legacies` section after the last section under `## Language`,
in the file's format:

```markdown
### Legacies

**Public legacy**:
A legacy in the Public Ambitions row (Foundation III's altered face), held
by nobody. Players compete for it in the Chronicle.
_Avoid_: shared legacy, row legacy

**Derived deck**:
A pile the game works out from what nobody holds instead of storing it.
The legacy deck is one: every legacy no lineage holds and the public row
does not show. A legacy returned to it leaves play.
_Avoid_: legacy box (the box and the legacy deck are the same pile here)
```

- [ ] **Step 2: `docs/rules/ambiguities.md`**

Item 1 ("Card powers and exact costs") says the holding package keeps "all
36 legacies". Append to that item: "Each legacy has its printed goal, suit
and altered foundation (Legacies design, slice 1); their powers are still
in holding.", with "Legacies design" a link to
`../superpowers/specs/2026-11/2026-11-13-legacies-design.md` (relative to
`docs/rules/`).

- [ ] **Step 3: Spec status**

In the Legacies design, replace "Nothing here is implemented." with
"Slice 1 landed (plan: [Legacies slice 1](../../plans/2026-11/2026-11-13-legacies-slice1.md))."

- [ ] **Step 4: Run the verification gate**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statements at or above 92% and branches at or above 84.4%.
The JVM run also links the optimized Scala.js bundle (`build.sbt` puts
`frontend / fullLinkJS` on the test classpath). If
`GameLogDecisionSuite`'s slow test hits munit's 30-second timeout under
coverage, run the gate once more; a second failure, or any other failure,
stops the task.

Run: `./sbtw "frontend/test"` (the frontend suites, which the full run
skips)
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

Run: `python3 scripts/check-markdown-links.py`
Expected: `Markdown link check passed`.

Run: `git status --short src/test/resources`
Expected: clean. No fixture or recorded journal changed.

- [ ] **Step 5: Commit**

```bash
git add CONTEXT.md docs/rules/ambiguities.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md
git commit -m "docs(legacies): record slice 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
