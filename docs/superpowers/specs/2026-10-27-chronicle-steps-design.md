# Chronicle steps design

Status: approved in brainstorming, 2026-10-27. Next step: implementation plan.

Roadmap item: **Phase - Chronicle steps**, "End-of-game Chronicle steps". The
Persistence work (saved-campaign continuation, campaign browsing, the TTS
string codec) follows as its own design once this phase lands.

## Goal

When a game has a result, the winner can begin the Chronicle Phase in the same
game journal. The phase resolves the five Chronicle tasks (Sun, Throne, World,
Beacon, Stars) as ordinary walker decisions and ends by journaling
`ChronicleUpdated(chronicle)`, the evolved between-game record.

## Decisions

- **Same journal.** The Chronicle Phase runs inside the finished game. There is
  no separate session aggregate.
- **Explicit start.** A startable `BeginChronicle` is legal only for the winner
  (`GameResult.winner`), only while `result` is set, and only once. Other seats
  see the victory banner and "Waiting on {winner}: Start Chronicle" through the
  existing waiting view.
- **Always ends with `ChronicleUpdated`.** There is no "play again or store"
  choice. The next game starting from the evolved Chronicle is the Persistence
  phase's concern.
- **Procedure shape.** One triggered procedure per task (`ChronicleSun`,
  `ChronicleThrone`, `ChronicleWorld`, `ChronicleBeacon`, `ChronicleStars`),
  plus `ChronicleBegin` and a closing `ChronicleFinish`. Not one large tree; not
  a player-ordered menu.
- **Chaining (amended 2026-10-27).** The driver chains them, not the queue:
  `QueuedProcedure.procedure` must be a `StartableRef` and its wire codec
  rejects triggered refs. A `ChronicleSequence` table names the successor of
  each completed Chronicle procedure, and `OathDriver.next` starts it while the
  phase is `Chronicle`. `OathDriver.next` also starts `ChronicleBegin` once when
  `result` is set and the phase is not yet `Chronicle` (today it returns early
  on a result). `ChronicleBegin` is a parked decision owned by the winner; no
  client command starts a procedure.
- **Live state extended.** Game start keeps the whole original `Chronicle` as an
  immutable baseline on the live state, including lineages of colors that did
  not play (their cards, legacies and relics). The phase evolves that baseline.
  Task steps are journaled operations; an adapter keeps live card moves and the
  working Chronicle in step.
- **Chronicle fields.** `Chronicle` gains required `era: EraState` and
  `oathkeeperGoal`. Old journals need not decode. `edificeDeck` and
  `suitedReserves` are derived views ("every card not accounted for elsewhere"),
  not stored. Visions and the Grand Scepter holder are not stored (the Scepter
  goes to the Chancellor). Ruined versus intact edifice faces are settled in
  setup and are not part of the Chronicle.
