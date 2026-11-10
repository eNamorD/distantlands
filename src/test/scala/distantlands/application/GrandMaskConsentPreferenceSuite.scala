package distantlands.application

import distantlands.gameplay.{GrandMaskCard, OathRules}
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.{ConsentPowers, WalkerPowerCatalog}
import distantlands.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathEvent.ConsentPreferencesCleared
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** A standing preference under the Grand Mask (catalog batch 11, slice 2,
  * "Preferences"): the wearer's turn makes them the granter of the masked
  * cards, so the Chancellor's preferences on those cards are cleared, as a
  * Citizen's turn clears them. */
class GrandMaskConsentPreferenceSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val desk = new ConsentDesk(new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog)),
    ConsentPowers.default)

  /** p2 is the Chancellor and p1 a Citizen. The Empire holds Broken Peaks,
    * where Spell Breaker stands, and p2 always lets p1 off its ban. p3, an
    * Exile, holds the Mask `facedown` or faceup. It is p3's Act. */
  private def chancellorPreferenceWithMaskHeldByP3(facedown: Boolean)
      : OathState =
    val ready = Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .warbandsAt(peaks, p2, 1).denizen(SpellBreakerCard.id, at = peaks)
      .relic(p3, GrandMaskCard, facedown = facedown).ready
    OathState.Ready(ready.copy(consent = ConsentState().preferenceSet(p2,
      SpellBreaker.id, p1, ConsentSetting.Always)))

  test("the wearer's turn clears the Chancellor's preference on a masked " +
      "card"):
    assertEquals(desk.stale(chancellorPreferenceWithMaskHeldByP3(false)),
      Vector(ConsentPreferencesCleared(p2, SpellBreaker.id)))

  test("with the Mask facedown, the preference stays"):
    assertEquals(desk.stale(chancellorPreferenceWithMaskHeldByP3(true)),
      Vector.empty)
