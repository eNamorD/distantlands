# Audit: the Game Log lines the wording ledger collects

> Status: implemented, 2026-11-03, with the owner's answers: the
> recommended default for every question below, and every recommended
> slice landed; the [ledger](../../superpowers/specs/2026-10-16-game-log-wording-ledger.md)
> names each commit.
>
> Written as a read-only audit, 2026-11-02, of the ROADMAP item "Improve the
> Game Log lines the wording ledger collects" under Game Log deferred items.
> It reads every entry of the
> [wording ledger](../../superpowers/specs/2026-10-16-game-log-wording-ledger.md)
> against the code that builds the line and the golden logs under
> `src/test/resources/gamelog/`, classifies each, and proposes replacement
> wording. Nothing here changes code or wording. Line numbers are those of
> the tree at commit `0d1ae92`.

Vocabulary: [CONTEXT.md](../../../CONTEXT.md) defines **note**, **note
key** and **wording**. Rulebook terms follow
[docs/rules/glossary.yaml](../../rules/glossary.yaml) and the New
Foundations rules text, which spells *traveling* and *burn* the American
way (`rules.yml`, "Travel Powers on Sites"; Raid victory step 3, "The
defender burns half of their favor").

## The house conventions the lines are judged against

Read off the lines that are not in the ledger, so a flagged line is judged
against its neighbours rather than against taste:

- **The actor goes unnamed; anyone else is named.** `LogWords.subject`
  (`src/main/scala/oathdigital/application/gamelog/LogWords.scala:21-25`)
  writes "Verb " for the run's actor and "{Blue} verb " for others. Every
  procedure line follows it: "Travelled to", "Mustered 1 warband with",
  "Took 1 favor from", "Kept", "Drew", "Forged", "Recovered", "Failed to
  recover at", "Negotiated with". Setup names everyone, because Setup has no
  turn (`SetupProcedure.scala:50-51`).
- **Past tense, no full stop, a determiner before a noun.** "Discarded
  Tinker to the Provinces discard", "Drew X from the World Deck", "Placed
  favor on X". Power notes end with a full stop and open with the source
  card ("Captains: Campaign at Deep Woods."), which separates them from
  procedure lines at a glance.
- **A number before its noun, pluralised by `NotePart.Plural`.** "Mustered
  2 warbands", "for 2 secrets", "Attack: 6 with 2 skulls".
- **Rulebook verbs.** *use* a battle plan (`rules.yml`, Campaign step
  "Use battle plans", and "Battle Plans"), *burn* favor, *sacrifice*
  warbands, *kill* warbands, *bury* a card.

## The ledger's open entries, classified

Ranked by how wrong the line reads to a player, then by how cheap the fix
is. "Where" names the key and the wording's source line; "Produces" is the
line as a golden log shows it.

### 1. `CampaignNotes.skipped` and `nothingToAttack` — two registers, two full stops

- Where: `gameplay/actions/campaign/CampaignNotes.scala:115-118`, written by
  `endedNote` (299-301); kind `LogKind.Delta` for both
  (`application/gamelog/ProcedureLog.scala:232-233`).
- Produces: "Skipped the Campaign." and "Nothing left to attack: the
  Campaign ended."
- Verdict: **reads wrong**. These are the only procedure lines in the log
  that end with a full stop, and the second is the only one shaped as
  "reason: outcome". The first is the actor's choice, the second a forced
  end; as alternatives at one place they should share a shape. Both are
  posted as `Delta` while "Skipped" is an answer the actor gave.
- Proposed: "Skipped the Campaign" and "Ended the Campaign with nothing
  left to attack". Both actor-subject, past tense, no full stop. Post
  `skipped` as `LogKind.Decision` (it restates the force decision's Skip
  answer, like `sacrificed`) and keep `nothingToAttack` as `Delta` or make it
  `Action`; see question 1.

### 2. `SetupProcedure.placedPawn` — missing determiner

- Where: `gameplay/setup/SetupProcedure.scala:52-53`, written by
  `placedPawnNote` (147-149).
- Produces: "p2 placed pawn at Ancient City" (`woken.actor.log:2`).
- Verdict: **reads wrong**. Its neighbours write "p2 kept Wizard's
  Conclave" and "p1 revealed X"; every other line in the module writes a
  determiner.
- Proposed: "{Blue} placed their pawn at {site}". "Their" rather than "the":
  the pawn is the player's and the line already names them.

### 3. `TradeProcedure.tradedForNoSecrets` / `tradedForNoFavor` — "for no secrets" names a commodity

- Where: `gameplay/actions/economy/TradeProcedure.scala:28-30` and `38-40`,
  written by `tradedNote` (85-100).
- Produces: "Traded with Hiding Place for no secrets"
  (`trade.other.log:24.0`).
- Verdict: **reads wrong**, as the ledger says: the `bought` slot of the
  sibling keys holds what was gained, so the zero case reads as if "no
  secrets" were the thing bought. The ledger's own fix of the favor branch
  (`tradedForNoFavor`) kept the shape.
- Proposed: "Traded with {card} and gained no secrets" and "Traded with
  {card} and gained no favor". Same opening as the gain lines, so the four
  keys still read as one family; the verb "gained" is the one the generic
  delta rule uses ("gained 2 favor from the Arcane bank",
  `DetailLines.scala:99-106`).

### 4. `TravelProcedure.travelled` — spelling against the rulebook

Not in the ledger; found while reading the neighbours.

- Where: `gameplay/actions/travel/TravelProcedure.scala:51-53`.
- Produces: "Travelled to Buried Giant" (`oathkeeper.actor.log:10.0`).
- Verdict: **inconsistent**. The rules text and card text spell it
  "Traveling" (`rules.yml`, "Travel Powers on Sites": "Traveling from here
  to a Coast"), and every burn line in the log is American ("burned 1
  favor", `NoteLines.scala:167`; `BurnVows.scala:72`; `Revelation.scala:39`).
  One British spelling in an otherwise American log.
- Proposed: "Traveled to {site}". The key name `travelled` may stay (it is
  the wire spelling, journaled in `LogNoted`); only the text changes.

### 5. `CampaignNotes.sentTo` — passive where every other line is active

- Where: `CampaignNotes.scala:112-114`, written by `relocationNote`
  (287-296).
- Produces: "p2 was sent to Broken Peaks" (`raid.other.log:36.0`).
- Verdict: **inconsistent**. The relocation is the attacker's decision
  (`CampaignRaid.scala:23-27`, `Decide(CampaignIds.relocation, actor, ...)`),
  and the two lines beside it are actor-subject ("Took Wine of Welcome and
  People's Favor from p2", `raid-spoils.actor.log:34.0`). This is the only
  passive sentence a procedure writes.
- Proposed: "Sent {Blue}'s pawn to {site}". Actor unnamed, the defender
  named as the pawn's owner, matching "Targets: p2's pawn" (`NoteThing.Pawn`,
  `NoteLines.scala:158-159`).

### 6. Plan activation lines — "activated" where the rules say "use"

- Where: `CampaignNotes.scala:44-60` (`activatedPlan`,
  `playerActivatedPlan`, `banditsActivatedPlan`,
  `activatedOathkeeperTitle`, `activatedUsurperTitle`), written by
  `activationNote` (150-161) and `banditActivationNote` (165-168).
- Produces: "Activated Careful Plans", "p2 activated Careful Plans", "The
  bandits activated Careful Plans", "p2 activated the Oathkeeper title".
- Verdict: **inconsistent with the rulebook and the log's own power
  vocabulary**. The rules say a player *uses* battle plans (`rules.yml`,
  Campaign step "Use battle plans"; "Battle plans are used during
  Campaigns"), and the log's generic power line is "Used {card}"
  (`NoteLines.usePowerLines`, `NoteLines.scala:183-186`). "Activated" is the
  word the legacy rule chose; nothing in the rules activates a plan. The
  ledger's own entry (the title plan lost its "add 1 defense die" label) is
  fine as it stands: every plan line names only its card, and the Defense
  total tells the dice.
- Proposed: "Used Careful Plans", "{Blue} used Careful Plans", "The bandits
  used Careful Plans", "{Blue} used the Oathkeeper title" and "... the
  Usurper title". The plan side need not be said: the line sits after the
  attacker's or defender's own lines.

### 7. `StartLines.continuedRecover` — a bare verb and a cost

- Where: `application/gamelog/StartLines.scala:70-76`.
- Produces: "Continued" with a `LogSpan.Cost` ("Continued · 1 Supply",
  `recover-failed.actor.log:11.0`).
- Verdict: **reads thin**, as the ledger says: the only line that is a
  single word plus a number. Its band opens "Recover · 1 Supply" then
  "Rolled blank, blank", so "Continued" is understandable in context but
  says nothing when read alone (in the overlay, or quoted).
- Proposed: "Continued recovering" with the same cost span. It pairs with
  the band's closing lines "Recovered {relic} at {site}" / "Failed to
  recover at {site}" (`RecoverProcedure.scala:86-93`).

### 8. The slot fallback, in `ChoiceWords` only

- Where: `LogWords.slotted` (`LogWords.scala:60-76`), called from
  `ChoiceWords.scala:34`; the Negotiation card lines use `NoteArg.Card` and
  fall back to "a Denizen".
- Produces: "p2's facedown adviser (slot 1)" for a parked adviser choice
  (`GameLogRareLinesSuite.scala:150-152`); "p2 showed p1 Birdsong" / "a
  Denizen" for the deal.
- Verdict: **inconsistent, by the ledger's own account**: one fallback for a
  choice, another for a line. A slot tells *which* facedown adviser moved; a
  count does not, and a slot leaks a position the parties may not have
  shown. Decide once.
- Proposed: keep the count fallback everywhere ("a Denizen") and drop the
  slot from `ChoiceWords`, unless the owner wants the slot: a parked choice
  names the slot because the chooser is looking at the row, but the log is
  read by everyone. See question 2.

### 9. `CampaignNotes.lost` against `playerLost` — the subject convention

- Where: `CampaignNotes.scala:96-103`, written by `lossNote` (218-230).
- Produces: "Lost 2 warbands" for the actor (`captains-raid-lost.other.log:
  34.0`), "p2 lost 1 warband" for anyone else (`raid.other.log:29.0`).
- Verdict: **fine**. This is exactly the convention every other line
  follows (`LogWords.subject`); the ledger's question "should the subject
  always be named" is a question about the whole log, not this line. Strike
  the entry, or reopen it as a log-wide question.

### 10. `SearchProcedure.keptCards` — "kept none"

- Where: `gameplay/actions/search/SearchProcedure.scala:62-63`, written by
  `keptNote` (98-107).
- Produces: nothing when nothing is kept; "Kept Threatening Roar" otherwise
  (`search.actor.log:14.0`).
- Verdict: **fine, and closed by the rules**. The keep decision's Keep
  section has a minimum of one (`SearchProcedure.scala:213-215`,
  `DecisionSection(keepKey, "Keep", 1, Some(1))`), so a player cannot keep
  nothing; the one power that empties the hand (Spinning Bee) writes its own
  line. A silent "kept none" has no reader. Strike the entry.

### 11. Card Play's adviser placement after a Search's "Kept"

- Where: `CardPlayProcedure.scala:91-98` (`playedFaceupAdviser`,
  `playedFacedownAdviser`) after `SearchProcedure.keptCards`.
- Produces: "Kept Threatening Roar" then "Played Threatening Roar as a
  facedown adviser".
- Verdict: **fine**. The two lines are two answers in two commands (the keep,
  then the placement), and a live viewer sees the first before the second
  exists. Dropping either would hide a step. If anything, the placement line
  could drop the card name after a "Kept" of the same card, but that is a
  cross-procedure rule the note boundary forbids. Strike the entry.

### 12. Totals after the plan that rewrote them

- Where: `CampaignNotes.attack*` and `defense` (66-84) after a plan's line.
- Produces: "Outriders: Skulls ignored." then "Attack: 8".
- Verdict: **fine**: reason, then the number it produced, is the order a
  reader follows. Strike the entry, recording the choice.

### 13. A Raid defender's losses as three sentences

- Where: `lost`/`playerLost`/`banditsLost` (losses window),
  `defenderLost` (consequences window), `sentTo` (relocation window).
- Produces: "p2 lost 1 warband" / "p2 burned 1 favor and discarded a
  Denizen" / "p2 was sent to Broken Peaks" (`raid-spoils.actor.log:32-41`).
- Verdict: **fine**. They are three rule steps (Kill defeated warbands;
  Raid victory steps 2-3; step 4) and other powers' lines can land between
  them (Keep replaces the last). Only `sentTo`'s voice is flagged (entry 5).

### 14. Loss lines before the victory's gains

- Produces: "The bandits lost 3 warbands" then "Placed 2 warbands on Ancient
  City" (`conquest-placed.actor.log:27-32`).
- Verdict: **fine**: that is the rulebook's order (Campaign step "Kill
  defeated warbands" precedes "Resolve conquest victory" and "Resolve raid
  victory"). Strike the entry.

## Lines read beside the ledger that are consistent

For the record, so the next reader need not re-check them: "p1 wins!" /
"The bandits win!" (the log's two exclamations, `CampaignNotes.scala:85-89`,
a deliberate pair); "Attack Pool: 3, Defense Pool: 2" (capitalised as a
label, like "Attack: 3"); "Nothing happened in Wake"; "Negotiated with p2" /
"Negotiation ended by p2"; "Increased supply from 2 to 5"; "Took 1 favor
from the Order bank".

## Questions for the owner

1. Entry 1: should "Skipped the Campaign" post as a `Decision` line (it
   restates the Skip answer) and "Ended the Campaign with nothing left to
   attack" as an `Action` line? Recommended default: yes to both; today both
   are `Delta`, which is the kind for a resource or card change.
2. Entry 8: slot or count for a facedown card the viewer cannot identify,
   everywhere? Recommended default: count ("a Denizen"), and drop the slot
   from `ChoiceWords`; the slot is a position the deal's parties may not
   have shown.
3. Entry 6: "used" for every plan line, including the bandits' and the
   title's? Recommended default: yes; the rules' verb, and the log's own
   "Used {card}".
4. Entry 9: should the log name the actor on every line (a log-wide change)
   rather than omit them? Recommended default: no; the band header names the
   actor and the convention is consistent.

## Recommended next slices

Each is one session: change the wording in its `NoteKey`, update the golden
logs that show it (`src/test/resources/gamelog/*.log`) and the suites that
assert the text, and strike the ledger entry naming the commit. Wording
changes stay out of any migration commit (procedures-journal-their-notes
design, ruling 6).

1. **Campaign endings and voice** (entries 1 and 5): `skipped`,
   `nothingToAttack`, `sentTo`, their kinds in `ProcedureLog`; goldens
   `queued-raid-skip`, `raid`, `raid-spoils`.
2. **Setup and Travel** (entries 2 and 4): `placedPawn`, `travelled`;
   goldens `woken`, `oathkeeper`, `round` and every script that travels.
3. **Trade's zero gains** (entry 3): `tradedForNoFavor`,
   `tradedForNoSecrets`; golden `trade`.
4. **Plan lines say "used"** (entry 6): the five activation keys;
   `CampaignLogScripts` and every Campaign golden with a plan.
5. **Recover's continue line** (entry 7): `StartLines.continuedRecover`;
   goldens `recover-failed`, `recover-succeeded`.
6. **Close the ledger entries that are fine** (entries 9 to 14): move them
   under "Fixed" or a new "Closed" heading with the reason recorded here, so
   the ledger lists only open work. Entry 8 waits on question 2.
