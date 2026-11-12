# Catalog Batch 12, Slice 6b: The Gathering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement The Gathering (card 27): its When Played power asks
each player elsewhere, in the next round's order, whether to put their
pawn on its site, then loops one open question that lets every player
there open a Negotiation deal, pass, or take back a pass, until all have
passed.

**Architecture:** Three new answers (`OpenDeal`, `PassGathering`,
`UnpassGathering`) and a new `DecisionQuery.Gathering` travel from the
model to the panel. `DecisionQuery.Negotiate` gains the deal's
bystanders, who may only pass. The power is a `WhenPlayedPower` whose
effect is one pawn question per player, a "Gathered" line, and a `Repeat`
(`GatheringDeals`) over the open question; a Negotiate answer walks one
slice 6a `DealWalk` under `gathering.<n>`. `PowerCtx.useRestrictions`
hands the power the walk's power-use restrictions, so the reach and each
deal's rules are read live and nothing is stored.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../../specs/2026-11/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (1 to 6, 31 and 32), "The deal model" and "Slice 6b:
A deal loop inside card play".

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
  key is required.
- Ruling 1: the pawns are asked in the next round's order, from the
  Chancellor, else the first player. The actor is asked too when their
  pawn is elsewhere. Pawns stay where they went.
- Ruling 2: a pawn's move is not Travel: a plain `Move`, no Supply, no
  travel window.
- Ruling 3: one deal is open at a time; the first command to arrive wins.
- Ruling 4: a player in no deal may pass and take back a pass. A passed
  player may be named in a deal. The Gathering ends once every player at
  the site has passed and no deal is open.
- Ruling 5: each deal is a full Negotiation: favor, secrets, relics and
  advisers between any pair, disclosures, the Citizenship offer,
  non-binding promises, binding promises under The Tribunal, site terms
  under Deed Writer, under the power-use blockers. The opener may name a
  player elsewhere whom Traveling Negotiator, The Tribunal or Bandit
  Tribunal lets them reach. Only players at the site open, pass and
  unpass.
- Ruling 32: opening a deal clears the opener's pass only.
- The Negotiate action and Whispering Stone do not change: their tree,
  lines and park positions stay, and no golden log but the new one may
  change.
- The package is `distantlands`. This plan anchors edits by code, not by
  line.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

## Setup

Slice 6a must be merged into `0.2.0-alpha` (it is, as `6aca92a8c`). Check
with:

```bash
git grep -n "final case class DealWalk\|secretsOpen" -- src/main/scala/distantlands/gameplay/actions/negotiation
```

It must print matches in `NegotiationDealTree.scala` and `DealRules.scala`.
Work in a worktree off `0.2.0-alpha`, and symlink the main checkout's
`.tooling` and `node_modules` into it before running `./sbtw`.

## Plan-time findings

1. **A When Played power had no power-use restrictions.** The Negotiate
   action reads them because the registry passes `powers.useRestrictions`
   to its build. A When Played power's `PowerCtx` had none, and The
   Gathering judges Traveling Negotiator, The Tribunal and Deed Writer
   under them (rulings 5, 24 to 27). `PowerCtx` gains
   `useRestrictions`, filled wherever the walker builds a context from its
   `WalkerPowers`. Reading `WalkerPowerCatalog.useRestrictions` directly
   would ignore the powers a test registers.
2. **Nothing needs storing for a rebuild.** The Negotiate action stores
   start facts because its rebuild has no powers and because a settled
   deal changes what they read. Here every command re-folds the window
   with the walk's powers, so the restrictions are always at hand; no
   state changes while the open question or a deal is parked (answers
   only); and after a deal settles its walk reads no rule but
   `privateDeal`, constant false. Each deal's rules are read from the
   state at the point the walk reaches its `Branch`, which is the state
   when it opened.
3. **The open question keeps one id**, `gathering.open`, not the spec's
   first `gathering.<n>.open`. A decision id computed from the answers
   would change once the answer it collects is recorded, and the walk
   resumes by id. The deal prefix `gathering.<n>` is computed in the deal
   `Branch`, after the answer, counting the `OpenDeal` answers: stable on
   every resume inside the deal, since no open question is asked during
   one. The spec now says so.
4. **The pass fold reads every Gathering answer.** Its decisions are
   `gathering.open` and `gathering.<n>.deal` (and `gathering.<n>.warbands.*`,
   which carry amounts). A fold over every answer whose id starts with
   `gathering.` adds a `PassGathering`, and removes an `UnpassGathering`
   or an `OpenDeal` (ruling 32). The deal fold ignores both, as it ignores
   every answer but its own three.
5. **New answers, not reused ones.** `ChooseOneAnswer` buttons could carry
   Pass and Unpass, and a `ChooseManyAnswer` the players, but the deal
   query would then take a choose-one from a bystander, and the open
   question's legality depends on who answers, as a deal's does. Three
   `DecisionAnswer` cases make each query's answers its own. They write no
   "Chose" line: `DetailLines` names only choose-one and choose-many
   answers.
6. **The open question's owner** is the actor when at the site, else the
   first player there in the Gathering's order; the others co-own it. The
   deal's owner is its opener; its co-owners are the other participants
   and the players at the site outside the deal (`DealWalk.bystanders`).
7. **Opening lines.** A deal the actor opens reuses `negotiated`
   ("Negotiated with p2", no full stop, as the Negotiate action writes
   it). One another player opens writes the new Negotiation key
   `negotiated-by` ("p2 negotiated with p1"). Both are Negotiation keys,
   since the deal walk's notes are written under the Negotiation's
   procedure id.
8. **The Gathered line is sourceless**, like the deal lines that follow:
   "Gathered at Ancient City: p1 and p2." or "Nobody gathered at Ancient
   City.". A sourced line would read "The Gathering: Gathered at …", and
   the card-play line before it already names the card. The pawn question
   is a narrated decision: the Gathered line names who came.
9. **The site is the hook's.** A site-only card's `CardPlayedFaceup`
   carries `RuleSourceRef.SiteCard(site, card)`; the effect reads the site
   there (`WhenPlayedHarness.hookAt` builds the same hook).
10. **The test kit answers a park only as its primary owner**, and cannot
    stop before a decision it has no answer for. A Gathering reached by a
    real Search needs both: `Situation.parkedAt(decisionId)` answers every
    park until the walk parks at `decisionId` (Task 5). The When Played
    harness gains `walkAnsweringAs` and `parkAfterAs`, which name the
    player answering each park (Task 4).
11. **Projection order.** The projector's Gathering case and the deal's
    bystander fields land with the query (Task 2), so the frontend
    compiles at every commit; they are tested through a real Gathering in
    Task 5.
12. **Two new note keys and one Negotiation key** join
    `src/test/resources/notes/declared-keys.txt`: `denizen.the-gathering
    gathered`, `denizen.the-gathering nobody` and `procedure.negotiation
    negotiated-by`.
13. **The card file empties.** `HoldingNomadDenizens.scala` holds only
    `TheGatheringCard`, so it is deleted; `NewFoundations` already imports
    `distantlands.gameplay.powers.whenplayed._`.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/Decisions.scala` | `OpenDeal`, `PassGathering`, `UnpassGathering`; `DecisionQuery.Gathering`; `Negotiate.bystanders`, `passed` |
| `src/main/scala/distantlands/serialization/DecisionAnswerCodec.scala` | three journal tags |
| `shared/src/main/scala/distantlands/protocol/CommandIntents.scala` | three answer wires |
| `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala` | their codec |
| `src/main/scala/distantlands/application/GameIntentMapper.scala` | maps them |
| `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` | the Gathering query, the deal's bystanders |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala` | `DecisionQueryProjection.Gathering`; deal `bystanderPlayerIds`, `passedPlayerIds` |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala` | form `gathering`, two deal keys |
| `src/main/scala/distantlands/application/WalkerDecisionProjector.scala` | projects the Gathering query |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | projects bystanders and passes |
| `frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala` | `Surface.Gathering`, `Surface.Negotiate.passing` |
| `frontend/src/main/scala/distantlands/frontend/GatheringPanel.scala` | new panel |
| `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala` | a bystander's Pass button |
| `src/main/scala/distantlands/gameplay/TurnOrder.scala` | `nextRoundOrder` |
| `src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala` | new: card, power, pawns, Gathered line |
| `src/main/scala/distantlands/gameplay/powers/whenplayed/GatheringDeals.scala` | new: the loop, passes, deal walks |
| `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala` | registers the power |
| `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala` | deleted |
| `src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala` | `PowerCtx.useRestrictions` |
| `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala`, `ModifierChoice.scala`, `ModifierOffers.scala` | fill it |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala` | `DealWalk.bystanders`, `passed` |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | `negotiatedBy` |
| `src/test/scala/distantlands/testkit/Situation.scala`, `SituationDriver.scala` | `parkedAt` |
| `src/test/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedHarness.scala` | `powersBeside`, `walkAnsweringAs`, `parkAfterAs` |
| `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt` | snapshots |
| `docs/architecture/all-exile-negotiation.md`, `docs/ROADMAP.md`, the spec | docs |

---

### Task 1: The Gathering's answers, journal to wire

**Files:**
- Modify: `src/main/scala/distantlands/model/Decisions.scala`
- Modify: `src/main/scala/distantlands/serialization/DecisionAnswerCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala`
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala`

**Interfaces:**
- Produces:
  - `DecisionAnswer.OpenDeal(players: Vector[PlayerId])`,
    `DecisionAnswer.PassGathering`, `DecisionAnswer.UnpassGathering`.
  - Journal tags `open-deal` (`{"kind","players"}`), `pass-gathering`,
    `unpass-gathering`.
  - `DecisionAnswerWire.OpenDealWire(playerIds: Vector[String])`,
    `DecisionAnswerWire.PassGatheringWire`,
    `DecisionAnswerWire.UnpassGatheringWire`; command kinds `open-deal`
    (`{"kind","playerIds"}`), `pass-gathering`, `unpass-gathering`.

- [ ] **Step 1: Write the failing tests**

Append to `WalkerDecisionWireSuite`, after "walker choice steps round
trip the negotiation answers":

```scala
  test("walker choice steps round trip the Gathering's answers"):
    val red = PlayerId("red")
    val events = Vector[DecisionAnswer](
      DecisionAnswer.OpenDeal(Vector(PlayerId("blue"), PlayerId("green"))),
      DecisionAnswer.PassGathering, DecisionAnswer.UnpassGathering)
      .map(answer => WalkerStepRecorded("0.0.0", ChoicePayload(
        "gathering.open", answer, red), Vector.empty): OathEvent)
    assertStreamRoundTrips("gathering", events)
```

In `CommandProtocolSuite`, add to `examples`, after the
`ResolveWalker("negotiation.deal", DecisionAnswerWire.DeclineDealWire)`
line:

```scala
    ResolveWalker("gathering.open",
      DecisionAnswerWire.OpenDealWire(Vector("blue", "green"))),
    ResolveWalker("gathering.open", DecisionAnswerWire.PassGatheringWire),
    ResolveWalker("gathering.1.deal", DecisionAnswerWire.UnpassGatheringWire),
```

and append:

```scala
  test("an open-deal answer naming a player twice is rejected"):
    val payload = ujson.Obj("kind" -> "open-deal",
      "playerIds" -> ujson.Arr("blue", "blue"))
    assert(CommandNestedCodecs.decodeDecisionAnswerWire(payload, "$.payload")
      .isLeft)
```

Append to `GameIntentMapperNegotiationSuite`:

```scala
  test("the Gathering's answers map to the engine's"):
    def mapped(answer: Wire) =
      GameIntentMapper.map(Intent.ResolveWalker("gathering.open", answer))
    def resolved(answer: DecisionAnswer) = Right(PlayerIntent.ResolveWalker(
      TreeDecision("gathering.open", answer)))
    assertEquals(mapped(Wire.OpenDealWire(Vector("blue", "green"))),
      resolved(DecisionAnswer.OpenDeal(Vector(PlayerId("blue"),
        PlayerId("green")))))
    assertEquals(mapped(Wire.PassGatheringWire),
      resolved(DecisionAnswer.PassGathering))
    assertEquals(mapped(Wire.UnpassGatheringWire),
      resolved(DecisionAnswer.UnpassGathering))
