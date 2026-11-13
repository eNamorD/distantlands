# Operation Families Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Oath's operation vocabulary out of the one
`CoreOperations.scala` into six sealed operation families joined by a union
alias, make the leaf applier exhaustive, and split the operation codec and
its wire suite along the same families, so that a new operation grows one
family's files and the compiler names every place it must be handled.

**Architecture:** Each family is a sealed trait in its own file in
`distantlands.model`, extending an open `OperationBase`.
`type CoreOperation` is the union of the six, and
`type PrimitiveOperation = CoreOperation & LeafOperation` names Oath's
leaves. Scala 3 checks exhaustiveness through both, so constructors,
extractors and every exhaustive match keep working. A new
`LeafApplication` replaces the applier's wildcard with one exhaustive match
per family, and six codec traits replace the codec's union groups. The
wire format does not change.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`;
Python 3 for `scripts/check-architecture.py`.

**Spec:** [Operation families design](../../specs/2026-11/2026-11-12-operation-families-design.md).
Read all of it before Task 1; the "Probe result" section is the evidence
that the union and intersection aliases behave as this plan assumes.

## Global Constraints

- Behavior does not change. Every existing JVM and Scala.js test passes
  unchanged, apart from tests the plan moves between files verbatim. No
  file under `src/test/resources` changes, and no wire `kind` string or
  field name changes.
- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches, no unchecked type tests.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`, `BackendArchitectureSuite`).
- `model` imports nothing from `gameplay` or `serialization`
  (`scripts/check-architecture.py`). The family files are in package
  `distantlands.model` and need no imports.
- The six family names are exactly `PieceOperation`, `ResourceOperation`,
  `CardOperation`, `DiceOperation`, `TurnOperation`, `ChronicleOperation`;
  each lives in `src/main/scala/distantlands/model/<Name>.scala`. The base
  trait is `OperationBase`. The leaf alias keeps the name
  `PrimitiveOperation`.
- Family membership is fixed by the spec's table:
  - Piece: Move, Take, Give, Exchange, Kill, Replace, Sacrifice
  - Resource: SpendSupply, GainSupply, FlipSecrets, Gain, Burn, PayCost, DrawNomadFavor
  - Card: Peek, Flip, Reveal, Bury, Discard (and DiscardCause), RemoveFromMap, Draw, Play, Swap, Shuffle
  - Dice: ModifyDicePool, Roll, ModifyRollOutcome, Reroll, SetDieFaces
  - Turn: EnterPhase, BeginTurn, AdvanceRound, AdvanceVisionsDrawn, UsurperLimiterRemoved, SetOathkeeper, SetOathkeeperGoal, RecordPowerUse, RecordCampaignResult, QueueProcedure, Defer, TakeDeferred, ActAsIf, ActAsIfAdvisers, TrackPawn, ClearTrackedPawn, SetFestivalMarker, OpenPick, ClosePick
  - Chronicle: ChronicleUpdated, RoleChanged, ChronicleTaskAssigned, GrandScepterFromBox, SuitedReserveFromBox, ImperialWarbandsTaken, WarbandsAdded, LineagePointsScored, EdificeBuilt, FoundationRestored, PlaceAtlasSite
- This is a refactor. A task's red step is a compile failure or a check
  failure the task provokes on purpose and then undoes, as each task shows.
  The green step is the compiler and the existing suites.
- If an exhaustive match turns up an operation that should change state
  and has no apply arm, or a wire test that fails after a move, stop and
  report: do not fix behavior inside this refactor.
- Edits are anchored by code, not by line number. Line numbers quoted
  below are from `0ec0f2405` and only help you find the code. If a block
  does not match because a later merge moved it, find it by content; if
  the code itself differs, stop and report.
- Shell is zsh: quote globs (`--include='*.scala'`), never write a bare
  `=word` token.
- Never `git add -A` or `git add .`; add files by path. Never push. No bare
  `git stash`.
- Commit messages and docs are plain prose. Every commit message ends with
  the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`,
  whatever model you run on. Each commit command below passes the subject,
  the body and the trailer as three `-m` arguments.

---

## Setup

Work in a worktree cut from `0.2.0-alpha` at or after the commit that adds
this plan:

```bash
cd /Users/roman/projects/distantlands
git worktree add .worktrees/operation-families -b operation-families 0.2.0-alpha
cd .worktrees/operation-families
test -e .tooling || ln -s /Users/roman/projects/distantlands/.tooling .tooling
test -e node_modules || ln -s /Users/roman/projects/distantlands/node_modules node_modules
```

Both links are ignored by git; never add them. Never run a git command that
changes state in the main checkout, `/Users/roman/projects/distantlands`.

Check the base, by content:

```bash
git merge-base --is-ancestor 0ec0f2405 HEAD && echo spec-ok
wc -l src/main/scala/distantlands/model/CoreOperations.scala
git grep -n "^sealed trait CoreOperation extends Operation:" -- src/main/scala/distantlands/model/CoreOperations.scala
git grep -n "^sealed trait PrimitiveOperation extends CoreOperation, LeafOperation" -- src/main/scala/distantlands/model/CoreOperations.scala
git grep -n "      case (result, _) => result" -- src/main/scala/distantlands/gameplay/operations/OperationApplication.scala
git grep -n "  private type ChronicleOperation = RoleChanged | ChronicleTaskAssigned |" -- src/main/scala/distantlands/serialization/WalkerOperationCodec.scala
git grep -nE "\b(PieceOperation|ResourceOperation|CardOperation|DiceOperation|TurnOperation|ChronicleOperation|OperationBase|LeafApplication|OperationWireSupport)\b" -- src shared frontend ':!src/main/scala/distantlands/serialization/WalkerOperationCodec.scala'
python3 scripts/check-architecture.py
```

The first prints `spec-ok`. `wc` prints 796 (a later merge may move it a
few lines). The next four greps print one line each. The family-name grep
prints nothing and exits with status 1: no name the plan adds is taken
outside the codec, whose private aliases of the same names Task 3 removes.
The architecture check passes.

A JVM suite runs as `./sbtw "testOnly distantlands.<package>.<Suite>"`; a
frontend suite as `./sbtw "frontend/testOnly distantlands.frontend.<Suite>"`.

---

### Task 1: Split the model into operation families

**Files:**
- Create: `src/main/scala/distantlands/model/PieceOperation.scala`
- Create: `src/main/scala/distantlands/model/ResourceOperation.scala`
- Create: `src/main/scala/distantlands/model/CardOperation.scala`
- Create: `src/main/scala/distantlands/model/DiceOperation.scala`
- Create: `src/main/scala/distantlands/model/TurnOperation.scala`
- Create: `src/main/scala/distantlands/model/ChronicleOperation.scala`
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (whole file rewritten)
- Modify: `scripts/check-architecture.py` (new rule before `if errors:`)
- Modify (doc comments only): `src/main/scala/distantlands/model/Operation.scala`,
  `OperationChildren.scala`, `CardPlayHooks.scala`, `BuryableCard.scala`,
  `DicePoolState.scala`, `Pieces.scala`, `PendingTree.scala` (all in
  `src/main/scala/distantlands/model/`), and
  `src/test/scala/distantlands/testkit/WalkerStubs.scala`
- Test: the compiler; `scripts/check-architecture.py`; the full JVM suite

**Interfaces:**
- Produces: `trait OperationBase extends Operation` (holds
  `def simultaneous: Boolean = false`); the six `sealed trait <Family>
  extends OperationBase`; `type CoreOperation = PieceOperation |
  ResourceOperation | CardOperation | DiceOperation | TurnOperation |
  ChronicleOperation`; `type PrimitiveOperation = CoreOperation &
  LeafOperation`. Every former `extends PrimitiveOperation` case now
  `extends <Family>, LeafOperation`; every former `extends CoreOperation`
  case now `extends <Family>`.

- [ ] **Step 1: Write the split script to the scratchpad**

Save this as `split_core_operations.py` in your scratchpad directory (not in
the repository; it is never committed). It moves each top-level block of
`CoreOperations.scala` (its doc comment, its declaration and its companion
object) verbatim into its family's file, rewriting only the `extends`
clauses, drops the old header and the "Walker leaves" banner comment, and
rewrites `CoreOperations.scala`.

```python
#!/usr/bin/env python3
"""Split model/CoreOperations.scala into the six operation family files.

Run from the repository root:  python3 <this file> [--dry-run]
Moves every top-level block (its doc comment, declaration and companion)
verbatim into its family's file, rewriting only the `extends` clauses,
then rewrites CoreOperations.scala to the base trait and the aliases.
"""
import re
import sys
from pathlib import Path

