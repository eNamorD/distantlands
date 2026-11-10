package distantlands.gameplay.powers.banner

import distantlands.gameplay.{RuleSourceFace, RuleSourceIndex}
import distantlands.gameplay.powers.PowerFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p1

/** The source index lists each banner face's powers, and the reviewed
  * catalog still audits every source the index lists.
  */
class BannerFaceSourcesSuite extends munit.FunSuite:
  import PowerFixture.quietTable

  private def darkest(ready: ReadyGame) = RuleSourceIndex.enumerate(catalog,
    ready).filter(_.source == RuleSourceRef.Banner(Banner.DarkestSecret.key))

  test("the Wandering Flame face lists its two powers, and Festival its own"):
    val flame = darkest(quietTable).head
    assertEquals(flame.face, RuleSourceFace.WanderingFlame)
    assertEquals(flame.powerIds, Vector(WanderingFlameMove.id,
      WanderingFlamePlace.id))
    val onFestival = Table.start.darkestSecret(Some(p1), 1,
      DarkestSecretFace.Festival).ready
    val festival = darkest(onFestival).head
    assertEquals(festival.powerIds, Vector(PowerId("banner.darkest-secret.festival")))
