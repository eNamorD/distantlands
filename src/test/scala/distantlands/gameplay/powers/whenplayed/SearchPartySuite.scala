package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.SearchPlay
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.cardplay.{GossipCard, VowOfObedienceCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look, MenuPlay,
  PlacementPicks, RecordedOps, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p2
import distantlands.testkit.Unwrap.accepted

/** Search Party (card 240, catalog batch 10, slice 12), WHEN PLAYED: "peek
  * at the bottom 3 cards of a discard pile and play one as if you
  * searched. Return the rest." Most tests play Search Party from p1's
  * facedown advisers, so no Search draw adds cards to a pile. */
class SearchPartySuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchFixture.{after, rules}
  import SearchPartyFixture._

  private def searchPartyLines(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(SearchParty.id, SearchParty.noteKeys, events)

  test("it peeks at the bottom 3 of the chosen pile and asks which to " +
      "play; the rest stay in order, and the line names the pile"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      plainHearth*))
    val played = playedFaceupFromFacedown(ready)
    assertEquals(EconomyActions.parkedDecision(played.state).decisionId,
      SearchParty.pileDecisionId)
    val piled = pilePicked(played, Region.Hinterland)
    val asked = EconomyActions.parkedDecision(piled.state)
    assertEquals(asked.decisionId, SearchParty.cardDecisionId)
    assertEquals(WhenPlayedHarness.chooseOneOptions(asked).map(_.ref),
      plainHearth.take(3).map(PlacementPicks.optionNaming))
    val peeked = after(piled)
    plainHearth.take(3).foreach(card => assert(Look(peeked).knowsAdviser(actor,
      card), s"p1 should know $card"))
    assert(!Look(peeked).knowsAdviser(actor, plainHearth(3)))
    plainHearth.foreach(card => assert(!Look(peeked).knowsAdviser(p2, card)))
    val done = placedAs(cardPicked(piled, plainHearth(1)), plainHearth(1),
      PlayedAs.Faceup)
    val end = after(done)
    assertEquals(Look(end).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plainHearth(1)))
    assertEquals(Look(end).discardPile(Region.Hinterland),
      Vector[WorldCardId](plainHearth(0), plainHearth(2), plainHearth(3)))
    assertEquals(searchPartyLines(done.events), Vector(NoteText.Said(NoteKey.Used,
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
      .discarded(Region.Hinterland, plainHearth(0)))
    val before = Look(ready).favor(p2)
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, plainHearth(0)), plainHearth(0), PlayedAs.Facedown)
    assertEquals(Look(after(done)).favor(p2), before + 1)
    assertEquals(Look(after(done)).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plainHearth(0)))

  test("the card may go to p1's site"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      plainHearth(0)))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, plainHearth(0)), plainHearth(0), PlayedAs.AtSite)
    assert(Look(after(done)).cardsAt(AncientCitySite).contains(plainHearth(0)))
    assertEquals(Look(after(done)).discardPile(Region.Hinterland),
      Vector.empty[WorldCardId])

  test("discarded, the card goes on top of the next region's pile, even " +
      "the pile it came from"):
    // p1 is in the Cradle, so the discard goes to the Provinces pile.
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
      plainHearth.take(3)*))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, plainHearth(0)), plainHearth(0), PlayedAs.Discarded)
    assertEquals(Look(after(done)).discardPile(Region.Provinces),
      Vector[WorldCardId](plainHearth(1), plainHearth(2), plainHearth(0)))

  test("discarded, the card's discard names Search Party"):
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
      plainHearth.take(3)*))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, plainHearth(0)), plainHearth(0), PlayedAs.Discarded)
    assertEquals(RecordedOps.all(done.events).collect {
      case discard: Discard.Denizen if discard.card == plainHearth(0) =>
        discard.cause }, Vector(Some(DiscardCause(SearchParty.id))))

  test("a Vision is revealed as from a Search, and the Visions Drawn " +
      "track stays"):
    val faith = VisionRules.Faith
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Hinterland,
      faith))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, faith), faith, PlayedAs.Faceup)
    assertEquals(Look(after(done)).player(actor).revealedVisions.map(_.id),
      Vector(faith))
    assertEquals(after(done).game.current.tracks.visionsDrawn,
      ready.game.current.tracks.visionsDrawn)

  test("a Conspiracy it plays faceup leaves the game from the pile"):
    // p2 stands away from p1 and no banner has a holder: nothing to seize.
    val conspiracy = VisionRules.Conspiracy
    val ready = p1HoldsSearchPartyFacedown(_.pawn(p2, BrokenPeaksSite)
      .discarded(Region.Hinterland, conspiracy))
    val done = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, conspiracy), conspiracy, PlayedAs.Faceup)
    assertEquals(Look(after(done)).discardPile(Region.Hinterland),
      Vector.empty[WorldCardId])
    assert(!accepted(CardIndex.from(after(done).game)).ids.contains(conspiracy))

  test("a faceup Vow of Obedience forbids playing the pile's Vision faceup"):
    val faith = VisionRules.Faith
    val ready = p1HoldsSearchPartyFacedown(_.adviser(actor, VowOfObedienceCard)
      .discarded(Region.Hinterland, faith))
    val asked = peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Hinterland, faith)
    val offered = PlacementPicks.offered(PlacementPicks.parkedOptions(
      asked.state, WalkerPowerCatalog.default(catalog)))
    assert(!offered.contains(PlayedAs.Faceup), clue(offered))
    assert(offered.contains(PlayedAs.Facedown), clue(offered))

  test("an empty pile shows unavailable"):
    val played = playedFaceupFromFacedown(p1HoldsSearchPartyFacedown(
      _.discarded(Region.Hinterland, plainHearth(0))))
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
    assertEquals(searchPartyLines(done.events), Vector(NoteText.Said("used.none",
      "Every discard pile was empty.", covers = false)))

  test("kept from a one-card Search, it plays its card from the pile and " +
      "the Search still keeps Search Party; replay and the wire agree"):
    val ready = Table.start.pawn(actor, AncientCitySite)
      .discarded(Region.Cradle, SearchPartyCard)
      .discarded(Region.Hinterland, plainHearth.take(2)*).ready
    val searched = accepted(SearchPlay.searched(rules, Ready(ready), actor,
      SearchSource.RegionalDiscard(Region.Cradle)))
    val kept = placedAs(searched, SearchPartyCard.id, PlayedAs.Faceup)
    val done = placedAs(peekedAndPicked(kept, Region.Hinterland, plainHearth(1)),
      plainHearth(1), PlayedAs.Faceup)
    val end = after(done)
    assertEquals(Look(end).advisers(actor),
      Vector[CardId](SearchPartyCard.id, plainHearth(1)))
    assert(end.game.current.temporaryHands.getOrElse(actor, Vector.empty)
      .isEmpty)
    assertEquals(Look(end).discardPile(Region.Hinterland),
      Vector[WorldCardId](plainHearth(0)))
    assertEquals(MenuPlay.procedureBeyondMenu(end), None)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), end)
    assertWireRoundTrips(done.events)
