# Asset proportions

The real size and proportions of each game asset the table draws, and where
each number came from. The stylesheet's ratio table and `MatLayout` are
built from this record, and
[game art](../operations/game-art.md) fills each box with its licensed
image. It holds numbers only. The assets themselves are licensed
and live under the ignored `reference/assets/`.

## Cards

Sizes are the pixel sizes of the New Foundations card images in the
`bgs-cards` clone (`content/card-images/oath/en-US`).

| Asset | Size | Ratio (w : h) | CSS |
|---|---|---|---|
| Denizen, edifice, their backs | 381×596 | 1 : 1.564 | `--ratio-denizen` |
| Relic | 381×381 | 1 : 1 | `--ratio-relic` |
| Site | 770×596 | 1.292 : 1 | none: `MatLayout.siteCard` places the box, and the image covers it |
| Legacy, Vision | 596×381 | 1.564 : 1 | `--ratio-legacy` |
| Banner | 1181×1181 | 1 : 1 | `--ratio-banner` |
| Foundation card | 469×803 | 1 : 1.712 | `--ratio-foundation` |
| Oath goal | 673×898 | 1 : 1.334 | `--ratio-oath-goal` |
| Oathkeeper title | 101.6×70 mm (print-and-play sheet) | 1.452 : 1 | `--ratio-oathkeeper-title` |

A Vision is a denizen card printed on its side. In a row it keeps the
denizen box; upright, it is the legacy box.

## Pieces

| Asset | Shape | Source |
|---|---|---|
| Attack, defense and round die face | square | Tabletop Simulator die textures |
| Favor token | round | owner's screenshot of the model |
| Secret token | book, about 0.87 : 1 | owner's screenshot of the model |

Token and die glyph boxes are square; a token image is fitted inside its
box.

## Die faces

Each die's sheet is 2048×2048 px. Its six sides lie three by two in the
sheet's lower two thirds. A face is shown as a square crop of the sheet;
a face printed on several sides is read from one. Numbers are in the
sheet's pixels, from its upper left.

| Face | Die | x | y | w | h |
|---|---|---|---|---|---|
| two-swords-skull | attack | 14 | 702 | 638 | 638 |
| hollow-sword | attack | 698 | 702 | 638 | 638 |
| one-sword | attack | 1392 | 702 | 638 | 638 |
| blank | defense | 10 | 707 | 642 | 642 |
| one-shield | defense | 700 | 707 | 642 | 642 |
| two-shields | defense | 10 | 1387 | 642 | 642 |
| doubler | defense | 1391 | 1387 | 642 | 642 |

How they were measured: each sheet was drawn to a canvas in the browser
and scanned along rows and columns for runs of the side's flat colour
(orange on the attack die, blue on the defense die). A side's run is 640
px on the attack sheet and 643 px on the defense sheet; each crop is the
run less a pixel at each edge. The doubler's side prints its own "2×".

## The mat

The mat image is 5787×2303 px (2.513 : 1). All numbers below are in its
pixels, from its upper left.

| Piece | x | y | w | h |
|---|---|---|---|---|
| Site frame, Cradle column | 71 | rows below | 669 | 509 |
| Site frame, Provinces column | 1994 | rows below | 669 | 509 |
| Site frame, Hinterland column | 3945 | rows below | 669 | 509 |
| Site rows (the Cradle has the first two) | | 428, 962.5, 1497 | | |
| Card slot | frame's right + 48, then every 373.4 | frame's | 325.4 | 509 |
| Discard pile (card on its side) | column's frame + 932 | 80 | 509 | 325.4 |
| Region name band | column's frame − 25 | 60 | 900 | 240 |
| Relic deck | 881 | 1913 | 325.4 | 325.4 |
| World deck (card on its side) | 1278 | 1913 | 509 | 325.4 |
| Round wheel | 62 | 1602 | 642 | 642 |
| Round marker (the current segment, over the wheel) | 62 | 1602 | 642 | 642 |
| Usurper's token (on the limiter's segment; in the wheel's 200 units, 54 by 55, centred 68.5 from the hub) | 62 | 1602 | 642 | 642 |
| Visions Drawn cells 0 to 5 | 805, 1000, 1120, 1295, 1415, 1535 | 1690 | 103 | 159 |
| Favor bank circle centres, Discord to Nomad | 2438, 2839, 3241, 3642, 4044, 4449 | 2155 | radius 93 | |
| Favor bank box (suit emblem and circle) | centre − 217 | 2050 | 310 | 240 |
| Favor bank emblem (a square about its ring) | centre − 212 | 2067 | 122 | 122 |
| Tracker's line of text | 71 | 1500 | 1790 | 80 |
| Visions Drawn label | 1680 | 1650 | 210 | 150 |

The mat prints a frame for each site and none for its cards. A site frame
is 669×509 by its nominal rectangle, but the mat is drawn by hand: each
frame's line is 5 to 6 px thick and strays up to 8 px outside that
rectangle (the Provinces' second frame reaches 7 px past its right edge,
the Cradle's first 5 px above its top). The site card's box is therefore
the frame grown by 8 px on every side, 685×525, so the card covers the
line; its image (770×596) fills the box with `cover`, which trims about
one percent of its height. The line was measured by drawing the mat to a
canvas and scanning rows and columns across each of eight frames' four
edges for runs of the line's yellow. The three card slots are derived:
the frame's height, the denizen ratio's width, 48 px apart from the
frame's nominal right edge.

The region name band starts a little left of the frames, where the mat's
lettering starts, and stops short of the discard, so the longest name keeps
one line at the glance size; the lettering itself spans about 580 px.

Not on the mat and not drawn at their proportions: player boards and the
reliquary board, which the table keeps abstract.
