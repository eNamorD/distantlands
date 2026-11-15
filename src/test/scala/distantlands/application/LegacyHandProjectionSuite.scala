package distantlands.application

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** A legacy in a hand is named to the hand's holder alone (catalog batch
  * 13, ruling 6), as a world card in a hand is. */
class LegacyHandProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  test("only the hand's holder identifies a legacy in it"):
    val ready = Table.start.legacyHand(p2, SeerCard).ready
    assert(presentation.identifiesAt(ready, Some(p2), SeerCard.id))
    Vector(Some(p1), Some(p3), None).foreach(viewer => assert(
      !presentation.identifiesAt(ready, viewer, SeerCard.id), s"$viewer"))
