# Catalog Batch 10 Slice 1: Empire Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Bandit Forest (E29 ruined), Council Seat (123), Naysayers (21) and Imperial Seal (R26), and generalise Giant Python's restriction into `NonPersistentPowerRestriction`.

**Architecture:**
- `GiantPythonRestriction` becomes a class, `NonPersistentPowerRestriction`, with two instances. Each instance has a guard (which cards it protects) and a binding (which players it binds).
- Council Seat is a rule-only `ContributingPower` with no contributions. `Citizenship.exile` builds no operations for a Citizen it guards. The self-exile button and the Grand Scepter's exile button carry a `DecisionOption.Warned`.
- Naysayers is an optional REST `PhasePower`. Imperial Seal is a `PaidAction` that asks a count and then one destination.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read "Goal and scope", "Catalog corrections", "Rulings" (4, 8 and 10), "Readings" (Naysayers, Bandit Forest), "Slice 1: Empire cards", "Testing" and "Out of scope". Card text: `~/projects/cards/content`. Rules: `~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`.

This is the first of 14 slice plans. Each later slice's plan is written just before that slice runs, against the code at that point.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-1` on branch `batch-10-slice-1`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused values and non-exhaustive matches fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write` (`src/test/resources/notes/declared-keys.txt`) and `UNIMPLEMENTED_SNAPSHOT=write` (`src/test/resources/powers/unimplemented.txt`), then read the diff. Commit only the lines the task explains. Each card task drops exactly its own power id from `unimplemented.txt`.
- **Moving a card out of holding:** delete its object from `src/main/scala/distantlands/catalog/holding/*.scala` and define it, unchanged except where a task says otherwise, beside its power. `gameplay/cards/NewFoundations.scala` imports every `gameplay.powers.*` package by wildcard. A card defined in `gameplay.roles` needs a named import there.
- **Log lines:** a power's line names only non-actors (memory: "Log lines omit actor"). A line that replaces a generic line is declared with `covers = true`.
- **Hover reasons:** show an unavailable option disabled with a reason. Hide only the options the player's role can never use.
- **Wire:** this slice adds no wire types. Old journals need not decode.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").

---

### Task 1: `NonPersistentPowerRestriction` and Bandit Forest

