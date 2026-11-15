package distantlands.application

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.{ChronicleWalk, ShapeFoundation}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Outcomes
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.present

/** The Shape Foundation question as the table sees it (Foundations design,
  * slice 7). p3 holds the Beacon. */
class ShapeFoundationProjectionSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** p1's won game after the Sun, Foundation I on Wide Horizons and
    * Foundation VI on the Festival, VI altered by The Needle, which a
    * lineage in the box holds active; walked to Shape Foundation. */
  private val parkedOnShapeFoundation: ReadyGame =
    val needle = LegacyId("L05")
    val table = ChronicleWalk.throneHeldByChancellorP1
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .update(_.updateCampaign(campaign => campaign.copy(
        baseline = campaign.baseline.copy(lineages = Vector(LineageState(
          LineageId("lineage:boxed"), None, Role.Exile,
          Vector(LegacyState(needle, active = true)), Vector.empty))),
        foundations = campaign.foundations.updated(FoundationNumber.VI,
          FoundationState(FoundationFace.Altered, Set(needle))))))
    Outcomes.ready(ChronicleWalk.beaconWalkedTo(rules, table,
      ShapeFoundation.decisionId).state)

  private val projector = new WalkerDecisionProjector(catalog,
    new GamePresentationProjector(catalog))

  test("the holder sees the question: Keep, then each altered face, the " +
      "one an active legacy holds unavailable with its reason"):
    val own = present(projector.project(ScopedProjectionContext(
      parkedOnShapeFoundation, Some(p3))), "p3's decision")
    assertEquals(own.decisionId, ShapeFoundation.decisionId)
    assertEquals(own.query.flatMap(_.heading), Some(ShapeFoundation.heading))
    assertEquals(own.query.toVector.flatMap(_.offeredOptions)
      .map(option => (option.id, option.label, option.state, option.reason)),
      Vector(
        ("keep", "Keep every foundation as it lies", "available", None),
        ("restore-i", "Restore Foundation I to Imperial Maps", "available",
          None),
        ("restore-vi", "Restore Foundation VI to Wandering Flame",
          "unavailable", Some("An active legacy alters it: The Needle"))))

  test("no one else is asked"):
    Vector(p1, p2).foreach(viewer => assertEquals(projector.project(
      ScopedProjectionContext(parkedOnShapeFoundation, Some(viewer))), None))
