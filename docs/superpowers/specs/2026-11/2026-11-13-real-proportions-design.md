# Real proportions for every drawn asset

**Status:** designed 2026-11-13, approved section by section by the owner.

**Builds on** the
[card shape and inspection design](../2026-09/2026-09-22-card-shape-and-inspection-design.md),
whose `1 : 1.4` card box this replaces, the
[table readability design](../2026-10/2026-10-02-table-readability-design.md),
whose glance layer this retunes, and the
[board decisions design](../2026-10/2026-10-03-board-decisions-design.md),
whose option targets this keeps.

## Goal and scope

Roadmap item **L3** shows licensed game assets with permanent drawn
fallbacks. That only works if a drawn asset and its licensed image fill the
same box. Today they would not: a drawn denizen is `1 : 1.4` and the real
card is `1 : 1.564`; a site is a tall box that holds its denizens, and the
real site is a landscape card beside them; the map is a flowing grid, and
the real map is one printed mat.

This spec gives every drawn asset the proportions of the real one and lays
the map out on the mat's own coordinates, so that L3 can swap art in
without moving anything.

Relative sizes between assets need not match the physical game. Each
asset's own width-to-height ratio must.

**In scope**

- One ratio table for card kinds, read by every card box in the client.
- A card face split into a content layer and an overlay layer.
- A map drawn as one board with the mat's ratio, every slot placed by a
  coordinate table measured from the mat.
- A real-ratio site card.
- A strip under the mat for what the physical table keeps beside it.
- A tracked record of the measured proportions.

**Out of scope**

- Showing any licensed image. That is L3.
- Porting the map to a canvas. The coordinate table this spec adds carries
  over to a canvas unchanged.
- Player boards and the reliquary board. They stay abstract, and L3 will not
  show them either (owner, 2026-11-13).
- The pills, chips and rows that stand in for a card on the table: a
  revealed Vision, the Oathkeeper title, the Oath goal line, foundation
  rows and the legacy chips on a player strip. They stay as they are; the
  inspection overlay shows each at its real ratio (owner, 2026-11-13).
- The travel-cost band the mat prints under each region name. The drawn
  mat shows the region name only (owner, 2026-11-13).

## Measured proportions

Card sizes are the pixel sizes of the New Foundations images in the owner's
`bgs-cards` clone (`content/card-images/oath/en-US`). The content network
serves the same files, so nothing there is larger.

| Asset | Real size | Ratio (w : h) | Drawn before this spec |
|---|---|---|---|
| Denizen, edifice, their backs | 381×596 px | 1 : 1.564 | 1 : 1.4 |
| Relic | 381×381 px | 1 : 1 | 1 : 1 |
| Site | 770×596 px | 1.292 : 1 | no card; a box holding its denizens |
| Legacy, Vision | 596×381 px | 1.564 : 1 | portrait denizen box |
| Banner | 1181×1181 px | 1 : 1 | 1 : 1 |
| Foundation card | 469×803 px | 1 : 1.712 | list row |
| Oath goal | 673×898 px | 1 : 1.334 | text |
| Oathkeeper title | 101.6×70 mm | 1.452 : 1 | pill |
| Map mat | 5787×2303 px | 2.513 : 1 | flowing grid, about 1.6 : 1 |
| Die face | square | 1 : 1 | square chip |
| Favor token | round | 1 : 1 | square glyph box |
| Secret token | book | about 0.87 : 1 | square glyph box |

A site card is two denizen cards wide. A site and its three card slots are
five card widths.

### The mat

The mat image is 5787×2303 px. It prints:

- Eight site frames, each sized for a site card: two in the Cradle, three
  each in the Provinces and the Hinterland. Measured, in mat pixels:
  - columns: Cradle x 71–740, Provinces x 1994–2663, Hinterland x 3945–4615;
  - rows: y 428–936, y 962–1471, y 1497–2006 (the Cradle has the first two).
- No frames for denizens. The space to the right of each site frame is
  unprinted and holds three cards.
- A discard slot at each region's upper right, a denizen card on its side.
- In the Cradle's foot: the round wheel, the Visions Drawn track with six
  cells (0 to 5), a square relic deck slot and a portrait world deck slot.
