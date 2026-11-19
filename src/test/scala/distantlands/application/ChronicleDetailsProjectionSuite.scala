package distantlands.application

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite,
  DeepWoodsSite, DesolateShoreSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  GameProjectionCodec, LineageDetailsProjection}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

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

  private val projector = new GameProjector(catalog)
  private val purple = LineageId("purple-lineage")

  private def detailsFor(ready: ReadyGame, viewer: Option[PlayerId])(
      using munit.Location): ChronicleDetailsProjection =
    val loaded = LoadedGame(Ready(ready), 1L)
    present(viewer.fold(projector.projectPublic("g", loaded))(
      projector.project("g", loaded, _)).chronicleDetails,
      "the Chronicle Details")

  private def lineageRow(ready: ReadyGame, viewer: Option[PlayerId],
      lineage: LineageId)(using munit.Location): LineageDetailsProjection =
    present(detailsFor(ready, viewer).lineages.find(_.lineage ==
      lineage.value), s"the row of ${lineage.value}")

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  /** `ready` with a purple lineage nobody is seated with in the Chronicle
    * the game started from: a Citizen there whom this game made an Exile
    * in the box, carrying 7 points from a TTS import, holding Iron Hand
    * active and Arbiter dormant, both taken out of the legacy deck. */
  private def withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      ready: ReadyGame): ReadyGame =
    val held = Vector(LegacyState(IronHandCard.id, active = true),
      LegacyState(ArbiterCard.id, active = false))
    ready.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages =
        campaign.baseline.lineages :+ LineageState(purple, None,
          Role.Citizen, held, Vector.empty)),
      boxRoles = campaign.boxRoles.updated(purple, Role.Exile),
      era = campaign.era.copy(carriedScores =
        campaign.era.carriedScores.updated(purple, 7))))
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(legacyDeck = current.commonCards.legacyDeck
          .filterNot(id => held.exists(_.id == id)))))

  test("a game not yet started has no Chronicle Details"):
    assertEquals(projector.project("g", LoadedGame(OathState.NoGame, 0L),
      p1).chronicleDetails, None)

  test("a dormant legacy is its face to its lineage's player and a back " +
      "to another seat; an active one is faceup to all"):
    val ready = Table.start.legacy(p1, SeerCard)
      .legacy(p1, ArbiterCard, active = false).ready
    val l1 = lineageOf(ready, p1)
    assertEquals(lineageRow(ready, Some(p1), l1).legacies.map(card =>
      (card.cardId, card.orientation, card.hidden)), Vector(
        (SeerCard.id.value, Some("face-up"), false),
        (ArbiterCard.id.value, Some("face-down"), false)))
    Vector(Some(p2), None).foreach(viewer =>
      assertEquals(lineageRow(ready, viewer, l1).legacies.map(card =>
        (card.cardId, card.hidden)), Vector((SeerCard.id.value, false),
          ("hidden", true)), viewer))

  test("an unseated lineage is listed first in PlayerColor order, with its " +
      "role in the box and its carried score, and its dormant legacy is a " +
      "back to every seat (R1, R5)"):
    val ready = withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      Table.start.ready)
    // Purple, then the seats' colours: l1 Red, l2 Blue, l3 Yellow.
    assertEquals(detailsFor(ready, Some(p1)).lineages.map(_.lineage),
      Vector(purple, lineageOf(ready, p1), lineageOf(ready, p2),
        lineageOf(ready, p3)).map(_.value))
    viewers.foreach { viewer =>
      val row = lineageRow(ready, viewer, purple)
      assertEquals((row.score, row.role), (7, None), viewer)
      assertEquals(row.legacies.map(card => (card.cardId, card.hidden)),
        Vector((IronHandCard.id.value, false), ("hidden", true)), viewer)
    }

  test("seated lineages follow their seats' colours, not their ids (R5)"):
    val start = Table.start.ready
    // l1 sits on Yellow and l3 on Red, so l3 comes first and l1 last.
    val ready = start.copy(playerColors = start.playerColors ++ Map(
      p1 -> PlayerColor.Yellow, p3 -> PlayerColor.Red))
    assertEquals(detailsFor(ready, Some(p1)).lineages.map(_.lineage),
      Vector(p3, p2, p1).map(lineageOf(ready, _).value))

  test("in the Chronicle Phase every seat sees every legacy's face, " +
      "seated or not (R18)"):
    val ready = withUnseatedPurpleExileHoldingIronHandAndDormantArbiter(
      Table.start.legacy(p1, SeerCard, active = false)
        .turn(p1, Phase.Chronicle).ready)
    viewers.foreach(viewer =>
      assert(detailsFor(ready, viewer).lineages.flatMap(_.legacies)
        .forall(!_.hidden), viewer))

  test("Score Lineage Points adds this game's column to the scorepad, and " +
      "each lineage's score follows it"):
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.reversingShuffle)
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
    val before = detailsFor(table.ready, Some(p1))
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val after = detailsFor(run.ready, Some(p1))
    assertEquals(after.scorepad.size, before.scorepad.size + 1)
    val era = run.ready.game.campaign.era
    assertEquals(after.lineages.map(row => row.lineage -> row.score),
      after.lineages.map(row => row.lineage ->
        era.score(LineageId(row.lineage))))
    assertEquals(after.eraTarget, era.targetScore)

  test("the Recent site changes after an Explore takes it"):
    import DefenseDieFace.TwoShields
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.scripted(Vector(TwoShields,
        TwoShields)))
    // Deep Woods at the Recent end, then Desolate Shore.
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled.ready
    assertEquals(detailsFor(ready, Some(p1)).atlasRecent.map(_.siteId),
      Some(DeepWoodsSite.id.value))
    val rolled = accepted(rules.startWalker(Ready(ready), ActionRef.Explore,
      p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    val explored = distantlands.testkit.Outcomes.ready(placed.state)
    assertEquals(detailsFor(explored, Some(p1)).atlasRecent.map(_.siteId),
      Some(DesolateShoreSite.id.value))

  test("the Atlas's Recent site is none once the Atlas holds no site"):
    val ready = Table.start.ready.updateCampaign(_.copy(atlas =
      AtlasState(Vector(AtlasEntry.EmpireDivider))))
    viewers.foreach(viewer =>
      assertEquals(detailsFor(ready, viewer).atlasRecent, None, viewer))

  test("a relic stored at the Recent site is hidden, and no seat's JSON " +
      "names it"):
    val ready = recentDeepWoodsStoringAnEdificeADenizenAndARelic
    val loaded = LoadedGame(Ready(ready), 1L)
    Vector(projector.project("g", loaded, p1),
        projector.project("g", loaded, p2),
        projector.projectPublic("g", loaded)).foreach { view =>
      assertEquals(view.chronicleDetails.flatMap(_.atlasRecent)
        .map(_.relics.relics.map(_.hidden)), Some(Vector(true)))
      assert(!GameProjectionCodec.encode(view)
        .contains(s"\"${StickyFireCard.id.value}\""))
    }