MODEL = Path("src/main/scala/distantlands/model")
SOURCE = MODEL / "CoreOperations.scala"

FAMILIES = {
    "PieceOperation": ["Move", "Take", "Give", "Exchange", "Kill", "Replace",
                       "Sacrifice"],
    "ResourceOperation": ["FlipSecrets", "GainSupply", "SpendSupply", "Burn",
                          "Gain", "DrawNomadFavor", "PayCost"],
    "CardOperation": ["Peek", "Flip", "Bury", "DiscardCause", "Discard",
                      "RemoveFromMap", "Draw", "Play", "Reveal", "Swap",
                      "Shuffle"],
    "DiceOperation": ["ModifyDicePool", "Roll", "ModifyRollOutcome", "Reroll",
                      "SetDieFaces"],
    "TurnOperation": ["RecordPowerUse", "EnterPhase", "SetOathkeeper",
                      "SetOathkeeperGoal", "RecordCampaignResult",
                      "QueueProcedure", "BeginTurn", "UsurperLimiterRemoved",
                      "AdvanceVisionsDrawn", "AdvanceRound", "Defer",
                      "TakeDeferred", "ActAsIf", "ActAsIfAdvisers",
                      "TrackPawn", "ClearTrackedPawn", "SetFestivalMarker",
                      "OpenPick", "ClosePick"],
    "ChronicleOperation": ["ChronicleUpdated", "RoleChanged",
                           "ChronicleTaskAssigned", "GrandScepterFromBox",
                           "SuitedReserveFromBox", "ImperialWarbandsTaken",
                           "WarbandsAdded", "LineagePointsScored",
                           "EdificeBuilt", "FoundationRestored",
                           "PlaceAtlasSite"],
}
FAMILY_OF = {name: family for family, names in FAMILIES.items()
             for name in names}

HEADERS = {
    "PieceOperation": (
        "Operations that move pieces between custodies: a single `Move`, and\n"
        "  * the composites that take, give, exchange, kill, replace or\n"
        "  * sacrifice pieces through `Move` children."),
    "ResourceOperation": (
        "Operations on counted resources and Supply: favor and secrets\n"
        "  * (flipping, burning, gaining, the Nomad bank), Supply spent and\n"
        "  * gained, and paying a typed cost."),
    "CardOperation": (
        "Operations on cards: looking at, flipping and revealing them,\n"
        "  * burying, discarding and removing them, drawing, playing and\n"
        "  * swapping them, and reordering a shuffle pile."),
    "DiceOperation": (
        "Operations on dice pools and rolls. Dice-pool counts live in\n"
        "  * `CurrentGameState.rollPools`; a `Roll` records that a roll\n"
        "  * happens, and the walker or the next command supplies its faces."),
    "TurnOperation": (
        "Operations on the turn and its tracks, and the walk's scratch:\n"
        "  * phases and turns, the round and Visions Drawn tracks, the\n"
        "  * Oathkeeper title and goal, power uses, campaign results, queued\n"
        "  * procedures, deferrals, as-if records, the Tracker record, the\n"
        "  * festival marker, and pick records."),
    "ChronicleOperation": (
        "Operations the Chronicle Phase and the Atlas journal: roles,\n"
        "  * tasks, the Grand Scepter, suited reserves, Imperial warbands,\n"
        "  * lineage points, built edifices, restored foundations, placed\n"
        "  * Atlas sites, and the evolved Chronicle."),
}

DECLARATION = re.compile(
    r"^(?:final case class|case class|case object|sealed trait|object)\s+(\w+)")


def blocks(lines):
    """Top-level blocks: (first declared name or None, text lines)."""
    starts = []
    for index, line in enumerate(lines):
        if not line or line[0] in " \t":
            continue
        previous = lines[index - 1] if index else ""
        starts_doc = line.startswith("/**") or line.startswith("//")
        if starts_doc and (not previous or previous[0] not in "/ \t*"):
            starts.append(index)
        elif DECLARATION.match(line) and previous.strip() in ("", "*/") \
                and not previous.rstrip().endswith("*/"):
            starts.append(index)
    starts.append(len(lines))
    result = []
    for begin, end in zip(starts, starts[1:]):
        chunk = lines[begin:end]
        name = next((DECLARATION.match(l).group(1) for l in chunk
                     if DECLARATION.match(l)), None)
        result.append((name, chunk))
    return result


def rewrite(chunk, family):
    text = "\n".join(chunk)
    text = re.sub(r"extends PrimitiveOperation\b",
                  f"extends {family}, LeafOperation", text)
    text = re.sub(r"extends CoreOperation\b", f"extends {family}", text)
    return text.rstrip("\n").split("\n")


def main(dry_run):
    lines = SOURCE.read_text(encoding="utf-8").split("\n")
    out = {family: [] for family in FAMILIES}
    unplaced = []
    for name, chunk in blocks(lines[1:]):           # line 0 is the package
        if name in ("CoreOperation", "PrimitiveOperation") or name is None:
            continue                                # header, root, banner
        family = FAMILY_OF.get(name)
        if family is None:
            unplaced.append(name)
            continue
        out[family].append(rewrite(chunk, family))
    missing = sorted(set(FAMILY_OF) - {n for n, _ in blocks(lines[1:])})
    if unplaced or missing:
        sys.exit(f"unplaced blocks {unplaced}, missing names {missing}")
    for family, chunks in out.items():
        body = ["package distantlands.model", "",
                f"/** {HEADERS[family]}",
                "  *",
                "  * One operation family: a sealed area of Oath's operation",
                "  * vocabulary, joined to [[CoreOperation]] by the union alias",
                "  * in `CoreOperations.scala`. A leaf also extends",
                "  * [[LeafOperation]]; a composite exposes its `children`.",
                "  */",
                f"sealed trait {family} extends OperationBase", ""]
        for chunk in chunks:
            body.extend(chunk)
            body.append("")
        target = MODEL / f"{family}.scala"
        text = "\n".join(body).rstrip("\n") + "\n"
        print(f"{target}: {len(text.splitlines())} lines")
        if not dry_run:
            target.write_text(text, encoding="utf-8")
    if not dry_run:
        SOURCE.write_text(ROOT_TEXT, encoding="utf-8")
    print(f"{SOURCE}: {len(ROOT_TEXT.splitlines())} lines")


