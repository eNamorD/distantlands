# The Last Chronicle Powers

**Status:** designed 2026-11-16, approved section by section by the owner.
Each slice is planned when the one before it lands. Slice 1 landed 2026-11-16.

**Builds on** the [Legacy Chronicle powers design](2026-11-13-legacy-chronicle-powers-design.md)
(the Throne's windows, Reformer at the start of the Throne, Heirloom's keep,
Rival's protection in Shape Empire, Iron Hand's Shape Empire) and
[catalog batch 13](2026-11-14-catalog-batch-13-design.md) (the Baths, which
register inert powers that a Chronicle step reads). The batch 4 rulings that
apply to a whole batch, and the cost and log-line rules of
[batch 8](../2026-10/2026-10-12-catalog-batch-8-design.md), apply unchanged.

## Goal and scope

Four printed powers still do nothing, and `src/test/resources/powers/unimplemented.txt`
lists only them:

| Power | Card | Printed text |
|---|---|---|
| `site.ancient-city.enduring` | Ancient City | Enduring: cards at this site are not discarded in the Chronicle Phase during the Shape Empire step. |
| `edifice.e16.ruined` | Hall of Bandits | CHRONICLE: Ruined edifices ruled by bandits are not discarded. |
| `edifice.e26.ruined` | Hiding Place | CHRONICLE (THRONE): This site's ruler may secretly keep a relic they have facedown. They start the next game with it. |
| `edifice.e07.ruined` | Blasted Pastures | CHRONICLE (START OF THRONE): Bury all Nomad cards on the map unless the winner rules this card. |

This design implements them, then retires the machinery that sorts,
filters and marks unimplemented cards, which an empty snapshot leaves
with nothing to do. It clears the ROADMAP's **The last Chronicle powers**
item and its deferred **Retire the unimplemented-card machinery across the
stack** item.

| Slice | Content | Seam |
|---|---|---|
| 1. Shape Empire protections | Ancient City, Hall of Bandits | `ShapeEmpire.discards` |
| 2. Start of Throne secrecy | Relics hidden at the start of the Throne; Hiding Place | `ChronicleStartOfThrone`; Heirloom's keep |
| 3. Blasted Pastures | Blasted Pastures | `ChronicleStartOfThrone`; `Bury.standard` |
| 4. Retire the machinery | The implemented-first deal, the `implemented` flag, the frontend badge, the snapshot, the holding rule, `PowerImplementationStatus` | Across the stack |

## Rulings

Taken with the owner on 2026-11-16. They open a "Last Chronicle powers
rulings" section in `docs/rules/ambiguities.md`, numbered as here.

1. **Ancient City is Enduring.** No card at its site is discarded in Shape
   Empire, whoever performs the step, Iron Hand's Exile included. The
   protection is Shape Empire's only: Revolutionary's discard comes after
   Build Edifice and still discards cards at Ancient City. The behavior is
   today's; only its source changes, from a handler-name suffix to a
   registered power.
2. **Hall of Bandits.** While its ruined face is on the map, a ruined
   edifice at a site bandits rule is not discarded in the Chronicle. Shape
   Empire is the only Chronicle discard that reaches a bandit site
   (Revolutionary discards at Imperial sites only). Hall of Bandits keeps
   itself when bandits rule its site. A bury is not a discard, so Blasted
   Pastures still buries a Nomad ruin at a bandit site. It writes no line,
   as Enduring writes none.
3. **Relics are hidden at the start of the Throne.** The step that turns
   every relic players hold but the Grand Scepter facedown, and clears every
   viewer's record of it, moves from the start of Stock Reliquary to the
   start of the Throne, before any power of that window. Each holder still
   sees their own relics. Warbands on cards still return to their supply at
   Stock Reliquary. So Hiding Place's keep is secret.
4. **Hiding Place.** At the start of the Throne, after the relics are
   hidden and before Shape Empire can discard it:
   - Its ruler is the player who rules its site, read by
     `SiteRule.rulingPlayer`: a player's warbands, or the Chancellor for an
     Imperial site. The Grand Mask does not count, since it is nobody's turn
     in the Chronicle. When bandits rule the site, or nobody does, nothing
     is asked.
   - "A relic they have facedown" names the result, not a condition: the
     ruler may keep any relic they hold but the Grand Scepter, which then
     stays facedown with them for the next game. They choose one or
     Decline, even with a single relic, since the text says "may".
   - The relic's secrets and any warbands on it return to the ruler, then
     `StartingRelicKept` adds it to their lineage's starting relics, as
     Heirloom's keep does.
   - Lines: "p2 kept a relic with Hiding Place" for every viewer, and
     "p2 kept Cup of Plenty with Hiding Place" for the ruler alone.
