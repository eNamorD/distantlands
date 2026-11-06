# Oath Digital Roadmap

This is the forward-looking project-level source of truth for planned work.
When a task is done, it should be deleted from this file.

Refrain from labeling phases with numbers, as the roadmap items may shift
in priority. The Later items keep their X and L labels for consistency with
specs.

## Now

**Phase - Cleanup tasks** holds what finished phases left behind.

### Phase - Cleanup tasks

Left behind by earlier phases:

- [ ] **Show that the action pane scrolls at 1024×700.** At that size the
  action pane is about 330px tall, and the Keep and Discard zones of the
  starting-adviser decision sit wholly below the sticky confirm button with
  nothing showing they are there. At 1440×900 the layout is fine. The pane
  proportions are deliberate (DESIGN.md), so the fix is a visible scroll
  cue or a smaller decision header rather than new proportions. Observed in
  the browser during the clean-up run of 2026-10-23.

Left behind by the
[consent work](superpowers/specs/2026-10-08-consent-system-design.md):

- [ ] **Arranged-start event for game save and load.** A journal must begin
  with `GameStarted`, and no codec exists for a full `ReadyGame`. A journaled
  event that carries a whole arranged position (players, sites, decks, banks,
  turn, walker state) would let a stream begin mid-game: loading a saved game,
  scenarios, tutorials, and bug reproductions from a pasted position. It needs
  a `ReadyGame` wire codec, a game-log line, projection handling, and a
  decision on who may start one. The test-only replay origin from the service
  and log start-state project (2026-09-28) is the seam it would grow from.
  Recorded in the
  [service log start states design](superpowers/specs/2026-09-28-service-log-start-states-design.md).
  The consent play-test below needs it.
- [ ] **Play-test consent in a browser.** The amber destination, campaign
  target, faceup Vision, Hall-protected card and free option of a toll, the confirmation, the
  ruler's prompt, automatic answers, the preferences grid and withdrawing
  with Undo are covered by tests only. Seeding a game with a player-ruled
  Narrow Pass, Secret Police, Hall of Ministers or toll card needs the journaled
  arranged start.

### Phase - Campaign battle summary

Left out of the Game Log action sections
([spec](superpowers/specs/2026-10-05-log-action-sections-design.md)), which
answered the 2026-10-01 playtest's "make it clear what a Campaign's battle
did":

- [ ] A summary of a Campaign's battle, opened by clicking one of its log
  lines: attack and defense dice, battle plans, sacrificed warbands, the
  totals and the winner. It is not always visible.
- [ ] The same summary filling in while a Campaign runs: the dice on each
  side as battle plans are chosen, and the sacrificed warbands.

### Phase - Catalog batch - engine-blocked

