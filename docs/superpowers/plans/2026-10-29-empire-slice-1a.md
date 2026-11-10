# Empire Slice 1a Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A game with a Chancellor and Citizens, built through the test
`Table`, plays correctly through Wake, Act (every action but the Campaign)
and Rest.

**Architecture:** A model-level turn-aware resolver,
`SiteRule.rulingPlayer`, turns the Empire's sites into one ruling player.
Every gameplay caller that asks "which player rules this site" moves to it,
so cards need no Empire branch. Roles stay on `LineageState.role`; one
`Imperials` object answers role questions. Rest, turn order, the Wake
checks, Supremacy and Vision play read roles through it. Two Imperial laws
that need another player's consent (Chancellor's Eye, Warband Transfers) are
builder-declared askable options, following Palanquin's Toll Roads ask.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules" and "Slice 1a").

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
  Empire"); this slice is reached only through test tables.
- The Campaign action, Military Parade, Siege Engines, the Grand Scepter's
  powers and the Empire endings are out of this slice. Do not change
  `CampaignSetup`, `CampaignBattle`, `CampaignConquest`,
  `CampaignPlanApplication`, `CampaignNotes`, or the round-8 War Exhaustion
  path.
- Unavailable options stay on offer, disabled with a hover reason; do not
  hide them.
- Test helper names state what they build.
- Docs, comments and commit messages are normal prose.

## File Structure

| File | Responsibility |
|---|---|
| `src/main/scala/distantlands/model/Imperials.scala` (new) | Role reads: a player's role, the Chancellor, the Imperial players, the governor of the Empire's sites |
| `src/main/scala/distantlands/model/World.scala` | `Role.hasVisionSlot`; `SiteRule.governed`, `SiteRule.rulingPlayer` |
| `src/main/scala/distantlands/model/GameStateProtocol.scala` | `MaterialBankState.ImperialWarbands` |
| `src/test/scala/distantlands/testkit/Table.scala` | `Table.imperial` step |
| `src/main/scala/distantlands/gameplay/SiteRulers.scala`, `PowerAccess.scala` | Resolver-based ruler reads; `SiteRulers.protectedAt` |
| `src/main/scala/distantlands/gameplay/TurnOrder.scala` (new) | Round order from the Chancellor or the first player |
| `src/main/scala/distantlands/gameplay/SupremacySites.scala` (new) | Each player's Supremacy count |
| `src/main/scala/distantlands/gameplay/actions/ImperialLaws.scala` (new) | Law ids, names and notes for Chancellor's Eye and Warband Transfers |
| `src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala` (new) | The Warband Transfers minor action |
| `FinishRestProcedure.scala`, `BeginRestProcedure.scala` | Imperial Supply and banks |
| `StateBasedEvaluation.scala`, `VisionVictoryEligibility.scala`, `OathkeeperRules.scala` | Guards, Wake, Supremacy |
| `CardSlots.scala`, `CardPlay.scala` | The Vision slot |
| `GamePresentationProjector.scala`, protocol DTOs and codecs, frontend `SupplyAtRest.scala` | Role and per-board Supply bands in the browser |

---

### Task 1: Roles and the ruler resolver

**Files:**
- Create: `src/main/scala/distantlands/model/Imperials.scala`
- Modify: `src/main/scala/distantlands/model/World.scala` (enum `Role`, object `SiteRule`)
- Modify: `src/main/scala/distantlands/model/GameStateProtocol.scala:13-15` (`MaterialBankState`)
- Modify: `src/test/scala/distantlands/testkit/Table.scala` (new step after `warbandsAt`, around line 279)
- Test: `src/test/scala/distantlands/model/SiteRulingPlayerSuite.scala` (new)

**Interfaces:**
- Produces:
  - `Imperials.role(ready: ReadyGame, player: PlayerId): Option[Role]`
  - `Imperials.isImperial(ready: ReadyGame, player: PlayerId): Boolean`
  - `Imperials.chancellor(ready: ReadyGame): Option[PlayerId]`
  - `Imperials.players(ready: ReadyGame): Vector[PlayerId]`
  - `Imperials.governor(ready: ReadyGame): Option[PlayerId]`
  - `Role.hasVisionSlot: Boolean`
  - `SiteRule.governed(ready: ReadyGame, ruler: SiteRuler): SiteRuler`
  - `SiteRule.rulingPlayer(ready: ReadyGame, site: SiteId): Option[SiteRuler]`
  - `MaterialBankState.ImperialWarbands: Int` (24)
  - Test step `Table.imperial(chancellor: PlayerId, citizens: PlayerId*): Table`

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.model

import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class SiteRulingPlayerSuite extends munit.FunSuite:
  private val dunes = "Dunes"

  /** p1 is the Chancellor and p2 a Citizen; the Empire holds the Dunes. */
  private def empireAtDunes: Table =
    Table.start.imperial(p1, p2).warbandsAt(dunes, p1, 2)

  private def rulerOfDunes(table: Table): Option[SiteRuler] =
    val ready = table.ready
    SiteRule.rulingPlayer(ready,
      distantlands.testkit.CatalogNames.site(dunes))

  test("the Empire's sites keep the Empire as their raw ruler"):
    val ready = empireAtDunes.ready
    val site = ready.game.current.map.sites(
      distantlands.testkit.CatalogNames.site(dunes))
    assertEquals(SiteRule.ruler(site.forces, ready.game.current.players),
      Right(SiteRuler.Empire))

  test("on a Citizen's turn the Citizen rules the Empire's sites (R1)"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p2, Phase.Act)),
      Some(SiteRuler.Player(p2)))

  test("on an Exile's turn the Chancellor rules the Empire's sites"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p3, Phase.Act)),
      Some(SiteRuler.Player(p1)))

  test("outside any turn the Chancellor rules the Empire's sites (R2)"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p2, Phase.RoundEnd)),
      Some(SiteRuler.Player(p1)))

  test("an Exile's site and a bandit site resolve unchanged"):
    val table = Table.start.warbandsAt(dunes, p3, 1)
    assertEquals(rulerOfDunes(table), Some(SiteRuler.Player(p3)))
    assertEquals(rulerOfDunes(Table.start.bandits(dunes, 1)),
      Some(SiteRuler.Bandits))

  test("only Exile boards have a Vision slot"):
    assertEquals(Role.values.filter(_.hasVisionSlot).toVector,
      Vector(Role.Exile))

  test("the Table's Imperial step seats the roles and the purple bank"):
    val ready = empireAtDunes.ready
    assertEquals(Imperials.chancellor(ready), Some(p1))
    assertEquals(Imperials.players(ready), Vector(p1, p2))
    assertEquals(ready.banks.warbandSupply.get(ForceKind.Imperial),
      Some(MaterialBankState.ImperialWarbands))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./sbtw "testOnly distantlands.model.SiteRulingPlayerSuite"`
Expected: compile failure: `imperial`, `rulingPlayer`, `Imperials` and
`hasVisionSlot` are not members.

- [ ] **Step 3: Write the implementation**

In `World.scala`, give `Role` its Vision slot:

```scala
enum Role(val isImperial: Boolean):
  case Exile extends Role(false)
  case Citizen extends Role(true)
  case Chancellor extends Role(true)

  /** Only an Exile's board has a Vision slot, so only an Exile can play a
    * true Vision faceup (Empire phase design, "Boards"). */
  def hasVisionSlot: Boolean = !isImperial
```

Create `Imperials.scala`:

```scala
package distantlands.model

/** The roles of the players at the table (Empire phase design, "Role").
  * A role lives on the lineage; every read of who is Imperial, or who is
  * the Chancellor, goes through here. */
