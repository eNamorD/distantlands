# Empire Slice 3b Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every remaining Citizenship source plays. Slice 3a built the role changes and two sources: the Grand Scepter's Action and self-exile. This slice adds:
- the Grand Scepter's battle plan, which exiles a defeated Citizen;
- the Grand Scepter's negotiation term, "becomes a Citizen";
- Martial Culture's battle plan;
- the When Played powers of Long-Lost Heir, Bewitch, Ancient Pact and Royal Ambitions;
- the Throne's Offer Citizenship step in the Chronicle.

**Architecture:**
- A role change has no actor. `Replace` gains `by: Option[PlayerId]`, as `Move` and `Discard` already have. A role change leaves it `None`, and Vow of Wandering refuses only a `Replace` its holder performs.
- `Citizenship` names its warband-split decision per player, so one tree can make several players Imperial. The two "Became" log lines move into a shared `RoleNotes` helper.
- Each source is a thin power over `Citizenship.become` or `Citizenship.exile`:
  - the two battle plans resolve at the Campaign's root window, as Salt the Earth does;
  - the four denizens share one When Played base, `CitizenshipWhenPlayed`;
  - three of the denizens take a random relic from the Reliquary first, through a new `ShufflePile.Reliquary`.
- The negotiation term is a new `citizenship` field on `NegotiationTerms`, which is offered through `NegotiationBounds`. On acceptance, `Citizenship.become` runs for each named Exile after the deal's transfers.
- In the Chronicle:
  - When the Empire breaks, Crown Chancellor puts one warband of the former Chancellor's colour on each site that held Imperial warbands (deviation from CR p. 36, Chronicle ruling 20).
  - Offer Citizenship lets the holder pick Exiles. Each offered Exile answers in seat order. An Exile who accepts swaps the warbands at each site they rule for one Imperial warband.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 3: Role changes in play", "Rulings" and
"Testing"). Task 10 corrects the spec where this plan's rulings change it.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/empire-slice-3b` on branch `empire-slice-3b`, off `0.2.0-alpha`. Never work in the main checkout.
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
- **Imperial roles:** game start keeps refusing them. Tests reach Imperial states through `Table.imperial`, `Table.relic` and the Chronicle fixtures.
- **Empire:** a game has an Empire while a Chancellor is seated (`Imperials.chancellor(ready).nonEmpty`).
- **Wire:** old journals need not decode. Every new operation or field gets a round-trip test.
- **Log lines** name only players other than the actor, never the actor: "Exiled p2.", not "p1 exiled p2."
- **Hover reasons:** show an unavailable option disabled with a reason. Hide only an option the player's current role can never use. Under the first-game option, a "become a Citizen" choice is shown disabled with `Citizenship.ignoredReason`.
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write`, `UNIMPLEMENTED_SNAPSHOT=write` and `GAMELOG_GOLDEN=write`, then read the diff. Commit only lines this task explains.

## Rulings for this slice

Decided with the user on 2026-11-01, or by the controller where noted.
Task 10 records them in `docs/rules/ambiguities.md`.

- **R20, actorless role changes (user).** Becoming a Citizen, at any source, is performed by no player. Its `Replace` operations carry `by = None`, so Vow of Wandering does not refuse them. The Vow refuses a `Replace` only when its `by` names the Vow's holder. Bandit Prince and Ballot Box set `by` to their actor.
- **R20, the Chronicle (controller).** Vow of Wandering holds no restriction in the Chronicle phase: its placements are procedural. Crown Chancellor's new placement (Chronicle ruling 20) would otherwise be refused for a former Chancellor who holds the Vow.
- **R21, "the most" (user).** "Have the most secrets" and "rule the most sites" mean strictly more than every other seated player; a tie fails.
  - Secrets are counted with `PlayerSecretSummary.totalSecrets`, the number shown under a player's area. It credits every secret on cards in play to the active player, the one playing the card.
  - Sites are counted with `PowerAccess.ruledSites`.
- **R22, empty Reliquary (user).** Bewitch, Ancient Pact and Royal Ambitions still make the player a Citizen when the Reliquary is empty. Instead of the relic, a line says "The Reliquary was empty."
- **Relic count (controller, from the card database).** Bewitch, Ancient Pact and Royal Ambitions each take a random relic from the Reliquary. Long-Lost Heir takes none. The spec says only Bewitch takes one; Task 10 corrects it. The relic is taken facedown, as `RelicDraws.takeTop` takes one.
- **R23, sides (user).** Martial Culture works on either side: the user wins against an Exile player, attacking or defending. The Grand Scepter's battle plan also works on either side: an Imperial user wins against a Citizen player. A defender who becomes a Citizen off-turn ends no Act phase; `RoleChanges.finish` already checks the active player.
- **R24, queued actions (user, provisional).** Actions a player queued for later, such as Second Wind's Travel and Campaign, still run after a role change ends that player's Act phase. Nothing changes in code.
- **Chronicle ruling 20, Empire breaks (user).** Deviation from CR p. 36. When an Exile wins, each site that held Imperial warbands, once they are removed, takes one warband of the former Chancellor's colour. The former Chancellor is then an Exile who rules those sites.
  - If they accept Citizenship at the Throne, their sites turn Imperial like any Exile's, so the Throne needs no "sites with no warbands" rule.
  - If they decline, they keep those sites.
  - A boxed former Chancellor (controller) places nothing, and those sites stay empty, as the CR says. A boxed lineage cannot accept at the Throne (ruling 21).
- **Chronicle ruling 21, Offer Citizenship (user).**
  - The Throne holder picks any number of seated Exiles, none included. Boxed Exiles are never offered.
  - Each offered Exile answers in seat order, with "Decline" first. Simultaneous answers wait for the walker's `Simultaneous` node, a ROADMAP item that Task 10 adds.
  - An Exile who accepts:
    - becomes a Citizen;
    - returns their board warbands to their own bank;
    - at each site they rule, swaps their warbands for one Imperial warband (`Kill` all but one, then `Replace` one).
  - No other Citizenship step runs in the Chronicle.
  - The first-game option does not apply. It covers powers, and the Throne step is a rule.
- **Chronicle ruling 22, unlimited purple at the Throne (user).** The Throne assumes an unlimited purple supply, since the step marks sites as the Empire's. When the purple bank is short, a new `ImperialWarbandsAdded(count)` raises the Imperial supply by the shortfall.
- **Per-player split (controller, after the user's question).** `Citizenship.replaceDecisionId(player)` is `role.replace-warbands.<player id>`, so one tree may make several players Imperial. The queued-procedure alternative would need a new procedure reference and log wiring for the same result.
- **Negotiation term (user and controller).**
  - Only the Grand Scepter's holder may propose the term, and only to another participant who is an Exile. The holder never names themself; they have the Scepter's Action for that (user).
  - The term may name several Exiles. Each becomes Imperial in participant order, after the deal's disclosures and transfers. In a game with no Empire, the first becomes the Chancellor, takes the Scepter, and the rest become Citizens.
  - The holder and the Exile status are checked against the state before the transfers, like the other terms. A deal that also gives the Scepter away still applies the term.
  - Under the first-game option, the bounds offer no Citizenship recipients. The editor shows the toggle disabled with `Citizenship.ignoredReason`.
- **Scepter battle plan (user).** It costs 3 burnt favor, and the text reads "you may exile them", as the card database prints it. The first-game option does not touch it: it is an exile, not a "become a Citizen" power. Council Seat stays unimplemented, so nothing protects the Citizen.
- **Declining (controller).** Every "you may" asks the player once, with "Decline" or its wording as the second option. A decline writes its own line.

## File map

| File | Responsibility |
|---|---|
| `model/CoreOperations.scala` | `Replace.by`; `ImperialWarbandsAdded` |
| `serialization/WalkerOperationCodec.scala` | `Replace.by`; `ImperialWarbandsAdded`; `ShufflePile.Reliquary` |
| `gameplay/powers/economy/VowOfWandering.scala` | skips a `Replace` its holder does not perform, and the Chronicle |
| `gameplay/powers/whenplayed/BanditPrince.scala`, `gameplay/powers/action/BallotBox.scala` | set `Replace.by` |
| `gameplay/roles/Citizenship.scala` | per-player split id |
| `gameplay/roles/RoleNotes.scala` (new) | the shared "Became a Citizen." and "Became the Chancellor." lines |
| `gameplay/powers/action/GrandScepterAction.scala` | uses `RoleNotes` and the new id |
| `gameplay/operations/GrandScepter.scala` | battle plan text and cost |
| `gameplay/powers/campaign/GrandScepterPlan.scala` (new) | the Scepter's battle plan |
| `gameplay/powers/campaign/MartialCulture.scala` (new) | Martial Culture card and plan |
| `gameplay/powers/campaign/SimplePlans.scala` | registers both plans |
| `model/ActionValues.scala`, `gameplay/operations/PileOperations.scala` | `ShufflePile.Reliquary` |
| `gameplay/powers/ReliquaryDraws.scala` (new) | a random relic from the Reliquary |
| `gameplay/powers/whenplayed/CitizenshipWhenPlayed.scala` (new) | the shared When Played base |
| `gameplay/powers/whenplayed/{LongLostHeir,Bewitch,AncientPact,RoyalAmbitions}.scala` (new) | the four denizens |
| `gameplay/powers/whenplayed/WhenPlayedPowers.scala` | registers them |
| `catalog/holding/Holding{Order,Beast,Arcane,Nomad,Discord}Denizens.scala` | the five cards leave |
| `model/NegotiationTerms.scala` | `citizenship` term and bound |
| `gameplay/actions/negotiation/{NegotiationDeal,NegotiationProcedure}.scala` | bounds, substance, settlement, lines |
| `gameplay/walker/DecisionQueries.scala` | validates the term |
| `serialization/NegotiationTermsCodec.scala`, `shared/.../protocol/{CommandIntents,CommandNestedCodecs}.scala`, `shared/.../protocol/projection/ActionProjectionDtos.scala`, `application/{GameIntentMapper,NegotiationDealProjector}.scala`, `serialization/ActionProjectionCodec.scala` | wire and projection |
| `frontend/.../{NegotiationDealPanel,ServerUiSupport,GameClient}.scala` | the toggle |
| `application/gamelog/ProcedureLog.scala` | classifies the new negotiation line |
| `gameplay/chronicle/ChronicleCrown.scala` | former Chancellor's warbands (Chronicle ruling 20) |
| `gameplay/chronicle/OfferCitizenship.scala` (new), `ChronicleThroneProcedure.scala` | the Throne's first step |
| `gameplay/operations/{OperationApplication,ChronicleStateOperations}.scala` | applies `ImperialWarbandsAdded` |

Paths without a prefix are under `src/main/scala/distantlands/`.

## Checkpoints

Dispatch a reviewer after Task 2 (mechanics), Task 6 (the card powers), Task 7 (negotiation) and Task 9 (Chronicle). The final whole-branch review runs after Task 10.

---

### Task 1: Actorless `Replace` and Vow of Wandering (R20)

**Files:**
- Modify: `model/CoreOperations.scala` (`Replace`, ~l.412)
- Modify: `serialization/WalkerOperationCodec.scala` (encode ~l.181, decode ~l.494)
- Modify: `gameplay/powers/whenplayed/BanditPrince.scala` (~l.86), `gameplay/powers/action/BallotBox.scala` (~l.102)
- Modify: `gameplay/powers/economy/VowOfWandering.scala` (`placing`, `places`)
- Modify: `gameplay/roles/RoleChanges.scala`, `gameplay/chronicle/ChronicleCrown.scala` (only the doc comment on `Replace` use: they already pass no `by`)
- Test: `src/test/scala/distantlands/gameplay/powers/economy/VowOfWanderingSuite.scala`, `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`, `src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala`

**Interfaces:**
- Produces: `Replace(removed, replacements, at, required = false, by: Option[PlayerId] = None)`. Its children are `Move(..., by = by)`.

- [ ] **Step 1: Write the failing tests**

In `VowOfWanderingSuite`, read the existing "Replace ... refused too" test (~l.112) and the `asCitizen` (~l.95) and `p2sTurn` (~l.146) helpers first. Change the existing test's `Replace` to pass `by = Some(<holder>)`; it must still be refused. Add:

```scala
  test("a Replace no player performs places nobody's warbands"):
    // The holder is a Citizen on their own turn, so Imperial warbands count
    // as theirs; a role change's Replace has no `by` and goes through.
    // Build the state as the existing Replace test does, then:
    val replace = Replace(Piece.Warbands(ForceKind.Exile(otherLineage), 1),
      Piece.Warbands(ForceKind.Imperial, 1),
      PositionedLocation(Location.Site(site)))
    assertEquals(restriction.reason(ready, replace), None)

  test("in the Chronicle the Vow refuses nothing"):
    // The holder's own warbands placed from their bank at a site, with the
    // turn in Phase.Chronicle.
    assertEquals(restriction.reason(chronicleReady, placing), None)
