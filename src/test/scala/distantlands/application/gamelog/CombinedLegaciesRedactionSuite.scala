package distantlands.application.gamelog

import distantlands.application.GamePresentationProjector
import distantlands.gameplay.setup.{LaterGameFixture, LegacyDeal}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** Who the combined six-power game names its hidden cards to, in the Game
  * Log and the projection (Legacy Setup powers, whole-feature review): p3
  * holds Secret Society and Quest to Distant Lands, p2 Matchmaker. */
class CombinedLegaciesRedactionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private lazy val (script, record) = LaterGameLogScripts.combinedLegacies
  private lazy val ready: ReadyGame = script.history.steps.last.after match
    case OathState.Ready(ready) => ready
    case other => fail(s"the script did not end ready: $other")
  private val viewers = Vector(Some(p1), Some(p2), Some(p3), None)

  private def named(viewer: Option[PlayerId]): Set[String] =
    "\\[card:([^|]+)\\|".r.findAllMatchIn(GoldenLog.render(
      LogScripts.format(script, viewer))).map(_.group(1)).toSet

  /** The Vision Secret Society gave p3, still facedown. */
  private lazy val vision: CardId = ready.game.current.player(p3).toVector
    .flatMap(_.advisers).collectFirst {
      case VisionState(id, Orientation.FaceDown) => id
    }.getOrElse(fail("p3 holds no facedown Vision"))

  test("the Vision is named in no log, and identified by its holder alone"):
    viewers.foreach(viewer => assert(!named(viewer).contains(vision.value),
      viewer))
    assert(presentation.identifiesAt(ready, Some(p3), vision))
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(!presentation.identifiesAt(ready, viewer, vision), viewer))

  test("no log names a legacy Matchmaker's holder kept for another player"):
    val orders = LaterGameFixture.orders(record, p1)
    val deal = LegacyDeal.of(record, orders.participants)
    val drawn = Vector(p1, p3).flatMap(deal.drawn(orders.legacyOrder, _))
      .map(_.value).toSet
    assert(drawn.nonEmpty)
    viewers.foreach(viewer => assertEquals(named(viewer).intersect(drawn),
      Set.empty[String], viewer))

  test("the Dispossessed and the swapped cards are named to nobody but p3, " +
      "and identified by p3 alone"):
    val dispossessed = ready.game.campaign.dispossessed.map(_.value).toSet
    val waiting = record.dispossessed.map(_.value).toSet
    val swapped = (dispossessed ++ waiting) -- dispossessed.intersect(waiting)
    assertEquals(swapped.size, 2)
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assertEquals(named(viewer).intersect(dispossessed ++ waiting),
        Set.empty[String], viewer))
    val taken = record.dispossessed.filterNot(card =>
      ready.game.campaign.dispossessed.contains(card))
    assertEquals(taken.size, 1)
    assert(presentation.identifiesAt(ready, Some(p3), taken.head))
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(!presentation.identifiesAt(ready, viewer, taken.head), viewer))