Cards that need an engine change first, grouped by the missing mechanism.
[Catalog batch 9](superpowers/specs/2026-11-02-catalog-batch-9-design.md)
(landed 2026-11-02) implemented every edifice face whose mechanisms the
engine had; its [Out of scope](superpowers/specs/2026-11-02-catalog-batch-9-design.md#out-of-scope)
section describes each seam below in a paragraph.

- **Recover source:** Drowned Library.
- **Discard replacement with a question:** Wild Pastures.
- **Vision collection:** Desecrated Ground.
- **As-if ruler:** Bandit Crown, Grand Mask.
- **Favor source redirection:** Vow of Kinship.
- **An askable Act action:** consent for Relic Patrols and Bandit Patrols.
- **Challenge** (`legalBanners` is hard-coded, no ribbon adjustment): School
  of Vines, Sigil of the Eye, Sigil of the Heart, Favored Son, Magician's
  Code.
- **Unchanged from batch 8:** Search Party (recorded in the
  [catalog batch 4 rulings](superpowers/specs/2026-10-05-catalog-batch-4-rulings.md#moved-out-of-the-batch)),
  Bracken, Cracked Horn and Ring of Devotion (a discard destination),
  Master of Disguise and Acting Troupe (acting as another card), Autumn
  Wind, Tracker and False Prophet (a player-aware off-turn card play).

### Phase - All-Exile alpha readiness

`0.1.0-alpha.2`, `0.1.0-alpha.3` and `0.1.0-alpha.4` shipped as GitHub prereleases through the
[alpha release workflow](operations/releases.md), each with bundled archives
for macOS arm64, Windows x64 and Linux x64. The last manual acceptance run is
[recorded](testing/alpha-acceptance-0.1.0-alpha.2.md); `0.1.0-alpha.3` and
`0.1.0-alpha.4` shipped without one. Deferred findings and requested UI changes are in the
[Phase 5 follow-ups](operations/phase-5-follow-ups.md).

- [ ] **Unanswered off-turn decisions.** A parked decision blocks every
  command until its owner answers; a `Simultaneous` park waits on every
  player who still owes an answer. There are no timeouts, forfeits or host
  resolution. Recorded in the
  [walker ownership and phases design](superpowers/specs/2026-09-12-walker-ownership-and-phases-design.md).
- [ ] **Reconnection UX.** After a client loses its connection it should
  retry with exponential backoff and show a **Try Again** button. Recorded
  in the Phase 5 follow-ups.
- [ ] **Alpha deferred defects.** Items 7, 12, 13, 16 and 18 of the
  [follow-ups](operations/phase-5-follow-ups.md#deferred-defects): bracketed
  `[::1]` rejected as loopback, the frontend linker on the test classpath,
  the test `index.html` fixture shadowing the generated one, the hardcoded
  `oathdigital:root` assertion, and no Docker `HEALTHCHECK`. Also document
  host bind mounts at `/var/lib/oathdigital` in the configuration guide.
- [ ] **Several seats in one browser.** Opening a second seat's link for the
  same game in one browser profile silently replaces the first seat, because
  `/s/{seat-code}` sets one cookie name scoped to `/games/{game-id}` and
  redirects there (`TrustedSeatRoutes`). Every release's notes carry this as
  a known caveat. One fix keeps the seat in the URL instead of redirecting to
  the shared game path, for example a per-seat path with its own cookie
  scope. Weigh the seat code staying in the address bar and history, which
  the redirect avoids, and update the network guide and quick start.
- [ ] **A lighter way to test separate-machine play.** The manual
  [acceptance record](operations/alpha-acceptance.md) takes two physical
  machines and a lot of hand-recorded evidence, so `0.1.0-alpha.3` and
  `0.1.0-alpha.4` shipped without one. Find a cheaper check for what it covers (LAN reachability,
  seat links opening on another machine, reconnect and restart, HTTPS proxy
  and origin behavior, and the bundled archives' first launch on each OS),
  ideally something a release run can execute or that takes minutes by hand.
  Then decide what happens to the record.

### Phase - Negotiation terms and promises

A Negotiation deal holds favor and relic transfers and disclosures only. Add
promises about future actions, transfers of secrets, advisers, sites and
banners, remote and private Negotiation, a registry of term kinds, and
eligibility powers. Record ignored Negotiation rules per participant rather
than for the actor only.

- [ ] **Negotiation cards.** The Gathering and Whispering Stone need a nested
  Negotiation with its own participants. Council Arbiter, Deed Writer and
  Traveling Negotiator widen who may negotiate and what a deal may hold.
  Catalog batch 8 left them for this phase.

### Phase - Chronicle steps

After the all-Exile alpha, implement the Chronicle and persistent campaign:
Foundation mutation, Legacy activation and scoring, and era scoring.

- [ ] **Foundations.** Fills the Beacon's empty Shape Foundation window.
  Game start also sets every foundation to its normal face and ignores the
  Chronicle's stored faces.
  Game start also refuses a later game with no Chancellor and no Citizen
  seated, which needs Foundation I's altered face, Wide Horizons, and its
  Explore action.
- [ ] **The Grand Council and Festival banner faces.** Legacies that set
  Foundation V or VI turn the banners to these faces. Their ids are in
  `RuleSourceIndex`, and the faces have no behaviour.
- [ ] **Legacies.** Fills the Sun's empty steps 2-5 and the legacy bonus in
  Score Lineage Points. Game start refuses a seated lineage's legacies and
  starting relics until then.
- [ ] **Eras.** Era end, tie-breaks (F7) and the reset of lineage points.
- [ ] **Edit Chronicle.** The Chronicle page shows the button disabled,
  "Coming in a later release". Edits a saved Chronicle's fields in the
  browser.

### Phase - Settings Feature

Settings players can change during a game.

- [ ] **Ignore Citizenship powers.** Toggle the first-game option that
  ignores "become a Citizen" powers. Game start sets it, and nothing changes
  it during a game ([Empire phase design](superpowers/specs/2026-10-29-empire-phase-design.md),
  slice 3).
- [ ] **Hover warnings.** Show warnings on hover, turn them off, or disable
  the options that carry a warning.
- [ ] **Hide disabled options.** Hide options instead of showing them
  disabled with a reason.
- [ ] **Names.** Change lineage and player names.

### Phase - Gameplay completeness gate

Audit every rulebook procedure and every runtime component handler against the
traceability matrix; close remaining hidden-information, simultaneous-ordering,
randomness, consent, and nested-action gaps; then run complete replay,
persistence, server, Scala.js, packaged-network, and browser acceptance gates.

## Deferred items

Unscheduled work, grouped by area. Pull an item into a phase when it blocks
one.

### Setup deferred items

- [ ] **Simultaneous setup effects.** The Chancellor or first player should
  resolve simultaneous setup effects; site order is used until then. Still to
  add: player choices earlier in setup once foundations and legacies exist,
  such as the Chancellor choosing Recent or Forgotten sites; and WHEN
  EXPLORED triggers once an explore procedure exists.
- [ ] **Keyboard focus on the Chronicle pages.** A redraw of the Chronicle
  page drops the focused control, and the table's save dialog does not trap
  focus. Do it with the next accessibility pass over the start pages.
- [ ] **Desecrated Ground.** E08's ruined face lets an Exile at its
  site keep any number of revealed Visions, which turns
  `PlayerState.revealedVision` into a collection across card play, projection,
  Vision victory, the frontend and the save format. E08 never appears in
  generated games, since the Nomad homeland becomes E06. Do it when E08 can
  appear. Recorded in the
  [rule gaps design](superpowers/specs/2026-09-26-rule-gaps-in-implemented-cards-design.md).

### Game Log deferred items

- [ ] **Phase headlines inside an action.** A power or minor action that
  ends the Act phase (Murky Fountain, Wizard School, Special Envoy, the
  Grand Scepter's Action, self-exile) posts its "Rest" headline before the
  action's own line, so the action reads as part of Rest. Hold a headline
  raised inside an action until after that action's lines. The
  `scepter-citizen` and `self-exiled` golden logs show it.
- [ ] **First-player marker with a Chancellor.** `GameProjection` sends
  `setup.firstPlayer` as the first player, but with a Chancellor seated
  rounds start from `tracks.roundStarter`. Send the round's starter so the
  board's marker points at the player who began the round.

- [ ] **Show a parked action's progress in the waiting message.**
  The Game Log posts a line only once its facts are complete, so an action
  that is still parked says nothing about its choices so far. The table's
  "Waiting on player" message is the place to show that progress. Recorded in
  the [Game Log design](superpowers/specs/2026-09-25-game-log-design.md).
- [ ] **Game Log features the design left out.** Click-to-highlight,
  filters and search, timestamps, a server-side read marker, failed victory
  checks and negotiation proposals. Replay navigation is **L4**. Recorded in
  the [Game Log design](superpowers/specs/2026-09-25-game-log-design.md).
- [ ] **Warn before a choice that has no effect.** The rules let a
  player pay for a power or pick an option that then does nothing: a
  modifier selected for an action it will not change (Cup of Plenty,
  Animal Playmates or Birdsong with a card of the wrong suit), a power whose
  target is empty, or a bank with no stock. The game should warn the player
  before they commit, without refusing the choice. Recorded while designing
  Catalog batch 2.
- [ ] **A card play's line before the lines its own batch causes.**
  `WalkerSteps.recordBatch` runs `AdviserArrivalPass.after` and an executed
  trigger's follow-up inside the play's own leaf, so a power whose effect
  fires during the play writes its line first and the play's line reads
  second, where it used to read first. Putting the play's line back in
  front needs it journaled inside that batch, and a `Note` is not a
  `CoreOperation`. See the wording ledger, "A card play's line now follows
  the lines its own batch caused". Recorded in the procedures journal their
  own notes design of 2026-10-16.

### Powers-related deferred items

- [ ] **Reasons on the remaining options.** Add hover reasons to the option
  builders that still lack them, as playtests find options people wonder
  about. Left by the
  [board decisions design](superpowers/specs/2026-10-03-board-decisions-design.md).
- [ ] **Mercenaries' player-chosen sign.** Mercenaries adds attack dice
  when its user attacks and removes defense dice from the attacker when it
  defends. The card lets the player choose the sign, and the plan fixes it by side.
  Choosing it needs a decision inside the plan and a preview that shows both.
- [ ] **Record a bandit's applied battle plan as an event.** A player's
  plan is a recorded answer that later windows read. A bandit defender applies its
  cost-free plans without asking, so `CampaignPlanApplication` records the use as
  a `ModifyDicePool` marker under `campaign.plan-applied.<kind>.<id>`. The marker
  shows in the journal as a dice-pool change and suites that count a Campaign's
  `ModifyDicePool` operations see it. A dedicated recorded operation would say
  what happened.
- [ ] **A board slot for distributions and Sticky Fire without a choice.**
  Warning Signals names the defender's board by a player option in its distribution, which
  the panel shows as a player name, and Sticky Fire asks its question even when a yes
  changes nothing (against bandits it only costs the favor). A board option, and skipping a
  question whose answers are the same, need a small change to the option vocabulary.

### Engine deferred items

- [ ] **Campaign notes read the attack rules.** The attack line counts skull
  faces as the chosen plans score them (Lancers doubles them), so
  `CampaignNotes` imports `powers.campaign.AttackScore`, whose `changes` name
  the plan cards. It is the one exception to "`gameplay/actions` imports
  nothing from `gameplay.powers`" that `scripts/check-architecture.py`
  allows. Plans that change the attack's scoring could contribute their
  `AttackChange` through a power window instead, or the scored roll could
  record its skull faces.
- [ ] **Move Campaign's pinned facts onto start facts.**
  `CampaignSetup.pinned` stores the Campaign's kind and defender as sentinel
  `ModifyDicePool` operations; start facts
  ([travel destination design](superpowers/specs/2026-10-07-travel-destination-walker-design.md),
  ruling 6) are the durable place for them.
- [ ] **One seam for "who may veto an operation".** `OperationPolicy`
  (an exact-shape allowlist, used by `StateBasedOperationPolicy` for
  state-based events) and `OperationRestriction` (contextual reasons from
  powers) answer the same question through two interfaces. Migrating the
  policy onto `OperationRestriction` would leave one seam, but it reorders
  precedence on the state-based path (allowlist reasons come before shape
  reasons today) and needs its own preservation argument. Recorded during the
  [operation family consolidation](superpowers/specs/2026-09-24-operation-family-consolidation-design.md)
  of 2026-09-24.
- [ ] **Key `CardKnowledge` by card id.** Knowledge is stored per
  place: `siteRelics` by viewer and site, `heldRelics` and `advisers` by
  viewer, and an owner's knowledge is implicit in `identifiesCard`. The Game
  Log phase patches the gaps this leaves (an ex-owner and a site peeker both
  lose a card's name when it moves) by copying knowledge as cards move. A
  single map from card id to the players who know it would make "knowledge
  follows the card" structural instead of maintained per move. Recorded in
  the [Game Log design](superpowers/specs/2026-09-25-game-log-design.md).
- [ ] **Frontend and protocol clean-up.** The projection has no protocol
  version field. (The decision ids and option ids both sides spell are
  settled by the
  [decision-id vocabulary decision](superpowers/specs/2026-10-18-decision-id-vocabulary-decision.md)
  and are not work.) `WalkerDecisionProjectorSuite` fabricates parks instead of
  reaching them through a Situation. `OathViolation.InvalidSearchPlacement`
  also reports card-play refusals outside Search (a facedown adviser play, a
  forbidden faceup Vision), and renaming it changes the wire format.
  Table-session clean-up is parked: no
  `SeatMode` enum, the `HttpGameClient` downcast for the raw event history in
  `TableSession`, and duplicate jsdom tests. Recorded in the typed decision
  form and table session designs.
- [ ] **Gather each restriction window once.**
  `WalkerPowerGather.attributed` gathers a window inside `windowsIn`, again
  for `rejected` and again for `emptied`, and `applyWindow` rebuilds its
  power-id map on every fold although `WalkerPowers` already caches five other
  derived values as lazy vals. `PhasePowerProcedure.startsFor` keeps a second
  state-keyed memo in the shape of `SearchMemo`. Gather once per window, make
  the map a lazy val, and give the two memos one shape. Profile first; the
  `ActMenuBudget` benchmark is the measure. Recorded as the first follow-up of
  the [test speed design](superpowers/specs/2026-10-07-test-speed-design.md).
- [ ] **Settle the engine's parked roll.** The parked-roll command path is
  gone, but the engine still parks on a `Roll` whose mode is `Parked`, which
  is the model default. `OathRulesWalker.rollWalkerPrepared` has test callers
  only, and `checkAnswerable` still accepts a Roll park that nothing projects
  and no command answers; nothing in production builds one. Either flip the
  default to `Automatic` and retire the engine half with the nine suites that
  build a parked roll, or keep it and make `checkAnswerable` refuse the park.
  See the
  [parked-roll retirement decision](superpowers/specs/2026-10-23-parked-roll-retirement-decision.md).

### Architecture candidates

Found by the two architecture scans of 2026-10-23, after every candidate of
the 2026-10-13 review had a record. Each is measured, not designed: take one
through a design conversation, and re-measure it at the current commit first,
before treating the numbers below as current.

- [ ] **A walker procedure is its own registry adapter.**
  `WalkerProcedureRegistry.Entry` has nine columns; of twenty entries one sets
  `startFacts`, two `optionPricing`, two `rollFeedback`, four
  `requiresPlayableOption`. Travel's `queuedAnswers` is a hand match outside
  the table, and `isRegistered` re-enumerates the refs `lookup` enumerates.
  A procedure object with defaults for the rare members would carry them.
  Weigh it against the initialisation cycle that made the registry hold
  function references, which already bit the notes table.
- [ ] **Narrow `ContributingPower` to capability traits.** The trait has 22
  members, six of them overridden by one power, and every new engine hook has
  landed as another defaulted member. Attach the rare ones as traits the way
  `OfferHost`, `RerollHost` and `SelectedModifier` already work.
- [ ] **Route Recover through the generic surfaces.** The client still holds
  three Recover decision ids, a `RecoverStep` enum and `renderRecoverPanel`,
  whose Choice arm computes whether the player has Supply, a legality fact
  only gameplay should own. Recover would declare Continue unavailable
  without Supply, with a hover reason, and word its two buttons. Named open
  by the 2026-09-24 parked-decision route design.
- [ ] **Declare a decision's subject card.** `WalkerDecisionProjector`
  re-spells four `cardplay.*` prefixes that `CardPlayProcedure` keeps private,
  to recover the subject card that CONTEXT.md defines and no model field
  carries. The procedure would declare it on the `Decide`.
- [ ] **Derive the wire codecs, with a defaults macro.** The probe of
  2026-11-03 stopped on the design note's own condition: no Mirror-only
  deriver reproduces today's wire, because a field's presence follows its
  case-class default, which a Mirror cannot see, and a few fields are
  renamed or computed. Deriving now needs the note's `scala.quoted`
  defaults macro plus per-field overrides, or a wire change in place.
  Decide when a wire-heavy phase begins. Design note:
  [derive the wire codecs](superpowers/specs/2026-11-02-derive-wire-codecs-design.md),
  "Probe result".
- [ ] **One frontend client (optional).** The development shell still talks
  to `/api/dev/first-games/{gameId}?playerId=` through its own
  `HttpGameClient`; mounting the dev API at `/games/{gameId}/api` with the
  query-parameter resolution would retire the second client. Slice 5 of the
  [one HTTP gateway](superpowers/specs/2026-11-02-one-http-gateway-design.md)
  note, left for a frontend session.
- [ ] **`ConsentDesk` settles a batch.** Its seven verbs have one caller,
  which owns their order at nine sites; the last five consent commits all
  edited the service. The desk could take a batch and return it settled.

## Later

- [ ] **X6 — Complete production authentication and deployment**
  - [ ] Add OIDC Authorization Code + PKCE, session issuance/rotation/logout,
    secure cookie-setting responses, and frontend login/session-expiry UX;
    then mount `GameApiRoutes` with `SessionMembershipActor` and a bootstrap
    route on `MembershipBootstrap`, which the retired authenticated transport
    left behind for it.
  - [ ] Add membership-management UX, rate limiting, audit logging, and the
    final production deployment gate. The alpha phase may add a narrower safe
    invitation flow first; until then, keep development routes loopback-only.
- [ ] **L3 — Licensed game assets with permanent visual fallbacks**, including
  suit and restriction icons on card faces.
- [ ] **L4 — Saved-game browser and replay navigation.** Saved campaigns
  are browsable; saved games and replays are not.
- [ ] **L5 — Asynchronous accounts, invitations, and notifications**
- [ ] **L6 — Incremental implementation of remaining phases and rules** (tracked
  in the phased sequence above; the next denizens and relics come
  from **Phase - Catalog batch - engine-blocked**)
- [ ] **L7 — Incremental synchronization transport**
  - Replace complete-snapshot polling with conditional responses, projection
    deltas, long polling, SSE, or another push transport when scale or latency
    justifies the added server lifecycle complexity.
  - A cheaper first step keeps redaction intact: a change-check that returns
    the viewer's current sequence (or a 304), fetching the projection only
    when it advanced. See the
    [Phase 5 follow-ups](operations/phase-5-follow-ups.md#snapshot-polling-sends-a-full-projection-on-every-tick).
- [ ] **Table UI rework.** Rework the layout of player areas and sites, and
  port the map to haunt-roll-fail's canvas approach. Recorded in the
  [card shape and inspection design](superpowers/specs/2026-09-22-card-shape-and-inspection-design.md).
  The playtest of 2026-10-01 asks whether the whole table, not only the map,
  should move to an HTML5 canvas.
- [ ] **Clicking a banner shows its details.** Darkest Secret and The
  People's Favor are clickable like other cards, opening their details.
- [ ] **Match the map's aspect ratio to the physical board.** The rendered
  map's proportions differ from the actual game map's.
- [ ] **In-game chat.**
- [ ] **Public Chronicle pages.** The winner of a game may write their summary
  of it, and players can browse all the summaries.
- [ ] **Private lineage pages.** Each lineage has its own private notebook for
  notes.
- [ ] **Distribution beyond the alpha.** Intel macOS and Linux arm64 archives,
  code signing and notarization, native installers, and spectator links.
  Fingerprint asset names so they can be cached long-term again.

## Standing rules

- New code reads cards from their card objects. A power is a `case object`
  that holds no `catalog: ExecutableCatalog` and offers no `forCatalog`
  constructor: one that looks other cards up reads `PowerCtx.catalog`, or
  the `catalog` its hook is handed (`PhasePower.build`,
  `operationRestrictions`, `PowerUseRestriction.refusal`, `AdviserArrivals`,
  `ExecutedBatch`, `PlanContext`), and `scripts/check-architecture.py`
  refuses any other. A new test names cards and sites by their card objects
  (`AlchemistCard`, `DeepWoodsSite`), never by a printed name looked up at
  run time (BackendArchitectureSuite checks).
- The verification gate is the complete JVM and Scala.js suites, the optimized
  Scala.js linker, the card catalog suite, the architecture and documentation-link
  checks, and diff validation.
- Before the first public release, event formats and fixtures may change in
  place; backward compatibility, migrations, and version bumps are not required.
  Update the current writer, reader, replay tests, fixtures, and development
  data together. Public release establishes the compatibility baseline.
