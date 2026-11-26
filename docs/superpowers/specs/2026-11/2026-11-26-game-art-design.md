# Game art from the host's folder

**Status:** designed 2026-11-26. It delivers roadmap item **L3**, building
on the box and layer rules of the
[real proportions design](2026-11-13-real-proportions-design.md) and its
[implementation rulings](2026-11-13-real-proportions-design-implementation-rulings.md)
(R9 above all). It settles the roadmap's open question about the glance
layer: over art, the art alone carries what is printed.

## Goal

A player can turn on **Game art** in the table's settings and see the
licensed card images, the mat, the symbols and the pieces in place of the
drawn faces. The layout is the same with the option on or off. Wherever an
image is slow, missing or unavailable, the player sees the drawn face
instead, box by box.

## Constraints

- No licensed file is committed or shipped. The archive on GitHub holds
  none, and no test reads one. The art reaches a player's browser only from
  a folder the host supplies.
- The layout must not depend on whether art is shown. Every drawn asset
  already has its real proportions, and the mat is laid out on the mat
  image's coordinates ([asset proportions](../../../catalog/asset-proportions.md)).
- Player boards and the reliquary board stay abstract. They are not drawn
  at their proportions, and this design does not change that.

## Out of scope

- Fetching art from public addresses (the Buried Giant card network, the
  Tabletop Simulator mod's links).
- A script that builds the art folder. The host assembles it from a
  documented layout.
- Art on the Chronicle pages, which draw no card faces.
- Player board and reliquary art.

## The art folder

The server takes an optional flag, `--art-path <dir>`. Without it the
server has no art; every client draws the faces it draws today, and the
setting shows disabled.

The folder mirrors the owner's ignored `reference/assets/`, so the owner's
own server runs with `--art-path reference/assets`:

```
<art-path>/
  cards/    a copy of bgs-cards content/card-images/oath/en-US
            (a card's "(NF)" file where the set has one, else its
            base file; other files are ignored)
  symbols/  the 21 symbol-*.png from bgs-cards content/card-symbols/oath
  tts/      mat/, backs/, tokens/, pieces/, markers/, titles/,
            attack-die/, defense-die/, round-die-texture/
```

File names are the source names: `cards/Alchemist (NF).png`,
`cards/Wrestlers.png` for a denizen New Foundations did not reprint
([implementation ruling R1](2026-11-26-game-art-design-implementation-rulings.md)),
`symbols/symbol-suit-arcane.png`, `tts/pieces/pawn-red.png`. A new tracked
page, `docs/operations/game-art.md`, lists every path the client can ask
for and where its file comes from. It holds names only, never art. The
quick start and `docs/operations/configuration.md` link to it, and
`configuration.md` documents the flag.

## Serving the art

At startup the server scans the folder once. It keeps each file whose
extension is `png`, `jpg` or `webp` and whose path is in `ArtCatalog.allPaths`
(below). A rescan needs a restart.

- `GET /art/manifest.json` returns `{"files": [...]}`, the kept paths. It
  is always served; without the flag the list is empty.
- `GET /art/<path>` serves a kept path. Any other path, including one with
  `..` or a wrong extension, is a 404, decided from the list without
  touching the disk.
- Art responses carry `Cache-Control: public, max-age=86400` with an ETag.
  A table loads hundreds of images; revalidating each on every page load
  costs more than a day's staleness after a host replaces a file.
- The server logs one line at startup, `art: <n> files from <dir>`, and a
  warning that names each catalog path the folder lacks, so a host can find
  a bad copy without opening a game.

## The catalog

`ArtCatalog` lives in the `shared` module, so the server and the client read
one table. It maps each game identifier to a path relative to the folder:

- a card and its side to `cards/<Name> (NF).png`, or to the base file
  `cards/<Name>.png` where New Foundations has none;
- a card kind to its back under `tts/backs/`;
- a symbol to `symbols/symbol-<name>.png`;
- a piece and a player colour to `tts/pieces/<piece>-<colour>.png`;
- a token kind, a title and a marker to their files under `tts/`;
- a die face to its sheet's path and the face's crop rectangle in the
  sheet's pixels.

`ArtCatalog.allPaths` is the server's allowlist and the source of the
startup warning. The die crop rectangles are also recorded in
`docs/catalog/asset-proportions.md`.

## The client

### `GameArt`

`GameArt` joins two inputs:

- **The manifest**, fetched once when the page loads. The table renders
  drawn faces at once and redraws when the manifest arrives; nothing waits
  for it. A failed fetch counts as an empty manifest for that page load.
- **The preference**, under the `localStorage` key `distantlands.art`, on
  by default. It is read through the same guard `ChronicleStorage` uses, so
  a browser that refuses storage gets the default, and the switch still
  works for the life of the page.

Renderers ask one question, `GameArt.url(key): Option[String]`. It answers
`Some("/art/<path>")` only when the preference is on and the manifest lists
the key's path, and `None` otherwise. On `None` a renderer draws exactly
what it draws today.

`GameArt` also remembers, for the life of the page, which URLs have loaded
and which have failed (see `ArtLayer`).

### The Settings dialog

A **Settings** button sits in the players pane header, beside Dev tools,
for every player. It opens a dialog built like the development panel
(heading, Close, focus returned to the button) holding one switch:

- **Game art**, a `role="switch"` control, with the hint "Card faces, the
  mat and pieces drawn from the host's art."
- When the manifest is empty, the switch is shown disabled with its reason:
  "This server has no game art." When the manifest fetch failed, the reason
  is "Couldn't load the art list; reload to try again."
- When the manifest lists only part of the catalog, the hint adds
  "<n> of <total> images; the rest stay drawn."
- Flipping the switch saves the preference and redraws the table at once,
  without a reload.

### `ArtLayer`

Every surface swaps art through one helper, `ArtLayer.attach(box, key)`.

- When `GameArt.url(key)` is `None`, it does nothing.
- Otherwise it appends `<img class="art" alt="" aria-hidden="true"
  decoding="async" loading="lazy">`, absolutely positioned to fill the box
  with `object-fit: contain`. The image never sizes the box.
- On `load` it adds `art-ready` to the box. The stylesheet then hides the
  box's drawn content with opacity. Printed content follows its nearest
  box: `.printed` reads the custom property `--printed-opacity`, which
  each box kind sets to 1 and a ready box sets to 0, so a ready box never
  hides what a box inside it draws when that box's own image is missing
  ([implementation ruling R3](2026-11-26-game-art-design-implementation-rulings.md)).
  Opacity, not `display: none`, keeps the drawn text's space and keeps it
  in the accessibility tree, so a screen reader still reads the card's
  name.
- On `error` it removes the image and the box stays drawn.
- The table re-renders on every poll, which builds a fresh image. A URL
  `GameArt` remembers as loaded marks the box `art-ready` at once, since
  the browser already holds the image, so a redraw never flashes the drawn
  face. A URL remembered as failed is not requested again until the page
  reloads.

## The surfaces

- **Card faces:** `CardFace` and `BannerFace` on the table. A foundation,
  the Oath goal and a board's legacy are words on the table (a row, a
  button, a pill) and keep that form; each shows its image in the
  inspection overlay
  ([implementation ruling R13](2026-11-26-game-art-design-implementation-rulings.md)). A face-up card gets its face; a card the
  viewer cannot identify gets its kind's back. A Vision in a denizen box
  shows its landscape image turned a quarter, as `card-face-vision`
  already turns the drawn face. The inspection overlay shows the image
  large; its rules text is not printed and stays.
