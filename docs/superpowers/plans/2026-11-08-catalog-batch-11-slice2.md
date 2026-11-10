# Catalog Batch 11, Slice 2: Grand Mask Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Grand Mask (R05): while its holder is an Exile, during
their own turn, they rule every denizen (edifices included) at an Imperial
site except battle plans, and the Imperials rule none of those cards. The
sites stay Imperial.

**Architecture:** A new root seam, `gameplay/CardRule.scala`, answers who
rules a card at a site: the site's ruling player, unless the Mask works and
the card is no battle plan. `gameplay/GrandMask.scala` holds the card and
`GrandMaskRule` (who wears it, and which sites it covers). Every reader of
a card's ruler moves to `CardRule`; every reader of a site's ruler stays.
`PowerAccess` gains `rulingSites`, the sites whose cards an actor uses as
their ruler, which adds the Imperial sites for the wearer and takes them
from the Imperials. Slice 1's consent granters read the card ruler.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read "Rulings" 7 and 15 to 18, and "Slice 2: Card-level ruler".

## Prerequisite

This branch (`batch-11-slice-2`) was cut from `batch-11-slice-1`, whose
only commit past `0.2.0-alpha` is the slice 1 plan. Slice 2 builds on
slice 1's code: `BanditCrownRule`, `PowerAccess.usableSites` with the
Crown's sites, `SiteRulers.consentGranterAt` and `consentGranter`,
`RulerBans.refusal(ready, ...)` and `PayOrAsk.letOffBy`. Execute and merge
slice 1 into `0.2.0-alpha` first, then rebase this branch onto
`0.2.0-alpha` before Task 1. Every "before" snippet below that touches
those names is the code as slice 1 leaves it.

## Plan-time findings

Inventoried at `b59dbac4`, with slice 1's plan read for what it changes.

