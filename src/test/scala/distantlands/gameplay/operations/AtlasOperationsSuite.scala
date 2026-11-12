package distantlands.gameplay.operations

import distantlands.gameplay.cards.{DeepWoodsSite, DesolateShoreSite,
  FairIsleSite, GoldenValleySite}
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.WizardsConclaveCard
import distantlands.model._
import distantlands.testkit.{Look, RawOperationBatch, Table}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** `PlaceAtlasSite` (Foundations design, slice 3, "Explore"): a site
  * leaves an end of the Atlas for the next slot and is populated as a
  * bandit site. */
class AtlasOperationsSuite extends munit.FunSuite:
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  /** The quiet map cut to Ancient City, Broken Peaks and Buried Giant. The
    * Atlas holds Deep Woods at its Recent end, then Desolate Shore, Dunes,
    * Fair Isle, and Golden Valley at its Forgotten end. The next slot is
    * in the Provinces. */
  private def threeSitesAndAnAtlas: Table = Table.start.mapCutIntoAtlas(3)

  /** The operation that places `site` from `end` as its printed facts say:
    * what a walk builds from the catalog. */
  private def placing(end: AtlasEnd, site: distantlands.catalog.Site,
      region: Region = Region.Provinces): PlaceAtlasSite =
    PlaceAtlasSite(end, region, site.capacity, site.relicSlots,
      site.startingResources)

  test("the Recent end's site takes the next slot with its stored cards: " +
      "its edifice ruined, its denizens faceup, its relics facedown"):
    val ready = threeSitesAndAnAtlas.storedInAtlas(DeepWoodsSite,
      GreatMarketCard, WizardsConclaveCard).ready
    val after = accepted(run(ready, placing(AtlasEnd.Recent, DeepWoodsSite)))
    val map = after.game.current.map
    assertEquals(map.provinces.last, DeepWoodsSite.id)
    assertEquals(map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(map.sites(DeepWoodsSite.id).denizens, Vector[SiteDenizenState](
      EdificeState(GreatMarketCard.id, EdificeSide.Ruined, Tokens.empty),
      DenizenState(WizardsConclaveCard.id, Orientation.FaceUp, Tokens.empty)))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).tail)

  test("the Forgotten end's site is the Atlas's last"):
    val ready = threeSitesAndAnAtlas.ready
    val after = accepted(run(ready,
      placing(AtlasEnd.Forgotten, GoldenValleySite)))
    assertEquals(after.game.current.map.hintermost, Some(GoldenValleySite.id))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).init)

  test("the placed site takes bandits up to its capacity, its printed " +
      "favor, and the relic deck's top cards for its open relic slots"):
    // Fair Isle: capacity 2, 1 relic slot, 3 favor.
    val ready = threeSitesAndAnAtlas.ready
    val recentFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.reverse.drop(1))))
    assertEquals(recentFirst.game.campaign.atlas.site(AtlasEnd.Recent)
      .map(_.id), Some(FairIsleSite.id))
    val top = recentFirst.game.current.commonCards.relicDeck.head
    val after = accepted(run(recentFirst,
      placing(AtlasEnd.Recent, FairIsleSite)))
    val look = Look(after)
    assertEquals(look.forces(FairIsleSite),
      SiteForces.Occupied(ForceKind.Bandit, 2))
    assertEquals(look.siteTokens(FairIsleSite), Tokens(3, 0))
    assertEquals(after.game.current.map.sites(FairIsleSite.id).relics,
      Vector(RelicState(top, Orientation.FaceDown, Tokens.empty)))
    assertEquals(after.game.current.commonCards.relicDeck,
      recentFirst.game.current.commonCards.relicDeck.tail)

  test("a stored relic fills a slot, so only the open slots are dealt, " +
      "the stored relics first"):
    // Desolate Shore has 2 relic slots; one stored relic leaves one open.
    val ready = threeSitesAndAnAtlas
      .storedInAtlas(DesolateShoreSite, StickyFireCard).ready
    val shoreFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.drop(1))))
    val top = shoreFirst.game.current.commonCards.relicDeck.head
    val after = accepted(run(shoreFirst,
      placing(AtlasEnd.Recent, DesolateShoreSite)))
    assertEquals(Look(after).relicsAt(DesolateShoreSite),
      Vector(StickyFireCard.id, top))
    assert(after.game.current.map.sites(DesolateShoreSite.id).relics
      .forall(_.orientation == Orientation.FaceDown))

  test("a relic deck that runs out leaves the slots it cannot fill open, " +
      "and a site of capacity 0 takes no bandits"):
    val ready = threeSitesAndAnAtlas.relicDeckEmptied
      .relicDeckTop(DowsingSticksCard).ready
    val shoreFirst = ready.updateCampaign(campaign => campaign.copy(atlas =
      AtlasState(campaign.atlas.entries.drop(1))))
    val after = accepted(run(shoreFirst, PlaceAtlasSite(AtlasEnd.Recent,
      Region.Provinces, capacity = 0, relicSlots = 2, Tokens.empty)))
    assertEquals(Look(after).relicsAt(DesolateShoreSite),
      Vector(DowsingSticksCard.id))
    assertEquals(after.game.current.commonCards.relicDeck, Vector.empty)
    assertEquals(Look(after).forces(DesolateShoreSite), SiteForces.Empty)

  test("a placement is refused with no site in the Atlas, into a region " +
      "that does not hold the next slot, and on a full map"):
    val ready = threeSitesAndAnAtlas.ready
    rejected(run(Table.start.mapCutIntoAtlas(8).ready,
      placing(AtlasEnd.Recent, DeepWoodsSite)))
    rejected(run(ready, placing(AtlasEnd.Recent, DeepWoodsSite,
      Region.Hinterland)))
    val emptyAtlas = ready.updateCampaign(_.copy(atlas =
      AtlasState(Vector.empty)))
    rejected(run(emptyAtlas, placing(AtlasEnd.Recent, DeepWoodsSite)))

  test("the pipeline's card inventory holds after a placement"):
    val ready = threeSitesAndAnAtlas.storedInAtlas(DeepWoodsSite,
      GreatMarketCard, WizardsConclaveCard).ready
    val run = accepted(OperationPipeline.run(ready,
      Vector(placing(AtlasEnd.Recent, DeepWoodsSite)),
      OperationPolicy.Permissive, Vector.empty)(Right(_)))
    assertEquals(run.state.game.cardIndex.map(_.ids),
      ready.game.cardIndex.map(_.ids))

  test("a placed site's capacity and relic slots cannot be negative"):
    intercept[IllegalArgumentException](PlaceAtlasSite(AtlasEnd.Recent,
      Region.Cradle, -1, 0, Tokens.empty))
    intercept[IllegalArgumentException](PlaceAtlasSite(AtlasEnd.Recent,
      Region.Cradle, 0, -1, Tokens.empty))
