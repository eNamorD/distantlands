package oathdigital.application

import oathdigital.gameplay.cards.DeepWoodsSite
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.powers.travel.BrokenPeaksSite
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.testkit.MenuPlay
import oathdigital.gameplay.operations.{HallOfMinisters, HallOfMinistersCard}
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.model._
import oathdigital.model.OathEvent.{CommandAccepted, ConsentAnswered,
  ConsentRequested}
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted
import oathdigital.testkit.Commands

/** Asking the Hall of Ministers' ruler through the service (consent design,
  * slice 3): a grant covers the card, and Roving Terror takes it as p1's
  * own undoable step. */
class HallOfMinistersConsentServiceSuite extends munit.FunSuite:
  private val gameId = "hall-consent"
  private val terror = RovingTerrorCard.id
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val errandBoy = ErrandBoyCard.id
  private val errandBoyRef = DecisionOptionRef.Denizen(errandBoy)
  /** Errand Boy's slot at Deep Woods, Roving Terror's answer for it. */
  private val errandBoySlot = DecisionOptionRef.SiteSlot(woods, 0)
  /** Errand Boy at the Hall's site, which p2 rules; Wrestlers at Broken
    * Peaks, which the Hall does not protect, so the question is asked. */
  private val board = Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
    .denizen(terror, at = home).denizen(errandBoy, at = woods)
    .edifice(HallOfMinistersCard.id, EdificeSide.Intact,
      at = woods).warbandsAt(woods, p2, 1)
    .denizen(WrestlersCard.id,
      at = BrokenPeaksSite.id)
  private val useTerror = MenuPlay.starting(p1, ActionRef.UsePower(RovingTerror.id), Vector(DecisionOptionRef.Denizen(terror)))
  private val request = ConsentRequest(p1, RovingTerror.decisionId,
    errandBoySlot, Vector(ConsentAsk(p2, Vector(HallOfMinisters.id))),
    Vector(ConsentKey(HallOfMinisters.id, p1, HallOfMinisters.discardKey,
      errandBoyRef)))

  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val used = accepted(service.handle(gameId, 0L, useTerror))
    accepted(service.handle(gameId, used.nextSequence,
      Commands.requestConsent(p1, RovingTerror.decisionId,
        errandBoySlot)))

  test("Yes lets Roving Terror take the card, as p1's own step, and the " +
      "journal reads back the same"):
    val (service, repository) = board.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p2, ConsentChoice.Yes)))
    assertEquals(yes.events.head, ConsentAnswered(p2, true))
    assert(!yes.events.exists(_.isInstanceOf[CommandAccepted]))
    // The discard is p1's batch: it opens right after the answer.
    assertEquals(yes.undo.map(step => (step.issuer, step.from)),
      Some((p1, asked.nextSequence + 1L)))
    assert(Look(Outcomes.ready(yes.state)).discardAfter(woods)
      .contains(errandBoy))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo))), Right(Some((yes.state, yes.undo))))

  test("No leaves the card where it is, and p1 still chooses"):
    val (service, _) = board.service()
    val no = accepted(service.handle(gameId, asking(service).nextSequence,
      Commands.answerConsent(p2, ConsentChoice.No)))
    assertEquals(no.events, Vector(ConsentAnswered(p2, false)))
    assert(Outcomes.ready(no.state).game.current.walkerPending.nonEmpty)
    assert(Look(Outcomes.ready(no.state)).cardsAt(woods).contains(errandBoy))