- Six favor bank circles in a row along the foot, under the Provinces and
  the Hinterland, in the order Discord, Arcane, Order, Hearth, Beast, Nomad.
- Region names at each region's upper left.

A site frame is 669×509 px, a ratio of 1.314. A site card of ratio 1.292
fits inside it by height.

## Design

### The ratio table

`:root` carries one custom property per card kind, each the real pixel size
as a CSS ratio:

```css
:root {
  --ratio-denizen: 381 / 596;
  --ratio-site: 770 / 596;
  --ratio-relic: 1 / 1;
  --ratio-banner: 1 / 1;
  --ratio-legacy: 596 / 381;
  --ratio-foundation: 469 / 803;
  --ratio-oath-goal: 673 / 898;
  --ratio-oathkeeper-title: 1016 / 700;
}
```

Every card box takes its shape from this table through `aspect-ratio`. No
literal `1.4` remains in `styles.css`, and `--card-h` is derived from
`--card-w` and `--ratio-denizen`.

`CardFace.boxClass` gains the kinds the table names. A legacy takes
`card-face-legacy`, landscape, wherever it is drawn as a card: the Public
Ambitions row, card lists and the inspection overlay.

A Vision is the same physical card as a denizen, turned on its side. In a
card row or a slot it keeps the portrait denizen box, so the row stays a
uniform grid; its licensed image will sit there with a quarter turn. The
inspection overlay shows a Vision in the legacy box, upright.

Foundation cards, the Oath goal and the Oathkeeper title use their ratios
in the inspection overlay only.

### Sizing

On the mat a slot sets its card's size. Everywhere else a card keeps the
height it has today and gives up width, about ten percent of it. The player
strip's height budget therefore does not change. The drawn name and emblems
must still fit the narrower face; where they do not, the face's type scale
is adjusted, not the box.

### The card face

A card face is a box of its kind's ratio holding two layers.

- **Content layer.** What is printed on the card. Today this is the drawn
  face: name, emblems, defense, power marks. L3 replaces this layer, and
  only this layer, with the licensed image.
- **Overlay layer.** What lies on the card during play: tokens, warbands,
  tracking marks, the selection ring and its mark, the Unimplemented badge.
  Each overlay is anchored to a corner or an edge of the box, never to an
  element of the content layer, so it stands in the same place over a
  drawn face and over an image.

The same split applies to the site card and the banner.

### The mat board

`MatLayout` is a new object with no DOM dependency. It holds the mat's size
and every slot as a rectangle in mat pixels:

- the eight site frames, as measured above;
- the site card inside each frame: the frame's height, the site ratio's
  width, centred in the frame;
- three card slots to the right of each frame: the frame's top and height,
  the denizen ratio's width (325 px), the first 48 px from the frame and
  each next 48 px from the last;
- the three discard slots, the relic deck slot, the world deck slot, the
  round wheel, the six Visions Drawn cells, the six favor banks and the
  three region name bands, each measured from the mat image by the same
  pixel survey that gave the frames, and recorded in the plan.

`WorldBoardRenderer.world` builds one `.mat-board` element with
`aspect-ratio: 5787 / 2303` and places each slot in it as an absolutely
positioned child whose `left`, `top`, `width` and `height` are percentages
computed from `MatLayout`. The board's design width is 2400 px, which
puts a card on the mat at 135 px wide, the size it has today, and leaves
Fit near 0.45 at 1440×900. The existing zoom, pan and Fit act on the board as they act on
`.map-content` now.

The board has two layers of its own:

- **The drawn mat.** Three region colour washes, the site frames, the
  region names and the slot labels ("Discard Pile", "Relic Deck", "World
  Deck", "Visions Drawn", the bank suit names), each at its mat position.
  L3 replaces this layer with the mat image as one background.
- **Live state.** Sites, cards, deck and discard counts and top cards, the
  round marker, the Usurper limiter, the Visions Drawn marker, favor
  counts, pawns, warbands and option rings. This layer is the same over a
  drawn mat and over the image.

What moves:

- The relic deck leaves the shared bank column for its slot in the Cradle.
- The favor banks leave their full-width row for the mat's six circles.
- The round wheel and the world deck take their mat positions in the
  Cradle's foot. The wheel keeps its eight drawn segments inside the mat's
  circle.
- Visions Drawn becomes a marker on the six-cell track, with the count kept
  in the tracker's accessible name.
- An empty site slot is the mat's frame with nothing in it.

Relics at a site stay in the card slots to the right of the site, after its
denizens, as they are today. Capacity and relic slots always sum to three,
so the three slots hold every card a site can have. The physical game lays
relics on the site card, which would hide the site's text (owner,
2026-11-13).

