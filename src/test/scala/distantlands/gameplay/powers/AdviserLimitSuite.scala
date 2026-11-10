package distantlands.gameplay.powers

import distantlands.gameplay.actions.{AdviserFace, CardPlayRules, VisionRules}
import distantlands.gameplay.powers.action.PiedPiperCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.cardplay.{FamilyWagonCard, LostTapestryCard}
import distantlands.gameplay.powers.rest.SilverTongueCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.Orientation
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class AdviserLimitSuite extends munit.FunSuite:
  private val tongue = SilverTongueCard.id

  // The default limit is three advisers; a faceup Silver Tongue lowers its
  // holder's to two.
  test("a faceup Silver Tongue lowers its holder's limit to two, and no one " +
      "else's"):
    val ready = Table.start.adviser(p1, tongue).ready
    assertEquals(AdviserLimit.of(catalog, ready, p1), 2)
    Vector(p2, p3).foreach(other =>
      assertEquals(AdviserLimit.of(catalog, ready, other), 3))

  test("a facedown Silver Tongue changes nothing"):
    val ready = Table.start.adviser(p1, tongue, facedown = true).ready
    assertEquals(AdviserLimit.of(catalog, ready, p1), 3)

  test("a Lost Tapestry holder's facedown Visions do not count toward the " +
      "limit, and only while it is faceup"):
    val faith = VisionRules.Faith
    def held(facedownTapestry: Boolean) = Table.start
      .relic(p1, LostTapestryCard, facedown = facedownTapestry)
      .adviser(p1, faith, facedown = true).adviser(p1, WrestlersCard).ready
    assertEquals(AdviserLimit.ignored(held(false), p1),
      Vector(AdviserFace(faith, Orientation.FaceDown)))
    assertEquals(AdviserLimit.towardLimit(held(false), p1), 1)
    assertEquals(AdviserLimit.towardLimit(held(true), p1), 2)

  test("a faceup Pied Piper does not count toward its holder's limit; " +
      "facedown it does"):
    val piper = PiedPiperCard.id
    def held(facedown: Boolean) = Table.start
      .adviser(p1, piper, facedown = facedown).adviser(p1, WrestlersCard).ready
    assertEquals(AdviserLimit.ignored(held(false), p1),
      Vector(AdviserFace(piper, Orientation.FaceUp)))
    assertEquals(AdviserLimit.towardLimit(held(false), p1), 1)
    assertEquals(AdviserLimit.towardLimit(held(true), p1), 2)

  test("a faceup Family Wagon gives its holder no limit; a lowering power " +
      "still applies, and facedown it lifts nothing"):
    val wagon = FamilyWagonCard.id
    assertEquals(AdviserLimit.of(catalog,
      Table.start.adviser(p1, wagon).ready, p1), CardPlayRules.Unbounded)
    assertEquals(AdviserLimit.of(catalog,
      Table.start.adviser(p1, wagon).adviser(p1, tongue).ready, p1), 2)
    assertEquals(AdviserLimit.of(catalog,
      Table.start.adviser(p1, wagon, facedown = true).ready, p1), 3)
