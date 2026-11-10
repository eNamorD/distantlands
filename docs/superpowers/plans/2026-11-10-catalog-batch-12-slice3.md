# Catalog Batch 12, Slice 3: Adviser Terms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Council Arbiter and Festival District put advisers into a
Negotiation deal, and refuse acceptance while the terms would leave any
participant over their adviser limit.

**Architecture:** One rule object beside the action,
`NegotiationAdvisers`, says who may use each card, under the walk's
power-use restrictions. The Negotiation's start stores those users in its
start facts, beside slice 2's reach. When the deal opens, the facts build
`DealRules`, the one place that says which pairs may trade advisers. A
transfer row gains `advisers`. Settlement moves each one with a required
`Give`, keeping its face. The adviser limit is the powers' knowledge
(`gameplay.powers.AdviserLimit`), which actions may not import, so a small
trait in `gameplay.actions` (`AdviserLimits`) carries it. The walker's
registry hands `AdviserLimit` to the Negotiation's build and rebuild.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../specs/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (14 to 17, 24 to 26), "The deal model" and "Slice 3:
Adviser terms".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, parameters or values, no
  non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`). It may import `gameplay.powerresolver`
  and `gameplay.operations`. `gameplay.walker` may import `gameplay.powers`.
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire and journal changes need not keep old journals decodable. The new
  `advisers` keys are required.
- Ruling 14: Council Arbiter allows adviser terms only where its holder
  gives or receives.
- Ruling 15: when any participant has access to Festival District
  (intact), every participant may give advisers to any other.
- Ruling 16: advisers keep their facing, and locked advisers never move.
  A recipient knows a facedown adviser they receive.
- Ruling 17: accepting is refused, with the reason shown, while the terms
  would leave any participant over their adviser limit. Nothing is
  discarded.
- Ruling 26: City Wall, Giant Python and Bandit Forest stop Council
  Arbiter and Festival District as rulings 24 and 25 stop The Tribunal:
  judged as the holder's or accessor's own use, with no consent asked.
- The package is `distantlands`. Paths below are at `73d450db2` (slice 1
  merged) plus slice 2
  (`docs/superpowers/plans/2026-11-10-catalog-batch-12-slice2.md`). This
  plan anchors edits by code, not by line.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Setup

Slice 2 must be merged into `0.2.0-alpha` first. This plan uses its
`NegotiationReach` (and its private `usable`), `NegotiationStartFacts`,
`NegotiationProcedure.build(restrictions)` / `rebuild` / `startFacts`,
`WalkerProcedureRegistry.negotiationEntry`, and the deal signatures
`NegotiationDeal.snapshot(state, deal, bindingPromises)` and
`bounds(state, author, participants, bindingPromises)`. Check with:

```bash
git grep -n "def tribunalUsers\|private def usable" -- src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala
git grep -n "negotiationEntry" -- src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala
```

If slice 2 landed with different names, follow the names it landed with
and keep this plan's behaviour.

Work in a worktree off `0.2.0-alpha`, never in the main checkout. Symlink
the main checkout's `.tooling` and `node_modules` into the worktree before
running `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

## Plan-time findings

1. **The limit is power knowledge.** `AdviserLimit.of` and `towardLimit`
   (`gameplay/powers/AdviserLimit.scala`) read Silver Tongue, Insomnia,
   Assassin, Family Wagon, Pied Piper and Lost Tapestry, whose card objects
   live in `gameplay.powers`. The deal snapshot, which `gameplay.actions`
   builds, must read the limit. The snapshot is also rebuilt where no
   walker powers are carried (`OathRules.declaredWalkerTree`,
   `WalkerDecisionProjector`, `ParkedNode`), so a power window cannot
   supply it. A trait in `gameplay.actions` (`AdviserLimits`) states the
   two reads, `AdviserLimit` implements it, and
   `WalkerProcedureRegistry` (in `gameplay.walker`, which may import
   powers) passes `AdviserLimit` to both the Negotiation's `build` and its
   `rebuild`. Slices 5 and 6 run the deal from powers code and pass
   `AdviserLimit` directly.
2. **Every narrowing card is locked.** Silver Tongue, Insomnia and
   Assassin carry `Locked`, so none of them can move faceup. A move
   changes a limit only through the count, or by moving a faceup Family
   Wagon or Pied Piper. The check still evaluates the limit on the state
   after the moves, so it stays right if an unlocked narrowing card is
   added later.
3. **Locks come from the printed restrictions.** `LockedCards` refuses a
   `Give` of a faceup locked adviser, and Ancient Bloodline (an
   `AsIfLocked` rule) refuses some more. A facedown card shows no lock.
   `OperationRestrictions.forCatalog(catalog).printed` holds both. An
   adviser is offered only when no printed restriction gives a reason
   against its `Give`.
4. **An optional `Give` that a restriction refuses is skipped.** The
   settlement's favor and relic `Give`s are optional. An adviser `Give`
   is `required = true` instead, so a refusal rejects the whole
   settlement rather than dropping one adviser. The bounds keep that from
   ever happening.
5. **The recipient already knows what they own.** `GamePresentationProjector.identifiesCard`
   names any card in the viewer's own play area, and
   `CardKnowledgeMoves.follow` records that the giver knew a card that
   left their area. A moved facedown adviser therefore stays known to
   both sides with no new `Peek`. The Game Log names it to both sides and
   writes "a Denizen" for everyone else (`LogWords.seen`).
6. **Facedown adviser terms show like facedown relic terms.** The
   projector shows a facedown relic's identity only to its author, and
   counts it for everyone else. Advisers follow the same rule. An author
   who wants the recipient to see a facedown adviser before accepting adds
   the Negotiation's existing adviser disclosure.
7. **Facedown Visions are advisers.** A facedown Vision sits in
   `PlayerState.advisers` and counts toward the limit. It is offered like
   a facedown denizen. A revealed Vision is not an adviser and is never
   offered.
8. **The users must be read at the start.** The two powers are
   non-persistent, so the blockers apply (ruling 26). The walk's
   restrictions are seen only at the start (slice 2, finding 2), so the
   start facts gain `arbiterUsers` and `festivalUsers`.
9. **`DealRules` holds no participant list.** The deal carries its
   participants, and the rules ask for them where needed. Disclosures
   and Citizenship hold in every deal (spec rulings 5 and 22), so they
   need no field. The spec's `privateDeal` and `prefix` fields, and the
   `Secret` and `Site` kinds, open with the slices that read them.
10. **The two cards move beside the Negotiation.** Council Arbiter leaves
    `HoldingArcaneDenizens.scala`, which then holds nothing and is
    deleted. Festival District leaves `powers/action/SqualidDistrict.scala`,
    which imports it back. The suite that called Council Arbiter "an
    unsupported Negotiation rule" switches to Deed Writer, which stays in
    holding until slice 4.

## File structure

| File | Responsibility |
|---|---|
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdviserCards.scala` (new) | `CouncilArbiterCard`, `FestivalDistrictCard` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdvisers.scala` (new) | who may use each card |
| `src/main/scala/distantlands/gameplay/powers/action/NegotiationAdviserPowers.scala` (new) | the two registered powers, hooking no window |
| `src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala` (deleted) | held only Council Arbiter |
| `src/main/scala/distantlands/gameplay/powers/action/SqualidDistrict.scala` | imports Festival District; comment |
| `src/main/scala/distantlands/model/NegotiationTerms.scala` | `NegotiationTransfer.advisers`; bounds' advisers |
| `src/main/scala/distantlands/model/Decisions.scala` | `Negotiate.overAdviserLimit` |
| `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala` | journal `advisers` |
| `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`, `CommandNestedCodecs.scala` | command wire `advisers` |
| `src/main/scala/distantlands/application/GameIntentMapper.scala` | maps `advisers` |
| `src/main/scala/distantlands/gameplay/actions/AdviserLimits.scala` (new) | the limit seam |
| `src/main/scala/distantlands/gameplay/powers/AdviserLimit.scala` | implements it |
| `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala` (new) | `TermKind`, `DealRules` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala` | the two new user lists; `rules` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | bounds, limit, settlement |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | limits threaded; log lines |
| `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` | validates adviser terms |
| `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala` | passes `AdviserLimit` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`, `ActionProjectionCodec.scala` | adviser rows and editing fields |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | projects them |
| `frontend/src/main/scala/distantlands/frontend/{NegotiationDealPanel,GameClient,ServerUiSupport}.scala` | adviser rows, the withheld Accept |
| `src/test/resources/powers/unimplemented.txt` | loses two lines |
| `docs/architecture/all-exile-negotiation.md` | adviser terms |

---

