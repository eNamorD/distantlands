# First-game setup

Status: implemented and replay-tested, reviewed September 2026 (Chronicle
design, slice 3).

`gameplay/setup/GameStartRules.evolve` builds a fresh `Ready` game directly
from a `Chronicle` and `SetupOrders`: it begins at `OathState.NoGame`, records
one `GameStarted` event carrying both, and runs Setup as an ordinary
`TriggeredProcedureRef` walker (`gameplay/setup/SetupProcedure.scala`) from
there. There is no separate setup state machine or command payload; the
walker's tree -- in a later game the Chancellor's Reliquary pick and
Imperial Maps' fill (`gameplay/setup/ImperialMaps.scala`), then per player
`Decide` a site (inside a `Branch`, so the legal sites are read when the
question is reached), place the pawn, `Decide` an
adviser, keep/discard, a `SetupEnd` window that folds the six batch-1 edifice
powers (`gameplay/powers/setup/`: Great Market/Bandit Market, Great
Forge/Broken Forge, Proving Grounds/Empty Grounds) -- ends the same way every
other triggered procedure does, with `BeginTurn(firstPlayer, Wake)`.

## Chronicle, orders, and validation

A `Chronicle` is the between-game record a Chronicle generator produces: the
ordered atlas box (which sites are in play and their stored edifices), the
World Deck's denizen pool, the ordinary-relic order, the era scorepad, the
Oathkeeper goal, the reliquary, and the dispossessed board. `SetupOrders` is the one-time per-game deal drawn from
it -- participants, first player, the shuffled `denizenOrder` that deals hands
and regional discards, the `worldDeckOrder` below them, and the shuffled
`relicOrder` -- built by `ChronicleFirstGamePlan.dealOrder` and recorded
alongside the Chronicle on `GameStarted`, so replay never re-derives either.
Setup deals from these orders, never from the Chronicle's deck order: a
Chronicle is shown to players, so its order must not be the deal.

Foundation IV decides how `denizenOrder` is dealt, and `SetupDeal` holds that
arithmetic for the plan factory, game start's validation and the
materializer. Under Ties of Fellowship the first 6 cards seed the regional
discards, each player with no stored adviser draws a hand of 3, and the rest
goes under the Visions as the world deck. Under Teeming World each player
draws until they hold three starting advisers and keeps every card drawn
facedown, after any adviser their lineage stored, with no hand and no
adviser choice; the rest of the stack is dealt one card at a time to the
Cradle, Provinces and Hinterland discard piles, the first card dealt to a
pile its top and each later card beneath; and the world deck is the five
Visions, shuffled. Foundation II decides the favor: Powerful Tribes
gives each bank 3, 1 more with five or more players, and 1 per denizen of
its suit on the map; Movers and Shakers gives each board 6 favor and each
bank only the 1 per denizen. Both altered faces are built directly, like the
rest of game start; nothing is journaled. An altered Foundation V, Grand
Council, starts the People's Favor on its Grand Council face, as Mob's
Favor starts it on the Mob, and an altered Foundation VI, Festival, starts
the Darkest Secret on its Festival face with its marker beside the map, as
Wandering Flame starts it on the Wandering Flame. The sites Imperial Maps places in
Setup's walk add their denizens' favor as they are placed
(`AtlasPlacement.suitFavor`).

