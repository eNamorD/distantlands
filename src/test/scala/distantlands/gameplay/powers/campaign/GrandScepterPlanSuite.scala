package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.gameplay.roles.{CouncilSeat, CouncilSeatCard}
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look}

/** The Grand Scepter's battle plan: an Imperial player victorious against a
  * Citizen may exile them (ruling R23). */
class GrandScepterPlanSuite extends munit.FunSuite:
  private val ref: DecisionOptionRef = GrandScepterPlan.cardRef

  private def lines(run: Run): Vector[NoteText.Said] =
    NoteText.said(GrandScepterPlan.id, GrandScepterPlan.noteKeys, run.events)

  /** The Chancellor raids the rival, a Citizen, at the origin, holding the
    * Scepter faceup with 3 favor to burn. */
  private def chancellorRaidsCitizen: Board =
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    on(base)(_.imperial(base.actor, base.rival)
      .relic(base.actor, TheGrandScepterCard).favor(base.actor, 3)
      .boardWarbands(base.rival, 3))

  /** A Citizen, the actor, raids the Chancellor, who holds the Scepter
    * faceup with 3 favor to burn. */
  private def citizenRaidsChancellor: Board =
    val base = withEnemyAtOrigin(board(warbands = 4, supply = 7))
    on(base)(_.imperial(base.rival, base.actor)
      .relic(base.rival, TheGrandScepterCard).favor(base.rival, 3)
      .boardWarbands(base.rival, 3))

  /** Past any relocation, to the Scepter's question. */
  private def toQuestion(run: Run, attacker: PlayerId): Run =
    if awaits(run, attacker, CampaignIds.relocation) then
      run.pick(attacker, CampaignIds.relocation, run.offered(attacker).head)
    else run

  private def wonWithScepter(b: Board): Run = toQuestion(
    commit(rules(winningDice), b, 4, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows,
    b.actor)

  test("a victorious Chancellor exiles the Citizen, burning 3 favor"):
    val b = chancellorRaidsCitizen
    val won = wonWithScepter(b)
    assertEquals(won.winner, Some(true))
    assert(awaits(won, b.actor, GrandScepterPlan.decisionId))
    val done = won.pick(b.actor, GrandScepterPlan.decisionId,
      GrandScepterPlan.exile)
    val after = ready(done.state)
    assertEquals(Imperials.role(after, b.rival), Some(Role.Exile))
    assertEquals(Look(after).player(b.actor).board.favor, 0)
    assertEquals(lines(done), Vector(NoteText.Said("exiled",
      s"Exiled ${b.rival.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      after)

  test("sparing the Citizen leaves them one and says so"):
    val b = chancellorRaidsCitizen
    val done = wonWithScepter(b).pick(b.actor, GrandScepterPlan.decisionId,
      GrandScepterPlan.spare)
    assertEquals(Imperials.role(ready(done.state), b.rival),
      Some(Role.Citizen))
    assertEquals(lines(done), Vector(NoteText.Said("spared",
      s"Did not exile ${b.rival.value}.", covers = false)))

  test("a defending Chancellor who wins exiles the attacking Citizen"):
    val b = citizenRaidsChancellor
    val won = commit(rules(losingDice), b, 4, raid = true)
      .finishAttackerPlans(b.actor)
      .pick(b.rival, CampaignIds.defenderPlan, ref).finishPlanWindows
    assertEquals(won.winner, Some(false))
    val done = won.pick(b.rival, GrandScepterPlan.decisionId,
      GrandScepterPlan.exile)
    val after = ready(done.state)
    assertEquals(Imperials.role(after, b.actor), Some(Role.Exile))
    // The defender burnt the 3 favor; the exiled attacker's Act ended.
    assertEquals(Look(after).player(b.rival).board.favor, 0)
    assert(done.since(won).contains(EnterPhase(Phase.Rest)))
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      after)

  test("against an Exile, or for an Exile, the plan is not offered"):
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    val exileHolder = on(base)(_.relic(base.actor, TheGrandScepterCard)
      .favor(base.actor, 3))
    val asked = commit(rules(winningDice), exileHolder, 4, raid = true)
    assert(!asked.offered(exileHolder.actor).contains(ref))
    val againstExile = on(base)(_.imperial(base.actor)
      .relic(base.actor, TheGrandScepterCard).favor(base.actor, 3))
    val raided = commit(rules(winningDice), againstExile, 4, raid = true)
    assert(!raided.offered(againstExile.actor).contains(ref))

  // The first-game option, on by default in these tables, leaves the plan
  // offered: it exiles, it makes no one a Citizen.
  test("a lost Campaign asks nothing"):
    val b = chancellorRaidsCitizen
    val lost = commit(rules(losingDice), b, 4, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    assertEquals(lost.winner, Some(false))
    assert(!awaits(lost, b.actor, GrandScepterPlan.decisionId))
    assertEquals(lines(lost), Vector.empty)

  /** As `chancellorRaidsCitizen`, with the Citizen holding Council Seat
    * faceup. */
  private def chancellorRaidsSeatedCitizen: Board =
    val base = chancellorRaidsCitizen
    on(base)(_.adviser(base.rival, CouncilSeatCard))

  private val seatWarning = OptionReason.Written(CouncilSeat.warning)

  private def warningOf(option: Option[DecisionOption]): Option[OptionReason] =
    option.collect { case DecisionOption.Warned(_, reason) => reason }

  test("against a Citizen holding Council Seat, the plan carries the " +
      "Seat's warning"):
    val b = chancellorRaidsSeatedCitizen
    val asked = commit(rules(winningDice), b, 4, raid = true)
    assertEquals(warningOf(asked.options(b.actor).find(_.ref == ref)),
      Some(seatWarning))

  test("against a Citizen holding Council Seat, the exile carries the " +
      "Seat's warning"):
    val b = chancellorRaidsSeatedCitizen
    val won = wonWithScepter(b)
    assertEquals(warningOf(won.options(b.actor).find(
      _.ref == GrandScepterPlan.exile)), Some(seatWarning))

  test("exiling a Citizen who holds Council Seat leaves them a Citizen"):
    val b = chancellorRaidsSeatedCitizen
    val done = wonWithScepter(b).pick(b.actor, GrandScepterPlan.decisionId,
      GrandScepterPlan.exile)
    assertEquals(Imperials.role(ready(done.state), b.rival),
      Some(Role.Citizen))
    assertEquals(lines(done), Vector(NoteText.Said("spared",
      s"Did not exile ${b.rival.value}.", covers = false)))
