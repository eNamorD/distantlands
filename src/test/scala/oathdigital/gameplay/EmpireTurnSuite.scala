package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.phases.rest.BeginRestProcedure
import oathdigital.model._
import oathdigital.model.OathEvent._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

class EmpireTurnSuite extends munit.FunSuite:
  /** p2 is the Chancellor and p3 a Citizen; p1, the first player, is an
    * Exile. */
  private def chancellorSecondInSeat: Table = Table.start.imperial(p2, p3)

  /** The Usurper Limiter is off in an Empire game (ruling R5), so only the
    * Imperial guard can stop a flip. */
  private def usurperLimiterOff(table: Table): Table =
    table.update(r => r.updateCurrent(c => c.copy(tracks =
      c.tracks.copy(usurperLimited = false))))

  test("a round starts with the Chancellor"):
    val ready = chancellorSecondInSeat.ready
    assertEquals(TurnOrder.roundStart(ready), p2)
    assertEquals(TurnOrder.order(ready), Vector(p2, p3, p1))

  test("with no Chancellor a round starts with the first player"):
    assertEquals(TurnOrder.order(Table.start.ready), Vector(p1, p2, p3))

  test("an Imperial Oathkeeper does not flip to Usurper"):
    val state = usurperLimiterOff(chancellorSecondInSeat
      .turn(p3, Phase.Wake).oathkeeper(Some(p3))).state
    assertEquals(StateBasedEvaluation.atWake(state), Right(None))

  test("an Exile Oathkeeper still flips with an Empire at the table"):
    val state = usurperLimiterOff(chancellorSecondInSeat
      .turn(p1, Phase.Wake).oathkeeper(Some(p1))).state
    assertEquals(StateBasedEvaluation.atWake(state),
      Right(Some(UsurperFlipped(p1))))

  test("an Imperial Usurper-side holder wins nothing at Wake"):
    // Built to break the invariant on purpose; Wake must still not read it
    // as an Exile's Usurper victory.
    val usurper = Table.start.turn(p1, Phase.Wake)
      .oathkeeper(Some(p1), TitleSide.Usurper)
    assertEquals(StateBasedEvaluation.atWake(usurper.state),
      Right(Some(UsurperVictory(p1))))
    assertEquals(StateBasedEvaluation.atWake(
      usurper.imperial(p2, p1).state), Right(None))

  test("an Imperial player wins no Vision at Wake"):
    // The People's goal counts no sites, so only the Imperial guard can
    // stop a Citizen holding it.
    val rebelling = Table.start.turn(p1, Phase.Wake)
      .revealedVision(p1, VisionRules.Rebellion)
      .peoplesFavor(Some(p1), 1).visionsDrawn(3)
    assertEquals(StateBasedEvaluation.visionAtWake(rebelling.state),
      Right(Some(VisionVictory(p1, Vector(VisionRules.Rebellion)))))
    assertEquals(StateBasedEvaluation.visionAtWake(
      rebelling.imperial(p2, p1).state), Right(None))

  test("Rest accepts an Imperial player"):
    val ready = chancellorSecondInSeat.turn(p2, Phase.Act).ready
    assertEquals(BeginRestProcedure.validateSupportedState(
      oathdigital.gameplay.setup.FirstGameSetupFixture.catalog, ready),
      Right(()))