ROOT_TEXT = """package distantlands.model

/** Core operations occurring in a game of Oath.
  *
  * These values describe semantic game instructions. They do not
  * validate restrictions, choices, and consent.
  *
  * Oath's own vocabulary is split into operation families, each a sealed
  * trait in its own file: [[PieceOperation]], [[ResourceOperation]],
  * [[CardOperation]], [[DiceOperation]], [[TurnOperation]] and
  * [[ChronicleOperation]]. [[CoreOperation]] is their union, so the
  * serializer, the applier and the restriction screener still match the
  * whole vocabulary exhaustively, and a new case in any family fails to
  * compile until every such match has an arm. The walker's generic control
  * vocabulary is not here: it lives under the unsealed [[Operation]] in
  * `Operation.scala`, and a control node is therefore not a
  * `Vector[CoreOperation]` member and never a recorded delta.
  *
  * Composites expose their constituent operations through `children`; read
  * the leaves with [[Operation.flatten]]. [[PrimitiveOperation]] is Oath's
  * half of [[LeafOperation]]: a family case that is also a leaf.
  *
  * Only the family files extend [[OperationBase]]
  * (`scripts/check-architecture.py`). A family joins the vocabulary by
  * joining the union below.
  */
trait OperationBase extends Operation:
  def simultaneous: Boolean = false

type CoreOperation = PieceOperation | ResourceOperation | CardOperation |
  DiceOperation | TurnOperation | ChronicleOperation

type PrimitiveOperation = CoreOperation & LeafOperation
"""

if __name__ == "__main__":
    main("--dry-run" in sys.argv)
```

- [ ] **Step 2: Dry-run the script**

Run (from the worktree root): `python3 <scratchpad>/split_core_operations.py --dry-run`

Expected, give or take a few lines if a merge moved code:

```text
src/main/scala/distantlands/model/PieceOperation.scala: 120 lines
src/main/scala/distantlands/model/ResourceOperation.scala: 140 lines
src/main/scala/distantlands/model/CardOperation.scala: 253 lines
src/main/scala/distantlands/model/DiceOperation.scala: 61 lines
src/main/scala/distantlands/model/TurnOperation.scala: 183 lines
src/main/scala/distantlands/model/ChronicleOperation.scala: 85 lines
src/main/scala/distantlands/model/CoreOperations.scala: 33 lines
```

If it exits with "unplaced blocks", a merge added an operation: add its
name to the family the spec's rules give it (ask if unclear) and rerun.

- [ ] **Step 3: Run the script and prove nothing was lost**

Run: `python3 <scratchpad>/split_core_operations.py`

Then check that every non-blank line of the old file, apart from the old
header, the root traits and the banner comment, is in a family file, with
only `extends` clauses rewritten:

```bash
python3 - <<'EOF'
import re, subprocess
from collections import Counter
from pathlib import Path
old = subprocess.run(["git", "show", "HEAD:src/main/scala/distantlands/model/CoreOperations.scala"],
                     capture_output=True, text=True, check=True).stdout.splitlines()
model = Path("src/main/scala/distantlands/model")
new = []
for family in ["PieceOperation", "ResourceOperation", "CardOperation",
               "DiceOperation", "TurnOperation", "ChronicleOperation"]:
    new += (model / f"{family}.scala").read_text().splitlines()
norm = lambda line: re.sub(r"extends \w+Operation(, LeafOperation)?", "extends X", line)
start = next(i for i, l in enumerate(old) if l.startswith("sealed trait PrimitiveOperation")) + 1
before = Counter(norm(l) for l in old[start:] if l.strip() and not l.startswith("// "))
after = Counter(norm(l) for l in new if l.strip())
print("missing:", sum((before - after).values()))
print("leftover:", [l for l in new if "extends PrimitiveOperation" in l or "extends CoreOperation" in l])
EOF
```

Expected: `missing: 0` and `leftover: []`.

- [ ] **Step 4: Compile**

Run: `./sbtw Test/compile`
Expected: success. The codec's private aliases `CardOperation`,
`PieceOperation`, `ResourceOperation`, `TurnOperation` and
`ChronicleOperation` shadow the model's names inside
`WalkerOperationCodec`; that is legal and Task 3 removes them. If the
compiler reports an unchecked type test or an exhaustivity warning, stop
and report it with the exact message: the spec's probe says neither
happens.

- [ ] **Step 5: See the architecture rule fail before it exists**

Plant a rogue extension of `OperationBase` outside the families:

```bash
printf 'package distantlands.model\n\nfinal case class RogueOperation() extends OperationBase, LeafOperation\n' > src/main/scala/distantlands/model/RogueOperation.scala
python3 scripts/check-architecture.py
```

Expected: `architecture check passed`. That is the gap: nothing stops it
yet. Leave the file in place for the next step.

- [ ] **Step 6: Add the architecture rule**

In `scripts/check-architecture.py`, insert this block immediately before
the line `if errors:`:

```python
# Oath's operation vocabulary is a union of sealed operation families
# (docs/superpowers/specs/2026-11/2026-11-12-operation-families-design.md).
# Only the family files extend OperationBase: a class that extends it
# anywhere else would be neither a family case nor a CoreOperation, and the
# walker would treat it as a control node.
MODEL_ROOT = "src/main/scala/distantlands/model"
OPERATION_FAMILY_FILES = {
    f"{MODEL_ROOT}/{family}.scala" for family in (
        "PieceOperation", "ResourceOperation", "CardOperation",
        "DiceOperation", "TurnOperation", "ChronicleOperation")
}
operation_base_extension = re.compile(
    r"\b(?:extends|with)\s+OperationBase\b|,\s*OperationBase\b")
for path in sources:
    rel = relative(path)
    if rel in OPERATION_FAMILY_FILES:
        continue
    for line_number, line in enumerate(
            path.read_text(encoding="utf-8").splitlines(), 1):
        if operation_base_extension.search(line):
            errors.append(f"{rel}:{line_number}: only an operation family "
                          "file may extend OperationBase")
for family_file in sorted(OPERATION_FAMILY_FILES):
    if not (ROOT / family_file).is_file():
        errors.append(f"{family_file}: listed as an operation family file "
                      "but missing; update check-architecture.py")
```

Run: `python3 scripts/check-architecture.py`
Expected: FAIL with exactly
`- src/main/scala/distantlands/model/RogueOperation.scala:3: only an operation family file may extend OperationBase`

Then remove the rogue file and rerun:

```bash
rm src/main/scala/distantlands/model/RogueOperation.scala
python3 scripts/check-architecture.py
```

Expected: `architecture check passed`.

- [ ] **Step 7: Update the doc comments that point at the old file**

Each block below occurs once in its file. Replace it as shown.

`src/main/scala/distantlands/model/Operation.scala`, replace:

```scala
  * Oath's operations live under the sealed [[CoreOperation]] in
  * `CoreOperations.scala`, where exhaustive matching over the game's
  * vocabulary is what the serializer and the restriction screener rely on.
