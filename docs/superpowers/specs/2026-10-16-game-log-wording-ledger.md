# Game Log Wording Ledger

Lines whose words are worth improving, found while reading the Game Log and
left alone. Not a design: nothing here is decided, and nothing here is
authorized.

The [procedures journal their own notes design](2026-10-16-procedures-journal-their-notes-design.md)
forbids changing wording inside a migration (its ruling 6), so that a golden
diff means one thing. This file is where the improvements wait. Append to it
when a migration passes a line it may not fix; strike an entry when a commit
fixes it, naming the commit.

Each entry: where the line is built, what it reads today, and why it is worth
a second look. No replacement text is proposed — picking the words is the
work, not the finding.

## Open

_(none)_

## Fixed

Settled by the [wording audit](../../testing/audits/game-log-wording-audit.md)
of 2026-11-02, with the owner's answers to its questions.

- **`CampaignNotes.skipped` and `CampaignNotes.nothingToAttack`** now read
  "Skipped the Campaign" (a `Decision` line) and "Ended the Campaign with
  nothing left to attack" (an `Action` line), and `CampaignNotes.sentTo`
  reads "Sent {Blue}'s pawn to {site}". Commit 3585de92.
- **`SetupProcedure.placedPawn`** reads "{Blue} placed their pawn at
  {site}", and `TravelProcedure.travelled` "Traveled to {site}", the
  rulebook's spelling. Commit abd42cf9.
- **`TradeProcedure.tradedForNoSecrets` and `tradedForNoFavor`** read
  "Traded with {card} and gained no secrets" (or favor). Commit a5935931.
- **The plan activation lines** say "used", the rules' verb: "Used {card}",
  "{Blue} used {card}", "The bandits used {card}", "{Blue} used the
  Oathkeeper title". Each plan line still names only its card; the totals
  tell what it did. Commit 3e320136.
- **`StartLines.continuedRecover`** reads "Continued recovering" with its
  Supply cost. Commit cd7339f2.
- **A facedown card's slot** (below): the count fallback holds everywhere.
  A choice naming a facedown adviser the viewer cannot identify reads "a
  Denizen from {Blue}'s advisers", and `LogWords.slotted` is gone. Commit
  3e82dd70.

## Closed

Entries the audit judged fine as they stand, with its reason.

- **`CampaignNotes.lost` against `playerLost`.** The unnamed actor and the
  named other player is the convention every line follows
  (`LogWords.subject`); naming the actor everywhere would be a log-wide
  change, and the owner kept the convention.
- **`SearchProcedure.keptCards`, "kept none".** The keep decision requires
  at least one card, so a Search cannot keep nothing; the one power that
  empties the hand (Spinning Bee) writes its own line.
- **`CardPlayProcedure`'s placement after a Search's "Kept".** Two answers
  in two commands; dropping either line would hide a step.
- **A Campaign's totals after the plan that rewrote them.** The reason, then
  the number it produced, is the order a reader follows.
- **A defeated Raid defender's losses as three sentences.** They are three
  rule steps, and other powers' lines can land between them.
- **A Campaign's loss lines before the victory's gains.** That is the
  rulebook's order: kill defeated warbands, then resolve the victory.

## A facedown card's slot, in a negotiation only

_Settled: the count fallback, everywhere (see Fixed)._

`LogWords.slotted` (`LogWords.scala:62-76`) names a card the viewer cannot
identify by its row and 1-based slot, "facedown adviser (slot 2)". Nothing
else in the Game Log does: every other card word falls back to "a Denizen".
Slice 2 migrated Negotiation's two card lines to `NoteArg.Card` and so
dropped the slot for a viewer outside the deal. `slotted` keeps its other
caller, `ChoiceWords.scala:34`, which describes a parked adviser choice, so
the slot survives in one place and not the other.

The question is which fallback is right everywhere. A slot tells a reader
*which* of a player's facedown advisers moved, which a count does not; it
also leaks a position the deal's parties may not have shown. Decide it once
and apply it to every card line, not to one procedure and one choice.

