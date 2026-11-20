# Reconsidering a consent answer: implementation rulings

Rulings the owner's orchestrator made while the
[reconsider design](2026-11-20-consent-reconsider-design.md) was
implemented, on questions the design and its slice plans did not settle.
Each entry gives the question, the decision and the reason, the confidence
when the orchestrator marked it low, and the slice and files it touches.
The design's own rulings 20 to 26 are not repeated here.

## Slice 1

**R1. The consent column's type size.**
Question: `.consent-grid` set no font size, so the heading and the
players' names took the overlay's own size (about 23px at 1440×900),
beside a details column at `0.82em` and settings at 0.78rem. With the
name on its own line, should the column keep that size?
Decision: `.consent-grid { font-size: 0.82em; }`, the details column's
size. If the browser check shows a name or a setting under the
Eleven-Pixel Floor at 360px, the work stops and reports; it does not
shrink the text further.
Reason: the name on its own line must not outrank the heading and the
settings, and slice 3 adds sentence lines to this column that need the
details column's size.
Slice/files: 1; `frontend/styles.css`.

**R2. Naming a row's three settings for a screen reader.**
Question: a row's three buttons had nothing tying them to the player's
name. Should the new wrapper say whose settings they are?
Decision: the settings' wrapper carries `role="group"` and `aria-label`
set to the player's name.
Reason: three unlabelled buttons per player cannot be told apart by a
screen reader, and it follows the existing toggle-group pattern.
Slice/files: 1; `frontend/.../SitePowerLinks.scala` (`ConsentGridView`).

**R3. The settings' side padding in thirds.**
Question: the Control face's padding is `4px 9px`; in equal thirds at
360px the widest label, "Case-by-case", has no margin left with 9px
sides. May the settings in this column take less?
Decision: `4px 6px` on `.consent-grid-choices .consent-grid-choice` only;
the shared Control-face rule is untouched. The browser check measures it.
If "Case-by-case" still clips or wraps at 360px, the work stops and
reports the measured widths; it does not choose another fix.
Reason: equal thirds must hold at 360px.
Limit (R5): above a root font size of about 18px, "Case-by-case" wraps
inside its setting at 360px.
Slice/files: 1; `frontend/styles.css`.

**R4. The face of a disabled setting.**
Question: slice 1's review found that a disabled setting has no disabled
face: full opacity, a pointer cursor, and a border that still brightens
on hover. The fault predates this work. Fix it now?
Decision: yes. `.consent-grid-choice:disabled { opacity: 0.5; cursor:
not-allowed; }`, the hover rule scoped with `:not(:disabled)`, and a
stylesheet suite line for each.
Reason: `DESIGN.md`'s Disabled face already requires it, the new Consent
Column section claims it, and slice 3 adds more disabled and withheld
buttons to this column that must read as unavailable.
Slice/files: 1 (review fix); `frontend/styles.css`,
`StylesheetConsentGridSuite.scala`, `DESIGN.md`.

**R5. "Case-by-case" at a large root font size.**
Question: with the browser's root font size above about 18px,
"Case-by-case" wraps inside its setting at 360px. Is that accepted?
Decision: yes.
Reason: it degrades to two lines without clipping, and it is outside the
spec's default case at 360px.
Slice/files: 1; none.

**R6. The settings group's name is spoken twice.**
Question: with `aria-label` on the settings group (R2), a screen reader
says the player's name twice: once as the row's text and once as the
group's name. Change it to `aria-labelledby`?
Decision: no; `aria-label` stays as R2 ruled.
Reason: `aria-labelledby` would announce the same name twice too, so the
change buys nothing.
Slice/files: 1; none.

## Slice 2

**R7. Redo after a changed answer.**
Superseded by R29. R7 let a change to a grant keep the redo stack, on the
reasoning that a grant only makes the ledger more permissive. With R12 a
grant also changes what a saved state should hold (who paid a toll), so
that reasoning no longer holds.
Slice/files: 2; `application/JournalFold.scala`.

**R8. A change while a request about the same option waits.**
Question: after a refusal changed to a grant, the requester asks again
and the new request waits on a second granter. If the first granter
changes back and the second then grants, the settling answer resumes
into an option the first granter blocks. Is the change accepted?
Decision: the desk refuses a change while a waiting request names the
same requester, decision and option. Any other change is accepted while
a request waits. In slice 3 the projection gives such an entry its own
`change` value, `waiting`, with the hover note "A request about this
option is waiting for an answer."
Reason: it is the spec's own case of a request that still waits on
another player's answer, and it keeps `settled` from resuming into a
blocked option.
Slice/files: 2 and 3; `application/ConsentDesk.scala`, and the slice 3
projection.

