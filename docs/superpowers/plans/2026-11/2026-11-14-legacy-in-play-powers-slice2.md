# Legacy In-Play Powers, Slice 2: Modifiers Implementation Plan

> Executed inline by the implementer.

**Goal:** Six legacy powers change rules a player already uses. Ancestral
Lands caps an Imperial holder's travel to the Cradle at 1 Supply.
Pathfinder's holder ignores the Mountain, Island and Pass travel powers.
Keeper of Order is a battle plan for an Imperial against an Imperial: ±2
attack dice, and an attacker's skulls ignored. Light Fingers' holder takes
wealth beside an enemy pawn. Beloved's holder trades on other players'
faceup advisers at their site. High Priest's holder may give secrets in a
deal. The Reliquary's backs in the shared bank are sized to the banners.

**Architecture:** Ruling 4's hybrid. The passive powers are
`ContributingPower`s whose `applicable` asks `ChronicleLegacyRules.holder`:
Ancestral Lands is a late `Transform` at `TravelCost`, and Pathfinder a
power that hooks `TravelCost` and `TravelActionEligibility` only to cast
its named ignore against the terrain and Pass powers there. Take Wealth's
pawn gate leaves `TakeWealthProcedure.build` for a new `Restriction`
power, `TakeWealthPawnGate`, which Light Fingers ignores the same way.
Beloved widens `TradeSourceSelection` as Skilled Merchants does, and
`MusterSource` learns to find another player's adviser. Keeper of Order is
a `BattlePlan` with a new `CampaignPlanSource.Legacy`, asked ±2 as
Mountain Giant asks 1 or 3, and scored by `AttackScore` as Outriders is.
High Priest's deal term is a new one-way `DealRules.secretGivers`, stored
as a new Negotiation start-fact list so a resumed deal keeps it.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), Scala.js
frontend in jsdom, compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy in-play powers design](../../specs/2026-11/2026-11-14-legacy-in-play-powers-design.md),
rulings 1-5 and 14-19 and section "Slice 2: modifiers".
[Implementation rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md).
Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Ancestral Lands L08, Keeper of Order L11, Light Fingers L27, Beloved L33,
Pathfinder L36) and `gameplay/powers/legacies/HighPriest.scala` (L21). The
rules: Travel and its terrain (`docs/rules/rules-reference.md#travel---1-to-4-supply-before-modifiers`),
Trade (`#trade---1-supply`), Take Wealth (`#wake`), Negotiation (`#minor-actions`),
Imperial Infighting and Imperial Tactics (`#imperial-laws`).

## Prerequisite

Slice 1 has landed (through `1dfaa1ee0`) and its review fixes are in.
Check before Task 1:

```bash
grep -q "object LegacyActionPowers" src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala &&
grep -q "object AncestralLandsCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "case RuleSourceRef.Legacy(_, id) =>" src/main/scala/distantlands/gameplay/phases/PhasePowerProcedure.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-14 as implementation
rulings R6-R9 ([rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md)):

- **R6** (Task 2): Pathfinder also ignores Lost Lagoon, the Island power
  at its site.
- **R7** (Task 5): choosing Keeper of Order asks a follow-up, "Add 2
  attack dice" or "Remove 2 attack dice", as Mountain Giant does.
- **R8** (Tasks 1-6): Ancestral Lands, Pathfinder, Light Fingers and
  Beloved write no lines; Keeper of Order writes "Added 2 attack dice."
  or "Removed 2 attack dice.", and Outriders' "Skulls ignored." when it
  ignores a skull; High Priest's secrets show in the deal's own lines.
- **R9** (Task 6): a Negotiation's start facts saved before this slice
  decode with an empty High Priest list, and a test decodes such facts.

Each Travel, Trade and Campaign test builds its table as a named fixture
with concrete sites and pieces in the test itself.

## Plan choices

- Ancestral Lands is a `Normal` transform at a priority after Special
  Envoy's (`SpecialEnvoy.Priority + 1`), so it caps whatever Supply every
  other Travel power left (`amount.min(1)`) and never raises a cost
  another made lower or removed. Way Station folds `Late` after it and
  can only lower the cost further, which ruling 14 allows.
- Pathfinder needs a contribution at each window to vote there: a
  `Transform` that returns its operations unchanged at `TravelCost`, and a
  `Restriction` that never refuses at `TravelActionEligibility`. It does
  not hook `CampaignTargetSelection`, so the Pass still limits a campaign.
- The pawn gate is a game rule, `TakeWealthPawnGate`, id
  `rule.take-wealth.enemy-pawns` (no card prefix, so `CardCatalogSuite`
  does not ask for a printed power), refusing with the same
  `OathViolation.EnemyPawnBlocksTakeWealth` the build gave, so the Wake
  menu's reason is unchanged. It is registered beside `TakeWealthLimit`.
- Beloved reads "your site" as Skilled Merchants reads "your region":
  from `PowerAccess.presentSite`. Its card moves to `gameplay/powers/economy`
  so it may use `EconomySources` (package-private there).
- Keeper of Order's card and plan live in `gameplay/powers/campaign`, as
  every plan does, since `PlanContext` internals and
  `Outriders.ignoreSkulls` are package-private there.
- `HighPriestCard` moves from `gameplay/powers/legacies/HighPriest.scala`
  to `gameplay/actions/negotiation/NegotiationLegacyCards.scala`, as
  Council Arbiter's card sits there: the Negotiation reads it at its
  start, and `gameplay/actions` may not import `gameplay/powers`. The
  ACTION in `HighPriest.scala` imports the card back.
- Lending (ruling 5): an Imperial ally's active legacy is a plan source
  their saved Imperial Tactics offer lends, exactly as their faceup
  adviser is; the offer is the holder's consent.

## Deviations from the spec

- `HighPriestCard`'s move, above.
- The spec's Approach says `MusterSource` "learns to locate another
  player's adviser"; it also gains the adviser's origin
  (`RuleSourceRef.Adviser(owner, id)`) so a cost on it is placed on that
  player's card.

- Take Wealth's enemy-pawn gate, now a restriction, refuses after the
  build's resource check: with an enemy pawn there and the resource
  missing, the refusal is `ResourceUnavailable` (R16, from the review).
- Pathfinder ignores Lost Lagoon only as the Island add on arriving at its
  site, never its Coast power on leaving (R15, from the review).

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches. A new `CampaignPlanSource` case must be handled
  at every match the compiler names.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`; `gameplay/actions` never imports
  `gameplay/powers`.
