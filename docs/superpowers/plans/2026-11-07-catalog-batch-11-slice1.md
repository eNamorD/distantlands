# Catalog Batch 11, Slice 1: Bandit Crown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Bandit Crown (R11): its holder uses the cards at
bandit-ruled sites as if they ruled them, uses their battle plans in any
Campaign, and gives the consent a ruler would for their waivable powers,
while the bandits stay the ruler.

**Architecture:** One root seam, `gameplay/BanditCrown.scala`, holds the
card and `BanditCrownRule`: who holds the Crown, and which sites it
reaches. `PowerAccess.usableSites` and `PlanContext.sitesRuled` add those
sites for the holder. A new `SiteRulers.consentGranterAt` and
`consentGranter` name who may waive a card's power: the ruling player, or
the Crown's holder when bandits rule. Every waiver granter for a card at a
site reads through them. `PayOrAsk.letOffBy` decides when a toll step is
asked, so a bandit toll gets the same pay-or-free step as a player's,
with the holder as the one who may let the payer off.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 11 design](../specs/2026-11-07-catalog-batch-11-design.md):
read "Rulings" 1 to 6 and "Slice 1: Acting as if ruling bandit denizens".

## Prerequisite

This branch (`batch-11-slice-1`) was cut from `batch-11-slice-0`, the only
branch that holds the spec. Merge slice 0 into `0.2.0-alpha` first, then
rebase this branch onto `0.2.0-alpha` before Task 1. Its only commit
before the rebase is this plan.

## Plan-time findings

Inventoried at `14e9d307`.

1. **Access has one seam.** `PowerAccess.usableSites` (private) feeds
   `accessible` (the phase menus, through
   `PhasePowerProcedure.sourcesFrom`), `locate` (`SelectedModifier`,
   `ModifierChoice`, `ChallengeModifiers`, `SpitefulMirror`,
   `CatacombsContribution`) and `siteOf` (`MurkyFountain`, Catacombs).
   Adding the bandit sites there gives the holder every path that uses a
   card. `ruledSites` stays as it is, so Royal Tax, Garrison, Baron,
   Messenger, Captains, Plague Engines, Quartermaster, Royal Ambitions,
   Vow of Union and `ActingSite` never count bandit sites. A Catacombs at a
   bandit site is used from that site, as its ruler would use it.
2. **`MusterSource.locate` is a different function.** It is local to
   `MusterSource` and reads the pawn site, so a Muster or Trade source is
   not widened.
3. **Battle plans have one seam.** Every plan finds its card through
   `PlanContext.denizen` or `edifice`, which search `sitesRuled`, the sites
   whose `CampaignSetup.defenderAt` is the plan's user. Adding the bandit
   sites for a user who holds the Crown covers both sides of any Campaign.
   `lentBy` reads a site card's ruler through `defenderAt`, which is
   Bandits, so a Crown source is never taken for an ally's loan.
4. **The "used" record is already per side.** A user's plans are picked in
   their side's decision (`CampaignIds.planDecision(side)`), and
   `banditPlans` applies its offers without reading the attacker's picks.
   The only cross-side check, `attackerUsed`, runs for a player defender.
   A bandit defender can still apply a card the holder picked as attacker,
   which is ruling 3. Nothing is widened.
5. **Battle plans go through the power-use filter.**
   `BattlePlan.scala:78` builds a `PowerUse`, so Bandit Forest refuses an
   Imperial holder's bandit plans (ruling 6). A Campaign use has
   `consent = false`, so no grant lifts it.
