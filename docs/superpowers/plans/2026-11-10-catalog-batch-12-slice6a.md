# Catalog Batch 12, Slice 6a: Secret Terms and a Prefixed Deal Walk Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Open secret terms in the Negotiation deal and run the deal walk
under a decision-id prefix, so slice 6b's Gathering can hold several deals
that trade secrets. The Negotiate action keeps secrets closed and keeps its
prefix `negotiation`.

**Architecture:** The deal decision's `Repeat` and the settlement move out
of `NegotiationProcedure` into `NegotiationDealTree`, driven by a
`DealWalk` (prefix, owner, participants, rules, opening line). The fold,
the decline line and the Deed Writer warband questions read
`<prefix>.deal` and `<prefix>.warbands.<site>`. A transfer row gains
`secrets`. `DealRules.secretsOpen` opens them; the bound is the author's
faceup secrets, and settlement moves them faceup with a plain `Move`.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../specs/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (6 and 31), "The deal model" and "Slice 6a: Secret
terms, a prefixed deal walk".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, parameters or values, no
  non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`). `gameplay.powers` may import
  `gameplay.actions`.
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire and journal changes need not keep old journals decodable. Every new
  key (`secrets`, `editableSecrets`, `secretRecipients`) is required.
- Ruling 6: secrets come from the player's own board. A banner's secrets
  are never exchanged.
- Ruling 31: an author may offer up to their faceup secrets, and each
  secret arrives faceup.
- The Negotiate action keeps secrets closed (`DealRules.secretsOpen` is
  false) and keeps the prefix `negotiation`: its tree shape, its lines and
  its park positions do not change.
- The package is `distantlands`. This plan anchors edits by code, not by
  line: slice 5 changes several of the files below.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

## Setup

Slice 5 (`docs/superpowers/plans/2026-11-10-catalog-batch-12-slice5.md`)
must be merged into `0.2.0-alpha` first. This plan moves the settlement
Branch as slice 5 leaves it: the private opening key, the promise lines'
readers and `DealRules.privateDeal`. Check with:

```bash
git grep -n "negotiatedPrivately\|privateDeal" -- src/main/scala/distantlands/gameplay/actions/negotiation
```

It must print matches in `NegotiationProcedure.scala` and
`DealRules.scala`. In a new worktree, symlink the main checkout's
`.tooling` and `node_modules` before running `./sbtw`.

## Plan-time findings

1. **The prefix is not a rule.** The spec's first deal model put `prefix`
   in `DealRules`. `DealRules` says who may put what into a deal; the
   prefix says where the deal's answers live. It is the walk's
   (`DealWalk.prefix`), and the spec now says so.
2. **`DealState` carries its prefix.** `fold` is the one place a deal is
   built, so it stamps the prefix on the state. The warband helpers read
   `deal.prefix` instead of taking a second argument everywhere.
3. **The warband question's id changes** from `deed-writer.warbands.<site>`
   to `negotiation.warbands.<site>` in the Negotiate action. No journal
   need decode the old id. The golden logs print no decision id; Task 1
   proves that by running them unchanged.
4. **Faceup secrets move by a plain `Move`.** `Give` has no source side.
   A `Move` from `PositionedLocation(PlayArea(author), side =
   Some(SecretSide.FaceUp))`, `by = Some(author)`, takes faceup secrets
   only, and a moved secret keeps its side, so it arrives faceup
   (`OperationSecretPlanner`). A banner's secrets sit on the banner
   (`Location.OnCard`), never on the board, so ruling 6 needs no code.
5. **`secrets` goes last** in `NegotiationTransfer`, the command wire's
   `NegotiationTransfer`, `NegotiationTransferInput` and
   `NegotiationTransferProjection`, defaulted to 0, so every positional
   construction keeps compiling. Codecs still require the key.
6. **No card opens secrets in 6a.** The secret line is tested on
   `NegotiationDealTree.transferred`, and the gameplay on
   `NegotiationDeal.snapshot`, `DecisionQueries.accepts` and
   `NegotiationDeal.settle` with `DealRules(secretsOpen = true)`. A golden
   log and a projection of offered secrets wait for slice 6b's Gathering.
7. **The tree keeps its shape.** `NegotiationProcedure.tree` still returns
   `Sequence(choose ++ Vector(Repeat, Branch))`; only where the two
   operations are built moves. Park positions are unchanged.
8. **`CommandProtocolSuite` holds JSON literals** with transfer rows. Each
   needs `"secrets":0`, or `exact` refuses the row before the check the
   test means to reach. No other fixture spells a transfer row
   (`grep -rl '"siteIds"' src/test shared/src/test frontend/src/test`).
9. **One new note key.** `gave-secrets` joins the Negotiation's keys, so
   `src/test/resources/notes/declared-keys.txt` gains one line.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala` | New: `DealWalk`, `NegotiationDealTree` (the deal's `Repeat`, settlement and lines) |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | `tree` walks a `DealWalk`; settlement code moves out; `gaveSecrets` key |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | `NegotiationPrefix`, `dealId`, `DealState.prefix`, `fold(prefix, …)`; secret bounds, substance, affordability, settlement |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala` | `warbandsDecisionId(prefix, site)`, `placed(answered, prefix, site)` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala` | `TermKind.Secret`, `secretsOpen`, `secretRecipients` |
| `src/main/scala/distantlands/model/NegotiationTerms.scala` | `NegotiationTransfer.secrets`; `NegotiationBounds.maxSecrets`, `secretRecipients` |
| `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` | `acceptsTerms` checks secrets |
| `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala` | `secrets` key |
| `shared/src/main/scala/distantlands/protocol/CommandIntents.scala` | wire `NegotiationTransfer.secrets` |
| `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala` | `secrets` key |
| `src/main/scala/distantlands/application/GameIntentMapper.scala` | maps `secrets` |
| `frontend/src/main/scala/distantlands/frontend/GameClient.scala` | `NegotiationTransferInput.secrets` |
| `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` | passes `secrets` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala` | `secrets`, `editableSecrets`, `secretRecipients` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala` | the three keys |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | projects them |
| `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala` | Secrets box, transfer line |
| `docs/architecture/all-exile-negotiation.md`, the spec | docs |

---

### Task 1: The deal walk under a prefix

A pure refactor: no behaviour of the Negotiate action changes.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala`
- Modify: `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`, `NegotiationProcedureSuite.scala`, `NegotiationCitizenshipSuite.scala`, `NegotiationSiteTermsSuite.scala`, `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationDealPrefixSuite.scala` (new)
- Test: `src/test/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTreeSuite.scala` (new)

**Interfaces:**
- Produces:
  - `NegotiationDeal.NegotiationPrefix: String` (`"negotiation"`),
    `NegotiationDeal.dealId(prefix: String): String`
    (`s"$prefix.deal"`); `dealDecisionId` and `negotiatorsDecisionId` keep
    their values.
  - `DealState(prefix: String, participants, terms, accepted, declined)`.
  - `NegotiationDeal.fold(prefix: String, participants: Vector[PlayerId],
    answered: Vector[Answered]): DealState`.
  - `NegotiationSites.warbandsDecisionId(prefix: String, site: SiteId):
    String` (`s"$prefix.warbands.${site.value}"`) and
    `NegotiationSites.placed(answered: Vector[Answered], prefix: String,
    site: SiteId): Option[Int]`.
  - `final case class DealWalk(prefix: String, owner: PlayerId,
    participants: (ReadyGame, PendingTree) => Vector[PlayerId],
    rules: Vector[PlayerId] => DealRules, opened: PendingTree => LogNote)`.
  - `NegotiationDealTree(catalog: ExecutableCatalog, limits:
    AdviserLimits, walk: DealWalk): Vector[Operation]`, and
    `private[negotiation]` `endedNote(prefix: String)(states: NoteStates):
    Option[LogNote]` and `transferred(deal: DealState): Vector[LogNote]`.

- [ ] **Step 1: Write the failing prefix tests**

Create `src/test/scala/distantlands/gameplay/NegotiationDealPrefixSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{NegotiationDeal,
  NegotiationSites}
import distantlands.gameplay.cards.DunesSite
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  DeclineDeal, ProposeTerms}
import distantlands.testkit.Table.{p1, p2}