1. **Battle plans are known by their printed mark.** A face is a battle
   plan when one of its printed powers carries `ModifierMark.BattlePlan`.
   `CardCatalogSuite` ("printed modifier marks agree with the engine's
   modifiers and plans") pins the marks equal to the engine's `BattlePlan`
   powers, so the mark is the plan catalog. An edifice is judged by its
   face up now.
2. **`CardRule` reads `NewFoundations.catalog`.** A waiver's granter is a
   `ReadyGame => Option[PlayerId]`, and `RulerToll`, `PayOrAsk`,
   `RulerBans` and `SecretPolice` see no catalog. The engine runs one
   catalog (`ServerRuntime`, `FirstGameSetupFixture`, the `WalkerPowers`
   default), so `CardRule` reads its printed powers from it. The two test
   suites that build their own catalog read no ruler.
3. **When the Mask works.** Its holder must be an Exile (`Role.Exile`)
   and the active player, in Wake, Act or Rest, the turn as
   `Imperials.governor` reads it. Only a faceup Mask counts. An Imperial
   site is a site the Empire's warbands hold (`SiteRulers.imperialSites`).
   During that turn the governor is the Chancellor, so the cards the Mask
   takes are taken from the Chancellor.
4. **The card readers move to `CardRule`.** Each reads "this card's
   ruler", "X's ruler", "you rule X" or "cards ruled by":

   | Reader | Today | Becomes |
   |---|---|---|
   | `RuledCards.of` (Curfew, Dissent, Town Meeting, Wizard's Conclave, True Oath, Diplomat, Fabled Feast, Lost Tongue, `PlanContext`, Wild Mounts) | `rulerOf(site)` | `CardRule.rulerAt` per card |
   | `RulerToll.enemyRuler` (Curfew, Forced Labor) | `rulerOfCard` | `CardRule.rulerOf` |
   | `RulerBans.ruler` (Spell Breaker, Forest Council, City Wall) | `rulerOfCard` | `CardRule.rulerOf` |
   | `Jinx.rules` | `rulerOfCard` | `CardRule.rulerOf` |
   | `SecretPolice.blocked` | `rulerOfCard` | `CardRule.rulerOf` |
   | `WayStation.arriving` | `rulerOfCard` | `CardRule.rulerOf` |
   | `GraspingVines.charged` | `rulerOfCard` | `CardRule.rulerOf` |
   | `TollRoads.owed` | `rulerOfCard` | `CardRule.rulerOf` |
   | `BannerGuardian.protectedRuler` (Hall of Debate, Tome Guardians) | `rulerOf(site)` | `CardRule.rulerAt` |
   | `PatrolRule.blocked`, the patrols' ruler | `rulerOf(site)` | `CardRule.rulerAt` |
   | `ForbiddenForest.blocked` | `rulerOf(site)` | `CardRule.rulerAt` |
   | `BoilingLakeTravel.applicable` ("do not rule this card") | `rulerOf(site)` | `CardRule.rulerAt` |
   | `LeagueTreatyContribution.treaty` | `rulingPlayer(site)` | `CardRule.rulerAt` |
   | `HallOfMinisters.hallRuler` | `rulingPlayer(site)` | `CardRule.rulerAt` |
   | `SiteRulers.protectedAt` (Forgotten Vault, Oaken Fortress: "If ruled by Empire") | `SiteRule.ruler(site)` | `SiteRulers.protectedBy(site, card)` |
   | `PayOrAsk.granter`, `RulerBans.waiver` and `reason`, `SecretPolice.waiver` (slice 1: `consentGranter`) | site of the card | `CardRule.rulerOf` (Task 3) |
   | `PatrolRule.granter`, `ForbiddenForest.granter`, `HallOfMinisters.granter` (slice 1: `consentGranterAt`) | edifice site | `SiteRulers.consentGranterOf` (Task 3) |

   `SiteRulers.rulerOfCard` is deleted. After slice 1 moves the waivers
   to `consentGranter`, seven callers remain, and they read
   `CardRule.rulerOf`, which keeps its reading (a player or bandits only).
5. **The site readers stay.** Each reads "sites ruled by", "this site's
   ruler" or the site's forces:
   - The second half of Forced Labor (`:72`), Secret Police (`:64`), Toll
     Roads (`:117`), Grasping Vines (`:82`), Relic Patrols (`:81`, the
     Recover's site) and Hall of Ministers (`siteRuler`, the discard's
     site).
   - Hidden Passages (ruling 15), Key to the City (ruling 16) and Hospital
     (ruling 17).
   - `TravelSitePowers` (site cards' own powers), `MinorActionProcedures`
     (the muster check), `CampaignSetup.defenderAt`, `ChronicleTasks`,
     `ForgeRules`, `EconomyTree`, the projector, `RuinedSiteSetupRules`,
     `GreatMarketRules` and `SiteRulers.imperialSites` (Imperial Seal).
   - `PowerAccess.ruledSites` and its counters: Royal Tax, Garrison, Royal
     Ambitions, Baron, Plague Engines, Messenger, Captains, Vow of Union and
     Ancient Bloodline ("sites you rule"). Quartermaster ("If you rule this
     card") was listed here by mistake; the Tasks 1-2 review caught it, and
     it reads `CardRule.rulerOf`.
   - `PlanContext.sitesRuled`: battle plans stay the Empire's.
   - Bandit Forest's guard ("cards ruled by bandits"): a bandit site is
     never Imperial, so the card and site readings agree.
6. **Access.** `usableSites` becomes `rulingSites ++ presentSite`.
   `rulingSites` is `ruledSites`, less the masked sites, plus the Crown's
   bandit sites and the wearer's Imperial sites. The Chancellor keeps the
   cards at their pawn's site (ruling 18). Battle plans are reached
   through `PlanContext`, not through access, so the Imperial sites' plans
   stay the Chancellor's. `ActingSite.pickedAtPawnSite` asks whether the
   pawn site's cards are used as a ruler, so it reads `rulingSites`
   instead of `ruledSites`; this also fixes the Crown's case, which slice 1
   left reading `ruledSites`.
7. **Who pays a toll to the wearer.** On the wearer's turn only the wearer
   acts, and they owe nothing to themselves. A toll to the wearer is owed
   by another player acting in that turn. The suite pins it at the seam:
   `RulerToll.enemyRuler` names the wearer for the Chancellor, and
   `PayOrAsk.letOffBy` names the wearer as the one who may let them off.
8. **Registration.** As Bandit Crown in slice 1, Grand Mask is a
   `ContributingPower` with no contributions, registered in
   `WalkerPowerCatalog`, with its card in the root seam file. Delete
   `relic.grand-mask` from `unimplemented.txt`.
9. **Tests that use the Mask as a stand-in relic.** `NegotiationDealSuite`,
   `NegotiationProcedureSuite`, `NegotiationDealProjectionSuite`,
   `ChronicleSunSuite`, `RelicHunterSuite` and `CampaignLogScripts` import
   `GrandMaskCard` from `catalog.holding`; their import moves. The
   Negotiation suites hold it facedown, and the others leave it at a site
   until Relic Hunter takes it. Nothing should change; the full run in
   Task 4 confirms it.

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-2`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it. Where
  a step drops the last use of `SiteRulers` from a file, drop it from the
  import too.
- The sites stay Imperial (ruling 7). `SiteRule`, `SiteRulers.rulerOf`,
  `ruledSites`, `imperialSites` and `CampaignSetup.defenderAt` do not
  change.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.
- Code comments and docs are plain prose. Log lines name only non-actors
  (memory: "Log lines omit actor"). This slice adds no log line.

---

### Task 1: The card ruler and the wearer's access

**Files:**
- Create: `src/main/scala/distantlands/gameplay/GrandMask.scala`
- Create: `src/main/scala/distantlands/gameplay/CardRule.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/poweruse/GrandMask.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingRelics.scala` (remove `GrandMaskCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import)
- Modify: `src/main/scala/distantlands/gameplay/PowerAccess.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/ActingSite.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify (import only): the six test files of finding 9
- Test: `src/test/scala/distantlands/gameplay/GrandMaskSuite.scala`

**Interfaces:**
- Consumes: `BanditCrownRule.sitesFor` (slice 1),
  `PowerAccess.faceupRelicHolder`, `SiteRulers.imperialSites`.
- Produces:
  - `GrandMaskRule.wearer(ready: ReadyGame): Option[PlayerId]`
  - `GrandMaskRule.maskingAt(ready: ReadyGame, site: SiteId): Option[PlayerId]`
  - `GrandMaskRule.maskedSites(ready: ReadyGame): Set[SiteId]`
  - `GrandMaskRule.sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId]`
  - `CardRule.rulerAt(ready: ReadyGame, site: SiteId, card: CardId): Option[SiteRuler]`
  - `CardRule.rulerOf(ready: ReadyGame, card: DenizenId): Option[SiteRuler]`
  - `PowerAccess.rulingSites(ready: ReadyGame, actor: PlayerId): Set[SiteId]`

- [ ] **Step 1: Move the card and write the rule**

Delete the `GrandMaskCard` object from `catalog/holding/HoldingRelics.scala`.
Create `src/main/scala/distantlands/gameplay/GrandMask.scala`:

```scala
package distantlands.gameplay

import distantlands.catalog.{PrintedPower, Relic}
import distantlands.model._

object GrandMaskCard extends Relic(RelicId("R05"), "Grand Mask", value = 36, defense = 1):
  val power = PrintedPower(PowerId("relic.grand-mask"),
    persistent = true, cost = Cost.free,
    text = "If you're an Exile, during your turn you rule cards except " +
      "battle plans at Imperial sites, and Imperial players do not.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Grand Mask (R05): while its holder is an Exile, during their own turn,
  * they rule every denizen (edifices included) at an Imperial site except
  * battle plans, and the Imperials rule none of those cards (catalog batch
  * 11, ruling 7). The sites stay Imperial (the card's FAQ). `CardRule`
  * reads this for every card ruler, and `PowerAccess.rulingSites` for
  * access. Only a faceup Mask counts.
  */
private[gameplay] object GrandMaskRule:
  /** The player who holds the Mask faceup, if any. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    PowerAccess.faceupRelicHolder(ready, GrandMaskCard.id)

  /** The holder while the Mask works: an Exile who is the active player,
    * in their Wake, Act or Rest (the turn as `Imperials.governor` reads
    * it). */
  def wearer(ready: ReadyGame): Option[PlayerId] =
    val turn = ready.game.current.turn
    val inTurn = turn.phase == Phase.Wake || turn.phase == Phase.Act ||
      turn.phase == Phase.Rest
    holder(ready).filter(player => inTurn && turn.activePlayer == player &&
      Imperials.role(ready, player).contains(Role.Exile))

  /** The wearer, when `site` is an Imperial site. */
  def maskingAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    wearer(ready).filter(_ => ready.game.current.map.sites.get(site)
      .exists(state => SiteRule.ruler(state.forces,
        ready.game.current.players) == Right(SiteRuler.Empire)))

  /** The Imperial sites while the Mask works, else none. */
  def maskedSites(ready: ReadyGame): Set[SiteId] =
    if wearer(ready).isEmpty then Set.empty
    else SiteRulers.imperialSites(ready).toSet

  /** The sites whose cards `player` rules through the Mask: the Imperial
    * sites while they wear it, else none. */
  def sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId] =
    if wearer(ready).contains(player) then maskedSites(ready)
    else Set.empty
```

Create `src/main/scala/distantlands/gameplay/CardRule.scala`:

```scala
package distantlands.gameplay

import distantlands.catalog.{ModifierMark, PrintedPower}
import distantlands.gameplay.cards.NewFoundations
import distantlands.model._

/** Who rules a card at a site (catalog batch 11, slice 2): the site's
  * ruling player (`SiteRule.rulingPlayer`), except that while the Grand
  * Mask works its wearer rules every card at an Imperial site but a battle
  * plan (`GrandMaskRule`). Every reading of "this card's ruler", "you rule
  * this card" or "cards ruled by" goes through here; a reading of the site
  * itself (Supremacy, Campaign defenders, the Chronicle, "sites ruled by")
  * reads `SiteRulers.rulerOf` or `SiteRule`.
  *
  * A face is a battle plan when a power it prints carries the battle-plan
  * mark, which `CardCatalogSuite` holds equal to the plan catalog. The
  * printed powers come from `NewFoundations.catalog`, the only catalog the
  * engine runs, because a waiver's granter sees the state alone.
  */
private[gameplay] object CardRule:
  /** Who rules `card`, which stands at `site`. Like
    * `SiteRule.rulingPlayer`, it may be the Empire or `Unruled`. */
  def rulerAt(ready: ReadyGame, site: SiteId, card: CardId)
      : Option[SiteRuler] =
    val masked = for
      wearer <- GrandMaskRule.maskingAt(ready, site)
      state <- ready.game.current.map.sites.get(site)
        .flatMap(_.denizens.find(_.id == card))
      if !battlePlan(state)
    yield SiteRuler.Player(wearer)
    masked.orElse(SiteRule.rulingPlayer(ready, site))

  /** Who rules the faceup denizen `card` at a site, when that is a player
    * or bandits. The Empire resolves to its governor; it stays the Empire
    * only with no Chancellor, which rules nothing. */
  def rulerOf(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    ready.game.current.faceUpDenizenSite(card)
      .flatMap(rulerAt(ready, _, card)).filter:
      case SiteRuler.Player(_) | SiteRuler.Bandits => true
      case _ => false

  private def battlePlan(state: SiteDenizenState): Boolean =
    printed(state).exists(_.modifier.exists {
      case _: ModifierMark.BattlePlan => true
      case _ => false
    })

  /** The powers `state`'s face up now prints. */
  private def printed(state: SiteDenizenState): Vector[PrintedPower] =
    val catalog = NewFoundations.catalog
    state match
      case DenizenState(id, _, _) =>
        catalog.denizen(id).toVector.flatMap(_.powers)
      case EdificeState(id, side, _) =>
        catalog.edifice(id).toVector.flatMap(edifice =>
          (if side == EdificeSide.Intact then edifice.intact
           else edifice.ruined).powers)
```

In `NewFoundations.scala`, add `GrandMaskCard` to the
`distantlands.gameplay.{...}` import (alphabetical: `ActingTroupeCard,
BanditCrownCard, GrandMaskCard, MarriageCard, PledgeToDiscordCard,
SpitefulMirrorCard`). In the six files of finding 9, replace
`distantlands.catalog.holding.GrandMaskCard` with
`distantlands.gameplay.GrandMaskCard`. In `NegotiationProcedureSuite`, split
the brace import so `CouncilArbiterCard` keeps `catalog.holding`.

- [ ] **Step 2: Register the power**

Create `src/main/scala/distantlands/gameplay/powers/poweruse/GrandMask.scala`:

```scala
package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Grand Mask (R05): "If you're an Exile, during your turn you rule cards
  * except battle plans at Imperial sites, and Imperial players do not."
  *
  * The rule is `GrandMaskRule`, which `CardRule` and `PowerAccess` read.
  * This power hooks no window. It is registered so the card counts as
  * implemented. It writes no line.
  */
case object GrandMask extends ContributingPower:
  val id: PowerId = GrandMaskCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `WalkerPowerCatalog.scala`, change
`Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown) ++` to
`Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown,
GrandMask) ++`. Delete the line `relic.grand-mask` from
`src/test/resources/powers/unimplemented.txt`.

- [ ] **Step 3: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/GrandMaskSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Grand Mask (R05): its wearer, an Exile in their own turn, rules the
  * cards at Imperial sites except battle plans, and the Imperials do not;
  * the sites stay Imperial (catalog batch 11, rulings 7 and 18). */
class GrandMaskSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id
  private val wrestlers = WrestlersCard.id

  /** p2 is the Chancellor and p1 a Citizen; the Empire holds Ancient City,
    * where Roving Terror and Wrestlers (a battle plan) stand. p3, an
    * Exile, and p2 both stand at Deep Woods. It is p3's Act. */
  private def imperialCardsAwayFromP3: Table =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act).pawn(p3, at = woods)
      .pawn(p2, at = woods).warbandsAt(home, p2, 2)
      .denizen(terror, at = home).denizen(wrestlers, at = home)

  private def p3WearsTheMask: Table =
    imperialCardsAwayFromP3.relic(p3, GrandMaskCard)

  private def terrorAccess(ready: ReadyGame, player: PlayerId): Boolean =
    PowerAccess.accessible(RuleSourceRef.SiteCard(home, terror),
      RuleSourceFace.FaceUp, ready, player)

  test("the wearer rules an Imperial site's card, and the Chancellor does " +
      "not"):
    val ready = p3WearsTheMask.ready
    assertEquals(CardRule.rulerOf(ready, terror),
      Some(SiteRuler.Player(p3)))
    assertEquals(CardRule.rulerAt(ready, home, terror),
      Some(SiteRuler.Player(p3)))

  test("a battle plan stays the Empire's"):
    assertEquals(CardRule.rulerOf(p3WearsTheMask.ready, wrestlers),
      Some(SiteRuler.Player(p2)))

  test("the sites stay Imperial, and the wearer rules no site"):
    val ready = p3WearsTheMask.ready
    assertEquals(SiteRule.rulingPlayer(ready, home),
      Some(SiteRuler.Player(p2)))
    assert(SiteRulers.imperialSites(ready).contains(home))
    assertEquals(PowerAccess.ruledSites(ready, p3), Set.empty[SiteId])
    assert(PowerAccess.ruledSites(ready, p2).contains(home))

  test("the wearer uses a masked card from the phase menu"):
    assert(MenuPlay.availableInMenu(catalog, p3WearsTheMask.ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))))
    assert(PowerAccess.rulingSites(p3WearsTheMask.ready, p3).contains(home))

  test("the Chancellor loses ruler access, but keeps their pawn's site"):
    val ready = p3WearsTheMask.ready
    assert(!terrorAccess(ready, p2))
    assert(!PowerAccess.rulingSites(ready, p2).contains(home))
    assert(terrorAccess(Table.from(ready).pawn(p2, at = home).ready, p2))

  test("nothing changes on another player's turn, for a Citizen holder, " +
      "or with the Mask facedown"):
    val othersTurn = p3WearsTheMask.turn(p1, Phase.Act).ready
    val citizenHolder = imperialCardsAwayFromP3.relic(p1, GrandMaskCard)
      .turn(p1, Phase.Act).ready
    val facedown = imperialCardsAwayFromP3
      .relic(p3, GrandMaskCard, facedown = true).ready
    Vector(othersTurn, citizenHolder, facedown).foreach { ready =>
      assertEquals(CardRule.rulerOf(ready, terror),
        SiteRule.rulingPlayer(ready, home))
      assertEquals(GrandMaskRule.maskedSites(ready), Set.empty[SiteId])
    }
    assert(!terrorAccess(facedown, p3))

  test("it works through the wearer's Wake and Rest"):
    Vector(Phase.Wake, Phase.Rest).foreach { phase =>
      assertEquals(CardRule.rulerOf(p3WearsTheMask.turn(p3, phase).ready,
        terror), Some(SiteRuler.Player(p3)))
    }

  test("a site the Empire does not hold is read as before"):
    // A Citizen's warbands are Imperial, so bandits hold the other site.
    val ready = Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .bandits(peaks, 1).denizen(terror, at = peaks)
      .relic(p3, GrandMaskCard).ready
    assertEquals(GrandMaskRule.maskingAt(ready, peaks), None)
    assertEquals(CardRule.rulerAt(ready, peaks, terror),
      Some(SiteRuler.Bandits))
```

`Table.from` exists (`Table.scala:512`); `turn` replaces the turn's
player and phase. `Table.start` may already hold Imperial sites and
denizens of its own, so the tests assert membership, not whole sets.

- [ ] **Step 4: Run the suite to see it fail**

```bash
./sbtw "testOnly distantlands.gameplay.GrandMaskSuite"
```

Expected: it fails to compile on `PowerAccess.rulingSites`, which Step 5
adds. The `CardRule` tests already hold once it compiles; the menu and
access tests are the ones Step 5 turns green.

- [ ] **Step 5: Widen access**

In `PowerAccess.scala` (`GrandMaskRule` is in the same package, so no
import is needed), replace

```scala
  private def usableSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ruledSites(ready, actor) ++ presentSite(ready, actor) ++
      BanditCrownRule.sitesFor(ready, actor)
```

with

```scala
  /** The sites whose cards `actor` uses as their ruler would: the sites
    * they rule, less those the Grand Mask takes from the Imperials during
    * its wearer's turn; the bandit sites for the Bandit Crown's holder; and
    * the Imperial sites for the Mask's wearer. A count of ruled sites reads
    * `ruledSites`, which neither relic changes. */
  def rulingSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ruledSites(ready, actor) -- GrandMaskRule.maskedSites(ready) ++
      BanditCrownRule.sitesFor(ready, actor) ++
      GrandMaskRule.sitesFor(ready, actor)

  private def usableSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    rulingSites(ready, actor) ++ presentSite(ready, actor)
```

In the object's doc comment, after slice 1's "for the Bandit Crown's
holder, cards at sites bandits rule (`BanditCrownRule`)", add "; for the
Grand Mask's wearer, cards at Imperial sites, which the Imperials then
reach only from their pawn's site (`GrandMaskRule`)". In `locate`'s doc
comment, change "a site the actor rules, a bandit site for the Crown's
holder, or the one they act from" to "a site whose cards the actor uses as
a ruler ([[rulingSites]]), or the one they act from".

In `ActingSite.scala`, in `pickedAtPawnSite`, change
`.filterNot(PowerAccess.ruledSites(ready, ctx.actor))` to
`.filterNot(PowerAccess.rulingSites(ready, ctx.actor))`, and its doc
comment "which the actor does not rule" to "whose cards the actor does not
use as a ruler (`PowerAccess.rulingSites`)".

- [ ] **Step 6: Run the suites**

```bash
./sbtw "testOnly distantlands.gameplay.GrandMaskSuite distantlands.gameplay.powers.poweruse.BanditCrownSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.powers.PowerImplementationStatusSuite distantlands.gameplay.powers.PowerCatalogUniquenessSuite distantlands.gameplay.powers.economy.* distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.chronicle.ChronicleSunSuite distantlands.gameplay.powers.campaign.RelicHunterSuite"
```

Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/GrandMask.scala src/main/scala/distantlands/gameplay/CardRule.scala src/main/scala/distantlands/gameplay/powers/poweruse/GrandMask.scala src/main/scala/distantlands/catalog/holding/HoldingRelics.scala src/main/scala/distantlands/gameplay/cards/NewFoundations.scala src/main/scala/distantlands/gameplay/PowerAccess.scala src/main/scala/distantlands/gameplay/powers/economy/ActingSite.scala src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala src/test/resources/powers/unimplemented.txt src/test/scala/distantlands/gameplay/GrandMaskSuite.scala src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala src/test/scala/distantlands/application/gamelog/CampaignLogScripts.scala src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleSunSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/RelicHunterSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): the Grand Mask's wearer rules an Imperial site's cards

CardRule answers who rules a card at a site: the site's ruling player,
or the Mask's wearer for a card at an Imperial site that is no battle
plan, during the wearer's own turn as an Exile. The wearer reaches those
cards as a ruler, and the Imperials keep only their pawn's site. The
sites stay Imperial.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: The card readers move to `CardRule`

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/SiteRulers.scala` (delete `rulerOfCard`, `protectedAt` becomes `protectedBy`)
- Modify: `src/main/scala/distantlands/gameplay/powers/RuledCards.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/RulerToll.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/poweruse/RulerBans.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/dice/Jinx.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/cardplay/SecretPolice.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/WayStation.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/GraspingVines.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/TollRoads.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/targeting/BannerGuardians.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/BoilingLake.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/rest/LeagueTreatyContribution.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/targeting/ForgottenVault.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/targeting/FortressRules.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/GrandMaskRulesSuite.scala`

**Interfaces:**
- Consumes: `CardRule.rulerAt`, `CardRule.rulerOf`, `GrandMaskRule.maskingAt`
  (Task 1); `PayOrAsk.letOffBy` and `RulerBans.refusal(ready, ...)`
  (slice 1).
- Produces: `SiteRulers.protectedBy(ready: ReadyGame, site: SiteId, card: CardId): Set[PlayerId]`,
  replacing `protectedAt`. `SiteRulers.rulerOfCard` no longer exists.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/GrandMaskRulesSuite.scala`:

```scala
package distantlands.gameplay.powers

import distantlands.gameplay.{GrandMaskCard, OathRules, SiteRulers}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powerresolver.PowerUse
import distantlands.gameplay.powers.action.RovingTerrorCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.dice.{Jinx, JinxCard}
import distantlands.gameplay.powers.economy.{Curfew, CurfewCard}
import distantlands.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import distantlands.gameplay.powers.targeting.ForgottenVaultCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{EconomyActions, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** The rules that read a card's ruler, under the Grand Mask (catalog batch
  * 11, ruling 7): during the wearer's turn they read the wearer for every
  * card at an Imperial site but a battle plan. */
class GrandMaskRulesSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val terror = RovingTerrorCard.id
  private val wrestlers = WrestlersCard.id
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))

  /** p2 is the Chancellor and p1 a Citizen, with no advisers. The Empire
    * holds Ancient City (Roving Terror, and Wrestlers, an Order battle
    * plan) and Broken Peaks (Curfew, Spell Breaker). p3, an Exile, stands
    * at Ancient City with the Mask `facedown` or faceup, beside the Beast
    * bank's 3 favor. It is p3's Act. */
  private def maskedEmpire(facedown: Boolean = false): Table =
    Table.start.imperial(p2, p1).withoutAdvisers(p2).withoutAdvisers(p3)
      .turn(p3, Phase.Act).pawn(p3, at = home)
      .warbandsAt(home, p2, 2).warbandsAt(peaks, p2, 1)
      .denizen(terror, at = home).denizen(wrestlers, at = home)
      .denizen(CurfewCard.id, at = peaks)
      .denizen(SpellBreakerCard.id, at = peaks)
      .bankFavor(Suit.Beast, 3)
      .relic(p3, GrandMaskCard, facedown = facedown)

  private def secretUse(by: PlayerId): PowerUse = PowerUse(by,
    PowerId("denizen.roving-terror"), Some(terror), persistent = false,
    costsSecret = true)

  test("the wearer trades with a masked card free of Curfew"):
    val ready = maskedEmpire().ready
    val done = EconomyActions.trade(rules, ready, p3, Vector.empty, "favor",
      DecisionOptionRef.Denizen(terror))
    assertEquals(NoteText.said(Curfew.id, Curfew.noteKeys,
      done.journal.events), Vector.empty)
    // With the Mask facedown, Curfew is the Chancellor's and p3 owes it.
    assertEquals(RulerToll.enemyRuler(maskedEmpire(facedown = true).ready,
      CurfewCard.id, p3), Some(SiteRuler.Player(p2)))

  test("a toll owed under the Mask is owed to the wearer"):
    val ready = maskedEmpire().ready
    assertEquals(RulerToll.enemyRuler(ready, CurfewCard.id, p2),
      Some(SiteRuler.Player(p3)))
    assertEquals(RulerToll.enemyRuler(ready, CurfewCard.id, p3), None)
    assertEquals(PayOrAsk.letOffBy(ready, SiteRuler.Player(p3), p2),
      Some(p3))

  test("Spell Breaker's ban turns on the wearer's enemies"):
    val ready = maskedEmpire().ready
    assertEquals(SpellBreaker.refusal(catalog, ready, secretUse(p3)), None)
    val refusal = SpellBreaker.refusal(catalog, ready, secretUse(p2))
    assertEquals(refusal.map(_.power), Some(SpellBreaker.id))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter),
      Some(p3))
    assert(SpellBreaker.refusal(catalog, maskedEmpire(facedown = true).ready,
      secretUse(p3)).nonEmpty)

  test("the wearer's ruled cards count the masked ones, and the " +
      "Chancellor keeps the battle plan"):
    def order(ready: ReadyGame, player: PlayerId): Vector[CardId] =
      RuledCards.of(catalog, ready, SiteRuler.Player(player), Suit.Order)
    val ready = maskedEmpire().ready
    assert(order(ready, p3).contains(CurfewCard.id))
    assert(!order(ready, p3).contains(wrestlers))
    assert(order(ready, p2).contains(wrestlers))
    assert(!order(ready, p2).contains(CurfewCard.id))
    assert(RuledCards.of(catalog, ready, SiteRuler.Player(p3), Suit.Beast)
      .contains(terror))
    val bare = maskedEmpire(facedown = true).ready
    assert(order(bare, p2).contains(CurfewCard.id))
    assert(!order(bare, p3).contains(CurfewCard.id))

  test("the wearer rules a masked Jinx"):
    val ready = Table.from(maskedEmpire().ready)
      .denizen(JinxCard.id, at = peaks).ready
    assert(Jinx.rules(ready, p3))
    assert(!Jinx.rules(ready, p2))

  test("an Empire's protection protects the wearer alone"):
    val ready = Table.from(maskedEmpire().ready)
      .denizen(ForgottenVaultCard.id, at = home).ready
    assertEquals(SiteRulers.protectedBy(ready, home, ForgottenVaultCard.id),
      Set(p3))
    val bare = Table.from(maskedEmpire(facedown = true).ready)
      .denizen(ForgottenVaultCard.id, at = home).ready
    assertEquals(SiteRulers.protectedBy(bare, home, ForgottenVaultCard.id),
      Set(p1, p2))
