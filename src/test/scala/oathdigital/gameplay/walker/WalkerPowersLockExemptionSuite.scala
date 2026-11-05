package oathdigital.gameplay.walker

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.operations.{HallOfMinistersCard,
  OperationRestrictions}
import oathdigital.gameplay.powers.economy.HallowedSpringCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table

/** The own-power lock exemption (catalog batch 10, slice 4): while a power
  * that ignores its own lock is used, its card's lock does not bind; every
  * other lock does, and the card's lock binds every other power. */
class WalkerPowersLockExemptionSuite extends munit.FunSuite:
  private val hall: CardId = HallOfMinistersCard.id
  private val spring: CardId = HallowedSpringCard.id
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** A power whose use ignores the Hall of Ministers' lock. */
  private val hallMover = PowerId("test.hall-mover")
  private val powers = WalkerPowers(Vector.empty,
    restrictionSet = OperationRestrictions.forCatalog(catalog),
    lockExemptions = Map(hallMover -> hall))
  /** The Hall of Ministers and Hallowed Spring, both locked while intact,
    * at Ancient City. */
  private val twoLockedEdifices: ReadyGame = Table.start
    .edifice(HallOfMinistersCard, EdificeSide.Intact, at = home)
    .edifice(HallowedSpringCard, EdificeSide.Intact, at = home).ready

  private def moveToPeaks(card: CardId): CoreOperation =
    Move(Piece.Card(card), PositionedLocation(Location.Site(home)),
      PositionedLocation(Location.Site(peaks)), by = Some(Table.p1))

  /** The code of the first restriction refusing `card`'s move during
    * `procedure`. */
  private def refusal(procedure: Option[ProcedureRef],
      card: CardId): Option[String] =
    powers.restrictionsDuring(procedure)
      .flatMap(_.reason(twoLockedEdifices, moveToPeaks(card)))
      .headOption.map(_.code)

  private val hallMoverUse = Some(ActionRef.UsePower(hallMover))

  test("outside any power use both locked edifices refuse a move"):
    assertEquals(refusal(None, hall), Some("locked"))
    assertEquals(refusal(None, spring), Some("locked"))

  test("during the exempt power's use its own card moves and every other " +
      "lock binds"):
    assertEquals(refusal(hallMoverUse, hall), None)
    assertEquals(refusal(hallMoverUse, spring), Some("locked"))

  test("during another power's use the exempt card's lock still binds"):
    assertEquals(refusal(Some(ActionRef.UsePower(PowerId("test.other"))),
      hall), Some("locked"))

  test("the exempt set is built once, and a plain walk keeps the shared set"):
    assert(powers.restrictionsDuring(hallMoverUse) eq
      powers.restrictionsDuring(hallMoverUse))
    assert(powers.restrictionsDuring(None) eq powers.operationRestrictions)
