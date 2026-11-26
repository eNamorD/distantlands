# Game art implementation rulings

Rulings the owner's orchestrator made while the
[game art design](2026-11-26-game-art-design.md) was implemented, on
questions the design and its plans did not settle. Each entry gives the
question, the decision and the reason. The design's own decisions are not
repeated here.

Plans: [slice 1](../../plans/2026-11/2026-11-26-game-art-slice1.md),
[slice 2](../../plans/2026-11/2026-11-26-game-art-slice2.md),
[slice 3](../../plans/2026-11/2026-11-26-game-art-slice3.md).

## Slice 1

**R1. Denizens with no "(NF)" file.**
Question: the catalog has 255 denizens, and only 33 of them are among the
240 "(NF)" files. The other 222 have only the base file `<Name>.png`,
which the design's folder left out.
Decision: a card's path is `cards/<Name> (NF).png` when the New
Foundations set has that file, else the base file `cards/<Name>.png`. The
catalog holds the explicit name list for each form. `game-art.md` lists
both and says the folder is a copy of the whole `en-US` directory, whose
unused files are ignored. The design's folder section is amended to match.
Reason: with the "(NF)" files alone most of the world deck stays drawn,
which defeats the feature. The owner confirmed it: a file is labelled
"(NF)" because it replaces base game art.
Source of truth: the `bgs-cards` clone's card data
(`content/card-data/oath/en-US`). A card uses the image its entry tagged
"New Foundations" names. The catalog's name lists are checked against
that data, not guessed from file names: all 255 denizens, 48 relics, 60
edifice faces, 36 legacies, 24 sites, the four Visions and the Conspiracy
were checked on 2026-11-26, and no entry names a file outside the two
forms.
Slice/files: slice 1; `ArtCatalog.scala`, `docs/operations/game-art.md`,
the design's "The art folder".

**R2. A card the viewer knows that lies facedown.**
Question: a knowable card shows its back at rest and a summary of its face
on hover, inside the same box; the design did not say which image goes
where.
Decision: the back's image goes on the box, and the face's image on the
hover summary, which is a box of its own with its own fallback.
Reason: the viewer knows the card, so its summary should match every other
face.
Slice/files: slice 1; `CardFace.scala`, `frontend/styles.css`.

**R3. Which printed content a ready box hides.**
Question: the design's rule `.art-ready .printed { opacity: 0 }` reaches
into boxes nested in a ready box, so a nested box whose own image is
missing would lose its drawn content.
Decision: printed content follows its nearest box. `.printed` reads the
custom property `--printed-opacity`; each box kind sets it to 1 and a
ready box sets it to 0. The design's `ArtLayer` text is amended to
describe it.
Reason: the design's selector breaks the box-by-box fallback for nested
boxes, which the design's goal requires.
Slice/files: slice 1; `frontend/styles.css`, the design's "`ArtLayer`".

**R4. `--art-path` naming something that is not a folder.**
Question: the design did not say what the server does then.
Decision: the server starts with no art and logs
`art: <dir> is not a folder; no game art is served`.
Reason: art is optional and must never block a game.
Slice/files: slice 1; `ArtLibrary.scala`.

**R5. An environment variable for the art folder.**
Question: every other option has a `DLQ_*` variable; the design named only
the flag.
Decision: `DLQ_ART_PATH`, read as the other options' variables are. It is
documented with the flag in slice 3.
Reason: consistency with every other option.
Slice/files: slice 1; `ServerConfig.scala`.

**R6. How much of the catalog each slice holds.**
Question: whether slice 1 lists every path the finished feature serves.
Decision: the catalog grows with each slice's surfaces. The test that
`game-art.md` equals `ArtCatalog.allPaths` holds at every slice.
Reason: keys without a consumer are untested shape.
Slice/files: every slice; `ArtCatalog.scala`,
`docs/operations/game-art.md`.

**R7. Six small points of slice 1.**
Question: details the design left open.
Decision: the manifest is served `Cache-Control: no-cache`, and only art
files carry the day's cache. While the manifest has not arrived the switch
is disabled with "Loading the art list…". A disabled switch shows Off,
whatever the stored preference. Opening Settings closes the development
panel and the reverse. The direction a Vision's image is turned in a
denizen box is confirmed by eye in the browser check and must match the
direction the inspection overlay shows a Vision's long edge. A dormant
legacy its viewer knows gets its face's image and keeps the Dormant tag.
Reason: each is the least surprising behaviour and none conflicts with the
design.
Slice/files: slice 1; `ArtRoutes.scala`, `SettingsDialog.scala`,
`GameTableShell.scala`, `CardFace.scala`, `frontend/styles.css`.

## Slice 1 browser check

**R8. Which way a Vision's image is turned in a denizen box.**
Question: the image is landscape and the box portrait, so the image is
turned a quarter; the design did not say which way.
Decision: `rotate(-90deg)`. The card's top edge points left and its text
reads from the foot of the box upward.
Reason: a quarter turn clockwise by the player gives the upright view, as
with a physical card laid sideways.
Slice/files: slice 1; `frontend/styles.css`.

**R9. Where the Settings dialog opens on a phone.**
Question: on a phone the dialog opens at the upper right and covers the
Actions and Log switch while it is open.
Decision: keep it there, as the development panel is.
Reason: one pattern for both panels; it closes with Close or Escape.
Slice/files: slice 1; `frontend/styles.css`.

## Slice 1 review