```

`NoteText.said(power, keys, events)` is the three-argument form
`CurfewSuite` uses. `Table.start` may hold denizens of its own at other
sites, so the counts assert membership, not whole vectors.

- [ ] **Step 2: Run the suite to see it fail**

```bash
./sbtw "testOnly distantlands.gameplay.powers.GrandMaskRulesSuite"
```

Expected: it fails to compile on `SiteRulers.protectedBy`. Comment that
test out to see the rest fail: the trade parks on Curfew's step, the
tolls name p2, Spell Breaker refuses p3, the counts put Curfew with p2,
and Jinx is p2's.

- [ ] **Step 3: Delete `rulerOfCard`, and read the card in `SiteRulers`**

In `SiteRulers.scala`, delete `rulerOfCard` and its doc comment. Replace
`protectedAt` with

```scala
  /** The players a protection printed on `card` at `site` covers, for a
    * card that says "If ruled by Empire, all Imperials have this power"
    * (Forgotten Vault, Oaken Fortress): the Grand Mask's wearer alone
    * while they rule the card (`CardRule.rulerAt`); else every Imperial
    * player while the Empire holds the site, or the player who rules it. */
  def protectedBy(ready: ReadyGame, site: SiteId, card: CardId)
      : Set[PlayerId] =
    CardRule.rulerAt(ready, site, card) match
      case Some(SiteRuler.Player(wearer))
          if GrandMaskRule.maskingAt(ready, site).contains(wearer) =>
        Set(wearer)
      case _ => ready.game.current.map.sites.get(site).flatMap(state =>
        SiteRule.ruler(state.forces, ready.game.current.players).toOption)
        match
          case Some(SiteRuler.Empire) => Imperials.players(ready).toSet
          case Some(SiteRuler.Player(player)) => Set(player)
          case _ => Set.empty
