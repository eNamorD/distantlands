# Legacies, Slice 2: Setup Deal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every game deals legacies at setup. Under Quiet Ambitions,
Foundation III's normal face and every first game's deal, each seated
player draws 2 legacies, dormant, and in Setup keeps one and returns the
other to the deck. Under Public Ambitions, the altered face, game start
deals 4 legacies faceup into the public row and nobody draws. A short deck
draws what remains. The deal order is shuffled by the plan factory and
recorded on `GameStarted`, as the denizen order is. The Game Log says who
kept a legacy without naming it, and names the public row. The saved
Chronicle keeps the legacies of a lineage seated for the first time
(implementation ruling R1).

**Architecture:** `LegacyDeal` in `gameplay/setup` is the deal arithmetic,
beside `SetupDeal`: the derived deck a Chronicle leaves, each player's
draw and the public row. `SetupOrders` gains `legacyOrder`, the shuffled
derived deck. `GameStartRules` checks it and applies the deal: drawn
legacies join their lineages dormant, and the public row is set.
`SetupProcedure` gains a per-player keep decision between pawn placement
and the adviser choice. Its options are a new decision option kind,
`DecisionOptionRef.Legacy`. The presentation layer learns a legacy's
name, suit and rules text, so options and log lines can name one.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacies design](../../specs/2026-11/2026-11-13-legacies-design.md),
rulings 1, 3, 4 and 5, and section "Slice 2: setup deal".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md),
R1. Card text: Foundation III in `catalog/FoundationCards.scala`; CR "Set
Up Players" (first game) and "Legacies in Detail" in
`~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`.

## Prerequisite

Slice 1 has landed (commits `cf8c6b9aa` to `33a6443df`). Check before
Task 1:

```bash
grep -q "publicLegacies" src/main/scala/distantlands/model/Cards.scala &&
grep -q "case object PublicLegacies" src/main/scala/distantlands/model/Location.scala &&
grep -q "FoundationAltered" src/main/scala/distantlands/model/ChronicleOperation.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R9-R17 ([rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md)):

- **R9** (Task 5): a player whose short-deck draw is one legacy keeps it
  without being asked; a player who drew none is asked nothing.
- **R10** (Task 4): a new decision option kind, `DecisionOptionRef.Legacy`,
  wire kind `"legacy"`.
- **R11** (Task 4): slice 2 adds a legacy's minimal card details (kind
  `"legacy"`, name, suit, rules text); slice 5 adds the goal and the UI.
- **R12** (Tasks 2, 3 and 5): game start refuses a legacy order that is not
  the derived deck, every fixture supplies one, and `Table.start` returns
  the dealt legacies so the quiet table holds none. Affected golden logs
  are rewritten and reviewed; every changed line must be one the deal
  explains (a new keep decision, a new log line, a shifted event number).
- **R13** (Task 5): the offered legacies are derived from state, the
  dormant legacies the lineage holds that its saved record does not;
  nothing new is stored.
- **R14** (Task 5): the Public Ambitions line posts first in the Setup walk,
  before the Reliquary pick, as a Setup note.
- **R15** (Task 1): `legacyOrder` is a required wire field.
- **R16** (Task 3): the deal is applied in `GameStartRules.evolve`; the
  plan factory and game start share the deal arithmetic. Task 7 updates
  the design's wording.
- **R17** (Task 7): the "Legacies rulings" section of
  `docs/rules/ambiguities.md`, begun in the slice 1 follow-up with design
  rulings 1 and 3 and R4, is completed with all ten design rulings.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a catalog
  `.text` or `rulesText` (`scripts/check-architecture.py`).
- A new test names cards by their card objects (`SeerCard`), never by a
  printed name looked up at run time. Legacy objects are in
  `distantlands.catalog.holding`.
- Before the first public release, event formats may change in place
  (ROADMAP standing rules): no migration for `legacyOrder`.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. A test that names a symbol the task has not
  built yet fails by not compiling; that counts as the failing run.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4% (`./sbtw clean coverage test coverageReport`).
- A rewritten golden file (`GAMELOG_GOLDEN=write`,
  `NOTE_KEYS_SNAPSHOT=write`) is reviewed line by line before it is
  committed, and only in the task that changes it.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: `LegacyDeal` and the recorded order

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala`
- Modify: `src/main/scala/distantlands/model/Setup.scala` (`SetupOrders`)
- Modify: `src/main/scala/distantlands/serialization/GameEventJsonSupport.scala`
  (`encodeSetupOrders`, `decodeSetupOrders`)
- Create: `src/test/scala/distantlands/gameplay/setup/LegacyDealSuite.scala`
- Modify: `src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala`

