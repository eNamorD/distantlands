package distantlands.application

import distantlands.application.gamelog.LogScriptSupport
import distantlands.gameplay.phases.wake.QuestFirstWakeProcedure
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{QuestToDistantLands,
  QuestToDistantLandsCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Dice, MenuPlay, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** A player who placed no pawn in Setup stands at no site until their first
  * Wake (R33): every other rule that reads a pawn's site finds none for
  * them, and the projection and the log carry them without one. l2, a
  * Citizen, holds Quest to Distant Lands and stays off; p1 Wakes, Travels,
  * Musters and Rests before p2's first Wake opens. */
class PawnlessPlayerSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    QuestToDistantLandsCard.id -> true)
  private val stayingOff: Answers =
    case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
      ChooseOneAnswer(QuestToDistantLands.stayOff)
  private val projector = new GameProjector(catalog)

  /** p1's Act, through the production service, with p2 pawnless. */
  private def acting(name: String)(using munit.Location)
      : (GameApplicationService, Situation) =
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    service -> Situation.wake(Situation.journaled(service, catalog,
      repository, name).withAnswers(stayingOff), record,
      LaterGameFixture.orders(record, p1)).endingWake(p1)

  /** p1's whole turn played through the production service with p2
    * pawnless, parked at p2's first-Wake placement. */
  private def played(name: String)(using munit.Location)
      : (GameApplicationService, Situation) =
    val (service, woken) = acting(name)
    assertEquals(woken.ready.game.current.pawnSite(p2), None)
    val traveled = woken.after(MenuPlay.start(p1, ActionRef.Travel))
    assertNotEquals(traveled.ready.game.current.pawnSite(p1),
      woken.ready.game.current.pawnSite(p1))
    val acted = traveled.after(MenuPlay.start(p1, ActionRef.Muster))
    assertEquals(acted.ready.game.current.pawnSite(p2), None)
    service -> acted.parkedAt(QuestFirstWakeProcedure.placeDecisionId(p2))(
      MenuPlay.start(p1, PhaseTransitionRef.BeginRest))

  test("p1's Wake, Travel, Muster and Rest run with p2 pawnless, and p2's first " +
      "Wake opens with the placement"):
    val (_, at) = played("pawnless-turn")
    assertEquals(at.ready.game.current.turn.activePlayer, p2)
    assertEquals(at.ready.game.current.pawnSite(p2), None)

  /** The options p1's Act menu offers, the ending aside. */
  private def offered(situation: Situation)(using munit.Location)
      : Vector[DecisionOptionRef] =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "the Act menu") match
      case ParkedNode.Decision(_, decide, _) => decide.query match
        case one: DecisionQuery.ChooseOne => one.options.filter(_.available)
          .map(_.ref).filterNot(_ == DecisionOptionRef.Button("end-act"))
        case other => fail(s"expected the Act menu, got $other")
      case other => fail(s"expected the Act menu, parked on $other")

  test("every option p1's Act menu offers runs while p2, pawnless, holds a " +
      "Ties hand, and leaves that hand alone"):
    val (_, act) = acting("pawnless-menu")
    val hand = act.ready.game.current.temporaryHands(p2)
    assertEquals(hand.size, 3)
    val options = offered(act)
    assert(options.contains(DecisionOptionRef.Button("search")), options)
    options.zipWithIndex.foreach { (option, index) =>
      val (_, fresh) = acting(s"pawnless-menu-$index")
      val after = fresh.after(MenuPlay.picking(p1, option))
      assertEquals(after.ready.game.current.temporaryHands.get(p2),
        Some(hand), option)
      val placing = after.parkedAt(QuestFirstWakeProcedure.placeDecisionId(
        p2))(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
      assertEquals(placing.ready.game.current.temporaryHands.get(p2),
        Some(hand), option)
    }

  test("the rules that read another player's site do not find p2"):
    val (_, at) = played("pawnless-sites")
    val ready = at.ready
    assert(!PawnMoves.atOtherSites(ready, p1).contains(p2))
    ready.game.current.map.inPlay.foreach(site =>
      assert(!ready.game.current.players.exists(player =>
        player.player == p2 && player.pawnSite.contains(site)), site))

  test("the projection and the log carry p2 with no pawn, for every " +
      "viewer"):
    val (service, at) = played("pawnless-views")
    val loaded = LoadedGame(at.state, at.nextSequence)
    Vector(p1, p2, PlayerId("p3")).foreach(viewer =>
      projector.project("pawnless-views", loaded, viewer))
    val steps = present(accepted(service.history("pawnless-views")),
      "the history").steps
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(LogScriptSupport.formatter.format(steps, viewer).nonEmpty,
        viewer))
