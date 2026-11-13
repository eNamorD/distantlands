# Legacies implementation rulings

Rulings the owner's orchestrator made while the
[Legacies design](2026-11-13-legacies-design.md) was implemented, on
questions the design and its slice plans did not settle. Each entry gives
the question, the decision and the reason. The design's own rulings 1-10
are not repeated here.

## Slice 1

**R1. Legacies of a lineage the baseline does not list.**
Question: `ChronicleRecord.campaignFields` writes a seated lineage that the
baseline Chronicle does not list (one seated for the first time) with no
legacies, so a legacy it gained in the game is lost from the saved
Chronicle. Slice 1 cannot reach this; slice 2's deal does.
Decision: slice 2 makes `campaignFields` write that lineage's live
legacies, with a test.
Reason: a legacy is a persistent lineage card (CR section 12); the fix
belongs with the first deal that can give such a lineage a legacy.

**R2. Starting relics of a lineage in the box.**
Question: game start no longer refuses starting relics. Those of a lineage
nobody seats stay on its record, outside play and outside the card index.
Should game start refuse them instead?
Decision: no. A box lineage's starting relics stay on its record unchanged
and are carried into the next Chronicle.
Reason: Legacies ruling 8, lineages in the box neither compete nor
change; a box lineage's starting advisers already work this way.

**R3. A move into the legacy deck names no position.**
Question: card moves into a deck must name its top or bottom. The legacy
deck keeps no order.
Decision: the legacy deck is not a stack. A move into it names no
position, and a move that names a top or bottom is refused.
Reason: the deck is derived and no rule reads its order (Legacies ruling
3).

## Slice 1 review

**R4. The evolved Chronicle shows dormant legacies to every seat.**
Question: `GameProjection.chronicleView` projects the evolved Chronicle,
lineages and all, to every seat once the Chronicle Phase journals it, so
another player's dormant legacies show there. Should it redact them?
Decision: no. The evolved Chronicle shows every legacy to every seat.
Reason: it is the end-of-game save every player carries into the next
game, as the visible relic deck order already is; the next game's
projection shows only the baseline's name and digest. Recorded in the
`chronicleView` doc comment and in the "Legacies rulings" of
`docs/rules/ambiguities.md`.

**R5. A legacy moved between lineages with no face given.**
Question: a legacy moved from one lineage to another with no resulting
orientation kept its face. The design says a legacy entering a lineage
takes the move's face, dormant when none is given.
Decision: follow the design. A legacy entering a lineage takes the move's
resulting orientation, dormant when none is given, whatever face it had
(`CardMovementOperations.insertCard`).
Reason: the design's slice 1 text; one rule for every source.

**R6. A face on a move into the public row or the legacy deck.**
Question: a move into the public row or the legacy deck could name a
resulting orientation that neither can hold.
Decision: such a move may name no face or faceup; facedown is refused
with `UnsupportedOrientation`, as an edifice's face is.
Reason: the public row is faceup, and the derived deck keeps no face.

**R7. `LegacyGoal.key` and `LegacyGoal.all`, and the legacy deck's
removal arm.**
Question: no production code reads `LegacyGoal.key` or `LegacyGoal.all`
yet, and `removeCard`'s `CardDeck.Legacy` arm cannot be reached.
Decision: keep both members for slice 5, and delete them then if slice 5
still does not use them in production. The unreachable arm stays, with a
comment saying nothing is indexed in the derived deck.
Reason: slice 5 draws the goal icon; the match must stay exhaustive.

**R8. Slice 1 docs.**
Question: the "Legacies rulings" section the design requires was not
written, `docs/rules/implementation-traceability.md` still said legacies
were excluded from game start, and the `LineageState.startingRelics` doc
said nothing creates them until Legacies.
Decision: add the section with design rulings 1 and 3 and R4 (slice 2
completes it, R17), correct the traceability row and the doc comment.
Reason: the docs must say what the code does.

## Slice 2

**R9. A short Quiet Ambitions deck leaves a player one legacy.**
Question: under Quiet Ambitions a short deck can leave a player one
legacy, or none.
Decision: a player whose draw is one legacy keeps it without being asked;
a player who drew none is asked nothing.
Reason: design ruling 5, and a forced choice is no choice.

**R10. The keep decision's options.**
Question: how the Quiet Ambitions keep decision offers the two drawn
legacies.
Decision: a new decision option kind, `DecisionOptionRef.Legacy`, wire
kind `"legacy"`, so the choose-one names the legacies as cards.
Reason: the alternative, buttons beside subject cards, needs a new
subject-card source and reads worse.

**R11. A legacy's card details.**
Question: the options and the Public Ambitions line need legacy names,
and the presentation layer named a legacy by its id.
Decision: slice 2 adds the minimal legacy card details (kind `"legacy"`,
name, suit, rules text); slice 5 adds the goal and the UI.
Reason: the deal's decision and log line need names now.

**R12. Every game deals legacies, test games included.**
Question: should game start refuse a legacy order that is not the
derived deck, so every fixture must supply one?
Decision: yes, strictly. Every fixture supplies the order, and
`Table.start` returns the dealt legacies so the quiet table holds none.
The affected golden logs are rewritten and reviewed; every changed golden
line must be one the deal explains (a new keep decision, a new log line,
a shifted event number), and nothing else may change in them.
Reason: an empty order meaning "no deal" would let a real game start
without legacies.

**R13. Which legacies the keep decision offers.**
Question: how Setup knows which legacies a player drew.
Decision: derive them from state: the dormant legacies the lineage holds
that its saved record does not. Nothing new is stored.
Reason: the draw is already visible in state; a stored copy could drift.

**R14. Where the Public Ambitions line posts.**
Decision: first in the Setup walk, before the Reliquary pick, as a Setup
note.
Reason: game start deals the row before anyone acts.

**R15. `legacyOrder` on the wire.**
Decision: a required field of `SetupOrders`; a stream recorded before
slice 2 no longer decodes.
Reason: the ROADMAP standing rule lets event formats change in place
before the first public release, and with R12 an empty order would fail
at replay anyway.

**R16. Where the deal is applied.**
Decision: `GameStartRules.evolve` applies the deal; the plan factory and
game start share the deal arithmetic (`LegacyDeal`). The design's wording,
which named the materializer too, is updated to match.
Reason: game start already builds the lineages and patches the common
cards; the materializer builds neither the lineages nor the public row.

**R17. The "Legacies rulings" section.**
Decision: the slice 1 follow-up (R8) adds it with design rulings 1 and 3
and R4; slice 2 completes it with all ten design rulings.
Reason: setup now acts on rulings 3-5.
