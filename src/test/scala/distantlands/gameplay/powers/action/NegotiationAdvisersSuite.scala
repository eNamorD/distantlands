package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, NegotiationAdvisers}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Council Arbiter (200) and Festival District (E21 intact): who may use
  * each to put advisers into a deal (catalog batch 12, rulings 14, 15 and
  * 26). */
class NegotiationAdvisersSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val city = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val everyone = Vector(p1, p2, p3)
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City. */
  private def apart: Table = Table.start
    .pawn(p1, at = peaks).pawn(p2, at = dunes).pawn(p3, at = city)

  private def arbiters(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty,
      among: Vector[PlayerId] = everyone): Vector[PlayerId] =
    NegotiationAdvisers.arbiterUsers(catalog, table.ready, among,
      restrictions)

  private def festivalGoers(table: Table,
      restrictions: Vector[PowerUseRestriction] = Vector.empty)
      : Vector[PlayerId] =
    NegotiationAdvisers.festivalUsers(catalog, table.ready, everyone,
      restrictions)

  test("Council Arbiter's faceup holder may use it"):
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id)),
      Vector(p2))

  test("a facedown Council Arbiter, or a holder outside the deal, gives " +
      "no user"):
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id,
      facedown = true)), Vector.empty[PlayerId])
    assertEquals(arbiters(apart.adviser(p2, CouncilArbiterCard.id),
      among = Vector(p1, p3)), Vector.empty[PlayerId])

  test("Festival District is used by players at its site or ruling it"):
    val festival = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = dunes)
    assertEquals(festivalGoers(festival), Vector(p2))
    val ruled = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = woods).warbandsAt(woods, p3, 1)
    assertEquals(festivalGoers(ruled), Vector(p3))

  test("Squalid District, the ruined face, gives no user"):
    assertEquals(festivalGoers(apart.edifice(FestivalDistrictCard,
      EdificeSide.Ruined, at = dunes)), Vector.empty[PlayerId])

  test("City Wall stops its ruler's enemy at its site from using Council " +
      "Arbiter"):
    val walled = apart.adviser(p1, CouncilArbiterCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(arbiters(walled), Vector(p1))
    assertEquals(arbiters(walled, blockers), Vector.empty[PlayerId])

  test("Giant Python at Festival District's site stops every use of it"):
    val python = apart.edifice(FestivalDistrictCard, EdificeSide.Intact,
      at = dunes).denizen(GiantPythonCard.id, at = dunes)
    assertEquals(festivalGoers(python), Vector(p2))
    assertEquals(festivalGoers(python, blockers), Vector.empty[PlayerId])
