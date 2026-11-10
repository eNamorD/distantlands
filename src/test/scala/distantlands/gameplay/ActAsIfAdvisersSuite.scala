package distantlands.gameplay

import distantlands.gameplay.walker.{ProcedureWalker, WalkerCompleted}
import distantlands.model._
import distantlands.model.TestGameFixtures.{blueId, redAndBlue}
import distantlands.testkit.{Outcomes, RawOperationBatch, RecordedOps, Table}
import distantlands.testkit.Outcomes.{finished, parked, recorded}
import distantlands.testkit.ProcedureWalkerFixture._
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `ActAsIfAdvisers` (catalog batch 11, slice 3): it records the player
  * whose advisers the walk's actor acts with, a parked walk keeps it, and
  * the walk's end clears it, live and on replay. */
class ActAsIfAdvisersSuite extends munit.FunSuite:
  /** The walker fixture's game with blue seated beside red, the walk's
    * actor. */
  private val redBesideBlue = redAndBlue()
  /** The seated player other than the walk's actor. */
  private val chosen = blueId

  private def run(state: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(state, ops.toVector)

  test("it records the chosen player"):
    val acting = accepted(run(Table.start.ready, ActAsIfAdvisers(p2)))
    assertEquals(acting.game.current.walkerAsIfAdvisers, Some(p2))

  test("it refuses the walk's actor and an unseated player"):
    // p1 acts on Table.start, which has no walk entry.
    rejected(run(Table.start.ready, ActAsIfAdvisers(p1)))
    rejected(run(Table.start.ready, ActAsIfAdvisers(PlayerId("nobody"))))

  test("a walk parked after it holds the player, and its events rebuild it"):
    val tree: Operation = Sequence(ActAsIfAdvisers(chosen), decide)
    val park = parked(ProcedureWalker.advance(redBesideBlue, tree, None,
      noPowers))
    assertEquals(RecordedOps.of(recorded(park.events.head)),
      Vector[CoreOperation](ActAsIfAdvisers(chosen)))
    assertEquals(applyEvents(redBesideBlue, park.events).game.current
      .walkerAsIfAdvisers, Some(chosen))

  test("the walk's end clears it, live and on replay"):
    val done = finished(ProcedureWalker.advance(redBesideBlue,
      ActAsIfAdvisers(chosen), None, noPowers))
    assertEquals(done.treeless.game.current.walkerAsIfAdvisers, None)
    val acting = accepted(run(redBesideBlue, ActAsIfAdvisers(chosen)))
    val completed = accepted(ProcedureWalker.applyRecorded(
      OathState.Ready(acting), WalkerCompleted(ActionRef.Trade)))
    assertEquals(Outcomes.ready(completed).game.current.walkerAsIfAdvisers,
      None)
