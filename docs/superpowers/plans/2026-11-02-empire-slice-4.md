# Empire Slice 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Game 2 starts from the Chronicle that game 1's Chronicle Phase journals. A seated Chancellor and Citizens start with purple warbands, the Empire's sites return to the map, and each player starts with any stored starting advisers.

**Architecture:**
- The deal records who becomes the Chancellor. With an Empire, `SetupOrders.firstPlayer` is the seated Chancellor. When the Chancellor's lineage is absent, it is a seated Citizen picked by the plan factory's random port. `GameStartRules` checks this; replay stays free of randomness.
- `GameStartRules.buildPlan` resolves an `EmpireSetup` (the Chancellor, every Imperial player, the Empire's stored sites) from the Chronicle and the seats. `FirstGameSetupPlan` carries it as `empire: Option[EmpireSetup]`, plus each player's starting advisers.
- `FirstGameSetupMaterializer` fills the Empire's sites first, Hinterward, then the remaining slots from the atlas's Recent end as Foundation I does. Foundation II counts every denizen on the map.
- A new `LaterGameSetup` object, called from `GameStartRules.evolve`, applies the Empire's table facts to the built game: roles, the absent Chancellor's box role, the purple supply, the Grand Scepter, the Oathkeeper title, the banners, the tracks and the starting advisers.
- `SetupProcedure` gains the Chancellor's Reliquary pick as its first step, offers each player only their legal pawn sites, and skips the adviser choice for an empty temporary hand.
- The player-selection page shows each lineage's role from the Chronicle summary.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 4: Later-game setup", "Rulings", "Testing"
and "Out of scope"). Rule text: `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`, "Setup for Later Games". Task 8 corrects the spec where this plan's rulings change it.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/empire-slice-4` on branch `empire-slice-4`, off `0.2.0-alpha`. Never work in the main checkout.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree.
  - Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `./sbtw frontend/test`
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Empire:** a Chronicle has an Empire when one of its lineages is the Chancellor. A first-game Chronicle has none, and its setup must not change: every existing `GameStartRulesSuite`, `SetupProcedure` and golden-log test keeps passing unchanged unless a task says otherwise.
- **Still refused:** legacies, starting relics, an Empire-less later game, stored foundation faces (kept normal, not refused).
- **Wire:** old journals need not decode. Every new or changed wire field gets a round-trip test. A browser's saved Chronicle entries must still decode, so a new summary field is optional.
- **Log lines:** Setup lines name their player, as the existing Setup lines do ("p1 placed pawn at Ancient City").
- **Hover reasons:** show an unavailable option disabled with a reason. Pawn sites a player can never use at this point are not offered, as Setup offers only legal sites today.
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write`, `UNIMPLEMENTED_SNAPSHOT=write` and `GAMELOG_GOLDEN=write`, then read the diff. Commit only lines this task explains.

## Rulings for this slice

Decided with the user on 2026-11-02. Task 8 records them in
`docs/rules/ambiguities.md` as Empire rulings R25 to R30.

- **R25, Exile pawn sites.** With an Empire, an Exile places their pawn at the Hintermost Empire site or at a site the bandits rule. A site with no warbands, such as Sunken Isles with capacity 0, is not ruled by bandits, so an Exile may not start there.
- **R26, Homelands in later games.** The check that each Homeland in play has a stored edifice of its suit applies only to a Chronicle with no Empire (a first game). Later-game setup places only the edifices stored with each site (CR p. 9, Foundation I).
- **R27, stored cards at the remaining sites.** A site filled from the atlas in a later game keeps its stored cards: its edifice on the ruined side, its denizens faceup, its relics facedown. Its empty relic slots, its relic slots less its stored relics and never fewer than 0, are filled from the relic deck. Sites always come from the Recent end; the Chancellor's Recent-or-Forgotten choice stays in the roadmap's "Simultaneous setup effects" item.
- **R28, the Empire always holds a site.** Shape Empire gives a winner who rules no site the site of their pawn (the Throne's Shape Empire step, `docs/rules/chronicle.md`), so a Chronicle with a Chancellor and an empty `world` is malformed. Game start refuses it: "an Empire needs at least one site".
- **R29, the Chancellor's Reliquary pick.** The pick is the first Setup step. The Chancellor alone identifies the Reliquary's relics during Setup, and keeps one facedown. An empty Reliquary skips the step. The line reads "p1 took a relic from the Reliquary".
- **R30, a short purple supply at setup.** Purple warbands are limited to the 24 components, in setup and in play. Setup first gives each Imperial player their 3 board warbands, then places 1 Imperial warband on each Empire site, Cradlemost first, until the supply runs out. An Empire site left without one is set up as an Empire site in every other way: intact edifice, stored relics only, no relic slots filled. Once the Empire is set up, the empty-site check that the walker runs at each action boundary (`StateBasedEvaluation`'s bandit refill) fills every empty site with bandits up to its capacity. A capacity-0 site stays empty. Chronicle rulings 20 and 22 keep their unlimited supply, since the Chronicle phase is not play.
- **Random Citizen.** When the Chancellor's lineage is not seated, the plan factory picks a random seated Citizen and records them as `firstPlayer`; they become the Chancellor and the absent lineage a Citizen in the box (R6).
- **Foundation II.** Each bank gets 1 extra favor per denizen of its suit on the map once every site is filled, edifices included, plus the 5-player bonus.

---

### Task 1: The deal for a later game

**Files:**
- Modify: `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`
- Modify: `src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala`
- Create: `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`
- Test: `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala` (create if absent; else add to the existing factory suite)
- Test: `src/test/scala/distantlands/application/ChronicleFirstGamePlanSuite.scala` (same rule)

**Interfaces:**
- Produces: `ChronicleFirstGamePlan.dealOrder(config: FirstGameBootstrapConfig, drawers: Int, denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId], mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders`. `drawers` is the number of seated players with no starting advisers. `mix` shuffles the Visions into a short deck.
- Produces: `SetupDeal.drawers(chronicle: Chronicle, participants: Vector[FirstGameParticipant]): Int`, in a new `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`, so `GameStartRules` (Task 2) and the application factory share it.
- Produces: `firstPlayer` rule. With a Chancellor lineage in the Chronicle: the seated Chancellor's player, else a random seated Citizen's player, else the first seat (Task 2 refuses that Chronicle).

- [ ] **Step 1: Write the failing tests**

```scala
test("a later game deals hands only to players with no starting advisers"):
  // 3 seats, one with a stored adviser: 6 discards + 2 hands of 3.
  val orders = ChronicleFirstGamePlan.dealOrder(config, drawers = 2,
    denizens, relics, identity)
  val dealt = 6 + 2 * 3
  assertEquals(orders.worldDeckOrder.collect { case id: DenizenId => id },
    denizens.drop(dealt))

