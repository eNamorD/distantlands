package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Unwrap.{accepted, rejected}
import oathdigital.testkit.RawOperationBatch

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