- A new test names cards by their card objects (`PathfinderCard`).
- Only seated players' active legacies act (ruling 1); "if you are an
  Imperial" is read when the power applies, through
  `ChronicleLegacyRules.holder` with `ChronicleLegacyRules.imperialRoles`
  (ruling 2).
- No `Branch`'s children or ids depend on state the walk itself changes;
  every new `Decide` has a resume test; no two decisions share an id.
- Old saved streams still decode: no journaled wire format changes;
  Negotiation start facts without the new list read as empty (R9).
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. One commit per task.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- A rewritten snapshot or golden file is reviewed line by line and
  committed in the task that changes it. The two power snapshots
  (`src/test/resources/notes/declared-keys.txt`,
  `src/test/resources/powers/unimplemented.txt`) change in each task that
  implements a card: regenerate with `NOTE_KEYS_SNAPSHOT=write
  UNIMPLEMENTED_SNAPSHOT=write` and review. `git status --short
  src/test/resources` is otherwise clean.
- A card that leaves holding changes the imports of the test files that
  name it (`ResolveLegaciesSuite`, `OfferCitizenshipSuite`,
  `IronHandSuite`, `LegacyIdentitySuite`, `PhasePowerSuite`); each task
  updates them.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files and commit with an explicit pathspec; run `git log -3`
  after each commit.

## The gate

Run after the last task, the two sbt commands in the background:

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes; coverage at or above 92% statements and
84.4% branches; both scripts pass; the last command prints nothing.

---

### Task 1: Ancestral Lands

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/travel/AncestralLands.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TravelModifiers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `AncestralLandsCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (the travel package is already imported)
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`
  (import)
- Test: `src/test/scala/distantlands/gameplay/powers/travel/AncestralLandsSuite.scala`

**Interfaces:**
- Produces: `AncestralLandsCard` (L08); `case object AncestralLands
  extends ContributingPower` with `Priority: Int = SpecialEnvoy.Priority + 1`.

- [ ] **Step 1: Write the failing test**

Read `src/test/scala/distantlands/gameplay/powers/travel/` for the Travel
harness the Coast and Royal Stables suites use (`TravelPlay`,
`MovementFixture` or the suite's own `travel(...)` helper) and use it.
The tests, each on `Table.start` with p1 in Act, a pawn in the
Provinces or Hinterland and 7 Supply:

```scala
/** Ancestral Lands (L08): an Imperial holder's travel to the Cradle costs
  * 1 Supply (Legacy in-play powers ruling 14). */
class AncestralLandsSuite extends munit.FunSuite:
  test("an Imperial holder reaches the Cradle for 1 Supply, even from a " +
      "Mountain route"):
    // p1 Chancellor holding Ancestral Lands at Broken Peaks (a Mountain,
    // which adds 1 when arriving there, not here) travels to Ancient City
    // (Cradle) from the Hinterland: the base cost and any add become 1.
    assertEquals(supplySpent(imperialHolder, to = AncientCitySite), 1)

  test("it never raises a cost another power made lower: Special Envoy " +
      "still spends none"):
    assertEquals(supplySpent(imperialHolder.adviser(p1, SpecialEnvoyCard),
      to = AncientCitySite, modifiers = Vector(SpecialEnvoy.id)), 0)

  test("an Exile holder pays the printed cost (ruling 2)"):
    assertEquals(supplySpent(exileHolder, to = AncientCitySite),
      printedCost(exileHolder, AncientCitySite))

  test("travel outside the Cradle is unchanged"):
    assertEquals(supplySpent(imperialHolder, to = DunesSite),
      printedCost(imperialHolder, DunesSite))

  test("a Pass still restricts: only the cost changes"):
    // NarrowPass in the Cradle, ruled by p2: p1 is refused as without
    // the legacy.
    assertEquals(travelTo(passTable, someOtherCradleSite).left.toOption
      .map(_.getClass), Some(classOf[OathViolation.TravelPassBlocked]))
```

`supplySpent` reads the walk's `SpendSupply` amounts for p1
(`RecordedOps.all`), `printedCost` is the same Travel without the legacy.
Build the Cradle and Mountain sites with `Table.sitesInPlay` or the
fixture's own layout; the sites named above are examples, not fixed.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.AncestralLandsSuite"`
Expected: compile error, `AncestralLandsCard` is not in
`gameplay.powers.travel`.

- [ ] **Step 3: Implement**

`AncestralLands.scala`:

```scala
package distantlands.gameplay.powers.travel

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import distantlands.model._

object AncestralLandsCard extends Legacy(LegacyId("L08"), "Ancestral Lands",
    LegacyGoal.GrandScepter, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.ancestral-lands"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **TRAVEL:** Traveling to the " +
      "Cradle costs you only 1 Supply.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ancestral Lands (legacy L08): "If you are an Imperial: Travel:
  * Traveling to the Cradle costs you only 1 Supply" (Legacy in-play powers
  * design, ruling 14).
  *
  * Its Imperial holder's Travel to a Cradle site spends at most 1 Supply.
  * It folds after every other Travel power, Special Envoy included, so it
  * caps what they leave and never raises a cost one of them made lower or
  * removed. Way Station folds `Late` and can only lower it further. Only
  * the cost changes: a Pass still restricts. It writes no line (R8).
  */
case object AncestralLands extends ContributingPower:
  val id: PowerId = AncestralLandsCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  val Priority: Int = SpecialEnvoy.Priority + 1
  override def priority: Int = AncestralLands.Priority

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      TravelRoute.adjustSupply(ctx, operations)(_.min(1)))))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, AncestralLandsCard.id,
      ChronicleLegacyRules.imperialRoles).contains(ctx.actor) &&
      TravelRoute.pawnMove(ctx.operation).exists(route =>
        route.player == ctx.actor &&
          ctx.state.game.current.map.regionOf(route.destination)
            .contains(Region.Cradle))
```

`TravelModifiers.all`: append `AncestralLands`. Remove
`AncestralLandsCard` from holding; update `ResolveLegaciesSuite`'s import.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.travel.* distantlands.gameplay.powers.*Suite distantlands.gameplay.cards.* distantlands.gameplay.chronicle.* distantlands.catalog.*"`
Expected: all pass; `unimplemented.txt` loses `legacy.ancestral-lands`,
`declared-keys.txt` is unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/travel/AncestralLands.scala \
  src/test/scala/distantlands/gameplay/powers/travel/AncestralLandsSuite.scala
git commit -m "feat(legacies): Ancestral Lands caps an Imperial holder's travel to the Cradle at 1 Supply

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/travel \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/travel/AncestralLandsSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 2: Pathfinder

Written to R6.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/travel/Pathfinder.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TravelModifiers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `PathfinderCard`)
- Test: `src/test/scala/distantlands/gameplay/powers/travel/PathfinderSuite.scala`