test("a deck of fewer than 25 denizens after the deal mixes all five " +
    "Visions through it"):
  val short = denizens.take(6 + 3 * 3 + 20)
  val orders = ChronicleFirstGamePlan.dealOrder(config, drawers = 3, short,
    relics, _.reverse)
  val rest: Vector[WorldCardId] = short.drop(15)
  assertEquals(orders.worldDeckOrder,
    (rest ++ FirstGameRulesData.visions).reverse)

test("the seated Chancellor goes first in a later game"):
  // Chronicle where l2 is the Chancellor; seats shuffle with `identity`.
  assertEquals(plan(empireChronicle(chancellor = "l2")).setup.firstPlayer,
    PlayerId("p2"))

test("with the Chancellor's lineage absent, a random seated Citizen goes " +
    "first"):
  // l9 is the Chancellor and not seated; l1 and l3 are Citizens. The port
  // reverses, so its pick is the last Citizen in seat order.
  assertEquals(plan(absentChancellor).setup.firstPlayer, PlayerId("p3"))
```

Use a deterministic `ChronicleRandomPort` whose `shuffle` is `identity` for seating and `_.reverse` where the test says. Build the Chronicles with `FirstGameSetupFixture.chronicle.copy(lineages = ...)`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.*FirstGamePlan*"`
Expected: FAIL to compile (`dealOrder` takes no `drawers`).

- [ ] **Step 3: Implement**

`SetupDeal` (`gameplay/setup`):

```scala
/** Players who draw 3 denizens in Foundation IV: those with no starting
  * advisers (CR p. 11). */
def drawers(chronicle: Chronicle,
    participants: Vector[FirstGameParticipant]): Int =
  val advised = chronicle.lineages.filter(_.startingAdvisers.nonEmpty)
    .map(_.id).toSet
  participants.count(p => !advised(p.lineageId))
```

`ChronicleFirstGamePlan`:

```scala
def dealOrder(config: FirstGameBootstrapConfig, drawers: Int,
    denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId],
    mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders =
  val dealt = 6 + drawers * 3
  val remaining = denizenOrder.drop(dealt)
  val visions = FirstGameRulesData.visions
  // CR p. 12: fewer than 25 denizens shuffle with every Vision.
  val worldDeckOrder: Vector[WorldCardId] =
    if remaining.size < 25 then mix(remaining ++ visions)
    else remaining.take(10) ++ visions.take(2) ++ remaining.slice(10, 25) ++
      visions.drop(2) ++ remaining.drop(25)
  SetupOrders(config.participants, config.firstPlayer, denizenOrder,
    worldDeckOrder, relicOrder)
```

Update the doc comment: the dealt count is 6 plus 3 per player with no starting advisers (Foundation IV).

`GeneratedFirstGamePlanFactory`: resolve the chronicle first, then seat. Replace `shuffledSeating(config)` with `seating(config, chronicle)`:

```scala
private def seating(config: FirstGameBootstrapConfig, chronicle: Chronicle)
    : FirstGameBootstrapConfig =
  val seats = random.shuffle(config.participants)
  def seatOf(role: Role) = chronicle.lineages.filter(_.role == role)
    .map(_.id).toSet
  val chancellor = seats.find(p => seatOf(Role.Chancellor)(p.lineageId))
  val citizens = seats.filter(p => seatOf(Role.Citizen)(p.lineageId))
  // A random seated Citizen stands in for an absent Chancellor (R6).
  val first = if seatOf(Role.Chancellor).isEmpty then seats.headOption
    else chancellor.orElse(random.shuffle(citizens).headOption)
      .orElse(seats.headOption)
  first.fold(config)(p => FirstGameBootstrapConfig(seats, p.playerId))
```

Pass `SetupDeal.drawers(chronicle, seated.participants)` and `mix = random.shuffle` to `dealOrder`. Update the class comment: with an Empire, the first player is the Chancellor.

Fix every other `dealOrder` caller (`grep -rn "dealOrder(" src`): first games pass `drawers = config.participants.size` and `mix = identity`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala src/main/scala/distantlands/application/ChronicleFirstGamePlan.scala <test files>
git commit -m "feat(setup): a later game's deal seats the Chancellor first"
```

---

### Task 2: Game-start validation for a later game

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/EmpireSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/model/Setup.scala` (`FirstGameSetupPlan`)
- Test: `src/test/scala/distantlands/gameplay/setup/GameStartRulesSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/setup/LaterGameFixture.scala` (create)

**Interfaces:**
- Produces, in `EmpireSetup.scala`:

```scala
/** The Empire a later game starts with (CR pp. 8-9): its Chancellor, every
  * seated Imperial player, and the Empire's stored sites Cradlemost first.
  * `absentChancellor` is the Chancellor's lineage when it sits out; it
  * becomes a Citizen in the box (ruling R6). */
final case class EmpireSetup(chancellor: PlayerId,
    imperial: Set[PlayerId], sites: Vector[StoredSite],
    absentChancellor: Option[LineageId])

object EmpireSetup:
  def resolve(chronicle: Chronicle, orders: SetupOrders)
      : Either[OathViolation, Option[EmpireSetup]]
```

- Produces: `FirstGameSetupPlan` gains `empire: Option[EmpireSetup]`, `atlasSites: Vector[StoredSite]` (the atlas sites filling the remaining slots, with their stored cards) and `startingAdvisers: Map[PlayerId, Vector[AdviserState]]`. `orderedSites` stays: the Empire's sites, then `atlasSites`.
- Produces: `LaterGameFixture` in tests, with `chronicle(chancellor: LineageId, citizens: LineageId*, world: Int = 2): Chronicle` and `orders(chronicle: Chronicle, firstPlayer: PlayerId): SetupOrders`. The fixture moves the first `world` atlas sites of `FirstGameSetupFixture.chronicle` into `world`, gives each world site one faceup denizen taken from the world deck, sets the lineages' roles, and builds the orders with `ChronicleFirstGamePlan.dealOrder` and `drawers`.

- [ ] **Step 1: Write the failing tests**

Replace the tests "game start refuses an Imperial role, even in the box, until the Empire" and "game start refuses a seated lineage's starting advisers until later-game setup" with:

```scala
test("a later game with a seated Chancellor starts"):
  val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
  accepted(GameStartRules.evolve(catalog, record,
    LaterGameFixture.orders(record, PlayerId("p1"))))

test("game start refuses a later game whose first player is not the " +
    "Chancellor"):
  val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
  assertEquals(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p2"))),
    Left(OathViolation.UnsupportedChronicle(
      "the first player must be the Chancellor")))

test("with the Chancellor absent, the first player must be a seated " +
    "Citizen"):
  val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l2"))
  accepted(GameStartRules.evolve(catalog, record,
    LaterGameFixture.orders(record, PlayerId("p2"))))
  assertEquals(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p1"))),
    Left(OathViolation.UnsupportedChronicle(
      "the first player must be a seated Citizen")))

test("game start refuses an Empire-less later game until Wide Horizons"):
  val record = LaterGameFixture.chronicle(LineageId("l9"), LineageId("l8"))
  assertEquals(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p1"))),
    Left(OathViolation.UnsupportedChronicle(
      "an Empire-less later game requires Wide Horizons")))

test("game start refuses an Empire with no site (R28)"):
  val record = LaterGameFixture.chronicle(LineageId("l1"), world = 0)
  assertEquals(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, PlayerId("p1"))),
    Left(OathViolation.UnsupportedChronicle(
      "an Empire needs at least one site")))

test("a later game skips the Homeland edifice check (R26)"):
  // Strip every stored edifice from the atlas.
  val record = LaterGameFixture.chronicle(LineageId("l1"))
  val bare = record.copy(atlasBox = record.atlasBox.map(site =>
    site.copy(items = site.items.filterNot(_.isInstanceOf[EdificeId]))))
  accepted(GameStartRules.evolve(catalog, bare,
    LaterGameFixture.orders(bare, PlayerId("p1"))))

test("a later game accepts stored denizens and relics at atlas sites (R27)"):
  val record = LaterGameFixture.chronicle(LineageId("l1"))
  val relic = record.relicDeck.head
  val denizen = record.worldDeck.head
  val atlas = record.atlasBox.head
  val stored = record.copy(relicDeck = record.relicDeck.tail,
    worldDeck = record.worldDeck.tail,
    atlasBox = atlas.copy(items = atlas.items ++ Vector(denizen, relic)) +:
      record.atlasBox.tail)
  accepted(GameStartRules.evolve(catalog, stored,
    LaterGameFixture.orders(stored, PlayerId("p1"))))
```

Keep the first-game refusals of stored denizens and relics and of missing Homeland edifices: a first-game Chronicle still refuses them. Keep "starting relics require Legacies" and "legacies require Legacies".

In `ChronicleEndToEndSuite`, the test "game start refuses the crowned Chancellor rather than seating them as an Exile, even with the Empire's sites back in the atlas" now expects "an Empire needs at least one site"; rename it to "game start refuses a crowned Chancellor whose Empire has no site". The assertion at line 66 that expects "a non-empty world requires the Empire" moves to Task 7, which starts that game.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.GameStartRulesSuite"`
Expected: FAIL, "a non-empty world requires the Empire".

- [ ] **Step 3: Implement**

`EmpireSetup.resolve`:

```scala
def resolve(chronicle: Chronicle, orders: SetupOrders)
    : Either[OathViolation, Option[EmpireSetup]] =
  val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
  def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
    .map(_.id)
  lineages(Role.Chancellor).headOption match
    case None => Either.cond(lineages(Role.Citizen).isEmpty &&
        chronicle.world.isEmpty, None,
      UnsupportedChronicle("an Empire-less later game requires Wide Horizons"))
    case Some(lineage) =>
      val citizens = lineages(Role.Citizen).flatMap(seated.get)
      for
        _ <- Either.cond(chronicle.world.nonEmpty, (),
          UnsupportedChronicle("an Empire needs at least one site"))
        chancellor <- seated.get(lineage) match
          case Some(player) => Either.cond(orders.firstPlayer == player,
            player, UnsupportedChronicle(
              "the first player must be the Chancellor"))
          case None if citizens.isEmpty => Left(UnsupportedChronicle(
            "an Empire-less later game requires Wide Horizons"))
          case None => Either.cond(citizens.contains(orders.firstPlayer),
            orders.firstPlayer, UnsupportedChronicle(
              "the first player must be a seated Citizen"))
      yield Some(EmpireSetup(chancellor,
        (citizens.toSet + chancellor), chronicle.world,
        Option.when(!seated.contains(lineage))(lineage)))
```

