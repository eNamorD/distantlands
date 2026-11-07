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
