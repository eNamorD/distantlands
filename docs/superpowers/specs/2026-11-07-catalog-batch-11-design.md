# Catalog Batch 11: The Engine-Wide Seams

**Status:** designed 2026-11-07. Slices 0 to 7 landed.

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

Taken with the owner on 2026-11-07; rulings 15 to 18 on 2026-11-08,
while planning slice 2, ruling 19 the same day, while planning slice 3,
rulings 20 to 25 the same day, while planning slice 4, rulings 26 to 29
on 2026-11-09, while planning slice 5, rulings 30 and 31 the same day,
while planning slice 6, and rulings 32 to 34 the same day, while planning
slice 7.

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
15. **Grand Mask: Hidden Passages reads its site.** "Its ruler" is the
    ruler of the Passages' site. During the wearer's turn the Chancellor
    still consents to its use, the wearer's own included.
16. **Grand Mask: Key to the City reads its site.** "The ruler" is the
    ruler of the site it is played to, so the wearer may play it to an
    Imperial site whose ruler's pawn is away and take the site.
17. **Grand Mask: Hospital reads its site.** "If you still rule it" means
    Hospital's site. The wearer does not rule an Imperial site, so their
    killed warbands are never placed there.
18. **Grand Mask: Imperials keep their pawn's site.** During the wearer's
    turn the Imperials lose only the access that ruling gives. They may
    still use the masked cards at their own pawn's site.
19. **Master of Disguise replaces advisers only.** Once it is picked, the
    holder may still pick Trade modifiers from their own relics and from
    the site cards they may use. Those modifiers read the chosen player's
    advisers wherever they read the holder's (Cup of Plenty's match).
20. **Wild Pastures: no question where the discard makes room.** A power
    that discards a card to make the room it then fills is not offered the
    payment: Roving Terror or Giant Python moving to a full site, Horned
    Mask at the adviser limit, a card play replacing an adviser or a card
    at a full site. Where the room exists anyway, the payment is offered and
    the power goes on.
21. **Wild Pastures: ruined edifices.** A ruined Nomad edifice that its
    ruler's power discards is asked about as a denizen is.
22. **Wild Pastures: discards a power permits in a card play.** A site
    discard that Baron, Crop Rotation or the People's Favor's Mob face
    allows is from that power. A card play's other discards are rules,
    unless a power made the play (Search Party, Oracle, Temple of
    Ancestors).
23. **Wild Pastures reads the effective suit.** A Pledge to Discord
    holder's faceup advisers are Discord, so their discard is not of a
    Nomad card.
24. **Wild Pastures: the rest of the power goes on.** After a payment the
    power's rewards stay (Taming Charm still gains 2 favor, Mounted Library
    still doubles), and its counts read what was actually discarded (Riots
    burns only for those).