**Interfaces:**
- Produces: `PathfinderCard` (L36); `case object Pathfinder extends
  ContributingPower`; `Pathfinder.ignored(other: ContributingPower): Boolean`.

- [ ] **Step 1: Write the failing test**

With the same Travel harness as Task 1, p1 holding Pathfinder active:

```scala
/** Pathfinder (L36): its holder's travel ignores the Mountain, Island and
  * Pass powers (Legacy in-play powers ruling 15). */
class PathfinderSuite extends munit.FunSuite:
  test("travel to a Mountain costs no Mountain add"):
    assertEquals(supplySpent(holder, to = BrokenPeaksSite),
      supplySpent(without, to = BrokenPeaksSite) - 1)

  test("travel to an Island costs no Island add"):
    assertEquals(supplySpent(holder, to = islandSite),
      supplySpent(without, to = islandSite) - 2)

  test("Lost Lagoon's Island add is ignored too (R6)"):
    assertEquals(supplySpent(holderBesideLostLagoon, to = lagoonSite),
      supplySpent(withoutBesideLostLagoon, to = lagoonSite) - 2)

  test("a Pass ruled by another player does not stop the holder's travel"):
    assert(travelTo(passTable(holder), otherSiteInPassRegion).isRight)
    assert(travelTo(passTable(without), otherSiteInPassRegion).isLeft)

  test("the Pass still limits the holder's campaign targets"):
    // CampaignTargetSelection is not a Travel window: the other sites
    // in the Pass's region stay unavailable as targets.
    assert(!campaignTargets(passTable(holder)).contains(otherSiteInPassRegion))

  test("a dormant Pathfinder ignores nothing (ruling 1)"):
    assertEquals(supplySpent(dormant, to = BrokenPeaksSite),
      supplySpent(without, to = BrokenPeaksSite))
```

Take the island and Pass layouts from `TravelSitePowersSuite` (or the
suite that tests `NarrowPassSitePower` and `IslandSitePower`) and the
Lost Lagoon layout from `GrandCanalSuite`; `campaignTargets` from the
Pass's campaign test.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.travel.PathfinderSuite"`
Expected: compile error, `PathfinderCard` is not in `gameplay.powers.travel`.

- [ ] **Step 3: Implement**

`Pathfinder.scala` (the card object as printed, moved from holding):

```scala
/** Pathfinder (legacy L36): "Travel: You may ignore the Mountain, Island,
  * and Pass powers" (Legacy in-play powers design, ruling 15). Its
  * foundation, Wide Horizons, is the Legacies batch's.
  *
  * Its holder's Travel ignores them always, with no question, since
  * ignoring them is never worse. The ignore is the collector's named
  * ignore, so this power hooks each window those powers hook: `TravelCost`
  * (the Mountain and Island adds, Lost Lagoon's included, R6) and
  * `TravelActionEligibility` (the Pass's restriction), contributing nothing
  * there but its vote. The Pass's campaign-targeting limit hooks
  * `CampaignTargetSelection`, which this power does not, so it still
  * applies. It writes no line (R8).
  */
case object Pathfinder extends ContributingPower:
  val id: PowerId = PathfinderCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((_, operations) => operations)),
    PowerWindow.TravelActionEligibility -> Vector(
      Restriction((_, _) => None)))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, PathfinderCard.id)
      .contains(ctx.actor) &&
      TravelRoute.pawnMove(ctx.operation).exists(_.player == ctx.actor)

  /** The powers Pathfinder's holder ignores. */
  def ignored(other: ContributingPower): Boolean = other match
    case _: MountainSitePower | _: IslandSitePower | LostLagoon |
        _: NarrowPassSitePower => true
    case _ => false

  override def shouldIgnore(other: ContributingPower): Boolean =
    Pathfinder.ignored(other)
```

Imports: `distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}`,
`distantlands.gameplay.chronicle.ChronicleLegacyRules`,
`distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
PowerCtx, Restriction, Transform}`, `distantlands.model._`.

`TravelModifiers.all`: append `Pathfinder`. Remove `PathfinderCard` from
holding.

- [ ] **Step 4: Run it and see it pass**

Run as Task 1's Step 4. Expected: all pass; `unimplemented.txt` loses
`legacy.pathfinder`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/travel/Pathfinder.scala \
  src/test/scala/distantlands/gameplay/powers/travel/PathfinderSuite.scala
git commit -m "feat(legacies): Pathfinder's holder ignores the Mountain, Island and Pass travel powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/travel \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/travel/PathfinderSuite.scala \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 3: Take Wealth's pawn gate and Light Fingers

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/wake/LightFingers.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/wake/TakeWealthPowers.scala`
  (add `TakeWealthPawnGate`)
- Modify: `src/main/scala/distantlands/gameplay/phases/wake/TakeWealthProcedure.scala`
  (drop `noEnemyPawn` from `build`; its doc names the new restriction)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
  (register both beside `TakeWealthLimit`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `LightFingersCard`)
- Test: `src/test/scala/distantlands/gameplay/TakeWealthProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/wake/LightFingersSuite.scala`

**Interfaces:**
- Produces: `case object TakeWealthPawnGate extends ContributingPower`
  (`id = PowerId("rule.take-wealth.enemy-pawns")`), refusing with
  `OathViolation.EnemyPawnBlocksTakeWealth(site, enemies)`;
  `LightFingersCard` (L27); `case object LightFingers extends
  ContributingPower`.

- [ ] **Step 1: Write the failing tests**

In `TakeWealthProcedureSuite`, beside "an enemy pawn at the site blocks
the take":

```scala
  test("the enemy pawn gate is a walker restriction, not the build's"):
    // Built with no walker powers, the take goes through: the gate is
    // TakeWealthPawnGate's (Legacy in-play powers ruling 17).
    val bare = new OathRules(catalog, walkerPowerCatalog = WalkerPowers.empty)
    assert(bare.startWalker(OathState.Ready(wake(favor = 1, secrets = 1,
      sharedEnemy = true)), ActionRef.TakeWealth, actor,
      TakeWealthProcedure.selection(WakeResource.Favor)).isRight)
