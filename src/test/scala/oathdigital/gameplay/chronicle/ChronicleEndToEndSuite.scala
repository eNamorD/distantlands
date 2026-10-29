package oathdigital.gameplay.chronicle

import oathdigital.gameplay.setup.{ChronicleAccounting, FirstGameSetupFixture,
  GameStartRules}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.testkit.Outcomes
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** A game won for real, through Start Chronicle, the Sun, Throne, World,
  * Beacon and Stars, to the journaled Chronicle. p1 is the Usurper as their Wake begins. */
class ChronicleEndToEndSuite extends munit.FunSuite:
  import EvolvedChronicleFixture.{done, record, rules, usurperWakes, won}

  test("the winner's Chronicle runs every task to a journaled record that " +
      "passes game start's accounting, and replays"):
    val events = won.events ++ done.events
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    val ready = Outcomes.ready(done.state)
    assertEquals(record.name, ready.game.campaign.baseline.name)
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

  test("game start refuses the crowned Chancellor rather than seating them " +
      "as an Exile, even with the Empire's sites back in the atlas"):
    val withoutEmpire = record.copy(atlasBox = record.world ++ record.atlasBox,
      world = Vector.empty)
    assertEquals(GameStartRules.evolve(catalog, withoutEmpire,
      FirstGameSetupFixture.orders),
      Left(OathViolation.UnsupportedChronicle(
        "Imperial roles require the Empire")))
