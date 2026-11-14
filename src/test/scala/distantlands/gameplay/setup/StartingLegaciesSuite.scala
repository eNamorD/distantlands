package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.setup.legacies.CircleOfSwordsCard
import distantlands.model._

class StartingLegaciesSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val seated = Vector(l1, l2, l3)
  private val base = LaterGameFixture.chronicle(l1, l2)

  test("a seated Exile holding the legacy active is its holder (ruling 1)"):
    val chronicle = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(chronicle, seated,
      CircleOfSwordsCard.id), Some(l3))

  test("a dormant legacy and a boxed lineage hold nothing (ruling 1)"):
    val dormant = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> false)
    assertEquals(StartingLegacies.holder(dormant, seated,
      CircleOfSwordsCard.id), None)
    val active = SetupLegacyFixture.holding(base, l3, Role.Exile,
      CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(active, seated.take(2),
      CircleOfSwordsCard.id), None)

  test("the stored role decides a role condition (R1)"):
    val citizen = SetupLegacyFixture.holding(base, l2, Role.Citizen,
      CircleOfSwordsCard.id -> true)
    assertEquals(StartingLegacies.holder(citizen, seated,
      CircleOfSwordsCard.id, ChronicleLegacyRules.imperialRoles), Some(l2))
    assertEquals(StartingLegacies.holder(citizen, seated,
      CircleOfSwordsCard.id, StartingLegacies.exileRoles), None)
