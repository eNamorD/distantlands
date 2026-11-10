# Chronicle Stars Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the Beacon, the Stars task archives the rival sites and stores the Empire's sites, and the Chronicle's end journals a `ChronicleUpdated` whose sites, decks, Dispossessed, Reliquary and starting advisers come from the table, checked by the same card accounting game start uses.

**Architecture:** One new triggered procedure, `ChronicleStars`, slots between `ChronicleBeacon` and `ChronicleFinish` in `ChronicleSequence`. Its four steps are power windows. They change nothing on the table (ruling 18): Archive Rival Sites and Store only write one log line per site, and Write in the Stars and Clean Up are empty windows. `ChronicleExtraction` reads the finished table into the Chronicle's table fields, and refuses a card it cannot store. `ChronicleAccounting` holds game start's id and uniqueness checks, extended to every Chronicle field, plus a check that the evolved record keeps every baseline site and relic exactly once. `ChronicleFinish` journals the record only when both checks pass.

**Tech Stack:** Scala 3.9, munit 1.0.4, sbt via `./sbtw`. No frontend change.

**Spec:** `docs/superpowers/specs/2026-10-27-chronicle-steps-design.md` ("Exit and extraction", "Order of work" slice 3). Rules: `docs/rules/chronicle.md` task 5 and `docs/rules/ambiguities.md` "Chronicle rulings" 1-16, plus rulings 17 and 18 below. Previous plan: `docs/superpowers/plans/2026-10-28-chronicle-world-and-beacon.md` (merged at `860aac77`).

**Not in this plan:** starting a playable game from the evolved Chronicle (Persistence; `GameStartRules` still refuses a non-empty `world` and stored denizens or relics on atlas sites), the constellation and narrative, Legacies and Foundations behaviour, Eras end and reset, the TTS codec.

## Global Constraints

- Branch and worktree off local `0.2.0-alpha`, never `main`. Merge back into `0.2.0-alpha`. Symlink the main checkout's `.tooling` and `node_modules` into the worktree before running `./sbtw`.
- Scalac runs with `-Werror`. A new enum case breaks every exhaustive match; fix each one the compiler names. Unused imports fail the build.
- Production Scala files stay at or under 800 lines (`scripts/check-architecture.py`, `BackendArchitectureSuite`). `model` must not import `gameplay`, `application` or `serialization`. `gameplay` must not import `application` or `serialization`. `gameplay/walker/ProcedureWalker.scala` is at 797 lines: add nothing to it.
- Tests run with `distantlands.notes.strict=true`: a log note key not declared by its writer throws.
- Tests never spell catalog ids (`BackendArchitectureSuite`). Use `CatalogNames`, `Table` helpers, or ids read from the table.
- Old journals need not decode. New fields and operations are required on the wire.
- Test helper names state what they build. A doc comment does not excuse a vague name.
- Never `git add -A` (the worktree symlinks show as untracked). Add files by path.
- Commit trailer, exactly: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. A subagent that commits uses this trailer, not its own model's.
- Docs: normal prose. Run `python3 scripts/check-markdown-links.py` after doc edits.
- Rule questions not answered by `docs/rules/chronicle.md` or rulings 1-18 go to the user. Never pick a default.
- Full suite: `./sbtw test` (about 100 s). Coverage gate: `./sbtw clean coverage test coverageReport`, at least 92% statements.

## Rules this plan implements

| Task | Step | Actor | This plan |
|---|---|---|---|
| Stars | 1 Write in the Stars [7] | none in the digital version | empty window |
| Stars | 2 Archive Rival Sites [17, 18] | none | log lines; extraction stores the sites |
| Stars | 3 Clean Up [18] | none | empty window; extraction orders the Empire's sites |
| Stars | 4 Store [8, 18] | none | log lines; extraction and `ChronicleFinish` |

Ruling 17 (user, 2026-10-28): a rival site's remaining cards are all stored with it. After Shape Empire, a rival Enduring site can still hold denizens or a ruined edifice; they go into storage on the site with its relics and intact edifices.

Ruling 18 (user, 2026-10-28): the Stars steps leave the table as it is. The Chronicle stores no pieces, so Clean Up's piece returns change nothing it records. `ChronicleFinish` reads the table once and builds the record.

How the table maps onto the record:

| Chronicle field | Read from |
|---|---|
| `world` | Sites in play that the winner rules (`ChronicleTasks.rules`), in `MapState.hinterward` order (Cradlemost first, which is the Cradleward consolidation), each with its denizens and edifices in slot order and then its relics |
| `atlasBox` | Rival sites in play, in `hinterward` order (Cradlemost on top of the archived stack), then the live Atlas's stored sites in order. The Empire divider is not stored: `world` replaces it |
| `worldDeck` | `commonCards.worldDeck`, in order |
| `relicDeck` | `commonCards.relicDeck`, in order |
| `dispossessed` | `campaign.dispossessed`, then `current.newlyDispossessed` |
| `reliquary` | `campaign.reliquary` |
| `lineages` | As `ChronicleRecord.campaignFields` today, except that a seated lineage takes its live starting advisers and its seated player as `previousPlayer` |
| `era`, `oathkeeperGoal`, `foundations` | Unchanged from `campaignFields` |

Edifices in the edifice deck and denizens in the suited reserves are derived views and are not stored. A card that is anywhere else blocks the record: a held adviser or relic other than the Grand Scepter, a card in a hand, a discard pile, the gathering pile, a set-aside pile, or any Vision. A stored site with more than three cards also blocks it (`StoredSite` holds at most three).

## File Structure

`M/` is `src/main/scala/distantlands/` and `T/` is `src/test/scala/distantlands/`.

