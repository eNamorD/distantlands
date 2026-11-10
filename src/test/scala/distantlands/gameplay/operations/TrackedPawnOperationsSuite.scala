package distantlands.gameplay.operations

import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** The Tracker's record across turns (catalog batch 10, slice 14):
  * `TrackPawn` writes the holder's choice and `ClearTrackedPawn` removes
  * it. */
class TrackedPawnOperationsSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  test("tracking a pawn records it under its holder, replacing any older " +
      "choice"):
    val first = accepted(run(Table.start.ready, TrackPawn(p1, p2)))
    assertEquals(first.game.current.trackedPawns, Map(p1 -> p2))
    val again = accepted(run(first, TrackPawn(p1, p3), TrackPawn(p2, p1)))
    assertEquals(again.game.current.trackedPawns, Map(p1 -> p3, p2 -> p1))

  test("a player cannot track their own pawn, or an unseated one"):
    rejected(run(Table.start.ready, TrackPawn(p1, p1)))
    rejected(run(Table.start.ready, TrackPawn(p1, PlayerId("nobody"))))
    rejected(run(Table.start.ready, TrackPawn(PlayerId("nobody"), p1)))

  test("clearing removes only the holder's record, and needs one"):
    val both = Table.start.trackedPawn(p1, p2).trackedPawn(p2, p1).ready
    val cleared = accepted(run(both, ClearTrackedPawn(p1)))
    assertEquals(cleared.game.current.trackedPawns, Map(p2 -> p1))
    rejected(run(cleared, ClearTrackedPawn(p1)))
