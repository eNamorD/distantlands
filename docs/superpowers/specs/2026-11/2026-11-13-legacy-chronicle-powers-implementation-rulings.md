# Legacy Chronicle powers implementation rulings

Rulings the owner's orchestrator made while the
[Legacy Chronicle powers design](2026-11-13-legacy-chronicle-powers-design.md)
was implemented, on questions the design and its slice plans did not
settle. Each entry gives the question, the decision and the reason, the
confidence when the orchestrator marked it low, and the slice and files it
touches. The design's own rulings 1-23 are not repeated here.

## Slice 1

**R1. Do dormant legacies score under Chronicler?**
Re-ruled by the owner 2026-11-14.
Decision: no. Dormant legacies do not score under Chronicler, Score
Lineage Points or Iron Hand's scoring. `LineageScoring.unscored` reads a
player's active, unscored legacies, and `LineageScoring.bonus` counts
those; all three scorers reuse them. The winner never holds a dormant
legacy at Score Lineage Points (an Exile winner's newly dormant ones
reactivate at the Crown), so the winner's score is unchanged, as the
golden logs show.
Reason: the owner's ruling: a dormant legacy has no effect, scoring
included. This narrows Legacies R33 ("every legacy the winner holds").
Earlier decision (2026-11-13): every legacy held scored under Chronicler,
dormant ones included.
Slice/files: 1 (changed in 5); `LineageScoring.unscored`, `Chronicler`,
`IronHand.scoring`.

**R2. Which legacies `LegaciesScored` records.**
Re-ruled by the owner 2026-11-14, following R1.
Decision: only the legacies that actually scored: each seated player's
active, unscored legacies when Chronicler resolves, those that score 0
points included. A legacy dormant when Chronicler resolves is not
recorded and may score later, such as an Exile winner's, reactivated at
the Crown.
Reason: a legacy scored for 0 has still been scored, and denying the
winner's later bonus is the card's point; a dormant one has not scored.
Earlier decision (2026-11-13): every legacy held was recorded, dormant
ones included.
Slice/files: 1 (changed in 5); `Chronicler`, `LegaciesScored`.

**R3. The scorepad's roles when Chronicler scores before the Crown.**
Decision: `RoleChanged` updates that lineage's entry in this game's open
era row, so its `endingRole` follows the lineage's role.
Reason: keeps Chronicle ruling 19 (roles after the Crown) without delaying
Chronicler's points from its log line.
Slice/files: 1; `ChronicleStateOperations.roleChanged`.

**R4. Chronicler's and Gatekeeper's lines.**
Decision: "p2 declared Chronicler" (a decision line) and, for each player
who scores at least one point, "p1 scored 2 lineage points (2 from
Beast)", the bonus format without the base (a delta line); Gatekeeper's
"p2 took Seer from p3 with Gatekeeper" (a decision line). Each legacy in a
line is a card span.
Reason: the plan's proposal, in the Legacies lines' style.
Slice/files: 1; `Chronicler`, `Gatekeeper`, `ProcedureLog`.

**R5. The two new Sun windows.**
Decision: `PowerWindow.ChronicleBeforeResolveLegacies`
(`chronicle.sun.before-resolve-legacies`), between Reveal Relics and Flip
Legacies, and `PowerWindow.ChronicleAfterResolveLegacies`
(`chronicle.sun.after-resolve-legacies`), between Shuffle and Crown.
Reason: named after the cards' printed timings; window keys are not
saved.
Slice/files: 1; `PowerWindow`, `ChronicleWindows`, `ChronicleSunProcedure`.

**R6. The decisions' shapes.**
Decision: Chronicler is a choose-one of two buttons, Decline first, then
Declare, asked whenever its holder holds it active. Gatekeeper is a
choose-one, Decline first, then one option per takeable legacy (a legacy
option, its owner named), asked only when at least one legacy is
takeable; no choose-many and no "Take" confirm. The ids are
`chronicle.sun.legacy.<power>.<player>`.
Reason: one answer, the same shape as Chronicler; design ruling 4's
choose-many is for picking several cards, not one.
Slice/files: 1; `Chronicler`, `Gatekeeper`.