```

`LightFingersSuite` (on `TakeWealthProcedureSuite`'s `wake` table shape:
p1 in Wake at a site with favor, p2's pawn there too):

```scala
/** Light Fingers (L27): its holder takes wealth beside an enemy pawn
  * (Legacy in-play powers ruling 17). */
class LightFingersSuite extends munit.FunSuite:
  test("its holder takes a favor from a site where an enemy pawn stands"):
    val done = accepted(take(withEnemy.legacy(p1, LightFingersCard).ready))
    assertEquals(Look(after(done)).favor(p1), before + 1)

  test("its Wake menu offers Take Wealth there"):
    assert(wealthOffered(withEnemy.legacy(p1, LightFingersCard).ready))
    assert(!wealthOffered(withEnemy.ready))

  test("a dormant Light Fingers, or another player's, opens nothing"):
    assertEquals(take(withEnemy.legacy(p1, LightFingersCard,
      active = false).ready).left.toOption.map(_.getClass),
      Some(classOf[OathViolation.EnemyPawnBlocksTakeWealth]))
    assert(take(withEnemy.legacy(p2, LightFingersCard).ready).isLeft)

  test("the once-per-site limit still holds"):
    // A second take at the same site this turn is refused, as without it.
```

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.TakeWealthProcedureSuite distantlands.gameplay.powers.wake.LightFingersSuite"`
Expected: the gate test fails (the build refuses); `LightFingersSuite`
does not compile.

- [ ] **Step 3: Implement**

`TakeWealthPowers.scala`, after `TakeWealthLimit`:

```scala
/** Take Wealth's "no enemy pawns at your site" (Legacy in-play powers
  * ruling 17), a `Restriction` at the action's window as the once-per-site
  * limit is, so a power may ignore it: Light Fingers' holder does. It
  * refuses with the reason the build gave, so the Wake menu's words are
  * unchanged. A game rule, not a printed power. */
case object TakeWealthPawnGate extends ContributingPower:
  val id: PowerId = PowerId("rule.take-wealth.enemy-pawns")
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.WakeTakeWealth -> Vector(
      Restriction((ctx, _) => blocked(ctx))))

  private def blocked(ctx: PowerCtx): Option[OathViolation] =
    val current = ctx.state.game.current
    current.pawnSite(ctx.actor).flatMap { site =>
      val enemies = current.players.collect {
        case other if other.player != ctx.actor &&
            other.pawnSite.contains(site) => other.player }
      Option.when(enemies.nonEmpty)(
        OathViolation.EnemyPawnBlocksTakeWealth(site, enemies))
    }
```

`LightFingers.scala` (card object as printed, moved from holding):

```scala
/** Light Fingers (legacy L27): "Wake: When you Take Wealth, you may take
  * favor or secrets from your site even if enemy pawns are there" (Legacy
  * in-play powers design, ruling 17). Its holder's Take Wealth ignores
  * `TakeWealthPawnGate`, always and with no question; it hooks the gate's
  * window only to cast that ignore. It writes no line (R8). */
case object LightFingers extends ContributingPower:
  val id: PowerId = LightFingersCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.WakeTakeWealth -> Vector(Restriction((_, _) => None)))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, LightFingersCard.id)
      .contains(ctx.actor)

  override def shouldIgnore(other: ContributingPower): Boolean =
    other == TakeWealthPawnGate
```

`TakeWealthProcedure.build`: drop `_ <- noEnemyPawn(state, actor, site)`
and the private `noEnemyPawn`; the scaladoc's gate list says the enemy
pawn rule is `TakeWealthPawnGate`'s. `WalkerPowerCatalog.contributing`:
`TakeWealthLimit, TakeWealthPawnGate, LightFingers`. Remove
`LightFingersCard` from holding.

- [ ] **Step 4: Run them and see them pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.catalog.* distantlands.application.gamelog.*"`
Expected: all pass, the Wake menu suites and the `take-wealth` and
`menu-wealth` goldens unchanged; `unimplemented.txt` loses
`legacy.light-fingers`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/wake/LightFingers.scala \
  src/test/scala/distantlands/gameplay/powers/wake/LightFingersSuite.scala
git commit -m "feat(legacies): Take Wealth's pawn gate becomes a restriction Light Fingers ignores

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/wake \
  src/main/scala/distantlands/gameplay/phases/wake/TakeWealthProcedure.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/TakeWealthProcedureSuite.scala \
  src/test/scala/distantlands/gameplay/powers/wake/LightFingersSuite.scala \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 4: Beloved

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/economy/Beloved.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/economy/MusterSource.scala`
  (`locate` finds another player's adviser)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
  (register `Beloved`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `BelovedCard`)
- Test: `src/test/scala/distantlands/gameplay/powers/economy/BelovedSuite.scala`

**Interfaces:**
- Produces: `BelovedCard` (L33); `case object Beloved extends
  ContributingPower`; `Beloved.advisersAtSite(catalog: ExecutableCatalog,
  ready: ReadyGame, actor: PlayerId): Vector[SiteDenizenState]`.

- [ ] **Step 1: Write the failing test**

On the Trade harness of `SkilledMerchantsSuite` (read it first): p1 in
Act at Ancient City with a secret, holding Beloved active; p2's pawn at
Ancient City with a faceup Arcane adviser; p3 elsewhere with a faceup
adviser.

```scala
/** Beloved (L33): its holder trades on the faceup advisers of other
  * players at their site (Legacy in-play powers ruling 18). */
class BelovedSuite extends munit.FunSuite:
  test("the Trade's sources add p2's faceup adviser, not p3's"):
    assertEquals(sourceRefs(trading(holder)).filter(isAdviser),
      Vector(DecisionOptionRef.Denizen(p2Adviser)))

  test("trading on it places the secret on p2's adviser, takes favor from " +
      "its suit's bank, and counts the trader's own matching advisers"):
    val done = tradeFor(holder, DecisionOptionRef.Denizen(p2Adviser))
    assertEquals(Look(after(done)).adviserTokens(p2, p2Adviser), Tokens(0, 1))
    assertEquals(Look(after(done)).favor(p1), before + 1 + p1MatchingAdvisers)

  test("a facedown adviser is not a source"):
    assert(!sourceRefs(trading(holderBesideFacedown)).contains(
      DecisionOptionRef.Denizen(p2Adviser)))

  test("an adviser holding tokens is shown, unavailable, as at a site"):
    // The token-free rule (MusterSource.Occupied).

  test("without the legacy, or with it dormant, nothing is added (ruling 1)"):
    assert(sourceRefs(trading(without)).forall(!isAdviser(_)))
    assert(sourceRefs(trading(dormant)).forall(!isAdviser(_)))

  test("a walk resumed at the source decision trades on p2's adviser"):
    // Replay the started events, answer on the replayed state.
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.BelovedSuite"`
Expected: compile error, `BelovedCard` is not in `gameplay.powers.economy`.

