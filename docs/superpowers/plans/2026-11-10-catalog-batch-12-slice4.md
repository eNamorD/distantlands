# Catalog Batch 12, Slice 4: Site Terms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Deed Writer put sites into a Negotiation deal: the old ruler
takes their warbands home, and each new ruler places warbands from their
board.

**Architecture:** One rule object beside the action, `NegotiationSites`,
says who may use Deed Writer, which sites an author may give and to whom,
and what settling a site does. The Negotiation's start stores Deed
Writer's users in its start facts, beside slice 3's adviser users, and
`DealRules` opens the `Site` kind for the pairs they sit in. A transfer
row gains `sites`. Settlement returns each given site's warbands to its
old ruler in a required step, then asks each new ruler, one site at a
time, how many warbands to place.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../specs/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (18 to 20, 24, 25, 27 to 29), "The deal model" and
"Slice 4: Site terms".

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
  `siteIds` keys are required.
- Ruling 18: a site moves only from a player whose own-colour warbands
  hold it, to a player who will have a warband to place there. The Bandit
  Crown plays no part.
- Ruling 19: no site moves between two Imperial players.
- Ruling 20: the old ruler moves all their warbands at the site to their
  board. After every transfer, each new ruler places at least 1 of their
  board's warbands there. Denizens, relics, pawns and other forces stay.
- Ruling 27: City Wall, Giant Python and Bandit Forest stop Deed Writer,
  judged as its holder's own use, with no consent asked.
- Ruling 28: any Imperial player may give an Imperial site, on any
  player's turn, and takes its warbands to their own board.
- Ruling 29: a recipient needs one warband on their board for every site
  they receive, counted after their own given sites' warbands come back.
  While anyone would be short, nobody may accept, and the reason shows.
- The package is `distantlands`. Paths below are at `c2e1bff56` plus
  slice 3 (`docs/superpowers/plans/2026-11-10-catalog-batch-12-slice3.md`).
  This plan anchors edits by code, not by line.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Setup

Slice 3 must be merged into `0.2.0-alpha` first. This plan uses its
`DealRules` and `TermKind`, `NegotiationStartFacts` with
`arbiterUsers`/`festivalUsers` and `rules(participants)`, the
`NegotiationTransfer.advisers` field and its wire, journal and mapper
code, `NegotiationBounds.advisers`/`adviserRecipients`,
`DecisionQuery.Negotiate.overAdviserLimit`, the snapshot
`NegotiationDeal.snapshot(catalog, state, deal, rules, limits)`, the
procedure's `tree(catalog, limits, actor, facts)`, the panel's withheld
Accept, and `NegotiationReach.usable` made `private[negotiation]`. Check
with:

```bash
git grep -n "final case class DealRules\|enum TermKind" -- src/main/scala/distantlands/gameplay/actions/negotiation
git grep -n "overAdviserLimit" -- src/main/scala/distantlands/model/Decisions.scala
git grep -n "private\[negotiation\] def usable" -- src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationReach.scala
```

If slice 3 landed with different names, follow the names it landed with
and keep this plan's behaviour.

Work in a worktree off `0.2.0-alpha`, never in the main checkout. Symlink
the main checkout's `.tooling` and `node_modules` into the worktree before
running `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

## Plan-time findings

1. **Own colour is `PlayerForceKind`.** `PlayerForceKind.of` gives a
   player's own warband kind: the shared `ForceKind.Imperial` for a
   Citizen or the Chancellor, their lineage's kind for an Exile. A site an
   author may give is one whose `SiteForces.Occupied` kind is theirs. That
   one test states rulings 18 and 28 together: every Imperial player may
   give every Imperial site, and a bandit site is nobody's.
2. **An empty site waits safely.** The bandit refill runs only at the
   action boundary (`OathDriver.boundary`), so a site whose warbands went
   home stays empty while its new ruler answers.
3. **A `Move` is optional.** `Give` takes `required`, `Move` does not. An
   optional move that a restriction refuses is skipped, which here would
   leave two kinds at a site or a site empty. The returns and each
   placement are therefore `BuildOps(..., required = true)`, so a refusal
   rejects the step. The returns step is built only when the deal moves a
   site, so every existing walk journals exactly what it did before.
4. **Vow of Wandering's holder can receive no site.** Its holder "cannot
   place warbands on sites", and a new ruler must place at least 1, so its
   faceup holder is never offered one (a "cannot" beats a "may"). The
   restriction that enforces it is a walker power's, not a printed one, so
   the bounds read `VowOfWanderingRule.holds`, which sits in
   `gameplay.actions` for this kind of reader.
5. **Two authors can own one site.** The Chancellor and a Citizen in the
   same deal may each give the same Imperial site. An author's bounds
   leave out the sites another author's terms already give, so the deal
   never names one site twice.
6. **One decision id per site.** The walker keys answers by decision id,
   so each site's question has its own: `deed-writer.warbands.<site id>`,
   as Citizenship asks `role.replace-warbands.<player>`. Site ids read
   like `site:dunes`. Slice 6 holds several deals in one walk and must
   prefix the id with the deal's.
7. **A Branch is re-selected on resume.** Each question sits in a
   `Branch` so its bound reads the board when asked, and a resumed walk
   re-selects every Branch from current state. The bound is therefore
   computed from the board plus the answers already placed
   (`NegotiationSites.most`), which gives the same number before and after
   any park.
8. **The generic warband line would double the placement line.**
   `DetailLines` writes "Moved n warbands to <site>" for any warband move
   to a site outside Campaign, Move Warbands and Transfer Warbands. The
   Negotiation writes its own placement line, so it joins that list. No
   existing Negotiation moves a warband to a site, so no golden log
   changes.
9. **Roles are judged when the terms are made.** Ruling 19 reads the
   roles in play while the deal is open. A Citizenship offer in the same
   deal runs after the sites move (the settlement order), so an Exile who
   receives a Citizen's site and becomes a Citizen in the same deal is
   allowed: they were an Exile when it moved.
10. **The unsupported-rule test moves again.** Slice 3 switched
    `NegotiationProcedureSuite`'s "an unsupported Negotiation rule does
    not block the Negotiation" to Deed Writer. It now uses Whispering
    Stone (R34), held as a relic, which stays in holding until slice 5.
11. **A site is not a card.** `gaveCard` takes a card argument. Site
    lines get their own key, `gave-site`, with a `NoteArg.Site`, as
    `wasShown` names a site. Sites are public, so every viewer sees a
    site on offer by name.

## File structure

| File | Responsibility |
|---|---|
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSiteCards.scala` (new) | `DeedWriterCard` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala` (new) | who may use Deed Writer; giftable sites, recipients, returns, placements |
| `src/main/scala/distantlands/gameplay/powers/action/DeedWriter.scala` (new) | the registered power, hooking no window |
| `src/main/scala/distantlands/catalog/holding/HoldingHearthDenizens.scala` (deleted) | held only Deed Writer |
| `src/main/scala/distantlands/model/NegotiationTerms.scala` | `NegotiationTransfer.sites`; bounds' sites |
| `src/main/scala/distantlands/model/Decisions.scala` | `Negotiate.shortOfWarbands` |
| `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala` | journal `siteIds` |
| `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`, `CommandNestedCodecs.scala` | command wire `siteIds` |
| `src/main/scala/distantlands/application/GameIntentMapper.scala` | maps `siteIds` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala` | `TermKind.Site`, `siteTraders` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala` | `deedWriterUsers` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | bounds, acceptance, settle checks |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | returns, placements, log lines |
| `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` | validates site terms |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`, `ActionProjectionCodec.scala` | site rows and editing fields |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | projects them |
| `frontend/src/main/scala/distantlands/frontend/{NegotiationDealPanel,GameClient,ServerUiSupport,frontend}.scala` | site rows, the withheld Accept's second reason |
| `src/main/scala/distantlands/application/gamelog/DetailLines.scala` | no generic warband line in a Negotiation |
| `src/test/resources/powers/unimplemented.txt` | loses one line |
| `docs/architecture/all-exile-negotiation.md` | site terms |

---

