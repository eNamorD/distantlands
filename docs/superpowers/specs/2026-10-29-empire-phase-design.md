# Empire phase design

Status: designed, 2026-10-29. Slice 1a implemented, 2026-10-29. Slice 1b-i implemented, 2026-10-30. Slice 1b-ii implemented, 2026-11-01. Slice 2 implemented, 2026-11-01. Slice 3a implemented, 2026-11-01. Slice 3b implemented, 2026-11-01. Slice 4 implemented, 2026-11-02.

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
4. **3, Role changes in play**, as two plans: 3a, the role changes with
   self-exile and the Scepter's Action; 3b, the Scepter's battle plan and
   deal term, the five denizens and the Throne's offer.
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

Slice 1b ships as two plans: 1b-i, the Imperial Campaign without shared
battle plans, then 1b-ii, Imperial Tactics.

### 1b-i: Imperial Infighting

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
- **Cards.** Military Parade counts only the one enemy's advisers (ruling
  R9). Siege Engines killing purple warbands logs "Killed 2 Imperial
  warbands at {site}."
- **UI.** Imperial-site targets in the Conquest targets question.
- **Docs.** Traceability row 104, in part.

### 1b-ii: Imperial Tactics

An Imperial player may use battle plans in any Imperial player's play area
with that player's consent, and plans at Imperial sites with the
Chancellor's consent (ruling R10). Any Imperial player may pay a plan's
costs, including a variable amount such as Unstable Summon's X, or meet its
requirements, split between players (rulings R11, R12). The Oathkeeper title
is a defensive plan shared the same way. Consent takes the form of offers, so
no one approves anything.

- **Offers.** While an Imperial player's battle-plan decision is open, any
  other Imperial player may submit or withdraw offers at any time, even the
  user's enemy. An Exile neither offers nor receives them. An offer holds:
  - faceup plans the ally rules: their play area, the Oathkeeper title when
    they hold it, and, for the Chancellor, plans at Imperial sites. Only the
    acting player may reveal a facedown card, so a facedown plan cannot be
    offered;
  - requirements the ally meets: holding the Darkest Secret (the banner, or
    the Yew Staff faceup), holding the People's Favor, or having their pawn
    at a site (ruling R16);
  - four amounts: favor to pay, secrets to pay, favor to burn, secrets to
    burn.

  The offer is the consent.
- **Using offers.** An offered plan is choosable in the user's normal plan
  window, labelled with its owner. A plan whose requirement an offer meets
  becomes usable. A chosen plan's costs, fixed and variable, are paid from
  the allies' offered amounts first, in turn order starting after the user,
  then by the user. A surcharge on the plan and Peace Envoy's gift are paid
  the same way, but the gift never comes from the enemy it is given to. The
  option's price shows only the user's share.
- **The window.** An Imperial user's plan window is asked when the user has
  a plan they could use with aid, even one they cannot pay for alone, or an
  ally has a faceup plan the user could use. Otherwise it is skipped as
  today.
- **Withdrawing.** An ally may withdraw any unused part of an offer. Spent
  amounts and chosen plans are no longer theirs to withdraw.
- **Lifetime.** Offers belong to one side's plan window and end when it
  finishes; unused amounts stay with the ally. They stand while the walk is
  still inside the Campaign, such as at a plan's amount question, since the
  plan's payment reads them, and clear when the walk ends or another plan
  window opens; allies can change them only while the plan decision waits. The attacker and defender
  cannot use the same plan: a plan one side used is shown unavailable to the
  other, with the reason "The attacker used this plan".
- **Offer panel.** Each other Imperial player sees a panel headed "You may
  offer your battle plans, to pay costs and to meet requirements for p3".
  They build the offer the way a plan is chosen: the plans and requirement
  sources they could offer (faceup advisers and relics, cards at Imperial
  sites for the Chancellor, the Darkest Secret, the Yew Staff, the People's
  Favor, their pawn) are highlighted on the board, and a click adds one to
  the panel's Plans or Requirements list, or takes it out again. Facedown
  cards are never highlighted. Below the lists, four steppers set the
  amounts. A pending addition shows no mark, a saved item a check mark, a
  pending removal is struck through, and a used item shows a lock and can no
  longer be clicked; a saved amount shows how much is offered and how much is
  used, and cannot go below what is used. The button reads "Offer" before
  the first offer, "Save changes" while anything is pending and "Offered",
  disabled, otherwise. Saving sends the whole offer, which replaces the one
  before it. The user needs no panel: offered plans are options of the
  normal plan window.
- **Log.** Each saved offer writes one public line naming what it added and
  what it took back, such as "p2 offered Military Parade and 2 favor to
  pay." or "p2 withdrew Towering Rampart."
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

