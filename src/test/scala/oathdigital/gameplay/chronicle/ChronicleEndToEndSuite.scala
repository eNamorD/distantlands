package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.{ChronicleAccounting, FirstGameSetupFixture,
  GameStartRules}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Outcomes, RecordedOps, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** A game won for real, through Start Chronicle, the Sun, Throne, World,
  * Beacon and Stars, to the journaled Chronicle. p1 is the Usurper as their Wake begins. */
class ChronicleEndToEndSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val usurperWakes: OathState = Table.start.turn(p1, Phase.Wake)
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  test("the winner's Chronicle runs every task to a journaled record that " +
      "passes game start's accounting, and replays"):
    val won = accepted(rules.next(OathTransition(usurperWakes, Vector.empty),
      PhaseTransitionRef.FinishRest))
    val started = accepted(rules.resolveWalker(won.state, p1,
      ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start)))
    val done = ChronicleWalk.answeringEverything(rules, started)
    val events = won.events ++ done.events
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    val ready = Outcomes.ready(done.state)
    val record = RecordedOps.all(done.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.last
    val lineage = ready.game.current.players.find(_.player == p1).get.lineage
    assertEquals(record.lineages.find(_.id == lineage).map(_.role),
      Some(Role.Chancellor))
    assertEquals(record.era.score(lineage), 3)
    assertEquals(record.era.games.last.entries.find(_.lineage == lineage)
      .map(_.endingRole), Some(Role.Chancellor))
    assertNotEquals(record.oathkeeperGoal,
      ready.game.campaign.baseline.oathkeeperGoal)
    assertEquals(ready.game.campaign.evolved, Some(record))
    val holders = Vector(ChronicleTask.World, ChronicleTask.Beacon)
      .map(ready.game.current.chronicleTasks)
    val kept = holders.distinct.map(player =>
      val lineage = ready.game.current.players.find(_.player == player)
        .get.lineage
      ready.game.campaign.lineages(lineage).startingAdvisers.size).sum
    // One starting adviser per task, whoever holds it.
    assertEquals(kept, 2)
    assertEquals(ready.game.current.newlyDispossessed.size, 6)
    assert(!ready.game.cardIndex.toOption.get.ids
      .exists(_.isInstanceOf[VisionId]))
    assert(ready.game.current.temporaryHands.values.forall(_.isEmpty))
    val layout = accepted(ChronicleExtraction.layout(ready))
    assertEquals(record.world.map(_.site), layout.empire)
    assert(layout.empire.nonEmpty)
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      ready.game.campaign.baseline, record), Right(()))
    // An Empire-state Chronicle is checked, not played: game start waits
    // for the Empire phase (Chronicle steps design, "Exit and extraction").
    assertEquals(GameStartRules.evolve(catalog, record,
      FirstGameSetupFixture.orders),
      Left(OathViolation.UnsupportedChronicle(
        "a non-empty world requires the Empire")))
    assertEquals(accepted(events.foldLeft[Either[OathViolation, OathState]](
      Right(usurperWakes))((state, event) => state.flatMap(
        rules.evolve(_, event)))), done.state)