- Create `M/gameplay/setup/ChronicleAccounting.scala`: `knownAndUnique` and `conserves`.
- Modify `M/gameplay/setup/GameStartRules.scala`: `validateIds` delegates to `ChronicleAccounting.knownAndUnique`.
- Modify `M/model/Chronicle.scala`: `StoredSite.capacity`.
- Modify `M/model/World.scala`: drop the coverage exemption and the unused `cradleward`.
- Create `M/gameplay/chronicle/ChronicleExtraction.scala`: `SiteLayout`, `layout`, `storedSite`, `extract`.
- Modify `M/gameplay/chronicle/ChronicleRecord.scala`: live starting advisers and `previousPlayer`; new `evolved`.
- Modify `M/gameplay/chronicle/ChronicleFinishProcedure.scala`: journal `ChronicleRecord.evolved`.
- Create `M/gameplay/chronicle/ChronicleStarsProcedure.scala`.
- Modify `M/model/ProcedureRef.scala`, `M/model/PowerWindow.scala`, `M/gameplay/chronicle/ChronicleWindows.scala`, `M/gameplay/chronicle/ChronicleSequence.scala`, `M/gameplay/walker/WalkerProcedureRegistry.scala`, `M/gameplay/actions/ProcedureNotes.scala`, `M/application/gamelog/ProcedureLog.scala`.
- Tests: create `T/gameplay/setup/ChronicleAccountingSuite.scala`, `T/gameplay/chronicle/ChronicleExtractionSuite.scala`, `T/gameplay/chronicle/StarsTaskSuite.scala`. Modify `ChronicleRecordSuite`, `ChronicleEndToEndSuite`, `ChronicleWindowsSuite`, `ProcedureRefSuite`, `EnumWireKeySuite`, the note-key snapshot and the `chronicle-started` log goldens.
- Docs: `docs/rules/ambiguities.md` (rulings 17, 18), `docs/rules/chronicle.md` (Stars digital notes), `docs/rules/implementation-traceability.md`, `docs/ROADMAP.md`, `CONTEXT.md`.

---

### Task 1: Chronicle accounting shared with game start

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/ChronicleAccounting.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala:90-121`
- Test: `src/test/scala/distantlands/gameplay/setup/ChronicleAccountingSuite.scala`

**Interfaces:**
- Produces: `ChronicleAccounting.knownAndUnique(catalog: ExecutableCatalog, chronicle: Chronicle): Either[String, Unit]` and `ChronicleAccounting.conserves(baseline: Chronicle, evolved: Chronicle): Either[String, Unit]`.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.setup

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._
import distantlands.testkit.CatalogNames

/** The card accounting a Chronicle passes before game start reads it, and
  * before the Chronicle Phase journals it (Chronicle steps design, "Exit
  * and extraction"). */
class ChronicleAccountingSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle

  private def lineageAdvising(card: DenizenId): LineageState =
    LineageState(LineageId("lineage:any"), None, Role.Exile, Vector.empty,
      Vector(DenizenState(card, Orientation.FaceDown, Tokens.empty)))

  test("the first-game fixture's Chronicle passes"):
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, chronicle),
      Right(()))

  test("an unknown site in the world is refused"):
    val unknown = chronicle.copy(world = Vector(StoredSite(SiteId("no-site"))))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, unknown),
      Left("unknown atlas site no-site"))

  test("a site both stored and in the world is refused"):
    val twice = chronicle.copy(world = chronicle.atlasBox.take(1))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"duplicate atlas site ${chronicle.atlasBox.head.site.value}"))

  test("a denizen both Dispossessed and a starting adviser is refused"):
    val card = chronicle.worldDeck.head
    val twice = chronicle.copy(worldDeck = chronicle.worldDeck.tail,
      dispossessed = Vector(card), lineages = Vector(lineageAdvising(card)))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"denizen ${card.value} is stored twice"))

  test("a relic both in the Reliquary and the relic deck is refused"):
    val relic = chronicle.relicDeck.head
    val twice = chronicle.copy(reliquary = Vector(relic))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"relic ${relic.value} is stored twice"))

  test("a Vision stored at a site is refused"):
    val vision = VisionRules.Conquest
    val stored = chronicle.copy(world = Vector(StoredSite(
      CatalogNames.site("Ancient City"), Vector(vision))))
      .copy(atlasBox = chronicle.atlasBox.filterNot(
        _.site == CatalogNames.site("Ancient City")))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, stored),
      Left(s"${vision.value} is a vision, which the Chronicle does not store"))

  test("the Grand Scepter is never stored"):
    val stored = chronicle.copy(reliquary = Vector(TheGrandScepterCard.id))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, stored),
      Left("the Grand Scepter is not stored"))

  test("a record that keeps every baseline site and relic conserves it"):
    val moved = chronicle.copy(world = chronicle.atlasBox.take(2),
      atlasBox = chronicle.atlasBox.drop(2),
      relicDeck = chronicle.relicDeck.tail,
      reliquary = chronicle.relicDeck.take(1))
    assertEquals(ChronicleAccounting.conserves(chronicle, moved), Right(()))

  test("a record that loses a site or a relic does not conserve it"):
    assertEquals(ChronicleAccounting.conserves(chronicle,
      chronicle.copy(atlasBox = chronicle.atlasBox.tail)),
      Left(s"site ${chronicle.atlasBox.head.site.value} is missing"))
    assertEquals(ChronicleAccounting.conserves(chronicle,
      chronicle.copy(relicDeck = chronicle.relicDeck.tail)),
      Left(s"relic ${chronicle.relicDeck.head.value} is missing"))

  test("a record with a site or relic the baseline lacks does not conserve it"):
    val extra = chronicle.atlasBox.head
    val baseline = chronicle.copy(atlasBox = chronicle.atlasBox.tail)
    assertEquals(ChronicleAccounting.conserves(baseline, chronicle),
      Left(s"site ${extra.site.value} was not in the baseline"))
```

Check before writing: the Grand Scepter must be in `catalog.relics` (the first-game generator filters it out of that list, so it is), or the Scepter test reports "unknown relic" first. "Ancient City" is one of the fixture's eight sites (`ShapeEmpireSuite` uses it).

- [ ] **Step 2: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.ChronicleAccountingSuite"`
Expected: compile error, `ChronicleAccounting` not found.

- [ ] **Step 3: Write `ChronicleAccounting`**

