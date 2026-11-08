# Catalog Batch 11: The Engine-Wide Seams

**Status:** designed 2026-11-07. No slice has landed.

**Builds on** [Catalog batch 10](2026-11-03-catalog-batch-10-design.md),
[batch 9](2026-11-02-catalog-batch-9-design.md) and
[batch 8](2026-10-12-catalog-batch-8-design.md), whose vocabulary, cost
rules, walker shapes and log-line rules apply unchanged. The batch 4
rulings that apply to a whole batch (restriction icons, site capacity,
"your site" and "your region", turn order) apply here too.

## Goal and scope

Batch 10 opened every minor or moderate seam. The seven faces it deferred
each need a seam that cuts across the engine, and they are the last cards
outside the Negotiation and Chronicle phases. This batch opens those seams
and implements the seven faces, which empties the roadmap's **Phase -
Catalog batch - engine-blocked**.

Every seam was re-inventoried at `68967858` before this design; the file
and function names below come from that inventory.

| Slice | Seam | Cards |
|---|---|---|
| 0 | Room in `CoreOperations.scala` | none |
| 1 | Acting as if ruling bandit denizens | Bandit Crown (R11) |
| 2 | Card-level ruler | Grand Mask (R05) |
| 3 | Another player's advisers for one Trade | Master of Disguise (78) |
| 4 | A question before a batch | Wild Pastures (E07i) |
| 5 | Favor redirection | Vow of Kinship (152) |
| 6 | Several revealed Visions | Desecrated Ground (E08r) |
| 7 | A claimed Vision and an off-turn reveal | False Prophet (85) |

Slices are ordered so that each seam exists before the slice that reuses
it: slice 5 asks its favor-source question through slice 4's seam, and
slice 7 reads slice 6's revealed-Visions set. Slices 4 to 7 touch the
procedure walker or the card-play path and run those suites in full before
landing.

### Out of this batch

Negotiation (The Tribunal, Bandit Tribunal, Festival District, The
Gathering, Whispering Stone, Council Arbiter, Deed Writer, Traveling
Negotiator) and the Chronicle steps (the 36 legacies, Ancient Writ, Stone
Baths, Ruined Baths, Amber Doors, Lost Doors, Blasted Pastures, Ruined
Temple, Hall of Bandits, Hiding Place, Reliquary Raid, Enduring) stay with
their phases.

### Catalog corrections

- **Master of Disguise** carries the Trade action-modifier icon on its
  image, which the card database's text omits. Slice 3 adds
  `modifier = Some(ModifierMark.Action(Trade))` when it moves the card out
  of holding. Its cost is 1 secret and its restriction icon is adviser-only,
  as held today.
