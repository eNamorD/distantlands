package distantlands.model

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.testkit.Table

class EdificeSiteSuite extends munit.FunSuite:
  private val edifice = GreatMarketCard.id
  private val site = AncientCitySite.id

  test("edificeSite finds an edifice staged on a side, misses the other side"):
    val staged = Table.start.edifice(edifice, EdificeSide.Ruined, at = site)
      .ready.game.current
    assertEquals(staged.edificeSite(edifice, EdificeSide.Ruined), Some(site))
    assertEquals(staged.edificeSite(edifice, EdificeSide.Intact), None)

  test("edificeSite finds nothing when the edifice is not on the board"):
    // The quiet table holds no edifice at any site.
    val cleared = Table.start.ready.game.current
    assertEquals(cleared.edificeSite(edifice, EdificeSide.Ruined), None)