- **Stubs.** Foundations and Legacies are later slices. Steps that depend on
  them (Sun steps 2-4 and the goal checks, Throne's legacy bonus points,
  Beacon's restore-a-foundation) are empty power windows that carry
  `foundations` and `lineages` through unchanged. Each cites its rule.
- **Windows, not hardcoded actors.** Each task step runs inside a power window,
  like Setup's `SetupEnd`. The base rule gives the default actor and step;
  legacy powers can later change the actor, add a step or skip one. A test with
  a fake power proves the seam before Legacies exist.
- **Actors.** Steps that make a choice have an actor; the rest run
  procedurally with none. Winner: the Sun and Throne choices. World task holder
  (People's Favor): World. Beacon task holder (Darkest Secret): Beacon. Stars
  has no actor (the constellation step is not implemented). Task holders are
  seated players. Sun's step 7 lets the winner choose the World holder when no
  one holds the People's Favor, and the Beacon holder when no one holds the
  Darkest Secret. The holder of both Beacon and World chooses the recipient and
  which task to give (user ruling).
- **Scoring.** Throne awards 3 points plus 2-for-1 gifts into
  `era.lineageScores`. Legacy bonus points stay stubbed. Era-end detection and
  reset belong to the Eras slice.
- **Empire state.** Sun crowns the winner and sets Empire state (Imperial
  warbands, `world` sites, Reliquary). Citizenship offers are a seam for the
  Empire phase. A game started from an Empire-state Chronicle is not playable
  until that phase; that is accepted.
- **Randomness and secrecy.** Shuffles and draws use the recorded-order
  mechanism Setup uses (`ChronicleRandomPort`), so replay never re-derives them.
  The retained starting adviser and the drawn newcomers are owner-private
  decisions, as Setup's adviser decision is.
- **UI.** The generic walker-decision panel and Game Log, plus the winner-only
  Begin button. No Chronicle-specific views. The frontend's `viewerPresentation`
  currently hides all controls once a winner exists; it must let a parked
  decision through.

## Rules source

`docs/rules/chronicle.md` is rewritten first from the Combined Rulebook pp.
36-41 and the task cards (`reference/2026-03-17-new-foundations-rev1/chronicle-tasks.pdf`,
image-only, read visually). Known gaps in the current summary:

- World and Beacon name no actor (World task holder, Beacon task holder).
- Shape Empire keeps your *last warband on the map*, not a ruled site, with a
  fallback when you rule no sites.
- Build Edifice has no one-edifice limit and no "intact" filter; ruined
  edifices go to the bottom of the deck.
- Beacon: when a chosen suit has too few cards, all Dispossessed except the six
  just added by World return to their suit sections before drawing.
- The three-active-legacy cap is per player.
- Crown omits the Chancellor board and Grand Scepter and the "do not add
  bandits" note.
- Several steps omit "facedown".

Rulebook ambiguities are put to the user and recorded in
`docs/rules/ambiguities.md` with citations. Rulings so far:

- Task recipients are seated players only.
- Sun step 7 is two rules: if no one holds the People's Favor, the winner
  chooses who takes the World task; if no one holds the Darkest Secret, the
  winner chooses who takes the Beacon task.
- Most cleanup steps have no actor and run procedurally. Stars has no
  actor-bound step except "Writing in the Stars" (constellation and narrative),
  which is not implemented for the digital version. Everything else in Stars is
  actorless. Setup steps may carry an actor; the rewrite checks each.
- Stars' "play again or store" choice does not apply; the phase always ends in
  `ChronicleUpdated`.

## Exit and extraction

After Stars, the last node journals `ChronicleUpdated(chronicle)` with the
evolved baseline. The result must pass game start's known-id, uniqueness and
card-accounting checks, including "every card exactly once" behind the derived
views. `ChronicleUpdated` gets a codec in `LifecycleEventCodec`. Feeding the
result to `GameStartRules` is tested where valid; Empire-state results are
checked against validation only.

## Testing

- Rules doc rewrite first, with ambiguities sent to the user before it lands.
- Per task: Situation-style tests starting at the task's boundary, asserting
  Chronicle changes and Game Log lines.
- Replay equals live; redaction hides newcomers, the retained adviser and
  unread World cards from other seats.
- One end-to-end test from a game result through all five tasks to
  `ChronicleUpdated`.
- Fake-power tests for each empty window.

## Order of work

0. Branch from `0.2.0-alpha`. Rules doc commit.
1. Slice 1: baseline kept at game start, `Phase.Chronicle`, Begin button,
   `ChronicleUpdated` skeleton, Sun and Throne. If it runs long, move the state
   extension into its own slice 0.
2. Slice 2: World and Beacon.
3. Slice 3: Stars, extraction, round trip.

## Risks

- Keeping the baseline in live state changes the `GameStarted` and state codecs
  and many fixtures. Do it as slice 1's first task.
- Sun's role changes touch `Role`, which the Empire phase builds on. Keep to
  what the rules need.

## Out of scope

Foundations and Legacies behaviour, Eras end and reset, Citizenship through
Negotiation, Imperial setup and play, the TTS codec. Its maintainer (harsch) will bump the encoding to version 3
and add one digit after the foundations bitmask for the current Oath (0 unset,
1 Supremacy/Conquest/Most Sites, 2 Devotion/Faith/Darkest Secret, 3
People/Rebellion/People's Favor, 4 Protection/Sanctuary/Relics, 5-20 reserved,
21+ fan content), so `oathkeeperGoal` has a wire home. Era still has none, saved-campaign browsing, and starting a playable
game 2.