### Task 1: Who may use the two cards

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdviserCards.scala`
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdvisers.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/action/NegotiationAdviserPowers.scala`
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/SqualidDistrict.scala`
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify: `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/powers/action/{SqualidDistrictSuite,DarkEnforcerSuite,MovementFixture}.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/action/NegotiationAdvisersSuite.scala` (new)

**Interfaces:**
- Consumes: slice 2's `NegotiationReach.usable(catalog, ready, use,
  restrictions): Boolean` (made `private[negotiation]` here),
  `PowerAccess.faceupAdviserHolder`, `PowerAccess.accessible`,
  `PowerUse.of`, `CurrentGameState.edificeSite`.
- Produces:
  - `CouncilArbiterCard` (`DenizenId("200")`, `.power`) and
    `FestivalDistrictCard` (`EdificeId("E21")`, `.intact.power`,
    `.ruined.power`), in `distantlands.gameplay.actions.negotiation`.
  - `NegotiationAdvisers.arbiterUsers(catalog: ExecutableCatalog, ready:
    ReadyGame, players: Vector[PlayerId], restrictions:
    Vector[PowerUseRestriction]): Vector[PlayerId]`.
  - `NegotiationAdvisers.festivalUsers(...)`, same parameters and result.
  - Powers `CouncilArbiter` and `FestivalDistrict` in
    `distantlands.gameplay.powers.action`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/action/NegotiationAdvisersSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, NegotiationAdvisers}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Council Arbiter (200) and Festival District (E21 intact): who may use
  * each to put advisers into a deal (catalog batch 12, rulings 14, 15 and
  * 26). */
class NegotiationAdvisersSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val city = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val everyone = Vector(p1, p2, p3)
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City. */
  private def apart: Table = Table.start
    .pawn(p1, at = peaks).pawn(p2, at = dunes).pawn(p3, at = city)

  private def arbiters(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty,
      among: Vector[PlayerId] = everyone): Vector[PlayerId] =
    NegotiationAdvisers.arbiterUsers(catalog, table.ready, among,
      restrictions)

  private def festivalGoers(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty)
      : Vector[PlayerId] =
    NegotiationAdvisers.festivalUsers(catalog, table.ready, everyone,
      restrictions)

  test("Council Arbiter's faceup holder may use it"):
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id)),
      Vector(p2))

  test("a facedown Council Arbiter, or a holder outside the deal, gives " +
      "no user"):
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id,
      facedown = true)), Vector.empty[PlayerId])
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id),
      among = Vector(p1, p3)), Vector.empty[PlayerId])

  test("Festival District is used by players at its site or ruling it"):
    val festival = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = dunes)
    assertEquals(festivalGoers(festival), Vector(p2))
    val ruled = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = woods).warbandsAt(woods, p3, 1)
    assertEquals(festivalGoers(ruled), Vector(p3))

  test("Squalid District, the ruined face, gives no user"):
    assertEquals(festivalGoers(apart.edifice(FestivalDistrictCard,
      EdificeSide.Ruined, at = dunes)), Vector.empty[PlayerId])

  test("City Wall stops its ruler's enemy at its site from using Council " +
      "Arbiter"):
    val walled = apart.adviser(p1, CouncilArbiterCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(arbiters(walled), Vector(p1))
    assertEquals(arbiters(walled, blockers), Vector.empty[PlayerId])

  test("Giant Python at Festival District's site stops every use of it"):
    val python = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = dunes).denizen(GiantPythonCard.id, at = dunes)
    assertEquals(festivalGoers(python), Vector(p2))
    assertEquals(festivalGoers(python, blockers), Vector.empty[PlayerId])
```

`GiantPythonCard` is in `distantlands.gameplay.powers.action`, the suite's
own package.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.NegotiationAdvisersSuite"`
Expected: compile error: `NegotiationAdvisers` not found in
`distantlands.gameplay.actions.negotiation`.

- [ ] **Step 3: Move the cards**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdviserCards.scala`
with the two card objects cut from their files, text unchanged:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{AdviserOnly, Denizen, Edifice, EdificeFace,
  Locked, PrintedPower}
import distantlands.model._

/** The cards whose powers let advisers into a Negotiation deal (catalog
  * batch 12, slice 3). They live beside the Negotiation, which reads them
  * ([[NegotiationAdvisers]]). Their powers are registered in
  * `gameplay.powers.action`; Squalid District, the ruined face, is an
  * action there. */
object CouncilArbiterCard extends Denizen(DenizenId("200"), "Council Arbiter", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-arbiter"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may give, receive, and swap " +
      "advisers. _You cannot exchange locked advisers. You still " +
      "follow your adviser limit._")
  val powers: Vector[PrintedPower] = Vector(power)

object FestivalDistrictCard extends Edifice(EdificeId("E21"), Suit.Hearth):
  object intact extends EdificeFace("Festival District") with Locked:
    val power = PrintedPower(PowerId("edifice.e21.intact"),
      persistent = false, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** Players may give, receive, and swap " +
        "advisers. _Players must follow their adviser limits._")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Squalid District"):
    val power = PrintedPower(PowerId("edifice.e21.ruined"),
      persistent = false, cost = Cost(favor = 1, favorBurnt = 1),
      text = "**ACTION:** Discard any number of facedown advisers you " +
        "have. Draw that many denizens from the bottom of the world " +
        "deck as facedown advisers.")
    val powers: Vector[PrintedPower] = Vector(power)
```

Then:

- Delete `src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala`
  (`git rm`). It held only Council Arbiter.
- In `SqualidDistrict.scala`, delete `FestivalDistrictCard`, add
  `import distantlands.gameplay.actions.negotiation.FestivalDistrictCard`,
  and narrow the catalog import to the names the file still uses (run the
  compile: `-Werror` names any unused one; `Edifice`, `EdificeFace`,
  `Locked` and `PrintedPower` move out with the card). In its doc
  comment, replace "The intact face is a Negotiation power and is not
  implemented." with "The intact face, Festival District, is a
  Negotiation power ([[distantlands.gameplay.actions.negotiation.NegotiationAdvisers]])."
- In `NewFoundations.scala`, extend slice 2's
  `import distantlands.gameplay.actions.negotiation.{TheTribunalCard,
  TravelingNegotiatorCard}` to
  `import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, TheTribunalCard, TravelingNegotiatorCard}`. The
  catalog lists keep naming both cards where they are.
- In `SqualidDistrictSuite.scala`, `DarkEnforcerSuite.scala` and
  `MovementFixture.scala`, add
  `import distantlands.gameplay.actions.negotiation.FestivalDistrictCard`.
- In `NegotiationProcedureSuite.scala`, replace the import
  `distantlands.catalog.holding.CouncilArbiterCard` with
  `distantlands.catalog.holding.DeedWriterCard`, and in the test "an
  unsupported Negotiation rule does not block the Negotiation" use
  `gathered.adviser(p1, DeedWriterCard)`.

- [ ] **Step 4: Write the rule**

In `NegotiationReach.scala`, change `private def usable` to
`private[negotiation] def usable`, and add to its doc comment: "Shared
with [[NegotiationAdvisers]]."

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdvisers.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Who may let advisers into a Negotiation deal (catalog batch 12,
  * rulings 14, 15 and 26). The Negotiation reads it at its start, so it
  * sits beside the action, outside `powers`; the two powers that register
  * the cards hook no window.
  *
  *  - **Council Arbiter** (faceup adviser): its holder may give and
  *    receive advisers ([[DealRules.kinds]] opens the pairs that hold
  *    them).
  *  - **Festival District** (intact): a player with access to it (its
  *    site, a site they rule, or another route `PowerAccess` grants) opens
  *    advisers between every pair of the deal.
  *
  * Both are non-persistent, so both pass `restrictions`, the walk's
  * power-use restrictions (City Wall, Giant Python, Bandit Forest), judged
  * as the use of the player who holds the card or has the access. The
  * Negotiation asks for no consent.
  */
object NegotiationAdvisers:
  /** The players among `players` who hold Council Arbiter faceup and may
    * use it: at most one. */
  def arbiterUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    PowerAccess.faceupAdviserHolder(ready, CouncilArbiterCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          CouncilArbiterCard.power, CouncilArbiterCard.id), restrictions))

  /** The players among `players` with access to Festival District
    * (intact) who may use it, in the order given. */
  def festivalUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    ready.game.current.edificeSite(FestivalDistrictCard.id, EdificeSide.Intact)
      .toVector.flatMap(site => players.filter(player =>
        PowerAccess.accessible(
          RuleSourceRef.Edifice(site, FestivalDistrictCard.id),
          RuleSourceFace.Intact, ready, player) &&
          NegotiationReach.usable(catalog, ready, PowerUse.of(player,
            FestivalDistrictCard.intact.power, FestivalDistrictCard.id),
            restrictions)))
```

- [ ] **Step 5: Register the powers**

Create `src/main/scala/distantlands/gameplay/powers/action/NegotiationAdviserPowers.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Council Arbiter (200): "**WHEN NEGOTIATING:** You may give, receive,
  * and swap advisers." (catalog batch 12, rulings 14, 16 and 17).
  *
  * The rule is `NegotiationAdvisers` and `DealRules`, which the
  * Negotiation reads. This power hooks no window. It is registered so the
  * card counts as implemented. Its moves write the Negotiation's own
  * "gave" lines.
  */
case object CouncilArbiter extends ContributingPower:
  val id: PowerId = CouncilArbiterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Festival District (E21, intact): "**WHEN NEGOTIATING:** Players may
  * give, receive, and swap advisers." (catalog batch 12, rulings 15 to
  * 17).
  *
  * The rule is `NegotiationAdvisers` and `DealRules`. This power hooks no
  * window. It is registered so the card counts as implemented.
  */
case object FestivalDistrict extends ContributingPower:
  val id: PowerId = FestivalDistrictCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `WalkerPowerCatalog.contributing`, append `CouncilArbiter,
FestivalDistrict` after slice 2's `BanditTribunal` in the vector that holds
`BanditCrown`.

