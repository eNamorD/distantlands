# Catalog Batch 10 Slice 5: Live Terrain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Read the Coast and Island site sets from the state, and implement Grand Canal (E03 intact) and Lost Lagoon (E03 ruined) on them.

**Architecture:**
- A new `private[travel] object TravelTerrain` holds the terrain reads:
  - `coastSites(ready)`: the printed Coast sites, plus the site of either E03 face in play.
  - `coastOrIslandSites(ready)`: `coastSites(ready)` plus the printed Island sites.
  - `coastRoute(ready, route)`: the route leaves a Coast for a Coast or Island.
  - `ignoredOnCoastRoute(other)`: the terrain adds a Coast route skips.
- `CoastSitePower` and `NarrowPassSitePower` lose their static set parameters and read `TravelTerrain` from `ctx.state`. `TravelSitePowers` keeps only the printed sets, as `printedCoastSites` and `printedIslandSites`.
- `GrandCanalCard` moves out of holding into a new `GrandCanal.scala`, beside two contributing powers:
  - `GrandCanal` gives the intact face's site the Coast power. It sets the Supply of a Coast route from that site to 1 and ignores the terrain adds.
  - `LostLagoon` gives the ruined face's site the Island power: +2 Supply to arrive. A Coast route into it skips the +2. An Island has the Coast power (ruling 12), so a Coast route from its site also costs 1 Supply.
- Both powers extend a sealed marker trait, `LiveTerrain`. `source` is static and cannot name a site that changes, so their source is `RuleSourceRef.GameRule(id.value)`, as Hallowed Spring's and Boiling Lake's are. Forest Paths ignores any `LiveTerrain` as it ignores site-sourced powers. This replaces the spec's "Each keeps a `RuleSourceRef.Site` source"; Task 2 rewords the spec.
- The sets are recomputed on every read, so a face flip (the Canal ruined mid-game) changes the terrain at once. Nothing is journaled, and nothing new is projected: the edifice's own card text shows the terrain.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Readings" (Grand Canal and Lost Lagoon)
- "Slice 5: Live terrain"
- "Testing"
- "Out of scope"

Card text (`src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`):
- Grand Canal: "Act as if this site is a Coast along with its other powers."
- Lost Lagoon: "Act as if this site is an Island along with its other powers."

Printed terrain (Fair Isle, in the card database):
- "**Island.** This site has the Coast power (see below). Traveling to it costs 2 Supply more than its normal Travel cost unless you're traveling from a Coast or Island."
- "**Coast.** Traveling from here to a Coast or Island costs only 1 Supply and ignores other Travel modifiers _(Mountain, Pass, etc.)_."

**Ruling 12 (owner, 2026-11-05):** Lost Lagoon's site has the Coast power, as every printed Island does. It joins both live sets, so Lagoon-to-Coast costs 1 Supply and counts as a Coast route for Narrow Pass.

