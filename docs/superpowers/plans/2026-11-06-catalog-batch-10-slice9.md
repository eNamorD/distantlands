# Catalog Batch 10 Slice 9: Search Discard Destination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a discard go to the bottom of its pile, and implement the three Search modifiers that change where a Search's discards go: Ring of Devotion (R12), Cracked Horn (R10) and Bracken (197).

**Architecture:**
- `Discard.Denizen` and `Discard.Vision` gain `position: StackPosition`, defaulted to `Top` as `Draw`'s is. The move into the pile uses it. The wire field is required, and old journals need not decode (memory: "No old replay support").
- A new trait, `SearchDiscards`, holds what the three cards share. Each is a `SelectedModifier` for Search with two `Transform`s, one at `SearchActionEligibility` (the unkept draws) and one at `SearchPlayAdviser` (the card play: an adviser bumped over the limit, a site card the play replaces or the People's Favor discards, the kept card discarded). Each `Transform` rewrites the user's own `Discard.Denizen` and `Discard.Vision` with `OperationRewrite.ops`. A ruined edifice's discard is a different operation, so it still goes aside.
- `OperationRewrite` leaves alone every node below that carries a window of its own. A WHEN PLAYED power runs under the card-play hook's window, so Dazzle's discards stay as they are (Bracken FAQ: "not cards like Dazzle").
- **Ring of Devotion** sets `to` to the region of the user's pawn. **Cracked Horn** turns each discard into a bury to the bottom of the world deck. **Bracken** asks a pile and Top or Bottom once, at its pick (`AsksOnSelection`), and sends every discard there.
- The trait makes the other two unavailable once one is picked.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Catalog corrections" (the three cards carry the Search icon; Ring of Devotion's id typo)
- "Rulings", ruling 2
- "Readings" (the last bullet: the three are exclusive)
- "Slice 9: Search discard destination"
- "Testing"

Card text:
- Bracken (`HoldingBeastDenizens.scala`), Beast, 1 secret: "You may put all of the cards you discard on the top or bottom of any one discard pile _(even your region's)_."
- Cracked Horn (`HoldingRelics.scala`), free: "You may put all of the cards you discard on the bottom of the world deck."
- Ring of Devotion (`HoldingRelics.scala`), free: "You may discard cards to your region's discard pile instead of the next region out."

FAQ (`~/projects/cards/content/faq/oath/en-US.yml`):
- Bracken: "It includes any cards discarded during the Search action, so it includes the People's Favor, but not cards like Dazzle."
- Bury: "Can I bury Visions? What does that do to the Visions Drawn track?" "Yes you can, and it does not do anything to the Visions Drawn track."

**Plan-time findings** (each goes into the spec in Task 4):
- **Spinning Bee needs no rewrite.** The spec names "Spinning Bee's window" as a third place to rewrite, for Bee's no-match discard. Bee reads "You cannot use other Search modifiers", and the engine enforces it both ways: once Bee is picked every other modifier is unavailable, and Bee is unavailable once another is picked. So a discard modifier and Bee's discard never meet in one Search.
- **`position` has a default.** The spec says every existing builder passes Top. About 20 builders in `src/main` and 24 in `src/test` would change for no behaviour, so `position` defaults to `Top`, as `Draw.position` does. The wire field is required, as the spec asks.
- **Lines.** The generic discard lines already name the pile a card went to, so Ring of Devotion and Bracken's pile need no line of their own. A buried card already reads "Buried {card}". Bracken writes one line at its pick that names the position too: "This Search's discards go to the bottom of the Hinterland discard pile." This is the spec's "once per Search" line. The generic lines are not changed to say "bottom".
- **Search Party (slice 12).** Its nested card play will fold `SearchPlayAdviser` with the outer Search's answers, so these modifiers would apply to it. Ruling 5 says they must not. Task 4 records this in the spec's slice 12 section so that slice keeps them out.

This is the ninth of 14 slice plans, written against `0.2.0-alpha` at `0b419ab6`, before slice 8 merged. Slice 8 touches the Recover only. Its doc edits sit on other lines of the same two files: the spec's status line and slice 8 section, and the ROADMAP's "Recover source" bullet.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-9` on branch `batch-10-slice-9`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - Before Task 1, if `batch-10-slice-8` has merged into local `0.2.0-alpha`, rebase this branch onto `0.2.0-alpha` (`git rebase 0.2.0-alpha`).
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
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains. The files are `src/test/resources/notes/declared-keys.txt` and `src/test/resources/powers/unimplemented.txt`.
- **Log lines:** a line names only non-actors (memory: "Log lines omit actor"). Pile names read "the Cradle discard pile" (spec, "Readings").
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").
- **Modifier icon:** each card's printed power gains `modifier = Some(ModifierMark.Action(MajorActionType.Search))` (spec, "Catalog corrections").
- **Exclusivity reason:** "Another power already sets where this Search's discards go." (spec, "Readings").

---

### Task 1: A discard's pile position

**Files:**
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (`Discard.Denizen`, `Discard.Vision`, `discardWorld`; about lines 124 to 190)
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` (encode at about line 90, decode at about line 342)
- Test: `src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces:
  - `Discard.Denizen(card, from, to, suit, favor, secrets, actingPlayer, required = false, actorless = false, position: StackPosition = StackPosition.Top)`
  - `Discard.Vision(card, from, to, required = false, position: StackPosition = StackPosition.Top)`
  - Both refuse `StackPosition.Unspecified` with `require`.

- [ ] **Step 1: Write the failing tests**

In `CoreOperationsSuite.scala`, add after the test "Draw is a top-first sequence of Take operations":

```scala
  test("a discard goes to the top of its pile unless it names the bottom"):
    val inHand = PositionedLocation(Location.Hand(red))
    def pileMove(discard: Discard): Option[PositionedLocation] =
      discard.children.collectFirst {
        case Move(Piece.Card(_), _, to @ PositionedLocation(
            Location.RegionalDiscard(_), _, _), _, _) => to
      }
    val card = DenizenId("denizen:discarded")
    val vision = VisionId("vision:discarded")
    assertEquals(pileMove(Discard.Denizen(card, inHand, Region.Hinterland,
      Suit.Beast, 0, 0, red)), Some(PositionedLocation(
      Location.RegionalDiscard(Region.Hinterland), StackPosition.Top)))
    assertEquals(pileMove(Discard.Denizen(card, inHand, Region.Hinterland,
      Suit.Beast, 0, 0, red, position = StackPosition.Bottom)),
      Some(PositionedLocation(Location.RegionalDiscard(Region.Hinterland),
        StackPosition.Bottom)))
    assertEquals(pileMove(Discard.Vision(vision, inHand, Region.Cradle,
      position = StackPosition.Bottom)), Some(PositionedLocation(
      Location.RegionalDiscard(Region.Cradle), StackPosition.Bottom)))
    intercept[IllegalArgumentException](Discard.Vision(vision, inHand,
      Region.Cradle, position = StackPosition.Unspecified))
    intercept[IllegalArgumentException](Discard.Denizen(card, inHand,
      Region.Cradle, Suit.Beast, 0, 0, red,
      position = StackPosition.Unspecified))
```

If `Move` has a different number of fields than the five this pattern names, match it by type instead (`case move: Move if move.to.location.isInstanceOf[Location.RegionalDiscard] => move.to`).

In `WalkerOperationWireSuite.scala`, add after the test "an actorless discard says so on the wire":

```scala
  test("a discard's pile position round trips, and a discard that names " +
      "none, or names neither end, is refused"):
    val inHand = PositionedLocation(Location.Hand(Table.p1))
    Vector[CoreOperation](
      Discard.Denizen(DenizenId("D1"), inHand, Region.Hinterland,
        Suit.Beast, 0, 0, Table.p1, position = StackPosition.Bottom),
      Discard.Vision(VisionId("vision:v1"), inHand, Region.Cradle,
        position = StackPosition.Bottom)
    ).foreach { discard =>
      val encoded = encodedStream("walker", Vector(recordedOperation(discard)))
      def withPosition(edit: ujson.Value => Unit): String =
        val stream = ujson.read(encoded).arr
        edit(stream(0)("payload")("step")("ops")(0))
        ujson.write(stream)
      assertEquals(accepted(GameEventWire.decodeStream(encoded))
        .map(_.event), Vector(recordedOperation(discard)))
      assertEquals(GameEventWire.decodeStream(withPosition(
        _.obj.remove("position"))), Left(WireError.InvalidValue(
          "$[0].payload.step.ops[0].position",
          "a discard names the top or the bottom of its pile")))
      assertEquals(GameEventWire.decodeStream(withPosition(
        _("position") = "unspecified")), Left(WireError.InvalidValue(
          "$[0].payload.step.ops[0].position",
          "a discard goes to the top or the bottom of its pile")))
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.CoreOperationsSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: compile error, `position` is not a parameter of `Discard.Denizen`.

- [ ] **Step 3: Add the field**

In `CoreOperations.scala`, change `Discard.Denizen` to:

```scala
  /** Discards a denizen facedown to the specified regional pile and returns
    * all resources on it. `actingPlayer` takes its secrets and discards it,
    * unless the discard is `actorless`: an effect no player performs, as
    * Empty Grounds'. Its secrets still go to `actingPlayer`, the active
    * player, since secrets reach a card only on a player's turn.
    * `position` is the end of the pile it goes to: the top, unless a power
    * says otherwise (Bracken, catalog batch 10, slice 9).
    */
  final case class Denizen(card: DenizenId, from: PositionedLocation,
      to: Region, suit: Suit, favor: Int,
      secrets: Int, actingPlayer: PlayerId,
      override val required: Boolean = false, actorless: Boolean = false,
      position: StackPosition = StackPosition.Top)
      extends Discard:
    require(favor >= 0, "discarded favor must be non-negative")
    require(secrets >= 0, "discarded secrets must be non-negative")
    require(position != StackPosition.Unspecified,
      "a discard goes to the top or the bottom of its pile")

    /** The player who discards it, if any. */
    def by: Option[PlayerId] = Option.unless(actorless)(actingPlayer)

    override val children: Vector[Operation] =
      discardWorld(card, from, to, by, position) ++
        returnedResources(card, suit, favor, secrets, actingPlayer)

  /** Visions carry no resources but otherwise use world-card discard rules,
    * `position` included. */
  final case class Vision(card: VisionId, from: PositionedLocation,
      to: Region, override val required: Boolean = false,
      position: StackPosition = StackPosition.Top) extends Discard:
    require(position != StackPosition.Unspecified,
      "a discard goes to the top or the bottom of its pile")
    override val children: Vector[Operation] =
      discardWorld(card, from, to, None, position)
```

And `discardWorld`:

```scala
  private def discardWorld(card: WorldCardId, from: PositionedLocation,
      destination: Region, by: Option[PlayerId],
      position: StackPosition): Vector[Operation] =
    Vector(Move(Piece.Card(card), from,
      PositionedLocation(Location.RegionalDiscard(destination), position),
      resultingOrientation = Some(Orientation.FaceDown), by = by))
```

`CardMovementOperations` already inserts at either end of a discard pile (`insertStack`, `topAtHead = false`), so nothing else in the pipeline changes.

- [ ] **Step 4: Write and read the field on the wire**

In `WalkerOperationCodec.scala`, change the two encode cases:

```scala
      case Discard.Denizen(card, from, to, suit, favor, secrets, actingPlayer,
          _, actorless, position) =>
        withActorless(ujson.Obj("kind" -> "discard-denizen",
          "card" -> card.value,
          "from" -> encodePositionedLocation(from), "to" -> to.key,
          "position" -> encodeStackPosition(position),
          "suit" -> suit.key, "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value), actorless)
      case Discard.Vision(card, from, to, _, position) => ujson.Obj(
        "kind" -> "discard-vision", "card" -> card.value,
        "from" -> encodePositionedLocation(from), "to" -> to.key,
        "position" -> encodeStackPosition(position))
```

Change the two decode cases:

```scala
    case "discard-denizen" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodeRegion(value("to").str, s"$path.to")
      position <- discardPosition(value, path)
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
    yield Discard.Denizen(DenizenId(value("card").str), from, to, suit,
      favor, secrets, PlayerId(value("actingPlayerId").str),
      actorless = decodeActorless(value), position = position)
    case "discard-vision" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodeRegion(value("to").str, s"$path.to")
      position <- discardPosition(value, path)
    yield Discard.Vision(VisionId(value("card").str), from, to,
      position = position)
```

Add the helper beside the other private decode helpers in the same object:

```scala
  /** A discard's end of its pile. Required: a journal written before the
    * field existed need not decode. */
  private def discardPosition(value: ujson.Value, path: String)
      : Either[WireError, StackPosition] = for
    found <- value.obj.get("position").toRight(InvalidValue(
      s"$path.position", "a discard names the top or the bottom of its pile"))
    position <- decodeStackPosition(found.str, s"$path.position")
    _ <- Either.cond(position != StackPosition.Unspecified, (),
      InvalidValue(s"$path.position",
        "a discard goes to the top or the bottom of its pile"))
  yield position
```

If `InvalidValue` is not imported unqualified in that file, write `WireError.InvalidValue`, as the file's other refusals do.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.CoreOperationsSuite distantlands.serialization.*"`
Expected: PASS, including the existing test "every CoreOperation variant round-trips through the walker codec".

- [ ] **Step 6: Run the whole suite**

Run: `./sbtw test`
Expected: PASS. Every builder relies on the default, so no behaviour changes. The golden logs do not change, since they are text and not wire.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala \
  src/main/scala/distantlands/serialization/WalkerOperationCodec.scala \
  src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "$(cat <<'EOF'
feat(model): a denizen or Vision discard names the end of its pile

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: The shared rewrite and Ring of Devotion

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/search/RingOfDevotion.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala` (remove `RingOfDevotionCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (regenerated)
- Create: `src/test/scala/distantlands/gameplay/powers/search/SearchDiscardFixture.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/search/RingOfDevotionSuite.scala`

**Interfaces:**
- Consumes: Task 1's `position` on `Discard.Denizen` and `Discard.Vision`.
- Produces:
  - `trait SearchDiscards extends SelectedModifier` with `def redirected(ready: ReadyGame, answered: Vector[Answered], actor: PlayerId)(discard: SearchDiscards.UserDiscard): Vector[CoreOperation]`
  - `object SearchDiscards` with `type UserDiscard = Discard.Denizen | Discard.Vision`, `val anotherPicked: String`, and `def cards: Set[CardId]`
  - `RingOfDevotionCard`, `RingOfDevotion` (id `relic.ring-of-devotion`)
  - `SearchDiscardFixture`, used by Tasks 3 and 4

- [ ] **Step 1: Write the fixture**

Create `SearchDiscardFixture.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.gameplay.powers.action.HomesteadersCard
import distantlands.model._
import distantlands.testkit.Table

/** The Search discards that Bracken, Cracked Horn and Ring of Devotion
  * redirect (catalog batch 10, slice 9, ruling 2): the cards not kept, an
  * adviser bumped over the limit, and a site card the People's Favor
  * discards. p1 searches the world deck at Ancient City, in the Cradle, so
  * a discard goes to the Provinces pile unless a power redirects it. */
object SearchDiscardFixture:
  import PowerFixture.actor
  import SearchFixture.{keep, place, placeReplacing}

  /** The three Discord denizens the Search draws, none with a power that
    * acts here. The first is kept. */
  val drawn: Vector[DenizenId] = SearchFixture.discordCards.take(3)
  val kept: DenizenId = drawn.head
  val unkept: Vector[DenizenId] = drawn.tail
  /** Three more, which fill p1's adviser area facedown. */
  val heldAdvisers: Vector[DenizenId] = SearchFixture.discordCards.slice(3, 6)
  /** The adviser a faceup play bumps. */
  val bumped: DenizenId = heldAdvisers.head
  /** The card at Ancient City the People's Favor lets p1 discard. */
  val siteCard: DenizenId = HomesteadersCard.id

  /** p1 at Ancient City with 5 Supply, the world deck topped by `top`, and
    * whatever `arrange` adds. */
  def searchAtAncientCity(arrange: Table => Table = identity,
      top: Vector[WorldCardId] = drawn): ReadyGame =
    arrange(Table.from(SearchFixture.staged(top, supply = 5))).ready

  /** `table` with p1's adviser area full of three facedown cards. */
  def withThreeFacedownAdvisers(table: Table): Table =
    heldAdvisers.foldLeft(table)((done, card) =>
      done.adviser(actor, card, facedown = true))

  /** `table` with Homesteaders at Ancient City and p1 holding the People's
    * Favor, Mob face up. */
  def withPeoplesFavorAndSiteCard(table: Table): Table =
    table.denizen(siteCard, at = AncientCitySite)
      .peoplesFavor(Some(actor), 1)

  /** `searched`, parked on the keep question, finished: `card` kept and
    * placed by `play`. The events are the whole Search's. */
  private def finished(searched: OathTransition, card: WorldCardId)(
      play: OathTransition => Either[OathViolation, OathTransition])
      : OathTransition = (for
    chosen <- keep(searched, card)
    placed <- play(chosen)
  yield placed.copy(events = searched.events ++ chosen.events ++
    placed.events)).fold(error => throw new AssertionError(error.toString),
    identity)

  /** The kept card played facedown, so only the unkept draws are
    * discarded. */
  def keptFacedown(searched: OathTransition)(using munit.Location)
      : OathTransition =
    finished(searched, kept)(place(_, kept, PlayedAs.Facedown))

  /** The kept card played faceup in `bumped`'s place. */
  def keptFaceupBumpingAnAdviser(searched: OathTransition)(
      using munit.Location): OathTransition =
    finished(searched, kept)(placeReplacing(_, kept, PlayedAs.Faceup,
      bumped))

  /** The kept card played to Ancient City in `siteCard`'s place. */
  def keptAtSiteReplacingTheSiteCard(searched: OathTransition)(
      using munit.Location): OathTransition =
    finished(searched, kept)(placeReplacing(_, kept, PlayedAs.AtSite,
      siteCard))

  /** `card` kept and played to Ancient City. */
  def keptAtSite(searched: OathTransition, card: WorldCardId)(
      using munit.Location): OathTransition =
    finished(searched, card)(place(_, card, PlayedAs.AtSite))
```

If `Table.from(ready)` or `Table.adviser(..., facedown = ...)` has another name, use the one `PeoplesFavorMobSuite` and `WildCrySuite` use.

- [ ] **Step 2: Write the failing tests**

Create `RingOfDevotionSuite.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.gameplay.powers.whenplayed.DazzleCard
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Unwrap.accepted

/** Ring of Devotion (relic R12), a free selected Search modifier: "You may
  * discard cards to your region's discard pile instead of the next region
  * out." (catalog batch 10, slice 9). */
class RingOfDevotionSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._

  private val picked = Vector(RingOfDevotion.id)

  private def holdingTheRing(table: Table): Table =
    table.relic(actor, RingOfDevotionCard)

  private def searched(ready: ReadyGame,
      modifiers: Vector[PowerId] = picked): OathTransition =
    accepted(SearchFixture.start(ready, modifiers))

  private def cradle(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Cradle)
  private def provinces(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Provinces)

  test("its id is spelled right and it carries the Search icon"):
    assertEquals(RingOfDevotionCard.power.id,
      PowerId("relic.ring-of-devotion"))
    assertEquals(RingOfDevotionCard.power.modifier,
      Some(distantlands.catalog.ModifierMark.Action(MajorActionType.Search)))

  test("unpicked, the cards not kept go to the next region out"):
    val done = keptFacedown(searched(
      searchAtAncientCity(holdingTheRing), Vector.empty))
    assertEquals(provinces(done).takeRight(2), unkept)

  test("picked, the cards not kept go to the Cradle pile, the pawn's own " +
      "region"):
    val done = keptFacedown(searched(searchAtAncientCity(holdingTheRing)))
    assertEquals(cradle(done).takeRight(2), unkept)
    assert(!provinces(done).exists(unkept.contains), provinces(done))

  test("picked, an adviser bumped over the limit goes to the Cradle pile"):
    val done = keptFaceupBumpingAnAdviser(searched(searchAtAncientCity(
      table => withThreeFacedownAdvisers(holdingTheRing(table)))))
    assertEquals(cradle(done).last, bumped)
    assert(!provinces(done).contains(bumped), provinces(done))

  test("picked, the People's Favor's site discard goes to the Cradle pile"):
    val done = keptAtSiteReplacingTheSiteCard(searched(searchAtAncientCity(
      table => withPeoplesFavorAndSiteCard(holdingTheRing(table)))))
    assertEquals(cradle(done).last, siteCard)
    assert(!provinces(done).contains(siteCard), provinces(done))

  test("picked, Dazzle's own discard still goes to the next region out"):
    // Dazzle is kept and played to Ancient City. Its WHEN PLAYED discards
    // the Hearth card there, which is not a discard of the Search.
    val dazzle = DazzleCard.id
    val ready = searchAtAncientCity(
      table => holdingTheRing(table).denizen(siteCard, at = AncientCitySite),
      top = dazzle +: unkept)
    val done = keptAtSite(searched(ready), dazzle)
    assertEquals(provinces(done).last, siteCard)
    assertEquals(cradle(done).takeRight(2), unkept)
```

Notes for the implementer:
- `Look.discardPile` lists a pile bottom first, so its last card is the top.
- If the Dazzle test fails because the Hearth card went to the Cradle, the rewrite reached a WHEN PLAYED power's discard. Stop and report: the card-play hook's window does not shield its subtree as this plan assumes.
- If `searchAtAncientCity` already places a Hearth or Order card at a Cradle site, Dazzle discards it too. The test reads only the pile's last card and the unkept pair, so a third discard does not break it unless it lands last. If it does, assert with `contains` instead.

- [ ] **Step 3: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.RingOfDevotionSuite"`
Expected: compile error, `RingOfDevotion` not found.

- [ ] **Step 4: Write the shared trait**

Create `SearchDiscards.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.powerresolver.{Contribution, ModifierPicks,
  OfferReason, OperationRewrite, PowerCtx, Transform}
import distantlands.gameplay.powers.SelectedModifier
import distantlands.model._

/** What Bracken, Cracked Horn and Ring of Devotion share (catalog batch 10,
  * slice 9): a selected Search modifier that changes where the user's own
  * discards go for the rest of that Search.
  *
  * A pick covers every card the user discards during the Search (ruling
  * 2): the cards not kept, at `SearchActionEligibility`, and every discard
  * of the card play, at `SearchPlayAdviser`: an adviser bumped over the
  * limit, a Vision displaced, a site card the play replaces or the People's
  * Favor discards, and the kept card itself when it is discarded. Each
  * window's `Transform` rewrites the user's `Discard.Denizen` and every
  * `Discard.Vision` with `OperationRewrite.ops`. A ruined edifice's discard
  * is another operation, so it is still set aside.
  *
  * Another power's discard is untouched (Bracken FAQ: "not cards like
  * Dazzle"): a WHEN PLAYED power runs under the card-play hook's own
  * window, which the rewrite does not enter. Spinning Bee's no-match
  * discard is never reached either, since Bee and these modifiers exclude
  * each other.
  *
  * Once one of the three is picked, the other two are unavailable.
  */
trait SearchDiscards extends SelectedModifier:
  /** `discard`, made by `actor` during the Search, as this power sends
    * it, read at the moment it is matched: the state, and the walk's
    * answers so far. */
  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation]

  final def actions: Set[MajorActionType] = Set(MajorActionType.Search)

  final def effects: Map[PowerWindow, Vector[Contribution]] =
    SearchDiscards.windows.map(_ -> Vector(Transform(rewritten))).toMap

  final override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    Option.when(ModifierPicks.cards(ctx.answered).exists(card =>
      card != cardId && SearchDiscards.cards(card)))(
      OfferReason.Unavailable(SearchDiscards.anotherPicked))

  private def rewritten(ctx: PowerCtx, children: Vector[Operation])
      : Vector[Operation] =
    OperationRewrite.ops(ctx, children)((ready, pending) => {
      case discard: Discard.Denizen if discard.by.contains(ctx.actor) =>
        redirected(ready, pending.answered, ctx.actor)(discard)
      case discard: Discard.Vision =>
        redirected(ready, pending.answered, ctx.actor)(discard)
    })

object SearchDiscards:
  /** A discard the user makes during the Search. A Vision discard names no
    * player, and every one the two windows hold is the user's. */
  type UserDiscard = Discard.Denizen | Discard.Vision

  /** Why the other two are unavailable once one is picked (spec,
    * "Readings"). */
  val anotherPicked: String =
    "Another power already sets where this Search's discards go."

  /** The windows whose discards are the Search's own. */
  private val windows: Vector[PowerWindow] = Vector(
    PowerWindow.SearchActionEligibility, PowerWindow.SearchPlayAdviser)

  /** The three cards' ids. Task 3 and Task 4 add theirs. */
  def cards: Set[CardId] = Set(RingOfDevotionCard.id)
```

The trait and the object share a name in one file, so they are companions, and the trait may read the object's private `windows`.

`RingOfDevotion.redirected` does not read `answered`, and Task 3's `CrackedHorn.redirected` reads neither `ready`, `answered` nor `actor`. Scala does not warn about an unused parameter of a method that implements an abstract one. If `-Werror` refuses one anyway, name it with a leading underscore.

- [ ] **Step 5: Write Ring of Devotion**

Remove `RingOfDevotionCard` from `HoldingRelics.scala`. Create `RingOfDevotion.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.gameplay.actions.search.SearchProcedure
import distantlands.model._

object RingOfDevotionCard extends Relic(RelicId("R12"), "Ring of Devotion", value = 90, defense = 1):
  val power = PrintedPower(PowerId("relic.ring-of-devotion"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may discard cards to your region's discard pile instead " +
      "of the next region out.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ring of Devotion (relic R12), a free selected Search modifier: "You may
  * discard cards to your region's discard pile instead of the next region
  * out." (catalog batch 10, slice 9).
  *
  * Every discard of the Search goes to the pile of the region the user's
  * pawn stands in ([[SearchDiscards]]). The pawn does not move during a
  * Search. Its id was misspelled `relic.king-of-devotion` in the card data;
  * old journals need not decode. The generic discard lines name the pile,
  * so it writes no line.
  */
case object RingOfDevotion extends SearchDiscards:
  val id: PowerId = RingOfDevotionCard.power.id
  val card: Relic = RingOfDevotionCard

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] =
    SearchProcedure.actorRegion(ready, actor).toOption.fold(
        Vector[CoreOperation](discard)) { region =>
      discard match
        case denizen: Discard.Denizen => Vector(denizen.copy(to = region))
        case vision: Discard.Vision => Vector(vision.copy(to = region))
    }
```

In `ActionModifiers.scala`, add `RingOfDevotion` to the `powers.search` import (alphabetical) and to `all` after `SpinningBee`:

```scala
      WineOfWelcome, PaintedTrumpet, SpinningBee, RingOfDevotion,
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.*"`
Expected: PASS, the existing Search suites included.

- [ ] **Step 7: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected: `unimplemented.txt` loses `relic.king-of-devotion`, and nothing else. `declared-keys.txt` is unchanged.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala \
  src/main/scala/distantlands/gameplay/powers/search/RingOfDevotion.scala \
  src/main/scala/distantlands/catalog/holding/HoldingRelics.scala \
  src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/powers/search/SearchDiscardFixture.scala \
  src/test/scala/distantlands/gameplay/powers/search/RingOfDevotionSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Ring of Devotion keeps a Search's discards in the pawn's region

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 1 and Task 2 diffs against this plan and the spec.

---

### Task 3: Cracked Horn

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/search/CrackedHorn.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala` (`cards`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala` (remove `CrackedHornCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (regenerated)
- Test: `src/test/scala/distantlands/gameplay/powers/search/CrackedHornSuite.scala`

**Interfaces:**
- Consumes: `SearchDiscards`, `SearchDiscardFixture` (Task 2).
- Produces: `CrackedHornCard`, `CrackedHorn` (id `relic.cracked-horn`).

- [ ] **Step 1: Write the failing tests**

Create `CrackedHornSuite.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Unwrap.accepted

/** Cracked Horn (relic R10), a free selected Search modifier: "You may put
  * all of the cards you discard on the bottom of the world deck." (catalog
  * batch 10, slice 9). */
class CrackedHornSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._

  private val picked = Vector(CrackedHorn.id)

  private def holdingTheHorn(table: Table): Table =
    table.relic(actor, CrackedHornCard)

  private def searched(ready: ReadyGame): OathTransition =
    accepted(SearchFixture.start(ready, picked))

  /** The world deck, top first, after `transition`. */
  private def worldDeck(transition: OathTransition): Vector[WorldCardId] =
    SearchFixture.after(transition).game.current.commonCards.worldDeck
  private def provinces(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Provinces)

  test("it carries the Search icon"):
    assertEquals(CrackedHornCard.power.modifier,
      Some(distantlands.catalog.ModifierMark.Action(MajorActionType.Search)))

  test("picked, the cards not kept go to the bottom of the world deck, in " +
      "the order discarded"):
    val done = keptFacedown(searched(searchAtAncientCity(holdingTheHorn)))
    assertEquals(worldDeck(done).takeRight(2), unkept)
    assert(!provinces(done).exists(unkept.contains), provinces(done))

  test("picked, an adviser bumped over the limit goes to the bottom of the " +
      "world deck"):
    val done = keptFaceupBumpingAnAdviser(searched(searchAtAncientCity(
      table => withThreeFacedownAdvisers(holdingTheHorn(table)))))
    assertEquals(worldDeck(done).last, bumped)

  test("picked, the People's Favor's site discard goes to the bottom of " +
      "the world deck"):
    val done = keptAtSiteReplacingTheSiteCard(searched(searchAtAncientCity(
      table => withPeoplesFavorAndSiteCard(holdingTheHorn(table)))))
    assertEquals(worldDeck(done).last, siteCard)

  test("a Vision it buries leaves the Visions Drawn track as the draw left " +
      "it"):
    val vision = VisionRules.Faith
    val ready = searchAtAncientCity(holdingTheHorn,
      top = Vector[WorldCardId](vision, kept, unkept.head))
    val before = ready.game.current.tracks.visionsDrawn
    val done = keptFacedown(searched(ready))
    assertEquals(worldDeck(done).takeRight(2),
      Vector[WorldCardId](vision, unkept.head))
    // The draw advanced the track once; the bury does nothing to it.
    assertEquals(SearchFixture.after(done).game.current.tracks.visionsDrawn,
      before + 1)
```

Note for the implementer: the temporary hand holds the cards in draw order, and `SearchFixture.keep` lists the discards in that order, so the Vision is buried first and lies above `unkept.head`. If the Search draws more than three cards from this deck, check `SearchRules.draw`; the fixture's deck top must be the whole draw.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.CrackedHornSuite"`
Expected: compile error, `CrackedHorn` not found.

- [ ] **Step 3: Write Cracked Horn**

Remove `CrackedHornCard` from `HoldingRelics.scala`. If no relic is left there that uses `Cost`, `PowerId` or another import, trim the import so `-Werror` passes; Grand Mask, Bandit Crown and others still use them.

Create `CrackedHorn.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.model._

object CrackedHornCard extends Relic(RelicId("R10"), "Cracked Horn", value = 21, defense = 1):
  val power = PrintedPower(PowerId("relic.cracked-horn"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may put all of the cards you discard on the bottom of " +
      "the world deck.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Cracked Horn (relic R10), a free selected Search modifier: "You may put
  * all of the cards you discard on the bottom of the world deck." (catalog
  * batch 10, slice 9).
  *
  * Every discard of the Search ([[SearchDiscards]]) becomes a bury to the
  * bottom of the world deck, with the returns a discard makes: favor to the
  * card's suit bank and secrets to the user, facedown. A buried Vision
  * leaves the Visions Drawn track alone (Cracked Horn FAQ). The bury keeps
  * the discard's `required`. The generic "Buried {card}" line tells each
  * one, so it writes no line of its own.
  */
case object CrackedHorn extends SearchDiscards:
  val id: PowerId = CrackedHornCard.power.id
  val card: Relic = CrackedHornCard

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] = discard match
    case denizen: Discard.Denizen =>
      Bury.standard(BuryableCard.Denizen(denizen.card), denizen.from,
        Some(denizen.suit), denizen.favor, denizen.secrets,
        denizen.actingPlayer).map {
        case bury: Bury => bury.copy(required = denizen.required)
        case other => other
      }
    case vision: Discard.Vision =>
      Vector(Bury(BuryableCard.Vision(vision.card), vision.from,
        required = vision.required))
```

In `SearchDiscards.scala`, change `cards` and its comment:

```scala
  /** The three cards' ids. Task 4 adds Bracken's. */
  def cards: Set[CardId] = Set(CrackedHornCard.id, RingOfDevotionCard.id)
```

In `ActionModifiers.scala`, add `CrackedHorn` to the `powers.search` import (alphabetical) and to `all` after `RingOfDevotion`:

```scala
      WineOfWelcome, PaintedTrumpet, SpinningBee, RingOfDevotion,
      CrackedHorn,
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected: `unimplemented.txt` loses `relic.cracked-horn`, and nothing else.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/search/CrackedHorn.scala \
  src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala \
  src/main/scala/distantlands/catalog/holding/HoldingRelics.scala \
  src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/powers/search/CrackedHornSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Cracked Horn buries a Search's discards under the world deck

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Bracken, exclusivity, docs and gates

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/search/Bracken.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala` (`cards`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingBeastDenizens.scala` (remove `BrackenCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt` (regenerated)
- Test: `src/test/scala/distantlands/gameplay/powers/search/BrackenSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/search/SearchDiscardsSuite.scala`
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Consumes: `SearchDiscards`, `SearchDiscardFixture` (Task 2), `CrackedHorn` (Task 3).
- Produces:
  - `BrackenCard`, `Bracken` (id `denizen.bracken`)
  - `Bracken.pileDecision = "bracken.pile"`, `Bracken.positionDecision = "bracken.position"`
  - `Bracken.pileRef(region: Region): DecisionOptionRef.Button`, `Bracken.top`, `Bracken.bottom`
  - note keys `denizen.bracken` `discards-top` and `discards-bottom`

- [ ] **Step 1: Write the failing tests**

Create `BrackenSuite.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.{ModifierPlay, SearchPlay}
import distantlands.gameplay.actions.search.SearchProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Bracken (card 197), a selected Search modifier for 1 secret: "You may
  * put all of the cards you discard on the top or bottom of any one
  * discard pile (even your region's)." (catalog batch 10, slice 9). */
class BrackenSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._
  import SearchFixture.rules

  private val bracken = BrackenCard.id

  /** Bracken at Ancient City, p1's site, and p1 with 1 faceup secret to
    * pay for it. */
  private def besideBracken(table: Table): Table =
    table.denizen(bracken, at = AncientCitySite).secrets(actor, faceUp = 1)

  /** p1 picks Bracken, then a Search starts. */
  private def brackenPicked(ready: ReadyGame): OathTransition =
    accepted(ModifierPlay.started(rules, Ready(ready), ActionRef.Search,
      actor, PowerWindow.SearchModifierSelection, Vector(Bracken.id)))

  /** `picked`, with Bracken's questions answered `pile` and `end`, the
    * Search started and the world deck picked. The events are every
    * command's. */
  private def searchedSending(picked: OathTransition, pile: Region,
      end: DecisionOptionRef.Button): OathTransition = (for
    piled <- rules.resolveWalker(picked.state, actor, Bracken.pileDecision,
      ChooseOneAnswer(Bracken.pileRef(pile)))
    ended <- rules.resolveWalker(piled.state, actor,
      Bracken.positionDecision, ChooseOneAnswer(end))
    started <- ModifierPlay.picking(rules, ended, actor,
      PowerWindow.SearchModifierSelection, Vector.empty)
    sourced <- rules.resolveWalker(started.state, actor,
      SearchProcedure.sourceDecisionId,
      SearchPlay.source(SearchSource.WorldDeck))
  // `picking` returns events that begin with `ended`'s, so `ended`'s are
  // not added again.
  yield sourced.copy(events = picked.events ++ piled.events ++
    started.events ++ sourced.events)).fold(
    error => throw new AssertionError(error.toString), identity)

  private def pile(transition: OathTransition, region: Region)
      : Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(region)

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(Bracken.id, Bracken.noteKeys, events)

  test("it carries the Search icon"):
    assertEquals(BrackenCard.power.modifier,
      Some(distantlands.catalog.ModifierMark.Action(MajorActionType.Search)))

  test("picked, it pays its secret onto the card and asks a pile, then an " +
      "end, before the Search starts"):
    val picked = brackenPicked(searchAtAncientCity(besideBracken))
    assertEquals(Look(SearchFixture.after(picked)).tokensOn(bracken),
      Tokens(0, 1))
    assertEquals(EconomyActions.parkedDecision(picked.state).decisionId,
      Bracken.pileDecision)
    val piled = accepted(rules.resolveWalker(picked.state, actor,
      Bracken.pileDecision, ChooseOneAnswer(Bracken.pileRef(Region.Cradle))))
    assertEquals(EconomyActions.parkedDecision(piled.state).decisionId,
      Bracken.positionDecision)

  test("to the bottom of the Hinterland pile: the cards not kept go there, " +
      "the last one discarded lowest, and its line says so once"):
    val done = keptFacedown(searchedSending(brackenPicked(
      searchAtAncientCity(besideBracken)), Region.Hinterland, Bracken.bottom))
    assertEquals(pile(done, Region.Hinterland).take(2), unkept.reverse)
    assert(!pile(done, Region.Provinces).exists(unkept.contains))
    assertEquals(said(done.events), Vector(NoteText.Said("discards-bottom",
      "This Search's discards go to the bottom of the Hinterland discard " +
        "pile.", covers = false)))

  test("to the top of p1's own region's pile, the Cradle's"):
    val done = keptFacedown(searchedSending(brackenPicked(
      searchAtAncientCity(besideBracken)), Region.Cradle, Bracken.top))
    assertEquals(pile(done, Region.Cradle).takeRight(2), unkept)
    assertEquals(said(done.events).map(_.key), Vector("discards-top"))

  test("an adviser bumped over the limit goes to the chosen end"):
    val done = keptFaceupBumpingAnAdviser(searchedSending(brackenPicked(
      searchAtAncientCity(table => withThreeFacedownAdvisers(
        besideBracken(table)))), Region.Hinterland, Bracken.bottom))
    assertEquals(pile(done, Region.Hinterland).head, bumped)

  test("the People's Favor's site discard goes to the chosen end"):
    val done = keptAtSiteReplacingTheSiteCard(searchedSending(brackenPicked(
      searchAtAncientCity(table => withPeoplesFavorAndSiteCard(
        besideBracken(table)))), Region.Hinterland, Bracken.bottom))
    assertEquals(pile(done, Region.Hinterland).head, siteCard)

  test("a Search with Bracken replays and round-trips the wire"):
    val ready = searchAtAncientCity(besideBracken)
    val done = keptFacedown(searchedSending(brackenPicked(ready),
      Region.Hinterland, Bracken.bottom))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      SearchFixture.after(done))
    assertWireRoundTrips(done.events)
```

Notes for the implementer:
- If Bracken is the only selectable modifier, the modifier decision starts the Search by itself after its questions. `ModifierPlay.picking` with no modifiers answers "Start" only when the decision was asked, so `searchedSending` works either way.
- If `ActPowerHarness.replayed` needs another argument order, or `EconomyActions.parkedDecision` lives elsewhere, use what `SilverCharmSuite` uses.
- If `assertWireRoundTrips` fails on an event this slice does not add, filter that event as `SilverCharmSuite` does for `ModifyDicePool`, with a comment naming the event, and report it at the review checkpoint.

Create `SearchDiscardsSuite.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.Unwrap.accepted

/** Bracken, Cracked Horn and Ring of Devotion exclude each other: once one
  * is picked, the other two show unavailable (catalog batch 10, slice 9,
  * "Readings"). */
class SearchDiscardsSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture.searchAtAncientCity
  import SearchFixture.rules

  /** p1 holding both relics, beside Bracken, with a secret to pay for it. */
  private val allThree: ReadyGame = searchAtAncientCity(table =>
    table.relic(actor, RingOfDevotionCard).relic(actor, CrackedHornCard)
      .denizen(BrackenCard, at = AncientCitySite).secrets(actor, faceUp = 1))

  /** How `card`'s option shows once `first` is picked: its state and its
    * reason. */
  private def shownAfterPicking(first: PowerId, card: CardId)
      : Option[(String, Option[String])] =
    val begun = accepted(rules.startWalker(Ready(allThree), ActionRef.Search,
      actor))
    val chosen = accepted(rules.resolveWalker(begun.state, actor,
      ModifierPicks.decisionId(PowerWindow.SearchModifierSelection),
      ModifierPlay.pick(first)))
    DecisionOptionRef.ofCard(card).flatMap(ref => ShownOptions.optionFor(
      ShownOptions.atParkedDecision(chosen.state), ref))
      .map(option => (option.state, option.reason))

  private val unavailable =
    Some(("unavailable", Some(SearchDiscards.anotherPicked)))

  test("with Ring of Devotion picked, Cracked Horn and Bracken are " +
      "unavailable"):
    assertEquals(shownAfterPicking(RingOfDevotion.id, CrackedHornCard.id),
      unavailable)
    assertEquals(shownAfterPicking(RingOfDevotion.id, BrackenCard.id),
      unavailable)

  test("with Cracked Horn picked, Ring of Devotion is unavailable"):
    assertEquals(shownAfterPicking(CrackedHorn.id, RingOfDevotionCard.id),
      unavailable)

  test("before any pick, all three are available"):
    val begun = accepted(rules.startWalker(Ready(allThree), ActionRef.Search,
      actor))
    val shown = ShownOptions.atParkedDecision(begun.state)
    Vector[CardId](RingOfDevotionCard.id, CrackedHornCard.id, BrackenCard.id)
      .foreach { card =>
        assertEquals(DecisionOptionRef.ofCard(card).flatMap(
          ShownOptions.optionFor(shown, _)).map(_.state), Some("available"),
          card.value)
      }
```

Bracken's pick is not tested first: its questions park the walk before the next pass of the modifier decision, so the next pass is two answers away. Ring's and the Horn's picks show the rule.

If `ShownOptions.atParkedDecision` reads only `p1` and `PowerFixture.actor` is not `p1`, use `Table.p1` throughout these two suites.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.BrackenSuite distantlands.gameplay.powers.search.SearchDiscardsSuite"`
Expected: compile error, `Bracken` and `BrackenCard` not found in `powers.search`.

- [ ] **Step 3: Write Bracken**

Remove `BrackenCard` from `HoldingBeastDenizens.scala`. Autumn Wind stays there, so the file's imports are still used.

Create `Bracken.scala`:

```scala
package distantlands.gameplay.powers.search

import distantlands.catalog.{Denizen, ExecutableCatalog, ModifierMark,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.AsksOnSelection
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object BrackenCard extends Denizen(DenizenId("197"), "Bracken", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.bracken"),
    persistent = false, cost = Cost(secret = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may put all of the cards you discard on the top or " +
      "bottom of any one discard pile _(even your region's)_.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bracken (card 197), a selected Search modifier for 1 secret, placed on
  * the card: "You may put all of the cards you discard on the top or bottom
  * of any one discard pile (even your region's)." (catalog batch 10,
  * slice 9).
  *
  * Picked and paid for, it asks a discard pile, any of the three, then Top
  * or Bottom, in the same pass of the modifier decision, as `ActingTroupe`
  * asks its suit. Every discard of the Search ([[SearchDiscards]]) then
  * goes to that end of that pile, read from the answers when the discard
  * is built. Several discards to the bottom go one under the other, so the
  * last discarded lies lowest.
  *
  * Its one line follows the answers and names both: "This Search's
  * discards go to the bottom of the Hinterland discard pile." The generic
  * discard lines then name the pile of each card.
  */
case object Bracken extends SearchDiscards, AsksOnSelection:
  val id: PowerId = BrackenCard.power.id
  val card: Denizen = BrackenCard
  override def cost: Cost = BrackenCard.power.cost

  val pileDecision: String = "bracken.pile"
  val positionDecision: String = "bracken.position"
  def pileRef(region: Region): DecisionOptionRef.Button =
    DecisionOptionRef.Button(region.key)
  val top: DecisionOptionRef.Button = DecisionOptionRef.Button("top")
  val bottom: DecisionOptionRef.Button = DecisionOptionRef.Button("bottom")

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "This Search's discards go to the top of the {Cradle discard
    * pile}." */
  val discardsTop: NoteKey = notes.key("discards-top", Vector(
    NotePart.Text("This Search's discards go to the top of the "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "This Search's discards go to the bottom of the {Cradle discard
    * pile}." */
  val discardsBottom: NoteKey = notes.key("discards-bottom", Vector(
    NotePart.Text("This Search's discards go to the bottom of the "),
    NotePart.Arg(0), NotePart.Text(".")))
  override def narratedDecisions: Set[String] =
    Set(pileDecision, positionDecision)

  /** The pile question, the end question, then the line. */
  def selectionQuestions(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[Operation] = Vector(
    Decide(pileDecision, actor, DecisionQuery.ChooseOne(
      Region.all.map(region => DecisionOption.Button(pileRef(region),
        SearchSource.name(SearchSource.RegionalDiscard(region))
          .capitalize)),
      heading = Some("Bracken: choose a discard pile"))),
    Decide(positionDecision, actor, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(top, "Top"),
      DecisionOption.Button(bottom, "Bottom")),
      heading = Some("Bracken: the top or the bottom of the pile?"))),
    Note(id, sentNote))

  /** The pile and the end the answers chose. */
  private def destination(answered: Vector[Answered])
      : Option[(Region, StackPosition)] = for
    pile <- PowerAnswers.last(answered, pileDecision)
      .flatMap(ref => Region.all.find(pileRef(_) == ref))
    end <- PowerAnswers.last(answered, positionDecision).collect {
      case `top` => StackPosition.Top
      case `bottom` => StackPosition.Bottom
    }
  yield (pile, end)

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] =
    destination(answered).fold(Vector[CoreOperation](discard)) {
      (pile, end) => discard match
        case denizen: Discard.Denizen =>
          Vector(denizen.copy(to = pile, position = end))
        case vision: Discard.Vision =>
          Vector(vision.copy(to = pile, position = end))
    }

  private def sentNote(states: NoteStates): Option[LogNote] =
    for
      chosen <- NoteSupport.answer(states, pileDecision)
      pile <- Region.all.find(pileRef(_) == chosen)
      end <- NoteSupport.answer(states, positionDecision)
      key <- Option.when(end == top)(discardsTop)
        .orElse(Option.when(end == bottom)(discardsBottom))
    yield key(PowerSourceRef.Card(cardId),
      NoteArg.Pile(SearchSource.RegionalDiscard(pile)))
```

If `Region.all` or `Region.key` has another name, use what `Scryer` uses for its pile buttons.

In `SearchDiscards.scala`, change `cards` and its comment:

```scala
  /** The three cards' ids. */
  def cards: Set[CardId] =
    Set(BrackenCard.id, CrackedHornCard.id, RingOfDevotionCard.id)
```

In `ActionModifiers.scala`, add `Bracken` to the `powers.search` import (alphabetical) and to `all` after `CrackedHorn`:

```scala
      WineOfWelcome, PaintedTrumpet, SpinningBee, RingOfDevotion,
      CrackedHorn, Bracken,
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.search.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected:
- `unimplemented.txt` loses `denizen.bracken`, and nothing else.
- `declared-keys.txt` gains the two `denizen.bracken` keys, `discards-top` and `discards-bottom`, and nothing else.

- [ ] **Step 6: Update the spec and the roadmap**

In the spec:
- Change the status line to "**Status:** designed 2026-11-03. Slices 1 to 9 landed." If slice 8 has not merged into this branch, write "Slices 1 to 7 and 9 landed." instead.
- Replace the "Slice 9: Search discard destination" body above "Tests:" with:

```markdown
`Discard.Denizen` and `Discard.Vision` gain `position: StackPosition` (Top
or Bottom), defaulted to Top as `Draw.position` is, so no existing builder
changes. The wire field is required.

Each card becomes a Search modifier, a `SelectedModifier` sharing the
`SearchDiscards` trait. Its `Transform`s at `SearchActionEligibility` (the
cards not kept) and `SearchPlayAdviser` (the card play, including an
adviser bumped over the limit and the People's Favor's site discard)
rewrite the user's own discards with `OperationRewrite.ops`. A ruined
edifice still goes to the edifice deck. A WHEN PLAYED power's discard sits
under the card-play hook's own window, which the rewrite does not enter,
so Dazzle's discard is untouched. Spinning Bee's no-match discard needs no
rewrite: Bee and every other Search modifier exclude each other.

- **Ring of Devotion:** `to` becomes the region of the user's pawn. Its id
  becomes `relic.ring-of-devotion`.
- **Bracken** (1 secret): `selectionQuestions` ask a discard pile and Top or
  Bottom, once at the Search's start.
- **Cracked Horn:** each discard becomes `Bury.standard` to the bottom of
  the world deck; a buried Vision leaves the Visions Drawn track alone.

Picking one makes the other two unavailable (see Readings). The generic
discard and bury lines name where each card went. Bracken also names the
end of the pile once per Search, at its pick: "This Search's discards go
to the bottom of the Hinterland discard pile."
```

- In the "Slice 12: Nested card play" section, after the paragraph that starts "**Search Party.**", add:

```markdown
The nested play's `SearchPlayAdviser` fold sees the outer Search's
answers, so a picked Bracken, Cracked Horn or Ring of Devotion would
rewrite its discards. Ruling 5 says Search modifiers do not apply, so this
slice keeps them out of the nested play.
```

In `docs/ROADMAP.md`, in the "**Unchanged from batch 8:**" bullet, delete "Bracken, Cracked Horn and Ring of Devotion (a discard destination),". Re-wrap the bullet to 80 columns as the file does. Leave the rest of the bullet alone: its other cards are later slices' work.

- [ ] **Step 7: Run the gates**

Run, in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes; statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/search/Bracken.scala \
  src/main/scala/distantlands/gameplay/powers/search/SearchDiscards.scala \
  src/main/scala/distantlands/catalog/holding/HoldingBeastDenizens.scala \
  src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/search/BrackenSuite.scala \
  src/test/scala/distantlands/gameplay/powers/search/SearchDiscardsSuite.scala \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): Bracken sends a Search's discards to one end of any pile

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 3 and Task 4 diffs against this plan and the spec.
