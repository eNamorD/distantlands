# Catalog Batch 9: Edifice Faces

**Status:** landed. Designed 2026-11-02; slices 1 to 10 landed on
2026-11-02. The rulings taken while implementing are under
[Rulings made while implementing](#rulings-made-while-implementing), and
[Out of scope](#out-of-scope) is the refreshed engine-blocked list the
roadmap points at.

**Builds on** [Catalog batch 8](2026-10-12-catalog-batch-8-design.md), whose
vocabulary, cost rules, walker shapes and log-line rules apply unchanged.
The per-card rulings of [batch 4](2026-10-05-catalog-batch-4-rulings.md)
that apply to the whole of that batch (restriction icons, site capacity,
"your site" and "your region", turn order) apply here too.

## Goal and scope

After batch 8 and the site powers, `src/test/resources/powers/unimplemented.txt`
lists 133 power ids. Twelve are `site.*` ids and one is Ancient Bloodline
(denizen 165), which another agent is moving out of holding; they are done
and out of this scope. Of the 120 left, 36 are legacies and belong to
**Phase - Chronicle steps**. This design triages the other 84: 46 edifice
faces, 24 denizens and 14 relic powers.

The bar is the one the brief sets: a card is in the batch only when every
mechanism it needs exists in the engine today, named by trait, window or
operation, so a slice changes `gameplay/powers/` and its registries and
nothing else. A card ships whole or not at all. Where a card misses by one
small seam, the triage says which seam, so the roadmap's engine-blocked item
can be refreshed from the [Out of scope](#out-of-scope) section.

The batch takes 22 edifice faces in 10 slices. Slices 1 to 7 are the core
(17 cards); slices 8 to 10 are independent and may be dropped or deferred
without touching the others. Every remaining denizen and relic power is
engine-blocked or belongs to a later phase.

| Slice | Count | Cards |
|---|---|---|
| 1. Setup rules | 4 | E01r Hall of Mockery, E18r Lost Guardians, E27r Ruined School, E30r Broken Horn |
| 2. Banner target protections | 2 | E01i Hall of Debate, E18i Tome Guardians |
| 3. Recover restrictions | 2 | E17i Relic Patrols, E17r Bandit Patrols |
| 4. Travel rules | 2 | E24i Boiling Lake, E29i Forbidden Forest |
| 5. Card-play and Trade rules | 2 | E24r Still Boiling Lake, E26i Hallowed Spring |
| 6. Pawn-placing phase powers | 3 | E14i Stone Portal, E25r Sinking Passages, E25i Hidden Passages |
| 7. Raid battle plans | 2 | E23i Spiral Castle, E23r Spiral Dungeons |
| 8. Site-card actions | 2 | E30i Forest Horn, E09r Giant's Rampage |
| 9. Card-draw actions | 2 | E10i Temple of Ancestors, E21r Squalid District |
| 10. Relic swap | 1 | E13i Underground Library |

Slices need nothing from each other. They are ordered simplest first; slice
6 is the first to use a waivable power-use restriction on a power's own
use, and slice 7 the first battle plan that replaces a Raid step, so each
comes after the plain rules that share its tests.

### The bar

No change outside `gameplay/powers/` and the two registries is approved.
Four helpers inside `powers/` widen their types, which is not an engine
change:

- `SiteSlotMoves` takes a `CardId` mover, so a ruined edifice may move
  itself (Giant's Rampage). Its `mover: DenizenId` is Giant Python's.
- `RegionDiscard` takes the region to discard in, not only the actor's
  pawn's (Forest Horn). It also moves out of `private[whenplayed]`.
- `PayOrAsk` is not reused; slice 6 declares a `PowerUseRestriction` with a
  `Waiver` as City Wall does, on the power's own use.
- `SiteRelicTake`'s peek step is reused over several sites (Underground
  Library); its take step is not.

If a slice finds it needs more, it stops and asks, as batch 8's "Seams
used for the first time" rule says.

### Catalog and roadmap corrections

- **The Hall of Mockery's and Lost Guardians' banner move is a later-game
  rule.** "Move it to the shared bank; players cannot start with it" undoes
  the later-game setup step that gives the Oathkeeper goal's banner to the
  Chancellor (rules 4.5 Foundations V and VI). In the all-Exile first game
  both banners start in the shared bank (`FirstGameSetupMaterializer`,
  holder `None`), so the move has nothing to do. The engine cannot move a
  banner to the shared bank today (`BoardControlOperations.moveBanner`
  accepts a play area destination only); the Chronicle phase adds that with
  the later-game setup. Slice 1 ships the token placement and the burn and
  records this.
- **Temple of Ancestors is not Search Party.** The roadmap's Search Party
  item says a card play nested in a WHEN PLAYED fires no triggers. Temple
  of Ancestors is an ACTION, and Oracle already plays a drawn card through
  `CardPlayProcedure.unchecked` from an ACTION, where the card-play windows
  fold as usual (batch 2 ruled Oracle's play subject to Vow of Obedience,
  Secret Police and Sacred Ground). The same path serves here.
- **Spiral Castle and Spiral Dungeons are battle plans.** The catalog text
  reads like an automatic rule, but the card data tags both "Battle Plan"
  and Spiral Dungeons says "Bandits will use this", which only a plan can.
  They join Towering Rampart as edifice battle plans.

## Triage

Every remaining unimplemented power, with the mechanisms it needs and
whether each exists. "Exists" names the trait, window or operation. The
verdict is **batch 9**, **blocked** (one line on what is missing) or
**phase** (which later phase). Printed text is abbreviated.

### Edifice faces

| Id | Card | Printed text (short) | Mechanisms needed | Exist today | Verdict |
|---|---|---|---|---|---|
| e01.intact | Hall of Debate | If its ruler holds the People's Favor, their enemies cannot target it. | `OptionRestriction` at `CampaignTargetSelection`; ruler of an edifice's site; banner holder | Yes: Circlet of Command, Forgotten Vault; `SiteRulers.rulerOf`, `EdificeSetupSupport.siteOf`; `BannerRules.holder` | batch 9, slice 2 |
| e01.ruined | Hall of Mockery | SETUP (END): 4 favor on the People's Favor, move it to the shared bank; burn 1 favor from each bank. | `SetupEnd` `Transform`; `Move(Favor, SharedBank, OnBanner)`; `Burn.favor` from each `FavorBank`; banner move to shared bank | Yes: `MarketRule`; `ResourceOperations` accepts `OnBanner`; Bandit Market burns. The banner move: no, and moot in the alpha (see corrections) | batch 9, slice 1 |
| e03.intact | Grand Canal | Act as if this site is a Coast. | A site's terrain read from live state | No: terrain is static on the `TravelSitePowers` objects (`coastSites` sets), and Narrow Pass reads the same sets | blocked: live terrain |
| e03.ruined | Lost Lagoon | Act as if this site is an Island. | Same | Same | blocked: live terrain |
| e04.intact | Stone Baths | CHRONICLE (SUN): dormant legacies not discarded. | Chronicle step, legacies | No | phase: Chronicle steps |
| e04.ruined | Ruined Baths | CHRONICLE (SUN): active legacies kept without their goal. | Same | No | phase: Chronicle steps |
| e05.intact | Amber Doors | SETUP: discard legacies to draw 2 dormant ones. | Legacy deck and states | No | phase: Chronicle steps |
| e05.ruined | Lost Doors | SETUP: draw a dormant legacy, discard one. | Same | No | phase: Chronicle steps |
| e07.intact | Wild Pastures | Its ruler may pay 1 favor instead of discarding a Nomad card from a power. | A hook that replaces an operation before it runs and asks a question | No: `takesBurn` replaces burns only, silently; `ExecutedTrigger` runs after | blocked: discard replacement |
| e07.ruined | Blasted Pastures | CHRONICLE (START OF THRONE): bury Nomad cards. | Chronicle step | No | phase: Chronicle steps |
| e08.ruined | Desecrated Ground | An Exile revealing a Vision here keeps their current one. | Several revealed Visions | No: `PlayerState.revealedVision` is one card ([rule gaps](2026-09-26-rule-gaps-in-implemented-cards-design.md)) | blocked: Vision collection |
| e09.intact | Giant's Path | ACTION: move this card to another site, place your pawn there. | A locked card moved by its own power | No: `LockedCards` refuses every `Move` of an intact edifice, consent or not | blocked: own-power lock exemption |
| e09.ruined | Giant's Rampage | ACTION: bury a denizen at any site; move this card there; place your pawn there. | `Bury.standard`; `SiteSlotMoves`; `PawnMoves.relocate` | Yes (Forest Warden's bury; Giant Python; Stone Portal's template) | batch 9, slice 8 |
| e10.intact | Temple of Ancestors | ACTION: draw the bottom world-deck card, play to advisers or discard as if searched. | `Draw` from `Deck(World)` bottom to `Hand`; `CardPlayProcedure.unchecked`; `PlacementLimit` | Yes: `Draw.position = Bottom` (Mushrooms); Oracle; Land Warden | batch 9, slice 9 |
| e10.ruined | Ruined Temple | SETUP: burn a secret; if you win, double legacy points, rule bandit sites. | Legacy scoring, end-of-game rule | No | phase: Chronicle steps |
| e11.intact | Great Spire | ACTION: 3 random Dispossessed cards to this region's pile; bottom 3 denizens of the pile to the Dispossessed. | A random draw from the Dispossessed | No: `Shuffle` takes a `SearchSource` (world deck, a discard pile) | blocked: Dispossessed shuffle |
| e11.ruined | Fallen Spire | SETUP (END): swap this pile's denizens with random Dispossessed cards; shuffle with its Visions. | Same | No | blocked: Dispossessed shuffle |
| e12.intact | Great Clock | ACTION: advance the round marker once. | A round-advance operation | No: `tracks.round` is written only by `RoundEnded`, which also lifts the Usurper Limiter | blocked: round advance |
| e12.ruined | Broken Clock | SETUP (END): advance the round marker once. | Same | No | blocked: round advance |
| e13.intact | Underground Library | ACTION: peek at this region's relics; swap one with a relic you have, facedown. | `Peek`; `SiteRelicSlot` and `RelicSlot` options; `Flip`; `Swap` site and play area | Yes: `SiteRelicTake`; Dream Thief and Enchantress swap; `CardMovementOperations` places a relic at a site | batch 9, slice 10 |
| e13.ruined | Drowned Library | At this site you may recover the relic deck's top card at difficulty 5. | A Recover whose source and difficulty a modifier changes | No: `RecoverProcedure.recovery` closes over the site's relics and difficulty | blocked: Recover source |
| e14.intact | Stone Portal | ACTION: at this site, place your pawn elsewhere; elsewhere, place it here. | `PaidAction`; `PawnMoves.siteChoice` and `relocate` | Yes: Whistle, Magic Carpet | batch 9, slice 6 |
| e14.ruined | Shattered Portal | ACTION: roll the round die, move that many sites Hinterward. | A round die | No: `DiceKind` has Attack and Defense | blocked: round die |
| e16.ruined | Hall of Bandits | CHRONICLE: bandit-ruled ruins not discarded. | Chronicle step | No | phase: Chronicle steps |
| e17.intact | Relic Patrols | Enemies of its ruler cannot recover at its ruler's sites. | `Restriction` at `RecoverActionEligibility` with `Waiver` | Yes: Vow of Peace's start refusal, Narrow Pass's waiver | batch 9, slice 3 |
| e17.ruined | Bandit Patrols | Enemies of its ruler cannot recover at bandit sites. | Same | Yes | batch 9, slice 3 |
| e18.intact | Tome Guardians | If its ruler holds the Darkest Secret, their enemies cannot target it. | As Hall of Debate | Yes | batch 9, slice 2 |
| e18.ruined | Lost Guardians | SETUP (END): 3 secrets on the Darkest Secret, move it to the shared bank. | `SetupEnd`; `Move(Secrets, SharedBank, OnBanner)` | Yes (`ResourceOperations` line for `OnBanner(DarkestSecret)`); the banner move is moot | batch 9, slice 1 |
| e19.intact | The Tribunal | WHEN NEGOTIATING: negotiate with players anywhere; binding promises. | Negotiation eligibility and promises | No | phase: Negotiation terms |
| e19.ruined | Bandit Tribunal | WHEN NEGOTIATING: players at bandit sites negotiate with each other. | Same | No | phase: Negotiation terms |
| e21.intact | Festival District | WHEN NEGOTIATING: give, receive, swap advisers. | Adviser terms | No | phase: Negotiation terms |
| e21.ruined | Squalid District | ACTION: discard any facedown advisers; draw that many denizens from the world deck's bottom as facedown advisers. | `ChooseMany` over `AdviserSlot`; `Discard.Denizen`/`Discard.Vision` from a play area; `Move` into a play area with `resultingOrientation` | Yes: Dream Thief's slots; card play's discards; Horned Mask's facedown move | batch 9, slice 9 |
| e23.intact | Spiral Castle | Victorious in a Raid: the defender's pawn here; they lose 2 Supply. | `BattlePlan` with `wrapping` at `CampaignRaidRelocation`; `SpendSupply(required = false)` | Yes: Towering Rampart, Keep's relocation replacement, Spoiled Supplies | batch 9, slice 7 |
| e23.ruined | Spiral Dungeons | Victorious (defender): the attacker's pawn here; they lose 2 Supply. Bandits use it. | `BattlePlan` for a bandit defender; `later` after the outcome | Yes: Battle Axes' bandit use, `PlanUse.won` | batch 9, slice 7 |
| e24.intact | Boiling Lake | Travel here without ruling it: kill 2 of your board's warbands if able. | `Transform` at `TravelCost` after the move, `AutomaticUse`; `Kill` from a play area | Yes: Tyrant; `Kill` takes any `PositionedLocation` | batch 9, slice 4 |
| e24.ruined | Still Boiling Lake | Cards cannot be played to this site. | `operationRestrictions` refusing a `Play` to a site | Yes: Forest Council's placement bans; `CardSlots.judge` marks the slot | batch 9, slice 5 |
| e25.intact | Hidden Passages | WAKE: at this site, with its ruler's consent, place your pawn anywhere. | WAKE `PhasePower`; a waivable `PowerUseRestriction` on its own use | Yes: Marble Fountains; City Wall's `waiver`, askable at the phase menu (batch 8, slice 10) | batch 9, slice 6 |
| e25.ruined | Sinking Passages | WAKE: at this site, burn 1 favor to place your pawn anywhere. | WAKE `PhasePower` with `Cost(favorBurnt = 1)` | Yes: `PhasePower.cost` pays burnt portions | batch 9, slice 6 |
| e26.intact | Hallowed Spring | Trading here for secrets, place both favor on the card. | `Transform` at `TradeCost` rewriting `PayCost` | Yes: `OperationRewrite` (Curfew, Vow of Wisdom) | batch 9, slice 5 |
| e26.ruined | Hiding Place | CHRONICLE (THRONE): keep a facedown relic into the next game. | Chronicle step | No | phase: Chronicle steps |
| e27.intact | School of Vines | Challenge an enemy's banner from any site. | A Challenge whose banner legality a power widens | No: `ChallengeProcedure.legalBanners` is hard-coded (roadmap, with the Sigils) | blocked: Challenge |
| e27.ruined | Ruined School | SETUP (END): 1 secret on each bandit site. | `SetupEnd`; `Move(Secrets, SharedBank, Site)` | Yes: Bandit Market with favor; `ResourceOperations` accepts a site | batch 9, slice 1 |
| e29.intact | Forbidden Forest | Enemies of its ruler cannot travel here unless they have a Beast adviser. | `Restriction` at `TravelActionEligibility` with `Waiver`; `EffectiveSuit.count` | Yes: Narrow Pass; batch 8, slice 8 | batch 9, slice 4 |
| e29.ruined | Bandit Forest | Imperials cannot place on or use bandit-ruled denizens. | Imperial players | No | phase: Empire |
| e30.intact | Forest Horn | ACTION: choose a region; discard its non-Beast denizens. | `PaidAction`; a region question; `RegionDiscard` | Yes: Dazzle's discard, `Button` options | batch 9, slice 8 |
| e30.ruined | Broken Horn | SETUP (END): bury this region's discard pile. | `SetupEnd`; `Bury` from `RegionalDiscard` | Yes: `Bury` moves to a deck bottom; Signal Trees moves a card out of a pile | batch 9, slice 1 |

### Denizens

| Id | Card | Printed text (short) | Mechanisms needed | Exist today | Verdict |
|---|---|---|---|---|---|
| 36 | Acting Troupe | Act as if it is a Beast or Order card. | A suit chosen per use | No | blocked: acting as another card (roadmap) |
| 166 | Ancient Pact | WHEN PLAYED: with the Darkest Secret, become a Citizen. | Citizenship | No | phase: Empire |
| 213 | Autumn Wind | WHEN PLAYED: shuffle all piles together, deal them out evenly. | A combined shuffle and deal | No | blocked (roadmap) |
| 67 | Bewitch | WHEN PLAYED: with the most secrets, become a Citizen. | Citizenship | No | phase: Empire |
| 197 | Bracken | Discards to the top or bottom of any pile. | A discard destination choice | No | blocked (roadmap) |
| 200 | Council Arbiter | WHEN NEGOTIATING: give, receive, swap advisers. | Adviser terms | No | phase: Negotiation terms |
| 123 | Council Seat | A Citizen cannot be exiled. | Exile and Citizen | No | phase: Empire |
| 146 | Deed Writer | WHEN NEGOTIATING: give, receive, swap sites. | Site terms | No | phase: Negotiation terms |
| 85 | False Prophet | WHEN PLAYED: a warband on a revealed Vision; play it when discarded. | Player-aware off-turn card play | No (roadmap) | blocked |
| 235 | Favored Son | WHEN PLAYED: take the People's Favor, adjust by its left ribbon. | A banner taken outside a Challenge with its ribbon | No (roadmap, Challenge) | blocked: Challenge |
| 207 | Glamor | Target an enemy's secrets in a Raid. | A new target kind | No (roadmap) | blocked |
| 44 | Long-Lost Heir | WHEN PLAYED: become a Citizen. | Citizenship | No | phase: Empire |
| 32 | Magician's Code | Challenging the Darkest Secret: 2 more secrets placed. | Challenge amount beyond the start gate | No (roadmap) | blocked: Challenge |
| 10 | Martial Culture | Defeat another Exile: become a Citizen. | Citizenship | No | phase: Empire |
| 78 | Master of Disguise | Act as if you had another player's advisers. | Acting as another card set | No (roadmap) | blocked |
| 21 | Naysayers | REST: if an Exile holds the title, take favor from the Chancellor. | Chancellor | No | phase: Empire |
| 161 | Pilgrimage | WHEN PLAYED: this site's denizens to the Dispossessed; draw as many back. | A Dispossessed shuffle | No (roadmap) | blocked: Dispossessed shuffle |
| 126 | Relic Hunter | Target facedown relics at targeted sites. | A new target kind | No (roadmap) | blocked |
| 220 | Reliquary Raid | WHEN PLAYED: take a random relic from the Reliquary. | A stocked Reliquary | `campaign.reliquary` exists but is empty until the Chronicle stocks it; a random draw from it is new | phase: Chronicle steps (does nothing in the alpha) |
| 88 | Royal Ambitions | WHEN PLAYED: ruling most sites, become a Citizen. | Citizenship | No | phase: Empire |
| 240 | Search Party | WHEN PLAYED: play one of a pile's bottom 3 as if searched. | A card play nested in a WHEN PLAYED | No (roadmap) | blocked |
| 221 | Tracker | ACTION: choose a pawn; WAKE next turn: go to it. | Memory across turns | No: `TurnState` resets each turn | blocked (roadmap) |
| 242 | Traveling Negotiator | WHEN NEGOTIATING: with any player. | Negotiation eligibility | No | phase: Negotiation terms |
| 27 | The Gathering | WHEN PLAYED: pawns come here, then a binding Negotiation. | A nested Negotiation | No | phase: Negotiation terms |
| 152 | Vow of Kinship | Your favor lives in the Nomad bank and is spent from there. | Costs and takes read from a bank as if from the board | No: `PayCost`, `Take`, `Burn` and every resource read go to `PlayArea`; the FAQ widens it to "any favor going to your board" | blocked: favor source redirection |

### Relics

| Id | Card | Printed text (short) | Mechanisms needed | Exist today | Verdict |
|---|---|---|---|---|---|
| R22 ancient-grit | Ancient Writ | ACTION: bury this to draw 3 legacies, keep 1. | Legacy deck | No | phase: Chronicle steps |
| R11 | Bandit Crown | Use cards at bandit sites as if you ruled them. | An as-if ruler for access, ruled cards and battle plans | No: `PowerAccess.ruledSites`, `RuledCards` and `PlanContext.rules` read `SiteRule.ruler`; the FAQ adds refill and last-warband interactions | blocked: as-if ruler |
| R10 | Cracked Horn | Discards to the bottom of the world deck. | A discard destination choice | No (roadmap) | blocked |
| R05 | Grand Mask | An Exile rules cards at Imperial sites on their turn. | Imperial sites | No | phase: Empire |
| R26 | Imperial Seal | ACTION: move Imperial warbands. | Imperial warbands | No | phase: Empire |
| R12 king-of-devotion | Ring of Devotion | Discard to your own region's pile. | A discard destination choice | No (roadmap) | blocked |
| R18 obsidian-cage.campaign | Obsidian Cage | Victorious: unkilled enemy warbands onto the Cage. | A warband store on a card | No (roadmap) | blocked |
| R18 obsidian-cage.action | Obsidian Cage | ACTION: move warbands from the Cage to a board. | Same | No | blocked |
| R43 | Sigil of the Eye | Challenge the Darkest Secret from any site. | Challenge banner legality | No (roadmap) | blocked: Challenge |
| R44 | Sigil of the Heart | Challenge the People's Favor from any site. | Same | No | blocked: Challenge |
| grand-scepter.action | The Grand Scepter | ACTION (Chancellor). | Chancellor | No | phase: Empire |
| grand-scepter.campaign | The Grand Scepter | Campaign (Imperial). | Same | No | phase: Empire |
| grand-scepter.negotiation | The Grand Scepter | WHEN NEGOTIATING (citizenship). | Same | No | phase: Empire and Negotiation terms |
| R34 | Whispering Stone | ACTION: negotiate in private with anyone. | Remote private Negotiation | No | phase: Negotiation terms |

### Legacies

All 36 `legacy.*` ids need the legacy deck, activation and goals, which
**Phase - Chronicle steps** owns. None does anything in an all-Exile first
game.

## Slice 1: Setup rules

Four `SetupEnd` rules in the shape of `MarketRule` (Bandit Market). Each
names `WhenExplored` beside `SetupEnd`, as E02 does, so the same
contribution serves an explore procedure later. Each is applicable only
while its face stands on the map.

- **Hall of Mockery** moves 4 favor from the shared bank onto the People's
  Favor (`Move(Piece.Favor(4), SharedBank, OnBanner(PeoplesFavor))`) and
  burns 1 favor from each favor bank with no burner, as Bandit Market does.
  It does not move the banner (see corrections). Template: Bandit Market.
  Line: "Placed 4 favor on the People's Favor and burned 6 favor from the
  banks." read from the step, counting what moved; a bank already empty
  burns nothing and the count says so.
- **Lost Guardians** moves 3 secrets from the shared bank onto the Darkest
  Secret. Template: Great Market's single-step note. Line: "Placed 3
  secrets on the Darkest Secret."
- **Ruined School** places 1 secret on each site bandits rule. Template:
  Bandit Market's placement half. Line: "Placed 2 secrets on the bandit
  sites." Players then take them with Take Wealth (rules 6.1.3 and 10.1),
  which already takes secrets.
- **Broken Horn** buries every card of its region's discard pile, in pile
  order: a `Bury(BuryableCard.Denizen | Vision, RegionalDiscard(region))`
  each, no returns since a discarded card carries no tokens. Template:
  Broken Forge's region sweep. Line: "Buried the Provinces discard pile (3
  cards)." or "The Provinces discard pile was empty."

Tests, for each: the rule runs once at Setup's end in a game whose first
eight sites hold the face, writes its line, and does nothing when the face
is in storage; a replayed Setup reaches the same state; the Game Log golden
of a generated first game that holds the face. Broken Horn is tested with a
Vision in the pile; Ruined School with no bandit site.

## Slice 2: Banner target protections

Hall of Debate and Tome Guardians are persistent rules that hide one
banner at `CampaignTargetSelection` through an `OptionRestriction`, as
Circlet of Command does, when the edifice's ruler holds the banner and the
acting player is an enemy of that ruler. "Target" is a Campaign's target
selection only (batch 3, "Target protections corrected"), so a Challenge
and a Conspiracy are not narrowed. Both join `TargetProtections`.

- Ruler: `SiteRulers.rulerOf` of the site `EdificeSetupSupport.siteOf`
  finds the intact face at. A player ruler is protected; under bandits or
  unruled nobody holds the banner through the edifice, so nothing is hidden.
- Line, on the hidden option: "Hall of Debate: {Blue}'s People's Favor
  cannot be targeted." and the same for Tome Guardians and the Darkest
  Secret.

Tests: the target is hidden with its reason for an enemy, offered to the
ruler's Raid of someone else, offered when the ruler does not hold the
banner, when the face is ruined, and when the edifice is at an unruled
site; a Raid with only the pawn as target still runs.

## Slice 3: Recover restrictions

Relic Patrols and Bandit Patrols refuse a Recover at its start with a
`Restriction` at `RecoverActionEligibility`, as Vow of Peace refuses a
Campaign, with a `Waiver` whose granter is the edifice's ruler, as Narrow
Pass's. The Act menu shows Recover unavailable with the reason, askable of
the ruler outside a Campaign (deferred: see the rulings made while
implementing).

- **Relic Patrols**: the actor is an enemy of the intact face's ruler and
  recovers at a site that ruler rules.
- **Bandit Patrols**: the actor is an enemy of the ruined face's ruler and
  recovers at a site bandits rule.
- Reasons: "Relic Patrols: Cannot recover relics at its ruler's sites." and
  "Bandit Patrols: Cannot recover relics at bandit sites.", each with the
  askable form ending " without ruler's consent.".
- A Recover another power starts (Catacombs' contribution) is the same
  action and is refused the same way.

Tests: refused for an enemy with the reason, allowed for the ruler, allowed
once the ruler consents (and the grant is remembered for the turn), not
refused under bandits-ruled Patrols for Relic Patrols (no ruler, no ruled
sites), and the question of whether bandits-ruled Bandit Patrols bind every
player (see the rulings).

## Slice 4: Travel rules

- **Boiling Lake** follows Tyrant: at `TravelCost`, after the pawn's move,
  an `AutomaticUse` kills 2 of the traveller's own warbands from their
  board (`Kill(Piece.Warbands(kind, 2), PlayArea(traveller))`), when the
  traveller does not rule the intact face. "If able" (rules 15.4): the kill
  is cut to what the board holds, and a board with none kills nothing. It
  follows the pawn that moves, so a Travel another player sent (Palanquin)
  counts, as the FAQ says ("Can I be forced to travel here and lose
  warbands? Sure can."). Line: "Killed 2 {Blue} warbands." or none when
  the board was empty.
- **Forbidden Forest** follows Narrow Pass: a `Restriction` at
  `TravelActionEligibility` refusing a route whose destination is the
  intact face's site, for an enemy of its ruler without a Beast adviser
  (`EffectiveSuit.count(..., Suit.Beast) > 0`, so Pledge to Discord's
  holder has none and Marriage counts). Waivable by the ruler. Reasons:
  "Forbidden Forest: Cannot travel here without a Beast adviser." and the
  askable form. A sent Travel's destination counts as its actor's answer
  (batch 8, slice 15), so Palanquin's user may ask the ruler.

Tests: Boiling Lake kills two, one, none; not for the ruler; off turn via
Palanquin; not when the face is ruined; the line. Forbidden Forest: hidden
destination with its reason, open for a Beast adviser, open for the ruler,
open after consent, under bandits unaskable.

## Slice 5: Card-play and Trade rules

- **Still Boiling Lake** declares an `operationRestrictions` refusing every
  `Play` whose destination is `Location.Site(x)` where x holds the ruined
  face, as Forest Council's placement ban refuses a `PayCost`. Card play's
  slot options at that site then show unavailable through
  `CardSlots.judge`, with "Still Boiling Lake: Cards cannot be played to
  this site." It binds every player, is not waivable ("Cards cannot"), and
  stops only plays: a card moved there by a power (Giant Python) is not
  played. New Growth's and Baron's other-site plays are refused there too.
- **Hallowed Spring** is a `Transform` at `TradeCost` that rewrites the
  Trade's `PayCost(actor, OnCard(source), Cost(favor = 1, favorBurnt = 1))`
  to `Cost(favor = 2)` when the Trade is for secrets and the source card
  stands at the intact face's site. It is a must, binds every player, and
  writes no line: the payment's generic line shows 2 favor placed. The
  source is read from the walk's answers, as Curfew reads it.

Tests: Still Boiling Lake hides the site's slots with the reason, leaves
the adviser plays and the discard open, leaves Giant Python's move open,
and is inert when the face is intact. Hallowed Spring: a Trade for secrets
at the site places 2 favor and burns none; a Trade for favor and a Trade
elsewhere are unchanged; Small Friends' as-if site counts as "at this
site" (item 13c).

## Slice 6: Pawn-placing phase powers

- **Stone Portal** is a `PaidAction` (1 secret). With the user's pawn at
  the intact face's site it asks "Stone Portal: place your pawn at which
  site?" over every other site (`PawnMoves.siteChoice`) and relocates;
  from a ruled site elsewhere it relocates the pawn to the Portal's site
  without asking. Line: "{Blue} placed at {site}." (`PawnMoves.placedKey`).
- **Sinking Passages** is a WAKE `PhasePower` with `Cost(favorBurnt = 1)`,
  usable when the user's pawn is at the ruined face's site, asking the
  destination over every other site. Same line.
- **Hidden Passages** is the same WAKE power with no cost, and declares a
  `PowerUseRestriction` with a `Waiver` on its own use: refused for a user
  who does not rule the site, waivable by the player who rules it, as City
  Wall declares its restriction (batch 8, slice 10). The Wake menu shows it
  askable with "Hidden Passages: Needs its ruler's consent."; under
  bandits it is unavailable with "Hidden Passages: Bandits rule this site."
  and when the user rules it, no question is asked.

Tests: each power's question, relocation and line; Stone Portal from a
ruled site; the Wake once-per-turn limit; Hidden Passages asked and
granted, refused and remembered for the turn, used by its ruler, unusable
under bandits; Sinking Passages unpayable with no favor.

## Slice 7: Raid battle plans

Both are `BattlePlan`s on edifice faces, as Towering Rampart is, used by
the ruler of the face's site and offered in a Raid only.

- **Spiral Castle** (attacker, intact face): `wrapping` at
  `CampaignRaidRelocation` replaces the relocation question, as Keep does,
  with the defender's pawn placed at the Castle's site (or staying), and a
  `SpendSupply(defender, 2, required = false)` cut to what they have. The
  takes and the consequences stand. Line: "Sent {Red}'s pawn to {site};
  they lost 2 Supply.", covering the move so the Campaign's closing line
  does not say it too.
- **Spiral Dungeons** (defender, ruined face): `later` at the Campaign's
  root after the outcome, when the defender won (`PlanUse.won`): the
  attacker's pawn placed at the Dungeons' site and 2 Supply lost. A bandit
  defender applies it when the Dungeons stand at a site bandits rule, as
  Battle Axes is applied, in a Conquest or a Raid. Line: "Sent {Blue}'s
  pawn to {site}; they lost 2 Supply."

Tests: each plan offered to its side only and in its Campaigns; Spiral
Castle after a Raid victory with and without Keep (rulings); Spiral
Dungeons for a player defender and a bandit defender, and not when the
attacker won; a replayed Campaign reaching the same state; the Campaign's
closing line.

## Slice 8: Site-card actions

- **Forest Horn** is a `PaidAction` (2 secrets) asking "Forest Horn:
  discard non-Beast denizens in which region?" over three `Button`
  options, then discarding every faceup non-Beast denizen and ruined
  non-Beast edifice at that region's sites with `RegionDiscard`, widened to
  take the region. Intact edifices are locked and stay; a card the Hall of
  Ministers protects is refused as usual. Line: Dazzle's shape, "Discarded
  {cards}." or "No card was discarded.".
- **Giant's Rampage** is a `PaidAction` (1 favor, 2 secrets) asking which
  denizen to bury among the faceup denizens at every site, the Rampage's
  own included, as `Denizen` options; the bury is `Bury.standard`, which
  ignores locks and returns the card's tokens. Then `SiteSlotMoves`, with a
  `CardId` mover, moves the Rampage into the freed slot at that site (no
  move when the denizen was at its own site), and the user's pawn is
  placed there. With no denizen anywhere the question shows Continue and
  the cost stays paid. Lines: "Buried {card} at {site}." then "Moved
  Giant's Rampage to {site}; {Blue} placed there."

Tests: Forest Horn across the three regions, Beast cards kept, intact
edifice kept, refused discard noted; Giant's Rampage at another site and
at its own, a locked denizen buried, the pawn placed, no denizen.

## Slice 9: Card-draw actions

- **Temple of Ancestors** is a `PaidAction` (1 favor): a `Draw` of the
  world deck's bottom card into the user's temporary hand, `AdvanceVisions
  Drawn` when it is a Vision (as Search and Oracle do), then
  `CardPlayProcedure.unchecked` from the temporary hand with a
  `PlacementLimit` allowing adviser placements only, so the slots offered
  are the advisers faceup or facedown and the discard pile, never a site.
  A faceup adviser play fires its WHEN PLAYED as a Search's does. Line:
  Oracle's shape, "{Blue} drew {card} from the bottom of the world deck."
  or "The world deck was empty."
- **Squalid District** is a `PaidAction` (1 favor, 1 burnt favor) asking a
  `ChooseMany` over the user's facedown adviser slots (`AdviserSlot`
  options, as Dream Thief names them), discarding each to the next
  region's pile (a Vision as `Discard.Vision`), then drawing that many
  denizens from the world deck's bottom into the user's play area facedown
  (`Move` with `resultingOrientation = FaceDown`, as Horned Mask places).
  Lines: "Discarded 2 facedown advisers and drew 2 denizens facedown."

Tests: Temple's play faceup, facedown and discarded; a Vision drawn
advances the track; the site slots absent with the limit's reason; an
empty deck. Squalid District with zero, one and two advisers; a Vision
among them; a deck short of denizens; Visions at the bottom (rulings).

## Slice 10: Relic swap

**Underground Library** is a `PaidAction` (1 favor). It peeks at every
relic at every site of the intact face's region (`Peek` each, as
`SiteRelicTake` records), asks which site relic to take over
`SiteRelicSlot` options, then which of the user's relics to give over
`RelicSlot` options, flips the given relic facedown if it is faceup, and
swaps the two with one `Swap` between the site and the play area. The taken
relic stays facedown, as the card says. With no relic in the region, or no
relic held, the question shows Continue and the cost stays paid. Lines:
"{Blue} peeked at the relics in the Provinces: {relics}." then "Swapped
{given} for {taken} at {site}."

Tests: the peek recorded for the user only, the two questions, the swap
with a faceup and a facedown held relic, Ancient Bloodline refusing the
site relic (askable of its holder), no relics, replay.

## Plan-time rulings

Each slice settles its lines and reasons at plan time and records them in
the rulings appendix. The rulings below are taken now; the questions are
listed for the owner with the recommended default, which the plan follows
unless told otherwise.

- **Enemies under bandits.** "Enemies of this card's ruler" when bandits
  rule the card's site: every player is an enemy and nobody may consent
  (batch 8, slice 10, and the Tome Guardians FAQ: "the bandits consider all
  players enemies"; rules 15.7 Consent: bandits never consent). Applies to
  Bandit Patrols and Forbidden Forest. Relic Patrols under bandits protects
  nothing, since bandits rule no site "ruled by this card's ruler" other
  than their own sites: see the question below.
- **"Target" is a Campaign's target selection** (batch 3). Hall of Debate
  and Tome Guardians leave a Challenge alone.
- **A must on everyone.** Hallowed Spring and Still Boiling Lake bind the
  face's ruler too, as persistent rules do (rules 9.6).
- **Setup banner moves are out** (see corrections).

### Questions for the owner

1. **Relic Patrols under bandits.** Rules 16 defines bandits as an enemy
   and the FAQ makes bandits treat every player as an enemy. Should a
   bandits-ruled Relic Patrols forbid every player to recover at every
   bandit site? Default: no. "Sites ruled by this card's ruler" with a
   bandit ruler would turn the intact face into Bandit Patrols; the ruined
   face exists for that. Bandit Patrols under bandits does bind every
   player.
2. **Hall of Debate and Tome Guardians at a Challenge.** The Tome Guardians
   FAQ speaks of "recovering" the Darkest Secret, older wording for the
   Challenge. Default: keep batch 3's ruling, Campaign targets only, and
   record the FAQ as read that way; the Challenge keeps its own gate.
3. **Keep and Spiral Castle together.** Both replace the Raid's relocation
   when the defender rules Keep's site and the attacker uses Spiral Castle.
   Default: the defender's Keep wins ("cannot ... end the action" is a
   defender's rule the attacker's plan does not override), and Spiral
   Castle's Supply loss still applies. Rules 15.4: a "cannot" overrides.
4. **Spiral Dungeons in a Conquest.** The text says "if you're victorious",
   not "in a raid", and the attacker's pawn is at the targeted site.
   Default: it applies in a Conquest and a Raid; the attacker's pawn moves
   to the Dungeons' site even from the site it conquered.
5. **Squalid District and Visions at the deck's bottom.** "Draw that many
   denizens" skips a Vision at the bottom? Default: yes, skip Visions and
   leave them in place, and advance the Visions Drawn track for none, since
   none is drawn. Draw fewer when the deck runs out of denizens.
6. **Temple of Ancestors drawing a Vision.** Default: a Vision is drawn like
   a Search's, advances the track, and may be revealed faceup (subject to
   Sacred Ground and Vow of Obedience) or discarded; "to your advisers"
   includes the revealed Vision slot, as a Search's faceup Vision play is
   an adviser play.
7. **Hidden Passages' consent under bandits.** Default: unusable, shown
   with "Bandits rule this site." (rules 15.7: bandits never consent).
8. **Forest Horn's region question.** Three `Button` options in a panel
   ("Cradle", "Provinces", "Hinterland"), or the discard-pile board
   options standing in for regions? Default: buttons; a pile is not a
   region.
9. **Boiling Lake's line.** "Killed 2 {Blue} warbands." in Tyrant's
   wording, with no site since the board is the source. Default: yes.
10. **Underground Library's held relic may be facedown.** Default: yes,
    any relic the user holds, faceup or facedown; a faceup one is flipped
    facedown first so a site holds facedown relics only (rules 7.7.3).

## Rulings made while implementing

Settled while implementing slices 1 to 3, with the owner's defaults for
the questions above.

- **Slice 1.** Broken Horn buries what Setup's own reveals discarded to
  its region's pile too, since those come before Setup's end. Its empty
  line can only show in a game whose Setup discards nothing to that
  region. Ruined School writes no line when no site is ruled by bandits.
  The banner moves of Hall of Mockery and Lost Guardians are not made
  (see the corrections), and neither line mentions the shared bank.
- **Slice 2.** The hidden banner's reason is "{Blue}'s People's Favor
  cannot be targeted.", naming the ruler, in the shape of Forgotten
  Vault's; no line is written (question 2's default).
