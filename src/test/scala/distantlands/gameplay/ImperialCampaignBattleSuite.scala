package distantlands.gameplay

import distantlands.gameplay.CampaignFixture.{Board, on, rules,
  withAdviserFor}
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.campaign.PlanDriver.{awaits, commit,
  losingDice, winningDice}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.Look
import distantlands.testkit.Table.{p1, p2, p3}

/** Whole Campaigns with Imperial players (Empire slice 1b-i): every
  * warband a Campaign places, kills or sacrifices is of its owner's kind,
  * purple for an Imperial player. Every attack die is a sword
  * (`winningDice`) or half a sword (`losingDice`), and every defense die is
  * blank. No one holds the Oathkeeper title, so no defender has the title's
  * plan. */
class ImperialCampaignBattleSuite extends munit.FunSuite:
  private def purple(n: Int) = Piece.Warbands(ForceKind.Imperial, n)
  private def boardOf(player: PlayerId) =
    PositionedLocation(Location.PlayArea(player))

  /** The fixture board with p2 the Chancellor and `citizens` Citizens, and
    * no title holder. p1 stands at the bandit-held Ancient City with 5
    * board warbands. */
  private def seated(citizens: PlayerId*): Board =
    on(CampaignFixture.board(warbands = 5, supply = 7))(
      _.imperial(p2, citizens*).oathkeeper(None))

  test("a Citizen who conquers places purple warbands, and the site becomes " +
      "Imperial"):
    val b = seated(p1)
    val run = commit(rules(winningDice), b, 4).declineSacrifice(p1)
    assert(awaits(run, p1, CampaignIds.placement))
    val placed = run.answer(p1, CampaignIds.placement, ChooseAmountAnswer(3))
    assertEquals(Look(placed.state).forces(b.origin),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(Look(placed.state).boardWarbands(p1), 2)

  test("a defeated Citizen loses purple warbands from their board"):
    val b = seated(p1)
    val run = commit(rules(losingDice), b, 2).declineSacrifice(p1)
    // Two survivors against two bandits: half of them, one, dies.
    assert(run.ops.contains(Kill(purple(1), boardOf(p1))), run.ops.toString)

  test("an Exile who conquers an Empire site kills its purple warbands and " +
      "returns half to the Chancellor's board"):
    val exile = seated()
    val b = on(exile)(_.warbandsAt(exile.origin, p2, 2).boardWarbands(p2, 0))
    val run = commit(rules(winningDice), b, 4)
    assert(!awaits(run, p2, CampaignIds.defenderPlan))
    val fought = run.declineSacrifice(p1)
    // The two that died at the site: one of them returns to p2's board.
    assertEquals(Look(fought.state).boardWarbands(p2), 1)
    assert(fought.ops.contains(Kill(purple(2),
      PositionedLocation(Location.Site(b.origin)))), fought.ops.toString)
    assert(awaits(fought, p1, CampaignIds.placement))
    val placed = fought.answer(p1, CampaignIds.placement, ChooseAmountAnswer(2))
    assertEquals(Look(placed.state).forces(b.origin),
      SiteForces.Occupied(Look(placed.state).forceKind(p1), 2))
    assertNotEquals(Look(placed.state).forceKind(p1), ForceKind.Imperial)

  test("an Exile who raids a Citizen kills half of the Citizen's purple " +
      "board"):
    val exile = seated(p3)
    // p3's 2 board warbands defend: 4 swords beat 2.
    val b = on(exile)(_.pawn(p3, at = exile.origin).boardWarbands(p3, 2))
    val run = commit(rules(winningDice), b, 4, raid = true)
    assert(!awaits(run, p3, CampaignIds.defenderPlan))
    val fought = run.declineSacrifice(p1)
    assert(fought.ops.contains(Kill(purple(1), boardOf(p3))),
      fought.ops.toString)

  test("a Citizen raided by another Citizen sacrifices a purple warband " +
      "from their board for Wrestlers"):
    val wrestlers = WrestlersCard.id
    val citizens = seated(p1, p3)
    val base = on(citizens)(_.pawn(p3, at = citizens.origin)
      .boardWarbands(p3, 3))
    val b = withAdviserFor(base, p3, wrestlers, Orientation.FaceUp)
    val run = commit(rules(losingDice), b, 2, raid = true)
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    val picked = run.pick(p3, CampaignIds.defenderPlan,
      DecisionOptionRef.Denizen(wrestlers))
    assert(picked.since(run).contains(Sacrifice(p3, purple(1), boardOf(p3))),
      picked.since(run).toString)