### The site card

A site is drawn as a card of the site ratio in its frame.

- **Content layer:** the name in a band at the foot, as the real card has
  it; above the band, the site's powers and its Forge or Recover
  requirement; the defense dice.
- **Overlay layer:** loose favor and secrets at the upper left, where the
  real card prints its starting resources; pawns; warbands; the Festival
  marker; the travel cost badge a decision puts on the site.

The site card and its three slots remain one option target for a board
decision that picks a site: the ring is drawn around the site card, and
`BoardOptions` keys do not change.

### Beside the mat

The shared bank column is removed. The mat is fitted by width, which leaves
about a third of the world pane's height free. A strip under the mat, on
the same zoom surface, holds what the physical table keeps beside the mat:
the banners, the Oathkeeper line, the foundation rows and the Public
Ambitions row.

### The glance layer

The zoomed-out text rules keep their purpose: names, pawns, tokens, defense
and forces hold a readable screen size while the board shrinks. They are
retuned for the site card and for slots whose size the mat fixes. The
threshold and the rule that nothing in the glance layer changes the board's
size both stay; a board with a fixed ratio satisfies the second by
construction.

### Tokens and dice

Glyph boxes stay square. A licensed token image will be fitted inside its
box by `object-fit: contain`, so the secret's 0.87 : 1 needs no layout of
its own.

## Reference assets

Licensed files live under `reference/assets/`, which `.gitignore` already
covers through `reference/`. Nothing in the product reads them.

- `reference/assets/tts/`: the mat, the attack and defense dice sheets, the
  round die's mesh (a cube; no face texture came with it) and the owner's
  screenshot of the favor and secret tokens (in place).
- `reference/assets/cards/`: the 240 New Foundations card images and the
  symbol images, copied from the `bgs-cards` clone.
- `reference/assets/backs/`: card backs rendered from the print-and-play
  PDFs in `reference/2026-03-17-new-foundations-rev1/`.

`docs/catalog/asset-proportions.md` is the tracked record: each asset kind,
its real size, its ratio and where the number came from. It holds numbers
and no licensed content, and L3 reads it for the box each image must fill.

## Delivery

One plan, three slices on one branch from `0.2.0-alpha`.

1. **Card boxes.** The ratio table, the content and overlay layers, the
   legacy box, every card list and the inspection overlay. The map's
   layout does not change in this slice; its cards only change shape.
2. **The mat.** `MatLayout`, the mat board, the site card, the strip beside
   the mat, and the reference assets.
3. **Finish.** The glance layer, the phone layout, `DESIGN.md`, and
   `docs/catalog/asset-proportions.md`.

## Testing

- `MatLayout`: every rectangle lies inside the mat; no two slots overlap;
  each site has three card slots; each slot's ratio is within one percent
  of its card kind's ratio; the site card lies inside its frame.
- Render suites: a site is a site card with its cards in the three slots
  beside it; the relic deck, world deck, discards, favor banks, round wheel
  and Visions Drawn marker are children of the mat board at `MatLayout`'s
  positions; a legacy drawn as a card has the legacy box.
- The board option, inspection and partition suites pass unchanged in what
  they assert about keys and behaviour.
- Browser check on a scratch database at 1440×900, 1024×768 and a phone
  width. For alignment the mat image and several card images are injected
  through the browser tools during the check, over the drawn layers; the
  product code shows no licensed asset.

## Follow-ups

- L3 itself: an image for the content layer of each card and for the drawn
  mat, with the drawn layers as fallback.
- The travel-cost band under each region name.
- The canvas port recorded under "Table UI rework" in the roadmap.
