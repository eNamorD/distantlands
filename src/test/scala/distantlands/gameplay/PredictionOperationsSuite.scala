package distantlands.gameplay

import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Conspirator's prediction store and its resolution (Legacy in-play
  * powers rulings 25-27): `WritePrediction` records a holder's round and
  * winner; `ResolvePrediction` clears it, and when it is fulfilled makes
  * the holder the winner under `VictoryKind.Conspirator`. */
class PredictionOperationsSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)
      : Either[?, ReadyGame] =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).map(_.state)

  /** The quiet three-player table. */
  private val table: ReadyGame = Table.start.ready
  /** The same, its game won by p2 as the Chancellor's Successor. */
  private val won: ReadyGame = table.updateCurrent(_.copy(result =
    Some(GameResult(p2, VictoryKind.Successor))))

  test("writing stores the holder's prediction, once"):
    val written = accepted(applied(table, WritePrediction(p1, 6, p2)))
    assertEquals(written.game.current.predictions,
      Map(p1 -> Prediction(6, p2)))
    assert(applied(written, WritePrediction(p1, 5, p2)).isLeft)

  test("a round outside 1 to 8, or a winner not seated, is refused"):
    assert(applied(table, WritePrediction(p1, 0, p2)).isLeft)
    assert(applied(table, WritePrediction(p1, 9, p2)).isLeft)
    assert(applied(table, WritePrediction(p1, 5, PlayerId("p9"))).isLeft)

  test("a fulfilled prediction makes its holder the winner by Conspirator " +
      "and empties the store"):
    val after = accepted(applied(won, WritePrediction(p1, 6, p2),
      ResolvePrediction(p1, fulfilled = true)))
    assertEquals(after.game.current.result,
      Some(GameResult(p1, VictoryKind.Conspirator)))
    assertEquals(after.game.current.predictions, Map.empty)

  test("an unfulfilled one only empties the store"):
    val after = accepted(applied(won, WritePrediction(p1, 6, p2),
      ResolvePrediction(p1, fulfilled = false)))
    assertEquals(after.game.current.result, won.game.current.result)
    assertEquals(after.game.current.predictions, Map.empty)

  test("a fulfilled one with no result, or with no prediction, is refused"):
    assert(applied(table, WritePrediction(p1, 6, p2),
      ResolvePrediction(p1, fulfilled = true)).isLeft)
    assert(applied(won, ResolvePrediction(p1, fulfilled = false)).isLeft)