```

Append to `DecisionQuerySuite`, after "negotiation answers and queries
only meet each other":

```scala
  test("the Gathering's answers meet no other query"):
    assertEquals(accepts(chooseOne, DecisionAnswer.PassGathering),
      invalid("decision recover.choice expects a single-choice answer"))
    assertEquals(acceptsBy(red, DecisionAnswer.OpenDeal(Vector(blue))),
      invalid("decision recover.choice expects a negotiation answer"))
```

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.application.GameIntentMapperNegotiationSuite distantlands.gameplay.walker.DecisionQuerySuite" "sharedJVM/testOnly distantlands.protocol.CommandProtocolSuite"`
Expected: compile failure: `OpenDeal`, `OpenDealWire` are not members.
If `sharedJVM` is not the shared project's name, `./sbtw projects`
lists it.

- [ ] **Step 3: The model answers**

In `Decisions.scala`, after `case object DeclineDeal extends
DecisionAnswer`:

```scala

  /** Answer to a [[DecisionQuery.Gathering]]: opens a deal with `players`,
    * named in the order the opener chose them (catalog batch 12, slice
    * 6b). */
  final case class OpenDeal(players: Vector[PlayerId]) extends DecisionAnswer

  /** Answer to a [[DecisionQuery.Gathering]], or a bystander's to a
    * Gathering deal: passes (ruling 4). */
  case object PassGathering extends DecisionAnswer

  /** As [[PassGathering]]: takes back a pass. */
  case object UnpassGathering extends DecisionAnswer
```

- [ ] **Step 4: The journal codec**

In `DecisionAnswerCodec`, beside the other tags:

```scala
  private val OpenDealTag = "open-deal"
  private val PassGatheringTag = "pass-gathering"
  private val UnpassGatheringTag = "unpass-gathering"
```

In `encode`, after the `DeclineDeal` case:

```scala
    case DecisionAnswer.OpenDeal(players) => ujson.Obj(
      "kind" -> OpenDealTag,
      "players" -> ujson.Arr.from(players.map(p => ujson.Str(p.value))))
    case DecisionAnswer.PassGathering => ujson.Obj("kind" -> PassGatheringTag)
    case DecisionAnswer.UnpassGathering =>
      ujson.Obj("kind" -> UnpassGatheringTag)
```

In `decode`, before `case other =>`:

```scala
      case OpenDealTag => Right(DecisionAnswer.OpenDeal(
        value("players").arr.toVector.map(v => PlayerId(v.str))))
      case PassGatheringTag => Right(DecisionAnswer.PassGathering)
      case UnpassGatheringTag => Right(DecisionAnswer.UnpassGathering)
```

- [ ] **Step 5: The command wire, its codec and the mapper**

In `CommandIntents.scala`, after `case object DeclineDealWire extends
DecisionAnswerWire`:

```scala

  /** Opens a deal in The Gathering with the named players. */
  final case class OpenDealWire(playerIds: Vector[String])
      extends DecisionAnswerWire

  /** Passes in The Gathering. */
  case object PassGatheringWire extends DecisionAnswerWire

  /** Takes back a pass in The Gathering. */
  case object UnpassGatheringWire extends DecisionAnswerWire
```

In `CommandNestedCodecs`, in the encoder after the `DeclineDealWire` case:

```scala
    case DecisionAnswerWire.OpenDealWire(playerIds) => ujson.Obj(
      "kind" -> "open-deal",
      "playerIds" -> ujson.Arr.from(playerIds.map(ujson.Str(_))))
    case DecisionAnswerWire.PassGatheringWire =>
      ujson.Obj("kind" -> "pass-gathering")
    case DecisionAnswerWire.UnpassGatheringWire =>
      ujson.Obj("kind" -> "unpass-gathering")
```

and in `decodeDecisionAnswerWire`, before `case kind =>`:

```scala
      case "open-deal" => for
        _ <- exact(root, Set("kind", "playerIds"), path)
        raw <- field(root, "playerIds", path)
        playerIds <- strings(raw, s"$path.playerIds")
        _ <- noDuplicates(playerIds, s"$path.playerIds")
      yield DecisionAnswerWire.OpenDealWire(playerIds)
      case "pass-gathering" => exact(root, Set("kind"), path)
        .map(_ => DecisionAnswerWire.PassGatheringWire)
      case "unpass-gathering" => exact(root, Set("kind"), path)
        .map(_ => DecisionAnswerWire.UnpassGatheringWire)
```

`strings` is `CommandJsonSupport.strings`, which the citizenship decoder
already uses; import it the same way if the compiler names it.

In `GameIntentMapper`, after the `DeclineDealWire` case:

```scala
    case DecisionAnswerWire.OpenDealWire(playerIds) =>
      Right(DecisionAnswer.OpenDeal(playerIds.map(PlayerId(_))))
    case DecisionAnswerWire.PassGatheringWire =>
      Right(DecisionAnswer.PassGathering)
    case DecisionAnswerWire.UnpassGatheringWire =>
      Right(DecisionAnswer.UnpassGathering)
```

Add a case wherever else the compiler names a non-exhaustive match on
`DecisionAnswer` or `DecisionAnswerWire`; neither answer writes a log
line or names an option.

- [ ] **Step 6: Run the tests**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/Decisions.scala src/main/scala/distantlands/serialization/DecisionAnswerCodec.scala shared/src/main/scala/distantlands/protocol/CommandIntents.scala shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala src/main/scala/distantlands/application/GameIntentMapper.scala src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): answers that open a Gathering deal or pass

OpenDeal, PassGathering and UnpassGathering travel from the client to
the journal (catalog batch 12, slice 6b). No query accepts them yet.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: The open question and the deal's bystanders, model to panel

**Files:**
- Modify: `src/main/scala/distantlands/model/Decisions.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/WalkerDecisionProjector.scala`
- Modify: `src/main/scala/distantlands/application/NegotiationDealProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/GatheringPanel.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/GatheringPanelSuite.scala` (new)
- Test: `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ParkedDecisionSuite.scala`

**Interfaces:**
- Consumes: Task 1's answers and wires.
- Produces:
  - `DecisionQuery.Gathering(present: Vector[PlayerId], reach:
    Map[PlayerId, Vector[PlayerId]], passed: Vector[PlayerId], heading:
    Option[String] = None)`.
  - `DecisionQuery.Negotiate` gains, last, `bystanders: Vector[PlayerId]
    = Vector.empty` and `passed: Vector[PlayerId] = Vector.empty`.
  - `DecisionQueryProjection.Gathering(presentPlayerIds: Vector[String],
    passedPlayerIds: Vector[String], reachablePlayerIds: Vector[String],
    heading: Option[String] = None)`, form `gathering`.
  - `NegotiationDealProjection` gains, last, `bystanderPlayerIds:
    Vector[String] = Vector.empty` and `passedPlayerIds: Vector[String] =
    Vector.empty`, both required codec keys.
  - `ParkedDecision.Surface.Gathering(decision: WalkerDecisionState,
    query: DecisionQueryState.Gathering)`;
    `Surface.Negotiate(deal, editor, passing: Option[String] = None)`,
    `passing` the decision id when the viewer is a bystander in control.
  - `GatheringPanel.render(surface, playerId, canControl, panel,
    controls)`; buttons `gathering-open`, `gathering-pass`,
    `gathering-unpass`; the deal panel's `negotiation-pass` and
    `negotiation-unpass`.

- [ ] **Step 1: Write the failing query tests**

Append to `DecisionQuerySuite`:

```scala
  private val gatheringQuery = DecisionQuery.Gathering(
    Vector(red, blue, green), Map(red -> Vector(blue, green),
      blue -> Vector(red, green), green -> Vector(red, blue)),
    Vector(green), Some("The Gathering"))

  test("a Gathering question needs two players present, reach for each " +
      "and passes from among them"):
    assertEquals(wellFormed(gatheringQuery), Right(()))
    assertEquals(wellFormed(gatheringQuery.copy(present = Vector(red),
      reach = Map(red -> Vector(blue)), passed = Vector.empty)),
      invalid("decision recover.choice declares fewer than two distinct " +
        "players present"))
    assertEquals(wellFormed(gatheringQuery.copy(
      reach = gatheringQuery.reach - green)),
      invalid("decision recover.choice declares reach for other players " +
        "than those present"))
    assertEquals(wellFormed(gatheringQuery.copy(
      reach = gatheringQuery.reach.updated(red, Vector(red, blue)))),
      invalid("decision recover.choice declares a reach naming its own " +
        "player, nobody, or a player twice"))
    assertEquals(wellFormed(gatheringQuery.copy(
      passed = Vector(PlayerId("purple")))),
      invalid("decision recover.choice declares a pass from a player not " +
        "present"))
    assertEquals(wellFormed(gatheringQuery.copy(
      passed = Vector(red, blue, green))),
      invalid("decision recover.choice declares every player passed, " +
        "leaving nothing to decide"))

  test("a Gathering question opens a deal within the answerer's reach, " +
      "and passes by who has not passed"):
    def by(player: PlayerId, answer: DecisionAnswer) =
      acceptsBy(player, answer, gatheringQuery)
    assertEquals(by(red, DecisionAnswer.OpenDeal(Vector(blue))), Right(()))
    // A passed player may open a deal (ruling 32).
    assertEquals(by(green, DecisionAnswer.OpenDeal(Vector(red, blue))),
      Right(()))
    assertEquals(by(PlayerId("purple"), DecisionAnswer.OpenDeal(Vector(red))),
      invalid("decision recover.choice is not open to this player"))
    assertEquals(by(red, DecisionAnswer.OpenDeal(Vector.empty)),
      invalid("decision recover.choice names nobody to negotiate with"))
    assertEquals(by(red, DecisionAnswer.OpenDeal(Vector(blue, blue))),
      invalid("decision recover.choice names a player twice"))
    assertEquals(by(red, DecisionAnswer.OpenDeal(Vector(PlayerId("purple")))),
      invalid("decision recover.choice names a player out of this " +
        "player's reach"))
    assertEquals(by(red, DecisionAnswer.PassGathering), Right(()))
    assertEquals(by(green, DecisionAnswer.PassGathering),
      invalid("decision recover.choice does not let this player pass now"))
    assertEquals(by(green, DecisionAnswer.UnpassGathering), Right(()))
    assertEquals(by(red, DecisionAnswer.UnpassGathering),
      invalid("decision recover.choice does not let this player take back " +
        "a pass"))
    assertEquals(by(red, DecisionAnswer.AcceptDeal),
      invalid("decision recover.choice expects a Gathering answer"))

  test("a deal's bystander may only pass or take back a pass, and a " +
      "participant may do neither"):
    val watched = deal(acceptors = Set(red, blue))
      .copy(bystanders = Vector(green))
    assertEquals(wellFormed(watched), Right(()))
    assertEquals(wellFormed(watched.copy(bystanders = Vector(blue))),
      invalid("decision recover.choice declares a bystander inside the deal"))
    assertEquals(wellFormed(watched.copy(passed = Vector(red))),
      invalid("decision recover.choice declares a pass from outside its " +
        "bystanders"))
    assertEquals(acceptsBy(green, DecisionAnswer.PassGathering, watched),
      Right(()))
    assertEquals(acceptsBy(green, DecisionAnswer.UnpassGathering, watched),
      invalid("decision recover.choice does not let this player take back " +
        "a pass"))
    assertEquals(acceptsBy(green, DecisionAnswer.UnpassGathering,
      watched.copy(passed = Vector(green))), Right(()))
    assertEquals(acceptsBy(blue, DecisionAnswer.PassGathering, watched),
      invalid("decision recover.choice does not let this player pass now"))
    assertEquals(acceptsBy(green, DecisionAnswer.AcceptDeal, watched),
      invalid("decision recover.choice does not let this player accept now"))
    assertEquals(acceptsBy(green, DecisionAnswer.DeclineDeal, watched),
      invalid("decision recover.choice is not open to this player"))
```

