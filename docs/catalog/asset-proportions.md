# Asset proportions

The real size and proportions of each game asset the table draws, and where
each number came from. The stylesheet's ratio table and `MatLayout` are
built from this record; roadmap item L3 reads it for the box each licensed
image must fill. It holds numbers only. The assets themselves are licensed
and live under the ignored `reference/assets/`.

## Cards

Sizes are the pixel sizes of the New Foundations card images in the
`bgs-cards` clone (`content/card-images/oath/en-US`).

| Asset | Size | Ratio (w : h) | CSS |
|---|---|---|---|
| Denizen, edifice, their backs | 381×596 | 1 : 1.564 | `--ratio-denizen` |
| Relic | 381×381 | 1 : 1 | `--ratio-relic` |
| Site | 770×596 | 1.292 : 1 | `--ratio-site` |
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
| Visions Drawn cells 0 to 5 | 805, 1000, 1120, 1295, 1415, 1535 | 1690 | 103 | 159 |
| Favor bank circle centres, Discord to Nomad | 2438, 2839, 3241, 3642, 4044, 4449 | 2155 | radius 93 | |

The mat prints a frame for each site and none for its cards. A site frame
is 669×509, a little wider than a site card of the same height (657.6), so
the card is centred in it. The three card slots are derived: the frame's
height, the denizen ratio's width, 48 px apart.

The region name band starts a little left of the frames, where the mat's
lettering starts, and stops short of the discard, so the longest name keeps
one line at the glance size; the lettering itself spans about 580 px.

Not on the mat and not drawn at their proportions: player boards and the
reliquary board, which the table keeps abstract.
