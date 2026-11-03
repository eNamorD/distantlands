# Audit: the walker follow-ups

> Status: followed up, 2026-11-03, with the owner's recommended defaults.
> Section 1 landed as commit b5da1f60 (a start walks its procedure), section
> 6 as 27f0a52e (the waiting line skips the look-ahead) and question 3 as
> cafeebd0 (Empty Attack Pool, applied where every remove-attack-dice plan
> takes effect). Section 5 and question 4 are overtaken: the Empire branch
> seats Imperials, and `GameStartRules` refuses the Chronicle shapes not
> yet supported. Of section 2's seven cards, Spiral Castle and the Hall of
> Debate and Tome Guardians restrictions landed in catalog batch 9 and
> Martial Culture and the Scepter's plan with the Empire; Glamor, Relic
> Hunter and Obsidian Cage have the owner's rulings of 2026-11-03, recorded
> under the roadmap's "The last battle plans", and wait to be built.
>
> Written as a read-only audit, 2026-11-02, of the ROADMAP item "Walker
> follow-ups" under Powers-related deferred items. The item was recorded by
> the [campaign walker design](../../superpowers/specs/2026-09-19-campaign-walker-design.md)
> ("Deferred, and recorded") and
> [bounded-campaign.md](../../architecture/bounded-campaign.md) ("Deferred").
> Nothing here changes code. Line numbers are those of the tree at commit
> `0d1ae92`. Rules are cited from the New Foundations rules text
> (`bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`, by section name and
> line) and, where the repository's own layer has them, by Combined
> Rulebook page from [docs/rules/glossary.yaml](../../rules/glossary.yaml).

Vocabulary: [CONTEXT.md](../../../CONTEXT.md) defines **actor**, **parked
decision**, **start selection** and **start fact**.

## Summary

The item bundles five findings of different ages. Ranked by what a player
could meet:

1. **`PowerCtx.procedure` is `None` on the command that starts a procedure
   and `Some` on every later one.** The two code paths are named below. One
   power reads it today (Welcoming Party), safe only because a Search
   always parks before the card is played. The fix is one parameter
   threaded from `OathRulesWalker.startNow`, or one `updateCurrent` as the
   look-ahead probes already do.
2. **The battle plan families are mostly done.** Of the 85 cards the card
   data tags "Battle Plan", every New Foundations one has a power under
   `gameplay/powers/campaign` except seven: Glamor, Relic Hunter, Obsidian
   Cage, Spiral Castle / Spiral Dungeons, Martial Culture and the Grand
   Scepter's plan (plus the Scepter Bearer legacy). Two are Raid-targeting
   extensions, two are Empire-only. The roadmap's wording ("add the
   remaining attacker, defender and bandit battle plan families") predates
   catalog batches 6 to 8 and overstates what is left.
