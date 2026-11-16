# Catalog Batch 13: Cards that act on legacies

**Status:** designed 2026-11-14, approved section by section by the owner.
The [Legacy in-play powers](2026-11-14-legacy-in-play-powers-design.md)
batch has landed, slice 4 (The Mouth) included: slice 5 here reuses The
Mouth's activation, and slice 2 changes where The Mouth's Iron Hand discard
sends the card. Each slice is planned when the one before it lands. Slice 1
landed 2026-11-14; with it every printed denizen power is implemented. Slice 2 landed 2026-11-15. Slice 3 landed 2026-11-15.
Amended 2026-11-15 by the owner while slice 2 was planned: the legacy deck
is stored and ordered, discarded legacies go to its bottom in place of a
set-aside zone, and drawn legacies wait in the drawer's hand, Setup's
Quiet Ambitions draw included (rulings 1, 2, 6, 9, 12 and 13; "Slice 2").

**Builds on** the [Legacies design](2026-11-13-legacies-design.md), the
[Legacy Setup powers](2026-11-13-legacy-setup-powers-design.md) batch (the
Quiet Ambitions keep, Matchmaker, The Standard Bearer and Wealthy in
`SetupPawnPlaced`) and the [Foundations design](2026-11-11-foundations-design.md)
(Explore, `WhenExplored`, `EdificeSetupSupport`). The batch 4 rulings that
apply to a whole batch, and the cost and log-line rules of
[batch 8](../2026-10/2026-10-12-catalog-batch-8-design.md), apply unchanged.

## Goal and scope

The last cards in `catalog/holding/`, the one card face out of holding
that does not yet run, and Matchmaker for a legacy draw during a game.
When this batch lands, `catalog/holding/` is empty. It clears the ROADMAP's
**Cards that act on legacies** item.

| Slice | Cards | Seam |
|---|---|---|
| 1. Reliquary Raid | Reliquary Raid (220) | `ReliquaryDraws`, unchanged |
| 2. The legacy deck and the draw | Ancient Writ (R22), Matchmaker during a game | a stored legacy deck; legacies in a hand; a keep decision during a game |
| 3. The keep in Foundation III | none | `SetupProcedure`, `LegacyKeep` |
| 4. Power order | none | an ordering host over `SetupPawnPlaced` |
| 5. The Doors | Amber Doors and Lost Doors (E05) | `EdificeSetupSupport`; The Mouth's activation |
| 6. Chronicle cards | Stone Baths and Ruined Baths (E04), Ruined Temple (E10 ruined) | `ResolveLegacies`; a win step; `LineageScoring` |

Slice 1 stands alone and touches nothing the other slices touch. Slices 2
to 4 build what slice 5 needs. Slice 6 reuses slice 4's host for Ruined
Temple's Setup burn. Slices 3 and 4 change the Setup walk, so they run the
Setup, replay and golden suites in full before landing.

Reliquary Raid was held for "a stocked Reliquary"
([batch 9](2026-11-02-catalog-batch-9-design.md)). The Chronicle now stocks
it, and Bewitch, Ancient Pact and Royal Ambitions already draw from it.

## Rulings

Taken with the owner on 2026-11-14. They open a "Catalog batch 13 rulings"
section of `docs/rules/ambiguities.md`, which the implementation rulings
then continue.

### General

1. **The game stores the legacy deck.** Game start keeps what Foundation
   III's deal leaves of the shuffled deck: every catalog legacy that no
   lineage holds (seated or boxed), less the Public Ambitions row and the
   legacies the players drew. A draw during a game takes the top N into
   the drawer's hand. A short deck draws what remains. An empty deck draws
   nothing, and a line says so. The deck is not shuffled during a game.
   A draw shows the drawer cards nobody saw, so it is a hidden outcome and
   undo never takes it back.