**R9. A co-granter's grant that a refusal kept back.**
Question: Green grants, then Red refuses; the request settles refused
and Green's grant is not remembered. Red changes to a grant, so the
entry reads granted by both. Is Green asked again?
Decision: no. When a change makes the entry granted, each other answered
grant in it is remembered for its keys, unless the key already holds an
answer. Green's remembered grant stays Green's own to reconsider.
Reason: the consent design's ruling 9 says grants given before a refusal
are remembered, and asking Green twice about an option Green already
allowed is noise.
Slice/files: 2; `model/Consent.scala` (`reconsidered`).

**R10. The entry when a granter is asked again about the same option.**
Question: the design keeps at most one entry per requester, decision,
option and answering granter, and says a later request replaces the
earlier one. An earlier entry can hold a second granter's answer too.
Decision: a later request takes over only the asks its own granters
answered before. The earlier entry stays while another answered ask is
left in it.
Reason: dropping the whole entry would take away a second granter's
right to change their answer (ruling 20).
Slice/files: 2; `model/Consent.scala` (`joined`).

**R11. How a use mark is scoped to a turn.**
Decision: the mark carries its `ConsentTurn`. A mark is set only when
the ledger's turn is the game's turn. Setting one drops the marks of
other turns. A round rename (Great Clock) renames the marks with the
ledger's turn.
Reason: one place scopes a mark, by the rule the answers already follow,
and there is no second place to forget.
Slice/files: 2; `model/Consent.scala` (`UseMark`),
`gameplay/consent/ConsentUseMarks.scala`,
`gameplay/operations/TurnStateOperations.scala`.

**R12. The ruler's waiver and the payer's agreement, both granted.**
Question: slice 2's review found that a sent Travel charges the toll when
the payer agreed, even when the ruler's waiver is also granted. That can
now happen: the ruler refuses, the payer agrees, the traveller undoes,
and the ruler changes to a grant.
Decision: when both are granted for one sent Travel, the waiver wins and
nobody pays.
Reason: consent to ignore Toll Roads skips the payment (consent design,
ruling 1), and the log has just told every player that the ruler gives
that consent.
Confidence: low. Which grant wins is a rules question for the owner.
Slice/files: 2 (review fix); `gameplay/powers/travel/TollRoads.scala`.

**R13. The ledger's turn across an undo of a round change.**
Question: undoing a Great Clock returns the game to the old round while
the carried ledger keeps the new one, so the turn's answers and history
read empty and a change is refused.
Decision: on an undo or a redo, the carried ledger's turn, and nothing
else in it, is renamed to the target state's turn when both name the
same active player and the ledger's turn is the current game's turn.
This rests on an undo never crossing a turn end, which is confirmed in
the undo design and code before the rename is applied.
Reason: the Great Clock moves the round within a turn (consent design,
ruling 9), and its undo must not end the turn for the ledger.
Slice/files: 2 (review fix); `application/JournalFold.scala`.

**R14. A refusal given before a withdrawal.**
Question: an alternative's refusal is remembered when it is given, but
joined the history only when the request settled. After a withdrawal it
blocked for the turn and could never be changed; while the request still
waited, the desk gave the wrong reason.
Decision: a withdrawn request's refused asks join the history, as an
entry holding only its answered asks. The desk checks the waiting-request
case before the "answered no request" case, so the reason is R8's. The
design's State section, which said a withdrawn request never joins, is
amended.
Reason: a remembered refusal must be the refuser's to change (ruling 20).
Slice/files: 2 (review fix); `model/Consent.scala`,
`application/ConsentDesk.scala`, the design's State section.

**R15. A use mark from an earlier use of the same option.**
Question: a mark names a requester, a decision and an option, so an
earlier use locks a later request about the same three. That is reachable
only where one target carries different `covers` twice in a turn, such
as the same Hall of Ministers slot.
Decision: accepted as it is, and recorded in the design's Deferred
section.
Reason: tying a mark to an entry needs entry ids in the ledger, for a
case that costs only the right to change one answer.
Slice/files: 2; the design's Deferred section.

## Slice 3

**R16. The consent column's width.**
Decision: under 620px, where the column stands alone on its line, it
fills the line up to a reading measure: `@media (max-width: 620px) {
.consent-grid { flex: 1 1 17rem; max-width: 22rem; } }`. Beside the
details at desktop width it keeps its content width. The bare rule, as
first ruled, grew the column beside the details too, which the browser
check measured, so it ships inside the media query.
Reason: sentence lines need the line's width on a phone.
Slice/files: 3; `frontend/styles.css`.

**R17. An answer that covers a power the viewer no longer rules.**
Decision: a fifth `change` value, `lapsed`. Its button is withheld with
the hover note "You no longer rule every power this answer covers."
Reason: an unavailable option is shown with its reason, not hidden and
not left to fail on a click.
Slice/files: 3; the projection, `ConsentRequestsView.scala`.