**R10. Symbolic links in the art folder.**
Question: the scan keeps a catalog path whose file is a symbolic link, so
a served file may lie outside the folder.
Decision: accept, on purpose: a host may link `cards/` to a `bgs-cards`
clone. A test pins that a linked file under a catalog path is listed and
served, and `game-art.md` says so.
Reason: the host owns the folder; whoever can write there can already
place any file under a catalog name.
Slice/files: slice 1 review; `ArtLibrary.scala`,
`docs/operations/game-art.md`.

**R11. `/art` and the seat check.**
Question: the manifest and the images are served to anyone who can reach
the server, with no seat cookie.
Decision: `/art` stays outside the seat check.
Reason: the manifest and the images carry no game state, the page shell
is public too, and a seat check would break the browser's image cache
across seats. The orchestrator flags it to the owner.
Slice/files: slice 1 review; `ServerRoutes.scala`.

**R12. What the stylesheet suite pins.**
Question: `StylesheetArtSuite` asserts the text of declarations, not what
a browser lays out.
Decision: accepted. The layout-invariance tests and the browser check
cover behaviour.
Reason: the test browser has no CSS engine.
Slice/files: slice 1 review; `StylesheetArtSuite.scala`.

## Slice 2

**R13. Foundations, the Oath goal and a board's legacies.**
Question: the design lists "the legacy and foundation strips, and the oath
goal" among the card faces, but on the table a foundation is a one-line
row, the Oath goal a text button and a board's legacy a pill; none is a
card box.
Decision: each gets its image in the inspection overlay, on the plate or
the card face drawn there. The rows, the button and the pills stay words.
The design's surface list is amended to say so.
Reason: the design forbids a layout that changes with the option, and
redrawing the strips as card boxes is the roadmap's separate Table UI
rework.
Slice/files: slice 2; `CardInspectionOverlay.scala`, `SetupCardStrip.scala`,
`FoundationStrip.scala`, `WorldBoardRenderer.scala`, the design's "The
surfaces".

**R14. A facedown legacy's pill.**
Question: whether the L pill on a player board shows the legacy back.
Decision: it stays drawn.
Reason: it is a pill on an abstract board, not a card box.
Slice/files: slice 2; `LegacyStrip.scala` (unchanged).

**R15. A site's power buttons over the image.**
Question: where the buttons go once their printed labels are hidden.
Decision: they stay in place. The browser check reports where each lands
on the site's art. A ruling is needed only if a button covers the card's
name band or cannot be found by hover.
Reason: the buttons are the map's only opener for a site power, and their
place over the art is best judged on the art.
Slice/files: slice 2; `ServerUiSupport.scala` (unchanged).

**R16. Piles on the mat.**
Question: how a deck or a discard pile shows art.
Decision: a pile on the mat shows the back of its top card's kind when
the viewer may know the kind and the pile holds a card. The world deck and
the discards lie on their sides, so their back is turned a quarter, as a
Vision is (R8). The count stays over the back, readable to the contrast
`DESIGN.md` asks. With art off a pile looks exactly as it does today: the
class that marks a sideways pile may be in the DOM but changes nothing
drawn. An empty pile, a pile whose top kind is hidden and a pile drawn off
the mat stay drawn.
Reason: the mat prints each pile's place, and the back is what lies there.
Slice/files: slice 2; `ServerUiSupport.scala`, `WorldBoardRenderer.scala`,
`frontend/styles.css`.

**R17. Chronicle Details' Atlas rows.**
Question: the Atlas rows are drawn by the world's site renderer, inside
the table.
Decision: they show site art.
Reason: it is the same renderer; suppressing art there needs a special
case with no benefit.
Slice/files: slice 2; `WorldBoardRenderer.siteRow`.

**R18. The rectangles placed by eye.**
Question: what to do when the tracker's line, the Visions Drawn label or a
favor bank box covers printed art on the mat.
Decision: move it and report. The rectangle moves for art on and off
alike, with the numbers before and after in `MatLayout`,
`asset-proportions.md` and the report, and the `MatLayout` suite still
passing. A bank's count that sits outside or badly off the mat's circle is
adjusted the same way.
Reason: the roadmap item asks for exactly this check.
Slice/files: slice 2; `MatLayout.scala`, `docs/catalog/asset-proportions.md`.

**R19. The site back and the Oath goal back.**
Question: whether the catalog lists them.
Decision: no. No surface draws either.
Reason: R6.
Slice/files: slice 2; `ArtCatalog.scala`.

## Slice 2 browser check

**R20. A site's power buttons over its art.**
Question: with their printed labels hidden, the buttons were invisible
controls with no cue.
Decision: on a ready site each site-power button shows as a small chip:
its label visible on the name band's ground, in the same position and box
as now, with no layout change; the invariance test covers it. The label
inside a site-power button therefore stops following `--printed-opacity`
on a ready site, and the design's site card bullet is amended. The chip's
contrast is measured against `DESIGN.md`, and so are the `#1a1610d9`
grounds slice 2 added.
Reason: an invisible control with no cue fails `DESIGN.md` and the owner's
rule that players must see what they can act on; the button is a control,
not print (R9 of the real proportions design).
Slice/files: slice 3; `frontend/styles.css`, the design's "The surfaces".

**R21. An empty pile on a ready mat.**
Question: an empty pile drew its empty box over the slot the mat prints.
Decision: an empty pile on a ready mat drops its drawn fill so the mat's
printed slot shows. With the mat not ready it is unchanged.
Reason: the mat prints the slot; a drawn box over it is duplicate print.
Slice/files: slice 3; `frontend/styles.css`.