In `src/test/resources/powers/unimplemented.txt`, delete the lines
`denizen.council-arbiter` and `edifice.e21.intact`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.NegotiationAdvisersSuite distantlands.gameplay.powers.action.SqualidDistrictSuite distantlands.gameplay.powers.action.DarkEnforcerSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.PowerKindsCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite"`
Expected: PASS.

If a blocker test fails, read that restriction's `refusal` before
changing anything: the test states ruling 26, and the fix belongs in
`NegotiationAdvisers`, not in the restriction.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdviserCards.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationAdvisers.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala \
  src/main/scala/distantlands/gameplay/powers/action/NegotiationAdviserPowers.scala \
  src/main/scala/distantlands/gameplay/powers/action/SqualidDistrict.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala \
  src/test/scala/distantlands/gameplay/powers/action/NegotiationAdvisersSuite.scala \
  src/test/scala/distantlands/gameplay/powers/action/SqualidDistrictSuite.scala \
  src/test/scala/distantlands/gameplay/powers/action/DarkEnforcerSuite.scala \
  src/test/scala/distantlands/gameplay/powers/action/MovementFixture.scala
git rm src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): who may use Council Arbiter and Festival District

The two cards move beside the Negotiation, which will read
NegotiationAdvisers. Both pass the power-use filter, judged as their
user's use. The two powers register with no window.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: A transfer row carries advisers

**Files:**
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` (`hasSubstance` only)
- Modify: `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala`
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameClient.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala`,
  `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`,
  `src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala`,
  `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`

**Interfaces:**
- Produces:
  - `distantlands.model.NegotiationTransfer(recipient: PlayerId, favor:
    Int, relics: Vector[RelicId], advisers: Vector[WorldCardId] =
    Vector.empty)`.
  - `distantlands.protocol.NegotiationTransfer(recipientPlayerId: String,
    favor: Int, relicIds: Vector[String], advisers: Vector[WorldCard] =
    Vector.empty)`.
  - `frontend.NegotiationTransferInput(recipientPlayerId: String, favor:
    Int, relicIds: Vector[String], advisers:
    Vector[distantlands.protocol.WorldCard] = Vector.empty)`.

- [ ] **Step 1: Write the failing tests**

In `WalkerDecisionWireSuite`, test "walker choice steps round trip the
negotiation answers", change the transfer row to
`NegotiationTransfer(blue, 2, Vector(RelicId("r1")), Vector(DenizenId("d1"), VisionId("v2")))`.

In `CommandProtocolSuite`, in the round-tripped intents list, change
`NegotiationTransfer("blue", 2, Vector("r1"))` in the `ProposeTermsWire`
to `NegotiationTransfer("blue", 2, Vector("r1"), Vector(WorldCard("denizen", "d1")))`.
Then add a test beside the existing duplicate-row test (the one whose
JSON names `"relicIds":[]` twice):

```scala
  test("a transfer row that names one adviser twice is refused"):
    val repeated = """{"expectedNextSequence":0,"intent":{"type":"resolveWalker","decisionId":"negotiation.deal","payload":{"kind":"propose-terms","terms":{"transfers":[{"recipientPlayerId":"p2","favor":0,"relicIds":[],"advisers":[{"kind":"denizen","id":"d1"},{"kind":"denizen","id":"d1"}]}],"disclosures":[],"citizenshipPlayerIds":[],"promises":[],"bindingPromises":[]}}}}"""
    assert(decode(repeated).isLeft, decode(repeated))
```

Use the decode helper the duplicate-row test uses; if it is named
differently, call that one.

In `GameIntentMapperNegotiationSuite`, add:

```scala
  test("an adviser in a transfer row maps to its world card"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector(distantlands.protocol.WorldCard("vision", "v1"),
          distantlands.protocol.WorldCard("denizen", "d1")))),
      Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          Vector(VisionId("v1"), DenizenId("d1")))))))))
    val unknown = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector(distantlands.protocol.WorldCard("relic", "r1")))),
      Vector.empty))
    assert(bound(unknown).isLeft, bound(unknown))
```

In `NegotiationDealSuite`, add:

```scala
  test("an adviser alone is substance"):
    val deal = fold(said(p1, ProposeTerms(NegotiationTerms(Vector(
      NegotiationTransfer(p2, 0, Vector.empty, Vector(DenizenId("d1"))))))))
    assert(deal.hasSubstance)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite"`
Expected: compile errors: `NegotiationTransfer` takes no fourth
argument.

- [ ] **Step 3: Widen the model**

In `NegotiationTerms.scala`, give `NegotiationTransfer` a last field
`advisers: Vector[WorldCardId] = Vector.empty`, and beside its relic
`require` add:

```scala
  require(advisers.distinct.size == advisers.size,
    "a transfer row names one adviser twice")
```

In `NegotiationDeal.scala`, `DealState.hasSubstance` reads
`t.transfers.exists(x => x.favor > 0 || x.relics.nonEmpty ||
x.advisers.nonEmpty)`.

- [ ] **Step 4: Widen the journal**

In `NegotiationTermsCodec.encode`, add to each transfer row, after
`"relicIds"`:

```scala
      "advisers" -> ujson.Arr.from(transfer.advisers.map(encodeCardId))))),
```

(moving the row's closing parentheses after it). In `decode`, replace the
transfer row's body with:

```scala
      transfers <- traverse(value("transfers").arr.toVector) { row =>
        val raw = row("favor").num
        for
          favor <- Either.cond(raw.isValidInt, raw.toInt, InvalidValue(
            s"$path.transfers.favor", s"favor '$raw' is not an integer"))
          advisers <- traverse(row("advisers").arr.toVector)(card =>
            decodeWorldCardId(card, s"$path.transfers.advisers"))
        yield NegotiationTransfer(PlayerId(row("recipientPlayerId").str),
          favor, row("relicIds").arr.toVector.map(v => RelicId(v.str)),
          advisers)
      }
```

The `require` on a repeated adviser throws inside the `try`, which the
existing `NonFatal` catch turns into `InvalidValue`.

- [ ] **Step 5: Widen the command wire**

In `CommandIntents.scala`, give `NegotiationTransfer` a last field
`advisers: Vector[WorldCard] = Vector.empty`.

In `CommandNestedCodecs.scala`:

- `encodeNegotiation`'s transfer row adds
  `"advisers" -> ujson.Arr.from(row.advisers.map(world))` after
  `"relicIds"`.
- `decodeTransfer` becomes:

```scala
  private def decodeTransfer(value: ujson.Value, path: String) = obj(value, path).flatMap { row => for
    _ <- exact(row, Set("recipientPlayerId", "favor", "relicIds", "advisers"), path)
    recipient <- string(row, "recipientPlayerId", path)
    favor <- field(row, "favor", path).flatMap(integer(_, s"$path.favor"))
    relics <- field(row, "relicIds", path).flatMap(strings(_, s"$path.relicIds"))
    _ <- noDuplicates(relics, s"$path.relicIds")
    adviserValues <- field(row, "advisers", path).flatMap(array(_, s"$path.advisers"))
    advisers <- traverse(adviserValues.zipWithIndex) { case (v, i) => decodeWorld(v, s"$path.advisers[$i]") }
    _ <- noDuplicates(advisers.map(card => s"${card.kind}/${card.id}"), s"$path.advisers")
  yield NegotiationTransfer(recipient, favor, relics, advisers) }
```

In `GameIntentMapper.negotiation`, map every row's advisers first, then
build the terms from them:

```scala
  private def negotiation(value: distantlands.protocol.NegotiationTerms)
      : Result[distantlands.model.NegotiationTerms] = for
    rows <- traverse(value.transfers)(row => traverse(row.advisers)(card =>
      world(card, "$.intent.terms.transfers.advisers")).map(row -> _))
    ds <- traverse(value.disclosures)(disclosure)
    promises <- promised(value.promises, "$.intent.terms.promises")
    binding <- promised(value.bindingPromises, "$.intent.terms.bindingPromises")
    terms <- try Right(distantlands.model.NegotiationTerms(
        rows.map((v, advisers) => distantlands.model.NegotiationTransfer(
          PlayerId(v.recipientPlayerId), v.favor,
          v.relicIds.map(RelicId.apply), advisers)), ds,
        value.citizenshipPlayerIds.map(PlayerId.apply), promises, binding))
      catch case refused: IllegalArgumentException =>
        Left(GameIntentMappingFailure("$.intent.terms",
          Option(refused.getMessage).getOrElse("invalid Negotiation terms")))
  yield terms
```

In `frontend/.../GameClient.scala`, give `NegotiationTransferInput` a last
field `advisers: Vector[distantlands.protocol.WorldCard] = Vector.empty`.
In `ServerUiSupport.protocolNegotiationTerms`, the transfer becomes
`NegotiationTransfer(v.recipientPlayerId, v.favor, v.relicIds, v.advisers)`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/NegotiationTerms.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala \
  src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala \
  shared/src/main/scala/distantlands/protocol/CommandIntents.scala \
  shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala \
  src/main/scala/distantlands/application/GameIntentMapper.scala \
  frontend/src/main/scala/distantlands/frontend/GameClient.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala \
  shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala \
  src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): a transfer row carries advisers