**Interfaces:**
- Produces: `final case class LegacyDeal(face: FoundationFace,
  seats: Vector[PlayerId])` with `drawn(order, player): Vector[LegacyId]`
  and `publicRow(order): Vector[LegacyId]`; `LegacyDeal.of(chronicle,
  participants)`, `LegacyDeal.deck(catalog, chronicle): Vector[LegacyId]`,
  `LegacyDeal.QuietDraw = 2`, `LegacyDeal.PublicRow = 4`.
- Produces: `SetupOrders.legacyOrder: Vector[LegacyId] = Vector.empty`,
  the last field; wire field `"legacyOrder"` (string array).

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/LegacyDealSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.holding.{ArbiterCard, RivalCard, SeerCard}
import distantlands.model._

/** Foundation III's deal of the derived legacy deck, on both faces
  * (Legacies design, slice 2, rulings 3-5). */
class LegacyDealSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val seats = FirstGameSetupFixture.participants
  private val Vector(p1, p2, p3) = seats.map(_.playerId)
  private val order = catalog.legacies.map(_.id)

  private def withFace(face: FoundationFace): Chronicle =
    FirstGameSetupFixture.chronicle.copy(foundations = Map(
      FoundationNumber.III -> FoundationState(face, Set.empty)))

  test("Quiet Ambitions draws 2 for each player in seat order and deals " +
      "no public row"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Normal), seats)
    assertEquals(seats.map(seat => deal.drawn(order, seat.playerId)),
      Vector(order.slice(0, 2), order.slice(2, 4), order.slice(4, 6)))
    assertEquals(deal.publicRow(order), Vector.empty)

  test("Public Ambitions deals the first 4 faceup and nobody draws"):
    val deal = LegacyDeal.of(withFace(FoundationFace.Altered), seats)
    assertEquals(deal.publicRow(order), order.take(4))
    assertEquals(seats.map(seat => deal.drawn(order, seat.playerId)),
      Vector.fill(3)(Vector.empty))

  test("a Chronicle that lists no Foundation III deals Quiet Ambitions"):
    assertEquals(LegacyDeal.of(FirstGameSetupFixture.chronicle, seats).face,
      FoundationFace.Normal)

  test("a short deck draws what remains, in seat order"):
    val deal = LegacyDeal(FoundationFace.Normal, Vector(p1, p2, p3))
    val short = order.take(3)
    assertEquals(Vector(p1, p2, p3).map(deal.drawn(short, _)),
      Vector(short.take(2), short.drop(2), Vector.empty))
    assertEquals(LegacyDeal(FoundationFace.Altered, Vector(p1))
      .publicRow(short), short)

  test("the deck is every legacy no lineage holds, seated or in the box"):
    val held = FirstGameSetupFixture.chronicle.copy(lineages = Vector(
      LineageState(LineageId("l1"), None, Role.Exile,
        Vector(LegacyState(SeerCard.id, active = true)), Vector.empty),
      LineageState(LineageId("boxed"), None, Role.Citizen,
        Vector(LegacyState(RivalCard.id, active = false),
          LegacyState(ArbiterCard.id, active = true)), Vector.empty)))
    assertEquals(LegacyDeal.deck(catalog, held),
      order.filterNot(Set(SeerCard.id, RivalCard.id, ArbiterCard.id)))
```

In `GameEventFactsWireSuite`, after the test "GameStarted round trips its
Chronicle and dealt SetupOrders", add:

```scala
  test("GameStarted round trips the legacy order it dealt"):
    val dealt = orders.copy(legacyOrder =
      Vector(LegacyId("L22"), LegacyId("L17")))
    val event = OathEvent.GameStarted(chronicle, dealt)
    val encoded = accepted(GameEventWire.encodeEvent("goal", 0L, event))
    assertEquals(encoded("payload")("orders")("legacyOrder"),
      ujson.Arr("L22", "L17"))
    assertEquals(accepted(GameEventWire.decode(encoded)).event, event)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacyDealSuite distantlands.serialization.GameEventFactsWireSuite"`
Expected: FAIL to compile: `Not found: LegacyDeal` and `value legacyOrder
is not a member of distantlands.model.SetupOrders`.

- [ ] **Step 3: Add `legacyOrder`**

