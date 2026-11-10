package distantlands.application.gamelog

import distantlands.application.{GameApplicationService, TreeDecision}
import distantlands.gameplay.actions.campaign.{CampaignIds, PlanAid}
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.campaign.ShieldWall
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.{Commands, Dice, MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}
import LogScripts.{formatter, texts}

/** What the Game Log says about Imperial Tactics offers (Empire phase
  * design, "1b-ii", "Log"). p1, an Exile, raids p3, a Citizen, at Ancient
  * City; p2, the Chancellor at Broken Peaks, holds Shield Wall, the Darkest
  * Secret and 2 favor. */
class GameLogPlanAidSuite extends munit.FunSuite:
  private val gameId = "plan-aid-log"
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val table: Table = Table.start.imperial(p2, p3)
    .turn(p1, Phase.Act).pawn(p3, at = AncientCitySite)
    .boardWarbands(p1, 5).boardWarbands(p3, 2).favor(p2, 2)
    .adviser(p2, ShieldWall.cardId).darkestSecret(Some(p2), 1)
    .oathkeeper(None)

  private def log(service: GameApplicationService, viewer: PlayerId)
      : Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(viewer)))

  private def offering(offers: AidOffer*): GameApplicationService =
    val (service, _) = table.service(campaignDice = Dice.raid)
    val started = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, ActionRef.Campaign)))
    val parked = accepted(service.handle(gameId, started.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(CampaignIds.force,
        ChooseAmountAnswer(4)))))
    offers.foldLeft(parked)((at, offer) => accepted(service.handle(gameId,
      at.nextSequence, Commands.savePlanAid(p2, offer))))
    service

  private val full = AidOffer(plans = Vector(shieldWall),
    requirements = Vector(PlanAid.darkestSecret),
    amounts = AidAmounts(favor = 2))
  private val withoutShieldWall = full.copy(plans = Vector.empty)

  private def aidLines(lines: Vector[String]) =
    lines.filter(_.startsWith("p2 "))

  test("an offer names what it added"):
    assertEquals(aidLines(log(offering(full), p1)), Vector(
      "p2 offered Shield Wall, the Darkest Secret and 2 favor to pay."))

  test("a smaller offer names what it took back"):
    assertEquals(aidLines(log(offering(full, withoutShieldWall), p1)).last,
      "p2 withdrew Shield Wall.")

  test("a changed offer names both"):
    val changed = AidOffer(requirements = Vector(DecisionOptionRef.Player(p2)),
      amounts = AidAmounts(favor = 1))
    assertEquals(aidLines(log(offering(full, changed), p1)).last,
      "p2 offered their pawn at Broken Peaks, and withdrew Shield Wall, " +
        "the Darkest Secret and 1 favor to pay.")

  test("an unchanged save writes nothing, and every viewer sees the line"):
    val service = offering(full, full)
    Vector(p1, p2, p3).foreach(viewer =>
      assertEquals(aidLines(log(service, viewer)), Vector(
        "p2 offered Shield Wall, the Darkest Secret and 2 favor to pay.")))
