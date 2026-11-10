# Catalog Batch 10, Slice 12: Nested Card Play Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Search Party (240): a WHEN PLAYED power that peeks at
the bottom 3 cards of a discard pile and plays one as if searched.

**Architecture:** The card play gains a third origin, a regional discard
pile, so the played card moves from the pile itself and never enters the
temporary hand that an enclosing Search re-reads. Search Party is a
`WhenPlayedPower` that asks a pile, peeks, asks a card and embeds
`CardPlayProcedure.unchecked` from that origin. Then the placement tree and
both card-play hooks learn which power made the play (`byPower`), and
`SelectedModifier` applies at no node of a power's play, so the outer
Search's picked modifiers stay out (ruling 5). The walker itself is
unchanged: a probe showed that a nested play already fires its own WHEN
PLAYED and the card-play triggers.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 10 design](../specs/2026-11-03-catalog-batch-10-design.md):
read "Slice 12: Nested card play", ruling 5, and the "Pile names in
lines" reading.

**Card text** (from `~/projects/cards`; Search Party has no FAQ):

- Search Party: "**WHEN PLAYED,** peek at the bottom 3 cards of a discard
  pile and play one as if you searched. _Return the rest._"

## Plan-time findings

1. **The walker needs no change.** The spec says a card played inside a
   WHEN PLAYED power fires neither its own WHEN PLAYED nor the card-play
   triggers, because `WalkerHooks.gathered` already holds the hook
   windows. That is no longer true: `CardPlayedFaceup` and
   `CardPlayedFacedown` are windowed composites, not `LeafOperation`s, and
   `walkComposite` never adds a window to `gathered`. A probe at
   `552c03c6` confirmed it: a test-only `WhenPlayedPower` on Autumn Wind's
   card, whose effect was a favor gain and a bare `CardPlayedFaceup` of
   Salad Days, gained the favor and then parked on Salad Days' bank
   question (`cardplay.salad-days.banks`, at `0.1.1.0.1.0.0`). The spec's
   `AutomaticUse` change is dropped, and Task 1's tests pin the behavior.
2. **The temporary hand.** On a one-card draw the Search reads its kept
   card from the live temporary hand, falling back on the first placement
   answer once the hand is empty (`SearchProcedure.selectedCards`). A
   nested card routed through the hand would be read as the Search's own
   card. The new `Origin.DiscardPile(region)` plays it from the pile.
   `placedCard` still finds the outer card, since the outer placement is
   answered first.
3. **Picked Search modifiers would reach the nested play.** The nested
   `PlacementTree` folds `SearchPlayAdviser` with the walk's answers, so a
   picked Bracken, Cracked Horn, Ring of Devotion, Baron, New Growth,
   Forest Warden, Crop Rotation or Land Warden applies to it. The nested
   hook's fold runs with `ctx.procedure` still `Search`, so Welcoming
   Party and Wild Cry fire for it. Ruling 5 says none of them apply. Task
   2 gives the placement tree and both hooks `byPower: Option[PowerId]`,
   and `SelectedModifier.applicable` refuses every window node of a
   power's play. It is one rule in one place, not a check in each
   modifier. The outer Search's `SearchActionEligibility` rewrite does not
   reach the nested play: `OperationRewrite` stops at the windowed hook.
4. **Temple of Ancestors would limit the nested play.** Its `Transform`
   limits every placement tree in its walk to the advisers and the
   discard, so a Search Party it draws could not play its card to a site.
   Task 2 scopes the limit to the tree whose `byPower` is Temple of
   Ancestors. Oracle's play is also marked, so `byPower` is truthful for
   every power's play; Oracle has no `SearchPlayAdviser` contribution, so
   nothing changes there.
5. **Active modifiers stay protected.** The nested play's `CardSlots` still
   receives the outer picks (`ModifierPicks.cards`), so it cannot replace
   or discard a picked modifier's card at a site. The modifiers are still
   in use for the Search; only their effects stay out.
6. **Persistent rules apply**, as if searched: Saddle Makers, Gossip, the
   adviser limits, the People's Favor's Mob discard, Lost Tapestry and
   Pied Piper.
7. **A discard may return to its own pile.** The play discards to the pile
   of the region after the user's pawn's, which may be the pile the card
   came from. `Discard.Denizen` moves it from the pile (position
   unspecified) to the same pile's top, which the `Move` guard accepts
   because the positions differ. A test pins it. If the pipeline refuses
   it, stop and report.
8. **Stable shape.** The play may empty every pile, so the effect decides
   between "nothing to peek" and the full shape from the live piles only
   until the pile question is answered. After that it always builds the
   full shape. The answered questions are never re-asked on resume, so
   their options may change after the play without moving any position.
9. **Lines.** "Peeked at the bottom of the Hinterland discard pile.", as
   the "Pile names in lines" reading words piles (the spec wrote
   "Provinces' discard pile"). It covers the peeks. With every pile empty:
   "Every discard pile was empty." An empty pile shows unavailable: "The
   pile is empty". Both decisions are narrated: the line names the pile,
   and the card play's own line names the card.
10. **The card moves** from `catalog/holding/HoldingNomadDenizens.scala` to
    `powers/whenplayed/SearchParty.scala`. Six campaign suites import it
    from holding and are rewritten.

## Global Constraints

- **Slice 11 first.** Slice 11 edits `WhenPlayedPowers.all`,
  `RivalKhanSuite`'s imports, the spec's status line, the ROADMAP's
  engine-blocked list and both snapshot files, which this plan also edits.
  Before Task 1, check that slice 11 has merged into `0.2.0-alpha`, then
  rebase onto it:

  ```bash
  git log --oneline 0.2.0-alpha | grep "feat(powers): implement Favored Son"
  ```

  If that prints nothing, stop and report that slice 11 has not merged.
  Otherwise, with a clean worktree, run `git rebase 0.2.0-alpha`.