In `src/main/scala/distantlands/model/Setup.scala`, `SetupOrders` gains a
last field, and its doc comment one sentence ("`legacyOrder` is the derived
legacy deck shuffled, which Foundation III deals (`LegacyDeal`)."):

```scala
    relicOrder: Vector[RelicId],
    legacyOrder: Vector[LegacyId] = Vector.empty
)
```

In `GameEventJsonSupport.encodeSetupOrders`, after `"relicOrder"`, add
`"legacyOrder" -> stringArray(orders.legacyOrder.map(_.value))`. In
`decodeSetupOrders`, pass
`obj("legacyOrder").arr.toVector.map(v => LegacyId(v.str))` as the last
argument. The field is required (R15).

- [ ] **Step 4: Add `LegacyDeal`**

Create `src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** Foundation III's deal of the legacy deck: the one arithmetic the plan
  * factory and game start share (Legacies design, slice 2). `seats` lists
  * every seated player in seat order; `order` below is always the deal's
  * `legacyOrder`, the derived deck shuffled.
  *
  * Quiet Ambitions, the normal face and every first game's deal (ruling
  * 4): each player draws 2, in seat order, and keeps one in Setup. Public
  * Ambitions, the altered face: the first 4 are dealt faceup to the public
  * row and nobody draws. A short deck draws what remains (ruling 5).
  */
final case class LegacyDeal(face: FoundationFace, seats: Vector[PlayerId]):
  private val public = face == FoundationFace.Altered

  /** The legacies `player` draws, dormant: 2 under Quiet Ambitions, fewer
    * from a short deck, none under Public Ambitions. */
  def drawn(order: Vector[LegacyId], player: PlayerId): Vector[LegacyId] =
    val seat = seats.indexOf(player)
    if public || seat < 0 then Vector.empty
    else order.slice(LegacyDeal.QuietDraw * seat,
      LegacyDeal.QuietDraw * (seat + 1))

  /** The Public Ambitions row, faceup in deal order. */
  def publicRow(order: Vector[LegacyId]): Vector[LegacyId] =
    if public then order.take(LegacyDeal.PublicRow) else Vector.empty

object LegacyDeal:
  val QuietDraw: Int = 2
  val PublicRow: Int = 4

  /** The deal of the game `chronicle` starts for `participants`, under the
    * Chronicle's Foundation III face. Setup With No Empire does not erode
    * Foundation III. */
  def of(chronicle: Chronicle, participants: Vector[FirstGameParticipant])
      : LegacyDeal =
    LegacyDeal(StartingFoundations.of(chronicle)(FoundationNumber.III).face,
      participants.map(_.playerId))

  /** The derived legacy deck (ruling 3): every catalog legacy no lineage
    * of `chronicle` holds, seated or in the box, in catalog order. A
    * Chronicle stores no public row. */
  def deck(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Vector[LegacyId] =
    val held = chronicle.lineages.flatMap(_.legacies).map(_.id).toSet
    catalog.legacies.map(_.id).filterNot(held)
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacyDealSuite distantlands.serialization.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala \
  src/main/scala/distantlands/model/Setup.scala \
  src/main/scala/distantlands/serialization/GameEventJsonSupport.scala \
  src/test/scala/distantlands/gameplay/setup/LegacyDealSuite.scala \
  src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala
git commit -m "feat(setup): deal Foundation III from the derived legacy deck

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The plan factory shuffles the legacy deck, and game start checks it

**Files:**
- Modify: `src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala`
- Modify: `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
  (`buildPlan`, `validateDeal`)
- Modify: `src/test/scala/distantlands/gameplay/setup/FirstGameSetupFixture.scala`
  (`orders`)
- Modify: `src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala`
  (`orders`)
- Modify: every other `ChronicleFirstGamePlan.dealOrder` caller in
  `src/test` (`git grep -n "dealOrder(" -- src/test`)
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/server/TrustedGameProvisioningSuite.scala`

**Interfaces:**
- Consumes: Task 1's `LegacyDeal.deck`, `SetupOrders.legacyOrder`.
- Produces: both `ChronicleFirstGamePlan.dealOrder` overloads take a
  `legacyOrder: Vector[LegacyId]` after `relicOrder`. The factory passes
  `random.shuffle(LegacyDeal.deck(catalog, chronicle))`. Game start
  refuses `UnsupportedChronicle("the deal does not hold the legacy deck")`.

- [ ] **Step 1: Write the failing tests**

In `GameStartRulesSuite`, add:

```scala
  test("game start refuses a deal that does not hold the legacy deck"):
    val short = orders.copy(legacyOrder = orders.legacyOrder.tail)
    assertEquals(GameStartRules.evolve(catalog, chronicle, short),
      Left(OathViolation.UnsupportedChronicle(
        "the deal does not hold the legacy deck")))

  test("the legacy deck a deal holds leaves out the legacies lineages hold"):
    val seated = orders.participants.head.lineageId
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated, None,
      Role.Exile, Vector(LegacyState(SeerCard.id, active = true)),
      Vector.empty)))
    assertEquals(GameStartRules.evolve(catalog, keeping, orders).left
      .toOption, Some(OathViolation.UnsupportedChronicle(
        "the deal does not hold the legacy deck")))
    assert(GameStartRules.evolve(catalog, keeping, orders.copy(legacyOrder =
      orders.legacyOrder.filterNot(_ == SeerCard.id))).isRight)