6. **The waivers that read a card's ruler.** Each collects
   `SiteRuler.Player` and so names nobody under bandits today:

   | Waiver | Reads | File |
   |---|---|---|
   | `RulerBans.waiver` and `reason` (Spell Breaker, Forest Council, City Wall) | card | `powers/poweruse/RulerBans.scala` |
   | `PayOrAsk.granter` (Curfew, Forced Labor, Toll Roads, Grasping Vines) | card | `powers/PayOrAsk.scala` |
   | `SecretPolice.waiver` | card | `powers/cardplay/SecretPolice.scala` |
   | `HiddenPassages.granter` and `refusal` | edifice site | `powers/wake/Passages.scala` |
   | `PatrolRule.granter` (Relic Patrols, Bandit Patrols) | edifice site | `powers/recover/RelicPatrols.scala` |
   | `ForbiddenForest.granter` | edifice site | `powers/travel/ForbiddenForest.scala` |
   | `HallOfMinisters.granter` | edifice site | `operations/HallOfMinisters.scala` |

   Left alone: `TravelSitePowers` (Narrow Pass and the other site cards'
   own powers: a site is not a card at a site), `AncientBloodline` (an
   adviser), `TollRoads.payerWaiver` (the payer's own agreement), and
   `KeyToTheCity`, `BannerGuardians`, `Jinx`, `BoilingLake`, `Hospital`,
   `WayStation`, which read a ruler but declare no waiver.
7. **Bandit Forest has no waiver today.** `NonPersistentPowerRestriction`
   says "It names no enemies, so no consent lifts it", and
   `BanditForestSuite` pins that. Ruling 6 overrides it for the holder, so
   Bandit Forest gains a waiver whose granter is the holder. Giant Python
   keeps none.
8. **Hidden Passages needs the holder as its ruler.** "If ... its ruler
   consents": the ruler uses it without asking, and under bandits the menu
   says "Bandits rule this site." With the Crown, the holder uses it
   freely, and anyone else is asked of the holder.
9. **A bandit toll asks nothing today.** `RulerToll.owed`, `Curfew.charged`,
   `TollRoads.charged` and `GraspingVines.charged` build the pay-or-free
   step only for `SiteRuler.Player(owner)`; under bandits they pay at once.
   Changing `PayOrAsk.granter` alone would name the holder on a step that
   is never asked. So the four callers ask the step whenever
   `PayOrAsk.letOffBy` names somebody other than the payer. The payment
   is unchanged: under bandits it is still burnt (ruling 4), and its pay
   option reads "Burn 1 favor". A sent Travel under bandits still pays
   without asking, as it does today. Way Station's question is a choice of
   payment, not a waivable toll, and stays.
10. **Consent never goes to oneself.** `OperationConsent.ask`,
    `PowerUseRules.waivable` and `WalkerPowerGather.consentAsk` drop a
    granter equal to the requester, so the holder is never asked to let
    themselves off (ruling 5). Their option shows the plain reason.
11. **Registration.** `PowerImplementationStatus` counts a power as
    implemented when `WalkerPowerCatalog.contributing` lists it. Marriage
    (`powers/economy/Marriage.scala`) is the precedent for a rule read
    elsewhere: a `ContributingPower` with no contributions. Its card sits
    in the root seam file (`gameplay/EffectiveSuit.scala`), which is the
    precedent for `gameplay/BanditCrown.scala`.
12. **Tests that use the Crown as a stand-in relic.**
    `ConspiracyWhenPlayedSuite`, `ConspiracyTargetWindowSuite`,
    `RecoverProcedureSuite` and `RestSuite` import `BanditCrownCard` from
    `catalog.holding`. Their import moves. The two Conspiracy suites hold
    it faceup, so it now has effects there. Their tables have no bandit
    site that the tested cards stand at, so none should change; the full
    run in Task 4 confirms it.

## Global Constraints

- Work only in this worktree (`.worktrees/batch-11-slice-1`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- The bandits still rule (ruling 1). `SiteRule`, `SiteRulers.rulerOf`,
  `rulerOfCard`, `ruledSites` and `CampaignSetup.defenderAt` do not change.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.
- Code comments and docs are plain prose. Log lines name only non-actors
  (memory: "Log lines omit actor").

---

### Task 1: The Crown's access and battle plans

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/BanditCrown.scala`
- Create: `src/main/scala/oathdigital/gameplay/powers/poweruse/BanditCrown.scala`
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingRelics.scala` (delete `BanditCrownCard`)
- Modify: `src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala:5-6` (import)
- Modify: `src/main/scala/oathdigital/gameplay/PowerAccess.scala:74-75` and its doc comment
- Modify: `src/main/scala/oathdigital/gameplay/powers/campaign/PlanContext.scala:45-46` and its doc comment
- Modify: `src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala` (register)
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `relic.bandit-crown`)
- Modify: the four test imports of finding 12
- Test: `src/test/scala/oathdigital/gameplay/powers/poweruse/BanditCrownSuite.scala` (create)
- Test: `src/test/scala/oathdigital/gameplay/powers/campaign/BanditCrownPlansSuite.scala` (create)

**Interfaces:**
- Produces: `oathdigital.gameplay.BanditCrownCard` (moved verbatim);
  `private[gameplay] object BanditCrownRule` with
  `holder(ready: ReadyGame): Option[PlayerId]`,
  `banditSites(ready: ReadyGame): Set[SiteId]` and
  `sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId]`;
  `case object BanditCrown extends ContributingPower` in
  `oathdigital.gameplay.powers.poweruse`.

- [ ] **Step 1: Move the card and add the rule**

Delete `object BanditCrownCard` (the 7 lines from `object BanditCrownCard`
to its `val powers`, and the blank line after) from `HoldingRelics.scala`.
Create `src/main/scala/oathdigital/gameplay/BanditCrown.scala`:

```scala
package oathdigital.gameplay

import oathdigital.catalog.{PrintedPower, Relic}
import oathdigital.model._

object BanditCrownCard extends Relic(RelicId("R11"), "Bandit Crown", value = 56, defense = 3):
  val power = PrintedPower(PowerId("relic.bandit-crown"),
    persistent = true, cost = Cost.free,
    text = "You may use cards at sites ruled by bandits as if you ruled " +
      "them. _You may use these battle plans against bandits._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bandit Crown (R11): its holder uses the cards at bandit-ruled sites as
  * if they ruled them (catalog batch 11, rulings 1 to 6). The bandits still
  * rule those sites and cards. The holder reaches them through
  * `PowerAccess.usableSites` and `PlanContext`'s sources, and gives the
  * consent their ruler would through `SiteRulers.consentGranterAt`. The
  * battle plans are usable in any Campaign; the italic "against bandits"
  * is flavor (ruling 3). Only a faceup Crown counts.
  */
private[gameplay] object BanditCrownRule:
  /** The player who holds the Crown faceup, if any. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    PowerAccess.faceupRelicHolder(ready, BanditCrownCard.id)

  /** The sites in play that bandits rule. */
  def banditSites(ready: ReadyGame): Set[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      SiteRule.rulingPlayer(ready, site).contains(SiteRuler.Bandits)).toSet

  /** The sites whose cards `player` uses through the Crown: the bandit
    * sites when they hold it, else none. */
  def sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId] =
    if holder(ready).contains(player) then banditSites(ready)
    else Set.empty
```

In `NewFoundations.scala`, add `BanditCrownCard` to the
`oathdigital.gameplay.{...}` import (alphabetical: `ActingTroupeCard,
BanditCrownCard, MarriageCard, PledgeToDiscordCard, SpitefulMirrorCard`).
In the four suites of finding 12, replace
`oathdigital.catalog.holding.BanditCrownCard` with
`oathdigital.gameplay.BanditCrownCard` (in `RestSuite`, split the brace
import so `ArbiterCard` keeps `catalog.holding`).

- [ ] **Step 2: Register the power**

Create `src/main/scala/oathdigital/gameplay/powers/poweruse/BanditCrown.scala`:

```scala
package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution}
import oathdigital.model._

/** Bandit Crown (R11): "You may use cards at sites ruled by bandits as if
  * you ruled them."
  *
  * The rule is `BanditCrownRule`, which `PowerAccess`, `PlanContext`,
  * `SiteRulers.consentGranterAt` and `PayOrAsk.letOffBy` read. This power
  * hooks no window. It is registered so the card counts as implemented.
  * It writes no line.
  */
case object BanditCrown extends ContributingPower:
  val id: PowerId = BanditCrownCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `WalkerPowerCatalog.scala`, change
`Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx) ++` to
`Vector(KnightsErrant, Marriage, PledgeToDiscord, Jinx, BanditCrown) ++`,
adding the import if `poweruse._` is not already imported there. Delete
the line `relic.bandit-crown` from
`src/test/resources/powers/unimplemented.txt`.

- [ ] **Step 3: Write the failing access tests**

Create `src/test/scala/oathdigital/gameplay/powers/poweruse/BanditCrownSuite.scala`:

```scala
package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.{BanditCrownCard, PowerAccess}
import oathdigital.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.gameplay.powers.travel.{Tents, TentsCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** Bandit Crown (R11): its holder uses the cards at bandit-ruled sites as
  * if they ruled them, and the bandits still rule (catalog batch 11,
  * rulings 1 and 2). */
class BanditCrownSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id

  /** p2 is the Chancellor and p1 a Citizen; p3, an Exile, stands at Deep
    * Woods with 1 favor. Bandits rule Ancient City, where Roving Terror
    * and Tents stand. It is p3's Act. */
  private def banditCardsAwayFromP3: Table =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act).pawn(p3, at = woods)
      .favor(p3, 1).bandits(home, 2).denizen(terror, at = home)
      .denizen(TentsCard.id, at = home)

  private def p3HoldsTheCrown: Table =
    banditCardsAwayFromP3.relic(p3, BanditCrownCard)

  private def tentsSelectable(ready: ReadyGame): Boolean =
    val window = PowerWindow.TravelModifierSelection
    Tents.applicable(PowerCtx(catalog, ready, p3, Tents.source, window,
      Vector.empty, Sequence(Vector.empty, Some(window))))

  test("the holder uses a bandit card's power from the phase menu"):
    assert(MenuPlay.availableInMenu(catalog, p3HoldsTheCrown.ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))))

  test("the holder may select a bandit card's modifier"):
    assert(tentsSelectable(p3HoldsTheCrown.ready))

  test("without the Crown, the cards are out of reach"):
    val ready = banditCardsAwayFromP3.ready
    assert(!PowerAccess.accessible(RuleSourceRef.SiteCard(home, terror),
      RuleSourceFace.FaceUp, ready, p3))
    assert(!tentsSelectable(ready))

  test("a facedown Crown reaches nothing"):
    val ready = banditCardsAwayFromP3
      .relic(p3, BanditCrownCard, facedown = true).ready
    assertEquals(PowerAccess.locate(ready, p3, terror), None)

  test("the bandits still rule, and the holder rules nothing more"):
    val ready = p3HoldsTheCrown.ready
    assertEquals(PowerAccess.ruledSites(ready, p3), Set.empty[SiteId])
    assertEquals(SiteRule.rulingPlayer(ready, home), Some(SiteRuler.Bandits))
