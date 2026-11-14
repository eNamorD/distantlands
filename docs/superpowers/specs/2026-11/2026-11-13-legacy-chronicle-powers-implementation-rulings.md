# Legacy Chronicle powers implementation rulings

Rulings the owner's orchestrator made while the
[Legacy Chronicle powers design](2026-11-13-legacy-chronicle-powers-design.md)
was implemented, on questions the design and its slice plans did not
settle. Each entry gives the question, the decision and the reason, the
confidence when the orchestrator marked it low, and the slice and files it
touches. The design's own rulings 1-23 are not repeated here.

## Slice 1

**R1. Do dormant legacies score under Chronicler?**
Decision: yes. Every legacy a seated player holds scores under Chronicler,
dormant ones included.
Reason: design ruling 7 and Legacies R33 count each legacy held; Chronicler
must score the same set Score Lineage Points would.
Slice/files: 1; `LineageScoring.unscored`, `Chronicler`.

**R2. Which legacies `LegaciesScored` records.**
Decision: every legacy each seated player holds when Chronicler resolves,
those that score 0 points included.
Reason: a legacy scored for 0 has still been scored; denying the winner's
later bonus is the card's point.
Slice/files: 1; `Chronicler`, `LegaciesScored`.

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
Decision: the denizens and ruined edifices at Imperial sites; intact
edifices stay.
Reason: intact edifices are locked and `Discard.RuinedEdifice` is the only
edifice discard; Chronicle ruling 16's protection holds.
Confidence: low; the card's text names no edifice exception.
Slice/files: 2; `Revolutionary`.

**R10. What Reformer moves and swaps.**
Decision: denizens, edifices included, intact and ruined.
Reason: an edifice is a denizen, and "ignoring the locked restriction" only
has force for locked cards such as intact edifices.
Confidence: low; the text could mean ordinary locked denizens only.
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
suit bank. A ruined edifice Revolutionary discards after Build Edifice
returned the set-aside list stays set aside, which is harmless; a test
checks the stored Chronicle still passes game start's card accounting.
Reason: the favor follows the card that leaves; game start rebuilds the
edifice deck from the catalog.
Slice/files: 2; `BuildEdifice`, `Revolutionary`.