object Imperials:
  def role(ready: ReadyGame, player: PlayerId): Option[Role] =
    ready.game.current.players.find(_.player == player)
      .flatMap(state => ready.game.campaign.lineages.get(state.lineage))
      .map(_.role)

  def isImperial(ready: ReadyGame, player: PlayerId): Boolean =
    role(ready, player).exists(_.isImperial)

  def chancellor(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.players.map(_.player)
      .find(role(ready, _).contains(Role.Chancellor))

  /** The Chancellor and the Citizens, in seat order. */
  def players(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.map(_.player).filter(isImperial(ready, _))

  /** The player who rules the Empire's sites now: the turn player during an
    * Imperial player's Wake, Act or Rest (Seal of the Empire, CR p. 35),
    * otherwise the Chancellor (Steward of Imperial Domains; rulings R1 and
    * R2). */
  def governor(ready: ReadyGame): Option[PlayerId] =
    val turn = ready.game.current.turn
    val inTurn = turn.phase == Phase.Wake || turn.phase == Phase.Act ||
      turn.phase == Phase.Rest
    if inTurn && isImperial(ready, turn.activePlayer) then
      Some(turn.activePlayer)
    else chancellor(ready)
```

In `World.scala`, add to `object SiteRule` after `ruledBy`:

```scala
  /** `ruler` as the player who rules for it now (Empire phase design,
    * "Ruler"): the Empire becomes its governor; any other ruler is
    * unchanged. The Empire stays the Empire only with no Chancellor. */
  def governed(ready: ReadyGame, ruler: SiteRuler): SiteRuler = ruler match
    case Empire => Imperials.governor(ready).fold[SiteRuler](Empire)(Player(_))
    case other => other

  /** Who rules `site` as a player, for every rule that asks which player
    * rules a site. `ruler` stays the reading for "does the Empire hold this
    * site". */
  def rulingPlayer(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    ready.game.current.map.sites.get(site)
      .flatMap(state => ruler(state.forces, ready.game.current.players)
        .toOption)
      .map(governed(ready, _))
```

In `GameStateProtocol.scala`, add beside `BanditWarbands`:

```scala
  /** The Chancellor's purple bank, every Imperial player's warband bank
    * (CR p. 45, "Warband Bank"). */
  val ImperialWarbands = 24
```

In `Table.scala`, add after `warbandsAt`:

```scala
  /** `chancellor` and `citizens` play Imperial, with the purple bank of
    * `MaterialBankState.ImperialWarbands` on the table. Their board warbands
    * become purple ones, since a board's kind follows its role. Seat them
    * before placing any of their warbands at a site. */
  def imperial(chancellor: PlayerId, citizens: PlayerId*)(
      using munit.Location): Table =
    val roles = (chancellor -> Role.Chancellor) +:
      citizens.map(_ -> Role.Citizen)
    roles.foldLeft(this) { case (table, (player, role)) =>
      val lineage = table.playerOf(player).lineage
      table.update(_.updateCampaign(campaign => campaign.copy(lineages =
        campaign.lineages.updatedWith(lineage)(_.map(_.copy(role = role))))))
    }.update(ready => ready.copy(banks = ready.banks.copy(warbandSupply =
      ready.banks.warbandSupply.updated(ForceKind.Imperial,
        MaterialBankState.ImperialWarbands))))
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./sbtw "testOnly distantlands.model.SiteRulingPlayerSuite"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/Imperials.scala src/main/scala/distantlands/model/World.scala src/main/scala/distantlands/model/GameStateProtocol.scala src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/model/SiteRulingPlayerSuite.scala
git commit -m "feat(empire): resolve the Empire's sites to their ruling player"
```

---

### Task 2: Every "which player rules" read goes through the resolver

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/SiteRulers.scala` (`rulerOf`, `rulerOfCard` doc, new `protectedAt`)
- Modify: `src/main/scala/distantlands/gameplay/PowerAccess.scala:56-60` (`ruledSites`)
- Modify: `src/main/scala/distantlands/gameplay/powers/targeting/FortressRules.scala:186-191` (Oaken Fortress `shields`, doc at :176-179)
- Modify: `src/main/scala/distantlands/gameplay/powers/targeting/ForgottenVault.scala` (`protectedRuler`, `shields`, doc)
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TravelSitePowers.scala:227-231, 244-247`
- Modify: `src/main/scala/distantlands/gameplay/powers/rest/LeagueTreatyContribution.scala:59-60`
- Modify: `src/main/scala/distantlands/gameplay/actions/ForgeRules.scala:20-22`
- Modify: `src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala:57-96`
- Modify comments only: `powers/RulerToll.scala:9`, `powers/travel/TollRoads.scala:24`, `powers/travel/WayStation.scala:24`, `powers/cardplay/SecretPolice.scala:22-29`, `powers/poweruse/RulerBans.scala:10`, `operations/AncientBloodline.scala:20-23`
- Test: `src/test/scala/distantlands/gameplay/EmpireRulerSuite.scala` (new)

**Interfaces:**
- Consumes: `SiteRule.rulingPlayer`, `SiteRule.governed`, `Imperials.players`, `Table.imperial` (Task 1)
- Produces: `SiteRulers.protectedAt(ready: ReadyGame, site: SiteId): Set[PlayerId]`

Leave these raw `SiteRule.ruler` reads as they are; each asks about the
site's force, not its ruling player: `GreatMarketRules.scala:125`,
`EconomyTree.scala:79`, `CampaignSetup.scala:135` (slice 1b),
`ChronicleTasks.scala:50`, `GamePresentationProjector.scala:124`.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay

import distantlands.model._
import distantlands.testkit.{CatalogNames, Table}
import distantlands.testkit.Table.{p1, p2, p3}

class EmpireRulerSuite extends munit.FunSuite:
  private val dunes = CatalogNames.site("Dunes")
  private val fairIsle = CatalogNames.site("Fair Isle")

  /** p1 is the Chancellor and p2 a Citizen; the Empire holds the Dunes and
    * p3, an Exile, holds Fair Isle. */
  private def empireAndExileSites: Table =
    Table.start.imperial(p1, p2).warbandsAt(dunes, p1, 2)
      .warbandsAt(fairIsle, p3, 1)

  test("a Citizen rules the Empire's sites on their own turn"):
    val ready = empireAndExileSites.turn(p2, Phase.Act).ready
    assertEquals(PowerAccess.ruledSites(ready, p2), Set(dunes))
    assertEquals(PowerAccess.ruledSites(ready, p1), Set.empty[SiteId])

  test("the Chancellor rules the Empire's sites on an Exile's turn"):
    val ready = empireAndExileSites.turn(p3, Phase.Act).ready
    assertEquals(PowerAccess.ruledSites(ready, p1), Set(dunes))
    assertEquals(PowerAccess.ruledSites(ready, p3), Set(fairIsle))

  test("a card at an Empire site has the governor as its ruler"):
    val ready = empireAndExileSites.turn(p2, Phase.Act).ready
    assertEquals(SiteRulers.rulerOf(ready, dunes),
      Some(SiteRuler.Player(p2)))

  test("an Empire site protects every Imperial player"):
    val ready = empireAndExileSites.turn(p3, Phase.Act).ready
    assertEquals(SiteRulers.protectedAt(ready, dunes), Set(p1, p2))
    assertEquals(SiteRulers.protectedAt(ready, fairIsle), Set(p3))
```

Add one card-level test to each existing suite, following that suite's own
setup helpers, and seat roles with `Table.imperial` before placing Imperial
warbands:

- `src/test/scala/distantlands/gameplay/powers/travel/TollRoadsSuite.scala`:
  "an Exile travelling to an Empire site on a Citizen's turn pays the
  Citizen" is impossible (the traveller is the turn player), so test "an
  Exile travelling to an Empire site pays the Chancellor": Toll Roads at an
  Empire site, the Exile p3 travels into another Empire site in its region,
  and the favor goes to p1.
- The Forgotten Vault suite (find it with
  `grep -rl ForgottenVault src/test`): with the Vault at an Empire site, an
  Exile's Raid cannot target a relic held by the Citizen p2, and the hidden
  option's note names p2.
- The Oaken Fortress suite (`grep -rl OakenFortress src/test`): with the
  intact Fortress at an Empire site where the Citizen p2's pawn stands, an
  Exile cannot Challenge p2.
- `src/test/scala/distantlands/application/HallOfMinistersConsentServiceSuite.scala`:
  with the intact Hall at an Empire site, the Citizen p2 discarding a card
  at another Empire site during an Exile's turn is refused and askable of
  the Chancellor p1, not waved through.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.EmpireRulerSuite"`
Expected: compile failure on `protectedAt`; after a stub, the ruled-sites
tests fail with empty sets.

- [ ] **Step 3: Write the implementation**

`SiteRulers.scala`:

```scala
  def rulerOf(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)

  /** The ruler of the site holding `card`, when that is a player or bandits.
    * The Empire resolves to its governor; it stays the Empire only with no
    * Chancellor, which rules nothing.
    */
  def rulerOfCard(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    siteOf(ready, card).flatMap(rulerOf(ready, _)).filter:
      case SiteRuler.Player(_) | SiteRuler.Bandits => true
      case _ => false

  /** The players a protection printed at `site` covers, for a card that
    * says "If ruled by Empire, all Imperials have this power" (Forgotten
    * Vault, Oaken Fortress): every Imperial player while the Empire holds
    * the site, else the player who rules it. */
  def protectedAt(ready: ReadyGame, site: SiteId): Set[PlayerId] =
    ready.game.current.map.sites.get(site).flatMap(state =>
      SiteRule.ruler(state.forces, ready.game.current.players).toOption) match
      case Some(SiteRuler.Empire) => Imperials.players(ready).toSet
      case Some(SiteRuler.Player(player)) => Set(player)
      case _ => Set.empty
```

Update the object's doc comment: it now covers the Empire through
`SiteRule.rulingPlayer`.

`PowerAccess.ruledSites`:

```scala
  def ruledSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      SiteRule.rulingPlayer(ready, site)
        .contains(SiteRuler.Player(actor))).toSet
```

Oaken Fortress `shields`:

```scala
  protected def shields(ready: ReadyGame, attacker: PlayerId,
      defender: PlayerId): Boolean = defender != attacker &&
    sites(ready).exists(site => pawnSite(ready, defender).contains(site) &&
      SiteRulers.protectedAt(ready, site).contains(defender))
```

and replace "Empire rulers are not supported." in its doc with "While the
Empire rules its site, it shields every Imperial player there."

Forgotten Vault: replace `protectedRuler` and `shields`, and route the note
through the shielded owner:

```scala
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.CampaignTargetSelection -> Vector(OptionRestriction(guard,
      (ctx, ref) => shieldedOwner(ctx, ref).map(said))))

  private def guard(ctx: PowerCtx, ref: DecisionOptionRef)
      : Option[OathViolation] = Option.when(shieldedOwner(ctx, ref).nonEmpty)(
    OathViolation.InvalidEventOrder(
      "the Forgotten Vault protects its ruler's relics"))

  /** The players the Vault protects from the acting player: its site's
    * ruler, or every Imperial player while the Empire rules it, when they
    * are enemies of the actor. */
  private def owners(ctx: PowerCtx): Set[PlayerId] =
    SiteRulers.siteOf(ctx.state, cardId).fold(Set.empty[PlayerId])(site =>
      SiteRulers.protectedAt(ctx.state, site)).filter(owner =>
      SiteRule.enemies(SiteRuler.Player(owner), SiteRuler.Player(ctx.actor)))

  /** The protected player holding the relic `ref` names. */
  private def shieldedOwner(ctx: PowerCtx, ref: DecisionOptionRef)
      : Option[PlayerId] = owners(ctx).find(owner => ref match
    case DecisionOptionRef.Relic(relic) =>
      ctx.state.game.current.players.exists(p =>
        p.player == owner && p.relics.exists(_.id == relic))
    case DecisionOptionRef.RelicSlot(slotOwner, _) => slotOwner == owner
    case _ => false)
```

Replace "Empire rulers are not supported." in its doc with "If the Empire
rules it, it protects every Imperial player's relics."

`TravelSitePowers.scala` (Narrow Pass), both reads:

```scala
      val ruledByActor = SiteRule.rulingPlayer(ctx.state, site)
        .contains(SiteRuler.Player(ctx.actor))
```

```scala
      else SiteRule.rulingPlayer(ctx.state, site) match
        case Some(SiteRuler.Player(player)) if player == route.player => None
        case _ => Some(OathViolation.TravelPassBlocked(site, route.destination))
```

`LeagueTreatyContribution.scala`:

```scala
      ruler <- SiteRule.rulingPlayer(ready, site)
        .collect { case SiteRuler.Player(player) => player }
```

(the enclosing method's `ReadyGame` parameter is `ready`).

`ForgeRules.scala`:

```scala
      raw <- SiteRule.ruler(s.forces, game.current.players)
        .left.map(x => ForgeUnavailable(s"cannot derive site ruler: $x"))
      _ <- Either.cond(SiteRule.governed(ready, raw) ==
        SiteRuler.Player(player.player), (),
        ForgeUnavailable("actor does not rule their pawn site"))
```

`HallOfMinisters.scala`: an Imperial actor is no longer "the Empire's
side"; each player is their own side, and the Hall's site resolves to its
governor (ruling R1). Replace `actorSide` and `siteRuler`:

```scala
    discardAt(ready, operation).flatMap { case (site, actor, card) =>
      for
        ruler <- siteRuler(ready, site)
        if SiteRule.enemies(SiteRuler.Player(actor), ruler)
        if hallRuler(ready).contains(ruler)
      yield OperationReason("discard-immune",
        s"cards at site ${site.value} cannot be discarded by ${actor.value}",
        OperationReasonKind.Impossible, Some(OperationWaiver(id,
          granter(ready), ConsentKey(id, actor, discardKey, card),
          noDiscardConsent(source), noDiscard(source))))
    }
```

```scala
  private def siteRuler(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)
```

Update the doc of `granter`: `None` when bandits or nobody rules it.

Comment-only edits: delete "Empire ruler not supported" or "Empire rulers
are not supported" from the listed files, and in `AncientBloodline.scala`
replace the paragraph starting "An Imperial holder rules through the
Empire" with "An Imperial holder rules the Empire's sites on their own
turn, through `PowerAccess.ruledSites`."

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.EmpireRulerSuite distantlands.gameplay.powers.* distantlands.application.HallOfMinistersConsentServiceSuite"`
Expected: PASS, including every existing power suite.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/SiteRulers.scala src/main/scala/distantlands/gameplay/PowerAccess.scala src/main/scala/distantlands/gameplay/powers src/main/scala/distantlands/gameplay/actions/ForgeRules.scala src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala src/main/scala/distantlands/gameplay/operations/AncientBloodline.scala src/test/scala/distantlands/gameplay/EmpireRulerSuite.scala
git add <each card suite you extended, by name>
git commit -m "feat(empire): read every site ruler through the turn-aware resolver"
```

---

### Task 3: Lift the all-Exile guards; turn order and Wake

**Files:**
- Create: `src/main/scala/distantlands/gameplay/TurnOrder.scala`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala` (`supported` :166-171, `atWake` :39-49, `visionAtWake` :51-59, `endRound` :63-66, `RoundEnded` arm :139-141, `expectedWarExhaustion` :185, doc :13-20)
- Modify: `src/main/scala/distantlands/gameplay/phases/rest/FinishRestProcedure.scala:142-145` (`turnOrder`)
- Modify: `src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala:7-25`
- Modify: `src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala:265-282` (`started`, `supported`)
- Modify: `src/main/scala/distantlands/model/DomainValidation.scala` (new `ImperialUsurper` problem)
- Test: `src/test/scala/distantlands/model/DomainValidationSuite.scala`, `src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala`, `src/test/scala/distantlands/gameplay/EmpireTurnSuite.scala` (new)

**Interfaces:**
- Consumes: `Imperials.chancellor`, `Imperials.isImperial`, `Table.imperial`
- Produces: `TurnOrder.roundStart(ready: ReadyGame): PlayerId`, `TurnOrder.order(ready: ReadyGame): Vector[PlayerId]`

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay

import distantlands.gameplay.phases.rest.BeginRestProcedure
import distantlands.model._
import distantlands.model.OathEvent._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class EmpireTurnSuite extends munit.FunSuite:
  /** p2 is the Chancellor and p3 a Citizen; p1, the first player, is an
    * Exile. */
  private def chancellorSecondInSeat: Table = Table.start.imperial(p2, p3)

  test("a round starts with the Chancellor"):
    val ready = chancellorSecondInSeat.ready
    assertEquals(TurnOrder.roundStart(ready), p2)
    assertEquals(TurnOrder.order(ready), Vector(p2, p3, p1))

  test("with no Chancellor a round starts with the first player"):
    assertEquals(TurnOrder.order(Table.start.ready), Vector(p1, p2, p3))

  test("an Imperial Oathkeeper does not flip to Usurper"):
    val state = chancellorSecondInSeat.turn(p3, Phase.Wake)
      .oathkeeper(Some(p3)).state
    assertEquals(StateBasedEvaluation.atWake(state), Right(None))

  test("an Exile Oathkeeper still flips with an Empire at the table"):
    val state = chancellorSecondInSeat.turn(p1, Phase.Wake)
      .oathkeeper(Some(p1)).state
    assertEquals(StateBasedEvaluation.atWake(state),
      Right(Some(UsurperFlipped(p1))))

  test("Rest accepts an Imperial player"):
    val ready = chancellorSecondInSeat.turn(p2, Phase.Act).ready
    assertEquals(BeginRestProcedure.validateSupportedState(
      distantlands.gameplay.setup.FirstGameSetupFixture.catalog, ready),
      Right(()))
```

`Table.start` sets `usurperLimited`; if `atWake` for p1 returns `None`
because of it, add `.update(r => r.updateCurrent(c => c.copy(tracks =
c.tracks.copy(usurperLimited = false))))` with a comment saying the
Usurper Limiter is off in an Empire game (ruling R5).

In `src/test/scala/distantlands/model/DomainValidationSuite.scala`:

```scala
  test("an Imperial player never holds the Usurper side of the title"):
    // Built with Table steps, then validated: the state breaks a rule
    // on purpose. Becoming a Citizen flips the title back to its
    // Oathkeeper side (Empire slice 3), so no rule can reach it.
    val usurperCitizen = distantlands.testkit.Table.start
      .imperial(distantlands.testkit.Table.p1, distantlands.testkit.Table.p2)
      .oathkeeper(Some(distantlands.testkit.Table.p2), TitleSide.Usurper)
      .ready.game
    assertEquals(DomainValidation.validate(usurperCitizen)
      .collect { case problem: DomainProblem.ImperialUsurper => problem },
      Vector(DomainProblem.ImperialUsurper(distantlands.testkit.Table.p2)))

  test("an Imperial player may hold the Oathkeeper side"):
    val oathkeeperCitizen = distantlands.testkit.Table.start
      .imperial(distantlands.testkit.Table.p1, distantlands.testkit.Table.p2)
      .oathkeeper(Some(distantlands.testkit.Table.p2))
      .ready.game
    assert(!DomainValidation.validate(oathkeeperCitizen)
      .exists(_.isInstanceOf[DomainProblem.ImperialUsurper]))
```

In `StateBasedEvaluationSuite`, find the test that expects
`UnsupportedWakeVictoryState` for a non-Exile role (grep the suite for
`all-Exile` and `UnsupportedWakeVictoryState`) and delete it: Task 3
removes that refusal. Do the same in `MinorActionsSuite` for
"minor actions are limited to fixed unaltered all-Exile" when the test
sets an Imperial role, keeping any test that changes the foundation
profile.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.EmpireTurnSuite"`
Expected: compile failure on `TurnOrder`.

- [ ] **Step 3: Write the implementation**

`TurnOrder.scala`:

```scala
package distantlands.gameplay

import distantlands.model._

/** The order of a round (CR p. 17): from the Chancellor, or from the first
  * player when there is none. */
object TurnOrder:
  def roundStart(ready: ReadyGame): PlayerId =
    Imperials.chancellor(ready).getOrElse(ready.setup.firstPlayer)

  def order(ready: ReadyGame): Vector[PlayerId] =
    val players = ready.game.current.players.map(_.player)
    val index = players.indexOf(roundStart(ready))
    players.drop(index) ++ players.take(index)
```

`FinishRestProcedure.turnOrder` delegates:

```scala
  def turnOrder(ready: ReadyGame): Vector[PlayerId] = TurnOrder.order(ready)
```

`StateBasedEvaluation`:

```scala
  def atWake(state: OathState): Either[OathViolation, Option[OathEvent]] =
    supported(state).map { ready =>
      val current = ready.game.current
      val active = current.turn.activePlayer
      if Imperials.isImperial(ready, active) then None
      else current.title match
        case OathkeeperState(Some(player), TitleSide.Usurper)
            if player == active => Some(UsurperVictory(player))
        case OathkeeperState(Some(player), TitleSide.Oathkeeper)
            if player == active && !current.tracks.usurperLimited =>
          Some(UsurperFlipped(player))
        case _ => None
    }

  def visionAtWake(state: OathState): Either[OathViolation, Option[OathEvent]] =
    supported(state).map { ready =>
      val current = ready.game.current
      current.players.find(_.player == current.turn.activePlayer)
        .filterNot(actor => Imperials.isImperial(ready, actor.player))
        .flatMap { actor =>
          actor.revealedVision.flatMap(v => Option.when(
            VisionVictoryEligibility.qualifies(actor.player, ready))(
            VisionVictory(actor.player, v.id)))
        }
    }
```

(`VisionVictoryEligibility.qualifies` takes `ReadyGame` from Task 4; until
Task 4, keep passing `current` here and switch the argument in Task 4.)

In `endRound` replace lines 64-66 with
`val turnOrder = TurnOrder.order(ready)`; in the `RoundEnded` arm replace
lines 139-141 with `val first = TurnOrder.roundStart(ready)`; in
`expectedWarExhaustion` pass `TurnOrder.roundStart(ready)` to
`endRoundWinner`.

`supported`:

```scala
  private[gameplay] def supported(state: OathState)
      : Either[OathViolation, ReadyGame] = state match
    case Ready(ready) => Right(ready)
    case _ => Left(GameNotStarted)
```

Rewrite the object doc's first line to "State-based checks: bandit refill,
the Wake victories, the end of a round and War Exhaustion." and add "The
Empire's endings (Stable Regime, the full War Exhaustion) are slice 2's."
If `UnsupportedWakeVictoryState` has no producer left, leave the case in
`OathViolation`; its codec still names it.

`BeginRestProcedure`:

```scala
/** Begin Rest: the supported-state gate and the phase change, nothing else.
  *
  * Cleanup waits for Finish Rest so REST powers can be used in between.
  */
object BeginRestProcedure:
  def validateSupportedState(catalog: ExecutableCatalog,
      ready: ReadyGame): Either[OathViolation, Unit] =
    val missing = ready.game.current.players
      .flatMap(PlayerForceKind.of(ready, _)).distinct
      .filterNot(ready.banks.warbandSupply.contains)
    if missing.nonEmpty then Left(UnsupportedRestState(
      s"no bounded warband supply for ${missing.mkString(", ")}"))
    else Right(())
```

`DomainValidation.scala`: add the problem beside `MultipleChancellors`,

```scala
  /** The Usurper side is an Exile's only (CR p. 16): a player who becomes
    * a Citizen flips it back to Oathkeeper at once (CR p. 34). */
  final case class ImperialUsurper(player: PlayerId) extends DomainProblem
```

and check it after the Chancellor count:

```scala
    game.current.title match
      case OathkeeperState(Some(holder), TitleSide.Usurper) =>
        game.current.players.find(_.player == holder)
          .flatMap(player => game.campaign.lineages.get(player.lineage))
          .filter(_.role.isImperial)
          .foreach(_ => problems += ImperialUsurper(holder))
      case _ => ()
```

`MinorActionProcedures`: the gate message becomes "minor actions are
limited to the fixed unaltered foundations", and `supported` drops the
role clause:

```scala
  private def supported(ready: ReadyGame): Boolean =
    ready.setup.foundationProfile ==
      FirstGameFoundationProfile.FixedUnaltered &&
      ready.game.campaign.foundations.values.forall(foundation =>
        foundation.face == FoundationFace.Normal &&
          foundation.alterationSources.isEmpty)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.model.DomainValidationSuite distantlands.gameplay.EmpireTurnSuite distantlands.gameplay.StateBasedEvaluationSuite distantlands.gameplay.MinorActionsSuite distantlands.gameplay.powers.whenplayed.*"`
Expected: PASS (Dissent and Revelation read `turnOrder`).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/TurnOrder.scala src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala src/main/scala/distantlands/gameplay/phases/rest/FinishRestProcedure.scala src/main/scala/distantlands/gameplay/phases/rest/BeginRestProcedure.scala src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala src/test/scala/distantlands/gameplay/EmpireTurnSuite.scala src/test/scala/distantlands/gameplay/StateBasedEvaluationSuite.scala src/test/scala/distantlands/gameplay/MinorActionsSuite.scala src/main/scala/distantlands/model/DomainValidation.scala src/test/scala/distantlands/model/DomainValidationSuite.scala
git commit -m "feat(empire): start rounds with the Chancellor and keep Wake wins for Exiles"
```

---

### Task 4: Supremacy with the Empire, and the Vision slot

**Files:**
- Create: `src/main/scala/distantlands/gameplay/SupremacySites.scala`
- Modify: `src/main/scala/distantlands/gameplay/oathkeeper/OathkeeperRules.scala:30-45`
- Modify: `src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala` (three `qualifies` calls; `endRoundWinner` takes `ready`)
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala:85-101, 303-314`
- Modify: `src/main/scala/distantlands/gameplay/actions/CardPlay.scala:142-145`
- Test: `src/test/scala/distantlands/gameplay/oathkeeper/OathkeeperRulesSuite.scala`, `src/test/scala/distantlands/gameplay/CardSlotsSuite.scala`

**Interfaces:**
- Consumes: `Imperials.role`, `Role.hasVisionSlot`, `PlayerForceKind.of`
- Produces: `SupremacySites.counts(ready: ReadyGame): Vector[(PlayerId, Int)]`; `VisionVictoryEligibility.qualifies(playerId: PlayerId, ready: ReadyGame): Boolean`

- [ ] **Step 1: Write the failing tests**

In `OathkeeperRulesSuite`:

```scala
  /** p1 is the Chancellor and p2 a Citizen, with the Empire holding
    * `empire` sites, and p3, an Exile, holding `exile` sites. Supremacy is
    * the goal. */
  private def empireAgainstExile(empire: Int, exile: Int,
      holder: Option[PlayerId] = None): OathkeeperOutcome =
    val sites = Vector("Dunes", "Fair Isle", "Deep Woods", "Mountains")
    val withEmpire = sites.take(empire).foldLeft(
      Table.start.imperial(p1, p2).oathkeeper(holder))((t, site) =>
        t.warbandsAt(site, p1, 1))
    val table = sites.drop(empire).take(exile).foldLeft(withEmpire)(
      (t, site) => t.warbandsAt(site, p3, 1))
    OathkeeperRules.outcome(table.ready)

  test("the Chancellor holds Supremacy when the Empire has the most sites"):
    assertEquals(empireAgainstExile(2, 1), Transfer(Some(p1)))

  test("a Citizen holding Supremacy loses it to the Chancellor"):
    assertEquals(empireAgainstExile(2, 1, holder = Some(p2)),
      Transfer(Some(p1)))

  test("an Exile with more sites than the Empire takes Supremacy"):
    assertEquals(empireAgainstExile(1, 2), Transfer(Some(p3)))
```

Check the site names against the catalog with `CatalogNames.site`; replace
any that do not exist with in-play sites from `Table.start` that hold no
pawn.

In `CardSlotsSuite`, beside "a faceup Conspiracy is Play, and takes no
slot" (the suite's `atAncientCity`, `slotsOf`, `options`, `pile`, `spot`,
`play`, `refused` and `adviserPlay` helpers):

```scala
  test("an Imperial player sees the Vision spot disabled with its reason"):
    val faith = VisionRules.Faith
    // p2 is the Chancellor; the drawing player p1 is a Citizen.
    val ready = atAncientCity.imperial(p2, p1).hand(p1, faith).ready
    assertEquals(options(slotsOf(ready, faith)), Vector(pile,
      refused(spot, CardSlots.noVisionSlotReason),
      adviserPlay(0, faceup = false)))
    assert(CardPlay.operations(catalog, ready, p1, faith,
      SearchPlacement.Adviser(Orientation.FaceUp, None),
      CardPlay.Origin.TemporaryHand).isLeft)

  test("an Imperial player may still play the Conspiracy"):
    val conspiracy = VisionRules.Conspiracy
    val ready = atAncientCity.imperial(p2, p1).hand(p1, conspiracy).ready
    assertEquals(options(slotsOf(ready, conspiracy)),
      Vector(pile, play, adviserPlay(0, faceup = false)))
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.oathkeeper.OathkeeperRulesSuite distantlands.gameplay.CardSlotsSuite"`
Expected: the Supremacy tests fail (the Empire's sites count for no one),
the Vision test fails (the slot is available).

- [ ] **Step 3: Write the implementation**

`SupremacySites.scala`:

```scala
package distantlands.gameplay

import distantlands.model._

/** Each player's count toward Supremacy, for the Oathkeeper title and the
  * Vision of Conquest (Steward of Imperial Domains, CR p. 35): an Exile's
  * own sites, the Empire's sites for the Chancellor, and none for a
  * Citizen, who never holds the Supremacy title. */
object SupremacySites:
  def counts(ready: ReadyGame): Vector[(PlayerId, Int)] =
    val current = ready.game.current
    def sitesOf(kind: ForceKind): Int =
      current.map.sites.values.count(_.forces match
        case SiteForces.Occupied(`kind`, _) => true
        case _ => false)
    current.players.map { player =>
      player.player -> (Imperials.role(ready, player.player) match
        case Some(Role.Citizen) => 0
        case _ => PlayerForceKind.of(ready, player).fold(0)(sitesOf))
    }
```

`OathkeeperRules.qualifyingPlayers` takes `ready` and uses it:

```scala
  def outcome(ready: ReadyGame): OathkeeperOutcome =
    val current = ready.game.current
    val leaders = qualifyingPlayers(ready)
    // ... unchanged match ...

  private def qualifyingPlayers(ready: ReadyGame): Set[PlayerId] =
    val current = ready.game.current
    ready.game.campaign.oathkeeperGoal match
      case OathkeeperGoal.Supremacy =>
        leadersWithPositiveCount(SupremacySites.counts(ready))
      case OathkeeperGoal.Protection =>
        leadersWithPositiveCount(current.players.map(player =>
          player.player -> player.relics.size))
      case OathkeeperGoal.ThePeople =>
        current.banners.peoplesFavor.holder.toSet
      case OathkeeperGoal.Devotion =>
        current.banners.darkestSecret.holder.toSet
```

`VisionVictoryEligibility.qualifies`:

```scala
  def qualifies(playerId: PlayerId, ready: ReadyGame): Boolean =
    val current = ready.game.current
    current.tracks.visionsDrawn >= 3 && current.players.find(
      _.player == playerId).flatMap(_.revealedVision).exists { revealed =>
      VisionRules.trueGoal(revealed.id).exists:
        case OathkeeperGoal.Supremacy =>
          uniquePositiveLeader(playerId, SupremacySites.counts(ready))
        case OathkeeperGoal.Protection => uniquePositiveLeader(playerId,
          current.players.map(player => player.player -> player.relics.size))
        case OathkeeperGoal.ThePeople =>
          current.banners.peoplesFavor.holder.contains(playerId)
        case OathkeeperGoal.Devotion =>
          current.banners.darkestSecret.holder.contains(playerId)
    }
```

In `StateBasedEvaluation`, pass `ready` to the three `qualifies` calls;
change `endRoundWinner(current, first)` to
`endRoundWinner(ready, first)` and read `val current = ready.game.current`
inside it.

`CardSlots`: in `faceup`, take `ready` and leave the true-Vision spot out
when the player has no slot; then add the disabled spot in `slots` outside
`judged`, since `judged` drops what the planner refuses:

```scala
        limited(judged(discardPile(ready, actor).toVector) ++
          playSites(ready, player, rules).flatMap(site =>
            siteSlots(catalog, ready, player, card, origin, site, rules,
              picked)) ++
          judged(faceup(ready, player, card, origin, rules)) ++
          noVisionSlot(ready, player, card) ++
          judged(facedown(player, card, origin, rules)), rules)
```

```scala
  /** Why an Imperial player cannot play a true Vision faceup. */
  val noVisionSlotReason: String = "An Imperial board has no Vision slot"

  private def hasVisionSlot(ready: ReadyGame, player: PlayerState): Boolean =
    Imperials.role(ready, player.player).forall(_.hasVisionSlot)

  /** The revealed Vision spot, disabled, for a player whose board has no
    * Vision slot (Empire phase design, "Boards"). */
  private def noVisionSlot(ready: ReadyGame, player: PlayerState,
      card: WorldCardId): Vector[CardSlot] = card match
    case VisionRules.Conspiracy => Vector.empty
    case _: VisionId if !hasVisionSlot(ready, player) =>
      Vector(CardSlot(DecisionOption.Unavailable(
        DecisionOption.RevealedVision(
          DecisionOptionRef.RevealedVision(player.player)),
        OptionReason.Written(noVisionSlotReason)),
        SearchPlacement.Adviser(Orientation.FaceUp, None)))
    case _ => Vector.empty

  private def faceup(ready: ReadyGame, player: PlayerState,
      card: WorldCardId, origin: CardPlay.Origin, rules: CardPlayRules)
      : Vector[(DecisionOption, SearchPlacement)] =
    val placement = SearchPlacement.Adviser(Orientation.FaceUp, None)
    card match
      case VisionRules.Conspiracy =>
        Vector(DecisionOption.Button(play, "Play") -> placement)
      case _: VisionId if !hasVisionSlot(ready, player) => Vector.empty
      case _: VisionId =>
        val spot = DecisionOption.RevealedVision(
          DecisionOptionRef.RevealedVision(player.player))
        Vector(player.revealedVision.fold[DecisionOption](spot)(held =>
          DecisionOption.Replacing(spot, held.id)) -> placement)
      case _ => adviserSlots(player, card, origin, Orientation.FaceUp, rules)
```

`CardPlay.scala` refuses the play authoritatively, before `planVision`:

```scala
      case id: VisionId =>
        if !FirstGameRulesData.visions.contains(id) then Left(UnknownWorldCard(id))
        else if orientation == Orientation.FaceUp &&
            id != VisionRules.Conspiracy &&
            !Imperials.role(ready, player.player).forall(_.hasVisionSlot) then
          Left(InvalidSearchPlacement("an Imperial board has no Vision slot"))
        else planVision(player, origin, id, orientation, replace)
```

Use the name the enclosing method gives its `ReadyGame` parameter.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.oathkeeper.* distantlands.gameplay.CardSlotsSuite distantlands.gameplay.StateBasedEvaluationSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/SupremacySites.scala src/main/scala/distantlands/gameplay/oathkeeper/OathkeeperRules.scala src/main/scala/distantlands/gameplay/VisionVictoryEligibility.scala src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala src/main/scala/distantlands/gameplay/actions/CardPlay.scala src/test/scala/distantlands/gameplay/oathkeeper/OathkeeperRulesSuite.scala src/test/scala/distantlands/gameplay/CardSlotsSuite.scala
git commit -m "feat(empire): count the Empire for Supremacy and keep Visions off Imperial boards"
```

---

### Task 5: Imperial Supply at Rest

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/phases/rest/FinishRestProcedure.scala:26-29, 74-121`
- Modify: `src/main/scala/distantlands/model/Resources.scala:42-45` (doc of `SupplyRules`)
- Test: `src/test/scala/distantlands/gameplay/phases/rest/ImperialSupplySuite.scala` (new)

**Interfaces:**
- Consumes: `Imperials.role`, `Imperials.chancellor`, `PlayerForceKind.of`
- Produces:
  - `FinishRestProcedure.ChancellorSupply: SupplyRules`
  - `FinishRestProcedure.supplyRules(ready: ReadyGame, player: PlayerId): Option[SupplyRules]` (`None` for a Citizen)
  - `supplyAfterRest` and `supplyGainAtRest` unchanged in signature, now role-aware

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.phases.rest

import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class ImperialSupplySuite extends munit.FunSuite:
  /** p1 is the Chancellor and p2 a Citizen. */
  private def empire: Table = Table.start.imperial(p1, p2)

  test("the Chancellor's bands read the purple bank"):
    // 24 purple, 6 on the two Imperial boards: 18 banked gives 6.
    val ready = empire.supply(p1, 0).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p1), Right(6))
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p1), Right(6))

  test("each Chancellor band"):
    val gains = Vector(18 -> 6, 17 -> 5, 11 -> 5, 10 -> 4, 4 -> 4, 3 -> 3,
      0 -> 3).map((banked, _) => banked -> FinishRestProcedure
        .ChancellorSupply.baseSupplyFor(banked))
    assertEquals(gains, Vector(18 -> Some(6), 17 -> Some(5), 11 -> Some(5),
      10 -> Some(4), 4 -> Some(4), 3 -> Some(3), 0 -> Some(3)))

  test("a Citizen matches the Chancellor's Supply and keeps their own surplus (R3)"):
    val ready = empire.supply(p1, 5).supply(p2, 1).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p2), Right(5))
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p2), Right(6))

  test("a Citizen's refresh is capped at 7"):
    val ready = empire.supply(p1, 6).supply(p2, 3).ready
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p2), Right(7))

  test("an Exile at an Imperial table keeps the Exile bands"):
    val ready = empire.supply(p3, 0).warbandsInBank(p3, 9).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p3), Right(6))

  test("a Citizen has no band table of their own"):
    assertEquals(FinishRestProcedure.supplyRules(empire.ready, p2), None)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.phases.rest.ImperialSupplySuite"`
