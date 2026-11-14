package distantlands.gameplay.chronicle

import distantlands.catalog.holding.SeerCard
import distantlands.model._
import distantlands.testkit.Table.{p2, p3}

/** Who holds a legacy in effect when its window is reached (Legacy
  * Chronicle powers design, rulings 1 and 2). */
class ChronicleLegacyRulesSuite extends munit.FunSuite:
  private def won = ChronicleWalk.chronicleWonByP1

  test("the seated player holding a legacy active holds it in effect"):
    assertEquals(ChronicleLegacyRules.holder(won.legacy(p2, SeerCard).ready,
      SeerCard.id), Some(p2))

  test("a dormant legacy, or one nobody holds, has no holder (ruling 1)"):
    assertEquals(ChronicleLegacyRules.holder(
      won.legacy(p2, SeerCard, active = false).ready, SeerCard.id), None)
    assertEquals(ChronicleLegacyRules.holder(won.ready, SeerCard.id), None)

  test("a boxed lineage's active legacy has no holder (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Exile,
      Vector(LegacyState(SeerCard.id, active = true)), Vector.empty)
    val table = won.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(boxed)))))
    assertEquals(ChronicleLegacyRules.holder(table.ready, SeerCard.id), None)

  test("a role condition is read from the roles as they stand (ruling 2)"):
    val imperial = won.imperial(p3, p2).legacy(p2, SeerCard).ready
    val citizens = Set(Role.Citizen)
    assertEquals(ChronicleLegacyRules.holder(imperial, SeerCard.id, citizens),
      Some(p2))
    assertEquals(ChronicleLegacyRules.holder(won.legacy(p2, SeerCard).ready,
      SeerCard.id, citizens), None)
