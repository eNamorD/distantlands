package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.{ExtraProvisionsCard, ShieldWall}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.ParkedDecisionAssertions
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{Dice, Look, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

/** A Citizen defender uses the Chancellor's battle plan, paid by the
  * Chancellor, beside their own (Imperial Tactics, CR p. 35; Empire phase
  * design, "1b-ii"), played through the service. */
class ImperialTacticsScenarioSuite extends munit.FunSuite:
  private val gameId = "imperial-tactics"
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val extraProvisions: DecisionOptionRef =
    DecisionOptionRef.Denizen(ExtraProvisionsCard.id)
  private val parked = new ParkedDecisionAssertions(catalog,
    WalkerPowerCatalog.default(catalog))

  /** p1, an Exile, is in Act at Ancient City with 5 board warbands. p3, a
    * Citizen, stands there too with 2 board warbands, Extra Provisions
    * faceup and 1 favor. p2, the Chancellor, holds Shield Wall faceup and
    * 1 favor. Nobody holds the Oathkeeper title. */
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p1, Phase.Act).pawn(p3, at = "Ancient City")
    .boardWarbands(p1, 5).boardWarbands(p3, 2)
    .adviser(p2, ShieldWall.cardId).favor(p2, 1)
    .adviser(p3, ExtraProvisionsCard.id).favor(p3, 1)
    .oathkeeper(None)

  private def resolve(service: GameApplicationService, from: GameAccepted,
      player: PlayerId, id: String, answer: DecisionAnswer): GameAccepted =
    accepted(service.handle(gameId, from.nextSequence,
      GameCommand.ResolveWalker(player, TreeDecision(id, answer))))

  private def look(at: GameAccepted): Look = Look(Outcomes.ready(at.state))

  test("a Citizen defender uses the Chancellor's plan, paid by the " +
      "Chancellor"):
    val (service, _) = table.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    val forced = resolve(service, started, p1, CampaignIds.force,
      ChooseAmountAnswer(4))
    assertEquals(parked.parkedDecision(forced.state).map(facts =>
      (facts.decision, facts.awaiting)), Some((CampaignIds.defenderPlan, p3)))
    val defense = look(forced).diceInPool(CampaignIds.defensePool)
    val offered = accepted(service.handle(gameId, forced.nextSequence,
      GameCommand.SavePlanAid(p2, AidOffer(plans = Vector(shieldWall),
        amounts = AidAmounts(favor = 1)))))
    val shield = resolve(service, offered, p3, CampaignIds.defenderPlan,
      ChooseOneAnswer(shieldWall))
    val provisions = resolve(service, shield, p3, CampaignIds.defenderPlan,
      ChooseOneAnswer(extraProvisions))
    // Shield Wall adds 2 defense dice and Extra Provisions 1.
    assertEquals(look(provisions).diceInPool(CampaignIds.defensePool),
      defense + 3)
    assertEquals(look(provisions).favor(p2), 0)
    assertEquals(look(provisions).favor(p3), 0)
    // Nothing is left to choose, so the window closes and the Campaign
    // rolls; 4 swords beat p3's blank defense, and Shield Wall kills the
    // whole of its defeated user's force.
    val ended = resolve(service, provisions, p1, CampaignIds.sacrifice,
      ChooseAmountAnswer(0))
    assertEquals(look(ended).boardWarbands(p3), 0)
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.contains("p2 offered Shield Wall and 1 favor to pay."),
      lines)
