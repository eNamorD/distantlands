package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.actions.minor.WarbandTransfers
import oathdigital.gameplay.actions.negotiation.NegotiationDeal
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.GrandScepterAction
import oathdigital.gameplay.powers.campaign.GrandScepterPlan
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  ChooseOneAnswer, ProposeTerms}
import oathdigital.testkit.{Commands, Dice, MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScripts.atTable

/** The Empire's rules (Imperial slice 1a). Kept beside `LogScripts`, which
  * is at its size limit. */
object ImperialLogScripts:
  /** p1 is the Chancellor and p2 a Citizen, both at Broken Peaks, with 5
    * warbands on p1's board. p1 picks p2, chooses to give 3, asks p2 to let
    * them, and p2 agrees. */
  def warbandsTransferred(using munit.Location): Script =
    val (service, act) = atTable("warbands-transferred", Table.start
      .imperial(p1, p2).pawn(p1, at = BrokenPeaksSite)
      .pawn(p2, at = BrokenPeaksSite).pawn(p3, at = BuriedGiantSite)
      .boardWarbands(p1, 5).boardWarbands(p2, 2).banditsRefilled)
    def answer(decisionId: String, value: DecisionAnswer) =
      Commands.resolveWalker(p1, TreeDecision(decisionId, value))
    act.parkedAfter(MenuPlay.start(p1, ActionRef.TransferWarbands))
      .parkedAfter(answer(WarbandTransfers.partnerDecisionId,
        ChooseOneAnswer(DecisionOptionRef.Player(p2))))
      .parkedAfter(answer(WarbandTransfers.directionDecisionId,
        ChooseOneAnswer(WarbandTransfers.give)))
      .parkedAfter(answer(WarbandTransfers.amountDecisionId,
        ChooseAmountAnswer(3)))
      .parkedAfter(Commands.requestConsent(p1,
        WarbandTransfers.confirmDecisionId,
        WarbandTransfers.confirmation(WarbandTransfers.give, 3)))
      .after(Commands.answerConsent(p2, ConsentChoice.Yes))
    Script("warbands-transferred", service, p1)

  /** p2 is the Chancellor and p1 a Citizen with 2 board warbands and 5
    * favor. p1 exiles themself, paying the Chancellor 3 favor. */
  def selfExiled(using munit.Location): Script =
    val (service, act) = atTable("self-exiled", Table.start
      .imperial(p2, p1).roundStarter(p1).pawn(p1, at = BrokenPeaksSite)
      .boardWarbands(p1, 2).favor(p1, 5).banditsRefilled)
    act.after(MenuPlay.start(p1, ActionRef.SelfExile))
    Script("self-exiled", service, p1)

  /** p2 is the Chancellor; p1, an Exile, holds the Grand Scepter and uses
    * its Action to become a Citizen. */
  def scepterCitizen(using munit.Location): Script =
    val (service, act) = atTable("scepter-citizen", Table.start
      .imperial(p2).roundStarter(p1).pawn(p1, at = BrokenPeaksSite)
      .relic(p1, TheGrandScepterCard).boardWarbands(p1, 2)
      .allowingCitizenshipPowers.banditsRefilled)
    act.after(MenuPlay.start(p1, ActionRef.UsePower(GrandScepterAction.id),
      Vector(DecisionOptionRef.Relic(TheGrandScepterCard.id))))
    Script("scepter-citizen", service, p1)

  private def deal(by: PlayerId, answer: DecisionAnswer): GameCommand =
    Commands.resolveWalker(by, TreeDecision(NegotiationDeal.dealDecisionId,
      answer))

  /** p1, the Chancellor, holds the Grand Scepter at Ancient City beside p2,
    * an Exile with 2 board warbands and 2 at Broken Peaks, and offers p2
    * Citizenship in a deal both accept (Empire slice 3b). */
  def dealCitizen(using munit.Location): Script =
    val (service, act) = atTable("deal-citizen", Table.start
      .imperial(p1).pawn(p1, at = AncientCitySite).pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BuriedGiantSite).relic(p1, TheGrandScepterCard)
      .boardWarbands(p2, 2).warbandsAt(BrokenPeaksSite, p2, 2)
      .allowingCitizenshipPowers.banditsRefilled)
    act.parkedAfter(MenuPlay.start(p1, ActionRef.Negotiation))
      .parkedAfter(deal(p1, ProposeTerms(NegotiationTerms(
        citizenship = Vector(p2)))))
      .parkedAfter(deal(p2, AcceptDeal))
      .after(deal(p1, AcceptDeal))
    Script("deal-citizen", service, p1)

  /** p1, the Chancellor, raids p2, a Citizen, at Ancient City, with the
    * Grand Scepter's plan and 3 favor to burn, wins and exiles p2 (Empire
    * slice 3b). */
  def scepterExiled(using munit.Location): Script =
    val (service, act) = atTable("scepter-exiled", Table.start
      .imperial(p1, p2).pawn(p1, at = AncientCitySite)
      .pawn(p2, at = AncientCitySite).pawn(p3, at = BuriedGiantSite)
      .relic(p1, TheGrandScepterCard).favor(p1, 3)
      .boardWarbands(p1, 4).boardWarbands(p2, 2).banditsRefilled, Dice.raid)
    val plan = GrandScepterPlan.cardRef
    LogScripts.raidAnswers(act, {
      case park if park.decisionId == CampaignIds.attackerPlan &&
          (park.decide.query match
            case one: DecisionQuery.ChooseOne =>
              one.options.exists(o => o.ref == plan && o.available)
            case _ => false) =>
        ChooseOneAnswer(plan)
      case park if park.decisionId == GrandScepterPlan.decisionId =>
        ChooseOneAnswer(GrandScepterPlan.exile)
    }).after(MenuPlay.start(p1, ActionRef.Campaign))
    Script("scepter-exiled", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("warbands-transferred" -> (() => warbandsTransferred),
      "self-exiled" -> (() => selfExiled),
      "scepter-citizen" -> (() => scepterCitizen),
      "deal-citizen" -> (() => dealCitizen),
      "scepter-exiled" -> (() => scepterExiled))
