package distantlands.gameplay.phases.rest

import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class ImperialSupplySuite extends munit.FunSuite:
  /** p1 is the Chancellor and p2 a Citizen. */
  private def empire: Table = Table.start.imperial(p1, p2)

  test("the Chancellor's bands read the purple bank"):
    // 24 purple, 6 on the two Imperial boards: 18 banked gives 6.
    val ready = empire.supply(p1, 0).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p1), Right(6))
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p1), Right(6))

  test("each Chancellor band"):
    val gains = Vector(18 -> 6, 17 -> 5, 11 -> 5, 10 -> 4, 4 -> 4, 3 -> 3,
      0 -> 3).map((banked, _) => banked -> FinishRestProcedure
        .ChancellorSupply.baseSupplyFor(banked))
    assertEquals(gains, Vector(18 -> Some(6), 17 -> Some(5), 11 -> Some(5),
      10 -> Some(4), 4 -> Some(4), 3 -> Some(3), 0 -> Some(3)))

  test("a Citizen matches the Chancellor's Supply and keeps their own surplus (R3)"):
    val ready = empire.supply(p1, 5).supply(p2, 1).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p2), Right(5))
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p2), Right(6))

  test("a Citizen's refresh is capped at 7"):
    val ready = empire.supply(p1, 6).supply(p2, 3).ready
    assertEquals(FinishRestProcedure.supplyAfterRest(ready, p2), Right(7))

  test("an Exile at an Imperial table keeps the Exile bands"):
    val ready = empire.supply(p3, 0).warbandsInBank(p3, 9).ready
    assertEquals(FinishRestProcedure.supplyGainAtRest(ready, p3), Right(6))

  test("a Citizen has no band table of their own"):
    assertEquals(FinishRestProcedure.supplyRules(empire.ready, p2), None)