```scala
package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** The card accounting every Chronicle passes (Chronicle steps design, "Exit
  * and extraction"). Game start checks `knownAndUnique` before it deals;
  * the Chronicle Phase checks both before it journals the evolved record.
  * Denizens and edifices a Chronicle does not list are in the suited
  * reserves and the edifice deck, which are derived, so only sites and
  * relics can go missing. */
object ChronicleAccounting:
  /** Every id is a known card of the kind its field holds, and no card or
    * site is stored twice, across every field. */
  def knownAndUnique(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Either[String, Unit] =
    val siteIds = catalog.sites.map(_.id).toSet
    val edificeIds = catalog.edifices.map(e => EdificeId(e.id.value)).toSet
    val denizenIds = catalog.denizens.map(d => DenizenId(d.id.value)).toSet
    val relicIds = catalog.relics.map(r => RelicId(r.id.value)).toSet
    val sites = (chronicle.atlasBox ++ chronicle.world).map(_.site)
    val items = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
    val advisers = chronicle.lineages.flatMap(_.startingAdvisers).map(_.id)
    val stored = items ++ advisers
    val denizens = chronicle.worldDeck ++ chronicle.dispossessed ++
      stored.collect { case id: DenizenId => id }
    val relics = chronicle.relicDeck ++ chronicle.reliquary ++
      stored.collect { case id: RelicId => id }
    val edifices = stored.collect { case id: EdificeId => id }
    val others = stored.filter {
      case _: DenizenId | _: RelicId | _: EdificeId => false
      case _ => true
    }
    first(Vector(
      sites.find(!siteIds(_)).map(id => s"unknown atlas site ${id.value}"),
      duplicate(sites).map(id => s"duplicate atlas site ${id.value}"),
      edifices.find(!edificeIds(_)).map(id => s"unknown edifice ${id.value}"),
      denizens.find(!denizenIds(_)).map(id => s"unknown denizen ${id.value}"),
      duplicate(chronicle.worldDeck)
        .map(id => s"duplicate world-deck denizen ${id.value}"),
      relics.find(!relicIds(_)).map(id => s"unknown relic ${id.value}"),
      duplicate(chronicle.relicDeck).map(id => s"duplicate relic ${id.value}"),
      others.headOption.map(id =>
        s"${id.value} is a ${id.kind}, which the Chronicle does not store"),
      duplicate(denizens).map(id => s"denizen ${id.value} is stored twice"),
      duplicate(relics).map(id => s"relic ${id.value} is stored twice"),
      duplicate(edifices).map(id => s"edifice ${id.value} is stored twice"),
      relics.find(_ == TheGrandScepterCard.id)
        .map(_ => "the Grand Scepter is not stored")))

  /** `evolved` holds exactly the sites and relics `baseline` holds. The
    * Chronicle Phase moves them; it never adds or loses one. */
  def conserves(baseline: Chronicle, evolved: Chronicle)
      : Either[String, Unit] =
    def sites(chronicle: Chronicle): Vector[SiteId] =
      (chronicle.atlasBox ++ chronicle.world).map(_.site)
    def relics(chronicle: Chronicle): Vector[RelicId] =
      chronicle.relicDeck ++ chronicle.reliquary ++
        (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
          .collect { case id: RelicId => id }
    val (beforeSites, afterSites) = (sites(baseline), sites(evolved).toSet)
    val (beforeRelics, afterRelics) = (relics(baseline), relics(evolved).toSet)
    first(Vector(
      beforeSites.find(!afterSites(_)).map(id => s"site ${id.value} is missing"),
      sites(evolved).find(!beforeSites.contains(_))
        .map(id => s"site ${id.value} was not in the baseline"),
      beforeRelics.find(!afterRelics(_))
        .map(id => s"relic ${id.value} is missing"),
      relics(evolved).find(!beforeRelics.contains(_))
        .map(id => s"relic ${id.value} was not in the baseline")))

  private def first(problems: Vector[Option[String]]): Either[String, Unit] =
    problems.flatten.headOption.toLeft(())

  private def duplicate[A](values: Vector[A]): Option[A] =
    val seen = scala.collection.mutable.HashSet.empty[A]
    values.find(value => !seen.add(value))
```

- [ ] **Step 4: Make `GameStartRules` use it**

In `GameStartRules.scala`, replace the whole body of `validateIds` (lines 90-121) and delete the private `duplicate` helper only if nothing else in the file uses it (`validateParticipants` does: keep it).

```scala
  private def validateIds(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Either[OathViolation, Unit] =
    ChronicleAccounting.knownAndUnique(catalog, chronicle)
      .left.map(UnsupportedChronicle(_))
```

Update the object's doc comment sentence "known and unique ids" to "known and unique ids across every Chronicle field (`ChronicleAccounting`)".

- [ ] **Step 5: Run the new suite and the start suites**

Run: `./sbtw "testOnly distantlands.gameplay.setup.*"`
Expected: all pass. The four existing messages in `GameStartRulesSuite` are unchanged. If another fixture now fails, the fixture stores a card twice: stop and report it rather than weakening the check.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/ChronicleAccounting.scala \
  src/main/scala/distantlands/gameplay/setup/GameStartRules.scala \
  src/test/scala/distantlands/gameplay/setup/ChronicleAccountingSuite.scala
git commit -m "feat(chronicle): check every Chronicle field for known, unique cards and conserved sites and relics

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Extract the table into the journaled Chronicle

**Files:**
- Modify: `src/main/scala/distantlands/model/Chronicle.scala`
- Modify: `src/main/scala/distantlands/model/World.scala:85-91`
- Create: `src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleFinishProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleExtractionSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala`

**Interfaces:**
- Consumes: `ChronicleAccounting.knownAndUnique`, `ChronicleAccounting.conserves` (Task 1); `ChronicleTasks.rules(ready, player, site): Boolean`.
- Produces:
  - `StoredSite.capacity: Int` (3).
  - `ChronicleExtraction.SiteLayout(empire: Vector[SiteId], rivals: Vector[SiteId])`.
  - `ChronicleExtraction.layout(ready: ReadyGame): Either[OathViolation, SiteLayout]`.
  - `ChronicleExtraction.storedSite(site: SiteId, items: Vector[CardId]): Either[OathViolation, StoredSite]`.
  - `ChronicleExtraction.extract(ready: ReadyGame, record: Chronicle): Either[OathViolation, Chronicle]`.
  - `ChronicleRecord.evolved(catalog: ExecutableCatalog, ready: ReadyGame): Either[OathViolation, Chronicle]`.

- [ ] **Step 1: Write the failing extraction tests**

