package oathdigital.application

import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.campaign.ShieldWall
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.protocol.projection._
import oathdigital.testkit.{Commands, Dice, MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** What an Imperial ally is shown of another Imperial player's open plan
  * window (Empire phase design, "Offer panel"). p1, an Exile, raids p3, a
  * Citizen, at Ancient City; p2, the Chancellor at Broken Peaks, holds
  * Shield Wall faceup, the Darkest Secret, 2 favor and 1 secret. */
class PlanAidProjectionSuite extends munit.FunSuite:
  private val gameId = "plan-aid-view"
  private val projector = new GameProjector(catalog)
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p1, Phase.Act).pawn(p3, at = AncientCitySite)
    .boardWarbands(p1, 5).boardWarbands(p3, 2).favor(p2, 2)
    .adviser(p2, ShieldWall.cardId).darkestSecret(Some(p2), 1)
    .oathkeeper(Some(p3))

  private def view(at: GameAccepted, viewer: PlayerId): GameProjection =
    projector.project(gameId, at.loaded, viewer)

  private def keys(rows: Vector[DecisionOptionProjection]): Vector[String] =
    rows.map(row => s"${row.kind}:${row.id}")

  /** p1 raids p3 with 4 warbands, up to p3's plan decision. */
  private def parkedAtCitizenPlans(build: Table = table)
      : (GameApplicationService, GameAccepted) =
    val (service, _) = build.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    (service, accepted(service.handle(gameId, started.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(CampaignIds.force,
        ChooseAmountAnswer(4))))))

  test("an ally sees what they may offer and what they hold"):
    val (_, at) = parkedAtCitizenPlans()
    val prompt = view(at, p2).planAidPrompt.get
    assertEquals(prompt.userPlayerId, "p3")
    assertEquals(prompt.side, "defender")
    assertEquals(keys(prompt.plans), Vector(s"denizen:${ShieldWall.cardId.value}"))
    assert(prompt.plans.forall(_.state == "available"), prompt.plans)
    assertEquals(keys(prompt.requirements),
      Vector("banner:darkest-secret", "player:p2"))
    assertEquals((prompt.favorHeld, prompt.secretsHeld), (2, 1))
    assertEquals(prompt.saved, PlanAidOfferProjection())

  test("a saved offer and its used part are shown"):
    val (service, at) = parkedAtCitizenPlans()
    val offer = AidOffer(plans = Vector(shieldWall),
      amounts = AidAmounts(favor = 1))
    val saved = accepted(service.handle(gameId, at.nextSequence,
      Commands.savePlanAid(p2, offer)))
    val picked = accepted(service.handle(gameId, saved.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(CampaignIds.defenderPlan,
        ChooseOneAnswer(shieldWall)))))
    val prompt = view(picked, p2).planAidPrompt.get
    val key = s"denizen:${ShieldWall.cardId.value}"
    assertEquals(prompt.saved, PlanAidOfferProjection(Vector(key),
      favor = 1))
    assertEquals(prompt.used, PlanAidOfferProjection(Vector(key),
      favor = 1))
    // The user chose it, so their window lists it no longer; the panel
    // still shows it, to lock it.
    assertEquals(keys(prompt.plans), Vector(key))

  test("a Chancellor holding the title may offer its defense"):
    val (_, at) = parkedAtCitizenPlans(table.oathkeeper(Some(p2)))
    assert(keys(view(at, p2).planAidPrompt.get.plans)
      .contains("button:title"))

  test("the user, an Exile and a bystander outside any window see no prompt"):
    val (_, at) = parkedAtCitizenPlans()
    assertEquals(view(at, p3).planAidPrompt, None)
    assertEquals(view(at, p1).planAidPrompt, None)
    val (_, started) = {
      val (service, _) = table.service(campaignDice = Dice.raid)
      (service, accepted(service.handle(gameId, 0L,
        MenuPlay.starting(p1, ActionRef.Campaign))))
    }
    assertEquals(view(started, p2).planAidPrompt, None)

  test("a facedown adviser is never among the plans"):
    val (_, at) = parkedAtCitizenPlans(table
      .adviser(p2, ShieldWall.cardId, facedown = true))
    assertEquals(view(at, p2).planAidPrompt.map(_.plans), Some(Vector.empty))
