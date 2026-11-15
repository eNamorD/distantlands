# Catalog Batch 12, Slice 2: Wider Eligibility and Binding Promises Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Traveling Negotiator, The Tribunal and Bandit Tribunal widen
who the Negotiate action reaches, and open slice 1's binding-promise box
when a participant may use The Tribunal.

**Architecture:** One rule object beside the action,
`NegotiationReach`, holds the three cards' checks. Actions may not import
`gameplay.powers`, so the rule lives in `gameplay.actions.negotiation`
with the two card objects, as `ChallengeReach` and `VowOfWanderingRule`
do. The two non-persistent powers pass the walk's power-use filter (City
Wall, Giant Python, Bandit Forest), judged as the use of whoever holds the
access. The Negotiation reads the filter once, at its start. It stores the
outcome as walker start facts: the candidates, and the players who may use
The Tribunal. Every rebuild reads them back, as Travel reads its source.
The three powers register with no window, so the cards count as
implemented.

**Tech Stack:** Scala 3, Scala.js (frontend, unchanged here), munit,
`./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../../specs/2026-11/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (7 to 9, 11, 24 and 25), "The deal model" and "Slice 2:
Wider eligibility, binding promises".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, parameters or values, no
  non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`). It may import `gameplay.powerresolver`
  and `gameplay.operations`.
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire and journal changes need not keep old journals decodable. A
  Negotiation parked before this slice has no start facts and need not
  resume.
- Ruling 7: Traveling Negotiator works both ways (its holder may choose any
  player; any player may choose its holder).
- Ruling 8: The Tribunal works the same way for every player with access
  (pawn at its site, or ruling its site).
- Ruling 9: Bandit Tribunal is persistent and ignores access: two players
  whose pawns both stand at bandit-ruled sites may negotiate.
- Ruling 11: binding promises open when any participant has access to The
  Tribunal (intact).
- Ruling 24: City Wall, Giant Python and Bandit Forest stop The Tribunal
  and Traveling Negotiator as they stop any non-persistent power.
- Ruling 25: the blockers judge the use of whoever holds the access. If
  either side has an unblocked route, the two may negotiate.
- The package is `distantlands`. Paths below are at `519f746cb` plus slice
  1 (`docs/superpowers/plans/2026-11/2026-11-09-catalog-batch-12-slice1.md`).
  Slice 1 shifts line numbers in the Negotiation files, so this plan
  anchors edits by code, not by line.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Setup

Slice 1 must be merged into `0.2.0-alpha` first. This plan uses its
`NegotiationBounds.bindingPromises`, `NegotiationTerms.bindingPromises`,
`NegotiationProcedure.promisedBinding`, the editing projection's
`bindingPromisesOpen` and the `negotiationPromised` log script. Check with
`git grep -n "bindingPromises" -- src/main/scala/distantlands/model/NegotiationTerms.scala`.

Work in a worktree off `0.2.0-alpha`, never in the main checkout. Symlink
the main checkout's `.tooling` and `node_modules` into the worktree before
running `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

## Plan-time findings

1. **Actions cannot name a power.** `scripts/check-architecture.py`
   forbids `gameplay.actions` importing `gameplay.powers`. The spec's
   "`TravelingNegotiator.reaches`" therefore becomes
   `NegotiationReach.travelingNegotiator` beside the action. The card
   objects move there too (precedent: `ChallengeReachCards`,
   `VowOfWanderingRule`). The power objects in `gameplay.powers.action`
   hook no window (precedent: `BanditCrown`), because
   `PowerImplementationStatus` counts a power as implemented only when it
   is registered.
2. **The blockers are not reachable from a procedure.** Power-use
   restrictions live on `WalkerPowers.useRestrictions`. A procedure's
   `build` gets no powers, except the entries `WalkerProcedureRegistry.lookup`
   builds with them (UsePower, the menus, the forced Wake). The Negotiation
   joins them: `lookup` builds its entry from the walk's powers, and
   `OathRulesWalker.buildWalker` routes it to the registry with
   `walkerPowers`.
3. **The reach must not be recomputed on rebuild.** The rebuild paths
   (`OathRules.declaredWalkerTree`, `WalkerDecisionProjector.declaredTree`)
   carry no powers. A Branch also re-selects on resume from current
   state, and after settlement Citizenship can change a player's role,
   which Bandit Forest reads. A rebuilt candidate list could then differ
   from the one the start built. The single-candidate path reads
   participants from it, so the tree would shift. The start therefore
   stores the reach as `walkerStartFacts`, which `OathRulesWalker.startNow`
   records through `WalkerProcedureRegistry.startFacts`, as Travel stores
   its source.
4. **Binding promises need the same facts.** Whether a participant may
   use The Tribunal also passes the blockers (ruling 24), and the deal
   snapshot is rebuilt on every command. So the start facts also carry the
   Tribunal's users. A deal opens binding promises when any participant is
   among them.
5. **Access already has one definition.** `PowerAccess.accessible` with
   `RuleSourceRef.Edifice(site, id)` covers a pawn at the site, a ruled
   site, the Bandit Crown's bandit sites and the Grand Mask.
   `BanditCrownRule.banditSites` lists the bandit-ruled sites in play. Both
   are `private[gameplay]`, so `gameplay.actions` can read them.
6. **Who uses it.** `PowerUse.of(user, printed, card)` builds the use, and
   `PowerUseRules.refusals` applies the blockers and lifts any grant this
   turn. The Negotiation asks no consent: a refusal City Wall's ruler
   could waive only blocks.

## File structure

| File | Responsibility |
|---|---|
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReachCards.scala` (new) | `TravelingNegotiatorCard`, `TheTribunalCard`, moved out of holding |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala` (new) | the three reach checks and the Tribunal's users |
| `src/main/scala/distantlands/gameplay/powers/action/NegotiationReachPowers.scala` (new) | the three registered powers, hooking no window |
| `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala` | loses Traveling Negotiator |
| `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala` | loses The Tribunal |
| `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` | imports the moved cards |
| `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` | registers the three powers |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala` (new) | candidates and Tribunal users, as start facts |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | `eligible` reads the reach; participants from the facts; the binding flag |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | build with restrictions, rebuild from facts, start facts |
| `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala` | the Negotiation entry built from the walk's powers |
| `src/main/scala/distantlands/gameplay/OathRulesWalker.scala` | routes the Negotiation start through the registry with powers |
| `src/test/resources/powers/unimplemented.txt` | loses three lines |
| `docs/architecture/all-exile-negotiation.md` | eligibility, start facts, supported powers |

