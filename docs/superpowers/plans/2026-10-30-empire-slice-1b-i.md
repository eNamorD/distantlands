# Empire Slice 1b-i Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Campaign in a game with a Chancellor and Citizens plays
correctly: the Chancellor defends Imperial sites, Imperial players raid each
other, a Citizen conquers Imperial sites, and every warband the Campaign
moves or kills is the right colour.

**Architecture:** The Campaign already asks one function who defends a site,
`CampaignSetup.defenderAt`. It learns that the Empire's sites are defended by
the Chancellor, and the targets question, the battle-plan context and the
plan sacrifice all follow, because they read it. The four places that build
a player's warbands as `ForceKind.Exile(lineage)` switch to
`PlayerForceKind.of`. No new procedure, decision or wire type is added.
Imperial Tactics (shared plans and split costs) is slice 1b-ii and is not in
this plan.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 1b" and "1b-i: Imperial Infighting", and
ruling R9).

## Global Constraints

- Work in a worktree off `0.2.0-alpha`, never in the main checkout. Symlink
  the main checkout's `.tooling` and `node_modules` into the worktree before
  running `./sbtw`; remove the symlinks before `git worktree remove --force`.
- Never run `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- The compiler runs with `-Werror`: unused imports, values and match arms
  fail the build.
- Gates before the last commit: `./sbtw clean coverage test coverageReport`
  with statement coverage of at least 92%, `./sbtw frontend/test`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.
- Game start keeps refusing Imperial roles ("Imperial roles require the
  Empire"); this slice is reached only through test tables
  (`Table.imperial`).
- Imperial Tactics is out of this slice: do not let a player use battle
  plans from another player's play area or from a site they do not defend,
  and do not split plan costs. `PlanContext` and `CampaignPlanChoice` keep
  their behaviour, except what follows from `defenderAt`.
- `CampaignDefender` gains no case; the wire is unchanged.
- Unavailable options stay on offer, disabled with a hover reason; hide only
  an option the player's role can never use.
- Test helper names state what they build.
- Docs, comments and commit messages are normal prose.

## File Structure

| File | Responsibility |
|---|---|
| `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignSetup.scala` | `defenderAt`: an Empire site's defender is the Chancellor |
| `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignConquest.scala` | Placed survivors take the attacker's warband kind |
| `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignBattle.scala` | Attacker kills and Raid board losses take the player's warband kind |
| `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignPlanApplication.scala` | A plan sacrifice from a board takes the player's warband kind |
| `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignNotes.scala` | Loss notes read the player's warband kind with no Exile fallback |
| `src/main/scala/oathdigital/gameplay/powers/action/SiegeEngines.scala` | The Imperial kill line |
| `src/main/scala/oathdigital/gameplay/powers/campaign/MilitaryParade.scala` | Comment: ruling R9 |
| `src/test/scala/oathdigital/gameplay/actions/campaign/ImperialInfightingSuite.scala` (new) | Defenders, targets and raids with the Empire |
| `src/test/scala/oathdigital/gameplay/ImperialCampaignBattleSuite.scala` (new) | Warband kinds through whole Campaigns |
| `src/test/scala/oathdigital/gameplay/EmpireCampaignScenarioSuite.scala` (new) | The slice scenario through the service |

---

### Task 1: The Chancellor defends Imperial sites

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignSetup.scala:130-139` (`defenderAt`)
- Test: `src/test/scala/oathdigital/gameplay/actions/campaign/ImperialInfightingSuite.scala` (new)

**Interfaces:**
- Consumes: `Imperials.chancellor(ready: ReadyGame): Option[PlayerId]`
  (model, slice 1a); `Table.imperial(chancellor, citizens*)`;
  `CampaignFixture.board`, `CampaignFixture.on`, `CampaignFixture.rules`.
- Produces: `CampaignSetup.defenderAt(ready, site)` returns
  `Some(CampaignDefender.Player(chancellor))` for a site the Empire holds,
  and `None` there when no Chancellor is seated. Every reader of
  `defenderAt` (`conquestDefender`, `targetOptions`,
  `CampaignTargets.withheld`, `PlanContext.rules`, `WarningSignals`,
  `CampaignPlanApplication.sacrificeFrom`) follows without change.

