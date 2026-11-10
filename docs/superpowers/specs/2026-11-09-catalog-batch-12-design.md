# Catalog Batch 12: Negotiation

**Status:** designed 2026-11-09. Slices 1 to 4 landed.

**Builds on** [Catalog batch 11](2026-11-07-catalog-batch-11-design.md),
[batch 10](2026-11-03-catalog-batch-10-design.md) and
[batch 8](2026-10-12-catalog-batch-8-design.md), whose vocabulary, cost
rules, walker shapes and log-line rules apply unchanged. The batch 4
rulings that apply to a whole batch (restriction icons, site capacity,
"your site" and "your region", turn order) apply here too.

## Goal and scope

A Negotiation deal holds favor and relic transfers, disclosures and the
Grand Scepter's Citizenship offer, and the Negotiate action reaches only the
players whose pawns share the actor's site. Eight powers need more than that.
This batch widens the deal and implements all eight. It also adds promises,
written as text, to every Negotiation.

| Slice | Seam | Cards |
|---|---|---|
| 1 | Promises | none |
| 2 | Wider eligibility, binding promises | Traveling Negotiator (242), The Tribunal (E19 intact), Bandit Tribunal (E19 ruined) |
| 3 | Adviser terms | Council Arbiter (200), Festival District (E21 intact) |
| 4 | Site terms | Deed Writer (146) |
| 5 | A remote, private deal | Whispering Stone (R34) |
| 6 | A deal loop inside card play, secret terms | The Gathering (27) |

Slices are ordered so that each seam exists before the slice that reuses
it. Slice 2's binding-promise box is slice 1's promise field with
`NegotiationBounds.bindingPromises` set. Slice 5 reuses slice 2's Tribunal check
for its binding promises. Slice 6 reuses slice 2's reach and Tribunal
check, slice 3's adviser terms, slice 4's site terms, and the deal subtree
that slices 1 to 5 generalize. Slices 5 and 6 touch the
procedure walker's Negotiation subtree and run the walker suites in full
before landing.

The roadmap's **Phase - Negotiation terms and promises** loses its
**Negotiation cards** item when slice 6 lands. Its other parts (banner,
secret and site transfers as general terms, a registry of term kinds,
binding promises with consequences) are not needed by any card here, and
stay with the phase as written, narrowed by what lands.

## Rulings

Taken with the owner on 2026-11-09.

### The Gathering

1. **Pawns first, in turn order from the Chancellor.** The FAQ's "From the
   Chancellor" answers which turn order the card's first sentence uses. With
   no Chancellor, the order starts with the first player, as a round does.
   Each player whose pawn is elsewhere is asked, off turn for all but the
   actor, whether to put their pawn on the site. The actor is asked too
   when their pawn is elsewhere (an Imperial player may play the card to
   another site). The pawns stay there afterwards (FAQ).
2. **Placing a pawn is not Travel.** It costs no Supply and opens no travel
   window.
3. **The deals run simultaneously, one at a time.** The card's second
   sentence gives no order, and the FAQ allows "as many [exchanges] as
   players want". Every player at the site may open a deal with any of the
   others there, or pass. The first command to arrive wins. One deal is open
   at a time.
4. **A pass can be taken back.** A player in no deal may pass, and may
   unpass. A passed player may still be named in a deal, and may decline it.
   The Gathering ends the moment every player at the site has passed and no
   deal is open.
5. **Each deal is a Negotiation** (revised 2026-11-10). The card's
   players "may negotiate", so a Gathering deal is a full Negotiation
   beside its printed exchanges. It holds favor, secrets, relics and
   advisers between any pair of its participants, disclosures, the Grand
   Scepter's Citizenship offer ("In Negotiation"), and non-binding
   promises. **When Negotiating** powers apply as in the Negotiate action:
   The Tribunal opens binding promises (ruling 11), Deed Writer opens site
   terms, and Council Arbiter and Festival District add nothing, since
   every pair may already trade advisers. The power-use blockers apply
   (rulings 24 to 26).

   **Reach widens a Gathering deal.** A player who opens a deal may also
   name a player whose pawn is elsewhere, when Traveling Negotiator, The
   Tribunal or Bandit Tribunal lets the opener reach them (rulings 7 to
   9). Only players at the site open deals, pass and unpass; a named
   remote player takes part in that deal and may decline it.