This is the fifth of 14 slice plans, written against `0.2.0-alpha` at `53959740`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-5` on branch `batch-10-slice-5`, created from local `0.2.0-alpha`. Never work in the main checkout.
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
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains. Neither power writes a line, so `declared-keys.txt` must not change.
- **Log lines:** none. Terrain changes a Supply amount, which the payment's own line shows, as the printed Coast and Island do.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").
- **Edifices are denizens** (memory): nothing in this slice depends on it, but do not add a card-type check.

---

### Task 1: Live terrain sets and Grand Canal

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/travel/GrandCanal.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TravelSitePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala:1-23`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:80`
- Modify: `src/test/resources/powers/unimplemented.txt` (drops `edifice.e03.intact`)
- Test: `src/test/scala/distantlands/gameplay/powers/travel/GrandCanalSuite.scala`

**Interfaces:**
- Consumes: `EdificeSetupSupport.siteOf(ready, edifice, side): Option[SiteId]`; `TravelRoute.pawnMove`, `TravelRoute.adjustSupply`, `TravelRoute.PawnMove`; `TravelFixture` (`board`, `on`, `candidates`, `travel`, the named sites).
- Produces:
  - `object GrandCanalCard extends Edifice(EdificeId("E03"), Suit.Discord)` in package `distantlands.gameplay.powers.travel`, with `intact.power` and `ruined.power` unchanged.
  - `sealed trait LiveTerrain extends ContributingPower { def side: EdificeSide; final def source; final def site(ready: ReadyGame): Option[SiteId] }`.
  - `case object GrandCanal extends LiveTerrain`.
  - `private[travel] object TravelTerrain { def coastSites(ready: ReadyGame): Set[SiteId]; def coastOrIslandSites(ready: ReadyGame): Set[SiteId]; def coastRoute(ready: ReadyGame, route: TravelRoute.PawnMove): Boolean; def ignoredOnCoastRoute(other: ContributingPower): Boolean }`.
  - `TravelSitePowers.printedCoastSites: Set[SiteId]` and `TravelSitePowers.printedIslandSites: Set[SiteId]`, both `private[travel]`.
  - `CoastSitePower(id: PowerId, site: SiteId)` and `NarrowPassSitePower(id: PowerId, site: SiteId)`.

The Travel board (`TravelFixture`): the Cradle holds `cradlePlains` (Buried Giant) and `coast` (Desolate Shore). The Provinces hold `provincePlains` (Dunes), `mountain` (Broken Peaks) and `pass` (Narrow Pass, with bandits). The Hinterland holds `hinterlandPlains` (Salt Flats), `island` (Fair Isle) and `secondHinterlandPlains` (Shrouded Woods). Printed bases: Cradle to Cradle 1, Cradle to Provinces 2, Cradle to Hinterland 4, Hinterland to Cradle 4, Hinterland to Hinterland 3. The Narrow Pass blocks entering the Provinces from outside, except by a Coast route or for its ruler. The actor has 7 Supply.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/travel/GrandCanalSuite.scala`:

```scala
package distantlands.gameplay.powers.travel

import distantlands.gameplay.powers.PowerFixture
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Outcomes}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Grand Canal and Lost Lagoon (E03, catalog batch 10, slice 5): the site
  * of the face in play also has the Coast power (the Canal) or the Island
  * power, which has the Coast power too (the Lagoon, ruling 12). */
class GrandCanalSuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  private val canal = GrandCanalCard.id

  /** The Travel board with the actor's pawn moved to `from`. The board's
    * own `source` would reorder its regions for a named site. */
  private def boardWithPawnAt(from: SiteId): ReadyGame =
    on(board())(_.pawn(actor, from))

  /** The Travel board with the actor at `from` and E03's `side` at `at`. */
  private def boardWithE03(side: EdificeSide, at: SiteId, from: SiteId)
      : ReadyGame =
    on(boardWithPawnAt(from))(_.edifice(canal, side, at = at))

  test("a Coast reaches the Canal's site for 1 Supply, and the journal " +
      "replays and round trips"):
    // The printed Cradle-to-Hinterland 4.
    assertEquals(candidates(boardWithPawnAt(coast)).get(hinterlandPlains),
      Some(4))
    val ready = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = coast)
    assertEquals(candidates(ready).get(hinterlandPlains), Some(1))
    val done = accepted(travel(ready, hinterlandPlains))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).player(actor).pawnSite, Some(hinterlandPlains))
    assertEquals(Look(result).supply(actor), 7 - 1)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), result)
    assertWireRoundTrips(done.events)

  test("from a plain site the Canal's site keeps its printed cost: a Coast " +
      "adds nothing to arrive"):
    val ready = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = cradlePlains)
    assertEquals(candidates(ready).get(hinterlandPlains), Some(4))

  test("the Canal's site reaches a Coast or an Island for 1 Supply, and a " +
      "plain site at its printed cost"):
    val costs = candidates(boardWithE03(EdificeSide.Intact,
      at = hinterlandPlains, from = hinterlandPlains))
    // The printed 4.
    assertEquals(costs.get(coast), Some(1))
    // The printed 3 and the Island's 2, both skipped.
    assertEquals(costs.get(island), Some(1))
    assertEquals(costs.get(secondHinterlandPlains), Some(3))
    assertEquals(costs.get(cradlePlains), Some(4))

  test("a Canal at a Mountain: a Coast route skips the Mountain's Supply " +
      "and the Narrow Pass"):
    // The Pass blocks the Provinces to a player who does not rule it.
    assertEquals(candidates(boardWithPawnAt(coast)).get(mountain), None)
    val ready = boardWithE03(EdificeSide.Intact, at = mountain, from = coast)
    // The printed 2 and the Mountain's 1, both skipped.
    assertEquals(candidates(ready).get(mountain), Some(1))
    val result = Outcomes.ready(accepted(travel(ready, mountain)).state)
    assertEquals(Look(result).player(actor).pawnSite, Some(mountain))
    assertEquals(Look(result).supply(actor), 7 - 1)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.GrandCanalSuite"`