/** A deal's answers live under its prefix (catalog batch 12, "One
  * decision id per deal"), so one walk can hold several deals. */
class NegotiationDealPrefixSuite extends munit.FunSuite:
  private val dunes = DunesSite.id
  private val gift = NegotiationTerms(Vector(
    NegotiationTransfer(p2, 1, Vector.empty)))

  test("a deal's ids are its prefix's, and the Negotiate action's stay"):
    assertEquals(NegotiationDeal.dealId("gathering.2"), "gathering.2.deal")
    assertEquals(NegotiationDeal.dealDecisionId, "negotiation.deal")
    assertEquals(NegotiationDeal.negotiatorsDecisionId,
      "negotiation.negotiators")
    assertEquals(NegotiationSites.warbandsDecisionId("gathering.2", dunes),
      s"gathering.2.warbands.${dunes.value}")

  test("a deal folds only the answers under its own prefix"):
    val answered = Vector(
      Answered("gathering.1.deal", ProposeTerms(gift), p1),
      Answered("gathering.2.deal", AcceptDeal, p2),
      Answered("gathering.2.deal", DeclineDeal, p2))
    val first = NegotiationDeal.fold("gathering.1", Vector(p1, p2), answered)
    assertEquals(first.prefix, "gathering.1")
    assertEquals(first.terms(p1), gift)
    assertEquals(first.accepted, Set.empty[PlayerId])
    assert(!first.declined)
    assert(NegotiationDeal.fold("gathering.2", Vector(p1, p2), answered)
      .declined)

  test("a placement is read under its own prefix"):
    val answered = Vector(Answered(
      NegotiationSites.warbandsDecisionId("gathering.1", dunes),
      ChooseAmountAnswer(2), p1))
    assertEquals(NegotiationSites.placed(answered, "gathering.1", dunes),
      Some(2))
    assertEquals(NegotiationSites.placed(answered, "gathering.2", dunes),
      None)
```

Create `src/test/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTreeSuite.scala`:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.model._
import distantlands.model.DecisionAnswer.DeclineDeal
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}

/** The deal walk's lines, read under its prefix. */
class NegotiationDealTreeSuite extends munit.FunSuite:
  test("the decline line names who declined the deal under its own " +
      "prefix"):
    val answered = Vector(Answered("gathering.1.deal", DeclineDeal, p3),
      Answered("gathering.2.deal", DeclineDeal, p2))
    val states = NoteStates(Table.start.ready, None, answered)
    assertEquals(NegotiationDealTree.endedNote("gathering.1")(states),
      Some(NegotiationProcedure.ended.sourceless(NoteArg.Player(p3))))
    assertEquals(NegotiationDealTree.endedNote("gathering.2")(states),
      Some(NegotiationProcedure.ended.sourceless(NoteArg.Player(p2))))
    assertEquals(NegotiationDealTree.endedNote("negotiation")(states), None)
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationDealPrefixSuite distantlands.gameplay.actions.negotiation.NegotiationDealTreeSuite"`
Expected: compile failure: `dealId`, `prefix`, `NegotiationDealTree` are
not members.

- [ ] **Step 3: Prefix the deal's ids and its fold**

In `NegotiationDeal.scala`:

- `DealState` gains a first field:

```scala
/** One deal, derived from the answers recorded so far under `prefix`, the
  * deal walk's decision-id prefix ([[DealWalk]]). */
final case class DealState(prefix: String, participants: Vector[PlayerId],
    terms: Map[PlayerId, NegotiationTerms], accepted: Set[PlayerId],
    declined: Boolean):
```

- Replace the two id vals at the top of `object NegotiationDeal` with:

```scala
  /** The Negotiate action's decision-id prefix. */
  val NegotiationPrefix: String = "negotiation"
  val negotiatorsDecisionId: String = s"$NegotiationPrefix.negotiators"

  /** The deal decision of the deal walked under `prefix` (catalog batch
    * 12, "One decision id per deal"). */
  def dealId(prefix: String): String = s"$prefix.deal"
  val dealDecisionId: String = dealId(NegotiationPrefix)
```

- Replace `fold`:

```scala
  /** The deal under `prefix`: only the answers to `<prefix>.deal` count, so
    * one walk may hold several deals. */
  def fold(prefix: String, participants: Vector[PlayerId],
      answered: Vector[Answered]): DealState =
    val id = dealId(prefix)
    answered.filter(_.decisionId == id).foldLeft(DealState(prefix,
      participants, participants.map(_ -> NegotiationTerms()).toMap,
      Set.empty, declined = false)):
      case (deal, Answered(_, ProposeTerms(terms), by)) =>
        deal.copy(terms = deal.terms.updated(by, terms), accepted = Set.empty)
      case (deal, Answered(_, AcceptDeal, by)) =>
        deal.copy(accepted = deal.accepted + by)
      case (deal, Answered(_, DeclineDeal, _)) => deal.copy(declined = true)
      case (deal, _) => deal
```