- Work only in this worktree (`.worktrees/batch-10-slice-12`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import or a
  non-exhaustive match fails it.
- Log lines name no actor (they omit "you").
- Test helper names say what they build.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.

---

### Task 1: The discard-pile origin and Search Party

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyFixture.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartySuite.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/CardPlay.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala` (the card moves out)
- Modify (imports): `PledgeOfDefenseSuite`, `GreatCrusadeSuite`, `KindredWarriorsSuite`, `ImperialTacticsSuite`, `SaltTheEarthSuite`, `RivalKhanSuite`, all in `src/test/scala/distantlands/gameplay/powers/campaign/`
- Modify: `src/test/resources/powers/unimplemented.txt` and `src/test/resources/notes/declared-keys.txt` (regenerated)

**Interfaces:**
- Produces:
  - `CardPlay.Origin.DiscardPile(region: Region)`
  - `CardPlayProcedure.Origin.DiscardPile(region: Region)`
  - `SearchPartyCard` and `case object SearchParty extends WhenPlayedPower`
    in `oathdigital.gameplay.powers.whenplayed`, with
    `pileDecisionId: String`, `cardDecisionId: String`,
    `pileRef(region: Region): DecisionOptionRef.Button`,
    `emptyPile: String`, `used: NoteKey`, `none: NoteKey`
  - `SearchPartyFixture` (test): `plainHearth`,
    `p1HoldsSearchPartyFacedown`, `playedFaceupFromFacedown`,
    `pilePicked`, `cardPicked`, `peekedAndPicked`, `placedAs`

- [ ] **Step 1: Check that slice 11 has merged, and rebase**

Follow the "Slice 11 first" constraint above.

- [ ] **Step 2: Write the fixture**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyFixture.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{PowerFixture, SearchFixture}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{PlacementPicks, Table}
import oathdigital.testkit.Unwrap.accepted

/** Drives Search Party's WHEN PLAYED through the rules with the production
  * walker powers (catalog batch 10, slice 12). p1 stands at Ancient City,
  * in the Cradle, so a card p1 discards goes to the Provinces pile. Each
  * helper returns the events of every command so far, so a replay can
  * start from the table. */
object SearchPartyFixture:
  import PowerFixture.actor
  import SearchFixture.{place, rules}

  /** Four plain Hearth denizens, with no production power of their own. */
  val plainHearth: Vector[DenizenId] =
    SearchFixture.anyPlainDenizensOf(Suit.Hearth).take(4)

  /** p1 at Ancient City holding Search Party facedown, with `arrange`
    * applied. The quiet table's discard piles start empty. */
  def p1HoldsSearchPartyFacedown(arrange: Table => Table)(
      using munit.Location): ReadyGame =
    arrange(Table.start.pawn(actor, AncientCitySite)
      .adviser(actor, SearchPartyCard, facedown = true)).ready

  /** p1 plays the facedown Search Party faceup, which fires its WHEN
    * PLAYED. */
  def playedFaceupFromFacedown(ready: ReadyGame)(using munit.Location)
      : OathTransition =
    SearchFixture.playFacedown(ready, Vector.empty, SearchPartyCard.id,
      PlayedAs.Faceup)

  /** `from`, parked on Search Party's pile question, answered `region`. */
  def pilePicked(from: OathTransition, region: Region)(
      using munit.Location): OathTransition =
    chained(from, rules.resolveWalker(from.state, actor,
      SearchParty.pileDecisionId, ChooseOneAnswer(SearchParty.pileRef(region))))

  /** `from`, parked on Search Party's card question, answered `card`. */
  def cardPicked(from: OathTransition, card: WorldCardId)(
      using munit.Location): OathTransition =
    chained(from, rules.resolveWalker(from.state, actor,
      SearchParty.cardDecisionId,
      ChooseOneAnswer(PlacementPicks.optionNaming(card))))

  /** Both questions answered: parked on `card`'s placement. */
  def peekedAndPicked(from: OathTransition, region: Region,
      card: WorldCardId)(using munit.Location): OathTransition =
    cardPicked(pilePicked(from, region), card)

  /** `from`, parked on `card`'s placement, placed the way `way` names. */
  def placedAs(from: OathTransition, card: WorldCardId, way: PlayedAs)(
      using munit.Location): OathTransition =
    chained(from, place(from, card, way))

  /** `next`'s transition, with `from`'s events before its own. */
  private def chained(from: OathTransition,
      next: Either[OathViolation, OathTransition])(using munit.Location)
      : OathTransition =
    val done = accepted(next)
    done.copy(events = from.events ++ done.events)
```

- [ ] **Step 3: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartySuite.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.SearchPlay
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{NoteText, PowerFixture, SearchFixture}
import oathdigital.gameplay.powers.cardplay.GossipCard
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, EconomyActions, Look, MenuPlay,
  PlacementPicks, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p2
import oathdigital.testkit.Unwrap.accepted

/** Search Party (card 240, catalog batch 10, slice 12), WHEN PLAYED: "peek
  * at the bottom 3 cards of a discard pile and play one as if you
  * searched. Return the rest." Most tests play Search Party from p1's
  * facedown advisers, so no Search draw adds cards to a pile. */
class SearchPartySuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchFixture.{after, rules}
  import SearchPartyFixture._

  private val plain = plainHearth

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(SearchParty.id, SearchParty.noteKeys, events)

  test("it peeks at the bottom 3 of the chosen pile and asks which to " +
      "play; the rest stay in order, and the line names the pile"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      plain*))
    val played = playedFaceupFromFacedown(ready)
    assertEquals(EconomyActions.parkedDecision(played.state).decisionId,
      SearchParty.pileDecisionId)
    val piled = pilePicked(played, Region.Hinterland)
    val asked = EconomyActions.parkedDecision(piled.state)
    assertEquals(asked.decisionId, SearchParty.cardDecisionId)
    assertEquals(WhenPlayedHarness.chooseOneOptions(asked).map(_.ref),
      plain.take(3).map(PlacementPicks.optionNaming))
    val peeked = after(piled)
    plain.take(3).foreach(card => assert(Look(peeked).knowsAdviser(actor,
      card), s"p1 should know $card"))
    assert(!Look(peeked).knowsAdviser(actor, plain(3)))
    plain.foreach(card => assert(!Look(peeked).knowsAdviser(p2, card)))
    val done = placedAs(cardPicked(piled, plain(1)), plain(1),
      PlayedAs.Faceup)
    val end = after(done)
    assertEquals(Look(end).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plain(1)))
    assertEquals(Look(end).discardPile(Region.Hinterland),
      Vector[WorldCardId](plain(0), plain(2), plain(3)))
    assertEquals(said(done.events), Vector(NoteText.Said(NoteKey.Used,
      "Peeked at the bottom of the Hinterland discard pile.",
      covers = true)))

  test("the card it plays fires its own WHEN PLAYED"):
    val ready = p1HoldsSearchPartyFacedown(_.darkestSecret(Some(p2), 3)
      .discarded(Region.Hinterland, CharlatanCard))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, CharlatanCard.id), CharlatanCard.id,
      PlayedAs.Faceup)
    assertEquals(Look(after(done)).secretsOnDarkestSecret, Charlatan.Kept)

  test("the card it plays facedown fires an enemy's Gossip"):
    val ready = p1HoldsSearchPartyFacedown(_.adviser(p2, GossipCard)
      .discarded(Region.Hinterland, plain(0)))
    val before = Look(ready).favor(p2)
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, plain(0)), plain(0), PlayedAs.Facedown)
    assertEquals(Look(after(done)).favor(p2), before + 1)
    assertEquals(Look(after(done)).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plain(0)))

  test("the card may go to p1's site"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      plain(0)))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, plain(0)), plain(0), PlayedAs.AtSite)
    assert(Look(after(done)).cardsAt(AncientCitySite).contains(plain(0)))
    assertEquals(Look(after(done)).discardPile(Region.Hinterland),
      Vector.empty[WorldCardId])

  test("discarded, the card goes on top of the next region's pile, even " +
      "the pile it came from"):
    // p1 is in the Cradle, so the discard goes to the Provinces pile.
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
      plain.take(3)*))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, plain(0)), plain(0), PlayedAs.Discarded)
    assertEquals(Look(after(done)).discardPile(Region.Provinces),
      Vector[WorldCardId](plain(1), plain(2), plain(0)))

  test("a Vision is revealed as from a Search, and the Visions Drawn " +
      "track stays"):
    val faith = VisionRules.Faith
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      faith))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, faith), faith, PlayedAs.Faceup)
    assertEquals(Look(after(done)).player(actor).revealedVision.map(_.id),
      Some(faith))
    assertEquals(after(done).game.current.tracks.visionsDrawn,
      ready.game.current.tracks.visionsDrawn)

  test("an empty pile shows unavailable"):
    val played = playedFaceupFromFacedown(p1HoldsSearchPartyFacedown(
      _.discarded(Region.Hinterland, plain(0))))
    val options = WhenPlayedHarness.chooseOneOptions(
      EconomyActions.parkedDecision(played.state))
    val empty = OptionReason.Written(SearchParty.emptyPile)
    assertEquals(options.collect {
      case DecisionOption.Unavailable(inner, reason) => inner.ref -> reason
    }, Vector(SearchParty.pileRef(Region.Cradle) -> empty,
      SearchParty.pileRef(Region.Provinces) -> empty))
    assert(options.exists(option => option.available &&
      option.ref == SearchParty.pileRef(Region.Hinterland)))

  test("with every pile empty nothing is asked, and the line says so"):
    val done = playedFaceupFromFacedown(p1HoldsSearchPartyFacedown(identity))
    assertNotEquals(EconomyActions.parkedDecision(done.state).decisionId,
      SearchParty.pileDecisionId)
    assertEquals(said(done.events), Vector(NoteText.Said("used.none",
      "Every discard pile was empty.", covers = false)))

  test("kept from a one-card Search, it plays its card from the pile and " +
      "the Search still keeps Search Party; replay and the wire agree"):
    val ready = Table.start.pawn(actor, AncientCitySite)
      .discarded(Region.Cradle, SearchPartyCard)
      .discarded(Region.Hinterland, plain.take(2)*).ready
    val searched = accepted(SearchPlay.searched(rules, Ready(ready), actor,
      SearchSource.RegionalDiscard(Region.Cradle)))
    val kept = placedAs(searched, SearchPartyCard.id, PlayedAs.Faceup)
    val done = placedAs(peekedAndPicked(kept, Region.Hinterland, plain(1)),
      plain(1), PlayedAs.Faceup)
    val end = after(done)
    assertEquals(Look(end).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plain(1)))
    assert(end.game.current.temporaryHands.getOrElse(actor, Vector.empty)
      .isEmpty)
    assertEquals(Look(end).discardPile(Region.Hinterland),
      Vector[WorldCardId](plain(0)))
    assertEquals(MenuPlay.procedureBeyondMenu(end), None)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), end)
    assertWireRoundTrips(done.events)