25. **Wild Pastures: only a faceup card with a place to stay.** Facedown
    advisers have no suit, so their discard is never asked about (Squalid
    District). A card in a hand cannot stay there, so its discard is not
    asked about either (Spinning Bee). A faceup card in play, and a card in
    a discard pile (Search Party's), are.
26. **Vow of Kinship: Challenge counts the Nomad bank.** The holder has the
    Nomad bank's favor for the People's Favor's "more favor than on it"
    check, and the favor they place on the banner, after a Challenge or
    with the banner's own action, may come from the bank.
27. **Vow of Kinship and Vow of Poverty.** Vow of Poverty's "if you have no
    favor" reads the holder's board only, so its REST works while the
    Nomad bank holds favor.
28. **Vow of Kinship and Whispering Leaves.** The favor a chosen player
    must place on Whispering Leaves is done to them: it comes from their
    board only.
29. **Vow of Kinship: gain lines say where the favor went.** Every line
    that says a holder gained favor, or that favor went to them, ends
    ", to the Nomad bank" when the favor went there.
30. **Desecrated Ground: the winner chooses the winning Vision.** A player
    may meet the goals of several of their revealed Visions at once. They
    win either way, and the Vision they won with matters only for the next
    game's Oath, so the win records every Vision whose goal they meet and
    Swear an Oath asks the winner which of them won. With one, nothing is
    asked. War Exhaustion still breaks a tie between Exiles in printed
    priority order, by each Exile's highest-priority qualifying Vision.
31. **Desecrated Ground: a kept Vision has a line.** A reveal that keeps
    Visions writes "Desecrated Ground: Kept Vision of Faith." after the
    reveal's own line.
32. **False Prophet: the current player chooses the claimant.** Several
    players may have a warband on one Vision. When it is discarded, every
    claimant's warband is killed, and the player taking their turn chooses
    which claimant plays or discards it (rule 15.3, Simultaneous Effects).
    The others get nothing, even when the chosen claimant discards it.
33. **False Prophet: faceup or discard.** The claimant may reveal the
    Vision or discard it, never play it facedown as an adviser. A claimant
    who cannot reveal a Vision, such as a Citizen, discards it.
34. **False Prophet's lines.** The When Played writes "False Prophet: Put a
    warband on Blue's Vision of Faith." The discard writes "False Prophet:
    Killed Blue's warband on Vision of Faith." for each claimant. A card
    play by a player who is not the walk's actor names them: "Blue revealed
    Vision of Faith", "Blue discarded Vision of Faith to the Provinces
    discard", and "Desecrated Ground: Blue kept Vision of Conquest."

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
  adviser modifiers come from their advisers only; the holder's relics and
  usable site cards still offer theirs (ruling 19).
- **Desecrated Ground.** An Exile who reveals a Vision while their pawn is
  at the site keeps every revealed Vision they already have. A reveal
  anywhere else discards all of the player's own revealed Visions. Any one
  of a player's revealed Visions can win (ruling 30). A player who becomes a Citizen
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
  card play (Search Party). A site discard a power permits in a card play
  is from that power (ruling 22).

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
- **Site readings.** Hidden Passages, Key to the City and Hospital read
  their site, not the card (rulings 15 to 17).
- **Preferences.** The wearer becomes the granter of the masked cards for
  their turn, so the consent reset (consent design, ruling 17) clears the
  Chancellor's standing preferences on those cards when the turn starts,
  and the wearer's when it ends. A Citizen's turn already does the same to
  the Chancellor. The owner accepted this on 2026-11-08;
  `GrandMaskConsentPreferenceSuite` pins it, and the ROADMAP's clean-up
  phase holds the question of keeping preferences across a one-turn
  handover.

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
  `accessible`'s adviser case and `holdsFaceupAdviser` read through it.
  `faceupAdviserHolder` keeps the real holder: no Trade reads it, and
  through `advisersOf` a chosen card would have two holders. The slice's
  plan lists the raw `.advisers` reads it leaves alone, with the reason for
  each. Slice 5 reads Vow of Kinship's holder through `holdsFaceupAdviser`,
  so Master of Disguise gets around Kinship too (FAQ).
- **The pick.** Master of Disguise is a Trade modifier that asks which
  player, emits `ActAsIfAdvisers`, and is unavailable once another modifier
  is picked. Its own availability reads the real advisers.

Tests: the chosen player's suits count; their Trade modifier offered and
paid by the holder; the holder's own modifiers no longer offered; the
chosen player's Vow of Poverty binding the holder, and the holder's own
vow lifted; the scratch cleared after the Trade and on replay; Master of
Disguise unavailable after another pick.

## Slice 4: A question before a batch

**Wild Pastures** (rulings 11, 12 and 20 to 25).

- **The seam.** Before the walker runs a `BuildOps` leaf's batch, each
  power that asks before a batch looks at the built operations, a `Take`'s
  leaving included. When one asks, the leaf is walked as a subtree under
  its own node. Its first child holds the questions, then each taken
  answer's line. Its second holds the batch rebuilt from the unchanged
  state, with each taken answer's replacement in its operation's place. A
  resume rebuilds the same subtree, so the answers meet the same batch,
  and each question's id names what it asks about (a card id). The batch's
  place does not depend on the number of questions, so a resume inside the
  batch's follow-ups, after the batch ran, never asks again. This is the operation-rewrite design's "ask per
  deferred op" extension point, placed at the leaf so that no discard has
  to be lifted out of its batch. A search walks the batch as built.
- **"From a power".** The walk context does not tell which power built a
  batch: `WalkCtx.procedure` names only a phase power's walk, and a leaf's
  contributions list every power gathered at its windows, a Search
  modifier at the Search's own discard included. So a discard carries its
  cause: `Discard.Denizen` and `Discard.RuinedEdifice` gain
  `cause: Option[DiscardCause]`, the power whose use discards the card and
  whether the discard makes the room that power then fills (ruling 20).
  Each power's discard sets it, and a rule's discard (a Search's own, a
  plain card play's, the Chronicle's) has none. A card play a power made
  gives its discards that power, and a site discard a power permits gives
  the permission's power (ruling 22).
- **Wild Pastures' question.** For each discard of a Nomad card (its
  effective suit, ruling 23), denizen or ruined edifice (ruling 21), whose
  acting player is Wild Pastures' card-level ruler, whose cause names a
  power and makes no room, of a faceup card in play or a card in a discard
  pile (ruling 25): "Pay 1 favor to the Nomad bank instead?" A yes replaces
  the discard with a `Give` of 1 favor to the Nomad bank and leaves the card
  where it is; the rest of the batch runs as built (ruling 24), and the line
  reads that they paid instead of discarding. A ruler without favor on
  their board is not asked.