```

In the object's doc comment, change "Who rules
a site, and where a rule card stands, for the powers whose reach is "the
sites this card's ruler rules"" to "Who rules a site, for the powers whose
reach is "the sites this card's ruler rules"; a card's own ruler is
`CardRule`'s".

In `ForgottenVault.scala:63-64`, change

```scala
    ctx.state.game.current.faceUpDenizenSite(cardId)
      .fold(Set.empty[PlayerId])(SiteRulers.protectedAt(ctx.state, _))
```

to

```scala
    ctx.state.game.current.faceUpDenizenSite(cardId)
      .fold(Set.empty[PlayerId])(SiteRulers.protectedBy(ctx.state, _,
        cardId))
```

In `FortressRules.scala:183`, change
`SiteRulers.protectedAt(ready, site).contains(defender))` to
`SiteRulers.protectedBy(ready, site, fortress).contains(defender))`.

- [ ] **Step 4: Point the card readers at `CardRule`**

Each change replaces the reading of a card's ruler. Add `CardRule` to the
file's `distantlands.gameplay` import, and drop `SiteRulers` where
`-Werror` reports it unused. The imports should end as the table says.

| File | Change | `distantlands.gameplay` import |
|---|---|---|
| `RuledCards.scala` | see below | `{CardRule, EffectiveSuit}` |
| `RulerToll.scala` | `enemyRuler`: `SiteRulers.rulerOfCard(ready, card)` to `CardRule.rulerOf(ready, card)` | `{CardRule, NoteSupport}` |
| `RulerBans.scala` | `ruler`: `SiteRulers.rulerOfCard(ready, card)` to `CardRule.rulerOf(ready, card)` | `{CardRule, SiteRulers}` |
| `Jinx.scala` | `rules`: `SiteRulers.rulerOfCard(ready, card)` to `CardRule.rulerOf(ready, card)` | `CardRule` |
| `SecretPolice.scala` | `blocked`: `SiteRulers.rulerOfCard(ctx.state, cardId)` to `CardRule.rulerOf(ctx.state, cardId)`; the pawn site's `rulerOf` stays | `{CardRule, SiteRulers}` |
| `WayStation.scala` | `arriving`: `SiteRulers.rulerOfCard(ctx.state, cardId)` to `CardRule.rulerOf(ctx.state, cardId)` | `CardRule` |
| `GraspingVines.scala` | `charged`: `SiteRulers.rulerOfCard(ctx.state, cardId)` to `CardRule.rulerOf(ctx.state, cardId)`; `route.source`'s `rulerOf` stays | `{CardRule, NoteSupport, SiteRulers}` |
| `TollRoads.scala` | `owed`: `SiteRulers.rulerOfCard(ready, TollRoadsCard.id)` to `CardRule.rulerOf(ready, TollRoadsCard.id)`; the destination's `rulerOf` stays | `{CardRule, NoteSupport, SiteRulers}` |
| `BannerGuardians.scala` | `protectedRuler`: `SiteRulers.rulerOf(ctx.state, site)` to `CardRule.rulerAt(ctx.state, site, edifice)` | `CardRule` |
| `RelicPatrols.scala` | `blocked`: `patrolsRuler <- SiteRulers.rulerOf(ctx.state, patrolsSite)` to `patrolsRuler <- CardRule.rulerAt(ctx.state, patrolsSite, edifice)`; `recoverSite`'s `rulerOf` stays | `{CardRule, SiteRulers}` |
| `ForbiddenForest.scala` | `blocked`: `ruler <- SiteRulers.rulerOf(ctx.state, site)` to `ruler <- CardRule.rulerAt(ctx.state, site, ForbiddenForestCard.id)` | `{CardRule, EffectiveSuit, SiteRulers}` |
| `BoilingLake.scala` | `applicable`: `!SiteRulers.rulerOf(ctx.state, site)` to `!CardRule.rulerAt(ctx.state, site, edifice)` | `CardRule` |
| `LeagueTreatyContribution.scala` | `treaty`: `ruler <- SiteRule.rulingPlayer(ready, site)` to `ruler <- CardRule.rulerAt(ready, site, cardId)` | add `import distantlands.gameplay.CardRule` |
| `HallOfMinisters.scala` | `hallRuler`: `.flatMap(siteRuler(ready, _))` to `.flatMap(CardRule.rulerAt(ready, _, HallOfMinistersCard.id))`; `siteRuler` stays for the discard's site | `{CardRule, SiteRulers}` |

In `BannerGuardians.scala` and `BoilingLake.scala`, `edifice` is the
power's `EdificeId` field (`BannerGuardian.edifice`, the Lake's
`edifice`); confirm the name in the file. In `RuledCards.scala`, replace

```scala
    val atSites: Vector[CardId] = current.map.inPlay
      .filter(site => SiteRulers.rulerOf(ready, site).contains(ruler))
      .flatMap(current.map.sites.get).flatMap(_.denizens).collect {
        case DenizenState(held, Orientation.FaceUp, _) => held
        case edifice: EdificeState => edifice.id
      }