```

- [ ] **Step 4: Write the failing battle-plan tests**

Create `src/test/scala/oathdigital/gameplay/powers/campaign/BanditCrownPlansSuite.scala`:

```scala
package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.CampaignFixture._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.model._
import oathdigital.testkit.Look

/** Bandit Crown's battle plans (catalog batch 11, ruling 3): the holder
  * uses the plans of cards at bandit sites in any Campaign, and a bandit
  * defender still applies them, so one plan may be used twice. Traveling
  * Doctor is free, and is discarded after a Campaign its user lost. */
class BanditCrownPlansSuite extends munit.FunSuite:
  private val card = TravelingDoctorCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)

  /** p1 attacks the bandits at the origin, where the Doctor stands. */
  private def doctorWithTheDefendingBandits(crown: Boolean): Board =
    val base = board(warbands = 5, supply = 7)
    val b = on(base)(_.denizen(card, at = base.origin))
    if crown then withRelic(b, BanditCrownCard) else b

  /** p1 attacks the rival at the origin; the Doctor stands at another
    * bandit site. */
  private def doctorAtAnotherBanditSite(crown: Boolean): Board =
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(extras = 1, warbands = 5, supply = 7))
    val b = on(base)(_.denizen(card, at = base.extras.head))
    if crown then withRelic(b, BanditCrownCard) else b

  test("the holder attacks the bandits with their plan, and the bandits " +
      "apply it too"):
    val b = doctorWithTheDefendingBandits(crown = true)
    val committed = commit(rules(winningDice), b, 4)
    assert(committed.offered(b.actor).contains(ref))
    val done = committed.pick(b.actor, CampaignIds.attackerPlan, ref)
      .finishPlanWindows
    assertEquals(done.winner, Some(true))
    // Only the losing bandits' application discards the Doctor.
    assert(Look(done.state).inRegionalDiscard(card))

  test("the holder uses a bandit card's plan against a player"):
    val b = doctorAtAnotherBanditSite(crown = true)
    val committed = commit(rules(losingDice), b, 4)
    assert(committed.offered(b.actor).contains(ref))
    val done = committed.pick(b.actor, CampaignIds.attackerPlan, ref)
      .finishPlanWindows
    assertEquals(done.winner, Some(false))
    // The Doctor spared p1's force.
    assertEquals(Look(done.state).player(b.actor).board.warbands, 5)

  test("without the Crown, neither plan is offered"):
    Vector(doctorWithTheDefendingBandits(crown = false),
        doctorAtAnotherBanditSite(crown = false)).foreach { b =>
      val committed = commit(rules(losingDice), b, 4)
      assert(!committed.offered(b.actor).contains(ref))
    }
```

`offered` reads the parked choice. If `commit` parks on another decision
first (the plan window is reached only after the force), advance with
`answeredUntil(CampaignIds.attackerPlan)` before `offered`, as other plan
suites do.

- [ ] **Step 5: Run the new suites to see them fail**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.poweruse.BanditCrownSuite oathdigital.gameplay.powers.campaign.BanditCrownPlansSuite"
```

Expected: they compile and fail. The menu, modifier and plan offers are
refused; the negative tests pass.

- [ ] **Step 6: Widen access**

In `PowerAccess.scala`, replace

```scala
  private def usableSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ruledSites(ready, actor) ++ presentSite(ready, actor)
```

with

```scala
  private def usableSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ruledSites(ready, actor) ++ presentSite(ready, actor) ++
      BanditCrownRule.sitesFor(ready, actor)
```

In the object's doc comment, change "cards at your site, and cards at
sites you rule" to "cards at your site, cards at sites you rule, and, for
the Bandit Crown's holder, cards at sites bandits rule (`BanditCrownRule`)".
In `locate`'s doc comment, change "a site the actor rules, or the one
they act from" to "a site the actor rules, a bandit site for the Crown's
holder, or the one they act from".

- [ ] **Step 7: Widen the plans' sources**

In `PlanContext.scala`, add `import oathdigital.gameplay.BanditCrownRule`
and replace

```scala
  private def sitesRuled: Vector[(SiteId, SiteState)] =
    sitesOf(ruler)
```

with

```scala
  /** The sites whose cards the user's plans come from: the sites they
    * rule, and the bandits' sites when they hold the Bandit Crown (catalog
    * batch 11, ruling 3). A bandit defender keeps its own sites, so a
    * Crown plan may be used by both sides. */
  private def sitesRuled: Vector[(SiteId, SiteState)] =
    val crowned = user.exists(BanditCrownRule.holder(ready).contains)
    sitesOf(ruler) ++
      (if crowned then sitesOf(CampaignDefender.Bandits) else Vector.empty)
```

In the class doc comment, after "or a card at a site the user rules.",
add "The Bandit Crown's holder also uses the cards at bandit sites."

