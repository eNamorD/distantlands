package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.gameplay.actions.minor.WarbandTransfers
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.GrandScepterAction
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{MenuPlay, Table}
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
      .imperial(p1, p2).pawn(p1, at = "Broken Peaks")
      .pawn(p2, at = "Broken Peaks").pawn(p3, at = "Buried Giant")
      .boardWarbands(p1, 5).boardWarbands(p2, 2).banditsRefilled)
    def answer(decisionId: String, value: DecisionAnswer) =
      GameCommand.ResolveWalker(p1, TreeDecision(decisionId, value))
    act.parkedAfter(MenuPlay.start(p1, ActionRef.TransferWarbands))
      .parkedAfter(answer(WarbandTransfers.partnerDecisionId,
        ChooseOneAnswer(DecisionOptionRef.Player(p2))))
      .parkedAfter(answer(WarbandTransfers.directionDecisionId,
        ChooseOneAnswer(WarbandTransfers.give)))
      .parkedAfter(answer(WarbandTransfers.amountDecisionId,
        ChooseAmountAnswer(3)))
      .parkedAfter(GameCommand.RequestConsent(p1,
        WarbandTransfers.confirmDecisionId,
        WarbandTransfers.confirmation(WarbandTransfers.give, 3)))
      .after(GameCommand.AnswerConsent(p2, ConsentChoice.Yes))
    Script("warbands-transferred", service, p1)

  /** p2 is the Chancellor and p1 a Citizen with 2 board warbands and 5
    * favor. p1 exiles themself, paying the Chancellor 3 favor. */
  def selfExiled(using munit.Location): Script =
    val (service, act) = atTable("self-exiled", Table.start
      .imperial(p2, p1).roundStarter(p1).pawn(p1, at = "Broken Peaks")
      .boardWarbands(p1, 2).favor(p1, 5).banditsRefilled)
    act.after(MenuPlay.start(p1, ActionRef.SelfExile))
    Script("self-exiled", service, p1)

  /** p2 is the Chancellor; p1, an Exile, holds the Grand Scepter and uses
    * its Action to become a Citizen. */
  def scepterCitizen(using munit.Location): Script =
    val (service, act) = atTable("scepter-citizen", Table.start
      .imperial(p2).roundStarter(p1).pawn(p1, at = "Broken Peaks")
      .relic(p1, "The Grand Scepter").boardWarbands(p1, 2)
      .allowingCitizenshipPowers.banditsRefilled)
    act.after(MenuPlay.start(p1, ActionRef.UsePower(GrandScepterAction.id),
      Vector(DecisionOptionRef.Relic(TheGrandScepterCard.id))))
    Script("scepter-citizen", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("warbands-transferred" -> (() => warbandsTransferred),
      "self-exiled" -> (() => selfExiled),
      "scepter-citizen" -> (() => scepterCitizen))
