# Ambiguities and implementation questions

These are not claimed errors in the rulebook. They are places where an implementation needs component data, a product decision, or a more explicit formalization.

## Component-data dependencies

1. **Card powers and exact costs.** The catalog contains stable printed IDs, reviewed text, and handler keys. Most denizens and relics now have executable Scala behavior; the cards still without it are kept in `src/main/scala/oathdigital/catalog/holding/`: 26 denizens, 10 relics, 11 sites, 22 edifices, and all 36 legacies. Many actions can be modified or replaced by these powers (CR pp. 28-30, 42).
2. **Foundation faces.** The catalog contains the six normal/altered records, but their setup effects still require typed handlers verified against CR pp. 10-11.
3. **Player-board tables/icons.** Supply refresh thresholds and some costs are
   graphically encoded on player boards. Trade yield is resolved by the current
   combined rulebook prose: favor is 1 + matches; secrets equal matches (CR
   pp. 18, 24).
4. **Banner ribbons and non-default faces.** CR p. 32 explains ribbon semantics, but machine execution needs exact component-side icon sequences and initial values.
5. **Component behavior coverage.** Printed identities and the runtime catalog are complete, but legacy powers, banner ribbons, and the other cards still in the holding package (item 1) still require typed executable handlers.

## Formalization decisions

1. **Action boundary.** Several automatic checks happen "after any action" (bandit refill, Oathkeeper transfer), while powers can introduce actions and nested effects. Define a transaction boundary and when state-based checks reach a fixed point (CR pp. 15-16, 42).
2. **Simultaneous effects.** Turn player chooses order during a turn; current Chancellor chooses during Chronicle. Define who chooses outside both contexts and whether ordering choices are logged (CR p. 42).
3. **Consent timeout/forgetting.** "Enemies cannot" effects fail if not enforced at the moment. A digital implementation needs an explicit prompt/auto-consent policy so silence is deterministic (CR p. 43).
   Resolved for the digital game: the engine always enforces, and a blocked player asks the ruler explicitly; a ruler may set a standing per-player answer ([consent design](../superpowers/specs/2026-10-08-consent-system-design.md), rulings 3 and 10).
   An official ruling on the Hall of Ministers makes enforcement all-or-nothing: once waived, it lets through every discard that happens at the same time. The engine treats one operation batch as that moment (consent design, ruling 3).
4. **Private information.** The rules permit owners to view/share their private data. Multiplayer UI needs visibility and reveal/audit policies for facedown advisers, relics, drawn cards, and secret selections (CR p. 42).
5. **Unlimited dice vs limited pieces.** Dice are unlimited; other components are limited and extra placement is ignored. Decide whether digital inventory should preserve physical limits exactly (CR p. 42).
6. **Random winner/coin flips.** No-Empire exhaustion can choose randomly, and unclear Tribunal promise breach uses a coin flip. Specify RNG seed and event log behavior (CR pp. 19, 30).
7. **Ties by role/context (resolved only for fixed first-game Supremacy).**
   Supremacy's ordinary qualification is strict: rule more sites than every
   other player, so an initially unheld title is not assigned across a highest
   tie. CR p. 16 then supplies title-specific exceptions: a current holder in
   the highest tie retains it; if the holder is displaced by multiple tied
   leaders, that holder chooses the recipient. The current bounded evaluator
   implements all three cases; the last creates a durable choice visible and
   actionable only by the displaced holder. This is not a generic tie
   resolver; legacy, Vision exhaustion, era, and other ties remain unresolved
   in their own contexts (CR pp. 16, 19, 36-37).
8. **First game exception.** CR recommends ignoring "become Citizen" powers in the first game, wording this as a recommendation rather than a strict prohibition. Product rules need a configurable or explicit interpretation (CR p. 30; CR p. 34 uses similar guidance).
    Resolved in part: a setup option, on by default, ignores these powers and shows them disabled (Empire slice 3a); the Settings Feature phase lets players change it.
