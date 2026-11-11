package distantlands.application

import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.whenplayed.{GatheringDeals, TheGathering,
  TheGatheringCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, OpenDeal}
import distantlands.protocol.projection.{DecisionQueryProjection,
  GameProjection, NegotiationDealProjection}
import distantlands.testkit.{Commands, MenuPlay, PlacementPicks, Situation,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** What each viewer of a Gathering is shown: the open question, and a
  * deal with a bystander (catalog batch 12, slice 6b). */
class GatheringProjectionSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  /** p1's Search plays The Gathering at Ancient City; p2 and p3 come.
    * Parked at the open question. */
  private def atOpen(): Situation =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).pawn(p3, BuriedGiantSite)
      .worldDeckTop(TheGatheringCard).banditsRefilled
    val (service, repository) = table.service()
    table.situation(Situation.journaled(service, catalog, repository,
      "gathering")).withAnswers {
      case park if park.decisionId ==
          CardPlayProcedure.placementDecisionId(TheGatheringCard.id) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, PlayedAs.AtSite))
      case park if park.decisionId == TheGathering.pawnDecisionId =>
        ChooseOneAnswer(TheGathering.join)
    }.parkedAt(GatheringDeals.openDecisionId)(
      MenuPlay.start(p1, ActionRef.Search))

  private def view(situation: Situation, viewer: PlayerId): GameProjection =
    projector.project("gathering", LoadedGame(situation.state,
      situation.nextSequence), viewer)

  private def openQuestion(projection: GameProjection)
      : DecisionQueryProjection.Gathering =
    projection.walkerDecision.flatMap(_.query).collect {
      case gathering: DecisionQueryProjection.Gathering => gathering
    }.getOrElse(fail("the open question must be projected"))

  private def deal(projection: GameProjection): NegotiationDealProjection =
    projection.walkerDecision.flatMap(_.query).collect {
      case negotiate: DecisionQueryProjection.Negotiate => negotiate.deal
    }.getOrElse(fail("the deal must be projected"))

  test("each player at the site is asked the open question with their own " +
      "reach"):
    val parked = atOpen()
    Vector(p1 -> Vector("p2", "p3"), p2 -> Vector("p1", "p3"),
      p3 -> Vector("p1", "p2")).foreach { (viewer, reach) =>
      val question = openQuestion(view(parked, viewer))
      assertEquals(question.presentPlayerIds, Vector("p1", "p2", "p3"))
      assertEquals(question.reachablePlayerIds, reach, viewer.value)
      assertEquals(question.passedPlayerIds, Vector.empty)
    }

  test("a bystander is asked the deal with no editor, and its parties " +
      "with one"):
    val dealt = atOpen().parkedAfter(Commands.resolveWalker(p1,
      TreeDecision(GatheringDeals.openDecisionId, OpenDeal(Vector(p2)))))
    val watching = deal(view(dealt, p3))
    assertEquals(watching.participantPlayerIds, Vector("p1", "p2"))
    assertEquals(watching.bystanderPlayerIds, Vector("p3"))
    assertEquals(watching.editing, None)
    assert(deal(view(dealt, p2)).editing.nonEmpty)