Why `defenderAt` and not `SiteRule.rulingPlayer`: on a Citizen's turn the
Citizen rules the Empire's sites (Seal of the Empire), but the Chancellor is
still the defender of a Conquest against them (Steward of Imperial Domains,
Imperial Infighting), and ruling R10 gives plans at those sites to the
Chancellor's side. So the Campaign keeps its own question.

- [ ] **Step 1: Write the failing tests**

The test lives in package `oathdigital.gameplay.actions.campaign` because
`CampaignTargets` is `private[campaign]`. Create the directory
`src/test/scala/oathdigital/gameplay/actions/campaign/`.

```scala
package oathdigital.gameplay.actions.campaign

import oathdigital.gameplay.CampaignFixture
import oathdigital.gameplay.CampaignFixture.{Board, board, on}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker,
  WalkerPowers, WalkerProcedureRegistry}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Outcomes.ready
import oathdigital.testkit.Unwrap.{accepted, as, present}

/** Imperial Infighting (CR p. 35): who defends the Empire's sites, which
  * Imperial player may target them, and Imperial players raiding each
  * other. */
class ImperialInfightingSuite extends munit.FunSuite:
  /** The board's p1 at Ancient City, now held by the Empire with two purple
    * warbands, and a second Empire site, `secondEmpireSite`, with two more.
    * p2 is the Chancellor; `citizens` are seated as Citizens. p2 and p3
    * stand elsewhere. p1 has 5 board warbands and 7 Supply. */
  private def empireOrigin(citizens: PlayerId*): Board =
    val b = board(warbands = 5, supply = 7)
    on(b)(_.imperial(p2, citizens*).warbandsAt(b.origin, p2, 2)
      .warbandsAt(secondEmpireSite(b), p2, 2))

  /** The first in-play site after the origin. */
  private def secondEmpireSite(b: Board): SiteId =
    b.ready.game.current.map.inPlay.filter(_ != b.origin).head

  private def defender(b: Board, site: SiteId): Option[CampaignDefender] =
    CampaignSetup.defenderAt(b.ready, site)

  test("the Chancellor defends every site the Empire holds"):
    val b = empireOrigin()
    assertEquals(defender(b, b.origin), Some(CampaignDefender.Player(p2)))
    assertEquals(defender(b, secondEmpireSite(b)),
      Some(CampaignDefender.Player(p2)))

  test("with no Chancellor seated, a purple site has no defender"):
    val b = board(warbands = 5, supply = 7)
    val noChancellor = on(b)(_.imperial(p2).warbandsAt(b.origin, p2, 2)
      .update(ready => ready.updateCampaign(campaign => campaign.copy(
        lineages = campaign.lineages.map { case (id, lineage) =>
          id -> lineage.copy(role = Role.Exile) }))))
    assertEquals(defender(noChancellor, b.origin), None)

  test("an Exile at an Empire site conquers against the Chancellor"):
    val b = empireOrigin()
    assertEquals(CampaignSetup.conquestDefender(b.ready, p1),
      Some(CampaignDefender.Player(p2)))
    assert(CampaignSetup.legalKinds(b.ready, p1)
      .contains(CampaignKind.Conquest))

  test("a Citizen conquers against the Chancellor and may add the Empire's " +
      "other sites"):
    val b = empireOrigin(p1)
    assertEquals(CampaignSetup.conquestDefender(b.ready, p1),
      Some(CampaignDefender.Player(p2)))
    assertEquals(CampaignSetup.targetOptions(b.ready, p1,
      CampaignKind.Conquest, CampaignDefender.Player(p2)),
      Vector(DecisionOption.Site(DecisionOptionRef.Site(secondEmpireSite(b)))))

  test("the Chancellor cannot conquer the Empire's own sites"):
    val b = board(warbands = 5, supply = 7)
    val chancellorAtHome = on(b)(_.imperial(p1, p2)
      .warbandsAt(b.origin, p1, 2))
    assertEquals(CampaignSetup.conquestDefender(chancellorAtHome.ready, p1),
      None)
    assertEquals(CampaignSetup.legalKinds(chancellorAtHome.ready, p1),
      Vector.empty)

  test("a Chancellor conquering a bandit site sees the Empire's sites as " +
      "theirs"):
    val b = board(warbands = 5, supply = 7)
    val site = secondEmpireSite(b)
    val chancellor = on(b)(_.imperial(p1).warbandsAt(site, p1, 2))
    val reasons = CampaignTargets.withheld(chancellor.ready, p1,
      CampaignKind.Conquest, b.origin, CampaignDefender.Bandits,
      Vector.empty).collect {
        case DecisionOption.Unavailable(DecisionOption.Site(ref),
            OptionReason.Written(text)) => ref.id -> text }.toMap
    assertEquals(reasons.get(site), Some("You rule this site"))

  test("an Exile conquering a bandit site sees the Empire's sites as not " +
      "the defender's"):
    val b = board(warbands = 5, supply = 7)
    val site = secondEmpireSite(b)
    val exile = on(b)(_.imperial(p2).warbandsAt(site, p2, 2))
    val reasons = CampaignTargets.withheld(exile.ready, p1,
      CampaignKind.Conquest, b.origin, CampaignDefender.Bandits,
      Vector.empty).collect {
        case DecisionOption.Unavailable(DecisionOption.Site(ref),
            OptionReason.Written(text)) => ref.id -> text }.toMap
    assertEquals(reasons.get(site), Some("Not ruled by the defender"))

  test("a Citizen may raid the Chancellor and another Citizen at their site"):
    val b = empireOrigin(p1, p3)
    val shared = on(b)(_.pawn(p2, at = b.origin).pawn(p3, at = b.origin))
    assertEquals(CampaignSetup.raidDefenders(shared.ready, p1).toSet,
      Set(p2, p3))

  test("the Chancellor may raid a Citizen at their site"):
    val b = board(warbands = 5, supply = 7)
    val shared = on(b)(_.imperial(p1, p2).pawn(p2, at = b.origin))
    assertEquals(CampaignSetup.raidDefenders(shared.ready, p1), Vector(p2))

  test("a Citizen's Conquest targets question tallies the Empire's sites"):
    val b = empireOrigin(p1)
    val game = CampaignFixture.rules()
    val started = accepted(game.startWalker(Ready(b.ready),
      ActionRef.Campaign, p1))
    val current = ready(started.state)
    val parked = new ParkedDecisionAssertions(catalog,
      WalkerPowerCatalog.default(catalog))
    val facts = present(parked.parkedDecision(started.state), "a park")
    assertEquals(facts.decision, CampaignIds.targets)
    val pending = present(current.game.current.walkerPending, "a pending walk")
    val tree = accepted(WalkerProcedureRegistry.rebuild(facts.procedure,
      catalog, current, p1, Vector.empty))
    val query = as[DecisionQuery.ChooseMany](ProcedureWalker.openDecisions(
      current, tree, pending, WalkerPowers.selected(
        WalkerPowerCatalog.default(catalog), Vector.empty)).head.query)
    val second = DecisionOptionRef.Site(secondEmpireSite(b))
    assert(query.options.contains(DecisionOption.Site(second)),
      query.options.toString)
    assertEquals(query.tallies.find(_.label == CampaignTargets.ForceLabel)
      .flatMap(_.values.get(second)), Some(2))
```