```

with

```scala
    val atSites: Vector[CardId] = current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).toVector.flatMap(_.denizens).collect {
        case DenizenState(held, Orientation.FaceUp, _) => held
        case edifice: EdificeState => edifice.id
      }.filter(card => CardRule.rulerAt(ready, site, card).contains(ruler)))
```

and in its doc comment change "the denizens and edifices at the sites the
ruler rules. Bandits hold no advisers; they rule the cards at the sites
they rule, as `SiteRulers.rulerOfCard` treats them." to "the denizens and
edifices it rules at sites (`CardRule`), which the Grand Mask gives its
wearer. Bandits hold no advisers; they rule the cards at the sites they
rule."

Update the doc comments that name `rulerOfCard` or "the ruler of the
site holding it": `RulerToll`'s object comment ("The rule card's ruler is
the ruler of the site holding it faceup" becomes "The rule card's ruler
is `CardRule.rulerOf`'s") and `enemyRuler`'s ("The ruler of the site
holding `card` faceup" becomes "The ruler of `card`"); `RulerBans`'s ("The
ruler is the ruler of the card's site, when that is a player or bandits"
becomes "The ruler is the card's ruler (`CardRule.rulerOf`), when that is
a player or bandits"). Grep for any comment left:

```bash
grep -rn 'rulerOfCard\|protectedAt' src/main/scala
```

Expected: no output.

- [ ] **Step 5: Run the suites**

```bash
./sbtw "testOnly distantlands.gameplay.powers.GrandMaskRulesSuite distantlands.gameplay.GrandMaskSuite distantlands.gameplay.powers.economy.* distantlands.gameplay.powers.poweruse.* distantlands.gameplay.powers.travel.* distantlands.gameplay.powers.targeting.* distantlands.gameplay.powers.recover.* distantlands.gameplay.powers.dice.* distantlands.gameplay.powers.cardplay.* distantlands.gameplay.powers.rest.* distantlands.gameplay.powers.whenplayed.* distantlands.gameplay.powers.search.* distantlands.gameplay.operations.*"
```

Expected: all pass. Without a faceup Mask, `CardRule.rulerAt` is
`SiteRule.rulingPlayer`, so no existing suite changes.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/SiteRulers.scala src/main/scala/distantlands/gameplay/powers/RuledCards.scala src/main/scala/distantlands/gameplay/powers/RulerToll.scala src/main/scala/distantlands/gameplay/powers/poweruse/RulerBans.scala src/main/scala/distantlands/gameplay/powers/dice/Jinx.scala src/main/scala/distantlands/gameplay/powers/cardplay/SecretPolice.scala src/main/scala/distantlands/gameplay/powers/travel/WayStation.scala src/main/scala/distantlands/gameplay/powers/travel/GraspingVines.scala src/main/scala/distantlands/gameplay/powers/travel/TollRoads.scala src/main/scala/distantlands/gameplay/powers/targeting/BannerGuardians.scala src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala src/main/scala/distantlands/gameplay/powers/travel/BoilingLake.scala src/main/scala/distantlands/gameplay/powers/rest/LeagueTreatyContribution.scala src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala src/main/scala/distantlands/gameplay/powers/targeting/ForgottenVault.scala src/main/scala/distantlands/gameplay/powers/targeting/FortressRules.scala src/test/scala/distantlands/gameplay/powers/GrandMaskRulesSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): read a card's ruler through CardRule

Every rule that names a card's ruler, the cards a ruler rules, or "you
rule this card" now reads CardRule, so during the Grand Mask's turn its
wearer is the ruler of the masked cards: Curfew's and the other tolls,
the ruler bans, the counts of ruled cards, Jinx and the protections. The
rules that read a site's ruler are unchanged.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: The wearer's consent

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/SiteRulers.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala`
- Modify (doc comment only): `src/main/scala/distantlands/gameplay/powers/wake/Passages.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/GrandMaskConsentSuite.scala`