- Delete `def deal(state, actor, candidates, pending)`: the walk folds
  through `DealWalk` now (Step 5). If a test calls it, fold with
  `NegotiationPrefix` and `participants(...)` there instead.

- [ ] **Step 4: Prefix the warband question**

In `NegotiationSites.scala`, replace `warbandsDecisionId` and `placed`, and
make `most` read the deal's prefix:

```scala
  /** The question that asks `site`'s new ruler how many warbands to place,
    * under the deal walk's `prefix`. */
  def warbandsDecisionId(prefix: String, site: SiteId): String =
    s"$prefix.warbands.${site.value}"

  /** How many warbands the new ruler placed at `site` in the deal under
    * `prefix`, once answered. */
  def placed(answered: Vector[Answered], prefix: String,
      site: SiteId): Option[Int] =
    val id = warbandsDecisionId(prefix, site)
    answered.collectFirst {
      case Answered(answeredId, ChooseAmountAnswer(count), _)
          if answeredId == id => count
    }
```

In `most`, the two calls become `placed(answered, deal.prefix, m.site)`.

- [ ] **Step 5: Move the deal walk into `NegotiationDealTree`**

Create `NegotiationDealTree.scala`. It holds the `Repeat` over the deal
decision and the settlement `Branch` that `NegotiationProcedure.tree`
builds today, and the private helpers only they call: `endedNote`,
`disclosed`, `transferred`, `placing`, `becameNote`, `offered` and
`promisedLines`. Move them as slice 5 left them, then make exactly these
substitutions:

- every `NegotiationDeal.deal(ready, actor, candidates, pending)` becomes
  `deal(walk, ready, pending)`;
- the deal decision's id is `NegotiationDeal.dealId(walk.prefix)`, its
  owner `walk.owner`, its rules `walk.rules(now.participants)`, and its
  co-owners `now.participants.filter(_ != walk.owner)`;
- the opening line of a deal nobody declined is `walk.opened(pending)`;
  the decline line is `endedNote(walk.prefix)`;
- the promise lines take readers when
  `walk.rules(deal.participants).privateDeal` (slice 5 read `privately`);
- in `placing`, `NegotiationSites.warbandsDecisionId(deal.prefix,
  move.site)` and `NegotiationSites.placed(…, deal.prefix, move.site)`;
- the note keys stay in `NegotiationProcedure` and are read as
  `NegotiationProcedure.ended`, `NegotiationProcedure.gaveFavor` and so on.
  Every `Note` keeps `ProcedureNotes.id(ActionRef.Negotiation)`.

The file's head and the two operations:

```scala
package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.{AdviserLimits, ProcedureNotes}
import distantlands.gameplay.roles.Citizenship
import distantlands.model._
import distantlands.model.DecisionAnswer.DeclineDeal

/** How one deal walks (catalog batch 12, slice 6a): the prefix of its
  * decision ids, the player who owns its decision, and, as the answers so
  * far give them, its participants, its rules and the line that opens a
  * deal nobody declined. The Negotiate action walks one under
  * `NegotiationDeal.NegotiationPrefix`; The Gathering walks one per deal
  * (slice 6b). */
final case class DealWalk(prefix: String, owner: PlayerId,
    participants: (ReadyGame, PendingTree) => Vector[PlayerId],
    rules: Vector[PlayerId] => DealRules,
    opened: PendingTree => LogNote)

/** One deal's walk, as the operations a procedure puts in its tree:
  *
  * {{{
  * Repeat(!closed) { Branch -> Decide(<prefix>.deal, co-owned) }
  * Branch(opening line; agreed -> BuildOps(settle), BuildOps(site returns),
  *   notes, per site: Branch -> Decide(<prefix>.warbands.<site>),
  *   BuildOps(place), note; Citizenship, promise notes)
  * }}}
  *
  * Its lines are the Negotiate action's keys ([[NegotiationProcedure]]).
  * In a private deal (`DealRules.privateDeal`) only the participants read
  * the promise lines (ruling 23).
  */
object NegotiationDealTree:
  def apply(catalog: ExecutableCatalog, limits: AdviserLimits,
      walk: DealWalk): Vector[Operation] = Vector(
    Repeat((ready, pending) => !deal(walk, ready, pending).closed,
      Branch((ready, pending) => {
        val now = deal(walk, ready, pending)
        Vector(Decide(NegotiationDeal.dealId(walk.prefix), walk.owner,
          NegotiationDeal.snapshot(catalog, ready, now,
            walk.rules(now.participants), limits),
          coOwners = now.participants.filter(_ != walk.owner)))
      })),
    Branch((ready, pending) => settlement(catalog, walk, ready, pending)))

  private def deal(walk: DealWalk, ready: ReadyGame,
      pending: PendingTree): DealState =
    NegotiationDeal.fold(walk.prefix, walk.participants(ready, pending),
      pending.answered)
```

`settlement(catalog, walk, ready, pending): Vector[Operation]` is the body
of today's settlement Branch, with the substitutions above; its first
lines become:

```scala
  private def settlement(catalog: ExecutableCatalog, walk: DealWalk,
      ready: ReadyGame, pending: PendingTree): Vector[Operation] =
    val deal = this.deal(walk, ready, pending)
    // The opening line: the decline names who ended the deal, and anything
    // else is the walk's own line, whether or not the deal had substance.
    val opening: Vector[Operation] = Vector(
      Note(ProcedureNotes.id(ActionRef.Negotiation),
        if deal.declined then endedNote(walk.prefix)
        else _ => Some(walk.opened(pending))))
```

`endedNote` reads its prefix's decision, and it and `transferred` are
package-visible for `NegotiationDealTreeSuite`:

```scala
  /** Who declined the deal under `prefix`. */
  private[negotiation] def endedNote(prefix: String)(
      states: NoteStates): Option[LogNote] =
    val id = NegotiationDeal.dealId(prefix)
    states.answered.reverse.collectFirst {
      case Answered(`id`, DeclineDeal, by) =>
        NegotiationProcedure.ended.sourceless(NoteArg.Player(by))
    }
```

`transferred` keeps its body, with `private[negotiation]` in place of
`private`.

- [ ] **Step 6: Walk the deal from the procedure**