- **The Bandit Crown FAQ** in the card database answers the base-game text
  ("count bandits as your warbands", "you rule empty sites", the
  last-warband minor action, the Chronicle's empty sites). The New
  Foundations card says something else, and none of those answers apply.

## Rulings

Taken with the owner on 2026-11-07.

1. **Bandit Crown: the bandits still rule.** Denizens at a bandit site stay
   ruled by the bandits. The Crown's holder acts as if they ruled them, and
   no other reading of "ruler" changes.
2. **Bandit Crown: using the denizens.** The holder may use the powers of
   denizens (edifices included) at bandit sites, from the phase menus and as
   selected modifiers, as a ruler would.
3. **Bandit Crown: battle plans.** The holder may use the battle plans of
   denizens at bandit sites in any Campaign, as attacker or defender. The
   bandits keep them too: a bandit defender still applies them, even
   against the holder. This is the only case in which one battle plan is
   used twice in one Campaign.
4. **Bandit Crown: tolls stay the bandits'.** A toll owed to the ruler of a
   bandit-ruled card (Curfew, Forced Labor) is still paid to the bandits,
   which burns it, and the holder pays it too.
5. **Bandit Crown: consent.** Rule 9.6.1 lets a ruler consent to enemies
   ignoring a persistent power the ruler rules, except in Campaign actions;
   the bandits never consent. The holder may give that consent for every
   waivable power of a denizen at a bandit site, as if they ruled it, but
   never to themselves.
6. **Bandit Crown: Bandit Forest** still binds an Imperial holder: they may
   not use those denizens' non-persistent powers or place favor or secrets
   on them. By ruling 5 they may waive it for their enemies.
7. **Grand Mask makes the holder the ruler of the cards.** While the holder
   is an Exile, during their own turn, they rule every denizen (edifices
   included) at an Imperial site except battle plans, for every player and
   every rule: they use those powers, tolls are paid to them, ruler bans
   treat their enemies as the enemies, and "cards you rule" counts include
   them. Imperials rule none of those cards during that turn. Battle-plan
   denizens stay the Empire's. The sites themselves stay Imperial (the
   card's FAQ): Supremacy, Campaign targets and defenders, the Chronicle and
   Imperial Seal read the site and do not change.
8. **Master of Disguise replaces every adviser for the Trade.** The holder
   chooses another player. For that Trade the chosen player's advisers are
   the holder's in every respect: their suits count, their Trade modifiers
   are offered (the holder pays their costs), and their persistent powers,
   vows and pledges bind or help the holder as if the holder held them. The
   holder's own advisers, Master of Disguise included, do nothing for that
   Trade. Master of Disguise is offered only as the Trade's first modifier
   pick (FAQ: no other modifier of yours first).
9. **Vow of Kinship: only spends draw on the Nomad bank.** Favor going to
   the holder's board goes to the Nomad bank instead (FAQ: a gift too).
   Favor the holder chooses to spend may come from the Nomad bank: costs
   they pay, favor they give (tolls, Peace Envoy, Witch's Bargain) and favor
   they choose to burn. Anything done to the holder (a take such as Royal
   Tax, the losing defender's half-favor burn, banishment) reads their board
   only.
10. **Vow of Kinship: the player chooses the source.** When both the
    holder's board and the Nomad bank hold favor, the holder chooses how
    much of a spend comes from each.
11. **Wild Pastures** applies only to powers its ruler uses, at 1 favor per
    Nomad card (batch 10, ruling 3). Its ruler is the card-level ruler of
    slice 2, so a Grand Mask holder may use it during their turn.
12. **Wild Pastures and Vow of Kinship.** A Kinship holder who rules Wild
    Pastures may pay its favor out of the Nomad bank into the Nomad bank.
    The payment needs 1 spendable favor per card and moves nothing, and the
    log still says they paid instead of discarding.
13. **Desecrated Ground is implemented.** E08 never appears in a first
    game, but Build Edifice can draw it and the next game's setup ruins it at
    a non-Empire site, so it can appear from the second game on.
14. **False Prophet: "it" is the Vision.** When the Vision under the False
    Prophet's warband is discarded, the warband is killed and its owner
    plays that Vision faceup or discards it, as a Search would. Losing False
    Prophet itself changes nothing (FAQ: the warband does not go away).

### Readings

These follow from the card text and the rulings. The owner reviews them
with this spec.

- **Vow of Kinship's arrival.** When the Vow becomes the holder's faceup
  adviser, all favor on their board moves to the Nomad bank. When it
  leaves, the favor stays in the bank.
- **Vow of Kinship's board.** The holder's board normally holds no favor;
  it can after a Trade under Master of Disguise with a player who holds no
  Kinship. Ruling 10's question is asked only when both sources hold favor
  and the spend could take from either.
- **Master of Disguise's chosen player** may be any other player. Their
  facedown advisers count as they would for them. The Trade's other
  modifier picks come from their advisers only.
- **Desecrated Ground.** An Exile who reveals a Vision while their pawn is
  at the site keeps every revealed Vision they already have. A reveal
  anywhere else discards all of the player's own revealed Visions. Any one
  of a player's revealed Visions can win. A player who becomes a Citizen
  discards all their own revealed Visions. No card discards one particular
  revealed Vision: Horned Mask and Inquisitor discard facedown Visions.
- **False Prophet's placement.** The warband goes on another player's
  revealed Vision. The When Played does nothing if the holder is not an
  Exile, has no warband in their warband bank, or no other player has a
  revealed Vision.
- **False Prophet's claim.** The Vision stays in its owner's slot and is
  revealed for both players. Its owner's own replacement or role change
  discards it as usual, which triggers ruling 14. A claimant who becomes a
  Citizen keeps the claim but cannot win with it until they are an Exile
  again (FAQ).
- **False Prophet's reveal.** Playing the Vision faceup is the claimant's
  own reveal: it follows the Vision rules, Desecrated Ground included, and
  it runs inside whatever walk discarded the Vision, which may be another
  player's turn.
- **Wild Pastures' "from a power".** A discard is from a power when the
  batch that holds it is built while one of the ruler's power uses runs: a
  phase power, a When Played effect, a selected modifier or a battle plan.
  A Search's own discard, or a card play's discard of a displaced adviser,
  is a rule and is not from a power, unless a power started that Search or
  card play (Search Party).

## Slice 0: Room in CoreOperations

`model/CoreOperations.scala` is 787 lines, 13 under the 800-line cap of
`scripts/check-architecture.py`, and slices 3 and 7 add operations. Scala 3
keeps every direct subtype of a sealed trait in the trait's file, and
`CoreOperation`, `Burn`, `Discard` and `Gain` are all sealed there, so the
cases stay. The code that builds their children moves to a sibling file in
`model`: `discardWorld`, `returnedResources` and the other companion
helpers. The goal is at least 80 free lines. No behaviour changes; the full
suite is the test. The roadmap's "Make room in `CoreOperations.scala`"
item is deleted when the slice lands.

## Slice 1: Acting as if ruling bandit denizens

**Bandit Crown** (rulings 1 to 6).

- **Use.** `PowerAccess.usableSites` (`gameplay/PowerAccess.scala:74`) adds
  every bandit-ruled site for an actor who holds the Crown. That one change
  reaches the phase menus (`accessible`, `PhasePowerProcedure.sourcesFrom`)
  and the selected modifiers (`locate`). `ruledSites` stays as it is, so
  the powers that count ruled sites (Royal Tax, Garrison, Baron and the
  others) never count bandit sites.
- **Battle plans.** `PlanContext.sitesRuled`
  (`gameplay/powers/campaign/PlanContext.scala:45`) also searches the
  bandit sites when the side's player holds the Crown, on either side of
  any Campaign. `CampaignPlanChoice.banditPlans` is unchanged, so a bandit
  defender still applies the same cards. The plan's "used" record must let
  the two sides each use the card once; the slice checks what records a
  plan's use today and widens it by side if it is keyed by card alone.
- **Consent.** Every waiver's granter for a power of a card at a bandit site
  is `None` today. A shared helper, beside `SiteRulers.rulerOfCard`, gives
  the consent granter of a card: its ruling player, or the Crown's holder
  when the bandits rule it. The waivers that read the card's ruler
  (`RulerBans`, `RulerToll`, `PayOrAsk`, Bandit Forest's restriction and
  the others the slice finds through `ConsentPowers`) read their granter
  through it. `OperationConsent` already drops the actor from the granters,
  which gives "never to themselves". Campaign actions stay unwaivable.
- **Unchanged.** Tolls still go to the bandits (`RulerToll` burns), and
  Bandit Forest still refuses an Imperial holder.

Tests: use from the menu and as a modifier; a plan used by the holder as
attacker against bandits while the bandit defender applies the same card;
a plan used by the holder in a player-against-player Campaign; the toll
still burned, holder included; the holder's consent asked for another
player's trade under Curfew and for Bandit Forest; no consent asked of the
holder for themselves; no change without the Crown.

## Slice 2: Card-level ruler

**Grand Mask** (ruling 7).

- **The seam.** A new `CardRule.rulerOf(ready, card)` answers who rules a
  denizen card. It is `SiteRule.rulingPlayer` of the card's site, except
  that during the turn of an Exile who holds the Grand Mask, a
  non-battle-plan denizen at an Imperial site is ruled by that Exile. A
  denizen is a battle plan when its power is a battle plan in the plan
  catalog.
- **Sorting the readers.** Each caller of `SiteRulers.rulerOf` (18) and
  `rulerOfCard` (11), `RuledCards.of` and `count`, and `PowerAccess`'s
  access checks reads either a card or a site. The card readers move to
  `CardRule.rulerOf`; the site readers stay. The slice's plan lists every
  caller with its choice. Site readers that stay include Supremacy,
  `CampaignSetup.defenderAt`, `ChronicleTasks.rules`, `imperialSites`
  (Imperial Seal) and the muster check.
- **Access.** `usableSites` adds the Imperial sites for the Mask's holder
  during their turn, and an Imperial actor loses access to the Mask's cards
  at those sites. Slice 1's consent granter reads the card ruler, so the
  holder is the granter of those cards' waivers during their turn.

Tests: use, toll paid to the holder, a ruler ban turned against the
holder's enemies, a ruled-card count, and Imperials refused, all during the
holder's turn; nothing changes on another player's turn, for a Citizen
holder, for a battle-plan denizen, or for the site's own ruling.

## Slice 3: Another player's advisers for one Trade

**Master of Disguise** (ruling 8).

- **The record.** A new primitive `ActAsIfAdvisers(player)`, a sibling of
  `ActAsIf`, writes walk-scoped scratch (`walkerAsIfAdvisers`) that the walk
  end clears, live and on replay, with a codec entry. A pick answer cannot
  carry it, unlike Drowned Library's (batch 10, ruling 15): the readers
  below see game state only, never a walk's answers.
- **One reader.** `PowerAccess.advisersOf(ready, player)` returns the
  chosen player's advisers for the walk actor while the scratch is set, and
  the player's own otherwise. `EffectiveSuit.count` (the Trade's matching
  suit, Cup of Plenty, Vow of Beastkin), `PowerAccess.locate` and
  `accessible`'s adviser case, `holdsFaceupAdviser` and
  `faceupAdviserHolder` read through it. The slice's plan lists the raw
  `.advisers` reads it leaves alone, with the reason for each.
- **The pick.** Master of Disguise is a Trade modifier that asks which
  player, emits `ActAsIfAdvisers`, and is unavailable once another modifier
  is picked. Its own availability reads the real advisers.

Tests: the chosen player's suits count; their Trade modifier offered and
paid by the holder; the holder's own modifiers no longer offered; the
chosen player's Vow of Poverty binding the holder, and the holder's own
vow lifted; the scratch cleared after the Trade and on replay; Master of
Disguise unavailable after another pick.

## Slice 4: A question before a batch

**Wild Pastures** (rulings 11 and 12).

- **The seam.** Before the walker applies a leaf's batch, it gives the
  registered batch questions a look at the built operations. A question
  that finds something to ask, and has no answer recorded, parks the walk
  on a `Decide`. On resume the leaf rebuilds its batch from the unchanged
  state and applies the answers. The answers are keyed by what they
  answer (a card id), so a rebuilt batch meets the same answers. This is
  the operation-rewrite design's "ask per deferred op" extension point,
  placed at the leaf so that no discard has to be lifted out of its batch.
- **Wild Pastures' question.** For each `Discard.Denizen` of a Nomad card
  whose acting player is Wild Pastures' card-level ruler, in a batch built
  while one of that ruler's power uses runs: "Pay 1 favor to the Nomad bank
  instead?" A yes replaces the discard with a favor payment to the Nomad
  bank and leaves the card where it is; the line reads that they paid
  instead of discarding. A ruler without spendable favor is not asked.
- **Composition.** Wild Mounts' `Defer` keeps its own question; the Wild
  Pastures question is asked when the deferred discard's leaf applies, so
  Wild Mounts asks first.
- **"From a power".** The walker knows which power use is running from its
  walk context; the slice uses that, and adds a cause to the batch only if
  it does not.

Tests: a phase power, a When Played effect, a battle plan and Search
Party's card play each asked; a plain Search's discard and a card play's
displaced adviser not asked; a batch whose later operation reads the freed
slot (Roving Terror) still correct after a yes; the question parked and
answered across a resume and a replay; Wild Mounts and Wild Pastures on
one plan; an unruled or another player's Wild Pastures never asks.

## Slice 5: Favor redirection

**Vow of Kinship** (rulings 9, 10 and 12).

- **Credit.** In `ResourceOperations.adjustFavor`, a favor `Move` into the
  holder's `PlayArea` lands in `FavorBank(Nomad)`. This catches `Gain`,
  `Take` and `Give` however they were built, without loosening their
  ownership `require`s.
- **Debit.** Favor moves made by the holder's own spend (`PayCost`, their
  own `Give`, their own burn) carry their spender; today `PayCost` and
  `Burn` moves carry none, and the slice adds it. Such a move may take from
  the Nomad bank. `favorSourceViolations` and the quantity check follow the
  same rule. A take or burn done to the holder reads the board only.
- **Affordability.** A shared `SpendableFavor.of(ready, player)` replaces
  the direct `board.favor` affordability reads (about 15:
  `PricedOptions`, `ModifierChoice`, `RerollChoice`, `PlanPrice`,
  `PlanAid`, `NegotiationDeal`, `SelfExile`, `TollRoads`, `RulerToll`,
  `PeaceEnvoy`, `WitchsBargain`, `PlanAidDesk`). The reads ruling 9 keeps
  on the board (Royal Tax, Chaos Cult, Charming Friend, Naysayers, Pied
  Piper, Witch's Bargain's take, Defame, the Campaign's defender burn, Vow
  of Poverty) stay. The slice's plan settles `BannerRules.playerResources`.
- **The source question.** When both sources hold favor, slice 4's seam
  asks the holder how much of the spend comes from the Nomad bank.
- **Arrival.** `AdviserArrivalPass` moves the holder's board favor to the
  Nomad bank when the Vow becomes a faceup adviser.
- **Log.** The favor-gain notes compute a gain from the board's change
  (`NoteSupport.favor`); under the Vow they would go silent, so they also
  count favor that went to the Nomad bank for the holder and say so.

Tests: gains, takes and gifts landing in the Nomad bank; a cost, a toll and
a chosen burn paid from it; Royal Tax and the defender's burn reading the
board only; affordability counting the bank; the source question asked only
with both sources; the arrival move; Wild Pastures paid from bank to bank;
Master of Disguise lifting the Vow for one Trade.

## Slice 6: Several revealed Visions

**Desecrated Ground** (ruling 13).

- **The model.** `PlayerState.revealedVision: Option[VisionState]` becomes
  `revealedVisions: Vector[VisionState]`. Its 24 production read and write
  sites move to it: card play (`CardPlay.planVision` and `revealed`,
  `CardSlots`), victory (`VisionVictoryEligibility`, `StateBasedEvaluation`,
  `SwearOath`), the role change, the movement and state writes, `CardIndex`,
  `OperationExecutor`, the projection DTOs and codec, and the frontend
  board.
- **One read path.** `RevealedVisions.of(ready, player)` returns a player's
  revealed Visions. Every reader that asks "which Visions has this player
  revealed" goes through it, so slice 7 can add claimed Visions in one
  place.
- **Desecrated Ground.** A reveal by an Exile whose pawn is at the ruined
  E08 keeps the Visions already revealed: `CardSlots` offers no
  replacement and the slot accepts the new Vision. Elsewhere a reveal
  replaces all of them. A player who becomes a Citizen discards all of
  them.
- **Wire.** The projection's revealed Vision becomes a list; old journals
  need not decode.

Tests: a reveal at the site keeps the old Vision; one elsewhere discards
every revealed Vision; victory through any one of several; the role
change; the projection and frontend showing several.

## Slice 7: A claimed Vision and an off-turn reveal

**False Prophet** (ruling 14).

- **The When Played.** For an Exile: choose another player's revealed
  Vision, gain one warband from the warband bank and put it on that Vision
  (`warbandsOnCards`, as the Obsidian Cage holds warbands).
- **The claim.** `RevealedVisions.of` adds, for a player, every other
  player's revealed Vision that carries one of that player's warbands. The
  victory reads use it, with the Exile check they already make. The
  projection's revealed Vision shows its warbands.
- **The card-play actor.** `CardPlayedFaceup` carries no player, and the
  card-play hooks read `PowerCtx.actor`, which is the walk's actor. The
  slice adds the player to `CardPlayedFaceup` and makes `VisionPlay` and the
  Vision restrictions (Secret Police, Sacred Ground, Vow of Obedience) read
  it, so a reveal inside another player's walk is judged for the right
  player.
- **The discard.** An `ExecutedTrigger` watches `Discard.Vision` of a
  claimed Vision: it kills the warband, then nests a card play for the
  warband's owner with `CardPlayProcedure.unchecked(..., Origin.DiscardPile,
  byPower)`, as Search Party does, offering faceup or discard. It runs
  inside the walk that discarded the Vision (walker game-loop design,
  ruling 15: "as if" nests).

Tests: the When Played and its three no-op cases; the claim winning for
an Exile and not for a Citizen; the owner's replacement and role change
each triggering the claimant's reveal, on the owner's turn; the claimant's
reveal at Desecrated Ground keeping their Visions; the warband killed; the
projection; replay of every new operation.

## Seams opened

| Seam | Slice | Reused by |
|---|---|---|
| Room in `CoreOperations.scala` | 0 | slices 3 and 7 |
| Crown access, plans and consent granter | 1 | Bandit Crown |
| `CardRule.rulerOf` | 2 | Grand Mask, Wild Pastures |
| `ActAsIfAdvisers`, `PowerAccess.advisersOf` | 3 | Master of Disguise |
| Batch questions | 4 | Wild Pastures, Vow of Kinship |
| Spender on favor moves, `SpendableFavor` | 5 | Vow of Kinship |
| `revealedVisions`, `RevealedVisions.of` | 6 | Desecrated Ground, False Prophet |
| Player on `CardPlayedFaceup` | 7 | False Prophet |

## Testing

Each slice follows the batch 8 to 10 shape: a power suite per card through
`Table`, a golden log line per new line, a codec round trip per new
primitive or field, and a replay of each new recorded operation. Each card
leaves `unimplemented.txt` with its slice. The coverage gate stays at 92%.

## Out of scope

The roadmap's engine-blocked phase loses each card as its slice lands and
is deleted with slice 7. Negotiation and the Chronicle steps keep their own
phases.
