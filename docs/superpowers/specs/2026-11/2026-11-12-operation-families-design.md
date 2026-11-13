# Operation families

**Status:** designed 2026-11-12, approved section by section by the owner,
and built by the [operation families plan](../../plans/2026-11/2026-11-12-operation-families.md).
Two throwaway probes (Scala 3.9.0, `-Werror`) confirmed the approach; see
"Probe result", and "What the full build showed" for what they missed.

**Answers** the roadmap's architecture candidate "`CoreOperations.scala` is
at the file limit", measured at the end of the
[Foundations work](2026-11-11-foundations-design.md). Legacies will add
operations, so this lands before the Legacies spec.

## Goal and scope

`CoreOperations.scala` holds Oath's whole operation vocabulary, 796 of the
800 lines `scripts/check-architecture.py` allows. It has been split around
four times already (`7d419e068`, `d4946831d`, `99ed18968`, `e1fb783fd`),
each time by moving helpers out, and each split bought room for a few more
operations. The goal is to remove the cause, not buy more room.

The cause is structural:

1. **The family can only grow in one file.** `CoreOperation` is `sealed`,
   and Scala 3 keeps every direct subtype of a sealed type in that type's
   file. A nested `sealed trait ChronicleOperation extends CoreOperation`
   does not help: it must itself sit in the parent's file, and so must its
   cases.
2. **Each new operation costs lines in three single files, plus a
   dispatcher arm.** `CoreOperations.scala` (796 lines),
   `WalkerOperationCodec.scala` (769) and `WalkerOperationWireSuite.scala`
   (759) grow together, and each sits near the cap.
3. **Most growth is bookkeeping.** The piece vocabulary (`Move`, `Take`,
   `Discard`, `Play`) is stable. Recent additions are state records:
   `FoundationRestored`, `SetFestivalMarker`, `TrackPawn`,
   `LineagePointsScored`, `PlaceAtlasSite`.
4. **The applier can drop an operation silently.**
   `OperationApplication.applyNonMoveLeaves` ends in
   `case (result, _) => result`. An operation added without an apply arm
   does nothing, and the compiler cannot say so. Today only the codec is
   exhaustive.

In scope: splitting the vocabulary into families, an exhaustive applier
dispatch, splitting the codec and its wire suite along the same lines, an
architecture rule, and the docs these touch. Out of scope: see the last
section.

## Approach

Each area of the vocabulary becomes its own sealed trait in its own file,
an **operation family**. `CoreOperation` becomes a type alias for the union
of the families. Scala 3 checks match exhaustiveness through a union of
sealed types, so every match over `CoreOperation` stays exhaustive, and
constructors and extractors keep their names: `FoundationRestored(n)` and
`case FoundationRestored(n)` still compile at all of their call sites.

Two alternatives were weighed and set aside:

- **A wrapper case per family** (`final case class Chronicle(op:
  ChronicleOperation) extends CoreOperation`). Proven Scala, but every
  construction site and nested pattern changes, which is more churn than it
  removes.
- **Exempt the file from the line cap.** One line, but the codec and wire
  suite still hit the cap, the silent no-op stays, and the file grows
  without bound.

## Probe result

A scratch sbt project on Scala 3.9.0 with `-deprecation -feature -Werror`,
deleted after the run, declared an open `OperationBase extends Operation`,
two sealed families in separate files, a control node outside them, and
`type CoreOperation = PieceOperation | ChronicleOperation`.

Compiled clean and behaved as expected at run time:

- a full match over the alias with no wildcard;
- `case c: CoreOperation`, `isInstanceOf[CoreOperation]` and
  `collect { case c: CoreOperation => c }` keep core operations and drop the
  control node;
- a shared member (`simultaneous`) read through the alias;
- `[T <: CoreOperation]` bounds and a `TypeTest[Operation, CoreOperation]`;
- a union sub-group such as the codec's private aliases;
- a per-family dispatch (`case p: PieceOperation => p match ...`).

Refused, as required:

- a new case in a family with no arm: `match may not be exhaustive ... It
  would fail on pattern case: probe.LegacyFlipped(_)`, in both the encoder
  and the per-family applier, an error under `-Werror`;
- a new family added to the alias: every match over `CoreOperation` warns;
- a family left out of the alias: `val x: CoreOperation = Roll(1)` is a
  type mismatch;
