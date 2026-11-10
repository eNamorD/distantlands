package distantlands.application

import distantlands.gameplay.actions.ImperialLaws
import distantlands.gameplay.actions.minor.MinorActionProcedures
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathEvent.{ConsentAnswered, ConsentRequested}
import distantlands.protocol.projection._
import distantlands.testkit.{Commands, MenuPlay, Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, present}

/** Chancellor's Eye through the service: a Citizen asks the Chancellor
  * before moving warbands from their site to their board, and a grant
  * opens that way (Imperial slice 1a). */
class ChancellorsEyeConsentServiceSuite extends munit.FunSuite:
  import ConsentFixture.gameId
  private val projector = new GameProjector(catalog)
  private val peaks = BrokenPeaksSite.id
  private val direction = MinorActionProcedures.directionDecisionId
  private val board = MinorActionProcedures.toBoard

  /** p1 is the Chancellor and p2 a Citizen on their own turn at Broken
    * Peaks, which the Empire holds with 3 warbands. */
  private val citizenAtImperialSite: Table = Table.start.imperial(p1, p2)
    .turn(p2, Phase.Act).pawn(p2, at = peaks).boardWarbands(p2, 4)
    .warbandsAt(peaks, p2, 3)

  private val request = ConsentRequest(p2, direction, board,
    Vector(ConsentAsk(p1, Vector(ImperialLaws.chancellorsEye))),
    Vector(MinorActionProcedures.eyeKey(p2)))

  /** p2 starts Move Warbands, then asks the Chancellor to let them move
    * warbands to their board. */
  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.MoveWarbands)))
    accepted(service.handle(gameId, started.nextSequence,
      Commands.requestConsent(p2, direction, board)))

  private def boardRow(at: GameAccepted)(using munit.Location)
      : DecisionOptionProjection =
    present(projector.project(gameId, at.loaded, p2).walkerDecision
      .flatMap(_.query).toVector.flatMap(_.offeredOptions)
      .find(_.id == board.wireId), "the board row")

  test("a Citizen's request asks the Chancellor, naming Chancellor's Eye"):
    val (service, _) = citizenAtImperialSite.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    assertEquals(projector.project(gameId, asked.loaded, p1).consentPrompt
      .map(_.powers), Some(Vector("Chancellor's Eye")))

  test("Yes opens the way to the board, and the move asks how many"):
    val (service, _) = citizenAtImperialSite.service()
    val asked = asking(service)
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p1, ConsentChoice.Yes)))
    assertEquals(yes.events.head, ConsentAnswered(p1, granted = true))
    ParkedNode.of(yes.state, catalog) match
      case Right(Some(node)) =>
        assertEquals(node.decisionId, MinorActionProcedures.amountDecisionId)
      case other => fail(s"expected the amount decision, got $other")

  test("No leaves the way to the board unavailable, with the refusal"):
    val (service, _) = citizenAtImperialSite.service()
    val asked = asking(service)
    val no = accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p1, ConsentChoice.No)))
    assertEquals(no.events, Vector(ConsentAnswered(p1, granted = false)))
    val row = boardRow(no)
    assertEquals(row.state, "unavailable")
    assertEquals(row.reason, Some("p1 refused this turn."))
    assert(Outcomes.ready(no.state).game.current.walkerPending.nonEmpty)