```

Update the slice 1 tests that start a game from a lineage holding Seer
and Arbiter, "a seated lineage's legacies start with their stored faces"
(`GameStartRulesSuite`) and "a game started from a lineage's legacies and
starting relic ..." (`ChronicleRecordSuite`), to pass `orders.copy(
legacyOrder = orders.legacyOrder.filterNot(Set(SeerCard.id,
ArbiterCard.id)))`.

In `TrustedGameProvisioningSuite`, find the test that reads the factory's
`FirstGamePlan` orders (near line 274) and add an assertion that
`plan.orders.legacyOrder.sortBy(_.value)` equals
`LegacyDeal.deck(catalog, plan.chronicle).sortBy(_.value)`. If that suite
has no test reading a generated plan's orders, add one there in its style.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite"`
Expected: FAIL: game start accepts the short order.

- [ ] **Step 3: Check the order at game start**

In `GameStartRules.validateDeal`, add `catalog: ExecutableCatalog` as the
first parameter (update its one caller in `buildPlan`) and, after the
relic deck check:

```scala
      _ <- Either.cond(orders.legacyOrder.sortBy(_.value) ==
          LegacyDeal.deck(catalog, chronicle).sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the legacy deck"))
```

Update its doc comment: "The deal holds the Chronicle's decks and the
derived legacy deck, each card once, in any order; ...".

- [ ] **Step 4: Shuffle it in the factory**

In `ChronicleFirstGamePlan`, both `dealOrder` overloads take
`legacyOrder: Vector[LegacyId]` after `relicOrder` and pass it to
`SetupOrders`. Add to the object's doc comment: "The caller also shuffles
the derived legacy deck (`LegacyDeal.deck`) into `legacyOrder`."

In `GeneratedFirstGamePlanFactory.build`, pass
`random.shuffle(LegacyDeal.deck(catalog, chronicle))` after the relic
order, and import `LegacyDeal` beside `SetupDeal`. Add "and the derived
legacy deck" to the doc comment's "Both decks are shuffled" sentence
("Every deck is shuffled ...").

- [ ] **Step 5: Supply it in the fixtures**

In `FirstGameSetupFixture`, `orders` passes
`legacyOrder = LegacyDeal.deck(catalog, chronicle)` (catalog order: a
fixture deal is reproducible). In `LaterGameFixture.orders`, pass
`LegacyDeal.deck(FirstGameSetupFixture.catalog, chronicle)`. Every other
`dealOrder` caller in `src/test` passes the same for its Chronicle.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw test`
Expected: PASS. Nothing deals yet, so no state changes; a failure names a
caller Step 5 missed.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/application \
  src/main/scala/distantlands/gameplay/setup/GameStartRules.scala \
  src/test/scala
git commit -m "feat(setup): shuffle the legacy deck into the deal and check it at game start

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Game start deals legacies

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
  (`evolve`)
- Modify: `src/test/scala/distantlands/testkit/Table.scala` (`quiet`)
- Create: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala`

**Interfaces:**
- Consumes: Task 1's `LegacyDeal`, Task 2's validated `legacyOrder`.
- Produces: after `GameStartRules.evolve`, each seated lineage holds its
  stored legacies followed by its drawn ones, dormant; under Public
  Ambitions `commonCards.publicLegacies` is `publicRow(legacyOrder)`.
  `Table.start` holds no legacy and no public row.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala`:

```scala
package distantlands.gameplay.setup

import distantlands.catalog.holding.SeerCard
import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** Foundation III at game start and in Setup (Legacies design, slice 2). */
class LegacySetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders
  private val seats = orders.participants

  private def publicAmbitions(base: Chronicle): Chronicle = base.copy(
    foundations = base.foundations.updated(FoundationNumber.III,
      FoundationState(FoundationFace.Altered, Set.empty)))

  test("Quiet Ambitions deals each seated lineage 2 dormant legacies, " +
      "after those it holds"):
    val seated = seats.head.lineageId
    val kept = LegacyState(SeerCard.id, active = true)
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated,
      None, Role.Exile, Vector(kept), Vector.empty)))
    val order = LegacyDeal.deck(catalog, keeping)
    val ready = accepted(GameStartRules.evolve(catalog, keeping,
      orders.copy(legacyOrder = order)))
    val deal = LegacyDeal.of(keeping, seats)
    seats.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies,
      (if seat.lineageId == seated then Vector(kept) else Vector.empty) ++
        deal.drawn(order, seat.playerId).map(LegacyState(_, active = false)),
      seat.playerId.value))
    assertEquals(ready.game.current.commonCards.publicLegacies, Vector.empty)

  test("Public Ambitions deals 4 legacies faceup to the row and nobody " +
      "draws"):
    val ready = accepted(GameStartRules.evolve(catalog,
      publicAmbitions(chronicle), orders))
    assertEquals(ready.game.current.commonCards.publicLegacies,
      orders.legacyOrder.take(4))
    assert(seats.forall(seat =>
      ready.game.campaign.lineages(seat.lineageId).legacies.isEmpty))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacySetupSuite"`