- [ ] **Step 3: Implement**

`MusterSource.locate`: after the actor's own advisers, the other players'
faceup denizen advisers, in seat order, with origin
`RuleSourceRef.Adviser(owner, id)`:

```scala
    val others = current.players.iterator
      .filter(_.player != actor).flatMap(owner => owner.advisers.collect {
        case card @ DenizenState(id2, Orientation.FaceUp, _) if id2 == id =>
          Located(card, RuleSourceRef.Adviser(owner.player, card.id))
      })
    (atSites ++ held ++ others).nextOption()
```

Its doc gains: "or among another player's faceup advisers, which only
Beloved adds to the decision (Legacy in-play powers ruling 18)."

`Beloved.scala` (card object as printed, moved from holding):

```scala
/** Beloved (legacy L33), its WHEN TRADING power: "You may trade with
  * advisers held by other players whose pawns are at your site" (Legacy
  * in-play powers design, ruling 18). Its foundation is the Legacies
  * batch's.
  *
  * Automatic for its holder, as no one is asked: the Trade's sources widen
  * to the faceup denizen advisers of every other player whose pawn stands
  * at the site the holder acts from, after the site's own cards
  * (`EconomySources.widened`). A facedown adviser is not offered, since its
  * suit is hidden, and one holding tokens is shown unavailable, as at a
  * site. Trade's own rules are unchanged: the cost goes on that adviser,
  * the favor comes from its suit's bank, and the bonus counts the trader's
  * own matching advisers. Tokens leave the card at its holder's Rest. It
  * writes no line: the Trade's line names the card (R8).
  */
case object Beloved extends ContributingPower:
  val id: PowerId = BelovedCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TradeSourceSelection -> Vector(Transform((ctx, operations) =>
      EconomySources.widened(ctx, operations,
        advisersAtSite(ctx.catalog, ctx.state, ctx.actor)))))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, BelovedCard.id).contains(ctx.actor)

  /** The faceup denizen advisers, with a suit, of every other player whose
    * pawn stands where `actor` acts from, in seat then adviser order. */
  def advisersAtSite(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[SiteDenizenState] =
    PowerAccess.presentSite(ready, actor).toVector.flatMap(site =>
      ready.game.current.players.filter(other => other.player != actor &&
        other.pawnSite.contains(site)).flatMap(_.advisers.collect {
          case card @ DenizenState(_, Orientation.FaceUp, _)
              if catalog.suitOf(card.id).nonEmpty => card
        }))
```

`EconomySources.widened` takes `Vector[SiteDenizenState]`;
`DenizenState` is one. Register `Beloved` in
`WalkerPowerCatalog.contributing` after `TakeWealthPawnGate, LightFingers`.
Remove `BelovedCard` from holding.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.economy.* distantlands.gameplay.* distantlands.catalog.*"`
Expected: all pass; `unimplemented.txt` loses `legacy.beloved`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/economy/Beloved.scala \
  src/test/scala/distantlands/gameplay/powers/economy/BelovedSuite.scala
git commit -m "feat(legacies): Beloved's holder trades on other players' faceup advisers at their site

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/economy/Beloved.scala \
  src/main/scala/distantlands/gameplay/actions/economy/MusterSource.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/economy/BelovedSuite.scala \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 5: Keeper of Order and the legacy plan source

Written to R7 and R8.

**Files:**
- Modify: `src/main/scala/distantlands/model/CampaignTypes.scala`
  (`CampaignPlanSource.Legacy`)
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlans.scala`
  (`cardOf`, `order`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PlanContext.scala`
  (`legacy`, `lentBy`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/AttackScore.scala`
  (`changes`)
- Create: `src/main/scala/distantlands/gameplay/powers/campaign/KeeperOfOrder.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PlanRules.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `KeeperOfOrderCard`)
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`
  (import)
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/KeeperOfOrderSuite.scala`

**Interfaces:**
- Produces:
  - `CampaignPlanSource.Legacy(playerId: PlayerId, id: LegacyId)`, its
    `stableKey` `legacy:<player>:<id>`, its `ref`
    `DecisionOptionRef.Legacy(id)`.
  - `PlanContext.legacy(id: LegacyId): Option[CampaignPlanSource]`.
  - `KeeperOfOrderCard` (L11); `case object KeeperOfOrder extends
    BattlePlan` with `decisionId = CampaignProcedure.decisionPrefix +
    "keeper-of-order"`, `Dice: Int = 2`.

- [ ] **Step 1: Write the failing test**

On `ImperialTacticsSuite`'s fixture (`CampaignFixture`, `PlanDriver`):

