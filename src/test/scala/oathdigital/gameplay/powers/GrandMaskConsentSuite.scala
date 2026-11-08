package oathdigital.gameplay.powers

import oathdigital.gameplay.{GrandMaskCard, SiteRulers}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powerresolver.PowerUse
import oathdigital.gameplay.operations.{HallOfMinisters,
  HallOfMinistersCard}
import oathdigital.gameplay.powers.economy.CurfewCard
import oathdigital.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import oathdigital.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import oathdigital.gameplay.powers.travel.{ForbiddenForest,
  ForbiddenForestCard}
import oathdigital.gameplay.powers.wake.{HiddenPassages, HiddenPassagesCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** Who consents for a masked card (catalog batch 11, rulings 7 and 15):
  * the wearer, during their turn, for every card at an Imperial site but a
  * battle plan; Hidden Passages reads its site, so the Chancellor. */
class GrandMaskConsentSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p2 is the Chancellor and p1 a Citizen. The Empire holds Ancient City
    * (Curfew, Relic Patrols, Hidden Passages) and Broken Peaks (Spell
    * Breaker, Forbidden Forest, Hall of Ministers). p3, an Exile, holds
    * the Mask `facedown` or faceup. It is p3's Act. */
  private def empireWithMaskHeldByP3(facedown: Boolean = false): ReadyGame =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .warbandsAt(home, p2, 1).warbandsAt(peaks, p2, 1)
      .denizen(CurfewCard.id, at = home)
      .edifice(RelicPatrolsCard, EdificeSide.Intact, at = home)
      .edifice(HiddenPassagesCard, EdificeSide.Intact, at = home)
      .denizen(SpellBreakerCard.id, at = peaks)
      .edifice(ForbiddenForestCard, EdificeSide.Intact, at = peaks)
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = peaks)
      .relic(p3, GrandMaskCard, facedown = facedown).ready

  private def granters(ready: ReadyGame): Vector[Option[PlayerId]] = Vector(
    SiteRulers.consentGranter(ready, CurfewCard.id),
    ConsentPowers.default.granter(ready, SpellBreaker.id),
    RelicPatrols.granter(ready),
    ForbiddenForest.granter(ready),
    HallOfMinisters.granter(ready))

  test("the wearer consents for every masked card"):
    assertEquals(granters(empireWithMaskHeldByP3()), Vector.fill(5)(Some(p3)))

  test("without the Mask, the Chancellor consents"):
    assertEquals(granters(empireWithMaskHeldByP3(facedown = true)),
      Vector.fill(5)(Some(p2)))

  test("Hidden Passages asks the site's ruler, under the Mask too"):
    assertEquals(HiddenPassages.granter(empireWithMaskHeldByP3()), Some(p2))
    // The wearer's own use asks the Chancellor (ruling 15).
    val use = PowerUse(p3, HiddenPassages.id, Some(HiddenPassagesCard.id),
      persistent = false, costsSecret = false)
    assertEquals(HiddenPassages.refusal(catalog, empireWithMaskHeldByP3(),
      use).flatMap(_.reason.waiver).flatMap(_.granter), Some(p2))
    assertEquals(SiteRulers.consentGranterOf(empireWithMaskHeldByP3(), home,
      HiddenPassagesCard.id), Some(p3))