5. **Blasted Pastures.** At the start of the Throne, unless the Throne
   holder rules its site, it buries every Nomad card at a site: denizens and
   edifices of either face, locked cards and Blasted Pastures itself.
   - "Rules" is Shape Empire's reading (`ChronicleTasks.rules`), so after
     Crown Chancellor the Empire's sites count as the winner's (Chronicle
     rulings 11 and 14).
   - Denizens go to the bottom of the world deck facedown and edifices to
     the edifice deck, through `Bury.standard`; tokens on them return as a
     bury returns them.
   - Advisers and relics are not on the map and stay. Rival keeps cards from
     a discard, not a bury, so it is not asked.
   - One line: "Blasted Pastures buried {cards}". Up to five cards are
     named inline; more are the clickable "6 cards" that opens the card list
     (catalog batch 2, N5). With no Nomad card on the map, nothing is
     buried and no line is written.
6. **Start of Throne order.** The relics are hidden, then Reformer acts,
   then Hiding Place, then Blasted Pastures. Reformer moves cards only
   among sites the winner rules, so it cannot change who rules Hiding Place
   or Blasted Pastures, and the order changes no outcome.

## Approach

Each power is a registered `ContributingPower`, as earlier Chronicle cards
are. Inert rules that a step reads hook no window, as the Baths do; steps
the card adds hook the window it names, as Reformer and Heirloom do. A
generic "keep from Shape Empire" hook was considered and rejected: Rival's
question already lives inside the discard's `Branch`, and two inert rules
do not justify rebuilding a working step.

The card objects stay where they are, with their intact faces' powers:
`WildPasturesCard` and `HallowedSpringCard` in `gameplay/powers/economy`,
`HallOfMinistersCard` in `gameplay/operations`. The new ruined-face powers
live in `gameplay/chronicle`, beside the steps that read or run them.

### Slice 1: Shape Empire protections

- `AncientCityEnduring` and `HallOfBandits`: case objects with no
  contributions, registered in `WalkerPowerCatalog` beside the Baths.
- `ShapeEmpire.discards` drops the `.endsWith(".enduring")` suffix check.
  A site is exempt when its catalog handlers include
  `AncientCityEnduring.id`. A ruined edifice is exempt when bandits rule its
  site and E16 shows its ruined face somewhere on the map.
- Iron Hand's Shape Empire uses the same `discards`, so it inherits both.
  Revolutionary reads `ShapeEmpire.discardOf`, not the exemptions, and is
  unchanged.

### Slice 2: Start of Throne secrecy and Hiding Place

- `ReliquaryAndRelics.hidden` moves out of `stock` and becomes the base step
  of `ChronicleStartOfThrone` in `ChronicleThroneProcedure`. `stock` keeps
  the return of warbands on cards and the stocking itself.
- Heirloom's keep (the secrets back to the holder, `StartingRelicKept`, the
  public and private lines) is extracted into a shared keep helper that
  Heirloom and Hiding Place both call, each naming its card in the lines.
  The helper also returns warbands on the kept relic, which Heirloom never
  meets because Stock Reliquary has returned them by its step.
- `HidingPlace` contributes a `Transform` to `ChronicleStartOfThrone`,
  registered after Reformer. Its `Branch` reads the site of E26's ruined
  face and its ruling player when the step is reached; asks ChooseOne over
  that player's relics but the Grand Scepter, with Decline; then keeps the
  chosen relic. The decision id names the ruler.
- No new operation and no wire change.

### Slice 3: Blasted Pastures

- `BlastedPastures` contributes a `Transform` to `ChronicleStartOfThrone`,
  registered after Hiding Place. Its `Branch` reads the board when the step
  is reached: the site of E07's ruined face, and whether the Throne holder
  rules it. Unless they do, one required batch buries every Nomad card at
  every site with `Bury.standard`, then writes the line with
  `NoteArg.Cards`.
