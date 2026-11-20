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
Slice/files: 1; `frontend/styles.css`.
