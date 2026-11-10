package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.{NoteText, PlayerFacts}
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look}
import distantlands.testkit.Outcomes.assertWireRoundTrips

/** Obsidian Cage's battle plan: a victorious user moves the enemy's force,
  * as the losses left it, onto the Cage. */
class ObsidianCageSuite extends munit.FunSuite:
  private val cage: RelicId = ObsidianCageCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Relic(cage)

  private def kindOf(b: Board, player: PlayerId): ForceKind =
    PlayerForceKind.of(b.ready, b.player(player)).get

  private def onCage(run: Run): Map[ForceKind, Int] =
    ready(run.state).game.current.warbandsOnCards.getOrElse(cage, Map.empty)

  /** p1 holds the Cage faceup and 5 warbands and Raids the rival, who
    * shares the origin with 3 board warbands. */
  private def raidingWithTheCage: Board =
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    on(withRelic(base, ObsidianCageCard))(_.boardWarbands(base.rival, 3))

  test("a victorious Raid cages the half of the defender's board the losses " +
      "left, and the warbands stay out of their bank"):
    val b = raidingWithTheCage
    val rival = kindOf(b, b.rival)
    val banked = PlayerFacts.banked(b.ready, rival)
    val done = commit(rules(winningDice), b, 5, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    // Five swords beat three warbands and blank dice.
    assertEquals(done.winner, Some(true))
    val won = done.pick(b.actor, CampaignIds.relocation,
      done.offered(b.actor).head)
    // The losses killed 1 of 3; the other 2 are on the Cage.
    assertEquals(Look(won.state).player(b.rival).board.warbands, 0)
    assertEquals(onCage(won), Map(rival -> 2))
    assertEquals(PlayerFacts.banked(ready(won.state), rival), banked + 1)
    assertEquals(said(ObsidianCage, won.events), Vector(NoteText.Said("caged",
      s"Moved 2 of ${b.rival.value}'s warbands onto Obsidian Cage.",
      covers = true)))
    parked.assertResumed(won.state, Phase.Act, b.actor)
    assertEquals(ActPowerHarness.replayed(won.game, b.ready, won.events),
      ready(won.state))
    assertWireRoundTrips(won.events)

  test("a victorious Conquest cages the warbands the victory returned to " +
      "the defender's board, and none it held before"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = on(withRelic(base, ObsidianCageCard))(
      _.boardWarbands(base.rival, 4))
    val done = commit(rules(winningDice), b, 5)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    // Five swords beat two warbands and blank dice; 1 of the 2 dies and 1
    // returns to the board, then onto the Cage.
    assertEquals(done.winner, Some(true))
    assertEquals(Look(done.state).player(b.rival).board.warbands, 4)
    assertEquals(onCage(done), Map(kindOf(b, b.rival) -> 1))

  test("a victorious defender cages the attacker's whole board as the " +
      "losses left it"):
    val ruled = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = on(ruled)(_.relic(ruled.rival, ObsidianCageCard))
    val done = commit(rules(losingDice), b, 2)
      .finishAttackerPlans(b.actor)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
      .finishPlanWindows
    // Two hollow swords make one, against two warbands: the attacker loses
    // 1 of the 2 committed, and the 4 left on the board go to the Cage.
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).player(b.actor).board.warbands, 0)
    assertEquals(onCage(done), Map(kindOf(b, b.actor) -> 4))
    assertEquals(said(ObsidianCage, done.events), Vector(NoteText.Said(
      "caged.own", "Moved 4 warbands onto Obsidian Cage.", covers = true)))

  test("the warbands the enemy's Hospital saved are caged with the rest, " +
      "and none reach Hospital's site"):
    // Owner's ruling, 2026-11-03: a warband that survives through Hospital
    // is still in the force, so it ends on the Cage.
    val ruled = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val hospitalSite = BrokenPeaksSite.id
    val b = withSiteCard(on(ruled)(_.relic(ruled.rival, ObsidianCageCard)
      .warbandsAt(hospitalSite, ruled.actor, 2)), hospitalSite, HospitalCard.id)
    val done = commit(rules(losingDice), b, 2)
      .pick(b.actor, CampaignIds.attackerPlan,
        DecisionOptionRef.Denizen(HospitalCard.id))
      .finishAttackerPlans(b.actor)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
      .finishPlanWindows
    // The attacker loses 1 of the 2 committed, which Hospital saves: the 4
    // left on the board and the saved one go to the Cage.
    assertEquals(done.winner, Some(false))
    assertEquals(onCage(done), Map(kindOf(b, b.actor) -> 5))
    assertEquals(Look(done.state).forces(hospitalSite),
      SiteForces.Occupied(kindOf(b, b.actor), 2))
    assertEquals(said(Hospital, done.events), Vector.empty)
    assertEquals(said(ObsidianCage, done.events), Vector(NoteText.Said(
      "caged.own", "Moved 5 warbands onto Obsidian Cage.", covers = true)))

  test("a defeated user cages nothing"):
    val b = raidingWithTheCage
    val done = commit(rules(losingDice), b, 2, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    assertEquals(done.winner, Some(false))
    assertEquals(onCage(done), Map.empty[ForceKind, Int])
    assertEquals(said(ObsidianCage, done.events), Vector.empty)

  test("against bandits it is shown unavailable"):
    // Longbows keeps the attacker's plan decision asked.
    val b = withAdviser(withRelic(board(warbands = 5, supply = 7),
      ObsidianCageCard), LongbowsCard, Orientation.FaceUp)
    assertEquals(commit(rules(winningDice), b, 4).withheld(b.actor, ref),
      Some(OptionReason.Written(ObsidianCage.againstBandits)))
