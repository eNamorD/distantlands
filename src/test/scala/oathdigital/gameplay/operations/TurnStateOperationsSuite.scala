package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The title's side and the Usurper Limiter (Empire slice 3a). */
class TurnStateOperationsSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    executor.executeAll(ready, ops.toVector)

  test("an unchanged holder's Usurper title turns to its Oathkeeper side"):
    val ready = Table.start.oathkeeper(Some(p1), TitleSide.Usurper).ready
    val after = accepted(run(ready, SetOathkeeper(Some(p1))))
    assertEquals(after.game.current.title,
      OathkeeperState(Some(p1), TitleSide.Oathkeeper))

  test("an unchanged holder on the Oathkeeper side is still refused"):
    val ready = Table.start.oathkeeper(Some(p1)).ready
    rejected(run(ready, SetOathkeeper(Some(p1))))

  test("removing the Usurper Limiter ends its limit"):
    val after = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    assert(!after.game.current.tracks.usurperLimited)

  test("the Usurper Limiter cannot be removed twice"):
    val off = accepted(run(Table.start.ready, UsurperLimiterRemoved))
    rejected(run(off, UsurperLimiterRemoved))