Expected: compile failure on `ChancellorSupply` and `supplyRules`.

- [ ] **Step 3: Write the implementation**

Beside `ExileSupply`:

```scala
  /** The Chancellor's board: bands by warbands in the purple bank. */
  val ChancellorSupply: SupplyRules = SupplyRules(SupplyTrack.Maximum, Vector(
    SupplyRefreshBand(InclusiveIntRange(18, Int.MaxValue), 6),
    SupplyRefreshBand(InclusiveIntRange(11, 17), 5),
    SupplyRefreshBand(InclusiveIntRange(4, 10), 4),
    SupplyRefreshBand(InclusiveIntRange(0, 3), 3)))

  /** The band table of `player`'s board: the Chancellor's or an Exile's. A
    * Citizen's board has none: it copies the Chancellor's Supply. */
  def supplyRules(ready: ReadyGame, player: PlayerId): Option[SupplyRules] =
    Imperials.role(ready, player) match
      case Some(Role.Citizen) => None
      case Some(Role.Chancellor) => Some(ChancellorSupply)
      case _ => Some(ExileSupply)
```

Replace `bankedWarbands`, `supplyAfterRest` and `supplyGainAtRest`:

```scala
  private def bankedWarbands(ready: ReadyGame, player: PlayerState)
      : Either[OathViolation, Int] = for
    kind <- PlayerForceKind.of(ready, player).toRight(UnsupportedRestState(
      s"no warband kind for ${player.player}"))
    banked <- Either.cond(ready.banks.warbandSupply.contains(kind),
      PlayerFacts.banked(ready, kind),
      UnsupportedRestState(s"no bounded warband supply for $kind"))
  yield banked

  /** What Rest adds to the unspent Supply: the board's band for an Exile or
    * the Chancellor, the Chancellor's current Supply for a Citizen (ruling
    * R3). */
  private def baseSupply(ready: ReadyGame, player: PlayerState)
      : Either[OathViolation, Int] = supplyRules(ready, player.player) match
    case Some(rules) => for
      banked <- bankedWarbands(ready, player)
      base <- rules.baseSupplyFor(banked).toRight(UnsupportedRestState(
        s"no Supply band for $banked banked warbands"))
    yield base
    case None => Imperials.chancellor(ready)
      .flatMap(chancellor => ready.game.current.players
        .find(_.player == chancellor))
      .map(_.board.supply.supply)
      .toRight(UnsupportedRestState("a Citizen's Supply needs a Chancellor"))

  def supplyAfterRest(ready: ReadyGame, resting: PlayerId)
      : Either[OathViolation, Int] = for
    player <- restingPlayer(ready, resting)
    base <- baseSupply(ready, player)
  yield math.min(SupplyTrack.Maximum, base + player.board.supply.supply)

  def supplyGainAtRest(ready: ReadyGame, resting: PlayerId)
      : Either[OathViolation, Int] =
    restingPlayer(ready, resting).flatMap(baseSupply(ready, _))
```

