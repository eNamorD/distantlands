package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.powerresolver.{ContributingPower, PowerCtx,
  PowerUse}
import distantlands.gameplay.powers.campaign.KeepRule
import distantlands.gameplay.powers.economy.InitiationRite
import distantlands.gameplay.powers.travel.Tyrant
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p1

/** Keep states its use at its windows; the wrapped automatic powers state
  * theirs on their `AutomaticUse` node instead; a cost change and a
  * lasting limit state none (catalog batch 8, slice 10, part 2). */
class AutomaticPowerUseSuite extends munit.FunSuite:
  private val ready = Table.start.turn(p1, Phase.Act).ready

  private def ctxAt(power: ContributingPower, window: PowerWindow)
      : PowerCtx = PowerCtx(catalog, ready, p1, power.source, window,
    Vector.empty,
    Sequence(Vector.empty, Some(window)))

  test("an automatic use is non-persistent and costs nothing"):
    val use = PowerUse.automatic(p1, Tyrant.id, Tyrant.cardId)
    assert(!use.persistent && !use.costsSecret && !use.places)

  test("Keep is used by nobody without a Raid"):
    assertEquals(KeepRule.use(ctxAt(KeepRule,
      PowerWindow.CampaignRaidRelocation)), None)

  test("a wrapped power states no use at its window; its node does"):
    assertEquals(Tyrant.use(ctxAt(Tyrant, PowerWindow.TravelCost)), None)

  test("Initiation Rite, a cost change, is never used"):
    assertEquals(InitiationRite.use(ctxAt(InitiationRite,
      PowerWindow.MusterCost)), None)