3. **No non-deterministic loss choice is left in the Campaign's own
   rules.** The one real choice, how many of the attacker's dead come from a
   queued origin's site, is asked (`CampaignBattle.originSplit`). The
   remaining choices live in the unimplemented cards (Glamor's secrets,
   Relic Hunter's relics).
4. **The Raid, victory, defeat and At End windows are bound.** Every
   Campaign window after the result has at least one power on it; the
   unbound rules are the seven cards above and two edifices' targeting
   restrictions (Hall of Debate, Tome Guardians).
5. **The first-game gates stood for Empire games, altered Foundations and
   legacy powers**, none of which a game can reach today: every lineage is
   seated as `Role.Exile`, the only Foundation profile is `FixedUnaltered`,
   and a first game's legacies are facedown and dormant. Dropping the gates
   changed nothing observable; what they guarded now needs one check at
   game start, not a gate in each action.
6. **`WalkerDecisionProjector.waiting`'s second `parkedDecide` can pass
   `probing = false`**, with one caveat about a pass-when-empty choose-one
   whose every option the look-ahead hides.

## 1. `PowerCtx.procedure` at an action's start

### What `procedure` means

`PowerCtx.procedure`
(`src/main/scala/oathdigital/gameplay/powerresolver/ContributingPower.scala:23-28`)
is documented as "the procedure the window is walked for, when it is
known ... `None` for the command that starts a procedure, which has not
recorded one yet." The doc describes the defect as intended behaviour.

### The two paths

**Start.** `OathRulesWalker.startNow`
(`src/main/scala/oathdigital/gameplay/OathRulesWalker.scala:56-73`) refuses
when `walkerProcedure.nonEmpty` (46-48), builds the tree, then:

- `checkRestrictions(tree, powers, ready, actor)` (153-157) calls
  `ProcedureWalker.restrictionViolations`, which is
  `WalkerPowerGather.restrictionViolations` → `attributed`
  (`src/main/scala/oathdigital/gameplay/walker/WalkerPowerGather.scala:295-313`).
  Its `ctxFor` builds `PowerCtx(state, actor, power.source, window, path,
  operation, state.game.current.walkerProcedure, answered)` (312-313), and
  the `Branch` and `AutomaticUse` descent (326-334) passes
  `state.game.current.walkerProcedure` again. At a start that is `None`.
- `ProcedureWalker.advance(ready, tree, None, powers, walkerDice)`
  (`src/main/scala/oathdigital/gameplay/walker/ProcedureWalker.scala:78-93`)
  builds `WalkCtx(..., state.game.current.walkerProcedure, action)` (91-93),
  again `None`. Every window fold then passes `ctx.procedure`:
  `walkFolded` → `applyWindow` (325-330, `WalkerPowerGather.scala:54-67`),
  and the look-ahead: `narrowAt`'s `lookAheadNoted` (508-510), `asksAt`
  (545-548), `verdict`'s `breaches` (573-581) and `SearchMemo.Base` (589-590).
  `breaches`, `waivableBreaches` and `lookAheadNoted` write the value onto
  the state they probe (`WalkerPowerGather.scala:396-397, 417-418, 440-441`),
  so at a start they write `None` there too.
- `startTriggered` (96-116) and `startQueued` → `startNow` (79-87) are the
  same. A queued start does set `walkerEntry`
  (`QueuedStart.startingFrom`, `QueuedStart.scala:99-102`), which is how
  Toll Roads learns the procedure (`TollRoads.scala:98`,
  `entry.procedure == ActionRef.Travel`) without `PowerCtx.procedure`.

**Every later command.** `walkerResumeContext` (289-309) reads
`walkerProcedure`, which `WalkerReplay` set when it applied the
`WalkerParked` fact (`WalkerReplay.scala:139-141`), and `resolve`/`roll`
(`ProcedureWalker.scala:112-121, 138-147`) build `WalkCtx` from the same
field, now `Some`. `resolveWalker`'s `checkRestrictions` after the resolve
(167-176) runs on `ready` with `Some`.

**Previews disagree with themselves too.**

- `ModifierOffers.offerable` asks `power.applicable(PowerCtx(..., Some
  (procedure)))` (`ModifierOffers.scala:24-30`) to list the offerable
  modifiers, while `ModifierChoice.option` judges the same power's option
  with `PowerCtx(pass.state, actor, power.source, modifierWindow,
  Vector.empty, this, None, pass.answered)` (`ModifierChoice.scala:67-68`),
  an explicit `None`. A modifier whose `applicable` read the procedure
  would be offered by one and refused by the other.
- `WalkerSimulation.run`/`applies`/`refusal`/`started`
  (`WalkerSimulation.scala:57-125`) and `requirePlayableOption`
  (`OathRulesWalker.scala:339-356`) preview on whatever the state holds:
  `None` at a start, `Some` inside a walk.

### Who reads it today

- `WelcomingParty.appliesAt` (`gameplay/powers/cardplay/WelcomingParty.scala:42-44`):
  `ctx.procedure.contains(ActionRef.Search)` at `ActionCardPlayedFaceup`.
  Safe today only because Search parks on `search.source` before any card
  is drawn or played (`SearchProcedure.scala:175-184`), so the play runs on
  a resume. A Search started by a queued entry whose start arguments
  answered the source would run the play in the start command and
  Welcoming Party would stay silent. `WelcomingPartySuite` pins
  `procedure = None` as not applicable (`WelcomingPartySuite.scala:111`),
  which is the right verdict for a `PlayFacedownAdviser` but also pins the
  start-command hole.
- `SearchMemo.Base` keys verdicts on `procedure` (`SearchMemo.scala:40-52`),
  so a verdict memoised during the start walk is missed by the first
  resume's probe. A cost, not a defect.
- No `Restriction` reads it yet, which is why the roadmap says to make the
  paths agree "before a Restriction reads the procedure".

### What would make them agree

The start knows the procedure: `startNow` and `startTriggered` take it as a
parameter. Two shapes, either one session:

- **Thread a parameter.** Add `procedure: Option[ProcedureRef]` to
  `ProcedureWalker.advance` and `restrictionViolations` (and
  `WalkerPowerGather.restrictionViolations`), used in place of
  `state.game.current.walkerProcedure` at `ProcedureWalker.scala:93` and
  `WalkerPowerGather.scala:313, 328, 334`; `startNow`/`startTriggered`
  pass `Some(procedure)`, the resume paths pass what they read. Give
  `WalkerSimulation.run`/`applies`/`refusal`/`started` the same parameter
  (their callers, `OfferHost` dry runs and `ModifierChoice`, know the
  procedure or can take it through `ModifierChoice.loop`,
  `ModifierChoice.scala:212-216`), and make `ModifierChoice.option` pass
  `Some(procedure)` as `ModifierOffers.offerable` does.
- **Write it onto a copy of the state**, as `breaches` does
  (`WalkerPowerGather.scala:417-418`): `startNow` runs `checkRestrictions`
  and `advance` on `ready.updateCurrent(_.copy(walkerProcedure =
  Some(procedure)))`. Cheaper, but the field doubles as the "a walk is
  open" flag (`startWalker` 46-48, `PhasePowerProcedure.scala:92-93`,
  `GameApplicationService.scala:460`), `ProcedureWalker.strip` clears it on
  the base state (267-269) and `finish` clears it again (271-276), so the
  copy must not leak into the returned state. It would not: `advance`
  reads the field before `strip`, and `walkerTransition` evolves the
  original `state`. Still, a parameter says what it means.

