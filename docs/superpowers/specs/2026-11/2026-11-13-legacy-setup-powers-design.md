# Legacy Setup powers

**Status:** designed 2026-11-13, approved section by section by the owner.
Not yet planned. Plan it once the [Legacy Chronicle powers](2026-11-13-legacy-chronicle-powers-design.md)
batch has landed: this batch reuses its holder seam, and slice 4 changes
the Setup adviser step that batch's slice 3 changes for Oaths of Loyalty.

**Builds on** the [Legacies design](2026-11-13-legacies-design.md), which
landed the legacy lifecycle and left every power in holding, and the
Legacy Chronicle powers design, whose general rulings this batch carries
over to Setup.

## Goal and scope

The SETUP halves of the legacies, plus the two in-play halves a SETUP
half needs: Quest to Distant Lands' first-Wake placement and World
Crafter's Wide Horizons fill. Twelve legacies:

| Slice | Powers | Hooks |
|---|---|---|
| 1. Seam and game start | Circle of Swords, Scepter Bearer, Iron Hand (SETUP half), Rival (SETUP half) | The holder seam; `LaterGameSetup`; `SetupPawnSites`; Notes at Setup's start |
| 2. Foundations | World Crafter, Populist, Matchmaker | Imperial Maps; Explore's fill; a new banks window; the Quiet Ambitions keep |
| 3. World deck | Secret Society, Seer | A new world-deck window; `DecisionQuery.ChooseAmounts` |
| 4. Player choices | The Standard Bearer, Wealthy, Quest to Distant Lands | `SetupPawnPlaced`; the pawn step; the adviser step; Wake |

Slices run in order. Slice 1 builds the seam the rest use. Quest comes
last because a player with no pawn reaches every rule that reads a pawn's
site.

Every power here acts only in a later game. In a first game every legacy
is dormant, and a dormant legacy has no power.

### When each power runs

Setup for later games runs Table, Players, Empire, Foundations I-VI,
Sites and World Deck, then Player Choices (CR pp. 8-13). This engine
builds the table at game start, with no questions (`GameStartRules`,
`FirstGameSetupMaterializer`, `LaterGameSetup`), and asks every Setup
question in the walker's Setup procedure (`SetupProcedure`).

The batch splits the powers by that line. A power with no choice that
changes what game start builds is applied at game start, where the CR
order can be kept exactly. A power with a choice is a walker step.

| CR step | Power | Runs at |
|---|---|---|
| Players | Circle of Swords, Scepter Bearer | Game start |
| Empire | Iron Hand's warbands | Game start |
| Before the foundations | Rival (Exile) | Game start |
| Foundation I | World Crafter: Imperial Maps; Wide Horizons' Explore fill | Walker: the Imperial Maps step; Explore |
| Foundation II | Populist | Walker: a new step after Imperial Maps |
| Foundation III | Matchmaker | Walker: the Quiet Ambitions keep |
| World Deck | Secret Society, then Seer | Walker: new steps |
| Place Pawn | Iron Hand (everyone at any site), Rival (its sites), Quest (no pawn) | Walker: `SetupPawnSites`, the pawn step |
| After the pawn | The Standard Bearer, Wealthy | Walker: `SetupPawnPlaced` |
| Before Choose Adviser | Quest's swap | Walker: the holder's step |
| First Wake | Quest's placement and deferred steps | Wake |

Game start does Circle of Swords' purple warbands before the Empire's
sites take theirs. That is why it cannot be a walker step: the walker
would draw from whatever purple warbands the sites left.

## Rulings

Taken with the owner on 2026-11-13. They open a "Legacy Setup powers
rulings" section of `docs/rules/ambiguities.md`, which the implementation
rulings then continue.

### General

1. **Only seated players' active legacies act.** A legacy's SETUP power
   applies when a seated player holds it active at the moment its step is
   reached. A boxed lineage's legacies never act, and neither does the
   Public Ambitions row, which nobody holds.
2. **"If you are an Imperial" or "an Exile" is read at the same moment.**
   Game start's Empire setup has already chosen the Chancellor by then (a
   random Citizen when none is seated), so every role is settled.
