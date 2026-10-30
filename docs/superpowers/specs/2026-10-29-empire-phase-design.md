# Empire phase design

Status: designed, 2026-10-29. Slice 1a implemented, 2026-10-29.

Roadmap item: **Phase - Empire**. Chancellor and Citizen roles in play,
Imperial warbands, the Imperial Campaign, the Stable Regime and Successor
endings, Citizenship and exile, and the Empire part of later-game setup, so
that game 2 starts from the Chronicle that game 1's Chronicle Phase journals.

Rule sources are the Combined Rulebook (CR) pp. 8-9, 13, 17, 19, 30 and
34-35, and the glossary on pp. 44-45. Rulings made while designing are listed
under [Rulings](#rulings) and go into `docs/rules/ambiguities.md` with the
slice that first needs them.

## Goal

A campaign continues past its first game: a seated Chancellor and Citizens
play with purple Imperial warbands, rule Imperial sites, campaign with and
against each other, change roles in play, and win through Stable Regime or as
Successor. Game start accepts an evolved Chronicle with an Empire.

## Current state

The model already holds the Empire's state; live play has no Imperial
behaviour.

- `Role` (Exile, Citizen, Chancellor) lives on `LineageState.role` in the
  live `CampaignState`. `PlayerState` has no role. `RoleChanged` exists and
  only the Chronicle emits it.
- `ForceKind.Imperial`, `SiteRuler.Empire`, the Reliquary pile and the Grand
  Scepter card exist. The Grand Scepter's restriction is implemented; its
  action, campaign and negotiation powers are not.
- The purple bank is created only by `ImperialWarbandsTaken`, which Crown
  Chancellor emits. Game start creates Exile and bandit banks only.
- Four guards refuse any role but Exile: `StateBasedEvaluation.supported`,
  the minor-action gate in `MinorActionProcedures`, `BeginRestProcedure`, and
  game start (`GameStartRules.buildPlan`).
- About fifteen places hard-code `ForceKind.Exile(lineage)` as a player's own
  warband kind, and about ten power helpers skip an Empire ruler.
- Turn order always starts at `setup.firstPlayer`. Supremacy counts only
  sites an Exile rules.

## Slices

Each slice gets its own implementation plan, written when the slice starts
against the code at that time, and its own branch off `0.2.0-alpha`.

1. **1a, Empire in play outside the Campaign.**
2. **1b, Imperial Campaign.**
3. **2, Empire endings.**
4. **3, Role changes in play.**
5. **4, Later-game setup.**

Slices 1a to 3 are reached through test fixtures only: game start keeps
refusing Imperial roles until slice 4. Each slice ships its browser UI and
its frontend tests on fixture states.

## Shared rules

These apply across slices. Each slice section refers to them rather than
restating them.

### Role

`LineageState.role` is the only source of a player's role. Every change in
play emits `RoleChanged`, as the Chronicle already does.

### Ruler

`SiteRule.ruler` keeps returning `SiteRuler.Empire` for a site with Imperial
warbands. Shape Empire, the Chronicle and the Supremacy count read it as the
Empire's side.

A new turn-aware resolver in the model, `SiteRule.rulingPlayer`, answers
"which player rules this site". For an Empire site it returns:

- the turn player, when the turn player is Imperial (Seal of the Empire);
- otherwise the Chancellor, including outside any turn (Steward of Imperial
  Domains, and ruling R2).

Exactly one player rules an Imperial site at a time (ruling R1). Every
caller that asks which player rules a site goes through the resolver; callers
that ask whether the Empire holds a site keep using `SiteRule.ruler`.

### Warbands

- A player's own warbands are of kind `PlayerForceKind.of`: purple for an
  Imperial player, the lineage's colour for an Exile.
- One purple bank of 24 warbands is the warband bank of every Imperial
  player. A killed purple warband returns to it.
- The purple bank is created when the first Imperial appears: at game start
  (slice 4) or when a player becomes the Chancellor in play (slice 3).
  `ImperialWarbandsTaken` creates it in both cases.
- Every other player is an enemy, Imperial or not (glossary, "Enemy").

### Boards

The player board follows the role.

| Role | Supply refresh | Vision slot |
|---|---|---|
| Exile | existing `ExileSupply` bands | yes |
| Chancellor | warbands in the purple bank: 18+ gives 6, 11-17 gives 5, 4-10 gives 4, 0-3 gives 3; maximum 7 | no |
| Citizen | the Chancellor's current Supply | no |

Unspent Supply carries over on every board: a refresh adds one per unspent
Supply, capped at 7. A Citizen matches the Chancellor's current Supply (the
marker as it stands, which includes the Chancellor's own carried surplus) and
then adds their own unspent Supply (ruling R3).

Playing a true Vision faceup requires a Vision slot, so Imperial players
cannot. The Conspiracy needs no slot: it resolves and returns to the box, so
Imperial players can play it (CR p. 30).

### An Empire that starts in play

When a player becomes the Chancellor in a game with no Empire (slice 3):

- the current round keeps its turn order; the next round starts with the
  Chancellor (ruling R4);
- the next end of round uses the Empire's round-end rules (slice 2);
- the Usurper Limiter is removed from the round track at once. Its limit
  applies only "in games with no Empire" (ruling R5). In a game that never
  has an Empire, it is still removed at the end of round 3.

### Grand Scepter source

A player who becomes the Chancellor takes the Grand Scepter from its holder,
or faceup from the box when no one holds it. This extends ruling 10 in
`docs/rules/ambiguities.md` from Crown Chancellor to every source.

### First-game toggle

CR p. 30 recommends ignoring "become a Citizen" powers in a player group's
first game; it does not forbid them (ambiguity F8). The setup option
`ignoreCitizenshipPowers` is on by default in a first game. While it is on,
"become a Citizen" powers are shown disabled with the hover reason
"Citizenship is ignored due to setup option". Slice 3 reads the flag; the
Settings Feature phase lets players change it during a game.

## Slice 1a: Empire in play outside the Campaign

A fixture game with a Chancellor and Citizens plays correctly through Wake,
Act (except the Campaign action) and Rest.

- **Guards.** `StateBasedEvaluation.supported`, the role part of the
  minor-action gate and `BeginRestProcedure` accept Imperial roles. The
  minor-action gate keeps its fixed-profile check. Game start keeps refusing
  until slice 4.
- **Ruler.** Route these through `SiteRule.rulingPlayer`:
  `PowerAccess.ruledSites`, `SiteRulers.rulerOfCard`, `RulerToll`,
  `RulerBans`, `SecretPolice`, `FortressRules`, `AncientBloodline`, and the
  consent checks for "Enemies cannot" powers, which test enmity against the
  resolved player. Curfew, Forced Labor, Toll Roads and Way Station then
  work unchanged.
- **Empire-shared protections.** "If ruled by the Empire, all Imperials
  have this power" extends a protection printed at a site: while the Empire
  rules its site, it covers every Imperial player. `SiteRulers.protectedAt`
  returns those players. Forgotten Vault and Oaken Fortress use it now;
  Hall of Debate and Tome Guardians use it when they are implemented.
- **Hall of Ministers.** It no longer treats all Imperials as one side: the
  Hall's site resolves to its ruling player, and every other player is an
  enemy (ruling R1).
- **Warband kind.** Move Warbands, Begin Rest and Finish Rest take a
  player's warband kind from `PlayerForceKind.of`, and read the purple bank
  for an Imperial player.
- **Turn order.** One `TurnOrder.roundStart`: the Chancellor if there is
  one, otherwise `setup.firstPlayer`. Finish Rest and the end of round both
  use it.
- **Wake.** The Usurper flip, the Usurper win and the Vision win apply to
  Exiles only.
- **Visions.** Playing a true Vision faceup requires a Vision slot (see
  [Boards](#boards)).
- **Oathkeeper.** Under Supremacy the Empire's sites count as one side,
  whose claim the Chancellor holds; Citizens never hold the Supremacy title.
  Under the other goals, Imperial players compete as individuals. The Vision
  of Conquest's site count goes through the resolver: at an Exile's Wake the
  Chancellor's count is the Empire's sites.
- **Supply.** A `ChancellorSupply` table with the bands under
  [Boards](#boards). A Citizen's Finish Rest uses the Citizen rule.
- **Chancellor's Eye.** A Citizen moving purple warbands from their site to
  their board needs the Chancellor's consent.
- **Warband Transfers.** A new minor action: an Imperial player moves
  warbands to or from another Imperial player's board when that player's pawn
  is at their site and that player consents.
- **UI.** Purple warbands; a role badge on each seat; the board side, with
  the Chancellor's Supply bands and no Vision slot on Imperial boards; the
  Warband Transfers action; consent prompts for Chancellor's Eye and Warband
  Transfers.
- **Docs.** Traceability rows 91, 92, 95, 110 (in part), 117 and 118.
  Remove the "Empire ruler not supported" and "every game is all-Exile"
  comments that stop being true.
- **Not in 1a.** The Campaign action, Military Parade and Siege Engines (1b),
  the Grand Scepter's powers (slice 3), and Desecrated Ground, whose ruined
  face is not implemented.

## Slice 1b: Imperial Campaign

- **Defender.** `CampaignDefender` gains no case. `defenderAt` resolves an
  Imperial site's defender to a player: for a conquest target it is always
  the Chancellor (Steward of Imperial Domains, Imperial Infighting). A target
  whose defender resolves to the attacker is not a legal target, so the
  Chancellor cannot target Imperial sites, while a Citizen can. The defending
  force at an Imperial site is the purple warbands there.
- **Raids.** An Imperial player may raid another Imperial player, since
  every other player is an enemy.
- **Warband kind.** `CampaignConquest`, `CampaignBattle` (raid losses and the
  fallback attacker kind), `CampaignPlanApplication` and `CampaignNotes` use
  `PlayerForceKind.of`.
- **Conquest.** An Imperial winner places purple warbands and the site
  becomes Imperial. A Citizen who wins against Imperial sites may place no
  warbands and leave the bandits to rule them, through the existing choice of
  whether to place.
- **Imperial Tactics.** An Imperial player may use battle plans in any
  Imperial player's play area with that player's consent, and at Imperial
  sites with the Chancellor's consent. The attacker and defender cannot use
  the same plan. Any Imperial player may pay a plan's costs or meet its
  requirements, split between players: each contributor consents to their
  share before the plan resolves, as an extension of the existing
  cost-gathering step. The Oathkeeper title is a defensive plan shared the
  same way.
- **Cards.** Military Parade's Imperial Allies clause; Siege Engines with an
  Imperial owner.
- **UI.** Imperial-site targets; shared battle plans labelled with their
  owner; the split-cost picker; consent prompts for each contributor and for
  plans at Imperial sites.
- **Docs.** Traceability row 104.

## Slice 2: Empire endings

These apply at the end of a round when the game has an Empire, including one
that started in play (from the next end of round).

- **Stable Regime.** At the end of rounds 5, 6 and 7, if the Chancellor or a
  Citizen holds the Oathkeeper title, roll the purple end die with the game's
  seeded randomness. The Chancellor wins on a 6, a 5-6, and a 3-6
  respectively. The roll is a logged event and the Game Log shows it.
- **War Exhaustion.** At the end of round 8, in order:
  1. an Imperial Oathkeeper: the Chancellor wins;
  2. an Exile Usurper wins;
  3. an Exile with a revealed Vision whose goal they meet wins; ties go to
     Conquest, then Rebellion, Sanctuary and Faith;
  4. otherwise the Chancellor wins.
- **No Empire.** The existing path is unchanged.
- **Successor.** Whenever the Chancellor would win, a Citizen who meets the
  Successor goal for the current Oathkeeper goal wins instead. At most one
  Citizen can meet it.

  | Oathkeeper goal | Successor goal |
  |---|---|
  | Supremacy | holds more relics than the Chancellor and than each other Citizen |
  | The People | holds the Darkest Secret |
  | Protection | holds the People's Favor |
  | Devotion | holds the Grand Scepter |

- **Model.** `VictoryKind` gains `StableRegime`, `ChancellorExhaustion` and
  `Successor`. Crown Chancellor already handles a Citizen winner; finished
  game summaries and the Chronicle's Throne input read the new kinds.
- **UI.** The end die in the Game Log; the victory screen names the kind of
  win, such as "Successor to the Chancellor".
- **Docs.** Traceability row 119.

## Slice 3: Role changes in play

### Becoming a Citizen

One procedure shared by every source (CR p. 34):

1. `RoleChanged` to Citizen.
2. Replace the player's warbands on their board and on the map with purple
   warbands. If the purple bank runs short, the player chooses which to
   replace, and the rest return to their own bank.
3. Discard their revealed Vision, if any.
4. If they hold the title on its Usurper side, flip it to Oathkeeper. Under
   Supremacy the title also passes to the Chancellor.
5. End their Act phase if it is their turn.
6. Refresh their Supply to the leftmost space (7).

### Becoming the Chancellor

In a game with no Empire, a "become a Citizen" power makes the player the
Chancellor instead (CR p. 34). It runs the same steps with the role
Chancellor; the player also takes the Grand Scepter (see
[Grand Scepter source](#grand-scepter-source)), the purple bank of 24 is
created, and the rules under
[An Empire that starts in play](#an-empire-that-starts-in-play) apply.

### Exiling a Citizen

1. `RoleChanged` to Exile.
2. Replace the purple warbands on their board with warbands of their
   colour. Excess purple warbands return to the purple bank. Imperial
   warbands on the map stay.
3. End their Act phase if it is their turn.
4. Refresh their Supply to the leftmost space (7).

### Sources

- **Self-exile.** A new minor action for a Citizen: give the Chancellor 1
  favor per purple warband on their board, at least 3. A Citizen without
  that much favor cannot take it.
- **Grand Scepter, action.** An Exile holder becomes a Citizen, or the
  Chancellor in a game with no Empire.
- **Grand Scepter, negotiation.** A new deal term, "(an Exile) becomes a
  Citizen". Only the Scepter's holder may propose it, only for an Exile, and
  it applies when the deal is accepted, alongside the other terms. Accepting
  the deal is the Exile's consent.
- **Grand Scepter, battle plan** (3 burnt favor). An Imperial player
  victorious against a Citizen exiles them.
- **Denizens.** Martial Culture, Long-Lost Heir, Bewitch (which also takes a
  relic from the Reliquary), Ancient Pact and Royal Ambitions.
- **The Throne's Offer Citizenship step.** The new Chancellor offers
  Citizenship to Exiles, including players who became Exiles earlier in the
  same Chronicle (ruling 5); each accepts or declines. Accepting changes the
  role and replaces the player's warbands on sites with purple warbands.
  The other Citizenship steps do not apply in the Chronicle, since boards
  reset at the next setup.

### UI and docs

- **UI.** The Citizenship term in the deal editor; the Grand Scepter and
  self-exile actions; the warband replacement picker; the Throne's offer and
  its accept or decline prompts; disabled "become a Citizen" options under the
  first-game toggle.
- **Docs.** Traceability rows 110, 120 and 129.
- **Not in slice 3.** The other Empire holding cards stay with the catalog
  batches: Council Seat, Naysayers, False Prophet, Grand Mask, Imperial Seal,
  and the Empire edifices and legacies.

## Slice 4: Later-game setup

The Empire part of CR pp. 8-9 and 13. Foundation faces stay normal and
legacies stay refused (see [Out of scope](#out-of-scope)).

- **Game-start refusals.** Lift "a non-empty world requires the Empire",
  "Imperial roles require the Empire" and "starting advisers require
  later-game setup". Keep the legacy and starting-relic refusals and the
  other checks. Add "an Empire-less later game requires Wide Horizons" for a
  Chronicle with a Chancellor where neither the Chancellor nor any Citizen is
  seated.
- **Chancellor.** If the Chancellor's lineage is not seated, a random seated
  Citizen becomes the Chancellor (seeded randomness), and the absent
  Chancellor's lineage becomes a Citizen in the box (ruling R6). The
  Chancellor takes the Chancellor board, the purple bank and the Grand
  Scepter, and chooses one Reliquary relic to take facedown, a setup decision
  only they see. The Chancellor takes the Oathkeeper title; the legacy
  exceptions come with Legacies. The end die is in play and the Usurper
  Limiter is not.
- **Banners.** Foundation V gives the People's Favor to the Chancellor when
  the Oathkeeper goal is The People, and Foundation VI gives the Darkest
  Secret to the Chancellor when it is Devotion.
- **Players.** Each seated lineage takes its stored starting advisers
  facedown, 1 favor, 1 secret and 3 warbands on its board (purple for an
  Imperial player), and Supply 7.
- **Imperial sites.** Before the foundation steps, fill site slots going
  Hinterward from the Chronicle's `world` (the Recent Sites up to the Empire
  divider). Stored edifices start intact, other denizens faceup and stored
  relics facedown; empty relic slots stay empty. Each site gets 1 Imperial
  warband. The existing foundation-driven setup fills the remaining slots.
- **Foundation IV.** Only a player with no starting advisers draws 3
  denizens into their temporary hand.
- **Player choices**, starting with the Chancellor and going clockwise:
  1. Place pawn. An Exile places at the Hintermost Empire site or at any
     bandit site; an Imperial player places at any Empire site.
  2. Choose adviser. Skipped for a player whose temporary hand is empty.
  3. Reveal cards, starting advisers included.
- **UI.** Imperial roles on player selection; the Chancellor's Reliquary
  pick; pawn placement offered only at legal sites.
- **Docs.** Traceability rows 80, 82, 83, 127 and 132. Remove the roadmap's
  "Saved-campaign continuation" item.

## Rulings

- **R1, one ruler.** On an Imperial player's turn only that player rules
  Imperial sites; the Chancellor does not also rule them. A site has exactly
  one ruler (CR p. 15).
- **R2, default ruler.** Outside any turn, as in the end-of-round checks and
  the Chronicle, the Chancellor rules Imperial sites.
- **R3, Citizen Supply.** A Citizen refreshes to the Chancellor's current
  Supply plus their own unspent Supply, capped at 7.
- **R4, mid-round Chancellor.** A Chancellor who appears mid-round leaves the
  current round's order unchanged; the next round starts with them.
- **R5, Usurper Limiter.** The Usurper Limiter's limit applies only in games
  with no Empire, so it ends, and its marker is removed, when an Empire
  starts in play.
- **R6, absent Chancellor.** When a seated Citizen becomes the Chancellor
  because the Chancellor's lineage sits out, that lineage becomes a Citizen
  in the box.
- **R7, no Imperial Usurper in play.** An Imperial player never holds the
  Usurper side during play. An Exile who becomes a Citizen flips the title to
  Oathkeeper at once (slice 3). The check is suspended in the Chronicle phase,
  where the Chronicle crowns the Usurper winner after the game ends.
- **R8, Warband Transfers consent names the transfer.** The partner is asked
  after the way and the amount are chosen, so the ask names them. Each
  consent covers one exact transfer; a refusal stops only that one for the
  turn.
- **Defender is the attacker.** A Campaign target whose defender resolves to
  the attacker is not legal.
- **First game.** "Become a Citizen" powers are ignored in a first game by a
  setup option, on by default (ambiguity F8).

## Testing

- **Fixtures.** A `Table.imperial(chancellor, citizens*)` test step seats
  a Chancellor and Citizens and puts the purple bank on the table, as every
  `Table` step edits state directly. Slices 1a to 3 test through it; slice 4
  adds real game starts.
- **Unit suites** for each rule: the resolver's three cases; the Supply bands
  with a Citizen's surplus; a defender resolving to the attacker; the end die
  at each round with a seeded roll; Successor for each goal; becoming a
  Citizen and exile, including a short purple bank; the Throne's offer.
- **One scenario per slice** through `OathDriver`, in the style of the
  existing end-to-end suites:
  - 1a: a Citizen's turn in which Toll Roads pays the Citizen;
  - 1b: a Citizen conquers an Imperial site and leaves it to the bandits;
  - 2: Stable Regime with a Successor taking the win;
  - 3: an Exile accepts Citizenship in a deal and later exiles themself;
  - 4: a Chronicle evolved from game 1 starts game 2 with the Empire. The
    `ChronicleEndToEndSuite` test that expects "Imperial roles require the
    Empire" changes to expect the start.
- **Frontend suites** on fixture states for each slice's UI, including hover
  reasons on disabled options.
- **Wire.** Round-trip tests for new operations, `VictoryKind` cases and deal
  terms. Old journals need not decode.
- **Gates for each slice.** `./sbtw clean coverage test coverageReport` with
  statement coverage of at least 92%, `./sbtw frontend/test`,
  `python3 scripts/check-architecture.py` and
  `python3 scripts/check-markdown-links.py`.
- **Docs for each slice.** Its traceability rows, plus `rules-reference.md`
  and `ambiguities.md` for the rulings it first needs.

## Out of scope

- **A later game with no Chancellor and no Citizen seated.** It needs
  Foundation I's altered face, Wide Horizons, and its Explore action. Game
  start refuses it until the Foundations item adds them.
- **Stored foundation faces.** Game start keeps the normal faces until the
  Foundations item.
- **Legacies.** Game start keeps refusing legacies and starting relics until
  the Legacies item.
- **Other Empire cards.** The holding cards listed under slice 3.
- **Production authentication.** Tracked as Later item X6.
- **Changing the first-game toggle during a game.** The Settings Feature
  phase.
