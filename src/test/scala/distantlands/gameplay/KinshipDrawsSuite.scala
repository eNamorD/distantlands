package distantlands.gameplay

import distantlands.gameplay.powers.burn.{VowOfDivision, VowOfDivisionCard}
import distantlands.gameplay.walker.{ProcedureWalker, WalkerPowers}
import distantlands.model._
import distantlands.testkit.{Look, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}

/** Vow of Kinship's draw (catalog batch 11, slice 5, ruling 9): before a
  * batch runs, a holder's spend that their board cannot cover draws the
  * rest from the Nomad bank, and nothing done to them draws at all. */
class KinshipDrawsSuite extends munit.FunSuite:
  /** p1 holds the Vow with `board` favor on their board and `bank` in the
    * Nomad bank; p2's board holds 3. It is p1's turn. */
  private def vowHolder(board: Int, bank: Int): Table =
    Table.start.adviser(p1, VowOfKinshipCard).favor(p1, board)
      .favor(p2, 3).bankFavor(Suit.Nomad, bank)

  private def burning(player: PlayerId, n: Int): PayCost =
    PayCost(player, Location.SharedBank, Cost(favorBurnt = n))

  private def giving(n: Int): Give = Give(Piece.Favor(n), p1,
    Location.PlayArea(p1), Location.PlayArea(p2), required = true)

  test("a cost the board cannot cover draws the rest first"):
    val ready = vowHolder(board = 1, bank = 4).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](DrawNomadFavor(p1, 2), burning(p1, 3)))

  test("a board that covers the spend, or a player without the Vow, " +
      "draws nothing"):
    val ready = vowHolder(board = 3, bank = 4).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](burning(p1, 3)))
    assertEquals(KinshipDraws.before(ready, Vector(burning(p2, 3))),
      Vector[CoreOperation](burning(p2, 3)))

  test("a take from the holder and a burn of their board draw nothing"):
    val ready = vowHolder(board = 0, bank = 4).ready
    val done: Vector[CoreOperation] = Vector(
      Take(Piece.Favor(2), p2, Location.PlayArea(p1), Location.PlayArea(p2)),
      Burn.favor(2, PositionedLocation(Location.PlayArea(p1)), Some(p1)))
    assertEquals(KinshipDraws.before(ready, done), done)

  test("one draw covers the later spends, and a draw already in the " +
      "batch counts"):
    val ready = vowHolder(board = 1, bank = 5).ready
    assertEquals(KinshipDraws.before(ready, Vector(giving(1), giving(2))),
      Vector[CoreOperation](giving(1), DrawNomadFavor(p1, 2), giving(2)))
    val answered: Vector[CoreOperation] =
      Vector(DrawNomadFavor(p1, 3), burning(p1, 3))
    assertEquals(KinshipDraws.before(ready, answered), answered)

  test("a take from the board earlier in the batch leaves the spend " +
      "short, so it draws"):
    val ready = vowHolder(board = 2, bank = 4).ready
    val taken = Take(Piece.Favor(2), p2, Location.PlayArea(p1),
      Location.PlayArea(p2))
    assertEquals(KinshipDraws.before(ready, Vector(taken, burning(p1, 2))),
      Vector[CoreOperation](taken, DrawNomadFavor(p1, 2), burning(p1, 2)))

  test("two holders draw on the one bank"):
    // p2 holds the Vow, and p1, the walk's actor, acts with p2's advisers
    // (Master of Disguise), so both hold it. Both boards are empty; the
    // bank holds 3.
    val ready = Table.start.adviser(p2, VowOfKinshipCard).favor(p1, 0)
      .favor(p2, 0).bankFavor(Suit.Nomad, 3).ready
      .updateCurrent(_.copy(walkerAsIfAdvisers = Some(p2)))
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 2),
        burning(p2, 2))),
      Vector[CoreOperation](DrawNomadFavor(p1, 2), burning(p1, 2),
        DrawNomadFavor(p2, 1), burning(p2, 2)))

  test("favor the holder places on a banner draws on the bank (ruling 26)"):
    val ready = vowHolder(board = 1, bank = 4).ready
    val placed = Move(Piece.Favor(3), PositionedLocation(Location.PlayArea(p1)),
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor)),
      by = Some(p1))
    assertEquals(KinshipDraws.before(ready, Vector(placed)),
      Vector[CoreOperation](DrawNomadFavor(p1, 2), placed))

  test("a draw is capped by the bank"):
    val ready = vowHolder(board = 0, bank = 1).ready
    assertEquals(KinshipDraws.before(ready, Vector(burning(p1, 3))),
      Vector[CoreOperation](DrawNomadFavor(p1, 1), burning(p1, 3)))

  test("a walked cost and a gift are paid out of the Nomad bank"):
    val ready = vowHolder(board = 0, bank = 4).ready
    val done = Outcomes.finished(ProcedureWalker.advance(ready,
      BuildOps((_, _) => Right(Vector[CoreOperation](burning(p1, 1),
        giving(2)))), None, WalkerPowers(Vector.empty)))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 0)
    assertEquals(look.bankFavor(Suit.Nomad), 1)
    assertEquals(look.favor(p2), 5)

  test("a cost's burn that Vow of Division takes comes out of the Nomad " +
      "bank"):
    val ready = vowHolder(board = 0, bank = 4)
      .adviser(p2, VowOfDivisionCard).ready
    val done = Outcomes.finished(ProcedureWalker.advance(ready,
      BuildOps((_, _) => Right(Vector[CoreOperation](burning(p1, 2)))),
      None, WalkerPowers(Vector(VowOfDivision))))
    val look = Look(done.treeless)
    assertEquals(look.bankFavor(Suit.Nomad), 2)
    assertEquals(look.favor(p2), 5)