Expected: FAIL: no lineage holds a drawn legacy and the row is empty.

- [ ] **Step 3: Apply the deal**

In `GameStartRules.evolve`, after `val deal = SetupDeal.of(...)`, add
`val legacyDeal = LegacyDeal.of(chronicle, orders.participants)`. Change
`commonCards` to also set the row:

```scala
      commonCards = material.commonCards.copy(edificeDeck =
        material.commonCards.edificeDeck.filterNot(storedEdifices),
        publicLegacies = legacyDeal.publicRow(orders.legacyOrder))
```

and `lineages` to append each player's draw, dormant:

```scala
      lineages = plan.participants.map { participant =>
        val record = recorded.getOrElse(participant.lineageId,
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
        participant.lineageId -> record.copy(legacies = record.legacies ++
          legacyDeal.drawn(orders.legacyOrder, participant.playerId)
            .map(LegacyState(_, active = false)))
      }.toMap
```

Extend the comment above `recorded`: "Foundation III's draw joins it
dormant (`LegacyDeal`); the player keeps one in Setup."

- [ ] **Step 4: Keep the quiet table quiet**

In `Table.quiet`, after the `updateCurrent` block, return the dealt
legacies to the derived deck: clear `commonCards.publicLegacies` and every
lineage's `legacies`. Extend its doc comment: "and the legacies Foundation
III dealt left in the derived deck".

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw test`
Expected: `LegacySetupSuite` passes. Other suites that read lineage
records or the card index at game start may now see 2 legacies per
lineage. Fix each by reading the dealt legacies (`LegacyDeal`) or by
starting from `Table.start`; never by dropping the deal. A suite that
compares a whole `LineageState` or `CampaignState` built from
`GameStartRules.evolve` is the likely failure. List every suite changed
in the commit message body.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/GameStartRules.scala \
  src/test/scala
git commit -m "feat(setup): deal Quiet Ambitions and Public Ambitions at game start

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: A legacy as a decision option, and its card details

**Files:**
- Modify: `src/main/scala/distantlands/model/Decisions.scala`
  (`DecisionOptionRef`, `DecisionOption`, `forRef`, `fromWire`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`cardKind`, `cardDetails`)
- Modify: `src/main/scala/distantlands/application/DecisionOptionRows.scala`
  (`project`)
- Modify: `src/main/scala/distantlands/application/gamelog/ChoiceWords.scala`,
  `NoteLines.scala`, `PlanAidLines.scala` (each match over
  `DecisionOptionRef` that names `Edifice`)
- Test: `src/test/scala/distantlands/model/DecisionOptionRefSuite.scala`
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`

**Interfaces:**
- Produces: `DecisionOptionRef.Legacy(id: LegacyId)`, kind `"legacy"`,
  wire id the legacy id; `DecisionOption.Legacy(ref)`. A legacy's
  `CardDetailsProjection` has kind `"legacy"`, its name, `suit` its seal's
  key and its rules text; `cardKind` of a legacy is `"legacy"`.

- [ ] **Step 1: Write the failing tests**

In `DecisionOptionRefSuite`, add (import
`distantlands.catalog.holding.SeerCard`):

```scala
  private val seer = DecisionOptionRef.Legacy(SeerCard.id)

  test("a legacy reference spells itself as a kind and id pair, parses " +
      "back and is presentable from the reference alone"):
    assertEquals((seer.kind, seer.wireId), ("legacy", "L22"))
    assertEquals(DecisionOptionRef.fromWire(seer.kind, seer.wireId),
      Some(seer))
    assertEquals(DecisionOption.forRef(seer), Some(DecisionOption.Legacy(seer)))
```

In `LegacyIdentitySuite`, add:

```scala
  private val rows = new DecisionOptionRows(catalog, presentation)

  test("a dormant legacy option reads as its card to its owner only"):
    val index = ready.game.cardIndex.toOption
    val option = DecisionOption.Legacy(DecisionOptionRef.Legacy(ArbiterCard.id))
    val row = rows.project(ready, Some(p1), index, option)
    assertEquals(row.flatMap(_.card).map(card =>
      (card.kind, card.name, card.suit)),
      Some(("legacy", ArbiterCard.name, Some(ArbiterCard.suit.key))))
    assertEquals(rows.project(ready, Some(p2), index, option), None)
```

