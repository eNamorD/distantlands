# Foundations

**Status:** designed 2026-11-11. Every slice landed: 1, 2, 3a, 3b, 4, 5, 6
and 7.

**Builds on** the [Chronicle steps design](2026-10/2026-10-27-chronicle-steps-design.md),
the [Chronicle setup design](2026-09/2026-09-21-chronicle-setup-design.md) and
[catalog batch 11](2026-11/2026-11-07-catalog-batch-11-design.md), whose setup,
Empire and consent rules apply unchanged.

## Goal and scope

New Foundations sets up a later game from six foundation setup cards, each
with a normal and an altered face (NF rulebook, "Foundations"). Game start
today forces every foundation to its normal face and ignores the faces a
Chronicle stores. It also refuses a later game with no Chancellor and no
Citizen seated, which needs Foundation I's altered face, Wide Horizons, and
its Explore action.

Before Legacies exist, a face is altered in two ways: an imported Chronicle
(the TTS string's foundations mask) stores it, or Setup With No Empire erodes
Foundation I to Wide Horizons. This batch makes every face but Foundation
III's work, and lets the Beacon restore one. Legacies later only add
alteration sources; the places that read a face stay the same.

| Slice | Seam | Faces |
|---|---|---|
| 1 | Faces reach the game | all six, read and shown |
| 2 | Setup-only faces | II Movers and Shakers, IV Teeming World |
| 3 | Empty map slots, Explore, no-Empire setup | I Wide Horizons |
| 4 | The Chancellor's site choice | I Imperial Maps |
| 5 | A banner face that opens play sites | V Grand Council |
| 6 | A banner face with a marker | VI Festival |
| 7 | Restoring a face in the Chronicle | Beacon's Shape Foundation |

Slices are ordered so that each seam exists before the slice that reuses
it. Slice 4 reuses slice 3's empty slots and its `PlaceAtlasSite`
operation. Slice 7 reads the faces slice 1 brings into the game.

Foundation III deals legacy cards on both faces (Quiet Ambitions: each
player draws 2 and keeps 1; Public Ambitions: a public row of 4). It waits
for the Legacies item of **Phase - Chronicle steps**. Game start accepts
either face of III and skips its setup, as it skips Quiet Ambitions today.

The roadmap's **Foundations** item leaves **Phase - Chronicle steps** and
becomes **Phase - Foundations**. Its **Grand Council and Festival banner
faces** item folds into slices 5 and 6.

## Rulings

Taken with the owner on 2026-11-10 and 2026-11-11.

1. **Foundation III waits for Legacies.** Both faces are accepted at game
   start and do nothing. The Beacon may still restore an altered III
   (slice 7).
2. **Explore borrows only Recover's dice.** No power that names Recover or
   recovering applies to Explore: Silver Charm, Relic Patrols and Drowned
   Library stay out, though Silver Charm's text names only spending Supply to
   roll defense dice. Jinx ("after you roll [attack-die] or [defense-die] for
   any reason") applies, through the generic `Reroll` window.
3. **No Empire sets the Reliquary aside.** "Discard all relics from the
   Reliquary. They return in the Chronicle Phase." The Reliquary's relics
   stay `campaign.reliquary` all game and never enter play; the slot fill
   cannot draw them, and the Chronicle's Stock Reliquary step adds to them.
4. **Explore's line** is "Explored Forgotten Site: Found Deep Woods" (or
   "Recent Site").
5. **The shared bank has no count.** Taking favor from it changes nothing
   but the taker's board.

Design calls the owner accepted with the slices:

- Any role may Explore.
- Festival's marker is placed automatically each Rest, with no question,
  since the face says "must".
- With no Empire, the Oathkeeper title and both banners start as in a first
  game.

## Approach

Faces live in `CampaignState.foundations`, and each step reads them where
that step already lives. Game start resolves the faces (the Chronicle's,
then erosion). The materializer branches per face for II, IV, V and VI.
The setup walker gains only the one choice a face needs, Imperial Maps',
and the fill that follows it. In play, Explore is offered while Foundation I is
altered, and V and VI set the banners' faces, which `BannersState` already
models.

Two alternatives were set aside. Making each face a contributing power at
setup windows, like the edifice setup powers, would move the directly built
II and IV setup into walked operations for no rules gain. Plain `if altered`
branches with no catalog leave the UI and the Game Log nothing to name.

## Slice 1: faces reach the game

- **Game start.** `GameStartRules.evolve` copies `chronicle.foundations`
  into `CampaignState.foundations`, a missing number as
  `FoundationState(Normal, Set.empty)`, sources kept. An altered I, II, IV,
  V or VI is refused with `UnsupportedChronicle`, naming the face and the
  slice it arrives with; each later slice deletes its own refusal. III is
  accepted on either face.
- **Chronicle record.** `ChronicleRecord` writes `campaign.foundations` into
  the evolved Chronicle instead of keeping the baseline's, so erosion
  (slice 3) and restores (slice 7) carry into the next game.
- **The minor-action gate.** `MinorActionProcedures.started` stops refusing
  with "minor actions are limited to the fixed unaltered foundations".
  `FirstGameFoundationProfile` goes too if nothing else reads it.
- **Catalog.** `FoundationCards` lists I to VI, each face with its printed
  name and its text as the setup card prints it:

  | | Normal | Altered |
  |---|---|---|
  | I | Imperial Maps | Wide Horizons |
  | II | Powerful Tribes | Movers and Shakers |
  | III | Quiet Ambitions | Public Ambitions |
  | IV | Ties of Fellowship | Teeming World |
  | V | Mob's Favor | Grand Council |
  | VI | Wandering Flame | Festival |

  `RuleSourceIndex` replaces the generic `foundation.altered` id with one
  id per face, normal faces included (`foundation.i.wide-horizons`), so
  `LogWords.power` names the face a line comes from. `PrintedCards` names
  only cards, and a foundation is none.
- **Projection and UI.** The game projection gains `foundations`: each
  number, its face, the face's name and its rules text. The shared bank, the
  table's game info column, shows a strip of six setup cards with the altered
  faces marked; a click reads a face's rules. The Chronicle page lists the
  same six from the Chronicle's summary. Both are read-only; editing waits
  for **Edit Chronicle**.
- **Tests.** Game start keeps stored faces and refuses the gated ones;
  minor actions run under an altered III; the projection and its codec round
  trip; the TTS mask still imports to faces; a frontend suite for the strip;
  the Chronicle record keeps a changed face.

## Slice 2: Movers and Shakers and Teeming World

Both are setup only and built directly by the materializer; nothing is
journaled. Each removes its refusal.

- **Movers and Shakers (II).** Each player takes 5 more favor from the
  shared bank. Each suit's bank then gets 1 favor per denizen of that suit on
  the map, edifices included, with no base 3 and no fifth-player favor, since
  the face prints neither.
- **Teeming World (IV).** Each player draws facedown denizens from the
  starting stack until they hold three facedown starting advisers, so a
  lineage with one stored adviser draws two. They keep them all: no hand,
  no Choose Adviser step. The rest of the starting stack is dealt one card at
  a time to the Cradle, Provinces and Hinterland discard piles in turn, with
  no seeding of 2 per pile. The world deck is then the five Visions alone,
  through the existing short-deck mix (fewer than 25 denizens).
  `SetupDeal` holds both faces' arithmetic for the plan factory, the deal
  validation and the materializer.
- **Readings taken with the plan,** for the owner to confirm. Under Movers
  and Shakers every board starts with 6 favor, the Chancellor's included.
  Under Teeming World a lineage that stores three or more advisers draws
  none and keeps them all. Players draw in seat order, and a lineage's
  stored advisers come before the ones it draws. A site-only denizen drawn
  is kept and cannot be revealed in Setup. Reveal Cards still runs. An
  uneven rest leaves the Cradle pile, then the Provinces pile, one card
  more, and the first card dealt to a pile is its top; later cards go
  beneath (decided in the slice's review, so that the unimplemented
  denizens the alpha shuffle puts last in the stack top no pile). A Search
  of the Visions-only deck draws one Vision and stops; once the five are
  drawn the world deck is empty and cannot be searched.
- **Tests.** Bank and board totals at 3 and 5 players; Teeming World with
  and without stored advisers; the discard piles' order; a world deck of
  Visions only, and a first Search drawing a Vision; card accounting still
  holds every card once.

## Slice 3: Wide Horizons, Explore and Setup With No Empire

Landed in two parts. Slice 3a is the empty slots, Wide Horizons at game
start, Setup With No Empire and the Chronicle. Slice 3b is Explore, which
fills an empty slot in play.

### Empty slots

The map's region vectors may be short. A slot is the next index in the
Cradle (2 slots), then the Provinces (3), then the Hinterland (3).
`MapState` gains `hintermost`, the last site in `hinterward`, and
`nextSlot: Option[Region]`, the first region not yet full. Game start builds
the map from the sites it places instead of slicing eight. The frontend
draws an empty slot as an empty frame. One task audits the other readers of
the map's shape. Slice 3a's audit found more than the materializer:
`GameStartRules` counted eight twice, `DomainValidation` required full
regions, and the projection read the region vectors alone. In play every
reader counts the sites on the map. Shattered Portal's wrap walks
`hinterward`, so it skips empty slots, and a roll that is a multiple of the
number of sites leaves the pawn where it stands. Stone Portal has no wrap:
it offers every other site, and on a map of one site it parked the walk on
a question with no option. The walker now refuses a required choose-one
that declares no option, so such an action is shown unavailable.

### Wide Horizons at game start

With a Chancellor, the Empire's sites return as today and no slot past them
is filled. No Imperial Maps step runs.

### Explore

A new major action, `explore`.

- **Offered when** Foundation I is Wide Horizons, the actor's pawn is at the
  Hintermost site, a slot is empty, and the Atlas holds a site. Any role may
  take it. Under Wide Horizons the Act menu otherwise shows it disabled
  with its reason. Foundation I cannot change during a game, so under
  Imperial Maps no one can ever take it and the menu leaves it out, as it
  leaves out an option the player's role can never use (decided in the
  slice 3b review).
