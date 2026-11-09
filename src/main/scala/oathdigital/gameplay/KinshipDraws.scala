package oathdigital.gameplay

import oathdigital.model._

/** Vow of Kinship's draw on the Nomad bank (catalog batch 11, slice 5,
  * ruling 9).
  *
  * A spend is a favor move out of a player's board that the player makes:
  * its mover is the board's owner. A cost's favor moves carry their payer,
  * a gift its giver, and a Challenge's or banner action's placement its
  * actor. A take, a forced burn and a forced placement carry another mover
  * or none, so they draw nothing and read the board only.
  *
  * Before a batch runs, for each holder, the pass reads the batch in order
  * against the favor on the holder's board. In front of the first
  * operation whose spend the board no longer covers, it puts a
  * `DrawNomadFavor` for what the board lacks, capped by the bank. Favor
  * bound for the holder's board lands in the bank, so the board only falls
  * within a batch, and one draw covers every later spend. A draw already in
  * the batch, the source question's answer, adds to the board.
  *
  * It runs before `BurnTakes`, which turns a cost's taken burn into a
  * `Take` by the taker and so hides its payer.
  */
object KinshipDraws:
  def before(ready: ReadyGame, operations: Vector[CoreOperation])
      : Vector[CoreOperation] =
    ready.game.current.players.map(_.player)
      .filter(KinshipRule.holds(ready, _))
      .foldLeft(operations)((batch, holder) => drawn(ready, holder, batch))

  /** The favor `operation` spends from `player`'s board. */
  def spent(operation: CoreOperation, player: PlayerId): Int =
    Operation.flatten(operation).collect {
      case Move(Piece.Favor(amount), from, _, _, Some(by))
          if by == player && from.location == Location.PlayArea(player) =>
        amount
    }.sum

  private def drawn(ready: ReadyGame, holder: PlayerId,
      operations: Vector[CoreOperation]): Vector[CoreOperation] =
    val board = ready.game.current.player(holder).fold(0)(_.board.favor)
    operations.foldLeft((Vector.empty[CoreOperation], board,
        KinshipRule.bank(ready))) {
      case ((out, held, left), draw @ DrawNomadFavor(`holder`, amount)) =>
        (out :+ draw, held + amount, left - amount)
      case ((out, held, left), operation) =>
        val need = spent(operation, holder)
        val short = math.min(math.max(0, need - held), math.max(0, left))
        val drawing = Option.when(short > 0)(DrawNomadFavor(holder, short))
        (out ++ drawing :+ operation, math.max(0, held + short - need),
          left - short)
    }._1