---

### Task 1: The reach rule and its cards

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReachCards.scala`
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/action/NegotiationReachPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/action/NegotiationReachSuite.scala` (new)

**Interfaces:**
- Consumes: `PowerAccess.faceupAdviserHolder`, `PowerAccess.accessible`,
  `BanditCrownRule.banditSites`, `PowerUseRules.refusals`, `PowerUse.of`,
  `GameState.edificeSite`.
- Produces:
  - `TravelingNegotiatorCard` (`DenizenId("242")`, `.power`) and
    `TheTribunalCard` (`EdificeId("E19")`, `.intact.power`,
    `.ruined.power`), in package
    `distantlands.gameplay.actions.negotiation`.
  - `NegotiationReach.reaches(catalog: ExecutableCatalog, ready: ReadyGame,
    actor: PlayerId, other: PlayerId, restrictions:
    Vector[PowerUseRestriction]): Boolean`.
  - `NegotiationReach.tribunalUsers(catalog: ExecutableCatalog, ready:
    ReadyGame, players: Vector[PlayerId], restrictions:
    Vector[PowerUseRestriction]): Vector[PlayerId]`, in the order given.
  - Powers `TravelingNegotiator`, `TheTribunal`, `BanditTribunal` in
    `distantlands.gameplay.powers.action`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/action/NegotiationReachSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{NegotiationReach,
  TheTribunalCard, TravelingNegotiatorCard}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.powers.travel.ForbiddenForestCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Traveling Negotiator (242), The Tribunal (E19 intact) and Bandit
  * Tribunal (E19 ruined): who each lets a Negotiation reach beyond the
  * actor's site (catalog batch 12, rulings 7 to 9, 24 and 25). */
class NegotiationReachSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val city = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City: no two share a
    * site. */
  private def apart: Table = Table.start
    .pawn(p1, at = peaks).pawn(p2, at = dunes).pawn(p3, at = city)

  /** The players other than `actor` whom a power lets `actor` reach, in
    * table order. */
  private def reachedBy(table: Table, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction] = Vector.empty)
      : Vector[PlayerId] =
    val ready = table.ready
    ready.game.current.players.map(_.player).filter(other => other != actor &&
      NegotiationReach.reaches(catalog, ready, actor, other, restrictions))

  test("with no power, nobody reaches a player at another site"):
    Vector(p1, p2, p3).foreach(actor =>
      assertEquals(reachedBy(apart, actor), Vector.empty[PlayerId], actor))

  test("Traveling Negotiator's holder reaches any player, and any player " +
      "reaches its holder"):
    val held = apart.adviser(p1, TravelingNegotiatorCard.id)
    assertEquals(reachedBy(held, p1), Vector(p2, p3))
    assertEquals(reachedBy(held, p2), Vector(p1))
    assertEquals(reachedBy(held, p3), Vector(p1))

  test("a facedown Traveling Negotiator reaches nobody"):
    val hidden = apart.adviser(p1, TravelingNegotiatorCard.id,
      facedown = true)
    assertEquals(reachedBy(hidden, p1), Vector.empty[PlayerId])

  test("The Tribunal reaches from its site, both ways"):
    val atDunes = apart.edifice(TheTribunalCard, EdificeSide.Intact,
      at = dunes)
    assertEquals(reachedBy(atDunes, p2), Vector(p1, p3))
    assertEquals(reachedBy(atDunes, p1), Vector(p2))
    assertEquals(reachedBy(atDunes, p3), Vector(p2))

  test("The Tribunal reaches from the site its user rules"):
    val ruled = apart.edifice(TheTribunalCard, EdificeSide.Intact, at = woods)
      .warbandsAt(woods, p1, 1)
    assertEquals(reachedBy(ruled, p1), Vector(p2, p3))
    assertEquals(NegotiationReach.tribunalUsers(catalog, ruled.ready,
      Vector(p1, p2, p3), Vector.empty), Vector(p1))

  test("the ruined face gives no Tribunal reach"):
    val ruined = apart.edifice(TheTribunalCard, EdificeSide.Ruined, at = dunes)
    assertEquals(reachedBy(ruined, p2), Vector.empty[PlayerId])
    assertEquals(NegotiationReach.tribunalUsers(catalog, ruined.ready,
      Vector(p1, p2, p3), Vector.empty), Vector.empty[PlayerId])

  test("Bandit Tribunal joins players whose pawns stand at bandit-ruled " +
      "sites, while it is on the map"):
    val bandits = apart.bandits(peaks, 1).bandits(dunes, 1)
    val ruined = bandits.edifice(TheTribunalCard, EdificeSide.Ruined,
      at = woods)
    assertEquals(reachedBy(ruined, p1), Vector(p2))
    assertEquals(reachedBy(ruined, p2), Vector(p1))
    assertEquals(reachedBy(ruined, p3), Vector.empty[PlayerId])
    assertEquals(reachedBy(bandits, p1), Vector.empty[PlayerId])
    // Persistent: no blocker touches it.
    assertEquals(reachedBy(ruined, p1, blockers), Vector(p2))

  test("City Wall stops its ruler's enemy at its site from using Traveling " +
      "Negotiator, whoever negotiates"):
    val walled = apart.adviser(p1, TravelingNegotiatorCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(reachedBy(walled, p1), Vector(p2, p3))
    assertEquals(reachedBy(walled, p1, blockers), Vector.empty[PlayerId])
    assertEquals(reachedBy(walled, p3, blockers), Vector.empty[PlayerId])

  test("Giant Python at The Tribunal's site stops every use of it"):
    val python = apart.edifice(TheTribunalCard, EdificeSide.Intact, at = dunes)
      .denizen(GiantPythonCard.id, at = dunes)
    assertEquals(reachedBy(python, p1), Vector(p2))
    assertEquals(reachedBy(python, p1, blockers), Vector.empty[PlayerId])

  test("Bandit Forest stops an Imperial, not an Exile, from using a " +
      "Tribunal bandits rule"):
    // p2 is the Chancellor and p1 a Citizen. p1 and p3 stand at Ancient
    // City, where bandits rule The Tribunal; Bandit Forest lies at Deep
    // Woods; p2 is at Broken Peaks.
    val forest = Table.start.imperial(p2, p1)
      .pawn(p1, at = city).pawn(p3, at = city).pawn(p2, at = peaks)
      .bandits(city, 2)
      .edifice(TheTribunalCard, EdificeSide.Intact, at = city)
      .edifice(ForbiddenForestCard, EdificeSide.Ruined, at = woods)
    assertEquals(NegotiationReach.tribunalUsers(catalog, forest.ready,
      Vector(p1, p2, p3), blockers), Vector(p3))
    assertEquals(reachedBy(forest, p1, blockers), Vector(p3))
    assertEquals(reachedBy(forest, p3, blockers), Vector(p1, p2))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.NegotiationReachSuite"`
Expected: compile error: `NegotiationReach` not found in
`distantlands.gameplay.actions.negotiation`.

- [ ] **Step 3: Move the cards**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReachCards.scala`
with the two card objects cut from holding, text unchanged:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{AdviserOnly, Denizen, Edifice, EdificeFace,
  Locked, PrintedPower}
import distantlands.model._

/** The cards whose powers widen who a Negotiation reaches (catalog batch
  * 12, slice 2). They live beside the Negotiation, which reads them
  * ([[NegotiationReach]]), as the Challenge's reach cards live beside the
  * Challenge. Their powers are registered in `gameplay.powers.action`. The
  * Tribunal's "even into later games" is printed text the engine does not
  * track. */
object TravelingNegotiatorCard extends Denizen(DenizenId("242"), "Traveling Negotiator", Suit.Nomad) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.traveling-negotiator"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may negotiate with any player.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheTribunalCard extends Edifice(EdificeId("E19"), Suit.Order):
  object intact extends EdificeFace("The Tribunal") with Locked:
    val power = PrintedPower(PowerId("edifice.e19.intact"),
      persistent = false, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** You may negotiate with players at any " +
        "sites. Players may make binding promises, even into later " +
        "games.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Bandit Tribunal"):
    val power = PrintedPower(PowerId("edifice.e19.ruined"),
      persistent = true, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** Players at sites ruled by bandits may " +
        "negotiate with each other.")
    val powers: Vector[PrintedPower] = Vector(power)
```

Delete `TravelingNegotiatorCard` from `HoldingNomadDenizens.scala` and
narrow its import to `import distantlands.catalog.{Denizen, PrintedPower,
SiteOnly}` (The Gathering is site-only; nothing else there is
adviser-only). Delete `TheTribunalCard` from `HoldingEdifices.scala`; its
import stays, since Stone Baths and Amber Doors use every name in it.

In `NewFoundations.scala`, after
`import distantlands.gameplay.actions.recover.UndergroundLibraryCard`, add:

```scala
import distantlands.gameplay.actions.negotiation.{TheTribunalCard,
  TravelingNegotiatorCard}
```

The two catalog lists keep naming `TravelingNegotiatorCard` and
`TheTribunalCard` where they are.

- [ ] **Step 4: Write the reach rule**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.{BanditCrownRule, PowerAccess}
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Who a Negotiation reaches beyond the actor's site (catalog batch 12,
  * rulings 7 to 9, 24 and 25). The Negotiation reads it, so it sits beside
  * the action, outside `powers`; the three powers that register the cards
  * hook no window.
  *
  *  - **Traveling Negotiator** (faceup adviser): its holder reaches any
  *    player, and any player reaches its holder.
  *  - **The Tribunal** (intact): a player with access to it (its site,
  *    a site they rule, or another route `PowerAccess` grants) reaches
  *    any player, and any player reaches them.
  *  - **Bandit Tribunal** (ruined, persistent): while it is on the map,
  *    two players whose pawns stand at bandit-ruled sites reach each other.
  *
  * The two non-persistent powers pass `restrictions`, the walk's power-use
  * restrictions (City Wall, Giant Python, Bandit Forest), judged as the use
  * of the player who holds the card or has the access (ruling 25). A grant
  * remembered this turn lifts a refusal; the Negotiation itself asks for
  * none. Bandit Tribunal ignores access and the blockers (CR "Persistent
  * Powers").
  */
object NegotiationReach:
  def reaches(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      other: PlayerId, restrictions: Vector[PowerUseRestriction]): Boolean =
    travelingNegotiator(catalog, ready, actor, other, restrictions) ||
      tribunalUsers(catalog, ready, Vector(actor, other), restrictions)
        .nonEmpty ||
      banditTribunal(ready, actor, other)

  /** Whether one of the two holds Traveling Negotiator faceup and may use
    * it. */
  def travelingNegotiator(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, other: PlayerId,
      restrictions: Vector[PowerUseRestriction]): Boolean =
    PowerAccess.faceupAdviserHolder(ready, TravelingNegotiatorCard.id)
      .exists(holder => (holder == actor || holder == other) &&
        usable(catalog, ready, PowerUse.of(holder,
          TravelingNegotiatorCard.power, TravelingNegotiatorCard.id),
          restrictions))

  /** The players among `players` with access to The Tribunal (intact) who
    * may use it, in the order given: they reach any player, and a deal
    * holding one of them takes binding promises (ruling 11). */
  def tribunalUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    ready.game.current.edificeSite(TheTribunalCard.id, EdificeSide.Intact)
      .toVector.flatMap(site => players.filter(player =>
        PowerAccess.accessible(RuleSourceRef.Edifice(site, TheTribunalCard.id),
          RuleSourceFace.Intact, ready, player) &&
          usable(catalog, ready, PowerUse.of(player,
            TheTribunalCard.intact.power, TheTribunalCard.id), restrictions)))

  /** Whether Bandit Tribunal is on the map and both pawns stand at
    * bandit-ruled sites. */
  def banditTribunal(ready: ReadyGame, actor: PlayerId,
      other: PlayerId): Boolean =
    val current = ready.game.current
    lazy val bandits = BanditCrownRule.banditSites(ready)
    current.edificeSite(TheTribunalCard.id, EdificeSide.Ruined).isDefined &&
      Vector(actor, other).forall(player =>
        current.pawnSite(player).exists(bandits))

  private def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      use: PowerUse, restrictions: Vector[PowerUseRestriction]): Boolean =
    PowerUseRules.refusals(catalog, ready, use, restrictions).isEmpty
