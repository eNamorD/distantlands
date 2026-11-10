package oathdigital.application

import oathdigital.gameplay.powers.wake.TrackerCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model.{PlayerId, ReadyGame}
import oathdigital.protocol.projection.{CardDetailsProjection,
  TrackedPawnProjection}
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** The pawn a Tracker's holder chose reaches the table on the card, for
  * every viewer, since the choice was made openly (catalog batch 10, slice
  * 14). */
class TrackerProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  /** p1's Tracker as `viewer` sees it on p1's board. */
  private def trackerOfP1(ready: ReadyGame, viewer: PlayerId)
      : CardDetailsProjection =
    presentation.playerBoards(ready, Some(viewer))
      .find(_.playerId == p1.value).get.advisers
      .find(_.cardId == TrackerCard.id.value).get

  test("p1's Tracker shows the pawn p1 chose, in its colour, to every " +
      "viewer"):
    val ready = Table.start.adviser(p1, TrackerCard.id).trackedPawn(p1, p2)
      .ready
    val color = ready.playerColors(p2)
    Vector(p1, p2, p3).foreach(viewer =>
      assertEquals(trackerOfP1(ready, viewer).tracking,
        Some(TrackedPawnProjection(p2.value, color,
          s"Tracking ${color.key.capitalize}"))))

  test("a facedown Tracker hidden from p2 keeps its record hidden too"):
    val ready = Table.start.adviser(p1, TrackerCard.id, facedown = true)
      .trackedPawn(p1, p2).ready
    val advisers = presentation.playerBoards(ready, Some(p2))
      .find(_.playerId == p1.value).get.advisers
    assert(advisers.nonEmpty)
    assert(advisers.forall(_.tracking.isEmpty), advisers)

  test("a Tracker with no record shows none"):
    val ready = Table.start.adviser(p1, TrackerCard.id).ready
    assertEquals(trackerOfP1(ready, p1).tracking, None)