Expected: compile failure, "Not found: GrandCanalCard" (the card still lives in `distantlands.catalog.holding`).

- [ ] **Step 3: Move the card and add the Canal**

In `HoldingEdifices.scala`, delete the whole `object GrandCanalCard` (lines 10-23). `ModifierMark` and `MajorActionType` are then unused there, so the imports become:

```scala
import distantlands.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import distantlands.model.{Cost, EdificeId, PowerId, Suit}
```

Check with `grep -n "ModifierMark\|MajorActionType" src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`; it must print nothing after the edit. `NewFoundations.scala` already imports `distantlands.gameplay.powers.travel._`, so its `GrandCanalCard` entry needs no change.

Create `src/main/scala/distantlands/gameplay/powers/travel/GrandCanal.scala`:

```scala
package distantlands.gameplay.powers.travel

import distantlands.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import distantlands.gameplay.powers.setup.EdificeSetupSupport
import distantlands.model._

object GrandCanalCard extends Edifice(EdificeId("E03"), Suit.Discord):
  object intact extends EdificeFace("Grand Canal") with Locked:
    val power = PrintedPower(PowerId("edifice.e03.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is a Coast along with its other powers.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Lagoon"):
    val power = PrintedPower(PowerId("edifice.e03.ruined"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is an Island along with its other " +
        "powers.")
    val powers: Vector[PrintedPower] = Vector(power)

/** A face of E03 that gives the site it stands at a printed terrain
  * (catalog batch 10, slice 5). The site changes as the card moves or
  * flips, so it is read from the state on every use, never stored.
  *
  * `source` is static and cannot name that site, so it is the face's game
  * rule, as Hallowed Spring's is. The terrain is still the site's: Forest
  * Paths ignores a `LiveTerrain` as it ignores the powers of sites.
  */
sealed trait LiveTerrain extends ContributingPower:
  /** The face this power is printed on. */
  def side: EdificeSide
  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  /** The site this face stands at, if it is in play. */
  final def site(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, GrandCanalCard.id, side)

/** Grand Canal (E03, intact): "Act as if this site is a Coast along with
  * its other powers." Its site joins the live Coast set
  * ([[TravelTerrain]]), so a Coast route reaches it for 1 Supply. This
  * power is the Coast power at its site: a Coast route from there costs 1
  * Supply and ignores the terrain adds, as `CoastSitePower` does. It
  * writes no line.
  */
case object GrandCanal extends LiveTerrain:
  val id: PowerId = GrandCanalCard.intact.power.id
  val side: EdificeSide = EdificeSide.Intact

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      TravelRoute.adjustSupply(ctx, operations)(_ => 1))))

  override def applicable(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.source) &&
        TravelTerrain.coastRoute(ctx.state, route))

  override def shouldIgnore(other: ContributingPower): Boolean =
    TravelTerrain.ignoredOnCoastRoute(other)
```

- [ ] **Step 4: Read the sets from the state**

In `TravelSitePowers.scala`:

Add `ReadyGame` to the `distantlands.model` import list.

Add, after `object TravelRoute`:

```scala
/** The Travel terrain of the sites in play, read from the state on every
  * call: the printed Coast and Island sites, plus the site of either face
  * of E03. Grand Canal makes its site a Coast. Lost Lagoon makes its site
  * an Island, which has the Coast power (catalog batch 10, ruling 12).
  */
private[travel] object TravelTerrain:
  def coastSites(ready: ReadyGame): Set[SiteId] =
    TravelSitePowers.printedCoastSites ++ GrandCanal.site(ready)

  def coastOrIslandSites(ready: ReadyGame): Set[SiteId] =
    coastSites(ready) ++ TravelSitePowers.printedIslandSites

  /** Whether `route` leaves a Coast for a Coast or an Island. */
  def coastRoute(ready: ReadyGame, route: TravelRoute.PawnMove): Boolean =
    coastSites(ready).contains(route.source) &&
      coastOrIslandSites(ready).contains(route.destination)

  /** The terrain adds a Coast route ignores: "ignores other Travel
    * modifiers (Mountain, Pass, etc.)". The Pass is not an add; its
    * restriction reads `coastRoute` itself. */
  def ignoredOnCoastRoute(other: ContributingPower): Boolean = other match
    case _: MountainSitePower | _: IslandSitePower => true
    case _ => false
```

Replace `CoastSitePower` with:

```scala
final case class CoastSitePower(id: PowerId, site: SiteId)
    extends ContributingPower:
  def source: RuleSourceRef = RuleSourceRef.Site(site)
  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map.empty.updated(PowerWindow.TravelCost,
      Vector(Transform((ctx, operations) =>
        TravelRoute.adjustSupply(ctx, operations)(_ => 1))))
  override def applicable(ctx: PowerCtx): Boolean = TravelRoute
    .pawnMove(ctx.operation).exists(route => route.source == site &&
      TravelTerrain.coastRoute(ctx.state, route))
  override def shouldIgnore(other: ContributingPower): Boolean =
    TravelTerrain.ignoredOnCoastRoute(other)
```

In `NarrowPassSitePower`, change the header to:

```scala
final case class NarrowPassSitePower(id: PowerId, site: SiteId)
    extends ContributingPower:
```

and replace the first two lines of `blocked`'s body inside `flatMap`:

```scala
      val coastRoute = coastSites.contains(route.source) &&
        coastOrIslandSites.contains(route.destination)
      if coastRoute then None
```

with:

```scala
      if TravelTerrain.coastRoute(ctx.state, route) then None
```

In `object TravelSitePowers`, replace the two private sets and `all` with:

```scala
  private[travel] val printedCoastSites: Set[SiteId] = supported.collect {
    case Supported(_, site, Coast) => site
  }.toSet
  private[travel] val printedIslandSites: Set[SiteId] = supported.collect {
    case Supported(_, site, Island) => site
  }.toSet

  val all: Vector[ContributingPower] = supported.map:
    case Supported(id, site, Mountain) => MountainSitePower(id, site)
    case Supported(id, site, Island) => IslandSitePower(id, site)
    case Supported(id, site, Coast) => CoastSitePower(id, site)
    case Supported(id, site, NarrowPass) => NarrowPassSitePower(id, site)
```

Update the `TravelSitePowers` doc's first sentence: "The sites' Travel terrain contributions. Printed site topology lives on the power objects; the live Coast and Island sets come from [[TravelTerrain]], and command state supplies the current route through PowerCtx.operation."

- [ ] **Step 5: Register the Canal**

In `WalkerPowerCatalog.scala`, change line 80 from `TravelSitePowers.all ++` to:

```scala
      TravelSitePowers.all ++
      Vector(GrandCanal) ++
```

`GrandCanal` resolves through the file's wildcard import, `distantlands.gameplay.powers.travel._`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.* distantlands.gameplay.TravelSitePowersSuite"`
Expected: PASS. The Travel modifier suites, both Narrow Pass consent suites and `TravelSitePowersSuite` prove the printed terrain is unchanged.