A Chronicle with no Chancellor but Citizens, or a `world`, also refuses as Empire-less: CR p. 8 makes a random Citizen the Chancellor only when "there's no Chancellor", and the factory cannot seat one here. Comment this.

`GameStartRules.buildPlan`:
- Delete the refusals "a non-empty world requires the Empire", "Imperial roles require the Empire" and "starting advisers require later-game setup".
- Resolve `empire <- EmpireSetup.resolve(chronicle, orders)` after `validateParticipants`.
- `inPlay = chronicle.world ++ chronicle.atlasBox.take(8 - chronicle.world.size)`; `atlasSites = chronicle.atlasBox.take(8 - chronicle.world.size)`. Check `chronicle.atlasBox.size >= 8 - chronicle.world.size` with the existing message, its count changed to the slots left.
- Keep "stored denizens or relics on an atlas site are not supported yet" and `homelandEdifices` only when `empire.isEmpty` (R26, R27). With an Empire, `homelands` is `Vector.empty`.
- The relic-count check counts only empty slots: for each atlas site, `max(0, relicSlots - stored relics)`; Empire sites take none.
- The world-deck count and `validateDeal` use `dealt = 6 + 3 * SetupDeal.drawers(chronicle, orders.participants)`.
- `validateDeal` compares the world deck's denizens to `denizenOrder.drop(dealt)` sorted by id, since a short deck is mixed (Task 1).
- `startingAdvisers`: for each seated lineage the Chronicle lists, its player to its `startingAdvisers`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.chronicle.ChronicleEndToEndSuite distantlands.application.*"`
Expected: PASS. The later-game tests pass with the first-game table for now; Task 3 builds the Empire's.

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "feat(setup): game start accepts a later game with an Empire"
```

---

### Task 3: The later-game map

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala` (edifice deck, atlas remainder)
- Test: `src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala` (create)

**Interfaces:**
- Consumes: `FirstGameSetupPlan.empire`, `.atlasSites` (Task 2).
- Produces: a map whose first `world.size` sites are the Empire's, Hinterward from the Cradle's top.

- [ ] **Step 1: Write the failing tests**

```scala
test("the Empire's sites fill the first slots Hinterward, each with one " +
    "Imperial warband, intact edifices and faceup denizens"):
  val ready = laterGame()  // LaterGameFixture, world = 2, l1 Chancellor
  val map = ready.game.current.map
  assertEquals(map.hinterward.take(2), record.world.map(_.site))
  record.world.foreach { stored =>
    val site = map.sites(stored.site)
    assertEquals(site.forces, SiteForces.Occupied(ForceKind.Imperial, 1))
    assert(site.denizens.forall {
      case EdificeState(_, side, _) => side == EdificeSide.Intact
      case state: DenizenState => state.orientation == Orientation.FaceUp
    })
  }

test("an Empire site keeps its stored relics facedown and fills no empty " +
    "slot"):
  // One stored relic on a 2-slot Empire site.
  assertEquals(siteRelics(ready, empireSite), Vector(storedRelic))

test("a remaining site keeps its stored cards, ruins its edifice and " +
    "fills only its empty relic slots (R27)"):
  // Atlas site with an edifice, a denizen and one relic, 2 relic slots.
  val site = ready.game.current.map.sites(atlasSite)
  assertEquals(site.forces, SiteForces.Occupied(ForceKind.Bandit, capacity))
  assert(site.denizens.contains(EdificeState(edifice, EdificeSide.Ruined,
    Tokens.empty)))
  assertEquals(site.relics.size, 2)
  assertEquals(site.relics.head.id, storedRelic)

test("the edifice deck holds no edifice stored at any site"):
  assert(!ready.game.current.commonCards.edificeDeck.exists(storedEdifices))

test("each favor bank adds one per denizen of its suit on the map"):
  val onMap = ready.game.current.map.sites.values.flatMap(_.denizens)
    .flatMap(state => suitOf(state.id)).toVector
  Suit.all.foreach(suit => assertEquals(ready.banks.favor(suit),
    3 + onMap.count(_ == suit)))

test("the atlas keeps the unused sites with their stored cards"):
  assert(ready.game.campaign.atlas.entries.contains(AtlasEntry.StoredSite(
    unused.site, Vector(SiteDenizenState-for-denizen), Vector(relicState),
    Some(edifice))))
```

Write each fixture value out in the suite: pick concrete sites with `CatalogNames.site(...)` and give the test values descriptive names, such as `empireSiteWithOneRelic` (memory: descriptive helper names). Check `SiteDenizenState`'s shape in `model/World.scala` before writing the last test.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LaterGameSetupSuite"`
Expected: FAIL, the Empire's sites hold bandits.

- [ ] **Step 3: Implement**

In `FirstGameSetupMaterializer.materialize`, build each site from its `StoredSite` rather than from `homelandEdifices` when `plan.empire` is defined:

```scala
private def empireSite(stored: StoredSite, purple: Boolean): SiteState =
  val definition = catalog.site(stored.site).get
  SiteState(if purple then SiteForces.Occupied(ForceKind.Imperial, 1)
    else SiteForces.Empty,
    denizens(stored, EdificeSide.Intact), storedRelics(stored),
    definition.startingResources)

private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
  val definition = catalog.site(stored.site).get
  val forces = if definition.capacity == 0 then SiteForces.Empty
    else SiteForces.Occupied(ForceKind.Bandit, definition.capacity)
  SiteState(forces, denizens(stored, EdificeSide.Ruined),
    storedRelics(stored) ++ drawn.map(RelicState(_, Orientation.FaceDown,
      Tokens.empty)), definition.startingResources)
```

