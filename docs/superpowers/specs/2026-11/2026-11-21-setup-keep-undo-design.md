# Undo in a later game's legacy keeps

**Status:** designed 2026-11-21. It amends
[catalog batch 13](2026-11-14-catalog-batch-13-design.md) ruling 3 and
changes where the Quiet Ambitions keep draws, as the
[Legacies design](2026-11-13-legacies-design.md) slice 2 and the
[Legacy Setup powers design](2026-11-13-legacy-setup-powers-design.md)
ruling 13 describe it.

Vocabulary: the [undo design](../2026-10/2026-10-06-undo-design.md) defines
**command batch** and **barrier**. This design adds **deal** and **dealt
pair**.

## Goal

In a later game's Setup, a player who keeps a legacy can take the keep back
until the next keeper answers. Today they never can.

## Why the keeps are barriers today

A later game runs the Quiet Ambitions keep while Foundation III resolves,
for every seat in seat order, before any pawn. Each seat's step is a draw
of 2 legacies into the keeper's hand, then the keep question
(`LegacyKeep.step`). The walk parks at the keep question, so a seat's keep
answer runs on into the next seat's draw, in the same command batch. The
draw is a `Draw`, a hidden outcome, so that batch is a barrier. Every keep
but the last is a barrier for this reason. The last runs on into the
`SetupWorldDeck` window instead, where Secret Society's shuffle and Seer's
peek make it a barrier only when one of them is held.

## Terms

**Deal**:
The one Setup step that draws every seat's legacies from the top of the
legacy deck, in seat order, before the first keep.

**Dealt pair**:
The legacies the deal drew for one seat: 2, or fewer from a short deck.
They wait in the keeper's hand until that seat's keep.

## Rulings

1. **Every seat's legacies are drawn before the first keep.** In a later
   game the deal runs where the first seat's draw runs today: while
   Foundation III resolves, after the `SetupBanks` window. The keeps then
   run in seat order, before any pawn, as today. This amends catalog
   batch 13 ruling 3, which drew each seat's legacies just before that
   seat's keep. The rulebook allows either: Quiet Ambitions says each
   player draws 2 legacies and keeps one, and names no order.
2. **The deal draws in seat order, 2 per seat, from the top.** A short deck
   gives the later seats fewer legacies, and a seat the deck cannot reach
   draws none. An earlier seat's returned legacy goes to the bottom of the
   deck (catalog batch 13 ruling 2), and no later seat can draw it in the
   deal. Today a later seat could draw it once the deck ran dry. With 36
   legacies, a 6-seat game does not run the deck dry in practice.
3. **The keeper draws.** Each dealt pair goes to the hand of
   `SetupLegacyRules.keeper`, as each draw does today. Matchmaker's holder
   therefore holds every other seat's dealt pair at once, as well as their
   own.
