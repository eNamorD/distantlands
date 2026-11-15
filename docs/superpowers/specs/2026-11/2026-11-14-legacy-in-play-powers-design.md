# Legacy in-play powers

**Status:** designed 2026-11-14, approved section by section by the owner.
All four slices have landed ([slice 1 plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice1.md),
[slice 2 plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice2.md),
[slice 3 plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice3.md),
[slice 4 plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice4.md),
[implementation rulings](2026-11-14-legacy-in-play-powers-implementation-rulings.md)).
It follows the [Legacy Chronicle powers](2026-11-13-legacy-chronicle-powers-design.md)
and [Legacy Setup powers](2026-11-13-legacy-setup-powers-design.md)
batches, which have landed; slice 4 here meets Quest to Distant Lands'
first Wake.

**Builds on** the [Legacies design](2026-11-13-legacies-design.md), which
landed the legacy lifecycle, and the Chronicle batch's holder seam,
`ChronicleLegacyRules.holder`.

## Goal and scope

The powers legacies use during a game, which clears the ROADMAP's
"Legacy powers: in play" item. Fifteen pieces, thirteen of them whole
legacies still in holding, plus The Ear's ACTION half (its card counts
as implemented for its Chronicle half) and Iron Hand's discard when its
holder becomes Imperial during a game:

| Slice | Powers | Hooks |
|---|---|---|
| 1. Seam and ACTIONs | Ruthless, Peacemaker, Golden Key, High Priest (ACTION), The Ear (ACTION) | Legacy source refs in the action menu, costs and decisions; the Reliquary in the shared bank |
| 2. Modifiers | Ancestral Lands, Pathfinder, Keeper of Order, Light Fingers, Beloved, High Priest (WHEN NEGOTIATING) | Travel windows; a legacy battle-plan source; Take Wealth's pawn gate; trade sources; deal terms |
| 3. Triggers and role | The Needle, Arbiter, Iron Hand's discard | Executed banner moves and a banner flip; a step before the end die; `RoleChanges.becomeImperial` |
| 4. Activation and prediction | The Mouth, Conspirator | Mid-game legacy activation; a private prediction and a win override |

Slices run in order. Slice 1 builds the seam the rest use. The Mouth and
Conspirator come last: The Mouth can activate any other legacy, and
Conspirator changes who wins.

Every power here acts only in a later game. In a first game every legacy
is dormant, and a dormant legacy has no power.

## Rulings

Taken with the owner on 2026-11-14. They open a "Legacy in-play powers
rulings" section of `docs/rules/ambiguities.md`, which the implementation
rulings then continue.

### General

1. **Only seated players' active legacies act.** Dormant legacies, a
   boxed lineage's legacies and the Public Ambitions row never act. The
   Mouth (rulings 23 and 24) is the one way a legacy becomes active during a
   game.
2. **"If you are an Imperial" is read when the power is used,** through
   `ChronicleLegacyRules.holder` with the Imperial roles. The Crown and
   Citizenship change it during a game, and every later use reads the new
   role.
3. **A legacy ACTION is a minor action** in the Act Phase menu, with the
   legacy named as its source. It follows the same reuse and cost rules as
   any other ACTION power, and its burnt cost is paid as a card's is.
4. **Passive powers check their holder; chosen powers carry a source.**
   A power the holder never picks (Travel, Take Wealth, trade, deal terms,
   triggers) checks `ChronicleLegacyRules.holder` in its `applicable`, as
   Tyrant checks a faceup adviser. A power a player picks (an ACTION, a
   battle plan) carries the legacy as its source, so the menu, the cost
   and the log can name it.
5. **A legacy is in its holder's play area.** Rules that read a player's
   play area for powers, such as Imperial Tactics, reach it.

### Slice 1: seam and ACTIONs

6. **The seam.** Legacies gain a case wherever a power's source is
   resolved for a menu, a cost or a decision: `PhasePowerProcedure`'s
   `sourceRef`, `sourceOf` and `payable`, `PowerSourceRef.of`, and
   `DecisionOptionRef.ofCard` and `cardOf`. `model/Decisions.scala` is near
   the 800-line limit and is split first. The printed costs of Ruthless
   (burn 2 favor), Golden Key (burn 1 secret and 1 favor) and The Mouth
   (burn 2 favor) are corrected from `Cost.free`. In the event `payable`
   needed no case: a legacy source is `PowerSourceRef.Card`, which it
   already pays from.
7. **Ruthless.** The holder buries any denizen at their site: a faceup
   site card, locked or not (bury ignores locked), edifices included (CR:
   "You can bury locked cards, including edifices"). Tokens on the card
   return as `Bury.standard` returns them. It is not offered when the
   holder's site has no denizen.