- **Composition.** Wild Mounts' `Defer` keeps its own question; the Wild
  Pastures question is asked when the deferred discard's batch runs, so
  Wild Mounts asks first.

Tests: a phase power, a When Played effect, a battle plan and Search
Party's card play each asked; a discard with no cause, one that makes room,
a facedown adviser's and a card in a hand never asked; Roving Terror moving
to a site with room after a yes; the question parked and answered across a
resume and a replay; Wild Mounts and Wild Pastures on one plan; an unruled
or another player's Wild Pastures never asks.

## Slice 5: Favor redirection

**Vow of Kinship** (rulings 9, 10, 12 and 26 to 29).

The holder is the player who acts with the Vow faceup among their
advisers (`PowerAccess.holdsFaceupAdviser`), so Master of Disguise lifts
it, or lends it, for one Trade.

- **Credit.** In `ResourceOperations`, a favor `Move` into the holder's
  `PlayArea` lands in `FavorBank(Nomad)`. This catches `Gain`, `Take` and
  `Give` however they were built, without loosening their ownership
  `require`s. The one exception is the draw below.
- **Spends.** A spend is a favor move out of the holder's board that the
  holder makes: the move carries `by = Some(holder)`. `Give` moves already
  carry their giver. The slice adds the payer to `PayCost`'s favor moves
  and the actor to the Challenge's and the banner action's favor moves.
  Revelation's chosen burn becomes a `PayCost` burn, as a bandit toll
  already is. A take, the Campaign's defender burn and Defame carry
  another mover or none, and Whispering Leaves' forced placement becomes a
  plain move with no mover (ruling 28), so all of them read the board only.
- **The draw.** The executor cannot draw on the bank by itself: a cost's
  burn that Vow of Division takes becomes a `Take` by the taker, which
  would then read the holder's board only. So before a walker batch runs,
  and before `BurnTakes` rewrites it, `KinshipDraws.before` puts a
  `DrawNomadFavor(holder, n)` in front of the first operation whose spend
  the board cannot cover. The draw moves `n` favor from the Nomad bank onto
  the board, unredirected, and the spend then runs unchanged.
  `ModifierOffers`' dry run of selection payments uses the same pass.
- **Affordability.** A shared `SpendableFavor.of(ready, player)`, the
  board plus the holder's Nomad bank, replaces the direct `board.favor`
  affordability reads: `Costs.plan`, `PricedOptions`, `ModifierChoice`,
  `RerollChoice`, `PlanPrice`, `PlanAid`, `PlanAidDesk`,
  `PlanAidProjector`, `NegotiationDeal`, `SelfExile`, `TollRoads`,
  `RulerToll`, `PeaceEnvoy`, `WitchsBargain`'s give and Revelation's
  amount. `BannerRules.playerResources` reads it for the People's Favor
  (ruling 26). The reads ruling 9 and rulings 27 and 28 keep on the board
  (Royal Tax, Chaos Cult, Charming Friend, Naysayers, Pied Piper, Witch's
  Bargain's take, Defame, the Campaign's defender burn, Vow of Poverty,
  Whispering Leaves) stay.
