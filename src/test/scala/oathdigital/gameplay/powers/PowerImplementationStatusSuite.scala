package oathdigital.gameplay.powers

import oathdigital.gameplay.cards.HomelandsAndPlains
import oathdigital.gameplay.operations.{AncientBloodline, GrandScepter,
  GrandScepterNegotiation, HallOfMinisters, OperationRestrictions,
  StillBoilingLake}
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.model.PowerId

/** The single predicate the presentation layer's unimplemented-card marker
  * reads. Each case reads the registries the predicate reads, so a card
  * that ships moves from one case to another instead of breaking a test
  * that named it; the card-level assertions live in
  * `GamePresentationProjectorImplementedSuite`, and the exact unimplemented
  * set in `UnimplementedPowersSuite`.
  */
class PowerImplementationStatusSuite extends munit.FunSuite:
  private val implemented = PowerImplementationStatus.implemented

  test("a power the walker or phase catalog covers is implemented"):
    val covered = WalkerPowerCatalog.default(catalog).powers.map(_.id) ++
      PhasePowerCatalog.default.powers.map(_.id)
    covered.foreach(id => assert(implemented(id), id.value))

  test("the printed rules are Ancient Bloodline, the Hall of Ministers, the " +
      "Grand Scepter's restriction and negotiation term and Still Boiling " +
      "Lake"):
    assertEquals(OperationRestrictions.printedRules.map(_.id),
      Vector(AncientBloodline.id, HallOfMinisters.id, GrandScepter.id,
        GrandScepterNegotiation.id, StillBoilingLake.id))

  test("a power a printed operation restriction enforces, with no walker " +
      "power, is implemented"):
    OperationRestrictions.printedRules.foreach(rule =>
      assert(implemented(rule.id), rule.id.value))

  test("the six Homelands' handlers, which card play's Homeland rule runs, " +
      "and the five Plains' handlers, which print no power, are implemented"):
    assertEquals(HomelandsAndPlains.homelands.size, 6)
    assertEquals(HomelandsAndPlains.plains.size, 5)
    assert(HomelandsAndPlains.homelands.forall(_.homeland.nonEmpty))
    assert(HomelandsAndPlains.plains.forall(_.homeland.isEmpty))
    (HomelandsAndPlains.homelands ++ HomelandsAndPlains.plains)
      .flatMap(_.handlers).foreach(id => assert(implemented(id), id.value))

  test("an id nothing declares at all is not implemented"):
    assert(!implemented(PowerId("denizen.not-a-real-power")))