- [ ] **Step 8: Run the suites**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.poweruse.BanditCrownSuite oathdigital.gameplay.powers.campaign.BanditCrownPlansSuite oathdigital.gameplay.powers.UnimplementedPowersSuite oathdigital.gameplay.powers.PowerImplementationStatusSuite oathdigital.gameplay.powers.PowerCatalogUniquenessSuite oathdigital.gameplay.powers.whenplayed.* oathdigital.gameplay.RecoverProcedureSuite oathdigital.gameplay.RestSuite"
```

Expected: all pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/BanditCrown.scala src/main/scala/oathdigital/gameplay/powers/poweruse/BanditCrown.scala src/main/scala/oathdigital/catalog/holding/HoldingRelics.scala src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala src/main/scala/oathdigital/gameplay/PowerAccess.scala src/main/scala/oathdigital/gameplay/powers/campaign/PlanContext.scala src/main/scala/oathdigital/gameplay/powers/WalkerPowerCatalog.scala src/test/resources/powers/unimplemented.txt src/test/scala/oathdigital/gameplay/powers/poweruse/BanditCrownSuite.scala src/test/scala/oathdigital/gameplay/powers/campaign/BanditCrownPlansSuite.scala src/test/scala/oathdigital/gameplay/RecoverProcedureSuite.scala src/test/scala/oathdigital/gameplay/RestSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/ConspiracyWhenPlayedSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/ConspiracyTargetWindowSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): the Bandit Crown's holder uses cards at bandit sites

The holder reaches every card at a bandit-ruled site through the phase
menus, the selected modifiers and the battle plans, in any Campaign.
The bandits still rule, so a bandit defender keeps applying the same
plans and no count of ruled sites changes.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: The holder's consent

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/SiteRulers.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/poweruse/RulerBans.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/poweruse/SpellBreaker.scala:49`
- Modify: `src/main/scala/oathdigital/gameplay/powers/poweruse/ForestCouncil.scala:85,96`
- Modify: `src/main/scala/oathdigital/gameplay/powers/poweruse/CityWall.scala:92,100`
- Modify: `src/main/scala/oathdigital/gameplay/powers/PayOrAsk.scala:65-68`
- Modify: `src/main/scala/oathdigital/gameplay/powers/cardplay/SecretPolice.scala:50-56`
- Modify: `src/main/scala/oathdigital/gameplay/powers/wake/Passages.scala:115-135` and its doc comment
- Modify: `src/main/scala/oathdigital/gameplay/powers/recover/RelicPatrols.scala:60-65`
- Modify: `src/main/scala/oathdigital/gameplay/powers/travel/ForbiddenForest.scala:60-63`
- Modify: `src/main/scala/oathdigital/gameplay/operations/HallOfMinisters.scala:72-75`
- Modify: `src/main/scala/oathdigital/gameplay/powers/poweruse/NonPersistentPowerRestriction.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `BanditCrownSuite.scala`, `BanditForestSuite.scala`, `PassagesSuite.scala`

**Interfaces:**
- Consumes: `BanditCrownRule.holder` (Task 1).
- Produces: `SiteRulers.consentGranterAt(ready: ReadyGame, site: SiteId): Option[PlayerId]`
  and `SiteRulers.consentGranter(ready: ReadyGame, card: DenizenId): Option[PlayerId]`.
  `RulerBans.refusal` and `reason` take `ready: ReadyGame` first instead of
  `ruler: SiteRuler` third.

- [ ] **Step 1: Write the failing granter tests**

Append to `BanditCrownSuite.scala`, with these imports added at the top:

```scala
import oathdigital.gameplay.operations.{HallOfMinisters, HallOfMinistersCard}
import oathdigital.gameplay.powers.ConsentPowers
import oathdigital.gameplay.powers.cardplay.{SecretPolice, SecretPoliceCard}
import oathdigital.gameplay.powers.economy.{Curfew, CurfewCard}
import oathdigital.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import oathdigital.gameplay.powers.search.{ForcedLabor, ForcedLaborCard}
import oathdigital.gameplay.powers.travel.{ForbiddenForest,
  ForbiddenForestCard, GraspingVines, GraspingVinesCard, TollRoads,
  TollRoadsCard}
import oathdigital.gameplay.powers.wake.{HiddenPassages, HiddenPassagesCard}
import oathdigital.gameplay.cards.NarrowPassSite
```

```scala
  private val consent = ConsentPowers.default

  /** Bandits rule Ancient City, where `placed` puts the card; p2 holds the
    * Crown when `crown` is set. */
  private def atTheBanditSite(placed: Table => Table, crown: Boolean)
      : ReadyGame =
    val table = placed(Table.start.bandits(home, 2))
    (if crown then table.relic(p2, BanditCrownCard) else table).ready

  /** Each waivable power of a card at a site, and how to place its card. */
  private val cardPowers: Vector[(PowerId, Table => Table)] = Vector(
    SpellBreaker.id -> (_.denizen(SpellBreakerCard.id, at = home)),
    ForestCouncil.id -> (_.denizen(ForestCouncilCard.id, at = home)),
    CityWall.id -> (_.denizen(CityWallCard.id, at = home)),
    SecretPolice.id -> (_.denizen(SecretPoliceCard.id, at = home)),
    Curfew.id -> (_.denizen(CurfewCard.id, at = home)),
    ForcedLabor.id -> (_.denizen(ForcedLaborCard.id, at = home)),
    TollRoads.id -> (_.denizen(TollRoadsCard.id, at = home)),
    GraspingVines.id -> (_.denizen(GraspingVinesCard.id, at = home)),
    HiddenPassages.id -> (_.edifice(HiddenPassagesCard, EdificeSide.Intact,
      at = home)),
    RelicPatrols.id -> (_.edifice(RelicPatrolsCard, EdificeSide.Intact,
      at = home)),
    ForbiddenForest.id -> (_.edifice(ForbiddenForestCard, EdificeSide.Intact,
      at = home)),
    HallOfMinisters.id -> (_.edifice(HallOfMinistersCard, EdificeSide.Intact,
      at = home)))

  test("the holder may waive every waivable power of a bandit card"):
    cardPowers.foreach { (power, placed) =>
      assertEquals(consent.granter(atTheBanditSite(placed, crown = true),
        power), Some(p2), power.value)
      assertEquals(consent.granter(atTheBanditSite(placed, crown = false),
        power), None, power.value)
    }

  test("the holder rules the waivable powers of bandit cards in the grid"):
    val ready = atTheBanditSite(_.denizen(CityWallCard.id, at = home),
      crown = true)
    assertEquals(consent.ruledBy(ready, p2), Vector(CityWall.id))

  test("a site's own power stays the bandits'"):
    val pass = NarrowPassSite.id
    val ready = Table.start.bandits(pass, 2).relic(p2, BanditCrownCard).ready
    assertEquals(consent.granter(ready, NarrowPassSite.pass), None)
```

The card and power names were checked at plan time.

- [ ] **Step 2: Write the failing Bandit Forest and Passages tests**

In `BanditForestSuite.scala`, change the class doc's last sentence to "It
binds Imperial players only, and only the Bandit Crown's holder may waive
it (catalog batch 11, ruling 6)." Replace the first test with:

```scala
  test("it refuses an Imperial player a non-persistent power of a card " +
      "bandits rule, and nobody may waive it without a Crown"):
    val ready = terrorUnderBanditsWithForestRuined(p1).ready
    val refusal = forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1))
    assertEquals(refusal.map(_.note), Some(forest.usePlain(source)))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter), None)
```

In the third test, change `exists(_.waiver.isEmpty)` to
`exists(_.waiver.exists(_.granter.isEmpty))`. Add the import
`oathdigital.gameplay.BanditCrownCard` and append:

```scala
  test("the Crown's holder may waive it for an Imperial player"):
    val ready = terrorUnderBanditsWithForestRuined(p1)
      .relic(p3, BanditCrownCard).ready
    val option = MenuPlay.menuOptionFor(catalog, ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror)))
    option match
      case DecisionOption.Unavailable(_, OptionReason.Askable(power, note,
          asks, _)) =>
        assertEquals(power, forest.id)
        assertEquals(note, forest.useAsked(source))
        assertEquals(asks, Vector(ConsentAsk(p3, Vector(forest.id))))
      case other => fail(s"not askable: $other")

  test("an Imperial holder is still bound, and cannot let themselves off"):
    val ready = terrorUnderBanditsWithForestRuined(p2)
      .relic(p2, BanditCrownCard).ready
    assertEquals(MenuPlay.menuOptionFor(catalog, ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))),
      DecisionOption.Unavailable(
        DecisionOption.Denizen(DecisionOptionRef.Denizen(terror)),
        OptionReason.Noted(forest.id, forest.usePlain(source))))