```scala
/** Keeper of Order (L11): an Imperial's battle plan against an Imperial,
  * ±2 attack dice, and an attacker's skulls ignored (Legacy in-play powers
  * rulings 5 and 16). */
class KeeperOfOrderSuite extends munit.FunSuite:
  private val keeper: DecisionOptionRef =
    DecisionOptionRef.Legacy(KeeperOfOrderCard.id)
  private val add = PlanAmounts.button(KeeperOfOrder.Dice)
  private val remove = PlanAmounts.button(-KeeperOfOrder.Dice)

  /** p1, a Citizen holding Keeper of Order active, raids p3, a Citizen,
    * at the origin; p2 is the Chancellor. */
  private def infighting: Board =
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, p1, p3).oathkeeper(None))
    on(b)(_.pawn(p3, at = b.origin).boardWarbands(p3, 2)
      .legacy(p1, KeeperOfOrderCard))

  test("an Imperial attacker against an Imperial is offered it, and " +
      "chooses add or remove"):
    val run = commit(rules(winningDice), infighting, 4, raid = true)
      .pick(p1, CampaignIds.attackerPlan, keeper)
    assertEquals(run.query(p1), DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(add, "Add 2 attack dice"),
      DecisionOption.Button(remove, "Remove 2 attack dice")),
      heading = Some("Keeper of Order: add or remove 2 attack dice?")))

  test("adding puts two dice in the pool, and its skulls are ignored"):
    val run = commit(rules(skullDice), infighting, 4, raid = true)
      .pick(p1, CampaignIds.attackerPlan, keeper)
      .pick(p1, KeeperOfOrder.decisionId, add)
    assert(run.ops.contains(ModifyDicePool(CampaignIds.attackPool, 2)))
    val done = run.finishPlanWindows
    assertEquals(done.result.skullLosses, 0)
    assert(said(KeeperOfOrder, done.events).exists(_.key == "ignored"))

  test("a defender removes two, and its skulls rule does not apply"):
    // p3 holds it; p1 attacks; p3's plan removes 2 attack dice; the
    // attacker's skulls still kill.

  test("not offered against an Exile or the bandits, or by an Exile"):
    // p3 an Exile; a Conquest of a bandit site; p1 an Exile holder.

  test("another Imperial uses it through Imperial Tactics with the " +
      "holder's offer (ruling 5)"):
    // p3 defends, p2 holds Keeper of Order and offers it
    // (AidOffer(plans = Vector(keeper))); p3 chooses it, lent by p2.

  test("a walk resumed at its question applies the chosen sign"):
    // Replay the events to the parked question and answer remove.
```

`skullDice` is a `WalkerDice` rolling a skull face for the attack: take
it from `OutridersSuite`.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.campaign.KeeperOfOrderSuite"`
Expected: compile error, `KeeperOfOrderCard` is not in
`gameplay.powers.campaign`.

- [ ] **Step 3: Implement**

`CampaignTypes.scala`, in `CampaignPlanSource`:

```scala
  /** A legacy in its holder's play area (Legacy in-play powers ruling
    * 5). */
  final case class Legacy(playerId: PlayerId, id: LegacyId)
      extends CampaignPlanSource:
    def stableKey: String = s"legacy:${playerId.value}:${id.value}"
    def ref: DecisionOptionRef = DecisionOptionRef.Legacy(id)
```

`CampaignPlans.cardOf`: `case CampaignPlanSource.Legacy(_, id) =>
Some(id)`; `order`: `case _: CampaignPlanSource.Legacy => 150` (after
advisers, before relics: the play area's cards together). Every other
match on `CampaignPlanSource` the compiler names gains its case:
`PlanContext.lentBy` returns `Some(player)`;
`CampaignPlanApplication.reveal` reveals nothing (an active legacy is
faceup).

`PlanContext.legacy`:

```scala
  /** A legacy the user's lineage holds active, else an Imperial ally's
    * the ally lends (Legacy in-play powers ruling 5): an active legacy is
    * in its holder's play area, as a faceup adviser is. */
  def legacy(id: LegacyId): Option[CampaignPlanSource] =
    val ref = DecisionOptionRef.Legacy(id)
    def holds(player: PlayerId) =
      ChronicleLegacyRules.holder(ready, id).contains(player)
    user.filter(holds).map(CampaignPlanSource.Legacy(_, id))
      .orElse(lenders.find(ally => holds(ally) && lends(ally, ref))
        .map(CampaignPlanSource.Legacy(_, id)))
```

`KeeperOfOrder.scala` (card object as printed, moved from holding):

```scala
/** Keeper of Order (legacy L11): "If you are an Imperial: Battle plan: If
  * your enemy is an Imperial, ±2 attack dice and you ignore all skulls you
  * roll when attacking" (Legacy in-play powers design, ruling 16).
  *
  * A plan for either side, offered while its user is an Imperial and the
  * enemy an Imperial player, as in Imperial Infighting; bandits are never
  * an Imperial. Its user is the holder, or another Imperial it is lent to
  * through Imperial Tactics (ruling 5). Once picked, its user chooses to
  * add 2 attack dice or remove 2 (R7), whichever side they are on. When
  * its user attacks, every skull they roll is ignored, rescored as
  * Outriders rescores. It is used once per Campaign, as every plan is.
  *
  * Lines (R8): "Added 2 attack dice." or "Removed 2 attack dice.", and
  * Outriders' "Skulls ignored." when it ignores a skull.
  */
case object KeeperOfOrder extends BattlePlan:
  val id: PowerId = KeeperOfOrderCard.power.id
  val Dice: Int = 2
  val decisionId: String = CampaignProcedure.decisionPrefix +
    "keeper-of-order"
  def cardRef: DecisionOptionRef = DecisionOptionRef.Legacy(KeeperOfOrderCard.id)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  private val notes = NoteSet(id)
  notes.include(Outriders.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Added 2 attack dice." */
  val added: NoteKey = notes.key("added", Vector(NotePart.Text("Added "),
    NotePart.Arg(0), NotePart.Plural(0, " attack die.", " attack dice.")))
  /** "Removed 2 attack dice." */
  val removed: NoteKey = notes.key("removed", Vector(
    NotePart.Text("Removed "), NotePart.Arg(0),
    NotePart.Plural(0, " attack die.", " attack dice.")))

  private def source: PowerSourceRef = PowerSourceRef.Card(KeeperOfOrderCard.id)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    def imperial(player: PlayerId) = Imperials.isImperial(context.ready, player)
    val enemyImperial = context.enemy match
      case CampaignDefender.Player(enemy) => imperial(enemy)
      case CampaignDefender.Bandits => false
    for
      user <- context.user
      if imperial(user) && enemyImperial
      held <- context.legacy(KeeperOfOrderCard.id)
    yield CampaignPlanOffer(held,
      s"Keeper of Order: add or remove $Dice attack dice", Vector.empty,
      Vector.empty, amount = Some(PlanAmounts.oneOf(decisionId,
        s"Keeper of Order: add or remove $Dice attack dice?", Vector(
          Dice -> s"Add $Dice attack dice",
          -Dice -> s"Remove $Dice attack dice"))(signed => Vector(
        if signed > 0 then CampaignPlanEffect.AddAttackDice(Dice)
        else CampaignPlanEffect.RemoveAttackDice(Dice),
        CampaignPlanEffect.Run(Vector(Note(id, _ => Some(
          (if signed > 0 then added else removed)(source,
            NoteArg.Number(Dice))))))))))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignAttackResult -> (use =>
      if use.side == CampaignPlanSide.Attacker then
        Outriders.ignoreSkulls(use, id, source)
      else Vector.empty))
```

`AttackScore.changes`: add
`AttackChange(DecisionOptionRef.Legacy(KeeperOfOrderCard.id),
CampaignPlanSide.Attacker, _.copy(skullsIgnored = true))`, and the
doc's list of `skullsIgnored` plans names Keeper of Order. `PlanRules.all`:
append `KeeperOfOrder`, and its doc names it ("Keeper of Order (the attack
scored again)"). Remove `KeeperOfOrderCard` from holding; update
`ResolveLegaciesSuite`'s import.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.campaign.* distantlands.gameplay.* distantlands.application.* distantlands.catalog.*"`
Expected: all pass; `declared-keys.txt` gains `legacy.keeper-of-order`'s
three lines, `unimplemented.txt` loses it.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/campaign/KeeperOfOrder.scala \
  src/test/scala/distantlands/gameplay/powers/campaign/KeeperOfOrderSuite.scala
git commit -m "feat(legacies): Keeper of Order, a legacy battle plan for Imperial Infighting

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/model/CampaignTypes.scala \
  src/main/scala/distantlands/gameplay/actions/campaign \
  src/main/scala/distantlands/gameplay/powers/campaign \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/campaign/KeeperOfOrderSuite.scala \
  src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 6: High Priest, when negotiating

Written to R8 and R9.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationLegacyCards.scala`
  (`HighPriestCard`, moved)
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/HighPriest.scala`
  (import the card; doc)
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`
  (`secretGivers`)
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`
  (`highPriestUsers`, its marker, tolerant read)
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdvisers.scala`
  (`highPriestUsers`)
- Modify: test files importing `HighPriestCard` (`HighPriestSuite`,
  `LegacyActionLogScripts`)
- Test: `src/test/scala/distantlands/gameplay/NegotiationSecretTermsSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala`

**Interfaces:**
- Produces: `DealRules.secretGivers: Vector[PlayerId] = Vector.empty`;
  `NegotiationStartFacts.highPriestUsers: Vector[PlayerId] = Vector.empty`
  and `NegotiationStartFacts.HighPriestMarker =
  "negotiation.high-priest-users"`;
  `NegotiationAdvisers.highPriestUsers(catalog, ready, players,
  restrictions): Vector[PlayerId]`.

- [ ] **Step 1: Write the failing tests**

`NegotiationSecretTermsSuite`:

```scala
  test("High Priest's holder may give secrets; the other side may not " +
      "(Legacy in-play powers ruling 19)"):
    val oneWay = NegotiationDeal.snapshot(catalog, withSecrets().ready,
      dealOf(), DealRules(secretGivers = Vector(p1)), AdviserLimit)
    assertEquals(oneWay.bounds(p1).secretRecipients, Vector(p2))
    assertEquals(oneWay.bounds(p2).secretRecipients, Vector.empty[PlayerId])
    assert(offered(oneWay, 2).isRight)

  test("a Negotiation started by High Priest's holder opens their secrets, " +
      "and a resumed deal keeps them"):
    // Table.start with p1 holding High Priest active and p2 at p1's site:
    // start the Negotiation, read the parked Negotiate's bounds (p1 gives
    // secrets, p2 not), replay the events, read them again.
```

`NegotiationStartFactsSuite`:

```scala
  test("High Priest's users round trip, and open one-way secret terms"):
    val facts = NegotiationStartFacts(Vector(p2), Vector.empty,
      highPriestUsers = Vector(p1))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p2)).secretGivers, Vector(p1))

  test("facts written before High Priest's list read it as empty (R9)"):
    val before = NegotiationStartFacts(Vector(p2), Vector.empty).refs
      .filterNot(_ == DecisionOptionRef.Button(
        NegotiationStartFacts.HighPriestMarker))
    assertEquals(NegotiationStartFacts.read(before).map(_.highPriestUsers),
      Right(Vector.empty))

  test("only an active, seated holder among the players is a user"):
    // NegotiationAdvisers.highPriestUsers on Table.start: p1 holding it
    // active is; dormant, outside `players`, or boxed is not.