3. **Every "you may" is the holder's decision,** with its own id,
   `setup.legacy.<power>.<player>`, and a per-item suffix where one power
   asks more than once. Declining is always an option.
4. **Choosing several cards is one choose-many.**

Simultaneous setup effects are not part of this batch. The `SetupEnd`
and `SetupPawnPlaced` powers still resolve in site order, and the
ROADMAP's "Simultaneous setup effects" item stays open. None of this
batch's powers needs an order choice.

### Slice 1: game start

5. **Circle of Swords.** The holder's board starts with 8 warbands
   instead of 3. An Exile takes them from their own colour's supply, as
   many as it holds. An Imperial takes purple warbands, boards first:
   every other Imperial board takes its 3 before the holder takes up to 8
   from what is left of the 24. The Empire's sites come after the boards,
   and a site the supply misses takes bandits (Empire ruling R30).
6. **Scepter Bearer.** An Imperial holder starts with the Grand Scepter
   instead of the Chancellor. The Chancellor keeps the Oathkeeper title
   and the Reliquary pick. If the holder is the Chancellor, nothing
   changes. With no Empire the Grand Scepter stays in the box, and the
   power does nothing.
7. **Iron Hand.** It always applies, whether or not the holder was in the
   previous game. Each site restored up to the Empire divider takes one of
   the holder's warbands instead of the Imperial warband, or instead of
   the bandits in Setup With No Empire. Those sites are the holder's, so
   the Empire may start ruling no site, which overrides Empire ruling R28.
   Every player may place their pawn at any site in play, which overrides
   R25. The holder is always an Exile here: Chronicle powers ruling 18
   discards Iron Hand from an Imperial.
8. **Rival.** For an Exile holder, before the foundations, which is before
   Imperial Maps and before the banks are filled: the three Cradlemost
   empty site slots take the next three Recent Sites. They are populated
   by the Empire rules: stored edifices intact, other denizens faceup,
   stored relics facedown, empty relic slots left empty. Each site gets one
   of the holder's warbands. Fewer sites are placed if the map or the
   Atlas runs short. Their denizens add favor to the banks as any site on
   the map does (Foundation II), and Imperial Maps fills only the slots
   still empty after them. The holder may place their pawn at these sites
   as well as at the sites R25 allows them.
9. **Game start's powers are logged** by Notes at the start of Setup,
   before the Public Ambitions line: "Circle of Swords gave p2 eight
   warbands", "p1 started with the Grand Scepter with Scepter Bearer",
   "Iron Hand placed p3's warbands at the Empire's sites", "Rival placed
   p4's warbands at Deep Woods, Fallen Shrine and Wild Steppe".

### Slice 2: foundations

10. **World Crafter, Imperial Maps.** When Imperial Maps resolves (a
    Chancellor is in play, Foundation I lies on its normal face, and slots
    are still empty after Rival), the holder fills the slots instead of
    the Chancellor and chooses Recent or Forgotten Sites for each slot, in
    Hinterward order. Each slot is one decision,
    `setup.legacy.world-crafter.<player>.<n>`. Game start fixes the number
    of slots, so the ids never depend on the walk. Visibility is as now:
    the Recent option names the Atlas's front site, and nothing else about
    the Atlas is shown. Each placement is logged, "p2 placed Deep Woods
    from Recent Sites with World Crafter", in place of the single "filled
    the map from" line.
11. **World Crafter, Wide Horizons.** After any player's successful
    Explore, the holder chooses Recent or Forgotten Sites instead of the
    explorer. It is the decision Explore asks now, with a different actor,
    so the holder may answer during another player's turn. If the holder
    is the explorer, nothing changes. "p3 chose Forgotten Sites with World
    Crafter".
12. **Populist.** A new step after Imperial Maps, or where it would run.
    The holder may divide all the favor on the six suit banks evenly: each
    bank ends with the total divided by six, rounded down, and the holder
    takes the remainder. It applies under both faces of Foundation II.
    Favor that edifices add to the banks at `SetupEnd` is not part of it.
    "p2 divided the favor evenly among the banks with Populist and took 1
    favor".
