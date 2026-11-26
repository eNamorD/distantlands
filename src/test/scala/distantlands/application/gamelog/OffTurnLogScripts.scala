package distantlands.application.gamelog

import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.powers.campaign.SneakAttackCard
import distantlands.gameplay.walker.FollowUpOffer
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer,
  ChooseOneAnswer}
import distantlands.testkit.{Answers, Dice, MenuPlay, Park, Step, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts.{atTable, raidAnswers}

/** Journaled walks by a player who is not the active player, for the Game
  * Log suites (catalog batch 8, slice 14). Kept beside `LogScripts`. */
object OffTurnLogScripts:
  /** p1 and p2 at Ancient City, which two bandits rule; p2 holds Sneak
    * Attack faceup. p3 stands at Buried Giant. */
  private def sneakAttackTable: Table =
    Table.start.pawn(p1, AncientCitySite).pawn(p2, AncientCitySite)
      .pawn(p3, BuriedGiantSite).bandits(AncientCitySite, 2)
      .adviser(p2, SneakAttackCard.id).banditsRefilled

  private def menuCampaign: Step = MenuPlay.start(p1, ActionRef.Campaign)

  /** No warband is sacrificed, so blank dice lose every Campaign. */
  private val noSacrifice: Answers = {
    case park if park.decisionId == CampaignIds.sacrifice =>
      ChooseAmountAnswer(0)
  }

  /** Every follow-up offer answered with `answer`, and no sacrifice. */
  private def everyOffer(answer: DecisionOptionRef): Answers = noSacrifice
    .orElse {
      case park if park.decisionId == FollowUpOffer.decisionId =>
        ChooseOneAnswer(answer)
    }

  /** p1 raids p2 at Ancient City and loses, so p2 stays there; p2 then
    * takes Sneak Attack and raids p1 there, off turn. */
  def sneakAttackTaken(using munit.Location): Script =
    val (service, act) = atTable("sneak-attack", sneakAttackTable,
      Dice.blanks)
    raidAnswers(act, everyOffer(FollowUpOffer.take)).after(menuCampaign)
    Script("sneak-attack", service, p1)

  /** The same, with p2 skipping the offer. */
  def sneakAttackSkipped(using munit.Location): Script =
    val (service, act) = atTable("sneak-attack-skipped", sneakAttackTable,
      Dice.blanks)
    raidAnswers(act, everyOffer(FollowUpOffer.skip)).after(menuCampaign)
    Script("sneak-attack-skipped", service, p1)