```

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationSecretTermsSuite distantlands.gameplay.NegotiationStartFactsSuite"`
Expected: compile errors, `secretGivers` and `highPriestUsers` are not
members.

- [ ] **Step 3: Implement**

`DealRules`: a field `secretGivers: Vector[PlayerId] = Vector.empty` after
`secretsOpen`, and in `kinds`:
`Option.when(secretsOpen || secretGivers.contains(author))(TermKind.Secret)`.
Its doc: "`secretGivers` may give secrets to anyone, one way (High
Priest, Legacy in-play powers ruling 19)."

`NegotiationLegacyCards.scala` holds `HighPriestCard`, moved unchanged
from `HighPriest.scala`, with a file doc: "The legacy cards the
Negotiation reads at its start, beside the action (as Council Arbiter's
card is): its ACTION is `gameplay/powers/legacies/HighPriest`."

`NegotiationAdvisers.highPriestUsers`:

```scala
  /** The players among `players` holding High Priest active, who may give
    * secrets in a deal (Legacy in-play powers ruling 19): at most one. */
  def highPriestUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    ChronicleLegacyRules.holder(ready, HighPriestCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          HighPriestCard.power, HighPriestCard.id), restrictions))
```

`NegotiationStartFacts`: the field `highPriestUsers: Vector[PlayerId] =
Vector.empty` last; `HighPriestMarker` appended to `Markers`; `refs`
zips it in; `at` fills it with `NegotiationAdvisers.highPriestUsers`;
`rules` passes `secretGivers = highPriestUsers.filter(participants.contains)`;
`read` appends the marker when `refs` lacks it, then reads as now
(R9), and builds `lists(5)`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.* distantlands.application.*"`
Expected: all pass, the negotiation goldens unchanged (no deal in them
holds High Priest).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationLegacyCards.scala
git commit -m "feat(legacies): High Priest's holder may give secrets in a deal, one way

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/actions/negotiation \
  src/main/scala/distantlands/gameplay/powers/legacies/HighPriest.scala \
  src/test/scala/distantlands/gameplay \
  src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala
git log -3
```

### Task 7: The Reliquary's backs at fit zoom (Minor)

**Files:**
- Modify: `frontend/styles.css` (the `.table-edge` rules)
- Create: `src/test/scala/distantlands/server/StylesheetTableEdgeSuite.scala`

- [ ] **Step 1: Write the failing test**

jsdom computes no layout, so pin the rule, not a pixel size, on the JVM
as `StylesheetProportionsSuite` reads the stylesheet:

```scala
package distantlands.server

