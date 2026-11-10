package distantlands.gameplay.powers.banner

import distantlands.gameplay.cards.AncientCitySite
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Yew Staff: its holder holds the Darkest Secret as the card powers they
  * use see it. Each power's own suite tests that it reads the check. */
class YewStaffSuite extends munit.FunSuite:
  private val yewStaff = YewStaffCard.id

  private def holdsForCardPowers(table: Table, player: PlayerId): Boolean =
    CardPowerBanners.holdsDarkestSecret(table.ready, player)

  test("the Darkest Secret's holder holds it, and no one else"):
    val table = Table.start.darkestSecret(Some(p1), 1)
    assert(holdsForCardPowers(table, p1))
    assert(!holdsForCardPowers(table, p2))

  test("a faceup Yew Staff holds it for its holder, wherever the banner is"):
    for banner <- Vector(Some(p2), None) do
      val table = Table.start.darkestSecret(banner, 1).relic(p1, yewStaff)
      assert(holdsForCardPowers(table, p1), banner.toString)

  test("a facedown Yew Staff, or one at a site, holds nothing"):
    val facedown = Table.start.darkestSecret(None, 1)
      .relic(p1, yewStaff, facedown = true)
    assert(!holdsForCardPowers(facedown, p1))
    val atSite = Table.start.darkestSecret(None, 1)
      .pawn(p1, AncientCitySite).relicAt(yewStaff, AncientCitySite)
    assert(!holdsForCardPowers(atSite, p1))
