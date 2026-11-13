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