```

with:

```scala
  * Oath's operations live under [[CoreOperation]], the union of the sealed
  * operation families (`CoreOperations.scala` lists them), where exhaustive
  * matching over the game's vocabulary is what the serializer, the applier
  * and the restriction screener rely on.
```

Then replace:

```scala
  * one leaf test. Unsealed, so a leaf may be authored outside
  * `CoreOperations.scala`; [[PrimitiveOperation]] is Oath's sealed half of
  * this.
```

with:

```scala
  * one leaf test. Unsealed, so a leaf may be authored outside Oath's
  * operation families; [[PrimitiveOperation]] is Oath's half of this.
```

`src/main/scala/distantlands/model/OperationChildren.scala`, replace:

```scala
/** Builds the children of the composite operations in
  * `CoreOperations.scala`: a world card's discard move, the favor and
  * secrets a card returns as it leaves play, and a payment out of a
  * player's play area. Split out of that file, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound
```

with:

```scala
/** Builds the children of the composite operations in the operation
  * families (`CardOperation.scala`, `ResourceOperation.scala`): a world
  * card's discard move, the favor and secrets a card returns as it leaves
  * play, and a payment out of a player's play area. Split out of the
  * single operations file that preceded the families, when it was close to
  * the project's per-file bound
```

`src/main/scala/distantlands/model/CardPlayHooks.scala`, replace:

```scala
// the unsealed [[Operation]], so they live outside `CoreOperations.scala`
```

with:

```scala
// the unsealed [[Operation]], so they live outside the operation families
```

`src/main/scala/distantlands/model/BuryableCard.scala`, replace:

```scala
/** A card [[Bury]] can send to the bottom of its deck. Split out of
  * `CoreOperations.scala`, which holds the sealed `CoreOperation` family
  * and is close to the project's per-file bound. */
```

with:

```scala
/** A card [[Bury]] can send to the bottom of its deck. [[Bury]] is a
  * [[CardOperation]]. */
```

`src/main/scala/distantlands/model/Pieces.scala`, replace:

```scala
/** The pieces an operation moves, and where in a location it moves them
  * from or to. Split out of `CoreOperations.scala`, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound. */
```

with:

```scala
/** The pieces an operation moves, and where in a location it moves them
  * from or to. */
```

`src/main/scala/distantlands/model/DicePoolState.scala`, replace:

```scala
  * `gameplay/operations/CoreOperations.scala`).
```

with:

```scala
  * `model/DiceOperation.scala`).
```

`src/main/scala/distantlands/model/PendingTree.scala`, replace:

```scala
  * not import gameplay, so the pool keys are declared here. The dice-pool
  * leaves in `gameplay/operations/CoreOperations.scala` reference them through
  * the normal model import.
```

with:

```scala
  * not import gameplay, so the pool keys are declared here. The dice-pool
  * leaves in `model/DiceOperation.scala` reference them through the normal
  * model import.
```

`src/test/scala/distantlands/testkit/WalkerStubs.scala`, replace:

```scala
    * the walker's generic wiring with a tree it builds itself. Oath's own
    * operations are sealed under `CoreOperation`, which Scala pins to
    * `CoreOperations.scala`; this node is deliberately not one of them.
```

with:

```scala
    * the walker's generic wiring with a tree it builds itself. Oath's own
    * operations are the sealed operation families under `CoreOperation`,
    * which Scala pins to their files; this node is deliberately not one of
    * them.
```

`ForgeProcedure.scala`'s "`SpendSupply` is a bare `PrimitiveOperation`"
stays true and does not change.

Run: `git grep -n "CoreOperations.scala" -- src`
Expected: only lines inside `src/main/scala/distantlands/model/CoreOperations.scala`
itself, or nothing.

- [ ] **Step 8: Run the JVM suite**

Run: `./sbtw test`
Expected: PASS. If any test fails, stop and report.

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/model/CoreOperations.scala \
  src/main/scala/distantlands/model/PieceOperation.scala \
  src/main/scala/distantlands/model/ResourceOperation.scala \
  src/main/scala/distantlands/model/CardOperation.scala \
  src/main/scala/distantlands/model/DiceOperation.scala \
  src/main/scala/distantlands/model/TurnOperation.scala \
  src/main/scala/distantlands/model/ChronicleOperation.scala \
  src/main/scala/distantlands/model/Operation.scala \
  src/main/scala/distantlands/model/OperationChildren.scala \
  src/main/scala/distantlands/model/CardPlayHooks.scala \
  src/main/scala/distantlands/model/BuryableCard.scala \
  src/main/scala/distantlands/model/Pieces.scala \
  src/main/scala/distantlands/model/DicePoolState.scala \
  src/main/scala/distantlands/model/PendingTree.scala \
  src/test/scala/distantlands/testkit/WalkerStubs.scala \
  scripts/check-architecture.py
git commit -m "refactor(model): split the operation vocabulary into sealed families" \
  -m "CoreOperations.scala could only grow in one file because CoreOperation was sealed. Each area of the vocabulary is now a sealed operation family in its own file, CoreOperation is their union and PrimitiveOperation their leaves, and the architecture check refuses an OperationBase extension outside the family files. Constructors, extractors and exhaustive matches are unchanged." \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Apply every leaf through an exhaustive family match

**Files:**
- Create: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/OperationApplication.scala`
  (`applyNonMoveLeaves`, about lines 213-267)
- Test: the compiler; `./sbtw "testOnly distantlands.gameplay.operations.*"`;
  the full JVM suite

**Interfaces:**
- Consumes: Task 1's six families and `PrimitiveOperation`.
- Produces: `private[operations] object LeafApplication` with
  `def apply(ready: ReadyGame, leaf: PrimitiveOperation): Either[OperationError, ReadyGame]`.
  The state objects it calls are unchanged.

- [ ] **Step 1: Create `LeafApplication`**

Create `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`.
Every arm is the arm `applyNonMoveLeaves` has today for that operation,
moved here, with `result.flatMap(f(_, ...))` written as `f(ready, ...)` and
`result.map(f(_, ...))` as `Right(f(ready, ...))`.

