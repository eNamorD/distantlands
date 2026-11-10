package distantlands.gameplay

import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The order of a round (CR p. 17) and a Chancellor who appears mid-round
  * (ruling R4). */
class TurnOrderSuite extends munit.FunSuite:
  test("a round runs from the player recorded when it began"):
    val ready = Table.start.roundStarter(p2).ready
    assertEquals(TurnOrder.order(ready), Vector(p2, p3, p1))

  test("a Chancellor who appears mid-round leaves the round's order alone"):
    // p1 began the round; p3 becomes the Chancellor during it.
    val ready = Table.start.imperial(p3).roundStarter(p1).ready
    assertEquals(TurnOrder.order(ready), Vector(p1, p2, p3))
    assertEquals(TurnOrder.nextRoundStart(ready), p3)

  test("with no Chancellor the next round starts with the first player"):
    val ready = Table.start.roundStarter(p2).ready
    assertEquals(TurnOrder.nextRoundStart(ready), ready.setup.firstPlayer)