```

- [ ] **Step 4: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.SearchPartySuite"`
Expected: FAIL to compile, with `Not found: SearchParty` (and the
fixture's `SearchPartyCard` resolving to nothing in its package).

- [ ] **Step 5: Add the discard-pile origin to `CardPlay`**

In `src/main/scala/distantlands/gameplay/actions/CardPlay.scala`, replace

```scala
  enum Origin { case FacedownAdviser, TemporaryHand }
```

with

```scala
  /** Where a played card comes from: the actor's facedown advisers, a
    * Search's temporary hand, or a regional discard pile, which Search
    * Party plays from (catalog batch 10, slice 12). */
  enum Origin:
    case FacedownAdviser, TemporaryHand
    case DiscardPile(region: Region)
```

In `keptSource`, add the pile after the `FacedownAdviser` case:

```scala
      case Origin.FacedownAdviser => PositionedLocation(Location.PlayArea(player))
      case Origin.DiscardPile(region) =>
        PositionedLocation(Location.RegionalDiscard(region))
```

In `validateOrigin`, the pile is checked by the move guard, which refuses
a card that is not where the move says:

```scala
    case Origin.TemporaryHand | _: Origin.DiscardPile => Right(())
```

In `planVision`, a Vision from a pile plays as one from the hand. Change
the two `Origin.TemporaryHand` cases to

```scala
    case Origin.TemporaryHand | _: Origin.DiscardPile
        if orientation == Orientation.FaceUp =>
```

and

```scala
    case Origin.TemporaryHand | _: Origin.DiscardPile =>
```

- [ ] **Step 6: Offer the pile's card the hand's slots**

In `src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala`,
in `facedown`, change `case CardPlay.Origin.TemporaryHand =>` to

```scala
    case CardPlay.Origin.TemporaryHand | _: CardPlay.Origin.DiscardPile =>
```

and in `adviserSlots` make the same change to its
`case CardPlay.Origin.TemporaryHand =>`.

- [ ] **Step 7: Add the origin to `CardPlayProcedure`**

In `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala`,
replace

```scala
  enum Origin { case TemporaryHand, FacedownAdviser }
```

with

```scala
  /** Where the played card is: a Search's temporary hand, the actor's
    * facedown advisers, or a regional discard pile (Search Party, catalog
    * batch 10, slice 12). A card played from a pile never passes through
    * the temporary hand, which an enclosing Search reads on a one-card
    * draw. */
  enum Origin:
    case TemporaryHand, FacedownAdviser
    case DiscardPile(region: Region)
```

In `heldAtOrigin`, add:

```scala
    case Origin.DiscardPile(region) =>
      ready.game.current.commonCards.discard(region).contains(card)
```

In `cardPlayOrigin`, add:

```scala
    case Origin.DiscardPile(region) => CardPlay.Origin.DiscardPile(region)
```

Run: `./sbtw compile`
Expected: success. A non-exhaustive match warning anywhere means a case
was missed; add the pile beside `TemporaryHand` there.

- [ ] **Step 8: Move the card and implement the power**

In `src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala`,
delete the `SearchPartyCard` object (its six lines and the blank line
after them). The file's imports stay used.

Create `src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver.{ModifierPicks, PowerCtx}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object SearchPartyCard extends Denizen(DenizenId("240"), "Search Party",
    Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.search-party"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** peek at the bottom 3 cards of a discard " +
      "pile and play one as if you searched. _Return the rest._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Search Party (card 240), WHEN PLAYED: "peek at the bottom 3 cards of a
  * discard pile and play one as if you searched. Return the rest."
  * (catalog batch 10, slice 12).
  *
  * The user picks a discard pile; an empty one shows unavailable. Its
  * bottom 3 cards, or fewer, are recorded as `Peek`s, so only the user may
  * identify them, and the user must play one. The play is the Search's
  * card play from the pile itself (`Origin.DiscardPile`): faceup to the
  * site, faceup or facedown as an adviser, or discarded to the next
  * region's pile (ruling 5). A Vision follows Search's rules and leaves
  * the Visions Drawn track alone. The card never enters the temporary
  * hand, which an enclosing Search reads. The rest never moved, so they
  * stay at the bottom in order. The play's hook fires the card's own WHEN
  * PLAYED and the card-play triggers.
  *
  * The play may empty every pile, so the effect reads the live piles only
  * until the pile question is answered. After that it always builds the
  * full shape, so a resume inside the play finds its position.
  *
  * Its line covers the peeks: "Peeked at the bottom of the Hinterland
  * discard pile." The card play writes its own line, which names the card.
  * With every pile empty nothing is asked: "Every discard pile was empty."
  */
case object SearchParty extends WhenPlayedPower:
  val id: PowerId = SearchPartyCard.power.id
  val cardId: DenizenId = SearchPartyCard.id
  val pileDecisionId: String = "power.search-party.pile"
  val cardDecisionId: String = "power.search-party.card"
  /** How many cards it peeks at. */
  val Peeked: Int = 3
  /** Why an empty pile cannot be picked. */
  val emptyPile: String = "The pile is empty"

  private val notes = NoteSet(id)
  /** "Peeked at the bottom of the {Hinterland discard pile}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Peeked at the bottom of the "), NotePart.Arg(0),
    NotePart.Text(".")))
  /** "Every discard pile was empty." */
  val none: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Every discard pile was empty.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(pileDecisionId, cardDecisionId)

  /** The pile question's button for `region`'s discard pile. */
  def pileRef(region: Region): DecisionOptionRef.Button =
    DecisionOptionRef.Button(region.key)

  private val searchParty: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, pending) =>
      if chosen(pending.answered).isEmpty &&
          Region.all.forall(bottom(live, _).isEmpty) then
        Vector[Operation](Note(id, _ => Some(none(searchParty))))
      else Vector[Operation](pileQuestion(live, ctx.actor), peeks(ctx.actor),
        Note(id, peekedNote, covers = true), cardQuestion(ctx.actor),
        play(ctx))))

  /** The three piles, an empty one unavailable. */
  private def pileQuestion(ready: ReadyGame, actor: PlayerId): Decide =
    Decide(pileDecisionId, actor, DecisionQuery.ChooseOne(
      Region.all.map { region =>
        val button = DecisionOption.Button(pileRef(region),
          SearchSource.name(SearchSource.RegionalDiscard(region)).capitalize)
        if bottom(ready, region).isEmpty then
          DecisionOption.Unavailable(button, OptionReason.Written(emptyPile))
        else button
      },
      heading = Some("Search Party: peek at the bottom of a discard pile")))

  /** A peek at each card at the bottom of the chosen pile. */
  private def peeks(actor: PlayerId): Operation =
    BuildOps((now, pending) => Right(chosen(pending.answered).toVector
      .flatMap(region => bottom(now, region).map(card =>
        Peek(actor, card, Location.RegionalDiscard(region))))))

  private def peekedNote(states: NoteStates): Option[LogNote] =
    chosen(states.answered).map(region => used(searchParty,
      NoteArg.Pile(SearchSource.RegionalDiscard(region))))

  /** The card to play, among the peeked ones, bottom first. */
  private def cardQuestion(actor: PlayerId): Operation =
    Branch((now, pending) => chosen(pending.answered).toVector.map(region =>
      Decide(cardDecisionId, actor, DecisionQuery.ChooseOne(
        bottom(now, region).map(option),
        heading = Some("Search Party: choose a card to play")))))

  /** The picked card's play from its pile. Once it has been played, the
    * tree settles from the answers (`CardPlayProcedure.unchecked`). */
  private def play(ctx: PowerCtx): Operation = Branch((now, pending) =>
    (for
      region <- chosen(pending.answered)
      card <- PowerAnswers.last(pending.answered, cardDecisionId)
        .flatMap(worldCard)
    yield CardPlayProcedure.unchecked(ctx.catalog, now, ctx.actor, card,
      CardPlayProcedure.Origin.DiscardPile(region),
      ModifierPicks.cards(pending.answered))).toVector)

  private def chosen(answered: Vector[Answered]): Option[Region] =
    PowerAnswers.last(answered, pileDecisionId)
      .flatMap(ref => Region.all.find(pileRef(_) == ref))

  /** The pile's bottom cards, bottom first. A pile is stored top last. */
  private def bottom(ready: ReadyGame, region: Region): Vector[WorldCardId] =
    ready.game.current.commonCards.discard(region).take(Peeked)

  private def option(card: WorldCardId): DecisionOption = card match
    case denizen: DenizenId =>
      DecisionOption.Denizen(DecisionOptionRef.Denizen(denizen))
    case vision: VisionId =>
      DecisionOption.Vision(DecisionOptionRef.Vision(vision))

  private def worldCard(ref: DecisionOptionRef): Option[WorldCardId] =
    ref match
      case DecisionOptionRef.Denizen(denizen) => Some(denizen)
      case DecisionOptionRef.Vision(vision) => Some(vision)
      case _ => None
```

In `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`,
append `SearchParty` to the end of `all`. After slice 11 the last line
reads `Pilgrimage, AutumnWind, FavoredSon)`; make it
`Pilgrimage, AutumnWind, FavoredSon, SearchParty)`.

- [ ] **Step 9: Rewrite the test imports**

List them:

```bash
git grep -n "SearchPartyCard" -- src/test | grep "holding"
```

Edit each match so `SearchPartyCard` comes from
`oathdigital.gameplay.powers.whenplayed`:

- `PledgeOfDefenseSuite` and `ImperialTacticsSuite`: replace
  `import oathdigital.catalog.holding.{SearchPartyCard, VowOfKinshipCard}`
  with

  ```scala
  import oathdigital.catalog.holding.VowOfKinshipCard
  import oathdigital.gameplay.powers.whenplayed.SearchPartyCard
  ```

- `GreatCrusadeSuite`: replace
  `import oathdigital.catalog.holding.{SearchPartyCard, TheGatheringCard,`
  and the next line `  VowOfKinshipCard}` with

  ```scala
  import oathdigital.catalog.holding.{TheGatheringCard, VowOfKinshipCard}
  import oathdigital.gameplay.powers.whenplayed.SearchPartyCard
  ```

- `KindredWarriorsSuite` and `SaltTheEarthSuite`: delete
  `import oathdigital.catalog.holding.SearchPartyCard`, and change their
  `import oathdigital.gameplay.powers.whenplayed.RoyalTaxCard` to
  `import oathdigital.gameplay.powers.whenplayed.{RoyalTaxCard, SearchPartyCard}`.
- `RivalKhanSuite`: slice 11 left it importing the card on its own line,
  `import oathdigital.catalog.holding.SearchPartyCard`. Replace that line
  with `import oathdigital.gameplay.powers.whenplayed.SearchPartyCard`. If
  it still reads `{MagiciansCodeCard, SearchPartyCard}`, slice 11 has not
  landed: stop and report.

Then check that nothing is left:

```bash
git grep -n "holding.*SearchPartyCard" -- src
```

Expected: no output.

- [ ] **Step 10: Regenerate the snapshots and run the suites**

```bash
NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"
```

```bash
git diff src/test/resources
```

Expected: `unimplemented.txt` loses exactly `denizen.search-party`, and
`declared-keys.txt` gains exactly:

```
denizen.search-party used = Peeked at the bottom of the {0}.
denizen.search-party used.none = Every discard pile was empty.
```

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.* oathdigital.gameplay.powers.cardplay.* oathdigital.gameplay.powers.search.* oathdigital.gameplay.powers.campaign.* oathdigital.gameplay.powers.action.* oathdigital.gameplay.CardPlayProcedureSuite oathdigital.gameplay.CardPlayHooksSuite oathdigital.gameplay.CardPlayRulesSuite oathdigital.gameplay.SearchSuite oathdigital.gameplay.SearchProcedureSuite oathdigital.gameplay.FacedownAdviserWalkerSuite"`
Expected: PASS. If the same-pile discard test fails inside the operation
pipeline, stop and report (finding 7).

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyFixture.scala src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartySuite.scala src/main/scala/distantlands/gameplay/actions/CardPlay.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardSlots.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala src/main/scala/distantlands/catalog/holding/HoldingNomadDenizens.scala src/test/scala/distantlands/gameplay/powers/campaign/PledgeOfDefenseSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/GreatCrusadeSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/KindredWarriorsSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/ImperialTacticsSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/SaltTheEarthSuite.scala src/test/scala/distantlands/gameplay/powers/campaign/RivalKhanSuite.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt
git commit -m "$(cat <<'EOF'
feat(powers): implement Search Party

The card it plays moves from its discard pile itself, so an enclosing
Search never finds it in the temporary hand.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 1:** review Task 1 (spec slice 12, findings 1, 2 and
5 to 10).

---

### Task 2: A power's play, docs and gates

**Files:**
- Create: `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyModifiersSuite.scala`
- Modify: `src/main/scala/distantlands/model/CoreOperations.scala` (both hooks)
- Modify: `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/TempleOfAncestors.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/action/Oracle.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala`
- Modify (hook patterns): `WhenPlayedPower.scala`, `ConspiracyWhenPlayed.scala` (both in `powers/whenplayed`), `WildCry.scala`, `SaddleMakers.scala`, `Gossip.scala`, `WelcomingParty.scala`, `VisionPlay.scala` (all in `powers/cardplay`)
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Consumes: `SearchParty`, `SearchPartyCard`, `SearchPartyFixture` from
  Task 1.
- Produces:
  - `CardPlayedFaceup(card: WorldCardId, resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)`
  - `CardPlayedFacedown(card: WorldCardId, player: PlayerId, byPower: Option[PowerId] = None)`
  - `CardPlayProcedure.PlacementTree.byPower: Option[PowerId]`
  - `CardPlayProcedure.unchecked(catalog, ready, actor, card, origin, picked: Vector[CardId] = Vector.empty, byPower: Option[PowerId] = None): Operation`
  - `CardPlayProcedure.playedByPower(operation: Operation): Boolean`

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyModifiersSuite.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.SearchPlay
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{PhasePowerCatalog, PowerFixture,
  SearchFixture, WalkerPowerCatalog}
import oathdigital.gameplay.powers.action.{TempleOfAncestors,
  TempleOfAncestorsCard}
import oathdigital.gameplay.powers.cardplay.{WelcomingParty,
  WelcomingPartyCard}
import oathdigital.gameplay.powers.search.{NewGrowth, NewGrowthCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, PlacementPicks, PowerRules, Table}
import oathdigital.testkit.Unwrap.accepted

/** Search Party's play is not the Search's (catalog batch 10, slice 12,
  * ruling 5): no modifier picked for the Search reaches it, and Temple of
  * Ancestors limits only its own play. p1 searches the Cradle discard
  * pile, which holds one card, so nothing else is drawn. */
class SearchPartyModifiersSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchFixture.{after, rules}
  import SearchPartyFixture._

  private val hearth = plainHearth
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val phasePowers = PhasePowerCatalog.default

  /** p1 at Ancient City with `kept` alone in the Cradle pile and `arrange`
    * applied. */
  private def cradlePileHolding(kept: WorldCardId)(arrange: Table => Table)
      : ReadyGame =
    arrange(Table.start.pawn(actor, AncientCitySite)
      .discarded(Region.Cradle, kept)).ready

  /** p1 searches the Cradle pile with `modifiers` picked: parked on the
    * kept card's placement. */
  private def cradleSearched(ready: ReadyGame, modifiers: Vector[PowerId])
      : OathTransition =
    accepted(SearchPlay.searched(rules, Ready(ready), actor,
      SearchSource.RegionalDiscard(Region.Cradle), modifiers))

  /** The sites the parked placement offers a slot at, available or not. */
  private def sitesOffered(state: OathState): Set[SiteId] =
    PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(state,
      walkerPowers)).flatMap(option =>
      CardPlayProcedure.siteOf(option.ref)).toSet

  test("Welcoming Party, picked for the Search, rewards Search Party's " +
      "play but not the card Search Party plays"):
    val ready = cradlePileHolding(SearchPartyCard.id)(_
      .denizen(WelcomingPartyCard, at = AncientCitySite)
      .discarded(Region.Hinterland, hearth(0)))
    val before = Look(ready).favor(actor)
    val kept = placedAs(cradleSearched(ready, Vector(WelcomingParty.id)),
      SearchPartyCard.id, PlayedAs.Faceup)
    val done = placedAs(peekedAndPicked(kept, Region.Hinterland, hearth(0)),
      hearth(0), PlayedAs.Faceup)
    assertEquals(Look(after(done)).favor(actor),
      before + WelcomingParty.Favor)

  test("New Growth, picked for the Search, opens other sites to a Hearth " +
      "card the Search keeps, but not to one Search Party plays"):
    val direct = cradleSearched(cradlePileHolding(hearth(0))(_
      .adviser(actor, NewGrowthCard)), Vector(NewGrowth.id))
    assert(sitesOffered(direct.state).exists(_ != AncientCitySite.id),
      "New Growth should open another site to the Search's own card")
    val ready = cradlePileHolding(SearchPartyCard.id)(_
      .adviser(actor, NewGrowthCard)
      .discarded(Region.Hinterland, hearth(0)))
    val kept = placedAs(cradleSearched(ready, Vector(NewGrowth.id)),
      SearchPartyCard.id, PlayedAs.Faceup)
    val asked = peekedAndPicked(kept, Region.Hinterland, hearth(0))
    assertEquals(sitesOffered(asked.state), Set(AncientCitySite.id))

  test("Search Party drawn by Temple of Ancestors may play its card to a " +
      "site"):
    val temple = PowerRules.withWalkerAndPhasePowers
    val ready = Table.start.pawn(actor, AncientCitySite)
      .edifice(TempleOfAncestorsCard.id, EdificeSide.Intact,
        at = AncientCitySite)
      .favor(actor, 2).worldDeckBottom(SearchPartyCard)
      .discarded(Region.Hinterland, hearth(0)).ready
    val drew = accepted(temple.startWalker(Ready(ready),
      ActionRef.UsePower(TempleOfAncestors.id), actor,
      Vector(DecisionOptionRef.Edifice(TempleOfAncestorsCard.id))))
    val played = accepted(temple.resolveWalker(drew.state, actor,
      CardPlayProcedure.placementDecisionId(SearchPartyCard.id),
      PlacementPicks.parkedAnswer(drew.state, PlayedAs.Faceup, walkerPowers,
        phasePowers)))
    val piled = accepted(temple.resolveWalker(played.state, actor,
      SearchParty.pileDecisionId,
      ChooseOneAnswer(SearchParty.pileRef(Region.Hinterland))))
    val asked = accepted(temple.resolveWalker(piled.state, actor,
      SearchParty.cardDecisionId,
      ChooseOneAnswer(PlacementPicks.optionNaming(hearth(0)))))
    val slots = PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(
      asked.state, walkerPowers, phasePowers))
    assert(slots.nonEmpty)
    assert(slots.forall(_.available), clue(slots))
