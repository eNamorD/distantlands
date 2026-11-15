package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture.{Board, board, on, rules}
import distantlands.gameplay.actions.campaign.{CampaignIds, PlanAmounts}
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.legacies.LegacyActionFixture
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.gameplay.walker.WalkerDice
import distantlands.model._
import distantlands.testkit.{Journal, RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Keeper of Order (L11): an Imperial's battle plan against an Imperial,
  * ±2 attack dice, and an attacker's skulls ignored (Legacy in-play powers
  * rulings 5 and 16; implementation rulings R7 and R8).
  *
  * The tables: p1 stands at Ancient City, which the bandits rule, with 5
  * board warbands and 7 Supply (`CampaignFixture.board`); p2 is the
  * Chancellor; p3's pawn stands at Ancient City with 2 board warbands. p1
  * raids p3, or conquers Ancient City from the bandits. */
class KeeperOfOrderSuite extends munit.FunSuite:
  private val keeper: DecisionOptionRef =
    DecisionOptionRef.Legacy(KeeperOfOrderCard.id)
  private val add = PlanAmounts.button(KeeperOfOrder.Dice)
  private val remove = PlanAmounts.button(-KeeperOfOrder.Dice)
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)

  /** Every attack die a two-swords-and-skull, every defense die blank. */
  private val skulls: WalkerDice = (kind, count) => Right(Vector.fill(count)(
    kind match
      case DiceKind.Attack => AttackDieFace.TwoSwordsSkull: DieFace
      case DiceKind.Defense => DefenseDieFace.Blank: DieFace
      case DiceKind.Round => RoundDieFace.One: DieFace))

  /** p1 raids p3 at Ancient City; p2 is the Chancellor and `citizens` are
    * Citizens; `steps` add the legacies. */
  private def raidOnP3(citizens: PlayerId*)(steps: Table => Table): Board =
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, citizens*).oathkeeper(None))
    on(b)(table => steps(table.pawn(p3, at = b.origin).boardWarbands(p3, 2)))

  /** p1 and p3 are Citizens, and p1 holds Keeper of Order. */
  private val infighting: Board =
    raidOnP3(p1, p3)(_.legacy(p1, KeeperOfOrderCard))

  private def raided(b: Board, dice: WalkerDice = winningDice): Run =
    commit(rules(dice), b, 4, raid = true)

  private def lines(run: Run): Vector[NoteText.Said] =
    said(KeeperOfOrder, run.events)

  test("an Imperial attacker against an Imperial is offered it, and " +
      "chooses add or remove (R7)"):
    val run = raided(infighting)
    assert(run.offered(p1).contains(keeper))
    assertEquals(run.pick(p1, CampaignIds.attackerPlan, keeper).query(p1),
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(add, "Add 2 attack dice"),
        DecisionOption.Button(remove, "Remove 2 attack dice")),
        heading = Some("Keeper of Order: add or remove 2 attack dice?")))

  test("adding puts two dice in the pool, and the attacker's skulls are " +
      "ignored (R8)"):
    val done = raided(infighting, skulls)
      .pick(p1, CampaignIds.attackerPlan, keeper)
      .pick(p1, KeeperOfOrder.decisionId, add).finishPlanWindows
    assert(done.ops.contains(ModifyDicePool(CampaignIds.attackPool, 2)))
    assertEquals(done.result.skullLosses, 0)
    assertEquals(lines(done), Vector(
      NoteText.Said("added", "Added 2 attack dice.", covers = false),
      NoteText.Said("ignored", "Skulls ignored.", covers = false)))

  test("a defender removes two, and the attacker's skulls still count"):
    val b = raidOnP3(p1, p3)(_.legacy(p3, KeeperOfOrderCard))
    val run = raided(b, skulls).finishAttackerPlans(p1)
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    val done = run.pick(p3, CampaignIds.defenderPlan, keeper)
      .pick(p3, KeeperOfOrder.decisionId, remove).finishPlanWindows
    assert(done.ops.contains(ModifyDicePool(CampaignIds.attackPool, -2)))
    assert(done.result.skullLosses > 0)
    assertEquals(lines(done), Vector(
      NoteText.Said("removed", "Removed 2 attack dice.", covers = false)))

  test("it is not offered against an Exile"):
    val run = raided(raidOnP3(p1)(_.legacy(p1, KeeperOfOrderCard)))
    assert(!run.offered(p1).contains(keeper))

  test("it is not offered against the bandits"):
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, p1).oathkeeper(None).legacy(p1, KeeperOfOrderCard))
    assert(!commit(rules(winningDice), b, 4).offered(p1).contains(keeper))

  test("an Exile holder is never offered it (ruling 2)"):
    val run = raided(raidOnP3(p3)(_.legacy(p1, KeeperOfOrderCard)))
    assert(!run.offered(p1).contains(keeper))

  test("a dormant Keeper of Order is never offered (ruling 1)"):
    val run = raided(raidOnP3(p1, p3)(
      _.legacy(p1, KeeperOfOrderCard, active = false)))
    assert(!run.offered(p1).contains(keeper))

  test("another Imperial uses it through Imperial Tactics with the " +
      "holder's offer (ruling 5)"):
    val withheld = raided(raidOnP3(p1, p3)(_.legacy(p2, KeeperOfOrderCard)))
      .finishAttackerPlans(p1)
    assert(awaits(withheld, p3, CampaignIds.defenderPlan))
    assertEquals(withheld.withheld(p1, keeper),
      Some(ImperialTactics.awaitsPlan(p2)))
    val lent = raided(raidOnP3(p1, p3)(_.legacy(p2, KeeperOfOrderCard)
      .aid(p2, defending, AidOffer(plans = Vector(keeper)))))
      .finishAttackerPlans(p1)
    assert(lent.options(p1).exists(option => option.ref == keeper &&
      DecisionOption.lender(option).contains(p2)))
    val done = lent.pick(p3, CampaignIds.defenderPlan, keeper)
      .pick(p3, KeeperOfOrder.decisionId, remove).finishPlanWindows
    assert(done.ops.contains(ModifyDicePool(CampaignIds.attackPool, -2)))

  test("lent to the attacker through Imperial Tactics, the borrowing " +
      "attacker's skulls are ignored"):
    val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
    val done = raided(raidOnP3(p1, p3)(_.legacy(p2, KeeperOfOrderCard)
      .aid(p2, attacking, AidOffer(plans = Vector(keeper)))), skulls)
      .pick(p1, CampaignIds.attackerPlan, keeper)
      .pick(p1, KeeperOfOrder.decisionId, add).finishPlanWindows
    assert(done.ops.contains(ModifyDicePool(CampaignIds.attackPool, 2)))
    assertEquals(done.result.skullLosses, 0)

  test("an Imperial defender against an Exile attacker is not offered it"):
    // p1, an Exile, raids p3, a Citizen holding Keeper of Order and the
    // Oathkeeper title, whose defense opens p3's plan window.
    val run = raided(raidOnP3(p3)(_.legacy(p3, KeeperOfOrderCard)
      .oathkeeper(Some(p3)))).finishAttackerPlans(p1)
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    assert(run.offered(p1).nonEmpty)
    assert(!run.offered(p1).contains(keeper))

  test("a boxed lineage's Keeper of Order is never offered (ruling 1)"):
    val boxed = infighting.copy(ready =
      LegacyActionFixture.boxed(infighting.ready, p1))
    assert(!raided(boxed).offered(p1).contains(keeper))

  test("a walk resumed at its question applies the chosen sign"):
    val b = infighting
    val asked = raided(b).pick(p1, CampaignIds.attackerPlan, keeper)
    val replayed = accepted(Journal.evolved(rules(winningDice), b.ready,
      asked.events))
    val done = accepted(rules(winningDice).resolveWalker(replayed, p1,
      KeeperOfOrder.decisionId, DecisionAnswer.ChooseOneAnswer(remove)))
    assert(RecordedOps.all(done.events).contains(
      ModifyDicePool(CampaignIds.attackPool, -2)))
