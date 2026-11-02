package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.walker.FollowUpOffer
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{Answers, CatalogNames, Dice, Look, MenuPlay, Park,
  Situation}
import oathdigital.testkit.Table.{p1, p2}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** The Raid scripts: the plain Raid the goldens read, the same under
  * Careful Plans, and the Raid run as a Campaign Knights Errant offered, and
  * the Raid answers every other Campaign script leans on. Reached through
  * `LogScripts`.
  */
object RaidLogScripts:
  /** A Raid on p2, whose pawn shares p1's site and who holds a facedown
    * adviser to lose. Every board warband goes into the force and every
    * survivor is sacrificed, so the attack is twice the force against the
    * defender's board warbands and blank dice: the attacker wins. No battle
    * plan is chosen. */
  def raid(using munit.Location): Script =
    val (service, act) = atTable("raid", tableWithOnePawnPerSite
      .pawn(p2, "Ancient City")
      .adviser(p2, "Birdsong", facedown = true).banditsRefilled, Dice.raid)
    raidAnswers(act).after(MenuPlay.start(p1, ActionRef.Campaign))
    Script("raid", service, p1)

  /** The `raid` table with Careful Plans faceup as p1's adviser and one
    * favor to pay for it. p1 chooses it, so p2's defense dice roll before
    * p1's attack dice. Everything else is answered as in `raid`. */
  def raidUnderCarefulPlans(using munit.Location): Script =
    val careful = CatalogNames.denizen("Careful Plans")
    val (service, act) = atTable("careful-raid", tableWithOnePawnPerSite
      .pawn(p2, "Ancient City")
      .adviser(p2, "Birdsong", facedown = true)
      .adviser(p1, careful).favor(p1, 1).banditsRefilled, Dice.raid)
    // Chosen once: the favor it places marks it used.
    raidAnswers(act, {
      case park if park.decisionId == CampaignIds.attackerPlan &&
          Look(park.ready).adviserTokens(p1, careful) == Tokens(0, 0) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(careful))
    }).after(MenuPlay.start(p1, ActionRef.Campaign))
    Script("careful-raid", service, p1)

  /** The `raid` table with a Campaign offered to p1 by Knights Errant, which
    * p1 holds faceup: p1 takes the offer and the Raid runs as that queued
    * Campaign. */
  def queuedRaid(using munit.Location): Script =
    val (service, act) = offeredRaidTable("queued-raid", Vector.empty)
    raidAnswers(act).after(takeOffer)
    Script("queued-raid", service, p1)

  /** The same with a second Campaign behind the first: p1 takes the first,
    * which runs, and skips the second. */
  def queuedRaidThenSkip(using munit.Location): Script =
    val (service, act) = offeredRaidTable("queued-raid-skip",
      Vector(knightsOffer))
    raidAnswers(act).withAnswers {
      case park if park.decisionId == FollowUpOffer.decisionId =>
        ChooseOneAnswer(FollowUpOffer.skip)
    }.after(takeOffer)
    Script("queued-raid-skip", service, p1)

  private def knightsOffer: QueuedProcedure = QueuedProcedure(p1,
    ActionRef.Campaign, Vector.empty,
    Some(PowerSourceRef.Card(CatalogNames.denizen("Knights Errant"))),
    QueueKind.Offer)

  private def takeOffer: GameCommand = GameCommand.ResolveWalker(p1,
    TreeDecision(FollowUpOffer.decisionId, ChooseOneAnswer(FollowUpOffer.take)))

  /** The `raid` table with Knights Errant held by p1, a Campaign offer
    * parked for p1 and `behind` queued after it. */
  private def offeredRaidTable(name: String, behind: Vector[QueuedProcedure])(
      using munit.Location): (GameApplicationService, Situation) =
    atTable(name, tableWithOnePawnPerSite
      .pawn(p2, "Ancient City")
      .adviser(p2, "Birdsong", facedown = true)
      .adviser(p1, CatalogNames.denizen("Knights Errant"))
      .update(_.updateCurrent(_.copy(queue = behind)))
      .offering(knightsOffer)
      .banditsRefilled, Dice.raid)

  /** A Raid's answers: all force committed and sacrificed, and Finish at
    * every battle plan window unless `plans` answers it first. */
  def raidAnswers(act: Situation,
      plans: Answers = PartialFunction.empty): Situation =
    act.withAnswers(plans.orElse {
      case park if park.decisionId == CampaignIds.kind =>
        ChooseOneAnswer(DecisionOptionRef.Button("raid"))
      case Park(Decide(CampaignIds.force, _,
          amount: DecisionQuery.ChooseAmount, _, _, _, _), _, _, _) =>
        ChooseAmountAnswer(amount.max)
      case Park(Decide(CampaignIds.sacrifice, _,
          amount: DecisionQuery.ChooseAmount, _, _, _, _), _, _, _) =>
        ChooseAmountAnswer(amount.max)
      case park if park.decisionId == CampaignIds.attackerPlan ||
          park.decisionId == CampaignIds.defenderPlan =>
        ChooseOneAnswer(CampaignIds.finish)
    })