```

- [ ] **Step 2: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.SearchPartyModifiersSuite"`
Expected: all three tests FAIL: p1's favor is `before + 2`; the nested
card is offered sites beyond Ancient City; the Temple's site slots are
unavailable with "Temple of Ancestors: the card goes to your advisers".

- [ ] **Step 3: Give both hooks `byPower`**

In `src/main/scala/distantlands/model/CoreOperations.scala`, replace the
two hook definitions with:

```scala
/** Semantic window for a card played faceup, to a site or as a faceup
  * adviser. `byPower` is the power whose effect made the play (Search
  * Party, Temple of Ancestors, Oracle), `None` for an action's own play.
  * Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFaceup(card: WorldCardId,
    resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFaceup)
  override val children: Vector[Operation] = Vector.empty

/** Semantic window for a card (a denizen or a Vision) placed facedown as an
  * adviser by `player`. `byPower` is as for [[CardPlayedFaceup]]. A discard
  * emits neither hook. Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFacedown(card: WorldCardId, player: PlayerId,
    byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFacedown)
  override val children: Vector[Operation] = Vector.empty
```

Widen the seven patterns on them:

```bash
perl -pi -e 's/CardPlayedFaceup\((card(?:: \w+)?), _\)/CardPlayedFaceup($1, _, _)/; s/CardPlayedFacedown\(_, player\)/CardPlayedFacedown(_, player, _)/' src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPower.scala src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala src/main/scala/distantlands/gameplay/powers/cardplay/WildCry.scala src/main/scala/distantlands/gameplay/powers/cardplay/SaddleMakers.scala src/main/scala/distantlands/gameplay/powers/cardplay/Gossip.scala src/main/scala/distantlands/gameplay/powers/cardplay/WelcomingParty.scala src/main/scala/distantlands/gameplay/powers/cardplay/VisionPlay.scala
```