```

- [ ] **Step 5: Register the powers**

Create `src/main/scala/distantlands/gameplay/powers/action/NegotiationReachPowers.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{TheTribunalCard,
  TravelingNegotiatorCard}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Traveling Negotiator (242): "**WHEN NEGOTIATING:** You may negotiate
  * with any player." (catalog batch 12, ruling 7).
  *
  * The rule is `NegotiationReach`, which the Negotiation reads at its
  * start. This power hooks no window. It is registered so the card counts
  * as implemented. It writes no line.
  */
case object TravelingNegotiator extends ContributingPower:
  val id: PowerId = TravelingNegotiatorCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** The Tribunal (E19, intact): "**WHEN NEGOTIATING:** You may negotiate
  * with players at any sites. Players may make binding promises, even into
  * later games." (catalog batch 12, rulings 8 and 11).
  *
  * The rule is `NegotiationReach`: its users reach any player, and a deal
  * holding one opens binding promises, which the engine records in the Game
  * Log and never enforces. "Even into later games" is not tracked. This
  * power hooks no window. It is registered so the card counts as
  * implemented.
  */
case object TheTribunal extends ContributingPower:
  val id: PowerId = TheTribunalCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Bandit Tribunal (E19, ruined): "**WHEN NEGOTIATING:** Players at sites
  * ruled by bandits may negotiate with each other." (catalog batch 12,
  * ruling 9). Persistent, so it applies to every player, ignoring access.
  *
  * The rule is `NegotiationReach`. This power hooks no window. It is
  * registered so the card counts as implemented.
  */
case object BanditTribunal extends ContributingPower:
  val id: PowerId = TheTribunalCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `WalkerPowerCatalog.contributing`, extend the vector that holds
`BanditCrown`:

```scala
      Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown,
        GrandMask, DesecratedGround, FalseProphetClaim, TravelingNegotiator,
        TheTribunal, BanditTribunal) ++
```

In `src/test/resources/powers/unimplemented.txt`, delete the lines
`denizen.traveling-negotiator`, `edifice.e19.intact` and
`edifice.e19.ruined`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.NegotiationReachSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.PowerKindsCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite"`
Expected: PASS.

If the City Wall, Giant Python or Bandit Forest test fails, read that
restriction's `refusal` before changing anything: the test states rulings
24 and 25, and the fix belongs in `NegotiationReach`, not in the
restriction.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReachCards.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala \
  src/main/scala/distantlands/gameplay/powers/action/NegotiationReachPowers.scala \
  src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala \
  src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/powers/action/NegotiationReachSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): reach rule for Traveling Negotiator and the Tribunal

The two cards move beside the Negotiation, which will read
NegotiationReach. The non-persistent powers pass the power-use filter,
judged as their user's use. The three powers register with no window.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: The Negotiation reads the reach at its start

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`
- Modify: `src/main/scala/distantlands/gameplay/OathRulesWalker.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`
- Test: `src/test/scala/distantlands/application/GameApplicationServiceReopenSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationReach.reaches`,
  `NegotiationReach.tribunalUsers`, the two cards; slice 1's
  `NegotiationBounds.bindingPromises: Boolean`.
- Produces:
  - `NegotiationStartFacts(candidates: Vector[PlayerId], tribunalUsers:
    Vector[PlayerId])` with `refs: Vector[DecisionOptionRef]` and
    `bindingPromises(participants: Vector[PlayerId]): Boolean`; companion
    `Marker: String`, `at(catalog, ready, actor, restrictions)`,
    `read(refs): Either[OathViolation, NegotiationStartFacts]`.
  - `NegotiationDeal.eligible(catalog: ExecutableCatalog, state: ReadyGame,
    actor: PlayerId, restrictions: Vector[PowerUseRestriction] =
    Vector.empty): Vector[PlayerId]`.
  - `NegotiationDeal.chosen(candidates, pending)`,
    `participants(state, actor, candidates, pending)`,
    `deal(state, actor, candidates, pending)`,
    `snapshot(state, deal, bindingPromises: Boolean = false)`.
  - `NegotiationProcedure.build(restrictions)(catalog, state, actor, args)`,
    `NegotiationProcedure.rebuild(catalog, state, actor, args)` (reads the
    facts), `NegotiationProcedure.startFacts(restrictions)(catalog, state,
    actor)`.
  - `WalkerProcedureRegistry.startFacts(procedure, catalog, state, actor,
    walkerPowers: WalkerPowers = WalkerPowers.empty)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.NegotiationStartFacts
import distantlands.model._
import distantlands.testkit.Table.{p1, p2, p3}