6. **Secrets come from the player's supply.** A banner's secrets are never
   exchanged.

### Eligibility

7. **Traveling Negotiator works both ways.** Its holder, taking Negotiate,
   may choose any player. Another player taking Negotiate may choose the
   holder, wherever the holder's pawn is.
8. **The Tribunal works the same way** for every player with access to it
   (their pawn at its site, or ruling its site).
9. **Bandit Tribunal is persistent.** While it is on the map it applies to
   every player, ignoring access (CR "Persistent Powers"). Two players whose
   pawns both stand at bandit-ruled sites may negotiate, even at different
   sites.

### Promises

10. **Every Negotiation takes non-binding promises.** Each author may add
    promises as text, up to 5, each at most 160 characters (about two
    sentences). The rules allow non-binding promises at any time, so a
    Gathering deal takes them too.
11. **Binding promises need The Tribunal.** When any participant has access
    to The Tribunal (intact), each author may also add binding promises,
    under the same limits.
12. **The engine enforces no promise.** An agreed deal writes one Game Log
    line per promise. Keeping it is up to the players.
13. **A promise is substance.** A deal holding only promises may be
    accepted and settles with their lines. Editing a promise clears every
    acceptance, as any change to terms does.

### Advisers

14. **Council Arbiter needs its holder on one side.** Adviser terms are
    allowed only where its holder gives or receives.
15. **Festival District opens advisers to all.** When any participant has
    access to it, every participant may give advisers to any other.
16. **Advisers keep their facing, and locked advisers never move.** Edifices
    are never advisers (CR "Edifices"). A recipient knows a facedown adviser
    they receive.
17. **The adviser limit blocks the deal.** Accepting is refused, with the
    reason shown, while the terms would leave any participant over their
    adviser limit. Nothing is discarded.

### Deed Writer

18. **Own colour only.** A site moves only from a player whose own-colour
    warbands rule it, to a player with at least one own-colour warband on
    their board. The Bandit Crown plays no part: the card database's FAQ
    answers the base-game Crown.
19. **No exchange between two Imperial players** (FAQ: they both rule the
    site already).
20. **Warbands move at settlement.** The old ruler moves all their warbands
    at the site to their board. After every transfer, each new ruler places
    any number of their warbands from their board to the site, at least 1
    (FAQ). Denizens, relics, pawns and other forces at the site stay.
28. **Own colour, on any turn** (2026-11-10). Every Imperial player's own
    colour is the Imperial warbands, so any Imperial player may give an
    Imperial site, on any player's turn, and takes its warbands to their
    own board (FAQ: no permission needed from the Chancellor).
29. **One warband per site, after the returns** (2026-11-10). A recipient
    needs one warband on their board for every site they receive, counted
    after the warbands of the sites they give in the same deal come back.
    While anyone would be short, nobody may accept, and the reason shows.

### Whispering Stone

21. **One player, anywhere.** It costs 1 secret, as catalogued, and the
    actor chooses any one other player, wherever their pawn is.
22. **Otherwise a full Negotiation.** Disclosures, the Citizenship offer and
    **When Negotiating** powers apply as in the Negotiate action.
23. **In private.** Players outside the deal see who is negotiating and when
    the deal closes, not its terms. The settlement's transfer and disclosure
    lines are public, since the moved pieces are on the table. Promise lines
    show only to the two participants, and other players get no line at
    all.

### Power-use blockers

Taken with the owner on 2026-11-10.