If `DecisionTally` names its map differently from `values`, read the field
name from `model/Decisions.scala` and use it; the assertion stays the same.
If `Table` has no `update` step reachable from a test, use the lineage-role
edit that `Table.imperial` uses (`Table.scala`, around line 284).

- [ ] **Step 2: Run them to see the defender tests fail**

Run: `./sbtw "testOnly oathdigital.gameplay.actions.campaign.ImperialInfightingSuite"`
Expected: FAIL. "the Chancellor defends every site the Empire holds"
gets `None`; the Exile and Citizen Conquest tests get `None` or no
Conquest. The raid tests and the "no Chancellor" test may already pass;
they pin behaviour that must not change.

- [ ] **Step 3: Resolve an Empire site to the Chancellor**

In `CampaignSetup.scala`, replace `defenderAt` and its comment:

```scala
  /** Who defends `site` in a Campaign: its ruling player, the bandits, or
    * for a site the Empire holds the Chancellor, on every turn (Steward of
    * Imperial Domains, Imperial Infighting). A Campaign does not ask
    * `SiteRule.rulingPlayer`: on a Citizen's turn the Citizen rules the
    * Empire's sites, but the Chancellor still defends them, and their
    * battle plans are the Chancellor's side's (ruling R10). `None` when the
    * site is unruled or its rule is corrupt, and for a purple site with no
    * Chancellor seated.
    */
  def defenderAt(ready: ReadyGame, site: SiteId): Option[CampaignDefender] =
    ready.game.current.map.sites.get(site).flatMap(state =>
      SiteRule.ruler(state.forces, ready.game.current.players).toOption)
      .flatMap:
        case SiteRuler.Bandits => Some(CampaignDefender.Bandits)
        case SiteRuler.Player(player) => Some(CampaignDefender.Player(player))
        case SiteRuler.Empire => Imperials.chancellor(ready)
          .map(CampaignDefender.Player(_))
        case _ => None
```

