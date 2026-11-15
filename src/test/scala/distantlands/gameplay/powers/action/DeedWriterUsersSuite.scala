package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{DeedWriterCard,
  NegotiationSites}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Deed Writer (146): who may use it to put sites into a deal (catalog
  * batch 12, rulings 18 and 27). */
class DeedWriterUsersSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val everyone = Vector(p1, p2, p3)
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City. */
  private def apart: Table = Table.start.pawn(p1, at = peaks)
    .pawn(p2, at = DunesSite.id).pawn(p3, at = AncientCitySite.id)

  private def writers(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty,
      among: Vector[PlayerId] = everyone): Vector[PlayerId] =
    NegotiationSites.deedWriterUsers(catalog, table.ready, among,
      restrictions)

  test("Deed Writer's faceup holder may use it"):
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id)), Vector(p2))

  test("a facedown Deed Writer, or a holder outside the deal, gives no " +
      "user"):
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id,
      facedown = true)), Vector.empty[PlayerId])
    assertEquals(writers(apart.adviser(p2, DeedWriterCard.id),
      among = Vector(p1, p3)), Vector.empty[PlayerId])

  test("City Wall stops its ruler's enemy at its site from using Deed " +
      "Writer"):
    val walled = apart.adviser(p1, DeedWriterCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(writers(walled), Vector(p1))
    assertEquals(writers(walled, blockers), Vector.empty[PlayerId])

  test("Giant Python at its holder's site leaves a held Deed Writer " +
      "usable"):
    // Giant Python stops the other cards at its site; an adviser is not
    // at a site.
    val python = apart.adviser(p1, DeedWriterCard.id)
      .denizen(GiantPythonCard.id, at = peaks)
    assertEquals(writers(python, blockers), Vector(p1))
