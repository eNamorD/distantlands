package distantlands.gameplay

import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ruined Temple's pledge store and the legacies Stone Baths keeps dormant
  * (catalog batch 13, rulings 14 and 17): `PledgeTemple` records a
  * player's pledge for the game, and `LegaciesKeptDormant` records the
  * kept legacies, so no reactivation takes them for newly dormant. */
class TempleAndBathsOperationsSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)
      : Either[?, ReadyGame] =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).map(_.state)

  /** The quiet three-player table. */
  private val table: ReadyGame = Table.start.ready

  test("a pledge records its player, once"):
    val pledged = accepted(applied(table, PledgeTemple(p1)))
    assertEquals(pledged.game.current.templePledges, Set(p1))
    assert(applied(pledged, PledgeTemple(p1)).isLeft)

  test("a player who is not seated cannot pledge"):
    assert(applied(table, PledgeTemple(PlayerId("p9"))).isLeft)

  test("kept legacies are recorded, each once"):
    val kept = accepted(applied(table,
      LegaciesKeptDormant(Vector(SeerCard.id))))
    assertEquals(kept.game.current.keptDormant, Set(SeerCard.id))
    assert(applied(kept, LegaciesKeptDormant(Vector(SeerCard.id))).isLeft)
    assert(applied(table,
      LegaciesKeptDormant(Vector(SeerCard.id, SeerCard.id))).isLeft)