```scala
package distantlands.gameplay.operations

import distantlands.model._

/** The non-move mutation of every Oath leaf, one exhaustive match per
  * operation family (operation families design, "The applier").
  * [[OperationApplication]] calls [[apply]] for each leaf after its move
  * passes, so a new leaf fails to compile here until it says what it
  * changes, or that it changes nothing in this step.
  *
  * Composites are not leaves: [[Operation.flatten]] never yields them, and
  * [[PrimitiveOperation]] (`CoreOperation & LeafOperation`) excludes them,
  * so no arm names them.
  */
private[operations] object LeafApplication:
  def apply(ready: ReadyGame,
      leaf: PrimitiveOperation): Either[OperationError, ReadyGame] =
    leaf match
      case leaf: PieceOperation => piece(ready, leaf)
      case leaf: ResourceOperation => resource(ready, leaf)
      case leaf: CardOperation => card(ready, leaf)
      case leaf: DiceOperation => dice(ready, leaf)
      case leaf: TurnOperation => turn(ready, leaf)
      case leaf: ChronicleOperation => chronicle(ready, leaf)

  private def piece(ready: ReadyGame, leaf: PieceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    // The counted, pawn-and-banner and card passes applied it.
    case _: Move => Right(ready)

  private def resource(ready: ReadyGame,
      leaf: ResourceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case FlipSecrets(player, amount, from, to) =>
      ResourceOperations.flipPlayerSecrets(ready, player, amount, from, to)
    case SpendSupply(player, amount, _) =>
      TurnStateOperations.adjustSupply(ready, player, -amount)
    case GainSupply(player, amount) =>
      TurnStateOperations.adjustSupply(ready, player, amount)

  private def card(ready: ReadyGame, leaf: CardOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case Flip(id, at, orientation) =>
      CardFaceOperations.flipCard(ready, id, at, orientation)
    case Peek(viewer, id, at) => CardFaceOperations.peek(ready, viewer, id, at)
    case Shuffle(pile, order) => PileOperations.shuffle(ready, pile, order)
    // The card pass applied it.
    case _: Bury => Right(ready)

  private def dice(ready: ReadyGame, leaf: DiceOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case ModifyDicePool(pool, delta, _) =>
      TurnStateOperations.adjustDicePool(ready, pool, delta)
    case ModifyRollOutcome(pool, skulls, score) =>
      Right(TurnStateOperations.modifyRollOutcome(ready, pool, skulls, score))
    case Reroll(pool, indices, faces) =>
      RollOperations.reroll(ready, pool, indices, faces)
    case SetDieFaces(pool, faces) => RollOperations.setFaces(ready, pool, faces)
    // Records that a roll happens; the walker or the next command supplies
    // its faces.
    case _: Roll => Right(ready)

  private def turn(ready: ReadyGame, leaf: TurnOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case AdvanceVisionsDrawn => TurnStateOperations.advanceVisionsDrawn(ready)
    case AdvanceRound => TurnStateOperations.advanceRound(ready)
    case UsurperLimiterRemoved =>
      TurnStateOperations.usurperLimiterRemoved(ready)
    case RecordPowerUse(power) =>
      Right(TurnStateOperations.recordPowerUse(ready, power))
    case Defer(key, ops) => Right(TurnStateOperations.defer(ready, key, ops))
    case TakeDeferred(key) => Right(TurnStateOperations.takeDeferred(ready, key))
    case ActAsIf(site) => Right(TurnStateOperations.actAsIf(ready, site))
    case ActAsIfAdvisers(player) =>
      TurnStateOperations.actAsIfAdvisers(ready, player)
    case TrackPawn(holder, target) =>
      TurnStateOperations.trackPawn(ready, holder, target)
    case ClearTrackedPawn(holder) =>
      TurnStateOperations.clearTrackedPawn(ready, holder)
    case SetFestivalMarker(site) =>
      BoardControlOperations.setFestivalMarker(ready, site)
    case OpenPick(decisionId, option, label) =>
      Right(TurnStateOperations.openPick(ready, decisionId, option, label))
    case ClosePick() => Right(TurnStateOperations.closePick(ready))
    case EnterPhase(phase) => TurnStateOperations.enterPhase(ready, phase)
    case SetOathkeeper(holder) => TurnStateOperations.setOathkeeper(ready, holder)
    case SetOathkeeperGoal(goal) =>
      TurnStateOperations.setOathkeeperGoal(ready, goal)
    case RecordCampaignResult(fact) =>
      Right(TurnStateOperations.recordCampaignResult(ready, fact))
    case QueueProcedure(entry, atFront) =>
      Right(TurnStateOperations.queueProcedure(ready, entry, atFront))
    case BeginTurn(player, phase) =>
      TurnStateOperations.beginTurn(ready, player, phase)

  private def chronicle(ready: ReadyGame,
      leaf: ChronicleOperation & LeafOperation)
      : Either[OperationError, ReadyGame] = leaf match
    case ChronicleUpdated(evolved) =>
      Right(TurnStateOperations.chronicleUpdated(ready, evolved))
    case RoleChanged(lineage, role) =>
      ChronicleStateOperations.roleChanged(ready, lineage, role)
    case ChronicleTaskAssigned(task, player) =>
      ChronicleStateOperations.taskAssigned(ready, task, player)
    case GrandScepterFromBox(player) =>
      ChronicleStateOperations.scepterFromBox(ready, player)
    case SuitedReserveFromBox(suit, cards) =>
      ChronicleStateOperations.suitedReserveFromBox(ready, suit, cards)
    case ImperialWarbandsTaken(count) =>
      Right(ChronicleStateOperations.imperialWarbandsTaken(ready, count))
    case WarbandsAdded(kind, count) =>
      ChronicleStateOperations.warbandsAdded(ready, kind, count)
    case LineagePointsScored(lineage, points) =>
      ChronicleStateOperations.lineagePointsScored(ready, lineage, points)
    case EdificeBuilt(edifice, site) =>
      ChronicleStateOperations.edificeBuilt(ready, edifice, site)
    case FoundationRestored(number) =>
      ChronicleStateOperations.foundationRestored(ready, number)
    case placement: PlaceAtlasSite => AtlasOperations.placeSite(ready, placement)
```

- [ ] **Step 2: Route `applyNonMoveLeaves` through it**

In `OperationApplication.scala`, replace the whole method, from

```scala
  private def applyNonMoveLeaves(
      ready: ReadyGame,
      leaves: Vector[Operation]
  ): Either[OperationError, ReadyGame] =
    leaves.foldLeft[Either[OperationError, ReadyGame]](Right(ready)):
      case (result, Flip(id, at, orientation)) =>
```

through its last arm

```scala
      case (result, BeginTurn(player, phase)) =>
        result.flatMap(TurnStateOperations.beginTurn(_, player, phase))
      case (result, _) => result
```

with:

```scala
  /** Every Oath leaf goes to [[LeafApplication]], whose family matches are
    * exhaustive. `Operation.flatten` of a `CoreOperation` yields only Oath
    * leaves, so the last arm is for a leaf of some other vocabulary, a
    * walker control node, which never reaches the applier; it is skipped
    * as before.
    */
  private def applyNonMoveLeaves(
      ready: ReadyGame,
      leaves: Vector[Operation]
  ): Either[OperationError, ReadyGame] =
    leaves.foldLeft[Either[OperationError, ReadyGame]](Right(ready)):
      case (result, leaf: PrimitiveOperation) =>
        result.flatMap(LeafApplication(_, leaf))
      case (result, _) => result
```

If the compiler warns that the type test `leaf: PrimitiveOperation` is
unchecked, use this arm instead, which tests the two class types in turn:

```scala
      case (result, leaf: LeafOperation) => leaf match
        case leaf: CoreOperation => result.flatMap(LeafApplication(_, leaf))
        case _ => result
```

- [ ] **Step 3: Compile and see the red case**

Run: `./sbtw Test/compile`
Expected: success, with no warning.

Now prove the match is exhaustive: delete the line
`    case FoundationRestored(number) =>` and the line after it from
`LeafApplication.scala`, then run `./sbtw compile`.
Expected: FAIL with `match may not be exhaustive.` and
`It would fail on pattern case: distantlands.model.FoundationRestored(_)`.
Restore the two lines (`git diff` on the file must then show it unchanged
from Step 1) and run `./sbtw compile` again: success.

If `./sbtw compile` instead reports a non-exhaustive match on an operation
Step 1 does not list, that is a leaf today's wildcard swallowed. Stop and
report its name; do not add an arm.

- [ ] **Step 4: Run the operation suites, then the JVM suite**

