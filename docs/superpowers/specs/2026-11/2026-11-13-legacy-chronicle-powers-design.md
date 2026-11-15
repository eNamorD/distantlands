# Legacy Chronicle powers

**Status:** designed 2026-11-13, approved section by section by the owner.
All five slices landed (plans:
[slice 1](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice1.md),
[slice 2](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice2.md),
[slice 3](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice3.md),
[slice 4](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice4.md),
[slice 5](../../plans/2026-11/2026-11-13-legacy-chronicle-powers-slice5.md));
its implementation rulings are in the
[rulings file](2026-11-13-legacy-chronicle-powers-implementation-rulings.md).

**Builds on** the [Legacies design](2026-11-13-legacies-design.md), which
landed the legacy lifecycle and left every power in holding, and the
[Chronicle steps design](../2026-10/2026-10-27-chronicle-steps-design.md),
whose "Windows, not hardcoded actors" decision put every Chronicle step in
a power window for these powers to use.

## Goal and scope

The CHRONICLE halves of the legacies, and the one SETUP rule a Chronicle
half needs to mean anything (Oaths of Loyalty's burial). Thirteen legacies:

| Slice | Powers | Hooks |
|---|---|---|
| 1. Seam and Sun | Steadfast, Gatekeeper, Chronicler | `ChronicleLegacyRules`; Sun steps 2-5; the scored-legacies record |
| 2. Throne denizens | Rival (Throne half), Great Architect, Revolutionary, Reformer | Start of Throne, Shape Empire, Build Edifice, after Build Edifice |
| 3. Relics and the next game | Heirloom, The Ear (Chronicle half), Oaths of Loyalty, The Bright Beacon | Scatter Relics, Gather Denizens, the starting-adviser keeps, Setup's adviser pick, Introduce Newcomers |
| 4. Iron Hand | Iron Hand (Chronicle half and its discard rule) | Sun, Throne, Score Lineage Points, Archive Rival Sites, Store |
| 5. Royal Line | Royal Line | A new first Sun window; the game's recorded winner |

Slices run in order. Slice 1 builds the seam the rest use. Iron Hand comes
after slice 2 because it changes who performs the steps slice 2 touches,
and Great Architect's text names it. Royal Line comes last because changing
the winner reaches every later step, the log and the result display.

**Out of scope:** every other SETUP half (Iron Hand, Circle of Swords,
Scepter Bearer, World Crafter, Matchmaker, Populist, Rival, The Standard
Bearer, Seer, Secret Society, Quest to Distant Lands, Wealthy); every power
used in play (The Ear's and the others' ACTION, WAKE, TRAVEL, BATTLE PLAN,
END OF ROUND and WHEN halves); Iron Hand's discard during a game; and the
non-legacy cards that act on legacies. They stay on the ROADMAP.

## Rulings

Taken with the owner on 2026-11-13. They open a "Legacy Chronicle powers
rulings" section of `docs/rules/ambiguities.md`, which the implementation
rulings then continue.

### General

1. **Only seated players' active legacies act.** A legacy's Chronicle power
   applies when a seated player holds it active at the moment its window is
   reached. A boxed lineage's legacies never act; a dormant legacy has no
   power (glossary, CR pp. 33 and 36).
2. **"If you are an Imperial" or "a Citizen" is read at the same moment.**
   A role condition is checked when the window is reached, so the Crown and
   Offer Citizenship change it for every later step.
3. **Every "you may" is the holder's decision.** It has its own decision id,
   `chronicle.<task>.legacy.<power>.<player>`, so no two decisions share an
   id (the Crown cap fault in Legacies slice 3). Declining is always an
   option.
4. **Choosing several cards is one choose-many.** Where a power picks any
   number of cards, the holder answers one `ChooseMany`, not a loop of
   single picks.

### Slice 1: Sun

5. **Steadfast is read at the start of step 3.** If the holder holds it
   active then, none of their other legacies go dormant in step 3.
   Steadfast itself can go dormant. Step 4's cap still discards extra
   legacies, since being discarded is not becoming dormant.
6. **Gatekeeper is a new step between Sun steps 1 and 2.** "Before players
   resolve legacies" comes after the relics are revealed. The holder may
   take one active legacy from an enemy whose goal the holder meets, as
   `LegacyGoals` counts it at that moment, or decline. The taken legacy
   joins the holder active. Step 2 then alters its foundation if it
   alters one, and step 4 applies the cap.
7. **Chronicler is a new step after Sun step 5.** If the holder declares,
   every seated player scores their legacies now: the bonus Score Lineage
   Points computes (each active legacy held, 1 point per card of its suit
   at the sites the player rules; Legacies R31-R33; dormant legacies do not
   score, implementation ruling R1, re-ruled by the owner 2026-11-14), with
   no 3 base points and no gifts. Each player who scores at least one point logs one line in the
   bonus format.
8. **A legacy scores once per game.** A new `ChronicleOperation` records
   the legacies scored so far this Chronicle, by legacy, not by player.
   Score Lineage Points and Iron Hand's scoring skip them.

### Slice 2: Throne

9. **Rival protects with one choose-many.** In Shape Empire, after the
   warbands are removed, the holder chooses any of the denizens the step
   would discard. Those stay; the rest are discarded as before.
10. **Great Architect performs Build Edifice instead of the Throne holder.**
    For an Imperial holder, the step's actor is the holder. Burying a
    denizen at a site they rule, the holder may place any of the edifices
    set aside as available (the topmost of each suit, at most six), not
    only the one matching the buried denizen's suit. It is never the whole
    edifice deck. When the holder is the Throne holder, only the matching
    rule changes.
11. **Revolutionary discards with one choose-many** after Build Edifice:
    any of the denizens at Imperial sites. Discards go where Shape Empire
    sends them (Chronicle rulings 9, 11 and 14).
12. **Reformer is a new step at the start of the Throne**, only when the
    holder won. The holder moves a denizen to another site they rule that
    has room, or swaps two denizens between sites they rule, ignoring
    locks, and repeats until they choose Done. Each move or swap is one
    decision, read when it is reached.

### Slice 3: relics and the next game

13. **Heirloom is a new step before Scatter Relics.** The holder secretly
    chooses one of their relics, never the Grand Scepter, or none when they
    hold no other relic. It leaves play into their lineage's
    `startingRelics`, which game start already hands back facedown. Other
    viewers see "p2 kept a relic with Heirloom"; the holder sees its name.
    A new `ChronicleOperation` records it.
14. **The Ear makes Scatter Relics the holder's choice.** The holder sees
    the relics being scattered and, slot by slot in the step's Hinterward
    order, chooses which relic goes in each empty slot. The relics left
    return to the relic deck as before. Only the holder sees the relics'
    faces; the deal stays facedown for everyone else.
15. **Oaths of Loyalty keeps up to three faceup advisers.** In Gather
    Denizens the holder answers a choose-many of zero to three of their
    faceup advisers, even when they hold more than three. The chosen ones
    are not shuffled in. They become the lineage's facedown starting
    advisers for the next game, with their tokens returned as ruling 16
    returns every adviser's.
16. **Oaths of Loyalty buries every other starting adviser.** Any other
    starting adviser the holder would gain is buried at the bottom of the
    world deck instead:
    - the card kept in Protect and Dispossess or Welcome a Stranger;
    - in the next game's Setup, the adviser chosen from a hand of three.

    Under Teeming World, the holder draws no starting advisers beyond the
    ones Oaths kept. The cards Quest to Distant Lands swaps in are exempt,
    as that card says, but Quest is a SETUP half and out of scope.
17. **The Bright Beacon chooses its source first.** If the holder is
    resolving the Beacon, they first choose, before seeing the Dispossessed,
    whether to take newcomers from the Dispossessed or from the suited box
    sections (the ordinary step). From the Dispossessed they then see every
    Dispossessed card, the six the World just added included (so there are
    always at least six), and answer a choose-many of exactly six. They
    never mix the two sources. Welcome a Stranger then runs on the six
    taken.

### Slice 4: Iron Hand

18. **Iron Hand is discarded when its holder is an Imperial.** It returns
    to the legacy deck at four Chronicle checkpoints: the start of the
    Sun, before the Sun's legacy cap (step 4), after Crown Chancellor, and
    after Offer Citizenship. The checkpoint before the cap was added by
    implementation ruling R46. Discarding during a game belongs to the
    in-play batch.
19. **Iron Hand takes over Shape Empire and Build Edifice.** While a seated
    Exile holds it active, the Empire's Shape Empire and Build Edifice are
    skipped; the holder performs both over the sites they rule. Great
    Architect's "even if the Iron Hand is in play" reads as this: an
    Imperial Great Architect performs Build Edifice instead of the holder,
    who then does not build (implementation ruling R32, re-ruled by the
    owner 2026-11-14).
20. **Iron Hand scores its holder's legacies.** In Score Lineage Points,
    after the winner, the holder scores the legacy bonus at the sites they
    rule, with no base points and no gifts, skipping legacies already
    scored (ruling 8).
21. **Iron Hand's sites are stored in place of the Empire's.** In Archive
    Rival Sites, the Empire's sites are archived as rival sites and the
    holder's are not. Store keeps the holder's sites, Cradlemost first, in
    the Chronicle's list of sites that stay on the map.

### Slice 5: Royal Line

22. **Royal Line chooses the winner of an Empire win.** It applies only when
    the recorded result is a Chancellor win or a Successor win, and the
    holder was a Citizen holding it active when the game ended. In a new
    window before Reveal Relics (moved before Start Chronicle by Legacy
    in-play powers implementation ruling R36), the holder chooses the winner from every
    Imperial player, the Chancellor included. Choosing the Chancellor is
    choosing no one. The owner's shape: the question offers Decline first,
    then every Imperial player, the Chancellor first; declining or
    choosing the current winner keeps the result (implementation ruling
    R38, re-ruled by the owner 2026-11-14).
23. **The new winner is journaled.** A new `ChronicleOperation` records it,
    and every later step reads the winner from the game's result. A Citizen
    wins as the Successor; the Chancellor wins by the Empire win that ended
    the game, which the operation carries when a Successor result does not
    keep it. The log line is "p3 named p2 the winner with Royal Line".

## Approach

- **Cards leave holding.** Each implemented legacy becomes a card class
  outside `catalog/holding/HoldingLegacies.scala` and declares its
  Chronicle half as a `ContributingPower`, as `ChronicleWindowsSuite`'s
  fake power does. `PowerImplementationStatus` lists it implemented. A card
  with an uncovered half keeps that half's text, and the ROADMAP item for
  that half still names it.
- **In effect is read live.** `ChronicleLegacyRules.holder(state, catalog,
  legacy)` returns the seated player holding that legacy active, after any
  role condition (rulings 1 and 2). Every Transform and every query asks
  it when its window is reached, never when the walk is built. This
  avoids the bug class Legacies slice 3 found: a Branch whose children or
  ids depend on state the walk itself changes.
- **Two hook kinds.**
  - *Added steps* put operations into their window through a `Transform`:
    Gatekeeper, Chronicler, Reformer, Revolutionary, Heirloom, Royal Line,
    and Iron Hand's discard checkpoints and scoring.
  - *Changed steps* ask `ChronicleLegacyRules`, which keeps each step one
    piece of code: Steadfast (`keepsOthersActive`), Rival (`protector`),
    Great Architect (`builder`, `anyEdifice`), The Ear (`scatterer`),
    Oaths of Loyalty (`keepsAdvisers`, `buriesStartingAdvisers`), The
    Bright Beacon (`beaconFromDispossessed`), and Iron Hand (`ironHand`,
    read by Shape Empire, Build Edifice and the Stars).
- **New windows** only where a step is added between existing ones: the
  Gatekeeper window before `ChronicleFlipLegacies`, Chronicler's after
  `ChronicleShuffleLegacies`, Reformer's before `ChronicleOfferCitizenship`,
  Revolutionary's after `ChronicleBuildEdifice`, Heirloom's before
  `ChronicleScatterRelics`, and Royal Line's before
  `ChronicleRevealRelics`. Each is empty with no power, and
  `ChronicleWindowsSuite` lists it.
- **Journal.** New `ChronicleOperation`s only where existing moves cannot
  say it: the scored legacies (ruling 8), Heirloom's kept relic
  (ruling 13) and Royal Line's winner (ruling 23). Oaths' kept advisers
  (ruling 15) are stored the way Protect and Dispossess stores its kept
  card. Each is replayed by the
  materializer and covered by the codec suites.
- **Hidden choices.** Heirloom's relic, The Ear's examined relics and The
  Bright Beacon's Dispossessed are shown to the holder only; the
  projection redacts them for others as it redacts facedown relics.
- **Log.** One line per power used, in the Legacies lines' style:
  "p2 took Seer from p3 with Gatekeeper", "p2 declared Chronicler",
  "p1 protected 2 denizens with Rival", "p2 discarded 3 denizens with
  Revolutionary", "p2 kept a relic with Heirloom", "p3 took six newcomers
  from the Dispossessed with The Bright Beacon", "p2 discarded Iron Hand".

## Slice 1: seam and Sun

`ChronicleLegacyRules` and its `holder`; the scored-legacies operation;
the Gatekeeper and Chronicler windows; Steadfast, Gatekeeper and
Chronicler leave holding. Score Lineage Points skips scored legacies.

Tests: each power's suite starts from a Chronicle state at its step; a
resume test for each new `Decide`; Steadfast going dormant itself while
protecting the others; Gatekeeper taking a foundation-altering legacy,
then the cap; Chronicler followed by Score Lineage Points scoring only
the base for the winner's already-scored legacies; a boxed lineage's
active Steadfast doing nothing. A golden log for a Chronicle where
Gatekeeper and Chronicler both act.

## Slice 2: Throne denizens

The Reformer and Revolutionary windows; Shape Empire reads `protector`;
Build Edifice reads `builder` and `anyEdifice`. Rival's Throne half,
Great Architect, Revolutionary and Reformer leave holding. Rival leaves
with its whole text and counts as implemented; the ROADMAP setup item
names its SETUP half (implementation ruling R7).

Tests: Rival keeping some denizens; Great Architect as a Citizen building
an off-suit edifice in place of the Chancellor, and as the Chancellor;
Revolutionary choosing none and several; Reformer moving a locked denizen,
swapping two, and refusing a full site; Reformer not asked when its holder
lost. Resume tests for each decision.

## Slice 3: relics and the next game

The Heirloom window; Scatter Relics reads `scatterer`; Gather Denizens,
Protect and Dispossess, Welcome a Stranger and Setup's adviser pick read
Oaths' queries; Introduce Newcomers reads `beaconFromDispossessed`.
Heirloom, The Ear's Chronicle half, Oaths of Loyalty and The Bright Beacon
leave holding.

Tests: Heirloom's relic reaching the next game's start facedown, and its
redaction for other viewers; The Ear placing chosen relics and the rest
returning to the deck; Oaths keeping three of five faceup advisers, then
burying a Protect keep, then the next Setup burying the hand pick, then
Teeming World dealing the holder nothing; The Bright Beacon choosing the
box (ordinary step) and the Dispossessed (six chosen, Welcome a Stranger
on those six). A golden log for an Oaths and Heirloom Chronicle into the
next game's setup.

## Slice 4: Iron Hand

The discard checkpoints; Shape Empire and Build Edifice skipped for the
Empire and run for the holder; the holder's scoring after the winner's;
Archive Rival Sites and Store keeping the holder's sites. Iron Hand's
Chronicle half leaves holding (its SETUP half stays).

Tests: an Exile holder taking over both steps; an Imperial Great
Architect still building beside Iron Hand; Iron Hand discarded when its
holder becomes a Citizen at Offer Citizenship; the holder's scoring
skipping legacies Chronicler scored; the stored Chronicle keeping the
holder's sites and archiving the Empire's, passing game start's card
accounting. A golden log of the whole Chronicle.

## Slice 5: Royal Line

The Royal Line window; the winner operation; every step and the
presentation read the winner from the result. Royal Line leaves holding.

Tests: Royal Line naming a different Citizen, and the Chancellor; not
asked on an Exile's or Vision win, nor when its holder was an Exile at
game end; the Crown, Assign Tasks and Score Lineage Points following the
new winner; the result display and log naming the new winner. A golden
log.

## Docs

Each slice updates, as it lands:

- `docs/rules/chronicle.md`: a "Digital:" note on each step a power
  changes;
- `docs/rules/ambiguities.md`: the rulings above;
- `docs/rules/implementation-traceability.md`: each legacy's row;
- `docs/ROADMAP.md`: the "Legacy powers: Chronicle" item dropped when
  slice 5 landed (implementation ruling R44); the setup and in-play items still name Iron Hand's,
  Rival's, Oaths' and The Ear's other halves, and the in-play item gains
  Iron Hand's discard during a game.

## Out of scope

- The SETUP and in-play halves listed under Goal and scope.
- Iron Hand's discard during a game (ruling 18).
- The cards that act on legacies (Ancient Writ, Amber Doors, Lost Doors,
  Ruined Temple, Stone Baths, Ruined Baths).
- Eras and the reset of lineage points.