import java.nio.file.{Files, Path}

/** The table's edge sizes the Reliquary as it sizes the banners (Legacy
  * in-play powers ruling 9). */
class StylesheetTableEdgeSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  test("on the table's edge the Reliquary's relics take the banners' side"):
    assert(css.contains(".table-edge .shared-reliquary { --card-w: " +
      "var(--banner-edge-side);"))

  test("at fit zoom its label takes the glance size, as the bank's do"):
    val glance = css.indexOf(".map-compact .table-edge :is(")
    assert(glance >= 0)
    assert(css.substring(glance, css.indexOf("}", glance))
      .contains(".shared-reliquary-label"))
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.server.StylesheetTableEdgeSuite"`
Expected: both tests fail.

- [ ] **Step 3: Implement**

In `styles.css`, beside `.table-edge .shared-banners`:

```css
/* The Reliquary's relics stand as large as the banners beside them,
   square as a relic is (Legacy in-play powers ruling 9). */
.table-edge .shared-reliquary { --card-w: var(--banner-edge-side);
  margin-top: 0; }
```

and add `.shared-reliquary-label` to the `.map-compact .table-edge
:is(...)` list that sets `font-size: var(--glance-text)`.

- [ ] **Step 4: Run it and see it pass, then check it in the app**

Run: `./sbtw "testOnly distantlands.server.Stylesheet*" "frontend/test"`. Then start `oath-server`, create a
later game with a non-empty Reliquary (as slice 1's check did: a saved
Chronicle through `POST /games`, opened at
`/?mode=server&gameId=<id>&playerId=p2`), and measure at fit zoom:
`.shared-reliquary .card-face`'s box matches a `.shared-banners
.banner-face`'s height, and the label is legible beside "Shared Bank".
Report both boxes.

- [ ] **Step 5: Commit**

```bash
git commit -m "fix(frontend): size the Reliquary's relics to the banners on the table's edge

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  frontend/styles.css \
  src/test/scala/distantlands/server/StylesheetTableEdgeSuite.scala
git log -3
```

### Task 8: The golden log, the docs and the gate

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala`
  (a `legacy-modifiers` script)
- Create: `src/test/resources/gamelog/legacy-modifiers.actor.log`,
  `src/test/resources/gamelog/legacy-modifiers.other.log` (generated)
- Modify: `docs/rules/ambiguities.md`,
  `docs/rules/implementation-traceability.md`,
  `docs/rules/rules-reference.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-design.md`
  (status)
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md`

- [ ] **Step 1: The script**

`legacyModifiers`: p1, the Chancellor, holds Ancestral Lands, Light
Fingers and Beloved active. In Wake at a Provinces site with 1 favor
there and p2's pawn beside them, p1 takes the favor (Light Fingers).
In Act p1 trades for favor on p2's faceup adviser there (Beloved), then
travels to a Cradle site for 1 Supply (Ancestral Lands). Each step through
`MenuPlay.start`, answers by default.

- [ ] **Step 2: Generate and review the golden**

`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *legacy-modifiers*"`,
then the gamelog suites. Expected lines: "Took 1 favor from <site>", the
Trade's "Traded with <p2's adviser> for N favor", and the Travel line with
its 1-Supply cost span; no legacy line (R8). Every other golden unchanged.

- [ ] **Step 3: Docs**

- `ambiguities.md`, the "Legacy in-play powers rulings" section: the
  design's rulings 14-19 and slice 2's implementation rulings, numbered
  on from the last item, and the section's opening sentence updated.
- `implementation-traceability.md`: a row "Legacy modifier powers" after
  "Legacy ACTION powers", citing the suites of Tasks 1-6 and the
  `legacy-modifiers` golden; the "Legacy ACTION powers" row's High Priest
  note points at it.
- `rules-reference.md`: "Digital:" notes under Travel (Ancestral Lands,
  Pathfinder), Trade (Beloved), Wake's Take Wealth (Light Fingers),
  Minor actions' negotiate (High Priest) and Imperial laws' Imperial
  Infighting (Keeper of Order), each citing its item.
- The design's status line: slice 2 has landed.

- [ ] **Step 4: Run the gate**

Run the gate (top of this plan).

- [ ] **Step 5: Commit**

```bash
git add src/test/resources/gamelog/legacy-modifiers.actor.log \
  src/test/resources/gamelog/legacy-modifiers.other.log
git commit -m "test(gamelog): a golden log of the legacies' modifiers, and their docs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala \
  src/test/resources/gamelog/legacy-modifiers.actor.log \
  src/test/resources/gamelog/legacy-modifiers.other.log \
  docs/rules docs/superpowers/specs/2026-11
git log -3
```

## Self-review notes

- Spec rulings: 14 Task 1; 15 Task 2; 16 Task 5; 17 Task 3; 18 Task 4; 19
  Task 6. Ruling 5 (Imperial Tactics reaches legacies): Task 5's lend.
  Rulings 1-2: each task's dormant, another player's or Exile case.
- The slice 2 test list: Ancestral Lands into the Cradle from a Mountain
  route and as an Exile (Task 1); Pathfinder over a Mountain, an Island
  and a Pass, the Pass still limiting a campaign (Task 2); Keeper of Order
  adding and removing in Imperial Infighting, skulls only when attacking,
  lent with consent, not against an Exile or the bandits (Task 5); Light
  Fingers beside an enemy pawn (Task 3); Beloved on a faceup adviser and
  not a facedown one (Task 4); High Priest giving secrets while the other
  side cannot, on resume too (Task 6). A golden log (Task 8).
- The Reliquary sizing (owner's request): Task 7.
- Names across tasks: `AncestralLands`, `Pathfinder`, `TakeWealthPawnGate`,
  `LightFingers`, `Beloved.advisersAtSite`, `MusterSource.locate`,
  `CampaignPlanSource.Legacy`, `PlanContext.legacy`, `KeeperOfOrder`
  (`decisionId`, `Dice`), `DealRules.secretGivers`,
  `NegotiationStartFacts.highPriestUsers` and `.HighPriestMarker`,
  `NegotiationAdvisers.highPriestUsers`.
- Several tests name their tables in prose where the harness to copy is
  an existing suite's (Travel, Trade, Campaign); each names the suite to
  read first. Their assertions are fixed.
