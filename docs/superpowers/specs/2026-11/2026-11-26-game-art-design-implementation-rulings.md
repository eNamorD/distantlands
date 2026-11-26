# Game art implementation rulings

Rulings the owner's orchestrator made while the
[game art design](2026-11-26-game-art-design.md) was implemented, on
questions the design and its plans did not settle. Each entry gives the
question, the decision and the reason. The design's own decisions are not
repeated here.

Plans: [slice 1](../../plans/2026-11/2026-11-26-game-art-slice1.md),
[slice 2](../../plans/2026-11/2026-11-26-game-art-slice2.md),
[slice 3](../../plans/2026-11/2026-11-26-game-art-slice3.md),
[slice 4](../../plans/2026-11/2026-11-26-game-art-slice4.md).

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
Amended by R37: the Oath goal is a card in the shared bank. Foundations
and a board's legacies keep this ruling.
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
Replaced by R45: the owner asked for no power words over a site's art.
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

## Slice 3

**R22. How a glyph holds its image.**
Question: the design says each glyph becomes a span holding the printed
character and a 1em image; a glyph today is one `svg.token-glyph`.
Decision: keep `svg.token-glyph` and add an SVG `<image>` inside it,
hiding the sprite's `<use>` on load, with the same load, error and
remembered behaviour as `ArtLayer`, per glyph. The design's wording is
amended.
Reason: no DOM change with art off, and some two hundred selectors stay
valid.
Slice/files: slice 3; `ArtLayer.scala`, `RulesTextRenderer.scala`.

