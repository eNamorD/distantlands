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
Extended 2026-11-14 to the lineages' starting relics and advisers, the
Reliquary and the site relics
([Legacy Chronicle powers R48](2026-11-13-legacy-chronicle-powers-implementation-rulings.md);
low confidence).

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
Reason: slice 5 shows the goal; the match must stay exhaustive. R35
settled it: `key` is used, `all` is deleted.

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

## Slice 4

**R28. Score Lineage Points' lines.** (Re-ruled by the owner on
2026-11-13; it replaces the first ruling, which named the kept amount in
one line ending "less N given".)
Decision: the step narrates in three parts.
1. Before the gift question, once the winner's total is known, the earned
   total with its breakdown: "Blue scored 7 lineage points (3 base, 3 from
   Beast, 1 from Nomad)"; with no bonus, today's wording for the earned
   total, "Blue scored 3 lineage points".
2. One line per gift, as it is given: "Blue gave up 4 points to give Red 2
   points." It replaces the old gift line.
3. After the gifts, only when at least one gift was given: "Blue keeps 3
   points".
The "less N given" ending is gone. The journal and the era scorepad still
record the kept amount at the end; the scored line is narration only.
Reason: the owner's re-ruling: the line reads as the table plays it, the
total first, then each gift, then what is kept.

**R29. A winner who gives every point away.**
Decision: a winner who keeps 0 has nothing recorded, as before, and the
last line reads "Blue keeps 0 points" (R28's third line); the earlier
"scored 0 lineage points (..., less 8 given)" line is gone.
Reason: `LineagePointsScored` refuses 0, and R28's sequence already shows
the total and every gift.

**R30. How the line carries the shares.**
Decision: a new note argument, `NoteArg.SuitShares`.
Reason: a note's template is fixed and the number of shares is not; one
argument is simpler than a key per count.

**R31. The order of the shares.**
Decision: suit order (Discord, Arcane, Order, Hearth, Beast, Nomad); empty
shares are left out.
Reason: it never depends on how legacies were dealt.

**R32. Ruined edifices in the bonus.**
Decision: a ruined edifice counts by its card's printed suit.
Reason: consistent with R23.

**R33. Which of the winner's legacies count.**
Decision: every legacy the winner holds, as the design says.
Reason: no legacy can be dormant at that point (design ruling 2, R19,
R20).
Narrowed by the owner 2026-11-14: only active legacies score, under any
scorer ([Legacy Chronicle powers R1](2026-11-13-legacy-chronicle-powers-implementation-rulings.md)).
The winner holds none dormant here, so nothing changes for them.

## Slice 5

**R34. The goal's icon.**
Decision: goals are shown in words for now: a short tag on the small card
and the printed goal in the inspection. Goal icons are a deferred ROADMAP
item.
Reason: the app has no art for the six goals; words read the same for
every goal and wait on no drawing.

**R35. How the goal travels.**
Decision: the projection carries `LegacyGoal.key` and the goal's printed
words; `LegacyGoal.all` is deleted. This settles R7.
Reason: the wording stays in the catalog beside the card data, and `key`
is used in production as a styling and icon hook.

**R36. A dormant legacy for its owner.**
Decision: its owner sees its face with a "Dormant" tag; every viewer sees
the same during the Chronicle Phase (R18); other viewers see an "L" back
outside that phase.
Reason: the design's slice 5 text.

**R37. Where the strip goes.**
Decision: a "Legacies" row of its own, below the cards in play.
Reason: legacies are not cards in play, and a lineage can hold several
dormant ones.

**R38. Where the public row goes.**
Decision: in the World pane, above the regions, headed "Public
Ambitions", shown only while it holds a legacy.
Reason: "near the map", as Foundation III prints.

**R39. The Chronicle page's legacies.**
Decision: a lineage list under the Chronicle page's summary line, e.g.
"Red 9 · Seer, Iron Hand (dormant)".
Reason: the page has no per-lineage row to extend.

**R40. Running the app.**
Decision: the untracked local `.claude/launch.json` is updated to the
README's current server command and data directory, changing only the
stale names and paths; it is not committed.
Reason: so the browser tools can start the server for the check.

**R41. The strip's legacies are chips.**
Question: whole legacy cards in the player strip were cut off, since the
players pane never scrolls and leaves a board one line under its cards.
Decision: the player strip shows chips (suit seal, name, "Dormant"), the
goal in the chip's hover note and the whole card in the inspection; the
public row keeps whole cards with the goal tag. This narrows R34's goal
tag "on the small card" to the public row.
Reason: the strip must fit the players pane without scrolling.

**R42. The goals' words.**
Decision: each goal's words follow the rules' Sun list, every one a full
sentence with a final period, the italics' text kept and the markup
dropped: "Hold the Grand Scepter. If none, win.", "Rule the most sites.",
"Hold the Darkest Secret.", "Hold the People's Favor.", "Rule the most
cards of this suit (even advisers)." and "Hold the relic of the highest
value (shown under name)." (`LegacyGoal.printed`, the design's Card data
table).
Reason: the rulebook's own wording, one source for every place a goal is
shown.

**R43. A count of 1 in the gift and keeps lines.**
Decision: a count of 1 reads "1 point": "Blue gave up 2 points to give
Red 1 point.", "Blue keeps 1 point".
Reason: plain English; the lines use a plural for every other count.

**R44. Gift recipients' lines.**
Decision: a gift recipient gets no separate "scored" line, since each gift
line says what they receive. Their points are still recorded.
Reason: one line per gift says it once.