Run: `./sbtw "testOnly distantlands.gameplay.operations.*"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/operations/LeafApplication.scala \
  src/main/scala/distantlands/gameplay/operations/OperationApplication.scala
git commit -m "refactor(operations): apply every leaf through an exhaustive family match" \
  -m "applyNonMoveLeaves ended in a wildcard, so a new operation without an apply arm did nothing and compiled. LeafApplication matches each family's leaves exhaustively and names the three leaves that change nothing in that step: Move and Bury, which the move passes apply, and Roll." \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Split the operation codec by family

**Files:**
- Create: `src/main/scala/distantlands/serialization/OperationWireSupport.scala`
- Create: `src/main/scala/distantlands/serialization/PieceOperationCodec.scala`
- Create: `src/main/scala/distantlands/serialization/ResourceOperationCodec.scala`
- Create: `src/main/scala/distantlands/serialization/CardOperationCodec.scala`
- Create: `src/main/scala/distantlands/serialization/DiceOperationCodec.scala`
- Create: `src/main/scala/distantlands/serialization/TurnOperationCodec.scala`
- Create: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Modify: `src/main/scala/distantlands/serialization/WalkerOperationCodec.scala` (whole file rewritten)
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Consumes: Task 1's families.
- Produces: in package `distantlands.serialization`,
  `private[serialization] trait OperationWireSupport` with
  `protected type DecodedOperation = PartialFunction[String, Either[WireError, CoreOperation]]`,
  `protected final def withMover(encoded: ujson.Obj, by: Option[PlayerId]): ujson.Obj` and
  `protected final def decodeMover(value: ujson.Value): Option[PlayerId]`;
  and for each family `<F>` a trait `<F>Codec` with
  `protected final def encode<F>(operation: <F>): ujson.Value` and
  `protected final def decode<F>(value: ujson.Value, path: String): DecodedOperation`.
  `WalkerOperationCodec.encodeOperation` and `decodeOperation` keep their
  signatures, so `WalkerEventCodec` does not change.

- [ ] **Step 1: Write the kind-disjointness test first**

In `WalkerOperationWireSuite.scala`, the test "every CoreOperation variant
round-trips through the walker codec" builds a local
`val operations: Vector[CoreOperation]` after nine local values (`player`,
`other`, `site`, `denizen`, `vision`, `relic`, `edifice`, `legacy`,
`lineage`). Move those nine values and the `operations` vector, unchanged,
out of the test to class level, renaming the vector `everyOperation` and
making all ten `private val`. Keep the comment block at the top of the
test with the test. Inside the test, change the two uses of `operations`
to `everyOperation`. Then add, directly after that test:

```scala
  /** The family an operation belongs to. Exhaustive, so a new family fails
    * to compile here until it is named. */
  private def familyOf(operation: CoreOperation): String = operation match
    case _: PieceOperation => "piece"
    case _: ResourceOperation => "resource"
    case _: CardOperation => "card"
    case _: DiceOperation => "dice"
    case _: TurnOperation => "turn"
    case _: ChronicleOperation => "chronicle"

  test("each wire kind belongs to one operation family"):
    val kinds: Map[String, Set[String]] = everyOperation.groupMap(familyOf)(
      operation => ujson.read(accepted(GameEventWire.encodeEvent("walker", 0,
        recordedOperation(operation))))("payload")("step")("ops")(0)("kind")
        .str).view.mapValues(_.toSet).toMap
    assertEquals(kinds.keySet,
      Set("piece", "resource", "card", "dice", "turn", "chronicle"))
    for
      (family, own) <- kinds
      (other, theirs) <- kinds
      if family < other
    do assertEquals(own.intersect(theirs), Set.empty[String],
      s"$family and $other share wire kinds")
```

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly distantlands.serialization.WalkerOperationWireSuite"`
Expected: PASS. The kinds are already disjoint; this test pins it for the
split. To see it can fail, temporarily change the `"kind" -> "gain-supply"`
spelling in the codec's `GainSupply` encode arm to `"kind" -> "move"`, run
the suite, and see "piece and resource share wire kinds" (other tests fail
too). Revert the spelling (`git diff` on the codec shows nothing) and rerun:
PASS.

- [ ] **Step 3: Create `OperationWireSupport`**

```scala
package distantlands.serialization

import distantlands.model._

/** What more than one operation family's codec uses: the decode-side
  * partial function each family contributes, and the optional `by` of a
  * move or a swap. */
private[serialization] trait OperationWireSupport:
  protected type DecodedOperation =
    PartialFunction[String, Either[WireError, CoreOperation]]
```

Then move `withMover` and `decodeMover` from `WalkerOperationCodec.scala`
into this trait unchanged except that `private def` becomes
`protected final def`. If either body uses something from the codec's
other mixins, add that mixin to `OperationWireSupport`'s `extends` clause
with the same self type the codec has (`this: GameEventJsonSupport =>`).

- [ ] **Step 4: Create the six family codecs**

Each family codec has this shape; `<F>` is the family name and the arms
come from `WalkerOperationCodec.scala`, moved unchanged:

```scala
package distantlands.serialization

import distantlands.model._

/** The wire spellings of the <F> family. */
private[serialization] trait <F>Codec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encode<F>(operation: <F>): ujson.Value =
    operation match
      // the encode arms

  protected final def decode<F>(value: ujson.Value,
      path: String): DecodedOperation =
    // the decode arms
```