- a case declared outside its family's file: `Cannot extend sealed trait
  ChronicleOperation in a different source file`.

One gap: a class that extends `OperationBase` directly, outside any family,
compiles. It is not a `CoreOperation`, so the walker would treat it as an
unknown control node. The architecture rule below closes it.

A second probe, run while planning with the build's `-Wunused` flags as
well, matched over `CoreOperation & LeafOperation` with arms only for the
leaves. It compiled clean, though the families held a composite final
case class, a sealed composite trait with a private final case, and a
sealed composite trait with a nested final case: the checker excludes
every case that is not a `LeafOperation`. Removing a leaf's arm failed the
build with "It would fail on pattern case: probe.GainSupply(_)". A nested
`case p: PieceOperation => p match` inside that match needed only the
leaf arms too, and at run time a type test against the intersection kept
leaves and dropped composites.

### What the full build showed

The probes were small, and building the whole repository showed two
behaviours they had not reached.

- **A mixed-family expression infers `OperationBase`.** Where one
  expression combines operations of two families, such as
  `Vector(Move(...), FlipSecrets(...))`, a match whose arms return a `Move`
  and a `Burn`, or `a ++ b` across families, Scala widens the union of
  their types to the families' common base, `OperationBase`, which is not
  a `CoreOperation`. Seven call sites (six in production, one in a test)
  therefore name the element type: `Vector[CoreOperation](...)`,
  `Option.when[CoreOperation](...)`. Marking the base traits `transparent`
  keeps the union instead, but the type checker then ran for over half an
  hour on the test sources, so they stay ordinary traits.
- **A match that cuts across the families is very slow to check.** The
  old codec matched a `CoreOperation` against its own private unions,
  which group the cases differently from the families (`Burn` sat with the
  pieces, the dice operations with the turn). Against the union of six
  sealed families, that one match's reachability check ran for many
  minutes. The family codecs match each family against its own cases, as
  the applier does, and compile in seconds, so the codec split landed with
  the model split rather than after it.

The repository was checked for what an alias cannot carry. Nothing defines
or calls a `CoreOperation` companion, and no `ClassTag` or `classOf` is
taken of an operation type (a `ClassTag` of a union erases to the common
supertype and would match too widely).

## The families

`CoreOperations.scala` shrinks to about 40 lines: the vocabulary's doc
comment, `trait OperationBase extends Operation` with the shared
`def simultaneous: Boolean = false`, and

```scala
type CoreOperation = PieceOperation | ResourceOperation | CardOperation |
  DiceOperation | TurnOperation | ChronicleOperation
