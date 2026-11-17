package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.legacies.{IronHandCard, Rival,
  RivalCard}
import distantlands.gameplay.operations.HallOfMinistersCard
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p3}

/** The cards Shape Empire keeps on the map besides the Throne holder's:
  * Ancient City's (Enduring) and, while Hall of Bandits is on the map, the
  * ruined edifices at bandit sites (Last Chronicle powers design, rulings 1
  * and 2). p1 is the Chancellor holding the Throne; the Empire rules the
  * sites each test gives an Imperial warband. */
class ShapeEmpireExemptionsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** The Throne walked to its end on `table`, every choice answered
    * plainly. */
  private def throne(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  test("Ancient City's Enduring is implemented"):
    assert(PowerImplementationStatus.implemented(AncientCitySite.enduring))
    assertEquals(AncientCityEnduring.id, AncientCitySite.enduring)

  test("a card at Ancient City stays when Iron Hand's Exile shapes the " +
      "Empire in the Chancellor's place (ruling 1)"):
    // p3 holds Iron Hand and rules Broken Peaks; the Empire rules Deep
    // Woods, whose card only Iron Hand's Shape Empire discards. Ancient
    // City is nobody's, so only Enduring keeps its card.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(DeepWoodsSite, p1, 1).warbandsAt(BrokenPeaksSite, p3, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .denizen(VillageIdiotCard, at = DeepWoodsSite)
      .legacy(p3, IronHandCard)
    val run = throne(table)
    assert(!onMap(run.ready, VillageIdiotCard.id))
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))

  private def empireAt(sites: distantlands.catalog.Site*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  test("Hall of Bandits is implemented"):
    assert(PowerImplementationStatus.implemented(
      HallOfMinistersCard.ruined.power.id))
    assertEquals(HallOfBandits.id, HallOfMinistersCard.ruined.power.id)

  test("with Hall of Bandits on the map, the ruined edifices at bandit " +
      "sites stay, the Hall included (ruling 2)"):
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2).bandits(DeepWoodsSite, 1)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = DeepWoodsSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(onMap(run.ready, SchoolOfVinesCard.id))

  test("Hall of Bandits keeps no ruin at a site bandits do not rule, nor " +
      "a denizen at a bandit site"):
    // Deep Woods has no warbands, so nobody rules it.
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = DeepWoodsSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))

  test("a ruined Hall of Bandits discarded in the same Shape Empire still " +
      "keeps the ruins at bandit sites"):
    // Deep Woods has no warbands: the Hall is discarded there, in the same
    // batch that would discard the ruin at Buried Giant.
    val table = empireAt(BrokenPeaksSite)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = DeepWoodsSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite)
    val run = throne(table)
    assert(!onMap(run.ready, HallOfMinistersCard.id))
    assert(onMap(run.ready, SchoolOfVinesCard.id))

  test("the Hall of Ministers, its intact face, keeps no ruin at a bandit " +
      "site"):
    val table = empireAt(BrokenPeaksSite)
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))

  test("a ruin Hall of Bandits keeps is not among Rival's options"):
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .legacy(p3, RivalCard)
    val run = throne(table)
    val asked = run.asked.find(_.decisionId == Rival.decisionId(p3)).get
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref), Vector[DecisionOptionRef](
        DecisionOptionRef.Denizen(FaeMerchantCard.id)))