```

The second new test reaches the card through the Crown when p2's pawn is
elsewhere, and through the pawn site otherwise; either way the refusal is
the same. In `PassagesSuite.scala`, add the import
`oathdigital.gameplay.BanditCrownCard` and append after "under bandits
nobody may consent, and the ruler uses it freely":

```scala
  test("under bandits the Crown's holder uses it freely, and anyone else " +
      "asks the holder"):
    assert(menuOption(staged(EdificeSide.Intact,
      ruled = _.bandits(home, 1).relic(p1, BanditCrownCard)),
      HiddenPassages.id).available)
    menuOption(staged(EdificeSide.Intact,
        ruled = _.bandits(home, 1).relic(p2, BanditCrownCard)),
        HiddenPassages.id) match
      case DecisionOption.Unavailable(_, OptionReason.Askable(_, note, asks,
          _)) =>
        assertEquals(note, HiddenPassages.needsConsent(HiddenPassages.card))
        assertEquals(asks, Vector(ConsentAsk(p2, Vector(HiddenPassages.id))))
      case other => fail(s"not askable: $other")
```

- [ ] **Step 3: Run them to see them fail**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.poweruse.BanditCrownSuite oathdigital.gameplay.powers.poweruse.BanditForestSuite oathdigital.gameplay.powers.wake.PassagesSuite"
```

Expected: compile error on `forest.useAsked`, which Step 7 adds. That is
the failure.

- [ ] **Step 4: Add the granter helpers**

In `SiteRulers.scala`, add after `rulerOfCard`:

```scala
  /** Who may consent for a power of a card at `site`: the player who rules
    * it, or the Bandit Crown's holder when bandits rule it (catalog batch
    * 11, rulings 5 and 6). Nobody at an unruled site. The Empire's sites
    * resolve to its governor. A consent ask drops a granter who is the
    * requester, so the holder never lets themselves off. */
  def consentGranterAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    rulerOf(ready, site).flatMap:
      case SiteRuler.Player(player) => Some(player)
      case SiteRuler.Bandits => BanditCrownRule.holder(ready)
      case _ => None

  /** [[consentGranterAt]] the site where `card` stands faceup. */
  def consentGranter(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    ready.game.current.faceUpDenizenSite(card)
      .flatMap(consentGranterAt(ready, _))
```

Update the object's doc comment: after "for the powers whose reach is ...
and Secret Police." add "It also names who may consent for a card's
power (`consentGranterAt`)."

- [ ] **Step 5: Point the card granters at them**

Make each change below. Each replaces a granter that collected
`SiteRuler.Player` with the helper.

`RulerBans.scala`: the waiver becomes

```scala
  def waiver(card: DenizenId, asked: NoteKey): Waiver =
    val source = PowerSourceRef.Card(card)
    Waiver(SiteRulers.consentGranter(_, card), source,
      (_, _) => asked(source))
```

Replace `refusal`'s and `reason`'s `ruler: SiteRuler` parameter with a
leading `ready: ReadyGame`, so they read

```scala
  def refusal(ready: ReadyGame, power: PowerId, card: DenizenId,
      requester: PlayerId, covers: ConsentKey, plain: NoteKey,
      asked: NoteKey, code: String): PowerUseRefusal =
    PowerUseRefusal(power, plain(PowerSourceRef.Card(card)),
      reason(ready, power, card, requester, covers, plain, asked, code))

  def reason(ready: ReadyGame, power: PowerId, card: DenizenId,
      requester: PlayerId, covers: ConsentKey, plain: NoteKey,
      asked: NoteKey, code: String): OperationReason =
    val source = PowerSourceRef.Card(card)
    OperationReason(code, s"${power.value} refuses ${requester.value}",
      OperationReasonKind.Impossible, Some(OperationWaiver(power,
        SiteRulers.consentGranter(ready, card), covers, asked(source),
        plain(source))))
```

Their doc comments: "waivable by a player ruler" becomes "waivable by
whoever `SiteRulers.consentGranter` names"; "Under bandits its waiver names
nobody" becomes "Under bandits its waiver names the Bandit Crown's holder,
if any". The object's doc comment: "Only a player ruler may consent
(consent design, ruling 4)" becomes "Only a player ruler, or the Bandit
Crown's holder for a bandit ruler, may consent (consent design, ruling 4;
catalog batch 11, ruling 5)".

Update the five callers to the new order. `SpellBreaker.scala:49`:
`RulerBans.refusal(ready, id, SpellBreakerCard.id, use.user, ...)`.
`ForestCouncil.scala:85`: `RulerBans.refusal(ready, id,
ForestCouncilCard.id, use.user, ...)`; `:96`: `RulerBans.reason(ready, id,
ForestCouncilCard.id, placed._1, ...)`. `CityWall.scala:92`:
`RulerBans.refusal(ready, id, CityWallCard.id, use.user, ...)`; `:100`:
`RulerBans.reason(ready, id, CityWallCard.id, placed._1, ...)`. The
for-comprehensions keep their `ruler <- RulerBans.ruler(...)` and enemy
guards; where `ruler` is then unused (`walledIn` still yields it), bind it
as `_` only if `-Werror` reports an unused value.

`PayOrAsk.scala`: `granter` becomes

```scala
  /** Who may waive this card's step: the player ruling the card, or the
    * Bandit Crown's holder under bandits. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    SiteRulers.consentGranter(ready, card)
```

`SecretPolice.scala`: the waiver's first argument becomes
`SiteRulers.consentGranter(_, cardId)`, and its doc comment reads "the
player who rules the Police's site may waive it, or the Bandit Crown's
holder under bandits".

`RelicPatrols.scala` (`PatrolRule.granter`):

```scala
  /** Who may consent: the player who rules the face's site, or the Bandit
    * Crown's holder under bandits; nobody at an unruled site. */
  final def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(edifice, side)
      .flatMap(SiteRulers.consentGranterAt(ready, _))
```

`ForbiddenForest.scala`:

```scala
  /** Who may consent: the player who rules the Forest's site, or the
    * Bandit Crown's holder under bandits. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    forestSite(ready).flatMap(SiteRulers.consentGranterAt(ready, _))
```

`HallOfMinisters.scala` (add `import oathdigital.gameplay.SiteRulers`):

```scala
  /** Who may consent: the player who rules the intact Hall's site, or the
    * Bandit Crown's holder under bandits; `None` when nobody rules it. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(HallOfMinistersCard.id, EdificeSide.Intact)
      .flatMap(SiteRulers.consentGranterAt(ready, _))
```

`Passages.scala`: `granter` becomes

```scala
  /** Who may consent: the player who rules the Passages' site, or the
    * Bandit Crown's holder under bandits. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    passagesSite(ready).flatMap(SiteRulers.consentGranterAt(ready, _))
```

and `refusal`'s for-comprehension becomes