In `NegotiationProcedure.tree`, keep `choose` and replace the `Repeat` and
the settlement `Branch` with the walk (slice 5's `privately` is `tree`'s
last parameter):

```scala
    Sequence(choose ++ NegotiationDealTree(catalog, limits, DealWalk(
      NegotiationDeal.NegotiationPrefix, actor,
      (ready, pending) => NegotiationDeal.participants(ready, actor,
        candidates, pending),
      participants => facts.rules(participants)
        .copy(privateDeal = privately),
      pending => (if privately then negotiatedPrivately else negotiated)
        .sourceless(NoteArg.Players(NegotiationDeal.chosen(candidates,
          pending))))))
```

Delete the helpers Step 5 moved. Update the object's doc comment: after
the tree, add "The deal's `Repeat` and its settlement are
[[NegotiationDealTree]]'s, walked under the prefix `negotiation`." Remove
the imports the compiler names unused.

- [ ] **Step 7: Update the tests' callers**

- `NegotiationDeal.fold(` in `NegotiationDealSuite`,
  `NegotiationProcedureSuite` and `NegotiationCitizenshipSuite` takes
  `NegotiationDeal.NegotiationPrefix` first:
  `NegotiationDeal.fold(NegotiationDeal.NegotiationPrefix, Vector(p1, p2), …)`.
- `NegotiationSites.warbandsDecisionId(x)` in `NegotiationSiteTermsSuite`
  and `NegotiationLogScripts` becomes
  `NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix, x)`.
  Add `NegotiationDeal` to the files' imports where missing.

Find them all with:

```bash
grep -rn "NegotiationDeal.fold(\|warbandsDecisionId(\|NegotiationDeal.deal(" src/test/scala
```

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationDealPrefixSuite distantlands.gameplay.actions.negotiation.NegotiationDealTreeSuite"`
Expected: PASS.

Run: `./sbtw "testOnly *Negotiation* *RoleChanges* *GameLog* *HornedMask* *WhisperingStone* *Walker*"`
Expected: PASS, with no golden log rewritten (finding 3, finding 7). If a
golden log differs, stop: the refactor changed the Negotiate action.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationSites.scala src/test/scala/distantlands/gameplay/NegotiationDealPrefixSuite.scala src/test/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTreeSuite.scala src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala src/test/scala/distantlands/gameplay/NegotiationProcedureSuite.scala src/test/scala/distantlands/gameplay/NegotiationCitizenshipSuite.scala src/test/scala/distantlands/gameplay/NegotiationSiteTermsSuite.scala src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala
git commit -m "$(cat <<'EOF'
refactor(negotiation): walk a deal under a decision-id prefix

The deal decision's loop and the settlement move into
NegotiationDealTree, driven by a DealWalk. The fold, the decline line
and the Deed Writer warband questions read their prefix's answers, so
one walk can hold several deals (catalog batch 12, slice 6a). The
Negotiate action walks under "negotiation" and keeps its tree.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add any other test file Step 7's grep named.

---

### Task 2: Secret terms on the wire

**Files:**
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala`
- Modify: `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala`
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameClient.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala`, `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`, `src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala`, `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`

**Interfaces:**
- Produces: `model.NegotiationTransfer(recipient, favor, relics, advisers
  = …, sites = …, secrets: Int = 0)`; `protocol.NegotiationTransfer(
  recipientPlayerId, favor, relicIds, advisers = …, siteIds = …, secrets:
  Int = 0)`; `NegotiationTransferInput(…, siteIds = …, secrets: Int = 0)`.
  Journal and command key `"secrets"`, required.

- [ ] **Step 1: Write the failing tests**

In `NegotiationDealSuite`, add:

```scala
  test("a transfer row refuses a negative number of secrets"):
    intercept[IllegalArgumentException](NegotiationTransfer(p2, 0,
      Vector.empty, secrets = -1))
```

In `WalkerDecisionWireSuite`'s "walker choice steps round trip the
negotiation answers", give the transfer row secrets:

```scala
      Vector(NegotiationTransfer(blue, 2, Vector(RelicId("r1")),
        Vector(DenizenId("d1"), VisionId("v2")), Vector(SiteId("site:dunes")),
        secrets = 1)),
```

In `CommandProtocolSuite`, give the example's transfer row secrets:

```scala
      NegotiationTerms(Vector(NegotiationTransfer("blue", 2, Vector("r1"),
        Vector(WorldCard("denizen", "d1")), Vector("site:dunes"),
        secrets = 1)),
```

then add `"secrets":0` after `"siteIds":[…]` in every transfer row of the
suite's JSON literals (finding 8), and add:

```scala
  test("a transfer row with no secrets key is refused"):
    val missing = """{"expectedNextSequence":0,"intent":{"type":"resolveWalker","decisionId":"negotiation.deal","payload":{"kind":"propose-terms","terms":{"transfers":[{"recipientPlayerId":"p2","favor":1,"relicIds":[],"advisers":[],"siteIds":[]}],"disclosures":[],"citizenshipPlayerIds":[],"promises":[],"bindingPromises":[]}}}}"""
    assertEquals(rejected(ActorlessCommandCodec.decode(missing)).path,
      "$.intent.payload.terms.transfers[0]")

  test("a transfer row with negative secrets is refused"):
    val negative = """{"expectedNextSequence":0,"intent":{"type":"resolveWalker","decisionId":"negotiation.deal","payload":{"kind":"propose-terms","terms":{"transfers":[{"recipientPlayerId":"p2","favor":0,"relicIds":[],"advisers":[],"siteIds":[],"secrets":-1}],"disclosures":[],"citizenshipPlayerIds":[],"promises":[],"bindingPromises":[]}}}}"""
    assertEquals(rejected(ActorlessCommandCodec.decode(negative)).path,
      "$.intent.payload.terms.transfers[0].secrets")
```

If `exact` names the missing key in its path
(`…transfers[0].secrets`), assert that path instead: read what it reports
for the advisers case and match it.

In `GameIntentMapperNegotiationSuite`, add:

```scala
  test("secrets in a transfer row map to the engine's row"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, secrets = 2)), Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          secrets = 2))))))))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite"`
Expected: compile failure: `secrets` is not a parameter.

- [ ] **Step 3: The model**

In `NegotiationTerms.scala`, `NegotiationTransfer` gains a last field and
a check:

```scala
final case class NegotiationTransfer(
    recipient: PlayerId,
    favor: Int,
    relics: Vector[RelicId],
    advisers: Vector[WorldCardId] = Vector.empty,
    sites: Vector[SiteId] = Vector.empty,
    /** Faceup secrets from the author's board (catalog batch 12, rulings 6
      * and 31). Only a deal whose rules open them takes any. */
    secrets: Int = 0
):
  require(favor >= 0, "Negotiation favor must be non-negative")
  require(secrets >= 0, "Negotiation secrets must be non-negative")
```

(keep the other `require`s).

- [ ] **Step 4: The journal codec**

In `NegotiationTermsCodec.encode`, after `"siteIds"`:

```scala
      "siteIds" -> ujson.Arr.from(transfer.sites.map(site =>
        ujson.Str(site.value))),
      "secrets" -> transfer.secrets))),
```

In `decode`, read it as favor is read:

```scala
      transfers <- traverse(value("transfers").arr.toVector) { row =>
        val raw = row("favor").num
        val rawSecrets = row("secrets").num
        for
          favor <- Either.cond(raw.isValidInt, raw.toInt, InvalidValue(
            s"$path.transfers.favor", s"favor '$raw' is not an integer"))
          secrets <- Either.cond(rawSecrets.isValidInt, rawSecrets.toInt,
            InvalidValue(s"$path.transfers.secrets",
              s"secrets '$rawSecrets' is not an integer"))
          advisers <- traverse(row("advisers").arr.toVector)(card =>
            decodeWorldCardId(card, s"$path.transfers.advisers"))
        yield NegotiationTransfer(PlayerId(row("recipientPlayerId").str),
          favor, row("relicIds").arr.toVector.map(v => RelicId(v.str)),
          advisers, row("siteIds").arr.toVector.map(v => SiteId(v.str)),
          secrets)
      }
```

- [ ] **Step 5: The command wire**

In `CommandIntents.scala`:

```scala
final case class NegotiationTransfer(
    recipientPlayerId: String, favor: Int, relicIds: Vector[String],
    advisers: Vector[WorldCard] = Vector.empty,
    siteIds: Vector[String] = Vector.empty,
    secrets: Int = 0)
```

In `CommandNestedCodecs.encodeNegotiation`, the row gains
`"secrets" -> row.secrets` after `"siteIds"`. In `decodeTransfer`:

```scala
  private def decodeTransfer(value: ujson.Value, path: String) = obj(value, path).flatMap { row => for
    _ <- exact(row, Set("recipientPlayerId", "favor", "relicIds", "advisers",
      "siteIds", "secrets"), path)
    recipient <- string(row, "recipientPlayerId", path)
    favor <- field(row, "favor", path).flatMap(integer(_, s"$path.favor"))
    secrets <- field(row, "secrets", path).flatMap(integer(_, s"$path.secrets"))
    …
  yield NegotiationTransfer(recipient, favor, relics, advisers, sites, secrets) }
```

(the lines between stay as they are; `integer` refuses a negative value,
as the aid offer test shows).

- [ ] **Step 6: The mapper and the client**

In `GameIntentMapper.negotiation`, the row passes `v.secrets` last:

```scala
        rows.map((v, advisers) => distantlands.model.NegotiationTransfer(
          PlayerId(v.recipientPlayerId), v.favor,
          v.relicIds.map(RelicId.apply), advisers,
          v.siteIds.map(SiteId.apply), v.secrets)), ds,
```

In `GameClient.scala`, `NegotiationTransferInput` gains
`secrets: Int = 0` last. In `ServerUiSupport.protocolNegotiationTerms`:

```scala
      value.transfers.map(v => NegotiationTransfer(
        v.recipientPlayerId, v.favor, v.relicIds, v.advisers, v.siteIds,
        v.secrets)),
```

- [ ] **Step 7: Run the tests**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.NegotiationDealSuite distantlands.protocol.CommandProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/NegotiationTerms.scala src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala shared/src/main/scala/distantlands/protocol/CommandIntents.scala shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala src/main/scala/distantlands/application/GameIntentMapper.scala frontend/src/main/scala/distantlands/frontend/GameClient.scala frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): a transfer row carries secrets

The model, the journal, the command wire, the intent mapper and the
client input gain a required secrets amount (catalog batch 12, slice
6a). No deal opens secrets yet.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Secret terms in the deal

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala`
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala`
- Modify: `src/test/resources/notes/declared-keys.txt` (rewritten by the suite)
- Test: `src/test/scala/distantlands/gameplay/NegotiationSecretTermsSuite.scala` (new), `NegotiationDealSuite.scala`, `actions/negotiation/NegotiationDealTreeSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationDeal.fold(prefix, …)`,
  `NegotiationDealTree.transferred`; Task 2's `NegotiationTransfer.secrets`.
- Produces: `TermKind.Secret`; `DealRules.secretsOpen: Boolean = false`
  (last field) and `DealRules.secretRecipients(author, participants):
  Vector[PlayerId]`; `NegotiationBounds.maxSecrets: Int = 0` and
  `NegotiationBounds.secretRecipients: Vector[PlayerId] = Vector.empty`
  (last fields); `NegotiationProcedure.gaveSecrets: NoteKey`.

- [ ] **Step 1: Write the failing tests**

In `NegotiationDealSuite`, beside "a site alone is substance":

```scala
  test("a secret alone is substance"):
    val deal = fold(said(p1, ProposeTerms(NegotiationTerms(Vector(
      NegotiationTransfer(p2, 0, Vector.empty, secrets = 1))))))
    assert(deal.hasSubstance)
```

In `NegotiationDealTreeSuite`, add (with `ProposeTerms` imported beside
`DeclineDeal`, and `p1` beside `p2, p3`):

```scala
  test("a row's secrets line follows its favor line and comes before " +
      "its relics"):
    val relic = RelicId("r1")
    val deal = NegotiationDeal.fold(NegotiationDeal.NegotiationPrefix,
      Vector(p1, p2), Vector(Answered(NegotiationDeal.dealDecisionId,
        ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(p2, 1,
          Vector(relic), secrets = 2)))), p1)))
    assertEquals(NegotiationDealTree.transferred(deal), Vector(
      NegotiationProcedure.gaveFavor.sourceless(NoteArg.Player(p1),
        NoteArg.Number(1), NoteArg.Player(p2)),
      NegotiationProcedure.gaveSecrets.sourceless(NoteArg.Player(p1),
        NoteArg.Number(2), NoteArg.Player(p2)),
      NegotiationProcedure.gaveCard.sourceless(NoteArg.Player(p1),
        NoteArg.Card(relic), NoteArg.Player(p2))))
```