- [ ] **Step 7: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`, then `git diff src/test/resources`.
Expected: `unimplemented.txt` drops exactly `edifice.e03.intact`. `declared-keys.txt` is unchanged.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/travel/GrandCanal.scala \
  src/main/scala/distantlands/gameplay/powers/travel/TravelSitePowers.scala \
  src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/powers/travel/GrandCanalSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): Grand Canal makes its site a Coast, read live from the state

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 1 diff against this plan and the spec.

---

### Task 2: Lost Lagoon, Forest Paths and the face flip

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/GrandCanal.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TravelSitePowers.scala` (`TravelTerrain`)
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/ForestPaths.scala` (`ignores` and its doc)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt` (drops `edifice.e03.ruined`)
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md:68`
- Test: `src/test/scala/distantlands/gameplay/powers/travel/GrandCanalSuite.scala`

**Interfaces:**
- Consumes: everything Task 1 produces; `ForestPaths`, `ErrandBoyCard` (a Beast denizen with no power these tests use); `ContributionCollector.gather`, `PowerWindowIndex.of`, `PowerCtx(catalog, ready, actor, source, window, answered, operation)`.
- Produces: `case object LostLagoon extends LiveTerrain`. `TravelTerrain.coastSites` includes the Lagoon's site, and `ignoredOnCoastRoute` includes `LostLagoon`.

- [ ] **Step 1: Write the failing tests**

Add these imports to `GrandCanalSuite.scala`:

```scala
import distantlands.gameplay.powerresolver.{ContributingPower,
  ContributionCollector, PowerCtx, PowerWindowIndex}
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
```

Add these members and tests to the class:

```scala
  /** The ids gathered at `TravelCost` for the actor's pawn moving `from`
    * `to` at `ready`, among `candidates` only. */
  private def travelCostPowersGathered(ready: ReadyGame, from: SiteId,
      to: SiteId, candidates: Vector[ContributingPower]): Vector[PowerId] =
    val node = Sequence(Vector(
      SpendSupply(actor, 2),
      Move(Piece.Pawn(actor), PositionedLocation(Location.Site(from)),
        PositionedLocation(Location.Site(to)))), Some(PowerWindow.TravelCost))
    ContributionCollector.gather(PowerWindow.TravelCost,
      PowerWindowIndex.of(candidates), power => PowerCtx(catalog, ready,
        actor, power.source, PowerWindow.TravelCost, Vector.empty, node))
      .order

  test("Lost Lagoon's site costs 2 more Supply to reach, except from a " +
      "Coast, and the journal replays and round trips"):
    val fromPlain = boardWithE03(EdificeSide.Ruined, at = hinterlandPlains,
      from = cradlePlains)
    // The printed 4 and the Island's 2.
    assertEquals(candidates(fromPlain).get(hinterlandPlains), Some(6))
    val done = accepted(travel(fromPlain, hinterlandPlains))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).supply(actor), 7 - 6)
    assertEquals(ActPowerHarness.replayed(rules, fromPlain, done.events),
      result)
    assertWireRoundTrips(done.events)
    val fromCoast = boardWithE03(EdificeSide.Ruined, at = hinterlandPlains,
      from = coast)
    assertEquals(candidates(fromCoast).get(hinterlandPlains), Some(1))

  test("Lost Lagoon has the Coast power: its site reaches a Coast or an " +
      "Island for 1 Supply (ruling 12)"):
    val costs = candidates(boardWithE03(EdificeSide.Ruined,
      at = hinterlandPlains, from = hinterlandPlains))
    assertEquals(costs.get(coast), Some(1))
    assertEquals(costs.get(island), Some(1))
    assertEquals(costs.get(secondHinterlandPlains), Some(3))

  test("a Lagoon at a Mountain adds both from a plain site, and neither " +
      "from a Coast"):
    // p1 rules the Pass, so the Provinces are open from the Cradle.
    val fromPlain = on(boardWithE03(EdificeSide.Ruined, at = mountain,
      from = cradlePlains))(_.warbandsAt(pass, actor, 1))
    // The printed 2, the Mountain's 1 and the Island's 2.
    assertEquals(candidates(fromPlain).get(mountain), Some(5))
    val fromCoast = boardWithE03(EdificeSide.Ruined, at = mountain,
      from = coast)
    // A Coast route: no Pass, no Mountain, no Island add.
    assertEquals(candidates(fromCoast).get(mountain), Some(1))

  test("ruining the Canal mid-game turns its site from a Coast into an " +
      "Island at once"):
    val intact = boardWithE03(EdificeSide.Intact, at = hinterlandPlains,
      from = cradlePlains)
    assertEquals(candidates(intact).get(hinterlandPlains), Some(4))
    val ruined = on(intact)(_.edifice(canal, EdificeSide.Ruined,
      at = hinterlandPlains))
    assertEquals(candidates(ruined).get(hinterlandPlains), Some(6))

  test("Forest Paths ignores both faces when a beast card stands at the " +
      "destination"):
    val canalRoute = on(boardWithE03(EdificeSide.Intact,
      at = hinterlandPlains, from = hinterlandPlains))(
      _.denizen(ErrandBoyCard.id, at = island))
    assert(travelCostPowersGathered(canalRoute, hinterlandPlains, island,
      Vector(GrandCanal)).contains(GrandCanal.id))
    val withPaths = travelCostPowersGathered(canalRoute, hinterlandPlains,
      island, Vector(GrandCanal, ForestPaths))
    assert(withPaths.contains(ForestPaths.id))
    assert(!withPaths.contains(GrandCanal.id))

    val lagoonRoute = on(boardWithE03(EdificeSide.Ruined,
      at = hinterlandPlains, from = cradlePlains))(
      _.denizen(ErrandBoyCard.id, at = hinterlandPlains))
    assert(travelCostPowersGathered(lagoonRoute, cradlePlains,
      hinterlandPlains, Vector(LostLagoon)).contains(LostLagoon.id))
    assert(!travelCostPowersGathered(lagoonRoute, cradlePlains,
      hinterlandPlains, Vector(LostLagoon, ForestPaths))
      .contains(LostLagoon.id))