9. **Campaign target legality under Pass and zero-capacity sites.** Target generation must combine campaign type, ruler, region Pass consent, and special no-capacity cases (CR pp. 21-22, 30-31).
10. **Chronicle actor availability.** Banner task assignment can target "anyone"; player attendance may change between games. Define whether absent/stored lineages can be assigned tasks or whether "anyone" means current participants (CR p. 36).
    Resolved: only players seated for the game receive tasks ([Chronicle rulings](#chronicle-rulings), ruling 1).

## Empire rulings

Rulings for the Empire in play ([Empire phase design](../superpowers/specs/2026-10-29-empire-phase-design.md), "Rulings"). Slice 1a implements R1 to R3 and the Usurper ruling; slice 3a implements R4, R5 and the Usurper flip of R7.

R1. **One ruler.** On an Imperial player's turn only that player rules Imperial sites; the Chancellor does not also rule them. A site has exactly one ruler (CR p. 15).

R2. **The Chancellor outside any turn.** Outside any turn, as in the end-of-round checks and the Chronicle, the Chancellor rules Imperial sites.

R3. **A Citizen's Supply.** A Citizen refreshes to the Chancellor's current Supply, which includes the Chancellor's own carried surplus, plus their own unspent Supply, capped at 7.

R4. **A mid-round Chancellor.** A Chancellor who appears mid-round leaves the current round's order unchanged; the next round starts with them. The round's first player is recorded when the round begins (CR p. 17; Empire slice 3a).

R5. **The Usurper Limiter.** Its limit applies only in games with no Empire, so it ends, and its marker is removed, when an Empire starts in play (CR p. 19; Empire slice 3a).

R7. **No Imperial Usurper in play.** An Imperial player never holds the Usurper side of the Oathkeeper title during play. An Exile who holds it and becomes a Citizen flips it to Oathkeeper at once (Empire slice 3a implements the flip), so domain validation rejects an Imperial player on the Usurper side outside the Chronicle phase. The check is suspended in the Chronicle phase, where the Chronicle crowns the Usurper winner after the game has ended.

R8. **Warband Transfers consent names the transfer.** The partner is asked after the player has chosen the way and the amount, so the ask names them: "Asked {Blue} for consent to give them 3 warbands." Each consent covers one exact transfer. A refusal stops only that way and amount for the turn; the player may ask about a different one.

R9. **Military Parade counts one enemy.** The New Foundations card applies to the one enemy only. Its "including Imperial Allies" text is a base-game concept and is ignored.

R10. **Plans at Imperial sites.** In a Campaign, plans at Imperial sites belong to the Chancellor's side on every turn. The Chancellor uses them freely; any other Imperial needs the Chancellor's consent, even on their own turn (Empire slice 1b-ii).

R11. **Split costs.** Every cost of a battle plan may be split between Imperial players, including a variable amount. The warband sacrifice still comes from the defender's force (Empire slice 1b-ii).

R12. **Shared requirements.** Any Imperial player who meets a plan's requirement (holding the Darkest Secret, holding the People's Favor, having their pawn at a site) may meet it for the plan's user, with that player's consent. A requirement met this way for a plan that checks it at the end of the Campaign (Hearts and Minds) stays met until then (CR p. 35; Empire slice 1b-ii).

R13. **Captains and Wild Allies at an Empire site.** A Citizen may use Captains or Wild Allies to campaign from a site the Empire holds, against the Chancellor. The site's warbands join the attack and count once, for the attacker, never in the defense; they do not die in the victory, and only the board's survivors are placed. This follows the Captains FAQ in the BGS card library; Wild Allies has no FAQ but the same wording. The FAQ's "you do not need to target the site" refers to a base-game rule and does not apply in New Foundations.

R14. **Keep.** Keep's site is used by whoever defends it in the Campaign, so a defeated Chancellor keeps Keep at an Empire site even on a Citizen's turn.

R15. **Hospital.** Hospital saves warbands only if its user rules its site by the turn's ruler at the Campaign's end, as its FAQ in the BGS card library says ("if you rule it at the end"). A Chancellor defending on a Citizen's turn may choose it at an Empire site (R10), but it saves nothing; a Citizen using it on their own turn has theirs saved.

R16. **What aid covers.** Imperial Tactics lets an ally pay a plan's costs and meet its requirements, and nothing more. A plan that counts what its user rules or holds (Great Crusade, Pledge of Defense, Kindred Warriors, Nature Worship) counts only the user's own cards. An ally's pawn at a site meets "your pawn is at this site" (Towering Rampart) (CR p. 35; Empire slice 1b-ii).

R17. **The Grand Scepter counts.** Under Supremacy, the Successor holds more relics than each other Imperial player (card OATH-265). The Grand Scepter is a relic, so it counts in its holder's total (CR p. 19; Empire slice 2).

R18. **Short purple bank.** When the purple bank holds fewer warbands than a player becoming a Citizen has on their board and at sites, the player splits the bank's warbands over their board and each site that holds their warbands. At each place, the warbands left over return to the player's own bank, so a site can end with fewer warbands, or none (CR p. 34; Empire slice 3a).

R19. **Supremacy title.** Under the Oath of Supremacy, a player who becomes a Citizen while holding the Oathkeeper title passes it to the Chancellor, on either side: Citizens never hold that title (CR p. 35). A player who becomes the Chancellor keeps it, turned to its Oathkeeper side (CR p. 34; Empire slice 3a).

R20. **No one performs a role change.** Becoming a Citizen, from any source, is performed by no player, so a restriction against a player's placements does not stop its warband replacements. Vow of Wandering refuses only a replacement its holder performs (Bandit Prince, Ballot Box). Chronicle steps have no actor (Chronicle ruling 7), so the Vow restricts nothing in the Chronicle phase (Empire slice 3b).

R21. **"The most."** "Have the most secrets" (Bewitch) and "rule the most sites" (Royal Ambitions) mean strictly more than every other seated player: a tie is not the most. Secrets are counted as the player's area shows them, credits for secrets on cards in play going to the active player; sites are the sites the player rules (Empire slice 3b).

R22. **Empty Reliquary.** Bewitch, Ancient Pact and Royal Ambitions still make the player a Citizen when the Reliquary is empty; only the relic is missed. All three take the relic, as their card text says, facedown (Empire slice 3b).

R23. **Either side.** Martial Culture works for an Exile who defeats another Exile player as attacker or defender, and the Grand Scepter's battle plan for an Imperial player who defeats a Citizen as attacker or defender. Its card database wording is followed: "you may exile them", for 3 burnt favor. A defender who becomes a Citizen or is exiled off-turn ends no Act phase (Empire slice 3b).

R24. **Queued actions after a role change (provisional).** Actions a player has queued, such as Second Wind's Travel and Campaign, still run after a role change ends that player's Act phase (Empire slice 3b). The Martial Culture FAQ suggests Second Wind's second Campaign does not happen once its holder has become a Citizen; revisit this.

## Chronicle rulings

Rulings given by the project owner for the Chronicle Phase ([chronicle.md](chronicle.md)). The rev1 task cards are slightly older than the CR; where they disagree, the CR governs.

1. **Task recipients.** Only players who were seated for the game can receive a Chronicle task (CR p. 36).
2. **Unheld banners.** "If no one holds a banner" is two rules: if no one holds the People's Favor, the winner chooses who takes the World task; if no one holds the Darkest Secret, the winner chooses who takes the Beacon task (CR p. 36).
3. **One player holds World and Beacon.** That player chooses which task to give away and to whom (CR p. 36).
4. **Legacy goal ties.** The CR's friendly (Imperial-only) and unfriendly (any Exile) tie rules govern. The Sun card's "The Chancellor or first player breaks goal ties" is outdated (CR p. 36).
5. **Citizenship.** Only Exiles can be offered Citizenship, including players who became Exiles earlier in the same Chronicle when an Exile won (CR pp. 36-37). The Throne card's "any players" is outdated.
6. **Shape Empire exceptions.** Intact edifices and cards at Enduring sites are not discarded, as the CR says; the Throne card omits both. Some legacies also protect denizens; those arrive with legacy powers (CR p. 37).
7. **Actors.** Most Chronicle steps have no actor and run procedurally. The Stars task has no actor-bound step except Write in the Stars, which the digital version does not implement (CR pp. 40-41).
8. **Play again or store.** The digital phase always ends by journaling the evolved Chronicle; the CR's choice between playing again and storing does not apply (CR p. 41).
9. **Edifice order.** Edifices discarded during play (for example a ruined edifice discarded by a homeland power) or in Shape Empire must not return in Build Edifice while another edifice of their suit is available. The end of Build Edifice shuffles the whole edifice deck, so no order survives between games. The digital version keeps an ordered set-aside edifice list on the live state, empty at game start. Build Edifice offers a random edifice of each suit from the edifices not set aside, and falls back to the first set-aside edifice of that suit only when none is left. The list is not part of the Chronicle (CR p. 37).
10. **Grand Scepter with no Chancellor.** When no one holds the Grand Scepter (a first game leaves it out of the relic pool), the winner takes it from the box into their relics, faceup, during Crown Chancellor. If a player holds it, it moves from that player (CR p. 36).
11. **The Chancellor's warbands in the Throne task.** After Crown Chancellor, "your warbands" in Shape Empire are the Imperial warbands, "sites you rule" are the sites the Empire rules, and "the last warband of yours on the map" is the last Imperial warband. If the Empire rules no sites, the fallback places one Imperial warband at the winner's pawn site (CR p. 37).
12. **Built edifice face.** An edifice placed by Build Edifice is placed intact (CR p. 37).
13. **Lineage point gifts.** The winner may give lineage points only to players who were seated for the game, as for task recipients in ruling 1 (CR p. 37).
14. **Shape Empire discard pile.** A denizen discarded in Shape Empire follows the general discard rule: it goes to the discard pile of the region after its site's region. The World task's Gather Denizens shuffles every discard pile together next, so the pile matters only to the Game Log (CR pp. 37-38).
15. **Newcomers from a short suit.** If a suit the Beacon task holder chose still has too few cards after all older Dispossessed return to their suit sections, the holder draws what remains of that suit. The project owner notes this should not happen in play (CR p. 39).
16. **Locks in the Chronicle.** Lock icons restrict play, not the Chronicle Phase. Gather Denizens takes every adviser, locked or not. Rules that name a protected card, such as Shape Empire's intact edifices, still protect it (CR pp. 37-38).
17. **Rival sites keep every card.** Archive Rival Sites stores every card left on a rival site with it, not only relics and intact edifices. After Shape Empire, a rival Enduring site can still hold denizens or a ruined edifice; they are stored too (CR p. 40).
18. **The Stars on the table.** The Stars steps change nothing on the table. The Chronicle stores no pieces, so Clean Up's piece returns change nothing it records. The digital version reads the finished table once, at the end of the Chronicle Phase, and stores the sites as Archive Rival Sites, Clean Up and Store order them (CR pp. 40-41).
19. **The scorepad's roles.** The era scorepad records each player's role when Score Lineage Points records their score, after the Sun has crowned, so a winning Exile is recorded as Chancellor ([chronicle.md](chronicle.md), the Throne, step 6: "circle the Chancellor's score").

20. **A broken Empire's sites.** When an Exile wins, each site that held Imperial warbands takes one warband of the former Chancellor's colour once the Imperial warbands are removed, so the former Chancellor rules it as an Exile through the rest of the Chronicle. This deviates from CR p. 36, which leaves those sites empty and has the Throne place an Imperial warband on each site with no warbands if the former Chancellor accepts Citizenship. A former Chancellor who declines keeps the sites; a boxed former Chancellor places none, and those sites stay empty (Empire slice 3b).

21. **Offer Citizenship.** The Throne holder offers Citizenship to any seated Exiles, none included; boxed lineages are never offered. Each offered Exile answers in seat order, Decline first; answering at the same time waits for the walker's simultaneous step. An Exile who accepts becomes a Citizen, returns their board warbands, and keeps one Imperial warband at each site they rule. No other Citizenship step, and not the first-game option, applies in the Chronicle (CR p. 37; Empire slice 3b).

22. **Unlimited purple at the Throne.** Offer Citizenship assumes an unlimited purple supply, since it marks sites as the Empire's; purple warbands the bank lacks are added to it (CR p. 37; Empire slice 3b).

## Source relationship cautions

- NF is intentionally a change overview. When its simplification omits a detail, use CR, not inference.
- The PDFs are dated May/June 2026. No errata, FAQ, card database snapshot, or base-edition rulebook was provided, so this layer cannot validate later corrections or reconstruct every old-vs-new sentence.
- Printed glyphs/icons do not always survive text extraction semantically. Numeric prose was cross-checked visually, but icon-only component details should be ingested from structured component data before implementation.