The model, journal, command wire and intent mapper each take a row's
advisers as world cards. A row naming one adviser twice is refused.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Deal rules, the limit and settlement

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/AdviserLimits.scala`
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/AdviserLimit.scala`
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala` (`NegotiationBounds`)
- Modify: `src/main/scala/distantlands/model/Decisions.scala` (`DecisionQuery.Negotiate`)
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationAdviserTermsSuite.scala` (new),
  `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala`,
  `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationAdvisers.arbiterUsers` and
  `festivalUsers`; Task 2's `NegotiationTransfer.advisers`.
- Produces:
  - `distantlands.gameplay.actions.AdviserLimits` with `of(catalog,
    ready, player): Int`, `towardLimit(ready, player): Int` and
    `exceeded(catalog, ready, player): Boolean`. `AdviserLimit` extends
    it.
  - `TermKind` (`Favor`, `Relic`, `Adviser`) and `DealRules(adviserTraders:
    Vector[PlayerId] = Vector.empty, advisersOpen: Boolean = false,
    bindingPromises: Boolean = false)` with `kinds(author, recipient):
    Set[TermKind]` and `adviserRecipients(author, participants):
    Vector[PlayerId]`.
  - `NegotiationStartFacts(candidates, tribunalUsers, arbiterUsers =
    Vector.empty, festivalUsers = Vector.empty)` with `rules(participants):
    DealRules`.
  - `NegotiationBounds.advisers: Vector[WorldCardId]` and
    `NegotiationBounds.adviserRecipients: Vector[PlayerId]`, both
    defaulting to empty.
  - `DecisionQuery.Negotiate.overAdviserLimit: Vector[PlayerId]`,
    defaulting to empty. When it is not empty, `acceptors` is empty.
  - `NegotiationDeal.snapshot(catalog, state, deal, rules: DealRules,
    limits: AdviserLimits)`, `NegotiationDeal.overAdviserLimit(catalog,
    state, deal, limits): Vector[PlayerId]`.
  - `NegotiationProcedure.build(limits, restrictions)(...)` and
    `NegotiationProcedure.rebuild(limits)(...)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/NegotiationAdviserTermsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, NegotiationAdvisers, NegotiationDeal,
  NegotiationStartFacts}
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.action.AssassinCard
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.rest.SilverTongueCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseManyAnswer, ProposeTerms}
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Adviser terms (catalog batch 12, rulings 14 to 17): who may offer
  * them, which advisers, the adviser limit, and what settling moves. */
class NegotiationAdviserTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id
  private val arbiter: WorldCardId = CouncilArbiterCard.id
  private val assassin: WorldCardId = AssassinCard.id
  private val birdsong: WorldCardId = BirdsongCard.id

  /** Every player at Broken Peaks with 5 favor. p1 holds Council Arbiter
    * faceup; p2 holds Birdsong facedown. */
  private def arbitrated: Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .favor(p1, 5).favor(p2, 5).favor(p3, 5)
    .adviser(p1, CouncilArbiterCard.id)
    .adviser(p2, BirdsongCard.id, facedown = true)

  /** p1's Negotiation with `who`, parked on the deal. */
  private def atDeal(table: Table, who: PlayerId*): OathState =
    val started = rules.startWalker(Ready(table.ready), ActionRef.Negotiation,
      p1).getOrElse(fail("Negotiation must start"))
    rules.resolveWalker(started.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(
        who.toVector.map(DecisionOptionRef.Player(_))))
      .getOrElse(fail("the negotiators must be accepted")).state

  private def say(state: OathState, by: PlayerId, answer: DecisionAnswer) =
    rules.resolveWalker(state, by, NegotiationDeal.dealDecisionId, answer)

  /** `by` offers `to` exactly `advisers`. */
  private def offer(to: PlayerId, advisers: WorldCardId*) =
    ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(to, 0,
      Vector.empty, advisers.toVector))))

  /** The deal `state` is parked on. */
  private def parkedDeal(state: OathState): DecisionQuery.Negotiate =
    ParkedNode.of(state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide.query match
        case negotiate: DecisionQuery.Negotiate => negotiate
        case other => fail(s"not a deal: $other")
      case other => fail(s"not parked on a decision: $other")

  /** Proposes `p1`'s and `p2`'s terms in a two-party deal, then has both
    * accept. */
  private def agreed(table: Table, p1Terms: DecisionAnswer,
      p2Terms: DecisionAnswer): OathState =
    Vector(p1 -> p1Terms, p2 -> p2Terms, p2 -> AcceptDeal, p1 -> AcceptDeal)
      .foldLeft(atDeal(table, p2))((state, step) =>
        say(state, step._1, step._2).getOrElse(fail(s"$step is refused"))
          .state)

  test("Council Arbiter opens adviser terms only where its holder gives " +
      "or receives"):
    val bounds = parkedDeal(atDeal(arbitrated, p2, p3)).bounds
    assertEquals(bounds(p1).adviserRecipients, Vector(p2, p3))
    assertEquals(bounds(p2).adviserRecipients, Vector(p1))
    assertEquals(bounds(p3).adviserRecipients, Vector(p1))
    val dealt = atDeal(arbitrated, p2, p3)
    assert(say(dealt, p2, offer(p3, birdsong)).isLeft)
    assert(say(dealt, p2, offer(p1, birdsong)).isRight)

  test("the start stores Council Arbiter's holder"):
    val started = rules.startWalker(Ready(arbitrated.ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    assertEquals(Outcomes.ready(started.state).game.current.walkerStartFacts,
      NegotiationStartFacts(Vector(p2, p3), Vector.empty, Vector(p1),
        Vector.empty).refs)

  test("without Council Arbiter or Festival District no adviser is offered"):
    val plain = Table.start.pawn(p1, at = site).pawn(p2, at = site)
      .pawn(p3, at = site).adviser(p2, BirdsongCard.id, facedown = true)
    val bounds = parkedDeal(atDeal(plain, p2)).bounds
    assert(bounds.values.forall(b => b.adviserRecipients.isEmpty &&
      b.advisers.isEmpty), bounds)
    assert(say(atDeal(plain, p2), p2, offer(p1, birdsong)).isLeft)

  test("Festival District opens advisers between every pair"):
    val festival = Table.start.pawn(p1, at = site).pawn(p2, at = site)
      .pawn(p3, at = site).adviser(p2, BirdsongCard.id, facedown = true)
      .edifice(FestivalDistrictCard, EdificeSide.Intact, at = site)
    val bounds = parkedDeal(atDeal(festival, p2, p3)).bounds
    assertEquals(bounds(p2).adviserRecipients, Vector(p1, p3))
    assertEquals(bounds(p3).adviserRecipients, Vector(p1, p2))
    assertEquals(bounds(p2).advisers, Vector(birdsong))

  test("a locked faceup adviser is never offered"):
    // Assassin is locked, and lowers p1's limit to 2.
    val armed = arbitrated.adviser(p1, AssassinCard.id)
    assertEquals(parkedDeal(atDeal(armed, p2)).bounds(p1).advisers,
      Vector(arbiter))
    assert(say(atDeal(armed, p2), p1, offer(p2, assassin)).isLeft)

  test("terms over a limit leave nobody able to accept, and name who"):
    // p2 holds Silver Tongue faceup (a limit of 2) and Birdsong.
    val tongue = arbitrated.adviser(p2, SilverTongueCard.id)
    val dealt = atDeal(tongue, p2)
    val over = say(dealt, p1, offer(p2, arbiter)).getOrElse(fail("terms"))
      .state
    assertEquals(parkedDeal(over).overAdviserLimit, Vector(p2))
    assertEquals(parkedDeal(over).acceptors, Set.empty[PlayerId])
    assert(say(over, p2, AcceptDeal).isLeft)
    // A swap keeps p2 at two.
    val swapped = say(over, p2, offer(p1, birdsong)).getOrElse(fail("swap"))
      .state
    assertEquals(parkedDeal(swapped).overAdviserLimit, Vector.empty[PlayerId])
    assertEquals(parkedDeal(swapped).acceptors, Set(p1, p2))

  test("a facedown adviser moves facedown, and its giver still knows it"):
    val settled = Outcomes.ready(agreed(arbitrated, offer(p2),
      offer(p1, birdsong)))
    val look = Look(settled)
    assert(look.player(p1).advisers.contains(
      DenizenState(BirdsongCard.id, Orientation.FaceDown, Tokens.empty)),
      look.player(p1).advisers)
    assert(!look.player(p2).advisers.exists(_.id == birdsong))
    assert(settled.knowledge.advisers.getOrElse(p2, Vector.empty)
      .contains(birdsong), settled.knowledge)

  test("a faceup adviser keeps its power with its new holder"):
    val settled = Outcomes.ready(agreed(arbitrated, offer(p2, arbiter),
      offer(p1, birdsong)))
    assertEquals(NegotiationAdvisers.arbiterUsers(catalog, settled,
      Vector(p1, p2, p3), Vector.empty), Vector(p2))
```

`offer(p2)` with no advisers is an empty row to p2, which is valid
terms. In the limit test p1 holds 1 adviser and p2 holds 2 under a limit
of 2, so only the offer of Council Arbiter breaks a limit.

In `NegotiationStartFactsSuite`, add:

```scala
  test("the adviser users round trip, and open the rules"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector.empty,
      Vector(p3), Vector(p2))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p2)).advisersOpen, true)
    assertEquals(facts.rules(Vector(p1, p3)).adviserTraders, Vector(p3))
    assertEquals(facts.rules(Vector(p1, p3)).advisersOpen, false)
    assert(NegotiationStartFacts.read(Vector(DecisionOptionRef.Player(p2),
      DecisionOptionRef.Button(NegotiationStartFacts.Marker))).isLeft)
```

In `NegotiationDealSuite`, import
`distantlands.gameplay.actions.negotiation.DealRules`,
`distantlands.gameplay.powers.AdviserLimit` and
`distantlands.gameplay.setup.FirstGameSetupFixture.catalog` (if slice 2
did not already), and change every `NegotiationDeal.snapshot(ready, deal)`
(and every slice 2 `snapshot(ready, deal, ...)`) to
`NegotiationDeal.snapshot(catalog, ready, deal, DealRules(), AdviserLimit)`.
Find them with `git grep -n "snapshot(" -- src/test/scala`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationAdviserTermsSuite distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationDealSuite"`
Expected: compile errors: `adviserRecipients`, `overAdviserLimit` and
`DealRules` not found.

- [ ] **Step 3: The limit seam**

Create `src/main/scala/distantlands/gameplay/actions/AdviserLimits.scala`:

```scala
package distantlands.gameplay.actions

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** A player's adviser limit, read from state, for a rule outside card play
  * that moves advisers between players: the Negotiation's adviser terms
  * (catalog batch 12, ruling 17). Which held cards narrow the limit, lift
  * it or leave an adviser uncounted is the powers' knowledge
  * (`gameplay.powers.AdviserLimit`), and actions may not import powers, so
  * the walker's procedure registry hands that one implementation in.
  */
trait AdviserLimits:
  /** How many advisers `player` may hold. */
  def of(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId): Int
  /** How many of `player`'s advisers count toward the limit. */
  def towardLimit(ready: ReadyGame, player: PlayerId): Int
  /** Whether `player` holds more counted advisers than their limit. */
  final def exceeded(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId): Boolean =
    towardLimit(ready, player) > of(catalog, ready, player)
```

In `AdviserLimit.scala`, change the import to
`import distantlands.gameplay.actions.{AdviserFace, AdviserLimits, CardPlayRules}`,
declare `object AdviserLimit extends AdviserLimits:`, and add to its doc
comment: "It is the one [[AdviserLimits]], which the Negotiation reads
through the walker's registry."

- [ ] **Step 4: The deal rules**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.model.PlayerId

/** A kind of term an author may put into a row for one recipient (catalog
  * batch 12, "The deal model"). Secrets and sites join with the slices
  * that move them. */
enum TermKind:
  case Favor, Relic, Adviser

/** Who may put what into one deal (catalog batch 12, "The deal model"):
  * the one place that says it. Built when the deal opens, from the
  * Negotiation's start facts ([[NegotiationStartFacts.rules]]).
  *
  * `adviserTraders` may give and receive advisers with anyone (Council
  * Arbiter, ruling 14). `advisersOpen` lets every pair trade them
  * (Festival District, ruling 15). `bindingPromises` opens binding
  * promises to every author (The Tribunal, ruling 11).
  */
final case class DealRules(adviserTraders: Vector[PlayerId] = Vector.empty,
    advisersOpen: Boolean = false, bindingPromises: Boolean = false):
  def kinds(author: PlayerId, recipient: PlayerId): Set[TermKind] =
    val printed: Set[TermKind] = Set(TermKind.Favor, TermKind.Relic)
    if advisersOpen || adviserTraders.contains(author) ||
        adviserTraders.contains(recipient) then printed + TermKind.Adviser
    else printed

  /** The participants `author` may give advisers, in participant order. */
  def adviserRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Adviser))
```

- [ ] **Step 5: Store the adviser users**

Replace `NegotiationStartFacts.scala` (slice 2's file) with:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.model._

/** What a Negotiation's start settles for its whole walk, stored as
  * `walkerStartFacts` (catalog batch 12, slices 2 and 3): the players the
  * actor may choose (`candidates`), and the players among the actor and
  * the candidates who may use The Tribunal (`tribunalUsers`), Council
  * Arbiter (`arbiterUsers`) and Festival District (`festivalUsers`).
  *
  * Each reads the walk's power-use restrictions, which a rebuild does not
  * carry, and each could change once the deal settles: Citizenship changes
  * a role, which Bandit Forest reads, and an adviser term can move Council
  * Arbiter. So the start computes them, and every rebuild reads them back,
  * as Travel reads its source.
  *
  * Stored as option refs: each candidate as a player, then for each user
  * list a `Button` holding its marker followed by its players.
  */
final case class NegotiationStartFacts(candidates: Vector[PlayerId],
    tribunalUsers: Vector[PlayerId],
    arbiterUsers: Vector[PlayerId] = Vector.empty,
    festivalUsers: Vector[PlayerId] = Vector.empty):
  def refs: Vector[DecisionOptionRef] =
    candidates.map(DecisionOptionRef.Player(_)) ++
      NegotiationStartFacts.Markers.zip(Vector(tribunalUsers, arbiterUsers,
        festivalUsers)).flatMap((marker, users) =>
        DecisionOptionRef.Button(marker) +: users.map(DecisionOptionRef.Player(_)))

  /** Whether a deal among `participants` takes binding promises: one of
    * them may use The Tribunal (ruling 11). */
  def bindingPromises(participants: Vector[PlayerId]): Boolean =
    participants.exists(tribunalUsers.contains)

  /** What a deal among `participants` allows (rulings 11, 14 and 15). */
  def rules(participants: Vector[PlayerId]): DealRules =
    DealRules(arbiterUsers.filter(participants.contains),
      festivalUsers.exists(participants.contains),
      bindingPromises(participants))

object NegotiationStartFacts:
  /** The key of the `Button` ref before the Tribunal's users. */
  val Marker: String = "negotiation.tribunal-users"
  val ArbiterMarker: String = "negotiation.arbiter-users"
  val FestivalMarker: String = "negotiation.festival-users"
  private val Markers: Vector[String] =
    Vector(Marker, ArbiterMarker, FestivalMarker)

  def at(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction]): NegotiationStartFacts =
    val candidates = NegotiationDeal.eligible(catalog, ready, actor,
      restrictions)
    val players = actor +: candidates
    NegotiationStartFacts(candidates,
      NegotiationReach.tribunalUsers(catalog, ready, players, restrictions),
      NegotiationAdvisers.arbiterUsers(catalog, ready, players, restrictions),
      NegotiationAdvisers.festivalUsers(catalog, ready, players,
        restrictions))

  /** The facts `refs` store: every marker once, in order, with only
    * players around them. */
  def read(refs: Vector[DecisionOptionRef])
      : Either[OathViolation, NegotiationStartFacts] =
    sections(refs).toRight(malformed(refs)).flatMap(parts =>
      parts.foldLeft[Either[OathViolation, Vector[Vector[PlayerId]]]](
        Right(Vector.empty))((done, part) =>
        done.flatMap(lists => players(part, refs).map(lists :+ _))))
      .map(lists => NegotiationStartFacts(lists(0), lists(1), lists(2),
        lists(3)))

  /** `refs` cut at each marker in order, or `None` when one is missing. */
  private def sections(refs: Vector[DecisionOptionRef])
      : Option[Vector[Vector[DecisionOptionRef]]] =
    Markers.foldLeft(Option((Vector.empty[Vector[DecisionOptionRef]], refs))) {
      case (Some((done, rest)), marker) =>
        val (before, after) = rest.span(_ != DecisionOptionRef.Button(marker))
        Option.when(after.nonEmpty)((done :+ before, after.drop(1)))
      case (None, _) => None
    }.map((done, rest) => done :+ rest)

  private def players(part: Vector[DecisionOptionRef],
      refs: Vector[DecisionOptionRef]): Either[OathViolation, Vector[PlayerId]] =
    val ids = part.collect { case DecisionOptionRef.Player(id) => id }
    Either.cond(ids.size == part.size, ids, malformed(refs))

  private def malformed(refs: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} has malformed start " +
        s"facts: ${refs.map(_.kind).mkString(", ")}")
```

A marker out of place lands inside a section and fails `players`, so it
is malformed too.

- [ ] **Step 6: Bounds, limit and settlement**

In `NegotiationTerms.scala`, give `NegotiationBounds` two last fields:

```scala
    /** The advisers the author may give (no printed restriction refuses
      * them), offered only to `adviserRecipients` (catalog batch 12,
      * rulings 14 to 16). */
    advisers: Vector[WorldCardId] = Vector.empty,
    adviserRecipients: Vector[PlayerId] = Vector.empty
```

In `Decisions.scala`, give `DecisionQuery.Negotiate` a last field after
`heading`:

```scala
    /** The participants the terms would leave over their adviser limit.
      * While it is not empty nobody may accept (ruling 17). */
    overAdviserLimit: Vector[PlayerId] = Vector.empty
```

In `NegotiationDeal.scala`, add imports
`distantlands.gameplay.actions.AdviserLimits` and change the operations
import to `distantlands.gameplay.operations.{OperationRestrictions,
TheGrandScepterCard}`. Then replace `snapshot` and the private `bounds`:

```scala
  /** The question the deal asks now: everyone's terms, who accepted, what
    * each author may still offer under `rules`, and who may accept (anyone
    * who has not, once the deal has substance and leaves nobody over their
    * adviser limit). */
  def snapshot(catalog: ExecutableCatalog, state: ReadyGame, deal: DealState,
      rules: DealRules, limits: AdviserLimits): DecisionQuery.Negotiate =
    val over = overAdviserLimit(catalog, state, deal, limits)
    DecisionQuery.Negotiate(deal.participants, deal.terms, deal.accepted,
      deal.participants.map(author => author ->
        bounds(catalog, state, author, deal.participants, rules)).toMap,
      if deal.hasSubstance && !deal.declined && over.isEmpty then
        deal.participants.filterNot(deal.accepted).toSet
      else Set.empty[PlayerId],
      heading = Some("Negotiation"), overAdviserLimit = over)

  private def bounds(catalog: ExecutableCatalog, state: ReadyGame,
      author: PlayerId, participants: Vector[PlayerId],
      rules: DealRules): NegotiationBounds =
    val player = state.game.current.player(author).get
    val exiles = offerable(state, author, participants)
    val ignored = Citizenship.ignored(state)
    val adviserRecipients = rules.adviserRecipients(author, participants)
    NegotiationBounds(participants.filter(_ != author),
      SpendableFavor.of(state, author),
      player.relics.map(_.id), disclosable(state, player),
      citizenship = exiles,
      citizenshipRefused = Option.when(ignored && exiles.nonEmpty)(
        Citizenship.ignoredReason),
      bindingPromises = rules.bindingPromises,
      advisers = adviserRecipients.headOption.fold(Vector.empty)(
        movable(catalog, state, player, _)),
      adviserRecipients = adviserRecipients)

  /** `player`'s advisers that may change hands: those no printed
    * restriction refuses to give, which keeps a faceup locked adviser and
    * an Ancient Bloodline denizen in place (ruling 16). A facedown card
    * shows no lock. Which recipient is judged makes no difference: no
    * printed restriction reads a play-area move's destination for an
    * adviser. */
  private def movable(catalog: ExecutableCatalog, state: ReadyGame,
      player: PlayerState, recipient: PlayerId): Vector[WorldCardId] =
    val printed = OperationRestrictions.forCatalog(catalog).printed
    player.advisers.collect {
      case DenizenState(id, _, _) => id: WorldCardId
      case VisionState(id, _) => id: WorldCardId
    }.filter(card => printed.forall(_.reason(state, Give(Piece.Card(card),
      player.player, Location.PlayArea(player.player),
      Location.PlayArea(recipient))).isEmpty))

  /** The participants the deal's adviser moves would leave over their
    * adviser limit, in participant order (ruling 17). The limit is read on
    * the state after the moves, so a moved card that changes a limit
    * counts. Only a player who gives or receives an adviser is judged. */
  def overAdviserLimit(catalog: ExecutableCatalog, state: ReadyGame,
      deal: DealState, limits: AdviserLimits): Vector[PlayerId] =
    val moves = adviserMoves(deal)
    val touched = moves.flatMap((from, to, _) => Vector(from, to)).toSet
    if touched.isEmpty then Vector.empty
    else
      val after = moves.foldLeft(state)(moved)
      deal.participants.filter(player => touched(player) &&
        limits.exceeded(catalog, after, player))

  /** Every adviser the deal moves: giver, recipient, card. */
  private def adviserMoves(deal: DealState)
      : Vector[(PlayerId, PlayerId, WorldCardId)] =
    deal.participants.flatMap(author =>
      deal.terms(author).transfers.flatMap(transfer =>
        transfer.advisers.map((author, transfer.recipient, _))))

  /** `state` with one adviser moved between play areas, face and tokens
    * kept: what the limit judges, not a move the game makes. */
  private def moved(state: ReadyGame,
      move: (PlayerId, PlayerId, WorldCardId)): ReadyGame =
    val (from, to, card) = move
    state.game.current.player(from).flatMap(_.advisers.find(_.id == card))
      .fold(state)(held => state.updateCurrent(current => current.copy(
        players = current.players.map {
          case seat if seat.player == from =>
            seat.copy(advisers = seat.advisers.filterNot(_.id == card))
          case seat if seat.player == to =>
            seat.copy(advisers = seat.advisers :+ held)
          case seat => seat
        })))
```

`ExecutableCatalog` is already imported by slice 2's `eligible`. If the
`Vector.empty` in `headOption.fold` does not infer, write
`Vector.empty[WorldCardId]`.

In `affordable`, after the relic check, add:

```scala
      _ <- Either.cond(terms.transfers.flatMap(_.advisers).forall(id =>
        player.advisers.exists(_.id == id)), (),
        OathViolation.NegotiationUnavailable(
          "an offered adviser is no longer held by its author"))
```

In `transfers`, after `relics`, add the advisers and return all three:

```scala
        // Required: a refused adviser rejects the settlement rather than
        // being skipped. The bounds never offer one a restriction refuses.
        val advisers = transfer.advisers.map(card => Give(Piece.Card(card),
          author, Location.PlayArea(author),
          Location.PlayArea(transfer.recipient), required = true): CoreOperation)
        favor ++ relics ++ advisers
```

Update `settle`'s doc comment: "then every transfer moves: favor, relics,
then advisers, each keeping its face."

- [ ] **Step 7: Validate adviser terms**

In `DecisionQueries.acceptsTerms`, after the relic check, add:

```scala
          _ <- require(advisers.distinct.size == advisers.size &&
            advisers.forall(own.advisers.contains) &&
            terms.transfers.forall(transfer => transfer.advisers.isEmpty ||
              own.adviserRecipients.contains(transfer.recipient)),
            decisionId, "offers an adviser it cannot")
```

with `val advisers = terms.transfers.flatMap(_.advisers)` beside
`val relics`.

- [ ] **Step 8: Thread the limits through the procedure**

In `NegotiationProcedure.scala`, add the import
`distantlands.gameplay.actions.AdviserLimits`, and change slice 2's
`build`, `rebuild` and `tree` heads:

```scala
  def build(limits: AdviserLimits,
      restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts = NegotiationStartFacts.at(catalog, state, actor, restrictions)
      _ <- Either.cond(facts.candidates.nonEmpty, (),
        OathViolation.NegotiationUnavailable(
          "no other player is at your site or within reach"))
    yield tree(catalog, limits, actor, facts)

  /** A resume rebuilds what the start built, from the facts it stored. */
  def rebuild(limits: AdviserLimits)(catalog: ExecutableCatalog,
      state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts <- NegotiationStartFacts.read(state.game.current.walkerStartFacts)
    yield tree(catalog, limits, actor, facts)
```

`tree(actor, facts)` becomes `tree(catalog: ExecutableCatalog, limits:
AdviserLimits, actor: PlayerId, facts: NegotiationStartFacts)`, and
inside it the snapshot becomes:

```scala
            NegotiationDeal.snapshot(catalog, ready, deal,
              facts.rules(deal.participants), limits),
```

In `transferred`, add each adviser's line after the relics:

```scala
          transfer.relics.map(relic => gaveCard.sourceless(
            NoteArg.Player(author), NoteArg.Card(relic),
            NoteArg.Player(transfer.recipient))) ++
          transfer.advisers.map(card => gaveCard.sourceless(
            NoteArg.Player(author), NoteArg.Card(card),
            NoteArg.Player(transfer.recipient)))))
```

and change its doc comment to "each author's favor, then relics, then
advisers. A facedown adviser reads as its back to every player but the
two who held it (`LogWords.seen`)."

In the object's doc comment, after slice 2's start-facts sentence, add:
"The adviser limit is the powers' knowledge, so the registry passes
`AdviserLimit` to `build` and `rebuild`."

- [ ] **Step 9: Pass `AdviserLimit` from the registry**

In `WalkerProcedureRegistry.scala`, add
`import distantlands.gameplay.powers.AdviserLimit` and change slice 2's
`negotiationEntry`:

```scala
  /** Negotiation reads the power-use restrictions at its start: The
    * Tribunal, Traveling Negotiator, Council Arbiter and Festival District
    * reach no further than their users may use them (catalog batch 12,
    * rulings 24 and 26). Its deal reads the adviser limit, which only the
    * powers know (ruling 17). A resume reads the start facts and needs no
    * powers. */
  private def negotiationEntry(powers: WalkerPowers): Entry = Entry(
    build = NegotiationProcedure.build(AdviserLimit, powers.useRestrictions),
    rebuild = NegotiationProcedure.rebuild(AdviserLimit),
    startFacts = NegotiationProcedure.startFacts(powers.useRestrictions))
```

Check that nothing else builds the Negotiation directly:

Run: `git grep -n "NegotiationProcedure.build\|NegotiationProcedure.rebuild" -- src`
Expected: only the two lines above.

- [ ] **Step 10: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationAdviserTermsSuite distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.walker.WalkerProcedureRegistrySuite distantlands.gameplay.walker.DecisionQuerySuite"`
Expected: PASS.

Then run every suite that drives a Negotiation:

Run: `./sbtw "testOnly *Negotiation* *RoleChanges* *GameLog* *HornedMask*"`
Expected: PASS. The golden logs do not change: no existing script moves
an adviser.