**Interfaces:**
- Consumes: `CardRule.rulerAt`, `CardRule.rulerOf` (Task 1);
  `BanditCrownRule.holder`, `SiteRulers.consentGranterAt` and
  `consentGranter` (slice 1).
- Produces: `SiteRulers.consentGranterOf(ready: ReadyGame, site: SiteId, card: CardId): Option[PlayerId]`.
  `consentGranter(ready, card: DenizenId)` keeps its signature and now
  reads the card's ruler. `consentGranterAt(ready, site)` keeps reading
  the site, for Hidden Passages (ruling 15).

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/GrandMaskConsentSuite.scala`:

```scala
package distantlands.gameplay.powers

import distantlands.gameplay.{GrandMaskCard, SiteRulers}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.{HallOfMinisters,
  HallOfMinistersCard}
import distantlands.gameplay.powers.economy.CurfewCard
import distantlands.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import distantlands.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import distantlands.gameplay.powers.travel.{ForbiddenForest,
  ForbiddenForestCard}
import distantlands.gameplay.powers.wake.{HiddenPassages, HiddenPassagesCard}
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Who consents for a masked card (catalog batch 11, rulings 7 and 15):
  * the wearer, during their turn, for every card at an Imperial site but a
  * battle plan; Hidden Passages reads its site, so the Chancellor. */
class GrandMaskConsentSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p2 is the Chancellor and p1 a Citizen. The Empire holds Ancient City
    * (Curfew, Relic Patrols, Hidden Passages) and Broken Peaks (Spell
    * Breaker, Forbidden Forest, Hall of Ministers). p3, an Exile, holds
    * the Mask `facedown` or faceup. It is p3's Act. */
  private def maskedEmpire(facedown: Boolean = false): ReadyGame =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .warbandsAt(home, p2, 1).warbandsAt(peaks, p2, 1)
      .denizen(CurfewCard.id, at = home)
      .edifice(RelicPatrolsCard, EdificeSide.Intact, at = home)
      .edifice(HiddenPassagesCard, EdificeSide.Intact, at = home)
      .denizen(SpellBreakerCard.id, at = peaks)
      .edifice(ForbiddenForestCard, EdificeSide.Intact, at = peaks)
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = peaks)
      .relic(p3, GrandMaskCard, facedown = facedown).ready

  private def granters(ready: ReadyGame): Vector[Option[PlayerId]] = Vector(
    SiteRulers.consentGranter(ready, CurfewCard.id),
    ConsentPowers.default.granter(ready, SpellBreaker.id),
    RelicPatrols.granter(ready),
    ForbiddenForest.granter(ready),
    HallOfMinisters.granter(ready))

  test("the wearer consents for every masked card"):
    assertEquals(granters(maskedEmpire()), Vector.fill(5)(Some(p3)))

  test("without the Mask, the Chancellor consents"):
    assertEquals(granters(maskedEmpire(facedown = true)),
      Vector.fill(5)(Some(p2)))

  test("Hidden Passages asks the site's ruler, under the Mask too"):
    assertEquals(HiddenPassages.granter(maskedEmpire()), Some(p2))
    assertEquals(SiteRulers.consentGranterOf(maskedEmpire(), home,
      HiddenPassagesCard.id), Some(p3))