- **The site card:** its face in the frame. The site-power buttons stay
  unmarked controls (R9), so they stay clickable over the image.
- **The mat:** one image behind the slots of `.mat-board`. The drawn
  washes, frames, region names and slot labels are printed and hide when
  it is ready. The live layer does not change.
- **Symbols in text:** in `RulesTextRenderer` and the glyph chips, each
  glyph keeps its `svg` and holds the symbol's image inside it, fitted to
  the glyph's square; the drawn sprite hides when the image has loaded
  ([implementation ruling R22](2026-11-26-game-art-design-implementation-rulings.md)).
  The fallback is per glyph, and the glyph keeps its spoken label.
- **Pieces and tokens:** `TokenSprite` draws pawns and warbands by player
  colour, and favor, secret, bandits, the titles, the Festival, the first
  player marker and the Visions Drawn marker, each fitted into its square
  glyph box. The black warband gets a light outline (`drop-shadow`) so it
  reads on the dark table.
- **Dice:** `DieFaceChip` shows the die's sheet inside an overflow-hidden
  box, offset to the face's crop rectangle.

Overlays on a box (tokens on cards, selection rings and their marks,
badges) are anchored to the box, not to its content, and need no change.

### The glance layer over art

The zoomed-out glance forms that restate printed content (names, defense)
are themselves printed (R9). They hide with the rest of the printed layer
on an `art-ready` box, at every zoom, with no code of their own. Glance
forms of live state, such as force counts, are not printed and stay: they
are pieces on the table, not print.

## Testing

No licensed file enters a test. Tests that need a folder build a temporary
one of 1×1 PNGs named on catalog paths.

- **Server routes:** without the flag the manifest is empty. With it the
  manifest lists exactly the files present and in the catalog. A listed
  path is served with `max-age=86400` and an ETag. An unlisted path, a path
  with `..` and a wrong extension are 404s. The startup warning names the
  missing paths.
- **Catalog:** `allPaths` equals the path list in `game-art.md`. Every New
  Foundations card in the catalog maps to exactly one path. No two keys
  share a path, except keys that share a back.
- **`GameArt`:** `url` is `None` when the preference is off, when the
  manifest lacks the path, and when the manifest fetch failed. A refused
  storage gives the default.
- **`ArtLayer`:** a dispatched `load` sets `art-ready`; a dispatched `error`
  removes the image. A remembered loaded URL is ready at once; a remembered
  failed URL makes no image.
- **Surfaces:** for each surface, with art on the box holds an `img.art`
  with the catalog's `src`, and with art off it holds none.
- **Layout invariance:** one projection rendered with art on and with art
  off gives the same box classes and inline geometry once the `.art`
  elements are removed.
- **Settings:** the disabled reasons, the partial count, and that the
  switch saves the preference and redraws.
- **Browser check** on a scratch database with
  `--art-path reference/assets`, at 1440×900, 1024×768 and a phone width:
  screenshots with art on and off show the same layout; a file blocked in
  the network tools stays drawn; and the three rectangles placed by eye
  (the tracker's line, the Visions Drawn label and the favor bank boxes)
  cover no printed art on the mat.

## Delivery

One branch, three slices, each passing the full gate.

1. **Plumbing and cards.** `--art-path`, the scan, the manifest, the `/art`
   route and the startup log; `ArtCatalog` and `game-art.md`; `GameArt`, the
   preference, the Settings dialog and `ArtLayer`; card faces, backs and
   the inspection overlay.
2. **The world board.** The mat, site cards, banners, legacies,
   foundations and oath goals, and the check of the three rectangles
   placed by eye.
3. **Small art and finish.** Symbols in text, pieces, tokens and dice with
   their crop rectangles in `asset-proportions.md`; `DESIGN.md`; the quick
   start and configuration links to `game-art.md`; L3 deleted from the
   roadmap.
