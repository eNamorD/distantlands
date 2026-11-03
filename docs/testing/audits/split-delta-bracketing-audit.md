# Audit: do a split delta's steps bracket their operation?

> Status: settled, 2026-11-03. The owner answered question 1 against the
> default: `Discard.Relic` and `Discard.RuinedEdifice` stay whole and post
> "{Blue} set aside {card}" (commit 239e8c85, which also made `OpStep` true
> by construction, slice 1). For question 2 the owner asked for the
> project's testing guidance to decide; it ruled out both tests below.
> The `parts` unit test restates `parts`' whole list in a second
> hand-written list, which drifts and only fails when someone remembers
> it. The property test guards an error that cannot happen once a part is
> judged at its step's states: the engine applies a composite atomically,
> so those states are the only honest bracket, and `delta` now takes the
> part beside its step instead of a hand-built `OpStep`. The new lines
> have focused tests in `GameLogRareLinesSuite`. Slice 2 is therefore not
> done; slice 3 landed as commit 287b3023 and slice 4 (commit 59aa8a3e) as a `formatAppended`
> that folds each appended batch rather than checking one it was handed.
>
> Written as a read-only audit, 2026-11-02, of the ROADMAP item "Check that a
> split delta's steps bracket their operation" under Game Log deferred
> items, which the
> [log operation strictness decision](../../superpowers/specs/2026-10-19-log-operation-strictness-decision.md)
> raised as a separate candidate. Nothing here changes code. Line numbers
> are those of the tree at commit `0d1ae92`.

Vocabulary: [CONTEXT.md](../../../CONTEXT.md) defines **note**, **note
boundary** and **command batch**. The
[Game Log design](../../superpowers/specs/2026-09-25-game-log-design.md),
"Input" and "Visibility", states the rule this audit checks: every line and
every visibility judgement is made against the state immediately before and
after the operation it describes.

## Summary

- No detail line is wrong today. Every rule that reads state off a split
  part fires either on a composite `parts` keeps whole, on a leaf no
  composite contains, or on the only child of a single-child composite. The
  roadmap's "latent wrong-visibility bug" has no instance in the current
  rule set.
- The invariant is held by the coincidence of three lists that nothing
  checks against each other: the composites `DetailLines.parts` keeps whole,
  the children each composite declares in `model/CoreOperations.scala`, and
  the patterns `DetailLines.delta` matches. A new rule that matched a child
  of `PayCost`, `Discard.Relic`, `Take` or `Swap` and read `before`/`after`
  would read the whole step's states and nobody would be told.
- The engine applies a composite atomically. `OperationApplication
  .applyFamilies` flattens the composite's leaves and applies them by family
  (counted moves, pawns and banners, cards, then the non-move leaves), so no
  state "between two children" ever exists. A per-part bracket therefore
  cannot be computed honestly; the whole step's states are the only bracket a
  part has. The fix is to make the code say so, not to invent intermediate
  states.

## The code under audit

`DetailLines.deltas`
(`src/main/scala/oathdigital/application/gamelog/DetailLines.scala:69-76`):

```scala
val found = journal.ops(at).flatMap(step =>
  DetailLines.parts(step.operation).flatMap(part =>
    delta(run, OpStep(part, step.before, step.after), placedDiscard,
      playedFaceup, viewer)))
```

`DetailLines.parts` (`DetailLines.scala:189-197`) keeps `Gain.Favor`,
`Gain.Secrets`, `Draw`, `Discard.Denizen`, `Discard.Vision` and `Reveal`
whole, returns any `LeafOperation` as itself, and opens every other
composite into its `CoreOperation` children, recursively.