```bash
git grep -n "case CardPlayedFace" -- src
```

Expected: every match has three fields.

- [ ] **Step 4: Mark a power's play in `CardPlayProcedure`**

In `src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala`:

1. Give `PlacementTree` the field, after `card`:

   ```scala
     final class PlacementTree private[cardplay](val card: WorldCardId,
         /** The power whose effect makes this play, `None` for an
           * action's own play (catalog batch 10, slice 12). */
         val byPower: Option[PowerId],
         childrenAt: CardPlayRules => Vector[Operation])
         extends Operation:
   ```

2. Give `unchecked` the parameter and pass it on:

   ```scala
     def unchecked(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
         card: WorldCardId, origin: Origin,
         picked: Vector[CardId] = Vector.empty,
         byPower: Option[PowerId] = None): Operation =
       new PlacementTree(card, byPower, rules =>
         childrenFor(catalog, ready, actor, card, origin, rules, picked,
           byPower))
   ```

3. Add a `byPower: Option[PowerId]` parameter at the end of
   `childrenFor`, `played`, `settled` and `hook`, and pass it down:
   `childrenFor` hands it to `played(...)` and `settled(...)`; both pass
   it to `hook(...)`. In `hook`, build the hooks with it:

   ```scala
     private def hook(ready: ReadyGame, actor: PlayerId, card: WorldCardId,
         placement: SearchPlacement, byPower: Option[PowerId])
         : Vector[Operation] = placement match
       case SearchPlacement.Adviser(Orientation.FaceDown, _) =>
         Vector(CardPlayedFacedown(card, actor, byPower))
       case _ => CardPlay.playedSource(ready, actor, card, placement)
         .map(CardPlayedFaceup(card, _, byPower)).toVector
   ```