```

If `on(...)(_.edifice(...))` in the flip test leaves two E03 states, stop: `Table.moving` must remove the card from its earlier site first.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.GrandCanalSuite"`
Expected: compile failure, "Not found: LostLagoon".

- [ ] **Step 3: Add the Lagoon**

Append to `GrandCanal.scala`:

```scala
/** Lost Lagoon (E03, ruined): "Act as if this site is an Island along with
  * its other powers." An Island costs 2 Supply more to reach "unless
  * you're traveling from a Coast or Island": a Coast route into the site
  * skips the add, since every Coast power ignores this power. An Island
  * "has the Coast power" (ruling 12), so the site joins the live Coast
  * set, and a Coast route from it costs 1 Supply and ignores the terrain
  * adds. Arriving adds 2; leaving by a Coast route sets 1. A route never
  * does both, since it cannot leave and reach the same site. It writes no
  * line.
  */
case object LostLagoon extends LiveTerrain:
  val id: PowerId = GrandCanalCard.ruined.power.id
  val side: EdificeSide = EdificeSide.Ruined
  /** The Island's add. */
  val Add: Int = 2

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      if leavesByCoastRoute(ctx) then
        TravelRoute.adjustSupply(ctx, operations)(_ => 1)
      else TravelRoute.adjustSupply(ctx, operations)(_ + Add))))

  override def applicable(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.destination)) || leavesByCoastRoute(ctx)

  /** The other terrain adds, only while it is the Coast power. It never
    * ignores itself. */
  override def ignores(ctx: PowerCtx, other: ContributingPower): Boolean =
    other != LostLagoon && leavesByCoastRoute(ctx) &&
      TravelTerrain.ignoredOnCoastRoute(other)

  private def leavesByCoastRoute(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      site(ctx.state).contains(route.source) &&
        TravelTerrain.coastRoute(ctx.state, route))
```

In `TravelTerrain` (`TravelSitePowers.scala`), make `coastSites` include the Lagoon, and `ignoredOnCoastRoute` ignore it:

```scala
  def coastSites(ready: ReadyGame): Set[SiteId] =
    TravelSitePowers.printedCoastSites ++ GrandCanal.site(ready) ++
      LostLagoon.site(ready)
```