The suite's `decisionId` is `recover.choice`, as its other messages show.

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.walker.DecisionQuerySuite"`
Expected: compile failure: `Gathering` and `bystanders` are not members.

- [ ] **Step 3: The query and the deal's bystanders**

In `Decisions.scala`, `DecisionQuery.Negotiate`'s last field becomes:

```scala
      privateDeal: Boolean = false,
      /** Players outside the deal who may still answer it, only to pass or
        * take back a pass: The Gathering's players at its site (catalog
        * batch 12, ruling 4). Empty in every other deal. */
      bystanders: Vector[PlayerId] = Vector.empty,
      /** The bystanders who have passed. */
      passed: Vector[PlayerId] = Vector.empty) extends DecisionQuery
```

After `Negotiate`, add:

```scala

  /** The Gathering's open question (catalog batch 12, slice 6b): each
    * player at its site may open a deal with players their reach lets them
    * name, pass, or take back a pass (rulings 3 to 5 and 32). `present` is
    * who is at the site, in the Gathering's order; `reach` gives each of
    * them the players they may name; `passed` is who has passed. As in a
    * deal, legality depends on who answers. */
  final case class Gathering(present: Vector[PlayerId],
      reach: Map[PlayerId, Vector[PlayerId]], passed: Vector[PlayerId],
      heading: Option[String] = None) extends DecisionQuery
```

In `DecisionQueries.wellFormed`, at the end of the `Negotiate` case's
`for`, before `yield ()`:

```scala
        _ <- require(bystanders.distinct.size == bystanders.size &&
          !bystanders.exists(members), decisionId,
          "declares a bystander inside the deal")
        _ <- require(negotiate.passed.forall(bystanders.contains), decisionId,
          "declares a pass from outside its bystanders")
```

with `val bystanders = negotiate.bystanders` beside the case's other
vals, and add a case after it:

```scala
    case gathering: DecisionQuery.Gathering =>
      val (present, reach, passed) =
        (gathering.present, gathering.reach, gathering.passed)
      for
        _ <- require(present.size >= 2 &&
          present.distinct.size == present.size, decisionId,
          "declares fewer than two distinct players present")
        _ <- require(reach.keySet == present.toSet, decisionId,
          "declares reach for other players than those present")
        _ <- require(reach.forall((player, named) => named.nonEmpty &&
          !named.contains(player) && named.distinct.size == named.size),
          decisionId,
          "declares a reach naming its own player, nobody, or a player twice")
        _ <- require(passed.forall(present.contains), decisionId,
          "declares a pass from a player not present")
        _ <- require(!present.forall(passed.contains), decisionId,
          "declares every player passed, leaving nothing to decide")
      yield ()
```

In `accepts`, in the `Negotiate` case, before `case _ =>`:

```scala
        case DecisionAnswer.PassGathering =>
          require(negotiate.bystanders.contains(by) &&
            !negotiate.passed.contains(by), decisionId,
            "does not let this player pass now")
        case DecisionAnswer.UnpassGathering =>
          require(negotiate.passed.contains(by), decisionId,
            "does not let this player take back a pass")
```

and add a case after the `Negotiate` one:

```scala
    case gathering: DecisionQuery.Gathering => answer match
      case DecisionAnswer.OpenDeal(players) =>
        val reach = gathering.reach.getOrElse(by, Vector.empty)
        for
          _ <- require(gathering.present.contains(by), decisionId,
            "is not open to this player")
          _ <- require(players.nonEmpty, decisionId,
            "names nobody to negotiate with")
          _ <- require(players.distinct.size == players.size, decisionId,
            "names a player twice")
          _ <- require(players.forall(reach.contains), decisionId,
            "names a player out of this player's reach")
        yield ()
      case DecisionAnswer.PassGathering =>
        require(gathering.present.contains(by) &&
          !gathering.passed.contains(by), decisionId,
          "does not let this player pass now")
      case DecisionAnswer.UnpassGathering =>
        require(gathering.passed.contains(by), decisionId,
          "does not let this player take back a pass")
      case _ =>
        reject(decisionId, "expects a Gathering answer")
```

Do not run the tests yet: `-Werror` refuses the build until Step 5 gives
`WalkerDecisionProjector`'s exhaustive query match its Gathering case.
Step 7 runs them.

- [ ] **Step 4: Write the failing projection and panel tests**

Append to `ProjectionProtocolSuite`, beside the inspect test:

```scala
  test("a Gathering's open question round-trips who is there, who passed " +
      "and whom the viewer may name"):
    val gathering = DecisionQueryProjection.Gathering(Vector("p1", "p2", "p3"),
      Vector("p3"), Vector("p2", "p3"),
      heading = Some("The Gathering at Ancient City"))
    val carrying = projection.copy(walkerDecision =
      projection.walkerDecision.map(_.copy(query = Some(gathering))))
    assertRoundTrips(carrying)

  test("a deal round-trips its bystanders and their passes"):
    val watched = DecisionQueryProjection.Negotiate(NegotiationDealProjection(
      Vector("p1", "p2"), Vector.empty, Vector.empty, Vector.empty,
      bystanderPlayerIds = Vector("p3"), passedPlayerIds = Vector("p3")))
    val carrying = projection.copy(walkerDecision =
      projection.walkerDecision.map(_.copy(query = Some(watched))))
    assertRoundTrips(carrying)
```

Import `NegotiationDealProjection` if the suite does not.

Create `frontend/src/test/scala/distantlands/frontend/GatheringPanelSuite.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, GameIntent}
import distantlands.testkit.Unwrap.as
import org.scalajs.dom

/** The Gathering's open question: who is there, a box per player the
  * viewer may name, Negotiate, and Pass or Take back pass. Runs under
  * jsdom. */
class GatheringPanelSuite extends munit.FunSuite:
  private def query(passed: Vector[String] = Vector.empty) =
    DecisionQueryState.Gathering(Vector("red", "blue", "green"), passed,
      Vector("blue", "green"), Some("The Gathering at Ancient City"))

  private def render(value: DecisionQueryState.Gathering,
      ui: RecordingControls, canControl: Boolean = true,
      playerId: String = "red"): dom.Element =
    val panel = dom.document.createElement("div")
    val decision = WalkerDecisionState("search", "gathering.open",
      query = Some(value))
    GatheringPanel.render(ParkedDecision.Surface.Gathering(decision, value),
      playerId, canControl, panel, ui)
    panel

  private def button(root: dom.Element, selector: String): dom.html.Button =
    as[dom.html.Button](Dom.one(root, selector))

  private def box(root: dom.Element, name: String): dom.html.Input =
    as[dom.html.Input](Dom.one(root,
      s"input[aria-label='Negotiate with $name']"))

  test("the panel names who is there and who has passed"):
    val panel = render(query(passed = Vector("green")), new RecordingControls())
    assertEquals(panel.querySelector("h2").textContent,
      "The Gathering at Ancient City")
    assertEquals(panel.querySelector(".gathering-present").textContent,
      "red · blue · green (passed)")

  test("Negotiate opens a deal with the checked players, and waits for one"):
    val ui = new RecordingControls()
    val panel = render(query(), ui)
    assert(button(panel, "button.gathering-open").disabled)
    val green = box(panel, "green")
    green.checked = true
    green.dispatchEvent(new dom.Event("change"))
    assert(!button(panel, "button.gathering-open").disabled)
    button(panel, "button.gathering-open").click()
    assertEquals(ui.submitted, Vector(GameIntent.ResolveWalker("gathering.open",
      DecisionAnswerWire.OpenDealWire(Vector("green")))))

  test("Pass, and Take back pass once passed"):
    val ui = new RecordingControls()
    button(render(query(), ui), "button.gathering-pass").click()
    button(render(query(passed = Vector("red")), ui),
      "button.gathering-unpass").click()
    assertEquals(ui.submitted, Vector(
      GameIntent.ResolveWalker("gathering.open",
        DecisionAnswerWire.PassGatheringWire),
      GameIntent.ResolveWalker("gathering.open",
        DecisionAnswerWire.UnpassGatheringWire)))

  test("a viewer out of control gets disabled controls, and one not there " +
      "gets none"):
    val idle = render(query(), new RecordingControls(), canControl = false)
    assert(button(idle, "button.gathering-pass").disabled)
    assert(box(idle, "blue").disabled)
    val away = render(query(), new RecordingControls(), playerId = "purple")
    assertEquals(away.querySelectorAll("button").length, 0)
```

`Dom.one` and `RecordingControls` are the frontend test kit's, as
`NegotiationDealPanelSuite` uses them.

Append to `NegotiationDealPanelSuite`:

```scala
  /** A Gathering deal between blue and green that red watches. */
  private def watchedByRed(passed: Vector[String] = Vector.empty) =
    NegotiationDealState(participantPlayerIds = Vector("blue", "green"),
      acceptedPlayerIds = Vector.empty, transfers = Vector.empty,
      disclosures = Vector.empty, bystanderPlayerIds = Vector("red"),
      passedPlayerIds = passed)

  test("a bystander passes, or takes back a pass, from the deal panel"):
    val ui = new RecordingControls()
    val passing = render(Surface.Negotiate(watchedByRed(), None,
      Some("gathering.1.deal")), ui)
    button(passing, "button.negotiation-pass").click()
    val passed = render(Surface.Negotiate(watchedByRed(Vector("red")), None,
      Some("gathering.1.deal")), ui)
    button(passed, "button.negotiation-unpass").click()
    assertEquals(ui.submitted, Vector(
      Intent.ResolveWalker("gathering.1.deal",
        DecisionAnswerWire.PassGatheringWire),
      Intent.ResolveWalker("gathering.1.deal",
        DecisionAnswerWire.UnpassGatheringWire)))
    // A spectator of the same deal gets no button.
    assertEquals(render(spectator(watchedByRed()), new RecordingControls())
      .querySelectorAll("button.negotiation-pass").length, 0)
```

Append to `ParkedDecisionSuite`:

```scala
  test("a Gathering's open question routes to its panel, for a viewer in " +
      "control only"):
    val gathering = DecisionQueryState.Gathering(Vector("red", "blue"),
      Vector.empty, Vector("blue"), heading)
    val open = parked("search", "gathering.open", gathering)
    assertEquals(routeOf(open).surface, Some(Surface.Gathering(open, gathering)))
    assertEquals(routeOf(open, observer).surface, None)

  test("a bystander's deal routes with its pass control"):
    val watched = NegotiationDealState(Vector("blue", "green"), Vector.empty,
      Vector.empty, Vector.empty, None, bystanderPlayerIds = Vector("red"))
    val dealt = parked("search", "gathering.1.deal",
      DecisionQueryState.Negotiate(watched, heading))
    assertEquals(routeOf(dealt).surface,
      Some(Surface.Negotiate(watched, None, Some("gathering.1.deal"))))
    assertEquals(routeOf(dealt, observer).surface,
      Some(Surface.Negotiate(watched, None, None)))
```

- [ ] **Step 5: The projection**

In `ActionProjectionDtos.scala`, after `DecisionQueryProjection.Negotiate`:

```scala

  /** The Gathering's open question, as one viewer may answer it: who is
    * at the site, who has passed, and whom this viewer may name (empty for
    * a viewer not at the site). */
  final case class Gathering(presentPlayerIds: Vector[String],
      passedPlayerIds: Vector[String], reachablePlayerIds: Vector[String],
      heading: Option[String] = None) extends DecisionQueryProjection:
    def offeredOptions: Vector[DecisionOptionProjection] = Vector.empty
```

`NegotiationDealProjection`'s last field becomes:

```scala
    privateDeal: Boolean = false,
    /** The players outside a Gathering deal who may still pass in it, and
      * those of them who have. Empty in every other deal. */
    bystanderPlayerIds: Vector[String] = Vector.empty,
    passedPlayerIds: Vector[String] = Vector.empty)
```

In `ActionProjectionCodec`, encode after the `Negotiate` case:

```scala
      case DecisionQueryProjection.Gathering(present, passed, reachable,
          heading) => ujson.Obj(
        "form" -> "gathering",
        "presentPlayerIds" -> encoded(present)(ujson.Str(_)),
        "passedPlayerIds" -> encoded(passed)(ujson.Str(_)),
        "reachablePlayerIds" -> encoded(reachable)(ujson.Str(_)),
        "heading" -> stringOption(heading))
```