Create `src/test/scala/distantlands/gameplay/NegotiationSecretTermsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{DealRules, NegotiationDeal}
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.operations.OperationExecutor
import distantlands.gameplay.powers.AdviserLimit
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.DecisionQueries
import distantlands.model._
import distantlands.model.DecisionAnswer.ProposeTerms
import distantlands.model.OathState.Ready
import distantlands.model.OathViolation.InsufficientSecrets
import distantlands.testkit.{ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2}

/** Secret terms (catalog batch 12, rulings 6 and 31): closed in the
  * Negotiate action, bounded by the author's faceup secrets where a deal's
  * rules open them, and moved faceup at settlement. The Gathering opens
  * them (slice 6b); until then these tests open them through `DealRules`.
  */
class NegotiationSecretTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id

  /** p1 and p2 at Broken Peaks; p1 holds `faceUp` faceup secrets and 1
    * facedown, p2 none. */
  private def withSecrets(faceUp: Int = 2): Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site)
    .secrets(p1, faceUp = faceUp, faceDown = 1).secrets(p2, faceUp = 0)

  private def give(secrets: Int): NegotiationTerms = NegotiationTerms(
    Vector(NegotiationTransfer(p2, 0, Vector.empty, secrets = secrets)))

  /** p1's deal with p2, folded from `answered`. */
  private def dealOf(answered: Answered*): DealState =
    NegotiationDeal.fold(NegotiationDeal.NegotiationPrefix, Vector(p1, p2),
      answered.toVector)

  private def query(table: Table, open: Boolean): DecisionQuery.Negotiate =
    NegotiationDeal.snapshot(catalog, table.ready, dealOf(),
      DealRules(secretsOpen = open), AdviserLimit)

  private def offered(query: DecisionQuery.Negotiate, secrets: Int) =
    DecisionQueries.accepts(NegotiationDeal.dealDecisionId, query,
      ProposeTerms(give(secrets)), p1)

  test("the Negotiate action keeps secrets closed"):
    val started = rules.startWalker(Ready(withSecrets().ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    val negotiate = ParkedNode.of(started.state, catalog) match
        case Right(Some(ParkedNode.Decision(_, decide, _))) =>
          decide.query match
            case negotiate: DecisionQuery.Negotiate => negotiate
            case other => fail(s"not a deal: $other")
        case other => fail(s"not parked on a decision: $other")
    assertEquals(negotiate.bounds(p1).maxSecrets, 0)
    assertEquals(negotiate.bounds(p1).secretRecipients,
      Vector.empty[PlayerId])
    assert(offered(negotiate, 1).isLeft)

  test("with secrets open, an author may give up to their faceup " +
      "secrets"):
    val open = query(withSecrets(), open = true)
    assertEquals(open.bounds(p1).maxSecrets, 2)
    assertEquals(open.bounds(p1).secretRecipients, Vector(p2))
    assertEquals(open.bounds(p2).maxSecrets, 0)
    assert(offered(open, 2).isRight)
    assert(offered(open, 3).isLeft)
    assert(offered(query(withSecrets(), open = false), 1).isLeft)

  test("settling moves faceup secrets, faceup, and leaves the facedown " +
      "one"):
    val ready = withSecrets().ready
    val ops = NegotiationDeal.settle(ready, dealOf(Answered(
      NegotiationDeal.dealDecisionId, ProposeTerms(give(2)), p1)))
      .getOrElse(fail("the settlement must hold"))
    val executor = new OperationExecutor()
    val after = ops.foldLeft(ready)((now, op) => executor.execute(now, op)
      .getOrElse(fail(s"$op must apply")))
    val (giver, recipient) = (after.game.current.player(p1).get.board,
      after.game.current.player(p2).get.board)
    assertEquals((giver.faceUpSecrets, giver.faceDownSecrets), (0, 1))
    assertEquals((recipient.faceUpSecrets, recipient.faceDownSecrets),
      (2, 0))

  test("settling is refused once the faceup secrets are gone"):
    val refused = NegotiationDeal.settle(withSecrets(faceUp = 1).ready,
      dealOf(Answered(NegotiationDeal.dealDecisionId, ProposeTerms(give(2)),
        p1)))
    assertEquals(refused, Left(InsufficientSecrets(2, 1)))
```

The imports are the ones `NegotiationAdviserTermsSuite` and
`NegotiationDealSuite` use.

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationSecretTermsSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.actions.negotiation.NegotiationDealTreeSuite"`
Expected: compile failure: `secretsOpen`, `maxSecrets` and `gaveSecrets`
are not members.

- [ ] **Step 3: The rules and the bounds**

In `DealRules.scala`, `TermKind` gains `Secret`, and its doc comment loses
"Secrets join with slice 6.":

```scala
enum TermKind:
  case Favor, Relic, Secret, Adviser, Site
```

`DealRules` gains `secretsOpen: Boolean = false` as its last field; its
doc comment adds "`secretsOpen` lets every pair trade secrets (The
Gathering, ruling 5)." `kinds` adds it, and a recipients reader joins the
other two:

```scala
      Option.when(secretsOpen)(TermKind.Secret) ++
```

```scala
  /** The participants `author` may give secrets, in participant order. */
  def secretRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Secret))
```

In `NegotiationTerms.scala`, `NegotiationBounds` gains two last fields
and a check:

```scala
    siteRecipients: Vector[PlayerId] = Vector.empty,
    /** The faceup secrets the author may give, offered only to
      * `secretRecipients` (catalog batch 12, rulings 6 and 31). */
    maxSecrets: Int = 0,
    secretRecipients: Vector[PlayerId] = Vector.empty
):
  require(maxFavor >= 0, "negotiation favor bound must be non-negative")
  require(maxSecrets >= 0, "negotiation secret bound must be non-negative")
```

In `NegotiationDeal.bounds`, compute and pass them:

```scala
    val secretRecipients = rules.secretRecipients(author, participants)
```

```scala
      siteRecipients = siteRecipients,
      maxSecrets = if secretRecipients.isEmpty then 0
        else player.board.faceUpSecrets,
      secretRecipients = secretRecipients)
```

- [ ] **Step 4: The answer check**

In `DecisionQueries.acceptsTerms`, after the favor check:

```scala
          _ <- require(terms.transfers.map(_.secrets.toLong).sum <=
            own.maxSecrets.toLong && terms.transfers.forall(transfer =>
              transfer.secrets == 0 ||
                own.secretRecipients.contains(transfer.recipient)),
            decisionId, "offers secrets it cannot")
```