24. **Blockers apply.** City Wall, Giant Python and Bandit Forest stop The
    Tribunal (intact) and Traveling Negotiator, both non-persistent, as
    they stop any non-persistent power. A refusal City Wall's ruler could
    waive only blocks: the Negotiation asks no consent. Ruling 26 answers
    the same question for Council Arbiter and Festival District, and
    ruling 27 for Deed Writer.
25. **Judged as the user's use.** A blocker judges the use of whoever holds
    the card or has the access, at their own site. If either side of a
    pair has an unblocked route, the two may negotiate.
26. **The adviser powers are blocked the same way** (2026-11-10). The
    three blockers stop Council Arbiter and Festival District, both
    non-persistent, judged as the use of the holder or the player with
    access. A blocked holder opens no adviser terms; another participant's
    unblocked access to Festival District still opens them for all.
27. **Deed Writer is blocked the same way** (2026-11-10). The three
    blockers stop Deed Writer, which is non-persistent, judged as its
    holder's use. A blocked holder opens no site terms.

## The deal model

- **Wider rows.** `NegotiationTransfer` gains `secrets: Int`,
  `advisers: Vector[WorldCardId]` and `sites: Vector[SiteId]`, beside
  `favor` and `relics`. A swap is two rows going opposite ways, agreed
  together; there is no swap term.
- **Promises.** `NegotiationTerms` gains `promises: Vector[String]` and
  `bindingPromises: Vector[String]`. The model refuses a blank promise,
  one with surrounding spaces or a control character, one over 160
  characters, the same promise twice, or more than 5 of a kind. The intent
  mapper trims what the client sends first. The client and the log render
  a promise as plain text, never as markup.
- **`DealRules`**, built when a deal opens, is the one place that says who
  may put what into it. It opens in slice 3, its first per-pair reader.
  Disclosures and the Citizenship offer hold in every deal (rulings 5 and
  22), so `DealRules` carries no flag for them.
  Until then the author's bounds carry the binding-promise flag
  (`NegotiationBounds.bindingPromises`), which `DealRules` sets from slice 3
  on. The `prefix` opens in slice 5. It holds:
  - `participants`;
  - `kinds(author, recipient): Set[TermKind]`, with `TermKind` one of
    `Favor`, `Relic`, `Secret`, `Adviser` and `Site`;
  - `bindingPromises` (ruling 11) and `privateDeal` (ruling 23);
  - `prefix`, the deal's decision-id prefix.
- **Bounds.** `NegotiationBounds` gains the author's secrets, their
  movable advisers and the sites they may give, each offered only to the
  recipients `kinds` allows. The bound is recomputed from live state on
  every command, as now.
- **Acceptance checks.** Beside affordability, accepting is refused when the
  net result would leave anyone over their adviser limit (ruling 17,
  through `AdviserLimit.of` and the count card play uses), or when a
  recipient would have too few warbands to rule every site they receive
  (ruling 29). The bounds already keep out a site term that breaks rulings
  18 and 19. The panel shows each reason on the disabled Accept button.
- **Substance.** `DealState.hasSubstance` also counts promises (ruling 13).
- **One decision id per deal.** The deal fold reads only the answers to
  `<prefix>.deal`. The Negotiate action uses `negotiation`, Whispering Stone
  `whispering-stone`, and the Gathering `gathering.<n>`, with `n` the deal's
  index, so one Gathering can hold several deals without mixing their
  answers.
- **Settlement order.** Disclosures, then transfers in participant order
  (favor, secrets, relics, advisers, sites), then the Deed Writer warband
  questions, then Citizenship, then the promise lines.

## Slice 1: Promises

Rulings 10, 12 and 13.

- **Terms and wire.** The two promise vectors on `NegotiationTerms`, in
  `NegotiationTermsCodec`, the command intents and the projection DTOs. The
  new fields are required: old journals need not decode.
- **Panel.** `NegotiationDealPanel` shows each author's promises under their
  terms, with an "Add promise" button that opens a one-line text field
  (160-character counter) and a remove button per promise. The binding
  section is hidden while the viewer's `NegotiationBounds.bindingPromises`
  is off, which it always is in this slice.