dispatch `case "gathering" => decodeGathering(value, path)` after
`"negotiate"`, and add beside `decodeNegotiate`:

```scala
  private def decodeGathering(value: ujson.Obj, path: String)
      : Result[DecisionQueryProjection] = for
    _ <- exact(value, Set("form", "presentPlayerIds", "passedPlayerIds",
      "reachablePlayerIds", "heading"), path)
    present <- strings(value, "presentPlayerIds", path)
    passed <- strings(value, "passedPlayerIds", path)
    reachable <- strings(value, "reachablePlayerIds", path)
    heading <- optionalString(value, "heading", path)
  yield DecisionQueryProjection.Gathering(present, passed, reachable, heading)
```

In `encodeDeal`, after `"privateDeal" -> value.privateDeal` (add the
comma):

```scala
    "bystanderPlayerIds" -> encoded(value.bystanderPlayerIds)(ujson.Str(_)),
    "passedPlayerIds" -> encoded(value.passedPlayerIds)(ujson.Str(_)))
```

In `decodeDeal`, add both keys to the `exact` set, read them after
`privateDeal`:

```scala
    bystanders <- strings(value, "bystanderPlayerIds", path)
    passed <- strings(value, "passedPlayerIds", path)
```

and pass `bystanderPlayerIds = bystanders, passedPlayerIds = passed` to
the constructor.

In `WalkerDecisionProjector`, after the query match's `Negotiate` case:

```scala
      case gathering: DecisionQuery.Gathering =>
        Some(DecisionQueryProjection.Gathering(
          gathering.present.map(_.value), gathering.passed.map(_.value),
          viewer.flatMap(gathering.reach.get).getOrElse(Vector.empty)
            .map(_.value), gathering.heading))
```

In `NegotiationDealProjector.project`, pass to the constructor, after
`privateDeal = query.privateDeal`:

```scala
      bystanderPlayerIds = query.bystanders.map(_.value),
      passedPlayerIds = query.passed.map(_.value))
```

Bystanders and passes are public: everyone at the table sees who stands
outside the deal.

- [ ] **Step 6: The panels**

In `ParkedDecision.scala`, `Surface.Negotiate` becomes:

```scala
    /** The deal summary every viewer sees; `editor` is the decision id and
      * the editable terms for the one viewer who may answer, `None` for an
      * observer of the parked deal or a viewer the deal is waiting on.
      * `passing` is the decision id for a Gathering bystander in control,
      * who may only pass or take back a pass.
      */
    case Negotiate(deal: NegotiationDealState,
        editor: Option[(String, NegotiationEditingState)],
        passing: Option[String] = None)
    /** The Gathering's open question (catalog batch 12, slice 6b). */
    case Gathering(decision: WalkerDecisionState,
        query: DecisionQueryState.Gathering)
```

In `render`, beside the `Negotiate` case:

```scala
      case surface: Surface.Gathering => GatheringPanel.render(surface,
        presentation.playerId, canControl, panel, controls)
```

In `formSurface`, the `Negotiate` case becomes, and a case follows it:

```scala
      case negotiate: DecisionQueryState.Negotiate =>
        // An owner with no editor is a bystander: only a Gathering deal
        // has owners outside it.
        Some(Surface.Negotiate(negotiate.deal, negotiate.deal.editing
          .filter(_ => showGameplayControls).map(decision.decisionId -> _),
          Option.when(showGameplayControls &&
            negotiate.deal.editing.isEmpty &&
            negotiate.deal.bystanderPlayerIds.nonEmpty)(decision.decisionId)))
      case gathering: DecisionQueryState.Gathering =>
        Some(Surface.Gathering(decision, gathering))
```

Create `frontend/src/main/scala/distantlands/frontend/GatheringPanel.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, GameIntent => GameCommand}
import org.scalajs.dom
import ServerUiSupport.{button, text}

/** The Gathering's open question (catalog batch 12, slice 6b): who is at
  * the site and who has passed, then, for a player there, a box for each
  * player they may name, Negotiate, and Pass or Take back pass. Negotiate
  * waits until a box is checked. A viewer not at the site reads the
  * names only.
  */
private[frontend] object GatheringPanel:
  def render(surface: ParkedDecision.Surface.Gathering, playerId: String,
      canControl: Boolean, panel: dom.Element, controls: TableControls): Unit =
    val query = surface.query
    val decisionId = surface.decision.decisionId
    panel.appendChild(text("h2", "", query.heading.getOrElse("The Gathering")))
    panel.appendChild(text("p", "gathering-present",
      query.presentPlayerIds.map(id =>
        if query.passedPlayerIds.contains(id) then s"$id (passed)" else id)
        .mkString(" · ")))
    if query.presentPlayerIds.contains(playerId) then
      val boxes = query.reachablePlayerIds.map(id =>
        id -> nameBox(id, canControl, panel))
      val negotiate = button("Negotiate", "gathering-open")
      def refresh(): Unit =
        negotiate.disabled = !canControl || !boxes.exists(_._2.checked)
      boxes.foreach((_, check) => check.onchange = _ => refresh())
      refresh()
      negotiate.onclick = _ => controls.submitCommand(GameCommand.ResolveWalker(
        decisionId, DecisionAnswerWire.OpenDealWire(boxes.collect {
          case (id, check) if check.checked => id })))
      panel.appendChild(negotiate)
      val passed = query.passedPlayerIds.contains(playerId)
      val toggle =
        if passed then button("Take back pass", "gathering-unpass")
        else button("Pass", "gathering-pass")
      toggle.disabled = !canControl
      toggle.onclick = _ => controls.submitCommand(GameCommand.ResolveWalker(
        decisionId,
        if passed then DecisionAnswerWire.UnpassGatheringWire
        else DecisionAnswerWire.PassGatheringWire))
      panel.appendChild(toggle)

  /** One player the viewer may name: the box and the name, in one
    * `label`, named by its `aria-label`. */
  private def nameBox(id: String, canControl: Boolean,
      panel: dom.Element): dom.html.Input =
    val check = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    check.`type` = "checkbox"
    check.setAttribute("aria-label", s"Negotiate with $id")
    check.disabled = !canControl
    val row = dom.document.createElement("label").asInstanceOf[dom.html.Label]
    row.className = "gathering-item"
    row.appendChild(check)
    row.appendChild(text("span", "gathering-item-label", id))
    panel.appendChild(row)
    check
```

In `NegotiationDealPanel.render`, after the editor:

```scala
    surface.passing.filter(_ => surface.deal.bystanderPlayerIds
      .contains(playerId)).foreach(decisionId =>
      passButton(decisionId, surface.deal, playerId, canControl, panel,
        controls))
```

and add:

```scala
  /** A Gathering bystander's one control (ruling 4): Pass, or Take back
    * pass once passed. */
  private def passButton(decisionId: String, deal: NegotiationDealState,
      playerId: String, canControl: Boolean, panel: dom.Element,
      controls: TableControls): Unit =
    val passed = deal.passedPlayerIds.contains(playerId)
    val toggle =
      if passed then button("Take back pass", "negotiation-unpass")
      else button("Pass", "negotiation-pass")
    toggle.disabled = !canControl
    toggle.onclick = _ => controls.submitCommand(GameCommand.ResolveWalker(
      decisionId,
      if passed then DecisionAnswerWire.UnpassGatheringWire
      else DecisionAnswerWire.PassGatheringWire))
    panel.appendChild(toggle)
```

Add a case wherever else the compiler names a non-exhaustive match on
`DecisionQuery`, `DecisionQueryProjection` or `Surface` (the walker's
narrowing, `BoardOptionViews`): the Gathering query has no options, so it
passes through as `Negotiate` does. The new classes need no stylesheet
rule to work; if the frontend style check names `.gathering-item`, give it
the rule `.negotiation-item` has.

- [ ] **Step 7: Run the tests**

Run: `./sbtw "testOnly distantlands.gameplay.walker.DecisionQuerySuite distantlands.application.NegotiationDealProjectionSuite distantlands.application.WalkerDecisionProjectorSuite" "sharedJVM/testOnly distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.GatheringPanelSuite distantlands.frontend.NegotiationDealPanelSuite distantlands.frontend.ParkedDecisionSuite"`
Expected: PASS. `./sbtw projects` names the shared and frontend
projects if these are not their names.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model/Decisions.scala src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala src/main/scala/distantlands/application/WalkerDecisionProjector.scala src/main/scala/distantlands/application/NegotiationDealProjector.scala frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala frontend/src/main/scala/distantlands/frontend/GatheringPanel.scala frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala frontend/src/test/scala/distantlands/frontend/GatheringPanelSuite.scala frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala frontend/src/test/scala/distantlands/frontend/ParkedDecisionSuite.scala
git commit -m "$(cat <<'EOF'
feat(negotiation): a Gathering's open question and a deal's bystanders

DecisionQuery.Gathering lets each player at the site open a deal with
the players they may reach, pass, or take back a pass. A deal's
bystanders may only pass. Both reach the client, which answers them
from a Gathering panel and the deal panel (catalog batch 12, slice 6b).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add any file Step 6's compiler pass touched.

---

### Task 3: The Gathering's pawns

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala`
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Modify: `src/main/scala/distantlands/gameplay/TurnOrder.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringSuite.scala` (new)

**Interfaces:**
- Produces:
  - `TurnOrder.nextRoundOrder(ready: ReadyGame): Vector[PlayerId]`.
  - `TheGatheringCard` (in `distantlands.gameplay.powers.whenplayed`),
    `TheGathering` with `cardId`, `id`, `pawnDecisionId`
    (`"cardplay.the-gathering.pawn"`), `join`, `stay`
    (`DecisionOptionRef.Button`), note keys `gathered`, `nobody`, and
    `present(ready: ReadyGame, site: SiteId, order: Vector[PlayerId]):
    Vector[PlayerId]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringSuite.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The Gathering (card 27): who is asked to come, in what order, and what
  * coming does (catalog batch 12, rulings 1 and 2). The card stands at
  * `homeSite`. */
class TheGatheringSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[TheGathering.type]
  private val gathering = power.cardId
  private val played = hookAt(gathering, homeSite)
  private val come = DecisionAnswer.ChooseOneAnswer(TheGathering.join)
  private val stay = DecisionAnswer.ChooseOneAnswer(TheGathering.stay)

  /** The Gathering at `homeSite`, and each pawn where `at` puts it. */
  private def gatheringWith(at: (PlayerId, SiteId)*): Table =
    at.foldLeft(Table.start.denizen(gathering, at = homeSite)) {
      case (table, (player, site)) => table.pawn(player, site) }

  private def pawnSite(ready: ReadyGame, player: PlayerId): Option[SiteId] =
    ready.game.current.pawnSite(player)

  private def gathered(players: String): NoteText.Said =
    NoteText.Said("gathered", s"Gathered at ${homeSite.value}: $players.",
      covers = false)

  test("each player elsewhere is asked, the actor included, in turn order " +
      "from the first player"):
    // No Chancellor, so the order is p1, p2, p3. p2 is already there.
    val walked = walkAnswering(gatheringWith(p1 -> nearSite,
      p2 -> homeSite, p3 -> awaySite).ready, power, played, stay, stay)
    assertEquals(walked.asked.map(question =>
      (question.decisionId, question.owner)), Vector(
      TheGathering.pawnDecisionId -> p1, TheGathering.pawnDecisionId -> p3))
    // One player there: the deals are skipped.
    assertEquals(said(power, walked.events), Vector(gathered("p2")))

  test("with a Chancellor, the questions start from the Chancellor"):
    val ready = gatheringWith(p1 -> nearSite, p2 -> awaySite,
      p3 -> nearSite).imperial(p3).ready
    val walked = walkAnswering(ready, power, played, stay, stay, stay)
    assertEquals(walked.asked.map(_.owner), Vector(p3, p1, p2))
    assertEquals(said(power, walked.events), Vector(NoteText.Said("nobody",
      s"Nobody gathered at ${homeSite.value}.", covers = false)))

  test("a yes moves the pawn with a plain move, not Travel"):
    val ready = gatheringWith(p1 -> nearSite, p2 -> awaySite,
      p3 -> awaySite).ready
    val walked = walkAnswering(ready, power, played, come, stay, stay)
    assertEquals(pawnSite(walked.end, p1), Some(homeSite))
    assertEquals(pawnSite(walked.end, p2), Some(awaySite))
    // The move is the one recorded operation: no Supply, no travel window.
    assertEquals(ops(walked.events), Vector(Move(Piece.Pawn(p1),
      PositionedLocation(Location.Site(nearSite)),
      PositionedLocation(Location.Site(homeSite)))))
    assertEquals(said(power, walked.events), Vector(gathered("p1")))
    assertJournalRebuilds(ready, walked.events, walked.end)