Keep the existing doc comments on `supplyAfterRest` and
`supplyGainAtRest`, adding "A Citizen's base is the Chancellor's Supply."
to the second. In `Resources.scala`, change "Citizens bypass this table and
copy the Chancellor's result." to "Citizens have no table: they match the
Chancellor's Supply and add their own unspent Supply
(`FinishRestProcedure.supplyRules`)."

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.phases.rest.* distantlands.gameplay.PhaseMenuSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/phases/rest/FinishRestProcedure.scala src/main/scala/distantlands/model/Resources.scala src/test/scala/distantlands/gameplay/phases/rest/ImperialSupplySuite.scala
git commit -m "feat(empire): refresh the Chancellor's and Citizens' Supply at Rest"
```

---

### Task 6: Move Warbands for Imperials, with Chancellor's Eye

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/ImperialLaws.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/RuleNotes.scala:17-21` (`all`)
- Modify: `src/main/scala/distantlands/application/ConsentProjector.scala:21` (`ConsentNames.power`)
- Modify: `src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala:190-240` (`way`, `most`, `move`)
- Modify: `src/test/resources/notes/declared-keys.txt` (golden note keys)
- Test: `src/test/scala/distantlands/gameplay/MinorActionsSuite.scala`, `src/test/scala/distantlands/application/ChancellorsEyeConsentServiceSuite.scala` (new)

