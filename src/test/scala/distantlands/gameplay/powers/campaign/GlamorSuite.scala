package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.{ActPowerHarness, Look}
import distantlands.testkit.Outcomes.assertWireRoundTrips

/** Glamor: an attacker's plan against a player that targets the defender's
  * board secrets but their last, for a defense die each; a victorious
  * attacker takes them, faceup ones first. */
class GlamorSuite extends munit.FunSuite:
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(GlamorCard.id)

  private def targeted(text: String): NoteText.Said =
    NoteText.Said("targeted", s"Targeted $text.", covers = false)

  private def took(text: String): NoteText.Said =
    NoteText.Said("took", s"Took $text.", covers = true)

  private def secrets(state: OathState, player: PlayerId): (Int, Int) =
    val board = Look(state).player(player).board
    board.faceUpSecrets -> board.faceDownSecrets

  /** p1 holds Glamor faceup, no secret and 5 warbands, at the origin with
    * the rival, who has one board warband and these secrets. */
  private def raidingRivalWithSecrets(faceUp: Int, faceDown: Int): Board =
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    on(withAdviser(base, GlamorCard, Orientation.FaceUp))(_
      .secrets(base.actor, faceUp = 0)
      .boardWarbands(base.rival, 1)
      .secrets(base.rival, faceUp = faceUp, faceDown = faceDown))

  /** The Raid won: plans finished and the defender's pawn moved on. */
  private def relocated(run: Run, b: Board): Run =
    run.pick(b.actor, CampaignIds.relocation, run.offered(b.actor).head)

  test("a Raid targets two of three secrets for two defense dice, and the " +
      "victor takes the two faceup ones"):
    val b = raidingRivalWithSecrets(faceUp = 2, faceDown = 1)
    val asked = commit(rules(winningDice), b, 4, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
    assert(awaits(asked, b.actor, Glamor.decisionId))
    assertEquals(asked.query(b.actor), DecisionQuery.ChooseAmount(1, 2,
      Some(Glamor.heading), Glamor.confirm))
    val chosen = asked.answer(b.actor, Glamor.decisionId,
      ChooseAmountAnswer(2))
    assert(chosen.since(asked).contains(
      ModifyDicePool(CampaignIds.defensePool, 2)))
    val won = chosen.finishPlanWindows
    // Four swords beat one warband and blank defense dice.
    assertEquals(won.winner, Some(true))
    val done = relocated(won, b)
    assertEquals(secrets(done.state, b.rival), 0 -> 1)
    assertEquals(secrets(done.state, b.actor), 2 -> 0)
    assertEquals(said(Glamor, done.events), Vector(
      targeted("2 secrets for 2 defense dice"),
      took(s"2 secrets from ${b.rival.value}")))
    parked.assertResumed(done.state, Phase.Act, b.actor)
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      ready(done.state))
    assertWireRoundTrips(done.events)

  test("the take runs past the faceup secrets into the facedown ones, each " +
      "keeping its side"):
    val b = raidingRivalWithSecrets(faceUp = 1, faceDown = 2)
    val done = relocated(commit(rules(winningDice), b, 4, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, Glamor.decisionId, ChooseAmountAnswer(2))
      .finishPlanWindows, b)
    assertEquals(secrets(done.state, b.rival), 0 -> 1)
    assertEquals(secrets(done.state, b.actor), 1 -> 1)

  test("a victorious Conquest against a player takes the targeted secret " +
      "after the placement"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = on(withAdviser(base, GlamorCard, Orientation.FaceUp))(_
      .secrets(base.actor, faceUp = 0)
      .secrets(base.rival, faceUp = 2))
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, Glamor.decisionId, ChooseAmountAnswer(1))
      .finishPlanWindows
    // Four swords beat the rival's two warbands and blank defense dice.
    assertEquals(done.winner, Some(true))
    assertEquals(secrets(done.state, b.rival), 1 -> 0)
    assertEquals(secrets(done.state, b.actor), 1 -> 0)
    assertEquals(said(Glamor, done.events), Vector(
      targeted("1 secret for 1 defense die"),
      took(s"1 secret from ${b.rival.value}")))

  test("a defeated attacker takes no secret"):
    val b = raidingRivalWithSecrets(faceUp = 3, faceDown = 0)
    val done = commit(rules(losingDice), b, 2, raid = true)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, Glamor.decisionId, ChooseAmountAnswer(2))
      .finishPlanWindows
    // Two hollow swords make one, which does not beat one warband.
    assertEquals(done.winner, Some(false))
    assertEquals(secrets(done.state, b.rival), 3 -> 0)
    assertEquals(secrets(done.state, b.actor), 0 -> 0)
    assertEquals(said(Glamor, done.events), Vector(
      targeted("2 secrets for 2 defense dice")))

  test("a defender with only their last secret leaves it unavailable"):
    // Longbows keeps the attacker's plan decision asked.
    val b = withAdviser(raidingRivalWithSecrets(faceUp = 0, faceDown = 1),
      LongbowsCard, Orientation.FaceUp)
    assertEquals(commit(rules(winningDice), b, 4, raid = true)
      .withheld(b.actor, ref), Some(OptionReason.Written(Glamor.noSecret)))

  test("it is never offered against bandits"):
    // Longbows keeps the attacker's plan decision asked.
    val b = withAdviser(withAdviser(board(warbands = 5, supply = 7),
      GlamorCard, Orientation.FaceUp), LongbowsCard, Orientation.FaceUp)
    val run = commit(rules(winningDice), b, 4)
    assert(awaits(run, b.actor, CampaignIds.attackerPlan))
    assert(!run.options(b.actor).exists(_.ref == ref))