```

Each test leaves at most one player at the site, so Task 4's deals never
start in them.

- [ ] **Step 2: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.TheGatheringSuite"`
Expected: compile failure: `TheGathering` is not found.

- [ ] **Step 3: The next round's order**

In `TurnOrder.scala`, after `nextRoundStart`:

```scala
  /** The order the next round would run in, round the table from
    * `nextRoundStart`: The Gathering's (catalog batch 12, ruling 1). */
  def nextRoundOrder(ready: ReadyGame): Vector[PlayerId] =
    val players = ready.game.current.players.map(_.player)
    val index = players.indexOf(nextRoundStart(ready))
    players.drop(index) ++ players.take(index)
```

- [ ] **Step 4: The card and its pawns**

Delete `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala`.

Create `src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{Denizen, PrintedPower, SiteOnly}
import distantlands.gameplay.TurnOrder
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.model._

object TheGatheringCard extends Denizen(DenizenId("27"), "The Gathering", Suit.Nomad) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.the-gathering"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** any players in turn order may put their " +
      "pawn on this site. Then, players with pawns here may " +
      "negotiate binding exchanges of [favor], [secret], relics, " +
      "and advisers.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Gathering (card 27, site-only), WHEN PLAYED: any players in turn
  * order may put their pawn on this site. Then, players with pawns here
  * may negotiate binding exchanges of favor, secrets, relics and advisers
  * (catalog batch 12, slice 6b).
  *
  * The order is the next round's, from the Chancellor, else the first
  * player (ruling 1). Each player whose pawn is elsewhere, the actor
  * included, is asked as the owner of their own question whether to put it
  * here; a yes moves it with a plain `Move`, not Travel (ruling 2), and it
  * stays. Every player keeps the same place in the tree whether or not
  * they are asked, so a later player's question resumes where it parked.
  * One line then names who is here.
  *
  * The site is the hook's: a site-only card's play names it in its
  * `RuleSourceRef.SiteCard`.
  */
case object TheGathering extends WhenPlayedPower:
  val cardId: DenizenId = TheGatheringCard.id
  val id: PowerId = TheGatheringCard.power.id
  val pawnDecisionId: String = "cardplay.the-gathering.pawn"
  val join: DecisionOptionRef.Button = DecisionOptionRef.Button("gathering.join")
  val stay: DecisionOptionRef.Button = DecisionOptionRef.Button("gathering.stay")
  private val notes = NoteSet(id)
  /** "Gathered at Ancient City: p1 and p2." Sourceless: the card-play line
    * before it names the card. */
  val gathered: NoteKey = notes.key("gathered", Vector(
    NotePart.Text("Gathered at "), NotePart.Arg(0), NotePart.Text(": "),
    NotePart.Arg(1), NotePart.Text(".")))
  /** "Nobody gathered at Ancient City." */
  val nobody: NoteKey = notes.key("nobody", Vector(
    NotePart.Text("Nobody gathered at "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The Gathered line names everyone here, so a pawn's answer needs no
    * "Chose" line: a yes is in the list, a no is not. */
  override def narratedDecisions: Set[String] = Set(pawnDecisionId)

  def effect(ctx: PowerCtx): Vector[Operation] = ctx.operation match
    case CardPlayedFaceup(_, _, RuleSourceRef.SiteCard(site, _), _) =>
      val order = TurnOrder.nextRoundOrder(ctx.state)
      val name = ctx.catalog.site(site).fold(site.value)(_.name)
      order.flatMap(pawn(site, name, _)) :+
        Note(id, states => Some(line(states.now, site, order)))
    case _ => Vector.empty

  /** The players whose pawns stand at `site`, in `order`. */
  def present(ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId]): Vector[PlayerId] =
    order.filter(ready.game.current.pawnSite(_).contains(site))

  /** `player`'s place: their question while their pawn is elsewhere, then
    * the move a yes makes. */
  private def pawn(site: SiteId, name: String,
      player: PlayerId): Vector[Operation] = Vector(
    Branch((live, _) => Option.when(
      !live.game.current.pawnSite(player).contains(site))(
      Decide(pawnDecisionId, player, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(join, s"Put my pawn on $name"),
        DecisionOption.Button(stay, "Stay")),
        heading = Some(s"The Gathering: put your pawn on $name?")))).toVector),
    BuildOps((live, pending) =>
      if joined(pending.answered, player) then
        PawnMoves.relocate(live, player, site)
      else Right(Vector.empty)))

  private def joined(answered: Vector[Answered], player: PlayerId): Boolean =
    answered.exists {
      case Answered(`pawnDecisionId`, DecisionAnswer.ChooseOneAnswer(`join`),
          `player`) => true
      case _ => false
    }

  private def line(ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId]): LogNote =
    present(ready, site, order) match
      case Vector() => nobody.sourceless(NoteArg.Site(site))
      case here => gathered.sourceless(NoteArg.Site(site), NoteArg.Players(here))
```

In `WhenPlayedPowers.all`, append `TheGathering` after `FalseProphet`.

Remove the line `denizen.the-gathering` from
`src/test/resources/powers/unimplemented.txt`.

- [ ] **Step 5: Run the tests and refresh the note-key snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.TheGatheringSuite"`
Expected: PASS.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
then `git diff src/test/resources/notes/declared-keys.txt`: exactly two
added lines, `denizen.the-gathering gathered = Gathered at {0}: {1}.` and
`denizen.the-gathering nobody = Nobody gathered at {0}.`. If the suite
lives elsewhere, `grep -rl NOTE_KEYS_SNAPSHOT src/test` finds it.

Run: `./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.PowerKindsCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite distantlands.gameplay.cards.CardCatalogSuite"`
Expected: PASS. A suite that lists holding cards or implemented When
Played powers by name and fails names The Gathering: move it there.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala src/main/scala/distantlands/gameplay/TurnOrder.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringSuite.scala
git commit -m "$(cat <<'EOF'
feat(cards): The Gathering asks each player to come

Its When Played power asks each player whose pawn is elsewhere, in the
next round's order, whether to put their pawn on its site, and moves it
without Travel (catalog batch 12, rulings 1 and 2). One line names who
gathered. The deals follow in the next commit.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

`git add` of the deleted file stages its deletion.

---

### Task 4: The Gathering's deals

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala`, `ModifierChoice.scala`, `ModifierOffers.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/GatheringDeals.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`
- Modify: `src/test/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedHarness.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringDealsSuite.scala` (new)

**Interfaces:**
- Consumes: Tasks 1 to 3; slice 6a's `DealWalk`, `NegotiationDealTree`,
  `DealRules(secretsOpen)`.
- Produces:
  - `PowerCtx.useRestrictions: Vector[PowerUseRestriction] =
    Vector.empty`, last.
  - `DealWalk` gains, last, `bystanders: (ReadyGame, PendingTree) =>
    Vector[PlayerId] = (_, _) => Vector.empty` and `passed: PendingTree
    => Vector[PlayerId] = _ => Vector.empty`.
  - `NegotiationProcedure.negotiatedBy: NoteKey` (`negotiated-by`).
  - `GatheringDeals.Prefix` (`"gathering"`), `openDecisionId`
    (`"gathering.open"`), `dealPrefix(n: Int): String`,
    `isGathering(decisionId: String): Boolean`, `passed(answered:
    Vector[Answered]): Set[PlayerId]`, `loop(ctx: PowerCtx, site: SiteId,
    order: Vector[PlayerId]): Operation`.
  - Harness: `powersBeside(power, others*): WalkerPowers`,
    `ParkedAt(at, tree, question, events)`, `walkAnsweringAs(ready,
    powers, hook, answers: (PlayerId, DecisionAnswer)*): Walked`,
    `parkAfterAs(...): ParkedAt`.

- [ ] **Step 1: The harness**

In `WhenPlayedHarness`, after `playBeside`:

```scala
  /** `power` registered beside `others`, with every production note key
    * declared: a test that needs another power's rule, such as City
    * Wall's, names it here. */
  def powersBeside(power: ContributingPower,
      others: ContributingPower*): WalkerPowers =
    WalkerPowers(power +: others.toVector,
      restrictionSet = OperationRestrictions.forCatalog(catalog),
      declared = PowerRules.declared)
```

after `walkShuffling`:

```scala
  /** Where a walk stands once it parks with no answer left: the state it
    * reached, its parked tree and question, and its events so far. */
  final case class ParkedAt(at: ReadyGame, tree: PendingTree,
      question: Decide, events: Vector[OathEvent])

  /** Fires `hook` under `powers` and answers each park with the next of
    * `answers`, as the player named beside it, so a co-owned decision is
    * answered by the owner the test names. `Left` is where the walk parks
    * once the answers run out; `Right` is the finished walk, which fails
    * when answers are left over. */
  private def driveAs(ready: ReadyGame, powers: WalkerPowers,
      hook: CardPlayedFaceup, answers: Seq[(PlayerId, DecisionAnswer)])(
      using munit.Location): Either[ParkedAt, Walked] =
    def go(outcome: WalkerOutcome, events: Vector[OathEvent],
        asked: Vector[Decide], left: List[(PlayerId, DecisionAnswer)])
        : Either[ParkedAt, Walked] = outcome match
      case WalkerOutcome.Finished(end, more) =>
        if left.nonEmpty then munit.Assertions.fail(
          s"${left.size} answers were left over")
        Right(Walked(asked, events ++ more, end))
      case WalkerOutcome.Parked(tree, more) =>
        val reached = events ++ more
        val at = reachedFrom(ready, reached)
        val parked = present(ProcedureWalker.parkedDecide(at, hook, tree,
          powers), "the parked question")
        left match
          case Nil => Left(ParkedAt(at, tree, parked, reached))
          case (by, next) :: rest =>
            go(accepted(ProcedureWalker.resolve(at, hook, tree,
              Answered(parked.decisionId, next, by), powers,
              WalkerDice.unavailable)), reached, asked :+ parked, rest)
    go(accepted(ProcedureWalker.advance(ready, hook, None, powers,
      WalkerDice.unavailable)), Vector.empty, Vector.empty, answers.toList)

  /** `driveAs` to the walk's end. */
  def walkAnsweringAs(ready: ReadyGame, powers: WalkerPowers,
      hook: CardPlayedFaceup, answers: (PlayerId, DecisionAnswer)*)(
      using munit.Location): Walked =
    driveAs(ready, powers, hook, answers).fold(parked =>
      munit.Assertions.fail(s"the walk parked at " +
        s"${parked.question.decisionId} with no answer left"), identity)

  /** `driveAs` to the park after the last answer. */
  def parkAfterAs(ready: ReadyGame, powers: WalkerPowers,
      hook: CardPlayedFaceup, answers: (PlayerId, DecisionAnswer)*)(
      using munit.Location): ParkedAt =
    driveAs(ready, powers, hook, answers).fold(identity, _ =>
      munit.Assertions.fail("the walk finished where it should have parked"))