2. **Discarded legacies go to the bottom of the legacy deck.** A legacy
   Quiet Ambitions returns "to the box", and every legacy discarded during
   a game, goes there. The discards are Iron Hand's when its holder is
   Imperial, The Mouth's Iron Hand discard, Ancient Writ's two, Lost Doors'
   one and Amber Doors' all. A discarded legacy is drawn again only once
   every legacy above it is drawn. The Sun's returns go to the bottom too.
   The Chronicle stores nothing new: the next game's deal derives its deck
   from the Chronicle as today. This amends Legacies ruling 3 (a derived
   deck with no order, which a returned legacy leaves) and implementation
   rulings R3 and R6 (a move into the deck names no position).
3. **The keep moves to Foundation III in later games.** The Quiet
   Ambitions keep runs while Foundation III resolves (CR "Later games",
   step 5), for every seated player in seat order, before any pawn is
   placed. Matchmaker's holder answers every other player's keep, as Legacy
   Setup powers ruling 13 and R16 say. A first game's keep stays after
   pawn placement (CR p. 7). Neither Doors face reaches a first game: the
   first-game Homeland edifice is the lowest-numbered implemented edifice of
   its suit (E01 for Discord, E06 for Nomad), and Explore needs Foundation I
   altered.
4. **The placer orders the powers in their `SetupPawnPlaced` window.**
   Whenever two or more powers apply in a player's window, that player is
   asked "Choose a power to resolve", with each power named by its card.
   The chosen power resolves. Then the window is gathered again against the
   live state, so a power that no longer applies drops out and a newly
   applicable one joins. Each power resolves at most once per window. With
   one power left, it resolves without a question. The pick writes no line;
   each power's own line appears in the order chosen. Legacy Setup powers
   R27's order (edifice, The Standard Bearer, Wealthy) applies only when
   nothing is asked. This narrows the ROADMAP's **Simultaneous setup
   effects** item to effects on several players.
5. **The Standard Bearer and Wealthy read their holder at their own
   step.** A legacy a Doors face discarded before it resolved does not
   resolve. A legacy Amber Doors activated in the same window does.
