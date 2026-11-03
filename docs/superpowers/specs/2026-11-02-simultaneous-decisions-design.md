# Simultaneous decisions

Status: designed, 2026-11-02.

The walker gains a `Simultaneous` node: several decisions open at once, one per
player, answered in any order. Its first use is the Throne's Offer Citizenship
step, where each offered Exile now answers when they like instead of in seat
order. This is the last item of the Empire phase in `docs/ROADMAP.md`.

The [negotiation walker design](2026-09-19-negotiation-walker-design.md),
"Simultaneous decisions", kept room for this node. This design fills it in.

## Rulings

- **Answers are public and apply at once (user).** Each answer is logged and
  takes effect when it is given. Answers are not hidden until the last one
  arrives. Only the forced seat order goes away. A later answerer may react to
  earlier answers.
- **A real node, not a co-owned decision (user).** A `Repeat` over one co-owned
  `Decide` would have served the Throne with no walker change. The user chose
  the node now, so that lineage setup can use it later, when each player has
  different options.
- **Child shape (user).** Each child is one `Decide` followed by a tail that
  never parks.
- **Search order (user).** The search and the dry run answer the children in
  child order, as in a `Sequence`.
- **Who sees what (user).** A player who still owes an answer sees their own
  decision. Everyone else sees the waiting line. That includes players who
  already answered and the Throne holder. The waiting line names every
  player who still owes an answer, in seat order, with no primary player.

## The node

```scala
final case class Simultaneous(override val children: Vector[Operation])
    extends Operation
```

It lives in `model/Operation.scala`, beside `Sequence`, `Branch` and `Repeat`.

**Well-formedness.** The walk checks these when it reaches the node. A breach
is an `OathViolation.InvalidEventOrder` naming the node's path:

- every child is a `Decide`, or a `Sequence` whose first child is a `Decide`;
- the children's `Decide`s have distinct decision ids and distinct owners;
- no child's `Decide` has co-owners;
- every child's `Decide` has the same heading;
- the node has at least one child.

A node with one child behaves like that child in a `Sequence`.

**Answered children.** A child is answered when `pending.answered` holds an
answer with its `Decide`'s decision id. The walk never reads this from
position, so the arrival order is free.

**Walk.**

- *Fresh walk, or plain resume.* The node runs nothing and parks at its own
  path. Every unanswered child's `Decide` is open.
- *Answer resume.* The cursor ends at the node. The walk:
  1. finds the unanswered child whose `Decide` has `answer.decisionId`, and
     rejects any other id as a stale or wrong answer;
  2. records the answer through `WalkerSteps.answerDecide`, which checks that
     `answer.by` owns the `Decide`;
  3. walks that child's tail at once;
  4. parks again at the node's own path while any child is unanswered, and
     otherwise completes and walks on.
- *Tails.* A tail that parks (a `Decide` or a parked `Roll`) is rejected with
  `InvalidEventOrder`. The tail's operations record under their own child
  paths.
- *Steps.* An answer step records `nodeId` = the node's path. Replay's
  `validateParkedStep` checks that `pending.at` equals the step's `nodeId`,
  so it passes with no change.

**Search.** With `ctx.searching`, the node walks its children in child order,
as a `Sequence` would. The answers are independent, so one order is enough to
find a legal path. The search's `visited` key and its `verdict` resume carry
the decision id beside the path, so children that share the node's path never
collide.

**Power windows.** Each child's `Decide` folds its own window, as anywhere
else. A window on the node itself is not supported, so the node has no
`window` parameter.

**Inside other nodes.** A `Simultaneous` may sit in a `Sequence`, a `Branch`
or a `Repeat`. The Repeat guard counts `answered`, so it sees one answer per
pass.

## Resolving open decisions

`WalkerPowerGather` gains a `Simultaneous` arm in `resolveAt` and
`foldedChildrenAt`. A new `leavesAt` returns every leaf open at a park. For a
`Simultaneous` park these are the unanswered children's `Decide`s, each folded
and narrowed. For any other park it is the one leaf `leafAt` returns today.

`ProcedureWalker.openDecisions` reads `leavesAt`, so it returns one `Decide`
per open child. Its doc comment already promises this.

## Callers

Every reader of the single parked `Decide` moves to one of three forms:

| Form | Callers |
| --- | --- |
| The open `Decide` with decision id *Y* | `OathRulesWalker` consent asks, grants and plan decisions (around lines 191, 215, 267); `PricedOptions.priced`; `ConsentProjector`; `ChoiceWords` |
| Any open decision | `OathRulesWalker.checkAnswerable`; `WalkerSimulation.waitsOnStep` |
| Every open decision | `WalkerSimulation.reached` and `previewParked`; `ModifierChoice.explained` |

- `ProcedureWalker.parkedDecide` is renamed `soleDecide`. It returns `None`
  when more than one `Decide` is open, so no caller picks a child by accident.
  `WalkerDecisionProjector.parkedDecide(ready)` gets the same rename and rule,
  plus a `decideById(ready, id)` for `ConsentProjector` and `ChoiceWords`.
- Tests that call `openDecisions(...).head` stay as they are. Their trees
  hold no `Simultaneous`.
- `ProcedureWalker.awaitedPlayer` (singular) is deleted.
  `awaitedPlayers` returns the owners of the open decisions, or the walk's
  actor for a parked `Roll`. A new `awaitedInSeatOrder` returns them as a
  vector in seat order, for projection.
- Authorization needs nothing new. `answerDecide` already checks the owner of
  the `Decide` it matched.

## Projection and wire

- **Owners.** A viewer who owns an unanswered child gets that child's
  `Decide` as `walkerDecision`, projected as today.
- **Everyone else.** Every other viewer, including the public view, gets
  `walkerWaiting`. A viewer gets one of the two, never both, as today.
- **`WalkerWaitingProjection`.** `playerId` and `coOwnerPlayerIds` are
  replaced by `awaitedPlayerIds: Vector[String]`, in seat order, with no
  primary player. A co-owned `Decide` lists its owners the same way. The
  heading, the deal and the status come from the open decisions. For a
  `Simultaneous` these are the shared heading, no deal, and the status lines
  of each open decision, in seat order of their owners.
- **Codec.** `ActionProjectionCodec` and the DTO round-trip tests in
  `ProjectionProtocolSuite` change with the DTO. Old journals and old clients
  need not decode.
- **Frontend.**
  - `ServerUiSupport` changes `waitingForPlayerId` to `waitingForPlayerIds`.
  - `ActionDecisionRenderer` and the waiting line render "Waiting for p2, p3"
    from it.
  - Highlights that read one awaited player read the set.

## The Throne

`OfferCitizenship.answers` puts one child per offered Exile into a single
`Simultaneous`. The children keep seat order, which the search follows:

```scala
if ordered.isEmpty then Vector.empty
else Vector(offeredNote,
  Simultaneous(ordered.map(exile => new Sequence(answer(exile)))))
```

Each child is the existing `answer(exile)`: the `Decide`, then the `Branch`
that runs `joins` and writes "accepted" or "declined". Those effects and lines
now run in arrival order. `joins` still computes the purple shortfall from the
live bank for each joiner, so two joiners in either order top up the bank
correctly.

## Tests

- **`SimultaneousDecideSuite`**, on a synthetic tree in the style of
  `CoOwnedDecideSuite`:
  - the node parks with every child open;
  - the children may be answered in reverse order;
  - each tail runs right after its answer;
  - the node completes after the last answer and the walk goes on;
  - a second answer to one child is rejected, and so is an unknown id;
  - a non-owner's answer is rejected;
  - a tail that parks is rejected;
  - each well-formedness breach is rejected;
  - replay of a journal answered out of order rebuilds the same state;
  - the search finds a path through the node.
- **Throne suite.** p3 answers before p2. Both answers apply, and the log
  shows them in arrival order.
- **Projection suite.**
  - An Exile who owes an answer sees their decision.
  - An Exile who already answered sees the waiting line.
  - The holder and the public view see the waiting line with every open name.
- **Frontend.** The waiting line names several players.
- **Golden logs.** Throne scripts that answer in seat order keep their lines.
  A new script answers out of order.

## Docs

- `docs/rules/ambiguities.md`, Chronicle ruling 21: offered Exiles answer in
  any order, and each answer applies at once.
- `docs/rules/chronicle.md`, task 2, step 1: the Digital line drops
  "in seat order".
- The negotiation walker design, "Simultaneous decisions": point to this
  design.
- `implementation-traceability.md`: a row for the node and the Throne's use.
- `docs/ROADMAP.md`: delete "Simultaneous Throne answers".

## Out of scope

- Hidden answers revealed together. No step needs them yet.
- Children whose tails park, such as a player's own follow-up decisions. Lineage
  setup may need them later.
- Other uses of the node: lineage setup and simultaneous setup effects.