### Task 1: Who may use Deed Writer

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSiteCards.scala`
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/action/DeedWriter.scala`
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingHearthDenizens.scala`
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify: `src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/powers/whenplayed/CallForHelpSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/action/DeedWriterUsersSuite.scala` (new)

**Interfaces:**
- Consumes: slice 2's `NegotiationReach.usable(catalog, ready, use,
  restrictions): Boolean` (`private[negotiation]` since slice 3),
  `PowerAccess.faceupAdviserHolder`, `PowerUse.of`.
- Produces:
  - `DeedWriterCard` (`DenizenId("146")`, `.power`), in
    `distantlands.gameplay.actions.negotiation`.
  - `NegotiationSites.deedWriterUsers(catalog: ExecutableCatalog, ready:
    ReadyGame, players: Vector[PlayerId], restrictions:
    Vector[PowerUseRestriction]): Vector[PlayerId]`.
  - Power `DeedWriter` in `distantlands.gameplay.powers.action`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/action/DeedWriterUsersSuite.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{DeedWriterCard,
  NegotiationSites}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Deed Writer (146): who may use it to put sites into a deal (catalog
  * batch 12, rulings 18 and 27). */
class DeedWriterUsersSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val everyone = Vector(p1, p2, p3)
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City. */
  private def apart: Table = Table.start.pawn(p1, at = peaks)
    .pawn(p2, at = DunesSite.id).pawn(p3, at = AncientCitySite.id)

  private def writers(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty,
      among: Vector[PlayerId] = everyone): Vector[PlayerId] =
    NegotiationSites.deedWriterUsers(catalog, table.ready, among,
      restrictions)

  test("Deed Writer's faceup holder may use it"):
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id)), Vector(p2))

  test("a facedown Deed Writer, or a holder outside the deal, gives no " +
      "user"):
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id,
      facedown = true)), Vector.empty[PlayerId])
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id),
      among = Vector(p1, p3)), Vector.empty[PlayerId])

  test("City Wall stops its ruler's enemy at its site from using Deed " +
      "Writer"):
    val walled = apart.adviser(p1, DeedWriterCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(writers(walled), Vector(p1))
    assertEquals(writers(walled, blockers), Vector.empty[PlayerId])

  test("Giant Python at its holder's site stops the use"):
    val python = apart.adviser(p1, DeedWriterCard.id)
      .denizen(GiantPythonCard.id, at = peaks)
    assertEquals(writers(python), Vector(p1))
    assertEquals(writers(python, blockers), Vector.empty[PlayerId])
```

`GiantPythonCard` is in `distantlands.gameplay.powers.action`, the
suite's own package.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.DeedWriterUsersSuite"`
Expected: compile error: `NegotiationSites` not found in
`distantlands.gameplay.actions.negotiation`.

- [ ] **Step 3: Move the card**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSiteCards.scala`
with the card object cut from holding, text unchanged:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{Denizen, PrintedPower}
import distantlands.model.{Cost, DenizenId, PowerId, Suit}

/** The card whose power lets sites into a Negotiation deal (catalog batch
  * 12, slice 4). It lives beside the Negotiation, which reads it
  * ([[NegotiationSites]]). Its power is registered in
  * `gameplay.powers.action`. */
object DeedWriterCard extends Denizen(DenizenId("146"), "Deed Writer", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.deed-writer"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may give, receive, and swap " +
      "sites. Old ruler moves warbands from site to their board, " +
      "and new ruler moves warbands to site from their board.")
  val powers: Vector[PrintedPower] = Vector(power)
```

Then:

- Delete `src/main/scala/distantlands/catalog/holding/HoldingHearthDenizens.scala`
  (`git rm`). It held only Deed Writer.
- In `NewFoundations.scala`, extend slice 3's
  `import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, TheTribunalCard, TravelingNegotiatorCard}` with
  `DeedWriterCard`, in alphabetical order. The catalog list keeps naming
  `DeedWriterCard` where it is.
- In `CallForHelpSuite.scala`, replace
  `import distantlands.catalog.holding.DeedWriterCard` with
  `import distantlands.gameplay.actions.negotiation.DeedWriterCard`.
- In `NegotiationProcedureSuite.scala`, replace slice 3's import
  `distantlands.catalog.holding.DeedWriterCard` with
  `distantlands.catalog.holding.WhisperingStoneCard`, and in the test "an
  unsupported Negotiation rule does not block the Negotiation" replace
  `gathered.adviser(p1, DeedWriterCard)` with
  `gathered.relic(p1, WhisperingStoneCard)`.

- [ ] **Step 4: Write the rule's first part**

Create `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Site terms in a Negotiation deal (catalog batch 12, rulings 18 to 20 and
  * 27 to 29). The Negotiation reads it at its start and while the deal is
  * open, so it sits beside the action, outside `powers`; the power that
  * registers Deed Writer hooks no window.
  *
  * Deed Writer (faceup adviser) lets its holder give and receive sites
  * ([[DealRules.kinds]] opens the pairs that hold them). It is
  * non-persistent, so its use passes `restrictions`, the walk's power-use
  * restrictions (City Wall, Giant Python, Bandit Forest), judged as its
  * holder's own use. The Negotiation asks for no consent.
  */
object NegotiationSites:
  /** The players among `players` who hold Deed Writer faceup and may use
    * it: at most one. */
  def deedWriterUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    PowerAccess.faceupAdviserHolder(ready, DeedWriterCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          DeedWriterCard.power, DeedWriterCard.id), restrictions))
```

Task 3 adds the rest of the object.

- [ ] **Step 5: Register the power**

Create `src/main/scala/distantlands/gameplay/powers/action/DeedWriter.scala`:

```scala
package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.DeedWriterCard
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Deed Writer (146): "**WHEN NEGOTIATING:** You may give, receive, and
  * swap sites. Old ruler moves warbands from site to their board, and new
  * ruler moves warbands to site from their board." (catalog batch 12,
  * rulings 18 to 20 and 27 to 29).
  *
  * The rule is `NegotiationSites` and `DealRules`, which the Negotiation
  * reads. This power hooks no window. It is registered so the card counts
  * as implemented. Its moves write the Negotiation's own lines.
  */
case object DeedWriter extends ContributingPower:
  val id: PowerId = DeedWriterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `WalkerPowerCatalog.contributing`, append `DeedWriter` after slice 3's
`FestivalDistrict`.

In `src/test/resources/powers/unimplemented.txt`, delete the line
`denizen.deed-writer`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.action.DeedWriterUsersSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.powers.whenplayed.CallForHelpSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.PowerKindsCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite"`
Expected: PASS.

If a blocker test fails, read that restriction's `refusal` before
changing anything: the test states ruling 27, and the fix belongs in
`NegotiationSites`, not in the restriction.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSiteCards.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala \
  src/main/scala/distantlands/gameplay/powers/action/DeedWriter.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala \
  src/test/scala/distantlands/gameplay/powers/whenplayed/CallForHelpSuite.scala \
  src/test/scala/distantlands/gameplay/powers/action/DeedWriterUsersSuite.scala
git rm src/main/scala/distantlands/catalog/holding/HoldingHearthDenizens.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): who may use Deed Writer

Deed Writer moves beside the Negotiation, which will read
NegotiationSites. Its use passes the power-use filter, judged as its
holder's use. The power registers with no window.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: A transfer row carries sites

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
- Consumes: slice 3's `NegotiationTransfer.advisers` in every layer.
- Produces:
  - `distantlands.model.NegotiationTransfer(recipient: PlayerId, favor:
    Int, relics: Vector[RelicId], advisers: Vector[WorldCardId] =
    Vector.empty, sites: Vector[SiteId] = Vector.empty)`.
  - `distantlands.protocol.NegotiationTransfer(recipientPlayerId: String,
    favor: Int, relicIds: Vector[String], advisers: Vector[WorldCard] =
    Vector.empty, siteIds: Vector[String] = Vector.empty)`.
  - `frontend.NegotiationTransferInput(recipientPlayerId: String, favor:
    Int, relicIds: Vector[String], advisers:
    Vector[distantlands.protocol.WorldCard] = Vector.empty, siteIds:
    Vector[String] = Vector.empty)`.

- [ ] **Step 1: Write the failing tests**

In `WalkerDecisionWireSuite`, test "walker choice steps round trip the
negotiation answers", change slice 3's transfer row to
`NegotiationTransfer(blue, 2, Vector(RelicId("r1")), Vector(DenizenId("d1"), VisionId("v2")), Vector(SiteId("site:dunes")))`.

In `CommandProtocolSuite`, in the round-tripped intents list, change
slice 3's `NegotiationTransfer("blue", 2, Vector("r1"), Vector(WorldCard("denizen", "d1")))`
to `NegotiationTransfer("blue", 2, Vector("r1"), Vector(WorldCard("denizen", "d1")), Vector("site:dunes"))`.
In slice 3's test "a transfer row that names one adviser twice is
refused", add `"siteIds":[]` after the `"advisers"` array, so the JSON
fails only on the repeated adviser. Then add beside it:

```scala
  test("a transfer row that names one site twice is refused"):
    val repeated = """{"expectedNextSequence":0,"intent":{"type":"resolveWalker","decisionId":"negotiation.deal","payload":{"kind":"propose-terms","terms":{"transfers":[{"recipientPlayerId":"p2","favor":0,"relicIds":[],"advisers":[],"siteIds":["site:dunes","site:dunes"]}],"disclosures":[],"citizenshipPlayerIds":[],"promises":[],"bindingPromises":[]}}}}"""
    assert(decode(repeated).isLeft, decode(repeated))
```

Use the decode helper the neighbouring tests use.

In `GameIntentMapperNegotiationSuite`, add:

```scala
  test("a site in a transfer row maps to its site id"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector.empty, Vector("site:dunes"))), Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          Vector.empty, Vector(SiteId("site:dunes"))))))))))
```

In `NegotiationDealSuite`, add:

```scala
  test("a site alone is substance"):
    val deal = fold(said(p1, ProposeTerms(NegotiationTerms(Vector(
      NegotiationTransfer(p2, 0, Vector.empty, Vector.empty,
        Vector(SiteId("site:dunes"))))))))
    assert(deal.hasSubstance)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite"`
Expected: compile errors: `NegotiationTransfer` takes no fifth argument.

- [ ] **Step 3: Widen the model**

In `NegotiationTerms.scala`, give `NegotiationTransfer` a last field
`sites: Vector[SiteId] = Vector.empty`, and beside its adviser `require`
add:

```scala
  require(sites.distinct.size == sites.size,
    "a transfer row names one site twice")
```

In `NegotiationDeal.scala`, `DealState.hasSubstance` reads
`t.transfers.exists(x => x.favor > 0 || x.relics.nonEmpty ||
x.advisers.nonEmpty || x.sites.nonEmpty)`.

- [ ] **Step 4: Widen the journal**

In `NegotiationTermsCodec.encode`, add to each transfer row, after
`"advisers"`:

```scala
      "siteIds" -> ujson.Arr.from(transfer.sites.map(site =>
        ujson.Str(site.value)))
```

In `decode`, the transfer row's `yield` passes
`row("siteIds").arr.toVector.map(v => SiteId(v.str))` as the last
argument. The `require` on a repeated site throws inside the `try`, which
the existing `NonFatal` catch turns into `InvalidValue`.

- [ ] **Step 5: Widen the command wire**

In `CommandIntents.scala`, give `NegotiationTransfer` a last field
`siteIds: Vector[String] = Vector.empty`.

In `CommandNestedCodecs.scala`:

- `encodeNegotiation`'s transfer row adds
  `"siteIds" -> ujson.Arr.from(row.siteIds.map(ujson.Str(_)))` after
  `"advisers"`.
- `decodeTransfer` adds `"siteIds"` to its `exact` set, and before the
  `yield`:

```scala
    sites <- field(row, "siteIds", path).flatMap(strings(_, s"$path.siteIds"))
    _ <- noDuplicates(sites, s"$path.siteIds")
```

  then yields `NegotiationTransfer(recipient, favor, relics, advisers,
  sites)`.

In `GameIntentMapper.negotiation`, slice 3's row builder becomes:

```scala
        rows.map((v, advisers) => distantlands.model.NegotiationTransfer(
          PlayerId(v.recipientPlayerId), v.favor,
          v.relicIds.map(RelicId.apply), advisers,
          v.siteIds.map(SiteId.apply))), ds,
```

In `frontend/.../GameClient.scala`, give `NegotiationTransferInput` a last
field `siteIds: Vector[String] = Vector.empty`. In
`ServerUiSupport.protocolNegotiationTerms`, the transfer becomes
`NegotiationTransfer(v.recipientPlayerId, v.favor, v.relicIds, v.advisers, v.siteIds)`.

Then find every JSON fixture that writes a transfer row and add
`"siteIds":[]` after its `"advisers"` key:

Run: `git grep -n '"relicIds"' -- src/test shared/src/test frontend/src/test`
Expected: each hit is a test JSON string or a journal fixture; update
each transfer row it holds.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

Add each fixture file Step 5's grep changed to the `git add` below.

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
feat(negotiation): a transfer row carries sites

