package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
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
    // Woods. Ancient City is nobody's, so only Enduring keeps its card.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(DeepWoodsSite, p1, 1).warbandsAt(BrokenPeaksSite, p3, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .legacy(p3, IronHandCard)
    val run = throne(table)
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