- **Tree.** It mirrors Recover's: the modifier choice any major action has
  (no Recover modifier is offered), then pool `explore` with 2 defense dice
  per 1 Supply against difficulty 4 and the continue-or-stop choice after a
  short roll. Its own windows are `ExploreModifierSelection`, which the
  modifier choice needs and where no card offers anything, `ExploreCost`
  and `ExploreRoll`; the generic `Reroll` window still runs (ruling 2).
  Explore is a `MajorActionType` of its own, so a modifier of another
  action is never offered for it.
- **Success.** The explorer chooses Recent or Forgotten (`explore.end`),
  unasked when the Atlas holds one site. `PlaceAtlasSite(end, region)`, a
  journaled operation, takes that end's site (the Atlas head for Recent, its
  tail for Forgotten) with its stored cards and puts it in the next slot:
  its edifice ruined, its denizens faceup, its stored relics facedown,
  bandits up to its capacity, its printed favor or secrets, and its open
  relic slots dealt from the relic deck while it lasts. An operation is
  applied with no catalog, so `PlaceAtlasSite` also carries the site's
  printed capacity, relic slots and resources, which `AtlasPlacement`
  copies from the catalog; `SiteState.banditSite` populates the site, for
  game start too. The card index now holds a stored site's edifice:
  placing one it did not hold would have changed the card inventory, which
  the operation pipeline refuses. The explorer's pawn goes there; this is
  not a Travel. Then the `WhenExplored` window runs on a node that names the
  explored site, `ExploredSite`. Every setup rule with a WHEN EXPLORED half
  already named the window, but applied wherever its face stood on the
  map; each now resolves at an Explore only for a face at the explored
  site (`EdificeSetupSupport.resolvesAt`). An explored edifice is placed
  ruined, so only ruined faces resolve: Hall of Mockery, Bandit Market,
  Broken Forge, Fallen Spire, Broken Clock, Lost Guardians, Empty Grounds,
  Ruined School and Broken Horn. The placement shows the site and what it
  stores, so it is a hidden outcome (`HiddenOutcomes`): undo never takes it
  back, nor the choice of end before it.
