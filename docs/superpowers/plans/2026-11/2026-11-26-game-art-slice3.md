# Game Art, Slice 3: Small Art and Finish Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** With Game art on, the symbols in rules text, the favor and secret
tokens, warbands, pawns, the Visions Drawn marker and the dice show the
licensed art; a site's power buttons show as chips over its art; an empty
pile lets the mat's printed slot show. The docs describe the feature, and
L3 leaves the roadmap.

**Architecture:** Slices 1 and 2 built `ArtCatalog`, `GameArt`, `ArtLayer`
and the nearest-box rule. A glyph is an inline `svg.token-glyph` drawing a
sprite symbol; this slice lays the symbol's image inside that same `svg`
as an SVG `<image>`, so no element, class or size changes with art off,
and each glyph falls back on its own. Pieces that are text marks (a pawn's
dot) and the die chip take an `<img>` through `ArtLayer`. A die face is a
crop of its sheet: the image is sized and offset inside an overflow-hidden
square so only the face shows.

**Tech Stack:** Scala 3, Scala.js, munit, jsdom, CSS, `./sbtw`, compiler
flags including `-Werror`.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md),
"The surfaces" (symbols, pieces and tokens, dice) and "Delivery", item 3.
Rulings: the
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md),
R1 to R21. Earlier plans: [slice 1](2026-11-26-game-art-slice1.md),
[slice 2](2026-11-26-game-art-slice2.md).

## Open questions this plan waits on

The plan is written to one answer for each. No production code is written
until the orchestrator rules, and slice 2's review fixes land first.

- **Q1. How a glyph holds its image.** The design says each glyph "becomes
  a span holding the printed character and a 1em image". Today a glyph is
  one `svg.token-glyph`, which some two hundred test selectors and forty
  stylesheet rules name. The plan keeps that element and adds an SVG
  `<image>` inside it, hiding the sprite's `<use>` when the image has
  loaded. Nothing changes with art off, the glyph keeps its `role="img"`
  and label, and the fallback is per glyph (Task 2).
- **Q2. Favor and secrets: two pictures.** The folder has the printed
  symbols (`symbols/symbol-favor.png`, `symbol-secret.png`) and pictures
  of the tokens (`tts/tokens/favor.png`, `secret.png`). The plan uses the
  symbol where a card prints it (rules text, a power's cost, a site's
  Forge price, a decision's cost badge) and the token's picture where
  tokens lie in play (on a card, on a site, in a bank, on a banner, on a
  player board). The split needs a second entry point beside
  `RulesTextRenderer.glyph` (Task 3). The simpler answer is the symbol
  everywhere.
- **Q3. Which die is which symbol.** The card data settles two:
  `symbol:diceb` is where our text has `[defense-die]` (Wrestlers) and
  `symbol:dicer` where it has `[attack-die]` (Bear Traps). `[round-die]`
  takes `symbol-dicep.png` by elimination. Confidence on the last: low.
- **Q4. The die chip.** A chip is 1.5em tall and at least 1.9em wide, and
  grows with its glyphs. The plan fits the face's square crop inside the
  chip, centred, hides the chip's glyphs and its "×2" when the image is
  ready, and leaves the chip's size alone (Task 6). The crop rectangles
  are measured from the two sheets during execution and recorded in
  `asset-proportions.md`; a test holds the page and the code equal.
- **Q5. Warbands and pawns by colour.** A player's pawn and warbands use
  `tts/pieces/pawn-<colour>.png` and `warband-<colour>.png`; Imperial
  warbands use purple; bandits use `tts/tokens/bandit.png`. The warband
  the Supply track prints under each band is print on an abstract board
  and stays drawn (Task 4).
- **Q6. The light outline.** The design gives the black warband a light
  outline. The plan gives the black pawn the same (Task 4).
- **Q7. The titles.** The Oathkeeper and Usurper title is a pill on a
  player board; it stays words (as R13, R14). Its overlay plate
  (`card-plate-oathkeeper-title`, 1016 : 700, from the print-and-play
  sheet) would hold `tts/titles/oathkeeper.jpg` or `usurper.jpg`, which
  are 602×827, portrait. The plan attaches the image to the plate and
  decides in the browser check whether it is turned a quarter or the
  plate takes the image's ratio; it stops for a ruling if the plate's
  ratio must change (Task 5).
- **Q8. The festival marker.** On the map the marker is a band across
  three card slots, about 11 : 1; the art (`festival-underway.jpg`) is
  2.9 : 1 and would be a sliver there. The plan puts the image on the
  overlay's printed plate, which has the art's shape, and leaves the map's
  band drawn (Task 5).
- **Q9. The first player marker.** It is a pill on a player board, "First
  Player". The plan leaves it words and lists no file for it (R14's
  reasoning).
- **Q10. The Visions Drawn marker and the round marker.** The Visions
  Drawn marker has a picture and takes its cell on the mat. The round
  marker has only a 3D texture, so the current round's wedge stays drawn
  (Task 5).
- **Q11. Restriction marks.** A drawn card's adviser-only and site-only
  banners take `symbol-restrictionperson.png` and
  `symbol-restrictiontree.png`. An edifice's tower marks have no symbol
  and stay drawn (Task 2).