Then: `python3 scripts/check-architecture.py`
Expected: "architecture check passed".

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/AdviserLimits.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala \
  src/main/scala/distantlands/gameplay/powers/AdviserLimit.scala \
  src/main/scala/distantlands/model/NegotiationTerms.scala \
  src/main/scala/distantlands/model/Decisions.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala \
  src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala \
  src/test/scala/distantlands/gameplay/NegotiationAdviserTermsSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): adviser terms under Council Arbiter and Festival District

The start stores who may use each card. DealRules opens adviser rows
for the pairs they allow, the bounds offer only advisers no printed
restriction refuses, and nobody may accept while the terms would leave
a participant over their adviser limit. Settlement gives each adviser
with its face kept.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: The projection and the panel

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/NegotiationDealProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Test: `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`,
  `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`,
  `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`

**Interfaces:**
- Consumes: Task 3's bounds fields and `Negotiate.overAdviserLimit`.
- Produces:
  - `NegotiationTransferProjection` gains `adviserCount: Int = 0` and
    `advisers: Vector[CardDetailsProjection] = Vector.empty`.
  - `NegotiationEditingProjection` gains `giftableAdvisers:
    Vector[CardDetailsProjection] = Vector.empty`, `adviserRecipients:
    Vector[String] = Vector.empty` and `overAdviserLimit: Vector[String] =
    Vector.empty`. (`editableAdvisers` stays: it holds the facedown
    advisers offered for disclosure.)

- [ ] **Step 1: Write the failing tests**

In `NegotiationDealProjectionSuite`, add imports
`distantlands.gameplay.actions.negotiation.CouncilArbiterCard`,
`distantlands.gameplay.powers.economy.BirdsongCard` and
`distantlands.gameplay.powers.rest.SilverTongueCard`, and add:

```scala
  test("a facedown adviser on offer is named to its author and counted " +
      "for everyone else"):
    val birdsong: WorldCardId = BirdsongCard.id
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
      .adviser(p1, BirdsongCard.id, facedown = true)
    val state = parkedDeal(table, who = Vector(p2, p3), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector(birdsong))))))
    val own = deal(view(state, p1)).transfers.head
    assertEquals((own.adviserCount, own.advisers.map(_.cardId)),
      (1, Vector(birdsong.value)))
    Vector(p2, p3).foreach { viewer =>
      val seen = deal(view(state, viewer)).transfers.head
      assertEquals((seen.adviserCount, seen.advisers), (1, Vector.empty),
        viewer.value)
    }

  test("the editor offers each author's movable advisers to the recipients " +
      "the rules allow"):
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
    val state = parkedDeal(table, who = Vector(p2, p3))
    val p1Editing = deal(view(state, p1)).editing.getOrElse(fail("editing"))
    assertEquals(p1Editing.giftableAdvisers.map(_.cardId),
      Vector(CouncilArbiterCard.id.value))
    assertEquals(p1Editing.adviserRecipients, Vector(p2.value, p3.value))
    val p2Editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assertEquals(p2Editing.adviserRecipients, Vector(p1.value))

  test("terms over an adviser limit are projected with who they leave over"):
    // p2 holds Silver Tongue faceup (a limit of 2) and Birdsong; p1 offers
    // them Council Arbiter.
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
      .adviser(p2, SilverTongueCard.id)
      .adviser(p2, BirdsongCard.id, facedown = true)
    val state = parkedDeal(table, who = Vector(p2), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector(CouncilArbiterCard.id))))))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing.getOrElse(fail("editing"))
      assertEquals(editing.overAdviserLimit, Vector(p2.value), viewer.value)
      assert(!editing.canAccept, viewer.value)
    }
```

In `ProjectionProtocolSuite`'s deal round trip, set the new fields to non-default
values in the deal it round-trips: a transfer with `adviserCount = 1` and
one adviser card, and editing with one giftable adviser,
`adviserRecipients = Vector("blue")` and `overAdviserLimit =
Vector("blue")`.

In `NegotiationDealPanelSuite`, add:

```scala
  private val giftable = CardDetails("d7", "denizen", "Tinker",
    orientation = Some("face-up"))

  test("an adviser is offered only to the recipients the rules allow, and " +
      "Save sends it as a world card"):
    val deal = threeWay(NegotiationEditingState(3, Vector.empty, Vector.empty,
      Vector.empty, canAccept = false, giftableAdvisers = Vector(giftable),
      adviserRecipients = Vector("blue")))
    val ui = new RecordingControls()
    val panel = render(owner(deal), ui)
    assert(Dom.all(panel, "[aria-label='Give Tinker to yellow']").isEmpty)
    box(panel, "Give Tinker to blue").checked = true
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(
          terms)) => terms }
    assertEquals(sent.flatMap(_.transfers).flatMap(_.advisers),
      Vector(distantlands.protocol.WorldCard("denizen", "d7")))

  test("a transfer names the advisers the viewer can see and counts the rest"):
    val base = dealWhereBlueHasAccepted()
    val panel = render(spectator(base.copy(transfers = Vector(
      NegotiationTransferState(authorPlayerId = "blue",
        recipientPlayerId = "red", favor = 0, relicCount = 0,
        relics = Vector.empty, adviserCount = 2,
        advisers = Vector(giftable))))), new RecordingControls())
    assertEquals(Dom.one(panel, ".negotiation-transfer").textContent,
      "blue gives red: Tinker, 1 facedown adviser")

  test("terms over an adviser limit withhold Accept with the reason"):
    val base = dealWhereBlueHasAccepted(canAccept = false)
    val panel = render(owner(base.copy(editing = base.editing.map(_.copy(
      overAdviserLimit = Vector("blue"))))), new RecordingControls())
    val accept = button(panel, ".negotiation-accept")
    assertEquals(accept.getAttribute("aria-disabled"), "true")
    assertEquals(accept.getAttribute("data-tip"),
      "These terms would leave blue over their adviser limit.")
```

`threeWay` (red, blue and yellow), `box`, `button`, `RecordingControls`
and `ui.submitted` (a `Vector[Intent]`) are the suite's existing helpers.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: compile errors: `adviserCount`, `giftableAdvisers` not found.

- [ ] **Step 3: Widen the DTOs and their codec**

In `ActionProjectionDtos.scala`, give `NegotiationTransferProjection` two
last fields `adviserCount: Int = 0, advisers:
Vector[CardDetailsProjection] = Vector.empty`, and
`NegotiationEditingProjection` three last fields:

```scala
    /** The advisers the viewer may give, and to whom (Council Arbiter,
      * Festival District). */
    giftableAdvisers: Vector[CardDetailsProjection] = Vector.empty,
    adviserRecipients: Vector[String] = Vector.empty,
    /** The participants the current terms would leave over their adviser
      * limit; while any is, nobody may accept. */
    overAdviserLimit: Vector[String] = Vector.empty
```

Update both doc comments that list what everyone sees: "relic and adviser
counts and the details of faceup relics and advisers".

In `ActionProjectionCodec.scala`:

- `encodeTransfer` adds `"adviserCount" -> row.adviserCount,
  "advisers" -> encoded(row.advisers)(encodeCard)`.
- `decodeTransfer`'s `exact` set adds `"adviserCount", "advisers"`, and it
  reads `advisers <- int(row, "adviserCount", child)` style fields:

```scala
    adviserCount <- int(row, "adviserCount", child)
    adviserRaws <- array(row, "advisers", child)
    advisers <- traverse(adviserRaws, s"$child.advisers")(decodeCard)
  yield NegotiationTransferProjection(author, recipient, favor, count, relics,
    adviserCount, advisers)
```

- `encodeDeal`'s editing object adds `"giftableAdvisers"`,
  `"adviserRecipients"` and `"overAdviserLimit"` beside
  `"bindingPromisesOpen"`, encoded as the neighbouring card list and
  string lists are.
- `decodeDeal`'s editing `exact` set adds the three keys, and it reads:

```scala
      giftableRaws <- array(row, "giftableAdvisers", child)
      giftable <- traverse(giftableRaws, s"$child.giftableAdvisers")(decodeCard)
      adviserRecipients <- strings(row, "adviserRecipients", child)
      overLimit <- strings(row, "overAdviserLimit", child)
    yield NegotiationEditingProjection(favor, relics, advisers, sites, canAccept,
      offerable, refused, open, giftable, adviserRecipients, overLimit) }
```

- [ ] **Step 4: Project them**

In `NegotiationDealProjector.project`, build each transfer's advisers as
its relics are built, from `owner.advisers`:

```scala
        val advisers = transfer.advisers.flatMap(id =>
          owner.advisers.find(_.id == id)).collect {
          case adviser if viewer.contains(author) ||
              presentation.adviserOrientation(adviser) == Orientation.FaceUp =>
            presentation.cardDetails(adviser.id,
              Some(presentation.adviserOrientation(adviser)), hidden = false)
        }
        NegotiationTransferProjection(author.value, transfer.recipient.value,
          transfer.favor, transfer.relics.size, relicDetails,
          transfer.advisers.size, advisers)
```

(`relicDetails` is the expression the constructor takes today; name it.)
`adviserOrientation(card: AdviserState): Orientation` is a public method
of `GamePresentationProjector`.

In `editing`, add the three fields:

```scala
      giftableAdvisers = own.advisers.flatMap(id =>
        player.advisers.find(_.id == id)).map(adviser =>
        presentation.cardDetails(adviser.id,
          Some(presentation.adviserOrientation(adviser)), hidden = false)),
      adviserRecipients = own.adviserRecipients.map(_.value),
      overAdviserLimit = query.overAdviserLimit.map(_.value)
```