- **Stop.** "Failed to explore"; the Supply stays spent.
- **Log.** "Explored Forgotten Site: Found Deep Woods" (ruling 4); the new
  site's own power lines follow. A second Supply writes "Continued
  exploring", as Recover writes "Continued recovering". The lines sit on
  the Game Log's Neutral band.
- **Readings taken with the plan,** for the owner to confirm. Only a power
  that hooks one of Explore's own windows changes an Explore, and none
  does: no modifier is offered, and no power changes its Supply cost. What
  holds for every operation still holds, so Brass Army asks its holder as
  their pawn is placed. The next slot is the first empty slot going
  Hinterward, so when the Hintermost site's region is full the site goes
  to the top slot of the next region. Who rules the Hintermost site does
  not matter. The explorer chooses the end after the roll succeeds, at
  each Explore, without seeing either site, and a later Explore may take
  the other end; one site left is not asked about and is logged as a
  Recent Site. The site is populated as Foundation I prints: a relic deck
  that runs out leaves slots open, and a bandit bank that runs out leaves
  the site short. Its denizens add no favor to the suit banks, since
  Foundation II's count is a setup step. WHEN EXPLORED resolves after the
  pawn is placed, so "if you place your pawn here" holds for the explorer,
  and no ruler is asked to consent, as at Setup. Broken Clock advances the
  round marker at once and does nothing on round 8. An Explore, stopped or
  not, is an action: the action boundary follows it. Lost Doors and Ruined
  Temple wait for Legacies.
