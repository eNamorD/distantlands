package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.gameplay.actions.minor.WarbandTransfers
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScripts.atTable

/** The Empire's rules (Imperial slice 1a). Kept beside `LogScripts`, which
  * is at its size limit. */
object ImperialLogScripts:
  /** p1 is the Chancellor and p2 a Citizen, both at Broken Peaks, with 5
    * warbands on p1's board. p1 asks p2 to let them transfer, p2 agrees, and
    * p1 gives 3. */
  def warbandsTransferred(using munit.Location): Script =
    val (service, act) = atTable("warbands-transferred", Table.start
      .imperial(p1, p2).pawn(p1, at = "Broken Peaks")
      .pawn(p2, at = "Broken Peaks").pawn(p3, at = "Buried Giant")
      .boardWarbands(p1, 5).boardWarbands(p2, 2).banditsRefilled)
    def answer(decisionId: String, value: DecisionAnswer) =
      GameCommand.ResolveWalker(p1, TreeDecision(decisionId, value))
    act.parkedAfter(MenuPlay.start(p1, ActionRef.TransferWarbands))
      .parkedAfter(GameCommand.RequestConsent(p1,
        WarbandTransfers.partnerDecisionId, DecisionOptionRef.Player(p2)))
      .parkedAfter(GameCommand.AnswerConsent(p2, ConsentChoice.Yes))
      .parkedAfter(answer(WarbandTransfers.directionDecisionId,
        ChooseOneAnswer(WarbandTransfers.give)))
      .after(answer(WarbandTransfers.amountDecisionId, ChooseAmountAnswer(3)))
    Script("warbands-transferred", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("warbands-transferred" -> (() => warbandsTransferred))
