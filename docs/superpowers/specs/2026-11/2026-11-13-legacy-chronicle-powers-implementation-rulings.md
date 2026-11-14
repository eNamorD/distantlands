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