```scala
package distantlands.gameplay.chronicle

import distantlands.model._
import distantlands.testkit.{CatalogNames, Table}
import distantlands.testkit.Table.{p1, p2}

/** Reading the finished table into the Chronicle's table fields (Chronicle
  * steps design, "Exit and extraction"; Chronicle rulings 17 and 18). */
class ChronicleExtractionSuite extends munit.FunSuite:
  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  /** p1's won game, p1 an Exile ruling Ancient City and Broken Peaks. */
  private val p1RulesTwoSites: Table = ChronicleWalk.chronicleWonByP1
    .warbandsAt("Ancient City", p1, 1).warbandsAt("Broken Peaks", p1, 1)

  test("the winner's sites are the Empire's and the rest are rivals, " +
      "each Cradlemost first"):
    val ready = p1RulesTwoSites.ready
    val inPlay = ready.game.current.map.hinterward
    val layout = ChronicleExtraction.layout(ready)
    assertEquals(layout, Right(ChronicleExtraction.SiteLayout(
      inPlay.filter(Set(ancientCity, brokenPeaks)),
      inPlay.filterNot(Set(ancientCity, brokenPeaks)))))

  test("a game with no result has no layout"):
    assert(ChronicleExtraction.layout(Table.start.ready).isLeft)

  test("a stored site holds at most three cards"):
    val cards = Vector("Fae Merchant", "Threatening Roar")
      .map(CatalogNames.denizen) ++ Vector("Brass Horse")
      .map(CatalogNames.relic)
    assertEquals(ChronicleExtraction.storedSite(ancientCity, cards),
      Right(StoredSite(ancientCity, cards)))
    val four = cards :+ CatalogNames.relic("Book of Records")
    assertEquals(ChronicleExtraction.storedSite(ancientCity, four),
      Left(OathViolation.UnsupportedChronicle(s"${ancientCity.value} holds " +
        "4 cards; a stored site holds at most 3")))

  test("a held adviser blocks the record"):
    val card = CatalogNames.denizen("Fae Merchant")
    val ready = p1RulesTwoSites.adviser(p2, "Fae Merchant").ready
    ChronicleExtraction.extract(ready, ready.game.campaign.baseline) match
      case Left(OathViolation.UnsupportedChronicle(reason)) =>
        assert(reason.contains(card.value), reason)
      case other => fail(other.toString)
```

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleExtractionSuite"`
Expected: compile error, `ChronicleExtraction` not found.

- [ ] **Step 3: Add the stored-site capacity**

In `Chronicle.scala`, below `StoredSite`:

```scala
final case class StoredSite(site: SiteId, items: Vector[CardId] = Vector.empty):
  require(items.size <= StoredSite.capacity,
    s"a stored site holds at most ${StoredSite.capacity} items")

object StoredSite:
  /** The most cards a site keeps in storage. */
  val capacity: Int = 3
```

- [ ] **Step 4: Retire the unused map direction**

In `World.scala`, replace lines 85-91 (the comment, `// $COVERAGE-OFF$`, both directions, `// $COVERAGE-ON$`) with:

```scala
  /** The map from the Cradle out (CR pp. 9, 45): the order the Chronicle's
    * Stars task archives rival sites in and keeps the Empire's sites in. */
  def hinterward: Vector[SiteId] = inPlay
```

Run `git grep -n "cradleward" src` and confirm nothing else uses it.

- [ ] **Step 5: Write `ChronicleExtraction`**

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** The table as the Stars task stores it (CR pp. 40-41, Chronicle rulings
  * 17 and 18). The Stars steps change nothing on the table; this reads it
  * once into the Chronicle's table fields. The winner's sites become the
  * Empire's `world` and the rest are archived at the atlas's Recent end,
  * each with every card left on it. A card in a pile the Chronicle cannot
  * store refuses the record. */
object ChronicleExtraction:
  /** The sites in play, split by ruler, each in `MapState.hinterward`
    * order: Cradlemost first. */
  final case class SiteLayout(empire: Vector[SiteId], rivals: Vector[SiteId])

  def layout(ready: ReadyGame): Either[OathViolation, SiteLayout] =
    ready.game.current.result.map(_.winner).toRight(OathViolation
      .InvalidEventOrder("the Chronicle stores a game with no winner")).map(
      winner =>
        val (empire, rivals) = ready.game.current.map.hinterward
          .partition(ChronicleTasks.rules(ready, winner, _))
        SiteLayout(empire, rivals))

  def storedSite(site: SiteId, items: Vector[CardId])
      : Either[OathViolation, StoredSite] =
    Either.cond(items.size <= StoredSite.capacity, StoredSite(site, items),
      unstorable(s"${site.value} holds ${items.size} cards; a stored site " +
        s"holds at most ${StoredSite.capacity}"))

  /** `record` with its sites, decks, Dispossessed and Reliquary read from
    * the table. */
  def extract(ready: ReadyGame, record: Chronicle)
      : Either[OathViolation, Chronicle] =
    val current = ready.game.current
    val campaign = ready.game.campaign
    def atSite(site: SiteId) = storedSite(site, current.map.sites.get(site)
      .toVector.flatMap(state =>
        state.denizens.map(_.id) ++ state.relics.map(_.id)))
    for
      _ <- leftovers(ready)
      sites <- layout(ready)
      empire <- all(sites.empire.map(atSite))
      archived <- all(sites.rivals.map(atSite))
      carried <- all(campaign.atlas.entries.collect {
        case AtlasEntry.StoredSite(id, denizens, relics, edifice) =>
          storedSite(id,
            edifice.toVector ++ denizens.map(_.id) ++ relics.map(_.id))
      })
    yield record.copy(atlasBox = archived ++ carried, world = empire,
      worldDeck = denizens(current.commonCards.worldDeck),
      relicDeck = current.commonCards.relicDeck,
      dispossessed = denizens(campaign.dispossessed ++ current.newlyDispossessed),
      reliquary = campaign.reliquary)

  /** `leftovers` has already refused any Vision, so every world card left
    * is a denizen. */
  private def denizens(cards: Vector[WorldCardId]): Vector[DenizenId] =
    cards.collect { case id: DenizenId => id }

  private def leftovers(ready: ReadyGame): Either[OathViolation, Unit] =
    ready.game.cardIndex.left.map(problems =>
      unstorable(problems.mkString(", "))).flatMap(index =>
      val stray = index.ids.toVector.flatMap(index.get)
        .filterNot(storable).sortBy(_.id.value)
      Either.cond(stray.isEmpty, (), unstorable(
        "the Chronicle cannot store " + stray.map(card =>
          s"${card.id.value} (${card.location.container})").mkString(", "))))

  private def storable(card: LocatedCard): Boolean =
    card.id match
      case _: VisionId => false
      case _ => card.location.container match
        case CardContainer.Deck(_) | CardContainer.Site(_, _) |
            CardContainer.AtlasSite(_, _, _) | CardContainer.Lineage(_, _) |
            CardContainer.Reliquary | CardContainer.Dispossessed |
            CardContainer.NewlyDispossessed | CardContainer.SuitedReserve(_) =>
          true
        case CardContainer.Player(_, PlayerCardArea.Relics) =>
          card.id == TheGrandScepterCard.id
        case _ => false

  private def all[A](results: Vector[Either[OathViolation, A]])
      : Either[OathViolation, Vector[A]] =
    results.foldLeft[Either[OathViolation, Vector[A]]](Right(Vector.empty))(
      (done, next) => done.flatMap(values => next.map(values :+ _)))

  private def unstorable(reason: String): OathViolation =
    OathViolation.UnsupportedChronicle(reason)