Drop `import WireError._` from a trait whose arms use no `WireError` name
(the compiler's unused-import warning tells you). Move the arms and helpers
as follows; an arm is the `case` line and its body, encode and decode both.

- `PieceOperationCodec`: from `encodePieceOperation`/`decodePieceOperation`,
  the arms for `Move`, `Take`, `Kill`, `Replace`, `Sacrifice`, `Give`
  (`case give: Give` and `case "give"`) and `Exchange`.
- `ResourceOperationCodec`: every arm of `encodeResourceOperation`/
  `decodeResourceOperation` (`SpendSupply`, `GainSupply`, `FlipSecrets`,
  `Gain.Favor`, `Gain.Secrets`, `Gain.Warbands`), plus, from the piece
  helpers, `Burn` (`case burn: Burn` and `case "burn"`), `DrawNomadFavor`
  and `PayCost`.
- `CardOperationCodec`: every arm of `encodeCardOperation`/
  `decodeCardOperation`, and the private helpers only they use:
  `withActorless`, `decodeActorless`, `withCause`, `decodeCause`,
  `discardPosition`, `encodeShufflePile`, `decodeShufflePile`,
  `decodeShuffleOrder`.
- `DiceOperationCodec`: from the turn helpers, the arms for
  `ModifyDicePool`, `Roll`, `ModifyRollOutcome`, `Reroll` and
  `SetDieFaces`, and the private helper `decodeRerolledFaces`.
- `TurnOperationCodec`: the remaining arms of `encodeTurnOperation`/
  `decodeTurnOperation` except `ChronicleUpdated`, and the private helpers
  `encodePowerTiming` and `decodePowerTiming`.
- `ChronicleOperationCodec`: every arm of `encodeChronicleOperation`/
  `decodeChronicleOperation`, plus `ChronicleUpdated`
  (`case ChronicleUpdated(chronicle)` and `case "chronicle-updated"`) from
  the turn helpers, and the private helpers `positive` and `nonNegative`.

A helper keeps `private`. If the compiler says a helper you placed in one
family is used by another, move it to `OperationWireSupport` as
`protected final` instead of duplicating it.

- [ ] **Step 5: Rewrite `WalkerOperationCodec.scala`**

Replace the whole file with:

```scala
package distantlands.serialization

import distantlands.model._

/** Operation spellings for recorded walker steps, one codec trait per
  * operation family. `WalkerOperationWireSuite` and the family wire suites
  * pin every spelling.
  *
  * `encodeOperation` is total over `CoreOperation`, exactly like
  * [[encodeLocation]] in [[WalkerValueCodec]] (added for the same reason: a
  * bounded operation set is what let Catacombs' recorded `Move` out of
  * `Location.Deck` reach an append-time throw with no test to catch it).
  * Totality is exceptionless: the walker's control vocabulary lives under
  * the unsealed `Operation`, not under `CoreOperation`, so a control node
  * cannot reach this method. A new case in any operation family fails to
  * compile in its family's encoder until it is given a real arm, and a new
  * family fails to compile here.
  *
  * Each family codec's decoder is a partial function over the wire kind,
  * chained below; `WalkerOperationWireSuite` pins that no two families
  * claim one kind.
  */
private[serialization] trait WalkerOperationCodec extends PieceOperationCodec
    with ResourceOperationCodec with CardOperationCodec
    with DiceOperationCodec with TurnOperationCodec
    with ChronicleOperationCodec:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeOperation(operation: CoreOperation): ujson.Value =
    operation match
      case operation: PieceOperation => encodePieceOperation(operation)
      case operation: ResourceOperation => encodeResourceOperation(operation)
      case operation: CardOperation => encodeCardOperation(operation)
      case operation: DiceOperation => encodeDiceOperation(operation)
      case operation: TurnOperation => encodeTurnOperation(operation)
      case operation: ChronicleOperation => encodeChronicleOperation(operation)

  protected final def decodeOperation(value: ujson.Value,
      path: String): Either[WireError, CoreOperation] =
    decodePieceOperation(value, path)
      .orElse(decodeResourceOperation(value, path))
      .orElse(decodeCardOperation(value, path))
      .orElse(decodeDiceOperation(value, path))
      .orElse(decodeTurnOperation(value, path))
      .orElse(decodeChronicleOperation(value, path))
      .applyOrElse(value("kind").str, (other: String) => Left(InvalidValue(
        s"$path.kind", s"unknown recorded walker operation '$other'")))
```

The family codecs already mix in `CampaignResultCodec`, `FollowUpWire`,
`OathkeeperWire`, `PositionWire` and `WalkerValueCodec`, so
`WalkerEventCodec`, which extends `WalkerOperationCodec`, still sees every
member it uses. If the compiler says otherwise, add the missing mixin to
`WalkerOperationCodec`'s `extends` clause.

Update the stale pointers: in `PositionWire.scala`, `OathkeeperWire.scala`,
`WalkerValueCodec.scala` and `CampaignResultCodec.scala`, a doc comment
says the trait was split out of `WalkerOperationCodec` for headroom. Leave
those sentences: they are history and still true.

- [ ] **Step 6: Compile, test, and prove the wire did not move**

Run: `./sbtw Test/compile`
Expected: success, no warning.

Run: `wc -l src/main/scala/distantlands/serialization/*OperationCodec.scala src/main/scala/distantlands/serialization/OperationWireSupport.scala`
Expected: every file under 300 lines; `WalkerOperationCodec.scala` about 60.

Run: `./sbtw "testOnly distantlands.serialization.*"`
Expected: PASS.

Run: `./sbtw test`
Expected: PASS.

Run: `git status --short src/test/resources`
Expected: nothing.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/serialization/OperationWireSupport.scala \
  src/main/scala/distantlands/serialization/PieceOperationCodec.scala \
  src/main/scala/distantlands/serialization/ResourceOperationCodec.scala \
  src/main/scala/distantlands/serialization/CardOperationCodec.scala \
  src/main/scala/distantlands/serialization/DiceOperationCodec.scala \
  src/main/scala/distantlands/serialization/TurnOperationCodec.scala \
  src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala \
  src/main/scala/distantlands/serialization/WalkerOperationCodec.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "refactor(serialization): split the operation codec by family" \
  -m "Each operation family has its own codec trait with its encode match and decode partial function, and WalkerOperationCodec only dispatches. The wire format is unchanged, and a new test pins that no two families claim one wire kind." \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Split the operation wire suite by family

**Files:**
- Create: `src/test/scala/distantlands/serialization/PieceOperationWireSuite.scala`
- Create: `src/test/scala/distantlands/serialization/ResourceOperationWireSuite.scala`
- Create: `src/test/scala/distantlands/serialization/CardOperationWireSuite.scala`
- Create: `src/test/scala/distantlands/serialization/TurnOperationWireSuite.scala`
- Create: `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`
- Modify: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Consumes: Task 3's codec (unchanged behavior).
- Produces: five suites; `WalkerOperationWireSuite` keeps the cross-family
  tests. Dice has no test of its own, so there is no dice suite.

- [ ] **Step 1: Record the test count**

Run: `./sbtw "testOnly distantlands.serialization.*"` and note the number of
tests run. After the split the same number must run.

- [ ] **Step 2: Create the five suites and move the tests**

Each new suite has this shape, in package `distantlands.serialization`:

```scala
package distantlands.serialization

// Copy WalkerOperationWireSuite's whole import block here; Step 3 trims it.

/** Walker steps carrying <family> operations on the wire: their spellings,
  * round trips, and the typed errors for ones the codec refuses. */
class <Family>OperationWireSuite extends munit.FunSuite:
  // the moved tests
```

Move each test below verbatim (the whole `test(...)` call and its body),
in the order listed, from `WalkerOperationWireSuite`. A test is named by
the start of its title.

- `PieceOperationWireSuite` (doc: "piece"):
  - "a recorded move of a Vision to the shared bank round trips and replays"
  - "a move names its mover on the wire, and a site card's move"
  - "a replace names its performer on the wire, and one nobody"
  - "a Take's leaving operations round trip, and a Take without them"
  - "a secret move's side round trips, and a move without one writes"
- `ResourceOperationWireSuite` (doc: "resource"):
  - "reduced optional spend is canonical in memory and on the wire"
  - "a burn's burner round trips, and a burn no player makes writes"
  - "a draw from the Nomad bank round trips"
- `CardOperationWireSuite` (doc: "card"):
  - "an actorless discard says so on the wire"
  - "a discard's cause round trips, and a discard without one writes"
  - "a discard's pile position round trips, and a discard that names"
  - "a swap names its mover on the wire"
  - "a draw written before it had a position decodes as a draw from the"
- `TurnOperationWireSuite` (doc: "turn"):
  - "recorded Visions Drawn advancement round trips without an amount"
  - "a recorded round advance round trips and replays as one more round"
  - "a journalled phase change round-trips the phase it names"
  - "a deferral and its take round trip, with the held ops"
  - "a pick record's opening, with and without a label, and its closing"
  - "an as-if site round trips"
  - "an as-if adviser holder round trips"
- `ChronicleOperationWireSuite` (doc: "Chronicle"):
  - "an unknown Chronicle task and non-positive lineage points or Imperial"
  - "a foundation restore names its number, and an unknown number"
  - "a site placement names its Atlas end, its region and the site's"

`WalkerOperationWireSuite` keeps, in their current order: "walker delta
events round-trip the node path each one ran at", "Catacombs' recorded
batch round-trips", "every Location variant round-trips", "the gathering
pile is card-gathering on the wire", "an unknown walker location kind,
deck, operation kind or negative", "a WalkerStepRecorded carrying two
contribution ids round-trips", "a delta's operations are written under its
payload", "an unencodable die face returns a typed WireError", "every
CoreOperation variant round-trips", "each wire kind belongs to one
operation family", the two "a walker roll payload round-trips" tests, and
"every Piece variant round-trips". Update its doc comment to:

```scala
/** Walker step events on the wire across operation families: locations,
  * pieces, payloads, the step envelope, every variant of the vocabulary,
  * and the wire kinds' partition into families. Each family's own
  * spellings are pinned in its `<Family>OperationWireSuite`. */
```

If a moved test uses a private helper or value of `WalkerOperationWireSuite`,
move that member with it when only moved tests use it; if tests in two
suites use it, move it to `WalkerWireEvents` (the shared test object) as a
`def` and call it from both.

- [ ] **Step 3: Trim the imports**

Run: `./sbtw Test/compile`
Expected: the unused-import warnings `-Werror` turns into errors, naming
each suite's unused imports. Delete each one it names and rerun until the
compile succeeds with no warning.

- [ ] **Step 4: Run the suites**

Run: `./sbtw "testOnly distantlands.serialization.*"`
Expected: PASS, with the same number of tests as Step 1.

Run: `wc -l src/test/scala/distantlands/serialization/*OperationWireSuite.scala`
Expected: every file under 500 lines.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/serialization/PieceOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/ResourceOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/CardOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/TurnOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git commit -m "test(serialization): split the operation wire suite by family" \
  -m "Each operation family's wire tests move verbatim to its own suite, and WalkerOperationWireSuite keeps the tests that cross families. The same tests run." \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

If Step 2 moved a helper into `WalkerWireEvents.scala`, add that file to
the `git add` too.

---

### Task 5: Docs, and the verification gate

**Files:**
- Modify: `docs/ROADMAP.md` (delete one item)
- Modify: `CONTEXT.md` (one new term)
- Modify: `docs/architecture/codebase-structure.md` (one new subsection)
- Modify: `docs/architecture/follow-up-actions.md`
- Modify: `docs/rules/implementation-traceability.md`
- Modify: `docs/testing/audits/split-delta-bracketing-audit.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-12-operation-families-design.md` (status line)

- [ ] **Step 1: Delete the roadmap item**

In `docs/ROADMAP.md`, delete the whole item that starts
`- [ ] **\`CoreOperations.scala\` is at the file limit.**` and ends with
`  2026-11-12.`, nine lines.

- [ ] **Step 2: Add the term to `CONTEXT.md`**

Under `### Walker`, after the **Board draft** entry (the last entry before
`### Note`), add:

```markdown
**Operation family**:
One sealed area of Oath's operation vocabulary, in its own file in `model`:
pieces, resources, cards, dice, the turn, or the Chronicle. `CoreOperation`
is the union of the families, so a match over it stays exhaustive, and a
new operation is a case of exactly one family.
_Avoid_: operation group (the operation codec's old name for it), operation category
```

- [ ] **Step 3: Add "Adding an operation" to `codebase-structure.md`**

In `docs/architecture/codebase-structure.md`, after the `### Event`
subsection's last line ("Update replay and malformed-wire tests; do not
serialize Scala class names."), add:

```markdown

### Operation

Add the case to the one operation family it belongs to
(`model/<Family>Operation.scala`): a leaf also extends `LeafOperation`, a
composite exposes its `children`. The compiler then names the rest: the
encode arm in the family's codec (`serialization/<Family>OperationCodec.scala`),
a decode arm beside it, and, for a leaf, its arm in `LeafApplication`,
which may say the leaf changes nothing there. Pin the spelling in the
family's wire suite and add the operation to `everyOperation` in
`WalkerOperationWireSuite`. A new family joins the `CoreOperation` union in
`CoreOperations.scala` and the family list in
`scripts/check-architecture.py`.
```

- [ ] **Step 4: Repoint the three docs that name the old file**

`docs/architecture/follow-up-actions.md`, replace
`` atFront)` (`model/CoreOperations.scala`, applied by `TurnStateOperations`), ``
with
`` atFront)` (`model/TurnOperation.scala`, applied by `TurnStateOperations`), ``.