```scala
  def ignoredOnCoastRoute(other: ContributingPower): Boolean = other match
    case _: MountainSitePower | _: IslandSitePower | LostLagoon => true
    case _ => false
```

`coastOrIslandSites` needs no change: the Lagoon's site reaches it through `coastSites`.

In `WalkerPowerCatalog.scala`, change `Vector(GrandCanal) ++` to `Vector(GrandCanal, LostLagoon) ++`.

- [ ] **Step 4: Let Forest Paths ignore live terrain**

In `ForestPaths.scala`, replace `ignores` with:

```scala
  override def ignores(ctx: PowerCtx, other: ContributingPower): Boolean =
    beastAtDestination(ctx) && (other match
      case _: LiveTerrain => true
      case _ => other.source match
        case _: RuleSourceRef.Site => true
        case _ => false)
```

In its doc, after "every power whose source is a site is dropped from the fold.", add: "So is a `LiveTerrain` (Grand Canal, Lost Lagoon), which gives a site its terrain from an edifice."

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.* distantlands.gameplay.TravelSitePowersSuite"`
Expected: PASS.

- [ ] **Step 6: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`, then `git diff src/test/resources`.
Expected: `unimplemented.txt` drops exactly `edifice.e03.ruined`. `declared-keys.txt` is unchanged.

- [ ] **Step 7: Update the spec and the roadmap**

In the spec:
- Change the status line to "**Status:** designed 2026-11-03. Slices 1 to 5 landed."
- After ruling 11, add:

```markdown
12. **Lost Lagoon has the Coast power** (owner, 2026-11-05). Every printed
    Island "has the Coast power", so the Lagoon's site joins both live
    sets: Lagoon-to-Coast costs 1 Supply and counts as a Coast route for
    Narrow Pass.
```

- Replace the "Slice 5: Live terrain" body above "Tests:" with:

```markdown
`TravelSitePowers` builds the Coast and Island sets once from a static
list. `TravelTerrain` reads them from the state instead: the printed Coast
sites plus the site of either E03 face for the Coast set, and the Coast set
plus the printed Islands for the Coast-or-Island set (ruling 12).
`CoastSitePower.applicable` and `NarrowPassSitePower.blocked` take them from
`ctx.state`.

Two new powers stand at a live site, found with
`EdificeSetupSupport.siteOf`: `GrandCanal`, with Coast behaviour, and
`LostLagoon`, with Island behaviour (+2 Supply to arrive, skipped on a
Coast route) and Coast behaviour to leave. A power's `source` is static, so
each has a `RuleSourceRef.GameRule` source and extends the `LiveTerrain`
marker; Forest Paths ignores a `LiveTerrain` as it ignores printed terrain.
```

- Change the Tests line to add "Lagoon-to-Coast at 1 Supply;" after "the Coast route to it;".

In `docs/ROADMAP.md`, delete the line `- **Live terrain:** Grand Canal, Lost Lagoon.` (memory: "Roadmap deletes done work").

- [ ] **Step 8: Run the gates**

Run, in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes; statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/travel/GrandCanal.scala \
  src/main/scala/distantlands/gameplay/powers/travel/TravelSitePowers.scala \
  src/main/scala/distantlands/gameplay/powers/travel/ForestPaths.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/powers/travel/GrandCanalSuite.scala \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): Lost Lagoon makes its site an Island with the Coast power

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 2 diff against this plan and the spec.

---

### Review additions

Taken after the task reviews, and committed with the review fixes:
- **Task 1:** a test that a Canal at a Mountain is still behind the Narrow Pass from a plain site, and still pays the Mountain's Supply once the Pass is ruled.
- **Task 2:**
  - A gather-level test that a Coast power drops `LostLagoon` on a Coast route into it. Without it, the fold order hid a missing ignore: the Lagoon's `game:` key sorts before every `site:` key, so its +2 folded first and the Coast power then set 1.
  - **Ruling 13 (owner, 2026-11-05):** Lost Lagoon at a printed Island adds nothing more on arrival, so the site costs +2 once. `LostLagoon.applicable` skips the arrival case at a `printedIslandSites` destination, with a test.