Purple is component-limited (R30). The boards take theirs first, so the sites share `MaterialBankState.ImperialWarbands - 3 * empire.imperial.size`: the first that many Empire sites, Cradlemost first, get `purple = true`, the rest `false`. A short site stays `SiteForces.Empty` here; Task 4 fills it with bandits.

`denizens` maps a stored `EdificeId` to `EdificeState(id, side, Tokens.empty)` and a stored `DenizenId` to `DenizenState(id, Orientation.FaceUp, Tokens.empty)`; `storedRelics` maps each stored `RelicId` to a facedown `RelicState`. `assignRelics` deals each atlas site `max(0, relicSlots - stored relics)` from `relicOrder`, in Hinterward order, and Empire sites none. The first-game path stays as it is.

`favorBanks` counts the suit of every denizen on the built map (`catalog.denizen(id)` or `catalog.edifice(id)`), not only `homelandEdifices`. For a first game this is the same count, since its map holds only the Homeland edifices; the existing favor-bank test proves it.

In `GameStartRules.evolve`:
- `storedEdifices` covers `chronicle.atlasBox ++ chronicle.world`.
- `atlas` keeps every atlas site past `atlasSites`, with its stored denizens as faceup `SiteDenizenState`s, relics as facedown `RelicState`s and its edifice, the inverse of `ChronicleExtraction.extract`'s `carried`.

Keep `FirstGameSetupMaterializer` under 800 lines; if the Empire path pushes it past ~250, move the stored-site builders to `gameplay/setup/StoredSitePopulation.scala`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.*"`
Expected: PASS, including every first-game test unchanged.

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "feat(setup): a later game restores the Empire's sites"
```

---

### Task 4: Players, roles and the Imperial table

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/LaterGameSetup.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/GameStartRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/setup/FirstGameSetupMaterializer.scala` (`handFor`, discards)
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala` (extract `emptySites`)
- Test: `src/test/scala/distantlands/gameplay/setup/LaterGameSetupSuite.scala`

**Interfaces:**
- Consumes: `EmpireSetup`, `FirstGameSetupPlan.startingAdvisers`.
- Produces: `LaterGameSetup.apply(ready: ReadyGame, plan: FirstGameSetupPlan): ReadyGame`, the identity for a plan with no Empire.

- [ ] **Step 1: Write the failing tests**

```scala
test("the Chancellor takes the Grand Scepter faceup and the Oathkeeper " +
    "title, and goes first"):
  val ready = laterGame()
  assert(player(ready, p1).relics.contains(RelicState(TheGrandScepterCard.id,
    Orientation.FaceUp, Tokens.empty)))
  assertEquals(ready.game.current.oathkeeper,
    OathkeeperState(Some(p1), TitleSide.Oathkeeper))
  assertEquals(ready.game.current.tracks.roundStarter, p1)
  assert(!ready.game.current.tracks.usurperLimited)

test("every player starts with 1 favor, 1 secret, 3 board warbands and " +
    "Supply 7; Imperial boards hold purple"):
  val ready = laterGame()
  ready.game.current.players.foreach(state => assertEquals(state.board,
    PlayerBoardState(1, 1, 0, 3, SupplyTrack.full)))
  assertEquals(PlayerFacts.banked(ready, ForceKind.Imperial),
    MaterialBankState.ImperialWarbands - 3 * 2 - record.world.size)

test("with too few purple warbands, the Hintermost Empire sites take " +
    "bandits up to their capacity (R30)"):
  // Six Imperial players and 8 Empire sites: the boards take 18, so only
  // the 6 Cradlemost Empire sites get a purple warband.
  val ready = laterGame(citizens = Vector(l2, l3, l4, l5, l6),
    world = eightEmpireSites)
  val map = ready.game.current.map
  val (purple, short) = eightEmpireSites.map(_.site).splitAt(6)
  purple.foreach(site => assertEquals(map.sites(site).forces,
    SiteForces.Occupied(ForceKind.Imperial, 1)))
  short.foreach(site => assertEquals(map.sites(site).forces,
    SiteForces.Occupied(ForceKind.Bandit, capacityOf(site))))
  assertEquals(ready.banks.warbandSupply(ForceKind.Imperial),
    MaterialBankState.ImperialWarbands)
  assertEquals(PlayerFacts.banked(ready, ForceKind.Imperial), 0)

test("a short Empire site is otherwise set up as an Empire site (R30)"):
  // Same table; check one of the 2 Hintermost sites.
  val site = ready.game.current.map.sites(eightEmpireSites.last.site)
  assert(site.denizens.forall {
    case EdificeState(_, side, _) => side == EdificeSide.Intact
    case state: DenizenState => state.orientation == Orientation.FaceUp
  })
  assertEquals(site.relics.map(_.id), storedRelicsOf(eightEmpireSites.last))

test("an absent Chancellor's seated Citizen becomes the Chancellor and the " +
    "absent lineage a Citizen in the box (R6)"):
  val ready = laterGame(chancellor = l9, citizens = l2)
  assertEquals(Imperials.chancellor(ready), Some(p2))
  assertEquals(ready.game.campaign.boxRoles(l9), Role.Citizen)

test("a seated lineage takes its starting advisers facedown and draws no " +
    "hand"):
  val ready = laterGame(advisers = Map(l2 -> Vector(adviser)))
  assertEquals(player(ready, p2).advisers, Vector(adviser))
  assertEquals(ready.game.campaign.lineages(l2).startingAdvisers, Vector.empty)
  assert(ready.game.current.temporaryHands.getOrElse(p2, Vector.empty).isEmpty)
  assertEquals(ready.game.current.temporaryHands(p3).size, 3)

test("the People's Favor goes to the Chancellor when the goal is The " +
    "People, and the Darkest Secret when it is Devotion"):
  assertEquals(laterGame(goal = OathkeeperGoal.ThePeople).game.current
    .banners.peoplesFavor.holder, Some(p1))
  assertEquals(laterGame(goal = OathkeeperGoal.Devotion).game.current
    .banners.darkestSecret.holder, Some(p1))

