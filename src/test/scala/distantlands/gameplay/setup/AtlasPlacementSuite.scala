package distantlands.gameplay.setup

import distantlands.gameplay.cards.{DeepWoodsSite, FairIsleSite,
  GoldenValleySite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `AtlasPlacement` reads the site an end of the Atlas gives and copies
  * its printed facts onto the operation that places it (Foundations
  * design, slice 3). The quiet map is cut to three sites: the Atlas holds
  * Deep Woods at its Recent end and Golden Valley at its Forgotten end,
  * and the next slot is in the Provinces. */
class AtlasPlacementSuite extends munit.FunSuite:
  private def threeSitesAndAnAtlas: Table = Table.start.mapCutIntoAtlas(3)

  test("each end gives its site, what it stores, and an operation " +
      "carrying the site's printed capacity, relic slots and resources"):
    val ready = threeSitesAndAnAtlas
      .storedInAtlas(DeepWoodsSite, GreatMarketCard).ready
    val recent = accepted(AtlasPlacement.at(catalog, ready, AtlasEnd.Recent))
    assertEquals(recent.site, DeepWoodsSite.id)
    assertEquals(recent.stored.edifice, Some(GreatMarketCard.id))
    assertEquals(recent.operation, PlaceAtlasSite(AtlasEnd.Recent,
      Region.Provinces, capacity = 3, relicSlots = 0, Tokens.empty))
    val forgotten = accepted(AtlasPlacement.at(catalog, ready,
      AtlasEnd.Forgotten))
    assertEquals(forgotten.site, GoldenValleySite.id)
    assertEquals(forgotten.operation.end, AtlasEnd.Forgotten)

  test("the operation names the region of the next slot and the printed " +
      "favor of a site that has some"):
    // Fair Isle alone in the Atlas, on a map whose Provinces are full.
    val ready = Table.start.mapCutIntoAtlas(5).update(_.updateCampaign(
      campaign => campaign.copy(atlas = AtlasState(campaign.atlas.entries
        .filter(_ == AtlasEntry.StoredSite(FairIsleSite.id, Vector.empty,
          Vector.empty)))))).ready
    assertEquals(accepted(AtlasPlacement.at(catalog, ready,
      AtlasEnd.Recent)).operation, PlaceAtlasSite(AtlasEnd.Recent,
        Region.Hinterland, capacity = 2, relicSlots = 1, Tokens(3, 0)))

  test("a placement is refused on a full map, with no site in the Atlas, " +
      "and for a site the catalog does not print"):
    rejected(AtlasPlacement.at(catalog, Table.start.ready, AtlasEnd.Recent))
    val emptyAtlas = threeSitesAndAnAtlas.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector.empty)))).ready
    rejected(AtlasPlacement.at(catalog, emptyAtlas, AtlasEnd.Recent))
    val unprinted = threeSitesAndAnAtlas.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector(AtlasEntry.StoredSite(
        SiteId("site:nowhere"), Vector.empty, Vector.empty)))))).ready
    assertEquals(rejected(AtlasPlacement.at(catalog, unprinted,
      AtlasEnd.Recent)), OathViolation.InvalidEventOrder(
        "site:nowhere is not a site the catalog prints"))