Either way: update the `PowerCtx.procedure` doc, and pin it with a test in
`ProcedureWalkerWindowSuite` style: a test `ContributingPower` whose
`Restriction` records `ctx.procedure` at `CampaignCost` (the first window
a start folds), started through `OathRulesWalker.startWalker` and resumed
once; assert both recordings are `Some(ActionRef.Campaign)`. A second
assertion for `ModifierChoice.option` versus `ModifierOffers.offerable` on
a test modifier whose `applicable` reads the procedure.

## 2. Battle plan families: rules versus implementation

### What the rules define

- A battle plan is "a type of Campaign modifier whose icon is surrounded by
  a crown"; "Red, blue, and gradient power boxes show that the battle plan is
  used by the attacker, defender, or either" (`rules.yml:991-998`, "Battle
  Plans"). It must be ruled to be accessed (994-995), is used once per
  Campaign even if full (1000-1001), and many change the dice pool
  (1003-1004).
- Campaign step "Use battle plans" (740-746): the attacker uses theirs, once
  each, in any order; then the defender. **Play Facedown Plans**: a facedown
  battle plan may be played faceup to use it, not a site-only plan.
  **Bandits** use plans they rule that cost no favor or secrets. **Empty
  Attack Pool**: a plan that would remove an attack die from an empty pool
  adds a defense die instead.
- The Oathkeeper title is a defensive plan: +1 defense die, +2 on its
  Usurper side (1006-1007; 506-507).
- "Use remaining battle plans" (794-795): plans resolved "At End", "If
  you're victorious" or "If you're defeated" resolve last.
- Empire only: Imperial Tactics (1237) lets an Imperial use other Imperials'
  plans with consent and split costs; Imperial Infighting (1240) forbids
  the same plan to both sides; the Grand Scepter's plan exiles a Citizen
  (1214).

### What is implemented

`BattlePlan` (`gameplay/powers/campaign/BattlePlan.scala:1-45`) models a
plan's offer, its `later` and `wrapping` windows, its `restrictions` and
`rewrites`, on the two windows `CampaignAttackerBattlePlans` and
`CampaignDefenderBattlePlans` (`model/PowerWindow.scala:78-81`).
`CampaignPlanSteps` (`gameplay/actions/campaign/CampaignPlanSteps.scala`)
runs the attacker's `Repeat` of `CampaignPlanChoice` passes, then the
defender's, and a bandit defender applies every cost-free plan it is
offered without asking (`CampaignPlanChoice.scala:56-70`). A facedown
adviser plan is revealed when used (`CampaignPlanApplication.scala:19,
97-101`). The title's plan is `TitleDefensePlan`. The registered families
are `BattlePlans`, `SimplePlans`, `PlanRules` and `PlanRewrites`
(`BattlePlans.scala`, `SimplePlans.scala`, `PlanRules.scala:1-40`,
`PlanRewrites.scala`), 70 or so cards between them.