- **Log.** A new `NoteArg.Promise(text)` renders as one quoted plain-text
  span. Lines, last in the settlement: "Blue promised Red: “…”" and "Blue
  made a binding promise to Red and Green: “…”", naming the other
  participants.

Tests: a promise round-trips through the codec; a blank, over-long or sixth
promise is refused; a binding promise is refused while the bounds close
them; a promise-only deal settles and writes its line; editing a promise
clears acceptances; the panel adds and removes a promise.

## Slice 2: Wider eligibility, binding promises

**Traveling Negotiator**, **The Tribunal** and **Bandit Tribunal**
(rulings 7 to 9, 11, 24 and 25).

- **One reader.** `NegotiationDeal.eligible(catalog, state, actor,
  restrictions)` returns the players at the actor's site, plus every other
  player for whom `NegotiationReach.reaches` holds. `NegotiationReach`
  sits beside the action, since actions may not import powers. It holds
  three checks: Traveling Negotiator (either player holds it faceup and
  may use it), The Tribunal (either player has access to it and may use
  it) and Bandit Tribunal (both pawns at bandit-ruled sites, while the card
  is on the map). The two non-persistent checks pass the walk's power-use
  restrictions (rulings 24 and 25).
- **Read once, at the start.** The Negotiation's registry entry is built
  from the walk's powers, so its start sees the restrictions. The start
  stores the candidates and The Tribunal's users as walker start facts
  (`NegotiationStartFacts`), and every rebuild reads them back, as Travel
  reads its source. A rebuild carries no powers, and a settled
  Citizenship can change what Bandit Forest blocks.
- **Binding promises.** `NegotiationBounds.bindingPromises` holds for
  every author when any participant is among The Tribunal's users.
- **Catalog.** Traveling Negotiator and The Tribunal move beside the
  Negotiation (`NegotiationReachCards`). Three powers register them with
  no window. The Tribunal's text keeps its "even into later games", which
  the engine does not track.

Tests: each power's reach in both directions; a player at neither site nor
holding the card stays out; Bandit Tribunal needs both pawns at bandit
sites and the card on the map; City Wall, Giant Python and Bandit Forest
each stopping a use, judged as its user's; the binding-promise box opens
with Tribunal access and its line reads "made a binding promise".

## Slice 3: Adviser terms

**Council Arbiter** and **Festival District** (rulings 14 to 17, 26).

- **Read once, at the start.** `NegotiationAdvisers`, beside the action,
  names the players who may use each card under the walk's power-use
  restrictions. The start stores them in `NegotiationStartFacts`
  (`arbiterUsers`, `festivalUsers`), beside slice 2's reach, since only
  the start sees the restrictions and a settled deal can move Council
  Arbiter.
- **Kinds.** `DealRules`, built from the facts when the deal opens, adds
  `Adviser` for a pair when a participant may use Festival District, or
  when either side may use Council Arbiter. It holds no participant list
  (the deal has one) and only the fields slice 3 reads; the rest open
  with the slices that read them.
- **Movement.** Each adviser moves with `Give(Piece.Card(id))` from play
  area to play area, keeping its orientation. A facedown adviser's recipient
  records knowledge of it.
- **Bounds.** Movable advisers are those no printed restriction refuses
  to give: a faceup locked card and an Ancient Bloodline denizen stay. A
  facedown Vision is an adviser and may move. The limit check reads
  `AdviserLimit.of` and `towardLimit` on the state after every row's
  moves, for each player who gives or receives one. Actions may not
  import powers, so a trait in `gameplay.actions` (`AdviserLimits`)
  carries the limit, and the walker's registry passes `AdviserLimit` to
  the Negotiation's build and rebuild. While anyone would be over,
  `DecisionQuery.Negotiate.overAdviserLimit` names them and nobody may
  accept.