**Interfaces:**
- Consumes: `Imperials`, `SiteRule.rulingPlayer`, `PlayerForceKind.of`, `Table.imperial`
- Produces:
  - `ImperialLaws.chancellorsEye: PowerId`, `ImperialLaws.warbandTransfers: PowerId`
  - `ImperialLaws.names: Map[PowerId, String]`
  - `ImperialLaws.needsChancellor: NoteKey`, `ImperialLaws.needsPartner: NoteKey` (one `Arg(0)`: the partner), `ImperialLaws.refused: NoteKey` (one `Arg(0)`: the granter)
  - `MinorActionProcedures.eyeKey(actor: PlayerId): ConsentKey`

- [ ] **Step 1: Write the failing tests**

In `MinorActionsSuite`, beside the existing Move Warbands tests (reuse its
`rules`, `accepted` and answer helpers):

```scala
  /** p1 is the Chancellor and p2 a Citizen on their own turn at Broken
    * Peaks, which the Empire holds with 3 warbands; p2's board holds 4. */
  private def citizenAtImperialSite: Table = Table.start.imperial(p1, p2)
    .turn(p2, Phase.Act).pawn(p2, at = brokenPeaks).boardWarbands(p2, 4)
    .warbandsAt(brokenPeaks, p2, 3)

  test("a Citizen moves purple warbands to the Empire site they rule"):
    val asked = accepted(rules.startWalker(citizenAtImperialSite.state,
      ActionRef.MoveWarbands, p2))
    val towardSite = accepted(rules.resolveWalker(asked.state, p2,
      MinorActionProcedures.directionDecisionId, ChooseOneAnswer(toSite)))
    val atSite = accepted(rules.resolveWalker(towardSite.state, p2,
      MinorActionProcedures.amountDecisionId, ChooseAmountAnswer(2)))
    val ready = atSite.state match
      case Ready(value) => value
      case other => fail(s"not ready: $other")
    assertEquals(ready.game.current.map.sites(brokenPeaks).forces,
      SiteForces.Occupied(ForceKind.Imperial, 5))

  test("a Citizen asks the Chancellor before moving warbands to their board"):
    val asked = accepted(rules.startWalker(citizenAtImperialSite.state,
      ActionRef.MoveWarbands, p2))
    assert(boardWay(asked.state).exists(_.askable), s"${boardWay(asked.state)}")

  test("the Chancellor moves warbands to their board without asking"):
    val chancellorAtImperialSite = Table.start.imperial(p1, p2)
      .turn(p1, Phase.Act).pawn(p1, at = brokenPeaks).boardWarbands(p1, 4)
      .warbandsAt(brokenPeaks, p1, 3)
    val asked = accepted(rules.startWalker(chancellorAtImperialSite.state,
      ActionRef.MoveWarbands, p1))
    assertEquals(boardWay(asked.state),
      Some(wayButton(toBoard, "To your board")))

  /** The "To your board" option of the parked direction decision. */
  private def boardWay(state: OathState): Option[DecisionOption] =
    parkedDecisionQuery(state) match
      case choose: DecisionQuery.ChooseOne => choose.options.find(_.ref == toBoard)
      case other => fail(s"expected a choice, got $other")
```