- **Q12. Files with no surface.** `symbol-chancellor.png`, `symbol-x2.png`,
  the suit tiles, `tts/tokens/usurper.png`, `festival-back.jpg`,
  `first-player.jpg` and the round die's texture are not listed (R6, R19).

## Global Constraints

- Work only in `/Users/roman/projects/distantlands/.worktrees/game-art`, on
  branch `game-art`. Never commit on `0.2-alpha` or `main`.
- Licensed art: never commit, copy into the worktree, or upload any file
  from `/Users/roman/projects/distantlands/reference/assets/`. No test
  reads a licensed file. Tracked docs list file names and numbers only.
- `.tooling` and `node_modules` are symlinks. Never `git add -A` or
  `git add .`; add files by path.
- The layout must not depend on whether art is shown. A ground under live
  state is a fill and a shadow of one colour, never padding. The
  invariance tests and the browser's rectangle comparison cover every box
  this slice touches, the site-power chips included (R20).
- With art off, or with no manifest, every renderer builds the DOM it
  builds today, apart from a class this plan adds for art on and off
  alike.
- `docs/operations/game-art.md` lists exactly `ArtCatalog.allPaths`, in
  order (R6). The die crop rectangles in `asset-proportions.md` equal the
  catalog's.
- A rule that sets opacity on an element drawn together with `printed`
  multiplies by `--printed-opacity` (the slice 1 review's guard test).
- Change a selector list with an exact, counted replacement, never a
  pattern that can match other rules; check the diff of `styles.css`
  before each commit.
- Never chain a commit after a test run with `;`. Use `&&`, and a test
  command whose exit status is the suite's.
- Contrast is measured, not judged by eye: text on a ground meets 4.5 : 1
  against the ground composited over white and over black, the two worst
  cases for a translucent ground over art (R20).
- No `transition` and no `animation`. No new colour.
- The build is `-Werror`; `scripts/check-architecture.py` enforces size
  limits (800 lines a file) and layering.
- Never write a bare `===` or `=word` in a shell command.
- Never touch the live database `var/distantlands*`. The browser check
  uses a scratch database on port 8083.
- Docs, comments, commit messages and plans are plain English prose. Test
  helper names state what they build. UI copy uses the terms in
  `CONTEXT.md` and `docs/rules/glossary.yaml`.
- When roadmap work is finished, delete the item outright: no "[x]" and
  no "landed" note.
- Commit messages are Conventional Commits and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after every commit.

### Protected

These must behave exactly as before. Do not edit them except where a step
names the edit:

- Slices 1 and 2: `GameArt`, `ArtLayer.attach`, card faces and backs, the
  mat, sites, banners, piles, plates, the Settings dialog. Their suites
  pass without edits.
- `RulesTextRenderer.glyph(token)`: its element, class, role, label and
  `<use>`. Every existing `token-glyph` selector keeps matching.
- `TokenSprite`: the sprite and its ids.
- `DieFaceChip.face` and `roll`: the chip's classes, label and glyphs with
  art off. `DieFace` and `Die` in the shared protocol.
- `MatLayout`'s numbers.
- The `printed` marking, except the two elements Task 7 names.
- Player boards and the reliquary board stay abstract. The Chronicle
  pages draw no art.

## File Structure

| File | Responsibility |
|---|---|
| `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala` | Keys for symbols, tokens, pieces, markers, titles and dice; `DieCrop`. |
| `docs/operations/game-art.md`, `docs/catalog/asset-proportions.md` | The new paths; the die crop rectangles. |
| `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala` | `attachGlyph` (SVG image) and `attachCrop` (a crop of a sheet). |
| `frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala` | Symbols in glyphs; `piece`, the token as it lies in play. |
| `frontend/src/main/scala/distantlands/frontend/PowerMarks.scala` | The two restriction symbols. |
| `frontend/src/main/scala/distantlands/frontend/CardFace.scala`, `BannerFace.scala`, `ServerUiSupport.scala`, `WorldBoardRenderer.scala` | Tokens in play, warbands, pawns, the Visions Drawn marker, the title plate. |
| `frontend/src/main/scala/distantlands/frontend/DieFaceChip.scala` | The face's crop. |
| `frontend/src/main/scala/distantlands/frontend/FestivalMarker.scala` | The printed plate's image. |
| `frontend/styles.css` | The glyph image, pieces, dice, R20 chips, R21 piles. |
| `DESIGN.md`, `docs/operations/configuration.md`, `docs/operations/quick-start.md`, `docs/ROADMAP.md`, `build.sbt` | The finish. |

---

### Task 1: Catalog keys for small art

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala`
- Modify: `docs/operations/game-art.md`
- Test: `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala`

**Interfaces:**
- Produces:
  - `ArtCatalog.symbol(token: String): Option[ArtKey]` for the sprite's
    token names. The table:

    | Token | File under `symbols/` |
    |---|---|
    | `favor` | `symbol-favor.png` |
    | `favor-burnt` | `symbol-favorb.png` |
    | `secret` | `symbol-secret.png` |
    | `secret-burnt` | `symbol-secretb.png` |
    | `suit-arcane`, `-beast`, `-discord`, `-hearth`, `-nomad`, `-order` | `symbol-suit-<suit>.png` |
    | `attack-die` | `symbol-dicer.png` |
    | `defense-die` | `symbol-diceb.png` |
    | `round-die` | `symbol-dicep.png` |
    | `skull` | `symbol-skull.png` |
    | `shield` | `symbol-shield.png` |
    | `sword` | `symbol-sword.png` |
    | `hollow-sword` | `symbol-swordh.png` |

    `warband` has no symbol.
  - `ArtCatalog.restriction(kind: String): Option[ArtKey]`: `"adviser"`
    is `symbols/symbol-restrictionperson.png`, `"site"` is
    `symbols/symbol-restrictiontree.png`; the towers have none. The kind
    names are `PowerMarks.restriction`'s; read them from that file.
  - `ArtCatalog.token(token: String): Option[ArtKey]`: `"favor"` is
    `tts/tokens/favor.png`, `"secret"` is `tts/tokens/secret.png`.
  - `ArtCatalog.pawn(colour: String): Option[ArtKey]` and
    `ArtCatalog.warband(force: String): Option[ArtKey]`, for the eight
    `PlayerColor` keys: `tts/pieces/pawn-<colour>.png`,
    `tts/pieces/warband-<colour>.png`. `warband("empire")` is the purple
    warband; `warband("bandit")` is `tts/tokens/bandit.png`.
  - `ArtCatalog.visionsDrawnMarker: ArtKey`,
    `tts/markers/visions-drawn-marker.png`.
  - `ArtCatalog.festivalMarker: ArtKey`, `tts/tokens/festival-underway.jpg`.
  - `ArtCatalog.title(side: String): Option[ArtKey]`: `"oathkeeper"` and
    `"usurper"` are `tts/titles/<side>.jpg`.
  - `final case class DieCrop(sheet: ArtKey, x: Int, y: Int, w: Int,
    h: Int, sheetW: Int, sheetH: Int)` and
    `ArtCatalog.dieFace(key: String): Option[DieCrop]` for the seven
    `DieFace` keys. The sheets are `tts/attack-die/attack_die.png` and
    `tts/defense-die/defense_die.png`, both 2048×2048. Task 6 fills the
    rectangles; this task adds the two sheet paths and an empty table.
  - `allPaths` grows by 17 + 2 + 2 + 16 + 1 + 1 + 1 + 2 + 2 = 44 paths,
    appended after slice 2's in that order: symbols, restrictions, tokens,
    pawns, warbands, the bandit warband, the Visions Drawn marker, the
    festival marker, the titles, the die sheets.

- [ ] **Step 1: Write the failing tests**

Append to `ArtCatalogSuite`:

- "every sprite token but the warband has its symbol": for each row of
  the table above, `ArtCatalog.symbol(token).map(_.path)` is
  `Some("symbols/<file>")`; `symbol("warband")` and `symbol("unknown")`
  are `None`. Write the table in the test as a `Vector` of pairs.
- "the adviser-only and site-only banners have their symbols, and the
  towers none".
- "favor and secrets have a token's picture, and nothing else does".
- "each player colour has a pawn and a warband": iterates
  `distantlands.model.PlayerColor.all`.
- "Imperial warbands are purple and bandits have their own piece".
- "the markers, the titles and the die sheets have their files".
- "the die sheets are square and each listed once".
- Update the slice 1 count test: add `+ 44` to the expected size.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: compilation fails, `value symbol is not a member of object
ArtCatalog`.

- [ ] **Step 3: Add the keys**

Write each table as a private `Vector` of pairs mapped to `ArtKey`, as
`banners` and `oathGoals` are, with a lookup `def` per kind. Build the
piece paths from `Vector("purple", "red", "blue", "yellow", "white",
"black", "pink", "brown")`; the shared module may not name
`PlayerColor`, so the test holds the list equal to `PlayerColor.all`.
Append the new paths to `allPaths` in the order given above.

- [ ] **Step 4: List the paths in `game-art.md`**

Add `symbols/` to the folder sketch and the `tts/` folders `tokens/`,
`pieces/`, `markers/`, `titles/`, `attack-die/`, `defense-die/`, each
with one bullet naming its source (the card database's
`content/card-symbols/oath`; the mod, as `tts/SOURCES.md` in the owner's
folder lists it). Append the 44 bullets in `allPaths` order.

- [ ] **Step 5: Run the tests and the link check**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: PASS.

Run: `python3 scripts/check-markdown-links.py`
Expected: no error.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala \
  docs/operations/game-art.md \
  src/test/scala/distantlands/catalog/ArtCatalogSuite.scala
git commit -m "feat(protocol): add symbols, pieces, markers and dice to the art catalog

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: Symbols in text

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala`
  (`glyph`)
- Modify: `frontend/src/main/scala/distantlands/frontend/PowerMarks.scala`
  (`restriction`)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/GlyphArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `ArtLayer.attachGlyph(glyph: dom.Element, key: Option[ArtKey]):
  Unit`. `glyph` is an `svg`. It appends
  `<image class="art" href="<url>" width="100%" height="100%"
  preserveAspectRatio="xMidYMid meet" aria-hidden="true">` in the SVG
  namespace, marks the `svg` `art-ready` on `load`, and removes the image
  on `error`, with the same remembered-URL behaviour as `attach`.
- Consumes: `ArtCatalog.symbol(token)`, `ArtCatalog.restriction(kind)`.

A glyph's box is square and set by its stylesheet rule; the image is
fitted inside it and never sizes it. The sprite's `<use>` is the glyph's
printed mark: it hides when the glyph is ready.

- [ ] **Step 1: Write the failing tests**

Create `GlyphArtSuite` with `afterEach` resetting `GameArt`:

- "a glyph with a symbol holds its image inside the same svg": with the
  whole catalog listed, `RulesTextRenderer.glyph("favor")` is still an
  `svg` with class `token-glyph token-favor`, `role="img"` and label
  "favor", holds its `<use href="#token-favor">`, and holds one
  `image.art` whose `href` is `/art/symbols/symbol-favor.png`.
- "each symbol's glyph asks for its own file": the Task 1 table.
- "the warband glyph and an unknown token hold no image".
- "a load marks the glyph ready; an error removes the image and leaves the
  sprite": dispatch on the `image`.
- "with art off, and before the manifest arrives, a glyph is the svg it
  is today": compare `outerHTML` with a glyph built after
  `GameArt.reset()`.
- "rules text draws each token with its symbol, one image a glyph":
  `RulesTextRenderer.power("Gain [favor] [favor] from the [suit-order]
  bank.")` holds three `image.art`, in order.
- "a drawn card's adviser-only and site-only banners hold their symbols,
  and a tower holds none": `PowerMarks.restriction` on three cards.
- "a ready card hides its header's glyphs with the header": a card face
  with art holds glyph images only inside `.printed`, so nothing new
  shows over the card's art.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.GlyphArtSuite"`
Expected: compilation fails, `value attachGlyph is not a member of object
ArtLayer`.

- [ ] **Step 3: Write `attachGlyph` and call it**

In `ArtLayer`, factor the URL and memory handling `attach` already has
into a private helper both use, then:

```scala
  /** Lays `key`'s image inside the glyph `svg`, fitted to its square box.
    * The sprite's `<use>` is what the glyph draws itself; it hides when
    * the image has loaded and stays when it fails. */
  def attachGlyph(glyph: dom.Element, key: Option[ArtKey]): Unit =
    key.flatMap(GameArt.url).filterNot(GameArt.hasFailed).foreach { url =>
      val image = ServerUiSupport.svgElement("image")
      image.setAttribute("class", "art")
      image.setAttribute("href", url)
      image.setAttribute("width", "100%")
      image.setAttribute("height", "100%")
      image.setAttribute("preserveAspectRatio", "xMidYMid meet")
      image.setAttribute("aria-hidden", "true")
      watch(image, url, glyph)
      glyph.appendChild(image)
    }
```

`watch(image, url, marked)` is the shared part: the remembered-loaded
mark, and the `load` and `error` listeners.

In `RulesTextRenderer.glyph`, before `node` is returned:
`ArtLayer.attachGlyph(node, ArtCatalog.symbol(token))`.

In `PowerMarks.restriction`, attach
`ArtCatalog.restriction(kind)` to the mark's `svg` frame.

- [ ] **Step 4: Add the rules, test first**

In `StylesheetArtSuite` add "a ready glyph hides its sprite and shows its
image": `rule(".token-glyph.art-ready > use")` and
`rule(".restriction-mark.art-ready > :not(.art)")` contain `opacity: 0`;
`rule("svg > .art")` resets what `.art` sets for an `<img>`
(`position: static; z-index: auto`). Then in `frontend/styles.css`, after
the plate's rule:

```css
/* A glyph's image lies inside its svg, fitted to the glyph's square. The
   sprite the glyph draws itself hides when the image has loaded. */
svg > .art { position: static; z-index: auto; }
.token-glyph.art-ready > use { opacity: 0; }
.restriction-mark.art-ready > :not(.art) { opacity: 0; }
```

The guard test from the slice 1 review reads a rule's subject, its last
compound. These rules' subjects are `use` and `:not(.art)`, children of
the glyph, not the glyph itself, so the guard does not apply to them. If
it objects all the same, narrow the guard with a comment saying that a
child selector is not the printed element; do not weaken it otherwise.

- [ ] **Step 5: Run the suites**

Run: `./sbtw frontend/test` and
`./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS. `RulesTextRendererSuite`, `PowerMarksSuite`,
`SuitGlyphSuite` and `CardFaceSuite` unedited.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ArtLayer.scala \
  frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/PowerMarks.scala \
  frontend/src/test/scala/distantlands/frontend/GlyphArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): draw symbols in text from the host's art

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Favor and secrets in play

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala`
- Modify the callers that draw tokens lying in play:
  `CardFace.counted`, `BannerFace.render`, `ServerUiSupport.siteToken`,
  `WorldBoardRenderer.counted` and the favor bank's favor glyph
  (`WorldBoardRenderer.favorBanks`)
- Test: `frontend/src/test/scala/distantlands/frontend/GlyphArtSuite.scala`

**Interfaces:**
- Produces: `RulesTextRenderer.piece(token: String): dom.Element`: the
  same `svg.token-glyph` `glyph` builds, with the token's picture
  (`ArtCatalog.token(token)`) when it has one and its symbol otherwise.
- The callers that stay on `glyph`: rules text, a power's cost
  (`CardInspectionOverlay.powerFace`), a site's Forge price
  (`ServerUiSupport.requirementCorner`), cost badges, a card's suit, the
  bank's suit, defense dice.

- [ ] **Step 1: Write the failing tests**

- "a token lying in play holds the token's picture":
  `RulesTextRenderer.piece("favor")` holds
  `/art/tts/tokens/favor.png`; `piece("secret")` the secret's.
- "a piece with no picture of its own falls back to its symbol":
  `piece("favor-burnt")` holds `symbol-favorb.png`.
- "favor on a card, on a site, in a bank and on a banner is the token;
  favor in rules text and in a Forge price is the symbol": one board
  from `WorldArtSuite`'s fixture pattern, with a card holding favor, a
  site with loose tokens and a Forge price, a bank and a banner.
- "with art off a piece is the glyph it is today".

- [ ] **Step 2: Run, see them fail, implement**

`piece` builds `glyph`'s element without the symbol and attaches
`ArtCatalog.token(token).orElse(ArtCatalog.symbol(token))`. To keep one
builder, give `glyph` a private twin taking the key.

- [ ] **Step 3: Run the suites and commit**

Run: `./sbtw frontend/test`
Expected: PASS, existing suites unedited.

```bash
git add frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/CardFace.scala \
  frontend/src/main/scala/distantlands/frontend/BannerFace.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/GlyphArtSuite.scala
git commit -m "feat(frontend): show favor and secret tokens where they lie in play

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Warbands and pawns

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala`
  (`warband(force)`)
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala`
  (warbands on a card), `ServerUiSupport.scala` (a site's short forces),
  `WorldBoardRenderer.scala` (`pawnMarker`)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/PieceArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `RulesTextRenderer.warband(forces: SiteForces): dom.Element`,
  the warband glyph holding the force's piece: a player's colour, purple
  for the Empire, the bandit piece for bandits.
- A pawn's dot (`span.pawn-dot`, the character "●" in the player's
  colour) holds an `<img class="art">` through `ArtLayer.attach`; when
  ready the dot's character is transparent and the image shows in its
  place, the same size.

The force's key is the suffix `ServerUiSupport.forceCssClass` already
computes (`force-red`, `force-empire`, `force-bandit`); read the key from
the same match so the two cannot disagree. The warband under each band of
the Supply track stays `glyph("warband")`.

- [ ] **Step 1: Write the failing tests**

- "a player's warbands on a card and in a site's short forces hold that
  colour's piece".
- "Imperial warbands hold the purple piece and bandits their own".
- "a pawn on the map holds its player's pawn, and keeps its dot, its
  name and its initial": the three forms stay in the DOM.
- "the Supply track's warband holds no image".
- "with art off no piece holds an image, and the board's markup is what
  it is today": the invariance comparison on a board with forces and
  pawns in three colours.

- [ ] **Step 2: Implement**

`warband(forces)` builds `glyph("warband")`'s element and attaches
`ArtCatalog.warband(key)` with `attachGlyph`. In `pawnMarker`,
`ArtLayer.attach(dot, ArtCatalog.pawn(colourKey))`, where `colourKey` is
the player's `PlayerColor.key`.

- [ ] **Step 3: Add the rules, test first**

```css
/* A pawn's dot holds the pawn's picture, fitted to the dot's own box:
   the character keeps its room and gives up its ink. */
.pawn-dot:has(> .art) { position: relative; display: inline-block; }
.pawn-dot.art-ready { color: transparent; }
/* The black pieces would vanish on the dark table: a light edge. */
.token-warband > .art[href$="warband-black.png"],
.pawn-dot > .art[src$="pawn-black.png"] {
  filter: drop-shadow(0 0 1px var(--cream)); }
```

`display: inline-block` on the dot must not change its box: compare the
dot's rectangle with art on and off in the browser check; if it moves,
drop the declaration and position the image against the pawn instead.
`drop-shadow` on an SVG `<image>` inside an `svg` applies in current
browsers; confirm in the browser check and, if it does not, put the
filter on the glyph's `svg` through a class the renderer adds for a black
force.

- [ ] **Step 4: Run the suites and commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/RulesTextRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/CardFace.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/test/scala/distantlands/frontend/PieceArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show warbands and pawns as pieces

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: Markers, the festival plate and the title plate

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  (`visionsDrawn`; the title pill's `openText`)
- Modify: `frontend/src/main/scala/distantlands/frontend/FestivalMarker.scala`
  (`plate`)
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/PieceArtSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/PlateArtSuite.scala`
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Consumes: `ArtCatalog.visionsDrawnMarker`, `festivalMarker`,
  `title(side)`.

**The Visions Drawn marker.** `span.vision-marker` is placed on the
current cell; the stylesheet draws the marker as its `::after`. The image
goes in the span; when ready the `::after` hides.

**The festival marker.** `FestivalMarker.plate(withRule = true)`, the
printed marker in the overlay, holds the image; its drawn shape, title
and rule hide when it is ready. They are not marked `printed` today, so
the plate is its own case: `.festival-plate-printed.art-ready > :not(.art)
{ opacity: 0 }`. The band on the map and the copy beside the Darkest
Secret's rules (`withRule = false`) stay drawn (Q8).

**The title.** The pill on a board opens `openText(..., Some(
"oathkeeper-title"))`; pass `ArtCatalog.title(oath.side)` as its art
(Q7).

- [ ] **Step 1: Write the failing tests**

- "the Visions Drawn marker holds its picture on the current cell".
- "the printed festival plate holds the marker's picture; the map's band
  and the Festival's copy hold none".
- "the title plate holds the title's picture for the side in force":
  `oathkeeper` and `usurper`.
- Stylesheet: the two hide rules; `.festival-plate-printed:has(> .art)`
  and `.vision-marker` are positioned boxes.

- [ ] **Step 2: Implement, run, commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/src/main/scala/distantlands/frontend/FestivalMarker.scala \
  frontend/src/test/scala/distantlands/frontend/PieceArtSuite.scala \
  frontend/src/test/scala/distantlands/frontend/PlateArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show the Visions Drawn marker, the festival plate and the title

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Dice

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala`
  (the crop table)
- Modify: `docs/catalog/asset-proportions.md` (a "Die faces" table)
- Modify: `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala`
  (`attachCrop`)
- Modify: `frontend/src/main/scala/distantlands/frontend/DieFaceChip.scala`
- Modify: `frontend/styles.css`
- Test: `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/DieArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**Interfaces:**
- Produces: `ArtLayer.attachCrop(box: dom.Element, crop: Option[DieCrop]):
  Unit`. It appends `<span class="art-crop">` holding `<img class="art">`.
  The span is a square, centred in `box`, as tall as the box, with
  `overflow: hidden`. The image is sized and offset in percentages of the
  span so the crop fills it:
  `width = sheetW / w`, `height = sheetH / h`, `left = -x / w`,
  `top = -y / h`, each times 100%. The box is marked `art-ready` on load.
- Consumes: `ArtCatalog.dieFace(key)`.

- [ ] **Step 1: Measure the crop rectangles**

Start the scratch server (the die sheets are in the catalog since
Task 1). In the browser, for each sheet, draw it into a canvas for
inspection and find each face: the seven faces are `hollow-sword`,
`one-sword`, `two-swords-skull` on the attack die and `blank`,
`one-shield`, `two-shields`, `doubler` on the defense die. A die has six
sides, so a face that appears on several sides is measured once, on the
side whose square is cleanest. Record each face's `x`, `y`, `w`, `h` in
the sheet's pixels, with `w` equal to `h`. Outline the seven rectangles
on the canvases and take a screenshot of each sheet for the report.

- [ ] **Step 2: Write the failing tests**

In `ArtCatalogSuite`:

- "every die face has a square crop inside its die's sheet": for each
  `DieFace`, the crop's sheet is its die's, `w == h`, and the rectangle
  lies within 2048×2048.
- "two faces of one die do not share a crop".
- "asset-proportions.md records the die crops the catalog holds": the
  page's "Die faces" table, parsed row by row, equals the catalog.

In `DieArtSuite`:

- "a rolled face holds its crop of the die's sheet": the chip holds one
  `.art-crop > img.art` whose `src` is the sheet's and whose inline
  `width`, `height`, `left` and `top` are the percentages the crop gives.
- "a face keeps its glyphs, its label and its classes under the crop".
- "an unknown face and a face with art off hold no crop".
- "a load marks the chip ready; an error removes the crop".

- [ ] **Step 3: Implement**

Fill the table in `ArtCatalog`, add the "Die faces" table to
`asset-proportions.md` (columns: face, die, x, y, w, h), write
`attachCrop`, and call it at the end of `DieFaceChip.face`:
`ArtLayer.attachCrop(node, ArtCatalog.dieFace(key))`.

- [ ] **Step 4: Add the rules, test first**

```css
/* A die face is a crop of its die's sheet: a square as tall as the chip,
   centred in it, showing only the face. The chip's own glyphs hide when
   the crop has loaded; the chip keeps its size. */
.die-face:has(> .art-crop) { position: relative; isolation: isolate; }
.art-crop { position: absolute; top: 0; bottom: 0; left: 50%;
  aspect-ratio: 1; overflow: hidden; transform: translateX(-50%);
  border-radius: inherit; pointer-events: none; }
.art-crop > .art { position: absolute; inset: auto; z-index: auto;
  max-width: none; object-fit: fill; }
.die-face.art-ready > :not(.art-crop) { opacity: 0; }
.die-face.art-ready { color: transparent; }
```

`color: transparent` hides the doubler's "×2", which is a text node. The
chip's fill and border stay: they frame the crop.

- [ ] **Step 5: Run the suites and commit**

```bash
git add shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala \
  docs/catalog/asset-proportions.md \
  frontend/src/main/scala/distantlands/frontend/ArtLayer.scala \
  frontend/src/main/scala/distantlands/frontend/DieFaceChip.scala \
  frontend/src/test/scala/distantlands/frontend/DieArtSuite.scala \
  src/test/scala/distantlands/catalog/ArtCatalogSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show a rolled face as a crop of its die's sheet

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: Site-power chips and empty piles (R20, R21)

**Files:**
- Modify: `frontend/styles.css`
- Modify: `docs/superpowers/specs/2026-11/2026-11-26-game-art-design.md`
  (the site card bullet)
- Test: `frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala`
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`

**R20.** On a ready site each power button is a chip: its label visible on
the name band's ground, in the place and the box it has today. The label
is a `span.printed` inside the button; on a ready site it stops following
`--printed-opacity`.

**R21.** An empty pile on a ready mat drops its drawn fill and border, so
the mat's printed slot shows; its count stays. With the mat not ready an
empty pile is unchanged.

- [ ] **Step 1: Write the failing tests**

Stylesheet:

- "on a ready site a power button's label stays visible, on the name
  band's ground": `rule(".site.art-ready .site-power > .printed")`
  contains `opacity: 1`; `rule(".site.art-ready .site-power")` contains
  `background: #1a1610d9`, `box-shadow: 0 0 0 3px #1a1610d9` and
  `border-radius: 3px`, and none of `padding`, `margin`, `width`,
  `height`, `border:`.
- Extend the guard test's understanding: `.site-power > .printed` sets
  opacity without the layer's variable on purpose. Add it to an
  "exempt, by ruling R20" list in the test, with the ruling named.
- "an empty pile on a ready mat drops its fill and keeps its count":
  `rule(".mat-board.art-ready .deck-card.pile-empty")` contains
  `background: none` and `border-color: transparent`.

`WorldArtSuite`: "a site's power button keeps its label and its box with
art and without" is already inside the invariance test; add an assertion
that the board's markup with art holds each `.site-power` with its
`span.printed` label.

- [ ] **Step 2: Add the rules**

```css
/* A site's power is a control, not print (R9, R20): over the site's art
   its button shows as a chip, the label on the name band's ground, where
   and as large as the button already is. */
.site.art-ready .site-power { border-radius: 3px; background: #1a1610d9;
  box-shadow: 0 0 0 3px #1a1610d9; }
.site.art-ready .site-power > .printed { opacity: 1; }
/* The mat prints each pile's slot, so an empty pile gives way to it. */
.mat-board.art-ready .deck-card.pile-empty { border-color: transparent;
  background: none; }
```

- [ ] **Step 3: Amend the spec**

In the design's "The site card" bullet, replace the sentence about the
buttons with: "The site-power buttons are controls, not print: over the
image each shows as a chip, its label on the name band's ground, in the
place it has on the drawn face (implementation rulings R15, R20)." Record
nothing new in the rulings file; R20 and R21 are recorded when this plan
is committed.

- [ ] **Step 4: Measure the contrast**

For each text colour that stands on the ground `#1a1610d9` (the power
label's, the forces' three colours, the token count's, a pawn's name),
compute the WCAG contrast ratio against the ground composited over white
and over black. Read the colours from the stylesheet. Use this script:

```bash
python3 - <<'PY'
def lin(c):
    c /= 255
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
def lum(rgb):
    return sum(w * lin(v) for w, v in zip((0.2126, 0.7152, 0.0722), rgb))
def ratio(a, b):
    hi, lo = sorted((lum(a), lum(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)
ground, alpha = (0x1a, 0x16, 0x10), 0xd9 / 255
for under in ((255, 255, 255), (0, 0, 0)):
    over = tuple(round(alpha * g + (1 - alpha) * u) for g, u in zip(ground, under))
    for name, text in TEXTS:
        print(under, name, round(ratio(text, over), 2))
PY
```

with `TEXTS` filled from the stylesheet. Every ratio must be at least
4.5. A colour that fails gets the ground's alpha raised for its rule, or
is reported. The same for the pile count's ground (`var(--base)`, opaque)
and the tracker line's halo, which has no ground: report its worst case
against the mat by sampling, in the browser check.

- [ ] **Step 5: Run the suites and commit**

```bash
git add frontend/styles.css \
  frontend/src/test/scala/distantlands/frontend/WorldArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  docs/superpowers/specs/2026-11/2026-11-26-game-art-design.md
git commit -m "feat(frontend): show site powers as chips and let empty piles show the mat

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 8: Docs and the roadmap

**Files:**
- Modify: `DESIGN.md`
- Modify: `docs/operations/configuration.md`,
  `docs/operations/quick-start.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/catalog/asset-proportions.md` (its opening paragraph)
- Modify: `build.sbt` (`verifyPackageMappings`' required files)
- Test: `src/test/scala/distantlands/server/` (a docs test, if the
  existing docs suites have a pattern for it; else none)

- [ ] **Step 1: `configuration.md`**

Add a row to the options table:
`| \`--art-path DIR\` | \`DLQ_ART_PATH\` | unset; no game art |`, and a
short paragraph under the table: what the folder is, that the server
reads it once at startup and logs what it found, that a path that is not
a folder serves no art and logs a line (R4), and a link to `game-art.md`
with the text "game art".

- [ ] **Step 2: The quick start**

Add one optional step, after the server is running and reachable: "To
show the game's own art, see game art.", the last two words linking to
`game-art.md`. Place it where the
page lists optional setup; do not interrupt the numbered first-run steps.

- [ ] **Step 3: `DESIGN.md`**

- "Navigation": the Players pane header carries Settings for every
  player, and Dev tools beside it in development.
- "Buttons": add Settings and its Close to the Control list.
- A new section, "Game Art", after the section on cards: the Settings
  dialog and its switch (Withheld when the server has no art); the art
  layer (an image over a box, `art-ready`, printed content hidden by its
  nearest box, the drawn face as the permanent fallback); what stays over
  art and its ground (`#1a1610d9` fill and same-colour shadow, the
  measured contrast from Task 7); site-power chips (R20); glyph images;
  the die crop; the black pieces' light edge; the Vision's quarter turn
  (R8).
- Fix every sentence that says an image "will" go somewhere.

- [ ] **Step 4: The roadmap**

Delete the L3 item whole, with its open question about the glance layer
and its note about the three rectangles: both are answered (the design's
"The glance layer over art"; rulings R18). Search for other places that
still call the glance question open or point at L3:

Run: `grep -rn "L3\b" docs README.md DESIGN.md PRODUCT.md CONTEXT.md --include=*.md | grep -v "superpowers/"`
and `grep -rn "glance layer" docs/ROADMAP.md docs/operations docs/catalog`

Fix each hit in a living document. In `asset-proportions.md`, "roadmap
item L3 reads it" becomes a link to the game art design. Rulings and
plans under `docs/superpowers/` are records and stay as written.

- [ ] **Step 5: The package list**

Add `"share/distantlands/game-art.md"` to `requiredFiles` in `build.sbt`,
beside `configuration.md`.

Run: `./sbtw verifyPackageMappings`
Expected: success.

- [ ] **Step 6: Check and commit**

Run: `python3 scripts/check-markdown-links.py`
Expected: no error.

```bash
git add DESIGN.md docs/operations/configuration.md \
  docs/operations/quick-start.md docs/ROADMAP.md \
  docs/catalog/asset-proportions.md build.sbt
git commit -m "docs: describe game art and take L3 off the roadmap

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 9: Gate and browser check

- [ ] **Step 1: Run the full gate after the last commit**

```bash
./sbtw test
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```

- [ ] **Step 2: Check in the browser**

On the scratch server, at 1440×900, 1024×768 and 375×812:

1. The startup line counts every catalog file and warns of none, or
   names what the folder lacks.
2. Symbols: in the inspection overlay's rules text, in a power's cost, in
   a cost badge and in a site's Forge price, each token shows its symbol
   at the glyph's size; a glyph whose file fails keeps its sprite while
   its neighbours keep their art.
3. Tokens in play, warbands in three colours, an Imperial and a bandit
   force, and pawns show their pieces; the black pieces show their light
   edge on the dark table.
4. The Visions Drawn marker stands on its cell; the festival plate and
   the title plate show their pictures in the overlay.
5. A roll shows each face as its crop, square and centred in its chip;
   the doubler shows no "×2" over its crop.
6. Site-power chips: visible on each ready site, each opens its power,
   and no chip covers the card's name band.
7. An empty pile on the ready mat shows the mat's slot and its count.
8. The bounding rectangles of every box from slice 2's list, and of
   every `.token-glyph`, `.pawn-dot`, `.die-face`, `.site-power` and
   `.vision-marker`, are equal with art on and off, at each size.
9. The idle end die is dimmed with art off and hidden with the mat ready
   (the slice 1 review fix), if an Empire game can be reached.
10. What earlier slices could not observe, if reachable: tokens lying on
    an art card, a Replace badge above the art, a Vision in a row and in
    the overlay, a dormant legacy.
11. A decision that offers a player as a target: the pawn on a ready site
    shows its ring, its hover glow and its selected fill over the art,
    and so does a favor bank offered on the ready mat (slice 2 review).
12. No error in the browser console.

- [ ] **Step 3: Stop the scratch server and report**

Report commits, gate numbers, the browser check with screenshots, the
die crops, the measured contrast, deviations, open questions and anything
left undone.

---

## Self-Review

**Spec coverage, slice 3 ("Small art and finish").**

| Spec or ruling | Task |
|---|---|
| Symbols in text: `RulesTextRenderer` and the glyph chips; fallback per glyph; spoken label kept | 2 |
| Pieces and tokens: pawns and warbands by colour, favor, secret, bandits | 3, 4 |
| The titles, the Festival, the Visions Drawn marker | 5 |
| The first player marker | Q9 |
| The black warband's light outline | 4 |
| Dice: the sheet in an overflow-hidden box, offset to the face's crop | 6 |
| Crop rectangles in `asset-proportions.md` | 6 |
| R20: site-power chips, contrast measured | 7 |
| R21: empty piles on a ready mat | 7 |
| `DESIGN.md`; `configuration.md` with `--art-path` and `DLQ_ART_PATH`; the quick start link | 8 |
| L3 deleted, with its answered question | 8 |
| Catalog and `game-art.md` kept equal | 1, 6 |
| Browser check at three sizes | 9 |

**Placeholder scan.** Tasks 1 to 5 describe their tests by name and
assertion and give the code that is new in kind (`attachGlyph`, the
rules); the rest is a call of an existing helper at a named place. Task 6
cannot give the crop numbers before the sheets are measured; its first
step is the measurement, and two tests hold the numbers to the sheets'
bounds and to the page.

**Type consistency.** `ArtCatalog.symbol`, `restriction`, `token`, `pawn`,
`warband`, `title`, `dieFace` return `Option`; `visionsDrawnMarker` and
`festivalMarker` are `ArtKey`; `DieCrop(sheet, x, y, w, h, sheetW,
sheetH)`. `ArtLayer.attachGlyph(svg, key)`, `ArtLayer.attachCrop(box,
crop)`. `RulesTextRenderer.piece(token)` and `warband(forces)` return the
element `glyph` returns.