**R18. An automatic power's entry.**
Decision: its sentence is line 1 ("Blue's Tyrant: deactivated" or "Blue's
Tyrant: let work") and the withheld button stands alone on line 2,
labelled with the prompt's word for the other answer: "Let it work" on a
deactivation, "Deactivate" on a let-work.
Reason: "Refused" and "Allow" are not the words that prompt used.
Slice/files: 3; `ConsentRequestsView.scala`.

**R19. An option the viewer may not identify.**
Decision: an entry whose option the viewer may not identify is not
listed, so no id for it leaves the server. A staged entry in the
projector's test covers it.
Reason: a hidden card's catalog id on the wire is a leak whether or not
the label hides it, and the cost is only that such an answer cannot be
changed from the overlay.
Confidence: low that the case is reachable today.
Slice/files: 3; `application/ConsentProjector.scala`.

**R20. What a refused change shows.**
Decision: nothing beyond the redraw. After a click the entry's button
waits, as `aria-disabled` so it keeps keyboard focus, until the server
has answered: until the snapshot's position or the failure the table
shows is new. A repaint of the same position, such as a log page
landing, leaves it waiting.
Reason: the design asks for the redraw only, and a button live again
while its command is in flight sends the command twice.
Slice/files: 3; `ConsentRequestsView.scala`,
`CardInspectionOverlay.scala`, `TableScreen.scala`.

**R21. The prompt's notice.**
Decision: `ConsentPromptProjection` gains one required field, `adjust`,
with three values. `answer`: the viewer rules every power of the ask and
the request is not about an automatic power; the new notice shows.
`preferences`: the viewer rules every power of the ask and the request is
about an automatic power; the old notice shows. `none`: some power of the
ask has no granter, as for a payer or an Imperial law; no notice element
is drawn. This changes those four prompts, which showed the old notice.
Reason: the old notice is already untrue for those four asks, which is
the kind of promise this work began by fixing.
Slice/files: 3; the prompt projection and codec,
`ConsentProjector.scala`, `ConsentPanels.scala`.

**R22. Telling the entries' buttons apart.**
Decision: each entry is a `role="group"` whose `aria-label` is its line 1
text, as R2 did for a player's settings.
Reason: a list of buttons all named "Allow" or "Refuse" cannot be told
apart by a screen reader.
Slice/files: 3; `ConsentRequestsView.scala`.

**R23. The list's wording of an option.**
Decision: the list words an option from the current state, as the design
says, although the log's line words it from the request's own state. A
positional option (an adviser or relic slot) names what sits there now.
Recorded in the design's Deferred section.
Reason: the projector has no journal, and carrying wording in the ledger
would put presentation text in the model.
Slice/files: 3; the design's Deferred section.

**R24. An entry once the game is over.**
Question: the desk refuses every change once the game is over, but the
projection still called the entry `open`.
Decision: the entry stays listed and is not changeable: `change` value
`over`, button withheld, hover note "The game is over." `ConsentChange`
judges it first, as the desk does.
Reason: one judgement for the button and the command, and an unavailable
option keeps its reason.
Slice/files: 3 (review fix); `application/ConsentChange.scala`,
`ConsentRequestsView.scala`.

**R25. The prompt's target for a card the viewer may not identify.**
Decision: it reads "a hidden card", and the card's catalog id is not in
the encoded projection. Every other fallback is unchanged. This fallback
predates this work, so it is listed as existing behaviour changed.
Reason: R19's leak, on the prompt.
Confidence: low that the case is reachable today; a staged state covers
it.
Slice/files: 3 (review fix); `application/ConsentProjector.scala`.

**R26. An answer given while its request still waits.**
Question: a ruler whose alternative refusal is given while the request
still waits has no entry until the request ends, though their prompt's
notice said the answer can be changed from the power.
Decision: a pending request's already answered asks are projected to
their giver as rows with `change` `waiting` (R8's hover note). The
ledger's history is unchanged; only the projection adds the rows. The
design's Projection section is amended.
Reason: the notice told that ruler the answer can be changed from the
power, so the power must show it and say why not yet.
Slice/files: 3 (review fix); `application/ConsentProjector.scala`, the
design's Projection section.

**R27. Entries not seen in a played game.**
Decision: the automatic, waiting, lapsed, over and "Also answers" entries
are verified by suites and a hand-built layout check only.
Reason: no short route reaches them in a scratch game, and each has a
view test.
Slice/files: 3; none.

**R28. The change line's lookup of its request.**
Question: the log's change line reads its option and powers from the
latest matching `ConsentRequested`; a mutant that reads the earliest
passes every suite.
Decision: accepted as it is.
Reason: no reachable flow was found where the two differ, and a
constructed journal would pin an unreachable case.
Slice/files: 2; none.

**R29. Redo after any changed answer.**
Question: a change to a grant kept the redo stack (R7). With R12 a Redo
could then restore a toll payment that the ruler's new waiver spares.
Decision: every `ConsentReconsidered`, to a grant or to a refusal,
empties the redo stack. This amends R7.
Reason: a grant can change what a saved state should contain, and
emptying on every change is one rule that inspects no saved state.
Slice/files: 2 (whole-feature review); `application/JournalFold.scala`.