- **Projection.** A facedown adviser on offer is named only to its
  author and counted for everyone else, as a facedown relic is. An
  author can add an adviser disclosure to show it first.
- **Log.** "Blue gave Tinker to Red." A facedown adviser follows the log's
  knowledge rule: others read "a Denizen".
- **Catalog.** Council Arbiter and Festival District (intact) leave
  holding; Squalid District's comment stops calling the intact face
  unimplemented.

Tests: an adviser term offered only with the holder on one side; Festival
District opening it for all participants; City Wall and Giant Python each
stopping a use; a locked adviser never offered;
a swap that would exceed a limit refused, with its reason; a facedown
adviser staying facedown and known to its recipient; an adviser moved
faceup keeping its power with its new holder.

## Slice 4: Site terms

**Deed Writer** (rulings 18 to 20, 27 to 29).

- **Read once, at the start.** `NegotiationSites` names the faceup holder
  of Deed Writer who may use it under the walk's power-use restrictions.
  The start stores them in `NegotiationStartFacts` (`deedWriterUsers`),
  beside slice 3's adviser users.
- **Kinds.** `Site` for a pair when either side may use Deed Writer.
- **Bounds.** The sites an author's own-colour warbands hold, on any turn
  (ruling 28), less any site another author already offers. They are
  offered to every recipient `kinds` allows except an Imperial recipient
  of an Imperial author (ruling 19) and a faceup Vow of Wandering holder,
  who cannot place warbands on sites.
- **Acceptance.** `DecisionQuery.Negotiate.shortOfWarbands` names the
  recipients who would have fewer warbands on their board than sites they
  receive, counted after their own given sites' warbands come back
  (ruling 29). While it is not empty nobody may accept.
- **Settlement.** After the other transfers, a required step moves each
  given site's warbands to the old ruler's board. Then, for each site in
  the order the sites moved, its new ruler answers
  `deed-writer.warbands.<site>`: a number from 1 to the warbands on their
  board, less one for each site they still have to place on. The answer
  places that many from their board, a required move. Rule then follows
  the warbands as usual. The bandit refill runs only at the action
  boundary, so a site left empty while its new ruler answers stays empty.
- **Log.** "Blue gave Broken Peaks to Red." and the placement line "Red
  placed 3 warbands at Broken Peaks." The placement line covers the
  generic "Moved n warbands" detail line of its step.
- **Catalog.** Deed Writer leaves holding.

Tests: an own-colour site offered and a bandit or Imperial one refused;
two Imperial players offered no site to each other; a Citizen giving an
Imperial site on an Exile's turn; City Wall stopping Deed Writer's holder;
a Vow of Wandering holder offered no site; a recipient with an empty board
short of warbands, and the same recipient able to accept when a swap
returns theirs; a swap moving both sites; the old ruler's warbands returned
and the new ruler asked for 1 to N; rule passing to the new ruler.

## Slice 5: A remote, private deal

**Whispering Stone** (rulings 21 to 23).

- **The action.** A `PaidAction` costing 1 secret that asks for one other
  player from every other player in the game, then runs the deal subtree
  with prefix `whispering-stone` and the Negotiate action's `DealRules`,
  with `privateDeal` on. Binding promises follow slice 2.
- **Projection.** For a viewer outside the deal, `NegotiationDealProjector`
  keeps the participants and the accepted marks and leaves transfers,
  disclosures, Citizenship and promises empty, with `privateDeal = true`.
  The panel then shows "Blue and Red are negotiating in private."
- **Log.** The opening line is "Negotiated with Red, in private." Transfer
  and disclosure lines are public. `LogNote` gains
  `readers: Option[Set[PlayerId]]`, `None` on every other line, and the
  promise lines carry `readers = participants`. The formatter numbers every
  entry first and then drops each entry whose readers exclude the viewer,
  so the other entries keep their `(sequence, ordinal)`. The Game Log
  property "every viewer receives the same entries" narrows to entries
  without readers.