The model, journal, command wire and intent mapper each take a row's
sites by id. A row naming one site twice is refused.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Site rules, acceptance and settlement

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala`
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala` (`NegotiationBounds`)
- Modify: `src/main/scala/distantlands/model/Decisions.scala` (`DecisionQuery.Negotiate`)
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationSiteTermsSuite.scala` (new),
  `src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationSites.deedWriterUsers`; Task 2's
  `NegotiationTransfer.sites`; slice 3's `DealRules`, `TermKind`,
  `NegotiationStartFacts`, `NegotiationDeal.snapshot` and `bounds`.
- Produces:
  - `TermKind.Site`; `DealRules(adviserTraders, advisersOpen,
    bindingPromises, siteTraders: Vector[PlayerId] = Vector.empty)` with
    `siteRecipients(author, participants): Vector[PlayerId]`.
  - `NegotiationStartFacts(candidates, tribunalUsers, arbiterUsers,
    festivalUsers, deedWriterUsers: Vector[PlayerId] = Vector.empty)`.
  - `SiteMove(giver: PlayerId, recipient: PlayerId, site: SiteId)`, and on
    `NegotiationSites`: `giftable(ready, author): Vector[SiteId]`,
    `mayReceive(ready, author, recipient): Boolean`,
    `moves(deal): Vector[SiteMove]`, `returns(ready, deal):
    Vector[CoreOperation]`, `shortOfWarbands(ready, deal):
    Vector[PlayerId]`, `warbandsDecisionId(site): String`,
    `placed(answered, site): Option[Int]`, `most(ready, deal, move,
    answered): Int`, `placement(ready, move, count):
    Option[CoreOperation]`.
  - `NegotiationBounds.sites: Vector[SiteId]` and
    `NegotiationBounds.siteRecipients: Vector[PlayerId]`, both defaulting
    to empty.
  - `DecisionQuery.Negotiate.shortOfWarbands: Vector[PlayerId]`,
    defaulting to empty. When it is not empty, `acceptors` is empty.
  - Note keys `NegotiationProcedure.gaveSite` and
    `NegotiationProcedure.placedWarbands`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/NegotiationSiteTermsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{DeedWriterCard,
  NegotiationDeal, NegotiationSites, NegotiationStartFacts}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powers.economy.VowOfWanderingCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  ChooseManyAnswer, ProposeTerms}
import distantlands.model.OathState.Ready
import distantlands.testkit.{Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Site terms (catalog batch 12, rulings 18 to 20 and 27 to 29): who may
  * offer which sites to whom, the warbands a recipient needs, and what
  * settling moves. */
class NegotiationSiteTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val woods = DeepWoodsSite.id
  private val city = AncientCitySite.id

  /** Every player at Broken Peaks, each board holding its 3 warbands. p1
    * holds Deed Writer faceup and rules Deep Woods with 2 warbands; p2
    * rules Dunes with 2. */
  private def deeded: Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .adviser(p1, DeedWriterCard.id)
    .warbandsAt(woods, p1, 2).warbandsAt(dunes, p2, 2)

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

  private def place(state: OathState, by: PlayerId, at: SiteId, n: Int) =
    rules.resolveWalker(state, by, NegotiationSites.warbandsDecisionId(at),
      ChooseAmountAnswer(n))

  /** A row giving `to` exactly `sites`. */
  private def give(to: PlayerId, sites: SiteId*) =
    ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(to, 0,
      Vector.empty, Vector.empty, sites.toVector))))

  /** The question `state` is parked on. */
  private def parkedOn(state: OathState): Decide =
    ParkedNode.of(state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"not parked on a decision: $other")

  private def parkedDeal(state: OathState): DecisionQuery.Negotiate =
    parkedOn(state).query match
      case negotiate: DecisionQuery.Negotiate => negotiate
      case other => fail(s"not a deal: $other")

  /** The id, owner and range of the placement question `state` is parked
    * on. */
  private def asked(state: OathState): (String, PlayerId, Int, Int) =
    val decide = parkedOn(state)
    decide.query match
      case DecisionQuery.ChooseAmount(min, max, _, _, _) =>
        (decide.decisionId, decide.owner, min, max)
      case other => fail(s"not an amount: $other")

  /** Runs `steps` in order from `state`, failing on any refusal. */
  private def run(state: OathState,
      steps: (PlayerId, DecisionAnswer)*): OathState =
    steps.foldLeft(state)((now, step) =>
      say(now, step._1, step._2).getOrElse(fail(s"$step is refused")).state)

  private def forces(state: OathState, at: SiteId): SiteForces =
    Outcomes.ready(state).game.current.map.sites(at).forces

  private def board(state: OathState, player: PlayerId): Int =
    Outcomes.ready(state).game.current.player(player).get.board.warbands

  private def ruler(state: OathState, at: SiteId): SiteRuler =
    val ready = Outcomes.ready(state)
    SiteRule.ruler(forces(state, at), ready.game.current.players)
      .getOrElse(fail(s"$at has no ruler"))

  test("Deed Writer opens site terms only where its holder gives or " +
      "receives, for the sites each author's warbands hold"):
    val bounds = parkedDeal(atDeal(deeded, p2, p3)).bounds
    assertEquals(bounds(p1).siteRecipients, Vector(p2, p3))
    assertEquals(bounds(p2).siteRecipients, Vector(p1))
    assertEquals(bounds(p3).siteRecipients, Vector(p1))
    assertEquals(bounds(p1).sites, Vector(woods))
    assertEquals(bounds(p2).sites, Vector(dunes))
    val dealt = atDeal(deeded, p2, p3)
    assert(say(dealt, p2, give(p3, dunes)).isLeft)
    assert(say(dealt, p2, give(p1, dunes)).isRight)

  test("the start stores Deed Writer's holder"):
    val started = rules.startWalker(Ready(deeded.ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    assertEquals(Outcomes.ready(started.state).game.current.walkerStartFacts,
      NegotiationStartFacts(Vector(p2, p3), Vector.empty, Vector.empty,
        Vector.empty, Vector(p1)).refs)

  test("a bandit site, or another player's, is never offered"):
    val held = deeded.bandits(city, 2)
    assertEquals(parkedDeal(atDeal(held, p2)).bounds(p1).sites,
      Vector(woods))
    assert(say(atDeal(held, p2), p1, give(p2, dunes)).isLeft)
    assert(say(atDeal(held, p2), p1, give(p2, city)).isLeft)

  test("two Imperial players are offered no site between them"):
    // p3 is the Chancellor and p2 a Citizen holding Deed Writer.
    val empire = Table.start.imperial(p3, p2)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
      .adviser(p2, DeedWriterCard.id).warbandsAt(dunes, p2, 2)
    val bounds = parkedDeal(atDeal(empire, p2, p3)).bounds
    assertEquals(bounds(p2).siteRecipients, Vector(p1))
    assertEquals(bounds(p3).siteRecipients, Vector.empty[PlayerId])

  test("a Citizen gives an Imperial site on an Exile's turn, which the " +
      "Chancellor may then not offer too"):
    // p1, the actor, is an Exile holding Deed Writer. p3 is the Chancellor
    // and p2 a Citizen; Dunes holds 2 Imperial warbands.
    val empire = Table.start.imperial(p3, p2)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
      .adviser(p1, DeedWriterCard.id).warbandsAt(dunes, p2, 2)
    val offered = run(atDeal(empire, p2, p3), p2 -> give(p1, dunes))
    assertEquals(parkedDeal(offered).bounds(p3).sites, Vector.empty[SiteId])
    assert(say(offered, p3, give(p1, dunes)).isLeft)
    val settled = run(offered, p1 -> AcceptDeal, p2 -> AcceptDeal,
      p3 -> AcceptDeal)
    assertEquals(asked(settled),
      (NegotiationSites.warbandsDecisionId(dunes), p1, 1, 3))
    val placed = place(settled, p1, dunes, 2).getOrElse(fail("placed")).state
    assertEquals(board(placed, p2), 5)
    assertEquals(ruler(placed, dunes), SiteRuler.Player(p1))

  test("a Vow of Wandering holder is offered no site"):
    val vowed = deeded.adviser(p2, VowOfWanderingCard.id)
    assertEquals(parkedDeal(atDeal(vowed, p2, p3)).bounds(p1).siteRecipients,
      Vector(p3))

  test("a recipient with an empty board leaves nobody able to accept, " +
      "until a swap returns their warbands"):
    val empty = deeded.boardWarbands(p2, 0)
    val short = run(atDeal(empty, p2), p1 -> give(p2, woods))
    assertEquals(parkedDeal(short).shortOfWarbands, Vector(p2))
    assertEquals(parkedDeal(short).acceptors, Set.empty[PlayerId])
    assert(say(short, p2, AcceptDeal).isLeft)
    val swapped = run(short, p2 -> give(p1, dunes))
    assertEquals(parkedDeal(swapped).shortOfWarbands, Vector.empty[PlayerId])
    assertEquals(parkedDeal(swapped).acceptors, Set(p1, p2))

  test("a swap returns both old rulers' warbands, asks each new ruler 1 " +
      "to N, and passes rule"):
    val agreed = run(atDeal(deeded, p2), p1 -> give(p2, woods),
      p2 -> give(p1, dunes), p1 -> AcceptDeal, p2 -> AcceptDeal)
    // Both boards hold 3 + 2 once the returns run; p1's row comes first.
    assertEquals(asked(agreed),
      (NegotiationSites.warbandsDecisionId(woods), p2, 1, 5))
    assertEquals(forces(agreed, woods), SiteForces.Empty)
    assert(place(agreed, p2, woods, 6).isLeft)
    val first = place(agreed, p2, woods, 4).getOrElse(fail("woods")).state
    assertEquals(asked(first),
      (NegotiationSites.warbandsDecisionId(dunes), p1, 1, 5))
    val done = place(first, p1, dunes, 5).getOrElse(fail("dunes")).state
    assertEquals(ruler(done, woods), SiteRuler.Player(p2))
    assertEquals(ruler(done, dunes), SiteRuler.Player(p1))
    assertEquals(forces(done, woods).total, 4)
    assertEquals((board(done, p1), board(done, p2)), (0, 1))

  test("a recipient of two sites keeps a warband for the second"):
    val two = deeded.warbandsAt(city, p2, 1)
    val agreed = run(atDeal(two, p2), p2 -> give(p1, dunes, city),
      p1 -> AcceptDeal, p2 -> AcceptDeal)
    assertEquals(asked(agreed),
      (NegotiationSites.warbandsDecisionId(dunes), p1, 1, 2))
    val first = place(agreed, p1, dunes, 2).getOrElse(fail("dunes")).state
    assertEquals(asked(first),
      (NegotiationSites.warbandsDecisionId(city), p1, 1, 1))
```

`ParkedNode.of` rebuilds the walk from its journal, so each `asked`
reads the bound a resumed walk asks (finding 7). In the two-site test p1
holds 3 board warbands and receives two sites, so the first bound keeps
one back.

In `NegotiationStartFactsSuite`, add:

```scala
  test("Deed Writer's users round trip, and open site terms"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector.empty,
      Vector.empty, Vector.empty, Vector(p3))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p3)).siteTraders, Vector(p3))
    assertEquals(facts.rules(Vector(p1, p3)).siteRecipients(p1,
      Vector(p1, p3)), Vector(p3))
    assertEquals(facts.rules(Vector(p1, p2)).siteTraders,
      Vector.empty[PlayerId])
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationSiteTermsSuite distantlands.gameplay.NegotiationStartFactsSuite"`
Expected: compile errors: `siteRecipients`, `shortOfWarbands` and
`warbandsDecisionId` not found.

- [ ] **Step 3: The deal rules open sites**

In `DealRules.scala`, change `TermKind` to
`case Favor, Relic, Adviser, Site` and its doc comment's last sentence to
"Secrets join with slice 6.". Give `DealRules` a last field
`siteTraders: Vector[PlayerId] = Vector.empty`, add to its doc comment
"`siteTraders` may give and receive sites with anyone (Deed Writer,
ruling 18).", and replace `kinds` and add `siteRecipients`:

```scala
  def kinds(author: PlayerId, recipient: PlayerId): Set[TermKind] =
    val pair = Set(author, recipient)
    Set[TermKind](TermKind.Favor, TermKind.Relic) ++
      Option.when(advisersOpen || adviserTraders.exists(pair))(
        TermKind.Adviser) ++
      Option.when(siteTraders.exists(pair))(TermKind.Site)

  /** The participants `author` may give sites, in participant order,
    * before rulings 19 and the Vow of Wandering narrow them
    * ([[NegotiationSites.mayReceive]]). */
  def siteRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Site))
```

