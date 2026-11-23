package distantlands.model

import distantlands.testkit.Table

/** Every lineage in the Chronicle as it stands (Chronicle Details
  * implementation ruling R1). */
class StandingLineagesSuite extends munit.FunSuite:
  private val boxedCitizenHoldingADormantLegacy = LineageState(
    LineageId("a-boxed"), None, Role.Citizen,
    Vector(LegacyState(LegacyId("L22"), active = false)), Vector.empty)
  private val boxedChancellor = LineageState(LineageId("z-boxed"), None,
    Role.Chancellor, Vector.empty, Vector.empty)

  test("the seated lineages' live records and the unseated ones' records " +
      "in the Chronicle the game started from, each with the role this " +
      "game gave it in the box, by id"):
    val campaign = Table.start.ready.game.campaign
    val seated = campaign.lineages.values.toVector.sortBy(_.id.value)
    // A seated lineage's baseline record is stale: the live one wins.
    val staleSeated = seated.head.copy(role = Role.Chancellor)
    val standing = campaign.copy(
      baseline = campaign.baseline.copy(lineages = campaign.baseline.lineages
        ++ Vector(staleSeated, boxedCitizenHoldingADormantLegacy,
          boxedChancellor)),
      boxRoles = Map(boxedChancellor.id -> Role.Exile)).standingLineages
    assertEquals(standing, boxedCitizenHoldingADormantLegacy +: seated :+
      boxedChancellor.copy(role = Role.Exile))