- **Catalog.** Whispering Stone leaves holding.

Tests: any player chosen regardless of pawn; the cost paid; an outsider's
projection with no terms; the settlement lines visible to an outsider and
the promise lines absent from their log; a When Negotiating power (Council
Arbiter) widening the private deal.

## Slice 6: A deal loop inside card play

**The Gathering** (rulings 1 to 6).

- **Pawns.** Its **When Played** power is a `Sequence` with one question
  per player in Chancellor order (`TurnOrder.nextRoundStart` and on, round
  the table), skipping players already there: put your pawn here, yes or
  no. A yes moves the pawn with no Travel window.
- **The loop.** A `Repeat` runs while the Gathering is not ended. Each pass
  asks one decision, `gathering.<n>.open`, co-owned by every player at the
  site. Its answers are **Negotiate** with chosen players, **Pass** and
  **Unpass**. The opener may choose players at the site, and players
  elsewhere whom `NegotiationReach` lets the opener reach (ruling 5). A
  Negotiate answer runs one deal subtree with prefix `gathering.<n>` and
  the Gathering's `DealRules`: all four printed kinds between any pair,
  site terms under Deed Writer, disclosures, the Citizenship offer,
  non-binding promises, and binding promises when a participant may use
  The Tribunal (ruling 5). The reach and the Tribunal's and Deed Writer's
  users are read when the deal opens, under the walk's power-use
  restrictions, and stored with the deal's answers so a rebuild reads
  them back.
- **Passing during a deal.** The deal's decision is co-owned by every
  player at the site and every named remote participant. Only players at
  the site may pass or unpass. A non-participant may answer only Pass or Unpass,
  which the deal fold ignores and the pass fold counts.
- **Ending.** Passes are folded from the answers. The loop ends when every
  player at the site has passed and no deal is open. With fewer than two
  players at the site, the loop is skipped.
- **Secrets.** `Secret` terms move secrets from player area to player area.
- **Log.** "Gathered at Broken Peaks: Blue and Red." then each deal's
  "Blue negotiated with Red." and its settlement lines.
- **Catalog.** The Gathering leaves holding.

Tests: the pawn question in Chancellor order, and from the first player
with no Chancellor; a moved pawn opening no Travel window; two deals in one
Gathering kept apart by prefix; a pass, an unpass and a passed player named
in a deal; the end once all pass; a secret and an adviser exchanged; a
disclosure and a Citizenship offer settled; a binding promise opened by
The Tribunal; a remote player named through Traveling Negotiator, and
refused without it; a lone player skipping the loop.

## Seams opened

| Seam | Slice | Reused by |
|---|---|---|
| Promise terms, `NoteArg.Promise`, `NegotiationBounds.bindingPromises` | 1 | every Negotiation, The Tribunal |
| `DealRules` | 3 | slices 4 to 6 |
| Per-deal decision prefix, `LogNote.readers` | 5 | Whispering Stone, The Gathering |
| `NegotiationReach`, `NegotiationStartFacts` | 2 | Traveling Negotiator, The Tribunal, Bandit Tribunal; Whispering Stone's binding promises; The Gathering's reach and binding promises |
| Adviser terms, `AdviserLimits` and the limit check | 3 | Council Arbiter, Festival District, The Gathering |
| Site terms and the warband question | 4 | Deed Writer, The Gathering |
| Private deal projection | 5 | Whispering Stone |
| Deal loop inside card play, secret terms | 6 | The Gathering |

## Testing

Each slice follows the batch 8 to 11 shape: a power suite per card through
`Table`, a golden log line per new line, a codec round trip per new field,
projection suites for what each viewer sees, and a frontend suite for each
new panel input. Each card leaves `unimplemented.txt` with its slice. The
coverage gate stays at 92%.

## Out of scope

- Enforcing promises, and The Tribunal's "even into later games".
- Banner transfers, and sites or secrets as terms outside the powers that
  allow them.
- Simultaneous open deals in one Gathering.
