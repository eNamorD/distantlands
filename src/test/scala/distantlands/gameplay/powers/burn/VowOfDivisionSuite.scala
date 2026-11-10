package distantlands.gameplay.powers.burn

import distantlands.gameplay.CampaignFixture.{raidBoard, rules,
  withAdviserFor}
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver.{awaits, commit,
  winningDice}
import distantlands.gameplay.powers.targeting.TargetingFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, MenuPlay}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}

class VowOfDivisionSuite extends munit.FunSuite:
  private val vow = VowOfDivisionCard.id

  /** The lines Vow of Division journaled in `events`. */
  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(VowOfDivision.id, VowOfDivision.noteKeys, events)

  test("an attacker holding it takes the favor a defeated defender burns"):
    val raid = raidBoard()
    val b = withAdviserFor(raid.board, raid.board.actor, vow,
      Orientation.FaceUp)
    val before = Look(b.ready).favor(b.actor)
    val run = commit(rules(winningDice), b, 4,
      targets = Vector(DecisionOptionRef.Relic(raid.relic)), raid = true)
      .finishPlanWindows
    assertEquals(run.winner, Some(true))
    assert(awaits(run, b.actor, CampaignIds.relocation))
    assertEquals(Look(run.state).favor(b.rival), 5 - 5 / 2)
    assertEquals(Look(run.state).favor(b.actor), before + 5 / 2)
    assertEquals(said(run.events), Vector(NoteText.Said("taken",
      s"Took ${b.rival.value}'s 2 burned favor.", covers = false)))
    assertWireRoundTrips(run.events)

  test("a defender holding it still loses half their favor to the bank: " +
      "a Raid's burn is their own"):
    val raid = raidBoard()
    val b = withAdviserFor(raid.board, raid.board.rival, vow,
      Orientation.FaceUp)
    val before = Look(b.ready).favor(b.actor)
    val run = commit(rules(winningDice), b, 4,
      targets = Vector(DecisionOptionRef.Relic(raid.relic)), raid = true)
      .finishPlanWindows
    assertEquals(run.winner, Some(true))
    assertEquals(Look(run.state).favor(b.rival), 5 - 5 / 2)
    assertEquals(Look(run.state).favor(b.actor), before)
    assertEquals(said(run.events), Vector.empty)

  test("its holder cannot take the Challenge action, and the menu says " +
      "why; another player's Vow does not stop them"):
    val held = TargetingFixture.challengeTable.adviser(p1, vow).ready
    MenuPlay.menuOptionFor(catalog, held, ActionRef.Challenge) match
      case DecisionOption.Unavailable(_, reason) => assertEquals(reason,
        OptionReason.Written("Vow of Division prevents you from challenging"))
      case other => fail(s"the Challenge is offered: $other")
    val elsewhere = TargetingFixture.challengeTable.adviser(p2, vow).ready
    assert(MenuPlay.availableInMenu(catalog, elsewhere, ActionRef.Challenge))