```

If the compiler reports the `CardContainer` match as non-exhaustive, the default `case _ => false` already covers it; if it reports an unreachable case, a container name above is wrong: read `M/model/CardIndex.scala:9-30` and fix the name.

- [ ] **Step 6: Run the extraction tests**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleExtractionSuite"`
Expected: PASS.

- [ ] **Step 7: Write the failing record tests**

In `ChronicleRecordSuite.scala`, change the expectation of "a seated lineage the baseline lists keeps its legacies" to the seated player (the live lineage seats `p2`):

```scala
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head,
      LineageState(lineage, Some(p2), Role.Exile, Vector(legacy),
        Vector.empty))
```

Add:

```scala
  test("a seated lineage takes the starting advisers this Chronicle kept"):
    val lineage = lineageOf(Table.start.ready, p2)
    val kept = DenizenState(CatalogNames.denizen("Fae Merchant"),
      Orientation.FaceDown, Tokens.empty)
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        lineage, None, Role.Exile, Vector.empty, Vector(DenizenState(
          CatalogNames.denizen("Threatening Roar"), Orientation.FaceDown,
          Tokens.empty))))),
      lineages = campaign.lineages.updatedWith(lineage)(
        _.map(_.copy(startingAdvisers = Vector(kept))))))).ready
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head
      .startingAdvisers, Vector(kept))
```

Replace the doc comment on the class with: "What the journaled Chronicle carries from the campaign: the live roles, era scores, Oathkeeper goal and starting advisers over the baseline. `ChronicleExtraction` supplies the table fields."

- [ ] **Step 8: Run them to see the new expectations fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleRecordSuite"`
Expected: the two changed tests FAIL (`previousPlayer` is `None`, the starting advisers are the baseline's).

- [ ] **Step 9: Update `ChronicleRecord`**

Replace the file body with:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.setup.ChronicleAccounting
import distantlands.model._

/** The Chronicle the Chronicle Phase journals (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores, the Oathkeeper goal and
  * starting advisers come from the live campaign; sites, decks, the
  * Dispossessed and the Reliquary from the table (`ChronicleExtraction`). */
object ChronicleRecord:
  /** The evolved Chronicle, when it passes game start's card accounting
    * and keeps every baseline site and relic. */
  def evolved(catalog: ExecutableCatalog, ready: ReadyGame)
      : Either[OathViolation, Chronicle] =
    for
      record <- ChronicleExtraction.extract(ready, campaignFields(ready))
      _ <- ChronicleAccounting.knownAndUnique(catalog, record)
        .flatMap(_ => ChronicleAccounting.conserves(
          ready.game.campaign.baseline, record))
        .left.map(OathViolation.UnsupportedChronicle(_))
    yield record

  def campaignFields(ready: ReadyGame): Chronicle =
    val campaign = ready.game.campaign
    val seatedBy = ready.game.current.players
      .map(player => player.lineage -> player.player).toMap
    // A seated lineage the baseline lists keeps its baseline legacies: game
    // start seats every lineage fresh, so the live state has none of its
    // own yet. Its starting advisers are the ones this Chronicle kept.
    val kept = campaign.baseline.lineages.map(lineage =>
      campaign.lineages.get(lineage.id)
        .map(live => lineage.copy(role = live.role,
          previousPlayer = seatedBy.get(lineage.id),
          startingAdvisers = live.startingAdvisers))
        .getOrElse(lineage.copy(role =
          campaign.boxRoles.getOrElse(lineage.id, lineage.role))))
    val known = kept.map(_.id).toSet
    val added = campaign.lineages.values.toVector
      .filterNot(lineage => known(lineage.id)).sortBy(_.id.value)
      .map(live => LineageState(live.id, seatedBy.get(live.id), live.role,
        Vector.empty, live.startingAdvisers))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal, lineages = kept ++ added)
```

- [ ] **Step 10: Journal the evolved record**

In `ChronicleFinishProcedure.scala`, replace the `BuildOps` body and the doc comment:

```scala
/** The last Chronicle procedure: journals the evolved Chronicle (Chronicle
  * steps design, "Exit and extraction"), and refuses one that fails the
  * card accounting. */
object ChronicleFinishProcedure:
  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty,
      // Rooted in a Sequence: the walker names nodes by path from a
      // container, so a bare leaf at the root has no node id.
      Sequence(Vector(BuildOps((state, _) =>
        ChronicleRecord.evolved(catalog, state)
          .map(chronicle => Vector(ChronicleUpdated(chronicle))),
        required = true))),
      OathViolation.InvalidEventOrder(
        "the Chronicle's end takes no start selection"))
```

- [ ] **Step 11: Run the Chronicle suites**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.setup.*"`
Expected: PASS, including `ChronicleEndToEndSuite`: its Finish now reads the table. If a suite fails with "the Chronicle cannot store …", read the named container. A card left by an earlier task is a bug in that task or in this table mapping: stop and report it with the message.

