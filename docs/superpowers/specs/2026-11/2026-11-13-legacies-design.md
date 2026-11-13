# Legacies

**Status:** designed 2026-11-13, approved section by section by the owner.
Slices 1 and 2 landed (plans: [slice 1](../../plans/2026-11/2026-11-13-legacies-slice1.md),
[slice 2](../../plans/2026-11/2026-11-13-legacies-slice2.md)).

**Builds on** the [Chronicle steps design](../2026-10/2026-10-27-chronicle-steps-design.md),
the [Foundations design](2026-11-11-foundations-design.md), whose faces and
alteration sources this fills in, and the
[operation families design](2026-11-12-operation-families-design.md), which
must land first: this spec adds an operation to its `ChronicleOperation`
family and legacy write paths to its card family.

## Goal and scope

A legacy is a persistent lineage card with a goal, a suit and a power,
faceup (active) or facedown (dormant) (CR section 12, pp. 33 and 36).
Today the engine carries legacies only as Chronicle data it cannot use:
game start refuses a seated lineage's legacies and starting relics, the
Sun's steps 2-5 are empty windows, Score Lineage Points ignores the legacy
bonus, and Foundation III's two faces deal nothing.

This spec covers the **legacy lifecycle**: card data, dealing, Resolve
Legacies, the scoring bonus, persistence and display. It implements no
legacy power. All 36 powers stay in `catalog/holding/HoldingLegacies.scala`
and come in three later batches, by where they hook in: setup, the
Chronicle, and in play. Until its batch lands, a legacy in play has its
goal and suit but no power.

| Slice | Seam | Delivers |
|---|---|---|
| 1 | Data and state | Goal, suit and foundation on all 36 cards; the derived deck; the public row; legacy card moves and flips; `FoundationAltered`; game start accepts legacies and starting relics; redaction |
| 2 | Setup deal | `LegacyDeal`; Quiet Ambitions' keep decision, which a first game also uses; the Public Ambitions row |
| 3 | Resolve Legacies | `LegacyGoals` and its tie rules; Sun steps 2-5; the Chancellor's public tie-break; the cap; foundation alteration |
| 4 | Scoring | The legacy bonus in Score Lineage Points and its log line |
| 5 | UI | The legacy strip, the public row, inspection, the Chronicle page's legacies |

Slice 5 reads only state and the projection, so it may run beside slices 3
and 4 once 1 and 2 land.

## Rulings

Taken with the owner on 2026-11-12 and 2026-11-13. They go into a new
"Legacies rulings" section of `docs/rules/ambiguities.md`.

1. **Only the owner sees a dormant legacy's face.** Every other viewer sees
   a legacy back. The projection redacts it as it redacts a facedown
   adviser; nothing tracks who has seen which card.
2. **Newly dormant is dormant.** Sun step 2 leaves no dormant legacy on a
   seated lineage: each becomes active or is discarded. Step 3 is the only
   step that makes legacies dormant, so after it every dormant legacy on a
   seated lineage is newly dormant. Crown Chancellor and Offer Citizenship,
   which flip newly dormant legacies to active, act only on seated players
   and only after Resolve Legacies. No flag records it.
3. **The legacy deck is derived.** It is every catalog legacy that no
   lineage holds, seated or in the box, and that is not in the public row.
   No rule reads its order after setup, and every legacy draw happens in
   setup, so the game stores no deck. "Return to the box" (Quiet
   Ambitions) and "return to the legacy deck" (the Sun) are the same pile.
   Sun step 5, Shuffle, has nothing to do.
4. **A first game is Quiet Ambitions.** Foundation III's normal face, draw
   2 and keep 1 facedown, is the first game's "2 random legacies, keep one
   dormant" (CR p. 7). One deal step serves both.
5. **A short deck draws what remains,** in seat order, as a short suit does
   in Introduce Newcomers (Chronicle ruling 15).