`Imperials` is in `oathdigital.model`, which the file already imports with
`oathdigital.model._`.

- [ ] **Step 4: Run the suite and the Campaign suites**

Run: `./sbtw "testOnly oathdigital.gameplay.actions.campaign.ImperialInfightingSuite oathdigital.gameplay.Campaign* oathdigital.gameplay.powers.campaign.*"`
Expected: PASS. No all-Exile Campaign test changes, because no all-Exile
board has a purple site.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/actions/campaign/CampaignSetup.scala src/test/scala/oathdigital/gameplay/actions/campaign/ImperialInfightingSuite.scala
git commit -m "feat(empire): the Chancellor defends Imperial sites in a Campaign"
```

---

### Task 2: Campaign warbands follow the role

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignConquest.scala:27-36` (`placements`)
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignBattle.scala` (`attackerKills`, `raidBoardLosses`)
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignPlanApplication.scala:172-185` (`killOps`)
- Modify: `src/main/scala/oathdigital/gameplay/actions/campaign/CampaignNotes.scala:316-322` (`forceKindOf`)
- Test: `src/test/scala/oathdigital/gameplay/ImperialCampaignBattleSuite.scala` (new)

**Interfaces:**
- Consumes: `PlayerForceKind.of(ready: ReadyGame, player: PlayerState): Option[ForceKind]`;
  Task 1's `defenderAt`; `PlanDriver.commit`, `PlanDriver.Run`
  (`pick`, `answer`, `declineSacrifice`, `ops`, `since`, `state`),
  `PlanDriver.winningDice`, `PlanDriver.losingDice`,
  `CampaignFixture.withAdviserFor`.
- Produces: no new names. After this task no Campaign file builds
  `ForceKind.Exile(...)`; check with
  `grep -n "ForceKind.Exile" src/main/scala/oathdigital/gameplay/actions/campaign/*.scala`
  (expected: no output).

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.gameplay

import oathdigital.gameplay.CampaignFixture.{Board, on, rules,
  withAdviserFor}
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.PlanDriver.{awaits, commit,
  losingDice, winningDice}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseAmountAnswer
import oathdigital.testkit.{CatalogNames, Look}
import oathdigital.testkit.Table.{p1, p2, p3}

/** Whole Campaigns with Imperial players (Empire slice 1b-i): every
  * warband a Campaign places, kills or sacrifices is of its owner's kind,
  * purple for an Imperial player. Every attack die is a sword
  * (`winningDice`) or half a sword (`losingDice`), and every defense die is
  * blank. No one holds the Oathkeeper title, so no defender has the title's
  * plan. */