- [ ] **Step 4: Store Deed Writer's users**

In `NegotiationStartFacts.scala`:

- Give the case class a last field
  `deedWriterUsers: Vector[PlayerId] = Vector.empty`, and name Deed Writer
  (`deedWriterUsers`) in the doc comment's list of user lists. Add to the
  doc comment's reasons: "a site term moves no card, but Deed Writer is
  non-persistent, so its users are read under the restrictions too."
- `refs` zips `NegotiationStartFacts.Markers` with
  `Vector(tribunalUsers, arbiterUsers, festivalUsers, deedWriterUsers)`.
- `rules` passes `siteTraders = deedWriterUsers.filter(participants.contains)`
  as the new last argument, and its doc comment cites rulings 11, 14, 15
  and 18.
- In the companion, add
  `val DeedWriterMarker: String = "negotiation.deed-writer-users"` and
  append it to `Markers`.
- `at` passes `NegotiationSites.deedWriterUsers(catalog, ready, players,
  restrictions)` as the last argument.
- `read` builds `NegotiationStartFacts(lists(0), lists(1), lists(2),
  lists(3), lists(4))`.

- [ ] **Step 5: The rest of the site rule**

Add to `NegotiationSites.scala`, above the object:

```scala
/** One site an agreed deal moves: who gives it and who receives it. */
final case class SiteMove(giver: PlayerId, recipient: PlayerId, site: SiteId)
```

and extend its imports with `distantlands.gameplay.actions.VowOfWanderingRule`
and `distantlands.model.DecisionAnswer.ChooseAmountAnswer`. Add to the
object:

```scala
  /** The sites `author` may give: the in-play sites their own-colour
    * warbands hold, in map order (rulings 18 and 28). Every Imperial
    * player's own colour is the Imperial warbands, so each may give every
    * Imperial site, on any turn. Bandits are nobody's colour. */
  def giftable(ready: ReadyGame, author: PlayerId): Vector[SiteId] =
    val current = ready.game.current
    current.player(author).flatMap(PlayerForceKind.of(ready, _))
      .fold(Vector.empty[SiteId])(own => current.map.inPlay.filter(id =>
        current.map.sites.get(id).exists(_.forces match
          case SiteForces.Occupied(kind, _) => kind == own
          case SiteForces.Empty => false)))

  /** Whether `author` may give a site to `recipient`, when the deal's
    * rules open sites between them: not when both are Imperial (ruling
    * 19), and not to a faceup Vow of Wandering holder, who cannot place
    * the warband ruling 20 requires. */
  def mayReceive(ready: ReadyGame, author: PlayerId,
      recipient: PlayerId): Boolean =
    !(Imperials.isImperial(ready, author) &&
      Imperials.isImperial(ready, recipient)) &&
      !VowOfWanderingRule.holds(ready, recipient)

  /** The sites the other participants' terms already give: an author may
    * not give one of them too, so the Chancellor and a Citizen never name
    * the same Imperial site. */
  def offeredByOthers(deal: DealState, author: PlayerId): Set[SiteId] =
    deal.participants.filter(_ != author).flatMap(other =>
      deal.terms(other).transfers.flatMap(_.sites)).toSet

  /** Every site the deal moves, by author in participant order, then in
    * the order each row names them: the order of the returns and of the
    * placement questions. */
  def moves(deal: DealState): Vector[SiteMove] =
    deal.participants.flatMap(author => deal.terms(author).transfers
      .flatMap(transfer => transfer.sites.map(SiteMove(author,
        transfer.recipient, _))))

  /** Each given site's warbands, all of them, moved to the old ruler's
    * board (ruling 20). */
  def returns(ready: ReadyGame, deal: DealState): Vector[CoreOperation] =
    moves(deal).flatMap(move => ready.game.current.map.sites.get(move.site)
      .map(_.forces).collect {
        case SiteForces.Occupied(kind, count) => Move(Piece.Warbands(kind,
          count), PositionedLocation(Location.Site(move.site)),
          PositionedLocation(Location.PlayArea(move.giver))): CoreOperation
      })

  /** The recipients the deal would leave with fewer warbands on their
    * board than sites they receive, counted after the warbands of the
    * sites they give come back (ruling 29), in participant order. */
  def shortOfWarbands(ready: ReadyGame, deal: DealState): Vector[PlayerId] =
    val all = moves(deal)
    deal.participants.filter { player =>
      val received = all.count(_.recipient == player)
      val returned = all.filter(_.giver == player)
        .map(move => standing(ready, move.site)).sum
      received > 0 && board(ready, player) + returned < received
    }

  /** The question that asks `site`'s new ruler how many warbands to
    * place. Slice 6 prefixes it with its deal's prefix. */
  def warbandsDecisionId(site: SiteId): String =
    s"deed-writer.warbands.${site.value}"

  /** How many warbands the new ruler placed at `site`, once answered. */
  def placed(answered: Vector[Answered], site: SiteId): Option[Int] =
    val id = warbandsDecisionId(site)
    answered.collectFirst {
      case Answered(`id`, ChooseAmountAnswer(count), _) => count
    }

  /** The most `move`'s recipient may place at its site: the warbands their
    * board held once the returns ran, less what they placed at their
    * earlier sites, less one for each site still to come. Read from the
    * board plus every placement already answered, so a resumed walk asks
    * the same bound (finding 7). Never below 1: the acceptance check keeps
    * a warband for every site. */
  def most(ready: ReadyGame, deal: DealState, move: SiteMove,
      answered: Vector[Answered]): Int =
    val own = moves(deal).filter(_.recipient == move.recipient)
    val (earlier, rest) = own.span(_ != move)
    val afterReturns = board(ready, move.recipient) +
      own.flatMap(m => placed(answered, m.site)).sum
    math.max(1, afterReturns - earlier.flatMap(m =>
      placed(answered, m.site)).sum - (rest.size - 1))

  /** `count` of the recipient's own warbands, from their board to the
    * site (ruling 20). */
  def placement(ready: ReadyGame, move: SiteMove,
      count: Int): Option[CoreOperation] =
    ready.game.current.player(move.recipient)
      .flatMap(PlayerForceKind.of(ready, _)).map(kind => Move(
        Piece.Warbands(kind, count),
        PositionedLocation(Location.PlayArea(move.recipient)),
        PositionedLocation(Location.Site(move.site))))

  private def board(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.player(player).fold(0)(_.board.warbands)

  private def standing(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites.get(site).fold(0)(_.forces.total)
```

If the backticked `id` in `placed` does not compile as a stable
identifier, match `case Answered(answeredId, ChooseAmountAnswer(count), _)
if answeredId == id`.

- [ ] **Step 6: Bounds, acceptance and settlement**

In `NegotiationTerms.scala`, give `NegotiationBounds` two last fields:

```scala
    /** The sites the author may give (their own-colour warbands hold
      * them), offered only to `siteRecipients` (catalog batch 12, rulings
      * 18, 19 and 28). */
    sites: Vector[SiteId] = Vector.empty,
    siteRecipients: Vector[PlayerId] = Vector.empty
```

In `Decisions.scala`, give `DecisionQuery.Negotiate` a last field after
slice 3's `overAdviserLimit`:

```scala
    /** The recipients the terms would leave with too few warbands to rule
      * every site they receive. While it is not empty nobody may accept
      * (ruling 29). */
    shortOfWarbands: Vector[PlayerId] = Vector.empty
```

In `NegotiationDeal.scala`:

- `snapshot` computes `val short = NegotiationSites.shortOfWarbands(state,
  deal)`, lets participants accept only when `over.isEmpty &&
  short.isEmpty` too, passes `shortOfWarbands = short`, and calls
  `bounds(catalog, state, author, deal, rules)`. Its doc comment adds "or
  short of warbands for a site".
- `bounds` takes `deal: DealState` in place of `participants`, reads
  `deal.participants` where it read `participants`, and adds:

```scala
    val siteRecipients = rules.siteRecipients(author, deal.participants)
      .filter(NegotiationSites.mayReceive(state, author, _))
```

  and passes, after slice 3's adviser fields:

```scala
      sites = if siteRecipients.isEmpty then Vector.empty
        else NegotiationSites.giftable(state, author).filterNot(
          NegotiationSites.offeredByOthers(deal, author)),
      siteRecipients = siteRecipients
```

- In `affordable`, after slice 3's adviser check, add:

```scala
      _ <- Either.cond(terms.transfers.flatMap(_.sites).forall(
        NegotiationSites.giftable(state, author).contains), (),
        OathViolation.NegotiationUnavailable(
          "an offered site is no longer held by its author's warbands"))
```

- `settle` refuses a deal that would leave a recipient short, before the
  per-author checks:

```scala
  def settle(state: ReadyGame,
      deal: DealState): Either[OathViolation, Vector[CoreOperation]] =
    Either.cond(NegotiationSites.shortOfWarbands(state, deal).isEmpty, (),
      OathViolation.NegotiationUnavailable(
        "a recipient has too few warbands for the sites they receive"))
      .flatMap(_ => deal.participants.foldLeft[Either[OathViolation, Unit]](
        Right(())) { (result, author) => result.flatMap(_ =>
          affordable(state, author, deal.terms(author))) })
      .map(_ => knowledge(deal) ++ transfers(deal))
```

  Its doc comment adds: "A site's warbands and placements are the
  procedure's next steps (`NegotiationSites.returns`, the placement
  questions)."

- [ ] **Step 7: Validate site terms**

In `DecisionQueries.acceptsTerms`, beside slice 3's `val advisers`, add
`val sites = terms.transfers.flatMap(_.sites)`, and after the adviser
check:

```scala
          _ <- require(sites.distinct.size == sites.size &&
            sites.forall(own.sites.contains) &&
            terms.transfers.forall(transfer => transfer.sites.isEmpty ||
              own.siteRecipients.contains(transfer.recipient)),
            decisionId, "offers a site it cannot")
```

- [ ] **Step 8: Return, ask and place in the procedure**

In `NegotiationProcedure.scala`, add two note keys after `gaveCard`
(`SiteMove` and `NegotiationSites` share the package, so no import is
needed):

```scala
  /** "p2 gave Broken Peaks to p1" (Deed Writer, catalog batch 12, ruling
    * 18). */
  val gaveSite: NoteKey = notes.key("gave-site", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" to "), NotePart.Arg(2)))
  /** "p1 placed 2 warbands at Broken Peaks" (ruling 20). */
  val placedWarbands: NoteKey = notes.key("placed-warbands", Vector(
    NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "warband", "warbands"),
    NotePart.Text(" at "), NotePart.Arg(2)))
```

In `tree`, the settlement adds the returns step after `BuildOps(settle)`,
only when the deal moves a site (finding 3):

```scala
        val returns: Vector[Operation] =
          Option.when(NegotiationSites.moves(deal).nonEmpty)(
            // Required: a refused return fails the settlement rather than
            // leaving two kinds of warband at a site (ruling 20).
            BuildOps((state, _) => Right(NegotiationSites.returns(state,
              deal)), required = true)).toVector
        val settlement: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else (BuildOps((state, _) => NegotiationDeal.settle(state, deal),
            window = Some(PowerWindow.NegotiationSettlement)) +: returns) ++
            (disclosed(deal) ++ transferred(deal) ++ offered(deal)).map(note =>
              Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(note)))
        // After every transfer, each new ruler places warbands at each site
        // they received, in the order the sites moved (rulings 20, 29).
        val placements: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else NegotiationSites.moves(deal).flatMap(placing(catalog, deal, _))
```

and returns `opening ++ settlement ++ placements ++ citizenship ++
promises`. Update the settlement comment above it: "disclosures first,
then transfers, then each given site's warbands home." Update the object's
tree diagram to:

```scala
  *   Branch(agreed -> BuildOps(settle), BuildOps(site returns), notes,
  *     per site: Branch -> Decide(warbands), BuildOps(place), note;
  *     Citizenship, promise notes))
```

In `transferred`, add each site's line after slice 3's adviser lines:

```scala
          transfer.sites.map(site => gaveSite.sourceless(
            NoteArg.Player(author), NoteArg.Site(site),
            NoteArg.Player(transfer.recipient)))
```

and add "then sites" to its doc comment's order.

Add the placement steps:

```scala
  /** `move`'s new ruler answers how many warbands to place, from 1 to
    * `NegotiationSites.most`; the answer places them, a required move, and
    * writes its line (ruling 20). The question sits in a `Branch` so its
    * bound reads the board when it is asked. */
  private def placing(catalog: ExecutableCatalog, deal: DealState,
      move: SiteMove): Vector[Operation] =
    val id = NegotiationSites.warbandsDecisionId(move.site)
    val name = catalog.site(move.site).fold(move.site.value)(_.name)
    Vector(
      Branch((ready, pending) => Vector(Decide(id, move.recipient,
        DecisionQuery.ChooseAmount(1, NegotiationSites.most(ready, deal,
          move, pending.answered),
          Some(s"Place how many warbands at $name?"), "Place")))),
      BuildOps((ready, pending) => Right(NegotiationSites.placed(
        pending.answered, move.site).flatMap(count =>
        NegotiationSites.placement(ready, move, count)).toVector),
        required = true),
      Note(ProcedureNotes.id(ActionRef.Negotiation), states =>
        NegotiationSites.placed(states.answered, move.site).map(count =>
          placedWarbands.sourceless(NoteArg.Player(move.recipient),
            NoteArg.Number(count), NoteArg.Site(move.site)))))
```

- [ ] **Step 9: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationSiteTermsSuite distantlands.gameplay.NegotiationStartFactsSuite distantlands.gameplay.NegotiationAdviserTermsSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.walker.WalkerProcedureRegistrySuite distantlands.gameplay.walker.DecisionQuerySuite"`
Expected: PASS.

If the swap test parks on a title or Chronicle question after the last
placement, `Outcomes.ready` still reads the state; if it does not, give
the table `.oathkeeper(Some(p1))` so ruling a new site changes no title.

Then run every suite that drives a Negotiation:

Run: `./sbtw "testOnly *Negotiation* *RoleChanges* *GameLog* *HornedMask*"`
Expected: PASS. The golden logs do not change: no existing script moves
a site.

Then: `python3 scripts/check-architecture.py`
Expected: "architecture check passed".

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationStartFacts.scala \
  src/main/scala/distantlands/model/NegotiationTerms.scala \
  src/main/scala/distantlands/model/Decisions.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala \
  src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala \
  src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala \
  src/test/scala/distantlands/gameplay/NegotiationSiteTermsSuite.scala \
  src/test/scala/distantlands/gameplay/NegotiationStartFactsSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): site terms under Deed Writer

The start stores Deed Writer's user. DealRules opens site rows for
the pairs it sits in, and the bounds offer each author the sites their
own-colour warbands hold, never between two Imperial players or to a
Vow of Wandering holder. Nobody may accept while a recipient would be
short of warbands. Settlement returns each site's warbands to its old
ruler, then asks each new ruler how many to place.

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
- Modify: `frontend/src/main/scala/distantlands/frontend/frontend.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Test: `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`,
  `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`,
  `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`

**Interfaces:**
- Consumes: Task 3's bounds fields and `Negotiate.shortOfWarbands`.
- Produces:
  - `NegotiationSiteProjection(siteId: String, name: String)`.
  - `NegotiationTransferProjection` gains `sites:
    Vector[NegotiationSiteProjection] = Vector.empty`.
  - `NegotiationEditingProjection` gains `giftableSites:
    Vector[NegotiationSiteProjection] = Vector.empty`, `siteRecipients:
    Vector[String] = Vector.empty` and `shortOfWarbands: Vector[String] =
    Vector.empty`.
  - Frontend alias `NegotiationSiteState`.

- [ ] **Step 1: Write the failing tests**

In `NegotiationDealProjectionSuite`, add imports
`distantlands.gameplay.actions.negotiation.DeedWriterCard` and
`distantlands.gameplay.cards.DunesSite`, and add:

```scala
  private val dunesOffer = NegotiationSiteProjection(DunesSite.id.value,
    "Dunes")

  test("every viewer sees a site on offer by name"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2)
    val state = parkedDeal(table, who = Vector(p2, p3), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector.empty, Vector(DunesSite.id))))))
    Vector(p1, p2, p3).foreach(viewer => assertEquals(
      deal(view(state, viewer)).transfers.head.sites, Vector(dunesOffer),
      viewer.value))

  test("the editor offers each author's sites to the recipients the " +
      "rules allow"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2)
    val state = parkedDeal(table, who = Vector(p2, p3))
    val p1Editing = deal(view(state, p1)).editing.getOrElse(fail("editing"))
    assertEquals(p1Editing.giftableSites, Vector(dunesOffer))
    assertEquals(p1Editing.siteRecipients, Vector(p2.value, p3.value))
    val p2Editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assertEquals(p2Editing.siteRecipients, Vector(p1.value))

  test("terms that leave a recipient short of warbands are projected " +
      "with who"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2).boardWarbands(p2, 0)
    val state = parkedDeal(table, who = Vector(p2), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector.empty, Vector(DunesSite.id))))))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing.getOrElse(fail("editing"))
      assertEquals(editing.shortOfWarbands, Vector(p2.value), viewer.value)
      assert(!editing.canAccept, viewer.value)
    }
