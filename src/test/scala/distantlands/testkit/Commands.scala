package distantlands.testkit

import distantlands.application.{GameCommand, PlayerIntent, TreeDecision}
import distantlands.model._

/** One `GameCommand.Issued` per `PlayerIntent`, with the actor first, so a
  * test reads `Commands.undo(p1)` where production code reads
  * `Issued(actor, intent)`. `GameCommand.Begin` needs no helper. */
object Commands:
  def undo(actor: PlayerId): GameCommand =
    GameCommand.Issued(actor, PlayerIntent.Undo)
  def redo(actor: PlayerId): GameCommand =
    GameCommand.Issued(actor, PlayerIntent.Redo)
  def resolveWalker(actor: PlayerId, treeDecision: TreeDecision): GameCommand =
    GameCommand.Issued(actor, PlayerIntent.ResolveWalker(treeDecision))
  def setConsentPreference(granter: PlayerId, power: PowerId,
      requester: PlayerId, setting: ConsentSetting): GameCommand =
    GameCommand.Issued(granter,
      PlayerIntent.SetConsentPreference(power, requester, setting))
  def requestConsent(requester: PlayerId, decisionId: String,
      target: DecisionOptionRef,
      granters: Vector[PlayerId] = Vector.empty): GameCommand =
    GameCommand.Issued(requester,
      PlayerIntent.RequestConsent(decisionId, target, granters))
  def answerConsent(granter: PlayerId, choice: ConsentChoice): GameCommand =
    GameCommand.Issued(granter, PlayerIntent.AnswerConsent(choice))
  def savePlanAid(ally: PlayerId, offer: AidOffer): GameCommand =
    GameCommand.Issued(ally, PlayerIntent.SavePlanAid(offer))