6. **A score of 0 never meets a goal.** For the counted goals (most sites,
   most cards of a suit, highest relic) only the highest positive score
   meets it, as `OathkeeperRules.leadersWithPositiveCount` reads the
   Oathkeeper goals.
7. **No Grand Scepter in play** (an exile-only game) means the winner meets
   the Grand Scepter goal, as the Sun card prints "If none, win."
8. **Lineages in the box neither compete nor change** in Resolve Legacies.
   Their active legacies still alter foundations (step 2), as Shape
   Foundation already counts them (Foundations rulings, item 15).
9. **Public legacies.** The player who alone meets a public legacy's goal
   takes it active. A tie between Imperials alone is broken by the outgoing
   Chancellor, the one before Crown Chancellor, who chooses one of the tied
   Imperials; any tie that includes an Exile is unfriendly and nobody takes
   it. Public legacies nobody takes return to the deck.
10. **Scoring counts printed suits.** No power changes a card's suit during
    the Chronicle; the bonus counts each denizen, edifices included, by its
    printed suit.

## Approach

Legacies are cards. They move between the lineage's Legacies area, the
public row and the derived deck through the existing card vocabulary:
`Move` between containers, `Flip` between active (faceup) and dormant
(facedown). This gives the Game Log, redaction and later power hooks the
same paths every other card uses. One operation is new:
`FoundationAltered`.

Two alternatives were set aside. Treating legacies as Chronicle facts that
only `ChronicleExtraction` edits leaves nothing to log or show, and no
state for in-play powers such as The Mouth. One batch operation for Sun
steps 2-4 cannot ask step 4's question, and leaves no gap between steps for
Steadfast or Gatekeeper.

## Card data

The goal is the white icon at the top of a legacy's ribbon, and the suit is
its wax seal (CR section 12; the Sun card prints the goal legend). They were
read from the `bgs-cards` card images
(`content/card-images/oath/en-US/<Name> (NF).png`). Each goal covers six
cards in a run of ids, and each run holds one card of every suit.

| Ids | Goal | Printed |
|---|---|---|
| L01-L06 | `MostSites` | Rule the most sites |
| L07-L12 | `GrandScepter` | Hold Grand Scepter. If none, win. |
| L13-L18 | `PeoplesFavor` | Hold People's Favor |
| L19-L24 | `DarkestSecret` | Hold Darkest Secret |
| L25-L30 | `HighestRelic` | Hold relic of highest value (under name) |
| L31-L36 | `MostOfSuit` | Rule most cards of suit (even advisers) |

| Id | Name | Suit | | Id | Name | Suit |
|---|---|---|---|---|---|---|
| L01 | Iron Hand | Order | | L19 | The Ear | Hearth |
| L02 | Chronicler | Arcane | | L20 | Heirloom | Beast |
| L03 | Steadfast | Discord | | L21 | High Priest | Order |
| L04 | Peacemaker | Hearth | | L22 | Seer | Arcane |
| L05 | The Needle | Beast | | L23 | Arbiter | Discord |
| L06 | Circle of Swords | Nomad | | L24 | The Bright Beacon | Nomad |
| L07 | Great Architect | Hearth | | L25 | Oaths of Loyalty | Hearth |
| L08 | Ancestral Lands | Nomad | | L26 | Conspirator | Discord |
| L09 | Scepter Bearer | Discord | | L27 | Light Fingers | Beast |
| L10 | Royal Line | Beast | | L28 | Secret Society | Arcane |
| L11 | Keeper of Order | Order | | L29 | Ruthless | Order |
| L12 | Golden Key | Arcane | | L30 | Quest to Distant Lands | Nomad |
| L13 | World Crafter | Nomad | | L31 | Wealthy | Hearth |
| L14 | Matchmaker | Hearth | | L32 | Gatekeeper | Discord |
| L15 | Revolutionary | Discord | | L33 | Beloved | Beast |
| L16 | Populist | Beast | | L34 | The Mouth | Arcane |
| L17 | Rival | Order | | L35 | Reformer | Order |
| L18 | The Standard Bearer | Arcane | | L36 | Pathfinder | Nomad |