4. **A keep offers only its seat's dealt pair.** The keep question reads
   the dealt pair, not the keeper's whole hand. Its decision id stays
   `setup.legacy-choice.<owner>`, its heading and confirm label are
   unchanged, and its line ("p2 kept a legacy", or Matchmaker's "p3 kept a
   legacy for p2 with Matchmaker") is unchanged. A seat dealt one legacy
   keeps it with no question; a seat dealt none is asked nothing
   (Legacies implementation ruling R9).
5. **A keep is an ordinary command batch.** It moves the kept legacy to
   the owner's lineage and the other to the bottom of the deck. Neither move
   is a hidden outcome, so the keeper may undo it while it is the latest
   live batch: until the next keeper answers (undo design, "Who may
   undo"). The last
   keep stays a barrier when Secret Society or Seer acts in the
   `SetupWorldDeck` window, which runs in the same batch.
6. **The deal rides in the batch before it.** The deal holds a `Draw`, so
   that batch is a barrier: Imperial Maps' end, the Chancellor's Reliquary
   pick when Imperial Maps does not run, or the Begin batch when nothing in
   Setup asks a question first. Each is a barrier today too, because the
   first seat's draw runs in it.
7. **A first game deals and keeps with the same steps.** A first game
   already draws every seat's legacies before the first pawn and keeps
   after each pawn (CR p. 7). It uses the same deal and the same keep, and
   only the keeps' place differs. Nothing a player sees changes in a first
   game.
8. **Matchmaker's holder sees every dealt pair from the deal on.** The
   deal puts every other seat's dealt pair in the holder's hand at once,
   and a legacy in a hand is named to the hand's holder. The holder may
   therefore choose one seat's keep knowing another seat's pair. This
   amends Legacy Setup powers implementation ruling R16, under which the
   holder saw another player's draw only while that player's keep was
   pending. The owner still never sees their dealt pair, the kept legacy
   is still named to its owner alone, and the returned one to nobody. The
   deal still comes after World Crafter's and Populist's choices, which
   R16 kept the holder from informing.

## Architecture

All changes are in `LegacyKeep` and the place `SetupProcedure` calls it.

### The deal

`LegacyKeep.deal(participants)` is a `Sequence` of one `Branch` per seat,
in seat order. Each does nothing when Foundation III does not show Quiet
Ambitions. Otherwise it reads the top 2 legacies of the deck when it is
reached, after every earlier seat's draw, as today's per-seat draw does.
For a seat with a nonempty dealt pair it returns two operations:

- `Defer(dealKey(owner), Vector(draw))`, which records the draw, and
- `draw` itself: `Draw(keeper, pair, Location.Deck(CardDeck.Legacy),
  Location.Hand(keeper))`.

`dealKey(owner)` is `DeferralKey("setup.legacy-deal.<owner>")`. Recording
first, then the operation, is the recording form `Deferred` documents.

The deal's `Branch`es hold no question, so the walk never parks inside
one, and no resumed walk depends on how one re-selects from a changed
deck. `Defer` is a leaf: the undo
barrier check and the legality search see only the `Draw` beside it.
`walkerDeferred` is not projected to any client, so the record shows no
viewer a legacy the `Draw` does not already show them.

### The keep

`LegacyKeep.keep(participant)` becomes
`Deferred.resolve(dealKey(owner))`. It reads the dealt pair from the held
`Draw`, then:

- one legacy: moves it to the owner's lineage with no question, and writes
  the kept line;
- two legacies: asks the keep question over the dealt pair, moves the kept
  legacy to the lineage and the other to the bottom of the deck, and writes
  the kept line.

`Deferred.resolve` appends the `TakeDeferred` after these. Nothing after the
keep question parks, as `Deferred.resolve` requires. A walk resumed at the
keep question re-selects the `Branch` and finds the same dealt pair still
held, however the deck and the keeper's hand have changed since the deal.
This replaces today's resume rule, under which the keep read the keeper's
whole hand and so relied on the keeper holding one seat's draw at a time.

The keeper is read when the keep runs, as today. Nothing in Setup changes
who holds Matchmaker between the deal and the keeps.

### SetupProcedure

- A later game calls `LegacyKeep.deal(participants)` once, after the
  `SetupBanks` window, then `LegacyKeep.keep` for every seat, in place of
  `participants.map(LegacyKeep.step)`.
- A first game calls `LegacyKeep.deal(participants)` in place of
  `participants.map(LegacyKeep.draw)`, and keeps `LegacyKeep.keep` after
  each pawn.
- `LegacyKeep.step` and `LegacyKeep.draw` are removed.

The walker drops whatever `Defer` still holds when the Setup walk ends, so
a seat that is never asked leaves nothing behind.

## Testing

New tests are written first and fail before the change.

- `UndoBarrierPlaySuite`, a later game with two keeping seats:
  - seat 1 keeps; seat 1's undo returns the game to seat 1's keep question;
  - after seat 2 keeps, seat 1 is refused as not the issuer, and seat 2
    may undo their own keep.
- The deal, in a later game: when the walk first parks at a keep question,
  every seat's keeper holds that seat's dealt pair.
- Matchmaker, in a later game: the holder holds their own dealt pair and
  every other seat's, and the holder's keep for p2 offers only p2's pair.
- A short deck, in a later game: 3 legacies for three seats. The first
  seat is asked over 2, the second keeps their one with no question, and
  the third is asked nothing.
- Matchmaker, from the deal on: the holder identifies p2's dealt pair at
  p1's keep, and the owner does not (ruling 8).
- A walk resumed at seat 2's keep question, after seat 1 kept, offers seat
  2's dealt pair. Every keep answered through `GameApplicationService`
  resumes the walk, so the scenario tests above cover this; one test
  states it by name.

Existing suites that walk a later game's Setup (`LaterGameSetupChoicesSuite`,
`LegacySetupSuite`, `MatchmakerSuite`, `ImperialMapsSetupSuite`,
`MatchmakerProjectionSuite`, `GameApplicationServiceProjectionSuite`, the
route suites) are updated only where they assert when a draw happens. The
first-game suites must pass unchanged: rule 7 says nothing a player sees
changes there.

## Docs

- Catalog batch 13 ruling 3 gains a pointer to this design's ruling 1.
- The undo design's "Consequences the playtest should expect" gains: "A
  later game's legacy keep can be undone until the next keeper answers;
  the last keep cannot when Secret Society or Seer acts on the world
  deck."

## Left out

- The last keep's barrier when Secret Society or Seer acts at
  `SetupWorldDeck`.
- Imperial Maps' end, the quest swap's peek and the adviser reveal, which
  show something new and stay barriers.
- Undo across the next keeper's answer. The undo design's issuer rule
  holds.