6. **Drawn legacies wait in the drawer's hand.** A hand is private to its
   holder, so no other viewer identifies a drawn legacy, and the keep's
   question shows it to the drawer through its options. The kept legacy
   then joins the owner's lineage, dormant, and the others go to the
   bottom of the deck, where nobody identifies them, so the hand is empty
   again. The legacy deck is not shown. Quiet Ambitions' keep draws the
   same way: game start deals no legacy to a lineage, and each player's
   keep draws 2 from the top into the keeper's hand, so Matchmaker's
   holder sees another player's draw because it is in their own hand. The
   draws happen in keep order, and a short deck can give a later player a
   legacy an earlier one returned. This amends Legacies ruling 5 (a short
   deck draws in seat order), Legacies implementation ruling R13 (the
   drawn legacies are the lineage's unrecorded dormant ones) and Legacy
   Setup powers R16 (the holder sees the draw while the keep is parked).

### Slice 1: Reliquary Raid

7. **Reliquary Raid.** WHEN PLAYED, the player takes a random relic from
   the Reliquary, facedown, as Bewitch, Ancient Pact and Royal Ambitions
   do. An empty Reliquary takes nothing and writes ruling R22's line. A
   Reliquary Setup With No Empire set aside cannot be drawn from
   (Foundations ruling 3).

### Slice 2: Ancient Writ and Matchmaker

8. **Ancient Writ.** ACTION: the holder buries the relic, draws 3
   legacies (fewer from a short deck), keeps 1 dormant and discards the
   others to the bottom of the legacy deck. A single drawn legacy is kept
   without a question, and an empty deck asks nothing.
9. **Matchmaker during a game.** When another seated player holds
   Matchmaker active, that player draws into their own hand and chooses
   for the Ancient Writ holder, off turn. The question names the player
   they choose for, "Matchmaker: keep a legacy for p1", as Setup's keep
   now does too. The owner never identifies the legacies they did not
   keep. A holder using Ancient Writ themself chooses for themself.
10. **Only Ancient Writ triggers Matchmaker.** Amber Doors is excluded by
    Matchmaker's printed text. Lost Doors' discard is the owner's choice
    (ruling 13).

### Slice 5: the Doors

11. **Both Doors resolve at Setup and When Explored.** "If you place your
    pawn here" is the player whose pawn was just placed at this site: the
    placer in `SetupPawnPlaced`, the explorer in `WhenExplored`
    (`EdificeSetupSupport.pawnPlacement`).
12. **Amber Doors.** "You may". On yes, every legacy the player holds,
    active and dormant, goes to the bottom of the legacy deck. They draw 2
    legacies and
    choose one to activate; the other stays dormant. A short deck draws
    what remains, and a single draw is activated without a question; an
    empty deck draws and activates nothing.
    Activation is The Mouth's: it sets the legacy's foundation, and Iron
    Hand activated by an Imperial is discarded at once. The activated
    legacy acts from then on. A SETUP power whose step has passed does not
    resolve, but one in the same window does (ruling 4).
13. **Lost Doors.** The player draws 1 dormant legacy, then discards one
    dormant legacy they hold, the new one included, to the bottom of the
    legacy deck. Active legacies are never discarded by it. With a single
    dormant legacy, it is discarded without a question; with none (an
    empty deck and no dormant legacy),
    nothing happens. Matchmaker plays no part.

### Slice 6: Chronicle cards

14. **Stone Baths.** CHRONICLE (SUN): in Sun step 2, a seated player whose
    pawn is at this site does not discard their dormant legacies. A dormant
    legacy whose goal they meet still activates; one whose goal they do not
    meet stays dormant on their lineage.
15. **Ruined Baths.** CHRONICLE (SUN): in Sun step 3, a seated Exile whose
    pawn is at this site keeps every active legacy, whether or not they
    meet its goal. Steadfast and the cap are unaffected.
16. **The Baths are read at the Sun.** The Sun is the Chronicle's first
    task, so the edifice and the pawns are still on the map. A pawn
    off the map (Quest to Distant Lands before its first Wake) is at no
    site.
17. **Ruined Temple's pledge.** "You may" burn a secret at Setup or When
    Explored. Each player who burns records a pledge for the game. Several
    players may place their pawns at this site in Setup, and each may
    pledge. A player with no secret is not asked.
18. **Ruined Temple at the win.** If the winner pledged, then at the win,
    before Crown Chancellor, every bandit site loses its bandits to the
    bandit supply and takes one of the winner's warbands (Imperial for an
    Imperial winner), their bank topped up when short. Crown Chancellor,
    Shape Empire and the rest then treat those sites as the winner's.
19. **Ruined Temple's double.** Score Lineage Points doubles the winner's
    legacy bonus. The 3 base points are not doubled. Gifts are spent from
    the doubled total.

## Approach

### Slice 1: Reliquary Raid

`ReliquaryRaid` is a WHEN PLAYED power whose steps are
`ReliquaryDraws.steps(power, player, source)`. `ReliquaryRaidCard` moves
from `HoldingDiscordDenizens` to sit beside its power, and that file is
deleted.

### Slice 2: the legacy deck and the draw

- **Deck.** `CardZones.legacyDeck: Vector[LegacyId]`, top first, beside
  the edifice deck. Game start fills it from the deal (ruling 1), and
  `CardIndex` indexes it as `CardContainer.Deck(CardDeck.Legacy)`. It is a
  stack like the other decks: a move into it names the top or the bottom,
  and still never a facedown face (R6). `OperationPipeline.boxed` no
  longer counts a move into it as leaving play.
- **Hand.** `CurrentGameState.legacyHands: Map[PlayerId, Vector[LegacyId]]`
  beside `temporaryHands`, which keeps holding world cards only so Search
  and its powers are untouched. Both are `Location.Hand(player)` and
  `CardContainer.Player(player, PlayerCardArea.Hand)`, so a legacy in a
  hand is private to its holder with no new projection rule (ruling 6).