**Files:**
- Delete: `src/main/scala/distantlands/gameplay/powers/poweruse/GiantPythonRestriction.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/poweruse/NonPersistentPowerRestriction.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala:73`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/GiantPython.scala:32` (doc comment)
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala` (doc comment's last sentence)
- Modify: `src/test/scala/distantlands/gameplay/powers/poweruse/GiantPythonRestrictionSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/poweruse/BanditForestSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt`

**Interfaces:**
- Produces: `final class NonPersistentPowerRestriction` with `val id: PowerId`, `val usePlain: NoteKey`, `val placingPlain: NoteKey`, `def refusal(catalog, ready, use): Option[PowerUseRefusal]` and `operationRestrictions(catalog)`. Its companion holds two instances, `NonPersistentPowerRestriction.giantPython` and `NonPersistentPowerRestriction.banditForest`.

- [ ] **Step 1: Point the Giant Python suite at the new instance**

In `GiantPythonRestrictionSuite.scala`, replace every `GiantPythonRestriction` with `NonPersistentPowerRestriction.giantPython`. There are 11 occurrences, at lines 38 to 74. The suite keeps its name: it tests Giant Python's instance.

```bash
sed -i '' 's/GiantPythonRestriction\./NonPersistentPowerRestriction.giantPython./g' src/test/scala/distantlands/gameplay/powers/poweruse/GiantPythonRestrictionSuite.scala
```

Check that the class line (`class GiantPythonRestrictionSuite`) is unchanged.

- [ ] **Step 2: Write the failing Bandit Forest suite**

Create `src/test/scala/distantlands/gameplay/powers/poweruse/BanditForestSuite.scala`:

```scala
package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import oathdigital.gameplay.powerresolver.PowerUse
import oathdigital.gameplay.powers.action.RovingTerrorCard
import oathdigital.gameplay.powers.travel.ForbiddenForestCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** Bandit Forest (E29, ruined): "Imperials cannot place [favor]/[secret] on
  * cards ruled by bandits or use their non-persistent powers." (catalog
  * batch 10, slice 1). It binds Imperial players only, and nobody may
  * waive it. */
class BanditForestSuite extends munit.FunSuite:
  private val forest = NonPersistentPowerRestriction.banditForest
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id
  private val source = PowerSourceRef.Card(ForbiddenForestCard.id)

  /** p2 is the Chancellor and p1 a Citizen; p3 is an Exile. Bandits rule
    * Ancient City, where Roving Terror stands, and Bandit Forest lies
    * ruined at Deep Woods. It is `actor`'s Act. */
  private def terrorUnderBanditsWithForestRuined(actor: PlayerId): Table =
    Table.start.imperial(p2, p1).turn(actor, Phase.Act)
      .bandits(home, 2).denizen(terror, at = home)
      .edifice(ForbiddenForestCard, EdificeSide.Ruined, at = woods)

  private def nonPersistentUseOf(card: DenizenId, by: PlayerId): PowerUse =
    PowerUse(by, PowerId("test.power"), Some(card), persistent = false,
      costsSecret = false)

  test("it refuses an Imperial player a non-persistent power of a card " +
      "bandits rule, and nobody may waive it"):
    val ready = terrorUnderBanditsWithForestRuined(p1).ready
    val refusal = forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1))
    assertEquals(refusal.map(_.note), Some(forest.usePlain(source)))
    assertEquals(refusal.flatMap(_.reason.waiver), None)

  test("an Exile may use the card"):
    val ready = terrorUnderBanditsWithForestRuined(p3).ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p3)), None)

  test("it refuses an Imperial player's payment onto a card bandits rule, " +
      "and lets an Exile's through"):
    val ban = forest.operationRestrictions(catalog).head
    val imperialPays = PayCost(p1, Location.OnCard(terror), Cost(favor = 1))
    assert(ban.reason(terrorUnderBanditsWithForestRuined(p1).ready,
      imperialPays).exists(_.waiver.isEmpty))
    val exilePays = PayCost(p3, Location.OnCard(terror), Cost(favor = 1))
    assertEquals(ban.reason(terrorUnderBanditsWithForestRuined(p3).ready,
      exilePays), None)

  test("a card at a site bandits do not rule is free"):
    val ready = terrorUnderBanditsWithForestRuined(p1)
      .update(_.updateCurrent(current => current.copy(map =
        current.map.copy(sites = current.map.sites.updatedWith(home)(
          _.map(_.copy(forces = SiteForces.Empty)))))))
      .ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1)), None)

  test("with the Forest's intact face on the map, nothing is refused"):
    val ready = Table.start.imperial(p2, p1).turn(p1, Phase.Act)
      .bandits(home, 2).denizen(terror, at = home)
      .edifice(ForbiddenForestCard, EdificeSide.Intact, at = woods).ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1)), None)
```

`Table.edifice(card, side, at)` and `SiteState.forces` are as used here.

- [ ] **Step 3: Run both suites and check they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.poweruse.*"`
Expected: compile failure, `Not found: NonPersistentPowerRestriction`.

- [ ] **Step 4: Write `NonPersistentPowerRestriction`**

Delete `GiantPythonRestriction.scala` (`git rm`). Create `NonPersistentPowerRestriction.scala`:

```scala
package oathdigital.gameplay.powers.poweruse

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerUse, PowerUseRefusal, PowerUseRestriction}
import oathdigital.gameplay.powers.action.GiantPythonCard
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.gameplay.powers.travel.ForbiddenForestCard
import oathdigital.model._

/** A persistent rule that guards some cards against non-persistent powers
  * and placements (catalog batch 10, slice 1): Giant Python's "Other cards
  * at this site cannot be used for non-persistent powers or placing
  * [favor] / [secret]" and Bandit Forest's "Imperials cannot place
  * [favor]/[secret] on cards ruled by bandits or use their non-persistent
  * powers".
  *
  * For a player it `binds`, it refuses a non-persistent use of a guarded
  * card, a use whose cost places on one ([[Placements.byUse]]), and a
  * placement onto one ([[Placements.onto]]). It names no enemies, so no
  * consent lifts it (consent design, ruling 1). It writes no line.
  *
  * `describe` names the guarded cards in the operation reason; the two
  * plain texts are the hover reasons.
  */
final class NonPersistentPowerRestriction(val id: PowerId, card: CardId,
    code: String, describe: String, usePlainText: String,
    placingPlainText: String, guarded: (ReadyGame, CardId) => Boolean,
    binds: (ReadyGame, PlayerId) => Boolean)
    extends ContributingPower with PowerUseRestriction:
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** Why a non-persistent use of a guarded card is refused. */
  val usePlain: NoteKey = notes.key("no-powers-here", Vector(NotePart.Text(
    usePlainText)))
  /** Why a placement on a guarded card is refused. */
  val placingPlain: NoteKey = notes.key("no-placing-here", Vector(
    NotePart.Text(placingPlainText)))

  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override def operationRestrictions(catalog: ExecutableCatalog)
      : Vector[OperationRestriction] =
    Vector(placing)

  private val cardSource = PowerSourceRef.Card(card)

  private def refused(reasonCode: String, player: PlayerId): OperationReason =
    OperationReason(reasonCode, s"$describe cannot be used by " +
      s"${player.value}", OperationReasonKind.Impossible)

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    for
      used <- use.card
      if binds(ready, use.user) && guarded(ready, used)
      key <- if !use.persistent then Some(usePlain)
        else Placements.byUse(ready, use).map(_ => placingPlain)
    yield PowerUseRefusal(id, key(cardSource), refused(code, use.user))

  private object placing extends OperationRestriction:
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] =
      Placements.onto(ready, operation).filter((player, placed) =>
        binds(ready, player) && guarded(ready, placed)).map(placed =>
        refused(s"$code-placing", placed._1))

object NonPersistentPowerRestriction:
  /** Whether `card` stands at `site`. */
  private def standsAt(ready: ReadyGame, site: SiteId, card: CardId)
      : Boolean =
    ready.game.current.map.sites.get(site).exists(_.denizens.exists(
      _.id == card))

  /** Giant Python's restriction (card 186, site-only): the other cards at
    * the site where Giant Python stands now, so it moves with it. It binds
    * every player, its ruler included. */
  val giantPython: NonPersistentPowerRestriction =
    NonPersistentPowerRestriction(GiantPythonCard.restriction.id,
      GiantPythonCard.id, "giant-python", "cards at Giant Python's site",
      "Cards at its site cannot be used for non-persistent powers.",
      "Cannot place favor or secrets on cards at its site.",
      guarded = (ready, card) => card != GiantPythonCard.id &&
        SiteRulers.siteOf(ready, GiantPythonCard.id).exists(
          standsAt(ready, _, card)),
      binds = (_, _) => true)

  /** Bandit Forest (E29, ruined): the cards at sites bandits rule, while
    * the ruined face is on the map. It binds Imperial players only. */
  val banditForest: NonPersistentPowerRestriction =
    NonPersistentPowerRestriction(ForbiddenForestCard.ruined.power.id,
      ForbiddenForestCard.id, "bandit-forest", "cards ruled by bandits",
      "Imperials cannot use the non-persistent powers of cards ruled by " +
        "bandits.",
      "Imperials cannot place favor or secrets on cards ruled by bandits.",
      guarded = (ready, card) => EdificeSetupSupport.siteOf(ready,
        ForbiddenForestCard.id, EdificeSide.Ruined).isDefined &&
        ready.game.current.map.inPlay.exists(site =>
          SiteRulers.rulerOf(ready, site).contains(SiteRuler.Bandits) &&
            standsAt(ready, site, card)),
      binds = Imperials.isImperial)
```

`GiantPythonRestriction` declared its notes through `NoteSet(id)` with the same key names, so Giant Python's two `declared-keys.txt` lines do not change.

- [ ] **Step 5: Register both instances and fix the doc comments**

In `WalkerPowerCatalog.scala:73`, replace `Vector(GiantPythonRestriction) ++` with:

```scala
      Vector(NonPersistentPowerRestriction.giantPython,
        NonPersistentPowerRestriction.banditForest) ++
```

Fix the import: `poweruse.GiantPythonRestriction` becomes `poweruse.NonPersistentPowerRestriction`, or the wildcard already covers it.

In `GiantPython.scala:32`, change `(`GiantPythonRestriction`)` to `(`NonPersistentPowerRestriction.giantPython`)`.

In `ForbiddenForest.scala`, replace the doc comment's last sentence ("The ruined face, Bandit Forest, binds Imperials only and is not implemented.") with: "The ruined face, Bandit Forest, is `NonPersistentPowerRestriction.banditForest`."

- [ ] **Step 6: Run the suites and check they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.poweruse.*"`
Expected: PASS, including every existing `GiantPythonRestrictionSuite` test.

- [ ] **Step 7: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.DeclaredNoteKeysSuite oathdigital.gameplay.powers.UnimplementedPowersSuite"`

Then `git diff src/test/resources`. Expected:
- `declared-keys.txt` gains exactly `edifice.e29.ruined no-placing-here = ...` and `edifice.e29.ruined no-powers-here = ...`.
- `unimplemented.txt` loses exactly `edifice.e29.ruined`.

- [ ] **Step 8: Run the restriction and travel suites**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.* oathdigital.gameplay.PhaseMenuSuite"`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/poweruse/NonPersistentPowerRestriction.scala src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala src/main/scala/distantlands/gameplay/powers/action/GiantPython.scala src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala src/test/scala/distantlands/gameplay/powers/poweruse/GiantPythonRestrictionSuite.scala src/test/scala/distantlands/gameplay/powers/poweruse/BanditForestSuite.scala src/test/resources/notes/declared-keys.txt src/test/resources/powers/unimplemented.txt
git rm src/main/scala/distantlands/gameplay/powers/poweruse/GiantPythonRestriction.scala
git commit -m "$(cat <<'EOF'
feat(powers): Bandit Forest through NonPersistentPowerRestriction

Giant Python's restriction becomes a class with a guard and a binding,
and Bandit Forest is its second instance: while the ruined face is on the
map, Imperial players cannot use the non-persistent powers of cards at
bandit-ruled sites or place favor or secrets on them.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Council Seat

**Files:**
- Create: `src/main/scala/distantlands/gameplay/roles/CouncilSeat.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingOrderDenizens.scala` (remove `CouncilSeatCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import `oathdigital.gameplay.roles.CouncilSeatCard`)
- Modify: `src/main/scala/distantlands/gameplay/roles/Citizenship.scala:35-36`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/GrandScepterPlan.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/ActOffers.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/PhaseMenu.scala:131` (the `acting` builder)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` (register `CouncilSeat`)
- Create: `src/test/scala/distantlands/gameplay/roles/CouncilSeatSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/powers/campaign/GrandScepterPlanSuite.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`

**Interfaces:**
- Produces:
  - `object CouncilSeatCard`.
  - `case object CouncilSeat extends ContributingPower`, with:
    - `val warning: String = "Council Seat: cannot be exiled."`
    - `def guards(ready: ReadyGame, player: PlayerId): Boolean`
    - `def warned(ready: ReadyGame, player: PlayerId, option: DecisionOption): DecisionOption`
  - `ActOffers.button(ready: ReadyGame, actor: PlayerId, offer: ActOffer): DecisionOption`.

- [ ] **Step 1: Write the failing self-exile tests**

Create `src/test/scala/distantlands/gameplay/roles/CouncilSeatSuite.scala`:

```scala
package oathdigital.gameplay.roles

import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Council Seat (card 123): "If you're a Citizen, you cannot be exiled,
  * even by yourself." (catalog batch 10, slice 1, ruling 8). Every exile
  * path stays choosable with a warning, and the exile is skipped. */
class CouncilSeatSuite extends munit.FunSuite:
  private val gameId = "council-seat"

  /** p1 is the Chancellor and p2 a Citizen in their Act, holding Council
    * Seat as an adviser (faceup unless `facedown`), with 4 board warbands
    * and 6 favor. */
  private def citizenHoldingSeat(facedown: Boolean = false): Table =
    Table.start.imperial(p1, p2).turn(p2, Phase.Act)
      .adviser(p2, CouncilSeatCard, facedown = facedown)
      .boardWarbands(p2, 4).favor(p2, 6)

  /** The warnings an option carries, through the menu's wrappers. */
  private def warningsOf(option: DecisionOption): Vector[OptionReason] =
    option match
      case DecisionOption.Warned(inner, reason) => reason +: warningsOf(inner)
      case DecisionOption.Grouped(inner, _) => warningsOf(inner)
      case DecisionOption.Badged(inner, _) => warningsOf(inner)
      case DecisionOption.Unavailable(inner, _) => warningsOf(inner)
      case _ => Vector.empty

  private val seatWarning = OptionReason.Written(CouncilSeat.warning)

  test("the Act menu's self-exile carries the Seat's warning"):
    val option = MenuPlay.menuOptionFor(catalog, citizenHoldingSeat().ready,
      ActionRef.SelfExile)
    assert(option.available, option)
    assertEquals(warningsOf(option), Vector(seatWarning))

  test("a facedown Seat warns of nothing"):
    val option = MenuPlay.menuOptionFor(catalog,
      citizenHoldingSeat(facedown = true).ready, ActionRef.SelfExile)
    assertEquals(warningsOf(option), Vector.empty)

  test("self-exile still pays the Chancellor, and the Citizen stays one"):
    val table = citizenHoldingSeat()
    val chancellorFavor = Look(table.ready).player(p1).board.favor
    val (service, _) = table.service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    val after = Outcomes.ready(done.state)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assertEquals(Look(after).player(p2).board.favor, 2)
    assertEquals(Look(after).player(p1).board.favor, chancellorFavor + 4)

  test("with the Seat facedown, self-exile exiles"):
    val (service, _) = citizenHoldingSeat(facedown = true).service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    assertEquals(Imperials.role(Outcomes.ready(done.state), p2),
      Some(Role.Exile))

  test("the Seat guards only a Citizen who holds it faceup"):
    val ready = citizenHoldingSeat().ready
    assert(CouncilSeat.guards(ready, p2))
    assert(!CouncilSeat.guards(ready, p1))
    assert(!CouncilSeat.guards(citizenHoldingSeat(facedown = true).ready,
      p2))
```

`Table.adviser(player, card, facedown)` and `DecisionOption.Grouped(option, group)` are as used here.

- [ ] **Step 2: Write the failing Grand Scepter tests**

Append to `GrandScepterPlanSuite.scala`, and import `oathdigital.gameplay.roles.{CouncilSeat, CouncilSeatCard}`:

```scala
  /** As `chancellorRaidsCitizen`, with the Citizen holding Council Seat
    * faceup. */
  private def chancellorRaidsSeatedCitizen: Board =
    val base = chancellorRaidsCitizen
    on(base)(_.adviser(base.rival, CouncilSeatCard))

  test("against a Citizen holding Council Seat, the exile carries the " +
      "Seat's warning"):
    val b = chancellorRaidsSeatedCitizen
    val won = wonWithScepter(b)
    val exile = won.options(b.actor).find(_.ref == GrandScepterPlan.exile)
    assertEquals(exile.collect { case DecisionOption.Warned(_, reason) =>
      reason }, Some(OptionReason.Written(CouncilSeat.warning)))

  test("exiling a Citizen who holds Council Seat leaves them a Citizen"):
    val b = chancellorRaidsSeatedCitizen
    val done = wonWithScepter(b).pick(b.actor, GrandScepterPlan.decisionId,
      GrandScepterPlan.exile)
    assertEquals(Imperials.role(ready(done.state), b.rival),
      Some(Role.Citizen))
    assertEquals(lines(done), Vector(NoteText.Said("spared",
      s"Did not exile ${b.rival.value}.", covers = false)))
```

If adding an adviser to the rival changes the Campaign's dice or plans (for example, a defense bonus), pick a rival setup that leaves the Campaign as it is in `chancellorRaidsCitizen`. Council Seat has no Campaign effect, so no change is expected.

- [ ] **Step 3: Run the tests and check they fail**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.CouncilSeatSuite oathdigital.gameplay.powers.campaign.GrandScepterPlanSuite"`
Expected: compile failure, `Not found: CouncilSeatCard`.

- [ ] **Step 4: Write `CouncilSeat`**

Remove `CouncilSeatCard` from `HoldingOrderDenizens.scala`. Remove that file's imports that are now unused; `-Werror` will name them. Create `src/main/scala/distantlands/gameplay/roles/CouncilSeat.scala`:

```scala
package oathdigital.gameplay.roles

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.PowerAccess
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower}
import oathdigital.model._

object CouncilSeatCard extends Denizen(DenizenId("123"), "Council Seat", Suit.Order) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-seat"),
    persistent = true, cost = Cost.free,
    text = "If you're a Citizen, you **cannot** be exiled, even by " +
      "yourself.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Council Seat (card 123, adviser-only, locked): "If you're a Citizen, you
  * cannot be exiled, even by yourself." (catalog batch 10, slice 1).
  *
  * A rule with no contributions: `Citizenship.exile` builds nothing for a
  * Citizen it guards. Every exile path stays choosable and carries the
  * warning (ruling 8); its costs are still paid. It guards only exiles
  * during play: the Chronicle's Empire break makes a Citizen an Exile
  * without exiling them (ruling 10). It writes no line.
  */
case object CouncilSeat extends ContributingPower:
  val id: PowerId = CouncilSeatCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  val warning: String = "Council Seat: cannot be exiled."

  /** Whether `player` is a Citizen holding the Seat faceup. */
  def guards(ready: ReadyGame, player: PlayerId): Boolean =
    Imperials.role(ready, player).contains(Role.Citizen) &&
      PowerAccess.holdsFaceupAdviser(ready, player, CouncilSeatCard.id)

  /** `option`, an exile of `player`, warned while the Seat guards them. */
  def warned(ready: ReadyGame, player: PlayerId, option: DecisionOption)
      : DecisionOption =
    if guards(ready, player) then
      DecisionOption.Warned(option, OptionReason.Written(warning))
    else option
```

In `NewFoundations.scala`, add `import oathdigital.gameplay.roles.CouncilSeatCard`. The `CouncilSeatCard,` entry at line 59 stays where it is.

In `WalkerPowerCatalog.scala`, add `Vector(CouncilSeat) ++` next to the `NonPersistentPowerRestriction` line, and import `oathdigital.gameplay.roles.CouncilSeat`.

`PowerAccess` is `private[gameplay]`, which `gameplay.roles` reaches.

- [ ] **Step 5: Skip the exile in `Citizenship.exile`**

Replace `Citizenship.scala:34-36` with:

```scala
  /** `actor`, a Citizen, becomes an Exile. Nothing happens while Council
    * Seat guards them (catalog batch 10, slice 1). */
  def exile(actor: PlayerId): Operation =
    BuildOps((live, _) =>
      if CouncilSeat.guards(live, actor) then Right(Vector.empty)
      else RoleChanges.exile(live, actor), required = true)
```

A `BuildOps` that returns `Vector.empty` runs nothing and records no step (`model/Operation.scala`, `BuildOps` doc).

- [ ] **Step 6: Warn on self-exile in the Act menu**

In `ActOffers.scala`, add after `offeredTo`:

```scala
  /** The button `offer` shows `actor`. Self-exile carries Council Seat's
    * warning while the Seat guards them. */
  def button(ready: ReadyGame, actor: PlayerId, offer: ActOffer)
      : DecisionOption =
    val plain = DecisionOption.Button(offer.ref, offer.label)
    if offer.procedure == ActionRef.SelfExile then
      CouncilSeat.warned(ready, actor, plain)
    else plain
```

Import `oathdigital.gameplay.roles.CouncilSeat`. In `PhaseMenu.offers`, inside the `acting` builder (line 131), replace `DecisionOption.Button(offer.ref, offer.label)` with `ActOffers.button(ready, actor, offer)`.

- [ ] **Step 7: Warn in the Grand Scepter's plan and its question**

In `GrandScepterPlan.scala`:

1. In `plan`, give the offer the warning, so the 3 favor is not burnt unawares:

```scala
  yield CampaignPlanOffer(source,
    "The Grand Scepter: you may exile the Citizen if victorious",
    Vector(CampaignPlanCost.FavorBurnt(3)), Vector.empty,
    warning = Option.when(CouncilSeat.guards(context.ready, enemy))(
      OptionReason.Written(CouncilSeat.warning)))
```

2. In `resolve`, pass `live` to `ask`: `else ask(live, user, enemy)`.

3. Replace `ask` with:

```scala
  private def ask(ready: ReadyGame, user: PlayerId, enemy: PlayerId)
      : Vector[Operation] =
    Vector(
      Decide(decisionId, user, DecisionQuery.ChooseOne(Vector(
        CouncilSeat.warned(ready, enemy,
          DecisionOption.Button(exile, s"Exile ${enemy.value}")),
        DecisionOption.Button(spare, "Do not exile")),
        heading = Some("The Grand Scepter: you may exile the Citizen"))),
      Branch((_, pending) =>
        if PowerAnswers.one(pending, decisionId).contains(exile) then
          Vector(Citizenship.exile(enemy), Note(id, states => Some(
            if Imperials.role(states.now, enemy).contains(Role.Exile) then
              exiled(card, NoteArg.Player(enemy))
            else spared(card, NoteArg.Player(enemy)))))
        else Vector(Note(id, _ => Some(spared(card, NoteArg.Player(enemy)))))))
```

Import `oathdigital.gameplay.roles.CouncilSeat`. Update the scaladoc: "Against a Citizen holding Council Seat faceup, the plan and the exile carry the Seat's warning, and the exile leaves them a Citizen ('Did not exile p2.')."

- [ ] **Step 8: Run the tests and check they pass**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.* oathdigital.gameplay.powers.campaign.GrandScepterPlanSuite oathdigital.gameplay.SelfExileSuite oathdigital.gameplay.PhaseMenuSuite oathdigital.application.RoleChangesScenarioSuite"`
Expected: PASS.

- [ ] **Step 9: Regenerate the unimplemented snapshot**

Run: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.UnimplementedPowersSuite"`
Expected diff: `unimplemented.txt` loses exactly `denizen.council-seat`.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/roles/CouncilSeat.scala src/main/scala/distantlands/catalog/holding/HoldingOrderDenizens.scala src/main/scala/distantlands/gameplay/cards/NewFoundations.scala src/main/scala/distantlands/gameplay/roles/Citizenship.scala src/main/scala/distantlands/gameplay/powers/campaign/GrandScepterPlan.scala src/main/scala/distantlands/gameplay/phases/ActOffers.scala src/main/scala/distantlands/gameplay/phases/PhaseMenu.scala src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala src/test/scala/distantlands/gameplay/roles/CouncilSeatSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/GrandScepterPlanSuite.scala src/test/resources/powers/unimplemented.txt
git commit -m "$(cat <<'EOF'
feat(powers): Council Seat guards its Citizen from exile

Citizenship.exile builds nothing for a Citizen holding the Seat faceup.
Self-exile and the Grand Scepter's plan and exile stay choosable with the
warning "Council Seat: cannot be exiled."; self-exile still pays the
Chancellor.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Naysayers

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/rest/Naysayers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala` (remove `NaysayersCard`)
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/rest/NaysayersSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt`

**Interfaces:**
- Produces: `object NaysayersCard`, and `case object Naysayers extends PhasePower` with `val took: NoteKey`.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/rest/NaysayersSuite.scala`:

```scala
package oathdigital.gameplay.powers.rest

import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Naysayers (card 21): "REST: If any Exile is the Oathkeeper or Usurper,
  * take [favor] from the Chancellor." (catalog batch 10, slice 1). */
class NaysayersSuite extends munit.FunSuite:
  import PhasePowerHarness._
  private val source = DecisionOptionRef.Denizen(NaysayersCard.id)

  /** p1, an Exile, rests holding Naysayers faceup; p2 is the Chancellor
    * with 2 favor, and `holder` holds the Oathkeeper title on `side`. */
  private def restingExileWhileTitled(holder: Option[PlayerId],
      side: TitleSide = TitleSide.Oathkeeper): Table =
    Table.start.imperial(p2).turn(p1, Phase.Rest)
      .adviser(p1, NaysayersCard).favor(p1, 0).favor(p2, 2)
      .oathkeeper(holder, side)

  private def usableFor(ready: ReadyGame): Boolean =
    Naysayers.usable(catalog, ready, p1, source)

  test("with an Exile as Oathkeeper it takes 1 favor from the Chancellor"):
    val ready = restingExileWhileTitled(Some(p1)).ready
    val done = accepted(use(ready, Naysayers, source))
    val end = Look(after(done))
    assertEquals(end.player(p1).board.favor, 1)
    assertEquals(end.player(p2).board.favor, 1)
    assertEquals(NoteText.said(Naysayers, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"Took 1 favor from ${p2.value}.",
        covers = true)))
    assertEquals(replayed(ready, done.events), Right(done.state))

  test("another Exile as Usurper counts too"):
    assert(usableFor(restingExileWhileTitled(Some(p3),
      TitleSide.Usurper).ready))

  test("it is not usable while an Imperial player or nobody holds the " +
      "title"):
    assert(!usableFor(restingExileWhileTitled(Some(p2)).ready))
    assert(!usableFor(restingExileWhileTitled(None).ready))

  test("it is not usable with no Chancellor, or a Chancellor with no " +
      "favor"):
    val noEmpire = Table.start.turn(p1, Phase.Rest)
      .adviser(p1, NaysayersCard).oathkeeper(Some(p1)).ready
    assert(!usableFor(noEmpire))
    assert(!usableFor(restingExileWhileTitled(Some(p1)).favor(p2, 0).ready))

  test("it is optional"):
    assert(!Naysayers.forced)
```

`PhasePowerHarness.replayed` returns `Either[OathViolation, OathState]`, so it compares with `Right(done.state)`.

- [ ] **Step 2: Run it and check it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.rest.NaysayersSuite"`
Expected: compile failure, `Not found: Naysayers`.

- [ ] **Step 3: Write `Naysayers`**

Remove `NaysayersCard` from `HoldingDiscordDenizens.scala`. Create `src/main/scala/distantlands/gameplay/powers/rest/Naysayers.scala`:

```scala
package oathdigital.gameplay.powers.rest

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import oathdigital.gameplay.powerresolver.PhasePower
import oathdigital.model._

object NaysayersCard extends Denizen(DenizenId("21"), "Naysayers", Suit.Discord) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.naysayers"),
    persistent = false, cost = Cost.free,
    text = "**REST:** If any Exile is the Oathkeeper or Usurper, take " +
      "[favor] from the Chancellor.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Naysayers (card 21, adviser-only): "REST: If any Exile is the Oathkeeper
  * or Usurper, take [favor] from the Chancellor." (catalog batch 10,
  * slice 1).
  *
  * An optional REST power, as Insomnia's is. It is usable while an Exile,
  * the user included (Naysayers FAQ), holds the title on either side, and
  * a Chancellor other than the user has favor. It takes the favor with a
  * `Take` from the Chancellor's play area. Its line, "Took 1 favor from
  * {Purple}.", covers the generic take line.
  */
case object Naysayers extends PhasePower:
  val id: PowerId = NaysayersCard.power.id
  val Favor: Int = 1
  private val notes = NoteSet(id)
  /** "Took 1 favor from {0}." */
  val took: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Took 1 favor from "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def timing: PowerTiming = PowerTiming.Rest

  /** Whether an Exile holds the title, on either side. */
  def exileTitled(ready: ReadyGame): Boolean =
    ready.game.current.title.holder.exists(holder =>
      Imperials.role(ready, holder).contains(Role.Exile))

  /** The Chancellor `player` would take from: one other than `player`,
    * with favor. */
  private def payer(ready: ReadyGame, player: PlayerId): Option[PlayerId] =
    Imperials.chancellor(ready).filter(chancellor => chancellor != player &&
      ready.game.current.players.find(_.player == chancellor)
        .exists(_.board.favor > 0))

  def usable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean =
    exileTitled(ready) && payer(ready, player).isDefined

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    payer(ready, player).toRight(OathViolation.StartUnavailable(
      "The Chancellor has no favor to take")).map(chancellor =>
      Sequence(Vector(
        BuildOps((_, _) => Right(Vector(Take(Piece.Favor(Favor), player,
          Location.PlayArea(chancellor), Location.PlayArea(player),
          required = true))), required = true),
        Note(id, _ => PowerSourceRef.of(source).map(card =>
          took(card, NoteArg.Player(chancellor))), covers = true))))
```

In `PhasePowerCatalog.scala`, change `Vector(Insomnia) ++` to `Vector(Insomnia, Naysayers) ++` and add `Naysayers` to the `rest` import.

- [ ] **Step 4: Run the suite and check it passes**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.rest.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.DeclaredNoteKeysSuite oathdigital.gameplay.powers.UnimplementedPowersSuite"`
Expected diff:
- `declared-keys.txt` gains exactly `denizen.naysayers used = Took 1 favor from {0}.`
- `unimplemented.txt` loses exactly `denizen.naysayers`.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/rest/Naysayers.scala src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala src/test/scala/distantlands/gameplay/powers/rest/NaysayersSuite.scala src/test/resources/notes/declared-keys.txt src/test/resources/powers/unimplemented.txt
git commit -m "$(cat <<'EOF'
feat(powers): Naysayers takes favor from the Chancellor at Rest

An optional REST power, usable while an Exile holds the title on either
side and the Chancellor has favor.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Imperial Seal

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/action/ImperialSeal.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala` (remove `ImperialSealCard`)
- Modify: `src/main/scala/distantlands/gameplay/SiteRulers.scala` (add `imperialSites`)
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/action/ImperialSealSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt`

**Interfaces:**
- Produces:
  - `SiteRulers.imperialSites(ready: ReadyGame): Vector[SiteId]`.
  - `object ImperialSealCard`.
  - `case object ImperialSeal extends PaidAction`, with `countDecisionId`, `destinationDecisionId`, `toSite: NoteKey` and `toBoard: NoteKey`.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/action/ImperialSealSuite.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Imperial Seal (R26): "ACTION: If no Imperial pawns are at your site,
  * move any number of Imperial warbands from your site to any Imperial
  * site or Imperial player board." (catalog batch 10, slice 1, ruling 4).
  */
class ImperialSealSuite extends munit.FunSuite:
  import PhasePowerHarness._
  private val source = DecisionOptionRef.Relic(ImperialSealCard.id)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val giant = BuriedGiantSite.id

  /** p1, an Exile, acts at Ancient City holding the Seal with a secret to
    * pay. p2 is the Chancellor at Broken Peaks and p3 a Citizen at Buried
    * Giant. The Empire holds Ancient City with 3 warbands and Broken Peaks
    * with 1. */
  private def exileAmongImperialWarbands: Table =
    Table.start.imperial(p2, p3).turn(p1, Phase.Act)
      .pawn(p1, at = home).pawn(p2, at = peaks).pawn(p3, at = giant)
      .relic(p1, ImperialSealCard).secrets(p1, faceUp = 1)
      .warbandsAt(home, p2, 3).warbandsAt(peaks, p2, 1)

  private def imperialAt(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites(site).forces match
      case SiteForces.Occupied(ForceKind.Imperial, n) => n
      case _ => 0

  private def moving(ready: ReadyGame, count: Int,
      to: DecisionOptionRef): UsedAndAnswered =
    val started = accepted(use(ready, ImperialSeal, source))
    val counted = accepted(answer(started, p1, ImperialSeal.countDecisionId,
      DecisionAnswer.ChooseAmountAnswer(count)))
    UsedAndAnswered(started, accepted(answer(counted, p1,
      ImperialSeal.destinationDecisionId, pick(to))))

  test("it moves the chosen warbands to another Imperial site"):
    val ready = exileAmongImperialWarbands.ready
    val done = moving(ready, 2, DecisionOptionRef.Site(peaks))
    val end = after(done.answered)
    assertEquals(imperialAt(end, home), 1)
    assertEquals(imperialAt(end, peaks), 3)
    assertEquals(NoteText.said(ImperialSeal, done.answered.events), Vector(
      NoteText.Said(NoteKey.Used,
        s"Moved 2 Imperial warbands to ${peaks.value}.", covers = true)))

  test("it moves them to an Imperial player's board"):
    val ready = exileAmongImperialWarbands.ready
    val before = Look(ready).player(p3).board.warbands
    val done = moving(ready, 1, DecisionOptionRef.Player(p3))
    assertEquals(Look(after(done.answered)).player(p3).board.warbands,
      before + 1)
    assertEquals(NoteText.said(ImperialSeal, done.answered.events), Vector(
      NoteText.Said("used.board",
        s"Moved 1 Imperial warband to ${p3.value}'s board.", covers = true)))

  test("it offers the other Imperial sites and every Imperial board, not " +
      "its own site"):
    val started = accepted(use(exileAmongImperialWarbands.ready,
      ImperialSeal, source))
    val counted = accepted(answer(started, p1, ImperialSeal.countDecisionId,
      DecisionAnswer.ChooseAmountAnswer(1)))
    assertEquals(offered(counted, p1).toVector.flatten.map(_._2).toSet,
      Set[DecisionOptionRef](DecisionOptionRef.Site(peaks),
        DecisionOptionRef.Player(p2), DecisionOptionRef.Player(p3))
        .map(_.wireId))

  test("an Imperial pawn at the site, or no Imperial warbands there, makes " +
      "it unusable"):
    val withPawn = exileAmongImperialWarbands.pawn(p3, at = home).ready
    assert(!ImperialSeal.usable(catalog, withPawn, p1, source))
    val noWarbands = Table.start.imperial(p2, p3).turn(p1, Phase.Act)
      .pawn(p1, at = home).relic(p1, ImperialSealCard)
      .secrets(p1, faceUp = 1).warbandsAt(peaks, p2, 1).ready
    assert(!ImperialSeal.usable(catalog, noWarbands, p1, source))

  test("replay rebuilds the same game"):
    val ready = exileAmongImperialWarbands.ready
    val done = moving(ready, 2, DecisionOptionRef.Site(peaks))
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
```

`PhasePowerHarness.offered` returns `Option[Vector[(kind, wireId)]]`, so the test compares wire ids.

- [ ] **Step 2: Run it and check it fails**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.ImperialSealSuite"`
Expected: compile failure, `Not found: ImperialSeal`.

- [ ] **Step 3: Add `imperialSites`**

In `SiteRulers.scala`, after `protectedAt`:

```scala
  /** The Imperial sites in play, in map order: the sites the Empire holds
    * with its warbands (rules: "Sites with Imperial warbands are called
    * Imperial sites"). */
  def imperialSites(ready: ReadyGame): Vector[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      ready.game.current.map.sites.get(site).exists(state =>
        SiteRule.ruler(state.forces, ready.game.current.players) ==
          Right(SiteRuler.Empire)))
```

- [ ] **Step 4: Write `ImperialSeal`**

Remove `ImperialSealCard` from `HoldingRelics.scala`. Create `src/main/scala/distantlands/gameplay/powers/action/ImperialSeal.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.catalog.{ExecutableCatalog, PrintedPower, Relic}
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object ImperialSealCard extends Relic(RelicId("R26"), "Imperial Seal", value = 35, defense = 2):
  val power = PrintedPower(PowerId("relic.imperial-seal"),
    persistent = false, cost = Cost(secret = 1),
    text = "**ACTION:** If no Imperial pawns are at your site, move any " +
      "number of Imperial warbands from your site to any Imperial " +
      "site or Imperial player board.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Imperial Seal (R26): "ACTION: If no Imperial pawns are at your site,
  * move any number of Imperial warbands from your site to any Imperial site
  * or Imperial player board." (catalog batch 10, slice 1).
  *
  * It is usable when no Imperial player's pawn stands at the user's site
  * and Imperial warbands are there, the user's own pawn counting when they
  * are Imperial. After the secret it asks how many, from 1, then one
  * destination (ruling 4): another Imperial site
  * ([[SiteRulers.imperialSites]]) or an Imperial player's board. Its line
  * covers the generic move line: "Moved 2 Imperial warbands to {site}." or
  * "... to {Purple}'s board."
  */
case object ImperialSeal extends PaidAction(ImperialSealCard.power):
  val countDecisionId: String = "power.imperial-seal.count"
  val destinationDecisionId: String = "power.imperial-seal.destination"
  private val notes = NoteSet(id)
  /** "Moved {0} Imperial warband(s) to {1}." */
  val toSite: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Moved "), NotePart.Arg(0), NotePart.Text(" Imperial "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1), NotePart.Text(".")))
  /** "Moved {0} Imperial warband(s) to {1}'s board." */
  val toBoard: NoteKey = notes.key("used.board", Vector(
    NotePart.Text("Moved "), NotePart.Arg(0), NotePart.Text(" Imperial "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1), NotePart.Text("'s board.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(countDecisionId, destinationDecisionId)

  /** The Imperial warbands at `site`. */
  def imperialAt(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites.get(site).map(_.forces) match
      case Some(SiteForces.Occupied(ForceKind.Imperial, n)) => n
      case _ => 0

  private def imperialPawnAt(ready: ReadyGame, site: SiteId): Boolean =
    ready.game.current.players.exists(state =>
      ready.game.current.pawnSite(state.player).contains(site) &&
        Imperials.isImperial(ready, state.player))

  /** Where the warbands at `site` may go. */
  private def destinations(ready: ReadyGame, site: SiteId)
      : Vector[DecisionOption] =
    SiteRulers.imperialSites(ready).filterNot(_ == site).map(other =>
      DecisionOption.Site(DecisionOptionRef.Site(other))) ++
      Imperials.players(ready).map(player =>
        DecisionOption.Player(DecisionOptionRef.Player(player)))

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    ready.game.current.pawnSite(player).exists(site =>
      !imperialPawnAt(ready, site) && imperialAt(ready, site) > 0 &&
        destinations(ready, site).nonEmpty)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    ready.game.current.pawnSite(player).toRight(
      OathViolation.StartUnavailable("Your pawn is not on the map")).map(
      site => Sequence(Vector[Operation](
        Branch((live, _) => Vector(Decide(countDecisionId, player,
          DecisionQuery.ChooseAmount(1, imperialAt(live, site),
            Some("Imperial Seal: move how many Imperial warbands?"),
            "Move")))),
        Branch((live, _) => Vector(Decide(destinationDecisionId, player,
          DecisionQuery.ChooseOne(destinations(live, site),
            heading = Some("Imperial Seal: move them where?"))))),
        BuildOps((_, pending) => moved(site, pending), required = true),
        Note(id, movedNote(_, site, source), covers = true))))

  private def moved(site: SiteId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] = for
    count <- PowerAnswers.amount(pending, countDecisionId)
      .toRight(PowerAnswers.missing(countDecisionId))
    to <- PowerAnswers.one(pending, destinationDecisionId).collect {
      case DecisionOptionRef.Site(other) => Location.Site(other)
      case DecisionOptionRef.Player(board) => Location.PlayArea(board)
    }.toRight(PowerAnswers.missing(destinationDecisionId))
  yield Vector(Move(Piece.Warbands(ForceKind.Imperial, count),
    PositionedLocation(Location.Site(site)), PositionedLocation(to)))

  /** The move just made, read as the warbands that left `site`. */
  private def movedNote(states: NoteStates, site: SiteId,
      source: DecisionOptionRef): Option[LogNote] = for
    card <- PowerSourceRef.of(source)
    (before, _) <- states.previous
    count = imperialAt(before, site) - imperialAt(states.now, site)
    if count > 0
    to <- NoteSupport.answer(states, destinationDecisionId)
    line <- to match
      case DecisionOptionRef.Site(other) =>
        Some(toSite(card, NoteArg.Number(count), NoteArg.Site(other)))
      case DecisionOptionRef.Player(board) =>
        Some(toBoard(card, NoteArg.Number(count), NoteArg.Player(board)))
      case _ => None
  yield line
```

Import `oathdigital.gameplay.NoteSupport`. Check `states.previous`'s shape in `GiantPython.movedNote`, which destructures `(before, _) <- states.previous`. Check whether `NotePart.Plural` counts a `NoteArg.Number`; its doc says it does.

In `PhasePowerCatalog.scala`, add `ImperialSeal` to the `Vector[PhasePower](WaysideInn, ...)` list. The `action._` import already reaches it, or add it to the named `action` import.

- [ ] **Step 5: Run the suite and check it passes**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.ImperialSealSuite"`
Expected: PASS.

- [ ] **Step 6: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.DeclaredNoteKeysSuite oathdigital.gameplay.powers.UnimplementedPowersSuite"`
Expected diff:
- `declared-keys.txt` gains exactly the two `relic.imperial-seal` lines.
- `unimplemented.txt` loses exactly `relic.imperial-seal`.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/action/ImperialSeal.scala src/main/scala/distantlands/catalog/holding/HoldingRelics.scala src/main/scala/distantlands/gameplay/SiteRulers.scala src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala src/test/scala/distantlands/gameplay/powers/action/ImperialSealSuite.scala src/test/resources/notes/declared-keys.txt src/test/resources/powers/unimplemented.txt
git commit -m "$(cat <<'EOF'
feat(powers): Imperial Seal moves Imperial warbands from your site

A paid Action, usable with no Imperial pawn at the user's site and
Imperial warbands there. It asks a count, then one destination: another
Imperial site or an Imperial player's board.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Roadmap, gates and the spec status

**Files:**
- Modify: `docs/ROADMAP.md` (sections "Phase - Catalog batch - engine-blocked" and "Phase - Empire cards")
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md` (status line)

- [ ] **Step 1: Update the roadmap**

In **Phase - Empire cards**, delete the Council Seat, Naysayers, Imperial Seal and Bandit Forest items outright (memory: "Roadmap deletes done work"). Grand Mask is deferred with Bandit Crown (spec, "Deferred"). Move it into **Phase - Catalog batch - engine-blocked**: change `- **As-if ruler:** Bandit Crown.` to `- **As-if ruler:** Bandit Crown, Grand Mask.` Then delete the now-empty **Phase - Empire cards** section, including its intro paragraph and the closing False Prophet sentence. False Prophet is already listed under engine-blocked.

Remove the "Phase - Empire cards" entry from the roadmap's phase index, if one lists it. Run `grep -n "Empire cards" docs/ROADMAP.md`; it should report nothing afterwards.

- [ ] **Step 2: Mark the slice in the spec**

In the spec, change `**Status:** designed 2026-11-03. Not started.` to `**Status:** designed 2026-11-03. Slice 1 landed.`

- [ ] **Step 3: Run the gates**

Run each, and expect each to pass:
- `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
- `python3 scripts/check-architecture.py`
- `python3 scripts/check-markdown-links.py`

If a golden game-log test fails because a menu now shows Council Seat's warning or Bandit Forest's refusal, read the diff. Regenerate with `GAMELOG_GOLDEN=write` only when the change is one this slice explains.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md
git commit -m "$(cat <<'EOF'
docs: close catalog batch 10 slice 1 on the roadmap

Council Seat, Naysayers, Imperial Seal and Bandit Forest leave the
roadmap; Grand Mask joins Bandit Crown under the as-if ruler seam.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