```

If `gathered`'s pawn site is Dunes, use Deep Woods (`DeepWoodsSite`,
"Deep Woods") for the offered site throughout instead.

In `ProjectionProtocolSuite`'s deal round trip, set the new fields to
non-default values in the deal it round-trips: a transfer with one site
`NegotiationSiteProjection("site:dunes", "Dunes")`, and editing with that
site in `giftableSites`, `siteRecipients = Vector("blue")` and
`shortOfWarbands = Vector("blue")`.

In `NegotiationDealPanelSuite`, add:

```scala
  private val dunes = distantlands.protocol.projection
    .NegotiationSiteProjection("site:dunes", "Dunes")

  test("a site is offered only to the recipients the rules allow, and " +
      "Save sends its id"):
    val deal = threeWay(NegotiationEditingState(3, Vector.empty, Vector.empty,
      Vector.empty, canAccept = false, giftableSites = Vector(dunes),
      siteRecipients = Vector("blue")))
    val ui = new RecordingControls()
    val panel = render(owner(deal), ui)
    assert(Dom.all(panel, "[aria-label='Give Dunes to yellow']").isEmpty)
    box(panel, "Give Dunes to blue").checked = true
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(
          terms)) => terms }
    assertEquals(sent.flatMap(_.transfers).flatMap(_.siteIds),
      Vector("site:dunes"))

  test("a transfer names the sites it gives"):
    val base = dealWhereBlueHasAccepted()
    val panel = render(spectator(base.copy(transfers = Vector(
      NegotiationTransferState(authorPlayerId = "blue",
        recipientPlayerId = "red", favor = 0, relicCount = 0,
        relics = Vector.empty, sites = Vector(dunes))))),
      new RecordingControls())
    assertEquals(Dom.one(panel, ".negotiation-transfer").textContent,
      "blue gives red: Dunes")

  test("terms that leave a recipient short of warbands withhold Accept " +
      "with the reason"):
    val base = dealWhereBlueHasAccepted(canAccept = false)
    val panel = render(owner(base.copy(editing = base.editing.map(_.copy(
      shortOfWarbands = Vector("blue"))))), new RecordingControls())
    val accept = button(panel, ".negotiation-accept")
    assertEquals(accept.getAttribute("aria-disabled"), "true")
    assertEquals(accept.getAttribute("data-tip"),
      "These terms would leave blue too few warbands to rule every site " +
        "they receive.")
```

`threeWay`, `box`, `button`, `owner`, `spectator`,
`dealWhereBlueHasAccepted`, `RecordingControls` and `ui.submitted` are the
suite's existing helpers, the same slice 3's adviser tests use.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: compile errors: `NegotiationSiteProjection` and
`giftableSites` not found.

- [ ] **Step 3: Widen the DTOs and their codec**

In `ActionProjectionDtos.scala`, add beside `NegotiationSiteRelicProjection`:

```scala
/** A site on offer in a deal, by id and name. Sites are public, so every
  * viewer sees them (Deed Writer). */
final case class NegotiationSiteProjection(siteId: String, name: String)
```

Give `NegotiationTransferProjection` a last field `sites:
Vector[NegotiationSiteProjection] = Vector.empty`, and
`NegotiationEditingProjection` three last fields after slice 3's:

```scala
    /** The sites the viewer may give, and to whom (Deed Writer). */
    giftableSites: Vector[NegotiationSiteProjection] = Vector.empty,
    siteRecipients: Vector[String] = Vector.empty,
    /** The recipients the current terms would leave with too few warbands
      * to rule every site they receive; while any is, nobody may accept. */
    shortOfWarbands: Vector[String] = Vector.empty
```

Add "and the sites on offer" to `NegotiationDealProjection`'s list of
what everyone sees.

In `ActionProjectionCodec.scala`:

- Add an encoder and decoder beside `encodeSiteRelic`/`decodeSiteRelic`:

```scala
  private def encodeSite(row: NegotiationSiteProjection): ujson.Value =
    ujson.Obj("siteId" -> row.siteId, "name" -> row.name)
  private def decodeSite(raw: ujson.Value, child: String)
      : Result[NegotiationSiteProjection] = for
    row <- obj(raw, child)
    _ <- exact(row, Set("siteId", "name"), child)
    site <- string(row, "siteId", child)
    name <- string(row, "name", child)
  yield NegotiationSiteProjection(site, name)
```

- `encodeTransfer` adds `"sites" -> encoded(row.sites)(encodeSite)`.
- `decodeTransfer`'s `exact` set adds `"sites"`, and before the `yield`:

```scala
    siteRaws <- array(row, "sites", child)
    sites <- traverse(siteRaws, s"$child.sites")(decodeSite)
```

  passing `sites` as the last argument.
- `encodeDeal`'s editing object adds `"giftableSites" ->
  encoded(editing.giftableSites)(encodeSite)`, `"siteRecipients"` and
  `"shortOfWarbands"`, encoded as the neighbouring string lists are.
- `decodeDeal`'s editing `exact` set adds the three keys, and it reads:

```scala
      giftableSiteRaws <- array(row, "giftableSites", child)
      giftableSites <- traverse(giftableSiteRaws, s"$child.giftableSites")(
        decodeSite)
      siteRecipients <- strings(row, "siteRecipients", child)
      short <- strings(row, "shortOfWarbands", child)
```

  passing `giftableSites, siteRecipients, short` after slice 3's three
  adviser fields.

- [ ] **Step 4: Project them**

In `NegotiationDealProjector.project`, each transfer passes as its last
argument:

```scala
          transfer.sites.map(site => NegotiationSiteProjection(site.value,
            presentation.siteLabel(site)))
```

In `editing`, add after slice 3's fields:

```scala
      giftableSites = own.sites.map(site => NegotiationSiteProjection(
        site.value, presentation.siteLabel(site))),
      siteRecipients = own.siteRecipients.map(_.value),
      shortOfWarbands = query.shortOfWarbands.map(_.value)
```

Add "and the sites on offer" to the class doc comment's list.

- [ ] **Step 5: Render them**

In `frontend.scala`, beside `NegotiationTransferState`, add
`type NegotiationSiteState = protocol.projection.NegotiationSiteProjection`.

In `NegotiationDealPanel.transferLine`, append the sites to `parts` after
slice 3's adviser parts:

```scala
        ++ t.sites.map(site => textNode(site.name))
```

and add "sites" to its doc comment.

Add a row builder beside `cardItem`:

```scala
  /** A one-site row: the box and the words that name the site, in one
    * `label`, like the favor row. A site has no card to inspect. */
  private def siteItem(control: dom.html.Input,
      site: NegotiationSiteState): dom.Element =
    val row = dom.document.createElement("label").asInstanceOf[dom.html.Label]
    row.className = "negotiation-item"
    row.appendChild(control)
    row.appendChild(text("span", "negotiation-item-label",
      s"Give ${site.name}"))
    row
```

In `editor`, add a buffer beside slice 3's `advisers`:

```scala
    val sites = scala.collection.mutable.ArrayBuffer.empty[
      (String, String, dom.html.Input)]
```

and, inside the per-recipient loop after the adviser rows:

```scala
      if editing.siteRecipients.contains(recipient) then
        editing.giftableSites.foreach { site =>
          val check = input("checkbox")
          check.setAttribute("aria-label", s"Give ${site.name} to $recipient")
          check.checked = deal.transfers.exists(t => t.authorPlayerId == me &&
            t.recipientPlayerId == recipient &&
            t.sites.exists(_.siteId == site.siteId))
          // A site goes to one recipient at most.
          check.onchange = _ => if check.checked then sites.foreach:
            case (other, otherSite, otherCheck)
                if other != recipient && otherSite == site.siteId =>
              otherCheck.checked = false
            case _ => ()
          panel.appendChild(siteItem(check, site))
          sites += ((recipient, site.siteId, check))
        }