```scala
    else for
      site <- passagesSite(ready)
      ruler <- SiteRulers.rulerOf(ready, site)
      if ruler != SiteRuler.Player(use.user)
      allowed = SiteRulers.consentGranterAt(ready, site)
      // The Bandit Crown's holder uses it as its ruler would.
      if !allowed.contains(use.user)
      (granter, plain) <- ruler match
        case SiteRuler.Player(player) => Some(Some(player) -> needsConsent)
        case SiteRuler.Bandits =>
          Some(allowed -> allowed.fold(banditsRule)(_ => needsConsent))
        case _ => None
    yield ...
```

with the `yield` unchanged. In the doc comment, change "Under bandits
nobody may consent and the menu shows "Hidden Passages: Bandits rule this
site."" to "Under bandits the Bandit Crown's holder acts as its ruler;
with no holder nobody may consent and the menu shows "Hidden Passages:
Bandits rule this site.""

- [ ] **Step 6: Run the granter tests**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.poweruse.BanditCrownSuite oathdigital.gameplay.powers.wake.PassagesSuite oathdigital.gameplay.powers.ConsentPowersSuite"
```

Expected: everything compiles except `BanditForestSuite` is not run yet;
the granter, grid and Passages tests pass. (Run `BanditForestSuite` after
Step 7.)

- [ ] **Step 7: Give Bandit Forest a waiver**

In `NonPersistentPowerRestriction.scala`, add a last constructor
parameter and its companion type:

```scala
final class NonPersistentPowerRestriction(val id: PowerId, card: CardId,
    code: String, describe: String, usePlainText: String,
    placingPlainText: String, guarded: (ReadyGame, CardId) => Boolean,
    binds: (ReadyGame, PlayerId) => Boolean,
    consent: Option[NonPersistentPowerRestriction.Consent] = None)
```

```scala
object NonPersistentPowerRestriction:
  /** Who may waive the restriction for a player it binds, and the two
    * reasons shown while they may be asked. */
  final case class Consent(granter: ReadyGame => Option[PlayerId],
      useAskedText: String, placingAskedText: String)
```

Inside the class, after `placingPlain`:

```scala
  /** The reasons while `consent`'s granter may be asked: (use, placing). */
  private val asked: Option[(NoteKey, NoteKey)] = consent.map(given =>
    notes.key("no-powers-here-consent", Vector(NotePart.Text(
      given.useAskedText))) ->
      notes.key("no-placing-here-consent", Vector(NotePart.Text(
        given.placingAskedText))))
  /** Why a non-persistent use is refused, while its granter may be asked. */
  def useAsked: NoteKey = asked.map(_._1).getOrElse(usePlain)

  override val waiver: Option[Waiver] = consent.map(given =>
    Waiver(given.granter, cardSource, (_, _) => useAsked(cardSource)))
```

(`cardSource` is declared below `placingPlain` today; move its line above
this block so the `val` is initialised first.)

Give `refused` the waiver:

```scala
  private def refused(reasonCode: String, player: PlayerId, ready: ReadyGame,
      target: DecisionOptionRef, plain: NoteKey): OperationReason =
    val askedKey = if plain == usePlain then asked.map(_._1)
      else asked.map(_._2)
    OperationReason(reasonCode, s"$describe cannot be used by " +
      s"${player.value}", OperationReasonKind.Impossible,
      consent.zip(askedKey).map((given, key) => OperationWaiver(id,
        given.granter(ready), ConsentKey(id, player, PowerUseRules.useKey,
          target), key(cardSource), plain(cardSource))))
```

and its two callers:

```scala
    yield PowerUseRefusal(id, key(cardSource), refused(code, use.user, ready,
      PowerUseRules.target(use), key))
```

```scala
        binds(ready, player) && guarded(ready, placed)).map(placed =>
        refused(s"$code-placing", placed._1, ready,
          PowerUseRules.cardRef(placed._2), placingPlain))
```

Add `import oathdigital.gameplay.operations.PowerUseRules` and
`Waiver` to the `powerresolver` import. Change the class doc's "It names
no enemies, so no consent lifts it (consent design, ruling 1)." to "It
names no enemies, so no ruler's consent lifts it (consent design, ruling
1); a `consent` names who else may (Bandit Forest's Crown holder)."

Pass the consent to Bandit Forest only:

```scala
      binds = Imperials.isImperial,
      consent = Some(Consent(BanditCrownRule.holder,
        "Imperials cannot use the non-persistent powers of cards ruled by " +
          "bandits without the Bandit Crown holder's consent.",
        "Imperials cannot place favor or secrets on cards ruled by bandits " +
          "without the Bandit Crown holder's consent.")))
```

with `import oathdigital.gameplay.BanditCrownRule`, and its doc comment
gains "The Bandit Crown's holder may waive it for an Imperial other than
themselves (catalog batch 11, ruling 6)."

In `src/test/resources/notes/declared-keys.txt`, insert after
`edifice.e29.ruined no-placing-here = ...`:

```text
edifice.e29.ruined no-placing-here-consent = Imperials cannot place favor or secrets on cards ruled by bandits without the Bandit Crown holder's consent.
```

and after `edifice.e29.ruined no-powers-here = ...`:

```text
edifice.e29.ruined no-powers-here-consent = Imperials cannot use the non-persistent powers of cards ruled by bandits without the Bandit Crown holder's consent.
```

- [ ] **Step 8: Run the consent suites**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.poweruse.* oathdigital.gameplay.powers.wake.PassagesSuite oathdigital.gameplay.powers.ConsentPowersSuite oathdigital.gameplay.powers.DeclaredNoteKeysSuite oathdigital.gameplay.powers.LogNoteCatalogSuite oathdigital.gameplay.operations.* oathdigital.gameplay.powers.recover.* oathdigital.gameplay.powers.travel.* oathdigital.gameplay.powers.cardplay.*"
```

Expected: all pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/SiteRulers.scala src/main/scala/oathdigital/gameplay/powers/poweruse/RulerBans.scala src/main/scala/oathdigital/gameplay/powers/poweruse/SpellBreaker.scala src/main/scala/oathdigital/gameplay/powers/poweruse/ForestCouncil.scala src/main/scala/oathdigital/gameplay/powers/poweruse/CityWall.scala src/main/scala/oathdigital/gameplay/powers/PayOrAsk.scala src/main/scala/oathdigital/gameplay/powers/cardplay/SecretPolice.scala src/main/scala/oathdigital/gameplay/powers/wake/Passages.scala src/main/scala/oathdigital/gameplay/powers/recover/RelicPatrols.scala src/main/scala/oathdigital/gameplay/powers/travel/ForbiddenForest.scala src/main/scala/oathdigital/gameplay/operations/HallOfMinisters.scala src/main/scala/oathdigital/gameplay/powers/poweruse/NonPersistentPowerRestriction.scala src/test/resources/notes/declared-keys.txt src/test/scala/oathdigital/gameplay/powers/poweruse/BanditCrownSuite.scala src/test/scala/oathdigital/gameplay/powers/poweruse/BanditForestSuite.scala src/test/scala/oathdigital/gameplay/powers/wake/PassagesSuite.scala
git commit -m "$(cat <<'EOF'
feat(consent): the Bandit Crown's holder consents for bandit cards

