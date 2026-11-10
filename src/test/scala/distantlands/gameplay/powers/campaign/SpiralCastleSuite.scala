package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.CampaignFixture._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look}
import oathdigital.testkit.Outcomes.assertWireRoundTrips

/** Spiral Castle and Spiral Dungeons (catalog batch 9, slice 7): the
  * loser's pawn is sent to the edifice's site and they lose 2 Supply. */
class SpiralCastleSuite extends munit.FunSuite:
  private val edifice = SpiralCastleCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Edifice(edifice)

  /** An in-play site that is neither the Raid's origin nor where the
    * bystander stands, so a pawn sent there has moved. */
  private def farSite(b: Board): SiteId =
    val bystander = Look(b.ready).player(b.bystander).pawnSite
    b.ready.game.current.map.inPlay
      .find(site => site != b.origin && !bystander.contains(site)).get

  private def sentLine(loser: PlayerId, site: SiteId, lost: Int)
      : NoteText.Said = NoteText.Said("sent",
    s"Sent ${loser.value}'s pawn to ${site.value}; they lost $lost Supply.",
    covers = true)

  // ---- Spiral Castle ----

  /** The Raid board with the intact Castle at a far site the actor rules. */
  private def castleRaid: RaidBoard =
    val raid = raidBoard()
    val b = raid.board
    RaidBoard(actorRules(withEdifice(b, farSite(b), edifice,
      EdificeSide.Intact), farSite(b)), raid.relic)

  test("a victorious Raid with Spiral Castle sends the defender's pawn to " +
      "the Castle's site, costing them 2 Supply, instead of asking where"):
    val raid = castleRaid
    val b = raid.board
    val site = farSite(b)
    val supply = Look(b.ready).supply(b.rival)
    val done = commit(rules(winningDice), b, 4,
      targets = Vector(DecisionOptionRef.Relic(raid.relic)), raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    assertEquals(done.winner, Some(true))
    assertEquals(Look(done.state).player(b.rival).pawnSite, Some(site))
    assertEquals(Look(done.state).supply(b.rival), supply - 2)
    // The takes and the consequences stand.
    assert(Look(done.state).player(b.actor).relics.exists(_.id == raid.relic))
    assertEquals(Look(done.state).player(b.rival).board.favor, 5 - 5 / 2)
    parked.assertResumed(done.state, Phase.Act, b.actor)
    assertEquals(said(SpiralCastle, done.events),
      Vector(sentLine(b.rival, site, 2)))
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      ready(done.state))
    assertWireRoundTrips(done.events)

  test("the Castle is offered in a Raid only, to the ruler of its site"):
    val raid = castleRaid
    val b = raid.board
    assert(!commit(rules(winningDice), b, 4).offers(b.actor, b.actor,
      CampaignIds.attackerPlan, ref))
    val unruled = withEdifice(raidBoard().board, farSite(raidBoard().board),
      edifice, EdificeSide.Intact)
    assert(!commit(rules(winningDice), unruled, 4, raid = true)
      .offers(unruled.actor, unruled.actor, CampaignIds.attackerPlan, ref))

  test("a lost Raid with the Castle chosen changes nothing"):
    val raid = castleRaid
    val b = raid.board
    val done = commit(rules(losingDice), b, 4,
      targets = Vector(DecisionOptionRef.Relic(raid.relic)), raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).player(b.rival).pawnSite, Some(b.origin))
    assertEquals(said(SpiralCastle, done.events), Vector.empty)

  // ---- Spiral Dungeons ----

  test("a bandit defender at a site holding the ruined Dungeons applies it " +
      "when it wins: the attacker's pawn goes there and loses 2 Supply"):
    val base = board(extras = 1, warbands = 5, supply = 7)
    val dungeons = base.extras.head
    val b = withEdifice(base, dungeons, edifice, EdificeSide.Ruined)
    val done = commit(rules(losingDice), b, 2).finishPlanWindows
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).pawn(b.actor), dungeons)
    // 7, minus the Campaign's 2 Supply, minus the Dungeons' 2.
    assertEquals(Look(done.state).supply(b.actor), 7 - 2 - 2)
    assertEquals(said(SpiralDungeons, done.events),
      Vector(sentLine(b.actor, dungeons, 2)))
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      ready(done.state))

  test("a player defender who wins with the Dungeons sends the attacker " +
      "to its site; a defender who loses sends nobody"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withEdifice(base, b_far(base), edifice, EdificeSide.Ruined)
    val held = on(b)(_.warbandsAt(b_far(base), b.rival, 2))
    val won = commit(rules(losingDice), held, 4)
      .pick(held.rival, CampaignIds.defenderPlan, ref).finishPlanWindows
    assertEquals(won.winner, Some(false))
    assertEquals(Look(won.state).pawn(held.actor), b_far(base))
    val lost = commit(rules(winningDice), held, 4)
      .pick(held.rival, CampaignIds.defenderPlan, ref).finishPlanWindows
    assertEquals(lost.winner, Some(true))
    assertEquals(Look(lost.state).pawn(held.actor), held.origin)
    assertEquals(said(SpiralDungeons, lost.events), Vector.empty)

  /** A far site for a board whose rival stands at the origin. */
  private def b_far(b: Board): SiteId =
    b.ready.game.current.map.inPlay.find(site => site != b.origin &&
      !Look(b.ready).player(b.bystander).pawnSite.contains(site)).get

  test("the intact Castle is no defender's plan, and the ruined Dungeons " +
      "no attacker's"):
    val raid = castleRaid
    assert(!commit(rules(winningDice), raid.board, 4, raid = true)
      .offers(raid.board.actor, raid.board.rival, CampaignIds.defenderPlan,
        ref))
