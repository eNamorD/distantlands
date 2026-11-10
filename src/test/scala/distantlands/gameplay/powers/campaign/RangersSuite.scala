package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.WalkerDiceFixture.skullThenSwords
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.Look

/** Rangers: an attacker's plan, a favor placed, that ignores every skull and
  * adds two attack dice when the defense pool holds four or more dice. */
class RangersSuite extends munit.FunSuite:
  private val card = RangersCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)

  private def attackDice(ops: Vector[CoreOperation]): Vector[Int] =
    ops.collect { case ModifyDicePool(CampaignIds.attackPool, delta, _) =>
      delta }

  /** p1 holds Rangers faceup and one favor. */
  private def attacker(extras: Int): Board =
    val base = withAdviser(board(extras = extras, warbands = 5, supply = 7),
      card, Orientation.FaceUp)
    on(base)(_.favor(base.actor, 1))

  test("against four or more defense dice it adds two attack dice, places " +
      "a favor and ignores every skull"):
    val b = attacker(extras = 2)
    // The origin prints two defense dice and every site prints at least one,
    // so three targets roll at least four.
    val run = commit(rules(skullThenSwords), b, 2,
      targets = b.extras.map(DecisionOptionRef.Site(_)))
    val picked = run.pick(b.actor, CampaignIds.attackerPlan, ref)
    assertEquals(attackDice(picked.since(run)), Vector(2))
    assertEquals(Look(picked.state).adviserTokens(b.actor, card), Tokens(1, 0))
    val done = picked.finishPlanWindows
    assertEquals(done.result.skullLosses, 0)
    assertEquals(NoteText.said(Rangers.id, Rangers.noteKeys, done.events),
      Vector(NoteText.Said("ignored", "Skulls ignored.", covers = false)))

  test("against fewer defense dice it is offered for the skulls alone"):
    val b = attacker(extras = 0)
    val run = commit(rules(skullThenSwords), b, 2)
    val picked = run.pick(b.actor, CampaignIds.attackerPlan, ref)
    assertEquals(attackDice(picked.since(run)), Vector.empty)
    assertEquals(picked.finishPlanWindows.result.skullLosses, 0)