test("the game validates and its card index holds each card once"):
  val ready = laterGame(advisers = Map(l2 -> Vector(adviser)))
  assert(ready.game.cardIndex.isRight)
```

The R30 tests need `LaterGameFixture` to take `citizens` as a `Vector` and the Empire's stored sites as `world`. Define `eightEmpireSites` as 8 concrete `StoredSite`s with capacity above 0, and `capacityOf` and `storedRelicsOf` as one-line lookups in the suite. The existing `StateBasedEvaluation` tests prove `banditRefill` unchanged after the extraction.

Check the banner field names (`banners.peoplesFavor`) and the `oathkeeper` field name in `CurrentGameState` before writing; use what the model has.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.LaterGameSetupSuite"`
Expected: FAIL, no Grand Scepter on the Chancellor.

- [ ] **Step 3: Implement**

`FirstGameSetupMaterializer`: `handFor` slices by the player's index among the players with no starting advisers, and returns nothing for a player with them. `initialDiscards` uses the same slicing.

`LaterGameSetup.apply` for `plan.empire = Some(empire)`:
- **Roles.** Each seated lineage's `LineageState` keeps its record. The Chancellor's lineage gets `Role.Chancellor`. `absentChancellor` goes into `campaign.boxRoles` as `Role.Citizen`.
- **Advisers.** Each player's `advisers` become their lineage's `startingAdvisers`, facedown; the lineage's `startingAdvisers` become empty, so the card index holds each card once.
- **Purple supply.** `banks.warbandSupply(ForceKind.Imperial)` stays `MaterialBankState.ImperialWarbands` (R30). Task 3 already placed only what the supply allows on the sites.
- **Bandits on short sites.** After the steps above, fill every empty site through the same check the walker runs. Extract the site list from `StateBasedEvaluation.banditRefill` into `StateBasedEvaluation.emptySites(catalog, ready): Vector[(SiteId, Int)]` (the in-play sites with `SiteForces.Empty` and capacity above 0, each with its capacity). `banditRefill` keeps its behaviour by calling it. `LaterGameSetup` sets each listed site's forces to `SiteForces.Occupied(ForceKind.Bandit, count)` directly, as `evolve` builds the whole game; no `BanditsRefilled` event is journaled.
- **Grand Scepter.** Append `RelicState(TheGrandScepterCard.id, Orientation.FaceUp, Tokens.empty)` to the Chancellor's relics, as `ChronicleStateOperations.scepterFromBox` does. The state is built directly here, as `evolve` builds the whole game.
- **Title.** `OathkeeperState(Some(chancellor), TitleSide.Oathkeeper)`. The legacy exceptions come with Legacies.
- **Banners.** People's Favor holder is the Chancellor when the goal is `ThePeople`; Darkest Secret holder when it is `Devotion`.
- **Tracks.** `usurperLimited = false`, `roundStarter = chancellor`. The end die needs no state: `EmpireEndings` rolls it while an Empire is in play.

`GameStartRules.evolve` calls `LaterGameSetup.apply` on the `ReadyGame.start` result before `DomainValidation.validate`. Validate the result, not the pre-Empire game. If `ReadyGame.start` is not where validation reads from, reorder so `DomainValidation.validate` and the card-index check run on the final game.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "feat(setup): the Empire's players start with purple, the Scepter and the title"
```

---

### Task 5: Setup choices for a later game

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`
- Create: `src/main/scala/distantlands/gameplay/setup/SetupPawnSites.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala` (`identifiesCard`, Reliquary case)
- Modify: `src/main/scala/distantlands/application/ProcedureLog.scala` (register the new line if Setup lines are registered there)
- Test: `src/test/scala/distantlands/gameplay/setup/SetupProcedureSuite.scala` (or the existing Setup walker suite; `grep -rln "pawnDecisionId" src/test`)
- Test: `src/test/scala/distantlands/application/GamePresentationProjectorSuite.scala` (or the suite that covers `identifiesCard`)

**Interfaces:**
- Produces: `SetupProcedure.reliquaryDecisionId: String = "setup.reliquary-pick"`, `SetupProcedure.tookRelic: NoteKey` ("p1 took a relic from the Reliquary").
- Produces: `SetupPawnSites.legal(ready: ReadyGame, player: PlayerId): Vector[SiteId]`.

- [ ] **Step 1: Write the failing tests**

```scala
test("the Chancellor first keeps one Reliquary relic facedown (R29)"):
  // Later game, Reliquary holds two relics.
  val parked = startSetup(laterGameWithReliquary)
  assertEquals(parked.decisionId, SetupProcedure.reliquaryDecisionId)
  assertEquals(parked.player, p1)
  val after = answer(parked, ChooseOneAnswer(DecisionOptionRef.Relic(kept)))
  assert(player(after, p1).relics.contains(RelicState(kept,
    Orientation.FaceDown, Tokens.empty)))
  assertEquals(after.game.campaign.reliquary, Vector(other))
  assertEquals(logLines(after).head, "p1 took a relic from the Reliquary")

test("an empty Reliquary skips the pick"):
  assertEquals(startSetup(laterGame()).decisionId,
    SetupProcedure.pawnDecisionId(p1))

test("Imperial players may start only at Empire sites"):
  assertEquals(SetupPawnSites.legal(ready, p1).toSet, empireSites.toSet)

test("an Exile may start at the Hintermost Empire site or a bandit site, " +
    "never at Sunken Isles (R25)"):
  // A map with Sunken Isles among the atlas sites.
  assertEquals(SetupPawnSites.legal(ready, exile),
    Vector(empireSites.last) ++ banditSites)

test("a first game offers every site"):
  assertEquals(SetupPawnSites.legal(firstGame, p1),
    firstGame.game.current.map.inPlay)

test("a player with a starting adviser is not asked to choose one"):
  // After p2 places their pawn, the next decision is p2's reveal.
  assertEquals(afterPawn(p2).decisionId, SetupProcedure.revealDecisionId(p2))

test("only the Chancellor identifies the Reliquary's relics, and only in " +
    "Setup"):
  val presentation = GamePresentationProjector(...)
  assert(presentation.identifiesCard(setupReady, Some(p1), relic, None,
    CardContainer.Reliquary))
  assert(!presentation.identifiesCard(setupReady, Some(p2), relic, None,
    CardContainer.Reliquary))
  assert(!presentation.identifiesCard(wakeReady, Some(p1), relic, None,
    CardContainer.Reliquary))
```