Against the card data (`cards/content/card-data/oath/en-US/*.yml`, tag
"Battle Plan": 85 cards, of which the New Foundations set excludes the
"Base Game Only" ones), the New Foundations plans without a power are:

| Card | Where it waits | Rule it needs | Window |
| --- | --- | --- | --- |
| Glamor (207) | `catalog/holding/HoldingArcaneDenizens.scala:17-23` | Raid may also target the enemy's secrets except their last; +1 defense die each; taken on victory | `CampaignTargetSelection` (new target kind), `CampaignGatherPools`, `CampaignRaidTransfer` |
| Relic Hunter (126) | `HoldingOrderDenizens.scala:25-31` | Conquest may target facedown relics at targeted sites, +1 defense die each; taken relics may go to the relic deck's bottom | `CampaignTargetSelection`, `CampaignGatherPools`, `CampaignLosses`/`CampaignPlacement` (a Conquest has no transfer step today) |
| Obsidian Cage (R18) | `gameplay/powers/campaign/ObsidianCage.scala`, `gameplay/powers/action/ObsidianCageAction.scala` | on victory, the enemy's unkilled force moves onto the card; an Action returns them | `CampaignLosses`; a new card location for warbands |
| Spiral Castle / Spiral Dungeons (E23) | `HoldingEdifices.scala:238-250` | Raid victory: defender's pawn placed at this site and loses 2 Supply; ruined: attacker's pawn, bandits use it | `CampaignRaidRelocation` (replace the decision), `CampaignActionEligibility` for the attacker's pawn |
| Martial Culture (10) | `HoldingOrderDenizens.scala:10-16` | an Exile who defeats another Exile may become a Citizen | Empire: no Citizen can exist yet |
| The Grand Scepter's plan | `gameplay/operations/GrandScepter.scala:11-14` | an Imperial victorious against a Citizen exiles them | Empire |
| Scepter Bearer (L09) | `HoldingLegacies.scala:75-93` | Imperial-only battle plan | Empire, legacy powers |

Two rules of the step itself deserve a check rather than a card:

- **Empty Attack Pool.** The campaign walker design cites the rule for a
  zero-force Campaign being legal (`2026-09-19-campaign-walker-design.md:81`),
  but no code converts a removed attack die into an added defense die when
  the attack pool is empty: `grep -i "defense die instead"` finds nothing
  under `gameplay/powers/campaign`. A plan with "−attack die" against an
  empty pool should add a defense die. Verify in `PlanDice` and, if absent,
  it is a one-session rule.
- **Imperial Tactics and Infighting** are Empire rules; nothing to do until
  Citizens exist.

The bandit family is complete as far as the rules go: bandits use
cost-free plans they rule (`CampaignPlanChoice.banditPlans`), and the one
deterministic bandit plan the design named (Watchdog) is in `BattlePlans`.

### Relic Hunter: rulings made while implementing

Relic Hunter (126) is implemented in
`gameplay/powers/campaign/RelicHunter.scala`, under the owner's rulings of
2026-11-03 (Conquest only; facedown relics at the targeted sites, a defense
die each; taken on victory; any may go to the bottom of the relic deck).
What the card and the rulings left open was decided as follows, for the
owner to confirm:

- **When the relics are chosen.** As the Glamor ruling has it: the plan is
  used in the attacker's plan window like any plan, and its user then
  chooses which relics to target. The question is the plan's amount
  (`PlanAmount`), a `ChooseMany` of `SiteRelicSlot`s, so no option
  discloses a facedown relic. Zero is a legal answer, and adds nothing.
- **Which relics.** Every facedown relic at a site the Conquest targets,
  the origin included (FAQ: a relic is targeted only with its site). A
  relic a restriction would not let the attacker take (Ancient Bloodline)
  is shown unavailable with its reason. With no relic to target the plan
  is shown unavailable: "No facedown relic at the targeted sites".
- **Where the take happens.** In the Conquest's placement window, after
  the survivors are placed. The window now stands on every Conquest
  victory, even with no survivor (`CampaignConquest`), so the take always
  has its place. It comes before every "At End" plan, so Salt the Earth
  buries the site after the relics are gone.
- **How the bottom choice is made.** The attacker peeks at the targeted
  relics, then answers one `ChooseMany` for those to put on the bottom of
  the relic deck, then every targeted relic is taken, facedown as it lay,
  and the chosen ones are moved to the bottom. "Any relics you take" means
  the relics this plan took; nothing else is taken in a Conquest.