- **The source question.** When the board and the bank both hold favor and
  a batch's spend could take from either, the Vow asks the holder, through
  slice 4's seam, how much of it comes from the Nomad bank. It asks once
  per batch, at the batch's first spend, so the seam's `BatchContext` gains
  the batch's operations. A cost walked as its child moves is a plain
  leaf, not a `BuildOps`, so the seam also asks about a plain leaf, as a
  batch of one. The answer puts its draw in front of the spend.
- **Arrival.** The Vow follows arrivals: when it becomes a faceup adviser,
  the holder's board favor moves to the Nomad bank.
- **Log.** `NoteSupport.favor` counts the holder's Nomad bank with their
  board, so a gain, payment or loss line reads what happened. A line about
  favor that went to a holder carries `NoteArg.ToNomadBank`, and the Game
  Log ends that sentence ", to the Nomad bank" (ruling 29).

Tests: gains, takes and gifts landing in the Nomad bank; a cost, a toll, a
chosen burn and a banner placement paid from it; a cost's burn taken by
Vow of Division; Royal Tax, the defender's burn and Whispering Leaves
reading the board only; affordability counting the bank; Vow of Poverty
reading the board; the source question asked only with both sources; the
arrival move; the gain lines' ending; Wild Pastures paid from bank to bank;
Master of Disguise lifting the Vow for one Trade.

## Slice 6: Several revealed Visions

**Desecrated Ground** (rulings 13, 30 and 31).

- **The model.** `PlayerState.revealedVision: Option[VisionState]` becomes
  `revealedVisions: Vector[VisionState]`, in reveal order. Its 24
  production read and write sites move to it: card play
  (`CardPlay.planVision` and `revealed`, `CardSlots`), victory
  (`VisionVictoryEligibility`, `StateBasedEvaluation`, `SwearOath`), the
  role change, the movement and state writes, `CardIndex`,
  `OperationExecutor`, the projection DTOs and codec, and the frontend
  board. The movement write no longer refuses a second Vision: card play
  decides what a reveal discards.
- **One read path.** `RevealedVisions.of(ready, player)` returns a player's
  revealed Visions. The victory reads go through it, so slice 7 can add
  claimed Visions in one place. The slot's own bookkeeping (card play, the
  role change, the movement writes) reads the field, because a claim never
  puts a Vision in the claimant's slot.
