package distantlands.gameplay.powers.wake

import distantlands.gameplay.OathRules
import distantlands.gameplay.cards.{AncientCitySite,
  BuriedGiantSite}
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powerresolver.PhasePowers
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, MenuPlay, Outcomes, Table, TakeWealthFixture}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Light Fingers (L27): its holder takes wealth beside an enemy pawn
  * (Legacy in-play powers ruling 17; implementation ruling R8). */
class LightFingersSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)

  /** p1's Wake at Ancient City, which holds 1 favor and 1 secret; p2 stands
    * there too, and p3 at Buried Giant. `legacies` set the table's
    * legacies. */
  private def wakeBesideP2(legacies: Table => Table): ReadyGame =
    TakeWealthFixture.wakeReady(legacies(Table.start.turn(p1, Phase.Wake)
      .pawn(p1, at = AncientCitySite).pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BuriedGiantSite)).ready, favor = 1, secrets = 1)

  private val holder = wakeBesideP2(_.legacy(p1, LightFingersCard))

  private def take(ready: ReadyGame) = TakeWealthFixture.take(rules, ready)

  private def offered(ready: ReadyGame): Boolean =
    MenuPlay.options(Table.from(ready).phaseMenuParked.state, catalog, powers,
      PhasePowers.empty).exists(_.ref == PhaseMenu.takeWealth(
        WakeResource.Favor))

  test("its holder takes a favor from a site where an enemy pawn stands"):
    val done = accepted(take(holder))
    assertEquals(Look(Outcomes.ready(done.state)).favor(p1),
      Look(holder).favor(p1) + 1)

  test("its Wake menu offers Take Wealth there"):
    assert(offered(holder))
    assert(!offered(wakeBesideP2(identity)))

  test("a dormant Light Fingers, or another player's, opens nothing " +
      "(ruling 1)"):
    val dormant = wakeBesideP2(_.legacy(p1, LightFingersCard, active = false))
    assertEquals(take(dormant).left.toOption,
      Some(OathViolation.EnemyPawnBlocksTakeWealth(AncientCitySite.id,
        Vector(p2))))
    assert(take(wakeBesideP2(_.legacy(p2, LightFingersCard))).isLeft)

  test("the once-per-site limit still holds"):
    val once = Outcomes.ready(accepted(take(holder)).state)
    val again = Table.from(once).backInPhase(Phase.Wake).ready
    assertEquals(TakeWealthFixture.take(rules, again,
      TakeWealthFixture.secretArg).left.toOption,
      Some(OathViolation.PowerAlreadyUsed(TakeWealthLimit.useRef(
        AncientCitySite.id))))