class ImperialCampaignBattleSuite extends munit.FunSuite:
  private def purple(n: Int) = Piece.Warbands(ForceKind.Imperial, n)
  private def boardOf(player: PlayerId) =
    PositionedLocation(Location.PlayArea(player))

  /** The fixture board with p2 the Chancellor and `citizens` Citizens, and
    * no title holder. p1 stands at the bandit-held Ancient City with 5
    * board warbands. */
  private def seated(citizens: PlayerId*): Board =
    on(CampaignFixture.board(warbands = 5, supply = 7))(
      _.imperial(p2, citizens*).oathkeeper(None))

  test("a Citizen who conquers places purple warbands, and the site becomes " +
      "Imperial"):
    val b = seated(p1)
    val run = commit(rules(winningDice), b, 4).declineSacrifice(p1)
    assert(awaits(run, p1, CampaignIds.placement))
    val placed = run.answer(p1, CampaignIds.placement, ChooseAmountAnswer(3))
    assertEquals(Look(placed.state).forces(b.origin),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(Look(placed.state).boardWarbands(p1), 2)

  test("a defeated Citizen loses purple warbands from their board"):
    val b = seated(p1)
    val run = commit(rules(losingDice), b, 2).declineSacrifice(p1)
    // Two survivors against two bandits: half of them, one, dies.
    assert(run.ops.contains(Kill(purple(1), boardOf(p1))), run.ops.toString)

  test("an Exile who conquers an Empire site kills its purple warbands and " +
      "returns half to the Chancellor's board"):
    val exile = seated()
    val b = on(exile)(_.warbandsAt(exile.origin, p2, 2).boardWarbands(p2, 0))
    val run = commit(rules(winningDice), b, 4)
    val fought = (if awaits(run, p2, CampaignIds.defenderPlan) then
      run.pick(p2, CampaignIds.defenderPlan, CampaignIds.finish) else run)
      .declineSacrifice(p1)
    assertEquals(Look(fought.state).forces(b.origin), SiteForces.Empty)
    // The two that died at the site: one of them returns to p2's board.
    assertEquals(Look(fought.state).boardWarbands(p2), 1)
    assert(fought.ops.contains(Kill(purple(2),
      PositionedLocation(Location.Site(b.origin)))), fought.ops.toString)

  test("an Exile who raids a Citizen kills half of the Citizen's purple " +
      "board"):
    val exile = seated(p3)
    val b = on(exile)(_.pawn(p3, at = exile.origin).boardWarbands(p3, 4))
    val run = commit(rules(winningDice), b, 4, raid = true)
    val fought = (if awaits(run, p3, CampaignIds.defenderPlan) then
      run.pick(p3, CampaignIds.defenderPlan, CampaignIds.finish) else run)
      .declineSacrifice(p1)
    assert(fought.ops.contains(Kill(purple(2), boardOf(p3))),
      fought.ops.toString)

  test("a Citizen raided by another Citizen sacrifices a purple warband " +
      "from their board for Wrestlers"):
    val wrestlers = CatalogNames.denizen("Wrestlers")
    val citizens = seated(p1, p3)
    val base = on(citizens)(_.pawn(p3, at = citizens.origin)
      .boardWarbands(p3, 3))
    val b = withAdviserFor(base, p3, wrestlers, Orientation.FaceUp)
    val run = commit(rules(losingDice), b, 2, raid = true)
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    val picked = run.pick(p3, CampaignIds.defenderPlan,
      DecisionOptionRef.Denizen(wrestlers))
    assert(picked.since(run).contains(Sacrifice(p3, purple(1), boardOf(p3))),
      picked.since(run).toString)
```

`seated(p1)` makes p1 a Citizen; with no arguments p1 stays an Exile.
Every board's origin is Ancient City, where p1 stands.

If `commit` with `raid = true` parks on a defender question because two
pawns stand at the origin, the board has a stray pawn: the fixture puts p2
and p3 at a third site, so only the pawn the test moves is there.

- [ ] **Step 2: Run them to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.ImperialCampaignBattleSuite"`
Expected: FAIL. The placement, Raid-loss and Wrestlers tests fail with an
operation refused for the wrong warband kind (an Exile kind moved from or
killed on an Imperial board), or with the wrong recorded `Kill`. The
Exile-conquest test may already pass, since the site's own force kind is
read from the site.

- [ ] **Step 3: Use the player's warband kind**

`CampaignConquest.placements`:

```scala
  private def placements(ready: ReadyGame, actor: PlayerId, result: CampaignResult,
      pending: PendingTree): Either[OathViolation, Vector[CoreOperation]] =
    ready.game.current.players.find(_.player == actor)
      .flatMap(PlayerForceKind.of(ready, _)).toRight(
      OathViolation.InvalidEventOrder("the Campaign's attacker is not in the game"))
      .map(kind => CampaignAnswers.placements(pending, result.targetSites)
        .filter(_._2 > 0).map { case (site, count) => Move(
          Piece.Warbands(kind, count),
          PositionedLocation(Location.PlayArea(actor)),
          PositionedLocation(Location.Site(site))): CoreOperation })
```

`CampaignBattle.attackerKills`: drop the Exile fallback, so a missing kind
is the same violation as a missing attacker:

```scala
    ready.game.current.players.find(_.player == attacker)
      .flatMap(PlayerForceKind.of(ready, _)).toRight(
      OathViolation.InvalidEventOrder(
        "the Campaign's attacker is not in the game")).map { kind =>
      val fromSite = origin.fold(0) { at =>
```

and delete the two lines that bound `player` and `kind` before
`fromSite`. The rest of the body is unchanged.

`CampaignBattle.raidBoardLosses`:

```scala
  private def raidBoardLosses(ready: ReadyGame, result: CampaignResult)
      : Vector[CoreOperation] = result.defender match
    case CampaignDefender.Player(player) =>
      ready.game.current.players.find(_.player == player).toVector.flatMap:
        defender =>
          val killed = defender.board.warbands / 2
          PlayerForceKind.of(ready, defender).filter(_ => killed > 0)
            .map(kind => Kill(Piece.Warbands(kind, killed),
              PositionedLocation(Location.PlayArea(player)))).toVector
    case CampaignDefender.Bandits => Vector.empty
```

`CampaignPlanApplication.killOps`, the board branch:

```scala
      case _ => ready.game.current.players.find(_.player == player)
        .flatMap(PlayerForceKind.of(ready, _))
```

`CampaignNotes.forceKindOf`:

```scala
  private def forceKindOf(ready: ReadyGame, owner: Option[PlayerId])
      : Option[ForceKind] = owner match
    case None => Some(ForceKind.Bandit)
    case Some(player) => ready.game.current.players
      .find(_.player == player).flatMap(PlayerForceKind.of(ready, _))
```

Update each touched doc comment that says "Exile" warbands to "the
player's own warbands" where it does.

- [ ] **Step 4: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.ImperialCampaignBattleSuite oathdigital.gameplay.Campaign* oathdigital.gameplay.powers.campaign.* oathdigital.application.gamelog.*"`
Expected: PASS. The golden Campaign logs do not change: every all-Exile
player's kind is still their lineage's.

Then run: `grep -n "ForceKind.Exile" src/main/scala/oathdigital/gameplay/actions/campaign/*.scala`
Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/actions/campaign/CampaignConquest.scala src/main/scala/oathdigital/gameplay/actions/campaign/CampaignBattle.scala src/main/scala/oathdigital/gameplay/actions/campaign/CampaignPlanApplication.scala src/main/scala/oathdigital/gameplay/actions/campaign/CampaignNotes.scala src/test/scala/oathdigital/gameplay/ImperialCampaignBattleSuite.scala
git commit -m "feat(empire): Campaign warbands follow the player's role"
```

---

### Task 3: Siege Engines, Military Parade and leftover comments

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/SiegeEngines.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/campaign/MilitaryParade.scala:17-29` (doc comment only)
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/QuickExit.scala:15-22` (doc comment rewrap only)
- Modify: `src/main/scala/oathdigital/gameplay/SiteRulers.scala:5-11` (doc comment rewrap only)
- Modify: `docs/rules/ambiguities.md` (ruling R9 under "Empire rulings")
- Test: `src/test/scala/oathdigital/gameplay/powers/action/SiegeEnginesSuite.scala`

**Interfaces:**
- Consumes: `Table.imperial`.
- Produces: `SiegeEngines.imperial: NoteKey`, key name `"used.imperial"`,
  text "Killed {n} Imperial warband(s) at {site}."

- [ ] **Step 1: Write the failing test**

Add to `SiegeEnginesSuite`, after the bandits test:

```scala
  test("purple warbands it kills are logged as the Empire's"):
    val Killed(_, t, done) = killAt(neighbour,
      _.imperial(victim).warbandsAt(neighbour, victim, 3))
    assertEquals(forcesAt(after(done), neighbour),
      SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(warbandBank(after(done), ForceKind.Imperial),
      warbandBank(after(t), ForceKind.Imperial) + 2)
    assertEquals(NoteText.said(SiegeEngines, done.events), Vector(
      NoteText.Said("used.imperial",
        s"Killed 2 Imperial warbands at ${neighbour.value}.",
        covers = false)))
```

- [ ] **Step 2: Run it to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.SiegeEnginesSuite"`
Expected: FAIL: the notes are `Vector()`, since purple warbands have no
single owner.

- [ ] **Step 3: Add the Imperial line**

In `SiegeEngines.scala`, after `bandits`:

```scala
  /** "Killed {n} Imperial warbands at {site}." Purple warbands are the
    * Empire's, not one player's. */
  val imperial: NoteKey = notes.key("used.imperial", Vector(
    NotePart.Text("Killed "), NotePart.Arg(0),
    NotePart.Plural(0, " Imperial warband at ", " Imperial warbands at "),
    NotePart.Arg(1), NotePart.Text(".")))
```

In `killNote`, between the bandit branch and the owner branch:

```scala
        else if kind == ForceKind.Bandit then
          Some(bandits(card, NoteArg.Number(lost), NoteArg.Site(site)))
        else if kind == ForceKind.Imperial then
          Some(imperial(card, NoteArg.Number(lost), NoteArg.Site(site)))
        else owner(step._1, kind).map(whose => killed(card,
```

Replace the comment on `owner` with:

```scala
  /** The player whose warbands are of `kind`: an Exile kind has one owner.
    * Purple warbands have none and take their own line. */
```

If a note-key catalog or golden test lists every declared key (search with
`grep -rn '"used.bandits"' src/test`), add `"used.imperial"` beside it.

- [ ] **Step 4: Fix the comments**

`MilitaryParade.scala`: replace the sentence "Every game is all-Exile, so
the Imperial Allies clause does nothing." with "The Imperial Allies clause
is a base-game rule; in New Foundations the plan counts only the one
enemy's advisers (ruling R9)." Change no code.

`QuickExit.scala`: rewrap the doc comment so no line passes 80 columns.
The words stay the same.

`SiteRulers.scala`: rewrap the object's doc comment (lines 5-11) into
full lines; the line "Whether a player is an enemy of a ruler is" is cut
short. The words stay the same.

`docs/rules/ambiguities.md`, under "Empire rulings", after R8, in the same
format:

```markdown
R9. **Military Parade counts one enemy.** The New Foundations card applies to the one enemy only. Its "including Imperial Allies" text is a base-game concept and is ignored ([Empire phase design](../superpowers/specs/2026-10-29-empire-phase-design.md#rulings)).
```

Match the link style R8 uses; if R8 has no link, drop it from R9 too.

- [ ] **Step 5: Run the suites**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.* oathdigital.gameplay.powers.campaign.MilitaryParadeSuite"`
Expected: PASS.

Run: `python3 scripts/check-markdown-links.py`
Expected: "Markdown link check passed".

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/action/SiegeEngines.scala src/main/scala/oathdigital/gameplay/powers/campaign/MilitaryParade.scala src/main/scala/oathdigital/gameplay/powers/action/QuickExit.scala src/main/scala/oathdigital/gameplay/SiteRulers.scala src/test/scala/oathdigital/gameplay/powers/action/SiegeEnginesSuite.scala docs/rules/ambiguities.md
git commit -m "feat(empire): log Siege Engines kills of Imperial warbands"
```

Add any note-catalog test file you changed in Step 3 to the `git add`.

---

### Task 4: Scenario, docs and gates

**Files:**
- Test: `src/test/scala/oathdigital/gameplay/EmpireCampaignScenarioSuite.scala` (new)
- Modify: `docs/rules/implementation-traceability.md` (row 104)
- Modify: `docs/rules/rules-reference.md` ("Imperial laws" section)
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md` (Status line)
- Modify: `docs/ROADMAP.md` (the slice 1b item)

**Interfaces:**
- Consumes: Tasks 1 and 2; `Table.service(campaignDice = ...)`,
  `oathdigital.testkit.Dice.raid`, `MenuPlay.starting`,
  `GameCommand.ResolveWalker`, `TreeDecision`, `Look`, `Outcomes.ready`.
- Produces: nothing later tasks use.

- [ ] **Step 1: Write the scenario**

Model it on `EmpireInPlayScenarioSuite` (same imports, same
`accepted(service.handle(...))` chain).

```scala
package oathdigital.gameplay

import oathdigital.application._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{CatalogNames, Dice, Look, MenuPlay, Outcomes,
  Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** A Citizen conquers an Imperial site and leaves it to the bandits
  * (Imperial Infighting, CR p. 35), played through the service. */
class EmpireCampaignScenarioSuite extends munit.FunSuite:
  private val gameId = "empire-campaign"
  private val peaks = CatalogNames.site("Broken Peaks")

  /** p2 is the Chancellor and p3 a Citizen, active in Act at Broken Peaks,
    * which the Empire holds with 2 warbands. p3 has 4 warbands on their
    * board and 7 Supply; p2 has none on their board and stands at Buried
    * Giant with p1. p1, an Exile, holds the Oathkeeper title, so the
    * Chancellor has no title plan. */
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p3, Phase.Act).pawn(p3, at = peaks)
    .pawn(p2, at = "Buried Giant").pawn(p1, at = "Buried Giant")
    .warbandsAt(peaks, p2, 2).boardWarbands(p3, 4).boardWarbands(p2, 0)
    .supply(p3, 7).oathkeeper(Some(p1))

  private def look(at: GameAccepted): Look = Look(Outcomes.ready(at.state))

  private def resolve(service: GameApplicationService, from: GameAccepted,
      player: PlayerId, id: String, answer: DecisionAnswer): GameAccepted =
    accepted(service.handle(gameId, from.nextSequence,
      GameCommand.ResolveWalker(player, TreeDecision(id, answer))))

  test("a Citizen conquers the Empire's site, places no warbands, and the " +
      "bandits take it"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, ActionRef.Campaign)))
    val forced = resolve(service, started, p3, CampaignIds.force,
      ChooseAmountAnswer(4))
    val sacrificed = resolve(service, forced, p3, CampaignIds.sacrifice,
      ChooseAmountAnswer(0))
    // Four swords beat the 2 purple warbands and blank defense dice. Both
    // die; one returns to the Chancellor's board.
    assertEquals(look(sacrificed).boardWarbands(p2), 1)
    assertEquals(look(sacrificed).boardWarbands(p3), 4)
    val placed = resolve(service, sacrificed, p3, CampaignIds.placement,
      ChooseAmountAnswer(0))
    val capacity = FirstGameSetupFixture.catalog.site(peaks).fold(0)(_.capacity)
    assertEquals(look(placed).forces(peaks),
      SiteForces.Occupied(ForceKind.Bandit, capacity))
    assertEquals(look(placed).boardWarbands(p3), 4)
```

The walk asks no kind (only a Conquest is legal: no other pawn is at
Broken Peaks), no targets (no other site is the Empire's), no attacker
plans (p3 holds none) and no defender plans (p2 holds none and not the
title). If the service parks on any of those, the table differs from this
description: read the parked decision from `look(started)` and fix the
table, not the walk. If `Dice.raid`'s defense dice are not blank, use a
`CampaignDicePort` stub from `testkit/Dice.scala` that rolls swords and
blanks. If the bandit refill runs only after the menu returns rather than
after the placement command, assert the refill on the command that ends
the Campaign.

Drop imports the finished test does not use (`ChooseOneAnswer`, `p1` if
unused): `-Werror` rejects them.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly oathdigital.gameplay.EmpireCampaignScenarioSuite"`
Expected: PASS. A failure points at Task 1 (no Conquest, or the wrong
defender) or Task 2 (a refused warband move); fix it there.

- [ ] **Step 3: Update the docs**

- Traceability row 104: in the evidence column, add that the Chancellor
  defends the Empire's sites, a Citizen may conquer them and Imperial
  players raid each other, with every warband of its owner's kind
  (`ImperialInfightingSuite`, `ImperialCampaignBattleSuite`,
  `EmpireCampaignScenarioSuite`). In the remaining column, replace "Empire
  defenders" with "Imperial Tactics (Empire slice 1b-ii)".
- `rules-reference.md`, "Imperial laws": add one line after Warband
  Transfers: "- **Imperial Infighting.** An Imperial player may raid another
  Imperial player, and a Citizen may conquer Imperial sites, which the
  Chancellor defends (CR p. 35)."
- Spec Status line: "Slice 1b-i implemented, 2026-MM-DD." with the date
  you merge, after the 1a sentence.
- Roadmap: replace the "Slice 1b, Imperial Campaign" item with
  "- [ ] **Slice 1b-ii, Imperial Tactics.** Shared battle plans, split costs
  and requirements, and the shared title, each with the contributor's
  consent." (The 1b-i half is done, so it is deleted, not ticked.)

Run: `python3 scripts/check-markdown-links.py`
Expected: "Markdown link check passed".

- [ ] **Step 4: Run every gate**

Run, in order:
- `./sbtw clean coverage test coverageReport`: all tests pass and
  statement coverage is at least 92%.
- `./sbtw frontend/test`: all tests pass. No frontend change is expected:
  the targets question, its tallies and its withheld reasons are rendered
  by the existing generic site options.
- `python3 scripts/check-architecture.py`: passes.
- `python3 scripts/check-markdown-links.py`: passes.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/oathdigital/gameplay/EmpireCampaignScenarioSuite.scala docs/rules/implementation-traceability.md docs/rules/rules-reference.md docs/superpowers/specs/2026-10-29-empire-phase-design.md docs/ROADMAP.md
git commit -m "test(empire): a Citizen conquers an Imperial site; record slice 1b-i"
```