`OpStep` (`LogJournal.scala:8-11`) is documented as "one operation of a
recorded batch with the state just before and just after it". `LogJournal
.opSteps` (`LogJournal.scala:120-132`) builds it honestly, one recorded
operation at a time through `ProcedureWalker.applyRecordedBatch`. The wrap
at `DetailLines.scala:75` is the one place that builds an `OpStep` whose
states are not its operation's.

The rules that read a part (`DetailLines.delta`, `DetailLines.scala:90-147`)
read state in three places:

| Reader | Reads | Lines |
| --- | --- | --- |
| `card(id)` = `words.card(id, before, after, viewer)` | whether the viewer identifies the card in `before` or `after` (`LogWords.scala:46-59`) | 98, 126, 130, 133, 135, 138, 140, 146 |
| `faceDown(before, id)` | the card's orientation in `before` (`DetailLines.scala:165-168`) | 144 |
| `whose(kind, amount, actor, before)` | the players' lineages in `before` (`DetailLines.scala:151-163`) | 122 |

Every other `OpStep` reader in the package matches on the operation alone
or on a leaf that no composite contains: `StartLines.scala:117-119,
127-128, 143-144` (`SpendSupply`, favor and secret `Move`s,
`ModifyDicePool`, `RecordCampaignResult`, unsplit), `LogJournal.supplySpent`
(`LogJournal.scala:95-100`, unsplit `SpendSupply`), `GameLogFormatter
.headlines` (`GameLogFormatter.scala:263-274`, `BeginTurn` and `EnterPhase`
leaves, reading `before.turn.phase`), `NoteLines.scala:220`
(`RecordPowerUse` leaf) and `DetailLines.roll` (`DetailLines.scala:32-37`,
`Reroll` leaf reading `after`).

## Every operation kind that can be split

A recorded delta is a `Vector[CoreOperation]`
(`gameplay/walker` `DeltaPayload`), so only the sealed `CoreOperation`
family reaches `parts`. Walker control nodes are not `CoreOperation`s and
are never recorded. The composites, from `model/CoreOperations.scala`:

### Kept whole by `parts` — OK by construction

| Composite | Children | Verdict |
| --- | --- | --- |
| `Gain.Favor` (315-322) | one `Move` | OK: `parts` returns it whole |
| `Gain.Secrets` (324-328) | one `Move` | OK |
| `Draw` (292-304) | `Take.children` per card | OK: whole; the drawn cards' visibility is judged at the `Draw`'s own states, as the spec's "a card drawn into a hand" case needs |
| `Discard.Denizen` (198-210) | card `Move`, favor `Move`, secrets `Move`, `FlipSecrets` | OK: whole; `card()` reads the discard's own states |
| `Discard.Vision` (214-217) | one `Move` | OK |
| `Reveal` (452-455) | one `Flip` | OK |

### Split into a single child — OK, the part's bracket is the step's

| Composite | Child | Rule it can reach | Verdict |
| --- | --- | --- | --- |
| `Burn` (133-151) | `Move` to `SharedBank` | none | OK, silent |
| `Gain.Warbands` (330-336) | `Move` bank to play area | none (`Move` warbands rule needs a `Site` destination) | OK, silent |
| `Give` (340-347) | `Move` with `by` | warbands to a site (`whose`, lineages only); a card `Move` has no orientation so 141-146 cannot match | OK |
| `Kill` (412-416) | `Move` to a warband bank | none | OK, silent |
| `Play` (421-431) | `Move` with `resultingOrientation` | 141-146 ("revealed"), which reads `faceDown(before, id)` and `card()` | OK: `Play` has exactly one child, so `step.before`/`after` are the `Move`'s own. This is the only split part on which a visibility rule fires. `CardPlay.adviserPlan` (`gameplay/actions/CardPlay.scala:157-161`) builds it for a facedown adviser played faceup to the advisers |
| `Sacrifice` (458-462) | `Kill.children`, one `Move` | none | OK, silent |
| `Take` without `leaving` (492-500) | one `Move` with `by` | favor from a bank or secrets from the shared bank (108-116, no state read); a card `Move` has no orientation | OK |

### Split into several children — OK today, suspect by construction

| Composite | Children | Rules reached | Verdict |
| --- | --- | --- | --- |
| `Discard.RuinedEdifice` (221-233) | card `Move` to the Edifice deck bottom, favor `Move` to a bank, secrets `Move` to a play area, `FlipSecrets` | none: the card goes to a deck, the favor leaves a card rather than a bank, the secrets leave a card rather than the shared bank | OK, silent. Suspect: a reader would expect a "Discarded" line and the nearest rule (`Discard.Denizen`, 127) would need the composite, not a part |
| `Discard.Relic` (238-245) | card `Move` to `SetAsideRelics`, secrets `Move`, `FlipSecrets` | none | OK, silent, same remark |
| `Exchange` (307-312) | `give.children ++ receive.children`, two `Move`s with `by` | warbands to a site (`whose`) only | OK: lineages do not change inside a batch |
| `PayCost` (375-407) | up to two `Move`s from the payer's play area, `FlipSecrets` | none: 108-116 need a bank as the source | OK, silent |
| `Replace` (435-446), `simultaneous` | `Move` removed to a bank, `Move` replacements bank to `at` | the second matches 117-123 when `at` is a site; `whose(before)` reads lineages only | OK |
| `Swap` (466-476), `simultaneous` | two card `Move`s with `by`, no orientation | none | OK, silent |
| `Take` with `leaving` (492-500) | `leaving` (a favor `Move` off a banner, or a `Burn`, which `parts` opens again), then the `Take`'s `Move` | none: banner favor leaves `OnBanner`, the burnt secrets go to the shared bank, the banner itself is not a card | OK, silent. Built by `CampaignRaid.takes` (`gameplay/actions/campaign/CampaignRaid.scala:78-93`) and `ConspiracyWhenPlayed.take` (`gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala:117-122`) |

No composite today has two children that both concern the same card, and no
multi-child composite has a child that any card-visibility rule matches. The
two `simultaneous` composites (`Swap`, `Replace`) are the ones for which
"apply the children one at a time from `step.before`" would be unsound: a
`Swap`'s first `Move` puts two cards in one slot before the second leaves,
which the executor's shape guard may refuse.

## What the existing tests pin

- `GameLogRareLinesSuite`, "gains, draws, buries, peeks and reveals each post
  one line" (`src/test/scala/oathdigital/application/gamelog/GameLogRareLinesSuite.scala:63-87`)
  pins the per-operation bracket across *recorded operations*: the `Peek`
  names no card, the `Reveal` names it and the `Bury` after it names it,
  because `opSteps` gave each its own states. It exercises no split.
- `GameLogDetailSuite`, "a facedown adviser played faceup is told once, by
  the action line" (`GameLogDetailSuite.scala:59-63`) walks the only split
  part a visibility rule reaches (`Play`'s `Move`), but asserts the action
  line, not the suppressed "Revealed" delta's visibility.
- `GameLogPropertiesSuite`, "no card span names a card its viewer cannot
  identify" (`GameLogPropertiesSuite.scala:101-120`) is the safety net: it
  checks every card span of every script against `identifiesAt` over the
  states of the whole run, so a part that leaked a name the viewer never had
  would fail it. It would not catch the opposite error, a back shown where
  the part's own states would have named the card, and it checks only the
  scripts' operations.
- No test calls `DetailLines.parts` directly, and nothing asserts which
  composites it keeps whole.

## The test that would pin it

The roadmap asks for "a test that splits a step with a hidden part before
changing anything". Because no production composite reaches a visibility
rule through a multi-child split, such a test cannot be written against a
real card line without first adding the rule it is meant to guard. Two
tests pin the invariant as it stands:

1. **Split parts carry no card word.** In `GameLogPropertiesSuite`, for
   every script, every step and every recorded operation whose
   `DetailLines.parts` has more than one element, assert that the `Found`s
   `delta` posts for those parts contain no `LogSpan.Card` and no
   `CardWord.Back`. `DetailLines` is `private[gamelog]`, so the suite can
   call `parts` and reach `delta` through a small `private[gamelog]`
   accessor, or assert on the posted lines of the step filtered to
   `LogKind.Delta`. The failure message names the composite, so the day a
   rule matches a `PayCost` or `Take` child it says which.
2. **`parts`' whole list is the visibility rules' list.** A unit test that
   asserts `parts(op) == Vector(op)` for one instance of each composite the
   rules name (`Gain.Favor`, `Gain.Secrets`, `Draw`, `Discard.Denizen`,
   `Discard.Vision`, `Reveal`) and `parts(op).size == op.children.size` for
   each other composite, built from legal values. It turns the implicit
   coupling between `parts` and `delta` into a stated fact.

If a rule for a multi-child composite is ever wanted (a "Discarded" line for
`Discard.Relic`, a "Paid" line for `PayCost`), the honest shape is to match
the composite whole, as `Discard.Denizen` does, and add it to `parts`' whole
list; the test above then has to be extended with it, which is the point.

## Making `OpStep` true by construction

The cheapest change that makes `OpStep`'s doc comment true on every path:
`deltas` stops wrapping parts in `OpStep` and passes `(part, step)` to
`delta`, whose signature becomes `delta(run, part: CoreOperation, step:
OpStep, ...)`. Nothing else changes: `delta` already destructures the states
out of the `OpStep`, and the three state readers keep reading the step's.
`OpStep` is then only ever built by `LogJournal.opSteps`. The design's
"Input" paragraph should gain one sentence: a composite is applied as one
operation, so a line about one of its children is judged at the composite's
states.

## The two leftovers beside it

- **`GameLogFormatter.format` re-applies the journal once per standing
  marker** (`GameLogFormatter.scala:59-89`). `formatLive(live(at), viewer)`
  builds a fresh `LogJournal`, whose constructor applies every operation of
  every step (`LogJournal.scala:39`), once for the live journal and once per
  standing `StepTakenBack`, per request, per seat. The voided-batch
  formatting only needs the entries inside `[marker.from, marker.until)`,
  so one `LogJournal` over the full prefix, with `covered`/`segmentEnd`
  computed against the live prefix as it stood, would do; alternatively the
  live `LogJournal`'s `batches` can be shared since a voided step's
  operations and states are the same values in every prefix. Not a
  correctness issue; it is the cost that scales with undo count.
- **`formatAppended` takes `stateBefore` and `lastStateAfter` unchecked**
  (`src/test/scala/oathdigital/application/gamelog/LogScripts.scala:220-233`).
  Strict `opSteps` throws on an operation that does not apply from
  `stateBefore(last)`; it does not notice a `lastStateAfter` that the
  operations never produce, so a test can still assert a line read from an
  `after` the batch would not reach. A check that `lastStateAfter(last)`
  equals the fold of the appended operations from `stateBefore(last)`, when
  the events hold a `DeltaPayload`, closes it; tests that patch
  `lastStateAfter` for a non-delta event (a `LogNoted`) are unaffected.

## Questions for the owner

1. Should a `Discard.Relic` (a Raid defender's facedown relic set aside) and
   a `Discard.RuinedEdifice` post a generic "Discarded" or "Set aside" delta
   line? Today both are silent at the detail level; the Campaign's
   `defenderLost` note covers the Raid case. Recommended default: no new
   line; keep the note.
2. Is the test in "The test that would pin it" worth its cost as a property
   over every script, or is the unit test on `parts` enough? Recommended
   default: both; the property test is the one that fires when a rule is
   added.

## Recommended next slices

Each is one session.

1. **Make `OpStep` true by construction.** Change `deltas`/`delta` to pass
   `(part, step)`, fix `OpStep`'s doc, add the "Input" sentence to the Game
   Log design, add the `parts` unit test. No golden changes.
2. **Pin the split invariant in `GameLogPropertiesSuite`.** The property
   test above, with a `private[gamelog]` seam if needed. No production
   change beyond the seam.
3. **One `LogJournal` per request.** Share the live journal's applied
   batches across the per-marker formattings in `GameLogFormatter.format`;
   add a test that a journal with N standing markers applies each operation
   once (count through a test `OperationExecutor` or by timing in
   `profile-tests.py`).
4. **Check `formatAppended`'s staged states.** Add the fold check in
   `LogScripts.formatAppended` for delta events, run the suite, fix any test
   it exposes with legal staging as the strictness decision did.
