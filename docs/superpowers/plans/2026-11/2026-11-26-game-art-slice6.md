# Game Art, Slice 6: The Owner's Second Review Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [x]`) syntax
> for tracking.

**Goal:** Eight changes the owner asked for after looking at slice 5 on
the running table: the round marker in the middle of its wedge, the
Usurper's note over the whole wedge, see-through chip grounds with one
setting for all of them, a ground behind a whole pawn and a whole warband
entry, a banner's resources in a chip at the middle of its art, one
picture a warband up to five, and thicker lines on empty card slots.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md)
and its
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md).
The coordinator ruled R57 to R64 with the request, one a change, in
order; each task names its ruling. R61 rewrites R51.

## Global Constraints

The constraints of the slice 5 plan stand: work in this worktree on
`game-art`; port 8083 and its database are the owner's; licensed art
never enters the repository or a test; files are added by path; the
layout is the same with art on and off; the three stylesheet guards
hold; a commit follows a green check with `&&`.

## Tasks

All done. Two additions came from the browser: the current round's
number on the marker is no longer printed, so it shows over the mat, and
a drawn banner's name stands at the face's foot, under the chip.

### Task 1: The round marker at its wedge's centre (R57)

- [x] Tests first (`RoundWheelSuite`, `StylesheetArtSuite`): the disc is
  centred 68.5 from the hub on the wedge's middle angle; every number
  stands there too, and the current round's is Base on the disc's silver
  at 4.5 : 1 or better; the limiter's diamond stands in the wedge's rim,
  82 from the hub, clear of the number.
- [x] `RoundSegment` keeps the wedge's centre and gains its rim point;
  the label and the disc use the centre, the diamond the rim, at 14px.
- [x] Commit: `fix(frontend): centre the round marker in its wedge`.

### Task 2: The Usurper's note over the whole wedge (R58)

- [x] Test first: the limiter round's group carries the lock's words as
  its hover note and its accessible name, with art on and off; no child
  carries a note of its own.
- [x] Move the note from the marker's path to the round's group.
- [x] Commit: `fix(frontend): show the Usurper's lock over its whole wedge`.

### Task 3: One see-through ground for the chips over art (R59, R60)

- [x] Tests first (`StylesheetArtSuite`): `--chip-ground-alpha` is
  written once, 0.65 (the owner rewrote R59 after this plan was written;
  R57 too: the disc is larger, radius 16); `--chip-ground` is Base at that alpha; each chip
  over art uses `var(--chip-ground)` for its fill and its halo; the
  figures on a chip carry a Base text edge.
- [x] Apply to the bank count, pile counts, the tracker's lines, loose
  tokens on a site, and (Tasks 4 and 5) pieces and banner resources.
- [x] Judge each in the browser at 100% and desktop Fit over the busiest
  art it can sit on; a chip that does not read keeps opaque Base and is
  listed in the report with a comparison.
- [x] Commit: `feat(frontend): make the chips over art see-through, from one setting`.

### Task 4: A ground behind a whole pawn and a whole warband entry (R61, R63)

- [x] Tests first (`SitePiecesSuite`, `StylesheetArtSuite`): a pawn holds
  a ground layer under its piece and name, in both modes, painted over
  art alone, for an offered pawn too, inside its ring; a force of one to
  five warbands is that many pictures and no count; six or more is one
  picture and "x6"; the name and the note always state the number; the
  entry has one ground.
- [x] Commit: `feat(frontend): stand each piece on a ground and show up to five warbands singly`.

### Task 5: A banner's resources in a chip on its art (R62)

- [x] Tests first: the banner's tokens are a chip centred one third down
  the face, the same box in both modes, sized as a share of the face.
- [x] Commit: `feat(frontend): show a banner's resources in a chip on its art`.

### Task 6: Thicker lines on empty card slots (R64)

- [x] Test first: the slot's line and the dark line under it are both
  `2px / --map-scale`.
- [x] Commit: `fix(frontend): double the line of an empty card slot`.

### Task 7: Records, gate and browser check

- [x] Rulings R57 to R64, spec, `DESIGN.md`.
- [x] Full gate; browser check on 8084: rectangles art on against off at
  three sizes, the screenshots the coordinator listed, and the bank
  comparison at alpha 0.35 and 0.65.