4. Add, after `unchecked`:

   ```scala
     /** Whether `operation` is a node of a card play a power makes: its
       * placement tree or its hook. No modifier picked for an action
       * applies there, since the play is not the action's (catalog batch
       * 10, ruling 5). */
     def playedByPower(operation: Operation): Boolean = operation match
       case tree: PlacementTree => tree.byPower.nonEmpty
       case CardPlayedFaceup(_, _, by) => by.nonEmpty
       case CardPlayedFacedown(_, _, by) => by.nonEmpty
       case _ => false
   ```

5. Extend the object's doc comment with one paragraph:

   ```scala
     * A power that plays a card itself (Search Party, Temple of Ancestors,
     * Oracle) names itself as `byPower`. The placement tree and both hooks
     * carry it, so no modifier picked for the enclosing action applies to
     * that play ([[playedByPower]]).
   ```

- [ ] **Step 5: Keep selected modifiers off a power's play**

In `src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala`,
add `import oathdigital.gameplay.actions.cardplay.CardPlayProcedure`, and
change the last case of `applicable` to:

```scala
      case None => !CardPlayProcedure.playedByPower(ctx.operation) &&
        appliesAt(ctx)
```

In the trait's doc comment, after the "At every other window it asks
`appliesAt`..." bullet, add:

```scala
  *  - At a node of a card play a power makes, such as Search Party's, no
  *    selected modifier applies: the play is not the action's (catalog
  *    batch 10, ruling 5, `CardPlayProcedure.playedByPower`).
```

- [ ] **Step 6: Mark each power's play**

1. `src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala`:
   in `play`, pass `byPower = Some(id)` after
   `ModifierPicks.cards(pending.answered)`. In the doc comment, after "The
   play's hook fires the card's own WHEN PLAYED and the card-play
   triggers.", add: "The play names Search Party as `byPower`, so no
   modifier picked for an enclosing Search applies to it (ruling 5)."
2. `src/main/scala/distantlands/gameplay/powers/action/TempleOfAncestors.scala`:
   pass `byPower = Some(id)` to its `CardPlayProcedure.unchecked` call,
   and limit only its own tree:

   ```scala
           case tree: CardPlayProcedure.PlacementTree
               if tree.byPower.contains(id) =>
   ```

   Change the doc's sentence "The power's own `Transform` at
   `SearchPlayAdviser`, applicable only inside this power's walk, limits
   the placements" to "The power's own `Transform` at `SearchPlayAdviser`
   limits the placements of its own play (`byPower`), so a Search Party
   it draws still plays its card as if searched,".
3. `src/main/scala/distantlands/gameplay/powers/action/Oracle.scala`: pass
   `byPower = Some(id)` to its `CardPlayProcedure.unchecked` call.

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.SearchParty*"`
Expected: PASS, both suites.

