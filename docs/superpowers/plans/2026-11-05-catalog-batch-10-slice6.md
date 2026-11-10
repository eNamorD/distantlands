# Catalog Batch 10 Slice 6: Dispossessed Draws Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the walker shuffle the Dispossessed, and implement Great Spire (E11 intact), Fallen Spire (E11 ruined) and Pilgrimage (161) on that draw.

**Architecture:**
- `ShufflePile.Dispossessed` reads and writes `campaign.dispossessed`, with a codec kind. The walker fills its order like any other pile's, and the shuffle forgets every card's knowledge.
- A new `DispossessedDraws` object, beside `ReliquaryDraws`, holds what the three cards share:
  - the `Shuffle` node a draw starts with;
  - the moves of the Dispossessed's first cards onto the top of a regional discard pile;
  - the bottom denizens of a pile, skipping Visions;
  - the move of a pile card into the Dispossessed.
- **Great Spire** is a `PaidAction`. A `Branch` reads the count, `min(3, Dispossessed size)` (ruling 7), then shuffles. One batch tops the pile with that many Dispossessed cards; a second batch, read after the first, sends that many of the pile's bottom denizens to the Dispossessed.
- **Fallen Spire** needs two `Shuffle` nodes, and a `Shuffle` is a tree node the walker orders, never a member of a `BuildOps` batch. `RuinedSetupRule` builds one batch, so it splits:
  - the new base, `RuinedSetupSteps`, adds whatever steps a face returns;
  - `RuinedSetupRule` extends it, and its subclasses are unchanged;
  - `FallenSpire` extends `RuinedSetupSteps` directly.
- **Pilgrimage** needs each card's token returns and its move to be one operation. A lock (printed, or Ancient Bloodline's) must then refuse the returns with the move, and the card keeps its tokens. A new composite `CoreOperation`, `RemoveFromMap`, does this, the way `Discard.Denizen` does for a discard:
  - a denizen goes to the Dispossessed;
  - a ruined edifice goes to the bottom of the edifice deck, which keeps no face (ruling 9).
  `CoreOperation` is sealed, so the composite goes in `CoreOperations.scala`. That file is at 786 of its 800 lines, so `Piece`, `SecretSide`, `StackPosition` and `PositionedLocation` move to a new `model/Pieces.scala` first.
- Pilgrimage peeks each drawn card for its user and shows them in an `Inspect`, as Scryer does. The peek records the card in the user's knowledge, so only the user's projection names it.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Rulings" 7 and 9
- "Readings" (Great Spire, Fallen Spire, Pilgrimage)
- "Slice 6: Dispossessed draws"
- "Testing"

Card text:
- Great Spire (`HoldingEdifices.scala`): "**ACTION:** Add 3 random cards from the Dispossessed to this region's discard pile, then add the bottom 3 denizens from that pile to the Dispossessed." Cost: 1 secret.
- Fallen Spire: "SETUP (END) / **WHEN EXPLORED:** Swap denizens in this region's discard pile with the same number of random cards from the Dispossessed. Then, shuffle these cards with any Visions in this pile."
- Pilgrimage (`HoldingNomadDenizens.scala`): "**WHEN PLAYED:** move all denizens at your site to the Dispossessed. Shuffle and draw denizens from the Dispossessed equal to the number you moved. Peek at them and put them on your region's discard pile."