13. **Matchmaker.** In Quiet Ambitions' keep, the holder answers every
    other player's keep instead of them. The decision id stays
    `setup.legacy-choice.<player>`; only the actor changes. The holder
    sees both drawn legacies while choosing. The owner sees only the legacy
    kept for them and never the one returned. After the choice the holder
    has no lasting view of it: the kept legacy is named only to its owner,
    as every dormant legacy is. A player who drew one legacy keeps it with
    no question, as now. Under Public Ambitions the power does nothing.
    "p3 kept a legacy for p2 with Matchmaker".

### Slice 3: world deck

Game start has already built the world deck, so both powers work on the
deck it built.

14. **Secret Society.** A new step after Populist and before Seer.
    - The holder may take one Vision from the world deck, any except the
      Conspiracy. It joins their advisers facedown, and Visions Drawn
      advances once. It is one `ChooseOne` of the Visions, plus Decline;
      only the holder sees their names.
    - The deck is then rebuilt with the card's layout. The remaining four
      Visions are lifted out, and the denizens keep their order. Two
      Visions are shuffled at random into the first 12 cards (the top 10
      denizens), and two into the next 17 cards (the next 15 denizens).
      With fewer than 25 denizens, the base rule's fallback applies
      instead: the Visions are shuffled through the whole deck (CR p. 12).
      The draw is a walker random outcome, hidden as the deck is.
    - The Vision is not a starting adviser, so Oaths of Loyalty does not
      bury it.
    - Excess advisers are discarded right after the holder places their
      pawn, by the normal adviser-limit rule, in one choose-many. Choose
      Adviser never takes the holder over the limit: a player who draws a
      Ties of Fellowship hand carried no starting advisers.
    - "p2 took a Vision as a facedown adviser with Secret Society". The
      other viewers see one more facedown adviser.
15. **Seer.** A new step after Secret Society. It has no "may", so the
    holder must answer it.
    - The holder sees every Vision in the world deck at once, each with
      its own amount from 1 to n + v, where n is the number of denizens in
      the deck and v the number of Visions. The amount is the Vision's
      final position, counted from the top, once all are inserted. No two
      Visions may share a position; the answer is refused if two do.
    - Applying it: lift the Visions out, the denizens keeping their order,
      then insert them in ascending order of position. Each lands exactly
      where it was chosen.
    - It needs a new query, `DecisionQuery.ChooseAmounts(slots, distinct)`:
      one decision with one amount per item, each within its bounds, and
      with `distinct` set no two equal.
    - Only the holder sees the Visions' names and the positions; the deck
      stays as hidden as before for everyone else. Under Teeming World the
      deck holds only Visions, and the holder sets their order.
    - "p2 placed the Visions with Seer".

### Slice 4: player choices

16. **The Standard Bearer.** In the holder's own `SetupPawnPlaced` window,
    after the pawn's line. The holder may take a banner from the shared
    bank, the People's Favor or the Darkest Secret, whichever the
    Oathkeeper goal left there. They then put two more of that banner's
    own resource on it from the shared bank: favor on the People's Favor,
    secrets on the Darkest Secret, as many as the bank holds. It is one
    `ChooseOne` of the banners in the bank, plus Decline, and no question
    when neither is there. "p2 took the People's Favor with The Standard
    Bearer".
17. **Wealthy.** In the same window. It has no "may": the holder takes 1
    favor from each of two other players, or 3 favor from the shared bank.
    The first decision chooses between the two. "Two players" is offered
    only when at least two other players hold favor, and with one option
    no question is asked. For two players, a `ChooseMany(2, 2)` picks them
    from the other players who hold favor. A short bank gives what it
    holds. "p2 took 1 favor from p1 and p3 with Wealthy", "p2 took 3 favor
    from the shared bank with Wealthy".
18. **Quest to Distant Lands, no pawn.** The holder's pawn decision offers
    "Don't place your pawn" as well as the sites. A holder with no pawn
    gets nothing from the edifice powers that hook `SetupPawnPlaced`.
    The Standard Bearer and Wealthy still run at that point in Setup. The
    holder's Choose Adviser, their Secret Society excess discard and their
    Reveal Cards wait for their first Wake (ruling 20), so the discards
    follow the region where the pawn is placed and Choose Adviser still
    comes before Reveal Cards.