The six `MostOfSuit` cards print their suit on the ribbon as well, and it
is the seal's suit. They also print "Set Foundation X to ...", always an
altered face:

| Id | Name | Alters |
|---|---|---|
| L31 | Wealthy | II, Movers and Shakers |
| L32 | Gatekeeper | III, Public Ambitions |
| L33 | Beloved | IV, Teeming World |
| L34 | The Mouth | VI, Festival |
| L35 | Reformer | V, Grand Council |
| L36 | Pathfinder | I, Wide Horizons |

## Slice 1: data and state

- **Catalog.** `catalog.Legacy` gains `goal: LegacyGoal`, `suit: Suit` and
  `alters: Option[FoundationNumber]`. `LegacyGoal` is a sealed type in
  `catalog` with the six cases above; `MostOfSuit` reads the card's own
  suit. Every holding legacy object gets its values from the tables above.
  `reference/catalog-ingestion/legacy-transcriptions.json` gains `goal` and
  `suit`, citing the `bgs-cards` images. `CardCatalogSuite` pins the
  shape: six cards per goal, six per suit, one per goal and suit, and
  `alters` exactly on L31-L36.
- **The deck.** `CardZones.legacyDeck` is deleted (ruling 3).
  `CardDeck.Legacy` stays as a destination: a legacy moved there leaves
  play, and `CardIndex` lists nothing in it. Card moves that read the
  legacy deck (`CardMovementOperations`) lose those arms.
- **The public row.** `CardZones.publicLegacies: Vector[LegacyId]`, with
  `CardContainer.PublicLegacies`, indexed by `CardIndex`. The Chronicle
  never stores it: after the Sun it is empty.
- **Moves and flips.** `Move` gains write paths into and out of
  `CardContainer.Lineage(_, LineageCardArea.Legacies)`,
  `CardContainer.PublicLegacies` and `CardContainer.Deck(CardDeck.Legacy)`.
  `Flip` gains the Legacies area: faceup sets `LegacyState.active`,
  facedown clears it. A legacy entering a lineage takes the move's
  `resultingOrientation`, dormant when none is given.
- **`FoundationAltered(number, source: LegacyId)`**, a new
  `ChronicleOperation` case: the face becomes altered and `source` joins
  its alteration sources. Applying it when the face already lists `source`
  is a doubled operation and refused, as `SetOathkeeperGoal` refuses an
  unchanged goal. It is the inverse of Shape Foundation's
  `FoundationRestored`. It gets its applier arm, codec arms and a case in
  the Chronicle wire suite.
- **Game start.** `GameStartRules.buildPlan` drops both refusals,
  "legacies require Legacies" and "starting relics require Legacies". A
  seated lineage's legacies enter the game with their stored faces. Its
  starting relics go to its player facedown, as the Chancellor's
  Reliquary relic does. `ChronicleAccounting` refuses a legacy that two
  lineages hold.
- **Redaction.** The projection shows an active legacy to everyone and a
  dormant legacy's face to its owner only (ruling 1); other viewers get a
  legacy back. The public row is faceup.

Tests: the catalog shape; `Move` and `Flip` on each legacy container;
`FoundationAltered` applied, refused when doubled, and round-tripped on the
wire; a later game starting with stored legacies and starting relics; a
dormant legacy redacted in another seat's snapshot.

## Slice 2: setup deal

`LegacyDeal(face, players)` sits beside `SetupDeal` in `gameplay/setup`.
It is the one deal arithmetic the plan factory and game start share; game
start applies it (implementation ruling R16). The plan factory shuffles
the derived deck with the setup seed, and `GameStarted` records the order
the deal used, as it records the denizen order.

