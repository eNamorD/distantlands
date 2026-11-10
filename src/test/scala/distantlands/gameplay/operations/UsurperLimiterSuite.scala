package distantlands.gameplay.operations

import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.{accepted, rejected}
import distantlands.testkit.RawOperationBatch

/** The Usurper Limiter (Empire slice 3a, ruling R5). */
class UsurperLimiterSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  test("removing the Usurper Limiter ends its limit"):
    val after = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    assert(!after.game.current.tracks.usurperLimited)

  test("the Usurper Limiter cannot be removed twice"):
    val off = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    rejected(run(off, UsurperLimiterRemoved))
