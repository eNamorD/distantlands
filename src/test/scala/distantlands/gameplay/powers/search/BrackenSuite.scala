package distantlands.gameplay.powers.search

import distantlands.catalog.ModifierMark
import distantlands.gameplay.{ModifierPlay, SearchPlay}
import distantlands.gameplay.actions.search.SearchProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Bracken (card 197), a selected Search modifier for 1 secret: "You may
  * put all of the cards you discard on the top or bottom of any one
  * discard pile (even your region's)." (catalog batch 10, slice 9). */
class BrackenSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._
  import SearchFixture.rules

  private val bracken = BrackenCard.id

  /** A card already in the Hinterland pile, so its top and bottom differ. */
  private val inHinterland = SearchFixture.discordCards(6)

  /** Bracken at Ancient City, p1's site, p1 with 1 faceup secret to pay for
    * it, and one card in the Hinterland pile. */
  private def besideBracken(table: Table): Table =
    table.denizen(bracken, at = AncientCitySite).secrets(actor, faceUp = 1)
      .discarded(Region.Hinterland, inHinterland)

  /** p1 picks Bracken, then a Search starts. */
  private def brackenPicked(ready: ReadyGame): OathTransition =
    accepted(ModifierPlay.started(rules, Ready(ready), ActionRef.Search,
      actor, PowerWindow.SearchModifierSelection, Vector(Bracken.id)))

  /** `picked`, with Bracken's questions answered `pile` and `end`, the
    * Search started and the world deck picked. The events are every
    * command's. */
  private def searchedSending(picked: OathTransition, pile: Region,
      end: DecisionOptionRef.Button): OathTransition = (for
    piled <- rules.resolveWalker(picked.state, actor, Bracken.pileDecision,
      ChooseOneAnswer(Bracken.pileRef(pile)))
    ended <- rules.resolveWalker(piled.state, actor,
      Bracken.positionDecision, ChooseOneAnswer(end))
    started <- ModifierPlay.picking(rules, ended, actor,
      PowerWindow.SearchModifierSelection, Vector.empty)
    sourced <- rules.resolveWalker(started.state, actor,
      SearchProcedure.sourceDecisionId,
      SearchPlay.source(SearchSource.WorldDeck))
  // `picking` returns events that begin with `ended`'s, so `ended`'s are
  // not added again.
  yield sourced.copy(events = picked.events ++ piled.events ++
    started.events ++ sourced.events)).fold(
    error => throw new AssertionError(error.toString), identity)

  private def pile(transition: OathTransition, region: Region)
      : Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(region)

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(Bracken.id, Bracken.noteKeys, events)

  test("it carries the Search icon"):
    assertEquals(BrackenCard.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Search)))

  test("picked, it pays its secret onto the card and asks a pile, then an " +
      "end, before the Search starts"):
    val picked = brackenPicked(searchAtAncientCity(besideBracken))
    assertEquals(Look(SearchFixture.after(picked)).tokensOn(bracken),
      Tokens(0, 1))
    assertEquals(EconomyActions.parkedDecision(picked.state).decisionId,
      Bracken.pileDecision)
    val piled = accepted(rules.resolveWalker(picked.state, actor,
      Bracken.pileDecision, ChooseOneAnswer(Bracken.pileRef(Region.Cradle))))
    assertEquals(EconomyActions.parkedDecision(piled.state).decisionId,
      Bracken.positionDecision)

  test("to the bottom of the Hinterland pile: the cards not kept go there, " +
      "the last one discarded lowest, and its line says so once"):
    val done = keptFacedown(searchedSending(brackenPicked(
      searchAtAncientCity(besideBracken)), Region.Hinterland, Bracken.bottom))
    assertEquals(pile(done, Region.Hinterland),
      unkept.reverse :+ inHinterland)
    assert(!pile(done, Region.Provinces).exists(unkept.contains))
    assertEquals(said(done.events), Vector(NoteText.Said("discards-bottom",
      "This Search's discards go to the bottom of the Hinterland discard " +
        "pile.", covers = false)))

  test("to the top of p1's own region's pile, the Cradle's"):
    val done = keptFacedown(searchedSending(brackenPicked(
      searchAtAncientCity(besideBracken)), Region.Cradle, Bracken.top))
    assertEquals(pile(done, Region.Cradle).takeRight(2), unkept)
    assertEquals(said(done.events).map(_.key), Vector("discards-top"))

  test("an adviser bumped over the limit goes to the chosen end"):
    val done = keptFaceupBumpingAnAdviser(searchedSending(brackenPicked(
      searchAtAncientCity(table => withThreeFacedownAdvisers(
        besideBracken(table)))), Region.Hinterland, Bracken.bottom))
    // The cards not kept went to the bottom first, then the bumped adviser.
    assertEquals(pile(done, Region.Hinterland),
      (bumped +: unkept.reverse) :+ inHinterland)

  test("the People's Favor's site discard goes to the chosen end"):
    val done = keptAtSiteReplacingTheSiteCard(searchedSending(brackenPicked(
      searchAtAncientCity(table => withPeoplesFavorAndSiteCard(
        besideBracken(table)))), Region.Hinterland, Bracken.bottom))
    // The cards not kept went to the bottom first, then the site card.
    assertEquals(pile(done, Region.Hinterland),
      (siteCard +: unkept.reverse) :+ inHinterland)

  test("a Search with Bracken replays and round-trips the wire"):
    val ready = searchAtAncientCity(besideBracken)
    val done = keptFacedown(searchedSending(brackenPicked(ready),
      Region.Hinterland, Bracken.bottom))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      SearchFixture.after(done))
    assertWireRoundTrips(done.events)
