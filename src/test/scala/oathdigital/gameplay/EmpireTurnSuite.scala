package oathdigital.gameplay

import oathdigital.gameplay.phases.rest.BeginRestProcedure
import oathdigital.model._
import oathdigital.model.OathEvent._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

class EmpireTurnSuite extends munit.FunSuite:
  /** p2 is the Chancellor and p3 a Citizen; p1, the first player, is an
    * Exile. */
  private def chancellorSecondInSeat: Table = Table.start.imperial(p2, p3)

  test("a round starts with the Chancellor"):
    val ready = chancellorSecondInSeat.ready
    assertEquals(TurnOrder.roundStart(ready), p2)
    assertEquals(TurnOrder.order(ready), Vector(p2, p3, p1))

  test("with no Chancellor a round starts with the first player"):
    assertEquals(TurnOrder.order(Table.start.ready), Vector(p1, p2, p3))

  test("an Imperial Oathkeeper does not flip to Usurper"):
    val state = chancellorSecondInSeat.turn(p3, Phase.Wake)
      .oathkeeper(Some(p3)).state
    assertEquals(StateBasedEvaluation.atWake(state), Right(None))

  test("an Exile Oathkeeper still flips with an Empire at the table"):
    // The Usurper Limiter is off in an Empire game (ruling R5).
    val state = chancellorSecondInSeat.turn(p1, Phase.Wake)
      .oathkeeper(Some(p1))
      .update(r => r.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(usurperLimited = false)))).state
    assertEquals(StateBasedEvaluation.atWake(state),
      Right(Some(UsurperFlipped(p1))))

  test("Rest accepts an Imperial player"):
    val ready = chancellorSecondInSeat.turn(p2, Phase.Act).ready
    assertEquals(BeginRestProcedure.validateSupportedState(
      oathdigital.gameplay.setup.FirstGameSetupFixture.catalog, ready),
      Right(()))