```

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringDealsSuite.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.negotiation.{NegotiationProcedure,
  TheTribunalCard, TravelingNegotiatorCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.poweruse.{CityWall, CityWallCard}
import distantlands.gameplay.walker.{ProcedureWalker, WalkerDice}
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** The Gathering's deals (catalog batch 12, rulings 3 to 5 and 32): the
  * open question, passes, and each deal under its own prefix. The card
  * stands at `homeSite`. */
class TheGatheringDealsSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[TheGathering.type]
  private val gathering = power.cardId
  private val played = hookAt(gathering, homeSite)
  private val alone = powers(power)
  private val birdsong: WorldCardId = BirdsongCard.id

  /** All three pawns at `homeSite` with the card: nobody is asked to
    * come. */
  private def together: Table = p1AtHomeSite.denizen(gathering, at = homeSite)
    .pawn(p2, homeSite).pawn(p3, homeSite)

  /** p1 and p2 there, p3 at `awaySite`, who is asked to come first. */
  private def p3Away: Table = p1AtHomeSite.denizen(gathering, at = homeSite)
    .pawn(p2, homeSite).pawn(p3, awaySite)

  private def open(players: PlayerId*): DecisionAnswer =
    DecisionAnswer.OpenDeal(players.toVector)
  private val pass = DecisionAnswer.PassGathering
  private val unpass = DecisionAnswer.UnpassGathering
  private val accept = DecisionAnswer.AcceptDeal
  private val decline = DecisionAnswer.DeclineDeal
  private val stay = DecisionAnswer.ChooseOneAnswer(TheGathering.stay)

  private def giving(to: PlayerId, favor: Int = 0, secrets: Int = 0,
      advisers: Vector[WorldCardId] = Vector.empty): DecisionAnswer =
    DecisionAnswer.ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(
      to, favor, Vector.empty, advisers, secrets = secrets))))

  private def openQuestions(walked: Walked): Vector[DecisionQuery.Gathering] =
    walked.asked.map(_.query).collect { case q: DecisionQuery.Gathering => q }

  private def openQuestion(at: ParkedAt)(using munit.Location)
      : DecisionQuery.Gathering = at.question.query match
    case question: DecisionQuery.Gathering => question
    case other => fail(s"expected the open question, got $other")

  private def deal(at: ParkedAt)(using munit.Location)
      : DecisionQuery.Negotiate = at.question.query match
    case question: DecisionQuery.Negotiate => question
    case other => fail(s"expected a deal, got $other")

  /** Whether `by`'s `answer` to the question `at` parks on is refused. */
  private def refused(at: ParkedAt, by: PlayerId, answer: DecisionAnswer,
      walkPowers: WalkerPowers = alone): Boolean =
    ProcedureWalker.resolve(at.at, played, at.tree,
      Answered(at.question.decisionId, answer, by), walkPowers,
      WalkerDice.unavailable).isLeft

  /** The Negotiation's lines among `events`, as plain sentences. */
  private def negotiationLines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(ProcedureNotes.id(ActionRef.Negotiation),
      NegotiationProcedure.noteKeys, events).map(_.text)

  test("two deals in one Gathering keep their answers apart"):
    val ready = together.favor(p1, 2).ready
    val walked = walkAnsweringAs(ready, alone, played,
      p1 -> open(p2), p1 -> giving(p2, favor = 1), p2 -> accept,
      p1 -> accept, p2 -> open(p3), p3 -> decline,
      p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(walked.asked.map(_.decisionId), Vector("gathering.open",
      "gathering.1.deal", "gathering.1.deal", "gathering.1.deal",
      "gathering.open", "gathering.2.deal",
      "gathering.open", "gathering.open", "gathering.open"))
    assertEquals(Look(walked.end).favor(p2), Look(ready).favor(p2) + 1)
    // The actor's deal reads as the Negotiate action's; p2's, declined,
    // names who ended it.
    assertEquals(negotiationLines(walked.events), Vector(
      "Negotiated with p2", "p1 gave 1 favor to p2",
      "Negotiation ended by p3"))
    assertJournalRebuilds(ready, walked.events, walked.end)

  test("a deal another player opens names its opener"):
    val walked = walkAnsweringAs(together.favor(p2, 1).ready, alone, played,
      p2 -> open(p1), p2 -> giving(p1, favor = 1), p1 -> accept,
      p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(negotiationLines(walked.events).take(2), Vector(
      "p2 negotiated with p1", "p2 gave 1 favor to p1"))

  test("a pass can be taken back, and a passed player named in a deal " +
      "keeps their pass"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p2 -> pass, p2 -> unpass, p2 -> pass, p1 -> open(p2), p2 -> decline,
      p3 -> pass, p1 -> pass)
    assertEquals(openQuestions(walked).map(_.passed), Vector(Vector(),
      Vector(p2), Vector(), Vector(p2), Vector(p2), Vector(p2, p3)))

  test("opening a deal clears the opener's pass, and only theirs"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p1 -> pass, p2 -> pass, p1 -> open(p2), p2 -> decline,
      p1 -> pass, p3 -> pass)
    assertEquals(openQuestions(walked).map(_.passed), Vector(Vector(),
      Vector(p1), Vector(p1, p2), Vector(p2), Vector(p1, p2)))

  test("a bystander may pass during a deal, and a participant may not"):
    val at = parkAfterAs(together.ready, alone, played, p1 -> open(p2),
      p3 -> pass)
    assertEquals(at.question.decisionId, "gathering.1.deal")
    assertEquals(deal(at).participants, Vector(p1, p2))
    assertEquals(deal(at).bystanders, Vector(p3))
    assertEquals(deal(at).passed, Vector(p3))
    assertEquals(at.question.owners, Vector(p1, p2, p3))
    assert(refused(at, p2, pass))

  test("the Gathering ends once every player at the site has passed"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(walked.asked.map(_.decisionId),
      Vector.fill(3)("gathering.open"))
    assertEquals(negotiationLines(walked.events), Vector.empty)

  test("a secret and an adviser change hands"):
    val ready = together.secrets(p2, faceUp = 1).adviser(p2, BirdsongCard)
      .ready
    val walked = walkAnsweringAs(ready, alone, played, p1 -> open(p2),
      p2 -> giving(p1, secrets = 1, advisers = Vector(birdsong)),
      p1 -> accept, p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    val (before, end) = (Look(ready), Look(walked.end))
    assertEquals(end.faceUpSecrets(p1), before.faceUpSecrets(p1) + 1)
    assertEquals(end.faceUpSecrets(p2), 0)
    assert(walked.end.game.current.player(p1).get.advisers
      .exists(_.id == birdsong))
    assertEquals(negotiationLines(walked.events).slice(1, 3), Vector(
      "p2 gave 1 secret to p1", s"p2 gave ${birdsong.value} to p1"))

  test("a disclosure settles, and the Grand Scepter's Citizenship offer " +
      "is open"):
    val ready = together.imperial(p1).relic(p1, TheGrandScepterCard)
      .boardWarbands(p2, 2).allowingCitizenshipPowers
      .adviser(p2, BirdsongCard, facedown = true).ready
    val at = parkAfterAs(ready, alone, played, p1 -> open(p2))
    assertEquals(deal(at).bounds(p1).citizenship, Vector(p2))
    val walked = walkAnsweringAs(ready, alone, played, p1 -> open(p2),
      p2 -> DecisionAnswer.ProposeTerms(NegotiationTerms(Vector.empty,
        Vector(NegotiationDisclosure(p1,
          NegotiationDisclosureRef.Adviser(p2, birdsong))))),
      p1 -> accept, p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    assert(negotiationLines(walked.events)
      .contains(s"p2 showed p1 ${birdsong.value}"),
      negotiationLines(walked.events))

  test("The Tribunal at the site opens binding promises, and they are " +
      "closed without it"):
    val tribunal = together.edifice(TheTribunalCard, EdificeSide.Intact,
      at = homeSite).ready
    val opened = parkAfterAs(tribunal, alone, played, p1 -> open(p2))
    assert(deal(opened).bounds.values.forall(_.bindingPromises),
      deal(opened).bounds)
    val closed = parkAfterAs(together.ready, alone, played, p1 -> open(p2))
    assert(deal(closed).bounds.values.forall(!_.bindingPromises),
      deal(closed).bounds)

  test("a player elsewhere is named through Traveling Negotiator, and " +
      "refused without it or under City Wall"):
    val reaching = p3Away.adviser(p3, TravelingNegotiatorCard.id).ready
    val asked = parkAfterAs(reaching, alone, played, p3 -> stay)
    assertEquals(openQuestion(asked).present, Vector(p1, p2))
    assertEquals(openQuestion(asked).reach(p1), Vector(p2, p3))
    val dealt = parkAfterAs(reaching, alone, played, p3 -> stay,
      p1 -> open(p3))
    assertEquals(deal(dealt).participants, Vector(p1, p3))
    assertEquals(deal(dealt).bystanders, Vector(p2))
    // Without the card, p3 is out of reach.
    val unreached = parkAfterAs(p3Away.ready, alone, played, p3 -> stay)
    assertEquals(openQuestion(unreached).reach(p1), Vector(p2))
    assert(refused(unreached, p1, open(p3)))
    // City Wall, ruled by p2 at p3's site, stops p3's use of the card
    // (rulings 24 and 25), as the walk's restrictions judge it.
    val walledPowers = powersBeside(power, CityWall)
    val walled = p3Away.adviser(p3, TravelingNegotiatorCard.id)
      .denizen(CityWallCard.id, at = awaySite).warbandsAt(awaySite, p2, 1)
      .ready
    val blocked = parkAfterAs(walled, walledPowers, played, p3 -> stay)
    assertEquals(openQuestion(blocked).reach(p1), Vector(p2))
```

If a `Table` step refuses a placement for a site's capacity (the
Gathering, The Tribunal or City Wall), move that card or the test's
pawns to another site in play and keep the rest of the test.

- [ ] **Step 3: Run them to make sure they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.TheGatheringDealsSuite"`
Expected: FAIL: every test finds no open question (the walk finishes
after the pawns), and the harness reports answers left over.

- [ ] **Step 4: Hand the restrictions to a power's context**

In `ContributingPower.scala`, `PowerCtx`'s last field becomes:

```scala
    answered: Vector[Answered] = Vector.empty,
    /** The walk's power-use restrictions (City Wall, Giant Python, Bandit
      * Forest), for an effect that judges another power's use as it runs:
      * The Gathering reads its reach and its deals' rules under them
      * (catalog batch 12, slice 6b). Empty outside a walk. */
    useRestrictions: Vector[PowerUseRestriction] = Vector.empty
```

`PowerUseRestriction` is in the same package. In `WalkerPowerGather`,
pass `powers.useRestrictions` as the last argument of each
`PowerCtx(...)` it builds (three: the window fold's `ctxFor`, the option
fold's and the restriction walk's); in `ModifierChoice` pass
`pass.powers.useRestrictions`, and in `ModifierOffers`
`powers.useRestrictions`.

- [ ] **Step 5: Bystanders in the deal walk, and the opener's line**

In `NegotiationDealTree.scala`, `DealWalk` becomes:

```scala
/** How one deal walks (catalog batch 12, slice 6a): the prefix of its
  * decision ids, the player who owns its decision, and, as the answers so
  * far give them, its participants, its rules and the line that opens a
  * deal nobody declined. The Negotiate action walks one under
  * `NegotiationDeal.NegotiationPrefix`; The Gathering walks one per deal
  * (slice 6b), with `bystanders`, its players at the site outside the
  * deal, who co-own the decision only to pass, and `passed`, who has
  * passed. */
final case class DealWalk(prefix: String, owner: PlayerId,
    participants: (ReadyGame, PendingTree) => Vector[PlayerId],
    rules: Vector[PlayerId] => DealRules,
    opened: PendingTree => LogNote,
    bystanders: (ReadyGame, PendingTree) => Vector[PlayerId] =
      (_, _) => Vector.empty,
    passed: PendingTree => Vector[PlayerId] = _ => Vector.empty)
```

and the deal decision's `Branch` in `apply` becomes:

```scala
      Branch((ready, pending) => {
        val now = deal(walk, ready, pending)
        val bystanders = walk.bystanders(ready, pending)
        Vector(Decide(NegotiationDeal.dealId(walk.prefix), walk.owner,
          NegotiationDeal.snapshot(catalog, ready, now,
            walk.rules(now.participants), limits).copy(
            bystanders = bystanders,
            passed = walk.passed(pending).filter(bystanders.contains)),
          coOwners = (now.participants ++ bystanders)
            .filter(_ != walk.owner)))
      })),
```