- **The banner move, decided with the owner.** Hall of Mockery and Lost
  Guardians "move it to the shared bank": a held banner goes back to the
  shared bank, when explored and at Setup alike. This changes a later
  game's Setup: a Chancellor who starts with the People's Favor or the
  Darkest Secret loses it at Setup's end when the matching ruined edifice
  is on the map, as the card says. The first game, where both banners
  start in the shared bank, does not change.

### Setup With No Empire

When the Chronicle has a Chancellor but neither the Chancellor's lineage nor
any Citizen is seated, game start no longer refuses (`EmpireSetupRules`).
The same holds for a Chronicle that lists no Chancellor: a seated Citizen
becomes the Chancellor, and with none seated the game has no Empire. Only a
Chronicle with no Chancellor, no Citizen and no Empire site starts a first
game.

- **Foundations erode.** I becomes Wide Horizons and V becomes Mob's Favor
  in `campaign.foundations`, with no sources. The Chronicle record carries
  them forward (slice 1).
- **Sites.** The former Empire's sites (`chronicle.world`) fill slots going
  Hinterward: edifices ruined, denizens faceup, relics facedown, bandits up
  to capacity, and every empty relic slot filled from the relic deck. No
  other site is placed.
- **Relics.** The Grand Scepter is set aside, as in a first game. The
  Reliquary's relics stay set aside (ruling 3).
- **Roles.** Every Citizen in the box becomes an Exile (`boxRoles`).
- **Table.** The plan's random first player begins; the Usurper Limiter is
  on; the Oathkeeper title and both banners start as in a first game.
- **Pawns.** An Exile may place their pawn at any site ruled by bandits,
  which here is every site that holds bandits: a site of capacity 0 holds
  no warband and is ruled by nobody (R25).
- **Readings taken with the plan,** for the owner to confirm. The absent
  Chancellor's lineage becomes an Exile in the box with the Citizens. A
  former Empire with no site is refused, as an Empire's is (R28). A
  Foundation I already on Wide Horizons keeps its sources. Relic slots are
  filled Cradlemost first while the relic deck lasts, and the rest stay
  empty. If no site holds bandits, an Exile may place their pawn at any
  site. A power that takes a Reliquary relic finds a set-aside Reliquary
  empty. A first game places its eight sites whatever Foundation I's face.
  Powers that make a player a Citizen are ignored as in a later game with
  an Empire: the rulebook sets them aside only in the very first game, but
  game start leaves that option on in every game until the Settings
  Feature phase, a known gap.
- **The reading most in need of the owner's eye: the set-aside Reliquary
  at Crown Chancellor.** Setup With No Empire says "Discard all relics
  from the Reliquary. They return in the Chronicle Phase", and the Sun's
  Crown Chancellor says of an Exile's win "Discard all relics in the
  Reliquary". Slice 3a leaves Crown Chancellor as it is: an Exile's win
  discards the Reliquary, the set-aside relics with it, and Scatter Relics
  deals them; a Chancellor's win keeps them, and Stock Reliquary adds to
  them. With no Empire the winner is an Exile unless a player became the
  Chancellor in play, so this narrows ruling 3's clause "the Chronicle's
  Stock Reliquary step adds to them" to that one case. The alternative is
  for Crown Chancellor to skip the discard while the Reliquary is set
  aside (`CampaignState.reliquarySetAside`), so that the relics always
  wait for Stock Reliquary.

### Chronicle

Stars and extraction already walk `hinterward`. Tests cover a map with empty
slots; the unexplored Atlas sites stay in the Atlas, in order.

### Tests