`GameStartRules.evolve` validates only what Setup itself needs from the
Chronicle: known and unique atlas/denizen/relic ids, an atlas site for every
slot the atlas fills (8 in a first game; under Imperial Maps, every slot
past the Empire's sites), enough World Deck cards to deal and, in a first
game, enough relics for every relic slot, a deal holding exactly the
Chronicle's decks. A first game's Homelands each store one matching edifice;
a Chronicle whose Atlas edifices differ carries history and starts an
exile-only game. The deeper suit-count and Vision-packet audit a Chronicle
must satisfy is the generator's own job, checked once at generation time
(`FirstGameChronicleGenerator.validate`) rather than again at every setup.

Game start keeps the whole Chronicle on `CampaignState.baseline`, including
lineages of colors that did not play, and takes the era and Oathkeeper goal
from it; a seated lineage the Chronicle has not scored starts at 0. The
Chronicle Phase records its result in `CampaignState.evolved`
([Chronicle steps design](../superpowers/specs/2026-10/2026-10-27-chronicle-steps-design.md)).
The phase now runs the Sun and Throne tasks; the recorded Chronicle carries
their roles, era scores and Oathkeeper goal, while sites, decks and the
Reliquary stay as in the baseline until the Stars task extracts them.

`EmpireSetupRules.resolve` reads how the game starts, a `SetupMode`. A
Chronicle that lists no Chancellor, no Citizen and no Empire site starts a
first game on eight atlas sites when it is brand new, carrying no history
(`EmpireSetupRules.hasHistory`: an altered or sourced foundation face, a
lineage that played before or keeps starting advisers, legacies or
starting relics, a scored era game, an Oathkeeper goal other than
Supremacy, a stocked Reliquary, a denizen or relic stored at an Atlas
site, or an Atlas edifice a new Chronicle would not store: one at a site
that is no Homeland, or a Homeland storing none or another suit's; the
check reads the catalog). With history it starts an exile-only game
(`SetupMode.ExileOnly`): the Atlas's first eight sites are placed as bandit sites with their stored
cards and open relic slots filled, whatever Foundation I's face, the
stored faces apply with no erosion, stored advisers are dealt, the
Citizenship option is off and the Reliquary is set aside, as with no
Empire. Exiles place their pawns as in a later game, and an Atlas of fewer
than eight sites is refused. With a Chancellor seated, or a seated
Citizen standing in for one, the Empire's sites return. Under Imperial Maps
Setup's walk then asks the Chancellor, right after the Reliquary pick,
whether the Recent or the Forgotten end fills the slots past them, and
places one site per slot going Hinterward from that end with
`PlaceAtlasSite`, populated as a bandit site (`SiteState.banditSite`) and
dealt its relic slots while the relic deck lasts; the placement is an undo
barrier. Wide Horizons, the
altered Foundation I, fills none. With no Chancellor and no Citizen
seated, Setup With No Empire erodes Foundation I to Wide Horizons and
Foundation V to Mob's Favor, places only the former Empire's sites, as
bandit sites with ruined edifices and every open relic slot filled while
the relic deck lasts, turns the box's Imperial lineages into Exiles
(`CampaignState.boxRoles`) and sets the Reliquary aside
(`CampaignState.reliquarySetAside`): its relics stay in the campaign's
Reliquary and no one can take one in play.

The map's slots fill going Hinterward, 2 in the Cradle and 3 in each other
region (`Region.slots`), and a region's vector may be shorter than its
slots (`MapState.inSlots`, `nextSlot`, `hintermost`). The atlas keeps
every atlas site game start did not place, in order. The projection gives
each region its `emptySlots`, which the table draws as empty frames. In
play, under Wide Horizons, the Explore action fills the next empty slot:
`PlaceAtlasSite` takes the site at the Recent or the Forgotten end of the
atlas and populates it as game start populates an atlas site
(`SiteState.banditSite`), and the `WhenExplored` window then resolves the
site's ruined edifice
([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md),
slice 3).

The completed aggregate includes site resources and bandits, facedown site
relics, ruined Homeland edifices, regional discards, player advisers, World and
relic decks, starting Exile boards and Supply, banners, tracks, the Oathkeeper
goal, favor banks, and the Chronicle's six Foundation faces
(`StartingFoundations`; a number the Chronicle does not list lies normal).

The scope follows Combined Rulebook pp. 6-7 and the New Foundations first-game
clarification on p. 8, and for later games the New Foundations rulebook's
"Setup for Later Games". It excludes Legacy selection/effects. Game start
accepts every face of every foundation
([Foundations design](../superpowers/specs/2026-11/2026-11-11-foundations-design.md));
Foundation III is accepted on either face and does nothing until Legacies.

## Application and transport

`GeneratedFirstGamePlanFactory` derives a random Chronicle through
`FirstGameChronicleGenerator`, or takes a saved one, shuffles seating, and
shuffles both decks into the deal; trusted-game provisioning, which the
development start page also uses, and `MembershipBootstrap`, kept for X6,
both use it (the dev-only loopback bootstrap route and its deterministic
`DevelopmentFirstGamePlanFactory` fixture were retired once the development
start page moved onto the same trusted `POST /games` provisioning). Tests
that need a fixed seating order construct it with an identity (no-shuffle)
`ChronicleRandomPort` instead. Bootstrap accepts only public participant
configuration; the server derives the Chronicle and records it in the first
event. Neither the bootstrap response nor ordinary projection exposes hidden
order.

Shared `FirstGameBootstrapRequest` and its codec compile on JVM and Scala.js.
They are actorless configuration DTOs. `MembershipBootstrap` derives
authorization and player seats from memberships. The application maps bootstrap
configuration to a Chronicle deal and appends the same current event envelope
used by gameplay.

Setup's `Decide`s are ordinary walker decisions: the client resolves them with
the same generic `resolveWalker` intent and walker-decision panel every other
procedure uses, not a bespoke pawn-placement or card-decision command. A
`Phase.Setup` game flows through the ordinary `Ready` projection; the
walker-decision projector governs who sees what -- the parked decision's
owner gets the owner-private `walkerDecision` view (including their
preview-only adviser identities), everyone else gets `walkerWaiting` naming
who it is waiting on. The setup table already exposes populated sites, ruined
Homeland edifices, decks, public pile tops, banks, banners, tracks, first
player, and starting boards, so completion does not visually rebuild the
world. Replay, redaction, codec, and continuity tests cover the boundary.