**R15. Inserted windows and a walk saved mid-Chronicle.**
Decision: accepted. Inserting Chronicle windows shifts the child indices
of a parked Sun or Throne walk, so a game saved mid-Chronicle by an
earlier build may not resume correctly.
Reason: the project makes no mid-phase save compatibility promise across
builds, and the Legacies slices inserted Sun steps the same way; the same
applies to every window slices 2-5 add.
Slice/files: all; `ChronicleWindows`, `ChronicleSunProcedure`,
`ChronicleThroneProcedure`.

## Slice 2

**R7. Does Rival leave holding?**
Decision: yes. Rival leaves holding with its whole text and counts as
implemented; the ROADMAP setup item names its SETUP half. The Ear, Oaths of
Loyalty and Iron Hand do the same in their slices. The design's slice 2
sentence is corrected to match.
Reason: the design's Approach and `CardCatalogSuite` both require it; the
ROADMAP is where unbuilt halves are tracked.
Slice/files: 2; `Rival`, `docs/ROADMAP.md`, the design.

**R8. What Rival protects.**
Decision: every card Shape Empire would discard, ruined edifices included.
Reason: an edifice is a denizen (glossary), and Rival prevents discards, so
its reach is exactly the discard's.
Slice/files: 2; `ShapeEmpire`, `Rival`.

**R9. What Revolutionary discards.**
Confirmed by the owner 2026-11-14.
Decision: the denizens and ruined edifices at Imperial sites; locked cards
stay, intact edifices and any locked denizen.
Reason: the owner's ruling: Revolutionary cannot discard a locked card.
`Discard.RuinedEdifice` is the only edifice discard, and Chronicle ruling
16's protection holds. Every locked denizen in the catalog is adviser-only,
so in play only intact edifices are locked at a site; the code checks the
lock on any card.
Slice/files: 2 (lock check widened in 5); `Revolutionary`.

**R10. What Reformer moves and swaps.**
Confirmed by the owner 2026-11-14.
Decision: denizens, edifices included, intact and ruined.
Reason: an edifice is a denizen, and "ignoring the locked restriction" only
has force for locked cards such as intact edifices.
Slice/files: 2; `Reformer`.

**R11. Reformer's pass.**
Decision: two choose-ones per pass: a card at a site the holder rules, or
Done (first), offering only cards with a destination; then its
destination, another ruled site with room (a move) or a card at another
ruled site (a swap). A swap is two moves, each card landing in the other's
site's last slot.
Reason: board picks scale; dozens of buttons do not.
Slice/files: 2; `Reformer`.

**R12. The lines and the windows.**
Decision: "p1 protected 2 denizens with Rival", "p2 discarded 3 denizens
with Revolutionary" ("1 denizen" for one), "p1 moved X to Y with Reformer"
and "p1 swapped X and Y with Reformer", each a decision line; Great
Architect writes no line of its own; choosing nothing writes nothing. The
windows are `PowerWindow.ChronicleStartOfThrone`
(`chronicle.throne.start-of-throne`), before Offer Citizenship, and
`PowerWindow.ChronicleAfterBuildEdifice`
(`chronicle.throne.after-build-edifice`), between Build Edifice and Stock
Reliquary.
Reason: the plan's proposal.
Slice/files: 2; the four powers, `PowerWindow`, `ProcedureLog`.

**R13. The decisions' shapes.**
Decision: Rival and Revolutionary each ask one choose-many of zero to all,
only when there is a card; Reformer asks
`chronicle.throne.legacy.reformer.<player>` and its `.to` destination each
pass, the ids reused across passes and read from the latest answers, as
Call for Help does; Great Architect keeps Build Edifice's per-suit
decisions, owned by the builder. A resume test parks mid-pass after at
least one completed move and resumes to the same next question.
Reason: the plan's proposal; reused ids across passes are the
parked-decision bug class, so they get their own resume test.
Slice/files: 2; the four powers, `BuildEdifice`.