Update the class doc comment the same way as the DTO's.

- [ ] **Step 5: Render them**

In `NegotiationDealPanel.transferLine`, after the relic parts, add the
advisers and their facedown count:

```scala
    val hiddenAdvisers = t.adviserCount - t.advisers.size
    val parts: Vector[dom.Node] =
      Option.when(t.favor > 0)(textNode(s"${t.favor} favor")).toVector ++
        t.relics.map(cardLink) ++
        Option.when(hidden > 0)(textNode(
          s"$hidden facedown ${if hidden == 1 then "relic" else "relics"}")) ++
        t.advisers.map(cardLink) ++
        Option.when(hiddenAdvisers > 0)(textNode(s"$hiddenAdvisers facedown " +
          s"${if hiddenAdvisers == 1 then "adviser" else "advisers"}"))
```

and add "advisers" to its doc comment.

In `editor`, add a buffer beside `relics`:

```scala
    val advisers = scala.collection.mutable.ArrayBuffer.empty[
      (String, CardDetails, dom.html.Input)]
```

and, inside the per-recipient loop after the relic rows:

```scala
      if editing.adviserRecipients.contains(recipient) then
        editing.giftableAdvisers.foreach { adviser =>
          val check = input("checkbox")
          check.setAttribute("aria-label", s"Give ${adviser.name} to $recipient")
          check.checked = deal.transfers.exists(t => t.authorPlayerId == me &&
            t.recipientPlayerId == recipient &&
            t.advisers.exists(_.cardId == adviser.cardId))
          // An adviser goes to one recipient at most.
          check.onchange = _ => if check.checked then advisers.foreach:
            case (other, otherAdviser, otherCheck)
                if other != recipient && otherAdviser.cardId == adviser.cardId =>
              otherCheck.checked = false
            case _ => ()
          panel.appendChild(cardItem(check, "Give ", adviser))
          advisers += ((recipient, adviser, check))
        }
```

A facedown adviser the author holds reaches the editor with its name, so
`check.checked` restores from the author's own transfer, which carries
the details for its author.

In Save, the transfer input gains its advisers:

```scala
        NegotiationTransferInput(recipient, favor.value.toIntOption.getOrElse(0),
          relics.collect { case (`recipient`, relic, check) if check.checked =>
            relic }.toVector,
          advisers.collect { case (`recipient`, adviser, check)
              if check.checked => distantlands.protocol.WorldCard(
                adviser.cardKind, adviser.cardId) }.toVector)
```

Replace the Accept button's `disabled` line with:

```scala
    if editing.overAdviserLimit.isEmpty then
      accept.disabled = !canControl || !editing.canAccept
    else
      // Withheld with its reason, not `disabled`: a disabled button takes
      // no pointer event, so a tap could not show the reason (HoverTip).
      val reason = "These terms would leave " +
        s"${editing.overAdviserLimit.mkString(" and ")} over their " +
        "adviser limit."
      accept.setAttribute("aria-disabled", "true")
      HoverTip.set(accept, reason)
```

and make its `onclick` submit only while it is not withheld:

```scala
    accept.onclick = _ => if editing.overAdviserLimit.isEmpty then
      controls.submitCommand(GameCommand.ResolveWalker(decisionId,
        DecisionAnswerWire.AcceptDealWire))
```

If `cardItem` with a "Give " prefix changes the first panel test's row
count, it does not: that test's editing offers no adviser.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala \
  src/main/scala/distantlands/application/NegotiationDealProjector.scala \
  frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala \
  src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala \
  frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala
```

```bash
git add shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): offer, show and withhold adviser terms in the panel

The editor offers each movable adviser to the recipients the rules
allow. A facedown adviser on offer is named only to its author. Accept
is withheld with its reason while the terms break an adviser limit.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: The Game Log, docs and verification

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`
- Create: `src/test/resources/gamelog/negotiation-adviser-given.actor.log`, `.other.log`
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md` (status line)

- [ ] **Step 1: Write the script and the failing test**

In `NegotiationLogScripts.scala`, import
`distantlands.gameplay.actions.negotiation.CouncilArbiterCard`, and add:

```scala
  /** The partner gives the actor, who holds Council Arbiter, a facedown
    * adviser (catalog batch 12, ruling 16). */
  def negotiationAdviserGiven(using munit.Location): Script =
    val birdsong: WorldCardId = BirdsongCard.id
    val (service, parked, actor, partner) = negotiating(
      "negotiation-adviser-given", _.adviser(p1, CouncilArbiterCard.id)
        .adviser(p2, BirdsongCard.id, facedown = true))
    parked
      .parkedAfter(deal(partner, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(actor, 0, Vector.empty, Vector(birdsong)))))))
      .parkedAfter(deal(actor, AcceptDeal))
      .after(deal(partner, AcceptDeal))
    Script("negotiation-adviser-given", service, actor)
```

Extend the object's doc comment: "…a facedown adviser disclosed, and a
facedown adviser given."

In `LogScripts.scala`, add `negotiationAdviserGiven` to the import from
`NegotiationLogScripts`, and to `named`:
`"negotiation-adviser-given" -> (() => negotiationAdviserGiven),`.

In `GameLogExchangeSuite`, add:

```scala
  test("a given facedown adviser is named to both who held it, a back to " +
      "others"):
    val script = negotiationAdviserGiven
    val gave = (viewer: Option[PlayerId]) =>
      val all = texts(format(script, viewer))
      all.find(_.contains(" gave ")).getOrElse(
        fail(s"no transfer line for $viewer in $all"))
    val (giver, recipient) = (playerLabel(partner), playerLabel(script.actor))
    Vector(Some(script.actor), Some(partner)).foreach(viewer =>
      assertEquals(gave(viewer), s"$giver gave Birdsong to $recipient",
        viewer.toString))
    assertEquals(gave(Some(bystander)), s"$giver gave a Denizen to $recipient")
```

- [ ] **Step 2: Run the test and write the golden logs**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: PASS (the line comes from Task 3). If the giver reads "a
Denizen", check `LogWords.seen`'s states before changing anything: the
giver identifies the card in the state before the run.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Then: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite distantlands.application.gamelog.GameLogPropertiesSuite"`
Expected: PASS. Read both new `.log` files: the actor's names Birdsong,
and the other seat's names it too if the other seat is the partner, or
reads "a Denizen" if it is a bystander. No other golden file changes
(`git status` shows only the two new ones).

- [ ] **Step 3: Docs**

In `docs/architecture/all-exile-negotiation.md`:

- Remove Council Arbiter and Festival District from the ignored-handlers
  list.
- Add to the deal model's description: "A transfer row may hold advisers
  where `DealRules` allows them: Council Arbiter's faceup holder trades
  advisers with anyone, and a participant with access to Festival
  District opens them between every pair. The start stores who may use
  each card, under the power-use restrictions (`NegotiationStartFacts`).
  The bounds offer only the advisers no printed restriction refuses to
  give. Nobody may accept while the terms would leave a participant over
  their adviser limit (`DecisionQuery.Negotiate.overAdviserLimit`), read
  through `AdviserLimits`, which the registry fills with the powers'
  `AdviserLimit`. Settlement gives each adviser after the favor and
  relics, keeping its face."

In the spec, set the status line to "Slices 1 to 3 landed."

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken links.

- [ ] **Step 4: Full verification**

Run: `./sbtw clean coverage test coverageReport`
Expected: every suite passes, statement coverage at or above 92%.

Run: `python3 scripts/check-architecture.py`
Expected: "architecture check passed".

The panel change is covered by `NegotiationDealPanelSuite` under the
frontend's jsdom test run; no browser check is needed for this slice.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/LogScripts.scala \
  src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala \
  src/test/resources/gamelog/negotiation-adviser-given.actor.log \
  src/test/resources/gamelog/negotiation-adviser-given.other.log \
  docs/architecture/all-exile-negotiation.md \
  docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 12 slice 3, adviser terms

A golden log script gives a facedown adviser under Council Arbiter: its
line names the card to both holders and its back to everyone else.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage.** Rulings 14 (Task 3, "only where its holder gives or
  receives"), 15 (Task 3, Festival District test), 16 (Task 3, locked and
  facing tests; Task 5, the log's knowledge rule), 17 (Task 3 limit test,
  Task 4 withheld Accept), 26 (Task 1 blocker tests). The spec's slice 3
  tests: holder on one side, Festival District for all, a locked adviser
  never offered, a limit-breaking deal refused with its reason, a
  facedown adviser staying facedown and known, and a faceup adviser
  keeping its power. Catalog: both cards leave holding, and Squalid
  District's comment is updated (Task 1).
- **Names.** `NegotiationAdvisers.arbiterUsers` / `festivalUsers` (Task 1)
  are what `NegotiationStartFacts.at` calls (Task 3).
  `NegotiationTransfer.advisers` (Task 2) is what Tasks 3 to 5 read.
  `NegotiationBounds.advisers` / `adviserRecipients` and
  `Negotiate.overAdviserLimit` (Task 3) are what Task 4 projects.
- **Not in this slice.** `DealRules`' private and prefix fields, and the
  `Secret` and `Site` kinds (slices 4 to 6).
```