- [ ] **Step 5: Substance, affordability and settlement**

In `DealState.hasSubstance`, the row test gains `x.secrets > 0`:

```scala
    t.transfers.exists(x => x.favor > 0 || x.secrets > 0 ||
      x.relics.nonEmpty || x.advisers.nonEmpty || x.sites.nonEmpty) ||
```

In `affordable`, after the favor check:

```scala
    val secrets = terms.transfers.map(_.secrets).sum
```

```scala
      _ <- Either.cond(secrets <= player.board.faceUpSecrets, (),
        OathViolation.InsufficientSecrets(secrets,
          player.board.faceUpSecrets))
```

In `transfers`, between the favor and the relics:

```scala
        // Faceup secrets only, which keep their side (rulings 6 and 31):
        // `Give` names no side, so this is the move a `Give` wraps.
        val secrets = Option.when(transfer.secrets > 0)(Move(
          Piece.Secrets(transfer.secrets),
          PositionedLocation(Location.PlayArea(author),
            side = Some(SecretSide.FaceUp)),
          PositionedLocation(Location.PlayArea(transfer.recipient)),
          by = Some(author)): CoreOperation).toVector
```

and the row's result becomes `favor ++ secrets ++ relics ++ advisers`.
`settle`'s doc comment reads "favor, secrets, relics, then advisers".

- [ ] **Step 6: The secrets line**

In `NegotiationProcedure`, after `gaveFavor`:

```scala
  /** "p1 gave 2 secrets to p2" (catalog batch 12, ruling 31). */
  val gaveSecrets: NoteKey = notes.key("gave-secrets", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "secret", "secrets"),
    NotePart.Text(" to "), NotePart.Arg(2)))
```

In `NegotiationDealTree.transferred`, after the favor line:

```scala
          Option.when(transfer.secrets > 0)(
            NegotiationProcedure.gaveSecrets.sourceless(
              NoteArg.Player(author), NoteArg.Number(transfer.secrets),
              NoteArg.Player(transfer.recipient))).toVector ++
```

Its doc comment reads "each author's favor, then secrets, then relics,
then advisers, then sites".

- [ ] **Step 7: Run the tests and rewrite the note snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.NegotiationSecretTermsSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.actions.negotiation.NegotiationDealTreeSuite distantlands.gameplay.walker.DecisionQuerySuite"`
Expected: PASS.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Then: `git diff src/test/resources/notes/declared-keys.txt`
Expected: exactly one added line, the Negotiation's `gave-secrets =
{0} gave {1} {1?secret|secrets} to {2}`.

Run: `./sbtw "testOnly *Negotiation* *GameLog* *DeclaredNoteKeys*"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/negotiation/DealRules.scala src/main/scala/distantlands/model/NegotiationTerms.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/NegotiationSecretTermsSuite.scala src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala src/test/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTreeSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): secret terms where a deal's rules open them

DealRules.secretsOpen lets every pair trade secrets. An author may give
up to their faceup secrets; the answer check, the substance check and
the settlement's affordability check read them, and settlement moves
them faceup after the favor, with a line (catalog batch 12, rulings 6
and 31). The Negotiate action keeps them closed.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Secret terms in the projection and the panel

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/NegotiationDealProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`, `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`, `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`

**Interfaces:**
- Consumes: Task 2's `NegotiationTransferInput.secrets`; Task 3's
  `NegotiationBounds.maxSecrets` and `secretRecipients`.
- Produces: `NegotiationTransferProjection.secrets: Int = 0` (last);
  `NegotiationEditingProjection.editableSecrets: Int = 0` and
  `secretRecipients: Vector[String] = Vector.empty` (last two). Codec keys
  `secrets`, `editableSecrets`, `secretRecipients`, required.

- [ ] **Step 1: Write the failing tests**

In `ProjectionProtocolSuite`'s "a negotiate query and a waiting deal
round trip with and without editing", give the fixture's transfer row
`secrets = 1` and its editing `editableSecrets = 2, secretRecipients =
Vector("p2")` (named arguments).

In `NegotiationDealProjectionSuite`, add:

```scala
  test("the Negotiate action's editor offers no secrets"):
    val editing = deal(view(parkedDeal(gathered.secrets(p1, faceUp = 2),
      Vector(p2)), p1)).editing.getOrElse(fail("p1 edits the deal"))
    assertEquals(editing.editableSecrets, 0)
    assertEquals(editing.secretRecipients, Vector.empty[String])
```

In `NegotiationDealPanelSuite`, add:

```scala
  test("secrets are offered only to the recipients the rules allow, and " +
      "Save sends the amount"):
    val deal = threeWay(NegotiationEditingState(3, Vector.empty,
      Vector.empty, Vector.empty, canAccept = false, editableSecrets = 2,
      secretRecipients = Vector("blue")))
    val ui = new RecordingControls()
    val panel = render(owner(deal), ui)
    assert(Dom.all(panel, "[aria-label='Secrets offered to yellow']")
      .isEmpty)
    val secrets = input(panel, "[aria-label='Secrets offered to blue']")
    assertEquals(secrets.max, "2")
    secrets.value = "2"
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(
          terms)) => terms }
    assertEquals(sent.flatMap(_.transfers).map(t =>
      t.recipientPlayerId -> t.secrets).toMap, Map("blue" -> 2,
      "yellow" -> 0))

  test("a transfer names the secrets it gives"):
    val base = dealWhereBlueHasAccepted()
    val panel = render(spectator(base.copy(transfers = Vector(
      NegotiationTransferState(authorPlayerId = "blue",
        recipientPlayerId = "red", favor = 1, relicCount = 0,
        relics = Vector.empty, secrets = 2)))), new RecordingControls())
    assertEquals(Dom.one(panel, ".negotiation-transfer").textContent,
      "blue gives red: 1 favor, 2 secrets")
```

`threeWay` builds the red, blue and yellow deal the site test uses. If
the Save button sends no row for a recipient with nothing offered, assert
`Map("blue" -> 2)` only: read how the favor rows behave first and match
them.

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: compile failure: `secrets`, `editableSecrets` and
`secretRecipients` are not members.

- [ ] **Step 3: The DTOs and their codec**

In `ActionProjectionDtos.scala`, `NegotiationTransferProjection` gains
`secrets: Int = 0` after `sites`, and `NegotiationEditingProjection`
gains, after `shortOfWarbands`:

```scala
    /** The faceup secrets the viewer may give, and to whom (catalog batch
      * 12, ruling 31). */
    editableSecrets: Int = 0,
    secretRecipients: Vector[String] = Vector.empty