Every waiver of a card at a site now names its granter through
SiteRulers.consentGranterAt: the player who rules the site, or the
Crown's holder when bandits rule it. Bandit Forest gains a waiver the
holder gives, and the holder uses Hidden Passages as its ruler would.
A consent ask still drops the requester, so the holder never lets
themselves off.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Bandit tolls the holder may let off

**Files:**
- Modify: `src/main/scala/oathdigital/gameplay/powers/PayOrAsk.scala` (add `letOffBy`)
- Modify: `src/main/scala/oathdigital/gameplay/powers/RulerToll.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/economy/Curfew.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/search/ForcedLabor.scala:73-75`
- Modify: `src/main/scala/oathdigital/gameplay/powers/travel/TollRoads.scala:233-262`
- Modify: `src/main/scala/oathdigital/gameplay/powers/travel/GraspingVines.scala:93-117`
- Test: `src/test/scala/oathdigital/gameplay/powers/economy/CurfewSuite.scala`

**Interfaces:**
- Consumes: `BanditCrownRule.holder` (Task 1), `PayOrAsk.granter` through
  `consentGranter` (Task 2).
- Produces: `PayOrAsk.letOffBy(ready: ReadyGame, ruler: SiteRuler, payer: PlayerId): Option[PlayerId]`;
  `RulerToll.paying(ruler: SiteRuler): String`;
  `RulerToll.payOption(ready, payer, ruler: SiteRuler, taken)` (its third
  parameter was `owner: PlayerId`).

- [ ] **Step 1: Write the failing Curfew tests**

In `CurfewSuite.scala`, add the import `oathdigital.gameplay.BanditCrownCard`
and append:

```scala
  /** Bandits rule both sites, so Curfew and Wrestlers are theirs; p2 holds
    * the Bandit Crown. */
  private def banditsRuleBothAndP2HoldsTheCrown: Table =
    curfewTable.bandits(peaks, 1).bandits(home, 1).relic(p2, BanditCrownCard)

  test("under bandits the Crown's holder may let a trader off: the step " +
      "offers to burn, and the free option is askable of the holder"):
    val ready = banditsRuleBothAndP2HoldsTheCrown.ready
    val free = Curfew.free(DecisionOptionRef.Denizen(wrestlers))
    val options = PlacementPicks.parkedOptions(tradedToStep(ready).state,
      automatic)
    assertEquals(options.find(_.ref == PayOrAsk.pay),
      Some(DecisionOption.Button(PayOrAsk.pay, "Burn 1 favor")))
    assertEquals(options.find(_.ref == free), Some(DecisionOption.Unavailable(
      DecisionOption.Button(free, "Trade without paying"),
      OptionReason.Askable(Curfew.id, Curfew.noFreeTradeConsent(
        PowerSourceRef.Card(CurfewCard.id)),
        Vector(ConsentAsk(p2, Vector(Curfew.id)))))))

  test("paid under the Crown, the favor is still burnt"):
    val ready = banditsRuleBothAndP2HoldsTheCrown.ready
    val (events, result) = tradedAnswering(ready, PayOrAsk.pay)
    assertEquals(Look(result).favor(p1), 1)
    assertEquals(Look(result).favor(p2), Look(ready).favor(p2))
    assertEquals(said(events), Vector(NoteText.Said("burned",
      "Burned 1 favor.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, events), result)

  test("once the holder grants it, the trader gives nothing"):
    val ready = banditsRuleBothAndP2HoldsTheCrown.ready
    val free = Curfew.free(DecisionOptionRef.Denizen(wrestlers))
    val granted = ready.copy(consent = ConsentState().granting(Vector(
      ConsentKey(Curfew.id, p1, Curfew.decisionId, free)),
      ConsentTurn.of(ready)))
    val (events, result) = tradedAnswering(granted, free)
    assertEquals(Look(result).favor(p1), Look(ready).favor(p1) + 1)
    assertEquals(said(events), Vector(NoteText.Said("waived",
      s"Gave nothing, with ${p2.value}'s consent.", covers = false)))

  test("a trader who cannot pay may still ask the holder"):
    assert(wrestlersOffered(banditsRuleBothAndP2HoldsTheCrown.favor(p1, 0)
      .ready))

  test("the holder pays the bandits' toll too, and is asked nothing"):
    val ready = curfewTable.bandits(peaks, 1).bandits(home, 1)
      .relic(p1, BanditCrownCard).ready
    val done = tradedForFavorWithWrestlers(ready)
    assertEquals(Look(done.result).favor(p1), 1)
    assertEquals(said(done.journal.events), Vector(NoteText.Said("burned",
      "Burned 1 favor.", covers = false)))
```

Also extend the suite's class doc: "Under bandits the Bandit Crown's holder
may let a trader off (catalog batch 11, rulings 4 and 5)."

- [ ] **Step 2: Run them to see them fail**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.economy.CurfewSuite"
```

Expected: the first, third and fourth new tests fail (no step is asked
under bandits); the burn tests pass.

- [ ] **Step 3: Add `letOffBy`**

In `PayOrAsk.scala`, in `object PayOrAsk`, add (with `import
oathdigital.gameplay.BanditCrownRule` beside the `SiteRulers` import):

```scala
  /** Who may let `payer` off what `ruler` is owed: a player ruler, or the
    * Bandit Crown's holder under bandits (catalog batch 11, ruling 5);
    * never the payer, who pays without being asked. A toll card's step is
    * asked exactly when this names somebody. */
  def letOffBy(ready: ReadyGame, ruler: SiteRuler, payer: PlayerId)
      : Option[PlayerId] =
    (ruler match
      case SiteRuler.Player(owner) => Some(owner)
      case SiteRuler.Bandits => BanditCrownRule.holder(ready)
      case _ => None).filter(_ != payer)
```

Update the class doc's first sentence: "When a player other than the payer
rules the card, the payer is asked" becomes "When somebody other than the
payer may let them off ([[PayOrAsk.letOffBy]]), the payer is asked".

- [ ] **Step 4: Generalise `RulerToll`**

In `RulerToll.scala`:

```scala
  /** What paying the toll does, for a step's heading: "give 1 favor" to a
    * player ruler, "burn 1 favor" under bandits. */
  def paying(ruler: SiteRuler): String = ruler match
    case SiteRuler.Player(_) => s"give $Favor favor"
    case _ => s"burn $Favor favor"
```

`owed` becomes

```scala
  def owed(step: PayOrAsk, ready: ReadyGame, payer: PlayerId,
      ruler: SiteRuler, operations: Vector[Operation], occasion: String,
      heading: String, freeLabel: String): Vector[Operation] =
    val paid = Vector(payment(payer, ruler),
      Note(step.power, note(step.card, payer, ruler)))
    PayOrAsk.letOffBy(ready, ruler, payer) match
      case Some(granter) => step.around(operations, payer, occasion,
        heading, payOption(ready, payer, ruler, favorTaken(operations, payer)),
        freeLabel, paid, Vector(Note(step.power, _ => Some(waived(
          step.source, NoteArg.Player(granter))))))
      case None => operations ++ paid
