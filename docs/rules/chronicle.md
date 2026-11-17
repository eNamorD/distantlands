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
- Six foundation faces, including which legacies alter an altered face (its alteration sources).
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

Digital: before Start Chronicle, a window runs once the game has a result (it opened the Sun until Legacy in-play powers R36). When the game ended in a Chancellor's or a Successor's win, a seated Citizen holding Royal Line active may decline, or choose the winner from every Imperial player, the Chancellor first. A Citizen chosen wins as the Successor; the Chancellor chosen wins by the Empire win that ended the game. A changed winner is journaled, "p3 named p2 the winner with Royal Line", and declining or choosing the current winner changes nothing. Every later step, the result display and the Game Log read the winner from the game's result ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 53 to 59).

**Resolve Legacies**

1. Reveal all facedown relics held by players. **Actor:** none: procedural.
   - Digital: then, before players resolve legacies, a seated player holding Gatekeeper active may take one active legacy from an enemy whose goal they meet, or decline; it joins them active, step 2 alters its foundation if it alters one, and step 4 applies the cap. They answer one choice, Decline first, each legacy naming its holder; "p2 took Seer from p3 with Gatekeeper" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 6 and 14).
   - Digital: before the reveal, a seated Imperial holding Iron Hand active discards it to the legacy deck, "p2 discarded Iron Hand"; the same checkpoint runs at the start of step 4, after Crown Chancellor and after Offer Citizenship, each before its cap. A dormant Iron Hand stays until Flip Legacies activates it ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 41, 45 and 46).