Also assert that p2's projection of the parked Reliquary decision is suppressed (the decision is p1's), using the walker projection helper the existing Setup projection tests use.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.application.GamePresentationProjectorSuite"`
Expected: FAIL, the first decision is the pawn placement.

- [ ] **Step 3: Implement**

`SetupPawnSites.legal`:

```scala
/** Where a player may place their pawn in Setup (CR p. 13, ruling R25).
  * With no Empire, any site; an Imperial player, any Empire site; an
  * Exile, the Hintermost Empire site or a site the bandits rule. */
def legal(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
  val map = ready.game.current.map
  def held(kind: ForceKind)(site: SiteId) = map.sites.get(site)
    .exists(_.forces match
      case SiteForces.Occupied(`kind`, _) => true
      case _ => false)
  val empire = map.hinterward.filter(held(ForceKind.Imperial))
  if empire.isEmpty then map.inPlay
  else if Imperials.isImperial(ready, player) then empire
  else map.hinterward.filter(site =>
    empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
```

`SetupProcedure.tree`: `siteOptions` becomes per player, from `SetupPawnSites.legal`. Prepend a `reliquaryPick` step when `Imperials.chancellor(ready)` is defined:

```scala
private def reliquaryPick(chancellor: PlayerId): Operation =
  Branch((state, _) =>
    val relics = state.game.campaign.reliquary
    if relics.isEmpty then Vector.empty
    else Vector(
      Decide(reliquaryDecisionId, chancellor, DecisionQuery.ChooseOne(
        relics.map(id => DecisionOption.Relic(DecisionOptionRef.Relic(id))),
        heading = Some("Take a relic from the Reliquary"),
        confirmVerb = Some("Take"))),
      BuildOps(takeRelic(chancellor)),
      Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ =>
        Some(tookRelic.sourceless(NoteArg.Player(chancellor))))))
```

`takeRelic` reads the `ChooseOneAnswer(DecisionOptionRef.Relic(id))` and moves it from `Location.Reliquary` to `Location.PlayArea(chancellor)` with `resultingOrientation = Some(Orientation.FaceDown)`, as `ReliquaryDraws` does. Update the tree diagram in the object's comment.

Wrap the adviser `Decide`, `BuildOps` and `Note` in a `Branch` that yields nothing when the player's temporary hand is empty.

`GamePresentationProjector.identifiesCard` gains, before the fallback:

```scala
// The Chancellor chooses a Reliquary relic in Setup (CR p. 8, ruling
// R29); nobody else, and nobody later, sees the pile.
case CardContainer.Reliquary =>
  ready.game.current.turn.phase == Phase.Setup &&
    viewer.exists(Imperials.chancellor(ready).contains)
```

Check the import layering with `python3 scripts/check-architecture.py`: `application` may import `model.Imperials`.

Register `tookRelic` wherever `placedPawn` is registered (`grep -rn "placedPawn" src/main`), regenerate the note-key snapshot with `NOTE_KEYS_SNAPSHOT=write`, and read its diff.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw test`
Expected: PASS. Golden logs for first games are unchanged.

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "feat(setup): the Chancellor's Reliquary pick and role-legal pawn sites"
```

---

### Task 6: Roles on player selection

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala`
- Test: the `ChronicleEntryCodec` round-trip suite (`grep -rln "ChronicleEntryCodec" src/test shared frontend/src/test`)
- Test: `frontend/src/test/scala/distantlands/frontend/TrustedHostUiSuite.scala` (or the existing player-selection suite)

**Interfaces:**
- Produces: `ChronicleScore(lineage: String, score: Int, role: Option[String] = None)`. `role` is `"chancellor"` or `"citizen"`, absent for an Exile. The codec writes it only when present and accepts entries without it, so saved browser entries still decode.

- [ ] **Step 1: Write the failing tests**

```scala
test("a summary row carries an Imperial role and omits an Exile's"):
  val entry = ChronicleEntry("{}", "d", ChronicleSummary("C", Some(2), 20,
    Vector(ChronicleScore("red-lineage", 4, Some("chancellor")),
      ChronicleScore("blue-lineage", 2))))
  val encoded = ChronicleEntryCodec.encodeValue(entry)
  assert(!encoded("summary")("scores")(1).obj.contains("role"))
  assertEquals(ChronicleEntryCodec.decodeValue(encoded, "entry"),
    Right(entry))

test("ChronicleEntries.summary records each lineage's Imperial role"):
  // A Chronicle where red is the Chancellor.

test("player selection labels each color's role from the loaded Chronicle"):
  // Continue(saved) with red Chancellor and blue Citizen: the add-player
  // menu and the rows read "Red, Chancellor" and "Blue, Citizen".

test("player selection says the Chancellor goes first in an Empire game"):
  // The intro text replaces "the first player are chosen at random" with
  // "the Chancellor goes first" when the loaded Chronicle has one.
```

Write the frontend tests against the DOM the page builds, in the style of the existing `TrustedHostUi` tests.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly *ChronicleEntry*" frontend/test`
Expected: FAIL to compile, `ChronicleScore` takes no `role`.