8. **Peacemaker.** Offered while the holder holds a banner; holding both,
   they choose which. The banner goes to the shared bank with its tokens
   left on it ("Do not adjust its favor or secret"). Then every site and
   every player board keeps one warband of what it holds, the holder's
   included: Imperial, Exile and bandit alike. Removed warbands return to
   their owner's bank, and bandits to the bandit supply.
9. **The Reliquary is always in play.** It may be empty. A non-empty
   Reliquary is shown in the shared bank, facedown; its faces are shown
   only to a viewer allowed to see them (Golden Key's holder, and the
   Chancellor during Setup as now).
10. **Golden Key.** An Imperial holder may always peek at the Reliquary:
    the projection shows them its faces. Its ACTION takes any relic from
    the Reliquary, chosen by the holder, facedown to their play area, as
    the Chancellor takes one at Setup. It is not a Recover: no recover cost
    or power applies. It is not offered while the Reliquary is empty.
11. **High Priest's ACTION.** Offered while the holder holds the Darkest
    Secret and it carries at least one secret. The holder chooses an
    amount from 1 to the secrets on it, and that many move to their board.
12. **The Ear's ACTION.** Free. The holder picks a site with at least one
    relic they have not identified and peeks at all its relics. They know
    them while they stay at that site, as with the minor action's peek.
13. **Lines.** Each power writes one line naming the legacy: "p1 buried
    Herbalist with Ruthless", "p2 returned the People's Favor with
    Peacemaker", "p3 took a relic from the Reliquary with Golden Key".
    The Ear's line is public and never names the relics.

### Slice 2: modifiers

14. **Ancestral Lands.** An Imperial holder's travel to a Cradle site
    costs 1 Supply, applied after every other travel-cost change unless
    one has already made it lower. Only the cost changes: a Pass still
    restricts.
15. **Pathfinder.** The holder's travel ignores the Mountain and Island
    cost powers and the Pass's travel restriction, always and without a
    question, since ignoring them is never worse. It hooks each window
    those powers hook, so its ignore gets a vote there. The Pass's
    campaign-targeting limit is not a Travel power and still applies. The
    foundation it sets is already handled by the Legacies batch.
16. **Keeper of Order.** A battle plan with the legacy as its source,
    usable once per campaign by the attacker or the defender. It applies
    only while the enemy is an Imperial player, as in Imperial
    Infighting. Its user chooses to add 2 attack dice or remove 2. When its
    user is the attacker, they also ignore every skull they roll, rescored
    as Outriders rescores. By ruling 5, another Imperial may use it
    through Imperial Tactics with the holder's consent; "you" is then that
    Imperial, whose enemy must be Imperial and whose skulls are ignored.
17. **Light Fingers.** Take Wealth's "no enemy pawns at your site" gate
    becomes a Restriction at `WakeTakeWealth`, which Light Fingers'
    holder ignores, always and without a question.
18. **Beloved.** When the holder trades, the sources widen to the faceup,
    empty advisers of other players whose pawns are at the holder's site.
    No consent is needed. Trade's own rules are unchanged: the token goes
    on that adviser, the favor comes from its suit's bank, and the bonus
    counts the trader's own faceup matching advisers. A facedown adviser is
    excluded, since its suit is hidden. Tokens later leave the card at the
    next Rest, as every card's tokens do (CR, "clean up favor and secrets";
    implementation ruling R21).
19. **High Priest, when negotiating.** The holder may give secrets in a
    deal. The term is one-way: the other side still cannot give secrets
    unless something else opens them. A new negotiation start-fact marker
    keeps the deal's term rules on resume.

### Slice 3: triggers and role

20. **The Needle.** After a banner moves into the holder's play area by a
    move the walk executes (a challenge, a raid, Favored Son, Wizard's
    Conclave, the Conspiracy, The Standard Bearer at Setup), the holder
    chooses "Flip it" or "Keep this side". The trigger watches the
    executed move through `ExecutedTrigger`; a banner placed at game start
    is not taken. A new core operation flips a banner: the People's Favor
    between Mob and Grand Council, the Darkest Secret between Wandering
    Flame and Festival. Its tokens stay on it. Flipping to Festival places
    no Festival marker, which is placed only in its own Rest step;
    flipping away from Festival removes the marker. No foundation changes.
21. **Arbiter.** The end die is rolled only for a Stable Regime check, at
    the end of rounds five to seven. Just before that roll, a new
    triggered walker step asks the holder to choose −1, +1, +2, +3 or
    Decline (no change). The choice is public and made before the roll.
    The `EndDieRolled` event gains the modifier, checked on replay, and
    `EmpireEndings.stableRegime` compares the modified total with the
    printed rolls read as "at least": 6 or more after round five, 5 or
    more after round six, 3 or more after round seven. The lines are "p2
    modified the end die by +2 with Arbiter", then the roll's line with
    the die and the total.