- **Draw.** `LegacyDraws`, beside `ReliquaryDraws`: a `Branch` that reads
  the deck when reached, then a `Draw` of its top N into the drawer's
  hand. `Draw` is a hidden outcome (`HiddenOutcomes`), so undo never takes
  a draw back. An empty deck writes "The legacy deck was empty."
- **Bottom.** Every move into the legacy deck names the bottom:
  `LegacyKeep.returnLegacies`, `IronHand.inGameDiscard` and its Chronicle
  discard, The Mouth's Iron Hand discard, and Resolve Legacies' returns.
- **Ancient Writ.** An ACTION through the relic ACTION path: it buries the
  relic, then `LegacyDraws` of 3 into the keeper's hand, then a keep
  decision that moves one legacy to the owner's lineage, dormant, and the
  others to the bottom of the deck. The keeper is
  `SetupLegacyRules.keeper`, which already reads Matchmaker's seated
  active holder in any phase. Its power id is fixed to
  `relic.ancient-writ`. `AncientWritCard` leaves `HoldingRelics`, and
  that file is deleted.
- **Setup's keep.** Game start deals Quiet Ambitions' draws to nobody:
  the deck keeps the whole shuffled order less the public row.
  `LegacyKeep.step` draws 2 into the keeper's hand through `LegacyDraws`,
  then keeps as Ancient Writ does. `LegacyDeal.drawn`,
  `LegacyKeep.drawn`, `LegacyKeep.matchmade` and the projection's
  Matchmaker arm are deleted. The keep's lines are unchanged.
- **Saved games.** Journals from before this slice need not replay: their
  moves into the legacy deck name no position.

### Slice 3: the keep in Foundation III

`SetupProcedure` calls `LegacyKeep.step` for every seat, in seat order,
inside Foundation III's resolution in later games, and leaves the call in
each player's pawn turn for a first game. `LegacyKeep.drawn` reads the
lineage's baseline (R13) and needs no change. Secret Society and Seer now
follow the keep. A Quest to Distant Lands holder who places no pawn keeps
in Foundation III like everyone else. The plan checks that a game saved
mid-Setup replays: answers are keyed by decision id. Since slice 2 the
keep draws its own legacies, so moving the keep moves the draw with it.

### Slice 4: power order

A new `OfferHost`, `OrderedWindow`, wraps the `SetupPawnPlaced` fold. On
every fold it gathers the powers that apply to the placer, reads which
already resolved from the answers, and asks a `ChooseOne` headed "Choose a
power to resolve" while two or more remain. Its shape is a pure function of
the offers and the answers, so a resumed walk folds it the same way (the
`OfferHost` contract). The Standard Bearer and Wealthy move their holder
check into a `Branch` at their step (ruling 5). The test that pins R27's
registration order is replaced by one that pins it only when nothing is
asked.

### Slice 5: the Doors

Both faces contribute to `SetupPawnPlaced` and `WhenExplored`, as Proving
Grounds and Great Forge do. The Mouth's private activation is extracted to
a shared `LegacyActivation` that both The Mouth and Amber Doors call.
Amber Doors moves every held legacy to the bottom of the legacy deck,
draws 2 through `LegacyDraws`, then asks which to activate. Lost Doors
draws 1, then asks which dormant legacy to discard to the bottom.
`AmberDoorsCard` leaves `HoldingEdifices`.

### Slice 6: Chronicle cards

- **Baths.** `ResolveLegacies.flip` (step 2) and `dormancy` (step 3) skip
  the players the Baths exempt, read through a new
  `BathsRules.exempt(ready, face)`: the site holding E04 with that face up,
  and the seated players whose pawns are there (Exiles only for Ruined
  Baths). `StoneBathsCard` leaves `HoldingEdifices`, and that file is
  deleted.
- **Pledge.** `templePledges: Set[PlayerId]` on the game, written by a
  recorded operation when a player burns. At Setup the burn is one of the
  powers slice 4's host orders.