In `NegotiationProcedure`, after `negotiatedPrivately`:

```scala
  /** "p2 negotiated with p1": a deal another player than the actor opened
    * in The Gathering (catalog batch 12, slice 6b). */
  val negotiatedBy: NoteKey = notes.key("negotiated-by", Vector(
    NotePart.Arg(0), NotePart.Text(" negotiated with "), NotePart.Arg(1)))
```

- [ ] **Step 6: The loop**

Create `src/main/scala/distantlands/gameplay/powers/whenplayed/GatheringDeals.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.negotiation.{DealRules, DealWalk,
  NegotiationDeal, NegotiationDealTree, NegotiationProcedure,
  NegotiationReach, NegotiationSites}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.AdviserLimit
import distantlands.model._
import distantlands.model.DecisionAnswer.{OpenDeal, PassGathering,
  UnpassGathering}

/** The Gathering's deals (catalog batch 12, rulings 3 to 5 and 32), after
  * the pawns:
  *
  * {{{
  * Repeat(two or more at the site, one of them not passed) {
  *   Branch -> Decide(gathering.open, co-owned by the players at the site)
  *   Branch(latest open answer is OpenDeal ->
  *     NegotiationDealTree under gathering.<n>, n counting the deals)
  * }
  * }}}
  *
  * The open question keeps one id, so the answer it collects never renames
  * the decision the walk parked at; each deal's answers live under its own
  * prefix, which the deal's `Branch` computes after the answer. Passes are
  * folded from every answer under `gathering.`: a Pass adds its player, and
  * an Unpass or an opened deal removes them (ruling 32).
  *
  * Nothing is stored. The reach and each deal's rules are read live, under
  * the walk's power-use restrictions (`PowerCtx.useRestrictions`): no state
  * changes while the question or a deal is parked, and once a deal settles
  * its walk reads no rule but `privateDeal`, which a Gathering never sets.
  */
object GatheringDeals:
  val Prefix: String = "gathering"
  val openDecisionId: String = s"$Prefix.open"

  /** The prefix of the `n`-th deal, from 1. */
  def dealPrefix(n: Int): String = s"$Prefix.$n"

  /** Whether `decisionId` is a Gathering's: its open question or one of its
    * deals' decisions. */
  def isGathering(decisionId: String): Boolean =
    decisionId.startsWith(s"$Prefix.")

  /** Who has passed, folded from every Gathering answer in order. */
  def passed(answered: Vector[Answered]): Set[PlayerId] =
    answered.filter(answer => isGathering(answer.decisionId))
      .foldLeft(Set.empty[PlayerId]):
        case (done, Answered(_, PassGathering, by)) => done + by
        case (done, Answered(_, UnpassGathering, by)) => done - by
        case (done, Answered(_, OpenDeal(_), by)) => done - by
        case (done, _) => done

  def loop(ctx: PowerCtx, site: SiteId, order: Vector[PlayerId]): Operation =
    Repeat((live, pending) => going(live, site, order, pending.answered),
      Sequence(Vector[Operation](
        Branch((live, pending) =>
          Vector(ask(ctx, live, site, order, pending.answered))),
        Branch((live, pending) =>
          dealt(ctx, live, site, order, pending.answered)))))

  /** Two or more players at the site, and one of them has not passed. */
  private def going(ready: ReadyGame, site: SiteId, order: Vector[PlayerId],
      answered: Vector[Answered]): Boolean =
    val here = TheGathering.present(ready, site, order)
    val out = passed(answered)
    here.size >= 2 && !here.forall(out)

  /** The open question: the actor owns it when at the site, else the first
    * player there; each player's reach is the Negotiate action's, from the
    * site, under the walk's restrictions (ruling 5). */
  private def ask(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], answered: Vector[Answered]): Decide =
    val here = TheGathering.present(ready, site, order)
    val out = passed(answered)
    val owner = if here.contains(ctx.actor) then ctx.actor else here.head
    val name = ctx.catalog.site(site).fold(site.value)(_.name)
    Decide(openDecisionId, owner, DecisionQuery.Gathering(here,
      here.map(player => player -> NegotiationDeal.eligible(ctx.catalog,
        ready, player, ctx.useRestrictions)).toMap,
      here.filter(out), heading = Some(s"The Gathering at $name")),
      coOwners = here.filter(_ != owner))

  /** The deal the latest open answer opened, if it opened one. */
  private def dealt(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], answered: Vector[Answered])
      : Vector[Operation] =
    answered.reverse.find(_.decisionId == openDecisionId) match
      case Some(Answered(_, OpenDeal(named), opener)) =>
        val n = answered.count {
          case Answered(`openDecisionId`, OpenDeal(_), _) => true
          case _ => false
        }
        NegotiationDealTree(ctx.catalog, AdviserLimit, walk(ctx, ready, site,
          order, dealPrefix(n), opener, named))
      case _ => Vector.empty

  private def walk(ctx: PowerCtx, ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId], prefix: String, opener: PlayerId,
      named: Vector[PlayerId]): DealWalk =
    DealWalk(prefix, opener,
      participants = (now, _) => opener +: now.game.current.players
        .map(_.player).filter(player => player != opener &&
          named.contains(player)),
      rules = participants => dealRules(ctx, ready, participants),
      opened = _ =>
        if opener == ctx.actor then
          NegotiationProcedure.negotiated.sourceless(NoteArg.Players(named))
        else NegotiationProcedure.negotiatedBy.sourceless(
          NoteArg.Player(opener), NoteArg.Players(named)),
      bystanders = (now, _) => TheGathering.present(now, site, order)
        .filter(player => player != opener && !named.contains(player)),
      passed = pending => passed(pending.answered).toVector)

  /** A Gathering deal's rules (ruling 5): every printed kind between any
    * pair, sites under Deed Writer, and binding promises when a
    * participant may use The Tribunal, both judged under the walk's
    * power-use restrictions. */
  private def dealRules(ctx: PowerCtx, ready: ReadyGame,
      participants: Vector[PlayerId]): DealRules =
    DealRules(advisersOpen = true, secretsOpen = true,
      bindingPromises = NegotiationReach.tribunalUsers(ctx.catalog, ready,
        participants, ctx.useRestrictions).nonEmpty,
      siteTraders = NegotiationSites.deedWriterUsers(ctx.catalog, ready,
        participants, ctx.useRestrictions))
```

In `TheGathering.effect`, the `SiteCard` case's result becomes:

```scala
      order.flatMap(pawn(site, name, _)) ++ Vector(
        Note(id, states => Some(line(states.now, site, order))),
        GatheringDeals.loop(ctx, site, order))
```

and the scaladoc's last paragraph before "The site is the hook's" gains:
"Then the deals ([[GatheringDeals]])."

- [ ] **Step 7: Run the tests and refresh the note-key snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.TheGatheringDealsSuite distantlands.gameplay.powers.whenplayed.TheGatheringSuite"`
Expected: PASS.

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
then `git diff src/test/resources/notes/declared-keys.txt`: exactly one
added line, `procedure.negotiation negotiated-by = {0} negotiated with {1}`.

Run: `./sbtw "testOnly *Negotiation* *WhisperingStone* *GameLog* *Walker* *WhenPlayed* *PowerGather*"`
Expected: PASS, with no golden log rewritten: the Negotiate action's
deals have no bystanders, and every other context's restrictions are
read by nobody.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powerresolver/ContributingPower.scala src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala src/main/scala/distantlands/gameplay/walker/ModifierChoice.scala src/main/scala/distantlands/gameplay/walker/ModifierOffers.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDealTree.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala src/main/scala/distantlands/gameplay/powers/whenplayed/GatheringDeals.scala src/main/scala/distantlands/gameplay/powers/whenplayed/TheGathering.scala src/test/resources/notes/declared-keys.txt src/test/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedHarness.scala src/test/scala/distantlands/gameplay/powers/whenplayed/TheGatheringDealsSuite.scala
git commit -m "$(cat <<'EOF'
feat(cards): The Gathering's deals

After the pawns, every player at the site may open a Negotiation deal,
pass, or take back a pass, until all have passed (catalog batch 12,
rulings 3 to 5 and 32). Each deal walks under its own prefix with
every printed kind open, and reads its reach and rules under the walk's
power-use restrictions, which a power's context now carries.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: The Game Log and what each viewer sees

**Files:**
- Modify: `src/test/scala/distantlands/testkit/SituationDriver.scala`
- Modify: `src/test/scala/distantlands/testkit/Situation.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`
- Create: `src/test/resources/gamelog/the-gathering.actor.log`, `the-gathering.other.log`
- Test: `src/test/scala/distantlands/application/GatheringProjectionSuite.scala` (new)

**Interfaces:**
- Consumes: Tasks 1 to 4.
- Produces: `Situation.parkedAt(decisionId: String)(steps*)`; the log
  script `theGathering`.

- [ ] **Step 1: Stop a situation at a decision**

In `SituationDriver.run`, add a last parameter `stopAt: Option[String] =
None`, and pass it to both `answerParks` calls. `answerParks` takes
`stopAt: Option[String]` after `failed`, and both of its
`.filterNot(settled)` become `.filterNot(node => settled(node) ||
stops(node, stopAt))`, with:

```scala
  /** Whether `node` is the decision a test asked the walk to stop at. */
  private def stops(node: ParkedNode, stopAt: Option[String]): Boolean =
    node match
      case ParkedNode.Decision(_, decide, _) =>
        stopAt.contains(decide.decisionId)
      case _ => false
```

Add to the trait's doc comment on `run`: "With `stopAt`, a park at that
decision is left in place, as a phase menu is."

In `Situation`, after `parkedAfter`:

```scala
  /** Issues each step and answers every park it causes until the walk
    * parks at `decisionId`, which is left for the test: a co-owned
    * decision the test answers as another owner, say. A walk that never
    * parks there settles as `after` would. */
  def parkedAt(decisionId: String)(steps: (GameCommand | Step)*)(
      using munit.Location): Situation =
    driver.run(this, steps.map(Step.of), settle = true,
      stopAt = Some(decisionId))
```

- [ ] **Step 2: The script**

In `NegotiationLogScripts.scala`, add the imports
`distantlands.gameplay.actions.cardplay.CardPlayProcedure`,
`distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs`,
`distantlands.gameplay.powers.whenplayed.{GatheringDeals, TheGathering,
TheGatheringCard}`, `distantlands.testkit.PlacementPicks`, and
`ChooseOneAnswer`, `OpenDeal`, `PassGathering` beside the other
`DecisionAnswer` imports; name the new script in the object's doc comment
("and The Gathering"); and add:

```scala
  /** p1 Searches The Gathering off the world deck and plays it at Ancient
    * City. p2 comes from Broken Peaks; p3 stays at Buried Giant. p2 opens
    * the one deal: p2 gives p1 a secret, p1 gives p2 a favor, and both
    * accept. Then both pass (catalog batch 12, slice 6b). */
  def theGathering(using munit.Location): Script =
    val (service, act) = atTable("the-gathering", tableWithOnePawnPerSite
      .worldDeckTop(TheGatheringCard).secrets(p2, faceUp = 1).favor(p1, 1)
      .banditsRefilled)
    val dealId = NegotiationDeal.dealId(GatheringDeals.dealPrefix(1))
    def say(by: PlayerId, answer: DecisionAnswer): GameCommand =
      Commands.resolveWalker(by, TreeDecision(dealId, answer))
    def open(by: PlayerId, answer: DecisionAnswer): GameCommand =
      Commands.resolveWalker(by, TreeDecision(GatheringDeals.openDecisionId,
        answer))
    act.withAnswers {
      case park if park.decisionId ==
          CardPlayProcedure.placementDecisionId(TheGatheringCard.id) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, PlayedAs.AtSite))
      case park if park.decisionId == TheGathering.pawnDecisionId =>
        ChooseOneAnswer(if park.awaiting == p2 then TheGathering.join
          else TheGathering.stay)
    }.parkedAt(GatheringDeals.openDecisionId)(
      MenuPlay.start(p1, ActionRef.Search))
      .parkedAfter(open(p2, OpenDeal(Vector(p1))))
      .parkedAfter(say(p2, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p1, 0, Vector.empty, secrets = 1))))))
      .parkedAfter(say(p1, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p2, 1, Vector.empty))))))
      .parkedAfter(say(p2, AcceptDeal))
      .parkedAfter(say(p1, AcceptDeal))
      .parkedAfter(open(p1, PassGathering))
      .after(open(p2, PassGathering))
    Script("the-gathering", service, p1)
```

