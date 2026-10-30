package oathdigital.gameplay

import oathdigital.application._
import oathdigital.gameplay.actions.ImperialLaws
import oathdigital.gameplay.phases.ActOffers
import oathdigital.gameplay.actions.minor.WarbandTransfers
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathEvent.{ConsentAnswered, ConsentRequested}
import oathdigital.protocol.projection._
import oathdigital.testkit.{MenuPlay, Outcomes, ParkedNode, Table}
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
    .turn(p1, Phase.Act).pawn(p1, at = "Broken Peaks")
    .pawn(p2, at = "Broken Peaks").boardWarbands(p1, 5).boardWarbands(p2, 2)

  private val request = ConsentRequest(p1, WarbandTransfers.partnerDecisionId,
    partner, Vector(ConsentAsk(p2, Vector(ImperialLaws.warbandTransfers))),
    Vector(WarbandTransfers.partnerKey(p1, p2)))

  /** p1 starts the transfer, then asks p2 to let them. */
  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.TransferWarbands)))
    accepted(service.handle(gameId, started.nextSequence,
      GameCommand.RequestConsent(p1, WarbandTransfers.partnerDecisionId,
        partner)))

  private def partnerRow(at: GameAccepted)(using munit.Location)
      : DecisionOptionProjection =
    present(projector.project(gameId, at.loaded, p1).walkerDecision
      .flatMap(_.query).toVector.flatMap(_.offeredOptions)
      .find(_.id == partner.wireId), "the partner row")

  private def answer(service: GameApplicationService, at: GameAccepted,
      decisionId: String, value: DecisionAnswer)(using munit.Location)
      : GameAccepted = accepted(service.handle(gameId, at.nextSequence,
    GameCommand.ResolveWalker(p1, TreeDecision(decisionId, value))))

  private def boardWarbands(at: GameAccepted, player: PlayerId): Int =
    Outcomes.ready(at.state).game.current.players.find(_.player == player)
      .map(_.board.warbands).getOrElse(-1)

  test("an Exile cannot start a transfer"):
    val table = imperialsTogether.turn(p3, Phase.Act)
      .pawn(p3, at = "Broken Peaks")
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
    val table = imperialsTogether.pawn(p2, at = "Ancient City")
    assert(rules.startWalker(table.state, ActionRef.TransferWarbands, p1)
      .isLeft)

  test("the partner must consent before the transfer goes on"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    assertEquals(projector.project(gameId, asked.loaded, p2).consentPrompt
      .map(_.powers), Some(Vector("Warband Transfers")))

  test("a refusal leaves the partner unavailable, with the refusal"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    val no = accepted(service.handle(gameId, asked.nextSequence,
      GameCommand.AnswerConsent(p2, ConsentChoice.No)))
    assertEquals(no.events, Vector(ConsentAnswered(p2, granted = false)))
    assertEquals(partnerRow(no).state, "unavailable")
    assertEquals(partnerRow(no).reason, Some("p2 refused this turn."))

  test("once the partner consented, warbands move between the boards"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      GameCommand.AnswerConsent(p2, ConsentChoice.Yes)))
    // The grant picks the partner, so the walk asks which way next.
    ParkedNode.of(yes.state, catalog) match
      case Right(Some(node)) =>
        assertEquals(node.decisionId, WarbandTransfers.directionDecisionId)
      case other => fail(s"expected the direction decision, got $other")
    val gave = answer(service, yes, WarbandTransfers.directionDecisionId,
      ChooseOneAnswer(WarbandTransfers.give))
    ParkedNode.of(gave.state, catalog) match
      case Right(Some(node)) =>
        assertEquals(node.decisionId, WarbandTransfers.amountDecisionId)
      case other => fail(s"expected the amount decision, got $other")
    val done = answer(service, gave, WarbandTransfers.amountDecisionId,
      ChooseAmountAnswer(3))
    assertEquals(boardWarbands(done, p1), 2)
    assertEquals(boardWarbands(done, p2), 5)

  test("taking moves warbands from the partner's board to the actor's"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      GameCommand.AnswerConsent(p2, ConsentChoice.Yes)))
    val took = answer(service, yes, WarbandTransfers.directionDecisionId,
      ChooseOneAnswer(WarbandTransfers.take))
    val done = answer(service, took, WarbandTransfers.amountDecisionId,
      ChooseAmountAnswer(2))
    assertEquals(boardWarbands(done, p1), 7)
    assertEquals(boardWarbands(done, p2), 0)

  test("an amount outside what the giving board holds is rejected"):
    val (service, _) = imperialsTogether.service()
    val asked = asking(service)
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      GameCommand.AnswerConsent(p2, ConsentChoice.Yes)))
    val gave = answer(service, yes, WarbandTransfers.directionDecisionId,
      ChooseOneAnswer(WarbandTransfers.give))
    Vector(0, 6).foreach: amount =>
      assert(service.handle(gameId, gave.nextSequence,
        GameCommand.ResolveWalker(p1, TreeDecision(
          WarbandTransfers.amountDecisionId, ChooseAmountAnswer(amount))))
        .isLeft, s"amount $amount was accepted")

  test("a way whose giving board is empty is disabled with its reason"):
    val (service, _) = imperialsTogether.boardWarbands(p2, 0).service()
    val asked = asking(service)
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      GameCommand.AnswerConsent(p2, ConsentChoice.Yes)))
    val taking = present(projector.project(gameId, yes.loaded, p1)
      .walkerDecision.flatMap(_.query).toVector.flatMap(_.offeredOptions)
      .find(_.id == WarbandTransfers.take.wireId), "the take row")
    assertEquals(taking.state, "unavailable")
    assertEquals(taking.reason, Some("No warbands on their board"))