/** A Negotiation's start facts as the walker stores them. */
class NegotiationStartFactsSuite extends munit.FunSuite:
  test("the facts read back as they were written"):
    Vector(NegotiationStartFacts(Vector(p2, p3), Vector(p1)),
        NegotiationStartFacts(Vector(p2), Vector.empty)).foreach(facts =>
      assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts)))

  test("facts with no marker, or with a ref that names no player, are " +
      "refused"):
    val refused = Vector(
      Vector[DecisionOptionRef](DecisionOptionRef.Player(p2)),
      Vector[DecisionOptionRef](DecisionOptionRef.Site(SiteId("s1")),
        DecisionOptionRef.Button(NegotiationStartFacts.Marker)),
      Vector.empty[DecisionOptionRef])
    refused.foreach(refs => assert(NegotiationStartFacts.read(refs).left
      .toOption.exists(_.isInstanceOf[OathViolation.InvalidEventOrder]),
      refs))

  test("a deal takes binding promises when a participant may use The " +
      "Tribunal"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector(p3))
    assert(facts.bindingPromises(Vector(p1, p3)))
    assert(!facts.bindingPromises(Vector(p1, p2)))
```

Add to `NegotiationProcedureSuite` (imports to add:
`distantlands.gameplay.actions.negotiation.{NegotiationStartFacts,
TheTribunalCard, TravelingNegotiatorCard}`,
`distantlands.gameplay.cards.{AncientCitySite, FairIsleSite}` beside the
existing site import, `distantlands.gameplay.powers.WalkerPowerCatalog`,
`distantlands.gameplay.powers.poweruse.CityWallCard`, and
`distantlands.testkit.{ParkedNode, PowerRules}` beside the existing
testkit import):

```scala
  /** The bounds of the deal `state` is parked on, by author. */
  private def parkedBounds(state: OathState): Map[PlayerId, NegotiationBounds] =
    ParkedNode.of(state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide.query match
        case negotiate: DecisionQuery.Negotiate => negotiate.bounds
        case other => fail(s"not a deal: $other")
      case other => fail(s"not parked on a decision: $other")

  test("Traveling Negotiator's holder chooses among players at other sites"):
    val remote = Table.start.pawn(p1, at = site).pawn(p2, at = DunesSite)
      .pawn(p3, at = DunesSite).favor(p1, 5).favor(p2, 5)
      .adviser(p1, TravelingNegotiatorCard.id).ready
    val started = start(remote).getOrElse(fail("Negotiation must start"))
    parked.assertParked(started.state, ActionRef.Negotiation, negotiators, p1)
    val dealt = choose(started.state, p1, p2).getOrElse(
      fail("the negotiators must be accepted"))
    parked.assertParked(dealt.state, ActionRef.Negotiation, dealId, p1)
    assert(say(dealt.state, p1, gift(p2, 1)).isRight)

  test("a lone remote candidate skips the choice, and the start stores " +
      "the reach"):
    val remote = Table.start.pawn(p1, at = site).pawn(p2, at = DunesSite)
      .pawn(p3, at = FairIsleSite).adviser(p2, TravelingNegotiatorCard.id)
      .ready
    val started = start(remote).getOrElse(fail("Negotiation must start"))
    parked.assertParked(started.state, ActionRef.Negotiation, dealId, p1)
    assertEquals(Outcomes.ready(started.state).game.current.walkerStartFacts,
      NegotiationStartFacts(Vector(p2), Vector.empty).refs)

  test("City Wall's ruler's enemy at its site cannot use Traveling " +
      "Negotiator to start one"):
    val walled = Table.start.pawn(p1, at = AncientCitySite)
      .pawn(p2, at = DunesSite).pawn(p3, at = DunesSite)
      .adviser(p1, TravelingNegotiatorCard.id)
      .denizen(CityWallCard.id, at = AncientCitySite)
      .warbandsAt(AncientCitySite, p2, 1).ready
    // With no walker powers nothing blocks it.
    assert(start(walled).isRight)
    val blocked = PowerRules.withWalkerPowers.startWalker(Ready(walled),
      ActionRef.Negotiation, p1)
    assert(blocked.left.toOption.exists(
      _.isInstanceOf[OathViolation.NegotiationUnavailable]), blocked)
    assert(!StartPreview.opens(catalog, walled, p1, ActionRef.Negotiation,
      WalkerPowerCatalog.default(catalog)))

  test("with The Tribunal in reach, binding promises are open to every " +
      "author, and closed without it"):
    val tribunal = gathered.pawn(p3, at = DunesSite)
      .edifice(TheTribunalCard, EdificeSide.Intact, at = site).ready
    // p1 may use The Tribunal, so it reaches p3 too: the choice is asked.
    val open = atDeal(tribunal, p2)
    assert(parkedBounds(open.state).values.forall(_.bindingPromises),
      parkedBounds(open.state))
    assert(say(open.state, p2, ProposeTerms(NegotiationTerms(
      bindingPromises = Vector("I will not attack you this round."))))
      .isRight)
    val closed = atDeal(gathered.ready, p2)
    assert(parkedBounds(closed.state).values.forall(!_.bindingPromises),
      parkedBounds(closed.state))
```

In `NegotiationDealSuite`, import
`distantlands.gameplay.setup.FirstGameSetupFixture.catalog` and
`distantlands.gameplay.actions.negotiation.TravelingNegotiatorCard`, and
replace the two eligibility and participant tests with:

```scala
  test("eligible lists the other players at the actor's site, in table order"):
    assertEquals(NegotiationDeal.eligible(catalog, ready, p1), Vector(p2, p3))
    assertEquals(NegotiationDeal.eligible(catalog,
      gathered.pawn(p3, at = DunesSite).ready, p1), Vector(p2))
    assertEquals(NegotiationDeal.eligible(catalog,
      gathered.pawn(p2, at = DunesSite).pawn(p3, at = DunesSite).ready, p1),
      Vector.empty[PlayerId])

  test("eligible adds the players a power reaches, in table order"):
    val away = gathered.pawn(p2, at = DunesSite)
      .adviser(p2, TravelingNegotiatorCard.id)
    assertEquals(NegotiationDeal.eligible(catalog, away.ready, p1),
      Vector(p2, p3))

  test("participants come from the negotiator answer, else the one candidate"):
    val chosen = PendingTree(Vector("1"), Vector(Answered(
      NegotiationDeal.negotiatorsDecisionId,
      ChooseManyAnswer(Vector(DecisionOptionRef.Player(p3))), p1)))
    assertEquals(NegotiationDeal.participants(ready, p1, Vector(p2, p3),
      chosen), Vector(p1, p3))
    assertEquals(NegotiationDeal.participants(ready, p1, Vector(p2),
      PendingTree(Vector.empty, Vector.empty)), Vector(p1, p2))
```

In `GameApplicationServiceReopenSuite.negotiationOpened`, change the
eligibility assertion to
`NegotiationDeal.eligible(catalog, Outcomes.ready(traveled.state), p1)`.
The suite already has `catalog` in scope.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.NegotiationDealSuite"`
Expected: compile error: `NegotiationStartFacts` not found.

- [ ] **Step 3: Write the start facts**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.model._

/** What a Negotiation's start settles for its whole walk, stored as
  * `walkerStartFacts` (catalog batch 12, slice 2): the players the actor
  * may choose (`candidates`), and the players among the actor and the
  * candidates who may use The Tribunal (`tribunalUsers`).
  *
  * Both read the walk's power-use restrictions, which a rebuild does not
  * carry, and both could change once the deal settles: Citizenship changes
  * a role, which Bandit Forest reads. So the start computes them, and every
  * rebuild reads them back, as Travel reads its source.
  *
  * Stored as option refs: each candidate as a player, then a `Button`
  * holding [[NegotiationStartFacts.Marker]], then each Tribunal user as a
  * player.
  */
final case class NegotiationStartFacts(candidates: Vector[PlayerId],
    tribunalUsers: Vector[PlayerId]):
  def refs: Vector[DecisionOptionRef] =
    candidates.map(DecisionOptionRef.Player(_)) ++
      (DecisionOptionRef.Button(NegotiationStartFacts.Marker) +:
        tribunalUsers.map(DecisionOptionRef.Player(_)))

  /** Whether a deal among `participants` takes binding promises: one of
    * them may use The Tribunal (ruling 11). */
  def bindingPromises(participants: Vector[PlayerId]): Boolean =
    participants.exists(tribunalUsers.contains)

object NegotiationStartFacts:
  /** The key of the `Button` ref between the two lists. */
  val Marker: String = "negotiation.tribunal-users"

  def at(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction]): NegotiationStartFacts =
    val candidates = NegotiationDeal.eligible(catalog, ready, actor,
      restrictions)
    NegotiationStartFacts(candidates, NegotiationReach.tribunalUsers(catalog,
      ready, actor +: candidates, restrictions))

  def read(refs: Vector[DecisionOptionRef])
      : Either[OathViolation, NegotiationStartFacts] =
    val (before, after) = refs.span(_ != DecisionOptionRef.Button(Marker))
    for
      _ <- Either.cond(after.nonEmpty, (), malformed(refs))
      candidates <- players(before, refs)
      users <- players(after.drop(1), refs)
    yield NegotiationStartFacts(candidates, users)

  private def players(part: Vector[DecisionOptionRef],
      refs: Vector[DecisionOptionRef]): Either[OathViolation, Vector[PlayerId]] =
    val ids = part.collect { case DecisionOptionRef.Player(id) => id }
    Either.cond(ids.size == part.size, ids, malformed(refs))

  private def malformed(refs: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} has malformed start " +
        s"facts: ${refs.map(_.kind).mkString(", ")}")