- **An untaken relic.** A defeated attacker takes nothing, and the targeted
  relics stay at their sites.
- **Lines.** "Targeted 2 facedown relics for 2 defense dice." when the
  relics are chosen; "Took {relics}." and "Put {relics} on the bottom of
  the relic deck." when they are taken. The peeks keep their generic
  "Peeked at" lines. The other seats see the relics' backs (golden
  `relic-hunter`).

### Glamor: rulings made while implementing

Glamor (207) is implemented in `gameplay/powers/campaign/Glamor.scala`,
under the owner's ruling of 2026-11-03 (chosen in the battle plan step
like any plan; its user then chooses how many secrets to target, and those
join the Campaign's targets). What the card and the ruling left open was
decided as follows, for the owner to confirm:

- **Who and where.** An attacker's plan, in a Raid or a Conquest against a
  player defender. Bandits have no board, so it is never offered against
  them. "Your enemy" is the Campaign's defender.
- **How many.** The plan's amount, a `ChooseAmount` from one to the
  defender's board secrets less one, faceup and facedown counted alike
  (FAQ for Book Burning: "all of their secrets except one, whether face up
  or down"). With one secret or none, the plan is shown unavailable: "The
  defender has no secret but their last". Each targeted secret adds one
  defense die.
- **Where the take happens.** In a Raid, in the transfer window, after the
  printed takes and consequences and before the pawn's relocation. In a
  Conquest, in the placement window, as Relic Hunter's take.
- **Which secrets, which side.** Faceup secrets are taken first, as Sleight
  of Hand and Witch's Bargain take them, so the defender keeps a facedown
  one when they have one. Each taken secret keeps its side (`Take` with a
  `sourceSide`): a facedown secret arrives facedown on the attacker's
  board and flips at their cleanup.
- **A board that shrank.** The take is cut to leave the defender one
  secret, should their board have lost any since the targeting (a burnt
  secret cost, say). A defense plan's paid secrets flip facedown and stay
  on the board, so they still count.
- **Defeat.** A defeated attacker takes nothing.
- **Lines.** "Targeted 2 secrets for 2 defense dice." when chosen, and
  "Took 2 secrets from Blue." when taken, parallel to the Raid's "Took …
  from Blue" (golden `glamor`).

### Obsidian Cage: rulings made while implementing

Obsidian Cage's battle plan (R18) is implemented in
`gameplay/powers/campaign/ObsidianCage.scala`, under the owner's ruling of
2026-11-03 (bandit warbands are always killed, so none reach the Cage).
What the card, the FAQ and the ruling left open was decided as follows;
the owner confirmed it on 2026-11-03, with the changes noted:

- **Side.** Either side: the card's braid is printed red into blue, and
  "your enemy's force" reads for both. Its holder uses it from the faceup
  relic, for free. Against bandits it is shown unavailable: "Bandit
  warbands are always killed, so none reach the Cage".
- **The enemy's unkilled force.** The force as the losses left it, read as
  Shield Wall and Sticky Fire read the force they kill (`KillWholeForce`,
  ruling R4): a Conquest defender's warbands the victory returned to its
  board (not the board it held before), a Raid defender's whole board, and
  a defeated attacker's whole board with its warbands at a queued origin.
- **Timing.** In the losses window, after the losses, so a Conquest's
  placement and a Raid's transfer follow it.
- **Hospital (owner's ruling).** A warband the enemy's Hospital saves
  still survives in the force, so it ends on the Cage, not on Hospital's
  site: the Cage moves Hospital's count from the supply to the card and
  clears it (`Hospital.divert`), so Hospital places nothing at the end.
  Cursed Cauldron counts those warbands as not dead
  (`ObsidianCage.fromHospital`). Both are pinned by tests.
- **The location.** Warbands on a card are kept by card and force in
  `CurrentGameState.warbandsOnCards`, so they stay on the Cage wherever it
  goes and keep an Exile's colour. They count as out of their bank
  (`PlayerFacts.banked`, the operation guard's bank count). The table shows
  them on the held relic's details, one entry per force, in its owner's
  colour (`CardDetailsProjection.warbands`, empty when absent on the wire,
  as `powers` is).
- **Lines.** "Moved 2 of Blue's warbands onto Obsidian Cage.", or "Moved 2
  warbands onto Obsidian Cage." when they are the actor's (golden
  `obsidian-cage`).
- **The Action.** Free, used by the Cage's holder
  (`gameplay/powers/action/ObsidianCageAction.scala`). "Any board of the
  same color" is every board whose warbands are of a caged force: an
  Exile's lineage player's, the Chancellor's and Citizens' for Imperial
  ones. One board asks how many, from one; several ask a distribution of at
  least one. It is offered only while a caged force has a board. Lines:
  "Returned 2 warbands from Obsidian Cage to Blue." and "Took back 1
  warband from Obsidian Cage." for the user's own.
- **The return (FAQ).** No in-game power returns a held relic to the relic
  deck; the Chronicle's Throne does, at the game's end, so before the
  Reliquary is stocked every warband on a card goes back to its supply,
  with nothing asked (owner's ruling: no board takes them).
The roadmap's separate item "Record a bandit's applied battle plan as an
event" covers how that use is journaled.

## 3. Non-deterministic loss choices

The rules: "Kill half of the warbands in the defeated player's force,
rounding down. Move the remaining warbands in their force to their player
board. If you defeated the bandits, kill all bandit warbands at the targeted
sites" (`rules.yml:768-772`). Skulls kill "a warband on their player board"
(753); the sacrifice kills warbands "on their player board" (755-756).

| Loss | Choice in the rules | Implementation |
| --- | --- | --- |
| Attacker's skull kills | none when the force is the board; when a queued origin's site warbands joined the force (Sneak Attack, Captains), which die | `CampaignBattle.originSplit` asks a `ChooseAmount` when the split is a real choice (`CampaignBattle.scala:183-196`), at the skull, sacrifice and defeat windows (`earlyKill` 229-237, `originDeathsStep` 239-243) |
| Attacker's defeat half | same | same |
| Conquest defender | which half dies | none needed: every target warband dies and half return from the bank to the board (`conquestLosses` 268-287), which the rules' "kill half, move the rest to the board" also reaches |
| Raid defender | which half of the board | none: warbands are interchangeable (`raidBoardLosses` 289-298) |
| Bandits | none | all targeted die |
| Conquest placement | how many survivors, where | `Distribute`/`ChooseAmount` (`CampaignConquest.scala:18-26`) |
| Raid relocation | which site | `ChooseOne` (`CampaignRaid.scala:23-27`) |
| Raid consequences | none: all facedown advisers and relics, half the favor | deterministic (`CampaignRaid.consequences`) |
| People's Favor's returned favor | the rule fixes the order (fewest bank, then leftmost) | `BannerRules.raidFavorReturn` |

Nothing is left in the core rules. The non-deterministic choices that
remain are the cards': Glamor (which secrets), Relic Hunter (which relics,
and whether to bury the taken ones), Obsidian Cage's Action (how many
warbands return). The design's phrase "where several legal assignments
matter" is satisfied for the Campaign itself.

## 4. The Raid, victory, defeat and At End windows

The windows after the result (`model/PowerWindow.scala:87-125`) and what
is bound to each (`grep -l PowerWindow.<name>` under `gameplay/powers`):

| Window | Bound today | Rules still unbound |
| --- | --- | --- |
| `CampaignAfterOutcome` | `VictoriousNow` (Peace Envoy, Hearts and Minds) | none |
| `CampaignLosses` | Billowing Fog, Traveling Doctor (`SparedForce`), Sticky Fire, Shield Wall (`KillWholeForce`), Hospital, Cursed Cauldron, Peace Envoy, Obsidian Cage | none |
| `CampaignSkullLosses`, `CampaignSacrificeLosses` | Hospital, Cursed Cauldron | none |
| `CampaignPlacement` | Vow of Wandering, Relic Hunter (its take) | none |
| `CampaignRaidTransfer` | the `BattlePlan` trait, Glamor (its take) | none |
| `CampaignRaidConsequences` | Keep | none |
| `CampaignRaidRelocation` | Keep | Spiral Castle |
| `CampaignActionEligibility` (the root, "At End") | Battle Honors, Book Burning, Careful Plans, `DiscardedAtEnd` (Pledge of Defense, Rain Boots, Rival Khan, Great Crusade, Mounted Patrol, Lancers, Horse Archers, Storm Caller, Warning Signals), Field Promotion, Herald, Hospital, Mercenaries, Military Parade, Salt the Earth, Second Wind, Sneak Attack, Tribute Spoils, Vow of Peace, Wild Mounts, Diplomat, the fortress rules | Spiral Dungeons (attacker's pawn and Supply), Martial Culture, Grand Scepter |
| `CampaignTargetSelection` | Circlet of Command, Forgotten Vault, Lost Tongue, the Pass | Hall of Debate and Tome Guardians (`HoldingEdifices.scala:8-16, 190-198`: the ruler's enemies cannot target the banner they hold), none for Glamor or Relic Hunter, which ask their targets as the plan's own question |

The Raid step order (takes, then consequences in their own window, then
relocation; `CampaignRaid.scala:11-30`) matches the rules' four Raid
sub-steps (`rules.yml:779-791`). The "Use remaining battle plans" step is
the root window: `DiscardedAtEnd.later` registers its discard there
(`DiscardedAtEnd.scala:30-34`), and `BattlePlan`'s doc says a plan that
must run last registers at the root.

## 5. The first-game gates

### What was dropped, and where it was decided

The legacy command handlers refused any state that was not an all-Exile
table on the fixed unaltered Foundation profile with no active legacy. The
walker ports dropped those checks action by action: economy
(`2026-09-18-economy-walker-design.md:9, 33`), Negotiation
(`2026-09-19-negotiation-walker-design.md:20`), Challenge
(`2026-09-19-challenge-walker-design.md:16`), Campaign
(`2026-09-19-campaign-walker-design.md:23`). The Campaign design deferred
"the first-game rule audit behind the dropped gates". This section is that
audit.

### What each gate stood for in the rules

- **Exile-only roles.** The first game seats everyone as an Exile
  (`rules.yml:196`, "Takes a player board on its Exile side"; 107, "In
  your first game, the players take on the role of Exiles"). Later games
  seat a Chancellor and possibly Citizens (212-234). The rules that then
  apply and the engine lacks: purple warbands and the shared Imperial bank
  (`PlayerForceKind` handles the kind, `model/PlayerForceKind.scala:8-12`,
  but no setup seats one), Imperial Tactics and Infighting (1237-1240),
  the Grand Scepter's three powers, the Usurper flip "only in the fourth
  round and later" being the no-Empire case (499-501; the engine's
  Usurper Limiter is first-game only), Citizen victory and Martial
  Culture.
- **Unaltered Foundations.** The six Foundations and their altered faces
  (`rules.yml:282-370`) are setup rules: they change what is dealt and
  placed. Two altered faces reach play: the Grand Council side of the
  People's Favor and the Festival side of the Darkest Secret, which are
  banner powers in play (card data tags them Campaign and Recover) and
  Festival is "a persistent power that is not on a card" (1009-1010).
  `FirstGameFoundationProfile` has the one value `FixedUnaltered`
  (`model/Setup.scala:24`) and `GameStateProtocol.scala:55-61` defaults
  to it.
- **Inactive legacies.** "Active legacies give you special powers in later
  games" (74); a first game keeps one legacy facedown (205), and a
  facedown legacy is dormant, its power does not apply
  (`glossary.yaml`, `dormant_legacy`). `LegacyState(id, active)` exists
  (`model/Cards.scala:42`) and `RuleSourceIndex` lists lineages' legacies
  (`RuleSourceIndex.scala:79-81`), but no legacy power is implemented
  (`catalog/holding/HoldingLegacies.scala` holds all 36).

### What the engine does today

`GameStartRules` seats every participant as `Role.Exile`
(`gameplay/setup/GameStartRules.scala:36`). `StateBasedEvaluation
.supported` still refuses a non-Exile lineage for the Oathkeeper
evaluation (`StateBasedEvaluation.scala:166-171`), the one gate that
survived. The economy design records that neither removed gate can be
exercised: "no Empire game exists yet, so no Citizen or Chancellor player
can be set up, and `FixedUnaltered` is the only profile"
(`2026-09-18-economy-walker-design.md:74`).

### Verdict

Dropping the per-action gates changed no reachable behaviour. The rules
they stood for are whole features (Empire roles, altered banner sides,
legacy powers), and the right guard for them is one refusal at game start
on a Chronicle that seats an Imperial, names an altered Foundation or an
active legacy, until each lands. The leftover `StateBasedEvaluation
.supported` gate is then redundant too, or the one place the check lives.

## 6. `WalkerDecisionProjector.waiting`'s second `parkedDecide`

`waiting`
(`src/main/scala/oathdigital/application/WalkerDecisionProjector.scala:117-137`)
first calls `this.parked(..., previewed = false)` as a gate, which runs
`ProcedureWalker.parkedDecide(ready, tree, pending, powers)` with the
look-ahead on (171) and `queryProjection` on the narrowed query; then it
calls `parkedDecide` again (125-126), also with the look-ahead on, and
reads only `query.heading`, a `Negotiate` query for the deal projection,
and `decisionId` for `status`.

What the look-ahead changes: `WalkerSearch.narrow`
(`gameplay/walker/WalkerSearch.scala:34-66`) narrows `ChooseOne` and
`ChooseMany` options, `ChooseAmount` bounds and checks `Partition`
splits; `Negotiate` and the rest fall through unchanged (66). It never
touches `decisionId` or the owner. So the second call can pass
`probing = false`, as `awaitedPlayer`, `ChoiceWords` and `ModifierChoice
.explained` do (`ProcedureWalker.scala:224`, `WalkerDecisionProjector
.scala:90-91`, `ModifierChoice.scala:176-177`).

One caveat. `ProcedureWalker.narrowAt` (`ProcedureWalker.scala:484-519`)
replaces a pass-when-empty choose-one with its Continue screen when the
look-ahead leaves nothing (`EmptyChoices.nothingLeft`/`offered`), and
`EmptyChoices.headingFor` (`EmptyChoices.scala:29-35`) rewrites the
heading to "{power}: nothing to ...". Without probing, that happens only
when the declared decision already has no choosable option (486-489). So a
waiting viewer would see the declared heading where the owner sees the
Continue heading, in exactly that case. The saving is the `leafAt` rebuild
and fold; the verdicts themselves are mostly `SearchMemo` hits, since the
gate just computed them on the same `powers` instance. Either accept the
caveat (the status line names the player and the question; a Continue
screen is momentary) or keep the probe and drop the duplicate call by
returning the `Decide` from `parked`.

## Questions for the owner

1. Section 1: parameter or state copy? Recommended default: the parameter,
   because `walkerProcedure` is also the "a walk is open" flag and a copy
   that carries it invites a leak.
2. Section 2: are Glamor and Relic Hunter wanted before Empire games? They
   add a target kind (secrets, facedown relics) to the Raid and Conquest
   target decisions and a take step the Conquest lacks. Recommended default:
   yes, as one slice each; Obsidian Cage and Spiral Castle after them;
   Martial Culture, the Scepter's plan and Scepter Bearer wait for Empire.
3. Section 2: is the Empty Attack Pool rule in force anywhere? If not, is
   it a rule of `PlanDice` (every "−attack die" plan) or of each plan?
   Recommended default: `PlanDice`, once.
4. Section 5: should a Chronicle that seats an Imperial, an altered
   Foundation or an active legacy be refused at game start until those
   features land, and `StateBasedEvaluation.supported` be folded into that
   check? Recommended default: yes.
5. Section 6: accept the Continue-heading caveat for the waiting line?
   Recommended default: yes, and pin it with a test that a waiting viewer's
   heading matches the owner's for an ordinary parked choice.

## Recommended next slices

Each is one session.

1. **One procedure on every `PowerCtx`.** Thread `procedure` through
   `ProcedureWalker.advance`, `restrictionViolations`,
   `WalkerSimulation.*` and `ModifierChoice.option`; update the doc at
   `ContributingPower.scala:23-28`; add the start-versus-resume test and
   the offer-versus-option test. No behaviour change for any shipped power
   except Welcoming Party on a start-command play, which gains its favor.
2. **`waiting` skips the look-ahead.** Pass `probing = false` at
   `WalkerDecisionProjector.scala:125`, or return the `Decide` from
   `parked`; add the heading test from question 5.
3. **Empty Attack Pool.** Verify, then implement in `PlanDice` with a test
   for a "−attack die" plan against a zero-force Campaign.
4. **Supported-Chronicle check at game start**, replacing
   `StateBasedEvaluation.supported`; a test per refused shape.
5. **Glamor**, then **Relic Hunter**: a new target kind in
   `CampaignTargets`, dice in `CampaignGatherPools`, the take at
   `CampaignRaidTransfer` (Glamor) or a new Conquest take step (Relic
   Hunter), each with its own slice and golden log.
6. **Spiral Castle / Spiral Dungeons**, then **Obsidian Cage** (which needs
   a warband location on a card).
7. **Hall of Debate and Tome Guardians**: an `OptionRestriction` at
   `CampaignTargetSelection`, in the shape of `ForgottenVault`.
8. **Refresh the roadmap item** to the list above; today's wording
   describes the state before catalog batches 6 to 8.