**R23. Favor and secrets: the symbol and the token.**
Question: the folder has the printed symbols and pictures of the tokens.
Decision: the printed symbol where a card prints it (rules text, a
power's cost, a Forge price, a cost badge); the token's picture where
tokens lie in play (on a card, a site, a bank, a banner).
Reason: that is what each is on the physical table.
Slice/files: slice 3; `RulesTextRenderer.scala` and its callers.

**R24. Which die is which symbol.**
Question: three die symbols, three die tokens.
Decision: `[defense-die]` is `symbol-diceb.png` and `[attack-die]` is
`symbol-dicer.png`, as the card data shows (Wrestlers, Bear Traps).
`[round-die]` is `symbol-dicep.png`: the card data uses it where the end
of the game is rolled for (Squalid District, "Whenever the die is rolled
to end the game"; the ruined site setup rules and Stone Portal, where our
text has `[round-die]`).
Reason: evidence from the card data, not elimination.
Slice/files: slice 3; `ArtCatalog.scala`.

**R25. The die chip.**
Question: a chip is not square and grows with its glyphs.
Decision: the face's square crop is centred in the chip and the chip
keeps its size; its glyphs hide when the crop is ready. A doubled face
must still read as doubled: if the sheet's face does not print the "×2",
the chip's "×2" stays visible.
Reason: the chip is the roll's place in the text; the crop is the face.
Slice/files: slice 3; `DieFaceChip.scala`, `frontend/styles.css`.

**R26. Pieces by colour.**
Question: which piece each force and pawn shows.
Decision: a player's colour for pawns and warbands; Imperial warbands
purple; bandits `tts/tokens/bandit.png`. The warband the Supply track
prints under each band stays drawn.
Reason: the pieces' own colours; the Supply track is print on an abstract
board.
Slice/files: slice 3; `ArtCatalog.scala`, the renderers.

**R27. The light outline.**
Question: the design names the black warband.
Decision: the black pawn gets the light outline too.
Reason: the same visibility problem on the dark table.
Slice/files: slice 3; `frontend/styles.css`.

**R28. The title's plate.**
Question: the overlay's plate is 1016 : 700 and the title files are
602×827.
Decision: keep the plate's ratio. The title's image is fitted with
`object-fit: contain`, turned a quarter only if the file is plainly the
landscape card stored on its side. No plate ratio changes in this work.
Reason: the plate's shape is the real proportions design's.
Slice/files: slice 3; `WorldBoardRenderer.scala`, `frontend/styles.css`.

**R29. The festival marker.**
Question: the map's band is about 11 : 1 and the art 2.9 : 1.
Decision: the image goes on the overlay's printed plate only; the map's
band stays drawn.
Reason: the art would be a sliver in the band.
Slice/files: slice 3; `FestivalMarker.scala`.

**R30. The first player marker.**
Decision: it stays words; no file is listed.
Reason: it is a pill on an abstract board (R14).
Slice/files: none.

**R31. The Visions Drawn marker and the round marker.**
Decision: the Visions Drawn marker takes its picture on its cell. The
current round's wedge stays drawn; the owner said the round marker needs
no art.
Slice/files: slice 3; `WorldBoardRenderer.scala`.

**R32. Restriction marks.**
Decision: the adviser-only and site-only banners take the two
restriction symbols; an edifice's towers stay drawn.
Reason: the folder has no tower symbol.
Slice/files: slice 3; `PowerMarks.scala`.

**R33. Files with no surface, and the Usurper's marker.**
Question: which files the catalog leaves out.
Decision: `symbol-chancellor.png`, `symbol-x2.png`, the suit tiles,
`festival-back.jpg`, `first-player.jpg` and the round die's texture are
not listed. The Usurper's limiter is a piece in play: the round wheel
draws it as a mark on the limiter's round, so it takes
`tts/tokens/usurper.png` there. The "Usurper locked" line under the wheel
is words and stays.
Reason: R6; a piece in play is drawn as its piece.
Slice/files: slice 3; `ArtCatalog.scala`, `WorldBoardRenderer.scala`.

## Slice 3 browser check

**R34. The ground under forces, loose tokens and pawns.**
Rewritten by R51 for pawns and warbands; it stands for loose tokens.
Question: on the name band's translucent fill the black player's colour
measured 4.17 : 1 over white art.
Decision: forces, loose tokens and pawns on a site's art stand on opaque
Base.
Reason: measured contrast; black fails on the translucent ground.
Slice/files: slice 3; `frontend/styles.css`.

**R35. The pawns' ground.**
Rewritten by R51: a pawn's picture has no ground, its name has a chip.
Question: the ground was the pawns' whole row, a dark strip across the
site's art for one pawn.
Decision: the ground hugs the pawns. It lies on each pawn that is not an
option target; a pawn a decision offers keeps its own background and
shadow for its option states. Box sizes do not change.
Reason: a strip hides art for nothing.
Slice/files: slice 3; `frontend/styles.css`.

## Whole-feature review

**R36. What the layout-invariance tests cover.**
Question: the invariance tests compare markup with art attached but never
dispatch `load`, so they do not compare the ready state.
Decision: they need not. The ready state's layout is covered by the
stylesheet pins (R12) and by the browser check's rectangle comparison.
Reason: the test browser has no layout engine.
Slice/files: `CardArtSuite`, `WorldArtSuite`, `PieceArtSuite`,
`DieArtSuite`.

## Slice 4: the owner's review of the running table

**R37. The Oath goal in the shared bank.**
Question: the owner asked for "the Oathkeeper image" in place of the
words "Oathkeeper of Supremacy"; the folder has the Oath goal card and the
Oathkeeper title.
Decision: the Oath goal card. It is a card box at the goal's ratio and
the banners' height, before the banners, and still the button that opens
the goal's overlay. The holder is a line of words under it. With art off
the box holds the drawn goal face at the same size. This amends R13 for
the Oath goal; foundations and a board's legacies keep R13.
Reason: the section names the goal, and the title's art already has its
plate.
Slice/files: slice 4; `WorldBoardRenderer.sharedBank`,
`frontend/styles.css`.

**R38. A site card and the frame the mat prints.**
Question: the mat's frame line showed around a site's card.
Decision: the site's box is the frame's rectangle grown by the line's
measured half-thickness on every side, and the image fills it with
`object-fit: cover`. The site's row and its neighbours do not move. The
drawn face takes the same box.
Reason: a one percent trim of the picture is invisible; scaling the card
whole would make sites taller than denizens.
Slice/files: slice 4; `MatLayout.siteCard`,
`docs/catalog/asset-proportions.md`.

**R39. The Usurper's token on the wheel.**
Question: the token was far smaller than the real one.
Decision: the token fills its wedge, its point to the hub. The drawn
fallback takes the same wedge shape. The round marker and the current
wedge's border lie over it.
Reason: that is the real token's size and place.
Slice/files: slice 4; `WorldBoardRenderer.roundTracker`, `ArtLayer`.

**R40. The round marker.**
Question: how the current round is shown.
Decision: a silver disc on the current round's wedge, for the game's
round marker, with art on and off. It takes a new colour,
`--round-marker`, recorded in `DESIGN.md`. Over a ready mat the current
wedge drops its brass fill and keeps its border and the disc; with art
off the fill stays.
Reason: the brass fill hid the mat's own wedge.
Slice/files: slice 4; `WorldBoardRenderer.roundTracker`,
`frontend/styles.css`, `DESIGN.md`.

**R41. Labels on the mat.**
Question: the tracker's lines could not be read over the mat's art.
Decision: over a ready mat each line is a chip on opaque Base. With art
off the lines are unchanged.
Reason: the drawn mat already gives them contrast; the complaint was
about the art.
Slice/files: slice 4; `frontend/styles.css`.

**R42. Favor banks.**
Question: a bank's favor and count were too small to read.
Decision: the favor token fills the mat's circle, the count stands
centred over it on a small Base disc, as large as the disc allows, and
the suit's glyph stands on the mat's emblem. The drawn fallback has the
same three places.
Reason: legibility at Fit is the point of the request.
Slice/files: slice 4; `MatLayout`, `WorldBoardRenderer.favorBanks`,
`frontend/styles.css`.

**R43. A banner's overlay.**
Decision: the overlay shows the banner's own face at the overlay's size,
left of its rules, as a card's overlay shows the card.
Slice/files: slice 4; `BannerFace`, `CardInspection`,
`CardInspectionOverlay`.

**R44. Empty card slots on the mat.**
Decision: over a ready mat an empty slot's dashed outline is Bright Cream
with a dark line under it; a slot a decision offers keeps its ring. If
one pixel is too thin at a phone's Fit, the border's width follows the
map's scale, with art on and off alike.
Slice/files: slice 4; `frontend/styles.css`.

**R45. Site powers between slices 4 and 5.**
Question: R20's chips are removed (the owner's item 5) before slice 5
builds the click targets on the site's printed power symbols.
Decision: accepted for the time between the two slices; the buttons stay
focusable with their hover notes. If slice 5's click targets are deferred
to the roadmap, this is reopened before the branch is done. R20 is
replaced.
Slice/files: slice 4; `frontend/styles.css`.