`parkedDecisionQuery` and `wayButton` are the suite's own helpers. If
`ChooseOne` names its options field differently, use that name.

In `ChancellorsEyeConsentServiceSuite`, model the request and answer on
`HallOfMinistersConsentServiceSuite.scala:43-67`: p2 requests consent on
`MinorActionProcedures.directionDecisionId` with target
`MinorActionProcedures.toBoard`, p1 answers `ConsentChoice.Yes`, and the
parked decision moves on to the amount decision. A second test: p1 answers
`ConsentChoice.No`, and the option is unavailable with the reason
"Player 1 refused this turn." (match the suite's player label).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.MinorActionsSuite distantlands.application.ChancellorsEyeConsentServiceSuite"`
Expected: the Citizen's move to the site fails ("You do not rule your
site"), and the askable assertion fails.

- [ ] **Step 3: Write the implementation**

`ImperialLaws.scala`:

```scala
package distantlands.gameplay.actions

import distantlands.model._

/** The Imperial laws that need another player's consent (CR p. 35). A law
  * is not a card, so it is named here for the consent panels, and its
  * notes are declared through `RuleNotes`. */
object ImperialLaws:
  /** Chancellor's Eye: a Citizen moves Imperial warbands from their site to
    * their board only with the Chancellor's consent. */
  val chancellorsEye: PowerId = PowerId("rule.chancellors-eye")
  /** Warband Transfers: an Imperial player moves warbands to or from
    * another Imperial player's board with that player's consent. */
  val warbandTransfers: PowerId = PowerId("rule.warband-transfers")

  val names: Map[PowerId, String] = Map(
    chancellorsEye -> "Chancellor's Eye",
    warbandTransfers -> "Warband Transfers")

  val notes: NoteSet = NoteSet.unowned("ImperialLaws")
  /** "Needs the Chancellor's consent." */
  val needsChancellor: NoteKey = notes.key("needs-chancellor", Vector(
    NotePart.Text("Needs the Chancellor's consent.")))
  /** "Needs {Blue}'s consent." */
  val needsPartner: NoteKey = notes.key("needs-partner", Vector(
    NotePart.Text("Needs "), NotePart.Arg(0), NotePart.Text("'s consent.")))
  /** "{Blue} refused this turn." */
  val refused: NoteKey = notes.key("refused", Vector(
    NotePart.Arg(0), NotePart.Text(" refused this turn.")))
```

`RuleNotes.all`:

```scala
  val all: Vector[(PowerId, Vector[NoteKey])] =
    Vector(homelandDiscard -> Vector(CardPlayRules.discardFirst),
      ImperialLaws.chancellorsEye -> Vector(ImperialLaws.needsChancellor,
        ImperialLaws.refused),
      ImperialLaws.warbandTransfers -> Vector(ImperialLaws.needsPartner,
        ImperialLaws.refused)) ++
      OperationRestrictions.printedRules.collect {
        case rule if rule.noteKeys.nonEmpty => rule.id -> rule.noteKeys }
```

`ConsentNames.power`, last arm:

```scala
    case None => ImperialLaws.names.getOrElse(id,
      presentation.safeLabel(id.value))
```

`MinorActionProcedures`: `most` reads the resolver and the player's own
kind; `move` moves that kind; `way` adds Chancellor's Eye to the board way:

```scala
  private def most(ready: ReadyGame, actor: PlayerId,
      way: DecisionOptionRef.Button): Either[String, Int] =
    val current = ready.game.current
    current.players.find(_.player == actor).zip(sitedPawn(ready, actor)) match
      case None => Left("Your pawn is not at a site")
      case Some((player, (siteId, site))) =>
        if way == toSite then
          if !SiteRule.rulingPlayer(ready, siteId)
              .contains(SiteRuler.Player(actor)) then
            Left("You do not rule your site")
          else if player.board.warbands == 0 then
            Left("No warbands on your board")
          else if VowOfWanderingRule.holds(ready, actor) then
            Left(VowOfWanderingRule.Reason)
          else Right(player.board.warbands)
        else (site.forces, PlayerForceKind.of(ready, player)) match
          case (SiteForces.Occupied(kind, count), Some(own)) if kind == own =>
            if count > 1 then Right(count - 1)
            else Left("One warband must stay at your site")
          case _ => Left("None of your warbands are at your site")

  private def move(ready: ReadyGame, actor: PlayerId,
      way: DecisionOptionRef.Button, amount: Int): Option[CoreOperation] =
    for
      player <- ready.game.current.players.find(_.player == actor)
      siteId <- player.pawnSite
      kind <- PlayerForceKind.of(ready, player)
    yield
      val board = PositionedLocation(Location.PlayArea(actor))
      val site = PositionedLocation(Location.Site(siteId))
      Move(Piece.Warbands(kind, amount),
        if way == toSite then board else site,
        if way == toSite then site else board)

  private def way(ready: ReadyGame, actor: PlayerId,
      ref: DecisionOptionRef.Button, label: String): DecisionOption =
    val option = DecisionOption.Button(ref, label)
    most(ready, actor, ref).fold(
      reason => DecisionOption.Unavailable(option,
        OptionReason.Written(reason)),
      _ => (if ref == toBoard then eye(ready, actor) else None)
        .fold[DecisionOption](option)(DecisionOption.Unavailable(option, _)))

  /** The key a Chancellor's Eye grant is remembered under. */
  def eyeKey(actor: PlayerId): ConsentKey =
    ConsentKey(ImperialLaws.chancellorsEye, actor, directionDecisionId,
      toBoard)

  /** Chancellor's Eye (CR p. 35): why a Citizen's move to their board
    * waits, or `None` once the Chancellor granted it or for anyone else. */
  private def eye(ready: ReadyGame, actor: PlayerId): Option[OptionReason] =
    if !Imperials.role(ready, actor).contains(Role.Citizen) then None
    else Imperials.chancellor(ready) match
      case None => Some(OptionReason.Written(
        "No Chancellor can consent"))
      case Some(chancellor) =>
        ready.consent.answer(eyeKey(actor), Some(chancellor),
            ConsentTurn.of(ready)) match
          case Some(ConsentAnswer.Granted) => None
          case Some(ConsentAnswer.Refused) => Some(OptionReason.Noted(
            ImperialLaws.chancellorsEye, ImperialLaws.refused.sourceless(
              NoteArg.Player(chancellor))))
          case None => Some(OptionReason.Askable(ImperialLaws.chancellorsEye,
            ImperialLaws.needsChancellor.sourceless(),
            Vector(ConsentAsk(chancellor,
              Vector(ImperialLaws.chancellorsEye))),
            Vector(eyeKey(actor))))
```

`buildMove`'s start gate keeps counting only `most`'s refusals, so a
Citizen whose only way waits on consent can still start the action and
ask.

Regenerate the golden note keys: run the suite that owns
`src/test/resources/notes/declared-keys.txt` (grep `declared-keys` in
`src/test`), read its failure, and add the three `ImperialLaws` keys in the
file's sorted position.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.MinorActionsSuite distantlands.application.ChancellorsEyeConsentServiceSuite distantlands.application.*Consent* distantlands.gameplay.powers.DeclaredNotesSuite"`
(substitute the golden-keys suite's real name).
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/ImperialLaws.scala src/main/scala/distantlands/gameplay/actions/RuleNotes.scala src/main/scala/distantlands/application/ConsentProjector.scala src/main/scala/distantlands/gameplay/actions/minor/MinorActionProcedures.scala src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/MinorActionsSuite.scala src/test/scala/distantlands/application/ChancellorsEyeConsentServiceSuite.scala
git commit -m "feat(empire): move Imperial warbands, asking the Chancellor's consent for a Citizen"
```

---

### Task 7: The Warband Transfers minor action

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala`
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala:120-135` (new `ActionRef`, listed in `ActionRef.all`)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala:269-273` (new entry)
- Modify: `src/main/scala/distantlands/gameplay/phases/ActOffers.scala:51, 65-66`
- Modify: `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala:92`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala:268-270`
- Modify: `src/main/scala/distantlands/application/gamelog/DetailLines.scala:118-120`
- Modify golden and listing tests: `src/test/scala/distantlands/model/ProcedureRefSuite.scala:21`, `src/test/resources/notes/declared-keys.txt`, `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala:178, 198`, `src/test/scala/distantlands/application/PhasePowerMenuSuite.scala:128`, `src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala:246`
- Test: `src/test/scala/distantlands/gameplay/WarbandTransfersSuite.scala` (new), `src/test/scala/distantlands/application/gamelog/LogScripts.scala` plus `src/test/resources/gamelog/warbands-transferred.{actor,other}.log`

**Interfaces:**
- Consumes: `Imperials`, `ImperialLaws.warbandTransfers`, `ImperialLaws.needsPartner`, `ImperialLaws.refused`
- Produces:
  - `ActionRef.TransferWarbands` (key `"transfer-warbands"`)
  - `WarbandTransfers.partnerDecisionId = "minor.transfer-partner"`, `directionDecisionId = "minor.transfer-direction"`, `amountDecisionId = "minor.transfer-amount"`
  - `WarbandTransfers.give: DecisionOptionRef.Button` (`"give"`), `WarbandTransfers.take: DecisionOptionRef.Button` (`"take"`)
  - `WarbandTransfers.build`, `WarbandTransfers.rebuild: MinorActionProcedures.Build`
  - `WarbandTransfers.keys: Vector[NoteKey]`
  - `WarbandTransfers.partnerKey(actor: PlayerId, partner: PlayerId): ConsentKey`

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.minor.WarbandTransfers
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class WarbandTransfersSuite extends munit.FunSuite:
  private val rules = new OathRules(
    distantlands.gameplay.setup.FirstGameSetupFixture.catalog)

  /** p1 is the Chancellor and p2 a Citizen, both at Broken Peaks on p1's
    * turn; p1's board holds 5 warbands and p2's 2. */
  private def imperialsTogether: Table = Table.start.imperial(p1, p2)
    .turn(p1, Phase.Act).pawn(p1, at = "Broken Peaks")
    .pawn(p2, at = "Broken Peaks").boardWarbands(p1, 5).boardWarbands(p2, 2)

  test("an Exile cannot start a transfer"):
    val table = imperialsTogether.turn(p3, Phase.Act)
      .pawn(p3, at = "Broken Peaks")
    assert(rules.startWalker(table.state, ActionRef.TransferWarbands, p3)
      .isLeft)

  test("with no Imperial at the site the transfer is unavailable"):
    val table = imperialsTogether.pawn(p2, at = "Ancient City")
    assert(rules.startWalker(table.state, ActionRef.TransferWarbands, p1)
      .isLeft)

  test("the partner must consent before the transfer goes on"):
    // Start, read the partner decision's options: p2 is askable under
    // ImperialLaws.warbandTransfers with key partnerKey(p1, p2).

  test("once the partner consented, warbands move between the boards"):
    // Grant the consent (through the service, as in
    // ChancellorsEyeConsentServiceSuite), answer give, then 3:
    // p1's board holds 2 and p2's 5.
```

Write the last two tests in full on the service, as Task 6's consent
suite does. Add a `LogScripts` entry following the Move Warbands script at
`LogScripts.scala:436-450` and its two golden logs, reading
"Gave 3 warbands to {Player 2}" for the actor and the other player.

- [ ] **Step 2: Run test to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.WarbandTransfersSuite"`
Expected: compile failure on `ActionRef.TransferWarbands`.

- [ ] **Step 3: Write the implementation**

`ProcedureRef.scala`, beside `MoveWarbands`, and add it to `ActionRef.all`
right after `MoveWarbands`:

```scala
  case object TransferWarbands extends ActionRef { val key = "transfer-warbands" }
```

`WarbandTransfers.scala`:

```scala
package distantlands.gameplay.actions.minor

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.{ImperialLaws, ProcedureNotes}
import distantlands.gameplay.operations.NoteSupport
import distantlands.model._
import distantlands.model.OathViolation._

/** Warband Transfers (CR p. 35): as a minor action, an Imperial player
  * moves warbands to or from another Imperial player's board, when that
  * player's pawn is at their site and that player consents. The partner is
  * chosen first; choosing them asks their consent. */
object WarbandTransfers:
  val partnerDecisionId: String = "minor.transfer-partner"
  val directionDecisionId: String = "minor.transfer-direction"
  val amountDecisionId: String = "minor.transfer-amount"
  val give: DecisionOptionRef.Button = DecisionOptionRef.Button("give")
  val take: DecisionOptionRef.Button = DecisionOptionRef.Button("take")

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.TransferWarbands))
  /** "Gave 3 warbands to {Blue}". */
  val gave: NoteKey = notes.key("gave", Vector(NotePart.Text("Gave "),
    NotePart.Arg(0), NotePart.Text(" "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1)))
  /** "Took 3 warbands from {Blue}". */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took "),
    NotePart.Arg(0), NotePart.Text(" "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" from "),
    NotePart.Arg(1)))
  def keys: Vector[NoteKey] = notes.declared

  def partnerKey(actor: PlayerId, partner: PlayerId): ConsentKey =
    ConsentKey(ImperialLaws.warbandTransfers, actor, partnerDecisionId,
      DecisionOptionRef.Player(partner))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) =>
    for
      _ <- Either.cond(args.isEmpty, (), InvalidEventOrder(
        "walker procedure transfer-warbands takes no start selection"))
      _ <- Either.cond(Imperials.isImperial(ready, actor), (),
        MinorActionUnavailable("Only an Imperial player transfers warbands"))
      _ <- Either.cond(partners(ready, actor).nonEmpty, (),
        MinorActionUnavailable("No other Imperial player is at your site"))
    yield tree(ready, actor)

  val rebuild: MinorActionProcedures.Build = (_, ready, actor, _) =>
    Right(tree(ready, actor))

  /** The other Imperial players whose pawns share the actor's site. */
  private def partners(ready: ReadyGame, actor: PlayerId): Vector[PlayerId] =
    val current = ready.game.current
    current.pawnSite(actor).toVector.flatMap(site =>
      Imperials.players(ready).filter(other => other != actor &&
        current.pawnSite(other).contains(site)))

  private def tree(ready: ReadyGame, actor: PlayerId): Operation =
    Sequence(Vector(
      Decide(partnerDecisionId, actor, DecisionQuery.ChooseOne(
        partners(ready, actor).map(partnerOption(ready, actor, _)),
        heading = Some("Transfer warbands with")),
        presentation = DecisionPresentation.Panel),
      Branch((live, pending) => partner(pending.answered).toVector.map(
        other => Decide(directionDecisionId, actor, DecisionQuery.ChooseOne(
          Vector(way(live, actor, other, give, "Give warbands"),
            way(live, actor, other, take, "Take warbands")),
          heading = Some("Transfer warbands")),
          presentation = DecisionPresentation.Panel))),
      Branch((live, pending) => (for
        other <- partner(pending.answered)
        picked <- direction(pending.answered)
        max = available(live, actor, other, picked)
        if max > 0
      yield Decide(amountDecisionId, actor, DecisionQuery.ChooseAmount(1,
        max, Some("Transfer how many warbands?"), "Transfer"))).toVector),
      BuildOps((live, pending) => Right((for
        other <- partner(pending.answered)
        picked <- direction(pending.answered)
        amount <- pending.answered.collectFirst {
          case Answered(`amountDecisionId`,
              DecisionAnswer.ChooseAmountAnswer(value), _) => value
        }
      yield transfer(actor, other, picked, amount)).toVector),
        required = true),
      Note(ProcedureNotes.id(ActionRef.TransferWarbands),
        transferredNote(actor))))

  /** The partner, askable until they consent (ruling: Warband Transfers
    * needs the other player's consent). */
  private def partnerOption(ready: ReadyGame, actor: PlayerId,
      other: PlayerId): DecisionOption =
    val shown = DecisionOption.Player(DecisionOptionRef.Player(other))
    val key = partnerKey(actor, other)
    ready.consent.answer(key, Some(other), ConsentTurn.of(ready)) match
      case Some(ConsentAnswer.Granted) => shown
      case Some(ConsentAnswer.Refused) => DecisionOption.Unavailable(shown,
        OptionReason.Noted(ImperialLaws.warbandTransfers,
          ImperialLaws.refused.sourceless(NoteArg.Player(other))))
      case None => DecisionOption.Unavailable(shown, OptionReason.Askable(
        ImperialLaws.warbandTransfers,
        ImperialLaws.needsPartner.sourceless(NoteArg.Player(other)),
        Vector(ConsentAsk(other, Vector(ImperialLaws.warbandTransfers))),
        Vector(key)))

  private def way(ready: ReadyGame, actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button, label: String): DecisionOption =
    val option = DecisionOption.Button(ref, label)
    if available(ready, actor, other, ref) > 0 then option
    else DecisionOption.Unavailable(option, OptionReason.Written(
      if ref == give then "No warbands on your board"
      else "No warbands on their board"))

  /** The warbands on the giving board. */
  private def available(ready: ReadyGame, actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button): Int =
    val from = if ref == give then actor else other
    ready.game.current.players.find(_.player == from)
      .fold(0)(_.board.warbands)

  private def transfer(actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button, amount: Int): CoreOperation =
    val mine = PositionedLocation(Location.PlayArea(actor))
    val theirs = PositionedLocation(Location.PlayArea(other))
    Move(Piece.Warbands(ForceKind.Imperial, amount),
      if ref == give then mine else theirs,
      if ref == give then theirs else mine)

  private def partner(answered: Vector[Answered]): Option[PlayerId] =
    answered.collectFirst {
      case Answered(`partnerDecisionId`, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Player(other)), _) => other
    }

  private def direction(answered: Vector[Answered])
      : Option[DecisionOptionRef.Button] = answered.collectFirst {
    case Answered(`directionDecisionId`, DecisionAnswer.ChooseOneAnswer(
        picked: DecisionOptionRef.Button), _) if picked == give ||
        picked == take => picked
  }

  /** How many warbands moved, read off the actor's board, and whom with. */
  private def transferredNote(actor: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    step <- states.previous
    other <- partner(states.answered)
    picked <- direction(states.answered)
    change = NoteSupport.warbands(step, actor)
    moved = if picked == give then -change else change
    if moved > 0
  yield (if picked == give then gave else took)
    .sourceless(NoteArg.Number(moved), NoteArg.Player(other))
```

Check `NoteSupport`'s package with `grep -rn "object NoteSupport"` and fix
the import. If the operation guard refuses a board-to-board warband `Move`
(the last test fails with an `IncompatibleLocation` or a destination
violation), add the `PlayArea` to `PlayArea` case beside the existing
board-to-site handling in `ResourceOperations.scala` (lines 85-100 and
150-190), with a test in that file's suite.

Registration, following Move Warbands at each place:

- `WalkerProcedureRegistry.scala`:
  `ActionRef.TransferWarbands -> Entry(rollDecisionId = None, modifierWindow = None, build = WarbandTransfers.build, rebuild = WarbandTransfers.rebuild)`
- `ActOffers.scala` `all`: `minor(ActionRef.TransferWarbands, "Transfer warbands")`
  after Move Warbands; in `reason`:
  `case (ActionRef.TransferWarbands, OathViolation.MinorActionUnavailable(detail)) => detail`
- `ProcedureNotes.scala`: `case ActionRef.TransferWarbands => WarbandTransfers.keys`
- `ProcedureLog.scala`: `case ActionRef.TransferWarbands => ProcedureLog(tag = None, lines = ActionLineSource.Notes(), narrated = Vector(Told(WarbandTransfers.directionDecisionId)))`
- `DetailLines.scala:118-120`: add `&& run.procedure != ActionRef.TransferWarbands`
  so the moves get no generic line.

Update the listing tests: add `"transfer-warbands"` to
`ProcedureRefSuite`'s key list and `GameLogPropertiesSuite`'s procedure
keys, the two `WarbandTransfers` keys to `declared-keys.txt`, and
"Transfer warbands" after "Move warbands" in `PhaseMenuSuite` and
`PhasePowerMenuSuite`'s expected Act menus. In those menus an Exile sees it
disabled with "Only an Imperial player transfers warbands".

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.WarbandTransfersSuite distantlands.model.ProcedureRefSuite distantlands.gameplay.PhaseMenuSuite distantlands.application.PhasePowerMenuSuite distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/minor/WarbandTransfers.scala src/main/scala/distantlands/model/ProcedureRef.scala src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/distantlands/gameplay/phases/ActOffers.scala src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala src/main/scala/distantlands/application/gamelog/ProcedureLog.scala src/main/scala/distantlands/application/gamelog/DetailLines.scala src/test/scala/distantlands/gameplay/WarbandTransfersSuite.scala src/test/scala/distantlands/model/ProcedureRefSuite.scala src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala src/test/scala/distantlands/application/PhasePowerMenuSuite.scala src/test/scala/distantlands/application/gamelog/GameLogPropertiesSuite.scala src/test/scala/distantlands/application/gamelog/LogScripts.scala src/test/resources/gamelog/warbands-transferred.actor.log src/test/resources/gamelog/warbands-transferred.other.log
git commit -m "feat(empire): add the Warband Transfers minor action"
```

---

### Task 8: Roles and Imperial boards in the browser

**Files:**
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala:57-60` (`readyPlayers`), `:140-180` (`playerBoards`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala:312-337` (`PlayerBoardProjection.supplyBands`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala:197-244` (board codec)
- Modify: `frontend/src/main/scala/distantlands/frontend/SupplyAtRest.scala`, `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala:64-91`
- Test: `src/test/scala/distantlands/application/GamePresentationProjectorSuite.scala` (or the suite that covers `playerBoards`; grep `restSupplyBase` in `src/test`), `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`, `frontend/src/test/scala/distantlands/frontend/SupplyAtRestSuite.scala`, `frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala`

**Interfaces:**
- Consumes: `Imperials.role`, `FinishRestProcedure.supplyRules`, `FinishRestProcedure.supplyGainAtRest`
- Produces: `PlayerBoardProjection.supplyBands: Vector[SupplyBandProjection]` (empty for a Citizen); the seat's `role` string is `"exile"`, `"citizen"` or `"chancellor"`

- [ ] **Step 1: Write the failing tests**

Projector suite, on `Table.start.imperial(p1, p2)`:

```scala
  test("each seat shows its lineage's role"):
    // readyPlayers' roles are Vector("chancellor", "citizen", "exile").

  test("each board carries its own Supply bands"):
    // p1's board bands are the Chancellor's (18+:6, 11-17:5, 4-10:4,
    // 0-3:3), p2's are empty, p3's the Exile's; p1's restSupplyBase reads
    // the purple bank (6 with the quiet table's boards).
```

Write them with the suite's projector helpers; build the expected bands
with `SupplyBandProjection(from, to, supply)`.

`ProjectionProtocolSuite`: round-trip a `PlayerBoardProjection` with
non-empty `supplyBands`, and decode a board JSON without the key into
empty bands.

`SupplyAtRestSuite`:

```scala
  test("the Chancellor reads their own track"):
    assert(SupplyAtRest.readsTrack("chancellor"))
    assert(SupplyAtRest.readsTrack("Exile"))
    assert(!SupplyAtRest.readsTrack("citizen"))
```

Rename the suite's existing `isExile` test to match.

`PlayerBoardSuite`: with a Chancellor seat whose board carries the
Chancellor's bands, clicking the Supply button opens the track panel built
from the board's bands, not the game's.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.* distantlands.protocol.ProjectionProtocolSuite" frontend/test`
Expected: compile failures on `supplyBands` and `readsTrack`.

- [ ] **Step 3: Write the implementation**

`readyPlayers`: replace the literal `"exile"` with the role's key:

```scala
  private def roleKey(ready: ReadyGame, player: PlayerId): String =
    Imperials.role(ready, player) match
      case Some(Role.Chancellor) => "chancellor"
      case Some(Role.Citizen) => "citizen"
      case _ => "exile"
```

`playerBoards`: `restSupplyBase` becomes
`FinishRestProcedure.supplyGainAtRest(ready, player.player).getOrElse(0)`,
and the new field reads the board's table:

```scala
      supplyBands = FinishRestProcedure.supplyRules(ready, player.player)
        .fold(Vector.empty)(_.refreshBands.map(band => SupplyBandProjection(
          band.warbandsInBank.minimum,
          Option.when(band.warbandsInBank.maximum != Int.MaxValue)(
            band.warbandsInBank.maximum),
          band.baseSupply))),
```

Delete the comment at lines 150-151 ("a citizen's kind is Imperial, whose
bag is not the Rest table's (out of scope)").

`PlayerBoardProjection`: add
`supplyBands: Vector[SupplyBandProjection] = Vector.empty` after
`restSupplyBase`. In the board codec, add `"supplyBands"` to the encoder,
to the decoder's `exact` key set, and decode it with the same row reader
`GameProjectionCodec.scala:124-130` uses (move that reader to
`ProjectionCodecSupport` if it is private, and use it from both codecs),
defaulting to empty when absent.

`SupplyAtRest.scala`: rename `isExile` to `readsTrack`, true for every role
but Citizen:

```scala
  /** Whether a seat's Supply follows its own band track: an Exile's and the
    * Chancellor's do; a Citizen's copies the Chancellor's. */
  def readsTrack(role: String): Boolean = !role.equalsIgnoreCase("citizen")
```

and `note` uses `readsTrack`. `panel` takes the bands from the board when
it has them:

```scala
      SupplyAtRest.panel(board,
        if board.supplyBands.nonEmpty then board.supplyBands
        else value.supplyBands, value.supplyMaximum, player.color, own = ...)
```

(the call site is in `WorldBoardRenderer.scala` around line 80; keep its
other arguments as they are).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.* distantlands.protocol.*" frontend/test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/GamePresentationProjector.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala frontend/src/main/scala/distantlands/frontend/SupplyAtRest.scala frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala
git add <each test file you changed, by name>
git commit -m "feat(empire): show roles and each board's own Supply bands"
```

---

### Task 9: Scenario, docs and gates

**Files:**
- Test: `src/test/scala/distantlands/gameplay/EmpireInPlayScenarioSuite.scala` (new)
- Modify: `docs/rules/implementation-traceability.md` (rows 91, 92, 95, 110, 117, 118)
- Modify: `docs/rules/rules-reference.md` (a new "Imperial laws" section after "Victory and game end")
- Modify: `docs/rules/ambiguities.md` (rulings R1, R2, R3)
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md` (Status line)
- Modify: `docs/ROADMAP.md` (delete the slice 1a item)

- [ ] **Step 1: Write the scenario test**

A Citizen's turn played through `OathDriver` on the service, in the style
of the existing end-to-end suites (find one with
`grep -rl "Table.*service" src/test/scala/distantlands/gameplay | head`):

1. `Table.start.imperial(p1, p2)`; the Empire holds two sites in one
   region, one of them holding Toll Roads; p2's turn, Act.
2. p2 moves 1 warband to the Empire site at their pawn (Task 6).
3. p2 ends the Act and rests: p2's Supply becomes p1's Supply plus p2's
   unspent Supply, capped at 7 (Task 5).
4. p3's turn: p3 travels into the Toll Roads site's region to the other
   Empire site and pays the toll to p1, the Chancellor (Task 2).
5. The next round begins with p1 (Task 3).

Assert the board, Supply and favor numbers after each command.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly distantlands.gameplay.EmpireInPlayScenarioSuite"`
Expected: PASS. A failure here points at the task whose behaviour it
checks; fix it there.

- [ ] **Step 3: Update the docs**

- Traceability rows: remove "Imperials deferred" or "Imperials excluded"
  from rows 91 (round order from the Chancellor), 92 (Wake: Exiles only),
  95 (Rest: Chancellor bands, Citizen copies), 117 (Supremacy with the
  Empire; Imperials never flip), 118 (no Vision slot on Imperial boards).
  Row 110: record Chancellor's Eye and Warband Transfers; keep
  "Citizenship, Grand Scepter and self-exile deferred (Empire slice 3)".
  Name the new suites as evidence in each row.
- `rules-reference.md`: an "Imperial laws" section stating Seal of the
  Empire, Steward of Imperial Domains, Imperial warbands, Refreshing
  Supply, Chancellor's Eye and Warband Transfers in one line each, with
  "(CR p. 35)".
- `ambiguities.md`: add R1 (one ruler at a time), R2 (the Chancellor
  outside any turn) and R3 (a Citizen's Supply) in the file's existing
  numbered format, each citing the Empire phase design.
- Spec Status: "Slice 1a implemented, 2026-MM-DD." with the date you merge.
- Roadmap: delete the "Slice 1a" item.

Run: `python3 scripts/check-markdown-links.py`
Expected: "Markdown link check passed".

- [ ] **Step 4: Run every gate**

Run, in order:
- `./sbtw clean coverage test coverageReport`: all tests pass and
  statement coverage is at least 92%.
- `./sbtw frontend/test`: all tests pass.
- `python3 scripts/check-architecture.py`: passes. A new `model` file
  must not import `gameplay`; `Imperials` reads only the model.
- `python3 scripts/check-markdown-links.py`: passes.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/gameplay/EmpireInPlayScenarioSuite.scala docs/rules/implementation-traceability.md docs/rules/rules-reference.md docs/rules/ambiguities.md docs/superpowers/specs/2026-10-29-empire-phase-design.md docs/ROADMAP.md
git commit -m "test(empire): play a Citizen's turn and record slice 1a in the docs"
```