**Readings this plan takes** (each also goes into the spec's "Readings" in Task 5):
- **Fallen Spire with a short Dispossessed.** Ruling 7 sets the count. The pile's bottom-most denizens are the ones that leave.
- **Fallen Spire with no denizen in the pile.** Nothing moves, nothing is shuffled, and no line is written, as Ruined School writes none with no bandit site.
- **Great Spire with an empty Dispossessed.** It cannot happen (ruling 7). If it did, the cost is paid and nothing moves.
- **Pilgrimage and locks.** Ruling 9 keeps intact edifices because locked cards cannot be moved. The same holds for every card a lock keeps: a lock-icon denizen, or a denizen Ancient Bloodline locks for the user. Each stays with its tokens and is not counted.
- **Line wording.** The lines name a pile as `NoteArg.Pile` renders it, "the Cradle discard pile", as Scryer and Broken Horn do, not "the Cradle's discard pile".

This is the sixth of 14 slice plans, written against `0.2.0-alpha` at `d869897b`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-6` on branch `batch-10-slice-6`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports and unused values fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains.
- **Hidden information:**
  - The projection names a card in a discard pile only to a viewer whose `knowledge.advisers` records it (`GamePresentationProjector.identifiesCard`). `Look.knowsAdviser` reads the same record.
  - A card moved out of the Dispossessed was known to nobody, and stays unknown in the pile (`CardKnowledgeMoves`) until someone peeks.
  - A shuffle forgets every card of its pile (`PileOperations`).
- **Log lines:** a line names only non-actors (memory: "Log lines omit actor"). Pilgrimage's line covers its peek step, so no generic "peeked at" line shows a card.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").
- **Edifices are denizens** (memory): Pilgrimage moves a ruined edifice like a denizen, to its own pile. Ruling 9 keeps it out of the count.

---

### Task 1: The Dispossessed as a shuffle pile

**Files:**
- Modify: `src/main/scala/distantlands/model/ActionValues.scala:76-108` (`ShufflePile`)
- Modify: `src/main/scala/distantlands/gameplay/operations/PileOperations.scala:28-46`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala:424-443`
- Modify: `src/test/scala/distantlands/testkit/Table.scala` (new `dispossessed` step)
- Test: `src/test/scala/distantlands/gameplay/operations/ShuffleOperationSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala:455`
- Test: `src/test/scala/distantlands/testkit/TableSuite.scala`

**Interfaces:**
- Consumes: `campaign.dispossessed: Vector[WorldCardId]`; `Table.worldDeckBottom`, `Table.moving`, `Table.update`.
- Produces:
  - `case object ShufflePile.Dispossessed extends ShufflePile`, named "Dispossessed", with wire kind `"dispossessed"`.
  - `Table.dispossessed(cards: WorldCardRef*): Table`. The Dispossessed holds exactly `cards`, first named at its head. Any card it held before goes to the bottom of the world deck.

- [ ] **Step 1: Write the failing tests**

In `ShuffleOperationSuite.scala`, add after the Reliquary test:

```scala
  /** The quiet table with exactly Fae Merchant, Second Chance and
    * Threatening Roar in the Dispossessed, in that order. */
  private val threeDispossessed = Table.start.dispossessed(FaeMerchantCard,
    SecondChanceCard, ThreateningRoarCard).ready

  test("a shuffle writes the Dispossessed's new order"):
    val three = threeDispossessed.game.campaign.dispossessed
    val done = accepted(executor.execute(threeDispossessed,
      Shuffle(ShufflePile.Dispossessed, Some(three.reverse))))
    assertEquals(done.game.campaign.dispossessed, three.reverse)

  test("an order that drops a Dispossessed card is refused"):
    val three = threeDispossessed.game.campaign.dispossessed
    assertEquals(executor.execute(threeDispossessed,
      Shuffle(ShufflePile.Dispossessed, Some(three.tail))).left.toOption,
      Some(OperationError.InvalidDescription(
        "a shuffle of the Dispossessed must hold exactly its cards")))
```

In `WalkerOperationWireSuite.scala`, after `Shuffle(ShufflePile.Reliquary),` (line 455), add:

```scala
      Shuffle(ShufflePile.Dispossessed, Some(Vector(denizen))),
```

In `TableSuite.scala`, add a test. Import `oathdigital.gameplay.powers.action.ScryerCard` and `oathdigital.gameplay.powers.whenplayed.DazzleCard` if they are not imported yet:

```scala
  test("dispossessed holds exactly the named cards, and moves the cards it " +
      "held to the bottom of the world deck"):
    val before = Table.start.ready.game.campaign.dispossessed
    val named = Vector[WorldCardId](DazzleCard.id, ScryerCard.id)
    val ready = Table.start.dispossessed(DazzleCard, ScryerCard).ready
    assertEquals(ready.game.campaign.dispossessed, named)
    val earlier = before.filterNot(named.contains)
    assertEquals(ready.game.current.commonCards.worldDeck
      .takeRight(earlier.size), earlier)
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ShuffleOperationSuite oathdigital.serialization.WalkerOperationWireSuite oathdigital.testkit.TableSuite"`
Expected: compile failure. `ShufflePile.Dispossessed` and `Table.dispossessed` do not exist.

- [ ] **Step 3: Implement the pile**

In `ActionValues.scala`:
- Add `Dispossessed` after `Reliquary`:

```scala
  /** The Dispossessed, for the random cards of Great Spire, Fallen Spire
    * and Pilgrimage (catalog batch 10, slice 6). */
  case object Dispossessed extends ShufflePile
```

- In `cards`, add `case Dispossessed => game.campaign.dispossessed`.
- In `name`, add `case Dispossessed => "Dispossessed"`.
- In the trait's doc, replace "or the set-aside relics (Scatter Relics deals them out)." with "the set-aside relics (Scatter Relics deals them out), or a campaign pile: a suited reserve, the Reliquary or the Dispossessed."

In `PileOperations.withCards`:
- Add a case before `case other =>`:

```scala
    case ShufflePile.Dispossessed => ready.updateCampaign(_.copy(
      dispossessed = cards.collect { case id: WorldCardId => id }))
```

- Change the inner fallthrough to `case ShufflePile.SuitedReserve(_) | ShufflePile.Reliquary | ShufflePile.Dispossessed => current`.

In `WalkerOperationCodec.scala`:
- In `encodeShufflePile`, add `case ShufflePile.Dispossessed => ujson.Obj("kind" -> "dispossessed")`.
- In `decodeShufflePile`, add `case Some("dispossessed") => Right(ShufflePile.Dispossessed)` after the `"reliquary"` case.

In `Table.scala`, after `worldDeckBottom`, add:

```scala
  /** The Dispossessed holds exactly `cards`, first named at its head: the
    * card a draw takes first when the shuffle keeps the order. Any card it
    * held before goes to the bottom of the world deck, so the inventory
    * stays whole. */
  def dispossessed(cards: WorldCardRef*): Table =
    val ids = cards.toVector.map(CardRefs.worldCardId)
    val earlier = game.game.campaign.dispossessed.filterNot(ids.contains)
    ids.foldLeft(worldDeckBottom(earlier*))(_.moving(_)).update(
      _.updateCampaign(_.copy(dispossessed = ids)))
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.ShuffleOperationSuite oathdigital.serialization.WalkerOperationWireSuite oathdigital.testkit.TableSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/ActionValues.scala \
  src/main/scala/distantlands/gameplay/operations/PileOperations.scala \
  src/main/scala/distantlands/serialization/WalkerOperationCodec.scala \
  src/test/scala/distantlands/testkit/Table.scala \
  src/test/scala/distantlands/testkit/TableSuite.scala \
  src/test/scala/distantlands/gameplay/operations/ShuffleOperationSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(operations): shuffle the Dispossessed with a recorded order

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Great Spire

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/DispossessedDraws.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/action/GreatSpire.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala:53-67` (remove `GreatSpireCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala` (receive `GreatSpireCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/action/OtherActionPowers.scala:19`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/powers/declared-keys.txt` (regenerated)
- Test: `src/test/scala/distantlands/gameplay/powers/action/GreatSpireSuite.scala`

**Interfaces:**
- Consumes: `ShufflePile.Dispossessed` and `Table.dispossessed` (Task 1); `PaidAction`; `EdificeSetupSupport.siteOf(ready, edifice, side): Option[SiteId]`; `ActPowerHarness.{rules, use, replayed}`; `WalkerDiceFixture.{shufflingBy, reversingShuffle}`; `PhasePowerHarness.backToActing`.
- Produces:
  - `object GreatSpireCard extends Edifice(EdificeId("E11"), Suit.Arcane)` in package `oathdigital.gameplay.powers.setup`, with both faces' text unchanged.
  - `object DispossessedDraws` in package `oathdigital.gameplay.powers`:
    - `def shuffle: Shuffle`
    - `def toPile(ready: ReadyGame, region: Region, count: Int, by: Option[PlayerId]): Vector[CoreOperation]`
    - `def bottomDenizens(ready: ReadyGame, region: Region, count: Int): Vector[DenizenId]`
    - `def intoDispossessed(card: DenizenId, region: Region, by: Option[PlayerId]): CoreOperation`
  - `case object GreatSpire extends PaidAction`, with `val Count: Int = 3` and `val used: NoteKey`.

The quiet table (`PowerFixture.quietTable`): the regional discards are empty, and p1 is the actor. Great Clock's suite stages its edifice at Ancient City with p1's pawn there; this suite does the same.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/action/GreatSpireSuite.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.powers.setup.GreatSpireCard
import oathdigital.gameplay.powers.whenplayed.{BewitchCard, DazzleCard,
  ThreateningRoarCard}
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Great Spire (E11 intact, catalog batch 10, slice 6): pay 1 secret, put
  * up to 3 shuffled Dispossessed cards on top of the Spire's region's
  * discard pile, then send as many of that pile's bottom denizens to the
  * Dispossessed (ruling 7). */
class GreatSpireSuite extends munit.FunSuite:
  import ActPowerHarness._
  import PowerFixture._

  private val spire = GreatSpireCard.id
  private val source = DecisionOptionRef.Edifice(spire)
  private val home = AncientCitySite.id
  private val region: Region = quietTable.game.current.map.regionOf(home).get
  private val pileName: String =
    SearchSource.name(SearchSource.RegionalDiscard(region))
  private val conspiracy = VisionRules.Conspiracy

  private val keepingOrder: OathRules =
    rules(WalkerDiceFixture.shufflingBy(count => (0 until count).toVector))
  private val reversing: OathRules = rules(WalkerDiceFixture.reversingShuffle)

  private val six = Vector[WorldCardId](DazzleCard.id, ScryerCard.id,
    BewitchCard.id, ThreateningRoarCard.id, FaeMerchantCard.id,
    SecondChanceCard.id)

  /** p1's Act at Ancient City beside the intact Great Spire, with 2 faceup
    * secrets. The Dispossessed holds `dispossessed`, head first, and the
    * Spire's region's discard pile holds `pile`, bottom first. */
  private def spireWith(dispossessed: Vector[WorldCardId],
      pile: Vector[WorldCardId]): ReadyGame =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(GreatSpireCard, EdificeSide.Intact, at = home)
      .secrets(actor, 2).dispossessed(dispossessed*)
      .discarded(region, pile*).ready

  private def spireUsed(start: ReadyGame, under: OathRules = keepingOrder)
      : OathTransition = accepted(use(under, start, GreatSpire.id, source))

  private def swappedLine(count: Int): Vector[NoteText.Said] =
    Vector(NoteText.Said(NoteKey.Used,
      s"Swapped $count cards between the Dispossessed and the $pileName.",
      covers = false))

  test("it pays 1 secret, tops the pile with 3 Dispossessed cards, and " +
      "sends the pile's bottom 3 denizens to the Dispossessed past the " +
      "Vision"):
    val start = spireWith(six, Vector(EldersCard.id, conspiracy,
      MessengerCard.id, HomesteadersCard.id))
    val done = spireUsed(start)
    assert(PhasePowerHarness.backToActing(done))
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      conspiracy, DazzleCard.id, ScryerCard.id, BewitchCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      ThreateningRoarCard.id, FaeMerchantCard.id, SecondChanceCard.id,
      EldersCard.id, MessengerCard.id, HomesteadersCard.id))
    assertEquals(Look(after).tokensOn(spire), Tokens(0, 1))
    assertEquals(NoteText.said(GreatSpire, done.events), swappedLine(3))

  test("nobody learns a card the Spire draws"):
    val after = ready(spireUsed(spireWith(six, Vector(EldersCard.id,
      MessengerCard.id, HomesteadersCard.id))).state)
    Vector(Table.p1, Table.p2, Table.p3).foreach(player =>
      Vector(DazzleCard.id, ScryerCard.id, BewitchCard.id).foreach(card =>
        assert(!Look(after).knowsAdviser(player, card), s"$player $card")))

  test("the draw takes the shuffle's order, and replay applies the " +
      "recorded order"):
    val start = spireWith(six, Vector(EldersCard.id, MessengerCard.id,
      HomesteadersCard.id))
    val done = spireUsed(start, reversing)
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      SecondChanceCard.id, FaeMerchantCard.id, ThreateningRoarCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      BewitchCard.id, ScryerCard.id, DazzleCard.id, EldersCard.id,
      MessengerCard.id, HomesteadersCard.id))
    // Replay has no shuffle source: it applies the recorded order.
    assertEquals(replayed(rules(), start, done.events), after)
    assertWireRoundTrips(done.events)

  test("a pile with fewer than 3 denizens sends back some of the cards " +
      "just added"):
    val after = ready(spireUsed(spireWith(six,
      Vector(EldersCard.id, conspiracy))).state)
    assertEquals(Look(after).discardPile(region),
      Vector[WorldCardId](conspiracy, BewitchCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      ThreateningRoarCard.id, FaeMerchantCard.id, SecondChanceCard.id,
      EldersCard.id, DazzleCard.id, ScryerCard.id))

  test("a Dispossessed of 2 cards moves 2 in each direction"):
    val done = spireUsed(spireWith(Vector(DazzleCard.id, ScryerCard.id),
      Vector(EldersCard.id, MessengerCard.id, HomesteadersCard.id)))
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      HomesteadersCard.id, DazzleCard.id, ScryerCard.id))
    assertEquals(after.game.campaign.dispossessed,
      Vector[WorldCardId](EldersCard.id, MessengerCard.id))
    assertEquals(NoteText.said(GreatSpire, done.events), swappedLine(2))
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GreatSpireSuite"`
Expected: compile failure. `GreatSpireCard` is not in `oathdigital.gameplay.powers.setup`, and `GreatSpire` does not exist.

- [ ] **Step 3: Move the card**

Cut `object GreatSpireCard` (both faces, text unchanged) from `HoldingEdifices.scala`. Paste it into `RuinedSiteSetupRules.scala` after `object GreatClockCard`. The file already imports `Edifice`, `EdificeFace`, `Locked` and `PrintedPower`. If `HoldingEdifices.scala` now has an unused import, remove it.

- [ ] **Step 4: Write the draws**

Create `src/main/scala/distantlands/gameplay/powers/DispossessedDraws.scala`:

```scala
package oathdigital.gameplay.powers

import oathdigital.model._

/** Draws from the Dispossessed (catalog batch 10, slice 6): Great Spire,
  * Fallen Spire and Pilgrimage. A draw shuffles the Dispossessed, so the
  * walker journals its order, then takes cards from its head, as
  * [[ReliquaryDraws]] takes a relic. Nobody knows a card in the
  * Dispossessed, and one moved from it into a discard pile stays unknown
  * until someone peeks (`CardKnowledgeMoves`).
  */
object DispossessedDraws:
  /** The shuffle a draw starts with. It is a tree node, never a member of a
    * batch: the walker fills its order. */
  def shuffle: Shuffle = Shuffle(ShufflePile.Dispossessed)

  /** The first `count` cards of the Dispossessed, read when the batch is
    * built, each put on top of `region`'s discard pile in turn, so the last
    * one taken lies on top. */
  def toPile(ready: ReadyGame, region: Region, count: Int,
      by: Option[PlayerId]): Vector[CoreOperation] =
    ready.game.campaign.dispossessed.take(count).map(card => Move(
      Piece.Card(card), PositionedLocation(Location.Dispossessed),
      PositionedLocation(Location.RegionalDiscard(region), StackPosition.Top),
      by = by))

  /** The bottom `count` denizens of `region`'s discard pile, bottom first,
    * skipping Visions. A pile is stored top last, so its bottom is its
    * head. */
  def bottomDenizens(ready: ReadyGame, region: Region, count: Int)
      : Vector[DenizenId] =
    ready.game.current.commonCards.discard(region)
      .collect { case denizen: DenizenId => denizen }.take(count)

  /** `card` from `region`'s discard pile to the end of the Dispossessed. */
  def intoDispossessed(card: DenizenId, region: Region,
      by: Option[PlayerId]): CoreOperation =
    Move(Piece.Card(card),
      PositionedLocation(Location.RegionalDiscard(region)),
      PositionedLocation(Location.Dispossessed), by = by)
```

- [ ] **Step 5: Write the action**

Create `src/main/scala/distantlands/gameplay/powers/action/GreatSpire.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powers.DispossessedDraws
import oathdigital.gameplay.powers.setup.{EdificeSetupSupport, GreatSpireCard}
import oathdigital.model._

/** Great Spire (E11, intact), ACTION: place 1 secret on this card, add 3
  * random cards from the Dispossessed to this region's discard pile, then
  * add the bottom 3 denizens from that pile to the Dispossessed (catalog
  * batch 10, slice 6).
  *
  * The draw is [[DispossessedDraws]]'s: the Dispossessed is shuffled, and
  * its first cards go on top of the pile. Both directions move the same
  * count, the smaller of 3 and the Dispossessed's size (ruling 7). The
  * bottom denizens are read after the new cards arrive, skipping Visions,
  * so a pile with fewer than 3 denizens sends back some of the new cards.
  * Nobody peeks. An empty Dispossessed cannot occur (ruling 7); if it did,
  * nothing would move and no line would be written. Its line: "Swapped 3
  * cards between the Dispossessed and the Cradle discard pile."
  */
case object GreatSpire extends PaidAction(GreatSpireCard.intact.power):
  val Count: Int = 3

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Swapped {3} {cards} between the Dispossessed and the {Cradle discard
    * pile}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Swapped "), NotePart.Arg(0),
    NotePart.Plural(0, " card", " cards"),
    NotePart.Text(" between the Dispossessed and the "), NotePart.Arg(1),
    NotePart.Text(".")))

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](Branch((live, _) =>
      spireRegion(live).toVector.flatMap { region =>
        val count = math.min(Count, live.game.campaign.dispossessed.size)
        if count == 0 then Vector.empty
        else swap(player, region, count)
      }))))

  private def swap(player: PlayerId, region: Region, count: Int)
      : Vector[Operation] = Vector(
    DispossessedDraws.shuffle,
    BuildOps((now, _) => Right(DispossessedDraws.toPile(now, region, count,
      Some(player))), required = true),
    BuildOps((now, _) => Right(DispossessedDraws.bottomDenizens(now, region,
      count).map(DispossessedDraws.intoDispossessed(_, region, Some(player)))),
      required = true),
    Note(id, _ => Some(used(PowerSourceRef.Card(GreatSpireCard.id),
      NoteArg.Number(count),
      NoteArg.Pile(SearchSource.RegionalDiscard(region))))))

  /** The region of the site where the intact Spire stands. */
  private def spireRegion(ready: ReadyGame): Option[Region] =
    EdificeSetupSupport.siteOf(ready, GreatSpireCard.id, EdificeSide.Intact)
      .flatMap(ready.game.current.map.regionOf)
```

In `OtherActionPowers.scala`, add `GreatSpire,` after `GreatClock,`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.GreatSpireSuite"`
Expected: PASS.

- [ ] **Step 7: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then `git diff src/test/resources`.
Expected:
- `unimplemented.txt` drops exactly `edifice.e11.intact`.
- `declared-keys.txt` gains exactly the key `used` of `edifice.e11.intact`.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/DispossessedDraws.scala \
  src/main/scala/distantlands/gameplay/powers/action/GreatSpire.scala \
  src/main/scala/distantlands/gameplay/powers/action/OtherActionPowers.scala \
  src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala \
  src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/powers/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/action/GreatSpireSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Great Spire swaps cards between the Dispossessed and its region's discard pile

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 1 and Task 2 diffs against this plan and the spec.

---

### Task 3: Fallen Spire

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala:45-73` (`RuinedSetupRule` split)
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala` (new `FallenSpire`)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:118`
- Modify: `src/test/scala/distantlands/gameplay/setup/SetupWalkDriver.scala:27-48` (a dice source)
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/powers/declared-keys.txt` (regenerated)
- Test: `src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala`

**Interfaces:**
- Consumes: `DispossessedDraws` and `GreatSpireCard` (Task 2); `Table.dispossessed` (Task 1); `ProcedureWalker.advance(state, action, pending, powers, dice)` and `ProcedureWalker.resolve(state, action, pending, answer, powers, dice)`.
- Produces:
  - `abstract class RuinedSetupSteps extends ContributingPower`, with `def edifice: EdificeId` and `protected def steps(site: SiteId): Vector[Operation]`.
  - `abstract class RuinedSetupRule extends RuinedSetupSteps`, with the same `build` and `note` members as now.
  - `case object FallenSpire extends RuinedSetupSteps`, with `val swapped: NoteKey`.
  - `SetupWalkDriver.driveWithEvents(ready, tree, procedure, powers, dice = WalkerDice.unavailable)`.

The Setup walk in `RuinedSetupRulesSuite` starts from `FirstGameSetupFixture.freshReady`. Its reveals may discard more cards into the Cradle pile, so each test reads the pile that a walk with the intact Spire leaves, as the Broken Horn tests do. The Setup tree has no other shuffle; the existing suites pass with no dice source. So a reversing dice source changes only the Spire's two shuffles.

- [ ] **Step 1: Give the Setup driver a dice source**

In `SetupWalkDriver.scala`:
- Import `WalkerDice` with the other `oathdigital.gameplay.walker` names.
- Add `dice: WalkerDice = WalkerDice.unavailable` after `powers: WalkerPowers` in `driveWithEvents`.
- Pass `dice` as the fifth argument of `ProcedureWalker.advance` and the sixth of `ProcedureWalker.resolve`.
- Leave `driveToCompletion` as it is.

- [ ] **Step 2: Write the failing tests**

In `RuinedSetupRulesSuite.scala`:
- Add the imports:
  - `oathdigital.gameplay.WalkerDiceFixture`
  - `oathdigital.gameplay.walker.WalkerDice`, beside `WalkerPowers`
  - `ScryerCard`, beside `EldersCard` and `MessengerCard`
  - `oathdigital.gameplay.powers.whenplayed.{BewitchCard, DazzleCard, ThreateningRoarCard}`
- Change `finish` to take a dice source:

```scala
  private def finish(ready: ReadyGame,
      dice: WalkerDice = WalkerDice.unavailable): SetupWalkDriver.SetupWalked =
    val tree = accepted(SetupProcedure.build(catalog, ready,
      ready.game.current.turn.activePlayer, Vector.empty))
    SetupWalkDriver.driveWithEvents(ready, tree, TriggeredProcedureRef.Setup,
      powers, dice)
```

- Change `said`'s parameter type from `RuinedSetupRule` to `RuinedSetupSteps`.
- Update the class doc's first sentence to "The SETUP (END) rules of catalog batch 9, slice 1 (Hall of Mockery, Lost Guardians, Ruined School and Broken Horn) and of batch 10 (Broken Clock and Fallen Spire)."
- Append:

```scala
  // ---- Fallen Spire ----

  private val reversing = WalkerDiceFixture.reversingShuffle
  private val fourDispossessed = Vector[WorldCardId](DazzleCard.id,
    ScryerCard.id, BewitchCard.id, ThreateningRoarCard.id)

  /** The Spire on `side` at the Cradle site, with Elders, Messenger and the
    * Conspiracy in the Cradle's pile beside what Setup's reveals discard
    * there, and exactly `dispossessed` in the Dispossessed. */
  private def spireWithPile(side: EdificeSide,
      dispossessed: Vector[WorldCardId]): ReadyGame =
    stagedWith(GreatSpireCard.id, side).discarded(Region.Cradle, staged*)
      .dispossessed(dispossessed*).ready

  /** The Cradle's pile as Setup leaves it under the intact Spire, which
    * swaps nothing. */
  private def pileLeftByIntactSpire(dispossessed: Vector[WorldCardId])
      : Vector[WorldCardId] =
    Look(finish(spireWithPile(EdificeSide.Intact, dispossessed), reversing)
      .ready).discardPile(Region.Cradle)

  private def denizensOf(pile: Vector[WorldCardId]): Vector[DenizenId] =
    pile.collect { case denizen: DenizenId => denizen }

  private def swappedLine(count: Int, noun: String): Vector[NoteText.Said] =
    Vector(NoteText.Said("swapped", s"Swapped $count $noun between the " +
      "Dispossessed and the Cradle discard pile, then shuffled the pile.",
      covers = false))

  test("Fallen Spire swaps the pile's denizens for as many shuffled " +
      "Dispossessed cards, then shuffles the pile with its Vision"):
    val pile = pileLeftByIntactSpire(fourDispossessed)
    val count = math.min(denizensOf(pile).size, fourDispossessed.size)
    // Elders and Messenger at least.
    assert(count >= 2, pile.toString)
    val leaving = denizensOf(pile).take(count)
    // The reversing source reverses the Dispossessed, then the pile.
    val arriving = fourDispossessed.reverse.take(count)
    val walked = finish(spireWithPile(EdificeSide.Ruined, fourDispossessed),
      reversing)
    val after = Look(walked.ready).discardPile(Region.Cradle)
    assertEquals(after, (pile.filterNot(leaving.contains) ++ arriving).reverse)
    assert(after.contains(conspiracy))
    assertEquals(walked.ready.game.campaign.dispossessed,
      fourDispossessed.reverse.drop(count) ++ leaving)
    Vector(Table.p1, Table.p2, Table.p3).foreach(player =>
      arriving.foreach(card =>
        assert(!Look(walked.ready).knowsAdviser(player, card))))
    assertEquals(said(FallenSpire, walked.events), swappedLine(count, "cards"))

  test("with one Dispossessed card, Fallen Spire swaps it for the pile's " +
      "bottom denizen"):
    val one = Vector[WorldCardId](DazzleCard.id)
    val pile = pileLeftByIntactSpire(one)
    val leaving = denizensOf(pile).head
    val walked = finish(spireWithPile(EdificeSide.Ruined, one), reversing)
    assertEquals(walked.ready.game.campaign.dispossessed,
      Vector[WorldCardId](leaving))
    assertEquals(Look(walked.ready).discardPile(Region.Cradle),
      (pile.filterNot(_ == leaving) :+ DazzleCard.id).reverse)
    assertEquals(said(FallenSpire, walked.events), swappedLine(1, "card"))

  test("the intact Great Spire swaps nothing at Setup's end"):
    val walked = finish(spireWithPile(EdificeSide.Intact, fourDispossessed),
      reversing)
    assertEquals(walked.ready.game.campaign.dispossessed, fourDispossessed)
    assertEquals(said(FallenSpire, walked.events), Vector.empty)
```

`staged` and `conspiracy` are the Broken Horn section's values. Fallen Spire is wired into `powers`, the default walker catalog, by Step 4.

- [ ] **Step 3: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.setup.RuinedSetupRulesSuite"`
Expected: compile failure. `RuinedSetupSteps` and `FallenSpire` do not exist.

- [ ] **Step 4: Split the rule and write Fallen Spire**

In `BannerSetupRules.scala`, replace the doc and body of `abstract class RuinedSetupRule` with:

```scala
/** A ruined edifice face that adds steps at SETUP (END), in the shape of
  * E02's `MarketRule` (catalog batch 9, slice 1). It names `WhenExplored`
  * beside `SetupEnd`, so the same contribution serves an explore procedure
  * once one folds that window. It applies only while its face stands on the
  * map.
  *
  * Most faces are a [[RuinedSetupRule]]: one batch, then a line. Fallen
  * Spire returns its own steps, since its shuffles are tree nodes the
  * walker orders, never batch members (catalog batch 10, slice 6).
  */
abstract class RuinedSetupSteps extends ContributingPower:
  def edifice: EdificeId

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  protected def at(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, edifice, EdificeSide.Ruined)

  override def applicable(ctx: PowerCtx): Boolean = at(ctx.state).isDefined

  /** The steps this face adds at its window, for the face at `site`. */
  protected def steps(site: SiteId): Vector[Operation]

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx.state) match {
      case Some(site) => ops ++ steps(site)
      case None => ops
    }))
    Map(PowerWindow.SetupEnd -> effect, PowerWindow.WhenExplored -> effect)

/** A ruined face whose SETUP (END) effect is one batch, then its line. */
abstract class RuinedSetupRule extends RuinedSetupSteps:
  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation]

  /** The line this face writes after its effect, read from that step. */
  protected def note(at: SiteId): Note

  protected final def steps(site: SiteId): Vector[Operation] =
    Vector(BuildOps((ready, _) => Right(build(ready, site))), note(site))
```

In `RuinedSiteSetupRules.scala`, import `oathdigital.gameplay.powers.DispossessedDraws`, then append:

```scala
/** Fallen Spire (E11, ruined): "SETUP (END): Swap denizens in this region's
  * discard pile with the same number of random cards from the Dispossessed.
  * Then, shuffle these cards with any Visions in this pile."
  *
  * The swap counts the pile's denizens, skipping Visions. When the
  * Dispossessed holds fewer, both directions move that smaller count
  * (ruling 7), and the pile's bottom-most denizens are the ones that leave.
  * The draw is [[DispossessedDraws]]'s: the Dispossessed is shuffled, and
  * its first cards go to the pile in the same step as the pile's denizens
  * go to the Dispossessed, so no card comes straight back. Then the whole
  * pile is shuffled, which mixes the new cards with its Visions. Nobody
  * learns a card. A pile with no denizen changes nothing and writes no
  * line.
  *
  * Its line: "Swapped 2 cards between the Dispossessed and the Cradle
  * discard pile, then shuffled the pile." The WHEN EXPLORED half waits for
  * an Explore procedure. Great Spire, the intact face, is an action.
  */
case object FallenSpire extends RuinedSetupSteps:
  val edifice: EdificeId = GreatSpireCard.id
  val id: PowerId = GreatSpireCard.ruined.power.id
  private val notes = NoteSet(id)
  /** "Swapped {2} {cards} between the Dispossessed and the {Cradle discard
    * pile}, then shuffled the pile." */
  val swapped: NoteKey = notes.key("swapped", Vector(
    NotePart.Text("Swapped "), NotePart.Arg(0),
    NotePart.Plural(0, " card", " cards"),
    NotePart.Text(" between the Dispossessed and the "), NotePart.Arg(1),
    NotePart.Text(", then shuffled the pile.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def steps(site: SiteId): Vector[Operation] = Vector(Branch(
    (live, _) => live.game.current.map.regionOf(site).toVector.flatMap {
      region =>
        val denizens = live.game.current.commonCards.discard(region)
          .count { case _: DenizenId => true; case _ => false }
        val count = math.min(denizens, live.game.campaign.dispossessed.size)
        if count == 0 then Vector.empty
        else swap(region, count)
    }))

  private def swap(region: Region, count: Int): Vector[Operation] =
    val pile = SearchSource.RegionalDiscard(region)
    Vector(
      DispossessedDraws.shuffle,
      // Both reads come from the state before the step, so the leaving
      // denizens are the pile's own and never the arriving cards.
      BuildOps((ready, _) => Right(
        DispossessedDraws.toPile(ready, region, count, by = None) ++
          DispossessedDraws.bottomDenizens(ready, region, count).map(
            DispossessedDraws.intoDispossessed(_, region, by = None))),
        required = true),
      Shuffle(ShufflePile.Search(pile)),
      Note(id, _ => Some(swapped(PowerSourceRef.Card(edifice),
        NoteArg.Number(count), NoteArg.Pile(pile)))))
```

In `WalkerPowerCatalog.scala`, add `FallenSpire` after `BrokenClock` in the contributing list. Add it to the `powers.setup` import if that import names each power.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.setup.* oathdigital.gameplay.setup.*"`
Expected: PASS. The setup suites prove the split left the other ruined faces unchanged.

- [ ] **Step 6: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then `git diff src/test/resources`.
Expected:
- `unimplemented.txt` drops exactly `edifice.e11.ruined`.
- `declared-keys.txt` gains exactly the key `swapped` of `edifice.e11.ruined`.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/setup/BannerSetupRules.scala \
  src/main/scala/distantlands/gameplay/powers/setup/RuinedSiteSetupRules.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/scala/distantlands/gameplay/setup/SetupWalkDriver.scala \
  src/test/scala/distantlands/gameplay/powers/setup/RuinedSetupRulesSuite.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/powers/declared-keys.txt
git commit -m "$(cat <<'EOF'
feat(powers): Fallen Spire swaps its region's discard denizens with the Dispossessed at Setup's end

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 3 diff against this plan and the spec.

---

### Task 4: `RemoveFromMap`

**Files:**
- Create: `src/main/scala/distantlands/model/Pieces.scala`
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala:23-51` (move out), and after `object Discard` (new composite)
- Modify: `src/main/scala/distantlands/gameplay/operations/ResourceOperations.scala:19-27`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala:36-37`, the card-operation encoder and decoder
- Test: `src/test/scala/distantlands/gameplay/operations/RemoveFromMapOperationSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala:396`

**Interfaces:**
- Consumes: `Discard.returns(card, suit, favor, secrets, actingPlayer): Vector[CoreOperation]` (`private[model]`); `LockedCards(cards).reason(ready, operation)`; `OperationExecutor.execute`.
- Produces: `final case class RemoveFromMap(card: CardId, from: PositionedLocation, to: PositionedLocation, suit: Suit, favor: Int, secrets: Int, actingPlayer: PlayerId) extends CoreOperation`, with wire kind `"remove-from-map"`.

- [ ] **Step 1: Make room in `CoreOperations.scala`**

Create `src/main/scala/distantlands/model/Pieces.scala` with this header:

```scala
package oathdigital.model

/** The pieces an operation moves, and where in a location it moves them
  * from or to. Split out of `CoreOperations.scala`, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound. */
```

Then move these definitions from `CoreOperations.scala` into it, unchanged and in order:
- `sealed trait Piece` and `object Piece`
- `enum SecretSide`
- `enum StackPosition`
- `PositionedLocation`, with its doc comment

This is lines 23 to 51 at `d869897b`. Run `./sbtw compile`.
Expected: success. The package is the same, so no import changes.

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/operations/RemoveFromMapOperationSuite.scala`:

```scala
package oathdigital.gameplay.operations

import oathdigital.gameplay.powers.action.HomesteadersCard
import oathdigital.gameplay.powers.setup.GreatClockCard
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** `RemoveFromMap` (catalog batch 10, slice 6): a card leaves the map for a
  * pile that holds no tokens. Its favor goes to its suit's bank and its
  * secrets to the acting player, facedown, in the same operation, so a
  * restriction that refuses the move refuses the returns too. */
class RemoveFromMapOperationSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private val site = Table.start.ready.game.current.map.inPlay.head
  private val from = PositionedLocation(Location.Site(site))
  private val toDispossessed = PositionedLocation(Location.Dispossessed)
  private val homesteaders = HomesteadersCard.id

  /** Homesteaders at `site` with 1 favor and 2 secrets, and 3 favor in the
    * Hearth bank. */
  private val homesteadersWithTokens: ReadyGame = Table.start
    .denizen(HomesteadersCard, at = site)
    .tokens(homesteaders, favor = 1, secrets = 2)
    .bankFavor(Suit.Hearth, 3).ready

  private def homesteadersRemoved(favor: Int): RemoveFromMap =
    RemoveFromMap(homesteaders, from, toDispossessed, Suit.Hearth,
      favor = favor, secrets = 2, actingPlayer = p1)

  test("a denizen goes to the end of the Dispossessed, its favor to its " +
      "bank and its secrets to the actor facedown"):
    val done = accepted(executor.execute(homesteadersWithTokens,
      homesteadersRemoved(favor = 1)))
    assertEquals(done.game.campaign.dispossessed.lastOption,
      Some(homesteaders))
    assert(!Look(done).cardsAt(site).contains(homesteaders))
    assertEquals(Look(done).bankFavor(Suit.Hearth), 3 + 1)
    assertEquals(Look(done).faceDownSecrets(p1),
      Look(homesteadersWithTokens).faceDownSecrets(p1) + 2)

  test("a ruined edifice goes to the bottom of the edifice deck"):
    val ruined = Table.start
      .edifice(GreatClockCard, EdificeSide.Ruined, at = site).ready
    val done = accepted(executor.execute(ruined, RemoveFromMap(
      GreatClockCard.id, from, PositionedLocation(Location.Deck(
        CardDeck.Edifice), StackPosition.Bottom), Suit.Arcane, favor = 0,
      secrets = 0, actingPlayer = p1)))
    assertEquals(done.game.current.commonCards.edificeDeck.lastOption,
      Some(GreatClockCard.id))
    assert(!Look(done).cardsAt(site).contains(GreatClockCard.id))

  test("tokens that do not match the card are refused"):
    assertEquals(executor.execute(homesteadersWithTokens,
      homesteadersRemoved(favor = 0)).left.toOption,
      Some(OperationError.InvalidDescription(
        s"discard resources on ${homesteaders.value} do not match the card")))

  test("a lock refuses the whole removal, returns included"):
    // The returns are the removal's children, so the lock judges them with
    // the card's move and the card keeps its tokens.
    assert(LockedCards(Set(homesteaders))
      .reason(homesteadersWithTokens, homesteadersRemoved(favor = 1))
      .isDefined)
```

In `WalkerOperationWireSuite.scala`, after the `Discard.Relic(...)` entry (line 398), add two entries. Use the `denizen`, `edifice`, `site` and `player` values the neighbouring `Discard` entries use:

```scala
      RemoveFromMap(denizen, PositionedLocation(Location.Site(site)),
        PositionedLocation(Location.Dispossessed), Suit.Hearth, favor = 1,
        secrets = 2, actingPlayer = player),
      RemoveFromMap(edifice, PositionedLocation(Location.Site(site)),
        PositionedLocation(Location.Deck(CardDeck.Edifice),
          StackPosition.Bottom), Suit.Order, favor = 0, secrets = 0,
        actingPlayer = player),
```

- [ ] **Step 3: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.RemoveFromMapOperationSuite oathdigital.serialization.WalkerOperationWireSuite"`
Expected: compile failure. `RemoveFromMap` does not exist.

- [ ] **Step 4: Write the composite**

In `CoreOperations.scala`, after `object Discard` ends and before the doc of `Draw` ("Takes known cards from a prompted draw source"), add:

```scala
/** Takes a card off the map into a pile that holds no tokens (Pilgrimage,
  * catalog batch 10, slice 6): a denizen to the Dispossessed, or a ruined
  * edifice to the bottom of the edifice deck. Its favor goes to `suit`'s
  * bank and its secrets to `actingPlayer`, facedown, as a discard's do.
  * The returns are children of this operation with the card's move, so a
  * restriction that refuses the move, such as a lock, refuses them too,
  * and the card keeps its tokens. */
final case class RemoveFromMap(card: CardId, from: PositionedLocation,
    to: PositionedLocation, suit: Suit, favor: Int, secrets: Int,
    actingPlayer: PlayerId) extends CoreOperation:
  require(favor >= 0, "removed favor must be non-negative")
  require(secrets >= 0, "removed secrets must be non-negative")

  override val children: Vector[Operation] =
    Discard.returns(card, Some(suit), favor, secrets, actingPlayer) :+
      Move(Piece.Card(card), from, to, by = Some(actingPlayer))
```

The counted moves run before the card moves (`OperationApplication.applyFamilies`), so the card holds no tokens when it enters its pile.

In `ResourceOperations.resourceDescriptionViolations`, add after the `Discard.RuinedEdifice` case:

```scala
      case value: RemoveFromMap =>
        Some((value.card, Some(value.favor), value.secrets))
```

In `WalkerOperationCodec.scala`:
- Add `| RemoveFromMap` to the `CardOperation` union.
- In `encodeCardOperation`, after the `Discard.Relic` case, add:

```scala
      case RemoveFromMap(card, from, to, suit, favor, secrets, actingPlayer) =>
        ujson.Obj("kind" -> "remove-from-map", "card" -> encodeCardRef(card),
          "from" -> encodePositionedLocation(from),
          "to" -> encodePositionedLocation(to), "suit" -> suit.key,
          "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value)
```

- In `decodeCardOperation`, after the `"discard-relic"` case, add:

```scala
    case "remove-from-map" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodePositionedLocation(value("to"), s"$path.to")
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
    yield RemoveFromMap(card, from, to, suit, favor, secrets,
      PlayerId(value("actingPlayerId").str))
```

If the compiler names another exhaustive match over `CoreOperation`, handle `RemoveFromMap` there as that match handles `Discard.RuinedEdifice`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.operations.* oathdigital.serialization.*"`
Expected: PASS.

Run: `wc -l src/main/scala/distantlands/model/CoreOperations.scala`
Expected: under 800.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/Pieces.scala \
  src/main/scala/distantlands/model/CoreOperations.scala \
  src/main/scala/distantlands/gameplay/operations/ResourceOperations.scala \
  src/main/scala/distantlands/serialization/WalkerOperationCodec.scala \
  src/test/scala/distantlands/gameplay/operations/RemoveFromMapOperationSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(operations): RemoveFromMap takes a card off the map with its tokens returned

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

If Step 5 needed changes to another file the compiler named, add it to the commit.

---

### Task 5: Pilgrimage, docs and gates

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/Pilgrimage.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala:19-26` (remove `PilgrimageCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/powers/declared-keys.txt` (regenerated)
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`, `docs/ROADMAP.md:70`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/PilgrimageSuite.scala`

**Interfaces:**
- Consumes:
  - `DispossessedDraws` (Task 2), `RemoveFromMap` (Task 4), `Table.dispossessed` (Task 1)
  - `WhenPlayedPower`
  - `WhenPlayedHarness`: `hook`, `fire`, `question`, `resumeBy`, `replayed`, `assertJournalRebuilds`, `said`, `homeSite`, `p1AtHomeSite`
  - `LockedCards`, which holds through the harness's `OperationRestrictions.forCatalog(catalog)`
- Produces:
  - `object PilgrimageCard extends Denizen(DenizenId("161"), "Pilgrimage", Suit.Nomad) with AdviserOnly`, in package `oathdigital.gameplay.powers.whenplayed`, text unchanged.
  - `case object Pilgrimage extends WhenPlayedPower`, with `val inspectDecisionId: String = "power.pilgrimage.inspect"`, `val used: NoteKey` and `val none: NoteKey`.

A `BuildOps` that is not `required` skips an operation a restriction refuses and runs the rest. `RegionDiscard` relies on this, and so does Pilgrimage's removal step.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/PilgrimageSuite.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.WalkerDiceFixture
import oathdigital.gameplay.operations.AncientBloodlineCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.{HomesteadersCard, ScryerCard}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.economy.InitiationRiteCard
import oathdigital.gameplay.powers.setup.{ForestHornCard, GreatClockCard}
import oathdigital.gameplay.walker.{WalkerDice, WalkerOutcome}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2, p3}

/** Pilgrimage (card 161, catalog batch 10, slice 6), WHEN PLAYED: the
  * denizens at p1's site go to the Dispossessed, and as many shuffled
  * Dispossessed cards go, peeked by p1 alone, onto p1's region's discard
  * pile. A ruined edifice leaves for the edifice deck uncounted, and a card
  * a lock keeps stays (ruling 9). */
class PilgrimageSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = Pilgrimage
  private val played = hook(Pilgrimage.cardId)
  private val keepingOrder: WalkerDice =
    WalkerDiceFixture.shufflingBy(count => (0 until count).toVector)
  private val region: Region =
    p1AtHomeSite.ready.game.current.map.regionOf(homeSite).get
  private val homesteaders = HomesteadersCard.id
  private val wrestlers = WrestlersCard.id

  /** p1 holds Pilgrimage faceup at `homeSite`, and the Dispossessed holds
    * Dazzle, Scryer and Bewitch, Dazzle at its head. */
  private val pilgrimageHeld: Table = p1AtHomeSite.adviser(p1, PilgrimageCard)
    .dispossessed(DazzleCard, ScryerCard, BewitchCard)

  /** `pilgrimageHeld` with Homesteaders (Hearth, 1 favor) and Wrestlers (2
    * secrets) at p1's site, and 3 favor in the Hearth bank. */
  private val twoDenizensAtSite: ReadyGame = pilgrimageHeld
    .denizen(homesteaders, at = homeSite).tokens(homesteaders, favor = 1)
    .denizen(wrestlers, at = homeSite).tokens(wrestlers, secrets = 2)
    .bankFavor(Suit.Hearth, 3).ready

  /** The walk parked on the Inspect, and the state its recorded steps
    * reach. */
  private def parkedOnInspect(ready: ReadyGame,
      dice: WalkerDice = keepingOrder): (WalkerOutcome.Parked, ReadyGame) =
    val first = Outcomes.parked(fire(ready, power, played, dice))
    (first, replayed(ready, first.events))

  private def sentLine(count: Int, noun: String): Vector[NoteText.Said] =
    Vector(NoteText.Said(NoteKey.Used, s"Sent $count $noun on Pilgrimage " +
      s"and drew $count from the Dispossessed.", covers = true))

  test("the denizens at p1's site go to the Dispossessed with their tokens " +
      "returned, and as many cards are drawn onto the region's pile"):
    val (first, mid) = parkedOnInspect(twoDenizensAtSite)
    assertEquals(Look(mid).cardsAt(homeSite), Vector.empty)
    assertEquals(mid.game.campaign.dispossessed,
      Vector[WorldCardId](BewitchCard.id, homesteaders, wrestlers))
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id, ScryerCard.id))
    assertEquals(Look(mid).bankFavor(Suit.Hearth), 3 + 1)
    assertEquals(Look(mid).faceDownSecrets(p1),
      Look(twoDenizensAtSite).faceDownSecrets(p1) + 2)
    assertEquals(said(power, first.events), sentLine(2, "denizens"))

  test("only p1 learns the drawn cards, which the Inspect shows top " +
      "first, and the journal rebuilds the end"):
    val (first, mid) = parkedOnInspect(twoDenizensAtSite)
    val drawn = Vector[WorldCardId](ScryerCard.id, DazzleCard.id)
    val decide = question(mid, power, played, first.tree)
    assertEquals(decide.decisionId, Pilgrimage.inspectDecisionId)
    assertEquals(decide.query, DecisionQuery.Inspect(drawn,
      heading = Some("Pilgrimage: the cards you drew")))
    drawn.foreach(card => assert(Look(mid).knowsAdviser(p1, card)))
    Vector(p2, p3).foreach(other => drawn.foreach(card =>
      assert(!Look(mid).knowsAdviser(other, card), s"$other $card")))
    val done = Outcomes.finished(resumeBy(mid, power, played, first.tree,
      Pilgrimage.inspectDecisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionQuery.Inspect.Done), p1,
      keepingOrder))
    assertJournalRebuilds(twoDenizensAtSite, first.events ++ done.events,
      done.treeless)
    assertWireRoundTrips(first.events ++ done.events)

  test("the shuffle includes the denizens just moved, so they can come " +
      "back"):
    val (_, mid) = parkedOnInspect(twoDenizensAtSite,
      WalkerDiceFixture.reversingShuffle)
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](wrestlers, homesteaders))
    assertEquals(mid.game.campaign.dispossessed, Vector[WorldCardId](
      BewitchCard.id, ScryerCard.id, DazzleCard.id))

  test("a ruined edifice goes to the bottom of the edifice deck with its " +
      "favor returned, uncounted; an intact edifice stays"):
    val ready = pilgrimageHeld.denizen(homesteaders, at = homeSite)
      .edifice(GreatClockCard, EdificeSide.Ruined, at = homeSite)
      .tokens(GreatClockCard.id, favor = 1)
      .edifice(ForestHornCard, EdificeSide.Intact, at = homeSite)
      .bankFavor(Suit.Arcane, 2).ready
    val (first, mid) = parkedOnInspect(ready)
    assertEquals(Look(mid).cardsAt(homeSite),
      Vector[CardId](ForestHornCard.id))
    assertEquals(mid.game.current.commonCards.edificeDeck.lastOption,
      Some(GreatClockCard.id))
    assertEquals(Look(mid).bankFavor(Suit.Arcane), 2 + 1)
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id))
    assertEquals(said(power, first.events), sentLine(1, "denizen"))

  test("a lock-icon denizen stays at the site with its tokens"):
    val ready = pilgrimageHeld
      .denizen(InitiationRiteCard, at = homeSite)
      .tokens(InitiationRiteCard.id, favor = 1)
      .denizen(homesteaders, at = homeSite).ready
    val (_, mid) = parkedOnInspect(ready)
    assertEquals(Look(mid).cardsAt(homeSite),
      Vector[CardId](InitiationRiteCard.id))
    assertEquals(Look(mid).tokensOn(InitiationRiteCard.id), Tokens(1, 0))
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id))

  test("a denizen Ancient Bloodline locks for p1 stays, nothing is " +
      "shuffled or drawn, and the line says none went"):
    // p2 rules p1's site and holds the Bloodline; the reversing source
    // would show any shuffle.
    val ready = pilgrimageHeld.warbandsAt(homeSite, p2, 1)
      .adviser(p2, AncientBloodlineCard.id)
      .denizen(homesteaders, at = homeSite).tokens(homesteaders, favor = 1)
      .ready
    val done = Outcomes.finished(fire(ready, power, played,
      WalkerDiceFixture.reversingShuffle))
    assertEquals(Look(done.treeless).cardsAt(homeSite),
      Vector[CardId](homesteaders))
    assertEquals(Look(done.treeless).tokensOn(homesteaders), Tokens(1, 0))
    assertEquals(done.treeless.game.campaign.dispossessed,
      Vector[WorldCardId](DazzleCard.id, ScryerCard.id, BewitchCard.id))
    assertEquals(said(power, done.events), Vector(NoteText.Said("used.none",
      "Sent no denizens on Pilgrimage.", covers = false)))
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.PilgrimageSuite"`
Expected: compile failure. `Pilgrimage` does not exist.

- [ ] **Step 3: Write the power**

Cut `object PilgrimageCard` from `HoldingNomadDenizens.scala`. If an import there is now unused, remove it.

Create `src/main/scala/distantlands/gameplay/powers/whenplayed/Pilgrimage.scala`. Paste `PilgrimageCard` under the imports, unchanged, then add the power:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.DispossessedDraws
import oathdigital.model._

// PilgrimageCard goes here, unchanged.

/** Pilgrimage (card 161), WHEN PLAYED: "move all denizens at your site to
  * the Dispossessed. Shuffle and draw denizens from the Dispossessed equal
  * to the number you moved. Peek at them and put them on your region's
  * discard pile." (catalog batch 10, slice 6).
  *
  * Each card at the user's site leaves in one `RemoveFromMap`, its favor
  * to its bank and its secrets to the user. A denizen goes to the
  * Dispossessed. A ruined edifice goes to the bottom of the edifice deck
  * and is not counted (ruling 9). A card a lock keeps stays with its
  * tokens: an intact edifice, a lock-icon denizen, or one Ancient Bloodline
  * locks for the user. The removal step skips what a restriction refuses.
  *
  * The count is the denizens that reached the Dispossessed. Then the draw
  * is [[DispossessedDraws]]'s: the shuffle includes the denizens just
  * moved, so they can come back. The cards go on top of the region's
  * discard pile, and the user peeks at each, so only the user's knowledge
  * records it. An `Inspect` shows them top first, as Scryer's does.
  *
  * The line covers the peeks, so no generic line names a card: "Sent 2
  * denizens on Pilgrimage and drew 2 from the Dispossessed." With none
  * sent, nothing is shuffled or drawn: "Sent no denizens on Pilgrimage."
  */
case object Pilgrimage extends WhenPlayedPower:
  val id: PowerId = PilgrimageCard.power.id
  val cardId: DenizenId = PilgrimageCard.id
  val inspectDecisionId: String = "power.pilgrimage.inspect"

  private val notes = NoteSet(id)
  /** "Sent {2} {denizens} on Pilgrimage and drew {2} from the
    * Dispossessed." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Text("Sent "),
    NotePart.Arg(0), NotePart.Plural(0, " denizen", " denizens"),
    NotePart.Text(" on Pilgrimage and drew "), NotePart.Arg(1),
    NotePart.Text(" from the Dispossessed.")))
  /** "Sent no denizens on Pilgrimage." */
  val none: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Sent no denizens on Pilgrimage.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def effect(ctx: PowerCtx): Vector[Operation] = Vector(Branch((live, _) =>
    (for
      site <- live.game.current.pawnSite(ctx.actor)
      region <- live.game.current.map.regionOf(site)
    yield
      val pilgrims = denizensAt(live, site)
      Vector[Operation](
        BuildOps((ready, _) => removals(ctx.catalog, ready, site, ctx.actor)),
        Branch((moved, _) => draw(moved, pilgrims, region, ctx.actor)))
    ).toVector.flatten))

  private def denizensAt(ready: ReadyGame, site: SiteId): Vector[DenizenId] =
    ready.game.current.map.sites.get(site).toVector.flatMap(_.denizens
      .collect { case denizen: DenizenState => denizen.id })

  /** One removal per card that may leave: every denizen, and a ruined
    * edifice. An intact edifice is locked, so none is built for it. */
  private def removals(catalog: ExecutableCatalog, ready: ReadyGame,
      site: SiteId, actor: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    val from = PositionedLocation(Location.Site(site))
    ready.game.current.map.sites.get(site).toVector.flatMap(_.denizens)
      .foldLeft[Either[OathViolation, Vector[CoreOperation]]](
        Right(Vector.empty)) { (done, card) =>
        done.flatMap(ops => card match
          case denizen: DenizenState => catalog.suitOf(denizen.id)
            .toRight(OathViolation.UnknownWorldCard(denizen.id))
            .map(suit => ops :+ RemoveFromMap(denizen.id, from,
              PositionedLocation(Location.Dispossessed), suit,
              denizen.tokens.favor, denizen.tokens.secrets, actor))
          case edifice: EdificeState if edifice.side == EdificeSide.Ruined =>
            catalog.suitOf(edifice.id)
              .toRight(OathViolation.UnknownEdifice(edifice.id))
              .map(suit => ops :+ RemoveFromMap(edifice.id, from,
                PositionedLocation(Location.Deck(CardDeck.Edifice),
                  StackPosition.Bottom), suit, edifice.tokens.favor,
                edifice.tokens.secrets, actor))
          case _ => Right(ops))
      }

  private def draw(ready: ReadyGame, pilgrims: Vector[DenizenId],
      region: Region, actor: PlayerId): Vector[Operation] =
    val sent = pilgrims.count(ready.game.campaign.dispossessed.contains)
    val card = PowerSourceRef.Card(cardId)
    if sent == 0 then Vector(Note(id, _ => Some(none(card))))
    else Vector(
      DispossessedDraws.shuffle,
      BuildOps((now, _) => Right(DispossessedDraws.toPile(now, region, sent,
        Some(actor))), required = true),
      BuildOps((now, _) => Right(drawnTopFirst(now, region, sent).map(drawn =>
        Peek(actor, drawn, Location.RegionalDiscard(region))))),
      Note(id, _ => Some(used(card, NoteArg.Number(sent),
        NoteArg.Number(sent))), covers = true),
      Branch((now, _) => Vector(Decide(inspectDecisionId, actor,
        DecisionQuery.Inspect(drawnTopFirst(now, region, sent),
          heading = Some("Pilgrimage: the cards you drew"))))))

  /** The `count` cards on top of `region`'s pile, top first. A pile is
    * stored top last. */
  private def drawnTopFirst(ready: ReadyGame, region: Region, count: Int)
      : Vector[CardId] =
    ready.game.current.commonCards.discard(region).takeRight(count).reverse
```

In `WhenPlayedPowers.all`, add `Pilgrimage` after `RoyalAmbitions`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`, then `git diff src/test/resources`.
Expected:
- `unimplemented.txt` drops exactly `denizen.pilgrimage`.
- `declared-keys.txt` gains exactly the keys `used` and `used.none` of `denizen.pilgrimage`.

- [ ] **Step 6: Update the spec and the roadmap**

In the spec:
- Change the status line to "**Status:** designed 2026-11-03. Slices 1 to 6 landed."
- In "Readings", after the Pilgrimage bullet, add:

```markdown
- **Fallen Spire with a short Dispossessed** moves ruling 7's smaller
  count, and the pile's bottom-most denizens are the ones that leave. With
  no denizen in the pile, nothing moves, nothing is shuffled, and no line
  is written.
- **Pilgrimage and locks:** ruling 9 keeps intact edifices because locked
  cards cannot be moved, so every card a lock keeps stays with its tokens
  and is not counted: a lock-icon denizen, or one Ancient Bloodline locks
  for the user.
```

- Replace the "Slice 6: Dispossessed draws" body above "Tests:" with:

```markdown
`ShufflePile.Dispossessed`, whose cards are `campaign.dispossessed`, with a
codec kind. `DispossessedDraws` holds the shared steps: a draw shuffles the
pile and takes from its head, as `ReliquaryDraws` does. Moves into and out
of `Location.Dispossessed` are already legal; the projection never
identifies its cards.

- **Great Spire.** A paid Action costing 1 secret. Up to 3 Dispossessed
  cards go to the top of the region's discard pile, then as many of the
  pile's bottom denizens, skipping Visions, go to the Dispossessed (ruling
  7). Nobody peeks. Line: "Swapped 3 cards between the Dispossessed and
  the Cradle discard pile."
- **Fallen Spire.** A ruined face at SETUP (END). The denizens in its
  region's discard pile swap with as many random Dispossessed cards; then
  the pile is shuffled, which mixes those cards with its Visions. A
  `Shuffle` is a tree node, so `RuinedSetupRule` splits: the new base
  `RuinedSetupSteps` adds a face's own steps, and Fallen Spire extends it.
  Line: "Swapped 2 cards between the Dispossessed and the Cradle discard
  pile, then shuffled the pile." WHEN EXPLORED stays inert.
- **Pilgrimage.** WHEN PLAYED. Each card at the user's site leaves in one
  `RemoveFromMap`, a new composite whose children are the token returns
  and the move, so a lock refuses all of them and the card keeps its
  tokens. A denizen goes to the Dispossessed. A ruined edifice goes to the
  bottom of the edifice deck, intact side up, and is not counted; an
  intact edifice stays (ruling 9). Then as many cards as denizens moved
  are drawn, `Peek`ed by the user, shown in an `Inspect`, and put on the
  region's discard pile. Line: "Sent 2 denizens on Pilgrimage and drew 2
  from the Dispossessed."
```

- In "Seams opened", change the slice 6 row's seam to `ShufflePile.Dispossessed`, `RemoveFromMap`.

In `docs/ROADMAP.md`, delete the line `- **Dispossessed shuffles:** Great Spire, Fallen Spire, Pilgrimage.` (memory: "Roadmap deletes done work").

- [ ] **Step 7: Run the gates**

Run, in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes; statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/Pilgrimage.scala \
  src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala \
  src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/powers/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/whenplayed/PilgrimageSuite.scala \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): Pilgrimage sends the denizens at your site to the Dispossessed and draws as many

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 4 and Task 5 diffs against this plan and the spec.