- **Quiet Ambitions** (Foundation III normal, and every first game, ruling
  4). Each seated player draws 2 legacies in seat order, including players
  who already hold legacies. Both go dormant into the lineage's Legacies
  area, so only their owner sees them. In `SetupProcedure`, for each
  participant after pawn placement and before the adviser choice, a
  `Branch(Decide(setup.legacy-choice.<player>, participant))` asks a
  choose-one between the two drawn legacies, both shown as subject cards.
  `BuildOps` then moves the other to the deck. This keeps the first game's
  order on CR p. 7: place pawn, keep legacy, keep adviser.
- **Public Ambitions** (Foundation III altered). Game start deals the
  shuffled deck's first 4 legacies faceup into `publicLegacies`. Nobody
  chooses.
- **A short deck** draws what remains, in seat order (ruling 5). It cannot
  happen in a first game.
- **Log.** "Blue kept a legacy" names no card: a dormant face fails the
  log's knowledge rule for other viewers. "Public Ambitions dealt Rival,
  Seer, Arbiter and Wealthy."

Tests: `LegacyDeal` draw order and a short deck; a Quiet Ambitions setup
leaving each player one more dormant legacy than they started with; a
Public Ambitions setup with 4 faceup public legacies; a first game with one
dormant legacy each; the keep decision's subject cards visible only to its
owner; the setup log line naming no card.

## Slice 3: Resolve Legacies

**`LegacyGoals`**, a pure module in `gameplay/chronicle`, answers who meets
a goal: `meeting(state, catalog, goal, suit): Set[PlayerId]`. Only seated
players compete. Bandits never do: count only sites and cards that players
rule.

| Goal | Score |
|---|---|
| `MostSites` | Sites ruled. Every Imperial is credited with each Empire site, through `SiteRule`. |
| `MostOfSuit` | Cards of the suit at sites ruled, plus that player's own advisers. Imperials share the Empire sites' cards. |
| `HighestRelic` | The printed `value` of the best relic held, faceup or facedown. Sun step 1 has already revealed them. |
| `PeoplesFavor`, `DarkestSecret` | The banner's holder meets it; an unheld banner, nobody. |
| `GrandScepter` | Its holder meets it; with no Grand Scepter in play, the winner (ruling 7). |

For the counted goals only the highest positive score meets the goal
(ruling 6). Ties between Imperials alone are friendly: every tied Imperial
meets it. A tie that includes any Exile is unfriendly: nobody meets it,
even when the Exile ties with the Empire. `LegacyGoals` also returns the
tied players, so a public legacy can be offered to the Chancellor's choice
(ruling 9).

Each Sun step runs in its existing window
(`ChronicleSunProcedure`):

2. **Flip legacies** (`PowerWindow.ChronicleFlipLegacies`). Each seated
   lineage's dormant legacy is flipped active when its player meets its
   goal, otherwise moved to the deck. Each public legacy moves active to
   the one player who meets its goal; on a tie between Imperials alone, a
   `Decide(chronicle.sun.public-legacy.<id>, chancellor)` choose-one over
   the tied players picks who. Public legacies nobody takes move to the
   deck. Then, for each active legacy with `alters`, held by any lineage,
   seated or in the box, whose face does not already list it as a source,
   `FoundationAltered`.
3. **Exiles' legacies** (`PowerWindow.ChronicleExileDormancy`). Each seated Exile's active
   legacy whose goal they do not meet flips dormant. Imperials and the
   foundations are untouched.
4. **The cap** (`PowerWindow.ChronicleLegacyCap`). In turn order, each
   player with `n > 3` active legacies answers a choose-many with exactly
   `n - 3` picks, and those move to the deck.
5. **Shuffle** (`PowerWindow.ChronicleShuffleLegacies`). The window only:
   the deck is derived (ruling 3).

**Log.** Checking goals reveals the dormant faces, so the lines name cards:
"Blue activated Seer", "Blue discarded Arbiter", "Red took Rival" (a public
legacy), "Red's Steadfast went dormant", "Wealthy set Foundation II to
Movers and Shakers".

