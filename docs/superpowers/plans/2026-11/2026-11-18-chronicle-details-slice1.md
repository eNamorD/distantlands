# Chronicle Details, Slice 1: Projection Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** Every seat's snapshot carries `chronicleDetails`: the live era
scorepad, every lineage in the Chronicle (seated or not) with its score,
role and the legacies the viewer may see, and the Atlas's Recent site.

**Architecture:** Two new protocol types and their codec sit in
`shared/.../protocol/projection`, beside the other projection DTOs.
`GameProjection` gains a required wire field. The server builds it in
`application/GameProjection.scala` beside `chronicleView`, from
`context.ready.game.campaign`, through `GamePresentationProjector`: its
existing `legacies` (R36 and R18 already applied) and a new `atlasSite`
that shares the map site's catalog facts. No new visibility rule is
written for legacies. One clause is added to `identifiesCard` so the Recent
site's denizens and edifice are named everywhere the overlay names them
(open question Q2).

**Why unseated lineages need more than `campaign.lineages`.** Game start
fills `campaign.lineages` with the seated lineages only
(`GameStartRules.evolve`). A lineage nobody seated lives in
`campaign.baseline.lineages`, and a role this game gives it in the box is
in `campaign.boxRoles` (`ChronicleStateOperations.roleChanged`). Crown
Chancellor reads every lineage's role the same way (`ChronicleCrown.roles`),
and so does the record of the evolved Chronicle
(`ChronicleRecord.campaignFields`). The projection reads them that way
too (open question Q1).

**Tech Stack:** Scala 3.9.0, munit, Scala.js frontend, `./sbtw` (sbt
wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Chronicle Details design](../../specs/2026-11/2026-11-18-chronicle-details-design.md),
rulings D1 and D3, section "Slice 1: projection". Rulings made while this
plan runs go in the
[implementation rulings](../../specs/2026-11/2026-11-18-chronicle-details-implementation-rulings.md).

## Global Constraints

- D1: a dormant legacy shows its face to its lineage's player; every other
  viewer sees a legacy back, except during the Chronicle Phase, where every
  viewer sees every face (R18). An unseated lineage's dormant legacies are
  backs to every viewer outside the Chronicle Phase. Active legacies are
  faceup for everyone. This is `identifiesCard`'s existing
  `CardContainer.Lineage(_, LineageCardArea.Legacies)` arm, unchanged.
- D3: the server projects the data on every snapshot, as a new
  `GameProjection` field, present whenever the game has a campaign: every
  `Ready` snapshot. A game not yet started has none.
- On the wire `chronicleDetails` is required, with no default: a snapshot
  without the key is refused. It is `null` before a game starts. Old
  journals and snapshots need not stay decodable.
- Inside `chronicleDetails` every key is required. `role` and
  `atlasRecent` are `null` when absent (Q4). Scorepad marks keep the
  existing `ChronicleScorepadEntry` wire, whose `role` is omitted for an
  Exile, so one codec serves both places.
- A stored relic is `hiddenCard("relic")` to every viewer. A stored site
  has no loose favor, secrets, warbands or festival marker.
- Protected (do not change): `identifiesCard`'s arms other than the new
  `AtlasSite` arm; `ChronicleCrown`, `ChronicleRecord`, `GameStartRules`;
  the `ChronicleEntry` wire format; every golden log.
- Work in `/Users/roman/projects/distantlands/.worktrees/chronicle-details`
  on branch `chronicle-details`. `.tooling` and `node_modules` are
  symlinks; never commit them. Never commit on `0.2.0-alpha` or `main`.
  Push nothing, merge nothing.
- Stage files by name. The hook blocks `git add -A`, `git add -u`,
  `git add .`, `git add --all`, `git commit -a` and `git stash`.
- Commits use Conventional Commits with a scope and end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Run
  `git log -3` after each commit and report any commit that is not ours.
- Code, comments, docs and commit messages are plain English prose. Wrap
  Scala and Markdown at 80 columns. Scaladoc cites "Chronicle Details
  design, slice 1".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## Open questions (rulings pending)

The plan below is written with each recommendation applied. A different
ruling changes only the steps named.

- **Q1. Where unseated lineages come from.** (A, recommended) The
  projection lists `campaign.lineages` plus the `campaign.baseline`
  lineages not in it, each boxed one with
  `campaign.boxRoles.getOrElse(id, role)`, the rule `ChronicleCrown.roles`
  uses; written once, privately, in `GameProjection.scala`.
  `GamePresentationProjector.legacies` takes the `LineageState` instead of
  its id, so a boxed lineage's legacies are read from its baseline record.
  (B) Add `CampaignState.standingLineages` to the model and switch
  `ChronicleCrown.roles` to it as well: one rule in one place, but it
  touches gameplay code the spec does not name. Affects Task 3.