22. **Iron Hand's discard during a game.** When its holder becomes
    Imperial, Iron Hand goes to the legacy deck right after `RoleChanged`,
    in `RoleChanges.becomeImperial`, so every path to Citizenship
    discards it. It discards what the Chronicle's discard does (Chronicle
    powers ruling 18). The line is "p3 discarded Iron Hand on becoming a
    Citizen".

### Slice 4: activation and prediction

23. **The Mouth: what it activates.** The holder's own dormant legacies,
    and the legacies in the Public Ambitions row, whose goals the holder
    meets when the action is used. Another player's dormant legacy is not
    a target: it is hidden, and the card names the Public Ambitions row as
    the exception. Goals are read with `LegacyGoals.meeting`, as Resolve
    Legacies reads them: Imperial sites are ruled by every Imperial, and
    bandits do not compete. It is not offered when no legacy qualifies.
24. **The Mouth: activation.** An own legacy flips faceup, which reveals
    it; a public legacy moves faceup into the holder's lineage. The change
    is written to the campaign. A legacy that alters a foundation flips it
    now, as activation does in the Chronicle. With more than three active
    legacies, the holder discards active legacies of their choice to the
    legacy deck until three remain, flipping no foundation back. The
    activated legacy is in effect at once (ruling 1); one whose powers are
    all SETUP or CHRONICLE does nothing until those windows.
25. **Conspirator: the prediction.** A forced Wake power in round one,
    since every player's first turn is in round one. A holder who gains
    Conspirator later, by The Mouth, never writes a prediction, and the
    power does nothing. The holder writes a round from one to eight and a
    winner from the seated players, themselves included: two private
    choices. A Quest to Distant Lands holder places their pawn first, then
    writes.
26. **Conspirator: secrecy.** The prediction is stored as hidden
    information. The projection and the log redact it from every other
    viewer until the Sun reveals it, after Royal Line's choice ("p2 wrote
    down a prediction with Conspirator"; as implementation ruling R24
    amends it); the holder sees it.
27. **Conspirator: resolution.** When the game ends, the round it ends in
    and the final winner, after any Successor substitution and any Royal
    Line choice, are compared with the prediction, at the Sun's first
    window right after Royal Line's step (as implementation ruling R24
    amends it). If both match and the holder still holds
    Conspirator active, the holder wins instead, under a new victory kind
    `Conspirator`. The prediction is revealed in the log either way. A
    Conspirator lost during the game, as to The Mouth's cap, voids it.

## Approach

- **The hybrid (ruling 4).** Passive powers are `ContributingPower`s
  whose `applicable` asks `ChronicleLegacyRules.holder`. The walker makes
  no source check of its own, so these need no source plumbing. Chosen
  powers carry `RuleSourceRef.Legacy`, which `RuleSourceIndex` already
  indexes and `PowerAccess.accessible` already accepts; slice 1 fills the
  places that fall through to `None` for it.
- **ACTIONs** are `PhasePower`s, mostly `PaidAction`s, listed by
  `PhaseMenu.offers` once `PhasePowerProcedure.sourceRef` resolves a
  legacy. Analogues: Giant's Rampage and Bed of Roots (Ruthless),
  `BannerToSharedBank` and Ancient Binding (Peacemaker), the Setup
  Reliquary pick (Golden Key), Book of Records (High Priest), the minor
  peek (The Ear).
- **The Reliquary's visibility** is a clause in
  `GamePresentationProjector.identifiesCard`; the shared bank's view
  gains the Reliquary when it is non-empty.
- **Battle plans.** `CampaignPlanSource` gains a legacy case, as the
  Oathkeeper title has its own. Keeper of Order follows Mercenaries for
  the dice and Outriders for the skulls. Imperial Tactics' candidates
  gain other Imperials' legacies.
- **Travel** follows the Coast powers: Ancestral Lands adjusts supply at
  `TravelCost`, and Pathfinder's `shouldIgnore` names the Mountain, Island
  and Pass powers at each window they hook. Both join
  `TravelModifiers.all`.
- **Take Wealth's pawn gate** moves from `TakeWealthProcedure`'s build to
  a Restriction, as `TakeWealthLimit` is one. **Trade** widens its
  sources at `TradeSourceSelection`, as Skilled Merchants does, and
  `MusterSource` learns to locate another player's adviser. **Deals** gain
  a one-way secret giver beside Council Arbiter's adviser traders.
- **The banner flip** is a new core operation beside
  `BoardControlOperations.moveBanner`, which handles the Festival marker
  today. The Needle's trigger is an `ExecutedTrigger` on an executed
  banner move into the holder's play area.