Tests: a `LegacyGoals` table per goal, with the Empire's sites and their
cards credited to every Imperial, a friendly tie, an unfriendly tie, a tie
with the Empire, a score of 0 and a facedown relic; the Sun steps in order
on a crafted end state; an Exile winner's dormant legacies made active
again by Crown Chancellor; the newly-dormant invariant (ruling 2): after
step 3, every dormant legacy on a seated lineage was active when the
Chronicle began; the cap decision; the Chancellor breaking a public tie;
foundation alteration idempotent across two Chronicles; a lineage in the
box unchanged.

## Slice 4: scoring

Score Lineage Points is the Throne's sixth step. By then Crown Chancellor
has made the winner the Chancellor, and Shape Empire has discarded the
denizens the Empire does not rule, so "sites they rule" are the Empire's
sites.

- **Bonus.** For each of the winner's legacies, 1 point per denizen of
  that legacy's printed suit at those sites (ruling 10). Advisers do not
  count; edifices do. Every legacy the winner holds counts: after the Sun
  none of the winner's can be dormant (ruling 2).
- **Gifts.** `LineageScoring.winnerPoints` becomes `3 + bonus`, and the
  gift bound becomes half that total, rounded down.
- **Log.** "Blue scored 7 lineage points (3 base, 3 from Beast, 1 from
  Nomad)". Legacies of one suit add into one share, a suit that scores 0
  is left out, and with no bonus the line stays "Blue scored 3 lineage
  points".

Tests: the bonus over two legacies; two legacies of one suit in one share;
advisers not counted; gifts bounded by the new total; the log line with and
without a bonus.

## Slice 5: UI

- **Player area.** A legacy strip shows each legacy as a small card with
  its name, goal icon and suit seal. An active legacy is faceup. A dormant
  legacy shows its face, marked "Dormant", to its owner and a legacy back
  to everyone else. Clicking one opens the card inspection: rules text,
  goal and suit.
- **Public Ambitions row.** The public legacies, faceup near the map, while
  the row holds any.
- **Decisions** reuse existing forms: the keep choice is a choose-one with
  both legacies as subject cards; the cap is a choose-many with exact
  bounds; the Chancellor's tie-break is a choose-one over players with the
  legacy as subject card.
- **Chronicle page.** Each lineage's row lists its legacies by name,
  active or dormant. A saved Chronicle is the owner's own record, so it
  shows every face.

Tests: the strip and the public row render from a projection, the dormant
back for another seat, and the Chronicle page's legacy list.

## Docs

- **ROADMAP.** The **Legacies** item is replaced by three items for the
  setup, Chronicle and in-play power batches, which take with them High
  Priest's `DealRules` term and the powers named below. The lifecycle
  half is deleted when slice 4 lands.
- **`docs/rules/chronicle.md`.** Sun steps 2-5 and Score Lineage Points
  marked implemented, citing this spec.
- **`docs/rules/ambiguities.md`.** The "Legacies rulings" section above.
  Item 1 changes: legacies have card data now, but their powers stay in
  holding. Foundations rulings item 1, "Foundation III waits for
  Legacies", is answered by slice 2.
- **`docs/rules/implementation-traceability.md`.** Rows for Sun steps 2-5,
  the two Foundation III deals and the scoring bonus.
- **`CONTEXT.md`.** **Public legacy**: a legacy in the Public Ambitions
  row, held by nobody. **Derived deck**: a pile the game works out from
  what nobody holds instead of storing it; the legacy deck is one.

## Out of scope

- All 36 legacy powers, in three later batches.
- The powers that change this lifecycle: Iron Hand and Chronicler scoring
  players other than the winner, Steadfast keeping legacies active,
  Gatekeeper taking an enemy's legacy, Royal Line choosing the Successor,
  The Mouth activating a legacy in play, and Matchmaker choosing for other
  players. Each lands with its batch, through the windows this spec keeps.
- Era end and the reset of lineage points (the roadmap's **Eras** item).
- Editing a saved Chronicle's legacies (the roadmap's **Edit Chronicle**).
