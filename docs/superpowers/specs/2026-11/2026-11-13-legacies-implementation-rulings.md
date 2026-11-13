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

## Slice 3

**R18. Naming the legacies the Sun reveals.**
Question: a dormant legacy is named only to its owner (design ruling 1),
so the Sun's lines could not name a dormant legacy that leaves play.
Decision: during the Chronicle Phase every legacy is shown to every
viewer. The scope is strictly the Chronicle Phase; the next game's dormant
legacies are owner-only again (ruling 1).
Reason: the game is over and goals are checked openly (the rules reveal
held relics in the same step), and R4 already shows the faces in the
saved Chronicle.

**R19. More than three active legacies after a reactivation.**
Decision: after any reactivation (Crown Chancellor, Offer Citizenship)
that leaves a player above three active legacies, the same discard choice
as Sun step 4 runs for that player at once.
Reason: "If you ever have more, you must discard active legacies until
you have three" ("Legacies in Detail").

**R20. Offer Citizenship's reactivation.**
Decision: both reactivations go in slice 3: an Exile winner's newly
dormant legacies at Crown Chancellor, and an Exile's who becomes a
Citizen at Offer Citizenship. The design's slice 3 text says so.
Reason: the rules give both, and design ruling 2 names both.

**R21. "In turn order" in the Chronicle.**
Decision: seat order starting from the player who started the game's
last round (`tracks.roundStarter`), the game's turn order.
Reason: the Chronicle has no turn of its own; this is the order the game
ended in.

**R22. A Grand Scepter in play that nobody holds.**
Decision: nobody meets the Grand Scepter goal. Design ruling 7 (the
winner meets it) applies only when no Grand Scepter is in play.
Reason: "If none, win" means no Grand Scepter in play.

**R23. What "cards of the suit" counts.**
Decision: denizens, edifices included (intact or ruined, by the printed
suit of the side showing), at the sites the player's side rules (for an
Imperial, every Imperial site), plus the player's own advisers of the
suit, faceup or facedown. Visions, relics, banners, sites and legacies do
not count. An edifice card in the catalog has one suit for both sides
(`Edifice.suit`), so a ruined edifice counts by it.
Reason: the goal prints "(even advisers)", and the rules credit every
Imperial with the cards at Imperial sites.

**R24. Lines the design does not word.**
Decision: a public legacy nobody takes: "Rival returned to the legacy
deck"; a tie-break: "Red took Rival"; a discard over the cap: "Blue
discarded Seer", as the player's decision; a reactivation: "Blue
reactivated Steadfast"; and a foundation line for each `FoundationAltered`,
even on a face already altered, since it records a new source.

**R25. Order within Sun step 2.**
Decision: dormant legacies first, in R21's order, in one batch; then the
public legacies in row order, each with its own tie question; then the
foundation alterations, Foundation I to VI, for every active legacy of a
seated or boxed lineage (design ruling 8).

**R26. An Imperial tie with no Chancellor seated.**
Decision: the walk refuses it as an invariant error, as other Chronicle
steps refuse missing state.
Reason: setup always seats a Chancellor when Imperials sit (a Citizen
stands in, Empire ruling R6), so it cannot happen in play.

**R27. The frontend's legacy cards.**
Question: the frontend has no legacy handling: `BoardOptions.cardKind`
maps `"legacy"` to `"denizen"`, and `CardFace.backLetter` gives "D".
Decision: deferred to slice 5, whose plan fixes both and checks the Setup
keep decision in the running app (hover and selection keys for
`legacy:<id>`).
Reason: slice 5 owns the legacy UI.
