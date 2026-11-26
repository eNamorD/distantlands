# Game art implementation rulings

Rulings the owner's orchestrator made while the
[game art design](2026-11-26-game-art-design.md) was implemented, on
questions the design and its plans did not settle. Each entry gives the
question, the decision and the reason. The design's own decisions are not
repeated here.

Plans: [slice 1](../../plans/2026-11/2026-11-26-game-art-slice1.md).

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