Game start under Wide Horizons with and without an Empire; each of
Explore's gates; rolls with a Jinx reroll, with Silver Charm not offered,
and with a stop; each end, and the one-site Atlas; When Explored powers
firing on the new site; a full map refusing Explore; the no-Empire state;
an Explore golden log; a Chronicle round trip from a map with empty slots.

## Slice 4: Imperial Maps' choice

Closes the deferral in ruling R27 (`docs/rules/ambiguities.md`), which
always took the Recent end.

- **When.** With a Chancellor and Foundation I normal. Game start leaves
  the slots past the Empire's sites empty.
- **Question.** The first setup question after the Reliquary pick is the
  Chancellor's `setup.imperial-maps`: fill from Recent or Forgotten Sites.
- **Fill.** One `PlaceAtlasSite` per empty slot going Hinterward (slice 3),
  each also giving 1 favor from the shared bank to the matching suit's bank
  per denizen it places, because Foundation II follows Foundation I. The
  Forgotten end takes the Atlas's tail first.
- **After.** Pawn placement follows and offers the new sites.
- **Log.** "p1 filled the map from Forgotten Sites", then "p1 placed Golden
  Valley on the map" per site. Setup has no turn and every Setup line names
  its player, so these name the Chancellor.
- **Tests.** Each end; bank totals; the relic deal; pawn options; replay
  equals live.
- **The question, decided with the owner.** The Recent option names the
  Atlas's first site, which the rulebook makes public: "Recent Sites
  (Buried Giant first)". The Forgotten end's site is private and is not
  named, and nothing else about the Atlas is shown before the fill.
