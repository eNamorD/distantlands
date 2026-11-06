package oathdigital.gameplay

import oathdigital.application._
import oathdigital.gameplay.actions.ImperialLaws
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.ActOffers
import oathdigital.gameplay.actions.minor.WarbandTransfers
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathEvent.{ConsentAnswered, ConsentRequested}
import oathdigital.protocol.projection._
import oathdigital.testkit.{Commands, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Warband Transfers (CR p. 35): an Imperial player moves warbands to or
  * from another Imperial player's board, at their site and with their
  * consent (Imperial slice 1a). */
class WarbandTransfersSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val gameId = "warband-transfers"
  private val projector = new GameProjector(catalog)
  private val partner = DecisionOptionRef.Player(p2)

  /** p1 is the Chancellor and p2 a Citizen, both at Broken Peaks on p1's
    * turn; p1's board holds 5 warbands and p2's 2. */
  private def imperialsTogether: Table = Table.start.imperial(p1, p2)
    .turn(p1, Phase.Act).pawn(p1, at = BrokenPeaksSite)
    .pawn(p2, at = BrokenPeaksSite).boardWarbands(p1, 5).boardWarbands(p2, 2)

  private val giveThree = WarbandTransfers.confirmation(WarbandTransfers.give,
    3)

  private val request = ConsentRequest(p1, WarbandTransfers.confirmDecisionId,
    giveThree, Vector(ConsentAsk(p2, Vector(ImperialLaws.warbandTransfers))),
    Vector(WarbandTransfers.confirmKey(p1, WarbandTransfers.give, 3)))

  private def answer(service: GameApplicationService, at: GameAccepted,
      decisionId: String, value: DecisionAnswer)(using munit.Location)
      : GameAccepted = accepted(service.handle(gameId, at.nextSequence,
    Commands.resolveWalker(p1, TreeDecision(decisionId, value))))

  /** p1 starts the transfer and picks p2 as the partner. */
  private def withPartner(service: GameApplicationService)
      (using munit.Location): GameAccepted =
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.TransferWarbands)))
    answer(service, started, WarbandTransfers.partnerDecisionId,
      ChooseOneAnswer(partner))

  /** p1 picks p2, then `way` and `count`, and stops at the confirm button. */
  private def confirming(service: GameApplicationService,
      way: DecisionOptionRef.Button, count: Int)(using munit.Location)
      : GameAccepted =
    val picked = answer(service, withPartner(service),
      WarbandTransfers.directionDecisionId, ChooseOneAnswer(way))
    answer(service, picked, WarbandTransfers.amountDecisionId,
      ChooseAmountAnswer(count))

  /** p1 confirms giving 3, which asks p2 to let them. */
  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val confirm = confirming(service, WarbandTransfers.give, 3)
    accepted(service.handle(gameId, confirm.nextSequence,
      Commands.requestConsent(p1, WarbandTransfers.confirmDecisionId,
        giveThree)))

  private def row(at: GameAccepted, ref: DecisionOptionRef)
      (using munit.Location): DecisionOptionProjection =
    present(projector.project(gameId, at.loaded, p1).walkerDecision
      .flatMap(_.query).toVector.flatMap(_.offeredOptions)
      .find(_.id == ref.wireId), s"the ${ref.wireId} row")

  private def granted(service: GameApplicationService, at: GameAccepted)
      (using munit.Location): GameAccepted = accepted(service.handle(gameId,
    at.nextSequence, Commands.answerConsent(p2, ConsentChoice.Yes)))

  private def boardWarbands(at: GameAccepted, player: PlayerId): Int =
    Outcomes.ready(at.state).game.current.player(player)
      .map(_.board.warbands).getOrElse(-1)

  test("an Exile cannot start a transfer"):
    val table = imperialsTogether.turn(p3, Phase.Act)
      .pawn(p3, at = BrokenPeaksSite)
    assertEquals(rules.startWalker(table.state, ActionRef.TransferWarbands, p3)
      .left.map(ActOffers.reason(ActionRef.TransferWarbands, _)),
      Left("Only an Imperial player transfers warbands"))

  test("an altered foundation refuses the transfer as it refuses a move"):
    val altered = imperialsTogether.update(ready => ready.updateCampaign(
      campaign => campaign.copy(foundations = campaign.foundations.map {
        case (number, foundation) =>
          number -> foundation.copy(face = FoundationFace.Altered)
      })))
    val refusal = rules.startWalker(altered.state, ActionRef.MoveWarbands, p1)
    assertEquals(refusal.left.toOption, Some(OathViolation.MinorActionUnavailable(
      "minor actions are limited to the fixed unaltered foundations")))
    assertEquals(rules.startWalker(altered.state, ActionRef.TransferWarbands,
      p1).left.toOption, refusal.left.toOption)

  test("with no Imperial at the site the transfer is unavailable"):
    val table = imperialsTogether.pawn(p2, at = AncientCitySite)
    assert(rules.startWalker(table.state, ActionRef.TransferWarbands, p1)
      .isLeft)

  test("the confirm button waits on the partner's consent"):
    val (service, _) = imperialsTogether.service()
    val confirm = confirming(service, WarbandTransfers.give, 3)
    assertEquals(row(confirm, giveThree).state, "askable")
    assertEquals(row(confirm, giveThree).label, "Give 3 warbands")

  test("confirming asks the partner about that exact transfer"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    assertEquals(projector.project(gameId, asked.loaded, p2).consentPrompt
      .map(_.powers), Some(Vector("Warband Transfers")))

  test("a refusal leaves the confirm button unavailable, with the refusal"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    val no = accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p2, ConsentChoice.No)))
    assertEquals(no.events, Vector(ConsentAnswered(p2, granted = false)))
    assertEquals(row(no, giveThree).state, "unavailable")
    assertEquals(row(no, giveThree).reason, Some("p2 refused this turn."))

  test("a refusal covers only the transfer it was asked about"):
    assertNotEquals(WarbandTransfers.confirmKey(p1, WarbandTransfers.give, 3),
      WarbandTransfers.confirmKey(p1, WarbandTransfers.give, 2))
    assertNotEquals(WarbandTransfers.confirmKey(p1, WarbandTransfers.give, 3),
      WarbandTransfers.confirmKey(p1, WarbandTransfers.take, 3))

  test("once the partner consents, the warbands move"):
    val (service, _) = imperialsTogether.service()
    val done = granted(service, asking(service))
    assertEquals(boardWarbands(done, p1), 2)
    assertEquals(boardWarbands(done, p2), 5)

  test("taking moves warbands from the partner's board to the actor's"):
    val (service, _) = imperialsTogether.service()
    val confirm = confirming(service, WarbandTransfers.take, 2)
    val asked = accepted(service.handle(gameId, confirm.nextSequence,
      Commands.requestConsent(p1, WarbandTransfers.confirmDecisionId,
        WarbandTransfers.confirmation(WarbandTransfers.take, 2))))
    val done = granted(service, asked)
    assertEquals(boardWarbands(done, p1), 7)
    assertEquals(boardWarbands(done, p2), 0)

  test("an amount outside what the giving board holds is rejected"):
    val (service, _) = imperialsTogether.service()
    val gave = answer(service, withPartner(service),
      WarbandTransfers.directionDecisionId,
      ChooseOneAnswer(WarbandTransfers.give))
    Vector(0, 6).foreach: amount =>
      assert(service.handle(gameId, gave.nextSequence,
        Commands.resolveWalker(p1, TreeDecision(
          WarbandTransfers.amountDecisionId, ChooseAmountAnswer(amount))))
        .isLeft, s"amount $amount was accepted")

  test("a way whose giving board is empty is disabled with its reason"):
    val (service, _) = imperialsTogether.boardWarbands(p2, 0).service()
    val taking = row(withPartner(service), WarbandTransfers.take)
    assertEquals(taking.state, "unavailable")
    assertEquals(taking.reason, Some("No warbands on their board"))

  test("a confirm button reads back as its way and amount"):
    assertEquals(WarbandTransfers.confirmed(giveThree),
      Some(WarbandTransfers.give -> 3))
    assertEquals(WarbandTransfers.confirmed(DecisionOptionRef.Button("give")),
      None)