- **The end die** is rolled outside the walker
  (`StateBasedEvaluation.endRound`), so Arbiter's step is a triggered
  walker procedure started before the roll, as the Oathkeeper's start is,
  and `EndDieRolled` gains its modifier.
- **The Mouth** reuses Resolve Legacies' `Flip` and take, its foundation
  flip and its cap discard. In-game changes to `LegacyState` already reach
  the Chronicle through `ChronicleRecord.campaignFields`.
- **Conspirator** needs a hidden store for the prediction, journaled and
  restored on replay, and redaction in the projection and in
  `DetailLines`, which logs decision answers to everyone today. Its win
  override follows Royal Line's `WinnerNamed`, applied after
  `EmpireEndings`' Successor substitution, at every place a result is
  written.
- **Decisions.** Every new `Decide` has a resume test, and no two
  decisions share an id. No Branch's children or ids depend on state the
  walk itself changes.
- **Holding.** Each card leaves `HoldingLegacies` as it lands, and
  `PowerImplementationStatus` lists it. The Ear and Iron Hand left
  holding with the Chronicle batch, so only their status notes change.

## Slice 1: seam and ACTIONs

The legacy source cases; the `Decisions.scala` split; the cost fixes; the
Reliquary in the shared bank and Golden Key's peek. Ruthless, Peacemaker,
Golden Key and High Priest's ACTION; The Ear's ACTION half.

Tests: each ACTION offered and not offered (no denizen, no banner, empty
Reliquary, no secrets on the Darkest Secret, no unidentified relic); its
burnt cost paid; Ruthless burying a locked denizen and an edifice;
Peacemaker leaving one warband on every site and board, bandits and
Imperials included; Golden Key's peek shown to its Imperial holder and to
no one else, and gone once the holder becomes an Exile; The Ear's
knowledge lasting while the relics stay; a dormant and a boxed legacy
offering nothing. A golden log.

## Slice 2: modifiers

The Travel powers; the legacy battle-plan source and Imperial Tactics'
reach; Take Wealth's Restriction; trade's widened sources; the one-way
secret term. Ancestral Lands, Pathfinder, Keeper of Order, Light Fingers,
Beloved and High Priest's negotiating power.

Tests: Ancestral Lands into the Cradle from a Mountain route and as an
Exile (no change); Pathfinder over a Mountain, an Island and a Pass,
with the Pass still limiting a campaign; Keeper of Order adding and
removing dice in Imperial Infighting, its skulls ignored only when
attacking, used by another Imperial with consent, and not offered against
an Exile or the bandits; Light Fingers taking wealth beside an enemy
pawn; Beloved trading on another player's faceup adviser and not on a
facedown one; High Priest giving secrets in a deal while the other side
cannot, on resume too. A golden log.

## Slice 3: triggers and role

The banner flip; The Needle's trigger; Arbiter's step and the
`EndDieRolled` modifier; Iron Hand's discard in `becomeImperial`. The
Needle and Arbiter leave holding.

Tests: The Needle flipping after a challenge and after a raid, the
Festival marker removed on flipping away and not placed on flipping to
it, and no question at game start; Arbiter turning a round-five 4 into a
win with +2 and a round-seven 3 into a loss with −1, no step when the end
die is not rolled, and replay rejecting a changed modifier; Iron Hand
discarded on Citizenship by the Grand Scepter's offer and by a deal. A
golden log.

## Slice 4: activation and prediction

The Mouth's targets, activation, foundation flip and cap; Conspirator's
forced Wake, hidden store, redaction and override. The Mouth and
Conspirator leave holding.

Tests: The Mouth activating its holder's dormant legacy and a Public
Ambitions legacy, the activated power usable that turn, a foundation
flipped, the cap discard, and not offered when no goal is met;
Conspirator's prediction redacted for every other viewer and in the log,
a correct prediction beating a Successor win, a wrong round or winner
changing nothing, a Conspirator lost to the cap voiding it, a Quest
holder placing first, and a Conspirator gained by The Mouth never asking.
Resume tests for each decision. A golden log through the game's end.

## Docs

Each slice updates, as it lands:

- `docs/rules/rules-reference.md`: a "Digital:" note on each rule a power
  changes;
- `docs/rules/ambiguities.md`: the rulings above;
- `docs/rules/implementation-traceability.md`: each legacy's row;
- `docs/ROADMAP.md`: the "Legacy powers: in play" item ticked when slice
  4 lands.

## Out of scope

- Cards that act on legacies: Ancient Writ, Amber Doors and Lost Doors,
  Ruined Temple, Stone Baths and Ruined Baths (the ROADMAP's own item).
- Simultaneous effects: who orders powers that share a window.
- Interface work beyond the Reliquary in the shared bank and the holder's
  view of their own prediction.
- Eras and the reset of lineage points.
