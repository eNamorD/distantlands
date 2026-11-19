# Chronicle Details

**Status:** designed 2026-11-18. Slices 1 (projection) and 2 (aids and
strip) have landed.

**Builds on** the [Chronicle browsing design](../2026-10/2026-10-28-chronicle-browsing-design.md)
(its scoresheet and Chronicle projection), the
[Foundations design](2026-11-11-foundations-design.md) (its strip) and the
[Legacies design](2026-11-13-legacies-design.md) with its
[implementation rulings](2026-11-13-legacies-implementation-rulings.md)
(R36 on who sees a dormant legacy).

## Goal and scope

During a game, the table shows little of the Chronicle it was started from.
The Shared Bank holds the six Foundations, and the projection carries only
the Chronicle's name and starting digest. The era scorepad, the legacies of
lineages nobody seated, and the Atlas are visible nowhere until the
Chronicle Phase ends, and then only on the Chronicle page.

This design adds a wide rectangular **Chronicle Details** button beside the
Shared Bank. It opens an overlay that gathers the Chronicle's meta
information, top to bottom:

1. The Chronicle's name, as the heading.
2. The Foundations, moved here from the Shared Bank.
3. The era scorepad, with every lineage's legacies in its row, seated or
   not.
4. The Atlas: its Recent site, drawn as the world map would draw it.
5. The Chronicle Phase aids: the five Chronicle task cards, in the
   Foundations strip's format.

Out of scope: the Atlas's Forgotten end; choosing or acting on anything in
the overlay (it is read-only); editing the Chronicle; more scorepad columns
than the Chronicle page shows.

| Slice | Seam | Delivers |
|---|---|---|
| 1 | Projection | `chronicleDetails` on every seat's snapshot |
| 2 | Aids and strip | Chronicle task text; `SetupCardStrip` |
| 3 | Site drawing | `siteRow` extracted from `world()`, no behaviour change |
| 4 | Overlay | Button, overlay, scorepad chips, Atlas, aids, CSS |

Each slice ships with its gates green and merges on its own.

## Rulings

**D1. Who sees a dormant legacy in the overlay.**
Decision: R36 holds unchanged. A dormant legacy shows its face to its
lineage's player, with a "Dormant" tag; every other viewer sees an "L" back,
except during the Chronicle Phase, where every viewer sees every face (R18).
A lineage nobody seated has no owner at the table, so outside the Chronicle
Phase its dormant legacies are backs to every viewer. Active legacies are
faceup for everyone.
Reason: the owner's ruling, 2026-11-18. The overlay shows the same cards the
player boards show, under the same rule.

**D2. Where legacies go.**
Decision: as chips in each lineage's scorepad row, not in a section of their
own.
Reason: the owner's ruling, 2026-11-18. One row per lineage keeps the
overlay compact, and a chip opens the whole card on a click.

**D3. Where the data comes from.**
Decision: the server projects it on every snapshot, as a new
`GameProjection` field.
Reason: only the host's browser keeps the starting Chronicle, and its copy
goes stale once an Explore takes the Recent site or the Chronicle Phase
scores lineage points. The projector already applies every other visibility
rule, R36 included. A separate request on open would need an endpoint and a
loading state for a small payload, and would go stale while open.

**D4. Where the task card text lives.**
Decision: in a static `ChronicleTaskPresentation` in
`shared/src/main/scala/distantlands/protocol/projection`, beside
`OathkeeperPresentation`, not in the projection.
Reason: the text is the same in every game.

## Slice 1: projection

New types in `shared/src/main/scala/distantlands/protocol/projection`:

```scala
/** The Chronicle as the table sees it mid-game (Chronicle Details design). */
final case class ChronicleDetailsProjection(
    eraTarget: Int,
    /** A column per game played, in order, as ChronicleSummary keeps it. */
    scorepad: Vector[Vector[ChronicleScorepadEntry]],
    /** Every lineage in the Chronicle, seated or not, in PlayerColor order. */
    lineages: Vector[LineageDetailsProjection],
    /** The Atlas's Recent site; None once the Atlas holds no site. */
    atlasRecent: Option[SetupSiteProjection])

/** One lineage: its current era total, its Imperial role, and its legacies
  * in held order as the viewer may see them (ruling D1). */
final case class LineageDetailsProjection(lineage: String, score: Int,
    role: Option[String], legacies: Vector[CardDetailsProjection])
```

`GameProjection` gains `chronicleDetails: Option[ChronicleDetailsProjection]`.
On the wire the field is required, with no default: old journals need not
stay decodable. `GameProjectionCodec` encodes and decodes both types.

The server builds the field in `application/GameProjection.scala`, beside
`chronicleView`, from `context.ready.game.campaign`. It is present whenever
the game has a campaign.

- **Scorepad and scores** come from `campaign.era`. `eraTarget` is
  `era.targetScore`. Each `EraGame` becomes a column of
  `ChronicleScorepadEntry` (lineage key, total, role as `"chancellor"`,
  `"citizen"` or absent). A lineage's `score` is `era.score(lineage)`, which
  covers carried TTS scores. The era is live: Score Lineage Points adds this
  game's column during the Chronicle Phase.
- **Role** is the lineage's current role in `campaign.lineages`.
- **Legacies** come from `GamePresentationProjector`'s existing
  `legacies(ready, viewer, lineage)`, made reachable from `GameProjection`.
  It already hides by R36 and reveals in the Chronicle Phase, so no new
  visibility rule is written.
- **Atlas Recent site** is `campaign.atlas.site(AtlasEnd.Recent)`, mapped to
  a `SetupSiteProjection` through the same path `siteProjection` uses for a
  map site. Stored denizens and edifices are faceup and named. Stored relics
  are facedown: `hiddenCard("relic")`, as on a map site. A stored site has
  no loose favor, secrets or pawns, so those counts are zero.