- **As built.** Game start places only the Empire's sites, so the fill's
  relic deal and its denizens' favor move from the materializer into the
  walk; a Recent fill gives the same sites, relics and bank totals as the
  old game start. The step (`ImperialMaps`) is declared from what game
  start fixed, so a resumed walk finds its nodes; the pawn question reads
  its sites when it is asked. That question now sits inside a `Branch` in
  every Setup, a first game's included, so each pawn node lies one level
  deeper than before (a first game's moved from `0.0` to `0.0.0`) and a
  journal parked mid-Setup before this slice resumes at a different path;
  old journals need not replay. The favor is a `Move` from the shared bank
  beside each `PlaceAtlasSite`, so an Explore still adds none. The fill is
  an undo barrier, as an Explore is, so the Reliquary pick before it can no
  longer be undone once the map is filled, and with one site in the Atlas,
  which asks nothing, never.
- **Readings taken with the plan,** for the owner to confirm. An Atlas too
  short to fill every slot is refused at game start, as before. A Citizen
  standing in for an absent Chancellor answers. A relic deck that runs
  short leaves the last relic slots open, so game start no longer refuses
  such a game. Foundation II's favor counts a placed site's ruined edifice, as
  "denizens" covers edifices, and is added as each site is placed, which
  gives the same totals as counting after the fill. One answer fills every
  slot, the Forgotten end placing the Atlas's last site in the Cradlemost
  empty slot. With one site in the Atlas nothing is asked and the line says
  Recent; an Empire of eight sites asks nothing. A placed site's SETUP half
  resolves at Setup's end, as before.

## Slice 5: Grand Council

- **Setup.** An altered V sets People's Favor to `GrandCouncil`. Its
  refusal goes.
- **Power.** `PeoplesFavorGrandCouncil`, the holder's persistent rule at
  `SearchPlayAdviser` beside Mob's: `openSites` adds every Imperial site as
  a place to play a card, and a site discard is permitted only when the
  card goes to an Imperial site. A ruined edifice may be discarded; an
  intact edifice or a locked card may not (the generic discard rules). The
  Mob's discard at other sites goes with the Mob face.
- **Challenge.** `ChallengeProcedure.supportedFaces` stops refusing the
  face; it still refuses the Festival. The face's ribbons, read from
  `reference/2026-03-17-new-foundations-rev1/banners.pdf`, print the same
  on the left (Challenge) and the right (Campaign): "HALF" over the
  return-favor icon, then "REST" over the bank-favor icon. Half the favor,
  rounded down, goes back to the banner's previous holder, and the rest
  to the least-favor banks one at a time. The Mob prints "ALL" over the
  bank-favor icon on both ribbons.
- **Tests.** Playing to a distant Imperial site; a discard there and none
  elsewhere; no Imperial sites in a no-Empire game; a Challenge of the face.
- **As built.** Game start reads Foundation V from the faces it resolved
  (`FirstGameSetupMaterializer`), so Setup With No Empire, which erodes V
  first, always starts the Mob. `PeoplesFavorGrandCouncil` sits beside
  `PeoplesFavorMob` in `BannerFacePowers` and uses Baron's seam: one
  `PlaySites` and one `DiscardPermission`, both holding at Imperial sites
  (`SiteRulers.isImperialSite`). Its discard writes the Mob's line,
  credited to the Grand Council. The ribbon is
  `BannerRules.peoplesFavorRibbon`, which reads the face, so a Challenge,
  Favored Son ("its left ribbon"), a Raid and the Conspiracy ("its right
  ribbon") all follow it, and a Raid's defender then burns half their
  favor, the returned half included. The frontend already drew and read
  both faces.
- **Readings taken with the plan,** for the owner to confirm. Every card
  play the engine plans is "playing a card": a Search's, the facedown
  adviser play's and a power's own, as for the Mob. A card goes to a
  distant Imperial site only as it could go to the pawn site: a Vision or
  an adviser-only card never, a site-only card yes. A full Imperial site
  takes the card only in place of one discarded there. The discard is
  optional, one card per play, stacks with the Homeland's, and holds at
  the pawn site only when it is Imperial. Nobody is asked to consent, and
  printed protections and locks still apply. An Imperial site is one the
  Empire's warbands hold when the play is planned; the Grand Mask changes
  who rules its cards, not the site. The holder's role does not matter.
  An unheld Grand Council has no previous holder, so all its favor goes to
  the banks. A first game with an altered V starts the Grand Council in
  the shared bank.

## Slice 6: Festival

- **Setup.** An altered VI sets Darkest Secret to `Festival`. The Wandering
  Flame's move and place actions go with that face; the action that places
  secrets on the banner stays. Its refusal goes, the last one: game start
  now refuses no altered face.
- **Marker.** `DarkestSecretState.festival: Option[SiteId]`, projected and
  drawn on the map.
- **Rest.** At `RestStart` the holder places the marker at their pawn's
  site, automatically: "Held the festival at Deep Woods".
- **Persistent power.** At the marked site, the holder's enemies cannot
  place favor or secrets on denizens and cannot use those denizens' powers
  unless they are persistent. Peeking at and recovering relics are untouched.
  It is a power-use restriction of the holder with consent: the holder is
  the granter, and bandits always enforce. Each refused option shows
  disabled with its reason.
- **Removal.** Both of the face's ribbons, read from
  `reference/2026-03-17-new-foundations-rev1/banners.pdf`, end with the
  remove-festival icon. The left (Challenge) prints "ONE" over the burn
  icon, then "REST" over the return-secrets icon: one secret burns, and the
  rest go to the previous holder, or burn when the banner was in the shared
  bank. The right (Campaign) prints "HALF" over the burn icon: half the
  secrets, rounded down, burn, and the rest stay on the banner. The
  Wandering Flame prints "HALF" over the scatter icon and "REST" over the
  return-secrets icon on the left, and "ALL" over the burn icon on the
  right.
- **Tests.** Placement each Rest and moving with the pawn; each blocked use;
  persistent powers still working; consent waiving it; a Challenge clearing
  it; a golden log.
- **As built.** Game start reads Foundation VI from the faces it resolved
  (`FirstGameSetupMaterializer`), so the marker starts beside the map, and
  `StartingFoundations` has no refusal left. Every way into Rest now
  folds the `RestStart` window once, declared since the Rest walker and
  folded by no procedure until now: `RestEntry` is the phase change, its
  lines and the window, which Begin Rest, Wizard School, Murky Fountain,
  Tracker and Special Envoy take, and `Citizenship` folds the window after
  a role change that ends the player's own Act. `DarkestSecretFestival`, beside the Grand Council in
  `BannerFacePowers`, is both the Rest contribution and the power-use
  restriction, in City Wall's shape: one `PowerUseRestriction` and one
  operation restriction on placements, its waiver granted by the holder.
  The marker moves by a journaled `SetFestivalMarker`. Challenge walks the
  left ribbon (`ChallengeRibbon`), and the marker's removal after the ribbon
  window, so Secret Testament, which replaces the ribbon's resources there,
  leaves it; a Raid and the Conspiracy walk the right ribbon
  (`BannerRules.darkestSecretRibbon`). `ChallengeProcedure.supportedFaces`
  and `OathViolation.UnsupportedBannerState` are gone. The projection gives
  each site a required `festival` flag, and the World pane ends that site's
  pawn row with a Festival pill.
- **Readings taken with the plan,** for the owner to confirm. Every other
  player is the holder's enemy, whatever their role. "Their powers" are the
  powers of the denizens at the marked site, edifices included, not the
  enemies' own powers used there; relics and the site's own powers are
  untouched, and a denizen's action modifier is one of its powers.
  "Persistent" is the power as printed. Placing favor or secrets on a
  denizen is what City Wall's ban reads: a cost paid onto it on the payer's
  turn, a gift onto it, or a forced placement from the placer's board, and
  a persistent power whose cost places on it. Consent is asked outside a
  Campaign only. The marker starts beside the map and first goes on it at
  the holder's first Rest; each Rest places it at the pawn's site, even
  where it already lies. A holder with no pawn on the map places nothing.
  With nobody holding the banner the marker guards nothing and stays where
  it lies. A take that walks no ribbon (Wizard's Conclave, or Hall of
  Mockery and Lost Guardians moving the banner to the shared bank) leaves
  the marker, which then guards for the new holder. "ONE" on an empty banner
  burns nothing. Under Secret Testament the holder takes every secret, and
  the marker still leaves the map. A first game with an altered VI starts
  the Festival in the shared bank. An automatic power of a guarded card is
  refused for an enemy like any other non-persistent use, even one that
  harms them: an enemy arriving at the festival waits at Boiling Lake's
  step, and the Lake stays deactivated once the holder refuses. Jinx at the
  festival, though persistent, is refused to an enemy who rules it, since
  its secret would go onto the card.
- **The Conspiracy, decided with the owner.** The Conspiracy (`OATH-V1-NF`)
  says to "adjust its [favor]/[secret] as shown by its right ribbon", and
  that ribbon ends with the remove-festival icon, so a Conspiracy that takes
  the Festival removes the festival marker, as a Raid does (2026-11-12).

## Slice 7: Shape Foundation

- **Window.** `ChronicleShapeFoundation`, empty since the Chronicle steps
  design, gets the Beacon task holder's question: restore one altered face to
  normal, or keep them all.
- **Offer.** Every altered face whose `alterationSources` holds no active
  legacy. Before Legacies that is every altered face, III included. No
  altered face, no question.
- **Log.** "Restored Foundation I to Imperial Maps".
- **Tests.** Each offer; the skip; the restored face reaching
  `ChronicleUpdated`; a hand-built face with an active legacy as its source
  is not offered.
- **As built.** `ShapeFoundation` builds the step in the Beacon's last
  window. Keep every foundation is the first option, as Decline is at the
  Throne, and each altered face follows, I to VI. A face an active legacy
  alters is shown unavailable, naming the legacy, rather than left out as
  "not offered" above might read; the holder is asked whenever a face is
  altered, even when every altered face is held and only Keep may be
  chosen (decided with the owner, 2026-11-11). The restore is a
  journaled `FoundationRestored`, which turns the face normal and clears
  its alteration sources, and `ChronicleRecord` carries it into
  `ChronicleUpdated`. Each foundation has its own line, since a restore
  always reaches the one normal face, and the line names the holder, as
  every Beacon decision line does: "p3 restored Foundation I to Imperial
  Maps". Keeping writes no line.
- **Readings taken with the plan,** for the owner to confirm. An active
  legacy is one that a lineage this Chronicle records, seated or in the box,
  holds active; a source legacy that no lineage holds active does not stop
  the restore. A restore clears the face's sources. A face Setup With No
  Empire eroded has no source and may be restored. The step runs after
  Welcome a Stranger, in the printed order.

### Docs

The slice's last task, for the whole batch:

- `docs/ROADMAP.md`: **Phase - Foundations** is deleted with the batch, and
  the **Grand Council and Festival banner faces** item goes with slice 6.
- `docs/rules/chronicle.md`, `docs/rules/ambiguities.md` (rulings 1 to 5,
  the readings of slices 2 to 7, and R27 closed) and
  `docs/rules/implementation-traceability.md`.
- `CONTEXT.md`: Explore, Hintermost, empty slot, Festival marker.
- This spec's status line.

## Out of scope

Foundation III's faces, and every legacy that sets or reads a foundation
(Legacies). Editing a Chronicle's faces (Edit Chronicle). Eras.