- **The win.** A step at the start of `ChronicleWinnerProcedure`, before
  Crown Chancellor, rules the bandit sites for a pledged winner.
- **The double.** `LineageScoring.bonus` doubles for a pledged winner, and
  the scoring line names it.

## Hidden information

- A drawn legacy is in the drawer's hand, which only the drawer sees:
  the owner, or Matchmaker's holder while they choose for the owner
  (rulings 6 and 9). A kept legacy is dormant and facedown on the owner's
  lineage.
- The legacy deck is not projected for any viewer (ruling 6).
- The draw is a hidden outcome.
- The power-order question lists the placer's own active legacies, which
  are faceup, and the edifice at their site, which is public.

## Lines

Settled against existing shapes in each slice's plan:

- "p2 took a relic from the Reliquary with Reliquary Raid"
- "p2 drew 3 legacies with Ancient Writ", then "p2 kept a legacy"; with
  Matchmaker, "p3 drew 3 legacies for p2 with Matchmaker", then "p3 kept
  a legacy for p2 with Matchmaker"
- "p2 discarded their legacies with Amber Doors", then "p2 activated Seer"
- "p2 drew a legacy with Lost Doors", then "p2 discarded a dormant legacy"
- "The legacy deck was empty."
- "p2 burned a secret at Ruined Temple"
- "p2 took rule of 3 bandit sites with Ruined Temple"
- The scoring line names the doubled bonus in R28's shape.

The Baths add no line: a legacy they keep is simply not among the
discards. The power-order pick adds no line (ruling 4).

## Testing

- One suite per card: `ReliquaryRaidSuite`, `AncientWritSuite`,
  `AmberDoorsSuite`, `LostDoorsSuite`, `BathsSuite`, `RuinedTempleSuite`.
- `LegacyDeckSuite`: game start's deck, a draw from the top, a short
  deck, an empty deck, and a discard at the bottom (a Quiet Ambitions
  return, an Iron Hand discard, an Ancient Writ discard).
- Matchmaker during a game: another holder draws into their hand and
  chooses, and the owner does not identify the legacies not kept.
- Setup's keep: game start deals no legacy, each keep draws into the
  keeper's hand, a short deck in keep order, and the Matchmaker suites
  unchanged in what each viewer identifies.
- The keep's position: a first game after pawn placement, a later game in
  Foundation III, Matchmaker in both, and replay of a game saved mid-Setup.
- `OrderedWindowSuite`: Wealthy before Amber Doors; Amber Doors first,
  discarding Wealthy so it does not resolve; a Standard Bearer that Amber
  Doors activated resolving in the same window; a single power asking
  nothing; a resume inside the window.
- Projection: only a hand's holder identifies a legacy in it.
- Chronicle: Stone Baths at step 2, Ruined Baths at step 3, Ruined
  Temple's bandit sites before Crown Chancellor for an Exile and an
  Imperial winner, the doubled bonus with a gift.
- `CardCatalogSuite` with `catalog/holding/` empty; the first-game
  generator's goldens unchanged (Discord stays E01, Nomad E06); the Setup
  and Explore goldens updated where slices 3 and 4 change them.

## Docs

Each slice updates, as it lands:

- `docs/rules/ambiguities.md`: the rulings above, and Legacies ruling 3
  amended (ruling 2);
- `docs/rules/rules-reference.md`: a "Digital:" note on each rule a card
  changes, and on Later games step 5 for the keep;
- `docs/rules/implementation-traceability.md`: each card's row, and the
  Explore row's "Lost Doors and Ruined Temple wait" note removed;
- `docs/catalog/README.md`: the holding sentence once the folder is empty;
- `docs/ROADMAP.md`: **Cards that act on legacies** deleted when slice 6
  lands; **Simultaneous setup effects** narrowed by slice 4; the "Legacy
  powers: in play" item's Matchmaker clause moved here.

## Out of scope

- Ordering simultaneous effects on several players, and ordering any window
  but `SetupPawnPlaced`.
- Any display of the legacy deck.
- Eras and the reset of lineage points.