- **Slice 3: consent is deferred.** Relic Patrols and Bandit Patrols refuse
  the Recover at its start with a `Restriction` at
  `RecoverActionEligibility` and no `Waiver`. The Act menu shows Recover
  unavailable with "Relic Patrols: Cannot recover relics at its ruler's
  sites." or "Bandit Patrols: Cannot recover relics at bandit sites.", not
  askable. The design's askable refusal needs a seam the engine does not
  have: the Act menu asks consent only for phase powers
  (`PhasePowerProcedure.refusals`) and for a queued start with a queued
  answer (`QueuedStart.consent`, Palanquin's Travel); a tree-level
  restriction is waived only through a grant keyed on an answered
  decision of the walk (`WalkerPowerGather.waived`), and a Recover has
  none before the refusal. Its modifier decision is skipped when no
  modifier is on offer, so an `OptionRestriction` on "Start Recover"
  would not reach the common case either. A waiver that no grant could
  apply was left out rather than listed in the consent grid. The seam,
  an askable Act action at the phase menu, joins the Out of scope list
  and would serve Forbidden Forest's start refusal too.
- **Slice 3.** Relic Patrols under bandits protects under bandits, just
  like Bandit Patrols (owner's ruling, overriding question 1's default):
  the bandits are its ruler, every player is their enemy, and every player
  is refused at every bandit site. Bandit Patrols under bandits refuses
  the same. Both faces are inert on the other face's reach under a player
  ruler. The rule reads the Recover's site as `RecoverProcedure.actorSite`
  reads it, the pawn site.
- **Slice 4: Forbidden Forest is askable after all.** Its refusal is not
  a start refusal: it reads the route, so the destination decision carries
  it, as the Narrow Pass's does. The destination is hidden with "Forbidden
  Forest: Cannot travel here without a Beast adviser or ruler's consent."
  and askable of the player ruler; a grant is keyed on the destination
  answer, and under bandits the plain reason shows. The Patrols' deferral
  stands, since a Recover has no such decision.
- **Slice 4.** Boiling Lake's line is "Killed 2 {Blue} warbands." (question
  9's default), with no site. It kills nothing and writes nothing from an
  empty board, kills one from a board of one, and follows the pawn that
  moves, so Palanquin's sent traveller suffers it.
- **Slice 5: Still Boiling Lake is a printed rule.** Card play judges its
  slots with the catalog's printed operation restrictions
  (`OperationRestrictions.forCatalog`), not with the walker powers'
  `operationRestrictions`, so a power-declared ban would have hidden the
  slot silently where the look-ahead refused it. Still Boiling Lake
  therefore joins Ancient Bloodline and the Hall of Ministers in
  `OperationRestrictions.printedRules`, in `gameplay/operations`, with its
  card object beside it (its intact face, Boiling Lake, stays a Travel
  power). Card play's reason map (`CardSlots`) gains its code, so the
  site's slots read "Still Boiling Lake: Cards cannot be played to this
  site." Two lines outside `gameplay/powers/`, both in the lists the Hall
  of Ministers already sits in.
- **Slice 5.** Hallowed Spring rewrites only a payment of exactly one
  placed and one burnt favor onto a card at its site, the Trade for
  secrets' printed cost; a Trade for favor, and a Trade with a card
  elsewhere, are untouched. No line: the payment's generic line shows the
  2 favor placed.
- **Slice 6.** Stone Portal from a ruled site asks nothing and places the
  pawn at the Portal; from the Portal it asks over every other site. Hidden
  Passages' consent is a `PowerUseRestriction` the power declares on its
  own use (City Wall's shape): the Wake menu shows "Hidden Passages: Needs
  its ruler's consent." askable, "Hidden Passages: Bandits rule this
  site." under bandits (question 7's default), and nothing at an unruled
  site or for the ruler; a refusal keeps the first reason. Sinking Passages
  burns its favor as the power's cost, before the question. All three
  write the movement relics' line, "{Blue} placed at {site}.", and the
  placement is a `Move`, not a Travel, so Boiling Lake and the Forest do
  not read it.
- **Slice 7.** Spiral Castle is offered in a Raid only, to the ruler of its
  site, and `wraps` the Raid's relocation as Keep does; Keep folds after
  it, so with both the pawn ends at Keep's site and the Supply loss stands
  (question 3's default). Spiral Dungeons fires at the Campaign's root
  after the outcome in a Conquest or a Raid (question 4's default), and a
  bandit defender applies it at a site bandits rule, as Battle Axes is
  applied. Both write "Sent {Red}'s pawn to {site}; they lost 2 Supply.",
  covering the move, and the loss is cut to the Supply held.
- **Slice 8.** Forest Horn asks its region as three buttons (question 8's
  default) and discards through `RegionDiscard`, which now takes the region
  to discard in and words its lines for any card; the Horn declares its
  own `used` lines ("Discarded {cards}.", "Nothing was discarded."), as a
  phase power must. Giant's Rampage needs no `SiteSlotMoves`: the buried
  denizen frees its own place, and the Rampage moves as a card moves. Its
  one line, "Buried {card} at {site}; Giant's Rampage and {Blue} now stand
  there.", covers the bury and both moves; a denizen at its own site is
  buried where it stands and nothing moves.
- **Slice 9: Squalid District draws a Vision.** Owner's ruling, overriding
  question 5's default: the District is not a Search modifier and does
  not follow Search's restrictions, so the draw takes the bottom cards
  whatever they are, with no special logic. A Vision drawn becomes a
  facedown adviser like a denizen and advances the Visions Drawn track,
  once per Vision drawn. No new seam: each card is at the deck's bottom
  when its move comes, the end the move names, so the position guard
  accepts the run (`CardMovementOperations.cardSourceOrderViolations`
  judges the batch in order). Only the deck running out draws fewer. The
  question is asked only when a facedown adviser is held (a choose-many
  cannot be passed when empty), and its line counts what was discarded
  and drawn without naming the kind, since a facedown draw is hidden:
  "Discarded 2 facedown advisers and drew 2 cards facedown."
- **Slice 9.** Temple of Ancestors limits the nested play through its own
  `Transform` at `SearchPlayAdviser`, applicable only while its walk runs
  (`ctx.procedure`), so the site slots read "Temple of Ancestors: the card
  goes to your advisers" and the adviser plays and the discard stay open.
  A Vision drawn advances the Visions Drawn track and may be revealed or
  discarded (question 6's default, confirmed as the owner's ruling; the
  suite asserts the advance).
- **Slice 10.** Underground Library peeks at every relic of its region for
  the user, asks the site relic by `SiteRelicSlot` and the given relic by
  `RelicSlot`, flips a faceup given relic facedown before the `Swap`
  (question 10's default) and writes two covering lines. With no relic in
  the region, or none held, nothing is asked and the cost stays paid.

## Seams used for the first time

Each gets a test through its first power before the power relies on it.
If a seam needs an engine change beyond the bar, stop and ask the product
owner.

- A `Bury` from a regional discard pile (Broken Horn).
- A `Restriction` at `RecoverActionEligibility` (Relic Patrols; without
  the `Waiver`, see the rulings).
- A `Kill` from a player's board inside an `AutomaticUse` (Boiling Lake).
- A printed operation restriction refusing a `Play` by destination (Still
  Boiling Lake; a printed rule, not a power's `operationRestrictions`, see
  the rulings).
- A power's own `PowerUseRestriction` with a `Waiver`, judged at the Wake
  menu (Hidden Passages).
- A battle plan `wrapping` the Raid's relocation (Spiral Castle) and a
  bandit defender's plan with a `later` effect (Spiral Dungeons).
- A ruined edifice moved by `SiteSlotMoves` (Giant's Rampage).
- A `PlacementLimit` that leaves adviser plays only (Temple of Ancestors).
- A `Swap` between a site's relics and a play area (Underground Library).

Every seam above was used and tested by its first power.

## Testing

- Each card gets a suite through the walker, from a board stated in Table
  steps, with its Game Log lines, in the family package of its template
  (`setup/`, `targeting/`, `recover/`, `travel/`, `cardplay/`, `economy/`,
  `wake/`, `action/`, `campaign/`).
- Every refused or unavailable option is tested for its hover reason, and
  every waivable refusal for a grant.
- Each face is tested inactive: the other face up, the edifice in storage,
  and where it matters the ruler changed.
- Slices that add a line test a generated first game's Game Log golden when
  the face appears in one; a face that never appears in a generated game
  (E08, see rule gaps) says so in its suite.
- Each slice tests a replayed game reaching the same state.
- The verification gate is the roadmap's standing rule.

## Out of scope

What remains after this batch, for the roadmap's engine-blocked item.

- **Live terrain** (Grand Canal, Lost Lagoon): `TravelSitePowers` fixes
  each site's terrain on its power object, and Narrow Pass reads the same
  static sets. A site's terrain must become a state read that an edifice
  can add to.
- **Round advance** (Great Clock, Broken Clock): a primitive that moves
  `tracks.round` one space without ending the round, applied by replay and
  recorded by the codec, which also decides what it does to the Usurper
  Limiter at round three.
- **Round die** (Shattered Portal): a third `DiceKind`.
- **Recover source** (Drowned Library): the relic source and the difficulty
  are start facts of `RecoverProcedure.recovery`; a modifier must be able
  to set both, as Trade's as-if site is set.
- **Discard replacement with a question** (Wild Pastures): a power that
  replaces another power's discard with a payment and asks. `takesBurn`
  replaces burns silently and `ExecutedTrigger` runs after the fact.
- **Own-power lock exemption** (Giant's Path): `LockedCards` refuses every
  move of an intact edifice. A card's own power moving it needs an
  exemption the move can carry.
- **Dispossessed shuffles** (Great Spire, Fallen Spire, Pilgrimage): a
  random draw from the Dispossessed. `Shuffle` takes a `SearchSource`, and
  the Dispossessed is private to everyone (rules 15.6).
- **Vision collection** (Desecrated Ground): `PlayerState.revealedVision`
  is one card.
- **As-if ruler** (Bandit Crown): `PowerAccess.ruledSites`, `RuledCards`
  and `PlanContext.rules` read `SiteRule.ruler`. The FAQ adds the refill
  and the last-warband minor action.
- **Favor source redirection** (Vow of Kinship): every cost, take, burn
  and resource read is against the board.
- **An askable Act action** (the Patrols' consent): the Act menu asks
  consent for phase powers and queued starts only, and a tree-level
  `Restriction` is waived only through a grant keyed on an answered
  decision, which a refusal at an action's start has none of. Forbidden
  Forest does not need it: its refusal rides the destination decision.
- **Challenge** (School of Vines, Sigil of the Eye, Sigil of the Heart,
  Favored Son, Magician's Code): unchanged from batch 8.
- **Unchanged from batch 8:** Search Party, Bracken, Cracked Horn, Ring of
  Devotion, Master of Disguise, Acting Troupe, Relic Hunter, Glamor, Autumn
  Wind, Tracker, Obsidian Cage, False Prophet.
- **Negotiation terms:** The Tribunal, Bandit Tribunal, Festival District,
  The Gathering, Whispering Stone, Council Arbiter, Deed Writer, Traveling
  Negotiator.
- **Chronicle steps:** the 36 legacies, Ancient Writ, Stone Baths, Ruined
  Baths, Amber Doors, Lost Doors, Blasted Pastures, Ruined Temple, Hall of
  Bandits, Hiding Place, Reliquary Raid (the Reliquary is empty until the
  Chronicle stocks it), and the later-game banner moves of Hall of Mockery
  and Lost Guardians.
- **Empire:** Grand Mask, Imperial Seal, the Grand Scepter's three powers,
  Bandit Forest, Ancient Pact, Bewitch, Long-Lost Heir, Royal Ambitions,
  Martial Culture, Council Seat, Naysayers.
