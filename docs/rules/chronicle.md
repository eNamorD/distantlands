# Chronicle, campaign persistence, and eras

Sources: Combined Rulebook (CR) pp. 36-41 and the New Foundations rev1 task
cards (`reference/2026-03-17-new-foundations-rev1/chronicle-tasks.pdf`). The
cards are slightly older than the CR; where they disagree, the CR wins (see
[ambiguities](ambiguities.md#chronicle-rulings)).

## Persistence model

- A game has a winner; its post-game **Chronicle Phase** transforms the stored world for the next game.
- A **campaign** has no inherent endpoint.
- An **era** is a group-defined scoring window across games. Suggested end is 20 lineage points. At era end, highest score wins; after the current Chronicle completes, scores reset and all other state persists (CR pp. 5, 37).
- Players need not remain constant. The group should pre-agree how new/returning players receive lineages and points (CR p. 5).

## Durable state to preserve

- Chancellor/Citizen/Exile roles and color ownership.
- Active/dormant legacy faces and each player's starting adviser.
- Current Oathkeeper goal.
- Six foundation faces, including whether an active legacy locks an altered face.
- Atlas ordering: Recent vs Forgotten site stacks, Empire divider, and cards stored on each site.
- World deck composition, Dispossessed denizens, and suited reserves. The digital version derives the world deck remainder and suited reserves ("every card not accounted for elsewhere") rather than storing them.
- Reliquary pile (max 5 after Chronicle) and player-held/stored cards. The Chancellor's pieces, the Grand Scepter and the Reliquary are stored apart from the players; the Grand Scepter always goes to the Chancellor, so the digital version does not record its holder.
- Lineage-point scorepad and current era endpoint (CR pp. 36-41).

Not stored by the digital version: the five Visions (always set aside together), the constellation and narrative, ruined/intact edifice faces (settled during setup), and edifice deck order (shuffled at the end of Build Edifice).

## Ordered Chronicle tasks

Resolve the five task cards in order (CR p. 36). The digital phase starts when
the winner chooses Start Chronicle and always ends by journaling
`ChronicleUpdated` with the evolved Chronicle.

Each step names its **Actor**: the player who makes its choices, or "none:
procedural" when nothing is chosen. Task holders are always players who were
seated for the game. Legacy powers may later change a step's actor, add a
step, or skip one; the engine runs each step inside a power window for that
reason.

### 1. The Sun

The winner makes most of this task's choices; each step names its actor.

**Resolve Legacies**

1. Reveal all facedown relics held by players. **Actor:** none: procedural.
2. Players flip dormant legacies whose goals they meet to active, and discard dormant legacies whose goals they do not meet, returning them to the legacy deck. If active legacies alter foundations, flip those foundation setup cards to their altered sides. **Actor:** none: procedural.
3. Exiles (not Imperials) flip their active legacies whose goals they do not meet to dormant. These are *newly dormant* for the rest of the Chronicle. This does not flip foundations. **Actor:** none: procedural.
4. Any player with more than three active legacies discards the excess, in turn order, returning them to the legacy deck. The cap is per player. This does not flip foundations. **Actor:** each such player, choosing which of their own legacies to discard.
5. Shuffle the legacy deck. **Actor:** none: procedural.

Legacy goals (CR p. 36): hold the Grand Scepter (if there is none, the winner
meets it, as in a first game); rule the most sites; hold the Darkest Secret;
hold the People's Favor; hold the relic of the highest value; rule the most
cards of a suit, advisers included. Bandits do not compete; count only sites
and cards ruled by players. Imperial sites, and the cards on them, are ruled
by all Imperials. Imperials have friendly ties: if only Imperials tie, all of
them meet the goal. Exiles have unfriendly ties: if any Exile is tied, nobody
meets the goal, even when tied with the Empire.

**Crown Chancellor**

6. The winner becomes Chancellor and takes the Chancellor board, the purple pieces, and the Grand Scepter. **Actor:** none: procedural.
   - Digital: when no one holds the Grand Scepter, the winner takes it from the box, faceup ([ruling 10](ambiguities.md#chronicle-rulings)).
   - If an Exile won, the Empire breaks. The winner's newly dormant legacies become active. The former Chancellor and all Citizens, even those in the box, become Exiles. Discard all relics in the Reliquary. Remove all Imperial warbands from sites, without adding bandits. Then, on each site the winner rules, replace their warbands with one Imperial warband. The winner rules these sites for the rest of the Chronicle.
   - If a Citizen won, a new dynasty begins: the former Chancellor becomes a Citizen.

**Assign Tasks**

7. The winner takes the Throne task. The player holding the People's Favor takes the World task. The player holding the Darkest Secret takes the Beacon task. **Actor:** the winner, only where a choice arises:
   - If no one holds the People's Favor, the winner gives the World task to any seated player, themself included.
   - If no one holds the Darkest Secret, the winner gives the Beacon task to any seated player, themself included.
   - If one player holds both the World and Beacon tasks, that player gives one of them (their choice) to another seated player of their choice.

### 2. The Throne

The player holding the Throne task (the winner) resolves these steps.

1. **Offer Citizenship.** Offer Citizenship to any Exiles, including players who became Exiles during Crown Chancellor. Each offered Exile accepts or declines. If an Exile becomes a Citizen, flip their newly dormant legacies to active, and replace their warbands at each site they rule with one Imperial warband (excess Imperial warbands may come from other sites). If the former Chancellor accepts, also place one Imperial warband on each site that was previously Imperial, that is, each site with no warbands. **Actor:** the Throne holder chooses whom to offer; each offered Exile answers.
2. **Shape Empire.** Remove any of your warbands from sites, except the last warband of yours on the map. If you rule no sites, remove all warbands from the site with your pawn and place one of your warbands there. Then discard the denizens you do not rule, except intact edifices and cards at Enduring sites. Some legacies also protect denizens from this discard. Ruined edifices discarded this way go to the bottom of the edifice deck. **Actor:** the Throne holder chooses which warbands to remove; the discard is procedural.
   - After Crown Chancellor, "your warbands" are the Imperial warbands and "sites you rule" are the Empire's sites; discarded denizens go to the discard pile of the region after their site's ([rulings 11 and 14](ambiguities.md#chronicle-rulings)).
   - Digital: discarded edifices join the ordered set-aside edifice list instead of the deck bottom ([ruling 9](ambiguities.md#chronicle-rulings)).
3. **Build Edifice.** Find the topmost edifice of each suit in the edifice deck and set them aside: these are available to build. You may bury a denizen at a site you rule to place the matching available edifice there. Locked cards, edifices included, can be buried. Buried denizens go to the bottom of the world deck, facedown; buried edifices go to the bottom of the edifice deck. Shuffle the unused available edifices back into the edifice deck; this shuffle mixes the whole deck, so the edifice deck keeps no order between games. The source sets no limit on how many edifices you build. **Actor:** the Throne holder.
   - Digital: each suit offers a random edifice from those not set aside, and the first set-aside edifice of that suit only when none is left; buried edifices join the set-aside list; a built edifice is placed intact ([rulings 9 and 12](ambiguities.md#chronicle-rulings)).
4. **Stock Reliquary.** Add all of your relics except the Grand Scepter to the Reliquary, facedown. If the Reliquary then holds more than 5 relics, discard relics of your choice until it holds 5. **Actor:** the Throne holder chooses the discards.
5. **Scatter Relics.** Set aside the Grand Scepter and the Reliquary. Shuffle all discarded relics and all relics held by players, and deal them facedown to empty relic slots going Hinterward: top to bottom in the Cradle, then the Provinces, then the Hinterland. Return the rest to the relic deck. **Actor:** none: procedural.
6. **Score Lineage Points.** The winner scores 3 lineage points plus, for each of their legacies, 1 point per matching card at sites they rule (not advisers). Any number of times, the winner may spend 2 of the points they are scoring to let another player score 1. Record the scores in this game's column; circle the Chancellor's score and underline Citizens' scores. If the era's end condition is reached, the player or players with the most lineage points win the era; after this Chronicle Phase, reset everyone's lineage points to 0 and change nothing else. **Actor:** the winner chooses gifts, only to seated players ([ruling 13](ambiguities.md#chronicle-rulings)); scoring is procedural.
7. **Swear an Oath.** If a player won by a Vision, set the Oathkeeper goal to match that Vision. Otherwise, choose an Oathkeeper goal different from the current one. **Actor:** the Throne holder, only when no Vision won.

### 3. The World

The player holding the World task resolves these steps.

1. **Separate Visions.** Set aside all 5 Vision cards. **Actor:** none: procedural.
2. **Gather Denizens.** Shuffle together all denizens from the discard piles and the players' advisers. Return all but 9 of them to the world deck, unseen. Split the 9 into three stacks of 3 and flip two stacks faceup. In the rare case of fewer than 9 denizens, take the difference from the world deck. **Actor:** none: procedural.
3. **Protect and Dispossess.** Choose one faceup stack to protect: secretly choose 1 of its denizens as a facedown starting adviser you keep for the next game, and add the other 2 facedown to the world deck. Place the other two stacks, 6 denizens, behind the Dispossessed divider. **Actor:** the World task holder.

### 4. The Beacon

The player holding the Beacon task resolves these steps.

1. **Introduce Newcomers.** Choose three different suits. Draw three random denizens of the first suit, two of the second, and one of the third, from the suited box sections. Do not take from the Dispossessed or Edifices sections. If any chosen suit lacks enough cards, then before drawing return all Dispossessed cards, except the 6 the World task just added, to their suited sections. **Actor:** the Beacon task holder chooses the suits; the draws are random.
2. **Welcome a Stranger.** Look at the drawn denizens. Keep one as a facedown starting adviser for the next game, and add the other five facedown to the world deck. **Actor:** the Beacon task holder.
3. **Shape Foundation.** You may flip one foundation setup card from its altered side to its normal side, unless an active legacy is altering it. **Actor:** the Beacon task holder.

### 5. The Stars

No step has an actor in the digital version.

1. **Write in the Stars.** The winner draws and names a constellation on the back of the era scorepad, and the group may record the story of the game. Not implemented in the digital version.
2. **Archive Rival Sites.** For each site not ruled by the winner, remove its pieces and stack its remaining cards (relics and intact edifices) on the site card. Stack these sites going Hinterward (top to bottom in the Cradle, then the Provinces, then the Hinterland), so the Cradlemost one is on top. Place the Empire divider on top, and put the stack on the Recent Sites side of the Atlas box, above the sites already there. **Actor:** none: procedural.
3. **Clean Up.** Return all pieces (favor, secrets, warbands, pawns) to their banks. Consolidate the winner's sites Cradleward with all their cards, so the only empty site slots are Hinterward. **Actor:** none: procedural.
4. **Store.** Store the Empire's sites, with their cards, on top of the Empire divider, Cradlemost on top. Store each player's pieces, advisers and legacies, keeping legacy faces; store the Chancellor's pieces, the Grand Scepter and the Reliquary apart from the players; store the Visions and the world deck behind the World Deck divider. The rulebook's "play again or store" choice does not apply: the digital phase always ends by journaling `ChronicleUpdated` with the evolved Chronicle. **Actor:** none: procedural.

## Card and rulebook differences

The rev1 task cards abbreviate several steps. Where the card and the CR differ,
this document follows the CR:

- The Sun card says the Chancellor or first player breaks goal ties; the CR's friendly/unfriendly tie rules govern.
- The Throne card offers Citizenship to "any players"; only Exiles can accept.
- The Throne card omits the intact-edifice and Enduring exceptions to the Shape Empire discard.
- The Throne card sends discarded and buried edifices "back in the box" and lets you choose any matching edifice; the CR uses the topmost edifice of each suit and puts discarded and buried edifices at the bottom of the edifice deck.
- The Beacon card names the suited box sections "the Archive" and omits the Dispossessed return.