- [ ] **Step 7: Run the touched suites**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.* oathdigital.gameplay.CardPlayProcedureSuite oathdigital.gameplay.CardPlayHooksSuite oathdigital.gameplay.CardPlayRulesSuite oathdigital.gameplay.SearchSuite oathdigital.gameplay.SearchProcedureSuite oathdigital.gameplay.SearchModifierChoiceSuite oathdigital.gameplay.FacedownAdviserWalkerSuite"`
Expected: PASS, and `git diff src/test/resources` shows nothing new.

- [ ] **Step 8: Update the spec**

In `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`:

1. Change the status line to
   `**Status:** designed 2026-11-03. Slices 1 to 12 landed.`
2. Replace the body of "## Slice 12: Nested card play", from its first
   paragraph up to (not including) "## Slice 13: Askable Act action",
   with:

```markdown
A card played inside a WHEN PLAYED power already fires its own WHEN
PLAYED and the card-play triggers. The card-play hooks are windowed
composites, which never enter `WalkerHooks.gathered`, so a nested hook
folds as any other (plan-time probe, 2026-11-06). The walker is
unchanged. Two seams remain:

- `CardPlay.Origin` and `CardPlayProcedure.Origin` gain
  `DiscardPile(region)`. The card moves from the pile itself and never
  enters the temporary hand, which an enclosing Search re-reads on a
  one-card draw.
- The placement tree and both card-play hooks carry `byPower`, the power
  whose effect makes the play (Search Party, Temple of Ancestors, Oracle),
  `None` for an action's own play. `SelectedModifier` applies at no node
  of a power's play (ruling 5), so the outer Search's picks, such as
  Bracken, New Growth or Welcoming Party, leave it alone. The nested
  play's slots still protect the picked modifiers' cards, which are in
  use for the Search. Temple of Ancestors limits only its own play to the
  advisers, so a Search Party it draws plays its card as if searched,
  site included.

**Search Party.** WHEN PLAYED. The user picks a discard pile; an empty one
shows unavailable: "The pile is empty". The bottom 3 cards are peeked
(`Peek`), and the user must play one through `CardPlayProcedure` with
Search's placements: faceup to the site, faceup or facedown as an
adviser, or discarded to the next region's pile, which may be the pile it
came from. A Vision follows Search's rules and leaves the Visions Drawn
track alone. The rest never moved, so they stay at the bottom in order.
Persistent card-play rules apply as in a Search. Line: "Peeked at the
bottom of the Provinces discard pile." With every pile empty nothing is
asked: "Every discard pile was empty."

Tests: the peeks, the question and the rest left in order; the nested
play firing its own WHEN PLAYED and a card-play trigger; each placement,
including a discard back to its own pile; a Vision; empty piles; a
one-card Search keeping Search Party; Welcoming Party and New Growth
picked for the Search leaving the nested play alone; Temple of Ancestors
drawing Search Party; replay and the wire.
```

3. In "## Seams opened", change the row
   `| Nested WHEN PLAYED | 12 | Search Party |` to
   `| A power's card play: \`Origin.DiscardPile\`, \`byPower\` | 12 | Search Party |`.

- [ ] **Step 9: Update the ROADMAP**

In `docs/ROADMAP.md`, in the engine-blocked list, replace

```markdown
- **Unchanged from batch 8:** Search Party (recorded in the
  [catalog batch 4 rulings](superpowers/specs/2026-10-05-catalog-batch-4-rulings.md#moved-out-of-the-batch)),
  Master of Disguise (acting as another player's advisers), Tracker and
  False Prophet (a player-aware off-turn card play).
```

with

```markdown
- **Unchanged from batch 8:** Master of Disguise (acting as another
  player's advisers), Tracker and False Prophet (a player-aware off-turn
  card play).
```

- [ ] **Step 10: Run the gates**

```bash
python3 scripts/check-architecture.py
```

```bash
python3 scripts/check-markdown-links.py
```

Expected: both pass. Then run the full suite with coverage in the
background:

```bash
./sbtw clean coverage test coverageReport
```

Expected: every test passes, and statement coverage is at least 92%.

- [ ] **Step 11: Commit**

```bash
git add src/test/scala/distantlands/gameplay/powers/whenplayed/SearchPartyModifiersSuite.scala src/main/scala/distantlands/model/CoreOperations.scala src/main/scala/distantlands/gameplay/actions/cardplay/CardPlayProcedure.scala src/main/scala/distantlands/gameplay/powers/SelectedModifier.scala src/main/scala/distantlands/gameplay/powers/action/TempleOfAncestors.scala src/main/scala/distantlands/gameplay/powers/action/Oracle.scala src/main/scala/distantlands/gameplay/powers/whenplayed/SearchParty.scala src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPower.scala src/main/scala/distantlands/gameplay/powers/whenplayed/ConspiracyWhenPlayed.scala src/main/scala/distantlands/gameplay/powers/cardplay/WildCry.scala src/main/scala/distantlands/gameplay/powers/cardplay/SaddleMakers.scala src/main/scala/distantlands/gameplay/powers/cardplay/Gossip.scala src/main/scala/distantlands/gameplay/powers/cardplay/WelcomingParty.scala src/main/scala/distantlands/gameplay/powers/cardplay/VisionPlay.scala docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
fix(powers): no action's modifier reaches a power's card play

The placement tree and both card-play hooks name the power that made the
play, and a selected modifier applies at none of their nodes. Temple of
Ancestors limits only its own play, so a Search Party it draws plays as
if searched.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 2:** review Task 2 (spec slice 12, findings 3 and 4).
