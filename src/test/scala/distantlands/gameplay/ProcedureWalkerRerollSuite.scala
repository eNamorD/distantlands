package oathdigital.gameplay

import oathdigital.gameplay.walker.{ProcedureWalker, WalkerOutcome}
import oathdigital.testkit.RecordedOps
import oathdigital.model._
import oathdigital.testkit.ProcedureWalkerFixture._

/** A declared `Reroll` on the walker: its faces come from the dice source
  * and ride the recorded operation, so replay applies them without rolling
  * (catalog batch 7, slice 1). */
class ProcedureWalkerRerollSuite extends munit.FunSuite:
  import DefenseDieFace.{Blank, TwoShields}

  private val tree: Operation = Sequence(
    ModifyDicePool(recoverPool, 2),
    Roll(recoverPool, DiceSpec(DiceKind.Defense), RollMode.Automatic),
    Reroll(recoverPool, Vector(1)))

  test("a declared reroll takes its faces from the dice source and " +
      "records them"):
    val dice = WalkerDiceFixture.scripted(Vector(Blank, Blank),
      Vector(TwoShields))
    ProcedureWalker.advance(ready, tree, None, noPowers, dice) match
      case Right(WalkerOutcome.Finished(end, events)) =>
        val recorded = RecordedOps.all(events)
        assert(recorded.contains(Reroll(recoverPool, Vector(1),
          Some(Vector(TwoShields)))))
        val outcome = end.game.current.rollOutcomes(recoverPool)
        assertEquals(outcome.faces, Vector[DieFace](Blank, TwoShields))
        assertEquals(outcome.score, 2)
      case other => fail(s"expected the tree to finish, got $other")


  test("a reroll of a round die asks the dice source for a round die"):
    // These dice answer only a round pool of one, so asking for the wrong
    // kind fails the walk.
    val dice = WalkerDiceFixture.dice(round = Vector(RoundDieFace.Five))
    val pool = PoolKey("shattered-portal")
    val rerolled = Sequence(ModifyDicePool(pool, 1),
      Roll(pool, DiceSpec(DiceKind.Round), RollMode.Automatic),
      Reroll(pool, Vector(0)))
    ProcedureWalker.advance(ready, rerolled, None, noPowers, dice) match
      case Right(WalkerOutcome.Finished(end, events)) =>
        assert(RecordedOps.all(events).contains(Reroll(pool, Vector(0),
          Some(Vector(RoundDieFace.Five)))))
        assertEquals(end.game.current.rollOutcomes(pool).score, 5)
      case other => fail(s"expected the tree to finish, got $other")
