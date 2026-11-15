package distantlands.application

import distantlands.gameplay.powers.action.{AncientWrit, AncientWritCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{MatchmakerCard, PopulistCard,
  SeerCard, WorldCrafterCard}
import distantlands.model._
import distantlands.testkit.{PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Who sees the legacies Matchmaker's holder draws for Ancient Writ's
  * holder (catalog batch 13, rulings 6 and 9): the drawer alone while they
  * choose; then the kept legacy its owner alone, and the discarded ones
  * nobody. p1 uses the Writ and p3 holds Matchmaker. */
class AncientWritProjectionSuite extends munit.FunSuite:
  import PhasePowerHarness.{after, answer, pick, use}

  private val presentation = new GamePresentationProjector(catalog)
  private val drawn = Vector(SeerCard.id, PopulistCard.id, WorldCrafterCard.id)
  private val viewers = Vector(Some(p1), Some(p2), Some(p3), None)

  private val used = accepted(use(Table.start.turn(p1, Phase.Act)
    .relic(p1, AncientWritCard)
    .legacyDeck(SeerCard, PopulistCard, WorldCrafterCard)
    .legacy(p3, MatchmakerCard).ready, AncientWrit,
    DecisionOptionRef.Relic(AncientWritCard.id)))

  test("while Matchmaker's holder chooses, only they identify the draw"):
    drawn.foreach { legacy =>
      assert(presentation.identifiesAt(after(used), Some(p3), legacy), legacy)
      Vector(Some(p1), Some(p2), None).foreach(viewer => assert(
        !presentation.identifiesAt(after(used), viewer, legacy),
        s"$viewer $legacy"))
    }
    assertEquals(PhasePowerHarness.offered(used, p3).map(_.map(_._2)),
      Some(drawn.map(_.value)))

  test("once kept, the owner alone identifies the kept legacy and nobody " +
      "the discarded ones"):
    val kept = after(accepted(answer(used, p3, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(PopulistCard.id)))))
    assert(presentation.identifiesAt(kept, Some(p1), PopulistCard.id))
    Vector(Some(p2), Some(p3), None).foreach(viewer => assert(
      !presentation.identifiesAt(kept, viewer, PopulistCard.id), s"$viewer"))
    Vector(SeerCard.id, WorldCrafterCard.id).foreach(legacy =>
      viewers.foreach(viewer => assert(
        !presentation.identifiesAt(kept, viewer, legacy), s"$viewer $legacy")))