One procedure shared by every source (CR p. 34). No player performs it, so
a restriction against a player's placements, such as Vow of Wandering,
does not stop its replacements (ruling R20):

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
  Citizen". Only the Scepter's holder may propose it, only for an Exile
  other than themself, and it applies when the deal is accepted, after the
  other terms. Accepting the deal is the Exile's consent.
- **Grand Scepter, battle plan** (3 burnt favor). An Imperial player
  victorious against a Citizen exiles them.
- **Denizens.** Martial Culture, Long-Lost Heir, Bewitch, Ancient Pact and
  Royal Ambitions. Bewitch, Ancient Pact and Royal Ambitions also take a
  random relic from the Reliquary (rulings R21-R23).
- **The Throne's Offer Citizenship step.** The new Chancellor offers
  Citizenship to Exiles, including players who became Exiles earlier in the
  same Chronicle (ruling 5); each accepts or declines. Accepting changes the
  role and replaces the player's warbands on sites with purple warbands.
  See Chronicle rulings 20-22: a broken Empire's sites pass to the former
  Chancellor, offers go to seated Exiles in seat order, and the Throne
  assumes unlimited purple warbands.
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
  Citizen becomes the Chancellor (seeded randomness, recorded as the plan's
  `firstPlayer`), and the absent
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
  warband, Cradlemost first, while the 24 purple warbands last after the
  boards; a site the supply misses takes bandits (R30). The existing
  foundation-driven setup fills the remaining slots, each keeping its stored
  cards (R27). The Homeland edifice check is a first game's only (R26), and
  game start refuses an Empire with no site or more than 8 (R28).
- **Foundation IV.** Only a player with no starting advisers draws 3
  denizens into their temporary hand.
- **Player choices**, starting with the Chancellor and going clockwise:
  1. Place pawn. An Exile places at the Hintermost Empire site or at any
     bandit site, never at a site with no warbands (R25); an Imperial player
     places at any Empire site. Before the first pawn, the Chancellor takes
     the Reliquary relic (R29).
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
- **R9, Military Parade.** The New Foundations card applies to the one
  enemy only. Its "including Imperial Allies" text is a base-game concept and
  is ignored.
- **R10, plans at Imperial sites.** In a Campaign, plans at Imperial sites
  belong to the Chancellor's side on every turn. The Chancellor uses them
  freely; any other Imperial needs the Chancellor's consent, even on their
  own turn.
- **R11, split costs.** Every cost of a battle plan may be split between
  Imperial players, including a variable amount. The warband sacrifice still
  comes from the defender's force.
- **R12, shared requirements.** Any Imperial player who meets a plan's
  requirement (holding the Darkest Secret, holding the People's Favor, having
  their pawn at a site) may meet it for the plan's user, with that player's
  consent. A requirement met this way for a plan that checks it at the end of
  the Campaign (Hearts and Minds) stays met until then.
- **R13, Captains and Wild Allies at an Empire site.** A Citizen may use
  Captains or Wild Allies to campaign from a site the Empire holds, against
  the Chancellor. The site's warbands join the attack and count once, for the
  attacker, never in the defense; they do not die in the victory, and only
  the board's survivors are placed (Captains FAQ). The FAQ's "you do not need
  to target the site" refers to a base-game rule and does not apply.
- **R14, Keep.** Keep's site is used by whoever defends it in the Campaign,
  so a defeated Chancellor keeps Keep at an Empire site even on a Citizen's
  turn.
- **R15, Hospital.** Hospital saves warbands only if its user rules its site
  by the turn's ruler at the Campaign's end (Hospital FAQ). A Chancellor
  defending on a Citizen's turn may choose it at an Empire site (R10), but it
  saves nothing; a Citizen using it on their own turn has theirs saved.
- **R16, what aid covers.** Imperial Tactics lets an ally pay a plan's
  costs and meet its requirements, and nothing more. A plan that counts what
  its user rules or holds (Great Crusade, Pledge of Defense, Kindred Warriors,
  Nature Worship) counts only the user's own cards. An ally's pawn at a site
  meets "your pawn is at this site" (Towering Rampart).
- **R17, the Grand Scepter counts.** Supremacy's Successor counts every
  relic each Imperial player holds, the Grand Scepter included.
- **Defender is the attacker.** A Campaign target whose defender resolves to
  the attacker is not legal.
- **First game.** "Become a Citizen" powers are ignored in a first game by a
  setup option, on by default (ambiguity F8).
- **R25-R30, later-game setup.** Exile pawn sites (R25), Homelands in
  later games (R26), stored cards at the remaining sites (R27), the Empire
  always holds a site (R28), the Chancellor's Reliquary pick (R29) and a
  short purple supply at setup (R30); see
  [ambiguities.md](../../rules/ambiguities.md#empire-rulings).

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
  - 1b-i: a Citizen conquers an Imperial site and leaves it to the bandits;
  - 1b-ii: a Citizen defender uses the Chancellor's plan, paid in part by
    the Chancellor;
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