**R14. Burial favor and late edifice discards.**
Decision: Build Edifice's burial returns favor to the buried card's own
suit bank. A ruined edifice Revolutionary discards after Build Edifice is
not left set aside: R16 returns it to the edifice deck. A test checks the
stored Chronicle still passes game start's card accounting.
Reason: the favor follows the card that leaves; game start rebuilds the
edifice deck from the catalog.
Slice/files: 2; `BuildEdifice`, `Revolutionary`.

**R16. A ruined edifice Revolutionary discards.** (Narrows R14's second
half.)
Question: `ChronicleExtraction` refuses a card left on the set-aside
edifice list, so a ruined edifice Revolutionary discards after Build
Edifice stopped the Chronicle Phase ("the Chronicle cannot store E27
(SetAsideEdifices)").
Decision: after its discards, Revolutionary returns each ruined edifice it
discarded from the set-aside list to the edifice deck, with the same move
Build Edifice's closing step uses. R9 stands.
Reason: it restores the invariant that the set-aside list is empty after
Build Edifice, matches the CR's "bottom of the edifice deck", and needs no
new move kind; the R14 test proves game start's accounting.
Slice/files: 2; `Revolutionary`.

## Slice 3

**R17. Heirloom held by the Throne holder.**
Decision: the printed timing: Heirloom's step comes after Stock Reliquary,
so a Throne holder with Heirloom keeps nothing Stock Reliquary took.
Reason: "before Scatter Relics" is printed, and Stock Reliquary takes the
winner's relics by rule.
Slice/files: 3; `Heirloom`, `ChronicleWindows`.

**R18. Heirloom's choice.**
Decision: a choose-one over the holder's relics but the Grand Scepter, with
no Decline; a single relic is kept without a question; holding none asks
nothing.
Reason: the card has no "may"; it matches Legacies R9.
Slice/files: 3; `Heirloom`.

**R19. Heirloom's lines.**
Decision: a public "p2 kept a relic with Heirloom" for every viewer, plus a
line only the holder reads naming the relic.
Reason: no new note feature; which relic left is the only secret, and the
holder's line keeps their log complete.
Slice/files: 3; `Heirloom`, `ProcedureLog`.

**R20. The kept relic's operation.**
Decision: `StartingRelicKept(player, relic)`, wire kind
`"starting-relic-kept"`: the relic leaves the player's relics for their
lineage's `startingRelics`; the Grand Scepter, a relic not held and a relic
with tokens are refused, and the walk first returns the relic's secrets to
its holder, facedown.
Reason: one operation; its refusals guard the invariants.
Slice/files: 3; `ChronicleOperation`, `ChronicleStateOperations`, the codec.

**R21. The Ear's choices.**
Decision: slot by slot in Hinterward order, each read when reached, under
one reused decision id, `chronicle.throne.legacy.the-ear.<player>`: a slot
is asked whenever two or more relics remain set aside; the last remaining
relic is placed without a question; the walk stops when relics or empty
slots run out, the rest returning to the deck as before. Each placed relic
is peeked again for the holder at its site. A resume test parks
mid-scatter, as R13 asked for Reformer.
Reason: per-slot picks match ruling 14; reused ids are the parked-decision
bug class.
Slice/files: 3; `TheEar`, `ReliquaryAndRelics`.

**R22. Showing hidden cards to their holder alone.**
Decision: the step journals a `Peek` of each card for the holder, and
`identifiesCard` gains cases for the set-aside relics, the Dispossessed and
the newly Dispossessed that read that knowledge. A test checks that another
seated player and a spectator still see those cards hidden.
Reason: one existing mechanism, which replay derives; the holder keeps
knowing cards they really saw.
Slice/files: 3; `TheEar`, `TheBrightBeacon`, `GamePresentationProjector`.

**R23. Oaths of Loyalty in Gather Denizens.**
Decision: before the gathering, a choose-many of zero to three over the
holder's faceup denizen advisers, id
`chronicle.world.legacy.oaths-of-loyalty.<player>`; each kept adviser's
tokens return as Gather Denizens returns every adviser's, and it moves
facedown to the lineage's starting advisers. The line names the kept
advisers: "p2 kept X and Y with Oaths of Loyalty".
Reason: up to three by owner ruling 15; faceup advisers were public, so the
line may name them.
Slice/files: 3; `OathsOfLoyalty`, `GatherDenizens`.

**R24. Oaths' burial of other starting advisers.**
Re-ruled by the owner 2026-11-14 for Teeming World; not yet implemented.
The owner assigned the change to the Setup powers session.
Decision: the keep questions stay (Protect and Dispossess, Welcome a
Stranger, the next Setup's hand of three) and the kept card is buried at
the bottom of the world deck instead: "p2 buried X with Oaths of Loyalty".
Under Teeming World the Oaths holder draws 3 - x cards, where x is the
number of starting advisers they already have, and buries every card
drawn. The shape:
- `SetupDeal.of` gives an Oaths holder `3 - x` draws under Teeming World;
- `SetupDeal` gains `buried(stack, player)`, which replaces `kept` for the
  holder;
- `FirstGameSetupMaterializer` places those cards at the bottom of the
  world deck, below the Visions, instead of as advisers, so the deal's
  card accounting stays exact;
- Setup writes one line per burial, "p2 buried X with Oaths of Loyalty",
  naming the card only to viewers who know it, as the Setup burial does
  now;
- tests for x = 0, 1 and 3.
Reason: one rule applied uniformly ("would gain, bury it"), with no
special-case paths; the owner's ruling extends it to Teeming World's
draws.
Earlier decision (2026-11-13): under Teeming World an Oaths holder drew no
starting adviser.
Slice/files: 3 (to change); `StartingAdviserChoice`, `SetupProcedure`,
`SetupDeal`, `FirstGameSetupMaterializer`.

**R25. The Bright Beacon's choices.**
Decision: a first choose-one, "Suited box" (first) or "The Dispossessed",
id `chronicle.beacon.legacy.the-bright-beacon.<player>`; from the
Dispossessed, a `Peek` of each card for the holder, then a choose-many of
exactly six over the older then the newly Dispossessed, denizens only, id
`...<player>.cards`; when exactly six remain they are taken without a
question. The six go to the holder's temporary hand and Welcome a Stranger
runs on them. Line: "p3 took six newcomers from the Dispossessed with The
Bright Beacon".
Reason: ruling 17 as the owner ruled it; a choose-many over all its
options is not a legal question.
Slice/files: 3; `TheBrightBeacon`, `IntroduceNewcomers`.

**R26. The new window.**
Decision: `PowerWindow.ChronicleBeforeScatterRelics`
(`chronicle.throne.before-scatter-relics`), between Stock Reliquary and
Scatter Relics.
Reason: the plan's proposal.
Slice/files: 3; `PowerWindow`, `ChronicleWindows`.

**R27. The golden logs.**
Decision: two goldens: the Chronicle where Oaths of Loyalty and Heirloom
act, and the next game's Setup started from that Chronicle's journaled
record.
Reason: each game is its own stream; the second golden proves the record
reaches the next Setup.
Slice/files: 3; `ChronicleLogScripts`, `LaterGameLogScripts`.

**R28. A deducible Heirloom relic.**
Re-ruled by the owner 2026-11-14.
Question: after Heirloom, Scatter Relics' generic line ("p2 set aside
Grand Mask") lets other seats deduce which revealed relic p2 kept.
Decision: relics must not be deducible. At the start of Stock Reliquary
every relic players hold turns facedown, the Grand Scepter excepted (it is
set aside and public), and every viewer's card knowledge of those relics
is cleared, by a new `RelicsHidden(relics)` Chronicle operation (wire kind
`"relics-hidden"`, field `"relics"`). Scatter Relics' set-aside lines then
read "p2 set aside a Relic" to anyone who does not know the card. An owner
still sees their own facedown relics, so Heirloom's holder names theirs in
its question; R19's holder-only line still names the kept relic, and The
Ear's peeks still give its holder the identities.
Reason: the owner's ruling: a physical table shuffles the relics facedown,
and the digital table must not reveal more.
Earlier decision (2026-11-13): the deduction was accepted.
The journaled evolved Chronicle still names these relics to every viewer
once the Chronicle ends (R48).
Slice/files: 3 (changed in 5); `ReliquaryAndRelics.stock`, `RelicsHidden`,
`ChronicleStateOperations.relicsHidden`, the codec.

## Slice 4

**R29. A dormant Iron Hand at a checkpoint.**
Decision: only an active Iron Hand is discarded at a checkpoint.
Reason: design ruling 1; after Sun step 2 a seated Imperial holds no
dormant legacy, so a dormant one is settled by step 2 and caught after the
Crown.
Slice/files: 4; `IronHand.discard`.

**R30. Where the checkpoints sit.**
Decision: the Crown and Offer Citizenship place the discard after their
reactivation and before their cap; the Sun's checkpoint opens Reveal
Relics' window.
Reason: no one should discard over the cap a legacy that Iron Hand's own
discard would spare.
Slice/files: 4; `ChronicleSunProcedure`, `OfferCitizenship`.

**R31. What the holder's Shape Empire discards.**
Decision: every card at a site the holder does not rule, the Empire's
included, with the usual intact-edifice and Enduring exceptions.
Reason: those sites are archived as rival sites (ruling 21), and the
archive expects Shape Empire to have cleared them.
Slice/files: 4; `ShapeEmpire`, `ChronicleLegacyRules.shaper`.

**R32. An Imperial Great Architect beside Iron Hand.**
Re-ruled by the owner 2026-11-14.
Decision: when an Imperial Great Architect is in play beside Iron Hand,
the Great Architect performs Build Edifice instead of the Iron Hand
holder; the holder does not build. Iron Hand still takes over Shape Empire
(R31), and its holder still builds when there is no Imperial Great
Architect. `ChronicleLegacyRules.builder` names one builder per suit; the
second builder slot and its `chronicle.build-edifice.<suit>.iron-hand`
decision id are removed, since nothing else needed them.
Reason: the owner's reading of Great Architect's "even if the Iron Hand is
in play": the Great Architect builds in Iron Hand's place.
Earlier decision (2026-11-13): both built, the Great Architect first per
suit, then the holder under its own id.
Slice/files: 4 (changed in 5); `BuildEdifice`,
`ChronicleLegacyRules.builder`.

**R33. Iron Hand's scoring records its legacies.**
Confirmed by the owner 2026-11-14: legacies Chronicler already scored are
not scored again by Iron Hand.
Decision: yes, with `LegaciesScored`.
Reason: "scored once per game" (design ruling 8) is a fact about the
legacy, kept whether or not a later step reads it.
Slice/files: 4; `LineageScoring`.

**R34. Iron Hand's lines.**
Decision: "p2 discarded Iron Hand" at each checkpoint; "p3 scored 2
lineage points with Iron Hand (2 from Beast)" after the winner's lines,
none for a bonus of 0; "Buried Giant stays on the map with Iron Hand" for
each of the holder's stored sites; all delta lines.
Reason: the plan's proposal.
Slice/files: 4; `IronHand`, `ProcedureLog`.

**R35. The next game until the setup batch.**
Decision: accepted for now: the next game restores the holder's stored
sites as the Empire's until Iron Hand's SETUP half lands; the ROADMAP's
setup item names it, and a test checks the record passes game start.
Reason: the SETUP half is out of scope by the design.
Confidence: low; the next game is wrong until then.
Slice/files: 4; `ChronicleExtraction`, `docs/ROADMAP.md`.

**R36. Test fixtures that used Iron Hand as a stand-in.**
Decision: swap each for a most-sites legacy with no Chronicle half,
listed in the commit, and confirm no golden log changes as a result.
Reason: Iron Hand's Chronicle half now acts, and the tests' intent is a
plain legacy.
Slice/files: 4; the affected suites.

## Slice 5

**R37. Where the Chancellor's Empire win comes from.**
Question: a Successor's result does not keep the Empire win that ended the
game, which the Chancellor takes back when Royal Line names them.
Decision: derive it from the ended game's state, with no saved-format
change. `EmpireEndings.empireWin` reuses the code that decided the ending
(the same predicate and kinds `stableRegime` and `warExhaustion` pass to
`chancellorWins`, with the Successor substitution skipped), not a
parallel restatement of the rules, and is tested against each Empire
ending the engine produces (the end die at rounds 5-7, and each round-8
case).
Reason: the ended game's state is fixed, and one source of truth cannot
drift.
Confidence: low; keeping the kind on the result would be sturdier if new
Empire endings appear; revisit then.
Slice/files: 5; `EmpireEndings`, `RoyalLine`.

**R38. Royal Line's question.**
Re-ruled by the owner 2026-11-14.
Decision: Royal Line is a "may" power, so the question offers Decline
first, which keeps the result, then every Imperial player, the Chancellor
first. Choosing the current winner still keeps the result and journals
nothing.
Reason: the owner's ruling: a "may" power offers Decline (design ruling
3).
Earlier decision (2026-11-13): every Imperial player, the Chancellor
first, and no Decline.
Slice/files: 5; `RoyalLine`.

**R39. The operation.**
Decision: `WinnerNamed(winner, kind)`, wire kind `"winner-named"` with
fields `"winner"` and `"victory"`; it refuses a game with no result, an
unseated winner and a kind that is not an Empire win.
Reason: the plan's proposal.
Slice/files: 5; `ChronicleOperation`, `ChronicleStateOperations`, the codec.

**R40. The new window.**
Decision: `PowerWindow.ChronicleStartOfSun` (`chronicle.sun.start-of-sun`),
the Sun's first window, before Reveal Relics and Iron Hand's Sun
checkpoint.
Reason: the plan's proposal.
Slice/files: 5; `PowerWindow`, `ChronicleWindows`, `ChronicleSunProcedure`.

**R41. The lines and the display.**
Decision: "p3 named p2 the winner with Royal Line" (a decision line), only
when the winner changes; the journaled end-of-game line stays; the result
banner follows the result; no other marker.
Reason: the plan's proposal.
Slice/files: 5; `RoyalLine`, `ProcedureLog`.

**R42. The ROADMAP.**
Decision: tick "Legacy powers: Chronicle"; the setup item keeps Iron
Hand's and Rival's SETUP halves and R35's interim next game, and the
in-play item keeps The Ear's ACTION half and Iron Hand's discard during a
game.
Reason: the design's Docs section.
Slice/files: 5; `docs/ROADMAP.md`.

The Sun reading the winner when each step is reached is accepted, with an
audit of every Chronicle step and power that reads the winner, each reading
it live after Royal Line, and a test that a Royal Line change reaches at
least the Crown, Reformer and Score Lineage Points.

**R43. A suit built twice beside Iron Hand.** (A slice 4 review fault.)
Superseded by the owner's re-ruling of R32, 2026-11-14.
Question: Build Edifice's second slot recomputed the available edifices
when reached, so after an Imperial Great Architect built a suit's edifice
the Iron Hand holder was offered that suit's next one and could build a
second edifice of the suit.
Decision: moot. Re-ruled R32 leaves one builder per suit, so there is no
second slot. The fix (the second slot skipped a suit the first slot built,
read from the journaled answer) was removed with the slot.
Slice/files: 4 (fixed in 5, then removed); `BuildEdifice`.

**R44. The landed ROADMAP item.** (Narrows R42.)
Decision: drop the landed "Legacy powers: Chronicle" item from the ROADMAP
instead of ticking it, following the file's convention; the phase's
introduction says the CHRONICLE powers landed with this design.
Reason: the convention is the file's standing rule, and R42 accepted a
proposal that did not account for it.
Slice/files: 5; `docs/ROADMAP.md`.

**R45. An edifice buried earlier in Build Edifice.** (A slice 2 fault,
found in the slice 5 review.)
Question: each suit recomputed its pool from the live deck, so an edifice
buried earlier in the same step, now at the deck's bottom, could be offered
by a later suit and rebuilt intact.
Decision: the available edifices are those available when the step began
(`docs/rules/chronicle.md`, Build Edifice). A suit's pool leaves out every
edifice that an earlier suit's answered build question (`decisionId`)
buried, read from journaled answers only. This stands after R32's
re-ruling: it fixes a Great Architect bug that happens with no Iron Hand
in play, rebuilding an edifice buried earlier in the same step.
Reason: it matches the rule and stays resume-safe; the pool is not
captured in an outer Branch.
Slice/files: 2 (fixed in 5); `BuildEdifice`.

**R46. An Iron Hand gained in the Sun before the cap.** (A whole-feature
review fault; amends design ruling 18 and R30.)
Question: an Iron Hand an Imperial gains active after the Sun's first
checkpoint, by Flip Legacies or by Gatekeeper, counted toward step 4's cap
and was only discarded at the Crown, so its holder discarded a legacy the
Iron Hand's own discard would have spared.
Decision: a fourth checkpoint, `IronHand.discard` at the start of the
`ChronicleLegacyCap` step, before the cap; the Sun-start, Crown and Offer
Citizenship checkpoints stay. Gatekeeper still offers Iron Hand to an
Imperial holder, since taking it to remove it from an enemy is a legal
play.
Reason: it restores R30's guarantee for every path into step 4 with one
placement.
Slice/files: 4 (fixed in 5); `ChronicleSunProcedure`, `IronHand`; the
design's ruling 18, `docs/rules/chronicle.md`, `docs/rules/ambiguities.md`
items 41 and 46.

**R47. The Ear's and The Bright Beacon's peeks in the Game Log.** (A
whole-feature review finding.)
Question: each knowledge-only Peek The Ear and The Bright Beacon record
wrote a "peeked at" line, one per relic or denizen and again at placement,
flooding every viewer's log.
Decision: these peeks write no log lines; each power keeps its one line
(the design's "One line per power used"). The Game Log's detail lines
skip peeks in the Throne and the Beacon, where only these two powers
peek; the knowledge records are unchanged.
Reason: a peek line per card says nothing the power's own line does not,
and floods the log.
Slice/files: 3 (fixed in 5); `DetailLines`.

**R48. The journaled evolved Chronicle names hidden cards.** (A re-ruling
review finding.)
Question: the evolved Chronicle the Chronicle Phase journals is projected
the same to every viewer (`GameProjection`, `GameEventJsonSupport`), so
after the Chronicle finishes it names each lineage's starting relics
(Heirloom's relic) and starting advisers (Oaths of Loyalty), the
Reliquary, and the relics stored at sites (The Ear's placements).
Decision: accepted for now; Legacies R4's acceptance extends to these
fields. R28 hides relic identities while the Chronicle Phase runs; the
stored record that ends it stays one shared record, as the TTS export and
the Chronicle page show it. A ROADMAP item, "Redact hidden cards in a
shown Chronicle", tracks redacting it per viewer.
Reason: the site relics the base Scatter Relics deals facedown were
already named this way before these powers. Redacting the record per
viewer touches the Chronicle page, the export codec and the next game's
import, which is a feature of its own.
Confidence: low; the owner may re-rule it.
Slice/files: 5; `GameProjection`, `GameEventJsonSupport`, `docs/ROADMAP.md`.
