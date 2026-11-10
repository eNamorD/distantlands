package distantlands.gameplay

import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.{p1, p2}

/** Vow of Kinship's credit (catalog batch 11, slice 5, ruling 9): favor
  * bound for its holder's board lands in the Nomad bank, however it was
  * built, and a draw from the bank lands on the board. */
class VowOfKinshipCreditSuite extends munit.FunSuite:
  /** p1 holds the Vow faceup with 1 favor on their board. The Nomad bank
    * holds 2 favor, the Order bank 3, and p2's board 3. */
  private def vowHolderWithOneFavor(facedown: Boolean = false): Table =
    Table.start.adviser(p1, VowOfKinshipCard, facedown = facedown)
      .favor(p1, 1).favor(p2, 3)
      .bankFavor(Suit.Nomad, 2).bankFavor(Suit.Order, 3)

  /** The state after `ops` run as one batch. */
  private def after(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): Look =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).fold(violation => fail(violation.toString),
      run => Look(run.state))

  test("favor the holder gains from a bank goes to the Nomad bank"):
    val look = after(vowHolderWithOneFavor().ready,
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Order), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 4)

  test("favor the holder takes, or is given, goes to the Nomad bank"):
    val look = after(vowHolderWithOneFavor().ready,
      Take(Piece.Favor(1), p1, Location.PlayArea(p2), Location.PlayArea(p1)),
      Give(Piece.Favor(1), p2, Location.PlayArea(p2), Location.PlayArea(p1)))
    assertEquals(look.favor(p2), 1)
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 4)

  test("a facedown Vow sends nothing to the bank, nor does another " +
      "player's gain"):
    val facedown = after(vowHolderWithOneFavor(facedown = true).ready,
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(facedown.favor(p1), 3)
    val other = after(vowHolderWithOneFavor().ready,
      Gain.Favor(p2, Suit.Order, 2))
    assertEquals(other.favor(p2), 5)
    assertEquals(other.bankFavor(Suit.Nomad), 2)

  test("a draw from the Nomad bank lands on the holder's board"):
    val look = after(vowHolderWithOneFavor().ready, DrawNomadFavor(p1, 2))
    assertEquals(look.favor(p1), 3)
    assertEquals(look.bankFavor(Suit.Nomad), 0)

  test("acting with another player's advisers lifts the Vow, and acting " +
      "with a holder's lends it"):
    // p1 is the walk's actor on Table.start, so the as-if advisers are
    // theirs.
    def actingWith(ready: ReadyGame, player: PlayerId): ReadyGame =
      ready.updateCurrent(_.copy(walkerAsIfAdvisers = Some(player)))
    val lifted = after(actingWith(vowHolderWithOneFavor().ready, p2),
      Gain.Favor(p1, Suit.Order, 2))
    assertEquals(lifted.favor(p1), 3)
    val lent = after(actingWith(Table.start.adviser(p2, VowOfKinshipCard)
      .favor(p1, 0).bankFavor(Suit.Nomad, 0).bankFavor(Suit.Order, 3).ready,
      p2), Gain.Favor(p1, Suit.Order, 2))
    assertEquals(lent.favor(p1), 0)
    assertEquals(lent.bankFavor(Suit.Nomad), 2)

  test("a holder's spendable favor counts the Nomad bank; another " +
      "player's does not"):
    val ready = vowHolderWithOneFavor().ready
    assertEquals(SpendableFavor.of(ready, p1), 3)
    assertEquals(SpendableFavor.of(ready, p2), 3)
    assertEquals(SpendableFavor.of(
      vowHolderWithOneFavor(facedown = true).ready, p1), 1)