## A Trade that gained nothing names the right resource

`ActionLines.tradeLines` posted its completion fallback with the literal
`"no secrets"`, so a Trade **for favor** whose suit bank was empty read
`Traded with Hiding Place for no secrets`. No test or golden covered the
branch. `TradeProcedure.tradedForNoFavor` now reads `… for no favor`
(procedures journal their own notes, slice 3 plan-time ruling 3).

The same rule matched every bank-to-player move in the event it formatted, so
a Trade a power gained again on — The Old Oak's bonus secret, Mounted
Library's doubling — posted a second `Traded with …` line. A probe run at
plan time measured it: a Trade for secrets on The Old Oak, with a beast
adviser held, posted two identical `Traded with The Old Oak for 1 secret`
lines. The note posts once, naming what the Trade itself gained.

## Search's one line became two, and its draw powers speak for themselves

`ActionLines.search` wrote `Drew {cards} from {pile} and kept {cards}` as one
line, from the journal. A note cannot: the kept set is in the action's
answers, the drawn set is in state, and the pile phrase was read off the
`Draw` operation **as powers had rewritten it**. The keep answer arrives in a
different walker command than the draw, so nothing can see all three at once
(procedures journal their own notes, slice 3 plan-time ruling 1).

The line is now two — `Drew {cards} from {pile}` at the draw, `Kept {cards}`
at the keep answer — and the three phrases the rule read off the operation
are lines of the powers that cause them (ruling 5):

- **Wine of Welcome** — "Drawing from the bottom of the World Deck".
- **Mushrooms** — "Drawing one card from the bottom of the {region} discard".
- **Painted Trumpet** — "Drawing the top card of each discard pile instead".
- **Spinning Bee** gains a second line, "Drew {cards}, and discarded them
  all", for its no-match draw. That is the one case where neither of
  Search's own lines is written at all: this power empties the temporary
  hand inside the draw's own window.

Each of the first three replaces a phrase the Search line used to carry, so
nothing is lost; Painted Trumpet's line corrects the pile the Search line
names, which is the pile the Search was priced at rather than the piles drawn
from.

### A Search above five cards is counted, not listed

The rule called `LogWords.cards` at any length. A note's `Cards` argument
goes through `LogWords.listed`, which above `LogWords.Inline` (5) writes
`6 cards` when the viewer identifies none of them, and a `LogSpan.Cards` span
when it identifies some. Six drawn cards is reachable — three printed, one
more from Augury, two more from Truthful Harp — so a Search that large now
reads `Drew 6 cards from the World Deck` for a viewer who sees only backs,
where the rule wrote `Drew 6 Denizens`. `ProcedureNoteLinesSuite` pins the
all-backs branch; the actor's own view of six takes `listed`'s span branch,
which needs a script with both draw extensions picked and is recorded here
rather than tested.

## A card play's line now follows the lines its own batch caused

`ActionLines.playedAdviser` anchored "Played {card} …" on the play's own
recorded batch, and its `!(index + 1 until at).exists(isDelta)` guard kept a
later delta from taking the line. The note sits after the play leaf instead,
and `WalkerSteps.recordBatch` runs `AdviserArrivalPass.after` and any
executed trigger's follow-up **inside** that leaf's own execution. So a power
whose effect fires during the play writes its line first and the play's line
reads second, where it used to read first.

The only such power today is `SuitOnlyAdvisers` (Family Wagon, Vow of Union):
"Family Wagon: Buried Wrestlers." now precedes "Played Family Wagon as an
adviser". No golden covers it, which is why the slice's golden proof did not
show it.

Putting the line back in front needs it journaled inside the play's own
batch, and a `Note` is not a `CoreOperation`; the alternative — reading the
destination before the play — is the inference this slice deleted, since the
regional pile a discard reaches is decided by `CardPlay.operations`. The
question is unchanged and is no longer waiting on slice 4: it is a ROADMAP
task, "A card play's line before the lines its own batch causes".
