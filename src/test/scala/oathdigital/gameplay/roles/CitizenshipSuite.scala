package oathdigital.gameplay.roles

import oathdigital.gameplay.cards.BrokenPeaksSite
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p3}

/** The question a short purple bank asks (ruling R18) and the first-game
  * option (ambiguity F8). */
class CitizenshipSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id

  /** p1 is the Chancellor; p3 an Exile with 2 board warbands and 3 at
    * Broken Peaks. `purple` is what the purple bank holds. */
  private def exileWith(purple: Int): ReadyGame = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = BrokenPeaksSite)
    .boardWarbands(p3, 2).warbandsAt(BrokenPeaksSite, p3, 3)
    .boardWarbands(p1, MaterialBankState.ImperialWarbands - purple).ready

  test("a bank with room for every warband asks nothing"):
    assertEquals(Citizenship.query(exileWith(5), p3), None)

  test("a short bank asks for a split over the board and each site"):
    assertEquals(Citizenship.query(exileWith(4), p3),
      Some(DecisionQuery.Distribute.exactly(Vector(
        DistributeSlot(DecisionOptionRef.Player(p3), 0, 2, None),
        DistributeSlot(DecisionOptionRef.Site(peaks), 0, 3, None)), 4,
        Some("Too few purple warbands: choose where yours turn purple"),
        "Replace warbands")))

  test("the split's rows become the kept counts"):
    val rows = Vector(DistributeAmount(DecisionOptionRef.Player(p3), 1),
      DistributeAmount(DecisionOptionRef.Site(peaks), 3))
    assertEquals(Citizenship.kept(p3, rows),
      Right(RoleChanges.Kept(1, Map(peaks -> 3))))

  test("a split naming another player's board is refused"):
    val rows = Vector(DistributeAmount(DecisionOptionRef.Player(p1), 1))
    assert(Citizenship.kept(p3, rows).isLeft)

  test("the first-game option is on unless a table turns it off"):
    assert(Citizenship.ignored(Table.start.ready))
    assert(!Citizenship.ignored(Table.start.allowingCitizenshipPowers.ready))

  test("each player's split has its own decision, so one tree may make " +
      "several players Imperial"):
    assertEquals(Citizenship.replaceDecisionId(p3),
      "role.replace-warbands.p3")
    assertNotEquals(Citizenship.replaceDecisionId(p1),
      Citizenship.replaceDecisionId(p3))

  test("a bank short for warbands all in one place asks nothing"):
    val boardOnly = Table.start.imperial(p1).turn(p3, Phase.Act)
      .boardWarbands(p3, 2)
      .boardWarbands(p1, MaterialBankState.ImperialWarbands - 1).ready
    assertEquals(Citizenship.query(boardOnly, p3), None)