2. Players flip dormant legacies whose goals they meet to active, and discard dormant legacies whose goals they do not meet, returning them to the legacy deck. If active legacies alter foundations, flip those foundation setup cards to their altered sides. **Actor:** none: procedural.
   - Digital: in the Chronicle Phase every legacy is shown to every viewer, so the lines name them. Dormant legacies resolve first, in turn order (seat order from the player who started the last round); then each public legacy in row order, to the one player who meets its goal, to the tied Imperial the outgoing Chancellor chooses, or back to the deck; then each active legacy of a seated or boxed lineage alters its foundation, I to VI, unless the face already lists it. Lines: "p2 activated Seer", "p3 discarded Iron Hand", "p3 took Matchmaker", "Iron Hand returned to the legacy deck", "Wealthy set Foundation II to Movers and Shakers" ([Legacies rulings](ambiguities.md#legacies-rulings)).
3. Exiles (not Imperials) flip their active legacies whose goals they do not meet to dormant. These are *newly dormant* for the rest of the Chronicle. This does not flip foundations. **Actor:** none: procedural.
   - Digital: in turn order; "p2's Iron Hand went dormant".
   - Digital: an Exile holding Steadfast active at the step's start keeps their other legacies active; Steadfast itself can go dormant, and step 4 still discards over the cap ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 5).
4. Any player with more than three active legacies discards the excess, in turn order, returning them to the legacy deck. The cap is per player. This does not flip foundations. **Actor:** each such player, choosing which of their own legacies to discard.
   - Digital: each player above three answers a choice of exactly the excess, in turn order; "p2 discarded Seer". The same choice runs again for a player whom a reactivation leaves above three: the winner at Crown Chancellor, a new Citizen once every Citizenship answer is in.
5. Shuffle the legacy deck. **Actor:** none: procedural.
   - Digital: an empty window; the legacy deck is derived and keeps no order ([Legacies rulings](ambiguities.md#legacies-rulings), item 3).
   - Digital: then a seated player holding Chronicler active may declare that every seated player scores their legacies now, or decline. Each scores the Score Lineage Points bonus for every active legacy they hold at the sites they rule, with no base points and no gifts. Every active legacy held is recorded as scored, even one that scores 0, and no later scoring counts it again. A dormant legacy neither scores nor is recorded, so one that becomes active later may still score, such as an Exile winner's, reactivated at Crown Chancellor. Lines: "p2 declared Chronicler", then "p1 scored 2 lineage points (2 from Beast)" for each player who scores a point ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 7, 8 and 9-12).

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
   - If an Exile won, the Empire breaks. The winner's newly dormant legacies become active ("p1 reactivated Iron Hand"). The former Chancellor and all Citizens, even those in the box, become Exiles. Discard all relics in the Reliquary. Remove all Imperial warbands from sites, without adding bandits. Then, on each site the winner rules, replace their warbands with one Imperial warband. The winner rules these sites for the rest of the Chronicle.
   - Digital: once the Imperial warbands are removed, each site that held them takes one warband of a seated former Chancellor's colour, so the former Chancellor rules it as an Exile, their bank topped up when short; a boxed one places none ([ruling 20](ambiguities.md#chronicle-rulings), a deviation).
   - If a Citizen won, a new dynasty begins: the former Chancellor becomes a Citizen.
   - Digital: a Reliquary Setup With No Empire set aside is discarded whoever wins, as an Exile's win discards it ([Foundations ruling 3](ambiguities.md#foundations-rulings)).

**Assign Tasks**

7. The winner takes the Throne task. The player holding the People's Favor takes the World task. The player holding the Darkest Secret takes the Beacon task. **Actor:** the winner, only where a choice arises:
   - If no one holds the People's Favor, the winner gives the World task to any seated player, themself included.
   - If no one holds the Darkest Secret, the winner gives the Beacon task to any seated player, themself included.
   - If one player holds both the World and Beacon tasks, that player gives one of them (their choice) to another seated player of their choice.

### 2. The Throne

The player holding the Throne task (the winner) resolves these steps.

- Digital: first, every relic players hold but the Grand Scepter turns facedown and no viewer's record of it remains; each holder still sees their own. Scatter Relics' lines then name a relic only to a viewer who knows it, "p2 set aside a Relic" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 40; [Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), item 3).
- Digital: then, if the winner holds Reformer active, they may move and swap cards, edifices included, between the sites they rule, ignoring locks. Each pass picks a card or Done, then another ruled site with room (a move) or a card at another ruled site (a swap); passes repeat until Done. Lines: "p1 moved Village Idiot to Broken Peaks with Reformer", "p1 swapped Village Idiot and Map Library with Reformer" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 18 and 22-25).
- Digital: then, while Hiding Place (E26 ruined) is on the map, the player who rules its site, the Chancellor for an Imperial site, may keep one of their relics but the Grand Scepter for the next game, or Decline; bandits or nobody asks nothing. Its secrets return to them facedown and warbands on it to their supply, and the relic joins their lineage's starting relics. Every viewer reads "p2 kept a relic with Hiding Place"; the ruler alone also reads which relic ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), item 4).
- Digital: then, while Blasted Pastures (E07 ruined) is on the map and the Throne holder does not rule its site (after Crown Chancellor the Empire's sites are theirs), every Nomad card at a site is buried, Blasted Pastures, locked cards and intact edifices included: denizens to the bottom of the world deck, edifices to the edifice deck. Favor on them returns to the Nomad bank and secrets go to the Throne holder. Advisers and relics stay. One line names the cards, "Blasted Pastures buried Elders and Giant's Rampage" ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 5 and 6).

1. **Offer Citizenship.** Offer Citizenship to any Exiles, including players who became Exiles during Crown Chancellor. Each offered Exile accepts or declines. If an Exile becomes a Citizen, flip their newly dormant legacies to active, and replace their warbands at each site they rule with one Imperial warband (excess Imperial warbands may come from other sites). If the former Chancellor accepts, also place one Imperial warband on each site that was previously Imperial, that is, each site with no warbands. **Actor:** the Throne holder chooses whom to offer; each offered Exile answers.
   - Digital: only seated Exiles are offered, and they answer at once, in any order, Decline first. Accepting returns the player's board warbands and turns each site they rule into one Imperial warband; the bank is topped up when short. The former Chancellor needs no special case: after ruling 20 their sites turn Imperial like any Exile's ([rulings 20-22](ambiguities.md#chronicle-rulings)).
2. **Shape Empire.** Remove any of your warbands from sites, except the last warband of yours on the map. If you rule no sites, remove all warbands from the site with your pawn and place one of your warbands there. Then discard the denizens you do not rule, except intact edifices and cards at Enduring sites. Some legacies also protect denizens from this discard. Ruined edifices discarded this way go to the bottom of the edifice deck. **Actor:** the Throne holder chooses which warbands to remove; the discard is procedural.
   - After Crown Chancellor, "your warbands" are the Imperial warbands and "sites you rule" are the Empire's sites; discarded denizens go to the discard pile of the region after their site's ([rulings 11 and 14](ambiguities.md#chronicle-rulings)).
   - Digital: a seated player holding Rival active first keeps any of the cards the step would discard, ruined edifices included, in one choice; "p1 protected 2 denizens with Rival" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 15, 20 and 24).
   - Digital: discarded edifices join the ordered set-aside edifice list instead of the deck bottom ([ruling 9](ambiguities.md#chronicle-rulings)).
   - Digital: while a seated Exile holds Iron Hand active, they perform this step in the Empire's place over the sites they rule, and every card at a site they do not rule, the Empire's included, is discarded but intact edifices and Enduring sites' cards ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 42 and 47).
   - Digital: Ancient City is the Enduring site. While Hall of Bandits (E16 ruined) is on the map, a ruined edifice at a site bandits rule is not discarded either, the Hall included; Rival is not offered it. Neither writes a line ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 1 and 2).
3. **Build Edifice.** Find the topmost edifice of each suit in the edifice deck and set them aside: these are available to build. You may bury a denizen at a site you rule to place the matching available edifice there. Locked cards, edifices included, can be buried. Buried denizens go to the bottom of the world deck, facedown; buried edifices go to the bottom of the edifice deck. Shuffle the unused available edifices back into the edifice deck; this shuffle mixes the whole deck, so the edifice deck keeps no order between games. The source sets no limit on how many edifices you build. **Actor:** the Throne holder.
   - Digital: each suit offers a random edifice from those not set aside, and the first set-aside edifice of that suit only when none is left. Buried edifices go to the edifice deck, and no later suit offers one buried in this step ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 61); the step ends by returning the set-aside list to the deck; a built edifice is placed intact ([rulings 9 and 12](ambiguities.md#chronicle-rulings)).
   - Digital: an Imperial holding Great Architect active performs the step in place of the Throne holder, and may bury any card at a site they rule for each available edifice, not only one of its suit; a burial returns favor to the buried card's own suit bank ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 16 and 26).
   - Digital: then a seated player holding Revolutionary active may discard any of the denizens and ruined edifices at Imperial sites, in one choice; locked cards stay. Denizens go where Shape Empire sends them, and ruined edifices return to the bottom of the edifice deck. "p2 discarded 3 denizens with Revolutionary" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 17, 21, 24 and 27).
   - Digital: while a seated Exile holds Iron Hand active they build over the sites they rule; an Imperial Great Architect builds in their place, and then the holder does not build ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 42 and 48).
4. **Stock Reliquary.** Add all of your relics except the Grand Scepter to the Reliquary, facedown. If the Reliquary then holds more than 5 relics, discard relics of your choice until it holds 5. **Actor:** the Throne holder chooses the discards.
   - Digital: after this step, a seated player holding Heirloom active keeps one of their relics but the Grand Scepter for the next game, a single one without a question; its secrets return to them facedown and the relic joins their lineage's starting relics, which game start hands back facedown. A Throne holder's relics are already in the Reliquary. Every viewer reads "p2 kept a relic with Heirloom"; the holder alone also reads which relic ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 29, 34 and 35).
5. **Scatter Relics.** Set aside the Grand Scepter and the Reliquary. Shuffle all discarded relics and all relics held by players, and deal them facedown to empty relic slots going Hinterward: top to bottom in the Cradle, then the Provinces, then the Hinterland. Return the rest to the relic deck. **Actor:** none: procedural.
   - Digital: a seated player holding The Ear active peeks every relic set aside, then chooses the relic for each empty slot, Hinterward, while two or more remain; the last is placed without a question, and the rest return to the relic deck. The holder alone sees the relics, and keeps knowing each where it lies; the peeks write no line. "p2 chose where the relics went with The Ear" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 30, 36, 37 and 62).
6. **Score Lineage Points.** The winner scores 3 lineage points plus, for each of their legacies, 1 point per matching card at sites they rule (not advisers). Any number of times, the winner may spend 2 of the points they are scoring to let another player score 1. Record the scores in this game's column; circle the Chancellor's score and underline Citizens' scores. If the era's end condition is reached, the player or players with the most lineage points win the era; after this Chronicle Phase, reset everyone's lineage points to 0 and change nothing else. **Actor:** the winner chooses gifts, only to seated players ([ruling 13](ambiguities.md#chronicle-rulings)); scoring is procedural.
   - Digital: the bonus counts each legacy's printed suit, denizens and edifices, intact or ruined, at the Empire's sites; legacies of one suit add into one share, in suit order. The winner may give away half the total, rounded down. Before the gift question the line names the earned total, "p1 scored 7 lineage points (3 base, 3 from Beast, 1 from Nomad)", or with no bonus "p1 scored 3 lineage points"; then one line per gift, "p1 gave up 4 points to give p2 2 points.", and after any gift "p1 keeps 3 points", 0 points included. The journal records the kept points ([Legacies rulings](ambiguities.md#legacies-rulings)).
   - Digital: a legacy Chronicler already scored this game adds nothing. The scorepad row records each lineage's role at the Chronicle's end, even when Chronicler opened the row in the Sun ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 8 and 11).
   - Digital: after the winner, a seated Exile holding Iron Hand active scores the legacy bonus at the sites they rule, with no base and no gifts, skipping legacies already scored and recording theirs: "p3 scored 2 lineage points with Iron Hand (2 from Beast)" ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 43, 49 and 50).
7. **Swear an Oath.** If a player won by a Vision, set the Oathkeeper goal to match that Vision. Otherwise, choose an Oathkeeper goal different from the current one. **Actor:** the Throne holder, only when no Vision won.

### 3. The World

The player holding the World task resolves these steps.

1. **Separate Visions.** Set aside all 5 Vision cards. **Actor:** none: procedural.
   - Digital: Visions leave play; setup adds all five again.
2. **Gather Denizens.** Shuffle together all denizens from the discard piles and the players' advisers. Return all but 9 of them to the world deck, unseen. Split the 9 into three stacks of 3 and flip two stacks faceup. In the rare case of fewer than 9 denizens, take the difference from the world deck. **Actor:** none: procedural.
   - Digital: the tokens on each adviser return to their banks first (favor to its suit's bank, secrets to the adviser's holder, facedown), and locked advisers go with the rest ([ruling 16](ambiguities.md#chronicle-rulings)). The nine dealt denizens sit in an ordered gathering pile: the facedown stack is always the last three cards and the faceup stacks come before it.
   - Digital: first, a seated player holding Oaths of Loyalty active keeps up to three of their faceup advisers as facedown starting advisers for the next game, their tokens returned as for every adviser; "p2 kept Birdsong and Golem Legions with Oaths of Loyalty". Every other starting adviser they would gain is buried at the world deck bottom: the keep of Protect and Dispossess and of Welcome a Stranger is still asked and the kept card buried, "p2 buried Fae Merchant with Oaths of Loyalty"; so is the next Setup's pick, and under Teeming World they draw until they would hold three, counting the advisers Oaths kept, and bury every card drawn below the Visions ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 31, 32 and 38).
3. **Protect and Dispossess.** Choose one faceup stack to protect: secretly choose 1 of its denizens as a facedown starting adviser you keep for the next game, and add the other 2 facedown to the world deck. Place the other two stacks, 6 denizens, behind the Dispossessed divider. **Actor:** the World task holder.
   - Digital: the holder protects a stack by naming any card in it. When the step ends, the six unprotected denizens move to a newly Dispossessed pile, and one log line names the faceup three. They stay apart from the older Dispossessed until the Chronicle is recorded, so the Beacon's return leaves them alone.
   - Digital: a World holder holding Oaths of Loyalty active still answers the keep, and the kept card is buried at the world deck bottom: "p2 buried Fae Merchant with Oaths of Loyalty" (see Gather Denizens; [Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 38).

### 4. The Beacon

The player holding the Beacon task resolves these steps.

1. **Introduce Newcomers.** Choose three different suits. Draw three random denizens of the first suit, two of the second, and one of the third, from the suited box sections. Do not take from the Dispossessed or Edifices sections. If any chosen suit lacks enough cards, then before drawing return all Dispossessed cards, except the 6 the World task just added, to their suited sections. **Actor:** the Beacon task holder chooses the suits; the draws are random.
   - Digital: the holder chooses the suit for three draws, then two, then one. Each chosen suit's boxed denizens join its live reserve before the draw. A suit still short after the return draws what remains ([ruling 15](ambiguities.md#chronicle-rulings)).
   - Digital: a Beacon holder holding The Bright Beacon active first chooses the suited box or the Dispossessed, before seeing them. From the Dispossessed they see every Dispossessed denizen, the six just added included, and take exactly six (all six without a question when exactly six remain), on which Welcome a Stranger runs; "p3 took six newcomers from the Dispossessed with The Bright Beacon". Only the holder sees the cards, and the peeks write no line ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 33, 37, 39 and 62).
2. **Welcome a Stranger.** Look at the drawn denizens. Keep one as a facedown starting adviser for the next game, and add the other five facedown to the world deck. **Actor:** the Beacon task holder.
   - Digital: a Beacon holder holding Oaths of Loyalty active still answers the keep, and the kept card is buried at the world deck bottom (see the World's Gather Denizens; [Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 38).
3. **Shape Foundation.** You may flip one foundation setup card from its altered side to its normal side, unless an active legacy is altering it. **Actor:** the Beacon task holder.
   - Digital: the holder is asked whenever a foundation lies altered. Keep every foundation comes first, then each altered face, I to VI. A face an active legacy alters is shown unavailable, naming the legacy, even when every altered face is held and only Keep may be chosen ([Foundations rulings](ambiguities.md#foundations-rulings), items 1 and 15). The restored face is normal in the journaled Chronicle, with no alteration source, and one log line names it: "p3 restored Foundation I to Imperial Maps". Keep writes "p3 did not flip any foundations".

### 5. The Stars

No step has an actor in the digital version.

1. **Write in the Stars.** The winner draws and names a constellation on the back of the era scorepad, and the group may record the story of the game. Not implemented in the digital version.
   - Digital: an empty window ([ruling 7](ambiguities.md#chronicle-rulings)).
2. **Archive Rival Sites.** For each site not ruled by the winner, remove its pieces and stack its remaining cards (relics and intact edifices) on the site card. Stack these sites going Hinterward (top to bottom in the Cradle, then the Provinces, then the Hinterland), so the Cradlemost one is on top. Place the Empire divider on top, and put the stack on the Recent Sites side of the Atlas box, above the sites already there. **Actor:** none: procedural.
   - Digital: one log line per archived site. The rival sites are stored at the atlas's Recent end, Cradlemost first, each with every card left on it ([rulings 17, 18](ambiguities.md#chronicle-rulings)). The Empire divider is not stored: the Chronicle keeps the Empire's sites in their own list.
   - Digital: while a seated Exile holds Iron Hand active, the Empire's sites are archived as rival sites and the holder's are not ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), item 44).
3. **Clean Up.** Return all pieces (favor, secrets, warbands, pawns) to their banks. Consolidate the winner's sites Cradleward with all their cards, so the only empty site slots are Hinterward. **Actor:** none: procedural.
   - Digital: an empty window. The Chronicle stores no pieces, and the Empire's sites are kept Cradlemost first ([ruling 18](ambiguities.md#chronicle-rulings)).
4. **Store.** Store the Empire's sites, with their cards, on top of the Empire divider, Cradlemost on top. Store each player's pieces, advisers and legacies, keeping legacy faces; store the Chancellor's pieces, the Grand Scepter and the Reliquary apart from the players; store the Visions and the world deck behind the World Deck divider. The rulebook's "play again or store" choice does not apply: the digital phase always ends by journaling `ChronicleUpdated` with the evolved Chronicle. **Actor:** none: procedural.
   - Digital: one log line per Empire site. The Chronicle Phase then journals the record, which must pass game start's card accounting and keep every site and relic it started with. The Dispossessed are the older ones followed by the six the World task added.
   - Digital: with Iron Hand, the holder's sites are stored in the Empire's place, Cradlemost first, "Buried Giant stays on the map with Iron Hand". Until Iron Hand's SETUP half lands, the next game restores them as the Empire's sites ([Legacy Chronicle powers rulings](ambiguities.md#legacy-chronicle-powers-rulings), items 44 and 51).

## Card and rulebook differences

The rev1 task cards abbreviate several steps. Where the card and the CR differ,
this document follows the CR:

- The Sun card says the Chancellor or first player breaks goal ties; the CR's friendly/unfriendly tie rules govern.
- The Throne card offers Citizenship to "any players"; only Exiles can accept.
- The Throne card omits the intact-edifice and Enduring exceptions to the Shape Empire discard.
- The Throne card sends discarded and buried edifices "back in the box" and lets you choose any matching edifice; the CR uses the topmost edifice of each suit and puts discarded and buried edifices at the bottom of the edifice deck.
- The Beacon card names the suited box sections "the Archive" and omits the Dispossessed return.