```

- [ ] **Step 4: Read the reach and the facts in the deal**

In `NegotiationDeal.scala`, add imports
`distantlands.catalog.ExecutableCatalog` and
`distantlands.gameplay.powerresolver.PowerUseRestriction`, and replace
`eligible`, `chosen`, `participants`, `deal` and `snapshot`:

```scala
  /** The players the actor may deal with, in table order: the others whose
    * pawn is at the actor's pawn site, and the others a power lets the
    * actor reach ([[NegotiationReach]], judged under `restrictions`, the
    * walk's power-use restrictions). The one place that rule lives. The
    * start reads it once and stores it ([[NegotiationStartFacts]]).
    */
  def eligible(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction] = Vector.empty)
      : Vector[PlayerId] =
    val current = state.game.current
    val site = current.pawnSite(actor)
    current.players.map(_.player).filter(other => other != actor &&
      (site.exists(current.pawnSite(other).contains) ||
        NegotiationReach.reaches(catalog, state, actor, other, restrictions)))

  /** The players the actor chose, in the order the answer named them, or
    * the one candidate when the choice was omitted. The Game Log's opening
    * line names them in this order. */
  def chosen(candidates: Vector[PlayerId],
      pending: PendingTree): Vector[PlayerId] =
    pending.answered.reverse.collectFirst {
      case Answered(`negotiatorsDecisionId`, ChooseManyAnswer(selected), _) =>
        selected.collect { case DecisionOptionRef.Player(id) => id }
    }.getOrElse(candidates)

  /** Actor first, then the chosen players in table order. With no recorded
    * negotiator answer the one candidate is the negotiator: the tree omits
    * the choice when there is only one.
    */
  def participants(state: ReadyGame, actor: PlayerId,
      candidates: Vector[PlayerId], pending: PendingTree): Vector[PlayerId] =
    val picked = chosen(candidates, pending).toSet
    actor +: state.game.current.players.map(_.player).filter(picked)
```

```scala
  def deal(state: ReadyGame, actor: PlayerId, candidates: Vector[PlayerId],
      pending: PendingTree): DealState =
    fold(participants(state, actor, candidates, pending), pending.answered)

  /** The question the deal asks now: everyone's terms, who accepted, what each
    * author may still offer, and who may accept (anyone who has not, once the
    * deal has substance). `bindingPromises` opens binding promises to every
    * author (ruling 11).
    */
  def snapshot(state: ReadyGame, deal: DealState,
      bindingPromises: Boolean = false): DecisionQuery.Negotiate =
    DecisionQuery.Negotiate(deal.participants, deal.terms, deal.accepted,
      deal.participants.map(author => author ->
        bounds(state, author, deal.participants, bindingPromises)).toMap,
      if deal.hasSubstance && !deal.declined then
        deal.participants.filterNot(deal.accepted).toSet
      else Set.empty[PlayerId],
      heading = Some("Negotiation"))
