package distantlands.application

import distantlands.catalog.holding.{ArbiterCard, RivalCard, SeerCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Who may identify a legacy (Legacies design, ruling 1): everyone an
  * active or public one, only its owner a dormant one. */
class LegacyIdentitySuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val ready = Table.start.legacy(p1, SeerCard)
    .legacy(p1, ArbiterCard, active = false)
    .publicLegacies(RivalCard).ready
  private val viewers = Vector(Some(p1), Some(p2), None)

  test("every viewer identifies an active legacy and a public one"):
    assert(viewers.forall(viewer =>
      presentation.identifiesAt(ready, viewer, SeerCard.id) &&
        presentation.identifiesAt(ready, viewer, RivalCard.id)))

  test("only its owner identifies a dormant legacy"):
    assert(presentation.identifiesAt(ready, Some(p1), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, Some(p2), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, None, ArbiterCard.id))