- [ ] **Step 3: Implement**

- `ChronicleEntryCodec`: `exact(row, Set("lineage", "score", "role"), child)` and `optionalAbsent(row, "role", child)(string)`; refuse a role other than `chancellor` or `citizen`.
- `ChronicleEntries.summary`: `role = Option.when(lineage.role.isImperial)(lineage.role match { case Role.Chancellor => "chancellor"; case _ => "citizen" })`. Use the `Role` wire key if the model already has one (`grep -n "enum Role" -A6 src/main/scala/distantlands/model`).
- `TrustedHostUi.start`: read the roles from `chronicle` when it is `HostChronicle.Continue(saved)`, keyed by lineage id (`FirstGameBootstrapMapper.lineageOf(color)` spelling, `<color>-lineage`). Append ", Chancellor" or ", Citizen" to the color's label in the menu and the row, and to the row input's `aria-label`. Change the intro sentence as the test says. Purple stays out of `LineageColors`: purple is the Imperial warband color, not a lineage.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw test frontend/test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "feat(frontend): player selection shows each lineage's Imperial role"
```

---

### Task 7: Game 2 from game 1's Chronicle

**Files:**
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleEndToEndSuite.scala`
- Create, if a golden log is added: `src/test/resources/gamelog/later-game-setup.*` (follow the existing golden layout; `grep -rln "GAMELOG_GOLDEN" src/test`)

- [ ] **Step 1: Write the test**

In `ChronicleEndToEndSuite`, replace the assertion that the evolved record refuses "a non-empty world requires the Empire" with a scenario:

```scala
test("game 2 starts from the Chronicle game 1 journals, with the Empire"):
  val chancellor = record.lineages.find(_.role == Role.Chancellor).get
  val orders = LaterGameFixture.orders(record,
    seatOf(chancellor.id))  // the crowned winner's player
  val ready = accepted(GameStartRules.evolve(catalog, record, orders))
  assertEquals(Imperials.chancellor(ready), Some(seatOf(chancellor.id)))
  assertEquals(ready.game.current.map.hinterward.take(record.world.size),
    record.world.map(_.site))
  // Then drive Setup through OathDriver to the first Wake: Reliquary pick
  // if the record has one, each pawn at a legal site, advisers, reveals.
  val woken = driveSetup(ready)
  assertEquals(woken.game.current.turn, TurnState(seatOf(chancellor.id),
    Phase.Wake, Set.empty))
```

Use the suite's own game-1 record. Drive Setup with the same driver helpers the suite's game-1 Setup uses, answering each pawn placement with the first offered site.

Add a `later-game-setup` golden log of that Setup if the golden suites cover first-game Setup; generate it with `GAMELOG_GOLDEN=write` and read every line.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ChronicleEndToEndSuite"`
Expected: PASS. A failure here is a defect in Tasks 1 to 5; fix it there, with a unit test in that task's suite.

- [ ] **Step 3: Commit**

```bash
git add <files>
git commit -m "test(chronicle): game 2 starts from game 1's Chronicle with the Empire"
```

---

### Task 8: Docs and gates

**Files:**
- Modify: `docs/rules/ambiguities.md` (Empire rulings R25 to R30, after R24)
- Modify: `docs/rules/implementation-traceability.md` (rows "Later-game restoration from campaign/Atlas", "Ordered pawn placement on an eight-site 2/3/3 map", "Starting adviser choice, pawn placement, and Reveal Cards", "Full campaign serialization and migration", "Stars task and storage"; lines 80, 82, 83, 127 and 132 when this plan was written)
- Modify: `docs/rules/rules-reference.md` (Later games: cite R25 to R30 where a step depends on them)
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md`

- [ ] **Step 1: Rulings.** Add R25 to R30 from "Rulings for this slice" to `ambiguities.md`, each ending "(Empire slice 4)", in the style of R23 and R24.

- [ ] **Step 2: Traceability.** "Later-game restoration" becomes **Tested (bounded)**, naming `EmpireSetup`, `LaterGameSetup`, `FirstGameSetupMaterializer` and `LaterGameSetupSuite`; its remaining gaps are the Empire-less setup, stored foundation faces, legacies, starting relics and the Recent-or-Forgotten choice. The pawn and adviser rows add role-legal sites (`SetupPawnSites`), the Reliquary pick and the skipped choice for a player with starting advisers. The serialization row adds that game 2 starts from a journaled Chronicle (`ChronicleEndToEndSuite`). The Stars row adds that game start restores what Store wrote.

- [ ] **Step 3: Roadmap.** Delete the item "Slice 4, Later-game setup". In "Simultaneous setup effects", delete "later-game setup (Empire `world` sites, stored denizens and relics);". If the Empire phase has no items left but "Simultaneous Throne answers", leave that item under the phase heading. Remove any "Saved-campaign continuation" item if one exists (`grep -n -i "saved-campaign" docs/ROADMAP.md`).

- [ ] **Step 4: Spec.** Add "Slice 4 implemented, 2026-11-02." to the status line. In "Slice 4", note the rulings R25 to R30 and that the random Citizen is recorded as `firstPlayer`. Add R25 to R30 to "Rulings".

- [ ] **Step 5: Gates.** Run, with nothing else using sbt:

```bash
./sbtw clean coverage test coverageReport
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```

Expected: all pass, statement coverage at least 92%.

- [ ] **Step 6: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/implementation-traceability.md docs/rules/rules-reference.md docs/ROADMAP.md docs/superpowers/specs/2026-10-29-empire-phase-design.md
git commit -m "docs(empire): slice 4 rulings, traceability and roadmap"
```

Also wrap the overlong doc-comment line in `src/main/scala/distantlands/gameplay/chronicle/ChronicleCrown.scala` (line 41, left from slice 3b) in this commit or its own.