`docs/rules/implementation-traceability.md`, in the "Bury semantics" row,
replace `` The `Bury` operation (`model/CoreOperations.scala`) `` with
`` The `Bury` operation (`model/CardOperation.scala`) ``.

`docs/testing/audits/split-delta-bracketing-audit.md` is a dated record;
keep its findings and only correct the two paths. Replace
`` the children each composite declares in `model/CoreOperations.scala`, and ``
with
`` the children each composite declares in its operation family (`model/*Operation.scala`), and ``,
and replace
`` are never recorded. The composites, from `model/CoreOperations.scala`: ``
with
`` are never recorded. The composites, from the operation families (`model/*Operation.scala`): ``.

Run: `git grep -n "CoreOperations.scala" -- docs/architecture docs/rules docs/testing docs/ROADMAP.md CONTEXT.md src`
Expected: only lines in `codebase-structure.md`'s new subsection and in
`src/main/scala/distantlands/model/CoreOperations.scala`.

- [ ] **Step 5: Mark the spec built**

In the spec, replace its status paragraph, from `**Status:** designed
2026-11-12` through `"Probe result".`, with:

```markdown
**Status:** designed 2026-11-12, approved section by section by the owner,
and built by the [operation families plan](../../plans/2026-11/2026-11-12-operation-families.md).
Two throwaway probes (Scala 3.9.0, `-Werror`) confirmed the approach; see
"Probe result".
```

- [ ] **Step 6: Run the verification gate**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, with the coverage minimums `build.sbt` sets.
`GameLogDecisionSuite`'s "decisions an action line already
tells post no Chose line" has hit munit's 30-second timeout under coverage
before; if only that test times out, run the gate again; if it fails a
second time, or any other test fails, stop and report.

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `./sbtw "frontend/fullLinkJS"`
Expected: success (the optimized Scala.js linker).

Run: `python3 scripts/check-architecture.py`
Expected: `architecture check passed`.

Run: `python3 scripts/check-markdown-links.py`
Expected: `Markdown link check passed`.

Run: `git diff --check 0.2.0-alpha` and
`git status --short src/test/resources`
Expected: no output from either.

Run: `wc -l src/main/scala/distantlands/model/*Operation*.scala src/main/scala/distantlands/serialization/*OperationCodec.scala`
Expected: every file at or under 300 lines.

- [ ] **Step 7: Commit**

```bash
git add docs/ROADMAP.md CONTEXT.md docs/architecture/codebase-structure.md \
  docs/architecture/follow-up-actions.md docs/rules/implementation-traceability.md \
  docs/testing/audits/split-delta-bracketing-audit.md \
  docs/superpowers/specs/2026-11/2026-11-12-operation-families-design.md
git commit -m "docs: record the operation families and how to add an operation" \
  -m "Add Operation family to CONTEXT.md and an Adding an operation note to the codebase structure, repoint the docs that named CoreOperations.scala as the operations' home, drop the roadmap's file-limit item, and mark the spec built." \
  -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