Tests:

- Projector: a dormant legacy is its face to its lineage's player and an
  "L" back to another seat; an unseated lineage's dormant legacy is a back
  to every seat; every face shows in the Chronicle Phase; an active legacy
  is faceup to all.
- Projector: Score Lineage Points adds a scorepad column; the Recent site
  changes after an Explore takes it; `atlasRecent` is `None` once the Atlas
  is empty; a stored relic is hidden.
- Wire: both types round-trip; a snapshot without `chronicleDetails` is
  refused.

## Slice 2: aids and strip

`ChronicleTaskPresentation` holds the five Chronicle task cards, in the
order the Chronicle Phase uses them: The Sun, The Throne, The World, The
Beacon, The Stars. Each card has a title and one or more faces. A face has
a label and its lines. The Sun and The Throne each have a front and a back;
the others have one face. The text is transcribed from the local card data,
`~/projects/bgs-cards/content/card-data/oath/en-US/oath-newfoundations.yml`
("Chronicle - The Sun (NF)" and its siblings).

`FoundationStrip` is generalised into `SetupCardStrip`. A strip takes a
heading and a vector of cards. Each card has an optional mark (the
foundation's numeral; none for a task card), a name, an optional tag
("Altered"), an inspector title and the lines its click opens.
`FoundationStrip.ofGame` builds the foundation cards for it.
`ChronicleTaskPresentation` builds the aid cards; a two-sided card's lines
are its front's, a "Back" line, then its back's.

Tests: a test pins the five cards, their order, and which have two faces.
The existing foundation strip tests pass unchanged.

## Slice 3: site drawing

`WorldBoardRenderer.world()` draws each site inline, placed by `MatLayout`
in the mat's percentages. The per-site body is extracted into
`siteRow(site, column, row, rowRect, options, links, pawns)`, which returns
the `article.site` element with its face, heading, pawns, festival marker
and cards placed relative to `rowRect`. `world()` calls it for each site and
behaves as before.

Tests: `MatBoardSuite` and the other world-map suites pass unchanged. A new
test renders one `siteRow` alone and checks that its card and relic slots
sit where `MatLayout` places them in the row.

## Slice 4: button and overlay

### Button

`tableEdge` draws `button.chronicle-details-button`, reading "Chronicle
Details", beside the Shared Bank section, only when `chronicleDetails` is
present. `sharedBank` no longer draws the Foundations strip. The World pane
is rebuilt every snapshot and cannot own the overlay, so the click goes
through a small `ChronicleDetails.open(origin)` hook, on the pattern of
`CardInspection.onOpen`, which `GameTableShell` registers.

### Overlay shell

`ChronicleDetailsOverlay(mount)` lives in `GameTableShell`, on the
`GameLogOverlay` pattern: an `aside` with `role="dialog"` and
`aria-modal="true"`, a scrim, a pane-fill panel, and a header with the
heading and Close.

- Escape, Close or a click on the scrim closes it.
- On open, focus moves to the content. On close, it returns to the opener
  while the opener is still in the document; otherwise to the rebuilt
  Chronicle Details button, found by its class.
- `show(projection)` runs on every snapshot, so an open overlay stays
  current. A redraw keeps the content's scroll position.
- While it is open, the table behind is inert, as with the log overlay.

Cards in the overlay open the existing `CardInspectionOverlay`, which stacks
above this overlay one z-index layer higher. Both overlays listen for Escape
on the document, so Chronicle Details ignores Escape while the inspector is
open: Escape closes the inspector first, then the overlay.

The overlay is read-only. Its cards are drawn with a `BoardOptions` that
offers nothing, so a parked decision never marks a card inside it. Choices
over legacies stay on the player boards and the Public Ambitions row.

### Content

1. **Header.** The heading is `chronicle.name`, and it labels the dialog.
   Under it, a quiet line: "Chronicle Details · Era target N", N being `eraTarget`.
2. **Foundations.** `SetupCardStrip` with the foundation cards, as the
   Shared Bank drew them.
3. **Era scorepad.** `ChronicleScoresheet` gains `of(details:
   ChronicleDetailsProjection)`. Its lineage cell holds the colour swatch,
   the lineage's name and a row of legacy chips from `LegacyStrip.chip`,
   made `private[frontend]`. A click on a chip opens the card. The Chronicle
   page keeps its text list of legacies. The columns are the last five games,
   with the "…" column when earlier ones exist. With no games played, the
   table has its lineage rows and totals only.
4. **Atlas.** Heading "Atlas · Recent". The site is a `siteRow` inside a box
   with the mat site row's aspect ratio, so the face, card slots and relic
   slots sit as on the map. It has no pawns, no festival marker and no
   site-power links: its powers can be read in the inspector but not used.
   An empty Atlas reads "The Atlas holds no sites."
5. **Chronicle Phase aids.** `SetupCardStrip` with the five task cards,
   headed "Chronicle Phase".

### Styles

The overlay panel fills the pane and scrolls inside itself; its sections
stack in one column. The button uses the Shared Bank's tokens and is full
width at phone widths. Both themes are covered by the existing tokens.

### Tests

- The button shows only when `chronicleDetails` is present; the Shared Bank
  no longer holds the Foundations strip.
- The overlay draws its five sections in order; the empty Atlas and the
  no-games scorepad draw their fallbacks.
- Escape with the inspector open closes only the inspector; a second Escape
  closes the overlay, and focus returns to the rebuilt button.
- A snapshot redraw keeps the overlay's scroll position.

Browser check: on a scratch DB server run through Bash on port 8081 (not
`preview_start`, which opens the live DB), open the overlay mid-game and in
the Chronicle Phase, and take screenshots at desktop and phone widths in
light and dark themes.
