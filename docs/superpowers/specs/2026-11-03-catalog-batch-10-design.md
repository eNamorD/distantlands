# Catalog Batch 10: Engine Seams

**Status:** designed 2026-11-03. Slices 1 to 5 landed.

**Builds on** [Catalog batch 9](2026-11-02-catalog-batch-9-design.md) and
[batch 8](2026-10-12-catalog-batch-8-design.md), whose vocabulary, cost
rules, walker shapes and log-line rules apply unchanged. The batch 4 rulings
that apply to a whole batch (restriction icons, site capacity, "your site"
and "your region", turn order) apply here too.

## Goal and scope

Batch 9 implemented every card whose mechanisms the engine already had. What
remains outside the Negotiation and Chronicle phases is engine-blocked: each
card needs a seam, and the roadmap's **Phase - Catalog batch -
engine-blocked** names them. This batch opens the seams that are minor or
moderate and implements every card behind them.

Every seam was re-inventoried at HEAD (`22a0c150`) before this design; the
file and function names below come from that inventory.

The batch takes 28 faces in 14 slices: 26 power ids from
`src/test/resources/powers/unimplemented.txt`, plus consent for the two
Patrols faces, which batch 9 implemented without it. Slices are ordered
cheapest and most self-contained first; slices 11 to 14 touch the procedure
walker.

| Slice | Seam | Cards |
|---|---|---|
| 1 | Empire cards | Council Seat (123), Naysayers (21), Imperial Seal (R26), Bandit Forest (E29r) |
| 2 | Round advance | Great Clock (E12i), Broken Clock (E12r) |
| 3 | Round die | Shattered Portal (E14r) |
| 4 | Own-power lock exemption | Giant's Path (E09i) |
| 5 | Live terrain | Grand Canal (E03i), Lost Lagoon (E03r) |
| 6 | Dispossessed draws | Great Spire (E11i), Fallen Spire (E11r), Pilgrimage (161) |
| 7 | Trade suit | Acting Troupe (36) |
| 8 | Recover source | Drowned Library (E13r) |
| 9 | Search discard destination | Bracken (197), Cracked Horn (R10), Ring of Devotion (R12) |
| 10 | Shuffle every discard pile | Autumn Wind (213) |
| 11 | Challenge reach | School of Vines (E27i), Sigil of the Eye (R43), Sigil of the Heart (R44), Magician's Code (32), Favored Son (235) |
| 12 | Nested card play | Search Party (240) |
| 13 | Askable Act action | Relic Patrols (E17i), Bandit Patrols (E17r) consent |
| 14 | Memory across turns | Tracker (221) |

### Deferred

Six faces need changes that cut across the whole engine. They stay on the
roadmap's engine-blocked list:

- **As-if ruler:** Bandit Crown (R11), Grand Mask (R05). Every ruler read
  changes; one seam would serve both.
- **Favor source redirection:** Vow of Kinship (152). Every cost, take, burn
  and resource read is against the board.
- **Acting as another player's advisers:** Master of Disguise (78).
- **Off-turn actor and a warband on a Vision:** False Prophet (85).
- **Vision collection:** Desecrated Ground (E08r). E08 never appears in a
  generated game.
- **Discard replacement with a question:** Wild Pastures (E07i). About 30
  implemented powers discard cards, most inside a flat `BuildOps` batch that
  cannot park mid-batch for "pay 1 favor instead?". A tree-level hook would
  silently miss most of them, so every Nomad-discarding power would need its
  discard lifted to a tree node first.

Negotiation (The Tribunal, Bandit Tribunal, Festival District, The
Gathering, Whispering Stone, Council Arbiter, Deed Writer, Traveling
Negotiator) and the Chronicle steps (the 36 legacies, Ancient Writ, Stone
Baths, Ruined Baths, Amber Doors, Lost Doors, Blasted Pastures, Ruined
Temple, Hall of Bandits, Hiding Place, Reliquary Raid, Enduring) stay with
their phases.

### Catalog corrections

The card text in the card database omits each card's action-modifier icon;
only the card image shows it. Each slice adds the missing
`modifier = Some(ModifierMark.Action(...))` when it moves its card out of
holding:

| Card | Icon |
|---|---|
| Bracken, Cracked Horn, Ring of Devotion | Search |
| Sigil of the Eye, Sigil of the Heart, Magician's Code, School of Vines | Challenge |
| Drowned Library | Recover |
| Acting Troupe | Trade |

Wild Pastures has no icon: it is a persistent rule.

Two relic power ids carry typos: `relic.king-of-devotion` (Ring of
Devotion) and `relic.ancient-grit` (Ancient Writ). Slice 9 renames the
first to `relic.ring-of-devotion` when it moves the card; old journals need
not decode. Ancient Writ stays in holding, and its id is fixed when the
Chronicle steps implement it.

## Rulings

Taken with the owner on 2026-11-03, except where another source is named.

1. **Usurper Limiter.** When Great Clock moves the round marker from round
   3 to round 4, the Usurper Limiter is removed, as it is when the marker
   reaches space 4 by any means. At round 8, "if possible" fails. "Cannot
   end the game" also means a jump past rounds 5 to 7 never rolls the end
   die.
2. **The discard cards are Search modifiers.** Bracken, Cracked Horn and
   Ring of Devotion each carry the Search icon. A pick covers every card the
   user discards during that Search: unkept draws, advisers bumped over the
   limit, and the People's Favor's site discard (Bracken FAQ: "any cards
   discarded during the Search action"). It does not cover other powers'
   discards (Bracken FAQ: "not cards like Dazzle"). A Vision Cracked Horn
   buries leaves the Visions Drawn track alone (Cracked Horn FAQ).
3. **Wild Pastures** applies only to powers its ruler uses, at 1 favor per
   Nomad card. Deferred (see above).
4. **Imperial Seal** sends every moved warband to one destination. "Imperial
   site" means a site with Imperial warbands (rules: "Sites with Imperial
   warbands are called Imperial sites"), the engine's `SiteRuler.Empire`.
5. **Search Party** runs the full Search card play: faceup to the site,
   facedown as an adviser, or discarded to the next region over. A Vision
   follows Search's rules. Playing one is required when any card is there.
   Search modifiers such as Bracken do not apply, since this is no Search
   action.
6. **Tracker's Wake power is optional**: a usable power in the Wake menu,
   never applied automatically.
7. **Great Spire and Fallen Spire move equal counts in and out.** The
   Dispossessed always holds at least 6 cards; if it ever holds fewer than
   3, both directions move that smaller count.
8. **Council Seat** shows a hover warning on every exile path instead of
   hiding or disabling it. If the player goes ahead, costs are paid and the
   Exile procedure is skipped for a Citizen who holds the Seat faceup.
9. **Pilgrimage and edifices** (BGS rules expert). Intact edifices at the
   site stay, since locked cards cannot be moved. A ruined edifice leaves
   the map for the bottom of the edifice deck, intact side up (CR p. 30:
   "If an edifice is removed from the map in any way, place it intact on the
   bottom of the edifice deck"). It does not count toward the draw, since it
   was not moved to the Dispossessed.
10. **Council Seat and the Chronicle** (BGS rules expert). The Empire's
    break in the Chronicle makes a Citizen become an Exile; it does not
    exile them. `ChronicleCrown.empireBreaks` is unchanged, and the Seat
    guards only exiles during play.
11. **Secret Testament leaves Favored Son alone.** Testament is a Challenge
    modifier, and Favored Son takes its banner outside a Challenge.
12. **Lost Lagoon has the Coast power** (owner, 2026-11-05). Every printed
    Island "has the Coast power", so the Lagoon's site joins both live
    sets: Lagoon-to-Coast costs 1 Supply and counts as a Coast route for
    Narrow Pass.

### Readings

The owner accepted these without a separate ruling:

- **Shattered Portal** rolls the end die, a d6. The map has 8 sites, so the
  pawn never wraps onto its own site.
- **Grand Canal and Lost Lagoon:** the site keeps its printed terrain and
  also gains Coast or Island.
- **Great Spire** adds its 3 Dispossessed cards to the top of the pile, and
  the "bottom 3 denizens" skip Visions.
- **Fallen Spire and Broken Clock:** only the SETUP (END) half is built.
  WHEN EXPLORED waits for an Explore procedure.
- **Tracker:** at Wake the pawn goes to the tracked pawn's site at that
  moment, by placement, not Travel. Tracker must still be the user's faceup
  adviser at Wake.
- **Autumn Wind:** the cards are dealt in random order; when the count does
  not divide by 3, the user picks which piles take the extra cards.
- **Pilgrimage:** the shuffle includes the denizens just moved, so they can
  come back. Only the user peeks.
- **Drowned Library** needs the user's pawn at its site. The relic is the
  relic deck's top card, at difficulty 5, unseen until recovered.
- **Favored Son:** if the user already holds the People's Favor, nothing
  happens. Otherwise they take it from its holder or the shared bank, and
  the People's Favor's left ribbon applies.
- **School of Vines and the Sigils** lift only the pawn-site requirement.
  Magician's Code is selectable only when its 2 secrets make the Darkest
  Secret challengeable.
- **Acting Troupe:** the user picks Beast or Order each Trade.
- **Naysayers** takes 1 favor; "any Exile" includes the user (Naysayers
  FAQ). Nothing happens with no Chancellor or a Chancellor with no favor.
- **Bandit Forest** refuses an Imperial player's use of bandit-ruled
  denizens' non-persistent powers and any placing of favor or secrets on
  them.
- **Giant's Path:** its destination needs an empty slot (batch 4, "Site
  capacity"); its tokens move with it; the pawn is placed, not moved by
  Travel.
- **Search discard modifiers are exclusive.** Once one of Bracken, Cracked
  Horn and Ring of Devotion is picked, the other two show unavailable:
  "Another power already sets where this Search's discards go."

## Slice 1: Empire cards

- **Council Seat** (persistent adviser). `Citizenship.exile` produces no
  operations for a Citizen who holds the Seat faceup
  (`PowerAccess.holdsFaceupAdviser`). The Grand Scepter battle plan's exile
  option (`GrandScepterPlan`, inside its live `Branch`) and the self-exile
  minor action carry the warning "Council Seat: cannot be exiled." as a
  `DecisionOption.Warned`, as `SearchSources` warns. Self-exile still gives
  its favor; the exile is skipped.
- **Naysayers.** An optional Rest `PhasePower`, as Insomnia is, registered
  in `PhasePowerCatalog`. Usable when `current.title` is held by an Exile
  (the user included) and `Imperials.chancellor` has favor. It takes the
  favor with `Take(Piece.Favor(1), ...)` from the Chancellor's play area.
  Line: "Took 1 favor from {Purple}."
- **Imperial Seal.** A paid Action costing 1 secret. Usable when no Imperial
  pawn is at the user's site and Imperial warbands are there. It asks a
  count, then one destination: another Imperial site, or an Imperial
  player's board. Imperial sites are `SiteRule.ruler == SiteRuler.Empire`;
  a small `imperialSites` helper sits beside `SiteRulers.protectedAt`.
  Line: "Moved 3 Imperial warbands to {site}."
- **Bandit Forest.** `GiantPythonRestriction` is renamed
  `NonPersistentPowerRestriction` and takes a guard. Its `PowerUseRestriction`
  refuses a non-persistent use of a guarded card, or a use whose cost
  places on one (`Placements.byUse`); its `OperationRestriction` refuses a
  placement onto one (`Placements.onto`). Giant Python and Bandit Forest are
  its two instances. Bandit Forest guards the denizens at bandit-ruled sites
  against Imperial players while E29's ruined face is on the map
  (`EdificeSetupSupport.siteOf`).

Tests: the Seat's warning on both paths and the skipped exile, costs still
paid; Naysayers usable and not (no Exile title holder, no Chancellor, no
favor); the Seat's count and destination questions, the pawn refusal;
Bandit Forest refusing a use and a placement for an Imperial and not for
an Exile; Giant Python unchanged.

## Slice 2: Round advance

A new primitive, `AdvanceRound`, a `TurnOperation` with a codec entry,
modelled on `AdvanceVisionsDrawn`. It moves `tracks.round` forward one space
and clears `usurperLimited` when the new round is past 3 (ruling 1). It is
never emitted at round 8. `RoundEnded` and the end-die replay read the live
`tracks.round`, so neither changes.

- **Great Clock.** A paid Action costing 1 favor, 2 secrets and 1 burnt
  secret. At round 8 it is shown unavailable: "The round marker is on its
  last space." It never rolls the end die. Line: "Advanced the round marker
  to round 4."
- **Broken Clock.** A `RuinedSetupRule` emitting `AdvanceRound` at SETUP
  (END), registered in `WalkerPowerCatalog` beside Ruined School. Its WHEN
  EXPLORED half stays inert.

Tests: the advance and its line; the Limiter cleared from 3 to 4 and left
alone elsewhere; unavailable at round 8; no end die past rounds 5 to 7;
Broken Clock at setup; codec round trip; replay.

## Slice 3: Round die

`DiceKind.Round` and an `enum RoundDieFace extends DieFace` with faces 1 to
6, scoring their value. The new kind reaches `WalkerDice`, `GameRandomPorts`
(and a port method), `WalkerRolls`, `RollOutcome.derive`, the face and kind
codecs, `RolledFaces`, and the exhaustive matches in `Jinx` and
`RerollChoice`. The end die of `EndDieRolled` is unchanged.

**Shattered Portal.** A paid Action costing 1 secret, usable with the user's
pawn at its site. An automatic `RollStep` of one round die, then a
`BuildOps` that moves the pawn `score` sites Hinterward in
`MapState.hinterward`, wrapping from the bottom Hinterland site to the top
Cradle site, with `PawnMoves.relocate`. Line: "Rolled 4; {Blue} placed at
{site}."

Tests: each wrap position, the roll recorded and replayed, the pawn
refusal, the codec.

## Slice 4: Own-power lock exemption

Operation restrictions run only at command time (`OperationResolution`);
replay re-applies recorded operations without them. The exemption therefore
needs no journaled data.

A capability trait, `IgnoresOwnLock`, mixed into a power rather than added
to `ContributingPower` (see the roadmap's "Narrow `ContributingPower` to
capability traits"). When the walker runs `ActionRef.UsePower` for a
power with the trait, `WalkerPowers.restrictionsDuring` returns the
restrictions with that power's card left out of `LockedCards`; each walk
reads them through `WalkCtx.operationRestrictions`. The exemption lasts for
that use only. The rulebook's own vocabulary matches: "Bury ignores locked".

**Giant's Path.** A paid Action costing 1 secret, mixing in `IgnoresOwnLock`.
Its destination is a choice over `SiteRoom.open` without its own site. It
moves its card, tokens and all, with a plain `Move`, then places the pawn
with `PawnMoves.relocate`, as Giant's Rampage does. Line: "Giant's Path and
{Blue} now stand at {site}."

Tests: the move and placement; the lock still refusing every other power's
move of the Path, and the Path's own move of another locked card; no site
with room; replay.

## Slice 5: Live terrain

`TravelSitePowers` builds the Coast and Island sets once from a static
list. `TravelTerrain` reads them from the state instead: the printed Coast
sites plus the site of either E03 face for the Coast set, and the Coast set
plus the printed Islands for the Coast-or-Island set (ruling 12).
`CoastSitePower.applicable` and `NarrowPassSitePower.blocked` take them from
`ctx.state`.

Two new powers stand at a live site, found with
`EdificeSetupSupport.siteOf`: `GrandCanal`, with Coast behaviour, and
`LostLagoon`, with Island behaviour (+2 Supply to arrive, skipped on a
Coast route) and Coast behaviour to leave. A power's `source` is static, so
each has a `RuleSourceRef.GameRule` source and extends the `LiveTerrain`
marker; Forest Paths ignores a `LiveTerrain` as it ignores printed terrain.

Tests: Coast-to-Canal and Canal-to-Coast at 1 Supply; Lagoon's +2 and the
Coast route to it; Lagoon-to-Coast at 1 Supply; Narrow Pass's coast
exemption with a Canal; Forest Paths ignoring both; the edifice ruined mid-
game switching terrain.

## Slice 6: Dispossessed draws

`ShufflePile.Dispossessed`, whose cards are `campaign.dispossessed`, with a
codec kind. A draw shuffles the pile and takes from its head, as
`ReliquaryDraws` does. Moves into and out of `Location.Dispossessed` are
already legal; the projection never identifies its cards.

- **Great Spire.** A paid Action costing 1 secret. Up to 3 Dispossessed
  cards go to the top of the region's discard pile, then as many of the
  pile's bottom denizens, skipping Visions, go to the Dispossessed (ruling
  7). Nobody peeks. Line: "Swapped 3 cards between the Dispossessed and the
  Cradle's discard pile."
- **Fallen Spire.** A `RuinedSetupRule` at SETUP (END). The denizens in its
  region's discard pile swap with as many random Dispossessed cards; then
  those cards are shuffled with the pile's Visions. WHEN EXPLORED stays
  inert.
- **Pilgrimage.** WHEN PLAYED. Every denizen at the user's site goes to the
  Dispossessed, its tokens back to their banks. A ruined edifice goes to the
  bottom of the edifice deck, intact side up, and is not counted; an intact
  edifice stays (ruling 9). Then as many cards as denizens moved are drawn,
  `Peek`ed by the user, and put on the region's discard pile. Line: "Sent 2
  denizens on Pilgrimage and drew 2 from the Dispossessed."

Tests: each card's moves and counts; hidden information (no other player
learns a card; the user's peeks); the edifice cases; equal counts with a
short Dispossessed; replay.

## Slice 7: Trade suit

**Acting Troupe.** A Trade modifier, a `SelectedModifier`, usable while the
Troupe is the user's faceup adviser. Its `selectionQuestions` ask Beast or
Order once, at the Trade's start. `EffectiveSuit.count` and `traded`
receive `answered`, so a picked Troupe counts as the chosen suit when Trade
counts matching advisers, as Spiteful Mirror's pick is read
(`EffectiveSuit.mirrored`). Line: "Acting Troupe acted as a Beast card."

Tests: the gain with each answer; unpicked, the Troupe counts as Arcane;
other actions unaffected.

## Slice 8: Recover source

**Drowned Library.** A Recover modifier, usable with the user's pawn at its
site; its secret is placed on the card. Picking it records a new scratch
primitive, `RecoverFromRelicDeck`, with a codec entry and a clear when the
walk ends, as `ActAsIf` is recorded and cleared.

`RecoverProcedure` reads both start facts from live state: `succeeded` and
`rollFeedback` use difficulty 5 when the scratch is set, else the site's;
`afterLoop` offers the relic deck's head and `relicMove` moves it from
`Location.Deck(CardDeck.Relic)`. The relic stays unseen until recovered.
With an empty relic deck, the modifier is unavailable with a hover reason.

If Recover refuses at its start when the site holds no relic, before the
modifier loop, this slice moves that refusal after the loop, as slice 11
does for Challenge: the relic options are judged once the picks are known.

Tests: success and failure against difficulty 5; the relic moved from the
deck; the pick unavailable away from the site or with an empty deck; a
Recover at a site with no relic; replay.

## Slice 9: Search discard destination

`Discard.Denizen` and `Discard.Vision` gain a required `position:
StackPosition` (Top or Bottom), with the codec. Every existing builder
passes Top.

Each card becomes a Search modifier, a `SelectedModifier`, whose
`Transform`s at `SearchActionEligibility`, `SearchPlayAdviser` (the card
play, including the People's Favor's site discard) and Spinning Bee's
window rewrite the user's own discards with `OperationRewrite.ops`. A
ruined edifice still goes to the edifice deck.

- **Ring of Devotion:** `to` becomes the region of the user's pawn. Its id
  becomes `relic.ring-of-devotion`.
- **Bracken** (1 secret): `selectionQuestions` ask a discard pile and Top or
  Bottom, once at the Search's start.
- **Cracked Horn:** each discard becomes `Bury.standard` to the bottom of
  the world deck; a buried Vision leaves the Visions Drawn track alone.

Picking one makes the other two unavailable (see Readings). Lines name the
destination once per Search: "Discarded 2 cards to the bottom of the
Hinterland's discard pile."

Tests: each destination for unkept draws, an adviser over the limit and the
People's Favor's discard; Dazzle's discard untouched; exclusivity; a Vision
buried by the Horn; codec.

## Slice 10: Shuffle every discard pile

`Location.ChronicleGathering` and `ShufflePile.ChronicleGathering` are
renamed `CardGathering`, since a play-time power now uses them. It holds
cards gathered for a shuffle, Visions included, so `DenizenGathering` would
misname it. The wire names
change; old journals need not decode.

**Autumn Wind.** WHEN PLAYED, with no new primitive. When the cards across
the three piles do not divide by 3, a `ChooseMany` asks for exactly the
remainder's count of piles to take the extra cards; otherwise nothing is
asked. Then every discard pile, Visions included, moves to the gathering
pile, which is shuffled (`WalkerShuffles` records the order), and a
`BuildOps` deals it round-robin per the answer, as `GatherDenizens` deals.
The question is asked before the shuffle, which `HiddenOutcomes` hides.
Line: "Shuffled the discard piles together and dealt 4, 4 and 3."

Tests: even and uneven totals; the answer honoured; empty piles; the
Chronicle's gathering unchanged after the rename; replay.

## Slice 11: Challenge reach

`ChallengeProcedure.afterModifiers` stops refusing on an empty
`legalBanners` and keeps its `supportedFaces` check. An empty banner
question then refuses the start through `ModifierChoice`'s Start preview and
`QueuedStart.priced`, as Travel and Trade judge their options after their
modifiers. `legalBanners` reads the picks with
`ModifierPicks.cards(answered)`, as `SearchProcedure` does:

- **School of Vines:** an enemy-held banner no longer needs the user's pawn
  at its holder's pawn's site.
- **Sigil of the Eye:** the same for the Darkest Secret; **Sigil of the
  Heart:** for the People's Favor.
- **Magician's Code** (2 favor), for the Darkest Secret only: the strictly-
  more check counts the user's secrets plus 2, the amount's lower bound
  drops to 0, and the payment places the 2 gained secrets with the rest.
  It is selectable only when those 2 secrets make the banner reachable.

**Favored Son.** WHEN PLAYED: a non-required `Take` of the People's Favor
from its holder or the shared bank, as Wizard's Conclave takes a banner,
with the People's Favor branch of `ChallengeRibbon` as its `leaving`.
`ChallengeRibbon` widens to `private[gameplay]` so the ribbon has one
source; it is not walked under the `ChallengeRibbon` window, so Secret
Testament stays out (ruling 11). If the user holds the banner, nothing
happens. Line: "Took the People's Favor." or "Already held the People's
Favor."

Tests: each modifier opening a banner that was out of reach, and the
Challenge refused without it; the Code's amounts and payment; the Code
unselectable when 2 secrets do not suffice; Favored Son from a player, from
the shared bank, and when already held; existing Challenge suites
unchanged.

## Slice 12: Nested card play

Today a card played inside a WHEN PLAYED power fires neither its own WHEN
PLAYED nor the card-play triggers: an `AutomaticUse` is walked with the
enclosing hooks, and `WalkerHooks.gathered` already holds
`ActionCardPlayedFaceup` and `ActionCardPlayedFacedown`. The walker's
`AutomaticUse` case removes those two windows from `gathered`. No loop
results: the nested play's leaf is a sibling, not under the use.

`CardPlayProcedure.Origin` gains a discard-pile origin, so a played card
never enters the temporary hand that the outer Search re-reads on a
single-card draw.

**Search Party.** WHEN PLAYED. The user picks a non-empty discard pile and
peeks at its bottom 3 (`Peek`). They must play one, through
`CardPlayProcedure` with Search's placements: faceup to the site, facedown
as an adviser, or discarded to the next region (ruling 5). The rest never
moved, so they stay at the bottom in order. With every pile empty, nothing
happens. Line: "Peeked at the bottom of the Provinces' discard pile."

Tests: the nested play firing its own WHEN PLAYED and a card-play trigger;
each placement; a Vision; the outer Search's kept card unchanged; empty
piles; replay.

## Slice 13: Askable Act action

`PatrolRule` gets a `Waiver` whose granter is the face's player ruler;
bandits never consent.

`PhaseMenu.offers` shows a refused Act
option as `OptionReason.Askable` when every breach is waivable, through a
`QueuedStart` helper over `waivableBreaches`, keyed on
`ConsentKey(power, actor, "phase.menu", offer.ref)`.
`OathRulesWalker.consentAsk` asks the granter as today, and
`queuedAnswers` treats a start from the menu as the answered `phase.menu`
choice, so the started Recover finds the grant, which lasts the turn. Any
other Act refusal that carries a `Waiver` becomes askable the same way.

Tests: Recover askable under Relic Patrols and under player-ruled Bandit
Patrols; granted, refused and remembered for the turn; not askable when
bandits rule the face; Narrow Pass's travel consent unchanged.

## Slice 14: Memory across turns

Tracker's power ids split into `denizen.tracker.action` and
`denizen.tracker.wake`, as Obsidian Cage's are.

`CurrentGameState` gains `trackedPawns: Map[PlayerId, PlayerId]`, keyed by
the Tracker's holder, with primitives `TrackPawn` and `ClearTrackedPawn`,
their codec entries and replay.

- **Action.** Free. The user picks an enemy pawn at their site (the action
  is available only when one is there), records `TrackPawn`, then
  `EnterPhase(Rest)`. Line: "Tracked {Red}."
- **Wake.** An optional `PhasePower` (`forced = false`, ruling 6), usable
  while the user holds Tracker faceup and has a record. It places the pawn
  at the target's current site with `PawnMoves.relocate`, as Passages does;
  unavailable with a reason when the user is already there. Line: "{Blue}
  placed at {site}."
- **Clear.** When the holder enters their Act phase, `ClearTrackedPawn`
  removes their record, used or not.

The projection shows a record publicly, since the choice was open; the Wake
power's hover names the tracked player.

Tests: the Action ending Act; the Wake power offered and optional; the
pawn placed at a moved target's site; the record cleared at Act; Tracker
lost before Wake; codec; replay.

## Seams opened

| Seam | Slice | Reused by |
|---|---|---|
| `NonPersistentPowerRestriction` | 1 | Giant Python, Bandit Forest |
| `AdvanceRound` | 2 | Great Clock, Broken Clock |
| `DiceKind.Round` | 3 | Shattered Portal |
| `IgnoresOwnLock` | 4 | Giant's Path |
| Live terrain sets | 5 | Grand Canal, Lost Lagoon, Narrow Pass |
| `ShufflePile.Dispossessed` | 6 | both Spires, Pilgrimage |
| `answered` in `EffectiveSuit` | 7 | Acting Troupe |
| `RecoverFromRelicDeck` | 8 | Drowned Library |
| `Discard` position | 9 | Bracken |
| `CardGathering` rename | 10 | Autumn Wind, the Chronicle |
| Challenge judged after modifiers | 11 | four Challenge modifiers |
| Nested WHEN PLAYED | 12 | Search Party |
| Askable Act options | 13 | both Patrols |
| `trackedPawns` | 14 | Tracker |

## Testing

Each slice follows the batch 8 and 9 shape: a power suite per card through
`Table`, a golden log line per new line, a codec round trip per new
primitive or field, and a replay of each new recorded operation. Each card
leaves `unimplemented.txt` with its slice. Slices 11 to 14 also run the full
suites of the procedures they touch (Challenge, card play, Search, the phase
menu and Wake) before landing.

## Out of scope

The roadmap's engine-blocked item keeps the deferred faces above, grouped
by seam, and drops every card this batch implements as each slice lands.