```

and its doc: "the step when a player rules it, the payment and its line
under bandits" becomes "the step when somebody may let the payer off
([[PayOrAsk.letOffBy]]), else the payment and its line".

`payOption` becomes

```scala
  def payOption(ready: ReadyGame, payer: PlayerId, ruler: SiteRuler,
      taken: Int = 0): DecisionOption =
    val option = DecisionOption.Button(PayOrAsk.pay, ruler match
      case SiteRuler.Player(owner) => s"Give $Favor favor to ${owner.value}"
      case _ => s"Burn $Favor favor")
    val held = ready.game.current.player(payer)
      .fold(0)(_.board.favor)
    val needed = Favor + taken
    if held >= needed then option
    else DecisionOption.Unavailable(option,
      OptionReason.Written(s"Needs $needed favor; you have $held"))
```

with its doc's "Give 1 favor to {owner}" becoming "Give 1 favor to
{owner}", or "Burn 1 favor" under bandits". The object doc's "When a
player rules the card, the payer is asked first" becomes "When somebody
may let the payer off (a player ruler, or the Bandit Crown's holder under
bandits), the payer is asked first".

- [ ] **Step 5: Ask the step in the four toll cards**

`ForcedLabor.scala`: the heading argument becomes
`s"Forced Labor: ${RulerToll.paying(ruler)} to search here?"`.

`Curfew.scala`: the `TradeCost` transform passes the state:
`.fold(operations)(charged(ctx.catalog, ctx.state, ctx.actor, _,
operations))`, and `charged` becomes

```scala
  private def charged(catalog: ExecutableCatalog, state: ReadyGame,
      trader: PlayerId, ruler: SiteRuler,
      operations: Vector[Operation]): Vector[Operation] =
    PayOrAsk.letOffBy(state, ruler, trader) match
      case Some(granter) =>
        val paid = Vector(RulerToll.payment(trader, ruler),
          Note(id, RulerToll.note(cardId, trader, ruler)))
        val waived = Vector(Note(id, _ => Some(RulerToll.waived(
          Curfew.step.source, NoteArg.Player(granter)))))
        Branch((ready, pending) => tradedWith(catalog, ready, ruler, pending)
          .toVector.map(source => Curfew.step.question(trader, source.wireId,
            s"Curfew: ${RulerToll.paying(ruler)} to trade with this card?",
            RulerToll.payOption(ready, trader, ruler,
              RulerToll.favorTaken(operations, trader)),
            "Trade without paying"))) +: operations :+
          Curfew.step.outcome(paid, waived)
      case None => operations ++ Vector(
        BuildOps((ready, pending) => Right(
          tradedWith(catalog, ready, ruler, pending)
            .map(_ => RulerToll.payment(trader, ruler)).toVector),
          required = true),
        Note(id, RulerToll.note(cardId, trader, ruler)))
```

Its doc: "the step first and its outcome last when a player rules Curfew"
becomes "the step first and its outcome last when somebody may let the
trader off"; "the payment and its line last under bandits" becomes "else
the payment and its line last".

`TollRoads.scala`, in `charged`, the match becomes

```scala
    ruler match
      case SiteRuler.Player(owner) if sent(ctx.state, route) =>
        // (unchanged body)
      // A sent Travel under bandits pays without asking, as before.
      case _ if sent(ctx.state, route) =>
        TravelRoute.beforeMove(operations, paid)
      case _ => PayOrAsk.letOffBy(ctx.state, ruler, route.player) match
        case Some(granter) =>
          TollRoads.step.question(route.player, route.destination.value,
            s"Toll Roads: ${RulerToll.paying(ruler)} to travel there?",
            RulerToll.payOption(ctx.state, route.player, ruler,
              RulerToll.favorTaken(operations, route.player)),
            "Travel without paying") +:
            TravelRoute.beforeMove(operations, Vector(TollRoads.step.outcome(
              paid, Vector(Note(id, _ => Some(TollRoads.waived(
                PowerSourceRef.Card(cardId), NoteArg.Player(route.player),
                NoteArg.Player(granter))))))))
        case None => TravelRoute.beforeMove(operations, paid)
```

and its doc: "the step first when a player rules Toll Roads, the payment
alone when bandits do" becomes "the step first when somebody may let the
traveller off, else the payment alone". Add `PayOrAsk` to its imports if
missing.

`GraspingVines.scala`, in `charged`, the match becomes

```scala
    PayOrAsk.letOffBy(ctx.state, ruler, route.player) match
      case Some(granter) =>
        GraspingVines.step.question(route.player, route.source.value,
          "Grasping Vines: kill 1 warband to travel on?",
          DecisionOption.Button(PayOrAsk.pay, "Kill 1 warband"),
          "Travel without killing") +:
          TravelRoute.beforeMove(operations, Vector(GraspingVines.step.outcome(
            killed, Vector(Note(id, _ => Some(GraspingVines.spared(
              PowerSourceRef.Card(cardId), NoteArg.Player(granter))))))))
      case None => TravelRoute.beforeMove(operations, killed)
```

and its doc: "the step first when a player rules the Vines, the kill alone
when bandits do" becomes "the step first when somebody may let the
traveller off, else the kill alone".

- [ ] **Step 6: Run the toll suites**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.economy.* oathdigital.gameplay.powers.search.* oathdigital.gameplay.powers.travel.* oathdigital.gameplay.powers.ConsentPowersSuite oathdigital.application.*"
```

Expected: all pass. Every pre-existing toll test keeps its result: with no
Crown, `letOffBy` names a player ruler exactly when the old code matched
`SiteRuler.Player`.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/PayOrAsk.scala src/main/scala/oathdigital/gameplay/powers/RulerToll.scala src/main/scala/oathdigital/gameplay/powers/economy/Curfew.scala src/main/scala/oathdigital/gameplay/powers/search/ForcedLabor.scala src/main/scala/oathdigital/gameplay/powers/travel/TollRoads.scala src/main/scala/oathdigital/gameplay/powers/travel/GraspingVines.scala src/test/scala/oathdigital/gameplay/powers/economy/CurfewSuite.scala
git commit -m "$(cat <<'EOF'
feat(powers): the Bandit Crown's holder may let a payer off a bandit toll

A toll owed to bandits is still burnt, and the holder pays it too. Any
other payer is now asked to pay or to go on free, as under a player
ruler, and the holder is the one who may consent. PayOrAsk.letOffBy
decides when the step is asked, for Curfew, Forced Labor, Toll Roads
and Grasping Vines alike.

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
change

```markdown
- **As-if ruler:** Bandit Crown, Grand Mask.
```

to

```markdown
- **As-if ruler:** Grand Mask.
```

In the spec, change the status line to

```markdown
**Status:** designed 2026-11-07. Slices 0 and 1 landed.
```

(If slice 0 has not landed, stop: see Prerequisite.) Then run:

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
statement coverage is at least 92%. If a Conspiracy suite changes
(finding 12), read why before touching it: a faceup Crown in its table
now reaches bandit cards.

- [ ] **Step 3: Commit**

```bash
git add docs/ROADMAP.md docs/superpowers/specs/2026-11-07-catalog-batch-11-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 11 slice 1, Bandit Crown

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```