```

Fill `otherLineage`, `site`, `restriction`, `chronicleReady` and `placing` from the suite's existing fixtures; `Table.turn(p, Phase.Chronicle)` sets the phase.

In `WalkerOperationWireSuite` (~l.387), add a round trip of `Replace(..., by = Some(PlayerId("p1")))` next to the existing one, and assert the encoded JSON of a `by = None` replace has no `"by"` key.

In `CoreOperationsSuite`, assert `Replace(a, b, at, by = Some(p1)).children` are two `Move`s with `by = Some(p1)`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.economy.VowOfWanderingSuite oathdigital.serialization.WalkerOperationWireSuite oathdigital.gameplay.CoreOperationsSuite"`
Expected: compile failure, since `Replace` has no `by`.

- [ ] **Step 3: Implement**

`CoreOperations.scala`:

```scala
/** Swaps warbands for a new color. This is distinct from kill and sacrifice.
  * `by` is the player who performs it, as for `Move`; `None` when nobody
  * does, as for a role change (ruling R20), so no restriction against a
  * player holds. */
final case class Replace(removed: Piece.Warbands,
    replacements: Piece.Warbands, at: PositionedLocation,
    override val required: Boolean = false,
    by: Option[PlayerId] = None)
    extends CoreOperation:
  require(removed.amount == replacements.amount,
    "replace must exchange equal numbers of warbands")
  require(removed.kind != replacements.kind,
    "replacement warbands must have a new color")

  override val children: Vector[Operation] = Vector(
    Move(removed, at,
      PositionedLocation(Location.WarbandBank(removed.kind)), by = by),
    Move(replacements,
      PositionedLocation(Location.WarbandBank(replacements.kind)), at,
      by = by))
  override val simultaneous: Boolean = true
```

Codec encode: `case Replace(removed, replacements, at, _, by) => withMover(ujson.Obj(...same fields...), by)`. Decode: `yield Replace(removed, replacements, at, by = decodeMover(value))`.

`BanditPrince` and `BallotBox`: pass `by = Some(<their actor>)` to each `Replace` they build. Read the method to find the actor in scope.

`VowOfWandering.placing.reason`:

```scala
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] = holderOf(ready)
      .filter(_ => ready.game.current.turn.phase != Phase.Chronicle)
      .filter(holder =>
        leaves(holder, operation).exists(places(ready, holder, _)))
      .map(_ => OperationReason("vow-of-wandering", Reason,
        OperationReasonKind.Impossible))

  /** The leaves the holder performs: a `Replace` counts only when its `by`
    * names the holder (ruling R20). */
  private def leaves(holder: PlayerId, step: Operation): Vector[Operation] =
    step match
      case replace: Replace =>
        if replace.by.contains(holder) then replace.children else Vector.empty
      case _: LeafOperation => Vector(step)
      case _ => step.children.flatMap(leaves(holder, _))
```

Update the Vow's class doc: "It reads the leaves of a composite, so a `Replace` its holder performs is refused too; a `Replace` nobody performs, such as a role change's, is not (ruling R20). It holds no restriction in the Chronicle phase, whose placements are procedural."

Grep for every remaining `case Replace(` pattern with four fields (`grep -rn "Replace(" src/main/scala src/test/scala`) and add the fifth field.

- [ ] **Step 4: Run the tests and see them pass**

Run the same `testOnly`, then `./sbtw "testOnly *BanditPrince* *BallotBox* *RoleChanges* *ChronicleSun*"`.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/main/scala/distantlands/gameplay/powers/whenplayed/BanditPrince.scala src/main/scala/distantlands/gameplay/powers/action/BallotBox.scala src/main/scala/distantlands/gameplay/powers/economy/VowOfWandering.scala src/test/scala/distantlands/gameplay/powers/economy/VowOfWanderingSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala src/test/scala/distantlands/gameplay/CoreOperationsSuite.scala
git commit -m "feat(empire): role changes replace warbands with no actor (R20)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Per-player split id and shared role lines

