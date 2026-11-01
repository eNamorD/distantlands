package oathdigital.application

import oathdigital.gameplay.actions.campaign.{CampaignIds, CampaignProcedure,
  PlanAid}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.powers.campaign.ShieldWall
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker,
  WalkerPowers}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{Dice, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Imperial Tactics offers through the service (Empire phase design,
  * "1b-ii"): an ally's offer is a standing fact, saved over the user's
  * parked plan decision, opening no command batch. */
class PlanAidServiceSuite extends munit.FunSuite:
  private val gameId = "plan-aid"
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)
  private val powers = WalkerPowerCatalog.default(catalog)
  private val parked = new ParkedDecisionAssertions(catalog, powers)

  /** p1, an Exile, is in Act at Ancient City with p3, a Citizen, who has 2
    * board warbands and 1 favor; p2, the Chancellor, holds Shield Wall
    * faceup and 2 favor. Nobody holds the Oathkeeper title. */
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p1, Phase.Act).pawn(p3, at = "Ancient City")
    .boardWarbands(p1, 5).boardWarbands(p3, 2).favor(p2, 2)
    .adviser(p2, ShieldWall.cardId).oathkeeper(None)

  private def resolve(service: GameApplicationService, from: GameAccepted,
      player: PlayerId, id: String, answer: DecisionAnswer): GameAccepted =
    accepted(service.handle(gameId, from.nextSequence,
      GameCommand.ResolveWalker(player, TreeDecision(id, answer))))

  private def awaiting(at: GameAccepted): Option[(String, PlayerId)] =
    parked.parkedDecision(at.state).map(facts =>
      (facts.decision, facts.awaiting))

  /** p1 raids p3 with 4 warbands, up to p3's plan decision. */
  private def toCitizenDefenderPlans(service: GameApplicationService)
      : GameAccepted =
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    val raid = awaiting(started) match
      case Some((CampaignIds.kind, _)) => resolve(service, started, p1,
        CampaignIds.kind, ChooseOneAnswer(DecisionOptionRef.Button("raid")))
      case _ => started
    val forced = resolve(service, raid, p1, CampaignIds.force,
      ChooseAmountAnswer(4))
    assertEquals(awaiting(forced), Some((CampaignIds.defenderPlan, p3)))
    forced

  private def parkedQuery(at: GameAccepted): DecisionQuery =
    val ready = Outcomes.ready(at.state)
    val tree = CampaignProcedure.rebuild(catalog, ready, p1, Vector.empty)
      .toOption.get
    ProcedureWalker.openDecisions(ready, tree,
      ready.game.current.walkerPending.get,
      WalkerPowers.selected(powers, Vector.empty)).head.query

  private def defenderOptions(at: GameAccepted): Vector[DecisionOptionRef] =
    parkedQuery(at) match
      case one: DecisionQuery.ChooseOne =>
        one.options.filter(_.available).map(_.ref)
      case _ => Vector.empty

  /** Answers whatever the Campaign asks next, with its first option or its
    * least amount, until it parks nothing of the Campaign's. */
  @annotation.tailrec
  private def toCampaignEnd(service: GameApplicationService,
      at: GameAccepted, seen: Vector[GameAccepted] = Vector.empty)
      : Vector[GameAccepted] = parked.parkedDecision(at.state)
      .filter(_.procedure == ActionRef.Campaign) match
    case None => seen :+ at
    case Some(facts) =>
      val (id, who) = (facts.decision, facts.awaiting)
      val answer = parkedQuery(at) match
        case one: DecisionQuery.ChooseOne =>
          ChooseOneAnswer(one.options.find(_.available).get.ref)
        case amount: DecisionQuery.ChooseAmount =>
          ChooseAmountAnswer(amount.min)
        case other => fail(s"no answer for $other")
      toCampaignEnd(service, resolve(service, at, who, id, answer),
        seen :+ at)

  private def save(service: GameApplicationService, at: GameAccepted,
      ally: PlayerId, offer: AidOffer) =
    service.handle(gameId, at.nextSequence, GameCommand.SavePlanAid(ally,
      offer))

  private def refusedFor(result: Either[GameApplicationError, GameAccepted])
      (using munit.Location): Unit = result match
    case Left(GameApplicationError.CommandRejected(
        _: OathViolation.PlanAidUnavailable)) => ()
    case other => fail(s"expected an unavailable offer, got $other")

  private val shieldWallAndFavor = AidOffer(plans = Vector(shieldWall),
    amounts = AidAmounts(favor = 1))

  test("an ally's offer is saved, opens no batch, and makes the plan " +
      "choosable"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val at = toCitizenDefenderPlans(service)
    assert(!defenderOptions(at).contains(shieldWall))
    val saved = accepted(save(service, at, p2, shieldWallAndFavor))
    assertEquals(saved.events, Vector(OathEvent.PlanAidSaved(p2, defending,
      shieldWallAndFavor)))
    assert(defenderOptions(saved).contains(shieldWall))
    assertEquals(saved.undo, at.undo)

  test("the user is not their own ally"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    refusedFor(save(service, toCitizenDefenderPlans(service), p3, AidOffer()))

  test("an Exile cannot offer"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    refusedFor(save(service, toCitizenDefenderPlans(service), p1, AidOffer()))

  test("an ally cannot offer a plan they do not lend, or more than they " +
      "hold"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val at = toCitizenDefenderPlans(service)
    refusedFor(save(service, at, p2, AidOffer(plans =
      Vector(DecisionOptionRef.Denizen(DenizenId("999"))))))
    refusedFor(save(service, at, p2, AidOffer(requirements =
      Vector(PlanAid.darkestSecret))))
    refusedFor(save(service, at, p2, AidOffer(amounts =
      AidAmounts(favor = 3))))

  test("an offer cannot drop what has been used"):
    // p3 holds the title, so the window stays open after Shield Wall.
    val (service, _) = table.oathkeeper(Some(p3))
      .service(campaignDice = Dice.raid)
    val saved = accepted(save(service, toCitizenDefenderPlans(service), p2,
      shieldWallAndFavor))
    val picked = resolve(service, saved, p3, CampaignIds.defenderPlan,
      ChooseOneAnswer(shieldWall))
    refusedFor(save(service, picked, p2, AidOffer()))
    refusedFor(save(service, picked, p2, AidOffer(plans = Vector(shieldWall))))
    accepted(save(service, picked, p2, shieldWallAndFavor))

  test("the offers clear when the Campaign's walk ends"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val saved = accepted(save(service, toCitizenDefenderPlans(service), p2,
      shieldWallAndFavor))
    val finished = resolve(service, saved, p3, CampaignIds.defenderPlan,
      ChooseOneAnswer(CampaignIds.finish))
    // The offers stand while the walk goes on, and clear when it ends.
    assertEquals(Outcomes.ready(finished.state).planAid.window,
      Some(defending))
    val steps = toCampaignEnd(service, finished)
    val ended = steps.last
    assert(steps.flatMap(_.events).contains(OathEvent.PlanAidCleared))
    assertEquals(Outcomes.ready(ended.state).planAid, PlanAidState())

  test("an offer survives the user's undo and a reload"):
    // p3 holds the title, so picking Shield Wall rolls nothing yet.
    val (service, repository) = table.oathkeeper(Some(p3))
      .service(campaignDice = Dice.raid)
    val saved = accepted(save(service, toCitizenDefenderPlans(service), p2,
      shieldWallAndFavor))
    val picked = resolve(service, saved, p3, CampaignIds.defenderPlan,
      ChooseOneAnswer(shieldWall))
    assertEquals(PlanAid.remaining(Outcomes.ready(picked.state), defending,
      p2), AidAmounts.none)
    val undone = accepted(service.handle(gameId, picked.nextSequence,
      GameCommand.Undo(p3)))
    val ready = Outcomes.ready(undone.state)
    assertEquals(ready.planAid.offerOf(p2), shieldWallAndFavor)
    assertEquals(PlanAid.remaining(ready, defending, p2),
      AidAmounts(favor = 1))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, undone.state)

  test("no offer is accepted when no Imperial plan decision is parked"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    refusedFor(save(service, started, p2, AidOffer()))