```

In Save, the transfer input passes its sites after the advisers:

```scala
          sites.collect { case (`recipient`, site, check) if check.checked =>
            site }.toVector)
```

Replace slice 3's Accept block (from `if editing.overAdviserLimit.isEmpty
then` through its `onclick`) with one that collects every reason:

```scala
    // Withheld with its reasons, not `disabled`: a disabled button takes
    // no pointer event, so a tap could not show them (HoverTip).
    val withheld = Vector(
      Option.when(editing.overAdviserLimit.nonEmpty)("These terms would " +
        s"leave ${editing.overAdviserLimit.mkString(" and ")} over their " +
        "adviser limit."),
      Option.when(editing.shortOfWarbands.nonEmpty)("These terms would " +
        s"leave ${editing.shortOfWarbands.mkString(" and ")} too few " +
        "warbands to rule every site they receive.")).flatten
    if withheld.isEmpty then
      accept.disabled = !canControl || !editing.canAccept
    else
      accept.setAttribute("aria-disabled", "true")
      HoverTip.set(accept, withheld.mkString(" "))
    accept.onclick = _ => if withheld.isEmpty then
      controls.submitCommand(GameCommand.ResolveWalker(decisionId,
        DecisionAnswerWire.AcceptDealWire))
```

Slice 3's adviser-limit test still reads its one sentence as the tip.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala \
  src/main/scala/distantlands/application/NegotiationDealProjector.scala \
  frontend/src/main/scala/distantlands/frontend/frontend.scala \
  frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala \
  src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala \
  frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): offer, show and withhold site terms in the panel

The editor offers each author's sites to the recipients the rules
allow, and every viewer sees a site on offer by name. Accept is
withheld with its reason while a recipient would be short of warbands.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: The Game Log, docs and verification

**Files:**
- Modify: `src/main/scala/distantlands/application/gamelog/DetailLines.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`
- Create: `src/test/resources/gamelog/negotiation-site-given.actor.log`, `.other.log`
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md` (status line)

- [ ] **Step 1: Write the script and the failing test**

In `NegotiationLogScripts.scala`, import
`distantlands.gameplay.actions.negotiation.{DeedWriterCard,
NegotiationSites}` (merging with the existing negotiation import) and
`distantlands.model.DecisionAnswer.ChooseAmountAnswer`, and add:

```scala
  /** The partner gives the actor, who holds Deed Writer, Broken Peaks; the
    * actor places 2 warbands there (catalog batch 12, rulings 18 and 20).
    * The actor holds the Oathkeeper title already, so ruling Broken Peaks
    * writes no title line. */
  def negotiationSiteGiven(using munit.Location): Script =
    val peaks = BrokenPeaksSite.id
    val (service, parked, actor, partner) = negotiating(
      "negotiation-site-given", _.adviser(p1, DeedWriterCard.id)
        .warbandsAt(BrokenPeaksSite, p2, 2).oathkeeper(Some(p1)))
    parked
      .parkedAfter(deal(partner, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(actor, 0, Vector.empty, Vector.empty,
          Vector(peaks)))))))
      .parkedAfter(deal(actor, AcceptDeal))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(Commands.resolveWalker(actor, TreeDecision(
        NegotiationSites.warbandsDecisionId(peaks), ChooseAmountAnswer(2))))
    Script("negotiation-site-given", service, actor)
```

Extend the object's doc comment: "…a facedown adviser given, and a site
given."

In `LogScripts.scala`, add `negotiationSiteGiven` to the import from
`NegotiationLogScripts`, and to `named`:
`"negotiation-site-given" -> (() => negotiationSiteGiven),`.

In `GameLogExchangeSuite`, add:

```scala
  test("a site given under Deed Writer writes its own lines, not the " +
      "generic warband line"):
    val script = negotiationSiteGiven
    val all = texts(format(script, Some(script.actor)))
    val (giver, recipient) = (playerLabel(partner), playerLabel(script.actor))
    assert(all.contains(s"$giver gave Broken Peaks to $recipient"), all)
    assert(all.contains(s"$recipient placed 2 warbands at Broken Peaks"), all)
    assert(!all.exists(_.startsWith("Moved ")), all)
```

`partner` and `playerLabel` are the suite's, as slice 3's adviser test
uses them.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: FAIL on the last assertion: the log holds "Moved 2 … warbands
to Broken Peaks" (finding 8). If the line texts differ in punctuation
(a trailing period), match what slice 3's adviser test matched. If the
test already passes, read `DetailLines` before going on: the generic line
is written from some other run kind, and Step 3 is still needed only if
a Negotiation run reaches that case.

- [ ] **Step 3: Drop the generic line in a Negotiation**

In `DetailLines.scala`, the warband-move case's guard adds
`run.procedure != ActionRef.Negotiation &&` beside the other excluded
procedures, with a comment above the case: "A Negotiation writes its own
placement line (catalog batch 12, ruling 20)."

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: PASS.

- [ ] **Step 4: Write the golden logs**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Then: `./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite distantlands.application.gamelog.GameLogPropertiesSuite"`
Expected: PASS. Read both new `.log` files: each holds the "gave Broken
Peaks" line and the placement line, and no "Moved" line. No other golden
file changes (`git status` shows only the two new ones).

- [ ] **Step 5: Docs**

In `docs/architecture/all-exile-negotiation.md`:

- Remove Deed Writer from the ignored-handlers list.
- Add to the deal model's description: "A transfer row may hold sites
  where `DealRules` allows them: Deed Writer's faceup holder trades sites
  with anyone, under the power-use restrictions, and the start stores who
  may (`NegotiationStartFacts`). An author may give the sites their
  own-colour warbands hold, so every Imperial player may give every
  Imperial site, but never to another Imperial player or to a Vow of
  Wandering holder (`NegotiationSites`). Nobody may accept while a
  recipient would have fewer warbands on their board than sites they
  receive, counted after their own sites' warbands come back
  (`DecisionQuery.Negotiate.shortOfWarbands`). Settlement returns each
  given site's warbands to its old ruler, then asks each new ruler, site
  by site, how many to place (`deed-writer.warbands.<site>`)."

In the spec, set the status line to "Slices 1 to 4 landed."

Run: `python3 scripts/check-markdown-links.py`
Expected: no broken links.

- [ ] **Step 6: Full verification**

Run: `./sbtw clean coverage test coverageReport`
Expected: every suite passes, statement coverage at or above 92%.

Run: `python3 scripts/check-architecture.py`
Expected: "architecture check passed".

The panel change is covered by `NegotiationDealPanelSuite` under the
frontend's jsdom test run. The placement question uses the generic
amount panel, which needs no change; no browser check is needed for this
slice.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/application/gamelog/DetailLines.scala \
  src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/LogScripts.scala \
  src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala \
  src/test/resources/gamelog/negotiation-site-given.actor.log \
  src/test/resources/gamelog/negotiation-site-given.other.log \
  docs/architecture/all-exile-negotiation.md \
  docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 12 slice 4, site terms

A golden log script gives a site under Deed Writer: its lines name the
site and the warbands placed, and the generic warband line is not
written for a Negotiation.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage.** Rulings 18 (Task 3, own-colour and bandit tests;
  ruling 18's warband need is ruling 29's check), 19 (Task 3, two
  Imperial players), 20 (Task 3, swap test: returns, 1 to N, rule
  passing; Task 5, placement line), 27 (Task 1 blocker tests), 28 (Task
  3, a Citizen on an Exile's turn), 29 (Task 3 short and two-site tests,
  Task 4 withheld Accept). The spec's slice 4 tests: an own-colour site
  offered and a bandit or Imperial one refused, two Imperial players, a
  Citizen on an Exile's turn, City Wall, a Vow of Wandering holder, the
  empty board and the swap that covers it, a swap moving both sites, the
  returns and the 1-to-N question, rule passing. Catalog: Deed Writer
  leaves holding (Task 1).
- **Names.** `NegotiationSites.deedWriterUsers` (Task 1) is what
  `NegotiationStartFacts.at` calls (Task 3). `NegotiationTransfer.sites`
  (Task 2) is what Tasks 3 to 5 read. `NegotiationBounds.sites` /
  `siteRecipients` and `Negotiate.shortOfWarbands` (Task 3) are what Task
  4 projects. `NegotiationSites.warbandsDecisionId` (Task 3) is what the
  log script answers (Task 5).
- **Not in this slice.** `DealRules`' private and prefix fields and the
  `Secret` kind (slices 5 and 6); the deal prefix on the warbands
  question (slice 6).