```

In `ActionProjectionCodec.scala`:

- `encodeTransfer` adds `"secrets" -> row.secrets`; `decodeTransfer` adds
  `"secrets"` to its `exact` set, reads `secrets <- int(row, "secrets",
  child)` and passes it last.
- `encodeDeal`'s editing object adds
  `"editableSecrets" -> editing.editableSecrets` and
  `"secretRecipients" -> encoded(editing.secretRecipients)(ujson.Str(_))`;
  `decodeDeal` adds both keys to the editing `exact` set, reads
  `secrets <- int(row, "editableSecrets", child)` and
  `secretRecipients <- strings(row, "secretRecipients", child)`, and
  passes them last.

- [ ] **Step 4: The projector**

In `NegotiationDealProjector.project`, the transfer passes
`secrets = transfer.secrets`. In `editing`, after `shortOfWarbands`:

```scala
      shortOfWarbands = query.shortOfWarbands.map(_.value),
      editableSecrets = own.maxSecrets,
      secretRecipients = own.secretRecipients.map(_.value))
```

Secrets are counted pieces on the table, so every viewer of a public deal
sees the amount, as favor.

- [ ] **Step 5: The panel**

In `NegotiationDealPanel.scala`:

- `transferLine` names secrets after favor:

```scala
      Option.when(t.favor > 0)(textNode(s"${t.favor} favor")).toVector ++
        Option.when(t.secrets > 0)(textNode(s"${t.secrets} " +
          s"${if t.secrets == 1 then "secret" else "secrets"}")) ++
        t.relics.map(cardLink) ++
```

  and its doc comment reads "favor, secrets, each relic …".
- `favorItem(control)` becomes `amountItem(caption: String, control)`,
  whose span reads `caption`; the favor row calls
  `amountItem("Favor", favor)`. Its doc comment reads "An amount row
  (favor, secrets): …".
- In `editor`, keep a buffer beside `favors`:

```scala
    val secretBoxes = scala.collection.mutable.Map.empty[String,
      dom.html.Input]
```

  and, right after the favor row of each recipient:

```scala
      if editing.secretRecipients.contains(recipient) then
        val secrets = input("number")
        secrets.min = "0"; secrets.max = editing.editableSecrets.toString
        secrets.value = deal.transfers.find(t => t.authorPlayerId == me &&
          t.recipientPlayerId == recipient).map(_.secrets).getOrElse(0)
          .toString
        secrets.setAttribute("aria-label", s"Secrets offered to $recipient")
        panel.appendChild(amountItem("Secrets", secrets))
        secretBoxes += recipient -> secrets
```

- Save's `NegotiationTransferInput` passes the amount last:

```scala
            site }.toVector,
          secretBoxes.get(recipient).flatMap(_.value.toIntOption)
            .getOrElse(0))
```

- [ ] **Step 6: Run the tests**

Run: `./sbtw "testOnly distantlands.application.NegotiationDealProjectionSuite distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite distantlands.frontend.ParkedDecisionSuite"`
Expected: PASS, the existing "every term the owner edits is labelled and
on its own row" included (no secrets box without `secretRecipients`).

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala src/main/scala/distantlands/application/NegotiationDealProjector.scala frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): offer and show secret terms in the panel

The projection carries each row's secrets and the viewer's faceup bound
and recipients. The panel offers a Secrets box to the recipients the
rules allow and names given secrets in the transfer line (catalog batch
12, slice 6a).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Docs and verification

**Files:**
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md`

- [ ] **Step 1: The architecture doc**

In `docs/architecture/all-exile-negotiation.md`:

- Under "Procedure", after the tree, add: "The deal's `Repeat` and its
  settlement are `NegotiationDealTree`'s, driven by a `DealWalk`: the
  prefix of the deal's decision ids, the owner of its decision, its
  participants and rules, and its opening line. The Negotiate action walks
  one under the prefix `negotiation`. The fold, the decline line and the
  Deed Writer warband questions read `<prefix>.deal` and
  `<prefix>.warbands.<site>`, so a walk may hold several deals."
- In the sites paragraph, `deed-writer.warbands.<site>` becomes
  `<prefix>.warbands.<site>`.
- After the sites paragraph, add: "A transfer row may hold secrets where
  `DealRules.secretsOpen` allows them. The Negotiate action never opens
  them; The Gathering does (catalog batch 12, slice 6b). An author may
  give up to their faceup secrets, and settlement moves them faceup from
  board to board after the favor (`Move` with a faceup source side). A
  banner's secrets are never on the board, so they never move."
- In "Settlement and disclosure", the transfers read "the favor, secret
  and relic transfers"; the refusals add `InsufficientSecrets`.
- In the substance list under "The deal is a fold over the answers", add
  "secrets" after "favor".
- In "Deferred", "secrets and banner transfers" becomes "banner
  transfers"; keep the rest of the sentence as slice 5 left it.

- [ ] **Step 2: The spec's status**

In the spec's header, the status reads "Slices 1 to 5 and 6a landed."

- [ ] **Step 3: Full verification**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92%.

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: no violations.

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/all-exile-negotiation.md docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 12 slice 6a, secret terms

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage.** Slice 6a's bullets: secret terms on every wire (Task
  2), `TermKind.Secret`, `secretsOpen`, the bound, the answer, substance
  and affordability checks, the faceup move and its line (Task 3), the
  panel (Task 4), the prefixed walk (Task 1). Its test list: substance
  (Task 3), closed in the Negotiate action (Tasks 3, 4), the faceup bound
  and the refusals (Task 3), the faceup move and the refused settlement
  (Task 3), the line order (Task 3), the prefixed fold, decline line and
  warband answers (Task 1), codec round trips (Tasks 2, 4), the panel's
  box and line (Task 4).
- **Deferred to slice 6b.** A golden log line for given secrets and a
  projection of offered secrets need a deal that opens them (finding 6).
- **Names.** `NegotiationPrefix`, `dealId`, `DealState.prefix`,
  `fold(prefix, participants, answered)`, `warbandsDecisionId(prefix,
  site)`, `placed(answered, prefix, site)`, `DealWalk`,
  `NegotiationDealTree`, `endedNote(prefix)`, `transferred`,
  `TermKind.Secret`, `secretsOpen`, `secretRecipients`, `maxSecrets`,
  `gaveSecrets`, `editableSecrets`, and the `secrets` field on each row
  type are used with one spelling throughout.