**Files:**
- Modify: `gameplay/roles/Citizenship.scala`
- Create: `gameplay/roles/RoleNotes.scala`
- Modify: `gameplay/powers/action/GrandScepterAction.scala`
- Test: `src/test/scala/distantlands/gameplay/roles/CitizenshipSuite.scala`, and every test that names `Citizenship.replaceDecisionId` (grep)

**Interfaces:**
- Produces:
  - `Citizenship.replaceDecisionId(player: PlayerId): String`, which is `s"role.replace-warbands.${player.value}"`
  - `RoleNotes.declare(notes: NoteSet): RoleNotes`, a `final case class RoleNotes(citizen: NoteKey, chancellor: NoteKey)` with `def became(states: NoteStates, player: PlayerId, source: PowerSourceRef): Option[LogNote]`
- Consumed by Tasks 3, 4, 6, 7 and 9.

- [ ] **Step 1: Write the failing test**

In `CitizenshipSuite`, add a tree with two `Citizenship.become` nodes in one `Sequence`, for p2 and p3, with a purple bank too short for both (`Table` fixtures as the suite's existing short-bank test). Answer each player's own `replaceDecisionId(p)`. Assert both end Citizens with the splits they chose.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.CitizenshipSuite"`
Expected: compile failure.

- [ ] **Step 3: Implement**

In `Citizenship`, replace `val replaceDecisionId` with:

```scala
  /** The split a short purple bank asks `player`: one per player, so a tree
    * may make several players Imperial. */
  def replaceDecisionId(player: PlayerId): String =
    s"role.replace-warbands.${player.value}"
```

Use `replaceDecisionId(actor)` in `become`, `answered` and the `kept` failure message. Drop the doc sentence "so a tree may make one player Imperial once".

`RoleNotes.scala`:

```scala
package oathdigital.gameplay.roles

import oathdigital.model._

/** The lines a "become a Citizen" power writes, under its own name: "Became
  * a Citizen." or, in a game that had no Empire, "Became the Chancellor." */
final case class RoleNotes(citizen: NoteKey, chancellor: NoteKey):
  /** The line for `player`'s new role, once the change has run. */
  def became(states: NoteStates, player: PlayerId, source: PowerSourceRef)
      : Option[LogNote] =
    Imperials.role(states.now, player).collect {
      case Role.Chancellor => chancellor(source)
      case Role.Citizen => citizen(source)
    }

object RoleNotes:
  def declare(notes: NoteSet): RoleNotes = RoleNotes(
    notes.key(NoteKey.Used, Vector(NotePart.Text("Became a Citizen."))),
    notes.key("used.chancellor",
      Vector(NotePart.Text("Became the Chancellor."))))
```

If `NoteKey.apply(source)` has another shape, follow `GrandScepterAction`'s current `becameCitizen(card)` call.

In `GrandScepterAction`, replace the two keys and `becameNote` with:

```scala
  private val notes = NoteSet(id)
  val became: RoleNotes = RoleNotes.declare(notes)
  ...
      Right(Sequence(Vector(Citizenship.become(player),
        Note(id, states => PowerSourceRef.of(source).flatMap(card =>
          became.became(states, player, card))))))
```

Update `GrandScepterActionSuite`, `RoleChangesScenarioSuite` and any golden-log script that names `becameCitizen`, `becameChancellor` or `replaceDecisionId`.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.roles.* *GrandScepterAction* *RoleChangesScenario* *DeclaredNoteKeys* *GameLogGolden*"`
Expected: PASS. The note keys and golden logs are unchanged: the same ids, keys and wording.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/roles/Citizenship.scala src/main/scala/distantlands/gameplay/roles/RoleNotes.scala src/main/scala/distantlands/gameplay/powers/action/GrandScepterAction.scala <the updated tests>
git commit -m "refactor(empire): name the warband split per player; share role lines

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The Grand Scepter's battle plan

**Files:**
- Modify: `gameplay/operations/GrandScepter.scala` (`campaign` power)
- Create: `gameplay/powers/campaign/GrandScepterPlan.scala`
- Modify: `gameplay/powers/campaign/SimplePlans.scala` (register)
- Modify: `src/test/resources/powers/unimplemented.txt` (delete `relic.the-grand-scepter.campaign`)
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/GrandScepterPlanSuite.scala`

**Interfaces:**
- Consumes: `Citizenship.exile`, `BattlePlan`, `PlanContext.relic`, `PlanUse`.
- Produces: `GrandScepterPlan` (a `case object`), with `decisionId = CampaignProcedure.decisionPrefix + "grand-scepter.exile"`, `exile`/`spare` buttons, and notes `exiled` ("Exiled {0}.") and `spared` ("Did not exile {0}.").

- [ ] **Step 1: Write the failing tests**

Read `KeepSuite` or `ImperialTacticsSuite` for an Imperial Campaign fixture, and `SaltTheEarthSuite` for `commit(...).pick(...).finishPlanWindows`. Cover:
1. The Chancellor, holding the Scepter, attacks a site a Citizen rules and wins. With the plan picked and "exile" answered, the Citizen is an Exile and 3 favor are burnt. The line reads "Exiled p2."
2. The same, with "spare" answered: the Citizen stays a Citizen, and the line reads "Did not exile p2."
3. Defending: a Citizen attacks an Empire site, and the Chancellor defends with the Scepter and wins. The attacker is exiled.
4. Against an Exile defender, or with an Exile user, the plan is not offered.
5. A lost Campaign asks nothing.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.GrandScepterPlanSuite"`
Expected: compile failure.

- [ ] **Step 3: Implement**

`GrandScepter.scala`, `campaign`:

```scala
  val campaign = PrintedPower(PowerId("relic.the-grand-scepter.campaign"),
    persistent = false, cost = Cost(favorBurnt = 3),
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "[favor-burnt][favor-burnt][favor-burnt] If you are Imperial " +
      "and victorious against a Citizen, you may exile them.")
```

Import `oathdigital.catalog.ModifierMark`, and check the `Cost` field name against `DisgracedCaptain`.

`GrandScepterPlan.scala`:

```scala
package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.actions.campaign.CampaignProcedure
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._

/** The Grand Scepter's battle plan (3 burnt favor): "If you are Imperial
  * and victorious against a Citizen, you may exile them" (CR p. 34, card
  * database wording; ruling R23).
  *
  * Offered on either side to an Imperial user whose enemy is a Citizen.
  * Once the Campaign has resolved and the user won, the user chooses
  * whether to exile the Citizen; roles are read again then, since one may
  * have changed. Lines: "Exiled p2." and "Did not exile p2."
  */
case object GrandScepterPlan extends BattlePlan:
  val relic: RelicId = TheGrandScepterCard.id
  val id: PowerId = TheGrandScepterCard.campaign.id
  val decisionId: String =
    CampaignProcedure.decisionPrefix + "grand-scepter.exile"
  val exile: DecisionOptionRef.Button = DecisionOptionRef.Button("exile")
  val spare: DecisionOptionRef.Button = DecisionOptionRef.Button("spare")
  private val notes = NoteSet(id)
  /** "Exiled {0}." */
  val exiled: NoteKey = notes.key("exiled", Vector(NotePart.Text("Exiled "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "Did not exile {0}." */
  val spared: NoteKey = notes.key("spared", Vector(
    NotePart.Text("Did not exile "), NotePart.Arg(0), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def cardRef: DecisionOptionRef = DecisionOptionRef.Relic(relic)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] = for
    user <- context.user
    if Imperials.isImperial(context.ready, user)
    CampaignDefender.Player(enemy) <- Some(context.enemy)
    if Imperials.role(context.ready, enemy).contains(Role.Citizen)
    source <- context.relic(relic)
  yield CampaignPlanOffer(source,
    "The Grand Scepter: you may exile the Citizen if victorious",
    Vector(CampaignPlanCost.FavorBurnt(3)), Vector.empty)

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignActionEligibility -> (use => (for
      user <- use.user
      result <- use.result
      if use.won.contains(true)
      citizen <- opponent(use, result)
    yield Vector[Operation](Branch((live, _) =>
      if !Imperials.isImperial(live, user) ||
          !Imperials.role(live, citizen).contains(Role.Citizen) then
        Vector.empty
      else Vector(
        Decide(decisionId, user, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(exile, s"Exile ${citizen.value}"),
          DecisionOption.Button(spare, "Do not exile")),
          heading = Some("The Grand Scepter: you may exile the Citizen"))),
        Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId).contains(exile) then
            Vector(Citizenship.exile(citizen),
              Note(id, _ => Some(exiled(PowerSourceRef.Card(relic),
                NoteArg.Player(citizen)))))
          else Vector(Note(id, _ => Some(spared(PowerSourceRef.Card(relic),
            NoteArg.Player(citizen))))))))))
      .getOrElse(Vector.empty)))

  /** The player on the other side. */
  private def opponent(use: PlanUse, result: CampaignResult)
      : Option[PlayerId] = use.side match
    case CampaignPlanSide.Attacker => result.defender match
      case CampaignDefender.Player(player) => Some(player)
      case CampaignDefender.Bandits => None
    case CampaignPlanSide.Defender => Some(result.attacker)
```

Check the names `Imperials.isImperial`, `PlanContext.ready`, `CampaignResult.attacker` and how a note key takes a source (`exiled(source, args*)`) against the neighbouring plans, and adjust. If the `for` pattern on `Some(context.enemy)` warns under `-Werror`, use `context.enemy match` instead.

Register `GrandScepterPlan` in `SimplePlans.forCatalog`, beside the other relic plans. Delete the `unimplemented.txt` line, then regenerate `declared-keys.txt`.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly *GrandScepterPlan* *UnimplementedPowers* *DeclaredNoteKeys* *LogNoteCatalog* *CardCatalog*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/GrandScepter.scala src/main/scala/distantlands/gameplay/powers/campaign/GrandScepterPlan.scala src/main/scala/distantlands/gameplay/powers/campaign/SimplePlans.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/campaign/GrandScepterPlanSuite.scala
git commit -m "feat(empire): the Grand Scepter's battle plan exiles a defeated Citizen

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Martial Culture

**Files:**
- Modify: `catalog/holding/HoldingOrderDenizens.scala` (remove `MartialCultureCard`)
- Create: `gameplay/powers/campaign/MartialCulture.scala`
- Modify: `gameplay/powers/campaign/SimplePlans.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/MartialCultureSuite.scala`

**Interfaces:**
- Consumes: `Citizenship.become`, `Citizenship.ignored`, `Citizenship.ignoredReason`, `RoleNotes`.
- Produces: `MartialCulture` (`case object`), `MartialCultureCard`, `decisionId = CampaignProcedure.decisionPrefix + "martial-culture"`, buttons `become`/`decline`, `became: RoleNotes`, note `declined` ("Declined Citizenship.").

- [ ] **Step 1: Write the failing tests**

Use `SaltTheEarthSuite`'s `CampaignFixture` boards. Martial Culture stands faceup at a site the user rules; read `TributeSpoilsSuite` for a site-card plan. Turn the first-game option off with `Table.allowingCitizenshipPowers`, or the board equivalent; read `GrandScepterActionSuite` for how. Cover:
1. With an Empire in play, an Exile attacker beats an Exile defender and answers "become": they are a Citizen, their Act has ended, and the line reads "Became a Citizen."
2. With no Empire, the line reads "Became the Chancellor."
3. "decline" leaves the role unchanged and writes "Declined Citizenship."
4. Against bandits, the plan is not offered.
5. An Exile defender who beats an Exile attacker may become a Citizen, and the turn's phase is unchanged.
6. Under the first-game option, the plan is offered with `withheld = Some(OptionReason.Written(Citizenship.ignoredReason))`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.MartialCultureSuite"`
Expected: compile failure.

- [ ] **Step 3: Implement**

Move `MartialCultureCard` unchanged into `MartialCulture.scala`, then add:

```scala
/** Martial Culture (card 10, site-only), a battle plan on either side: "If
  * you're an Exile and defeat another Exile, you may become a Citizen"
  * (ruling R23).
  *
  * Offered to an Exile user whose enemy is an Exile player, and shown
  * withheld under the first-game option. Once the user has won, they choose
  * whether to become a Citizen, or the Chancellor in a game with no Empire.
  * Lines: "Became a Citizen.", "Became the Chancellor." and "Declined
  * Citizenship."
  */
case object MartialCulture extends BattlePlan:
  val cardId: DenizenId = MartialCultureCard.id
  val id: PowerId = MartialCultureCard.power.id
  val decisionId: String = CampaignProcedure.decisionPrefix + "martial-culture"
  val become: DecisionOptionRef.Button = DecisionOptionRef.Button("become")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  private val notes = NoteSet(id)
  val became: RoleNotes = RoleNotes.declare(notes)
  /** "Declined Citizenship." */
  val declined: NoteKey = notes.key("declined",
    Vector(NotePart.Text("Declined Citizenship.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)
  private def card: PowerSourceRef = PowerSourceRef.Card(cardId)

  def cardRef: DecisionOptionRef = DecisionOptionRef.Denizen(cardId)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] = for
    user <- context.user
    if exile(context.ready, user)
    enemy <- context.enemy match
      case CampaignDefender.Player(player) => Some(player)
      case CampaignDefender.Bandits => None
    if exile(context.ready, enemy)
    source <- context.denizen(cardId)
  yield CampaignPlanOffer(source,
    "Martial Culture: you may become a Citizen if victorious",
    Vector.empty, Vector.empty,
    withheld = Option.when(Citizenship.ignored(context.ready))(
      OptionReason.Written(Citizenship.ignoredReason)))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignActionEligibility -> (use => (for
      user <- use.user
      if use.won.contains(true)
    yield Vector[Operation](Branch((live, _) =>
      if !exile(live, user) then Vector.empty
      else Vector(
        Decide(decisionId, user, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(become, "Become a Citizen"),
          DecisionOption.Button(decline, "Stay an Exile")),
          heading = Some("Martial Culture: you may become a Citizen"))),
        Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId).contains(become) then
            Vector(Citizenship.become(user),
              Note(id, became.became(_, user, card)))
          else Vector(Note(id, _ => Some(declined(card))))))))))
      .getOrElse(Vector.empty)))

  private def exile(ready: ReadyGame, player: PlayerId): Boolean =
    Imperials.role(ready, player).contains(Role.Exile)
```

Register it in `SimplePlans`, delete its `unimplemented.txt` line, regenerate `declared-keys.txt`, and remove the holding file's unused imports.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly *MartialCulture* *UnimplementedPowers* *DeclaredNoteKeys* *LogNoteCatalog* *CardCatalog*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/catalog/holding/HoldingOrderDenizens.scala src/main/scala/distantlands/gameplay/powers/campaign/MartialCulture.scala src/main/scala/distantlands/gameplay/powers/campaign/SimplePlans.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/campaign/MartialCultureSuite.scala
git commit -m "feat(empire): Martial Culture makes a victorious Exile a Citizen

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: A random relic from the Reliquary

**Files:**
- Modify: `model/ActionValues.scala` (`ShufflePile.Reliquary`)
- Modify: `gameplay/operations/PileOperations.scala` (`withCards`, ~l.28-44)
- Modify: `serialization/WalkerOperationCodec.scala` (shuffle pile codec, ~l.419-433)
- Create: `gameplay/powers/ReliquaryDraws.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/ReliquaryDrawsSuite.scala`, `WalkerOperationWireSuite`

**Interfaces:**
- Produces:
  - `ShufflePile.Reliquary`
  - `ReliquaryDraws.steps(power: PowerId, player: PlayerId, source: PowerSourceRef): Vector[Operation]`: shuffle the Reliquary and take its first relic facedown into `player`'s play area. With the Reliquary empty, nothing moves and the line reads "The Reliquary was empty."
  - `ReliquaryDraws.notes: NoteSet` (unowned), `ReliquaryDraws.empty: NoteKey`

- [ ] **Step 1: Write the failing tests**

`ReliquaryDrawsSuite` walks `Sequence(ReliquaryDraws.steps(...))` with the walker's harness, the way `RelicDraws`' suite does (read it first). Cover:
1. With a three-relic Reliquary, the player holds one of the three facedown, two stay, and the replay is equal.
2. With an empty Reliquary, nothing moves and the empty line is written.

Also round-trip `Shuffle(ShufflePile.Reliquary)` in `WalkerOperationWireSuite`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly *ReliquaryDraws* *WalkerOperationWire*"`
Expected: compile failure.

- [ ] **Step 3: Implement**

`ShufflePile`: add `case object Reliquary extends ShufflePile`, with `cards` returning `game.campaign.reliquary` and `name` returning `"Reliquary"`. In `PileOperations.withCards`, write `campaign.reliquary` the way `SuitedReserve` writes a campaign-level pile. Encode the pile in the codec as `"reliquary"`.

`ReliquaryDraws.scala`:

```scala
package oathdigital.gameplay.powers

import oathdigital.model._

/** A random relic from the Reliquary, taken facedown (Bewitch, Ancient
  * Pact, Royal Ambitions). The draw shuffles the Reliquary, so the walker
  * journals its order, then takes the first relic. An empty Reliquary takes
  * nothing (ruling R22). */
object ReliquaryDraws:
  val notes: NoteSet = NoteSet.unowned("ReliquaryDraws")
  /** "The Reliquary was empty." */
  val empty: NoteKey = notes.key("reliquary-empty",
    Vector(NotePart.Text("The Reliquary was empty.")))

  def steps(power: PowerId, player: PlayerId, source: PowerSourceRef)
      : Vector[Operation] = Vector(Branch((live, _) =>
    if live.game.campaign.reliquary.isEmpty then
      Vector(Note(power, _ => Some(empty(source))))
    else Vector(
      Shuffle(ShufflePile.Reliquary),
      BuildOps((ready, _) => Right(ready.game.campaign.reliquary.headOption
        .toVector.map(relic => Move(Piece.Card(relic),
          PositionedLocation(Location.Reliquary),
          PositionedLocation(Location.PlayArea(player)),
          resultingOrientation = Some(Orientation.FaceDown)))),
        required = true))))
```

Check that `Shuffle` is an `Operation` the walker can place in a `Vector[Operation]` (see `ClayRattle.scala:43`). Check `RelicDraws.emptyDeck` for how an unowned key is borrowed by the powers that write it.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly *ReliquaryDraws* *WalkerOperationWire* *PileOperations*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/ActionValues.scala src/main/scala/distantlands/gameplay/operations/PileOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/main/scala/distantlands/gameplay/powers/ReliquaryDraws.scala src/test/scala/distantlands/gameplay/powers/ReliquaryDrawsSuite.scala src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(empire): take a random relic from the Reliquary

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The four "become a Citizen" denizens

**Files:**
- Create: `gameplay/powers/whenplayed/CitizenshipWhenPlayed.scala`
- Create: `gameplay/powers/whenplayed/LongLostHeir.scala`, `Bewitch.scala`, `AncientPact.scala`, `RoyalAmbitions.scala` (each moves its `...Card` object out of the holding file)
- Modify: `catalog/holding/Holding{Beast,Arcane,Nomad,Discord}Denizens.scala`
- Modify: `gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala`

**Interfaces:**
- Consumes: `Citizenship`, `RoleNotes`, `ReliquaryDraws.steps`, `PlayerSecretSummary.derive`, `PowerAccess.ruledSites`, `CardPowerBanners.holdsDarkestSecret`.
- Produces: `CitizenshipWhenPlayed`, with the members `decisionId`, `become`, `decline`, `became`, `declined`, `notExile` ("Was not an Exile.") and `unmet`. Also the four powers, whose decision ids are `cardplay.<card-key>.citizenship`.

- [ ] **Step 1: Write the failing tests**

Use `WhenPlayedHarness` as `TrueOathSuite` does (`registered[...]`, `hook(card)`, `walkAnswering`, `chooseOne`). Turn the first-game option off with `Table.allowingCitizenshipPowers`. Cover:
1. **Long-Lost Heir:** an Exile in an Empire game answers "become": they are a Citizen, their Act has ended, and the line reads "Became a Citizen." Answering "decline" writes "Declined Citizenship."
2. **Long-Lost Heir played by a Citizen:** nothing is asked, and the line reads "Was not an Exile."
3. **Bewitch:** with strictly more total secrets than every other player (`PlayerSecretSummary.totalSecrets`), it asks, and "become" takes a Reliquary relic facedown, then makes the player a Citizen. A tie asks nothing and writes "Did not have the most secrets."
4. **Ancient Pact:** holding the Darkest Secret asks. Not holding it writes "Did not hold the Darkest Secret."
5. **Royal Ambitions:** strictly the most `ruledSites` asks. A tie writes "Did not rule the most sites."
6. **Empty Reliquary:** Bewitch writes "The Reliquary was empty." and still makes the player a Citizen.
7. **First-game option:** "become" is `DecisionOption.Unavailable(..., OptionReason.Written(Citizenship.ignoredReason))`, and "decline" stays available.
8. **No Empire:** Long-Lost Heir makes the player the Chancellor, with the line "Became the Chancellor."

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly *CitizenshipWhenPlayed*"`
Expected: compile failure.

- [ ] **Step 3: Implement**

`CitizenshipWhenPlayed.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.{PowerAnswers, ReliquaryDraws}
import oathdigital.gameplay.roles.{Citizenship, RoleNotes}
import oathdigital.model._

/** A WHEN PLAYED "if you're an Exile and ..., you may [take a random relic
  * from the Reliquary and] become a Citizen" (Long-Lost Heir, Bewitch,
  * Ancient Pact, Royal Ambitions).
  *
  * Read when the card resolves: a player who is not an Exile, or does not
  * meet `condition`, is asked nothing and gets a line saying why. Otherwise
  * the player chooses to become a Citizen, which is shown unavailable under
  * the first-game option, or to decline. Becoming one first takes the relic
  * when `takesRelic`, then runs `Citizenship.become`.
  *
  * Lines: "Became a Citizen.", "Became the Chancellor.", "Declined
  * Citizenship.", "Was not an Exile." and the card's own unmet line. */
trait CitizenshipWhenPlayed extends WhenPlayedPower:
  /** The card's extra condition, `None` for Long-Lost Heir. */
  def condition: Option[(ReadyGame, PlayerId) => Boolean]
  def takesRelic: Boolean
  /** The unmet condition's wording, as "Did not hold the Darkest Secret." */
  def unmetText: String
  def decisionId: String

  val become: DecisionOptionRef.Button = DecisionOptionRef.Button("become")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  protected lazy val notes: NoteSet = NoteSet(id)
  lazy val became: RoleNotes = RoleNotes.declare(notes)
  lazy val declined: NoteKey = notes.key("declined",
    Vector(NotePart.Text("Declined Citizenship.")))
  lazy val notExile: NoteKey = notes.key("not-exile",
    Vector(NotePart.Text("Was not an Exile.")))
  lazy val unmet: NoteKey = notes.key("unmet",
    Vector(NotePart.Text(unmetText)))
  override def noteKeys: Vector[NoteKey] =
    Vector(became.citizen, became.chancellor, declined, notExile) ++
      condition.map(_ => unmet).toVector ++
      Option.when(takesRelic)(ReliquaryDraws.empty).toVector
  override def narratedDecisions: Set[String] = Set(decisionId)

  private def card: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    val actor = ctx.actor
    Vector(Branch((live, _) =>
      if !Imperials.role(live, actor).contains(Role.Exile) then
        Vector(Note(id, _ => Some(notExile(card))))
      else if condition.exists(meets => !meets(live, actor)) then
        Vector(Note(id, _ => Some(unmet(card))))
      else Vector(
        Decide(decisionId, actor, DecisionQuery.ChooseOne(Vector(
          offer(live), DecisionOption.Button(decline, "Stay an Exile")),
          heading = Some(s"$name: you may become a Citizen"))),
        Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId).contains(become) then
            (if takesRelic then ReliquaryDraws.steps(id, actor, card)
              else Vector.empty) ++ Vector(Citizenship.become(actor),
              Note(id, became.became(_, actor, card)))
          else Vector(Note(id, _ => Some(declined(card))))))))

  private def offer(ready: ReadyGame): DecisionOption =
    val button = DecisionOption.Button(become, "Become a Citizen")
    if Citizenship.ignored(ready) then DecisionOption.Unavailable(button,
      OptionReason.Written(Citizenship.ignoredReason))
    else button

  /** The card's name, for the question's heading. */
  def name: String
```

`ReliquaryDraws.empty` belongs to an unowned set. Follow how a power borrows `RelicDraws.emptyDeck` (`notes.borrow(...)`), so `DeclaredNoteKeysSuite` lists it under each power that writes it. If `lazy val` keys trip `LogNoteCatalogSuite`'s declaration order, give each concrete power a plain `NoteSet` and pass the keys in.

A shared helper for the strict maximum, in the base's companion object:

```scala
object CitizenshipWhenPlayed:
  /** Whether `player`'s count is strictly more than every other seated
    * player's (ruling R21). */
  def most(ready: ReadyGame, player: PlayerId)(count: PlayerId => Int)
      : Boolean =
    val mine = count(player)
    ready.game.current.players.map(_.player).filter(_ != player)
      .forall(count(_) < mine)
```

The four powers, each in its own file with its `...Card` moved in unchanged:

```scala
case object LongLostHeir extends CitizenshipWhenPlayed:
  val cardId: DenizenId = LongLostHeirCard.id
  val id: PowerId = LongLostHeirCard.power.id
  val decisionId: String = "cardplay.long-lost-heir.citizenship"
  val name: String = "Long-Lost Heir"
  val condition: Option[(ReadyGame, PlayerId) => Boolean] = None
  val takesRelic: Boolean = false
  val unmetText: String = "Met no condition."

case object Bewitch extends CitizenshipWhenPlayed:
  ...
  val decisionId: String = "cardplay.bewitch.citizenship"
  val name: String = "Bewitch"
  val condition = Some((ready: ReadyGame, player: PlayerId) =>
    CitizenshipWhenPlayed.most(ready, player)(p =>
      PlayerSecretSummary.derive(ready, p).fold(_ => 0, _.totalSecrets)))
  val takesRelic: Boolean = true
  val unmetText: String = "Did not have the most secrets."

case object AncientPact extends CitizenshipWhenPlayed:
  ...
  val condition = Some((ready: ReadyGame, player: PlayerId) =>
    CardPowerBanners.holdsDarkestSecret(ready, player))
  val takesRelic: Boolean = true
  val unmetText: String = "Did not hold the Darkest Secret."

case object RoyalAmbitions extends CitizenshipWhenPlayed:
  ...
  val condition = Some((ready: ReadyGame, player: PlayerId) =>
    CitizenshipWhenPlayed.most(ready, player)(p =>
      PowerAccess.ruledSites(ready, p).size))
  val takesRelic: Boolean = true
  val unmetText: String = "Did not rule the most sites."
```

Long-Lost Heir has no condition, so its `unmet` key is never declared (`condition.map(...)` above); its `unmetText` is never read. Each power's doc comment quotes its card text and names ruling R21 or R22 where it applies. Register all four in `WhenPlayedPowers.forCatalog`, delete their four `unimplemented.txt` lines, regenerate `declared-keys.txt`, and remove the holding files' unused imports.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly *CitizenshipWhenPlayed* *UnimplementedPowers* *DeclaredNoteKeys* *LogNoteCatalog* *CardCatalog* *WhenPlayed*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add <the four new power files> src/main/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayed.scala src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala src/main/scala/distantlands/catalog/holding/HoldingBeastDenizens.scala src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/whenplayed/CitizenshipWhenPlayedSuite.scala
git commit -m "feat(empire): four When Played denizens make an Exile a Citizen

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: The Citizenship term in negotiation

**Files:**
- Modify: `model/NegotiationTerms.scala`
- Modify: `gameplay/actions/negotiation/NegotiationDeal.scala`, `NegotiationProcedure.scala`
- Modify: `gameplay/walker/DecisionQueries.scala` (`acceptsTerms`)
- Modify: `serialization/NegotiationTermsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`, `CommandNestedCodecs.scala`, `projection/ActionProjectionDtos.scala`
- Modify: `application/GameIntentMapper.scala`, `application/NegotiationDealProjector.scala`, `serialization/ActionProjectionCodec.scala`
- Modify: `application/gamelog/ProcedureLog.scala` (the Negotiation `Notes` map)
- Modify: `frontend/src/main/scala/distantlands/frontend/{NegotiationDealPanel,ServerUiSupport,GameClient}.scala`
- Modify: `gameplay/operations/GrandScepter.scala` (no change to the `negotiation` printed power; delete its `unimplemented.txt` line)
- Test: `NegotiationDealSuite`, `NegotiationProcedureSuite`, `DecisionQuerySuite`, `WalkerDecisionWireSuite`, `NegotiationDealProjectionSuite`, `GameIntentMapperNegotiationSuite`, `shared/.../CommandProtocolSuite`, `ProjectionProtocolSuite`, `frontend/.../NegotiationDealPanelSuite`, `ServerUiSupportSuite`

**Interfaces:**
- Produces:
  - `NegotiationTerms(transfers, disclosures, citizenship: Vector[PlayerId] = Vector.empty)`. It requires distinct entries.
  - `NegotiationBounds(..., citizenship: Vector[PlayerId], citizenshipRefused: Option[String])`. `citizenship` is the participants this author may offer Citizenship to. `citizenshipRefused` is the hover reason when they hold the Scepter but the first-game option is on.
  - `NegotiationProcedure.offeredCitizenship` note: "{0} offered Citizenship to {1}." (the author is named, as the other deal lines name theirs), followed by the role change's own lines.
  - Projection: `NegotiationEditingProjection.citizenship: Vector[String]`, `citizenshipRefused: Option[String]`; `NegotiationTermsProjection` and input DTOs gain `citizenship: Vector[String]`.

- [ ] **Step 1: Write the failing tests**

1. `NegotiationDealSuite`:
   - The Scepter's holder gets `citizenship` bounds naming exactly the Exile participants who are not themself.
   - A non-holder gets none.
   - Under the first-game option the list is empty and `citizenshipRefused == Some(Citizenship.ignoredReason)`.
   - `hasSubstance` is true for a citizenship-only deal.
2. `DecisionQuerySuite`: `ProposeTerms` with a citizenship recipient outside the author's bound is rejected with "offers Citizenship it cannot".
3. `NegotiationProcedureSuite`:
   - The Chancellor, holding the Scepter, offers Citizenship to p2 and p3 in one deal. Both accept. p2 and p3 are Citizens, each answering their own `Citizenship.replaceDecisionId` when the bank is short.
   - The lines include "p1 offered Citizenship to p2 and p3."
   - A deal whose recipient is no longer an Exile at settlement is refused.
4. The wire suites round-trip the new fields. A projection suite shows the editing bounds to the author only.
5. `NegotiationDealPanelSuite`: the author's per-recipient row shows a "Citizenship" checkbox for each listed recipient. It is disabled, with the reason as its title, when `citizenshipRefused` is set. Saving sends `citizenship`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly *Negotiation* *DecisionQuerySuite *WalkerDecisionWire*" "sharedJVM/testOnly *ProtocolSuite" "frontend/testOnly *NegotiationDealPanel* *ServerUiSupport*"`
Use the project ids in `build.sbt` for the shared and frontend modules.
Expected: compile failures.

- [ ] **Step 3: Implement the model and rules**

`NegotiationTerms`: add `citizenship: Vector[PlayerId] = Vector.empty`, with `require(citizenship.distinct.size == citizenship.size, "Negotiation Citizenship offers must be distinct")`. Add the two `NegotiationBounds` fields.

`NegotiationDeal`:

```scala
  def hasSubstance: Boolean = terms.values.exists(t =>
    t.transfers.exists(x => x.favor > 0 || x.relics.nonEmpty) ||
      t.disclosures.nonEmpty || t.citizenship.nonEmpty)
```

In `bounds`:

```scala
    val holder = player.relics.exists(_.id == TheGrandScepterCard.id)
    val exiles = participants.filter(other => other != author &&
      Imperials.role(state, other).contains(Role.Exile))
    val ignored = holder && Citizenship.ignored(state)
    NegotiationBounds(participants.filter(_ != author), player.board.favor,
      player.relics.map(_.id), disclosable(state, player),
      citizenship = if holder && !ignored then exiles else Vector.empty,
      citizenshipRefused = Option.when(ignored && exiles.nonEmpty)(
        Citizenship.ignoredReason))
```

In `affordable`, also check `terms.citizenship.isEmpty ||` (the author holds the Scepter, and every named player is an Exile who is not the author). On failure, return `NegotiationUnavailable("a Citizenship offer no longer stands")`.

Add the citizenship recipients to `NegotiationDeal`, in participant order:

```scala
  /** The players an agreed deal makes Imperial, in participant order. */
  def citizens(deal: DealState): Vector[PlayerId] =
    deal.participants.flatMap(deal.terms(_).citizenship).distinct
```

`NegotiationProcedure.settlement`: after the `BuildOps` and its lines, append, for each `citizen` in `NegotiationDeal.citizens(deal)`, `Citizenship.become(citizen)` followed by one `offeredCitizenship` note per author who offered. Declare:

```scala
  /** "p1 offered Citizenship to p2 and p3." */
  val offeredCitizenship: NoteKey = notes.key("offered-citizenship", Vector(
    NotePart.Arg(0), NotePart.Text(" offered Citizenship to "),
    NotePart.Arg(1), NotePart.Text(".")))
```

Write the offer line before the role changes, so the log reads offer, then replacements and title lines. The role change's "Became" line is not written here: the generic lines show the change. Add `offered-citizenship` to the Negotiation `Notes(Map(...))` in `ProcedureLog.scala` as `LogKind.Delta`.

`DecisionQueries.acceptsTerms`: add

```scala
          _ <- require(terms.citizenship.forall(own.citizenship.contains),
            decisionId, "offers Citizenship it cannot")
```

and add `terms.citizenship` to `recipients`.

- [ ] **Step 4: Implement wire, projection and frontend**

- `NegotiationTermsCodec`: encode `"citizenship"` as an array of player ids; decode it as required (old journals need not decode).
- Shared protocol `NegotiationTerms` DTO: add `citizenship: Vector[String]`. In the codecs, add `"citizenship"` to the `exact(...)` key set and encode it.
- `GameIntentMapper`: map it to `PlayerId`.
- `NegotiationEditingProjection`: add `citizenship: Vector[String]` and `citizenshipRefused: Option[String]`. The terms projection carries `citizenship`.
- `NegotiationDealProjector`: fill both from the viewer's own bounds; everyone sees the terms.
- `NegotiationDealPanel.editor`, in the per-recipient loop: when the recipient is in `editing.citizenship`, or `citizenshipRefused` is set and the recipient is an Exile, add a checkbox labelled "Offer Citizenship". It is checked from the current terms and disabled with `title := reason` when refused.
  - The `summary` renders "Offers Citizenship to p2".
  - `ServerUiSupport.protocolNegotiationTerms` and `GameClient`'s input type carry it.

Delete `relic.the-grand-scepter.negotiation` from `unimplemented.txt`. Check how `PowerImplementationStatus` counts a printed power with no walker power; if the line cannot be deleted, register the negotiation power the way `OperationRestrictions.printedRules` lists `GrandScepter`, and say so in the report.

- [ ] **Step 5: Run the tests and see them pass**

Run the Step 2 command, then `./sbtw "testOnly *DeclaredNoteKeys* *UnimplementedPowers* *GameLogProperties*"`.
Expected: PASS.

- [ ] **Step 6: Commit**

Add every changed file by name.

```bash
git commit -m "feat(empire): the Grand Scepter's holder offers Citizenship in a deal

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Crown Chancellor leaves the former Chancellor's warbands (Chronicle ruling 20)

**Files:**
- Modify: `gameplay/chronicle/ChronicleCrown.scala` (`empireBreaks`)
- Test: `src/test/scala/distantlands/gameplay/chronicle/ChronicleSunSuite.scala`

**Interfaces:**
- Produces: after an Exile's win, each in-play site that held Imperial warbands holds one warband of the former Chancellor's colour. If the former Chancellor is boxed, those sites stay empty.

- [ ] **Step 1: Write the failing tests**

In `ChronicleSunSuite`, next to "an Exile winner becomes Chancellor...":
1. A seated former Chancellor p2, Imperial warbands at two sites, and p3 wins as an Exile. After Crown, each of those sites holds `Occupied(Exile(p2's lineage), 1)`.
2. A boxed former Chancellor (use `withBoxedLineage(table, Role.Chancellor)`) leaves those sites `SiteForces.Empty`.
3. A former Chancellor holding Vow of Wandering faceup: Crown still succeeds (R20, the Chronicle).

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.ChronicleSunSuite"`
Expected: FAIL. The sites are empty.

- [ ] **Step 3: Implement**

In `empireBreaks`, read the former Chancellor among the seated players before the role changes:

```scala
    val former = current.players.find(other => other.player != player.player &&
      ready.game.campaign.lineages.get(other.lineage)
        .exists(_.role == Role.Chancellor))
    val held = former.toVector.flatMap(chancellor => forces.collect {
      case (site, SiteForces.Occupied(ForceKind.Imperial, _)) =>
        Move(Piece.Warbands(ForceKind.Exile(chancellor.lineage), 1),
          PositionedLocation(Location.WarbandBank(
            ForceKind.Exile(chancellor.lineage))), at(site))
    })
```

Emit `held` right after `imperialRemoved`. Update the doc comment:

> "Imperial warbands leave the map without bandits taking their place; each site they held then takes one warband of the former Chancellor's colour, so the former Chancellor rules it as an Exile (Chronicle ruling 20, a deviation from CR p. 36). A boxed former Chancellor places none."

The former Chancellor's own bank holds 14, and a map has at most 8 sites, so no shortfall arises.

- [ ] **Step 4: Run the tests and see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.*"`
Expected: PASS. Fix any end-to-end expectation the new pieces move, and explain each change in the commit body.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ChronicleCrown.scala src/test/scala/distantlands/gameplay/chronicle/ChronicleSunSuite.scala
git commit -m "feat(chronicle): a broken Empire leaves its sites to the former Chancellor

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: The Throne's Offer Citizenship (Chronicle rulings 21, 22)

**Files:**
- Modify: `model/CoreOperations.scala` (`ImperialWarbandsAdded`)
- Modify: `gameplay/operations/OperationApplication.scala`, `ChronicleStateOperations.scala`
- Modify: `serialization/WalkerOperationCodec.scala`
- Create: `gameplay/chronicle/OfferCitizenship.scala`
- Modify: `gameplay/chronicle/ChronicleThroneProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/OfferCitizenshipSuite.scala`, `ChronicleStateOperationsSuite`, `WalkerOperationWireSuite`, and fixes in `ShapeEmpireSuite` and `LineageScoringSuite`

**Interfaces:**
- Produces:
  - `ImperialWarbandsAdded(count: Int)`, which requires `count > 0` and adds `count` to `warbandSupply(Imperial)`. It requires an Imperial supply to exist.
  - `OfferCitizenship.offerDecisionId = "chronicle.throne.offer"`
  - `OfferCitizenship.answerDecisionId(player) = s"chronicle.throne.citizenship.${player.value}"`
  - `accept`/`decline` buttons
  - notes `offered` ("Offered Citizenship to {0}."), `accepted` ("{0} accepted Citizenship.") and `declined` ("{0} declined Citizenship.")
  - `OfferCitizenship.steps: Vector[Operation]`

- [ ] **Step 1: Write the failing tests**

`ChronicleStateOperationsSuite`: `ImperialWarbandsAdded(3)` raises a supply of 24 to 27, and is refused with no supply. `WalkerOperationWireSuite`: round-trip it.

`OfferCitizenshipSuite`, built on `ChronicleWalk.throneHeldByChancellorP1` (p1 Chancellor, p2 and p3 Exiles):
1. The first parked decision is `offerDecisionId`, owned by p1, a `ChooseMany(0, 2, …)` of p2 and p3.
2. Offering none moves on to Shape Empire.
3. Offering p2 and p3:
   - p2's answer parks first, then p3's;
   - each `ChooseOne` lists Decline first.
4. p2 accepts while holding 3 warbands at a site they rule and 2 on their board:
   - p2 is a Citizen;
   - the site holds 1 Imperial warband;
   - p2's board holds no warbands;
   - lines: "Offered Citizenship to p2 and p3." and "p2 accepted Citizenship."
5. p3 declines: role and warbands are unchanged, and the line reads "p3 declined Citizenship."
6. With the purple bank short by 2 (fixture supply 24, minus warbands placed by hand), `ImperialWarbandsAdded(2)` runs first and the step succeeds.
7. With no seated Exile, nothing is asked.
8. After Task 8, a former Chancellor who accepts has each former Imperial site turned Imperial.

Then run `ShapeEmpireSuite` and `LineageScoringSuite`. Tests that expect Shape Empire's decision to park first must answer the offer first: offering none. Add a `ChronicleWalk` helper `offeringNone` if two suites need it.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.OfferCitizenshipSuite *ChronicleStateOperations* *WalkerOperationWire*"`
Expected: compile failure.

- [ ] **Step 3: Implement the operation**

```scala
/** Adds purple warbands to an Imperial supply that exists (the Throne's
  * Offer Citizenship, Chronicle ruling 22: the step assumes unlimited
  * purple warbands). */
final case class ImperialWarbandsAdded(count: Int) extends PrimitiveOperation:
  require(count > 0, "added Imperial warbands must be positive")
```

`ChronicleStateOperations.imperialWarbandsAdded`: `Left(InvalidEventOrder("no Imperial supply"))` when it is absent; otherwise add `count`. Wire it in `OperationApplication` next to `ImperialWarbandsTaken`. In the codec, its kind is `"imperial-warbands-added"` with a `count` field.

- [ ] **Step 4: Implement the step**

```scala
package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._

/** Offer Citizenship, the Throne's first step (CR p. 37, Chronicle rulings
  * 5, 20-22). The Throne holder picks any seated Exiles; each answers in
  * seat order. An Exile who accepts becomes a Citizen: their board warbands
  * return to their bank, and at each site they rule their warbands become
  * one Imperial warband. No other Citizenship step runs in the Chronicle.
  * The step assumes unlimited purple warbands, adding any it lacks.
  *
  * Lines: "Offered Citizenship to p2 and p3.", "p2 accepted Citizenship."
  * and "p3 declined Citizenship." */
object OfferCitizenship:
  val offerDecisionId: String = "chronicle.throne.offer"
  def answerDecisionId(player: PlayerId): String =
    s"chronicle.throne.citizenship.${player.value}"
  val accept: DecisionOptionRef.Button = DecisionOptionRef.Button("accept")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val offered: NoteKey = notes.key("offered", Vector(
    NotePart.Text("Offered Citizenship to "), NotePart.Arg(0),
    NotePart.Text(".")))
  val accepted: NoteKey = notes.key("accepted", Vector(NotePart.Arg(0),
    NotePart.Text(" accepted Citizenship.")))
  val declined: NoteKey = notes.key("declined-citizenship", Vector(
    NotePart.Arg(0), NotePart.Text(" declined Citizenship.")))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
      exiles(state) match
        case Vector() => Vector.empty
        case candidates => Vector(
          Decide(offerDecisionId, holder, DecisionQuery.ChooseMany(0,
            candidates.size, candidates.map(p =>
              DecisionOption.Player(DecisionOptionRef.Player(p))),
            heading = Some("Offer Citizenship to any Exiles"))),
          Branch((live, pending) => answers(live, chosen(pending)))))))

  /** Seated Exiles, in seat order (ruling 21). */
  private def exiles(state: ReadyGame): Vector[PlayerId] =
    ChronicleTasks.seated(state).filter(p => Imperials.role(state, p)
      .contains(Role.Exile))

  private def chosen(pending: PendingTree): Vector[PlayerId] =
    PowerAnswers.many(pending, offerDecisionId).collect {
      case DecisionOptionRef.Player(p) => p
    }

  private def answers(state: ReadyGame, offeredTo: Vector[PlayerId])
      : Vector[Operation] =
    val ordered = exiles(state).filter(offeredTo.contains)
    Option.when(ordered.nonEmpty)(Note(noteId, _ => Some(offered.sourceless(
      NoteArg.Players(ordered))))).toVector ++ ordered.flatMap(exile => Vector(
      Decide(answerDecisionId(exile), exile, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(decline, "Decline"),
        DecisionOption.Button(accept, "Become a Citizen")),
        heading = Some("The new Chancellor offers you Citizenship"))),
      Branch((_, pending) =>
        if PowerAnswers.one(pending, answerDecisionId(exile)).contains(accept)
        then Vector(BuildOps((live, _) => joins(live, exile), required = true),
          Note(noteId, _ => Some(accepted.sourceless(NoteArg.Player(exile)))))
        else Vector(Note(noteId, _ => Some(declined.sourceless(
          NoteArg.Player(exile)))))))))

  /** `exile` becomes a Citizen. The board empties before the role changes,
    * since a board's warbands follow its owner's role. */
  private def joins(ready: ReadyGame, exile: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    state <- PlayerFacts.player(ready, exile)
  yield
    val own = ForceKind.Exile(state.lineage)
    val current = ready.game.current
    val ruled = current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(_.forces).collect {
        case SiteForces.Occupied(`own`, n) => site -> n })
    val board = Option.when(state.board.warbands > 0)(Move(
      Piece.Warbands(own, state.board.warbands),
      PositionedLocation(Location.PlayArea(exile)),
      PositionedLocation(Location.WarbandBank(own)))).toVector
    val short = ruled.size - PlayerFacts.banked(ready, ForceKind.Imperial)
    val added = Option.when(short > 0)(ImperialWarbandsAdded(short)).toVector
    val swaps = ruled.flatMap((site, n) =>
      val at = PositionedLocation(Location.Site(site))
      Option.when(n > 1)(Kill(Piece.Warbands(own, n - 1), at)).toVector :+
        Replace(Piece.Warbands(own, 1), Piece.Warbands(ForceKind.Imperial, 1),
          at))
    board ++ added ++ Vector(RoleChanged(state.lineage, Role.Citizen)) ++
      swaps
```

Check the signatures of `ChronicleTasks.seated` and `PowerAnswers.many`, the `NoteArg.Players` argument type and the `sourceless` call against `LineageScoring` and `ChronicleTaskAssignment`. The note key `declined-citizenship` avoids clashing with a Throne key already named `declined`, if there is one; grep `declared-keys.txt`.

`ChronicleThroneProcedure`: replace the first step with `ChronicleWindows.step(PowerWindow.ChronicleOfferCitizenship, OfferCitizenship.steps)`, drop the "waits for the Empire phase" comment, and add `OfferCitizenship.noteKeys` to `noteKeys`.

- [ ] **Step 5: Run the tests and see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.chronicle.* *ChronicleStateOperations* *WalkerOperationWire* *DeclaredNoteKeys* *GameLogGolden*"`
Expected: PASS once `chronicle-started` golden logs are regenerated. The only new line should be the offer decision, if the default answer offers no one; read the diff.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/operations/OperationApplication.scala src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala src/main/scala/distantlands/serialization/WalkerOperationCodec.scala src/main/scala/distantlands/gameplay/chronicle/OfferCitizenship.scala src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala <the tests and regenerated snapshots>
git commit -m "feat(chronicle): the Throne offers Citizenship to the Exiles

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Scenario, golden logs, docs and gates

**Files:**
- Modify: `src/test/scala/distantlands/application/RoleChangesScenarioSuite.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/ImperialLogScripts.scala` (or the registry the 3a `scepter-citizen` script uses)
- Create: golden logs `scepter-exiled` and `deal-citizen` (`.actor.log`, `.other.log`)
- Modify: `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md` (rows 110, 120 and 129), `docs/rules/chronicle.md` (Crown and Throne digital notes), `docs/rules/rules-reference.md` if it lists the cards, `docs/ROADMAP.md`, `docs/superpowers/specs/2026-10-29-empire-phase-design.md`

- [ ] **Step 1: Scenario and logs**

`RoleChangesScenarioSuite`: add one command-level game in which:
- the Chancellor offers Citizenship to an Exile in a deal;
- the Exile accepts;
- later the Chancellor beats that Citizen with the Scepter's plan and exiles them.

Assert the projection rows the browser renders: the deal panel's Citizenship toggle, and the plan in the plan picker.

Add log scripts for both halves, `deal-citizen` and `scepter-exiled`. Regenerate with `GAMELOG_GOLDEN=write`. Check that the title-flip, Vision-discard and replacement lines appear from the existing machinery, and that no line names its actor.

- [ ] **Step 2: Docs**

- `ambiguities.md`:
  - Empire rulings R20-R24, as worded in this plan's "Rulings for this slice";
  - Chronicle rulings 20-22. Ruling 20 says plainly that it deviates from CR p. 36.
- `implementation-traceability.md`: rows 110 and 120 drop "remain (Empire slice 3b)" and name the suites. Row 129 drops "Offer Citizenship is an empty window" and names `OfferCitizenshipSuite`.
- `chronicle.md`, Crown Chancellor: add a "Digital:" bullet for ruling 20.
- `chronicle.md`, Throne step 1: add "Digital:" bullets for rulings 21 and 22. Note that the former Chancellor's sites need no special case, because of ruling 20.
- `ROADMAP.md`:
  - delete the "Slice 3b, Citizenship sources" item;
  - under "Phase - Empire", add `- [ ] **Simultaneous Throne answers.** Build the walker's Simultaneous node (negotiation walker design, "Simultaneous decisions") so the Exiles offered Citizenship at the Throne answer at once instead of in seat order.`
- Spec:
  - add "Slice 3b implemented, 2026-11-01" to the status line;
  - in "Sources", say that Bewitch, Ancient Pact and Royal Ambitions take a relic;
  - add the actorless note to "Becoming a Citizen";
  - point the Throne paragraph at Chronicle rulings 20-22;
  - the negotiation bullet says the holder may not name themself.

- [ ] **Step 3: Gates**

Run every gate in Global Constraints, and fix what fails.

- [ ] **Step 4: Commit**

```bash
git add <scenario, scripts, golden logs, docs by name>
git commit -m "docs(empire): record slice 3b, rulings R20-R24 and Chronicle 20-22

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