- No new operation.

### Slice 4: retire the machinery

Remove, with their tests:

- The implemented-first deal: `ShufflePolicy.implementedFirst`,
  `ImplementedCardCatalog`, the implemented filler in
  `FirstGameChronicleGenerator`'s pools, and the unimplemented denizens
  `SetupDeal` keeps off the discard tops.
- `implemented` on `CardDetailsProjection` and its codec in
  `WorldProjectionCodec`.
- The frontend's unimplemented class, accessible-name suffix and badge in
  `CardFace` and `CardInspectionOverlay`.
- `UnimplementedPowersSuite`, `src/test/resources/powers/unimplemented.txt`,
  and `CardCatalogSuite`'s holding rule.
- `PowerImplementationStatus` itself (owner, 2026-11-16): every printed
  power is accounted for, so no check of it remains.

The plan re-inventories every file this list names at the HEAD it starts
from before editing.

## Hidden information

- From the start of the Throne, every relic players hold but the Grand
  Scepter is facedown, and no viewer but its holder knows it (ruling 3).
- Hiding Place's choice is the ruler's alone: the decision is theirs, and
  only the private line names the relic.
- Blasted Pastures buries faceup cards from the map, so every viewer may
  identify them; the card list shows their faces.

## Lines

- "p2 kept a relic with Hiding Place" (every viewer)
- "p2 kept Cup of Plenty with Hiding Place" (the ruler)
- "Blasted Pastures buried Wild Horses and Hunting Party"
- "Blasted Pastures buried 6 cards" (a card list)

Hall of Bandits and Ancient City write none.

## Testing

Scenario tables in the style of `RuinedTempleSuite`, one suite per slice:

- **Slice 1.** Ancient City keeps its cards in Shape Empire, also under
  Iron Hand; Revolutionary still discards there. Hall of Bandits keeps
  ruined edifices at bandit sites, itself included, and keeps none once it
  is off the map or intact; denizens and intact edifices at bandit sites
  are handled as before.
- **Slice 2.** Relics are hidden before Reformer's question. The ruler
  keeps a relic or Declines; a single relic still asks; the Chancellor acts
  for an Imperial site; a bandit or unruled site asks nobody; the Grand
  Scepter is never offered. Secrets and warbands on the kept relic return.
  Both lines have the right readers. The relic is in `startingRelics` and
  the next game hands it back facedown. Heirloom's suite stays green on
  the shared helper.
- **Slice 3.** A Throne holder who rules Blasted Pastures prevents the
  bury; after Crown Chancellor, the Empire's sites count as theirs. The
  bury takes Nomad denizens and both faces of Nomad edifices, locked cards
  and itself, and leaves advisers and other suits. The line lists the
  cards, inline and as a card list. No Nomad card writes no line.
- **Slice 4.** The full gates. The removed suites are gone, and the
  frontend shows no unimplemented badge.

Every slice: wire and replay round trips for each new decision and line,
the declared-keys snapshot rewrite, and the removal of the slice's line
from `unimplemented.txt` until slice 4 deletes the file.

## Docs

- `docs/rules/ambiguities.md`: the "Last Chronicle powers rulings" section,
  rulings 1 to 6, each added by its slice.
- `docs/rules/chronicle.md`: Digital notes at the start of the Throne
  (the hidden relics, Hiding Place, Blasted Pastures), at Shape Empire
  (Ancient City, Hall of Bandits), and at Stock Reliquary, whose facedown
  bullet moves to the start of the Throne.
- `docs/rules/rules-reference.md` and `docs/rules/implementation-traceability.md`:
  matching entries.
- `docs/ROADMAP.md`: slice 3 deletes **The last Chronicle powers**; slice 4
  deletes **Retire the unimplemented-card machinery across the stack**.
- Slice 4 updates the docs that describe the holding rule,
  `PowerImplementationStatus` or unimplemented cards
  (`docs/catalog/README.md`, `docs/architecture/codebase-structure.md`,
  `docs/architecture/all-exile-round-endings.md`, `docs/rules/ambiguities.md`
  items 1 and 5, and any the plan's inventory finds).

## Out of scope

- Ordering simultaneous effects on several players in the Chronicle.
- Eras and the reset of lineage points.