19. **Quest to Distant Lands, swap.** A holder who placed no pawn may then
    swap one card from the Dispossessed for one of their own.
    - First a `ChooseOne` over the Dispossessed, plus Decline. The
      Dispossessed is private: only the holder sees it.
    - Then a `ChooseOne` over the cards they may give: their starting
      advisers, or, when they have none, the Ties of Fellowship hand they
      drew. Foundation IV draws only for a player with no starting
      advisers, so a holder never has both.
    - The two cards trade places. A facedown adviser's replacement is a
      facedown adviser, a hand card's replacement goes to the hand for
      Choose Adviser, and the given card goes to the Dispossessed.
    - A swap is not a gain, so Oaths of Loyalty does not bury a card
      swapped in as a facedown adviser. The card's italic note about Oaths
      is a reminder, not an exemption. A hand card swapped in is still
      chosen at Choose Adviser, and Oaths buries that choice as it buries
      any starting adviser gained there.
    - "p2 swapped a card with the Dispossessed with Quest to Distant
      Lands". Only the holder sees which cards.
20. **Quest to Distant Lands, first Wake.** At the start of the Wake of a
    seated player who has no pawn, before anything else, they must place
    it at any site in play. It is a `ChooseOne` with no Decline,
    `wake.legacy.quest.<player>`. It is keyed on the missing pawn rather
    than on holding the legacy, so it cannot be lost. Then come the steps
    ruling 18 deferred: Choose Adviser (with Oaths' burial), the Secret
    Society excess discard and Reveal Cards. The edifice powers that hook
    `SetupPawnPlaced` do not fire: the Wake is not Setup. Until the
    placement, the holder stands at no site: no rule finds them at a site,
    and no site's effect applies to them. "p2 placed their pawn at Deep
    Woods".

## Approach

- **The holder seam.** Each power asks who holds it seated and active,
  read live. The Chronicle batch's `ChronicleLegacyRules.holder` is reused
  if it fits Setup; otherwise both read a shared `LegacyHolders`.
- **Game start.** `LaterGameSetup` gains a legacy pass in CR order:
  Circle of Swords and Scepter Bearer with the players, Iron Hand with the
  Empire's sites, then Rival. Rival's sites must exist before Foundation
  II's bank count and Imperial Maps' slot count read the map. Nothing at
  game start is journaled, so the slice 1 Notes are how the log learns of
  these powers. The boards and the map are built by
  `FirstGameSetupMaterializer` from the setup plan, so the pass lives
  there, and only Scepter Bearer is applied in `LaterGameSetup`
  (implementation ruling R8).
- **Walker steps.**
  - New empty windows where a step is added: `SetupBanks` after Imperial
    Maps (Populist) and `SetupWorldDeck` (Secret Society, then Seer).
    Each is listed in the windows suite.
  - The changed steps ask the holder seam's queries, which keeps each step
    one piece of code: Matchmaker (`keeper`), World Crafter (`mapFiller`
    for Imperial Maps, `exploreEndChooser` for Explore), Iron Hand and
    Rival (`pawnSites`, read by `SetupPawnSites`), and Quest
    (`mayStayOff`).
  - The Standard Bearer and Wealthy hook `SetupPawnPlaced`.
  - Each card leaves `HoldingLegacies` as its SETUP half lands, and
    `PowerImplementationStatus` lists it. Rival left holding whole with the
    Chronicle batch (its implementation ruling R7), so only its status
    changes; Iron Hand left holding whole too, so only its status changes.
- **New model pieces.** `DecisionQuery.ChooseAmounts`, with its codec,
  `DecisionQueries.wellFormed`, answer validation and a frontend control
  of one amount per item, its Confirm disabled while two amounts are
  equal. A walker random outcome for Secret Society's reshuffle.
- **Quest's Wake.** A branch at the start of Wake for a seated player with
  no pawn. The Ties of Fellowship hand already lives in `temporaryHands`,
  so no saved field is added and older saved games still decode. Slice 4
  audits every reader of a player's pawn site that fails when it is
  missing (`PawnSiteMissing`), so a player with no pawn breaks no other
  player's turn before their own.
- **Hidden information.** Matchmaker's drawn legacies, Secret Society's
  Vision, Seer's Visions and positions, and Quest's Dispossessed and
  swapped cards are shown to the holder only. The projection redacts them
  for every other viewer, and each has a redaction test per viewer. World
  Crafter shows the Atlas as Imperial Maps does now.
- **Decisions.** Every new `Decide` has a resume test, and no two
  decisions share an id. No Branch's children or ids depend on state the
  walk itself changes: World Crafter's slot count, Seer's bounds and
  Wealthy's options are read from what game start fixed or from earlier
  answers.

## Slice 1: seam and game start

The holder seam; `LaterGameSetup`'s legacy pass; `SetupPawnSites` reading
`pawnSites`; the Notes at Setup's start. Circle of Swords, Scepter
Bearer and Iron Hand leave holding; Rival's SETUP half is implemented.

Tests: Circle of Swords for an Exile and for an Imperial beside two other
Imperial boards, with an Empire site taking bandits; Scepter Bearer as a
Citizen and as the Chancellor; Iron Hand with an Empire and with no
Empire, every player offered every site; Rival filling three slots, then
Imperial Maps filling the rest, the banks counting Rival's denizens; a
boxed lineage's active Circle of Swords doing nothing; a first game
asking nothing. A later-game Setup golden log with all four.

## Slice 2: foundations

The `SetupBanks` window; Imperial Maps reading `mapFiller`; Explore
reading `exploreEndChooser`; the keep reading `keeper`. World Crafter,
Populist and Matchmaker leave holding.

Tests: World Crafter mixing Recent and Forgotten Sites slot by slot, as
an Exile; World Crafter choosing the end for another player's Explore;
Populist dividing under both faces of Foundation II, and declining;
Matchmaker keeping for two players, the owner's and a third viewer's
projections, and a one-legacy draw asking nothing. Resume tests for each
decision. A golden log.

## Slice 3: world deck

The `SetupWorldDeck` window; `DecisionQuery.ChooseAmounts` from the model
to the frontend; the random reshuffle. Secret Society and Seer leave
holding.

Tests: Secret Society's Vision reaching the holder's advisers, Visions
Drawn advancing, and the rebuilt deck holding two Visions in the top 12
cards and two in the next 17; the fallback under 25 denizens; the excess
discard under Teeming World; Seer's positions landing exactly, a
duplicate position refused, and Teeming World ordering only Visions;
Secret Society then Seer for one holder; redaction of both. A frontend
test for the `ChooseAmounts` control. A golden log.

## Slice 4: player choices

The Standard Bearer and Wealthy on `SetupPawnPlaced`; the pawn step's
"Don't place your pawn"; the swap; the Wake branch; the pawn-site audit.
The Standard Bearer, Wealthy and Quest to Distant Lands leave holding.

Tests: The Standard Bearer with one banner, both and none in the bank;
Wealthy from the bank and from two players, and a two-player game asking
nothing; Quest swapping with a facedown adviser under Oaths (not buried),
and with a Ties hand card (buried at the Wake's Choose Adviser); the
first Wake placing the pawn, then Choose Adviser discarding by its
region, then Reveal Cards; another player's turn before the holder's
first Wake with no failure. A golden log from Setup through the holder's
first Wake.

## Docs

Each slice updates, as it lands:

- `docs/rules/rules-reference.md`: a "Digital:" note on each Setup step a
  power changes;
- `docs/rules/ambiguities.md`: the rulings above;
- `docs/rules/implementation-traceability.md`: each legacy's row, and the
  Setup rows' dependency notes;
- `docs/ROADMAP.md`: the "Legacy powers: setup" item ticked when slice 4
  lands. The in-play item still names every other in-play half, and
  "Simultaneous setup effects" stays open.

## Out of scope

- Every in-play half except Quest's first Wake and World Crafter's Wide
  Horizons fill: The Ear's, Golden Key's and the other ACTION, WAKE,
  TRAVEL, BATTLE PLAN, END OF ROUND and WHEN halves.
- Simultaneous setup effects: who orders powers that share a Setup
  window.
- Matchmaker on draws during a game (Amber Doors, Lost Doors, Ancient
  Writ), with the other cards that act on legacies.
- Iron Hand's discard during a game.
- Eras and the reset of lineage points.