- [ ] **Step 12: Run the full suite**

Run: `./sbtw test`
Expected: PASS. `GameApplicationServiceStreamSuite`, the log goldens and `ChronicleBeginSuite` also run the Chronicle to its end. A golden diff here is unexpected (Finish is a silent procedure): investigate before rewriting a golden.

- [ ] **Step 13: Commit**

```bash
git add src/main/scala/distantlands/model/Chronicle.scala \
  src/main/scala/distantlands/model/World.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleRecord.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleFinishProcedure.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleExtractionSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleRecordSuite.scala
git commit -m "feat(chronicle): journal the table's sites, decks, Dispossessed, Reliquary and starting advisers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The Stars task

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/ChronicleStarsProcedure.scala`
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala:91-94,145-149`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala:225`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSequence.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala:357-366`
- Modify: `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala:101-103`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala:346-351`
- Test: `src/test/scala/distantlands/gameplay/chronicle/StarsTaskSuite.scala`
- Test: `ChronicleWindowsSuite`, `ProcedureRefSuite`, `EnumWireKeySuite`, the note-key snapshot, the `chronicle-started` goldens

**Interfaces:**
- Consumes: `ChronicleExtraction.layout`, `ChronicleExtraction.SiteLayout` (Task 2); `ChronicleTasks.failing(violation: OathViolation): Operation`.
- Produces: `TriggeredProcedureRef.ChronicleStars` (key `"chronicle-stars"`); windows `ChronicleWriteInTheStars`, `ChronicleArchiveRivalSites`, `ChronicleCleanUp`, `ChronicleStore`; `ChronicleWindows.stars`; `ChronicleStarsProcedure.archived: NoteKey` and `ChronicleStarsProcedure.keptInEmpire: NoteKey`.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{CatalogNames, Outcomes, RecordedOps, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** The Stars task (CR pp. 40-41, Chronicle rulings 7, 8, 17 and 18): rival
  * sites are archived with their cards, the Empire's sites are kept, and
  * the journaled Chronicle reads both from the table. p1 is the Chancellor
  * holding the Throne; the Empire holds Ancient City (an Enduring site)
  * only when a test gives it an Imperial warband. */
class StarsTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  private def empireAt(sites: String*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  /** `table`, from the Throne through Finish, every choice plain. */
  private def chronicleFinished(table: Table)(using munit.Location)
      : OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.next(
      OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun)))

  private def recordOf(done: OathTransition)(using munit.Location)
      : Chronicle =
    RecordedOps.all(done.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.lastOption.getOrElse(fail("no Chronicle was journaled"))

  private def cardsAt(ready: ReadyGame, site: SiteId): Vector[CardId] =
    val state = ready.game.current.map.sites(site)
    state.denizens.map(_.id) ++ state.relics.map(_.id)

  test("rival sites are archived Cradlemost first, above the stored atlas, " +
      "and the Empire keeps its sites"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    val stored = ready.game.campaign.atlas.entries.collect {
      case AtlasEntry.StoredSite(id, _, _, _) => id }
    val inPlay = ready.game.current.map.hinterward
    assertEquals(record.world.map(_.site), Vector(brokenPeaks))
    assertEquals(record.atlasBox.map(_.site),
      inPlay.filterNot(_ == brokenPeaks) ++ stored)

  test("every site keeps every card left on it, including an Enduring " +
      "rival site's denizen"):
    val roar = CatalogNames.denizen("Threatening Roar")
    val done = chronicleFinished(empireAt("Broken Peaks")
      .denizen("Threatening Roar", at = "Ancient City"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    assert(record.atlasBox.find(_.site == ancientCity).get.items
      .contains(roar), record.atlasBox)
    (record.world ++ record.atlasBox.take(7)).foreach(stored =>
      assertEquals(stored.items, cardsAt(ready, stored.site), stored.site))

  test("the record takes the decks, the Reliquary, both Dispossessed piles " +
      "and the kept starting advisers from the table"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    val current = ready.game.current
    val campaign = ready.game.campaign
    assertEquals(record.worldDeck, current.commonCards.worldDeck.collect {
      case id: DenizenId => id })
    assertEquals(record.relicDeck, current.commonCards.relicDeck)
    assertEquals(record.reliquary, campaign.reliquary)
    assertEquals(record.dispossessed, (campaign.dispossessed ++
      current.newlyDispossessed).collect { case id: DenizenId => id })
    assertEquals(current.newlyDispossessed.size, 6)
    current.players.foreach(player =>
      assertEquals(record.lineages.find(_.id == player.lineage)
        .map(_.startingAdvisers),
        Some(campaign.lineages(player.lineage).startingAdvisers)))

  test("each archived and kept site writes its line"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val inPlay = Outcomes.ready(done.state).game.current.map.hinterward
    val said = NoteText.said(
      ProcedureNotes.id(TriggeredProcedureRef.ChronicleStars),
      ChronicleStarsProcedure.noteKeys, done.events).map(_.text)
    assertEquals(said, inPlay.filterNot(_ == brokenPeaks)
      .map(site => s"${site.value} was archived") :+
      s"${brokenPeaks.value} stays in the Empire")

  test("the Stars follow the Beacon and precede Finish"):
    assertEquals(Vector(TriggeredProcedureRef.ChronicleBeacon,
      TriggeredProcedureRef.ChronicleStars).map(ChronicleSequence.after),
      Vector(Some(TriggeredProcedureRef.ChronicleStars),
        Some(TriggeredProcedureRef.ChronicleFinish)))
```

The third test asserts the six newly Dispossessed are still on the table after Finish: the Stars change nothing (ruling 18). In the second test, `atlasBox.take(7)` is the archived rival sites (eight in play, one in the Empire).

Edit `ChronicleWindowsSuite`:
- In "a power folded into every Chronicle window runs once per window", add `++ ChronicleWindows.stars` to `windows`.
- Add:

```scala
  test("Stars window keys stay as power data spells them"):
    assertEquals(ChronicleWindows.stars.map(_.key),
      Vector("chronicle.stars.write-in-the-stars",
        "chronicle.stars.archive-rival-sites", "chronicle.stars.clean-up",
        "chronicle.stars.store"))
```

- Rename "the Throne is followed by the World, the Beacon, then Finish" to "the Throne is followed by the World, the Beacon, then the Stars", and change its last expected value to `Some(TriggeredProcedureRef.ChronicleStars)`.

Edit `ProcedureRefSuite.scala:35`: add `TriggeredProcedureRef.ChronicleStars,` after `TriggeredProcedureRef.ChronicleBeacon,`. Edit `EnumWireKeySuite.scala:20`: `"chronicle-beacon", "chronicle-stars", "chronicle-finish"`.

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.StarsTaskSuite"`
Expected: compile error, `ChronicleStars` not found.

- [ ] **Step 3: Add the procedure reference and windows**

`ProcedureRef.scala`, after `ChronicleBeacon`:

```scala
  /** The Stars task: archive and store the sites. No actor. */
  case ChronicleStars extends TriggeredProcedureRef("chronicle-stars")
```

and in `TriggeredProcedureRef.all`, `ChronicleBeacon, ChronicleStars, ChronicleFinish)`.

`PowerWindow.scala`, after `ChronicleShapeFoundation`:

```scala
  case object ChronicleWriteInTheStars extends OtherWindow:
    val key = "chronicle.stars.write-in-the-stars"
  case object ChronicleArchiveRivalSites extends OtherWindow:
    val key = "chronicle.stars.archive-rival-sites"
  case object ChronicleCleanUp extends OtherWindow:
    val key = "chronicle.stars.clean-up"
  case object ChronicleStore extends OtherWindow:
    val key = "chronicle.stars.store"
```

`ChronicleWindows.scala`, after `beacon`:

```scala
  val stars: Vector[PowerWindow] = Vector(ChronicleWriteInTheStars,
    ChronicleArchiveRivalSites, ChronicleCleanUp, ChronicleStore)
```

`ChronicleSequence.scala`: add `TriggeredProcedureRef.ChronicleStars` between `ChronicleBeacon` and `ChronicleFinish` in `order`, and delete the doc sentence "Stars is later inserted between Beacon and Finish."

- [ ] **Step 4: Write `ChronicleStarsProcedure`**

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** The Stars task (CR pp. 40-41, `docs/rules/chronicle.md` task 5). No step
  * has an actor, and none changes the table (Chronicle ruling 18): the
  * Chronicle stores no pieces, and `ChronicleFinish` reads the sites as
  * these steps name them. Each step is its own power window. */
object ChronicleStarsProcedure:
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleStars)
  private val notes = NoteSet(noteId)
  val archived: NoteKey = notes.key("archived", Vector(NotePart.Arg(0),
    NotePart.Text(" was archived")))
  val keptInEmpire: NoteKey = notes.key("kept-in-empire", Vector(
    NotePart.Arg(0), NotePart.Text(" stays in the Empire")))

  /** The notes the Stars' steps write. */
  def noteKeys: Vector[NoteKey] = notes.declared

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, Sequence(steps),
      OathViolation.InvalidEventOrder("the Stars take no start selection"))

  private val steps: Vector[Operation] = Vector(
    // The constellation and narrative are not implemented (ruling 7).
    ChronicleWindows.step(PowerWindow.ChronicleWriteInTheStars, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleArchiveRivalSites,
      Vector(perSite(_.rivals, archived))),
    // Pieces return to their banks, which the Chronicle does not store;
    // the Empire's sites are already kept Cradlemost first.
    ChronicleWindows.step(PowerWindow.ChronicleCleanUp, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleStore,
      Vector(perSite(_.empire, keptInEmpire))))

  private def perSite(sites: ChronicleExtraction.SiteLayout => Vector[SiteId],
      key: NoteKey): Operation =
    Branch((state, _) => ChronicleExtraction.layout(state) match
      case Left(violation) => Vector(ChronicleTasks.failing(violation))
      case Right(layout) => sites(layout).map(site =>
        Note(noteId, _ => Some(key.sourceless(NoteArg.Site(site))))))
```

- [ ] **Step 5: Register it**

`WalkerProcedureRegistry.scala`, between the Beacon and Finish entries:

```scala
    TriggeredProcedureRef.ChronicleStars -> Entry(
      rollDecisionId = None,
      modifierWindow = None,
      build = ChronicleStarsProcedure.build,
      rebuild = ChronicleStarsProcedure.build),
```

and add `ChronicleStarsProcedure` to the `distantlands.gameplay.chronicle` import on line 4.

`ProcedureNotes.scala`, after the Beacon case (and add `ChronicleStarsProcedure` to its import on line 6):

```scala
      case TriggeredProcedureRef.ChronicleStars =>
        ChronicleStarsProcedure.noteKeys
```

`ProcedureLog.scala`, after the Beacon case (import `ChronicleStarsProcedure` beside `WelcomeStranger`):

```scala
    case TriggeredProcedureRef.ChronicleStars => ProcedureLog(tag = None,
      lines = ActionLineSource.Notes(Map(
        ChronicleStarsProcedure.archived -> LogKind.Delta,
        ChronicleStarsProcedure.keptInEmpire -> LogKind.Delta)),
      narratedPrefixes = Vector.empty, isSection = false)
```

- [ ] **Step 6: Run the Stars and wiring suites**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.model.*"`
Expected: PASS.

- [ ] **Step 7: Regenerate the note snapshot and log goldens, then read the diffs**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly *DeclaredNoteKeysSuite"`
Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly *GameLogGolden*"` (find the golden suite with `git grep -ln GAMELOG_GOLDEN src/test`).
Run: `git diff --stat; git diff -- '*declared-keys.txt' '*chronicle-started*'`
Expected: `declared-keys.txt` gains `chronicle-stars` `archived` and `kept-in-empire`; the goldens gain one "was archived" line per rival site and one "stays in the Empire" line per Empire site, after the Beacon's lines. Any other change is a regression: investigate it.

- [ ] **Step 8: Run the full suite**

Run: `./sbtw test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ChronicleStarsProcedure.scala \
  src/main/scala/distantlands/model/ProcedureRef.scala \
  src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleSequence.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala \
  src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala/distantlands/gameplay/chronicle/StarsTaskSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala \
  src/test/scala/distantlands/model/ProcedureRefSuite.scala \
  src/test/scala/distantlands/model/EnumWireKeySuite.scala
git add $(git diff --name-only -- '*declared-keys.txt' '*chronicle-started*')
git commit -m "feat(chronicle): the Stars archive rival sites and keep the Empire's sites

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The round trip end to end, and the docs

**Files:**
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala`
- Modify: `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`, `docs/rules/implementation-traceability.md`, `docs/ROADMAP.md`, `CONTEXT.md`

**Interfaces:**
- Consumes: `ChronicleAccounting.knownAndUnique`, `ChronicleAccounting.conserves` (Task 1); `ChronicleExtraction.layout` (Task 2); `GameStartRules.evolve`, `FirstGameSetupFixture.orders`.

- [ ] **Step 1: Extend the end-to-end test**

In `ChronicleEndToEndSuite`, rename the test to "the winner's Chronicle runs every task to a journaled record that passes game start's accounting, and replays", update the class doc to name the Stars, and add before the replay assertion:

```scala
    val layout = accepted(ChronicleExtraction.layout(ready))
    assertEquals(record.world.map(_.site), layout.empire)
    assert(layout.empire.nonEmpty)
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      ready.game.campaign.baseline, record), Right(()))
    // An Empire-state Chronicle is checked, not played: game start waits
    // for the Empire phase (Chronicle steps design, "Exit and extraction").
    assertEquals(GameStartRules.evolve(catalog, record,
      FirstGameSetupFixture.orders),
      Left(OathViolation.UnsupportedChronicle(
        "a non-empty world requires the Empire")))
```

Add imports `distantlands.gameplay.setup.{ChronicleAccounting, FirstGameSetupFixture, GameStartRules}` (merge with the existing `FirstGameSetupFixture.catalog` import). `accepted` works on `Either[OathViolation, A]`; if it does not accept this type, use `.toOption.get`.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: PASS.

- [ ] **Step 3: Record rulings 17 and 18**

Append to "Chronicle rulings" in `docs/rules/ambiguities.md`:

```markdown
17. **Rival sites keep every card.** Archive Rival Sites stores every card left on a rival site with it, not only relics and intact edifices. After Shape Empire, a rival Enduring site can still hold denizens or a ruined edifice; they are stored too (CR p. 40).
18. **The Stars on the table.** The Stars steps change nothing on the table. The Chronicle stores no pieces, so Clean Up's piece returns change nothing it records. The digital version reads the finished table once, at the end of the Chronicle Phase, and stores the sites as Archive Rival Sites, Clean Up and Store order them (CR pp. 40-41).
```

- [ ] **Step 4: Add the Stars digital notes**

In `docs/rules/chronicle.md` task 5, add a `- Digital:` sub-bullet under each step. Link each ruling reference to `ambiguities.md#chronicle-rulings`, as the World and Beacon notes do:
- Step 1: "Digital: an empty window (ruling 7)."
- Step 2: "Digital: one log line per archived site. The rival sites are stored at the atlas's Recent end, Cradlemost first, each with every card left on it (rulings 17, 18). The Empire divider is not stored: the Chronicle keeps the Empire's sites in their own list."
- Step 3: "Digital: an empty window. The Chronicle stores no pieces, and the Empire's sites are kept Cradlemost first (ruling 18)."
- Step 4: "Digital: one log line per Empire site. The Chronicle Phase then journals the record, which must pass game start's card accounting and keep every site and relic it started with. The Dispossessed are the older ones followed by the six the World task added."

- [ ] **Step 5: Update traceability, roadmap and glossary**

`docs/rules/implementation-traceability.md` (link ruling references to `ambiguities.md#chronicle-rulings` there too):
- World and Beacon rows: replace the "reach the Chronicle only with the Stars slice's extraction" and "extraction waits for Stars" remarks with "Recorded in the Chronicle by the Stars task."
- Throne row: drop "sites, decks and the Reliquary reach the Chronicle only with the Stars slice's extraction;".
- Stars row (line 132): status **Partial**. Evidence: "Archive Rival Sites and Store write one line per site; the journaled Chronicle reads the sites, decks, Dispossessed, Reliquary and starting advisers from the table and passes game start's card accounting (`gameplay/chronicle/StarsTaskSuite.scala`, `ChronicleExtractionSuite`, `gameplay/setup/ChronicleAccountingSuite.scala`, `ChronicleEndToEndSuite`); rulings 17, 18." Gap: "Write in the Stars is not implemented. Game start refuses an Empire-state Chronicle until the Empire phase; Persistence starts the next game."
- Hinterward row (line 152): "`MapState.hinterward` orders the map from the Cradle out; the Stars task archives and keeps sites in that order (`StarsTaskSuite`). `AtlasEntry` stores sites in order around the Empire divider." Gap: "None."

`docs/ROADMAP.md` near line 204: replace "Remaining: Stars and the extraction of the table into the Chronicle." with a new sub-bullet:

```markdown
  - Stars landed: rival sites are archived and the Empire's sites kept, and
    the journaled Chronicle reads the table and passes game start's card
    accounting. Starting the next game from it is Persistence's work.
```

and tick the phase's own checkbox if every sub-item of it is now done (read the surrounding list first; leave it if anything else is open).

`CONTEXT.md`, after **Newly Dispossessed**, add in the same format:

```markdown
**Archived site**: a rival site the Stars task stores at the atlas's Recent end with every card left on it. The Empire's sites are kept in the Chronicle's `world` instead.
```

- [ ] **Step 6: Check the docs**

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken links.

- [ ] **Step 7: Run the full gates**

Run: `./sbtw test` then `python3 scripts/check-architecture.py` then `./sbtw clean coverage test coverageReport`
Expected: all pass; statement coverage at least 92%.

- [ ] **Step 8: Commit**

```bash
git add src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala \
  docs/rules/ambiguities.md docs/rules/chronicle.md \
  docs/rules/implementation-traceability.md docs/ROADMAP.md CONTEXT.md
git commit -m "test(chronicle): the journaled Chronicle passes game start's accounting; document the Stars and rulings 17-18

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