(`ArbiterCard.name` is the card object's own field, not a lookup.) Add the
imports `distantlands.model.{DecisionOption, DecisionOptionRef}`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.model.DecisionOptionRefSuite distantlands.application.LegacyIdentitySuite"`
Expected: FAIL to compile: `value Legacy is not a member of object
distantlands.model.DecisionOptionRef`.

- [ ] **Step 3: Add the kind**

In `Decisions.scala`, after `Edifice`, add

```scala
  final case class Legacy(id: LegacyId) extends DecisionOptionRef:
    val kind: String = "legacy"
    def wireId: String = id.value
```

and `final case class Legacy(ref: DecisionOptionRef.Legacy) extends
DecisionOption` after `DecisionOption.Edifice`; in `forRef`,
`case value: DecisionOptionRef.Legacy => Some(Legacy(value))`; in
`fromWire`, `case "legacy" => Some(Legacy(LegacyId(wireId)))`. Update the
"seventeen variants" count in the trait's doc comment.

Compile (`./sbtw Test/compile`) and add a `Legacy` arm next to each
`Edifice` arm the compiler or `git grep -n "DecisionOptionRef.Edifice" --
src/main` reports: in `ChoiceWords` and `PlanAidLines`, as a card, like
`Edifice`; in `NoteLines`, as a card. `PowerSourceRef.of` keeps its
`case _ => None`: no power is used from a legacy option.

- [ ] **Step 4: Present a legacy**

In `GamePresentationProjector`, `cardKind` gains
`case _: LegacyId => "legacy"`, and `cardDetails`, before `case other`:

```scala
    case value: LegacyId => catalog.legacy(value).fold(
      CardDetailsProjection(value.value, "legacy", safeLabel(value.value),
        orientation = orientation.map(orientationName), hidden = hidden)) { l =>
      CardDetailsProjection(value.value, "legacy", l.name, Some(l.suit.key),
        rulesText = Some(l.rulesText),
        orientation = orientation.map(orientationName), hidden = hidden,
        implemented = cardImplemented(l), powers = powerFaces(l))
    }
```

In `DecisionOptionRows.project`, after the `Vision` arm:

```scala
      case DecisionOption.Legacy(legacy) =>
        card(ready, viewer, index, legacy.id)
          .flatMap(details => row(details.name, Some(details)))
```

In `LogWords.face`, a legacy back is already `"Legacy"` (`backOf`); check
that `hiddenCard(presentation.cardKind(id))` now gives `"legacy"`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.model.* distantlands.application.*"` and
`./sbtw "frontend/test"`
Expected: PASS. The frontend draws an unknown card kind as a plain card;
if a frontend suite enumerates kinds, add `"legacy"` beside `"relic"`.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/Decisions.scala \
  src/main/scala/distantlands/application \
  src/test/scala/distantlands/model/DecisionOptionRefSuite.scala \
  src/test/scala/distantlands/application/LegacyIdentitySuite.scala
git commit -m "feat(decisions): offer a legacy as a decision option and name it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The keep decision and the Setup log

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/LegacySetupSuite.scala`
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`
  (or the suite that projects a parked Setup decision to another seat;
  `git grep -ln "adviserDecisionId" -- src/test/scala/distantlands/application`)
- Modify: `src/test/resources/notes/declared-keys.txt` (rewritten)
- Modify: `src/test/resources/gamelog/*.log` (rewritten, those that walk
  Setup)

**Interfaces:**
- Consumes: Task 3's dealt legacies, Task 4's `DecisionOptionRef.Legacy`.
- Produces: `SetupProcedure.legacyDecisionId(player)` =
  `s"setup.legacy-choice.${player.value}"`; note keys `kept-legacy`
  ("{0} kept a legacy") and `dealt-public-legacies` ("Public Ambitions
  dealt {0}").

- [ ] **Step 1: Write the failing tests**

Append to `LegacySetupSuite`, adding the imports each test needs:

```scala
  test("in a first game each player keeps one dormant legacy and returns " +
      "the other to the deck"):
    val setup = FirstGameSetupFixture.setupThroughWake()
    val ready = distantlands.testkit.Outcomes.ready(setup.state)
    val deal = LegacyDeal.of(chronicle, seats)
    seats.foreach { seat =>
      val drawn = deal.drawn(orders.legacyOrder, seat.playerId)
      assertEquals(ready.game.campaign.lineages(seat.lineageId).legacies,
        Vector(LegacyState(drawn.head, active = false)), seat.playerId.value)
      assertEquals(ready.game.cardIndex.toOption.get.get(drawn(1)), None)
    }
```

(`Situation.defaultAnswer` keeps the first option, so the first drawn
legacy is the one kept.) Add four more tests in the same style:

- **The question:** park on `SetupProcedure.legacyDecisionId(first)` for
  the first player in turn order, the way `SetupProcedureSuite` parks on
  `adviserDecisionId`
  (`git grep -n "parkedAt\|adviserDecisionId" -- src/test/scala/distantlands/gameplay/setup`),
  and assert its query is a `ChooseOne` whose option refs are
  `deal.drawn(orders.legacyOrder, first).map(DecisionOptionRef.Legacy(_))`.
  Answering the second option keeps the second legacy.

- **Short deck:** a Chronicle whose lineages hold all but 3 legacies
  (`LegacyDeal.deck` leaves 3), so the third seat in seat order draws 1
  and the others 2 and 0 (seat order is `orders.participants`). After
  Setup the seat that drew 1 holds it dormant with no
  `legacyDecisionId` park, and the seat that drew 0 holds nothing new
  (R9).
- **Public Ambitions:** a Setup walk under `publicAmbitions(chronicle)`
  parks on no `legacyDecisionId` and ends with the row unchanged.
- **Log:** the events of `FirstGameSetupFixture.setupThroughWake()` hold,
  for each player, one `LogNoted` with key `SetupProcedure.keptLegacy`
  whose only argument is `NoteArg.Player(player)`; under Public
  Ambitions, one with key `SetupProcedure.dealtPublicLegacies` and
  argument `NoteArg.Cards(row)`, before any pawn line.

In the parked-decision projection suite, add: projecting the first
player's parked `legacyDecisionId` to another seat names neither drawn
legacy (no option row and no card carries either id), and to its owner
names both.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LegacySetupSuite"`
Expected: FAIL to compile: `value legacyDecisionId is not a member of
object distantlands.gameplay.setup.SetupProcedure`.

- [ ] **Step 3: Declare the notes and the id**

In `SetupProcedure`:

```scala
  /** "p2 kept a legacy". It names no card: the kept legacy is dormant, and
    * only its owner may know it (Legacies ruling 1). */
  val keptLegacy: NoteKey = notes.key("kept-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy")))
  /** "Public Ambitions dealt Rival, Seer, Arbiter and Wealthy". */
  val dealtPublicLegacies: NoteKey = notes.key("dealt-public-legacies",
    Vector(NotePart.Text("Public Ambitions dealt "), NotePart.Arg(0)))

  def legacyDecisionId(player: PlayerId): String =
    s"setup.legacy-choice.${player.value}"
```

- [ ] **Step 4: Ask the question**

1. In `tree`, put a `publicRow` note first in the sequence:

```scala
  /** The Public Ambitions row game start dealt, named once (Legacies
    * design, slice 2). Nothing when the row is empty. */
  private def publicRow(ready: ReadyGame): Vector[Operation] =
    val row = ready.game.current.commonCards.publicLegacies
    Option.when(row.nonEmpty)(Note(ProcedureNotes.id(
      TriggeredProcedureRef.Setup), _ => Some(dealtPublicLegacies.sourceless(
        NoteArg.Cards(row))))).toVector
```

and `Sequence(publicRow(ready) ++ chancellor.map(...)...`.

2. In `playerStep`, between the pawn pair and the adviser `Branch`, add
   `keepLegacy(participant)`:

```scala
  /** Quiet Ambitions: keep one of the two legacies game start drew,
    * facedown, and return the other to the deck (CR "Set Up Players";
    * Legacies design, slice 2). The drawn legacies are the dormant ones
    * the lineage holds that its baseline record does not, read when the
    * question is reached. A short deck's single legacy is kept without a
    * question; a player who drew none is asked nothing. */
  private def keepLegacy(participant: FirstGameParticipant): Operation =
    val decisionId = legacyDecisionId(participant.playerId)
    Branch((state, _) =>
      drawnLegacies(state, participant.lineageId) match
        case Vector() => Vector.empty
        case Vector(_) => Vector(keptLegacyNote(participant.playerId))
        case drawn => Vector(
          Decide(decisionId, participant.playerId, DecisionQuery.ChooseOne(
            drawn.map(id => DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
            heading = Some("Keep a legacy"), confirmVerb = Some("Keep"))),
          BuildOps(returnLegacies(participant.lineageId, drawn, decisionId)),
          keptLegacyNote(participant.playerId)))

  private def keptLegacyNote(player: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      _ => Some(keptLegacy.sourceless(NoteArg.Player(player))))

  private def drawnLegacies(ready: ReadyGame, lineage: LineageId)
      : Vector[LegacyId] =
    val stored = ready.game.campaign.baseline.lineages.find(_.id == lineage)
      .toVector.flatMap(_.legacies).map(_.id).toSet
    ready.game.campaign.lineages.get(lineage).toVector
      .flatMap(_.legacies).collect {
        case LegacyState(id, false) if !stored(id) => id }

  private def returnLegacies(lineage: LineageId, drawn: Vector[LegacyId],
      decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      pending.answered.find(_.decisionId == decisionId).map(_.answer) match
        case Some(DecisionAnswer.ChooseOneAnswer(
            DecisionOptionRef.Legacy(kept))) if drawn.contains(kept) =>
          Right(drawn.filterNot(_ == kept).map(id => Move(Piece.Card(id),
            PositionedLocation(Location.Legacies(lineage)),
            PositionedLocation(Location.Deck(CardDeck.Legacy)))))
        case _ => Left(InvalidEventOrder(
          s"no legacy answer is recorded for $decisionId"))
```

3. Update the tree diagram in the object's doc comment with the public
   row note and the keep branch.

- [ ] **Step 5: Run the tests to verify they pass, and rewrite the goldens**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.*"`
Expected: PASS except `GameLogGoldenSuite` and `DeclaredNoteKeysSuite`.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
then `git diff src/test/resources/notes`: exactly the two new
`procedure.setup` lines.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
then `git diff --stat src/test/resources/gamelog` and read the diff. Each
changed log gains one "kept a legacy" line per player in Setup, names no
legacy in the `.other.log`, names none in the `.actor.log` either (the
line names no card), and shifts later event numbers. Any other change is
a bug: stop and find it.

Run: `./sbtw test`
Expected: PASS. A suite that counts Setup's decisions or events, or
answers Setup's parks by position, now meets one more park per player;
update it to answer or count the keep decision. List every such suite in
the commit message body.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala \
  src/test/scala src/test/resources/notes src/test/resources/gamelog
git commit -m "feat(setup): keep one drawn legacy in Setup and log the deal

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The record keeps a new lineage's legacies (R1)

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala`
  (`campaignFields`)
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala`

**Interfaces:**
- Produces: a seated lineage the baseline does not list is written with
  its live legacies (implementation ruling R1).

- [ ] **Step 1: Write the failing test**

In `ChronicleRecordSuite` (import `distantlands.catalog.holding.SeerCard`):

```scala
  test("a lineage the baseline lacks is recorded with its legacies"):
    val ready = Table.start.legacy(p1, SeerCard, active = false).ready
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.lineages.find(_.id == lineageOf(ready, p1))
      .map(_.legacies), Some(Vector(LegacyState(SeerCard.id, active = false))))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleRecordSuite"`
Expected: FAIL: the lineage is recorded with no legacies.

- [ ] **Step 3: Write them**

In `campaignFields`, in `added`, replace the fourth argument
`Vector.empty` with `live.legacies`. Update the object's doc comment:
"Roles, era scores, the Oathkeeper goal, the foundation faces, legacies and
starting advisers come from the live campaign".

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala
git commit -m "fix(chronicle): record the legacies of a lineage seated for the first time

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Docs and the gate

**Files:**
- Modify: `docs/rules/ambiguities.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md`
  (status line)

- [ ] **Step 1: `docs/rules/ambiguities.md`**

1. Item 2, "Foundation faces": replace "Foundation III's faces are skipped
   until Legacies." with "Foundation III deals legacies at game start:
   Quiet Ambitions draws 2 for each player to keep one in Setup, and
   Public Ambitions deals a faceup row of 4 (Legacies design, slice 2)."
   with "Legacies design" linked to
   `../superpowers/specs/2026-11/2026-11-13-legacies-design.md`.
2. Foundations rulings item 1, "Foundation III waits for Legacies": append
   "Answered by the Legacies design, slice 2: both faces now deal."
3. Complete the "Legacies rulings" section (R17): items 1-10 become the
   design's rulings 1-10, each a sentence or two with its reason, and
   R4 follows as item 11. Update the section's lead to match.

- [ ] **Step 2: `docs/rules/implementation-traceability.md`**

In the row "Six foundation setup effects and world-deck construction",
add to the evidence: "`LegacyDeal` deals Quiet Ambitions' 2 legacies a
player, kept in Setup, or Public Ambitions' faceup row of 4
(`gameplay/setup/LegacyDealSuite.scala`, `LegacySetupSuite.scala`)." and
drop "Foundation III's faces wait for Legacies;" from its gaps.

- [ ] **Step 3: The design**

In the Legacies design, "Slice 2: setup deal", replace "It is the one deal
arithmetic the plan factory, game start and the materializer share." with
"It is the one deal arithmetic the plan factory and game start share;
game start applies it (implementation ruling R16)." Then replace "Slice 1 landed (plan: ...)." with
"Slices 1 and 2 landed (plans: [slice 1](../../plans/2026-11/2026-11-13-legacies-slice1.md),
[slice 2](../../plans/2026-11/2026-11-13-legacies-slice2.md))."

- [ ] **Step 4: Run the verification gate**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statements at or above 92% and branches at or above 84.4%.

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

Run: `python3 scripts/check-markdown-links.py`
Expected: `Markdown link check passed`.

Run: `git status --short src/test/resources`
Expected: clean. Task 5 committed its rewritten goldens; nothing else
changes them.

- [ ] **Step 5: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/implementation-traceability.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md
git commit -m "docs(legacies): record slice 2

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
