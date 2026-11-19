package distantlands.application

import distantlands.gameplay.cards.{DeepWoodsSite, DesolateShoreSite}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.present

/** The Chronicle as the table sees it mid-game (Chronicle Details design,
  * slice 1): the era scorepad, every lineage with the legacies its viewer
  * may see (ruling D1), and the Atlas's Recent site. */
class ChronicleDetailsProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val viewers = Vector(Some(p1), Some(p2), None)

  /** The quiet map cut to three sites, Deep Woods at the Atlas's Recent
    * end storing the Great Market, Wizard's Conclave and Sticky Fire, and
    * Desolate Shore behind it storing Threatening Roar. */
  private def recentDeepWoodsStoringAnEdificeADenizenAndARelic: ReadyGame =
    Table.start.mapCutIntoAtlas(3)
      .storedInAtlas(DeepWoodsSite, GreatMarketCard, WizardsConclaveCard,
        StickyFireCard)
      .storedInAtlas(DesolateShoreSite, ThreateningRoarCard).ready

  test("the Recent site shows its denizens faceup, its edifice intact " +
      "after them, its relics facedown and hidden, and no loose " +
      "resources or warbands"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    val site = presentation.atlasSite(present(
      ready.game.campaign.atlas.site(AtlasEnd.Recent), "the Recent site"))
    assertEquals((site.siteId, site.denizenCapacity, site.relicCapacity),
      (DeepWoodsSite.id.value, DeepWoodsSite.capacity,
        DeepWoodsSite.relicSlots))
    assertEquals(site.denizens.map(card => (card.denizenId,
      card.details.flatMap(_.orientation), card.details.flatMap(_.side))),
      Vector((WizardsConclaveCard.id.value, Some("face-up"), None),
        (GreatMarketCard.id.value, None, Some("intact"))))
    assertEquals(site.relics.relics.map(relic => (relic.cardId, relic.hidden)),
      Vector(("hidden", true)))
    assertEquals((site.looseFavor, site.looseSecrets, site.forces,
      site.festival), (0, 0, None, false))

  test("every viewer identifies the Recent site's denizens and edifice, " +
      "and none its relics or another Atlas site's cards (R2)"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    viewers.foreach { viewer =>
      assert(presentation.identifiesAt(ready, viewer,
        WizardsConclaveCard.id), viewer)
      assert(presentation.identifiesAt(ready, viewer, GreatMarketCard.id),
        viewer)
      assert(!presentation.identifiesAt(ready, viewer, StickyFireCard.id),
        viewer)
      assert(!presentation.identifiesAt(ready, viewer,
        ThreateningRoarCard.id), viewer)
    }
