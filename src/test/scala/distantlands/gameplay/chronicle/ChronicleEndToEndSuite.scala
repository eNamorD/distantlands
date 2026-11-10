package oathdigital.gameplay.chronicle

import oathdigital.gameplay.setup.{ChronicleAccounting, FirstGameSetupFixture,
  GameStartRules, LaterGameFixture}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.testkit.{Outcomes, Situation}
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
    val lineage = ready.game.current.player(p1).get.lineage
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
      val lineage = ready.game.current.player(player)
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
    assertEquals(accepted(events.foldLeft[Either[OathViolation, OathState]](
      Right(usurperWakes))((state, event) => state.flatMap(
        rules.evolve(_, event)))), done.state)

  test("game 2 starts from the Chronicle game 1 journals, with the Empire, " +
      "and Setup runs to the Chancellor's first turn"):
    // p1 was crowned, and plays l1 again.
    val orders = LaterGameFixture.orders(record, p1)
    val ready = accepted(GameStartRules.evolve(catalog, record, orders))
    assertEquals(Imperials.chancellor(ready), Some(p1))
    assertEquals(ready.game.current.map.hinterward.take(record.world.size),
      record.world.map(_.site))
    // Every choice takes the first option offered: the Reliquary pick if
    // the record has one, a legal pawn site, an adviser, no reveal.
    // An Exile won game 1, so the broken Empire discarded the Reliquary and
    // Setup asks for no pick.
    assert(record.reliquary.isEmpty)
    val woken = Situation.wake(Situation.rules(catalog), record, orders)
    // A Wake with nothing to decide has already ended, in Act.
    assertEquals(woken.ready.game.current.turn.activePlayer, p1)
    assert(Set(Phase.Wake, Phase.Act)(woken.ready.game.current.turn.phase))
    assertEquals(woken.ready.game.current.tracks.round, 1)
    assert(woken.ready.game.current.players.forall(_.pawnSite.nonEmpty))

  test("game start refuses a crowned Chancellor whose Empire has no site"):
    val withoutEmpire = record.copy(atlasBox = record.world ++ record.atlasBox,
      world = Vector.empty)
    assertEquals(GameStartRules.evolve(catalog, withoutEmpire,
      FirstGameSetupFixture.orders),
      Left(OathViolation.UnsupportedChronicle(
        "an Empire needs at least one site")))