```

`OperationBase` stays open; only the family files extend it. The root file
changes only when a whole family is added.

Each family is a sealed trait in its own file in package
`distantlands.model`, so no `import distantlands.model._` changes. Files
take the trait's singular name, which keeps them apart from the gameplay
objects of similar names (`gameplay.operations.ResourceOperations`).

| File | Operations | Estimated lines |
|---|---|---|
| `PieceOperation.scala` | Move, Take, Give, Exchange, Kill, Replace, Sacrifice | ~140 |
| `ResourceOperation.scala` | SpendSupply, GainSupply, FlipSecrets, Gain, Burn, PayCost, DrawNomadFavor | ~140 |
| `CardOperation.scala` | Peek, Flip, Reveal, Bury, Discard (with DiscardCause), RemoveFromMap, Draw, Play, Swap, Shuffle | ~250 |
| `DiceOperation.scala` | ModifyDicePool, Roll, ModifyRollOutcome, Reroll, SetDieFaces | ~50 |
| `TurnOperation.scala` | EnterPhase, BeginTurn, AdvanceRound, AdvanceVisionsDrawn, UsurperLimiterRemoved, SetOathkeeper, SetOathkeeperGoal, RecordPowerUse, RecordCampaignResult, QueueProcedure, Defer, TakeDeferred, ActAsIf, ActAsIfAdvisers, TrackPawn, ClearTrackedPawn, SetFestivalMarker, OpenPick, ClosePick | ~170 |
| `ChronicleOperation.scala` | ChronicleUpdated, RoleChanged, ChronicleTaskAssigned, GrandScepterFromBox, SuitedReserveFromBox, ImperialWarbandsTaken, WarbandsAdded, LineagePointsScored, EdificeBuilt, FoundationRestored, PlaceAtlasSite | ~70 |

The resource family holds the operations on counted resources and Supply:
favor and secrets in one place, and `Gain`, which also covers warbands.

The families follow the codec's existing groups (`CardOperation`,
`PieceOperation`, `ResourceOperation`, `TurnOperation`,
`ChronicleOperation`), with two changes: dice leaves the turn group, and
`Burn`, `PayCost` and `DrawNomadFavor` leave the piece group for the
resource group. The codec split therefore matches the model one to one.

`TurnOperation` holds both turn-state records and the walk's scratch
(`Defer`, `ActAsIf`, `TrackPawn`, `OpenPick`). They stay together while the
file has room; a split can wait until it grows.

**`PrimitiveOperation` becomes an alias.** It stays as
`type PrimitiveOperation = CoreOperation & LeafOperation` in
`CoreOperations.scala`: Oath's leaves, as its doc comments already say. A
leaf is a family case that also extends `LeafOperation`, which stays as it
is in `Operation.scala`. `Operation.flatten` reads `LeafOperation`, so it
does not change. (Revised while planning: the second probe below showed the
alias is what the applier should match on, so keeping the name saves the
doc churn of deleting it.)

`OperationChildren` stays `private[model]`; the families are all in
`distantlands.model`, so every composite still reaches it.

## The applier

`OperationApplication` keeps its shape: the guard first, then the three
move passes (counted resources, pawns and banners, cards), then every other
leaf.

`applyNonMoveLeaves` hands every `PrimitiveOperation` leaf to a new
`gameplay/operations/LeafApplication.scala`, which dispatches over the six
families with no `case _`: `case l: ResourceOperation =>`,
`case l: TurnOperation =>` and so on. Each family has one exhaustive match
over its leaves (`ResourceOperation & LeafOperation`), whose arms call the
gameplay object that already owns that state. The only wildcard left in
`applyNonMoveLeaves` is for a leaf that is not a `CoreOperation` at all, a
walker control node, which never reaches the applier and which it skips
as it does today.

| Family | Applied by |
|---|---|
| `PieceOperation` | nothing beyond the move passes |
| `ResourceOperation` | `ResourceOperations`, `TurnStateOperations` (Supply) |
| `CardOperation` | `CardFaceOperations`, `PileOperations` |
| `DiceOperation` | `TurnStateOperations`, `RollOperations` |
| `TurnOperation` | `TurnStateOperations`, `BoardControlOperations` (`SetFestivalMarker`) |
| `ChronicleOperation` | `ChronicleStateOperations`, `AtlasOperations`, `TurnStateOperations` (`ChronicleUpdated`) |

Composites (`Take`, `Give`, `Exchange`, `Kill`, `Replace`, `Sacrifice`,
`Burn`, `PayCost`, `DrawNomadFavor`, `Gain`, `Discard`, `RemoveFromMap`,
`Draw`, `Play`, `Reveal`, `Swap`) do not extend `LeafOperation`, so the
intersection excludes them and no arm names them (second probe). The
leaves that do nothing in this step are named in explicit arms, each with
its reason:

- `Move` and `Bury`: the move passes already applied them (`Bury` in the
  card pass);
- `Roll`, which has no state of its own: it records that a roll happens.

These three are exactly the leaves today's wildcard catches. A new leaf
therefore forces its author to say whether it applies here or does nothing
here.

**The guard keeps its wildcard.** `nonMoveViolations` also ends in
`case _`, but most operations have no guard, and an exhaustive guard would
add about 50 "no guard" arms. A missing guard can only admit an illegal
input, which tests of illegal input catch; a missing mutation drops state
silently even in legal play. Only the mutation dispatch becomes exhaustive.

## The codec

`WalkerOperationCodec` is already a set of per-group `encodeXOperation`
helpers and `DecodedOperation` partial functions chained with `orElse`. It
splits into one trait per family:

- `PieceOperationCodec`, `ResourceOperationCodec`, `CardOperationCodec`,
  `DiceOperationCodec`, `TurnOperationCodec`, `ChronicleOperationCodec`,
  each with its family's encode match and decode partial function, about
  70 to 200 lines;
- `WalkerOperationCodec` keeps the exhaustive family dispatch for encode,
  the `orElse` chain for decode, and the unknown-kind error.

The private union aliases (`CardOperation` and the others) go away; the
model families replace them. The coverage tool's limit on a method body
stays met, since no single match spans the vocabulary again.

**The wire format does not change.** Every `kind` string and every field
keeps its spelling, so journals and fixtures are untouched.

The two helpers both the card and the piece codecs use, `withMover` and
`decodeMover`, and the `DecodedOperation` alias move to a small shared
trait, `OperationWireSupport`.

`WalkerOperationWireSuite` splits along the families: a suite each for
pieces, resources, cards, the turn and the Chronicle takes that family's
tests, and `WalkerOperationWireSuite` keeps the tests that cross families
(locations, payloads, the step envelope, Catacombs' batch, every variant).
Dice has no test of its own today, so it gets no suite. One new check, in
`WalkerOperationWireSuite`, pins that the families' kind sets are
disjoint: two families claiming one `kind` would otherwise decode silently
by `orElse` order.

Matchers that pick out a few operations with a deliberate wildcard
(`OperationRestrictions`, `DetailLines`, `HallOfMinisters`, the powers)
are not touched; constructor and extractor names do not change.

## Architecture rule

`scripts/check-architecture.py` gains a rule: only the six family files may
write `extends OperationBase` or `with OperationBase`. The rule lists the
family files by name.

Adding a family touches four places: the alias in `CoreOperations.scala`,
the rule's list, a codec trait, and an applier arm. The compiler forces
all but the rule's list, which the rule itself covers.

## Slices

Each slice preserves behavior and ends green on the full verification gate
(ROADMAP standing rules).

1. **Model split.** Create the six family files, shrink
   `CoreOperations.scala` to the base trait and the two aliases, add the
   architecture rule. The compiler is the main test; existing suites do
   not change.
2. **Exhaustive applier.** Replace the wildcard with `LeafApplication`'s
   family dispatch. If the exhaustive match turns up an operation that
   should change state and has no apply arm, stop and report it to the
   owner rather than fixing it inside the refactor.
3. **Codec and wire suite.** Create the six codec traits, split the wire
   suite, add the kind-disjointness check. `git diff` must show no change to
   fixtures or recorded journals, which shows the wire format did not move.

## Docs

- Delete the roadmap's "`CoreOperations.scala` is at the file limit" item.
- Repoint references to `CoreOperations.scala` as the home of the
  operations, and the claim that `PrimitiveOperation` is sealed, in
  `docs/architecture/follow-up-actions.md`,
  `docs/rules/implementation-traceability.md`,
  `docs/testing/audits/split-delta-bracketing-audit.md`, and the doc
  comments in `Operation.scala`, `OperationChildren.scala`,
  `CardPlayHooks.scala`, `BuryableCard.scala`, `DicePoolState.scala`,
  `Pieces.scala`, `PendingTree.scala`, `ForgeProcedure.scala` and the test
  kit's `WalkerStubs.scala`.
- Add **Operation family** to `CONTEXT.md`: one sealed area of Oath's
  operation vocabulary in its own file, joined to `CoreOperation` by the
  union alias. _Avoid_: operation group (the codec's old name for it).
- Add an "Adding an operation" note to
  `docs/architecture/codebase-structure.md`: choose the family, then add
  the encode and decode arms, the apply arm (or its explicit no-op arm) and
  the wire-suite case.

## Testing

The compiler carries most of the safety: under `-Werror`, a missing arm in
the encoder, the family dispatch or a family applier fails the build. New
tests are the kind-disjointness check and the architecture rule. The JVM,
Scala.js, replay and wire suites pass unchanged. The Scala.js build is not
affected: it grafts only three `model` files, none of them an operation
file.

## Out of scope

- Legacies' operations. The Legacies spec adds them as a new family or as
  cases of `ChronicleOperation`.
- An exhaustive guard (see "The applier").
- Deriving the codec. The
  [derive-wire-codecs design](2026-11-02-derive-wire-codecs-design.md)
  kept the operation codec hand-written because its exhaustiveness does
  real work; this split keeps that reason.
- Splitting `TurnOperation` into turn records and walk scratch.
