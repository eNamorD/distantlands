package distantlands.gameplay.setup.legacies

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, GoldenValleySite}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{LogNoted, WalkerCompleted, WalkerDice}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Look, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** World Crafter under Wide Horizons: "when anyone explores, you fill the
  * slot with a Recent or Forgotten Site" (spec ruling 11). p1 explores
  * from Buried Giant, the Hintermost of three sites; p2 holds World
  * Crafter. */
class WorldCrafterExploreSuite extends munit.FunSuite:
  import DefenseDieFace.TwoShields
  private val fourShields: Vector[DieFace] = Vector(TwoShields, TwoShields)

  private def table(holder: PlayerId, active: Boolean = true): ReadyGame =
    Table.start.foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
      .legacy(holder, WorldCrafterCard, active).ready

  private def rules(dice: WalkerDice): OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = dice)

  private def rolled(ready: ReadyGame): (OathRules, OathTransition) =
    val explore = rules(scripted(fourShields))
    explore -> accepted(explore.startWalker(Ready(ready), ActionRef.Explore,
      p1))

  private def lines(events: Vector[OathEvent]): Vector[String] =
    events.collect { case LogNoted(_, note, _)
      if ExploreProcedure.noteKeys.exists(_.name == note.key) => note.key }

  test("the holder answers another player's Explore end, and a walk " +
      "resumed at it places the chosen site"):
    val (explore, at) = rolled(table(p2))
    val decide = EconomyActions.parkedDecision(at.state)
    assertEquals(decide.decisionId, ExploreProcedure.endDecisionId)
    assertEquals(decide.owner, p2)
    val placed = accepted(explore.resolveWalker(at.state, p2,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Forgotten))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)))
    val after = Outcomes.ready(placed.state)
    assertEquals(Look(after).pawn(p1), GoldenValleySite.id)
    assertEquals(lines(placed.events), Vector("world-crafter-chose-forgotten",
      "explored-forgotten"))

  test("the explorer may not answer in the holder's place"):
    val (explore, at) = rolled(table(p2))
    assert(explore.resolveWalker(at.state, p1, ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))).isLeft)

  test("a holder who explores answers as before, with no World Crafter " +
      "line"):
    val (_, at) = rolled(table(p1))
    assertEquals(EconomyActions.parkedDecision(at.state).owner, p1)
    assert(!at.events.exists {
      case LogNoted(_, note, _) => note.key.startsWith("world-crafter")
      case _ => false
    })

  test("a dormant World Crafter leaves the end to the explorer (ruling 1)"):
    val (explore, at) = rolled(table(p2, active = false))
    assertEquals(EconomyActions.parkedDecision(at.state).owner, p1)
    val placed = accepted(explore.resolveWalker(at.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    assert(!lines(at.events ++ placed.events).exists(
      _.startsWith("world-crafter")))