The Search keeps its first drawn card by default, which is The Gathering
on top of the deck, and discards the rest. If Ancient City is full, the
placement offers no `AtSite` option and `PlacementPicks.pick` fails:
then move p1's pawn to a site with room (`.pawn(p1, at = …)` after
`tableWithOnePawnPerSite`), and update the doc comment and Step 4's
expected lines to name that site.

In `LogScripts.scala`, add `theGathering` to the `NegotiationLogScripts`
export and `"the-gathering" -> (() => theGathering),` after
`"negotiation-whispered"`.

- [ ] **Step 3: Write the failing log tests**

Append to `GameLogExchangeSuite`:

```scala
  test("a Gathering names who gathered, and a deal another player opens " +
      "names its opener"):
    val script = theGathering
    val all = lines(script)
    val (actor, opener) = (playerLabel(p1), playerLabel(p2))
    assert(all.exists(line => line.startsWith("Gathered at ") &&
      line.endsWith(s": $actor and $opener.")), all)
    assert(all.contains(s"$opener negotiated with $actor"), all)
    assert(all.contains(s"$opener gave 1 secret to $actor"), all)
    assert(all.contains(s"$actor gave 1 favor to $opener"), all)
    // The pawn question's answer is told by the Gathered line.
    assert(!all.exists(_.contains(" chose ")), all)
```

Run: `./sbtw "testOnly distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: PASS, since Tasks 1 to 4 implement the lines. A failure names
the lines read: fix the power, not the test.

- [ ] **Step 4: The golden logs**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`

Review both new files line by line:

- `the-gathering.actor.log` (p1) has, in one `search` run: the Search
  lines, the card-play line placing `[card:27|The Gathering]` at Ancient
  City, "Gathered at [site:…|Ancient City]: [player:p1|p1] and
  [player:p2|p2].", "[player:p2|p2] negotiated with [player:p1|p1]",
  "[player:p2|p2] gave 1 secret to [player:p1|p1]" and
  "[player:p1|p1] gave 1 favor to [player:p2|p2]", in that order, and no
  "chose" line for the pawn questions.
- `the-gathering.other.log` has the same lines with the same
  `(sequence, ordinal)` keys.

Then confirm `git status --short src/test/resources/gamelog` lists only
the two new files. A change to any other golden is a regression: revert
it and fix its cause.

- [ ] **Step 5: Write the projection tests**

Create `src/test/scala/distantlands/application/GatheringProjectionSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.whenplayed.{GatheringDeals, TheGathering,
  TheGatheringCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, OpenDeal}
import distantlands.protocol.projection.{DecisionQueryProjection,
  GameProjection, NegotiationDealProjection}
import distantlands.testkit.{Commands, MenuPlay, PlacementPicks, Situation,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** What each viewer of a Gathering is shown: the open question, and a
  * deal with a bystander (catalog batch 12, slice 6b). */
class GatheringProjectionSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  /** p1's Search plays The Gathering at Ancient City; p2 and p3 come.
    * Parked at the open question. */
  private def atOpen(): Situation =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).pawn(p3, BuriedGiantSite)
      .worldDeckTop(TheGatheringCard).banditsRefilled
    val (service, repository) = table.service()
    table.situation(Situation.journaled(service, catalog, repository,
      "gathering")).withAnswers {
      case park if park.decisionId ==
          CardPlayProcedure.placementDecisionId(TheGatheringCard.id) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, PlayedAs.AtSite))
      case park if park.decisionId == TheGathering.pawnDecisionId =>
        ChooseOneAnswer(TheGathering.join)
    }.parkedAt(GatheringDeals.openDecisionId)(
      MenuPlay.start(p1, ActionRef.Search))

  private def view(situation: Situation, viewer: PlayerId): GameProjection =
    projector.project("gathering", LoadedGame(situation.state,
      situation.nextSequence), viewer)

  private def openQuestion(projection: GameProjection)
      : DecisionQueryProjection.Gathering =
    projection.walkerDecision.flatMap(_.query).collect {
      case gathering: DecisionQueryProjection.Gathering => gathering
    }.getOrElse(fail("the open question must be projected"))

  private def deal(projection: GameProjection): NegotiationDealProjection =
    projection.walkerDecision.flatMap(_.query).collect {
      case negotiate: DecisionQueryProjection.Negotiate => negotiate.deal
    }.getOrElse(fail("the deal must be projected"))

  test("each player at the site is asked the open question with their own " +
      "reach"):
    val parked = atOpen()
    Vector(p1 -> Vector("p2", "p3"), p2 -> Vector("p1", "p3"),
      p3 -> Vector("p1", "p2")).foreach { (viewer, reach) =>
      val question = openQuestion(view(parked, viewer))
      assertEquals(question.presentPlayerIds, Vector("p1", "p2", "p3"))
      assertEquals(question.reachablePlayerIds, reach, viewer.value)
      assertEquals(question.passedPlayerIds, Vector.empty)
    }

  test("a bystander is asked the deal with no editor, and its parties " +
      "with one"):
    val dealt = atOpen().parkedAfter(Commands.resolveWalker(p1,
      TreeDecision(GatheringDeals.openDecisionId, OpenDeal(Vector(p2)))))
    val watching = deal(view(dealt, p3))
    assertEquals(watching.participantPlayerIds, Vector("p1", "p2"))
    assertEquals(watching.bystanderPlayerIds, Vector("p3"))
    assertEquals(watching.editing, None)
    assert(deal(view(dealt, p2)).editing.nonEmpty)
```

Use the import that `NegotiationLogScripts` uses for `TreeDecision` if
it is not `distantlands.application.TreeDecision`.

Run: `./sbtw "testOnly distantlands.application.GatheringProjectionSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/test/scala/distantlands/testkit/SituationDriver.scala src/test/scala/distantlands/testkit/Situation.scala src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala src/test/scala/distantlands/application/gamelog/LogScripts.scala src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala src/test/resources/gamelog/the-gathering.actor.log src/test/resources/gamelog/the-gathering.other.log src/test/scala/distantlands/application/GatheringProjectionSuite.scala
git commit -m "$(cat <<'EOF'
test(gamelog): log a Gathering and project its questions

A Search plays The Gathering, p2 comes and opens a deal that trades a
secret for a favor, and both pass. Each player at the site is asked the
open question with their own reach, and a bystander sees the deal
without its editor (catalog batch 12, slice 6b).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Docs and verification

**Files:**
- Modify: `docs/architecture/all-exile-negotiation.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md`

- [ ] **Step 1: The architecture doc**

In `docs/architecture/all-exile-negotiation.md`, after the "Private
deals" section, add:

~~~markdown
## The Gathering

The Gathering's When Played power (`gameplay/powers/whenplayed/`) asks
each player whose pawn is elsewhere, in the next round's order, whether
to put it on the card's site, and moves a yes with a plain `Move`. Then
`GatheringDeals` loops one open question, `gathering.open`, co-owned by
every player at the site:

```
Repeat(two or more at the site, one of them not passed) {
  Branch -> Decide(gathering.open)          -- OpenDeal, PassGathering, UnpassGathering
  Branch(OpenDeal -> NegotiationDealTree under gathering.<n>)
}
```

- Each deal is a `DealWalk` with every printed kind open
  (`advisersOpen`, `secretsOpen`), site terms under Deed Writer and binding
  promises under The Tribunal. Its decision is co-owned by its
  participants and by the players at the site outside it, its
  bystanders, who may only pass or take back a pass.
- The opener may name a player elsewhere whom the Negotiate action's
  reach lets them reach. The reach and each deal's rules are read live,
  under the walk's power-use restrictions (`PowerCtx.useRestrictions`):
  nothing is stored, since no state changes while a question or a deal
  is parked.
- Passes are folded from every answer under `gathering.`; opening a deal
  clears the opener's pass only. The loop ends when every player at the
  site has passed.
~~~

- [ ] **Step 2: The roadmap and the spec**

In `docs/ROADMAP.md`, delete the **Negotiation cards** item of **Phase -
Negotiation terms and promises** and rewrite the phase's paragraph as:

```markdown
A Negotiation deal holds favor, secret, relic, adviser and site
transfers, disclosures, Citizenship offers and promises, and the
Negotiation cards are implemented. Add banner transfers, and secrets and
sites as terms outside the powers that open them, a registry of term
kinds, and binding promises with consequences. Record ignored Negotiation
rules per participant rather than for the actor only.
```

In the spec, the status line becomes:

```markdown
**Status:** designed 2026-11-09. Every slice landed.
```

and the intro's "The roadmap's **Phase - Negotiation terms and
promises** loses its **Negotiation cards** item when slice 6b lands."
becomes "The roadmap's **Phase - Negotiation terms and promises** lost
its **Negotiation cards** item with slice 6b."

- [ ] **Step 3: Verify**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92%.

Run: `./sbtw "frontend/test"` (the frontend suites, if the full run
skipped them)
Expected: PASS.

Run: `python3 scripts/check-architecture.py`
Expected: no violation; in particular `Decisions.scala` and
`ActionProjectionCodec.scala` stay under 800 lines.

Run: `git status --short src/test/resources/gamelog`
Expected: clean (Task 5 committed the two new goldens).

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/all-exile-negotiation.md docs/ROADMAP.md docs/superpowers/specs/2026-11/2026-11-09-catalog-batch-12-design.md
git commit -m "$(cat <<'EOF'
docs(catalog): close catalog batch 12 with The Gathering

The architecture doc describes the Gathering's loop, the roadmap drops
the Negotiation cards item, and the spec records every slice landed.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- **Spec coverage.** Pawns in Chancellor order and from the first player
  (Task 3); no Travel (Task 3); the loop, the open question with Negotiate,
  Pass and Unpass (Tasks 1, 2, 4); the reach beyond the site (Task 4,
  Traveling Negotiator, its absence, City Wall); every printed kind,
  disclosures, the Citizenship offer, binding promises under The Tribunal
  (Task 4); site terms under Deed Writer are the same `DealRules` field as
  the Negotiate action's, read through `NegotiationSites.deedWriterUsers`,
  and slice 4's suites cover the terms; passing during a deal, bystanders
  (Tasks 2, 4); ruling 32 (Task 4); the end once all pass and the skipped
  loop (Tasks 3, 4); the log lines (Tasks 3 to 5); the catalog (Task 3);
  the panels (Task 2); the restrictions question (finding 1, Task 4). The
  spec's test list: every item has a test above, with the Citizenship
  offer checked as open rather than settled, since its settlement is
  slice 1's and the Grand Scepter suites cover it.
- **Placeholders.** None. Two steps tell the executor what to do if a
  site's capacity refuses a fixture's card; neither changes what a test
  asserts.
- **Type consistency.** `OpenDeal(players: Vector[PlayerId])`,
  `PassGathering`, `UnpassGathering`; `DecisionQuery.Gathering(present,
  reach, passed, heading)`; `Negotiate.bystanders`, `passed`;
  `DecisionQueryProjection.Gathering(presentPlayerIds, passedPlayerIds,
  reachablePlayerIds, heading)`; `NegotiationDealProjection
  .bystanderPlayerIds`, `passedPlayerIds`; `Surface.Negotiate(deal,
  editor, passing)`, `Surface.Gathering(decision, query)`;
  `TheGathering.pawnDecisionId`, `join`, `stay`, `present`;
  `GatheringDeals.openDecisionId`, `dealPrefix`, `passed`, `loop`;
  `DealWalk.bystanders`, `passed`; `PowerCtx.useRestrictions`;
  `NegotiationProcedure.negotiatedBy`; `Situation.parkedAt`;
  `walkAnsweringAs`, `parkAfterAs`, `powersBeside`, `ParkedAt`. Each is
  used with the same name and shape in every task that reads it.