```

Give the private `bounds` a last parameter `bindingPromises: Boolean`
and pass `bindingPromises = bindingPromises` as the last argument of its
`NegotiationBounds(...)`. Keep the rest of `bounds` as slice 1 left it.

- [ ] **Step 5: Build from the reach, rebuild from the facts**

In `NegotiationProcedure.scala`, add the import
`distantlands.gameplay.powerresolver.PowerUseRestriction`, and replace
`build`, `rebuild` and the head of `tree`:

```scala
  /** A fresh start: the reach is read once, under the walk's power-use
    * restrictions, and refused when it holds nobody. */
  def build(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts = NegotiationStartFacts.at(catalog, state, actor, restrictions)
      _ <- Either.cond(facts.candidates.nonEmpty, (),
        OathViolation.NegotiationUnavailable(
          "no other player is at your site or within reach"))
    yield tree(actor, facts)

  /** A resume rebuilds what the start built, from the facts it stored. */
  def rebuild(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts <- NegotiationStartFacts.read(state.game.current.walkerStartFacts)
    yield tree(actor, facts)

  /** What the start stores for every rebuild ([[NegotiationStartFacts]]). */
  def startFacts(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Vector[DecisionOptionRef] =
    NegotiationStartFacts.at(catalog, state, actor, restrictions).refs
```

Change `private def tree(state: ReadyGame, actor: PlayerId): Operation`
to `private def tree(actor: PlayerId, facts: NegotiationStartFacts):
Operation`, and inside it:

- `val candidates = NegotiationDeal.eligible(state, actor)` becomes
  `val candidates = facts.candidates`;
- every `NegotiationDeal.deal(ready, actor, pending)` becomes
  `NegotiationDeal.deal(ready, actor, candidates, pending)`;
- `NegotiationDeal.snapshot(ready, deal)` becomes
  `NegotiationDeal.snapshot(ready, deal,
  facts.bindingPromises(deal.participants))`;
- `NegotiationDeal.chosen(ready, actor, pending)` becomes
  `NegotiationDeal.chosen(candidates, pending)`.

In the object's doc comment, add after the tree diagram: "The start reads
the reach once, under the walk's power-use restrictions, and stores it as
walker start facts ([[NegotiationStartFacts]]); a rebuild reads them back."

- [ ] **Step 6: Give the Negotiation the walk's powers**

In `WalkerProcedureRegistry.scala`:

- Replace the `ActionRef.Negotiation -> Entry(...)` entry with
  `ActionRef.Negotiation -> negotiationEntry(WalkerPowers.empty),`, keeping
  its doc comment, and add to the comment: "Its start reads the walk's
  power-use restrictions, so `lookup` builds it from the walk's powers."
- Add beside `forcedWakeEntry`:

```scala
  /** Negotiation reads the power-use restrictions at its start: The
    * Tribunal and Traveling Negotiator reach no further than their users
    * may use them (catalog batch 12, ruling 24). A resume reads the start
    * facts and needs no powers. */
  private def negotiationEntry(powers: WalkerPowers): Entry = Entry(
    build = NegotiationProcedure.build(powers.useRestrictions),
    rebuild = NegotiationProcedure.rebuild,
    startFacts = NegotiationProcedure.startFacts(powers.useRestrictions))
```

- In `lookup`, before `case _ =>`, add
  `case ActionRef.Negotiation => Right(negotiationEntry(walkerPowers))`.
- Give `startFacts` a last parameter
  `walkerPowers: WalkerPowers = WalkerPowers.empty` and call
  `lookup(procedure, entries, PhasePowers.empty, walkerPowers)`.

In `OathRulesWalker.scala`:

- In `startNow`, pass the powers to the facts:
  `WalkerProcedureRegistry.startFacts(procedure, catalog, ready, actor,
  walkerPowers = powers)`.
- In `buildWalker`, add `ActionRef.Negotiation` to both alternative lists
  (`case _: ActionRef.UsePower | TriggeredProcedureRef.ForcedWake | ...`),
  so its start goes through the registry with `walkerPowers`.

Then check that no suite injects its own tree for the Negotiation, which
this routing would bypass:

Run: `git grep -n "Negotiation" -- src/test/scala/distantlands/application/WalkerDecisionQueryPowerSuite.scala src/test/scala/distantlands/gameplay/OathRulesWalkerPowerSuite.scala`
Expected: no output.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.NegotiationCitizenshipSuite distantlands.application.GameApplicationServiceReopenSuite distantlands.application.NegotiationDealProjectionSuite distantlands.gameplay.walker.WalkerProcedureRegistrySuite"`
Expected: PASS.

Then run every suite that drives a Negotiation:

Run: `./sbtw "testOnly *Negotiation* *RoleChanges* *GameLog*"`
Expected: PASS. The golden logs do not change: every existing script
negotiates at one site.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala \
  src/main/scala/distantlands/gameplay/OathRulesWalker.scala \
  src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala \
  src/test/scala/distantlands/application/GameApplicationServiceReopenSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): reach players at other sites, and open binding promises

The start reads the reach once under the walk's power-use restrictions
and stores the candidates and the Tribunal's users as walker start
facts. Rebuilds read them back. A deal holding a Tribunal user opens
binding promises to every author.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: The binding promise in the projection and the Game Log

**Files:**
- Modify: `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`
- Create: `src/test/resources/gamelog/negotiation-binding-promised.actor.log`
- Create: `src/test/resources/gamelog/negotiation-binding-promised.other.log`

**Interfaces:**
- Consumes: Task 2's binding flag; slice 1's `bindingPromisesOpen` on the
  editing projection and its `promisedBinding` line ("{0} made a binding
  promise to {1}: {2}").
- Produces: the script `NegotiationLogScripts.negotiationBindingPromised`
  and its stream name `"negotiation-binding-promised"`.

No production code changes in this task. It proves that slice 1's
binding-promise box and line work end to end once Task 2 opens them.

- [ ] **Step 1: Write the tests**

Add to `NegotiationDealProjectionSuite` (imports to add:
`distantlands.gameplay.actions.negotiation.TheTribunalCard`):

```scala
  test("with The Tribunal in reach, every author's editing opens binding " +
      "promises"):
    val state = parkedDeal(gathered.edifice(TheTribunalCard,
      EdificeSide.Intact, at = site), who = Vector(p2))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing
        .getOrElse(fail(s"${viewer.value} edits"))
      assert(editing.bindingPromisesOpen, viewer.value)
    }
```

Add to `NegotiationLogScripts`, after `negotiationPromised` (imports to
add: `distantlands.gameplay.actions.negotiation.TheTribunalCard`, and
`BrokenPeaksSite` beside `AncientCitySite`):

```scala
  /** p2 rules Broken Peaks, where The Tribunal stands, so p2 may use it and
    * the deal takes binding promises (catalog batch 12, ruling 11). p1, who
    * has no access, makes one. Nobody else is in reach, so the negotiators
    * decision is not asked. */
  def negotiationBindingPromised(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating(
      "negotiation-binding-promised", _.edifice(TheTribunalCard,
        EdificeSide.Intact, at = BrokenPeaksSite)
        .warbandsAt(BrokenPeaksSite, p2, 1))
    parked
      .parkedAfter(deal(actor, ProposeTerms(NegotiationTerms(bindingPromises =
        Vector("I will give you 2 favor next round.")))))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(deal(actor, AcceptDeal))
    Script("negotiation-binding-promised", service, actor)
```

In `LogScripts`, add `negotiationBindingPromised` to the
`NegotiationLogScripts` export, and to `named` after
`"negotiation-promised"`:

```scala
    "negotiation-binding-promised" -> (() => negotiationBindingPromised),
```

Add to `GameLogExchangeSuite`, beside slice 1's promise test, using the
same helpers (`partner`, `bystander`, `texts`, `format`, `lines`):

```scala
  test("an agreed binding promise is written once, naming its author and " +
      "the others"):
    val script = negotiationBindingPromised
    val expected = s"${playerLabel(script.actor)} made a binding promise " +
      s"to ${playerLabel(partner)}: “I will give you 2 favor next round.”"
    Vector(None, Some(script.actor), Some(partner), Some(bystander))
      .foreach { viewer =>
        val all = texts(format(script, viewer).filter(_.depth == 1))
        assertEquals(all.count(_ == expected), 1, s"$viewer: $all")
      }
    assertEquals(lines(script).last, expected, lines(script))
```

- [ ] **Step 2: Run the tests**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: PASS. If the script fails at a command, read the refusal: a
negotiators decision means a third player is in reach, which the table
must not allow.

- [ ] **Step 3: Write and review the golden logs**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Read both new files. Each must end with the binding-promise line after
the opening "Negotiated with" line, and must show no transfer line. Run
`git status --short src/test/resources/gamelog` and confirm that only the
two new files appear. If an existing golden changed, stop and report.

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala \
  src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/LogScripts.scala \
  src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala \
  src/test/resources/gamelog/negotiation-binding-promised.actor.log \
  src/test/resources/gamelog/negotiation-binding-promised.other.log
git commit -m "$(cat <<'EOF'
test(negotiation): a binding promise through The Tribunal, end to end

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Docs and verification

**Files:**
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md`

- [ ] **Step 1: Update the architecture doc**

In `docs/architecture/all-exile-negotiation.md`, as it reads after slice 1:

- Replace the negotiator-choice bullet with: "The negotiator choice is a
  `ChooseMany(1, n, ...)` over the candidates: the other players at the
  actor's site, and those Traveling Negotiator, The Tribunal or Bandit
  Tribunal lets the actor reach (`NegotiationReach`). Its window is
  `NegotiationEligibility`."
- Replace the start-refusal bullet with: "A start is refused when there
  is no candidate (`NegotiationUnavailable`). The start reads the reach
  once, under the walk's power-use restrictions (City Wall, Giant Python,
  Bandit Forest), and stores the candidates and The Tribunal's users as
  walker start facts (`NegotiationStartFacts`). Every rebuild reads them
  back. There is no first-game gate: the action is available to any role
  and any Foundation face."
- Add a bullet: "A deal opens binding promises to every author when one
  participant may use The Tribunal (intact)."
- In "Unsupported Negotiation rules", drop Traveling Negotiator and the
  Tribunal faces from the list.
- In "Deferred", replace "remote and private Negotiation" with
  "Whispering Stone's remote, private Negotiation".

- [ ] **Step 2: Mark the slice in the spec**

Set the spec's status line to "**Status:** designed 2026-11-09. Slices 1
and 2 landed." (or the next slice count if more have landed).

- [ ] **Step 3: Run the full verification**

Run each, in the background where it may exceed two minutes:

```bash
./sbtw clean coverage test coverageReport
```

Expected: every suite passes; statement coverage at or above 92%.

```bash
python3 scripts/check-architecture.py
```

Expected: no errors.

```bash
python3 scripts/check-markdown-links.py
```

Expected: no broken links.

No frontend code changed, so no frontend run and no browser check is
needed: slice 1's panel already shows the binding section whenever
`bindingPromisesOpen` is set, and Task 3 proves the projection sets it.

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/all-exile-negotiation.md \
  docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
docs(negotiation): eligibility reach and start facts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

## Self-review

- **Spec coverage.** Ruling 7: Task 1's Traveling Negotiator tests, Task 2's
  remote starts. Ruling 8: Task 1's Tribunal tests (site, ruled site,
  both ways). Ruling 9: Task 1's Bandit Tribunal test (both pawns, card on
  the map, ignores blockers). Ruling 11: Task 2's binding test, Task 3's
  projection and log. Rulings 24 and 25: Task 1's City Wall, Giant Python
  and Bandit Forest tests, Task 2's blocked start. Catalog: Task 1. Spec
  tests "a player at neither site nor holding the card stays out": Task 1's
  first test and the `p3` assertions.
- **Out of scope.** Consent asked from the Negotiation for a waivable
  refusal; "even into later games"; enforcing promises.