## Slice 4 review

**R46. The limiter's round as the current round, drawn.**
Question: with art off, the current round's brass fill covers the drawn
limiter's wedge when both are one round.
Decision: accepted, no change. The server clears the limiter at round 4,
so the two never coincide in play.
Reason: an unreachable state needs no drawing.
Slice/files: slice 4 review; none.

**R47. Tests that read `MatLayout.siteCard`.**
Question: `MatBoardSuite` and `SiteRowSuite` compare a face's box with
`MatLayout.siteCard` instead of literals.
Decision: allowed. `MatLayoutSuite` pins the rectangle in literals, so
the other suites may read it.
Slice/files: slice 4 review; the three suites.

## Slice 5: pieces and powers on a site

**R48. The pawn row.**
Decision: six fixed pawn boxes, filled from the left. One pawn has the
size it has among six; a pawn's place and size do not change as others
come and go.
Slice/files: slice 5; `MatLayout`, `SitePieces`, `frontend/styles.css`.

**R49. Warband groups.**
Decision: built for one group, since the projection carries one force a
site. The row stays a row, so a second group would need no layout
change.
Slice/files: slice 5; `SitePieces`.

**R50. Names under pawns.**
Decision: a pawn's name stands under it at its share of the site at
every zoom, and is hidden from sight on the tiny map. It stays the
pawn's accessible name and hover note everywhere. The pawn's colour
carries identity when zoomed out.
Slice/files: slice 5; `frontend/styles.css`.

**R51. Grounds under pieces.**
Decision: no ground under a piece's picture; the black pieces keep R27's
light edge. Over art only, a pawn's name and a warband's count stand on
a Base chip. An offered pawn's ring goes around its piece and name.
Loose tokens keep R34. This rewrites R34 and R35 for pawns and warbands.
Slice/files: slice 5; `frontend/styles.css`.

**R52. The pieces' size against the site.**
Question: a fixed ratio makes pieces smaller than today's on a
zoomed-out map.
Decision: the ratio holds on every map size, a phone's included, with no
floor.
Reason: the owner asked for it in words: "zooming in and out should
maintain the pawns' and warbands' size relative to the site".
Confidence: low on the phone's result; the owner judges it from the
slice's phone screenshot.
Slice/files: slice 5; `frontend/styles.css`, `DESIGN.md`.

**R53. The drawn power shield.**
Decision: a plain rounded outline with the powers' labels inside, cut
with an ellipsis in a half; the full label stays in the accessible name
and the hover note.
Slice/files: slice 5; `SitePieces`, `frontend/styles.css`.

**R54. Small power targets.**
Decision: targets under 24px are accepted when zoomed out. The hit area
is the print's region and no larger, so it never takes a click from the
pieces beside it. A zoom and Chronicle Details are the other ways.
Slice/files: slice 5; `frontend/styles.css`.

**R55. Two powers on one shield.**
Decision: the islands print Coast above and Island below; the other
pairs stand left and right in the order Enduring, Mountain, River,
Coast. Confirmed on the images of all five two-power cards (Ancient
City, Headwaters, Tidal Marshes, Fair Isle, Sunken Isles): none
disagrees, so the table has no exception.
Slice/files: slice 5; `MatLayout.sitePowerRegions`.

With slice 5's click targets on the shield, R45's gap is closed.
