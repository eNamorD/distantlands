package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.CampaignFixture._
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.walker.WalkerDice
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, CatalogNames}

/** Martial Culture: an Exile who defeats another Exile may become a Citizen,
  * on either side (ruling R23). */
class MartialCultureSuite extends munit.FunSuite:
  private val card = CatalogNames.denizen("Martial Culture")
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)
  private val held = CatalogNames.site("Broken Peaks")

  private def lines(run: Run): Vector[NoteText.Said] =
    NoteText.said(MartialCulture.id, MartialCulture.noteKeys, run.events)

  /** The actor raids the rival at the origin, both Exiles. `owner` rules
    * Broken Peaks, where Martial Culture stands faceup. The bystander is
    * the Chancellor unless `empire` is off. */
  private def exilesFight(owner: Board => PlayerId, empire: Boolean = true,
      allowed: Boolean = true): Board =
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    on(base) { table =>
      val placed = table.boardWarbands(base.rival, 3)
        .warbandsAt(held, owner(base), 1).denizen(card, at = held)
      val seated = if empire then placed.imperial(base.bystander) else placed
      if allowed then seated.allowingCitizenshipPowers else seated
    }

  private def raided(b: Board, dice: WalkerDice): Run =
    commit(rules(dice), b, 4, raid = true)

  private def toQuestion(run: Run, attacker: PlayerId): Run =
    if awaits(run, attacker, CampaignIds.relocation) then
      run.pick(attacker, CampaignIds.relocation, run.offered(attacker).head)
    else run

  private def attackerWon(b: Board): Run = toQuestion(raided(b, winningDice)
    .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows, b.actor)

  test("a victorious Exile attacker becomes a Citizen and their Act ends"):
    val b = exilesFight(_.actor)
    val won = attackerWon(b)
    assert(awaits(won, b.actor, MartialCulture.decisionId))
    val done = won.pick(b.actor, MartialCulture.decisionId,
      MartialCulture.become)
    val after = ready(done.state)
    assertEquals(Imperials.role(after, b.actor), Some(Role.Citizen))
    // The Act ends; the driver then plays on into the next turn.
    assert(done.since(won).contains(EnterPhase(Phase.Rest)))
    assertEquals(lines(done), Vector(NoteText.Said("used",
      "Became a Citizen.", covers = false)))
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      after)

  test("with no Empire the victor becomes the Chancellor"):
    val b = exilesFight(_.actor, empire = false)
    val done = attackerWon(b).pick(b.actor, MartialCulture.decisionId,
      MartialCulture.become)
    assertEquals(Imperials.chancellor(ready(done.state)), Some(b.actor))
    assertEquals(lines(done), Vector(NoteText.Said("used.chancellor",
      "Became the Chancellor.", covers = false)))

  test("declining keeps the role and says so"):
    val b = exilesFight(_.actor)
    val done = attackerWon(b).pick(b.actor, MartialCulture.decisionId,
      MartialCulture.decline)
    assertEquals(Imperials.role(ready(done.state), b.actor), Some(Role.Exile))
    assertEquals(lines(done), Vector(NoteText.Said("declined",
      "Declined Citizenship.", covers = false)))

  test("a victorious Exile defender may become a Citizen off-turn"):
    val b = exilesFight(_.rival)
    val won = raided(b, losingDice).finishAttackerPlans(b.actor)
      .pick(b.rival, CampaignIds.defenderPlan, ref).finishPlanWindows
    assertEquals(won.winner, Some(false))
    val done = won.pick(b.rival, MartialCulture.decisionId,
      MartialCulture.become)
    val after = ready(done.state)
    assertEquals(Imperials.role(after, b.rival), Some(Role.Citizen))
    assertEquals(after.game.current.turn.activePlayer, b.actor)

  test("against bandits the plan is not offered"):
    val base = board(warbands = 5, supply = 7)
    val b = on(base)(_.warbandsAt(held, base.actor, 1).denizen(card, at = held)
      .allowingCitizenshipPowers)
    assert(!commit(rules(winningDice), b, 4).offered(b.actor).contains(ref))

  test("under the first-game option it is shown withheld with its reason"):
    // Battle Honors keeps the plan question open; a lone withheld plan
    // asks nothing.
    val b = withAdviser(exilesFight(_.actor, allowed = false),
      "Battle Honors", Orientation.FaceUp)
    assertEquals(raided(b, winningDice).withheld(b.actor, ref),
      Some(OptionReason.Written(Citizenship.ignoredReason)))