- **Desecrated Ground.** `DesecratedGroundRule.keeps(ready, player)` is
  true for an Exile whose pawn is at the site where E08 stands ruined, in
  play. It sits in `gameplay`, beside the moved `SacredGroundCard`, because
  card play may not import `gameplay.powers`. When it holds, `CardSlots`
  offers the spot with no replacement and `CardPlay` discards nothing.
  Elsewhere the spot replaces every revealed Vision: `CardSlots` nests one
  `Replacing` per Vision, and the option row names them all ("Replace
  Vision of Conquest and Vision of Faith as revealed Vision"). A player who
  becomes a Citizen discards all of them. The power is registered with no
  window, as Grand Mask is, so the card counts as implemented.
- **The kept line** (ruling 31). The card play's tree gains a fourth child
  after the play's own line: a note sourced from E08 that names the
  player's revealed Visions other than the one just played. Elsewhere none
  are left, so it writes nothing.
- **The win** (ruling 30). `VisionVictory` and `WarExhaustionResolved`
  carry `visions: Vector[VisionId]`, every revealed Vision whose goal the
  winner meets, and `GameResult` gains the same field. The headline names
  them all ("Red won with Vision of Conquest and Vision of Faith"). Swear an
  Oath reads `GameResult.visions`: one sets its goal as today; several ask
  the winner, through a new `chronicle.swear-oath.vision` decision, which
  Vision won.
- **Wire.** The projection's `revealedVision` becomes `revealedVisions`, a
  list, and the two events' `visionId` becomes `visionIds`; old journals
  need not decode.

Tests: a reveal at the site keeps the old Vision and writes the kept line;
one elsewhere discards every revealed Vision; victory through any one of several; Swear an Oath asking among
several; the role change; the projection and frontend showing several.

## Slice 7: A claimed Vision and an off-turn reveal

**False Prophet** (rulings 14, 32, 33 and 34).

- **The When Played.** For an Exile with a warband in their bank: choose
  another player's revealed Vision, gain one warband and put it on that
  Vision (`warbandsOnCards`, as the Obsidian Cage holds warbands). With no
  such Vision, no warband or no Exile role it does nothing and writes
  nothing. A warband on a card is checked against the card's tokens today,
  which a Vision has none of, so a Vision's warbands are read from
  `warbandsOnCards` alone.
- **The claim.** `RevealedVisions.of` adds, for a player, every other
  player's revealed Vision that carries a warband of that player's lineage
  (`ForceKind.Exile`). The kind is the lineage's, not the role's, so a
  claimant who becomes a Citizen keeps the claim, and the victory reads'
  Exile check stops them winning with it (FAQ). The owner's board shows the
  Vision's warbands.
- **The card-play actor.** `CardPlayedFaceup` carries no player, and the
  card-play hooks read `PowerCtx.actor`, which is the walk's actor. The
  slice adds the player, as `CardPlayedFacedown` has it, and `VisionPlay`
  and the Vision readers (Secret Police, Sacred Ground, Vow of Obedience,
  Book Binders) read it, so a reveal inside another player's walk is
  judged for the right player.
- **The discard.** `FalseProphetClaim`, an `ExecutedTrigger`, watches the
  batch's `Discard.Vision` of a Vision that carries warbands. Its
  follow-up asks the current player which claimant plays it, when there
  are several (ruling 32), nests a card play for that claimant with
  `CardPlayProcedure.unchecked(..., Origin.DiscardPile(to), byPower =
  Some(FalseProphetClaim.id))`, as Search Party does, and then kills every
  claimant's warband. A Transform at `SearchPlayAdviser` limits that play
  to faceup or discard (ruling 33), as Temple of Ancestors limits its own.
  A trigger's follow-up must give the same tree when a command resumes
  inside it, and the warbands on the Vision are what it reads, so the
  kills come after every Vision's question and play, and their lines
  follow the plays'. The trigger has its
  own power id, `denizen.false-prophet.claim`, because the walker catalog
  holds one power per id.
- **The look-ahead reads the follow-up.** The walker's restriction
  look-ahead folded only the action's tree, which never holds a trigger's
  follow-up, so a Vision rule could not hide the claimant's faceup
  placement. The walk, and the lookup of a parked decision, now fold the
  follow-up the decision lies in beside the action's tree.
- **Off-turn lines** (ruling 34). The card play's own lines, and the kept
  line, gain a variant naming the player when they are not the walk's
  actor.

Tests: the When Played and its three no-op cases; the claim winning for
an Exile and not for a Citizen; the owner's replacement and role change
each triggering the claimant's reveal, on the owner's turn; the current
player choosing among two claimants; the claimant's restrictions judged
for them; the claimant's reveal at Desecrated Ground keeping their
Visions; the warband killed; the projection; the journal rebuilding every
new step.

## Seams opened

| Seam | Slice | Reused by |
|---|---|---|
| Room in `CoreOperations.scala` | 0 | slices 3 and 7 |
| Crown access, plans and consent granter | 1 | Bandit Crown |
| `CardRule.rulerOf` | 2 | Grand Mask, Wild Pastures |
| `ActAsIfAdvisers`, `PowerAccess.advisersOf` | 3 | Master of Disguise |
| Batch questions, `DiscardCause` | 4 | Wild Pastures, Vow of Kinship |
| A spend's mover, `DrawNomadFavor`, `SpendableFavor`, `NoteArg.ToNomadBank` | 5 | Vow of Kinship |
| `revealedVisions`, `RevealedVisions.of`, `DesecratedGroundRule`, a win's `visions` | 6 | Desecrated Ground, False Prophet |
| Player on `CardPlayedFaceup`, warbands on a Vision, off-turn card-play lines, look-ahead into a follow-up | 7 | False Prophet |

## Testing

Each slice follows the batch 8 to 10 shape: a power suite per card through
`Table`, a golden log line per new line, a codec round trip per new
primitive or field, and a replay of each new recorded operation. Each card
leaves `unimplemented.txt` with its slice. The coverage gate stays at 92%.

## Out of scope

The roadmap's engine-blocked phase loses each card as its slice lands and
is deleted with slice 7. Negotiation and the Chronicle steps keep their own
phases.