```

The last assertion pins the difference: the card helper would name the
wearer, and the Passages read the site instead.

- [ ] **Step 2: Run the suite to see it fail**

```bash
./sbtw "testOnly distantlands.gameplay.powers.GrandMaskConsentSuite"
```

Expected: it fails to compile on `consentGranterOf`. With that line
commented out, the first test fails: every granter is p2.

- [ ] **Step 3: Read the card in the granters**

In `SiteRulers.scala`, replace slice 1's `consentGranterAt` and
`consentGranter` with

```scala
  /** Who may consent for a power whose ruler is `ruler`: that player, or
    * the Bandit Crown's holder when bandits rule (catalog batch 11, rulings
    * 5 and 6). Nobody for an unruled site or an Empire with no
    * Chancellor. A consent ask drops a granter who is the requester, so
    * nobody lets themselves off. */
  private def granterFor(ready: ReadyGame, ruler: SiteRuler)
      : Option[PlayerId] = ruler match
    case SiteRuler.Player(player) => Some(player)
    case SiteRuler.Bandits => BanditCrownRule.holder(ready)
    case _ => None

  /** Who may consent for a power that reads `site`'s ruler: Hidden
    * Passages (ruling 15). The Empire's sites resolve to its governor. */
  def consentGranterAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    rulerOf(ready, site).flatMap(granterFor(ready, _))

  /** Who may consent for a power of `card`, which stands at `site`: its
    * ruler (`CardRule.rulerAt`), so the Grand Mask's wearer during their
    * turn. */
  def consentGranterOf(ready: ReadyGame, site: SiteId, card: CardId)
      : Option[PlayerId] =
    CardRule.rulerAt(ready, site, card).flatMap(granterFor(ready, _))

  /** Who may consent for a power of the faceup denizen `card`. */
  def consentGranter(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    CardRule.rulerOf(ready, card).flatMap(granterFor(ready, _))
```

Keep slice 1's doc line on the object ("It also names who may consent for
a card's power"), changed to "(`consentGranter`, `consentGranterOf`; a
site's, `consentGranterAt`)".

Point the three edifice granters at the card:

`RelicPatrols.scala` (`PatrolRule.granter`):

```scala
  /** Who may consent: the face's ruler (`SiteRulers.consentGranterOf`),
    * or the Bandit Crown's holder under bandits; nobody at an unruled
    * site. */
  final def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(edifice, side)
      .flatMap(SiteRulers.consentGranterOf(ready, _, edifice))
```

`ForbiddenForest.scala`:

```scala
  /** Who may consent: the Forest's ruler, or the Bandit Crown's holder
    * under bandits. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    forestSite(ready).flatMap(SiteRulers.consentGranterOf(ready, _,
      ForbiddenForestCard.id))
```

`HallOfMinisters.scala`:

```scala
  /** Who may consent: the intact Hall's ruler, or the Bandit Crown's
    * holder under bandits; `None` when nobody rules it. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(HallOfMinistersCard.id, EdificeSide.Intact)
      .flatMap(SiteRulers.consentGranterOf(ready, _, HallOfMinistersCard.id))
```

Leave `HiddenPassages.granter` and `refusal` on `consentGranterAt`. In
the Passages' doc comment, add "Its ruler is the site's (catalog batch
11, ruling 15), so the Grand Mask changes nothing here."

- [ ] **Step 4: Run the suites**

```bash
./sbtw "testOnly distantlands.gameplay.powers.GrandMaskConsentSuite distantlands.gameplay.powers.GrandMaskRulesSuite distantlands.gameplay.powers.ConsentPowersSuite distantlands.gameplay.powers.poweruse.* distantlands.gameplay.powers.recover.* distantlands.gameplay.powers.travel.* distantlands.gameplay.powers.wake.* distantlands.gameplay.operations.* distantlands.gameplay.powers.economy.*"
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/SiteRulers.scala src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala src/main/scala/distantlands/gameplay/powers/travel/ForbiddenForest.scala src/main/scala/distantlands/gameplay/operations/HallOfMinisters.scala src/main/scala/distantlands/gameplay/powers/wake/Passages.scala src/test/scala/distantlands/gameplay/powers/GrandMaskConsentSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): the Grand Mask's wearer consents for the masked cards

A waiver's granter is now the card's ruler, so during the wearer's turn
they consent for every masked card's power. Hidden Passages names its
site's ruler and still asks the Chancellor.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Close the slice

**Files:**
- Modify: `docs/ROADMAP.md` (the engine-blocked phase)
- Modify: `docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md` (status)

- [ ] **Step 1: Update the roadmap and the spec**

In `docs/ROADMAP.md`, under **Phase - Catalog batch - engine-blocked**,
delete the line slice 1 left:

```markdown
- **As-if ruler:** Grand Mask.
```

In the spec, change the status line to

```markdown
**Status:** designed 2026-11-07. Slices 0 to 2 landed.
```

(If slice 1 has not landed, stop: see Prerequisite.) Then run:

```bash
python3 scripts/check-markdown-links.py
```

Expected: passes.

- [ ] **Step 2: Run the full gates**

```bash
python3 scripts/check-architecture.py
```

```bash
./sbtw frontend/test
```

```bash
./sbtw clean coverage test coverageReport
```

Run the last in the background. Expected: every test passes, and
statement coverage is at least 92%. If a suite of finding 9 changes, read
why before touching it: a faceup Mask held by the active Exile now rules
the Imperial sites' cards.

- [ ] **Step 3: Commit**

```bash
git add docs/ROADMAP.md docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 11 slice 2, Grand Mask

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