- **Q2. Naming the Recent site's cards outside the overlay.**
  `identifiesCard`'s doc says atlas sites are never identified, and that
  the world projection and it must be tightened "together or neither".
  The rules make the front Recent site's denizens and edifices public, not
  its relics (NF rulebook, "Public and Private Information": "In the Atlas
  box, all information is private except the first site on the Recent
  Sites side and denizens and edifices stored with it."). (A, recommended)
  Add an arm naming `AtlasSite(_, site, Denizens)` cards to every viewer
  while `site` is the Recent site, so the log (`identifiesAt`) and the
  overlay agree. (B) Leave `identifiesCard` alone and note the overlay as
  an exception in its doc. Affects Task 2, Steps 1 and 4.
- **Q3. The side a stored edifice shows.** `AtlasEntry.StoredSite.edifice`
  carries no side; `CardIndex` notes "it has no side until the site is
  placed". (A, recommended) Show it intact: Archive Rival Sites stores
  "intact edifices", and Empire setup returns them "on their intact side"
  (NF rulebook). It goes after the stored denizens, the position
  `CardIndex` gives it. (B) Show it with no side (a card with `side`
  absent). Affects Task 2.
- **Q4. The case class default.** On the wire the field is required.
  (A, recommended) In Scala, `chronicleDetails` is the last
  `GameProjection` parameter with `= None`, as `foundations` is required on
  the wire and defaulted in Scala; the seven constructor calls in tests and
  the `NoGame` projection compile unchanged. (B) No Scala default: every
  constructor call names the field. Affects Task 1, Step 3.

## Deviations already made in this plan

- `ChronicleEntryCodec`'s inline scorepad mark codec is extracted to
  `encodeScorepadEntry` and `decodeScorepadEntry`, and `knownRole` is made
  public, so the new codec reuses them. The `ChronicleEntry` wire is
  unchanged.
- `ChronicleEntries.roleName` is made public and a `scorepad(era)` helper is
  extracted from `summary`, so the projection maps roles and marks as the
  Chronicle page does.
- `WorldProjectionCodec.encodeSite` and `decodeSite` lose `private`.
- `GamePresentationProjector.siteProjection` is split: `siteCard` maps one
  site card, `siteShape` adds the catalog facts. The map site projection
  is unchanged.
- "PlayerColor order" for a lineage with no colour: a seated lineage takes
  its seat's colour, a boxed one the colour its id names
  (`FirstGameBootstrapMapper.lineageOf`); a lineage with neither comes
  last, by id. Test tables use lineage ids such as `l1`, so the seat's
  colour is what orders them.

## Files

- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsDtos.scala`
- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`
- Create: `shared/src/test/scala/distantlands/protocol/ChronicleDetailsProtocolSuite.scala`
- Modify: `frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala`
  (its hand-written snapshot JSON gains `"chronicleDetails":null`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
- Modify: `src/main/scala/distantlands/application/GameProjection.scala`
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
- Create: `src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala`
- Modify docs: the spec's status line,
  `docs/rules/implementation-traceability.md` (the visibility row's suite
  list).

## Running things

- Shared sources build in the root project, so the root runs their suites.
- One suite: `./sbtw "testOnly distantlands.protocol.ChronicleDetailsProtocolSuite"`.
- Server suites: `./sbtw "testOnly distantlands.application.ChronicleDetailsProjectionSuite distantlands.application.LegacyIdentitySuite distantlands.application.ChronicleProjectionSuite"`.
- Frontend suite: `./sbtw "frontend/testOnly distantlands.frontend.HttpGameClientSuite"`.
- Full gate at slice end, long ones in the background: `./sbtw compile`,
  `./sbtw test`, `./sbtw frontend/test`, `./sbtw frontend/fullOptJS`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory below was read at `5c643a5f8`. Each task begins by running
its grep at HEAD. Treat what the grep shows as the list; handle a hit
missing from the task by the task's rule, and report it.

---

### Task 1: The wire types and their codec

**Files:**
- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsDtos.scala`
- Create: `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala` (last parameter)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala` (`Fields`, `encodeValue`, `decodeValue`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala` (`encodeSite`, `decodeSite`)
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala` (`ChronicleEntryCodec`)
- Modify: `frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala` (snapshot JSON)
- Test: `shared/src/test/scala/distantlands/protocol/ChronicleDetailsProtocolSuite.scala`

**Interfaces:**
- Produces, in package `distantlands.protocol.projection`:
  `final case class ChronicleDetailsProjection(eraTarget: Int, scorepad: Vector[Vector[ChronicleScorepadEntry]], lineages: Vector[LineageDetailsProjection], atlasRecent: Option[SetupSiteProjection])`;
  `final case class LineageDetailsProjection(lineage: String, score: Int, role: Option[String], legacies: Vector[CardDetailsProjection])`;
  `GameProjection.chronicleDetails: Option[ChronicleDetailsProjection] = None`
  (last parameter).
- Produces, in `distantlands.protocol.ChronicleEntryCodec`:
  `def encodeScorepadEntry(mark: ChronicleScorepadEntry): ujson.Value`,
  `def decodeScorepadEntry(raw: ujson.Value, path: String): Result[ChronicleScorepadEntry]`,
  `def knownRole(raw: ujson.Value, at: String): Result[String]`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E '"foundations":\[\]|"completed":' -- src/test shared/src/test frontend/src/test
```
Expected: `HttpGameClientSuite.scala` (the snapshot JSON, lines 215-234)
and `TrustedGameProtocolSuite.scala:109` (a Chronicle summary, not a
snapshot; leave it). Any other hand-written snapshot JSON gains
`"chronicleDetails":null` in Step 5.

- [ ] **Step 2: Write the failing wire tests**

Create `shared/src/test/scala/distantlands/protocol/ChronicleDetailsProtocolSuite.scala`:

```scala
package distantlands.protocol

import distantlands.protocol.projection._
import distantlands.testkit.Unwrap

/** The Chronicle Details wire (Chronicle Details design, slice 1): every
  * key is required; a role and the Atlas's Recent site are null when
  * absent. */
class ChronicleDetailsProtocolSuite extends munit.FunSuite:
  private val faceupLegacy = CardDetailsProjection("L22", "legacy", "Seer",
    orientation = Some("face-up"))
  private val legacyBack = CardDetailsProjection("hidden", "legacy",
    "Facedown legacy", orientation = Some("face-down"), hidden = true)
  private val relicBack = CardDetailsProjection("hidden", "relic",
    "Facedown relic", orientation = Some("face-down"), hidden = true)
  private val deepWoodsWithADenizenAndARelic = SetupSiteProjection(
    "site:deep-woods", "Deep Woods", 0, 0, 3, 1,
    Vector(SiteCardProjection("208", "Wizard's Conclave",
      Some(CardDetailsProjection("208", "denizen", "Wizard's Conclave",
        orientation = Some("face-up"))))),
    SiteRelicsProjection(Vector(relicBack)))

  /** Two games on the scorepad, a Chancellor and an Exile, and a Recent
    * site with a denizen and a relic. */
  private val detailsAfterTwoGames = ChronicleDetailsProjection(20,
    Vector(
      Vector(ChronicleScorepadEntry("red-lineage", 3, Some("chancellor")),
        ChronicleScorepadEntry("blue-lineage", 2)),
      Vector(ChronicleScorepadEntry("red-lineage", 5, Some("citizen")),
        ChronicleScorepadEntry("blue-lineage", 6))),
    Vector(
      LineageDetailsProjection("red-lineage", 5, Some("citizen"),
        Vector(faceupLegacy, legacyBack)),
      LineageDetailsProjection("blue-lineage", 6, None, Vector.empty)),
    Some(deepWoodsWithADenizenAndARelic))

  private def snapshotWith(details: Option[ChronicleDetailsProjection])
      : GameProjection =
    GameProjection("game-1", 7, "act", Some("red"), Vector.empty,
      Vector.empty, Vector.empty, ready = true, completed = false,
      chronicleDetails = details)

  private def assertRoundTrips(candidate: GameProjection)(
      using munit.Location): Unit =
    assertEquals(
      GameProjectionCodec.decode(GameProjectionCodec.encode(candidate)),
      Right(candidate))

  /** The encoded snapshot with `edit` applied, decoded. */
  private def decodedAfter(candidate: GameProjection)(
      edit: ujson.Value => Unit) =
    val raw = ujson.read(GameProjectionCodec.encode(candidate))
    edit(raw)
    GameProjectionCodec.decode(ujson.write(raw))

  test("Chronicle Details round-trips with its scorepad, its lineages and " +
      "the Recent site"):
    assertRoundTrips(snapshotWith(Some(detailsAfterTwoGames)))

  test("an empty Atlas, an empty scorepad and a snapshot before a game " +
      "round-trip"):
    assertRoundTrips(snapshotWith(Some(detailsAfterTwoGames.copy(
      scorepad = Vector.empty, atlasRecent = None))))
    assertRoundTrips(snapshotWith(None))

  test("a snapshot without chronicleDetails is refused"):
    val failure = Unwrap.rejected(decodedAfter(snapshotWith(
      Some(detailsAfterTwoGames)))(_.obj.remove("chronicleDetails")))
    assertEquals(failure.path, "$.chronicleDetails")

  test("a lineage without its role key, or a Chronicle Details without " +
      "its Recent site key, is refused"):
    val snapshot = snapshotWith(Some(detailsAfterTwoGames))
    assertEquals(Unwrap.rejected(decodedAfter(snapshot)(
      _("chronicleDetails")("lineages")(1).obj.remove("role"))).path,
      "$.chronicleDetails.lineages[1].role")
    assertEquals(Unwrap.rejected(decodedAfter(snapshot)(
      _("chronicleDetails").obj.remove("atlasRecent"))).path,
      "$.chronicleDetails.atlasRecent")

  test("a lineage role other than chancellor or citizen is refused"):
    val failure = Unwrap.rejected(decodedAfter(snapshotWith(
      Some(detailsAfterTwoGames)))(raw =>
      raw("chronicleDetails")("lineages")(0)("role") = "exile"))
    assertEquals(failure.path, "$.chronicleDetails.lineages[0].role")
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleDetailsProtocolSuite"`
Expected: compilation fails with "Not found: type
ChronicleDetailsProjection".

- [ ] **Step 4: Add the types**

Create `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsDtos.scala`:

```scala
package distantlands.protocol.projection

import distantlands.protocol.ChronicleScorepadEntry

/** The Chronicle as the table sees it mid-game (Chronicle Details design,
  * slice 1). */
final case class ChronicleDetailsProjection(
    eraTarget: Int,
    /** A column per game played, in order, as ChronicleSummary keeps it. */
    scorepad: Vector[Vector[ChronicleScorepadEntry]],
    /** Every lineage in the Chronicle, seated or not, in PlayerColor order. */
    lineages: Vector[LineageDetailsProjection],
    /** The Atlas's Recent site; None once the Atlas holds no site. */
    atlasRecent: Option[SetupSiteProjection])

/** One lineage: its current era total, its Imperial role (`"chancellor"` or
  * `"citizen"`, absent for an Exile), and its legacies in held order as the
  * viewer may see them (Chronicle Details design, ruling D1). */
final case class LineageDetailsProjection(lineage: String, score: Int,
    role: Option[String], legacies: Vector[CardDetailsProjection])
```

In `GameProjectionDto.scala`, after the `publicPreview` parameter:

```scala
    /** The Chronicle Details overlay's data (Chronicle Details design,
      * slice 1); none before a game. Required on the wire. */
    ,chronicleDetails: Option[ChronicleDetailsProjection] = None
```

- [ ] **Step 5: Add the codec**

In `ChronicleProtocol.scala`, inside `ChronicleEntryCodec`, add:

```scala
  /** One mark on the era scorepad; its role is omitted for an Exile. */
  def encodeScorepadEntry(mark: ChronicleScorepadEntry): ujson.Value =
    val row = ujson.Obj("lineage" -> mark.lineage, "score" -> mark.score)
    mark.role.foreach(role => row("role") = role)
    row

  def decodeScorepadEntry(raw: ujson.Value, path: String)
      : Result[ChronicleScorepadEntry] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("lineage", "score", "role"), path)
    lineage <- string(row, "lineage", path)
    score <- int(row, "score", path)
    role <- optionalAbsent(row, "role", path)(knownRole)
  yield ChronicleScorepadEntry(lineage, score, role)
```

Replace the inline mark encoder in `encodeValue` with
`"scorepad" -> encoded(entry.summary.scorepad)(column =>
encoded(column)(encodeScorepadEntry))`, and the inline mark decoder in
`decodeFields` with
`array(raw, column).flatMap(markRaws => traverse(markRaws, column)(decodeScorepadEntry))`.
Make `knownRole` public (drop `private`) and give it the doc comment
`/** An Imperial role as the wire spells it: chancellor or citizen. */`.

In `WorldProjectionCodec.scala`, drop `private` from `encodeSite` and
`decodeSite`.

Create `shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsCodec.scala`:

```scala
package distantlands.protocol.projection

import distantlands.protocol.ChronicleEntryCodec
import ProjectionCodecSupport._

/** The wire of [[ChronicleDetailsProjection]] (Chronicle Details design,
  * slice 1). Every key is required; a lineage's `role` and `atlasRecent`
  * are null when absent. Scorepad marks keep the Chronicle summary's
  * wire. */
private[projection] object ChronicleDetailsCodec:
  private val Fields = Set("eraTarget", "scorepad", "lineages", "atlasRecent")
  private val LineageFields = Set("lineage", "score", "role", "legacies")

  def encode(value: ChronicleDetailsProjection): ujson.Value = ujson.Obj(
    "eraTarget" -> value.eraTarget,
    "scorepad" -> encoded(value.scorepad)(column =>
      encoded(column)(ChronicleEntryCodec.encodeScorepadEntry)),
    "lineages" -> encoded(value.lineages)(lineage => ujson.Obj(
      "lineage" -> lineage.lineage, "score" -> lineage.score,
      "role" -> stringOption(lineage.role),
      "legacies" -> encoded(lineage.legacies)(
        WorldProjectionCodec.encodeCard))),
    "atlasRecent" -> option(value.atlasRecent)(
      WorldProjectionCodec.encodeSite))

  def decode(raw: ujson.Value, path: String)
      : Result[ChronicleDetailsProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Fields, path)
    target <- int(row, "eraTarget", path)
    columnRaws <- array(row, "scorepad", path)
    scorepad <- traverse(columnRaws, s"$path.scorepad") { (raw, column) =>
      array(raw, column).flatMap(marks =>
        traverse(marks, column)(ChronicleEntryCodec.decodeScorepadEntry)) }
    lineageRaws <- array(row, "lineages", path)
    lineages <- traverse(lineageRaws, s"$path.lineages")(decodeLineage)
    recent <- optional(row, "atlasRecent", path)(
      WorldProjectionCodec.decodeSite)
  yield ChronicleDetailsProjection(target, scorepad, lineages, recent)

  private def decodeLineage(raw: ujson.Value, path: String)
      : Result[LineageDetailsProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, LineageFields, path)
    lineage <- string(row, "lineage", path)
    score <- int(row, "score", path)
    role <- optional(row, "role", path)(ChronicleEntryCodec.knownRole)
    legacyRaws <- array(row, "legacies", path)
    legacies <- traverse(legacyRaws, s"$path.legacies")(
      WorldProjectionCodec.decodeCard)
  yield LineageDetailsProjection(lineage, score, role, legacies)
```

In `GameProjectionCodec.scala`:
- Add `"chronicleDetails"` to `Fields` after `"publicPreview"`.
- In `encodeValue`, after the `publicPreview` pair:
  `"chronicleDetails" -> option(value.chronicleDetails)(ChronicleDetailsCodec.encode)`.
- In `decodeValue`, after `publicPreview <- ...`:
  ```scala
    // Required: a snapshot without it is refused (Chronicle Details
    // design, slice 1).
    chronicleDetails <- optional(value, "chronicleDetails", path)(
      ChronicleDetailsCodec.decode)
  ```
  and end the `yield` with `publicLegacies, reliquary, publicPreview,
  chronicleDetails)`.

In `HttpGameClientSuite.scala`, change the snapshot JSON's last line
`|"foundations":[]` to `|"foundations":[],` followed by
`|"chronicleDetails":null`. Do the same for any other snapshot JSON Step 1
found.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.protocol.ChronicleDetailsProtocolSuite distantlands.protocol.ProjectionProtocolSuite distantlands.protocol.ChronicleProtocolSuite distantlands.protocol.TrustedGameProtocolSuite" "frontend/testOnly distantlands.frontend.HttpGameClientSuite"`
Expected: all pass. The Chronicle entry suites prove the extracted mark
codec kept its wire.

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/ChronicleDetailsCodec.scala \
  shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala \
  shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala \
  shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala \
  shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala \
  shared/src/test/scala/distantlands/protocol/ChronicleDetailsProtocolSuite.scala \
  frontend/src/test/scala/distantlands/frontend/HttpGameClientSuite.scala
git commit -m "feat(protocol): carry Chronicle Details on the snapshot

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The Atlas's Recent site as every viewer sees it

**Files:**
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`siteProjection` split into `siteCard` and `siteShape`; new
  `atlasSite`; `identifiesCard` gains an `AtlasSite` arm and its doc
  changes)
- Test: `src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala`

**Interfaces:**
- Consumes: `SetupSiteProjection`, `SiteCardProjection`,
  `SiteRelicsProjection` (existing).
- Produces: `GamePresentationProjector.atlasSite(site: AtlasEntry.StoredSite): SetupSiteProjection`,
  `private[application]`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'atlas sites|AtlasSite\(' -- src/main/scala/distantlands/application src/test/scala/distantlands/application
```
Expected: the "Everything else" bullet of `identifiesCard`'s doc
("other decks, set-aside relics, the dispossessed pile, suited reserves,
atlas sites") and no other hit. A hit in the game log projector means a
log line reads atlas cards; report it before Step 4.

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.cards.{DeepWoodsSite, DesolateShoreSite}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.present

/** The Chronicle as the table sees it mid-game (Chronicle Details design,
  * slice 1): the era scorepad, every lineage with the legacies its viewer
  * may see (ruling D1), and the Atlas's Recent site. */
class ChronicleDetailsProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val viewers = Vector(Some(p1), Some(p2), None)

  /** The quiet map cut to three sites, Deep Woods at the Atlas's Recent
    * end storing the Great Market, Wizard's Conclave and Sticky Fire, and
    * Desolate Shore behind it storing Threatening Roar. */
  private def recentDeepWoodsStoringAnEdificeADenizenAndARelic: ReadyGame =
    Table.start.mapCutIntoAtlas(3)
      .storedInAtlas(DeepWoodsSite, GreatMarketCard, WizardsConclaveCard,
        StickyFireCard)
      .storedInAtlas(DesolateShoreSite, ThreateningRoarCard).ready

  test("the Recent site shows its denizens faceup, its edifice intact " +
      "after them, its relics facedown and hidden, and no loose " +
      "resources or warbands"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    val site = presentation.atlasSite(present(
      ready.game.campaign.atlas.site(AtlasEnd.Recent), "the Recent site"))
    assertEquals((site.siteId, site.denizenCapacity, site.relicCapacity),
      (DeepWoodsSite.id.value, DeepWoodsSite.capacity,
        DeepWoodsSite.relicSlots))
    assertEquals(site.denizens.map(card => (card.denizenId,
      card.details.flatMap(_.orientation), card.details.flatMap(_.side))),
      Vector((WizardsConclaveCard.id.value, Some("face-up"), None),
        (GreatMarketCard.id.value, None, Some("intact"))))
    assertEquals(site.relics.relics.map(relic => (relic.cardId, relic.hidden)),
      Vector(("hidden", true)))
    assertEquals((site.looseFavor, site.looseSecrets, site.forces,
      site.festival), (0, 0, None, false))

  test("every viewer identifies the Recent site's denizens and edifice, " +
      "and none its relics or another Atlas site's cards"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    viewers.foreach { viewer =>
      assert(presentation.identifiesAt(ready, viewer,
        WizardsConclaveCard.id), viewer)
      assert(presentation.identifiesAt(ready, viewer, GreatMarketCard.id),
        viewer)
      assert(!presentation.identifiesAt(ready, viewer, StickyFireCard.id),
        viewer)
      assert(!presentation.identifiesAt(ready, viewer,
        ThreateningRoarCard.id), viewer)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.ChronicleDetailsProjectionSuite"`
Expected: compilation fails with "value atlasSite is not a member of
distantlands.application.GamePresentationProjector".

- [ ] **Step 4: Write `atlasSite` and the `identifiesCard` arm**

In `GamePresentationProjector.scala`, replace `siteProjection` with:

```scala
  private def siteProjection(siteId: SiteId, state: Option[SiteState],
      ready: ReadyGame, viewer: Option[PlayerId]): SetupSiteProjection =
    siteShape(siteId, state.fold(Tokens.empty)(_.tokens),
      state.toVector.flatMap(_.denizens).map(siteCard),
      state.toVector.flatMap(_.relics).map(relic =>
        val known = viewer.exists(player =>
          ready.knowledge.siteRelics.getOrElse(player, Map.empty)
            .getOrElse(siteId, Vector.empty).contains(relic.id))
        if known then
          cardDetails(relic.id, Some(Orientation.FaceDown), hidden = false)
        else hiddenCard("relic")),
      state.flatMap(_.forces match {
        case value: SiteForces.Occupied => Some(forceProjection(value, ready))
        case SiteForces.Empty => None
      }),
      ready.game.current.banners.darkestSecret.festival.contains(siteId))

  /** The Atlas's stored `site` as every viewer sees it (Chronicle Details
    * design, slice 1; NF rulebook, "Public and Private Information"): its
    * denizens faceup and named, its edifice on its intact side after them,
    * where `CardIndex` lays it, and each relic facedown and hidden. A
    * stored site holds no loose favor or secrets, no warbands and no
    * festival marker. */
  private[application] def atlasSite(site: AtlasEntry.StoredSite)
      : SetupSiteProjection =
    val cards = site.denizens.map {
      case denizen: DenizenState =>
        denizen.copy(orientation = Orientation.FaceUp)
      case edifice: EdificeState => edifice
    } ++ site.edifice.map(EdificeState(_, EdificeSide.Intact, Tokens.empty))
    siteShape(site.id, Tokens.empty, cards.map(siteCard),
      site.relics.map(_ => hiddenCard("relic")), forces = None,
      festival = false)

  /** One card in a site's denizen slots, named, with its tokens. */
  private def siteCard(card: SiteDenizenState): SiteCardProjection =
    card match
      case value: DenizenState => SiteCardProjection(value.id.value,
        denizenLabel(value.id), Some(cardDetails(value.id,
          Some(value.orientation), hidden = false).copy(
            favor = value.tokens.favor, secrets = value.tokens.secrets)))
      case value: EdificeState => SiteCardProjection(value.id.value,
        edificeLabel(value.id, value.side), Some(edificeCardDetails(value)))

  /** A site's projection: `cards`, `relics`, `forces` and `festival` as
    * given, with its printed facts from the catalog. */
  private def siteShape(siteId: SiteId, loose: Tokens,
      cards: Vector[SiteCardProjection], relics: Vector[CardDetailsProjection],
      forces: Option[SiteForcesProjection], festival: Boolean)
      : SetupSiteProjection =
    val definition = catalog.site(siteId)
    SetupSiteProjection(siteId.value, siteLabel(siteId), loose.favor,
      loose.secrets, definition.fold(0)(_.capacity),
      definition.fold(0)(_.relicSlots), cards, SiteRelicsProjection(relics),
      definition.fold(0)(_.defense),
      definition.flatMap(site => Option.when(site.forgeRequirements.isEmpty)(
        site.recoverDifficulty).flatten),
      definition.flatMap(_.forgeRequirements).map(tokens =>
        ForgeCostProjection(tokens.favor, tokens.secrets)),
      definition.toVector.flatMap(_.handlers).map(sitePower),
      forces, festival)
```

In `identifiesCard`, add before `case _ => false`:

```scala
      // The Atlas's front site on its Recent side, with the denizens and
      // edifices stored with it, is public; its relics, and every other
      // Atlas site's cards, are not (NF rulebook, "Public and Private
      // Information"; Chronicle Details design, slice 1).
      case CardContainer.AtlasSite(_, site, SiteCardArea.Denizens) =>
        ready.game.campaign.atlas.site(AtlasEnd.Recent).exists(_.id == site)
```

In its doc comment, add the bullet

```scala
    *  - The Atlas's Recent site's denizens and edifices are named to every
    *    viewer, as the rulebook makes them public; its relics are not.
```

before the "Everything else" bullet, and in that bullet replace "atlas
sites" with "the other atlas sites, every atlas relic".

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.ChronicleDetailsProjectionSuite distantlands.application.GamePresentationProjectorMapSuite distantlands.application.ExploreGameSuite distantlands.application.gamelog.*"`
Expected: all pass. The map suite proves the split kept the map site's
projection; the log suites prove no golden line changed.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala
git commit -m "feat(projection): show the Atlas's Recent site as the table sees it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Build `chronicleDetails` on every snapshot

**Files:**
- Modify: `src/main/scala/distantlands/application/GameProjection.scala`
  (`readyProjection`, new `chronicleDetails` and `standingLineages`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`legacies` takes a `LineageState`, `private[application]`)
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
  (`scorepad`, public `roleName`)
- Test: `src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala`

**Interfaces:**
- Consumes: `ChronicleDetailsProjection`, `LineageDetailsProjection`
  (Task 1); `GamePresentationProjector.atlasSite` (Task 2).
- Produces: `GamePresentationProjector.legacies(ready: ReadyGame, viewer: Option[PlayerId], lineage: LineageState): Vector[CardDetailsProjection]`;
  `ChronicleEntries.scorepad(era: EraState): Vector[Vector[ChronicleScorepadEntry]]`;
  `ChronicleEntries.roleName(role: Role): Option[String]`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'legacies\(ready, viewer|roleName|ChronicleScorepadEntry\(' -- src/main
```
Expected: `GamePresentationProjector.scala` (`playerBoards`' call) and
`ChronicleEntries.scala` (`summary` and `roleName`).

- [ ] **Step 2: Write the failing tests**

In `ChronicleDetailsProjectionSuite.scala`, add the imports:

```scala
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  GameProjectionCodec, LineageDetailsProjection}
```

widen `Unwrap.present` to `Unwrap.{accepted, present}` and `Table.{p1,
p2}` to `Table.{p1, p2, p3}`. Then add to the class:

```scala
  private val projector = new GameProjector(catalog)
  private val purple = LineageId("purple-lineage")

  private def detailsFor(ready: ReadyGame, viewer: Option[PlayerId])(
      using munit.Location): ChronicleDetailsProjection =
    val loaded = LoadedGame(Ready(ready), 1L)
    present(viewer.fold(projector.projectPublic("g", loaded))(
      projector.project("g", loaded, _)).chronicleDetails,
      "the Chronicle Details")

  private def lineageRow(ready: ReadyGame, viewer: Option[PlayerId],
      lineage: LineageId)(using munit.Location): LineageDetailsProjection =
    present(detailsFor(ready, viewer).lineages.find(_.lineage ==
      lineage.value), s"the row of ${lineage.value}")

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  /** `ready` with a purple lineage nobody is seated with in the Chronicle
    * the game started from: a Citizen there whom this game made an Exile
    * in the box, carrying 7 points from a TTS import, holding Iron Hand
    * active and Arbiter dormant, both taken out of the legacy deck. */
  private def withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      ready: ReadyGame): ReadyGame =
    val held = Vector(LegacyState(IronHandCard.id, active = true),
      LegacyState(ArbiterCard.id, active = false))
    ready.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages =
        campaign.baseline.lineages :+ LineageState(purple, None,
          Role.Citizen, held, Vector.empty)),
      boxRoles = campaign.boxRoles.updated(purple, Role.Exile),
      era = campaign.era.copy(carriedScores =
        campaign.era.carriedScores.updated(purple, 7))))
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(legacyDeck = current.commonCards.legacyDeck
          .filterNot(id => held.exists(_.id == id)))))

  test("a game not yet started has no Chronicle Details"):
    assertEquals(projector.project("g", LoadedGame(OathState.NoGame, 0L),
      p1).chronicleDetails, None)

  test("a dormant legacy is its face to its lineage's player and a back " +
      "to another seat; an active one is faceup to all"):
    val ready = Table.start.legacy(p1, SeerCard)
      .legacy(p1, ArbiterCard, active = false).ready
    val l1 = lineageOf(ready, p1)
    assertEquals(lineageRow(ready, Some(p1), l1).legacies.map(card =>
      (card.cardId, card.orientation, card.hidden)), Vector(
        (SeerCard.id.value, Some("face-up"), false),
        (ArbiterCard.id.value, Some("face-down"), false)))
    Vector(Some(p2), None).foreach(viewer =>
      assertEquals(lineageRow(ready, viewer, l1).legacies.map(card =>
        (card.cardId, card.hidden)), Vector((SeerCard.id.value, false),
          ("hidden", true)), viewer))

  test("an unseated lineage is listed first in PlayerColor order, with its " +
      "role in the box and its carried score, and its dormant legacy is a " +
      "back to every seat"):
    val ready = withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      Table.start.ready)
    // Purple, then the seats' colours: l1 Red, l2 Blue, l3 Yellow.
    assertEquals(detailsFor(ready, Some(p1)).lineages.map(_.lineage),
      Vector(purple, lineageOf(ready, p1), lineageOf(ready, p2),
        lineageOf(ready, p3)).map(_.value))
    viewers.foreach { viewer =>
      val row = lineageRow(ready, viewer, purple)
      assertEquals((row.score, row.role), (7, None), viewer)
      assertEquals(row.legacies.map(card => (card.cardId, card.hidden)),
        Vector((IronHandCard.id.value, false), ("hidden", true)), viewer)
    }

  test("in the Chronicle Phase every seat sees every legacy's face, " +
      "seated or not (R18)"):
    val ready = withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      Table.start.legacy(p1, SeerCard, active = false)
        .turn(p1, Phase.Chronicle).ready)
    viewers.foreach(viewer =>
      assert(detailsFor(ready, viewer).lineages.flatMap(_.legacies)
        .forall(!_.hidden), viewer))

  test("Score Lineage Points adds this game's column to the scorepad, and " +
      "each lineage's score follows it"):
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.reversingShuffle)
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
    val before = detailsFor(table.ready, Some(p1))
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val after = detailsFor(run.ready, Some(p1))
    assertEquals(after.scorepad.size, before.scorepad.size + 1)
    val era = run.ready.game.campaign.era
    assertEquals(after.lineages.map(row => row.lineage -> row.score),
      after.lineages.map(row => row.lineage ->
        era.score(LineageId(row.lineage))))
    assertEquals(after.eraTarget, era.targetScore)

  test("the Recent site changes after an Explore takes it"):
    import DefenseDieFace.TwoShields
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.scripted(Vector(TwoShields,
        TwoShields)))
    // Deep Woods at the Recent end, then Desolate Shore.
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled.ready
    assertEquals(detailsFor(ready, Some(p1)).atlasRecent.map(_.siteId),
      Some(DeepWoodsSite.id.value))
    val rolled = accepted(rules.startWalker(Ready(ready), ActionRef.Explore,
      p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    val explored = distantlands.testkit.Outcomes.ready(placed.state)
    assertEquals(detailsFor(explored, Some(p1)).atlasRecent.map(_.siteId),
      Some(DesolateShoreSite.id.value))

  test("the Atlas's Recent site is none once the Atlas holds no site"):
    val ready = Table.start.ready.updateCampaign(_.copy(atlas =
      AtlasState(Vector(AtlasEntry.EmpireDivider))))
    viewers.foreach(viewer =>
      assertEquals(detailsFor(ready, viewer).atlasRecent, None, viewer))

  test("a relic stored at the Recent site is hidden, and no seat's JSON " +
      "names it"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    val loaded = LoadedGame(Ready(ready), 1L)
    Vector(projector.project("g", loaded, p1),
        projector.project("g", loaded, p2),
        projector.projectPublic("g", loaded)).foreach { view =>
      assertEquals(view.chronicleDetails.flatMap(_.atlasRecent)
        .map(_.relics.relics.map(_.hidden)), Some(Vector(true)))
      assert(!GameProjectionCodec.encode(view)
        .contains(s"\"${StickyFireCard.id.value}\""))
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.ChronicleDetailsProjectionSuite"`
Expected: the two Task 2 tests and "a game not yet started" pass; every
other test fails with "the Chronicle Details" absent.

- [ ] **Step 4: Expose the role and scorepad mapping**

In `ChronicleEntries.scala`, in `object ChronicleEntries`, replace the
`summary`'s last argument with `scorepad(era))` and add:

```scala
  /** The era scorepad as a summary lists it: a column per game played, in
    * order, each mark its lineage's total and role at that game's end. */
  def scorepad(era: EraState): Vector[Vector[ChronicleScorepadEntry]] =
    era.games.map(_.entries.map(entry => ChronicleScorepadEntry(
      entry.lineage.value, entry.totalScore, roleName(entry.endingRole))))
```

Drop `private` from `roleName`. Import `EraState` from
`distantlands.model`.

- [ ] **Step 5: Let `legacies` read any lineage**

In `GamePresentationProjector.scala`, replace `legacies` with:

```scala
  /** `lineage`'s legacies as `viewer` may see them (Legacies design, ruling
    * 1; implementation rulings R18 and R36): a face where `identifiesCard`
    * names the legacy, a legacy back elsewhere. `lineage` is a seated
    * lineage's live record or an unseated one's in the Chronicle the game
    * started from (Chronicle Details design, slice 1). */
  private[application] def legacies(ready: ReadyGame,
      viewer: Option[PlayerId], lineage: LineageState)
      : Vector[CardDetailsProjection] =
    lineage.legacies.map { legacy =>
      val face = GamePresentationProjector.orientationOf(Some(legacy))
      if identifiesCard(ready, viewer, legacy.id, face,
          CardContainer.Lineage(lineage.id, LineageCardArea.Legacies)) then
        cardDetails(legacy.id, face, hidden = false)
      else hiddenCard("legacy")
    }
```

and in `playerBoards` replace `legacies(ready, viewer, player.lineage))`
with:

```scala
        ready.game.campaign.lineages.get(player.lineage).toVector
          .flatMap(legacies(ready, viewer, _)))
```

- [ ] **Step 6: Build the projection**

In `GameProjection.scala` (package `distantlands.application`, which
already holds `FirstGameBootstrapMapper` and imports `ChronicleEntries`
and `distantlands.model._`, so no import is added), in
`readyProjection`'s `.copy(...)`, after
`chronicle = Some(chronicleView(context.ready.game.campaign)),` add
`chronicleDetails = Some(chronicleDetails(context)),`. After
`chronicleView`, add:

```scala
  /** The Chronicle as the table sees it mid-game (Chronicle Details design,
    * slice 1): the live era scorepad, which Score Lineage Points extends in
    * the Chronicle Phase; every lineage, seated or not, with its total
    * (carried TTS scores included), its role and the legacies `context`'s
    * viewer may see (ruling D1); and the Atlas's Recent site. */
  private def chronicleDetails(context: ScopedProjectionContext)
      : ChronicleDetailsProjection =
    val ready = context.ready
    val campaign = ready.game.campaign
    ChronicleDetailsProjection(campaign.era.targetScore,
      ChronicleEntries.scorepad(campaign.era),
      standingLineages(ready).map(lineage => LineageDetailsProjection(
        lineage.id.value, campaign.era.score(lineage.id),
        ChronicleEntries.roleName(lineage.role),
        presentation.legacies(ready, context.viewer, lineage))),
      campaign.atlas.site(AtlasEnd.Recent).map(presentation.atlasSite))

  /** Every lineage in the Chronicle as it stands, in PlayerColor order. A
    * seated lineage is its live record; an unseated one is its record in
    * the Chronicle the game started from, with the role this game gave it
    * in the box, as Crown Chancellor reads it (`ChronicleCrown.roles`). A
    * lineage takes its seat's colour, else the colour its id names; one
    * with neither comes last, by id. */
  private def standingLineages(ready: ReadyGame): Vector[LineageState] =
    val campaign = ready.game.campaign
    val boxed = campaign.baseline.lineages
      .filterNot(lineage => campaign.lineages.contains(lineage.id))
      .map(lineage => lineage.copy(role =
        campaign.boxRoles.getOrElse(lineage.id, lineage.role)))
    val seatColors = ready.game.current.players.flatMap(seat =>
      ready.playerColors.get(seat.player).map(seat.lineage -> _)).toMap
    def colorIndex(lineage: LineageId): Int = seatColors.get(lineage)
      .orElse(PlayerColor.all.find(color =>
        FirstGameBootstrapMapper.lineageOf(color) == lineage))
      .fold(PlayerColor.all.size)(PlayerColor.all.indexOf)
    (campaign.lineages.values.toVector ++ boxed).sortBy(lineage =>
      (colorIndex(lineage.id), lineage.id.value))
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.ChronicleDetailsProjectionSuite distantlands.application.LegacyIdentitySuite distantlands.application.ChronicleProjectionSuite distantlands.application.chronicle.* distantlands.application.HiddenChronicleCardsSuite distantlands.application.LegacyActionSecrecySuite distantlands.application.ReliquaryViewSuite"`
Expected: all pass. The secrecy suites search the whole snapshot JSON for
hidden card ids, so they also cover the new field.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/application/GameProjection.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala \
  src/test/scala/distantlands/application/ChronicleDetailsProjectionSuite.scala
git commit -m "feat(projection): project Chronicle Details on every snapshot

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Docs and the full gate

**Files:**
- Modify: `docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md` (status line)
- Modify: `docs/rules/implementation-traceability.md` (the visibility row)

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'No slice has landed|HiddenChronicleCardsSuite' -- docs ':!docs/superpowers/plans'
```
Expected: the spec's status line and the traceability row
"Public/private visibility, owner peek/share, ...".

- [ ] **Step 2: Update the docs**

- Spec status line: `**Status:** designed 2026-11-18. Slice 1
  (projection) has landed.`
- Traceability row: in the list of suites, after
  `` `HiddenChronicleCardsSuite`, `` add
  `` `ChronicleDetailsProjectionSuite`, `` and after "relic peeks," add
  "the Atlas's Recent site (its relics hidden),".

- [ ] **Step 3: Run the full gate**

Run, the long ones in the background:
`./sbtw compile`, `./sbtw test`, `./sbtw frontend/test`,
`./sbtw frontend/fullOptJS`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass. A failing golden or secrecy test is a finding: report
it, do not re-record.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-11/2026-11-18-chronicle-details-design.md \
  docs/rules/implementation-traceability.md
git commit -m "docs(chronicle): record Chronicle Details slice 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

- **Spec coverage.** New types: Task 1. Required wire field, both types
  round-trip, a snapshot without it refused: Task 1. Built beside
  `chronicleView`, present whenever the game has a campaign: Task 3.
  Scorepad, `eraTarget`, `era.score` with carried scores, the live column
  Score Lineage Points adds: Task 3. Role from the campaign: Task 3
  (boxed lineages per Q1). Legacies through the existing `legacies`, no
  new legacy rule: Task 3. Atlas Recent site through the map site's path,
  denizens and edifices named, relics `hiddenCard("relic")`, zero counts:
  Task 2. Projector tests (dormant legacy own/other, unseated back to every
  seat, Chronicle Phase faces, active faceup, scorepad column, Explore,
  empty Atlas, hidden relic): Tasks 2 and 3. Wire tests: Task 1.
- **Placeholder scan.** No step defers code. Each open question names the
  steps a different ruling changes.
- **Type consistency.** `ChronicleDetailsProjection`,
  `LineageDetailsProjection`, `atlasSite`, `legacies(ready, viewer,
  lineage: LineageState)`, `ChronicleEntries.scorepad` and `roleName`
  carry the same names and types in every task that uses them.
