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
Question: the requester selects an option on a grant and undoes it; the
granter then refuses. A Redo would restore the move under a ledger that
now refuses it. Only a new command batch empties the redo stack. What
does a changed answer do to it?
Decision: only a `ConsentReconsidered` whose `granted` is false empties
the redo stack. A change to a grant leaves it, and the redo then
succeeds.
Reason: a redo restores a state that was legal under the ledger when it
was saved. A change to a grant only makes the ledger more permissive and
cannot invalidate that state, so it must not cost the requester their
redo steps.
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
